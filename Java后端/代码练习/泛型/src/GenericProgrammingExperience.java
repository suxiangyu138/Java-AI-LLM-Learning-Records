import java.util.ArrayList;
import java.util.List;

/**
 * Java 泛型程序设计核心体验
 * 覆盖：泛型类、泛型方法、泛型接口、通配符、泛型限制、类型擦除
 */
public class GenericProgrammingExperience {
    public static void main(String[] args) {
        // ====================== 模块1：非泛型 vs 泛型（对比体验） ======================
        System.out.println("===== 模块1：非泛型 vs 泛型 =====");
        // 1.1 非泛型集合（类型不安全，需强制转换）
        List nonGenericList = new ArrayList();
        nonGenericList.add("Java");
        nonGenericList.add(100); // 可以添加任意类型，编译不报错
        // 取值时需强制转换，运行时可能抛ClassCastException
        String str1 = (String) nonGenericList.get(0); // 正常
        // String str2 = (String) nonGenericList.get(1); // 运行时异常：Integer不能转String
        System.out.println("非泛型集合取值：" + str1);

        // 1.2 泛型集合（类型安全，编译时检查）
        List<String> genericList = new ArrayList<>(); // 菱形语法（Java 7+）
        genericList.add("泛型");
        genericList.add("Java");
        // genericList.add(100); // 编译报错：不允许添加非String类型
        String str3 = genericList.get(0); // 无需强制转换
        System.out.println("泛型集合取值：" + str3);
        System.out.println();

        // ====================== 模块2：泛型类的使用 ======================
        System.out.println("===== 模块2：泛型类 =====");
        // 2.1 泛型类：指定具体类型（String）
        GenericClass<String> stringGeneric = new GenericClass<>("泛型类测试");
        System.out.println("泛型类（String）：" + stringGeneric.getData());
        stringGeneric.setData("修改后的值");
        System.out.println("修改后：" + stringGeneric.getData());

        // 2.2 泛型类：指定具体类型（Integer）
        GenericClass<Integer> intGeneric = new GenericClass<>(123456);
        System.out.println("泛型类（Integer）：" + intGeneric.getData());
        System.out.println();

        // ====================== 模块3：泛型方法的使用 ======================
        System.out.println("===== 模块3：泛型方法 =====");
        // 3.1 泛型方法：自动推断类型（String）
        String strResult = GenericMethodUtil.printAndReturn("泛型方法测试");
        System.out.println("泛型方法返回值：" + strResult);

        // 3.2 泛型方法：自动推断类型（Double）
        Double doubleResult = GenericMethodUtil.printAndReturn(3.14159);
        System.out.println("泛型方法返回值：" + doubleResult);

        // 3.3 带限制的泛型方法（只能传入Number子类）
        GenericMethodUtil.printNumber(100); // Integer（Number子类）
        GenericMethodUtil.printNumber(3.14); // Double（Number子类）
        // GenericMethodUtil.printNumber("100"); // 编译报错：String不是Number子类
        System.out.println();

        // ====================== 模块4：泛型接口的使用 ======================
        System.out.println("===== 模块4：泛型接口 =====");
        // 4.1 泛型接口：实现时指定具体类型（Integer）
        GenericInterfaceImpl1 impl1 = new GenericInterfaceImpl1();
        System.out.println("泛型接口（Integer）：" + impl1.process(999));

        // 4.2 泛型接口：实现时保留泛型参数
        GenericInterfaceImpl2<String> impl2 = new GenericInterfaceImpl2<>();
        System.out.println("泛型接口（String）：" + impl2.process("接口泛型测试"));
        System.out.println();

        // ====================== 模块5：泛型通配符 ======================
        System.out.println("===== 模块5：泛型通配符 =====");
        // 5.1 无界通配符（?）：可以接收任意泛型类型，但只能读不能写
        List<String> strList = new ArrayList<>();
        strList.add("A");
        strList.add("B");
        WildcardUtil.printAnyList(strList); // 传入String泛型列表

        List<Integer> intList = new ArrayList<>();
        intList.add(1);
        intList.add(2);
        WildcardUtil.printAnyList(intList); // 传入Integer泛型列表

        // 5.2 上界通配符（? extends T）：只能读取T及其子类，不能写入
        List<Apple> appleList = new ArrayList<>();
        appleList.add(new Apple("红富士"));
        WildcardUtil.printFruit(appleList); // Apple是Fruit子类

        List<Banana> bananaList = new ArrayList<>();
        bananaList.add(new Banana("小米蕉"));
        WildcardUtil.printFruit(bananaList); // Banana是Fruit子类

        // 5.3 下界通配符（? super T）：只能写入T及其子类，读取时为Object
        List<Fruit> fruitList = new ArrayList<>();
        WildcardUtil.addFruit(fruitList); // 写入Apple（Fruit子类）
        System.out.println("下界通配符添加元素后：" + fruitList.size());
    }
}

