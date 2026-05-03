import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * 矩阵特征值+特征向量求解器
 * 低阶方阵（1/2/3阶）：特征方程法求特征值 + 高斯消元求特征向量（解析解）
 * 高阶方阵（≥4阶）：幂法求主特征值 + 同步迭代求主特征向量（数值解）
 * 仅支持方阵，支持实/复特征值、实/复特征向量输出
 */
public class MatrixEigenvalueEigenvector {
    // 浮点数精度阈值
    private static final double EPS = 1e-8;
    // 幂法最大迭代次数
    private static final int MAX_ITER = 1000;
    // 幂法收敛阈值
    private static final double CONVERGENCE = 1e-6;

    /**
     * 特征值+特征向量结果封装
     */
    public static class EigenPair {
        Complex eigenvalue;       // 特征值
        List<ComplexVector> eigenvectors; // 对应特征向量（基础解系）

        public EigenPair(Complex eigenvalue, List<ComplexVector> eigenvectors) {
            this.eigenvalue = eigenvalue;
            this.eigenvectors = eigenvectors;
        }
    }

    /**
     * 复数类：实部+虚部
     */
    public static class Complex {
        double real;
        double imag;

        public Complex(double real, double imag) {
            this.real = real;
            this.imag = imag;
        }

        // 复数乘法
        public Complex multiply(Complex other) {
            double r = this.real * other.real - this.imag * other.imag;
            double i = this.real * other.imag + this.imag * other.real;
            return new Complex(r, i);
        }

        // 复数加法
        public Complex add(Complex other) {
            return new Complex(this.real + other.real, this.imag + other.imag);
        }

        // 复数减法
        public Complex subtract(Complex other) {
            return new Complex(this.real - other.real, this.imag - other.imag);
        }

        // 复数数乘
        public Complex multiply(double num) {
            return new Complex(this.real * num, this.imag * num);
        }

        // 判断是否为0
        public boolean isZero() {
            return Math.abs(real) < EPS && Math.abs(imag) < EPS;
        }
    }

    /**
     * 复数向量类：存储复数数组，代表特征向量
     */
    public static class ComplexVector {
        Complex[] data;

        public ComplexVector(int n) {
            data = new Complex[n];
            for (int i = 0; i < n; i++) {
                data[i] = new Complex(0, 0);
            }
        }

        public ComplexVector(Complex[] data) {
            this.data = data;
        }

        // 获取向量维度
        public int getDim() {
            return data.length;
        }
    }

