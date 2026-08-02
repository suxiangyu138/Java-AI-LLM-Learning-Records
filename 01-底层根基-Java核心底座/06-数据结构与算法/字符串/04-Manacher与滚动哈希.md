# Manacher与滚动哈希
> 回文的 O(n) 解法与子串的 O(1) 哈希——两个"面试加分"的字符串算法

## 📚 目录
1. [Manacher 算法：O(n) 最长回文](#1-manacher-算法on-最长回文)
2. [中心扩展 vs Manacher](#2-中心扩展-vs-manacher)
3. [滚动哈希（Rabin-Karp）](#3-滚动哈希rabin-karp)
4. [滚动哈希的应用](#4-滚动哈希的应用)

---

## 1. Manacher 算法：O(n) 最长回文

### 1.1 为什么需要 Manacher？

```
中心扩展法：O(n²)——每个中心都要向外扩展
  最长回文子串（5 题）在 n=10⁵ 时 O(n²) 不可行

Manacher：利用"回文的对称性"避免重复扩展 → O(n)

核心洞察：
  回文内部是对称的 → 已算过的右半部分可以直接复用左半结果
  例：已算出中心 c 的半径 r（覆盖到 right）
      在 c 右侧的 i 的回文半径 ≥ min(对称点 i' 的半径, right-i)
```

### 1.2 预处理：插入分隔符

```
原串 "aba" → 插入 # → "#a#b#a#"
  奇数长度回文 "aba" → 仍是奇数（中心 b）
  偶数长度回文 "abba" → "#a#b#b#a#" 变成奇数（中心 #）
  → 统一处理奇偶回文！

技巧：用 ^$ 做边界哨兵（避免越界判断）
  "^#a#b#a#$"
```

### 1.3 完整实现（面试展示版）

```java
/**
 * LeetCode 5. 最长回文子串（Manacher 版 O(n)）
 * 面试加分项：中心扩展 O(n²) 是标准答案，
 * Manacher 展示深度
 */
public String longestPalindrome(String s) {
    // ① 预处理：插入分隔符
    StringBuilder sb = new StringBuilder("^");
    for (char c : s.toCharArray()) {
        sb.append('#').append(c);
    }
    sb.append("#$");
    char[] t = sb.toString().toCharArray();
    int n = t.length;

    int[] radius = new int[n];       // radius[i] = 以 i 为中心的回文半径
    int center = 0, right = 0;       // 当前最右回文的中心和右边界
    int maxLen = 0, maxCenter = 0;

    for (int i = 1; i < n - 1; i++) {
        // ② 初始化半径：利用对称性
        if (i < right) {
            int mirror = 2 * center - i;        // i 的对称点
            radius[i] = Math.min(right - i, radius[mirror]);
        }

        // ③ 中心扩展（利用对称性后只需扩展未覆盖部分）
        while (t[i + radius[i] + 1] == t[i - radius[i] - 1]) {
            radius[i]++;
        }

        // ④ 更新最右回文
        if (i + radius[i] > right) {
            center = i;
            right = i + radius[i];
        }

        // ⑤ 记录最大
        if (radius[i] > maxLen) {
            maxLen = radius[i];
            maxCenter = i;
        }
    }

    // 还原：半径-1 = 原串回文长度，中心换算回原串下标
    int start = (maxCenter - maxLen) / 2;
    return s.substring(start, start + maxLen);
}
```

### 1.4 核心理解

```
三个关键变量：
  center/right：当前"最右回文"的中心和右边界
  radius[i]：i 的回文半径

对称性利用：
  i 在 [center, right] 内 → 对称点 mirror = 2*center - i
  radius[i] 至少 = min(radius[mirror], right - i)
  → 已算过的部分不再重复扩展 → O(n)

边界哨兵 ^$：
  扩展时 t[i+radius+1] 永不越界
  （^ 和 $ 是哨兵字符，永远不匹配）
```

---

## 2. 中心扩展 vs Manacher

| 维度 | 中心扩展 | Manacher |
|------|:---:|:---:|
| 时间复杂度 | O(n²) | **O(n)** |
| 空间 | O(1) | O(n) |
| 代码复杂度 | 简单 | 较复杂 |
| 面试定位 | **标准答案** | 加分项/深度展示 |
| 适用 | 一般面试 | 大数数据/竞赛 |

```
面试策略：
  先写中心扩展（O(n²) 直观正确）
  被追问"能不能更快" → 讲 Manacher 思想（对称性 + 右边界）
  能完整手写 Manacher 是明显加分
```

---

## 3. 滚动哈希（Rabin-Karp）

### 3.1 核心思想

```
字符串 → 整数：用多项式哈希
  hash(s) = (s[0]×base^(n-1) + s[1]×base^(n-2) + ... + s[n-1]) % mod

子串哈希 O(1) 计算（滚动）：
  从 "abc" 到 "bcd"：
  hash("bcd") = (hash("abc") - a×base²) × base + d
  → O(1) 滚动更新！

base 常用：131 / 31（质数，减少冲突）
mod 常用：10⁹+7 / 2⁶⁴（自然溢出）
```

### 3.2 完整实现（Rabin-Karp 匹配）

```java
/**
 * Rabin-Karp：滚动哈希做字符串匹配
 * 期望 O(n+m)，最坏 O(n×m)（哈希冲突时）
 */
public int rabinKarp(String text, String pattern) {
    int n = text.length(), m = pattern.length();
    if (m > n) return -1;

    long base = 131;
    long mod = 1_000_000_007;

    // ① 计算 base^(m-1)（预计算最高位权重）
    long power = 1;
    for (int i = 0; i < m - 1; i++) {
        power = power * base % mod;
    }

    // ② 计算 pattern 哈希 + text 第一个窗口哈希
    long patternHash = hash(pattern);
    long windowHash = 0;
    for (int i = 0; i < m; i++) {
        windowHash = (windowHash * base + text.charAt(i)) % mod;
    }

    // ③ 滚动比较
    if (windowHash == patternHash) return 0;    // 哈希相同（可再验证）
    for (int i = m; i < n; i++) {
        // 滚动：去掉旧首字符，加上新尾字符
        windowHash = ((windowHash - text.charAt(i - m) * power % mod
                       + mod) % mod * base + text.charAt(i)) % mod;
        if (windowHash == patternHash) return i - m + 1;
    }
    return -1;
}

private long hash(String s) {
    long h = 0;
    for (char c : s.toCharArray()) {
        h = (h * 131 + c) % 1_000_000_007;
    }
    return h;
}
// ⚠️ 哈希相等时可能冲突 → 工程上需再逐字符验证
// 冲突概率 ≈ 1/mod，极低
```

### 3.3 哈希冲突与处理

```
冲突来源：不同字符串映射到同一哈希值
处理：
  ① 双哈希：用两个 base 或两个 mod 同时算（冲突概率平方级降低）
  ② 冲突验证：哈希相等后再逐字符比较（KMP 保证正确）
  ③ 自然溢出：用 long 溢出代替取模（快，但无确定性）

面试话术：
"滚动哈希用多项式哈希把字符串映射为整数，
 子串哈希 O(1) 滚动更新，期望 O(n+m)。
 哈希冲突概率约 1/10⁹，工程上可加二次验证。"
```

---

## 4. 滚动哈希的应用

### 4.1 应用场景速查

| 题号 | 题目 | 滚动哈希应用 |
|:---:|------|------|
| 28 | strStr | Rabin-Karp 替代 KMP |
| 1044 | 最长重复子串 | 二分长度 + 哈希查重 |
| 214 | 最短回文串 | 哈希比较前缀/后缀 |
| 1062 | 最长重复子串 | 同 1044 |
| 718 | 最长重复子数组 | 二分 + 双哈希 |

### 4.2 1062/1044. 最长重复子串（二分+哈希）

```java
/**
 * 思路：二分答案长度 + 滚动哈希判重
 *   ① 二分可能的最长重复长度 [1, n]
 *   ② check(len)：滑动窗口计算所有长度为 len 的
 *      子串哈希 → 哈希出现重复 → len 可行
 *   复杂度：O(n log n)
 */
public boolean hasDuplicate(String s, int len) {
    Set<Long> seen = new HashSet<>();
    // 滚动计算所有长度 len 的子串哈希
    // ...（滚动哈希模板）
    return false;
}
```

### 4.3 滚动哈希 vs KMP 对比

| 维度 | KMP | 滚动哈希 |
|------|:---:|:---:|
| 时间复杂度 | O(n+m) 确定 | O(n+m) 期望 |
| 空间 | O(m) | O(1) |
| 正确性 | 100% | 有冲突概率 |
| 扩展性 | 仅匹配 | 子串查重/比较 |
| 适用 | 单模式匹配 | 多模式/查重/二分答案 |

```
选型：
  单模式匹配 → KMP（确定正确）
  子串查重/最长重复 → 滚动哈希（Set 查重利器）
  → 面试各备一题即可
```

> 🎯 **核心要点**：Manacher 与滚动哈希是字符串面试的两大"深度加分项"——**Manacher 用对称性把回文降到 O(n)**（中心扩展是标准答案，Manacher 是展示深度），**滚动哈希把子串比较变成 O(1)**（配合二分解决最长重复子串）。两者都理解思想 + 能写模板，字符串算法就到顶了。

---

**下一模块**：[07-Java实现与面试实战](07-Java实现与面试实战.md) | **返回总览**：[00-字符串知识体系总览](00-字符串知识体系总览.md)
