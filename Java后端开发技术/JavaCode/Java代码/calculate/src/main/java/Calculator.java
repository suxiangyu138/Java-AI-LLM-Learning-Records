import java.util.Scanner;

/**
 * 多功能控制台计算器
 * 支持：普通四则运算、矩阵加减乘运算
 * 优化点：结构解耦、输入校验增强、用户体验提升、代码规范优化
 */
public class Calculator {
    // 常量定义（统一管理提示文案、运算符号）
    private static final String MENU_MAIN = "===== 多功能控制台计算器 =====\n1. 普通四则运算（+、-、*、/）\n2. 矩阵运算（+、-、*）\n输入 'q' 或 'Q' 可退出程序\n";
    private static final String PROMPT_CHOICE = "请选择运算类型（输入1/2，或q退出）：";
    private static final String ERROR_INVALID_INPUT = "错误：输入无效，请重新操作！\n";
    private static final String EXIT_MSG = "\n感谢使用计算器，程序已退出！";

    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        // 主菜单展示
        System.out.println(MENU_MAIN);

        boolean isRunning = true;
        while (isRunning) {
            String userChoice = InputUtils.getValidatedInput(PROMPT_CHOICE, scanner);

            // 退出逻辑
            if (InputUtils.isQuitCommand(userChoice)) {
                isRunning = false;
                continue;
            }

            // 分支执行运算
            switch (userChoice) {
                case "1":
                    BasicCalculationHandler.handleBasicCalculation(scanner);
                    break;
                case "2":
                    MatrixCalculationHandler.handleMatrixCalculation(scanner);
                    break;
                default:
                    System.out.println("错误：请输入1、2或q！\n");
            }
        }

