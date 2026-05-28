# 各种图论算法的Java代码实现
以下实现涵盖图的邻接表存储（最常用、效率高），并实现图论核心算法：深度优先搜索(DFS)、广度优先搜索(BFS)、迪杰斯特拉(Dijkstra)单源最短路径、弗洛伊德(Floyd-Warshall)多源最短路径、克鲁斯卡尔(Kruskal)最小生成树、普里姆(Prim)最小生成树、拓扑排序，代码均为可直接运行的完整示例，注释详尽，适配新手理解。

## 一、基础准备：图的邻接表存储（通用）
图分为无向图和有向图，带权/无权，邻接表用 List 数组实现，核心是 Edge 边类存储邻接节点和边权（无权图权值设为1）。
```java
import java.util.*;
// 边类：存储邻接节点id和边的权值
class Edge {
    int to; // 邻接节点
    int weight; // 边权，无权图为1
    public Edge(int to, int weight) {
        this.to = to;
        this.weight = weight;
    }
}
// 图的邻接表存储类（通用）
class Graph {
    int n; // 节点总数（节点id从0开始，也可改为1开始，只需调整初始化）
    List<Edge>[] adj; // 邻接表：adj[u]存储u的所有出边
    // 构造器：初始化n个节点的图
    public Graph(int n) {
        this.n = n;
        adj = new List[n];
        for (int i = 0; i < n; i++) {
            adj[i] = new ArrayList<>();
        }
    }
    // 添加有向边：u -> v，权值weight
    public void addDirectedEdge(int u, int v, int weight) {
        adj[u].add(new Edge(v, weight));
    }
    // 添加无向边：u <-> v，权值weight（双向添加有向边）
    public void addUndirectedEdge(int u, int v, int weight) {
        addDirectedEdge(u, v, weight);
        addDirectedEdge(v, u, weight);
    }
    // 获取图的邻接表（供外部算法调用）
    public List<Edge>[] getAdj() {
        return adj;
    }
    // 获取节点总数
    public int getN() {
        return n;
    }
}
 
节点id说明：示例中节点id从 0 开始（编程常用），若需改为 1 开始，只需将数组初始化、循环边界改为 1~n 即可。
```

## 二、深度优先搜索（DFS）- 递归/非递归
核心思想：先深后广，沿一条路径走到头，再回溯探索其他路径。
应用：图的遍历、连通性判断、找路径、拓扑排序（递归版）。

### 2.1 递归版DFS（简洁，适合小规模图，避免栈溢出）
```java
// 图的DFS遍历（递归版）
class DFS {
    // graph：待遍历的图，start：起始节点
    public static void dfs(Graph graph, int start) {
        int n = graph.getN();
        boolean[] visited = new boolean[n]; // 标记节点是否访问过
        dfsRecursion(graph.getAdj(), start, visited);
    }
    // 递归核心：u为当前节点
    private static void dfsRecursion(List<Edge>[] adj, int u, boolean[] visited) {
        visited[u] = true;
        System.out.print(u + " "); // 访问节点，可替换为业务逻辑
        // 遍历u的所有邻接节点
        for (Edge edge : adj[u]) {
            int v = edge.to;
            if (!visited[v]) { // 未访问则递归
                dfsRecursion(adj, v, visited);
            }
        }
    }
}
 
```

### 2.2 非递归版DFS（用栈实现，适合大规模图，无栈溢出）
```java
// 非递归版DFS（栈实现）
class DFSNonRecursive {
    public static void dfs(Graph graph, int start) {
        int n = graph.getN();
        boolean[] visited = new boolean[n];
        Deque<Integer> stack = new LinkedList<>(); // 栈：存储待访问节点
        stack.push(start);
        visited[start] = true;
        while (!stack.isEmpty()) {
            int u = stack.pop();
            System.out.print(u + " "); // 访问节点
            // 逆序入栈（保证遍历顺序与递归版一致，非必须）
            List<Edge> edges = graph.getAdj()[u];
            for (int i = edges.size() - 1; i >= 0; i--) {
                int v = edges.get(i).to;
                if (!visited[v]) {
                    visited[v] = true;
                    stack.push(v);
                }
            }
        }
    }
}
 
```

