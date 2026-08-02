# 02 - HashMap 源码深度剖析

> 定位：逐行剖析 HashMap 的 hash 扰动、put/get/resize 全流程、树化与泊松分布，梳理 JDK 7→8 的演进与 2026 年的最新优化

## 📚 目录

1. [总体设计与核心参数](#1-总体设计与核心参数)
2. [hash() 扰动函数](#2-hash-扰动函数)
3. [put() 全流程源码](#3-put-全流程源码)
4. [get() 与 remove()](#4-get-与-remove)
5. [resize() 扩容机制](#5-resize-扩容机制)
6. [树化：阈值 8 的泊松分布依据](#6-树化阈值-8-的泊松分布依据)
7. [JDK 7 vs JDK 8 演进](#7-jdk-7-vs-jdk-8-演进)
8. [容量选择与最新优化](#8-容量选择与最新优化)
9. [面试高频考点](#9-面试高频考点)

---

## 1. 总体设计与核心参数

### 1.1 数据结构（JDK 8 → JDK 26 保持稳定）

```
┌─────────────────────────────────────────────────────┐
│ Node<K,V>[] table（桶数组，默认 16，容量恒为 2 的幂）  │
├────────┬────────┬──────────┬──────────┬─────────────┤
│ [0]    │ [1]    │ [2]      │ ...      │ [n-1]       │
├────────┴───┬────┴────┬─────┴────┬─────┴─────────────┤
│  空桶 null │ 单节点  │ 链表(<8) │ 红黑树(>=8)       │
│           │ Node    │ Node→Node│ TreeNode          │
└───────────┴─────────┴──────────┴───────────────────┘
        hash 碰撞解决：链地址法；链表 ≥ 8 且容量 ≥ 64 → 树化
```

### 1.2 核心参数表

| 参数 | 值 | 含义 |
|------|-----|------|
| DEFAULT_INITIAL_CAPACITY | 1 << 4 = 16 | 默认桶数 |
| MAXIMUM_CAPACITY | 1 << 30 | 桶数上限 |
| DEFAULT_LOAD_FACTOR | 0.75f | 装载因子（时空折中） |
| TREEIFY_THRESHOLD | 8 | 链表 → 红黑树阈值 |
| UNTREEIFY_THRESHOLD | 6 | 树 → 链表阈值（扩容拆分后） |
| MIN_TREEIFY_CAPACITY | 64 | 树化所需最小容量 |

> 💡 **6 和 8 之间的"滞回"**：7 是中间缓冲带，避免链表/树在边界反复切换（抖动）。树化后元素减少到 6 才退化为链表，而不是 7。

### 1.3 容量为什么必须是 2 的幂

```java
index = (n - 1) & hash    // 等价于 hash % n，但快一个数量级
```

| 原因 | 说明 |
|------|------|
| 位运算替代取模 | `&` 一条指令，取模要除法，快得多 |
| 充分利用低位 | 2^n - 1 二进制全为 1（如 15 = 1111），任何 hash 位都有机会参与 |
| 扩容无需重算 | 见 5.3，新下标只有"原位"或"原位+oldCap"两种可能 |

传入非 2 的幂？`tableSizeFor()` 会向上找最小 2 的幂：传入 19 → 实际 32。

---

## 2. hash() 扰动函数

```java
/** ⭐ 扰动函数：让高位信息参与低位下标计算 */
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
```

### 2.1 为什么要扰动

下标计算 `(n - 1) & hash` 只用到了 hash 的**低 n 位**（n=16 时只用到低 4 位）。若多个 key 的 hashCode 低位相同而高位不同，会全部挤到同一桶。扰动后高位与低位异或，碰撞大幅减少。

```
key.hashCode() 0x12345678  →  0001 0010 0011 0100 0101 0110 0111 1000
h >>> 16      0x00001234   →  0000 0000 0000 0000 0001 0010 0011 0100
XOR           0x1234464C   ←  高位信息混合进了低位
```

### 2.2 版本演进

| JDK | 扰动次数 | 逻辑 |
|-----|---------|------|
| JDK 7 | 9 次 | 4 次移位 + 5 次异或 |
| JDK 8+ | 2 次 | 1 次异或（`h ^ (h>>>16)`） |

> 🎯 **JDK 8 为什么敢简化？** 因为新增了红黑树兜底：即使扰动不够、碰撞多，最坏也是 O(log n) 而非 O(n)。性能与鲁棒性的权衡。

---

## 3. put() 全流程源码

```java
final V putVal(int hash, K key, V value, boolean onlyIfAbsent, boolean evict) {
    Node<K,V>[] tab; Node<K,V> p; int n, i;
    // ① 首次 put：延迟初始化（构造时不创建数组，省内存）
    if ((tab = table) == null || (n = tab.length) == 0)
        n = (tab = resize()).length;
    // ② 空桶：直接放入（最常见路径）
    if ((p = tab[i = (n - 1) & hash]) == null)
        tab[i] = newNode(hash, key, value, null);
    else {
        Node<K,V> e; K k;
        // ③ 首节点命中（hash 相同且 key 相同）
        if (p.hash == hash && ((k = p.key) == key || (key != null && key.equals(k))))
            e = p;
        // ④ 已是红黑树 → 走树节点插入
        else if (p instanceof TreeNode)
            e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
        // ⑤ 链表：尾插遍历
        else {
            for (int binCount = 0; ; ++binCount) {
                if ((e = p.next) == null) {          // 未找到 → 尾插
                    p.next = newNode(hash, key, value, null);
                    if (binCount >= TREEIFY_THRESHOLD - 1)   // 到 8 个 → 尝试树化
                        treeifyBin(tab, hash);
                    break;
                }
                if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k))))
                    break;                           // 找到 → 替换
                p = e;
            }
        }
        // ⑥ 覆盖旧值并返回
        if (e != null) {
            V oldValue = e.value;
            if (!onlyIfAbsent || oldValue == null) e.value = value;
            afterNodeAccess(e);
            return oldValue;
        }
    }
    ++modCount;                                      // ⑦ 结构性修改计数（fail-fast）
    if (++size > threshold) resize();                // ⑧ 超过阈值 → 扩容
    afterNodeInsertion(evict);                       // ⑨ 钩子方法（LinkedHashMap 用）
    return null;
}
```

> 🎯 **put 七步口诀**：空表初始化 → 空桶直放 → 头节点命中 → 树走树 → 链表尾插（满 8 树化）→ 覆盖旧值 → 检查扩容。树化是"尝试"，容量 < 64 时实际做的是扩容。

---

## 4. get() 与 remove()

### 4.1 get() 流程

```java
public V get(Object key) {
    Node<K,V> e;
    return (e = getNode(hash(key), key)) == null ? null : e.value;
}

final Node<K,V> getNode(int hash, Object key) {
    Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (first = tab[(n - 1) & hash]) != null) {      // 定位桶
        if (first.hash == hash &&                     // ① 头节点比较
            ((k = first.key) == key || (key != null && key.equals(k))))
            return first;
        if ((e = first.next) != null) {
            if (first instanceof TreeNode)            // ② 树查找 O(log n)
                return ((TreeNode<K,V>)first).getTreeNode(hash, key);
            do {                                      // ③ 链表遍历
                if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k))))
                    return e;
            } while ((e = e.next) != null);
        }
    }
    return null;
}
```

**查找双重判断**：先 `hash ==` 快速过滤（同桶才可能相等），再 `==` 或 `equals` 精确匹配。这就是为什么 `equals` 相同 `hashCode` 必须相同——否则第一步就过滤掉了。

### 4.2 remove() 流程

同 get 定位后，链表删除需维护前驱节点（`p.next = e.next`），树删除走 `removeTreeNode`（可能触发去树化/旋转）。删除后 `--size; ++modCount`。

---

## 5. resize() 扩容机制

### 5.1 扩容时机与决策

```
默认 16 × 0.75 = 12 → 第 13 个元素触发扩容（16 → 32）
规则：oldCap >= 16 时 newThr = oldThr << 1（阈值翻倍）
```

### 5.2 迁移核心：lo/hi 双链表

```java
// resize() 中链表迁移的关键逻辑（源码精简）
do {
    if ((e.hash & oldCap) == 0) {     // ⭐ 关键判断
        loTail 尾插 → loHead       // 新下标 = 原位 j
    } else {
        hiTail 尾插 → hiHead       // 新下标 = j + oldCap
    }
} while ((e = e.next) != null);
newTab[j] = loHead;
newTab[j + oldCap] = hiHead;
```

### 5.3 为什么不用重算 hash

容量翻倍（oldCap 是 2 的幂），下标计算公式从 `hash & (oldCap-1)` 变为 `hash & (2*oldCap-1)`——**只新增了 1 个高位参与计算**。这个位正是 `oldCap` 那一位：

```
hash = ...0100 1010,  oldCap = 16 (10000)
hash & 15  = 1010 = 10        （原位）
hash & 31  = 11010 = 26       （原位 + 16 = oldCap 位为 1 → hi）
hash & oldCap = 16 ≠ 0 → 迁移到 原位+oldCap
```

| 判断 | 结果 | 新下标 |
|------|------|--------|
| `hash & oldCap == 0` | lo 链表 | 原位 j |
| `hash & oldCap == oldCap` | hi 链表 | j + oldCap |

> 💡 **尾插顺序保持**：lo/hi 都按原链表顺序迁移（尾插），不会反转。这既是 JDK 8 修复死循环的关键，也让迭代器顺序在扩容后依然稳定。

### 5.4 树节点的拆分

`TreeNode.split()` 同样按 `hash & oldCap` 拆成 lo/hi 两棵树，拆完节点数 ≤ 6 则**去树化**（`untreeify` 转回链表）。

---

## 6. 树化：阈值 8 的泊松分布依据

### 6.1 触发条件（两个条件缺一不可）

```
链表长度 ≥ 8  且  桶数组长度 ≥ 64
  ↑                        ↑
  桶内第 9 个节点插入时     小于 64 时只扩容不树化
```

> ⚠️ **为什么容量 < 64 不树化**：容量小时整体元素少，扩容（重散列）通常就能打散链表；红黑树节点是普通 Node 的 2 倍内存，过早树化浪费空间。

### 6.2 泊松分布推导

假设哈希完全均匀（每元素落入任一桶概率 1/n），装载因子 0.75 时，某桶链表长度达到 k 的概率服从泊松分布：

```
P(k) = (λ^k · e^-λ) / k!      λ = 0.75（平均每个桶的元素数）
P(0)=0.4724  P(1)=0.3543  P(2)=0.1329  P(3)=0.0332
P(4)=0.0062  P(5)=0.0009  P(6)=0.0001  P(7)=0.000018
P(8)=0.0000002  →  约 千万分之一
```

**结论**：正常数据下链表长到 8 的概率低于千万分之一——一旦出现，几乎必然是**恶意构造（HashDoS）或劣质 hashCode**，此时用红黑树把最坏复杂度从 O(n) 兜到 O(log n)。

---

## 7. JDK 7 vs JDK 8 演进

| 特性 | JDK 7 | JDK 8+ |
|------|-------|--------|
| 数据结构 | 数组 + 链表 | 数组 + 链表 + 红黑树 |
| 扰动次数 | 9 次 | 2 次 |
| 插入方式 | 头插法 | 尾插法 |
| 并发扩容 | ⚠️ 死循环（环形链表） | ✅ 无死循环（但仍有数据丢失） |
| 扩容重定位 | 重算 hash 再取模 | `hash & oldCap` 拆链 |
| 初始化 | 构造时建数组 | 首次 put 延迟初始化 |
| 树化 | 无 | 有（见第 6 节） |

### 7.1 JDK 7 头插法死循环

```
多线程并发扩容：
扩容前 table[2] → A → B → null
线程1 头插迁移 → newTab[2] → B → A → null（顺序反转）
线程2 同时迁移看到中间状态 → 可能形成 A.next = B, B.next = A
→ get() 遍历该桶死循环 → CPU 100%
```

JDK 8 尾插法保持顺序，环形链表不再形成。**但注意**：JDK 8 并发下仍有覆盖丢数据问题，多线程一律用 [04-ConcurrentHashMap并发哈希剖析](04-ConcurrentHashMap并发哈希剖析.md)。

---

## 8. 容量选择与最新优化

### 8.1 初始容量公式

```java
// 期望放 n 个元素，避免扩容：容量 = n / 0.75 ≈ n * 4/3
Map<String, Object> map = new HashMap<>(expectedSize / 3 * 4 + 1);
// Guava 提供更精确的：Maps.newHashMapWithExpectedSize(n)
```

### 8.2 最新优化（2026-08 验证）

| 更新 | 内容 | 影响 |
|------|------|------|
| JDK-8371656（2026-04-28 合入，backport 17/21/25/26） | `HashMap.putAll()` 与拷贝构造器批量插入优化：输入是 HashMap/UnmodifiableMap 时直接按目标容量扩容一次 | 插入快 40%-80%，业务批量灌数据场景直接受益 |
| JDK 26 `Map.ofLazy()`（JEP 526） | key 集合固定、value 懒加载的不可变 Map | 常量 Map 启动内存优化，与 HashMap 无关但值得知晓 |

> 💡 **现状**：HashMap 核心数据结构自 JDK 8 起十年未变，说明"数组+链表+红黑树 + 扰动函数"的设计已经稳定成熟；JDK 的精力转向了批量操作性能与不可变集合 API。

---

## 9. 面试高频考点

**Q1: put 流程？** 空表初始化 → `(n-1)&hash` 定位 → 空桶直放 / 树插入 / 链表尾插 → 满 8 且容量满 64 树化 → 覆盖旧值 → size 超 threshold 扩容。

**Q2: 为什么扰动一次就够了？** 红黑树兜底后最坏 O(log n)，一次异或已把高位混入低位，复杂度收益与实现成本平衡。

**Q3: 树化阈值为什么是 8？** 泊松分布下装载因子 0.75 时链表到 8 的概率约千万分之一，出现即视为异常输入，用 O(log n) 兜底。

**Q4: 为什么 6 转链表、8 转树？** 滞回区间防抖动。树转链再转树会反复执行，浪费 CPU。

**Q5: 扩容后元素新位置？** `hash & oldCap == 0` 在原位，否则原位 + oldCap，无需重算 hash，且保持链表顺序。

**Q6: JDK 8 怎么修的死循环？** 尾插法 + 迁移时不反转顺序。但并发仍有丢数据问题 → ConcurrentHashMap。

**Q7: 为什么容量是 2 的幂？** 位运算替代取模、2^n-1 全 1 均匀散列、扩容只拆两条链。

---

> 🎯 **核心要点**：HashMap = 均匀散列（扰动）+ 冲突兜底（链表→红黑树）+ 增量扩容（lo/hi 拆链）。三者共同保证"正常 O(1)、恶意 O(log n)"的工程鲁棒性。

---

**下一模块**：[03-Hash家族集合全景](03-Hash家族集合全景.md)（HashMap 之外的 Hash 集合） / **返回总览**：[00-Hash总览](00-Hash总览.md)
