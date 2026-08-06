# 01 - BST 的性质与验证

> BST 的秩序：左 < 根 < 右，中序即升序。本节建立性质地基与验证模板——「区间传递」而非左右比较，是 98 题（验证 BST）的经典陷阱与标准解。

## 📚 目录

1. [BST 的定义](#1-bst-的定义)
2. [中序升序性质](#2-中序升序性质)
3. [验证 BST（98 题）](#3-验证-bst98-题)
4. [最小/最大与有序性](#4-最小最大与有序性)
5. [性质速查表](#5-性质速查表)

## 1. BST 的定义

**二叉搜索树（Binary Search Tree）**：满足**左子树所有节点 < 根 < 右子树所有节点**的二叉树。

```text
       5
      / \
     3   8        ← 合法 BST
    / \   \
   2   4   9

       5
      / \
     3   8        ← 非法！4 在 5 的右子树中，但 4 < 5
        /
       4
```

| 要点 | 说明 |
|------|------|
| 比较的是「所有节点」 | 不是只与直接孩子比较！ |
| 子树也是 BST | 递归定义 |
| 值允许重复吗 | 经典题默认不重复；重复值 BST 用 ≤ / ≥ 边界处理 |
| 时间复杂度 | 查找/插入/删除 O(h)，h 为树高（平衡时 O(log n)） |

> 💡 BST 本质 = **有序数组 + 树形结构**：既支持有序操作（第 K 小、范围查询），又避免数组插入 O(n) 的搬家成本。

## 2. 中序升序性质

**BST 最核心性质：中序遍历（左根右）= 升序序列**。

```text
       5
      / \
     3   8        中序: 2 3 4 5 8 9（严格升序）
    / \   \
   2   4   9
```

**两大应用**（贯穿整个 BST 体系）：
1. **验证**：中序应严格递增（98 题的另一解法）；
2. **有序输出**：第 K 小、范围统计、双向链表转换全靠它。

```java
// 中序输出 BST（02-二叉树体系的中序模板直接复用）
List<Integer> inorder(TreeNode root) {
    List<Integer> res = new ArrayList<>();
    Deque<TreeNode> stack = new ArrayDeque<>();
    TreeNode cur = root;
    while (cur != null || !stack.isEmpty()) {
        while (cur != null) { stack.push(cur); cur = cur.left; }
        cur = stack.pop();
        res.add(cur.val);
        cur = cur.right;
    }
    return res;
}
```

## 3. 验证 BST（98 题）

### 3.1 错误写法：只比较直接孩子

```java
// ❌ 错误：只检查 root.val 与左右孩子的直接大小
boolean isValidBST(TreeNode root) {
    if (root == null) return true;
    if (root.left != null && root.left.val >= root.val) return false;
    if (root.right != null && root.right.val <= root.val) return false;
    return isValidBST(root.left) && isValidBST(root.right);
}
// 反例：右子树里藏着一个比根小的节点（第 1 节第二个图）——本写法会误判合法
```

### 3.2 标准写法：区间传递

```java
// ✅ 正确：传递 (min, max) 区间约束
public boolean isValidBST(TreeNode root) {
    return valid(root, null, null);
}

boolean valid(TreeNode node, Integer min, Integer max) {
    if (node == null) return true;
    if (min != null && node.val <= min) return false;   // 超出下界
    if (max != null && node.val >= max) return false;   // 超出上界
    return valid(node.left, min, node.val)              // 左子树上限收紧
        && valid(node.right, node.val, max);            // 右子树下限收紧
}
```

> ⚠️ **区间传递 vs 左右比较**：BST 的约束是「全局区间」——左子树所有节点 < 根，右子树所有节点 > 根。只检查直接孩子无法发现「跨层违规」，必须把区间一路收紧传递。

### 3.3 中序解法

```java
// 中序 + 严格递增检查（利用性质 2）
TreeNode prev = null;
public boolean isValidBST(TreeNode root) {
    if (root == null) return true;
    if (!isValidBST(root.left)) return false;
    if (prev != null && prev.val >= root.val) return false;
    prev = root;
    return isValidBST(root.right);
}
```

| 解法 | 思路 | 复杂度 |
|------|------|--------|
| 区间传递（递归） | 收紧 (min, max) | O(n) 时间 O(h) 空间 |
| 中序递增 | 利用中序升序 | O(n) 时间 O(h) 空间 |

## 4. 最小/最大与有序性

```java
// BST 最小节点：一路向左
TreeNode minNode(TreeNode root) {
    while (root.left != null) root = root.left;
    return root;
}

// BST 最大节点：一路向右
TreeNode maxNode(TreeNode root) {
    while (root.right != null) root = root.right;
    return root;
}
```

**有序性带来的 O(log n) 红利**（对比普通二叉树 O(n)）：

| 操作 | 普通二叉树 | BST |
|------|:---:|:---:|
| 查找指定值 | O(n) | O(h) |
| 插入/删除 | 需遍历 | O(h) |
| 第 K 小 | O(n) | O(h + K) |
| 范围查询 | O(n) | O(h + K) |

> ⚠️ **h 的警告**：BST 复杂度取决于树高 h——按有序序列插入会退化成斜树（h = n），一切 O(h) 变 O(n)。**平衡化**（AVL/红黑树）就是为了保住 h = O(log n)，这也是[AVL 树](../AVL 树（平衡二叉搜索树）/)与[红黑树](../红黑树 Red‑Black Tree/)存在的意义。

## 5. 性质速查表

| 说法 | 含义 | 关联 |
|------|------|------|
| "BST 中序" | 升序序列（验证/第 K 小/转换） | 03/04 模块 |
| "严格递增" | 无重复值时中序严格升 | 98 题 |
| "左大右小" | 左子树全小、右子树全大 | 区间传递验证 |
| "退化成链表" | 有序插入 → 斜树 → O(n) | AVL/红黑树衔接 |
| "后继" | 中序序列下一个 | 03 模块 |
| "floor/ceil" | 小于等于/大于等于 x 的最大/最小值 | 面试扩展 |

> 🎯 **核心要点**：本节验收——能画出合法/非法 BST 的对比（跨层违规例子）、默写区间传递验证（98）、说出「中序升序」是 BST 一切有序操作的地基；「BST 不平衡退化 O(n)」是为 AVL/红黑树埋的伏笔。增删查实现见下一节，二叉树基础见[二叉树体系](../二叉树 Binary Tree/00-二叉树 Binary Tree 知识体系总览.md)。

---

**上一模块**：[00-知识体系总览](00-二叉搜索树 BST 知识体系总览.md) ｜ **下一模块**：[02-BST 的增删查](02-BST的增删查.md) ｜ **返回总览**：[00-二叉搜索树 BST 知识体系总览](00-二叉搜索树 BST 知识体系总览.md)

**【参考来源】**
- LeetCode 98 验证二叉搜索树
- 算法导论（CLRS）第 12.1 节：二叉搜索树的定义
