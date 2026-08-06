# 06 - Java 实现与面试实战

> 从模板到实战：五大必背模板、12 道高频题解、面试追问话术与易错点清单——二叉树面试的「肌肉记忆」全在这篇。

## 📚 目录

1. [五大必背 Java 模板](#1)
2. [高频题解速览](#2)
3. [面试追问与答题话术](#3)
4. [易错点清单](#4)

## 1. 五大必背 Java 模板

### 模板一：递归遍历（前序为例，中后序只移 add 位置）

```java
void dfs(TreeNode node, List<Integer> res) {
    if (node == null) return;
    res.add(node.val);        // 前序（中序放中间、后序放最后）
    dfs(node.left, res);
    dfs(node.right, res);
}
```

### 模板二：迭代中序（一路向左 + 弹栈）

```java
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

### 模板三：层序（队列 + size 分层）

```java
List<List<Integer>> levelOrder(TreeNode root) {
    List<List<Integer>> res = new ArrayList<>();
    if (root == null) return res;
    Deque<TreeNode> q = new ArrayDeque<>();
    q.offer(root);
    while (!q.isEmpty()) {
        int size = q.size();
        List<Integer> level = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            TreeNode node = q.poll();
            level.add(node.val);
            if (node.left != null) q.offer(node.left);
            if (node.right != null) q.offer(node.right);
        }
        res.add(level);
    }
    return res;
}
```

### 模板四：树形 DP（返回子树信息）

```java
// 543 直径：后序汇报高度 + 全局更新
int max = 0;
int depth(TreeNode node) {
    if (node == null) return 0;
    int left = depth(node.left);
    int right = depth(node.right);
    max = Math.max(max, left + right);   // 局部最优（经过本节点）
    return Math.max(left, right) + 1;    // 向父汇报
}
```

### 模板五：LCA 递归（236 四行）

```java
TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
    if (root == null || root == p || root == q) return root;
    TreeNode left = lowestCommonAncestor(root.left, p, q);
    TreeNode right = lowestCommonAncestor(root.right, p, q);
    if (left != null && right != null) return root;
    return left != null ? left : right;
}
```

> 🎯 五个模板约 70 行——**闭眼默写是二叉树面试的及格线**。

## 2. 高频题解速览

| 题号 | 题目 | 考点 | 一句话思路 |
|------|------|------|-----------|
| 104 | 最大深度 | 树形 DP | 返回 max(左,右)+1 |
| 102 | 层序遍历 | BFS 模板 | 队列 + size 分层 |
| 226 | 翻转二叉树 | 后序组合 | 交换左右子树 |
| 101 | 对称二叉树 | 双指针交叉 | a.left vs b.right |
| 110 | 平衡二叉树 | 后序哨兵 | 高度差>1 返 -1 |
| 543 | 直径 | 树形 DP | 全局 max(左高+右高) |
| 112 | 路径总和 | 自顶向下 | 传剩余值 |
| 236 | LCA | 递归四行 | 左右都有=根 |
| 105 | 构造二叉树 | 遍历应用 | 前序定根中序分界 |
| 297 | 序列化 | 递归一对 | null 标记空位 |
| 437 | 路径总和 III | 前缀和回溯 | 树上「和为 K」 |
| 114 | 展开为链表 | 前序/后序 | 右指针串接 |

```java
// 114 二叉树展开为链表（后序法）
public void flatten(TreeNode root) {
    if (root == null) return;
    flatten(root.left);
    flatten(root.right);
    TreeNode right = root.right;          // 保存右子树
    root.right = root.left;               // 左变右
    root.left = null;
    TreeNode cur = root;
    while (cur.right != null) cur = cur.right;   // 走到最右
    cur.right = right;                    // 接上原右子树
}
```

## 3. 面试追问与答题话术

| 追问 | 答题要点 |
|------|---------|
| 递归 vs 迭代遍历怎么选？ | 递归简洁、迭代防栈溢出；斜树深 10⁵ 时递归爆栈，必须迭代 |
| 中序遍历有什么用？ | BST 中序 = 升序序列，求第 K 小/验证 BST 都靠它 |
| 前序+后序能唯一构造树吗？ | 不能（分不清左右子树）；前/后 + 中序才能唯一 |
| 为什么平衡检查用 -1 哨兵？ | 避免重复计算高度，不平衡立即剪枝，O(n) |
| LCA 递归的原理？ | 三个终止（空/p/q）+ 左右结果合并（都有=根、单边传递） |
| 树形 DP 怎么识别？ | 「任意两节点/经过某点」的最值 → 后序返回子树信息 + 全局更新 |
| 完全二叉树怎么判断？ | 层序 + 空位标记：遇到 null 后不能再有节点 |
| 深度优先还是广度优先？ | 求深度/路径用 DFS；按层/最短/宽度用 BFS（与图 BFS 呼应） |

## 4. 易错点清单

| # | 易错点 | 后果 | 对策 |
|---|--------|------|------|
| 1 | 最小深度直接 Math.min | 单子树算错 | 单子树沿唯一孩子 |
| 2 | 平衡检查只在顶层判断 | 漏子树不平衡 | 后序 -1 哨兵传播 |
| 3 | 忘记处理 root == null | NPE | 每个递归先判空 |
| 4 | 序列化忘标记 null | 无法唯一还原 | null 占位符 |
| 5 | 构造树的中序下标每次扫描 | O(n²) 超时 | 哈希表缓存下标 |
| 6 | 路径收集忘撤销 | 路径污染 | 回溯 remove 末尾 |
| 7 | 递归深度爆栈（斜树） | StackOverflow | 迭代栈模板 |
| 8 | 返回值语义混淆 | 逻辑错乱 | 先写清「向父汇报什么」 |
| 9 | 437 前缀和忘撤销 | 计数错乱 | 出栈 merge -1 |
| 10 | 层序忘 size 分层 | 层级混合 | 每轮固定 size |

> 🎯 **核心要点**：二叉树面试通关路线——五个模板默写 → 12 道高频题 → 八个追问能答；核心心法「**递归三问 + 遍历选择**」贯穿全部题型；面试节奏建议：先递归后迭代、先判定后构造、先基础后 DP。进阶（回溯构造/树形 DP/莫里斯遍历）见[专题 4 进阶](../../../03-高频专题模块/专题 4：树（二叉树为主）/二叉树/01-二叉树进阶：回溯构造与树形DP.md)。

---

**上一模块**：[05-属性判断与 LCA](05-属性判断与LCA.md) ｜ **返回总览**：[00-二叉树 Binary Tree 知识体系总览](00-二叉树 Binary Tree 知识体系总览.md)

**【参考来源】**
- LeetCode 二叉树题单：https://leetcode.cn/studyplan/binary-tree/
- 算法导论（CLRS）第 10.4 / 12 章
