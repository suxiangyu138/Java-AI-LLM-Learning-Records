# 网格DFS与沉岛
> 方向数组 + 原地标记——岛屿家族与网格路径的 DFS 实战

## 📚 目录
1. [网格 DFS 模板](#1-网格-dfs-模板)
2. [200. 岛屿数量：沉岛](#2-200-岛屿数量沉岛)
3. [695. 岛屿的最大面积](#3-695-岛屿的最大面积)
4. [79. 单词搜索：回溯 + visited](#4-79-单词搜索回溯--visited)
5. [130. 被围绕的区域](#5-130-被围绕的区域)
6. [岛屿家族总结](#6-岛屿家族总结)

---

## 1. 网格 DFS 模板

```java
/**
 * 网格 DFS 四步模板
 * ① 越界判断
 * ② 障碍/条件判断
 * ③ 标记（沉岛/visited）——必须放在递归前！
 * ④ 四方向递归
 */
public void gridDFS(char[][] grid, int i, int j) {
    int m = grid.length, n = grid[0].length;

    // ① 越界
    if (i < 0 || i >= m || j < 0 || j >= n) return;
    // ② 条件（障碍/已访问）
    if (grid[i][j] != '1') return;

    // ③ 标记（⚠️ 递归前！）
    grid[i][j] = '0';

    // ④ 四方向递归
    gridDFS(grid, i + 1, j);
    gridDFS(grid, i - 1, j);
    gridDFS(grid, i, j + 1);
    gridDFS(grid, i, j - 1);
}
```

> ⚠️ **标记太晚的后果**：如果先递归再标记，相邻格子会来回递归（A 访问 B，B 又访问 A）→ **栈溢出**。标记必须在递归调用之前！

---

## 2. 200. 岛屿数量：沉岛

### 2.1 完整实现（DFS 版）

```java
/**
 * LeetCode 200. 岛屿数量（Hot 100 最高频）
 * '1' 陆地 '0' 水，四方向相邻的 1 组成岛屿
 *
 * 沉岛法：遍历网格，遇 1 → 计数+1 → DFS 把整座岛改成 0
 *   → 每个格子只被处理一次 → O(mn)
 */
public int numIslands(char[][] grid) {
    int m = grid.length, n = grid[0].length;
    int count = 0;

    for (int i = 0; i < m; i++) {
        for (int j = 0; j < n; j++) {
            if (grid[i][j] == '1') {        // 发现新岛屿
                count++;
                dfsSink(grid, i, j);        // 沉掉整座岛
            }
        }
    }
    return count;
}

private void dfsSink(char[][] grid, int i, int j) {
    int m = grid.length, n = grid[0].length;
    if (i < 0 || i >= m || j < 0 || j >= n) return;
    if (grid[i][j] != '1') return;

    grid[i][j] = '0';                       // 沉岛（代替 visited）

    dfsSink(grid, i + 1, j);                // 四方向
    dfsSink(grid, i - 1, j);
    dfsSink(grid, i, j + 1);
    dfsSink(grid, i, j - 1);
}
```

### 2.2 为什么"沉岛"可行？

```
沉岛 = 原地标记（grid[i][j] = '0'）
  代替 visited 数组 → 省 O(mn) 空间
  且天然防止重复访问（'0' 不再是陆地）

面试说清楚：
"我用沉岛技巧——访问过的陆地改成 0，
 既标记了访问，又省了 visited 数组，
 还保证每个格子最多被处理一次，总复杂度 O(mn)。"
```

---

## 3. 695. 岛屿的最大面积

### 3.1 完整实现（DFS 返回面积）

```java
/**
 * LeetCode 695. 岛屿的最大面积
 * 网格中 1 的最大连通块面积
 *
 * DFS 返回"从当前格子出发的连通面积"
 *   = 1（自己）+ 四个方向的面积
 */
public int maxAreaOfIsland(int[][] grid) {
    int m = grid.length, n = grid[0].length;
    int maxArea = 0;

    for (int i = 0; i < m; i++) {
        for (int j = 0; j < n; j++) {
            if (grid[i][j] == 1) {
                maxArea = Math.max(maxArea, dfs(grid, i, j));
            }
        }
    }
    return maxArea;
}

private int dfs(int[][] grid, int i, int j) {
    int m = grid.length, n = grid[0].length;
    if (i < 0 || i >= m || j < 0 || j >= n) return 0;
    if (grid[i][j] != 1) return 0;

    grid[i][j] = 0;                          // 沉岛

    // 后序聚合：自己 + 四方向面积
    return 1 + dfs(grid, i + 1, j)
             + dfs(grid, i - 1, j)
             + dfs(grid, i, j + 1)
             + dfs(grid, i, j - 1);
}
```

### 3.2 200 vs 695 对比

| 题目 | DFS 角色 | 返回值 |
|:---:|:---:|:---:|
| 200 岛屿数量 | 沉岛（无返回值） | count++ |
| 695 最大面积 | 后序聚合面积 | 1 + 四方向 |

```
同一模板两种用法：
  需要"计数" → DFS 无返回值（200）
  需要"聚合大小" → DFS 返回面积（695）
```

---

## 4. 79. 单词搜索：回溯 + visited

### 4.1 完整实现（DFS + 回溯）

```java
/**
 * LeetCode 79. 单词搜索（Hot 100 高频）
 * 网格中是否存在路径匹配单词（四方向，不能重复使用格子）
 *
 * 与沉岛的区别：
 *   沉岛：不需要"恢复"（岛永久消失）
 *   本题：需要"恢复"（探索其他路径）→ 回溯！
 *   visited 标记 → 递归 → 撤销标记
 */
public boolean exist(char[][] board, String word) {
    int m = board.length, n = board[0].length;

    for (int i = 0; i < m; i++) {
        for (int j = 0; j < n; j++) {
            if (dfs(board, word, 0, i, j)) return true;   // 任一起点
        }
    }
    return false;
}

private boolean dfs(char[][] board, String word, int index,
                    int i, int j) {
    // ① 匹配完整个单词 → 找到
    if (index == word.length()) return true;

    // ② 越界 / 字符不匹配
    if (i < 0 || i >= board.length || j < 0 || j >= board[0].length) {
        return false;
    }
    if (board[i][j] != word.charAt(index)) return false;

    // ③ 标记（原地用 '\0' 代替 visited 数组）
    char temp = board[i][j];
    board[i][j] = '\0';

    // ④ 四方向探索
    boolean found = dfs(board, word, index + 1, i + 1, j)
                 || dfs(board, word, index + 1, i - 1, j)
                 || dfs(board, word, index + 1, i, j + 1)
                 || dfs(board, word, index + 1, i, j - 1);

    // ⑤ 撤销（回溯的灵魂！）
    board[i][j] = temp;

    return found;
}
```

### 4.2 沉岛 vs 回溯

| 场景 | 需要恢复？ | 标记方式 | 代表题 |
|------|:---:|:---:|:---:|
| 连通块计数 | ❌ 永久标记 | 沉岛（1→0） | 200/695 |
| 路径搜索 | ✅ 需要撤销 | visited/'\0' + 恢复 | 79 |
| 被围绕区域 | ❌ 永久标记 | 沉岛 | 130 |

```
记忆：
  "数清楚每块" → 沉岛（不用恢复）
  "找特定路径" → 回溯（必须恢复）
```

---

## 5. 130. 被围绕的区域

### 5.1 完整实现（逆向思维）

```java
/**
 * LeetCode 130. 被围绕的区域
 * 把被 'X' 完全包围的 'O' 改成 'X'
 * （边界上的 O 不会被包围）
 *
 * 逆向思维：从边界上的 O 开始 DFS，标记为 '#'
 *   → 剩下的 O 都是被包围的 → 改 X
 *   → '#' 恢复为 O
 */
public void solve(char[][] board) {
    int m = board.length, n = board[0].length;

    // ① 从四条边界的 O 开始 DFS（它们不会被包围）
    for (int i = 0; i < m; i++) {
        dfs(board, i, 0);            // 左边界
        dfs(board, i, n - 1);        // 右边界
    }
    for (int j = 0; j < n; j++) {
        dfs(board, 0, j);            // 上边界
        dfs(board, m - 1, j);        // 下边界
    }

    // ② 遍历：'O' → 'X'（被包围），'#' → 'O'（边界连通）
    for (int i = 0; i < m; i++) {
        for (int j = 0; j < n; j++) {
            if (board[i][j] == 'O') board[i][j] = 'X';
            else if (board[i][j] == '#') board[i][j] = 'O';
        }
    }
}

private void dfs(char[][] board, int i, int j) {
    int m = board.length, n = board[0].length;
    if (i < 0 || i >= m || j < 0 || j >= n) return;
    if (board[i][j] != 'O') return;

    board[i][j] = '#';               // 标记：与边界连通

    dfs(board, i + 1, j);
    dfs(board, i - 1, j);
    dfs(board, i, j + 1);
    dfs(board, i, j - 1);
}
```

### 5.2 逆向思维的价值

```
正向：判断每个 O 是否被包围 → 复杂（需要检查四个方向是否全 X）
逆向：从边界 O 出发标记 → 剩下的 O 必然被包围
  → 把"复杂判断"变成"简单标记"

面试话术："这题我逆向思考——从边界 O 出发 DFS
 标记连通区域，剩下的 O 就是被包围的。"
```

---

## 6. 岛屿家族总结

### 6.1 岛屿题全景

| 题号 | 题目 | 核心变化 |
|:---:|------|------|
| 200 | 岛屿数量 | 沉岛计数 |
| 695 | 最大面积 | DFS 返回面积 |
| 463 | 岛屿周长 | 水/边界计数 |
| 1254 | 封闭岛屿 | 边界连通不算 |
| 130 | 被围绕区域 | 逆向从边界标记 |
| 1020 | 飞地的数量 | 同 130 逆向 |

### 6.2 网格 DFS 解题框架

```
网格 DFS 五步：
① 遍历所有格子找起点（遇目标值）
② 越界判断
③ 条件判断（是否目标/已访问）
④ 标记（沉岛 或 visited 或 '\0'）
⑤ 递归四方向（或八方向）

需要恢复吗？
  计数/连通 → 不恢复（沉岛）
  路径搜索 → 恢复（回溯）
```

### 6.3 面试话术

```
"网格 DFS 模板四步：越界 → 条件 → 标记 → 递归。
 我用沉岛（改成 0）代替 visited，O(1) 额外空间。
 标记必须在递归前，否则相邻格子来回递归会栈溢出。
 每个格子最多访问一次 → O(mn)。"
```

> 🎯 **核心要点**：网格 DFS 是 DFS 面试主力——**四步模板**（越界/条件/标记/递归）+ **标记时机**（递归前！）+ **沉岛 vs 回溯**（计数不恢复、路径要恢复）。200/695/79/130 四题覆盖了计数、聚合、路径、逆向四个方向，掌握它们网格 DFS 全题型可解。

---

**下一模块**：[03-图DFS与判环](03-图DFS与判环.md) | **返回总览**：[00-DFS知识体系总览](00-DFS知识体系总览.md)
