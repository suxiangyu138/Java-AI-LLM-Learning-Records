# 网格BFS与多源BFS
> 方向数组 + 沉岛 + 多源扩散——面试最高频的 BFS 形态（岛屿/腐烂/距离）

## 📚 目录
1. [网格 BFS 模板](#1-网格-bfs-模板)
2. [200. 岛屿数量：沉岛](#2-200-岛屿数量沉岛)
3. [1091. 二进制矩阵最短路径](#3-1091-二进制矩阵最短路径)
4. [994. 腐烂的橘子：多源 BFS](#4-994-腐烂的橘子多源-bfs)
5. [542. 01 矩阵：多源距离](#5-542-01-矩阵多源距离)
6. [网格 BFS 总结](#6-网格-bfs-总结)

---

## 1. 网格 BFS 模板

```java
/**
 * 网格 BFS 通用模板
 * 四要素：方向数组、越界判断、visited/标记、size 分层
 */
public int gridBFS(int[][] grid, int[] start) {
    int m = grid.length, n = grid[0].length;
    int[][] dirs = {{0,1},{0,-1},{1,0},{-1,0}};   // 四方向
    boolean[][] visited = new boolean[m][n];

    Queue<int[]> queue = new LinkedList<>();
    queue.offer(start);
    visited[start[0]][start[1]] = true;   // 入队即标记

    int steps = 0;
    while (!queue.isEmpty()) {
        int size = queue.size();
        for (int i = 0; i < size; i++) {
            int[] cur = queue.poll();
            for (int[] d : dirs) {
                int nx = cur[0] + d[0];
                int ny = cur[1] + d[1];
                // ① 越界判断
                if (nx < 0 || nx >= m || ny < 0 || ny >= n) continue;
                // ② 障碍/已访问判断
                if (grid[nx][ny] == 1 || visited[nx][ny]) continue;

                visited[nx][ny] = true;   // 入队即标记
                queue.offer(new int[]{nx, ny});
            }
        }
        steps++;                          // 一层 = 一步
    }
    return steps;
}
```

---

## 2. 200. 岛屿数量：沉岛

### 2.1 完整实现（BFS 版）

```java
/**
 * LeetCode 200. 岛屿数量（Hot 100 高频）
 * 1 是陆地，0 是水，四方向相邻的 1 组成岛屿
 *
 * 思路：遍历网格，遇到 1 → 岛屿数+1 → BFS/DFS 把整个岛标记
 * 技巧：原地"沉岛"（1→0）省 visited 数组
 */
public int numIslands(char[][] grid) {
    int m = grid.length, n = grid[0].length;
    int count = 0;

    for (int i = 0; i < m; i++) {
        for (int j = 0; j < n; j++) {
            if (grid[i][j] == '1') {
                count++;                  // 发现新岛屿
                bfsSink(grid, i, j);      // 沉掉整座岛
            }
        }
    }
    return count;
}

private void bfsSink(char[][] grid, int i, int j) {
    int m = grid.length, n = grid[0].length;
    int[][] dirs = {{0,1},{0,-1},{1,0},{-1,0}};
    Queue<int[]> queue = new LinkedList<>();
    queue.offer(new int[]{i, j});
    grid[i][j] = '0';                     // ⚠️ 入队即沉岛！

    while (!queue.isEmpty()) {
        int[] cur = queue.poll();
        for (int[] d : dirs) {
            int nx = cur[0] + d[0], ny = cur[1] + d[1];
            if (nx < 0 || nx >= m || ny < 0 || ny >= n) continue;
            if (grid[nx][ny] != '1') continue;
            grid[nx][ny] = '0';           // 沉岛（代替 visited）
            queue.offer(new int[]{nx, ny});
        }
    }
}
```

### 2.2 三种解法对比

| 解法 | 时间 | 空间 | 适用 |
|------|:---:|:---:|------|
| DFS | O(mn) | O(mn) 栈 | 面试推荐（代码短） |
| **BFS** | O(mn) | O(mn) 队列 | 广度扩散直观 |
| 并查集 | O(mn α) | O(mn) | 动态合并加分 |

### 2.3 岛屿家族

| 题号 | 题目 | 变体 |
|:---:|------|------|
| 200 | 岛屿数量 | 基础 |
| 695 | 岛屿最大面积 | DFS 返回面积 |
| 463 | 岛屿周长 | 水/边界计数 |
| 1254 | 封闭岛屿 | 边界排除 |
| 130 | 被围绕的区域 | 从边界 BFS |

---

## 3. 1091. 二进制矩阵最短路径

### 3.1 完整实现（8 方向 + 最短）

```java
/**
 * LeetCode 1091. 二进制矩阵中的最短路径
 * 0 可走 1 障碍，从 (0,0) 到 (m-1,n-1)，8 方向
 * 求最短步数
 *
 * ⚠️ 8 方向：水平/垂直/对角（对角也算一步）
 * 最短性：BFS 第一次到达终点即最短
 */
public int shortestPathBinaryMatrix(int[][] grid) {
    int m = grid.length, n = grid[0].length;
    if (grid[0][0] == 1 || grid[m-1][n-1] == 1) return -1;   // 起点/终点障碍

    int[][] dirs = {{0,1},{0,-1},{1,0},{-1,0},
                    {1,1},{1,-1},{-1,1},{-1,-1}};   // ⚠️ 8 方向

    Queue<int[]> queue = new LinkedList<>();
    queue.offer(new int[]{0, 0});
    grid[0][0] = 1;                  // 标记已访问（原地标记）
    int steps = 1;                   // 起点算 1 步

    while (!queue.isEmpty()) {
        int size = queue.size();
        for (int i = 0; i < size; i++) {
            int[] cur = queue.poll();
            if (cur[0] == m-1 && cur[1] == n-1) {
                return steps;        // 第一次到达终点 → 最短 ✓
            }
            for (int[] d : dirs) {
                int nx = cur[0] + d[0], ny = cur[1] + d[1];
                if (nx < 0 || nx >= m || ny < 0 || ny >= n) continue;
                if (grid[nx][ny] == 1) continue;    // 障碍或已访问

                grid[nx][ny] = 1;   // 原地标记（省 visited 数组）
                queue.offer(new int[]{nx, ny});
            }
        }
        steps++;                    // 一层 = 一步
    }
    return -1;                      // 不可达
}
```

### 3.2 为什么可以原地标记？

```
网格题常用优化：grid[i][j] = 1（沉岛/标记）
  代替 visited 数组 → 省 O(mn) 空间
  前提：允许修改原数组（面试先说"我原地标记可以吗"）

变形：
  1293. 网格最短路径（最多消除 k 个障碍）
    → 状态扩展为 (x, y, 剩余消除次数)
  778. 水位上升的泳池
    → 二分 + BFS（答案二分见二分体系）
```

---

## 4. 994. 腐烂的橘子：多源 BFS

### 4.1 完整实现（多源 + 计时）

```java
/**
 * LeetCode 994. 腐烂的橘子（多源 BFS 经典）
 * 0 空 1 新鲜 2 腐烂；每分钟腐烂橘子感染四方向邻居
 * 求全部腐烂需要几分钟；有新鲜橘子无法感染返回 -1
 *
 * 多源 BFS：
 *   所有初始腐烂橘子同时入队（等价于超级源点）
 *   按层扩散计时（每层 = 1 分钟）
 *   同时统计新鲜橘子数 → 结束时 > 0 → -1
 */
public int orangesRotting(int[][] grid) {
    int m = grid.length, n = grid[0].length;
    Queue<int[]> queue = new LinkedList<>();
    int fresh = 0;

    // ① 所有腐烂橘子入队 + 统计新鲜数
    for (int i = 0; i < m; i++) {
        for (int j = 0; j < n; j++) {
            if (grid[i][j] == 2) queue.offer(new int[]{i, j});
            else if (grid[i][j] == 1) fresh++;
        }
    }

    // ② 多源 BFS 扩散
    int minutes = 0;
    int[][] dirs = {{0,1},{0,-1},{1,0},{-1,0}};
    while (!queue.isEmpty() && fresh > 0) {      // ⚠️ fresh > 0 剪枝
        int size = queue.size();
        for (int i = 0; i < size; i++) {
            int[] cur = queue.poll();
            for (int[] d : dirs) {
                int nx = cur[0] + d[0], ny = cur[1] + d[1];
                if (nx < 0 || nx >= m || ny < 0 || ny >= n) continue;
                if (grid[nx][ny] != 1) continue;   // 只感染新鲜

                grid[nx][ny] = 2;                // 腐烂（原地标记）
                fresh--;                         // 新鲜数-1
                queue.offer(new int[]{nx, ny});
            }
        }
        minutes++;                               // 1 分钟 = 1 层
    }
    return fresh == 0 ? minutes : -1;            // 有剩余新鲜 → -1
}
```

### 4.2 多源 BFS 的原理

```
多源 BFS 等价于：添加一个"超级源点"
  超级源点 → 所有初始腐烂橘子（距离 1）
  从超级源点 BFS → 距离 = 腐烂时间

好处：一次 BFS 解决"多个起点"问题
  （逐个 BFS 是 O(k×mn)，多源是 O(mn)）

多源家族：
  542. 01 矩阵（最近 0 的距离）
  1162. 地图分析（离陆地最远海洋）
  286. 墙与门（最近的门）
```

---

## 5. 542. 01 矩阵：多源距离

### 5.1 完整实现

```java
/**
 * LeetCode 542. 01 矩阵
 * 每个位置到最近 0 的曼哈顿距离
 *
 * 多源 BFS：所有 0 同时入队，逐层扩散
 *   第一层扩散 = 距离 1，第二层 = 距离 2 ...
 */
public int[][] updateMatrix(int[][] mat) {
    int m = mat.length, n = mat[0].length;
    Queue<int[]> queue = new LinkedList<>();
    int[][] dist = new int[m][n];

    // ① 所有 0 入队（多源），1 标记为未访问（-1）
    for (int i = 0; i < m; i++) {
        for (int j = 0; j < n; j++) {
            if (mat[i][j] == 0) {
                queue.offer(new int[]{i, j});
            } else {
                dist[i][j] = -1;          // 未访问标记
            }
        }
    }

    // ② 逐层扩散计算距离
    int[][] dirs = {{0,1},{0,-1},{1,0},{-1,0}};
    while (!queue.isEmpty()) {
        int[] cur = queue.poll();
        for (int[] d : dirs) {
            int nx = cur[0] + d[0], ny = cur[1] + d[1];
            if (nx < 0 || nx >= m || ny < 0 || ny >= n) continue;
            if (dist[nx][ny] != -1) continue;      // 已访问

            dist[nx][ny] = dist[cur[0]][cur[1]] + 1;   // 距离 = 父+1
            queue.offer(new int[]{nx, ny});
        }
    }
    return dist;
}
```

### 5.2 多源 vs 单源对比

| 问题 | 起点 | BFS 类型 | 结果 |
|:---:|:---:|:---:|------|
| 1091 | 1 个（左上角） | 单源 | 最短步数 |
| 994 | 多个（所有烂橘子） | 多源 | 扩散时间 |
| 542 | 多个（所有 0） | 多源 | 最近距离矩阵 |
| 1162 | 多个（所有陆地） | 多源 | 最远距离 |

---

## 6. 网格 BFS 总结

### 6.1 网格 BFS 五步框架

```
① 方向数组：四方向（+对角 = 8 方向）
② 入队初始：单源（起点）或多源（所有候选起点）
③ visited：boolean 数组 或 原地标记（沉岛/置 1）
④ 分层：size 固定本层，steps++ 记录距离
⑤ 终止：第一次到达目标（最短）或队列空（不可达）
```

### 6.2 面试话术

```
"网格 BFS 四要素：方向数组、越界判断、入队即标记、
 size 分层。
 这题是多源 BFS：所有腐烂橘子同时入队，
 等价于虚拟超级源点，逐层扩散计时。
 每个格子入队一次 → O(mn)。
 我选择原地标记（腐烂置 2），省 visited 数组空间。"
```

> 🎯 **核心要点**：网格 BFS 是面试最高频形态——**方向数组**（四/八方向）、**入队即标记**（visited 或沉岛）、**size 分层计步**是铁三角。**多源 BFS**（994/542）是重点变形：所有起点同时入队 = 超级源点，一次 BFS 解决多起点问题。200/1091/994 三道必背题覆盖了网格 BFS 的全部套路。

---

**下一模块**：[03-状态BFS与最短路径](03-状态BFS与最短路径.md) | **返回总览**：[00-BFS知识体系总览](00-BFS知识体系总览.md)
