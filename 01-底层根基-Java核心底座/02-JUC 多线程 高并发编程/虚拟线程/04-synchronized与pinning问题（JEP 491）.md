# synchronized 与 pinning 问题（JEP 491）

> 虚拟线程历史上最大的坑：synchronized 块内的阻塞会"钉死"载入线程。JDK 24 的 JEP 491 从 JVM 底层重写了监视器实现基本消灭该问题——理解它的来龙去脉，是虚拟线程生产落地绕不开的一课

---

## 📚 目录

1. [一次典型的 pinning 事故](#1-一次典型的-pinning-事故)
2. [机制剖析：为什么 synchronized 会 pin](#2-机制剖析为什么-synchronized-会-pin)
3. [JDK 21 的限制与临时对策](#3-jdk-21-的限制与临时对策)
4. [JEP 491 修复原理（JDK 24）](#4-jep-491-修复原理jdk-24)
5. [修复后仍存在的 pin 场景](#5-修复后仍存在的-pin-场景)
6. [诊断：JFR 事件与转储](#6-诊断jfr-事件与转储)
7. [synchronized 与 ReentrantLock 的新取舍](#7-synchronized-与-reentrantlock-的新取舍)
8. [核心要点与思考题](#8-核心要点与思考题)

---

## 1. 一次典型的 pinning 事故

```java
// 很常见的代码形态：业务锁 + 阻塞 I/O
class InventoryService {
    private final Object lock = new Object();

    void deduct(String sku, int qty) throws Exception {
        synchronized (lock) {                    // ← 进入 synchronized
            checkStock(sku);                     // 读 DB（阻塞 I/O）
            // ...
            Thread.sleep(50);                    // ← 阻塞点！JDK 21 下卡死载入线程
        }
    }
}
```

**JDK 21 下的现象**：

- 8 核机器，载入线程池只有 8 个线程；
- 8 个虚拟线程同时进入 `synchronized` 块内 sleep 50ms → **8 个载入线程全部被钉住 50ms**；
- 期间第 9 个虚拟线程**完全无法被调度**（没有空闲载入线程）；
- 锁竞争越激烈、阻塞时间越长，吞吐雪崩越严重——虚拟线程吞吐直接退化到"平台线程数 × 1 任务"。

> 🎯 **核心要点**：pinning = 虚拟线程阻塞时**无法卸载**，载入线程被它占住。不是死锁，但效果类似"8 车道高速上 8 辆车同时抛锚，其他百万辆车全堵在入口"。

## 2. 机制剖析：为什么 synchronized 会 pin

### 2.1 监视器的所有权记录在"线程"上

HotSpot 的 synchronized 实现（轻量锁 + 膨胀锁）把**监视器所有权与持有线程绑定**：

| 锁状态 | 所有权记录方式 | 问题 |
|-------|---------------|------|
| 轻量锁（fast-path） | 载入线程的 **LockStack**（每线程栈上数组）记录锁对象引用 | 卸载时 LockStack 跟着载入线程走，无法安全转移 |
| 膨胀锁（inflated） | `ObjectMonitor._owner` 保存 **`JavaThread*`**（指向 OS 线程对象） | 卸载后虚拟线程与载入线程分离，`_owner` 就悬空 |

**核心矛盾**：监视器只知道"哪个**线程**持有"，而虚拟线程阻塞时，物理上继续留在载入线程上才能维持"持有者"语义——所以 JVM 选择**不让它卸载**（pinning），以保证正确性。

### 2.2 JDK 21 会 pin 的三种情形

```text
情形 1：synchronized 块内发生阻塞（sleep / I/O / 锁等待）   ← 最常见
情形 2：虚拟线程阻塞在"获取已被持有的监视器"上（锁竞争）
情形 3：Object.wait() / wait(timeout) 等待通知            ← JDK 21 也 pin
```

> ⚠️ **容易误判**：`ReentrantLock` 系列（JUC 锁）不走监视器，**从第一天起就支持虚拟线程卸载**——这是 JDK 21 官方建议"synchronized 换成 ReentrantLock"的原因。

## 3. JDK 21 的限制与临时对策

### 3.1 官方立场（JEP 444 转正时）

- 这是**已知限制**，不是 bug：为了正确性主动选择 pin；
- 建议：库作者避免在 synchronized 块内执行阻塞操作；必要时改用 `ReentrantLock`；
- 提供诊断开关：`-Djdk.tracePinnedThreads=full|short`，把 pin 位置的栈打到标准错误。

### 3.2 JDK 21 用户的临时对策

| 对策 | 说明 | 代价 |
|------|------|------|
| 改用 ReentrantLock | 换锁即可解除 pin | 代码侵入、锁语义差异（不可中断性等） |
| 缩短临界区 | 阻塞 I/O 移出同步块 | 需要重构业务 |
| 提高 `maxPoolSize` | `jdk.virtualThreadScheduler.maxPoolSize=512` | 治标不治本，CPU 密集场景仍被 pin 拖死 |
| 诊断定位 | `tracePinnedThreads=full` + 修代码 | 人工成本高 |

## 4. JEP 491 修复原理（JDK 24）

> JEP 491 "Synchronize Virtual Threads without Pinning"，JDK 24 交付（作者：Patricio Chilano Mateo、Alan Bateman）。目标：让虚拟线程在 synchronized 块内阻塞时也能卸载。

### 4.1 三项核心改造

**① 轻量锁路径：LockStack 随 Continuation 迁移**

```text
unmount（冻结）时：                        remount（解冻）时：
载入线程 A 的 LockStack ──拷贝──→ 堆上 stackChunk  ──拷贝──→ 载入线程 B 的 LockStack
      （清空 A 的 LockStack）                                        （可能换线程）
```

锁对象引用（oops）从载入线程的 LockStack 复制进虚拟线程的分块栈，解冻时复制到**下一个载入线程**的 LockStack——锁的"持有者"从"物理线程"解耦为"虚拟线程的栈状态"。

**② 膨胀锁路径：`_owner` 记录 `tid` 而非 `JavaThread*`**

```java
// ObjectMonitor 内部（语义变化）
// JDK 24 前：_owner = JavaThread*（物理线程指针）
// JDK 24 后：_owner = java.lang.Thread 的 tid（逻辑线程标识）
```

监视器所有权与 `java.lang.Thread` 实例绑定，而不是平台线程——虚拟线程卸载后，`_owner` 依然指向虚拟线程的 tid，正确性不再依赖"持有者还在载入线程上"。

**③ 阻塞点支持卸载**：获取监视器阻塞、`Object.wait()`（含定时版）现在都会卸载虚拟线程、释放载入线程；唤醒后重新提交调度器，**可能换一个载入线程**执行。

### 4.2 修复覆盖范围

| 维度 | 说明 |
|------|------|
| 支持平台 | x64、aarch64、riscv、ppc（全部支持 Continuation 的架构） |
| 锁模式 | 默认 `LM_LIGHTWEIGHT` 与 `LM_MONITOR` 均修复 |
| 不修复 | `LM_LEGACY` 模式（早已废弃，默认关闭） |
| 转正版本 | JDK 24（2025-03）正式交付，非预览 |

### 4.3 实测效果（JEP 官方基准）

> 5,000 个虚拟线程，每个在**独立锁**的 synchronized 块内做 CPU 工作后 sleep 5ms，调度器并行度强制为 1：

| 版本 | 耗时 |
|:---:|:---:|
| JDK 21 | **~31.8 秒** |
| JDK 24 | **~0.454 秒**（约 70 倍提升） |

> 💡 **归因**：并行度=1 时 JDK 21 每次 pin 都是串行等 5ms——5000 × 5ms ≈ 25s 量级；JDK 24 下虚拟线程 sleep 即卸载，单载入线程可以全速推进所有任务。

## 5. 修复后仍存在的 pin 场景

JEP 491 没有消灭全部 pin。剩余场景与原因：

| 场景 | 原因 | 说明 |
|------|------|------|
| **原生代码回调 Java** | JNI 调用 Java 方法，栈在原生帧之上 | 虚拟线程栈与原生栈交错，JVM 无法安全冻结 |
| **类初始化（class initializer）中阻塞** | 初始化期间 JVM 内部锁 | 少见，但 `Class.forName` + 静态块 I/O 可能触发 |
| **符号引用解析中阻塞** | 链接期锁 | 极端冷启动场景 |

诊断方式：以上场景仍会触发 **`jdk.VirtualThreadPinned`** JFR 事件（事件增强为携带 **pinning 原因**与**载入线程标识**）。

```bash
# 查看 JFR 中 pin 事件（示例）
java -XX:StartFlightRecording=filename=app.jfr,dumponexit=true app.jar
jfr print --events jdk.VirtualThreadPinned app.jfr
```

> ⚠️ **版本注意**：JDK 21/22/23 的 pinning 行为与本文不同（synchronized 内阻塞均 pin）。若你仍在这些版本，回看 3.2 节对策。

## 6. 诊断：JFR 事件与转储

### 6.1 JFR 事件体系（JDK 21+）

| 事件 | 触发 | 用途 |
|------|------|------|
| `jdk.VirtualThreadStart` | 虚拟线程开始执行 | 吞吐统计 |
| `jdk.VirtualThreadEnd` | 虚拟线程结束 | 吞吐统计 |
| `jdk.VirtualThreadPinned` | 发生 pinning（默认阈值：**pin 超过 20ms** 才记录） | **定位 pin 场景** |
| `jdk.VirtualThreadSubmitFailed` | 提交到调度器失败 | 调度器过载告警 |

> 💡 **JFR 阈值的意义**：短于 20ms 的 pin 不值得报（吞吐损失可忽略）；默认阈值过滤噪音，聚焦真正的"载入线程被长时间钉住"问题。

### 6.2 JDK 24 后 `tracePinnedThreads` 已移除

```text
jdk.tracePinnedThreads 系统属性（JDK 21 引入）
    → JDK 24 起：设置无效（该路径已随 JEP 491 重写，JFR 事件取代它）
```

统一诊断入口：**JFR 的 `jdk.VirtualThreadPinned` + 线程转储**（`jcmd <pid> Thread.dump_to_file -format=json dump.json`）。

### 6.3 转储中的信号

```json
// jcmd 转储中，被 pin 的虚拟线程会显示在载入线程的栈中
{
  "threadName": "ForkJoinPool-1-worker-1",
  "stackTrace": [
    { "method": "jdk.internal.vm.Continuation.run" },   // ← 载入线程在跑某个虚拟线程
    { "method": "YourApp.someSynchronizedMethod" }       // ← 钉住它的位置
  ]
}
```

## 7. synchronized 与 ReentrantLock 的新取舍

JEP 491 之后，"换锁避免 pinning"的理由消失了，但锁选型仍有讲究：

| 维度 | synchronized | ReentrantLock / Condition |
|------|:---:|:---:|
| 虚拟线程阻塞会 pin 吗 | JDK 24+ 不会（原生回调除外） | 一直不会 |
| 不可中断阻塞 | 支持（不能 tryLock） | `lockInterruptibly` / `tryLock(timeout)` |
| 公平性 | 无 | 可配置公平 |
| 条件等待 | `wait/notify` | 多路 `Condition` |
| 与虚拟线程协作 | 正常 | 正常 |
| 代码风格 | 语法简洁 | 显式 lock/unlock（try-finally） |

**结论**：

- **默认用 synchronized**：语义最简、与 Java 生态（包括 `synchronized` 集合、Spring 等）兼容、JDK 24+ 无 pin 顾虑；
- **需要 tryLock/超时/公平/多条件**：用 JUC 锁——这是**功能需求**驱动，不再是 pin 驱动；
- **库作者注意**：仍应避免在临界区内做长阻塞操作——不是 pin 问题，而是锁竞争会放大为整个应用吞吐的瓶颈（临界区是串行段）。

## 8. 核心要点与思考题

> 🎯 **本章三句话总结**：
> 1. pinning 是 JDK 21 虚拟线程的已知限制：监视器所有权绑在物理线程上，synchronized 内阻塞无法卸载 → 载入线程被钉死；
> 2. JDK 24（JEP 491）从 JVM 底层修复：LockStack 随 Continuation 迁移、`_owner` 改记 `tid`、监视器阻塞/`Object.wait()` 支持卸载——实测吞吐提升约 70 倍；
> 3. 剩余 pin 仅剩原生回调、类初始化、符号解析三类场景，用 `jdk.VirtualThreadPinned` JFR 事件定位；锁选型回归"功能需求驱动"。

**思考题**：

1. 为什么轻量锁要拷贝 LockStack 才能解决 pin？（→ 4.1 ①）
2. `ObjectMonitor._owner` 从 `JavaThread*` 改为 tid 后，JVM 怎么判断锁释放？（→ 4.1 ②）
3. 生产环境怎么知道有没有 pin、pin 在哪？（→ 6.1）
4. JDK 21 用户升级到 JDK 24 后，有哪些代码可以"反向优化"？（→ 7：换回 synchronized）

---

**下一模块**：[05-线程池迁移与ExecutorService](05-线程池迁移与ExecutorService.md)｜**返回总览**：[00-虚拟线程知识体系总览](00-虚拟线程知识体系总览.md)
