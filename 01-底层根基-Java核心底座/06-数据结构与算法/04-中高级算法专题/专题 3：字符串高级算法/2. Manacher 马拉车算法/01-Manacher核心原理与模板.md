# 01-Manacher核心原理与模板
> 一句话：回文半径数组 + 对称性复用——# 插入统一奇偶、维护最右边界，"O(n) 求最长回文"的马拉车魔法

## 📚 目录
1. [Manacher 模型：回文的对称性](#1-manacher-模型回文的对称性)
2. [# 插入：统一奇偶回文](#2--插入统一奇偶回文)
3. [核心状态：半径数组与最右边界](#3-核心状态半径数组与最右边界)
4. [Manacher 模板（完整实现）](#4-manacher-模板完整实现)
5. [逐步推演](#5-逐步推演)
6. [识别信号与易错点](#6-识别信号与易错点)

---

## 1. Manacher 模型：回文的对称性

```text
问题: 最长回文子串（朴素 O(n²) 中心扩展 / O(n³) 暴力）
Manacher（1975）: O(n) 线性——

核心洞察: 回文的"对称性"——
  已处理回文的内部，关于中心对称的位置
  → 对称位置的回文半径可"复制"

例: "abacaba" 中心 c——左侧 "aba" 与右侧 "aba" 对称
  右侧的回文半径 ≥ 左侧的对应值（对称性复用）

⚠️ 类比: KMP 复用 border、Manacher 复用对称——
  两者都是"利用已计算信息避免重复计算"
```

> 🎯 **核心认知**："**Manacher = '回文对称性复用'——右侧半径 ≥ 左侧对称位置的半径（受最右边界限制）**。O(n) 的来源：最右边界单调右移，每个位置最多被扩展一次。"

## 2. # 插入：统一奇偶回文

### 2.1 为什么需要 # 插入

```text
回文有两种: 奇数长（"aba" 中心是字符）与偶数长（"abba" 中心是空隙）
→ 处理逻辑要分两种情况（中心扩展要写两版）

# 插入: 在字符间（含两端）插入 '#'——
  "abba" → "#a#b#b#a#"（长度 2n+1 恒为奇数）
  所有回文（奇/偶）统一为"以 # 或字符为中心的奇数回文"

⚠️ 效果: 一种处理逻辑覆盖全部——
  中心扩展只需考虑"奇数中心"一种情况

记忆: "# 插入 = 奇偶归一"
```

### 2.2 下标映射

```text
原串 s → 新串 t（插 #）:
  原下标 i → 新下标 2i+1（字符）
  半径 d（t 中）→ 原回文长度 d−1
  原回文起点 = (i − d) / 2

例: s="aba" → t="#a#b#a#"
  中心 t[3]='b'，半径 4 → 原回文 "aba" 长 3 = 4−1 ✓

⚠️ 映射公式:
  最长回文长度 = max(半径) − 1
  起点 = (中心下标 − 半径) / 2
```

> 💡 **# 插入的代价**："**# 插入让空间翻倍（2n+1）但逻辑统一——'奇偶归一'换 O(n) 实现**。半径减 1 得原长、减半得起点，两个映射公式是必背细节。"

## 3. 核心状态：半径数组与最右边界

### 3.1 两个关键变量

```text
radius[i]: 以 t[i] 为中心的回文半径（含中心本身）
  → t[i−radius[i]+1 .. i+radius[i]−1] 是回文

maxRight: 已发现回文的"最右边界"
center: 对应最右边界的回文中心

⚠️ 为什么需要 maxRight:
  对称性只在"已确认的回文内部"有效——
  超出 maxRight 的部分无法复用，必须暴力扩展

复用规则（i ≤ maxRight 时）:
  mirror = 2·center − i（i 关于 center 的对称点）
  radius[i] = min(radius[mirror], maxRight − i + 1)
  → 先取"对称值"与"边界限制"的较小者，再尝试扩展
```

### 3.2 为什么 O(n)

```text
每次扩展都会推进 maxRight（最右边界单调右移）
maxRight 最多右移 2n 次 → 总扩展量 O(n)
复用部分 O(1) → 总复杂度 O(n)

⚠️ 面试表达:
  "maxRight 单调右移——每个位置最多被暴力扩展一次，
   其余全靠对称复用 → O(n)"
```

> 🎯 **Manacher 的灵魂**："**'radius[mirror] 与 maxRight−i 取 min 再扩展'——对称复用 + 边界限制**。maxRight 单调右移是 O(n) 的证明，也是它区别于 O(n²) 中心扩展的关键。"

## 4. Manacher 模板（完整实现）

### 4.1 完整代码（5 最长回文子串）

```java
/**
 * LeetCode 5. 最长回文子串（Manacher 版）
 * 时间 O(n)，空间 O(n)
 */
public String longestPalindrome(String s) {
    // ① # 插入（奇偶归一）
    StringBuilder sb = new StringBuilder("#");
    for (char c : s.toCharArray()) {
        sb.append(c).append('#');
    }
    String t = sb.toString();
    int n = t.length();

    int[] radius = new int[n];                   // ② 半径数组
    int center = 0, maxRight = 0;                // ③ 最右边界与中心
    int bestCenter = 0, bestLen = 0;             // ④ 最优记录

    for (int i = 0; i < n; i++) {
        // ⑤ 对称复用（i 在已确认回文内）
        if (i < maxRight) {
            int mirror = 2 * center - i;
            radius[i] = Math.min(radius[mirror], maxRight - i);
        }
        // ⑥ 尝试扩展（超出复用范围的部分暴力扩展）
        int l = i - radius[i] - 1, r = i + radius[i] + 1;
        while (l >= 0 && r < n && t.charAt(l) == t.charAt(r)) {
            radius[i]++;
            l--; r++;
        }
        // ⑦ 更新最右边界
        if (i + radius[i] > maxRight) {
            maxRight = i + radius[i];
            center = i;
        }
        // ⑧ 记录最优
        if (radius[i] > bestLen) {
            bestLen = radius[i];
            bestCenter = i;
        }
    }
    // ⑨ 映射回原串: 起点 = (bestCenter − bestLen) / 2
    int start = (bestCenter - bestLen) / 2;
    return s.substring(start, start + bestLen);
}
// 推演 s="babad"：t="#b#a#b#a#d#"
//   center=3('a') 半径 4 → 回文 "aba" 长 3 = 4−1
//   或 "bab" 长 3 → 返回 "bab" ✓
// ⚠️ 模板四步: 插入 # → 对称复用 → 扩展 → 更新边界
```

### 4.2 模板要点

```text
四步: ① # 插入 ② 对称复用（min 取）③ 暴力扩展 ④ 更新 maxRight

⚠️ 与中心扩展 O(n²) 的差异:
  中心扩展: 每个中心独立扩展（O(n) 个中心 × O(n) 扩展）
  Manacher: 对称复用（O(1)）+ 扩展只发生在 maxRight 之外
```

> 🎯 **模板的节奏**："**'复用 → 扩展 → 更新边界'三步循环——复用省时间、扩展推边界、边界保对称**。会背四步模板 + 讲清 maxRight 单调，Manacher 就过了。"

## 5. 逐步推演

**s = "aba" → t = "#a#b#a#"**（n=7）

| i | t[i] | 复用 | 扩展 | radius[i] | maxRight |
|:---:|:---:|:---:|:---:|:---:|:---:|
| 0 | # | — | 无 | 1 | 1 |
| 1 | a | — | 左右 # 相等 → +1 | 2 | 3 |
| 2 | # | mirror=0 → 1 | 无 | 1 | 3 |
| 3 | b | mirror=1 → 2 | a==a → +1、#==# → +1 | 4 | 7 |
| 4 | # | mirror=2 → 1 | 无 | 1 | 7 |
| 5 | a | mirror=1 → 2 | 无（越界） | 2 | 7 |
| 6 | # | mirror=0 → 1 | 无 | 1 | 7 |

**结果**：max radius = 4（中心 3）→ 回文长度 3，起点 (3−4)/2 = 0 → "aba" ✓

```text
观察: i=4、5、6 全部复用（对称性）——
  只有 i=3 暴力扩展了 2 次 → 总扩展量 O(n)
```

## 6. 识别信号与易错点

### 6.1 识别信号

```text
"最长回文子串" → Manacher（O(n)）或中心扩展（O(n²)）
"回文子串计数" → Manacher（647 进阶版）
"任意子串是否回文" → 半径数组查询
"最短回文串" → Manacher 或 KMP（214）

⚠️ 面试定位: 5 题中心扩展 O(n²) 够用——
  Manacher 是"被追问优化"时的进阶答案
```

### 6.2 易错点

| # | 易错点 | 正确写法 | 后果 |
|:---:|------|------|------|
| 1 | 忘 # 插入 | 统一奇偶 | 偶数回文漏 |
| 2 | 复用公式错 | `min(radius[mirror], maxRight − i)` | 半径过大 |
| 3 | 扩展越界 | l ≥ 0 && r < n | 越界 |
| 4 | 忘更新 maxRight | `i + radius[i] > maxRight` 才更新 | 复用失效 |
| 5 | 原串映射错 | `(center − len) / 2` | 起点偏移 |
| 6 | 半径减 1 忘 | 原长 = radius − 1 | 长度多 1 |
| 7 | maxRight 初始 0 | center=0, maxRight=0 | 首元素特判 |

> 🎯 **核心要点**：Manacher 通关四件事——**① # 插入奇偶归一**；**② 对称复用公式**（min 取）；**③ maxRight 单调 = O(n) 证明**；**④ 映射公式**（长 = 半径−1、起点 = (c−len)/2）。**Manacher = '回文对称性复用'——'复用省时间、边界保对称'十个字，就是马拉车的全部魔法**。

---

**下一篇**：[02-Manacher应用与题解](02-Manacher应用与题解.md)
**返回总览**：[00-字符串高级算法知识体系总览](../00-字符串高级算法知识体系总览.md)
