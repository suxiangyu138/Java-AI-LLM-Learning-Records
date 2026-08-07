# 02-拓扑排序进阶：Kahn与DFS双模板

> 定位：拓扑排序的理论纵深——Kahn 算法与 DFS 双模板、判环、分层拓扑、拓扑 + DP、传递闭包、与最短路/强连通的衔接

## 📚 目录

1. [拓扑排序的本质](#1-拓扑排序的本质)
2. [Kahn 算法（BFS 版）](#2-kahn-算法bfs-版)
3. [DFS 版与判环](#3-dfs-版与判环)
4. [分层拓扑：并行课程](#4-分层拓扑并行课程)
5. [拓扑 + DP：最长路径](#5-拓扑--dp最长路径)
6. [拓扑的变体应用](#6-拓扑的变体应用)
7. [面试要点与追问](#7-面试要点与追问)

---

## 1. 拓扑排序的本质

### 1.1 定义与前提

```
拓扑排序：DAG（有向无环图）的线性序
  边 u→v 表示 u 先于 v → 排序后 u 在 v 前

⚠️ 前提：必须是 DAG！
  有环 → 拓扑排序不存在（环内无法定先后）

⚠️ 面试必答：
"拓扑排序 = DAG 的线性化——
 有环图无拓扑序（环内谁先谁后无法确定）。
 判环是拓扑题的第一步。"
```

### 1.2 应用场景

```
① 课程依赖（207/210）
② 编译依赖（Makefile）
③ 任务调度（有依赖的并行）
④ 字典序推理（269 火星词典）
⑤ 包管理依赖解析

⚠️ 面试表达：
"'前置依赖 + 顺序' → 拓扑排序——
 课程/编译/调度都是同一模型。"
```

---

## 2. Kahn 算法（BFS 版）

### 2.1 算法流程

```
Kahn（BFS 版）：
  ① 统计所有节点的入度
  ② 入度为 0 的节点入队（无前置 → 先输出）
  ③ 出队 → 输出 → 其邻居入度 -1 → 归零入队
  ④ 输出数 < 节点数 → 有环！

⚠️ 复杂度：O(V + E)（每节点出队一次、每边减一次）

⚠️ 面试必答：
"Kahn = 入度归零入队——
 '没有前置的先行'；输出数不足 = 有环
 （环内节点入度永远 ≥ 1）。"
```

### 2.2 完整模板（207/210 通用）

```java
/** 拓扑排序（Kahn 版），返回拓扑序；有环返回空 */
List<Integer> topologicalSort(int n, int[][] prerequisites) {
    List<Integer>[] graph = new List[n];
    for (int i = 0; i < n; i++) graph[i] = new ArrayList<>();
    int[] indegree = new int[n];

    for (int[] p : prerequisites) {
        graph[p[0]].add(p[1]);               // 建图
        indegree[p[1]]++;                    // 入度
    }

    Deque<Integer> queue = new ArrayDeque<>();
    for (int i = 0; i < n; i++)
        if (indegree[i] == 0) queue.offer(i);    // ① 零入度入队

    List<Integer> result = new ArrayList<>();
    while (!queue.isEmpty()) {
        int u = queue.poll();
        result.add(u);                       // ② 输出
        for (int v : graph[u]) {
            if (--indegree[v] == 0) queue.offer(v);   // ③ 减到 0 入队
        }
    }
    return result.size() == n ? result : new ArrayList<>();  // ⚠️ 判环
}
// ⚠️ 面试必答："Kahn 模板四步——建图、入度、
//   零度入队、出队减邻居。输出不足判环。"
```

---

## 3. DFS 版与判环

### 3.1 DFS 拓扑（栈序）

```
DFS 版：后序入栈 → 逆序即拓扑
  dfs(u)：访问所有邻居 → u 入栈（u 的后置都已完成）
  ⚠️ 栈逆序 = 拓扑序（后置先完成 → 前置后入栈）

⚠️ 判环：三色标记（白/灰/黑）
  灰（正在访问）→ 再次遇到 = 环！

⚠️ 面试必答：
"DFS 拓扑 = 后序入栈逆序输出；
 判环用三色——'灰色回边'即环
 （正在访问的祖先再次被访问）。"
```

### 3.2 三色标记模板

```java
/** DFS 拓扑 + 三色判环 */
int[] color;                                // 0 白（未访问）/1 灰（访问中）/2 黑（完成）
List<Integer> order = new ArrayList<>();
boolean hasCycle = false;

boolean topologicalDFS(int n, List<Integer>[] graph) {
    color = new int[n];
    for (int i = 0; i < n; i++)
        if (color[i] == 0 && !dfs(i, graph)) return false;   // 有环
    Collections.reverse(order);              // ⚠️ 逆序 = 拓扑
    return true;
}

boolean dfs(int u, List<Integer>[] graph) {
    color[u] = 1;                            // 灰：开始访问
    for (int v : graph[u]) {
        if (color[v] == 1) return false;     // ⚠️ 灰回边 → 环！
        if (color[v] == 0 && !dfs(v, graph)) return false;
    }
    color[u] = 2;                            // 黑：完成
    order.add(u);                            // 后序入栈
    return true;
}
// ⚠️ 面试必答："DFS 拓扑三色——
//   灰 = 在栈中（回边即环）、黑 = 完成（入栈）、
//   白 = 未访问。Kahn 判环 vs 三色判环
//   二选一，面试常问对比。"
```

### 3.3 Kahn vs DFS 对比

| 维度 | Kahn（BFS） | DFS |
|------|:---:|:---:|
| 实现 | 队列 + 入度 | 递归 + 三色 |
| 判环 | 输出数 < n | 灰回边 |
| 顺序 | 天然字典序（可调） | 需逆序 |
| 栈风险 | 无 | 深图可能溢出 |

```
⚠️ 面试表达：
"Kahn 迭代无栈风险、输出顺序可控；
 DFS 递归简洁、三色判环直观。
 面试默认 Kahn（工程更稳）。"
```

---

## 4. 分层拓扑：并行课程

### 4.1 分层的含义

```
分层拓扑：按"深度"分层输出
  层 0：入度 0（无依赖）
  层 k：依赖层 k-1 的节点

⚠️ 应用：
  1136 并行课程：最少学期数 = 层数
  310 最小高度树：从叶子剥层（反向拓扑）

⚠️ 面试必答：
"分层拓扑 = 每轮处理一层——
 层数即'并行轮数'（1136 学期数）、
 反向剥层找重心（310）。"
```

### 4.2 分层实现（1136）

```java
/** 1136. 并行课程：最少学期数（分层 Kahn） */
int minSemesters(int n, int[][] relations) {
    // 建图 + 入度（同上）
    int semesters = 0, processed = 0;
    Deque<Integer> queue = new ArrayDeque<>();
    for (int i = 1; i <= n; i++)
        if (indegree[i] == 0) queue.offer(i);

    while (!queue.isEmpty()) {
        int size = queue.size();             // ⚠️ 当前层大小
        for (int i = 0; i < size; i++) {     // 一层一学期
            int u = queue.poll();
            processed++;
            for (int v : graph[u])
                if (--indegree[v] == 0) queue.offer(v);
        }
        semesters++;                         // ⚠️ 每层 +1 学期
    }
    return processed == n ? semesters : -1;  // 判环
}
// ⚠️ 面试必答："分层 = 每轮取'整层'——
//   queue.size() 是当前层大小；
//   轮数即最少并行轮数。"
```

---

## 5. 拓扑 + DP：最长路径

### 5.1 DAG 上的 DP

```
DAG 最长路径（2050 并行课程 III）：
  拓扑序保证"前置先算" → 按序 DP

dp[v] = max(dp[u] + cost[v])（所有前置 u）

⚠️ 为什么必须拓扑：
  有环 → DP 顺序无法确定（依赖循环）
  拓扑序 = DAG 的"自然 DP 顺序"

⚠️ 面试必答：
"DAG 上的 DP 用拓扑序——
 拓扑序保证前置节点先算完，
 dp 递推自然成立。有环则 DP 失效。"
```

### 5.2 实现要点（2050 骨架）

```java
/** 2050 并行课程 III：拓扑 + DP */
long minTime(int n, int[][] relations, int[] time) {
    // 建图 + 入度
    long[] dp = new long[n + 1];             // 完成课程的最早时间
    Deque<Integer> queue = new ArrayDeque<>();
    for (int i = 1; i <= n; i++) {
        dp[i] = time[i - 1];                 // ⚠️ 初始：自己的时长
        if (indegree[i] == 0) queue.offer(i);
    }

    while (!queue.isEmpty()) {
        int u = queue.poll();
        for (int v : graph[u]) {
            dp[v] = Math.max(dp[v], dp[u] + time[v - 1]);   // ⚠️ 最晚前置 + 自己
            if (--indegree[v] == 0) queue.offer(v);
        }
    }
    long best = 0;
    for (int i = 1; i <= n; i++) best = Math.max(best, dp[i]);
    return best;
}
// ⚠️ 面试必答："2050 = 拓扑序 DP——
//   dp[v] = max(dp[前置]) + 时长；
//   拓扑序保证前置先算。"
```

---

## 6. 拓扑的变体应用

### 6.1 变体矩阵

| 变体 | 技巧 | 代表 |
|------|------|------|
| 判环 | 输出数 < n / 三色 | 207 |
| 输出拓扑序 | Kahn 队列序 | 210 |
| 分层 | 每轮整层 | 1136 |
| 反向剥层 | 叶入队（度=1） | 310 |
| 传递闭包 | 拓扑 + bitset | 1462 |
| 字典序拓扑 | 优先队列替代队列 | 269 |
| 拓扑 + DP | 前置先算 | 2050 |

```
⚠️ 面试必答：
"拓扑七大变体——判环/输出/分层/反向/
 闭包/字典序/DP——全是 Kahn 模板的
 状态扩展（队列变优先队列、加 DP 数组）。"
```

### 6.2 269 火星词典（字典序拓扑）

```
字符间建边：比较相邻单词找第一个不同字符 → 边
⚠️ 细节：
  ① 相邻单词比较（不是全部两两）
  ② "ab" vs "abc" 前缀情况（ab 应在前 → 非法）
  ③ 字典序拓扑 → 优先队列

⚠️ 面试必答：
"火星词典 = 相邻单词提取边 + 拓扑；
 细节是前缀特判与字典序输出（优先队列）。"
```

---

## 7. 面试要点与追问

### 7.1 面试话术模板

```
"拓扑排序前提是 DAG。Kahn：零入度入队、
 出队减邻居、输出不足判环。O(V+E)。
 DFS 版：后序入栈逆序 + 三色判环。
 变体：分层（并行轮数）、DP（前置先算）、
 字典序（优先队列）、反向剥层（最小高度树）。"
```

### 7.2 高频追问 TOP 8

| # | 追问 | 标准回答 |
|:---:|------|------|
| 1 | 拓扑前提？ | DAG（有环无拓扑） |
| 2 | Kahn 流程？ | 零度入队 + 减邻居 |
| 3 | 判环？ | 输出数 < n / 三色灰回边 |
| 4 | Kahn vs DFS？ | 迭代稳定 vs 递归简洁 |
| 5 | 分层？ | 每轮整层（学期数） |
| 6 | 拓扑 + DP？ | 前置先算 dp |
| 7 | 字典序？ | 优先队列 |
| 8 | 复杂度？ | O(V+E) |

### 7.3 常见错误

| 错误 | 后果 | 修正 |
|------|------|------|
| 有环直接拓扑 | 无限/错序 | 先判环 |
| 忘入度初始化 | 空指针 | 建图时统计 |
| 分层忘 size 快照 | 层错乱 | queue.size() 先取 |
| 三色忘标记黑 | 重复访问 | 完成置 2 |
| DP 忘取 max | 时间少算 | 最晚前置 |

---

> 🎯 **核心要点**：拓扑体系 = **Kahn 模板**（零度入队 + 判环）+ **DFS 三色**（灰回边）+ **七大变体**（分层/DP/字典序/反向/闭包）。"有依赖求顺序"识别后直接套模板，判环是第一检查。

---

**上一篇**：[01-拓扑排序核心原理与Kahn模板](01-拓扑排序核心原理与Kahn模板.md) | **下一篇**：[03-拓扑排序实战与面试](03-拓扑排序实战与面试.md) | **返回总览**：[00-拓扑排序知识体系总览](00-拓扑排序知识体系总览.md)
