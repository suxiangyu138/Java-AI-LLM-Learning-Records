# 02 - 遍历：DFS 与 BFS

> N 叉树遍历 = 二叉树遍历模板的「for 循环化」：`left/right` 换成 `for (child : children)`——前序、后序、层序三模板直接推广，二叉树体系的知识无缝迁移。

## 📚 目录

1. [N 叉前序遍历（589 题）](#1)
2. [N 叉后序遍历（590 题）](#2)
3. [N 叉层序遍历（429 题）](#3)
4. [二叉树 → N 叉的模板迁移](#4)
5. [遍历模板速查表](#5)

## 1. N 叉前序遍历（589 题）

**前序 = 根 → 依次遍历每个孩子**（与二叉树「根→左→右」同构）。

```java
// 589 N 叉树的前序遍历（递归）
public List<Integer> preorder(Node root) {
    List<Integer> res = new ArrayList<>();
    dfs(root, res);
    return res;
}

void dfs(Node node, List<Integer> res) {
    if (node == null) return;                  // ① 终止
    res.add(node.val);                         // ② 访问根（前序：先根）
    for (Node child : node.children) {         // ③ for 循环代替 left/right
        dfs(child, res);
    }
}
```

**迭代版**（显式栈，压栈顺序反转为孩子）：

```java
// 前序迭代：栈压孩子（从右到左压，左先出）
public List<Integer> preorder(Node root) {
    List<Integer> res = new ArrayList<>();
    if (root == null) return res;
    Deque<Node> stack = new ArrayDeque<>();
    stack.push(root);
    while (!stack.isEmpty()) {
        Node node = stack.pop();
        res.add(node.val);
        List<Node> children = node.children;
        for (int i = children.size() - 1; i >= 0; i--) {  // 逆序压栈
            stack.push(children.get(i));
        }
    }
    return res;
}
```

> 💡 与二叉树前序迭代对比：二叉树「先压右再压左」，N 叉树「逆序压全部孩子」——**同一思想：后进先出保证正序访问**。

## 2. N 叉后序遍历（590 题）

**后序 = 依次遍历每个孩子 → 根**。

```java
// 590 N 叉树的后序遍历（递归）
void dfs(Node node, List<Integer> res) {
    if (node == null) return;
    for (Node child : node.children) {         // 先全部孩子
        dfs(child, res);
    }
    res.add(node.val);                         // 再根（后序）
}

// 迭代：前序变体（根→孩子，反转结果）
// 前序收集「根→孩子」，反转后 = 「孩子→根」= 后序
public List<Integer> postorder(Node root) {
    List<Integer> res = new ArrayList<>();
    if (root == null) return res;
    Deque<Node> stack = new ArrayDeque<>();
    stack.push(root);
    while (!stack.isEmpty()) {
        Node node = stack.pop();
        res.add(node.val);
        for (Node child : node.children) {     // 正序压栈（与前序相反）
            stack.push(child);
        }
    }
    Collections.reverse(res);                  // 反转 = 后序
    return res;
}
```

## 3. N 叉层序遍历（429 题）

**层序 = 队列 + size 分层**——与二叉树 102 题**完全同模板**，只换 Node 类型。

```java
// 429 N 叉树的层序遍历
public List<List<Integer>> levelOrder(Node root) {
    List<List<Integer>> res = new ArrayList<>();
    if (root == null) return res;
    Deque<Node> queue = new ArrayDeque<>();
    queue.offer(root);
    while (!queue.isEmpty()) {
        int size = queue.size();               // 本层节点数
        List<Integer> level = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Node node = queue.poll();
            level.add(node.val);
            for (Node child : node.children) { // 孩子全部入队
                queue.offer(child);
            }
        }
        res.add(level);
    }
    return res;
}
```

**层序变形**（与二叉树同）：N 叉树最大深度（559 题）用层序计数层数即可。

## 4. 二叉树 → N 叉的模板迁移

| 二叉树模板 | N 叉树模板 | 变化点 |
|-----------|-----------|--------|
| `dfs(node.left)` / `dfs(node.right)` | `for (child : node.children) dfs(child)` | 两递归 → 循环 |
| 前序迭代压右再压左 | 逆序压全部孩子 | 压栈顺序 |
| 层序左右入队 | 全部孩子入队 | 入队操作 |
| `node.left == null && node.right == null` | `node.children.isEmpty()` | 叶子判定 |
| `Math.max(height(left), height(right))` | `for` 循环取 max | 两分支 → 循环 |

```java
// N 叉树最大深度（559 题）：迁移示例
public int maxDepth(Node root) {
    if (root == null) return 0;
    int depth = 0;
    for (Node child : root.children) {         // 循环代替两分支
        depth = Math.max(depth, maxDepth(child));
    }
    return depth + 1;
}
```

> 🎯 **迁移口诀**：**「两个子树 → for 循环，两个分支 → 循环取最值」**——二叉树的所有递归模板（深度/直径/路径/LCA）都能这样推广到 N 叉树。

## 5. 遍历模板速查表

| 遍历 | 递归 | 迭代核心 | 题号 |
|------|------|---------|------|
| 前序 | 根 → for 孩子 | 栈逆序压孩子 | 589 |
| 后序 | for 孩子 → 根 | 前序变体 + 反转 | 590 |
| 层序 | — | 队列 + size 分层 | 429 |
| 最大深度 | for 取 max + 1 | 层序计数 | 559 |

> 🎯 **核心要点**：N 叉遍历验收——三模板默写（递归一行 for、迭代逆序压栈、层序 size 分层）；「二叉树 → N 叉」的迁移口诀（两子树→for、两分支→循环最值）；理解前序迭代「逆序压栈」与后序迭代「反转」的技巧与二叉树一脉相承。经典问题（深度/直径/叶子）见下一节。

---

**上一模块**：[01-普通树的概念与表示](01-概念与表示.md) ｜ **下一模块**：[03-经典问题](03-经典问题.md) ｜ **返回总览**：[00-普通树知识体系总览](00-普通树知识体系总览.md)

**【参考来源】**
- LeetCode 589/590/429/559（N 叉树遍历与深度）
- 算法导论（CLRS）第 10.4 节
