# 06 - Java 实现与面试实战

> 普通树面试收尾：五大模板（Node/前序/后序/层序/深度）、8 道高频题、面试追问话术、易错点——N 叉树的「肌肉记忆」全在这篇。

## 📚 目录

1. [五大必背模板](#1)
2. [高频题解速览](#2)
3. [面试追问与答题话术](#3)
4. [易错点清单](#4)

## 1. 五大必背模板

### 模板一：N 叉 Node 类

```java
class Node {
    public int val;
    public List<Node> children;

    public Node() {}
    public Node(int val) { this.val = val; }
    public Node(int val, List<Node> children) {
        this.val = val;
        this.children = children;
    }
}
```

### 模板二：前序递归

```java
void dfs(Node node, List<Integer> res) {
    if (node == null) return;
    res.add(node.val);
    for (Node child : node.children) dfs(child, res);
}
```

### 模板三：前序迭代（栈逆序压孩子）

```java
List<Integer> preorder(Node root) {
    List<Integer> res = new ArrayList<>();
    if (root == null) return res;
    Deque<Node> stack = new ArrayDeque<>();
    stack.push(root);
    while (!stack.isEmpty()) {
        Node node = stack.pop();
        res.add(node.val);
        for (int i = node.children.size() - 1; i >= 0; i--) {
            stack.push(node.children.get(i));   // 逆序压栈
        }
    }
    return res;
}
```

### 模板四：层序（队列 + size）

```java
List<List<Integer>> levelOrder(Node root) {
    List<List<Integer>> res = new ArrayList<>();
    if (root == null) return res;
    Deque<Node> q = new ArrayDeque<>();
    q.offer(root);
    while (!q.isEmpty()) {
        int size = q.size();
        List<Integer> level = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Node node = q.poll();
            level.add(node.val);
            for (Node child : node.children) q.offer(child);
        }
        res.add(level);
    }
    return res;
}
```

### 模板五：最大深度（循环 max）

```java
int maxDepth(Node root) {
    if (root == null) return 0;
    int depth = 0;
    for (Node child : root.children) {
        depth = Math.max(depth, maxDepth(child));
    }
    return depth + 1;
}
```

> 🎯 五个模板约 50 行——核心是「**两子树 → for 循环**」的迁移思想。

## 2. 高频题解速览

| 题号 | 题目 | 考点 | 一句话思路 |
|------|------|------|-----------|
| 589 | N 叉树前序遍历 | 前序模板 | 递归 for / 迭代逆序压栈 |
| 590 | N 叉树后序遍历 | 后序模板 | 前序变体 + 反转 |
| 429 | N 叉树层序遍历 | 层序模板 | 队列 + size 分层 |
| 559 | N 叉树最大深度 | 树形 DP | for 循环取 max |
| 428 | N 叉树序列化 | 序列化 | 孩子数 + 前序（`1[3[2][4][5]]`） |
| 558 | 四叉树交集 | 四叉树 | 递归合并（N=4 特例） |
| 431 | N 叉树编解码 | 二叉树互转 | 孩子兄弟编码 |
| 589 变体 | 树形结构扁平化 | 前序收集 | 组织树转列表 |

```java
// 428 序列化示例：前序 + 孩子数标记（"1[3[2][4][5]]" 风格）
// 序列化: 节点值 + [孩子数] + 递归孩子
// 反序列化: 读值 → 读孩子数 → 循环读孩子
```

## 3. 面试追问与答题话术

| 追问 | 答题要点 |
|------|---------|
| N 叉树和二叉树什么关系？ | 二叉树是 N=2 的特例；N 叉是工程的默认形态 |
| 遍历模板怎么迁移？ | 两子树 → for 循环；两分支 → 循环最值 |
| 为什么迭代前序要逆序压栈？ | 栈 LIFO：逆序压保证正序出 |
| 最小深度有什么坑？ | 空孩子不算路径，跳过空孩子 |
| 普通树能转二叉树吗？ | 能——孩子兄弟表示法（left=长子、right=兄弟） |
| 转换后遍历还一致吗？ | 先序/后序不变；中序语义变（孩子→兄弟链） |
| 工程树怎么存数据库？ | parent_id 父指针 + 应用层建孩子法树 |
| N 叉树能二分查找吗？ | 不能——孩子无序；有序多叉树是 B 树（多路搜索） |

## 4. 易错点清单

| # | 易错点 | 后果 | 对策 |
|---|--------|------|------|
| 1 | 迭代前序正序压栈 | 访问顺序错乱 | 逆序压孩子 |
| 2 | 最小深度 Math.min 直用 | 空孩子误判 | 跳过空孩子 |
| 3 | 孩子列表判空用 `null` | 叶子误判 | `children.isEmpty()` |
| 4 | 层序忘 size 分层 | 层级混合 | 每轮固定 size |
| 5 | 后序迭代忘反转 | 顺序错误 | Collections.reverse |
| 6 | 直径只取最大深度 | 漏次大深度 | 前两大 top-2 |
| 7 | 序列化忘标孩子数 | 无法还原 | 值 + [孩子数] |
| 8 | 转换后当普通二叉树遍历 | 语义混淆 | 记住 right = 兄弟 |
| 9 | 递归深度爆栈（超深目录） | StackOverflow | 迭代栈/层序 |
| 10 | 把 B 树当 N 叉树 | 概念混淆 | B 树节点多键有序 |

> 🎯 **核心要点**：N 叉面试通关——五模板 + 8 道高频题 + 八个追问；核心心法「**两子树 → for 循环**」让二叉树全部经验无缝迁移；工程认知（文件系统/DOM = 多叉树）是加分项；面试进阶方向：序列化（428）、二叉树互转（431）、四叉树（558）。衔接：[二叉树 Binary Tree](../二叉树 Binary Tree/00-二叉树 Binary Tree 知识体系总览.md)与[各种树全景](../计算机中的各种树/00-各种树全景总览.md)。

---

**上一模块**：[05-工程应用](05-工程应用.md) ｜ **返回总览**：[00-普通树知识体系总览](00-普通树知识体系总览.md)

**【参考来源】**
- LeetCode N-ary Tree 题单：https://leetcode.cn/tag/n-ary-tree/
- 算法导论（CLRS）第 10.4 节
