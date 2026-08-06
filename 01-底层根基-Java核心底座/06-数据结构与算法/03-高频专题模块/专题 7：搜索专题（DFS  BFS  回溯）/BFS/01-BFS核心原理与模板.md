# BFS核心原理与模板
> 队列 + 逐层扩散——为什么 BFS 能找到最短路径？分层模板的正确打开方式

## 📚 目录
1. [BFS 的本质：洋葱模型](#1-bfs-的本质洋葱模型)
2. [为什么第一次到达即最短](#2-为什么第一次到达即最短)
3. [BFS 通用模板](#3-bfs-通用模板)
4. [visited 的标记时机（最大坑）](#4-visited-的标记时机最大坑)
5. [102. 二叉树层序遍历](#5-102-二叉树层序遍历)
6. [BFS vs DFS](#6-bfs-vs-dfs)

---

## 1. BFS 的本质：洋葱模型

### 1.1 定义

> 广度优先搜索（BFS）：从起点出发，**逐层向外扩散**——先访问距离 1 的所有节点，再访问距离 2 的所有节点，依次类推。像洋葱一层层剥开。

```
       起点 S
      /  |  \
    A    B    C        ← 第 1 层（距离 1）
   / \        / \
  D   E      F   G     ← 第 2 层（距离 2）

BFS 访问顺序：S → A,B,C → D,E,F,G
（按距离从小到大，同层顺序无关）
```

### 1.2 为什么用队列？

```
BFS 需要"先进先出"的顺序：
  先进入的节点（更近）先处理
  后进入的节点（更远）排队等待
  → 天然匹配队列（FIFO）

对比 DFS 用栈/递归（后进先出 → 深度优先）

记忆：BFS 队列 → 广度；DFS 栈 → 深度
```

### 1.3 适用场景判断

```
"最少几步 / 最短路径 / 扩散到所有位置 / 最近" → BFS
"所有方案 / 是否存在路径 / 连通块" → DFS

经典判断：迷宫问题求最短步数 → BFS
         迷宫问题求所有路径 → DFS/回溯
```

---

## 2. 为什么第一次到达即最短

### 2.1 逐层扩散的最短性证明

```
BFS 按层扩散：第 k 层节点 = 距离起点 k 步

证明（归纳法）：
  第 0 层：只有起点（距离 0）✓
  第 1 层：起点的所有邻居（距离 1）✓
  第 k 层：所有"距离 k 且未被访问"的节点
    —— 若某节点有更短路径（< k 步），它会在更早的层被访问
    —— 队列顺序保证层内节点先进先出

结论：节点第一次被访问时的层数 = 最短距离
  → BFS 求最短路径的前提：无权图（每条边代价相同）
  ⚠️ 带权图最短路径不能用 BFS（用 Dijkstra）
```

### 2.2 最短路径的两种返回方式

```java
// 方式一：记录层数（dist 数组）
//   dist[节点] = 第几次扩散到达

// 方式二：记录前驱（回溯路径）
//   prev[节点] = 从哪个节点来
//   最后从终点回溯 prev 得到完整路径
```

---

## 3. BFS 通用模板

### 3.1 基础模板

```java
/**
 * BFS 通用模板（求最短距离）
 */
public int bfs(Node start, Node target) {
    Queue<Node> queue = new LinkedList<>();
    Set<Node> visited = new HashSet<>();   // 或 boolean[]

    queue.offer(start);
    visited.add(start);        // ⚠️ 入队即标记！
    int steps = 0;

    while (!queue.isEmpty()) {
        int size = queue.size();           // ⚠️ 固定本层大小
        for (int i = 0; i < size; i++) {
            Node cur = queue.poll();
            if (cur == target) return steps;   // 到达目标

            for (Node next : cur.neighbors) {  // 遍历邻居
                if (!visited.contains(next)) {
                    queue.offer(next);
                    visited.add(next);         // 入队即标记
                }
            }
        }
        steps++;                             // 一层扩散完成
    }
    return -1;                               // 不可达
}
```

### 3.2 模板四要素

```
① Queue：FIFO 逐层扩散
② visited：防重复访问（入队时标记！）
③ size 分层：固定本层节点数（层数 = 步数）
④ steps：每层 +1，首次到达即最短

⚠️ 三层循环结构：
  while（队列非空）→ for（本层全部）→ for（邻居）
```

---

## 4. visited 的标记时机（最大坑）

### 4.1 错误演示：出队才标记

```java
// ❌ 错误：出队时标记
while (!queue.isEmpty()) {
    Node cur = queue.poll();
    if (visited.contains(cur)) continue;    // 太晚了！
    visited.add(cur);                       // 出队才标记
    for (Node next : cur.neighbors) {
        queue.offer(next);                  // 同一节点可能被多次入队！
    }
}

// 后果：节点被重复入队 → 队列膨胀 → 死循环/超时
// 例：A 和 B 是邻居，都入队 C →
//   C 被入队两次 → 处理两次 → 又重复入队它的邻居...
```

### 4.2 正确写法：入队即标记

```java
// ✅ 正确：入队时标记
queue.offer(next);
visited.add(next);          // 入队那一刻就标记

// 为什么？
//   BFS 中"第一次到达"就是最短 → 后续到达无需处理
//   入队时标记 → 每个节点恰好入队一次 → O(V+E)
```

> 🎯 **铁律**："**入队即标记，出队只处理**"——这是 BFS 面试最大坑，面试官最爱问"为什么这样写"。

---

## 5. 102. 二叉树层序遍历

### 5.1 完整实现（分层模板）

```java
/**
 * LeetCode 102. 二叉树的层序遍历（BFS 基础题）
 * 返回逐层节点列表
 *
 * 分层关键：每轮先记录 size，一次性处理完整层
 */
public List<List<Integer>> levelOrder(TreeNode root) {
    List<List<Integer>> result = new ArrayList<>();
    if (root == null) return result;

    Queue<TreeNode> queue = new LinkedList<>();
    queue.offer(root);

    while (!queue.isEmpty()) {
        int levelSize = queue.size();          // ⚠️ 先固定本层节点数！
        List<Integer> level = new ArrayList<>();

        for (int i = 0; i < levelSize; i++) {  // 处理完整一层
            TreeNode node = queue.poll();
            level.add(node.val);
            if (node.left != null) queue.offer(node.left);
            if (node.right != null) queue.offer(node.right);
        }
        result.add(level);                     // 收集本层
    }
    return result;
}
```

### 5.2 为什么必须固定 size？

```
❌ 错误：不用 size，直接 while(!queue.isEmpty())
  → 不知道当前层边界 → 所有节点混在一层

✅ 正确：int size = queue.size() 固定本层
  队列中此时恰好是本层节点（上一层已全部出队）
  处理完 size 个 → 本层结束 → 下一层已全部入队

变体：
  107 自底向上 → result.add(0, level)
  199 右视图 → 只加每层最后一个
  103 锯齿形 → 奇偶层反转
```

---

## 6. BFS vs DFS

### 6.1 对比总表

| 维度 | BFS | DFS |
|------|:---:|:---:|
| 数据结构 | 队列 | 栈/递归 |
| 遍历顺序 | 按层（广度） | 一条路走到底 |
| 最短路径 | ✅ 无权图第一次到达即最短 | ❌ 需要全部搜索 |
| 所有路径/方案 | 不擅长 | ✅ 天然适合 |
| 空间 | 大（存整层） | 小（存路径） |
| 连通块 | 可以 | 可以 |
| 拓扑排序 | Kahn（BFS） | DFS 颜色法 |

### 6.2 选择标准

```
"最短/最少/最近" → BFS（994/1091/127）
"所有方案/路径" → DFS/回溯（79/131）
"连通块大小" → 两者皆可（200 两种都能写）
"层级信息" → BFS（102）
"路径记录" → DFS（路径在递归栈中）

面试话术：
"这是最短步数问题 → BFS。无权图 BFS 逐层扩散，
 第一次到达目标就是最短距离。DFS 需要搜索
 所有路径才能确定最短，效率低。"
```

> 🎯 **核心要点**：BFS 的本质是**队列驱动的层扩散**，其正确性源于"第一次到达即最短"（无权图）。**入队即标记**是防死循环的铁律，**size 分层**是记录步数的关键。拿到"最短/最少/扩散"关键词直接 BFS 模板，O(V+E) 搞定。

---

**下一模块**：[02-网格BFS与多源BFS](02-网格BFS与多源BFS.md) | **返回总览**：[00-BFS知识体系总览](00-BFS知识体系总览.md)
