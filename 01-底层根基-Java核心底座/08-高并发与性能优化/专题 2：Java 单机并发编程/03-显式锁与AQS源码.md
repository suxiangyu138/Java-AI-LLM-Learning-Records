# 03 显式锁与 AQS 源码

> Lock 接口的完整家族——ReentrantLock、ReadWriteLock、StampedLock，以及它们共同的骨架 AQS：state + 队列 + Condition

---

## 📚 目录

1. [Lock 接口：synchronized 之外的选择](#1-lock-接口synchronized-之外的选择)
2. [ReentrantLock：公平、可中断、超时](#2-reentrantlock公平可中断超时)
3. [ReadWriteLock 与 StampedLock](#3-readwritelock-与-stampedlock)
4. [AQS 源码：state 与队列](#4-aqs-源码state-与队列)
5. [锁选型决策](#5-锁选型决策)

---

## 1. Lock 接口：synchronized 之外的选择

Lock 是 JDK 5 引入的显式锁接口，核心能力是 synchronized 给不了的四个：**可中断获取**（`lockInterruptibly()`）、**超时获取**（`tryLock(1, TimeUnit.SECONDS)`）、**非阻塞试探**（`tryLock()`）、**公平性控制**（构造参数）。代价是必须手动解锁——标准姿势是 try-finally：

```java
lock.lock();
try {
    // 临界区
} finally {
    lock.unlock();   // 必须释放，否则死锁；建议 unlock 前判 isHeldByCurrentThread（可重入场景）
}
```

注意两个高频坑：`lock()` 与 `tryLock()` 的解锁语义不同——`tryLock()` 失败时并未持有锁，finally 里无条件 unlock 会抛 `IllegalMonitorStateException`；中断获取时异常与锁状态要理清，避免"锁没拿到却释放"。现代最佳实践是 `ReentrantLock` + `lock.lockInterruptibly()`，见 [Java 并发工具清单](../../02-JUC%20多线程%20高并发编程/JUC_Java高并发/04-Lock与AQS详解.md)。

## 2. ReentrantLock：公平、非公平、超时

ReentrantLock 是 Lock 的默认实现，语义与 synchronized 相同的可重入互斥锁，另支持：

- **公平性**：构造传 `true` 为公平锁——线程按先来后到排队，避免饥饿但增加切换开销；非公平锁允许新线程插队，吞吐更高。**生产默认非公平**，公平锁仅用于"必须严格先来先服务"的场景。为什么非公平反而快？插队线程可能刚好持锁者在释放临界点，免去一次 park/unpark 的挂起唤醒往返；代价是队尾线程的等待时间方差变大——绝大多数业务对"谁先等谁先得"无要求，这个方差换吞吐是划算的。
- **可中断**：`lockInterruptibly()` 让阻塞中的线程可被 interrupt 唤醒并抛异常，适用于"取消任务"场景。
- **超时**：`tryLock(2, SECONDS)` 在限定时间内获取，超时返回 false——是分布式/微服务调用"快速失败"的基础姿势。
- **Condition**：`newCondition()` 替代 wait/notify，支持**多条件队列**——一个锁上建多个条件（如"缓冲区非空""缓冲区未满"），比 wait/notifyAll 的广播唤醒精确得多，见 [05 篇](05-JUC同步工具与阻塞队列.md)。实现上 Condition 内部是 AQS 的**第二条队列**（条件队列）：`await()` 把当前线程封装成 Node 挂到条件队列并释放锁，`signal()` 把条件队列头节点**转移回同步队列**重新抢锁——两个队列一进一出，正是"等待"与"排队"的物理表达，理解了这个结构，Condition 的时序问题（signal 后谁先拿到锁）就有了判断依据。

## 3. ReadWriteLock 与 StampedLock

读多写少场景（缓存、配置）值得用读写分离：

- **ReentrantReadWriteLock**：读-读共享，读-写/写-写互斥。坑在**写饥饿**——大量读者持续进入时写者可能长期拿不到锁，可选用公平模式缓解。读锁不能升级为写锁（避免死锁），写锁可以降级为读锁。
- **StampedLock**（JDK 8）：在读写锁之上增加**乐观读**——`tryOptimisticRead()` 不真正加锁，先读后 `validate(stamp)` 校验版本，版本变了再升级为悲观读或重试。读多且读代价小时吞吐显著提升；代价是不支持重入、不支持 Condition，且**锁中断语义特殊**，使用门槛更高。

```java
long stamp = lock.tryOptimisticRead();
double v = readState();                    // 无锁读取
if (!lock.validate(stamp)) {               // 写操作发生了？
    stamp = lock.readLock();               // 升级为悲观读重读
    try { v = readState(); } finally { lock.unlockRead(stamp); }
}
```

## 4. AQS 源码：state 与队列

AQS（AbstractQueuedSynchronizer）是 JUC 同步器的公共骨架，ReentrantLock、Semaphore、CountDownLatch、ReentrantReadWriteLock 都建立在它之上。核心只有三样东西：

1. **volatile int state**：同步状态。ReentrantLock 里是"持有次数"（可重入计数），Semaphore 里是"剩余许可"，CountDownLatch 里是"剩余计数"。子类通过 `tryAcquire/tryRelease` 定义"什么算获取成功"。
2. **CLH 变体等待队列**：获取失败者封装成 Node 入队自旋/阻塞。公平锁与非公平锁的差异就一行——非公平锁在入队前先 `compareAndSetState(0, 1)` 试抢一次，抢不到才入队；公平锁要求 `hasQueuedPredecessors()` 为空才允许尝试。
3. **park/unpark 阻塞原语**：`LockSupport.park()` 挂起线程、`unpark()` 唤醒，与 wait/notify 的本质区别是**没有对象监视器依赖、没有时序漏洞**——unpark 先于 park 执行时，许可会保留（许可证机制），park 立即返回；而 wait 若先于 notify 执行会永久错过。注意 park 返回**不携带原因**——被唤醒后必须重新检查条件（`shouldParkAfterFailedAcquire`），这是 AQS 无虚假唤醒 bug 的关键。另外 park 支持 blocker 参数（`park(Object blocker)`），jstack 能看到"停在哪个对象上"，是排查阻塞现场的重要手段。

`acquire` 的标准流程：`tryAcquire` 成功即返回 → 失败则 `addWaiter` 入队 → `acquireQueued` 自旋检查前驱是否为 head 并再次 tryAcquire → 不满足则 park。释放走相反路径，`unparkSuccessor` 唤醒队首后继。**把 state + 队列 + park 想通，整个 JUC 锁体系就只剩"换 tryAcquire 的实现"**——这正是 [JUC 并发工具 07 篇](../../02-JUC%20多线程%20高并发编程/JUC_Java高并发/07-JUC并发工具类.md) 的底层逻辑。

验证这个心智模型最快的方式是手写一个 AQS 同步器——30 行实现一个一次性门闩：

```java
public class OneShotLatch {
    private static class Sync extends AbstractQueuedSynchronizer {
        @Override protected int tryAcquireShared(int arg) {
            return getState() == 1 ? 1 : -1;   // state=1 才放行
        }
        @Override protected boolean tryReleaseShared(int arg) {
            setState(1); return true;          // 一次性置 1
        }
    }
    private final Sync sync = new Sync();
    public void await() { sync.acquireSharedInterruptibly(0); }
    public void signal() { sync.releaseShared(0); }
}
```

这个例子说明：AQS 子类只需回答"什么状态算获取成功"，排队、阻塞、唤醒、中断处理全部复用——ReentrantLock、Semaphore、CountDownLatch 本质都是同样的几十行。

## 5. 锁选型决策

- **默认用 synchronized**：无需手动解锁、JVM 锁消除优化、代码最简，无竞争时与 ReentrantLock 性能同级；虚拟线程下默认也不 pin（JEP 491）。这符合"最小机制原则"——锁的语义越少，出错面越小；ReentrantLock 的每个额外能力（可中断、超时、公平）都是需要维护的契约，没有需求就不引入。
- **需要可中断/超时/公平/多条件**：ReentrantLock。超时获取是"调用外部服务"的底线纪律——任何等待都必须有上限，否则一次下游故障会沿着调用链把整个线程池挂死。
- **读多写少**：优先 ReadWriteLock；读操作昂贵且一致性要求宽松时上 StampedLock 乐观读。
- **虚拟线程 + 持锁阻塞路径**：ReentrantLock（不依赖对象监视器，无条件不 pin）。
- **计数/状态标记**：不用锁——原子类（[04 篇](04-CAS与原子类.md)）成本更低。
- **不可变或线程封闭能覆盖的**：不用任何锁——最便宜的锁是没有锁。

最后一条常被忽略：性能、正确性、可维护性三个维度上，锁都是最后手段而非第一手段。写代码前先问"这个状态真的需要共享吗"，比在锁选型表里纠结半天更值钱。

> 🎯 **核心要点**：AQS 是"state + CLH 队列 + park/unpark"的通用骨架，ReentrantLock/Semaphore/CountDownLatch 只是它的不同 tryAcquire 实现；锁选型不是"哪个快"，而是按可中断、超时、公平、读写比例、虚拟线程兼容五维决策。

---

**下一模块**：[04 CAS 与原子类](04-CAS与原子类.md)

**返回总览**：[00-总览](00-总览.md)
