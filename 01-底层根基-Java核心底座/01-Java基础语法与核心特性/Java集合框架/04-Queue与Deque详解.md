# 04 Queue 与 Deque 详解

> 队列是"先进先出"的容器，双端队列还能当栈用——ArrayDeque 与 PriorityQueue 是最重要的两个实现，BlockingQueue 是并发的桥梁

---

## 📚 目录

1. [Queue / Deque 接口语义](#1-queue--deque-接口语义)
2. [ArrayDeque：循环数组实现，栈队列首选](#2-arraydeque循环数组实现栈队列首选)
3. [PriorityQueue：二叉堆与优先级语义](#3-priorityqueue二叉堆与优先级语义)
4. [Stack 的正确替代：ArrayDeque](#4-stack-的正确替代arraydeque)
5. [BlockingQueue 家族概览](#5-blockingqueue-家族概览)
6. [Queue 选型决策与陷阱](#6-queue-选型决策与陷阱)

---

## 1. Queue / Deque 接口语义

**Queue（队列）**：FIFO 容器，元素在队尾进入、队头取出。语义核心是"**处理顺序**"而非"遍历方便"。

**Deque（双端队列）**：两端都可进可出——既能当队列（FIFO），也能当栈（LIFO）。

**Queue 的两套操作体系**（关键！）：

| 操作 | 抛异常版本 | 返回特殊值版本 | 区别 |
|------|-----------|--------------|------|
| 入队 | `add(e)` | `offer(e)` | 满时 add 抛异常，offer 返回 false |
| 出队 | `remove()` | `poll()` | 空时 remove 抛 NoSuchElementException，poll 返回 null |
| 查看队头 | `element()` | `peek()` | 空时 element 抛异常，peek 返回 null |

```java
Queue<String> q = new ArrayDeque<>();
q.add("a");                  // 满（有界队列）时抛 IllegalStateException
q.offer("b");                // 满时返回 false，不抛异常 —— 生产环境推荐
q.peek();                    // a，空返回 null
q.poll();                    // a，空返回 null —— 出队推荐
```

> 🎯 **核心要点**：**生产代码用 `offer/poll/peek` 三件套**——用返回 null/false 表达"空/满"，避免异常流程控制。`add/remove/element` 三件套语义上更"严格"，适合确定不会空/满的场合。

**Deque 的双端 API**（JDK 6+，队列语义同 Queue）：

| 操作 | 队头（first） | 队尾（last） |
|------|-------------|-------------|
| 入 | `addFirst` / `offerFirst` | `addLast` / `offerLast` |
| 出 | `removeFirst` / `pollFirst` | `removeLast` / `pollLast` |
| 看 | `getFirst` / `peekFirst` | `getLast` / `peekLast` |

JDK 21+（SequencedCollection）起：`getFirst()/getLast()/removeFirst()/removeLast()` 成为通用有序集合 API，Deque 的方法被"上提"到父接口（见 01 模块第 5 节）。

---

## 2. ArrayDeque：循环数组实现，栈队列首选

**底层**：**循环数组**（ring buffer）——数组 + `head`/`tail` 两个指针，指针触底回绕到数组头：

```text
       [ _ ][ b ][ c ][ _ ][ _ ][ _ ]        head=1, tail=3
        ↑         ↑
       head      tail
addFirst(d)：head 前移回绕 → [ d ][ b ][ c ][ _ ]...   O(1)
addLast(e)：tail 后移 →      [ d ][ b ][ c ][ e ]...   O(1)
```

**为什么比 LinkedList 更适合当队列/栈**：

| 维度 | ArrayDeque | LinkedList |
|------|:----------:|:----------:|
| 底层 | 循环数组（连续内存） | 双向链表（节点分散） |
| 内存 | 只存元素 + 2 个指针 | 每元素多 ~24B 节点开销 |
| 缓存 | ✅ 连续内存，迭代/操作友好 | ❌ 指针跳跃 |
| 双端操作 | O(1) 均摊（扩容翻倍复制） | O(1) |
| null | **禁止 null**（poll 用 null 表达空） | 允许 |
| 栈/队列 API | 完整 Deque | 完整 Deque |

**结论**：JDK 文档明确建议——**优先使用 ArrayDeque 作为栈和队列**。

```java
Deque<String> queue = new ArrayDeque<>();
queue.addLast("a"); queue.addLast("b");     // 入队
String head = queue.pollFirst();            // 出队 a

Deque<String> stack = new ArrayDeque<>();
stack.push("a"); stack.push("b");           // 栈：push = addFirst
String top = stack.pop();                   // b，pop = pollFirst
```

> ⚠️ 容量细节：ArrayDeque 初始容量 16，扩容 2 倍（且强制为 2 的幂）；构造时可 `new ArrayDeque<>(预计容量)` 避免扩容。**不允许 null**——因为 poll/peek 用 null 表示"空"，null 元素会破坏这个约定。

---

## 3. PriorityQueue：二叉堆与优先级语义

**语义**：`poll()` 返回的不是最早入队的，而是**优先级最高**（最小/最大）的元素——由 `Comparable` 或构造器传入的 `Comparator` 决定。

**底层**：**二叉小顶堆**（数组实现的完全二叉树）：

```text
           (3)
         /     \
       (5)     (8)       数组表示：[3, 5, 8, 10, 7, 9, 12]
      /   \   /   \      堆性质：父节点 ≤ 子节点（小顶堆）
    (10) (7) (9) (12)    poll 弹堆顶 3 → 末尾 12 上浮调整 → O(log n)
```

| 操作 | 复杂度 | 说明 |
|------|:------:|------|
| `offer` / `add` | O(log n) | 尾插 + 上浮（siftUp） |
| `poll` | O(log n) | 弹顶 + 末元素下沉（siftDown） |
| `peek` | O(1) | 只看堆顶 |
| `contains` / `remove(Object)` | O(n) | 线性扫描（不是二叉搜索） |

**实战：TopK 问题**（维护最小的 K 个 / 最大的 K 个）：

```java
// 求 100 万个数里最大的 10 个：维护大小为 10 的最小堆
PriorityQueue<Integer> topK = new PriorityQueue<>(10);      // 默认小顶堆
for (int x : allNumbers) {
    if (topK.size() < 10) {
        topK.offer(x);
    } else if (x > topK.peek()) {      // 比堆顶大 → 换掉堆顶
        topK.poll();
        topK.offer(x);
    }
}
// 遍历 100 万次，每次 O(log 10)，总复杂度 O(n log k) ≈ O(n)
```

**排序规则与相等性**：

```java
PriorityQueue<Task> pq = new PriorityQueue<>(Comparator.comparing(Task::priority));
// 或自然序：元素实现 Comparable
// 注意：PriorityQueue 的 contains/remove 用 equals；而堆序用 compareTo ——
// compareTo 返回 0 的两个元素不保证互相 remove 得掉，规范上要求两者一致
```

> 🎯 **核心要点**：PriorityQueue 是"**每次取最值**"场景的利器（TopK、定时任务、Huffman 编码、Dijkstra 算法）。迭代顺序**不是排序序**（堆只是部分有序），要全序结果需 `poll` 循环或转数组后排序——这是最常见的误解。

---

## 4. Stack 的正确替代：ArrayDeque

JDK 官方文档对 `Stack` 的处置：**"A more complete and consistent set of LIFO stack operations is provided by the Deque interface and its implementations, which should be used in preference to this class."**（应优先使用 Deque 实现）

```java
// ❌ 遗留写法
Stack<String> stack = new Stack<>();

// ✅ 现代写法（语义、API 完全一致，性能更好）
Deque<String> stack = new ArrayDeque<>();
stack.push("a");      // push / pop / peek —— 与 Stack 同名同义
stack.pop();
```

**Stack 的三个问题**：① 继承 Vector——暴露了 `add(0, x)` 这类**违反栈语义**的方法；② 方法级同步——单线程白白付锁开销；③ 全局锁在并发下更是性能灾难。

---

## 5. BlockingQueue 家族概览

**BlockingQueue 是 Queue 的并发扩展**——加了两套**阻塞操作**：

| 操作 | 抛异常 | 返回特殊值 | 阻塞 | 超时 |
|------|:------:|:---------:|:----:|:----:|
| 入队 | `add` | `offer` | **`put(e)`**（满则阻塞） | `offer(e, t, unit)` |
| 出队 | `remove` | `poll` | **`take()`**（空则阻塞） | `poll(t, unit)` |

**五大实现**（详见 JUC 并发系统，此处只做选型速览）：

| 实现 | 底层 | 有界 | 特性 | 典型场景 |
|------|------|:----:|------|---------|
| ArrayBlockingQueue | 循环数组 + 单锁 | ✅ | 公平性可配 | 有界任务队列 |
| LinkedBlockingQueue | 链表 + 双锁 | 可配（默认 Integer.MAX） | 吞吐高 | 线程池默认队列 |
| PriorityBlockingQueue | 二叉堆 + 锁 | ❌ | 优先级出队 | 有优先级任务 |
| SynchronousQueue | 无缓冲 | — | put/take 直接交接 | 传递性任务（newCachedThreadPool） |
| DelayQueue | 堆 + 锁 | ❌ | 到期才可取 | 定时任务、延迟重试 |
| LinkedTransferQueue | 无锁链表 | ❌ | transfer 直交 | 高性能传递 |

```java
// 生产-消费经典骨架（线程池内部就是 LinkedBlockingQueue）
BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(100);
new Thread(() -> {
    try {
        Runnable task = queue.take();   // 空则阻塞等待 —— 消费者
        task.run();
    } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
}).start();

queue.put(() -> System.out.println("task"));   // 满则阻塞 —— 生产者
```

> ⚠️ 阻塞队列的坑：`take()` 中断时抛 InterruptedException，**处理惯例是恢复中断标志**（`Thread.currentThread().interrupt()`）而不是吞掉；`put` 永不返回的特性让它天然成为线程池"拒绝策略"之外的背压机制。

---

## 6. Queue 选型决策与陷阱

```text
需要什么队列语义？
├── 普通 FIFO 队列/栈（单线程） → ArrayDeque ✅
├── 按优先级取 → PriorityQueue
├── 有界阻塞队列（生产者-消费者） → ArrayBlockingQueue / LinkedBlockingQueue
├── 无界非阻塞并发队列 → ConcurrentLinkedQueue（见 06 模块）
├── 延迟到期取出 → DelayQueue
└── 需要双端并发操作 → ConcurrentLinkedDeque（见 06 模块）
```

| 陷阱 | 说明 | 正确姿势 |
|------|------|---------|
| poll 与 null 元素 | 队列用 null 表达"空"，**ArrayDeque/BlockingQueue 禁止 null 元素** | 业务对象校验非 null |
| PriorityQueue 迭代序 | 迭代不是排序序 | 要全序用 poll 循环或 stream().sorted() |
| PriorityQueue 的 equals 陷阱 | 堆序用 compareTo，contains 用 equals，两者不一致时行为怪异 | 保持 compareTo 与 equals 一致 |
| 无界队列内存溢出 | LinkedBlockingQueue 默认无界、PriorityBlockingQueue 无界——生产消费不平衡时 OOM | 指定容量或监控队列深度 |
| 把 LinkedList 当队列 | 可用但内存翻倍、缓存差 | ArrayDeque |
| 空队列 peek 误判 | `if (q.peek() != null)` 后直接 remove()——多线程下可能已被取走 | 直接用 poll() 判空处理 |

---

**下一模块**：[05-Map详解与选型](./05-Map详解与选型.md) / **返回总览**：[00-Java集合框架知识体系总览](./00-Java集合框架知识体系总览.md)
