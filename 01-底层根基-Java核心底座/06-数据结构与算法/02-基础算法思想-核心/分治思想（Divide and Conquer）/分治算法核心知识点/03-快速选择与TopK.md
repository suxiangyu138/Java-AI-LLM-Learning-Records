# 03 - 快速选择与 TopK

> 定位：TopK 问题的完整解法体系——快速选择（期望 O(n)）、堆解法、排序解法三足鼎立，含前 K 高频与最近 K 点变体

## 📚 目录

1. [TopK 问题全景](#1-topk-问题全景)
2. [快速选择原理：期望 O(n) 的直觉](#2-快速选择原理期望-on-的直觉)
3. [215. 第 K 大（快选完整实现）](#3-215-第-k-大快选完整实现)
4. [堆解法：N 个元素维护 K 个](#4-堆解法n-个元素维护-k-个)
5. [347. 前 K 个高频元素](#5-347-前-k-个高频元素)
6. [973. 最接近原点的 K 个点](#6-973-最接近原点的-k-个点)
7. [BFPRT：确定性 O(n) 的思想](#7-bfprt确定性-on-的思想)
8. [三种解法对比与选型](#8-三种解法对比与选型)
9. [面试要点与追问](#9-面试要点与追问)

---

## 1. TopK 问题全景

### 1.1 题型家族

| 题型 | 代表题 | 核心 |
|------|--------|------|
| 第 K 大/小 | 215 数组第 K 大 | 快选 / 堆 |
| 前 K 高频 | 347 元素频率 TopK | 哈希 + 快选/桶/堆 |
| 最接近 K | 973 距离原点最近 | 快选变体（距离做 key） |
| 有序结构第 K | 378 矩阵第 K 小 | 堆 / 二分答案 |
| 数据流 TopK | 703 数据流第 K 大 | 堆（在线） |
| TopK 总和 | 373 最小 K 对 | 堆（多路归并） |

```text
⚠️ 识别信号："第 K 大/小、前 K 个、TopK" → 本题族
关键区别：需要"前 K"（有序）还是"第 K"（只一个）？
  → 前 K 有序用堆；第 K 单值用快选
```

### 1.2 三条解法路线

```text
路线一：排序 O(n log n)（最笨，面试只能当保底）
路线二：堆 O(n log k)（k 小优势大，在线数据流的唯一解）
路线三：快选 O(n) 期望（数组可随意交换时最优）

⚠️ 面试表达：
"TopK 三解法：排序 O(n log n) 保底、堆 O(n log k)
 适合数据流和 k 很小的场景、快选期望 O(n) 适合
 静态数组——我会根据场景选。"
```

---

## 2. 快速选择原理：期望 O(n) 的直觉

### 2.1 与快排的关系

```text
快排：partition 后两侧都递归 → O(n log n)
快选：partition 后只递归包含答案的一侧 → 期望 O(n)

⚠️ 本质：快选 = 快排 + 二分（单侧分治）
  每次 partition 缩小一半搜索空间（期望）
```

### 2.2 期望 O(n) 的数学直觉

```text
每次 partition 代价 O(n)，期望把问题缩小一半：
  T(n) = T(n/2) + O(n)
  = n + n/2 + n/4 + ... ≈ 2n = O(n) ✓

⚠️ 最坏 O(n²)：每次 partition 都恰好分出 1 个元素
  （有序输入 + 固定基准）→ 随机化消除

⚠️ 面试必答：
"快选每次 partition 期望砍半搜索空间，
 几何级数求和 n + n/2 + n/4 + ... = O(n)。
 随机化基准后最坏 O(n²) 概率趋近 0。"
```

### 2.3 快选的三种姿势

| 姿势 | 做法 | 适用 |
|------|------|------|
| 数组原地 | partition 交换，返回基准位 | 静态数组（215） |
| 下标索引 | 带 index 排序（315 的计数基础） | 需要保留位置 |
| 值域二分 | 二分答案 + 计数（378） | 有序结构/答案可枚举 |

---

## 3. 215. 第 K 大（快选完整实现）

### 3.1 思路

```text
第 K 大 = 第 (n-K) 小（0 基）
partition 返回基准最终位置 p：
  p == target → 答案
  p > target  → 去左侧
  p < target  → 去右侧
```

### 3.2 完整代码

```java
/**
 * LeetCode 215. 数组中的第 K 个最大元素（快选版）
 * 期望 O(n)，最坏 O(n²)（随机化后概率趋近 0）
 */
public int findKthLargest(int[] nums, int k) {
    int target = nums.length - k;          // ⚠️ 第 K 大 = 第 n-K 小
    int lo = 0, hi = nums.length - 1;
    Random rand = new Random();

    while (lo <= hi) {                     // 迭代版（防栈溢出）
        int p = partition(nums, lo, hi, rand);
        if (p == target) return nums[p];
        if (p < target) lo = p + 1;        // 答案在右侧
        else            hi = p - 1;        // 答案在左侧
    }
    return -1;
}

private int partition(int[] nums, int lo, int hi, Random rand) {
    int pivotIdx = lo + rand.nextInt(hi - lo + 1);   // ⚠️ 随机化
    swap(nums, pivotIdx, hi);                        // 基准换到末尾
    int pivot = nums[hi];
    int i = lo;
    for (int j = lo; j < hi; j++) {                  // Lomuto 分区
        if (nums[j] <= pivot) swap(nums, i++, j);
    }
    swap(nums, i, hi);                               // 基准归位
    return i;
}

private void swap(int[] a, int i, int j) {
    int t = a[i]; a[i] = a[j]; a[j] = t;
}
// 推演 [3,2,1,5,6,4], k=2 → target=4
//   partition → p=3（pivot=5? 随机）... 期望每次砍半
// ⚠️ 三要点：target = n-k、只递归一侧、随机化基准
```

### 3.3 对比：堆版（O(n log k)）

```java
public int findKthLargestHeap(int[] nums, int k) {
    // 小顶堆维护前 K 大：堆顶就是第 K 大
    PriorityQueue<Integer> heap = new PriorityQueue<>(k);
    for (int x : nums) {
        heap.offer(x);
        if (heap.size() > k) heap.poll();   // 弹出最小 → 堆内保留前 K 大
    }
    return heap.peek();
}
// 时间 O(n log k)，空间 O(k)
// ⚠️ 场景：k 很小、数据流（不能交换数组）时堆是唯一选择
```

---

## 4. 堆解法：N 个元素维护 K 个

### 4.1 大小顶堆的选择

```text
求第 K 大 / 前 K 大 → 小顶堆（堆顶是最小，弹出小值，留大值）
求第 K 小 / 前 K 小 → 大顶堆（堆顶是最大，弹出大值，留小值）

口诀："留什么就堆什么的反面"
  前 K 大：小顶堆（堆顶是第 K 大）✓
  前 K 小：大顶堆（堆顶是第 K 小）✓
```

### 4.2 堆解的进阶：数据流（703）

```java
/**
 * LeetCode 703. 数据流中第 K 大元素
 * ⚠️ 堆是"在线"解法：元素不断到来，无法重排数组
 */
class KthLargest {
    private final int k;
    private final PriorityQueue<Integer> heap = new PriorityQueue<>();

    public KthLargest(int k, int[] nums) {
        this.k = k;
        for (int x : nums) add(x);          // 复用 add 逻辑
    }

    public int add(int val) {
        heap.offer(val);
        if (heap.size() > k) heap.poll();   // 超过 k 弹最小
        return heap.peek();                 // 堆顶 = 第 K 大
    }
}
// ⚠️ 快选做不到在线：数据流必须堆（每次 O(log k)）
```

### 4.3 堆 vs 快选的关键分界

| 场景 | 快选 | 堆 |
|------|:---:|:---:|
| 静态数组 | ✅ O(n) 期望 | O(n log k) |
| 数据流/在线 | ❌ | ✅ O(log k)/次 |
| 需要前 K 有序输出 | 需再排序 | ✅ 堆顶逐个弹出 |
| k ≈ n/2 | O(n) 更快 | O(n log n) |
| 数组可修改 | ✅ 原地交换 | ✅ 不修改原数组 |

---

## 5. 347. 前 K 个高频元素

### 5.1 思路

```text
两阶段：
  ① 哈希统计频率（O(n)）
  ② 频率 TopK：堆 O(n log k) 或 桶排序 O(n) 或 快选 O(n)

⚠️ 桶排序版是 O(n) 但需要"频率值域"已知（≤ n）
```

### 5.2 桶排序版（最优 O(n)）

```java
/**
 * LeetCode 347. 前 K 个高频元素 —— 桶排序版
 * 频率范围 1..n → 建 n 个桶，桶 i 存频率为 i 的元素
 */
public int[] topKFrequent(int[] nums, int k) {
    Map<Integer, Integer> freq = new HashMap<>();
    for (int x : nums) freq.merge(x, 1, Integer::sum);    // ① 统计

    List<Integer>[] buckets = new List[nums.length + 1];  // ② 桶
    for (var e : freq.entrySet()) {
        int f = e.getValue();
        if (buckets[f] == null) buckets[f] = new ArrayList<>();
        buckets[f].add(e.getKey());
    }

    int[] res = new int[k];
    int idx = 0;
    for (int f = buckets.length - 1; f >= 1 && idx < k; f--) {  // ③ 从高频桶取
        if (buckets[f] != null)
            for (int v : buckets[f]) res[idx++] = v;
    }
    return res;
}
// 时间 O(n)（桶是数组下标定位），空间 O(n)
// ⚠️ 快选版：对频率数组做快选，期望 O(n)；堆版 O(n log k)
// ⚠️ 变体 692：前 K 高频单词（频率同则字典序 → 排序规则进比较器）
```

---

## 6. 973. 最接近原点的 K 个点

### 6.1 思路

```text
距离 = x² + y²（不用开方，单调性保留）
→ 变为"求距离数组的第 K 小"→ 快选直接套

⚠️ 快选的关键优势：不用算全距离排序——只排到第 K 个
  期望 O(n) vs 排序 O(n log n)
```

### 6.2 完整代码（快选变体）

```java
/**
 * LeetCode 973. 最接近原点的 K 个点
 */
public int[][] kClosest(int[][] points, int k) {
    quickSelect(points, 0, points.length - 1, k);
    return Arrays.copyOf(points, k);        // 前 K 个即是答案（无序）
}

private void quickSelect(int[][] pts, int lo, int hi, int k) {
    if (lo >= hi) return;
    int p = partition(pts, lo, hi);
    if (p == k) return;                     // 恰好前 K 归位
    if (p < k)  quickSelect(pts, p + 1, hi, k);
    else        quickSelect(pts, lo, p - 1, k);
}

private int partition(int[][] pts, int lo, int hi) {
    int pivot = dist(pts[hi]);              // 基准距离
    int i = lo;
    for (int j = lo; j < hi; j++) {
        if (dist(pts[j]) <= pivot) swap(pts, i++, j);   // 按距离分区
    }
    swap(pts, i, hi);
    return i;
}

private int dist(int[] p) { return p[0] * p[0] + p[1] * p[1]; }
// ⚠️ 核心：把"点"的排序标准换成距离函数，快选模板不变
//   前 K 个点无序即可（题目不要求按距离排序输出）
```

> 🎯 **通用化**：任何"可比较 key 的第 K 个"都能套快选——数字（215）、频率（347）、距离（973）、字典序（692）。**把比较函数抽象出来，快选模板就是万能的**。

---

## 7. BFPRT：确定性 O(n) 的思想

### 7.1 快选为什么"期望"而非"确定"

```text
快选的 O(n) 依赖随机基准的"平均表现"
最坏（每次分 1 个）仍是 O(n²)——只是概率趋近 0

BFPRT（Blum-Floyd-Pratt-Rivest-Tarjan，中位数的中位数）：
  保证每次 partition 至少去掉 30% 元素 → 确定 O(n)
```

### 7.2 算法五步（思想层面）

```text
① 每 5 个元素一组，组内排序（5 个元素排序常数级）
② 取每组中位数，组成中位数数组
③ 递归求中位数数组的中位数（作为基准 pivot）
④ 用这个"足够好"的 pivot 分区
⑤ 保证：至少有 3n/10 元素 ≤ pivot、3n/10 ≥ pivot
   → 每轮至少缩小 30% → T(n) = T(7n/10) + O(n) → O(n)

⚠️ 现实评估：
  常数巨大（每层做 5 分组排序）
  工程几乎不用（随机化快选又快又简单）
  面试价值：展示"最坏情况的确定性保证"的思维
```

> 🎯 **面试表达**："BFPRT 用中位数的中位数做基准，保证每轮去掉 30% 元素，确定 O(n)；但常数大，工程上随机化快选就够——BFPRT 的价值是理论保证。"

---

## 8. 三种解法对比与选型

| 解法 | 时间 | 空间 | 稳定性 | 场景 |
|------|:---:|:---:|:---:|------|
| 排序 | O(n log n) | O(1) | 视排序 | 需要全序/保底 |
| 堆 | O(n log k) | O(k) | 无 | 数据流、k 小、在线 |
| 快选 | O(n) 期望 | O(log n) | 无 | 静态数组、只需要第 K |

```text
选型决策链：
  数据流/在线？────── 是 → 堆
      ↓ 否
  k 很小（k << n）？── 是 → 堆（log k 小）
      ↓ 否
  需要前 K 有序？──── 是 → 堆（弹出即有序）/ 排序
      ↓ 否
  静态数组可交换？── 是 → 快选（O(n) 期望）
      ↓ 否
  只能排序
```

---

## 9. 面试要点与追问

### 9.1 面试话术模板

```text
"第 K 大用快选：partition 后只递归包含答案的一侧，
 期望 O(n)（n + n/2 + n/4 + ... 几何求和）。
 随机化基准防退化；如果数据是流式的，
 改用堆维护前 K 个（O(n log k)）。"
```

### 9.2 高频追问 TOP 8

| # | 追问 | 标准回答 |
|:---:|------|------|
| 1 | 快选为什么 O(n)？ | 每次期望砍半 → 几何级数求和 |
| 2 | 快选最坏？ | O(n²)：每轮只分 1 个；随机化后概率趋近 0 |
| 3 | 第 K 大转第 K 小？ | target = n - k（0 基） |
| 4 | 堆怎么选大小顶？ | 前 K 大用小顶堆（留大弹小） |
| 5 | 数据流 TopK？ | 只能堆（无法重排），O(log k)/次 |
| 6 | 前 K 高频？ | 哈希统计 + 桶排序/快选/堆 |
| 7 | BFPRT？ | 中位数中位数做基准，确定 O(n)，常数大 |
| 8 | 快选与快排区别？ | 快排两侧递归；快选单侧递归（+二分） |

### 9.3 常见错误

| 错误 | 后果 | 修正 |
|------|------|------|
| 快选两侧都递归 | 退化为快排 O(n log n) | 只递归含答案侧 |
| target 忘转换（K 大 K 小） | 答案错 | n - k |
| 堆顶方向选反 | 得到最小 K | 前 K 大 → 小顶堆 |
| 固定基准 + 有序输入 | O(n²) | 随机化 |
| 快选用 while 忘更新边界 | 死循环 | lo/hi 明确推进 |

---

> 🎯 **核心要点**：TopK = 快选（期望 O(n)，单侧分治）+ 堆（O(n log k)，在线/小 k）+ 排序（保底）。比较函数抽象化后，快选模板可套 215/347/973/692 全家。BFPRT 是理论补充（确定 O(n) 但工程不用）。

---

**返回总览**：[00-分治知识体系总览](00-分治知识体系总览.md) | **上一篇**：[02-归并与快排：排序分治双雄](02-归并与快排：排序分治双雄.md) | **下一篇**：[04-跨边界分治与归并计数](04-跨边界分治与归并计数.md)
