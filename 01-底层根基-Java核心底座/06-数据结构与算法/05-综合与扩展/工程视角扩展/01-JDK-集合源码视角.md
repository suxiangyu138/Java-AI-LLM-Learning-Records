# JDK 集合源码视角
> 数据结构在 JDK 里的「工程化答案」：ArrayList 扩容、HashMap 树化、LinkedHashMap 双重链表、PriorityQueue 堆化、TimSort、SequencedCollection（JDK 21+）——每个实现都回答了「理论结构怎么落地」

## 📚 目录
1. [ArrayList：动态数组的工程实现](#1-arraylist动态数组的工程实现)
2. [HashMap：哈希表的工程实现](#2-hashmap哈希表的工程实现)
3. [LinkedHashMap 与 SequencedCollection（JDK 21+）](#3-linkedhashmap-与-sequencedcollectionjdk-21)
4. [LinkedList / ArrayDeque / PriorityQueue](#4-linkedlist--arraydeque--priorityqueue)
5. [排序与查找的 JDK 实现](#5-排序与查找的-jdk-实现)
6. [JDK 集合面试问题清单](#6-jdk-集合面试问题清单)

---

## 1. ArrayList：动态数组的工程实现

| 设计点 | 实现 | 为什么 |
|------|------|------|
| 默认容量 | 10（懒初始化） | 首次 add 才分配，节省空集合内存 |
| 扩容策略 | 1.5 倍（`old + old >> 1`） | 摊还 O(1)，比 2 倍省内存 |
| 扩容时机 | `size == elementData.length` | 先扩容再 add |
| 缩容 | 不自动（`trimToSize` 手动） | 避免频繁搬移 |
| 删除 | 左移元素 O(n) | 数组结构本质 |
| 均摊分析 | n 次 add = O(n) 总 | 扩容搬移摊薄 |

```text
扩容过程：10 → 15 → 22 → 33 → 49 → ...（1.5 倍）
⚠️ 预分配：确定大小时用 ArrayList(n) 构造，省去多次扩容搬移
```

> 🎯 面试必答：**ArrayList 均摊 O(1) 的证明**——扩容搬移总代价 = 10 + 15 + 22 + ... ≤ 2n，摊到每次 add 上是 O(1)。

### 1.1 扩容的均摊证明（面试口述版）

```text
容量按 1.5 倍增长：总搬移次数 ≈ 10 + 15 + 22 + ... 
等比求和 < 2 × 最终容量 ≈ 2n
n 次 add 总代价 = n 次赋值 + O(n) 搬移 = O(n)
单次均摊 = O(1) ✅
⚠️ 注意：单次 add 最坏仍是 O(n)（扩容那一次）——均摊 ≠ 最坏
```

---

## 2. HashMap：哈希表的工程实现

### 2.1 核心设计参数

| 参数 | 值 | 含义 |
|------|:---:|------|
| 默认容量 | 16 | 2 的幂（`(n-1) & hash` 取模） |
| 负载因子 | 0.75 | 空间与冲突的平衡点 |
| 树化阈值 | 8 | 链表长度 ≥ 8 转红黑树 |
| 退链表阈值 | 6 | 树节点 ≤ 6 转链表 |
| 树化容量下限 | 64 | 容量 < 64 先扩容不树化 |

### 2.2 源码关键点（JDK 8+）

| 点 | 细节 |
|------|------|
| 哈希扰动 | `(h = key.hashCode()) ^ (h >>> 16)` 高位参与运算 |
| 链表 → 红黑树 | 泊松分布论证：负载 0.75 下链长 8 概率 < 千万分之一 |
| 扩容 rehash | 高低位拆分：`(e.hash & oldCap) == 0` 留原位，否则 +oldCap |
| 尾插法 | JDK 7 头插 → JDK 8 尾插（消除并发扩容死链） |
| 懒加载 | 首次 put 才 `resize()` 建表 |
| null 键 | 特殊处理放入 `table[0]` |

### 2.3 并发正确姿势

```text
HashMap         → 线程不安全（数据覆盖、环链风险）
Hashtable       → 全方法 synchronized（性能差，已过时）
ConcurrentHashMap → 分段/桶级锁 + CAS（JDK 8 桶级 synchronized + CAS）
Collections.synchronizedMap → 粗粒度包装（简单场景）
```

### 2.4 哈希扰动的作用（面试追问）

```text
问题：为什么 hashCode 后还要 ^ (h >>> 16)？
原因：HashMap 容量小时（如 16），只有低 4 位参与取桶
      → 高 16 位信息全部浪费 → 冲突概率上升
修复：h ^ (h >>> 16)，把高位混入低位 → 分布更均匀
例：两个对象 hashCode 只差在高位 → 扰动前同桶，扰动后不同桶
```

---

## 3. LinkedHashMap 与 SequencedCollection（JDK 21+）

### 3.1 LinkedHashMap：哈希 + 双向链表

| 特性 | 说明 |
|------|------|
| 结构 | HashMap + 双向链表维护顺序 |
| accessOrder | false = 插入序；true = 访问序（LRU 基础） |
| removeEldestEntry | 重写可做 LRU 缓存（`size() > cap` 时淘汰） |
| 遍历 | 按序 O(n)，插入/查询 O(1) |

### 3.2 SequencedCollection（JEP 431，JDK 21，JDK 25 标配）

| 接口 | 关键方法 | 实现类 |
|------|------|------|
| SequencedCollection | `getFirst()/getLast()/addFirst()/addLast()/removeFirst()/removeLast()/reversed()` | List、Deque |
| SequencedSet | 继承 SequencedCollection，`reversed()` 协变 | LinkedHashSet、SortedSet |
| SequencedMap | `firstEntry()/lastEntry()/putFirst()/putLast()/sequencedKeySet()` | LinkedHashMap、SortedMap |

| 工程要点 | 说明 |
|------|------|
| 解决的问题 | 此前有序集合无统一首尾 API（`list.get(0)` vs `deque.peekFirst()` 混乱） |
| reversed() | **逆序视图**，不复制，修改双向影响 |
| LinkedHashSet 增强 | `addFirst()/addLast()` 可将已存在元素**移动到对应位置** |
| TreeSet 限制 | 基于排序的集合不支持 `addFirst()/addLast()` |
| API 设计建议 | 方法签名用 `SequencedCollection` 声明，把「顺序契约」写进接口 |

```java
// 示例：LinkedHashMap 作为 SequencedMap 的首尾访问（JDK 21+）
LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
map.put("a", 1); map.put("b", 2);
Map.Entry<String, Integer> first = map.firstEntry();  // "a"→1
Map.Entry<String, Integer> last  = map.lastEntry();   // "b"→2
SequencedMap<String, Integer> rev = map.reversed();   // 逆序视图
```

> 🎯 面试必答：**JDK 21 集合框架最大变革 = SequencedCollection 家族**——统一了有序集合的首尾操作 API；2025 年（JDK 25）已成标准实践。HashMap 仍不保证迭代顺序，需要顺序用 LinkedHashMap（SequencedMap）。

---

## 4. LinkedList / ArrayDeque / PriorityQueue

### 4.1 LinkedList vs ArrayDeque（栈/队列选型）

| 维度 | LinkedList | ArrayDeque |
|------|:---:|:---:|
| 底层 | 双向链表 | 循环数组 |
| 内存 | 节点 + 指针开销大 | 连续，紧凑 |
| 随机访问 | O(n) | 不支持（Deque 语义） |
| 性能 | 节点分配频繁 | **快（缓存友好）** |
| 结论 | 几乎只用做 Deque 的替代品 | **JDK 推荐栈/队列默认实现** |

> ⚠️ 工程结论：**Java 里「栈」用 `ArrayDeque` 而不是 `Stack`（Vector 遗留）**；「队列」用 `ArrayDeque` 或 `LinkedList`。

### 4.2 PriorityQueue：堆的工程实现

| 设计点 | 说明 |
|------|------|
| 底层 | Object[] 数组模拟完全二叉树 |
| 堆化 | `siftDown` 从 `(n/2)-1` 向上做 O(n) 建堆 |
| 入队 | `siftUp` 上浮 O(log n) |
| 出队 | 堆顶出 + 尾元素下沉 O(log n) |
| 初始容量 | 11，扩容 2 倍 |
| 迭代顺序 | 非有序（只有 poll 保证堆序） |

```java
// 大顶堆正确写法（防溢出）
PriorityQueue<Integer> maxHeap = new PriorityQueue<>((a, b) -> Integer.compare(b, a));
// ⚠️ 不能写 (a, b) -> b - a：a=MIN, b=MAX 时溢出为负，堆序反转
```

### 4.3 为什么 O(n) 建堆（追问）

```text
自底向上 siftDown：越下层节点越多但下滤深度越小
总代价 = Σ (层节点数 × 下滤深度) = O(n)
反例：自上而下 siftUp 逐个插入是 O(n log n)
结论：建堆用自底向上 O(n)，插入用 siftUp O(log n)
```

---

## 5. 排序与查找的 JDK 实现

### 5.1 Arrays.sort 的工程分层

| 输入 | 算法 | 理由 |
|------|------|------|
| 基本类型数组 | DualPivotQuickSort（双轴快排） | 原地、缓存友好；插入排序兜底（≤ 47） |
| 对象数组 | TimSort（归并 + 插入混合） | **稳定**（对象排序要求稳定） |
| 大数组 | 递归转迭代、阈值切换 | 栈安全 + 常数最优 |

### 5.2 二分查找的 JDK 细节

```text
Arrays.binarySearch：
  - 找到返回下标；未找到返回 -(插入点) - 1
  - 负数语义 = 返回值定位插入位置（背熟，常考）
Collections.binarySearch：只对实现 RandomAccess 的 List 做索引二分
  - LinkedList 用迭代器二分 → O(n)，陷阱题
```

### 5.3 查找结构对比（JDK 视角）

| 场景 | JDK 选择 | 复杂度 |
|------|------|:---:|
| 通用键值 | HashMap | O(1) |
| 有序键值 | TreeMap（红黑树） | O(log n) |
| 去重且有序 | TreeSet | O(log n) |
| 快速极值 | PriorityQueue | O(1) 查极值 |
| 插入序键值 | LinkedHashMap（SequencedMap） | O(1) |

---

## 6. JDK 集合面试问题清单

| 问题 | 一句话答案 |
|------|------|
| ArrayList 为什么 1.5 倍扩容？ | 摊还 O(1) 且比 2 倍省内存 |
| HashMap 为什么树化阈值 8？ | 泊松分布：链长 8 概率 < 千万分之一 |
| HashMap 为什么容量 2 的幂？ | 位与代替取模 + 扩容高低位拆分 |
| 为什么 ConcurrentHashMap 快？ | 桶级锁 + CAS，锁粒度远小于 Hashtable |
| 栈/队列用哪个类？ | ArrayDeque（不是 Stack/LinkedList） |
| PriorityQueue 是稳定的吗？ | 不是——poll 只保证堆序 |
| SequencedCollection 是什么？ | JDK 21（JEP 431）统一有序集合首尾 API |

> 🎯 **核心要点**：JDK 源码是「数据结构理论 × 工程权衡」的最佳教材——**ArrayList 1.5 倍扩容、HashMap 0.75 负载 + 树化阈值、TimSort 稳定性、ArrayDeque 取代 LinkedList、SequencedCollection 统一顺序 API**，五个记忆点覆盖集合框架 80% 的面试追问。每个实现都回答同一个问题：理论结构在「内存、缓存、并发、稳定性」四重约束下怎么改。

---

**下一模块**：[02-中间件与系统设计算法](02-中间件与系统设计算法.md) | **返回总览**：[00-工程视角扩展总览](00-工程视角扩展总览.md)
