package DataStructure_Java;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class ListGraph {
    private ArrayList<ArrayList<Integer>> adj;
    private int vertexNum;
    private boolean[] visited;

    public ListGraph(int n) {
        vertexNum = n;
        adj = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            adj.add(new ArrayList<>());
        }
        visited = new boolean[n];
    }

    // 添加无向边
    public void addEdge(int a, int b) {
        adj.get(a).add(b);
        adj.get(b).add(a);
    }

    // DFS
    public void dfs(int v) {
        visited[v] = true;
        System.out.print(v + " ");
        for (int next : adj.get(v)) {
            if (!visited[next]) {
                dfs(next);
            }
        }
    }

    // BFS
    public void bfs(int v) {
        boolean[] vis = new boolean[vertexNum];
        Queue<Integer> queue = new LinkedList<>();
        queue.offer(v);
        vis[v] = true;
        while (!queue.isEmpty()) {
            int cur = queue.poll();
            System.out.print(cur + " ");
            for (int next : adj.get(cur)) {
                if (!vis[next]) {
                    vis[next] = true;
                    queue.offer(next);
                }
            }
        }
    }

    public static void main(String[] args) {
        ListGraph g = new ListGraph(5);
        g.addEdge(0,1);
        g.addEdge(0,2);
        g.addEdge(1,3);
        g.addEdge(2,4);

        System.out.println("\n\n邻接表 DFS：");
        g.dfs(0);
        System.out.println("\n邻接表 BFS：");
        g.bfs(0);
    }
}