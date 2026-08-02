# 07 - LeetCode 高频：字符串与数组

> 🎯 字符串和数组是 LeetCode 出题最多的两个类型，占面试算法题 40%+。本章按题型分类 + 代码模板，覆盖最高频 15 题

---

## 目录

1. [字符串高频套路](#1-字符串高频套路)
2. [数组高频题型](#2-数组高频题型)

---

## 1. 字符串高频套路

```java
// 最长回文子串（中心扩展法 O(n²)）
public String longestPalindrome(String s) {
    String res = "";
    for (int i = 0; i < s.length(); i++) {
        String s1 = expand(s, i, i);      // 奇数长度
        String s2 = expand(s, i, i + 1);  // 偶数长度
        if (s1.length() > res.length()) res = s1;
        if (s2.length() > res.length()) res = s2;
    }
    return res;
}
private String expand(String s, int l, int r) {
    while (l >= 0 && r < s.length() && s.charAt(l) == s.charAt(r)) {
        l--; r++;
    }
    return s.substring(l + 1, r);
}

// 字符串转整数 (atoi) — 处理边界条件
public int myAtoi(String s) {
    int i = 0, n = s.length(), sign = 1, result = 0;
    while (i < n && s.charAt(i) == ' ') i++;         // 1. 跳过空格
    if (i < n && (s.charAt(i) == '+' || s.charAt(i) == '-')) {
        sign = s.charAt(i++) == '-' ? -1 : 1;          // 2. 符号
    }
    while (i < n && Character.isDigit(s.charAt(i))) {  // 3. 数字
        int digit = s.charAt(i++) - '0';
        if (result > (Integer.MAX_VALUE - digit) / 10) // 溢出检测
            return sign == 1 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
        result = result * 10 + digit;
    }
    return result * sign;
}
```

### 字符串高频题型

| 题目 | 核心技巧 | 难度 |
|------|---------|:---:|
| 无重复最长子串 | 滑动窗口 + HashSet | M |
| 最长回文子串 | 中心扩展 | M |
| 字符串转整数 | 边界处理 + 溢出检测 | M |
| 字母异位词分组 | HashMap + 排序key | M |
| 最小覆盖子串 | 滑动窗口 + 双HashMap | H |
| 正则表达式匹配 | DP | H |

## 2. 数组高频题型

```java
// 三数之和（排序 + 双指针）
public List<List<Integer>> threeSum(int[] nums) {
    Arrays.sort(nums);
    List<List<Integer>> res = new ArrayList<>();

    for (int i = 0; i < nums.length - 2; i++) {
        if (i > 0 && nums[i] == nums[i - 1]) continue;  // 跳过重复
        int left = i + 1, right = nums.length - 1;
        while (left < right) {
            int sum = nums[i] + nums[left] + nums[right];
            if (sum == 0) {
                res.add(List.of(nums[i], nums[left], nums[right]));
                while (left < right && nums[left] == nums[left + 1]) left++;
                while (left < right && nums[right] == nums[right - 1]) right--;
                left++; right--;
            } else if (sum < 0) left++;
            else right--;
        }
    }
    return res;
}
```

### 数组高频题型

| 题目 | 核心技巧 | 难度 |
|------|---------|:---:|
| 两数之和 | HashMap | E |
| 三数之和 | 排序 + 双指针 | M |
| 最大子数组和 | DP / Kadane | M |
| 合并区间 | 排序 + 遍历 | M |
| 除自身外数组乘积 | 前缀积 × 后缀积 | M |
| 缺失的第一个正数 | 原地哈希 | H |

## 核心要点回顾

- 字符串问题三板斧：滑动窗口 / 中心扩展 / HashMap
- 数组问题三板斧：双指针 / 前缀和 / 排序
- 字符串 → 先想能不能转 `char[]`（StringBuilder 更灵活）
- 回文 → 中心扩展比 DP 更直观
- 三数之和 = 排序 + 固定一个 + 双指针扫两个

## 参考资料

1. LeetCode Top Interview 150
2. 《剑指 Offer》字符串章节
