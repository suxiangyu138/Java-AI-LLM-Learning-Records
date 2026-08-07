# 01 - 字符串匹配进阶：KMP 原理与 Z 算法

> 定位：字符串匹配的理论纵深——KMP 的 next 数组推导、Z 算法、与 Rabin-Karp/后缀数组对比、周期检测、多模式匹配（AC 自动机简介）

## 📚 目录

1. [朴素匹配为什么慢](#1-朴素匹配为什么慢)
2. [KMP：next 数组的推导](#2-kmpnext-数组的推导)
3. [Z 算法](#3-z-算法)
4. [KMP 与 Z 的统一视角](#4-kmp-与-z-的统一视角)
5. [next 数组的周期应用](#5-next-数组的周期应用)
6. [匹配算法全景对比](#6-匹配算法全景对比)
7. [面试要点与追问](#7-面试要点与追问)

---

## 1. 朴素匹配为什么慢

### 1.1 问题与退化

```
朴素匹配：每个位置开始逐字符比较 → O(n·m)
退化场景：
  文本 "AAAAAAAAAB" 模式 "AAAB"
  → 每个位置都匹配到倒数第二步才失败 → O(n·m)

⚠️ 核心浪费：失败后"回退已匹配的部分"重新比较

⚠️ 面试必答：
"朴素匹配的浪费 = 失败后重新比较已匹配前缀——
 KMP 的核心是'利用已匹配信息'跳过重复比较。"
```

### 1.2 KMP 的核心思想

```
KMP 观察：
  匹配到模式第 j 位失败 → 模式的前 j 位已匹配
  → 模式自己的"最长相等前后缀"可以复用

例：模式 "ABABABC"，匹配到第 6 位（C）失败
  已匹配 "ABABAB" → 最长相等前后缀 "ABAB"（长度 4）
  → 文本指针不动，模式指针回退到 4（不是 0！）

⚠️ 面试必答：
"KMP = 失败时模式指针回退到'最长相等
 前后缀'位置——文本永不回退，O(n+m)。"
```

---

## 2. KMP：next 数组的推导

### 2.1 next 数组定义

```
next[j] = 模式前 j 个字符的最长相等前后缀长度
  （真前后缀：不能是整个串）

例："ABABAB" → next = [0,0,1,2,3,4]
  A:0；AB:0；ABA:1；ABAB:2；ABABA:3；ABABAB:4

⚠️ 面试必答：
"next[j] = 前 j 个字符的最长真相等前后缀——
 失败时模式跳到 next[j]，文本不动。"
```

### 2.2 next 的构建（递推）

```java
/** KMP next 数组构建（递推，O(m)） */
int[] buildNext(String pattern) {
    int m = pattern.length();
    int[] next = new int[m];
    int j = 0;                               // ⚠️ 当前最长前后缀长度

    for (int i = 1; i < m; i++) {
        while (j > 0 && pattern.charAt(i) != pattern.charAt(j))
            j = next[j - 1];                 // ⚠️ 回退（同 KMP 匹配逻辑）
        if (pattern.charAt(i) == pattern.charAt(j)) j++;
        next[i] = j;
    }
    return next;
}
// 推演 "ABABAB"：
//   i=1(B)：j=0，B≠A → next[1]=0
//   i=2(A)：A==A → j=1 → next[2]=1
//   i=3(B)：B==B → j=2 → next[3]=2
//   ... next=[0,0,1,2,3,4] ✓
// ⚠️ 递推与匹配同构：构建 next 就是
//   "模式匹配自己"（前缀匹配后缀）
```

### 2.3 KMP 匹配完整实现（28）

```java
/** LeetCode 28. 找出字符串中第一个匹配项的下标（KMP） */
public int strStr(String haystack, String needle) {
    int n = haystack.length(), m = needle.length();
    if (m == 0) return 0;

    int[] next = buildNext(needle);
    int j = 0;
    for (int i = 0; i < n; i++) {
        while (j > 0 && haystack.charAt(i) != needle.charAt(j))
            j = next[j - 1];                 // ⚠️ 失败回退（文本不动）
        if (haystack.charAt(i) == needle.charAt(j)) j++;
        if (j == m) return i - m + 1;        // 匹配完成
    }
    return -1;
}
// 推演 "aabaabaafa" 找 "aabaaf"：
//   匹配到 aabaab → 第 6 位 a vs f 失败
//   next[5]=2 → 模式跳到第 2 位继续 → 命中 ✓
// ⚠️ 复杂度：O(n+m)（文本指针不回退）
// ⚠️ 面试必答："KMP 匹配 = next 回退——
//   失败时 j = next[j-1]，i 永不回退。"
```

---

## 3. Z 算法

### 3.1 Z 数组定义

```
Z[i] = 以 i 开头的后缀与整个串的最长公共前缀长度

例："aabcaabx"：
  Z = [_, 1, 0, 0, 3, 1, 0, 0]
  Z[4]=3：s[4..]="aabx" 与 s="aabcaabx" 的 LCP = "aab" ✓

⚠️ 面试必答：
"Z 数组 = 每个后缀与全串的 LCP——
 与 next 是'兄弟算法'（都利用已匹配信息）。"
```

### 3.2 Z 的线性构建

```java
/** Z 数组构建（O(n)） */
int[] buildZ(String s) {
    int n = s.length();
    int[] z = new int[n];
    int l = 0, r = 0;                        // ⚠️ [l, r) 当前最右匹配区间

    for (int i = 1; i < n; i++) {
        if (i < r) {
            z[i] = Math.min(r - i, z[i - l]);    // ⚠️ 复用区间内已知值
        }
        while (i + z[i] < n && s.charAt(z[i]) == s.charAt(i + z[i]))
            z[i]++;                          // 扩展
        if (i + z[i] > r) { l = i; r = i + z[i]; }   // 更新最右区间
    }
    return z;
}
// ⚠️ 核心：i 在 [l, r) 内 → z[i] 可复用 z[i-l]（镜像）
// ⚠️ 复杂度：O(n)（r 只增不减）
// ⚠️ 面试必答："Z 算法 = 区间复用——
//   i 在最右区间内时 z[i] = min(r-i, z[i-l])，
//   保证线性 O(n)。"
```

---

## 4. KMP 与 Z 的统一视角

### 4.1 两者的关系

```
KMP 的 next[i]：前缀 i 的最长前后缀（从前看）
Z 的 z[i]：后缀 i 与全串的 LCP（从后看）

⚠️ 转换：
  next[i] 可由 Z 数组推导（前缀的最长后缀匹配）
  → 两者本质同一（"自匹配"信息的不同视角）

⚠️ 匹配应用：
  KMP：模式匹配（next 回退）
  Z：模式 + 分隔符 + 文本拼接 → Z 值 = m 即匹配

⚠️ 面试必答：
"KMP 与 Z 是'自匹配'的两种视角——
 next 从前缀看、Z 从后缀看；
 都 O(n) 构建、都利用已匹配信息。"
```

### 4.2 字符串匹配的 Z 版

```
文本匹配用 Z：
  s = pattern + '#' + text（分隔符）
  扫描 Z 数组：Z[i] == m → 匹配位置

⚠️ 面试必答：
"'模式#文本'拼接是 Z 匹配的标准姿势——
 分隔符保证 LCP 不跨串。"
```

---

## 5. next 数组的周期应用

### 5.1 最小周期

```
字符串 s 的最小周期：
  n - next[n-1]（若 n % (n - next[n-1]) == 0）

例：s = "abcabcabc"（n=9）
  next[8] = 6（"abcabc"）
  周期 = 9 - 6 = 3，9 % 3 == 0 → 周期 3 ✓

⚠️ 面试必答：
"最小周期 = n - next[n-1]——
 前提是 n 能被它整除（否则无整周期）。"
```

### 5.2 459 重复子串的 next 解

```java
/** 459. 重复的子字符串（next 求周期） */
public boolean repeatedSubstringPattern(String s) {
    int n = s.length();
    int[] next = buildNext(s);
    int period = n - next[n - 1];            // ⚠️ 最小周期
    return period != n && n % period == 0;   // 周期存在且整除
}
// 推演 "abab"：next[3]=2 → period=2，4%2==0 → true ✓
// 推演 "aba"：next[2]=1 → period=2，3%2≠0 → false ✓
// ⚠️ 面试必答："459 = next 周期公式——
//   n - next[n-1] 是最小周期，整除即重复。"
```

---

## 6. 匹配算法全景对比

### 6.1 四种匹配算法

| 算法 | 复杂度 | 确定性 | 特点 |
|------|:---:|:---:|------|
| 朴素 | O(n·m) | ✅ | 简单 |
| KMP | O(n+m) | ✅ | next 回退 |
| Rabin-Karp | O(n+m) 期望 | ⚠️ 概率 | 哈希 |
| Z | O(n+m) | ✅ | 区间复用 |

```
⚠️ 选型：
  确定性 + 单模式 → KMP/Z
  简单实现 → Rabin-Karp
  多模式 → AC 自动机（Trie + fail 指针）

⚠️ 面试必答：
"单模式确定性选 KMP/Z（O(n+m)）、
 哈希法实现简单（概率）、
 多模式用 AC 自动机（Trie + KMP 融合）。"
```

### 6.2 AC 自动机（多模式简介）

```
AC 自动机 = Trie + fail 指针（KMP 思想扩展到树）
  多模式匹配一次扫描 O(n + 总模式长)

⚠️ 面试必答：
"多模式匹配（敏感词过滤）→ AC 自动机——
 Trie 建模式、fail 指针复用失败信息、
 一次扫描全部命中。"
```

---

## 7. 面试要点与追问

### 7.1 面试话术模板

```
"字符串匹配四算法：朴素 O(nm)、
 KMP（next 回退，文本不回退，O(n+m)）、
 Rabin-Karp（哈希，概率）、Z（区间复用）。
 next[j] = 最长相等前后缀；最小周期
 = n - next[n-1]。多模式用 AC 自动机。"
```

### 7.2 高频追问 TOP 8

| # | 追问 | 标准回答 |
|:---:|------|------|
| 1 | KMP 核心？ | next 回退，文本不回退 |
| 2 | next 定义？ | 最长相等真前后缀 |
| 3 | next 构建？ | 模式匹配自己 |
| 4 | Z 数组？ | 后缀与全串 LCP |
| 5 | KMP vs Z？ | 前缀视角 vs 后缀视角 |
| 6 | 最小周期？ | n - next[n-1] |
| 7 | vs Rabin-Karp？ | 确定 vs 概率 |
| 8 | 多模式？ | AC 自动机 |

### 7.3 常见错误

| 错误 | 后果 | 修正 |
|------|------|------|
| next 数组语义混 | 回退错 | 前缀视角 |
| while 忘 j>0 | 死循环 | 回退条件 |
| 周期忘整除检查 | 误判 | n % period |
| Z 区间复用越界 | 数组越界 | min(r-i, ...) |
| 忘分隔符（Z 匹配） | 跨串 LCP | '#' 隔离 |

---

> 🎯 **核心要点**：字符串匹配 = **KMP**（next 回退）+ **Z**（区间复用）+ **Rabin-Karp**（哈希）+ **AC**（多模式）。next 数组的"自匹配"思想还延伸出周期检测（459）——"利用已匹配信息"是全部高级匹配算法的共同灵魂。

---

**上一篇**：[00-字符串匹配专题精要](00-字符串匹配专题精要.md) | **下一篇**：[03-字符串匹配高频题解](03-字符串匹配高频题解.md) | **返回总览**：[00-字符串高级算法知识体系总览](../00-字符串高级算法知识体系总览.md)
