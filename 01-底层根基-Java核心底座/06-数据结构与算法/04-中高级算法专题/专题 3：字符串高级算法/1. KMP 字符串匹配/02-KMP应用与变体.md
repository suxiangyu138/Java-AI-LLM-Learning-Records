# 02-KMP应用与变体
> 一句话：686 重复叠加匹配、459 重复子串、循环节判定——KMP 三变体全解，"border 整除性"是隐藏考点

## 📚 目录
1. [686 重复叠加字符串匹配](#1-686-重复叠加字符串匹配)
2. [459 重复的子字符串（循环节）](#2-459-重复的子字符串循环节)
3. [28 最短回文串（KMP 应用）](#3-28-最短回文串kmp-应用)
4. [循环节判定：next 的隐藏考点](#4-循环节判定next-的隐藏考点)
5. [KMP 家族总览](#5-kmp-家族总览)
6. [易错点与面试要点](#6-易错点与面试要点)

---

## 1. 686 重复叠加字符串匹配

### 1.1 题目与思路

**题**：重复叠加 a 使 b 成为其子串，求最小叠加次数。

**KMP + 边界分析**：

```java
/**
 * LeetCode 686. 重复叠加字符串匹配
 * 思路: 重复 a 直到长度 ≥ b，最多多叠一次（边界）
 * KMP 匹配 + 叠加次数统计
 */
public int repeatedStringMatch(String a, String b) {
    // ① 重复次数边界: ceil(b.len / a.len) 或 +1
    int repeat = (b.length() + a.length() - 1) / a.length();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < repeat; i++) sb.append(a);

    // ② KMP 匹配（复用 01 模板）
    int index = kmp(sb.toString(), b);
    if (index != -1) return repeat;

    // ③ 边界: 可能要多叠一次（b 跨 a 的边界）
    sb.append(a);
    return kmp(sb.toString(), b) != -1 ? repeat + 1 : -1;
}
// 推演 a="abcd", b="cdabcdab"：
//   repeat = ceil(8/4) = 2 → "abcdabcd" 不含 → 叠 3 次
//   "abcdabcdabcd" 含 "cdabcdab" ✓ → 3
// ⚠️ 边界: b 可能跨 a 的拼接边界——最多需要 repeat+1 次
```

> 🎯 **686 的边界**："**'重复到长度够 + 最多多叠一次'——b 跨 a 边界的场景是 686 的隐藏边界**。KMP 匹配是手段、次数边界分析是考点。"

## 2. 459 重复的子字符串（循环节）

### 2.1 题目与思路

**题**：判断字符串是否由某个子串重复构成。

**循环节判定**：`len − next[len−1]` 是循环节长度 ⇔ 整除：

```java
/**
 * LeetCode 459. 重复的子字符串
 * 核心: 最长 border 决定循环节——
 *   len − next[len−1] = 循环节长度（可整除则重复构成）
 */
public boolean repeatedSubstringPattern(String s) {
    int n = s.length();
    int[] next = buildNext(s);                   // ① KMP 构建（自我匹配）
    int border = next[n - 1];                    // ② 最长 border
    int cycle = n - border;                      // ③ 循环节长度

    return border > 0 && n % cycle == 0;         // ④ border > 0 且整除
}
// 推演 "abab"：next[3] = 2 → cycle = 2 → 4%2==0 → true ✓
//   "aba"：next[2] = 1 → cycle = 2 → 3%2≠0 → false ✓
//   "abcabcabc"：next[8] = 6 → cycle = 3 → 9%3==0 → true ✓
// ⚠️ 为什么: border 是"前缀=后缀"——
//   重复串的 border = 去掉最后一个循环节；cycle = 循环节长度
```

> 🎯 **459 的隐藏考点**："**'len − next[len−1] = 循环节长度'是 KMP 的经典隐藏结论**——重复串的最长 border 恰是'去掉最后一个循环节'。能现场推导 border/cycle 关系，459 就是送分题。"

## 3. 214 最短回文串（KMP 应用）

### 3.1 题目与思路

**题**：在 s 前加字符使其成为回文，求最短结果。

**KMP 视角**：找 s 的"最长回文前缀"——`s + '#' + reverse(s)` 的 border：

```java
/**
 * LeetCode 214. 最短回文串
 * 核心: 拼接 s#rev(s)，其最长 border = s 的最长回文前缀
 * 剩余部分反转加到前面
 */
public String shortestPalindrome(String s) {
    String rev = new StringBuilder(s).reverse().toString();
    String combined = s + "#" + rev;             // ① # 分隔（防 border 跨接）

    int[] next = buildNext(combined);            // ② KMP 构建
    int border = next[combined.length() - 1];    // ③ 最长 border = 回文前缀长

    String suffix = rev.substring(0, s.length() - border);  // ④ 多余部分
    return suffix + s;                           // ⑤ 前置反转 → 拼接
}
// 推演 s="aacecaaa"：
//   combined = "aacecaaa#aaacecaa"
//   最长 border = 7（"aacecaa" 是回文前缀）
//   suffix = "aaacecaa"[0..0] = "a" → "a" + "aacecaaa" = "aaacecaaa" ✓
// ⚠️ 为什么: border 的前缀 = 后缀 → 前缀是回文（s 的前缀 == rev 的后缀）
//   # 分隔防止 border 跨越 s 与 rev 的边界
```

> 💡 **KMP × 回文的组合**："**214 = '拼接 + KMP border'——s#rev 的最长 border 就是 s 的最长回文前缀**。Manacher 也能解，KMP 版展示 border 的另一种用途。"

## 4. 循环节判定：next 的隐藏考点

### 4.1 公式与证明

```text
定理: 字符串 s 由循环节构成 ⇔
  n % (n − next[n−1]) == 0 且 next[n−1] > 0

证明直觉:
  重复串 "abcabcabc" 的 border = "abcabc"（6）——
  "去掉最后一个循环节"就是最长 border
  cycle = n − border = 3 = 循环节长度
  整除 → 可重复构成

⚠️ 推论: 最小循环节长度 = n − next[n−1]
  例: "abcabcabc" → 3；"abab" → 2；"aaa" → 1

⚠️ 应用: 459、686 的周期判断、字符串周期性问题
```

### 4.2 面试表达

```text
"KMP 的 next 数组不只用于匹配——
  n − next[n−1] 是字符串的最小循环节长度。
  459 重复子串判定 = border > 0 且 n % cycle == 0。"

⚠️ 这是 KMP 面试的进阶加分点——
  会匹配 + 会循环节 = 对 next 有深层理解
```

## 5. KMP 家族总览

| 题 | 应用 | 核心 |
|:---:|:---:|:---:|
| 28 匹配 | 单模式匹配 | 主流程 |
| 686 叠加 | 重复 + 边界 | 次数分析 |
| 459 重复子串 | 循环节 | border 整除 |
| 214 最短回文 | 回文前缀 | 拼接 border |
| 1392 最长快乐前缀 | 最长 border | next 直接应用 |
| 459 变体（周期串） | 周期判定 | cycle = n − border |

```text
⚠️ 家族规律:
  "匹配（28）→ 周期（459）→ 回文（214）——
   KMP 的 next 数组从'匹配工具'升级为'结构分析工具'"
```

## 6. 易错点与面试要点

### 6.1 易错点

| # | 易错点 | 正确写法 | 后果 |
|:---:|------|------|------|
| 1 | 686 忘多叠一次 | 最多 repeat+1 | 跨边界漏解 |
| 2 | 459 忘 border > 0 | `next[n−1] > 0` 前置 | "a" 误判 true |
| 3 | 459 循环节算错 | `n − next[n−1]` | 周期错 |
| 4 | 214 忘 # 分隔 | s + "#" + rev | border 跨接错 |
| 5 | 214 后缀截取错 | `rev.substring(0, n−border)` | 拼接错 |
| 6 | 686 匹配用暴力 | KMP | 超时 |
| 7 | next 数组复用未重建 | 每次新构建 | 数据污染 |

### 6.2 面试要点

```text
① 主流程先默写: "构建 next + i 前进 j 回退"
② border 语义讲清: "最长公共前后缀——失配复用"
③ 循环节加分: "n − next[n−1] = 循环节长度"
④ 复杂度: O(n+m) 时间、O(m) 空间

⚠️ 高频追问: "为什么主串指针不回退？"
  → "已匹配部分的 border 前缀已经匹配——
    j 回退到 border 长度即可继续，
    主串无需回头（i 只前进）"
```

> 🎯 **核心要点**：KMP 应用通关——**28（主流程）+ 686（边界分析）+ 459（循环节）+ 214（回文前缀）**。四题覆盖 KMP 的"匹配 → 周期 → 结构"三层应用——"border 不只是失配跳转，更是字符串的结构指纹"。

---

**上一篇**：[01-KMP核心原理与next数组](01-KMP核心原理与next数组.md)
**返回总览**：[00-字符串高级算法知识体系总览](../00-字符串高级算法知识体系总览.md)
