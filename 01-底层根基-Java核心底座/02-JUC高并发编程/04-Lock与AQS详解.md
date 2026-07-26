# 04 — Lock与AQS详解

> 定位：掌握ReentrantLock核心API（lock/tryLock/lockInterruptibly）与Condition条件队列，深入AQS的CLH队列、state状态机与独占/共享模式实现原理

## 目录

1. [Lock接口与synchronized对比](#1-lock接口与synchronized对比)
2. [ReentrantLock核心API详解](#2-reentrantlock核心api详解)
3. [ReentrantLock公平锁vs非公平锁](#3-reentrantlock公平锁vs非公平锁)
4. [Condition条件队列](#4-condition条件队列)
5. [ReentrantReadWriteLock读写锁](#5-reentrantreadwritelock读写锁)
6. [StampedLock（JDK 8+）](#6-stampedlockjdk-8)
7. [AQS（AbstractQueuedSynchronizer）核心原理](#7-aqsabstractqueuedsynchronizer核心原理)
8. [AQS的CLH队列与state状态机](#8-aqs的clh队列与state状态机)
9. [AQS独占模式与共享模式](#9-aqs独占模式与共享模式)
10. [Lock使用规范与最佳实践](#10-lock使用规范与最佳实践)
11. [面试高频考点](#11-面试高频考点)

---

## 1. Lock接口与synchronized对比

### 1.1 核心定位

`java.util.concurrent.locks.Lock`是JDK 5引入的显式锁接口，作为`synchronized`内置锁的补充与升级。Lock需要手动控制加锁与解锁流程，具备更高的灵活性，支持公平锁、可中断锁、超时锁、多条件等待等高级特性。

`synchronized`是Java原生的内置隐式锁，由JVM自动管理加锁和释放，使用简单但灵活性不足。

### 1.2 详细对比

| 对比维度 | synchronized | Lock（ReentrantLock） |
|----------|-------------|----------------------|
| 锁管理方式 | 隐式，JVM自动加锁/释放 | 显式，手动lock()/unlock() |
| 异常安全性 | 异常自动释放锁，不会死锁 | 必须在finally中手动释放，否则死锁 |
| 公平性 | 仅非公平 | 支持公平和非公平两种模式 |
| 可中断性 | 不支持，线程一直阻塞 | lockInterruptibly()支持中断响应 |
| 超时获取 | 不支持 | tryLock(time, unit)支持超时 |
| 多条件等待 | 单一条件，wait/notify | 多个Condition独立管理 |
| 锁状态查询 | 不支持 | 支持hasQueuedThreads()等查询 |
| 性能（JDK 6+） | 优化后与Lock接近 | 高竞争场景略微占优 |
| 调试难度 | 简单，栈信息清晰 | 较复杂 |
| 代码简洁度 | 简洁、无冗余代码 | 需标准try/finally范式 |

### 1.3 选型建议

- **简单同步**：优先使用`synchronized`，代码简洁且无需手动管理锁，降低出错概率
- **复杂场景**：需要公平锁、可中断、超时、多条件通信等高级特性时，使用Lock
- **虚拟线程（JDK 21+）**：`synchronized`会pin虚拟线程到平台线程，推荐使用ReentrantLock

> 💡 JDK 6之后synchronized经过锁升级优化（偏向锁→轻量级锁→重量级锁），性能已大幅提升，不再是"重量级锁"代名词。选择依据应该是功能需求而非性能。

---

## 2. ReentrantLock核心API详解

### 2.1 Lock接口方法速览

| 方法 | 行为描述 | 响应中断 | 适用场景 |
|------|---------|---------|---------|
| `void lock()` | 获取锁，失败则阻塞等待 | 否 | 标准互斥场景 |
| `void lockInterruptibly()` | 可中断获取锁 | 是 | 允许线程被取消的场景 |
| `boolean tryLock()` | 非阻塞尝试，立即返回 | 否 | 无需等待、可放弃的场景 |
| `boolean tryLock(long, TimeUnit)` | 超时等待获取锁 | 是 | 有限等待、降级兜底 |
| `void unlock()` | 释放锁 | — | 必须放在finally中 |
| `Condition newCondition()` | 创建条件队列 | — | 多条件精准唤醒 |

### 2.2 标准使用范式

Lock的使用有严格规范，核心原则是：**锁必须在finally块中释放**，加锁操作在try之前，避免加锁成功后执行业务逻辑抛出异常导致死锁。

```java
private final Lock lock = new ReentrantLock();

public void syncMethod() {
    // 加锁必须在 try 之前
    lock.lock();
    try {
        // 临界区：共享资源操作
        doSharedWork();
    } finally {
        // 无论正常/异常，都必须释放锁
        lock.unlock();
    }
}
```

> ⚠️ **严禁写法**：不要把`lock()`放在`try`代码块内！若try内代码在加锁前抛出异常，会直接执行finally的`unlock()`，此时线程未持有锁，会抛出`IllegalMonitorStateException`。

### 2.3 基础实战示例

```java
class SafeCounter {
    private int count = 0;
    private final Lock lock = new ReentrantLock();

    public void increment() {
        lock.lock();
        try {
            count++;
        } finally {
            lock.unlock();
        }
    }

    public int getCount() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }
}

public class LockDemo {
    public static void main(String[] args) throws InterruptedException {
        SafeCounter counter = new SafeCounter();
        // 10个线程各自增1000次
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    counter.increment();
                }
            }).start();
        }
        Thread.sleep(2000);
        System.out.println("最终计数：" + counter.getCount()); // 10000
    }
}
```

### 2.4 lockInterruptibly() — 可中断获取

解决常规`lock()`一直阻塞无法中断的问题，适合线程池关闭、用户取消操作等场景。

```java
public void interruptibleLock() throws InterruptedException {
    lock.lockInterruptibly();  // 响应中断
    try {
        doSharedWork();
    } finally {
        lock.unlock();
    }
}

// 中断示例
Thread t = new Thread(() -> {
    try {
        interruptibleLock();
    } catch (InterruptedException e) {
        System.out.println("线程被中断，放弃锁等待");
        Thread.currentThread().interrupt();
    }
});
t.start();
t.interrupt(); // 中断等待锁的线程
```

### 2.5 tryLock() — 非阻塞与超时获取

避免线程无限阻塞，灵活处理获取锁失败的逻辑，提升程序健壮性。

```java
// 非阻塞尝试
public void tryLockImmediately() {
    if (lock.tryLock()) {
        try {
            doSharedWork();
        } finally {
            lock.unlock();
        }
    } else {
        // 未获取到锁，跳过或降级
        System.out.println("未获取到锁，执行降级逻辑");
    }
}

// 超时尝试
public void tryLockWithTimeout() throws InterruptedException {
    boolean acquired = lock.tryLock(3, TimeUnit.SECONDS);
    if (acquired) {
        try {
            doSharedWork();
        } finally {
            lock.unlock();
        }
    } else {
        // 超时未获取，执行降级
        System.out.println("3秒内未获取到锁，执行降级逻辑");
    }
}
```

---

## 3. ReentrantLock公平锁vs非公平锁

### 3.1 核心区别

ReentrantLock通过构造方法`ReentrantLock(boolean fair)`指定模式：

| 特性 | 非公平锁（默认） | 公平锁 |
|------|----------------|--------|
| 构造方式 | `new ReentrantLock()` / `new ReentrantLock(false)` | `new ReentrantLock(true)` |
| 获取锁策略 | 直接CAS竞争，不排队 | 按等待队列FIFO顺序获取 |
| 吞吐量 | 更高 | 略低 |
| 线程饥饿 | 可能（高竞争下） | 不会 |
| 上下文切换 | 更少 | 更多 |

### 3.2 公平锁源码行为

```java
// 公平锁 tryAcquire 核心逻辑
protected final boolean tryAcquire(int acquires) {
    final Thread current = Thread.currentThread();
    int c = getState();
    if (c == 0) {
        // hasQueuedPredecessors() 检查队列中是否有更早等待的线程
        if (!hasQueuedPredecessors() && compareAndSetState(0, acquires)) {
            setExclusiveOwnerThread(current);
            return true;
        }
    } else if (current == getExclusiveOwnerThread()) {
        // 可重入
        int nextc = c + acquires;
        setState(nextc);
        return true;
    }
    return false;
}

// 非公平锁 tryAcquire 缺少 hasQueuedPredecessors() 检查
// 直接 CAS 抢锁，允许"插队"
```

### 3.3 为什么非公平锁性能更高

非公平锁在`tryAcquire`时直接尝试CAS抢锁，如果抢锁成功则避免了一次线程挂起和唤醒操作（一次park/unpark约需5-10微秒）。这种"插队"行为减少了上下文切换，提升了整体吞吐量。

> 💡 默认场景使用非公平锁追求性能；当业务要求线程按照请求顺序获取锁，或需要避免线程饥饿时，使用公平锁。

---

## 4. Condition条件队列

### 4.1 与wait/notify对比

Condition通过`lock.newCondition()`创建，替代Object的`wait()`/`notify()`机制，支持多条件独立等待与精准唤醒。

| 对比维度 | Object.wait/notify | Condition.await/signal |
|----------|-------------------|----------------------|
| 前置条件 | 必须持有synchronized锁 | 必须持有Lock锁 |
| 条件数量 | 单个锁只有一个条件 | 一个Lock可创建多个Condition |
| 唤醒粒度 | notify随机/notifyAll全唤醒 | signal精准唤醒指定条件 |
| 虚假唤醒 | 需要while循环检查 | 同样需要while循环检查 |
| 中断响应 | wait抛出InterruptedException | await抛出InterruptedException |

### 4.2 多条件精准唤醒示例

```java
class BoundedBuffer<T> {
    private final Lock lock = new ReentrantLock();
    private final Condition notFull  = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    private final T[] items;
    private int putIdx, takeIdx, count;

    @SuppressWarnings("unchecked")
    public BoundedBuffer(int capacity) {
        items = (T[]) new Object[capacity];
    }

    public void put(T item) throws InterruptedException {
        lock.lock();
        try {
            // 队列满时，等待 notFull 条件
            while (count == items.length) {
                notFull.await();
            }
            items[putIdx] = item;
            if (++putIdx == items.length) putIdx = 0;
            count++;
            // 唤醒等待 notEmpty 条件的消费者
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    public T take() throws InterruptedException {
        lock.lock();
        try {
            // 队列空时，等待 notEmpty 条件
            while (count == 0) {
                notEmpty.await();
            }
            T item = items[takeIdx];
            items[takeIdx] = null;
            if (++takeIdx == items.length) takeIdx = 0;
            count--;
            // 唤醒等待 notFull 条件的生产者
            notFull.signal();
            return item;
        } finally {
            lock.unlock();
        }
    }
}
```

> 🎯 Condition的核心价值在于：一个Lock可以管理多个条件队列，生产者等待"不满"，消费者等待"不空"，`signal()`只唤醒对应条件上等待的线程，避免传统`notifyAll()`无效唤醒带来的性能损耗。

### 4.3 虚假唤醒防护

```java
// await() 必须在 while 循环中调用，不能使用 if
while (!conditionMet) {
    condition.await(); // 可能被"虚假"唤醒
}
// 条件满足后继续执行
```

> ⚠️ Object.wait()和Condition.await()都可能发生**虚假唤醒**（spurious wakeup），即线程在未被notify/signal且未被中断的情况下被唤醒。必须用while循环检查等待条件，不能使用if。

---

## 5. ReentrantReadWriteLock读写锁

### 5.1 核心思想

读写锁维护一对锁：读锁（共享锁）和写锁（排他锁），遵循"读读不互斥、读写互斥、写写互斥"的原则，在读多写少场景下显著提升并发性能。

| 当前持有锁 | 请求读锁 | 请求写锁 |
|-----------|---------|---------|
| 读锁 | 允许 | 阻塞 |
| 写锁 | 阻塞 | 阻塞 |

### 5.2 标准用法

```java
class ReadWriteCache {
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock  = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();
    private final Map<String, String> cache = new HashMap<>();

    // 读操作：多个线程可同时获取读锁
    public String get(String key) {
        readLock.lock();
        try {
            return cache.get(key);
        } finally {
            readLock.unlock();
        }
    }

    // 写操作：排他，必须等待所有读锁释放
    public void put(String key, String value) {
        writeLock.lock();
        try {
            cache.put(key, value);
        } finally {
            writeLock.unlock();
        }
    }
}
```

### 5.3 锁降级

ReentrantReadWriteLock支持**锁降级**：持有写锁时获取读锁，再释放写锁，使锁从写锁降级为读锁。

```java
// 锁降级：写锁 → 读锁（保证数据可见性）
public void processWithLockDowngrade() {
    writeLock.lock();
    try {
        // 写操作
        cache.put("key", "value");
        // 降级：先获取读锁，再释放写锁
        readLock.lock();
    } finally {
        writeLock.unlock(); // 释放写锁
    }
    try {
        // 此时仍持有读锁，保证写操作对其他读线程可见
        String val = cache.get("key");
    } finally {
        readLock.unlock();
    }
}
```

> 💡 锁降级是一种保护机制：确保写操作完成后，当前线程在释放写锁后仍持有读锁，防止其他写线程在中间状态修改数据。锁升级（读锁→写锁）不被支持，直接获取写锁会死锁。

---

## 6. StampedLock（JDK 8+）

### 6.1 三种访问模式

StampedLock是JDK 8引入的高性能读写锁，比ReentrantReadWriteLock更轻量，支持三种模式：

| 模式 | 说明 | 性能 |
|------|------|------|
| 写锁（writeLock） | 排他锁，独占 | 与读写锁写锁相当 |
| 悲观读锁（readLock） | 共享锁，允许并发读 | 与读写锁读锁相当 |
| **乐观读（tryOptimisticRead）** | 不加锁，仅检测数据版本 | 极高 |

### 6.2 乐观读模式

StampedLock的核心优化是乐观读：读取时不加锁，读完后验证数据是否被写线程修改，未被修改则直接使用，否则升级为悲观读锁重新读取。

```java
class Point {
    private final StampedLock lock = new StampedLock();
    private double x, y;

    // 乐观读：无锁读取，性能最高
    public double distanceFromOrigin() {
        long stamp = lock.tryOptimisticRead(); // 1. 获取版本戳
        double currentX = x;
        double currentY = y;
        if (!lock.validate(stamp)) {           // 2. 检查是否被写线程修改
            stamp = lock.readLock();           // 3. 乐观读失败，升级为悲观读锁
            try {
                currentX = x;
                currentY = y;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return Math.sqrt(currentX * currentX + currentY * currentY);
    }

    // 写锁：排他
    public void move(double deltaX, double deltaY) {
        long stamp = lock.writeLock();
        try {
            x += deltaX;
            y += deltaY;
        } finally {
            lock.unlockWrite(stamp);
        }
    }
}
```

### 6.3 StampedLock vs ReentrantReadWriteLock

| 特性 | StampedLock | ReentrantReadWriteLock |
|------|-------------|------------------------|
| 乐观读 | 支持（核心优势） | 不支持 |
| 可重入 | 不支持 | 支持 |
| Condition | 不支持 | 写锁支持Condition |
| 锁升级/降级 | 支持转换 | 仅支持降级 |
| 性能（读多写少） | 更高 | 良好 |
| 使用复杂度 | 较高（需管理stamp） | 较低 |

> ⚠️ StampedLock不是可重入锁，同一个线程不能重复获取锁；不支持Condition；且不能从写锁直接升级，需要使用`tryConvertToReadLock()`等转换方法。

---

## 7. AQS（AbstractQueuedSynchronizer）核心原理

### 7.1 AQS是什么

**AbstractQueuedSynchronizer**（抽象队列同步器）是JUC包的基石，几乎所有同步工具（ReentrantLock、Semaphore、CountDownLatch、ReentrantReadWriteLock等）都基于AQS实现。AQS提供了一套通用的同步状态管理框架，上层只需要实现`tryAcquire`/`tryRelease`等模板方法。

### 7.2 AQS的整体架构

```
                    AQS (AbstractQueuedSynchronizer)
  ┌──────────────────────────────────────────────────────────────────┐
  │  state (volatile int)           ─── 同步状态（核心字段）           │
  │    0 = 未锁定, 1 = 锁定, N = 重入次数                             │
  ├──────────────────────────────────────────────────────────────────┤
  │  CLH 等待队列（双向链表）         ─── 排队等待获取锁的线程          │
  │  ┌──────┐   ┌──────┐   ┌──────┐                                 │
  │  │ Node │◀─▶│ Node │◀─▶│ Node │  head → tail                    │
  │  │ prev │   │ prev │   │ prev │                                 │
  │  │ next │   │ next │   │ next │                                 │
  │  │thread│   │thread│   │thread│                                 │
  │  │waitSt│   │waitSt│   │waitSt│                                 │
  │  └──────┘   └──────┘   └──────┘                                 │
  ├──────────────────────────────────────────────────────────────────┤
  │  ConditionObject 条件队列        ─── await/signal 等待队列       │
  │  ┌──────┐   ┌──────┐                                            │
  │  │ Node │◀─▶│ Node │   firstWaiter → lastWaiter                 │
  │  │ Cond │   │ Cond │    每个 Condition 实例对应一个队列           │
  │  └──────┘   └──────┘                                            │
  └──────────────────────────────────────────────────────────────────┘
```

### 7.3 AQS的设计模式

AQS采用**模板方法模式**，子类通过实现特定方法来定义自己的同步语义：

```java
// 需要子类实现的模板方法
// 独占模式
protected boolean tryAcquire(int arg)     // 尝试获取锁
protected boolean tryRelease(int arg)     // 尝试释放锁

// 共享模式
protected int tryAcquireShared(int arg)   // 尝试获取共享锁
protected boolean tryReleaseShared(int arg) // 尝试释放共享锁

// 辅助方法
protected boolean isHeldExclusively()     // 当前线程是否独占
```

### 7.4 JUC同步器与AQS模式对应

| 同步工具 | 模式 | state语义 |
|---------|------|-----------|
| ReentrantLock | 独占 | 0=未锁定, >=1=重入次数 |
| Semaphore | 共享 | 剩余许可数 |
| CountDownLatch | 共享 | 剩余计数 |
| ReentrantReadWriteLock | 独占+共享 | 高16位=读锁数, 低16位=写锁重入数 |
| ThreadPoolExecutor | 独占+共享 | 高3位=线程池状态, 低29位=线程数 |

> 🎯 AQS的核心设计思路：将同步器拆分为"状态管理"（state）+ "等待队列"（CLH）+ "阻塞/唤醒"（LockSupport），子类只需关注如何获取和释放state，排队和线程调度由AQS统一处理。

---

## 8. AQS的CLH队列与state状态机

### 8.1 CLH队列变体

AQS中的等待队列是CLH锁队列的变体（变体CLH，不是原始的CLH自旋锁），使用**双向链表**代替原始的单向链表，增加对阻塞和唤醒的支持。

#### 节点状态（waitStatus）

| 值 | 名称 | 含义 |
|----|------|------|
| 1 | CANCELLED | 线程取消等待（中断或超时），节点进入终结态，不再变化 |
| -1 | SIGNAL | 后继节点需要被唤醒，当前节点释放锁后需通知后继 |
| -2 | CONDITION | 节点在Condition条件队列中等待 |
| -3 | PROPAGATE | 共享模式下，唤醒应向后传播 |
| 0 | 初始状态 | 节点刚被创建时的默认状态 |

```java
// Node 内部结构（简化）
static final class Node {
    volatile Node prev;        // 前驱节点
    volatile Node next;        // 后继节点
    volatile Thread thread;    // 当前节点持有的线程
    volatile int waitStatus;   // 等待状态

    // 模式标记
    static final Node SHARED = new Node(); // 共享模式
    static final Node EXCLUSIVE = null;    // 独占模式
}
```

### 8.2 acquire() 完整流程

以ReentrantLock的`lock()`为例，调用链为：`lock()` → `sync.acquire(1)` → AQS模板方法：

```
acquire(1)
  │
  ├─ tryAcquire(1)      ── 子类实现：尝试CAS设置state
  │    ├─ 成功 → return（获得锁）
  │    └─ 失败 → 进入队列
  │
  ├─ addWaiter(Node.EXCLUSIVE)
  │    ── 创建Node，CAS插入CLH队列尾
  │    ── 如果tail为空，初始化head和tail哨兵节点
  │
  └─ acquireQueued(node, 1)
       ── 循环自旋
       │
       ├─ p == head && tryAcquire(1) 成功
       │    ── 设当前节点为head
       │    ── 断开原head的next引用（help GC）
       │    ── return（获取锁成功）
       │
       ├─ shouldParkAfterFailedAcquire(p, node)
       │    ── 检查前驱waitStatus是否为SIGNAL
       │    ── 若是CANCELLED，跳过（删除取消节点）
       │    ── 若不是SIGNAL，CAS设置为SIGNAL
       │    ── 返回true/false
       │
       └─ parkAndCheckInterrupt()
            ── LockSupport.park(this) 阻塞线程
            ── 被unpark后检查中断标志
```

### 8.3 release() 流程

```java
release(1)
  ├─ tryRelease(1)     ── 子类实现：释放state
  │    ├─ state == 0 → 完全释放，返回true
  │    └─ state > 0  → 可重入未完全释放，返回false
  │
  └─ unparkSuccessor(h)
       ── 找到head后继节点中第一个非CANCELLED的节点
       ── LockSupport.unpark(thread) 唤醒等待线程
```

### 8.4 state状态机变化（以ReentrantLock为例）

```
线程A获取锁：      state: 0 → 1  (exclusiveOwnerThread = ThreadA)
线程A重入：        state: 1 → 2
线程A重入再次：    state: 2 → 3
线程A释放一次：    state: 3 → 2
线程A完全释放：    state: 2 → 0  (exclusiveOwnerThread = null)
线程B获取锁：      state: 0 → 1  (exclusiveOwnerThread = ThreadB)
```

> 💡 state的CAS操作通过`unsafe.compareAndSwapInt()`实现，保证原子性。重入计数器与owner线程双重判断，确保只有持有锁的线程才能修改state。

---

## 9. AQS独占模式与共享模式

### 9.1 独占模式（Exclusive）

独占模式下，同一时刻只有一个线程可以成功获取锁，其他线程进入CLH队列等待。典型实现：**ReentrantLock**。

```java
// 自定义独占锁示例
class Mutex {
    private static class Sync extends AbstractQueuedSynchronizer {
        @Override
        protected boolean tryAcquire(int acquires) {
            // state 0 → 1
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        @Override
        protected boolean tryRelease(int releases) {
            if (!isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            setExclusiveOwnerThread(null);
            setState(0);  // 直接归零，不支持重入
            return true;
        }

        @Override
        protected boolean isHeldExclusively() {
            return getExclusiveOwnerThread() == Thread.currentThread();
        }
    }

    private final Sync sync = new Sync();

    public void lock()        { sync.acquire(1); }
    public void unlock()      { sync.release(1); }
    public boolean tryLock()  { return sync.tryAcquire(1); }
}
```

### 9.2 共享模式（Shared）

共享模式下，多个线程可以同时获取锁，state表示剩余资源数。典型实现：**Semaphore**、**CountDownLatch**。

```java
// tryAcquireShared 返回值含义
// 负数：获取失败，进入队列等待
// 0：获取成功，但剩余资源为0，后续线程不再唤醒
// 正数：获取成功，还有剩余资源，后续线程可以继续获取
```

**共享模式关键区别**：

```
独占模式 release() → unparkSuccessor()
                          唤醒 head 的后继节点
                          一个节点唤醒后，获取锁，结束

共享模式 releaseShared() → doReleaseShared()
                          唤醒 head 的后继节点
                          被唤醒节点获取资源后，若还有剩余
                          继续传播唤醒下一个节点（setHeadAndPropagate）

共享模式 doAcquireShared() 流程：
  1. tryAcquireShared() < 0 → 进入队列自旋
  2. 前驱是head → 再次tryAcquireShared()
  3. 返回值 >= 0 → setHeadAndPropagate(node, r)
     └─ 如果剩余资源 > 0，继续唤醒后继节点（传播）
```

### 9.3 两种模式的信号传播差异

```
独占模式释放锁：
  head ──▶ NodeA ──▶ NodeB ──▶ NodeC
         唤醒A      A还在等     A还没唤醒
         (只唤醒head.next)

共享模式释放资源（剩余2个）：
  head ──▶ NodeA ──▶ NodeB ──▶ NodeC
         唤醒A       A唤醒后     传播继续
         A获取资源   发现还有    唤醒C
         后传播      剩余，唤醒B
```

---

## 10. Lock使用规范与最佳实践

### 10.1 强制规范

| 规范 | 说明 | 反例 |
|------|------|------|
| finally中释放锁 | unlock()必须放在finally块 | 异常时锁永久占用 |
| 加锁在try之前 | lock()在try代码块前调用 | try内lock()，加锁异常时误unlock |
| 加解锁次数一致 | 重入几次就释放几次 | 可重入锁释放不完整导致死锁 |
| 锁对象不可变 | private final修饰 | 锁引用被修改，同步失效 |
| 禁止重复释放 | 未持有锁调用unlock() | 抛出IllegalMonitorStateException |

### 10.2 最佳实践

```java
// 最佳实践：标准范式
private final Lock lock = new ReentrantLock();

public void bestPractice() {
    lock.lock();
    try {
        // 临界区代码尽量简短，不包含耗时操作
        // 不要在临界区内调用外部方法（可能死锁）
        doMinimalSharedWork();
    } finally {
        lock.unlock();
    }
}
```

### 10.3 死锁的四个必要条件与预防

| 必要条件 | 定义 | 预防策略 |
|----------|------|---------|
| 互斥 | 资源一次只能被一个线程占用 | 无法避免（锁的本质） |
| 持有并等待 | 线程持有锁A的同时等待锁B | 一次性申请所有锁 |
| 不可剥夺 | 已获得的锁不能被强制释放 | 使用tryLock超时机制 |
| 循环等待 | 线程1等锁B，线程2等锁A | 固定锁获取顺序 |

```java
// 预防方案1：固定锁顺序（破坏循环等待）
public void transfer(Account from, Account to, int amount) {
    // 根据ID大小确定锁顺序，避免循环等待
    Account first = from.id() < to.id() ? from : to;
    Account second = from.id() < to.id() ? to : from;

    synchronized (first) {
        synchronized (second) {
            from.debit(amount);
            to.credit(amount);
        }
    }
}

// 预防方案2：tryLock超时（破坏不可剥夺）
public boolean transferWithTryLock(Account from, Account to, int amount)
        throws InterruptedException {
    if (from.lock().tryLock(1, TimeUnit.SECONDS)) {
        try {
            if (to.lock().tryLock(1, TimeUnit.SECONDS)) {
                try {
                    from.debit(amount);
                    to.credit(amount);
                    return true;
                } finally {
                    to.lock().unlock();
                }
            }
        } finally {
            from.lock().unlock();
        }
    }
    return false; // 获取锁失败，不阻塞
}
```

### 10.4 锁粒度选择

| 场景 | 推荐方案 | 原因 |
|------|---------|------|
| 简单互斥 | synchronzied | 代码简洁，JVM自动管理 |
| 高级特性需求 | ReentrantLock | 公平/中断/超时/Condition |
| 读多写少 | ReentrantReadWriteLock | 读读并发，提升吞吐量 |
| 超高并发读 | StampedLock（乐观读） | 无锁读取，性能最优 |
| 虚拟线程（JDK 21+） | ReentrantLock | 避免synchronized pin问题 |

---

## 11. 面试高频考点

### 11.1 基础问答

**Q1：synchronized和ReentrantLock有什么区别？**

> 锁管理方式（隐式/显式）、公平性支持、可中断性、超时获取、多Condition、异常安全性。JDK 6后synchronized性能差距不大，选择依据在功能需求。

**Q2：ReentrantLock的lock()和lockInterruptibly()有什么区别？**

> lock()不响应中断，线程一直阻塞直到获取锁；lockInterruptibly()在等待锁的过程中响应`Thread.interrupt()`，抛出InterruptedException并退出等待。

**Q3：tryLock()和lock()有什么区别？**

> tryLock()非阻塞，立即返回boolean；lock()阻塞直到获取锁。tryLock支持超时重载：`tryLock(time, unit)`。

### 11.2 进阶问答

**Q4：AQS的原理是什么？**

> AQS基于模板方法模式，核心是volatile int state + CLH双向等待队列。通过CAS原子操作state，子类实现tryAcquire/tryRelease等模板方法。独占模式下state=0表示未锁定，共享模式下state表示剩余资源。

**Q5：AQS队列中节点的waitStatus有哪些状态？**

> CANCELLED(1)：线程取消；SIGNAL(-1)：后继需要唤醒；CONDITION(-2)：在条件队列；PROPAGATE(-3)：共享传播；0：初始状态。

**Q6：独占模式和共享模式有什么区别？**

> 独占模式一次只唤醒后继一个节点（head.next）；共享模式获取资源后有剩余时会传播唤醒后续节点（setHeadAndPropagate）。ReentrantLock是独占，Semaphore/CountDownLatch是共享。

### 11.3 深度问答

**Q7：公平锁和非公平锁的源码区别在哪里？**

> 非公平锁在tryAcquire中直接CAS抢锁；公平锁多了一步`hasQueuedPredecessors()`检查，如果队列中有等待线程则不能抢锁，必须排队。

**Q8：Condition的await()和signal()底层是怎么实现的？**

> await()在当前线程加入Condition条件队列（单链表），释放锁（完全释放到state=0），然后park阻塞；signal()将条件队列头节点转移到CLH队列尾，等待获取锁的线程唤醒后继续执行。

**Q9：ReentrantReadWriteLock的锁降级是如何实现的？**

> 写锁降级：持有写锁时获取读锁（state高16位+1），再释放写锁（state低16位-1）。保证写完成后其他读线程立即可见，防止写线程释放写锁后再次被其他写线程中断。

**Q10：StampedLock的乐观读为什么比读锁快？**

> 乐观读完全不修改state，也不阻塞线程，仅通过stamp版本号验证数据有效性。没有CAS操作，没有线程挂起/唤醒，因此性能接近无锁。

### 11.4 手写代码题

```java
// 高频考题1：基于AQS实现一个共享锁（最多N个线程同时访问）
class SharedLock {
    private static class Sync extends AbstractQueuedSynchronizer {
        Sync(int maxConcurrent) {
            setState(maxConcurrent);
        }

        @Override
        protected int tryAcquireShared(int acquires) {
            for (;;) {
                int available = getState();
                int remaining = available - acquires;
                if (remaining < 0 || compareAndSetState(available, remaining)) {
                    return remaining;
                }
            }
        }

        @Override
        protected boolean tryReleaseShared(int releases) {
            for (;;) {
                int current = getState();
                int next = current + releases;
                if (compareAndSetState(current, next)) {
                    return true;
                }
            }
        }
    }

    private final Sync sync;
    public SharedLock(int max) { sync = new Sync(max); }
    public void acquire()      { sync.acquireShared(1); }
    public void release()      { sync.releaseShared(1); }
}
```

---

> 🎯 Lock与AQS是JUC包的基石，理解它们能从原理层面掌握ReentrantLock、Semaphore、CountDownLatch、ReentrantReadWriteLock等所有同步工具。核心记住：state控制同步状态，CLH队列管理等待线程，Condition提供多路等待/通知——三者结合构成了Java并发同步的完整体系。

---

*参考：Java Lock使用全指南、Java并发编程、Java高并发核心技能*
*最后更新：2026-07-26 | JDK 8/11/17/21*