## 三、广度优先搜索（BFS）- 队列实现
核心思想：先广后深，逐层遍历节点，按距离起始节点由近到远访问。
应用：无权图最短路径、图的层序遍历、连通性判断、找最短步数。
```java
// 图的BFS遍历（队列实现，天然求无权图最短路径）
class BFS {
    // graph：图，start：起始节点
    public static void bfs(Graph graph, int start) {
        int n = graph.getN();
        boolean[] visited = new boolean[n];
        Queue<Integer> queue = new LinkedList<>(); // 队列：存储待访问节点
        queue.offer(start);
        visited[start] = true;
        while (!queue.isEmpty()) {
            int u = queue.poll();
            System.out.print(u + " "); // 访问节点
            // 遍历u的所有邻接节点
            for (Edge edge : graph.getAdj()[u]) {
                int v = edge.to;
                if (!visited[v]) {
                    visited[v] = true;
                    queue.offer(v);
                }
            }
        }
    }
    // 扩展：BFS求无权图中start到所有节点的最短路径长度
    public static int[] bfsShortestPath(Graph graph, int start) {
        int n = graph.getN();
        int[] dist = new int[n]; // dist[i]：start到i的最短距离，-1表示不可达
        Arrays.fill(dist, -1);
        Queue<Integer> queue = new LinkedList<>();
        dist[start] = 0;
        queue.offer(start);
        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (Edge edge : graph.getAdj()[u]) {
                int v = edge.to;
                if (dist[v] == -1) { // 未访问即最短距离
                    dist[v] = dist[u] + 1;
                    queue.offer(v);
                }
            }
        }
        return dist;
    }
}
 
```

## 四、迪杰斯特拉（Dijkstra）算法 - 单源最短路径
核心思想：贪心算法，从起始节点出发，每次选当前距离最近的未访问节点，更新其邻接节点的距离，适用于非负权边的图（有负权用SPFA/Bellman-Ford）。
实现：用优先队列（小根堆） 优化，时间复杂度O(ElogV)（E为边数，V为节点数）。
```java
// 迪杰斯特拉算法：单源最短路径（非负权边），返回start到所有节点的最短距离
class Dijkstra {
    public static int[] dijkstra(Graph graph, int start) {
        int n = graph.getN();
        List<Edge>[] adj = graph.getAdj();
        int[] dist = new int[n]; // 存储start到各节点的最短距离
        Arrays.fill(dist, Integer.MAX_VALUE); // 初始化为无穷大
        dist[start] = 0; // 起始节点到自身距离为0
        // 小根堆：存储(当前距离, 节点)，按距离升序排列
        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
        pq.offer(new int[]{0, start});
        while (!pq.isEmpty()) {
            int[] cur = pq.poll();
            int curDist = cur[0];
            int u = cur[1];
            // 若当前距离大于已记录的最短距离，直接跳过（堆中冗余数据）
            if (curDist > dist[u]) {
                continue;
            }
            // 遍历u的邻接节点，松弛操作
            for (Edge edge : adj[u]) {
                int v = edge.to;
                int weight = edge.weight;
                // 若经过u到v的距离更短，更新并加入堆
                if (dist[v] > dist[u] + weight) {
                    dist[v] = dist[u] + weight;
                    pq.offer(new int[]{dist[v], v});
                }
            }
        }
        return dist;
    }
}
 
说明：结果中 Integer.MAX_VALUE 表示对应节点与起始节点不可达。
```

