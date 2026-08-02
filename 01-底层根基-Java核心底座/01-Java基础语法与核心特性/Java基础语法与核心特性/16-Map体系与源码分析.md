# 16 - Map体系与源码分析

> 定位：深入HashMap的数组+链表+红黑树结构、put/resize流程、JDK 7→8的优化，理解TreeMap、LinkedHashMap和ConcurrentHashMap的演进

## 目录

1. [Map体系全景图](#1-map体系全景图)
2. [HashMap底层结构：数组+链表+红黑树](#2-hashmap底层结构数组链表红黑树)
3. [HashMap的put流程详解](#3-hashmap的put流程详解)
4. [HashMap扩容机制（resize）](#4-hashmap扩容机制resize)
5. [hash()算法与index计算](#5-hash算法与index计算)
6. [JDK 7 vs JDK 8的HashMap差异](#6-jdk-7-vs-jdk-8的hashmap差异)
7. [LinkedHashMap：LRU缓存实现](#7-linkedhashmaplru缓存实现)
8. [TreeMap：红黑树实现排序](#8-treemap红黑树实现排序)
9. [Hashtable与ConcurrentHashMap演进](#9-hashtable与concurrenthashmap演进)
10. [ConcurrentHashMap源码分析（JDK 8）](#10-concurrenthashmap源码分析jdk-8)
11. [Map选型指南](#11-map选型指南)
12. [面试高频考点](#12-面试高频考点)

---

## 1. Map体系全景图

```
                      Map<K,V> (接口)
                         │
            ┌────────────┼────────────┐
            │            │            │
       HashMap      TreeMap      Hashtable
            │                              │
       LinkedHashMap                   Properties
            │
       ConcurrentHashMap
```

| 实现类 | 底层结构 | 有序性 | 线程安全 | null key | 复杂度 |
|--------|---------|--------|---------|----------|--------|
| HashMap | 数组+链表+红黑树 | 无序 | 否 | 允许一个 | O(1)/O(log n) |
| LinkedHashMap | HashMap+双向链表 | 插入/访问顺序 | 否 | 允许 | O(1) |
| TreeMap | 红黑树 | 自然/比较器顺序 | 否 | 不允许 | O(log n) |
| Hashtable | 数组+链表 | 无序 | 是（全表锁） | 不允许 | O(n) |
| ConcurrentHashMap | 数组+链表+红黑树 | 无序 | 是（CAS+synchronized） | 不允许 | O(1)/O(log n) |

> 💡 **HashMap 是最核心的 Map 实现**，面试出现频率最高。理解 HashMap 就等于理解了 Map 体系的 80%。

---

## 2. HashMap底层结构：数组+链表+红黑树

### 2.1 数据结构

```
JDK 8 HashMap：

┌───────────────────────────────────────────────┐
│ Node<K,V>[] table（桶数组）                      │
├─────┬─────┬─────┬──────┬─────┬──────┬─────────┤
│  0  │  1  │  2  │ ...  │ n-2 │ n-1  │   n     │
├─────┼─────┼─────┼──────┼─────┼──────┼─────────┤
│null │Node │null │ Node │null │ Node │ null    │
│     ├─K──┤     ├─K────┤     ├─K────┤          │
│     │ V  │     │  V   │     │  V   │          │
│     │next──▶   │  ▓   │     │  ▓   │          │
│     └────┘     │  ▓   │     │  ▓   │          │
│                │  ▓   │     │  ▓   │          │
│                └──────┘     └──────┘          │
│              链表 > 8 → 红黑树                 │
└───────────────────────────────────────────────┘
```

### 2.2 核心参数

| 参数 | 值 | 说明 |
|------|-----|------|
| DEFAULT_INITIAL_CAPACITY | 1<<4 = 16 | 默认初始容量（必须是2的幂） |
| DEFAULT_LOAD_FACTOR | 0.75f | 负载因子（时空折中） |
| TREEIFY_THRESHOLD | 8 | 链表转红黑树阈值 |
| UNTREEIFY_THRESHOLD | 6 | 红黑树转链表阈值（扩容时） |
| MIN_TREEIFY_CAPACITY | 64 | 树化最小数组容量 |

### 2.3 核心字段

```java
// HashMap 核心字段
public class HashMap<K,V> extends AbstractMap<K,V> {
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4;  // 16
    static final float DEFAULT_LOAD_FACTOR = 0.75f;
    transient Node<K,V>[] table;   // 桶数组（第一次 put 时延迟初始化）
    transient int size;            // 元素数量
    transient int modCount;        // 结构性修改计数（fail-fast）
    int threshold;                 // 扩容阈值 = capacity * loadFactor
    final float loadFactor;
}
// Node（链表节点）：hash + key + value + next
// TreeNode（红黑树节点）：继承 Node，多 parent/left/right/prev/red
```

> 💡 **为什么容量必须是 2 的幂？** `(n-1)&hash` 等价于 `hash%n` 但更快。且 2^n-1 全为 1 能充分利用 hash 所有位，扩容时元素不用重算 hash。

---

## 3. HashMap的put流程详解

### 3.1 put() 源码

```java
final V putVal(int hash, K key, V value, boolean onlyIfAbsent, boolean evict) {
    Node<K,V>[] tab; Node<K,V> p; int n, i;
    // ⭐ 步骤1：首次 put 时延迟初始化
    if ((tab = table) == null || (n = tab.length) == 0)
        n = (tab = resize()).length;
    // ⭐ 步骤2：计算下标 (n-1)&hash，空桶直接放入
    if ((p = tab[i = (n - 1) & hash]) == null)
        tab[i] = newNode(hash, key, value, null);
    else {
        Node<K,V> e; K k;
        // 步骤3：桶不为空 → 处理冲突
        if (p.hash == hash && ((k = p.key) == key || (key != null && key.equals(k))))
            e = p;                                         // 3a：首节点匹配
        else if (p instanceof TreeNode)
            e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value); // 3b：红黑树
        else {
            for (int binCount = 0; ; ++binCount) {         // 3c：链表遍历
                if ((e = p.next) == null) {
                    p.next = newNode(hash, key, value, null);
                    if (binCount >= TREEIFY_THRESHOLD - 1) treeifyBin(tab, hash);
                    break;
                }
                if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k))))
                    break;
                p = e;
            }
        }
        if (e != null) {                                   // 步骤4：替换旧值
            V oldValue = e.value;
            if (!onlyIfAbsent || oldValue == null) e.value = value;
            afterNodeAccess(e);
            return oldValue;
        }
    }
    ++modCount;
    if (++size > threshold) resize();                      // ⭐ 步骤5：扩容检查
    afterNodeInsertion(evict);
    return null;
}
```

树化条件：链表长度>=8 **且** 数组长度>=64。数组<64时优先扩容（链表自动拆分）。树化用泊松分布算出的阈值8保证最坏O(log n)。

### 3.2 get() 流程

```java
public V get(Object key) {
    return (e = getNode(hash(key), key)) == null ? null : e.value;
}
final Node<K,V> getNode(int hash, Object key) {
    Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (first = tab[(n - 1) & hash]) != null) {
        if (first.hash == hash && ((k = first.key) == key || (key != null && key.equals(k))))
            return first;
        if ((e = first.next) != null) {
            if (first instanceof TreeNode) return ((TreeNode<K,V>)first).getTreeNode(hash, key);
            do {
                if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k))))
                    return e;
            } while ((e = e.next) != null);
        }
    }
    return null;
}
```

> 🎯 **get 只需要看两个条件**：hash 相等 + key 相等（== 或 equals）。先比 hash 快速过滤，再比 key 精确匹配。

---

## 4. HashMap扩容机制（resize）

### 4.1 扩容触发

当 `size > threshold` 时触发（threshold = capacity x loadFactor）。默认：16 x 0.75 = 12，第 13 个元素 put 时扩容为 32。

### 4.2 resize() 源码

```java
/** ⭐ 扩容机制 (JDK 8) — 约 1/3 的面试题围绕此方法 */
final Node<K,V>[] resize() {
    Node<K,V>[] oldTab = table;
    int oldCap = (oldTab == null) ? 0 : oldTab.length;
    int oldThr = threshold;
    int newCap, newThr = 0;

    if (oldCap > 0) {
        if (oldCap >= MAXIMUM_CAPACITY) { threshold = Integer.MAX_VALUE; return oldTab; }
        else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY && oldCap >= DEFAULT_INITIAL_CAPACITY)
            newThr = oldThr << 1;          // 容量 ×2，阈值翻倍
    }
    else if (oldThr > 0) newCap = oldThr;  // 指定初始容量
    else { newCap = 16; newThr = 12; }     // 默认初始化

    Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
    table = newTab;

    if (oldTab != null) {                  // ⭐ 数据迁移（无需重算 hash）
        for (int j = 0; j < oldCap; ++j) {
            Node<K,V> e;
            if ((e = oldTab[j]) != null) {
                oldTab[j] = null;
                if (e.next == null) newTab[e.hash & (newCap - 1)] = e;
                else if (e instanceof TreeNode)
                    ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                else {
                    Node<K,V> loHead = null, loTail = null, hiHead = null, hiTail = null;
                    do {
                        if ((e.hash & oldCap) == 0) {    // ⭐ 关键：原位
                            if (loTail == null) loHead = e; else loTail.next = e;
                            loTail = e;
                        } else {                          // 原位+oldCap
                            if (hiTail == null) hiHead = e; else hiTail.next = e;
                            hiTail = e;
                        }
                        e = e.next;
                    } while (e != null);
                    if (loTail != null) { loTail.next = null; newTab[j] = loHead; }
                    if (hiTail != null) { hiTail.next = null; newTab[j + oldCap] = hiHead; }
                }
            }
        }
    }
    return newTab;
}
```

容量翻倍后新下标只有两种可能：**原位置** 或 **原位置+oldCap**。判断方法：`hash & oldCap`，结果为 0 在原位，为 oldCap 则原位+oldCap。既避免重算又保持链表顺序。

---

## 5. hash()算法与index计算

### 5.1 扰动函数

```java
/** ⭐ 扰动函数：让高位也参与低位的下标计算 */
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
```
为什么要这样做？下标计算 `(n-1)&hash` 只用到低位，如果两个 key 的高16位不同但低16位相同容易碰撞。将高16位异或到低16位让高位也参与计算。
```
hashCode: 0x12345678  →  0001 0010 0011 0100 0101 0110 0111 1000
h>>>16:   0x00001234  →  0000 0000 0000 0000 0001 0010 0011 0100
XOR:      0x1234464C  ← 高位信息混合到了低位
```

| JDK 版本 | 扰动次数 | 逻辑 |
|----------|---------|------|
| JDK 7 | 9次 | 4次位运算+5次异或 |
| JDK 8 | 2次 | 1次异或（高16位^低16位） |

### 5.2 容量为什么是2的幂

```java
index = (n - 1) & hash   // 等价于 hash % n，但快得多
```

三个原因：
1. **位运算替代取模**：性能高一个数量级
2. **均匀散列**：2^n-1 二进制全为 1，充分利用 hash 所有位
3. **扩容高效**：元素新索引=原索引或原索引+oldCap，无需重算

> ⚠️ **传入非 2 的幂？** `tableSizeFor()` 找到大于指定容量的最小 2 的幂。传入 19 → 实际 32。

---

## 6. JDK 7 vs JDK 8的HashMap差异

### 6.1 核心差异

| 特性 | JDK 7 | JDK 8 |
|------|-------|-------|
| 数据结构 | 数组+链表 | 数组+链表+红黑树 |
| hash 扰动 | 9次 | 2次 |
| 插入方式 | ⚠️ 头插法 | ✅ 尾插法 |
| 扩容死锁 | ⚠️ 有 | ✅ 无 |
| 扩容重hash | 需重新计算 | hash & oldCap 判断 |
| 树化 | 无 | 链表>=8且容量>=64 |
| 初始化 | 构造时创建 | 第一次 put 时延迟 |

### 6.2 头插法死循环问题

```
JDK 7 多线程并发扩容形成环形链表：
  扩容前 table[2] → A → B → null
  线程1 头插迁移后 newTab[2] → B → A → null（顺序反转）
  线程2 同时迁移看到部分完成 → 可能形成 A.next=B, B.next=A
  之后 get() 遍历此桶 → 死循环 → CPU 100%
```

JDK 8 **尾插法**修复：扩容时按原有顺序迁移，用 lo/hi 两条链表分别存储原位和高位元素，不反转链表顺序。

> ⚠️ **但 JDK 8 HashMap 并发下仍有数据丢失！** 多线程必须使用 ConcurrentHashMap。

---

## 7. LinkedHashMap：LRU缓存实现

### 7.1 数据结构与顺序模式

```
LinkedHashMap = HashMap + 双向链表
head ◄─► Node ◄─► Node ◄─► tail
(最老)                    (最新/最近访问)
```

两种顺序模式：
- **插入顺序**（默认）：迭代顺序=插入顺序
- **访问顺序**（accessOrder=true）：每次 get/put 将节点移到尾部，适合 LRU

```java
class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int maxCapacity;
    public LRUCache(int maxCapacity) {
        super(16, 0.75f, true);          // accessOrder = true
        this.maxCapacity = maxCapacity;
    }
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxCapacity;     // 超过容量淘汰最久未访问的
    }
}

LRUCache<String, Integer> cache = new LRUCache<>(3);
cache.put("a", 1); cache.put("b", 2); cache.put("c", 3);
cache.get("a");                     // a 移到尾部
cache.put("d", 4);                  // 触发淘汰 b
System.out.println(cache.keySet()); // [c, a, d]
```

> 🎯 **LinkedHashMap LRU 要点**：accessOrder=true + 重写 removeEldestEntry。这是 Java 中最优雅的 LRU 实现方式。

---

## 8. TreeMap：红黑树实现排序

### 8.1 数据结构

TreeMap 基于**红黑树**（自平衡二叉搜索树），实现 `NavigableMap`，操作 O(log n)。五大性质：节点红或黑、根黑、叶黑、无连续红色、任一节点到叶路径黑节点数相同。

### 8.2 核心特性

```java
// ===== 自然顺序 =====
TreeMap<Integer, String> map = new TreeMap<>();
map.put(3, "three"); map.put(1, "one"); map.put(2, "two");
System.out.println(map); // {1=one, 2=two, 3=three} 自动排序

// ===== NavigableMap 导航方法 =====
map.firstKey();          // 1
map.lastKey();           // 3
map.lowerKey(2);         // 1  （小于2的最大key）
map.higherKey(2);        // 3  （大于2的最小key）
map.ceilingKey(2);       // 2  （>=2的最小key）
map.floorKey(2);         // 2  （<=2的最大key）
map.subMap(1, true, 3, true);  // {1,2,3} 子图

// ===== 自定义排序 =====
TreeMap<String, Integer> rev = new TreeMap<>(Comparator.reverseOrder());
rev.put("a", 1); rev.put("c", 3); rev.put("b", 2);
System.out.println(rev); // {c=3, b=2, a=1}
```

### 8.3 TreeMap vs HashMap

| 特性 | TreeMap | HashMap |
|------|---------|---------|
| 底层结构 | 红黑树 | 数组+链表+红黑树 |
| 时间复杂度 | O(log n) | O(1)/O(log n) |
| 有序性 | key 按比较器排序 | 无序 |
| null key | 不允许 | 允许一个 |
| 额外接口 | NavigableMap/SortedMap | 无 |
| 适用场景 | 需要排序/范围查询 | 通用键值对 |

---

## 9. Hashtable与ConcurrentHashMap演进

### 9.1 演进路线

```
JDK 1.0             JDK 1.5               JDK 8
Hashtable    →    ConcurrentHashMap  →  ConcurrentHashMap
(全表锁)         (分段锁 16个)         (CAS+synchronized)
```

### 9.2 Hashtable（已过时）

全表锁（synchronized），不允许 null key/value，初始容量 11，扩容 2n+1，只有数组+链表。多线程时只能串行，性能极差，已不被推荐。

### 9.3 JDK 7 CHM：分段锁

```
Segment[]（继承 ReentrantLock，默认 16 个）
  每个 Segment = 一把锁 + 一个小 HashMap
  写操作只锁对应 Segment，不同 Segment 可并发
  最大并发度 = 16（分段数固定），扩容时整个 Segment 内 rehash
```

### 9.4 演进对比

| 特性 | Hashtable | CHM JDK 7 | CHM JDK 8 |
|------|-----------|-----------|-----------|
| 锁粒度 | 全表锁 | 分段锁 | 桶级锁（链表/树头节点） |
| 锁实现 | synchronized | ReentrantLock | CAS+synchronized |
| 并发度 | 1 | 默认 16 | 数组长度 |
| 数据结构 | 数组+链表 | 数组+链表 | 数组+链表+红黑树 |
| null key/value | 不允许 | 不允许 | 不允许 |

---

## 10. ConcurrentHashMap源码分析（JDK 8）

### 10.1 put 流程与 sizeCtl

```java
/** CHM JDK 8 put — 三阶段锁策略：CAS + synchronized + 协助扩容 */
final V putVal(K key, V value, boolean onlyIfAbsent) {
    if (key == null || value == null) throw new NullPointerException();
    int hash = spread(key.hashCode());
    int binCount = 0;
    for (Node<K,V>[] tab = table;;) {   // 自旋（失败重试）
        Node<K,V> f; int n, i, fh;
        if (tab == null || (n = tab.length) == 0)
            tab = initTable();                              // 1. 延迟初始化
        else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
            if (casTabAt(tab, i, null, new Node<>(hash, key, value)))
                break;                                      // ⭐ 2. 空桶→CAS无锁
        }
        else if ((fh = f.hash) == MOVED)
            tab = helpTransfer(tab, f);                     // 3. 扩容中→帮忙迁移
        else {
            V oldVal = null;
            synchronized (f) {                              // ⭐ 4. 桶非空→锁头节点
                if (tabAt(tab, i) == f) {
                    if (fh >= 0) { /* 链表操作 */ }
                    else if (f instanceof TreeBin) { /* 红黑树操作 */ }
                }
            }
            if (binCount >= TREEIFY_THRESHOLD) treeifyBin(tab, i);
        }
    }
    addCount(1L, binCount);  // LongAdder 机制计数
    return null;
}

/** sizeCtl — 多用途控制字段 */
private transient volatile int sizeCtl;
// -1: 正在初始化   -N: N-1个线程在扩容   >0: 扩容阈值或新容量
```

### 10.2 initTable() 与多线程扩容

```java
/** 初始化表 — CAS 竞争初始化权 */
private final Node<K,V>[] initTable() {
    Node<K,V>[] tab; int sc;
    while ((tab = table) == null || tab.length == 0) {
        if ((sc = sizeCtl) < 0) Thread.yield();  // 其他线程正初始化
        else if (U.compareAndSetInt(this, SIZECTL, sc, -1)) {
            try {
                if ((tab = table) == null || tab.length == 0) {
                    int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
                    table = tab = (Node<K,V>[])new Node<?,?>[n];
                    sc = n - (n >>> 2);        // 0.75n
                }
            } finally { sizeCtl = sc; }
            break;
        }
    }
    return tab;
}
// 多线程扩容：其他线程 put 发现 ForwardingNode(MOVED) → helpTransfer()
```

> 🎯 **CHM JDK 8 精髓**：空桶 CAS 无锁（最频繁路径），非空桶 synchronized 锁头节点（细粒度），扩容多线程协作。并发度从固定 16 提升到数组长度级别。

---

## 11. Map选型指南

### 11.1 决策树

```
并发安全？
  ├── 是 → ConcurrentHashMap
  └── 否 → 需要排序？
              ├── 是 → TreeMap
              └── 否 → 需要保持顺序？
                          ├── 是 → LinkedHashMap
                          └── 否 → HashMap（默认首选）
```

| 场景 | 推荐 | 原因 |
|------|------|------|
| 通用键值对 | HashMap | O(1) 综合性能最优 |
| 保持插入顺序 | LinkedHashMap | 双向链表维护顺序 |
| LRU 缓存 | LinkedHashMap(accessOrder=true) | 访问顺序+removeEldestEntry |
| key 排序 | TreeMap | 红黑树自动排序 |
| 范围查询 | TreeMap | NavigableMap subMap 等 |
| 高并发 | ConcurrentHashMap | CAS+桶级锁 |
| 配置文件 | Properties | 专为 .properties 设计 |

### 11.2 最佳实践

```java
// ✅ 指定初始容量：expectedSize/0.75+1 避免频繁扩容
Map<String, Object> map = new HashMap<>(expectedSize / 3 * 4 + 1);
// ✅ 用接口声明（面向抽象编程）
Map<String, Integer> map = new HashMap<>();
// ✅ 遍历用 entrySet（避免 keySet+get 的二次 hash）
for (Map.Entry<String, Integer> e : map.entrySet())
    System.out.println(e.getKey() + "=" + e.getValue());
// ✅ JDK 8 遍历方式
map.forEach((k, v) -> System.out.println(k + "=" + v));
// ✅ computeIfAbsent（原子操作，替代"先检查再put"的竞态模式）
map.computeIfAbsent("key", k -> expensiveCompute(k));
// ✅ merge（存在则合并，不存在则插入）
map.merge("score", 1, Integer::sum);
// ❌ 多线程不要用 HashMap → 用 ConcurrentHashMap
```

---

## 12. 面试高频考点

**Q1: HashMap put 流程？** 计算hash→下标→空桶直接放→非空桶遍历链表/树→找到替换/未找到尾插→检查树化→检查扩容。

**Q2: hash 函数为什么 `(h ^ (h >>> 16))`？** 让高位参与低位下标计算，减少碰撞。因 `(n-1)&hash` 只用到低位。

**Q3: 为什么容量是 2 的幂？** 位运算替代取模(快)、均匀散列、扩容无需重算hash。

**Q4: 树化阈值为什么是 8？** 泊松分布：0.75负载因子下链表到8的概率<千万分之一，时空平衡点。

**Q5: 负载因子 0.75？** 时空权衡的较优值。太大冲突多查询慢，太小空间浪费大。

**Q6: JDK 7 头插法为什么死循环？** 多线程扩容时头插法反转链表顺序，可能形成环形链表导致 get() 死循环 CPU 100%。

**Q7: JDK 8 如何修复？** 尾插法保持原有顺序，不反转链表。但并发下仍有数据丢失，仍需 ConcurrentHashMap。

**Q8: HashMap vs ConcurrentHashMap？** HashMap 线程不安全；CHM 线程安全。JDK 8 CHM：CAS(空桶)+synchronized(桶锁)，并发度=数组长度。

**Q9: CHM JDK 7 vs 8？** JDK 7：Segment分段锁，并发度固定16，数组+链表。JDK 8：CAS+synchronized，并发度=桶数，数组+链表+红黑树，多线程协作扩容。

**Q10: LinkedHashMap 实现 LRU？** accessOrder=true 开启访问顺序，重写 removeEldestEntry，size>maxCapacity时淘汰最老节点。

**Q11: TreeMap 底层？特有功能？** 红黑树，O(log n)。特有：key自动排序、NavigableMap范围查询。

**Q12: TreeMap key 不能为 null？** 需调用 compareTo() 排序，null 抛 NPE。自定义 Comparator 支持 null 则可允许。

---

> 🎯 **Map 体系总结**：HashMap 是核心（掌握 put/resize/hash 三大流程），ConcurrentHashMap 是关键难点（理解 CAS+synchronized 锁演进），LinkedHashMap 和 TreeMap 是重要扩展（有序性的两种实现方式）。理解这些 Map 的底层原理和演进思路，是 Java 面试和实战的必经之路。

---

*最后更新: 2026-07-26 | 适用于 JDK 8/11/17/21*
