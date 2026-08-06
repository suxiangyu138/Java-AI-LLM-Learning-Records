# 03 - 图的遍历：DFS

> 深度优先搜索——「一路走到黑，撞墙再回头」。递归模板 + visited 标记，统一解决网格连通块、岛屿计数、判环、路径查找四类问题。DFS 是回溯与记忆化的地基（衔接[回溯算法](../../02-基础算法思想-核心/06-回溯与DFS/)）。

## 📚 目录

1. [DFS 核心思想](#1)
2. [图 DFS 模板](#2)
3. [网格 DFS（隐藏图）](#3)
4. [visited 的时机与形态](#4)
5. [DFS 应用清单](#5)

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

**三色标记（判环/拓扑排序前置）**：

```java
// DFS 判环：状态 0=未访问 1=递归栈中 2=已完成
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

> ⚠️ 无向图判环**不能用**「visited 即环」——无向边会来回走；无向图判环用「父节点排除法」：`dfs(u, parent)` 时跳过 `v == parent`，再遇 visited 才是环。

## 5. DFS 应用清单

| 应用 | 核心操作 | 典型题 |
|------|---------|--------|
| 连通分量计数 | 遍历起点计数 | 200 岛屿、547 省份 |
| 可达性 | 单源 DFS | 1971 寻找图中是否存在路径 |
| 环检测（有向） | 三色标记 | 207 课程表（配合拓扑） |
| 环检测（无向） | 父节点排除 | 684 冗余连接 |
| 路径枚举 | 回溯 + 撤销 | 797 所有路径 |
| 拓扑排序（DFS 版） | 完成序逆序 | 210 课程表 II（阶段三图论基础） |
| 记忆化 DFS | 缓存返回值 | 329 矩阵最长递增路径（衔接记忆化） |

```java
// 记忆化 DFS 模板：返回值的 DFS + 缓存（329 最长递增路径）
int dfsMemo(int[][] matrix, int x, int y, int[][] memo) {
    if (memo[x][y] != 0) return memo[x][y];
    int best = 1;
    for (int[] d : dirs) {
        int nx = x + d[0], ny = y + d[1];
        if (inBound(nx, ny) && matrix[nx][ny] > matrix[x][y]) {
            best = Math.max(best, 1 + dfsMemo(matrix, nx, ny, memo));
        }
    }
    return memo[x][y] = best;
}
```

> 🎯 **核心要点**：DFS 的验收标准——四类问题（连通块/可达/判环/路径）各能默写一个模板；记住三条纪律：①标记在进函数时；②网格题用方向数组当邻接表；③有向图判环用三色、无向图判环排除父节点。DFS 熟练后自然衔接[回溯算法](../../02-基础算法思想-核心/06-回溯与DFS/)（在 DFS 上加撤销动作）与[记忆化](../../03-高频专题模块/06-记忆化/)（在 DFS 上加缓存）。

---

**上一模块**：[02-图的存储结构](02-图的存储结构.md) ｜ **下一模块**：[04-图的遍历：BFS](04-图的遍历-BFS.md) ｜ **返回总览**：[00-图算法知识体系总览](00-图算法知识体系总览.md)

**【参考来源】**
- 算法导论（CLRS）第 22.3 节：深度优先搜索
- LeetCode 200 岛屿数量 / 207 课程表 / 329 矩阵最长递增路径
