# 02-Manacher应用与题解
> 一句话：647 回文计数、回文半径查询、5 双解对比——Manacher 从"求最长"到"全回文结构"的应用延伸

## 📚 目录
1. [5 最长回文子串（中心扩展 vs Manacher）](#1-5-最长回文子串中心扩展-vs-manacher)
2. [647 回文子串计数（Manacher 版）](#2-647-回文子串计数manacher-版)
3. [回文半径数组的查询应用](#3-回文半径数组的查询应用)
4. [214 最短回文串（Manacher 视角）](#4-214-最短回文串manacher-视角)
5. [回文家族总览](#5-回文家族总览)
6. [易错点与面试要点](#6-易错点与面试要点)

---

## 1. 5 最长回文子串（中心扩展 vs Manacher）

### 1.1 中心扩展版（O(n²)，面试基础答案）

```java
/**
 * LeetCode 5. 最长回文子串（中心扩展 O(n²)）
 * 每个位置（及相邻对）向两边扩展——奇偶两种中心
 */
public String longestPalindrome(String s) {
    int start = 0, maxLen = 0;
    for (int i = 0; i < s.length(); i++) {
        int len1 = expand(s, i, i);              // ① 奇数中心
        int len2 = expand(s, i, i + 1);          // ② 偶数中心
        int len = Math.max(len1, len2);
        if (len > maxLen) {
            maxLen = len;
            start = i - (len - 1) / 2;           // ③ 由长度反推起点
        }
    }
    return s.substring(start, start + maxLen);
}

private int expand(String s, int l, int r) {
    while (l >= 0 && r < s.length() && s.charAt(l) == s.charAt(r)) {
        l--; r++;
    }
    return r - l - 1;                            // ④ 回文长度
}
// 推演 "babad" → "bab" 或 "aba" ✓
// ⚠️ 中心扩展是面试基础答案；Manacher 是被追问优化时的进阶
```

> 💡 **双解递进**："**5 题面试流程 = 中心扩展（2 分钟保底）→ 追问'能 O(n) 吗' → Manacher（01 模板）**。'先保底后进阶'是 5 题的完整答案路径。"

## 2. 647 回文子串计数（Manacher 版）

### 2.1 题目与思路

**题**：回文子串总个数（中心扩展 O(n²) 可过；Manacher O(n)）。

**半径数组求和**：以 i 为中心的回文数 = radius[i] / 2（# 处理）：

```java
/**
 * LeetCode 647. 回文子串计数（Manacher O(n)）
 * 每个中心的回文子串数 = ceil(radius[i] / 2)
 */
public int countSubstrings(String s) {
    // ① # 插入（同 01 模板）
    StringBuilder sb = new StringBuilder("#");
    for (char c : s.toCharArray()) sb.append(c).append('#');
    String t = sb.toString();
    int n = t.length();

    int[] radius = new int[n];
    int center = 0, maxRight = 0;
    int count = 0;

    for (int i = 0; i < n; i++) {
        if (i < maxRight) {
            int mirror = 2 * center - i;
            radius[i] = Math.min(radius[mirror], maxRight - i);
        }
        int l = i - radius[i] - 1, r = i + radius[i] + 1;
        while (l >= 0 && r < n && t.charAt(l) == t.charAt(r)) {
            radius[i]++;
            l--; r++;
        }
        if (i + radius[i] > maxRight) {
            maxRight = i + radius[i];
            center = i;
        }
        // ② ⚠️ 计数: 中心 i 贡献的回文子串数 = (radius[i] + 1) / 2
        count += (radius[i] + 1) / 2;
    }
    return count;
}
// 推演 "abc" → 3 ✓（a,b,c——每个 # 中心 radius=1 贡献 0，字符中心贡献 1）
//   "aaa" → 6 ✓（a,a,a,aa,aa,aaa）
// ⚠️ 半径 → 计数: t 中半径 radius 的回文（原串）个数 = (radius+1)/2
//   （# 中心贡献偶数回文、字符中心贡献奇数回文）
```

> 🎯 **647 的 Manacher 价值**："**'count += (radius[i]+1)/2' 一行把 O(n²) 计数变 O(n)**——半径数组不仅是'最长'，更是'全部回文结构'的编码。647 是 Manacher 从'求最长'到'计数'的应用延伸。"

## 3. 回文半径数组的查询应用

### 3.1 任意子串回文判定

```java
// 问题: 多次查询 s[l..r] 是否回文（O(1) 每次）
// 解法: Manacher 半径数组 + 中心映射

// ① 预处理: Manacher 得 radius（t 串）
// ② 查询 (l, r): 原串区间 → t 串中心 (l+r+1)
//   回文判定: radius[(l+r+1)] ≥ (r−l+1)（半径覆盖区间）

// ⚠️ 核心: 区间 [l, r] 是回文 ⇔
//   其中心在 t 中的半径 ≥ 区间长度
//   → O(1) 查询（配合大量询问）

// 应用: 131 分割回文串的优化、回文动态规划预处理
```

> 💡 **半径数组的查询价值**："**'中心半径 ≥ 区间长 → 回文'是 O(1) 回文判定的核心——Manacher 预处理一次、任意查询 O(1)**。131 分割回文串等题的预处理优化全靠它。"

## 4. 214 最短回文串（Manacher 视角）

### 4.1 双解法

```java
// 214 最短回文串——KMP 版（02 文件）与 Manacher 版：
// Manacher 版: 求 s 的"最长回文前缀"——
//   回文中心在 [0, n/2] 且左边界贴 0 的回文
//   剩余后缀反转加到前面

// ⚠️ 两解对比:
//   KMP 版: s#rev(s) 的 border（01 文件）
//   Manacher 版: 半径数组找"贴左边界"的回文前缀
//   面试: KMP 版更简洁；Manacher 版展示半径查询应用

// 例: s="aacecaaa" 的最长回文前缀 = "aacecaa"（长 7）
//   → 剩余 "a" 反转前置 → "a" + "aacecaaa" ✓
```

> 🎯 **双解展示**："**214 = KMP border 与 Manacher 半径的双解交汇题——同一问题两种线性算法都能解**。面试两个都能给 = 对字符串算法家族有全景理解。"

## 5. 回文家族总览

| 题 | 回文需求 | Manacher 角色 |
|:---:|:---:|:---:|
| 5 最长回文 | 最长子串 | O(n) 主解法 |
| 647 回文计数 | 全部子串数 | 半径求和 O(n) |
| 214 最短回文 | 最长回文前缀 | 贴左边界半径 |
| 131 分割回文 | 子串回文判定 | 半径 O(1) 查询 |
| 516 回文子序列 | 子序列（非连续） | 不适用（DP 题） |

```text
⚠️ 家族规律:
  "连续回文 → Manacher/中心扩展；
   子序列回文 → DP（516）
   ——'连续 vs 子序列'是回文题的第一分叉"
```

## 6. 易错点与面试要点

### 6.1 易错点

| # | 易错点 | 正确写法 | 后果 |
|:---:|------|------|------|
| 1 | 647 计数公式错 | `(radius[i]+1)/2` | 回文数错 |
| 2 | 查询中心映射错 | `(l+r+1)` 映射 t | 判定错 |
| 3 | 214 最长回文前缀找错 | 左边界贴 0 的中心 | 拼接错 |
| 4 | 中心扩展起点公式错 | `i − (len−1)/2` | 截取错位 |
| 5 | 半径数组忘复用 | 每位置暴力 | O(n²) 退化 |
| 6 | 5 题直接写 Manacher | 先中心扩展保底 | 复杂度讲不清 |
| 7 | 回文子序列用 Manacher | DP（516） | 错误解法 |

### 6.2 面试要点

```text
① 5 题流程: "中心扩展 O(n²) 保底 → Manacher O(n) 进阶"
② 半径语义: "半径数组 = 全部回文结构的编码"
③ 647 计数: "(radius+1)/2 一行求和"
④ 复杂度: "maxRight 单调 → O(n)"

⚠️ 高频追问: "Manacher 为什么是 O(n)？"
  → "maxRight 单调右移——暴力扩展只在边界外发生，
    每个位置最多扩展一次；其余靠对称复用 O(1)"
```

> 🎯 **核心要点**：Manacher 应用通关——**5（双解递进）+ 647（半径计数）+ 查询应用（O(1) 判定）+ 214（贴边半径）**。四应用覆盖"最长/计数/查询/前缀"——"半径数组 = 回文结构全集"这一认知，就是 Manacher 从模板到应用的桥梁。

---

**上一篇**：[01-Manacher核心原理与模板](01-Manacher核心原理与模板.md)
**返回总览**：[00-字符串高级算法知识体系总览](../00-字符串高级算法知识体系总览.md)
