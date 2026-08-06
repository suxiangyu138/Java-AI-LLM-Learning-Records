# 05 - 属性判断与 LCA

> 二叉树的性质验证器：对称、平衡、子树、完全性、最近公共祖先（LCA）——「双指针比较」「后序汇报高度」「DFS 路径标记」三大模板在此定型。

## 📚 目录

1. [对称与相同（双指针比较）](#1)
2. [平衡与完全（后序汇报）](#2)
3. [子树判断](#3)
4. [最近公共祖先 LCA](#4)
5. [属性判断速查表](#5)

## 1. 对称与相同（双指针比较）

「两棵树（或一棵树的两个子树）是否相同/镜像」——双指针同步遍历：

```java
// 100 相同的树：双指针同步比较
public boolean isSameTree(TreeNode p, TreeNode q) {
    if (p == null && q == null) return true;
    if (p == null || q == null) return false;
    return p.val == q.val
        && isSameTree(p.left, q.left)     // 同侧比较
        && isSameTree(p.right, q.right);
}

// 101 对称二叉树：交叉比较（a 的左 vs b 的右）
boolean isMirror(TreeNode a, TreeNode b) {
    if (a == null && b == null) return true;
    if (a == null || b == null) return false;
    return a.val == b.val
        && isMirror(a.left, b.right)      // 交叉！
        && isMirror(a.right, b.left);
}

// 572 另一棵树的子树：isSameTree 套在任意节点上
public boolean isSubtree(TreeNode root, TreeNode subRoot) {
    if (root == null) return false;
    return isSameTree(root, subRoot)      // 当前节点相同
        || isSubtree(root.left, subRoot)  // 或左子树包含
        || isSubtree(root.right, subRoot);// 或右子树包含
}
```

> 💡 「相同」「对称」「子树」三题共用一套双指针比较模板——区别只在比较方向（同侧/交叉）与挂载方式（当前/递归找）。

## 2. 平衡与完全（后序汇报）

### 2.1 平衡二叉树（110）

**定义**：任意节点左右子树高度差 ≤ 1。自底向上验证，**发现不平衡立即剪枝**：

```java
// 110 平衡二叉树：返回高度，-1 表示不平衡
public boolean isBalanced(TreeNode root) {
    return height(root) != -1;
}

int height(TreeNode node) {
    if (node == null) return 0;
    int left = height(node.left);
    if (left == -1) return -1;            // 左子树已不平衡
    int right = height(node.right);
    if (right == -1) return -1;           // 右子树已不平衡
    if (Math.abs(left - right) > 1) return -1;   // 本节点不平衡
    return Math.max(left, right) + 1;
}
```

> ⚠️ 错误做法：先算左右高度再在**顶层**判断——只能判断根平衡，无法覆盖「子树内不平衡」；正确做法是把「不平衡」作为哨兵值（-1）沿后序传播。

### 2.2 完全二叉树（958）

**定义**：除最后一层外全满、末层靠左。**层序 + 空位标记**：

```java
// 958 完全二叉树检验：遇到 null 后不应再出现非 null
public boolean isCompleteTree(TreeNode root) {
    Deque<TreeNode> queue = new ArrayDeque<>();
    queue.offer(root);
    boolean seenNull = false;
    while (!queue.isEmpty()) {
        TreeNode node = queue.poll();
        if (node == null) { seenNull = true; continue; }
        if (seenNull) return false;       // 空位之后出现节点 = 非完全
        queue.offer(node.left);
        queue.offer(node.right);          // null 也入队（标记空位）
    }
    return true;
}
```

## 3. 子树判断

**572 另一棵树的子树**已在第 1 节给出（isSameTree 任意节点挂载）。

**进阶变体**：

| 变体 | 处理 | 题号 |
|------|------|------|
| 子树（值完全一致） | isSameTree 递归挂载 | 572 |
| 子结构（值可部分） | 匹配可提前终止 | 剑指 26 |
| 同构（结构一致值无关） | 比较结构忽略值 | 面试变体 |

> 💡 子树系列的本质 = **「相同树」判定 × 遍历所有节点**——把 02 模块的遍历模板与双指针比较组合。

## 4. 最近公共祖先 LCA

**LCA（Lowest Common Ancestor）**：p、q 的最深共同祖先。

### 4.1 递归法（236，经典）

```java
// 236 二叉树的最近公共祖先
public TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
    if (root == null || root == p || root == q) return root;  // ① 命中即返回
    TreeNode left = lowestCommonAncestor(root.left, p, q);    // ② 左找
    TreeNode right = lowestCommonAncestor(root.right, p, q);  //    右找
    if (left != null && right != null) return root;           // ③ 左右都有 = 根是 LCA
    return left != null ? left : right;                       // ④ 单边有 = 传递
}
```

**四行逻辑拆解**：
- ① 当前节点是 p/q/空 → 直接返回（终止条件）；
- ③ 左右子树各找到一个 → **当前节点就是 LCA**；
- ④ 只有一边找到 → 把找到的节点向上传递。

### 4.2 路径法（直观理解）

```java
// 思路：分别求 root→p、root→q 的路径，最后一个相同节点即 LCA
// 实现：前序 DFS + 回溯收集路径，再双指针比较
```

| 方法 | 复杂度 | 适用 |
|------|--------|------|
| 递归法 | O(n) 时间 O(h) 空间 | **默认（推荐）** |
| 路径法 | O(n) 时间 O(n) 空间 | 理解 LCA 语义、需路径本身 |

### 4.3 BST 的 LCA（235，特化）

**BST 性质**：p、q 都小于当前 → 向左；都大于 → 向右；否则当前即 LCA：

```java
public TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
    while (root != null) {
        if (p.val < root.val && q.val < root.val) root = root.left;
        else if (p.val > root.val && q.val > root.val) root = root.right;
        else return root;          // 分岔点（一大一小或相等）= LCA
    }
    return null;
}
```

> 💡 BST 的 LCA 从 O(n) 降到 O(h)——二叉搜索树性质的红利（详见[BST 体系](../二叉搜索树 BST（二叉排序树）/)）。

## 5. 属性判断速查表

| 题号 | 题目 | 模板 | 一句话思路 |
|------|------|------|-----------|
| 100 | 相同的树 | 双指针同侧 | 值 + 左左 + 右右 |
| 101 | 对称二叉树 | 双指针交叉 | 值 + 左右交叉 |
| 110 | 平衡二叉树 | 后序+哨兵 | 高度差>1 返回 -1 |
| 572 | 另一棵树的子树 | 双指针+遍历挂载 | 任意节点 isSameTree |
| 958 | 完全二叉树 | 层序+空位标记 | 见 null 后无节点 |
| 236 | 二叉树的 LCA | 递归四行 | 左右都有=根 |
| 235 | BST 的 LCA | 值比较 | 分岔点即答案 |

> 🎯 **核心要点**：本节验收——101/100/110 三模板默写（双指针同侧/交叉、后序哨兵 -1）；236 LCA 四行递归能讲清（命中返回、左右都有、单边传递）；958 的「空位标记」层序变体；LCA 的 BST 特化（235）是性质应用的典范。BST 体系见[二叉搜索树](../二叉搜索树 BST（二叉排序树）/)与[专题 4](../../../03-高频专题模块/专题 4：树（二叉树为主）/树/00-树知识体系总览.md)。

---

**上一模块**：[04-二叉树的路径问题](04-二叉树的路径问题.md) ｜ **下一模块**：[06-Java 实现与面试实战](06-Java实现与面试实战.md) ｜ **返回总览**：[00-二叉树 Binary Tree 知识体系总览](00-二叉树 Binary Tree 知识体系总览.md)

**【参考来源】**
- LeetCode 100/101/110/572/958/236/235
- 算法导论（CLRS）第 12 章
