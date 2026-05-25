数据结构与算法分析：不相交集类（Java后端开发视角深度剖析）
一、不相交集类核心概念（后端开发必备）
1. 定义与本质
    不相交集（Disjoint Set Union，DSU），又称并查集，是一种用于管理动态连通性的数据结构
    核心操作：查找（Find）、合并（Union），用于判断元素是否属于同一集合、合并两个集合
2. 核心应用场景（Java后端高频）
    - 网络连通性判断（如服务器集群、分布式节点连通）
    - 图论问题（无向图连通分量、最小生成树Kruskal算法）
    - 后端业务场景（好友关系、权限分组、区域合并）
    - 大数据去重、集合划分
    二、并查集基础实现（原理+Java代码）
    1. 基础结构设计
    - parent数组：parent[i]表示元素i的父节点，根节点parent[i]=i
    - 核心操作：find（找根节点）、union（合并两个集合）
    2. 基础实现（无优化）
    java
    /**
     * 基础并查集（无优化）
     * 时间复杂度：find/union 最坏O(n)
     * 空间复杂度：O(n)
     */
    public class DSU {
    private int[] parent;
    /**
     * 初始化：每个元素父节点为自身
     */
    public DSU(int n) {
        parent = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }
    }
    /**
     * 查找根节点（无路径压缩）
     */
    public int find(int x) {
        // 递归查找根节点
        if (parent[x] != x) {
            return find(parent[x]);
        }
        return x;
    }
    /**
     * 合并两个集合（无按秩合并）
     */
    public void union(int x, int y) {
        int rootX = find(x);
        int rootY = find(y);
        if (rootX != rootY) {
            parent[rootY] = rootX;
        }
    }
    /**
     * 判断是否连通
     */
    public boolean isConnected(int x, int y) {
        return find(x) == find(y);
    }
    }
 
3. 路径压缩优化（核心优化1）
    java
    /**
     * 并查集（路径压缩优化）
     * 时间复杂度：find 接近O(1)
     * 空间复杂度：O(n)
     */
    public class DSUWithPathCompression {
    private int[] parent;
    public DSUWithPathCompression(int n) {
        parent = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }
    }
    /**
     * 路径压缩：查找时直接将节点指向根节点
     */
    public int find(int x) {
        if (parent[x] != x) {
            parent[x] = find(parent[x]);
        }
        return parent[x];
    }
    public void union(int x, int y) {
        int rootX = find(x);
        int rootY = find(y);
        if (rootX != rootY) {
            parent[rootY] = rootX;
        }
    }
    public boolean isConnected(int x, int y) {
        return find(x) == find(y);
    }
    }
 
4. 按秩合并优化（核心优化2）
    java
    /**
     * 并查集（路径压缩+按秩合并，最优实现）
     * 时间复杂度：find/union 均摊O(α(n))，α为阿克曼函数反函数，接近O(1)
     * 空间复杂度：O(n)
     */
    public class OptimizedDSU {
    private int[] parent;
    // 秩：树的高度（或节点数），用于控制合并方向
    private int[] rank;
    public OptimizedDSU(int n) {
        parent = new int[n];
        rank = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
            rank[i] = 1;
        }
    }
    /**
     * 路径压缩查找
     */
    public int find(int x) {
        if (parent[x] != x) {
            parent[x] = find(parent[x]);
        }
        return parent[x];
    }
    /**
     * 按秩合并：将矮树合并到高树，避免树退化
     */
    public void union(int x, int y) {
        int rootX = find(x);
        int rootY = find(y);
        if (rootX == rootY) {
            return;
        }
        // 按秩合并：秩小的根指向秩大的根
        if (rank[rootX] > rank[rootY]) {
            parent[rootY] = rootX;
        } else if (rank[rootX] < rank[rootY]) {
            parent[rootX] = rootY;
        } else {
            // 秩相同，合并后秩+1
            parent[rootY] = rootX;
            rank[rootX]++;
        }
    }
    public boolean isConnected(int x, int y) {
        return find(x) == find(y);
    }
    }
 
三、Java后端实战场景（高频应用）
1. 最小生成树（Kruskal算法）
    java
    import java.util.Arrays;
    import java.util.Comparator;
    /**
     * Kruskal算法：基于并查集实现最小生成树
     * 应用：后端网络拓扑优化、分布式链路规划
     */
    public class KruskalMST {
    static class Edge {
        int u, v, weight;
        public Edge(int u, int v, int weight) {
            this.u = u;
            this.v = v;
            this.weight = weight;
        }
    }
    public int kruskal(int n, Edge[] edges) {
        // 按权重升序排序
        Arrays.sort(edges, Comparator.comparingInt(e -> e.weight));
        OptimizedDSU dsu = new OptimizedDSU(n);
        int totalWeight = 0;
        int edgeCount = 0;
        for (Edge edge : edges) {
            if (!dsu.isConnected(edge.u, edge.v)) {
                dsu.union(edge.u, edge.v);
                totalWeight += edge.weight;
                edgeCount++;
                // 生成树边数为n-1时退出
                if (edgeCount == n - 1) {
                    break;
                }
            }
        }
        return totalWeight;
    }
    }
 
