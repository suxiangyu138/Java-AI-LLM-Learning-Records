import java.util.Scanner;

public class MatrixAdjugate {

    /**
     * 计算矩阵的伴随矩阵
     * @param matrix 待计算的方阵
     * @return 原矩阵的伴随矩阵
     * @throws IllegalArgumentException 非方阵时抛出异常
     */
    public static int[][] calculateAdjugateMatrix(int[][] matrix) {
        // 检查矩阵是否为null
        if (matrix == null) {
            throw new IllegalArgumentException("矩阵不能为null");
        }

        // 验证是否为方阵
        int n = matrix.length;
        for (int[] row : matrix) {
            if (row == null || row.length != n) {
                throw new IllegalArgumentException("只有方阵（行数=列数）才能计算伴随矩阵！");
            }
        }

        // 1阶矩阵的伴随矩阵是[[1]]
        if (n == 1) {
            return new int[][]{{1}};
        }

        // 步骤1：计算所有元素的代数余子式，生成余子式矩阵
        int[][] cofactorMatrix = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                // 1. 获取去掉第i行第j列的子矩阵
                int[][] subMatrix = getSubMatrix(matrix, i, j);
                // 2. 计算子矩阵的行列式
                int subDet = calculateDeterminant(subMatrix);
                // 3. 计算代数余子式：(-1)^(i+j) * 子矩阵行列式
                int sign = ((i + j) % 2 == 0) ? 1 : -1;
                cofactorMatrix[i][j] = sign * subDet;
            }
        }

        // 步骤2：对余子式矩阵转置，得到伴随矩阵
        int[][] adjugateMatrix = transposeMatrix(cofactorMatrix);

        return adjugateMatrix;
    }

    /**
     * 计算矩阵的行列式（复用之前的递归实现）
     * @param matrix 待计算的方阵
     * @return 行列式值
     */
    private static int calculateDeterminant(int[][] matrix) {
        int n = matrix.length;
        // 1阶矩阵
        if (n == 1) {
            return matrix[0][0];
        }
        // 2阶矩阵
        if (n == 2) {
            return matrix[0][0] * matrix[1][1] - matrix[0][1] * matrix[1][0];
        }
        // n阶矩阵（n>2）
        int det = 0;
        for (int j = 0; j < n; j++) {
            int sign = (j % 2 == 0) ? 1 : -1;
            det += matrix[0][j] * sign * calculateDeterminant(getSubMatrix(matrix, 0, j));
        }
        return det;
    }

    /**
     * 获取去掉指定行和列后的子矩阵
     * @param matrix 原矩阵
     * @param excludeRow 要排除的行索引
     * @param excludeCol 要排除的列索引
     * @return 子矩阵
     */
    private static int[][] getSubMatrix(int[][] matrix, int excludeRow, int excludeCol) {
        int n = matrix.length;
        int[][] subMatrix = new int[n - 1][n - 1];
        int subRow = 0;
        for (int i = 0; i < n; i++) {
            if (i == excludeRow) continue;
            int subCol = 0;
            for (int j = 0; j < n; j++) {
                if (j == excludeCol) continue;
                subMatrix[subRow][subCol] = matrix[i][j];
                subCol++;
            }
            subRow++;
        }
        return subMatrix;
    }

    /**
     * 矩阵转置（行变列，列变行）
     * @param matrix 原矩阵
     * @return 转置后的矩阵
     */
    private static int[][] transposeMatrix(int[][] matrix) {
        int n = matrix.length;
        int[][] transposed = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                transposed[j][i] = matrix[i][j];
            }
        }
        return transposed;
    }

    /**
     * 打印矩阵的辅助方法
     * @param matrix 要打印的矩阵
     */
    public static void printMatrix(int[][] matrix) {
        if (matrix == null) {
            System.out.println("矩阵为null");
            return;
        }
        for (int[] row : matrix) {
            for (int element : row) {
                System.out.print(element + "\t");
            }
            System.out.println();
        }
    }

    /**
     * 从控制台读取方阵
     * @param scanner Scanner对象
     * @return 读取到的整数方阵
     */
    public static int[][] readSquareMatrixFromConsole(Scanner scanner) {
        try {
            System.out.print("请输入方阵的阶数（行数=列数）：");
            int n = Integer.parseInt(scanner.nextLine().trim());
            if (n <= 0) {
                throw new IllegalArgumentException("方阵的阶数必须是正整数");
            }

            int[][] matrix = new int[n][n];
            System.out.println("请输入" + n + "阶方阵，每行输入" + n + "个整数，用空格分隔：");
            for (int i = 0; i < n; i++) {
                System.out.print("请输入第" + (i + 1) + "行：");
                String[] elements = scanner.nextLine().trim().split("\\s+");
                if (elements.length != n) {
                    throw new IllegalArgumentException(
                            "第" + (i + 1) + "行应输入" + n + "个元素，实际输入了" + elements.length + "个"
                    );
                }
                for (int j = 0; j < n; j++) {
                    matrix[i][j] = Integer.parseInt(elements[j]);
                }
            }
            return matrix;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("输入的阶数/元素必须是有效整数");
        }
    }

    // 主方法：控制台交互计算伴随矩阵
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            System.out.println("===== 矩阵伴随矩阵计算器 =====");
            // 读取方阵
            int[][] matrix = readSquareMatrixFromConsole(scanner);

            // 打印输入的方阵
            System.out.println("\n你输入的" + matrix.length + "阶方阵：");
            printMatrix(matrix);

            // 计算伴随矩阵并输出结果
            int[][] adjugateMatrix = calculateAdjugateMatrix(matrix);
            System.out.println("\n该矩阵的伴随矩阵为：");
            printMatrix(adjugateMatrix);

        } catch (IllegalArgumentException e) {
            System.out.println("\n操作失败：" + e.getMessage());
        } finally {
            scanner.close();
        }
    }
}