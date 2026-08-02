# 01 刷题方法论与 Java 模板库

> 磨刀不误砍柴工——掌握刷题方法论和 Java 代码模板，刷题效率提升 10 倍

---

## 📚 目录

1. [刷题五步法](#1-刷题五步法)
2. [如何看题解最高效](#2-如何看题解最高效)
3. [复杂度分析速成](#3-复杂度分析速成)
4. [Java 数据类型与集合框架模板](#4-java-数据类型与集合框架模板)
5. [通用代码模板](#5-通用代码模板)
6. [常见边界条件检查清单](#6-常见边界条件检查清单)
7. [调试技巧](#7-调试技巧)
8. [刷题记录模板](#8-刷题记录模板)

---

## 1. 刷题五步法

以 LeetCode **1. 两数之和（Two Sum）** 为例，演示完整五步法。

### 第 1 步：读题与复述（2 分钟）

```text
题号：LeetCode 1. Two Sum（两数之和）
难度：⭐ Easy
标签：数组、哈希表

题目：给定整数数组 nums 和一个整数 target，返回两数之和等于 target 的下标。
      每种输入只有一个解，不能重复使用同一元素，返回顺序任意。

输入示例：nums = [2, 7, 11, 15], target = 9
输出示例：[0, 1]  (因为 nums[0] + nums[1] = 2 + 7 = 9)

数据范围：2 <= nums.length <= 10^4
         -10^9 <= nums[i] <= 10^9
         -10^9 <= target <= 10^9

复述：给一个数组和目标值，找到两个不同的数加起来等于目标，返回它们的下标。
```

### 第 2 步：思考解法（5 分钟）

```text
暴力解：
- 两层循环枚举所有组合，O(n²) 时间，O(1) 空间
- n = 10^4 → O(n²) = 10^8 次操作，勉强可过但不推荐

优化：
- 需要快速判断 target - nums[i] 是否存在 → 用 HashMap
- 遍历时把值存到 Map，同时检查 target - nums[i] 是否在 Map 中
- O(n) 时间，O(n) 空间

能想到的边界：
- 数组只有两个元素 → 直接返回 [0, 1]
- 有负数 → 不影响，加法结果不依赖正负
- 无解 → 题目保证有唯一解，但习惯性返回 new int[]{-1, -1}
```

### 第 3 步：看题解（10 分钟）

```java
// 最优解：一遍哈希表
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            int complement = target - nums[i];           // 需要的另一个数
            if (map.containsKey(complement)) {           // 找到了！
                return new int[]{map.get(complement), i}; // 注意顺序：先存的在前
            }
            map.put(nums[i], i);                         // 存当前值
        }
        return new int[]{-1, -1};                        // 无解（题目保证有解）
    }
}
```

**核心思路一句话**：利用 HashMap 的 O(1) 查找，遍历时就把值存进去，这样后面的元素可以回头找前面的。

### 第 4 步：手写实现（10 分钟）

关掉题解，自己从头写。写完在脑中走测试：

```text
nums = [2, 7, 11, 15], target = 9

i=0: complement=7, map={} 无 → put(2,0) → map={2:0}
i=1: complement=2, map有2 → return [0, 1] ✅
```

边界测试：

```text
nums = [3, 3], target = 6
i=0: complement=3, map={} 无 → put(3,0) → map={3:0}
i=1: complement=3, map有3 → return [0, 1] ✅
```

### 第 5 步：复盘与模板化（5 分钟）

| 问题 | 答案 |
|:----|------|
| 一句话核心思路 | 用 HashMap 空间换时间，一遍遍历完成查找 |
| 属于什么模式 | 哈希表优化查找（数组类题目最常用优化手段） |
| 复杂度 | 时间 O(n)，空间 O(n) |
| 容易错在哪 | 忘记检查 `map.containsKey()` 就直接放；下标顺序搞反 |
| 变体 | 有序数组用双指针 O(1) 空间；三数之和用排序+双指针 |

---

## 2. 如何看题解最高效

### 三遍阅读法

| 遍数 | 时间 | 目标 | 怎么做 |
|:---:|:----:|------|--------|
| 第一遍 | 3 分钟 | 理清思路 | 只看文字解析、图解、评论区高赞，**不看代码** |
| 第二遍 | 5 分钟 | 理解代码 | 逐行看代码，问自己"这行为什么这么写？这行删掉会怎样？" |
| 第三遍 | 2 分钟 | 复述检验 | 关闭题解，口述完整解法+写伪代码 |

### 什么情况下必须看题解

- 思考 10 分钟没有任何思路 → 说明对该题型不熟悉，标记为"新题型"
- 思路想出来了但代码写不出 → 说明代码实现能力不足，需要模仿模板
- 代码写出来但过不了全部测试用例 → 边界条件没想全，看题解如何处理的

### 什么情况下不要看题解

- 有思路但不确定是否最优 → 先自己写，哪怕不是最优，写完再看题解对比
- 已经会做但想更优解 → 自己先尝试优化，实在想不出再看

> 💡 **核心原则**：题解是"老师"不是"答案"。看了要关掉自己写一遍，才算真会。

---

## 3. 复杂度分析速成

### 3.1 时间复杂度速算

#### 循环法

```java
// O(n) —— 一个循环
for (int i = 0; i < n; i++) { sum += nums[i]; }

// O(n²) —— 两层嵌套循环
for (int i = 0; i < n; i++) {
    for (int j = 0; j < n; j++) { ... }
}

// O(n log n) —— 循环 × 二分
for (int i = 0; i < n; i++) {
    int idx = binarySearch(arr, nums[i]);  // O(log n)
}
```

#### 递归法（Master Theorem）

对于 T(n) = aT(n/b) + O(n^d)：

| 条件 | 复杂度 | 示例 |
|:----:|:------:|------|
| d > log_b(a) | O(n^d) | 二分查找 T(n) = T(n/2) + O(1), d=0, log₂1=0 → O(log n) |
| d = log_b(a) | O(n^d log n) | 归并排序 T(n)=2T(n/2)+O(n), d=1, log₂2=1 → O(n log n) |
| d < log_b(a) | O(n^{log_b(a)}) | 斐波那契 T(n)=T(n-1)+T(n-2)+O(1) → O(2ⁿ) |

### 3.2 空间复杂度速算

```text
O(1)   → 只用了几个变量（指针、标记位等）
O(n)   → 用了一个数组/哈希表/队列等线性结构
O(n²)  → 用了二维矩阵
O(n)   → 递归深度 n（注意递归栈也算空间！）
```

> ⚠️ **常见陷阱**：
> 1. 递归不算空间？—— 递归栈算空间！快排最坏 O(n) 栈空间
> 2. 输入不计入空间复杂度，但辅助空间要算
> 3. `String` 拼接是 O(n²) 空间（每次创建新字符串），要用 `StringBuilder`

### 3.3 面试中如何口算复杂度

面试官："你分析一下复杂度。"

```text
标准回答模板：
"时间复杂度：代码只有一个循环遍历 n 个元素，每次循环中的操作是 O(1) 的 HashMap 查找，
所以总的时间复杂度是 O(n)。空间复杂度：用了一个 HashMap 存储 n 个元素的值和下标，
所以空间复杂度是 O(n)。如果要求 O(1) 空间，可以先排序然后用双指针，
但那样时间复杂度会升到 O(n log n)，是典型的空间换时间权衡。"
```

> 🎯 **要点**：说清"循环层数 × 每层操作" + 解释为什么不能更优 + 给出 trade-off 方案。

---

## 4. Java 数据类型与集合框架模板

### 4.1 数组操作模板

```java
import java.util.*;

public class ArrayTemplates {
    public static void main(String[] args) {
        // ===== 1. 遍历 =====
        int[] arr = {1, 2, 3, 4, 5};
        for (int i = 0; i < arr.length; i++) {          // 带下标遍历
            System.out.println(arr[i]);
        }
        for (int num : arr) { }                          // 增强 for 循环（无下标）

        // ===== 2. 排序 =====
        int[] nums = {5, 3, 1, 4, 2};
        Arrays.sort(nums);                               // 升序排序，O(n log n)
        // 降序排序（需要 Integer 数组）
        Integer[] numsBoxed = {5, 3, 1, 4, 2};
        Arrays.sort(numsBoxed, (a, b) -> b - a);         // 降序

        // ===== 3. 二分查找 =====
        int idx = Arrays.binarySearch(nums, 3);          // 返回下标，未找到返回负数

        // ===== 4. 填充 =====
        int[] filled = new int[10];
        Arrays.fill(filled, -1);                         // 全部填充为 -1

        // ===== 5. 复制 =====
        int[] copy = Arrays.copyOf(nums, nums.length);   // 完整复制
        int[] copyRange = Arrays.copyOfRange(nums, 1, 4); // 复制 [1,4)

        // ===== 6. 比较 =====
        int[] a = {1, 2, 3}, b = {1, 2, 3};
        boolean equal = Arrays.equals(a, b);             // true

        // ===== 7. 二维数组排序 =====
        int[][] intervals = {{1, 3}, {2, 6}, {8, 10}};
        Arrays.sort(intervals, (o1, o2) -> o1[0] - o2[0]); // 按第一列升序

        // ===== 8. 二维数组初始化 =====
        int[][] matrix = new int[3][4];                  // 3行4列
        for (int[] row : matrix) Arrays.fill(row, 0);    // 填充每一行
    }
}
```

### 4.2 HashMap / HashSet 操作模板

```java
import java.util.*;

public class MapSetTemplates {
    public static void main(String[] args) {
        // ===== 1. 频率统计（经典）=====
        int[] nums = {1, 2, 2, 3, 3, 3};
        Map<Integer, Integer> freq = new HashMap<>();
        for (int num : nums) {
            freq.put(num, freq.getOrDefault(num, 0) + 1);  // 频率 +1
        }
        // freq → {1:1, 2:2, 3:3}

        // ===== 2. 字符频率统计（字母异位词判断）=====
        String s = "anagram";
        Map<Character, Integer> charFreq = new HashMap<>();
        for (char c : s.toCharArray()) {
            charFreq.merge(c, 1, Integer::sum);            // 另一写法
        }

        // ===== 3. 遍历 HashMap =====
        for (Map.Entry<Integer, Integer> entry : freq.entrySet()) {
            int key = entry.getKey();
            int value = entry.getValue();
        }
        // 只遍历 key
        for (int key : freq.keySet()) { }
        // 只遍历 value
        for (int val : freq.values()) { }

        // ===== 4. HashMap 存映射关系（两数之和）=====
        int[] nums2 = {2, 7, 11, 15};
        int target = 9;
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < nums2.length; i++) {
            int complement = target - nums2[i];
            if (map.containsKey(complement)) {
                System.out.println(map.get(complement) + ", " + i);
            }
            map.put(nums2[i], i);
        }

        // ===== 5. HashMap 存 List（分组）=====
        String[] strs = {"eat", "tea", "tan", "ate", "nat", "bat"};
        Map<String, List<String>> group = new HashMap<>();
        for (String str : strs) {
            char[] chars = str.toCharArray();
            Arrays.sort(chars);
            String key = new String(chars);
            group.computeIfAbsent(key, k -> new ArrayList<>()).add(str);
        }

        // ===== 6. HashSet 去重 =====
        int[] nums3 = {1, 2, 2, 3, 3, 3};
        Set<Integer> set = new HashSet<>();
        for (int num : nums3) set.add(num);
        // set → {1, 2, 3}

        // ===== 7. HashSet 判重（无重复字符的最长子串）=====
        String str = "abcabcbb";
        Set<Character> window = new HashSet<>();
        int left = 0, maxLen = 0;
        for (int right = 0; right < str.length(); right++) {
            char c = str.charAt(right);
            while (window.contains(c)) {
                window.remove(str.charAt(left++));
            }
            window.add(c);
            maxLen = Math.max(maxLen, right - left + 1);
        }
        System.out.println(maxLen);  // 3 → "abc"

        // ===== 8. 用 int[] 替代 HashMap（字符计数）=====
        // 适用于 key 是有限范围的整数（如小写字母 26 个）
        String s1 = "anagram", s2 = "nagaram";
        int[] count = new int[26];
        for (char c : s1.toCharArray()) count[c - 'a']++;
        for (char c : s2.toCharArray()) count[c - 'a']--;
        boolean isAnagram = true;
        for (int c : count) if (c != 0) isAnagram = false;  // true
    }
}
```

### 4.3 ArrayList / LinkedList 模板

```java
import java.util.*;

public class ListTemplates {
    public static void main(String[] args) {
        // ===== ArrayList =====
        List<Integer> list = new ArrayList<>();
        list.add(1);                                    // 末尾添加 O(1) 均摊
        list.add(0, 0);                                 // 指定位置插入 O(n)
        list.get(0);                                    // 随机访问 O(1)
        list.set(0, 10);                                // 修改 O(1)
        list.remove(list.size() - 1);                   // 删除末尾 O(1)
        list.remove(0);                                 // 删除开头 O(n)
        list.contains(1);                               // 是否包含 O(n)
        list.indexOf(1);                                // 查找下标 O(n)
        list.size();                                    // 长度

        // 遍历
        for (int i = 0; i < list.size(); i++) {
            System.out.println(list.get(i));
        }
        for (int num : list) { }

        // List 转 int[]
        int[] arr = list.stream().mapToInt(i -> i).toArray();
        // int[] 转 List（装箱）
        int[] nums = {1, 2, 3};
        List<Integer> listFromArray = new ArrayList<>();
        for (int n : nums) listFromArray.add(n);

        // 排序
        Collections.sort(list);
        Collections.sort(list, (a, b) -> b - a);        // 降序

        // ===== LinkedList（作为队列/双端队列）=====
        LinkedList<Integer> linkedList = new LinkedList<>();
        linkedList.addFirst(1);                         // 头插
        linkedList.addLast(2);                          // 尾插
        linkedList.getFirst();                          // 取头
        linkedList.getLast();                           // 取尾
        linkedList.removeFirst();                       // 删头
        linkedList.removeLast();                        // 删尾
    }
}
```

### 4.4 Stack / Queue / Deque 模板

```java
import java.util.*;

public class StackQueueTemplates {
    public static void main(String[] args) {
        // ===== Stack（推荐用 ArrayDeque）=====
        // ⚠️ Stack 是遗留类，性能较差，推荐用 ArrayDeque
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(1);                                  // 入栈
        stack.push(2);
        int top = stack.peek();                         // 栈顶（不出栈）→ 2
        int pop = stack.pop();                          // 出栈 → 2
        boolean empty = stack.isEmpty();                // 是否为空

        // 单调栈：下一个更大元素（LeetCode 496）
        int[] temperatures = {73, 74, 75, 71, 69, 72, 76, 73};
        int[] result = dailyTemperatures(temperatures);
        System.out.println(Arrays.toString(result));    // [1,1,4,2,1,1,0,0]

        // ===== Queue（队列）=====
        Queue<Integer> queue = new LinkedList<>();
        queue.offer(1);                                 // 入队
        queue.offer(2);
        int front = queue.peek();                       // 队首 → 1
        int poll = queue.poll();                        // 出队 → 1

        // ===== Deque（双端队列）=====
        Deque<Integer> deque = new ArrayDeque<>();
        deque.addFirst(1);                              // 队首入
        deque.addLast(2);                               // 队尾入
        deque.peekFirst();                              // 看队首
        deque.peekLast();                               // 看队尾
        deque.pollFirst();                              // 队首出
        deque.pollLast();                               // 队尾出

        // 双端队列用作 BFS
        Deque<Integer> bfsDeque = new ArrayDeque<>();
        bfsDeque.offerLast(0);                          // 入队（尾）
        while (!bfsDeque.isEmpty()) {
            int cur = bfsDeque.pollFirst();             // 出队（头）
            // 处理 cur
            // bfsDeque.offerLast(next);                // 扩展节点
        }
    }

    // 单调栈示例：每日温度（LeetCode 739）
    public static int[] dailyTemperatures(int[] temperatures) {
        int n = temperatures.length;
        int[] answer = new int[n];
        Deque<Integer> stack = new ArrayDeque<>();       // 存下标
        for (int i = 0; i < n; i++) {
            while (!stack.isEmpty() && temperatures[i] > temperatures[stack.peek()]) {
                int prevIdx = stack.pop();
                answer[prevIdx] = i - prevIdx;
            }
            stack.push(i);
        }
        return answer;
    }
}
```

### 4.5 PriorityQueue 模板

```java
import java.util.*;

public class HeapTemplates {
    public static void main(String[] args) {
        // ===== 1. 最小堆（默认）=====
        PriorityQueue<Integer> minHeap = new PriorityQueue<>();
        minHeap.offer(3);
        minHeap.offer(1);
        minHeap.offer(2);
        System.out.println(minHeap.poll());  // 1（最小值优先出）

        // ===== 2. 最大堆 =====
        PriorityQueue<Integer> maxHeap = new PriorityQueue<>((a, b) -> b - a);
        maxHeap.offer(3);
        maxHeap.offer(1);
        maxHeap.offer(2);
        System.out.println(maxHeap.poll());  // 3（最大值优先出）

        // ===== 3. TopK 问题（前 K 个最小元素）=====
        int[] nums = {3, 2, 1, 5, 6, 4};
        int k = 3;
        PriorityQueue<Integer> topK = new PriorityQueue<>((a, b) -> b - a); // 最大堆
        for (int num : nums) {
            topK.offer(num);
            if (topK.size() > k) topK.poll();  // 超过 K 个就弹出最大的
        }
        // topK 中就是前 K 个最小元素
        System.out.println(topK.peek());       // 前 K 个中的最大值 → 3

        // ===== 4. 合并 K 个有序链表（LeetCode 23）=====
        // PriorityQueue<ListNode> pq = new PriorityQueue<>((a, b) -> a.val - b.val);

        // ===== 5. 自定义比较器（二维数组）=====
        // 按第一个元素升序，第一个相同按第二个升序
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) ->
            a[0] == b[0] ? a[1] - b[1] : a[0] - b[0]);

        // ===== 6. 双堆维护中位数（LeetCode 295）=====
        PriorityQueue<Integer> left = new PriorityQueue<>((a, b) -> b - a); // 大顶堆
        PriorityQueue<Integer> right = new PriorityQueue<>();              // 小顶堆
        int[] stream = {1, 2, 3, 4, 5};
        for (int num : stream) {
            if (left.isEmpty() || num <= left.peek()) {
                left.offer(num);
            } else {
                right.offer(num);
            }
            // 平衡两堆大小
            if (left.size() > right.size() + 1) right.offer(left.poll());
            if (right.size() > left.size()) left.offer(right.poll());
        }
        System.out.println(left.peek());  // 中位数
    }
}
```

### 4.6 String / StringBuilder 模板

```java
import java.util.*;

public class StringTemplates {
    public static void main(String[] args) {
        // ===== 1. String 基础操作 =====
        String s = "Hello World";
        s.length();                                     // 长度 11
        s.charAt(0);                                    // 'H'
        s.substring(0, 5);                              // "Hello" [0,5)
        s.substring(6);                                 // "World" 从 6 到末尾
        s.indexOf("o");                                 // 4（第一个 o）
        s.lastIndexOf("o");                             // 7（最后一个 o）
        s.contains("World");                            // true
        s.startsWith("He");                             // true
        s.endsWith("ld");                               // true
        s.equals("Hello World");                        // true（必须用 equals!）
        s.toCharArray();                                // char[] 数组
        s.split(" ");                                   // ["Hello", "World"]

        // ===== 2. StringBuilder（高频拼接用）=====
        StringBuilder sb = new StringBuilder();
        sb.append('a');                                 // 追加字符
        sb.append("bc");                                // 追加字符串
        sb.insert(0, "x");                              // 在指定位置插入
        sb.deleteCharAt(sb.length() - 1);               // 删除最后一个字符
        sb.reverse();                                   // 反转
        sb.toString();                                  // 转 String

        // ===== 3. 常用操作：回文检查 =====
        String str = "racecar";
        boolean isPalindrome = true;
        for (int i = 0; i < str.length() / 2; i++) {
            if (str.charAt(i) != str.charAt(str.length() - 1 - i)) {
                isPalindrome = false;
                break;
            }
        }
        // 或用 StringBuilder
        boolean isPal2 = str.equals(new StringBuilder(str).reverse().toString());

        // ===== 4. 字符计数（字母异位词）=====
        String s1 = "anagram", s2 = "nagaram";
        int[] count = new int[26];
        for (char c : s1.toCharArray()) count[c - 'a']++;
        for (char c : s2.toCharArray()) count[c - 'a']--;
        boolean isAnagram = Arrays.stream(count).allMatch(c -> c == 0);

        // ===== 5. 数字 ↔ 字符串=====
        int num = 123;
        String numStr = Integer.toString(num);          // "123"
        int parsed = Integer.parseInt("123");           // 123

        // ===== 6. StringBuilder 作为可变字符串（回溯常用）=====
        StringBuilder path = new StringBuilder();
        path.append('a');                                // "a"
        path.append('b');                                // "ab"
        path.deleteCharAt(path.length() - 1);           // 回溯 → "a"
        path.deleteCharAt(path.length() - 1);           // 回溯 → ""
    }
}
```

### 4.7 TreeMap / TreeSet 模板

```java
import java.util.*;

public class TreeTemplates {
    public static void main(String[] args) {
        // ===== TreeMap（有序 KV 存储）=====
        TreeMap<Integer, String> treeMap = new TreeMap<>();
        treeMap.put(3, "three");
        treeMap.put(1, "one");
        treeMap.put(2, "two");

        // 遍历会按键升序输出
        for (Map.Entry<Integer, String> entry : treeMap.entrySet()) {
            System.out.println(entry.getKey() + " → " + entry.getValue());
        }  // 1→one, 2→two, 3→three

        // 常用 API
        treeMap.firstKey();                              // 最小 key → 1
        treeMap.lastKey();                               // 最大 key → 3
        treeMap.ceilingKey(2);                           // ≥ 2 的最小 key → 2
        treeMap.floorKey(2);                             // ≤ 2 的最大 key → 2
        treeMap.higherKey(2);                            // > 2 的最小 key → 3
        treeMap.lowerKey(2);                             // < 2 的最大 key → 1

        // ===== 区间操作 =====
        treeMap.subMap(1, 3);                            // key 在 [1,3) 范围内的子 Map
        treeMap.headMap(2);                              // key < 2 的子 Map
        treeMap.tailMap(2);                              // key ≥ 2 的子 Map

        // ===== TreeSet（有序集合）=====
        TreeSet<Integer> treeSet = new TreeSet<>();
        treeSet.add(3); treeSet.add(1); treeSet.add(2);

        treeSet.first();                                 // 1
        treeSet.last();                                  // 3
        treeSet.ceiling(2);                              // ≥2 的最小 → 2
        treeSet.floor(2);                                // ≤2 的最大 → 2
        treeSet.higher(2);                               // >2 的最小 → 3
        treeSet.lower(2);                                // <2 的最大 → 1

        // TreeSet 用于滑动窗口中的有序维护
        // 示例：包含重复数字 III（LeetCode 220）
        int[] nums = {1, 3, 2, 5, 4};
        int k = 2, t = 1;
        TreeSet<Integer> window = new TreeSet<>();
        for (int i = 0; i < nums.length; i++) {
            Integer ceil = window.ceiling(nums[i] - t);  // 找 ≥ nums[i]-t 的元素
            if (ceil != null && ceil <= nums[i] + t) {
                System.out.println("Found: " + ceil + " and " + nums[i]);
            }
            window.add(nums[i]);
            if (window.size() > k) {
                window.remove(nums[i - k]);
            }
        }
    }
}
```

### 4.8 自定义排序 Comparator 模板

```java
import java.util.*;

public class ComparatorTemplates {
    public static void main(String[] args) {
        // ===== 1. 一维数组排序 =====
        int[] arr = {5, 3, 1, 4, 2};
        Arrays.sort(arr);                                   // 升序

        // ===== 2. 二维数组排序 =====
        int[][] intervals = {{1, 3}, {8, 10}, {2, 6}};

        // 按第一个元素升序
        Arrays.sort(intervals, (a, b) -> a[0] - b[0]);

        // 按第一个元素升序，相同则按第二个降序
        Arrays.sort(intervals, (a, b) ->
            a[0] == b[0] ? b[1] - a[1] : a[0] - b[0]);

        // 用 Comparator.comparing（更易读）
        Arrays.sort(intervals, Comparator.comparingInt(a -> a[0]));

        // ===== 3. 字符串排序 =====
        String[] strs = {"banana", "apple", "cherry"};
        Arrays.sort(strs);                                   // 字典序

        // 按字符串长度排序
        Arrays.sort(strs, (a, b) -> a.length() - b.length());

        // ===== 4. List 排序 =====
        List<Integer> list = Arrays.asList(3, 1, 2);
        Collections.sort(list);                              // 升序
        Collections.sort(list, (a, b) -> b - a);             // 降序

        // ===== 5. 对象排序 =====
        List<Person> people = new ArrayList<>();
        people.add(new Person("Alice", 25));
        people.add(new Person("Bob", 20));
        people.add(new Person("Charlie", 30));

        // 按年龄升序
        Collections.sort(people, (p1, p2) -> p1.age - p2.age);
        // 先按年龄升序，同年龄按名字字典序
        Collections.sort(people, (p1, p2) -> {
            if (p1.age != p2.age) return p1.age - p2.age;
            return p1.name.compareTo(p2.name);
        });

        // 用 Comparator.comparing
        Collections.sort(people, Comparator.comparingInt(p -> p.age));
        // 链式比较
        Collections.sort(people,
            Comparator.comparingInt((Person p) -> p.age)
                      .thenComparing(p -> p.name));

        // ===== 6. PriorityQueue 自定义排序 =====
        // 小顶堆（按第二个元素）
        PriorityQueue<int[]> pq = new PriorityQueue<>(
            (a, b) -> a[1] - b[1]);
        // 大顶堆
        PriorityQueue<int[]> pqMax = new PriorityQueue<>(
            (a, b) -> b[1] - a[1]);

        System.out.println("Sorted: " + people);
    }
}

class Person {
    String name;
    int age;
    Person(String name, int age) { this.name = name; this.age = age; }
    @Override
    public String toString() { return name + "(" + age + ")"; }
}
```

---

## 5. 通用代码模板

### 5.1 二分查找模板

```java
/**
 * 二分查找模板（三种变体）
 *
 * 核心原则：
 * - 左闭右闭 [left, right] 最通用
 * - 循环条件 left <= right
 * - mid = left + (right - left) / 2 防溢出
 */
public class BinarySearchTemplate {

    public static void main(String[] args) {
        int[] nums = {1, 2, 2, 2, 3, 4, 5};
        System.out.println("精确查找 3: " + binarySearch(nums, 3));         // 4
        System.out.println("左边界 2: "  + leftBound(nums, 2));             // 1
        System.out.println("右边界 2: "  + rightBound(nums, 2));            // 3
        System.out.println("左边界 6: "  + leftBound(nums, 6));             // -1
    }

    // ===== 1. 精确查找 =====
    // 返回 target 的下标，未找到返回 -1
    public static int binarySearch(int[] nums, int target) {
        int left = 0, right = nums.length - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (nums[mid] == target) return mid;
            else if (nums[mid] < target) left = mid + 1;
            else right = mid - 1;
        }
        return -1;
    }

    // ===== 2. 查找左边界（第一个 >= target 的位置）=====
    // 返回最左边等于 target 的下标，如果没有则返回 -1
    public static int leftBound(int[] nums, int target) {
        int left = 0, right = nums.length - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (nums[mid] >= target) {
                right = mid - 1;            // 收缩右边界，找更左的
            } else {
                left = mid + 1;
            }
        }
        // 检查 left 是否越界且值等于 target
        if (left >= nums.length || nums[left] != target) return -1;
        return left;
    }

    // ===== 3. 查找右边界（最后一个 <= target 的位置）=====
    public static int rightBound(int[] nums, int target) {
        int left = 0, right = nums.length - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (nums[mid] <= target) {
                left = mid + 1;             // 收缩左边界，找更右的
            } else {
                right = mid - 1;
            }
        }
        if (right < 0 || nums[right] != target) return -1;
        return right;
    }

    // ===== 4. 二分答案（LeetCode 875 爱吃香蕉的珂珂）=====
    public static int minEatingSpeed(int[] piles, int h) {
        int left = 1, right = 1_000_000_000;
        while (left < right) {
            int mid = left + (right - left) / 2;
            if (canEat(piles, mid, h)) {
                right = mid;            // 满足条件，尝试更小的速度
            } else {
                left = mid + 1;
            }
        }
        return left;
    }
    private static boolean canEat(int[] piles, int k, int h) {
        int hours = 0;
        for (int p : piles) {
            hours += (p + k - 1) / k;   // 向上取整
        }
        return hours <= h;
    }
}
```

### 5.2 滑动窗口模板

```java
import java.util.*;

/**
 * 滑动窗口模板
 *
 * 适用于：连续子数组/子串的最优值问题
 *
 * 定长窗口：窗口大小固定，每次右移一位
 * 变长窗口：右指针扩展直到不满足条件，左指针收缩恢复条件
 */
public class SlidingWindowTemplate {

    public static void main(String[] args) {
        // 定长窗口
        System.out.println("定长窗口最大值: " +
            Arrays.toString(maxSlidingWindow(new int[]{1,3,-1,-3,5,3,6,7}, 3)));  // [3,3,5,5,6,7]

        // 变长窗口 - 无重复最长子串
        System.out.println("最长无重复子串: " +
            lengthOfLongestSubstring("abcabcbb"));  // 3

        // 变长窗口 - 最小覆盖子串
        System.out.println("最小覆盖子串: " +
            minWindow("ADOBECODEBANC", "ABC"));  // "BANC"
    }

    // ===== 1. 定长滑动窗口模板（LeetCode 239 滑动窗口最大值）=====
    public static int[] maxSlidingWindow(int[] nums, int k) {
        if (nums == null || nums.length == 0) return new int[0];
        int n = nums.length;
        int[] res = new int[n - k + 1];
        Deque<Integer> deque = new ArrayDeque<>();  // 存下标，保持单调递减

        for (int i = 0; i < n; i++) {
            // 移除超出窗口范围的下标
            while (!deque.isEmpty() && deque.peekFirst() < i - k + 1) {
                deque.pollFirst();
            }
            // 保持单调递减：移除所有小于当前值的下标
            while (!deque.isEmpty() && nums[deque.peekLast()] < nums[i]) {
                deque.pollLast();
            }
            deque.offerLast(i);
            // 窗口已形成，记录结果
            if (i >= k - 1) {
                res[i - k + 1] = nums[deque.peekFirst()];
            }
        }
        return res;
    }

    // ===== 2. 变长滑动窗口模板（LeetCode 3 无重复字符的最长子串）=====
    public static int lengthOfLongestSubstring(String s) {
        if (s == null || s.length() == 0) return 0;
        Set<Character> window = new HashSet<>();
        int left = 0, maxLen = 0;

        for (int right = 0; right < s.length(); right++) {
            char c = s.charAt(right);
            // 如果窗口内有重复字符，收缩左边界
            while (window.contains(c)) {
                window.remove(s.charAt(left));
                left++;
            }
            window.add(c);
            maxLen = Math.max(maxLen, right - left + 1);
        }
        return maxLen;
    }

    // ===== 3. 最小覆盖子串模板（LeetCode 76）=====
    public static String minWindow(String s, String t) {
        if (s == null || t == null || s.length() < t.length()) return "";

        Map<Character, Integer> need = new HashMap<>();  // 目标字符计数
        Map<Character, Integer> window = new HashMap<>(); // 窗口内计数
        for (char c : t.toCharArray()) {
            need.put(c, need.getOrDefault(c, 0) + 1);
        }

        int left = 0, right = 0;
        int valid = 0;                                     // 已满足的字符种类数
        int start = 0, minLen = Integer.MAX_VALUE;

        while (right < s.length()) {
            char c = s.charAt(right);
            right++;
            // 加入窗口
            if (need.containsKey(c)) {
                window.put(c, window.getOrDefault(c, 0) + 1);
                if (window.get(c).equals(need.get(c))) {
                    valid++;
                }
            }

            // 所有字符已覆盖，尝试收缩左边界
            while (valid == need.size()) {
                if (right - left < minLen) {
                    minLen = right - left;
                    start = left;
                }
                char d = s.charAt(left);
                left++;
                if (need.containsKey(d)) {
                    if (window.get(d).equals(need.get(d))) {
                        valid--;
                    }
                    window.put(d, window.get(d) - 1);
                }
            }
        }
        return minLen == Integer.MAX_VALUE ? "" : s.substring(start, start + minLen);
    }
}
```

### 5.3 双指针模板

```java
import java.util.*;

/**
 * 双指针模板
 *
 * 三种模式：
 * 1. 左右指针 → 有序数组、两数之和、反转数组
 * 2. 快慢指针 → 链表环、原地去重、找中点
 * 3. 滑动窗口 → 已在上方模板中单独列出
 */
public class TwoPointersTemplate {

    public static void main(String[] args) {
        // 左右指针：两数之和 II
        int[] nums = {2, 7, 11, 15};
        System.out.println("两数之和 II: " +
            Arrays.toString(twoSumII(nums, 9)));  // [1, 2]

        // 快慢指针：原地去重
        int[] dup = {0, 0, 1, 1, 1, 2, 2, 3, 3, 4};
        int len = removeDuplicates(dup);
        System.out.println("去重后长度: " + len + " → " +
            Arrays.toString(Arrays.copyOf(dup, len)));  // [0,1,2,3,4]

        // 三数之和
        int[] nums3 = {-1, 0, 1, 2, -1, -4};
        System.out.println("三数之和: " + threeSum(nums3));  // [[-1,-1,2],[-1,0,1]]
    }

    // ===== 1. 左右指针：两数之和 II（LeetCode 167）=====
    // 输入有序数组，返回下标（从 1 开始）
    public static int[] twoSumII(int[] numbers, int target) {
        int left = 0, right = numbers.length - 1;
        while (left < right) {
            int sum = numbers[left] + numbers[right];
            if (sum == target) {
                return new int[]{left + 1, right + 1};  // 题目要求 1-indexed
            } else if (sum < target) {
                left++;
            } else {
                right--;
            }
        }
        return new int[]{-1, -1};
    }

    // ===== 2. 快慢指针：原地去重（LeetCode 26）=====
    public static int removeDuplicates(int[] nums) {
        if (nums == null || nums.length == 0) return 0;
        int slow = 0;  // 指向不重复区域的最后一个位置
        for (int fast = 1; fast < nums.length; fast++) {
            if (nums[fast] != nums[slow]) {
                slow++;
                nums[slow] = nums[fast];
            }
        }
        return slow + 1;
    }

    // ===== 3. 快慢指针：链表找中点 =====
    public static ListNode middleNode(ListNode head) {
        ListNode slow = head, fast = head;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }
        return slow;
    }

    // ===== 4. 三数之和（LeetCode 15）=====
    public static List<List<Integer>> threeSum(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        if (nums == null || nums.length < 3) return result;

        Arrays.sort(nums);  // 排序是双指针的前提

        for (int i = 0; i < nums.length - 2; i++) {
            // 跳过重复的第一个数
            if (i > 0 && nums[i] == nums[i - 1]) continue;

            int left = i + 1, right = nums.length - 1;
            int target = -nums[i];

            while (left < right) {
                int sum = nums[left] + nums[right];
                if (sum == target) {
                    result.add(Arrays.asList(nums[i], nums[left], nums[right]));
                    // 跳过重复
                    while (left < right && nums[left] == nums[left + 1]) left++;
                    while (left < right && nums[right] == nums[right - 1]) right--;
                    left++;
                    right--;
                } else if (sum < target) {
                    left++;
                } else {
                    right--;
                }
            }
        }
        return result;
    }

    static class ListNode {
        int val;
        ListNode next;
        ListNode(int val) { this.val = val; }
    }
}
```

### 5.4 BFS 模板

```java
import java.util.*;

/**
 * BFS 模板
 *
 * 适用场景：最短路径、层序遍历、拓扑排序
 *
 * 核心数据结构：Queue（队列）
 * 核心操作：将当前层的所有节点出队，将它们的子节点入队
 */
public class BFSTemplate {

    public static void main(String[] args) {
        // ===== 1. 二叉树层序遍历（LeetCode 102）=====
        // 树结构：[3,9,20,null,null,15,7]
        TreeNode root = new TreeNode(3);
        root.left = new TreeNode(9);
        root.right = new TreeNode(20);
        root.right.left = new TreeNode(15);
        root.right.right = new TreeNode(7);

        System.out.println("二叉树层序遍历: " + levelOrder(root));
        // [[3], [9, 20], [15, 7]]

        // ===== 2. 矩阵 BFS：岛屿数量 =====
        char[][] grid = {
            {'1','1','0','0','0'},
            {'1','1','0','0','0'},
            {'0','0','1','0','0'},
            {'0','0','0','1','1'}
        };
        System.out.println("岛屿数量: " + numIslands(grid));  // 3
    }

    // ===== 二叉树层序遍历（LeetCode 102）=====
    public static List<List<Integer>> levelOrder(TreeNode root) {
        List<List<Integer>> result = new ArrayList<>();
        if (root == null) return result;

        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            List<Integer> level = new ArrayList<>();

            for (int i = 0; i < levelSize; i++) {
                TreeNode node = queue.poll();
                level.add(node.val);

                if (node.left != null) queue.offer(node.left);
                if (node.right != null) queue.offer(node.right);
            }
            result.add(level);
        }
        return result;
    }

    // ===== 矩阵 BFS：岛屿数量（LeetCode 200）=====
    private static final int[][] DIRECTIONS = {{1,0}, {-1,0}, {0,1}, {0,-1}};

    public static int numIslands(char[][] grid) {
        if (grid == null || grid.length == 0) return 0;
        int m = grid.length, n = grid[0].length;
        int count = 0;

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == '1') {
                    count++;
                    bfsIsland(grid, i, j, m, n);
                }
            }
        }
        return count;
    }

    private static void bfsIsland(char[][] grid, int startI, int startJ, int m, int n) {
        Queue<int[]> queue = new LinkedList<>();
        queue.offer(new int[]{startI, startJ});
        grid[startI][startJ] = '0';  // 标记已访问（沉岛）

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            for (int[] dir : DIRECTIONS) {
                int ni = cur[0] + dir[0];
                int nj = cur[1] + dir[1];
                if (ni >= 0 && ni < m && nj >= 0 && nj < n && grid[ni][nj] == '1') {
                    grid[ni][nj] = '0';
                    queue.offer(new int[]{ni, nj});
                }
            }
        }
    }

    // ===== 图的 BFS：最短路径（无权图）=====
    public static int bfsShortestPath(Map<Integer, List<Integer>> graph, int start, int target) {
        Queue<Integer> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();
        queue.offer(start);
        visited.add(start);
        int steps = 0;

        while (!queue.isEmpty()) {
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                int cur = queue.poll();
                if (cur == target) return steps;

                for (int neighbor : graph.getOrDefault(cur, new ArrayList<>())) {
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        queue.offer(neighbor);
                    }
                }
            }
            steps++;
        }
        return -1;  // 不可达
    }

    static class TreeNode {
        int val;
        TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }
}
```

### 5.5 DFS 模板

```java
import java.util.*;

/**
 * DFS 模板
 *
 * 适用场景：所有路径、二叉树遍历、回溯穷举、网格渗透
 *
 * 核心：递归 + 回溯（恢复状态）
 */
public class DFSTemplate {

    public static void main(String[] args) {
        // ===== 1. 二叉树 DFS：路径总和（LeetCode 112）=====
        TreeNode root = new TreeNode(5);
        root.left = new TreeNode(4);
        root.right = new TreeNode(8);
        root.left.left = new TreeNode(11);
        root.right.left = new TreeNode(13);
        root.right.right = new TreeNode(4);
        System.out.println("路径总和: " + hasPathSum(root, 22));  // true

        // ===== 2. 回溯：全排列（LeetCode 46）=====
        System.out.println("全排列: " + permute(new int[]{1, 2, 3}));
        // [[1,2,3],[1,3,2],[2,1,3],[2,3,1],[3,1,2],[3,2,1]]

        // ===== 3. 网格 DFS：岛屿数量 =====
        char[][] grid = {
            {'1','1','1','1','0'},
            {'1','1','0','1','0'},
            {'1','1','0','0','0'},
            {'0','0','0','0','0'}
        };
        System.out.println("DFS 岛屿数量: " + numIslandsDFS(grid));  // 1

        // ===== 4. 回溯：组合（LeetCode 77）=====
        System.out.println("组合 C(4,2): " + combine(4, 2));
        // [[1,2],[1,3],[1,4],[2,3],[2,4],[3,4]]
    }

    // ===== 1. 二叉树 DFS（LeetCode 112 路径总和）=====
    public static boolean hasPathSum(TreeNode root, int targetSum) {
        if (root == null) return false;
        // 叶子节点：判断当前值是否等于目标
        if (root.left == null && root.right == null) {
            return root.val == targetSum;
        }
        // 递归左右子树
        return hasPathSum(root.left, targetSum - root.val)
            || hasPathSum(root.right, targetSum - root.val);
    }

    // ===== 2. 回溯框架：全排列（LeetCode 46）=====
    public static List<List<Integer>> permute(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        boolean[] used = new boolean[nums.length];
        backtrackPermute(nums, used, new ArrayList<>(), result);
        return result;
    }

    private static void backtrackPermute(int[] nums, boolean[] used,
                                          List<Integer> path, List<List<Integer>> result) {
        // 终止条件：路径长度等于数组长度
        if (path.size() == nums.length) {
            result.add(new ArrayList<>(path));  // 必须 new，否则存的引用
            return;
        }
        for (int i = 0; i < nums.length; i++) {
            if (used[i]) continue;        // 已使用过
            used[i] = true;
            path.add(nums[i]);            // 做选择
            backtrackPermute(nums, used, path, result);
            path.remove(path.size() - 1); // 撤销选择
            used[i] = false;
        }
    }

    // ===== 3. 网格 DFS（LeetCode 200 岛屿数量）=====
    private static final int[][] DIRS = {{1,0}, {-1,0}, {0,1}, {0,-1}};

    public static int numIslandsDFS(char[][] grid) {
        if (grid == null || grid.length == 0) return 0;
        int m = grid.length, n = grid[0].length;
        int count = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == '1') {
                    count++;
                    dfsIsland(grid, i, j, m, n);
                }
            }
        }
        return count;
    }

    private static void dfsIsland(char[][] grid, int i, int j, int m, int n) {
        if (i < 0 || i >= m || j < 0 || j >= n || grid[i][j] == '0') return;
        grid[i][j] = '0';  // 沉岛，标记已访问
        for (int[] dir : DIRS) {
            dfsIsland(grid, i + dir[0], j + dir[1], m, n);
        }
    }

    // ===== 4. 回溯：组合（LeetCode 77）=====
    public static List<List<Integer>> combine(int n, int k) {
        List<List<Integer>> result = new ArrayList<>();
        backtrackCombine(1, n, k, new ArrayList<>(), result);
        return result;
    }

    private static void backtrackCombine(int start, int n, int k,
                                          List<Integer> path, List<List<Integer>> result) {
        if (path.size() == k) {
            result.add(new ArrayList<>(path));
            return;
        }
        for (int i = start; i <= n; i++) {
            path.add(i);
            backtrackCombine(i + 1, n, k, path, result);  // 注意是 i+1 不是 start+1
            path.remove(path.size() - 1);
        }
    }

    static class TreeNode {
        int val;
        TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }
}
```

### 5.6 动态规划模板

```java
import java.util.*;

/**
 * 动态规划模板
 *
 * 三要素：最优子结构、重叠子问题、状态转移方程
 * 五步法：
 * 1. 定义 dp 数组/函数的含义
 * 2. 找状态转移方程
 * 3. 确定初始条件/base case
 * 4. 确定遍历顺序
 * 5. 举例子验证
 */
public class DPTemplate {

    public static void main(String[] args) {
        // ===== 一维 DP =====
        System.out.println("爬楼梯: " + climbStairs(10));                      // 89
        System.out.println("打家劫舍: " + rob(new int[]{2,7,9,3,1}));         // 12
        System.out.println("最大子数组和: " + maxSubArray(new int[]{-2,1,-3,4,-1,2,1,-5,4})); // 6

        // ===== 二维 DP =====
        System.out.println("最长公共子序列: " + longestCommonSubsequence("abcde", "ace")); // 3

        // ===== 背包 DP =====
        System.out.println("01背包: " + knapsack01(new int[]{1,2,3}, new int[]{15,20,30}, 4)); // 45
        System.out.println("零钱兑换: " + coinChange(new int[]{1,2,5}, 11));    // 3
    }

    // ===== 1. 一维 DP：爬楼梯（LeetCode 70）=====
    // dp[i] = dp[i-1] + dp[i-2]（斐波那契）
    public static int climbStairs(int n) {
        if (n <= 2) return n;
        int prev2 = 1, prev1 = 2;   // dp[1]=1, dp[2]=2
        for (int i = 3; i <= n; i++) {
            int cur = prev1 + prev2;
            prev2 = prev1;
            prev1 = cur;
        }
        return prev1;
    }

    // ===== 2. 一维 DP：打家劫舍（LeetCode 198）=====
    // dp[i] = max(dp[i-1], dp[i-2] + nums[i])
    // 状态压缩：只用两个变量
    public static int rob(int[] nums) {
        if (nums == null || nums.length == 0) return 0;
        if (nums.length == 1) return nums[0];

        int prev2 = 0;               // dp[i-2]
        int prev1 = nums[0];         // dp[i-1]

        for (int i = 1; i < nums.length; i++) {
            int cur = Math.max(prev1, prev2 + nums[i]);
            prev2 = prev1;
            prev1 = cur;
        }
        return prev1;
    }

    // ===== 3. 一维 DP：最大子数组和（LeetCode 53）=====
    // dp[i] = max(nums[i], dp[i-1] + nums[i])，以 i 结尾的最大子数组和
    public static int maxSubArray(int[] nums) {
        int dp = nums[0];
        int max = nums[0];
        for (int i = 1; i < nums.length; i++) {
            dp = Math.max(nums[i], dp + nums[i]);  // 要么重新开始，要么延续前面
            max = Math.max(max, dp);
        }
        return max;
    }

    // ===== 4. 二维 DP：最长公共子序列（LeetCode 1143）=====
    // dp[i][j] = text1[0..i-1] 和 text2[0..j-1] 的 LCS 长度
    public static int longestCommonSubsequence(String text1, String text2) {
        int m = text1.length(), n = text2.length();
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (text1.charAt(i - 1) == text2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;           // 相等 → 加 1
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]); // 不等 → 取大
                }
            }
        }
        return dp[m][n];
    }

    // ===== 5. 01背包 =====
    // dp[i][w] = 前 i 个物品，容量 w 的最大价值
    // 空间优化：一维数组从后往前遍历
    public static int knapsack01(int[] weights, int[] values, int capacity) {
        int n = weights.length;
        int[] dp = new int[capacity + 1];

        for (int i = 0; i < n; i++) {
            // 一维数组必须从后往前！否则物品会被重复使用
            for (int w = capacity; w >= weights[i]; w--) {
                dp[w] = Math.max(dp[w], dp[w - weights[i]] + values[i]);
            }
        }
        return dp[capacity];
    }

    // ===== 6. 完全背包：零钱兑换（LeetCode 322）=====
    // 完全背包：一维数组从前往后遍历（物品可重复使用）
    public static int coinChange(int[] coins, int amount) {
        int[] dp = new int[amount + 1];
        Arrays.fill(dp, amount + 1);  // 初始化为不可能的大值
        dp[0] = 0;

        for (int i = 1; i <= amount; i++) {
            for (int coin : coins) {
                if (i >= coin) {
                    dp[i] = Math.min(dp[i], dp[i - coin] + 1);
                }
            }
        }
        return dp[amount] > amount ? -1 : dp[amount];
    }
}
```

### 5.7 位运算模板

```java
/**
 * 位运算常用技巧速查表
 *
 * 适用场景：状态压缩、集合操作、优化运算
 */
public class BitTemplate {

    public static void main(String[] args) {
        int a = 5, b = 3;  // a=101, b=011

        // ===== 基本运算 =====
        int and    = a & b;   // 001 → 1（按位与）
        int or     = a | b;   // 111 → 7（按位或）
        int xor    = a ^ b;   // 110 → 6（异或，相同为0不同为1）
        int not    = ~a;      // ...11111010（取反，含符号位）
        int left   = a << 1;  // 1010 → 10（左移1位 = 乘2）
        int right  = a >> 1;  // 010 → 2（右移1位 = 除2）
        int uRight = a >>> 1; // 无符号右移（高位补0）

        // ===== 常用技巧 =====

        // 1. 判断奇偶
        boolean isOdd = (a & 1) == 1;      // true（奇数）

        // 2. 获取二进制中第 k 位（从0开始）
        int k = 2;
        int bitK = (a >> k) & 1;           // a=101, 第2位=1

        // 3. 将第 k 位设为 1
        int setBit = a | (1 << k);         // 101 | 100 = 111 → 7

        // 4. 将第 k 位设为 0
        int clearBit = a & ~(1 << k);      // 101 & 011 = 001 → 1

        // 5. 翻转第 k 位
        int toggleBit = a ^ (1 << k);      // 101 ^ 100 = 001 → 1

        // 6. 判断是否为 2 的幂
        int n = 16;
        boolean isPowerOfTwo = n > 0 && (n & (n - 1)) == 0;  // true

        // 7. 统计 1 的个数（汉明重量）
        int count = Integer.bitCount(a);   // 内置 API，5(101) → 2
        // 手写版
        int bitCount = 0;
        for (int x = a; x != 0; x &= (x - 1)) bitCount++;  // 每次去掉最末位1

        // 8. 获取最末位的 1 的值
        int lowbit = a & (-a);             // 101 & 011(补码) = 001 → 1

        // 9. 集合操作（用 int 的位表示集合）
        // 假设集合元素范围 0-31
        int set = 0;
        // 添加元素 3
        set |= (1 << 3);
        // 添加元素 5
        set |= (1 << 5);
        // 判断元素 3 是否存在
        boolean contains3 = (set & (1 << 3)) != 0;  // true
        // 删除元素 3
        set &= ~(1 << 3);

        // 10. 状态压缩枚举
        // 枚举一个集合的所有子集
        int[] items = {1, 2, 3};
        int total = 1 << items.length;  // 8 个子集
        for (int mask = 0; mask < total; mask++) {
            // 二进制中 1 的位置对应选中的元素
            for (int i = 0; i < items.length; i++) {
                if ((mask & (1 << i)) != 0) {
                    System.out.print(items[i] + " ");
                }
            }
            System.out.println();
        }
        // 输出：[], [1], [2], [1,2], [3], [1,3], [2,3], [1,2,3]

        // ===== LeetCode 常见位运算题 =====
        // 136. 只出现一次的数字 → 全部异或
        // 191. 位1的个数 → Integer.bitCount()
        // 231. 2的幂 → n>0 && (n&(n-1))==0
        // 338. 比特位计数 → dp[i] = dp[i>>1] + (i&1)
        // 461. 汉明距离 → Integer.bitCount(x ^ y)
        System.out.println("\n=== 速查 ===");
        printCheatSheet();
    }

    public static void printCheatSheet() {
        String[][] tips = {
            {"判断奇偶",    "(n & 1) == 1"},
            {"2 的幂",     "n > 0 && (n & (n-1)) == 0"},
            {"1 的个数",   "Integer.bitCount(n)"},
            {"取末位 1",   "n & (-n)"},
            {"去掉末位 1",  "n & (n-1)"},
            {"异或交换",    "a ^= b; b ^= a; a ^= b;"},
            {"取反某位",    "n ^ (1 << k)"},
            {"设置某位为1", "n | (1 << k)"},
            {"清除某位",    "n & ~(1 << k)"},
            {"取模 2 的幂", "n & (mod - 1)  // 等价于 n % mod 当 mod 是 2 的幂"},
        };
        for (String[] tip : tips) {
            System.out.printf("%-10s → %s%n", tip[0], tip[1]);
        }
    }
}
```

---

## 6. 常见边界条件检查清单

> 写完代码后，逐项检查，养成肌肉记忆。

### 6.1 输入检查

| 检查项 | 代码示例 | 常见于 |
|:------|---------|--------|
| 数组/集合为 null | `if (nums == null || nums.length == 0)` | 所有输入 |
| 空集合/空字符串 | `s == null \|\| s.length() == 0` | 字符串处理 |
| 单个元素 | `if (nums.length == 1) return ...;` | 链表/数组 |
| 节点为 null | `if (node == null) return ...;` | 二叉树、链表 |

### 6.2 索引检查

| 检查项 | 代码示例 | 常见于 |
|:------|---------|--------|
| 数组越界 | `if (i < 0 \|\| i >= n) continue;` | 网格遍历 |
| 链表越界 | `while (fast != null && fast.next != null)` | 快慢指针 |
| 字符串越界 | `i < s.length()` | 字符串遍历 |
| 下标+1 溢出 | `int mid = left + (right - left) / 2;` | 二分查找 |

### 6.3 整数溢出

| 检查项 | 代码示例 | 常见于 |
|:------|---------|--------|
| 加法溢出 | `if (a > Integer.MAX_VALUE - b)` 不如用 long | 两数之和 |
| 乘法溢出 | `long product = (long) a * b;` | 字符串转整数 |
| 负数转正溢出 | `Math.abs(Integer.MIN_VALUE)` 仍为负数 | 位运算 |

### 6.4 其他常见边界

```text
✅ 树：root 为 null → 返回 0 / null / []
✅ 矩阵：行/列为 0 → 返回 0 / []
✅ 排序：全相同元素 → 检查去重逻辑
✅ 负值：target/路径和为负数 → 检查条件
✅ 大值：Integer.MAX_VALUE / MIN_VALUE → 防溢出
✅ 图：有环/自环 → visited 集合防死循环
✅ 递归：深度过大 → 栈溢出 → 考虑迭代
✅ 双指针：左指针超过右指针 → 停止
```

> 💡 **写代码时就在注释中标出边界**，比写完再检查更高效。

---

## 7. 调试技巧

### 7.1 本地 IDE 调试（推荐 IDEA）

```java
// 1. Debug 模式运行
// 2. 在关键行打断点（如循环入口、递归调用、返回语句）
// 3. F8 单步跳过，F7 单步进入，F9 跳到下一个断点
// 4. 在 Debug 面板中查看变量值，尤其关注：
//    - 循环变量（i, j, left, right）
//    - 集合内容（window, map, set）
//    - 递归参数
```

### 7.2 打印调试法（适合无法用 IDE 的场景）

```java
// 示例：调试滑动窗口
public void debugSlidingWindow(String s) {
    Set<Character> window = new HashSet<>();
    int left = 0;
    for (int right = 0; right < s.length(); right++) {
        char c = s.charAt(right);
        while (window.contains(c)) {
            window.remove(s.charAt(left));
            left++;
        }
        window.add(c);
        // 关键：打印每一步的窗口状态
        System.out.printf("left=%d, right=%d, window=%s, c=%c%n",
            left, right, window, c);
    }
}
```

### 7.3 边界构造法

构造测试用例的优先级：

```text
1. 正常例 → 最典型的输入
2. 最小例 → 输入最小（数组长度 1 / 树只有根节点）
3. 最大例 → 覆盖最大数据范围（测试性能，不一定在面试中跑）
4. 重复例 → 所有元素相同
5. 无解例 → 题目要求不存在解的情况
6. 负数例 → 包含负数
7. 空例 → null / 空集合
```

### 7.4 常见 Bug 排查方向

| Bug 现象 | 可能原因 |
|:---------|---------|
| 死循环 | 循环条件写错 / 变量没更新 / 递归没有终止条件 |
| 数组越界 | 下标搞错 / `i+1` 没检查边界 |
| 结果为空 | 初始值设错 / 条件判断反了 |
| 结果过多 | 去重逻辑缺失 / 重复计算 |
| 结果不正确 | 状态转移方程错了 / 边界条件漏了 |
| 栈溢出 | 递归深度太大 / 递归没有终止 |

---

## 8. 刷题记录模板

### 8.1 单题记录模板

将以下内容复制到自己的笔记中，每道题填一份：

```markdown
## [题号] 题目名称

### 信息
- **难度**：⭐ Easy / ⭐⭐ Medium / ⭐⭐⭐ Hard
- **标签**：数组 | 哈希表 | 双指针
- **LeetCode**：[链接](https://leetcode.com/problems/xxx)

### 一句话思路
（用一句话概括核心解法）

### 复杂度
- 时间：O(?)
- 空间：O(?)

### 代码
```java

```
```

### 容易错
1. 
2. 

### 同类题
- [题号] 题目名称（相似度原因）
```

### 8.2 每日复盘模板

```markdown
## 第 X 天复盘

### 今日完成
| 题号 | 题目 | 掌握度 | 备注 |
|:----:|------|:------:|------|
| 1 | 两数之和 | ★★★ | 一遍过 |
| 206 | 反转链表 | ★★☆ | 递归版忘了 |

### 今日收获
1. 学到了什么新技巧？
2. 哪个边界条件没注意到？
3. 哪个模板需要加强？

### 明日计划
- 3 道哈希表相关题
- 复习双指针模板
```

### 8.3 错题本索引表

```text
## 错题本

| 题号 | 题目 | 错因 | 下次复习 |
|:----:|------|------|:-------:|
| 42 | 接雨水 | 单调栈理解不深 | 2天后 |
| 72 | 编辑距离 | DP 初始化错 | 3天后 |
| 146 | LRU 缓存 | LinkedHashMap API 不熟 | 7天后 |
```

> 🎯 **复盘远比刷题本身重要**。每道题花 5 分钟复盘，这笔时间的 ROI 是最高的。

---

**下一模块**：[02 Hot100分类精讲-哈希与双指针](./02-Hot100分类精讲-哈希与双指针.md) | **返回总览**：[刷题总览](./00-LeetCode刷题总览.md)
