# 03-排列问题：used数组与去重
> 一句话：排列从 0 遍历 + used[] 标记已选——去重的关键在"同层 vs 同路径"：`!used[i-1]` 是标准答案，`used[i-1]` 是经典陷阱

## 📚 目录
1. [排列模型：used[] 与从 0 遍历](#1-排列模型used-与从-0-遍历)
2. [46 全排列（回顾与深潜）](#2-46-全排列回顾与深潜)
3. [47 全排列 II（排序 + 同层去重）](#3-47-全排列-ii排序--同层去重)
4. [去重核心：!used[i-1] vs used[i-1]](#4-去重核心usedi-1-vs-usedi-1)
5. [剑指 38 字符串的排列](#5-剑指-38-字符串的排列)
6. [排列家族与变体](#6-排列家族与变体)
7. [排列问题易错点](#7-排列问题易错点)

---

## 1. 排列模型：used[] 与从 0 遍历

```text
排列 vs 组合的本质区别:
  组合: 顺序无关（[1,2] ≡ [2,1]）→ startIndex 防重复
  排列: 顺序敏感（[1,2] ≠ [2,1]）→ 每层从 0 遍历 + used 标记

排列的选择列表 = 所有"未使用"的元素:
  used[i] = true → 已选 → 跳过
  每层遍历 0..n-1（不像组合从 start 开始）

结果数: n!（n=10 → 362 万；n=12 → 4.79 亿——注意爆炸）

⚠️ 复杂度: O(n·n!)，空间 O(n)（递归深度 + path + used）
```

> 🎯 **核心认知**："**排列的'从 0 遍历'是'顺序敏感'的直接体现——每个位置都能选任何未用的元素**；used[] 是它的'已选账本'，与组合的 startIndex（'起点账本'）形成对照。"

## 2. 46 全排列（回顾与深潜）

### 2.1 完整实现（01 文件母题）

```java
/**
 * LeetCode 46. 全排列
 * used[] 标记 + 从 0 遍历
 * 时间 O(n·n!)，空间 O(n)
 */
public List<List<Integer>> permute(int[] nums) {
    List<List<Integer>> result = new ArrayList<>();
    boolean[] used = new boolean[nums.length];
    backtrack(nums, used, new ArrayList<>(), result);
    return result;
}

private void backtrack(int[] nums, boolean[] used,
                       List<Integer> path, List<List<Integer>> result) {
    if (path.size() == nums.length) {            // ① 结束: 长度 = n
        result.add(new ArrayList<>(path));
        return;
    }
    for (int i = 0; i < nums.length; i++) {      // ② 从 0 遍历（排列标志）
        if (used[i]) continue;                   // ③ 已选跳过
        used[i] = true;                          // ④ 选 + 标记
        path.add(nums[i]);
        backtrack(nums, used, path, result);     // ⑤ 递
        path.remove(path.size() - 1);            // ⑥ 回（撤销选择）
        used[i] = false;                         //    撤销标记！
    }
}
// 推演 [1,2,3]：3×2×1 = 6 个排列 ✓
// ⚠️ used 的撤销与 path 的撤销必须成对——漏一个即状态污染
```

> 💡 **used 撤销的成对性**："**`used[i] = true` 与 `used[i] = false` 必须对称出现**——这与 path 的 add/remove 是同一对'选/回'动作的两个方面。写代码时养成'选什么就回什么'的习惯。"

## 3. 47 全排列 II（排序 + 同层去重）

### 3.1 题目与思路

**题**：含重复元素的数组的全排列（结果不重复）。

**排序 + `!used[i-1]` 同层去重**：

```java
/**
 * LeetCode 47. 全排列 II
 * 排序 + 同层去重（!used[i-1]）
 * 时间 O(n·n!)，空间 O(n)
 */
public List<List<Integer>> permuteUnique(int[] nums) {
    Arrays.sort(nums);                           // ① 排序（去重前提）
    List<List<Integer>> result = new ArrayList<>();
    boolean[] used = new boolean[nums.length];
    backtrack(nums, used, new ArrayList<>(), result);
    return result;
}

private void backtrack(int[] nums, boolean[] used,
                       List<Integer> path, List<List<Integer>> result) {
    if (path.size() == nums.length) {
        result.add(new ArrayList<>(path));
        return;
    }
    for (int i = 0; i < nums.length; i++) {
        if (used[i]) continue;                   // ② 已选跳过
        // ③ ⚠️ 同层去重: 前一个相同值"未使用" → 跳过（同层重复）
        if (i > 0 && nums[i] == nums[i - 1] && !used[i - 1]) continue;
        used[i] = true;
        path.add(nums[i]);
        backtrack(nums, used, path, result);
        path.remove(path.size() - 1);
        used[i] = false;
    }
}
// 推演 [1,1,2]：排序 [1,1,2] → 结果 3 个（非 6 个）：
//   [1,1,2] [1,2,1] [2,1,1] ✓
// ⚠️ 为什么 !used[i-1]（前一个未用）:
//   同一层中，前一个相同值已回溯完（未用）→ 当前是重复起点 → 跳过
//   前一个正在用（used）→ 说明是同一路径的合法重复（[1,1,2] 的两个 1）
```

> 🎯 **47 的面试价值**："**`!used[i-1]` 是排列去重的标准答案——'前一个相同值未使用 = 同层重复 = 跳过'**。能解释'为什么不是 used[i-1]'（那会误删同一路径的合法重复），排列去重就满分了。"

## 4. 去重核心：!used[i-1] vs used[i-1]

### 4.1 两种写法的语义对比

```text
写法一: !used[i-1]（✅ 正确 — 同层去重）
  前一个相同值"未使用" → 说明它刚被回溯完（同层）→ 当前是重复起点
  效果: 同一层只选第一个重复值 → 结果不重复

写法二: used[i-1]（❌ 错误 — 同路径去重）
  前一个相同值"正在使用" → 跳过 → 会误删同一路径的合法重复
  例: [1a,1b,2] 中路径 [1a,1b,2]——
    选 1b 时前一个 1a 正被使用 → 被跳过 → [1a,1b,2] 永远无法生成 → 漏解！

⚠️ 记忆口诀:
  "去重看'前一个用完没有'——用完（!used）说明同层，跳过；
   正在用（used）说明同路径，保留！"
```

### 4.2 与组合去重的对照

| 题型 | 去重条件 | 语义 |
|------|------|------|
| 组合/子集（40/90） | `i > start && nums[i] == nums[i-1]` | 同层跳过重复起点 |
| 排列（47） | `!used[i-1]` | 前一个用完 = 同层 = 跳过 |
| 共同前提 | 先排序 | 重复元素相邻才能判定 |

> 🎯 **去重统一认知**："**所有去重的本质都是'同一层只取第一个重复值'——组合用 `i > start` 表达、排列用 `!used[i-1]` 表达，语义完全相同**。'同层去重、跨层保留'八个字是所有去重题的答案。"

## 5. 剑指 38 字符串的排列

### 5.1 题目与思路

**题**：字符串的所有排列（含重复字符，结果不重复，字典序）。

**47 的字符串版**：

```java
/**
 * 剑指 Offer 38. 字符串的排列
 * 排序 + used 去重 + 字符数组（与 47 同构）
 * 时间 O(n·n!)，空间 O(n)
 */
public String[] permutation(String s) {
    char[] chars = s.toCharArray();
    Arrays.sort(chars);                          // ① 排序（去重前提）
    List<String> result = new ArrayList<>();
    boolean[] used = new boolean[chars.length];
    backtrack(chars, used, new StringBuilder(), result);
    return result.toArray(new String[0]);
}

private void backtrack(char[] chars, boolean[] used,
                       StringBuilder path, List<String> result) {
    if (path.length() == chars.length) {
        result.add(path.toString());
        return;
    }
    for (int i = 0; i < chars.length; i++) {
        if (used[i]) continue;
        if (i > 0 && chars[i] == chars[i - 1] && !used[i - 1]) continue;  // ② 同层去重
        used[i] = true;
        path.append(chars[i]);
        backtrack(chars, used, path, result);
        path.deleteCharAt(path.length() - 1);    // ③ StringBuilder 撤销
        used[i] = false;
    }
}
// 推演 "abc" → 6 个排列 ✓；"aab" → 3 个（去重后）✓
// ⚠️ 数组版 path 用 ArrayList、字符串版用 StringBuilder——
//   撤销动作对应不同（remove vs deleteCharAt）
```

> 💡 **载体迁移**："**47 与剑指 38 完全同构——只是 path 载体从 ArrayList 换成 StringBuilder**。'会 47 就会剑指 38'是排列家族的常见面试考察方式（换载体考原题）。"

## 6. 排列家族与变体

| 题 | 变形 | 新要素 |
|:---:|------|------|
| 46 全排列 | 母题 | used[] 基础 |
| 47 全排列 II | 重复元素 | 排序 + !used[i-1] |
| 剑指 38 字符串排列 | 字符串载体 | StringBuilder 撤销 |
| 784 字母大小写全排列 | 字符变换 | 每字符两种选择（大小写） |
| 526 优美的排列 | 约束排列 | 位置条件剪枝 |
| 996 正方形数组排列 | 相邻约束 | 排序去重 + 相邻和检查 |

```text
⚠️ 变体识别:
  "排列" + 无重复 → 46 模板
  "排列" + 有重复 → 47 模板（排序 + !used[i-1]）
  "排列" + 约束条件 → 46/47 模板 + 剪枝
  "大小写变换" → 每字符 2 选（类似 17 多集合）
```

## 7. 排列问题易错点

| # | 易错点 | 正确写法 | 后果 |
|:---:|------|------|------|
| 1 | 排列用 startIndex | 从 0 遍历 + used | 漏掉大量排列 |
| 2 | 忘重置 used | 递归后 `used[i] = false` | 分支全错 |
| 3 | 去重用 `used[i-1]` | 用 `!used[i-1]` | 漏解（[1a,1b,2] 被删） |
| 4 | 忘排序直接去重 | 先 Arrays.sort | 去重失效 |
| 5 | 字符串版用 ArrayList | StringBuilder + deleteCharAt | 载体不匹配 |
| 6 | used 与 path 撤销不对称 | 成对出现 | 状态污染 |
| 7 | 结果数公式记错 | n!（非 2ⁿ） | 复杂度分析错 |

> 🎯 **核心要点**：排列通关四件事——**① 从 0 遍历 + used[]**（排列标志）；**② `!used[i-1]` 同层去重**（'前一个用完 = 同层'语义）；**③ 与组合去重对照**（`i > start` vs `!used[i-1]` 同义不同形）；**④ 载体迁移**（47 → 剑指 38 的 StringBuilder 版）。**排列家族 = 46 模板 + 一行去重 + 剪枝——'同层去重、跨层保留'八个字，就是去重题的最终答案**。

---

**下一模块**：[04-切割与棋盘问题](04-切割与棋盘问题.md)
**返回总览**：[00-回溯知识体系总览](00-回溯知识体系总览.md)
