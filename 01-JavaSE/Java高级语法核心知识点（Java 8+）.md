Java高级语法核心知识点（Java 8+）
Java高级语法是在基础语法之上，用于解决复杂开发场景、提升代码效率和可维护性的核心内容，重点围绕面向对象进阶、Lambda表达式、Stream流、反射、注解、泛型等模块展开，以下是系统梳理的核心知识点，搭配简洁案例辅助理解。
一、面向对象进阶（基础延伸，核心重点）
1. 抽象类（abstract class）
    抽象类是无法实例化的类，用于定义共性模板，包含抽象方法（无方法体）和非抽象方法，子类必须重写所有抽象方法（除非子类也是抽象类）。
    // 抽象类
    abstract class Animal {
    // 非抽象方法（有方法体）
    public void eat() {
        System.out.println("动物需要进食");
    }
    // 抽象方法（无方法体，必须用abstract修饰）
    public abstract void move();
    }
    // 子类继承抽象类，必须重写抽象方法
    class Dog extends Animal {
    @Override
    public void move() {
        System.out.println("狗用四肢奔跑");
    }
    }
    // 错误：抽象类无法实例化 → Animal animal = new Animal();
    核心要点：抽象类不能实例化；可以包含构造方法（供子类调用）；抽象方法必须在子类中重写；抽象类可以有普通属性和方法。
2. 接口（interface）
    接口是一种特殊的“抽象类”，仅定义方法规范（无方法体），不包含具体实现，Java 8后允许定义默认方法（default修饰，有方法体）和静态方法（static修饰）。
    // 接口（用interface修饰）
    interface Flyable {
    // 抽象方法（默认public abstract，可省略）
    void fly();
    // Java 8+ 默认方法（有方法体，子类可重写，也可直接使用）
    default void stop() {
        System.out.println("停止飞行");
    }
    // Java 8+ 静态方法（有方法体，只能通过接口名调用）
    static void showRule() {
        System.out.println("飞行需遵守安全规则");
    }
    }
    // 类实现接口（implements），必须重写抽象方法
    class Bird implements Flyable {
    @Override
    public void fly() {
        System.out.println("鸟扇动翅膀飞行");
    }
    }
    // 调用静态方法
    Flyable.showRule();
    核心要点：接口不能实例化；类可以实现多个接口（解决Java单继承限制）；接口中的成员变量默认是public static final（常量）；默认方法和静态方法是Java 8的重要更新，提升接口的灵活性。
3. 多态（Polymorphism）
    多态是面向对象三大特性之一，指“同一方法调用，不同对象有不同实现”，核心是“父类引用指向子类对象”，需满足3个条件：继承、重写、父类引用指向子类对象。
    // 父类
    class Animal {
    public void shout() {
        System.out.println("动物发出叫声");
    }
    }
    // 子类1
    class Cat extends Animal {
    @Override
    public void shout() {
        System.out.println("猫喵喵叫");
    }
    }
    // 子类2
    class Dog extends Animal {
    @Override
    public void shout() {
        System.out.println("狗汪汪叫");
    }
    }
    // 测试多态
    public class Test {
    public static void main(String[] args) {
        Animal animal1 = new Cat(); // 父类引用指向Cat对象
        Animal animal2 = new Dog(); // 父类引用指向Dog对象
        animal1.shout(); // 输出：猫喵喵叫（调用Cat的shout方法）
        animal2.shout(); // 输出：狗汪汪叫（调用Dog的shout方法）
    }
    }
    核心要点：多态通过“重写”实现；父类引用只能调用父类中定义的方法（子类特有方法需强制转换）；多态提升代码的扩展性和灵活性。
