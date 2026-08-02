# Java实现与面试实战
> 三类模板速查 + 大厂面经复盘——回溯是最容易"套模板"的题型，也是最容易踩去重坑的题型

## 📚 目录
1. [LeetCode 回溯高频题映射](#1-leetcode-回溯高频题映射)
2. [三类核心模板速查（面试默写版）](#2-三类核心模板速查面试默写版)
3. [面试解题话术](#3-面试解题话术)
4. [高频追问与标准回答](#4-高频追问与标准回答)
5. [高频题深度复盘](#5-高频题深度复盘)
6. [调试技巧与自检清单](#6-调试技巧与自检清单)

---

## 1. LeetCode 回溯高频题映射

### 1.1 全题单

| 题号 | 题目 | 题型 | 难度 | 一句话要点 | 对应模块 |
|:---:|------|------|:---:|------|:---:|
| 77 | 组合 | 组合 | 🟡 | startIndex + 范围剪枝 | [02-组合](02-组合问题全解.md) |
| 39 | 组合总和 | 组合 | 🟡 | 可重复选传 i，排序+break 剪枝 | [02-组合](02-组合问题全解.md) |
| 40 | 组合总和 II | 组合+去重 | 🟡 | 排序 + 同层去重 | [02-组合](02-组合问题全解.md) |
| 216 | 组合总和 III | 组合 | 🟡 | 双重约束剪枝 | [02-组合](02-组合问题全解.md) |
| 17 | 电话号码组合 | 组合(多串) | 🟡 | index 定位当前串 | [02-组合](02-组合问题全解.md) |
| 46 | 全排列 | 排列 | 🟡 | used 数组 | [03-排列子集](03-排列与子集问题.md) |
| 47 | 全排列 II | 排列+去重 | 🟡 | `!used[i-1]` 同层去重 | [03-排列子集](03-排列与子集问题.md) |
| 78 | 子集 | 子集 | 🟡 | 进入即收集 | [03-排列子集](03-排列与子集问题.md) |
| 90 | 子集 II | 子集+去重 | 🟡 | 排序 + `i>start` 去重 | [03-排列子集](03-排列与子集问题.md) |
| 491 | 非递减子序列 | 子集+去重 | 🟡 | 不能排序 → Set 去重 | [03-排列子集](03-排列与子集问题.md) |
| 131 | 分割回文串 | 切割 | 🟡 | 预处理回文表 O(1) 判断 | [04-切割棋盘](04-切割与棋盘问题.md) |
| 93 | 复原 IP 地址 | 切割 | 🟡 | 段合法性 + 剩余长度剪枝 | [04-切割棋盘](04-切割与棋盘问题.md) |
| 51 | N 皇后 | 棋盘 | 🔴 | 行列对角线三数组 | [04-切割棋盘](04-切割与棋盘问题.md) |
| 37 | 解数独 | 棋盘 | 🔴 | boolean 返回提前终止 | [04-切割棋盘](04-切割与棋盘问题.md) |
| 79 | 单词搜索 | 棋盘/网格 | 🟡 | visited + 方向数组 | [04-切割棋盘](04-切割与棋盘问题.md) |
| 22 | 生成括号 | 组合变形 | 🟡 | 左右括号计数约束 | — |

### 1.2 26届秋招实际手撕回溯题（面经）

```
① 47. 全排列 II    —— 去重模板题，最常考
② 77. 组合         —— startIndex 基础
③ 216. 组合总和 III —— 双重约束
④ 93. 复原 IP 地址  —— 切割 + 三重剪枝
```

---

## 2. 三类核心模板速查（面试默写版）

```java
// ═══════════ 模板一：组合（startIndex 版）═══════════
// 适用：77/39/40/216/17/131/93
void dfs(int[] nums, int start) {
    if (满足条件) { res.add(new ArrayList<>(path)); return; }
    for (int i = start; i < nums.length; i++) {
        if (剪枝条件) continue/break;
        path.add(nums[i]);
        dfs(nums, i + 1);        // 可重复选改 i
        path.remove(path.size() - 1);
    }
}

// ═══════════ 模板二：排列（used 版）══════════════════
// 适用：46/47
void dfs(int[] nums) {
    if (path.size() == nums.length) { res.add(new ArrayList<>(path)); return; }
    for (int i = 0; i < nums.length; i++) {
        if (used[i]) continue;
        if (i > 0 && nums[i] == nums[i-1] && !used[i-1]) continue;  // 去重
        used[i] = true;
        path.add(nums[i]);
        dfs(nums);
        path.remove(path.size() - 1);
        used[i] = false;         // ⚠️ 标记也要撤销
    }
}

// ═══════════ 模板三：子集（进入即收集版）══════════════
// 适用：78/90/491
void dfs(int[] nums, int start) {
    res.add(new ArrayList<>(path));   // 每个节点都收集
    for (int i = start; i < nums.length; i++) {
        if (去重条件) continue;
        path.add(nums[i]);
        dfs(nums, i + 1);
        path.remove(path.size() - 1);
    }
}
```

---

## 3. 面试解题话术

```
📣 第 1 步：识别题型 (10秒)
"题目要找出所有可能的方案/组合/排列 → 这是回溯问题。
我判断依据是：'列出所有可行解'类问题，用 DFS 穷举 + 状态回退。"

📣 第 2 步：说三要素 (30秒)
"我的状态是（当前位置 start / 剩余值 remain / 已选路径 path）。
 选择列表是 [start, n) 中未被选过的元素。
 结束条件是（path 长度 == k / remain == 0 / start == n）。"

📣 第 3 步：说模板与关键差异 (30秒)
"这是组合类问题，我用 startIndex 防止回头重复。
 关键差异：这题允许重复选 → 递归传 i；
 数组含重复元素 → 先排序，再同层去重 i>start && nums[i]==nums[i-1]。"

📣 第 4 步：说剪枝（加分项）(30秒)
"我做了剪枝：
 1) 单调剪枝——排序后 nums[i] > remain 直接 break；
 2) 范围剪枝——组合题 i 上界 = n-(k-size)+1；
 剪枝不改变最坏复杂度，但显著减少实际搜索。"
 
📣 第 5 步：说复杂度 (15秒)
"复杂度由输出规模决定：组合 C(n,k)×k，全排列 n×n!，
 子集 2ⁿ。空间 O(n)（path + 递归栈）。"

📣 第 6 步：写代码 (2-3分钟)
"我写的时候注意：收集结果时拷贝 path，递归后撤销选择。"
```

---

## 4. 高频追问与标准回答

| # | 追问 | 标准回答 |
|:---:|------|------|
| 1 | 组合和排列模板的区别？ | 组合用 startIndex（顺序无关，不回头）；排列用 used（顺序相关，从 0 遍历跳过已选） |
| 2 | 为什么收集结果要拷贝 path？ | path 是共享引用，后续递归会撤销修改，不拷贝所有结果会变成同一个最终状态 |
| 3 | 去重为什么先排序？ | 排序让相同值相邻，才能用 `nums[i]==nums[i-1]` 判断同层重复 |
| 4 | 39 为什么传 i 而不是 i+1？ | 可重复选当前元素 → 下一层从 i 开始，允许再次选它 |
| 5 | 剪枝和回溯的区别？ | 剪枝是在进入递归前终止无效分支（省调用）；回溯是穷举所有分支。剪枝不改变复杂度只减常数 |
| 6 | 回溯时间复杂度怎么算？ | 由输出规模决定：全排列 n!、子集 2ⁿ、组合 C(n,k)，再乘每解构造开销 |
| 7 | 什么时候需要 visited/used？ | 同一元素不能重复使用（排列/网格搜索）时需要；组合/子集用 start 天然防重 |
| 8 | 为什么 break 而不是 continue？ | 排序后，nums[i] > remain 时后面的元素更大，必然也 > remain → break 终止整个循环 |

---

## 5. 高频题深度复盘

### 5.1 47. 全排列 II —— 26届秋招最常考

```java
public List<List<Integer>> permuteUnique(int[] nums) {
    List<List<Integer>> res = new ArrayList<>();
    Arrays.sort(nums);
    boolean[] used = new boolean[nums.length];
    dfs(nums, used, new ArrayList<>(), res);
    return res;
}

private void dfs(int[] nums, boolean[] used,
                 List<Integer> path, List<List<Integer>> res) {
    if (path.size() == nums.length) {
        res.add(new ArrayList<>(path));
        return;
    }
    for (int i = 0; i < nums.length; i++) {
        if (used[i]) continue;
        // 核心去重：同层相同值跳过（前一个相同值未被使用说明在本层已展开过）
        if (i > 0 && nums[i] == nums[i - 1] && !used[i - 1]) continue;
        used[i] = true;
        path.add(nums[i]);
        dfs(nums, used, path, res);
        path.remove(path.size() - 1);
        used[i] = false;
    }
}
```

**复盘要点**：
- 去重三条件：`i>0`（非首元素）+ `nums[i]==nums[i-1]`（与前值相同）+ `!used[i-1]`（前值没用过）
- 面试推演时用 `nums=[1,1,2]` 跑一遍：解释第一层跳过第二个 1、但 `[1,1,...]` 路径保留第二个 1

### 5.2 39. 组合总和 —— Hot 100 高频

```java
public List<List<Integer>> combinationSum(int[] candidates, int target) {
    List<List<Integer>> res = new ArrayList<>();
    Arrays.sort(candidates);
    dfs(candidates, target, 0, new ArrayList<>(), res);
    return res;
}

private void dfs(int[] nums, int remain, int start,
                 List<Integer> path, List<List<Integer>> res) {
    if (remain == 0) {
        res.add(new ArrayList<>(path));
        return;
    }
    for (int i = start; i < nums.length; i++) {
        if (nums[i] > remain) break;   // 排序 + 单调剪枝
        path.add(nums[i]);
        dfs(nums, remain - nums[i], i, path, res);   // 传 i 可重复
        path.remove(path.size() - 1);
    }
}
```

**复盘要点**：
- 排序不仅为去重，更是单调剪枝的前提
- `nums[i] > remain` 必须 break（排序后后面更大），continue 是错误示范

### 5.3 79. 单词搜索 —— 网格回溯代表

```java
public boolean exist(char[][] board, String word) {
    for (int i = 0; i < board.length; i++)
        for (int j = 0; j < board[0].length; j++)
            if (dfs(board, word, 0, i, j)) return true;
    return false;
}

private boolean dfs(char[][] board, String word, int idx, int i, int j) {
    if (idx == word.length()) return true;
    if (i < 0 || i >= board.length || j < 0 || j >= board[0].length) return false;
    if (board[i][j] != word.charAt(idx)) return false;

    char temp = board[i][j];
    board[i][j] = '\0';                     // 原地标记 visited
    boolean found = dfs(board, word, idx+1, i+1, j)
                 || dfs(board, word, idx+1, i-1, j)
                 || dfs(board, word, idx+1, i, j+1)
                 || dfs(board, word, idx+1, i, j-1);
    board[i][j] = temp;                     // 撤销
    return found;
}
```

**复盘要点**：
- 用 `'\0'` 原地标记省 visited 数组空间（面试加分优化）
- 短路或 `||` 找到解立即返回，避免无谓搜索

---

## 6. 调试技巧与自检清单

### 6.1 回溯 Bug 自检三步法

```
第 1 步：检查撤销
  □ 每个 path.add 后是否都有 path.remove？
  □ used/visited 标记是否也撤销了？
  □ StringBuilder 场景用 setLength 或 deleteCharAt 恢复？

第 2 步：检查收集
  □ res.add(new ArrayList<>(path)) —— 是否拷贝？
  □ 收集时机对不对（满足条件才收集 vs 进入即收集）？

第 3 步：检查去重
  □ 去重前提排序了吗？
  □ 同层去重条件（i>start）还是同路径（i>0）？
  □ 排列去重带 !used[i-1] 吗？
```

### 6.2 高频 Bug 速查表

| Bug | 原因 | 修复 |
|------|------|------|
| 结果全是空列表 | 没拷贝 path，或拷贝位置错误 | `res.add(new ArrayList<>(path))` |
| 结果大量重复 | 未排序或去重条件写错 | 排序 + `i>start && nums[i]==nums[i-1]` |
| 死循环/栈溢出 | 递归参数没变（传 i 与 i+1 搞混） | 检查可重复选传 i、不可重复传 i+1 |
| 遗漏解 | 剪枝条件过严 | 用 `i>start` 而非 `i>0`（保留同路径重复） |
| 结果顺序错乱 | 撤销顺序不对 | 做选择和撤销严格对称 |
| N皇后结果空 | 对角线编号越界 | d2 = row-col+n-1 偏移防负 |

### 6.3 面试手写 Checklist

```
□ 先说题型判断（列出所有方案 → 回溯）
□ 说清三要素（path / 选择列表 / 结束条件）
□ 组合用 start，排列用 used，没说混
□ 有重复元素先排序 + 去重条件正确
□ 剪枝写在选择前
□ 收集结果拷贝 path
□ 撤销完整（path + used）
□ 说出复杂度（输出规模决定）
□ 手动跑 1 个用例
```

> 🎯 **核心要点**：回溯是面试中"投入产出比"最高的题型——三类模板背熟，识别题型后 30 秒内就能给出完整方案。最大的失分点永远是**去重和撤销**：写代码时心里默念"排序去重、拷贝收集、对称撤销"三句话，回溯题稳拿分。

---

**返回总览**：[00-回溯算法知识体系总览](00-回溯算法知识体系总览.md)
