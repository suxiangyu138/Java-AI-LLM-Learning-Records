# 02-二叉树DFS：路径与属性
> 一句话：路径题往下传"当前和/路径"、属性题向上传"子树信息"、BST 题传"上下界"——二叉树 DFS 的三种信息流

## 📚 目录
1. [二叉树 DFS 的三种信息流](#1-二叉树-dfs-的三种信息流)
2. [112 路径总和（boolean 返回）](#2-112-路径总和boolean-返回)
3. [113 路径总和 II 与 257 所有路径（收集路径）](#3-113-路径总和-ii-与-257-所有路径收集路径)
4. [98 验证二叉搜索树（上下界传递）](#4-98-验证二叉搜索树上下界传递)
5. [114 二叉树展开为链表（后序整合）](#5-114-二叉树展开为链表后序整合)
6. [938 二叉搜索树的范围和（BST 剪枝）](#6-938-二叉搜索树的范围和bst-剪枝)
7. [二叉树 DFS 易错点](#7-二叉树-dfs-易错点)

---

## 1. 二叉树 DFS 的三种信息流

```text
二叉树 DFS 的信息传递方式决定题型:

  ① 向下传参（前序）: 路径/和/上下界——
     例: 112 传"当前和"、98 传"上下界"
  ② 向上整合（后序）: 子树信息汇总——
     例: 104 树高、114 展开、打家劫舍 III
  ③ 中间处理（中序）: BST 有序性——
     例: 94 升序输出、98 的中序验证法

⚠️ 选型口诀:
  "答案依赖祖先信息 → 向下传参（前序）
   答案依赖子树信息 → 向上整合（后序）
   需要有序性 → 中序"
```

> 🎯 **核心认知**："**二叉树 DFS 的题型差异 = 信息流方向差异**——'路径总和'往下传和、'树高'往上收值、'BST 验证'传边界。识别'信息往哪流'，题型就定位了。"

## 2. 112 路径总和（boolean 返回）

### 2.1 题目与思路

**题**：是否存在根到叶子的路径和 = targetSum。

**向下传"剩余和"**（或当前和）+ boolean 返回：

```java
/**
 * LeetCode 112. 路径总和
 * 向下传剩余和；叶子节点判定
 * 时间 O(N)，空间 O(H)
 */
public boolean hasPathSum(TreeNode root, int targetSum) {
    if (root == null) return false;                // ① 空树无路径
    if (root.left == null && root.right == null) { // ② 叶子节点
        return targetSum == root.val;              //    剩余和 == 当前值？
    }
    // ③ 向下传"剩余和 − 当前值"
    return hasPathSum(root.left, targetSum - root.val)
        || hasPathSum(root.right, targetSum - root.val);
}
// 推演 [5,4,8,11,null,13,4,7,2,null,null,null,1], target=22 → true ✓
// ⚠️ 叶子判定: left == null && right == null——不是 root == null！
//   （root == null 是"路过空位"不是"到达叶子"）
```

> 💡 **叶子 vs 空节点**："**'叶子'（左右皆空）与'空节点'（null）是两种终止**——112 必须判叶子（路径到叶子结束）；104 树高判空节点（高度从空算起）。终止条件选错 = 整题错。"

## 3. 113 路径总和 II 与 257 所有路径（收集路径）

### 3.1 113 路径总和 II（收集所有路径）

```java
/**
 * LeetCode 113. 路径总和 II（收集所有路径）
 * 向下传当前和 + path；回溯撤销
 * 时间 O(N²)（拷贝路径），空间 O(H)
 */
public List<List<Integer>> pathSum(TreeNode root, int targetSum) {
    List<List<Integer>> result = new ArrayList<>();
    dfs(root, targetSum, new ArrayList<>(), result);
    return result;
}

private void dfs(TreeNode node, int remaining,
                 List<Integer> path, List<List<Integer>> result) {
    if (node == null) return;
    path.add(node.val);                            // ① 选（加入路径）

    if (node.left == null && node.right == null && remaining == node.val) {
        result.add(new ArrayList<>(path));         // ② 叶子且和达标 → 深拷贝收集
        // ⚠️ 收集后不 return——让撤销逻辑统一执行！
    } else {
        dfs(node.left, remaining - node.val, path, result);   // ③ 递
        dfs(node.right, remaining - node.val, path, result);
    }
    path.remove(path.size() - 1);                  // ④ 回（撤销）
}
// 推演 [5,4,8,11,13,4,7,2,5,1], target=22 → [[5,4,11,2],[5,8,4,5]] ✓
// ⚠️ 收集后不 return 的原因: return 会跳过撤销 →
//   用 else 分支区分"叶子收集"与"继续递归"，撤销统一在末尾
```

### 3.2 257 二叉树的所有路径（字符串路径）

```java
/**
 * LeetCode 257. 二叉树的所有路径
 * 前序 + 字符串路径（"1->2->5"）
 * 时间 O(N²)（字符串拼接），空间 O(H)
 */
public List<String> binaryTreePaths(TreeNode root) {
    List<String> result = new ArrayList<>();
    dfs(root, "", result);
    return result;
}

private void dfs(TreeNode node, String path, List<String> result) {
    if (node == null) return;
    String cur = path.isEmpty() ? String.valueOf(node.val)
                                : path + "->" + node.val;   // ① 拼接当前节点
    if (node.left == null && node.right == null) {
        result.add(cur);                           // ② 叶子收集
        return;
    }
    dfs(node.left, cur, result);                   // ③ 传新字符串（不可变天然撤销）
    dfs(node.right, cur, result);
}
// ⚠️ 字符串不可变 → 无需撤销（每层新建 cur）——比 List 版简单
```

> 🎯 **路径题的两种载体**："**List 路径要撤销（可变）、String 路径免撤销（不可变）**——'载体决定撤销'是路径题的实现细节。113 用 List（收集 List<Integer>），257 用 String（收集字符串）。"

## 4. 98 验证二叉搜索树（上下界传递）

### 4.1 题目与思路

**题**：判断二叉树是否 BST（左 < 根 < 右，所有子树成立）。

**向下传上下界**（前序）：

```java
/**
 * LeetCode 98. 验证二叉搜索树
 * 向下传 (lower, upper) 边界——每个节点值必须在区间内
 * 时间 O(N)，空间 O(H)
 */
public boolean isValidBST(TreeNode root) {
    return dfs(root, Long.MIN_VALUE, Long.MAX_VALUE);   // ① 初始区间全开
}

private boolean dfs(TreeNode node, long lower, long upper) {
    if (node == null) return true;
    if (node.val <= lower || node.val >= upper) return false;  // ② 越界 → 非法

    // ③ 左子树收紧上界、右子树收紧下界
    return dfs(node.left, lower, node.val)
        && dfs(node.right, node.val, upper);
}
// 推演 [5,1,4,null,null,3,6]：根 5 合法；
//   右子树 (5, ∞): 4 ≤ 5 → false ✓（经典反例——4 小于根 5）
// ⚠️ 为什么用 long: 节点值可能是 Integer.MIN_VALUE/MAX_VALUE——
//   边界要用 Long 防"恰好等于极值"的误判
// ⚠️ 为什么不能只比较父节点: BST 要求"所有祖先"约束——
//   只比父节点会放过 [5,1,4] 这种"4 小于根 5"的非法树
```

> 💡 **上下界传递的通用性**："**'向下传边界'是树 DFS 的经典信息流——98 传数值边界、112 传剩余和、938 传范围**。识别'约束来自祖先'就想到边界传递。"

## 5. 114 二叉树展开为链表（后序整合）

### 5.1 题目与思路

**题**：二叉树展开为右链（先序顺序，原地 O(1) 额外空间）。

**后序整合**：先处理子树，再把左子树接到右子树前：

```java
/**
 * LeetCode 114. 二叉树展开为链表（后序版）
 * 时间 O(N)，空间 O(H)
 * ⚠️ 后序: 先展开左右子树，再拼接
 */
public void flatten(TreeNode root) {
    if (root == null) return;
    flatten(root.left);                          // ① 先展开左子树
    flatten(root.right);                         // ② 再展开右子树

    TreeNode left = root.left;
    TreeNode right = root.right;

    root.left = null;                            // ③ 左指针清空
    root.right = left;                           // ④ 左子树接到右
    // ⑤ 找到新右链末尾，接上原右子树
    TreeNode cur = root;
    while (cur.right != null) cur = cur.right;
    cur.right = right;
}
// 推演 [1,2,5,3,4,null,6] → 1→2→3→4→5→6（先序顺序）✓
// ⚠️ 为什么后序: 展开右子树前需要"原右子树"引用——
//   先展开左右、再拼接，引用才不会丢
```

> 💡 **后序整合的识别**："**'需要子树完成后再处理当前'→ 后序**——114 展开、104 树高、打家劫舍 III 全是后序。'向上整合'是后序信息流的代表。"

## 6. 938 二叉搜索树的范围和（BST 剪枝）

### 6.1 题目与思路

**题**：BST 中 [low, high] 范围内节点值之和。

**BST 特性剪枝**：当前值 < low → 右子树才有戏；> high → 左子树才有戏：

```java
/**
 * LeetCode 938. 二叉搜索树的范围和
 * BST 剪枝: 利用有序性跳过无意义子树
 * 时间 O(N) 最坏（全在范围内）/ O(H) 平均（剪枝）
 */
public int rangeSumBST(TreeNode root, int low, int high) {
    if (root == null) return 0;
    if (root.val < low) {                        // ① 当前 < low → 左子树全小 → 只走右
        return rangeSumBST(root.right, low, high);
    }
    if (root.val > high) {                       // ② 当前 > high → 右子树全大 → 只走左
        return rangeSumBST(root.left, low, high);
    }
    // ③ 当前在范围内 → 计入 + 两侧都走
    return root.val
        + rangeSumBST(root.left, low, high)
        + rangeSumBST(root.right, low, high);
}
// 推演 [10,5,15,3,7,null,18], low=7, high=15 → 32（7+10+15）✓
// ⚠️ 剪枝依据: BST 有序性——"当前 < low 则左子树必然全 < low"
//   与回溯的"和约束 break"同一哲学: 提前知道走不通
```

> 🎯 **BST 剪枝的价值**："**938 展示了 DFS + 数据结构特性的组合——BST 的有序性让'剪枝'从可选项变成必然项**。'当前 < low 只走右'的单调性论证，与回溯剪枝的'排序后 break'完全同源。"

## 7. 二叉树 DFS 易错点

| # | 易错点 | 正确写法 | 后果 |
|:---:|------|------|------|
| 1 | 112 用 `root == null` 判终点 | 判叶子（左右皆空） | 空节点误算路径 |
| 2 | 113 收集后 return | 不 return（撤销统一） | 跳过撤销状态污染 |
| 3 | 98 用 int 边界 | 用 long（防极值误判） | Integer 极值边界错 |
| 4 | 98 只比较父节点 | 传上下界（所有祖先约束） | 放过非法 BST |
| 5 | 114 先拼接再展开 | 后序先展开再拼接 | 引用丢失 |
| 6 | 938 无剪枝 | 利用 BST 有序性跳过 | 多遍历（仍对但慢） |
| 7 | 路径 String 拼接顺序 | 前序（根在前） | 路径顺序反 |

> 🎯 **核心要点**：二叉树 DFS 通关四件事——**① 三种信息流识别**（向下传参/向上整合/中序有序）；**② 叶子 vs 空节点**（终止条件选对）；**③ 上下界传递**（98 的 long 边界 + 祖先约束）；**④ BST 剪枝**（938 的单调性）。**二叉树是 DFS 的第一战场——信息流方向识别对了，路径/属性/BST 三类题全部拿下**。

---

**下一模块**：[03-网格DFS：岛屿与洪水填充](03-网格DFS：岛屿与洪水填充.md)
**返回总览**：[00-DFS知识体系总览](00-DFS知识体系总览.md)