4. 封装进阶（访问修饰符+单例模式）
    （1）访问修饰符（权限控制）
    Java提供4种访问修饰符，控制类、属性、方法的访问权限，从大到小依次为：public > protected > 默认（无修饰符）> private。
    修饰符
    本类
    同包
    子类（不同包）
    其他包
    public
    可访问
    可访问
    可访问
    可访问
    protected
    可访问
    可访问
    可访问
    不可访问
    默认（无修饰）
    可访问
    可访问
    不可访问
    不可访问
    private
    可访问
    不可访问
    不可访问
    不可访问
    （2）单例模式（封装的典型应用）
    单例模式是一种设计模式，确保一个类只有一个实例，且提供全局唯一的访问方式，常用两种实现方式：饿汉式、懒汉式（线程安全版）。
    // 1. 饿汉式（线程安全，类加载时初始化实例）
    class SingletonHungry {
    // 私有静态实例（类加载时创建）
    private static final SingletonHungry instance = new SingletonHungry();
    // 私有构造方法（禁止外部实例化）
    private SingletonHungry() {}
    // 公共静态方法，返回实例
    public static SingletonHungry getInstance() {
        return instance;
    }
    }
    // 2. 懒汉式（线程安全，延迟初始化，推荐用双重检查锁）
    class SingletonLazy {
    // 私有静态实例（volatile防止指令重排）
    private static volatile SingletonLazy instance;
    // 私有构造方法
    private SingletonLazy() {}
    // 双重检查锁，确保线程安全且高效
    public static SingletonLazy getInstance() {
        if (instance == null) {
            synchronized (SingletonLazy.class) {
                if (instance == null) {
                    instance = new SingletonLazy();
                }
            }
        }
        return instance;
    }
    }
    二、Java 8+ 新特性（核心重点）
    1. Lambda表达式（简化代码）
    Lambda表达式是“函数式编程”的核心，用于简化匿名内部类的写法，适用于函数式接口（只有一个抽象方法的接口）。
    语法格式：(参数列表) -> { 方法体 }（参数列表无参数写()，单参数可省略()；方法体单条语句可省略{}和return）。
    // 函数式接口（只有一个抽象方法）
    interface Calculator {
    int calculate(int a, int b);
    }
    public class TestLambda {
    public static void main(String[] args) {
        // 传统匿名内部类写法
        Calculator add1 = new Calculator() {
            @Override
            public int calculate(int a, int b) {
                return a + b;
            }
        };
        // Lambda表达式简化（参数列表+方法体）
        Calculator add2 = (a, b) -> a + b; // 单条语句，省略{}和return
        Calculator sub = (a, b) -> { return a - b; }; // 多条语句，需加{}和return
        // 调用
        System.out.println(add2.calculate(3, 5)); // 输出：8
        System.out.println(sub.calculate(10, 4)); // 输出：6
    }
    }
    核心要点：Lambda表达式依赖函数式接口；简化匿名内部类，提升代码简洁度；Java 8提供的java.util.function包包含常用函数式接口（如Consumer、Supplier、Predicate）。
2. Stream流（集合/数组操作神器）
    Stream流是Java 8用于处理集合、数组的高效工具，通过“流式操作”实现过滤、映射、排序、聚合等功能，代码简洁且可链式调用，分为中间操作（返回Stream）和终止操作（返回最终结果）。
    import java.util.Arrays;
    import java.util.List;
    import java.util.stream.Collectors;
    public class TestStream {
    public static void main(String[] args) {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8);
        // 链式操作：过滤（偶数）→ 乘以2 → 排序 → 收集为List
        List<Integer> result = list.stream()
                .filter(num -> num % 2 == 0) // 中间操作：过滤偶数
                .map(num -> num * 2)        // 中间操作：每个元素乘以2
                .sorted()                   // 中间操作：排序
                .collect(Collectors.toList()); // 终止操作：收集结果
        System.out.println(result); // 输出：[4, 8, 12, 16]
        // 其他常用操作：统计、求和、遍历
        long count = list.stream().filter(num -> num > 5).count(); // 统计大于5的元素个数
        int sum = list.stream().filter(num -> num % 2 == 1).mapToInt(Integer::intValue).sum(); // 奇数求和
        System.out.println(count); // 输出：3
        System.out.println(sum);   // 输出：16
    }
    }
    核心要点：Stream流不改变原集合/数组；中间操作可链式调用，只有执行终止操作时才会执行所有中间操作（惰性求值）；常用中间操作：filter、map、sorted；常用终止操作：collect、count、forEach、sum。
