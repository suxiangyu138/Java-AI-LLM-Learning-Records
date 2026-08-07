# 03-Treap与面试实战
> 一句话：BST 键值 + 随机堆优先级——期望 O(log n) 的"最简单平衡树"，旋转版 30 行、第 K 大/动态集合首选

## 📚 目录
1. [Treap 模型：BST + 随机堆](#1-treap-模型bst--随机堆)
2. [旋转版 Treap 实现](#2-旋转版-treap-实现)
3. [分裂合并版（FHQ-Treap）](#3-分裂合并版fhq-treap)
4. [平衡树面试选型](#4-平衡树面试选型)
5. [实战应用：第 K 大与动态集合](#5-实战应用第-k-大与动态集合)
6. [面试话术与追问](#6-面试话术与追问)

---

## 1. Treap 模型：BST + 随机堆

```text
Treap = Tree + Heap——
  键值 val: 满足 BST（左 < 根 < 右）
  优先级 pri: 满足堆（父 ≥ 子，随机生成）

为什么随机优先级能平衡:
  插入顺序固定时 BST 可能退化；
  Treap 的形态由"随机优先级"决定——
  期望树高 O(log n)（随机数据的概率保证）

⚠️ 期望 vs 最坏:
  期望 O(log n)（随机优先级下高概率）
  最坏仍可能退化（概率指数级小）

为什么是"最简单平衡树":
  不需要 AVL 的平衡因子/四型判定——
  只需 BST 插入 + 一次旋转（比较优先级）
  代码 ~30 行（vs AVL ~80、红黑 ~150）
```

> 🎯 **核心认知**："**Treap = 'BST 管有序 + 堆管平衡'——随机优先级让树形态概率平衡**。不需要复杂平衡规则，一次旋转搞定——'用随机换简单'是它的设计哲学。"

## 2. 旋转版 Treap 实现

### 2.1 完整实现（~30 行）

```java
/**
 * 旋转版 Treap（面试手写推荐）
 * 插入: BST 插入 + 优先级违反时旋转
 * 时间: 期望 O(log n)
 */
class TreapNode {
    int val, pri;                                // ① 键值 + 随机优先级
    TreapNode left, right;
    TreapNode(int val) {
        this.val = val;
        this.pri = new Random().nextInt();       // ② 随机优先级
    }
}

TreapNode rotateRight(TreapNode y) {             // ③ 右旋（保持 BST + 修复堆）
    TreapNode x = y.left;
    y.left = x.right;
    x.right = y;
    return x;
}

TreapNode rotateLeft(TreapNode x) {
    TreapNode y = x.right;
    x.right = y.left;
    y.left = x;
    return y;
}

TreapNode insert(TreapNode node, int val) {
    if (node == null) return new TreapNode(val);   // ④ 插入叶子
    if (val < node.val) {
        node.left = insert(node.left, val);
        if (node.left.pri > node.pri) node = rotateRight(node);  // ⑤ 堆违规 → 右旋
    } else if (val > node.val) {
        node.right = insert(node.right, val);
        if (node.right.pri > node.pri) node = rotateLeft(node);
    }
    return node;
}
// 推演 插入 1,2,3（随机优先级假设 1 最高）：
//   1 → 2（右）→ 3（右）: 若 3 的优先级最高 → 左旋两次 →
//   3 成为根 → 树平衡（期望）
// ⚠️ 旋转条件: 插入后孩子的优先级 > 父 → 旋转（大顶堆语义）
//   唯一的新逻辑: 一次旋转 + 优先级比较
```

> 🎯 **旋转版的价值**："**'BST 插入 + 优先级违规旋转'——Treap 是唯一'会 BST 就会平衡树'的实现**。30 行手撕是竞赛与面试的性价比之王。"

## 3. 分裂合并版（FHQ-Treap）

### 3.1 概念与骨架

```java
/**
 * FHQ-Treap（分裂合并版）: 无需旋转——
 * 按值 split 成两棵树、merge 合并两棵树
 * 优势: 支持区间操作（懒标记）、可持久化
 */
// split(node, val): 拆成 ≤ val 与 > val 两棵树（递归）
// merge(a, b): 按优先级合并两棵树（a 全 < b）

TreapNode split(TreapNode node, int val) {      // ① 分裂
    if (node == null) return null;
    if (node.val <= val) {
        // ② 右子树继续分裂 → 挂到 node 右
        TreapNode right = split(node.right, val);
        node.right = right 的左半部分;
        return ...
    } else {
        // ③ 左子树分裂（对称）
    }
}

TreapNode merge(TreapNode a, TreapNode b) {     // ④ 合并（a 全 < b）
    if (a == null) return b;
    if (b == null) return a;
    if (a.pri > b.pri) {                         // ⑤ 优先级决定根
        a.right = merge(a.right, b);
        return a;
    } else {
        b.left = merge(a, b.left);
        return b;
    }
}
// ⚠️ 优势: 区间翻转/加减（打懒标记）、可持久化——
//   竞赛进阶（FHQ 是平衡树的全能形态）
```

> 💡 **FHQ 的定位**："**'分裂 + 合并'替代旋转——FHQ-Treap 支持区间操作与持久化，是竞赛平衡树的全能版**。面试提'还有分裂合并版'即可，手撕以旋转版为主。"

## 4. 平衡树面试选型

| 场景 | 选型 | 原因 |
|------|:---:|------|
| 工程有序集合 | 红黑树 | TreeMap 底层、增删便宜 |
| 查询优先 | AVL | 树矮查询快 |
| 面试手撕 | **Treap** | 30 行、期望 O(log n) |
| 区间操作 | FHQ-Treap / Splay | 分裂合并/懒标记 |
| 磁盘数据 | B+ 树 | 页对齐、I/O 少 |

```text
⚠️ 面试一句话:
  "讲原理选 AVL/红黑（性质与旋转）；
   手撕选 Treap（30 行最简单）；
   区间操作提 FHQ/Splay。"
```

## 5. 实战应用：第 K 大与动态集合

### 5.1 第 K 大（Treap + 子树大小）

```java
/**
 * Treap 求第 K 大: 节点维护 size（子树大小）
 * 时间 O(log n)
 */
// ① 节点加 size 字段 + 更新（旋转后 pushup）
// ② 第 K 大:
int kth(TreapNode node, int k) {
    int leftSize = size(node.left);              // ① 左子树大小
    if (k <= leftSize) return kth(node.left, k); // ② 在左
    if (k == leftSize + 1) return node.val;      // ③ 就是根
    return kth(node.right, k - leftSize - 1);    // ④ 在右（偏移）
}
// 推演 树 {1,3,5} → kth(2) = 3 ✓
// ⚠️ size 维护: 插入/旋转后更新——"动态第 K 大"的 Treap 版
//   与 BIT+二分（03 文件）等价，Treap 更直接
```

### 5.2 动态集合（有序 + 排名）

```java
// 应用: 动态有序集合——
//   insert/delete/find O(log n)
//   kth（第 K 大）/ rank（排名）O(log n)
//   floor/ceiling（前驱/后继）O(log n)

// ⚠️ vs TreeMap: Treap 多"排名查询"（size 维护）——
//   需要动态排名时 Treap 比 TreeMap 更直接
```

> 🎯 **Treap 的实战定位**："**'BST 有序 + size 排名'——动态集合的'第 K 大/排名'查询 Treap 最直接**。比 TreeMap 多排名能力、比线段树更简单——竞赛与面试手写的首选。"

## 6. 面试话术与追问

### 6.1 话术模板

```text
"这是【动态集合】题——平衡树【家族成员】。
退化问题: BST 有序插入 → O(n)。
【成员】的方案: 【平衡手段】。
复杂度: 【O(log n) 稳定/期望】。
选型: 【场景 vs 其他成员】。"

例: "动态第 K 大——Treap。
     BST 键值 + 随机优先级 → 期望 O(log n)。
     size 维护排名，kth 树上二分。
     选 Treap 因为手写最简（30 行）。"
```

### 6.2 高频追问 TOP 8

| # | 追问 | 标准回答 |
|:---:|------|---------|
| 1 | BST 为什么退化？ | 有序插入挂同边 → 单链表 O(n) |
| 2 | AVL vs 红黑树？ | AVL 严格查询快；红黑弱平衡增删旋转少——工程默认红黑 |
| 3 | 红黑树五性质？ | 根黑叶黑不连红、黑高相同——最长 ≤ 2× 最短 |
| 4 | Treap 为什么平衡？ | 随机优先级期望平衡——BST+堆 |
| 5 | TreeMap 底层？ | 红黑树（O(log n) 增删查 + 有序 API） |
| 6 | HashMap 为什么树化？ | 桶内 8 个转红黑树——防哈希洪水（泊松概率极低） |
| 7 | 数据库为什么 B+ 树？ | 磁盘页对齐 + 树高 3-4 层——减 I/O |
| 8 | 手写选哪个？ | Treap（30 行）；红黑树讲性质 |

> 🎯 **核心要点**：平衡树实战通关——**Treap 手写（30 行旋转版）+ 第 K 大（size + 树上二分）+ 家族选型（读 AVL/增删红黑/手写 Treap/磁盘 B+）**。平衡树面试 = "退化原因 + 家族对比 + 一个手撕"——Treap 就是那个手撕。

---

**上一篇**：[02-红黑树与工程应用](02-红黑树与工程应用.md)
**返回总览**：[00-平衡树知识体系总览](00-平衡树知识体系总览.md)
