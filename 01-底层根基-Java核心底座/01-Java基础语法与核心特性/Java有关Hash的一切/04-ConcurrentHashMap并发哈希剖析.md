# 04 - ConcurrentHashMap 并发哈希剖析

> 定位：梳理并发哈希的演进路线（Hashtable → JDK 7 分段锁 → JDK 8 CAS+synchronized），逐段剖析 put/get/size 的并发设计

## 📚 目录

1. [并发哈希的演进路线](#1-并发哈希的演进路线)
2. [JDK 7：分段锁设计](#2-jdk-7分段锁设计)
3. [JDK 8：CAS + synchronized 设计总览](#3-jdk-8cas--synchronized-设计总览)
4. [put() 并发流程源码](#4-put-并发流程源码)
5. [并发扩容：多线程协作迁移](#5-并发扩容多线程协作迁移)
6. [无锁读：get() 与 size()](#6-无锁读get-与-size)
7. [JDK 7 vs 8 对比与演进现状](#7-jdk-7-vs-8-对比与演进现状)
8. [并发实践与踩坑](#8-并发实践与踩坑)
9. [面试高频考点](#9-面试高频考点)

---

## 1. 并发哈希的演进路线

```
JDK 1.0              JDK 1.5                  JDK 8 至今
Hashtable     →     ConcurrentHashMap   →    ConcurrentHashMap
(全表锁)            (分段锁 16 段)           (CAS + synchronized)
并发度 1             并发度 ≤ 16              并发度 = 桶数
```

| 方案 | 锁粒度 | 问题/优势 |
|------|--------|----------|
| Hashtable | 整张表一把锁 | 任何读写都串行，并发度 = 1，已过时 |
| CHM JDK 7 | Segment 分段锁 | 并发度固定 16，扩容锁段内 |
| CHM JDK 8 | 桶头节点锁 + CAS | 并发度 = 桶数，无锁读 |

---

## 2. JDK 7：分段锁设计

```
Segment[]（继承 ReentrantLock，默认 16 个）
├─ Segment[0] → 小 HashMap（数组+链表）
├─ Segment[1] → 小 HashMap
└─ ...
写操作：只锁对应 Segment，不同 Segment 可并行
并发度上限 = 分段数 = 16（构造时指定，不可扩容段数）
```

- **优点**：比 Hashtable 并发度高 16 倍
- **缺点**：并发度固定；扩容时该段内整体 rehash（JDK 8 改为桶级迁移）；内存多一层包装

---

## 3. JDK 8：CAS + synchronized 设计总览

### 3.1 设计思想

```
读：无锁（volatile 数组 + CAS 获取桶引用）
写：空桶 → CAS 直接写入（最频繁路径，无锁）
    非空桶 → synchronized(桶头节点)（细粒度，不同桶互不干扰）
扩容：多线程协作（来了一个帮一把）
计数：LongAdder 思想（CounterCell 分片累加）
```

### 3.2 sizeCtl：多用途控制字段

```java
private transient volatile int sizeCtl;

// 取值语义：
//   -1              ：正在初始化
//   -(1 + n)        ：n 个线程正在协作扩容
//   0               ：默认值（表未初始化）
//   正数             ：扩容阈值（表已初始化时）= 容量 * 0.75
```

> 🎯 **sizeCtl 是 CHM 的"状态寄存器"**：初始化竞争、扩容进度、扩容阈值全靠这一个 volatile 字段 + CAS 完成同步。

### 3.3 spread()：CHM 的扰动函数

```java
// 与 HashMap.hash() 相比多了一步 & HASH_BITS（清除符号位，保证非负）
static final int spread(int h) {
    return (h ^ (h >>> 16)) & HASH_BITS;   // HASH_BITS = 0x7fffffff
}
```

---

## 4. put() 并发流程源码

```java
final V putVal(K key, V value, boolean onlyIfAbsent) {
    if (key == null || value == null) throw new NullPointerException();
    int hash = spread(key.hashCode());
    int binCount = 0;
    for (Node<K,V>[] tab = table;;) {          // ⭐ 自旋：失败就重试
        Node<K,V> f; int n, i, fh;
        if (tab == null || (n = tab.length) == 0)
            tab = initTable();                              // ① 初始化
        else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
            // ② 空桶：CAS 直接放入（无锁！失败说明被抢，自旋重来）
            if (casTabAt(tab, i, null, new Node<>(hash, key, value)))
                break;
        }
        else if ((fh = f.hash) == MOVED)
            tab = helpTransfer(tab, f);        // ③ 遇到扩容转发节点 → 帮忙迁移
        else {
            V oldVal = null;
            synchronized (f) {                 // ④ 非空桶：锁桶头节点
                if (tabAt(tab, i) == f) {      //    双重检查：锁后确认桶没变
                    if (fh >= 0) {             //    链表插入（尾插，同 HashMap）
                        ...
                    }
                    else if (f instanceof TreeBin) {  // 红黑树插入
                        ...
                    }
                }
            }
            if (binCount >= TREEIFY_THRESHOLD)
                treeifyBin(tab, i);            // ⑤ 树化
        }
    }
    addCount(1L, binCount);                    // ⑥ 分片计数
    return null;
}
```

**四个关键设计**：

| 设计 | 目的 |
|------|------|
| CAS 写空桶 | 最频繁路径无锁，只有并发写同一空桶时才冲突 |
| synchronized 锁头节点 | 锁粒度 = 桶，而非全表或分段 |
| 双重检查 `tabAt(tab,i) == f` | 防止锁后桶已被替换（扩容/删除） |
| MOVED 帮助扩容 | 所有线程一起搬，扩容不再阻塞整体写入 |

---

## 5. 并发扩容：多线程协作迁移

### 5.1 ForwardingNode 转发节点

```java
// 扩容期间，已迁移完的桶放一个 ForwardingNode（hash == MOVED）
// 后续线程 put/get 到该桶 → 转发到新表继续操作

static final class ForwardingNode<K,V> extends Node<K,V> {
    final Node<K,V>[] nextTable;
    ForwardingNode(Node<K,V>[] tab) { super(MOVED, null, null, null); ... }
}
```

### 5.2 协作流程

```
线程1 触发扩容（size > sizeCtl）→ 创建 nextTable（2 倍）
线程1 迁移一段桶（stride 步长），迁移完桶位放 ForwardingNode
线程2 写操作遇到 MOVED → helpTransfer()：认领下一段桶继续迁移
迁移完成 → table 指向 nextTable，sizeCtl 更新为新阈值
```

| 特性 | 说明 |
|------|------|
| 迁移粒度 | 一段一段（stride = NCPU > 1 ? N/8/NCPU : N），多线程各领一段 |
| 数据一致性 | 迁移完成的桶用 CAS 写入新表，未完成的桶仍在旧表 |
| 读一致性 | get 遇到 ForwardingNode 会**转发到新表**继续找 |

> 🎯 **核心思想**：扩容不阻塞——正在扩容时新写入照常进行（写旧表或转发新表），每个线程"搭把手"让总迁移时间随线程数下降。

---

## 6. 无锁读：get() 与 size()

### 6.1 get()：全程无锁

```java
public V get(Object key) {
    Node<K,V>[] tab; Node<K,V> e, p; int n, eh; K ek;
    int h = spread(key.hashCode());
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (e = tabAt(tab, (n - 1) & h)) != null) {
        if ((eh = e.hash) == h) { ... 头节点命中 ... }
        else if (eh < 0)                // 树或转发节点
            return (p = e.find(h, key)) != null ? p.val : null;
        else { ... 链表遍历 ... }        // 普通链表
    }
    return null;
}
```

**为什么无锁安全**：table 是 volatile（可见性），桶内 Node 的 next/val 都是 volatile；即使读到"半写入"状态也只会得到旧值，不会崩溃——这就是**弱一致性**（读可能暂时滞后，但终将一致）。

### 6.2 size() / mappingCount()：分片计数

```java
// LongAdder 思想：CounterCell[] 分片，各线程累加到不同片，避免单计数竞争
private transient volatile CounterCell[] counterCells;

// size() 内部：sumCount() = 遍历所有 CounterCell 求和 + baseCount
// 计数不准时（竞争激烈）才退化为加锁快照统计
```

> 💡 `mappingCount()` 与 `size()` 等价（返回值 long），官方建议用前者，因为 size 可能溢出。

---

## 7. JDK 7 vs 8 对比与演进现状

| 特性 | JDK 7 | JDK 8+ |
|------|-------|--------|
| 锁 | Segment（ReentrantLock） | CAS + synchronized（桶头） |
| 并发度 | 固定 16 | 桶数（动态） |
| 数据结构 | 数组+链表 | 数组+链表+红黑树 |
| 扩容 | 段内整体 rehash | 多线程分段协作迁移 |
| 计数 | Segment 内计数 | CounterCell 分片 |
| 读 | 无锁 | 无锁 |
| null key/value | 不允许 | 不允许 |

**演进现状（2026-08 验证）**：CHM 核心结构自 JDK 8 稳定至今（JDK 17/21/25/26 未改动核心算法）；JDK 12 起对 `keySet()` 视图、批量操作等做了小幅优化，但 put/get/resize 三件套逻辑不变。学习 JDK 8 源码即可应对当前所有版本面试。

---

## 8. 并发实践与踩坑

### 8.1 使用规范

```java
// ✅ 正确姿势
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
map.putIfAbsent("k", 1);                       // 原子：没有才放
map.computeIfAbsent("k", k -> expensive());    // 原子：懒计算（JDK 8 保证每个 key 只执行一次）
map.compute("k", (k, v) -> v == null ? 1 : v + 1);   // 原子：读改写

// ❌ 错误姿势（非原子的先查后写，多线程下丢更新）
if (!map.containsKey("k")) map.put("k", 1);    // 竞态窗口存在！
```

| 场景 | 方法 | 说明 |
|------|------|------|
| 不存在才写入 | putIfAbsent | 原子 |
| 值懒加载 | computeIfAbsent | 计算函数只执行一次 |
| 累计/计数 | compute/merge | 原子读改写 |
| 批量安全读写 | ConcurrentHashMap 包装 | 单个操作原子，组合操作需自己加锁 |

### 8.2 常见误区

| 误区 | 正解 |
|------|------|
| "CHM 完全线程安全" | 单操作原子，**多步组合操作仍需外部同步** |
| "可以用 null 值" | 不允许 null key/value（与 HashMap 不同）——因为无法区分"值为 null"与"不存在"（并发下二义） |
| "size() 实时精确" | 弱一致，仅近似值；精确统计要加锁遍历 |
| "有序" | 无序！要排序用 ConcurrentSkipListMap（O(log n)） |

---

## 9. 面试高频考点

**Q1: CHM JDK 8 为什么快？** 空桶 CAS 无锁（最频繁路径），非空桶锁桶头（细粒度），读全程无锁，扩容协作。

**Q2: 为什么用 synchronized 而不用 ReentrantLock？** JDK 8 synchronized 经优化（偏向/轻量锁）性能已不输 ReentrantLock，且代码更简洁、锁可随 JIT 优化；锁对象就是桶头节点。

**Q3: 为什么不允许 null？** HashMap 允许 null 因为单线程可用 `get()==null` 区分；并发下无法区分"不存在"与"值为 null"，为避免二义性直接禁止。

**Q4: 扩容时其他线程干什么？** 写操作遇到 MOVED 转发节点就 helpTransfer 一起搬；读操作转发到新表。

**Q5: size() 怎么实现？** LongAdder 分片计数（CounterCell[]），竞争时退化为锁快照；mappingCount 防溢出。

**Q6: 弱一致性是什么？** 读操作不加锁，可能读到旧值但绝不读到破坏状态；适合"读多写少、容忍瞬时旧值"的场景。

**Q7: 与 ConcurrentSkipListMap 区别？** CHM 无序、桶级锁、不支持 null；CSLM 有序 O(log n)、支持范围查询，选型看是否需要排序。

---

> 🎯 **核心要点**：CHM 的并发哲学是"把锁拆到最小 + 让无锁路径覆盖最频繁操作"。空桶 CAS、非空桶锁头、扩容协作、分片计数——四个机制组合出 JDK 8 至今最优的并发哈希方案。

---

**下一模块**：[05-哈希碰撞攻击与HashDoS](05-哈希碰撞攻击与HashDoS.md)（哈希的安全面） / **返回总览**：[00-Hash总览](00-Hash总览.md)