## 五、弗洛伊德（Floyd-Warshall）算法 - 多源最短路径
核心思想：动态规划，枚举中间节点k，判断经过k的路径i→k→j是否比直接i→j更短，适用于小规模图（时间复杂度O(V^3)），支持负权边（不支持负权环）。
实现：用二维数组存储任意两点间的最短距离，无需依赖邻接表，直接基于邻接矩阵实现。
```java
// 弗洛伊德算法：多源最短路径，支持负权边（无负权环），返回邻接矩阵形式的最短距离
class FloydWarshall {
    // graph：图，INF：无穷大（需大于图中最大可能的路径和）
    public static int[][] floyd(Graph graph, int INF) {
        int n = graph.getN();
        List<Edge>[] adj = graph.getAdj();
        // 初始化邻接矩阵：dist[i][j]表示i到j的初始距离
        int[][] dist = new int[n][n];
        for (int i = 0; i < n; i++) {
            Arrays.fill(dist[i], INF);
            dist[i][i] = 0; // 自身到自身距离为0
            // 填充邻接表的边
            for (Edge edge : adj[i]) {
                int j = edge.to;
                dist[i][j] = edge.weight;
            }
        }
        // 动态规划核心：枚举中间节点k，起点i，终点j
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    // 若i到k、k到j可达，且经过k更短，则更新
                    if (dist[i][k] != INF && dist[k][j] != INF) {
                        dist[i][j] = Math.min(dist[i][j], dist[i][k] + dist[k][j]);
                    }
                }
            }
        }
        return dist;
    }
}
 
```

## 六、克鲁斯卡尔（Kruskal）算法 - 最小生成树（无向图）
核心思想：贪心算法，按边权从小到大排序，依次选边，若边的两个节点不在同一连通分量，则加入生成树，直到选够V-1条边（V为节点数）。
关键：用并查集（Union-Find） 判断连通性，避免环，时间复杂度O(ElogE)（排序占主导），适合稀疏图。

### 6.1 并查集（Union-Find）实现（依赖）
```java
// 并查集：用于Kruskal算法判断连通性
class UnionFind {
    int[] parent; // 父节点
    int[] rank;   // 秩：用于按秩合并，优化效率
    public UnionFind(int n) {
        parent = new int[n];
        rank = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i; // 初始父节点为自身
            rank[i] = 1;   // 初始秩为1
        }
    }
    // 查找根节点（路径压缩）
    public int find(int x) {
        if (parent[x] != x) {
            parent[x] = find(parent[x]);
        }
        return parent[x];
    }
    // 合并两个集合（按秩合并）
    public boolean union(int x, int y) {
        int rootX = find(x);
        int rootY = find(y);
        if (rootX == rootY) {
            return false; // 已在同一集合，合并失败
        }
        // 秩小的合并到秩大的下面
        if (rank[rootX] < rank[rootY]) {
            parent[rootX] = rootY;
        } else if (rank[rootX] > rank[rootY]) {
            parent[rootY] = rootX;
        } else {
            parent[rootY] = rootX;
            rank[rootX]++;
        }
        return true; // 合并成功
    }
}
 
```

### 6.2 克鲁斯卡尔算法实现
```java
// 克鲁斯卡尔算法：无向图的最小生成树（MST），返回MST的总权值，-1表示无MST（图不连通）
class Kruskal {
    // 辅助类：存储边（u, v, weight），用于排序
    static class EdgeNode implements Comparable<EdgeNode> {
        int u, v, weight;
        public EdgeNode(int u, int v, int weight) {
            this.u = u;
            this.v = v;
            this.weight = weight;
        }
        // 按边权升序排序
        @Override
        public int compareTo(EdgeNode o) {
            return this.weight - o.weight;
        }
    }
    public static int kruskal(Graph graph) {
        int n = graph.getN();
        List<Edge>[] adj = graph.getAdj();
        List<EdgeNode> edges = new ArrayList<>();
        UnionFind uf = new UnionFind(n);
        // 提取无向图的所有边（去重：只存u<v的边，避免重复处理）
        for (int u = 0; u < n; u++) {
            for (Edge edge : adj[u]) {
                int v = edge.to;
                int w = edge.weight;
                if (u < v) { // 无向图去重关键
                    edges.add(new EdgeNode(u, v, w));
                }
            }
        }
        // 按边权升序排序
        Collections.sort(edges);
        int mstWeight = 0; // MST总权值
        int edgeCount = 0; // 已选边数
        // 依次选边
        for (EdgeNode e : edges) {
            if (uf.union(e.u, e.v)) { // 无环则加入
                mstWeight += e.weight;
                edgeCount++;
                if (edgeCount == n - 1) { // 选够V-1条边，MST完成
                    break;
                }
            }
        }
        // 若边数不足V-1，图不连通，无MST
        return edgeCount == n - 1 ? mstWeight : -1;
    }
}
 
```