2. 朋友圈问题（LeetCode 547）
    java
    /**
     * 朋友圈问题：判断有多少个朋友圈（连通分量）
     * 应用：后端用户关系分组、社群划分
     */
    public class FriendCircle {
    public int findCircleNum(int[][] isConnected) {
        int n = isConnected.length;
        OptimizedDSU dsu = new OptimizedDSU(n);
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (isConnected[i][j] == 1) {
                    dsu.union(i, j);
                }
            }
        }
        // 统计根节点数量（连通分量数）
        int count = 0;
        for (int i = 0; i < n; i++) {
            if (dsu.find(i) == i) {
                count++;
            }
        }
        return count;
    }
    }
 
3. 岛屿数量（LeetCode 200，并查集解法）
    java
    /**
     * 岛屿数量：二维网格中连通的陆地数量
     * 应用：后端地理数据处理、区域统计
     */
    public class NumberOfIslands {
    public int numIslands(char[][] grid) {
        if (grid == null || grid.length == 0) {
            return 0;
        }
        int m = grid.length;
        int n = grid[0].length;
        OptimizedDSU dsu = new OptimizedDSU(m * n);
        int landCount = 0;
        // 方向数组：右、下
        int[][] dirs = {{0, 1}, {1, 0}};
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == '1') {
                    landCount++;
                    // 合并相邻陆地
                    for (int[] dir : dirs) {
                        int x = i + dir[0];
                        int y = j + dir[1];
                        if (x < m && y < n && grid[x][y] == '1') {
                            int root1 = dsu.find(i * n + j);
                            int root2 = dsu.find(x * n + y);
                            if (root1 != root2) {
                                dsu.union(root1, root2);
                                landCount--;
                            }
                        }
                    }
                }
            }
        }
        return landCount;
    }
    }
 
四、并查集高级特性（后端进阶）
1. 带权并查集（记录节点关系）
    java
    /**
     * 带权并查集：记录节点到根节点的权值
     * 应用：后端权限层级、距离计算、等式方程判断
     */
    public class WeightedDSU {
    private int[] parent;
    private int[] weight; // weight[x]：x到parent[x]的权值
    public WeightedDSU(int n) {
        parent = new int[n];
        weight = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
            weight[i] = 0;
        }
    }
    public int find(int x) {
        if (parent[x] != x) {
            int origParent = parent[x];
            parent[x] = find(parent[x]);
            // 路径压缩时更新权值
            weight[x] += weight[origParent];
        }
        return parent[x];
    }
    /**
     * 合并x和y，满足x到y的权值为w
     */
    public void union(int x, int y, int w) {
        int rootX = find(x);
        int rootY = find(y);
        if (rootX != rootY) {
            parent[rootX] = rootY;
            // 调整权值关系
            weight[rootX] = weight[y] + w - weight[x];
        }
    }
    /**
     * 获取x到根节点的权值
     */
    public int getWeight(int x) {
        find(x);
        return weight[x];
    }
    }
 
2. 可持久化并查集（记录历史状态）
    应用：后端数据回滚、版本控制、分布式事务
    五、Java后端面试高频题
    1. 基础题
    - 并查集的核心操作与优化手段？
    - 路径压缩与按秩合并的原理？
    - 并查集的时间复杂度为什么是均摊O(1)？
    2. 进阶题
    - 带权并查集如何实现？解决什么问题？
    - 并查集与DFS/BFS在连通性问题上的区别？
    - 如何用并查集实现动态连通性（支持增删边）？
3. 实战题
    java
    /**
     * 题目：等式方程的可满足性（LeetCode 990）
     * 场景：后端规则引擎、逻辑判断
     */
    public class EquationsPossible {
    public boolean equationsPossible(String[] equations) {
        OptimizedDSU dsu = new OptimizedDSU(26);
        // 先处理相等关系
        for (String eq : equations) {
            if (eq.charAt(1) == '=') {
                int x = eq.charAt(0) - 'a';
                int y = eq.charAt(3) - 'a';
                dsu.union(x, y);
            }
        }
        // 再处理不等关系
        for (String eq : equations) {
            if (eq.charAt(1) == '!') {
                int x = eq.charAt(0) - 'a';
                int y = eq.charAt(3) - 'a';
                if (dsu.isConnected(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }
    }
 
六、总结（Java后端核心要点）
1. 并查集是解决动态连通性问题的最优数据结构，均摊时间复杂度接近O(1)
2. 核心优化：路径压缩（简化查找）、按秩合并（避免树退化）
3. 后端高频场景：最小生成树、连通分量统计、关系分组、规则判断
4. 高级扩展：带权并查集（处理复杂关系）、可持久化并查集（支持回滚）
5. 面试重点：基础实现、优化原理、Kruskal算法、经典题型（朋友圈、岛屿数量）
    suxiangyu
