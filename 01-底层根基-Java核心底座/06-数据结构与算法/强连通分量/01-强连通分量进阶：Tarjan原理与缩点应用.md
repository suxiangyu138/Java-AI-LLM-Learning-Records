# 01 - 强连通分量进阶：Tarjan 原理与缩点应用

> 定位：强连通分量的理论纵深——Tarjan 算法原理推导（dfn/low/栈）、缩点 DAG 化、SCC 的四大应用（最少加边/可达性/DP/2-SAT）、无向图桥与割点

## 📚 目录

1. [Tarjan 算法原理推导](#1-tarjan-算法原理推导)
2. [缩点：有向图 → DAG](#2-缩点有向图--dag)
3. [应用一：最少加边使图强连通](#3-应用一最少加边使图强连通)
4. [应用二：可达性统计与 DAG DP](#4-应用二可达性统计与-dag-dp)
5. [无向图变体：桥与割点](#5-无向图变体桥与割点)
6. [SCC 与其他图论概念](#6-scc-与其他图论概念)
7. [面试要点与追问](#7-面试要点与追问)

---

## 1. Tarjan 算法原理推导

### 1.1 核心概念

```
两个关键数组：
  dfn[u]：u 被 DFS 访问的顺序号（时间戳）
  low[u]：u 及其子树通过"回边"能到达的最早时间戳

判定：DFS 栈中节点 u 的子节点 v：
  low[v] >= dfn[u] → u 是 SCC 根 → 弹出栈中 u 之后的所有节点 = 一个 SCC

⚠️ 直觉：
  low[v] >= dfn[u] 意味着 v 的子树"翻不出" u 之上
  → u 是这个强连通分量的"出口"（根）
```

### 1.2 完整模板（00 模块已有，此处给逐行注释版）

```java
/** Tarjan 求 SCC（完整注释版） */
List<List<Integer>> result = new ArrayList<>();
int[] dfn, low;
boolean[] inStack;
Deque<Integer> stack = new ArrayDeque<>();
int timer = 0;

void tarjan(int u, List<Integer>[] graph) {
    dfn[u] = low[u] = ++timer;             // ① 时间戳
    stack.push(u);
    inStack[u] = true;

    for (int v : graph[u]) {
        if (dfn[v] == 0) {                 // ② 未访问 → 递归
            tarjan(v, graph);
            low[u] = Math.min(low[u], low[v]);    // ③ 子节点更新 low
        } else if (inStack[v]) {           // ④ 回边（栈中 = 同 SCC 候选）
            low[u] = Math.min(low[u], dfn[v]);    // ⚠️ 用 dfn 非 low！
        }
    }

    if (low[u] == dfn[u]) {                // ⑤ u 是 SCC 根
        List<Integer> comp = new ArrayList<>();
        while (true) {
            int x = stack.pop();
            inStack[x] = false;
            comp.add(x);
            if (x == u) break;             // 弹出到 u 为止
        }
        result.add(comp);
    }
}
// ⚠️ 为什么回边用 dfn[v] 而非 low[v]：
//   low[v] 可能包含"已弹出的 SCC"的信息（会串号）
//   dfn[v] 保证只利用"仍在栈中"的祖先信息
// ⚠️ 复杂度：O(V + E)（每个节点/边恰好一次）
```

### 1.3 手推示例

```
图：1→2, 2→3, 3→1, 3→4, 4→5, 5→4

DFS 顺序：
  1(dfn=1) → 2(dfn=2) → 3(dfn=3) → 1（回边：low[3]=1）
  3 → 4(dfn=4) → 5(dfn=5) → 4（回边：low[5]=4）
  low[4]=4 → 4 是根 → 弹出 {5,4}（SCC1）
  low[3]=1 ≠ dfn[3]=3 继续
  3 结束：low[3]=1 ≠ 3 → 不弹
  2 结束：low[2]=1 ≠ 2 → 不弹
  1 结束：low[1]=1 == 1 → 弹出 {3,2,1}（SCC2）
结果：{4,5} 与 {1,2,3} ✓
```

---

## 2. 缩点：有向图 → DAG

### 2.1 缩点的意义

```
缩点：把每个 SCC 压缩成一个节点
  → 原图变成 DAG（有向无环图）！

⚠️ 为什么重要：
  DAG 上可以拓扑排序 → 可以 DP → 可以贪心
  原图的复杂环结构 → 压缩为"无环骨架"

⚠️ 面试必答：
"缩点把任意有向图变成 DAG——
 环被压缩成节点，图论问题（可达性/最长路/最小加边）
 全部落到 DAG 上求解。这是 SCC 的最大价值。"
```

### 2.2 缩点代码

```java
/** 缩点：SCC → 节点，构建 DAG */
int[] compId;                              // 节点 → SCC 编号
List<Integer>[] dag;                       // 缩点后的 DAG

void buildDAG(List<Integer>[] graph) {
    int n = graph.length;
    compId = new int[n];
    int compCount = 0;
    for (var comp : result) {              // result 来自 Tarjan
        for (int v : comp) compId[v] = compCount;
        compCount++;
    }
    // 建 DAG：原边 (u,v) 若跨 SCC → DAG 边 (compId[u], compId[v])
    dag = new List[compCount];
    for (int i = 0; i < compCount; i++) dag[i] = new ArrayList<>();
    for (int u = 0; u < n; u++)
        for (int v : graph[u])
            if (compId[u] != compId[v])    // ⚠️ 只保留跨 SCC 边
                dag[compId[u]].add(compId[v]);
}
// ⚠️ DAG 性质：无环（SCC 定义保证）
// 应用：可达性（缩点后拓扑 DP）、最长路径（有向图最长路）
```

---

## 3. 应用一：最少加边使图强连通

### 3.1 经典结论

```
问题：有向图最少加几条边变成强连通图

结论（缩点后）：
  设缩点后入度为 0 的 SCC 数 = in0，出度为 0 的 SCC 数 = out0
  答案 = max(in0, out0)
  特例：原图本身强连通（1 个 SCC）→ 答案 0

⚠️ 直觉：
  入度 0 的 SCC 需要"被进入"（加入边）
  出度 0 的 SCC 需要"能出去"（加出边）
  每条边同时解决一个入 0 和一个出 0 → 取 max

⚠️ 面试必答：
"最少加边 = max(入度 0 的 SCC 数, 出度 0 的 SCC 数)。
 每条新边同时补一个'源头'和一个'尽头'。"
```

### 3.2 完整实现

```java
/** 最少加边使图强连通 */
int minEdgesToStronglyConnected(List<Integer>[] graph) {
    // ① Tarjan 求 SCC → 缩点
    // ② 统计缩点后出入度
    int compCount = dag.length;
    boolean[] hasIn = new boolean[compCount];
    boolean[] hasOut = new boolean[compCount];
    for (int u = 0; u < compCount; u++)
        for (int v : dag[u]) {
            hasOut[u] = true;
            hasIn[v] = true;
        }
    int in0 = 0, out0 = 0;
    for (int i = 0; i < compCount; i++) {
        if (!hasIn[i]) in0++;
        if (!hasOut[i]) out0++;
    }
    if (compCount == 1) return 0;          // ⚠️ 已强连通
    return Math.max(in0, out0);            // 结论
}
// 应用场景：
//   最少添加"单行道"使所有路口互通
//   最少添加依赖边使模块互相可达
```

---

## 4. 应用二：可达性统计与 DAG DP

### 4.1 SCC + DP 的模式

```
有向图上的 DP 问题（最长路/最大值）：
  ① 环 → 无法直接 DP（无限循环）
  ② 缩点 → DAG → 拓扑序 DP ⭐

⚠️ 通用流程：
  Tarjan 缩点 → 拓扑排序 → 按拓扑序 DP
  ⚠️ SCC 内部也可做 DP（环内累积）

⚠️ 面试必答：
"有向图 DP 的前提是 DAG——有环就缩点。
 缩点后按拓扑序 DP，答案 = 所有 SCC 的
 聚合值。2065 路径最大价值就是模板。"
```

### 4.2 模板（2065 路径最大价值骨架）

```java
/** 缩点 + 拓扑 DP（2065 路径最大价值） */
int maxPathValue(List<Integer>[] graph, int[] values) {
    // ① Tarjan 缩点
    // ② 每个 SCC 的价值 = Σ 节点价值
    long[] compValue = new long[compCount];
    for (int u = 0; u < n; u++) compValue[compId[u]] += values[u];

    // ③ 拓扑排序（Kahn）+ DP
    int[] indegree = new int[compCount];
    for (int u = 0; u < compCount; u++)
        for (int v : dag[u]) indegree[v]++;

    long[] dp = new long[compCount];       // 到达该 SCC 的最大价值
    Deque<Integer> queue = new ArrayDeque<>();
    for (int i = 0; i < compCount; i++)
        if (indegree[i] == 0) queue.offer(i);

    long best = 0;
    while (!queue.isEmpty()) {
        int u = queue.poll();
        dp[u] += compValue[u];             // 加上自身
        best = Math.max(best, dp[u]);
        for (int v : dag[u]) {
            dp[v] = Math.max(dp[v], dp[u]);
            if (--indegree[v] == 0) queue.offer(v);
        }
    }
    return (int) best;
}
// ⚠️ 注意：2065 有起点终点限制 → 加"只从起点可达"过滤
// ⚠️ 面试表达："环内价值先聚合（SCC 和），
//   DAG 上拓扑 DP 取最大——'缩点+DP'两步走。"
```

---

## 5. 无向图变体：桥与割点

### 5.1 Tarjan 的双重身份

```
同一套 dfn/low 思想：
  有向图 → 强连通分量
  无向图 → 桥（割边）与割点

无向图桥的判定：
  边 (u, v)（v 是 u 的子节点）是桥 ⟺ low[v] > dfn[u]
  ⚠️ 注意无向图"父边"要跳过（否则回边误判）

⚠️ 面试必答：
"Tarjan 一套 dfn/low 思想双用——
 有向图判 SCC（low[v] >= dfn[u]），
 无向图判桥（low[v] > dfn[u]）。
 差在一个等号：SCC 根含 u，桥不含。"
```

### 5.2 桥的模板（1192 基础）

```java
/** 无向图找桥（Tarjan 变体） */
int[] dfn, low;
int timer = 0;
List<int[]> bridges = new ArrayList<>();

void findBridges(int u, int parent, List<Integer>[] graph) {
    dfn[u] = low[u] = ++timer;
    for (int v : graph[u]) {
        if (v == parent) continue;         // ⚠️ 跳过父边（无向图）
        if (dfn[v] == 0) {
            findBridges(v, u, graph);
            low[u] = Math.min(low[u], low[v]);
            if (low[v] > dfn[u]) {         // ⚠️ 桥判定（严格大于）
                bridges.add(new int[]{u, v});
            }
        } else {
            low[u] = Math.min(low[u], dfn[v]);   // 回边（非父边）
        }
    }
}
// ⚠️ 重边处理：有重边时"父边跳过"会漏判 → 用边编号跳过
// ⚠️ 割点判定（根节点特判）：
//   非根 u 是割点 ⟺ 存在子 v 使 low[v] >= dfn[u]
//   根是割点 ⟺ 有 ≥ 2 个子节点
```

---

## 6. SCC 与其他图论概念

### 6.1 概念对照

| 概念 | 图类型 | 判定 | 算法 |
|------|:---:|------|------|
| 强连通分量 | 有向 | low[v] >= dfn[u] | Tarjan |
| 弱连通 | 有向（无视方向） | 并查集/BFS | 基础遍历 |
| 桥 | 无向 | low[v] > dfn[u] | Tarjan 变体 |
| 割点 | 无向 | low[v] >= dfn[u]（根特判） | Tarjan 变体 |
| 边双连通 | 无向（无桥） | 缩桥后 | Tarjan + 缩点 |

```
⚠️ 关系：
  有向强连通 → 无向边双连通的"方向版"
  缩 SCC / 缩桥 → 都得到"树/DAG"结构

⚠️ 面试表达：
"Tarjan 思想是图论连通性的统一框架——
 dfn/low 的比值判定四件事：SCC、桥、割点、
 双连通分量。记住判定式差异即可。"
```

---

## 7. 面试要点与追问

### 7.1 面试话术模板

```
"有向图环问题 → Tarjan 求 SCC（dfn/low + 栈，
 O(V+E)），然后缩点成 DAG。
 应用：最少加边 = max(入 0, 出 0)；
 有向图 DP 先缩点再拓扑；
 无向图桥 = low[v] > dfn[u]。
 面试关键：讲清 dfn/low 语义与判定式。"
```

### 7.2 高频追问 TOP 8

| # | 追问 | 标准回答 |
|:---:|------|------|
| 1 | dfn 和 low 是什么？ | 访问时间戳 / 子树可达的最早时间戳 |
| 2 | SCC 判定式？ | low[v] >= dfn[u]（u 是 SCC 根） |
| 3 | 回边为什么用 dfn？ | low 可能含已弹出 SCC 信息（串号） |
| 4 | 缩点得到什么？ | DAG（环压缩为节点） |
| 5 | 最少加边结论？ | max(入 0, 出 0)，已强连通为 0 |
| 6 | 有向图 DP 前提？ | DAG → 先缩点再拓扑 |
| 7 | 桥的判定？ | low[v] > dfn[u]（无向图） |
| 8 | 复杂度？ | O(V+E) 线性 |

### 7.3 常见错误

| 错误 | 后果 | 修正 |
|------|------|------|
| 回边用 low[v] | SCC 串号 | 用 dfn[v] |
| 无向图忘跳父边 | 桥误判 | parent 跳过 |
| 缩点忘去重边 | DAG 重复 | compId 不同才加 |
| 最少加边忘特判 | 已强连通误算 | compCount==1 → 0 |
| 桥判定用 >= | 桥多算 | 严格 > |

---

> 🎯 **核心要点**：强连通体系 = **Tarjan**（dfn/low + 栈，O(V+E)）+ **缩点**（环 → DAG）+ **四大应用**（最少加边/可达性/DP/2-SAT）+ **无向图变体**（桥 low[v]>dfn[u]、割点）。"有环就缩点，缩点成 DAG"是图论难题的通解思路。

---

**返回总览**：[00-算法汇总知识体系总览](../算法汇总/00-算法汇总知识体系总览.md) | **上一篇**：[00-强连通分量专题精要](00-强连通分量专题精要.md) | **下一篇**：[02-强连通分量高频题解](02-强连通分量高频题解.md)
