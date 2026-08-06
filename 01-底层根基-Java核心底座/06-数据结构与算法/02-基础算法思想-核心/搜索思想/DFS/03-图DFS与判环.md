# 图DFS与判环
> 邻接表遍历、克隆、染色判环——DFS 在图上的四种经典应用

## 📚 目录
1. [图的存储与 DFS 遍历](#1-图的存储与-dfs-遍历)
2. [133. 克隆图](#2-133-克隆图)
3. [785. 判断二分图：染色法](#3-785-判断二分图染色法)
4. [841. 钥匙和房间](#4-841-钥匙和房间)
5. [207. 课程表：颜色法判环](#5-207-课程表颜色法判环)

---

## 1. 图的存储与 DFS 遍历

### 1.1 图的两种存储

```java
// ① 邻接表（推荐，稀疏图）
List<List<Integer>> graph = new ArrayList<>();
for (int i = 0; i < n; i++) graph.add(new ArrayList<>());
graph.get(u).add(v);          // 有向边 u → v
graph.get(u).add(v);
graph.get(v).add(u);          // 无向图要加两条

// ② 邻接矩阵（稠密图）
int[][] matrix = new int[n][n];
matrix[u][v] = 1;             // 边 u → v
```

### 1.2 图 DFS 模板

```java
/**
 * 图 DFS 模板（visited 数组防重复）
 * ⚠️ 图与树不同：树天然无环，图必须 visited！
 */
public void graphDFS(List<List<Integer>> graph, int node,
                     boolean[] visited) {
    visited[node] = true;                  // 标记

    for (int next : graph.get(node)) {
        if (!visited[next]) {
            graphDFS(graph, next, visited);
        }
    }
}
// 遍历所有连通分量：
for (int i = 0; i < n; i++) {
    if (!visited[i]) graphDFS(graph, i, visited);   // 每个分量一次
}
```

---

## 2. 133. 克隆图

### 2.1 完整实现（DFS + 哈希映射）

```java
/**
 * LeetCode 133. 克隆图
 * 深拷贝一张图（节点 + 邻居关系）
 *
 * 核心：哈希映射 原节点 → 新节点
 *   ① 已创建 → 直接返回（防无限递归！）
 *   ② 未创建 → 新建 + 递归克隆邻居
 *
 * ⚠️ 必须有 visited 映射：图可能有环，
 *    不记录已克隆节点会无限递归
 */
public Node cloneGraph(Node node) {
    if (node == null) return null;
    Map<Node, Node> map = new HashMap<>();    // 原节点 → 新节点
    return dfs(node, map);
}

private Node dfs(Node node, Map<Node, Node> map) {
    if (map.containsKey(node)) {
        return map.get(node);                 // 已克隆 → 直接返回
    }

    Node clone = new Node(node.val);          // 新建节点
    map.put(node, clone);                     // ⚠️ 先放入 map 再递归！

    for (Node neighbor : node.neighbors) {
        clone.neighbors.add(dfs(neighbor, map));   // 递归克隆邻居
    }
    return clone;
}
```

### 2.2 为什么"先放入 map 再递归"？

```
⚠️ 顺序关键：
  先 put 再递归 → 环中的节点能命中缓存
  先递归再 put → 环中节点无限递归（A→B→A）

例：A ↔ B（环）
  正确：克隆 A 并放入 map → 克隆 B 放入 map → 回到 A 命中缓存 ✓
  错误：克隆 A → 递归 B → 递归 A → 又克隆 A... 无限循环 ❌
```

---

## 3. 785. 判断二分图：染色法

### 3.1 完整实现（DFS 染色）

```java
/**
 * LeetCode 785. 判断二分图
 * 图能否分成两组，组内无边、组间才有边
 * （等价于：能否用两种颜色染色，相邻节点颜色不同）
 *
 * 染色法：
 *   颜色 0 = 未染色，1 = 红，-1 = 蓝（或 0/1）
 *   DFS 给节点染色，邻居必须染相反色
 *   冲突 → 不是二分图
 */
public boolean isBipartite(int[][] graph) {
    int n = graph.length;
    int[] color = new int[n];              // 0 未染，1/-1 两色

    for (int i = 0; i < n; i++) {
        // 遍历所有连通分量（图可能不连通）
        if (color[i] == 0 && !dfs(graph, i, 1, color)) {
            return false;
        }
    }
    return true;
}

private boolean dfs(int[][] graph, int node, int c, int[] color) {
    color[node] = c;                       // 染色

    for (int next : graph[node]) {
        if (color[next] == 0) {
            // 邻居染相反色，失败则返回 false
            if (!dfs(graph, next, -c, color)) return false;
        } else if (color[next] == c) {
            return false;                  // ⚠️ 邻居同色 → 冲突！
        }
    }
    return true;
}
```

### 3.2 为什么必须遍历所有连通分量？

```
图可能不连通：
  分量 1 是二分图，分量 2 不是
  → 只从一个节点开始检查会漏掉分量 2

for 循环遍历所有未染色节点 → 覆盖全图
（BFS 版同理，也可以用队列染色）

记忆：图题（连通性相关）先问"图连通吗？"
```

---

## 4. 841. 钥匙和房间

### 4.1 完整实现

```java
/**
 * LeetCode 841. 钥匙和房间
 * 从房间 0 出发，房间 i 里有钥匙 rooms[i]（可以打开对应房间）
 * 判断能否进入所有房间
 *
 * 图遍历：房间 = 节点，钥匙 = 边（0 → rooms[i] 中每个房间）
 * DFS 从 0 出发 → 统计可达房间数
 */
public boolean canVisitAllRooms(List<List<Integer>> rooms) {
    int n = rooms.size();
    boolean[] visited = new boolean[n];
    dfs(rooms, 0, visited);

    for (boolean v : visited) {
        if (!v) return false;              // 有房间不可达
    }
    return true;
}

private void dfs(List<List<Integer>> rooms, int room,
                 boolean[] visited) {
    visited[room] = true;                  // 标记已进入

    for (int key : rooms.get(room)) {      // 每把钥匙
        if (!visited[key]) {
            dfs(rooms, key, visited);      // 进入新房间
        }
    }
}
```

### 4.2 图遍历题小结

| 题号 | 题目 | 图模型 | 技巧 |
|:---:|------|------|------|
| 133 | 克隆图 | 节点+邻居 | 哈希映射防环 |
| 785 | 二分图 | 染色 | 两色互斥 |
| 841 | 钥匙房间 | 可达性 | visited 计数 |
| 207 | 课程表 | 判环 | 颜色法/Kahn |

---

## 5. 207. 课程表：颜色法判环

### 5.1 完整实现（DFS 三色法）

```java
/**
 * LeetCode 207. 课程表（DFS 颜色法判环）
 * 颜色：0 = 未访问，1 = 访问中，2 = 已完成
 *
 * 判环：DFS 过程中遇到"访问中"的节点 → 有环！
 *   （访问中 = 当前递归栈中 → 说明能回到自己）
 *
 * ⚠️ 撤销标记 vs 颜色法的区别：
 *   回溯的撤销会丢信息；颜色法区分
 *   "访问中"（本路径上）和"已完成"（其他路径）
 */
public boolean canFinish(int numCourses, int[][] prerequisites) {
    // 建图
    List<List<Integer>> graph = new ArrayList<>();
    for (int i = 0; i < numCourses; i++) graph.add(new ArrayList<>());
    for (int[] pre : prerequisites) {
        graph.get(pre[1]).add(pre[0]);    // pre[1] → pre[0]
    }

    int[] color = new int[numCourses];     // 0 未访问 1 访问中 2 完成

    for (int i = 0; i < numCourses; i++) {
        if (color[i] == 0 && hasCycle(graph, i, color)) {
            return false;                  // 有环 → 无法完成
        }
    }
    return true;
}

private boolean hasCycle(List<List<Integer>> graph, int node,
                         int[] color) {
    color[node] = 1;                       // 访问中

    for (int next : graph.get(node)) {
        if (color[next] == 1) return true;     // ⚠️ 遇到访问中 → 环！
        if (color[next] == 0 && hasCycle(graph, next, color)) {
            return true;
        }
    }

    color[node] = 2;                       // 已完成
    return false;
}
```

### 5.2 颜色法 vs 简单 visited

```
简单 visited 为什么不能判环？
  访问过 → 跳过 → 无法区分"本路径"和"其他路径"

颜色法三态：
  0 未访问 → 进入递归
  1 访问中 → 当前递归栈上 → 有环！(回到自己)
  2 已完成 → 其他路径已处理 → 跳过

类比：DFS 里"访问中"就是当前调用栈中的节点
  → 遇到它说明存在环
```

### 5.3 207 两种解法对比

| 维度 | Kahn（BFS） | 颜色法（DFS） |
|------|:---:|:---:|
| 思路 | 入度 0 队列 | 递归栈判环 |
| 代码 | 固定模板 | 颜色数组 |
| 面试推荐 | 更易手写 | 展示 DFS 深度 |
| 复杂度 | O(V+E) | O(V+E) |

```
面试完整展示：
"课程表有两种解法——Kahn 算法（BFS）统计入度判环，
 以及 DFS 颜色法：0 未访问、1 访问中、2 已完成，
 遇到访问中的节点说明有环。我写 Kahn 版（更易手写）。"
```

> 🎯 **核心要点**：图 DFS 四大应用——**克隆**（哈希映射 + 先 put 防环）、**染色**（两色互斥判二分）、**可达性**（visited 计数）、**判环**（三色法：遇到访问中即环）。图题共同点：**必须 visited/color**（图有环、有共享节点）、**遍历所有连通分量**（图可能不连通）。207 的两种解法（Kahn + 颜色法）都要能讲。

---

**下一模块**：[04-记忆化DFS](04-记忆化DFS.md) | **返回总览**：[00-DFS知识体系总览](00-DFS知识体系总览.md)
