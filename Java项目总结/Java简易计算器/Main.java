public class Main {
    static java.util.Scanner sc = new java.util.Scanner(System.in);

    public static void main(String[] args) {
        while (true) {
            try {
                double a = inputNumber("请输入第一个数：");
                char op = inputOperator("请输入运算符(+ - * /)：");
                double b = inputNumber("请输入第二个数：");

                double ans = calculate(a, b, op);
                System.out.println("结果为：" + ans);
            } catch (java.util.InputMismatchException e) {
                System.out.println("输入类型错误，请输入数字。");
                sc.nextLine();
            } catch (ArithmeticException e) {
                System.out.println("运算错误：" + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.out.println("输入错误：" + e.getMessage());
            }

            if (!isContinue()) {
                System.out.println("程序结束。");
                break;
            }
        }
        sc.close();
    }

    public static double inputNumber(String msg) {
        System.out.print(msg);
        return sc.nextDouble();
    }

    public static char inputOperator(String msg) {
        System.out.print(msg);
        char op = sc.next().charAt(0);
        if (op != '+' && op != '-' && op != '*' && op != '/') {
            throw new IllegalArgumentException("不支持该运算符");
        }
        return op;
    }

    public static double calculate(double a, double b, char op) {
        switch (op) {
            case '+':
                return a + b;
            case '-':
                return a - b;
            case '*':
                return a * b;
            case '/':
                if (b == 0) {
                    throw new ArithmeticException("除数不能为0");
                }
                return a / b;
            default:
                throw new IllegalArgumentException("运算符非法");
        }
    }

    public static boolean isContinue() {
        System.out.print("是否继续计算？(y/n)：");
        char ch = sc.next().charAt(0);
        return ch == 'y' || ch == 'Y';
    }
}