3. 方法引用（Lambda简化）
    方法引用是Lambda表达式的进一步简化，当Lambda表达式的方法体只是调用一个已存在的方法时，可使用方法引用替代，格式：类名::方法名 或 对象::方法名。
    import java.util.Arrays;
    import java.util.List;
    import java.util.stream.Collectors;
    public class TestMethodReference {
    public static void main(String[] args) {
        List<String> list = Arrays.asList("apple", "banana", "orange");
        // Lambda表达式：将字符串转为大写
        List<String> upper1 = list.stream().map(s -> s.toUpperCase()).collect(Collectors.toList());
        // 方法引用简化（String类的toUpperCase方法）
        List<String> upper2 = list.stream().map(String::toUpperCase).collect(Collectors.toList());
        System.out.println(upper2); // 输出：[APPLE, BANANA, ORANGE]
    }
    }
    常用方法引用类型：对象::实例方法、类名::静态方法、类名::实例方法（如String::equals）、构造器引用（类名::new）。
4. Optional（解决空指针异常）
    Optional是Java 8提供的容器类，用于包装可能为null的对象，避免空指针异常（NullPointerException），提供一系列方法安全操作对象。
    import java.util.Optional;
    public class TestOptional {
    public static void main(String[] args) {
        // 1. 创建Optional对象
        Optional<String> optional1 = Optional.of("hello"); // 不允许null
        Optional<String> optional2 = Optional.ofNullable(null); // 允许null
        Optional<String> optional3 = Optional.empty(); // 空Optional
        // 2. 安全获取值（推荐）
        String value1 = optional1.orElse("默认值"); // 有值返回值，无值返回默认值
        String value2 = optional2.orElseGet(() -> "默认值2"); // 无值时执行Lambda获取默认值
        // 3. 过滤和映射
        optional1.filter(s -> s.length() > 3) // 过滤字符串长度>3
                .ifPresent(s -> System.out.println(s)); // 有值则执行操作
        // 4. 避免空指针
        String str = null;
        // 传统方式：if (str != null) { ... }
        // Optional方式：安全调用
        Optional.ofNullable(str).ifPresent(s -> System.out.println(s.length()));
    }
    }
    核心要点：Optional不推荐用get()方法（无值时抛异常）；推荐用orElse、orElseGet、ifPresent等方法安全操作；避免嵌套Optional，保持代码简洁。
    三、反射（框架核心，动态操作类）
    1. 反射概述
    反射是Java的核心特性之一，允许程序在运行时动态获取类的信息（类名、属性、方法、构造器），并动态调用类的属性和方法，是Spring、MyBatis等框架的底层实现原理。
    获取Class对象的3种方式（Class是反射的核心类）：
    // 方式1：通过类名.class（编译期确定，最安全）
    Class<?> clazz1 = User.class;
    // 方式2：通过对象.getClass()（运行时确定）
    User user = new User();
    Class<?> clazz2 = user.getClass();
    // 方式3：通过Class.forName("全类名")（动态加载，最灵活）
    Class<?> clazz3 = Class.forName("com.demo.entity.User");
