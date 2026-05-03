升级后的简易计算器建议实现这几个点：

- 支持多次计算，用户算完一轮后可以选择继续或退出，循环结构常用于这类控制台程序。 [stackoverflow](https://stackoverflow.com/questions/50672338/how-can-i-loop-this-switch-statement)
- 用多个方法拆分职责，例如输入数字、输入运算符、执行运算、判断是否继续，这样比所有代码堆在 `main` 里更规范。 [stackoverflow](https://stackoverflow.com/questions/22070535/java-try-catch-exception-calculator)
- 对异常做更细处理，`Scanner.nextDouble()` 在输入非数字时可能抛出输入不匹配异常，非法运算符和除零也应单独处理。 [geeksforgeeks](https://www.geeksforgeeks.org/java/scanner-nextdouble-method-in-java-with-examples/)

## 代码实现

按你的习惯，我给你简洁、可直接运行版：

```java
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
```

## 代码亮点

这版比基础版更完整，原因在于它已经具备一个小程序该有的基本结构。 [stackoverflow](https://stackoverflow.com/questions/31252001/using-a-loop-with-switch-case-statements-in-my-shape-calculator-java-program)

- `while(true)` 实现连续计算，输入结束选项后再退出，符合控制台交互程序的常见写法。 [tutorialspoint](https://www.tutorialspoint.com/java/java_continue_statement.htm)
- `inputNumber`、`inputOperator`、`calculate`、`isContinue` 分别负责输入数字、输入运算符、执行计算、判断是否继续，体现了方法封装思想。 [w3schools](https://www.w3schools.com/java/java_try_catch.asp)
- `try-catch` 统一处理异常，既能拦截非数字输入，也能处理除零和非法运算符问题。 [stackoverflow](https://stackoverflow.com/questions/17150627/scanner-double-value-inputmismatchexception)

## 适合交作业的总结

你可以直接把下面这段写进实验总结：

本程序使用 Java 实现了一个控制台版简易计算器，能够完成加、减、乘、除四种基本运算，并支持循环多次计算。 [programiz](https://www.programiz.com/java-programming/examples/calculator-switch-case)
在程序设计中，采用了方法封装思想，将数字输入、运算符输入、计算逻辑和继续判断分别写成独立方法，使主函数结构更加清晰，增强了代码的可读性与可维护性。 [stackoverflow](https://stackoverflow.com/questions/22070535/java-try-catch-exception-calculator)
同时，程序加入了异常处理机制，能够对输入类型错误、非法运算符以及除数为零等情况进行捕获和提示，从而提高了程序的健壮性和用户体验。 [geeksforgeeks](https://www.geeksforgeeks.org/java/scanner-nextdouble-method-in-java-with-examples/)
通过本次实现，我进一步掌握了 `Scanner` 输入、`switch` 分支结构、循环控制、异常捕获以及方法封装等 Java 基础知识。 [w3schools](https://www.w3schools.com/java/java_switch.asp)

## 你答辩时怎么说

你可以简洁地这样讲：

- 这个程序的核心是“循环 + 分支 + 异常处理 + 方法封装”。 [w3schools](https://www.w3schools.com/java/java_switch.asp)
- 循环负责多次计算，`switch` 负责判断运算类型，方法封装负责模块化，异常处理负责增强健壮性。 [w3schools](https://www.w3schools.com/java/java_try_catch.asp)
- 升级后程序不再只是一次性计算，而是更接近一个完整的小型控制台应用。 [stackoverflow](https://stackoverflow.com/questions/31252001/using-a-loop-with-switch-case-statements-in-my-shape-calculator-java-program)

如果你要，我下一条可以直接继续给你整理成一份 **实验报告完整版**：  
