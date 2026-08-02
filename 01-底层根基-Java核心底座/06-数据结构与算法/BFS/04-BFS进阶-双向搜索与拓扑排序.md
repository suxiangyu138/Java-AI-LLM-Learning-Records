# BFS进阶：双向搜索与拓扑排序
> 双向 BFS 的平方级加速 + Kahn 拓扑排序——BFS 的两大进阶形态

## 📚 目录
1. [双向 BFS 原理](#1-双向-bfs-原理)
2. [127. 单词接龙（双向版）](#2-127-单词接龙双向版)
3. [Kahn 拓扑排序](#3-kahn-拓扑排序)
4. [207. 课程表](#4-207-课程表)
5. [210. 课程表 II](#5-210-课程表-ii)
6. [BFS 进阶总结](#6-bfs-进阶总结)

---

## 1. 双向 BFS 原理

### 1.1 为什么双向更快？

```
单向 BFS：从起点扩散
  层数 d → 状态数 b^d（b = 分支因子）

双向 BFS：同时从起点和终点扩散
  每侧扩散 d/2 层 → 状态数 2 × b^(d/2)

对比（b=8, d=10）：
  单向：8¹⁰ ≈ 10⁹
  双向：2 × 8⁵ ≈ 2×32768 ≈ 65536
  → 平方级减少！（开根号效果）
```

### 1.2 双向 BFS 模板

```java
/**
 * 双向 BFS 模板
 * 要点：
 *   ① 两个 Set 分别记录两端的当前层
 *   ② 每次扩散"较小"的一侧
 *   ③ 两侧集合相交 → 找到最短路径
 */
public int bidirectionalBFS(Set<String> startSet,
                            Set<String> endSet,
                            Set<String> blocked) {
    Set<String> visited = new HashSet<>(startSet);
    int steps = 0;

    while (!startSet.isEmpty() && !endSet.isEmpty()) {
        // 优化：扩散较小的一侧
        if (startSet.size() > endSet.size()) {
            Set<String> temp = startSet;
            startSet = endSet;
            endSet = temp;
        }

        Set<String> nextLevel = new HashSet<>();
        for (String cur : startSet) {
            for (String next : 生成邻居(cur)) {
                if (endSet.contains(next)) return steps + 1;   // 相遇！
                if (blocked.contains(next) || visited.contains(next)) continue;
                visited.add(next);
                nextLevel.add(next);
            }
        }
        startSet = nextLevel;
        steps++;
    }
    return -1;    // 不可达
}
```

---

## 2. 127. 单词接龙（双向版）

### 2.1 完整实现

```java
/**
 * LeetCode 127. 单词接龙（双向 BFS 优化版）
 * 从 beginWord 和 endWord 同时扩散
 */
public int ladderLength(String beginWord, String endWord,
                        List<String> wordList) {
    Set<String> wordSet = new HashSet<>(wordList);
    if (!wordSet.contains(endWord)) return 0;

    Set<String> beginSet = new HashSet<>();
    Set<String> endSet = new HashSet<>();
    Set<String> visited = new HashSet<>();
    beginSet.add(beginWord);
    endSet.add(endWord);

    int steps = 1;                          // 起点算第 1 层
    while (!beginSet.isEmpty() && !endSet.isEmpty()) {
        // ⚠️ 扩散较小的一侧（性能优化）
        if (beginSet.size() > endSet.size()) {
            Set<String> temp = beginSet;
            beginSet = endSet;
            endSet = temp;
        }

        Set<String> nextLevel = new HashSet<>();
        for (String word : beginSet) {
            char[] chars = word.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                char original = chars[i];
                for (char c = 'a'; c <= 'z'; c++) {
                    if (c == original) continue;
                    chars[i] = c;
                    String next = new String(chars);

                    if (endSet.contains(next)) return steps + 1;  // 相遇！

                    if (wordSet.contains(next) && !visited.contains(next)) {
                        visited.add(next);
                        nextLevel.add(next);
                    }
                }
                chars[i] = original;
            }
        }
        beginSet = nextLevel;
        steps++;
    }
    return 0;
}
```

### 2.2 单向 vs 双向对比

| 维度 | 单向 BFS | 双向 BFS |
|------|:---:|:---:|
| 扩散方向 | 起点 → 终点 | 两端同时 |
| 状态数 | b^d | 2×b^(d/2) |
| 代码 | 简单 | 略复杂 |
| 适用 | 通用 | 分支因子大/层数深 |
| 面试定位 | 标准答案 | 优化加分 |

---

## 3. Kahn 拓扑排序

### 3.1 什么是拓扑排序

```
拓扑排序：有向无环图（DAG）的线性排列
  每条边 u→v，u 必须排在 v 前面

例：课程依赖
  A → B（先修 A 再修 B）
  A → C，B → D，C → D
  拓扑序：A, B, C, D 或 A, C, B, D

用途：课程安排、编译顺序、任务调度
前提：必须是 DAG！有环 → 无拓扑序
```

### 3.2 Kahn 算法（BFS 版）

```java
/**
 * Kahn 拓扑排序（BFS 版，最易手写）
 * 步骤：
 *   ① 统计所有节点入度
 *   ② 入度为 0 的节点入队（无前置依赖）
 *   ③ 出队 → 加入拓扑序 → 后继节点入度-1
 *   ④ 入度变 0 → 入队
 *   ⑤ 结束：处理数 == 节点数 → 无环；否则有环
 */
public List<Integer> kahnTopo(int n, int[][] edges) {
    // ① 建图（邻接表）+ 统计入度
    List<List<Integer>> graph = new ArrayList<>();
    int[] indegree = new int[n];
    for (int i = 0; i < n; i++) graph.add(new ArrayList<>());
    for (int[] e : edges) {
        graph.get(e[0]).add(e[1]);      // e[0] → e[1]
        indegree[e[1]]++;               // 入度+1
    }

    // ② 入度为 0 的入队
    Queue<Integer> queue = new LinkedList<>();
    for (int i = 0; i < n; i++) {
        if (indegree[i] == 0) queue.offer(i);
    }

    // ③ 处理
    List<Integer> topo = new ArrayList<>();
    while (!queue.isEmpty()) {
        int cur = queue.poll();
        topo.add(cur);
        for (int next : graph.get(cur)) {
            if (--indegree[next] == 0) {   // 入度减到 0
                queue.offer(next);         // 可以处理了
            }
        }
    }
    return topo;   // 长度 < n → 有环（无法完成拓扑）
}
```

---

## 4. 207. 课程表

### 4.1 完整实现（Kahn 判环）

```java
/**
 * LeetCode 207. 课程表（拓扑排序高频题）
 * numCourses 门课，prerequisites 是先修关系
 * 判断能否完成所有课程（是否有环）
 *
 * Kahn 判环：拓扑序长度 == 课程数 → 无环可完成
 */
public boolean canFinish(int numCourses, int[][] prerequisites) {
    // ① 建图 + 入度
    List<List<Integer>> graph = new ArrayList<>();
    int[] indegree = new int[numCourses];
    for (int i = 0; i < numCourses; i++) graph.add(new ArrayList<>());
    for (int[] pre : prerequisites) {
        graph.get(pre[1]).add(pre[0]);   // pre[1] → pre[0]（先修 → 后续）
        indegree[pre[0]]++;
    }

    // ② 入度 0 入队
    Queue<Integer> queue = new LinkedList<>();
    for (int i = 0; i < numCourses; i++) {
        if (indegree[i] == 0) queue.offer(i);
    }

    // ③ BFS 处理
    int processed = 0;
    while (!queue.isEmpty()) {
        int cur = queue.poll();
        processed++;
        for (int next : graph.get(cur)) {
            if (--indegree[next] == 0) {
                queue.offer(next);
            }
        }
    }
    return processed == numCourses;      // 全部处理完 → 无环 ✓
}
// [[1,0],[0,1]]：1→0 且 0→1 → 环 → 无法完成 ✓
```

### 4.2 为什么"处理数 == 节点数"能判环？

```
无环 DAG：每个节点最终入度都会变为 0 → 全部入队处理
有环：环上节点入度永远 ≥ 1 → 永远无法入队 → 处理数 < n

例：0→1→0（环）
  入度：0:1, 1:1
  没有入度 0 的节点 → 队列空 → processed=0 ≠ 2 → 有环 ✓
```

---

## 5. 210. 课程表 II

### 5.1 完整实现（输出拓扑序）

```java
/**
 * LeetCode 210. 课程表 II
 * 返回一种可能的课程学习顺序（拓扑序）
 *
 * 与 207 唯一区别：返回 topo 列表而不是 boolean
 */
public int[] findOrder(int numCourses, int[][] prerequisites) {
    List<List<Integer>> graph = new ArrayList<>();
    int[] indegree = new int[numCourses];
    for (int i = 0; i < numCourses; i++) graph.add(new ArrayList<>());
    for (int[] pre : prerequisites) {
        graph.get(pre[1]).add(pre[0]);
        indegree[pre[0]]++;
    }

    Queue<Integer> queue = new LinkedList<>();
    for (int i = 0; i < numCourses; i++) {
        if (indegree[i] == 0) queue.offer(i);
    }

    int[] order = new int[numCourses];
    int index = 0;
    while (!queue.isEmpty()) {
        int cur = queue.poll();
        order[index++] = cur;                  // 拓扑序
        for (int next : graph.get(cur)) {
            if (--indegree[next] == 0) queue.offer(next);
        }
    }
    return index == numCourses ? order : new int[0];   // 有环 → 空
}
```

### 5.2 拓扑排序题型速查

| 题号 | 题目 | 变体 |
|:---:|------|------|
| 207 | 课程表 | 判环（boolean） |
| 210 | 课程表 II | 输出拓扑序 |
| 269 | 火星词典 | 字符间建边 + 拓扑 |
| 329 | 矩阵最长递增路径 | DFS 记忆化（非 Kahn） |
| 1136 | 并行课程 | 拓扑 + 分层 |

---

## 6. BFS 进阶总结

### 6.1 BFS 全家桶

```
BFS
├── 基础：队列 + 分层（102）
├── 网格：方向数组 + 沉岛（200/1091）
├── 多源：所有起点入队（994/542）
├── 状态：状态图最短路径（127/752）
├── 双向：两端扩散（127 优化）
└── 拓扑：Kahn 入度（207/210）
```

### 6.2 复杂度总结

| 形态 | 复杂度 | 关键 |
|------|:---:|------|
| 二叉树 BFS | O(n) | size 分层 |
| 网格 BFS | O(mn) | 方向数组 |
| 多源 BFS | O(mn) | 超级源点 |
| 状态 BFS | O(N×K) | N 状态 K 转移 |
| 双向 BFS | O(2×b^(d/2)) | 平方级减少 |
| Kahn 拓扑 | O(V+E) | 入度队列 |

### 6.3 面试话术（拓扑）

```
"这题是拓扑排序，用 Kahn 算法（BFS 版）：
 ① 统计入度，入度为 0 的先入队（无前置依赖）；
 ② 出队加入结果，后继入度减 1，减到 0 再入队；
 ③ 处理数 == 课程数 → 无环可完成；
    少于 → 有环，无法完成。
 复杂度 O(V+E)。"
```

> 🎯 **核心要点**：BFS 进阶两大武器——**双向 BFS**（两端扩散，状态数从 b^d 降到 2×b^(d/2)，127 优化）和 **Kahn 拓扑排序**（入度 0 入队 → 减入度 → 判环，207/210 课程表）。Kahn 是"最易手写"的拓扑算法，模板固定（建图/入度/队列/计数），面试必背。双向 BFS 在状态空间大时是明显的加分优化。

---

**下一模块**：[05-Java实现与面试实战](05-Java实现与面试实战.md) | **返回总览**：[00-BFS知识体系总览](00-BFS知识体系总览.md)
