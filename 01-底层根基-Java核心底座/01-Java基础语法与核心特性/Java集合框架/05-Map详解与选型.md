# 05 Map 详解与选型

> HashMap 是使用率第一的数据结构——本模块覆盖使用级 API 全景与七大 Map 实现选型，源码级深挖（红黑树、扩容 rehash、碰撞）交叉引用 Hash 知识系统

---

## 📚 目录

1. [Map 接口语义](#1-map-接口语义)
2. [HashMap：使用级 API 全景](#2-hashmap使用级-api-全景)
3. [LinkedHashMap：顺序保持与 LRU 缓存](#3-linkedhashmap顺序保持与-lru-缓存)
4. [TreeMap：有序 Map 与范围查询](#4-treemap有序-map-与范围查询)
5. [冷门四将：WeakHashMap / IdentityHashMap / EnumMap / Hashtable](#5-冷门四将weakhashmap--identityhashmap--enummap--hashtable)
6. [null 键值规则总表](#6-null-键值规则总表)
7. [Map 选型决策](#7-map-选型决策)

---

## 1. Map 接口语义

**Map 存储键值对（key-value）**：key 唯一（由 equals/hashCode 决定），value 可重复。

| 契约 | 说明 |
|------|------|
| 键唯一 | `put(k, v)` 时若 key 已存在 → 覆盖旧值并**返回旧值**；新键 → 返回 null |
| 键不可变 | key 的 hashCode 变化会导致"丢失"（见 03 模块第 6 节） |
| 三视图 | `keySet()`、`values()`、`entrySet()`——**活视图**，视图修改会写穿回 Map |
| 遍历首选 | `entrySet()` 同时拿到键值，避免二次 get |

```java
Map<String, Integer> scores = new HashMap<>();
scores.put("A", 90);
Integer old = scores.put("A", 95);        // 覆盖并返回 90
System.out.println(scores.get("A"));      // 95

// 遍历：entrySet 一次拿全
for (Map.Entry<String, Integer> e : scores.entrySet()) {
    System.out.println(e.getKey() + "=" + e.getValue());
}
// JDK 10+：var 简化
scores.forEach((k, v) -> System.out.println(k + "=" + v));
```

> 🎯 **核心要点**：`put` 返回"旧值或 null"这个返回值经常被忽略——实现"如果键存在就不覆盖"用 `putIfAbsent`，实现"存在就累加"用 `merge`，**不要用 get 再 put 的弱并发模式**。

---

## 2. HashMap：使用级 API 全景

**结构速览**（JDK 8+，源码级详见 `Java有关Hash的一切/02-HashMap源码深度剖析.md`）：

```text
数组 + 链表 + 红黑树
- 数组默认 16，负载因子 0.75 → 扩容阈值 12
- 链表长度 ≥ 8 且数组容量 ≥ 64 → 转红黑树（最坏 O(n) → O(log n)）
- 红黑树节点 ≤ 6 → 转回链表
- 扩容为 2 倍，重排 rehash
```

**现代 API 家族**（JDK 8+，改写代码风格的关键）：

| 方法 | 语义 | 传统写法（弱） | 现代写法（强） |
|------|------|--------------|--------------|
| `getOrDefault` | 取不到返回默认值 | `map.containsKey ? map.get : 0` | `map.getOrDefault(k, 0)` |
| `putIfAbsent` | 键不存在才放入 | `if (!map.containsKey(k)) map.put(k, v)` | `map.putIfAbsent(k, v)` |
| `computeIfAbsent` | 键缺失时**计算**并放入 | get→判空→new→put（4 步+并发隐患） | `map.computeIfAbsent(k, kk -> new ArrayList<>())` |
| `computeIfPresent` | 键存在时重算 | 三重判断 | `map.computeIfPresent(k, (kk, v) -> v + 1)` |
| `compute` | 无论存在与否都算 | — | `map.compute(k, (kk, v) -> v == null ? 1 : v + 1)` |
| `merge` | 合并，常用于计数累加 | get→判空→put | `map.merge(k, 1, Integer::sum)` |

**经典案例：按 key 分组**——`computeIfAbsent` 是"分组"的官方标准写法：

```java
// 需求：List<Order> 按 status 分组
Map<String, List<Order>> byStatus = new HashMap<>();
for (Order o : orders) {
    byStatus.computeIfAbsent(o.status(), k -> new ArrayList<>()).add(o);
}
// computeIfAbsent 是原子操作（JDK 8 默认方法内部同步语义下线程安全度优于手动 get-put）

// 需求：词频统计
Map<String, Integer> freq = new HashMap<>();
for (String w : words) freq.merge(w, 1, Integer::sum);   // 存在则 v+1，不存在则 1
```

**容量与性能实践**：

```java
// 预估容量公式：expectedSize / 0.75 + 1 —— 避免"刚放满就扩容"的反复 rehash
Map<String, Object> m = new HashMap<>(200);          // 预期 150 个键 → 150/0.75=200
```

> ⚠️ 迭代顺序不可依赖；`put` 大量键时**预估容量**可省 20+ 次 rehash；`HashMap` 允许 1 个 null 键 + 任意 null 值。

---

## 3. LinkedHashMap：顺序保持与 LRU 缓存

**底层**：HashMap + **双向链表**记录顺序。两种模式：

| 模式 | 构造参数 | 顺序依据 | 用途 |
|------|---------|---------|------|
| 插入序（默认） | `new LinkedHashMap<>()` | 插入顺序（重复 put 不改变位置） | 保序 Map |
| 访问序 | `new LinkedHashMap<>(16, 0.75f, true)` | **最近访问**顺序（get/put 都前移） | **LRU 缓存** |

**手写 LRU 缓存**（面试高频，只需重写 `removeEldestEntry`）：

```java
public class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int maxSize;

    public LRUCache(int maxSize) {
        super(16, 0.75f, true);          // 访问序模式
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;         // 超过容量 → 自动移除最久未访问的
    }
}

LRUCache<String, String> cache = new LRUCache<>(3);
cache.put("a", "1"); cache.put("b", "2"); cache.put("c", "3");
cache.get("a");                            // 访问 a → a 前移到最近
cache.put("d", "4");                       // 超过 3 → 移除最久未访问的 b
System.out.println(cache.keySet());        // [c, a, d]
```

> 🎯 **核心要点**：`removeEldestEntry` 在每次 put 后被回调——return true 表示"最老的该被移除"，这就是 LRU 的全部机制。注意：**不是线程安全**，多线程 LRU 需外层加锁或换 Caffeine 等缓存框架。

---

## 4. TreeMap：有序 Map 与范围查询

**底层**：红黑树（自平衡二叉搜索树），按 key 的自然序或构造器 `Comparator` 排序。

| 能力 | 方法 | 示例 |
|------|------|------|
| 有序遍历 | `firstKey/lastKey` | 最小/最大键 |
| 导航 | `floorKey`（≤）、`ceilingKey`（≥）、`lowerKey`（<）、`higherKey`（>） | `map.ceilingKey(90)` → 第一个 ≥90 的分数键 |
| 范围视图 | `subMap(from, to)`（左闭右开）、`headMap`、`tailMap` | 活视图，支持再次操作 |
| 首尾弹出 | `pollFirstEntry` / `pollLastEntry` | 双端弹出 |

**实战：按时间范围查询**（键为时间戳）：

```java
TreeMap<Long, LogEntry> logs = new TreeMap<>();
logs.put(1700000000L, e1); logs.put(1700000100L, e2); logs.put(1700000200L, e3);

// 查 [start, end) 区间日志 —— O(log n + k)，秒杀全量遍历
SortedMap<Long, LogEntry> range = logs.subMap(1700000050L, 1700000150L);
// 查"最后一个 ≤ 当前时间"的配置版本
LogEntry v = logs.floorEntry(System.currentTimeMillis() / 1000).getValue();
```

> ⚠️ **TreeMap 约束**：key **不可为 null**（比较器无法比较 null，抛 NPE）；key 必须实现 `Comparable` 或构造时传 `Comparator`；排序与 `equals` 建议保持一致（契约同 TreeSet）。复杂度 O(log n)——**需要有序/范围查询才用它，否则 HashMap 更快**。

---

## 5. 冷门四将：WeakHashMap / IdentityHashMap / EnumMap / Hashtable

### 5.1 WeakHashMap：弱引用键缓存

**机制**：key 使用**弱引用**（`WeakReference`）——key 对象仅被 WeakHashMap 引用时，GC 就会回收该 key 对应的条目（`expungeStaleEntries` 在访问时清除）。

```java
WeakHashMap<Image, byte[]> cache = new WeakHashMap<>();
Image img = new Image("big.jpg");
cache.put(img, loadBytes(img));
img = null;                    // 强引用断开 → 下次 GC 后条目被自动清除

// 用途：缓存与生命周期绑定（如组件关联数据）
// 坑：value 若强引用 key（如 value 内部持 key 引用），key 永不回收 → 值应只持有 key 的弱引用
```

### 5.2 IdentityHashMap：按引用身份比较

**机制**：比较用 `==` 而非 `equals`，hashCode 用 `System.identityHashCode`。场景：需要**区分 equals 相同但不同实例**的键——如 Spring 的单例注册、序列化时跟踪已处理对象（避免循环引用）。

```java
IdentityHashMap<String, Integer> m = new IdentityHashMap<>();
m.put(new String("a"), 1);
m.put(new String("a"), 2);     // ✅ 两个不同实例 → 两个条目（普通 HashMap 会合并）
```

### 5.3 EnumMap：枚举键的极致优化

**机制**：内部用**数组按枚举序索引**（枚举序 → 数组下标），无哈希计算，O(1) 且迭代按枚举声明序，性能与内存碾压 HashMap。

```java
Map<Day, String> schedule = new EnumMap<>(Day.class);
schedule.put(Day.MON, "开会");
// 场景：按枚举做配置（状态 → 处理器）、按月统计
```

### 5.4 Hashtable：遗留类为什么不推荐

| 维度 | Hashtable | HashMap |
|------|:---------:|:-------:|
| 线程安全 | 方法级 synchronized（全局锁） | 不安全（要安全用 CHM） |
| null 键值 | **禁止** | 允许（1 个 null 键） |
| 扩容 | 2 倍 + 1 | 2 倍 |
| 迭代器 | fail-fast | fail-fast |
| 现状 | 遗留类，官方建议 ConcurrentHashMap | 现代默认 |

> 🎯 **核心要点**：Hashtable 的问题不是"不安全"，而是"用最粗暴的全局锁换来了可有可无的同步"——性能差且无并发收益。**单线程用 HashMap，多线程用 ConcurrentHashMap，Hashtable 没有任何生存空间**（面试必答点）。

---

## 6. null 键值规则总表

| Map | null 键 | null 值 | 说明 |
|-----|:-------:|:-------:|------|
| HashMap | ✅ 1 个 | ✅ | null 键 hash 固定为 0 |
| LinkedHashMap | ✅ 1 个 | ✅ | 同 HashMap |
| TreeMap | ❌ | ✅ | 键参与比较，null 无法比较 |
| Hashtable | ❌ | ❌ | 遗留约束 |
| **ConcurrentHashMap** | ❌ | ❌ | 避免 get 返回 null 无法区分"没有"与"值是 null" |
| EnumMap | ❌ | ✅ | 键必须为枚举 |
| WeakHashMap | ✅ 1 个 | ✅ | 同 HashMap |

> ⚠️ **CHM 禁止 null 的原因**（面试高频）：`map.get(k)` 返回 null 时无法区分"键不存在"与"键存在但值为 null"，而并发场景下无法靠 `containsKey` 再确认（状态可能已变化）——所以干脆从源头禁止。

---

## 7. Map 选型决策

```text
需要什么能力？
├── 快速键值存取、无顺序要求 → HashMap ✅
├── 保持插入/访问顺序 → LinkedHashMap（LRU 用访问序 + removeEldestEntry）
├── 按键有序、范围查询 → TreeMap
├── 键是枚举 → EnumMap ✅（性能碾压）
├── 键需要"引用身份" → IdentityHashMap
├── 缓存与 key 生命周期绑定 → WeakHashMap
├── 并发安全 → ConcurrentHashMap（见 06 模块）
└── 需要"最近使用优先" → LinkedHashMap 访问序 或 Caffeine
```

**Map 全家福复杂度总表**：

| 实现 | put | get | 顺序 | 线程安全 |
|------|:---:|:---:|------|:-------:|
| HashMap | O(1) | O(1) | 无 | ❌ |
| LinkedHashMap | O(1) | O(1) | 插入/访问序 | ❌ |
| TreeMap | O(log n) | O(log n) | 键序 | ❌ |
| EnumMap | O(1) | O(1) | 枚举序 | ❌ |
| ConcurrentHashMap | O(1) | O(1) | 无 | ✅ |
| Hashtable | O(1) | O(1) | 无 | ✅（全局锁，不推荐） |

---

**下一模块**：[06-并发集合与安全同步](./06-并发集合与安全同步.md) / **返回总览**：[00-Java集合框架知识体系总览](./00-Java集合框架知识体系总览.md) / **交叉导航**：[Java有关Hash的一切/02-HashMap源码深度剖析](../Java有关Hash的一切/02-HashMap源码深度剖析.md)
