# 03 - Java PriorityQueue

> 工程用 PriorityQueue，面试才手写堆：默认小顶堆、O(log n) 插入删除、比较器定制——本节目录全部 API、五种比较器写法与复杂度速查。

## 📚 目录

1. [核心 API 一览](#1-核心-api-一览)
2. [构造与比较器](#2-构造与比较器)
3. [对象排序的三种写法](#3-对象排序的三种写法)
4. [工程最佳实践](#4-工程最佳实践)
5. [复杂度速查表](#5-复杂度速查表)

## 1. 核心 API 一览

```java
PriorityQueue<Integer> heap = new PriorityQueue<>();

heap.offer(5);            // 插入元素（add 的别名），O(log n)
heap.add(3);              // offer 的别名，失败抛异常
int top = heap.peek();    // 查看堆顶（最小），空返回 null，O(1)
int min = heap.poll();    // 取出堆顶，O(log n)
heap.remove(5);           // 删除指定元素（O(n) 线性查找！慎用）
heap.contains(3);         // 包含检查，O(n)
heap.size(); heap.isEmpty();
heap.clear();
```

> ⚠️ **remove(Object)/contains 是 O(n)**——堆只保证根最快，任意元素操作需要线性扫描；需要频繁删任意元素时用「延迟删除」技巧（见 06 模块与专题 5 精要）。

## 2. 构造与比较器

```java
// 默认：小顶堆（自然序）
PriorityQueue<Integer> minHeap = new PriorityQueue<>();

// 大顶堆：逆序比较器
PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Comparator.reverseOrder());

// 指定初始容量（减少扩容，海量数据建议）
PriorityQueue<Integer> heap = new PriorityQueue<>(k);

// 从集合建堆：O(n)（与手写建堆同复杂度）
PriorityQueue<Integer> heap = new PriorityQueue<>(list);

// 自定义比较器（小顶堆 = 按 weight 升序）
PriorityQueue<Node> heap = new PriorityQueue<>((a, b) -> a.weight - b.weight);
```

| 场景 | 比较器 | 说明 |
|------|--------|------|
| 默认小顶堆 | 无 | 自然序 |
| 大顶堆 | `Comparator.reverseOrder()` | 数字/可比较对象逆序 |
| 自定义升序 | `(a, b) -> a.field - b.field` | 小顶堆语义 |
| 自定义降序 | `(a, b) -> b.field - a.field` | 大顶堆语义 |

## 3. 对象排序的三种写法

```java
// 写法一：Lambda（推荐，最简洁）
PriorityQueue<Order> heap = new PriorityQueue<>(
    (a, b) -> a.amount - b.amount);          // 按金额小顶堆

// 写法二：Comparator 链式（多字段）
PriorityQueue<Order> heap = new PriorityQueue<>(
    Comparator.comparingInt(Order::getAmount)
              .thenComparing(Order::getId)); // 金额升序，同额按 id

// 写法三：compareTo（对象实现 Comparable）
class Order implements Comparable<Order> {
    int amount;
    @Override
    public int compareTo(Order o) { return this.amount - o.amount; }
}
PriorityQueue<Order> heap = new PriorityQueue<>();   // 自然序即按 amount
```

> ⚠️ **整数相减溢出陷阱**：`a.amount - b.amount` 当值接近 Integer.MAX_VALUE 时会溢出——生产代码用 `Integer.compare(a.amount, b.amount)` 或 `Comparator.comparingInt`。

## 4. 工程最佳实践

### 4.1 大顶堆装小数、小顶堆装大数（TopK 口诀）

```java
// 求「前 K 大」：维护大小为 K 的小顶堆（堆顶是第 K 大的门槛）
public int findKthLargest(int[] nums, int k) {   // 215
    PriorityQueue<Integer> heap = new PriorityQueue<>(k);
    for (int num : nums) {
        if (heap.size() < k) {
            heap.offer(num);
        } else if (num > heap.peek()) {
            heap.poll();
            heap.offer(num);        // 或 JDK 17+ heap.replace(num) 略快
        }
    }
    return heap.peek();
}
```

### 4.2 海量数据场景

- 数据量大到无法全量排序（10 亿取 Top100）：**只维护 K=100 的堆**，空间 O(K)、时间 O(n log K)；
- 内存装不下：堆 + 外部归并/分桶（参考排序算法外部排序）；
- 数组 + 双指针类排序场景（如 378 有序矩阵第 K 小）：堆存每行指针，每次弹出行内最小并推进。

### 4.3 与手写堆的取舍

| 维度 | PriorityQueue | 手写堆 |
|------|---------------|--------|
| 代码量 | 一行 | 40 行 |
| 通用性 | 任意对象+比较器 | 只支持基础类型简单场景 |
| 性能 | 略慢（对象装箱/动态扩容） | 更可控 |
| 使用场景 | **工程默认** | 面试手写、性能极端敏感 |

> 💡 通用优先队列模板：`PriorityQueue<int[]>` 存二元组（值, 索引）是图/矩阵/双指针题的万能搭档。

## 5. 复杂度速查表

| 操作 | PriorityQueue | 手写堆 |
|------|:---:|:---:|
| peek（查最值） | O(1) | O(1) |
| offer/add（插入） | O(log n) | O(log n) |
| poll（删最值） | O(log n) | O(log n) |
| remove(任意元素) | O(n) | O(n) |
| contains | O(n) | O(n) |
| 集合构造 | O(n) | O(n)（建堆） |
| 空间 | O(n) | O(n) |

### 4.4 PriorityQueue 源码要点（面试加分）

| 源码细节 | 说明 |
|---------|------|
| 底层数组 | `Object[] queue`，默认容量 11 |
| 扩容 | 容量 < 64 时翻倍，否则 +50% |
| siftUp/siftDown | 与 02 模块手写堆同构（基于 compareTo/比较器） |
| 不允许 null | `offer(null)` 抛 NPE |
| 非线程安全 | 并发用 `PriorityBlockingQueue`（锁）或 `ConcurrentSkipListSet`（无锁） |

```java
// 并发场景选择（面试对比）
PriorityBlockingQueue<Task> pbq = new PriorityBlockingQueue<>();  // 锁实现
ConcurrentSkipListMap<Integer, String> skiplist = new ConcurrentSkipListMap<>();  // 无锁有序
// 低频更新高频读 → 跳表；写多 → 阻塞队列
```

### 4.5 常见误区清单

| 误区 | 正确认知 |
|------|---------|
| `PriorityQueue` 是有序的 | ❌ 只有根最值有序；`toArray()` 不保证有序 |
| 迭代器按优先级遍历 | ❌ 迭代器无序，要 `poll()` 循环才有序 |
| `add` 与 `offer` 等价 | 基本等价（add 失败抛异常、offer 返回 false） |
| 对象比较器写反 | 小顶堆是 `(a,b) -> a-b`；写反变「伪大顶堆」但语义错乱 |

> 🎯 **核心要点**：PriorityQueue 验收——会写四种堆（默认/大顶/对象 Lambda/多字段链式）、记住「**TopK 口诀**：前 K 大装小堆」、警惕两个 O(n) 陷阱（remove/contains）与整数溢出；工程题永远优先 PriorityQueue，只有面试官说「手写」才掏 02 模块的模板。

---

**上一模块**：[02-二叉堆的数组实现](02-二叉堆的数组实现.md) ｜ **下一模块**：[04-堆排序与建堆](04-堆排序与建堆.md) ｜ **返回总览**：[00-堆 Heap 知识体系总览](00-堆 Heap 知识体系总览.md)

**【参考来源】**
- Oracle JavaDoc：PriorityQueue：https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/PriorityQueue.html
- LeetCode 215 数组中的第 K 个最大元素
