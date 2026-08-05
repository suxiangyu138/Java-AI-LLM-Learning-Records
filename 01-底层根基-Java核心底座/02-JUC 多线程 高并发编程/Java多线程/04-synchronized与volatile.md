# 04 synchronized 与 volatile

> synchronized 是 Java 互斥的基石，volatile 是可见性的轻量通道——理解它们的语义边界与锁的现代演进，是并发正确性的核心

---

## 📚 目录

1. [synchronized 三种用法与语义](#1-synchronized-三种用法与语义)
2. [Monitor 与对象头](#2-monitor-与对象头)
3. [锁升级的现代路径（偏向锁 15 废弃 / 18 移除）](#3-锁升级的现代路径偏向锁-15-废弃--18-移除)
4. [synchronized vs ReentrantLock](#4-synchronized-vs-reentrantlock)
5. [虚拟线程 pinning 与 JEP 491（JDK 24）](#5-虚拟线程-pinning-与-jep-491jdk-24)
6. [volatile：可见性、有序性与三大场景](#6-volatile可见性有序性与三大场景)

---

## 1. synchronized 三种用法与语义

| 用法 | 锁的对象 | 适用 |
|------|---------|------|
| 同步方法（实例） | **this（当前实例）** | 实例状态保护 |
| 同步静态方法 | **类对象（X.class）** | 类级别状态（static 字段） |
| 同步代码块 | **任意对象**（可指定） | 最小化临界区 |

```java
public class Account {
    private double balance;

    public synchronized void deposit(double amt) {      // 锁 this —— 所有实例方法互斥
        balance += amt;
    }

    public synchronized static void reset() { }          // 锁 Account.class —— 与实例锁互不相关

    public void transfer(Account target, double amt) {   // 代码块：锁自己 + 锁对方（避免死锁需固定顺序）
        synchronized (this) {                            // 锁当前对象
            synchronized (target) { ... }
        }
    }
}
```

**synchronized 的完整语义**（互斥 + 可见性 + 可重入）：

1. **互斥**：同一把锁同一时刻只允许一个线程进入临界区；
2. **可见性**：解锁 happens-before 后续加锁——锁内修改，下一个拿到锁的线程必然可见（见 03 模块第 6 节）；
3. **可重入**：同一线程可重复获取同一把锁（`synchronized` 方法调用自身 `synchronized` 方法不死锁）——**实现上锁与线程关联（持有计数）**；
4. **异常自动释放**：临界区抛异常，锁自动释放（区别于手动 Lock 必须 finally unlock）。

> 🎯 **核心要点**：synchronized 锁的是**对象**而非代码——"锁哪个对象"决定了互斥范围：两个线程锁不同对象则不互斥。可重入 + 异常自动释放，是它比手动 Lock 更"省心"的两个特性。

---

## 2. Monitor 与对象头

**Monitor（监视器锁）**：每个 Java 对象内在关联的"锁设施"——synchronized 的本质是"获取该对象的 Monitor"：

```text
对象内存布局（64 位 JVM，压缩指针下）：
┌─────────────────────┐
│ Mark Word（8B）      │ ← 锁状态、hashCode、GC 分代年龄
│ Class Pointer（4B）  │ ← 类型指针（指向类元数据）
│ 实例数据             │
│ Padding（对齐）      │
└─────────────────────┘
Mark Word 的复用：无锁时存 hashCode/分代；有锁时存锁记录指针/Monitor 指针
```

**Monitor 的内部结构**（重量级锁的实现）：

```text
Monitor（C++ 实现，每个对象可关联一个）：
├── Owner：持有线程
├── EntryList：等待获取锁的线程队列（BLOCKED 状态在这）
└── WaitSet：调用了 wait() 的线程队列（WAITING 状态在这）
   —— wait/notify 的语义就是操作 WaitSet！这也解释了为什么 wait 必须在 synchronized 里
```

**两个面试必答的"为什么"**：

| 问题 | 答案 |
|------|------|
| 为什么 `wait()` 必须在 synchronized 块里？ | wait 要把线程放入 Monitor 的 WaitSet 并释放锁——不持有锁就无法"释放"和"登记"，编译器直接检查 |
| BLOCKED 和 WAITING 状态对应什么？ | BLOCKED = 在 EntryList 等锁；WAITING = 在 WaitSet 等 notify |

> 🎯 **核心要点**：synchronized 的本质 = **获取对象 Monitor（Mark Word 指向它）**——锁状态存在对象头里，所以"每个对象都能当锁"；wait/notify 操作的就是 Monitor 的 WaitSet，所以必须持有 Monitor 才能调用（对象头与 Mark Word 细节见 `03-JVM完整底层/`）。

---

## 3. 锁升级的现代路径（偏向锁 15 废弃 / 18 移除）

**JDK 6 引入锁升级**（无锁 → 偏向锁 → 轻量级锁 → 重量级锁），但 **JDK 15/18 之后路径变了**：

```text
JDK 8-14 的路径：无锁 → 偏向锁 → 轻量级锁 → 重量级锁
现代 JDK（18+）路径：无锁 → 轻量级锁（CAS 自旋）→ 重量级锁
                        （偏向锁已移除！）
```

**偏向锁的完整时间线**（时效性重点，JUC/03 只讲到 15）：

| 版本 | 事件 | 说明 |
|------|------|------|
| JDK 6 | 引入偏向锁 | 针对"无竞争 + 同一线程反复加锁"优化：记录偏向线程，免 CAS |
| **JDK 15** | **JEP 374 废弃并默认禁用** | 收益不再明显（现代应用多用非同步集合）；撤销需要 STW（stop-the-world）成本高 |
| **JDK 18** | **代码移除（obsolete）** | `-XX:+UseBiasedLocking` 参数也不再有效 |
| 现代 JDK | 锁路径 = 轻量级（CAS 自旋）→ 重量级 | 简单、可维护、虚拟线程友好 |

**现代锁升级路径**（18+）：

```text
① 轻量级锁：CAS 尝试把 Mark Word 换成锁记录指针 —— 成功则持有（自旋等待）
② 自旋（JDK 6+ 自适应）：短竞争时 CAS 自旋等待，避免立即进内核
③ 重量级锁：自旋失败/超时 → 膨胀为 Monitor → 线程进入 EntryList（BLOCKED）
```

> 🎯 **核心要点**：**2025 年面试答"锁升级"必须带版本意识**——"JDK 8 的偏向锁升级路径"已是历史知识；现代 JDK（15 废弃、18 移除）的路径是**无锁 → 轻量级（CAS+自旋）→ 重量级**。答出这个版本窗口 = 区分"背老教程"与"追最新"。

---

## 4. synchronized vs ReentrantLock

| 维度 | synchronized | ReentrantLock |
|------|:------------:|:-------------:|
| 获取/释放 | 自动（语言级） | 手动（lock/unlock，**必须 finally unlock**） |
| 可中断 | ❌ 不可中断获取 | ✅ `lockInterruptibly()` |
| 超时获取 | ❌ | ✅ `tryLock(timeout)`（避免死锁利器） |
| 公平锁 | ❌ 非公平 | ✅ `new ReentrantLock(true)` |
| 条件变量 | wait/notify（一个队列） | **多条件 Condition**（精确唤醒） |
| 可观测性 | 弱 | `isLocked()`/`getQueueLength()`（监控友好） |
| 性能 | 现代 JVM 与 Lock 相当 | 同左（竞争激烈时同量级） |

```java
// ReentrantLock 的标准姿势：tryLock 超时 + finally 释放（防死锁）
ReentrantLock lock = new ReentrantLock();
if (lock.tryLock(2, TimeUnit.SECONDS)) {        // 拿不到锁 2 秒后放弃，不死等
    try {
        // 临界区
    } finally {
        lock.unlock();                           // 必须释放！漏了 = 死锁
    }
} else {
    log.warn("获取锁超时，降级处理");
}
```

**选择建议**（2025 共识）：

- **默认 synchronized**：代码简洁、自动释放、现代 JVM 性能已足够；
- **选 ReentrantLock 的四种场景**：需要**可中断/超时**获取、需要**公平性**、需要**多条件变量**、需要**锁的可观测性**（监控队列长度）；
- **虚拟线程场景（Java 21-23）**：synchronized 有 pinning 问题，ReentrantLock 优先（见下节；**Java 24+ 修复后恢复默认**）。

> 🎯 **核心要点**：synchronized 是"省心锁"（自动释放、可重入），ReentrantLock 是"精细锁"（超时/中断/公平/多条件）——**能用 synchronized 就 synchronized**，需要"拿不到就放弃"的场景换 ReentrantLock（tryLock 是防死锁的标准武器，见 06 模块）。

---

## 5. 虚拟线程 pinning 与 JEP 491（JDK 24）

**问题（Java 21-23）**：虚拟线程在 **synchronized 块内阻塞时被"钉住"（pinning）**——无法从载体线程卸载，载体线程被占住：

```text
虚拟线程的调度：阻塞时卸载（unmount），载体线程去执行其他虚拟线程
synchronized 的问题：JVM 不希望在持锁时卸载（锁在载体线程栈上）
→ 虚拟线程在 synchronized 内阻塞 → 被 pin 在载体线程上 → 不卸载
→ 如果所有虚拟线程都被 pin（如都在 synchronized 内做 IO）→ 载体线程耗尽 → 服务假死

实测数据（2025 案例）：50,000 虚拟线程场景，ReentrantLock 比 synchronized 快 8.8 倍
生产案例（games24x7）：2 个载体线程（=CPU 核数）被 pin 住 → 连健康检查都跑不了
```

**修复（JDK 24，JEP 491）**：JVM 让虚拟线程**可以独立于载体线程获取、持有、释放 Monitor**——pinning 问题消除，**synchronized 在 Java 24+ 恢复正常**：

```java
// Java 24+ 后的正确认知：
// ① synchronized 在虚拟线程中可正常使用（JEP 491 修复）
// ② Java 21-23 时代为绕开 pinning 而加的 ReentrantLock workaround 可以移除
// ③ 仍要避免的：synchronized 内做 CPU 密集/原生代码（与 pinning 无关的通用建议）

// 检测 pinning 的工具（部署在 21-23 时用）：
// JVM 参数：-Djdk.tracePinnedThreads=full     → 打印 pinning 时的堆栈
// JFR 事件：jdk.VirtualThreadPinned            → 监控面板告警
```

> 🎯 **核心要点**：这是"**多线程基础与最新 Java 交汇**"的最佳考题——答链 = **synchronized 原理（持锁与栈绑定）→ 虚拟线程 pinning 表现（载体线程被占）→ JEP 491 修复（JDK 24）→ 当前结论（24+ 可正常用 synchronized）**。能讲清这条链 = 既懂基础又追最新。

---

## 6. volatile：可见性、有序性与三大场景

**volatile 的语义**（Java 5+ 的内存语义）：

1. **可见性**：写 volatile 字段立即对后续读可见（写-读 happens-before）；
2. **有序性**：禁止对该字段的读写重排（内存屏障）；
3. **不保证原子性**：`volatile int i; i++` 依然非原子（读-加-写三步）——**volatile 不是锁**。

```java
// 适用场景一：状态标志（最经典）
volatile boolean running = true;         // 写线程停止循环；读线程循环检查
// 非 volatile 的问题：读线程可能永远看到旧值（缓存/寄存器优化）
while (running) { doWork(); }

// 适用场景二：DCL 双检锁单例（见 03 模块第 6 节）—— 有序性
private static volatile Singleton instance;

// 适用场景三：轻量级"最近值"（多读单写场景）
// 如：配置项热更新、进度上报（读多写少，容忍短暂不一致）
volatile double progress;
```

**volatile vs synchronized 选择**：

| 场景 | volatile | synchronized |
|------|:--------:|:------------:|
| 状态标志/开关 | ✅ 首选 | 重了 |
| 复合操作（i++） | ❌ 不安全 | ✅ |
| 多字段一致性 | ❌（单字段） | ✅ |
| 性能 | 无锁，极轻 | 有锁开销 |

> ⚠️ **volatile 的三大误用**（面试陷阱）：① `volatile int i; i++` 以为安全（**不修原子性**）；② 用 volatile 保证"多个字段的组合一致"（**只保单字段**）；③ 以为 volatile 是弱化版锁（**它没有互斥**——两个线程同时写 volatile 依然竞态）。

> 🎯 **核心要点**：volatile 的记忆锚 = **"单字段的可见性通道，不是锁"**——标志位用它，复合操作用锁/CAS。能说出"volatile 的三大场景 + 三大误用" = 此题满分。

---

**下一模块**：[05-线程间协作机制](./05-线程间协作机制.md) / **返回总览**：[00-Java多线程知识体系总览](./00-Java多线程知识体系总览.md)
