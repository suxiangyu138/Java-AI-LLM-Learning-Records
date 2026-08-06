# 09 - LeetCode 高频：DP 与贪心

> 🎯 DP 是 LeetCode 最难的题型也是大厂最爱考的——它是区分"会用 API"和"会算法思维"的关键分界线。贪心虽比 DP 简单，但证明贪心正确性往往更难

---

## 目录

1. [DP 高频题](#1-dp-高频题)
2. [贪心高频题](#2-贪心高频题)

---

## 1. DP 高频题

```java
// 最长递增子序列 LIS（耐心排序思维）
public int lengthOfLIS(int[] nums) {
    int[] tails = new int[nums.length];
    int size = 0;
    for (int x : nums) {
        int i = 0, j = size;
        while (i < j) {                    // 二分找左边界
            int mid = (i + j) / 2;
            if (tails[mid] < x) i = mid + 1;
            else j = mid;
        }
        tails[i] = x;
        if (i == size) size++;
    }
    return size;
}

// 编辑距离（二维 DP 经典）
public int minDistance(String word1, String word2) {
    int m = word1.length(), n = word2.length();
    int[][] dp = new int[m + 1][n + 1];
    for (int i = 0; i <= m; i++) dp[i][0] = i;  // 删除
    for (int j = 0; j <= n; j++) dp[0][j] = j;  // 插入

    for (int i = 1; i <= m; i++) {
        for (int j = 1; j <= n; j++) {
            if (word1.charAt(i - 1) == word2.charAt(j - 1)) {
                dp[i][j] = dp[i - 1][j - 1];
            } else {
                dp[i][j] = 1 + Math.min(
                    Math.min(dp[i - 1][j], dp[i][j - 1]),  // 删 / 插
                    dp[i - 1][j - 1]                         // 替换
                );
            }
        }
    }
    return dp[m][n];
}
```

### DP 高频题型

| 题目 | 核心思路 | 难度 |
|------|---------|:---:|
| 爬楼梯/打家劫舍 | 一维 DP，dp[i] = max(dp[i-1], dp[i-2]+nums[i]) | E/M |
| 最长递增子序列 | 耐心排序 + 二分 O(n log n) | M |
| 编辑距离 | 二维 DP, if 相等则对角 else 三种操作 | H |
| 0-1 背包/分割等和子集 | dp[j] = dp[j] \| dp[j-nums[i]] | M |
| 最长公共子序列 | if 相等 dp[i-1][j-1]+1 else max(dp[i-1][j],dp[i][j-1]) | M |
| 正则表达式匹配 | 二维 DP + 处理 `*` 通配符 | H |

## 2. 贪心高频题

```java
// 跳跃游戏 II（最少跳跃次数 = 贪心 + BFS 层数）
public int jump(int[] nums) {
    int jumps = 0, curEnd = 0, curFarthest = 0;
    for (int i = 0; i < nums.length - 1; i++) {
        curFarthest = Math.max(curFarthest, i + nums[i]);
        if (i == curEnd) {         // 到达当前层边界
            jumps++;
            curEnd = curFarthest;  // 进入下一层
        }
    }
    return jumps;
}

// 分发糖果（左右各扫一遍）
public int candy(int[] ratings) {
    int n = ratings.length;
    int[] candies = new int[n];
    Arrays.fill(candies, 1);
    // 左→右：比左边高就多 1 颗
    for (int i = 1; i < n; i++)
        if (ratings[i] > ratings[i - 1]) candies[i] = candies[i - 1] + 1;
    // 右→左：比右边高且当前不够就多给
    for (int i = n - 2; i >= 0; i--)
        if (ratings[i] > ratings[i + 1]) candies[i] = Math.max(candies[i], candies[i + 1] + 1);
    return Arrays.stream(candies).sum();
}
```

## 核心要点回顾

- LIS: 耐心排序（维护递增 tails 数组 + 二分）
- 编辑距离: 二维 DP，dp[i][j] 表示 word1[0:i] ↔ word2[0:j]
- 背包问题: 0-1 背包倒序，完全背包正序
- 贪心证明: 先猜解法 → 举反例 → 过不了就换 DP
- DP vs 贪心: DP 有明确递推公式，贪心需要"证明"

## 参考资料

1. LeetCode DP 精选题单
2. 《算法导论》DP 章节
