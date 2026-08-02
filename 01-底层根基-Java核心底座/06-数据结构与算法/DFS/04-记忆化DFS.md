# 记忆化DFS
> DFS + 缓存 = 记忆化搜索——重叠子问题的优雅解法，DP 的递归形态

## 📚 目录
1. [什么是记忆化 DFS](#1-什么是记忆化-dfs)
2. [62. 不同路径](#2-62-不同路径)
3. [329. 矩阵中的最长递增路径](#3-329-矩阵中的最长递增路径)
4. [记忆化 vs 表格 DP](#4-记忆化-vs-表格-dp)
5. [记忆化 DFS 应用总结](#5-记忆化-dfs-应用总结)

---

## 1. 什么是记忆化 DFS

### 1.1 定义

> 记忆化搜索（Memoization）= DFS + 缓存。递归探索时，把已经计算过的状态结果缓存起来，遇到相同状态直接返回，避免重复计算。

```
朴素 DFS：每次递归都重新计算 → 指数级

fib(5)
├── fib(4)
│   ├── fib(3)
│   │   ├── fib(2) ← 重复计算！
│   │   └── fib(1)
│   └── fib(2) ← 重复！
└── fib(3) ← 重复！

记忆化：第一次算完存入缓存
fib(3) 算一次 → 存起来 → 后面直接取
→ O(n) 而非 O(2ⁿ)
```

### 1.2 为什么有效？

```
记忆化利用"重叠子问题"：
  递归树中存在大量相同状态的重复计算
  缓存后每个状态只算一次

本质：动态规划的递归实现
  自顶向下（递归）+ 缓存
  vs 表格 DP：自底向上（迭代）+ 数组

两个关键：
  ① 状态可哈希/可索引（坐标/参数）
  ② 存在重叠子问题（否则缓存无意义）
```

---

## 2. 62. 不同路径

### 2.1 朴素 DFS（会超时）

```java
/**
 * LeetCode 62. 不同路径
 * m×n 网格，从左上到右下，只能向右/下
 * 求不同路径数
 *
 * 朴素 DFS：f(i,j) = f(i+1,j) + f(i,j+1)
 * 问题：大量重叠子问题 → O(2^(m+n)) 超时
 */
public int uniquePaths_Naive(int m, int n) {
    return dfs(m, n, 0, 0);
}
private int dfs(int m, int n, int i, int j) {
    if (i == m - 1 && j == n - 1) return 1;   // 到达终点
    if (i >= m || j >= n) return 0;           // 越界
    return dfs(m, n, i + 1, j) + dfs(m, n, i, j + 1);
}
```

### 2.2 记忆化 DFS（O(mn)）

```java
/**
 * 记忆化：memo[i][j] 缓存"从 (i,j) 出发的路径数"
 *   -1 = 未计算
 */
public int uniquePaths(int m, int n) {
    int[][] memo = new int[m][n];
    for (int[] row : memo) Arrays.fill(row, -1);   // 未计算标记
    return dfs(m, n, 0, 0, memo);
}

private int dfs(int m, int n, int i, int j, int[][] memo) {
    if (i == m - 1 && j == n - 1) return 1;
    if (i >= m || j >= n) return 0;

    if (memo[i][j] != -1) return memo[i][j];      // ⚠️ 查缓存！

    int paths = dfs(m, n, i + 1, j, memo)
              + dfs(m, n, i, j + 1, memo);
    return memo[i][j] = paths;                    // 存缓存
}
// 每个格子算一次 → O(mn)
// 也可以直接用 DP：dp[i][j] = dp[i-1][j] + dp[i][j-1]
```

### 2.3 记忆化三步模板

```java
// ① 初始化缓存（-1 = 未计算）
// ② 递归入口：查缓存 → 命中直接返回
// ③ 计算结果 → 存入缓存 → 返回

private int dfs(状态参数, int[][] memo) {
    if (基础情况) return 基础值;
    if (memo[i][j] != -1) return memo[i][j];   // 查
    int result = 递归计算;
    return memo[i][j] = result;                // 存
}
```

---

## 3. 329. 矩阵中的最长递增路径

### 3.1 完整实现（Hard 记忆化经典）

```java
/**
 * LeetCode 329. 矩阵中的最长递增路径（Hard 经典）
 * 在矩阵中找最长递增路径（四方向移动，只能走更大的）
 *
 * 朴素 DFS：从每个格子出发 DFS → O(mn × 4^len) 超时
 * 记忆化：memo[i][j] 缓存"从 (i,j) 出发的最长路径"
 *   → 每个格子只算一次 → O(mn)
 *
 * ⚠️ 为什么不需要 visited？
 *   递增条件保证无环（不可能走回更小的格子）
 *   → 天然不会重复访问 → 无需 visited！
 */
public int longestIncreasingPath(int[][] matrix) {
    int m = matrix.length, n = matrix[0].length;
    int[][] memo = new int[m][n];        // 0 = 未计算

    int maxLen = 0;
    for (int i = 0; i < m; i++) {
        for (int j = 0; j < n; j++) {
            maxLen = Math.max(maxLen, dfs(matrix, i, j, memo));
        }
    }
    return maxLen;
}

private int dfs(int[][] matrix, int i, int j, int[][] memo) {
    int m = matrix.length, n = matrix[0].length;
    if (memo[i][j] != 0) return memo[i][j];     // 查缓存

    int[][] dirs = {{0,1},{0,-1},{1,0},{-1,0}};
    int best = 1;                               // 自己至少 1

    for (int[] d : dirs) {
        int ni = i + d[0], nj = j + d[1];
        // 越界 / 不是递增 → 跳过
        if (ni < 0 || ni >= m || nj < 0 || nj >= n) continue;
        if (matrix[ni][nj] <= matrix[i][j]) continue;

        best = Math.max(best, 1 + dfs(matrix, ni, nj, memo));
    }

    return memo[i][j] = best;                   // 存缓存
}
```

### 3.2 为什么递增路径不需要 visited？

```
普通网格 DFS 需要 visited：
  可能走回已访问的格子（如 79 单词搜索）

递增路径天然无环：
  只能走到更大的值 → 永远走不回已访问的格子
  → 不需要 visited，也不会死循环

关键观察（面试必讲）：
"因为路径严格递增，不可能形成环，
 所以不需要 visited，配合记忆化每个格子
 只计算一次 → O(mn)。"
```

---

## 4. 记忆化 vs 表格 DP

### 4.1 两种实现对比

| 维度 | 记忆化 DFS | 表格 DP |
|------|:---:|:---:|
| 方向 | 自顶向下（递归） | 自底向上（迭代） |
| 计算范围 | 只算需要的状态 | 可能算全部状态 |
| 代码 | 递归 + 缓存 | 循环 + 数组 |
| 依赖顺序 | 无需显式指定 | 必须确定遍历顺序 |
| 栈溢出 | 深度大时可能 | 无风险 |
| 面试选择 | 快速验证思路 | 最终实现更稳 |

### 4.2 什么时候选记忆化？

```
✅ 记忆化更合适：
  状态空间稀疏（只算用到的）
  遍历顺序难以确定（329 递增路径）
  树形/递归结构天然（树 DP）

✅ 表格 DP 更合适：
  所有状态都需要（二维表格）
  需要滚动数组优化空间
  状态转移顺序明确

记忆化 = 递归 + 缓存（动态规划的另一种写法）
面试说："这道题我写记忆化搜索——DFS + memo 缓存，
 和 DP 本质等价，只是自顶向下实现。"
```

---

## 5. 记忆化 DFS 应用总结

### 5.1 经典题单

| 题号 | 题目 | 记忆化作用 | 复杂度 |
|:---:|------|------|:---:|
| 62 | 不同路径 | 缓存路径数 | O(mn) |
| 329 | 最长递增路径 | 缓存最长路径 | O(mn) |
| 576 | 出界的路径数 | 缓存 (i,j,步数) | O(mn×k) |
| 140 | 单词拆分 II | 缓存后缀结果 | O(2ⁿ) 结果多 |
| 403 | 青蛙过河 | 缓存 (位置,跳跃) | O(n²) |
| 1155 | 掷骰子方法数 | 缓存 (和,骰子) | O(n×k) |
| 871 | 最低加油次数 | 缓存 (位置,油量) | — |

### 5.2 状态设计方法

```
记忆化状态设计 = 递归参数
  坐标 → memo[i][j]
  坐标+属性 → memo[i][j][k]（576 出界：位置+剩余步数）
  位置+状态 → 打包/位掩码

自检问题：
  ① 哪些参数决定"剩余子问题"？→ 状态
  ② 状态会被重复计算吗？→ 需要缓存吗？
  ③ 缓存数组多大？→ 状态空间大小
```

### 5.3 面试话术

```
"这题我用记忆化 DFS：
 状态是 (i, j)，memo[i][j] 缓存从该位置出发的结果。
 递归里先查缓存（命中直接返回），
 计算完存入缓存。
 因为【递增/无环】所以不需要 visited。
 每个状态只算一次 → O(mn)。
 这本质是动态规划的自顶向下实现。"
```

> 🎯 **核心要点**：记忆化 DFS = **递归 + 缓存**——三步模板（初始化缓存/查缓存/存缓存）。62 路径计数、329 递增路径是两大经典；329 的"递增无环不需要 visited"是面试必讲点。与表格 DP 的关系（自顶向下 vs 自底向上）要说清楚，记忆化是"验证思路最快"的 DP 写法。

---

**下一模块**：[07-Java实现与面试实战](07-Java实现与面试实战.md) | **返回总览**：[00-DFS知识体系总览](00-DFS知识体系总览.md)
