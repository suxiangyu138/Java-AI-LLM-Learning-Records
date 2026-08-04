# 03 - put 与 get 流程详解

> **核心摘要**：put/get 是 HashMap 的核心操作——每个分支都是面试点（首次 put 初始化、key 相同覆盖、树化触发、扩容触发）。本文从源码级拆解完整流程。

> **前置阅读**：[[01-HashMap核心概念与数据结构]]、[[02-Hash计算与索引定位]]

---

## 📚 目录

1. [put 流程全景](#1-put-流程全景)
2. [put 源码逐行解析](#2-put-源码逐行解析)
3. [get 流程详解](#3-get-流程详解)
4. [remove 与 containsKey](#4-remove-与-containskey)
5. [树化触发条件](#5-树化触发条件)
6. [遍历与迭代器](#6-遍历与迭代器)
7. [核心要点](#7-核心要点)

---

## 1. put 流程全景

> **背景**：put 是 HashMap 最复杂的操作——涉及初始化、冲突、树化、扩容四个分支。
> **目的**：建立 put 流程的完整心智模型。
> **适用范围**：HashMap 写入场景。

```text
put(key, value) 流程
┌─────────────────────────────────────────────┐
│ ① hash(key)（扰动函数）                      │
├─────────────────────────────────────────────┤
│ ② 数组为空？→ 懒初始化（resize 创建容量 16）  │
├─────────────────────────────────────────────┤
│ ③ 定位下标：(n-1) & hash                    │
├─────────────────────────────────────────────┤
│ ④ 桶为空？→ 直接插入 ✅ 完成                 │
│    └── 桶非空 → 下一步                       │
├─────────────────────────────────────────────┤
│ ⑤ 遍历链表/树：                              │
│    ├── key 相同 → 覆盖 value（旧值返回）      │
│    └── key 不同 → 追加（尾插法）             │
├─────────────────────────────────────────────┤
│ ⑥ 链表长度 ≥ 8？→ 树化（需容量 ≥ 64）        │
├─────────────────────────────────────────────┤
│ ⑦ size > 阈值（12）？→ 扩容（resize）        │
└─────────────────────────────────────────────┘
```

---

## 2. put 源码逐行解析

```java
public V put(K key, V value) {
    return putVal(hash(key), key, value, false, true);
}

final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
               boolean evict) {
    Node<K, V>[] tab; Node<K, V> p; int n, i;

    // ① 懒初始化：数组为空 → resize 创建（容量 16）
    if ((tab = table) == null || (n = tab.length) == 0)
        n = (tab = resize()).length;

    // ② 定位下标 + 桶为空 → 直接插入
    if ((p = tab[i = (n - 1) & hash]) == null)
        tab[i] = newNode(hash, key, value, null);
    else {
        // ③ 桶非空：冲突处理
        Node<K, V> e; K k;
        if (p.hash == hash &&
            ((k = p.key) == key || (key != null && key.equals(k))))
            e = p;                          // 首节点 key 相同
        else if (p instanceof TreeNode)
            e = ((TreeNode<K, V>) p).putTreeVal(this, tab, hash, key, value);
                                            // 红黑树插入
        else {
            // ④ 链表遍历（尾插法）
            for (int binCount = 0; ; ++binCount) {
                if ((e = p.next) == null) {
                    p.next = newNode(hash, key, value, null);  // 尾部插入
                    if (binCount >= TREEIFY_THRESHOLD - 1)     // ⑤ ≥8 树化
                        treeifyBin(tab, hash);
                    break;
                }
                if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k))))
                    break;                  // 找到相同 key
                p = e;
            }
        }
        // ⑤ key 相同 → 覆盖 value
        if (e != null) {
            V oldValue = e.value;
            e.value = value;
            return oldValue;                // 返回旧值
        }
    }
    ++modCount;                             // 修改计数（迭代器快失败）
    // ⑥ 扩容触发
    if (++size > threshold)
        resize();
    return null;                            // 新插入返回 null
}
```

### 2.1 put 的关键细节

```text
put 流程细节
├── ① key 相同判定：hash 相等 + (引用相等 || equals)
│   ├── 先比 hash（快速排除）
│   └── 再比 equals（内容判定）
├── ② 覆盖返回旧值：put 返回旧值（新 key 返回 null）
├── ③ 尾插法：新节点插链表尾部（JDK 8——修死循环）
├── ④ 树化判断：binCount ≥ 7（即链表长度 ≥ 8）
├── ⑤ modCount：修改计数（fail-fast 迭代器）
└── ⑥ 扩容在插入后：size+1 > 阈值 → 扩容
```

---

## 3. get 流程详解

```java
public V get(Object key) {
    Node<K, V> e;
    return (e = getNode(hash(key), key)) == null ? null : e.value;
}

final Node<K, V> getNode(int hash, Object key) {
    Node<K, V>[] tab; Node<K, V> first, e; int n; K k;

    // ① 数组非空 + 桶非空（快速失败）
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (first = tab[(n - 1) & hash]) != null) {
        // ② 首节点命中（最常见）
        if (first.hash == hash &&
            ((k = first.key) == key || (key != null && key.equals(k))))
            return first;
        // ③ 冲突链查找
        if ((e = first.next) != null) {
            if (first instanceof TreeNode)
                return ((TreeNode<K, V>) first).getTreeNode(hash, key);  // 树
            do {
                if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k))))
                    return e;               // 链表遍历
            } while ((e = e.next) != null);
        }
    }
    return null;    // 未找到
}
```

```text
get 流程要点
├── ① 快速路径：数组空/桶空 → 直接 null
├── ② 首节点命中：O(1)（最常见）
├── ③ 冲突处理：树 O(log n) / 链表 O(n)
├── ④ key 判定：hash + (== || equals)
└── ⑤ ⚠️ 返回 null 的歧义：key 不存在 vs value 为 null
    └── 生产：containsKey 区分（或不用 null value）
```

---

## 4. remove 与 containsKey

```java
// remove：删除（返回旧值/null）
V removed = map.remove(key);

// remove(key, value)：仅当键值都匹配才删除
boolean removed = map.remove(key, value);

// containsKey：判断 key 存在（区分 null value）
boolean exists = map.containsKey(key);
// ⚠️ get 返回 null ≠ 不存在（value 可能是 null）——用 containsKey 确认

// getOrDefault：不存在给默认值
V value = map.getOrDefault(key, defaultValue);

// 安全取值（生产模式）
Order order = map.containsKey("ORD-1") ? map.get("ORD-1") : null;
// 或 getOrDefault + 业务默认

// ⚠️ remove 的实现细节（树/链表删除 + 树退化判断）
// 树删除后：节点 ≤ 6 → 退链表（见 05 篇）
```

---

## 5. 树化触发条件

> ⚠️ **树化双条件（高频面试陷阱）**：链表长度 ≥ 8 **且** 数组容量 ≥ 64——容量 < 64 时**先扩容而非树化**：

```java
final void treeifyBin(Node<K, V>[] tab, int hash) {
    int n, index; Node<K, V> e;
    // ① 容量 < 64：先扩容（不树化！）
    if (tab == null || (n = tab.length) < MIN_TREEIFY_CAPACITY)
        resize();                       // 扩容让链表分散
    // ② 容量 ≥ 64：转红黑树
    else if ((e = tab[index = (n - 1) & hash]) != null) {
        // ...链表 → 红黑树转换
    }
}
```

```text
树化双条件
├── ① 链表长度 ≥ 8（TREEIFY_THRESHOLD）
├── ② 容量 ≥ 64（MIN_TREEIFY_CAPACITY）
├── ③ 容量 < 64 时：扩容（元素分散 → 链表变短）
├── ④ 逻辑：小容量下扩容收益 > 树化收益
└── ⑤ 面试点：只说「长度 8 转树」是错的——漏了容量条件！
```

---

## 6. 遍历与迭代器

### 6.1 遍历方式

```java
// ① entrySet（最常用——同时拿 key/value）
for (Map.Entry<String, Integer> e : map.entrySet()) {
    e.getKey(); e.getValue();
}

// ② keySet + get（⚠️ 低效：每次 get 再哈希）
for (String key : map.keySet()) {
    map.get(key);       // 二次哈希（避免——用 entrySet）
}

// ③ values（只要值）
for (Integer v : map.values()) { ... }

// ④ forEach（Java 8+）
map.forEach((k, v) -> { ... });

// ⑤ Stream（过滤/转换）
map.entrySet().stream()
    .filter(e -> e.getValue() > 10)
    .toList();
```

### 6.2 遍历删除（fail-fast）

```java
// ❌ 错误：遍历中直接 remove → ConcurrentModificationException
for (String key : map.keySet()) {
    if (key.startsWith("x"))
        map.remove(key);    // ❌ fail-fast！
}

// ✅ 正确 1：Iterator.remove（安全）
Iterator<String> it = map.keySet().iterator();
while (it.hasNext()) {
    if (it.next().startsWith("x"))
        it.remove();        // ✅ 迭代器删除
}

// ✅ 正确 2：removeIf（Java 8+ 简洁）
map.keySet().removeIf(k -> k.startsWith("x"));

// ✅ 正确 3：先收集再删（两遍）
List<String> toRemove = map.keySet().stream()
    .filter(k -> k.startsWith("x")).toList();
toRemove.forEach(map::remove);

// ⚠️ modCount 机制：修改计数不匹配 → 抛异常（fail-fast 设计）
```

---

## 7. 核心要点

> 🎯 **核心要点**：
> 1. put 流程：懒初始化 → 定位 → 空桶直插/冲突遍历 → 覆盖/追加 → 树化 → 扩容
> 2. key 判定：hash 相等 +（引用相等 || equals）——先比 hash 快速排除
> 3. get 流程：快速路径（空数组/空桶/首节点）→ 冲突链（树/链表）
> 4. **树化双条件**：≥8 且容量 ≥64（容量 <64 先扩容——面试陷阱）
> 5. get 返回 null 有歧义（不存在 vs value 为 null）——用 containsKey
> 6. 遍历删除：Iterator.remove / removeIf（fail-fast 禁止直接 remove）

---

**下一模块**：[04-扩容机制resize](04-扩容机制resize.md) | **返回总览**：[00-HashMap知识体系总览](00-HashMap知识体系总览.md)
