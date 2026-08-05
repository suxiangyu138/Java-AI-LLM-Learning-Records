# Java 集合框架知识体系总览

> 从 Collection/Map 两大体系的接口设计，到 ArrayList、HashMap、ConcurrentHashMap 的实现与选型——集合框架是 Java 日常开发使用率最高、面试区分度最大的知识域

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [为什么必须学透集合框架](#3-为什么必须学透集合框架)
4. [核心概念速查](#4-核心概念速查)
5. [与周边知识的关系](#5-与周边知识的关系)
6. [学习路线推荐](#6-学习路线推荐)
7. [快速自测 10 题](#7-快速自测-10-题)

---

## 1. 知识体系导图

```text
Java 集合框架知识体系
│
├── 01 Collection 体系与迭代器机制
│   ├── Collection / List / Set / Queue 根接口设计
│   ├── Iterable 与 Iterator：for-each 的底层原理
│   ├── fail-fast 与 modCount 结构修改计数
│   ├── 遍历中安全删除的三种姿势
│   └── JDK 21 SequencedCollection（JEP 431）
│
├── 02 List 详解与选型
│   ├── ArrayList：动态数组、1.5 倍扩容、缓存友好
│   ├── LinkedList：双向链表、节点开销、双端操作
│   ├── Vector / Stack：遗留类的正确姿势
│   ├── CopyOnWriteArrayList：读多写少
│   └── 选型决策：随机访问 vs 插入删除 vs 内存
│
├── 03 Set 详解与选型
│   ├── HashSet：HashMap 的马甲，去重原理
│   ├── LinkedHashSet：保持插入顺序的去重
│   ├── TreeSet：有序去重与范围查询
│   ├── EnumSet：位向量实现的极致优化
│   └── 去重正确性：equals/hashCode 契约
│
├── 04 Queue 与 Deque 详解
│   ├── Queue 语义：FIFO、容量、阻塞
│   ├── ArrayDeque：循环数组，栈/队列首选
│   ├── PriorityQueue：二叉堆、优先级顺序
│   ├── BlockingQueue 家族概览（衔接 JUC）
│   └── 栈的正确实现：ArrayDeque 替代 Stack
│
├── 05 Map 详解与选型
│   ├── HashMap：使用级 API 全景（源码级见 Hash 系统）
│   ├── LinkedHashMap：插入/访问顺序与 LRU 缓存
│   ├── TreeMap：红黑树、有序与范围查询
│   ├── WeakHashMap / IdentityHashMap / EnumMap 冷门四将
│   ├── Hashtable：遗留类为什么不推荐
│   └── null 键值规则总表
│
├── 06 并发集合与安全同步
│   ├── 线程安全策略全景：同步包装 vs 并发容器
│   ├── ConcurrentHashMap：分段演进、CAS+synchronized
│   ├── CopyOnWriteArrayList / CopyOnWriteArraySet
│   ├── ConcurrentSkipListMap/Set：无锁有序
│   ├── BlockingQueue 五大实现
│   └── Collections.synchronizedXxx 的适用边界
│
├── 07 排序与比较器
│   ├── Comparable vs Comparator 设计对比
│   ├── 比较器组合：thenComparing 链式
│   ├── 排序稳定性：归并 vs 双轴快排
│   ├── Comparator 现代写法：lambda/方法引用
│   └── 常见坑：比较器不一致、溢出
│
├── 08 不可变集合与工具类
│   ├── List.of / Set.of / Map.of（JDK 9+）
│   ├── copyOf 与 不可变视图的差异
│   ├── Collections 工具：排序/查找/同步包装/不可变
│   ├── Arrays 工具：asList/排序/二分/拷贝
│   └── 不可变集合与空集合的边界行为
│
└── 09 面试高频考点与总结
    ├── 复杂度总表与选择决策树
    ├── 必背考点：扩容/负载因子/红黑树/fail-fast
    ├── 高频陷阱题
    └── 记忆口诀与进阶导航
```

---

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 | 文件 |
|:---:|------|---------|---------|------|
| 01 | Collection 体系与迭代器机制 | 根接口、Iterator、fail-fast、SequencedCollection | 初中级必须掌握 | [01-Collection体系与迭代器机制](./01-Collection体系与迭代器机制.md) |
| 02 | List 详解与选型 | ArrayList/LinkedList/Vector/CopyOnWriteArrayList | 初中级必须掌握 | [02-List详解与选型](./02-List详解与选型.md) |
| 03 | Set 详解与选型 | HashSet/LinkedHashSet/TreeSet/EnumSet | 初中级必须掌握 | [03-Set详解与选型](./03-Set详解与选型.md) |
| 04 | Queue 与 Deque 详解 | ArrayDeque/PriorityQueue/BlockingQueue | 初中级 → 中高级 | [04-Queue与Deque详解](./04-Queue与Deque详解.md) |
| 05 | Map 详解与选型 | HashMap/LinkedHashMap/TreeMap/冷门 Map | 初中级必须掌握 | [05-Map详解与选型](./05-Map详解与选型.md) |
| 06 | 并发集合与安全同步 | CHM/CopyOnWrite/跳表/BlockingQueue | 中高级 | [06-并发集合与安全同步](./06-并发集合与安全同步.md) |
| 07 | 排序与比较器 | Comparable/Comparator/排序稳定性 | 中级 | [07-排序与比较器](./07-排序与比较器.md) |
| 08 | 不可变集合与工具类 | List.of/Collections/Arrays | 中级 | [08-不可变集合与工具类](./08-不可变集合与工具类.md) |
| 09 | 面试高频考点与总结 | 复杂度表、决策树、陷阱题 | 面试冲刺 | [09-面试高频考点与总结](./09-面试高频考点与总结.md) |

---

## 3. 为什么必须学透集合框架

1. **使用率第一的 API 域**：任何业务系统的数据承载、缓存、去重、排序、队列都建立在集合之上；`HashMap` 的 `computeIfAbsent`、`merge` 等现代 API 直接改变代码组织方式。
2. **面试必考且深度极深**：扩容机制、负载因子 0.75 的由来、链表转红黑树阈值 8、fail-fast 原理、ConcurrentHashMap 的锁粒度演进——这些是区分"背题者"与"懂原理者"的标准题。
3. **数据结构在 JDK 中的活教材**：动态数组、双向链表、哈希表、红黑树、二叉堆、跳表——集合框架是六大基础数据结构最权威的工业级实现，学完集合等于学完数据结构实战。
4. **并发正确性的第一现场**：普通集合的线程安全问题、`ConcurrentModificationException`、并发容器的弱一致性迭代器——理解并发容器的设计，是理解 JUC 与高并发系统的前提。
5. **持续演进的活 API**：从 JDK 8 的 HashMap 重写、JDK 9 的不可变集合、到 JDK 21 的 SequencedCollection（JEP 431），集合框架仍在演进——了解版本窗口是资深开发的标志。

---

## 4. 核心概念速查

### 4.1 两大体系与核心接口

| 体系 | 根接口 | 特点 | 主要子接口 |
|------|--------|------|-----------|
| 单列集合 | `Collection<E>` | 存储单个元素 | `List`（有序可重复）、`Set`（去重）、`Queue`（队列）、`SequencedCollection`（JDK 21+） |
| 双列集合 | `Map<K,V>` | 存储键值对，键唯一 | `SortedMap`、`NavigableMap`、`SequencedMap`（JDK 21+） |

### 4.2 核心实现类速查

| 类 | 底层结构 | 有序性 | 线程安全 | 复杂度（核心操作） |
|----|---------|:------:|:-------:|-------------------|
| ArrayList | 动态数组 | 插入序 | ❌ | get O(1)、尾插 O(1) 均摊 |
| LinkedList | 双向链表 | 插入序 | ❌ | 头尾操作 O(1)、随机访问 O(n) |
| HashSet | HashMap | ❌ 无序 | ❌ | 增删查 O(1) |
| LinkedHashSet | HashMap + 链表 | 插入序 | ❌ | O(1) |
| TreeSet | 红黑树 | 自然序/比较器序 | ❌ | O(log n) |
| ArrayDeque | 循环数组 | 双端序 | ❌ | 双端 O(1) |
| PriorityQueue | 二叉堆 | 优先级序 | ❌ | 入队 O(log n)、peek O(1) |
| HashMap | 数组+链表+红黑树 | ❌ 无序 | ❌ | O(1) |
| LinkedHashMap | HashMap + 双向链表 | 插入/访问序 | ❌ | O(1) |
| TreeMap | 红黑树 | 键序 | ❌ | O(log n) |
| ConcurrentHashMap | 数组+链表+红黑树（分段演进） | ❌ | ✅ | O(1) |

### 4.3 关键机制速查

| 机制 | 一句话 | 出现位置 |
|------|--------|---------|
| fail-fast | 迭代中结构性修改抛 `ConcurrentModificationException` | 普通集合迭代器 |
| modCount | 结构修改计数，迭代器快照比对其值 | ArrayList/HashMap 等 |
| 弱一致性迭代 | 迭代不抛异常，可能看不到最新修改 | 并发容器 |
| 负载因子 0.75 | 空间与时间的平衡点，扩容阈值 = 容量 × 0.75 | HashMap/HashSet |
| 树化阈值 8/6 | 链表长度 ≥8 且容量 ≥64 转红黑树，≤6 转回 | HashMap（JDK 8+） |
| 不可变集合 | 拒绝任何修改，抛 `UnsupportedOperationException` | `List.of` 等（JDK 9+） |

---

## 5. 与周边知识的关系

```text
                    ┌── Java有关Hash的一切 —— HashMap/CHM 源码级深挖（本体系交叉引用）
                    ├── 泛型 —— 集合的类型安全（List<String> 的菱形语法）
Java 集合框架 ──────┼── 并发（JUC）—— BlockingQueue、并发容器的进阶
                    ├── Java流Stream —— Stream 基于集合产生，声明式处理
                    ├── Java面向对象 —— equals/hashCode 契约决定 Set/Map 正确性
                    └── 数据结构 —— 数组/链表/哈希/红黑树/堆/跳表的工业级实现
```

- **往下走（原理）**：`HashMap` 的源码级剖析（红黑树转换、扩容 rehash、哈希碰撞攻击）在 `Java有关Hash的一切/` 知识系统，本体系只做使用级与选型级覆盖。
- **往旁走（类型）**：泛型让集合在编译期就约束元素类型；`equals`/`hashCode` 契约错误会让 HashSet/HashMap 行为诡异。
- **往高走（工程）**：集合选型是性能优化的第一站（预估容量、正确容器、并发选择），也是 Spring/MyBatis 等框架内部大量使用的数据结构。

---

## 6. 学习路线推荐

**路线一：入门夯实（2~3 天，对应模块 01-05）**
Collection 体系 → List → Set → Queue → Map，每个类手写增删改查验证；重点吃透 ArrayList 扩容、HashMap 结构与 null 键规则、遍历时删除的正确姿势。

**路线二：进阶深化（1 周，对应模块 06-08）**
并发集合（重点 ConcurrentHashMap 的设计演进）→ 排序与比较器 → 不可变集合；用 LinkedHashMap 手写一个 LRU 缓存，用 PriorityQueue 实现一个 TopK。

**路线三：面试冲刺（对应模块 09 + Hash 系统）**
背复杂度总表 → 过一遍必背考点 → 自测陷阱题；然后进入 `Java有关Hash的一切/` 啃 HashMap/CHM 源码级内容。

> 🎯 **核心要点**：集合框架的"懂"分三层——会用（API 层）、懂结构（数据结构层）、明取舍（选型与并发层）。本体系把三层一次讲透，源码深挖留给 Hash 系统。

---

## 7. 快速自测 10 题

1. `for (String s : list)` 的底层是怎么执行的？删除元素为什么会抛 `ConcurrentModificationException`？
2. ArrayList 扩容为什么是 1.5 倍而不是 2 倍？指定初始容量有什么好处？
3. LinkedList 的插入真的比 ArrayList 快吗？中间插入呢？
4. HashSet 是怎么做到去重的？为什么重写 equals 必须重写 hashCode？
5. ArrayDeque 为什么比 LinkedList 更适合当队列和栈？Stack 类为什么被弃用？
6. HashMap 的负载因子为什么是 0.75？链表多长会转红黑树？为什么要转？
7. TreeMap 的 key 可以是 null 吗？HashMap 呢？ConcurrentHashMap 呢？
8. `Collections.synchronizedMap(map)` 和 ConcurrentHashMap 有什么区别？哪个更好？
9. `List.of("a", "b")` 和 `Arrays.asList("a", "b")` 有什么区别？
10. 对一个 `List<String>` 按长度排序再按字典序排序，怎么写？`TreeSet` 去重时比较器怎么判断相等？

> 💡 答不出的题目对应上方模块导航中的文件，按图索骥复习即可。

---

**下一模块**：[01-Collection体系与迭代器机制](./01-Collection体系与迭代器机制.md)
