# 堆进阶：TopK双堆与延迟删除
> 从"会用堆"到"能设计堆方案"——TopK 选型、双堆平衡与延迟删除

## 📚 目录
1. [堆的完整应用体系](#1-堆的完整应用体系)
2. [TopK 问题的双解法](#2-topk-问题的双解法)
3. [双堆平衡机制](#3-双堆平衡机制)
4. [延迟删除技巧](#4-延迟删除技巧)
5. [堆的实现原理](#5-堆的实现原理)
6. [进阶总结](#6-进阶总结)

---

## 1. 堆的完整应用体系

### 1.1 堆的角色

```
① TopK：定长堆（堆顶 = 第 K 大/小）
② 调度/贪心：每次取当前最优（Dijkstra/合并 K 链）
③ 数据流：双堆（中位数）
④ 延迟删除：堆 + 标记（窗口删除）

⚠️ 识别信号：
  "求最大/最小 K 个" → TopK
  "每次取最优" → 调度
  "数据流统计" → 双堆
```

### 1.2 复杂度认知

```
取最值 peek：O(1)
插入 offer：O(log n)
删除 poll：O(log n)
建堆：O(n)（从 n/2-1 起下沉）

⚠️ 面试必答：
"堆的核心：O(1) 取最值 + O(log n) 增删。
 建堆 O(n) 而非 O(n log n)（底层下沉少）。"
```

### 1.3 识别信号

```
"第 K 大/前 K 个" → 堆或快选
"数据流/在线" → 堆（不能排序）
"中位数" → 双堆
"窗口内删除" → 延迟删除

⚠️ 选型：
  静态数组 → 快选更快（O(n)）
  数据流 → 堆（在线）
```

---

## 2. TopK 问题的双解法

### 2.1 堆解法（O(n log k)）

```java
/**
 * LeetCode 215. 数组中的第 K 个最大元素（堆版）
 * 小根堆维护"最大的 k 个"：堆顶 = 第 k 大
 */
public int findKthLargest(int[] nums, int k) {
    PriorityQueue<Integer> minHeap = new PriorityQueue<>();

    for (int x : nums) {
        minHeap.offer(x);
        if (minHeap.size() > k) {
            minHeap.poll();          // 弹出最小的 → 留最大的 k 个
        }
    }
    return minHeap.peek();           // 堆顶 = 第 k 大
}
// 复杂度 O(n log k)：插入 O(log k) × n 次
// 空间 O(k)
// ⚠️ 记忆："求大的留小堆（堆顶是被淘汰候选）"
```

### 2.2 快速选择（O(n) 平均）

```java
/**
 * 快速选择：partition 后只递归一侧
 * 平均 O(n)（比堆快）
 */
public int findKthLargest_QuickSelect(int[] nums, int k) {
    return quickSelect(nums, 0, nums.length - 1, nums.length - k);
}

private int quickSelect(int[] arr, int lo, int hi, int target) {
    if (lo == hi) return arr[lo];

    // ⚠️ 随机基准（防有序数组退化）
    int idx = lo + new Random().nextInt(hi - lo + 1);
    swap(arr, idx, hi);

    int pivot = arr[hi], i = lo - 1;
    for (int j = lo; j < hi; j++) {
        if (arr[j] < pivot) swap(arr, ++i, j);
    }
    swap(arr, i + 1, hi);

    if (i + 1 == target) return arr[i + 1];
    else if (i + 1 < target) return quickSelect(arr, i + 2, hi, target);
    else return quickSelect(arr, lo, i, target);
}
// 平均 O(n)：n + n/2 + n/4... = 2n（等比求和）
// ⚠️ 会修改数组（原地 partition）
```

### 2.3 双解法选型

| 维度 | 堆 | 快速选择 |
|------|:---:|:---:|
| 平均复杂度 | O(n log k) | **O(n)** |
| 最坏 | O(n log k) 稳定 | O(n²)（随机化缓解） |
| 空间 | O(k) | O(log n) 递归 |
| 数据流 | ✅ 在线 | ❌ 需全量 |
| 修改数组 | 不修改 | 原地交换 |

```
⚠️ 面试必答：
"静态数组 → 快速选择（更快）；
 数据流/在线 → 堆（不能排序）。
 两种都能写 = 加分。"
```

---

## 3. 双堆平衡机制

### 3.1 双堆原理

```
大根堆（左半）+ 小根堆（右半）：
  左半存较小的一半（大根堆顶 = 左半最大）
  右半存较大的一半（小根堆顶 = 右半最小）
  保持 size 差 ≤ 1
  → 两个堆顶夹住中位数

⚠️ 为什么左半用大根堆？
  中位数是"左右分界处"：
  左半最大 + 右半最小 = 分界点
```

### 3.2 295. 数据流中位数

```java
/**
 * LeetCode 295. 数据流的中位数（双堆经典）
 */
class MedianFinder {
    PriorityQueue<Integer> left  = new PriorityQueue<>((a, b) -> b - a);  // 大根堆
    PriorityQueue<Integer> right = new PriorityQueue<>();                 // 小根堆

    public void addNum(int num) {
        left.offer(num);                 // ① 先入左半
        right.offer(left.poll());        // ② 左半最大给右半（平衡分界）
        if (left.size() < right.size()) {
            left.offer(right.poll());    // ③ 保持左 ≥ 右
        }
    }

    public double findMedian() {
        if (left.size() > right.size()) {
            return left.peek();          // 奇数：左堆顶
        }
        return (left.peek() + right.peek()) / 2.0;   // 偶数：平均
    }
}
// 推演 add 1,2,3：
//   1 → left=[1]
//   2 → left=[1,2] → 右 2 → left=[1], right=[2] → 平衡
//   3 → left=[1,3] → 右 3 → left=[1], right=[2,3] → 左小 → 还 2
//      → left=[1,2], right=[3]
//   中位数 = left.peek() = 2 ✓
// ⚠️ 插入 O(log n)，查询 O(1)
// ⚠️ 三步平衡顺序不能乱（入左 → 给右 → 还左）
```

### 3.3 双堆的其他应用

```
① 480 滑动窗口中位数：双堆 + 延迟删除
② 数据流 TopK：定长堆
③ 双堆选型：
   "中位数/有序统计" → 双堆
   "最值" → 单调队列/单堆

⚠️ 面试必答：
"双堆平衡三步：入左 → 左顶给右 → 右多还左。
 差 ≤ 1 保证中位数在两堆顶之间。"
```

---

## 4. 延迟删除技巧

### 4.1 问题背景

```
堆无法 O(1) 删除任意元素：
  poll 只删堆顶 O(log n)
  删任意元素需要 find O(n) + 删 O(log n)

⚠️ 需要场景：
  滑动窗口滑出元素（480）
  数据流删除（295 变体）
```

### 4.2 延迟删除原理

```
① 标记待删：map.merge(val, 1, +1)（记录待删次数）
② 弹出时验证：堆顶在待删表中 → 弹出并减计数
③ 重复直到堆顶是"有效"元素

⚠️ 每个元素"假删"一次 + "真删"一次
  → 均摊 O(log n)
```

### 4.3 延迟删除模板

```java
/**
 * 延迟删除通用模板
 */
public class LazyHeap {
    private PriorityQueue<Integer> heap;
    private Map<Integer, Integer> lazy = new HashMap<>();

    // 标记删除（不真正删）
    public void lazyRemove(int val) {
        lazy.merge(val, 1, Integer::sum);
    }

    // 弹出"有效"的堆顶（清理被标记的）
    public int pollValid() {
        while (!heap.isEmpty()
               && lazy.getOrDefault(heap.peek(), 0) > 0) {
            int val = heap.poll();           // 真正删除
            lazy.merge(val, -1, Integer::sum);
        }
        return heap.isEmpty() ? null : heap.poll();
    }

    public Integer peekValid() {
        while (!heap.isEmpty()
               && lazy.getOrDefault(heap.peek(), 0) > 0) {
            int val = heap.poll();
            lazy.merge(val, -1, Integer::sum);
        }
        return heap.isEmpty() ? null : heap.peek();
    }
}
// ⚠️ 面试必答：
//   "延迟删除：标记待删（Map 计数）+ 弹出时
//    惰性验证。堆不支持 O(1) 删任意元素，
//    延迟删除均摊 O(log n)。"
```

---

## 5. 堆的实现原理

### 5.1 完全二叉树的数组表示

```
数组存储完全二叉树：
  左孩子 = 2i+1，右孩子 = 2i+2，父 = (i-1)/2

⚠️ 为什么数组？
  完全二叉树无空洞 → 数组紧凑存储
  下标运算替代指针（省空间）
```

### 5.2 上浮与下沉

```java
/**
 * 手写堆核心操作（了解原理）
 */
// 上浮（插入）：新元素与父比较，大则交换
private void siftUp(int i) {
    while (i > 0) {
        int parent = (i - 1) / 2;
        if (heap[i] <= heap[parent]) break;
        swap(i, parent);
        i = parent;
    }
}

// 下沉（删除/建堆）：与较大孩子比较，小则交换
private void siftDown(int i) {
    while (2 * i + 1 < size) {
        int child = 2 * i + 1;
        if (child + 1 < size && heap[child + 1] > heap[child]) {
            child++;                       // 选较大孩子
        }
        if (heap[i] >= heap[child]) break;
        swap(i, child);
        i = child;
    }
}
// ⚠️ 插入：尾部放入 → siftUp
// 删除：堆顶与尾部交换 → siftDown
```

### 5.3 建堆为什么 O(n)

```
深度 d 的节点最多下沉 h-d 次
第 d 层有 n/2^(d+1) 个节点
总下沉 = Σ n/2^(d+1) × (h-d) ≈ 2n → O(n)

⚠️ 面试必答：
"建堆从 n/2-1 开始下沉——底层节点
 下沉次数少（Σ 等比 ≈ 2n），O(n)。
 插入是上浮（每次 O(log n)），O(n log n)。"
```

---

## 6. 进阶总结

### 6.1 进阶要点速查

```
① 应用体系：TopK/调度/双堆/延迟删除
② TopK 选型：静态快选 O(n)；流式堆 O(n log k)
③ 双堆：三步平衡（入左→给右→还左）
④ 延迟删除：标记 + 惰性验证（均摊 O(log n)）
⑤ 实现：数组表示 + 上浮/下沉 + 建堆 O(n)
```

### 6.2 面试话术模板

```
"堆的应用：
 ① TopK【定长堆：求大留小堆】；
 ② 双堆【三步平衡：入左→给右→还左】；
 ③ 延迟删除【标记 + 惰性验证】；
 ④ 复杂度【peek O(1)/增删 O(log n)/建堆 O(n)】。
 选型：静态快选、流式堆。"
```

### 6.3 易错点清单

| 错误 | 后果 | 修正 |
|------|------|------|
| 比较器写反 | 大小根颠倒 | b-a 大根堆 |
| 双堆顺序乱 | 分界错 | 三步固定 |
| 延迟删除忘验证 | 返回已删值 | 弹出时检查 |
| 建堆起点错 | 结果错 | n/2-1 |
| 静态用堆 | 不够快 | 快选 O(n) |

> 🎯 **核心要点**：堆进阶 = **TopK 双解法**（堆/快选选型）+ **双堆平衡**（三步机制）+ **延迟删除**（标记+惰性验证）+ **实现原理**（建堆 O(n)）。"求大留小堆"与"三步平衡"是两个必背口诀；延迟删除是滑动窗口类题的进阶武器。

---

**返回总览**：[00-算法汇总知识体系总览](../00-算法汇总知识体系总览.md) | **上一篇**：[00-堆（优先队列）专题精要](00-堆（优先队列）专题精要.md) | **下一篇**：[02-堆（优先队列）高频题解](02-堆（优先队列）高频题解.md)
