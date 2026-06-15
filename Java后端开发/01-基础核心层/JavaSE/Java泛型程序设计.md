Java泛型程序设计

Java泛型（Generics）是JDK 5引入的核心特性，本质是参数化类型，允许在定义类、接口、方法时，不指定具体的数据类型，而是通过参数占位符（如T、E、K、V）表示，在使用时再指定具体类型。泛型的核心价值是“类型安全”和“代码复用”，既能避免强制类型转换带来的错误，又能编写通用的代码适配多种数据类型，是Java面向对象编程中不可或缺的重要技术，也是面试高频考点。本文将全面梳理泛型的核心知识点、语法规则、实战用法及常见误区，帮你快速掌握并灵活运用泛型。

一、泛型的核心定义与作用
1. 核心定义
    泛型，即“参数化类型”，通俗理解：将数据类型作为“参数”传递给类、接口或方法，在声明时不固定具体类型，在创建对象或调用方法时，再指定具体的类型。例如，定义一个“容器类”，不固定存储String、Integer还是其他类型，而是用一个占位符T表示，使用时再指定T为String或Integer，实现“一个类适配多种类型”。
    泛型的本质是“编译期语法糖”，在编译阶段对类型进行检查，确保类型安全，编译后会进行“类型擦除”（将泛型参数替换为Object或其上限类型），不会增加运行时的开销。
2. 核心作用（为什么需要泛型）
    类型安全：编译期检查数据类型，避免将错误类型的数据存入容器（如避免将String存入Integer类型的集合），减少ClassCastException（类型转换异常），让错误在编译期暴露，而非运行时崩溃；
    代码复用：编写通用代码，无需为每种数据类型单独定义类、接口或方法（如一个泛型集合，可存储任意类型数据，无需分别定义String集合、Integer集合）；
    简化代码：避免频繁的强制类型转换，泛型会自动进行类型适配，代码更简洁、可读性更高；
    提升扩展性：泛型代码可灵活适配多种数据类型，后续新增类型时，无需修改原有通用代码，符合“开闭原则”。
3. 泛型出现前的问题（对比理解）
    在JDK 5之前，没有泛型，通常使用Object类（所有类的父类）实现通用代码，但存在两个核心问题：
    类型不安全：可以将任意类型的数据存入容器，编译期不报错，运行时进行类型转换时，可能抛出ClassCastException；
    代码繁琐：每次取出数据时，必须手动进行强制类型转换，代码冗余且易出错。
    // 泛型出现前，用Object实现通用容器
    class Container {
    private Object obj;
    // 存入数据
    public void setObj(Object obj) {
        this.obj = obj;
    }
    // 取出数据，需强制类型转换
    public Object getObj() {
        return obj;
    }
    }
    public class NoGenericTest {
    public static void main(String[] args) {
        Container container = new Container();
        container.setObj(100); // 存入Integer类型
        // 取出时必须强制转换为Integer，若误转换为String，运行时抛出异常
        Integer num = (Integer) container.getObj(); 
        container.setObj("hello"); // 存入String类型
        String str = (String) container.getObj();
    }
    }
    上述代码中，Container可以存入任意类型数据，若存入Integer后，误将其转换为String，运行时会抛出ClassCastException；而泛型可以彻底解决这个问题，在编译期就限制数据类型。
    二、泛型的基本语法与规范
    1. 泛型参数占位符（约定俗成）
    泛型使用“类型参数”作为占位符，通常使用单个大写字母表示，Java中有约定俗成的命名规范（便于阅读，非强制）：
    T（Type）：表示任意类型（最常用，如泛型类、泛型方法）；
    E（Element）：表示集合中的元素类型（常用于集合框架，如List&lt;E&gt;）；
    K（Key）：表示键值对中的键类型（常用于Map<K, V>）；
    V（Value）：表示键值对中的值类型（常用于Map<K, V>）；
    S、U、V：表示多个不同的类型参数（如泛型方法中需要多个类型参数时使用）。
    注意：泛型参数不能是基本数据类型（如int、double、boolean），只能是引用数据类型（如Integer、String、自定义类）；若需要使用基本数据类型，需使用其对应的包装类（如int→Integer、double→Double）。
