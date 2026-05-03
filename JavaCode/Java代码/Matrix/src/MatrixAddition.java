import java.util.Scanner;

public class MatrixAddition {

    /**
     * 矩阵加法核心方法
     * @param matrix1 第一个矩阵
     * @param matrix2 第二个矩阵
     * @return 两个矩阵相加后的结果矩阵
     * @throws IllegalArgumentException 当两个矩阵行列数不匹配时抛出异常
     */
    public static int[][] addMatrices(int[][] matrix1, int[][] matrix2) {
        // 检查矩阵是否为null
        if (matrix1 == null || matrix2 == null) {
            throw new IllegalArgumentException("矩阵不能为null");
        }

        // 获取矩阵的行数
        int rows1 = matrix1.length;
        int rows2 = matrix2.length;

        // 检查行数是否一致
        if (rows1 != rows2) {
            throw new IllegalArgumentException("两个矩阵的行数不匹配，无法相加");
        }

        // 检查列数是否一致（先确保至少有一行）
        if (rows1 > 0) {
            int cols1 = matrix1[0].length;
            int cols2 = matrix2[0].length;
            if (cols1 != cols2) {
                throw new IllegalArgumentException("两个矩阵的列数不匹配，无法相加");
            }
        }

        // 初始化结果矩阵
        int[][] result = new int[rows1][rows1 > 0 ? matrix1[0].length : 0];

        // 遍历矩阵，执行加法运算
        for (int i = 0; i < rows1; i++) {
            for (int j = 0; j < matrix1[i].length; j++) {
                result[i][j] = matrix1[i][j] + matrix2[i][j];
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
     * @param rows 矩阵行数
     * @param cols 矩阵列数
     * @return 读取到的整数矩阵
     */
    public static int[][] readMatrixFromConsole(Scanner scanner, int rows, int cols) {
        int[][] matrix = new int[rows][cols];
        System.out.println("请输入" + rows + "行" + cols + "列的矩阵，每行输入" + cols + "个整数，用空格分隔：");

        for (int i = 0; i < rows; i++) {
            System.out.print("请输入第" + (i + 1) + "行：");
            // 读取一行输入并按空格分割
            String[] elements = scanner.nextLine().trim().split("\\s+");

            // 验证输入的元素个数是否符合列数要求
            if (elements.length != cols) {
                throw new IllegalArgumentException("第" + (i + 1) + "行输入的元素个数应为" + cols + "个，实际输入了" + elements.length + "个");
            }

            // 将字符串转换为整数并赋值给矩阵
            for (int j = 0; j < cols; j++) {
                try {
                    matrix[i][j] = Integer.parseInt(elements[j]);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("第" + (i + 1) + "行第" + (j + 1) + "个元素不是有效的整数");
                }
            }
        }
        return matrix;
    }

    // 主方法：从控制台输入矩阵并执行加法
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            // 第一步：输入矩阵的行数和列数
            System.out.print("请输入矩阵的行数：");
            int rows = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("请输入矩阵的列数：");
            int cols = Integer.parseInt(scanner.nextLine().trim());

            // 验证行列数是否为正整数
            if (rows <= 0 || cols <= 0) {
                throw new IllegalArgumentException("行数和列数必须是正整数");
            }

            // 第二步：读取第一个矩阵
            System.out.println("\n===== 输入第一个矩阵 =====");
            int[][] matrix1 = readMatrixFromConsole(scanner, rows, cols);

            // 第三步：读取第二个矩阵
            System.out.println("\n===== 输入第二个矩阵 =====");
            int[][] matrix2 = readMatrixFromConsole(scanner, rows, cols);

            // 第四步：打印原始矩阵
            System.out.println("\n你输入的第一个矩阵：");
            printMatrix(matrix1);
            System.out.println("\n你输入的第二个矩阵：");
            printMatrix(matrix2);

            // 第五步：执行矩阵加法并打印结果
            int[][] sumMatrix = addMatrices(matrix1, matrix2);
            System.out.println("\n两个矩阵相加的结果：");
            printMatrix(sumMatrix);

        } catch (NumberFormatException e) {
            System.out.println("输入错误：请输入有效的整数作为行数/列数");
        } catch (IllegalArgumentException e) {
            System.out.println("操作失败：" + e.getMessage());
        } finally {
            // 关闭Scanner，释放资源
            scanner.close();
        }
    }
}