# 06 - 线程池与Executor框架
> 定位：彻底理解ThreadPoolExecutor七大核心参数与执行流程、四种拒绝策略、Executors工具类的缺陷、线程池参数配置公式（CPU/IO密集型）、线程池监控与优雅关闭

## 目录
1. [为什么需要线程池](#1-为什么需要线程池)
2. [Executor框架体系](#2-executor框架体系)
3. [ThreadPoolExecutor七大核心参数](#3-threadpoolexecutor七大核心参数)
4. [线程池执行流程详解](#4-线程池执行流程详解)
5. [四种拒绝策略](#5-四种拒绝策略)
6. [Executors工具类的陷阱](#6-executors工具类的陷阱)
7. [线程池参数配置（CPU密集型/IO密集型）](#7-线程池参数配置cpu密集型io密集型)
8. [ScheduledThreadPoolExecutor定时线程池](#8-scheduledthreadpoolexecutor定时线程池)
9. [ForkJoinPool（JDK 7+）](#9-forkjoinpooljdk-7)
10. [线程池监控与优雅关闭](#10-线程池监控与优雅关闭)
11. [面试高频考点](#11-面试高频考点)

---

## 1. 为什么需要线程池

### 1.1 手动创建线程的弊端

在高并发场景下，频繁创建和销毁线程会带来严重问题：

| 问题 | 说明 |
|------|------|
| 资源开销大 | 每次 `new Thread()` 都需要操作系统分配栈空间、创建内核线程 |
| 线程失控 | 无法限制并发线程数，易导致CPU飙升、系统崩溃 |
| 管理困难 | 缺乏统一调度、监控、生命周期管理机制 |
| 响应变慢 | 任务到达时才创建线程，任务等待耗时 |

> ⚠️ **企业级铁律**：禁止手动 `new Thread()` 执行任务，强制使用线程池。

### 1.2 线程池的核心优势

| 优势 | 说明 |
|------|------|
| **线程复用** | 核心线程常驻，避免频繁创建销毁，降低资源消耗 |
| **流量控制** | 限制最大并发线程数，防止线程过多拖垮系统 |
| **统一管理** | 统一调度、监控、调优，提升系统稳定性 |
| **响应提速** | 核心线程预先创建，任务到达直接执行，无需等待线程创建 |
| **任务缓冲** | 阻塞队列暂存任务，削峰填谷，平滑流量波动 |

---

## 2. Executor框架体系

### 2.1 核心接口层次

```
Executor         (顶层接口: execute(Runnable))
    |
ExecutorService  (子接口: submit, shutdown, awaitTermination...)
    |
    +-- ThreadPoolExecutor          (核心实现)
    +-- ScheduledThreadPoolExecutor (定时任务)
    +-- ForkJoinPool                (工作窃取, JDK 7+)
```

| 接口/类 | 作用 |
|---------|------|
| `Executor` | 顶层接口，定义 `void execute(Runnable)` |
| `ExecutorService` | 扩展接口，提供 `submit()`、`shutdown()`、`awaitTermination()` 等方法 |
| `ThreadPoolExecutor` | 核心实现类，提供完整的线程池参数控制，生产环境直接使用 |
| `ScheduledExecutorService` | 定时任务接口，扩展 `schedule()`、`scheduleAtFixedRate()` |
| `ScheduledThreadPoolExecutor` | 定时线程池实现，替代 `Timer` |
| `ForkJoinPool` | JDK 7+ 引入，基于工作窃取（Work-Stealing）的分治线程池 |
| `Executors` | 工具类，提供快捷创建工厂方法（**生产慎用**） |

### 2.2 Executors 提供的快捷工厂

| 方法 | 返回 | 特点 | 生产推荐 |
|------|------|------|:--------:|
| `newFixedThreadPool(n)` | `ThreadPoolExecutor` | 固定核心线程，无界队列 | ❌ |
| `newCachedThreadPool()` | `ThreadPoolExecutor` | 核心0，最大无限，60s超时 | ❌ |
| `newSingleThreadExecutor()` | `ThreadPoolExecutor` | 单线程，无界队列 | ❌ |
| `newScheduledThreadPool(n)` | `ScheduledThreadPoolExecutor` | 定时/周期任务 | ⚠️ 有界队列可接受 |
| `newWorkStealingPool(n)` | `ForkJoinPool` | JDK 8+，工作窃取 | ✅ |

> ⚠️ `FixedThreadPool` 和 `SingleThreadExecutor` 内部使用无界 `LinkedBlockingQueue`，任务堆积会导致 OOM。`CachedThreadPool` 最大线程数为 `Integer.MAX_VALUE`，也易导致 OOM。

---

## 3. ThreadPoolExecutor七大核心参数

### 3.1 构造方法签名

```java
public ThreadPoolExecutor(
    int corePoolSize,                          // ① 核心线程数
    int maximumPoolSize,                       // ② 最大线程数
    long keepAliveTime,                        // ③ 非核心线程空闲超时时间
    TimeUnit unit,                             // ④ 时间单位
    BlockingQueue<Runnable> workQueue,         // ⑤ 工作队列（任务缓冲）
    ThreadFactory threadFactory,               // ⑥ 线程工厂（自定义名称）
    RejectedExecutionHandler handler           // ⑦ 拒绝策略
);
```

### 3.2 参数详解

| 参数 | 含义 | 说明 |
|------|------|------|
| `corePoolSize` | 核心线程数 | 线程池常驻线程数，即使空闲也不销毁（除非 `allowCoreThreadTimeOut(true)`），新任务到达时优先复用核心线程 |
| `maximumPoolSize` | 最大线程数 | 线程池允许创建的最大线程数量，包含核心+非核心线程，超过则执行拒绝策略 |
| `keepAliveTime` | 非核心线程超时 | 非核心线程空闲超过此时间自动销毁，`allowCoreThreadTimeOut(true)` 时核心线程也受此控制 |
| `unit` | 时间单位 | `TimeUnit.SECONDS`、`MILLISECONDS` 等 |
| `workQueue` | 工作队列 | 核心线程满时，新任务入队等待，详见下方队列类型对比 |
| `threadFactory` | 线程工厂 | 自定义线程名、是否为守护线程，便于线上排查 |
| `handler` | 拒绝策略 | 线程池 + 队列全部满时触发，详见第5节 |

### 3.3 工作队列（BlockingQueue）对比

| 队列 | 特性 | 有界/无界 | 典型场景 |
|------|------|:---------:|----------|
| `ArrayBlockingQueue` | 数组结构，FIFO，有界 | 有界 | 生产推荐，防止OOM |
| `LinkedBlockingQueue` | 链表结构，FIFO | 无界/有界 | Executors默认（无界版本危险） |
| `SynchronousQueue` | 直接交付，不存储任务 | 无容量 | CachedThreadPool 内部使用 |
| `PriorityBlockingQueue` | 优先级排序 | 无界 | 任务需要按优先级执行 |
| `DelayQueue` | 延迟时间出队 | 无界 | 延迟任务调度 |

> 💡 **生产推荐**：始终使用 `ArrayBlockingQueue` 或指定容量的 `LinkedBlockingQueue` 作为有界队列，避免无界队列导致 OOM。

### 3.4 ThreadFactory：自定义线程名

```java
// 方式一：Guava ThreadFactoryBuilder
ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
        .setNameFormat("biz-pool-%d")
        .setDaemon(false)
        .build();

// 方式二：自定义 ThreadFactory 实现
ThreadFactory customFactory = r -> {
    Thread t = new Thread(r);
    t.setName("my-service-thread");
    t.setDaemon(false);
    return t;
};
```

> 💡 自定义线程名是线上排查的基石——Arthas/jstack 中可以快速定位线程归属。

---

## 4. 线程池执行流程详解

### 4.1 核心执行流程

```
                        提交任务 (execute/submit)
                                │
                                ▼
              ┌─────────────────────────────────────┐
              │  工作线程数 < corePoolSize?          │
              └──────────────┬──────────────────────┘
                       是 │       │ 否
                          ▼       ▼
              ┌──────────────────────┐
              │ 创建核心线程执行任务    │
              └──────────────────────┘
                                  │
                          ┌───────┴────────┐
                          │                ▼
                          │    ┌───────────────────────────┐
                          │    │  任务队列是否已满?          │
                          │    └──────────┬────────────────┘
                          │         否 │       │ 是
                          │            ▼       ▼
                          │    ┌──────────────────┐
                          │    │  任务入队等待      │
                          │    │  (队列中等待)      │
                          │    └──────────────────┘
                          │                │
                          │         ┌──────┴────────┐
                          │         ▼               ▼
                          │  ┌───────────────────────────────┐
                          │  │  工作线程数 < maximumPoolSize?  │
                          │  └──────────┬────────────────────┘
                          │       是 │       │ 否
                          │          ▼       ▼
                          │  ┌──────────────────────┐
                          │  │ 创建非核心线程执行任务   │
                          │  └──────────────────────┘
                          │                │
                          │         ┌──────┴──────────┐
                          │         ▼                 ▼
                          │  ┌───────────────────────────────┐
                          │  │   执行拒绝策略 (handler)        │
                          │  └───────────────────────────────┘
                          ▼
              ════════════════════════════════════
               非核心线程空闲 → 超过 keepAliveTime
               → 回收非核心线程，回归核心线程池规模
```

### 4.2 三步判断规则

**核心逻辑：** 提交任务时 ThreadPoolExecutor 按照以下三步逐步扩容：

| 步骤 | 条件 | 行为 |
|:----:|------|------|
| 1 | `workerCount < corePoolSize` | 即使有空闲线程，也新建核心线程执行任务 |
| 2 | 队列未满 | 任务入队等待，核心线程从队列取任务执行 |
| 3 | 队列满 且 `workerCount < maximumPoolSize` | 新建非核心线程执行任务 |
|  | 队列满 且 `workerCount == maximumPoolSize` | 执行拒绝策略 |

> 🎯 **关键理解**：线程池不是"先核心、再队列、最后非核心"的简单三段式，而是**当核心线程都在忙时先缓冲到队列，队列满后才创建非核心线程**。核心线程永远优先执行新任务，队列只是缓冲层。

### 4.3 prestartAllCoreThreads：预启动核心线程

```java
// 默认：核心线程懒加载，有任务到达时才创建
// 预启动：提前创建所有核心线程，避免任务到达时再创建
threadPoolExecutor.prestartAllCoreThreads();
threadPoolExecutor.prestartCoreThread(); // 只启动一个
```

---

## 5. 四种拒绝策略

### 5.1 JDK内置的四种策略

| 策略 | 行为 | 使用场景 |
|------|------|----------|
| `AbortPolicy`（默认） | 抛出 `RejectedExecutionException` | 需要明确感知任务被拒，触发告警 |
| `CallerRunsPolicy` | 提交任务的线程（调用者）自己执行 | **生产推荐**：天然降级，降低提交速度，避免任务丢失 |
| `DiscardPolicy` | 静默丢弃新提交的任务 | 允许丢弃不重要任务（日志、统计等） |
| `DiscardOldestPolicy` | 丢弃队列中最老的（未处理）任务，然后重试提交 | 允许任务过期，追求最新任务优先处理 |

### 5.2 自定义拒绝策略

```java
// 实现 RejectedExecutionHandler 接口
RejectedExecutionHandler customHandler = (r, executor) -> {
    // 1. 记录告警日志
    System.err.printf("[ALERT] Task %s rejected! pool=%d, active=%d, queue=%d%n",
            r.toString(),
            executor.getPoolSize(),
            executor.getActiveCount(),
            executor.getQueue().size());

    // 2. 持久化到数据库/MQ，后续补偿重试
    saveToDeadLetterQueue(r);

    // 3. 如果业务容忍阻塞，可配合 CallerRunsPolicy 或阻塞提交
    if (!executor.isShutdown()) {
        try {
            executor.getQueue().offer(r, 30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
};
```

> ⚠️ **生产经验**：`CallerRunsPolicy` 是最常用的拒绝策略——调用者（通常是业务线程）自己执行慢任务，反向压力自然降低提交速度，避免任务丢弃。

---

## 6. Executors工具类的陷阱

### 6.1 问题根源

```java
// 1. FixedThreadPool - 无界队列 OOM 风险
ExecutorService fixed = Executors.newFixedThreadPool(10);
// 内部实现：new ThreadPoolExecutor(n, n, 0L, TimeUnit.MILLISECONDS,
//                     new LinkedBlockingQueue<Runnable>());
// LinkedBlockingQueue 默认容量 = Integer.MAX_VALUE ≈ 21亿
// 任务堆积时，队列占用全部堆内存 → OOM

// 2. SingleThreadExecutor - 同上，无界队列
ExecutorService single = Executors.newSingleThreadExecutor();
// 内部实现同理，LinkedBlockingQueue 无界

// 3. CachedThreadPool - 最大线程数无限
ExecutorService cached = Executors.newCachedThreadPool();
// 内部实现：new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS,
//                     new SynchronousQueue<Runnable>());
// maxPoolSize = Integer.MAX_VALUE → 无限创建线程 → OOM 或栈溢出
```

### 6.2 Executors 缺陷总结

| 线程池类型 | 缺陷 | 风险等级 |
|-----------|------|:--------:|
| `FixedThreadPool` | 无界 `LinkedBlockingQueue`，任务堆积 OOM | 🔴 高危 |
| `SingleThreadExecutor` | 同上 | 🔴 高危 |
| `CachedThreadPool` | `maxPoolSize` 无限，线程爆炸 OOM | 🔴 高危 |
| `ScheduledThreadPool` | 无界 `DelayedWorkQueue`，定时任务堆积 OOM | 🟡 中危 |
| `newWorkStealingPool` | 内部 `ForkJoinPool`，参数相对可控 | 🟢 可控 |

### 6.3 生产环境推荐方案

```java
// ✅ 正确做法：手动 new ThreadPoolExecutor，指定有界队列
ThreadPoolExecutor executor = new ThreadPoolExecutor(
        4,                              // corePoolSize
        8,                              // maximumPoolSize
        60L, TimeUnit.SECONDS,          // keepAliveTime
        new ArrayBlockingQueue<>(1000), // ✅ 有界队列，防止OOM
        new ThreadFactoryBuilder()      // ✅ 自定义线程名
                .setNameFormat("biz-pool-%d")
                .build(),
        new ThreadPoolExecutor.CallerRunsPolicy() // ✅ 合理拒绝策略
);
```

> ⚠️ **企业级铁律**：禁止使用 `Executors` 创建线程池，必须手动 `new ThreadPoolExecutor`。

---

## 7. 线程池参数配置（CPU密集型/IO密集型）

### 7.1 核心配置公式

#### CPU密集型任务

```bash
# 公式：N_cpu + 1
# N_cpu = Runtime.getRuntime().availableProcessors()
#
# 说明：CPU 密集型指大量计算、无阻塞（如加密、排序、图像处理）
# +1 是为了补偿页缺失等偶尔的等待，使 CPU 利用率最大化
# 线程数建议与 CPU 核心数相当，过多线程只会增加上下文切换
```

#### IO密集型任务

```bash
# 公式：N_cpu * (1 + wait_time / compute_time)
# 
# wait_time  = IO 等待时间（网络/磁盘/数据库）
# compute_time = CPU 计算时间
#
# 示例：8核CPU，IO等待 100ms，计算 10ms
# 8 * (1 + 100/10) = 8 * 11 = 88 线程
```

### 7.2 配置速查表

| 任务类型 | 推荐公式 | 8核示例 | 说明 |
|---------|---------|:-------:|------|
| CPU密集型 | `N_cpu + 1` | 9 | 计算密集，避免上下文切换 |
| IO密集型 | `N_cpu * (1 + wait/compute)` | 20~200 | 根据实际IO等待比计算 |
| 混合型 | 两类线程池隔离 | — | 不同任务分不同的线程池 |
| 纯异步型 | `N_cpu * 2` | 16 | 大量非阻塞调用 |

### 7.3 完整配置示例

```java
import java.util.concurrent.*;

public class ThreadPoolConfigDemo {

    // CPU核心数
    private static final int N_CPU = Runtime.getRuntime().availableProcessors();

    // 获取CPU密集型线程池实例
    public static ThreadPoolExecutor newCpuIntensivePool() {
        return new ThreadPoolExecutor(
                N_CPU + 1,                  // core
                N_CPU * 2,                  // max (保守一点)
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(N_CPU * 100),
                r -> {
                    Thread t = new Thread(r);
                    t.setName("cpu-pool-" + t.getId());
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    // 获取IO密集型线程池实例
    public static ThreadPoolExecutor newIoIntensivePool() {
        // 假设 wait_time / compute_time ≈ 10
        int poolSize = N_CPU * (1 + 10);
        return new ThreadPoolExecutor(
                poolSize,                   // core
                poolSize * 2,               // max
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1000),
                r -> {
                    Thread t = new Thread(r);
                    t.setName("io-pool-" + t.getId());
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
```

### 7.4 线程池隔离原则

```
业务A（订单） → ThreadPoolExecutor A
业务B（支付） → ThreadPoolExecutor B
业务C（日志） → ThreadPoolExecutor C

目的：避免一个业务耗尽线程池，影响其他业务
```

> 💡 **线程池隔离**是高并发微服务架构中的最佳实践——不同业务使用独立的线程池，避免相互干扰。

---

## 8. ScheduledThreadPoolExecutor定时线程池

### 8.1 与 Timer 对比

| 维度 | `Timer` | `ScheduledThreadPoolExecutor` |
|------|---------|-------------------------------|
| 线程数 | 单线程 | 线程池，多线程调度 |
| 异常处理 | 单个任务抛异常 → Timer 整个终止 | 单任务异常不影响其他任务 |
| 时间精度 | 受系统时间调整影响 | 相对时间，稳定 |
| catch异常 | 不捕获 | ✅ 可自定义异常处理 |
| 推荐指数 | ❌ 不推荐 | ✅ 推荐 |

### 8.2 核心方法

```java
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

// 1. 延迟执行一次
scheduler.schedule(() -> System.out.println("延迟任务"),
        5, TimeUnit.SECONDS);

// 2. 固定频率执行（不考虑任务执行时间）
scheduler.scheduleAtFixedRate(() -> System.out.println("每隔2秒"),
        0, 2, TimeUnit.SECONDS);
// 说明：每 2 秒触发一次，忽略任务本身的执行耗时

// 3. 固定延迟执行（考虑任务执行时间）
scheduler.scheduleWithFixedDelay(() -> System.out.println("任务结束后等待3秒"),
        0, 3, TimeUnit.SECONDS);
// 说明：任务执行完毕后，等待 3 秒再执行下一次

// 4. 异常处理：异常不会自动传播，需要 wrap 处理
scheduler.scheduleAtFixedRate(() -> {
    try {
        // 业务逻辑
    } catch (Exception e) {
        // 记录异常，否则后续任务会静默取消
        System.err.println("定时任务异常: " + e.getMessage());
    }
}, 0, 5, TimeUnit.SECONDS);
```

> 💡 使用 `scheduleAtFixedRate` 时，异常必须自行捕获，否则任务会在第一次异常后静默取消不再执行。

---

## 9. ForkJoinPool（JDK 7+）

### 9.1 核心概念

`ForkJoinPool` 是 JDK 7 引入的**分治（Divide-and-Conquer）线程池**，其核心思想是将大任务拆分为小任务，并行执行，再合并结果。

| 核心概念 | 说明 |
|---------|------|
| **Fork**（拆分） | 将大任务递归拆分为足够小的子任务 |
| **Join**（合并） | 等待子任务完成，合并结果 |
| **Work-Stealing（工作窃取）** | 空闲线程从其他线程的任务队列尾部"偷"任务执行，提高CPU利用率 |
| **双端队列** | 每个线程维护一个双端队列，自己从头部取任务，偷任务从尾部取 |

### 9.2 核心类

| 类 | 作用 |
|----|------|
| `ForkJoinPool` | 线程池实现，替代 `ThreadPoolExecutor` 用于分治场景 |
| `RecursiveTask<V>` | 有返回值的拆分任务 |
| `RecursiveAction` | 无返回值的拆分任务 |

### 9.3 示例：并行求和

```java
import java.util.concurrent.*;

public class ForkJoinSumExample {

    static class SumTask extends RecursiveTask<Long> {
        private static final int THRESHOLD = 1_000; // 不再拆分的阈值
        private final long[] array;
        private final int start;
        private final int end;

        SumTask(long[] array, int start, int end) {
            this.array = array;
            this.start = start;
            this.end = end;
        }

        @Override
        protected Long compute() {
            int length = end - start;
            if (length <= THRESHOLD) {
                // 足够小，直接计算
                long sum = 0;
                for (int i = start; i < end; i++) {
                    sum += array[i];
                }
                return sum;
            }
            // 拆分为两个子任务
            int mid = start + length / 2;
            SumTask left = new SumTask(array, start, mid);
            SumTask right = new SumTask(array, mid, end);
            left.fork();  // 异步执行左任务
            Long rightResult = right.compute(); // 同步执行右任务
            Long leftResult = left.join();      // 等待左任务结果
            return leftResult + rightResult;
        }
    }

    public static void main(String[] args) {
        long[] array = new long[10_000];
        for (int i = 0; i < array.length; i++) {
            array[i] = i + 1;
        }

        // JDK 8+ 方式：ForkJoinPool.commonPool()
        ForkJoinPool pool = new ForkJoinPool(); // 并行度 = CPU核心数
        Long result = pool.invoke(new SumTask(array, 0, array.length));
        System.out.println("Sum: " + result); // 50005000

        // 或使用 Executors 创建
        // ExecutorService forkJoinPool = Executors.newWorkStealingPool();
    }
}
```

### 9.4 适用场景

| 场景 | 说明 |
|------|------|
| 大数据并行计算 | 数组排序、矩阵运算、海量数据统计 |
| 递归分治算法 | 归并排序、快速排序、二分查找 |
| `CompletableFuture` 默认线程池 | JDK 8+ 异步编程底层使用 `ForkJoinPool.commonPool()` |
| `Parallel Stream` | `Arrays.parallelSort()`、`list.parallelStream()` |

---

## 10. 线程池监控与优雅关闭

### 10.1 线程池监控指标

| 方法 | 返回 | 含义 |
|------|------|------|
| `getPoolSize()` | `int` | 当前线程池中的线程总数（核心+非核心） |
| `getActiveCount()` | `int` | 正在执行任务的活跃线程数 |
| `getCorePoolSize()` | `int` | 核心线程数配置 |
| `getMaximumPoolSize()` | `int` | 最大线程数配置 |
| `getQueue().size()` | `int` | 当前队列中的等待任务数 |
| `getQueue().remainingCapacity()` | `int` | 队列剩余可用容量 |
| `getCompletedTaskCount()` | `long` | 已完成的任务总数 |
| `getTaskCount()` | `long` | 已提交的任务总数（含队列+已执行） |
| `getLargestPoolSize()` | `int` | 线程池历史最大线程数（峰值） |

### 10.2 监控工具类

```java
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolMonitor {

    private final ThreadPoolExecutor executor;
    private final String poolName;

    public ThreadPoolMonitor(ThreadPoolExecutor executor, String poolName) {
        this.executor = executor;
        this.poolName = poolName;
    }

    // 打印线程池状态
    public String status() {
        return String.format(
                "[%s] Active: %d/%d, Queue: %d/%d, Completed: %d, Pool: %d (peak: %d)%n",
                poolName,
                executor.getActiveCount(),
                executor.getPoolSize(),
                executor.getQueue().size(),
                executor.getQueue().remainingCapacity(),
                executor.getCompletedTaskCount(),
                executor.getPoolSize(),
                executor.getLargestPoolSize()
        );
    }

    // 启动定期监控（配合日志/告警）
    public void startMonitoring(long interval, TimeUnit unit) {
        Thread monitor = new Thread(() -> {
            while (!executor.isTerminated()) {
                System.out.print(status());
                // 告警阈值判断
                if (executor.getQueue().remainingCapacity() < 100) {
                    System.err.printf("[ALERT] %s queue is almost full!%n", poolName);
                }
                try {
                    Thread.sleep(unit.toMillis(interval));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, poolName + "-monitor");
        monitor.setDaemon(true);
        monitor.start();
    }
}
```

### 10.3 优雅关闭三阶段

```java
public class GracefulShutdownDemo {

    private final ThreadPoolExecutor executor;

    public GracefulShutdownDemo(ThreadPoolExecutor executor) {
        this.executor = executor;
    }

    /**
     * 优雅关闭线程池（三阶段法）
     */
    public void shutdown() {
        // 阶段一：拒绝新任务提交
        // - shutdown() 后 execute/submit 会抛 RejectedExecutionException
        // - 已提交任务（含队列中等待的）会继续执行完毕
        executor.shutdown();

        try {
            // 阶段二：等待已提交的任务完成（最多等60秒）
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                // 阶段三：60秒未完成 → 强制终止剩余任务
                // - shutdownNow() 返回未执行的任务列表
                // - 正在执行的任务接收 Thread.interrupt()
                executor.shutdownNow();

                // 再等待一小段时间，让线程响应中断
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    System.err.println("线程池未能完全终止");
                }
            }
        } catch (InterruptedException e) {
            // 当前线程被中断 → 立即强制关闭
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```

### 10.4 完整实践模板

```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 企业级线程池最佳实践
 * 特性：有界队列 + 自定义线程名 + CallerRunsPolicy + 监控 + 优雅关闭
 */
public class BestPracticeTemplate {

    private final ThreadPoolExecutor executor;

    public BestPracticeTemplate(int core, int max, int queueSize, String poolName) {
        AtomicInteger threadNum = new AtomicInteger(1);
        this.executor = new ThreadPoolExecutor(
                core,
                max,
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueSize),
                r -> {
                    Thread t = new Thread(r);
                    t.setName(poolName + "-" + threadNum.getAndIncrement());
                    t.setDaemon(false);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        // 预启动核心线程
        executor.prestartAllCoreThreads();
    }

    public ThreadPoolExecutor getExecutor() {
        return executor;
    }

    // 提交任务
    public void submit(Runnable task) {
        executor.execute(task);
    }

    // 打印监控
    public String status() {
        return String.format(
                "Active: %d/%d, Queue: %d/%d, Completed: %d, PoolPeak: %d",
                executor.getActiveCount(),
                executor.getPoolSize(),
                executor.getQueue().size(),
                executor.getQueue().remainingCapacity(),
                executor.getCompletedTaskCount(),
                executor.getLargestPoolSize()
        );
    }

    // 优雅关闭
    public void destroy() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    System.err.println("Force shutdown failed");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```

---

## 11. 面试高频考点

### 11.1 基础问答

| 问题 | 核心回答 |
|------|---------|
| **线程池的七大核心参数** | corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler |
| **线程池执行流程** | 核心线程→队列缓冲→非核心线程→拒绝策略 |
| **四种拒绝策略** | AbortPolicy（抛异常）、CallerRunsPolicy（调用者执行）、DiscardPolicy（丢弃）、DiscardOldestPolicy（丢弃最老） |
| **为什么禁止 Executors** | FixedThreadPool/SingleThreadExecutor 无界队列 OOM，CachedThreadPool 无限线程 OOM |

### 11.2 进阶问题

| 问题 | 核心要点 |
|------|---------|
| **核心线程是否会超时回收** | 默认不会；`allowCoreThreadTimeOut(true)` 后核心也受 keepAliveTime 控制 |
| **线程池大小如何配置** | CPU密集: `N_cpu+1`；IO密集: `N_cpu*(1+wait/compute)` |
| **submit() vs execute() 区别** | `submit()` 返回 `Future`，可获结果/异常；`execute()` 无返回值 |
| **shutdown() vs shutdownNow()** | `shutdown()` 优雅关闭不中断正在执行的任务；`shutdownNow()` 尝试中断所有任务 |
| **如何获取线程池运行状况** | `getActiveCount()`, `getQueue().size()`, `getCompletedTaskCount()` 等 |
| **ForkJoinPool 的核心思想** | 分治法 + 工作窃取（Work-Stealing） |

### 11.3 常见笔试/手写

```java
// 手写 ThreadPoolExecutor 创建（标准七参数）
ThreadPoolExecutor executor = new ThreadPoolExecutor(
        5,                              // core
        10,                             // max
        60L, TimeUnit.SECONDS,          // keepAlive
        new ArrayBlockingQueue<>(100),  // 有界队列
        r -> {
            Thread t = new Thread(r);
            t.setName("my-pool-worker");
            return t;
        },
        new ThreadPoolExecutor.CallerRunsPolicy()
);
```

```java
// 手写线程池优雅关闭
public void shutdownPool(ExecutorService pool, long timeout, TimeUnit unit) {
    pool.shutdown(); // 拒绝新任务
    try {
        if (!pool.awaitTermination(timeout, unit)) {
            pool.shutdownNow(); // 强制取消
            if (!pool.awaitTermination(timeout, unit)) {
                System.err.println("Pool did not terminate");
            }
        }
    } catch (InterruptedException e) {
        pool.shutdownNow();
        Thread.currentThread().interrupt();
    }
}
```

---

> 🎯 **本章核心总结**
>
> 1. **线程池是企业级并发基石**，禁止手动 `new Thread()`，必须使用 `ThreadPoolExecutor`
> 2. **七大参数**：`corePoolSize`（常驻核心）、`maximumPoolSize`（扩容上限）、`keepAliveTime`（超时回收）、`workQueue`（有界队列防OOM）、`threadFactory`（命名溯源）、`handler`（降级兜底）
> 3. **执行流程**：核心线程 → 队列缓冲 → 非核心线程扩容 → 拒绝策略兜底，队列缓冲位于扩容之前是关键特性
> 4. **拒绝策略**：`CallerRunsPolicy` 生产首选，天然降源自适应
> 5. **Executors 工具类**：有 OOM 陷阱，**生产禁用**
> 6. **配置公式**：CPU 密集 `N_cpu+1`，IO 密集 `N_cpu*(1+wait/compute)`
> 7. **优雅关闭**：`shutdown` → `awaitTermination` → `shutdownNow` 三阶段，保证任务不丢失
> 8. **监控与隔离**：定期监控线程池状态，不同业务线程池隔离避免相互影响