// -------------------------- 模块2：泛型类定义 --------------------------
/**
 * 泛型类定义：<T> 是类型参数（T是约定俗成的名称，可自定义）
 * 常用类型参数命名：
 * T - Type（类型）
 * E - Element（元素）
 * K - Key（键）
 * V - Value（值）
 * N - Number（数字）
 */
class GenericClass<T> {
    // 泛型属性
    private T data;

    // 泛型构造方法
    public GenericClass(T data) {
        this.data = data;
    }

    // 泛型get/set方法
    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}

// -------------------------- 模块3：泛型方法工具类 --------------------------
/**
 * 泛型方法定义：类型参数 <T> 放在返回值前
 */
class GenericMethodUtil {
    // 基础泛型方法：打印并返回传入的任意类型数据
    public static <T> T printAndReturn(T data) {
        System.out.println("泛型方法接收数据：" + data + "，类型：" + data.getClass().getSimpleName());
        return data;
    }

    // 带限制的泛型方法：只能传入Number及其子类（Integer/Double等）
    public static <T extends Number> void printNumber(T number) {
        System.out.println("数字类型数据：" + number + "，值：" + number.doubleValue());
    }
}

// -------------------------- 模块4：泛型接口定义 --------------------------
/**
 * 泛型接口定义
 */
interface GenericInterface<T> {
    T process(T input);
}

/**
 * 泛型接口实现：实现时指定具体类型（Integer）
 */
class GenericInterfaceImpl1 implements GenericInterface<Integer> {
    @Override
    public Integer process(Integer input) {
        return input * 2; // 处理逻辑：乘以2
    }
}

/**
 * 泛型接口实现：实现时保留泛型参数（使用时指定）
 */
class GenericInterfaceImpl2<T> implements GenericInterface<T> {
    @Override
    public T process(T input) {
        return input; // 处理逻辑：直接返回
    }
}

// -------------------------- 模块5：泛型通配符相关类 --------------------------
/**
 * 水果父类（用于通配符演示）
 */
class Fruit {
    private String name;

    public Fruit(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}

/**
 * 苹果（Fruit子类）
 */
class Apple extends Fruit {
    public Apple(String name) {
        super(name);
    }
}

/**
 * 香蕉（Fruit子类）
 */
class Banana extends Fruit {
    public Banana(String name) {
        super(name);
    }
}

/**
 * 通配符工具类
 */
class WildcardUtil {
    // 1. 无界通配符（?）：接收任意泛型列表，只能读不能写
    public static void printAnyList(List<?> list) {
        System.out.println("无界通配符遍历：");
        for (Object obj : list) {
            System.out.print(obj + " ");
        }
        System.out.println();
        // list.add("test"); // 编译报错：无界通配符不能添加元素（类型不确定）
    }

    // 2. 上界通配符（? extends Fruit）：只能读取Fruit及其子类，不能写入
    public static void printFruit(List<? extends Fruit> list) {
        System.out.println("上界通配符遍历水果：");
        for (Fruit fruit : list) {
            System.out.print(fruit + " ");
        }
        System.out.println();
        // list.add(new Apple("青苹果")); // 编译报错：上界通配符不能添加元素（不确定具体子类）
    }

    // 3. 下界通配符（? super Fruit）：只能写入Fruit及其子类，读取为Object
    public static void addFruit(List<? super Fruit> list) {
        list.add(new Apple("红富士")); // 可以添加Fruit子类
        list.add(new Banana("小米蕉"));
        // Object obj = list.get(0); // 读取时只能是Object类型
    }
}