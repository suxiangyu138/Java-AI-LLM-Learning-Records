# 06 虚拟线程与 Spring Boot 4 的调度变革

> Spring Boot 4.0（2025-11，Spring Framework 7）在 Java 21+ 上默认用虚拟线程执行 @Scheduled 与 @Async：SimpleAsyncTaskScheduler 取代线程池调度器、pool-size 属性被忽略、ThreadLocal 靠上下文快照传播——本模块讲透这次变革的机制、收益与陷阱

---

## 📚 目录

1. [虚拟线程原理：M:N 调度与廉价线程](#1-虚拟线程原理mn-调度与廉价线程)
2. [Boot 4 的调度变革：默认虚拟线程](#2-boot-4-的调度变革默认虚拟线程)
3. [pool-size 为什么被忽略](#3-pool-size-为什么被忽略)
4. [ThreadLocal 传播与上下文快照](#4-threadlocal-传播与上下文快照)
5. [pinning 陷阱与规避](#5-pinning-陷阱与规避)
6. [何时关闭虚拟线程](#6-何时关闭虚拟线程)
7. [虚拟线程下的调优新思维](#7-虚拟线程下的调优新思维)

---

## 1. 虚拟线程原理：M:N 调度与廉价线程

**虚拟线程（Project Loom，JDK 21 稳定）**：JVM 管理的轻量线程，由调度器把大量虚拟线程映射到少量平台线程（carrier）上执行。

```text
平台线程（Thread）   1 个线程 = 1 个 OS 线程 ≈ 1MB 栈内存，创建昂贵
虚拟线程（Virtual）  1 个虚拟线程 ≈ 1KB 栈内存，创建极廉价（百万级不成问题）
                    阻塞时自动让出 carrier → 其它虚拟线程继续跑（M:N 调度）
```

| 对比 | 平台线程池 | 虚拟线程 |
|------|:---:|:---:|
| 内存成本 | ~1MB/线程 | ~1KB/线程 |
| 数量 | 数百~数千上限 | 数十万~百万级 |
| 阻塞时 | 占住 OS 线程 | **让出 carrier**（高并发 I/O 关键优势） |
| 池化 | 必须（创建贵） | 无需（创建廉） |
| 适用 | 短任务/CPU 密集 | **I/O 密集**（阻塞等待场景） |

> 🎯 **核心要点**：虚拟线程的最大价值 = **阻塞不再是浪费**。任务在等待 DB/HTTP 响应时，carrier 被释放给其它任务——"每请求一线程"从噩梦变成可行。

## 2. Boot 4 的调度变革：默认虚拟线程

### 2.1 变化对比

| 版本 | 调度器 | 线程模型 |
|------|--------|---------|
| Boot 3.x | `ThreadPoolTaskScheduler` | 平台线程池（默认 1 线程） |
| **Boot 4 + Java 21** | **`SimpleAsyncTaskScheduler`** | **虚拟线程（每任务一线程）** |
| Boot 4 + Java 17 | ThreadPoolTaskScheduler | 平台线程池（属性生效） |

```yaml
# Boot 4 的默认行为（无需任何配置）：
# Java 21+ → @Scheduled/@Async 自动跑在虚拟线程上
# 关闭虚拟线程（可选）：
spring:
  threads:
    virtual:
      enabled: false
```

### 2.2 对任务语义的影响

| 场景 | 平台线程池时代 | 虚拟线程时代（Boot 4） |
|------|:---:|:---:|
| 多个 @Scheduled 互相阻塞 | 常见（默认 1 线程） | **天然免疫**（各自独立虚拟线程） |
| fixedRate 叠加执行 | 多线程下可能 | 仍可能（语义没变，需自行防重叠） |
| 任务内同步调用 DB/HTTP | 占线程 → 池打满 | 阻塞自动让出 carrier |
| 线程池参数调优 | 核心工作 | **大部分消失**（无池化） |

> 🎯 **要点**：Boot 4 的变革消灭了"调度线程阻塞""线程池打满"两类经典问题——**池参数调优不再是 @Scheduled/@Async 的主要工作**，精力转移到任务语义（防重叠、幂等、错误处理）。

### 2.3 源码视角

```text
TaskSchedulingAutoConfiguration（Boot 4）：
  Java 21+ 且 virtual.enabled=true
    → SimpleAsyncTaskScheduler
        → Executors.newVirtualThreadPerTaskExecutor()  每个任务新建虚拟线程
        → 不读 spring.task.scheduling.pool.size（无池可配）

  Java 17 或 virtual.enabled=false
    → ThreadPoolTaskScheduler（pool.size 生效）
```

## 3. pool-size 为什么被忽略

**官方明确文档化**：虚拟线程模式下，任务执行器的池大小属性（`pool.size`/`core-size`/`max-size`/`queue-capacity`）**全部忽略**。

```text
原因：虚拟线程创建成本 ~1KB、无池化必要
      "池"的假设（线程贵、复用）在虚拟线程下不成立
      SimpleAsyncTaskScheduler 就是"每任务一线程"——不需要队列与池
```

| 属性 | 平台线程模式 | 虚拟线程模式 |
|------|:---:|:---:|
| `spring.task.scheduling.pool.size` | ✅ 生效 | ⚠️ **忽略** |
| `spring.task.execution.pool.core-size` | ✅ 生效 | ⚠️ **忽略** |
| `thread-name-prefix` | ✅ | ✅（命名仍生效） |
| `shutdown.await-termination` | ✅ | ✅ |

> ⚠️ **注意**：升级 Boot 4 后"线程池配置不生效"不是 bug——是虚拟线程模式的预期行为。若确实需要平台线程池语义，设置 `spring.threads.virtual.enabled=false`。

## 4. ThreadLocal 传播与上下文快照

### 4.1 问题：虚拟线程的 ThreadLocal

- 虚拟线程支持 ThreadLocal（JDK 21 起合法），但**每任务新建虚拟线程 = 每任务全新 ThreadLocal**；
- 与平台线程池的"线程复用 + 线程本地缓存"完全不同——**不能假设上下文残留**。

### 4.2 Boot 4.0.4+ 的上下文快照

Spring 在包装任务时做**上下文捕获与恢复**（capture → restore）：

```text
提交任务时：捕获 RequestAttributes / SecurityContext / 事务 / MDC 等
执行任务时：恢复快照到虚拟线程
任务结束：清理（防泄漏）
```

| 上下文 | 传播 | 说明 |
|--------|:---:|------|
| SecurityContext | ✅ 自动（Boot 4.0.4+） | 无需手动 Delegating 包装 |
| RequestAttributes | ✅ 自动 | 同上 |
| MDC | 需配合 | 确认 Logback 与虚拟线程的兼容版本 |
| 事务 | 不传播 | 异步方法内部自开事务（同 04 篇） |

> ⚠️ **内存泄漏风险**：百万级虚拟线程 + 未清理的 ThreadLocal（尤其大对象）→ 快照恢复后不清理会累积。**任务内用完即清**（try-finally）。

## 5. pinning 陷阱与规避

**pinning（钉扎）**：虚拟线程执行 `synchronized` 块或调用原生方法时，无法让出 carrier——阻塞时 carrier 被占住，虚拟线程的优势退化。

| 触发源 | 说明 | 规避 |
|--------|------|------|
| `synchronized` 块 | 虚拟线程持有监视器时阻塞 → 钉扎 carrier | 换 `ReentrantLock`（可让出） |
| 原生方法（JNI） | 不可挂起 | 少见，评估依赖 |
| 旧库内部 synchronized | 如某些连接池/客户端 | 升级到虚拟线程兼容版本 |

> 💡 **JDK 24 的 JEP 491**（虚拟线程同步 pinning 修复）：synchronized 在虚拟线程中不再钉扎——**升级 JDK 24+ 后 pinning 问题从根上消失**；JDK 21/23 阶段仍需规避。

**检测方法：**

```bash
jcmd <pid> Thread.dump_to_file -format=json dump.json
# 检查虚拟线程是否被钉扎（JDK 21+ 支持）
# 或监控 carrier 线程数：pinning 严重时 carrier 数接近平台线程上限
```

## 6. 何时关闭虚拟线程

```yaml
spring:
  threads:
    virtual:
      enabled: false     # 明确回退平台线程池
```

| 应关闭（回退平台线程池） | 原因 |
|------------------------|------|
| 大量 synchronized/锁竞争的核心路径 | JDK 21/23 pinning 退化 |
| 依赖不支持虚拟线程的第三方库 | 行为不可控 |
| CPU 密集任务主导 | 虚拟线程收益有限，反而增加调度开销 |
| 监控/APM 工具不兼容 | 观测缺失风险大于收益 |

| 保持开启（默认） | 原因 |
|------------------|------|
| I/O 密集任务（DB/HTTP/消息） | 收益最大 |
| 任务数多、阻塞多的场景 | 省内存省线程 |
| JDK 24+ | pinning 已修复，无理由关闭 |

> 🎯 **要点**：虚拟线程是 I/O 密集任务的礼物、CPU 密集任务的平庸选项。决策看**任务画像**：阻塞多 → 开；计算多 → 平台线程池也不差。JDK 24 起（JEP 491）pinning 修复，默认开启几乎是安全答案。

## 7. 虚拟线程下的调优新思维

| 平台线程池时代 | 虚拟线程时代 |
|---------------|-------------|
| 调 core/max/queue | 关注**并发上限**（防资源耗尽）：限流/信号量 |
| 防线程池打满 | 防**无界并发**：虚拟线程便宜但外部资源（DB 连接池）仍有限 |
| 线程命名排查 | 命名 + **任务级观测**（指标/追踪） |
| 拒绝策略 | 无需（不拒绝）——但要**防任务堆积**（执行太慢导致排队） |

```java
// 虚拟线程时代的关键调优：限制并发而非线程数
Semaphore limiter = new Semaphore(50);    // DB 连接池 50 → 并发任务上限 50

@Scheduled(fixedDelay = 1000)
public void sync() {
    if (!limiter.tryAcquire()) {
        log.warn("concurrency limit reached, skip round");
        return;
    }
    try { doSync(); } finally { limiter.release(); }
}
```

> ⚠️ **新陷阱**：虚拟线程消灭了"线程不够"，但**没有消灭"资源不够"**——DB 连接池、外部 API 限流、内存仍是硬约束。**并发上限设计（信号量/限流）取代线程池参数，是虚拟线程时代的调优主战场**。

> 🎯 **核心要点**：Boot 4 调度变革三句话——①Java 21+ 默认虚拟线程（SimpleAsyncTaskScheduler，池参数忽略）；②ThreadLocal 靠上下文快照传播（Boot 4.0.4+ 自动），用完必清；③虚拟线程管"线程够不够"，信号量管"资源够不够"——JDK 24（JEP 491）后 pinning 问题消失，默认开启是 2026 年的安全答案。

---

**上一模块**：[05-线程池配置与性能调优](05-线程池配置与性能调优.md)　**下一模块**：[07-定时任务的异常处理与可观测](07-定时任务的异常处理与可观测.md)
