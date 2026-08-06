# 05 - BST 的应用

> BST 性质的应用题集：LCA 值比较 O(h)、两数之和中序双指针、众数连续统计、恢复被交换的 BST——四个经典场景展示「有序性红利」。

## 📚 目录

1. [LCA：值比较分岔（235 题）](#1-lca值比较分岔235-题)
2. [两数之和：中序双指针（653 题）](#2-两数之和中序双指针653-题)
3. [众数：中序连续统计（501 题）](#3-众数中序连续统计501-题)
4. [恢复 BST：中序逆序对（99 题）](#4-恢复-bst中序逆序对99-题)
5. [应用速查表](#5-应用速查表)

## 1. LCA：值比较分岔（235 题）

**原理**：p、q 与当前节点比较——都小向左、都大向右、**分岔（一大一小）即 LCA**。

```java
// 235 二叉搜索树的最近公共祖先（迭代，O(h)）
public TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
    while (root != null) {
        if (p.val < root.val && q.val < root.val) {
            root = root.left;              // 都在左子树
        } else if (p.val > root.val && q.val > root.val) {
            root = root.right;             // 都在右子树
        } else {
            return root;                   // 分岔点 = LCA（一个在左一个在右，或本身是 p/q）
        }
    }
    return null;
}
```

**对比普通二叉树 LCA（236）**：二叉树的 LCA 要递归遍历整个树 O(n)；BST 利用有序性每步排除一半子树 **O(h)**——性质红利的最好例证（二叉树体系 05 模块有 236 详解）。

## 2. 两数之和：中序双指针（653 题）

**原理**：BST 中序 = 有序数组 → 「两数之和」的有序双指针解法直接可用。

```java
// 653 两数之和 IV：BST 中序转有序列表 + 双指针
public boolean findTarget(TreeNode root, int k) {
    List<Integer> sorted = new ArrayList<>();
    inorder(root, sorted);                    // BST 中序 = 升序
    int left = 0, right = sorted.size() - 1;
    while (left < right) {
        int sum = sorted.get(left) + sorted.get(right);
        if (sum == k) return true;
        if (sum < k) left++;
        else right--;
    }
    return false;
}
```

> 💡 与哈希表解法（遍历时查 k - val）对比：哈希 O(n) 空间、双指针 O(1) 空间（需有序）——BST 天然有序，双指针是零成本红利；「有序 + 双指针」与双指针体系呼应。

## 3. 众数：中序连续统计（501 题）

**原理**：BST 中序 = 升序 → 相同值**必然相邻**，一趟中序统计即可。

```java
// 501 二叉搜索树中的众数
List<Integer> res = new ArrayList<>();
TreeNode prev = null;
int count = 0, maxCount = 0;

public int[] findMode(TreeNode root) {
    inorder(root);
    return res.stream().mapToInt(Integer::intValue).toArray();
}

void inorder(TreeNode node) {
    if (node == null) return;
    inorder(node.left);
    // 与上一个节点比较
    if (prev == null || node.val != prev.val) {
        count = 1;                            // 新值段
    } else {
        count++;                              // 连续相同
    }
    if (count > maxCount) {
        maxCount = count;
        res.clear();                          // 新众数，清空重来
        res.add(node.val);
    } else if (count == maxCount) {
        res.add(node.val);                    // 并列众数
    }
    prev = node;
    inorder(node.right);
}
```

> ⚠️ 众数题的陷阱：不用中序的话，普通二叉树需要 Map 统计（O(n) 空间）——BST 中序让「相同值相邻」，一趟遍历 O(1) 额外空间搞定。**「中序相邻」是 BST 频次/去重类题的共同钥匙**。

## 4. 恢复 BST：中序逆序对（99 题）

**问题**：BST 中恰好两个节点被交换，要求恢复（不改变结构）。

**原理**：正确 BST 中序严格递增；交换两个节点后中序出现**逆序对**——找到并交换回值即可。

```java
// 99 恢复二叉搜索树（Morris 中序可 O(1) 空间，此处给递归版）
TreeNode first = null, second = null, prev = null;

public void recoverTree(TreeNode root) {
    inorder(root);
    // 交换两个被交换的节点值
    int tmp = first.val;
    first.val = second.val;
    second.val = tmp;
}

void inorder(TreeNode node) {
    if (node == null) return;
    inorder(node.left);
    if (prev != null && prev.val > node.val) {
        if (first == null) first = prev;   // 第一个逆序对的前者
        second = node;                     // 不断更新（相邻交换时只出现一次）
    }
    prev = node;
    inorder(node.right);
}
```

**逆序对的两种情况**：
- **相邻交换**（如 1,3,2,4）：只出现 1 个逆序对 → first=3, second=2；
- **非相邻交换**（如 2,3,1,4 中 1 和 3 交换）：出现 2 个逆序对 → first 取第一个逆序对前者、second 取最后一个逆序对后者。

## 5. 应用速查表

| 题号 | 题目 | 核心思想 | 复杂度 |
|------|------|---------|--------|
| 235 | BST 的 LCA | 值比较分岔点 | O(h) |
| 653 | 两数之和 IV | 中序 + 双指针 | O(n) 时间 O(n) 空间 |
| 501 | 众数 | 中序相邻统计 | O(n) 时间 O(1) 空间 |
| 99 | 恢复 BST | 中序逆序对 | O(n) 时间 O(h) 空间 |
| 530 | 最小绝对差 | 中序相邻差 | O(n) |
| 783 | 最小距离 | 中序相邻差 | O(n) |
| 538 | 累加树 | 反向中序（右根左） | O(n) |

```java
// 538 把二叉搜索树转换为累加树：反向中序累加
int sum = 0;
public TreeNode convertBST(TreeNode root) {
    if (root == null) return null;
    convertBST(root.right);        // 先右（大值优先）
    sum += root.val;
    root.val = sum;                // 累加替换
    convertBST(root.left);
    return root;
}
```

> 🎯 **核心要点**：应用验收——235 LCA 值比较（O(h)）与二叉树 236 的对比能讲；「中序 = 有序数组」的转化思维（653 双指针、501 相邻统计、99 逆序对）；538 反向中序（右根左）是「中序」的逆向运用；这五个应用覆盖 BST 题 80% 的高频变体。

---

**上一模块**：[04-构造与转换](04-构造与转换.md) ｜ **下一模块**：[06-Java 实现与面试实战](06-Java实现与面试实战.md) ｜ **返回总览**：[00-二叉搜索树 BST 知识体系总览](00-二叉搜索树 BST 知识体系总览.md)

**【参考来源】**
- LeetCode 235/653/501/99/538
- 算法导论（CLRS）第 12 章
