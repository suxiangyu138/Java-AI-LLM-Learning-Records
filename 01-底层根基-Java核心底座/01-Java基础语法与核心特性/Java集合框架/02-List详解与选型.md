# 02 List 详解与选型

> ArrayList 是日常第一选择，LinkedList 是面试常客——搞清动态数组与双向链表的底层差异，选型才有依据

---

## 📚 目录

1. [List 接口语义](#1-list-接口语义)
2. [ArrayList：动态数组实现](#2-arraylist动态数组实现)
3. [LinkedList：双向链表实现](#3-linkedlist双向链表实现)
4. [Vector / Stack：遗留类怎么用](#4-vector--stack遗留类怎么用)
5. [CopyOnWriteArrayList：读多写少特化](#5-copyonwritearraylist读多写少特化)
6. [List 选型决策](#6-list-选型决策)
7. [常见陷阱与最佳实践](#7-常见陷阱与最佳实践)

---

## 1. List 接口语义

**List 是"有序、可重复、可按索引访问"的集合**——三个特性都来自接口契约：

| 契约 | 说明 | 方法 |
|------|------|------|
| 有序 | 有确定顺序（默认插入序），顺序就是数据的一部分 | 迭代顺序即插入顺序 |
| 可重复 | 相同元素可多次出现，`add` 永远成功返回 true | `add`、`addAll` |
| 索引访问 | 按整数下标定位，范围 [0, size) | `get(int)`、`set(int, E)`、`add(int, E)`、`remove(int)` |
| 子视图 | 左闭右开区间视图，修改写穿 | `subList(from, to)` |
| 查询 | 首次出现位置 | `indexOf(Object)`、`lastIndexOf(Object)` |

> 🎯 **核心要点**：List 的语义是"**索引即身份**"——两个 `equals` 相等的元素在同一 List 中可以共存（它们索引不同）；这与 Set"值即身份"完全不同。

---

## 2. ArrayList：动态数组实现

**底层结构**：`Object[] elementData` 动态数组。

### 2.1 关键机制

| 机制 | 细节 |
|------|------|
| 默认容量 | `new ArrayList()` 初始是**空数组**，首次 `add` 才扩容到 10（懒加载） |
| 扩容倍数 | 1.5 倍：`oldCapacity + (oldCapacity >> 1)` |
| 扩容动作 | `Arrays.copyOf` → `System.arraycopy` 整块复制，O(n) |
| 缩容 | 不会自动缩容；`trimToSize()` 手动缩到 size |
| 指定容量 | `new ArrayList<>(1000)` 直接给足，避免反复扩容 |

**为什么是 1.5 倍而不是 2 倍？** 空间与时间的权衡：2 倍扩容平均浪费 50% 空间（扩容后只用了 50%），1.5 倍浪费 33%；两者均摊插入复杂度都是 O(1)，但 1.5 倍更省内存。而每次扩容复制 O(n) 元素，**扩容次数 = log(最终容量/初始容量)**——所以预估容量能显著减少复制次数。

### 2.2 复杂度全景

| 操作 | 复杂度 | 说明 |
|------|:------:|------|
| `get(i)` / `set(i, e)` | O(1) | 连续内存 + 下标偏移，缓存友好 |
| `add(e)` 尾部 | O(1) 均摊 | 偶发扩容 O(n)，均摊后 O(1) |
| `add(i, e)` 中间 | O(n) | 从 i 起全部后移一位 |
| `remove(i)` / `remove(Object)` | O(n) | 前移一位；remove(Object) 还要先 O(n) 找 |
| `contains` / `indexOf` | O(n) | 线性扫描，依赖 equals |

### 2.3 实战：容量预估

```java
// 反例：add 100 万次触发约 25 次扩容（10 → 15 → 22 → ...），每次都复制全部元素
List<String> bad = new ArrayList<>();
for (int i = 0; i < 1_000_000; i++) bad.add("x" + i);

// 正例：一次给足，零扩容
List<String> good = new ArrayList<>(1_000_000);
for (int i = 0; i < 1_000_000; i++) good.add("x" + i);
```

> 💡 已知大概量级时一定指定初始容量；`Stream.toList()`（JDK 16+）内部也做了精确容量分配。

---

## 3. LinkedList：双向链表实现

**底层结构**：`Node<E>` 双向链表，每个节点持有 `item`、`prev`、`next` 三个引用。

### 3.1 复杂度全景

| 操作 | 复杂度 | 说明 |
|------|:------:|------|
| `get(i)` / `set(i, e)` | **O(n)** | 从头/尾按索引步进（小于 size/2 从头走） |
| `addFirst` / `addLast` | O(1) | 只改头尾指针 |
| `add(i, e)` 中间 | **O(n)** | 先 O(n) 走到位置，再 O(1) 插入 |
| `removeFirst` / `removeLast` | O(1) | |
| `remove(Object)` | O(n) | 线性查找 |
| `contains` | O(n) | |

### 3.2 内存开销

```text
ArrayList 存 100 个 String：1 个 Object[100]  ≈  800B（对象头+引用数组）
LinkedList 存 100 个 String：100 个 Node 节点 ≈  3200B+（每个节点对象头+3 引用+对齐）
```

每个节点多 ~24~40 字节（对象头 16B + prev/next 两个引用 8B，含对齐），**LinkedList 内存约为 ArrayList 的 2 倍**。

> ⚠️ **面试名场面："LinkedList 插入比 ArrayList 快"是错的**。中间插入两者都是 O(n)——LinkedList 的 n 花在"走到位置"的遍历上，且遍历链表对缓存极不友好；实测数据量大时 ArrayList 的元素移动反而更快。LinkedList 真正的优势只有**头尾操作 O(1)**。

### 3.3 双接口身份：List 也是 Deque

```java
LinkedList<String> ll = new LinkedList<>();
ll.addFirst("a");        // Deque 身份：头插 O(1)
ll.addLast("b");         // 尾插 O(1)
ll.pollFirst();          // 弹头
// 但用 Deque 接口声明更明确：Deque<String> q = new LinkedList<>();
```

> 🎯 **核心要点**：LinkedList 能当队列/栈用，但**轮不到它**——ArrayDeque 循环数组实现，内存更少、缓存友好、双端操作同样 O(1)，是队列/栈的正确选择（见 04 模块）。

---

## 4. Vector / Stack：遗留类怎么用

| 类 | 与 ArrayList 差异 | 现状 |
|----|------------------|------|
| `Vector` | 方法全部 `synchronized`（**全局锁**）；容量可设增量；2 倍扩容 | 遗留类（legacy），线程安全但性能差，不推荐 |
| `Stack extends Vector` | 栈语义 `push/pop/peek`，同步方法 | 遗留类，**已明确不推荐**（JDK 文档建议用 `ArrayDeque` 替代） |

```java
// ❌ 遗留用法
Stack<String> stack = new Stack<>();
stack.push("a");

// ✅ 正确替代（JDK 官方建议）
Deque<String> stack = new ArrayDeque<>();
stack.push("a");      // 同样的 push/pop/peek API
```

**为什么 Vector/Stack 被淘汰**：① 同步代价——所有方法全局加锁，即使单线程也白付锁开销；② 设计缺陷——`Stack` 继承 `Vector` 违反语义（栈不该有 `add(0, x)` 中间插入），是教科书级的"继承滥用"反例；③ 现代并发集合（CopyOnWriteArrayList、ConcurrentLinkedDeque）提供更好的线程安全方案。

> ⚠️ 注意：Vector/Hashtable/Stack 均**未被标注 `@Deprecated`**（语言层面仍可用），但 JDK 文档与工程实践一致不建议使用——面试答"已废弃"会显得不严谨，正确说法是"**遗留类，官方建议替代**"。

---

## 5. CopyOnWriteArrayList：读多写少特化

**原理**：写操作（add/set/remove）时**复制底层数组副本**，在副本上修改后整体替换引用（`volatile` 发布）；读操作不加锁直接读旧数组。

```text
读：get(i) 直接读 volatile array[i]        → 无锁 O(1)
写：synchronized 块内 copy 出新数组 → 修改 → 发布   → O(n) 复制
```

**特点与代价**：

| 特性 | 说明 |
|------|------|
| 读多写少场景极优 | 读完全无锁，写多时反复复制灾难性低效 |
| 弱一致性迭代器 | 迭代器是"创建时数组的快照"，修改不影响已创建的迭代器，**不抛 ConcurrentModificationException** |
| 写开销 O(n) | 每次写都全量复制 |
| 数据最终一致 | 同一时刻不同线程可能看到不同版本 |

**适用场景**：监听器列表（Spring 事件监听器）、缓存配置项、读多写少的白名单——**写频率高于读频率的场合绝对不要用**。

```java
// 监听器注册表：读（通知所有监听器）远多于写（注册/注销）
private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

public void addListener(Listener l) { listeners.add(l); }          // 偶尔写
public void notifyAll() { for (Listener l : listeners) l.onEvent(); } // 频繁读
```

---

## 6. List 选型决策

```text
是否需要线程安全？
├── 是 → 读多写少 → CopyOnWriteArrayList
│       写多读少 → Collections.synchronizedList + 手动同步块
│       并发队列语义 → ConcurrentLinkedDeque（见 06 模块）
└── 否 → 主要操作是什么？
    ├── 随机访问 / 按索引查 → ArrayList ✅
    ├── 尾部增删（栈/队列） → ArrayList ✅（或 ArrayDeque）
    ├── 头尾增删都频繁 → ArrayDeque ✅（比 LinkedList 更好）
    ├── 中间插入删除 → 两者都是 O(n)，选 ArrayList（缓存友好）
    └── 只需按序遍历 → ArrayList（连续内存，迭代快）
```

| 维度 | ArrayList | LinkedList |
|------|:---------:|:----------:|
| 随机访问 | ✅ O(1) | ❌ O(n) |
| 头尾操作 | 头 O(n)，尾 O(1) | ✅ 双端 O(1) |
| 内存 | 紧凑（引用数组） | 每节点 ~2 倍开销 |
| 缓存友好 | ✅ 连续内存 | ❌ 节点分散 |
| 中间插入 | O(n) 移动 | O(n) 遍历 + O(1) 改链 |
| 结论 | **绝大多数场景首选** | 仅"头尾操作 + 无需索引"的狭窄场景，且通常被 ArrayDeque 替代 |

> 🎯 **核心要点**：实践中 **99% 的 List 需求用 ArrayList**。LinkedList 的"优点"（头尾 O(1)）被 ArrayDeque 覆盖，"缺点"（随机访问 O(n)、内存翻倍）却无法弥补——面试能说出"LinkedList 的应用场景几乎被 ArrayDeque 取代"是明显的加分项。

---

## 7. 常见陷阱与最佳实践

| 陷阱 | 现象 | 正确做法 |
|------|------|---------|
| `Arrays.asList` 误用 | 返回的 List **不可增删**（固定大小数组视图），`add` 抛 UnsupportedOperationException；且修改元素会写穿原数组 | `new ArrayList<>(Arrays.asList(...))` 包装一层；或 JDK 9+ 用 `List.of` |
| `subList` 用后改原列表 | subList 是**活视图**，原列表结构性修改后，subList 行为不确定甚至抛 ConcurrentModificationException | 需要独立副本用 `new ArrayList<>(list.subList(a, b))` |
| 遍历中删除 | 见 01 模块第 4 节 | `removeIf` / 迭代器 remove |
| 按索引删除混淆 | `remove(1)` 删的是**下标 1**；想删值为 1 的 Integer 元素必须 `remove(Integer.valueOf(1))` | 注意自动装箱陷阱 |
| 泛型基础类型 | `new ArrayList<int>()` 编译错误 | 用包装类 `Integer`（有性能损耗意识） |
| `toArray()` 返回值 | 无参 `toArray()` 返回 `Object[]`，直接强转 `String[]` 抛 ClassCastException | `list.toArray(new String[0])` |

```java
// toArray 的正确姿势
List<String> list = new ArrayList<>(List.of("a", "b"));
String[] arr = list.toArray(new String[0]);    // ✅ JDK 8+ 推荐：传入空数组最快
// 原因：非空数组若长度不足，内部仍会新建数组——空数组省去一次长度判断与复制
```

---

**下一模块**：[03-Set详解与选型](./03-Set详解与选型.md) / **返回总览**：[00-Java集合框架知识体系总览](./00-Java集合框架知识体系总览.md)