2. 泛型的核心语法格式
    泛型的使用主要分为三类：泛型类、泛型接口、泛型方法，三者语法各有侧重，但核心逻辑一致（参数化类型）。
    // 1. 泛型类语法
    class 类名<类型参数1, 类型参数2,...> {
    // 可以使用类型参数定义属性、方法参数、返回值
    private 类型参数1 变量名;
    public 类型参数1 方法名(类型参数2 参数名) {
        return 变量名;
    }
    }
    // 2. 泛型接口语法
    interface 接口名<类型参数1, 类型参数2,...> {
    类型参数1 方法名(类型参数2 参数名);
    }
    // 3. 泛型方法语法（注意：泛型方法的类型参数定义在方法返回值前）
    public <类型参数1, 类型参数2,...> 类型参数1 方法名(类型参数2 参数名) {
    // 方法实现
    }
    三、泛型类与泛型接口（重点）
    泛型类和泛型接口是泛型最常用的场景，定义时指定类型参数，使用时（创建对象、实现接口）指定具体类型，实现代码复用和类型安全。
    1. 泛型类（最常用）
    泛型类是在类声明时引入类型参数，类中的属性、方法可以使用该类型参数，创建泛型类对象时，必须指定具体的类型参数。
    实战案例：自定义泛型容器类
    // 泛型类：Container<T>，T表示任意引用类型
    class Container<T> {
    // 使用泛型参数T定义属性
    private T data;
    // 构造方法，参数类型为T
    public Container(T data) {
        this.data = data;
    }
    // 方法返回值为T，参数类型为T
    public T getData() {
        return data;
    }
    public void setData(T data) {
        this.data = data;
    }
    // 泛型类中的普通方法，使用T类型
    public void showType() {
        System.out.println("当前容器存储的类型：" + data.getClass().getName());
    }
    }
    // 测试泛型类
    public class GenericClassTest {
    public static void main(String[] args) {
        // 1. 创建泛型类对象，指定T为Integer类型
        Container<Integer> intContainer = new Container<>(100); // JDK 7后可省略右侧类型，简化为菱形语法
        Integer intData = intContainer.getData(); // 无需强制类型转换
        intContainer.showType(); // 输出：当前容器存储的类型：java.lang.Integer
        // 2. 创建泛型类对象，指定T为String类型
        Container<String> strContainer = new Container<>("hello");
        String strData = strContainer.getData();
        strContainer.showType(); // 输出：当前容器存储的类型：java.lang.String
        // 3. 错误示例：不能存入与指定类型不符的数据（编译期报错）
        // intContainer.setData("hello"); // 编译报错，类型不匹配
    }
    }
    关键说明：
    创建泛型类对象时，必须指定具体类型（如<Integer>），JDK 7及以上支持“菱形语法”（new Container<>(100)），右侧可省略类型参数；
    泛型类的类型参数一旦指定，类中的所有T都会被替换为该具体类型，编译期会检查类型，避免存入错误类型数据；
    不同类型参数的泛型类对象，属于不同的类型（如Container<Integer>和Container<String>是两个不同的类型，不能相互赋值）。
2. 泛型接口
    泛型接口与泛型类语法类似，在接口声明时引入类型参数，实现接口时，需指定具体的类型参数（或继续保留泛型参数，成为泛型实现类）。
    实战案例：自定义泛型接口
    // 泛型接口：Generator<T>，用于生成指定类型的数据
    interface Generator<T> {
    T generate(); // 抽象方法，返回值为T类型
    }
    // 方式1：实现泛型接口，指定具体类型（T为String）
    class StringGenerator implements Generator<String> {
    @Override
    public String generate() {
        // 返回String类型数据
        return "随机字符串：" + Math.random();
    }
    }
    // 方式2：实现泛型接口，保留泛型参数（成为泛型实现类）
    class NumberGenerator<T extends Number> implements Generator<T> {
    @Override
    public T generate() {
        // 简化实现，返回Integer类型（Integer是Number的子类）
        return (T) Integer.valueOf((int) (Math.random() * 100));
    }
    }
    // 测试泛型接口
    public class GenericInterfaceTest {
    public static void main(String[] args) {
        // 测试方式1：具体类型实现
        Generator<String> strGen = new StringGenerator();
        System.out.println(strGen.generate());
        // 测试方式2：泛型实现类，指定T为Integer
        Generator<Integer> intGen = new NumberGenerator<>();
        System.out.println(intGen.generate());
        // 测试方式2：指定T为Double
        Generator<Double> doubleGen = new NumberGenerator<>();
        System.out.println(doubleGen.generate());
    }
    }
    四、泛型方法（重点）
    泛型方法是指在方法声明时引入类型参数，与泛型类/接口的区别是：泛型方法的类型参数独立于类的类型参数（即使类不是泛型类，也可以定义泛型方法），核心是“方法级别的通用”。
    关键语法：泛型方法的类型参数必须定义在“返回值类型前”，格式为<T> 返回值类型 方法名(参数列表)。
    1. 泛型方法的基本使用
    public class GenericMethodTest {
    // 泛型方法：printData，T为类型参数，打印任意类型的数据
    public <T> void printData(T data) {
        System.out.println("数据类型：" + data.getClass().getName() + "，数据值：" + data);
    }
    // 泛型方法：getMax，返回两个同类型数据中的最大值（需实现Comparable接口）
    public <T extends Comparable<T>> T getMax(T a, T b) {
        return a.compareTo(b) >= 0 ? a : b;
    }
    public static void main(String[] args) {
        GenericMethodTest test = new GenericMethodTest();
        // 调用泛型方法，自动推断类型（无需手动指定T）
        test.printData(100); // 自动推断T为Integer
        test.printData("hello"); // 自动推断T为String
        test.printData(3.14); // 自动推断T为Double
        // 调用泛型方法，手动指定T类型（可选）
        test.<Integer>printData(200);
        // 测试getMax方法
        Integer maxInt = test.getMax(10, 20);
        String maxStr = test.getMax("apple", "banana");
        System.out.println("最大整数：" + maxInt); // 输出：20
        System.out.println("最大字符串：" + maxStr); // 输出：banana
    }
    }
