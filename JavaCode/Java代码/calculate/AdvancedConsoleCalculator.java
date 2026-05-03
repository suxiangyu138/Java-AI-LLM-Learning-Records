import java.util.Scanner;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.RombergIntegrator;
import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.analysis.differentiation.UnivariateDifferentiableFunction;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularMatrixException;

public class AdvancedConsoleCalculator {

    // 定义常用科学常数
    private static final double PI = Math.PI;
    private static final double E = Math.E;
    private static final double G = 9.81;           // 重力加速度 (m/s²)
    private static final double C = 299792458;     // 光速 (m/s)
    private static final double H = 6.62607015e-34;// 普朗克常数 (J·s)
    private static final double KB = 1.380649e-23; // 玻尔兹曼常数 (J/K)
    private static final double NA = 6.02214076e23;// 阿伏伽德罗常数 (mol⁻¹)

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== 高级大学控制台计算器 ===");
        System.out.println("支持：基础计算、微积分、矩阵运算、科学常数");
        System.out.println("输入 'help' 查看命令列表，输入 'exit' 退出");

        while (true) {
            System.out.print("\n> ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit")) {
                System.out.println("再见！");
                break;
            } else if (input.equalsIgnoreCase("help")) {
                printHelp();
            } else if (input.startsWith("const ")) {
                showConstant(input.substring(6).trim());
            } else if (input.startsWith("diff ")) {
                calculateDerivative(input.substring(5).trim());
            } else if (input.startsWith("int ")) {
                calculateIntegral(input.substring(4).trim());
            } else if (input.startsWith("matrix ")) {
                handleMatrixCommand(input.substring(7).trim(), scanner);
            } else {
                // 基础表达式计算
                calculateBasic(input);
            }
        }
        scanner.close();
    }

    // 帮助信息
    private static void printHelp() {
        System.out.println("\n=== 命令列表 ===");
        System.out.println("1. 直接输入表达式进行基础计算，例如：2+3*sin(PI/2)");
        System.out.println("2. const <常数名> 查看科学常数，例如：const G");
        System.out.println("3. diff <函数> at <x> 计算导数，例如：diff x^2 at 2");
        System.out.println("4. int <函数> from <a> to <b> 计算定积分，例如：int sin(x) from 0 to PI");
        System.out.println("5. matrix det <n> 计算n阶矩阵行列式，例如：matrix det 2");
        System.out.println("6. matrix inv <n> 计算n阶矩阵逆矩阵，例如：matrix inv 2");
        System.out.println("7. matrix trans <n> 计算n阶矩阵转置，例如：matrix trans 2");
        System.out.println("8. help 显示此帮助信息");
        System.out.println("9. exit 退出计算器");
        System.out.println("\n可用常数：PI, E, G(重力), C(光速), H(普朗克), KB(玻尔兹曼), NA(阿伏伽德罗)");
    }

    // 显示科学常数
    private static void showConstant(String name) {
        switch (name.toUpperCase()) {
            case "PI": System.out.println("π = " + PI); break;
            case "E": System.out.println("e = " + E); break;
            case "G": System.out.println("重力加速度 G = " + G + " m/s²"); break;
            case "C": System.out.println("光速 c = " + C + " m/s"); break;
            case "H": System.out.println("普朗克常数 h = " + H + " J·s"); break;
            case "KB": System.out.println("玻尔兹曼常数 k_B = " + KB + " J/K"); break;
            case "NA": System.out.println("阿伏伽德罗常数 N_A = " + NA + " mol⁻¹"); break;
            default: System.out.println("未知常数：" + name);
        }
    }

    // 计算导数
    private static void calculateDerivative(String input) {
        try {
            String[] parts = input.split(" at ");
            String funcExpr = parts[0].trim();
            double x = Double.parseDouble(parts[1].trim());

            UnivariateDifferentiableFunction func = createFunction(funcExpr);
            DerivativeStructure ds = new DerivativeStructure(1, 1, 0, x);
            DerivativeStructure result = func.value(ds);
            System.out.println("导数在 x=" + x + " 处的值为：" + result.getPartialDerivative(1));
        } catch (Exception e) {
            System.out.println("导数计算错误：" + e.getMessage());
        }
    }

    // 计算定积分
    private static void calculateIntegral(String input) {
        try {
            String[] parts = input.split(" from | to ");
            String funcExpr = parts[0].trim();
            double a = Double.parseDouble(parts[1].trim());
            double b = Double.parseDouble(parts[2].trim());

            UnivariateFunction func = createBasicFunction(funcExpr);
            RombergIntegrator integrator = new RombergIntegrator();
            double result = integrator.integrate(10000, func, a, b);
            System.out.println("积分从 " + a + " 到 " + b + " 的结果为：" + result);
        } catch (Exception e) {
            System.out.println("积分计算错误：" + e.getMessage());
        }
    }

    // 处理矩阵命令
    private static void handleMatrixCommand(String command, Scanner scanner) {
        try {
            String[] parts = command.split(" ");
            String op = parts[0];
            int n = Integer.parseInt(parts[1]);

            System.out.println("请输入 " + n + "×" + n + " 矩阵（每行输入" + n + "个数字，空格分隔）：");
            double[][] data = new double[n][n];
            for (int i = 0; i < n; i++) {
                System.out.print("第 " + (i+1) + " 行：");
                String[] row = scanner.nextLine().trim().split(" ");
                for (int j = 0; j < n; j++) {
                    data[i][j] = Double.parseDouble(row[j]);
                }
            }

            RealMatrix matrix = MatrixUtils.createRealMatrix(data);
            switch (op) {
                case "det":
                    double det = matrix.getDeterminant();
                    System.out.println("行列式值：" + det);
                    break;
                case "inv":
                    try {
                        RealMatrix inv = matrix.inverse();
                        System.out.println("逆矩阵：");
                        printMatrix(inv);
                    } catch (SingularMatrixException e) {
                        System.out.println("矩阵不可逆（行列式为0）");
                    }
                    break;
                case "trans":
                    RealMatrix trans = matrix.transpose();
                    System.out.println("转置矩阵：");
                    printMatrix(trans);
                    break;
                default:
                    System.out.println("未知矩阵操作：" + op);
            }
        } catch (Exception e) {
            System.out.println("矩阵操作错误：" + e.getMessage());
        }
    }

    // 打印矩阵
    private static void printMatrix(RealMatrix matrix) {
        double[][] data = matrix.getData();
        for (double[] row : data) {
            System.out.print("[ ");
            for (double val : row) {
                System.out.printf("%.2f ", val);
            }
            System.out.println("]");
        }
    }

    // 基础表达式计算
    private static void calculateBasic(String input) {
        try {
            // 替换表达式中的常数
            String expr = input.replace("PI", String.valueOf(PI))
                    .replace("E", String.valueOf(E))
                    .replace("G", String.valueOf(G))
                    .replace("C", String.valueOf(C))
                    .replace("H", String.valueOf(H))
                    .replace("KB", String.valueOf(KB))
                    .replace("NA", String.valueOf(NA))
                    .replace("^", "**");

            // 使用JavaScript引擎计算
            double result = (double) new javax.script.ScriptEngineManager()
                    .getEngineByName("JavaScript")
                    .eval(expr);
            System.out.println("结果：" + result);
        } catch (Exception e) {
            System.out.println("表达式错误：" + e.getMessage());
        }
    }

    // 创建可微分函数
    private static UnivariateDifferentiableFunction createFunction(String expr) {
        return x -> {
            String funcExpr = expr.replace("x", String.valueOf(x))
                    .replace("PI", String.valueOf(PI))
                    .replace("E", String.valueOf(E));
            try {
                return (double) new javax.script.ScriptEngineManager()
                        .getEngineByName("JavaScript")
                        .eval(funcExpr);
            } catch (Exception e) {
                throw new RuntimeException("函数解析错误：" + e.getMessage());
            }
        };
    }

    // 创建基础函数
    private static UnivariateFunction createBasicFunction(String expr) {
        return x -> {
            String funcExpr = expr.replace("x", String.valueOf(x))
                    .replace("PI", String.valueOf(PI))
                    .replace("E", String.valueOf(E));
            try {
                return (double) new javax.script.ScriptEngineManager()
                        .getEngineByName("JavaScript")
                        .eval(funcExpr);
            } catch (Exception e) {
                throw new RuntimeException("函数解析错误：" + e.getMessage());
            }
        };
    }
}
