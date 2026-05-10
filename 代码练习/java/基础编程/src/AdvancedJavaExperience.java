import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Java 接口、Lambda表达式、内部类 核心操作综合体验
 * 覆盖：接口定义/实现、函数式接口、Lambda简化、各类内部类
 */
public class AdvancedJavaExperience {
    public static void main(String[] args) {
        // ====================== 模块1：接口的基础使用 ======================
        System.out.println("===== 模块1：接口的基础使用 =====");
        // 1. 接口不能实例化，只能通过实现类创建对象
        AnimalInterface dog = new DogImpl();
        dog.eat();
        dog.move();
        // 2. 调用接口的默认方法
        dog.sleep();
        // 3. 调用接口的静态方法
        AnimalInterface.staticMethod();
        System.out.println();

        // ====================== 模块2：函数式接口 + Lambda表达式 ======================
        System.out.println("===== 模块2：函数式接口 + Lambda表达式 =====");
        // 2.1 匿名内部类实现函数式接口（Lambda的前身）
        CalculatorInterface calc1 = new CalculatorInterface() {
            @Override
            public int calculate(int a, int b) {
                return a + b;
            }
        };
        System.out.println("匿名内部类实现：10+20=" + calc1.calculate(10, 20));

        // 2.2 Lambda表达式简化（函数式接口专属）
        CalculatorInterface calc2 = (a, b) -> a + b; // 最简写法
        System.out.println("Lambda简化实现：10+20=" + calc2.calculate(10, 20));

        // 2.3 Lambda带多行逻辑
        CalculatorInterface calc3 = (a, b) -> {
            System.out.println("执行乘法逻辑");
            return a * b;
        };
        System.out.println("Lambda多行逻辑：10*20=" + calc3.calculate(10, 20));

        // 2.4 内置函数式接口（java.util.function）
        List<String> list = new ArrayList<>();
        list.add("Java");
        list.add("Lambda");
        list.add("接口");
        // forEach接收Consumer函数式接口，用Lambda简化
        list.forEach(str -> System.out.println("遍历元素：" + str));
        System.out.println();

        // ====================== 模块3：内部类（4种类型） ======================
        System.out.println("===== 模块3：内部类 =====");
        // 3.1 成员内部类（依赖外部类对象）
        OuterClass outer = new OuterClass();
        OuterClass.MemberInnerClass inner = outer.new MemberInnerClass();
        inner.innerMethod();

        // 3.2 静态内部类（不依赖外部类对象）
        OuterClass.StaticInnerClass staticInner = new OuterClass.StaticInnerClass();
        staticInner.staticInnerMethod();

        // 3.3 局部内部类（定义在方法内）
        outer.methodWithLocalInnerClass();

        // 3.4 匿名内部类（无类名，直接实现接口/继承类）
        // 对比：匿名内部类 vs Lambda（Lambda只能简化函数式接口）
        AnimalInterface cat = new AnimalInterface() {
            @Override
            public void eat() {
                System.out.println("匿名内部类：猫吃鱼");
            }

            @Override
            public void move() {
                System.out.println("匿名内部类：猫走猫步");
            }
        };
        cat.eat();
        cat.move();
    }
}

// -------------------------- 模块1：接口定义 --------------------------
/**
 * 普通接口示例
 * 特性：抽象方法、默认方法、静态方法
 */
interface AnimalInterface {
    // 1. 抽象方法（默认public abstract，可省略）
    void eat();
    void move();

    // 2. 默认方法（Java 8+）：有实现，子类可重写
    default void sleep() {
        System.out.println("动物睡觉（接口默认方法）");
    }

    // 3. 静态方法（Java 8+）：属于接口，不能被子类继承/重写
    static void staticMethod() {
        System.out.println("接口静态方法：动物的通用行为");
    }
}

/**
 * 接口实现类
 */
class DogImpl implements AnimalInterface {
    @Override
    public void eat() {
        System.out.println("狗吃骨头（实现接口抽象方法）");
    }

    @Override
    public void move() {
        System.out.println("狗跑（实现接口抽象方法）");
    }

    // 可选：重写接口默认方法
    @Override
    public void sleep() {
        System.out.println("狗趴着睡觉（重写接口默认方法）");
    }
}

// -------------------------- 模块2：函数式接口（Lambda适配） --------------------------
/**
 * 函数式接口：只有一个抽象方法（@FunctionalInterface注解校验）
 * Lambda表达式只能简化函数式接口的实现
 */
@FunctionalInterface
interface CalculatorInterface {
    int calculate(int a, int b);

    // 允许有默认方法/静态方法，不影响函数式接口特性
    default void printResult(int result) {
        System.out.println("计算结果：" + result);
    }
}

// -------------------------- 模块3：内部类（外部类） --------------------------
/**
 * 外部类：包含各类内部类
 */
class OuterClass {
    private String outerField = "外部类属性";
    private static String staticOuterField = "外部类静态属性";

    // 3.1 成员内部类（非静态内部类）
    class MemberInnerClass {
        private String innerField = "成员内部类属性";

        public void innerMethod() {
            // 可访问外部类的所有属性（包括private）
            System.out.println("成员内部类：");
            System.out.println("  访问外部类属性：" + outerField);
            System.out.println("  访问自身属性：" + innerField);
        }
    }

    // 3.2 静态内部类
    static class StaticInnerClass {
        private String staticInnerField = "静态内部类属性";

        public void staticInnerMethod() {
            // 只能访问外部类的静态属性
            System.out.println("\n静态内部类：");
            System.out.println("  访问外部类静态属性：" + staticOuterField);
            System.out.println("  访问自身属性：" + staticInnerField);
        }
    }

    // 3.3 局部内部类（定义在方法内）
    public void methodWithLocalInnerClass() {
        String localVar = "方法局部变量"; // 局部变量（隐式final）

        // 局部内部类：只能在当前方法内使用
        class LocalInnerClass {
            public void localMethod() {
                System.out.println("\n局部内部类：");
                System.out.println("  访问外部类属性：" + outerField);
                System.out.println("  访问方法局部变量：" + localVar);
            }
        }

        // 创建局部内部类对象并调用方法
        LocalInnerClass localInner = new LocalInnerClass();
        localInner.localMethod();
    }
}