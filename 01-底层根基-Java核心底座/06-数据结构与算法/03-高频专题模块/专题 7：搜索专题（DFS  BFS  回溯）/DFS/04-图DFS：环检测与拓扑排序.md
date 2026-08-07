# 04-图DFS：环检测与拓扑排序
> 一句话：三色标记判环、后序反转出拓扑——207 课程表、210 课程表 II、785 二分图、1192 关键连接，图 DFS 四大任务

## 📚 目录
1. [图 DFS 模型：visited 的三种形态](#1-图-dfs-模型visited-的三种形态)
2. [207 课程表（有向图环检测，三色标记）](#2-207-课程表有向图环检测三色标记)
3. [210 课程表 II（拓扑排序，DFS 后序反转）](#3-210-课程表-iitopological-排序dfs-后序反转)
4. [785 判断二分图（染色法）](#4-785-判断二分图染色法)
5. [1192 关键连接（无向图 + Tarjan 思想）](#5-1192-关键连接无向图--tarjan-思想)
6. [图 DFS 任务全景与易错点](#6-图-dfs-任务全景与易错点)

---

## 1. 图 DFS 模型：visited 的三种形态

```text
图的 visited 比树/网格复杂——需要区分"访问状态":

  ① 未访问（0）: 还没进入过
  ② 访问中（1）: 当前递归栈上（该节点后代还没处理完）
  ③ 已访问（2）: 子树处理完毕（可以安全跳过）

为什么三种状态:
  树无环 → 两种够用（访问/未访问）
  图有环 → 必须区分"访问中"——
    遇到"访问中"节点 = 环！（当前路径上绕回来了）

⚠️ 三色标记是图 DFS 环检测的核心:
  colors[node] == 1（访问中）→ 有环
  colors[node] == 2（已访问）→ 跳过（已确认无环）
```

> 🎯 **核心认知**："**三色标记是图 DFS 的灵魂——'访问中'状态把'环'从'重复访问'中区分出来**。树的两态（visited/not）在图里不够用，第三态（在栈上）就是环的探测器。"

## 2. 207 课程表（有向图环检测，三色标记）

### 2.1 题目与思路

**题**：课程先修关系是否有环（能否学完所有课）。

**DFS 三色标记**：

```java
/**
 * LeetCode 207. 课程表（DFS 环检测）
 * 三色标记: 0 未访问 / 1 访问中（当前栈上）/ 2 已访问
 * 时间 O(V+E)，空间 O(V+E)
 */
public boolean canFinish(int numCourses, int[][] prerequisites) {
    List<List<Integer>> graph = new ArrayList<>();
    for (int i = 0; i < numCourses; i++) graph.add(new ArrayList<>());
    for (int[] p : prerequisites) {
        graph.get(p[1]).add(p[0]);             // ① 建邻接表: 先修 → 后续
    }

    int[] color = new int[numCourses];         // ② 三色标记
    for (int i = 0; i < numCourses; i++) {
        if (color[i] == 0 && hasCycle(graph, i, color)) {
            return false;                      // ③ 发现环 → 无法学完
        }
    }
    return true;
}

private boolean hasCycle(List<List<Integer>> graph, int node, int[] color) {
    color[node] = 1;                           // ④ 标记访问中
    for (int next : graph.get(node)) {
        if (color[next] == 1) return true;     // ⑤ 遇到"访问中"= 环！
        if (color[next] == 0 && hasCycle(graph, next, color)) return true;
    }
    color[node] = 2;                           // ⑥ 标记已访问（子树无环）
    return false;
}
// 推演 2 门课 [[1,0]]：0 → 1 无环 → true ✓
//   [[1,0],[0,1]]：0→1→0 环 → false ✓
// ⚠️ 为什么"访问中"= 环: 当前路径上绕回自己——
//   而"已访问"说明该子树已确认无环，直接跳过
```

> 🎯 **三色标记的面试表达**："**'访问中（1）遇到自己路径上的节点 = 环，已访问（2）直接跳过'**——两态不够（会把已确认无环的子树重复扫描），三态才正确。能讲清三态语义，环检测就满分了。"

## 3. 210 课程表 II（拓扑排序，DFS 后序反转）

### 3.1 题目与思路

**题**：返回课程学习顺序（拓扑排序；有环返回空）。

**DFS 后序反转**：完成后序收集 → 反转即拓扑序：

```java
/**
 * LeetCode 210. 课程表 II（DFS 拓扑排序）
 * 后序收集 + 反转 = 拓扑序
 * 时间 O(V+E)，空间 O(V+E)
 */
public int[] findOrder(int numCourses, int[][] prerequisites) {
    List<List<Integer>> graph = new ArrayList<>();
    for (int i = 0; i < numCourses; i++) graph.add(new ArrayList<>());
    for (int[] p : prerequisites) graph.get(p[1]).add(p[0]);

    int[] color = new int[numCourses];
    List<Integer> order = new ArrayList<>();

    for (int i = 0; i < numCourses; i++) {
        if (color[i] == 0 && !dfs(graph, i, color, order)) {
            return new int[0];                 // ① 有环 → 无拓扑序
        }
    }
    Collections.reverse(order);                // ② ⚠️ 后序反转 = 拓扑序
    return order.stream().mapToInt(Integer::intValue).toArray();
}

private boolean dfs(List<List<Integer>> graph, int node, int[] color, List<Integer> order) {
    color[node] = 1;
    for (int next : graph.get(node)) {
        if (color[next] == 1) return false;    // ③ 环检测（同 207）
        if (color[next] == 0 && !dfs(graph, next, color, order)) return false;
    }
    color[node] = 2;
    order.add(node);                           // ④ 后序收集（子树完成后）
    return true;
}
// 推演 4 门课 [[1,0],[2,0],[3,1],[3,2]] → [0,1,2,3] 或 [0,2,1,3] ✓
// ⚠️ 为什么反转: 后序顺序 = "先子后父"（先修在后）——
//   反转后"先父后子"（先修在前）= 拓扑序
//   例: 0（无先修）最后完成 → 反转后在最前 ✓
```

> 💡 **DFS 拓扑 vs Kahn（BFS）**："**DFS 后序反转与 Kahn（入度队列）是拓扑排序的两版实现**——DFS 用递归栈（先深后出）、Kahn 用入度（先浅后出）。207/210 两版都能写是加分，Kahn 版见 BFS 目录。"

## 4. 785 判断二分图（染色法）

### 4.1 题目与思路

**题**：图能否二染色（相邻节点异色）。

**染色 DFS**：未染色 → 染当前色，邻居染反色；冲突 → 非二分图：

```java
/**
 * LeetCode 785. 判断二分图（染色法）
 * 时间 O(V+E)，空间 O(V)
 */
public boolean isBipartite(int[][] graph) {
    int[] color = new int[graph.length];       // 0 未染 / 1 / -1 两色
    for (int i = 0; i < graph.length; i++) {
        if (color[i] == 0 && !dfs(graph, i, 1, color)) {
            return false;                      // ① 每个连通分量都要验证
        }
    }
    return true;
}

private boolean dfs(int[][] graph, int node, int c, int[] color) {
    color[node] = c;                           // ② 染色
    for (int next : graph[node]) {
        if (color[next] == c) return false;    // ③ 邻居同色 → 冲突
        if (color[next] == 0 && !dfs(graph, next, -c, color)) return false;  // ④ 染反色
    }
    return true;
}
// 推演 [[1,3],[0,2],[1,3],[0,2]] → true（环长度为偶）✓
//   [[1,2,3],[0,2],[0,1,3],[0,2]] → false（奇环）✓
// ⚠️ 定理: 二分图 ⇔ 无奇数环——
//   染色冲突必然来自奇数环（偶数环可以交替染色）
```

> 🎯 **785 的定理价值**："**'二分图 ⇔ 无奇数环'是 785 的理论内核**——染色法本质上是在'找奇环'：染到邻居同色 = 找到奇环 = 非二分图。能讲出这个定理，785 就从'背代码'升级为'懂原理'。"

## 5. 1192 关键连接（无图 + Tarjan 思想）

### 5.1 题目与思路

**题**：无向图中删掉会让图不连通的边（桥）。

**Tarjan 核心**：`disc[i]`（发现时间）+ `low[i]`（子树能到达的最早祖先）：

```java
/**
 * LeetCode 1192. 关键连接（无向图的桥，Tarjan 思想）
 * 时间 O(V+E)，空间 O(V+E)
 */
public List<List<Integer>> criticalConnections(int n, List<List<Integer>> connections) {
    List<List<Integer>> graph = new ArrayList<>();
    for (int i = 0; i < n; i++) graph.add(new ArrayList<>());
    for (List<Integer> c : connections) {
        graph.get(c.get(0)).add(c.get(1));
        graph.get(c.get(1)).add(c.get(0));     // ① 无向图双向建边
    }

    int[] disc = new int[n];                   // ② 发现时间
    int[] low = new int[n];                    // ③ 子树可达的最早祖先
    Arrays.fill(disc, -1);
    List<List<Integer>> bridges = new ArrayList<>();
    dfs(graph, 0, -1, 0, disc, low, bridges);  // ④ 从 0 开始（连通图）
    return bridges;
}

private void dfs(List<List<Integer>> graph, int node, int parent, int time,
                 int[] disc, int[] low, List<List<Integer>> bridges) {
    disc[node] = low[node] = time;             // ⑤ 初始化
    for (int next : graph.get(node)) {
        if (next == parent) continue;          // ⑥ ⚠️ 防回父（无向图必须）
        if (disc[next] == -1) {                // ⑦ 未访问 → 递归
            dfs(graph, next, node, time + 1, disc, low, bridges);
            low[node] = Math.min(low[node], low[next]);   // ⑧ 子树信息上收
            if (low[next] > disc[node]) {      // ⑨ 桥判定: 子树回不到祖先
                bridges.add(Arrays.asList(node, next));
            }
        } else {
            low[node] = Math.min(low[node], disc[next]);  // ⑩ 回边更新 low
        }
    }
}
// 推演 n=4, [[0,1],[1,2],[2,0],[1,3]] → [[1,3]] ✓（唯一桥）
// ⚠️ 桥判定: low[next] > disc[node]——子树最远只能回到 next 自己
//   （回不到 node 或更早祖先）→ 删掉这条边子树孤立
```

> 💡 **1192 的面试定位**："**Tarjan 是图 DFS 的'天花板'——disc/low 双数组 + 桥判定**。面试中 1192 属于 hard，能写出来是加分项；讲清'low > disc = 桥'的判定语义即可（详细版见图专题 11）。"

## 6. 图 DFS 任务全景与易错点

### 6.1 四大任务

| 任务 | 核心技巧 | 代表题 |
|------|------|:---:|
| 环检测（有向） | 三色标记（1 = 访问中） | 207 |
| 拓扑排序 | 后序收集 + 反转 | 210 |
| 二分图判定 | 染色法（反色 + 冲突检测） | 785 |
| 桥/关键连接 | disc/low + 防回父 | 1192 |
| 连通分量 | 遍历计数（每分量一次 DFS） | 323/200（图版） |

### 6.2 易错点

| # | 易错点 | 正确写法 | 后果 |
|:---:|------|------|------|
| 1 | 图 DFS 只用两态 visited | 三色（区分访问中） | 环检测失效 |
| 2 | 无向图忘防回父 | 传 parent 参数 | 父子来回走死循环 |
| 3 | 拓扑忘反转 | 后序收集后 reverse | 顺序反（先修在后） |
| 4 | 785 只染一个分量 | 遍历所有节点 | 非连通图漏检 |
| 5 | 1192 回边更新错 | 回边更新 low（非 disc） | 桥判定错误 |
| 6 | 建图方向反 | 先修 → 后续（p[1]→p[0]） | 拓扑序反 |
| 7 | 邻接表未初始化 | 每个节点 new ArrayList | 空指针 |

> 🎯 **核心要点**：图 DFS 通关四件事——**① 三色标记**（访问中 = 环探测器）；**② 后序反转 = 拓扑**；**③ 防回父**（无向图 parent 参数）；**④ 染色法**（反色 + 冲突）。**图是 DFS 的终极战场——三色标记解决'环'、后序解决'序'、染色解决'二分'，一套递归框架承载图论四大任务**。

---

**下一模块**：[05-DFS与BFS选型及记忆化边界](05-DFS与BFS选型及记忆化边界.md)
**返回总览**：[00-DFS知识体系总览](00-DFS知识体系总览.md)
