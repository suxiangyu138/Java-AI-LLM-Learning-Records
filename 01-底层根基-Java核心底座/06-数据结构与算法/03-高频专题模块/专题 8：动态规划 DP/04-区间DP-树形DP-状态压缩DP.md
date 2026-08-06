# 区间DP·树形DP·状态压缩DP
> 冲击动态规划的"深水区"——三大 Hard 题模型深度拆解，从区间合并到旅行商问题

## 📚 目录
1. [区间 DP：从小区间合并到大区间](#1-区间-dp从小区间合并到大区间)
2. [树形 DP：在树上做决策](#2-树形-dp在树上做决策)
3. [状态压缩 DP：用位运算表示集合](#3-状态压缩-dp用位运算表示集合)
4. [数位 DP：按位统计的艺术](#4-数位-dp按位统计的艺术)
5. [DP 的其他优化技巧](#5-dp-的其他优化技巧)

---

## 1. 区间 DP：从小区间合并到大区间

### 1.1 核心模板

> 区间 DP 的通用模式：`dp[i][j]` 表示区间 `[i, j]` 上的最优解。通过**枚举分割点 k**，将 `[i, j]` 拆分为 `[i, k]` + `[k+1, j]` 合并求解。

```java
/**
 * 区间 DP 通用框架
 * 要点：
 *   1. 先枚举区间长度 len（从小到大）
 *   2. 再枚举区间起点 i
 *   3. 再枚举分割点 k
 *   时间复杂度通常为 O(n³)
 */
public int intervalDP(int[] arr) {
    int n = arr.length;
    int[][] dp = new int[n][n];

    // 方式 A：按长度递增枚举
    for (int len = 2; len <= n; len++) {           // 区间长度
        for (int i = 0; i + len - 1 < n; i++) {    // 区间起点
            int j = i + len - 1;                   // 区间终点
            // 初始化 dp[i][j] (根据具体问题)
            for (int k = i; k < j; k++) {           // 枚举分割点
                dp[i][j] = f(dp[i][j], dp[i][k], dp[k + 1][j]);
            }
        }
    }

    // 方式 B：逆序枚举 i，正序枚举 j
    for (int i = n - 1; i >= 0; i--) {
        for (int j = i + 1; j < n; j++) {
            for (int k = i; k < j; k++) {
                dp[i][j] = f(dp[i][j], dp[i][k], dp[k + 1][j]);
            }
        }
    }
    return dp[0][n - 1];
}
```

> ⚠️ **注意**：方式 B 中的 `dp[i][k]` 和 `dp[k+1][j]` 必须确保 `[i,k]` 和 `[k+1,j]` 都已经计算过了。逆序 i（从大到小）+ 正序 j（从小到大）天然保证这一点。

### 1.2 矩阵链乘法 (Matrix Chain Multiplication)

```java
/**
 * 问题：给定矩阵维度数组 dims[]，求最优加括号方式使乘法次数最少
 * 若 A(10×30), B(30×5), C(5×60)，dims = [10,30,5,60]
 * dp[i][j] = 计算矩阵 i 到 j 的最小代价
 */
public int matrixChainOrder(int[] dims) {
    int n = dims.length - 1;  // 矩阵个数
    int[][] dp = new int[n][n];

    for (int len = 2; len <= n; len++) {
        for (int i = 0; i + len - 1 < n; i++) {
            int j = i + len - 1;
            dp[i][j] = Integer.MAX_VALUE;
            for (int k = i; k < j; k++) {
                int cost = dp[i][k] + dp[k + 1][j]
                         + dims[i] * dims[k + 1] * dims[j + 1];
                dp[i][j] = Math.min(dp[i][j], cost);
            }
        }
    }
    return dp[0][n - 1];
}
```

### 1.3 戳气球 (312. Burst Balloons) ⭐

```java
/**
 * 核心技巧：逆向思维——与其考虑"戳爆谁"，不如考虑"谁最后被戳"
 * dp[i][j] = 戳破 (i,j) 开区间内所有气球的最大硬币数
 *
 * 设 k 为 (i,j) 中最后一个被戳破的气球：
 * dp[i][j] = max_{k∈(i,j)} (dp[i][k] + dp[k][j] + nums[i]*nums[k]*nums[j])
 */
public int maxCoins(int[] nums) {
    int n = nums.length;
    int[] arr = new int[n + 2];
    arr[0] = arr[n + 1] = 1;                // 边界添加虚拟气球
    System.arraycopy(nums, 0, arr, 1, n);

    int[][] dp = new int[n + 2][n + 2];
    for (int len = 3; len <= n + 2; len++) {          // 区间至少包含 3 个位置
        for (int i = 0; i + len - 1 < n + 2; i++) {
            int j = i + len - 1;
            for (int k = i + 1; k < j; k++) {
                dp[i][j] = Math.max(dp[i][j],
                    dp[i][k] + dp[k][j] + arr[i] * arr[k] * arr[j]);
            }
        }
    }
    return dp[0][n + 1];
}
```

### 1.4 区间 DP 面试高频题

| 题号 | 题目 | 核心思路 | 难度 |
|:---:|------|------|:---:|
| 312 | 戳气球 | 逆向思维："最后被戳" → 开区间合并 | 🔴 |
| 5 | 最长回文子串 | `dp[i][j]` = s[i]==s[j] && dp[i+1][j-1] | 🟡 |
| 516 | 最长回文子序列 | 比较包含两端或不包含 | 🟡 |
| 1000 | 合并石头的最低成本 | 石子合并变体，k 堆合并为 1 堆 | 🔴 |
| 87 | 扰乱字符串 | 递归+记忆化 / 区间 DP | 🔴 |
| 664 | 奇怪的打印机 | 类似戳气球，区间 DP + 贪心剪枝 | 🔴 |

---

## 2. 树形 DP：在树上做决策

### 2.1 核心模板：后序遍历 + 状态合并

```java
/**
 * 树形 DP 三步走：
 *   1. 递归遍历（通常是后序 post-order）
 *   2. 在叶子节点初始化 base case
 *   3. 在非叶子节点合并所有子节点的 dp 结果
 */

// 通用框架：二叉树的树形 DP
class TreeNode {
    int val;
    TreeNode left, right;
}

// 后序遍历框架
public ResultType dfs(TreeNode root) {
    if (root == null) return baseResult;

    ResultType left  = dfs(root.left);
    ResultType right = dfs(root.right);

    // 合并 left 和 right，结合 root.val 计算当前结果
    return merge(left, right, root);
}
```

### 2.2 四大树形 DP 模型

#### 模型 1：打家劫舍 III (337) — 选/不选

```java
// dp[node][0] = 不偷 node 时的最大收益
// dp[node][1] = 偷 node 时的最大收益
public int rob(TreeNode root) {
    int[] res = dfs(root);
    return Math.max(res[0], res[1]);
}

private int[] dfs(TreeNode node) {
    if (node == null) return new int[]{0, 0};

    int[] left  = dfs(node.left);
    int[] right = dfs(node.right);

    int[] cur = new int[2];
    cur[0] = Math.max(left[0], left[1]) + Math.max(right[0], right[1]); // 不偷：子节点可偷可不偷
    cur[1] = node.val + left[0] + right[0];  // 偷：子节点绝对不能偷
    return cur;
}
```

#### 模型 2：二叉树最大路径和 (124) — 全局最优

```java
// 经典 Hard 题：路径可以穿过根节点也可以不穿过
// 后序遍历返回"以 node 为一端的最大单边路径和"
// 全局答案 = 左单边 + node + 右单边
public int maxPathSum(TreeNode root) {
    int[] globalMax = new int[]{Integer.MIN_VALUE};
    maxGain(root, globalMax);
    return globalMax[0];
}

private int maxGain(TreeNode node, int[] globalMax) {
    if (node == null) return 0;
    // 负数贡献直接舍弃（和 0 取 max）
    int leftGain  = Math.max(0, maxGain(node.left, globalMax));
    int rightGain = Math.max(0, maxGain(node.right, globalMax));

    // 全局路径：可以同时走左右
    globalMax[0] = Math.max(globalMax[0], leftGain + node.val + rightGain);

    // 返回单边最大贡献（给父节点用）
    return node.val + Math.max(leftGain, rightGain);
}
```

#### 模型 3：树的直径 (543) — 边数/节点数

```java
// 直径 = 任意两节点间最长路径的边数
// 后序遍历维护高度，直径 = 左高 + 右高（或不穿过当前节点）
public int diameterOfBinaryTree(TreeNode root) {
    int[] diameter = new int[1];
    height(root, diameter);
    return diameter[0];
}
private int height(TreeNode node, int[] diameter) {
    if (node == null) return 0;
    int lh = height(node.left, diameter);
    int rh = height(node.right, diameter);
    diameter[0] = Math.max(diameter[0], lh + rh);  // 路径 = 左高 + 右高
    return 1 + Math.max(lh, rh);                    // 节点高度
}
```

#### 模型 4：树的最小点覆盖 (968) — 三状态

```java
// 968. 监控二叉树：每个节点的状态
//   0: 未被覆盖（需要父节点来覆盖）
//   1: 已安装摄像头
//   2: 已被覆盖（被子节点的摄像头覆盖）
public int minCameraCover(TreeNode root) {
    int[] res = dfs(root);
    return Math.min(res[1], res[2]); // 根不需要父节点覆盖
}
private int[] dfs(TreeNode node) {
    if (node == null) return new int[]{0, 0, 99999}; // 空节点不需要被覆盖
    int[] L = dfs(node.left), R = dfs(node.right);
    int[] cur = new int[3];
    cur[0] = L[2] + R[2];                                         // 0: 依赖父节点
    cur[1] = 1 + min3(L) + min3(R);                               // 1: 装摄像头
    cur[2] = Math.min(L[1] + Math.min(R[1], R[2]),
                      R[1] + Math.min(L[1], L[2]));               // 2: 被子节点覆盖
    return cur;
}
```

---

## 3. 状态压缩 DP：用位运算表示集合

### 3.1 位运算基础速查

```java
// 常用位运算操作
mask & (1 << i)          // 检查第 i 位是否为 1
mask | (1 << i)          // 将第 i 位置为 1
mask ^ (1 << i)          // 翻转第 i 位
mask & (mask - 1)        // 去掉最低位的 1
Integer.bitCount(mask)   // 统计 1 的个数
Integer.lowestOneBit(mask) // 获取最低位的 1

// 枚举 mask 的所有子集
for (int sub = mask; sub > 0; sub = (sub - 1) & mask) { ... }
```

### 3.2 旅行商问题 (TSP) — 状态压缩 DP 的经典

```java
/**
 * 旅行商问题：从起点 0 出发，访问所有城市恰好一次后返回 0 的最短路径
 *
 * dp[mask][i] = 当前已访问城市集合为 mask、最后停在城市 i 的最短距离
 * 转移：dp[mask|(1<<j)][j] = min(dp[mask|(1<<j)][j], dp[mask][i] + dist[i][j])
 *
 * 时间复杂度: O(n² × 2ⁿ)  空间: O(n × 2ⁿ)
 */
public int tsp(int[][] dist) {
    int n = dist.length;
    int[][] dp = new int[1 << n][n];
    for (int[] row : dp) Arrays.fill(row, Integer.MAX_VALUE / 2);
    dp[1][0] = 0;  // 从城市 0 出发，只访问了城市 0（mask=1，最低位为 1）

    for (int mask = 1; mask < (1 << n); mask++) {
        for (int i = 0; i < n; i++) {
            if ((mask & (1 << i)) == 0) continue; // i 不在当前集合中
            if (dp[mask][i] >= Integer.MAX_VALUE / 2) continue;

            for (int j = 0; j < n; j++) {
                if ((mask & (1 << j)) != 0) continue; // j 已经访问过了
                int nextMask = mask | (1 << j);
                dp[nextMask][j] = Math.min(dp[nextMask][j],
                    dp[mask][i] + dist[i][j]);
            }
        }
    }

    // 回到起点
    int fullMask = (1 << n) - 1;
    int ans = Integer.MAX_VALUE;
    for (int i = 1; i < n; i++) {
        ans = Math.min(ans, dp[fullMask][i] + dist[i][0]);
    }
    return ans;
}
```

### 3.3 状态压缩 DP 面试题

| 题号 | 题目 | 状态含义 | 技巧 |
|:---:|------|------|------|
| — | TSP 旅行商 | `dp[mask][i]` 当前集合停在 i | 经典模板 |
| 526 | 优美的排列 | `dp[mask]` 已用数字集合 | 枚举最后一个放谁 |
| 698 | 划分为k个相等的子集 | `dp[mask]` 已选元素 | 状态压缩 + 剪枝 |
| 1349 | 参加考试的最大学生数 | `dp[row][mask]` 每行座位状态 | 二进制枚举行 |
| 1655 | 分配重复整数 | `dp[mask]` 已满足的顾客 | 子集枚举 |

---

## 4. 数位 DP：按位统计的艺术

```java
/**
 * 数位 DP 通用模板：统计 [0, n] 中满足某条件的数的个数
 * 核心：按位 DFS + 记忆化，状态为 (pos, tight, ...)
 *
 * pos:   当前处理到第几位（从高位到低位）
 * tight: 是否受上界限制 (true → 当前位不能超过 n 的对应位)
 * isLeadingZero: 是否有前导零
 */
public class DigitDP {
    char[] digits;      // n 的字符表示
    Integer[][][] memo; // 记忆化数组

    public int count(int n) {
        digits = String.valueOf(n).toCharArray();
        int len = digits.length;
        memo = new Integer[len][2][2];  // 维度取决于具体问题
        return dfs(0, true, true);       // 从最高位开始，初始受限且有前导零
    }

    private int dfs(int pos, boolean tight, boolean leadingZero) {
        if (pos == digits.length) return leadingZero ? 0 : 1; // 处理完所有位

        int t = tight ? 1 : 0, lz = leadingZero ? 1 : 0;
        if (memo[pos][t][lz] != null) return memo[pos][t][lz];

        int limit = tight ? digits[pos] - '0' : 9;
        int total = 0;
        for (int d = 0; d <= limit; d++) {
            total += dfs(pos + 1,
                tight && (d == limit),              // 下一位是否受限
                leadingZero && (d == 0));            // 下一位是否前导零
        }
        return memo[pos][t][lz] = total;
    }
}

// 实际示例：统计 [1,n] 中数字 1 出现的次数 (LeetCode 233)
// 需要在 DFS 中额外维护一个 count 状态：当前已出现几个 1
```

> 💡 数位 DP 的题目非常固定——一旦掌握了模板，几乎所有同类题都是换转移条件。

---

## 5. DP 的其他优化技巧

### 5.1 优化技巧速查

| 技巧 | 适用场景 | 效果 | 示例 |
|------|------|------|------|
| **单调队列优化** | `dp[i] = max_{i-k≤j<i} (dp[j]) + cost[i]` | O(n²)→O(n) | 239.滑动窗口最大值 |
| **斜率优化** | `dp[i] = min_{j<i} (a[j]×x[i] + b[j])` | O(n²)→O(n) | 装箱问题 |
| **四边形不等式** | 区间 DP 的分割点单调 | O(n³)→O(n²) | 石子合并优化 |
| **二分优化** | LIS 的 tails 数组 | O(n²)→O(n log n) | 300.LIS |
| **WQS 二分** | 带限制的最大化问题 | 去掉一维 | 买卖股票 IV 优化 |

### 5.2 单调队列优化示例

```java
// 经典场景：dp[i] = max(dp[j]) + nums[i], 其中 i-k ≤ j < i
// 用双端队列维护一个单调递减的窗口
Deque<Integer> deque = new ArrayDeque<>();
for (int i = 0; i < n; i++) {
    // 1. 移除窗口外的元素
    while (!deque.isEmpty() && deque.peekFirst() < i - k) deque.pollFirst();
    // 2. dp[i] = dp[最佳j] + nums[i]
    dp[i] = (deque.isEmpty() ? 0 : dp[deque.peekFirst()]) + nums[i];
    // 3. 维护单调递减：从队尾移除比 dp[i] 小的元素
    while (!deque.isEmpty() && dp[deque.peekLast()] <= dp[i]) deque.pollLast();
    deque.offerLast(i);
}
```

> 🎯 **核心要点**：区间 DP 记得按长度递增或逆序 i 正序 j 枚举；树形 DP 核心是后序遍历 + 状态合并；状态压缩 DP 掌握了位运算基础后就是枚举集合；数位 DP 背下模板，所有题只是在 DFS 转移条件上做文章。这四大类一旦贯通，LeetCode 上 95% 的 DP Hard 题都能找到突破口。

---

**下一模块**：[07-Java实现与面试实战](05-Java实现与面试实战.md) | **返回总览**：[00-动态规划知识体系总览](00-动态规划知识体系总览.md)
