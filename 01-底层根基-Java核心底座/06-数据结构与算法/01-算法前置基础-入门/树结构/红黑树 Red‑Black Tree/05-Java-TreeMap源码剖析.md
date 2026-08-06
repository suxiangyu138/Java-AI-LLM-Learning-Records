# 05 - Java TreeMap 源码剖析

> 红黑树在 Java 的标准实现：TreeMap/TreeSet 底层、节点结构与旋转、HashMap 的树化门槛（链表 ≥ 8 转红黑树）——工程视角验证理论。

## 📚 目录

1. [TreeMap 与红黑树](#1-treemap-与红黑树)
2. [节点结构与基本操作](#2-节点结构与基本操作)
3. [HashMap 树化：红黑树的另一舞台](#3-hashmap-树化红黑树的另一舞台)
4. [TreeMap vs HashMap 选型](#4-treemap-vs-hashmap-选型)
5. [源码考点速查表](#5-源码考点速查表)

## 1. TreeMap 与红黑树

**TreeMap = 红黑树实现的有序 Map**（JDK 1.2 至今未变）。

| 特性 | TreeMap | 依据 |
|------|---------|------|
| 底层 | 红黑树（自平衡 BST） | 节点带 color 字段 |
| 有序性 | 按键升序（可定制比较器） | BST 中序 |
| 复杂度 | put/get/remove 均 **O(log n)** | 红黑树性质 |
| 不允许 | null 键（自然序时） | compareTo 需非空 |
| 线程安全 | 否（Collections.synchronizedMap 包装） | — |

```java
// 有序 API 一览（红黑树有序性的直接应用）
TreeMap<Integer, String> map = new TreeMap<>();
map.put(5, "five"); map.put(2, "two"); map.put(8, "eight"); map.put(3, "three");

map.firstKey();          // 2（最左节点）
map.lastKey();           // 8（最右节点）
map.floorKey(4);         // 3（≤ 4 的最大键）
map.ceilingKey(4);       // 5（≥ 4 的最小键）
map.lowerKey(5);         // 3
map.higherKey(5);        // 8
map.subMap(2, 8);        // {2,3,5}
```

## 2. 节点结构与基本操作

### 2.1 节点定义（JDK 源码）

```java
static final class Entry<K,V> {
    K key;
    V value;
    Entry<K,V> left;
    Entry<K,V> right;
    Entry<K,V> parent;         // 红黑树节点有 parent 指针（BST 没有）
    boolean color = BLACK;     // 颜色标记

    Entry(K key, V value, Entry<K,V> parent) {
        this.key = key;
        this.value = value;
        this.parent = parent;
    }
}
```

### 2.2 put（插入）流程

```java
// TreeMap.put 核心逻辑（概念还原）
public V put(K key, V value) {
    Entry<K,V> t = root;
    if (t == null) { ... root = new Entry<>(key, value, null); ... }
    Comparator<? super K> cpr = comparator;
    // ① BST 插入：按比较器找位置
    while (t != null) {
        int cmp = cpr != null ? cpr.compare(key, t.key)
                              : key.compareTo(t.key);
        if (cmp < 0) { parent = t; t = t.left; }
        else if (cmp > 0) { parent = t; t = t.right; }
        else { t.value = value; return oldValue; }   // 键已存在：覆盖值
    }
    // ② 创建红节点挂接
    Entry<K,V> e = new Entry<>(key, value, parent);
    if (cmp < 0) parent.left = e; else parent.right = e;
    // ③ 修复红黑树性质（03 模块的 fixAfterInsertion）
    fixAfterInsertion(e);
    size++; modCount++;
}
```

### 2.3 旋转（JDK 风格）

```java
private void rotateLeft(Entry<K,V> p) {
    if (p != null) {
        Entry<K,V> r = p.right;
        p.right = r.left;                 // ① r 的左子树过继
        if (r.left != null) r.left.parent = p;
        r.parent = p.parent;              // ② r 上提
        if (p.parent == null) root = r;
        else if (p.parent.left == p) p.parent.left = r;
        else p.parent.right = r;
        r.left = p;                       // ③ p 变为 r 的左子
        p.parent = r;
    }
}
```

> 💡 与 03 模块的手写旋转对比：源码多了 parent 指针维护——TreeMap 用三向链表（left/right/parent），删除修复（04 模块）需要向上回溯。

## 3. HashMap 树化：红黑树的另一舞台

**HashMap 的链表 → 红黑树**（JDK 8+）：当哈希冲突严重时，链表退化为 O(n) 查找——红黑树把最坏降到 O(log n)。

| 阈值 | 值 | 含义 |
|------|:---:|------|
| 树化阈值 | 8 | 链表长度 ≥ 8 时转红黑树 |
| 树化容量门槛 | 64 | 容量 < 64 时优先扩容而非树化 |
| 退化阈值 | 6 | 红黑树节点 ≤ 6 时转回链表 |

```java
// HashMap.putVal 中的树化判断（概念还原）
if (binCount >= TREEIFY_THRESHOLD - 1) {   // binCount >= 7 → 长度 ≥ 8
    treeifyBin(tab, hash);                 // 转红黑树（还需容量 ≥ 64）
}
```

**为什么阈值是 8**（面试高频追问）：

```text
泊松分布计算：容量 16、负载因子 0.75 时
链表长度 ≥ 8 的概率 ≈ 0.00000006（千万分之六）
→ 8 是「正常哈希冲突几乎不可能达到」的阈值
→ 达到 8 说明哈希函数严重失效（恶意攻击/设计缺陷），
   此时树化兜底防最坏 O(n)
```

> ⚠️ **面试必答**：HashMap 树化不是常态优化，而是**恶意哈希（HashDoS）防御**——正常数据下链表 ≥ 8 的概率是千万分之六。详见[哈希体系](../../哈希（散列）结构/00-哈希（散列）结构知识体系总览.md)与[Java 有关 Hash 的一切](../../../../01-Java基础语法与核心特性/Java有关Hash的一切/00-Hash总览.md)。

## 4. TreeMap vs HashMap 选型

| 维度 | TreeMap（红黑树） | HashMap（数组+链表/红黑树） |
|------|-------------------|---------------------------|
| 顺序 | **有序**（键排序） | 无序 |
| 查询 | O(log n) | **O(1) 平均** |
| 插入/删除 | O(log n) | O(1) 平均 |
| 有序 API | floor/ceiling/subMap | 无 |
| 适用 | 需要有序遍历/区间/极值 | 快速查增删（默认选择） |

**选型口诀**：**要有序选 TreeMap，要速度选 HashMap**——90% 场景 HashMap，只有「取极值/范围/顺序遍历」才换 TreeMap（[有序集合](../../../04-中高级算法专题/专题 4：高级数据结构/高级数据结构/有序集合/)体系衔接）。

## 5. 源码考点速查表

| 考点 | 答案 |
|------|------|
| TreeMap 底层？ | 红黑树（Entry 带 parent 和 color） |
| put 流程？ | BST 插入（比较器定位）→ 红节点挂接 → fixAfterInsertion |
| 为什么节点有 parent 指针？ | 删除修复需要向上回溯 |
| HashMap 为什么树化？ | 恶意哈希防御（链表 ≥ 8 概率千万分之六） |
| 树化阈值？ | 链表 ≥ 8 且容量 ≥ 64；退化 6 |
| 红黑树 vs 链表查找？ | log n vs n（最坏） |
| TreeMap 允许 null 键？ | 自然序不允许（compareTo NPE） |
| 旋转在源码里出现在哪？ | fixAfterInsertion / fixAfterDeletion |

> 🎯 **核心要点**：源码验收——能讲 TreeMap.put 三步（BST 定位 → 红节点 → 修复）；HashMap 树化阈值的**泊松分布论证**（千万分之六）是面试杀手锏；「要有序选 TreeMap、要速度选 HashMap」的选型一句话；红黑树 + parent 指针的结构设计（与 AVL 的对比见 06 模块）。

---

**上一模块**：[04-删除与修复](04-删除与修复.md) ｜ **下一模块**：[06-红黑树 vs AVL vs B 树](06-红黑树vsAVLvsB树.md) ｜ **返回总览**：[00-红黑树知识体系总览](00-红黑树知识体系总览.md)

**【参考来源】**
- OpenJDK TreeMap 源码：https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/java/util/TreeMap.java
- OpenJDK HashMap 源码（TREEIFY_THRESHOLD）
