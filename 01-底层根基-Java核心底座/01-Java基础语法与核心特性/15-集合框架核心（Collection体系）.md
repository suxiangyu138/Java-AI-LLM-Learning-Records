# 15 — 集合框架核心（Collection体系）

> Java集合框架是存储、管理一组数据的核心工具，替代数组的固定长度与类型单一弊端。本文全面剖析Collection体系（List/Set/Queue）及各实现类的底层数据结构、扩容机制、时间复杂度与选型决策。

## 目录

1. [集合框架全景图](#1-集合框架全景图)
2. [Collection接口与迭代器](#2-collection接口与迭代器)
3. [List体系：ArrayList、LinkedList、Vector](#3-list体系arraylistlinkedlistvector)
4. [ArrayList底层原理与扩容机制](#4-arraylist底层原理与扩容机制)
5. [LinkedList双向链表原理](#5-linkedlist双向链表原理)
6. [Set体系：HashSet、LinkedHashSet、TreeSet](#6-set体系hashsetlinkedhashsettreeset)
7. [Queue与Deque体系](#7-queue与deque体系)
8. [Collections工具类](#8-collections工具类)
9. [线程安全的List与Set](#9-线程安全的list与set)
10. [时间复杂度对比总表](#10-时间复杂度对比总表)
11. [集合选型决策指南](#11-集合选型决策指南)
12. [面试高频考点](#12-面试高频考点)

---

## 1. 集合框架全景图

Java 集合框架顶层分为两大独立体系：**Collection**（存储单个元素）和 **Map**（存储键值对），二者无继承关系。

```
                    +----------------+
                    |   Iterable     |
                    +-------+--------+
                            |
                    +-------v--------+
                    |  Collection    |
                    +-------+--------+
                   /        |         \
                  /         |          \
         +------v--+  +----v---+  +----v------+
         |   List   |  |  Set   |  |  Queue    |
         +----------+  +--------+  +-----------+
            |  |           |            |
        ArrayList  LinkedList  HashSet   LinkedList
        Vector                 TreeSet   ArrayDeque
        Stack                  LinkedHashSet  PriorityQueue


        +-----------------+
        |     Map         |   (不是 Collection 的子接口)
        +-----------------+
            |       |
        HashMap   TreeMap
        LinkedHashMap
        Hashtable
        ConcurrentHashMap
```

**Collection vs Map**：

| 特性 | Collection | Map |
|------|-----------|-----|
| 存储单位 | 单个元素 | 键值对 (K-V) |
| 核心子接口 | List, Set, Queue | （无子接口） |
| 通用实现 | ArrayList, HashSet | HashMap, TreeMap |
| 遍历方式 | 增强 for, Iterator | keySet, entrySet, values |

**三大子接口对比**：

| 接口 | 有序性 | 允许重复 | 允许 null | 实现类 |
|------|--------|---------|-----------|--------|
| List | 有（按插入顺序） | 允许 | 允许 | ArrayList, LinkedList, Vector |
| Set | 无（部分有） | 不允许 | 允许（部分） | HashSet, TreeSet, LinkedHashSet |
| Queue | 有（FIFO/优先级） | 允许 | 不允许（部分） | LinkedList, ArrayDeque, PriorityQueue |

> 注意：集合只能存储引用类型，不能直接存 `int`、`char`，必须使用包装类 `Integer`、`Character`。

---

## 2. Collection接口与迭代器

### 2.1 核心方法

```java
// Collection 接口的核心方法（所有集合都必须实现）
int size();                    // 元素个数
boolean isEmpty();             // 是否为空
boolean contains(Object o);    // 是否包含指定元素
boolean add(E e);              // 添加元素（Set 中重复返回 false）
boolean remove(Object o);      // 移除元素
void clear();                  // 清空
Iterator<E> iterator();        // 获取迭代器
Object[] toArray();            // 转为数组
```

### 2.2 迭代器与 fail-fast

```java
// Iterator 遍历（适用于所有 Collection）
List<String> list = Arrays.asList("A", "B", "C");
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    String s = it.next();
    // it.remove();  // 安全删除（推荐此方式）
}

// 增强 for 底层也是 Iterator
for (String s : list) {
    System.out.println(s);
}

// ⚠️ 迭代过程中禁止直接修改集合（会抛 ConcurrentModificationException）
// 内部通过 modCount 字段检测：每次结构性修改 modCount++，
// 迭代器创建时保存 expectedModCount = modCount，
// 每次 next() 检查 modCount != expectedModCount 则抛异常
```

> 💡 **fail-fast（快速失败）** 是一种 bug 检测机制，不是线程安全保障。并发修改应使用 `ConcurrentHashMap`、`CopyOnWriteArrayList` 等并发容器。

### 2.3 ListIterator（List 专属双向迭代器）

```java
ListIterator<String> li = list.listIterator();
while (li.hasNext()) { li.next(); }          // 前移到末尾
while (li.hasPrevious()) {                   // 后移遍历
    System.out.println(li.previous());
}
li.set("X");                                 // 修改当前元素
li.add("Y");                                 // 插入当前元素
```

---

## 3. List体系：ArrayList、LinkedList、Vector

| 对比项 | ArrayList | LinkedList | Vector |
|--------|-----------|------------|--------|
| 底层结构 | 动态数组 (Object[]) | 双向链表 (Node) | 动态数组 (Object[]) |
| 随机访问 get(i) | O(1) | O(n) | O(1) |
| 尾部插入 add(e) | O(1) 均摊 | O(1) | O(1) 均摊 |
| 头部插入 | O(n) | O(1) | O(n) |
| 指定位置插入 | O(n) 元素位移 | O(n) 遍历查找 | O(n) |
| 内存占用 | 更少（数组） | 更大（Node 对象） | 更少 |
| 线程安全 | 否 | 否 | 是（synchronized） |
| 适用场景 | 随机访问多，尾部增删多 | 头部增删多，队列/栈 | 已淘汰，不推荐 |

**Vector 淘汰原因**：JDK 1.0 遗留，方法级 `synchronized` 粒度太粗。官方建议不要再使用，用 `CopyOnWriteArrayList` 或 `Collections.synchronizedList()` 替代。

> 💡 **选型口诀**：读写比例高或尾部追加→`ArrayList`；频繁头部插入/删除→`LinkedList`；需要队列/栈操作→`LinkedList` 或 `ArrayDeque`。

---

## 4. ArrayList底层原理与扩容机制

### 4.1 数据结构

```
ArrayList 内部实现：
  ┌─────────────────────────────────────────────────────┐
  │  ArrayList                                          │
  │  ┌───────────────────────────────────────────────┐  │
  │  │ transient Object[] elementData                │  │
  │  │ [0] [1] [2] [3] [4] [5] [6] ...   [n-1]      │  │
  │  └───────────────────────────────────────────────┘  │
  │  int size                                           │
  │  int modCount                                       │
  └─────────────────────────────────────────────────────┘
```

### 4.2 核心源码分析

```java
// ========== 常量与字段 ==========
private static final int DEFAULT_CAPACITY = 10;              // 默认初始容量
private static final Object[] EMPTY_ELEMENTDATA = {};        // 空实例
private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {}; // 默认空
transient Object[] elementData;   // 存储元素的数组
private int size;                  // 元素个数

// ========== 构造器 ==========
// 无参构造：懒加载，初始化为空数组（第一次 add 才扩容到 10）
public ArrayList() {
    this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
}

// ========== add() 方法 ==========
public boolean add(E e) {
    ensureCapacityInternal(size + 1);   // 确保容量
    elementData[size++] = e;
    return true;
}
```

### 4.3 扩容机制（grow() 源码）

```java
// 扩容触发路径：add() → ensureCapacityInternal() → ensureExplicitCapacity() → grow()

private void ensureCapacityInternal(int minCapacity) {
    // 首次添加：取 DEFAULT_CAPACITY(10) 和 minCapacity 的最大值
    if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
        minCapacity = Math.max(DEFAULT_CAPACITY, minCapacity);
    }
    ensureExplicitCapacity(minCapacity);
}

private void ensureExplicitCapacity(int minCapacity) {
    modCount++;  // ⚠️ 结构性修改计数 +1
    if (minCapacity - elementData.length > 0)
        grow(minCapacity);
}

// ⭐ 扩容核心
private void grow(int minCapacity) {
    int oldCapacity = elementData.length;
    // 扩容 1.5 倍（位运算高效计算）
    // JDK 6: (oldCapacity * 3/2 + 1)
    // JDK 7+: oldCapacity + (oldCapacity >> 1) = 1.5 倍
    int newCapacity = oldCapacity + (oldCapacity >> 1);

    if (newCapacity - minCapacity < 0)
        newCapacity = minCapacity;           // 取所需最小值
    if (newCapacity - MAX_ARRAY_SIZE > 0)
        newCapacity = hugeCapacity(minCapacity); // 大数组处理

    // ⭐ 创建新数组 + 复制旧元素（主要性能开销）
    elementData = Arrays.copyOf(elementData, newCapacity);
}
```

**扩容机制总结**：

| 属性 | 值 |
|------|-----|
| 初始容量 | 0（懒加载），第一次 add 时扩容为 10 |
| 扩容因子 | 1.5 倍（`oldCapacity + (oldCapacity >> 1)`） |
| 扩容操作 | `Arrays.copyOf()` 创建新数组并复制旧数据 |
| 最大容量 | `Integer.MAX_VALUE - 8` |
| 时间复杂度 | 均摊 O(1)（扩容本身 O(n)，但发生频率低） |

> ⚠️ **扩容代价**：每次扩容需要 O(n) 的数组复制。预估大数据量时应指定初始容量，避免频繁扩容。例如：`new ArrayList<>(expectedSize)`。

---

## 5. LinkedList双向链表原理

### 5.1 数据结构

```
LinkedList 双向链表结构：
  ┌──────────┐    ┌──────────┐    ┌──────────┐
  │    Node   │    │    Node   │    │    Node   │
  │ prev null │◀──▶│ prev     │◀──▶│ prev     │
  │ item: A   │    │ item: B  │    │ item: C  │
  │ next ─────┼───▶│ next ────┼───▶│ next null│
  └──────────┘    └──────────┘    └──────────┘
       ↑                               ↑
     first                            last
```

### 5.2 节点结构与核心操作

```java
// 内部节点类
private static class Node<E> {
    E item;          // 数据
    Node<E> next;    // 后继指针
    Node<E> prev;    // 前驱指针

    Node(Node<E> prev, E element, Node<E> next) {
        this.item = element;
        this.next = next;
        this.prev = prev;
    }
}

transient Node<E> first;   // 头节点
transient Node<E> last;    // 尾节点
transient int size = 0;

// ========== 头部插入 O(1) ==========
private void linkFirst(E e) {
    final Node<E> f = first;
    final Node<E> newNode = new Node<>(null, e, f);
    first = newNode;
    if (f == null) last = newNode;    // 空链表情况
    else f.prev = newNode;
    size++; modCount++;
}

// ========== 尾部插入 O(1) ==========
private void linkLast(E e) {
    final Node<E> l = last;
    final Node<E> newNode = new Node<>(l, e, null);
    last = newNode;
    if (l == null) first = newNode;   // 空链表情况
    else l.next = newNode;
    size++; modCount++;
}

// ========== 按索引查找（二分优化） ==========
// ⭐ LinkedList 的 node() 方法：根据 index 靠近头还是尾决定遍历方向
Node<E> node(int index) {
    if (index < (size >> 1)) {
        // 前半部分 → 从头向后遍历
        Node<E> x = first;
        for (int i = 0; i < index; i++) x = x.next;
        return x;
    } else {
        // 后半部分 → 从尾向前遍历
        Node<E> x = last;
        for (int i = size - 1; i > index; i--) x = x.prev;
        return x;
    }
}
```

> 💡 LinkedList 的 get(index) 需要遍历链表，每次 O(n)，但 JDK 做了优化：用 `index < (size >> 1)` 判断目标靠近头部还是尾部，选择最短遍历路径。

---

## 6. Set体系：HashSet、LinkedHashSet、TreeSet

### 6.1 三种 Set 对比

| 特性 | HashSet | LinkedHashSet | TreeSet |
|------|---------|--------------|---------|
| 底层结构 | HashMap | LinkedHashMap | TreeMap（红黑树） |
| 顺序 | 无序 | 插入顺序 | 自然顺序/比较器顺序 |
| 时间复杂度 | O(1) | O(1) | O(log n) |
| null | 允许一个 null | 允许一个 null | 不允许（有 Comparator 时可） |
| 比较方式 | hashCode + equals | hashCode + equals | compareTo / Comparator |
| 适用场景 | 通用去重 | 保持插入顺序 | 自动排序 |

### 6.2 代码示例

```java
// HashSet：无序 O(1)
Set<String> hashSet = new HashSet<>();
hashSet.add("banana"); hashSet.add("apple"); hashSet.add("cherry");
hashSet.add("banana");         // 重复，不会添加
System.out.println(hashSet);   // 无序：[banana, apple, cherry]

// LinkedHashSet：保持插入顺序
Set<String> linkedHashSet = new LinkedHashSet<>();
linkedHashSet.add("banana"); linkedHashSet.add("apple"); linkedHashSet.add("cherry");
System.out.println(linkedHashSet); // 有序：[banana, apple, cherry]

// TreeSet：自动排序 O(log n)
Set<String> treeSet = new TreeSet<>();
treeSet.add("banana"); treeSet.add("apple"); treeSet.add("cherry");
System.out.println(treeSet);       // 排序：[apple, banana, cherry]

Set<String> reverseSet = new TreeSet<>(Comparator.reverseOrder());
reverseSet.add("banana"); reverseSet.add("apple"); reverseSet.add("cherry");
System.out.println(reverseSet);    // [cherry, banana, apple]
```

### 6.3 HashSet 去重原理

HashSet 底层依赖 HashMap 实现：

```java
// HashSet 源码核心：value 是固定的 PRESENT 虚值对象
private static final Object PRESENT = new Object();

public boolean add(E e) {
    return map.put(e, PRESENT) == null;  // 元素作为 key，去重靠 HashMap 的 key 不重复
}
```

**去重流程**：
1. 调用 `hashCode()` 计算哈希值，定位桶位置
2. 桶为空 → 直接存入
3. 桶不为空 → 调用 `equals()` 逐个比较
4. 比较相同 → 拒绝存入（返回 false），不同 → 链表/红黑树存储

> ⚠️ **关键规则**：`equals()` 相同 → `hashCode()` 必须相同。重写 `equals()` 时必须重写 `hashCode()`，否则 HashSet/HashMap 无法正确去重。

---

## 7. Queue与Deque体系

### 7.1 Queue（队列，FIFO）

| 操作 | 抛异常 | 返回特殊值 |
|------|--------|-----------|
| 插入 | `add(e)` | `offer(e)` |
| 移除 | `remove()` | `poll()` |
| 查看 | `element()` | `peek()` |

```java
Queue<String> queue = new LinkedList<>();
queue.offer("a");            // 推荐：添加成功返回 true
queue.add("b");              // 失败抛 IllegalStateException
String head = queue.poll();  // 推荐：移除并返回头，空返回 null
String peek = queue.peek();  // 查看头不移除，空返回 null
```

### 7.2 Deque（双端队列）

```java
Deque<String> deque = new ArrayDeque<>();

// 作为队列（FIFO）
deque.offerLast("a");        // 尾部入
deque.offerLast("b");
String first = deque.pollFirst();  // 头部出 → "a"

// 作为栈（LIFO）
deque.push("x");             // 头部压栈
deque.push("y");
String top = deque.pop();    // 头部出栈 → "y"
String peek = deque.peek();  // 查看栈顶
```

### 7.3 ArrayDeque vs LinkedList

| 对比项 | ArrayDeque | LinkedList |
|--------|-----------|------------|
| 底层结构 | 循环数组 | 双向链表 |
| 性能 | 更快（连续内存，无节点开销） | 略差 |
| 容量限制 | 无界（自动扩容） | 无界 |
| 用途 | 队列/栈场景首选 | 同时需要 List + 队列/栈功能 |

> 💡 **建议**：单纯用作队列或栈时，`ArrayDeque` 性能优于 `LinkedList`。需要索引操作、中间插入等 List 功能时才选 `LinkedList`。

### 7.4 PriorityQueue（优先级队列）

```java
// 最小堆（默认）：每次 poll() 返回最小元素
Queue<Integer> pq = new PriorityQueue<>();
pq.offer(5); pq.offer(1); pq.offer(3);
System.out.println(pq.poll());  // 1

// 最大堆
Queue<Integer> maxPq = new PriorityQueue<>(Comparator.reverseOrder());
maxPq.offer(5); maxPq.offer(1); maxPq.offer(3);
System.out.println(maxPq.poll());  // 5
```

---

## 8. Collections工具类

```java
List<Integer> list = new ArrayList<>(Arrays.asList(3, 1, 4, 1, 5, 9, 2, 6));

// 排序与查找
Collections.sort(list);                              // 自然排序
Collections.sort(list, Comparator.reverseOrder());   // 逆序
Collections.shuffle(list);                           // 随机打乱
Collections.reverse(list);                           // 反转
int idx = Collections.binarySearch(list, 5);         // 二分查找（先排序）

// 极值与批量
int max = Collections.max(list);
int min = Collections.min(list);
Collections.fill(list, 0);                           // 全部填充为 0
Collections.addAll(list, 7, 8, 9);                   // 批量添加
Collections.replaceAll(list, 3, 33);                 // 替换所有

// 不可变集合
List<Integer> unmodifiable = Collections.unmodifiableList(list);
Set<Integer> singleton = Collections.singleton(1);   // 单元素集合

// JDK 9+ 更简洁的不可变集合
List.of("A", "B");       // 不可变 List
Set.of("A", "B");        // 不可变 Set
Map.of("A", 1, "B", 2);  // 不可变 Map
```

---

## 9. 线程安全的List与Set

| 需求场景 | 推荐方案 | 原理 |
|----------|---------|------|
| 读多写少（监听器、配置） | `CopyOnWriteArrayList` | 写时复制（add 时复制整个数组），读不加锁 |
| 写频繁 | `Collections.synchronizedList(new ArrayList<>())` | 同步包装器，所有方法加 synchronized |
| 并发 Set | `ConcurrentHashMap.newKeySet()` | 基于 CHM，线程安全 |
| 并发 Queue | `ConcurrentLinkedQueue` | CAS 无锁实现 |

```java
// CopyOnWriteArrayList：读多写少场景首选
CopyOnWriteArrayList<String> cowList = new CopyOnWriteArrayList<>();
cowList.add("A");      // 内部 synchronized + Arrays.copyOf()
cowList.get(0);        // 无锁，直接 array[index]

// ⚠️ CopyOnWriteArrayList 不适合写频繁场景（每次写 O(n) 复制整个数组）

// 同步包装器（迭代时需外部加锁）
List<String> syncList = Collections.synchronizedList(new ArrayList<>());
synchronized (syncList) {
    for (String s : syncList) { /* 迭代操作 */ }
}
```

> 🎯 **线程安全集合选型原则**：优先使用 `java.util.concurrent` 包中的专用并发集合（如 `ConcurrentHashMap`、`CopyOnWriteArrayList`），避免使用 `Hashtable`、`Vector` 等过时集合。

---

## 10. 时间复杂度对比总表

| 操作 | ArrayList | LinkedList | HashSet | TreeSet | ArrayDeque | PriorityQueue |
|------|-----------|------------|---------|---------|-----------|--------------|
| get(i) | **O(1)** | O(n) | — | — | O(1) | — |
| add(e) 尾部 | O(1) 均摊 | O(1) | O(1) | O(log n) | O(1) 均摊 | O(log n) |
| add(i, e) | O(n) | O(n) | — | — | — | — |
| addFirst | O(n) | **O(1)** | — | — | O(1) 均摊 | — |
| remove(e) | O(n) | O(n) | O(1) | O(log n) | O(n) | O(n) |
| contains(e) | O(n) | O(n) | **O(1)** | O(log n) | O(n) | O(n) |
| 迭代 | O(n) | O(n) | O(n) | O(n) | O(n) | O(n) |
| 内存 | 低 | 高 | 中 | 中 | 低 | 中 |

> 💡 **O(1) 均摊**：单次操作可能是 O(n)（如扩容），但平均到每次操作是 O(1)。

---

## 11. 集合选型决策指南

### 11.1 按场景选型

| 场景 | 推荐实现 | 理由 |
|------|---------|------|
| 频繁索引访问 | ArrayList | 随机访问 O(1) |
| 头部频繁增删 | LinkedList | 头尾操作 O(1) |
| 通用去重 | HashSet | O(1) 增删查 |
| 去重 + 保持插入顺序 | LinkedHashSet | 哈希表 + 链表 |
| 去重 + 自动排序 | TreeSet | 红黑树自动排序 |
| 通用键值对 | HashMap | O(1) 增删改查 |
| 键值对 + 插入顺序 | LinkedHashMap | 可用于 LRU 缓存 |
| 键值对 + 排序 | TreeMap | 红黑树有序 |
| 并发场景 | ConcurrentHashMap | CAS + synchronized |
| 队列 FIFO | ArrayDeque / LinkedList | 性能好 / 功能全 |
| 优先级调度 | PriorityQueue | 二叉堆自动排序 |

### 11.2 选型原则速记

> 查询多→ArrayList | 增删多头尾→LinkedList | 去重→HashSet | 有序去重→LinkedHashSet | 排序去重→TreeSet | 键值对→HashMap | 有序键值对→LinkedHashMap | 排序键值对→TreeMap | 并发安全→ConcurrentHashMap

### 11.3 开发规范

- **用接口声明**：`List<String> list = new ArrayList<>()` 而非实现类声明
- **指定初始容量**：预估大小时指定，避免频繁扩容
- **ArrayList 大循环**：避免中间插入，用 LinkedList 或尾部追加
- **遍历 Map**：用 `entrySet()` 而非 `keySet() + get()`（后者多一次哈希查找）
- **空集合返回**：用 `Collections.emptyList()` 而非 `null`，避免 NPE

---

## 12. 面试高频考点

**Q1: ArrayList 和 LinkedList 的区别及适用场景？**
- ArrayList：动态数组，随机访问 O(1)，中间增删 O(n)，内存紧凑
- LinkedList：双向链表，头部增删 O(1)，随机访问 O(n)，内存开销大
- 选型：随机访问多选 ArrayList，头部频繁增删选 LinkedList

**Q2: ArrayList 扩容机制是怎样的？**
- 懒加载：JDK 8 无参构造初始化为空数组，第一次 add 扩容到 10
- 扩容因子：1.5 倍 `(oldCapacity + (oldCapacity >> 1))`
- 操作：`Arrays.copyOf()` 复制到新数组，均摊 O(1)

**Q3: HashSet 如何保证元素不重复？**
- 底层是 HashMap，元素作为 key，value 统一为 `PRESENT` 常量对象
- 调用 `hashCode()` 定位桶，`equals()` 比较是否相同
- 相同则拒绝存入，不同则链表/红黑树存储

**Q4: HashSet/LinkedHashSet/TreeSet 区别？**
- HashSet：HashMap 实现，无序，O(1)
- LinkedHashSet：LinkedHashMap 实现，保持插入顺序，O(1)
- TreeSet：TreeMap（红黑树）实现，自然/比较器排序，O(log n)

**Q5: Queue 和 Deque 核心方法？**
- Queue：`offer()`/`poll()`/`peek()`（返回特殊值），`add()`/`remove()`/`element()`（抛异常）
- Deque：双端操作 `addFirst/Last`、`pollFirst/Last`、`peekFirst/Last`
- ArrayDeque 作为队列/栈性能优于 LinkedList

**Q6: fail-fast 机制是什么？**
- 迭代器创建时保存 `expectedModCount = modCount`
- 每次 `next()` 检查是否被其他线程修改（`modCount != expectedModCount`）
- 发现修改则抛 `ConcurrentModificationException`
- 是 bug 检测机制，不是线程安全保障

**Q7: Collections 工具类常用方法？**
- 排序：`sort()`、`reverse()`、`shuffle()`
- 查找：`binarySearch()`、`max()`、`min()`
- 不可变：`unmodifiableList()`、`singleton()`、`emptyList()`
- 线程安全包装：`synchronizedList()` 等

**Q8: 线程安全的 List/Set 有哪些？**
- `CopyOnWriteArrayList`：读多写少场景，写时复制
- `Collections.synchronizedList()`：全表锁，迭代需同步
- `ConcurrentHashMap.newKeySet()`：并发 Set 首选
- `ConcurrentLinkedQueue`：CAS 无锁队列

> 🎯 **核心总结**：集合框架的核心是掌握各实现类的底层数据结构与时间复杂度。ArrayList 的扩容机制、LinkedList 的双向链表、HashSet 基于 HashMap 的去重原理、以及如何根据场景选型，是面试和实际开发中的重中之重。

---

**下一步**：掌握 Collection 体系后，继续学习 [Map 体系与 HashMap 源码深度解析](./16-Map体系与HashMap源码深度解析.md)，深入理解数组+链表+红黑树的哈希表实现。
