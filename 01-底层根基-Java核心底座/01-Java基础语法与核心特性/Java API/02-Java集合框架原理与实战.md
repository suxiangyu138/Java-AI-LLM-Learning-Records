# 02 Java 集合框架原理与实战

> 集合是日常开发的数据容器——ArrayList、HashMap、HashSet 每天在用，但它们的扩容策略、哈希扰动、红黑树化门槛和 fail-fast 行为才是面试与排错的分水岭

---

## 📚 目录

1. [集合框架全景](#1-集合框架全景)
2. [ArrayList vs LinkedList 原理与选型](#2-arraylist-vs-linkedlist-原理与选型)
3. [HashMap 深度解析](#3-hashmap-深度解析)
4. [HashSet、TreeSet 与 LinkedHashSet](#4-hashsettreeset-与-linkedhashset)
5. [迭代器、fail-fast 与 fail-safe](#5-迭代器fail-fast-与-fail-safe)
6. [Comparable vs Comparator 排序体系](#6-comparable-vs-comparator-排序体系)
7. [Collections 与 Arrays 工具类](#7-collections-与-arrays-工具类)
8. [选型速查与练习](#8-选型速查与练习)

---

## 1. 集合框架全景

### 1.1 集合框架的两大接口树

Java 集合框架（Java Collections Framework，JCF）以两个顶级接口为根，向下分化出所有常用容器：

```text
┌──────────────────────────────────────────────────────────┐
│                    Iterable (java.lang)                    │
│  iterator() / forEach() / spliterator()                    │
└─────────────────────────┬────────────────────────────────┘
                          │ extends
                          ▼
┌──────────────────────────────────────────────────────────┐
│                    Collection<E>                           │
│  add / remove / size / isEmpty / contains / iterator      │
│  stream / parallelStream / removeIf / spliterator         │
└──────────┬──────────────────────────┬───────────────────┘
           │                          │
           ▼                          ▼
    ┌──────────────┐       ┌───────────────────┐
    │    List      │       │      Set          │
    │ 有序 可重复   │       │  无序 不可重复     │
    │ ArrayList    │       │ HashSet           │
    │ LinkedList   │       │ TreeSet (Sorted)  │
    │ Vector       │       │ LinkedHashSet     │
    └──────────────┘       └───────────────────┘
                                      │
                                      ▼
                               ┌──────────────┐
                               │    Queue     │
                               │  队列 (FIFO)  │
                               │ LinkedList   │
                               │ PriorityQueue│
                               │ ArrayDeque   │
                               └──────────────┘
```

Map 是一个独立体系，不继承 Collection，但与集合框架深度融合：

```text
                     Map<K,V>
     put / get / remove / containsKey / containsValue
     keySet() / values() / entrySet()
          │
     ┌────┴──────────────────────┐
     │                           │
     ▼                           ▼
 ┌──────────┐           ┌──────────────┐
 │ HashMap  │           │  SortedMap   │
 │ 哈希实现   │           │   排序键      │
 │ LinkedHashMap │       │      │       │
 │ (双向链表) │           │      ▼       │
 └──────────┘           │  TreeMap     │
                        │  红黑树实现    │
                        └──────────────┘

 并发分支（java.util.concurrent）：
   ConcurrentHashMap / CopyOnWriteArrayList / ConcurrentLinkedQueue
```

### 1.2 Iterable 与 Iterator 接口

**Iterable** 是集合框架最顶层的接口，定义了"可被迭代"的能力：

| 方法 | 作用 | 默认实现 |
|------|------|---------|
| `iterator()` | 返回迭代器 | 抽象方法，子类必须实现 |
| `forEach(Consumer)` | 内部迭代 | JDK 8 默认方法，等价于增强 for 循环 |
| `spliterator()` | 分片迭代器 | JDK 8，支持并行流的分治遍历 |

**Iterator** 是迭代器接口，提供逐个访问元素的能力：

| 方法 | 作用 | 说明 |
|------|------|------|
| `hasNext()` | 是否还有下一个元素 | 调用 `next()` 前的守卫 |
| `next()` | 返回当前元素并移动指针 | 没有元素时抛 `NoSuchElementException` |
| `remove()` | 删除当前元素 | 必须在 `next()` 之后调用 |
| `forEachRemaining(Consumer)` | 对剩余元素批量消费 | JDK 8 默认方法 |

> 💡 **设计启示**：Iterable + Iterator 分离是策略模式的经典应用——Iterable 定义"有迭代能力"，Iterator 定义"如何迭代"，两者解耦使得同一个集合可以返回不同的迭代策略。

```java
// 经典迭代写法
List<String> list = Arrays.asList("A", "B", "C");
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    String s = it.next();
    if ("B".equals(s)) {
        it.remove();        // 安全删除
    }
}

// JDK 8 forEach + Lambda
list.forEach(System.out::println);

// 增强 for 循环（语法糖，编译后还是 iterator）
for (String s : list) {
    System.out.println(s);
}
```

### 1.3 集合框架的核心设计原则

| 原则 | 说明 | 体现 |
|------|------|------|
| 接口与实现分离 | 面向接口编程，具体实现可互换 | `List list = new ArrayList<>()` |
| 算法多态 | Collections 工具类提供通用算法 | `Collections.sort()`, `Collections.binarySearch()` |
| 装饰器扩展 | 包装类在不改接口的前提下添加功能 | `Collections.synchronizedList()`, `Collections.unmodifiableList()` |
| 一致性命名 | 所有集合的增删查方法签名一致 | `add()`, `remove()`, `contains()`, `size()` |

---

## 2. ArrayList vs LinkedList 原理与选型

### 2.1 底层数据结构

| 特性 | ArrayList | LinkedList |
|------|-----------|------------|
| 底层结构 | `Object[]` 动态数组 | 双向链表（Node 节点） |
| 随机访问 | O(1) — 直接下标计算地址 | O(n) — 需要遍历到位置 |
| 尾部插入 | O(1) 均摊 | O(1) — 有 last 指针 |
| 中间插入 | O(n) — 元素后移 | O(n) — 需先遍历到位置 + 改指针 |
| 头部插入 | O(n) | O(1) — 有 first 指针 |
| 删除尾部 | O(1) | O(1) |
| 删除中间 | O(n) | O(n) |
| 内存开销 | 更小（只有数组元素） | 更大（每个 Node 3 个引用：prev, item, next） |
| 内存连续性 | 连续内存 | 离散节点（缓存不友好） |

```java
// LinkedList 内部节点结构（JDK 源码简化）
private static class Node<E> {
    E item;           // 实际元素
    Node<E> next;     // 后向指针
    Node<E> prev;     // 前向指针

    Node(Node<E> prev, E element, Node<E> next) {
        this.item = element;
        this.next = next;
        this.prev = prev;
    }
}
```

### 2.2 核心误区：LinkedList 中间插入真的快吗？

很多开发者认为"LinkedList 插入快"——**这个结论仅在操作头尾时成立**。

```java
// 错误认知：一定要用 LinkedList 在中间插入
LinkedList<String> list = new LinkedList<>();
list.add("A"); list.add("B"); list.add("C");
// ↓ 要在 index=1 处插入 "X" —— LinkedList 需要先遍历到位置！
list.add(1, "X");
// 后台：list.listIterator(1) 从 first 开始遍历到 index=1 → O(n)
// 然后才是 O(1) 的指针修改

// 真实对比：ArrayList 插入中间是 O(n) 的元素后移（System.arraycopy）
// LinkedList 插入中间是先 O(n) 遍历 + O(1) 改指针
// 两者在中间插入的场景复杂度上界相同，但 ArrayList 的 cache locality 更好
```

> 🎯 **核心结论**：
> - **频繁随机访问 + 尾部追加** → 选 ArrayList（90% 的场景）
> - **频繁头尾增删**（队列/栈）→ 选 LinkedList 或 ArrayDeque
> - **频繁中间插入** → 两者都不理想，考虑其他数据结构

### 2.3 ArrayList 扩容机制深度分析

```java
// JDK 8 ArrayList 无参构造
private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};

public ArrayList() {
    this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA; // 空数组！
}
```

**关键设计决策：延迟初始化**

- JDK 8 起，无参构造不再初始化容量为 10 的数组，而是一个空数组
- 首次调用 `add()` 时，才通过 `ensureCapacityInternal()` 扩容到 `DEFAULT_CAPACITY = 10`
- 这是一种**延迟加载优化**：避免创建空 ArrayList 就浪费 10 个对象引用

**扩容过程：**

```text
首次 add(): elementData = new Object[10]      (容量从 0 → 10)

第 11 个元素 add(): elementData = new Object[15]   (10 → 15, 1.5 倍)
  新容量 = 旧容量 + (旧容量 >> 1)
         = 10 + 5
         = 15

第 16 个元素 add(): elementData = new Object[22]   (15 → 22, 1.5 倍)
  15 + (15 >> 1) = 15 + 7 = 22

第 23 个元素 add(): elementData = new Object[33]   (22 → 33)
  22 + (22 >> 1) = 22 + 11 = 33
```

```java
// JDK 8 ArrayList.grow() 核心代码
private Object[] grow(int minCapacity) {
    int oldCapacity = elementData.length;
    int newCapacity = oldCapacity + (oldCapacity >> 1);  // 1.5 倍
    if (newCapacity - minCapacity < 0)
        newCapacity = minCapacity;
    if (newCapacity - MAX_ARRAY_SIZE > 0)
        newCapacity = hugeCapacity(minCapacity);
    return elementData = Arrays.copyOf(elementData, newCapacity);
}
```

> 💡 **为什么是 1.5 倍而不是 2 倍或固定增量？**
>
> - 1.5 倍是**时间与空间的平衡**：如果翻倍，扩容次数更少但平均浪费更多内存（最坏浪费 50%）；1.5 倍则扩容更频繁一些但内存更紧凑
> - 如果翻倍，扩容后最坏情况浪费近 100% 的空间；1.5 倍最坏浪费约 33%（新容量一半未使用，新容量是旧容量的 1.5 倍，0.5/1.5 ≈ 33%）
> - 1.5 倍意味着**每次扩容后，前面元素占用的百分比上升**，因为新容量是旧容量的 1.5 倍，已有元素占了 2/3

**扩容开销：**

```java
// 扩容的核心是 Arrays.copyOf —— 底层是 System.arraycopy 本地方法
// 每次扩容需要拷贝旧数组所有元素到新数组
// 摊销分析：N 次插入的总拷贝次数约 O(N)，单次平均 O(1)
//
// 数学证明：
// 假设初始容量 10，从 0 到 N 次插入的总拷贝次数
// = 10 + 15 + 22 + 33 + ... ≤ N * (1.5 / (1.5 - 1)) = 3N
// 所以均摊到每次插入，拷贝量是 O(1)

// 最佳实践：预判容量
List<String> list = new ArrayList<>(1000);  // 已知 1000 个元素，避免扩容
```

### 2.4 subList 的结构修改陷阱

`subList(fromIndex, toIndex)` 返回的是**视图**而非独立副本，这是最容易踩的坑之一。

```java
List<String> list = new ArrayList<>(Arrays.asList("A", "B", "C", "D", "E"));
List<String> sub = list.subList(1, 4);  // sub = [B, C, D]

// 陷阱 1：修改 sub 会影响原 list
sub.set(0, "X");
System.out.println(list);  // [A, X, C, D, E] — 原 list 被改了！

// 陷阱 2：修改原 list 的结构（add/remove）后再操作 sub → ConcurrentModificationException
list.add("F");             // 修改原 list 结构
System.out.println(sub);   // ❌ ConcurrentModificationException
// subList 内部维护了 parent 的 modCount，结构修改使校验失败
```

```java
// 安全做法：如果需要独立子列表，复制一份
List<String> safeSub = new ArrayList<>(list.subList(1, 4));
// 现在操作 safeSub 与 list 完全独立
```

### 2.5 性能实测参考

```text
操作 1,000,000 个元素：

| 操作              | ArrayList  | LinkedList |
|-------------------|-----------|------------|
| 尾部追加          | ~15 ms    | ~20 ms     |
| 头部插入          | ~800 ms   | ~5 ms      |
| 随机访问 10w 次   | ~2 ms     | ~8,000 ms  |
| 遍历全部          | ~8 ms     | ~15 ms     |
| 中间插入 1 个     | ~0.1 ms   | ~0.3 ms    |

（实际数据因机器、JVM 配置而异，但量级差异趋势确定）
```

---

## 3. HashMap 深度解析

HashMap 是 Java 中最重要的数据结构之一，也是面试中出镜率最高的集合类。理解它需要逐个击破：**数据结构 → hash 函数 → put 流程 → 扩容 → 树化 → 并发行为**。

### 3.1 底层数据结构演进

```text
JDK 7：
┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
│  S  │  S  │  S  │  S  │  S  │  S  │  S  │  S  │  S  │  S  │  S  │  ← 数组 (Entry[])
│  E  │  E  │  E  │  E  │  E  │  E  │  E  │  E  │  E  │  E  │  E  │
└─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┘
  │           │           │
  ▼           ▼           ▼
Entry       Entry       Entry     ← 链表（头插法）
  │           │
  ▼           ▼
Entry       Entry

JDK 8+：
┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
│  S  │  S  │  S  │  S  │  S  │  S  │  S  │  S  │  S  │  S  │  S  │  S  │  ← 数组 (Node[])
│  N  │  N  │  N  │  N  │  N  │  N  │  N  │  N  │  N  │  N  │  N  │  N  │
└─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┘
  │           │           │
  ▼           ▼           ▼
Node        Node        TreeNode    ← 链表 → 红黑树（尾插法）
              │
              ▼
             Node
```

| 版本 | 存储结构 | 节点类 | 插入方式 | 树化 |
|------|---------|--------|---------|------|
| JDK 7 | 数组 + 链表 | `Entry<K,V>` | 头插法 | 无 |
| JDK 8+ | 数组 + 链表 + 红黑树 | `Node<K,V>` | 尾插法 | 链表长度 ≥ 8 |

### 3.2 hash 扰动函数

```java
// JDK 8 HashMap.hash() 源码
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
```

**为什么需要扰动？**

`hashCode()` 返回 32 位 int，但 HashMap 数组长度 n 通常较小（默认 16），当计算数组下标 `(n - 1) & hash` 时，只有低位参与运算，高位被"屏蔽"了：

```text
例：假设 hashCode = 0x12345678，数组容量 n = 16（二进制 10000）

n - 1 = 15 = 0000 0000 0000 0000 0000 0000 0000 1111

不扰动：直接 (n - 1) & hashCode
  0x12345678 & 0xFFFF = 0x0008  ← 只用了后 4 位！

扰动后：hash = hashCode ^ (hashCode >>> 16)
  0x12345678 ^ 0x00001234 = 0x1234444C
  (n - 1) & 0x1234444C = 0x000C  ← 高位特征参与到低位了
```

> 🎯 **扰动函数 = 将高位 16 位的特征"揉"进低位 16 位**，使得即使 `hashCode()` 区分度集中在高位，也能在 HashMap 的低位掩码运算中体现出来，**减少哈希碰撞**。

**JDK 7 的扰动更复杂（4 次位运算）：**

```java
// JDK 7
static int hash(int h) {
    h ^= (h >>> 20) ^ (h >>> 12);
    return h ^ (h >>> 7) ^ (h >>> 4);
}
// JDK 8 简化为 1 次 (h >>> 16) —— 足够好且更快
```

### 3.3 put 流程图解

```text
put(key, value)
    │
    ▼
┌─────────────────────────────────────────────┐
│ 1. 数组是否为空或长度 == 0？                │
│    → 是：resize() 初始化（默认 16）          │
└──────────────────┬──────────────────────────┘
                   ▼
┌─────────────────────────────────────────────┐
│ 2. 计算 hash = hash(key)                    │
│    i = (n - 1) & hash   ← 数组下标          │
└──────────────────┬──────────────────────────┘
                   ▼
┌─────────────────────────────────────────────┐
│ 3. table[i] == null？                       │
│    → 是：直接 new Node(table[i])            │ ✓ 无碰撞
│    → 否：进入碰撞处理                        │
└──────────────────┬──────────────────────────┘
                   ▼ (碰撞)
┌─────────────────────────────────────────────┐
│ 4. 检查 table[i] 是否与 key 相同            │
│    （先比较 hash，再比较 equals）            │
│    → 是：替换旧值，返回旧值                  │
└──────────────────┬──────────────────────────┘
                   ▼ (不相同)
┌─────────────────────────────────────────────┐
│ 5. table[i] 是 TreeNode（红黑树）？          │
│    → 是：putTreeVal() 插入红黑树             │
│    → 否：遍历链表                            │
│       ├─ 链表中找到 equals 的 key → 替换     │
│       └─ 未找到 → 尾插法插入新节点           │
│          └─ 插入后链表长度 ≥ TREEIFY_THRESHOLD(8) → treeifyBin() │
└──────────────────┬──────────────────────────┘
                   ▼
┌─────────────────────────────────────────────┐
│ 6. 插入后 size > threshold？                 │
│    → 是：resize() 扩容                      │
│    → 否：结束                               │
└─────────────────────────────────────────────┘
```

```java
// JDK 8 putVal 源码思路简化
final V putVal(int hash, K key, V value, boolean onlyIfAbsent, boolean evict) {
    Node<K,V>[] tab; Node<K,V> p; int n, i;
    if ((tab = table) == null || (n = tab.length) == 0)  // Step 1: 懒初始化
        n = (tab = resize()).length;
    if ((p = tab[i = (n - 1) & hash]) == null)            // Step 2-3: 空位直接插入
        tab[i] = newNode(hash, key, value, null);
    else {
        Node<K,V> e; K k;
        if (p.hash == hash &&                              // Step 4: 检查是否 key 相同
            ((k = p.key) == key || (key != null && key.equals(k))))
            e = p;
        else if (p instanceof TreeNode)                    // Step 5: 红黑树插入
            e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
        else {                                             // Step 5: 链表遍历
            for (int binCount = 0; ; ++binCount) {
                if ((e = p.next) == null) {
                    p.next = newNode(hash, key, value, null);  // 尾插法
                    if (binCount >= TREEIFY_THRESHOLD - 1)
                        treeifyBin(tab, hash);                 // ≥8 转红黑树
                    break;
                }
                if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k))))
                    break;
                p = e;
            }
        }
        if (e != null) {  // 找到已存在的 key → 替换 value
            V oldValue = e.value;
            if (!onlyIfAbsent || oldValue == null)
                e.value = value;
            return oldValue;
        }
    }
    ++modCount;
    if (++size > threshold)   // Step 6: 超过阈值则扩容
        resize();
    return null;
}
```

### 3.4 扩容机制：resize() 全解

**扩容核心逻辑：容量翻倍 + 哈希重定位（rehash）**

```java
// 简化版 resize() 核心
final Node<K,V>[] resize() {
    Node<K,V>[] oldTab = table;
    int oldCap = (oldTab == null) ? 0 : oldTab.length;
    int oldThr = threshold;
    int newCap, newThr = 0;

    if (oldCap > 0) {
        if (oldCap >= MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;   // 不能再扩容了
            return oldTab;
        }
        newCap = oldCap << 1;                // 容量翻倍！
        newThr = oldThr << 1;                // 阈值翻倍
    } else if (oldThr > 0) {
        newCap = oldThr;                     // 使用阈值作为初始容量
    } else {
        newCap = DEFAULT_INITIAL_CAPACITY;   // 16
        newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY); // 12
    }
    // ... 计算新阈值

    // 创建新容量数组
    Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
    table = newTab;

    if (oldTab != null) {
        for (int j = 0; j < oldCap; ++j) {      // 遍历旧数组
            Node<K,V> e = oldTab[j];
            if (e != null) {
                oldTab[j] = null;               // help GC
                if (e.next == null)              // 只有一个节点
                    newTab[e.hash & (newCap - 1)] = e;
                else if (e instanceof TreeNode)   // 红黑树 split
                    ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                else {                           // 链表 → 拆分成高低位两条链
                    Node<K,V> loHead = null, loTail = null;
                    Node<K,V> hiHead = null, hiTail = null;
                    Node<K,V> next;
                    do {
                        next = e.next;
                        if ((e.hash & oldCap) == 0) {  // 关键判断：高位为 0 还是 1
                            if (loTail == null)
                                loHead = e;
                            else
                                loTail.next = e;
                            loTail = e;
                        } else {
                            if (hiTail == null)
                                hiHead = e;
                            else
                                hiTail.next = e;
                            hiTail = e;
                        }
                        e = next;
                    } while (e != null);
                    if (loTail != null) {
                        loTail.next = null;
                        newTab[j] = loHead;             // 低位链：位置不变
                    }
                    if (hiTail != null) {
                        hiTail.next = null;
                        newTab[j + oldCap] = hiHead;   // 高位链：位置 + oldCap
                    }
                }
            }
        }
    }
    return newTab;
}
```

**高低位链拆分原理——JDK 8 的优化精髓：**

JDK 7 需要重新计算每个元素在新数组中的位置（rehash 操作成本高）。JDK 8 利用**容量是 2 的幂次**这一特性，实现巧妙优化：

```text
扩容前：容量 n = 16，下标计算 (n - 1) & hash = 0x1111 & hash
扩容后：容量 2n = 32，(2n - 1) & hash = 0x11111 & hash

观察：
  (2n - 1) & hash 与 (n - 1) & hash 的区别在于第 5 位（从 0 开始）
  第 5 位的值 = hash & 16 = hash & oldCap

如果 hash & oldCap == 0：新位置 = 旧位置
如果 hash & oldCap == 1：新位置 = 旧位置 + oldCap

例：oldCap = 16
  hash1 = 0x0001  → hash1 & 16 = 0 → 位置不变（如 index=1）
  hash2 = 0x0011  → hash2 & 16 = 1 → 位置 + 16（index=1+16=17）
```

> 🎯 **JDK 8 rehash 优化**：不再逐个重新计算 hash，而是用 `(e.hash & oldCap) == 0` 一分为二——原位置链和原位置 + oldCap 链。位置判断从 O(n) 降为 O(1)，且节点间的相对顺序不变（尾插法保证）。

### 3.5 树化与退化阈值

| 阈值常量 | 值 | 含义 |
|---------|:---:|------|
| `TREEIFY_THRESHOLD` | 8 | 链表长度 ≥ 8 时转为红黑树 |
| `UNTREEIFY_THRESHOLD` | 6 | 扩容拆分后红黑树节点 ≤ 6 时退化为链表 |
| `MIN_TREEIFY_CAPACITY` | 64 | 数组长度 < 64 时，即使链表 >= 8 也**不树化，而是扩容** |

**为什么树化门槛是 8？**

```text
源码注释解释（翻译大意）：

当 hashCode 分布均匀时，每个槽位的链表长度符合泊松分布，
平均长度约 0.5，链表长度达到 8 的概率约为 0.00000006
（六亿分之一）。因此设置 8 作为阈值意味着：
- 正常情况下（hash 分布均匀）永远不会触发树化
- 只有 hash 函数严重缺陷或恶意构造哈希碰撞时才会树化
- 红黑树是保底措施，避免 hash 攻击（DoS）

泊松分布 P(X=λ) 概率：
  k=0: 0.60653066
  k=1: 0.30326533
  k=2: 0.07581633
  k=3: 0.01263606
  k=4: 0.00157952
  k=5: 0.00015795
  k=6: 0.00001316
  k=7: 0.00000094
  k=8: 0.00000006   ← 六亿分之一
```

> 🎯 **树化的真正原因不是"提升链表性能"，而是防御性设计**——防止大量哈希冲突导致链表退化到 O(n)。8 是泊松分布下的极小概率事件，由于红黑树节点的内存开销是普通节点的 2 倍（TreeNode 有 parent、left、right、prev、red 五个额外字段），因此只在极端情况下启用树化。

**为什么退化是 6 而不是 7？**

避免在阈值附近来回震荡：如果链表长度恰好是 7，频繁增删导致反复树化/退化，性能损耗巨大。**6 和 8 之间的 1 的差值作为缓冲区间**。

**MIN_TREEIFY_CAPACITY = 64 的含义：**

当数组很短时，即使某个槽链表很长，也可能是整体分布不均而非 hash 函数问题——此时应优先扩容（让元素分散到更多槽），而不是树化。

### 3.6 负载因子 0.75 的数学含义

```java
// 阈值计算公式
threshold = (int)(capacity * loadFactor);

// 默认：threshold = 16 * 0.75 = 12
// 插入第 13 个元素时触发扩容，容量从 16 → 32
```

| 负载因子 | 优点 | 缺点 |
|---------|------|------|
| 0.75（JDK 默认） | 空间与时间的平衡 | 无 |
| 1.0 | 空间利用率高（满才扩容） | 哈希碰撞剧增，查询/插入性能下降 |
| 0.5 | 哈希碰撞极少，性能好 | 空间浪费严重（一半空位） |

> 🎯 **为什么是 0.75 不是 0.7 或 0.8？** 源码注释说明这是在空间利用率和查询效率之间寻找的一个"经验平衡点"。统计学上，在随机 hashCode 下，负载因子为 0.75 时，桶中节点数量的期望约为 0.5，冲突概率较低。且 0.75 = 3/4，容量乘以 0.75 是整数（容量是 16 的倍数，16 * 0.75 = 12），避免浮点运算的精度损失。

**自定义初始容量和负载因子的最佳实践：**

```java
// 预知键值对数量时，按需初始化
int expectedSize = 1000;

// 错误方式：直接用预期大小
Map<String, String> bad = new HashMap<>(expectedSize);
// 实际容量 1024（2 的幂），threshold = 1024 * 0.75 = 768
// 插入 1000 > 768 → 触发扩容！→ 容量 2048

// 正确方式：考虑负载因子
int capacity = (int)(expectedSize / 0.75f) + 1;
Map<String, String> good = new HashMap<>(capacity);
// capacity ≈ 1334 → 实际容量 2048，threshold = 1536 > 1000 → 不扩容

// 或者使用 Guava
// import com.google.common.collect.Maps;
// Maps.newHashMapWithExpectedSize(1000);
```

### 3.7 JDK 7 头插法死循环 vs JDK 8 尾插法修复

**JDK 7 头插法死循环问题（多线程环境）：**

```text
条件：两个线程同时 resize()

扩容前：
  [1] → [3] → null       （槽位 index 上两个元素）

扩容时 transfer()：
  void transfer(Entry[] newTable, boolean rehash) {
      int newCapacity = newTable.length;
      for (Entry<K,V> e : table[j]; e != null; ) {
          Entry<K,V> next = e.next;   // 先保存下一个节点
          e.next = newTable[i];        // 头插：e 指向新数组当前头
          newTable[i] = e;             // 设置 e 为新数组的头
          e = next;                    // 处理下一个
      }
  }

临界点：两个线程同时执行到 Entry<K,V> next = e.next;
此时 [1] → [3] → null

线程 1 挂起：
  e = [1], next = [3]
  头插过程：[1] → null → 下一轮 [3] → [1] → null

线程 2 完成扩容：
  新数组：[3] → null, [1] → [3] → null
  且 table 指向了新数组

线程 1 继续：
  此时 table 已经变了，但线程 1 的 e 和 next 是旧的引用：
  e = [1], next = [3]
  [1].next = newTable[i]  → [1].next = [3]  ← 环形！
  newTable[i] = [1]
  → [1] → [3] → [1] → [3] ... 死循环！
```

**JDK 8 的修复：**

1. **尾插法**：新节点追加到链表尾部，而非头部。即使在扩容时，也是拆分成两条链（loTail → null, hiTail → null），节点顺序不变，不会形成环
2. **高低位链拆分**：扩容时不重新计算每个 hash，而是用 `(e.hash & oldCap)` 分成"低位链"和"高位链"，链内顺序与旧链表一致

> ⚠️ **注意**：JDK 8 的 HashMap 仍然**不是线程安全**的，尾插法修复的是"死循环"这一特定 bug，但多线程下仍然存在数据丢失（put 覆盖）、size 计数不准确等问题。需要用 `ConcurrentHashMap`。

### 3.8 HashMap 变体：LinkedHashMap 与 TreeMap

#### LinkedHashMap — 保留插入顺序或访问顺序

```java
// 核心区别：在 HashMap 基础上，用双向链表维护顺序
// Entry 继承自 HashMap.Node，多了 before/after 指针
static class Entry<K,V> extends HashMap.Node<K,V> {
    Entry<K,V> before, after;  // 前驱和后继
}

// 构造参数 accessOrder：true = 访问顺序（LRU），false = 插入顺序（默认）
Map<String, String> lru = new LinkedHashMap<>(16, 0.75f, true);
```

**LinkedHashMap 的三大用途：**

| 场景 | 方式 | 说明 |
|------|------|------|
| 保持插入顺序 | 默认 (accessOrder=false) | 遍历顺序与 put 顺序一致（不像 HashMap 无序） |
| LRU 缓存 | accessOrder=true | 每次 get/put 将节点移到链表末尾，最久未访问的在头部 |
| 实现 LRU 淘汰 | 重写 `removeEldestEntry` | 在插入后判断是否移除最老节点 |

```java
// 基于 LinkedHashMap 实现 LRU 缓存
class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int maxCapacity;

    public LRUCache(int maxCapacity) {
        super(16, 0.75f, true);  // accessOrder = true
        this.maxCapacity = maxCapacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxCapacity;  // 超过容量时淘汰最久未访问的
    }
}

// 使用示例
LRUCache<String, String> cache = new LRUCache<>(3);
cache.put("A", "1");
cache.put("B", "2");
cache.put("C", "3");
cache.get("A");                    // 访问 A，A 被移到末尾
cache.put("D", "4");               // 此时 size=4 > 3，自动淘汰"最久未访问"的 B
System.out.println(cache);         // {C=3, A=1, D=4} — B 已被淘汰
```

#### TreeMap — 红黑树 + 排序键

| 特性 | HashMap | TreeMap | LinkedHashMap |
|------|---------|---------|-------------|
| 底层 | 数组+链表+红黑树 | 红黑树 | 数组+链表+双向链表 |
| 元素顺序 | 无序 | 自然序/比较器序 | 插入序/访问序 |
| 时间复杂度 | O(1) 均摊 | O(log n) | O(1) 均摊 |
| 空间开销 | 较小 | 较大（红黑树着色、子节点指针） | 较大（双向链表指针） |
| null 键 | 允许 | 不允许（自然序下无法比较 null） | 允许 |
| 适用范围 | 通用查找 | 需要有序遍历的键值对 | 需要预测遍历顺序 |

```java
// TreeMap 使用示例
TreeMap<String, Integer> treeMap = new TreeMap<>();
treeMap.put("Charlie", 3);
treeMap.put("Alice", 1);
treeMap.put("Bob", 2);
// 按字母顺序遍历
treeMap.forEach((k, v) -> System.out.println(k + "=" + v));
// 输出：
// Alice=1
// Bob=2
// Charlie=3

// 常用导航方法
treeMap.firstKey();        // "Alice"
treeMap.lastKey();         // "Charlie"
treeMap.lowerKey("Bob");   // "Alice"（小于 Bob 的最大键）
treeMap.higherKey("Bob");  // "Charlie"（大于 Bob 的最小键）
treeMap.subMap("B", "D");  // {Bob=2, Charlie=3}（B ≤ key < D）
```

---

## 4. HashSet、TreeSet 与 LinkedHashSet

### 4.1 内部结构：全部委托给 Map

HashSet、TreeSet 和 LinkedHashSet 在底层的实现方式非常"偷懒"——**它们内部持有一个对应的 Map 实例，元素作为 Map 的 key，value 固定为一个哑对象 `PRESENT`**。

```java
// HashSet 核心源码
public class HashSet<E> {
    private transient HashMap<E, Object> map;

    // 虚值对象：所有元素共享同一个 Object 实例作为 value
    private static final Object PRESENT = new Object();

    public HashSet() {
        map = new HashMap<>();  // 内部就是 HashMap！
    }

    public boolean add(E e) {
        // 元素作为 key 放入 HashMap，value 固定为 PRESENT
        return map.put(e, PRESENT) == null;
        // put 返回 null 说明之前没有该 key → 添加成功
        // put 返回非 null 说明 key 已存在 → 添加失败
    }

    public boolean contains(Object o) {
        return map.containsKey(o);  // 委托给 HashMap
    }

    public boolean remove(Object o) {
        return map.remove(o) == PRESENT;
    }
}

// TreeSet 内部是 TreeMap
public class TreeSet<E> {
    private transient NavigableMap<E, Object> m;
    // 无参构造：m = new TreeMap<>();
}

// LinkedHashSet 继承 HashSet，但通过 LinkedHashMap 实现
// 核心：LinkedHashSet 的构造调用的是 HashSet 的特殊构造器
// HashSet(int initialCapacity, float loadFactor, boolean dummy) {
//     map = new LinkedHashMap<>(initialCapacity, loadFactor);
// }
```

### 4.2 元素唯一性的保证

Set 保证元素唯一的条件是：**`equals` 为 true 且 `hashCode` 相等**。

```java
// 错误示例：没有重写 hashCode
class BadUser {
    private String name;
    private int age;

    public BadUser(String name, int age) {
        this.name = name;
        this.age = age;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BadUser)) return false;
        BadUser u = (BadUser) o;
        return age == u.age && Objects.equals(name, u.name);
    }

    // 忘记重写 hashCode！！！
}

// 测试
Set<BadUser> set = new HashSet<>();
set.add(new BadUser("Alice", 20));
set.add(new BadUser("Alice", 20));
System.out.println(set.size());  // 2 ❌ 因为两个对象 hashCode 不同（默认 Object.hashCode）

// 正确示例
class GoodUser {
    private String name;
    private int age;

    // 构造器、equals ...

    @Override
    public int hashCode() {
        return Objects.hash(name, age);  // 用 Objects.hash 生成
    }
}

Set<GoodUser> set2 = new HashSet<>();
set2.add(new GoodUser("Alice", 20));
set2.add(new GoodUser("Alice", 20));
System.out.println(set2.size());  // 1 ✅
```

> 💡 **Set 查找唯一性的底层流程（HashMap 视角）：**
> 1. `put(key, PRESENT)` → 先调 `hash(key.hashCode())` 找到桶位置
> 2. 桶内遍历时先比较 `hash` 值（快速过滤）
> 3. 再比较 `equals`（精确判断）
> 4. 两者都相等才认为是同一个 key → 不插入

### 4.3 HashSet vs TreeSet vs LinkedHashSet

| 特性 | HashSet | TreeSet | LinkedHashSet |
|------|---------|---------|--------------|
| 底层 | HashMap | TreeMap（红黑树） | LinkedHashMap + HashSet |
| 元素顺序 | 无序 | 自然序/比较器序 | 插入顺序 |
| 性能 | O(1) 均摊 | O(log n) | O(1) 均摊 |
| null 值 | 允许一个 null | 不允许（compareTo 抛 NPE） | 允许 |
| 适用场景 | 通用去重 | 需要有序集合 | 需要保持插入顺序的去重 |

```java
// 选择示例
Set<String> hashSet = new HashSet<>(Arrays.asList("C", "A", "B", "A"));
System.out.println(hashSet);         // 可能 [A, B, C] — 无序，去重成功

Set<String> treeSet = new TreeSet<>(Arrays.asList("C", "A", "B", "A"));
System.out.println(treeSet);         // [A, B, C] — 字典序

Set<String> linkedSet = new LinkedHashSet<>(Arrays.asList("C", "A", "B", "A"));
System.out.println(linkedSet);       // [C, A, B] — 保持插入顺序，去重成功
```

---

## 5. 迭代器、fail-fast 与 fail-safe

### 5.1 modCount —— fail-fast 的底层实现

fail-fast 机制的核心是集合内部维护的一个计数器 `modCount`。

```java
// AbstractList 中的定义
protected transient int modCount = 0;

// modCount 的变更时机（各种结构性修改都 ++modCount）：
// ArrayList.add()    → modCount++
// ArrayList.remove() → modCount++
// ArrayList.clear()  → modCount++
// 等等

// Iterator 创建时会保存 expectedModCount = modCount
private class Itr implements Iterator<E> {
    int cursor;           // 下一个返回元素的索引
    int lastRet = -1;     // 上一个返回元素的索引（-1 表示无）
    int expectedModCount = modCount;  // 快照时刻的 modCount

    // 每次调用 next() 时都检查
    public E next() {
        checkForComodification();  // ← 检查点
        int i = cursor;
        // ... 返回元素
    }

    final void checkForComodification() {
        if (modCount != expectedModCount)
            throw new ConcurrentModificationException();  // ← 抛异常！
    }
}
```

**判断时机总结：**

| 操作 | 是否检查 modCount | 说明 |
|------|:---:|------|
| `iterator.next()` | 是 | 每次获取元素时检查 |
| `iterator.remove()` | 是 | 先检查再删除，然后同步 expectedModCount |
| `foreach` 循环中的集合修改 | 是 | foreach 的底层是 iterator，会检查 |
| `forEach()` (Iterable 默认方法) | 是 | 本质也是迭代器 |
| `list.get(i)` （直接索引） | 否 | 直接操作不通过迭代器 |

```java
// 反例：foreach 中直接删除元素
List<String> list = new ArrayList<>(Arrays.asList("A", "B", "C", "D"));
for (String s : list) {
    if ("B".equals(s)) {
        list.remove(s);  // ❌ ConcurrentModificationException！
        // 底层：list.remove() → modCount++
        // 下一次 foreach.next() → checkForComodification → modCount != expectedModCount
    }
}

// 正确方式 1：使用 iterator.remove()
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    String s = it.next();
    if ("B".equals(s)) {
        it.remove();  // ✅ 内部会同步 expectedModCount
    }
}

// 正确方式 2：JDK 8 removeIf（内部用了 iterator）
list.removeIf(s -> "B".equals(s));  // ✅ 推荐

// 正确方式 3：收集到新集合，统一删除
List<String> toRemove = new ArrayList<>();
for (String s : list) {
    if ("B".equals(s)) toRemove.add(s);
}
list.removeAll(toRemove);
```

> 🎯 **fail-fast 的设计哲学**：与其在并发修改时产生不可预知的结果（数据损坏、死循环），不如**快速失败**，将问题暴露给开发者。ConcurrentModificationException 是一个**编程错误的信号**，不应用 try-catch 处理。

### 5.2 fail-safe（安全失败）机制

fail-safe 迭代器的特点是：**迭代器在创建时对数据做一个快照（或拷贝），迭代器遍历的是快照，而不是集合本身**。因此，即使原集合在迭代中被修改，迭代器仍然安全无异常。

```java
// CopyOnWriteArrayList —— fail-safe 的代表
List<String> safeList = new CopyOnWriteArrayList<>(Arrays.asList("A", "B", "C", "D"));

// 迭代器创建时，内部保存了当前数组的快照引用
Iterator<String> it = safeList.iterator();
safeList.add("E");          // 修改原集合 —— OK
safeList.remove("A");       // 修改原集合 —— OK

// 迭代器遍历的是创建时的快照，不受影响
while (it.hasNext()) {
    System.out.println(it.next());  // 仍输出 A, B, C, D（没有 E，因为那是快照后的修改）
}
```

**fail-fast vs fail-safe 对比：**

| 特性 | fail-fast | fail-safe |
|------|-----------|-----------|
| 代表集合 | ArrayList, HashMap, HashSet 等 | CopyOnWriteArrayList, ConcurrentHashMap |
| 检测并发修改 | 通过 modCount 检测 | 不检测，直接操作快照/分段锁 |
| 遍历的数据 | 集合原始数据 | 快照副本或安全遍历 |
| 是否抛 CME | 是 | 否 |
| 性能开销 | 低（仅一个 int 比较） | 较高（拷贝成本） |
| 一致性 | 强一致性 | 弱一致性（最终一致性） |
| 适用场景 | 单线程或安全同步 | 高并发读多写少 |

### 5.3 forEach 与 Spliterator

**forEach (JDK 8 Iterable 默认方法)：**

```java
// 内部迭代 —— 框架帮你管理迭代器
list.forEach(s -> {
    if ("B".equals(s)) {
        // 注意：内部实现本质还是用 iterator，所以这里修改集合仍然会抛 CME
        // list.remove(s);  ❌ 也是不行的
    }
});

// 等效于：
for (String s : list) {
    // 处理每个元素
}
```

**Spliterator（可分割迭代器）：**

```java
// Spliterator 是并行流的基石，支持分治遍历
List<String> list = Arrays.asList("A", "B", "C", "D", "E", "F");

// 看看 Spliterator 的特性
Spliterator<String> spliterator = list.spliterator();
System.out.println(spliterator.characteristics());
// 输出包含：ORDERED, SIZED, SUBSIZED 等标志位

// 分割：将原迭代器一分为二
Spliterator<String> secondHalf = spliterator.trySplit();

// 遍历剩余部分
spliterator.forEachRemaining(System.out::println);

// Spliterator 特性标志：
//   ORDERED  — 有顺序
//   SIZED    — 知道精确大小
//   SUBSIZED — 分割后的大小也是精确的
//   DISTINCT — 元素不重复
//   IMMUTABLE — 不可修改
//   NONNULL  — 不允许 null
//   CONCURRENT — 线程安全
```

| 迭代方式 | 控制方 | 优点 | 缺点 |
|---------|--------|------|------|
| 增强 for | 外部（开发者） | 简单直观 | 无法在循环中安全删除 |
| Iterator | 外部 | 支持安全删除 | 冗余 |
| forEach | 内部（框架） | 简洁 Lambda | 无法在循环中抛受检异常 |
| Stream | 内部 | 并行、延迟、链式 | 调试相对困难 |
| Spliterator | 内部 | 分治并行、轻量 | 使用门槛高 |

---

## 6. Comparable vs Comparator 排序体系

### 6.1 自然序 Comparable

```java
// Comparable 位于 java.lang 包
public interface Comparable<T> {
    public int compareTo(T o);
}

// 返回值约定：
// this < o   → 返回负数（通常 -1）
// this == o  → 返回 0
// this > o   → 返回正数（通常 1）
```

**很多核心类已经实现了 Comparable：**

| 类 | 自然排序规则 |
|----|------------|
| `Integer` | 数值从小到大 |
| `String` | 字典序（Unicode 码点） |
| `LocalDate` | 日期先后 |
| `BigDecimal` | 数值大小（注意 scale 不影响） |

```java
// 常用类的自然序
System.out.println(Integer.compare(3, 5));          // -1 (3 < 5)
System.out.println("banana".compareTo("apple"));    // 1  ("banana" > "apple")
System.out.println(LocalDate.of(2024, 1, 2)
    .compareTo(LocalDate.of(2024, 1, 1)));           // 1 (2024-01-02 > 2024-01-01)
```

**自定义类实现 Comparable：**

```java
public class Student implements Comparable<Student> {
    private String name;
    private int score;

    public Student(String name, int score) {
        this.name = name;
        this.score = score;
    }

    @Override
    public int compareTo(Student o) {
        // 先按分数降序，分数相同按姓名升序
        int scoreCmp = Integer.compare(o.score, this.score);  // 降序：o.score - this.score
        if (scoreCmp != 0) return scoreCmp;
        return this.name.compareTo(o.name);  // 升序
    }

    // getter, toString ...
}

// 使用
List<Student> students = Arrays.asList(
    new Student("Alice", 90),
    new Student("Bob", 85),
    new Student("Charlie", 90)
);
Collections.sort(students);  // 依赖 Student 的 compareTo
// 结果：[Charlie(90), Alice(90), Bob(85)]
```

### 6.2 定制序 Comparator

```java
// Comparator 位于 java.util 包，是一个函数式接口
@FunctionalInterface
public interface Comparator<T> {
    int compare(T o1, T o2);

    // JDK 8+ 大量默认方法和静态方法（链式 API）
}
```

**Comparator 的三大优势：**

1. **不修改原类**：给不能或不想修改源码的类排序
2. **多种排序规则**：同一个类可以有不同的排序
3. **精细控制**：null 处理、排序链、反转

```java
// 1. 匿名内部类（JDK 7 及以前风格）
Comparator<Student> byScore = new Comparator<Student>() {
    @Override
    public int compare(Student a, Student b) {
        // 分数降序
        return Integer.compare(b.getScore(), a.getScore());
    }
};

// 2. Lambda 表达式（JDK 8）
Comparator<Student> byScoreLambda =
    (a, b) -> Integer.compare(b.getScore(), a.getScore());

// 3. Comparator 静态方法链（JDK 8 推荐）
Comparator<Student> byScoreChain =
    Comparator.comparingInt(Student::getScore).reversed()
        .thenComparing(Student::getName);

students.sort(byScoreChain);          // 等效于 Collections.sort(students, byScoreChain);

// 4. 处理 null
Comparator<String> nullSafe =
    Comparator.nullsFirst(String::compareTo);        // null 视为最小值
Comparator<String> nullLast =
    Comparator.nullsLast(Comparator.naturalOrder()); // null 视为最大值
```

### 6.3 链式 Comparator API 全景

| 方法 | 作用 | 示例 |
|------|------|------|
| `comparing(keyExtractor)` | 按指定键排序 | `Comparator.comparing(Student::getScore)` |
| `comparingInt(keyExtractor)` | 按 int 键排序（避免装箱） | `Comparator.comparingInt(Student::getScore)` |
| `comparingLong(keyExtractor)` | 按 long 键排序 | `Comparator.comparingLong(Person::getId)` |
| `comparingDouble(keyExtractor)` | 按 double 键排序 | `Comparator.comparingDouble(Item::getPrice)` |
| `thenComparing(next)` | 当前排序相同后的次级排序 | `comparingInt(s).thenComparing(Student::getName)` |
| `reversed()` | 反转排序 | `comparingInt(s).reversed()` |
| `naturalOrder()` | 自然序 | `Comparator.<String>naturalOrder()` |
| `reverseOrder()` | 反转自然序 | `Comparator.<String>reverseOrder()` |
| `nullsFirst(comp)` | null 值排在最前 | `nullsFirst(comparingInt(s))` |
| `nullsLast(comp)` | null 值排在最后 | `nullsLast(comparingInt(s))` |

```java
// 复杂业务排序示例
// 需求：按分数降序 → 姓名升序 → null 的分数排在最后
Comparator<Student> complex =
    Comparator.nullsLast(       // 整个 Student 对象为 null 的处理
        Comparator.comparingInt(Student::getScore)
            .reversed()
            .thenComparing(Student::getName)
    );

// 使用
List<Student> list = Arrays.asList(
    new Student("Charlie", 90),
    null,
    new Student("Alice", 90),
    new Student("Bob", 85)
);
list.sort(complex);
// 结果：[Alice(90), Charlie(90), Bob(85), null]  ← null 最后
```

### 6.4 Comparable vs Comparator 对比

| 维度 | Comparable | Comparator |
|------|-----------|------------|
| 包 | `java.lang` | `java.util` |
| 函数 | `int compareTo(T o)` | `int compare(T o1, T o2)` |
| 参数数量 | 1 | 2 |
| 含义 | 自然序（"我是谁"） | 定制序（"规则是什么"） |
| 修改原类 | 需要实现接口 | 不需要——策略模式 |
| 灵活性 | 单一排序规则 | 多种排序规则可切换 |
| 空值处理 | 通常不支持（抛 NPE） | 支持 nullsFirst/nullsLast |
| 链式组合 | 不支持 | 支持 thenComparing |
| 常用方法 | `Collections.sort(list)` | `Collections.sort(list, comparator)` |

> 🎯 **选择原则**：如果"这类天生就有一种自然的排序方式"（如日期、数字），实现 Comparable；如果是"临时根据业务需要排序"或不能修改源码，用 Comparator。

---

## 7. Collections 与 Arrays 工具类

### 7.1 Collections 常用方法

| 类别 | 方法 | 作用 |
|------|------|------|
| 排序 | `sort(List)` / `sort(List, Comparator)` | 排序（需要元素实现 Comparable） |
| 查找 | `binarySearch(List, key)` | 二分查找（需要 List 已排序） |
| 洗牌 | `shuffle(List)` / `shuffle(List, Random)` | 随机打乱 |
| 反转 | `reverse(List)` | 反转列表顺序 |
| 旋转 | `rotate(List, distance)` | 列表循环位移 |
| 填充 | `fill(List, obj)` | 全部替换为同一个值 |
| 复制 | `copy(dest, src)` | 列表拷贝（dest 长度 ≥ src） |
| 最值 | `min(Collection)` / `max(Collection)` | 自然序的最大最小值 |
| 频率 | `frequency(Collection, obj)` | 元素出现次数 |
| 替换 | `replaceAll(List, old, new)` | 全部替换元素 |
| 交换 | `swap(List, i, j)` | 交换两个位置 |
| 同步包装 | `synchronizedList/Set/Map(...)` | 返回线程安全包装器 |
| 不可变包装 | `unmodifiableList/Set/Map(...)` | 返回只读视图 |
| 单值 | `singletonList/Set/Map(...)` | 只有一个元素的集合 |
| 空集合 | `emptyList/Set/Map(...)` | 空集合常量（类型安全） |

```java
// 同步包装示例
List<String> syncList = Collections.synchronizedList(new ArrayList<>());
// 注意：迭代遍历时仍需要在外部同步块
synchronized (syncList) {
    Iterator<String> it = syncList.iterator();
    while (it.hasNext()) {
        System.out.println(it.next());
    }
}

// 不可变集合
List<String> unmod = Collections.unmodifiableList(Arrays.asList("A", "B", "C"));
// unmod.add("D");  // ❌ UnsupportedOperationException

// 不可变集合陷阱：原引用仍然可改
List<String> mutable = new ArrayList<>(Arrays.asList("A", "B"));
List<String> readOnly = Collections.unmodifiableList(mutable);
mutable.add("C");  // 原引用修改了 → readOnly 也会变（视图效果）
// 安全做法：先包装为不可变，再丢弃原引用

// JDK 9+ 有更好的选择 List.of()
// List<String> immutable = List.of("A", "B", "C");  // 真正的不可变
```

### 7.2 Arrays 常用方法

| 类别 | 方法 | 作用 |
|------|------|------|
| 排序 | `sort(int[]/Object[]/T[]...)` | 完整数组排序（Dual-Pivot QuickSort） |
| 并行排序 | `parallelSort(T[])` | 多线程排序（ForkJoin 池） |
| 二分查找 | `binarySearch(arr, key)` | 需要先排序 |
| 填充 | `fill(arr, val)` | 全部替换 |
| 拷贝 | `copyOf(arr, newLen)` | 扩容/截断数组 |
| 范围拷贝 | `copyOfRange(arr, from, to)` | 部分复制 |
| 比较 | `equals(arr1, arr2)` | 元素级相等比较 |
| 深度比较 | `deepEquals(Object[], Object[])` | 嵌套数组 |
| 转 List | `asList(T...)` | 数组 → 固定大小列表 |
| 哈希 | `hashCode(arr)` | 数组哈希值 |
| 转字符串 | `toString(arr)` | 格式化为 `[a, b, c]` |
| 深度转字符串 | `deepToString(Object[])` | 多维数组 |

**重点陷阱：Arrays.asList()**

```java
// Arrays.asList() 返回的是 Arrays 的内部类 ArrayList（不是 java.util.ArrayList）
List<String> list = Arrays.asList("A", "B", "C");

list.set(0, "X");             // ✅ 修改元素 OK
// list.add("D");             // ❌ UnsupportedOperationException：固定长度
// list.remove("A");          // ❌ UnsupportedOperationException

// 内部类 Arrays.ArrayList 没有实现 add/remove
// 它的 size 是数组长度，无法改变

// 如果需要可变列表：
List<String> mutableList = new ArrayList<>(Arrays.asList("A", "B", "C"));
mutableList.add("D");  // ✅

// 同时注意：asList 返回的列表与原数组共享底层数据
String[] arr = {"A", "B", "C"};
List<String> list2 = Arrays.asList(arr);
list2.set(0, "X");
System.out.println(arr[0]);  // "X" — 改 list 就是改数组！
arr[0] = "Y";
System.out.println(list2.get(0));  // "Y" — 改数组也影响 list
```

---

## 8. 选型速查与练习

### 8.1 集合选型速查表

```text
你需要什么？
│
├─ 存储单值
│   ├─ 需要去重？
│   │   ├─ 是 → 不需要排序 → HashSet
│   │   │           需要排序 → TreeSet
│   │   │           需要按插入序 → LinkedHashSet
│   │   └─ 否 →
│   │       随机访问为主 → ArrayList
│   │       头尾增删为主 → ArrayDeque / LinkedList
│   │       需要线程安全 → CopyOnWriteArrayList
│   └─ 需要队列/栈？→ ArrayDeque / LinkedList
│
├─ 存储键值对
│   ├─ 需要排序键？→ TreeMap
│   │      需要 LRU？→ LinkedHashMap(accessOrder=true)
│   │      其他 → HashMap
│   └─ 需要并发？→ ConcurrentHashMap
│
├─ 需要并发访问？
│   ├─ 高性能并发 Map → ConcurrentHashMap
│   ├─ 高性能并发 Queue → ConcurrentLinkedQueue / LinkedBlockingQueue
│   └─ 读多写少 List → CopyOnWriteArrayList
│
└─ 需要不可变？
     └─ Java 9+: List.of() / Set.of() / Map.of()
        Java 8-: Collections.unmodifiableXxx()
```

### 8.2 时间复杂度速查

| 操作 | ArrayList | LinkedList | HashSet | TreeSet | HashMap | TreeMap |
|------|:---------:|:----------:|:-------:|:-------:|:-------:|:-------:|
| 插入 | O(1) 均摊 | O(1) 头尾 | O(1) | O(log n) | O(1) 均摊 | O(log n) |
| 删除 | O(n) | O(1) 头尾 | O(1) | O(log n) | O(1) 均摊 | O(log n) |
| 查找 | O(1) 索引 / O(n) 值 | O(n) | O(1) | O(log n) | O(1) 均摊 | O(log n) |
| 遍历 | O(n) | O(n) | O(n) | O(n) | O(n) | O(n) |

### 8.3 自测练习 15 题

```java
// 练习 1：判断输出
List<String> list = new ArrayList<>(Arrays.asList("A", "B", "C"));
list.subList(1, 3).clear();
System.out.println(list);  // 输出什么？___________

// 练习 2：扩容次数
// ArrayList 无参构造，连续添加 17 个元素，resize() 触发几次？___________

// 练习 3：HashMap 容量
// HashMap 容量为何必须是 2 的幂？请从下标计算角度解释

// 练习 4：hash 扰动
// "Aa" 和 "BB" 的 hashCode 一样吗？HashMap 中它们会冲突吗？

// 练习 5：ConcurrentModificationException
// 以下代码是否抛异常？
List<Integer> nums = new ArrayList<>(Arrays.asList(1, 2, 3));
for (int i : nums) {
    if (i == 2) nums.remove(Integer.valueOf(i));
}  // ___________

// 练习 6：TreeMap 的 null 键
// TreeMap 的 key 可以为 null 吗？为什么？___________

// 练习 7：LinkedHashMap LRU
// 如果 removeEldestEntry 始终返回 true，会发生什么？___________

// 练习 8：Comparator 链
// 按年龄升序，年龄相同按姓名降序，如何写 Comparator？___________

// 练习 9：fail-safe
// CopyOnWriteArrayList 的迭代器支持 remove 吗？___________

// 练习 10：Arrays.asList 陷阱
List<String> asList = Arrays.asList("A", "B");
asList.add("C");  // 结果？___________

// 练习 11：自定义类作为 HashMap key
// 如果只重写 equals 不重写 hashCode，HashMap 的 get 会怎样？___________

// 练习 12：负载因子
// HashMap loadFactor = 0.75，容量 64 时，threshold 是多少？___________

// 练习 13：Spliterator 分割
// 一个 size=10 的 ArrayList 的 spliterator，调用一次 trySplit 后，
// 原 spliterator 还剩多少个元素？___________

// 练习 14：Collections.synchronizedList
// 以下代码哪里不安全？
List<String> sync = Collections.synchronizedList(new ArrayList<>());
sync.add("A");
sync.add("B");
for (String s : sync) {    // 这里安全吗？
    System.out.println(s);
}  // ___________

// 练习 15：TreeSet 与 equals
// TreeSet 去重依赖的是 compareTo 还是 equals？考虑 a.compareTo(b) == 0 但 a.equals(b) == false
```

**参考答案：**

| 题号 | 答案 | 要点 |
|:---:|------|------|
| 1 | `[A]` | subList 是视图，clear 会删除原 List 中对应的元素 |
| 2 | 1 次 | 无参构造容量 0→10（第 1 次 add），第 11 个元素触发扩容到 15，17 < 15 不扩容。实际上第 11 个触发一次扩容到 15，所以 17 个元素时已扩过一次。答案是**1 次**（0→10 那次算初始化不算 resize，第 11 个元素添加时 resize → 15） |
| 3 | `(n - 1) & hash` 等价于 `hash % n`，效率远高取模；且 2 次幂时低位全 1，新 hash 均匀分布；rehash 可用高低位拆分 | 位运算 vs 模运算 |
| 4 | "Aa" = 2112, "BB" = 2112，不同字符串但可能 hashCode 碰撞；HashMap 中确实会冲突 | hashCode 碰撞不可避免 |
| 5 | **抛 CME** | foreach 底层 iterator 检测 modCount |
| 6 | 不能，TreeMap 用 compareTo 排序，null 无法与任何对象比较 | 实现细节 |
| 7 | 每次插入后都被移除，集合始终为空 | LRU 退化 |
| 8 | `Comparator.comparingInt(Person::getAge).thenComparing(Comparator.comparing(Person::getName).reversed())` | 注意 reversed 的作用范围 |
| 9 | 不支持，抛出 UnsupportedOperationException | CopyOnWriteArrayList 的迭代器是快照 |
| 10 | 抛 UnsupportedOperationException | 固定长度 list |
| 11 | get 时找不到（即使 equals 为 true） | 找不到桶位置 |
| 12 | 48 | 64 * 0.75 = 48 |
| 13 | 约 5 个 | ArrayList 的 spliterator 均分 |
| 14 | 不安全——多线程下 foreach 遍历需要同步块 | synchronizedList 的文档说明 |
| 15 | **compareTo** | TreeSet 通过 TreeMap 实现，key 的比较依赖 Comparator/Comparable，不调 equals |

---

## 版本与参考

| 项 | 说明 |
|----|------|
| 基线 | Java 8；树化、尾插法、Lambda 链式 Comparator |
| JDK 7 差异 | 头插法（死循环隐患）、Entry 节点、无红黑树、扰动函数 4 次异或 |
| JDK 9+ | `List.of()` / `Set.of()` / `Map.of()` 不可变集合 |
| JDK 11+ | `Collection.toArray(IntFunction)` 便利方法 |
| 推荐源码 | `java.util.HashMap`、`java.util.ArrayList`、`java.util.TreeMap` |
| 参考书籍 | 《Effective Java》第 3 版第 3 章、《Java 核心技术》卷 I |

---

**上一模块**：[01-java.lang核心API深度解析](./01-java.lang核心API深度解析.md)
**下一模块**：[03-Java IO与NIO体系全解](./03-Java%20IO与NIO体系全解.md)
**返回总览**：[00-Java API知识体系总览](./00-Java API知识体系总览.md)
