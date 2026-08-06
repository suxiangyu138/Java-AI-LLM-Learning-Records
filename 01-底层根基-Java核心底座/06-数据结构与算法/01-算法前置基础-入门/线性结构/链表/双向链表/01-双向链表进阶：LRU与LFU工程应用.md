# 01 - 双向链表进阶：LRU/LFU 与工程应用

> 定位：双向链表的理论纵深——API 设计与哨兵、LRU/LFU 设计模式、循环双端队列、与哈希的复合结构、Java 内部实现对比

## 📚 目录

1. [双向链表 API 设计](#1-双向链表-api-设计)
2. [哨兵节点：工程细节](#2-哨兵节点工程细节)
3. [LRU：双向链表的旗舰应用](#3-lru双向链表的旗舰应用)
4. [LFU：双哈希 + 双向链表](#4-lfu双哈希--双向链表)
5. [循环双端队列](#5-循环双端队列)
6. [Java 内部实现对比](#6-java-内部实现对比)
7. [面试要点与追问](#7-面试要点与追问)

---

## 1. 双向链表 API 设计

### 1.1 为什么需要双向

```
单向链表：删除节点需要找前驱（O(n)）
双向链表：prev 指针直接给前驱（O(1)）

⚠️ 双向链表的 O(1) 能力：
  ① 头部插入/删除
  ② 尾部插入/删除
  ③ 任意已知节点的插入/删除
  → 配合哈希定位 = 全 O(1)

⚠️ 面试必答：
"'已知节点'的插入删除 O(1) 是双向链表
 的核心价值——哈希提供'定位'，
 双向链表提供'操作'，组合成全 O(1)。"
```

### 1.2 完整 API 模板

```java
/** 双向链表基础 API（面试手写必备） */
class DListNode {
    int val;
    DListNode prev, next;
    DListNode(int v) { val = v; }
}

class DLinkedList {
    DListNode head, tail;           // ⚠️ 头尾指针

    void addFirst(DListNode node) {          // 头部插入 O(1)
        node.next = head;
        node.prev = null;
        if (head != null) head.prev = node;
        head = node;
        if (tail == null) tail = node;
    }

    void addLast(DListNode node) {           // 尾部插入 O(1)
        node.prev = tail;
        node.next = null;
        if (tail != null) tail.next = node;
        tail = node;
        if (head == null) head = node;
    }

    void remove(DListNode node) {            // ⚠️ 任意节点删除 O(1)
        if (node.prev != null) node.prev.next = node.next;
        else head = node.next;
        if (node.next != null) node.next.prev = node.prev;
        else tail = node.prev;
    }

    void moveToTail(DListNode node) {        // 移到尾部（LRU 刷新）
        remove(node);
        addLast(node);
    }
}
// ⚠️ 边界：空链表/单节点/头尾操作（prev/next 判空）
```

### 1.3 双向链表的变体

| 变体 | 特点 | 应用 |
|------|------|------|
| 普通双向 | head/tail 指针 | LRU 等 |
| 哨兵双向 | 固定 head/tail 哨兵 | 免空判（146 用） |
| 循环双向 | 首尾相连 | 浏览器历史、环 |
| 带频次 | 节点存 count | LFU 桶内 |

```
⚠️ 面试表达：
"双向链表四变体——哨兵版最实用
 （146 用哨兵免空判）、循环版用于
 环形场景（641 循环队列）。"
```

---

## 2. 哨兵节点：工程细节

### 2.1 哨兵的价值

```
哨兵（dummy）：固定的头尾节点（不存数据）
  ① 免去空链表判断（head/tail 永远存在）
  ② 插入/删除边界统一（无特判）

⚠️ 对比无哨兵：
  无哨兵：addFirst 要判 head 是否 null
  有哨兵：addAfter(dummy) 天然正确

⚠️ 面试必答：
"哨兵节点把'边界判断'变成'普通操作'——
 头尾永远存在，插入删除代码统一。
 工程上哨兵是双向链表的标准写法。"
```

### 2.2 哨兵版 LRU 骨架

```java
/** 哨兵版双向链表（146 的核心结构） */
class LRUList {
    static class Node {
        int key, val;
        Node prev, next;
        Node(int k, int v) { key = k; val = v; }
    }

    private final Node head = new Node(0, 0);   // ⚠️ 哨兵头（最旧侧）
    private final Node tail = new Node(0, 0);   // 哨兵尾（最新侧）

    LRUList() {
        head.next = tail;                        // ⚠️ 初始互指
        tail.prev = head;
    }

    void addToTail(Node node) {
        node.prev = tail.prev;
        node.next = tail;
        tail.prev.next = node;
        tail.prev = node;
    }

    void removeNode(Node node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }
}
// ⚠️ 哨兵互指初始化是常见遗漏（否则空链表操作 NPE）
```

---

## 3. LRU：双向链表的旗舰应用

### 3.1 设计要点

```
LRU = 哈希（定位）+ 双向链表（顺序）+ 哨兵（边界）
  ① get：命中 → moveToTail（刷新）
  ② put：新 key → 尾部插入 + 超容量淘汰头部
  ③ 节点存 key：淘汰时同步删哈希

⚠️ 复杂度论证（面试必答）：
  哈希 get O(1) + 链表 move O(1) = O(1)

⚠️ 完整实现见设计/02 的 146（本模块讲设计模式）
```

### 3.2 LRU 变体

| 变体 | 改动 |
|------|------|
| LRU-K（访问 K 次才入缓存） | 记录访问计数 |
| 2Q（两次队列） | 双队列分级 |
| 过期 LRU | 节点带时间戳 |
| 并发 LRU | 锁/分段（见设计/01） |

```
⚠️ 面试表达：
"LRU 是设计题模板——'哈希定位 +
 双向链表保序 + 哨兵边界'三件套，
 变体（LFU/LRU-K/过期）都是加状态。"
```

---

## 4. LFU：双哈希 + 双向链表

### 4.1 LFU 的设计（460）

```
LFU = 最少使用淘汰（频次最低者）
结构：双哈希
  key → 节点（O(1) 定位）
  频次 → 双向链表（每个频次一条 LRU 链）
  维护 minFreq（当前最低频次）

⚠️ 复杂度论证：
  访问 +1 频次：节点从 freq 链移到 freq+1 链（O(1)）
  淘汰：minFreq 链的头部（O(1)）

⚠️ 面试必答：
"LFU = 双哈希嵌套——外层 key 哈希定位节点，
 内层'频次 → LRU 链'。访问时跨链移动 O(1)，
 淘汰取 minFreq 链头。"
```

### 4.2 骨架实现

```java
/** LFU 骨架（460 的核心结构） */
class LFUCache {
    Map<Integer, Node> nodes = new HashMap<>();        // key → 节点
    Map<Integer, LinkedList<Node>> freqList = new HashMap<>();  // 频次 → LRU 链
    int minFreq = 0;
    int capacity;

    void touch(Node node) {                  // ⚠️ 访问：频次 +1
        int oldFreq = node.freq;
        freqList.get(oldFreq).remove(node);
        if (freqList.get(oldFreq).isEmpty()
                && oldFreq == minFreq) minFreq++;      // ⚠️ minFreq 提升
        node.freq++;
        freqList.computeIfAbsent(node.freq, k -> new LinkedList<>())
                .addLast(node);              // 新频次链尾部（LRU）
    }

    void evict() {                           // 淘汰 minFreq 链头
        Node victim = freqList.get(minFreq).removeFirst();
        nodes.remove(victim.key);
    }
}
// ⚠️ 面试必答："LFU 的难点是 minFreq 维护——
//   空链且等于 minFreq 时 minFreq++；
//   新节点 freq=1 时 minFreq 重置为 1。"
```

---

## 5. 循环双端队列

### 5.1 641 的设计

```
双端队列（前后都可插入删除）
两种实现：
  ① 循环数组：head/tail 指针 + 取模（O(1) 空间好）
  ② 双向链表：哨兵 + 前后操作（O(1) 无扩容）

⚠️ 面试必答：
"双端队列 = 双向链表的天然应用——
 前后插入删除都是 O(1)；
 数组版用循环下标模拟（641 两种都可）。"
```

### 5.2 循环数组版要点

```java
/** 641 循环双端队列（数组版要点） */
class MyCircularDeque {
    private final int[] data;
    private int head = 0, tail = 0;          // ⚠️ 循环下标
    private int size = 0;
    private final int capacity;

    boolean insertFront(int value) {
        if (isFull()) return false;
        head = (head - 1 + capacity) % capacity;   // ⚠️ 前移（防负）
        data[head] = value;
        size++;
        return true;
    }

    boolean insertLast(int value) {
        if (isFull()) return false;
        data[tail] = value;
        tail = (tail + 1) % capacity;        // ⚠️ 后移（循环）
        size++;
        return true;
    }
    // ⚠️ 删除同理：front 后移、last 前移
}
// ⚠️ 循环下标的统一公式：(i + delta + capacity) % capacity
// ⚠️ 面试必答："循环队列 = 数组 + 取模下标——
//   head/tail 环形移动，满/空用 size 判断
//   （不用 head==tail 二义）。"
```

---

## 6. Java 内部实现对比

### 6.1 Java 集合中的双向链表

| 类 | 底层 | 特点 |
|------|------|------|
| LinkedList | 双向链表 | 任意位置 O(n) 查找 |
| ArrayDeque | 循环数组 | ⚠️ 更推荐（缓存友好） |
| LinkedHashMap | 哈希+双向链表 | accessOrder 可做 LRU |

```
⚠️ 工程真相：
  LinkedList 的"随机访问 O(n)"常被误解
  ArrayDeque 在栈/队列场景更快（数组连续）

⚠️ 面试必答：
"Java 栈队列首选 ArrayDeque（数组连续、缓存友好）；
 LinkedList 的双向链表适合'已知节点的 O(1) 删除'
 （配合哈希）。LinkedHashMap(accessOrder=true)
 是 JDK 内置 LRU。"
```

### 6.2 LinkedHashMap 实现 LRU

```java
// JDK 内置 LRU（无需手写）：
LinkedHashMap<Integer, Integer> lru =
        new LinkedHashMap<>(16, 0.75f, true) {   // ⚠️ accessOrder=true
    @Override
    protected boolean removeEldestEntry(Map.Entry<Integer, Integer> eldest) {
        return size() > capacity;                 // 超容量淘汰最老
    }
};
// ⚠️ 面试表达："LinkedHashMap(accessOrder=true)
//   + removeEldestEntry = JDK 内置 LRU——
//   手写 146 展示底层原理，工程用内置。"
```

---

## 7. 面试要点与追问

### 7.1 面试话术模板

```
"双向链表价值 = 已知节点的 O(1) 插入删除。
 设计模式：哈希定位 + 双向链表保序 + 哨兵边界。
 LRU = 三件套（访问刷新/淘汰头部/同步删哈希）；
 LFU = 双哈希嵌套（频次 → LRU 链）+ minFreq 维护；
 循环队列 = 数组 + 取模下标。
 Java 工程用 ArrayDeque/LinkedHashMap。"
```

### 7.2 高频追问 TOP 8

| # | 追问 | 标准回答 |
|:---:|------|------|
| 1 | 为什么双向？ | 删除需前驱 O(1) |
| 2 | 哨兵价值？ | 免空判、边界统一 |
| 3 | LRU 三件套？ | 哈希 + 链表 + 哨兵 |
| 4 | LFU 结构？ | 双哈希（频次→LRU 链） |
| 5 | minFreq 维护？ | 空链且最小 → ++ |
| 6 | 循环队列？ | 数组 + 取模下标 |
| 7 | Java 推荐？ | ArrayDeque/LinkedHashMap |
| 8 | 节点存 key？ | 淘汰同步删哈希 |

### 7.3 常见错误

| 错误 | 后果 | 修正 |
|------|------|------|
| 单向链表删除 | O(n) | 双向 |
| 哨兵忘互指 | NPE | head.next=tail |
| 循环下标忘防负 | 越界 | (i-1+cap)%cap |
| LFU 忘 minFreq | 淘汰错 | 空链维护 |
| 节点不存 key | 哈希删不掉 | key 字段 |

---

> 🎯 **核心要点**：双向链表 = **O(1) 删除**（prev 指针）+ **哨兵**（边界统一）+ **复合**（哈希定位）。LRU/LFU 是旗舰应用，循环队列是数组变体，Java 内置（ArrayDeque/LinkedHashMap）是工程答案——"三件套 + 状态扩展"覆盖全部设计题。

---

**返回总览**：[00-算法汇总知识体系总览](../../../../05-综合与扩展/刷题实战与综合/算法汇总/00-算法汇总知识体系总览.md) | **上一篇**：[00-双向链表专题精要](00-双向链表专题精要.md) | **下一篇**：[02-双向链表高频题解](02-双向链表高频题解.md)
