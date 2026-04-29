import java.util.Scanner;

public class MatrixMultiplication {

    /**
     * 矩阵乘法核心方法
     * @param matrix1 第一个矩阵（左矩阵）
     * @param matrix2 第二个矩阵（右矩阵）
     * @return 两个矩阵相乘后的结果矩阵
     * @throws IllegalArgumentException 当矩阵维度不满足乘法条件时抛出异常
     */
    public static int[][] multiplyMatrices(int[][] matrix1, int[][] matrix2) {
        // 检查矩阵是否为null
        if (matrix1 == null || matrix2 == null) {
            throw new IllegalArgumentException("矩阵不能为null");
        }

        // 获取矩阵的行列数
        int rows1 = matrix1.length;       // 左矩阵行数
        int cols1 = matrix1[0].length;    // 左矩阵列数
        int rows2 = matrix2.length;       // 右矩阵行数
        int cols2 = matrix2[0].length;    // 右矩阵列数

        // 验证乘法条件：左矩阵列数 == 右矩阵行数
        if (cols1 != rows2) {
            throw new IllegalArgumentException(
                    "矩阵无法相乘！第一个矩阵的列数(" + cols1 + ") 必须等于第二个矩阵的行数(" + rows2 + ")"
            );
        }

        // 初始化结果矩阵：行数=rows1，列数=cols2
        int[][] result = new int[rows1][cols2];

        // 矩阵乘法核心计算
        for (int i = 0; i < rows1; i++) {         // 遍历左矩阵的每一行
            for (int j = 0; j < cols2; j++) {     // 遍历右矩阵的每一列
                int sum = 0;
                for (int k = 0; k < cols1; k++) { // 累加对应元素的乘积
                    sum += matrix1[i][k] * matrix2[k][j];
                }
                result[i][j] = sum;
            }
        }

        return result;
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
     * 从控制台读取矩阵
     * @param scanner Scanner对象
     * @param matrixName 矩阵名称（用于提示用户）
     * @return 读取到的整数矩阵
     */
    public static int[][] readMatrixFromConsole(Scanner scanner, String matrixName) {
        try {
            // 输入矩阵的行数和列数
            System.out.print("请输入" + matrixName + "的行数：");
            int rows = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("请输入" + matrixName + "的列数：");
            int cols = Integer.parseInt(scanner.nextLine().trim());

            // 验证行列数是否为正整数
            if (rows <= 0 || cols <= 0) {
                throw new IllegalArgumentException("行数和列数必须是正整数");
            }

            // 逐行输入矩阵元素
            int[][] matrix = new int[rows][cols];
            System.out.println("请输入" + rows + "行" + cols + "列的" + matrixName + "，每行输入" + cols + "个整数，用空格分隔：");
            for (int i = 0; i < rows; i++) {
                System.out.print("请输入第" + (i + 1) + "行：");
                String[] elements = scanner.nextLine().trim().split("\\s+");

                // 验证每行元素个数
                if (elements.length != cols) {
                    throw new IllegalArgumentException(
                            matrixName + "第" + (i + 1) + "行应输入" + cols + "个元素，实际输入了" + elements.length + "个"
                    );
                }

                // 转换为整数并赋值
                for (int j = 0; j < cols; j++) {
                    matrix[i][j] = Integer.parseInt(elements[j]);
                }
            }
            return matrix;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(matrixName + "的行列数/元素必须是有效整数");
        }
    }

    // 主方法：控制台交互执行矩阵乘法
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            System.out.println("===== 矩阵乘法计算器 =====");
            // 读取第一个矩阵（左矩阵）
            int[][] matrixA = readMatrixFromConsole(scanner, "矩阵A（左矩阵）");
            // 读取第二个矩阵（右矩阵）
            int[][] matrixB = readMatrixFromConsole(scanner, "矩阵B（右矩阵）");

            // 打印原始矩阵
            System.out.println("\n你输入的矩阵A：");
            printMatrix(matrixA);
            System.out.println("\n你输入的矩阵B：");
            printMatrix(matrixB);

            // 执行矩阵乘法并打印结果
            int[][] productMatrix = multiplyMatrices(matrixA, matrixB);
            System.out.println("\n矩阵A × 矩阵B 的结果：");
            printMatrix(productMatrix);

        } catch (IllegalArgumentException e) {
            System.out.println("\n操作失败：" + e.getMessage());
        } finally {
            scanner.close(); // 释放资源
        }
    }
}