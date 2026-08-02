# 03 - Hash 家族集合全景

> 定位：盘点以哈希为核心的所有集合类——HashSet 去重原理、LinkedHashMap LRU、WeakHashMap 缓存、IdentityHashMap 引用相等，附完整选型指南

## 📚 目录

1. [Hash 家族图谱](#1-hash-家族图谱)
2. [HashSet 与 LinkedHashSet：去重原理](#2-hashset-与-linkedhashset去重原理)
3. [LinkedHashMap：有序性与 LRU](#3-linkedhashmap有序性与-lru)
4. [WeakHashMap：弱引用缓存](#4-weakhashmap弱引用缓存)
5. [IdentityHashMap：引用相等性](#5-identityhashmap引用相等性)
6. [EnumMap 与其他](#6-enummap-与其他)
7. [遍历方式与性能对比](#7-遍历方式与性能对比)
8. [选型指南](#8-选型指南)
9. [面试高频考点](#9-面试高频考点)

---

## 1. Hash 家族图谱

```
                        Hash 家族
                            │
          ┌─────────┬───────┼────────┬──────────┐
          │         │       │        │          │
      Map 类       Set 类  弱引用   引用相等  枚举专用
          │         │       │        │          │
    HashMap     HashSet    Weak-  Identity-   EnumMap
    LinkedHash-  Linked-   HashMap HashMap
    Map          HashSet
    Hashtable    (已过时)
    ConcurrentHashMap → 见 04 模块
```

| 类 | 底层 | 判等方式 | 有序 | 特殊点 |
|----|------|---------|------|--------|
| HashMap | 数组+链表+红黑树 | equals | 否 | 通用首选 |
| HashSet | HashMap（key 复用） | equals | 否 | 去重 |
| LinkedHashMap | HashMap+双向链表 | equals | 插入/访问序 | LRU 缓存 |
| WeakHashMap | 数组+链表（弱引用 key） | equals | 否 | key 可被 GC |
| IdentityHashMap | 数组（线性探测） | == | 否 | 引用相等 |
| EnumMap | 数组（ordinal 下标） | 枚举相等 | 枚举声明序 | O(1) 极快 |

---

## 2. HashSet 与 LinkedHashSet：去重原理

### 2.1 HashSet 本质是"阉割版 HashMap"

```java
// HashSet 源码（精简）
public class HashSet<E> extends AbstractSet<E> {
    private transient HashMap<E,Object> map;   // 复用 HashMap
    private static final Object PRESENT = new Object();  // 占位 value

    public boolean add(E e) {
        return map.put(e, PRESENT) == null;    // key 是元素，value 恒为 PRESENT
    }
}
```

**去重原理**：add 实际是 `map.put(e, PRESENT)`——返回 null 说明之前不存在（新增），返回旧值说明已存在（覆盖占位符）。

> 💡 **去重的真正裁判是 equals/hashCode**：`new HashSet<>().add("a")` 再 add 一个 `new String("a")` 会失败——String 重写了 equals，所以"内容相同"即重复。自定义类不重写 equals，两个内容相同的对象不会被去重。

### 2.2 与 List 去重的性能对比

```java
// ❌ O(n²)：每次 contains 线性扫描
List<Integer> list = ...;
list.stream().distinct().collect(toList());

// ✅ O(n)：哈希查找
Set<Integer> set = new HashSet<>(list);
```

### 2.3 LinkedHashSet

`HashSet + 双向链表`：去重且保持**插入顺序**。底层是 LinkedHashMap。适用：需要保序去重（如最近访问的 UV 统计）。

---

## 3. LinkedHashMap：有序性与 LRU

### 3.1 数据结构

```
LinkedHashMap = HashMap + 双向链表（贯穿所有节点的额外指针）
head ◄─► Node ◄─► Node ◄─► ... ◄─► tail
(最老)                          (最新)
```

两种模式：
- **插入顺序**（默认）：迭代顺序 = 插入顺序（再次 put 已有 key 不改变位置）
- **访问顺序**（`accessOrder = true`）：每次 get/put 把节点移到尾部

### 3.2 LRU 缓存的优雅实现

```java
// ⭐ Java 最经典的 LRU 实现：继承 + removeEldestEntry
class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int maxCapacity;

    public LRUCache(int maxCapacity) {
        super(16, 0.75f, true);      // accessOrder = true → 访问即刷新
        this.maxCapacity = maxCapacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxCapacity; // 容量超标 → 淘汰最久未访问
    }
}

LRUCache<String, Integer> cache = new LRUCache<>(3);
cache.put("a", 1); cache.put("b", 2); cache.put("c", 3);
cache.get("a");                       // a 移到链表尾部（最近使用）
cache.put("d", 4);                    // 淘汰最老节点 b
System.out.println(cache.keySet());   // [c, a, d]
```

> ⚠️ **注意**：LinkedHashMap 不是线程安全的。并发场景用 `Collections.synchronizedMap` 包裹或 Caffeine 等专门缓存库。

### 3.3 SequencedMap（Java 21+）

Java 21 新增 `SequencedMap` 接口，LinkedHashMap/TreeMap 均实现，有序操作统一化：

```java
LinkedHashMap<String, Integer> m = new LinkedHashMap<>() {{
    put("a", 1); put("b", 2); put("c", 3);
}};
m.firstEntry();        // a=1（最老）
m.lastEntry();         // c=3（最新）
m.pollFirstEntry();    // 取出并移除最老
m.reversed();          // 反转顺序视图
```

---

## 4. WeakHashMap：弱引用缓存

### 4.1 原理

```java
// WeakHashMap 内部：Entry 继承 WeakReference<Object>
// key 是弱引用：堆上除 WeakHashMap 外无强引用 → GC 时被回收
private static class Entry<K,V> extends WeakReference<Object> implements Map.Entry<K,V>
```

回收流程：GC 回收 key → key 进入 `ReferenceQueue` → 下次操作时 `expungeStaleEntries()` 清除对应 Entry。

### 4.2 典型场景：值对象缓存

```java
// 经典用法：缓存 key 的生命周期交给 GC
WeakHashMap<Thread, Connection> threadCache = new WeakHashMap<>();
threadCache.put(Thread.currentThread(), conn);  // 线程消亡 → 缓存自动清空
```

| 场景 | 合适？ | 原因 |
|------|:---:|------|
| 线程局部缓存 | ✅ | 线程销毁自动清 |
| 全局热点缓存 | ❌ | 热点 key 随时被回收，缓存命中率低 |
| 大对象缓存 | ⚠️ | 可缓解内存压力，但 value 的强引用可能拖延 Entry 回收 |

> 💡 **设计要点**：value 不能强引用 key（否则 key 永不回收，Entry 白占内存）。`Entry` 只弱引用 key，value 是普通强引用字段。

---

## 5. IdentityHashMap：引用相等性

### 5.1 核心差异：== 代替 equals

```java
// IdentityHashMap 内部实现：
//   hashCode 用 System.identityHashCode(key)（引用哈希）
//   相等判断用 key1 == key2（引用比较），不走 equals
IdentityHashMap<String, Integer> map = new IdentityHashMap<>();
map.put(new String("a"), 1);
map.put(new String("a"), 2);
map.size();  // 2 ！两个不同引用，即使内容相同也不视为重复
```

### 5.2 底层结构：开放寻址（线性探测）

与 HashMap 的链地址法不同，IdentityHashMap 用**数组 + 线性探测**（元素直接存在数组中，冲突时顺延到下一个空位）。这也是它不用考虑"equals 与 hashCode 的一致性"的原因——它根本不调 equals。

### 5.3 使用场景

| 场景 | 示例 |
|------|------|
| 序列化框架去重 | 按对象引用判断"是否已序列化"（JDK Serialization 内部） |
| Spring 单例池 | 按 bean 引用判断单例 |
| 按引用而非内容管理对象 | JVM 内省、debugger、对象图遍历 |

---

## 6. EnumMap 与其他

### 6.1 EnumMap：数组 + ordinal 下标

```java
// 枚举数量有限（默认最多 64 个？不——不限制，但一般很少），直接映射为数组
EnumMap<Status, String> map = new EnumMap<>(Status.class);
// 内部：Object[] vals，下标 = status.ordinal()
// 获取：vals[key.ordinal()]  → 连哈希都不用算，真 O(1)
```

| 优点 | 缺点 |
|------|------|
| 无哈希计算、无扩容、极快 | 仅限枚举 key |
| 内存紧凑（数组） | ordinal 排序即自然顺序 |

### 6.2 Hashtable / Properties（已过时）

Hashtable 是 JDK 1.0 遗留：全表 synchronized、不允许 null key/value、容量 11 扩容 2n+1（非 2 的幂，需真取模）。**除非维护老代码，否则一律不用**——单线程用 HashMap，多线程用 [ConcurrentHashMap](04-ConcurrentHashMap并发哈希剖析.md)。Properties 仅用于 `.properties` 配置文件。

---

## 7. 遍历方式与性能对比

```java
Map<String, Integer> map = new HashMap<>();

// 方式一：entrySet（✅ 推荐，一次遍历拿到 key+value，无二次查找）
for (Map.Entry<String, Integer> e : map.entrySet())
    System.out.println(e.getKey() + "=" + e.getValue());

// 方式二：keySet + get（❌ 每次 get 都重新 hash 一次，性能差）
for (String k : map.keySet()) System.out.println(k + "=" + map.get(k));

// 方式三：values（只要值）
for (Integer v : map.values()) { }

// 方式四：JDK 8 forEach
map.forEach((k, v) -> System.out.println(k + "=" + v));

// 方式五：Stream（需要流水线处理时）
map.entrySet().stream().filter(e -> e.getValue() > 10).toList();
```

| 方式 | 性能 | 说明 |
|------|:---:|------|
| entrySet | ⭐ 最优 | 一次迭代 |
| keySet + get | ❌ 最差 | 二次 hash，元素多时明显 |
| forEach | ⭐ 同 entrySet | 内部也是 entrySet 迭代 |
| Stream | 取决于操作 | 需要中间处理时用 |

> ⚠️ **fail-fast**：遍历中结构修改（put/remove）抛 `ConcurrentModificationException`——`modCount` 检测到变化即失败。需要边遍历边删除：迭代器 `iterator.remove()` 或 `removeIf`。

---

## 8. 选型指南

```
需求：
├─ 去重？
│   ├─ 要保序 → LinkedHashSet
│   └─ 不要 → HashSet
├─ 键值对？
│   ├─ 并发 → ConcurrentHashMap
│   ├─ 排序/范围 → TreeMap
│   ├─ 保序/LRU → LinkedHashMap
│   ├─ key 可被 GC → WeakHashMap
│   ├─ 按引用区分 → IdentityHashMap
│   ├─ 枚举 key → EnumMap
│   └─ 其他 → HashMap（默认）
```

| 场景 | 推荐 | 一句话理由 |
|------|------|-----------|
| 通用键值对 | HashMap | 综合最优 |
| 去重 | HashSet | 底层复用 HashMap |
| 保序去重 | LinkedHashSet | 去重 + 插入序 |
| LRU 缓存 | LinkedHashMap(accessOrder=true) | 重写 removeEldestEntry |
| 线程局部缓存 | WeakHashMap | 生命周期随线程消亡 |
| 对象图/引用去重 | IdentityHashMap | 引用相等 |
| 枚举统计 | EnumMap | 真 O(1) |
| 高并发 | ConcurrentHashMap | 见 04 模块 |

---

## 9. 面试高频考点

**Q1: HashSet 怎么保证不重复？** 底层是 HashMap，add 即 put(e, PRESENT)；重复判定依赖 equals/hashCode。

**Q2: LinkedHashMap 实现 LRU？** accessOrder=true + 重写 removeEldestEntry(size>max)，访问把节点移到尾部。

**Q3: WeakHashMap key 回收机制？** key 弱引用，GC 后进 ReferenceQueue，下次操作 expungeStaleEntries 清理。

**Q4: IdentityHashMap 与 HashMap 区别？** 用 == 和 identityHashCode，不走 equals；底层线性探测。场景：序列化/引用去重。

**Q5: EnumMap 为什么最快？** ordinal 直接做数组下标，连哈希都不用算。

**Q6: 遍历中能 remove 吗？** 集合方法不行（fail-fast），用迭代器 remove() 或 removeIf。

---

> 🎯 **核心要点**：Hash 家族万变不离其宗——核心哈希结构都是"数组 + 冲突处理"，差异只在于**判等规则**（equals / == / 弱引用）、**附加结构**（双向链表 / 线性探测）与**适用场景**。

---

**下一模块**：[04-ConcurrentHashMap并发哈希剖析](04-ConcurrentHashMap并发哈希剖析.md)（并发场景的哈希） / **返回总览**：[00-Hash总览](00-Hash总览.md)
