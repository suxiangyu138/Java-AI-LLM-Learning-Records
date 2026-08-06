# 06 - Java 实现与面试实战

> 从模板到实战：五个必背 Java 模板、八道高频题解思路、面试追问与话术。图题面试的「肌肉记忆」全在这篇——闭眼能写建图、遍历、拓扑、染色。

## 📚 目录

1. [五个必背 Java 模板](#1)
2. [高频题解速览](#2)
3. [面试追问与答题话术](#3)
4. [易错点清单](#4)

## 1. 五个必背 Java 模板

### 模板一：邻接表建图

```java
// 无向无权图
List<Integer>[] graph = new List[n];
for (int i = 0; i < n; i++) graph[i] = new ArrayList<>();
for (int[] edge : edges) {
    graph[edge[0]].add(edge[1]);
    graph[edge[1]].add(edge[0]);     // 有向图删此行
}
```

### 模板二：DFS 遍历

```java
boolean[] visited = new boolean[n];
void dfs(List<Integer>[] graph, int u) {
    visited[u] = true;
    for (int v : graph[u]) {
        if (!visited[v]) dfs(graph, v);
    }
}
```

### 模板三：BFS 分层遍历

```java
boolean[] visited = new boolean[n];
Deque<Integer> q = new ArrayDeque<>();
q.offer(start);
visited[start] = true;
int steps = 0;
while (!q.isEmpty()) {
    int size = q.size();
    for (int i = 0; i < size; i++) {
        int u = q.poll();
        for (int v : graph[u]) {
            if (!visited[v]) { visited[v] = true; q.offer(v); }
        }
    }
    steps++;
}
```

### 模板四：Kahn 拓扑排序

```java
int[] inDegree = new int[n];
// ... 建图时统计入度 ...
Deque<Integer> q = new ArrayDeque<>();
for (int i = 0; i < n; i++) if (inDegree[i] == 0) q.offer(i);
List<Integer> order = new ArrayList<>();
while (!q.isEmpty()) {
    int u = q.poll();
    order.add(u);
    for (int v : graph[u]) {
        if (--inDegree[v] == 0) q.offer(v);
    }
}
// order.size() == n ? 有拓扑序 : 存在环
```

### 模板五：二分图染色

```java
int[] color = new int[n];          // 0 未染 1/-1 两色
boolean ok = true;
for (int i = 0; i < n && ok; i++) {
    if (color[i] == 0) {
        color[i] = 1;
        ok = dfsColor(graph, i, color);
    }
}
```

> 🎯 五个模板合计约 60 行——**全部闭眼默写**是图题面试的及格线。

## 2. 高频题解速览

| 题号 | 题目 | 考点 | 一句话思路 |
|------|------|------|-----------|
| 200 | 岛屿数量 | 网格 DFS | 沉岛法：遇 '1' 计数并淹没整岛 |
| 994 | 腐烂的橘子 | 多源 BFS | 烂橘全入队，逐层扩散计时 |
| 207 | 课程表 | 有向判环 | Kahn 计数 / 三色 DFS |
| 210 | 课程表 II | 拓扑排序 | Kahn 输出顺序，数量不足即环 |
| 542 | 01 矩阵 | 多源 BFS | 所有 0 入队，BFS 算最近距离 |
| 547 | 省份数量 | 连通分量 | 邻接矩阵 DFS 遍历起点计数 |
| 684 | 冗余连接 | 无向判环 | 并查集合并时发现已连通 = 多余边 |
| 785 | 判断二分图 | 染色法 | 邻居异色，冲突即否 |
| 127 | 单词接龙 | 隐式图最短路 | 单词为节点，BFS 逐字母变换 |
| 1971 | 找路径 | 可达性 | 单源 BFS/DFS |

```java
// 200 岛屿数量（模板默写验证）
public int numIslands(char[][] grid) {
    int m = grid.length, n = grid[0].length, count = 0;
    int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
    for (int i = 0; i < m; i++) {
        for (int j = 0; j < n; j++) {
            if (grid[i][j] == '1') {
                count++;
                Deque<int[]> q = new ArrayDeque<>();
                q.offer(new int[]{i, j});
                grid[i][j] = '0';
                while (!q.isEmpty()) {
                    int[] cur = q.poll();
                    for (int[] d : dirs) {
                        int x = cur[0]+d[0], y = cur[1]+d[1];
                        if (x >= 0 && x < m && y >= 0 && y < n && grid[x][y] == '1') {
                            grid[x][y] = '0';
                            q.offer(new int[]{x, y});
                        }
                    }
                }
            }
        }
    }
    return count;
}
```

## 3. 面试追问与答题话术

| 面试官追问 | 答题要点 |
|-----------|---------|
| 「BFS 和 DFS 怎么选？」 | 求**最少步数/最短**用 BFS（首次到达即最短）；**连通块/路径枚举/判环**用 DFS；空间：BFS 吃宽度、DFS 吃深度 |
| 「图用什么存？为什么？」 | 默认邻接表（稀疏图 O(V+E) 空间，遍历高效）；稠密图或频繁判边用矩阵；建图要点：无向双边、有向单边 |
| 「visited 为什么要入队/进函数时标记？」 | 防重复入队：出队时标记会导致同一节点被多个邻居重复加入，环图死循环/超时 |
| 「拓扑排序什么时候无解？」 | 图中有环（Kahn 出队数 < n）；顺带一提：拓扑序不唯一，队列按什么序出就是什么序 |
| 「无向图判环有什么坑？」 | 不能用「遇 visited 即环」（无向边来回）；要排除父节点，或用并查集合并发现已连通 |
| 「岛屿题能不能用 BFS？」 | 能——沉岛法 DFS/BFS 都行；但「最短距离」类（542/1091）必须 BFS |
| 「图很大（10⁵+）会栈溢出吗？」 | Java 递归 DFS 默认栈深有限（约万级）——大图用显式栈（Deque 模拟）或改 BFS |

## 4. 易错点清单

| # | 易错点 | 后果 | 对策 |
|---|--------|------|------|
| 1 | visited 出队/出函数才标记 | 死循环、超时 | 入队/进函数立刻标记 |
| 2 | 无向图忘加反向边 | 遍历不全 | 建图双边插入 |
| 3 | 无向判环忘排除父节点 | 假环 | `v == parent` 跳过 |
| 4 | 有向图当无向建图 | 方向错误 | 看题面「单向依赖」 |
| 5 | 网格越界判断放递归后 | 数组越界 | 先判界再访问 |
| 6 | 拓扑排序忘判数量 | 有环仍返回「成功」 | `order.size() == n` 校验 |
| 7 | BFS 层数统计位置错 | 步数差 1 | size 循环外 `steps++` |
| 8 | 节点字符串（题目用 String） | 数组下标失效 | `Map<String, List<String>>` + visited Set |
| 9 | 图不连通时只从起点遍历 | 漏节点 | 外层 for 遍历所有起点 |
| 10 | 大 n 用邻接矩阵 | MLE | n > 10⁴ 一律邻接表 |

> 🎯 **核心要点**：实战验收——打开 LeetCode 图论标签随机抽 3 题，能 15 分钟内完成「建图 + 遍历 + 提交通过」即过关；面试复盘五句话：存储选型、遍历模板、标记时机、判环差异、拓扑判解，图题面试的五个必答点全部覆盖。

---

**上一模块**：[05-图的应用](05-图的应用.md) ｜ **返回总览**：[00-图算法知识体系总览](00-图算法知识体系总览.md)

**【参考来源】**
- LeetCode 图论题单：https://leetcode.cn/studyplan/graph/
- 算法导论（CLRS）第 22 章
