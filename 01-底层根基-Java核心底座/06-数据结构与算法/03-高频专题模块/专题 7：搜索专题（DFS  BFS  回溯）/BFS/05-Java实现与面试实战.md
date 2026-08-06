# Java实现与面试实战
> BFS 高频题全覆盖 + 模板默写 + 追问——"入队即标记"一句话值千金

## 📚 目录
1. [LeetCode BFS 高频题映射](#1-leetcode-bfs-高频题映射)
2. [三大模板速查（默写版）](#2-三大模板速查默写版)
3. [面试解题话术](#3-面试解题话术)
4. [高频追问与标准回答](#4-高频追问与标准回答)
5. [高频题深度复盘](#5-高频题深度复盘)
6. [调试技巧与自检清单](#6-调试技巧与自检清单)

---

## 1. LeetCode BFS 高频题映射

### 1.1 全题单

| 题号 | 题目 | 类型 | 难度 | 一句话要点 | 对应模块 |
|:---:|------|------|:---:|------|:---:|
| 102 | 二叉树层序 | 基础 | 🟡 | size 分层 | [01-原理](01-BFS核心原理与模板.md) |
| 107 | 自底向上层序 | 基础 | 🟡 | 头插 | 01 模块 |
| 199 | 右视图 | 基础 | 🟡 | 每层最后一个 | 01 模块 |
| 103 | 锯齿形层序 | 基础 | 🟡 | 奇偶反转 | 01 模块 |
| 200 | 岛屿数量 | 网格 | 🟡 | 沉岛 | [02-网格](02-网格BFS与多源BFS.md) |
| 695 | 岛屿最大面积 | 网格 | 🟡 | DFS 返回面积 | 02 模块 |
| 1091 | 矩阵最短路径 | 网格 | 🟡 | 8 方向 | [02-网格](02-网格BFS与多源BFS.md) |
| 994 | 腐烂橘子 | 多源 | 🟡 | 多源计时 | [02-网格](02-网格BFS与多源BFS.md) |
| 542 | 01 矩阵 | 多源 | 🟡 | 距离扩散 | [02-网格](02-网格BFS与多源BFS.md) |
| 1162 | 地图分析 | 多源 | 🟡 | 最远海洋 | 02 模块 |
| 127 | 单词接龙 | 状态 | 🔴 | 改一字母 | [03-状态](03-状态BFS与最短路径.md) |
| 752 | 转盘锁 | 状态 | 🟡 | 8 种拨动 | [03-状态](03-状态BFS与最短路径.md) |
| 909 | 蛇梯棋 | 状态 | 🟡 | 骰子转移 | [03-状态](03-状态BFS与最短路径.md) |
| 207 | 课程表 | 拓扑 | 🟡 | Kahn 判环 | [04-进阶](04-BFS进阶-双向搜索与拓扑排序.md) |
| 210 | 课程表 II | 拓扑 | 🟡 | 输出拓扑序 | [04-进阶](04-BFS进阶-双向搜索与拓扑排序.md) |

### 1.2 面试频率 TOP 8

```
排名  题号  题目                面经出现频率
 1    102   层序遍历            ████████████████████
 2    200   岛屿数量            ████████████████
 3    994   腐烂的橘子          ██████████████
 4    127   单词接龙            ████████████
 5    207   课程表              ███████████
 6    1091  矩阵最短路径        █████████
 7    542   01 矩阵             ████████
 8    752   转盘锁              ████████
```

---

## 2. 三大模板速查（默写版）

```java
// ═══════════ 模板一：基础 BFS（分层）══════════════
// 适用：102/994/1091
Queue<Node> queue = new LinkedList<>();
Set<Node> visited = new HashSet<>();
queue.offer(start);
visited.add(start);              // ⚠️ 入队即标记！
int steps = 0;

while (!queue.isEmpty()) {
    int size = queue.size();     // 固定本层
    for (int i = 0; i < size; i++) {
        Node cur = queue.poll();
        if (到达目标) return steps;
        for (Node next : 邻居) {
            if (不合法 || visited.contains(next)) continue;
            visited.add(next);
            queue.offer(next);
        }
    }
    steps++;
}

// ═══════════ 模板二：网格 BFS ═══════════════════════
// 适用：200/1091/994
int[][] dirs = {{0,1},{0,-1},{1,0},{-1,0}};   // 8 方向加对角
Queue<int[]> queue = new LinkedList<>();
grid[sx][sy] = 标记;             // 原地标记（沉岛/置 1）
while (!queue.isEmpty()) {
    int[] cur = queue.poll();
    for (int[] d : dirs) {
        int nx = cur[0]+d[0], ny = cur[1]+d[1];
        if (越界 || 障碍 || 已标记) continue;
        grid[nx][ny] = 标记;
        queue.offer(new int[]{nx, ny});
    }
}

// ═══════════ 模板三：Kahn 拓扑 ═══════════════════════
// 适用：207/210
int[] indegree = new int[n];
Queue<Integer> queue = new LinkedList<>();
for (int i = 0; i < n; i++)
    if (indegree[i] == 0) queue.offer(i);   // 入度 0 入队
int processed = 0;
while (!queue.isEmpty()) {
    int cur = queue.poll();
    processed++;
    for (int next : graph.get(cur))
        if (--indegree[next] == 0) queue.offer(next);
}
return processed == n;          // 判环
```

---

## 3. 面试解题话术

```
📣 第 1 步：识别 (15秒)
"求最短步数 → BFS。无权图 BFS 逐层扩散，
 第一次到达目标即最短距离。"

📣 第 2 步：说结构 (30秒)
"用队列做逐层扩散，visited 记录已访问。
 关键细节：入队时标记 visited（不是出队时），
 防止同一节点重复入队。"

📣 第 3 步：说分层计步 (30秒)
"每轮先记录 queue.size()，一次处理完整一层，
 steps++ 就是步数/分钟数。
 第一次到达目标 → 返回 steps。"

📣 第 4 步：复杂度 (15秒)
"每个节点恰好入队一次 → O(V+E)（网格 O(mn)）。
 空间 O(V)。"

📣 第 5 步：验证 (30秒)
"用题目示例推演 1-2 层扩散过程。"
```

---

## 4. 高频追问与标准回答

| # | 追问 | 标准回答 |
|:---:|------|------|
| 1 | 为什么 BFS 能找到最短路径？ | BFS 按层扩散，第 k 层节点距离起点恰好 k 步；第一次到达目标时不可能有更短路径（更短的会在更早的层被访问） |
| 2 | visited 为什么入队时标记？ | 出队才标记会导致同一节点被多次入队（多个邻居同时发现它）→ 队列膨胀死循环；入队即标记保证每个节点恰好入队一次 |
| 3 | BFS 和 DFS 怎么选？ | 最短/最少/最近 → BFS；所有方案/路径 → DFS；连通块两者皆可 |
| 4 | 多源 BFS 的原理？ | 所有源点同时入队，等价于虚拟超级源点连到所有源点；一次 BFS 得到所有位置到最近源点的距离 |
| 5 | 带权图能用 BFS 求最短吗？ | 不能——BFS 的最短性依赖"每条边代价相同"；带权图用 Dijkstra（堆优化） |
| 6 | 双向 BFS 为什么快？ | 单向扩散 b^d 个状态；双向各扩散 d/2 层 → 2×b^(d/2)，开根号级别减少 |
| 7 | Kahn 怎么判环？ | 入度 0 的节点才能入队处理；环上节点入度永远 ≥1 无法处理 → 处理数 < n 即有环 |
| 8 | 状态 BFS 状态数多大可行？ | 状态数 × 转移数可控即可（如 10⁴×8）；10¹⁰ 量级 BFS 不可行，需换思路 |

---

## 5. 高频题深度复盘

### 5.1 200. 岛屿数量（BFS/DFS 双解）

```java
// BFS 版：沉岛
public int numIslands(char[][] grid) {
    int count = 0;
    for (int i = 0; i < grid.length; i++) {
        for (int j = 0; j < grid[0].length; j++) {
            if (grid[i][j] == '1') {
                count++;
                bfsSink(grid, i, j);
            }
        }
    }
    return count;
}
// 面试先写 DFS（代码短），再展示 BFS（队列版）
// 并查集作为第三种方案一提（动态合并场景）
```

**复盘要点**：
- 沉岛（1→0）代替 visited 数组
- 三解展示（DFS/BFS/并查集）是"全面掌握"的信号

### 5.2 207. 课程表（Kahn 判环）

```java
public boolean canFinish(int numCourses, int[][] prerequisites) {
    List<List<Integer>> graph = new ArrayList<>();
    int[] indegree = new int[numCourses];
    for (int i = 0; i < numCourses; i++) graph.add(new ArrayList<>());
    for (int[] pre : prerequisites) {
        graph.get(pre[1]).add(pre[0]);
        indegree[pre[0]]++;
    }
    Queue<Integer> queue = new LinkedList<>();
    for (int i = 0; i < numCourses; i++)
        if (indegree[i] == 0) queue.offer(i);

    int processed = 0;
    while (!queue.isEmpty()) {
        int cur = queue.poll();
        processed++;
        for (int next : graph.get(cur))
            if (--indegree[next] == 0) queue.offer(next);
    }
    return processed == numCourses;
}
```

**复盘要点**：
- 建图（邻接表）+ 入度 + 队列 + 计数 = 四步固定
- 边方向：先修 → 后续（pre[1] → pre[0]）
- 追问 DFS 颜色法（0/1/2 标记判环）时能讲即可

---

## 6. 调试技巧与自检清单

### 6.1 BFS 自检三步法

```
第 1 步：visited 时机检查
  □ 入队时标记了吗？（不是出队时！）
  □ 起点标记了吗？

第 2 步：分层检查
  □ int size = queue.size() 固定了吗？
  □ steps++ 在层循环外吗？
  □ 返回 steps 的时机对吗？（首次到达 vs 循环结束）

第 3 步：边界检查
  □ 起点/终点就是障碍吗？
  □ 空网格 / 单格子
  □ 全部可达 / 完全不可达
  □ 多源：所有源都入队了吗？
```

### 6.2 高频 Bug 速查表

| Bug | 原因 | 修复 |
|------|------|------|
| 死循环/超时 | visited 出队才标记 | 入队即标记 |
| 步数多 1 或少 1 | steps 初始化/更新时机错 | 起点算 0 还是 1 明确 |
| 层混在一起 | 没固定 size | `int size = q.size()` |
| 越界异常 | 网格判断顺序错 | 先越界再访问 |
| 岛屿漏数 | 忘记沉岛 | 遍历到 1 立即标记 |
| 拓扑误判 | 忘记统计入度 | 建图时同步 indegree |
| 目标不可达 | 返回错误值 | BFS 结束返回 -1/0 |

### 6.3 面试手写 Checklist

```
□ 先说"最短 → BFS"的识别依据
□ visited 入队即标记（强调！）
□ size 分层正确
□ 网格：方向数组 + 越界判断
□ 多源：所有起点入队
□ 拓扑：入度统计 + 判环
□ 复杂度 O(V+E)/O(mn)
□ 边界：空/单格/全障碍
□ 手动跑 1 个用例
```

> 🎯 **核心要点**：BFS 面试 = **模板**（分层 + 入队即标记）+ **形态识别**（网格/多源/状态/双向/拓扑）。"入队即标记"是防死循环铁律，"size 分层"是计步关键，多源 = 超级源点、双向 = 平方级加速、Kahn = 入度判环——五个形态模板默写 + 一句话识别，BFS 全题型稳拿分。

---

**返回总览**：[00-BFS知识体系总览](00-BFS知识体系总览.md)