2. 泛型方法的核心特点
    类型推断：调用泛型方法时，JVM会根据传入的参数类型，自动推断泛型参数T的具体类型，无需手动指定（手动指定也可）；
    独立于泛型类：即使所在的类不是泛型类，也可以定义泛型方法（如上述案例中，GenericMethodTest不是泛型类，但包含泛型方法）；
    可定义多个类型参数：如<T, U> void method(T t, U u)，适配多种不同类型的参数。
    五、泛型通配符（难点+高频）
    泛型通配符（Wildcard）用于“不确定泛型参数的具体类型”的场景，核心是“灵活匹配多种泛型类型”，常用的通配符有三种：无界通配符（?）、上界通配符（? extends 类型）、下界通配符（? super 类型）。
    1. 无界通配符（?）
    无界通配符?表示“任意类型”，相当于? extends Object，适用于“仅需要读取数据，不需要修改数据”的场景（如打印任意类型的泛型容器数据）。
    // 无界通配符示例：打印任意类型的Container
    public class WildcardTest {
    // 无界通配符：? 表示任意类型
    public void printContainer(Container<?> container) {
        // 仅读取数据，可正常使用
        Object data = container.getData();
        System.out.println("容器数据：" + data);
    }
    public static void main(String[] args) {
        Container<Integer> intContainer = new Container<>(100);
        Container<String> strContainer = new Container<>("hello");
        WildcardTest test = new WildcardTest();
        // 无界通配符可匹配任意类型的Container
        test.printContainer(intContainer);
        test.printContainer(strContainer);
        // 注意：无界通配符的容器，不能修改数据（编译报错）
        // container.setData(200); // 编译报错，无法确定具体类型，避免存入错误数据
    }
    }
2. 上界通配符（? extends 类型）
    上界通配符? extends T表示“泛型参数必须是T类型或T的子类”，核心是“限制泛型类型的上限”，适用于“读取数据，且数据类型有继承关系”的场景（如操作所有Number类型的子类容器）。
    // 上界通配符示例：操作Number及其子类（Integer、Double等）的Container
    public class UpperBoundTest {
    // 上界通配符：? extends Number，表示泛型参数是Number或其子类
    public void printNumberContainer(Container<? extends Number> container) {
        // 读取数据，可转换为Number类型
        Number data = container.getData();
        System.out.println("数字容器数据：" + data);
    }
    public static void main(String[] args) {
        Container<Integer> intContainer = new Container<>(100); // Integer是Number子类
        Container<Double> doubleContainer = new Container<>(3.14); // Double是Number子类
        Container<String> strContainer = new Container<>("hello"); // String不是Number子类
        UpperBoundTest test = new UpperBoundTest();
        test.printNumberContainer(intContainer); // 合法
        test.printNumberContainer(doubleContainer); // 合法
        // test.printNumberContainer(strContainer); // 编译报错，String不是Number的子类
        // 注意：上界通配符的容器，也不能修改数据（编译报错）
        // container.setData(200); // 无法确定具体是Number的哪个子类，避免存入错误类型
    }
    }