## 七、普里姆（Prim）算法 - 最小生成树（无向图）
核心思想：贪心算法，从起始节点出发，维护一个已选节点集合，每次选连接已选集合和未选集合的权值最小的边，将对应节点加入集合，直到所有节点加入，时间复杂度O(V^2)（邻接矩阵）/ O(ElogV)（优先队列），适合稠密图。
```java
// 普里姆算法：无向图的最小生成树（MST），从start节点开始，返回总权值，-1表示无MST
class Prim {
    public static int prim(Graph graph, int start) {
        int n = graph.getN();
        List<Edge>[] adj = graph.getAdj();
        int[] dist = new int[n]; // dist[i]：已选集合到节点i的最小边权
        boolean[] inMST = new boolean[n]; // 标记节点是否在MST中
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[start] = 0; // 起始节点到已选集合的距离为0
        int mstWeight = 0; // MST总权值
        int nodeCount = 0; // 已加入MST的节点数
        // 共选n个节点
        for (int i = 0; i < n; i++) {
            // 步骤1：找未加入MST且dist最小的节点u
            int u = -1;
            int minDist = Integer.MAX_VALUE;
            for (int j = 0; j < n; j++) {
                if (!inMST[j] && dist[j] < minDist) {
                    minDist = dist[j];
                    u = j;
                }
            }
            // 无可达节点，图不连通
            if (u == -1) {
                return -1;
            }
            // 步骤2：将u加入MST，累加权值
            inMST[u] = true;
            mstWeight += minDist;
            nodeCount++;
            // 步骤3：松弛操作，更新未加入节点的dist
            for (Edge edge : adj[u]) {
                int v = edge.to;
                int weight = edge.weight;
                if (!inMST[v] && weight < dist[v]) {
                    dist[v] = weight;
                }
            }
        }
        return nodeCount == n ? mstWeight : -1;
    }
}
 
```

## 八、拓扑排序 - 有向无环图（DAG）
核心思想：对有向无环图的节点排序，使得所有有向边u→v的节点u都在v之前，应用：任务调度、课程安排、依赖解析，实现用入度表+队列（Kahn算法），时间复杂度O(V+E)。
```java
// 拓扑排序（Kahn算法：入度表+队列），适用于有向无环图（DAG），返回拓扑序列，空列表表示有环
class TopoSort {
    public static List<Integer> topoSort(Graph graph) {
        int n = graph.getN();
        List<Edge>[] adj = graph.getAdj();
        int[] inDegree = new int[n]; // 入度表：inDegree[i]为节点i的入度
        Queue<Integer> queue = new LinkedList<>(); // 存储入度为0的节点
        List<Integer> topoSeq = new ArrayList<>(); // 拓扑序列
        // 步骤1：统计每个节点的入度
        for (int u = 0; u < n; u++) {
            for (Edge edge : adj[u]) {
                int v = edge.to;
                inDegree[v]++;
            }
        }
        // 步骤2：将入度为0的节点加入队列
        for (int i = 0; i < n; i++) {
            if (inDegree[i] == 0) {
                queue.offer(i);
            }
        }
        // 步骤3：处理队列中的节点
        while (!queue.isEmpty()) {
            int u = queue.poll();
            topoSeq.add(u);
            // 遍历u的邻接节点，入度减1
            for (Edge edge : adj[u]) {
                int v = edge.to;
                inDegree[v]--;
                if (inDegree[v] == 0) { // 入度为0则加入队列
                    queue.offer(v);
                }
            }
        }
        // 若拓扑序列长度不等于节点数，说明有环，返回空列表
        return topoSeq.size() == n ? topoSeq : new ArrayList<>();
    }
}
 
```

