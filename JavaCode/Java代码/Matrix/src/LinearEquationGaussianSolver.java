import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class LinearEquationGaussianSolver {
    // 浮点数精度阈值（避免浮点误差导致的判断错误）
    private static final double EPS = 1e-8;

    /**
     * 高斯消元法求解线性方程组 Ax = b
     * @param augmentedMatrix 增广矩阵（m行n+1列，最后一列是常数项）
     * @param varCount 未知量个数n
     * @return 解的结果封装（类型+具体解）
     */
    public static SolutionResult solve(double[][] augmentedMatrix, int varCount) {
        int m = augmentedMatrix.length;    // 方程个数
        int n = varCount;                  // 未知量个数
        int rankA = 0;                     // 系数矩阵的秩
        int rankAug = 0;                   // 增广矩阵的秩
        int[] pivotCol = new int[m];       // 记录主元列索引（-1表示无主元）
        for (int i = 0; i < m; i++) pivotCol[i] = -1;

        // 步骤1：将增广矩阵化为行阶梯形
        int col = 0; // 当前处理的列
        for (int row = 0; row < m && col < n; row++, col++) {
            // 1.1 找当前列的主元行（绝对值最大的行，避免除0和精度损失）
            int pivotRow = row;
            for (int i = row; i < m; i++) {
                if (Math.abs(augmentedMatrix[i][col]) > EPS) {
                    pivotRow = i;
                    break;
                }
            }

            // 1.2 当前列无主元，处理下一列（行不变）
            if (Math.abs(augmentedMatrix[pivotRow][col]) < EPS) {
                row--;
                continue;
            }

            // 1.3 交换主元行到当前行
            if (pivotRow != row) {
                double[] temp = augmentedMatrix[row];
                augmentedMatrix[row] = augmentedMatrix[pivotRow];
                augmentedMatrix[pivotRow] = temp;
            }
            pivotCol[row] = col; // 记录主元列

            // 1.4 消去主元行下方的所有行
            for (int i = row + 1; i < m; i++) {
                double factor = augmentedMatrix[i][col] / augmentedMatrix[row][col];
                for (int j = col; j <= n; j++) {
                    augmentedMatrix[i][j] -= factor * augmentedMatrix[row][j];
                }
            }
        }

        // 步骤2：计算系数矩阵和增广矩阵的秩
        // 系数矩阵秩：主元行的数量
        for (int i = 0; i < m; i++) {
            if (pivotCol[i] != -1) rankA++;
        }
        // 增广矩阵秩：检查是否有 [0 0 ... 0 | c] (c≠0) 的行
        rankAug = rankA;
        for (int i = 0; i < m; i++) {
            boolean allZero = true;
            // 检查系数部分是否全0
            for (int j = 0; j < n; j++) {
                if (Math.abs(augmentedMatrix[i][j]) > EPS) {
                    allZero = false;
                    break;
                }
            }
            // 系数全0但常数项非0 → 增广矩阵秩 > 系数矩阵秩 → 无解
            if (allZero && Math.abs(augmentedMatrix[i][n]) > EPS) {
                rankAug = rankA + 1;
                break;
            }
        }

        // 步骤3：判断解的类型并计算具体解
        SolutionResult result = new SolutionResult();
        if (rankA != rankAug) {
            // 情况1：无解
            result.type = SolutionType.NO_SOLUTION;
        } else if (rankA == n) {
            // 情况2：唯一解（回代求解）
            result.type = SolutionType.UNIQUE_SOLUTION;
            double[] uniqueSolution = new double[n];
            // 从最后一行开始回代
            for (int row = rankA - 1; row >= 0; row--) {
                int pc = pivotCol[row];
                double sum = augmentedMatrix[row][n];
                for (int j = pc + 1; j < n; j++) {
                    sum -= augmentedMatrix[row][j] * uniqueSolution[j];
                }
                uniqueSolution[pc] = sum / augmentedMatrix[row][pc];
            }
            result.uniqueSolution = uniqueSolution;
        } else {
            // 情况3：无穷多解（含自由未知量）
            result.type = SolutionType.INFINITE_SOLUTIONS;
            result.freeVars = new ArrayList<>();
            result.solutionExpr = new ArrayList<>();

            // 标记自由未知量（无主元的列）
            boolean[] isFree = new boolean[n];
            for (int j = 0; j < n; j++) {
                boolean isPivot = false;
                for (int i = 0; i < m; i++) {
                    if (pivotCol[i] == j) {
                        isPivot = true;
                        break;
                    }
                }
                if (!isPivot) {
                    isFree[j] = true;
                    result.freeVars.add(j + 1); // 自由未知量编号（从1开始）
                }
            }

            // 构造通解表达式
            double[] baseSolution = new double[n]; // 特解
            List<double[]> basis = new ArrayList<>(); // 基础解系

            // 先求特解（所有自由未知量设为0）
            for (int row = 0; row < rankA; row++) {
                int pc = pivotCol[row];
                double sum = augmentedMatrix[row][n];
                for (int j = pc + 1; j < n; j++) {
                    sum -= augmentedMatrix[row][j] * baseSolution[j];
                }
                baseSolution[pc] = sum / augmentedMatrix[row][pc];
            }

            // 求基础解系（每个自由未知量轮流设为1，其余为0）
            for (int j = 0; j < n; j++) {
                if (isFree[j]) {
                    double[] vec = new double[n];
                    vec[j] = 1.0; // 当前自由未知量设为1
                    // 回代求非自由未知量
                    for (int row = rankA - 1; row >= 0; row--) {
                        int pc = pivotCol[row];
                        double sum = 0;
                        for (int k = pc + 1; k < n; k++) {
                            sum -= augmentedMatrix[row][k] * vec[k];
                        }
                        vec[pc] = sum / augmentedMatrix[row][pc];
                    }
                    basis.add(vec);
                }
            }

            // 拼接通解表达式
            StringBuilder expr = new StringBuilder();
            expr.append("通解 = ");
            // 特解部分
            expr.append("(");
            for (int i = 0; i < n; i++) {
                expr.append(String.format("%.4f", baseSolution[i]));
                if (i < n - 1) expr.append(", ");
            }
            expr.append(") + ");
            // 基础解系部分
            for (int i = 0; i < basis.size(); i++) {
                expr.append("k").append(i + 1).append("×(");
                double[] vec = basis.get(i);
                for (int j = 0; j < n; j++) {
                    expr.append(String.format("%.4f", vec[j]));
                    if (j < n - 1) expr.append(", ");
                }
                expr.append(")");
                if (i < basis.size() - 1) expr.append(" + ");
            }
            expr.append(" （k1,k2...为任意常数）");
            result.solutionExpr.add(expr.toString());
        }

        return result;
    }

    // ====================== 控制台输入辅助方法 ======================
    /**
     * 读取系数矩阵
     */
    public static double[][] readCoefficientMatrix(Scanner scanner, int m, int n) {
        try {
            double[][] matrix = new double[m][n];
            System.out.println("请输入" + m + "行" + n + "列的系数矩阵（每行" + n + "个数，用空格分隔，支持整数/小数）：");
            for (int i = 0; i < m; i++) {
                System.out.print("第" + (i + 1) + "行：");
                String[] elements = scanner.nextLine().trim().split("\\s+");
                if (elements.length != n) {
                    throw new IllegalArgumentException("第" + (i + 1) + "行应输入" + n + "个元素，实际输入了" + elements.length + "个");
                }
                for (int j = 0; j < n; j++) {
                    matrix[i][j] = Double.parseDouble(elements[j]);
                }
            }
            return matrix;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("系数矩阵元素必须是有效数字（整数/小数）");
        }
    }

    /**
     * 读取常数项向量
     */
    public static double[] readConstantVector(Scanner scanner, int m) {
        try {
            double[] vector = new double[m];
            System.out.println("请输入" + m + "个常数项（用空格分隔，支持整数/小数）：");
            System.out.print("常数项：");
            String[] elements = scanner.nextLine().trim().split("\\s+");
            if (elements.length != m) {
                throw new IllegalArgumentException("应输入" + m + "个常数项，实际输入了" + elements.length + "个");
            }
            for (int i = 0; i < m; i++) {
                vector[i] = Double.parseDouble(elements[i]);
            }
            return vector;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("常数项必须是有效数字（整数/小数）");
        }
    }

    /**
     * 构造增广矩阵（系数矩阵 + 常数项列）
     */
    public static double[][] buildAugmentedMatrix(double[][] coeffMatrix, double[] constVector) {
        int m = coeffMatrix.length;
        int n = coeffMatrix[0].length;
        double[][] augMatrix = new double[m][n + 1];
        for (int i = 0; i < m; i++) {
            System.arraycopy(coeffMatrix[i], 0, augMatrix[i], 0, n);
            augMatrix[i][n] = constVector[i];
        }
        return augMatrix;
    }

    // ====================== 打印辅助方法 ======================
    /**
     * 打印浮点数矩阵
     */
    public static void printDoubleMatrix(double[][] matrix) {
        if (matrix == null) {
            System.out.println("矩阵为null");
            return;
        }
        for (double[] row : matrix) {
            for (double element : row) {
                System.out.printf("%.4f\t", element);
            }
            System.out.println();
        }
    }

    /**
     * 打印唯一解
     */
    public static void printUniqueSolution(double[] solution) {
        for (int i = 0; i < solution.length; i++) {
            System.out.printf("x%d = %.4f\n", (i + 1), solution[i]);
        }
    }

    // ====================== 结果封装类 ======================
    /**
     * 解的类型枚举
     */
    public enum SolutionType {
        UNIQUE_SOLUTION,    // 唯一解
        NO_SOLUTION,        // 无解
        INFINITE_SOLUTIONS  // 无穷多解
    }

    /**
     * 解的结果封装
     */
    public static class SolutionResult {
        SolutionType type;          // 解的类型
        double[] uniqueSolution;    // 唯一解（type=UNIQUE_SOLUTION时有效）
        List<Integer> freeVars;     // 自由未知量编号（type=INFINITE_SOLUTIONS时有效）
        List<String> solutionExpr;  // 无穷多解的表达式（type=INFINITE_SOLUTIONS时有效）
    }

    // ====================== 主方法（交互入口） ======================
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            System.out.println("===== 线性方程组求解器（高斯消元法） =====");
            // 1. 输入方程个数和未知量个数
            System.out.print("请输入方程个数m：");
            int m = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("请输入未知量个数n：");
            int n = Integer.parseInt(scanner.nextLine().trim());
            if (m <= 0 || n <= 0) {
                throw new IllegalArgumentException("方程个数m和未知量个数n必须是正整数");
            }

            // 2. 输入系数矩阵和常数项
            double[][] coeffMatrix = readCoefficientMatrix(scanner, m, n);
            double[] constVector = readConstantVector(scanner, m);

            // 3. 构造增广矩阵并打印
            double[][] augMatrix = buildAugmentedMatrix(coeffMatrix, constVector);
            System.out.println("\n===== 输入的增广矩阵（A|b） =====");
            printDoubleMatrix(augMatrix);

            // 4. 求解方程组
            SolutionResult result = solve(augMatrix, n);

            // 5. 打印求解结果
            System.out.println("\n===== 求解结果 =====");
            switch (result.type) {
                case NO_SOLUTION:
                    System.out.println("该线性方程组无解");
                    break;
                case UNIQUE_SOLUTION:
                    System.out.println("该线性方程组有唯一解：");
                    printUniqueSolution(result.uniqueSolution);
                    break;
                case INFINITE_SOLUTIONS:
                    System.out.println("该线性方程组有无穷多解：");
                    System.out.println("自由未知量：" + result.freeVars);
                    for (String expr : result.solutionExpr) {
                        System.out.println(expr);
                    }
                    break;
            }

        } catch (IllegalArgumentException e) {
            System.out.println("\n求解失败：" + e.getMessage());
        } finally {
            scanner.close();
        }
    }
}