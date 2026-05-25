数据结构与算法分析：图论算法（Java后端开发视角深度剖析）
一、图论核心概念（后端开发必备）
1. 图的定义与分类
    图由顶点集合V和边集合E组成，记为G=(V,E)
    分类：
    - 有向图/无向图
    - 有权图/无权图
    - 连通图/非连通图
    - 稀疏图/稠密图
2. 图的存储结构（Java后端常用）
    - 邻接矩阵：二维数组，适合稠密图，查询O(1)，空间O(V²)
    - 邻接表：数组+链表/列表，适合稀疏图，空间O(V+E)，遍历高效
    二、图的遍历算法（基础核心）
    1. 深度优先遍历（DFS）
    核心原理
    递归/栈实现，优先访问深度路径，回溯探索所有分支
    Java实现（邻接表）
    java
    import java.util.List;
    import java.util.ArrayList;
    /**
     * 深度优先遍历
     * 时间复杂度：O(V+E)
     * 空间复杂度：O(V)（递归栈+访问标记）
     */
    public class DFS {
    private List<List<Integer>> adj;
    private boolean[] visited;
    public DFS(int n) {
        adj = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            adj.add(new ArrayList<>());
        }
        visited = new boolean[n];
    }
    public void addEdge(int u, int v) {
        adj.get(u).add(v);
        adj.get(v).add(u);
    }
    public void dfs(int start) {
        visited[start] = true;
        for (int neighbor : adj.get(start)) {
            if (!visited[neighbor]) {
                dfs(neighbor);
            }
        }
    }
    }
 
后端应用
- 连通分量检测、拓扑排序、路径查找、回溯算法
    2. 广度优先遍历（BFS）
    核心原理
    队列实现，按层遍历，适合最短路径（无权图）
    Java实现（邻接表）
    java
    import java.util.List;
    import java.util.ArrayList;
    import java.util.LinkedList;
    import java.util.Queue;
    /**
 * 广度优先遍历
 * 时间复杂度：O(V+E)
 * 空间复杂度：O(V)（队列+访问标记）
     */
    public class BFS {
    private List<List<Integer>> adj;
    private boolean[] visited;
    public BFS(int n) {
        adj = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            adj.add(new ArrayList<>());
        }
        visited = new boolean[n];
    }
    public void addEdge(int u, int v) {
        adj.get(u).add(v);
        adj.get(v).add(u);
    }
    public void bfs(int start) {
        Queue<Integer> queue = new LinkedList<>();
        queue.offer(start);
        visited[start] = true;
        while (!queue.isEmpty()) {
            int curr = queue.poll();
            for (int neighbor : adj.get(curr)) {
                if (!visited[neighbor]) {
                    visited[neighbor] = true;
                    queue.offer(neighbor);
                }
            }
        }
    }
    }
 
后端应用
- 无权图最短路径、层级遍历、社交网络距离计算
    三、最短路径算法（后端高频）
    1. Dijkstra算法（单源最短路径，非负权）
    核心原理
    贪心思想，每次选距离源点最近节点，松弛相邻边
    Java实现（邻接表+优先队列）
    java
    import java.util.List;
    import java.util.ArrayList;
    import java.util.PriorityQueue;
    import java.util.Arrays;
    /**
 * Dijkstra算法（优先队列优化）
 * 时间复杂度：O(ElogV)
 * 空间复杂度：O(V+E)
     */
    public class Dijkstra {
    static class Edge {
        int to;
        int weight;
        public Edge(int to, int weight) {
            this.to = to;
            this.weight = weight;
        }
    }
    public int[] dijkstra(int n, List<List<Edge>> adj, int start) {
        int[] dist = new int[n];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[start] = 0;
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[1] - b[1]);
        pq.offer(new int[]{start, 0});
        while (!pq.isEmpty()) {
            int[] curr = pq.poll();
            int u = curr[0];
            int d = curr[1];
            if (d > dist[u]) continue;
            for (Edge edge : adj.get(u)) {
                int v = edge.to;
                int w = edge.weight;
                if (dist[v] > dist[u] + w) {
                    dist[v] = dist[u] + w;
                    pq.offer(new int[]{v, dist[v]});
                }
            }
        }
        return dist;
    }
    }
 
后端应用
- 网络路由、地图导航、分布式链路最短路径
    2. Bellman-Ford算法（单源最短路径，含负权）
    核心原理
    动态规划，松弛V-1轮，第V轮检测负权环
    Java实现
    java
    import java.util.List;
    import java.util.ArrayList;
    import java.util.Arrays;
    /**
 * Bellman-Ford算法
 * 时间复杂度：O(VE)
 * 空间复杂度：O(V)
     */
    public class BellmanFord {
    static class Edge {
        int from;
        int to;
        int weight;
        public Edge(int from, int to, int weight) {
            this.from = from;
            this.to = to;
            this.weight = weight;
        }
    }
    public int[] bellmanFord(int n, List<Edge> edges, int start) {
        int[] dist = new int[n];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[start] = 0;
        for (int i = 0; i < n - 1; i++) {
            boolean updated = false;
            for (Edge edge : edges) {
                int u = edge.from;
                int v = edge.to;
                int w = edge.weight;
                if (dist[u] != Integer.MAX_VALUE && dist[v] > dist[u] + w) {
                    dist[v] = dist[u] + w;
                    updated = true;
                }
            }
            if (!updated) break;
        }
        for (Edge edge : edges) {
            int u = edge.from;
            int v = edge.to;
            int w = edge.weight;
            if (dist[u] != Integer.MAX_VALUE && dist[v] > dist[u] + w) {
                return null;
            }
        }
        return dist;
    }
    }
 
