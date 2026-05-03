import java.util.Scanner;

/**
 * 复数四则运算工具类（自动输出所有运算结果）
 * 功能：控制台输入两个复数 → 自动输出加、减、乘、除所有结果 → 支持循环运算
 * 核心：无需选择运算类型，批量输出所有结果，提升效率
 */
public class ComplexNumber {
    // 浮点数精度阈值
    private static final double EPS = 1e-8;

    private double real; // 实部
    private double imag; // 虚部

    // 构造方法
    public ComplexNumber(double real, double imag) {
        this.real = real;
        this.imag = imag;
    }

    // ====================== 核心四则运算 ======================
    /** 加法：(a+bi) + (c+di) = (a+c) + (b+d)i */
    public ComplexNumber add(ComplexNumber other) {
        return new ComplexNumber(this.real + other.real, this.imag + other.imag);
    }

    /** 减法：(a+bi) - (c+di) = (a-c) + (b-d)i */
    public ComplexNumber subtract(ComplexNumber other) {
        return new ComplexNumber(this.real - other.real, this.imag - other.imag);
    }

    /** 乘法：(a+bi)(c+di) = (ac-bd) + (ad+bc)i */
    public ComplexNumber multiply(ComplexNumber other) {
        double newReal = this.real * other.real - this.imag * other.imag;
        double newImag = this.real * other.imag + this.imag * other.real;
        return new ComplexNumber(newReal, newImag);
    }

    /** 除法：(a+bi)/(c+di) = [(ac+bd)+(bc-ad)i]/(c²+d²) */
    public ComplexNumber divide(ComplexNumber other) throws ArithmeticException {
        double denominator = other.real * other.real + other.imag * other.imag;
        if (Math.abs(denominator) < EPS) {
            throw new ArithmeticException("除数为0（复数模长为0），无法计算除法");
        }
        double newReal = (this.real * other.real + this.imag * other.imag) / denominator;
        double newImag = (this.imag * other.real - this.real * other.imag) / denominator;
        return new ComplexNumber(newReal, newImag);
    }

    // ====================== 辅助方法 ======================
    /** 计算复数模长 */
    public double modulus() {
        return Math.sqrt(this.real * this.real + this.imag * this.imag);
    }

    /** 格式化输出复数（符合数学习惯） */
    public String format() {
        double r = round(this.real, 4);
        double i = round(this.imag, 4);

        if (Math.abs(i) < EPS) return String.format("%.4f", r);
        if (Math.abs(r) < EPS) return String.format("%.4fi", i);
        return i > 0 ? String.format("%.4f + %.4fi", r, i) : String.format("%.4f - %.4fi", r, -i);
    }

    /** 四舍五入保留4位小数 */
    private double round(double num, int decimals) {
        double scale = Math.pow(10, decimals);
        return Math.round(num * scale) / scale;
    }

    // ====================== 控制台输入方法 ======================
    /** 控制台输入一个复数（带容错） */
    private static ComplexNumber inputComplex(Scanner scanner, int num) {
        System.out.println("\n===== 输入第" + num + "个复数 =====");
        double real = 0.0, imag = 0.0;

        // 输入实部（循环校验）
        while (true) {
            try {
                System.out.print("实部：");
                real = Double.parseDouble(scanner.nextLine().trim());
                break;
            } catch (NumberFormatException e) {
                System.out.println("❌ 输入错误！请输入数字（整数/小数）");
            }
        }

        // 输入虚部（循环校验）
        while (true) {
            try {
                System.out.print("虚部（i的系数）：");
                imag = Double.parseDouble(scanner.nextLine().trim());
                break;
            } catch (NumberFormatException e) {
                System.out.println("❌ 输入错误！请输入数字（整数/小数）");
            }
        }

        ComplexNumber complex = new ComplexNumber(real, imag);
        System.out.println("✅ 第" + num + "个复数：z" + num + " = " + complex.format());
        return complex;
    }

    /** 批量计算并输出所有四则运算结果 */
    private static void printAllResults(ComplexNumber z1, ComplexNumber z2) {
        System.out.println("\n==================== 运算结果 ====================");
        // 加法
        ComplexNumber addRes = z1.add(z2);
        System.out.printf("%s + %s = %s（模长：%.4f）\n",
                z1.format(), z2.format(), addRes.format(), addRes.modulus());

        // 减法
        ComplexNumber subRes = z1.subtract(z2);
        System.out.printf("%s - %s = %s（模长：%.4f）\n",
                z1.format(), z2.format(), subRes.format(), subRes.modulus());

        // 乘法
        ComplexNumber mulRes = z1.multiply(z2);
        System.out.printf("%s × %s = %s（模长：%.4f）\n",
                z1.format(), z2.format(), mulRes.format(), mulRes.modulus());

        // 除法（捕获除数为0异常）
        try {
            ComplexNumber divRes = z1.divide(z2);
            System.out.printf("%s ÷ %s = %s（模长：%.4f）\n",
                    z1.format(), z2.format(), divRes.format(), divRes.modulus());
        } catch (ArithmeticException e) {
            System.out.printf("%s ÷ %s = ❌ %s\n", z1.format(), z2.format(), e.getMessage());
        }
        System.out.println("==================================================");
    }

    // ====================== 主方法（程序入口） ======================
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("===== 复数四则运算计算器（自动输出所有结果） =====");

        while (true) {
            // 1. 输入两个复数
            ComplexNumber z1 = inputComplex(scanner, 1);
            ComplexNumber z2 = inputComplex(scanner, 2);

            // 2. 自动计算并输出所有运算结果
            printAllResults(z1, z2);

            // 3. 询问是否继续
            System.out.print("\n是否继续计算？(输入y继续，其他键退出)：");
            String flag = scanner.nextLine().trim().toLowerCase();
            if (!flag.equals("y")) {
                System.out.println("\n👋 程序已退出！");
                break;
            }
            System.out.println("\n--------------------- 重新开始 ---------------------");
        }

        scanner.close();
    }
}