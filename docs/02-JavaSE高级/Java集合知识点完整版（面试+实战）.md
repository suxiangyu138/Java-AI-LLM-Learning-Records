# Java 集合知识点完整版（面试 + 实战）

> **文档定位**：Java 后端企业级技术文档 | Java 集合框架  
> **核心包**：`java.util`、`java.util.concurrent`  
> **前置基础**：Java 基础语法、泛型、数据结构基础

---

## 一、核心概念

### 1.1 Java 集合框架概述

Java 集合是存储、管理一组数据的核心工具，核心作用是 **替代数组**（解决数组固定长度、类型单一的弊端）。顶层接口分为两大体系：

```
Collection（存储单个元素）
├── List    —— 有序、可重复、有索引
├── Set     —— 无序、不可重复
└── Queue   —— 队列（FIFO）

Map（存储键值对，key 唯一，value 可重复）
├── HashMap
├── LinkedHashMap
├── TreeMap
└── Hashtable（过时）
```

> Collection 和 Map 二者 **无继承关系**，共同构成 Java 集合的核心骨架。

### 1.2 核心注意点

| 注意点 | 说明 |
|--------|------|
| 只能存储引用类型 | 不能直接存 `int`、`char`，必须使用包装类 `Integer`、`Character` |
| 位于 `java.util` 包 | 使用时需 `import java.util.*` |
| "有序/无序" 的定义 | 有序指存储顺序 = 遍历顺序（非自动排序）；无序指存储顺序 ≠ 遍历顺序 |

---

## 二、底层原理

### 2.1 ArrayList 底层结构

- **数据结构**：动态数组（`Object[]`）
- **初始容量**：10
- **扩容机制**：容量不足时扩容为原来的 **1.5 倍**（`oldCapacity + (oldCapacity >> 1)`）
- **查询复杂度**：O(1)（通过索引直接访问）
- **增删复杂度**：O(n)（需要 System.arraycopy 移动元素）

### 2.2 LinkedList 底层结构

- **数据结构**：双向链表（每个节点持有 prev/next 引用）
- **查询复杂度**：O(n)（需遍历链表）
- **增删复杂度**：O(1)（仅修改节点引用）

### 2.3 HashMap 底层结构（JDK 8+）

```
HashMap = 数组（Bucket） + 链表 + 红黑树

  Bucket[0] → Node → Node → ...    （链表，解决哈希冲突）
  Bucket[1] → TreeNode ↔ TreeNode  （红黑树，链表长度 ≥ 8 且数组容量 ≥ 64 时转换）
  ...
  Bucket[n]

核心参数：
  初始容量：16
  负载因子：0.75（元素数量达到 容量 × 0.75 时触发扩容）
  扩容倍数：原容量的 2 倍
  树化阈值：链表长度 ≥ 8 且数组长度 ≥ 64
  退化阈值：树节点 ≤ 6 时转回链表
```

### 2.4 HashSet 去重原理

HashSet 底层依赖 **HashMap** 实现（HashSet 的 value 是一个固定的 `PRESENT` 对象）：

1. 调用元素的 `hashCode()` 计算哈希值，定位桶位置
2. 若该位置无元素，直接存入
3. 若该位置有元素，调用 `equals()` 比较——相等则去重（不存入），不等则链表/红黑树存储

> **关键规则**：`hashCode()` 相同 → 不一定相同，需 `equals()` 判断；`equals()` 相同 → `hashCode()` 必须相同

### 2.5 TreeSet / TreeMap 排序原理

- **数据结构**：红黑树（自平衡二叉搜索树）
- **排序方式**：元素必须实现 `Comparable` 接口（自然排序），或构造时传入 `Comparator`（自定义排序）
- **key 不能为 null**（TreeMap/TreeSet）

---

## 三、代码实现

### 3.1 List 接口核心实现类

#### ArrayList（最常用）

```java
/**
 * ArrayList 使用示例 —— 底层动态数组，查询快 O(1)，增删慢 O(n)。
 */
List<String> list = new ArrayList<>();
list.add("Java");
list.add("Spring");
list.add("MyBatis");
String item = list.get(0);          // "Java"（索引访问）
list.remove(1);                      // 删除 "Spring"（需移动后续元素）
list.set(0, "Kotlin");              // 修改元素
```

#### LinkedList

```java
/**
 * LinkedList 使用示例 —— 底层双向链表，查询慢 O(n)，增删快 O(1)。
 * 同时实现了 List 和 Deque 接口，可作队列/栈使用。
 */
LinkedList<String> linkedList = new LinkedList<>();
linkedList.add("A");
linkedList.addFirst("First");       // 头部添加
linkedList.addLast("Last");         // 尾部添加
String first = linkedList.getFirst(); // 获取头部
linkedList.removeFirst();           // 移除头部
```

