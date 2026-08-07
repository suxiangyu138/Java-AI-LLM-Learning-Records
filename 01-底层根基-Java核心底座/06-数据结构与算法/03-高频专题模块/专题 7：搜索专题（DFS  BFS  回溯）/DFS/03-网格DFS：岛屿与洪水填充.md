# 03-网格DFS：岛屿与洪水填充
> 一句话：方向数组 + 沉岛标记（前置！）——200 数量、695 面积、130 边界技巧、79 撤销标记，岛屿系列一模板通吃

## 📚 目录
1. [网格 DFS 模型：矩阵即图](#1-网格-dfs-模型矩阵即图)
2. [200 岛屿数量（沉岛模板）](#2-200-岛屿数量沉岛模板)
3. [695 岛屿的最大面积（返回值型）](#3-695-岛屿的最大面积返回值型)
4. [130 被围绕的区域（边界逆向技巧）](#4-130-被围绕的区域边界逆向技巧)
5. [79 单词搜索（visited + 撤销）](#5-79-单词搜索visited--撤销)
6. [岛屿系列变体](#6-岛屿系列变体)
7. [网格 DFS 易错点](#7-网格-dfs-易错点)

---

## 1. 网格 DFS 模型：矩阵即图

```text
矩阵转图:
  每个格子 (i,j) = 一个节点
  上下左右四方向 = 边（方向数组）
  越界 = 图的边界

四件套模板:
  ① 越界检查（前置！）
  ② 目标判断（是 '1' 吗？）
  ③ 标记（沉岛 / visited）——必须前置！
  ④ 四方向递归（方向数组循环）

复杂度: 时间 O(mn)（每格最多访问一次），空间 O(mn)（全陆地时栈深）

⚠️ 标记前置的原因:
  先递归再标记 → 相邻格子来回递归（A→B→A→B…）→ 栈溢出
  标记前置 → 每个格子只进入一次
```

> 🎯 **核心认知**："**网格 DFS = 图 DFS 的特例——'坐标当节点、方向数组当边'**。四件套的顺序是纪律：越界先判、标记前置、方向循环，任何一步颠倒都会崩。"

## 2. 200 岛屿数量（沉岛模板）

### 2.1 题目与思路

**题**：'1'（陆地）连成的岛屿数量（四方向连通）。

**扫描 + 沉岛**：遇到 '1' 计数 +1，DFS 把整座岛标记掉：

```java
/**
 * LeetCode 200. 岛屿数量
 * 时间 O(mn)，空间 O(mn)
 */
public int numIslands(char[][] grid) {
    int count = 0;
    for (int i = 0; i < grid.length; i++) {
        for (int j = 0; j < grid[0].length; j++) {
            if (grid[i][j] == '1') {           // ① 遇到未访问陆地
                count++;                       // ② 新岛屿计数
                dfs(grid, i, j);               // ③ 整岛沉掉（标记）
            }
        }
    }
    return count;
}

private void dfs(char[][] grid, int i, int j) {
    if (i < 0 || i >= grid.length || j < 0 || j >= grid[0].length) return;  // ④ 越界先判
    if (grid[i][j] != '1') return;             // ⑤ 水/已沉跳过
    grid[i][j] = '2';                          // ⑥ ⚠️ 沉岛（标记前置）！
    dfs(grid, i + 1, j);                       // ⑦ 四方向
    dfs(grid, i - 1, j);
    dfs(grid, i, j + 1);
    dfs(grid, i, j - 1);
}
// 推演 4×5 网格（3 块岛）→ 3 ✓
// ⚠️ 为什么"整岛沉掉"后不会重复计数:
//   第一块陆地 DFS 后整岛变 '2' → 后续扫描跳过 → 每岛只计一次
```

> 🎯 **200 的模板地位**："**200 是网格 DFS 的母题——'扫描 + 沉岛'模式在 695/130/岛屿变体中全部复用**。'沉岛'（原地改值）比 visited 数组省空间且天然防重，是岛屿题的标志性手法。"

## 3. 695 岛屿的最大面积（返回值型）

### 3.1 题目与思路

**题**：最大的岛屿面积（连通 '1' 的个数）。

**DFS 返回面积**（1 + 四方向之和）：

```java
/**
 * LeetCode 695. 岛屿的最大面积
 * 返回值型 DFS: 面积 = 1 + 四方向面积之和
 * 时间 O(mn)，空间 O(mn)
 */
public int maxAreaOfIsland(int[][] grid) {
    int best = 0;
    for (int i = 0; i < grid.length; i++) {
        for (int j = 0; j < grid[0].length; j++) {
            if (grid[i][j] == 1) {
                best = Math.max(best, dfs(grid, i, j));   // ① 每岛算面积取最大
            }
        }
    }
    return best;
}

private int dfs(int[][] grid, int i, int j) {
    if (i < 0 || i >= grid.length || j < 0 || j >= grid[0].length) return 0;
    if (grid[i][j] != 1) return 0;             // ② 水/已沉 → 0 面积
    grid[i][j] = 0;                            // ③ 沉岛（标记前置）

    return 1                                   // ④ 自己 + 四方向
        + dfs(grid, i + 1, j)
        + dfs(grid, i - 1, j)
        + dfs(grid, i, j + 1)
        + dfs(grid, i, j - 1);
}
// 推演 5×8 网格 → 最大面积 6 ✓
// ⚠️ 与 200 的差异: 返回值型（面积）vs void 型（只标记）——
//   同样的沉岛模板，返回值让"计数"变"求和"
```

> 💡 **返回值型 vs void 型**："**200 只标记（void）、695 要算面积（返回值）**——'要不要从子树拿结果'决定返回类型。会 200 改 695 只差一个返回值，这就是模板复用的力量。"

## 4. 130 被围绕的区域（边界逆向技巧）

### 4.1 题目与思路

**题**：把被 X 包围的 O 改为 X（边界 O 不被包围）。

**逆向思维**：先标记所有"边界可达的 O"（不会被包围），再遍历改值：

```java
/**
 * LeetCode 130. 被围绕的区域
 * 逆向: 边界 O 不可能被包围 → 先标记为 T，再统一改
 * 时间 O(mn)，空间 O(mn)
 */
public void solve(char[][] board) {
    int m = board.length, n = board[0].length;

    // ① 从四条边界 DFS，把"边界可达的 O"标记为 T
    for (int i = 0; i < m; i++) {
        dfs(board, i, 0);                      // 左边界
        dfs(board, i, n - 1);                  // 右边界
    }
    for (int j = 0; j < n; j++) {
        dfs(board, 0, j);                      // 上边界
        dfs(board, m - 1, j);                  // 下边界
    }

    // ② 统一改值: O（内部被围）→ X；T（边界可达）→ O
    for (int i = 0; i < m; i++) {
        for (int j = 0; j < n; j++) {
            if (board[i][j] == 'O') board[i][j] = 'X';
            else if (board[i][j] == 'T') board[i][j] = 'O';
        }
    }
}

private void dfs(char[][] board, int i, int j) {
    if (i < 0 || i >= board.length || j < 0 || j >= board[0].length) return;
    if (board[i][j] != 'O') return;            // ③ 只处理 O
    board[i][j] = 'T';                         // ④ 标记为"边界可达"
    dfs(board, i + 1, j);
    dfs(board, i - 1, j);
    dfs(board, i, j + 1);
    dfs(board, i, j - 1);
}
// 推演 4×4（经典例）→ 内部 O 变 X，边界连通 O 保留 ✓
// ⚠️ 逆向思维: 正着找"被包围的 O"难（要从内部判断）
//   反着找"不被包围的 O"（边界可达）易 → 标记后取补集
```

> 🎯 **130 的逆向思维**："**'正着难、反着易'是网格题的经典套路——被包围的 O 难找，边界可达的 O 好找（从四条边 DFS 即可）**。标记 T 后'补集'就是答案，一次遍历改值收尾。"

## 5. 79 单词搜索（visited + 撤销）

### 5.1 题目与思路

**题**：网格中是否存在单词路径（相邻四方向，不重复用格）。

**visited + 撤销**（路径类必须恢复标记）：

```java
/**
 * LeetCode 79. 单词搜索
 * visited 标记 + 撤销（路径类题——走过要能回头）
 * 时间 O(mn·4^L)（L = 单词长度），空间 O(mn)
 */
public boolean exist(char[][] board, String word) {
    int m = board.length, n = board[0].length;
    for (int i = 0; i < m; i++) {
        for (int j = 0; j < n; j++) {
            if (dfs(board, word, 0, i, j)) return true;   // ① 每个格子当起点试
        }
    }
    return false;
}

private boolean dfs(char[][] board, String word, int index, int i, int j) {
    if (index == word.length()) return true;   // ② 单词全部匹配
    if (i < 0 || i >= board.length || j < 0 || j >= board[0].length) return false;
    if (board[i][j] != word.charAt(index)) return false;   // ③ 字符不匹配

    char tmp = board[i][j];                    // ④ 保存原值
    board[i][j] = '#';                         // ⑤ 标记（防同格重复用）
    boolean found = dfs(board, word, index + 1, i + 1, j)
                 || dfs(board, word, index + 1, i - 1, j)
                 || dfs(board, word, index + 1, i, j + 1)
                 || dfs(board, word, index + 1, i, j - 1);
    board[i][j] = tmp;                         // ⑥ ⚠️ 撤销标记（路径类必须！）
    return found;
}
// 推演 board=[["A","B","C","E"],["S","F","C","S"],["A","D","E","E"]], word="ABCCED" → true ✓
// ⚠️ 与岛屿题的区别: 岛屿"沉岛"不撤销（整岛一次用完）；
//   单词搜索要"撤销"（同一格可能被不同路径尝试）
```

> 💡 **沉岛 vs 撤销**："**岛屿题沉岛不撤销（每格归属确定）、搜索题撤销（每格可被多路径尝试）**——'要不要撤销'由'格子是否可能被再次使用'决定。这是网格 DFS 的最高频区分点。"

## 6. 岛屿系列变体

| 题 | 变形 | 与 200 的关系 |
|:---:|------|------|
| 200 岛屿数量 | 母题 | 沉岛计数 |
| 695 最大面积 | 面积 | 返回值型 |
| 130 被围绕区域 | 边界逆向 | 边界 DFS + 补集 |
| 463 岛屿周长 | 周长 | 每格统计边界数 |
| 827 最大人工岛 | 改一格 | 岛屿编号 + 哈希 |
| 1254 封闭岛屿 | 边界排除 | 130 + 200 组合 |
| 694 不同形状岛屿 | 形状去重 | 路径编码 + HashSet |
| 1020 飞地数量 | 边界排除 | 130 思路 |

```text
⚠️ 变体识别:
  "数量/计数" → 200 模板
  "最大/面积" → 695 模板（返回值）
  "边界/围绕/飞地" → 130 模板（逆向）
  "形状/不同" → 路径编码（DFS 记录拐弯序列）
```

## 7. 网格 DFS 易错点

| # | 易错点 | 正确写法 | 后果 |
|:---:|------|------|------|
| 1 | 标记放递归后 | 标记前置 | 死循环/栈溢出 |
| 2 | 越界检查放访问后 | 越界先判 | 数组越界 |
| 3 | 手写四个递归 | 方向数组循环 | 漏方向 |
| 4 | 79 忘撤销 | 恢复 board[i][j] | 路径交叉污染 |
| 5 | 130 只 DFS 一个边界 | 四条边界全 DFS | 漏掉边界连通 O |
| 6 | 695 忘返回值 | 1 + 四方向之和 | 面积恒 1 |
| 7 | 200 用 visited 数组 | 沉岛（原地改值） | 多 O(mn) 空间 |

> 🎯 **核心要点**：网格 DFS 通关四件事——**① 四件套纪律**（越界先判、标记前置、方向数组）；**② 沉岛 vs 撤销**（岛屿沉、搜索撤）；**③ 逆向思维**（130 边界可达 + 补集）；**④ 返回值 vs void**（695 的面积求和）。**网格 DFS 是'矩阵即图'的思想落地——四件套 + 沉岛，岛屿系列 8 题一模板通吃**。

---

**下一模块**：[04-图DFS：环检测与拓扑排序](04-图DFS：环检测与拓扑排序.md)
**返回总览**：[00-DFS知识体系总览](00-DFS知识体系总览.md)