        // 资源释放
        scanner.close();
        System.out.println(EXIT_MSG);
    }

    // ====================== 输入工具类（统一处理输入逻辑） ======================
    static class InputUtils {
        /**
         * 获取用户输入并去除首尾空格
         */
        public static String getValidatedInput(String prompt, Scanner scanner) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            // 处理空输入
            return input.isEmpty() ? "" : input;
        }

        /**
         * 判断是否为退出指令
         */
        public static boolean isQuitCommand(String input) {
            return "q".equalsIgnoreCase(input);
        }

        /**
         * 安全解析数字（避免NumberFormatException）
         */
        public static Double parseSafeDouble(String input) {
            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        /**
         * 安全解析整数（用于矩阵行列数输入）
         */
        public static Integer parseSafeInteger(String input) {
            try {
                int num = Integer.parseInt(input);
                return num > 0 ? num : null; // 只返回正整数
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    // ====================== 普通四则运算处理类 ======================
    static class BasicCalculationHandler {
        /**
         * 处理普通四则运算逻辑
         */
        public static void handleBasicCalculation(Scanner scanner) {
            System.out.println("\n----- 普通四则运算 -----");

            // 输入第一个数字
            Double num1 = getValidNumberInput("第一个", scanner);
            if (num1 == null) return;

            // 输入运算符号
            String operator = getValidBasicOperator(scanner);
            if (operator == null) return;

            // 输入第二个数字
            Double num2 = getValidNumberInput("第二个", scanner);
            if (num2 == null) return;

            // 执行计算并输出结果
            calculateAndPrintBasicResult(num1, operator, num2);
        }

        /**
         * 获取有效的数字输入（支持退出）
         */
        private static Double getValidNumberInput(String numDesc, Scanner scanner) {
            while (true) {
                String input = InputUtils.getValidatedInput("请输入" + numDesc + "数字（输入q退出）：", scanner);
                if (InputUtils.isQuitCommand(input)) {
                    return null;
                }

                Double num = InputUtils.parseSafeDouble(input);
                if (num != null) {
                    return num;
                }
                System.out.println("错误：请输入有效的数字！");
            }
        }

        /**
         * 获取有效的四则运算符号
         */
        private static String getValidBasicOperator(Scanner scanner) {
            while (true) {
                String input = InputUtils.getValidatedInput("请输入运算符号（+、-、*、/，输入q退出）：", scanner);
                if (InputUtils.isQuitCommand(input)) {
                    return null;
                }

                if (input.matches("[+\\-*/]")) {
                    return input;
                }
                System.out.println("错误：请输入有效的运算符号（+、-、*、/）！");
            }
        }

        /**
         * 执行四则运算并打印结果
         */
        private static void calculateAndPrintBasicResult(double num1, String operator, double num2) {
            try {
                double result = switch (operator) {
                    case "+" -> num1 + num2;
                    case "-" -> num1 - num2;
                    case "*" -> num1 * num2;
                    case "/" -> {
                        if (num2 == 0) {
                            throw new ArithmeticException("除数不能为0");
                        }
                        yield num1 / num2;
                    }
                    default -> 0;
                };
                System.out.printf("计算结果：%.2f %s %.2f = %.2f%n%n", num1, operator, num2, result);
            } catch (ArithmeticException e) {
                System.out.println("错误：" + e.getMessage() + "\n");
            }
        }
    }

    // ====================== 矩阵运算处理类 ======================
    static class MatrixCalculationHandler {
        /**
         * 处理矩阵运算逻辑
         */
        public static void handleMatrixCalculation(Scanner scanner) {
            System.out.println("\n----- 矩阵运算 -----");

            // 输入第一个矩阵
            double[][] matrix1 = MatrixUtils.inputMatrix("第一个", scanner);
            if (matrix1 == null) return;

            // 输入矩阵运算符号
            String operator = getValidMatrixOperator(scanner);
            if (operator == null) return;

            // 输入第二个矩阵
            double[][] matrix2 = MatrixUtils.inputMatrix("第二个", scanner);
            if (matrix2 == null) return;

            // 执行矩阵运算并输出结果
            calculateAndPrintMatrixResult(matrix1, operator, matrix2);
        }

        /**
         * 获取有效的矩阵运算符号
         */
        private static String getValidMatrixOperator(Scanner scanner) {
            while (true) {
                String input = InputUtils.getValidatedInput("请输入矩阵运算符号（+、-、*，输入q退出）：", scanner);
                if (InputUtils.isQuitCommand(input)) {
                    return null;
                }

                if (input.matches("[+\\-*]")) {
                    return input;
                }
                System.out.println("错误：请输入有效的运算符号（+、-、*）！");
            }
        }

        /**
         * 执行矩阵运算并打印结果
         */
        private static void calculateAndPrintMatrixResult(double[][] m1, String operator, double[][] m2) {
            try {
                double[][] result = switch (operator) {
                    case "+" -> MatrixUtils.add(m1, m2);
                    case "-" -> MatrixUtils.subtract(m1, m2);
                    case "*" -> MatrixUtils.multiply(m1, m2);
                    default -> null;
                };

                System.out.println("\n计算结果矩阵：");
                MatrixUtils.printMatrix(result);
                System.out.println();
            } catch (IllegalArgumentException e) {
                System.out.println("错误：" + e.getMessage() + "\n");
            }
        }
    }

    // ====================== 矩阵工具类（纯静态方法，专注矩阵计算） ======================
    static class MatrixUtils {
        /**
         * 引导用户输入矩阵
         */
        public static double[][] inputMatrix(String matrixDesc, Scanner scanner) {
            System.out.println("请输入" + matrixDesc + "矩阵的信息：");

            // 输入行数
            Integer rows = getValidMatrixDimension("行", scanner);
            if (rows == null) return null;

            // 输入列数
            Integer cols = getValidMatrixDimension("列", scanner);
            if (cols == null) return null;

            // 输入矩阵元素
            double[][] matrix = inputMatrixElements(rows, cols, scanner);
            if (matrix == null) return null;

            // 展示输入的矩阵
            System.out.println("你输入的" + matrixDesc + "矩阵：");
            printMatrix(matrix);
            return matrix;
        }

        /**
         * 获取有效的矩阵行列数（正整数）
         */
        private static Integer getValidMatrixDimension(String dimDesc, Scanner scanner) {
            while (true) {
                String input = InputUtils.getValidatedInput("请输入矩阵的" + dimDesc + "数（正整数，输入q退出）：", scanner);
                if (InputUtils.isQuitCommand(input)) {
                    return null;
                }

                Integer num = InputUtils.parseSafeInteger(input);
                if (num != null) {
                    return num;
                }
                System.out.println("错误：请输入有效的正整数！");
            }
        }

        /**
         * 输入矩阵元素
         */
        private static double[][] inputMatrixElements(int rows, int cols, Scanner scanner) {
            double[][] matrix = new double[rows][cols];
            System.out.println("请按行输入矩阵元素（共" + rows + "行，每行" + cols + "个数字，用空格分隔，输入q退出）：");

            for (int i = 0; i < rows; i++) {
                while (true) {
                    String line = InputUtils.getValidatedInput("第" + (i + 1) + "行：", scanner);
                    if (InputUtils.isQuitCommand(line)) {
                        return null;
                    }

                    String[] elements = line.split("\\s+");
                    if (elements.length != cols) {
                        System.out.println("错误：该行必须输入" + cols + "个数字，请重新输入！");
                        continue;
                    }

                    // 解析每行元素
                    boolean isValidLine = true;
                    for (int j = 0; j < cols; j++) {
                        Double num = InputUtils.parseSafeDouble(elements[j]);
                        if (num == null) {
                            isValidLine = false;
                            break;
                        }
                        matrix[i][j] = num;
                    }

                    if (isValidLine) {
                        break;
                    } else {
                        System.out.println("错误：包含无效数字，请重新输入该行！");
                    }
                }
            }
            return matrix;
        }

        /**
         * 矩阵加法
         */
        public static double[][] add(double[][] m1, double[][] m2) {
            validateMatrixAddSub(m1, m2);
            int rows = m1.length;
            int cols = m1[0].length;
            double[][] result = new double[rows][cols];

            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    result[i][j] = m1[i][j] + m2[i][j];
                }
            }
            return result;
        }

        /**
         * 矩阵减法
         */
        public static double[][] subtract(double[][] m1, double[][] m2) {
            validateMatrixAddSub(m1, m2);
            int rows = m1.length;
            int cols = m1[0].length;
            double[][] result = new double[rows][cols];

            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    result[i][j] = m1[i][j] - m2[i][j];
                }
            }
            return result;
        }

        /**
         * 矩阵乘法
         */
        public static double[][] multiply(double[][] m1, double[][] m2) {
            validateMatrixMultiply(m1, m2);
            int m1Rows = m1.length;
            int m1Cols = m1[0].length;
            int m2Cols = m2[0].length;
            double[][] result = new double[m1Rows][m2Cols];

            for (int i = 0; i < m1Rows; i++) {
                for (int j = 0; j < m2Cols; j++) {
                    double sum = 0;
                    for (int k = 0; k < m1Cols; k++) {
                        sum += m1[i][k] * m2[k][j];
                    }
                    result[i][j] = sum;
                }
            }
            return result;
        }

        /**
         * 校验矩阵加减的合法性（行列数一致）
         */
        private static void validateMatrixAddSub(double[][] m1, double[][] m2) {
            if (m1.length != m2.length || m1[0].length != m2[0].length) {
                throw new IllegalArgumentException("矩阵加减要求两个矩阵的行数和列数完全一致");
            }
        }

        /**
         * 校验矩阵乘法的合法性（前矩阵列数=后矩阵行数）
         */
        private static void validateMatrixMultiply(double[][] m1, double[][] m2) {
            if (m1[0].length != m2.length) {
                throw new IllegalArgumentException("矩阵乘法要求第一个矩阵的列数等于第二个矩阵的行数");
            }
        }

        /**
         * 格式化打印矩阵（优化对齐，更美观）
         */
        public static void printMatrix(double[][] matrix) {
            for (double[] row : matrix) {
                System.out.print("[ ");
                for (double val : row) {
                    // 宽度8，保留2位小数，右对齐，确保矩阵列对齐
                    System.out.printf("%8.2f ", val);
                }
                System.out.println("]");
            }
        }
    }
}