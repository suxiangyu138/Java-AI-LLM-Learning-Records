import java.util.Scanner;
import java.util.Arrays;
import java.util.ArrayList;




    /**
     * Java 核心基础知识点综合体验
     * 覆盖：数据类型、变量与常量、运算符、字符串、输入输出、数据操作
     * 每个模块都有详细注释，运行后可直观看到效果
     */
    public class JavaBasicExperience {
        // 1. 常量定义（类级常量，全大写+下划线分隔）
        public static final double PI = 3.1415926; // 圆周率常量
        public static final String GREETING = "欢迎学习Java基础！"; // 字符串常量

        public static void main(String[] args) {
            // ====================== 模块1：数据类型 ======================
            System.out.println("===== 模块1：数据类型 =====");
            // 基本数据类型（8种）
            byte byteVar = 127; // 字节型（-128~127）
            short shortVar = 32767; // 短整型（-32768~32767）
            int intVar = 2147483647; // 整型（默认整数类型，-2^31~2^31-1）
            long longVar = 9223372036854775807L; // 长整型，后缀L
            float floatVar = 3.14F; // 单精度浮点型，后缀F
            double doubleVar = 3.1415926; // 双精度浮点型（默认浮点类型）
            char charVar = 'A'; // 字符型（单个字符，单引号）
            boolean boolVar = true; // 布尔型（true/false）

            // 引用数据类型（字符串、数组、对象等）
            String strVar = "Java基础"; // 字符串（引用类型，双引号）
            int[] arrayVar = {1, 2, 3}; // 数组（引用类型）

            // 打印各数据类型的值和类型
            System.out.println("byte类型：" + byteVar + "，类型：" + ((Object) byteVar).getClass().getSimpleName());
            System.out.println("long类型：" + longVar + "，类型：" + ((Object) longVar).getClass().getSimpleName());
            System.out.println("float类型：" + floatVar + "，类型：" + ((Object) floatVar).getClass().getSimpleName());
            System.out.println("char类型：" + charVar + "，ASCII码：" + (int) charVar); // 字符转ASCII
            System.out.println("boolean类型：" + boolVar);
            System.out.println("String类型：" + strVar + "，类型：" + strVar.getClass().getSimpleName());
            System.out.println("数组类型：" + Arrays.toString(arrayVar) + "\n");

            // ====================== 模块2：变量与常量 ======================
            System.out.println("===== 模块2：变量与常量 =====");
            // 变量：可修改
            int age = 20;
            System.out.println("初始年龄：" + age);
            age = 21; // 修改变量值
            System.out.println("修改后年龄：" + age);

            // 常量：不可修改（final修饰）
            final int MAX_SCORE = 100; // 局部常量
            System.out.println("常量PI：" + PI); // 类级常量
            System.out.println("常量GREETING：" + GREETING);
            System.out.println("局部常量MAX_SCORE：" + MAX_SCORE + "\n");
            // MAX_SCORE = 99; // 报错：常量不能重新赋值

            // ====================== 模块3：运算符 ======================
            System.out.println("===== 模块3：运算符 =====");
            int a = 10, b = 3;

            // 算术运算符
            System.out.println("算术运算：");
            System.out.println(a + " + " + b + " = " + (a + b)); // 加法
            System.out.println(a + " - " + b + " = " + (a - b)); // 减法
            System.out.println(a + " * " + b + " = " + (a * b)); // 乘法
            System.out.println(a + " / " + b + " = " + (a / b)); // 除法（整数除法取整）
            System.out.println(a + " % " + b + " = " + (a % b)); // 取模（余数）
            System.out.println("a++ = " + (a++) + "，自增后a=" + a); // 后置自增
            System.out.println("++a = " + (++a) + "，自增后a=" + a); // 前置自增

            // 赋值运算符
            int c = a;
            c += b; // 等价于c = c + b
            System.out.println("c += b 后c=" + c);

            // 比较运算符（返回boolean）
            System.out.println("比较运算：");
            System.out.println(a + " > " + b + " = " + (a > b));
            System.out.println(a + " == " + (13) + " = " + (a == 13));
            System.out.println(a + " != " + b + " = " + (a != b));

            // 逻辑运算符
            boolean cond1 = (a > b), cond2 = (b < 5);
            System.out.println("逻辑运算：");
            System.out.println(cond1 + " && " + cond2 + " = " + (cond1 && cond2)); // 与（都真才真）
            System.out.println(cond1 + " || " + false + " = " + (cond1 || false)); // 或（有真就真）
            System.out.println("!cond1 = " + (!cond1)); // 非（取反）
            System.out.println();

            // ====================== 模块4：字符串操作 ======================
            System.out.println("===== 模块4：字符串操作 =====");
            String str1 = "Hello";
            String str2 = "Java";
            String str3 = "hello java";

            // 拼接
            String concatStr = str1 + " " + str2;
            System.out.println("字符串拼接：" + concatStr);

            // 长度、截取、替换
            System.out.println("字符串长度：" + concatStr.length());
            System.out.println("截取前3个字符：" + concatStr.substring(0, 3)); // [0,3)
            System.out.println("替换Java为World：" + concatStr.replace("Java", "World"));

            // 大小写转换、去除空格
            String spaceStr = "  Java Basic  ";
            System.out.println("去除空格：'" + spaceStr.trim() + "'");
            System.out.println("转小写：" + concatStr.toLowerCase());
            System.out.println("转大写：" + concatStr.toUpperCase());

            // 比较（== 比较地址，equals比较内容）
            String str4 = new String("Hello Java");
            System.out.println("== 比较：" + (concatStr == str4)); // false（地址不同）
            System.out.println("equals比较：" + concatStr.equals(str4)); // true（内容相同）
            System.out.println("忽略大小写比较：" + concatStr.equalsIgnoreCase(str3)); // true
            System.out.println();

            // ====================== 模块5：输入输出 ======================
            System.out.println("===== 模块5：输入输出 =====");
            Scanner scanner = new Scanner(System.in); // 创建Scanner对象读取输入

            // 输出（三种方式）
            System.out.print("print输出（不换行）：");
            System.out.println("println输出（换行）");
            System.out.printf("格式化输出：圆周率=%.2f，年龄=%d%n", PI, age); // 格式化输出

            // 输入（交互式）
            System.out.print("请输入你的姓名：");
            String name = scanner.nextLine(); // 读取字符串
            System.out.print("请输入你的年龄：");
            int inputAge = scanner.nextInt(); // 读取整数

            System.out.println("你好，" + name + "！你今年" + inputAge + "岁，明年" + (inputAge + 1) + "岁。");
            System.out.println();

            // ====================== 模块6：数据操作 ======================
            System.out.println("===== 模块6：数据操作 =====");
            // 类型转换
            System.out.println("类型转换：");
            // 自动转换（小范围→大范围）
            int numInt = 100;
            double numDouble = numInt; // int→double 自动转换
            System.out.println("int→double自动转换：" + numInt + " → " + numDouble);

            // 强制转换（大范围→小范围，需加()）
            double numD = 3.99;
            int numI = (int) numD; // double→int 强制转换（丢失小数）
            System.out.println("double→int强制转换：" + numD + " → " + numI);

            // 数组操作
            int[] nums = {5, 2, 9, 1, 5, 6};
            System.out.println("原数组：" + Arrays.toString(nums));
            Arrays.sort(nums); // 数组排序
            System.out.println("排序后数组：" + Arrays.toString(nums));
            System.out.println("数组索引2的值：" + nums[2]);

            // 集合操作（简单体验）
            ArrayList<String> list = new ArrayList<>();
            list.add("Java");
            list.add("Python");
            list.add("C++");
            System.out.println("集合元素：" + list);
            list.remove(1); // 删除索引1的元素
            System.out.println("删除后集合：" + list);
            System.out.println("集合是否包含Java：" + list.contains("Java"));

            // 关闭Scanner
            scanner.close();
        }
    }