## 九、测试主类（所有算法一键运行）
```java
// 测试主类：验证所有图论算法
public class GraphAlgorithmTest {
    public static void main(String[] args) {
        // ========== 1. 构建测试图 ==========
        int n = 6; // 6个节点：0,1,2,3,4,5
        Graph graph = new Graph(n);
        // 无向带权图（用于MST算法）
        graph.addUndirectedEdge(0, 1, 2);
        graph.addUndirectedEdge(0, 2, 1);
        graph.addUndirectedEdge(1, 2, 3);
        graph.addUndirectedEdge(1, 3, 4);
        graph.addUndirectedEdge(2, 3, 5);
        graph.addUndirectedEdge(3, 4, 1);
        graph.addUndirectedEdge(3, 5, 2);
        graph.addUndirectedEdge(4, 5, 3);
        // 单独构建有向图（用于拓扑排序）
        Graph dag = new Graph(4);
        dag.addDirectedEdge(0, 1, 1);
        dag.addDirectedEdge(0, 2, 1);
        dag.addDirectedEdge(1, 3, 1);
        dag.addDirectedEdge(2, 3, 1);
        // ========== 2. 测试DFS ==========
        System.out.println("递归版DFS（从0开始）：");
        DFS.dfs(graph, 0);
        System.out.println("\n非递归版DFS（从0开始）：");
        DFSNonRecursive.dfs(graph, 0);
        // ========== 3. 测试BFS ==========
        System.out.println("\n\nBFS（从0开始）：");
        BFS.bfs(graph, 0);
        int[] bfsDist = BFS.bfsShortestPath(graph, 0);
        System.out.println("\n无权图最短路径（0到各节点）：" + Arrays.toString(bfsDist));
        // ========== 4. 测试Dijkstra ==========
        int[] dijkstraDist = Dijkstra.dijkstra(graph, 0);
        System.out.println("\n迪杰斯特拉最短路径（0到各节点）：" + Arrays.toString(dijkstraDist));
        // ========== 5. 测试Floyd-Warshall ==========
        int INF = Integer.MAX_VALUE / 2; // 避免加法溢出
        int[][] floydDist = FloydWarshall.floyd(graph, INF);
        System.out.println("\n弗洛伊德多源最短路径（邻接矩阵）：");
        for (int[] row : floydDist) {
            System.out.println(Arrays.toString(row));
        }
        // ========== 6. 测试Kruskal ==========
        int kruskalWeight = Kruskal.kruskal(graph);
        System.out.println("\n克鲁斯卡尔MST总权值：" + kruskalWeight);
        // ========== 7. 测试Prim ==========
        int primWeight = Prim.prim(graph, 0);
        System.out.println("普里姆MST总权值（从0开始）：" + primWeight);
        // ========== 8. 测试拓扑排序 ==========
        List<Integer> topoSeq = TopoSort.topoSort(dag);
        System.out.println("\n拓扑排序序列（DAG）：" + topoSeq);
        // 测试有环图（添加环：3->0）
        dag.addDirectedEdge(3, 0, 1);
        List<Integer> cycleSeq = TopoSort.topoSort(dag);
        System.out.println("有环图的拓扑序列（空表示有环）：" + cycleSeq);
    }
}
 
```

## 十、算法适用场景总结
算法 核心用途 适用场景 时间复杂度（优化版） 
DFS 图遍历、连通性、找路径 小规模图、递归友好场景   
BFS 无权图最短路径、层序遍历 所有图   
Dijkstra 单源最短路径 非负权边的图、稀疏图   
Floyd-Warshall 多源最短路径 小规模图、支持负权边（无环）   
Kruskal 最小生成树（无向图） 稀疏图   
Prim 最小生成树（无向图） 稠密图   
拓扑排序 有向无环图节点排序 任务调度、依赖解析   

## 十一、关键注意事项
1. 节点id：示例中为 0 开始，若需 1 开始，只需将所有数组初始化、循环边界改为 1~n ；
2. 权值处理：无权图将边权设为 1 即可；
3. 负权边：Dijkstra不支持，用Bellman-Ford/SPFA；弗洛伊德支持但不能有负权环；
4. 连通性：MST算法仅适用于连通的无向图，非连通图返回 -1 ；
5. 拓扑排序：仅适用于有向无环图（DAG），有环则返回空序列。