后端应用
- 含负权边路径、负权环检测（金融风控、交易套利）
    3. Floyd-Warshall算法（多源最短路径）
    核心原理
    动态规划，k为中间节点，更新i→j最短路径
    Java实现
    java
    import java.util.Arrays;
    /**
 * Floyd-Warshall算法
 * 时间复杂度：O(V³)
 * 空间复杂度：O(V²)
     */
    public class FloydWarshall {
    public int[][] floydWarshall(int n, int[][] graph) {
        int[][] dist = new int[n][n];
        for (int i = 0; i < n; i++) {
            dist[i] = Arrays.copyOf(graph[i], n);
        }
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (dist[i][k] != Integer.MAX_VALUE && dist[k][j] != Integer.MAX_VALUE) {
                        dist[i][j] = Math.min(dist[i][j], dist[i][k] + dist[k][j]);
                    }
                }
            }
        }
        return dist;
    }
    }
 
后端应用
- 全节点路径计算、小规模图全局路径规划
    四、最小生成树（MST）算法（后端高频）
    1. Kruskal算法（并查集+排序）
    核心原理
    按边权升序，用并查集避免环，选V-1条边
    Java实现
    java
    import java.util.List;
    import java.util.ArrayList;
    import java.util.Collections;
    /**
 * Kruskal算法
 * 时间复杂度：O(ElogE)
 * 空间复杂度：O(V+E)
     */
    public class Kruskal {
    static class Edge implements Comparable<Edge> {
        int from;
        int to;
        int weight;
        public Edge(int from, int to, int weight) {
            this.from = from;
            this.to = to;
            this.weight = weight;
        }
        @Override
        public int compareTo(Edge o) {
            return this.weight - o.weight;
        }
    }
    private int[] parent;
    public Kruskal(int n) {
        parent = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }
    }
    private int find(int x) {
        if (parent[x] != x) {
            parent[x] = find(parent[x]);
        }
        return parent[x];
    }
    private boolean union(int x, int y) {
        int fx = find(x);
        int fy = find(y);
        if (fx == fy) return false;
        parent[fy] = fx;
        return true;
    }
    public int kruskal(int n, List<Edge> edges) {
        Collections.sort(edges);
        int res = 0;
        int count = 0;
        for (Edge edge : edges) {
            if (union(edge.from, edge.to)) {
                res += edge.weight;
                count++;
                if (count == n - 1) break;
            }
        }
        return res;
    }
    }
 
后端应用
- 网络布线、集群拓扑、资源分配
    2. Prim算法（贪心+优先队列）
    核心原理
    选起点，每次加最小权边连接新节点
    Java实现
    java
    import java.util.List;
    import java.util.ArrayList;
    import java.util.PriorityQueue;
    import java.util.Arrays;
    /**
 * Prim算法（优先队列优化）
 * 时间复杂度：O(ElogV)
 * 空间复杂度：O(V+E)
     */
    public class Prim {
    static class Edge {
        int to;
        int weight;
        public Edge(int to, int weight) {
            this.to = to;
            this.weight = weight;
        }
    }
    public int prim(int n, List<List<Edge>> adj) {
        boolean[] visited = new boolean[n];
        int[] dist = new int[n];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[0] = 0;
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[1] - b[1]);
        pq.offer(new int[]{0, 0});
        int res = 0;
        int count = 0;
        while (!pq.isEmpty() && count < n) {
            int[] curr = pq.poll();
            int u = curr[0];
            int w = curr[1];
            if (visited[u]) continue;
            visited[u] = true;
            res += w;
            count++;
            for (Edge edge : adj.get(u)) {
                int v = edge.to;
                int weight = edge.weight;
                if (!visited[v] && dist[v] > weight) {
                    dist[v] = weight;
                    pq.offer(new int[]{v, dist[v]});
                }
            }
        }
        return res;
    }
    }
 
