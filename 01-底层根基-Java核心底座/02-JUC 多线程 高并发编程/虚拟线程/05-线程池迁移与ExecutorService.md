# 线程池迁移与 ExecutorService

> 虚拟线程时代最大的工程动作：把固定线程池换成"每任务一线程"。本节给出迁移决策树、`newVirtualThreadPerTaskExecutor` 的实现剖析、限流方案与 7 步迁移清单——哪些迁、哪些不迁、怎么限流

---

## 📚 目录

1. [task-per-thread：虚拟线程时代的执行模型](#1-task-per-thread虚拟线程时代的执行模型)
2. [newVirtualThreadPerTaskExecutor 实现剖析](#2-newvirtualthreadpertaskexecutor-实现剖析)
3. [与固定线程池的全方位对比](#3-与固定线程池的全方位对比)
4. [迁移决策树：哪些迁、哪些不迁](#4-迁移决策树哪些迁哪些不迁)
5. [限流：Semaphore 取代池大小](#5-限流semaphore-取代池大小)
6. [不可迁移的耦合点](#6-不可迁移的耦合点)
7. [7 步迁移清单](#7-7-步迁移清单)
8. [核心要点与思考题](#8-核心要点与思考题)

---

## 1. task-per-thread：虚拟线程时代的执行模型

### 1.1 模式的提出

JEP 444 明确推荐的核心模式：**每个任务创建一个虚拟线程**，任务结束线程即消亡：

```java
// 传统：池化复用（线程是稀缺资源时的妥协）
ExecutorService pool = Executors.newFixedThreadPool(200);
for (Task t : tasks) pool.submit(t::run);

// 虚拟线程：任务即线程（线程便宜到不需要复用）
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (Task t : tasks) executor.submit(t::run);
}
```

### 1.2 为什么池化被反模式化

| 池化的动机 | 虚拟线程下还成立吗 |
|-----------|:---:|
| 线程创建成本高 | ❌ 创建虚拟线程≈堆分配，纳秒~微秒级 |
| 栈内存贵 | ❌ 堆上分块栈按需伸缩，用完随 GC 回收 |
| 复用避免内核资源消耗 | ❌ 没有内核资源占用 |
| **限流**（控制并发度） | ✅ **唯一成立的动机，但用 Semaphore 实现**（见第 5 节） |

> ⚠️ **官方立场**：OpenJDK 维护者 2026-01 在 loom-dev 再次重申"不要池化虚拟线程"——JDK 自身甚至移除了若干假设线程池复用的 ThreadLocal 缓存（如 byte[] 缓冲），就是因为虚拟线程场景下缓存反而占用大量内存。

### 1.3 常见的"假迁移"

```java
// ❌ 反模式：虚拟线程套线程池——既没省线程数，又破坏了"用完即弃"
ExecutorService pool = Executors.newFixedThreadPool(200);
ThreadFactory tf = Thread.ofVirtual().factory();     // 每个工作线程是虚拟线程？
pool = Executors.newFixedThreadPool(200, tf);        // 但只创建 200 个！本质是池化

// ✅ 正确：直接使用每任务一线程执行器
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
```

## 2. newVirtualThreadPerTaskExecutor 实现剖析

### 2.1 内部逻辑（JDK 源码）

```java
// java.util.concurrent.Executors（JDK 21，简化）
public static ExecutorService newVirtualThreadPerTaskExecutor() {
    return new VirtualThreadPerTaskExecutor();
}

private static final class VirtualThreadPerTaskExecutor implements ExecutorService {
    public void execute(Runnable command) {
        // 核心：每个任务 = 一个新虚拟线程，立即 start
        Thread.ofVirtual().name(...).start(command);
    }
    // shutdown()：停止接收新任务；已提交任务继续执行
    // close()：shutdown() + awaitTermination() —— try-with-resources 友好
}
```

要点：

- **无队列、无池、无拒绝策略**：`execute()` 直接创建一个虚拟线程并 start；
- **命名**：自动为虚拟线程命名（便于转储识别）；
- **`ExecutorService` 接口完整兼容**：`submit` / `invokeAll` / `invokeAny` / `shutdown` / `awaitTermination` 语义与普通执行器一致；
- **`close()`（JDK 19+）**：实现 `AutoCloseable`，try-with-resources 中关闭时会**等待所有已提交任务完成**——比手写 `shutdown+awaitTermination` 更简洁。

### 2.2 与 `newCachedThreadPool` 的关系

`CachedThreadPool` 是虚拟线程执行器的"前身"：任务多就建新线程，空闲 60s 回收。但两者有本质差别：

| 维度 | CachedThreadPool | VirtualThreadPerTaskExecutor |
|------|:---:|:---:|
| 线程上限 | 无上限但受 OS 限制（数千即崩） | 无上限（数百万级） |
| 线程创建成本 | 系统调用 + ~1MB 栈 | 堆分配 + 分块栈 |
| 空闲回收 | 60s | 立即（任务完即弃） |
| 平台线程占用 | 高峰占用 N 个 OS 线程 | **始终只占用 CPU 核数个 OS 线程** |

> 💡 **迁移直觉**：凡是"高并发 I/O 任务 + CachedThreadPool 或大固定池"的组合，几乎都可以直接换成 `newVirtualThreadPerTaskExecutor()`。

## 3. 与固定线程池的全方位对比

| 维度 | `newFixedThreadPool(200)` | `newVirtualThreadPerTaskExecutor()` |
|------|:---:|:---:|
| 并发能力 | ≤200（受池大小硬限制） | 无上限（受堆内存软限制） |
| 排队行为 | 任务进有界/无界队列 | **无队列**，任务直接运行 |
| 峰值内存 | 200×1MB 原生栈（固定） | 随并发线性增长（堆，可 GC） |
| 阻塞任务 | 占一个池线程（浪费） | 阻塞即卸载（不占载入线程） |
| 任务耗时不均 | 慢任务拖慢池整体（队头阻塞） | 互不影响 |
| 降级能力 | 排队 = 天然背压 | **无背压**，需显式限流 |
| 关闭语义 | shutdown 后等待 | 同（+close() 便捷） |
| 适用任务类型 | CPU 密集、有界资源操作 | I/O 密集、无共享瓶颈 |

> 🎯 **核心要点**：固定池的本质是"并发度硬上限 + 排队缓冲"；虚拟线程执行器的本质是"并发度无硬上限 + 无缓冲"。前者天然限流，后者**必须自己加限流**（第 5 节）。

## 4. 迁移决策树：哪些迁、哪些不迁

### 4.1 决策树

```text
现有 ExecutorService 迁移评估
│
├─ 任务是否以阻塞 I/O 为主（网络、DB、文件、HTTP、MQ 消费）？
│   ├─ 是 → 迁移 ✅（收益最大）
│   │      └─ 是否依赖"池大小"做限流？
│   │          ├─ 是 → 迁移 + Semaphore 限流（第 5 节）
│   │          └─ 否 → 直接换执行器即可
│   └─ 否（CPU 密集：计算、加解密、JSON/图像处理）
│        └─ 不迁移 ⚠️（虚拟线程无收益，且有载入线程饥饿风险，见 08 章）
│
└─ 任务是否持有原生资源 / 需要长时间驻留状态？
     └─ 是 → 谨慎：保留平台线程池（如连接池内部、JNI 组件）
```

### 4.2 迁移收益量化

```text
假设：8 核机器，每任务 80ms 阻塞 I/O + 20ms CPU，QPS 目标 10,000

固定池（经验公式：线程数 = 核数 × (1 + 等待/计算) = 8 × 5 = 40 线程）
    极限吞吐 ≈ 40 / 0.1s = 400 QPS ❌

虚拟线程（每任务一线程，阻塞不占载入线程）
    载入线程数 = 8，每载入线程每秒可推进 ≈ 8 个任务（80ms 阻塞期间换其他任务）
    极限吞吐 ≈ 8 / 0.02s(仅CPU段串行) ... 实际受 CPU 段限制：10000 QPS × 0.02s = 200s CPU/s
    8 核只能支撑 ≈ 400 QPS CPU —— 吞吐瓶颈从"线程数"转移到了"CPU"
```

> 💡 **关键洞察**：虚拟线程迁移后，吞吐瓶颈从**线程数**变成**CPU 与外部系统**。如果 I/O 占比高（如 95%），同配置下吞吐可提升 10-50 倍；如果 CPU 占比高，提升几乎为 0——这就是决策树的依据。

### 4.3 框架级迁移（一行配置）

```yaml
# Spring Boot 3.2+：内嵌 Tomcat 切换到虚拟线程
spring:
  threads:
    virtual:
      enabled: true        # Tomcat 每请求一个虚拟线程
```

```java
// Jetty 12 / Tomcat 10.1+ 也原生支持虚拟线程执行器
// 注意：切换后要重新评估限流——请求数不再受线程池上限保护
```

## 5. 限流：Semaphore 取代池大小

### 5.1 问题：无队列 = 无背压

固定池 200 线程时，第 201 个任务自动排队——这是**隐式限流**。换成虚拟线程后，突发 100 万任务会瞬间创建 100 万虚拟线程，造成：

- 堆内存峰值飙升（每个线程对象 + 栈 chunk）；
- 外部系统（DB、下游服务）被突发流量打垮。

### 5.2 方案：显式 Semaphore 限流

```java
// 保留"并发上限"的语义，去掉"线程池"的机制
class VirtualThreadLimiter {
    private final Semaphore sem = new Semaphore(500);   // 最多 500 个并发

    void process(Runnable task) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(() -> {
                try {
                    sem.acquire();                       // 获取并发许可
                    task.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    sem.release();
                }
            });
        }
    }
}
```

| 限流维度 | 固定池 | Semaphore |
|---------|-------|-----------|
| 控制并发数 | ✅（隐式） | ✅（显式） |
| 排队/拒绝策略 | 队列 + RejectedExecutionHandler | 可阻塞等待 / 可 tryAcquire 快速失败 |
| 公平性 | 队列 FIFO | 可配置公平模式 |
| 与虚拟线程配合 | 反模式 | ✅ 天然适配（阻塞在 semaphore 上会卸载） |

> 💡 **限流位置**：对**下游依赖**限流比全局限流更精细——例如 `Semaphore` 按目标服务拆多个（DB 50 并发、外部 API 20 并发），比"全局 500 并发"更能保护脆弱依赖。

## 6. 不可迁移的耦合点

以下代码模式**不要**迁移，或迁移前必须先解耦：

| 耦合点 | 问题 | 处理 |
|--------|------|------|
| 依赖池大小的业务逻辑 | 如"池满则降级"的 `RejectedExecutionHandler` | 改为 Semaphore `tryAcquire` 快速失败 |
| ThreadLocal 大量缓存 | 百万虚拟线程 × 缓存 = 内存放大 | 换 ScopedValue / 移除缓存（08 章） |
| `Thread.setPriority` | **虚拟线程忽略优先级**（无优先级语义） | 删除或依赖 OS 线程数控制 |
| 线程名假设 | 代码里解析 `Thread.currentThread().getName()` 格式 | 用 `VirtualThreadPerTaskExecutor` 默认命名即可 |
| `ThreadGroup` | 虚拟线程不属于传统 ThreadGroup | 迁移监控逻辑（用 JFR/jcmd） |
| 每任务固定成本极高 | 如任务内创建 50MB 堆对象 | 迁移本身无影响，但限流更要紧 |
| synchronized 内长阻塞 | JDK 21 环境会 pin | 升级 JDK 24+ 或改 ReentrantLock（04 章） |

## 7. 7 步迁移清单

```text
□ 1. 盘点：列出所有 ExecutorService，标注任务类型（I/O 密集 / CPU 密集）
□ 2. 决策：按 4.1 决策树，标记"迁移 / 不迁移"
□ 3. 替换：Executors.newVirtualThreadPerTaskExecutor() 替换固定池工厂
        （保留 ExecutorService 类型引用，业务代码零改动）
□ 4. 限流：原池大小 → Semaphore（5.2 模式）；有界队列 → 快速失败策略
□ 5. 清理：删除池化相关代码（队列满处理、线程工厂、ThreadLocal 缓存）
□ 6. 验证：压测对比吞吐/时延/P99；观察载入线程数、JFR VirtualThread 事件
□ 7. 监控：线程转储脚本改为 json 格式；新增 jdk.VirtualThreadPinned 告警
```

> 🎯 **迁移原则**：**只换执行器工厂，不动业务代码**——虚拟线程最大的工程红利就是 `ExecutorService` 接口兼容。改动面越小，回归风险越低。

## 8. 核心要点与思考题

> 🎯 **本章三句话总结**：
> 1. 虚拟线程时代的执行模型是 task-per-thread：每任务一线程、用完即弃、**禁止池化**；
> 2. 迁移决策树的核心判据是"是否 I/O 密集"：是则迁（收益 10-50 倍吞吐），否则不迁（CPU 密集无收益且有饥饿风险）；
> 3. 失去"池大小限流"后必须用 Semaphore 显式限流——虚拟线程的并发是软上限，保护下游靠显式控制。

**思考题**：

1. `newVirtualThreadPerTaskExecutor()` 有队列吗？突发任务时会发生什么？（→ 2.1/5.1）
2. "虚拟线程套固定线程池"为什么是反模式？（→ 1.3）
3. 迁移后吞吐瓶颈会转移到哪里？（→ 4.2）
4. 为什么 OpenJDK 自己都移除了 ThreadLocal 缓存？（→ 1.2，08 章展开）

---

**下一模块**：[06-结构化并发StructuredTaskScope](06-结构化并发StructuredTaskScope.md)｜**返回总览**：[00-虚拟线程知识体系总览](00-虚拟线程知识体系总览.md)