### 3.2 Set 接口核心实现类

#### HashSet（最常用）

```java
/**
 * HashSet 使用示例 —— 无序、不可重复，查询/增删 O(1)。
 * 存储自定义对象必须重写 equals() 和 hashCode()。
 */
Set<String> hashSet = new HashSet<>();
hashSet.add("apple");
hashSet.add("banana");
hashSet.add("apple");  // 重复，不会存入
System.out.println(hashSet.size()); // 2
```

#### LinkedHashSet

```java
// 有序（存储顺序 = 遍历顺序）、不可重复，效率略低于 HashSet
Set<String> linkedSet = new LinkedHashSet<>();
linkedSet.add("C");
linkedSet.add("A");
linkedSet.add("B");
System.out.println(linkedSet); // [C, A, B]（保持插入顺序）
```

#### TreeSet

```java
// 自动排序（默认自然排序）、不可重复
Set<Integer> treeSet = new TreeSet<>();
treeSet.add(5);
treeSet.add(1);
treeSet.add(3);
System.out.println(treeSet); // [1, 3, 5]（自动升序排列）

// 自定义排序
Set<Integer> descSet = new TreeSet<>(Comparator.reverseOrder());
```

### 3.3 Map 接口核心实现类

#### HashMap（最常用）

```java
/**
 * HashMap 使用示例 —— key 无序、唯一，value 可重复，查询/增删 O(1)。
 * 允许一个 null key，允许多个 null value。
 */
Map<Integer, String> hashMap = new HashMap<>();
hashMap.put(1, "One");
hashMap.put(2, "Two");
hashMap.put(1, "New One");  // key 重复，覆盖旧值
String value = hashMap.get(1);  // "New One"
hashMap.containsKey(2);         // true
hashMap.remove(2);
```

#### LinkedHashMap

```java
// key 有序（存储顺序 = 遍历顺序），适合 LRU 缓存
Map<String, Integer> linkedMap = new LinkedHashMap<>();
linkedMap.put("C", 3);
linkedMap.put("A", 1);
linkedMap.put("B", 2);
// 遍历顺序：C → A → B
```

#### TreeMap

```java
// key 自动排序，key 不能为 null
Map<String, Integer> treeMap = new TreeMap<>();
treeMap.put("banana", 2);
treeMap.put("apple", 1);
treeMap.put("cherry", 3);
// 按 key 字典序排列：apple → banana → cherry
```

### 3.4 集合遍历方式

```java
// List 遍历（三种方式）
List<String> list = Arrays.asList("A", "B", "C");
// 方式1：普通 for（有索引时推荐）
for (int i = 0; i < list.size(); i++) {
    System.out.println(list.get(i));
}
// 方式2：增强 for
for (String item : list) {
    System.out.println(item);
}
// 方式3：迭代器（Iterator）
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    System.out.println(it.next());
}

// Map 遍历（推荐 entrySet()）
Map<String, Integer> map = new HashMap<>();
map.put("A", 1);
map.put("B", 2);
// 推荐：entrySet() 一次获取 key + value
for (Map.Entry<String, Integer> entry : map.entrySet()) {
    System.out.println(entry.getKey() + " = " + entry.getValue());
}
// keySet()：遍历 key（需额外 get 获取 value，效率低）
// values()：仅遍历 value
```

### 3.5 集合与数组转换

```java
// 集合 → 数组
List<String> list = new ArrayList<>();
String[] array = list.toArray(new String[0]);

// 数组 → 集合（注意：Arrays.asList 返回的集合不可修改！）
String[] arr = {"A", "B", "C"};
List<String> fixedList = Arrays.asList(arr);
// fixedList.add("D"); // ❌ UnsupportedOperationException
// 正确：使用 new ArrayList<>() 包装
List<String> modifiableList = new ArrayList<>(Arrays.asList(arr));
modifiableList.add("D"); // ✅
```

---

## 四、实战要点

### 4.1 集合选型决策表

| 场景 | 推荐集合 | 原因 |
|------|----------|------|
| 查询频繁、增删少 | `ArrayList` | 索引直接访问 O(1) |
| 增删频繁、查询少 | `LinkedList` | 修改节点引用 O(1) |
| 需去重、不看顺序 | `HashSet` | 哈希表去重 O(1) |
| 需去重、保持顺序 | `LinkedHashSet` | 哈希表 + 链表维护顺序 |
| 需去重、需要排序 | `TreeSet` | 红黑树自动排序 |
| 键值对存储（通用） | `HashMap` | O(1) 增删改查 |
| 键值对、需有序 | `LinkedHashMap` | 维护插入顺序 |
| 键值对、需排序 | `TreeMap` | key 自动排序 |
| 多线程环境 | `ConcurrentHashMap` | 分段锁，并发安全 |
| 多线程 List | `CopyOnWriteArrayList` | 写时复制，读多写少场景 |

