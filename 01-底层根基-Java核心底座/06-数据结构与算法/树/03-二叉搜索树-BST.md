# 二叉搜索树 BST
> 中序有序的核心性质——BST 的验证、搜索、插入、删除四大操作与构造

## 📚 目录
1. [BST 的核心性质](#1-bst-的核心性质)
2. [98. 验证二叉搜索树](#2-98-验证二叉搜索树)
3. [700/701. BST 搜索与插入](#3-700701-bst-搜索与插入)
4. [450. 删除 BST 节点](#4-450-删除-bst-节点)
5. [230. 二叉搜索树第 K 小](#5-230-二叉搜索树第-k-小)
6. [BST 的构造](#6-bst-的构造)
7. [BST 面试题汇总](#7-bst-面试题汇总)

---

## 1. BST 的核心性质

### 1.1 定义

```
二叉搜索树（Binary Search Tree, BST）：
  左子树所有节点 < 根 < 右子树所有节点
  且左右子树自身也是 BST

     8
    / \
   3   10
  / \    \
 1   6    14
    / \   /
   4   7 13

关键推论：BST 中序遍历 = 从小到大有序输出！
```

### 1.2 为什么 BST 重要？

```
1. 平均 O(log n) 的搜索/插入/删除（树高 h = log n）
2. 中序有序 → 范围查询、第 K 小/大、排序
3. 平衡 BST（AVL/红黑树）是数据库索引、TreeMap 的底层
4. 退化风险：插入有序序列 → 变成链表 → O(n)
```

### 1.3 BST 操作复杂度

| 操作 | 平均 | 最坏（退化） | 实现 |
|------|:---:|:---:|------|
| 搜索 | O(log n) | O(n) | 二分式比较 |
| 插入 | O(log n) | O(n) | 二分式定位叶子 |
| 删除 | O(log n) | O(n) | 三情况分类 |
| 第 K 小 | O(n) | O(n) | 中序遍历 |

---

## 2. 98. 验证二叉搜索树

### 2.1 三种解法

```java
/**
 * LeetCode 98. 验证二叉搜索树
 * 注意：BST 是"左子树所有节点 < 根 < 右子树所有节点"
 *   —— 只比较左右孩子是不够的！（可能左孩子的右子树里有大值）
 */

// 解法一：中序遍历（BST 中序必须严格递增）
public boolean isValidBST(TreeNode root) {
    List<Integer> list = new ArrayList<>();
    inorder(root, list);
    for (int i = 1; i < list.size(); i++) {
        if (list.get(i) <= list.get(i - 1)) return false;  // 必须严格递增
    }
    return true;
}
private void inorder(TreeNode node, List<Integer> list) {
    if (node == null) return;
    inorder(node.left, list);
    list.add(node.val);
    inorder(node.right, list);
}

// 解法二：区间约束法（推荐，一次遍历）
public boolean isValidBST2(TreeNode root) {
    return validate(root, null, null);
}
private boolean validate(TreeNode node, Integer lo, Integer hi) {
    if (node == null) return true;
    if (lo != null && node.val <= lo) return false;   // 超出下界
    if (hi != null && node.val >= hi) return false;   // 超出上界
    return validate(node.left, lo, node.val)     // 左子树：上界收紧
        && validate(node.right, node.val, hi);   // 右子树：下界收紧
}
```

### 2.2 为什么只比较孩子会错？

```
反例：
       5
      / \
     1   4
        / \
       3   6

只看孩子：1<5<4 ✓ 误判合法
实际上 6 在根 5 的右子树中，6 > 5 违反 BST 定义！
→ 必须用"区间约束"或"中序递增"验证所有节点
```

---

## 3. 700/701. BST 搜索与插入

### 3.1 搜索（二分式递归）

```java
/**
 * LeetCode 700. BST 中的搜索
 * 二分思想：比根大往右，比根小往左
 */
public TreeNode searchBST(TreeNode root, int val) {
    if (root == null || root.val == val) return root;
    if (val < root.val) return searchBST(root.left, val);
    return searchBST(root.right, val);
}

// 迭代版
public TreeNode searchBST_Iterative(TreeNode root, int val) {
    while (root != null && root.val != val) {
        root = val < root.val ? root.left : root.right;
    }
    return root;
}
```

### 3.2 插入（找叶子位置）

```java
/**
 * LeetCode 701. BST 中的插入
 * 规则：新节点总是插到叶子位置（无需调整结构）
 */
public TreeNode insertIntoBST(TreeNode root, int val) {
    if (root == null) return new TreeNode(val);   // 找到插入位置

    if (val < root.val) {
        root.left = insertIntoBST(root.left, val);     // 插入左子树
    } else {
        root.right = insertIntoBST(root.right, val);   // 插入右子树
    }
    return root;
}
```

---

## 4. 450. 删除 BST 节点

### 4.1 三情况分类（本题核心）

```java
/**
 * LeetCode 450. 删除 BST 中的节点
 * 删除后的树仍必须是 BST。
 *
 * 三种情况：
 *   ① 叶子节点 → 直接删除（返回 null）
 *   ② 只有一个孩子 → 用孩子顶替（返回孩子）
 *   ③ 两个孩子 → 用右子树最小节点（或左子树最大节点）顶替：
 *        找到后继 → 复制值 → 删除右子树中的后继
 */
public TreeNode deleteNode(TreeNode root, int key) {
    if (root == null) return null;

    if (key < root.val) {
        root.left = deleteNode(root.left, key);       // 去左子树删
    } else if (key > root.val) {
        root.right = deleteNode(root.right, key);     // 去右子树删
    } else {
        // 找到要删除的节点 root

        // 情况 ②：只有一个孩子 → 返回孩子顶替
        if (root.left == null) return root.right;
        if (root.right == null) return root.left;

        // 情况 ③：两个孩子 → 找右子树最小值（中序后继）
        TreeNode successor = findMin(root.right);
        root.val = successor.val;                     // 复制值
        root.right = deleteNode(root.right, successor.val);  // 删除后继
    }
    return root;
}

private TreeNode findMin(TreeNode node) {
    while (node.left != null) node = node.left;   // 一路向左
    return node;
}
```

### 4.2 为什么用"后继"顶替？

```
删除有两个孩子的节点：
  中序后继（右子树最小值）是唯一满足 BST 性质的替代者：
    - 比左子树所有节点大（它来自右子树，右子树全比根大）
    - 比右子树其他节点小（它是右子树最小）
  → 用后继值顶替，BST 性质保持 ✓
  （也可以用左子树最大值，两种对称）
```

---

## 5. 230. 二叉搜索树第 K 小

### 5.1 中序计数法

```java
/**
 * LeetCode 230. 二叉搜索树中第 K 小的元素
 * 中序遍历天然有序 → 第 k 个访问到的就是答案
 */
public int kthSmallest(TreeNode root, int k) {
    int[] count = new int[]{0};
    int[] result = new int[1];
    inorder(root, k, count, result);
    return result[0];
}

private void inorder(TreeNode node, int k, int[] count, int[] result) {
    if (node == null || count[0] >= k) return;   // 剪枝

    inorder(node.left, k, count, result);
    if (++count[0] == k) {
        result[0] = node.val;                    // 第 k 个访问到
        return;
    }
    inorder(node.right, k, count, result);
}
```

### 5.2 进阶思考（面试加分）

```
追问：如果频繁查询第 K 小怎么办？
回答：给每个节点增加"左子树节点数"字段（size）
     → 搜索时根据 size 判断目标在左/右子树
     → 查询 O(log n)
     （等价于"树状数组/加权平衡树"思想）
```

---

## 6. BST 的构造

### 6.1 108. 有序数组转平衡 BST

```java
/**
 * LeetCode 108. 将有序数组转换为二叉搜索树
 * 选中间元素为根 → 左右递归 → 自然平衡
 */
public TreeNode sortedArrayToBST(int[] nums) {
    return build(nums, 0, nums.length - 1);
}

private TreeNode build(int[] nums, int lo, int hi) {
    if (lo > hi) return null;
    int mid = lo + (hi - lo) / 2;          // 中间元素做根
    TreeNode root = new TreeNode(nums[mid]);
    root.left = build(nums, lo, mid - 1);
    root.right = build(nums, mid + 1, hi);
    return root;
}
```

### 6.2 105. 前序+中序构造二叉树（经典构造题）

```java
/**
 * LeetCode 105. 从前序与中序遍历序列构造二叉树
 * 原理：
 *   ① 前序第一个元素 = 根
 *   ② 中序中找根 → 左边是左子树，右边是右子树
 *   ③ 递归构造
 * 优化：哈希表记录中序值→下标，O(1) 定位
 */
public TreeNode buildTree(int[] preorder, int[] inorder) {
    Map<Integer, Integer> idxMap = new HashMap<>();
    for (int i = 0; i < inorder.length; i++) {
        idxMap.put(inorder[i], i);           // 中序值 → 下标
    }
    return build(preorder, 0, preorder.length - 1,
                 inorder, 0, inorder.length - 1, idxMap);
}

private TreeNode build(int[] pre, int preLo, int preHi,
                       int[] in, int inLo, int inHi,
                       Map<Integer, Integer> idxMap) {
    if (preLo > preHi) return null;

    int rootVal = pre[preLo];                    // 前序第一个 = 根
    TreeNode root = new TreeNode(rootVal);
    int rootIdxIn = idxMap.get(rootVal);         // 中序中根的位置

    int leftSize = rootIdxIn - inLo;             // 左子树大小

    root.left = build(pre, preLo + 1, preLo + leftSize,
                      in, inLo, rootIdxIn - 1, idxMap);
    root.right = build(pre, preLo + leftSize + 1, preHi,
                       in, rootIdxIn + 1, inHi, idxMap);
    return root;
}
```

---

## 7. BST 面试题汇总

| 题号 | 题目 | 核心技巧 | 难度 |
|:---:|------|------|:---:|
| 98 | 验证 BST | 中序递增 / 区间约束 | 🟡 |
| 700 | BST 搜索 | 二分式比较 | 🟢 |
| 701 | BST 插入 | 找叶子位置 | 🟡 |
| 450 | BST 删除 | 三情况 + 后继顶替 | 🟡 |
| 230 | 第 K 小 | 中序计数 / size 优化 | 🟡 |
| 108 | 有序数组转 BST | 中间元素为根 | 🟢 |
| 105 | 前序+中序构造 | 哈希表 + 左子树大小 | 🟡 |
| 106 | 中序+后序构造 | 与 105 对称 | 🟡 |
| 538 | 累加树 | 反向中序（右根左） | 🟡 |
| 501 | BST 众数 | 中序 + 计数 | 🟢 |

> 🎯 **核心要点**：BST 的一切都源于**中序有序**这一性质。验证用中序/区间约束，删除记三情况（叶子/单孩子/双孩子用后继顶替），构造用"根定位 + 子树大小划分"。面试必答追问：BST 退化条件（插入有序序列变链表）与平衡方案（AVL/红黑树）。

---

**下一模块**：[04-树的进阶问题](04-树的进阶问题.md) | **返回总览**：[00-树知识体系总览](00-树知识体系总览.md)
