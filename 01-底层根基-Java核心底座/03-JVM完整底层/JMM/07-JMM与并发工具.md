# JMM与并发工具
> AQS 的状态可见性、并发容器的 HB 边、线程池的提交-执行语义：java.util.concurrent 如何构建在 JMM 之上。

---

## 📚 目录

1. [JUC 的 JMM 基础](#1-juc-的-jmm-基础)
2. [AQS：锁的可见性引擎](#2-aqs锁的可见性引擎)
3. [并发容器与发布](#3-并发容器与发布)
4. [线程池的 HB 语义](#4-线程池的-hb-语义)
5. [同步工具剖析](#5-同步工具剖析)
6. [工具类可见性速查](#6-工具类可见性速查)

---

## 1. JUC 的 JMM 基础

### 1.1 JUC 的构建块

```text
java.util.concurrent 的底层三件套：
  ① volatile：状态可见性（AQS 的 state、容器内部标志）
  ② CAS：原子状态更新（AQS 的 tryAcquire、原子类）
  ③ 锁（synchronized/ReentrantLock）：互斥 + 可见性

JUC 没有"魔法"——所有工具类都是这三件套的组装
```

### 1.2 为什么 JUC 类是线程安全的

```text
规范答案：每个 JUC 类都建立了清晰的 HB 边：
  Lock.unlock() HB 后续 Lock.lock()（同一锁实例）
  BlockingQueue.put() HB 后续 take()（同一队列）
  ThreadPoolExecutor.submit() HB 任务执行（队列语义）

→ 使用 JUC 工具的代码自动获得可见性保证
→ 不需要额外 volatile/锁（工具内部已处理）
```

> 🎯 核心认知：**JUC 的价值 = 把"难用的 HB 边"封装成"好用的 API"**——正确使用 JUC 类，就自动满足 JMM 要求。

---

## 2. AQS：锁的可见性引擎

### 2.1 AbstractQueuedSynchronizer 结构

```text
AQS 核心字段：
  private volatile int state;        ← 锁状态（可见性核心）
  private transient Node head/tail;  ← 等待队列（volatile 指针）

state 的语义由子类定义：
  ReentrantLock：state = 重入次数
  Semaphore：state = 剩余许可
  CountDownLatch：state = 未计数
  ReentrantReadWriteLock：state 高低位 = 读写计数
```

### 2.2 获取/释放的 HB 边

```java
// ReentrantLock.lock() / unlock() 的语义链
// 获取（acquire）：
//   CAS(state, 0 → 1) 成功 → 获得锁
//   （volatile 读 state → 建立"之前释放者的可见性"）

// 释放（release）：
//   state = 0（volatile 写）
//   唤醒队首节点（unpark）

// HB 链：线程 A 的临界区 → A.unlock()（state 写）
//   → B.lock() 读到 state=0（volatile 读）→ B 的临界区
//   传递性：A 临界区的所有写对 B 可见 ✓
```

### 2.3 队列节点的可见性

```text
等待队列节点的构建（enqueue）：
  prev/next 指针用 volatile 写 + CAS 发布
  → 唤醒时（unpark）通过 LockSupport 的 HB 语义传递

LockSupport 的 HB 保证：
  unpark(thread) HB 该线程后续的 park 返回
  → 唤醒方写入的数据对被唤醒线程可见
```

---

## 3. 并发容器与发布

### 3.1 ConcurrentHashMap 的可见性

```text
ConcurrentHashMap（JDK 8+）的设计：
  volatile Node<K,V>[] table（数组引用 volatile）
  Node 的 val/next 为 volatile（读不加锁可见最新值）
  写路径：CAS 插入 + synchronized 锁桶头（JDK 8 锁粒度）

用户视角的 HB 保证：
  map.put(k, v) HB 后续 map.get(k)（同一 key）
  → put 前的其他写也可见（容器内部同步传递）

面试高频："ConcurrentHashMap 为什么读不加锁"
  → volatile 链保证可见性；哈希桶冲突用 CAS/锁
```

### 3.2 发布安全模式

| 发布方式 | 安全性 | 说明 |
|---------|:---:|------|
| 静态初始化 | ✅ | 类加载的 HB 语义 |
| volatile 引用 | ✅ | 规则三 |
| 锁保护发布 | ✅ | 规则二 |
| 并发容器 put/get | ✅ | 容器内部 HB |
| 普通引用直接赋值 | ❌ | 无 HB 边 |

```text
工程口诀："对象要通过同步渠道发布，不能裸扔"
  裸发布 = 另一个线程可能看到"半初始化"对象
  （final 字段有部分保护，非 final 字段无保证）
```

### 3.3 CopyOnWriteArrayList 的语义

```text
原理：写时复制（volatile Object[] array 引用替换）
  add：加锁 → 复制数组 → 替换引用（volatile 写）
  get：直接读 array 引用（volatile 读）→ 无需锁

HB 链：add 的数组替换（volatile 写）HB 后续 get（volatile 读）
  → 写后读可见 ✓
适用：读多写极少（缓存、事件监听器列表）
```

---

## 4. 线程池的 HB 语义

### 4.1 提交-执行-返回的完整链

```java
ExecutorService pool = Executors.newFixedThreadPool(4);

pool.submit(task);          // ① 提交（入工作队列）
// ② 工作线程从队列取任务（take/阻塞获取）
// ③ 任务内执行（读写数据）
Future<?> f = pool.submit(() -> data = 42);
f.get();                    // ④ 获取结果（HB 边！）

// HB 链：
// ① 提交者写 → 队列（ConcurrentLinkedQueue 的 volatile/CAS）
//   → ② 工作线程读到 → ③ 任务内操作 → ④ f.get() 返回
// 结论：submit 前写的数据任务内可见；任务内的写对 get() 可见
```

### 4.2 线程池内部的同步点

```text
工作队列：BlockingQueue（put HB take）
Worker 状态：volatile（线程生命周期）
FutureTask：volatile state + CAS（结果发布）
  任务完成 → state 写 → get() 读到 → 结果可见

经典应用：
  主线程准备任务数据 → submit → 任务线程必然可见
  任务线程计算结果 → 返回 → 主线程 get() 必然可见
  —— 无需额外同步（线程池已建立 HB 边）
```

### 4.3 线程池与虚拟线程（JDK 21+）

```text
Executors.newVirtualThreadPerTaskExecutor()：
  每任务一个虚拟线程（无池化队列语义）
  提交-执行链依然成立（submit HB 虚拟线程执行）

JEP 491 后（JDK 24）：
  synchronized 不再 pinning → 线程池 + 同步块组合安全
  详见 08 模块
```

---

## 5. 同步工具剖析

### 5.1 CountDownLatch / CyclicBarrier

```text
CountDownLatch：await() 等待 count 归零
  countDown()（state 递减，volatile+CAS）
  await() 读到 0（volatile 读）→ HB 边建立

HB 链：countDown() 前的写 HB await() 返回后的读
  → "所有任务完成后再汇总"的模式天然安全

CyclicBarrier：栅栏（参与者到达后同时放行）
  内部用锁 + condition → 屏障点建立 HB 边
```

### 5.2 BlockingQueue 家族

| 队列 | 内部机制 | HB 边 |
|------|---------|-------|
| ArrayBlockingQueue | 锁 + 条件 | put HB take |
| LinkedBlockingQueue | 双锁（读写分离） | put HB take |
| SynchronousQueue | 传递（无缓冲） | 直接交接 |
| DelayQueue | 优先级 + 条件 | 到期 HB |
| ConcurrentLinkedQueue | 无锁（CAS+volatile） | 入队 HB 出队 |

```text
工程含义：生产者-消费者无需额外同步
  put 前写的数据，take 后必然可见（队列内部保证）
```

### 5.3 ThreadLocal 的隔离语义

```text
ThreadLocal：每线程自己的存储（无跨线程共享）
  → 不需要 HB 边（根本没有共享）
  → 但注意：ThreadLocal 的值是"线程私有"
    不要用它传跨线程数据（父线程 → 子线程需 InheritableThreadLocal
    或显式传递——InheritableThreadLocal 用构造时拷贝，有特殊语义）

虚拟线程注意（JDK 21+）：
  ThreadLocal 对虚拟线程同样隔离，但数量巨大时
  建议 ScopedValue（JEP 506）替代（详见 JUC/虚拟线程目录）
```

---

## 6. 工具类可见性速查

| 工具 | 关键 HB 边 | 使用注意 |
|------|-----------|---------|
| ReentrantLock | unlock HB lock | 读写都用同一锁 |
| Semaphore | release HB acquire | 许可语义 |
| CountDownLatch | countDown HB await 返回 | 一次性 |
| Future.get() | 任务完成 HB get 返回 | 结果发布 |
| BlockingQueue | put HB take | 生产者消费者 |
| ConcurrentHashMap | put HB get（同 key） | 复合操作仍需锁 |
| CopyOnWriteList | 写替换 HB 读 | 只适合读多写少 |
| Atomic* | CAS 写 HB CAS 读 | 无锁计数 |
| LongAdder | 分片（近似值） | 统计场景 |
| ThreadLocal | 无跨线程边 | 线程私有 |

> 🎯 **核心要点**：JUC 的全部价值 = 把 JMM 的 HB 规则封装成安全 API——正确使用工具类，可见性自动保证。工程判断方法：**问"我的读写经过哪个同步点？"**——经过锁/队列/Future 等，则有 HB 边；裸变量直接跨线程读写，则没有。记住：JUC 类不是"黑魔法"，是"封装好的 HB 边"。

---

## 7. 小结

| 问题 | 答案 |
|------|------|
| JUC 的底层三件套？ | volatile + CAS + 锁 |
| AQS 的核心字段？ | volatile state + 等待队列 |
| 队列的 HB 边？ | put HB take（BlockingQueue） |
| Future 的可见性？ | 任务完成 HB get() 返回 |
| ConcurrentHashMap 读为何无锁？ | volatile 链 + CAS/锁桶 |
| 判断工具正确性？ | 找同步点 → 确认 HB 链 |

**下一模块**：[08-前沿与工程实践](08-前沿与工程实践.md)　**返回总览**：[00-JMM知识体系总览](00-JMM知识体系总览.md)
