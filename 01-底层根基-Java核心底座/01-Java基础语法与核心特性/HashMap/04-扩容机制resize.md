# 04 - 扩容机制 resize

> **核心摘要**：扩容是 HashMap 最精妙的设计——触发条件、容量翻倍、高低位拆分（免重算 hash）。JDK 8 的高低位拆分既修复了 JDK 7 的死循环，又提升了扩容效率。本文从源码拆解 resize 全流程。

> **前置阅读**：[[01-HashMap核心概念与数据结构]]、[[03-put与get流程详解]]

---

## 📚 目录

1. [扩容触发与时机](#1-扩容触发与时机)
2. [扩容流程全景](#2-扩容流程全景)
3. [高低位拆分原理](#3-高低位拆分原理)
4. [JDK 7 vs JDK 8 扩容](#4-jdk-7-vs-jdk-8-扩容)
5. [扩容的死循环问题](#5-扩容的死循环问题)
6. [扩容的性能代价](#6-扩容的性能代价)
7. [生产避坑：容量预估](#7-生产避坑容量预估)
8. [核心要点](#8-核心要点)

---

## 1. 扩容触发与时机

> **背景**：HashMap 的元素超过阈值时扩容——「数组不够用了，换更大的」。
> **目的**：理解扩容的触发与代价。
> **适用范围**：所有 HashMap 写入场景。

```text
扩容触发
├── ① 条件：size > threshold（阈值 = 容量 × 负载因子）
│   ├── 默认：容量 16 → 阈值 12
│   └── 第 13 个元素触发扩容（16 → 32）
├── ② 时机：插入之后（putVal 尾部判断）
├── ③ 新容量：旧容量 × 2（2 的幂保持）
├── ④ 新阈值：新容量 × 负载因子
└── ⑤ 代价：重新分配 + 元素迁移（O(n)）

为什么必须扩容
├── ① 冲突增加：元素多了链表变长（查询退化）
├── ② 保持 O(1)：容量足够 → 分布均匀
└── ③ 触发频率：负载因子 0.75 平衡「空间 vs 冲突」
```

---

## 2. 扩容流程全景

```java
final Node<K, V>[] resize() {
    Node<K, V>[] oldTab = table;
    int oldCap = (oldTab == null) ? 0 : oldTab.length;
    int oldThr = threshold;

    // ① 计算新容量与阈值
    int newCap, newThr = 0;
    if (oldCap > 0) {
        // 普通扩容：容量 × 2
        if (oldCap >= MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;   // 到上限不再扩
            return oldTab;
        }
        newCap = oldCap << 1;                // ×2
        newThr = oldThr << 1;                // 阈值 ×2
    } else if (oldThr > 0) {
        newCap = oldThr;                     // 指定容量构造（懒初始化）
    } else {
        newCap = DEFAULT_INITIAL_CAPACITY;   // 默认 16
        newThr = (int) (DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY); // 12
    }

    // ② 新数组
    Node<K, V>[] newTab = (Node<K, V>[]) new Node[newCap];
    table = newTab;

    // ③ 元素迁移（关键——高低位拆分）
    if (oldTab != null) {
        for (int j = 0; j < oldCap; ++j) {
            Node<K, V> e;
            if ((e = oldTab[j]) != null) {
                oldTab[j] = null;            // 置空（帮助 GC）
                if (e.next == null)
                    newTab[e.hash & (newCap - 1)] = e;   // 单节点直接放
                else if (e instanceof TreeNode)
                    ((TreeNode<K, V>) e).split(this, newTab, j, oldCap);  // 树拆分
                else { /* 链表高低位拆分（见下） */ }
            }
        }
    }
    return newTab;
}
```

---

## 3. 高低位拆分原理

> 🎯 **JDK 8 核心优化**：扩容时用 `(e.hash & oldCap) == 0` 判断元素去留——**免重新计算 hash**：

```text
高低位拆分原理
├── ① 为什么容量翻倍 = 多一位参与
│   ├── 旧容量 16：下标用低 4 位（1111 & hash）
│   ├── 新容量 32：下标用低 5 位（11111 & hash）
│   └── 新增的「第 5 位」决定元素去留
├── ② 判定：e.hash & oldCap
│   ├── 结果为 0：新增位是 0 → 留在原索引
│   ├── 结果非 0：新增位是 1 → 移到「原索引 + oldCap」
│   └── 无需重新计算 hash（一次位运算）
├── ③ 源码（链表拆分）：
│   Node<K,V> loHead = null, loTail = null;   // 低位链（留原索引）
│   Node<K,V> hiHead = null, hiTail = null;   // 高位链（+oldCap）
│   do {
│       if ((e.hash & oldCap) == 0) {
│           if (loTail == null) loHead = e;
│           else loTail.next = e;
│           loTail = e;
│       } else {
│           if (hiTail == null) hiHead = e;
│           else hiTail.next = e;
│           hiTail = e;
│       }
│   } while ((e = e.next) != null);
│   newTab[j] = loHead;              // 原位
│   newTab[j + oldCap] = hiHead;     // 原索引 + 旧容量
└── ④ 效果：一次遍历拆成两条链（O(n) 迁移）
```

### 3.1 示例

```text
示例（容量 16 → 32）
├── 元素 A：hash = 0x05（低 5 位 00101）
│   ├── 旧：1111 & hash = 5（桶 5）
│   ├── hash & oldCap(16) = 0x05 & 0x10 = 0 → 留桶 5
├── 元素 B：hash = 0x15（低 5 位 10101）
│   ├── 旧：1111 & hash = 5（桶 5——与 A 冲突）
│   ├── hash & 16 = 0x15 & 0x10 = 0x10 ≠ 0 → 移桶 5+16=21
└── 效果：原本冲突的 A/B 扩容后分散（冲突减少）
```

---

## 4. JDK 7 vs JDK 8 扩容

| 维度 | JDK 7 | JDK 8+ |
|------|:---:|:---:|
| 迁移方式 | **重新计算每个 hash** | **高低位拆分（一次位运算）** |
| 链表插入 | 头插法 | 尾插法 |
| 迁移顺序 | 反转（头插导致） | 保持原顺序 |
| 并发安全 | ❌ 死循环 | ✅ 无死循环（仍非安全） |
| 复杂度 | O(n) 全重算 | O(n) 但免重算 |

```text
JDK 8 扩容改进总结
├── ① 效率：免重算（位运算代替完整 hash）
├── ② 安全：尾插法 + 保序（修死循环）
├── ③ 树处理：红黑树 split 拆分（低/高位子树）
└── ④ 金句：扩容从「重算迁移」到「位运算拆分」
```

---

## 5. 扩容的死循环问题

### 5.1 JDK 7 的死循环（历史面试题）

> ⚠️ **JDK 7 多线程扩容死循环**——头插法 + 并发迁移导致环形链表：

```text
死循环成因
├── ① JDK 7 头插法：迁移时新节点插头部（顺序反转）
├── ② 多线程并发扩容：
│   ├── 线程 A 迁移链表到一半（暂停）
│   ├── 线程 B 完成迁移（链表反转）
│   └── 线程 A 恢复 → 基于旧结构迁移 → 环形链表！
├── ③ 结果：
│   ├── 环形链表 → get 死循环
│   └── CPU 100%（经典生产事故）
└── ④ 修复（JDK 8）：
    ├── 尾插法（不反转——不会形成环）
    └── 高低位拆分（一次遍历完成）

⚠️ 注意：JDK 8 修了死循环，但仍有数据覆盖/丢失
→ 结论：并发场景必须用 ConcurrentHashMap（不是「JDK 8 安全了」）
```

---

## 6. 扩容的性能代价

```text
扩容的三重代价
├── ① 内存：新数组分配（旧数组 GC）
├── ② CPU：元素迁移（O(n)）
├── ③ 停顿：大 Map 扩容卡顿（STW 式）
└── ④ 高频扩容：容量太小 + 大量写入 → 多次扩容

扩容触发频率（负载因子 0.75）
├── 容量 16：12 个元素后扩容
├── 容量 32：24 个后
├── 容量 1024：768 后
└── 结论：初始容量合理 → 扩容次数指数级减少
```

---

## 7. 生产避坑：容量预估

> 🎯 **预估容量避免多次扩容**（生产最佳实践）：

```java
// ❌ 不预估：已知要存 1000 个 → 多次扩容
Map<String, Order> map = new HashMap<>();
for (int i = 0; i < 1000; i++) map.put("k" + i, order);
// 扩容路径：16 → 32 → 64 → ... → 1024（7 次扩容！）

// ✅ 预估：initialCapacity = 数据量 / 0.75 + 1
Map<String, Order> map = new HashMap<>(1000 / 3 * 4 + 1);
// 或常用公式：容量 = 预期大小 / 负载因子 + 1
Map<String, Order> map = new HashMap<>((int) (1000 / 0.75f) + 1);
// → 容量 2048：一次到位（0 次扩容）

// 通用工具方法
public static <K, V> HashMap<K, V> newHashMap(int expectedSize) {
    return new HashMap<>((int) (expectedSize / 0.75f) + 1);
}
```

```text
容量预估要点
├── ① 公式：initialCapacity = expectedSize / 0.75 + 1
├── ② 效果：已知数据量 → 零扩容（省 7 次 O(n) 迁移）
├── ③ 场景：批量导入/缓存预加载/已知数据量
├── ④ ⚠️ 过度预估：内存浪费（数组按容量分配）
└── ⑤ 金句：预估 = 「多花一点初始内存，省多次迁移」
```

---

## 8. 核心要点

> 🎯 **核心要点**：
> 1. 扩容触发：size > 阈值（容量 × 0.75）——插入后判断
> 2. resize 双职责：懒初始化（首次创建）+ 扩容（×2）
> 3. **高低位拆分**：`(hash & oldCap) == 0` 决定去留——免重算 + 一次遍历
> 4. JDK 7 → 8：重算 → 位运算；头插 → 尾插（修死循环）
> 5. JDK 7 死循环是历史问题：JDK 8 修死循环但**仍非线程安全**（并发用 ConcurrentHashMap）
> 6. 扩容代价：内存 + O(n) 迁移 + 停顿——**容量预估（/0.75+1）避免多次扩容**

---

**下一模块**：[05-红黑树化机制](05-红黑树化机制.md) | **返回总览**：[00-HashMap知识体系总览](00-HashMap知识体系总览.md)
