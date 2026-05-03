package LeetCode;

import java.util.LinkedList;
import java.util.Queue;

public class SolutionupdateMatrix {
    public int[][] updateMatrix(int[][] mat) {
        int m = mat.length;
        int n = mat[0].length;
        int[][] res = new int[m][n];
        boolean[][] visited = new boolean[m][n];
        Queue<int[]> queue = new LinkedList<>();

        // 把所有 0 入队并标记访问
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (mat[i][j] == 0) {
                    queue.offer(new int[]{i, j});
                    visited[i][j] = true;
                }
            }
        }

        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int x = cur[0];
            int y = cur[1];

            for (int i = 0; i < 4; i++) {
                int nx = x + dx[i];
                int ny = y + dy[i];
                if (nx >= 0 && nx < m && ny >= 0 && ny < n && !visited[nx][ny]) {
                    res[nx][ny] = res[x][y] + 1;
                    visited[nx][ny] = true;
                    queue.offer(new int[]{nx, ny});
                }
            }
        }
        return res;
    }
}

// 去掉 public，同一个文件只能有一个 public 类
class TestMain {
    public static void main(String[] args) {
        SolutionupdateMatrix s = new SolutionupdateMatrix();

        int[][] mat1 = {
            {0,0,0},
            {0,1,0},
            {0,0,0}
        };
        int[][] res1 = s.updateMatrix(mat1);
        printMatrix(res1);

        System.out.println("==========");

        int[][] mat2 = {
            {0,0,0},
            {0,1,0},
            {1,1,1}
        };
        int[][] res2 = s.updateMatrix(mat2);
        printMatrix(res2);
    }

    public static void printMatrix(int[][] matrix) {
        for (int[] row : matrix) {
            for (int num : row) {
                System.out.print(num + " ");
            }
            System.out.println();
        }
    }
}