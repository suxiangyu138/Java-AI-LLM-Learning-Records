# 02 - Hash 计算与索引定位

> **核心摘要**：HashMap 的性能根基是「哈希定位」——扰动函数让 hash 分布更均匀、位运算 `(n-1) & hash` 让取模更快。为什么容量必须是 2 的幂？扰动函数有什么用？本文从源码拆解 Hash 计算全链路。

> **前置阅读**：[[01-HashMap核心概念与数据结构]]

---

## 📚 目录

1. [索引定位的本质](#1-索引定位的本质)
2. [扰动函数](#2-扰动函数)
3. [为什么容量是 2 的幂](#3-为什么容量是-2-的幂)
4. [tableSizeFor 实现](#4-tablesizefor-实现)
5. [哈希冲突的数学](#5-哈希冲突的数学)
6. [null key 的处理](#6-null-key-的处理)
7. [自定义 key 的哈希要求](#7-自定义-key-的哈希要求)
8. [核心要点](#8-核心要点)

---

## 1. 索引定位的本质

> **背景**：HashMap 的查询 = 把 key 映射到数组下标——「哈希定位」决定 O(1) 与否。
> **目的**：理解「key → hash → 下标」的完整链路。
> **适用范围**：HashMap 的全部读写操作。

```text
定位链路（两次计算）
┌────────┐  hashCode()  ┌────────┐  扰动函数  ┌────────┐  位运算  ┌────────┐
│ key    │ ──────────→ │ hash   │ ────────→ │ 扰动后  │ ───────→ │ 下标    │
│ "abc"  │  (Object)   │ 原始值  │  ^>>>16   │  hash   │ (n-1)&  │  5     │
└────────┘             └────────┘           └────────┘          └────────┘

两步计算
├── ① hash(key)：扰动函数（高 16 位参与）
└── ② index = (n - 1) & hash：位运算定位

源码
static final int hash(Object key) {
    int h;
    return (key == null) ? 0
         : (h = key.hashCode()) ^ (h >>> 16);   // 扰动
}
// 定位（putVal/getNode 中）：
// i = (n - 1) & hash
```

---

## 2. 扰动函数

### 2.1 原理

> 🎯 **扰动函数**：`hash = hashCode() ^ (hashCode() >>> 16)`——高 16 位与低 16 位异或：

```text
为什么要扰动（高 16 位参与）
├── ① 问题：数组容量小（默认 16——只用低 4 位）
│   ├── (n-1) & hash 只用到 hash 的低位
│   └── 如果 hashCode 低位相同 → 全部冲突同一桶！
├── ② 解法：把高 16 位「混合」进低 16 位
│   ├── h ^ (h >>> 16)：高 16 位与低 16 位异或
│   └── 高位信息参与定位（分布更均匀）
├── ③ 示例：
│   hashCode: 0x12345678
│   >>> 16:   0x00001234
│   XOR:      0x12345678 ^ 0x00001234 = 0x1234444C（混合后）
└── ④ 金句：扰动 = 「让高位参与低位」——小容量下分布均匀

JDK 7 vs JDK 8 扰动
├── JDK 7：9 次位运算（4 次移位 + 5 次异或）
├── JDK 8：2 次（1 移位 + 1 异或）
└── 简化原因：树化兜底（长冲突有红黑树）+ 扰动性价比
```

### 2.2 实际价值（反例）

```java
// 反例：没有扰动时（hashCode 低位相同）
// key1.hashCode() = 0x00000001
// key2.hashCode() = 0x00000011
// key3.hashCode() = 0x00000021
// 容量 16：(n-1) & hash 只看低 4 位 → 全部命中桶 1！（全冲突）

// 扰动后：
// hash1 = 0x00000001 ^ 0x00000000 = 1
// hash2 = 0x00000011 ^ 0x00000000 = 0x11 = 17 → 桶 1
// hash3 = 0x00000021 ^ 0x00000000 = 0x21 = 33 → 桶 1
// ⚠️ 高位相同则扰动也无用（真正问题是 hashCode 设计）
// 结论：扰动优化「低位分布」，不拯救「差的 hashCode」
```

---

## 3. 为什么容量是 2 的幂

> 🎯 **容量必须是 2 的幂**——位运算 `(n - 1) & hash` 代替取模 `hash % n`：

```text
2 的幂的两个价值
├── ① 位运算代替取模（性能）
│   ├── (n-1) & hash：位运算（纳秒级）
│   ├── hash % n：取模（除法——慢）
│   └── n=16：1111 & hash（截取低 4 位）
├── ② 分布均匀（当 hash 均匀时）
│   ├── n 为 2 的幂：低 k 位全覆盖（0 到 n-1）
│   └── n 非 2 的幂：部分位永远用不到（浪费桶）
└── ③ 扩容友好（高低位拆分——见 04 篇）
    ├── 容量翻倍 = 多一位参与
    └── 元素只需判断「新位」——免重算

⚠️ 自定义初始容量的处理
├── new HashMap<>(10)：不是直接用 10！
├── tableSizeFor 转成 ≥ 10 的最近 2 的幂（16）
└── 结论：任何传入容量都被规范为 2 的幂
```

---

## 4. tableSizeFor 实现

```java
// 把任意容量转为 ≥ 传入值的最近 2 的幂
static final int tableSizeFor(int cap) {
    int n = cap - 1;                    // 防 cap 本身是 2 的幂
    n |= n >>> 1;                       // 高位逐步扩散
    n |= n >>> 2;
    n |= n >>> 4;
    n |= n >>> 8;
    n |= n >>> 16;
    return (n < 0) ? 1
         : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY
         : n + 1;
}

// 示例：tableSizeFor(10)
// 10 - 1 = 9（1001）
// 逐步或扩散：1111 = 15
// 15 + 1 = 16 ✅（≥10 的最近 2 的幂）

// 示例：tableSizeFor(16) → 16 - 1 = 15 → 扩散 15 → +1 = 16
// （不减 1 会得到 32——所以 cap - 1 很重要）
```

```text
tableSizeFor 要点
├── ① 目的：任意容量 → 2 的幂
├── ② 实现：位运算扩散（5 次 >>> + |）
├── ③ cap - 1：防「已经是 2 的幂」被翻倍
└── ④ 结果：≥ cap 的最小 2 的幂
```

---

## 5. 哈希冲突的数学

### 5.1 冲突概率（泊松分布）

> **背景**：哈希冲突不可避免——理解冲突的数学规律（负载因子 0.75 的设计依据）：

```text
泊松分布（负载因子 0.75 时链表长度概率）
├── 长度 0：约 60.7%
├── 长度 1：约 30.3%
├── 长度 2：约 7.6%
├── 长度 3：约 1.2%
├── ...
├── 长度 8：约 0.00000006（千万分之六！）
└── 结论：正常使用链表长度 ≥ 8 几乎不可能
    → 树化是「防极端攻击」而非常规路径

极端场景（触发树化）
├── 恶意 key（hashCode 相同——哈希碰撞攻击）
├── 差的 hashCode 实现（业务 bug）
└── 有界数据（固定 hash 的低位相同）
```

### 5.2 冲突的代价

```text
冲突 → 链表/树的查询代价
├── 无冲突：O(1)
├── 短链表：O(n)（n 小可忽略）
├── 长链表：O(n) 退化（查询慢）
├── 红黑树：O(log n)（树化兜底）
└── 金句：HashMap 的 O(1) 前提 = hash 分布均匀
```

---

## 6. null key 的处理

```java
// null key 的 hash 固定为 0
static final int hash(Object key) {
    int h;
    return (key == null) ? 0
         : (h = key.hashCode()) ^ (h >>> 16);
}

// null key 总是存桶 0
map.put(null, "value");       // hash = 0 → 桶 0
map.get(null);                // 桶 0 查找

// 特点
├── ① 只允许 1 个 null key（后放覆盖前放）
├── ② 桶 0 的链表头部区域
├── ③ null value 允许多个（与 key 无关）
└── ④ 对比：ConcurrentHashMap/Hashtable 不允许 null
    └── 原因：并发下 get 返回 null 无法区分「不存在」与「值为 null」
```

---

## 7. 自定义 key 的哈希要求

### 7.1 黄金法则

```text
自定义 key 的三条要求（生产铁律）
├── ① 重写 equals + hashCode（一起！不重写 hashCode 无法定位）
├── ② 与 equals 保持一致：
│   ├── equals 相等 → hashCode 必须相等（同一桶）
│   └── hashCode 相等 → equals 可不相等（冲突正常）
├── ③ 不可变（final 字段）：
│   ├── 修改 key → hash 变化 → get 不到（永久丢失）
│   └── 推荐：record/不可变类
```

### 7.2 示例

```java
// ✅ 正确的自定义 key（不可变 + equals/hashCode 一致）
record OrderKey(String orderId, Long userId) { }
// record 自动生成：equals/hashCode/toString（不可变）

// ❌ 错误：可变 key
class BadKey {
    String id;
    // 无 equals/hashCode 重写 → 引用比较（永远 get 不到）
}
// ❌ 错误：hashCode 与 equals 不一致
class WorseKey {
    String id;
    @Override public boolean equals(Object o) { ... }
    // ⚠️ 没重写 hashCode → equals 相等但 hash 不同 → 不同桶！
}

// 生产实践
Map<OrderKey, Order> orderCache = new HashMap<>();
orderCache.put(new OrderKey("ORD-1", 100L), order);
Order found = orderCache.get(new OrderKey("ORD-1", 100L));  // ✅ 找到
```

---

## 8. 核心要点

> 🎯 **核心要点**：
> 1. 定位链路：hashCode → 扰动（^>>>16）→ `(n-1) & hash`（位运算）
> 2. **扰动函数让高位参与低位**——小容量下分布均匀（JDK 8 简化为 2 次）
> 3. **容量 2 的幂三价值**：位运算代替取模 + 分布均匀 + 扩容友好
> 4. tableSizeFor：任意容量 → 最近 2 的幂（cap-1 防翻倍）
> 5. 泊松分布：链表 ≥ 8 概率千万分之六——树化是防极端不是常规
> 6. null key：hash=0 存桶 0（HashMap 允许；ConcurrentHashMap 不允许）
> 7. 自定义 key 铁律：不可变 + equals/hashCode 一致（record 最安全）

---

**下一模块**：[03-put与get流程详解](03-put与get流程详解.md) | **返回总览**：[00-HashMap知识体系总览](00-HashMap知识体系总览.md)
