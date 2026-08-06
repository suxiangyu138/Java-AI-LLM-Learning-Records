# DFS核心原理与模板
> 一条路走到底——前序/后序、递归与显式栈，DFS 的地基三要素

## 📚 目录
1. [DFS 的本质](#1-dfs-的本质)
2. [前序 vs 后序](#2-前序-vs-后序)
3. [递归 DFS 模板](#3-递归-dfs-模板)
4. [显式栈：递归转迭代](#4-显式栈递归转迭代)
5. [DFS 的应用场景](#5-dfs-的应用场景)
6. [DFS vs BFS](#6-dfs-vs-bfs)

---

## 1. DFS 的本质

### 1.1 定义

> 深度优先搜索（DFS）：从起点出发，**沿着一条路径走到尽头**，走不通时**回退（回溯）**到上一个节点，换一条未走过的路径继续，直到遍历完所有可达节点。

```
          A
        / | \
       B  C  D
      / \    |
     E   F   G

DFS（从 A 出发）：
  A → B → E（到底）→ 回退 B → F（到底）→ 回退 B → 回退 A
  → C（到底）→ 回退 A → D → G（到底）
  访问序：A, B, E, F, C, D, G
```

### 1.2 为什么用栈/递归？

```
DFS 需要"后进先出"的回退机制：
  走到尽头后要回到**最近**的岔路口
  → 天然匹配栈（LIFO）和递归（系统栈）

对比 BFS：队列（FIFO）先处理近的

记忆：DFS 栈 → 深度；BFS 队列 → 广度
```

### 1.3 两种实现

| 实现 | 机制 | 空间 | 栈溢出风险 |
|------|:---:|:---:|:---:|
| **递归** | 系统调用栈 | O(深度) | 深链可能溢出 |
| **显式栈** | 手动栈 | O(深度) | 可控（堆内存） |

> 💡 面试先写递归（代码简洁），被问"栈溢出怎么办"时展示显式栈。

---

## 2. 前序 vs 后序

### 2.1 定义

```
前序（Preorder）：先处理当前节点，再递归子树（自顶向下）
  处理(node) → dfs(left) → dfs(right)

后序（Postorder）：先递归子树，再处理当前节点（自底向上）
  dfs(left) → dfs(right) → 处理(node)

中序（Inorder）：左 → 处理 → 右（仅二叉树）
```

### 2.2 什么时候用哪个？

```
前序（自顶向下）：
  信息从根流向叶子 → 路径累积/传参
  例：257 所有路径、112 路径总和

后序（自底向上）：
  需要子树信息才能决策 → 返回值聚合
  例：104 深度、110 平衡、124 最大路径和、236 LCA

判定口诀：
  "左右孩子给我答案后，我能合并出自己的答案？"
  能 → 后序；不能 → 前序
```

### 2.3 代码对比

```java
// 前序：处理在递归前
void preorder(TreeNode node, 参数) {
    if (node == null) return;
    处理(node, 参数);          // 先处理
    preorder(node.left, 新参数);
    preorder(node.right, 新参数);
}

// 后序：处理在递归后
int postorder(TreeNode node) {
    if (node == null) return 0;
    int left = postorder(node.left);    // 先递归
    int right = postorder(node.right);
    return 合并(left, right, node);     // 后处理
}
```

---

## 3. 递归 DFS 模板

### 3.1 树 DFS 模板

```java
/**
 * 树 DFS 通用模板
 * 要素：递归出口（null 判断）+ 处理逻辑 + 递归调用
 */
public void treeDFS(TreeNode root, 参数) {
    if (root == null) return;              // ① 递归出口

    处理当前节点(root);                     // ② 前序处理

    treeDFS(root.left, 新参数);            // ③ 递归左
    treeDFS(root.right, 新参数);           // ④ 递归右
}
```

### 3.2 网格 DFS 模板

```java
/**
 * 网格 DFS 模板（四方向）
 * 要素：越界判断 + 障碍判断 + 标记 + 四方向递归
 */
public void gridDFS(int[][] grid, int i, int j) {
    int m = grid.length, n = grid[0].length;

    // ① 越界 / 障碍 / 已访问
    if (i < 0 || i >= m || j < 0 || j >= n) return;
    if (grid[i][j] == 0) return;           // 或 != 1

    grid[i][j] = 0;                        // ② 标记（沉岛）！

    // ③ 四方向递归
    gridDFS(grid, i + 1, j);               // 下
    gridDFS(grid, i - 1, j);               // 上
    gridDFS(grid, i, j + 1);               // 右
    gridDFS(grid, i, j - 1);               // 左
}
```

### 3.3 图 DFS 模板

```java
/**
 * 图 DFS 模板（邻接表）
 * visited 数组防重复访问
 */
public void graphDFS(List<List<Integer>> graph, int node,
                     boolean[] visited) {
    visited[node] = true;                  // 标记

    for (int next : graph.get(node)) {
        if (!visited[next]) {
            graphDFS(graph, next, visited);
        }
    }
}
```

---

## 4. 显式栈：递归转迭代

### 4.1 为什么需要显式栈

```
递归的问题：深度 = 递归深度
  树退化成链表（深度 n）→ 递归 n 层 → 栈溢出（StackOverflow）

显式栈：用堆内存的 Deque 模拟递归 → 无栈溢出风险
  面试加分："树可能退化为链表，递归 O(n) 栈空间可能溢出，
  我可以用显式栈转迭代。"
```

### 4.2 树前序遍历（显式栈）

```java
/**
 * 树前序迭代（显式栈）
 * 栈模拟递归：先压右再压左（后进先出 → 先处理左）
 */
public List<Integer> preorderIterative(TreeNode root) {
    List<Integer> result = new ArrayList<>();
    if (root == null) return result;

    Deque<TreeNode> stack = new ArrayDeque<>();
    stack.push(root);

    while (!stack.isEmpty()) {
        TreeNode node = stack.pop();
        result.add(node.val);                 // 前序处理
        if (node.right != null) stack.push(node.right);   // 右后压
        if (node.left != null) stack.push(node.left);     // 左先出
    }
    return result;
}
```

### 4.3 图 DFS（显式栈）

```java
// 图 DFS 迭代版
Deque<Integer> stack = new ArrayDeque<>();
boolean[] visited = new boolean[n];
stack.push(start);

while (!stack.isEmpty()) {
    int node = stack.pop();
    if (visited[node]) continue;         // ⚠️ 迭代版需要在出栈时判重
    visited[node] = true;
    for (int next : graph.get(node)) {
        if (!visited[next]) stack.push(next);
    }
}
// ⚠️ 与 BFS 不同：DFS 迭代版可以出栈时标记
// （因为栈不会像队列那样"重复入队膨胀"）
// 但入栈时标记可以避免重复入栈（性能更好）
```

---

## 5. DFS 的应用场景

### 5.1 场景判定

```
"所有路径/方案" → DFS（257/79/131）
"连通块大小/数量" → DFS（200/695）
"是否存在路径" → DFS（841 钥匙和房间）
"树的属性计算" → DFS（104/124）
"图判环/染色" → DFS 颜色法（207/785）

一句话："存在性/枚举/聚合" → DFS；"最短/最少" → BFS
```

### 5.2 典型应用速查

| 场景 | 代表题 | DFS 角色 |
|------|:---:|------|
| 树遍历/属性 | 104/124 | 前/后序 |
| 网格连通 | 200/695 | 沉岛 |
| 网格路径 | 79 | 回溯+visited |
| 图遍历 | 133 | 克隆/映射 |
| 图染色 | 785 | 二分图 |
| 图判环 | 207 | 颜色法 |
| 记忆化 | 329 | 缓存 |

---

## 6. DFS vs BFS

| 维度 | DFS | BFS |
|------|:---:|:---:|
| 数据结构 | 栈/递归 | 队列 |
| 顺序 | 深度优先 | 逐层 |
| 最短路径 | ❌ | ✅ 无权图 |
| 所有方案 | ✅ | 不擅长 |
| 空间 | O(深度)（小） | O(层宽)（大） |
| 连通块 | ✅ | ✅ |
| 路径记录 | ✅ 天然（递归栈） | 需 prev 数组 |

```
选择口诀：
  "最短/最少/最近" → BFS
  "所有方案/存在/连通" → DFS
  "层信息" → BFS；"路径枚举" → DFS
```

> 🎯 **核心要点**：DFS 的本质是**栈驱动的深度探索**——递归（简洁）或显式栈（防溢出）。**前序**处理自顶向下传参（路径题），**后序**聚合子树信息（属性题）。网格模板记住"越界→障碍→标记→四方向"四步，标记（沉岛）必须在递归前。DFS 与回溯的区别只差"撤销"一步。

---

**下一模块**：[02-网格DFS与沉岛](02-网格DFS与沉岛.md) | **返回总览**：[00-DFS知识体系总览](00-DFS知识体系总览.md)
