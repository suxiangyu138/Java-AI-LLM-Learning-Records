# 04 CAS 与原子类

> 无锁并发的地基——CAS 硬件指令、ABA 陷阱、原子类全景与 LongAdder 分段计数，理解"无锁为什么能快"

---

## 📚 目录

1. [CAS：比较并交换的硬件原语](#1-cas比较并交换的硬件原语)
2. [ABA 问题与解决](#2-aba-问题与解决)
3. [原子类全景](#3-原子类全景)
4. [LongAdder：分段计数的设计](#4-longadder分段计数的设计)
5. [CAS 的适用边界](#5-cas-的适用边界)

---

## 1. CAS：比较并交换的硬件原语

CAS（Compare-And-Swap）是一条硬件指令：`compareAndSet(expected, update)` 只有当前值等于 expected 时才更新为 update，无论成功与否都返回实际值，整个过程不可分割。它在 JVM 层的承载有两个：`Unsafe.compareAndSwapInt`（JDK 9 前的事实标准，至今仍是 JUC 内部主力）与官方入口 `VarHandle`（JDK 9+ 提供，更安全、支持任意字段/数组元素/静态字段的原子操作）。日常开发只碰原子类，但面试要能说清：Unsafe 是 JVM 后门（不可控、无法跨 JDK 版本保证），VarHandle 是规范化的替代，JUC 源码内部尚未全部迁移。

**无锁为什么能快**：锁让线程阻塞（内核态 park/unpark + 上下文切换），CAS 让线程自旋重试——自旋在竞争不激烈时完全在用户态完成，省掉内核往返。典型姿势：

```java
AtomicInteger counter = new AtomicInteger();
// 等价 i++ 的原子版
counter.incrementAndGet();
// 自定义复合更新
int prev;
do {
    prev = counter.get();
} while (!counter.compareAndSet(prev, prev + 1));
```

注意 `getAndIncrement` 内部就是这么实现的——自旋 CAS。它在 JMM 里同时是原子 + 可见（带 volatile 语义），一条指令解决三要素中的两个，这是它比"锁 + volatile"更轻的原因。

## 2. ABA 问题与解决

CAS 只比较"值相等"，不比较"值是否被改过又改回来"：线程 A 读到值 5，线程 B 改成 6 又改回 5，A 的 CAS 依然成功——但"5 曾变成过 6"这个历史可能影响正确性（经典场景：链表栈出栈时，头节点被换掉又换回，ABA 会让 A 误以为栈没变）。

解决方案两条：**版本号**（`AtomicStampedReference`，比较"值+戳"二元组，戳每次更新自增）与 **AtomicMarkableReference**（只关心"是否被改过"的布尔标记）。实务判断标准：如果更新的值有业务连续性依赖（计数器、状态机），用版本号；纯"确保当前值未被他人改动"的朴素计数，ABA 通常无害。

## 3. 原子类全景

原子类按类型分四族，选型只看数据结构：

- **基础标量**：AtomicInteger / AtomicLong / AtomicBoolean——替代 volatile 计数与标志位，核心是 getAndIncrement、compareAndSet、updateAndGet（函数式更新，JDK 8+ 首选，免手写自旋）。
- **数组**：AtomicIntegerArray / AtomicLongArray / AtomicReferenceArray——按索引原子更新，替代"Atomic 数组"这种错误组合（普通数组元素无法原子化）。
- **引用**：AtomicReference——原子替换整个对象引用，配 `getAndSet`/`accumulateAndGet`；`AtomicReferenceFieldUpdater` 用于把已有类的非原子字段原子化（省去改类字段类型的成本）。
- **累加器**：LongAdder / LongAccumulator——高竞争计数专用（见下节）。

无锁数据结构把 CAS 用到了极致：`ConcurrentLinkedQueue` 出队用 CAS 移动 head，`ConcurrentHashMap` 的空桶初始化用 CAS 占位——"能 CAS 就不加锁"是 JUC 源码的设计基调，也是 [06 篇](06-并发容器.md) 的阅读线索。手写一个无锁栈能直观感受这个思路：入栈只在"推入的新节点指向当前栈顶"之后 CAS 替换栈顶，并发入栈者只有一个成功，失败的循环重试：

```java
public class LockFreeStack<T> {
    private final AtomicReference<Node<T>> top = new AtomicReference<>();
    public void push(T v) {
        Node<T> n = new Node<>(v);
        Node<T> old;
        do { old = top.get(); n.next = old; }   // 读栈顶 + 预链接
        while (!top.compareAndSet(old, n));     // 失败说明栈顶变了，重来
    }
}
```

注意这里的 ABA 隐患：出栈线程 A 读到 old，期间栈被"入-出"一轮后栈顶值相同，A 的 CAS 会把一个已出栈的节点错误链接回栈——所以生产无锁结构都要像 `ConcurrentLinkedQueue` 一样用"next 指针 + 多字段协调"规避，自研无锁结构是高风险行为，能用 JUC 现成的就别造。

## 4. LongAdder：分段计数的设计

AtomicLong 在高竞争下会退化：几十个线程反复 CAS 同一个缓存行，失败者不停重读——这就是**缓存行颠簸**（cache line contention），吞吐断崖式下跌。LongAdder 的解法是**分段**：内部维护 `Cell[]` 数组，每个线程先 CAS 命中自己的 Cell（线程哈希分流），最后 `sum()` 把所有 Cell 与 base 相加。竞争被均摊到 N 个 Cell 上，每个 Cell 的 CAS 冲突概率降到 1/N。

```java
LongAdder count = new LongAdder();
count.increment();          // 无锁，命中自己分段的 Cell
long total = count.sum();   // 汇总所有段（无快照一致性，只保证最终值）
```

代价与边界：`sum()` 不是强一致快照（读时可能叠加到一半的并发写），所以 LongAdder 只适合"计总数、不在乎瞬时值"的统计场景——QPS 计数、请求量统计、热点计数。需要"读到最新值 + 原子读改写"（如发号器）仍是 AtomicLong。**LongAdder 自 JDK 8 引入至今 API 稳定**，2026 基线无变化。压测经验：8 线程竞争时 LongAdder 吞吐可达 AtomicLong 的 5-10 倍。

为什么差距这么大？回到缓存行：AtomicLong 的所有线程 CAS 同一个地址，每次失败重读都打到同一缓存行，多核之间反复失效——这是"缓存行颠簸"。LongAdder 的 Cell 数组让每个线程大概率命中自己的缓存行（线程哈希分散），命中率与竞争强度负相关，竞争越激烈优势越大。这也是它内部对 Cell 做缓存行填充（避免伪共享）的原因，与 [09 篇](09-并发性能调优与故障排查.md) 伪共享主题呼应。

## 5. CAS 的适用边界

CAS 不是万能钥匙，三条硬边界：

1. **高竞争下自旋浪费 CPU**：冲突率超过阈值后，自旋变成空转——此时 synchronized 重量级锁的阻塞唤醒反而更优，JUC 的"自旋阈值+升级"混合策略才是答案。
2. **只能保护单个变量**：跨多个变量的原子操作（转账：扣款 + 加款）CAS 无能为力，要么锁，要么合并状态（把两个变量编码进一个 long，如高 32 位 + 低 32 位，配合 CAS 一次更新——这是"复合状态单变量化"的技巧）。
3. **ABA 与语义脆弱**：CAS 不知道"为什么变"，业务语义要自己维护（版本号）。

选型一句话：**单变量计数/标志 → 原子类；高竞争统计 → LongAdder；多变量复合操作 → 锁；无锁数据结构 → 研究 JUC 源码而非自己造**。面试常问"什么时候用 CAS 什么时候用锁"——正确答法是按竞争强度与语义复杂度分档：竞争低用 CAS（省内核开销）、竞争高用分段或锁（避免自旋空转）、语义复杂（多变量一致性）用锁（CAS 表达不了），而不是非此即彼。

> 🎯 **核心要点**：CAS 用一条硬件指令把"检查+更新"原子化，无竞争时是零内核开销的同步；高竞争下分段（LongAdder）比自旋更聪明；ABA 用版本号解决；无锁只适用于单变量语义，复合操作请回锁。

---

**下一模块**：[05 JUC 同步工具与阻塞队列](05-JUC同步工具与阻塞队列.md)

**返回总览**：[00-总览](00-总览.md)
