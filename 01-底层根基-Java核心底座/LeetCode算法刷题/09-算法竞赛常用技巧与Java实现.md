# 09 算法竞赛常用技巧与 Java 实现

> 不是要打竞赛，而是用竞赛技巧降维打击面试——掌握位运算、前缀和、差分、快速幂这些"黑科技"，面试中直接输出最优解

---

## 📚 目录

1. [位运算技巧大全](#1-位运算技巧大全)
2. [前缀和+差分技巧](#2-前缀和差分技巧)
3. [二分查找变体](#3-二分查找变体)
4. [快速幂与快速乘](#4-快速幂与快速乘)
5. [字符串算法](#5-字符串算法)
6. [并查集 Union-Find](#6-并查集-union-find)
7. [线段树/树状数组入门](#7-线段树树状数组入门)
8. [单调队列](#8-单调队列)
9. [记忆化搜索](#9-记忆化搜索)
10. [随机算法](#10-随机算法)

---

## 1. 位运算技巧大全

### 1.1 常用位运算操作速查

| 操作 | 表达式 | 说明 |
|------|--------|------|
| 取最低位 | `x & 1` | 判断奇偶，结果为 1 则为奇数 |
| 右移取半 | `x >> 1` | 等价于 `x / 2`，速度更快 |
| 消除最低位 1 | `x & (x - 1)` | 将二进制最低位的 1 变为 0 |
| 取最低位 1 | `x & -x` | 获取最低位 1 所在位置的值 |
| 异或交换 | `a ^= b; b ^= a; a ^= b` | 不使用临时变量交换两数 |
| 取反 | `~x` | 按位取反 |
| 绝对值 | `(x ^ (x >> 31)) - (x >> 31)` | 整数的位运算绝对值 |
| 判断 2 的幂 | `x > 0 && (x & (x - 1)) == 0` | 2 的幂二进制只有一个 1 |
| 判断符号 | `(x ^ y) >= 0` | true 表示同号 |
| 模 2 的幂 | `x & (mod - 1)` | 当 mod = 2^n 时使用 |

### 1.2 LC 136 只出现一次的数字

> 数组中除了一个数字出现一次，其余出现两次——全员异或即可。

```java
class Solution {
    public int singleNumber(int[] nums) {
        int result = 0;
        for (int num : nums) {
            result ^= num; // a ^ a = 0, a ^ 0 = a
        }
        return result;
    }
}
```

| 维度 | 数据 |
|------|------|
| 时间复杂度 | O(n) |
| 空间复杂度 | O(1) |
| 进阶 | LC 260 找两个只出现一次的数（分组异或） |

---

### 1.3 LC 191 位 1 的个数

```java
class Solution {
    // 方法一：逐位检查
    public int hammingWeight(int n) {
        int count = 0;
        while (n != 0) {
            n &= (n - 1); // 每次消除一个 1
            count++;
        }
        return count;
    }
}
```

| 维度 | 数据 |
|------|------|
| 时间复杂度 | O(k)，k 为 1 的个数（最优 O(1)） |
| 空间复杂度 | O(1) |
| 技巧 | `n & (n - 1)` 是最常用的清最低位 1 操作 |

---

### 1.4 LC 231 2 的幂

```java
class Solution {
    public boolean isPowerOfTwo(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }
}
```

| 维度 | 数据 |
|------|------|
| 时间复杂度 | O(1) |
| 空间复杂度 | O(1) |
| 扩展 | LC 326 3 的幂，LC 342 4 的幂 |

---

### 1.5 LC 338 比特位计数

> 动态规划 + 位运算组合。

```java
class Solution {
    public int[] countBits(int n) {
        int[] dp = new int[n + 1];
        for (int i = 1; i <= n; i++) {
            // i >> 1: 右移一位相当于去掉最低位
            // i & 1: 最低位是否为 1
            dp[i] = dp[i >> 1] + (i & 1);
        }
        return dp;
    }
}
```

| 维度 | 数据 |
|------|------|
| 时间复杂度 | O(n) |
| 空间复杂度 | O(n) |

---

### 1.6 LC 371 两整数之和（不用加减）

```java
class Solution {
    public int getSum(int a, int b) {
        while (b != 0) {
            int carry = (a & b) << 1; // 进位
            a = a ^ b;                // 无进位加法
            b = carry;                // 进位传递
        }
        return a;
    }
}
```

| 维度 | 数据 |
|------|------|
| 时间复杂度 | O(log C)，C 为整数范围 |
| 空间复杂度 | O(1) |
| 面试官追问 | 如何实现减法 `getSubtract(a, b)`？→ `getSum(a, ~b + 1)`（补码） |

> 🎯 **核心要点**：位运算不是炫技而是必须——某些场景（如设计 BitMap、状态压缩 DP、内存受限环境）位运算就是唯一解。

---

## 2. 前缀和+差分技巧

### 2.1 一维前缀和模板

> 快速求 `[l, r]` 区间和，预处理 O(n)，查询 O(1)。

```java
class PrefixSum1D {
    private final int[] prefix;

    /**
     * @param nums 原始数组（0-indexed）
     */
    public PrefixSum1D(int[] nums) {
        int n = nums.length;
        prefix = new int[n + 1]; // prefix[i] 表示 nums[0..i-1] 的和
        for (int i = 0; i < n; i++) {
            prefix[i + 1] = prefix[i] + nums[i];
        }
    }

    /**
     * 查询闭区间 [l, r] 的和（0-indexed）
     */
    public int rangeSum(int l, int r) {
        return prefix[r + 1] - prefix[l];
    }
}
```

> 💡 `prefix[i + 1] = prefix[i] + nums[i]` 这一写法将 `prefix[0]` 初始化为 0，避免了对 l=0 的特殊处理。

---

### 2.2 二维前缀和模板（LC 304 二维区域和检索）

```java
class NumMatrix {
    private final int[][] prefix;

    public NumMatrix(int[][] matrix) {
        int m = matrix.length, n = matrix[0].length;
        prefix = new int[m + 1][n + 1];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                // 当前前缀和 = 上方 + 左方 - 左上重叠 + 当前值
                prefix[i + 1][j + 1] = prefix[i][j + 1] + prefix[i + 1][j]
                                     - prefix[i][j] + matrix[i][j];
            }
        }
    }

    /**
     * 查询 (row1, col1) 到 (row2, col2) 矩形区域的和
     */
    public int sumRegion(int row1, int col1, int row2, int col2) {
        return prefix[row2 + 1][col2 + 1] - prefix[row1][col2 + 1]
             - prefix[row2 + 1][col1] + prefix[row1][col1];
    }
}
```

| 维度 | 数据 |
|------|------|
| 预处理 | O(m × n) |
| 查询 | O(1) |
| 空间 | O(m × n) |

---

### 2.3 差分数组模板

> 差分数组是前缀和的逆运算——用于"频繁对区间做同一加减操作，最后统一查询"的场景。

```java
class Difference {
    private final int[] diff;

    /**
     * @param nums 原始数组
     */
    public Difference(int[] nums) {
        int n = nums.length;
        diff = new int[n];
        diff[0] = nums[0];
        for (int i = 1; i < n; i++) {
            diff[i] = nums[i] - nums[i - 1];
        }
    }

    /**
     * 对闭区间 [l, r] 增加 val
     */
    public void increment(int l, int r, int val) {
        diff[l] += val;
        if (r + 1 < diff.length) {
            diff[r + 1] -= val;
        }
    }

    /**
     * 应用所有增量，返回最终数组
     */
    public int[] getResult() {
        int n = diff.length;
        int[] result = new int[n];
        result[0] = diff[0];
        for (int i = 1; i < n; i++) {
            result[i] = result[i - 1] + diff[i];
        }
        return result;
    }
}
```

> ⚠️ 差分数组适用于：区间修改次数多、查询次数少的场景。如果边修改边查询，请考虑线段树或树状数组。

---

### 2.4 LC 1109 航班预订统计

> 差分数组经典应用——航班预订数 = 每个航班上预定的座位总数。

```java
class Solution {
    public int[] corpFlightBookings(int[][] bookings, int n) {
        int[] diff = new int[n];

        for (int[] booking : bookings) {
            int first = booking[0] - 1;  // 转为 0-indexed
            int last = booking[1] - 1;
            int seats = booking[2];
            diff[first] += seats;
            if (last + 1 < n) {
                diff[last + 1] -= seats;
            }
        }

        // 前缀和还原
        int[] result = new int[n];
        result[0] = diff[0];
        for (int i = 1; i < n; i++) {
            result[i] = result[i - 1] + diff[i];
        }
        return result;
    }
}
```

| 维度 | 数据 |
|------|------|
| 时间复杂度 | O(n + m)，m 为预订记录数 |
| 空间复杂度 | O(n) |
| 暴力法对比 | 暴力 O(m × n)，差分 O(n + m) |

---

### 2.5 LC 1094 拼车

```java
class Solution {
    public boolean carPooling(int[][] trips, int capacity) {
        int maxLocation = 0;
        for (int[] trip : trips) {
            maxLocation = Math.max(maxLocation, trip[2]);
        }

        int[] diff = new int[maxLocation + 1];

        for (int[] trip : trips) {
            int numPassengers = trip[0];
            int from = trip[1];
            int to = trip[2];
            diff[from] += numPassengers;
            if (to < diff.length) {
                diff[to] -= numPassengers; // 在 to 处下车，所以 to 位置减
            }
        }

        int current = 0;
        for (int i = 0; i < diff.length; i++) {
            current += diff[i];
            if (current > capacity) return false;
        }
        return true;
    }
}
```

| 维度 | 数据 |
|------|------|
| 时间复杂度 | O(n + L)，L 为最大位置 |
| 空间复杂度 | O(L) |
| 公司标签 | Uber、美团、字节跳动 |

> 🎯 **前缀和 vs 差分速查**：前缀和 = "静态区间求和"；差分 = "动态区间修改"。两者互逆——对差分数组求前缀和得到原数组，对前缀和数组求差分得到原数组。

---

## 3. 二分查找变体

### 3.1 六种二分查找模板速查

假设数组 `nums` 为升序排列。

**模板一：精确查找**

```java
int binarySearch(int[] nums, int target) {
    int left = 0, right = nums.length - 1;
    while (left <= right) {
        int mid = left + (right - left) / 2; // 防止溢出
        if (nums[mid] == target) return mid;
        else if (nums[mid] < target) left = mid + 1;
        else right = mid - 1;
    }
    return -1;
}
```

**模板二：查找左边界（第一个 >= target）**

```java
int findLeftBound(int[] nums, int target) {
    int left = 0, right = nums.length; // 注意：右边界开区间
    while (left < right) {
        int mid = left + (right - left) / 2;
        if (nums[mid] >= target) {
            right = mid;
        } else {
            left = mid + 1;
        }
    }
    return left; // left 是第一个 >= target 的下标
}
```

**模板三：查找右边界（最后一个 <= target）**

```java
int findRightBound(int[] nums, int target) {
    int left = 0, right = nums.length;
    while (left < right) {
        int mid = left + (right - left) / 2;
        if (nums[mid] <= target) {
            left = mid + 1; // 左指针右移
        } else {
            right = mid;
        }
    }
    return left - 1; // left-1 是最后一个 <= target 的下标
}
```

**模板四：查找第一个 >= target**

同模板二——`findLeftBound` 即为第一个 ≥target 的位置。

**模板五：查找最后一个 <= target**

同模板三——`findRightBound` 即为最后一个 ≤target 的位置。

**模板六：旋转数组查 target（LC 33）**

```java
int searchRotated(int[] nums, int target) {
    int left = 0, right = nums.length - 1;
    while (left <= right) {
        int mid = left + (right - left) / 2;
        if (nums[mid] == target) return mid;

        if (nums[left] <= nums[mid]) {
            // 左半有序
            if (nums[left] <= target && target < nums[mid]) {
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        } else {
            // 右半有序
            if (nums[mid] < target && target <= nums[right]) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
    }
    return -1;
}
```

### 3.2 二分查找细节对比

| 模板 | 区间 | while 条件 | left/right 更新 | 返回值 |
|------|------|-----------|----------------|--------|
| 精确查找 | `[left, right]` | `left <= right` | `mid±1` | 下标或 -1 |
| 左边界 | `[left, right)` | `left < right` | `right=mid`, `left=mid+1` | left |
| 右边界 | `[left, right)` | `left < right` | `left=mid+1`, `right=mid` | left-1 |

> 💡 **记忆口诀**：闭区间等号三路分支，开区间不等号两路分支。遇到变体先判断是"精确查找"还是"边界查找"，选对模板事半功倍。

---

### 3.3 二分答案模板（LC 875 爱吃香蕉的珂珂）

> 当题目要求"最小化最大值"或"最大化最小值"时，考虑二分答案。

```java
class Solution {
    public int minEatingSpeed(int[] piles, int h) {
        int left = 1, right = 0;
        for (int pile : piles) {
            right = Math.max(right, pile);
        }

        while (left < right) {
            int mid = left + (right - left) / 2;
            if (canEatAll(piles, h, mid)) {
                right = mid; // 还能更慢吗？
            } else {
                left = mid + 1; // 需要更快
            }
        }
        return left;
    }

    private boolean canEatAll(int[] piles, int h, int speed) {
        int hours = 0;
        for (int pile : piles) {
            hours += (pile + speed - 1) / speed; // 向上取整
            if (hours > h) return false;
        }
        return hours <= h;
    }
}
```

| 维度 | 数据 |
|------|------|
| 时间复杂度 | O(n log m)，m 为搜索范围 |
| 空间复杂度 | O(1) |
| 二分答案适用特征 | 单调性：速度越快，时间越短 |

---

## 4. 快速幂与快速乘

### 4.1 快速幂模板

> 计算 `a^b % mod` 在 O(log b) 时间内。

**递归写法**

```java
long fastPow(long a, long b, long mod) {
    if (b == 0) return 1L;
    long half = fastPow(a, b / 2, mod);
    if (b % 2 == 0) {
        return (half * half) % mod;
    } else {
        return ((half * half) % mod * a) % mod;
    }
}
```

**迭代写法**

```java
long fastPowIter(long a, long b, long mod) {
    long result = 1L;
    a %= mod;
    while (b > 0) {
        if ((b & 1) == 1) { // 当前 bit 为 1
            result = (result * a) % mod;
        }
        a = (a * a) % mod;
        b >>= 1;
    }
    return result;
}
```

### 4.2 矩阵快速幂（斐波那契 O(log n)）

> 计算斐波那契数列第 n 项，O(log n) 时间（面试中可与 DP O(n) 对比，展示进阶能力）。

```java
class MatrixFastPow {
    static final long MOD = 1_000_000_007L;

    public long fibonacci(int n) {
        if (n <= 1) return n;
        long[][] base = {{1, 1}, {1, 0}};
        long[][] result = matrixPow(base, n - 1);
        return result[0][0];
    }

    private long[][] matrixPow(long[][] mat, int exp) {
        int size = mat.length;
        // 单位矩阵
        long[][] result = new long[size][size];
        for (int i = 0; i < size; i++) result[i][i] = 1;

        while (exp > 0) {
            if ((exp & 1) == 1) {
                result = multiply(result, mat);
            }
            mat = multiply(mat, mat);
            exp >>= 1;
        }
        return result;
    }

    private long[][] multiply(long[][] a, long[][] b) {
        int n = a.length;
        long[][] c = new long[n][n];
        for (int i = 0; i < n; i++) {
            for (int k = 0; k < n; k++) {
                if (a[i][k] == 0) continue;
                for (int j = 0; j < n; j++) {
                    c[i][j] = (c[i][j] + a[i][k] * b[k][j]) % MOD;
                }
            }
        }
        return c;
    }
}
```

| 维度 | 数据 |
|------|------|
| 时间复杂度 | O(log n) |
| 空间复杂度 | O(1) |
| 对比 | 常规 DP O(n)，矩阵快速幂 O(log n) |

---

### 4.3 LC 50 Pow(x, n)

```java
class Solution {
    public double myPow(double x, int n) {
        // 处理 n 为 Integer.MIN_VALUE 的边界情况
        long N = n;
        if (N < 0) {
            x = 1 / x;
            N = -N;
        }

        double result = 1.0;
        while (N > 0) {
            if ((N & 1) == 1) {
                result *= x;
            }
            x *= x;
            N >>= 1;
        }
        return result;
    }
}
```

| 维度 | 数据 |
|------|------|
| 时间复杂度 | O(log n) |
| 空间复杂度 | O(1) |
| 公司标签 | 字节跳动、阿里 |

> ⚠️ `n = Integer.MIN_VALUE` 时，直接取反会溢出，必须先用 `long` 转换。

---

### 4.4 LC 372 超级次方

> 快速幂 + 模运算的结合。

```java
class Solution {
    private static final int MOD = 1337;

    public int superPow(int a, int[] b) {
        if (b == null || b.length == 0) return 1;
        return superPowHelper(a, b, b.length - 1);
    }

    private int superPowHelper(int a, int[] b, int idx) {
        if (idx < 0) return 1;
        // 公式：pow(a, b) = pow(a, 最后一位) * pow(pow(a, 前几位), 10) % MOD
        return (fastPow(superPowHelper(a, b, idx - 1), 10) * fastPow(a, b[idx])) % MOD;
    }

    private int fastPow(int a, int b) {
        int result = 1;
        a %= MOD;
        while (b > 0) {
            if ((b & 1) == 1) {
                result = (result * a) % MOD;
            }
            a = (a * a) % MOD;
            b >>= 1;
        }
        return result;
    }
}
```

| 维度 | 数据 |
|------|------|
| 时间复杂度 | O(n log b) |
| 空间复杂度 | O(n) 递归栈 |
| 核心公式 | `pow(a, [b₀, b₁, ..., bₖ]) = pow(pow(a, [b₀, ..., bₖ₋₁]), 10) * pow(a, bₖ)` |

---

## 5. 字符串算法

### 5.1 KMP 算法

> 完整的 KMP 实现，包括 next 数组推导。

```java
class KMP {
    /**
     * KMP 匹配：返回 pattern 在 text 中首次出现的位置，未找到返回 -1
     */
    public int kmp(String text, String pattern) {
        if (pattern.isEmpty()) return 0;
        int n = text.length(), m = pattern.length();

        // 构建 next 数组
        int[] next = buildNext(pattern);

        // 匹配
        int j = 0;
        for (int i = 0; i < n; i++) {
            while (j > 0 && text.charAt(i) != pattern.charAt(j)) {
                j = next[j - 1];
            }
            if (text.charAt(i) == pattern.charAt(j)) {
                j++;
            }
            if (j == m) {
                return i - m + 1;
            }
        }
        return -1;
    }

    /**
     * 构建 next 数组（前缀函数）
     * next[i] = pattern[0..i] 中最长相等前后缀的长度
     */
    private int[] buildNext(String pattern) {
        int m = pattern.length();
        int[] next = new int[m];
        int j = 0; // 最长前后缀长度

        for (int i = 1; i < m; i++) {
            while (j > 0 && pattern.charAt(i) != pattern.charAt(j)) {
                j = next[j - 1];
            }
            if (pattern.charAt(i) == pattern.charAt(j)) {
                j++;
            }
            next[i] = j;
        }
        return next;
    }
}
```

**next 数组推导示例**

```
pattern = "ABABACA"
next[0] = 0
next[1] = 0 (AB: 无相等前后缀)
next[2] = 1 (ABA: A = A)
next[3] = 2 (ABAB: AB = AB)
next[4] = 3 (ABABA: ABA = ABA)
next[5] = 0 (ABABAC: 无相等前后缀)
next[6] = 1 (ABABACA: A = A)
```

| 维度 | 数据 |
|------|------|
| 时间复杂度 | O(n + m) |
| 空间复杂度 | O(m) |
| 对比暴力 | 暴力 O(n × m)，KMP O(n + m) |

---

### 5.2 Manacher 算法（最长回文子串 O(n)）

> 在 O(n) 时间内找出字符串的最长回文子串。

```java
class Manacher {
    public String longestPalindrome(String s) {
        if (s == null || s.length() == 0) return "";

        // 1. 插入分隔符，统一奇偶处理
        char[] t = preprocess(s);
        int n = t.length;
        int[] p = new int[n]; // p[i] 表示以 t[i] 为中心的回文半径（含中心）

        int center = 0, right = 0; // 当前最右回文边界

        for (int i = 0; i < n; i++) {
            // 初始化 p[i]：利用对称性
            if (i < right) {
                int mirror = 2 * center - i;
                p[i] = Math.min(p[mirror], right - i);
            }

            // 中心扩展
            while (i - p[i] - 1 >= 0 && i + p[i] + 1 < n
                    && t[i - p[i] - 1] == t[i + p[i] + 1]) {
                p[i]++;
            }

            // 更新最右边界
            if (i + p[i] > right) {
                center = i;
                right = i + p[i];
            }
        }

        // 找最长回文
        int maxLen = 0, maxCenter = 0;
        for (int i = 0; i < n; i++) {
            if (p[i] > maxLen) {
                maxLen = p[i];
                maxCenter = i;
            }
        }

        // 还原原始字符串中的回文
        int start = (maxCenter - maxLen) / 2;
        return s.substring(start, start + maxLen);
    }

    private char[] preprocess(String s) {
        // "abc" => "^#a#b#c#$"
        int n = s.length();
        char[] t = new char[2 * n + 3];
        t[0] = '^';
        for (int i = 0; i < n; i++) {
            t[2 * i + 1] = '#';
            t[2 * i + 2] = s.charAt(i);
        }
        t[2 * n + 1] = '#';
        t[2 * n + 2] = '$';
        return t;
    }
}
```

| 维度 | 数据 |
|------|------|
| 时间复杂度 | O(n) |
| 空间复杂度 | O(n) |
| 对比 | 暴力 O(n³)，中心扩展 O(n²)，Manacher O(n) |

> 💡 面试中如果不是明确要求 O(n)，通常中心扩展法（O(n²)）已经足够好。Manacher 是加分项。

---

### 5.3 LC 28 实现 strStr()

> 经典字符串匹配问题，可用 KMP。

```java
class Solution {
    public int strStr(String haystack, String needle) {
        if (needle == null || needle.isEmpty()) return 0;
        if (haystack.length() < needle.length()) return -1;

        // 使用 KMP
        return new KMP().kmp(haystack, needle);
    }
}
```

| 维度 | 数据 |
|------|------|
| 时间复杂度 | O(n + m) |
| 空间复杂度 | O(m) |

---

### 5.4 LC 214 最短回文串

> 在字符串前面添加字符，使结果变成回文串，求最短结果。本质是求从开头开始的最长回文子串。

```java
class Solution {
    public String shortestPalindrome(String s) {
        if (s == null || s.length() == 0) return "";

        // 反转 + KMP 求最长回文前缀
        String rev = new StringBuilder(s).reverse().toString();
        String combined = s + "#" + rev; // "#" 分隔避免跨越匹配

        // 计算 combined 的 next 数组（最后一个值即为最长回文前缀长度）
        int[] next = new KMP().buildNext(combined);
        int longestPrefix = next[combined.length() - 1];

        // 将剩余部分反转后加到前面
        String suffix = s.substring(longestPrefix);
        return new StringBuilder(suffix).reverse().toString() + s;
    }
}
```

| 维度 | 数据 |
|------|------|
| 时间复杂度 | O(n) |
| 空间复杂度 | O(n) |
| 公司标签 | 字节跳动、阿里 |

---

## 6. 并查集 Union-Find

### 6.1 完整模板（路径压缩 + 按秩合并）

```java
class UnionFind {
    private final int[] parent;
    private final int[] rank;
    private int count; // 连通分量数

    public UnionFind(int n) {
        parent = new int[n];
        rank = new int[n];
        count = n;
        for (int i = 0; i < n; i++) {
            parent[i] = i;
            rank[i] = 1;
        }
    }

    /**
     * 查找根节点（路径压缩）
     */
    public int find(int x) {
        if (parent[x] != x) {
            parent[x] = find(parent[x]); // 递归压缩路径
        }
        return parent[x];
    }

    /**
     * 合并两个集合（按秩合并）
     */
    public void union(int x, int y) {
        int rootX = find(x);
        int rootY = find(y);
        if (rootX == rootY) return;

        // 将秩较小的树合并到秩较大的树上
        if (rank[rootX] < rank[rootY]) {
            parent[rootX] = rootY;
        } else if (rank[rootX] > rank[rootY]) {
            parent[rootY] = rootX;
        } else {
            parent[rootY] = rootX;
            rank[rootX]++;
        }
        count--;
    }

    /**
     * 判断两个节点是否连通
     */
    public boolean isConnected(int x, int y) {
        return find(x) == find(y);
    }

    /**
     * 返回连通分量数
     */
    public int getCount() {
        return count;
    }
}
```

| 操作 | 时间复杂度 |
|------|:---------:|
| find | O(α(n))，近似 O(1) |
| union | O(α(n))，近似 O(1) |
| 说明 | α(n) 为阿克曼函数的反函数，n ≤ 10⁶ 时 α(n) ≤ 5 |

---

### 6.2 LC 200 岛屿数量（并查集解法）

```java
class Solution {
    public int numIslands(char[][] grid) {
        if (grid == null || grid.length == 0) return 0;
        int m = grid.length, n = grid[0].length;
        UnionFind uf = new UnionFind(m * n);
        int waterCount = 0;

        int[][] dirs = {{1, 0}, {0, 1}}; // 只需合并右和下

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == '0') {
                    waterCount++;
                } else {
                    for (int[] dir : dirs) {
                        int ni = i + dir[0];
                        int nj = j + dir[1];
                        if (ni < m && nj < n && grid[ni][nj] == '1') {
                            uf.union(i * n + j, ni * n + nj);
                        }
                    }
                }
            }
        }
        return uf.getCount() - waterCount;
    }
}
```

| 维度 | 数据 |
|------|------|
| 时间复杂度 | O(m × n × α(m×n)) |
| 空间复杂度 | O(m × n) |
| 对比 DFS | DFS O(m×n) 栈空间可能溢出，并查集更适合超大规模矩阵 |

---

### 6.3 LC 547 省份数量

见 08 模块 6.3 节实现。

---

### 6.4 LC 684 冗余连接

```java
class Solution {
    public int[] findRedundantConnection(int[][] edges) {
        int n = edges.length;
        UnionFind uf = new UnionFind(n);

        for (int[] edge : edges) {
            int u = edge[0] - 1, v = edge[1] - 1; // 转为 0-indexed
            if (uf.isConnected(u, v)) {
                return edge; // 形成环，这条边就是冗余的
            }
            uf.union(u, v);
        }
        return new int[0];
    }
}
```

| 维度 | 数据 |
|------|------|
| 时间复杂度 | O(n × α(n)) |
| 空间复杂度 | O(n) |

> 🎯 **并查集使用场景**：图的连通性问题、冗余连接、Kruskal 最小生成树、动态连通性。

---

## 7. 线段树/树状数组入门

### 7.1 树状数组 BIT 模板（单点更新 + 区间查询）

> 树状数组适用于"单点更新、区间前缀和查询"场景。

```java
class BinaryIndexedTree {
    private final int[] tree;
    private final int n;

    public BinaryIndexedTree(int n) {
        this.n = n;
        this.tree = new int[n + 1]; // 1-indexed
    }

    /**
     * 单点更新：在下标 i 的位置增加 val
     */
    public void update(int i, int val) {
        while (i <= n) {
            tree[i] += val;
            i += lowbit(i); // 跳到父节点
        }
    }

    /**
     * 前缀和查询：求前 i 个元素的和
     */
    public int query(int i) {
        int sum = 0;
        while (i > 0) {
            sum += tree[i];
            i -= lowbit(i); // 跳到前驱节点
        }
        return sum;
    }

    /**
     * 区间查询：[l, r] 的和
     */
    public int rangeQuery(int l, int r) {
        return query(r) - query(l - 1);
    }

    private int lowbit(int x) {
        return x & -x;
    }
}
```

| 维度 | 数据 |
|------|------|
| 单点更新 | O(log n) |
| 前缀和查询 | O(log n) |
| 空间 | O(n) |
| 对比前缀和 | 前缀和 O(1) 但不可修改，BIT 支持修改 |

> ⚠️ 树状数组不能直接做区间更新（如果区间更新 + 区间查询，需要使用差分思想扩展的 BIT 或直接上线段树）。

---

### 7.2 LC 307 区域和检索 - 可修改

```java
class NumArray {
    private final BinaryIndexedTree bit;
    private final int[] nums;

    public NumArray(int[] nums) {
        this.nums = nums;
        this.bit = new BinaryIndexedTree(nums.length);
        for (int i = 0; i < nums.length; i++) {
            bit.update(i + 1, nums[i]); // BIT 使用 1-indexed
        }
    }

    public void update(int index, int val) {
        int diff = val - nums[index];
        nums[index] = val;
        bit.update(index + 1, diff);
    }

    public int sumRange(int left, int right) {
        return bit.rangeQuery(left + 1, right + 1);
    }
}
```

| 维度 | 数据 |
|------|------|
| 初始化 | O(n log n) |
| 单次更新 | O(log n) |
| 区间查询 | O(log n) |
| 公司标签 | 阿里、字节跳动 |

> 💡 面试中线段树 vs 树状数组的选择：树状数组代码量少、常数小，但只能处理前缀和可合并的问题（求和、求异或、求最大值）；线段树功能更强（区间修改、懒标记、区间合并），但代码量更大。

---

## 8. 单调队列

### 8.1 Deque 滑动窗口最大值模板

```java
class MonotonicQueue {
    // 双端队列，存储下标，对应的值保持单调递减
    private final Deque<Integer> deque = new ArrayDeque<>();
    private final int[] nums;

    public MonotonicQueue(int[] nums) {
        this.nums = nums;
    }

    /**
     * 添加新元素（下标为 i）
     */
    public void add(int i) {
        // 移除队尾所有比当前元素小的元素
        while (!deque.isEmpty() && nums[deque.peekLast()] < nums[i]) {
            deque.pollLast();
        }
        deque.offerLast(i);
    }

    /**
     * 移除超出窗口范围的元素（窗口左边界为 left）
     */
    public void removeOutOfWindow(int left) {
        while (!deque.isEmpty() && deque.peekFirst() < left) {
            deque.pollFirst();
        }
    }

    /**
     * 获取当前窗口最大值
     */
    public int getMax() {
        return nums[deque.peekFirst()];
    }
}
```

> 💡 单调队列的核心思想：维护一个递减队列，队首是窗口最大值。每个元素最多入队一次、出队一次。

---

### 8.2 LC 239 滑动窗口最大值

见 08 模块 2.4 节实现。

---

### 8.3 LC 862 和至少为 K 的最短子数组

> 前缀和 + 单调双端队列，hard 题。

```java
class Solution {
    public int shortestSubarray(int[] nums, int k) {
        int n = nums.length;
        long[] prefix = new long[n + 1];
        for (int i = 0; i < n; i++) {
            prefix[i + 1] = prefix[i] + nums[i];
        }

        // Deque 存储前缀和的下标，保持前缀和递增
        Deque<Integer> deque = new ArrayDeque<>();
        int minLen = Integer.MAX_VALUE;

        for (int i = 0; i <= n; i++) {
            // 对每个 i，从队首找可行的起点
            while (!deque.isEmpty() && prefix[i] - prefix[deque.peekFirst()] >= k) {
                minLen = Math.min(minLen, i - deque.pollFirst());
            }
            // 维护前缀和的单调递增（移除队尾大于等于当前前缀和的下标）
            while (!deque.isEmpty() && prefix[i] <= prefix[deque.peekLast()]) {
                deque.pollLast();
            }
            deque.offerLast(i);
        }

        return minLen == Integer.MAX_VALUE ? -1 : minLen;
    }
}
```

| 维度 | 数据 |
|------|------|
| 时间复杂度 | O(n) |
| 空间复杂度 | O(n) |
| 公司标签 | 字节跳动 |

> 🎯 **单调队列使用场景**：需要在一个滑动窗口内快速获取最值时使用。典型标志——"固定/可变长度的子数组，求最值"。

---

## 9. 记忆化搜索

### 9.1 记忆化搜索模板

> 记忆化搜索 = DFS + 缓存。将递归过程中的中间结果存下来，避免重复计算。

```java
class Memoization {
    // 记忆化数组/Map
    private int[] memo;

    public int solve(int n) {
        memo = new int[n + 1];
        Arrays.fill(memo, -1); // -1 表示未计算过
        return dfs(n);
    }

    private int dfs(int i) {
        if (i == 0) return 0; // base case
        if (memo[i] != -1) return memo[i]; // 已经计算过

        // 状态转移
        int result = 0;
        // ... 递归计算 ...

        memo[i] = result;
        return result;
    }
}
```

### 9.2 记忆化搜索 vs DP

| 对比维度 | 记忆化搜索（自顶向下） | 动态规划（自底向上） |
|---------|:-------------------:|:-----------------:|
| 思考方式 | 从大问题拆解到小问题 | 从小问题构建到大问题 |
| 实现难度 | 更符合直觉，代码简单 | 需要推导递推公式和遍历顺序 |
| 性能 | 有递归栈开销 | 通常更优（无栈开销） |
| 空间 | 额外存 memo | DP 数组，可空间优化 |
| 适用范围 | DFS + 缓存即可 | 需要明确的状态转移顺序 |

> 💡 **面试建议**：如果 DP 方程推导不熟练，先用记忆化搜索写出正确解，再根据面试官要求改为 DP。这比现场推导 DP 公式更稳妥。

### 9.3 记忆化搜索示例：LC 329 矩阵中的最长递增路径

```java
class Solution {
    private int[][] memo;
    private int[][] matrix;
    private int m, n;
    private final int[][] dirs = {{-1,0}, {1,0}, {0,-1}, {0,1}};

    public int longestIncreasingPath(int[][] matrix) {
        if (matrix == null || matrix.length == 0) return 0;
        this.matrix = matrix;
        this.m = matrix.length;
        this.n = matrix[0].length;
        this.memo = new int[m][n];

        int maxLen = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                maxLen = Math.max(maxLen, dfs(i, j));
            }
        }
        return maxLen;
    }

    private int dfs(int i, int j) {
        if (memo[i][j] != 0) return memo[i][j];

        int maxChild = 0;
        for (int[] dir : dirs) {
            int ni = i + dir[0], nj = j + dir[1];
            if (ni >= 0 && ni < m && nj >= 0 && nj < n
                    && matrix[ni][nj] > matrix[i][j]) {
                maxChild = Math.max(maxChild, dfs(ni, nj));
            }
        }
        memo[i][j] = maxChild + 1;
        return memo[i][j];
    }
}
```

| 维度 | 数据 |
|------|------|
| 时间复杂度 | O(m × n)，每个格子只计算一次 |
| 空间复杂度 | O(m × n) |
| 公司标签 | 字节跳动 |

> 🎯 **记忆化搜索适用场景**：状态空间有限、递归有大量重叠子问题、且天然适合 DFS 思维的问题。

---

## 10. 随机算法

### 10.1 蓄水池抽样

> 适用于"从海量数据中等概率随机抽取 K 个样本"——数据未知大小或无法全部加载到内存。

```java
import java.util.Random;

class ReservoirSampling {
    private final Random random = new Random();

    /**
     * 从流中随机选取 k 个元素
     * @param stream 数据流（假设实现 Iterator 接口）
     * @param k 抽样个数
     * @return 抽取的样本
     */
    public int[] sample(Iterator<Integer> stream, int k) {
        int[] reservoir = new int[k];

        // 1. 前 k 个元素直接放入蓄水池
        for (int i = 0; i < k && stream.hasNext(); i++) {
            reservoir[i] = stream.next();
        }

        // 2. 从第 k+1 个元素开始，以 k / (i+1) 的概率决定是否替换
        int i = k;
        while (stream.hasNext()) {
            int val = stream.next();
            int j = random.nextInt(i + 1); // [0, i] 随机
            if (j < k) {
                reservoir[j] = val;
            }
            i++;
        }

        return reservoir;
    }
}
```

| 维度 | 数据 |
|------|------|
| 时间复杂度 | O(n) |
| 空间复杂度 | O(k) |
| 正确性 | 每个元素被选中的概率为 k/n |

---

### 10.2 洗牌算法（Fisher-Yates）

见 08 模块 5.4 节 `LC 384 打乱数组` 实现。

**通用洗牌模板**：

```java
void shuffle(int[] arr) {
    Random random = new Random();
    for (int i = arr.length - 1; i > 0; i--) {
        int j = random.nextInt(i + 1); // [0, i] 随机下标
        // 交换
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }
}
```

> 💡 **蓄水池抽样 vs 洗牌**：蓄水池抽样适用于未知长度的流式数据，洗牌适用于已知长度的数组。两者本质相同——从 n 个元素中随机取 k 个。

---

> 🎯 **竞赛技巧在面试中的定位**：不是每道题都需要最优解，但当面试官说"有没有更好的方法"时，上面的技巧就是你拉开差距的武器。优先掌握前三节（位运算、前缀和与差分、二分变体），其余在刷题中逐步积累。

---

**返回总览**：[刷题总览](./00-LeetCode刷题总览.md)
