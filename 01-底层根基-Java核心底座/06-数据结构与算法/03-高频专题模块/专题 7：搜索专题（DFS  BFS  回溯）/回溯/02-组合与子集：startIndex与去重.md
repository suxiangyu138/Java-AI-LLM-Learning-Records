# 02-组合与子集：startIndex与去重
> 一句话：startIndex 控制顺序（可重复选传 i、不可重复传 i+1）、排序 + 同层去重治重复元素——组合家族五题一个模板

## 📚 目录
1. [组合家族全景：startIndex 的四种形态](#1-组合家族全景startindex-的四种形态)
2. [39 组合总和（可重复选，传 i）](#2-39-组合总和可重复选传-i)
3. [40 组合总和 II（排序 + 去重）](#3-40-组合总和-ii排序--去重)
4. [90 子集 II（去重子集）](#4-90-子集-ii去重子集)
5. [216 组合总和 III 与 17 电话号码](#5-216-组合总和-iii-与-17-电话号码)
6. [去重核心：同层 vs 同路径](#6-去重核心同层-vs-同路径)
7. [组合家族易错点](#7-组合家族易错点)

---

## 1. 组合家族全景：startIndex 的四种形态

```text
组合家族（30%+ 面试占比）的五种变形:
  77 组合（取 k 个）: startIndex + 结束（长度 k）
  39 组合总和（可重复选）: 递归传 i（不是 i+1！）
  40 组合总和 II（不可重复 + 重复元素）: 传 i+1 + 排序去重
  216 组合总和 III（定长 + 和约束）: 传 i+1 + 和剪枝
  17 电话号码（多集合组合）: 字符串映射 + 回溯

startIndex 的本质:
  控制"下一层的起点"——防止同一组合的不同顺序重复出现
  ⚠️ 传 i vs 传 i+1:
    可重复选（39）→ 传 i（当前元素还能再用）
    不可重复（40/77/216）→ 传 i+1（当前元素用完即弃）
```

> 🎯 **核心认知**："**startIndex 的'传 i 还是 i+1'是组合家族的第一分水岭**——'可重复选'传 i、'不可重复'传 i+1，一字之差决定全部结果。记住这个，组合五题就通了一半。"

## 2. 39 组合总和（可重复选，传 i）

### 2.1 题目与思路

**题**：候选数字（可重复使用）中和 = target 的组合（不重复组合）。

**传 i 允许重复** + **排序 + 和剪枝**：

```java
/**
 * LeetCode 39. 组合总和
 * ⚠️ 可重复选 → 递归传 i（不是 i+1）
 * 排序 + sum > target 剪枝（break）
 * 时间 O(指数级)，空间 O(target/min)
 */
public List<List<Integer>> combinationSum(int[] candidates, int target) {
    Arrays.sort(candidates);                     // ① 排序（剪枝前提）
    List<List<Integer>> result = new ArrayList<>();
    backtrack(candidates, target, 0, 0, new ArrayList<>(), result);
    return result;
}

private void backtrack(int[] nums, int target, int start, int sum,
                       List<Integer> path, List<List<Integer>> result) {
    if (sum == target) {                         // ② 和达标
        result.add(new ArrayList<>(path));
        return;
    }
    for (int i = start; i < nums.length; i++) {
        if (sum + nums[i] > target) break;       // ③ 剪枝: 排序后更大必超
        path.add(nums[i]);                       // ④ 选
        backtrack(nums, target, i, sum + nums[i], path, result);  // ⑤ ⚠️ 传 i（可重复！）
        path.remove(path.size() - 1);            // ⑥ 回
    }
}
// 推演 [2,3,6,7], target=7 → [[2,2,3],[7]] ✓
//   [2,3,5], target=8 → [[2,2,2,2],[2,3,3],[3,5]] ✓
// ⚠️ 传 i 的效果: 选 2 后还能再选 2（[2,2,3] 合法）
//   传 i+1 的效果: 每元素最多一次（那是 40 题的语义）
```

> 💡 **为什么排序 + break 是安全的**："**排序后 'sum + nums[i] > target' 时，后续元素更大也必超**——break 比 continue 更强（跳过整层）。剪枝不减正确性，只减搜索树。"

## 3. 40 组合总和 II（排序 + 去重）

### 3.1 题目与思路

**题**：候选有**重复元素**且**每个只能用一次**，和 = target 的组合（结果不重复）。

**排序 + 同层去重**（`i > startIndex && nums[i] == nums[i-1]`）：

```java
/**
 * LeetCode 40. 组合总和 II
 * 重复元素 + 不可重复用 → 排序 + 同层去重
 * 时间 O(指数级)，空间 O(n)
 */
public List<List<Integer>> combinationSum2(int[] candidates, int target) {
    Arrays.sort(candidates);                     // ① 排序（去重前提）
    List<List<Integer>> result = new ArrayList<>();
    backtrack(candidates, target, 0, 0, new ArrayList<>(), result);
    return result;
}

private void backtrack(int[] nums, int target, int start, int sum,
                       List<Integer> path, List<List<Integer>> result) {
    if (sum == target) {
        result.add(new ArrayList<>(path));
        return;
    }
    for (int i = start; i < nums.length; i++) {
        if (sum + nums[i] > target) break;       // ② 和剪枝
        if (i > start && nums[i] == nums[i - 1]) continue;  // ③ ⚠️ 同层去重！
        path.add(nums[i]);
        backtrack(nums, target, i + 1, sum + nums[i], path, result);  // ④ 传 i+1（不可重复）
        path.remove(path.size() - 1);
    }
}
// 推演 [10,1,2,7,6,1,5], target=8 → 排序 [1,1,2,5,6,7,10]
//   结果 [[1,1,6],[1,2,5],[1,7],[2,6]] ✓（无重复组合！）
// ⚠️ 去重条件为什么是 i > start 而非 i > 0:
//   同层（同一循环）跳过重复；不同层（不同 start）允许——
//   [1,1,6] 里的第二个 1 来自"下一层"，必须保留！
```

> 🎯 **40 的教科书价值**："**'排序 + `i > start` 同层去重'是组合去重的标准姿势**——`i > start`（而非 `i > 0`）保证'同层去重、跨层保留'：结果 `[1,1,6]` 中的两个 1 分属两层，不能误删。"

## 4. 90 子集 II（去重子集）

### 4.1 题目与思路

**题**：含重复元素的数组的所有子集（不重复）。

**78 子集模板 + 同层去重**：

```java
/**
 * LeetCode 90. 子集 II
 * 78 子集 + 排序 + 同层去重
 * 时间 O(n·2ⁿ)，空间 O(n)
 */
public List<List<Integer>> subsetsWithDup(int[] nums) {
    Arrays.sort(nums);                           // ① 排序（去重前提）
    List<List<Integer>> result = new ArrayList<>();
    backtrack(nums, 0, new ArrayList<>(), result);
    return result;
}

private void backtrack(int[] nums, int start,
                       List<Integer> path, List<List<Integer>> result) {
    result.add(new ArrayList<>(path));           // ② 开头记录（子集）
    for (int i = start; i < nums.length; i++) {
        if (i > start && nums[i] == nums[i - 1]) continue;   // ③ 同层去重
        path.add(nums[i]);
        backtrack(nums, i + 1, path, result);
        path.remove(path.size() - 1);
    }
}
// 推演 [1,2,2]：排序 [1,2,2] → 结果 6 个（非 8 个）：
//   [] [1] [1,2] [1,2,2] [2] [2,2] ✓（[2] 只出现一次）
// ⚠️ 去重后子集数 = 6（2³ − 重复的 [2] 分支）
```

> 💡 **家族同构**："**90 = 78 + 一行去重**——子集 II 与组合总和 II 共享完全相同的去重条件，'排序 + 同层跳过'是含重复元素题目的统一配方。"

## 5. 216 组合总和 III 与 17 电话号码

### 5.1 216 组合总和 III（定长 + 和约束）

```java
/**
 * LeetCode 216. 组合总和 III（1..9 取 k 个，和 = n）
 * 77 组合 + 和剪枝（sum > n 提前返回）
 */
public List<List<Integer>> combinationSum3(int k, int n) {
    List<List<Integer>> result = new ArrayList<>();
    backtrack(k, n, 1, 0, new ArrayList<>(), result);
    return result;
}

private void backtrack(int k, int n, int start, int sum,
                       List<Integer> path, List<List<Integer>> result) {
    if (sum > n) return;                         // ① 和剪枝
    if (path.size() == k) {                      // ② 定长
        if (sum == n) result.add(new ArrayList<>(path));
        return;
    }
    for (int i = start; i <= 9; i++) {
        path.add(i);
        backtrack(k, n, i + 1, sum + i, path, result);
        path.remove(path.size() - 1);
    }
}
// 推演 k=3, n=7 → [[1,2,4]] ✓；k=3, n=9 → [[1,2,6],[1,3,5],[2,3,4]] ✓
```

### 5.2 17 电话号码的字母组合（多集合组合）

```java
/**
 * LeetCode 17. 电话号码的字母组合
 * 多集合组合: 每层对应一个数字的字母集
 * 时间 O(4ⁿ·n)，空间 O(n)
 */
public List<String> letterCombinations(String digits) {
    List<String> result = new ArrayList<>();
    if (digits.isEmpty()) return result;
    String[] map = {"", "", "abc", "def", "ghi", "jkl", "mno",
                    "pqrs", "tuv", "wxyz"};      // ① 数字 → 字母映射
    backtrack(digits, map, 0, new StringBuilder(), result);
    return result;
}

private void backtrack(String digits, String[] map, int index,
                       StringBuilder path, List<String> result) {
    if (index == digits.length()) {              // ② 结束: 所有数字处理完
        result.add(path.toString());
        return;
    }
    String letters = map[digits.charAt(index) - '0'];
    for (char c : letters.toCharArray()) {
        path.append(c);                          // ③ 选（当前数字的一个字母）
        backtrack(digits, map, index + 1, path, result);   // ④ 递（下一个数字）
        path.deleteCharAt(path.length() - 1);    // ⑤ 回
    }
}
// 推演 "23" → ["ad","ae","af","bd","be","bf","cd","ce","cf"] ✓
// ⚠️ 这里没有 startIndex——每层选择集不同（数字不同），
//   用 index 指向"当前处理的数字"即可
```

> 💡 **17 的特殊性**："**17 是'多集合组合'——每层从不同的集合选（而非同一个数组）**，所以不用 startIndex 而用 index 指向数字位。它展示了回溯的通用性：'选择列表'可以是任何东西。"

## 6. 去重核心：同层 vs 同路径

### 6.1 两种去重的语义

```text
同层去重（组合/子集）: "同一层不选重复的起点"
  条件: i > startIndex && nums[i] == nums[i-1]
  效果: 同一层跳过重复元素 → 结果组合不重复
  例: [1,1,2] 取 2 个 → [1,2] 只出现一次

跨层保留: 不同层允许重复元素
  例: [1,1,6] 中两个 1 分属两层（40 题）——必须保留

⚠️ 判断口诀:
  "`i > start` 同层去重（组合/子集用）；
   `!used[i-1]` 同层去重（排列用，03 文件）；
   用错条件 → 要么重复、要么漏解"
```

### 6.2 去重前置条件

```text
去重前必须先排序:
  只有排序后，重复元素才相邻 → `nums[i] == nums[i-1]` 才能判定"同值"

⚠️ 不排序的后果:
  [1,2,1] 中两个 1 不相邻 → 去重条件失效 → 结果重复
```

> 🎯 **去重一句话**："**'排序 + 同层跳过'是含重复元素题目的统一配方——`i > start` 管组合/子集、`!used[i-1]` 管排列（03 文件）**。能讲清'同层去重、跨层保留'的语义，去重题就通了。"

## 7. 组合家族易错点

| # | 易错点 | 正确写法 | 后果 |
|:---:|------|------|------|
| 1 | 39 传 i+1 | 可重复选传 **i** | 漏掉 [2,2,3] 型结果 |
| 2 | 40 忘排序 | 先排序再去重 | 去重失效 |
| 3 | 40 去重用 `i > 0` | 用 `i > start` | 误删跨层重复（漏解） |
| 4 | 40 剪枝用 continue | 排序后用 break | 多遍历无意义分支 |
| 5 | 90 在结束记录 | 开头记录（子集） | 漏中间子集 |
| 6 | 17 忘空 digits 特判 | 空串返回空列表 | 错误返回 [""] |
| 7 | 216 忘和剪枝 | `sum > n` 返回 | 指数级空转 |

> 🎯 **核心要点**：组合家族通关四件事——**① 传 i vs i+1**（可重复选传 i、不可重复传 i+1）；**② 排序 + `i > start` 同层去重**（跨层保留的语义）；**③ 和剪枝 break**（排序后安全）；**④ 17 多集合组合**（index 代替 startIndex）。**组合家族五题一个模板——startIndex 的四种形态 + 去重一行，全部变体都在掌握中**。

---

**下一模块**：[03-排列问题：used数组与去重](03-排列问题：used数组与去重.md)
**返回总览**：[00-回溯知识体系总览](00-回溯知识体系总览.md)
