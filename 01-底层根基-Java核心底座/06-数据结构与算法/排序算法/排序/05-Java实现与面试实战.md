# Java实现与面试实战
> 手撕三件套演练 + 高频追问 TOP 10——排序是唯一"手写代码 + 理论深度"双重考察的题型

## 📚 目录
1. [LeetCode 排序相关题映射](#1-leetcode-排序相关题映射)
2. [手撕演练：快排完整版（面试标准答案）](#2-手撕演练快排完整版面试标准答案)
3. [手撕演练：归并 + 堆排](#3-手撕演练归并--堆排)
4. [高频追问 TOP 10](#4-高频追问-top-10)
5. [面试解题话术模板](#5-面试解题话术模板)
6. [调试技巧与自检清单](#6-调试技巧与自检清单)

---

## 1. LeetCode 排序相关题映射

| 题号 | 题目 | 考察点 | 难度 | 推荐解法 |
|:---:|------|------|:---:|------|
| 912 | 排序数组 | 手撕任意排序 | 🟡 | 快排/归并 |
| 215 | 数组第 K 大 | TopK | 🟡 | 堆 / 快速选择 |
| 347 | 前 K 个高频元素 | TopK+计数 | 🟡 | 堆 / 桶排序 |
| 451 | 根据字符频率排序 | 桶思想 | 🟡 | 桶排序 |
| 493 | 翻转对 | 归并应用 | 🔴 | 归并统计 |
| 剑指51 | 逆序对 | 归并应用 | 🔴 | 归并统计 |
| 148 | 排序链表 | 归并+链表 | 🟡 | 归并 |
| 75 | 颜色分类 | 三路快排 | 🟡 | 双指针/三路 |
| 56 | 合并区间 | 排序+扫描 | 🟡 | 按起点排序 |
| 324 | 摆动排序 II | 快选+重排 | 🟡 | 快速选择 |
| 315 | 右侧更小元素 | 归并/树状数组 | 🔴 | 归并 |

---

## 2. 手撕演练：快排完整版（面试标准答案）

### 2.1 带优化的完整版

```java
/**
 * 快速排序 —— 面试标准答案（含三数取中 + 小区间插入）
 * 时间：平均 O(n log n)，最坏 O(n²)（但三数取中后几乎不触发）
 * 空间：O(log n)（递归栈）
 */
public class QuickSort {
    private static final int THRESHOLD = 16;

    public void sort(int[] arr) {
        if (arr == null || arr.length < 2) return;
        quickSort(arr, 0, arr.length - 1);
    }

    private void quickSort(int[] arr, int lo, int hi) {
        // ① 小区间 → 插入排序（减少递归开销）
        if (hi - lo + 1 < THRESHOLD) {
            insertionSort(arr, lo, hi);
            return;
        }

        // ② 三数取中选基准（防有序数组退化）
        int pivotIdx = medianOfThree(arr, lo, hi);
        swap(arr, pivotIdx, hi);              // 基准放末尾

        int pivot = arr[hi];
        int i = lo - 1;
        for (int j = lo; j < hi; j++) {
            if (arr[j] < pivot) {
                swap(arr, ++i, j);
            }
        }
        swap(arr, i + 1, hi);                 // 基准归位

        // ③ 递归（子区间可能为空，无需判断）
        quickSort(arr, lo, i);
        quickSort(arr, i + 2, hi);
    }

    private int medianOfThree(int[] arr, int lo, int hi) {
        int mid = lo + (hi - lo) / 2;
        if (arr[lo] > arr[mid]) swap(arr, lo, mid);
        if (arr[lo] > arr[hi]) swap(arr, lo, hi);
        if (arr[mid] > arr[hi]) swap(arr, mid, hi);
        return mid;                            // 中位数位置
    }

    private void insertionSort(int[] arr, int lo, int hi) {
        for (int i = lo + 1; i <= hi; i++) {
            int key = arr[i], j = i - 1;
            while (j >= lo && arr[j] > key) {
                arr[j + 1] = arr[j--];
            }
            arr[j + 1] = key;
        }
    }

    private void swap(int[] arr, int i, int j) {
        int tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp;
    }
}
```

### 2.2 面试手撕讲解流程

```
"我先说思路：快排是分治——选基准、partition 分区、递归两侧。
 我补充两个优化：
 ① 三数取中选基准，避免有序数组退化成 O(n²)；
 ② 小区间（<16）用插入排序，减少递归调用开销。
 平均 O(n log n)，最坏 O(n²)，空间 O(log n)，不稳定。"
```

---

## 3. 手撕演练：归并 + 堆排

### 3.1 归并（含稳定性讲解）

```java
/**
 * 归并排序 —— 面试标准答案
 * 要点：merge 用 <= 保证稳定；辅助数组只在 merge 时分配
 */
public class MergeSort {

    public void sort(int[] arr) {
        int[] temp = new int[arr.length];      // 全局辅助数组（优化：不反复分配）
        mergeSort(arr, 0, arr.length - 1, temp);
    }

    private void mergeSort(int[] arr, int lo, int hi, int[] temp) {
        if (lo >= hi) return;
        int mid = lo + (hi - lo) / 2;
        mergeSort(arr, lo, mid, temp);
        mergeSort(arr, mid + 1, hi, temp);
        merge(arr, lo, mid, hi, temp);
    }

    private void merge(int[] arr, int lo, int mid, int hi, int[] temp) {
        System.arraycopy(arr, lo, temp, lo, hi - lo + 1);   // 拷贝到辅助

        int i = lo, j = mid + 1, k = lo;
        while (i <= mid && j <= hi) {
            // ⚠️ 用 <= 保证稳定性（左半相等元素优先）
            if (temp[i] <= temp[j]) arr[k++] = temp[i++];
            else arr[k++] = temp[j++];
        }
        while (i <= mid) arr[k++] = temp[i++];
        while (j <= hi)  arr[k++] = temp[j++];
    }
}
```

### 3.2 堆排（含建堆讲解）

```java
/**
 * 堆排序 —— 面试标准答案
 * 要点：建堆从 n/2-1 开始；下沉用循环防栈溢出
 */
public class HeapSort {

    public void sort(int[] arr) {
        int n = arr.length;

        // ① 建最大堆：从最后一个非叶子节点开始下沉
        for (int i = n / 2 - 1; i >= 0; i--) {
            siftDown(arr, i, n);
        }

        // ② 取堆顶交换到末尾
        for (int i = n - 1; i > 0; i--) {
            swap(arr, 0, i);                 // 最大元素归位
            siftDown(arr, 0, i);             // 重建堆（范围缩到 i）
        }
    }

    private void siftDown(int[] arr, int i, int size) {
        while (2 * i + 1 < size) {           // 有左孩子
            int child = 2 * i + 1;
            if (child + 1 < size && arr[child + 1] > arr[child]) {
                child++;                     // 选较大的孩子
            }
            if (arr[i] >= arr[child]) break; // 已满足堆性质
            swap(arr, i, child);
            i = child;                       // 继续下沉
        }
    }

    private void swap(int[] arr, int i, int j) {
        int tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp;
    }
}
```

### 3.3 手撕易错点对照

| 算法 | 易错点 | 修正 |
|------|------|------|
| 快排 | 分区后递归边界写错 | 递归 `[lo, i]` 和 `[i+2, hi]`（基准在 i+1） |
| 快排 | 忘写小区间判断 | 递归出口 `lo >= hi` 或阈值判断 |
| 归并 | 剩余元素遗漏 | 两个 while 拷贝剩余 |
| 归并 | 稳定性写错 | merge 用 `<=` 不用 `<` |
| 堆排 | 建堆起点错 | 从 `n/2 - 1`（最后一个非叶子） |
| 堆排 | 下沉子节点越界 | `2*i+1 < size` 检查 |

---

## 4. 高频追问 TOP 10

| # | 追问 | 标准回答 |
|:---:|------|------|
| 1 | 快排最坏情况什么时候发生？ | 数组已有序/逆序且固定取首尾做基准 → 每次划分只排除一个元素 → O(n²) |
| 2 | 快排优化方案？ | 三数取中（选基准）、随机基准（防恶意输入）、小区间插入排序（减递归开销）、三路快排（大量重复元素） |
| 3 | 为什么快排平均比归并/堆排快？ | ① 顺序访问，缓存命中率高；② 无辅助数组拷贝；③ 平均比较次数少（1.39n log n） |
| 4 | 归并为什么稳定？ | merge 时相等元素取左半（`<=`），相对顺序不变 |
| 5 | 归并为什么适合链表？ | 只需 next 指针操作，不需要随机访问；链表天然适合拆分合并 |
| 6 | 堆排建堆复杂度？ | O(n)——底层节点下沉次数少，Σ(n/2^h × h) ≈ 2n |
| 7 | 堆排为什么不稳定？ | 堆顶与末尾交换会跨越中间元素，打乱相等元素顺序 |
| 8 | TopK 用什么？ | 堆 O(n log k) 支持数据流；快速选择平均 O(n) 适合静态数组 |
| 9 | 1TB 数据怎么排序？ | 外部排序：分批内存排序生成顺段 → K 路归并（败者树优化） |
| 10 | Java Arrays.sort 原理？ | 基本类型双轴快排（速度）；对象 TimSort（稳定）；并行用 parallelSort |

---

## 5. 面试解题话术模板

```
📣 手撕排序题（如 912. 排序数组）：

第 1 步：确认输入与要求 (10秒)
"是 int 数组排序，没有稳定性要求对吧？我写快排。
 如果要求稳定，我会写归并。"

第 2 步：说算法思路 (30秒)
"快排是分治：选基准、分区（小于基准在左，大于在右）、递归两侧。
 我加三数取中优化避免有序数组退化。"

第 3 步：写代码 (2-3分钟)
（按 2.1 节的标准答案写）

第 4 步：验证与复杂度 (30秒)
"我用 [1,3,2,5,4] 快速验证一下。平均 O(n log n)，
 最坏 O(n²)（三数取中后基本不触发），空间 O(log n) 递归栈。"

📣 TopK 题（如 215. 数组第 K 大）：

"两个方案：
 ① 小根堆 O(n log k) 空间 O(k)——维护 k 个候选，堆顶就是第 k 大；
 ② 快速选择平均 O(n)——partition 后只递归一侧。
 数组静态我会选快速选择（更快），
 如果题目是数据流我选堆。"
```

---

## 6. 调试技巧与自检清单

### 6.1 排序代码验证方法

```
① 空数组 / 单元素 → 直接返回
② 已有序 [1,2,3,4,5] → 验证无退化（快排三数取中）
③ 逆序 [5,4,3,2,1] → 验证正确性
④ 含重复 [3,1,2,1,3] → 验证去重场景不崩溃
⑤ 随机大数组 → 与 Arrays.sort() 结果对比

快速验证数组：
  [1,3,2,5,4]  ← 手推最常用
  [2,1]        ← 最小交换场景
  [1,1]        ← 相等元素场景
```

### 6.2 排序 Bug 速查表

| Bug | 原因 | 修复 |
|------|------|------|
| 快排死循环 | 分区后边界重叠 | 递归 `[lo, i]` 与 `[i+2, hi]` |
| 归并结果缺失 | 剩余元素没拷完 | 补两个 while |
| 归并不稳定 | 用了 `<` | 改为 `<=` |
| 堆排结果错乱 | 建堆起点错 | 从 `n/2-1` 开始 |
| 堆排越界 | 子节点下标检查缺失 | `2*i+1 < size` |
| 计数排序乱序 | 未倒序回填 | 倒序遍历保证稳定 |

### 6.3 面试手写 Checklist

```
□ 先确认稳定性要求（决定快排还是归并）
□ 说思路再动手（分治/递归/建堆三步）
□ 快排：三数取中 + 小区间插入（加分）
□ 归并：<= 保证稳定 + 剩余元素拷贝
□ 堆排：建堆起点 n/2-1 + 下沉循环
□ 手推一个用例验证
□ 报出完整复杂度（时间最好/平均/最坏 + 空间 + 稳定）
□ 能回答"为什么选这个排序"
```

> 🎯 **核心要点**：排序面试 = 手撕（快排/归并/堆排）+ 理论（复杂度/稳定性/选型）+ 应用（TopK/外部排序）。**手撕快排必带三数取中，归并必讲稳定性，堆排必讲建堆 O(n)**。把 TOP 10 追问背熟，用决策树应对任何选型题，排序专题就是你的稳定得分点。

---

**返回总览**：[00-排序知识体系总览](00-排序知识体系总览.md)
