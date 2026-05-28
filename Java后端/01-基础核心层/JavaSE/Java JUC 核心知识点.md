# Java JUC 核心知识点
JUC 是 **java.util.concurrent** 工具包的简称，是 Java 处理**高并发、多线程**的核心 API，从 JDK 1.5 开始引入，用于解决传统多线程（Thread、synchronized）性能低、功能弱的问题。


---

## 一、JUC 核心架构
JUC 分为三大核心组成：
1. **线程与线程池**：Thread、Runnable、Callable、Executor、ThreadPoolExecutor
2. **锁机制**：Lock 接口、ReentrantLock、ReentrantReadWriteLock、StampedLock
3. **并发容器**：ConcurrentHashMap、CopyOnWriteArrayList、BlockingQueue
4. **同步工具**：CountDownLatch、CyclicBarrier、Semaphore、Exchanger
5. **原子类**：AtomicInteger、AtomicLong、Unsafe（CAS 底层）
6. **线程调度**：Future、FutureTask、CompletableFuture

---

## 二、基础必备：线程创建方式
JUC 比传统多线程新增了**带返回值**的线程创建方式：
1. **Thread + Runnable**：无返回值
2. **Callable + FutureTask**：**有返回值、可抛异常**（JUC 核心）

```java
// Callable 示例（JUC 推荐）
Callable<Integer> callable = () -> {
    return 1 + 1;
};
FutureTask<Integer> task = new FutureTask<>(callable);
new Thread(task).start();
Integer result = task.get(); // 阻塞获取结果
```

---

## 三、JUC 锁体系（重点）

### 1. Lock 接口 vs synchronized
| 特性 | synchronized | Lock（JUC） |
|------|--------------|-------------|
| 实现 | JVM 关键字 | Java 接口 |
| 释放锁 | 自动释放 | 必须手动 unlock() |
| 等待可中断 | 不可中断 | 可中断 |
| 公平锁 | 非公平 | 支持公平/非公平 |
| 尝试获取锁 | 不支持 | tryLock() 支持 |

### 2. 常用锁实现
- **ReentrantLock**：可重入锁（最常用，替代 synchronized）
- **ReentrantReadWriteLock**：读写锁（读共享、写独占，适合读多写少）
- **StampedLock**：读写锁升级版，性能更高（JDK 8）

**锁必须规范使用**：
```java
Lock lock = new ReentrantLock();
lock.lock(); // 加锁
try {
    // 业务逻辑
} finally {
    lock.unlock(); // 必须在 finally 释放锁
}
```

### 3. 核心概念
- **可重入锁**：同一个线程可以多次获取同一把锁，避免死锁
- **公平锁**：按申请顺序获取锁；非公平锁：线程可插队（默认，性能高）
- **独占锁**：同一时间只有一个线程持有（ReentrantLock）
- **共享锁**：多个线程可同时持有（读锁）

---

## 四、CAS 与原子类（JUC 底层核心）

### 1. CAS（Compare And Swap）比较并交换
- **无锁算法**，靠 CPU 原语保证原子性，比锁性能更高
- 三个参数：**内存值、预期值、新值**
- 只有内存值 = 预期值时，才更新为新值
- 缺点：ABA 问题、循环耗时、只能保证一个变量原子性

### 2. 原子类（JUC 提供）
基于 CAS 实现，保证变量操作原子性，**无锁高效**：
- 基本类型：`AtomicInteger`、`AtomicLong`、`AtomicBoolean`
- 引用类型：`AtomicReference`
- 数组类型：`AtomicIntegerArray`
- 原子更新字段：`AtomicIntegerFieldUpdater`

```java
AtomicInteger num = new AtomicInteger(0);
num.incrementAndGet(); // 自增，线程安全
```

---

## 五、并发容器（面试高频）
JUC 提供线程安全的容器，替代低效的 `Vector`、`Hashtable`。

### 1. ConcurrentHashMap
- **线程安全、高性能**的哈希表
- JDK 1.7：分段锁（Segment）
- JDK 1.8：**CAS + synchronized**，粒度更细，性能大幅提升
- 关键：key、value 不能为 null

