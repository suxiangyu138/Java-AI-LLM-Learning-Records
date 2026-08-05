# Java多线程与JUC 面试宝典
> 基于课程大纲全面覆盖Java多线程与JUC面试高频考点，涵盖基础概念、深度原理、实战场景、手写代码、系统设计及常见坑点。对标阿里、腾讯、字节、美团等一线大厂面试难度。

## 目录
1. [基础概念速答](#1-基础概念速答15-20题)
2. [深度原理剖析](#2-深度原理剖析10-15题)
3. [实战场景题](#3-实战场景题8-12题)
4. [手写代码题](#4-手写代码题5-8题)
5. [系统设计题](#5-系统设计题3-5题)
6. [常见坑点与最佳实践](#6-常见坑点与最佳实践)
7. [面试回答模板](#7-面试回答模板)
8. [快速查漏补缺](#8-快速查漏补缺-checklist)

---

## 一、基础概念速答（15-20题）

### 1. 什么是进程？什么是线程？两者的区别？
**进程**是操作系统资源分配的最小单位，每个进程拥有独立的地址空间、文件描述符、堆栈。**线程**是CPU调度的最小单位，是进程内的一个执行路径，共享进程的堆和方法区，但每个线程拥有独立的程序计数器、虚拟机栈和本地方法栈。

> 💡 **核心区别**：进程间相互隔离（一个崩了不影响其他），线程间共享堆空间（一个线程OOM会导致进程内所有线程受影响）。上下文切换成本：进程 >> 线程。

### 2. 什么是并发？什么是并行？两者的区别？
**并发**指两个或多个任务在**同一时间段内**交替执行（宏观同时，微观串行），单核CPU即可实现。**并行**指两个或多个任务在**同一时刻**真正同时执行，需要多核CPU支持。

| 维度 | 并发 | 并行 |
|------|------|------|
| 核心数要求 | 单核即可 | 必须多核 |
| 执行方式 | 时间片轮转，交替执行 | 同时执行 |
| 关注点 | 任务结构设计 | 执行效率 |
| 典型场景 | Web服务器处理多个请求 | 大数据并行计算 |

### 3. 创建线程有哪几种方式？
三种核心方式：

1. **继承Thread类**：重写 `run()` 方法，`new MyThread().start()`。缺点：Java单继承，无法继承其他类。
2. **实现Runnable接口**：实现 `run()` 方法，`new Thread(new MyRunnable()).start()`。无返回值，无法抛受检异常。
3. **实现Callable接口**（配合FutureTask）：实现 `call()` 方法，有返回值，可抛异常。`FutureTask<Integer> ft = new FutureTask<>(new MyCallable()); new Thread(ft).start(); ft.get();`

> 💡 **实际开发**：优先使用Runnable/Callable，避免继承局限。从Java 8起，更推荐使用 `CompletableFuture` 或线程池提交任务。

### 4. start() 和 run() 的区别？
- **run()**：普通方法调用，在当前线程中同步执行，不会创建新线程。
- **start()**：启动新线程，使线程进入就绪状态，由JVM自动调用run()方法。

> ⚠️ 一个线程对象只能调用一次 `start()`，第二次调用会抛出 `IllegalThreadStateException`。

### 5. 线程的优先级是什么？是否可以保证执行顺序？
线程优先级用 `setPriority(1-10)` 设置，默认5（`Thread.NORM_PRIORITY`）。**高优先级线程获得CPU时间片的概率更大，但不能保证执行顺序**，最终调度由底层OS决定。Windows/Linux等不同操作系统对优先级的映射处理不同。

> 💡 **不建议依赖优先级来保证执行顺序**，应使用 `join()`、`CountDownLatch` 等同步工具。

### 6. 什么是守护线程？如何设置？
守护线程（Daemon Thread）是为用户线程提供服务的后台线程，当所有用户线程结束时，JVM自动退出，**不会等待守护线程执行完毕**。通过 `thread.setDaemon(true)` 设置，**必须在 `start()` 之前调用**。

典型守护线程：GC垃圾回收线程、Finalizer线程、Tomcat/Netty等框架中的监控线程。

### 7. yield() 和 join() 方法的作用？
- **yield()**：提示线程调度器"我愿意让出当前CPU时间片"，使当前线程从运行态回到就绪态。但调度器**可以忽略**这个提示，不保证其他线程一定获得CPU。
- **join()**：当前线程等待目标线程执行完毕再继续执行。`t.join()` — 无限等待直到t死亡；`t.join(1000)` — 最多等待1000ms。底层基于 `wait/notify` 机制实现。

### 8. 线程有哪6种状态？状态如何转换？
Java线程有6种状态（定义在 `Thread.State` 枚举中）：

| 状态 | 说明 | 触发条件 |
|------|------|----------|
| NEW | 新建，尚未启动 | `new Thread()` 创建后，未调用 `start()` |
| RUNNABLE | 可运行（就绪 + 运行中） | 调用 `start()`，或从等待/阻塞返回 |
| BLOCKED | 阻塞等待monitor锁 | 等待进入 `synchronized` 代码块/方法 |
| WAITING | 无限期等待 | `wait()` / `join()` / `park()` |
| TIMED_WAITING | 限期等待 | `sleep(ms)` / `wait(ms)` / `join(ms)` / `parkNanos()` |
| TERMINATED | 终止 | `run()` 执行完毕或抛出异常 |

> 💡 **经典流转**：NEW -> (start()) -> RUNNABLE -> (synchronized竞争失败) -> BLOCKED -> (获得锁) -> RUNNABLE -> (wait()) -> WAITING -> (notify()) -> RUNNABLE -> (run()结束) -> TERMINATED

### 9. sleep() 和 wait() 的区别？
| 维度 | sleep() | wait() |
|------|---------|--------|
| 所属类 | Thread的静态方法 | Object的实例方法 |
| 是否释放锁 | **不释放**任何锁 | **释放**monitor锁 |
| 调用前提 | 无需持有锁 | 必须在 `synchronized` 块中 |
| 唤醒方式 | 超时后自动唤醒 | 需要 `notify()`/`notifyAll()` 唤醒 |
| 作用目标 | 当前线程 | 持有锁对象上的等待线程集 |

### 10. 什么是线程安全？什么情况下会出现线程安全问题？
**线程安全**：当多个线程访问某个类时，无论运行环境如何调度，类始终表现出正确的行为。线程安全问题发生的三个必要条件：**多线程环境** + **共享资源** + **非原子操作**。

常见场景：
- 多线程操作共享变量（i++ 非原子性）
- 多线程操作集合类（HashMap扩容死链、ArrayList数组越界）
- 多线程读写文件/数据库连接等共享资源

### 11. synchronized 关键字的底层原理？
`synchronized` 基于JVM内置锁（Monitor锁）实现。JDK 1.6 后引入**锁升级**机制：

1. **偏向锁**：无竞争时，CAS将线程ID写入对象头Mark Word，同一线程再次进入无需同步
2. **轻量级锁**：偏向锁被其他线程竞争时升级，通过CAS自旋获取锁
3. **重量级锁**：自旋超过阈值（默认10次或自适应）后升级，线程阻塞等待，依赖OS互斥量（mutex）

> 💡 **对象头Mark Word**是关键。32位JVM中Mark Word存储锁标志位：01（无锁/偏向锁）、00（轻量级锁）、10（重量级锁）、11（GC标记）。

### 12. volatile 关键字的作用和原理？
**作用**：
1. **保证可见性**：一个线程修改变量后，其他线程立即看到最新值
2. **禁止指令重排序**：内存屏障防止指令重排（经典的DCL单例中防止 `instance = new Singleton()` 的指令重排）

**原理**：volatile的读/写操作通过插入**内存屏障**（Memory Barrier）实现：
- 写volatile变量：在写之前插入StoreStore屏障，写之后插入StoreLoad屏障，强制将修改写入主存
- 读volatile变量：在读之后插入LoadLoad屏障和LoadStore屏障，强制从主存读取最新值

> ⚠️ volatile **不能保证原子性**！如 `count++` 这种复合操作仍需使用 `synchronized` 或 `AtomicInteger`。

### 13. 什么是CAS？原理是什么？有什么问题？
**CAS**（Compare And Swap，比较并交换）是一种无锁原子操作。三个操作数：**内存地址V**、**预期值A**、**新值B**。仅当V当前值为A时才更新为B，否则不更新，返回V当前值。

底层调用 `Unsafe` 类的 `compareAndSwapXXX` 方法，由CPU的 `cmpxchg` 指令保证原子性。

**三大问题**：
| 问题 | 说明 | 解决方案 |
|------|------|----------|
| ABA问题 | 值从A->B->A，CAS误判 | 加版本号：`AtomicStampedReference` |
| 自旋开销 | 高竞争下CPU空转 | 自适应自旋、分段锁 |
| 仅单变量 | 只能操作一个变量 | 封装为对象或用锁 |

### 14. ReentrantLock 和 synchronized 的区别？
| 维度 | synchronized | ReentrantLock |
|------|-------------|---------------|
| 实现层级 | JVM内置锁（C++实现） | JDK层面（Java实现） |
| 锁获取方式 | 自动获取/释放 | 手动 `lock()` / `unlock()` |
| 灵活性 | 低，结构固定 | 高，支持公平/非公平、可中断 |
| 公平性 | 非公平 | 可设置公平（`new ReentrantLock(true)`） |
| 条件变量 | 每个对象一个等待集 | 支持多个 `Condition` |
| 可中断 | 锁等待不可中断 | `lockInterruptibly()` 支持中断 |
| 性能 | 优化后与ReentrantLock相近 | 高竞争下略优 |

### 15. 什么是死锁？产生的必要条件？
**死锁**：两个或多个线程互相持有对方需要的资源，且不释放，导致全部永久阻塞。

**必要四个条件（缺一不可）**：
1. **互斥**：资源一次只能被一个线程占用
2. **持有并等待**：持有至少一个资源，同时等待其他资源
3. **不可剥夺**：已获得的资源不能被强制剥夺
4. **循环等待**：多个线程间形成循环等待链

> 🎯 **破解死锁**只需破坏上述任意一个条件，最常用的是破坏"循环等待"：规定资源获取顺序，所有线程按相同顺序获取锁。

### 16. ThreadLocal 的原理？内存泄漏如何解决？
**ThreadLocal** 为每个线程维护独立的变量副本。每个Thread内部维护一个 `ThreadLocalMap`，键为ThreadLocal弱引用，值为变量副本。

**内存泄漏原因**：ThreadLocal的key是弱引用（`WeakReference`），GC后key变为null，但value仍存在（强引用链：Thread -> ThreadLocalMap -> Entry -> value）。线程不结束则value无法回收。

**解决方案**：
- 使用完**必须调用 `remove()`** 清理Entry
- 最佳实践：在 `try-finally` 中确保 `remove()` 执行
- 使用框架（如Spring的 `RequestContextHolder`）时注意清理

### 17. 线程池的核心参数有哪些？
`ThreadPoolExecutor` 的7大核心参数：

| 参数 | 类型 | 含义 |
|------|------|------|
| `corePoolSize` | int | 核心线程数（常驻） |
| `maximumPoolSize` | int | 最大线程数 |
| `keepAliveTime` | long | 非核心线程空闲超时 |
| `unit` | TimeUnit | 时间单位 |
| `workQueue` | BlockingQueue<Runnable> | 任务阻塞队列 |
| `threadFactory` | ThreadFactory | 线程工厂 |
| `handler` | RejectedExecutionHandler | 拒绝策略 |

**执行流程**：核心线程 -> 任务队列 -> 最大线程 -> 拒绝策略

### 18. 线程池的拒绝策略有哪些？

| 策略 | 说明 | 适用场景 |
|------|------|----------|
| `AbortPolicy` | 直接抛 `RejectedExecutionException` | 需要感知超载的业务 |
| `CallerRunsPolicy` | 调用者线程直接执行任务 | 降级、流量控制 |
| `DiscardPolicy` | 直接丢弃，不抛异常 | 允许丢弃的任务 |
| `DiscardOldestPolicy` | 丢弃队列中最旧的任务，重新提交 | 消息推送等时效性强的场景 |

### 19. AQS是什么？核心原理？
**AQS**（AbstractQueuedSynchronizer）是JUC锁和同步器的**基础框架**。`ReentrantLock`、`Semaphore`、`CountDownLatch`、`ReentrantReadWriteLock` 等都基于AQS实现。

**核心原理**：
1. 一个 `volatile int state` 表示同步状态（ReentrantLock中表示重入次数，Semaphore中表示剩余许可数）
2. 一个 **CLH双向队列**（FIFO）管理等待线程
3. 提供模板方法 `tryAcquire()` / `tryRelease()` / `tryAcquireShared()` 等，由子类实现

> 💡 理解AQS = 理解JUC锁的底层。核心方法：`acquire(int arg)` = `tryAcquire` 尝试 + 失败则 `addWaiter` 入队 + `acquireQueued` 阻塞。

### 20. ConcurrentHashMap 实现原理？
**JDK 1.7**：`Segment` 数组 + `HashEntry` 数组，分段锁（继承ReentrantLock），默认16个Segment，支持16个线程并发写。

**JDK 1.8+**：放弃分段锁，采用 **数组 + 链表/红黑树** + **CAS + synchronized**：
- 初始化使用CAS
- 写操作：hash位置的Node为空则CAS插入；非空则 `synchronized` 锁定链表头节点
- 链表转红黑树：链表长度 >= 8 且数组长度 >= 64

> 🎯 **1.8 相比 1.7 的优势**：锁粒度更细（从Segment级别降到桶级别）；CAS减少锁开销；红黑树优化高冲突场景。

---

## 二、深度原理剖析（10-15题）

### 1. JMM（Java内存模型）详解
**JMM** 定义了Java线程与主存之间的抽象关系，规范了多线程下的可见性、有序性、原子性。

**核心结构**：
```
                    主存储器 (Main Memory)
                    ┌────────────────────┐
                    │ 共享变量 (堆对象)     │
                    │ 静态变量             │
                    │ 类信息               │
                    └────────┬───────────┘
           ┌─────────────────┼─────────────────┐
           │                 │                 │
    ┌──────▼──────┐  ┌──────▼──────┐  ┌──────▼──────┐
    │  工作内存    │  │  工作内存    │  │  工作内存    │
    │  (线程A)     │  │  (线程B)     │  │  (线程C)     │
    │  CPU缓存+    │  │  CPU缓存+    │  │  CPU缓存+    │
    │  寄存器      │  │  寄存器      │  │  寄存器      │
    └──────┬──────┘  └──────┬──────┘  └──────┬──────┘
           │                 │                 │
    ┌──────▼──────┐  ┌──────▼──────┐  ┌──────▼──────┐
    │  CPU核心1   │  │  CPU核心2   │  │  CPU核心3   │
    └─────────────┘  └─────────────┘  └─────────────┘
```

**JMM三大特性**：
- **原子性**：synchronized、Lock、Atomic类保证
- **可见性**：volatile、synchronized、final保证
- **有序性**：volatile（加屏障）、synchronized（加锁）、happens-before规则保证

**8种happens-before规则**（面试重点）：
1. 程序次序规则：单线程中代码按序执行
2. volatile规则：对volatile变量的写操作happens-before于后续的读操作
3. 锁规则：解锁happens-before于加锁
4. 传递性：A happens-before B, B happens-before C => A happens-before C
5. 线程启动规则：`start()` happens-before 线程中的任何动作
6. 线程终止规则：线程中任何动作 happens-before 其他线程检测到该线程终止
7. 中断规则：`interrupt()` happens-before 检测中断
8. 对象终结规则：构造函数结束 happens-before `finalize()` 开始

### 2. synchronized 锁升级全过程（偏向锁→轻量级锁→重量级锁）

```
偏向锁 → 轻量级锁(自旋锁) → 重量级锁（不可逆）
```

| 阶段 | 触发条件 | Mark Word内容 | 性能特征 |
|------|----------|--------------|----------|
| **无锁** | 初始状态 | 哈希码 + 分代年龄 + 01 | 无开销 |
| **偏向锁** | 单一线程连续获取锁 | 线程ID + 偏向时间戳 + epoch | 极低开销（一次CAS） |
| **轻量级锁** | 另一线程竞争偏向锁 | 栈帧中锁记录地址(LR指针) + 00 | CAS自旋等待 |
| **重量级锁** | 自旋次数超标/等待线程多 | Mutex指针 + 10 | 线程阻塞/唤醒（OS层面） |

**升级触发条件**：
- 偏向锁 → 轻量级锁：第二个线程尝试获取偏向锁且偏向模式已关闭或已偏向其他线程
- 轻量级锁 → 重量级锁：自旋超过阈值（默认10次，JDK 1.6+自适应）

> 💡 **锁粗化**：JVM将多个连续的相同锁的加解锁合并为一个（如循环内的synchronized）。
> **锁消除**：JVM逃逸分析发现对象不会被其他线程访问时，直接去掉同步。

### 3. AQS 源码级原理详解

AQS核心设计：

```java
// AQS的核心数据结构
public abstract class AbstractQueuedSynchronizer {
    // 同步状态（核心字段）
    private volatile int state;
    
    // CLH队列头尾
    private transient volatile Node head;
    private transient volatile Node tail;
    
    // Node节点（内部类）
    static final class Node {
        volatile int waitStatus;     // 等待状态：0/CANCELLED(1)/SIGNAL(-1)/CONDITION(-2)/PROPAGATE(-3)
        volatile Node prev;          // 前驱节点
        volatile Node next;          // 后继节点
        volatile Thread thread;      // 对应的线程
    }
}
```

**独占模式获取锁**（`acquire(int arg)`）：
```
acquire(arg)
  ├── tryAcquire(arg)         // 子类实现：尝试获取锁
  │    └── 成功 → 返回
  └── 失败 → addWaiter(Node.EXCLUSIVE)  // 创建node入队尾
       └── acquireQueued(node, arg)     // 循环park等待
            ├── 前驱是head && tryAcquire成功 → 设为新head，返回
            └── 否则 → shouldParkAfterFailedAcquire + park()
```

**独占模式释放锁**（`release(int arg)`）：
```
release(arg)
  ├── tryRelease(arg)         // 子类实现：释放锁
  │    └── 成功 → unparkSuccessor(h)  // 唤醒后继节点
  └── 失败 → 返回false
```

### 4. ReentrantLock 源码分析

**公平锁 vs 非公平锁的核心区别**：

非公平锁 `lock()`：
```java
// NonfairSync.lock()
final void lock() {
    if (compareAndSetState(0, 1))  // 先尝试直接抢锁（插队）
        setExclusiveOwnerThread(Thread.currentThread());
    else
        acquire(1);                // 失败才走AQS流程
}
```

公平锁 `lock()`：
```java
// FairSync.lock()
final void lock() {
    acquire(1);  // 直接走AQS流程，不插队
}

// FairSync.tryAcquire() 多了一个hasQueuedPredecessors()判断
protected final boolean tryAcquire(int acquires) {
    final Thread current = Thread.currentThread();
    int c = getState();
    if (c == 0) {
        if (!hasQueuedPredecessors() &&  // 检查队列中是否有更早等待的线程
            compareAndSetState(0, acquires)) {
            setExclusiveOwnerThread(current);
            return true;
        }
    }
    // ...
}
```

**可重入实现**：每次 `lock()` 前判断当前线程是否已经是持有线程，如果是则 `state++`；每次 `unlock()` 时 `state--`，直到state为0才真正释放锁。

### 5. 读写锁 ReentrantReadWriteLock 原理

维护两把锁：**读锁（共享锁）** 和 **写锁（独占锁）**。

**state高16位 = 读锁计数，低16位 = 写锁重入次数**：
```
state (32位)
┌─────────────────┬─────────────────┐
│ 读锁计数 (16位)   │ 写锁重入次数(16位)│
└─────────────────┴─────────────────┘
```

**锁规则**：
- 读读不互斥（多个线程可以同时读）
- 读写互斥（读时不能写，写时不能读）
- 写写互斥（同一时刻只有一个线程写）

**写锁降级**：持有写锁的线程可以再获取读锁（先写后读），然后释放写锁实现降级为读锁。这是读写锁支持的重要特性。

### 6. StampedLock 原理（JDK 8 新增）

比 `ReentrantReadWriteLock` 更优的读写锁，支持**乐观读**。

```java
class Point {
    private double x, y;
    private final StampedLock sl = new StampedLock();
    
    void move(double dx, double dy) {
        long stamp = sl.writeLock();
        try { x += dx; y += dy; } 
        finally { sl.unlockWrite(stamp); }
    }
    
    double distanceFromOrigin() {
        long stamp = sl.tryOptimisticRead();  // 乐观读：不阻塞，不加锁
        double curX = x, curY = y;
        if (!sl.validate(stamp)) {            // 检查是否有写操作发生
            stamp = sl.readLock();            // 验证失败 → 升级为悲观读锁
            try { curX = x; curY = y; } 
            finally { sl.unlockRead(stamp); }
        }
        return Math.sqrt(curX * curX + curY * curY);
    }
}
```

> 💡 **乐观读完全不加锁**，仅在验证时判断数据是否有效，读多写少场景性能远优于 `ReadWriteLock`。

### 7. CountDownLatch vs CyclicBarrier vs Semaphore

| 维度 | CountDownLatch | CyclicBarrier | Semaphore |
|------|---------------|---------------|-----------|
| **作用** | 一个线程等待多个线程完成 | 多个线程互相等待到同一屏障点 | 控制同时访问的线程数量 |
| **计数器** | 递减（await等待到0） | 递增（达到parties时触发） | 许可数（acquire减，release加） |
| **能否重用** | 不能（需重新new实例） | 能（调用reset()重置） | 能 |
| **触发动作** | 无 | 支持传入barrierAction（到达时执行） | 无 |
| **典型场景** | 等待N个服务启动完成 | 并行计算分片汇总 | 数据库连接池限流 |

**源码角度**：三者都基于AQS，CountDownLatch/闭锁用共享模式、CyclicBarrier用ReentrantLock + Condition。

### 8. CompletableFuture 异步编程详解

JDK 8引入，支持**链式调用**和**异步编排**。

**核心方法分类**：

| 类别 | 方法 | 说明 |
|------|------|------|
| 创建 | `supplyAsync()` / `runAsync()` | 异步执行有/无返回值任务 |
| 转换 | `thenApply()` / `thenApplyAsync()` | 对结果做同步/异步变换 |
| 消费 | `thenAccept()` | 消费结果，无返回值 |
| 组合 | `thenCompose()` | 扁平化组合两个Future |
| 合并 | `thenCombine()` | 合并两个Future的结果 |
| 异常处理 | `exceptionally()` / `handle()` | 异常恢复 / 正常+异常通用处理 |
| 多任务 | `allOf()` / `anyOf()` | 全部完成 / 任一完成 |

**示例**：
```java
CompletableFuture.supplyAsync(() -> queryUserInfo(userId), executor)
    .thenApplyAsync(user -> enrichWithScore(user), executor)
    .thenAccept(user -> cacheService.put(user.getId(), user))
    .exceptionally(ex -> {
        log.error("处理用户信息失败", ex);
        return null;
    });
```

> ⚠️ 不带 `Async` 后缀的方法默认使用ForkJoinPool.commonPool()，生产环境必须自定义线程池。

### 9. ForkJoinPool 分治线程池原理

**ForkJoinPool** 专为分治算法（Divide-and-Conquer）设计，核心是 **工作窃取（Work-Stealing）** 算法。

**工作窃取**：每个工作线程维护一个双端队列（Deque），自己的任务从队头取；空闲线程从其他线程队列的**队尾**偷任务执行，减少竞争。

**经典应用**：
```java
class SumTask extends RecursiveTask<Long> {
    static final int THRESHOLD = 1000;
    private long[] array;
    private int start, end;
    
    @Override
    protected Long compute() {
        if (end - start <= THRESHOLD) {
            long sum = 0;
            for (int i = start; i < end; i++) sum += array[i];
            return sum;
        }
        int mid = (start + end) / 2;
        SumTask left = new SumTask(array, start, mid);
        SumTask right = new SumTask(array, mid, end);
        left.fork();      // 异步执行子任务
        right.fork();
        return left.join() + right.join();
    }
}
```

> 💡 `Arrays.parallelSort()`、`Stream.parallel()` 等底层都使用ForkJoinPool。

### 10. LongAdder 原理（高并发原子累加器）

JDK 8引入，比 `AtomicLong` 在高并发下性能更优。

**设计思想**：**热点分离**（类似ConcurrentHashMap的分段锁思路）。

```
AtomicLong:          [base]         ← 所有线程CAS竞争同一个值
LongAdder:     [cells[0]] [cells[1]] ... [base]
                   ↑          ↑
              线程1      线程2
```

- 低竞争时直接CAS修改 `base`
- 高竞争时分配不同的 `Cell` 数组槽位，每个线程只CAS自己的槽位
- 最终求和时累加 `base + sum(cells)`

> 🎯 **选择建议**：追求最终一致且非频繁读取用 LongAdder；需要强一致实时值用 AtomicLong。

### 11. Thread.join() 的底层实现

```java
// Thread.join() 源码（简化版）
public final synchronized void join(long millis) throws InterruptedException {
    long base = System.currentTimeMillis();
    long now = 0;
    if (millis < 0) throw new IllegalArgumentException();
    if (millis == 0) {
        while (isAlive()) {
            wait(0);  // 当前线程无限等待，直到被notify
        }
    } else {
        while (isAlive()) {
            long delay = millis - now;
            if (delay <= 0) break;
            wait(delay);
            now = System.currentTimeMillis() - base;
        }
    }
}
```

> 💡 **关键点**：`join()` 本身是一个 `synchronized` 方法，内部通过 `wait()` 让调用线程等待；当目标线程执行完毕后，JVM会在线程退出时调用 `this.notifyAll()` 唤醒所有等待在该线程对象上的其他线程。

---

## 三、实战场景题（8-12题）

### 场景 1：高并发下缓存击穿 / 缓存更新并发问题
**问题描述**：热点缓存key过期瞬间，大量请求同时打穿到数据库，如何控制仅一个线程去加载缓存？

**解决方案**：
```java
// 布隆过滤器 + 分布式锁控制
public String getData(String key) {
    String cache = redis.get(key);
    if (cache != null) return cache;
    
    // 缓存未命中，加锁（防止缓存击穿）
    String lockKey = "lock:" + key;
    if (redis.setNx(lockKey, "1", 3, TimeUnit.SECONDS)) {
        try {
            cache = redis.get(key);  // 双重检查
            if (cache != null) return cache;
            
            String dbData = queryFromDB(key);  // 从数据库加载
            redis.setex(key, 300, dbData);
            return dbData;
        } finally {
            redis.del(lockKey);  // 释放锁
        }
    } else {
        // 其他线程等待重试
        Thread.sleep(50);
        return getData(key);  // 递归重试
    }
}
```

> 💡 **电商场景**：商品详情页缓存，QPS 10000+，必须防止缓存击穿。

### 场景 2：多线程批量处理百万级数据
**问题描述**：需要从Excel/数据库中读取100万条记录，逐条调用外部API处理后写入结果，如何提升处理效率？

**解决思路**：
```java
int batchSize = 1000;
int totalCount = 1000000;
int totalPages = (totalCount + batchSize - 1) / batchSize;
CountDownLatch latch = new CountDownLatch(totalPages);
ExecutorService executor = Executors.newFixedThreadPool(10);

for (int i = 0; i < totalPages; i++) {
    int page = i;
    executor.submit(() -> {
        try {
            List<Record> records = queryPage(page, batchSize);
            for (Record r : records) {
                Result result = callExternalApi(r);
                saveResult(result);
            }
        } finally {
            latch.countDown();
        }
    });
}
latch.await();  // 等待所有批次完成
executor.shutdown();
```

> ⚠️ **注意点**：100万数据内存会OOM，必须分页。外部API需要加断路器（Hystrix/Resilience4j）防止雪崩。

### 场景 3：接口限流（Rate Limiter）实现
**问题描述**：某核心接口每秒最多处理100个请求，如何实现限流？

**方案一：Semaphore 令牌桶**
```java
private final Semaphore semaphore = new Semaphore(100);

public void handleRequest(Request req) {
    if (!semaphore.tryAcquire(100, TimeUnit.MILLISECONDS)) {
        throw new RateLimitException("服务繁忙，请稍后重试");
    }
    try {
        process(req);
    } finally {
        semaphore.release();
    }
}
```

**方案二：RateLimiter（Guava）漏桶算法** — Semaphore不控制速率，只控制并发数。RateLimiter按时间窗口控制速率：
```java
private final RateLimiter limiter = RateLimiter.create(100.0); // 每秒100个

public void handleRequest(Request req) {
    double waitTime = limiter.acquire();  // 阻塞直到获取许可
    process(req);
}
```

> 💡 **阿里 Sentinel** 是生产级限流降级组件，支持QPS/线程数限流、熔断降级、系统自适应保护。

### 场景 4：多线程下的日志收集
**问题描述**：高并发系统，多个线程同时写日志文件，导致日志错乱。如何处理？

**解决思路**：
1. **使用 Logback/Log4j2 异步 Appender**：日志事件放入 ArrayBlockingQueue，后台线程批量写入
2. **ThreadLocal 追踪链路**：每个请求分配一个 TraceId，贯穿整个调用链

```java
// 使用 MDC（Mapped Diagnostic Context）传递 TraceId
public class TraceFilter implements Filter {
    private static final String TRACE_ID = "traceId";
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        MDC.put(TRACE_ID, traceId);    // MDC 底层是 ThreadLocal
        chain.doFilter(request, response);
        MDC.remove(TRACE_ID);          // 必须清理
    }
}
```

### 场景 5：多线程事务问题
**问题描述**：在一个事务方法中开启多个子线程并行处理，子线程异常时主事务能否回滚？

**答案**：**不能**。Spring事务默认绑定在当前线程的 `ThreadLocal` 中（`DataSourceTransactionManager`），子线程无法获取主线程的事务连接。子线程的异常只能回滚子线程自己的数据库操作，主线程无法感知。

**解决方案**：
1. **编程式事务**：手动控制子线程事务，通过 `CompletableFuture` + 结果聚合判断是否全局回滚
2. **最终一致性**：使用本地消息表 + MQ，异步补偿，放弃强一致性
3. **seata TCC**：分布式事务框架，try-confirm-cancel三段式

### 场景 6：CPU 100% 问题排查
**问题描述**：线上Java服务器CPU突然飙升到100%，如何排查？

**排查步骤**：
```
1. top -H -p <pid>              → 找到CPU最高的线程ID
2. printf "%x\n" <tid>          → 转16进制
3. jstack <pid> | grep <十六进制tid> -A 30  → 查看线程堆栈
   （或用 jstack <pid> > stack.log 导出后本地分析）
4. 分析堆栈中的类名、行号，定位问题代码
```

**常见原因**：
- 死循环（while(true) 未正确退出）
- 频繁GC（Full GC导致CPU高，用 `jstat -gcutil` 确认）
- 大对象频繁创建
- 锁竞争激烈（大量线程CAS自旋）

### 场景 7：线程池任务异常如何处理？
**问题描述**：`execute()` 和 `submit()` 提交任务，线程池中抛出异常后，线程会怎样？

**区别**：
| 提交方式 | 异常行为 | 后续影响 |
|----------|----------|----------|
| `execute(Runnable)` | 异常抛给当前线程的未捕获异常处理器 | 该线程销毁，线程池创建新线程替代 |
| `submit(Callable<?>)` | 异常被封装在 `Future` 中 | 线程不会被销毁，可复用 |

**处理方案**：
1. submit + Future.get() 捕获 ExecutionException
2. 自定义 `ThreadFactory.setUncaughtExceptionHandler`
3. 重写 `ThreadPoolExecutor.afterExecute()` 钩子方法
4. 任务内 try-catch 兜底

### 场景 8：ArrayList 线程安全问题
**问题描述**：多线程并发对同一个ArrayList进行add操作，会有什么后果？

**典型问题**：
- **数组越界**：容量检查与扩容非原子，多个线程同时触发扩容
- **元素覆盖**：多个线程同时写同一索引位置
- **null值**：扩容时内部数组新旧引用问题

**解决方案**：
| 替代方案 | 特点 | 适用场景 |
|----------|------|----------|
| `Vector` | 全方法synchronized | 不推荐，性能差 |
| `Collections.synchronizedList()` | 包装器同步 | 简单场景 |
| `CopyOnWriteArrayList` | 写时复制，读无锁 | **读多写极少**场景 |
| `ConcurrentLinkedDeque/Queue` | CAS无锁 | 队列操作场景 |

---

## 四、手写代码题（5-8题）

### 1. DCL单例模式（双重检查锁）

```java
public class Singleton {
    // volatile 防止指令重排序：new Singleton() 三步（分配内存、初始化、赋值）可能重排
    private static volatile Singleton instance;
    
    private Singleton() {}
    
    public static Singleton getInstance() {
        if (instance == null) {                     // 第一次检查（不加锁）
            synchronized (Singleton.class) {
                if (instance == null) {             // 第二次检查（加锁）
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```

> 💡 **为什么两次 check？** → 第一次避免不必要的加锁；第二次保证在被阻塞的线程中只存在一个实例。
> **为什么 volatile？** → 防止 `instance = new Singleton()` 的指令重排导致其他线程拿到半初始化对象。

### 2. 生产者消费者模式（BlockingQueue实现）

```java
public class ProducerConsumer {
    private static final int CAPACITY = 10;
    private static final BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(CAPACITY);
    
    static class Producer implements Runnable {
        @Override
        public void run() {
            try {
                for (int i = 0; i < 100; i++) {
                    queue.put(i);  // 队列满时自动阻塞
                    System.out.println(Thread.currentThread().getName() + " 生产: " + i);
                    Thread.sleep((long) (Math.random() * 100));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    static class Consumer implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    Integer val = queue.take();  // 队列空时自动阻塞
                    System.out.println(Thread.currentThread().getName() + " 消费: " + val);
                    Thread.sleep((long) (Math.random() * 200));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public static void main(String[] args) {
        new Thread(new Producer(), "生产者-1").start();
        new Thread(new Consumer(), "消费者-1").start();
        new Thread(new Consumer(), "消费者-2").start();
    }
}
```

### 3. 手写阻塞队列（Lock + Condition实现）

```java
public class MyBlockingQueue<T> {
    private final List<T> queue = new LinkedList<>();
    private final int capacity;
    private final Lock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();  // 队列非空条件
    private final Condition notFull = lock.newCondition();   // 队列未满条件
    
    public MyBlockingQueue(int capacity) {
        this.capacity = capacity;
    }
    
    public void put(T item) throws InterruptedException {
        lock.lock();
        try {
            while (queue.size() == capacity) {
                notFull.await();  // 队列满，等待消费者消费
            }
            queue.add(item);
            notEmpty.signal();   // 通知消费者：队列已非空
        } finally {
            lock.unlock();
        }
    }
    
    public T take() throws InterruptedException {
        lock.lock();
        try {
            while (queue.isEmpty()) {
                notEmpty.await();  // 队列空，等待生产者生产
            }
            T item = queue.remove(0);
            notFull.signal();     // 通知生产者：队列已未满
            return item;
        } finally {
            lock.unlock();
        }
    }
    
    public int size() {
        lock.lock();
        try { return queue.size(); } 
        finally { lock.unlock(); }
    }
}
```

### 4. 线程安全计数器（CAS实现 vs synchronized实现）

```java
// 方式一：CAS + AtomicInteger（推荐）
public class SafeCounterByCAS {
    private final AtomicInteger count = new AtomicInteger(0);
    
    public void increment() {
        count.incrementAndGet();
    }
    
    public int getCount() {
        return count.get();
    }
}

// 方式二：synchronized
public class SafeCounterBySync {
    private int count = 0;
    
    public synchronized void increment() {
        count++;
    }
    
    public synchronized int getCount() {
        return count;
    }
}

// 方式三：LongAdder（超高并发下最优）
public class SafeCounterByAdder {
    private final LongAdder count = new LongAdder();
    
    public void increment() {
        count.increment();
    }
    
    public int getCount() {
        return count.intValue();
    }
}
```

> 💡 **性能对比**（线程数16，每个线程递增100万次）：synchronized < AtomicInteger < LongAdder。LongAdder在高竞争下性能为AtomicInteger的数倍。

### 5. 手写死锁及排查

```java
public class DeadlockDemo {
    private static final Object lockA = new Object();
    private static final Object lockB = new Object();
    
    public static void main(String[] args) {
        new Thread(() -> {
            synchronized (lockA) {
                System.out.println("线程1获得锁A");
                try { Thread.sleep(100); } catch (InterruptedException e) {}
                synchronized (lockB) {
                    System.out.println("线程1获得锁B");
                }
            }
        }, "线程-1").start();
        
        new Thread(() -> {
            synchronized (lockB) {
                System.out.println("线程2获得锁B");
                try { Thread.sleep(100); } catch (InterruptedException e) {}
                synchronized (lockA) {
                    System.out.println("线程2获得锁A");
                }
            }
        }, "线程-2").start();
    }
}
```

**排查命令**：
```bash
jps                    # 找到Java进程ID
jstack <pid>           # 输出线程堆栈，死锁会直接提示 "Found one Java-level deadlock"
```

### 6. 手写线程池（简易版）

```java
public class SimpleThreadPool {
    private final int corePoolSize;
    private final List<Worker> workers = new ArrayList<>();
    private final BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();
    private volatile boolean running = true;
    
    public SimpleThreadPool(int corePoolSize) {
        this.corePoolSize = corePoolSize;
        for (int i = 0; i < corePoolSize; i++) {
            Worker worker = new Worker();
            workers.add(worker);
            worker.start();
        }
    }
    
    public void execute(Runnable task) {
        if (!running) throw new RejectedExecutionException("线程池已关闭");
        taskQueue.offer(task);
    }
    
    public void shutdown() {
        running = false;
        for (Worker worker : workers) {
            worker.interrupt();
        }
    }
    
    private class Worker extends Thread {
        @Override
        public void run() {
            while (running || !taskQueue.isEmpty()) {
                Runnable task = taskQueue.poll();  // 非阻塞拿任务
                if (task != null) {
                    task.run();
                } else {
                    Thread.yield();  // 无任务让出CPU
                }
            }
        }
    }
}
```

### 7. 交替打印 A-B-C（用 Lock + Condition 实现）

```java
public class AlternatePrint {
    private static final Lock lock = new ReentrantLock();
    private static int state = 0;  // 0:A, 1:B, 2:C
    
    static class Printer implements Runnable {
        private final int targetState;
        private final char ch;
        
        Printer(int targetState, char ch) {
            this.targetState = targetState;
            this.ch = ch;
        }
        
        @Override
        public void run() {
            for (int i = 0; i < 10; i++) {
                lock.lock();
                try {
                    while (state % 3 != targetState) {
                        // 自旋等待（面试简单实现）；生产中用 Condition.await
                    }
                    System.out.print(ch);
                    state++;
                } finally {
                    lock.unlock();
                }
            }
        }
    }
    
    public static void main(String[] args) {
        new Thread(new Printer(0, 'A')).start();
        new Thread(new Printer(1, 'B')).start();
        new Thread(new Printer(2, 'C')).start();
    }
}
```

---

## 五、系统设计题（3-5题）

### 1. 设计一个高性能缓存系统
**要求**：支持多线程并发读写、过期淘汰、高吞吐量。

**架构设计**：
```text
┌──────────────────────────────────────────────┐
│                  Cache API                     │
│   get(key)  put(key,val,ttl)  remove(key)    │
├──────────────────────────────────────────────┤
│               读写策略层                       │
│  读：读锁（共享）   写：写锁（互斥）           │
├──────────────────────────────────────────────┤
│               数据分片层                       │
│  Segment[0]  Segment[1]  ...  Segment[15]     │
│  (每片独立读写锁，提升并发度)                  │
├──────────────────────────────────────────────┤
│               存储与淘汰层                     │
│  ConcurrentHashMap + LinkedHashMap(LRU)       │
│  支持 LRU / TTL / 懒淘汰                      │
└──────────────────────────────────────────────┘
```

**关键设计点**：
1. 参考ConcurrentHashMap的分段思想，减小锁粒度
2. 使用 `ReadWriteLock` 实现读多写少场景高性能
3. 淘汰策略：`W-TinyLFU`（类似Caffeine）优于LRU，抗突发扫描
4. 避免热点key锁竞争：使用 **Striped Lock**（哈希到锁数组）

```java
public class HighConcurrencyCache<K, V> {
    private final ConcurrentHashMap<K, CacheEntry<V>> map = new ConcurrentHashMap<>();
    private final StripedLock stripedLock = new StripedLock(32);  // 32个锁槽位
    
    public V get(K key) {
        CacheEntry<V> entry = map.get(key);
        if (entry == null || entry.isExpired()) {
            return null;
        }
        return entry.getValue();
    }
    
    public void put(K key, V value, long ttlMs) {
        int lockIndex = Math.abs(key.hashCode() % 32);
        stripedLock.lock(lockIndex);
        try {
            map.put(key, new CacheEntry<>(value, System.currentTimeMillis() + ttlMs));
        } finally {
            stripedLock.unlock(lockIndex);
        }
    }
}
```

### 2. 设计一个任务调度系统
**要求**：支持定时任务、延迟任务、任务依赖、失败重试、大规模任务管理。

**核心组件**：
```text
┌───────────────────────────────────────────────┐
│                任务调度系统                      │
├───────────────────────────────────────────────┤
│   API层：submitTask() cancelTask() pause()    │
├───────────────────────────────────────────────┤
│   调度引擎层                                    │
│   ├── 时间轮（HashedWheelTimer）延迟执行         │
│   ├── Cron表达式解析 → 下次执行时间              │
│   └── DAG任务依赖管理（有向无环图）              │
├───────────────────────────────────────────────┤
│   执行引擎层                                    │
│   ├── 线程池执行任务                            │
│   ├── 失败阶梯重试（1s/5s/30s/5min/30min）      │
│   └── 超时中断（Future.get(timeout)）           │
├───────────────────────────────────────────────┤
│   存储层                                        │
│   └── 任务状态持久化（DB/Redis）                │
└───────────────────────────────────────────────┘
```

**时间轮原理**：
```
tick 1: [槽0] → [槽1] → [槽2] → ... → [槽N-1] → 回到槽0
          ↑                        ↑
     当前指针                   到期任务链表
```
- 每个刻度（tick）时间固定（如100ms）
- 任务根据延迟时间映射到对应的槽位
- 指针每tick前进一格，执行该槽位链表中的到期任务
- 时间复杂度：O(1) 添加，O(1) 执行

### 3. 设计一个高并发抢红包系统
**要求**：10万人抢1000个红包，不能多抢、不能少抢、保证性能。

**方案设计**：

```text
┌───────────────────────────────────────────────┐
│                抢红包系统                        │
├───────────────────────────────────────────────┤
│   预拆分阶段（发红包时完成）                    │
│   ├── 提前拆分为N份随机金额存入Redis List       │
│   └── 每个金额存入 String 结构                  │
├───────────────────────────────────────────────┤
│   抢红包阶段                                    │
│   ├── 布隆过滤器拦截非用户请求                  │
│   ├── Redis Lua脚本原子弹出（RPOP）             │
│   ├── 用户ID去重（Redis Set）                   │
│   └── 异步落库（MQ削峰填谷）                    │
├───────────────────────────────────────────────┤
│   关键逻辑                                        │
│   └── Lua脚本保证原子性：检查未抢过 → RPOP红包    │
│       → Set记录用户ID → 返回金额                 │
└───────────────────────────────────────────────┘
```

**Lua脚本核心**：
```lua
-- KEYS[1] = 红包列表, KEYS[2] = 抢到用户Set, ARGV[1] = 用户ID
if redis.call('sismember', KEYS[2], ARGV[1]) == 1 then
    return nil  -- 已抢过
end
local amount = redis.call('lpop', KEYS[1])
if amount then
    redis.call('sadd', KEYS[2], ARGV[1])
    return amount
end
return nil  -- 红包已抢完
```

### 4. 设计一个分布式锁
**要求**：基于Redis实现，支持重入、自动续期、防误删。

```java
public class RedisDistributedLock {
    private final StringRedisTemplate redis;
    private final String lockKey;
    private final String lockValue;  // UUID + 线程ID，用于防误删
    private final long leaseTime;    // 锁租约时间
    
    public boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < waitTime) {
            Boolean success = redis.opsForValue()
                .setIfAbsent(lockKey, lockValue, leaseTime, unit);
            if (Boolean.TRUE.equals(success)) {
                // 启动看门狗：自动续期（定时刷新过期时间）
                startWatchdog(leaseTime, unit);
                return true;
            }
            Thread.sleep(50);  // 自旋重试
        }
        return false;
    }
    
    public void unlock() {
        // Lua脚本：校验lockValue == 当前线程ID，相同才删除（防误删其他线程的锁）
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] " +
                        "then return redis.call('del', KEYS[1]) " +
                        "else return 0 end";
        redis.execute(script, List.of(lockKey), lockValue);
        stopWatchdog();
    }
}
```

> 💡 **生产建议**：使用 **Redisson** 框架，已封装看门狗、可重入、红锁等特性。Redisson的看门狗默认续期30秒。

---

## 六、常见坑点与最佳实践

| 坑点 | 原因 | 解决方案 |
|------|------|----------|
| `ArrayList` 并发add抛 `ArrayIndexOutOfBoundsException` | 扩容与写入非原子 | 使用 `CopyOnWriteArrayList` 或 `synchronizedList` |
| `HashMap` 并发put导致死循环（JDK 1.7） | 头插法在resize时形成环形链表 | 使用 `ConcurrentHashMap` |
| `SimpleDateFormat` 多线程解析异常 | 内部Calendar共享，多线程冲突 | 使用 `DateTimeFormatter`（线程安全） |
| `for` 循环内new线程 | 频繁创建销毁，性能差 | 使用线程池复用线程 |
| ThreadLocal 内存泄漏 | 未调用 `remove()`，value强引用无法回收 | try-finally 中强制 remove() |
| 线程池任务吞掉异常 | `submit()` 将异常封装在Future中 | `Future.get()` 捕获或重写 `afterExecute` |
| `double/long` 非volatile读写非原子 | 64位值分两次32位操作 | 加 `volatile` 或锁保护 |
| 父子线程无法共享 ThreadLocal 变量 | ThreadLocal作用域绑定当前线程 | 使用 `InheritableThreadLocal` 或显式传参 |
| `Integer` 自增不保证线程安全 | `i++` 是 read-modify-write 三步 | 使用 `AtomicInteger` |
| 线程池 `shutdown()` 未等待任务完成 | 立即返回，任务队列中的任务被丢弃 | 使用 `awaitTermination()` 等待 |
| 使用 `Executors.newCachedThreadPool()` 时任务过多 | 创建无限线程，导致OOM | 手动创建 `ThreadPoolExecutor` 指定参数 |
| 使用 `Executors.newFixedThreadPool()` 时 | 任务队列无界（`LinkedBlockingQueue` 默认 Integer.MAX_VALUE） | 手动创建 `ThreadPoolExecutor` 指定有界队列 |

---

## 七、面试回答模板

### 模板 1：synchronized 和 ReentrantLock 的区别

> **一句话总结**：synchronized是JVM内置锁，自动加解锁；ReentrantLock是JDK实现的显式锁，功能更丰富。

**展开说明**：synchronized由JVM通过monitorenter/monitorexit指令实现，会自动释放锁，从JDK 1.6开始有偏向锁、轻量级锁、重量级锁的升级过程。ReentrantLock基于AQS实现，需要手动lock/unlock，通常配合try-finally使用。

**举例**：在"等待可中断"场景，如果一个线程死锁了，ReentrantLock可以用 `lockInterruptibly()` 由另一个线程中断它；而synchronized一旦进入阻塞无法中断。另外ReentrantLock可以绑定多个Condition，比如阻塞队列中分别用notEmpty和notFull来区分通知生产者和消费者。

**引导追问**：关于ReentrantLock，面试官可能想深入AQS的原理（CLH队列、CAS、state状态），我也可以展开讲一下公平锁和非公平锁的核心区别。

### 模板 2：volatile 的原理

> **一句话总结**：volatile保证可见性和有序性，但不保证原子性。

**展开说明**：volatile通过内存屏障实现。写volatile变量时，JMM会在写前插入StoreStore屏障、写后插入StoreLoad屏障；读volatile变量时，读后插入LoadLoad和LoadStore屏障。这保证了volatile变量的修改能立即被其他线程看到，并且禁止了指令重排序。

**举例**：在DCL单例模式中，如果不加volatile，`instance = new Singleton()` 可能被重排为"分配内存→赋值（引用指向未初始化的内存）→初始化对象"，导致其他线程拿到半初始化对象。

**引导追问**：volatile不能替代锁的场景是 `count++` 这种复合操作，因为++是read-modify-write三步，volatile只保证每一步的可见性，不保证三步的原子性。这时候应该用AtomicInteger或synchronized。

### 模板 3：线程池的参数配置

> **一句话总结**：核心参数是corePoolSize、maximumPoolSize、workQueue和拒绝策略，配置取决于任务类型（CPU密集/IO密集）。

**展开说明**：CPU密集型任务配置 `core = CPU核心数 + 1`（防止页缺失）；IO密集型任务配置 `core = CPU核心数 * 2`（或更大，因为IO等待时释放CPU）。队列选择：有界队列防OOM，同步传递（SynchronousQueue）适合高吞吐低延迟场景。

**举例**：在阿里的Java规范中，强制禁止使用Executors创建线程池。比如 `Executors.newFixedThreadPool(10)` 底层用了无界的 `LinkedBlockingQueue`，高峰期任务积压可能导致OOM。

**引导追问**：子线程中异常如何处理？execute方式会抛异常（线程销毁重建），submit方式异常被封装，需通过Future.get()捕获。线程池的监控参数（activeCount、completedTaskCount、largestPoolSize）也很重要。

### 模板 4：ConcurrentHashMap 原理

> **一句话总结**：JDK 1.8的ConcurrentHashMap采用数组+链表+红黑树，CAS+synchronized实现高效并发。

**展开说明**：相比1.7的Segment分段锁，1.8取消了Segment，直接将锁粒度降到数组槽位。初始化使用CAS（`sizeCtl` 变量控制并发初始化），写操作时如果槽位为空就用CAS插入，非空则synchronized锁住链表头节点。

**举例**：当get一个key时，完全不加锁，利用volatile的可见性保证读到最新值。只有在修改（put/remove/replace）时才会加锁。此外，扩容机制支持多线程协助扩容（`transfer()` 方法），每个线程负责迁移一部分桶。

**引导追问**：扩容过程是如何保证并发安全的？通过 `ForwardingNode` 标记已迁移的桶，新操作遇到ForwardingNode就让出CPU或者协助扩容。

### 模板 5：线程状态转换

> **一句话总结**：Java线程有6种状态：NEW、RUNNABLE、BLOCKED、WAITING、TIMED_WAITING、TERMINATED。

**展开说明**：从生命周期看，new Thread后进入NEW；start()后进入RUNNABLE（包含就绪和运行中）；遇到synchronized未获得锁进入BLOCKED；调用wait()/join()/park()进入WAITING；调用sleep(ms)/wait(ms)/join(ms)进入TIMED_WAITING；run()执行完毕进入TERMINATED。

**举例**：一个常见的错误理解是"运行中的线程状态是RUNNING"，但实际上JVM层面没有RUNNING状态，RUNNABLE包含了"正在执行"和"等待CPU时间片"两个子状态。操作系统调度的就绪/运行区分在JVM层面被统一为RUNNABLE。

**引导追问**：线程从WAITING回到RUNNABLE是被notify唤醒后重新竞争锁，如果竞争失败会进入BLOCKED而不是直接RUNNABLE。BLOCKED和WAITING的区别是：BLOCKED是等待monitor锁入口，WAITING是已经持有锁但主动调用wait释放了锁。

---

## 八、快速查漏补缺 Checklist

### 必须能口述的原理
- [ ] JMM 内存模型（主存-工作内存、happens-before规则）
- [ ] synchronized 锁升级全过程（偏向→轻量→重量，Mark Word内容）
- [ ] volatile 内存屏障实现原理
- [ ] AQS 核心（state + CLH队列 + tryAcquire/Release模板方法）
- [ ] CAS 原理（Unsafe + CPU cmpxchg + ABA问题）
- [ ] ThreadLocal 原理（ThreadLocalMap + 弱引用 + 内存泄漏）
- [ ] ConcurrentHashMap 1.8 实现（CAS + synchronized + 红黑树）
- [ ] CompletableFuture 异步编排原理
- [ ] ForkJoinPool 工作窃取算法
- [ ] LongAdder 热点分离设计思想

### 必须能手写的代码
- [ ] DCL 单例（volatile + double check）
- [ ] 生产者消费者（BlockingQueue / wait-notify / Lock-Condition 三种方式）
- [ ] 阻塞队列（Lock + Condition 手写）
- [ ] 线程池执行流程伪代码
- [ ] 死锁代码 + jstack排查
- [ ] 交替打印（A-B-C / 奇偶数）

### 必须能区分的概念
- [ ] sleep vs wait
- [ ] Runnable vs Callable
- [ ] execute vs submit
- [ ] synchronized vs ReentrantLock vs ReentrantReadWriteLock
- [ ] ConcurrentHashMap 1.7 vs 1.8
- [ ] CountDownLatch vs CyclicBarrier vs Semaphore
- [ ] AtomicInteger vs LongAdder vs synchronized
- [ ] ThreadLocal vs InheritableThreadLocal vs TransmittableThreadLocal（阿里TTL）

### 大厂高频考题速查

| 公司 | 高频题 |
|------|--------|
| 阿里 | ConcurrentHashMap原理、线程池参数配置、ThreadLocal内存泄漏 |
| 腾讯 | synchronized锁升级、JMM内存模型、CAS ABA问题 |
| 字节 | CompletableFuture编排、多线程事务处理、CPU 100%排查 |
| 美团 | 读写锁应用场景、分布式锁设计、高并发缓存设计 |
| 蚂蚁 | AQS源码分析、ReentrantLock公平/非公平、LongAdder原理 |

---

> 🎯 **总结**：多线程面试底层原理（JMM、AQS、CAS）和实战应用（线程池配置、并发容器、异步编排）各占一半。建议将上述Checklist逐项过一遍，确保每个点都能用"一句话总结 + 展开说明 + 举例 + 引导追问"的四段式回答清楚。
