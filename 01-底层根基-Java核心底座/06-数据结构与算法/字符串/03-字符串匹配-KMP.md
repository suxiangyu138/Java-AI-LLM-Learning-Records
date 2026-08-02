# 字符串匹配 KMP
> 主串指针永不回溯——KMP 算法从 next 数组到完整实现，O(n+m) 的经典优化

## 📚 目录
1. [暴力匹配的问题](#1-暴力匹配的问题)
2. [KMP 的核心思想](#2-kmp-的核心思想)
3. [next 数组（LPS）构建](#3-next-数组lps构建)
4. [28. 找出字符串中第一个匹配项的下标](#4-28-找出字符串中第一个匹配项的下标)
5. [KMP 的应用扩展](#5-kmp-的应用扩展)

---

## 1. 暴力匹配的问题

### 1.1 暴力匹配（Naive）

```java
/**
 * 暴力匹配：主串每个位置尝试匹配模式串
 * 时间复杂度 O(n×m)：最坏情况大量回溯
 */
public int naiveMatch(String text, String pattern) {
    int n = text.length(), m = pattern.length();
    for (int i = 0; i <= n - m; i++) {
        int j = 0;
        while (j < m && text.charAt(i + j) == pattern.charAt(j)) {
            j++;
        }
        if (j == m) return i;          // 匹配成功
    }
    return -1;
}
```

### 1.2 最坏情况分析

```
text    = "AAAAAAB"
pattern = "AAAB"

暴力匹配过程：
  i=0: AAAA 匹配到第 4 个 A 时失败 → 回到 i=1 重新匹配
  i=1: 又匹配到第 4 个 A 失败 → 回到 i=2...
  → 大量重复比较（每步都从头开始）

问题根源：主串指针 i 反复回溯，已经比较过的字符被重复比较

最坏 O(n×m)：
  text = "AAAA...A"（n 个 A）
  pattern = "AAA...B"（m-1 个 A + B）
  每次匹配到最后一个字符才失败 → n×m 次比较
```

---

## 2. KMP 的核心思想

### 2.1 三个关键洞察

```
① 主串指针不回溯！
   匹配失败时，主串指针 i 不动，只移动模式串指针 j

② 移动多少由模式串自身决定
   利用"已经匹配部分"的**最长公共前后缀**
   → 失败时跳过不可能匹配的位置

③ 预处理模式串 → next 数组
   避免每次失败都重新计算 → 空间换时间

记忆：KMP = 预处理（next 数组）+ 单次扫描（主串）
```

### 2.2 图解：为什么不用回溯？

```
text    = "ABCABD..."
pattern = "ABCABC"

匹配到位置 5 失败：
  text : A B C A B D
  pattern: A B C A B C
                  ↑ 失配

已经匹配的部分是 "ABCAB"
  "ABCAB" 的最长公共前后缀 = "AB"（长度 2）
  → 模式串可以直接把前缀 "AB" 挪到后缀 "AB" 的位置
  → j 从 5 跳到 2（不用从 0 开始！）
  → 主串指针 i 不动 ✓

text : A B C A B D
pattern:       A B C A B C
              j=2 → 继续比较 text[i] 与 pattern[2]
```

### 2.3 最长公共前后缀

```
"ABCAB" 的前缀：A, AB, ABC, ABCA
"ABCAB" 的后缀：B, AB, CAB, BCAB
公共前后缀：A, AB → 最长的是 "AB"（长度 2）

next[j] = 前 j 个字符的最长公共前后缀长度
next[5] = 2（"ABCAB" 的前缀 "AB" 也是它的后缀）
```

---

## 3. next 数组（LPS）构建

### 3.1 递推构建

```java
/**
 * 构建 next 数组（最长公共前后缀表）
 * next[i] = pattern[0..i-1] 的最长公共前后缀长度
 *
 * 递推逻辑：
 *   两个指针：i 扫描，j 记录当前最长前后缀长度
 *   ① pattern[i] == pattern[j] → j+1，next[i+1] = j+1
 *   ② 不相等且 j > 0 → j = next[j] 回退（⚠️ 用 while 不是 if！）
 *   ③ j == 0 → next[i+1] = 0
 */
public int[] buildNext(String pattern) {
    int m = pattern.length();
    int[] next = new int[m];
    int j = 0;                              // 最长公共前后缀长度

    for (int i = 1; i < m; i++) {
        // ⚠️ 回退用 while（可能多次回退）
        while (j > 0 && pattern.charAt(i) != pattern.charAt(j)) {
            j = next[j];                    // 回退到更短的前后缀
        }
        if (pattern.charAt(i) == pattern.charAt(j)) {
            j++;                            // 匹配 → 长度+1
        }
        next[i] = j;                        // 记录
    }
    return next;
}
// pattern = "ABABCABAB"
// next = [0, 0, 1, 2, 0, 1, 2, 3, 4]
//        A  B  A  B  C  A  B  A  B
```

### 3.2 推演：pattern = "ABAB"

```
i=1(B): j=0, B≠A → next[1]=0
i=2(A): j=0, A==A → j=1 → next[2]=1
i=3(B): j=1, B==B → j=2 → next[3]=2
next = [0, 0, 1, 2]
  next[3]=2：前 3 个 "ABA" 的最长公共前后缀 = "A"？不，是 1
  等等，next[3] 应该对应 pattern[0..2]="ABA" → 前后缀 "A" → 1？
  重新推演：
  next[i] 定义 = pattern[0..i] 的最长公共前后缀
  实际上我的循环里 next[i] 在 i 位置赋值，对应 pattern[0..i]
  标准定义 next[i] = pattern[0..i-1] 的 LCP
  两种版本都常见，实现一致即可
```

> 💡 **注意**：next 数组有两种主流定义（含自身/不含自身），不同教材可能不同。面试时**先说明自己的定义**再写代码，避免歧义。

---

## 4. 28. 找出字符串中第一个匹配项的下标

### 4.1 完整实现（KMP 匹配）

```java
/**
 * LeetCode 28. 找出字符串中第一个匹配项的下标（KMP 经典应用）
 * haystack = "sadbutsad", needle = "sad" → 0
 *
 * 匹配过程（主串不回溯）：
 *   i 扫描主串，j 扫描模式串
 *   ① 相等 → i++, j++
 *   ② 不相等且 j > 0 → j = next[j-1]（模式串回退）
 *   ③ j == m → 匹配成功
 */
public int strStr(String haystack, String needle) {
    int n = haystack.length(), m = needle.length();
    if (m == 0) return 0;
    if (n < m) return -1;

    int[] next = buildNext(needle);
    int j = 0;

    for (int i = 0; i < n; i++) {
        // 失配 → 模式串回退（while 可能多次回退）
        while (j > 0 && haystack.charAt(i) != needle.charAt(j)) {
            j = next[j - 1];
        }
        if (haystack.charAt(i) == needle.charAt(j)) {
            j++;
        }
        if (j == m) {
            return i - m + 1;            // 匹配成功，返回起始位置
        }
    }
    return -1;
}

// 也可用 JDK 自带：haystack.indexOf(needle)
// （面试手写 KMP 展示算法能力，indexOf 作为补充说明）
```

### 4.2 匹配过程推演

```
haystack = "ABABDABAC", needle = "ABABAC"
next = [0, 0, 1, 2, 3, 0]（needle: A B A B A C）

i=0..4: A B A B A 全部匹配 → j=5
i=5(D): D ≠ C → 回退 j = next[4] = 3
  继续比较 D vs pattern[3]=B → 不等 → 回退 j = next[2] = 1
  D vs pattern[1]=B → 不等 → 回退 j = next[0] = 0
  D vs pattern[0]=A → 不等，j=0
i=6(A): A == A → j=1
i=7(B): B == B → j=2
i=8(A): A == A → j=3
...
主串指针 i 从 0 走到 8 只扫描一遍 → O(n) ✓
```

### 4.3 复杂度分析

```
预处理：O(m)（next 数组构建）
匹配：O(n)（主串指针不回溯）
总复杂度：O(n + m)

空间：O(m)（next 数组）

对比暴力 O(n×m)：
  KMP 的核心价值是主串指针永不回溯
  → 文本流场景（一次读取无法回退）特别适用
```

---

## 5. KMP 的应用扩展

### 5.1 变体题速查

| 题号 | 题目 | KMP 应用 |
|:---:|------|------|
| 28 | strStr | 标准 KMP 匹配 |
| 686 | 重复字符串匹配 | 拼接后 KMP |
| 214 | 最短回文串 | 逆序拼接求 next |
| 459 | 重复的子字符串 | 拼接自身 + KMP 找周期 |
| 1392 | 最长快乐前缀 | next 数组直接取 |

### 5.2 459. 重复的子字符串（next 应用）

```java
/**
 * LeetCode 459. 重复的子字符串
 * 判断 s 是否可以由子串重复构成
 * "abab" → true（"ab"×2）
 *
 * 技巧：s + s 去掉首尾后是否包含 s
 *   或者用 KMP next：s.length() % (n - next[n-1]) == 0
 *   （n - 最长公共前后缀 = 最小周期）
 */
public boolean repeatedSubstringPattern(String s) {
    int n = s.length();
    int[] next = buildNext(s);
    int period = n - next[n - 1];      // 最小周期长度
    return period < n && n % period == 0;
}
// "abab"：next[3]=2 → period = 4-2 = 2 → 4%2==0 → true ✓
// "aba"：next[2]=1 → period = 3-1 = 2 → 3%2≠0 → false ✓
```

### 5.3 面试定位

```
KMP 面试定位（2025-2026 趋势）：
  "思想要懂，手写看公司"
  - 大厂算法面（字节/腾讯）：可能要求手撕 KMP 或至少讲清 next 数组
  - 中小厂：知道"避免主串回溯"的核心思想即可
  - 熟练背诵：next 构建（while 回退）+ 匹配循环

面试话术：
"KMP 的核心是主串指针不回溯——预处理模式串
 得到 next 数组（最长公共前后缀），失配时
 模式串直接跳到后缀匹配位置。
 时间复杂度 O(n+m)，空间 O(m)。"
```

> 🎯 **核心要点**：KMP 是字符串匹配的经典优化——**预处理 next 数组（最长公共前后缀）+ 主串不回溯**。next 构建的"while 回退"是最大难点（不能用 if），28 题是标准应用。面试先说思想（为什么主串不回溯），再手写 next 与匹配两个循环。O(n+m) 对比暴力 O(n×m) 的论证要能脱口而出。

---

**下一模块**：[04-Manacher与滚动哈希](04-Manacher与滚动哈希.md) | **返回总览**：[00-字符串知识体系总览](00-字符串知识体系总览.md)