2. 反射核心操作（获取属性、方法、构造器）
    import java.lang.reflect.Constructor;
    import java.lang.reflect.Field;
    import java.lang.reflect.Method;
    class User {
    private String username;
    public Integer age;
    // 无参构造
    public User() {}
    // 有参构造
    public User(String username, Integer age) {
        this.username = username;
        this.age = age;
    }
    // 公共方法
    public void show() {
        System.out.println("username: " + username + ", age: " + age);
    }
    // 私有方法
    private void sayHello(String name) {
        System.out.println("Hello, " + name);
    }
    }
    public class TestReflection {
    public static void main(String[] args) throws Exception {
        // 1. 获取Class对象
        Class<?> clazz = User.class;
        // 2. 获取构造器，创建对象
        // 无参构造创建对象
        Constructor<?> constructor1 = clazz.getConstructor();
        User user1 = (User) constructor1.newInstance();
        // 有参构造创建对象
        Constructor<?> constructor2 = clazz.getConstructor(String.class, Integer.class);
        User user2 = (User) constructor2.newInstance("zhangsan", 20);
        // 3. 获取属性并操作
        // 获取公共属性
        Field ageField = clazz.getField("age");
        ageField.set(user2, 22); // 设置属性值
        System.out.println(ageField.get(user2)); // 获取属性值（输出：22）
        // 获取私有属性（需设置可访问）
        Field usernameField = clazz.getDeclaredField("username");
        usernameField.setAccessible(true); // 打破封装，允许访问私有属性
        usernameField.set(user2, "lisi");
        System.out.println(usernameField.get(user2)); // 输出：lisi
        // 4. 获取方法并调用
        // 调用公共方法
        Method showMethod = clazz.getMethod("show");
        showMethod.invoke(user2); // 输出：username: lisi, age: 22
        // 调用私有方法（需设置可访问）
        Method sayHelloMethod = clazz.getDeclaredMethod("sayHello", String.class);
        sayHelloMethod.setAccessible(true);
        sayHelloMethod.invoke(user2, "Java"); // 输出：Hello, Java
    }
    }
    核心要点：反射可以打破封装（setAccessible(true)），访问私有属性和方法；反射操作会牺牲部分性能，尽量避免频繁使用；反射是框架的核心，理解反射能更好地掌握框架原理。
    四、注解（Annotation）
    1. 注解概述
    注解是Java 5引入的特性，用于给类、方法、属性等添加“元数据”（描述数据的数据），本身不直接影响代码执行，但可以通过反射获取注解信息，实现自定义逻辑（如参数校验、权限控制）。
    常用内置注解：
    @Override：标识方法重写，编译器会校验重写规则；
    @Deprecated：标识方法/类已过时，编译器会提示警告；
    @SuppressWarnings("all")：抑制编译器警告；
    @FunctionalInterface：标识函数式接口（只有一个抽象方法）。
2. 自定义注解（核心）
    自定义注解通过@interface关键字定义，可搭配“元注解”（描述注解的注解）控制注解的使用范围和生命周期。
    import java.lang.annotation.*;
    // 元注解：控制注解的使用范围（类、方法、属性等）
    @Target({ElementType.METHOD, ElementType.FIELD})
    // 元注解：控制注解的生命周期（RUNTIME表示运行时保留，可通过反射获取）
    @Retention(RetentionPolicy.RUNTIME)
    // 自定义注解
    @interface MyAnnotation {
    // 注解属性（默认值可选）
    String value() default "默认值";
    int age() default 18;
    }
    // 使用自定义注解
    class Student {
    @MyAnnotation(value = "张三", age = 20)
    private String name;
    @MyAnnotation("学习Java")
    public void study() {
        System.out.println("正在学习Java");
    }
    }
    // 通过反射获取注解信息
    public class TestAnnotation {
    public static void main(String[] args) throws Exception {
        Class<?> clazz = Student.class;
        // 获取属性上的注解
        Field nameField = clazz.getDeclaredField("name");
        MyAnnotation annotation1 = nameField.getAnnotation(MyAnnotation.class);
        System.out.println(annotation1.value()); // 输出：张三
        System.out.println(annotation1.age()); // 输出：20
        // 获取方法上的注解
        Method studyMethod = clazz.getMethod("study");
        MyAnnotation annotation2 = studyMethod.getAnnotation(MyAnnotation.class);
        System.out.println(annotation2.value()); // 输出：学习Java
    }
    }
    核心要点：元注解@Target和@Retention是自定义注解的必备；注解属性的类型只能是基本类型、String、枚举、注解、数组；通过反射获取注解信息，实现自定义逻辑（如参数校验、日志记录）。
    五、泛型（Generic）
    1. 泛型概述
    泛型是Java 5引入的特性，用于“参数化类型”，将类型作为参数传递，避免类型转换异常，提升代码的通用性和安全性，常见于集合、自定义工具类。
    核心作用：编译期类型检查，避免运行时类型转换异常；代码复用，无需为不同类型编写重复代码。