### 2. CopyOnWriteArrayList
- 写时复制容器：**写时复制新数组，读无锁**
- 适用：读多写少场景
- 缺点：写性能低，占用内存

### 3. 阻塞队列 BlockingQueue
- 线程通信核心工具，**生产者-消费者模式**必备
- 常用实现：
  - `ArrayBlockingQueue`：有界数组队列
  - `LinkedBlockingQueue`：无界链表队列
  - `SynchronousQueue`：不存储元素，直接传递

---

## 六、线程同步工具类
用于控制线程执行顺序、并发数量，是 JUC 高频工具。

### 1. CountDownLatch
- **倒计时门闩**：一个线程等待其他 N 个线程完成后再执行
- 只能使用一次

### 2. CyclicBarrier
- **循环屏障**：一组线程互相等待，全部到达后一起执行
- 可重复使用

### 3. Semaphore
- **信号量**：控制**同时执行的线程数量**（限流）

### 4. Exchanger
- 线程间数据交换工具

---

## 七、线程池（JUC 最重要知识点）

### 1. 为什么用线程池？
- 降低资源消耗：复用线程，避免频繁创建销毁
- 提高响应速度：任务来直接执行
- 管理线程：控制并发数、定时执行

### 2. 核心参数（ThreadPoolExecutor 7 大参数）
```java
new ThreadPoolExecutor(
    corePoolSize,  // 核心线程数
    maximumPoolSize, // 最大线程数
    keepAliveTime, // 非核心线程空闲时间
    unit, // 时间单位
    workQueue, // 阻塞队列
    threadFactory, // 线程工厂
    handler // 拒绝策略
);
```

### 3. 执行流程
1. 任务 → 核心线程未满 → 创建核心线程
2. 核心线程满 → 放入阻塞队列
3. 队列满 → 创建非核心线程
4. 总线程数达到最大 → 执行**拒绝策略**

### 4. 拒绝策略（4 种）
- AbortPolicy：直接抛异常（默认）
- CallerRunsPolicy：调用者线程执行
- DiscardPolicy：丢弃任务
- DiscardOldestPolicy：丢弃队列最老任务

### 5. Executors 工具类（不推荐生产使用）
- `newFixedThreadPool`：固定线程
- `newSingleThreadExecutor`：单线程
- `newCachedThreadPool`：缓存线程
- **生产禁止用**：允许队列最大长度为 Integer.MAX_VALUE，会 OOM

---

## 八、Future 与 CompletableFuture

### 1. Future
- 获取异步线程的执行结果
- 缺点：`get()` 会阻塞，功能单一

### 2. CompletableFuture（JDK 8）
- 异步编程神器，**非阻塞、链式调用、多任务组合**
- 支持：回调、多任务并行、依赖执行

```java
CompletableFuture.supplyAsync(() -> 1)
    .thenApply(i -> i+1)
    .thenAccept(System.out::println);
```

---

## 九、JUC 核心原理：AQS
**AbstractQueuedSynchronizer** 抽象队列同步器
- JUC 锁和同步工具的**底层基石**（ReentrantLock、CountDownLatch 都基于 AQS）
- 核心：
  1. 用 `volatile int state` 表示同步状态
  2. 用 **CLH 双向队列** 存储等待线程
  3. CAS 修改 state 状态

---

## 十、面试高频总结（必背）
1. **synchronized 和 ReentrantLock 区别**
2. **CAS 原理、优缺点、ABA 问题解决**
3. **ConcurrentHashMap 1.7 和 1.8 区别**
4. **线程池 7 大参数、执行流程、拒绝策略**
5. **CountDownLatch 和 CyclicBarrier 区别**
6. **volatile 和 synchronized 区别**
7. **ThreadLocal 原理与使用场景**
8. **阻塞队列作用、生产者消费者实现**

---

### 总结
1. JUC 是 Java 高并发核心，围绕**线程池、锁、CAS、并发容器、同步工具**展开
2. 底层依赖 **AQS + CAS**，实现高效并发
3. 生产开发：**必须用线程池**、优先用 **ConcurrentHashMap**、读多写少用 **CopyOnWriteArrayList**
4. 面试重点：线程池、AQS、CAS、ConcurrentHashMap、锁机制
