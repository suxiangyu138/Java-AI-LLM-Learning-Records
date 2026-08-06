# 02 - BFS 广度优先搜索

> 广度优先搜索——「洋葱式逐层扩散」。队列 + size 分层是全部模板的精髓：无权最短路径、最少步数、多源扩散、层序问题的一体化解法。

## 📚 目录

1. [BFS 核心思想](#1)
2. [图 BFS 模板](#2)
3. [分层技巧：size 扩散](#3)
4. [多源 BFS](#4)
5. [无权最短路径](#5)
6. [BFS 应用清单](#6)

## 1. BFS 核心思想

**广度优先**：从起点出发，先访问所有「一步可达」的节点，再访问「两步可达」……用**队列**按层推进。

```text
       A
      / \
     B   C
    / \   \
   D   E   F

BFS 顺序：A → B → C → D → E → F
特点：逐层扩散，第一次到达 = 最短路径
```

| 关键性质 | 说明 |
|---------|------|
| 首次到达即最短 | 无权图天然保证（无权 BFS 的黄金性质） |
| 时间复杂度 | O(V + E)，每点每边各一次 |
| 空间 | O(层宽度)，可能比 DFS 大 |

> 🎯 **识别信号**：「最短/最少步数/扩散/层序」→ 直接套 BFS；「连通块/路径枚举」→ DFS。这是遍历选型的第一判断。

## 2. 图 BFS 模板

```java
// 图 BFS 万能模板
class GraphBFS {
    int bfs(int n, List<Integer>[] graph, int start, int target) {
        boolean[] visited = new boolean[n];
        Deque<Integer> queue = new ArrayDeque<>();
        queue.offer(start);
        visited[start] = true;                 // 入队即标记（铁律！）

        int steps = 0;
        while (!queue.isEmpty()) {
            int size = queue.size();           // 当前层节点数
            for (int i = 0; i < size; i++) {   // 处理完整一层
                int u = queue.poll();
                if (u == target) return steps; // 到达目标
                for (int v : graph[u]) {
                    if (!visited[v]) {
                        visited[v] = true;     // 入队即标记，防重复入队
                        queue.offer(v);
                    }
                }
            }
            steps++;                           // 一层结束，步数 +1
        }
        return -1;                             // 不可达
    }
}
```

**模板四要素**：

| 要素 | 说明 |
|------|------|
| 队列 | 层序推进的容器 |
| 入队即标记 | 防重复入队（第一铁律） |
| size 分层 | 本层节点数（步数计数） |
| 目标判定 | 出队时检查（可提前返回） |

> ⚠️ **入队即标记 vs 出队再标记**：必须「入队时标记」——否则同一节点可能被多个邻居重复入队，正确性崩坏且可能超时。这是 BFS 最经典的低级错误。

## 3. 分层技巧：size 扩散

`int size = queue.size()` 在每轮循环开始时固定本层大小——层序遍历、步数计数的关键：

```java
// 1091 二进制矩阵中的最短路径：网格 BFS 标准题
public int shortestPathBinaryMatrix(int[][] grid) {
    int n = grid.length;
    if (grid[0][0] == 1 || grid[n-1][n-1] == 1) return -1;
    int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
    Deque<int[]> q = new ArrayDeque<>();
    q.offer(new int[]{0, 0});
    grid[0][0] = 1;                            // 原地标记 visited
    int steps = 1;
    while (!q.isEmpty()) {
        int size = q.size();                   // 本层节点数
        for (int i = 0; i < size; i++) {
            int[] cur = q.poll();
            if (cur[0] == n-1 && cur[1] == n-1) return steps;
            for (int[] d : dirs) {
                int nx = cur[0]+d[0], ny = cur[1]+d[1];
                if (nx >= 0 && nx < n && ny >= 0 && ny < n && grid[nx][ny] == 0) {
                    grid[nx][ny] = 1;          // 原地标记（沉岛思想）
                    q.offer(new int[]{nx, ny});
                }
            }
        }
        steps++;                               // 一层结束步数 +1
    }
    return -1;
}
```

**size 分层的三个应用**：

| 应用 | 技巧 | 典型题 |
|------|------|--------|
| 层序输出 | 每层收集 List | 102/429 |
| 步数计数 | 每层 steps++ | 1091/127 |
| 层内处理 | 层内特殊逻辑 | 103 锯齿 |

> 💡 树层序（二叉树体系）与图层序**完全同模板**——树是图的特例，模板无缝迁移。

## 4. 多源 BFS

多个起点同时扩散——**超级源点思想**：把多个起点想象成一个虚拟源点，第一层全部入队：

```java
// 994 腐烂的橘子：多源 BFS
public int orangesRotting(int[][] grid) {
    int m = grid.length, n = grid[0].length;
    Deque<int[]> q = new ArrayDeque<>();
    int fresh = 0;
    for (int i = 0; i < m; i++)
        for (int j = 0; j < n; j++) {
            if (grid[i][j] == 2) q.offer(new int[]{i, j});   // 所有烂橘子入队
            else if (grid[i][j] == 1) fresh++;
        }
    if (fresh == 0) return 0;
    int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
    int minutes = 0;
    while (!q.isEmpty()) {
        int size = q.size();
        boolean rotted = false;
        for (int i = 0; i < size; i++) {
            int[] cur = q.poll();
            for (int[] d : dirs) {
                int nx = cur[0]+d[0], ny = cur[1]+d[1];
                if (nx >= 0 && nx < m && ny >= 0 && ny < n && grid[nx][ny] == 1) {
                    grid[nx][ny] = 2;
                    fresh--;
                    rotted = true;
                    q.offer(new int[]{nx, ny});
                }
            }
        }
        if (rotted) minutes++;                 // 有扩散才计时
    }
    return fresh == 0 ? minutes : -1;
}
```

| 多源题型 | 起点 | 经典题 |
|---------|------|--------|
| 腐烂扩散 | 所有烂橘子 | 994 腐烂的橘子 |
| 最近距离 | 所有 '0' | 542 01 矩阵 |
| 多出口逃生 | 所有出口 | 类似网格题 |

> 💡 多源 BFS 正确性：多个起点同步扩散 = 每个格子的「最近源距离」由最先到达者决定——恰好等价于「到最近起点最短距离」。

## 5. 无权最短路径

**BFS 第一性质**：无权图中，BFS 首次到达某节点时走的步数 = 最短步数。

```java
// 127 单词接龙：隐式图 + 无权最短路径（经典变形）
public int ladderLength(String beginWord, String endWord, List<String> wordList) {
    Set<String> wordSet = new HashSet<>(wordList);
    if (!wordSet.contains(endWord)) return 0;
    Deque<String> q = new ArrayDeque<>();
    Set<String> visited = new HashSet<>();     // 字符串节点：Set 代替数组
    q.offer(beginWord);
    visited.add(beginWord);
    int steps = 1;
    while (!q.isEmpty()) {
        int size = q.size();                   // 分层
        for (int i = 0; i < size; i++) {
            String cur = q.poll();
            if (cur.equals(endWord)) return steps;
            char[] chars = cur.toCharArray();
            for (int j = 0; j < chars.length; j++) {
                char orig = chars[j];
                for (char c = 'a'; c <= 'z'; c++) {   // 规则生成邻居
                    chars[j] = c;
                    String next = new String(chars);
                    if (wordSet.contains(next) && !visited.contains(next)) {
                        visited.add(next);     // 入队即标记
                        q.offer(next);
                    }
                }
                chars[j] = orig;
            }
        }
        steps++;
    }
    return 0;
}
```

**隐式图**：节点与边不显式给出，由规则推导（单词变形、状态转移、数字变换）——邻接表省了，方向/变换规则即邻接。

## 6. BFS 应用清单

| 应用 | 判定信号 | 典型题 |
|------|---------|--------|
| 无权最短路径 | 「最少步数/最短距离」 | 1091 二进制矩阵、127 单词接龙 |
| 层序处理 | 「按层/第 k 层」 | 102 层序遍历、103 锯齿层序 |
| 多源扩散 | 「所有起点同时蔓延」 | 994 腐烂橘子、542 01 矩阵 |
| 状态空间 | 「状态转移找最小步」 | 752 开锁、433 基因变化 |
| 拓扑排序（Kahn） | 「依赖顺序」 | 207 课程表（图经典算法目录） |

**BFS vs DFS 选型速记**：

```text
最少步数/扩散/层序 → BFS（首次到达即最短）
连通块/路径/判环   → DFS（递归天然）
双向 BFS（进阶）   → 起点终点同时扩散（平方级加速）
```

> 🎯 **核心要点**：BFS 验收标准——能默写三层模板（外层 while、中层 size 分层、内层 for 邻居）；三句口诀：「**入队即标记、size 分层、首次到达即最短**」；识别「最少步数/扩散/层序」信号直接套 BFS 而非 DFS；多源 BFS（超级源点思想）与隐式图（规则推导邻居）是进阶双件套；衔接[图经典算法](../图经典算法/)（Kahn 拓扑 = BFS + 入度）。

---

**上一模块**：[01-DFS 深度优先搜索](01-DFS深度优先搜索.md) ｜ **返回总览**：[00-图的遍历知识体系总览](00-图的遍历知识体系总览.md)

**【参考来源】**
- 算法导论（CLRS）第 22.2 节：广度优先搜索
- LeetCode 994 腐烂的橘子 / 1091 二进制矩阵 / 127 单词接龙
