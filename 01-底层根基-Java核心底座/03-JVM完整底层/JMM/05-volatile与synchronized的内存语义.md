# volatile与synchronized的内存语义
> volatile 的屏障插入与适用边界、synchronized 的 monitorenter/exit 语义、锁的可见性传递——Java 同步原语的内存模型解剖。

---

## 📚 目录

1. [volatile 的内存语义](#1-volatile-的内存语义)
2. [volatile 的屏障插入](#2-volatile-的屏障插入)
3. [volatile 的适用边界](#3-volatile-的适用边界)
4. [synchronized 的内存语义](#4-synchronized-的内存语义)
5. [锁的可见性传递](#5-锁的可见性传递)
6. [两者对比与选型](#6-两者对比与选型)

---

## 1. volatile 的内存语义

### 1.1 语义总览

| 维度 | volatile 保证 | 不保证 |
|------|-------------|--------|
| 可见性 | 写 HB 后续读（同一变量） | — |
| 有序性 | 读写两侧不跨越（屏障） | 无关操作的相互重排 |
| 原子性 | 单个读/写（含 long/double） | 读-改-写复合操作 |
| 互斥 | 无 | — |

```text
volatile 变量特性：
  ① 读写操作不会使用寄存器/缓存副本（直接走内存语义）
  ② 编译器不缓存、不重排（在屏障范围内）
  ③ 单个 volatile 读写的原子性（JDK 5 起含 long/double）
```

### 1.2 核心定位

```text
volatile = "状态标志 + 轻量同步"工具：
  适用：一个线程写、多个线程读的简单状态
  不适用：读-改-写（计数）、复合状态（需锁）

典型场景：
  运行标志（stop/ready）、状态发布（配置更新）、
  double-checked locking、单写者-多读者
```

> 🎯 一句话：**volatile 是"可见性开关"而非"原子性工具"**——它让一个变量的读写跨线程有序，但不阻止"多步操作"被撕开。

---

## 2. volatile 的屏障插入

### 2.1 JMM 要求的插入规则

```text
volatile 写（编译器需插入）：
  写前：LoadStore + StoreStore 屏障（之前的读写先完成）
  写后：StoreLoad 屏障（后续读不能读到旧值——x86 上必要）

volatile 读（编译器需插入）：
  读后：LoadLoad + LoadStore 屏障（后续读写不能提前）

含义：
  写 volatile 前，本线程所有普通写已完成
  读 volatile 后，本线程后续读不再读到更旧的值
```

### 2.2 平台落地（HotSpot）

| 平台 | volatile 写 | volatile 读 |
|------|-----------|------------|
| x86（TSO） | lock 前缀（或 xchg）→ 全屏障 | 普通读（硬件已保证） |
| ARM（弱） | stlr（写释放） | ldar（读获取） |
| 作用 | 刷出 store buffer | 等待失效队列 |

```text
x86 上 volatile 的成本真相：
  读：≈ 0（普通 load，TSO 保证 LoadLoad/LoadStore）
  写：lock 前缀（约几十周期，但远低于直觉的"几百周期"）

ARM 上 volatile 的成本：
  读写各一条 ldar/stlr（有真实指令开销）
  → 移动端并发代码的成本更高
```

### 2.3 volatile 与编译器优化

```text
volatile 对 JIT 的约束：
  ① 不可消除（即使"看起来多余"）
  ② 不可重排（跨屏障边界）
  ③ 不可缓存到寄存器（每次必须访问内存）

代价：优化受限 → 热点循环中的 volatile 读写可能显著
  例：高并发计数器用 volatile 循环自旋 → 每次全屏障
  → 正确但慢；优化方案：LongAdder（分片计数，见 07 模块）
```

---

## 3. volatile 的适用边界

### 3.1 经典适用场景

| 场景 | 示例 | 为什么可行 |
|------|------|-----------|
| 运行标志 | `volatile boolean running` | 单写多读，无复合操作 |
| 状态发布 | `volatile Config config` | 发布后只读（final 字段配合） |
| DCL 单例 | `volatile static Singleton` | 初始化发布 |
| 单写者计数 | 一线程写计数，多线程读 | 无读-改-写竞争 |

### 3.2 经典不适用场景

| 场景 | 错误 | 正确做法 |
|------|------|---------|
| 计数器 | `volatile int count; count++` | AtomicInteger |
| 检查-执行 | `if (!volatileSet.contains(k))` | 锁/并发集合 |
| 复合状态 | 多个 volatile 变量的一致性 | 锁/AtomicReference |
| 生产者-消费者 | volatile 队列指针 | BlockingQueue |

```text
判定口诀：只读不写复合 → volatile 够用
        写-读-改 / 多变量一致 → 上锁或原子类
```

---

## 4. synchronized 的内存语义

### 4.1 字节码与语义

```text
synchronized 的字节码：monitorenter / monitorexit

内存语义（与 volatile 的对照）：
  monitorenter（获取锁）≈ volatile 读 + 原子操作
  monitorexit（释放锁） ≈ volatile 写

规范保证（04 模块规则二）：
  unlock HB 后续同一锁的 lock
  → 释放前写的全部内容，获取后可见
```

### 4.2 synchronized 的三重角色

| 角色 | 保证 | 对应规则 |
|------|------|---------|
| 互斥 | 临界区同时只有一个线程 | 锁的排他性 |
| 原子性 | 临界区整体不可分割 | 互斥的推论 |
| 可见性 | 释放 HB 获取（同一锁） | 规则二 |

```text
synchronized 的优势：三者一体（"一把锁全搞定"）
代价：互斥带来的串行化（吞吐上限）

注意：锁的可见性只在"同一把锁"上成立
  → 读写双方必须用同一把锁（否则无 HB 边）
```

### 4.3 锁的实现层级（详见 06 模块）

```text
synchronized 的底层演化：
  偏向锁（JDK 15 默认禁用、JDK 18 移除）
  轻量级锁（CAS 自旋）
  重量级锁（Monitor，阻塞唤醒）

JEP 491（JDK 24）重大变化：
  虚拟线程不再因 synchronized 被 pinning
  → 锁的监视器所有权从"载体线程"改为"虚拟线程自身"
```

---

## 5. 锁的可见性传递

### 5.1 一写多读的锁模式

```java
class SafePublish {
    private volatile boolean ready = false;
    private String data;

    void publish(String d) {
        synchronized (this) {          // ① 锁内写
            data = d;
            ready = true;
        }
    }

    String read() {
        if (ready) return data;        // ② 无锁读
        return null;
    }
}
// 正确性：① 的 unlock HB ② 的……？—— ② 没有 lock！
// 分析：read 无锁 → 与 publish 之间无锁的 HB 边
// 但 volatile ready 提供：data=d HB ready=true（程序顺序）
//   → ready 写 HB ready 读（volatile 规则）→ data 可见 ✓
// 结论：锁内写 + volatile 标志发布 = 安全发布模式
```

### 5.2 纯锁模式的可见性

```java
class Counter {
    private int count = 0;

    synchronized void inc() { count++; }     // 写
    synchronized int get() { return count; } // 读

    // 正确性：inc 的 unlock HB get 的 lock（同一 this 锁）
    //   → count 必然可见 ✓
    // 注意：若 get 不加 synchronized → 无 HB 边 → 可能读到旧值 ✗
}
```

> ⚠️ 高频陷阱："读操作加不加锁都行，反正写加了锁"——**错**。读方必须也走同一把锁（或等价的 HB 边），否则读到旧值是合法的。

### 5.3 锁与 volatile 的混合工程

```text
工程模式：写多读少 → 锁保护写 + volatile 发布快照
  例：配置热更新（写端锁，读端 volatile 引用 + 不可变对象）

模式：计数器 → AtomicInteger（无锁 CAS）
  例：统计、限流（LongAdder 更优）

模式：复合状态 → 锁或 AtomicReference
  例：账户余额（balance 变更需原子）
```

---

## 6. 两者对比与选型

### 6.1 全面对比

| 维度 | volatile | synchronized |
|------|:---:|:---:|
| 互斥 | ❌ | ✅ |
| 原子性 | 单操作 | 临界区整体 |
| 可见性 | ✅（变量级） | ✅（锁的 HB） |
| 有序性 | ✅（屏障） | ✅（锁的 HB） |
| 成本（x86） | 读≈0/写 lock | 无竞争≈CAS，竞争≈阻塞 |
| 适用 | 状态标志/发布 | 复合操作/互斥 |

### 6.2 选型决策树

```text
需要互斥？ ──是──→ synchronized / ReentrantLock
    否 ↓
读-改-写？ ──是──→ Atomic* 类（CAS）
    否 ↓
多变量一致？ ──是──→ 锁 或 AtomicReference（不可变快照）
    否 ↓
         → volatile（单变量状态/发布）
```

> 🎯 **核心要点**：volatile 是"轻量可见性开关"（x86 上读免费、写一个 lock），synchronized 是"重量级三合一"（互斥+原子+可见）。选型的关键是回答三个问题：需要互斥吗？是读-改-写吗？多变量一致吗？——三个否，才轮到 volatile。

---

## 7. 小结

| 问题 | 答案 |
|------|------|
| volatile 保证什么？ | 可见性 + 有序性（单变量），无原子性 |
| 屏障怎么插？ | 写前 LoadStore/StoreStore、写后 StoreLoad；读后 LoadLoad/LoadStore |
| x86 上 volatile 贵吗？ | 读≈0、写≈lock 前缀（比直觉便宜） |
| synchronized 语义？ | monitorenter≈volatile 读+原子、monitorexit≈volatile 写 |
| 读方不加锁行吗？ | 不行——无 HB 边 = 可读旧值 |
| 选型三问？ | 互斥？读-改-写？多变量？全否才用 volatile |

**下一模块**：[06-原子性与CAS](06-原子性与CAS.md)　**返回总览**：[00-JMM知识体系总览](00-JMM知识体系总览.md)
