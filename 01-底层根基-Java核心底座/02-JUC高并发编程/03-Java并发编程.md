# 03 — Java 并发编程 (Concurrency Programming)

> **并发是 Java 最核心的能力，也是最有挑战的部分之一**

| 版本 | 关键变化 |
|------|---------|
| JDK 5 | JSR-166: `java.util.concurrent` 包引入 |
| JDK 6 | 偏向锁、锁优化大量引入 |
| JDK 7 | ForkJoinPool, Phaser |
| JDK 8 | CompletableFuture, StampedLock, LongAdder |
| JDK 9 | Reactive Streams, Flow API |
| JDK 11 | Epsilon, ZGC (支持并发) |
| JDK 15 | 禁用偏向锁 |
| JDK 19 | Virtual Threads (Preview) |
| JDK 21 | **Virtual Threads (Production)** — `java.lang.Thread` 虚拟化 |

---

## 目录

1. [线程基础与生命周期](#1-线程基础与生命周期)
2. [线程创建方式](#2-线程创建方式)
3. [线程池核心原理](#3-线程池核心原理)
4. [同步机制详解](#4-同步机制详解)
5. [AQS 抽象队列同步器](#5-aqs-抽象队列同步器)
6. [volatile 与原子类](#6-volatile-与原子类)
7. [并发容器](#7-并发容器)
8. [ThreadLocal 深度剖析](#8-threadlocal-深度剖析)
9. [CompletableFuture 异步编程](#9-completablefuture-异步编程)
10. [虚拟线程 (JDK 21+)](#10-虚拟线程-jdk-21)
11. [结构化并发](#11-结构化并发)
12. [并发模式与死锁](#12-并发模式与死锁)
13. [并发性能优化](#13-并发性能优化)
14. [面试经典问题](#14-面试经典问题)

---

## 1. 线程基础与生命周期

### 1.1 线程状态详解

```
                        ┌───────────────┐
                        │     NEW       │
                        │ (new Thread())│
                        └───────┬───────┘
                                │ start()
                                ▼
                        ┌───────────────┐
                  ┌────▶│  RUNNABLE     │◀────┐
                  │     │ (准备/运行)    │     │
                  │     └───────┬───────┘     │
  ┌───────────────┐            │              │
  │   BLOCKED     │◀── 未获得 ─┤              │
  │ (等待锁)      │   sync锁   │              │
  └───────────────┘            │              │
                               │ wait()       │ notify()
                  ┌────────────▼───────┐      │
                  │     WAITING        │──────┘
                  │ (Object.wait,      │
                  │  Thread.join,      │
                  │  LockSupport.park) │
                  └────────────┬───────┘
                               │
                  ┌────────────▼───────┐
                  │  TIMED_WAITING     │
                  │ (wait(ms), sleep,  │
                  │  parkNanos, join)  │
                  └────────────┬───────┘
                               │
                        ┌──────▼───────┐
                        │  TERMINATED  │
                        │ (执行完毕)    │
                        └──────────────┘
```

```java
/**
 * 线程状态演示
 */
public class ThreadStateDemo {
    public static void main(String[] args) throws Exception {
        // NEW
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        System.out.println("State after new: " + t.getState()); // NEW

        // RUNNABLE
        t.start();
        System.out.println("State after start: " + t.getState()); // RUNNABLE

        // TIMED_WAITING
        Thread.sleep(100);
        System.out.println("State during sleep: " + t.getState()); // TIMED_WAITING

        // TERMINATED
        t.join();
        System.out.println("State after join: " + t.getState()); // TERMINATED
    }
}
```

### 1.2 线程核心 API

| 方法 | 作用 | 释放锁？ | 恢复条件 |
|------|------|---------|---------|
| `Thread.sleep(millis)` | 当前线程休眠 | **不释放** | 时间到或被中断 |
| `Object.wait()` | 进入 WAITING | **释放**（必须持有锁） | notify/notifyAll |
| `Object.wait(timeout)` | 带超时等待 | **释放** | notify/超时/中断 |
| `Thread.join()` | 等待线程结束 | — | 目标线程终止 |
| `LockSupport.park()` | 暂停线程 | — | unpark/中断 |
| `yield()` | 让出 CPU（不可靠） | **不释放** | 调度器重新调度 |
| `t.interrupt()` | 设置中断标志 | — | — |

---

## 2. 线程创建方式

### 2.1 三种基础方式

```java
/**
 * 方式 1: 继承 Thread
 */
class WorkerThread extends Thread {
    @Override
    public void run() {
        System.out.println("Thread: " + getName() + " running");
    }
}
// 使用: new WorkerThread().start();

/**
 * 方式 2: 实现 Runnable（推荐）
 */
class TaskRunnable implements Runnable {
    @Override
    public void run() {
        System.out.println("Runnable task running");
    }
}
// 使用: new Thread(new TaskRunnable()).start();

/**
 * 方式 3: Callable + Future（可获取结果）
 */
class CallableTask implements Callable<String> {
    @Override
    public String call() throws Exception {
        Thread.sleep(1000);
        return "Task done at " + System.currentTimeMillis();
    }
}
// 使用:
// ExecutorService executor = Executors.newSingleThreadExecutor();
// Future<String> future = executor.submit(new CallableTask());
// String result = future.get(); // 阻塞
```

### 2.2 Callable + Future 实战

```java
public class CallableFutureDemo {
    public static void main(String[] args) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        List<Future<Integer>> futures = new ArrayList<>();

        // 提交多个任务
        for (int i = 0; i < 5; i++) {
            int taskId = i;
            Future<Integer> future = executor.submit(() -> {
                Thread.sleep(500);
                return taskId * 10;
            });
            futures.add(future);
        }

        // 获取结果
        for (Future<Integer> future : futures) {
            try {
                // get() 阻塞等待结果
                Integer result = future.get(1, TimeUnit.SECONDS);
                System.out.println("Result: " + result);
            } catch (TimeoutException e) {
                System.out.println("Task timed out, cancelling...");
                future.cancel(true); // 强制取消
            }
        }

        executor.shutdown();
    }
}
```

---

## 3. 线程池核心原理

### 3.1 ThreadPoolExecutor 构造参数

```java
/**
 * ThreadPoolExecutor 完整的 7 个参数
 */
public ThreadPoolExecutor(
    int corePoolSize,          // 核心线程数（一直存活，除非 allowCoreThreadTimeOut）
    int maximumPoolSize,       // 最大线程数
    long keepAliveTime,        // 非核心线程空闲存活时间
    TimeUnit unit,             // keepAliveTime 的单位
    BlockingQueue<Runnable> workQueue,  // 任务队列
    ThreadFactory threadFactory,        // 线程工厂（可自定义名称）
    RejectedExecutionHandler handler    // 拒绝策略
);
```

### 3.2 线程池运行流程

```
提交任务
    │
    ▼
┌───────────────────┐
│ 是否 < corePool?  │───是──▶ 创建新线程执行
└────────┬──────────┘
         │ 否
         ▼
┌───────────────────┐
│ 队列是否满?       │───否──▶ 放入队列等待
└────────┬──────────┘
         │ 是
         ▼
┌───────────────────┐
│ 是否 < maxPool?   │───是──▶ 创建新线程执行
└────────┬──────────┘
         │ 否
         ▼
┌───────────────────┐
│ 执行拒绝策略      │
└───────────────────┘
```

### 3.3 拒绝策略 (RejectedExecutionHandler)

| 策略 | 行为 | 使用场景 |
|------|------|---------|
| `AbortPolicy` (默认) | 抛出 `RejectedExecutionException` | 需要明确知道任务被拒 |
| `CallerRunsPolicy` | 提交任务的线程自己执行 | 降低提交速度，压控 |
| `DiscardPolicy` | 静默丢弃 | 不重要的任务 |
| `DiscardOldestPolicy` | 丢弃队列中最老的任务 | 允许任务过期 |
| 自定义 | 实现接口 | 记录日志、存入数据库等 |

### 3.4 常见线程池

```java
public class ThreadPoolFactoryDemo {

    public static void main(String[] args) {
        // 1. FixedThreadPool: 固定大小，无界队列
        //    适用：负载均衡，资源可控
        ExecutorService fixed = Executors.newFixedThreadPool(4);
        // 内部：core=max=N, LinkedBlockingQueue (无界)

        // 2. CachedThreadPool: 弹性大小，SynchronousQueue
        //    适用：大量短生命周期任务
        ExecutorService cached = Executors.newCachedThreadPool();
        // 内部：core=0, max=Integer.MAX_VALUE, keepAlive=60s
        // SynchronousQueue (直接交付)

        // 3. SingleThreadExecutor: 单线程，保证顺序执行
        ExecutorService single = Executors.newSingleThreadExecutor();
        // 内部：core=max=1, LinkedBlockingQueue (无界)

        // 4. ScheduledThreadPool: 定时/延迟执行
        ScheduledExecutorService scheduled = Executors.newScheduledThreadPool(2);
        scheduled.schedule(() -> System.out.println("Delayed"), 1, TimeUnit.SECONDS);
        scheduled.scheduleAtFixedRate(() -> System.out.println("Fixed rate"),
                0, 1, TimeUnit.SECONDS);
        scheduled.scheduleWithFixedDelay(() -> System.out.println("Fixed delay"),
                0, 1, TimeUnit.SECONDS);

        // 重要：Executors 创建的默认线程池可能有问题
        // - FixedThreadPool: 无界队列可能导致 OOM
        // - CachedThreadPool: 最大线程数无限，可能导致 OOM
        // 建议：直接使用 ThreadPoolExecutor 或 new ThreadPoolBuilder()
    }
}
```

### 3.5 最佳实践：自定义线程池

```java
/**
 * 线程池最佳实践
 */
public class ThreadPoolBestPractice {
    // 1. 自定义 ThreadFactory：命名线程便于排查
    private static final ThreadFactory NAMED_THREAD_FACTORY =
            new ThreadFactoryBuilder()          // Guava
                    .setNameFormat("my-pool-%d")
                    .setDaemon(true)
                    .build();

    // 2. 自定义拒绝策略
    private static final RejectedExecutionHandler REJECT_HANDLER =
            (r, executor) -> {
                // 记录日志 + 重试或持久化
                System.err.println("Task rejected: " + r.toString());
                // 或者使用 MQ 发送到其他服务
            };

    // 3. 创建线程池
    private static final ThreadPoolExecutor EXECUTOR =
            new ThreadPoolExecutor(
                    4,                          // core
                    8,                          // max
                    60, TimeUnit.SECONDS,       // keepAlive
                    new ArrayBlockingQueue<>(1000), // 有界队列！
                    NAMED_THREAD_FACTORY,
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );

    // 4. 监控线程池
    public static void monitor() {
        System.out.printf(
                "Active: %d/%d, Queue: %d/%d, Completed: %d%n",
                EXECUTOR.getActiveCount(),
                EXECUTOR.getPoolSize(),
                EXECUTOR.getQueue().size(),
                EXECUTOR.getQueue().remainingCapacity(),
                EXECUTOR.getCompletedTaskCount()
        );
    }
}
```

### 3.6 线程池大小估算

```bash
# CPU 密集型：N_cpu + 1  (或 N_cpu * 2)
# IO 密集型：N_cpu * (1 + wait_time / compute_time)
#
# 示例：8 核 CPU，IO 等待 100ms，计算 10ms
# 8 * (1 + 100/10) = 8 * 11 = 88 线程
```

---

## 4. 同步机制详解

### 4.1 synchronized 关键字

```java
/**
 * synchronized 用法
 */
public class SynchronizedDemo {
    private int count = 0;
    private final Object lock = new Object();

    // 1. 同步实例方法 — 锁的是 this
    public synchronized void instanceMethod() {
        count++; // this 锁
    }

    // 2. 同步静态方法 — 锁的是 Class 对象
    public static synchronized void staticMethod() {
        // 锁: SynchronizedDemo.class
    }

    // 3. 同步代码块 — 指定锁对象
    public void blockMethod() {
        synchronized (lock) {
            count++;
        }
    }

    // JDK 16+: 终于有完整验证
    public synchronized int getCount() { return count; }
}
```

#### 锁升级过程 (JDK 6+)

```
无锁 → 偏向锁 → 轻量级锁 → 重量级锁 (只升不降)

对象头 Mark Word 的锁标志位变化：

无锁 (01):    | unused:25 | hash:31 | age:4 | biased:0 | 01 |
                     ↓ 第一次被线程获取
偏向锁 (01):  | thread:54 | epoch:2 | age:4 | biased:1 | 01 |
                     ↓ 发生竞争（另一个线程尝试获取）
轻量级锁 (00): |              LockRecord 指针 (62)          | 00 |
                     ↓ 自旋失败（或自旋到阈值）
重量级锁 (10): |              Monitor 指针 (62)             | 10 |
```

```java
/**
 * 锁升级观测示例（查看 Mark Word）
 * JVM 参数: -XX:+UseBiasedLocking (JDK 8 默认，JDK 15 移除)
 */
public class LockUpgradeDemo {
    public static void main(String[] args) throws Exception {
        Object obj = new Object();

        // 1. 无锁状态: 001
        System.out.println("未加锁: " + ClassLayout.parseInstance(obj).toPrintable());

        synchronized (obj) {
            // 2. 偏向锁 (第一次获取): 101
            System.out.println("偏向锁: " + ClassLayout.parseInstance(obj).toPrintable());
        }

        // 等待偏向锁撤销
        Thread.sleep(5000);

        synchronized (obj) {
            // 3. 轻量级锁 (竞争): 000
            System.out.println("轻量锁: " + ClassLayout.parseInstance(obj).toPrintable());
        }
    }
}
```

### 4.2 Lock 接口与 ReentrantLock

```java
/**
 * ReentrantLock vs synchronized
 */
public class ReentrantLockDemo {
    private final ReentrantLock lock = new ReentrantLock(true); // fair = true
    private int count = 0;

    public void increment() {
        lock.lock();  // 获取锁
        try {
            count++;
        } finally {
            lock.unlock();  // 必须 finally 释放
        }
    }

    // 可中断：lockInterruptibly()
    public void interruptibleLock() throws InterruptedException {
        // 可以响应中断，避免死锁
        lock.lockInterruptibly();
        try {
            // 业务逻辑
        } finally {
            lock.unlock();
        }
    }

    // 尝试获取锁：tryLock()
    public boolean tryLockDemo() {
        boolean acquired = false;
        try {
            acquired = lock.tryLock(1, TimeUnit.SECONDS);
            if (acquired) {
                // 获取成功
                return true;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (acquired) {
                lock.unlock();
            }
        }
        return false;
    }

    // Condition：类似 Object.wait/notify
    private final Condition notFull  = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();
    private final Object[] items = new Object[100];
    private int putIdx, takeIdx, count;

    public void put(Object x) throws InterruptedException {
        lock.lock();
        try {
            while (count == items.length) {
                notFull.await();    // 类似 wait()
            }
            items[putIdx] = x;
            if (++putIdx == items.length) putIdx = 0;
            count++;
            notEmpty.signal();      // 类似 notify()
        } finally {
            lock.unlock();
        }
    }
}
```

| 特性 | synchronized | ReentrantLock |
|------|-------------|---------------|
| 隐式/显式 | 隐式（自动释放） | 显式（必须 finally unlock） |
| 公平性 | 非公平 | 可公平/非公平 |
| 可中断 | 否（不可响应中断） | 是（lockInterruptibly） |
| 超时 | 否 | 是（tryLock） |
| 多个条件 | 一个条件（wait/notify） | 多个 Condition |
| 性能 | JDK 6+ 优化后与 Lock 接近 | 竞争高时略优 |
| 调试 | 简单 | 较复杂 |

### 4.3 ReadWriteLock / StampedLock

```java
/**
 * ReadWriteLock: 读读不互斥，读写/写写互斥
 */
public class ReadWriteLockDemo {
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock r = rwLock.readLock();
    private final Lock w = rwLock.writeLock();
    private Map<String, String> cache = new HashMap<>();

    public String get(String key) {
        r.lock();   // 读锁——可多个线程同时获得
        try {
            return cache.get(key);
        } finally {
            r.unlock();
        }
    }

    public void put(String key, String value) {
        w.lock();   // 写锁——排他
        try {
            cache.put(key, value);
        } finally {
            w.unlock();
        }
    }
}

/**
 * StampedLock (JDK 8+)：更高效的乐观读
 */
public class StampedLockDemo {
    private final StampedLock lock = new StampedLock();
    private double x, y;

    public double read() {
        // 1. 尝试乐观读（不阻塞）
        long stamp = lock.tryOptimisticRead();
        double currentX = x;
        double currentY = y;

        // 2. 检查是否被写线程修改
        if (!lock.validate(stamp)) {
            // 乐观读失败，升级为悲观读锁
            stamp = lock.readLock();
            try {
                currentX = x;
                currentY = y;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return Math.sqrt(currentX * currentX + currentY * currentY);
    }

    public void write(double newX, double newY) {
        long stamp = lock.writeLock();
        try {
            x = newX;
            y = newY;
        } finally {
            lock.unlockWrite(stamp);
        }
    }
}
```

### 4.4 CountDownLatch / CyclicBarrier / Semaphore

```java
/**
 * CountDownLatch: 一个线程等待多个线程完成
 * 不可重用
 */
public class CountDownLatchDemo {
    public static void main(String[] args) throws Exception {
        // 计数器初始化为 3
        CountDownLatch latch = new CountDownLatch(3);

        Runnable worker = () -> {
            try {
                Thread.sleep((long) (Math.random() * 1000));
                System.out.println(Thread.currentThread().getName() + " done");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown(); // 计数器减 1
            }
        };

        // 启动 3 个工作线程
        for (int i = 0; i < 3; i++) {
            new Thread(worker).start();
        }

        // 主线程等待所有工作完成
        latch.await();
        System.out.println("All workers done, main continues");
    }
}

/**
 * CyclicBarrier: 多个线程相互等待到某一点
 * 可重用（reset）
 */
public class CyclicBarrierDemo {
    public static void main(String[] args) {
        int threadCount = 3;
        CyclicBarrier barrier = new CyclicBarrier(threadCount,
                () -> System.out.println("=== All arrived, let's go! ==="));

        Runnable runner = () -> {
            try {
                System.out.println(Thread.currentThread().getName() + " running...");
                Thread.sleep((long) (Math.random() * 1000));
                System.out.println(Thread.currentThread().getName() + " arrived at barrier");
                barrier.await(); // 等待其他线程
                System.out.println(Thread.currentThread().getName() + " passed barrier");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        };

        for (int i = 0; i < threadCount; i++) {
            new Thread(runner).start();
        }
    }
}

/**
 * Semaphore: 信号量，控制并发访问数量
 */
public class SemaphoreDemo {
    // 限流：最多 3 个线程同时访问
    private static final Semaphore SEMAPHORE = new Semaphore(3);

    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            int userId = i;
            new Thread(() -> {
                try {
                    SEMAPHORE.acquire(); // 获取许可
                    System.out.println("User " + userId + " accessing, available: "
                            + SEMAPHORE.availablePermits());
                    Thread.sleep(1000); // 模拟处理
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    SEMAPHORE.release(); // 释放许可
                }
            }).start();
        }
    }
}
```

| 工具 | 作用 | 可重用 | 使用场景 |
|------|------|--------|---------|
| CountDownLatch | 等待 N 个操作完成 | 否 | 并行初始化、等待服务启动 |
| CyclicBarrier | N 个线程到达同步点 | 是 (reset) | 分阶段并行计算 |
| Semaphore | 控制并发线程数 | 是 | 限流、连接池 |
| Exchanger | 两线程交换数据 | 是 | 生产者-消费者交换缓冲区 |
| Phaser (JDK 7) | 分阶段同步屏障 | 是（可动态增减） | 多阶段并行任务 |

---

## 5. AQS 抽象队列同步器

### 5.1 AQS 架构

```
AQS (AbstractQueuedSynchronizer)
  ┌─────────────────────────────────────────────┐
  │  state (volatile int)                       │  ← 同步状态
  │    0 = 未锁定, 1 = 锁定, N = 重入次数      │
  ├─────────────────────────────────────────────┤
  │  CLH 等待队列 (双向链表)                      │  ← 等待获取锁的线程
  │  ┌──────┐   ┌──────┐   ┌──────┐             │
  │  │ Node │◀─▶│ Node │◀─▶│ Node │             │
  │  │ prev │   │ prev │   │ prev │             │
  │  │ next │   │ next │   │ next │             │
  │  │ thead│   │ thead│   │ thead│             │
  │  │waitSt│   │waitSt│   │waitSt│             │
  │  └──────┘   └──────┘   └──────┘             │
  ├─────────────────────────────────────────────┤
  │  Condition 条件队列 (ConditionObject)         │  ← await/signal
  │  ┌──────┐   ┌──────┐                         │
  │  │ Node │◀─▶│ Node │                         │
  │  │ Cond │   │ Cond │                         │
  │  └──────┘   └──────┘                         │
  └─────────────────────────────────────────────┘
```

### 5.2 AQS 的两种模式

```java
// 独占模式 (ReentrantLock)
class Mutex {
    private static class Sync extends AbstractQueuedSynchronizer {
        @Override
        protected boolean tryAcquire(int acquires) {
            // CAS 设置 state 从 0 → 1
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        @Override
        protected boolean tryRelease(int releases) {
            if (!isHeldExclusively()) throw new IllegalMonitorStateException();
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        @Override
        protected boolean isHeldExclusively() {
            return getExclusiveOwnerThread() == Thread.currentThread();
        }
    }

    private final Sync sync = new Sync();
    public void lock() { sync.acquire(1); }
    public void unlock() { sync.release(1); }
}

// 共享模式 (Semaphore, CountDownLatch)
// tryAcquireShared: 返回负数(失败)，0(成功但无剩余)，正数(成功且有剩余)
// tryReleaseShared: 释放共享状态，需要 CAS 循环
```

### 5.3 AQS 的 CLH 锁节点变体

```
等待队列节点 (Node) 的状态 (waitStatus)：
  CANCELLED = 1   — 线程被取消（中断或超时）
  SIGNAL    = -1  — 后继节点需要被唤醒
  CONDITION = -2  — 在条件队列中等待
  PROPAGATE = -3  — (共享锁) 传播唤醒
  0         — 初始状态
```

```java
/**
 * acquire 方法流程 (ReentrantLock.lock() 的底层)
 *
 * 1. tryAcquire — 尝试获取锁（快速路径）
 *    ├─ 成功 → 直接返回
 *    └─ 失败 → 2
 *
 * 2. addWaiter — 创建等待节点加入队列尾
 *
 * 3. acquireQueued — 循环自旋
 *    ├─ 前驱是头节点 & tryAcquire 成功 → 设自己为头节点
 *    ├─ 否则 → 判断是否应该 park (阻塞)
 *    └─ 阻塞后等待前驱唤醒
 *
 * 4. 被中断 → selfInterrupt()
 */
```

---

## 6. volatile 与原子类

### 6.1 volatile 详解

```java
/**
 * volatile 的语义：
 * 1. 可见性：写操作立即对其他线程可见
 * 2. 有序性：禁止指令重排序
 * 3. ❌ 不保证原子性（i++ 需要额外同步）
 */
public class VolatileDemo {
    private volatile boolean running = true;
    private volatile static int counter = 0;

    public void run() {
        // volatile 保证 running 的修改对当前线程可见
        while (running) {
            // 处理业务...
        }
        System.out.println("Stopped");
    }

    public void stop() {
        running = false;  // volatile 写
    }

    // volatile 不保证原子性！
    public static void increment() {
        counter++; // 等同于: temp = counter; counter = temp + 1;
        // 两个线程同时执行时可能丢失更新
    }
}
```

### 6.2 内存屏障与 volatile

```bash
# volatile 写插入的内存屏障：
[StoreStore]  ← 禁止前面的普通读写重排到此屏障之后
volatile 写操作
[StoreLoad]   ← 禁止 volatile 写重排到后面的 volatile 读之前

# volatile 读插入的内存屏障：
volatile 读操作
[LoadLoad]    ← 禁止后面的普通读重排到此屏障之前
[LoadStore]   ← 禁止后面的普通写重排到此屏障之前
```

### 6.3 Atomic 类族

```java
/**
 * 原子类分类
 *
 * 1. 基本类型：AtomicBoolean, AtomicInteger, AtomicLong
 * 2. 引用类型：AtomicReference, AtomicStampedReference, AtomicMarkableReference
 * 3. 数组类：AtomicIntegerArray, AtomicLongArray, AtomicReferenceArray
 * 4. 字段更新器：AtomicIntegerFieldUpdater, AtomicReferenceFieldUpdater
 * 5. 累加器 (JDK 8+)：LongAdder, DoubleAdder, LongAccumulator
 */
public class AtomicDemo {
    // 基本原子类
    private final AtomicInteger count = new AtomicInteger(0);

    public void increment() {
        // CAS 操作 (Compare-And-Swap)
        count.incrementAndGet();
        // getAndIncrement, getAndAdd, compareAndSet(expected, update)
    }

    public int getCount() {
        return count.get();
    }

    // LongAdder: 比 AtomicLong 更适合高并发计数
    // 分段设计，减少 CAS 竞争
    private final LongAdder highConcurrencyCounter = new LongAdder();

    public void highConcurrencyIncrement() {
        highConcurrencyCounter.increment();
    }

    // ABA 问题演示
    public static class ABADemo {
        // AtomicStampedReference 通过版本号解决 ABA
        private final AtomicStampedReference<String> ref =
                new AtomicStampedReference<>("A", 0);

        public void demonstrateABA() {
            int[] stamp = new int[1];
            String value = ref.get(stamp);
            int oldStamp = stamp[0];

            // A → B → A（其他线程修改后又改回来）
            // 带版本号：A(0) → B(1) → A(2)，可以识别出变化
            boolean success = ref.compareAndSet("A", "C", oldStamp, oldStamp + 1);
        }
    }
}
```

---

## 7. 并发容器

### 7.1 ConcurrentHashMap — JDK 8 实现

#### 核心数据结构

```
JDK 8 ConcurrentHashMap:
┌────┬────┬────┬────┬────┬────┬────┐
│    │ N1 │    │ N2 │    │ N3 │    │  ← Node<K,V>[]
├────┼────┼────┼────┼────┼────┼────┤
│    │ K→ │    │ K→ │    │ K→ │    │
│    │ V→ │    │ V→ │    │ V→ │    │
│    │    │    │ ▓  │    │ ▓  │    │  ← 红黑树 (>= 8)
│    │    │    │ ▓  │    │ ▓  │    │
│    └────┘    └────┘    └────┘    │
└─────────────────────────────────┘

关键字段：
- sizeCtl: -1 (正在初始化), -N (N-1 个线程在扩容)
          >0 (阈值 = capacity * 0.75)
- transferIndex: 扩容时迁移索引
- counterCells: 高并发计数的分段计数器
```

#### put() 流程

```java
// 简化版 put 流程（JDK 8 ConcurrentHashMap）
final V putVal(K key, V value, boolean onlyIfAbsent) {
    if (key == null || value == null) throw new NullPointerException();
    int hash = spread(key.hashCode());  // (h ^ (h >>> 16)) & HASH_BITS

    for (Node<K,V>[] tab = table;;) {
        Node<K,V> f; int n, i, fh;
        if (tab == null || (n = tab.length) == 0)
            tab = initTable();                       // 1. 延迟初始化

        else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
            // 2. 空桶 → CAS 放入
            if (casTabAt(tab, i, null, new Node<K,V>(hash, key, value, null)))
                break;
        }
        else if ((fh = f.hash) == MOVED)
            tab = helpTransfer(tab, f);              // 3. 正在扩容 → 帮忙迁移

        else {
            V oldVal = null;
            synchronized (f) {                       // 4. 锁定桶的头节点（细粒度）
                if (tabAt(tab, i) == f) {
                    if (fh >= 0) {
                        // 链表 → 遍历插入
                        binCount = 1;
                        for (Node<K,V> e = f;; ++binCount) {
                            if (e.hash == hash && ((ek = e.key) == key || ek.equals(key))) {
                                oldVal = e.val;  // 找到，替换
                                break;
                            }
                            Node<K,V> pred = e;
                            if ((e = e.next) == null) {
                                pred.next = new Node<K,V>(hash, key, value, null); // 尾部插入
                                break;
                            }
                        }
                    } else if (f instanceof TreeBin) {
                        // 红黑树 → 树插入
                        ...
                    }
                }
            }
            if (binCount != 0) {
                if (binCount >= TREEIFY_THRESHOLD)
                    treeifyBin(tab, i);              // 5. >=8 → 转红黑树（需容量≥64）
                ...
            }
        }
    }
    addCount(1L, binCount);                          // 6. 计数（分段计数器）
    return null;
}
```

#### get() 流程

```java
// get() 不需要加锁！
public V get(Object key) {
    Node<K,V>[] tab; Node<K,V> e, p; int n, eh; K ek;
    int h = spread(key.hashCode());
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (e = tabAt(tab, (n - 1) & h)) != null) {
        if ((eh = e.hash) == h) {
            if ((ek = e.key) == key || (ek != null && key.equals(ek)))
                return e.val;
        }
        // 红黑树或下一个节点
        else if (eh < 0)
            return (p = e.find(h, key)) != null ? p.val : null;
        while ((e = e.next) != null) {
            if (e.hash == h && ((ek = e.key) == key || key.equals(ek)))
                return e.val;
        }
    }
    return null;
}
```

### 7.2 CopyOnWriteArrayList

```java
/**
 * CopyOnWriteArrayList：写时复制
 *
 * 读：不加锁（直接读数组）
 * 写：加锁 + 复制新数组
 * 适用：读多写少的场景
 */
public class CopyOnWriteDemo {
    // 监听器列表：读多写少
    private final CopyOnWriteArrayList<Listener> listeners =
            new CopyOnWriteArrayList<>();

    public void addListener(Listener listener) {
        listeners.add(listener); // 内部复制数组
    }

    public void fireEvent(Event event) {
        // 不需要加锁，迭代的是快照
        for (Listener listener : listeners) {
            listener.onEvent(event);
        }
    }

    // add 的底层逻辑（简化版）：
    // public boolean add(E e) {
    //     final ReentrantLock lock = this.lock;
    //     lock.lock();
    //     try {
    //         Object[] elements = getArray();
    //         int len = elements.length;
    //         Object[] newElements = Arrays.copyOf(elements, len + 1);
    //         newElements[len] = e;
    //         setArray(newElements); // volatile 写
    //         return true;
    //     } finally {
    //         lock.unlock();
    //     }
    // }
}
```

### 7.3 BlockingQueue 实现

```java
/**
 * BlockingQueue 实现类对比
 */
public class BlockingQueueDemo {
    public static void main(String[] args) throws Exception {
        // 1. ArrayBlockingQueue: 有界，数组，公平/非公平
        BlockingQueue<String> arrayQueue = new ArrayBlockingQueue<>(100, true);

        // 2. LinkedBlockingQueue: 可选有界，链表（默认无界）
        BlockingQueue<String> linkedQueue = new LinkedBlockingQueue<>(100);

        // 3. PriorityBlockingQueue: 无界，优先级队列
        BlockingQueue<Task> priorityQueue = new PriorityBlockingQueue<>(11,
                Comparator.comparingInt(Task::getPriority));

        // 4. DelayQueue: 延迟队列（元素必须实现 Delayed）
        DelayQueue<DelayedTask> delayQueue = new DelayQueue<>();

        // 5. SynchronousQueue: 容量为 0，直接交付
        BlockingQueue<String> syncQueue = new SynchronousQueue<>();
        // put() 会阻塞直到有线程 take()

        // 6. LinkedTransferQueue (JDK 7): 传递队列
        LinkedTransferQueue<String> transferQueue = new LinkedTransferQueue<>();
        // transfer()：如果有消费者立即传递，否则阻塞

        // 操作方式
        // 抛异常: add(), remove(), element()
        // 特定值: offer(e), poll(), peek()
        // 阻塞: put(), take()
        // 超时: offer(e, time, unit), poll(time, unit)
    }
}
```

### 7.4 ConcurrentLinkedQueue

无界非阻塞队列，基于 CAS 的无锁实现：
- 使用 Michael-Scott 算法
- 更新 head/tail 使用 CAS
- 允许 tail 落后于实际尾节点（为了减少 CAS 次数）

---

## 8. ThreadLocal 深度剖析

### 8.1 内部结构

```
Thread
  ┌──────────────────────┐
  │ threadLocals         │────▶ ThreadLocalMap
  │ (ThreadLocalMap)     │      ┌──────────────────────┐
  ├──────────────────────┤      │  Entry[] table       │
  │ inheritableThread... │      │  ┌────────────────┐  │
  └──────────────────────┘      │  │ key: WeakRef   │  │ ← 弱引用 ThreadLocal
                                │  │ value: Object  │  │ ← 强引用
                                │  └────────────────┘  │
                                │  ┌────────────────┐  │
                                │  │ key: WeakRef   │  │
                                │  │ value: Object  │  │
                                │  └────────────────┘  │
                                └──────────────────────┘
```

### 8.2 内存泄漏问题

```java
/**
 * ThreadLocal 内存泄漏详解
 *
 * 原因：
 * 1. Entry 继承 WeakReference<ThreadLocal> (key 为弱引用)
 * 2. value 是强引用
 * 3. 当 ThreadLocal 被 GC 回收后，key = null
 * 4. 但 value 仍然存在！(Entry 还活着)
 * 5. 只要线程存活，这些"脏 Entry"永远不会被回收
 *
 * 解决：
 * - 每次使用后调用 remove()
 * - ThreadLocalMap 的 get/set 方法会清理部分脏 Entry
 */
public class ThreadLocalLeakPrevention {
    private static final ThreadLocal<byte[]> TL = new ThreadLocal<>();

    public void process() {
        TL.set(new byte[1024 * 1024 * 10]); // 10MB
        try {
            // 业务处理
        } finally {
            TL.remove(); // ⚠️ 必须清理！防止内存泄漏
        }
    }

    // 最佳实践：封装成工具类
    public static class ThreadLocalUtils {
        private static final ThreadLocal<Map<String, Object>> CONTEXT =
                ThreadLocal.withInitial(HashMap::new);

        public static void set(String key, Object value) {
            CONTEXT.get().put(key, value);
        }

        @SuppressWarnings("unchecked")
        public static <T> T get(String key) {
            return (T) CONTEXT.get().get(key);
        }

        public static void clear() {
            CONTEXT.remove(); // 关键！
        }
    }
}
```

### 8.3 InheritableThreadLocal

```java
/**
 * InheritableThreadLocal：子线程继承父线程的 ThreadLocal 值
 * 在创建子线程时传递
 */
public class InheritableThreadLocalDemo {
    private static final InheritableThreadLocal<String> CONTEXT =
            new InheritableThreadLocal<>();

    public static void main(String[] args) {
        CONTEXT.set("parent-value");

        new Thread(() -> {
            // 子线程可以获取到父线程的值
            System.out.println("Child: " + CONTEXT.get()); // parent-value

            CONTEXT.set("child-value"); // 修改不影响父线程
        }).start();
    }
}
```

---

## 9. CompletableFuture 异步编程

### 9.1 基础使用

```java
/**
 * CompletableFuture (JDK 8+)：声明式异步编程
 */
public class CompletableFutureDemo {
    public static void main(String[] args) throws Exception {
        // 1. 基本用法：异步执行
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try { Thread.sleep(1000); } catch (Exception e) {}
            return "Hello";
        });

        // 2. 获取结果（阻塞）
        String result = future.get();
        System.out.println(result); // Hello

        // 3. 不阻塞，注册回调
        CompletableFuture.supplyAsync(() -> "World")
                .thenAccept(s -> System.out.println("Callback: " + s));

        // 4. 等待所有完成
        CompletableFuture<Void> all = CompletableFuture.allOf(
                CompletableFuture.supplyAsync(() -> "A"),
                CompletableFuture.supplyAsync(() -> "B"),
                CompletableFuture.supplyAsync(() -> "C")
        );
        all.get(); // 等待全部完成
    }
}
```

### 9.2 链式调用

```java
/**
 * CompletableFuture 链式调用
 */
public class CompletableFutureChain {
    public static void main(String[] args) throws Exception {
        CompletableFuture.supplyAsync(() -> {
                    // 1. 获取用户信息
                    return fetchUser(1);
                })
                .thenApply(user -> {
                    // 2. 查询订单（依赖用户信息）
                    return fetchOrders(user);
                })
                .thenApply(orders -> {
                    // 3. 计算总金额
                    return calculateTotal(orders);
                })
                .thenAccept(total -> {
                    System.out.println("Total: " + total);
                })
                .exceptionally(ex -> {
                    System.err.println("Error: " + ex);
                    return null;
                });
    }

    // 链式 API 速查
    // thenApply / thenApplyAsync     — 转换（Function, 有返回值）
    // thenAccept / thenAcceptAsync   — 消费（Consumer, 无返回值）
    // thenRun / thenRunAsync         — 执行（Runnable, 不关心结果）
    // thenCompose                    — 展平（flatMap）
    // thenCombine                    — 合并两个 CompletableFuture

    // 组合两个异步结果
    public static void combineDemo() throws Exception {
        CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> "Hello");
        CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> "World");

        // thenCombine: 两个结果组合
        f1.thenCombine(f2, (a, b) -> a + " " + b)
                .thenAccept(System.out::println); // Hello World

        // allOf: 所有完成
        CompletableFuture.allOf(f1, f2).join();

        // anyOf: 任一完成
        CompletableFuture.anyOf(f1, f2).join();
    }

    // 异常处理
    public static void exceptionHandling() {
        CompletableFuture.supplyAsync(() -> {
            if (Math.random() > 0.5) throw new RuntimeException("Error!");
            return "Success";
        })
        .exceptionally(ex -> "Fallback value")       // 异常时返回默认值
        .handle((result, ex) -> {                     // 无论是否异常都调用
            if (ex != null) return "Handled: " + ex;
            return "Result: " + result;
        });
    }

    static String fetchUser(int id) { return "User-" + id; }
    static String fetchOrders(String user) { return "Orders for " + user; }
    static int calculateTotal(String orders) { return 100; }
}
```

---

## 10. 虚拟线程 (JDK 21)

### 10.1 背景与原理

```java
/**
 * 传统线程 (Platform Thread) vs 虚拟线程 (Virtual Thread)
 *
 * 传统线程：1:1 映射到 OS 线程
 *   - 创建成本高（栈 1MB+）
 *   - 上下文切换成本高
 *   - 最大线程数受限于 OS
 *   - 适合 CPU 密集型
 *
 * 虚拟线程：M:N 映射
 *   - JDK 的调度器 (ForkJoinPool) 将多个虚拟线程映射到少数平台线程
 *   - 阻塞时自动 yield，不阻塞底层平台线程
 *   - 可以创建数百万个
 *   - 适合 IO 密集型 (每个请求一个线程)
 */
public class VirtualThreadDemo {
    public static void main(String[] args) throws Exception {
        // JDK 21+ 创建虚拟线程

        // 方式 1：Thread.ofVirtual()
        Thread vt = Thread.ofVirtual()
                .name("virtual-1")
                .start(() -> {
                    System.out.println("Virtual thread: " + Thread.currentThread());
                });

        // 方式 2：Executors.newVirtualThreadPerTaskExecutor()
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(() -> System.out.println("Task in virtual thread"));
        }

        // 对比：创建 10000 个虚拟线程 vs 平台线程
        long start = System.currentTimeMillis();

        // 创建 10000 个虚拟线程
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 10000; i++) {
                executor.submit(() -> {
                    try { Thread.sleep(100); } catch (Exception e) {}
                    return null;
                });
            }
        }
        System.out.println("10000 virtual threads: " + (System.currentTimeMillis() - start) + "ms");
        // 通常 < 1 秒

        // 可以轻松创建数百万虚拟线程
        // 但平台线程 10000 个就会很吃力（栈空间 ~10GB）
    }
}
```

### 10.2 虚拟线程的阻塞策略

```java
/**
 * 虚拟线程中的阻塞不再昂贵
 *
 * 传统线程：线程池 (200 线程) → 200 个请求并发 → 更多请求等待
 * 虚拟线程：每个请求一个线程 → 大量线程 → 阻塞时挂起
 *
 * 关键点：
 * - synchronized 会 pin 虚拟线程到平台线程（不能挂起）
 * - ReentrantLock 不会 pin
 * - 建议用 ReentrantLock 替代 synchronized
 */
public class VirtualThreadBlockingDemo {
    private static final Object lock = new Object();
    private static final ReentrantLock reentrantLock = new ReentrantLock();

    public static void main(String[] args) throws Exception {
        // synchronized 会 pin 虚拟线程
        Thread.ofVirtual().start(() -> {
            synchronized (lock) {     // 可能 pin 住底层平台线程
                // IO 操作
                try { Thread.sleep(100); } catch (Exception e) {}
            }
        });

        // ReentrantLock 不会 pin
        Thread.ofVirtual().start(() -> {
            reentrantLock.lock();
            try {
                // IO 操作
                Thread.sleep(100);
            } catch (Exception e) {
            } finally {
                reentrantLock.unlock();
            }
        });
    }
}
```

---

## 11. 结构化并发

```java
/**
 * 结构化并发 (JDK 21 Preview, JDK 22 Second Preview)
 *
 * 核心理念：并发任务的生命周期绑定到代码块
 * 类似结构化编程中的代码块作用域
 */
public class StructuredConcurrencyDemo {
    // 使用 StructuredTaskScope
    // 需要: --enable-preview (JDK 21)
    /*
    public Response handle() throws ExecutionException, InterruptedException {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            // 并发执行两个子任务
            Future<String> user = scope.fork(() -> fetchUser());
            Future<Integer> order = scope.fork(() -> fetchOrder());

            // 等待所有完成或任一失败
            scope.join();
            scope.throwIfFailed();

            // 组合结果
            return new Response(user.resultNow(), order.resultNow());
        }
    }
    */

    static String fetchUser() { return "Alice"; }
    static int fetchOrder() { return 42; }

    record Response(String user, int order) {}
}
```

---

## 12. 并发模式与死锁

### 12.1 经典并发模式

```java
/**
 * 模式 1: Producer-Consumer
 */
public class ProducerConsumerPattern {
    private static final BlockingQueue<Integer> QUEUE = new ArrayBlockingQueue<>(10);

    static class Producer implements Runnable {
        @Override
        public void run() {
            for (int i = 0; i < 100; i++) {
                try {
                    QUEUE.put(i);  // 队列满时阻塞
                    System.out.println("Produced: " + i);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    static class Consumer implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    Integer item = QUEUE.take();  // 队列空时阻塞
                    System.out.println("Consumed: " + item);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    public static void main(String[] args) {
        new Thread(new Producer()).start();
        new Thread(new Consumer()).start();
    }
}

/**
 * 模式 2: Double-Checked Locking (DCL)
 * 用于：单例模式，延迟初始化
 */
class Singleton {
    // volatile 防止指令重排序!
    private static volatile Singleton instance;

    private Singleton() {}

    public static Singleton getInstance() {
        if (instance == null) {                       // 第一次检查
            synchronized (Singleton.class) {           // 加锁
                if (instance == null) {               // 第二次检查
                    instance = new Singleton();        // 可能重排序！
                    // 1. 分配内存
                    // 2. 初始化对象
                    // 3. 赋值给 instance (volatile 防止 2 和 3 交换)
                }
            }
        }
        return instance;
    }
}
```

### 12.2 死锁

```java
/**
 * 死锁演示
 *
 * 四个必要条件：
 * 1. 互斥：资源不共享
 * 2. 持有并等待：持有锁的同时等待其他锁
 * 3. 不可剥夺：不能强行释放锁
 * 4. 循环等待：A 等 B 的资源，B 等 A 的资源
 */
public class DeadlockDemo {
    private static final Object LOCK_A = new Object();
    private static final Object LOCK_B = new Object();

    public static void main(String[] args) {
        new Thread(() -> {
            synchronized (LOCK_A) {
                System.out.println("Thread 1: 持有 A");
                try { Thread.sleep(100); } catch (Exception e) {}
                synchronized (LOCK_B) {
                    System.out.println("Thread 1: 持有 B");
                }
            }
        }).start();

        new Thread(() -> {
            synchronized (LOCK_B) {
                System.out.println("Thread 2: 持有 B");
                try { Thread.sleep(100); } catch (Exception e) {}
                synchronized (LOCK_A) {
                    System.out.println("Thread 2: 持有 A");
                }
            }
        }).start();
        // 两个线程互相等待，程序卡死
    }
}
```

#### 死锁检测

```bash
# 使用 jstack
jstack <pid>

# 输出示例：
# Found one Java-level deadlock:
# =============================
# "Thread-1":
#   waiting to lock monitor 0x00007f... (object 0x... LockA)
#   which is held by "Thread-0"
# "Thread-0":
#   waiting to lock monitor 0x00007f... (object 0x... LockB)
#   which is held by "Thread-1"
```

#### 死锁预防

```java
/**
 * 1. 固定锁获取顺序（破坏循环等待）
 */
public class DeadlockPrevention {
    private static final Object LOCK_A = new Object();
    private static final Object LOCK_B = new Object();

    public void safeMethod() {
        // 始终先锁 A → 再锁 B
        synchronized (LOCK_A) {
            synchronized (LOCK_B) {
                // 业务
            }
        }
    }
}

// 2. tryLock 超时（破坏不可剥夺）
// ReentrantLock.tryLock(timeout, unit)

// 3. 避免一个线程持有多个锁
// 4. 使用更高层级的抽象：而不是手写 synchronized 嵌套
```

---

## 13. 并发性能优化

### 13.1 优化方向

```bash
# 1. 减少锁粒度
#    ConcurrentHashMap: 桶锁 → 一个桶一个锁
#    LongAdder: 分段计数 → 多个 Cell

# 2. 锁粗化
#    for (int i=0; i<100; i++) { synchronized(lock) { ... } }
#    → synchronized(lock) { for (int i=0; i<100; i++) { ... } }

# 3. 锁消除
#    JIT 检测到锁不可能有竞争时直接消除

# 4. 使用无锁数据结构
#    Atomic*, CAS, ConcurrentLinkedQueue

# 5. 减少上下文切换
#    合理设置线程池大小
#    避免不必要的阻塞

# 6. 使用偏向锁（JDK 8, JDK 15 默认禁用）
```

### 13.2 JMH 微基准测试

```java
/**
 * JMH (Java Microbenchmark Harness)
 * 依赖: org.openjdk.jmh:jmh-core, jmh-generator-annprocess
 */
@BenchmarkMode(Mode.Throughput)   // 吞吐量
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class ConcurrencyBenchmark {

    private final ReentrantLock reentrantLock = new ReentrantLock();
    private final synchronized Object syncLock = new Object();
    private int counter;

    @Benchmark
    public int synchronizedMethod() {
        synchronized (syncLock) {
            return counter++;
        }
    }

    @Benchmark
    public int reentrantLockMethod() {
        reentrantLock.lock();
        try {
            return counter++;
        } finally {
            reentrantLock.unlock();
        }
    }

    @Benchmark
    public int atomicMethod() {
        // 假设 counter 是 AtomicInteger
        return 0; // placeholder
    }
}
```

---

## 14. 面试经典问题

### 基础问题

**Q1: 线程的五种状态是什么？wait 和 sleep 的区别？**
- NEW, RUNNABLE, BLOCKED, WAITING/TIMED_WAITING, TERMINATED
- wait 释放锁；sleep 不释放
- wait 需要在 synchronized 块内；sleep 在任何地方

**Q2: volatile 和 synchronized 的区别？**
- volatile：可见性 + 禁止重排序，不保证原子性
- synchronized：可见性 + 原子性，但更重

**Q3: 线程池的核心参数有哪些？拒绝策略有哪些？**
- corePoolSize, maximumPoolSize, keepAliveTime, workQueue, threadFactory, handler
- AbortPolicy, CallerRunsPolicy, DiscardPolicy, DiscardOldestPolicy

**Q4: synchronized 的锁升级过程？**
- 无锁 → 偏向锁 → 轻量级锁 → 重量级锁（JDK 15 禁用偏向锁）

### 进阶问题

**Q5: ConcurrentHashMap JDK 7 和 JDK 8 的区别？**
- JDK 7: Segment（继承 ReentrantLock）+ HashEntry
- JDK 8: Node + CAS + synchronized（桶锁）
- JDK 7: 并发度 = Segment 数量（默认 16）
- JDK 8: 并发度 = 桶数量（可扩展）
- JDK 8: 红黑树优化长链表

**Q6: ThreadLocal 的原理？内存泄漏怎么避免？**
- Thread → ThreadLocalMap → Entry(key=WeakRef, value)
- 内存泄漏原因：key 被回收后 value 仍存在
- 避免：每次使用后调用 remove()

**Q7: AQS 的原理？**
- 同步器框架，核心是 state + CLH 等待队列
- 支持独占和共享两种模式
- ReentrantLock, Semaphore, CountDownLatch 等基于 AQS 实现

**Q8: 死锁的四个条件和检测方法？**
- 互斥、持有并等待、不可剥夺、循环等待
- jstack、VisualVM 线程 Dump

**Q9: CompletableFuture 的常用 API？**
- supplyAsync, thenApply, thenAccept, thenCombine, allOf, anyOf
- exceptionally, handle, whenComplete

### 深度问题

**Q10: 虚拟线程的原理和优势？**
- M:N 调度：大量虚拟线程映射到少量平台线程
- 阻塞时自动 yield，不阻塞 OS 线程
- 适合 IO 密集型应用
- synchronized pin 问题需要关注

**Q11: LongAdder 比 AtomicLong 好在哪？**
- AtomicLong：所有线程 CAS 同一个变量（高竞争时性能差）
- LongAdder：分段 Cell 数组，每个线程操作自己的 Cell
- 最终求和时汇总所有 Cell

**Q12: 如何排查线上死锁或线程阻塞问题？**
- jstack 获取线程栈
- VisualVM / Arthas 监控
- 分析 BLOCKED 状态的线程和锁持有者

---

## 参考资源

- [Java Concurrency in Practice (JCIP)](https://jcip.net/)
- [JSR-166: Concurrency Utilities](https://jcp.org/en/jsr/detail?id=166)
- [JEP 444: Virtual Threads (JDK 21)](https://openjdk.org/jeps/444)
- [JEP 428: Structured Concurrency](https://openjdk.org/jeps/428)
- [AQS 论文](https://gee.cs.oswego.edu/dl/papers/aqs.pdf)
- [JMH 官方示例](https://github.com/openjdk/jmh)

---

*最后更新: 2026-05-31 | 适用于 JDK 8/11/17/21*
