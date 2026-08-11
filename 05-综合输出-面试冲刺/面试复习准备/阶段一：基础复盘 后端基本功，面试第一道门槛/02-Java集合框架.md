# 02 - Java 集合框架

> 🎯 集合框架是 Java 面试的"必考大题"——HashMap 源码几乎场场出现，ConcurrentHashMap 是并发考点的最佳载体。本模块只复盘大厂真题：HashMap put 全流程、扩容机制、为什么 2 的幂、ConcurrentHashMap 1.7→1.8 演进、fail-fast 机制

## 📚 目录

1. [集合体系全景](#1-集合体系全景)
2. [List 三兄弟与选型](#2-list-三兄弟与选型)
3. [HashMap 源码深潜](#3-hashmap-源码深潜)
4. [ConcurrentHashMap：1.7 到 1.8 的演进](#4-concurrenthashmap17-到-18-的演进)
5. [fail-fast 与 fail-safe 机制](#5-fail-fast-与-fail-safe-机制)
6. [Set 与排序结构](#6-set-与排序结构)
7. [集合选型决策](#7-集合选型决策)
8. [高频面试题与追问预案](#8-高频面试题与追问预案)

## 1. 集合体系全景

一句话背熟总纲：**集合分两大分支——Collection 存单值（List/Set/Queue），Map 存键值对**。Collection 之下，List 有序可重复（ArrayList/LinkedList/Vector），Set 无序不可重复（HashSet/LinkedHashSet/TreeSet），Queue 队列（ArrayDeque/PriorityQueue/阻塞队列）。Map 三巨头：HashMap（无序）、LinkedHashMap（按插入或访问序）、TreeMap（按 key 排序）。

记忆锚点：Hash 系列无序、Linked 系列保序、Tree 系列排序、Concurrent 系列线程安全。面试先说总纲再深入，显体系感——"从集合体系的演进看 Java 设计"（1.2 的同步集合 → 1.5 的 JUC → 1.8 的优化）也是进阶加分表述。

## 2. List 三兄弟与选型

**ArrayList 是数组实现**：随机访问 O(1)，插入删除尾部 O(1)、中间 O(n)（System.arraycopy 移动元素）；初始容量 10，扩容 1.5 倍（`oldCapacity + (oldCapacity >> 1)`），扩容是创建新数组 + 复制，成本高。**LinkedList 是双向链表实现**：插入删除 O(1)（前提是已定位到节点），随机访问 O(n)（遍历一半）；节点对象占用内存更大（多两个指针）。**Vector 线程安全（方法级 synchronized）但性能差**，官方已不推荐，需要线程安全列表用 `Collections.synchronizedList` 或 CopyOnWriteArrayList。

高频辨析题：**ArrayList 和 LinkedList 谁更适合频繁插入**——答案不是无脑 LinkedList：如果插入发生在尾部，ArrayList 更快（数组尾部追加无移动，链表还要维护节点）；只有"定位到中间节点反复插删"才 LinkedList 占优。面试加分句："数据量小且频繁随机访问用 ArrayList，千万级尾部追加也是 ArrayList"。

## 3. HashMap 源码深潜

HashMap 是面试重灾区，按"结构 → put → 扩容 → 为什么"四步背：

**结构**（JDK 8+）：数组 + 链表 + 红黑树。数组默认初始容量 16，负载因子 0.75（空间与时间权衡：太小浪费空间、太大碰撞加剧），链表长度 ≥ 8 且数组长度 ≥ 64 转红黑树（树化阈值 8，退化阈值 6——留 1 的缓冲避免频繁震荡），链表长度 < 6 转回链表。扰动函数 `hash = key.hashCode() ^ (hashCode >>> 16)`，让高位参与低位运算，减少碰撞。

**put 流程**：计算 hash → `(n-1) & hash` 定位桶下标（等价取模，且 n 为 2 的幂时性能最优）→ 桶空直接插入 → 桶非空：key 相同覆盖，否则尾插法挂链表，链表超阈值树化。**JDK 8 改尾插法**是为了解决 JDK 7 头插法在并发扩容时的环形链表死循环问题。

**扩容机制**：元素数 > `容量 × 负载因子` 时扩容为 2 倍。JDK 8 的优化是**高低位拆分**：元素在新数组的下标只有两种可能——原位，或原位 + 旧容量（由新增的最高位决定），因此不用重新计算 hash，只需判断 `(e.hash & oldCap) == 0` 留原位还是 +oldCap。扩容时整体搬迁是 O(n)，这是 HashMap 最大的性能隐患——**预估容量给足初始值**是工程铁律（`new HashMap<>(expectedSize / 0.75f + 1)`）。

**为什么容量是 2 的幂**：一是 `(n-1) & hash` 替代 `%` 取模更快；二是扩容时高低位拆分只需看一个 bit，配合上面的 O(1) 迁移。若自定义初始容量不是 2 的幂，构造时会 `tableSizeFor` 向上取整到最近的 2 的幂。

## 4. ConcurrentHashMap：1.7 到 1.8 的演进

演进史是必背：**1.7 分段锁**：Segment 数组 + HashEntry 数组，锁粒度是段（默认 16 段），定位两次 hash；段数固定，扩容只扩段内。**1.8 全面重构**：放弃分段，改为**数组 + 链表/红黑树 + CAS + synchronized**——put 时桶为空用 CAS 直接插入，桶非空对桶头节点加 synchronized 锁（锁粒度从"段"细到"单桶"），读操作基本无锁（volatile 修饰节点与数组）。

追问三连：为什么 1.8 锁粒度更细性能更好（并发度从 16 提升到桶数）；为什么用 synchronized 而不是 ReentrantLock（JDK 8 synchronized 经过锁升级优化后性能不输，且语言原生、代码更简、可读性好）；size() 怎么统计（baseCount + CounterCell 累加，JDK 8 用 LongAdder 思路，热点分散）。注意：ConcurrentHashMap 的 key/value **都不允许 null**——因为无法区分"值为 null"和"未放入"，HashMap 允许 null 是为了单线程下语义简洁。

## 5. fail-fast 与 fail-safe 机制

**fail-fast（快速失败）**：迭代器遍历时用 `modCount` 记录修改次数，每次 next() 校验，一旦发现并发修改（modCount 变化）立即抛 ConcurrentModificationException。注意：单线程下边遍历边 `list.remove()` 同样触发——必须用 `Iterator.remove()`。**fail-safe（安全失败）**：CopyOnWriteArrayList 等遍历的是**快照副本**，修改不影响迭代，不抛异常——但迭代读到的是旧数据，弱一致性。

工程结论：**遍历时禁止直接增删，用迭代器或收集后统一处理**；多线程读写用 CopyOnWriteArrayList（读多写少场景），用 ConcurrentHashMap 替代 Hashtable 与 Collections.synchronizedMap（后者全表锁，并发度退化到 1）。

## 6. Set 与排序结构

HashSet 底层就是 HashMap（value 用固定 Object 占位），去重依赖 hashCode + equals——**重写 equals 必须重写 hashCode** 的经典场景：只重写 equals 会导致相同对象 hashCode 不同，Set 判重失效、HashMap 查不到已放入的 key。LinkedHashSet 保持插入序，TreeSet 按比较器排序（元素需实现 Comparable 或传入 Comparator）。

TreeMap/TreeSet 底层是红黑树，key 有序，支持范围查询（subMap/floorKey 等）——适合排行榜、范围统计场景。PriorityQueue 是二叉小顶堆，默认最小堆，`offer`/`poll` O(log n)——TopK、定时任务（DelayQueue）场景首选。

## 7. 集合选型决策

选型口诀一句话："**读多写少 CopyOnWrite，并发写多 Concurrent，单线程看语义（有序/去重/排序），初始容量算清楚**"。展开说：要线程安全 + 高并发读写 → ConcurrentHashMap（列表对应 CopyOnWriteArrayList）；只要顺序 → ArrayList；频繁中间插删 → LinkedList；去重 → HashSet；保插入序去重 → LinkedHashSet；排序 → TreeSet/TreeMap；队列 → ArrayDeque（非阻塞）/LinkedBlockingQueue（阻塞）。HashMap 高并发会丢数据、链表成环（1.7）、扩容脏读——**生产环境并发 Map 无脑 ConcurrentHashMap**。

还有一个高频的"读源码"追问：**HashMap 的迭代顺序不稳定**——所以需要确定遍历顺序的缓存、排行榜场景，要么用 LinkedHashMap（插入序或访问序），要么用 TreeMap（key 排序），千万不能依赖 HashMap 的遍历顺序写业务（线上踩过"遍历顺序变了导致批量处理顺序漂移"的坑）。另外**初始容量设置公式**（期望元素数 ÷ 0.75 + 1）要会算：要存 100 个元素就 `new HashMap<>(134)`，避免扩容一次白白复制一轮。

## 8. 高频面试题与追问预案

### 基础概念档

| # | 题目 | 答题要点 |
|---|------|----------|
| 1 | HashMap 底层数据结构？ | 数组+链表+红黑树 / 树化条件 8 且数组 64 / 退化阈值 6 |
| 2 | HashMap 为什么容量 2 的幂？ | 位运算替代取模 / 扩容高低位拆分 O(1) |
| 3 | 重写 equals 为什么要重写 hashCode？ | Set 判重 / HashMap 定位 key 靠 hashCode 先定位再 equals |

### 工程实践档

| # | 题目 | 答题要点 |
|---|------|----------|
| 4 | 遇到过 ConcurrentModificationException？ | 遍历中删除 / 迭代器 remove 或收集后批量删 / 并发场景用 CopyOnWrite |
| 5 | 大 Map 频繁扩容卡顿？ | 扩容 O(n) 复制 / 预估容量设置初始值 / 或分片 |
| 6 | HashMap 和 ConcurrentHashMap 选哪个？ | 单线程 HashMap / 多线程无脑 ConcurrentHashMap |

### 架构设计档

| # | 题目 | 答题要点 + 追问点 |
|---|------|------------------|
| 7 | 设计一个 LRU 缓存？ | LinkedHashMap 覆写 removeEldestEntry / 或双向链表+HashMap 手写；追问"并发版怎么做"（加锁 + 分段） |
| 8 | 红黑树 vs 跳表？ | 红黑树写复杂读稳定 / 跳表实现简单支持范围查询（Redis zset）；追问"为什么 Redis 用跳表不用红黑树" |
| 9 | 集合批量删除千万数据？ | 分页删除 + 小事务 / 或迭代器 remove；追问"为什么不能一次性全删" |

### 追问速查

HashMap put 流程让手写一遍（口头描述即可）；树化阈值为什么是 8（泊松分布，概率极低）；1.8 为什么用 synchronized（锁升级后性能不输 ReentrantLock + 代码简洁）；ConcurrentHashMap 为什么不能存 null（无法区分 null 与不存在，且源码强制校验）；HashMap 允许 null 吗（允许，key/value 都可——单线程语义下 null 有意义，CHM 不行是并发歧义）。

> 🎯 **核心要点**：集合题的通吃公式 = **结构 → 流程 → 权衡 → 演进**。以 HashMap 为例：结构（数组+链表+红黑树）→ put 流程（hash 定位/尾插/树化）→ 权衡（0.75/8/64/2 的幂都是权衡结果）→ 演进（1.7 头插死循环 → 1.8 尾插+树化；CHM 分段锁 → CAS+synchronized）。按这条线答，任何集合题都逃不出这个框架。

---

**下一模块**：[03-Java并发编程](03-Java并发编程.md) / **返回总览**：[00-阶段一基础复盘总览](00-阶段一基础复盘总览.md)