2. 泛型的使用（类、方法、接口）
    // 1. 泛型类（将类型作为类的参数）
    class GenericClass<T> { // T是类型参数，可自定义（如E、K、V）
    private T data;
    public T getData() {
        return data;
    }
    public void setData(T data) {
        this.data = data;
    }
    }
    // 2. 泛型方法（将类型作为方法的参数）
    class GenericMethod {
    public <T> T getValue(T value) {
        return value;
    }
    }
    // 3. 泛型接口
    interface GenericInterface<T> {
    T getResult();
    }
    // 实现泛型接口
    class GenericImpl implements GenericInterface<String> {
    @Override
    public String getResult() {
        return "泛型接口实现";
    }
    }
    // 测试泛型
    public class TestGeneric {
    public static void main(String[] args) {
        // 泛型类使用（指定类型为String）
        GenericClass<String> strClass = new GenericClass<>();
        strClass.setData("Java");
        String str = strClass.getData(); // 无需类型转换
        // 泛型类使用（指定类型为Integer）
        GenericClass<Integer> intClass = new GenericClass<>();
        intClass.setData(100);
        Integer num = intClass.getData();
        // 泛型方法使用
        GenericMethod method = new GenericMethod();
        String str2 = method.getValue("Hello");
        Integer num2 = method.getValue(200);
    }
    }
3. 泛型通配符（?）
    泛型通配符用于表示“任意类型”，解决泛型类型不兼容的问题，分为无界通配符（?）、上界通配符（? extends 父类）、下界通配符（? super 子类）。
    import java.util.ArrayList;
    import java.util.List;
    public class TestWildcard {
    public static void main(String[] args) {
        List<String> strList = new ArrayList<>();
        strList.add("Java");
        List<Integer> intList = new ArrayList<>();
        intList.add(100);
        // 无界通配符（?）：可接收任意类型的List
        printList(strList);
        printList(intList);
        // 上界通配符（? extends Number）：只能接收Number及其子类（Integer、Double等）
        printNumberList(intList);
        // 下界通配符（? super Integer）：只能接收Integer及其父类（Number、Object等）
        addNumber(intList);
    }
    // 无界通配符
    public static void printList(List<?> list) {
        for (Object obj : list) {
            System.out.println(obj);
        }
    }
    // 上界通配符
    public static void printNumberList(List<? extends Number> list) {
        for (Number num : list) {
            System.out.println(num);
        }
    }
    // 下界通配符
    public static void addNumber(List<? super Integer> list) {
        list.add(200); // 可添加Integer及其子类
    }
    }
    核心要点：泛型是编译期特性，运行时会“擦除”类型信息（即运行时泛型类型会变为Object）；泛型通配符解决泛型类型的灵活适配问题，避免代码冗余。
    六、异常处理进阶
    1. 异常体系
    Java异常分为两大类：Checked异常（编译期异常，必须处理）和Unchecked异常（运行时异常，可选择处理）。
    Checked异常：继承自Exception（非RuntimeException），如IOException、SQLException，编译期必须捕获或抛出；
    Unchecked异常：继承自RuntimeException，如NullPointerException、ArrayIndexOutOfBoundsException，运行时抛出，无需强制处理。
