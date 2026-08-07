# 03-队列与单调队列：BFS与滑动窗口
> 一句话：队列是 BFS 的天然载体（长度快照防混层）、单调队列是窗口最值的 O(n) 答案（队头过期 + 队尾踢小）——239 是母题

## 📚 目录
1. [队列模型：FIFO 与 BFS](#1-队列模型fifo-与-bfs)
2. [102 二叉树的层序遍历（长度快照）](#2-102-二叉树的层序遍历长度快照)
3. [多源 BFS：542 01 矩阵与 994 腐烂的橘子](#3-多源-bfs542-01-矩阵与-994-腐烂的橘子)
4. [622 设计循环队列（k+1 槽）](#4-622-设计循环队列k1-槽)
5. [239 滑动窗口最大值（单调队列母题）](#5-239-滑动窗口最大值单调队列母题)
6. [1438 绝对差不超过限制的最长连续子数组（双单调队列）](#6-1438-绝对差不超过限制的最长连续子数组双单调队列)
7. [队列家族与易错点](#7-队列家族与易错点)

---

## 1. 队列模型：FIFO 与 BFS

```text
队列 = 先进先出（队尾入、队头出）:
  offer O(1) / poll O(1) / peek O(1)

两大应用:
  ① BFS（广度优先）: 按层扩散，队列天然按"距离"排序
     → 层序遍历、最短路径、多源扩散
  ② 数据流/滑动窗口: 按序消费
     → 移动平均、窗口最值（单调队列）

实现: ArrayDeque（Java 推荐）或 LinkedList
  ⚠️ Java 里 Queue 接口 + ArrayDeque 是工程默认
```

> 🎯 **核心认知**："**BFS = 队列 + 访问标记**——队列保证'先发现的先处理'（距离单调），这就是最短路径问题的结构保证。树的 BFS 不需要标记（无环），图的 BFS 必须标记（防重复入队）。"

## 2. 102 二叉树的层序遍历（长度快照）

### 2.1 题目与思路

**题**：二叉树按层输出（每层一个 List）。

**长度快照**：每层开始前记录队列长度——**不记录会把下一层混进本层**：

```java
/**
 * LeetCode 102. 二叉树的层序遍历
 * 队列 + 长度快照（每层处理前记录 size）
 * 时间 O(n)，空间 O(n)
 */
public List<List<Integer>> levelOrder(TreeNode root) {
    List<List<Integer>> result = new ArrayList<>();
    if (root == null) return result;

    Deque<TreeNode> queue = new ArrayDeque<>();
    queue.offer(root);

    while (!queue.isEmpty()) {
        int size = queue.size();               // ① 长度快照（本层节点数！）
        List<Integer> level = new ArrayList<>();
        for (int i = 0; i < size; i++) {       // ② 只处理本层的 size 个
            TreeNode node = queue.poll();
            level.add(node.val);
            if (node.left != null) queue.offer(node.left);    // ③ 下一层入队
            if (node.right != null) queue.offer(node.right);
        }
        result.add(level);                     // ④ 一层一个 List
    }
    return result;
}
// ⚠️ 若不快照: for 用 queue.size() 会随着入队增长 → 本层混入下一层
// 变体: 103 锯齿遍历（加层号翻转）、107 自底向上（结果反转）
```

> 🎯 **长度快照的意义**："**`int size = queue.size()` 是层序遍历的灵魂**——它把'队列里有什么'切成'这一层'与'下一层'。这个技巧在 103/107/199（右视图）中全部复用。"

## 3. 多源 BFS：542 01 矩阵与 994 腐烂的橘子

### 3.1 542 01 矩阵（多源 BFS）

**题**：每个 0/1 到最近 0 的距离。

**多源 BFS**：所有 0 作为起点同时入队扩散（天然最短路）：

```java
/**
 * LeetCode 542. 01 矩阵（多源 BFS）
 * 所有 0 同时入队 → 逐层扩散 → 距离天然最短
 * 时间 O(m×n)，空间 O(m×n)
 */
public int[][] updateMatrix(int[][] mat) {
    int m = mat.length, n = mat[0].length;
    int[][] dist = new int[m][n];
    Deque<int[]> queue = new ArrayDeque<>();

    for (int i = 0; i < m; i++) {
        for (int j = 0; j < n; j++) {
            if (mat[i][j] == 0) queue.offer(new int[]{i, j});  // ① 所有 0 入队
            else dist[i][j] = -1;                // ② -1 = 未访问
        }
    }

    int[][] dirs = {{0,1},{0,-1},{1,0},{-1,0}};
    while (!queue.isEmpty()) {
        int[] cur = queue.poll();
        for (int[] d : dirs) {
            int ni = cur[0] + d[0], nj = cur[1] + d[1];
            if (ni >= 0 && ni < m && nj >= 0 && nj < n && dist[ni][nj] == -1) {
                dist[ni][nj] = dist[cur[0]][cur[1]] + 1;   // ③ 距离 = 前驱 + 1
                queue.offer(new int[]{ni, nj});
            }
        }
    }
    return dist;
}
// ⚠️ 多源 BFS 的正确性: 所有源同时扩散 → 首次到达 = 最短距离
//   （与单源 BFS 的距离单调性同理）
// 同族: 994 腐烂的橘子（多源 + 计层数）、286 墙与门
```

> 🎯 **多源 BFS 的价值**："**'多个起点同时入队'把 O(源数 × BFS) 优化为一次 BFS**——542/994/286 三题一个模板。'首次访问即最短'是 BFS 在网格上的核心性质。"

## 4. 622 设计循环队列（k+1 槽）

### 4.1 题目与思路

**题**：设计容量 k 的循环队列（O(1) 全操作）。

**k+1 槽技巧**：数组开 k+1 个位置，用"头尾指针是否相邻"区分空/满——**不用额外标志位**：

```java
/**
 * LeetCode 622. 设计循环队列
 * k+1 槽: 数组长度 k+1，head==tail 为空，(tail+1)%len==head 为满
 * 时间 O(1) 全操作，空间 O(k)
 */
class MyCircularQueue {
    private final int[] data;
    private final int capacity;              // 实际容量 = data.length−1
    private int head = 0, tail = 0;          // tail 指向下一个写入位置

    public MyCircularQueue(int k) {
        data = new int[k + 1];               // ① k+1 槽（牺牲一格换"空满判定"）
        capacity = k;
    }

    public boolean enQueue(int value) {
        if (isFull()) return false;
        data[tail] = value;
        tail = (tail + 1) % data.length;     // ② 环形前进
        return true;
    }

    public boolean deQueue() {
        if (isEmpty()) return false;
        head = (head + 1) % data.length;
        return true;
    }

    public int Front() { return isEmpty() ? -1 : data[head]; }
    public int Rear() { return isEmpty() ? -1 : data[(tail - 1 + data.length) % data.length]; }
    public boolean isEmpty() { return head == tail; }              // ③ 空
    public boolean isFull() { return (tail + 1) % data.length == head; }  // ④ 满
}
// 推演 k=3: data 长 4；入 1,2,3 → tail=3；入 4 → (3+1)%4==0==head → 满 → false ✓
```

> 💡 **k+1 槽的取舍**："**牺牲 1 格空间，换掉 isFull 的标志位分支**——这是经典的空间换简单。641 双端队列在 622 基础上加'头尾都能进出'（05 文件详讲）。"

## 5. 239 滑动窗口最大值（单调队列母题）

### 5.1 题目与思路

**题**：滑动窗口（大小 k）内最大值（O(n) 要求）。

**单调递减队列**：队头 = 窗口最大值；**入队踢小（队尾）、过期踢旧（队头）**：

```java
/**
 * LeetCode 239. 滑动窗口最大值（单调队列）
 * 单调递减队列（存下标）: 队头最大；入队踢小、过期踢旧
 * 时间 O(n)（每元素进出各一次，摊还），空间 O(k)
 */
public int[] maxSlidingWindow(int[] nums, int k) {
    Deque<Integer> deque = new ArrayDeque<>();     // 存下标，值单调递减
    int[] result = new int[nums.length - k + 1];

    for (int i = 0; i < nums.length; i++) {
        // ① 队尾踢小: 所有 ≤ 当前值的都"永远当不了最大"
        while (!deque.isEmpty() && nums[deque.peekLast()] <= nums[i]) {
            deque.pollLast();
        }
        deque.offerLast(i);                        // ② 入队（下标）

        // ③ 队头过期: 滑出窗口的下标弹出
        if (deque.peekFirst() <= i - k) deque.pollFirst();

        // ④ 窗口满 → 队头即最大值
        if (i >= k - 1) {
            result[i - k + 1] = nums[deque.peekFirst()];
        }
    }
    return result;
}
// 推演 [1,3,-1,-3,5,3,6,7]，k=3：
//   结果 [3,3,5,5,6,7] ✓（每元素入队/出队各一次 → O(n)）
// ⚠️ 为什么"踢小"正确: 更小且更旧的元素永远不可能成为窗口最大
```

> 🎯 **239 的 O(n) 魔法**："**'队尾踢小 + 队头过期'两个操作各让每个元素进出一次**——摊还 O(n) 而非 O(n·k)。这个'淘汰不可能成为答案的元素'的思维，与单调栈的'踢破坏者'完全同源。"

## 6. 1438 绝对差不超过限制的最长连续子数组（双单调队列）

### 6.1 题目与思路

**题**：窗口内最大最小值差 ≤ limit 的最长连续子数组。

**双单调队列**：一个保最大、一个保最小 + 滑动窗口：

```java
/**
 * LeetCode 1438. 绝对差不超过限制的最长连续子数组
 * 双单调队列（最大 + 最小）+ 滑动窗口
 * 时间 O(n)，空间 O(n)
 */
public int longestSubarray(int[] nums, int limit) {
    Deque<Integer> maxQ = new ArrayDeque<>();      // 递减: 队头最大
    Deque<Integer> minQ = new ArrayDeque<>();      // 递增: 队头最小
    int left = 0, best = 0;

    for (int right = 0; right < nums.length; right++) {
        // ① 维护双单调队列
        while (!maxQ.isEmpty() && nums[maxQ.peekLast()] <= nums[right]) maxQ.pollLast();
        while (!minQ.isEmpty() && nums[minQ.peekLast()] >= nums[right]) minQ.pollLast();
        maxQ.offerLast(right);
        minQ.offerLast(right);

        // ② 不合法 → 收缩（窗口最大−最小 > limit）
        while (nums[maxQ.peekFirst()] - nums[minQ.peekFirst()] > limit) {
            left++;
            if (maxQ.peekFirst() < left) maxQ.pollFirst();   // ③ 过期出队
            if (minQ.peekFirst() < left) minQ.pollFirst();
        }
        best = Math.max(best, right - left + 1);   // ④ 最长题: 收缩后更新
    }
    return best;
}
// ⚠️ 双队列 = 一个 239 + 一个镜像（保最小）→ 窗口极值差 O(1) 查询
// 同族: 1696 跳跃 VI（单调队列 + DP）、862 最短子数组（前缀和 + 单调队列）
```

> 💡 **1438 的进阶价值**："**双单调队列让'窗口极值差'变成 O(1) 查询**——239 的镜像应用。1696 跳跃 VI 是单调队列 + DP 的经典组合（DP 优化专题会再遇到）。"

## 7. 队列家族与易错点

### 7.1 家族一览

| 题 | 队列形态 | 核心技巧 | 与母题关系 |
|:---:|------|------|------|
| 102 层序遍历 | 普通队列 | 长度快照 | 母题 |
| 542/994 多源 BFS | 普通队列 | 多源入队 | 102 + 网格 |
| 622 循环队列 | 数组环形 | k+1 槽 | 设计母题 |
| 346 移动平均 | 普通队列 | 和 + 队列长度 | 数据流 |
| 239 窗口最大值 | 单调队列 | 踢小 + 过期 | **单调队列母题** |
| 1438 极值差 | 双单调队列 | 最大 + 最小 | 239 × 2 |
| 1696 跳跃 VI | 单调队列 + DP | 窗口最大 DP | 239 + DP |

### 7.2 易错点

| # | 易错点 | 正确写法 | 后果 |
|:---:|------|------|------|
| 1 | 层序遍历忘快照 | `int size = queue.size()` | 本层混入下一层 |
| 2 | 图 BFS 忘访问标记 | 入队即标记 | 重复入队/死循环 |
| 3 | 622 头尾指针越界 | `% data.length` 环形 | 越界/逻辑错 |
| 4 | 239 队头过期条件错 | `peekFirst() <= i − k` | 窗口外元素残留 |
| 5 | 239 踢小用 `<` | 用 `<=`（相等元素也踢） | 相等值处理错 |
| 6 | 多源 BFS 源未全入队 | 所有 0 先入队再 BFS | 距离算错 |
| 7 | 队列用 `Stack`/`Vector` 类 | ArrayDeque | 性能差/官方不推荐 |

> 🎯 **核心要点**：队列通关四件事——**① BFS 三要素**（队列 + 访问标记 + 长度快照）；**② 622 k+1 槽设计**（牺牲一格换空满判定）；**③ 239 单调队列烂熟**（踢小 + 过期 + 存下标，摊还 O(n)）；**④ 多源 BFS 模板**（所有源先入队）。**队列的'先进先出'让 BFS 的距离天然单调，单调队列的'淘汰'让窗口最值 O(n)——FIFO 的简单性就是它的强大**。

---

**下一模块**：[04-栈与队列高频题解](04-栈与队列高频题解.md)
**返回总览**：[00-栈与队列知识体系总览](00-栈与队列知识体系总览.md)