3. 下界通配符（? super 类型）
    下界通配符? super T表示“泛型参数必须是T类型或T的父类”，核心是“限制泛型类型的下限”，适用于“修改数据，且数据类型有继承关系”的场景（如向容器中添加T类型或其子类的数据）。
    // 下界通配符示例：向容器中添加Integer及其子类（Integer、Byte等）的数据
    public class LowerBoundTest {
    // 下界通配符：? super Integer，表示泛型参数是Integer或其父类（如Number、Object）
    public void addInteger(Container<? super Integer> container, Integer data) {
        // 可修改数据，添加Integer类型数据（或其子类）
        container.setData(data);
    }
    public static void main(String[] args) {
        Container<Integer> intContainer = new Container<>(100); // Integer是自身
        Container<Number> numberContainer = new Container<>(0); // Number是Integer父类
        Container<String> strContainer = new Container<>("hello"); // String不是Integer父类
        LowerBoundTest test = new LowerBoundTest();
        test.addInteger(intContainer, 200); // 合法
        test.addInteger(numberContainer, 300); // 合法
        // test.addInteger(strContainer, 400); // 编译报错，String不是Integer的父类
        // 注意：下界通配符的容器，读取数据时只能转换为Object类型
        Object data1 = intContainer.getData();
        Object data2 = numberContainer.getData();
    }
    }
4. 通配符使用总结（避坑）
    无界通配符（?）：任意类型，仅读取，不修改；
    上界通配符（? extends T）：T及其子类，仅读取（可转换为T类型），不修改；
    下界通配符（? super T）：T及其父类，可修改（添加T及其子类数据），读取时仅能转换为Object；
    核心原则：“PECS”原则——Producer Extends，Consumer Super（生产者用extends，消费者用super）： - 生产者（仅读取数据）：用? extends T，如List<? extends Number>，只能读取数据，不能添加； - 消费者（仅修改数据）：用? super T，如List<? super Integer>，只能添加T及其子类数据，读取时为Object。
    六、泛型的类型擦除（核心原理）
    Java泛型是“编译期语法糖”，编译后会进行类型擦除：即把泛型参数替换为其上限类型（若未指定上限，替换为Object），运行时JVM不知道泛型的存在，仅保留原始类型。
    1. 类型擦除的具体表现
    // 泛型类
    class Container<T> {
    private T data;
    public T getData() { return data; }
    }
    // 编译后，类型擦除为：
    class Container {
    private Object data;
    public Object getData() { return data; }
    }
    // 带上限的泛型类
    class NumberContainer<T extends Number> {
    private T data;
    public T getData() { return data; }
    }
    // 编译后，类型擦除为：
    class NumberContainer {
    private Number data;
    public Number getData() { return data; }
    }
