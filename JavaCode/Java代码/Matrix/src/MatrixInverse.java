import java.util.Scanner;

public class MatrixInverse {

    /**
     * 计算矩阵的逆矩阵（伴随矩阵法）
     * @param matrix 待计算的方阵
     * @return 逆矩阵（浮点数类型）
     * @throws IllegalArgumentException 非方阵/行列式为0时抛出异常
     */
    public static double[][] calculateInverseMatrix(int[][] matrix) {
        // 检查矩阵是否为null
        if (matrix == null) {
            throw new IllegalArgumentException("矩阵不能为null");
        }

        // 验证是否为方阵
        int n = matrix.length;
        for (int[] row : matrix) {
            if (row == null || row.length != n) {
                throw new IllegalArgumentException("只有方阵（行数=列数）才能计算逆矩阵！");
            }
        }

        // 步骤1：计算矩阵的行列式
        int determinant = calculateDeterminant(matrix);
        if (determinant == 0) {
            throw new IllegalArgumentException("该矩阵的行列式为0，是奇异矩阵，不存在逆矩阵！");
        }

        // 步骤2：计算矩阵的伴随矩阵（整数型）
        int[][] adjugateMatrix = calculateAdjugateMatrix(matrix);

        // 步骤3：伴随矩阵 ÷ 行列式 = 逆矩阵（转换为浮点数）
        double[][] inverseMatrix = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                inverseMatrix[i][j] = (double) adjugateMatrix[i][j] / determinant;
            }
        }

        return inverseMatrix;
    }

    // ====================== 复用之前实现的核心方法 ======================
    /**
     * 计算矩阵行列式（递归实现）
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
     * 计算矩阵的伴随矩阵
     */
    private static int[][] calculateAdjugateMatrix(int[][] matrix) {
        int n = matrix.length;
        // 1阶矩阵的伴随矩阵是[[1]]
        if (n == 1) {
            return new int[][]{{1}};
        }
        // 计算代数余子式矩阵
        int[][] cofactorMatrix = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                int[][] subMatrix = getSubMatrix(matrix, i, j);
                int subDet = calculateDeterminant(subMatrix);
                int sign = ((i + j) % 2 == 0) ? 1 : -1;
                cofactorMatrix[i][j] = sign * subDet;
            }
        }
        // 转置得到伴随矩阵
        return transposeMatrix(cofactorMatrix);
    }

    /**
     * 获取去掉指定行和列的子矩阵
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
     * 矩阵转置
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

    // ====================== 辅助方法 ======================
    /**
     * 打印整数矩阵
     */
    public static void printIntMatrix(int[][] matrix) {
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
     * 打印浮点数矩阵（保留4位小数，格式更整洁）
     */
    public static void printDoubleMatrix(double[][] matrix) {
        if (matrix == null) {
            System.out.println("矩阵为null");
            return;
        }
        for (double[] row : matrix) {
            for (double element : row) {
                // 格式化输出，保留4位小数
                System.out.printf("%.4f\t", element);
            }
            System.out.println();
        }
    }

    /**
     * 从控制台读取方阵
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

    // ====================== 主方法（交互入口） ======================
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            System.out.println("===== 矩阵逆矩阵计算器 =====");
            // 1. 读取用户输入的方阵
            int[][] matrix = readSquareMatrixFromConsole(scanner);

            // 2. 打印输入的方阵
            System.out.println("\n你输入的" + matrix.length + "阶方阵：");
            printIntMatrix(matrix);

            // 3. 计算逆矩阵
            double[][] inverseMatrix = calculateInverseMatrix(matrix);

            // 4. 打印逆矩阵
            System.out.println("\n该矩阵的逆矩阵（保留4位小数）：");
            printDoubleMatrix(inverseMatrix);

        } catch (IllegalArgumentException e) {
            System.out.println("\n操作失败：" + e.getMessage());
        } finally {
            scanner.close(); // 释放资源
        }
    }
}