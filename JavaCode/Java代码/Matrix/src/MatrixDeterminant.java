import java.util.Scanner;

public class MatrixDeterminant {

    /**
     * 计算矩阵行列式的核心方法（递归实现）
     * @param matrix 待计算的方阵
     * @return 矩阵的行列式值
     * @throws IllegalArgumentException 非方阵时抛出异常
     */
    public static int calculateDeterminant(int[][] matrix) {
        // 检查矩阵是否为null
        if (matrix == null) {
            throw new IllegalArgumentException("矩阵不能为null");
        }

        // 验证是否为方阵（行数 = 列数）
        int n = matrix.length;
        for (int[] row : matrix) {
            if (row == null || row.length != n) {
                throw new IllegalArgumentException("只有方阵（行数=列数）才能计算行列式！");
            }
        }

        // 1阶矩阵：直接返回唯一元素
        if (n == 1) {
            return matrix[0][0];
        }

        // 2阶矩阵：直接用公式计算
        if (n == 2) {
            return matrix[0][0] * matrix[1][1] - matrix[0][1] * matrix[1][0];
        }

        // n阶矩阵（n>2）：按第一行展开递归计算
        int determinant = 0;
        for (int j = 0; j < n; j++) {
            // 计算代数余子式的符号：(-1)^(1+j)，这里索引从0开始，所以是(-1)^(0+j)
            int sign = (j % 2 == 0) ? 1 : -1;
            // 获取去掉第0行第j列的子矩阵
            int[][] subMatrix = getSubMatrix(matrix, 0, j);
            // 累加：元素值 × 符号 × 子矩阵行列式
            determinant += matrix[0][j] * sign * calculateDeterminant(subMatrix);
        }

        return determinant;
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
        int subRow = 0; // 子矩阵的行索引

        for (int i = 0; i < n; i++) {
            // 跳过要排除的行
            if (i == excludeRow) {
                continue;
            }
            int subCol = 0; // 子矩阵的列索引
            for (int j = 0; j < n; j++) {
                // 跳过要排除的列
                if (j == excludeCol) {
                    continue;
                }
                subMatrix[subRow][subCol] = matrix[i][j];
                subCol++;
            }
            subRow++;
        }

        return subMatrix;
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
            // 输入方阵的阶数（行数=列数）
            System.out.print("请输入方阵的阶数（行数=列数）：");
            int n = Integer.parseInt(scanner.nextLine().trim());

            // 验证阶数是否为正整数
            if (n <= 0) {
                throw new IllegalArgumentException("方阵的阶数必须是正整数");
            }

            // 逐行输入矩阵元素
            int[][] matrix = new int[n][n];
            System.out.println("请输入" + n + "阶方阵，每行输入" + n + "个整数，用空格分隔：");
            for (int i = 0; i < n; i++) {
                System.out.print("请输入第" + (i + 1) + "行：");
                String[] elements = scanner.nextLine().trim().split("\\s+");

                // 验证每行元素个数
                if (elements.length != n) {
                    throw new IllegalArgumentException(
                            "第" + (i + 1) + "行应输入" + n + "个元素，实际输入了" + elements.length + "个"
                    );
                }

                // 转换为整数并赋值
                for (int j = 0; j < n; j++) {
                    matrix[i][j] = Integer.parseInt(elements[j]);
                }
            }
            return matrix;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("输入的阶数/元素必须是有效整数");
        }
    }

    // 主方法：控制台交互计算矩阵行列式
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            System.out.println("===== 矩阵行列式计算器 =====");
            // 读取方阵
            int[][] matrix = readSquareMatrixFromConsole(scanner);

            // 打印输入的方阵
            System.out.println("\n你输入的" + matrix.length + "阶方阵：");
            printMatrix(matrix);

            // 计算行列式并输出结果
            int determinant = calculateDeterminant(matrix);
            System.out.println("\n该矩阵的行列式值为：" + determinant);

        } catch (IllegalArgumentException e) {
            System.out.println("\n操作失败：" + e.getMessage());
        } finally {
            scanner.close(); // 释放资源
        }
    }
}