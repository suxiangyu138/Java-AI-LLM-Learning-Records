# 03 - Java 并发编程

> 🎯 并发是后端面试的"分水岭"考点——答好它，面试官默认你有处理高并发系统的能力。本模块直击必考主线：synchronized 锁升级、volatile 语义、AQS 原理、线程池七参数、ThreadLocal 泄漏、虚拟线程

## 📚 目录

1. [线程基础与三要素](#1-线程基础与三要素)
2. [synchronized 原理与锁升级](#2-synchronized-原理与锁升级)
3. [volatile 与 JMM 语义](#3-volatile-与-jmm-语义)
4. [AQS 与锁体系](#4-aqs-与锁体系)
5. [线程池：七参数与执行流程](#5-线程池七参数与执行流程)
6. [ThreadLocal 与内存泄漏](#6-threadlocal-与内存泄漏)
7. [JUC 并发工具与 CAS](#7-juc-并发工具与-cas)
8. [虚拟线程与 JDK 25 并发新特性](#8-虚拟线程与-jdk-25-并发新特性)
9. [高频面试题与追问预案](#9-高频面试题与追问预案)

## 1. 线程基础与三要素

并发三大特性是总纲，所有并发题都从这三条展开：**原子性**（一个操作不可分割——i++ 是读-改-写三步，不原子）、**可见性**（线程修改对其它线程可见——CPU 缓存导致不可见）、**有序性**（编译器/CPU 指令重排导致的乱序）。对应解决方案：原子性靠锁/CAS、可见性靠 volatile/锁、有序性靠 volatile 禁重排/锁的内存屏障。

线程的生命周期必背：**NEW → RUNNABLE →（BLOCKED/WAITING/TIMED_WAITING）→ TERMINATED**。注意 Java 把"操作系统就绪+运行"统一为 RUNNABLE，睡 1 秒是 TIMED_WAITING 不是"休眠"；BLOCKED 是等 monitor 锁，WAITING 是等 notify 或 join。`wait/notify` 必须在 synchronized 块内用（依赖 monitor），`sleep` 不释放锁——这是送分题也是送命题。

## 2. synchronized 原理与锁升级

synchronized 是**对象头 Mark Word 上的锁标记**。JDK 6 后的锁升级是必背链路：**无锁 → 偏向锁 → 轻量级锁 → 重量级锁**，只升不降（消除偏向锁除外）。**偏向锁**：同一线程反复进入，记录线程 ID，CAS 标记偏向，无竞争时零开销；**轻量级锁**：有第二线程竞争时撤销偏向，线程在自己的栈帧中拷贝 Mark Word 并在对象头 CAS 自旋，自旋失败；**重量级锁**：膨胀为 monitor（ObjectMonitor），未抢到进入阻塞队列，涉及用户态→内核态切换，代价最大。

JDK 15 开始偏向锁被废弃（JEP 374），JDK 18 移除——**2026 年面试答锁升级要更新口径**：说清楚历史演进（偏向锁在 JDK 15 deprecated、JDK 18 移除），现在 synchronized 默认是轻量级 → 重量级路径，配合自旋与自适应自旋优化。追问方向：轻量级锁自旋的缺点（长时间持锁时自旋空转烧 CPU）、锁粗化与锁消除（JIT 优化）、synchronized 与 ReentrantLock 的对比（可中断/公平/超时/多条件 vs 简单）。

## 3. volatile 与 JMM 语义

volatile 三句话背熟：**保证可见性**（写后立即刷主存，读前强制读主存，本质是内存屏障）、**禁止指令重排**（写屏障/读屏障，经典应用是 DCL 单例）、**不保证原子性**（i++ 场景无效，要用 AtomicInteger 或锁）。volatile 的可见性靠 JMM 的 happen-before 规则：**对一个 volatile 变量的写操作 happens-before 于后续对该变量的读操作**。

DCL 单例为什么必须 volatile：`instance = new Singleton()` 是三步——分配内存、初始化对象、引用赋值，指令重排后可能"先赋值后初始化"，另一线程读到未初始化完成的对象；volatile 禁止这步重排。追问"volatile 能替代锁吗"——不能，它管不了复合操作与临界区互斥；"性能损耗"——仅禁重排与刷主存，远小于锁。

## 4. AQS 与锁体系

**AQS（AbstractQueuedSynchronizer）是 JUC 锁的基石**：内部一个 volatile int state（资源状态）+ CLH 变体双向队列（等待线程）。核心方法：`acquire`（CAS 改 state，失败入队阻塞）、`release`（改 state，唤醒队首）。ReentrantLock/ReentrantReadWriteLock/Semaphore/CountDownLatch 全部基于它——**答"ReentrantLock 原理"的本质就是答 AQS**。

ReentrantLock 与 synchronized 的对比是必考题：**可中断**（lockInterruptibly）、**可超时**（tryLock(3, TimeUnit.SECONDS)）、**公平锁**（队列先来先服务，代价是吞吐降低）、**多条件队列**（Condition 的 await/signal，比 wait/notify 更精确）、**显式 unlock**（必须 finally 释放，否则死锁）。ReentrantReadWriteLock 读写分离：读读并发、读写互斥、写写互斥——但读多写多的极端场景写锁饥饿，StampedLock 的乐观读（tryOptimisticRead）可无锁读，适合读多写少且对一致性要求不极端的场景。

## 5. 线程池：七参数与执行流程

线程池是并发实战题的重灾区，七参数必背：**corePoolSize（核心线程数）、maximumPoolSize（最大线程数）、keepAliveTime（非核心线程空闲存活）、unit、workQueue（任务队列）、threadFactory（线程工厂）、RejectedExecutionHandler（拒绝策略）**。

执行流程口诀："**先核心，再队列，后最大，满则拒**"——提交任务时核心线程未满则新建核心线程执行；满了进队列等待；队列满了才创建非核心线程（最多到 maximumPoolSize）；再满走拒绝策略。四种拒绝策略：AbortPolicy（抛异常，默认）、CallerRunsPolicy（调用者线程执行，反压）、DiscardPolicy（丢弃）、DiscardOldestPolicy（丢最老）。**生产禁止 Executors 快捷工厂**（newFixedThreadPool 无界队列 OOM、newCachedThreadPool 最大线程数 Integer.MAX 风险），必须 ThreadPoolExecutor 显式传参——阿里编码规约明确要求。

核心线程数怎么定：**CPU 密集型 = CPU 核数 + 1（或核数×2），IO 密集型 = 核数 × (1 + 等待时间/计算时间)**——本质是 IO 等待时不占 CPU，可以多开线程重叠。面试加分句："线上按公式初设，再用压测与监控（队列积压、拒绝数）回调节，公式只是起点。"

## 6. ThreadLocal 与内存泄漏

ThreadLocal 原理一句话：**每个 Thread 内部有一个 ThreadLocalMap，key 是 ThreadLocal 弱引用，value 是强引用**。为什么 key 用弱引用：ThreadLocal 对象生命周期比线程短，弱引用让 key 可被 GC；但 value 是强引用，**如果线程长期存活（线程池场景），key 被回收后 value 永远无法访问 → 内存泄漏**。

因此工程铁律：**使用完必须 remove()**（尤其线程池复用线程场景），或 try-finally 包住。追问方向：为什么阿里规约要求 remove（防泄漏）；ThreadLocal 传递到子线程失效怎么办（InheritableThreadLocal，但线程池场景要用 TransmittableThreadLocal）；为什么 MDC 里存 TraceId（全链路日志的关键实现）。

## 7. JUC 并发工具与 CAS

CAS（Compare And Swap）是并发原语：**比较当前值与预期值，相等才更新**，由 CPU 指令（cmpxchg）保证原子。三个缺点必背：**ABA 问题**（AtomicStampedReference 加版本号解决）、**自旋消耗 CPU**（高竞争下退化为自旋）、**只能保证单变量原子**（多变量用锁或 AtomicReference 封装）。

工具三兄弟区分：**CountDownLatch**（一次性的门闩，主线程等 N 个任务完成）、**CyclicBarrier**（可循环的屏障，N 个线程互相等齐再放行，可复用）、**Semaphore**（信号量，限流 N 个许可证）。经典场景：并行请求聚合用 CountDownLatch；多线程分片统计用 CyclicBarrier；接口限流用 Semaphore。线程安全集合再补充：CopyOnWriteArrayList（写时复制，读多写少）、BlockingQueue 四组（Array/Linked/Synchronous/Delay）对应生产者消费者模式。

## 8. 虚拟线程与 JDK 25 并发新特性

虚拟线程（JDK 21 正式版，JEP 444）是 2026 年面试新热点：**平台线程池化 + 虚拟线程按需创建**，每个虚拟线程占约 1KB 栈，可创建百万级，遇到阻塞 I/O 自动让出载体线程（调度器），解决"线程池耗尽"问题——但**只适用于 IO 密集型**，CPU 计算型无收益。注意：**synchronized 块内的阻塞会 pinning（钉住载体线程）**，JDK 24（JEP 491）已修复 synchronized pinning，虚拟线程内用 synchronized 不再钉住；结构化并发（JEP 525）与 ScopedValue（JEP 506）是配套新特性，面试能提出来是加分项。JDK 25 LTS（2025-09 发布）为当前最新长期支持版本，生产主力选 21/25。

## 9. 高频面试题与追问预案

### 基础概念档

| # | 题目 | 答题要点 |
|---|------|----------|
| 1 | synchronized 原理？ | 对象头 Mark Word / 锁升级链路（JDK 15 弃偏向）/ 锁粗化消除 |
| 2 | volatile 能保证原子性吗？ | 不能 / 可见性+禁重排 / i++ 用 AtomicInteger |
| 3 | 线程池七大参数？ | core/max/keepAlive/队列/工厂/拒绝策略 + 执行流程口诀 |
| 4 | 线程有哪些状态？ | NEW/RUNNABLE/BLOCKED/WAITING/TIMED_WAITING/TERMINATED |

### 工程实践档

| # | 题目 | 答题要点 |
|---|------|----------|
| 5 | 线程池 OOM 或拒绝任务？ | 检查队列是否无界 / 显式 ThreadPoolExecutor / 监控拒绝数告警 |
| 6 | ThreadLocal 泄漏排查？ | 堆 dump 查 ThreadLocalMap / remove 修复 / 代码审查防再犯 |
| 7 | 接口如何限流？ | Semaphore / 令牌桶（Guava RateLimiter）/ 网关层限流对比 |

### 架构设计档

| # | 题目 | 答题要点 + 追问点 |
|---|------|------------------|
| 8 | 高并发秒杀怎么防超卖？ | 数据库行锁/乐观锁 CAS/Redis 原子扣减；追问"Redis 扣减后 DB 不一致怎么办" |
| 9 | 为什么禁用 Executors 工厂？ | 无界队列 OOM / max 无限 / 显式参数可控；追问"CallerRunsPolicy 有什么用" |
| 10 | 虚拟线程能替代线程池吗？ | IO 密集可替代 / CPU 密集无效 / 池化语义仍存；追问"pinning 问题" |

### 追问速查

JMM 八大操作或 happen-before 规则至少背 3 条（程序次序/volatile/锁/传递性）；AQS 的 state 是什么含义（资源数，不同锁语义不同）；ReentrantLock 公平锁怎么实现（hasQueuedPredecessors 检查）；sleep 与 wait 区别（锁释放与否、来源）。

> 🎯 **核心要点**：并发题的通吃公式 = **是什么 → 解决了什么问题 → 代价与权衡 → 演进（JDK 版本）**。2026 年答题务必带版本意识：锁升级讲清楚偏向锁被移除的历史、线程池讲清楚虚拟线程的适用边界——面试官要的不是背答案，是知道"什么时候该用什么"。

---

**下一模块**：[04-JVM内存与垃圾回收](04-JVM内存与垃圾回收.md) / **返回总览**：[00-阶段一基础复盘总览](00-阶段一基础复盘总览.md)
