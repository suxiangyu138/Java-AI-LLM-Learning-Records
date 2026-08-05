# 08 - CAS与原子类

> 定位：掌握CAS无锁并发的底层CPU指令（cmpxchg）实现、ABA问题与AtomicStampedReference解决方案、JUC原子类全景及LongAdder高并发计数原理

## 目录

1. [CAS原理与底层CPU指令](#1-cas原理与底层cpu指令)
2. [Unsafe类与CAS操作](#2-unsafe类与cas操作)
3. [CAS的三大问题：ABA、自旋开销、单变量限制](#3-cas的三大问题aba自旋开销单变量限制)
4. [基本类型原子类：AtomicInteger/AtomicLong/AtomicBoolean](#4-基本类型原子类atomicintegeratomiclongatomicboolean)
5. [数组原子类：AtomicIntegerArray/AtomicLongArray/AtomicReferenceArray](#5-数组原子类atomicintegerarrayatomiclongarrayatomicreferencearray)
6. [引用原子类：AtomicReference/AtomicStampedReference/AtomicMarkableReference](#6-引用原子类atomicreferenceatomicstampedreferenceatomicmarkablereference)
7. [LongAdder与LongAccumulator（JDK 8+分层累加原理）](#7-longadder与longaccumulatorjdk-8分层累加原理)
8. [CAS vs synchronized vs Lock性能对比](#8-cas-vs-synchronized-vs-lock性能对比)
9. [原子类选型指南](#9-原子类选型指南)
10. [面试高频考点](#10-面试高频考点)

---

## 1. CAS原理与底层CPU指令

### 1.1 什么是CAS

**CAS（Compare-And-Swap，比较并交换）** 是一种无锁并发算法，核心思想：在更新一个变量时，只有变量的当前值等于预期值时，才将其更新为新值。

```
CAS流程（read-modify-write 合并为一条CPU指令）：
Thread1: 读 V=5 → CAS(V,5→6) → 匹配 ✅ → 写入6
Thread2: 读 V=5 → CAS(V,5→6) → V已是6，不匹配 ❌ → 自旋重试 → CAS(V,6→7) ✅
```

### 1.2 CPU底层指令：cmpxchg

CAS的原子性由CPU硬件指令保证：

| CPU架构 | 指令 | 说明 |
|---------|------|------|
| x86/x64 | `lock cmpxchg` | `lock`前缀锁定总线/缓存行，保证原子性 |
| ARM | `LDREX + STREX` | 加载独占/存储独占配对 |
| RISC-V | `LR + SC` | Load-Reserved / Store-Conditional |

```java
// CAS伪代码——对应CPU一条指令，非多步操作
public final class CasSimulation {
    public static native boolean compareAndSwap(
            Object obj, long offset, int expected, int x);
    // 实际由 CPU cmpxchg 指令保证原子性，不需要加锁
}
```

> 💡 CAS比锁更轻量的本质原因：直接在CPU指令层面实现原子性，不需要操作系统介入线程挂起和恢复，避免了用户态与内核态的切换开销。

### 1.3 CAS自旋重试模型

```java
/**
 * CAS自旋重试 —— 模拟 AtomicInteger.incrementAndGet 底层
 */
public class CasSpinDemo {
    private volatile int value;

    public int incrementAndGet() {
        for (;;) {                              // 自旋
            int current = value;                // 1. volatile读
            int next = current + 1;             // 2. 计算新值
            if (compareAndSet(current, next)) { // 3. CAS尝试
                return next;
            }   // 失败→继续循环，不阻塞线程
        }
    }

    private synchronized boolean compareAndSet(int expect, int update) {
        if (value == expect) { value = update; return true; }
        return false;
    }
}
```

> ⚠️ **自旋CPU开销**：如果CAS长时间不成功（竞争激烈），线程会持续占用CPU空转，造成CPU飙升。这是CAS的核心代价。

---

## 2. Unsafe类与CAS调用链路

`sun.misc.Unsafe` 是CAS操作的后门，**JUC原子类底层全部依赖Unsafe**。核心方法：

| 方法 | 用途 |
|------|------|
| `compareAndSwapInt(obj, offset, expected, x)` | 对象int字段CAS |
| `compareAndSwapLong(obj, offset, expected, x)` | 对象long字段CAS |
| `compareAndSwapObject(obj, offset, expected, x)` | 对象引用字段CAS |
| `getAndAddInt(obj, offset, delta)` | 原子加并返回旧值（JDK 8+） |

```java
// 通过反射获取Unsafe实例，执行CAS
public class UnsafeDemo {
    private volatile int value = 0;
    public static void main(String[] args) throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Unsafe unsafe = (Unsafe) f.get(null);
        long offset = unsafe.objectFieldOffset(
                UnsafeDemo.class.getDeclaredField("value"));
        UnsafeDemo d = new UnsafeDemo();
        boolean ok = unsafe.compareAndSwapInt(d, offset, 0, 5);
        System.out.println("CAS: " + ok + ", value=" + d.value);
    }
}
```

> 💡 JUC不用反射而用Unsafe，是因为Unsafe直接操作内存地址，绕过访问权限检查，比反射更快。

**JUC调用链路**：
```
AtomicInteger.incrementAndGet()
  └─ unsafe.getAndAddInt(this, valueOffset, 1)
      └─ do-while 循环：
          volatile读当前值 → CAS写新值 → 失败则自旋重试
```

> 🎯 所有JUC原子类最终通过Unsafe的native方法调用CPU的 `lock cmpxchg` 指令。

---

## 3. CAS的三大问题：ABA、自旋开销、单变量限制

### 3.1 ABA问题

```
ABA问题：
Thread1: 读 V=A → 被阻塞 ...
Thread2: CAS(A→B) ✅ → CAS(B→A) ✅  → V又变回A
Thread1: 恢复 → CAS(A→X) ✅  → ⚠️ CAS成功，但V已被修改过！
```

**ABA问题场景**：线程1读取值A后被阻塞，线程2将值A改为B再改回A。线程1恢复后CAS成功，但值已经"面目全非"。

```java
public class ABADemo {
    private static final AtomicReference<String> REF = new AtomicReference<>("A");

    public static void main(String[] args) throws Exception {
        // 线程1：读到A后被阻塞
        new Thread(() -> {
            String prev = REF.get();
            try { Thread.sleep(100); } catch (Exception e) {}
            System.out.println("Thread1 CAS A→C: " + REF.compareAndSet(prev, "C")
                    + ", val=" + REF.get()); // true! 但A已被B→A→B改过
        }).start();

        // 线程2：A→B→A，改回原值
        new Thread(() -> {
            REF.compareAndSet("A", "B");
            REF.compareAndSet("B", "A");
        }).start();
    }
}
```

### 3.2 ABA解决方案：版本号机制

核心思想：为变量附加版本号，每次修改版本号递增。CAS时同时检查值和版本号。

```java
public class AbaFixByStampDemo {
    private static final AtomicStampedReference<String> REF =
            new AtomicStampedReference<>("A", 0);  // 初始值A, 版本0

    public static void main(String[] args) {
        new Thread(() -> {
            int[] holder = new int[1];
            String v = REF.get(holder);
            int stamp = holder[0];
            try { Thread.sleep(100); } catch (Exception e) {}
            // 版本号已变为2，CAS失败
            boolean ok = REF.compareAndSet(v, "C", stamp, stamp + 1);
            System.out.println("CAS: " + ok + ", stamp=" + REF.getStamp()); // false
        }).start();

        new Thread(() -> {
            int[] h = new int[1];
            REF.get(h); REF.compareAndSet("A", "B", h[0], h[0] + 1); // A→B, 0→1
            REF.get(h); REF.compareAndSet("B", "A", h[0], h[0] + 1); // B→A, 1→2
        }).start();
    }
}
```

> ⚠️ `AtomicStampedReference` 使用整型版本号（stamp），可完全防止ABA；`AtomicMarkableReference` 使用布尔标记（mark），只能标记对象是否被修改过，无法追踪修改次数。

### 3.3 三大问题总结

| 问题 | 产生原因 | 后果 | 解决方案 |
|------|---------|------|---------|
| **ABA问题** | 变量被改回原值，CAS误判为未修改 | 数据语义错误 | `AtomicStampedReference`（版本号） |
| **自旋开销** | 竞争激烈时CAS持续失败 | CPU飙升，吞吐量下降 | `LongAdder`分段思想、退避策略 |
| **单变量限制** | CAS只能操作单个变量 | 无法多字段原子更新 | `AtomicReference`封装对象 |

> 💡 自旋优化退避策略：CAS失败后让出CPU（`Thread.yield()`）、短暂休眠（`Thread.sleep(0)`）或`Thread.onSpinWait()`（JDK 9+），减少CPU竞争。

---

## 4. 基本类型原子类：AtomicInteger/AtomicLong/AtomicBoolean

### 4.1 类层次结构

```text
java.util.concurrent.atomic 包结构

┌───────────────────────────────────────────────────────────┐
│                    Atomic 类族（JDK 5+）                    │
├──────────────┬──────────────┬──────────────┬───────────────┤
│  基本类型     │  数组         │  引用         │  累加器       │
├──────────────┼──────────────┼──────────────┼───────────────┤
│ AtomicInteger│ AtomicIntArr │ AtomicRef    │ LongAdder     │
│ AtomicLong   │ AtomicLngArr │ AtomicStmpRef│ DoubleAdder   │
│ AtomicBoolean│ AtomicRefArr │ AtomicMrkRef │ LongAccum     │
│              │              │              │ DoubleAccum   │
├──────────────┴──────────────┴──────────────┴───────────────┤
│  字段更新器（反射+字段CAS）：AtomicIntegerFieldUpdater       │
│  AtomicLongFieldUpdater / AtomicReferenceFieldUpdater      │
└───────────────────────────────────────────────────────────┘
```

### 4.2 AtomicInteger 核心API

```java
public class AtomicIntegerApiDemo {
    private final AtomicInteger count = new AtomicInteger(0);

    public int incAndGet() { return count.incrementAndGet(); }  // ++i
    public int getAndInc() { return count.getAndIncrement(); }  // i++
    public int addAndGet(int delta) { return count.addAndGet(delta); }
    public boolean cas(int expect, int update) {
        return count.compareAndSet(expect, update);
    }
    // JDK 8+ 函数式更新
    public int update() { return count.updateAndGet(x -> x * 2); }
    public int accumulate(int x) {
        return count.accumulateAndGet(x, Integer::sum);
    }
    public void lazySet(int v) { count.lazySet(v); } // 弱内存语义
}
```

### 4.3 基本原子类对比

| 类名 | 底层字段类型 | 用途 | 说明 |
|------|------------|------|------|
| `AtomicInteger` | `volatile int` | 计数器、ID生成、状态标记 | JDK 8+支持 `updateAndGet` 函数式更新 |
| `AtomicLong` | `volatile long` | 长整型计数器、时间戳生成 | 32位JVM需要CAS两次（高低32位） |
| `AtomicBoolean` | `volatile int`(0/1) | 开关标记、一次性标志位 | 内部用int，非boolean |

### 4.4 计数器方案演进

```java
// synchronized → AtomicInteger → LongAdder
class SyncCounter {
    private int count = 0;
    public synchronized int inc() { return ++count; }      // 低并发
}
class AtomicCounter {
    private final AtomicInteger count = new AtomicInteger(0);
    public int inc() { return count.incrementAndGet(); }    // 中等并发
}
class AdderCounter {
    private final LongAdder count = new LongAdder();
    public void inc() { count.increment(); }                 // 超高并发
    public int sum() { return count.sum(); }                 // 最终一致性
}
```

---

## 5. 数组原子类：AtomicIntegerArray/AtomicLongArray/AtomicReferenceArray

### 5.1 核心特点

数组原子类**不是**对整个数组加锁，而是对**单个元素**执行CAS操作，粒度更细。

```java
public class AtomicArrayDemo {
    private final AtomicIntegerArray array = new AtomicIntegerArray(10);

    public void demo() {
        int v = array.get(0);                // volatile读
        array.set(0, 100);                   // volatile写
        array.compareAndSet(0, 100, 200);    // 单元素CAS
        array.getAndIncrement(3);
        array.getAndAdd(3, 5);
    }
    // 索引CAS原理：Unsafe.arrayBaseOffset + i * arrayIndexScale
}
```

### 5.2 数组原子类对比

| 类名 | 内部数组类型 | 适用场景 |
|------|-------------|---------|
| `AtomicIntegerArray` | `int[]` | 统计数组、计数器数组 |
| `AtomicLongArray` | `long[]` | 时间戳数组、长整型数据池 |
| `AtomicReferenceArray<E>` | `Object[]` | 对象池、缓存数组 |

---

## 6. 引用原子类：AtomicReference / AtomicStampedReference / AtomicMarkableReference

### 6.1 AtomicReference —— 无锁栈实现

```java
/**
 * 基于 AtomicReference 的无锁栈（Treiber Stack）
 */
public class LockFreeStack<T> {
    private final AtomicReference<Node<T>> top = new AtomicReference<>(null);

    public void push(T value) {
        Node<T> newHead = new Node<>(value);
        Node<T> oldHead;
        do { oldHead = top.get(); newHead.next = oldHead; }
        while (!top.compareAndSet(oldHead, newHead)); // CAS自旋
    }

    public T pop() {
        Node<T> oldHead, newHead;
        do {
            oldHead = top.get();
            if (oldHead == null) return null;
            newHead = oldHead.next;
        } while (!top.compareAndSet(oldHead, newHead));
        return oldHead.value;
    }

    private static class Node<T> {
        final T value; Node<T> next;
        Node(T v) { this.value = v; }
    }
}
```

### 6.2 AtomicStampedReference —— 带版本号防ABA

```java
/**
 * AtomicStampedReference 内部结构：Pair<reference, stamp>
 * CAS同时对 reference 和 stamp 做原子更新
 */
public class AtomicStampedRefDemo {
    private final AtomicStampedReference<BigDecimal> balance =
            new AtomicStampedReference<>(BigDecimal.ZERO, 0);

    public boolean transfer(BigDecimal amount) {
        int[] stampHolder = new int[1];
        BigDecimal current = balance.get(stampHolder);
        int currentStamp = stampHolder[0];
        BigDecimal newBalance = current.add(amount);
        // 版本号和值同时匹配才更新
        return balance.compareAndSet(
                current, newBalance, currentStamp, currentStamp + 1);
    }
}
```

### 6.3 三种引用原子类对比

| 类名 | 更新条件 | ABA防护 | 使用场景 |
|------|---------|---------|---------|
| `AtomicReference<V>` | 值变化检测 | ❌ 无防护 | 无锁数据结构、配置更新 |
| `AtomicStampedReference<V>` | 值 + int版本号 | ✅ 完全防护 | 资金操作、版本敏感数据 |
| `AtomicMarkableReference<V>` | 值 + boolean标记 | ⚠️ 有限防护 | 标记对象是否"被删除/失效" |

> 💡 `AtomicStampedReference` 内部将 `reference` 和 `stamp` 封装为 `Pair` 对象，每次修改创建新 `Pair`，有一定GC压力。

---

## 7. LongAdder与LongAccumulator（JDK 8+分层累加原理）

### 7.1 LongAdder设计思想

核心思想：**空间换时间**——将单个热点变量拆分为多个Cell，每个线程操作自己的Cell，最终求和时汇总。

```
LongAdder 内部结构：

  base (volatile long) ← 低竞争时直接CAS
  cells (volatile Cell[]) ← 高竞争时扩容
    [Cell 0] [Cell 1] [Cell 2] [Cell 3]
       ↑        ↑        ↑       ↑
    Thread1  Thread2  Thread3  Thread4
    (线程probe哈希映射到Cell)

  cellsBusy: 0=未锁定 1=锁定中（CAS自旋锁）

sum() = base + 遍历所有Cell求和
⚠️ sum() 无锁，只保证最终一致性，非强一致快照
```

### 7.2 LongAdder核心逻辑

```java
/**
 * LongAdder add(x) 流程（简化版）：
 * 1. cells==null && CAS(base)成功 → 直接累加（快速路径）
 * 2. cells!=null → 按线程probe定位Cell，CAS累加
 * 3. 上述都失败 → longAccumulate()：扩容/创建Cell/重试
 */
public class LongAdderPrinciple {
    public void add(long x) { /* 以上三步流程 */ }

    public long sum() {
        Cell[] cs = cells;
        long sum = base;
        if (cs != null) for (Cell c : cs) if (c != null) sum += c.value;
        return sum;
    }

    // Cell通过@Contended防止伪共享（填充缓存行，避免多核修改同一缓存行）
    @jdk.internal.vm.annotation.Contended
    static final class Cell {
        volatile long value;
        Cell(long x) { value = x; }
        final boolean cas(long cmp, long val) {
            return UNSAFE.compareAndSwapLong(this, valueOffset, cmp, val);
        }
    }
}
```

### 7.3 LongAccumulator —— 更通用的累加器

```java
// LongAccumulator 支持任意二元运算，内部机制与LongAdder完全一致
public class LongAccumulatorDemo {
    private final LongAccumulator maxAccumulator =
            new LongAccumulator(Long::max, Long.MIN_VALUE);
    public void recordLatency(long latency) {
        maxAccumulator.accumulate(latency);
    }
}
```

### 7.4 性能对比

| 场景 | AtomicLong | LongAdder | 结论 |
|------|-----------|-----------|------|
| 低竞争（2线程） | ~20M ops/s | ~15M ops/s | AtomicLong略优 |
| 中竞争（8线程） | ~5M ops/s | ~25M ops/s | LongAdder优5倍 |
| 高竞争（32线程） | ~1M ops/s | ~30M ops/s | LongAdder优30倍 |
| 内存占用 | 1个long值 | Cell[]数组 + base | LongAdder更多 |

> 🎯 选型建议：高并发统计（QPS、访问量、延迟）优先 `LongAdder`；需要强一致性的ID生成器用 `AtomicLong`。

---

## 8. CAS vs synchronized vs Lock性能对比

| 维度 | CAS（无锁） | synchronized | ReentrantLock |
|------|-----------|-------------|---------------|
| 原理 | CPU指令原子性 | Monitor管程 | AQS + CLH队列 |
| 线程阻塞 | ❌ 不阻塞（自旋） | ✅ 阻塞（BLOCKED） | ✅ 阻塞（WAITING） |
| 上下文切换 | 无 | 用户态↔内核态 | 有 |
| 适用竞争强度 | 低竞争 | 中高竞争 | 中高竞争 |
| 代码复杂度 | 简单（不需释放） | 简单（自动释放） | 需finally解锁 |
| 超时/中断 | 不支持 | 不支持 | 支持（tryLock/lockInterruptibly） |
| 可组合性 | 可组合CAS操作 | 不可中断 | 可配合Condition |

```text
吞吐量趋势：低竞争CAS >> Lock > synchronized；中竞争Lock ≈ CAS > synchronized；
高竞争synchronized ≈ Lock >> CAS（CAS自旋风暴导致性能急剧下降）
```

> ⚠️ 不要盲目使用CAS：高竞争下CAS自旋风暴会导致CPU飙升，JDK 8+的synchronized经过锁升级优化，高竞争下表现已很好。

---

## 9. 原子类选型指南

```text
需要线程安全的计数器或状态标记？
│
├─ 计数场景（高频累加）？
│  ├─ 需要强一致性快照 → AtomicLong / AtomicInteger
│  └─ 允许最终一致性 → LongAdder / LongAccumulator（高并发推荐）
│
├─ 需要对象引用原子更新？
│  ├─ 简单引用更新 → AtomicReference
│  ├─ 需防止ABA → AtomicStampedReference（版本号）
│  └─ 仅需标记修改 → AtomicMarkableReference
│
├─ 需要数组元素原子更新？
│  └─ AtomicIntegerArray / AtomicLongArray / AtomicReferenceArray
│
├─ 需要更新已有对象的字段（不修改类设计）？
│  └─ AtomicIntegerFieldUpdater / AtomicReferenceFieldUpdater
```

```java
/**
 * 原子类经典用法组合（企业实战常见场景）
 */
public class AtomicBestPractice {
    private final LongAdder requestCount = new LongAdder();       // 高并发请求计数
    private final AtomicLong idGenerator = new AtomicLong(0);     // 严格递增ID
    private final AtomicBoolean initialized = new AtomicBoolean(false); // 一次性开关
    private final AtomicReference<CacheConfig> config =
            new AtomicReference<>(new CacheConfig(1000, 60));     // 配置原子更新
    private final AtomicStampedReference<BigDecimal> balance =
            new AtomicStampedReference<>(BigDecimal.ZERO, 0);     // 带版本号的资金
    private final LongAccumulator maxLatency =
            new LongAccumulator(Long::max, Long.MIN_VALUE);       // 最大值统计
}
```

---

## 10. 面试高频考点

### 10.1 基础问题

**Q1: CAS原理？为什么比锁轻量？**

CPU原子指令（x86 `lock cmpxchg`）。不需要OS介入线程挂起/恢复，无用户态到内核态切换；低竞争时短暂自旋，无上下文切换成本。

**Q2: CAS三大问题及解决方案？**

ABA问题（AtomicStampedReference加版本号）、自旋开销大（LongAdder分段/退避策略）、单变量限制（AtomicReference封装对象）。

**Q3: AtomicInteger.incrementAndGet() 底层？**

`Unsafe.getAndAddInt(this, valueOffset, 1)` → do-while自旋：volatile读当前值，CAS写新值，失败重试 → CPU `lock cmpxchg`。

### 10.2 进阶问题

**Q4: LongAdder相比AtomicLong优势？**

AtomicLong所有线程CAS同一变量，高竞争自旋飙升。LongAdder拆分为Cell数组，线程映射到不同Cell，大幅降低CAS冲突。空间换时间。

**Q5: LongAdder.sum() 为何无锁？**

遍历Cell无锁，非严格一致性快照。统计计数（QPS、UV等）可接受最终一致性。

**Q6: AtomicReferenceFieldUpdater vs AtomicReference？**

AtomicReference需创建新对象。FieldUpdater通过反射+字段偏移直接操作已有volatile字段，无需改原结构，适合遗留代码改造。

### 10.3 深度问题

**Q7: 伪共享（False Sharing）？**

多核修改同一缓存行的不同变量，导致缓存行失效传递。LongAdder的Cell类用 `@Contended` 填充128字节，保证不同Cell在不同缓存行：

```
┌───────── 缓存行 ─────────┐
│ 填充 │ Cell0.value │ ... │
└──────────────────────────┘
```

**Q8: 手写CAS自旋锁**

```java
class SpinLock {
    private final AtomicReference<Thread> owner = new AtomicReference<>(null);
    public void lock() {
        Thread t = Thread.currentThread();
        while (!owner.compareAndSet(null, t)) {}
    }
    public void unlock() {
        owner.compareAndSet(Thread.currentThread(), null);
    }
}
```

**Q9: TLAB和CAS的关系？**

TLAB是JVM的线程私有Eden分配缓冲区，通过CAS更新`top`指针快速分配：`CAS(top, oldTop, oldTop + size)`。失败则申请新TLAB或进入Eden全局分配——CAS在JVM内部的经典应用。

### 10.4 对比总结

| 问题 | 核心回答要点 |
|------|------------|
| CAS vs 锁 | CAS无锁、不阻塞、无上下文切换；锁会阻塞线程，有切换成本 |
| ABA问题 | 值A→B→A导致CAS误判；`AtomicStampedReference`版本号解决 |
| LongAdder原理 | Cell分段，线程操作独立Cell，sum()汇总；减少CAS冲突 |
| AtomicInteger vs LongAdder | 低竞争AtomicInteger快；高竞争LongAdder优30倍 |
| 自旋优化 | `Thread.onSpinWait()`(JDK 9+)、退避策略、`yield()` |

---

> 🎯 **核心总结**：CAS是CPU提供的无锁并发原语，是JUC原子类的基石。理解CAS原理、ABA问题与解决方案、LongAdder的分段思想，是掌握Java高并发的关键一步。工程实践中：低竞争直接用AtomicInteger，高并发统计用LongAdder，防ABA用AtomicStampedReference。
