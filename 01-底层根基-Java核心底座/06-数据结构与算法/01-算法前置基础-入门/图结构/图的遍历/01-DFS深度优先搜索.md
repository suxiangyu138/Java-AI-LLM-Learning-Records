# 01 - DFS 深度优先搜索

> 深度优先搜索——「一路走到黑，撞墙再回头」。递归模板 + visited 标记，统一解决网格连通块、岛屿计数、判环、路径查找四类问题。DFS 是回溯与记忆化的地基。

## 📚 目录

1. [DFS 核心思想](#1)
2. [图 DFS 模板](#2)
3. [网格 DFS（隐藏图）](#3)
4. [visited 的时机与形态](#4)
5. [DFS 判环：三色标记](#5)
6. [记忆化 DFS](#6)

## 1. DFS 核心思想

**深度优先**：从起点出发，沿一条边走到尽头，回溯后再走下一条——用**系统栈（递归）**模拟这个过程。

```text
       A
      / \
     B   C
    / \   \
   D   E   F

DFS 顺序：A → B → D → E → C → F
特点：先深入 B 的分支到底，再回头走 C
```

| 与 BFS 对比 | DFS | BFS |
|------------|-----|-----|
| 数据结构 | 栈（递归） | 队列 |
| 路径性质 | 不保证最短 | **首次到达即最短**（无权） |
| 空间 | O(深度) | O(层宽度) |
| 擅长 | 连通块、路径枚举、回溯 | 最短步数、层序、扩散 |

> 🎯 **选型一句话**：求**最少步数**用 BFS；**连通块/路径/判环**用 DFS——「能走多远先走多远」是 DFS 的本能。

## 2. 图 DFS 模板

```java
// 图 DFS 万能模板（连通分量 + 可达性）
class GraphDFS {
    boolean[] visited;

    void solve(int n, List<Integer>[] graph) {
        visited = new boolean[n];
        for (int i = 0; i < n; i++) {          // 防不连通：遍历所有起点
            if (!visited[i]) {
                dfs(graph, i);
                // 每次进入 = 一个新连通分量（计数在此处 +1）
            }
        }
    }

    void dfs(List<Integer>[] graph, int u) {
        visited[u] = true;                     // 标记：进函数立刻标记
        for (int v : graph[u]) {
            if (!visited[v]) {
                dfs(graph, v);
            }
        }
    }
}
```

**模板要点**：

| 要点 | 说明 |
|------|------|
| 外层 for | 防不连通（每个起点都尝试） |
| 进函数即标记 | 防环图重复入栈 |
| 递归深入 | 每层访问一个邻居 |
| 返回值 | 可达性/连通块可无返回；路径类返回结果 |

> ⚠️ **标记时机是铁律**：`visited[u] = true` 必须在**进入 dfs 时**（或入栈前），而不是访问完邻居后——否则环图中节点会被重复入栈，可能栈溢出。

## 3. 网格 DFS（隐藏图）

网格题不需要显式建图——邻居由方向数组推导：

```java
// LeetCode 200 岛屿数量：核心模板
class Solution {
    int m, n;
    int[][] dirs = {{1,0}, {-1,0}, {0,1}, {0,-1}};

    public int numIslands(char[][] grid) {
        m = grid.length; n = grid[0].length;
        int count = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == '1') {
                    count++;                   // 发现新岛屿
                    dfs(grid, i, j);           // 淹没整座岛
                }
            }
        }
        return count;
    }

    void dfs(char[][] grid, int x, int y) {
        if (x < 0 || x >= m || y < 0 || y >= n || grid[x][y] != '1') {
            return;                            // 越界或已访问
        }
        grid[x][y] = '0';                      // 标记：改为'0'避免重复（原地 visited）
        for (int[] d : dirs) {
            dfs(grid, x + d[0], y + d[1]);
        }
    }
}
```

**网格 DFS 三个变体**：

| 题目 | 标记方式 | 处理 |
|------|---------|------|
| 200 岛屿数量 | 原地改 `'1'→'0'` | 沉岛法，省 visited 数组 |
| 130 被围绕的区域 | 从边界 'O' 出发标记 | 标记不被围的，其余翻 X |
| 695 岛屿最大面积 | 返回值累加 | `return 1 + 四个方向之和` |

> 💡 **沉岛法**（原地修改）只适用于「网格内容可改写」的题；不可改写的场景（如染色类）用独立 visited 数组或 Set。

## 4. visited 的时机与形态

| 形态 | 适用 | 写法 |
|------|------|------|
| 布尔数组（基础） | 普通可达性/连通块 | `boolean[] visited` |
| 三色标记 | **环检测**（0 未访问/1 栈中/2 完成） | `int[] color` |
| 原地修改 | 网格可改写 | `grid[x][y] = '0'` |
| 路径记录 | 需要输出路径 | 栈 + 回溯（出栈时撤销） |

**路径记录（回溯）**：

```java
// 输出从 start 到 target 的路径：DFS + 回溯
List<Integer> path = new ArrayList<>();
List<Integer> result = null;

void dfsPath(List<Integer>[] graph, int u, int target) {
    if (result != null) return;                // 已找到
    path.add(u);
    visited[u] = true;
    if (u == target) {
        result = new ArrayList<>(path);        // 拷贝保存
        return;
    }
    for (int v : graph[u]) {
        if (!visited[v]) dfsPath(graph, v, target);
    }
    visited[u] = false;                        // 回溯：撤销标记
    path.remove(path.size() - 1);              // 回溯：撤销路径
}
```

> 💡 回溯 = DFS + **撤销**（visited 还原 + path 弹出）——「找所有路径/所有组合」类题的核心（衔接[回溯算法](../../02-基础算法思想-核心/回溯算法（Backtracking）/)体系）。

## 5. DFS 判环：三色标记

**三色标记**：0 未访问、1 递归栈中、2 已完成——遇到栈中节点 = 环。

```java
// DFS 判环（有向图）：状态 0=未访问 1=递归栈中 2=已完成
boolean dfsCycle(List<Integer>[] graph, int u, int[] color) {
    color[u] = 1;                              // 进入栈
    for (int v : graph[u]) {
        if (color[v] == 1) return true;        // 遇到栈中节点 = 环！
        if (color[v] == 0 && dfsCycle(graph, v, color)) return true;
    }
    color[u] = 2;                              // 完成出栈
    return false;
}
```

**无向图判环**（父节点排除法）：

```java
// 无向图判环：跳过父节点，再遇 visited 才是环
boolean dfsCycleUndirected(List<Integer>[] graph, int u, int parent, boolean[] visited) {
    visited[u] = true;
    for (int v : graph[u]) {
        if (v == parent) continue;             // 无向边天然来回
        if (visited[v]) return true;           // 非父节点已访问 = 环
        if (dfsCycleUndirected(graph, v, u, visited)) return true;
    }
    return false;
}
```

> ⚠️ 无向图判环**不能**用「visited 即环」——无向边会来回走；无向图判环用「父节点排除法」：`dfs(u, parent)` 时跳过 `v == parent`，再遇 visited 才是环。

## 6. 记忆化 DFS

**记忆化 DFS**：返回值的 DFS + 缓存——同一状态的重复计算只算一次。

```java
// 329 矩阵最长递增路径：记忆化 DFS（防爆栈 + 防超时）
class Solution {
    int m, n;
    int[][] memo;
    int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};

    public int longestIncreasingPath(int[][] matrix) {
        m = matrix.length; n = matrix[0].length;
        memo = new int[m][n];                  // 缓存：从 (i,j) 出发的最长路径
        int ans = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                ans = Math.max(ans, dfs(matrix, i, j));
            }
        }
        return ans;
    }

    int dfs(int[][] matrix, int x, int y) {
        if (memo[x][y] != 0) return memo[x][y];   // ① 缓存命中
        int best = 1;
        for (int[] d : dirs) {
            int nx = x + d[0], ny = y + d[1];
            if (nx >= 0 && nx < m && ny >= 0 && ny < n
                && matrix[nx][ny] > matrix[x][y]) {
                best = Math.max(best, 1 + dfs(matrix, nx, ny));
            }
        }
        return memo[x][y] = best;                 // ② 缓存写入
    }
}
```

**记忆化 vs 普通 DFS**：

| 对比 | 普通 DFS | 记忆化 DFS |
|------|:---:|:---:|
| 重复计算 | 可能大量重复 | **只算一次** |
| 状态 | visited（路径） | 返回值缓存 |
| 适用 | 可达性/连通 | **路径最值/计数** |
| 复杂度 | O(V+E) | 同（但常数大减） |

> 💡 记忆化的识别信号：**「从每个点出发的最长/最短路」**（329）、「组合计数」（子序列）——返回值可缓存即记忆化。

> 🎯 **核心要点**：DFS 的验收标准——四类问题（连通块/可达/判环/路径）各能默写一个模板；记住三条纪律：①标记在进函数时；②网格题用方向数组当邻接表；③有向图判环用三色、无向图判环排除父节点。DFS 熟练后自然衔接[回溯算法](../../02-基础算法思想-核心/回溯算法（Backtracking）/)（在 DFS 上加撤销动作）与 BFS（02 模块）。

---

**上一模块**：[00-知识体系总览](00-图的遍历知识体系总览.md) ｜ **下一模块**：[02-BFS 广度优先搜索](02-BFS广度优先搜索.md) ｜ **返回总览**：[00-图的遍历知识体系总览](00-图的遍历知识体系总览.md)

**【参考来源】**
- 算法导论（CLRS）第 22.3 节：深度优先搜索
- LeetCode 200 岛屿数量 / 207 课程表 / 329 矩阵最长递增路径
