# 07 - JUC并发工具类

> 定位：掌握CountDownLatch、CyclicBarrier、Semaphore、Exchanger、Phaser五大协同工具的核心用法、区别与实战场景

## 目录

1. [JUC工具类全景图](#1-juc工具类全景图)
2. [CountDownLatch：倒计时门闩](#2-countdownlatch倒计时门闩)
3. [CyclicBarrier：循环栅栏](#3-cyclicbarrier循环栅栏)
4. [CountDownLatch vs CyclicBarrier](#4-countdownlatch-vs-cyclicbarrier)
5. [Semaphore：信号量](#5-semaphore信号量)
6. [Exchanger：交换器](#6-exchanger交换器)
7. [Phaser：多阶段栅栏（JDK 7+）](#7-phaser多阶段栅栏jdk-7)
8. [五大工具对比总结](#8-五大工具对比总结)
9. [实战：模拟高并发场景](#9-实战模拟高并发场景)
10. [面试高频考点](#10-面试高频考点)

---

## 1. JUC工具类全景图

`java.util.concurrent` 包在 JDK 5 通过 JSR-166 引入，为Java并发编程提供了完整的"武器库"。其中的五大协同工具专门解决多线程间的协作、等待、唤醒、限流与数据交换问题。

```
JUC 并发协同工具家族

┌─────────────────────────────────────────────────────────┐
│                  线程协同工具 (AQS 衍生)                   │
│                                                         │
│  CountDownLatch  ──── 倒计时等待（一次性）                │
│       │                 主线程等 N 个子线程完成            │
│       │                                                  │
│  CyclicBarrier   ──── 循环屏障（可复用）                  │
│       │                 N 个线程互相等待到齐               │
│       │                                                  │
│  Semaphore       ──── 信号量限流（可复用）                 │
│       │                 控制同时访问的线程数量             │
│       │                                                  │
│  Exchanger       ──── 双线程数据交换（点对点）             │
│       │                 两个线程在交换点互换数据           │
│       │                                                  │
│  Phaser          ──── 多阶段栅栏（JDK 7+, 动态注册）      │
│                       分阶段同步，支持动态增减参与者       │
└─────────────────────────────────────────────────────────┘
```

| 特性 | CountDownLatch | CyclicBarrier | Semaphore | Exchanger | Phaser |
|------|---------------|---------------|-----------|-----------|--------|
| 引入版本 | JDK 5 | JDK 5 | JDK 5 | JDK 5 | JDK 7 |
| 核心机制 | 计数器递减 | 计数器递增到阈值 | 许可证数量 | 双线程交换点 | 阶段+参与者 |
| 可重用 | 否 | 是 (reset) | 是 | 是 | 是 (动态增减) |
| 参与者数量 | 构造时固定 | 构造时固定 | 构造时固定 | 固定2个 | 动态注册/注销 |
| 是否基于AQS | 是 (共享模式) | 否 (ReentrantLock+Condition) | 是 (共享模式) | 否 | 是 (共享模式) |
| 主要用途 | 等待完成 | 等待到齐 | 限流控制 | 数据交换 | 分阶段任务 |

> 💡 JUC工具类的底层核心是AQS（AbstractQueuedSynchronizer），CountDownLatch和Semaphore基于AQS共享模式实现，CyclicBarrier通过ReentrantLock+Condition组合实现，Exchanger使用CAS+自旋实现，Phaser则是AQS共享模式的扩展。

---

## 2. CountDownLatch：倒计时门闩

### 2.1 核心原理

CountDownLatch 允许一个或多个线程等待其他线程完成操作，计数器一旦归零则不可重置。

```
初始化: latch = new CountDownLatch(3)
          计数器 = 3

主线程: latch.await()   ─────────────────────────┐
                                                  │ 阻塞等待
子线程1: doWork(); latch.countDown()  // 3 → 2    │
子线程2: doWork(); latch.countDown()  // 2 → 1    │
子线程3: doWork(); latch.countDown()  // 1 → 0 ───┘

当计数器归零时, 主线程自动唤醒继续执行
```

### 2.2 核心API

| 方法 | 说明 |
|------|------|
| `CountDownLatch(int count)` | 构造器，指定初始计数器值（必须 >= 0） |
| `await()` | 阻塞当前线程，直到计数器归零 |
| `await(long timeout, TimeUnit unit)` | 带超时的等待，超时返回 false |
| `countDown()` | 计数器减1，减到0时释放所有等待线程 |
| `getCount()` | 获取当前计数器值 |

### 2.3 完整可运行代码

```java
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * CountDownLatch 示例：模拟服务启动
 * 主线程等待 3 个子线程完成初始化后再继续
 */
public class CountDownLatchDemo {
    private static final int SERVICE_COUNT = 3;

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(SERVICE_COUNT);

        System.out.println("=== 主线程：等待 " + SERVICE_COUNT + " 个服务启动 ===");

        for (int i = 1; i <= SERVICE_COUNT; i++) {
            final int serviceId = i;
            new Thread(() -> {
                try {
                    System.out.println("服务 " + serviceId + " 启动中...");
                    Thread.sleep((long) (Math.random() * 2000));
                    System.out.println("服务 " + serviceId + " 启动完成");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown(); // ⚠️ 必须放在 finally 中
                }
            }, "Service-" + i).start();
        }

        // 主线程等待（最多等 5 秒）
        boolean allStarted = latch.await(5, TimeUnit.SECONDS);
        if (allStarted) {
            System.out.println("=== 所有服务启动完毕，主线程继续 ===");
        } else {
            System.out.println("=== 等待超时，存在服务未启动 ===");
        }
    }
}
```

```text
// 运行结果示例：
=== 主线程：等待 3 个服务启动 ===
服务 1 启动中...
服务 2 启动中...
服务 3 启动中...
服务 2 启动完成
服务 1 启动完成
服务 3 启动完成
=== 所有服务启动完毕，主线程继续 ===
```

### 2.4 典型应用场景

- **并行初始化：** 主线程等待所有服务组件（数据库、缓存、MQ）启动完成
- **多任务汇总：** 多个线程分别计算，主线程汇总结果
- **压测模拟：** N 个线程同时准备，主线程一声令下同时执行

> ⚠️ CountDownLatch 是一次性的，计数器归零后不能复用。如果需要循环等待，应使用 CyclicBarrier。

---

## 3. CyclicBarrier：循环栅栏

### 3.1 核心原理

CyclicBarrier 让一组线程到达一个屏障（Barrier）时被阻塞，直到所有线程都到达后，屏障打开，所有线程继续执行。支持可选的 BarrierAction（屏障打开时优先执行的回调）。

```
初始化: barrier = new CyclicBarrier(3, barrierAction)
          计数器 = 3

线程1: doWork(); barrier.await()  → 等待，计数器 3 → 2
线程2: doWork(); barrier.await()  → 等待，计数器 2 → 1
线程3: doWork(); barrier.await()  → 计数器 1 → 0 → 触发 barrierAction
                                      ↓
                             所有线程同时继续执行
```

### 3.2 核心API

| 方法 | 说明 |
|------|------|
| `CyclicBarrier(int parties)` | 构造器，指定参与线程数 |
| `CyclicBarrier(int parties, Runnable barrierAction)` | 构造器，指定屏障打开时的回调 |
| `await()` | 到达屏障并等待，返回到达顺序索引 |
| `await(long timeout, TimeUnit unit)` | 带超时的等待 |
| `reset()` | 重置屏障（正在等待的线程会收到 BrokenBarrierException） |
| `getNumberWaiting()` | 获取当前在屏障处等待的线程数 |
| `isBroken()` | 判断屏障是否被破坏 |

### 3.3 完整可运行代码

```java
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * CyclicBarrier 示例：多人组队副本
 * 所有玩家都准备好后，同时进入副本
 */
public class CyclicBarrierDemo {
    private static final int PLAYER_COUNT = 4;

    public static void main(String[] args) {
        CyclicBarrier barrier = new CyclicBarrier(PLAYER_COUNT,
                () -> System.out.println("=== 所有玩家已准备好，副本开始！ ==="));

        for (int i = 1; i <= PLAYER_COUNT; i++) {
            final int playerId = i;
            new Thread(() -> {
                try {
                    // 模拟玩家加载游戏
                    System.out.println("玩家 " + playerId + " 加载中...");
                    Thread.sleep((long) (Math.random() * 3000));
                    System.out.println("玩家 " + playerId + " 已准备好，等待其他玩家");

                    // 到达屏障，等待其他玩家
                    barrier.await();

                    // 屏障打开后，所有玩家同时进入
                    System.out.println("玩家 " + playerId + " 进入副本！");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (BrokenBarrierException e) {
                    System.out.println("玩家 " + playerId + " 检测到屏障被破坏");
                }
            }, "Player-" + i).start();
        }
    }
}
```

```text
// 运行结果示例：
玩家 1 加载中...
玩家 2 加载中...
玩家 3 加载中...
玩家 4 加载中...
玩家 4 已准备好，等待其他玩家
玩家 2 已准备好，等待其他玩家
玩家 3 已准备好，等待其他玩家
玩家 1 已准备好，等待其他玩家
=== 所有玩家已准备好，副本开始！ ===
玩家 1 进入副本！
玩家 3 进入副本！
玩家 4 进入副本！
玩家 2 进入副本！
```

### 3.4 CyclicBarrier 的三大核心特性

1. **可重用性：** 调用 `reset()` 可重置屏障，实现多轮等待
2. **屏障回调：** 最后一个到达的线程会执行 BarrierAction，可用于汇总或过渡逻辑
3. **破坏检测：** 线程中断或超时会导致屏障破坏（BrokenBarrierException），其他等待线程需要处理

> 💡 屏障回调（BarrierAction）的执行线程是最后一个到达屏障的线程，不是单独的线程，因此回调中不应执行耗时操作。

### 3.5 使用 reset() 实现多轮等待

```java
CyclicBarrier barrier = new CyclicBarrier(3);
ExecutorService executor = Executors.newFixedThreadPool(3);

// 第 1 轮
for (int i = 0; i < 3; i++) {
    executor.submit(() -> {
        try { barrier.await(); } catch (Exception e) { Thread.currentThread().interrupt(); }
    });
}

// 等待所有线程完成第 1 轮
Thread.sleep(100);
System.out.println("未重置前，等待线程数: " + barrier.getNumberWaiting()); // 0

// 第 2 轮
barrier.reset(); // 重置
for (int i = 0; i < 3; i++) {
    executor.submit(() -> {
        try { barrier.await(); } catch (Exception e) { Thread.currentThread().interrupt(); }
    });
}
```

---

## 4. CountDownLatch vs CyclicBarrier

### 4.1 核心区别对比

| 对比维度 | CountDownLatch | CyclicBarrier |
|---------|---------------|---------------|
| **计数器方向** | 递减（向下数） | 递增（向上数到 parties） |
| **可重用性** | ❌ 一次性，用完即废 | ✅ 可调用 reset() 循环使用 |
| **等待机制** | 一个线程等待多个线程 | 多个线程互相等待 |
| **参与者感知** | 计数线程不感知彼此 | 所有线程互相感知 |
| **回调** | 无 | ✅ 支持 BarrierAction |
| **使用场景** | 等待结束（一等多） | 等待到齐（多等多） |

> ⚠️ 最容易混淆的点：CountDownLatch 是**一个线程等待多个线程完成**（一等多），CyclicBarrier 是**多个线程互相等待直到全部到齐**（多等多）。CountDownLatch 侧重"事件是否发生"，CyclicBarrier 侧重"所有线程是否到齐"。

### 4.2 场景选择指南

```
你需要什么？
├── 主线程等子线程完成 → CountDownLatch
│   例：主线程等待所有服务启动
│
├── 一组线程互相等待到齐 → CyclicBarrier
│   例：多线程分阶段计算，每阶段结束需同步
│
├── 需要循环使用同一屏障 → CyclicBarrier (reset)
│   例：分批次处理数据，每批结束后同步
│
└── 只需要事件通知（不需要计数线程参与） → CountDownLatch
    例：所有线程就绪后发令枪
```

### 4.3 模拟"发令枪"场景

```java
/**
 * 同时对比：CountDownLatch 和 CyclicBarrier 实现发令枪
 */
public class RaceDemo {
    public static void main(String[] args) throws InterruptedException {
        int runnerCount = 5;

        // ---- 方式一：CountDownLatch ----
        CountDownLatch readyLatch = new CountDownLatch(runnerCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        System.out.println("=== CountDownLatch 模式 ===");
        for (int i = 1; i <= runnerCount; i++) {
            int id = i;
            new Thread(() -> {
                try {
                    Thread.sleep((long) (Math.random() * 1000));
                    readyLatch.countDown(); // 到达起点
                    startLatch.await();     // 等待发令枪
                    System.out.println("选手 " + id + " 起跑！");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }

        readyLatch.await(); // 等待所有选手就位
        System.out.println("所有选手就位，预备——跑！");
        startLatch.countDown(); // 发令枪响

        Thread.sleep(2000);

        // ---- 方式二：CyclicBarrier ----
        CyclicBarrier barrier = new CyclicBarrier(runnerCount,
                () -> System.out.println("预备——跑！"));

        System.out.println("\n=== CyclicBarrier 模式 ===");
        for (int i = 1; i <= runnerCount; i++) {
            int id = i;
            new Thread(() -> {
                try {
                    Thread.sleep((long) (Math.random() * 1000));
                    System.out.println("选手 " + id + " 就位");
                    barrier.await();
                    System.out.println("选手 " + id + " 起跑！");
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }
}
```

---

## 5. Semaphore：信号量

### 5.1 核心原理

Semaphore 维护一组许可证，线程在执行前需要先获取许可证，执行完毕后归还。可有效控制同时访问特定资源的线程数量，实现限流和资源池管理。

```
初始化: semaphore = new Semaphore(3)   // 3 个许可证

线程1: acquire() → 许可证: 3 → 2  → 执行 → release() → 许可证: 2 → 3
线程2: acquire() → 许可证: 2 → 1  → 执行 → release() → 许可证: 1 → 2
线程3: acquire() → 许可证: 1 → 0  → 执行 → release() → 许可证: 0 → 1
线程4: acquire() ⛔ 阻塞等待... (直到有线程 release)
```

### 5.2 核心API

| 方法 | 说明 |
|------|------|
| `Semaphore(int permits)` | 构造器，指定许可证数量（非公平） |
| `Semaphore(int permits, boolean fair)` | 构造器，可指定公平/非公平 |
| `acquire()` | 获取一个许可证（阻塞，可中断） |
| `acquire(int permits)` | 获取 N 个许可证 |
| `acquireUninterruptibly()` | 获取许可证（不响应中断） |
| `tryAcquire()` | 尝试获取许可证，立即返回 boolean |
| `tryAcquire(long timeout, TimeUnit unit)` | 尝试获取，带超时 |
| `release()` | 归还一个许可证 |
| `release(int permits)` | 归还 N 个许可证 |
| `availablePermits()` | 当前可用许可证数量 |
| `drainPermits()` | 获取并返回所有可用许可证 |
| `getQueueLength()` | 等待获取许可证的线程数 |

### 5.3 完整可运行代码

```java
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Semaphore 示例：模拟数据库连接池限流
 * 最多允许 3 个线程同时获取连接
 */
public class SemaphoreDemo {
    private static final int MAX_CONNECTIONS = 3;
    private static final int THREAD_COUNT = 10;

    public static void main(String[] args) {
        Semaphore semaphore = new Semaphore(MAX_CONNECTIONS, true);

        System.out.println("=== 数据库连接池限流演示 ===");
        System.out.println("最大并发连接数: " + MAX_CONNECTIONS + "\n");

        for (int i = 1; i <= THREAD_COUNT; i++) {
            int taskId = i;
            new Thread(() -> {
                try {
                    System.out.println("任务 " + taskId
                            + " 等待获取连接 (可用: " + semaphore.availablePermits() + ")");

                    // 获取许可证（最多等 2 秒）
                    if (semaphore.tryAcquire(2, TimeUnit.SECONDS)) {
                        try {
                            System.out.println("任务 " + taskId + " ☆ 获取连接，开始查询");
                            Thread.sleep(1000); // 模拟数据库查询
                            System.out.println("任务 " + taskId + " 查询完成，释放连接");
                        } finally {
                            semaphore.release(); // ⚠️ 必须在 finally 中释放
                        }
                    } else {
                        System.out.println("任务 " + taskId + " ✗ 获取连接超时，稍后重试");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Thread-" + i).start();
        }
    }
}
```

```text
// 运行结果示例（部分）：
=== 数据库连接池限流演示 ===
最大并发连接数: 3

任务 1 等待获取连接 (可用: 3)
任务 2 等待获取连接 (可用: 3)
任务 3 等待获取连接 (可用: 3)
任务 1 ☆ 获取连接，开始查询
任务 2 ☆ 获取连接，开始查询
任务 3 ☆ 获取连接，开始查询
任务 4 等待获取连接 (可用: 0)  ⛔ 等待
任务 5 等待获取连接 (可用: 0)  ⛔ 等待
任务 1 查询完成，释放连接
任务 4 ☆ 获取连接，开始查询
...
```

### 5.4 Semaphore 的公平与非公平

| 模式 | 行为 | 适用场景 |
|------|------|---------|
| 非公平（默认） | 新线程可能插队获取许可证 | 追求吞吐量，允许短任务插队 |
| 公平 | 按等待顺序分配许可证（FIFO） | 防止线程饥饿，保证公平 |

```java
// 公平信号量
Semaphore fair = new Semaphore(3, true);

// 非公平信号量
Semaphore unfair = new Semaphore(3, false); // 默认
```

> 💡 Semaphore 的 release() 方法不需要由 acquire() 的线程调用——任何线程都可以释放许可证，这是与锁的重要区别。

### 5.5 典型应用场景

- **接口限流：** 限制接口同时处理的请求数
- **连接池管理：** 数据库连接池、HTTP 连接池
- **资源池控制：** 有限资源的访问控制（如打印机、文件句柄）
- **信号量隔离：** 不同业务使用不同信号量，避免相互影响

---

## 6. Exchanger：交换器

### 6.1 核心原理

Exchanger 用于两个线程之间的数据交换。线程在交换点调用 `exchange()` 后会阻塞，等待对方线程也到达交换点，然后互相交换数据，再继续执行。

```
线程A: exchange(dataA)   ────→   阻塞等待
                                     │
线程B: exchange(dataB)   ────→   阻塞等待
                                     │
两个线程同时到达交换点 ↓
                                     │
线程A 收到 dataB ←────────────────────┤
线程B 收到 dataA ←────────────────────┘
```

### 6.2 核心API

| 方法 | 说明 |
|------|------|
| `Exchanger<V>()` | 构造器，V 为交换的数据类型 |
| `exchange(V x)` | 交换数据，阻塞直到对方到达 |
| `exchange(V x, long timeout, TimeUnit unit)` | 带超时的交换 |

### 6.3 完整可运行代码

```java
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;

/**
 * Exchanger 示例：生产者-消费者数据交换
 * 生产者传递原始数据，消费者返回处理结果
 */
public class ExchangerDemo {
    public static void main(String[] args) {
        Exchanger<String> exchanger = new Exchanger<>();

        // 生产者线程
        new Thread(() -> {
            try {
                for (int i = 1; i <= 3; i++) {
                    String rawData = "RawData-" + i;
                    System.out.println("[生产者] 生产: " + rawData);

                    // 交换数据（等待消费者处理，最多等 2 秒）
                    String processed = exchanger.exchange(rawData, 2, TimeUnit.SECONDS);
                    System.out.println("[生产者] 收到处理结果: " + processed);
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.out.println("[生产者] 交换异常: " + e.getMessage());
            }
        }, "Producer").start();

        // 消费者线程
        new Thread(() -> {
            try {
                for (int i = 1; i <= 3; i++) {
                    // 获取生产者数据，返回处理结果
                    String rawData = exchanger.exchange("Processed-" + i);
                    System.out.println("[消费者] 收到原始数据: " + rawData
                            + " → 处理完成");
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Consumer").start();
    }
}
```

```text
// 运行结果示例：
[生产者] 生产: RawData-1
[消费者] 收到原始数据: RawData-1 → 处理完成
[生产者] 收到处理结果: Processed-1
[生产者] 生产: RawData-2
[消费者] 收到原始数据: RawData-2 → 处理完成
[生产者] 收到处理结果: Processed-2
[生产者] 生产: RawData-3
[消费者] 收到原始数据: RawData-3 → 处理完成
[生产者] 收到处理结果: Processed-3
```

> ⚠️ Exchanger 仅适用于两个线程之间的点对点交换。如果超过两个线程需要交换数据，应使用 ConcurrentHashMap 等并发容器或其他方案。

### 6.4 典型应用场景

- **生产者-消费者缓冲区交换：** 生产者和消费者交替使用两个缓冲区
- **两个线程间的结果传递：** 一个线程计算结果，另一个线程消费
- **遗传算法/并行计算：** 两个线程在每一轮计算后交换中间结果

---

## 7. Phaser：多阶段栅栏（JDK 7+）

### 7.1 核心原理

Phaser 是 JDK 7 引入的灵活同步屏障，支持多阶段（Phase）同步，且参与者数量可以动态注册和注销，是 CyclicBarrier 的增强版。

```
Phase 0:      线程1 ──await──┐
              线程2 ──await──┤ 全部到齐 → 进入 Phase 1
              线程3 ──await──┘

Phase 1:      线程1 ──await──┐
              线程2 ──await──┤ 全部到齐 → 进入 Phase 2
              线程3 ──await──┘

Phase 2:      ...
```

### 7.2 核心API

| 方法 | 说明 |
|------|------|
| `Phaser()` | 构造器，初始参与者为 0 |
| `Phaser(int parties)` | 构造器，指定初始参与者数量 |
| `register()` | 注册一个参与者（增加 parties） |
| `bulkRegister(int parties)` | 批量注册参与者 |
| `arrive()` | 到达但不等待，返回阶段号 |
| `arriveAndDeregister()` | 到达并注销当前线程 |
| `arriveAndAwaitAdvance()` | 到达并等待其他参与者 |
| `awaitAdvance(int phase)` | 等待指定阶段完成 |
| `getPhase()` | 获取当前阶段编号 |
| `getRegisteredParties()` | 获取注册的参与者数量 |
| `onAdvance(int phase, int registeredParties)` | 阶段推进时的回调（重写以自定义逻辑） |
| `forceTermination()` | 强制终止 Phaser |

### 7.3 完整可运行代码

```java
import java.util.concurrent.Phaser;

/**
 * Phaser 示例：多阶段并行计算
 * 3 个线程共同完成 3 个阶段的任务，每个阶段结束后汇总
 */
public class PhaserDemo {
    public static void main(String[] args) {
        int workerCount = 3;
        int phaseCount = 3;

        // 创建 Phaser，注册主线程
        Phaser phaser = new Phaser(1);
        System.out.println("=== 多阶段并行任务开始 ===\n");

        // 注册并启动工作线程
        for (int i = 1; i <= workerCount; i++) {
            phaser.register(); // 注册参与者
            final int workerId = i;
            new Thread(() -> {
                try {
                    for (int phase = 1; phase <= phaseCount; phase++) {
                        System.out.println("工作线程 " + workerId
                                + " 执行阶段 " + phase);
                        Thread.sleep((long) (Math.random() * 1000));

                        System.out.println("工作线程 " + workerId
                                + " 到达阶段 " + phase + " 屏障");
                        phaser.arriveAndAwaitAdvance(); // 到达并等待
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    phaser.arriveAndDeregister(); // 完成所有阶段后注销
                }
            }, "Worker-" + i).start();
        }

        // 主线程也参与同步
        for (int phase = 1; phase <= phaseCount; phase++) {
            System.out.println("\n--- 主线程等待阶段 " + phase + " 完成 ---");
            phaser.arriveAndAwaitAdvance();
            System.out.println("=== 阶段 " + phase + " 全部完成 ===");
        }

        // 注销主线程
        phaser.arriveAndDeregister();
        System.out.println("\n=== 所有阶段完成！===");
        System.out.println("Phaser 是否终止: " + phaser.isTerminated());
    }
}
```

```text
// 运行结果示例（部分）：
=== 多阶段并行任务开始 ===

工作线程 1 执行阶段 1
工作线程 2 执行阶段 1
工作线程 3 执行阶段 1
工作线程 1 到达阶段 1 屏障
工作线程 2 到达阶段 1 屏障
工作线程 3 到达阶段 1 屏障

--- 主线程等待阶段 1 完成 ---
=== 阶段 1 全部完成 ===

工作线程 1 执行阶段 2
...
=== 所有阶段完成！===
```

### 7.4 Phaser vs CyclicBarrier：何时使用 Phaser

| 需求 | CyclicBarrier | Phaser |
|------|---------------|--------|
| 固定参与者数量 | ✅ 简洁 | ✅ 支持 |
| 动态增减参与者 | ❌ 不支持 | ✅ register/arriveAndDeregister |
| 多阶段同步 | ❌ 需手动 reset | ✅ 原生支持多阶段 |
| 阶段回调 | ✅ BarrierAction | ✅ 重写 onAdvance |
| 终止控制 | ❌ 不支持 | ✅ forceTermination |
| 获取等待线程数 | ✅ getNumberWaiting | ✅ getArrivedParties |

> 💡 当参与者数量固定且只有单阶段同步时，使用 CyclicBarrier 更简洁。当需要多阶段同步或参与者数量可能变化时，Phaser 是更好的选择。

### 7.5 Phaser 的 onAdvance 高级用法

```java
/**
 * 自定义 Phaser：在阶段推进时执行回调
 * 在所有线程完成 stage-2 后自动终止
 */
Phaser phaser = new Phaser(3) {
    @Override
    protected boolean onAdvance(int phase, int registeredParties) {
        System.out.println("=== 阶段 " + phase + " 完成, 当前参与者: "
                + registeredParties + " ===");
        // 返回 true 会终止 Phaser，false 则继续
        return phase >= 2 || registeredParties == 0;
    }
};
```

---

## 8. 五大工具对比总结

### 8.1 综合对比表

| 对比维度 | CountDownLatch | CyclicBarrier | Semaphore | Exchanger | Phaser |
|---------|---------------|---------------|-----------|-----------|--------|
| 核心语义 | 等待完成 | 等待到齐 | 限流控制 | 数据交换 | 分阶段同步 |
| 参与者方向 | 一等多 | 多等多 | 多争多 | 一对一 | 多等多 |
| 计数器方向 | 递减 | 递增 | 许可证数量 | N/A | 阶段+参与者 |
| 可重用 | ❌ | ✅ reset() | ✅ 自动 | ✅ 自动 | ✅ 自动 |
| 是否支持超时 | ✅ | ✅ | ✅ | ✅ | ✅ |
| 是否可中断 | ✅ | ✅ | ✅ | ✅ | ✅ |
| 回调机制 | ❌ | ✅ BarrierAction | ❌ | ❌ | ✅ onAdvance |
| 动态参与者 | ❌ | ❌ | ❌ | ❌ | ✅ |
| 内部实现 | AQS 共享 | ReentrantLock+Condition | AQS 共享 | CAS+自旋 | AQS 共享 |
| 底层源码核心类 | Sync (内部类) | Generation, lock, trip | Sync (内部类) | Participant, Node | QNode, root |

### 8.2 场景速查

```text
场景                                        → 推荐工具
──────────────────────────────────────────────────────────
主线程等待多个子线程完成                      → CountDownLatch
多个线程互相等待到齐后继续                    → CyclicBarrier
多个线程分多阶段同步执行                      → Phaser
限制同一资源的并发访问数                      → Semaphore
两个线程之间交换数据                          → Exchanger
需要循环使用同步屏障                          → CyclicBarrier / Phaser
参与者可能动态增减                            → Phaser
纯限流，不需要线程间协作                      → Semaphore + RateLimiter
```

### 8.3 避坑要点

| 工具 | 常见问题 | 预防方案 |
|------|---------|---------|
| CountDownLatch | countDown 遗漏导致永久阻塞 | countDown 放在 finally 中；await 设置超时 |
| CyclicBarrier | 线程中断导致 BrokenBarrierException | 捕获异常并调用 reset()；设置超时 |
| Semaphore | release 遗漏导致许可泄漏 | release 放在 finally 中；使用 tryRelease |
| Exchanger | 只有一方到达导致永久阻塞 | 使用带超时的 exchange 方法 |
| Phaser | 参与者注册/注销不匹配 | 使用 try/finally 确保注销；注意 phase 溢出 |

> 🎯 核心要诀：JUC 并发工具的使用可以总结为一句话——**先定语义再选工具**。你是想"等待完成"（CountDownLatch）？还是"等待到齐"（CyclicBarrier/Phaser）？还是"控制并发"（Semaphore）？还是"交换数据"（Exchanger）？确定语义后再选择对应工具，自然水到渠成。

---

## 9. 实战：模拟高并发场景

### 9.1 场景描述

模拟电商秒杀系统的限流与并发控制：
- 200 个用户同时抢购 10 件商品
- 通过 Semaphore 控制并发访问数据库的线程数（最多 20 个）
- 通过 CountDownLatch 统计所有用户是否完成抢购
- 通过 CyclicBarrier 模拟多阶段库存校验

### 9.2 完整代码

```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 实战：模拟秒杀系统并发协同
 */
public class SeckillSimulation {

    private static final int USER_COUNT = 200;       // 参与用户数
    private static final int MAX_CONCURRENT_DB = 20; // 最大并发访问数据库数
    private static final int STOCK = 10;             // 总库存

    private static final AtomicInteger successCount = new AtomicInteger(0);
    private static final AtomicInteger stock = new AtomicInteger(STOCK);

    public static void main(String[] args) throws InterruptedException {
        // 1. 信号量：控制数据库并发
        Semaphore dbSemaphore = new Semaphore(MAX_CONCURRENT_DB, true);

        // 2. 倒计时：等待所有用户完成
        CountDownLatch allDone = new CountDownLatch(USER_COUNT);

        // 3. 循环栅栏：分阶段库存校验（第一阶段：查库存，第二阶段：扣减）
        CyclicBarrier phaseBarrier = new CyclicBarrier(MAX_CONCURRENT_DB,
                () -> System.out.println("--- 批量校验完成，当前已售: "
                        + successCount.get() + " ---"));

        System.out.println("=== 秒杀开始 ===");
        System.out.println("总库存: " + STOCK + ", 参与用户: " + USER_COUNT + "\n");

        ExecutorService executor = Executors.newFixedThreadPool(100);

        for (int i = 1; i <= USER_COUNT; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    // 阶段 1：限流等待（最多等 5 秒）
                    if (!dbSemaphore.tryAcquire(5, TimeUnit.SECONDS)) {
                        System.out.println("用户 " + userId + " 排队超时");
                        return;
                    }

                    try {
                        // 模拟数据库查询库存（第一阶段）
                        Thread.sleep(50);
                        int currentStock = stock.get();

                        // 到达阶段屏障
                        phaseBarrier.await(2, TimeUnit.SECONDS);

                        // 阶段 2：扣减库存
                        if (currentStock > 0) {
                            int remaining = stock.decrementAndGet();
                            if (remaining >= 0) {
                                successCount.incrementAndGet();
                                System.out.println("用户 " + userId + " ★ 抢购成功！");
                            } else {
                                System.out.println("用户 " + userId + " 抢购失败（库存不足）");
                            }
                        }
                    } finally {
                        dbSemaphore.release(); // 释放数据库连接
                    }
                } catch (Exception e) {
                    System.out.println("用户 " + userId + " 异常: " + e.getClass().getSimpleName());
                } finally {
                    allDone.countDown(); // 标记完成
                }
            });
        }

        // 等待所有用户完成
        allDone.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        System.out.println("\n=== 秒杀结束 ===");
        System.out.println("成功抢购: " + successCount.get());
        System.out.println("剩余库存: " + Math.max(0, stock.get()));
    }
}
```

### 9.3 实战要点分析

| 工具 | 在本场景中的作用 | 关键注意点 |
|------|----------------|-----------|
| Semaphore | 控制同时操作数据库的线程数（限流） | tryAcquire 设置超时避免死等 |
| CountDownLatch | 主线程等待所有用户完成抢购 | 每个线程在 finally 中 countDown |
| CyclicBarrier | 分阶段：先查库存再扣减 | 设置超时防止屏障破坏导致的死等 |
| AtomicInteger | 替代锁实现原子库存扣减 | CAS 高性能，但 ABA 问题不影响此处 |

> 🎯 在高并发实战中，**工具组合使用**远比单一工具更常见。限流 + 协同 + 计数的组合能有效应对大多数秒杀、抢购、排队等场景。

---

## 10. 面试高频考点

### 10.1 基础问答

**Q1: CountDownLatch 和 CyclicBarrier 有什么区别？**

| 区别点 | CountDownLatch | CyclicBarrier |
|--------|---------------|---------------|
| 计数器 | 递减 | 递增到 parties |
| 重用 | 不可重用 | 可调用 reset() 重用 |
| 等待模式 | 一等多 | 多等多 |
| 回调 | 无 | 支持 BarrierAction |

**Q2: Semaphore 的公平和非公平模式有什么区别？**

公平模式下，等待时间最长的线程优先获取许可证，使用 FIFO 队列；非公平模式下，新到达的线程可能插队。默认是非公平模式，追求更高吞吐量，但可能导致线程饥饿。

**Q3: Semaphore 和锁有什么区别？**

锁（Lock、synchronized）是排他的、持有锁的线程才能释放；Semaphore 是计数的，任何线程都可以 release()，不要求与 acquire() 是同一个线程。

### 10.2 进阶问答

**Q4: CountDownLatch 的 await() 和 Thread.join() 有什么区别？**

```text
join()：等待目标线程终止（TERMINATED 状态）
await()：等待计数器归零（不要求线程结束，只要求调用 countDown）
```

join() 要求线程必须终止才能解除阻塞，而 CountDownLatch 更加灵活——线程完成任务后可以继续执行其他操作，只需调用 countDown() 即可。

**Q5: Phaser 相比 CyclicBarrier 有哪些增强？**

- 支持动态注册和注销参与者（register / arriveAndDeregister）
- 原生支持多阶段同步，无需手动 reset
- 可通过重写 onAdvance 自定义阶段推进逻辑
- 可调用 forceTermination 强制终止

**Q6: CyclicBarrier 中的 BrokenBarrierException 是如何产生的？**

当以下情况之一发生时，屏障被破坏（Broken）：
- 某个等待线程被中断（InterruptedException）
- 某个等待线程超时（TimeoutException）
- 调用 barrier.reset() 重置
被破坏后，所有正在等待和未来到达的线程都会收到 BrokenBarrierException。

**Q7: Exchanger 的底层实现原理？**

Exchanger 使用 CAS + 自旋 + 槽位（Slot）机制实现。内部维护一个 Participant 数组，每个线程通过 CAS 抢占槽位。当两个线程都到达时，通过对象赋值完成数据交换。高并发下使用 Striped 锁优化减少竞争。

### 10.3 深度问答

**Q8: 手写一个支持重置的 CountDownLatch？**

```java
/**
 * 可重置的 CountDownLatch（扩展场景题）
 * 思路：使用 AQS 自旋+CAS 或 ReentrantLock+Condition 实现
 */
public class ResettableCountDownLatch {
    private volatile int count;
    private final Object lock = new Object();

    public ResettableCountDownLatch(int count) {
        this.count = count;
    }

    public void await() throws InterruptedException {
        synchronized (lock) {
            while (count > 0) {
                lock.wait();
            }
        }
    }

    public void countDown() {
        synchronized (lock) {
            if (count > 0) {
                count--;
                if (count == 0) {
                    lock.notifyAll();
                }
            }
        }
    }

    /** 重置计数器，唤醒所有等待线程 */
    public void reset(int newCount) {
        synchronized (lock) {
            this.count = newCount;
            lock.notifyAll();
        }
    }
}
```

**Q9: 线上一个 CountDownLatch 的 countDown 没有调用导致服务阻塞，如何排查？**

1. 使用 `jstack <pid>` 查看线程堆栈，找到 BLOCKED/WAITING 状态的线程
2. CountDownLatch 的等待线程会停在 `parkAndCheckInterrupt()` 调用
3. 通过线程栈信息反查业务代码，定位业务逻辑中遗漏 countDown 的位置
4. 修复建议：为 await 设置超时，在 finally 中执行 countDown

**Q10: Semaphore 实现限流和 RateLimiter 有什么区别？**

```text
Semaphore：    控制"最大并发数"，基于许可证计数
RateLimiter：  控制"单位时间吞吐量"，基于令牌桶算法（Guava）

Semaphore 适合：限制同时访问资源的线程数
RateLimiter 适合：平滑限制请求速率，允许突发流量
```

> 🎯 面试核心：JUC 工具类的高频考点集中在 **CountDownLatch vs CyclicBarrier 的区别**、**Semaphore 的限流原理**以及**Phaser 的多阶段同步优势**。手写代码时需特别注意异常处理（finally 释放）、超时设置和线程中断的响应。

---

## 参考资源

- [JSR-166: Concurrency Utilities](https://jcp.org/en/jsr/detail?id=166)
- [Java Concurrency in Practice (JCIP)](https://jcip.net/)
- [AQS 论文 — Doug Lea](https://gee.cs.oswego.edu/dl/papers/aqs.pdf)
- [Phaser 源码分析](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/Phaser.html)

---

*最后更新: 2026-07-26 | 适用于 JDK 8/11/17/21*
