# 01 Collection 体系与迭代器机制

> Collection 是单列集合的根——理解接口设计、迭代器与 fail-fast 机制，是整个集合框架的通用地基

---

## 📚 目录

1. [Collection 接口体系设计](#1-collection-接口体系设计)
2. [Iterable 与 Iterator：for-each 的底层原理](#2-iterable-与-iteratorfor-each-的底层原理)
3. [fail-fast 机制：modCount 与 ConcurrentModificationException](#3-fail-fast-机制modcount-与-concurrentmodificationexception)
4. [遍历中安全删除的三种姿势](#4-遍历中安全删除的三种姿势)
5. [JDK 21 SequencedCollection：统一有序访问](#5-jdk-21-sequencedcollection统一有序访问)
6. [Collection 的通用操作与边界](#6-collection-的通用操作与边界)

---

## 1. Collection 接口体系设计

```text
Iterable<E>                          （可迭代：唯一顶层）
  └── Collection<E>                  （单列集合根接口）
        ├── List<E>                  （有序、可重复、可按索引访问）
        ├── Set<E>                   （不可重复、数学集合语义）
        │     ├── SortedSet<E>       （有序 Set，自然序/比较器序）
        │     └── NavigableSet<E>    （可导航：floor/ceiling/lower/higher）
        ├── Queue<E>                 （队列：FIFO、容量、阻塞语义）
        │     └── Deque<E>           （双端队列：可当栈）
        └── SequencedCollection<E>   （JDK 21，JEP 431：有序访问统一接口）
```

**接口设计的两条主线**：

| 主线 | 设计 | 说明 |
|------|------|------|
| 能力递进 | List/Set/Queue 各自扩展 Collection | 每个子接口只加自己语义需要的方法，不互相污染 |
| 有序强化 | SortedSet → NavigableSet → SequencedCollection | 从"有序"到"可导航"到"首尾访问"逐级增强 |

**设计启示**：接口按"能力"而非"实现"划分——`Stack` 能当列表用但语义错误；`LinkedHashSet` 是 Set 却有序——**面向接口编程让这些差异透明化**。

> 🎯 **核心要点**：永远用接口类型声明变量（`List<String>` 而非 `ArrayList<String>`），实现类可以自由替换——这是集合框架最重要的设计哲学。

---

## 2. Iterable 与 Iterator：for-each 的底层原理

**`for-each` 是语法糖**，编译器会把：

```java
List<String> list = new ArrayList<>();
for (String s : list) {
    System.out.println(s);
}
```

**等价展开为**：

```java
for (Iterator<String> it = list.iterator(); it.hasNext(); ) {
    String s = it.next();          // 实际是 it.next() 返回并移动指针
    System.out.println(s);
}
```

**Iterable / Iterator 职责分离**：

| 接口 | 职责 | 关键方法 |
|------|------|---------|
| `Iterable<T>` | 可迭代标记，提供迭代器工厂 | `iterator()`；还有 `forEach(Consumer)` 与 `spliterator()` |
| `Iterator<T>` | 单向游标，负责遍历 | `hasNext()`、`next()`、`remove()`（默认抛异常）、`forEachRemaining(Consumer)` |

**Iterator 的边界行为**：

```java
List<String> list = List.of("a", "b");
Iterator<String> it = list.iterator();
it.next();        // a
it.next();        // b
// it.next();     // ❌ NoSuchElementException：游标越界
// it.remove();   // ❌ UnsupportedOperationException：不可变集合的迭代器不支持删除
```

**三条易错点**：

1. `next()` 先返回当前元素再移动游标；连续调用两次 `next()` 会**跳过**元素；
2. `Iterator.remove()` 是"最后返回的那个元素"，不能在 `next()` 之前调用（抛 `IllegalStateException`）；
3. 迭代器是一次性游标，用完即弃，不能复用。

> 💡 增强 for 内部还隐藏一个细节：**数组的 for-each 是普通 for 循环**（按索引），不是迭代器——所以数组遍历无 fail-fast 概念。

---

## 3. fail-fast 机制：modCount 与 ConcurrentModificationException

**fail-fast（快速失败）**：迭代器迭代过程中，若集合发生**结构性修改**（添加、删除、扩容——但不含修改已存在元素的值），迭代器立即抛 `ConcurrentModificationException`，而不是继续遍历产生不可预期的结果。

**实现原理**（以 ArrayList 为例）：

```java
// ArrayList 内部
protected transient int modCount = 0;      // 结构修改计数器，每次 add/remove/clear 都 ++

// ArrayList.Itr（迭代器）
int expectedModCount = modCount;           // 创建迭代器时快照

public E next() {
    checkForComodification();              // 每次 next 前比对
    ...
}

final void checkForComodification() {
    if (modCount != expectedModCount)      // 快照与当前不一致 → 有并发修改
        throw new ConcurrentModificationException();
}
```

**触发场景**（单线程也能触发）：

```java
List<String> list = new ArrayList<>(List.of("a", "b", "c"));

// 场景 1：for-each 中直接 remove → 抛异常
for (String s : list) {
    if (s.equals("b")) list.remove(s);     // ❌ ConcurrentModificationException
}

// 场景 2：迭代器与集合操作混用 → 抛异常
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    String s = it.next();
    if (s.equals("b")) list.remove(s);     // ❌ 集合的 remove 改动了 modCount
}
```

> ⚠️ **三个边界认知**：① fail-fast 是"尽力而为"的检测，不保证绝对安全——单线程内删除后**恰好**把 modCount 加回原值（如删除两个再添加两个）理论上能绕过检测；② 多线程下即使不抛异常也不代表安全，普通集合本就线程不安全；③ **`set()` 修改元素值不算结构性修改**（元素个数没变），不触发异常。

---

## 4. 遍历中安全删除的三种姿势

| 姿势 | 写法 | 特点 |
|------|------|------|
| 迭代器自带 remove | `it.remove()` | 单次删除最优，不额外分配 |
| 反向索引遍历 | `for (int i = size-1; i >= 0; i--) list.remove(i)` | 适合"按条件删除多个"且知道索引 |
| 流式过滤重建 | `list = list.stream().filter(pred).toList()` | JDK 16+，声明式、不可变结果 |
| removeIf（现代首选） | `list.removeIf(predicate)` | JDK 8+，单行解决，内部用迭代器实现 |

```java
List<String> list = new ArrayList<>(List.of("a", "b", "c", "b"));

// 姿势 1：迭代器删除
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    if (it.next().equals("b")) it.remove();     // ✅ 删除的是"刚返回的元素"

    // 姿势 4（等价于上面三行）：
    // list.removeIf(s -> s.equals("b"));       // ✅ 单行，内部也是迭代器+remove
}
System.out.println(list);   // [a, c]
```

> 🎯 **核心要点**：所有安全删除的本质是**让"检查者"与"修改者"是同一个对象**——迭代器内部删除会同步更新 expectedModCount，所以不触发 fail-fast。`removeIf` 是它的封装，日常首选。

---

## 5. JDK 21 SequencedCollection：统一有序访问

**背景问题**（JEP 431，JDK 21 交付，Stuart Marks 主导）：集合框架缺少"有定义顺序的集合"的统一类型。取首尾元素各接口做法不一——`List` 用 `get(0)`、`Deque` 用 `getFirst()`、`SortedSet` 用 `first()`、`LinkedHashSet` 取末尾只能遍历完整个集合。

**三个新接口**：

```java
// 1. SequencedCollection<E> extends Collection<E>
//    reversed() / addFirst() / addLast() / getFirst() / getLast() / removeFirst() / removeLast()

// 2. SequencedSet<E> extends Set<E>, SequencedCollection<E>
//    重写 reversed() 返回 SequencedSet

// 3. SequencedMap<K,V> extends Map<K,V>
//    reversed() / sequencedKeySet() / sequencedValues() / sequencedEntrySet()
//    putFirst() / putLast() / firstEntry() / lastEntry() / pollFirstEntry() / pollLastEntry()
```

**核心用法**：

```java
// 以前：不同集合取首尾写法五花八门
List<String> list = List.of("a", "b", "c");
list.get(0); list.get(list.size() - 1);

Deque<String> deque = new ArrayDeque<>();
deque.getFirst(); deque.getLast();

// JDK 21+：统一接口，一处写法
SequencedCollection<String> sc = list;
sc.getFirst();            // a
sc.getLast();             // c
sc.reversed();            // 逆序"活视图"：O(1) 创建，不拷贝，修改会写穿回原集合

LinkedHashSet<String> lhs = new LinkedHashSet<>(List.of("a", "b", "c"));
lhs.reversed().stream().forEach(System.out::println);   // c, b, a —— 以前要遍历完才拿到 last
```

**哪些集合是"有序列"**：

| 实现 | Sequenced 类型 | 说明 |
|------|---------------|------|
| ArrayList / LinkedList / Vector / Stack | SequencedCollection | List 体系 |
| ArrayDeque | SequencedCollection | Deque 体系 |
| LinkedHashSet | SequencedSet | 插入序 |
| TreeSet | SequencedSet（经 SortedSet） | 自然序 |
| LinkedHashMap | SequencedMap | 插入/访问序 |
| TreeMap | SequencedMap（经 SortedMap） | 键序 |

**边界行为**：空集合上 `getFirst()` 抛 `NoSuchElementException`；不可变集合的 `addFirst` 抛 `UnsupportedOperationException`；`HashMap`/`HashSet`/`EnumMap` **不实现**（无顺序保证）。

> 🎯 **核心要点**：SequencedCollection 的工程价值是"**参数类型可以声明顺序语义**"——方法签名写 `SequencedCollection<T>` 而非 `List<T>` 或 `Deque<T>`，任何有序列集合（含 LinkedHashSet）都能传入，同时首尾操作有了统一 API。

---

## 6. Collection 的通用操作与边界

| 操作 | 方法 | 边界/注意 |
|------|------|----------|
| 增 | `add(E)` / `addAll(Collection)` | Set 返回 false 表示重复拒绝；`addAll` 结果是"或"语义 |
| 删 | `remove(Object)` / `removeAll` / `clear()` | `remove` 只删**第一个匹配**（List）；返回 boolean |
| 查 | `contains(Object)` / `containsAll` | 依赖元素的 `equals`，自定义对象必须重写 |
| 大小 | `size()` / `isEmpty()` | 与 `size() == 0` 等价但更可读 |
| 转换 | `toArray()` / `toArray(T[])` | `toArray(new String[0])` 是 JDK 8+ 推荐写法（比预分配数组更快，见 08 模块） |
| 聚合 | `stream()` / `parallelStream()` | JDK 8+，衔接 Stream 体系 |

**Collection 不保证的东西**：不保证顺序（除非子接口声明）、不保证线程安全（除并发容器）、不保证"值"的唯一性（Set 才保证）。

```java
// contains 依赖 equals —— 反例警示
List<Order> orders = new ArrayList<>();
orders.add(new Order("A1"));
System.out.println(orders.contains(new Order("A1")));  // false！
// 除非 Order 重写了 equals（值相等），否则这是"引用比较"，必为 false
```

> ⚠️ **通用契约**：`add` 成功后 `contains` 必须为 true；`remove` 成功后 `size` 减一——违反这些契约的实现（如 equals/hashCode 错误的元素）会让集合行为不可预期，排查时先检查元素类的 equals。

---

**下一模块**：[02-List详解与选型](./02-List详解与选型.md) / **返回总览**：[00-Java集合框架知识体系总览](./00-Java集合框架知识体系总览.md)
