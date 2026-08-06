# 02 - BST 的增删查

> BST 的三大基本操作：搜索 O(h)、插入 O(h)、删除三态（无子/单子/双子）——「递归返回挂接」是 BST 修改类题的统一模式。

## 📚 目录

1. [搜索（700 题）](#1)
2. [插入（701 题）](#2)
3. [删除（450 题）](#3)
4. [递归返回挂接模式](#4)
5. [操作复杂度速查](#5)

## 1. 搜索（700 题）

**原理**：比根小向左、比根大向右、相等返回——每步排除一半子树。

```java
// 700 二叉搜索树中的搜索（迭代）
public TreeNode searchBST(TreeNode root, int val) {
    while (root != null) {
        if (val == root.val) return root;
        root = val < root.val ? root.left : root.right;
    }
    return null;
}

// 递归版（对称直观）
public TreeNode searchBST(TreeNode root, int val) {
    if (root == null || root.val == val) return root;
    return val < root.val ? searchBST(root.left, val)
                          : searchBST(root.right, val);
}
```

> 💡 搜索是 BST 的「hello world」——理解它 = 理解 BST 的性质如何加速查找（O(h) vs 线性扫描 O(n)）。

## 2. 插入（701 题）

**原理**：按搜索路径走到空位，新节点挂上去——**递归返回挂接**（子树替换）：

```java
// 701 二叉搜索树中的插入操作
public TreeNode insertIntoBST(TreeNode root, int val) {
    if (root == null) return new TreeNode(val);   // 找到空位，创建节点
    if (val < root.val) {
        root.left = insertIntoBST(root.left, val);   // 挂回左子树
    } else {
        root.right = insertIntoBST(root.right, val); // 挂回右子树
    }
    return root;                                     // 原样返回（结构不变）
}
```

**迭代版**（面试可能追问）：

```java
public TreeNode insertIntoBST(TreeNode root, int val) {
    if (root == null) return new TreeNode(val);
    TreeNode cur = root;
    while (true) {
        if (val < cur.val) {
            if (cur.left == null) { cur.left = new TreeNode(val); break; }
            cur = cur.left;
        } else {
            if (cur.right == null) { cur.right = new TreeNode(val); break; }
            cur = cur.right;
        }
    }
    return root;
}
```

> 💡 递归版与迭代版的区别：递归版**返回新根**（适合嵌套调用），迭代版原地修改返回原根——BST 修改类题两种风格都要会。

## 3. 删除（450 题）

**三态分析**：

| 情况 | 处理 | 图例 |
|------|------|------|
| ① 叶子 | 直接删（返回 null） | `5` → 删 5 后无痕 |
| ② 单子树 | 用唯一孩子顶替 | `5` 有左子 `3` → 3 顶上 |
| ③ 双子树 | 用**右子树最小节点**替代值，再删掉它 | `5` 左右都有 → 右子树最左替代 |

```java
// 450 删除二叉搜索树中的节点
public TreeNode deleteNode(TreeNode root, int key) {
    if (root == null) return null;
    if (key < root.val) {
        root.left = deleteNode(root.left, key);
    } else if (key > root.val) {
        root.right = deleteNode(root.right, key);
    } else {                                    // 找到目标
        if (root.left == null) return root.right;   // ①叶子/②只有右子
        if (root.right == null) return root.left;   // ②只有左子
        // ③ 双子树：右子树最小节点替代
        TreeNode successor = minNode(root.right);
        root.val = successor.val;                   // 值替代
        root.right = deleteNode(root.right, successor.val);  // 删掉最小节点
    }
    return root;
}

TreeNode minNode(TreeNode node) {
    while (node.left != null) node = node.left;
    return node;
}
```

> ⚠️ **双子树替代的两种选法**：右子树**最小**（后继）或左子树**最大**（前驱）都合法——选右子树最小是惯例；替代后 BST 性质保持（右子树最小值 > 根、< 右子树其余所有）。

## 4. 递归返回挂接模式

**BST 修改类题（插入/删除）的统一模式**：

```text
递归函数返回值 = 处理后的子树根
调用方：root.left = f(root.left, ...)   ← 挂接回来
```

| 步骤 | 操作 |
|------|------|
| ① 终止 | null 时返回 null / 新节点 |
| ② 分治 | 按值大小决定进左/右子树 |
| ③ 挂接 | `root.left/right = 递归结果` |
| ④ 返回 | 返回当前节点（结构可能已变） |

**为什么必须挂接**：递归处理子树后，子树根可能改变（删除根节点时）——必须把新根挂回父节点，否则树就断了。

> 💡 这个模式在**所有树的修改类题**通用（翻转、修剪、合并二叉树……）——二叉树体系 03 模块的「递归三问」在这里升级为「递归返回挂接」。

## 5. 操作复杂度速查

| 操作 | 时间 | 空间 | 要点 |
|------|:---:|:---:|------|
| 搜索 | O(h) | O(h) 递归 / O(1) 迭代 | 每步排除一半 |
| 插入 | O(h) | O(h) 递归 | 空位落点 |
| 删除 | O(h) | O(h) 递归 | 三态处理 |
| 取最小/最大 | O(h) | O(1) | 一路向左/右 |

> ⚠️ **h 的不确定性**：平衡 BST h = O(log n)；有序插入退化 h = O(n)。工程上 Java 的 TreeMap/TreeSet 用红黑树保证 h = O(log n)——这也是[红黑树](../红黑树 Red‑Black Tree/)的动机。

> 🎯 **核心要点**：增删查验收——搜索/插入能写迭代+递归双版本；删除三态默写（叶子、单子、双子最小替代）；理解「递归返回挂接」是树修改类题的总模式；能回答「删除双子树为什么用右子树最小」（保持 BST 性质、实现简单）。有序操作（第 K 小、后继）见下一节。

---

**上一模块**：[01-BST 的性质与验证](01-BST的性质与验证.md) ｜ **下一模块**：[03-有序操作与第 K 小](03-有序操作与第K小.md) ｜ **返回总览**：[00-二叉搜索树 BST 知识体系总览](00-二叉搜索树 BST 知识体系总览.md)

**【参考来源】**
- LeetCode 700 二叉搜索树中的搜索 / 701 插入 / 450 删除
- 算法导论（CLRS）第 12.2-12.3 节