    // 主方法：控制台交互入口
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        try {
            System.out.println("===== 矩阵特征值+特征向量求解器 =====");
            System.out.print("请输入方阵的阶数n：");
            int n = Integer.parseInt(scanner.nextLine().trim());
            if (n <= 0) {
                throw new IllegalArgumentException("阶数n必须为正整数");
            }
            // 读取方阵（支持整数/小数）
            double[][] matrix = readSquareMatrix(scanner, n);
            // 打印输入的矩阵
            System.out.println("\n你输入的" + n + "阶方阵：");
            printDoubleMatrix(matrix);

            // 求解特征值+特征向量
            List<EigenPair> eigenPairs = solveEigenvalueEigenvector(matrix, n);

            // 打印结果
            System.out.println("\n===== 特征值+特征向量求解结果 =====");
            if (n <= 3) {
                System.out.println(n + "阶方阵，解析解：");
            } else {
                System.out.println(n + "阶方阵，幂法求主特征值+对应特征向量（数值解）：");
            }
            for (int i = 0; i < eigenPairs.size(); i++) {
                EigenPair pair = eigenPairs.get(i);
                System.out.printf("【%d】特征值λ = %s\n", i + 1, formatComplex(pair.eigenvalue));
                System.out.println("   对应特征向量（基础解系，非零线性组合均为特征向量）：");
                for (int j = 0; j < pair.eigenvectors.size(); j++) {
                    System.out.printf("     ξ%d = %s\n", j + 1, formatComplexVector(pair.eigenvectors.get(j)));
                }
                System.out.println();
            }

        } catch (IllegalArgumentException e) {
            System.out.println("\n求解失败：" + e.getMessage());
        } finally {
            scanner.close();
        }
    }

    /**
     * 统一求解入口：根据阶数选择方法
     */
    public static List<EigenPair> solveEigenvalueEigenvector(double[][] matrix, int n) {
        List<EigenPair> eigenPairs = new ArrayList<>();
        switch (n) {
            case 1:
                eigenPairs = solve1Order(matrix);
                break;
            case 2:
                eigenPairs = solve2Order(matrix);
                break;
            case 3:
                eigenPairs = solve3Order(matrix);
                break;
            default: // n≥4，幂法求主特征值+特征向量
                EigenPair mainPair = powerMethodWithVector(matrix);
                eigenPairs.add(mainPair);
                break;
        }
        return eigenPairs;
    }

    // ====================== 低阶方阵：解析解（特征值+特征向量） ======================
    /**
     * 1阶方阵求解：特征值为唯一元素，特征向量为任意非零数（取[1]）
     */
    private static List<EigenPair> solve1Order(double[][] matrix) {
        List<EigenPair> res = new ArrayList<>();
        Complex lambda = new Complex(matrix[0][0], 0);
        // 特征向量：任意非零数，取基础解系[1]
        List<ComplexVector> vectors = new ArrayList<>();
        Complex[] vecData = {new Complex(1, 0)};
        vectors.add(new ComplexVector(vecData));
        res.add(new EigenPair(lambda, vectors));
        return res;
    }

    /**
     * 2阶方阵求解：特征方程法求特征值 + 高斯消元求特征向量
     */
    private static List<EigenPair> solve2Order(double[][] matrix) {
        List<EigenPair> res = new ArrayList<>();
        double a = matrix[0][0], b = matrix[0][1];
        double c = matrix[1][0], d = matrix[1][1];
        double tr = a + d;
        double det = a * d - b * c;
        double delta = tr * tr - 4 * det;

        // 求所有特征值
        List<Complex> lambdas = new ArrayList<>();
        if (Math.abs(delta) < EPS) {
            lambdas.add(new Complex(tr / 2, 0));
        } else if (delta > 0) {
            double sqrtD = Math.sqrt(delta);
            lambdas.add(new Complex((tr + sqrtD) / 2, 0));
            lambdas.add(new Complex((tr - sqrtD) / 2, 0));
        } else {
            double real = tr / 2;
            double imag = Math.sqrt(-delta) / 2;
            lambdas.add(new Complex(real, imag));
            lambdas.add(new Complex(real, -imag));
        }

        // 对每个特征值，求对应特征向量
        for (Complex lambda : lambdas) {
            List<ComplexVector> vectors = solveHomogeneousEquation(buildLambdaIMinusA(lambda, matrix));
            res.add(new EigenPair(lambda, vectors));
        }
        return res;
    }

    /**
     * 3阶方阵求解：特征方程法求特征值 + 高斯消元求特征向量
     */
    private static List<EigenPair> solve3Order(double[][] matrix) {
        List<EigenPair> res = new ArrayList<>();
        double a11 = matrix[0][0], a12 = matrix[0][1], a13 = matrix[0][2];
        double a21 = matrix[1][0], a22 = matrix[1][1], a23 = matrix[1][2];
        double a31 = matrix[2][0], a32 = matrix[2][1], a33 = matrix[2][2];

        double tr = a11 + a22 + a33;
        double s2 = (a11*a22 - a12*a21) + (a11*a33 - a13*a31) + (a22*a33 - a23*a32);
        double det = a11*(a22*a33 - a23*a32) - a12*(a21*a33 - a23*a31) + a13*(a21*a32 - a22*a31);
        double p = -tr, q = s2, r = -det;

        // 求所有特征值
        List<Complex> lambdas = solveCubicEquation(p, q, r);
        // 对每个特征值，求对应特征向量
        for (Complex lambda : lambdas) {
            List<ComplexVector> vectors = solveHomogeneousEquation(buildLambdaIMinusA(lambda, matrix));
            res.add(new EigenPair(lambda, vectors));
        }
        return res;
    }

    /**
     * 构造矩阵 λI - A
     */
    private static Complex[][] buildLambdaIMinusA(Complex lambda, double[][] A) {
        int n = A.length;
        Complex[][] mat = new Complex[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                // 对角线元素：λ - A[i][j]，非对角线：-A[i][j]
                if (i == j) {
                    mat[i][j] = lambda.subtract(new Complex(A[i][j], 0));
                } else {
                    mat[i][j] = new Complex(-A[i][j], 0);
                }
            }
        }
        return mat;
    }

    /**
     * 求解齐次线性方程组 Ax=0 的基础解系（特征向量）
     */
    private static List<ComplexVector> solveHomogeneousEquation(Complex[][] A) {
        List<ComplexVector> basis = new ArrayList<>();
        int n = A.length;
        // 高斯消元将A化为行最简形
        Complex[][] rref = reduceRowEchelonForm(A);
        // 记录主元列和自由列
        int[] pivotCol = new int[n];
        boolean[] isFree = new boolean[n];
        int pivotCount = 0;
        for (int i = 0; i < n; i++) pivotCol[i] = -1;
        for (int i = 0; i < n; i++) isFree[i] = true;

        for (int row = 0; row < n && pivotCount < n; row++) {
            // 找当前行的主元列
            int col = -1;
            for (int j = 0; j < n; j++) {
                if (!rref[row][j].isZero()) {
                    col = j;
                    break;
                }
            }
            if (col == -1) break; // 全零行
            pivotCol[pivotCount++] = col;
            isFree[col] = false;
        }
        int freeCount = 0;
        for (boolean f : isFree) if (f) freeCount++;
        if (freeCount == 0) return basis; // 仅有零解（理论上特征值对应必有非零解）

        // 构造基础解系
        for (int f = 0; f < n; f++) {
            if (!isFree[f]) continue;
            ComplexVector vec = new ComplexVector(n);
            // 自由变量设为1，其余自由变量设为0
            vec.data[f] = new Complex(1, 0);
            // 回代求主元变量
            for (int p = pivotCount - 1; p >= 0; p--) {
                int col = pivotCol[p];
                Complex sum = new Complex(0, 0);
                for (int j = col + 1; j < n; j++) {
                    sum = sum.add(rref[p][j].multiply(vec.data[j]));
                }
                // 主元变量 = -sum / 主元元素
                Complex pivotVal = rref[p][col];
                // 修复：删除错误的复数除法代码，仅保留正确的divideComplex调用
                vec.data[col] = divideComplex(sum.multiply(-1), pivotVal);
            }
            basis.add(vec);
        }
        return basis;
    }

    /**
     * 复数矩阵行最简形变换（高斯消元）
     */
    private static Complex[][] reduceRowEchelonForm(Complex[][] A) {
        int n = A.length;
        Complex[][] mat = copyComplexMatrix(A);
        int row = 0;
        for (int col = 0; col < n && row < n; col++) {
            // 找主元行
            int pivotRow = row;
            for (int i = row; i < n; i++) {
                if (!mat[i][col].isZero()) {
                    pivotRow = i;
                    break;
                }
            }
            if (mat[pivotRow][col].isZero()) continue;
            // 交换主元行
            swapRows(mat, row, pivotRow);
            // 主元归一化
            Complex pivotVal = mat[row][col];
            for (int j = col; j < n; j++) {
                mat[row][j] = divideComplex(mat[row][j], pivotVal);
            }
            // 消去上下所有行
            for (int i = 0; i < n; i++) {
                if (i != row && !mat[i][col].isZero()) {
                    Complex factor = mat[i][col];
                    for (int j = col; j < n; j++) {
                        mat[i][j] = mat[i][j].subtract(factor.multiply(mat[row][j]));
                    }
                }
            }
            row++;
        }
        return mat;
    }

    // ====================== 高阶方阵：幂法（主特征值+对应特征向量） ======================
    /**
     * 幂法同步求解主特征值+对应特征向量
     */
    private static EigenPair powerMethodWithVector(double[][] matrix) {
        int n = matrix.length;
        double[] v = new double[n];
        for (int i = 0; i < n; i++) v[i] = 1.0; // 初始向量
        double lambdaPrev = 0.0;

        for (int iter = 0; iter < MAX_ITER; iter++) {
            double[] vNew = multiplyMatrixVector(matrix, v);
            double lambdaCurr = maxAbsElement(vNew);
            normalizeVector(vNew, lambdaCurr);
            // 收敛判断
            if (Math.abs(lambdaCurr - lambdaPrev) < CONVERGENCE) {
                // 转换为复数特征值+特征向量
                Complex lambda = new Complex(lambdaCurr, 0);
                List<ComplexVector> vectors = new ArrayList<>();
                Complex[] vecData = new Complex[n];
                for (int i = 0; i < n; i++) {
                    vecData[i] = new Complex(vNew[i], 0);
                }
                vectors.add(new ComplexVector(vecData));
                return new EigenPair(lambda, vectors);
            }
            lambdaPrev = lambdaCurr;
            v = vNew;
        }
        throw new IllegalArgumentException("幂法迭代" + MAX_ITER + "次未收敛，无法求解");
    }

    /**
     * 矩阵×实向量
     */
    private static double[] multiplyMatrixVector(double[][] A, double[] v) {
        int n = A.length;
        double[] res = new double[n];
        for (int i = 0; i < n; i++) {
            double sum = 0;
            for (int j = 0; j < n; j++) {
                sum += A[i][j] * v[j];
            }
            res[i] = sum;
        }
        return res;
    }

    /**
     * 求向量模最大元素
     */
    private static double maxAbsElement(double[] v) {
        double max = 0;
        for (double val : v) {
            if (Math.abs(val) > Math.abs(max)) max = val;
        }
        return max;
    }

    /**
     * 向量归一化
     */
    private static void normalizeVector(double[] v, double div) {
        if (Math.abs(div) < EPS) return;
        for (int i = 0; i < v.length; i++) v[i] /= div;
    }

    // ====================== 工具方法：复数/矩阵操作 ======================
    /**
     * 复数除法：a / b
     */
    private static Complex divideComplex(Complex a, Complex b) {
        if (b.isZero()) return new Complex(0, 0);
        double denominator = b.real * b.real + b.imag * b.imag;
        double real = (a.real * b.real + a.imag * b.imag) / denominator;
        double imag = (a.imag * b.real - a.real * b.imag) / denominator;
        return new Complex(real, imag);
    }

    /**
     * 复制复数矩阵
     */
    private static Complex[][] copyComplexMatrix(Complex[][] A) {
        int n = A.length;
        Complex[][] mat = new Complex[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(A[i], 0, mat[i], 0, n);
        }
        return mat;
    }

    /**
     * 交换复数矩阵的两行
     */
    private static void swapRows(Complex[][] A, int r1, int r2) {
        Complex[] temp = A[r1];
        A[r1] = A[r2];
        A[r2] = temp;
    }

    /**
     * 求解标准三次方程 x³ + px² + qx + r = 0
     */
    private static List<Complex> solveCubicEquation(double p, double q, double r) {
        List<Complex> roots = new ArrayList<>();
        double a = q - p*p/3;
        double b = r - p*q/3 + 2*Math.pow(p,3)/27;
        double delta = Math.pow(b/2, 2) + Math.pow(a/3, 3);

        if (Math.abs(delta) < EPS) {
            if (Math.abs(a) < EPS && Math.abs(b) < EPS) {
                roots.add(new Complex(-p/3, 0));
                roots.add(new Complex(-p/3, 0));
                roots.add(new Complex(-p/3, 0));
            } else {
                double y1 = 3*b/(-a);
                double y2 = -3*b/(2*a);
                roots.add(new Complex(y1 - p/3, 0));
                roots.add(new Complex(y2 - p/3, 0));
                roots.add(new Complex(y2 - p/3, 0));
            }
        } else if (delta > 0) {
            double u = Math.cbrt(-b/2 + Math.sqrt(delta));
            double v = Math.cbrt(-b/2 - Math.sqrt(delta));
            double y1 = u + v;
            double real = -(u + v)/2;
            double imag = (u - v)*Math.sqrt(3)/2;
            roots.add(new Complex(y1 - p/3, 0));
            roots.add(new Complex(real - p/3, imag));
            roots.add(new Complex(real - p/3, -imag));
        } else {
            double theta = Math.acos(Math.max(-1, Math.min(1, -(b/2) / Math.sqrt(-Math.pow(a/3, 3)))));
            double sqrtA = Math.sqrt(-a/3);
            double y1 = 2 * sqrtA * Math.cos(theta/3);
            double y2 = 2 * sqrtA * Math.cos((theta + 2*Math.PI)/3);
            double y3 = 2 * sqrtA * Math.cos((theta + 4*Math.PI)/3);
            roots.add(new Complex(y1 - p/3, 0));
            roots.add(new Complex(y2 - p/3, 0));
            roots.add(new Complex(y3 - p/3, 0));
        }
        return roots;
    }

    // ====================== 控制台输入/打印辅助方法 ======================
    /**
     * 读取n阶实方阵
     */
    public static double[][] readSquareMatrix(Scanner scanner, int n) {
        try {
            double[][] matrix = new double[n][n];
            System.out.println("请输入" + n + "阶方阵，每行" + n + "个数，用空格分隔（支持整数/小数）：");
            for (int i = 0; i < n; i++) {
                System.out.print("第" + (i + 1) + "行：");
                String[] elements = scanner.nextLine().trim().split("\\s+");
                if (elements.length != n) {
                    throw new IllegalArgumentException("第" + (i + 1) + "行应输入" + n + "个元素，实际输入" + elements.length + "个");
                }
                for (int j = 0; j < n; j++) {
                    matrix[i][j] = Double.parseDouble(elements[j]);
                }
            }
            return matrix;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("矩阵元素必须是有效数字（整数/小数）");
        }
    }

    /**
     * 打印实矩阵（保留4位小数）
     */
    public static void printDoubleMatrix(double[][] matrix) {
        for (double[] row : matrix) {
            for (double val : row) {
                System.out.printf("%.4f\t", val);
            }
            System.out.println();
        }
    }

    /**
     * 格式化复数输出
     */
    public static String formatComplex(Complex c) {
        double real = round(c.real, 4);
        double imag = round(c.imag, 4);
        if (Math.abs(imag) < EPS) return String.format("%.4f", real);
        if (Math.abs(real) < EPS) return String.format("%.4fi", imag);
        return imag > 0 ? String.format("%.4f + %.4fi", real, imag) : String.format("%.4f - %.4fi", real, -imag);
    }

    /**
     * 格式化复数向量输出
     */
    public static String formatComplexVector(ComplexVector v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.getDim(); i++) {
            sb.append(formatComplex(v.data[i]));
            if (i < v.getDim() - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 四舍五入保留指定小数位数
     */
    private static double round(double num, int decimals) {
        double scale = Math.pow(10, decimals);
        return Math.round(num * scale) / scale;
    }
}