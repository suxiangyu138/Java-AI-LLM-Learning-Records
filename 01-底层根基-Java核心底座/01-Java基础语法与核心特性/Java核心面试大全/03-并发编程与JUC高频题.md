# 03 - 并发编程与 JUC 高频题

> 🎯 并发是面试"重灾区"也是"分水岭" — 最容易拉开差距。核心链路：synchronized 锁升级 → volatile/JMM → CAS → AQS → 线程池 → ThreadLocal → 虚拟线程

---

## 目录

1. [线程基础](#1-线程基础)
2. [synchronized 与锁升级](#2-synchronized-与锁升级)
3. [volatile 与 Java 内存模型](#3-volatile-与-java-内存模型)
4. [CAS 与原子类](#4-cas-与原子类)
5. [AQS 与 ReentrantLock](#5-aqs-与-reentrantlock)
6. [线程池](#6-线程池)
7. [ThreadLocal](#7-threadlocal)
8. [JUC 工具类与 CompletableFuture](#8-juc-工具类与-completablefuture)
9. [Java 21 虚拟线程](#9-java-21-虚拟线程)

---

## 1. 线程基础

### 1.1 线程创建的四种方式与对比？

| 方式 | 特点 | 场景 |
|------|------|------|
| 继承 Thread | 简单，但 Java 单继承受限 | 极少用 |
| 实现 Runnable | 无返回值，推荐 | 常规任务 |
| 实现 Callable + FutureTask | **有返回值、可抛异常** | 需要结果 |
| 线程池（ExecutorService） | 复用线程、控制并发度 | **生产首选** |

```java
// Callable + Future
ExecutorService pool = Executors.newFixedThreadPool(2);
Future<Integer> f = pool.submit(() -> 1 + 2);
Integer result = f.get();      // 阻塞等待结果
```

**追问：** Future.get 与 CompletableFuture？→ get 阻塞、异常难处理、多任务编排困难；CompletableFuture 提供回调/编排（thenApply、allOf）。线程生命周期？→ NEW → RUNNABLE → BLOCKED/WAITING/TIMED_WAITING → TERMINATED。

### 1.2 sleep / wait / join / yield 的区别？

| 方法 | 释放锁 | 作用 | 调用位置 |
|------|:---:|------|----------|
| Thread.sleep | ❌ 不释放 | 让出 CPU 指定毫秒 | 任意 |
| Object.wait | ✅ 释放 | 等待通知，需在同步块内 | synchronized 内 |
| Thread.join | ❌（等待线程） | 主线程等待子线程结束 | 任意 |
| Thread.yield | ❌ | 谦让，提示调度器 | 任意 |

```java
// 经典等待-通知模式
synchronized (lock) {
    while (!ready) {      // 用 while 防虚假唤醒
        lock.wait();
    }
    // 条件满足后处理
    lock.notifyAll();
}
```

**追问：** 为什么 wait 必须配 synchronized？→ wait 释放锁的前提是持有锁（否则无法保证通知者看到状态）。虚假唤醒？→ 多线程 notifyAll 后条件未必满足，必须 while 循环重查条件。

---

## 2. synchronized 与锁升级

### 2.1 synchronized 的实现原理？

**参考答案（三层理解）：**

| 层 | 机制 |
|----|------|
| 字节码层 | monitorenter / monitorexit（方法级用 ACC_SYNCHRONIZED） |
| 对象头层 | Mark Word 记录锁状态（偏向线程 ID / 指向锁记录 / Monitor 指针） |
| 语义层 | 进入临界区必须持有对象的 Monitor（管程） |

**锁升级路径（JDK 8 默认）：**

```text
无锁 → 偏向锁 → 轻量级锁 → 重量级锁（不可逆升级）
（JDK15 起默认禁用偏向锁，JDK 后续版本逐步移除 — 因为竞争场景下偏向锁收益低、复杂度高）
```

| 锁状态 | 机制 | 适用场景 |
|--------|------|----------|
| 偏向锁 | 只有一个线程访问，记录线程 ID，重入无需 CAS | 单线程反复进入 |
| 轻量级锁 | 线程间交替访问，CAS 拷贝 Mark Word 到栈锁记录 | 低竞争 |
| 重量级锁 | 竞争激烈，锁对象关联 Monitor，未抢到进阻塞队列 | 高竞争 |

**追问：**
- 为什么要有锁升级而不是一步到位？→ 大部分锁竞争极低，用最小成本保护；升级是"按需付费"
- 为什么是**非公平**锁？→ Monitor 阻塞唤醒是操作系统调度，无法保证 FIFO；公平锁成本高
- 锁消除和锁粗化？→ JIT 逃逸分析发现局部对象无逃逸则消除锁；连续加锁同一对象则合并扩大锁范围
- 自旋锁？→ 重量级锁竞争时先自旋（CPU 空转）避免立即阻塞，自适应自旋会动态调整次数

**记忆点：**
> 🎯 synchronized 面试讲"三个为什么"：为什么分级（竞争成本不同）、为什么不可逆（设计简单，避免反复竞争开销）、为什么非公平（OS 调度决定）。

### 2.2 synchronized vs ReentrantLock？

| 对比 | synchronized | ReentrantLock |
|------|--------------|----------------|
| 实现 | JVM 层（monitor） | JDK 层（AQS） |
| 公平性 | 非公平 | 默认非公平，可公平 |
| 中断响应 | 不支持 | lockInterruptibly 支持 |
| 超时等待 | 不支持 | tryLock(timeout) |
| 多个条件 | 一个 wait/notify | newCondition() 多个条件队列 |
| 语法 | 简洁（自动释放） | 必须 finally unlock |

**追问：** 为什么 Spring/并发框架很多用 ReentrantLock？→ 需要超时、可中断、多条件时 ReentrantLock 更强；普通同步 synchronized 即可。可重入的实现？→ 持有线程计数（Monitor 的 recursion 计数 / AQS 的 state 累加），同一线程重入只计数不阻塞。

---

## 3. volatile 与 Java 内存模型

### 3.1 volatile 能保证什么？不能保证什么？

**参考答案：**

| 保证 | 机制 | 说明 |
|------|------|------|
| ✅ 可见性 | 写操作立即刷主存 + 读操作失效本地缓存（volatile 写会插入内存屏障） | 其他线程立即可见 |
| ✅ 有序性 | 禁止指令重排（内存屏障：LoadLoad/StoreStore 等） | 防止重排序破坏语义 |
| ❌ 原子性 | **不保证** | 自增 i++ 仍是"读-改-写"三步，可能丢更新 |

```java
// 正确用法：状态标志（一写多读）
volatile boolean stop = false;
// 错误用法：计数
volatile int count = 0; count++;   // ❌ 并发下丢更新
```

**追问：** 什么场景必须 volatile？→ ① 状态标志 ② DCL 单例的 instance（防止指令重排导致半初始化对象被引用）。volatile 和 synchronized 的关系？→ volatile 是轻量级同步，只解决可见性/有序性；synchronized 通过锁的 happens-before 规则同时保证原子性。

### 3.2 happens-before 规则？

**参考答案（重点记 6 条）：**

| 规则 | 内容 |
|------|------|
| 程序次序 | 单线程内代码按书写顺序 |
| 锁规则 | 解锁 happens-before 后续加锁 |
| volatile 规则 | volatile 写 happens-before 后续读 |
| 传递性 | A→B 且 B→C 则 A→C |
| 线程启动 | start() happens-before 线程内所有操作 |
| 线程终止 | 线程内操作 happens-before join() 返回 |

**追问：** JMM 为什么允许重排序？→ 处理器流水线优化，单线程语义不变（as-if-serial）；重排序只破坏多线程可见性，所以需要屏障。volatile 与锁的区别？→ volatile 是"无锁"的，不阻塞、无上下文切换，但能力弱。

---

## 4. CAS 与原子类

### 4.1 CAS 原理与 ABA 问题？

**参考答案：**
- CAS（Compare And Swap）：比较内存值 == 期望值，相等则更新，**不相等则重试**（循环）
- 底层：`Unsafe.compareAndSwapInt` → CPU 原子指令（cmpxchg），保证比较+交换一条指令完成

```java
// 典型实现：AtomicInteger.incrementAndGet（自旋）
for (;;) {
    int cur = get();
    if (compareAndSet(cur, cur + 1)) return cur + 1;   // 失败重试
}
```

**ABA 问题**：值从 A 变 B 又变回 A，CAS 误判"没变过" → 用 **AtomicStampedReference**（带版本号）解决。

**追问：** CAS 的缺点？→ ① 自旋 CPU 空转（高竞争下）② ABA ③ 只能操作单个变量（原子更新多个用 AtomicReference 包装对象）。LongAdder 为什么比 AtomicLong 快？→ 分段累加（Cell 数组），竞争分散到多个槽位，最后 sum 时合并 — 适合"读少写多"的高并发计数。

### 4.2 乐观锁 vs 悲观锁？

| 对比 | 乐观锁 | 悲观锁 |
|------|--------|--------|
| 思想 | 冲突少，先操作再验证（CAS/版本号） | 冲突多，先加锁再操作 |
| 实现 | CAS、数据库 version 字段 | synchronized、ReentrantLock、数据库 for update |
| 适用 | 读多写少 | 写多竞争激烈 |
| 代价 | 失败重试 | 阻塞、死锁风险 |

**追问：** 数据库乐观锁怎么实现？→ `UPDATE ... SET version = version + 1 WHERE id = ? AND version = ?`，影响行数为 0 则重试。什么时候乐观锁反而差？→ 冲突率高时大量重试，性能劣于锁。

---

## 5. AQS 与 ReentrantLock

### 5.1 AQS 是什么？核心原理？

**参考答案：**
- AQS（AbstractQueuedSynchronizer）：JUC 锁和同步器的**模板基类**（ReentrantLock、Semaphore、CountDownLatch、ReentrantReadWriteLock 都基于它）
- 核心三件套：**volatile int state**（同步状态）+ **CLH 双向队列**（等待线程）+ **模板方法模式**（tryAcquire/tryRelease 由子类实现）

```text
获取资源流程：
① CAS 尝试修改 state → 成功则持有
② 失败 → 封装为 Node 入队
③ 阻塞（park）→ 被唤醒后重新竞争
```

**追问：** 公平锁与非公平锁在 AQS 里差在哪？→ 非公平锁获取时先直接 CAS 抢一次（插队）；公平锁先查队列是否有前驱（hasQueuedPredecessors）。为什么非公平性能好？→ 刚释放锁的线程可能还在 CPU 上，直接抢成功率高，减少上下文切换。

### 5.2 ReentrantLock 的可重入如何实现？

**参考答案：** state 计数：首次加锁 state=1，重入 state++，每次 unlock state--，归零才真正释放。

```java
lock.lock();
try {
    lock.lock();     // 可重入：state 1 → 2
    // ...
    lock.unlock();   // state 2 → 1
} finally {
    lock.unlock();   // state 1 → 0，真正释放
}
```

**追问：** 与读写锁？→ ReentrantReadWriteLock：读读共享、写写互斥、读写互斥；StampedLock 更先进（乐观读，JDK8+，适合读多写少但并发更高场景）。

---

## 6. 线程池

### 6.1 ThreadPoolExecutor 七个参数？

| 参数 | 含义 | 记忆 |
|------|------|------|
| corePoolSize | 核心线程数（常驻） | 常态并发量 |
| maximumPoolSize | 最大线程数 | 峰值并发量 |
| keepAliveTime | 非核心线程空闲存活时间 | 回收节奏 |
| workQueue | 任务队列（阻塞队列） | 缓冲池 |
| threadFactory | 线程工厂 | 命名/守护标记 |
| handler | 拒绝策略 | 队列+线程全满时 |

**execute 流程：**

```text
提交任务 → ① 核心线程未满？→ 新建核心线程
        → ② 已满 → 入队（workQueue.offer）
        → ③ 队列满 → 新建非核心线程（直到 maximum）
        → ④ 都满 → 拒绝策略
```

**四种拒绝策略：** AbortPolicy（默认，抛异常）、CallerRunsPolicy（调用者线程自己跑）、DiscardPolicy（丢弃）、DiscardOldestPolicy（丢弃最老任务）。

**追问：**
- 为什么 core 线程不回收？→ 减少创建销毁开销；核心线程在 allowCoreThreadTimeOut 后可回收
- 队列选型？→ 有界 ArrayBlockingQueue（防 OOM，推荐）+ CallerRunsPolicy；无界 LinkedBlockingQueue 队列无限增长有 OOM 风险

### 6.2 为什么禁止用 Executors 创建线程池？

| Executors 方法 | 隐患 |
|----------------|------|
| newFixedThreadPool | 无界队列（Integer.MAX_VALUE）→ **OOM** |
| newCachedThreadPool | 最大线程数 Integer.MAX_VALUE → **线程数爆炸** |
| newSingleThreadExecutor | 无界队列 |

```java
// ✅ 生产用法：手动创建 + 有界队列 + 自定义命名 + 拒绝策略
ThreadPoolExecutor pool = new ThreadPoolExecutor(
    4, 8, 60, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(1000),
    new ThreadFactory() { ... 命名 "order-pool-" + n },
    new ThreadPoolExecutor.CallerRunsPolicy());
```

**追问：** 核心线程数怎么定？→ CPU 密集：`CPU核数 + 1`；IO 密集：`CPU核数 × (1 + 等待时间/计算时间)`（或经验值 2 倍）。线程池状态？→ RUNNING/SHUTDOWN/STOP/TIDYING/TERMINATED；shutdown() 与 shutdownNow()：前者等任务完成，后者 interrupt 中断。

**记忆点：**
> 🎯 线程池背"3+1"：3 个数（core/max/队列）、1 个策略（拒绝策略）；永远手动 new，永远有界队列，线程工厂命名（排查问题时一眼定位）。

### 6.3 线程池异常处理？

**参考答案：**
- execute() 提交的任务异常会吞掉（线程死亡后由线程工厂新线程替代，异常只在 worker 日志）
- **submit() 返回的 Future.get() 会抛异常**（包装 ExecutionException）

```java
pool.execute(() -> { throw new RuntimeException(); });  // ❌ 异常丢失（只有后台日志）
Future<?> f = pool.submit(() -> { throw new RuntimeException(); });
f.get();   // ✅ 抛 ExecutionException，可捕获
// 或在任务内 try-catch 兜底
```

---

## 7. ThreadLocal

### 7.1 ThreadLocal 原理与内存泄漏？

**参考答案：**
- 每个 Thread 内部持有 ThreadLocalMap（key = ThreadLocal 弱引用，value = 实际值）— **线程隔离**
- 泄漏原因：**key 是弱引用（ThreadLocal 被回收后 key 变 null），但 value 是强引用**，若线程长期存活（线程池），value 永远无法回收 → 内存泄漏

```java
// 正确用法：finally 中 remove
ThreadLocal<Connection> tl = new ThreadLocal<>();
try {
    tl.set(conn);
    // 业务逻辑
} finally {
    tl.remove();    // ⚠️ 必须 remove，尤其线程池场景
}
```

**追问：** 为什么不把 value 也做成弱引用？→ value 弱引用会导致业务数据被误回收。为什么 key 用弱引用？→ 允许 ThreadLocal 对象本身被回收，防止 ThreadLocalMap 持有 ThreadLocal 强引用形成"ThreadLocal 永远不回收"。解决哈希冲突？→ 开放定址法（线性探测，与 HashMap 链地址法不同）。父子线程传递？→ InheritableThreadLocal（已渐弃用）；跨线程传递推荐 TransmittableThreadLocal（阿里 TTL）。

---

## 8. JUC 工具类与 CompletableFuture

### 8.1 CountDownLatch / CyclicBarrier / Semaphore 区别？

| 工具 | 机制 | 典型场景 | 能否复用 |
|------|------|----------|:---:|
| CountDownLatch | 倒计时门闩，countDown 归零放行 | 主线程等 N 个任务完成 | ❌ 一次性 |
| CyclicBarrier | 栅栏，N 个线程到齐才一起走 | 分批并发汇总（如多阶段计算） | ✅ 循环 |
| Semaphore | 信号量，控制同时访问数 | 限流（如连接池、令牌） | ✅ |

```java
CountDownLatch latch = new CountDownLatch(3);
for (int i = 0; i < 3; i++) pool.execute(() -> { /* 任务 */ latch.countDown(); });
latch.await(10, TimeUnit.SECONDS);      // 主线程等待（带超时防卡死）
```

**追问：** await 为什么要带超时？→ 防止子任务异常未 countDown 导致主线程永久阻塞（生产事故高发点）。

### 8.2 CompletableFuture 异步编排？

```java
CompletableFuture.supplyAsync(() -> 查订单(1))
    .thenApplyAsync(订单 -> 查用户(订单.getUserId()))     // 串行依赖
    .thenCombine(CompletableFuture.supplyAsync(() -> 查库存(1)), (u, inv) -> 组装)
    .exceptionally(e -> 兜底结果)                          // 异常恢复
    .thenAccept(result -> log.info(result));
```

**追问：** thenApply vs thenApplyAsync？→ 前者默认复用提交线程（或公共池），后者提交 ForkJoinPool 新线程执行。生产陷阱？→ 自定义线程池传入，避免公共 ForkJoinPool 被阻塞任务拖垮。

---

## 9. Java 21 虚拟线程

### 9.1 虚拟线程是什么？解决了什么？

**参考答案（JEP 444，JDK 21 正式）：**

| 对比 | 平台线程 | 虚拟线程 |
|------|----------|----------|
| 映射 | 1:1 映射 OS 线程 | M:N 映射（大量虚拟线程复用少量载体线程） |
| 数量 | 几千级上限（栈内存 ~1MB） | 百万级（栈在堆中，动态） |
| 阻塞代价 | 阻塞即占用 OS 线程，代价高 | 阻塞时自动挂起，释放载体线程 |
| 适用 | 任何场景 | **IO 密集**（高并发连接/请求） |
| 创建成本 | 高 | 极低（≈new 对象） |

```java
// 创建方式
Thread.ofVirtual().start(() -> doIo());                  // 单个
ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();
pool.submit(() -> doIo());                               // 每任务一虚拟线程
```

**追问：**
- 虚拟线程适合 CPU 密集吗？→ 不适合！计算场景没有阻塞点，调度无收益；**虚拟线程只解 IO 型并发**
- 对线程池设计的影响？→ 高并发 IO 场景可用"每任务一线程"，传统"池化"失去意义（连接池/限流仍需自己控制）
- 与 GC 的关系？→ 百万虚拟线程栈在堆中，对象多但存活短，对 G1/ZGC 友好
- Tomcat/Spring Boot 3.2+ 如何启用？→ `spring.threads.virtual.enabled=true`

---

> 🎯 **核心要点**：并发模块的面试主线 = **一条锁的演进史**：无锁（CAS/volatile）→ 轻量同步（synchronized 升级）→ 框架锁（AQS/ReentrantLock）→ 池化（线程池）→ 新型并发（CompletableFuture/虚拟线程）。能沿着这条线讲，再补 ThreadLocal 泄漏和线程池拒绝策略两个"生产事故点"，并发就稳了。

**下一模块**：[04-JVM内存与垃圾回收高频题](04-JVM内存与垃圾回收高频题.md) / **返回总览**：[00-Java核心面试大全总览](00-Java核心面试大全总览.md)
