# 原子性与CAS
> 原子操作的范围、CAS 的语义与 ABA 问题、Atomic 类家族、synchronized 锁升级（偏向锁的移除）——原子性的完整图谱。

---

## 📚 目录

1. [原子性的范围](#1-原子性的范围)
2. [CAS：比较并交换](#2-cas比较并交换)
3. [ABA 问题](#3-aba-问题)
4. [Atomic 类家族](#4-atomic-类家族)
5. [synchronized 的锁升级](#5-synchronized-的锁升级)
6. [原子性工程实践](#6-原子性工程实践)

---

## 1. 原子性的范围

### 1.1 JMM 保证的原子操作

```text
JMM 规定天然原子的操作：
  ① 引用类型的读写（对象引用）
  ② 基本类型读写（boolean/byte/short/int/float）
  ③ 非 volatile 的 long/double？—— 规范上不保证（JDK 5 起
     volatile long/double 原子；普通 long/double 允许撕裂）

补充（实践中）：64 位 JVM 上普通 long/double 读写
  在 HotSpot 中实际原子（无撕裂），但规范不承诺
  → 依赖规范：long/double 请用 volatile 或原子类
```

### 1.2 原子 vs 复合

```text
原子操作：单条读写（不可分割）
复合操作：读-改-写、检查-执行（可被撕开）

  count++ 的三步：读 count → 加 1 → 写 count
  中间任何一步可被打断 → 更新丢失

工程推论：凡"读-改-写"必须用原子类/锁
  这就是 volatile 计数器错误的根本原因（05 模块）
```

---

## 2. CAS：比较并交换

### 2.1 定义与语义

```text
CAS（Compare-And-Swap）：
  if (内存值 == 期望值) 内存值 = 新值；返回是否成功
  硬件原子指令（x86：cmpxchg；ARM：cas）

Java 暴露：sun.misc.Unsafe.compareAndSwapInt 等
  → AtomicInteger.compareAndSet(expected, new)

CAS 的语义特点：
  ① 原子（硬件保证）
  ② 无锁（自旋等待而非阻塞）
  ③ 乐观（假设冲突少，冲突则重试）
```

```java
// CAS 自旋实现原子递增（AtomicInteger 的内部逻辑）
public final int incrementAndGet() {
    for (;;) {
        int current = get();
        int next = current + 1;
        if (compareAndSet(current, next)) return next;
        // 失败 → 重读重试（自旋）
    }
}
```

### 2.2 CAS 与锁的对比

| 维度 | CAS（无锁） | 锁（阻塞） |
|------|:---:|:---:|
| 等待方式 | 自旋（忙等） | 阻塞/唤醒 |
| 冲突少时 | 快（无上下文切换） | 有切换开销 |
| 冲突多时 | 自旋浪费 CPU | 阻塞让出 CPU |
| 公平性 | 无（可能饿死） | 可公平 |
| 适用 | 短临界区、低冲突 | 长临界区、高冲突 |

```text
工程经验：
  短操作 + 低冲突 → CAS（原子类默认）
  长操作 / 高冲突 → 锁
  超高并发计数 → LongAdder（分片 CAS，07 模块）
```

---

## 3. ABA 问题

### 3.1 定义

```text
ABA：CAS 比较时，值从 A 变 B 又变回 A
  → CAS 认为"没变过"（只比较值）→ 误判成功

场景（经典）：
  线程 1 读 value = A
  线程 2：value A→B→A（两次修改又还原）
  线程 1 CAS(A, C) → 成功（但期间状态已变化！）
  若 A 代表"某对象引用"，对象可能已被替换——危险
```

### 3.2 示例：无锁栈

```java
// 无锁栈的 ABA 风险
// 栈顶 top = Node(A)，线程 1 准备 CAS(top=A, 新A.next)
// 线程 2 弹出 A（top=B）又压回 A（top=A，但 A.next 已变！）
// 线程 1 CAS 成功 → 栈结构被破坏

// 修复：AtomicStampedReference（版本号）
AtomicStampedReference<Node> top;
// CAS 时同时比较"引用 + 版本戳"
```

### 3.3 解决方案

| 方案 | 机制 | 适用 |
|------|------|------|
| AtomicStampedReference | 引用 + int 版本戳 | 通用 |
| AtomicMarkableReference | 引用 + boolean 标记 | 只需标记 |
| 不可变对象 + 新引用 | 每次变更换新对象 | 值语义场景 |
| 锁 | 互斥消除 ABA | 不追求无锁时 |

```text
判断是否受 ABA 影响：
  只看值 → 可能受影响（指针/对象场景）
  值语义（计数等）→ 通常无害（A→B→A 结果等价）
```

---

## 4. Atomic 类家族

### 4.1 家族总览

| 类别 | 代表 | 用途 |
|------|------|------|
| 标量原子 | AtomicInteger/Long/Boolean | 计数器、标志 |
| 引用原子 | AtomicReference/StampedReference | 无锁对象、快照 |
| 数组原子 | AtomicIntegerArray 等 | 元素级 CAS |
| 字段原子 | AtomicIntegerFieldUpdater | 已有类字段原子化 |
| 累加器 | LongAdder/LongAccumulator | 高并发计数 |
| 复合 | AtomicReference + 不可变快照 | 多变量一致更新 |

### 4.2 LongAdder：高并发计数的答案

```text
问题：AtomicLong 的 CAS 在高冲突时自旋严重（所有线程争同一地址）
方案：LongAdder 分片
  多个 cell（分段计数器）+ 最终 sum() 合并
  → 每个线程 CAS 自己的 cell → 冲突大幅降低
  代价：sum() 非实时（近似值）

适用：统计类（吞吐优先、精确性次要）
  例：TPS 统计、QPS 计数、埋点
不适用：需要精确读-改-写的业务计数
```

### 4.3 Atomic 与 volatile 的联动

```text
Atomic 类的内部结构：
  private volatile int value;   ← 底层就是 volatile！
  CAS 操作在其上原子更新

可见性来源：volatile 写/读的 HB 规则
原子性来源：CAS 硬件指令

→ Atomic 类 = volatile（可见性）+ CAS（原子性）的组合
→ 这也解释了为什么 Atomic 类是"线程安全的"
```

---

## 5. synchronized 的锁升级

### 5.1 四种锁状态（JDK 6 起）

```text
无锁 → 偏向锁 → 轻量级锁 → 重量级锁（单向升级，不降级）

① 偏向锁：首次获取记录线程 ID，无竞争时零开销
   （JDK 15 默认禁用、JDK 18 移除——撤销成本高于收益）
② 轻量级锁：CAS 抢锁（自旋），无竞争时开销小
③ 重量级锁：Monitor 阻塞唤醒，有竞争时让出 CPU

升级条件：竞争加剧（CAS 失败/自旋超限）
```

### 5.2 偏向锁移除后的现状（JDK 18+）

```text
2026 现实（JDK 18+）：
  不再有偏向锁 → synchronized 无竞争时的路径：
    lock：CAS 设锁记录（轻量级）→ 成功即获取
    unlock：CAS 还原
  → 无竞争 synchronized ≈ 一次 CAS（比直觉便宜）

性能含义：
  synchronized 在低竞争下的成本 ≈ ReentrantLock
  → "synchronized 一定慢"是过时观念
  → 简单场景优先 synchronized（可读性 + 自动释放）
```

### 5.3 JEP 491 的锁变化（JDK 24）

```text
虚拟线程与 synchronized（详见 08 模块）：
  旧：monitor 归属"载体线程"→ 虚拟线程阻塞时 pinning 载体
  新（JEP 491）：monitor 归属"虚拟线程自身"
    → 阻塞可卸载载体 → synchronized 不再卡住虚拟线程
  → JDK 24+ 的 synchronized 重新成为虚拟线程安全的选择
```

---

## 6. 原子性工程实践

### 6.1 决策清单

| 场景 | 方案 |
|------|------|
| 计数器（精确） | AtomicLong / AtomicInteger |
| 计数器（高并发统计） | LongAdder |
| 标志/状态 | volatile |
| 引用快照更新 | AtomicReference |
| 多变量一致 | 锁 或 AtomicReference + 不可变对象 |
| 复合业务操作 | synchronized / ReentrantLock |

### 6.2 常见错误

| 错误 | 后果 |
|------|------|
| volatile 做计数器 | 更新丢失 |
| CAS 自旋无重试上限 | 极端竞争下 CPU 打满 |
| 忽视 ABA（指针场景） | 结构破坏 |
| 读方不加锁 | 可见性无保证（05 模块） |
| 用 Atomic 包装大临界区 | 自旋浪费（应上锁） |

> 🎯 **核心要点**：原子性的完整答案 = JMM 天然原子（引用/int）+ CAS（硬件原子）+ 锁（临界区原子）三层。CAS 是"乐观原子"（冲突重试）、锁是"悲观原子"（互斥等待）——选型看冲突率；而 ABA 提醒我们：CAS 比较的"值"可能说谎，需要版本戳。JDK 18+ 的 synchronized（无偏向锁）与 JEP 491（虚拟线程友好）让"锁"在现代 Java 中重新变得轻量。

---

## 7. 小结

| 问题 | 答案 |
|------|------|
| 天然原子操作？ | 引用 + 基本类型读写；long/double 需 volatile |
| CAS 是什么？ | 硬件原子"比较并交换"，无锁自旋 |
| ABA 是什么？ | A→B→A 骗过 CAS 比较，需版本戳 |
| LongAdder 为何快？ | 分片 CAS（每线程自己的 cell） |
| 偏向锁去哪了？ | JDK 15 禁用、JDK 18 移除（撤销成本高） |
| 无竞争 synchronized 成本？ | ≈ 一次 CAS（轻量级锁路径） |

**下一模块**：[07-JMM与并发工具](07-JMM与并发工具.md)　**返回总览**：[00-JMM知识体系总览](00-JMM知识体系总览.md)
