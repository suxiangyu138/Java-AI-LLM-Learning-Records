# 01-双模板深潜：Kahn与DFS与环定位
> 一句话：Kahn 判环直观、DFS 三色能定位环路径——双模板的选型由需求决定，"判有无"用 Kahn、"找位置"用 DFS

## 📚 目录
1. [双模板对比：Kahn vs DFS](#1-双模板对比kahn-vs-dfs)
2. [Kahn 深潜：判环与分层基础](#2-kahn-深潜判环与分层基础)
3. [DFS 深潜：三色与后序反转](#3-dfs-深潜三色与后序反转)
4. [环定位：三色的工程价值](#4-环定位三色的工程价值)
5. [双模板选型决策](#5-双模板选型决策)
6. [识别信号与易错点](#6-识别信号与易错点)

---

## 1. 双模板对比：Kahn vs DFS

| 维度 | Kahn（BFS） | DFS 三色 |
|------|:---:|:---:|
| 思路 | 入度消解 | 递归栈三态 |
| 判环 | 计数 < n（直观） | 遇"访问中" |
| 环定位 | 不能 | **能（记录路径）** |
| 字典序 | ✔（堆控制） | 难控制 |
| 分层 | ✔（size 快照） | 不自然 |
| 拓扑+DP | ✔（天然按序） | 需后序反转 |
| 栈溢出 | 无 | 大图可能 |
| 工程首选 | **✔** | 定位场景 |

```text
⚠️ 选型主线:
  "判有无 → Kahn（直观）；找位置 → DFS（三色）
   工程调度 → Kahn（可分层/DP/字典序）"
```

> 🎯 **核心认知**："**Kahn 是'全能主力'（判环/分层/DP 都能扩展）、DFS 是'定位专家'（环路径精准）**。面试两版都要能写——'判环 Kahn、定位 DFS'是标准分工。"

## 2. Kahn 深潜：判环与分层基础

### 2.1 判环的三种表达

```java
/**
 * Kahn 判环: 处理数 vs 节点数
 * 三种形态: boolean / 计数 / 输出序
 */
// ① boolean 判环（207）: processed == n
// ② 计数（通用）: 返回 processed（< n 即有环）
// ③ 输出序（210）: order 数组长度 < n 即有环

// 分层基础（02 文件详讲）: size 快照——
//   每层入度 0 的节点整体弹出
// ⚠️ Kahn 的扩展力: 一个模板承载判环/分层/字典序/DP
```

### 2.2 复杂度与空间

```text
时间: O(V+E)（每节点入队一次、每边处理一次）
空间: O(V+E)（邻接表 + 入度）

⚠️ 面试表达:
  "Kahn 每节点入队一次、每边消解一次——
   O(V+E) 是图遍历的理论下界"
```

> 🎯 **Kahn 的扩展性**："**一个 Kahn 模板 = 判环/分层/字典序/DP 的公共底座**——02/03 文件的变体都只是改队列或加状态。"

## 3. DFS 深潜：三色与后序反转

### 3.1 三色 DFS 完整实现

```java
/**
 * DFS 拓扑排序（三色 + 后序反转）
 * 时间 O(V+E)，空间 O(V) 栈
 */
int[] topologicalSortDFS(int n, List<List<Integer>> graph) {
    int[] color = new int[n];                    // ① 0 未访问 / 1 访问中 / 2 完成
    List<Integer> order = new ArrayList<>();

    for (int i = 0; i < n; i++) {
        if (color[i] == 0 && !dfs(graph, i, color, order)) {
            return new int[0];                   // ② 有环 → 无拓扑序
        }
    }
    Collections.reverse(order);                  // ③ ⚠️ 后序反转 = 拓扑序
    return order.stream().mapToInt(Integer::intValue).toArray();
}

private boolean dfs(List<List<Integer>> graph, int node, int[] color, List<Integer> order) {
    color[node] = 1;                             // ④ 访问中（递归栈上）
    for (int next : graph.get(node)) {
        if (color[next] == 1) return false;      // ⑤ 遇到访问中 = 环
        if (color[next] == 0 && !dfs(graph, next, color, order)) return false;
    }
    color[node] = 2;                             // ⑥ 完成
    order.add(node);                             // ⑦ 后序收集
    return true;
}
// 推演 0→1→2：DFS 顺序 0→1→2（后序收集 [2,1,0]）→ 反转 [0,1,2] ✓
// ⚠️ 为什么反转: 后序 = "先子后父"——父的先修在最后完成
//   反转后"先父后子" = 拓扑序
```

### 3.2 三色 vs 简单 visited

```text
简单 visited: 只区分"访问过/没访问过"——
  无法区分"当前路径上"（访问中）与"其他路径"（完成）
  → 无法判环（把完成节点当可走）

三色: 0/1/2 三态——
  遇"访问中"（1）= 当前路径绕回自己 = 环

⚠️ 记忆: "1 在递归栈上——遇到栈上的节点就是环"
```

> 🎯 **三色的灵魂**："**'访问中'状态是环的探测器——递归栈上的节点被再次遇到 = 环**。与简单 visited 的差异（能否区分路径），是 DFS 判环的核心。"

## 4. 环定位：三色的工程价值

### 4.1 精确定位环路径

```java
/**
 * 环定位: 三色 DFS 记录路径
 * 工程: 循环依赖报错（Maven/Gradle/系统服务）
 */
List<Integer> findCycle(List<List<Integer>> graph) {
    int n = graph.size();
    int[] color = new int[n];
    List<Integer> path = new ArrayList<>();      // ① 递归路径栈

    for (int i = 0; i < n; i++) {
        if (color[i] == 0) {
            List<Integer> cycle = dfsCycle(graph, i, color, path);
            if (cycle != null) return cycle;     // ② 找到环
        }
    }
    return null;
}

private List<Integer> dfsCycle(List<List<Integer>> graph, int node,
                               int[] color, List<Integer> path) {
    color[node] = 1;
    path.add(node);                              // ③ 路径入栈

    for (int next : graph.get(node)) {
        if (color[next] == 1) {                  // ④ ⚠️ 找到环起点
            int start = path.indexOf(next);      //    定位环在路径中的位置
            return new ArrayList<>(path.subList(start, path.size()));  // ⑤ 环路径
        }
        if (color[next] == 0) {
            List<Integer> cycle = dfsCycle(graph, next, color, path);
            if (cycle != null) return cycle;
        }
    }
    path.remove(path.size() - 1);                // ⑥ 回溯
    color[node] = 2;
    return null;
}
// 推演 0→1→2→0：路径 [0,1,2] 时 2 的邻居 0 是"访问中" → 环 [0,1,2] ✓
// ⚠️ 工程价值: "循环依赖报错 = 输出环路径"——
//   与只报"有环"相比，用户能直接看到依赖循环在哪
```

> 🎯 **环定位的价值**："**'报'有环'升级为'报环在哪'——三色 + 路径栈记录环节点**。Maven/Gradle 的循环依赖报错正是这个机制，工程谈资加分。"

## 5. 双模板选型决策

```text
① 判有无环（207）: Kahn（计数）或 DFS（三色）皆可
② 输出拓扑序（210）: Kahn（出队序）或 DFS（后序反转）
③ 字典序（B3644）: 必须 Kahn + 堆
④ 分层/并行（1136）: 必须 Kahn（size 快照）
⑤ 环定位（报错）: DFS 三色（Kahn 做不到）
⑥ 拓扑 + DP（2050）: Kahn（天然按序出队）

⚠️ 一句话:
  "调度/字典序/DP → Kahn；环定位 → DFS"
```

## 6. 识别信号与易错点

### 6.1 识别信号

```text
"判环 + 输出序" → 双模板皆可（偏好 Kahn）
"字典序/分层/并行" → Kahn 变体
"定位环/循环依赖报错" → DFS 三色
"拓扑 + 最长路径/计数" → Kahn + DP

⚠️ 进阶信号: "依赖 + 时间/权值" → 拓扑 + DP（03 文件）
```

### 6.2 易错点

| # | 易错点 | 正确写法 | 后果 |
|:---:|------|------|------|
| 1 | DFS 忘后序反转 | order 收集后 reverse | 拓扑序反 |
| 2 | 三色忘置 2 | 返回前 color=2 | 误判环 |
| 3 | 环定位忘回溯 | path 弹出 | 路径污染 |
| 4 | indexOf 定位错 | 环起点 = path 中首次出现 | 环路径错 |
| 5 | 判环忘计数比较 | processed == n | 有环误判 |
| 6 | 字典序用 DFS | Kahn + 堆 | 无法控制 |
| 7 | 大图递归 DFS | Kahn/迭代栈 | 栈溢出 |

> 🎯 **核心要点**：双模板通关四件事——**① 选型主线**（调度 Kahn、定位 DFS）；**② 三色语义**（访问中 = 环）；**③ 后序反转**（拓扑序）；**④ 环定位路径栈**。**'判有无 Kahn、找位置 DFS'——双模板的分工，就是拓扑进阶的第一课**。

---

**下一篇**：[02-字典序与分层拓扑](02-字典序与分层拓扑.md)
**返回总览**：[00-拓扑排序进阶知识体系总览](00-拓扑排序进阶知识体系总览.md)