### 4.2 选型原则速记

- **查询多** → `ArrayList`
- **增删多** → `LinkedList`
- **去重** → `HashSet`
- **有序去重** → `LinkedHashSet`
- **排序去重** → `TreeSet`
- **键值对** → `HashMap`
- **有序键值对** → `LinkedHashMap`
- **排序键值对** → `TreeMap`

---

## 五、避坑总结

### 5.1 线程安全陷阱

| 集合 | 线程安全 | 替代方案 |
|------|----------|----------|
| `ArrayList` | ❌ 非线程安全 | `CopyOnWriteArrayList` 或 `Collections.synchronizedList()` |
| `LinkedList` | ❌ 非线程安全 | `ConcurrentLinkedQueue` |
| `HashSet` | ❌ 非线程安全 | `ConcurrentHashMap.newKeySet()` |
| `HashMap` | ❌ 非线程安全 | `ConcurrentHashMap`（推荐） |
| `Vector` | ✅ 线程安全 | 已过时，性能低 |
| `Hashtable` | ✅ 线程安全 | 已过时，换 `ConcurrentHashMap` |

### 5.2 去重与排序注意事项

- `HashSet`/`HashMap` 去重：存储自定义对象必须 **同时重写** `equals()` 和 `hashCode()`
- 两个方法必须一致：`hashCode` 相同则 `equals` 必须相同
- `TreeSet`/`TreeMap` 排序：元素必须实现 `Comparable` 接口，或构造时传入 `Comparator`
- `TreeMap`/`TreeSet` 的 key **不能为 null**

### 5.3 面试高频考点

#### ArrayList vs LinkedList

| 维度 | ArrayList | LinkedList |
|------|-----------|------------|
| 底层结构 | 动态数组 | 双向链表 |
| 查询 | O(1) 快 | O(n) 慢 |
| 增删 | O(n) 慢（需移动元素） | O(1) 快（改引用） |
| 内存 | 连续内存 | 每个节点存前后引用，占用更高 |
| 线程安全 | 否 | 否 |

#### HashMap vs Hashtable

| 维度 | HashMap | Hashtable |
|------|---------|-----------|
| 线程安全 | 否 | 是（效率低） |
| null key/value | 允许（key 仅一个 null） | 均不允许 |
| 初始容量 | 16 | 11 |
| 扩容倍数 | 2 倍 | 2 倍 + 1 |
| JDK 8 红黑树优化 | 有 | 无 |

#### HashMap 底层实现（JDK 8）

1. 底层是"**数组 + 链表 + 红黑树**"的组合结构
2. 哈希冲突时使用链表存储
3. 链表长度 ≥ 8 且数组容量 ≥ 64 时，链表转为红黑树（查询从 O(n) → O(log n)）
4. 树节点数 ≤ 6 时，红黑树退化为链表

#### HashSet 去重原理

1. 底层依赖 `HashMap`（value 是固定 Object 常量）
2. 调用 `hashCode()` 定位桶
3. 调用 `equals()` 判断是否相同
4. 相同则拒绝存入，不同则链表/红黑树存储

---

## 六、企业级最佳实践

### 6.1 开发规范

| 规范 | 说明 |
|------|------|
| **指定初始容量** | 预估数据量大时，`new HashMap<>(expectedSize / 0.75 + 1)` 避免频繁扩容 |
| **使用接口声明** | `List<String> list = new ArrayList<>()` 而非 `ArrayList<String> list = new ArrayList<>()` |
| **线程安全选型** | 多线程环境优先用 `ConcurrentHashMap`，不用 `Hashtable` 或 `Vector` |
| **空集合返回** | 方法返回集合时，空值用 `Collections.emptyList()` 而非 `null` |
| **避免原始类型** | 不使用无泛型的 `List`/`Map`（失去编译期类型检查） |

### 6.2 性能优化

- **大循环内避免 ArrayList 中间插入**：使用 LinkedList 或尾部追加
- **批量操作**：`addAll()` 优于逐个 `add()`
- **遍历 HashMap** 用 `entrySet()` 而非 `keySet() + get()`（后者额外一次哈希查找）
- **预估容量**：避免频繁的扩容开销

### 6.3 本章小结

1. 集合核心体系：Collection（单元素）和 Map（键值对），无继承关系
2. 重点掌握 List（有序可重复）、Set（无序不重复）、Map（键值对）的常用实现类
3. 面试重点：底层结构、实现区别、去重原理、HashMap 底层实现
4. 实战避坑：线程安全选型、自定义对象去重需重写 `equals()`/`hashCode()`、数组转集合不可修改
