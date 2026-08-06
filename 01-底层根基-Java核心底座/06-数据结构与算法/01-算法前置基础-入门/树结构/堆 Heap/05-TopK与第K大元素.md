# 05 - TopK 与第 K 大元素

> TopK 是堆的最高频考点：小顶堆装大数、大顶堆装小数、K 大小堆 O(n log K)——与快速选择对比、海量数据场景、八大变体一网打尽。

## 📚 目录

1. [TopK 核心套路](#1)
2. [第 K 大元素：堆解法](#2)
3. [堆 vs 快速选择](#3)
4. [海量数据 TopK](#4)
5. [TopK 八大变体](#5)

## 1. TopK 核心套路

**口诀：求前 K 大用小顶堆，求前 K 小用大顶堆。**

| 问题 | 堆类型 | 堆大小 | 堆顶含义 |
|------|--------|--------|---------|
| 前 K 大 | **小顶堆** | K | 门槛：第 K 大 |
| 前 K 小 | **大顶堆** | K | 门槛：第 K 小 |
| 第 K 大 | 小顶堆 | K | 答案 |
| 第 K 小 | 大顶堆 | K | 答案 |

**为什么反着选**：堆里装「前 K 大」，新元素比堆顶（当前最小门槛）大就踢掉堆顶换新——堆顶永远是「第 K 大」的门槛。

```java
// 通用模板：前 K 大（小顶堆，K 大小）
PriorityQueue<Integer> heap = new PriorityQueue<>(k);   // 小顶堆
for (int x : nums) {
    if (heap.size() < k) heap.offer(x);
    else if (x > heap.peek()) { heap.poll(); heap.offer(x); }   // 踢门槛换新
}
// heap 里就是前 K 大，peek() 是第 K 大
```

## 2. 第 K 大元素：堆解法

```java
// LeetCode 215 数组中的第 K 个最大元素
public int findKthLargest(int[] nums, int k) {
    PriorityQueue<Integer> heap = new PriorityQueue<>(k);
    for (int num : nums) {
        if (heap.size() < k) {
            heap.offer(num);
        } else if (num > heap.peek()) {
            heap.poll();
            heap.offer(num);
        }
    }
    return heap.peek();   // 堆顶 = 第 K 大
}
```

| 步骤 | 操作 | 复杂度 |
|------|------|--------|
| 遍历 n 个元素 | 每个至多一次替换 | O(n) × O(log K) |
| 总复杂度 | **O(n log K)** | 空间 O(K) |

> 💡 当 K 很小（如 K=1 求最大）时堆退化：直接一趟扫描 O(n) 即可；K 与 n 同量级时堆无优势，考虑快速选择。

## 3. 堆 vs 快速选择

| 维度 | 堆（小顶堆） | 快速选择 |
|------|-------------|---------|
| 时间复杂度 | O(n log K) | 期望 O(n)，最坏 O(n²) |
| 空间 | O(K) | O(1)（迭代版） |
| 数据动态性 | **支持动态插入**（数据流） | 静态数组一次性 |
| 稳定性 | 稳定输出前 K 个（有序） | 只保证第 K 位置 |
| 适合 | 海量数据、数据流、需要 TopK 全量 | 静态数组第 K 大、性能敏感 |

**选型决策**：
- 静态数组求第 K → 快速选择（期望 O(n) 更快）；
- 数据流/海量/需要前 K 个全量 → 堆；
- 面试优先答堆（模板固定、不会写出 O(n²) 最坏）。

## 4. 海量数据 TopK

**10 亿条数据取 Top100**：内存装不下全量？

```text
方案一：堆（最优）
  - 只维护 K=100 的小顶堆，遍历一遍，O(n log 100)
  - 空间 O(100)，内存无压力 ✓ 推荐

方案二：分桶/分片 + 归并
  - 数据按哈希分桶 → 每桶堆取 Top100 → 归并
  - 适合分布式（MapReduce 的经典 TopK 模式）

方案三：外部排序
  - 全量外排后取前 K——杀鸡用牛刀，一般不选
```

**数据流 TopK（动态场景）**：

```java
// 295 数据流中位数：双堆模板（大顶堆存左半，小顶堆存右半）
class MedianFinder {
    PriorityQueue<Integer> left = new PriorityQueue<>(Comparator.reverseOrder());  // 大顶
    PriorityQueue<Integer> right = new PriorityQueue<>();                          // 小顶

    public void addNum(int num) {
        if (left.isEmpty() || num <= left.peek()) left.offer(num);
        else right.offer(num);
        // 平衡：left 最多比 right 多 1
        if (left.size() > right.size() + 1) right.offer(left.poll());
        if (right.size() > left.size()) left.offer(right.poll());
    }

    public double findMedian() {
        if (left.size() > right.size()) return left.peek();
        return (left.peek() + right.peek()) / 2.0;
    }
}
```

## 5. TopK 八大变体

| # | 变体 | 堆思路 | 典型题 |
|---|------|--------|--------|
| 1 | 数组第 K 大 | K 大小顶堆 | 215 |
| 2 | 前 K 高频元素 | 统计频次 + K 大小顶堆（按频次） | 347 |
| 3 | 前 K 高频单词 | 频次 + 字典序双比较器 | 692 |
| 4 | 合并 K 个有序链表 | 每链表头入堆，弹出后推后继 | 23 |
| 5 | 有序矩阵第 K 小 | 每行指针入堆，弹出推进 | 378 |
| 6 | 数据流中位数 | 双堆对半分 | 295 |
| 7 | 滑动窗口最大值 | 单调队列（堆不能删任意元素） | 239 |
| 8 | 任务调度器 | 频次堆 + 冷却队列 | 621 |

```java
// 347 前 K 个高频元素：频次统计 + 小顶堆
public int[] topKFrequent(int[] nums, int k) {
    Map<Integer, Integer> freq = new HashMap<>();
    for (int n : nums) freq.merge(n, 1, Integer::sum);
    // 小顶堆按频次排序，只留 K 个
    PriorityQueue<Integer> heap = new PriorityQueue<>(
        (a, b) -> freq.get(a) - freq.get(b));
    for (int key : freq.keySet()) {
        heap.offer(key);
        if (heap.size() > k) heap.poll();
    }
    return heap.stream().mapToInt(Integer::intValue).toArray();
}
```

> 🎯 **核心要点**：TopK 验收——「前 K 大装小堆」口诀 + 通用模板默写；变体识别：统计类（347）先 Map 后堆、归并类（23/378）堆存指针、数据流类（295）双堆对半分、窗口类（239）改用单调队列；海量数据面试题的标准开场白：「只维护 K 大小的堆，空间 O(K)」。

---

**上一模块**：[04-堆排序与建堆](04-堆排序与建堆.md) ｜ **下一模块**：[06-双堆技巧与面试实战](06-双堆技巧与面试实战.md) ｜ **返回总览**：[00-堆 Heap 知识体系总览](00-堆 Heap 知识体系总览.md)

**【参考来源】**
- LeetCode 215 / 347 / 295 / 23 / 378
- 算法导论（CLRS）第 9 章：中位数与顺序统计
