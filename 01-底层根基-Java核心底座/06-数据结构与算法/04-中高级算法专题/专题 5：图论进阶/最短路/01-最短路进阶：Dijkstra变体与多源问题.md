# 01 - 最短路进阶：Dijkstra 变体与多源问题

> 定位：最短路的理论纵深——Dijkstra 原理与堆优化、变体（最大概率/最小最大差/零一 BFS）、Bellman-Ford 限步、Floyd 多源、状态压缩 BFS

## 📚 目录

1. [Dijkstra 原理与堆优化](#1-dijkstra-原理与堆优化)
2. [Dijkstra 的三大变体](#2-dijkstra-的三大变体)
3. [Bellman-Ford：限步数最短路](#3-bellman-ford限步数最短路)
4. [Floyd：多源最短路](#4-floyd多源最短路)
5. [0-1 BFS 与双端队列](#5-0-1-bfs-与双端队列)
6. [最短路算法选型地图](#6-最短路算法选型地图)
7. [面试要点与追问](#7-面试要点与追问)

---

## 1. Dijkstra 原理与堆优化

### 1.1 原理

```
Dijkstra：非负权图的单源最短路
  ① 贪心：每次取"未确定的最短距离最小"的节点
  ② 松弛：用该节点更新邻居距离
  ③ 每个节点确定一次 → O(V·(V+E)) 朴素

⚠️ 正确性前提：非负权（贪心成立）
  负权 → Dijkstra 失效 → Bellman-Ford/SPFA

⚠️ 面试必答：
"Dijkstra = 贪心 + 松弛——
 每次取距离最小者（非负权保证不再被更新），
 松弛邻居。负权会破坏贪心正确性。"
```

### 1.2 堆优化实现（743 模板）

```java
/** Dijkstra（堆优化）核心模板 */
int[] dijkstra(int n, List<int[]>[] graph, int start) {
    int[] dist = new int[n + 1];
    Arrays.fill(dist, Integer.MAX_VALUE);
    dist[start] = 0;

    // ⚠️ 优先队列：{距离, 节点}（距离小的先出）
    PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[0] - b[0]);
    pq.offer(new int[]{0, start});

    while (!pq.isEmpty()) {
        int[] cur = pq.poll();
        int d = cur[0], u = cur[1];
        if (d > dist[u]) continue;            // ⚠️ 懒删除（旧记录跳过）
        for (int[] e : graph[u]) {
            int v = e[0], w = e[1];
            if (dist[u] + w < dist[v]) {      // 松弛
                dist[v] = dist[u] + w;
                pq.offer(new int[]{dist[v], v});
            }
        }
    }
    return dist;
}
// ⚠️ 复杂度：O(E log V)（每边入堆一次）
// ⚠️ 面试必答："堆优化 Dijkstra = 优先队列 +
//   懒删除（dist 更新过时跳过）——O(E log V)。"
```

---

## 2. Dijkstra 的三大变体

### 2.1 变体总览

| 变体 | 距离定义 | 代表题 |
|------|---------|--------|
| 最大概率 | 概率相乘（log 转换或直接贪心） | 1514 |
| 最小最大差 | 路径上最大边最小 | 1631、778 |
| 最小费用 | 权值相加 | 743 标准 |

```
⚠️ 面试必答：
"Dijkstra 变体 = 改'距离定义'——
 贪心前提（单调性）不变：
 概率（相乘递减）、最大差（取 max 单调）。
 只要'确定节点不再被更新'成立，模板照用。"
```

### 2.2 最大概率（1514）

```java
/** 1514. 概率最大的路径（Dijkstra 变体） */
// ⚠️ 距离 = 概率乘积（单调递减），用最大堆
PriorityQueue<double[]> pq = new PriorityQueue<>((a, b) ->
        Double.compare(b[0], a[0]));         // ⚠️ 最大堆（概率大优先）

// 松弛：prob[v] = max(prob[v], prob[u] × w)
// ⚠️ 面试必答："1514 = Dijkstra 取最大——
//   概率相乘单调递减，贪心仍成立；
//   只是堆变最大堆、松弛变取 max。"
```

### 2.3 最小最大差（1631）

```java
/** 1631. 最小体力消耗路径（Dijkstra 变体） */
// ⚠️ 距离 = 路径上最大边差（取 max 单调）
// 松弛：effort[v] = min(effort[v], max(effort[u], 边差))
// ⚠️ 面试必答："1631 = Dijkstra 的 max 语义——
//   路径代价是'最大边'，松弛用 max 组合；
//   贪心前提（代价单调不减）仍成立。"
```

---

## 3. Bellman-Ford：限步数最短路

### 3.1 原理与适用

```
Bellman-Ford：
  迭代 V-1 轮，每轮松弛所有边
  → 第 k 轮后得到"最多 k 条边"的最短路

⚠️ 适用：
  ① 负权图（Dijkstra 失效）
  ② 限步数（787 K 站中转）
  ③ 判负环（第 V 轮仍松弛 → 负环）

⚠️ 面试必答：
"Bellman-Ford = 全边松弛 V-1 轮——
 第 k 轮 = 最多 k 条边的最短路；
 负权可用、可判负环、可限步数。"
```

### 3.2 限步数实现（787 关键点）

```java
/** 787. K 站中转最便宜（Bellman-Ford 限步） */
int findCheapestPrice(int n, int[][] flights, int src, int dst, int k) {
    int[] dist = new int[n];
    Arrays.fill(dist, Integer.MAX_VALUE);
    dist[src] = 0;

    for (int i = 0; i <= k; i++) {           // ⚠️ 最多 k 次中转 = k+1 段
        int[] next = dist.clone();           // ⚠️ 快照（本轮基于上轮）
        for (int[] f : flights) {
            if (dist[f[0]] != Integer.MAX_VALUE) {
                next[f[1]] = Math.min(next[f[1]], dist[f[0]] + f[2]);
            }
        }
        dist = next;
    }
    return dist[dst] == Integer.MAX_VALUE ? -1 : dist[dst];
}
// ⚠️ 快照的必要性：每轮必须基于"上一轮结果"（防同轮串联）
// ⚠️ 面试必答："787 = Bellman-Ford 限步——
//   k+1 轮松弛、每轮基于快照（防串联超步）。"
```

---

## 4. Floyd：多源最短路

### 4.1 原理

```
Floyd-Warshall：任意两点最短路
  dp[k][i][j] = 经过前 k 个中转的最短路
  滚动：dp[i][j] = min(dp[i][j], dp[i][k] + dp[k][j])

⚠️ 复杂度：O(V³)——稠密图/多源查询才用

⚠️ 面试必答：
"Floyd = 三重循环中转点——
 O(V³)，适合'多源查询'（任意两点）；
 单源用 Dijkstra 更优。"
```

### 4.2 实现要点（1334）

```java
/** Floyd 模板（1334 阈值距离内最少城市） */
int[][] floyd(int n, int[][] edges) {
    int INF = Integer.MAX_VALUE / 2;         // ⚠️ 防加和溢出
    int[][] dist = new int[n][n];
    for (int[] row : dist) Arrays.fill(row, INF);
    for (int i = 0; i < n; i++) dist[i][i] = 0;
    for (int[] e : edges) {
        dist[e[0]][e[1]] = Math.min(dist[e[0]][e[1]], e[2]);
        dist[e[1]][e[0]] = Math.min(dist[e[1]][e[0]], e[2]);
    }

    for (int k = 0; k < n; k++)              // ⚠️ 中转点最外层
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                if (dist[i][k] + dist[k][j] < dist[i][j])
                    dist[i][j] = dist[i][k] + dist[k][j];
    return dist;
}
// ⚠️ 面试必答："Floyd 模板 = 中转点最外层
//   （k 必须在外层）+ INF 防溢出。"
```

---

## 5. 0-1 BFS 与双端队列

### 5.1 0-1 BFS

```
边权只有 0 或 1 的最短路：
  ⚠️ 双端队列替代优先队列：
    0 权边 → 队首插入
    1 权边 → 队尾插入
  → O(V+E)（优于 Dijkstra 的 O(E log V)）

⚠️ 面试必答：
"0-1 BFS = 双端队列——
 0 权前插、1 权后插，
 队列天然有序 → O(V+E)。"
```

### 5.2 适用场景

```
识别信号："边权只有 0/1" 或 "代价 0/1"
  例：矩阵中'免费一次'类问题、
      开关类（+1 代价）问题

⚠️ 面试表达：
"'0/1 边权'识别后直接用 0-1 BFS——
 比 Dijkstra 快一个 log 因子。"
```

---

## 6. 最短路算法选型地图

### 6.1 选型总表

| 场景 | 算法 | 复杂度 |
|------|------|:---:|
| 非负权单源 | Dijkstra（堆） | O(E log V) |
| 负权 | Bellman-Ford/SPFA | O(VE) |
| 限步数 | Bellman-Ford 快照 | O(kE) |
| 多源 | Floyd | O(V³) |
| 0/1 边权 | 0-1 BFS | O(V+E) |
| 无权 | BFS | O(V+E) |

```
⚠️ 面试必答：
"最短路六算法——无权 BFS、0/1 边 0-1 BFS、
 非负 Dijkstra、负权 BF/SPFA、限步 BF、
 多源 Floyd。'图的性质'决定选型。"
```

### 6.2 决策流程

```
边权？
  无权 → BFS
  0/1 → 0-1 BFS
  非负 → Dijkstra
  负权 → Bellman-Ford/SPFA
  多源查询 → Floyd
  限步数 → BF 快照

⚠️ 面试必答：
"'边权类型 + 查询类型'两问定算法——
 这是最短路选型的完整决策树。"
```

---

## 7. 面试要点与追问

### 7.1 面试话术模板

```
"最短路选型：无权 BFS、0/1 边 0-1 BFS、
 非负 Dijkstra（堆 O(E log V)）、
 负权/限步 BF、多源 Floyd。
 Dijkstra 变体改'距离语义'（概率/max）。
 关键：贪心前提 = 距离单调不减。"
```

### 7.2 高频追问 TOP 8

| # | 追问 | 标准回答 |
|:---:|------|------|
| 1 | Dijkstra 前提？ | 非负权 |
| 2 | 堆优化？ | O(E log V) 懒删除 |
| 3 | 变体？ | 概率/max/费用 |
| 4 | BF 限步？ | 快照防串联 |
| 5 | Floyd？ | O(V³) 多源 |
| 6 | 0-1 BFS？ | 双端队列 O(V+E) |
| 7 | 负权？ | BF/SPFA |
| 8 | 判负环？ | BF 第 V 轮 |

### 7.3 常见错误

| 错误 | 后果 | 修正 |
|------|------|------|
| 负权用 Dijkstra | 结果错 | BF/SPFA |
| BF 忘快照 | 步数超限 | clone |
| Floyd 中转层错 | 结果错 | k 最外层 |
| INF 加和溢出 | 结果错 | MAX/2 |
| 0-1 BFS 用普通队列 | 顺序错 | 双端 |

---

> 🎯 **核心要点**：最短路 = **选型决策**（边权 + 查询类型）+ **Dijkstra 变体**（距离语义可改）+ **限步/多源**（BF 快照/Floyd）。"图的性质决定算法"是最短路题的第一原则。

---

**返回总览**：[00-算法汇总知识体系总览](../../../05-综合与扩展/刷题实战与综合/算法汇总/00-算法汇总知识体系总览.md) | **上一篇**：[00-最短路专题精要](00-最短路专题精要.md) | **下一篇**：[02-最短路高频题解](02-最短路高频题解.md)