2. 异常处理方式（try-catch-finally + throws）
    import java.io.FileInputStream;
    import java.io.FileNotFoundException;
    import java.io.IOException;
    public class TestException {
    // throws：抛出异常，由调用者处理
    public static void readFile(String path) throws FileNotFoundException, IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(path);
            // 读取文件操作
        } catch (FileNotFoundException e) {
            // 捕获文件不存在异常
            System.out.println("文件不存在：" + e.getMessage());
            throw e; // 重新抛出异常，让调用者进一步处理
        } finally {
            // finally：无论是否发生异常，都会执行（用于释放资源）
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    // Java 7+ try-with-resources（自动释放资源，无需手动close）
    public static void readFile2(String path) throws FileNotFoundException, IOException {
        // 实现AutoCloseable接口的类，会自动释放资源
        try (FileInputStream fis = new FileInputStream(path)) {
            // 读取文件操作
        }
    }
    public static void main(String[] args) {
        try {
            readFile("test.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    }
3. 自定义异常
    自定义异常用于描述业务场景中的特殊异常（如用户不存在、参数非法），继承自Exception（Checked异常）或RuntimeException（Unchecked异常）。
    // 自定义运行时异常（Unchecked异常）
    class UserNotFoundException extends RuntimeException {
    // 无参构造
    public UserNotFoundException() {}
    // 带消息的构造
    public UserNotFoundException(String message) {
        super(message);
    }
    }
    // 测试自定义异常
    public class TestCustomException {
    public static void findUser(Long id) {
        if (id == null || id <= 0) {
            // 抛出自定义异常
            throw new UserNotFoundException("用户ID非法：" + id);
        }
        // 模拟查询用户
        boolean userExists = false;
        if (!userExists) {
            throw new UserNotFoundException("用户不存在，ID：" + id);
        }
    }
    public static void main(String[] args) {
        try {
            findUser(0);
        } catch (UserNotFoundException e) {
            System.out.println("异常信息：" + e.getMessage()); // 输出：用户ID非法：0
        }
    }
    }
    核心要点：自定义异常需根据业务场景选择继承Exception或RuntimeException；异常信息要清晰，便于问题定位；避免过度捕获异常，导致异常被“吞噬”。
    七、其他核心高级知识点
    1. 多线程基础（入门）
    Java多线程允许程序同时执行多个任务，核心是Thread类和Runnable接口，Java 8后可通过Lambda表达式简化线程创建。
    // 方式1：继承Thread类
    class MyThread extends Thread {
    @Override
    public void run() {
        for (int i = 0; i < 5; i++) {
            System.out.println("Thread1: " + i);
            try {
                Thread.sleep(100); // 休眠100毫秒
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    }
    // 方式2：实现Runnable接口（推荐，避免单继承限制）
    class MyRunnable implements Runnable {
    @Override
    public void run() {
        for (int i = 0; i < 5; i++) {
            System.out.println("Thread2: " + i);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    }
    public class TestThread {
    public static void main(String[] args) {
        // 启动线程
        new MyThread().start();
        new Thread(new MyRunnable()).start();
        // Lambda表达式简化（Runnable是函数式接口）
        new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                System.out.println("Thread3: " + i);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    }
2. 枚举（Enum）
    枚举是Java 5引入的类型，用于定义固定数量的常量（如性别、状态），枚举类是final类，不能继承，可包含属性、方法。
    // 枚举类
    enum Gender {
    // 枚举常量（必须放在最前面）
    MALE("男"), FEMALE("女");
    // 枚举属性
    private String desc;
    // 枚举构造方法（必须是private）
    private Gender(String desc) {
        this.desc = desc;
    }
    // 枚举方法
    public String getDesc() {
        return desc;
    }
    }
    public class TestEnum {
    public static void main(String[] args) {
        // 使用枚举常量
        Gender gender = Gender.MALE;
        System.out.println(gender); // 输出：MALE
        System.out.println(gender.getDesc()); // 输出：男
        // 遍历所有枚举常量
        for (Gender g : Gender.values()) {
            System.out.println(g + ": " + g.getDesc());
        }
    }
    }
