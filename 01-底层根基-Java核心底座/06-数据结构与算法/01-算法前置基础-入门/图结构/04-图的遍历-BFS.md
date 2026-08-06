# 04 - 图的遍历：BFS

> 广度优先搜索——「洋葱式逐层扩散」。队列 + size 分层是全部模板的精髓：无权最短路径、最少步数、多源扩散、层序问题的一体化解法。与 [BFS 进阶](../../03-高频专题模块/04-BFS/) 体系呼应。

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

> ⚠️ **入队即标记 vs 出队再标记**：必须「入队时标记」——否则同一节点可能被多个邻居重复入队，正确性崩坏且可能超时。这是 BFS 最经典的低级错误。

## 3. 分层技巧：size 扩散

`int size = queue.size()` 在每轮循环开始时固定本层大小——层序遍历、步数计数的关键：

```java
// 103 二叉树的锯齿形层序（树的 BFS 是图 BFS 特例）
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
        int size = q.size();
        for (int i = 0; i < size; i++) {
            int[] cur = q.poll();
            if (cur[0] == n-1 && cur[1] == n-1) return steps;
            for (int[] d : dirs) {
                int nx = cur[0]+d[0], ny = cur[1]+d[1];
                if (nx >= 0 && nx < n && ny >= 0 && ny < n && grid[nx][ny] == 0) {
                    grid[nx][ny] = 1;
                    q.offer(new int[]{nx, ny});
                }
            }
        }
        steps++;
    }
    return -1;
}
```

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
        if (rotted) minutes++;
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
// 每个单词是节点，差一个字母的单词相连
public int ladderLength(String beginWord, String endWord, List<String> wordList) {
    Set<String> wordSet = new HashSet<>(wordList);
    if (!wordSet.contains(endWord)) return 0;
    Deque<String> q = new ArrayDeque<>();
    Set<String> visited = new HashSet<>();
    q.offer(beginWord);
    visited.add(beginWord);
    int steps = 1;
    while (!q.isEmpty()) {
        int size = q.size();
        for (int i = 0; i < size; i++) {
            String cur = q.poll();
            if (cur.equals(endWord)) return steps;
            char[] chars = cur.toCharArray();
            for (int j = 0; j < chars.length; j++) {
                char orig = chars[j];
                for (char c = 'a'; c <= 'z'; c++) {
                    chars[j] = c;
                    String next = new String(chars);
                    if (wordSet.contains(next) && !visited.contains(next)) {
                        visited.add(next);
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
| 拓扑排序（Kahn） | 「依赖顺序」 | 207 课程表（阶段三衔接） |

> 🎯 **核心要点**：BFS 验收标准——能默写三层模板（外层 while、中层 size 分层、内层 for 邻居）；三句口诀：「**入队即标记、size 分层、首次到达即最短**」；识别「最少步数/扩散/层序」信号直接套 BFS 而非 DFS。进阶（双向 BFS、状态 BFS）见 [BFS 体系](../../03-高频专题模块/专题 7：搜索专题（DFS  BFS  回溯）/BFS/00-BFS知识体系总览.md)。

---

**上一模块**：[03-图的遍历：DFS](03-图的遍历-DFS.md) ｜ **下一模块**：[05-图的应用](05-图的应用.md) ｜ **返回总览**：[00-图算法知识体系总览](00-图算法知识体系总览.md)

**【参考来源】**
- 算法导论（CLRS）第 22.2 节：广度优先搜索
- LeetCode 994 腐烂的橘子 / 1091 二进制矩阵 / 127 单词接龙