后端应用
- 稠密图最小生成树、实时网络构建
    五、拓扑排序（有向无环图）
    核心原理
    Kahn算法（入度+队列）/DFS，输出顶点线性序列
    Java实现（Kahn算法）
    java
    import java.util.List;
    import java.util.ArrayList;
    import java.util.LinkedList;
    import java.util.Queue;
    /**
 * 拓扑排序（Kahn算法）
 * 时间复杂度：O(V+E)
 * 空间复杂度：O(V+E)
     */
    public class TopologicalSort {
    public List<Integer> topologicalSort(int n, List<List<Integer>> adj) {
        int[] inDegree = new int[n];
        for (int i = 0; i < n; i++) {
            for (int neighbor : adj.get(i)) {
                inDegree[neighbor]++;
            }
        }
        Queue<Integer> queue = new LinkedList<>();
        for (int i = 0; i < n; i++) {
            if (inDegree[i] == 0) {
                queue.offer(i);
            }
        }
        List<Integer> res = new ArrayList<>();
        while (!queue.isEmpty()) {
            int curr = queue.poll();
            res.add(curr);
            for (int neighbor : adj.get(curr)) {
                inDegree[neighbor]--;
                if (inDegree[neighbor] == 0) {
                    queue.offer(neighbor);
                }
            }
        }
        return res.size() == n ? res : new ArrayList<>();
    }
    }
 
后端应用
- 任务调度、依赖解析、编译顺序、课程安排
    六、强连通分量（SCC）算法
    1. Tarjan算法
    核心原理
    DFS+栈，记录dfn/low值，识别强连通分量
    Java实现
    java
    import java.util.List;
    import java.util.ArrayList;
    import java.util.Stack;
    /**
 * Tarjan算法（强连通分量）
 * 时间复杂度：O(V+E)
 * 空间复杂度：O(V)
     */
    public class Tarjan {
    private List<List<Integer>> adj;
    private int[] dfn;
    private int[] low;
    private boolean[] inStack;
    private Stack<Integer> stack;
    private int time;
    private List<List<Integer>> sccList;
    public Tarjan(int n) {
        adj = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            adj.add(new ArrayList<>());
        }
        dfn = new int[n];
        low = new int[n];
        inStack = new boolean[n];
        stack = new Stack<>();
        time = 0;
        sccList = new ArrayList<>();
    }
    public void addEdge(int u, int v) {
        adj.get(u).add(v);
    }
    public List<List<Integer>> tarjan(int n) {
        for (int i = 0; i < n; i++) {
            if (dfn[i] == 0) {
                dfs(i);
            }
        }
        return sccList;
    }
    private void dfs(int u) {
        dfn[u] = low[u] = ++time;
        stack.push(u);
        inStack[u] = true;
        for (int v : adj.get(u)) {
            if (dfn[v] == 0) {
                dfs(v);
                low[u] = Math.min(low[u], low[v]);
            } else if (inStack[v]) {
                low[u] = Math.min(low[u], dfn[v]);
            }
        }
        if (dfn[u] == low[u]) {
            List<Integer> scc = new ArrayList<>();
            int v;
            do {
                v = stack.pop();
                inStack[v] = false;
                scc.add(v);
            } while (v != u);
            sccList.add(scc);
        }
    }
    }
 
后端应用
- 有向图环检测、社区发现、推荐系统
    七、Java后端面试高频题
    1. 基础题
- 邻接矩阵与邻接表的区别与适用场景？
- DFS与BFS的原理、时间复杂度、应用场景？
- Dijkstra与Bellman-Ford的区别？
    2. 进阶题
- 如何判断有向图是否存在环？
- 最小生成树两种算法的适用场景？
- 拓扑排序的应用场景与实现？
    3. 实战题
    java
    /**
 * 题目：课程表（LeetCode 207）
 * 场景：后端依赖调度、任务编排
     */
    public class CourseSchedule {
    public boolean canFinish(int numCourses, int[][] prerequisites) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < numCourses; i++) {
            adj.add(new ArrayList<>());
        }
        int[] inDegree = new int[numCourses];
        for (int[] pre : prerequisites) {
            adj.get(pre[1]).add(pre[0]);
            inDegree[pre[0]]++;
        }
        Queue<Integer> queue = new LinkedList<>();
        for (int i = 0; i < numCourses; i++) {
            if (inDegree[i] == 0) {
                queue.offer(i);
            }
        }
        int count = 0;
        while (!queue.isEmpty()) {
            int curr = queue.poll();
            count++;
            for (int neighbor : adj.get(curr)) {
                inDegree[neighbor]--;
                if (inDegree[neighbor] == 0) {
                    queue.offer(neighbor);
                }
            }
        }
        return count == numCourses;
    }
    }
 
八、总结（Java后端核心要点）
1. 图存储：稀疏图用邻接表，稠密图用邻接矩阵
2. 遍历：DFS（深度探索）、BFS（最短路径），时间O(V+E)
3. 最短路径：Dijkstra（非负权）、Bellman-Ford（负权）、Floyd（多源）
4. 最小生成树：Kruskal（稀疏图）、Prim（稠密图）
5. 拓扑排序：Kahn算法，用于依赖调度
6. 强连通分量：Tarjan算法，用于环检测、社区发现
7. 后端高频场景：网络路由、任务调度、依赖解析、社交网络、推荐系统
    suxiangyu
