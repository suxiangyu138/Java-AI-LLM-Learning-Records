package com.leetcode.solution.graph;

public class BipartiteSolution {
    // 染色数组 0=未染色 1=颜色A -1=颜色B
    private int[] color;

    public boolean isBipartite(int[][] graph) {
        int n = graph.length;
        color = new int[n];

        // 遍历所有节点（图可能不连通）
        for (int i = 0; i < n; i++) {
            if (color[i] == 0) {
                if (!dfs(graph, i, 1)) {
                    return false;
                }
            }
        }
        return true;
    }

    // DFS：给节点 u 染成颜色 c
    private boolean dfs(int[][] graph, int u, int c) {
        color[u] = c;

        for (int v : graph[u]) {
            if (color[v] == 0) {
                if (!dfs(graph, v, -c)) {
                    return false;
                }
            } else if (color[v] == c) {
                return false;
            }
        }
        return true;
    }
}

// 独立主类，不放在内部类里
class TestMainBipartite {
    public static void main(String[] args) {
        BipartiteSolution s = new BipartiteSolution();

        int[][] g1 = {{1,2,3},{0,2},{0,1,3},{0,2}};
        System.out.println(s.isBipartite(g1)); // false

        int[][] g2 = {{1,3},{0,2},{1,3},{0,2}};
        System.out.println(s.isBipartite(g2)); // true
    }
}
