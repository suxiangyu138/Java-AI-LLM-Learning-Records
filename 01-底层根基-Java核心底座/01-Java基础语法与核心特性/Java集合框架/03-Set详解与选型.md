# 03 Set 详解与选型

> Set 是"值即身份"的集合——去重、成员判断、交集并集，理解它的底层马甲与 equals/hashCode 契约是正确使用的前提

---

## 📚 目录

1. [Set 接口语义与数学集合](#1-set-接口语义与数学集合)
2. [HashSet：HashMap 的马甲](#2-hashsethashmap-的马甲)
3. [LinkedHashSet：保持插入顺序的去重](#3-linkedhashset保持插入顺序的去重)
4. [TreeSet：有序去重与范围查询](#4-treeset有序去重与范围查询)
5. [EnumSet：位向量实现的极致优化](#5-enumset位向量实现的极致优化)
6. [去重的正确性：equals 与 hashCode 契约](#6-去重的正确性equals-与-hashcode-契约)
7. [Set 选型决策](#7-set-选型决策)

---

## 1. Set 接口语义与数学集合

**Set 是"不可重复、无索引"的集合**——元素身份由 `equals` 决定，两个 equals 相等的元素不能共存。

**与 List 的本质差异**：

| 维度 | List | Set |
|------|------|-----|
| 元素身份 | 索引 | **值**（equals） |
| 重复元素 | 允许 | 拒绝（add 返回 false） |
| 顺序保证 | 必有 | 视实现而定（HashSet 无序、LinkedHashSet 插入序、TreeSet 有序） |
| 访问方式 | 按索引 | 只能迭代 / contains 判断 |

**Set 继承数学集合运算**（来自 `Collection` 通用契约，语义上就是并集交集差集）：

| 操作 | 方法 | 数学含义 |
|------|------|---------|
| 并集 | `addAll(other)` | 保留所有元素 |
| 交集 | `retainAll(other)` | 只保留与 other 共同的元素 |
| 差集 | `removeAll(other)` | 去掉 other 中的元素 |
| 包含 | `containsAll(other)` | 判断子集关系 |

```java
Set<Integer> a = new HashSet<>(Set.of(1, 2, 3));
Set<Integer> b = new HashSet<>(Set.of(3, 4, 5));

Set<Integer> union = new HashSet<>(a); union.addAll(b);       // {1,2,3,4,5}
Set<Integer> inter = new HashSet<>(a); inter.retainAll(b);    // {3}
Set<Integer> diff = new HashSet<>(a);  diff.removeAll(b);     // {1,2}
```

> 🎯 **核心要点**：Set 的三个实现（Hash/LinkedHash/Tree）数据结构与 Map 三兄弟一一对应，区别只在"顺序语义"——选 Set 先问自己：需要什么顺序？

---

## 2. HashSet：HashMap 的马甲

**底层**：内部持有 `HashMap<E, Object>`，元素作为 **key**，value 统一为同一个 `PRESENT` 占位对象。

```java
// HashSet 内部（JDK 8 源码简化）
public class HashSet<E> extends AbstractSet<E> implements Set<E> {
    private transient HashMap<E, Object> map;
    private static final Object PRESENT = new Object();   // 所有元素共用一个占位 value

    public boolean add(E e) {
        return map.put(e, PRESENT) == null;   // 返回 null = 新插入；返回旧 value = 重复
    }
}
```

**去重原理**（即 HashMap put 键的过程）：

```text
add(e) → hash(e.hashCode()) → 定位桶 → 桶内找 equals 相等的键
  ├── 找到 → 重复，不插入，返回 false
  └── 没找到 → 插入，返回 true
```

**特性总结**：无序（迭代顺序由 hash 分布决定，**同一 JVM 内稳定但不可依赖**）、O(1) 增删查、允许一个 null 元素（HashMap 允许一个 null 键）、fail-fast 迭代器、线程不安全。

> ⚠️ **易错点**：HashSet 的"无序"不是说"随机"，而是"**不保证顺序**"——依赖迭代顺序做业务（如取第一个元素当默认值）是不可靠的；需要确定性顺序就换 LinkedHashSet/TreeSet。

---

## 3. LinkedHashSet：保持插入顺序的去重

**底层**：`LinkedHashMap` 的马甲——HashMap 数组 + **双向链表串联所有元素**，链表记录插入顺序。

```java
// 底层结构：数组 + 链表（哈希冲突）+ 双向链表（记录插入序）
// 图中虚线即"记录顺序"的双向链表：
//   a <-> b <-> c <-> d    （插入顺序）
//   每个节点额外有 before/after 指针
```

| 维度 | HashSet | LinkedHashSet |
|------|:-------:|:-------------:|
| 底层 | HashMap | LinkedHashMap |
| 顺序 | 无保证 | **插入顺序** |
| 增删查复杂度 | O(1) | O(1)（多了链表维护的常数开销） |
| 额外内存 | — | 每元素多两个引用（before/after） |
| 典型场景 | 纯去重 | 需要"去重 + 保序"：请求去重、最近使用列表、去重后的展示顺序 |

```java
// 场景：订单去重但保持下单顺序
Set<String> orderIds = new LinkedHashSet<>();
orderIds.add("A3"); orderIds.add("A1"); orderIds.add("A2"); orderIds.add("A1");
System.out.println(orderIds);   // [A3, A1, A2] —— 去重且保持首次出现顺序
```

> 💡 JDK 21 起 LinkedHashSet 是 `SequencedSet`——`getFirst()/getLast()/reversed()` 直接可用（见 01 模块第 5 节），"取最近加入的去重列表"变得一行搞定。

---

## 4. TreeSet：有序去重与范围查询

**底层**：`TreeMap` 的马甲（红黑树），元素作为 key。元素按**自然序或比较器序**排列，`add`/`contains`/`remove` 均 O(log n)。

**关键特性**：

| 特性 | 说明 |
|------|------|
| 有序 | 迭代即有序输出，支持 `first()/last()` |
| 导航 | `floor/ceiling/lower/higher`（≤、≥、<、> 的最近值） |
| 范围 | `subSet(from, to)`（左闭右开）、`headSet`、`tailSet`——活视图 |
| 去重判定 | **比较器判等**：`compareTo == 0` 视为重复（不是 equals！） |
| null | **不允许** null 元素（比较器无法处理 null，抛 NPE） |
| 一致性 | 元素必须实现 `Comparable` 或构造时传 `Comparator` |

```java
// 场景：分数排行榜 Top 查询
TreeSet<Score> ranks = new TreeSet<>(Comparator.comparingInt(Score::points).reversed());
ranks.add(new Score("张三", 85));
ranks.add(new Score("李四", 92));
ranks.add(new Score("王五", 78));

System.out.println(ranks.first());           // 李四(92) —— 最高分
Score s = ranks.floor(new Score("?", 90));   // 李四(92) ≤ 90 的最近？不：90 向上最近是 92？floor 是 ≤ 90 → 张三(85)
// 注意 floor/ceiling 语义：floor(x) = 最大的 ≤x，ceiling(x) = 最小的 ≥x
```

> ⚠️ **TreeSet 的"相等"由比较器决定**：两个 `equals` 不同但 `compareTo` 为 0 的元素会被视为重复——这既是去重语义（推荐与 equals 保持一致），也是坑：比较器写错（如只比较部分字段）会导致元素被"意外吞掉"。同时 **Set 契约要求 compareTo 与 equals 一致**，否则行为违反 Set 语义。

---

## 5. EnumSet：位向量实现的极致优化

**底层**：枚举元素被映射为**位向量**（`long` 位掩码，枚举 ≤64 个用单个 long，超过用 long[]），每个枚举常量占一位。

```java
public enum Day { MON, TUE, WED, THU, FRI, SAT, SUN }

Set<Day> workdays = EnumSet.range(Day.MON, Day.FRI);    // 连续范围
Set<Day> weekend = EnumSet.of(Day.SAT, Day.SUN);
Set<Day> all = EnumSet.allOf(Day.class);
Set<Day> none = EnumSet.noneOf(Day.class);

workdays.contains(Day.MON);    // O(1)，本质是位运算 & —— 快到极致
workdays.retainAll(weekend);   // 位运算 | & ^ —— 集合运算全是位操作
```

**性能与约束**：

| 维度 | 说明 |
|------|------|
| 性能 | 所有操作 O(1) 位运算，比 HashSet 快一个数量级，且无对象开销（不用包装 EnumSet 的 key） |
| 迭代顺序 | 按枚举声明顺序 |
| 约束 | **只接受枚举元素**；元素必须非 null |
| 构造 | 不能 new，必须静态工厂 `of/range/allOf/noneOf/complementOf` |

> 🎯 **核心要点**：枚举集合无脑用 EnumSet——它把"集合运算"降维成"位运算"，是 JDK 性能优化的典范。`EnumMap` 同理（见 05 模块）。

---

## 6. 去重的正确性：equals 与 hashCode 契约

**HashSet/HashMap 的去重与查找依赖双引擎**：

```text
查找流程：hashCode() 定位桶 → equals() 桶内比较
```

**契约**（`Object` 文档的硬性要求）：

1. `equals` 相等 → **hashCode 必须相等**（否则桶都不同，永远找不到，重复元素"去不掉"）；
2. `hashCode` 相等 → equals 可以不等（哈希碰撞，允许）；
3. 参与比较的字段必须稳定——**字段变化导致 hashCode 变化，元素将"丢失"**。

**反例演示**：

```java
public class User {
    private String name;
    private int age;

    // ❌ 只重写 equals 不重写 hashCode
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User u)) return false;
        return name.equals(u.name) && age == u.age;
    }
}

Set<User> users = new HashSet<>();
users.add(new User("a", 1));
System.out.println(users.contains(new User("a", 1)));   // false！
// 原因：两个对象 hashCode 不同（基于身份）→ 定位到不同桶 → equals 根本没机会被调用
```

**可变字段陷阱**（第三个反例，最隐蔽）：

```java
User u = new User("a", 1);
users.add(u);
u.setAge(2);                       // 参与 hashCode 的字段被修改
System.out.println(users.contains(u));   // false —— u 的桶号变了，HashMap 里找不到了
// 且 users 里仍"有"它但永远查不到 —— 泄漏
```

> 🎯 **核心要点**：放入 HashSet/HashMap 的元素要么不可变，要么**绝不修改参与 equals/hashCode 的字段**；`record`（JDK 16+）自动实现基于全部组件的值相等与哈希，天然规避这个坑。

---

## 7. Set 选型决策

```text
需要什么顺序？
├── 无顺序要求，只要去重/成员判断 → HashSet ✅（O(1) 最快）
├── 去重 + 保持插入顺序（保序展示、去重队列） → LinkedHashSet
├── 去重 + 自然序/自定义序（排行榜、字典序） → TreeSet
└── 元素是枚举 → EnumSet ✅（位运算，性能碾压）
```

| 维度 | HashSet | LinkedHashSet | TreeSet | EnumSet |
|------|:-------:|:-------------:|:-------:|:-------:|
| 底层 | HashMap | LinkedHashMap | TreeMap(红黑树) | long 位向量 |
| 复杂度 | O(1) | O(1) | O(log n) | O(1) 位运算 |
| 顺序 | 无 | 插入序 | 比较器序 | 枚举声明序 |
| null | 允许 1 个 | 允许 1 个 | 禁止 | 禁止 |
| 额外功能 | — | SequencedSet(JDK21+) | floor/ceiling/范围 | 集合运算位级 |
| 典型场景 | 通用去重 | 保序去重 | 有序查询 | 枚举组合 |

---

**下一模块**：[04-Queue与Deque详解](./04-Queue与Deque详解.md) / **返回总览**：[00-Java集合框架知识体系总览](./00-Java集合框架知识体系总览.md)