2. 类型擦除带来的影响（避坑）
    不能用泛型参数创建对象：如new T()，编译报错，因为类型擦除后T变为Object，且JVM无法确定具体类型；
    不能用泛型参数作为数组的类型：如T[] arr = new T[10]，编译报错，可改用Object[] arr = new Object[10]，再强制转换；
    泛型参数不能是基本数据类型：因为类型擦除后会替换为Object，而基本数据类型不是Object的子类，需用包装类；
    不能在catch块中使用泛型参数：如catch (T e)，编译报错，因为类型擦除后T的类型不确定，无法捕获具体异常；
    泛型类的静态方法不能使用类的泛型参数：因为静态方法属于类，而泛型参数属于对象，类加载时泛型参数未确定。
    七、泛型的常见问题与避坑指南
    问题1：泛型参数不能是基本数据类型？ 解决：使用对应的包装类（int→Integer、double→Double、boolean→Boolean）；
    问题2：泛型类的静态方法不能使用类的泛型参数？ 解决：将静态方法定义为泛型方法（独立的类型参数），如public static <T> void method(T t)；
    问题3：无法创建泛型参数的对象（new T()）？ 解决：通过反射创建对象（如Class<T> clazz.newInstance()），或传入具体类型的对象；
    问题4：泛型数组创建报错（T[] arr = new T[10]）？ 解决：创建Object数组，再强制转换为泛型数组（T[] arr = (T[]) new Object[10]），注意可能出现unchecked警告；
    问题5：不同泛型类型的对象不能相互赋值（如Container<Integer>不能赋值给Container<String>）？ 说明：这是类型安全的设计，避免存入错误类型数据，若需灵活匹配，使用泛型通配符；
    问题6：泛型通配符的容器无法修改数据？ 说明：上界通配符（? extends T）和无界通配符（?）仅支持读取，不支持修改；下界通配符（? super T）支持修改（添加T及其子类数据）。
    八、泛型实战案例（综合应用）
    结合泛型类、泛型方法、通配符，实现一个通用的集合工具类，包含添加、删除、查找、排序等功能，适配任意类型的集合。
    import java.util.ArrayList;
    import java.util.List;
    // 泛型集合工具类
    public class GenericCollectionUtil {
    // 泛型方法：向集合中添加元素（支持任意类型集合）
    public static <T> void addElement(List<T> list, T element) {
        list.add(element);
        System.out.println("添加元素：" + element);
    }
    // 泛型方法：从集合中查找元素，返回索引（支持任意类型集合）
    public static <T> int findElement(List<T> list, T element) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equals(element)) {
                return i;
            }
        }
        return -1; // 未找到
    }
    // 泛型方法：排序（上界通配符，仅支持实现Comparable接口的类型）
    public static <T extends Comparable<T>> void sortList(List<T> list) {
        int n = list.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - 1 - i; j++) {
                if (list.get(j).compareTo(list.get(j + 1)) > 0) {
                    // 交换元素
                    T temp = list.get(j);
                    list.set(j, list.get(j + 1));
                    list.set(j + 1, temp);
                }
            }
        }
        System.out.println("排序后集合：" + list);
    }
    // 泛型方法：打印集合（无界通配符，支持任意类型集合）
    public static void printList(List<?> list) {
        System.out.print("集合元素：");
        for (Object obj : list) {
            System.out.print(obj + " ");
        }
        System.out.println();
    }
    // 测试
    public static void main(String[] args) {
        // 测试Integer类型集合
        List<Integer> intList = new ArrayList<>();
        addElement(intList, 10);
        addElement(intList, 5);
        addElement(intList, 15);
        printList(intList); // 输出：集合元素：10 5 15
        sortList(intList); // 输出：排序后集合：[5, 10, 15]
        System.out.println("元素10的索引：" + findElement(intList, 10)); // 输出：1
        // 测试String类型集合
        List<String> strList = new ArrayList<>();
        addElement(strList, "apple");
        addElement(strList, "banana");
        addElement(strList, "orange");
        printList(strList); // 输出：集合元素：apple banana orange
        sortList(strList); // 输出：排序后集合：[apple, banana, orange]
    }
    }
    九、面试高频考点（必背）
    1. Java泛型的核心作用是什么？
    核心作用有两个：① 类型安全：编译期检查数据类型，避免类型转换异常；② 代码复用：编写通用代码，适配多种数据类型，无需重复定义。
2. 泛型的类型擦除是什么？有什么影响？
    类型擦除：Java泛型是编译期语法糖，编译后会将泛型参数替换为其上限类型（未指定上限则为Object），运行时JVM不感知泛型。
    影响：不能用泛型参数创建对象、不能创建泛型数组、泛型参数不能是基本数据类型、静态方法不能使用类的泛型参数。
3. 泛型通配符有哪几种？各自的作用是什么？
    三种：① 无界通配符（?）：匹配任意类型，仅读取，不修改；② 上界通配符（? extends T）：匹配T及其子类，仅读取，适用于生产者场景；③ 下界通配符（? super T）：匹配T及其父类，可修改（添加T及其子类），适用于消费者场景。
4. 泛型类和泛型方法的区别是什么？
    ① 泛型类的类型参数定义在类上，作用于整个类；泛型方法的类型参数定义在方法上，仅作用于当前方法；② 泛型方法可独立于泛型类存在（非泛型类也可定义泛型方法）；③ 调用泛型方法时会自动推断类型，泛型类创建对象时必须指定类型。
5. 为什么泛型参数不能是基本数据类型？
    因为泛型编译后会进行类型擦除，泛型参数会被替换为其上限类型（默认是Object），而基本数据类型（int、double等）不是Object的子类，无法被替换，因此必须使用对应的包装类（Integer、Double等）。
    
    十、总结
    1. 泛型是JDK 5引入的参数化类型特性，核心是“类型安全”和“代码复用”，本质是编译期语法糖，编译后会进行类型擦除；
    2. 泛型的核心用法：泛型类（类级别的通用）、泛型接口（接口级别的通用）、泛型方法（方法级别的通用），三者语法各有侧重，但逻辑一致；
    3. 泛型通配符是难点，需掌握无界、上界、下界通配符的用法，遵循“PECS”原则（生产者extends，消费者super）；
    4. 实战中需规避常见误区：泛型参数不能是基本数据类型、不能创建泛型对象和泛型数组、静态方法不能使用类的泛型参数；
    5. 泛型广泛应用于Java集合框架（如List<E>、Map<K, V>），是企业开发中编写通用代码的核心工具，也是面试高频考点，需结合实战案例理解记忆。
