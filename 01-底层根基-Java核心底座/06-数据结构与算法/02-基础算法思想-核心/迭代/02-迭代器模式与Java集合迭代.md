# 迭代器模式与 Java 集合迭代
> Iterator 解耦遍历与容器——for-each 语法糖背后的设计模式与 fail-fast 陷阱

## 📚 目录
1. [迭代器模式](#1-迭代器模式)
2. [for-each 语法糖](#2-for-each-语法糖)
3. [Iterator 与 ListIterator](#3-iterator-与-listiterator)
4. [fail-fast 与遍历中删除](#4-fail-fast-与遍历中删除)
5. [Spliterator 与并行迭代](#5-spliterator-与并行迭代)
6. [手写迭代器](#6-手写迭代器)
7. [迭代器 vs 索引遍历](#7-迭代器-vs-索引遍历)

---

## 1. 迭代器模式

> 迭代器模式（Iterator Pattern）：**提供一种顺序访问聚合对象元素的方法，而不暴露其内部表示**。GoF 行为型设计模式，Java `Iterable`/`Iterator` 是标准实现。

```
为什么需要迭代器？
  ① 解耦：调用方只依赖 Iterator 接口，不关心底层是数组/链表/树
  ② 统一：ArrayList/LinkedList/HashSet/TreeMap 遍历方式完全一致
  ③ 封装：容器内部结构对外不可见，遍历逻辑不散落各处

角色划分：
  Iterable<T>   → "可迭代"：提供 iterator() 工厂方法
  Iterator<T>   → "迭代器"：hasNext()/next() 游标推进
  聚合对象      → 实现 Iterable，把游标逻辑委托给 Iterator
```

```java
// 标准接口（java.lang / java.util）
public interface Iterable<T> {
    Iterator<T> iterator();          // 返回迭代器
}

public interface Iterator<T> {
    boolean hasNext();               // 是否还有元素
    T next();                        // 返回当前并前进（无元素则抛 NoSuchElementException）
    default void remove() { ... }    // 移除刚返回的元素（可选操作）
}
```

> 💡 集合框架里 ArrayList/LinkedList/HashSet 都实现 `Iterable`，所以全部能用 for-each——这就是迭代器模式"统一遍历"的威力。

## 2. for-each 语法糖

> for-each（增强 for）不是独立机制，而是**编译器展开成 Iterator 的语法糖**。

```java
// 写法（集合）
for (String s : list) {
    System.out.println(s);
}

// 编译后等价于
for (Iterator<String> it = list.iterator(); it.hasNext(); ) {
    String s = it.next();
    System.out.println(s);
}
```

| 遍历对象 | 编译展开 | 说明 |
|---------|---------|------|
| 数组 | 索引 `for (int i = 0; i < arr.length; i++)` | 数组不用迭代器 |
| Iterable 集合 | Iterator + hasNext/next | 走迭代器模式 |
| 流/其他 | 视对象而定 | 自定义 Iterable 也适用 |

⚠️ 三个注意点：

```
① 遍历中删除元素会抛 ConcurrentModificationException（见 §4）
② 无法获取"当前索引"（需要下标时用传统 for）
③ 只读语义：修改元素引用可以，替换结构不行
```

## 3. Iterator 与 ListIterator

### 3.1 Iterator 核心方法

```java
List<String> list = new ArrayList<>(List.of("a", "b", "c"));
Iterator<String> it = list.iterator();

while (it.hasNext()) {        // 先判断，再取值
    String s = it.next();
    System.out.println(s);
}
// ⚠️ 无 hasNext 保护直接 next()：游标越过末尾 → NoSuchElementException
```

### 3.2 ListIterator：双向迭代

```java
// ListIterator 是 List 专有的"加强版迭代器"：可反向、可改值、可插入
ListIterator<String> it = list.listIterator(list.size());  // 从末尾开始
while (it.hasPrevious()) {
    String s = it.previous();       // 反向遍历
    it.set(s.toUpperCase());        // 替换当前元素
}

// 方法表：hasNext/next/hasPrevious/previous/nextIndex/previousIndex
//        set(E) 替换 / add(E) 插入 / remove() 删除
```

| 能力 | Iterator | ListIterator |
|------|:---:|:---:|
| 正向遍历 | ✅ | ✅ |
| 反向遍历 | ❌ | ✅ |
| 替换元素 | ❌ | ✅ |
| 插入元素 | ❌ | ✅ |
| 获取索引 | ❌ | ✅ |
| 适用范围 | 所有集合 | 仅 List |

## 4. fail-fast 与遍历中删除

### 4.1 modCount 机制

```
fail-fast（快速失败）：
  容器维护 modCount（结构修改次数：add/remove/clear 都会 +1）
  迭代器创建时快照 expectedModCount
  每次 next() 检查两者是否一致
  不一致 → 抛 ConcurrentModificationException（CME）

目的：并发/结构性修改时快速失败，避免"脏读"出难以排查的错误
```

```java
// ❌ 经典错误：for-each 中直接删除 → CME
List<String> list = new ArrayList<>(List.of("a", "b", "c"));
for (String s : list) {
    if (s.equals("b")) {
        list.remove(s);        // ⚠️ 结构修改 → 下次 next() 抛 CME
    }
}
```

### 4.2 三种正确删除姿势

```java
// ✅ 方式一：迭代器自己的 remove（同步维护 modCount）
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    if (it.next().equals("b")) it.remove();
}

// ✅ 方式二：removeIf（Java 8+，内部也是迭代器）
list.removeIf(s -> s.equals("b"));

// ✅ 方式三：倒序索引遍历（只适合支持随机访问的 List）
for (int i = list.size() - 1; i >= 0; i--) {
    if (list.get(i).equals("b")) list.remove(i);
}

// ⚠️ 注意：iterator.remove() 前必须先 next()，且只能调一次
```

| 场景 | 正确做法 | 错误做法 |
|------|---------|---------|
| 遍历删除集合元素 | `it.remove()` / `removeIf` | 集合直接 `remove` |
| 遍历时替换值 | `ListIterator.set()` | 结构替换 |
| 并发修改 | `CopyOnWriteArrayList` / `ConcurrentHashMap` | 普通集合硬遍历 |

## 5. Spliterator 与并行迭代

```
Spliterator（split + iterator）：
  Java 8 引入，为并行流设计
  把大集合"切块"（trySplit），分给多线程各自迭代
  支持：tryAdvance（单步）/ forEachRemaining（批量）/ estimateSize（预估）

应用：parallelStream 底层就是 Spliterator 切分
  数据量大 + 无状态操作 → 并行收益明显
  数据量小 → 切分开销反而更慢（别滥用并行）
```

```java
// 并行流底层：Spliterator 分块迭代
long sum = list.parallelStream()
               .mapToLong(Long::longValue)
               .sum();          // 多线程分块累加，结果再合并

// 手动切分示意
Spliterator<String> s1 = list.spliterator();
Spliterator<String> s2 = s1.trySplit();   // 切一半给 s2
```

## 6. 手写迭代器

### 6.1 链表迭代器（经典面试手写题）

```java
/**
 * 单向链表迭代器：不暴露内部 Node，只暴露遍历能力
 */
public class LinkedListIterator<T> implements Iterator<T> {
    private Node<T> cur;                        // 游标

    public LinkedListIterator(Node<T> head) {
        this.cur = head;                        // 初始化：指向头节点
    }

    @Override
    public boolean hasNext() {
        return cur != null;                     // 游标未到尾
    }

    @Override
    public T next() {
        if (!hasNext()) throw new NoSuchElementException();
        T val = cur.val;
        cur = cur.next;                         // 步进（迭代三要素之更新）
        return val;
    }
}
// 使用：for (T x : new IterableLinkedList(head)) { ... }
// ⚠️ 面试要点：游标语义（next() 返回"当前"并前进，不返回"下一个"）
```

### 6.2 无限序列迭代器（惰性求值）

```java
/**
 * 斐波那契迭代器：无限序列也能"迭代"——用到才算（惰性）
 */
public class FibonacciIterator implements Iterator<Integer> {
    private int a = 0, b = 1;

    @Override
    public boolean hasNext() { return true; }   // 无限序列永远有下一个

    @Override
    public Integer next() {
        int cur = a;
        int next = a + b;
        a = b;
        b = next;
        return cur;              // 0, 1, 1, 2, 3, 5 ...
    }
}
// 配合 Stream：StreamSupport.stream(...) 或手写 while 取前 N 个
// ⚠️ 价值：把"状态机"封装成迭代器，调用方无需关心内部状态
```

> 💡 手写迭代器的面试话术："迭代器把游标状态封装在内部，调用方只依赖 hasNext/next 两个方法——这就是迭代器模式的解耦价值。"

## 7. 迭代器 vs 索引遍历

| 维度 | 索引遍历（get(i)） | 迭代器遍历 |
|------|------|------|
| ArrayList | O(1) 随机访问，快 | 略慢（间接调用） |
| LinkedList | **O(n²) 灾难**（get(i) 从头找） | **O(n) 正确姿势** |
| HashSet/Map | 不支持 | 唯一方式 |
| 遍历中删除 | 需维护索引偏移 | `it.remove()` 安全 |
| 语义 | 依赖"支持随机访问" | 只依赖"可迭代" |

```java
// ❌ 链表用索引遍历：每次 get(i) 都是 O(n) 从头扫描 → 整体 O(n²)
for (int i = 0; i < linkedList.size(); i++) {
    process(linkedList.get(i));
}

// ✅ 链表用迭代器/for-each：O(n) 顺序推进
for (String s : linkedList) {
    process(s);
}
```

> 🎯 **核心要点**：迭代器模式 = **遍历与容器解耦**（GoF 行为型）。for-each 是编译成 Iterator 的语法糖；**遍历中删除必须用 `iterator.remove()` 或 `removeIf`**，否则触发 fail-fast 抛 CME；手写迭代器要把握"游标 + hasNext/next"语义；LinkedList 用索引遍历是 O(n²) 陷阱，迭代器才是正确姿势。

---

**下一模块**：[03-递归转迭代](03-递归转迭代.md) | **返回总览**：[00-迭代知识体系总览](00-迭代知识体系总览.md)
