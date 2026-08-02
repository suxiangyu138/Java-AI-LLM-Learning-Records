# 19 - Lambda表达式与方法引用

> 定位：掌握Lambda表达式的语法演变、函数式接口的核心概念、Java内置四大函数式接口、方法引用的四种形式，理解invokedynamic底层实现

## 目录

1. [Lambda表达式的诞生背景](#1-lambda表达式的诞生背景)
2. [Lambda语法详解](#2-lambda语法详解)
3. [函数式接口（@FunctionalInterface）](#3-函数式接口functionalinterface)
4. [Java内置四大函数式接口](#4-java内置四大函数式接口)
5. [Lambda vs 匿名内部类](#5-lambda-vs-匿名内部类)
6. [变量捕获（effectively final）](#6-变量捕获effectively-final)
7. [方法引用的四种形式](#7-方法引用的四种形式)
8. [构造器引用](#8-构造器引用)
9. [Lambda底层实现：invokedynamic](#9-lambda底层实现invokedynamic)
10. [函数式编程思想](#10-函数式编程思想)
11. [Lambda实战应用场景](#11-lambda实战应用场景)
12. [常见陷阱与最佳实践](#12-常见陷阱与最佳实践)
13. [面试高频考点](#13-面试高频考点)

---

## 1. Lambda表达式的诞生背景

### 1.1 从匿名内部类到Lambda的演进

Java 8之前，行为传递只能通过匿名内部类实现，代码冗长。Lambda表达式正是为解决这一问题而生。

**三段式演进——同一功能三种写法**：

```java
// 阶段一：匿名内部类
Comparator<String> c1 = new Comparator<String>() {
    @Override
    public int compare(String a, String b) {
        return Integer.compare(a.length(), b.length());
    }
};

// 阶段二：Lambda表达式
Comparator<String> c2 = (a, b) -> Integer.compare(a.length(), b.length());

// 阶段三：方法引用（进一步简化）
Comparator<String> c3 = Comparator.comparingInt(String::length);
```

| 阶段 | 代码量 | 可读性 | 底层机制 |
|------|--------|--------|----------|
| 匿名内部类 | 冗长 | 低 | 编译生成独立.class文件 |
| Lambda | 简洁 | 中 | invokedynamic动态生成 |
| 方法引用 | 极简 | 高 | 语法糖进一步简化 |

> 💡 Lambda的本质是**行为参数化**——将函数/方法当作参数传递，实现策略模式的内联化。

### 1.2 使用前提

Lambda只能用于**函数式接口**——有且仅有一个抽象方法的接口，这是Lambda类型推断的基础。

---

## 2. Lambda语法详解

### 2.1 标准语法

```java
(参数列表) -> { 方法体 }
```

### 2.2 四种简写规则

| 规则 | 示例 | 说明 |
|------|------|------|
| 参数类型可省略 | `(a, b) -> a + b` | 编译器通过泛型推断 |
| 单参数省略括号 | `s -> s.length()` | 只有一个参数时省略 `()` |
| 单行省略大括号 | `s -> System.out.println(s)` | 方法体仅一条语句时省略 `{}` |
| 单行省略return | `(a, b) -> a + b` | 单行且有返回值时省略 `return` |

```java
// 完整 -> 逐步简化
(String s) -> { return s.length(); }   // 完整
(s) -> { return s.length(); }          // 省略类型
s -> { return s.length(); }            // 单参省略括号
s -> s.length()                        // 单行省略 {} 和 return
```

### 2.3 多行表达式

```java
// 多行必须保留 {}、; 和 return
BinaryOperator<Integer> safeDivide = (a, b) -> {
    if (b == 0) return 0;
    return a / b;
};
```

> ⚠️ 只有单行表达式才能省略 `{}`、`;` 和 `return`。多行语句三者均不可省略。

---

## 3. 函数式接口（@FunctionalInterface）

### 3.1 定义与规范

函数式接口是有且仅有一个**抽象方法**的接口，通过 `@FunctionalInterface` 注解标识。

```java
@FunctionalInterface
interface Calculator {
    int calculate(int a, int b);

    // Object类方法不计入抽象方法计数
    boolean equals(Object obj);
    String toString();

    // 默认方法和静态方法不影响函数式接口定义
    default void log() { System.out.println("日志"); }
    static void info() { System.out.println("工具"); }
}
```

### 3.2 核心要点

| 特性 | 说明 |
|------|------|
| 抽象方法 | 有且仅有一个 |
| `@FunctionalInterface` | 编译器校验注解，推荐加（但不强制） |
| 默认方法（default） | 允许任意数量，不影响函数式接口定义 |
| 静态方法（static） | 允许任意数量，通过接口名调用 |
| Object类方法 | `equals`、`hashCode`、`toString` 不计入抽象方法计数 |

> 💡 判断函数式接口的标准：**接口中需要子类实现的方法是否只有一个**。默认/静态方法已有实现，不参与计数。

---

## 4. Java内置四大函数式接口

`java.util.function` 包提供了大量预定义的函数式接口，最核心的是以下四个：

### 4.1 核心接口

| 接口 | 抽象方法 | 用途 | 输入 | 输出 | 典型场景 |
|------|----------|------|------|------|----------|
| `Predicate<T>` | `boolean test(T t)` | 断言/判断 | 1参数 | boolean | 条件过滤、校验 |
| `Function<T, R>` | `R apply(T t)` | 映射/转换 | 1参数 | 1返回值 | 类型转换、字段提取 |
| `Consumer<T>` | `void accept(T t)` | 消费/处理 | 1参数 | 无 | 遍历、打印 |
| `Supplier<T>` | `T get()` | 供给/生产 | 无 | 1返回值 | 工厂模式、懒加载 |

```java
Predicate<String> nonEmpty = s -> s != null && !s.isEmpty();
Function<String, Integer> lenFunc = String::length;     // s -> s.length()
Consumer<String> printer = System.out::println;         // s -> System.out.println(s)
Supplier<Double> randomGen = () -> Math.random();
```

### 4.2 扩展接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `BiFunction<T, U, R>` | `R apply(T, U)` | 双参数映射 |
| `BinaryOperator<T>` | `T apply(T, T)` | 同类型双参数（Function特化） |
| `UnaryOperator<T>` | `T apply(T)` | 同类型单参数（Function特化） |
| `BiConsumer<T, U>` | `void accept(T, U)` | 双参数消费 |
| `BiPredicate<T, U>` | `boolean test(T, U)` | 双参数断言 |

> 💡 基本类型有专用函数式接口（如 `IntPredicate`、`LongConsumer`），使用它们可避免自动装箱开销。

---

## 5. Lambda vs 匿名内部类

| 对比维度 | 匿名内部类 | Lambda |
|----------|-----------|--------|
| **this指向** | 指向当前匿名内部类对象 | 指向**外部类**对象 |
| **编译产物** | 生成独立 `.class` 文件 | 不生成class文件，invokedynamic动态生成 |
| **作用域** | 拥有独立作用域，可定义局部变量 | 无独立作用域，共享外部类作用域 |
| **可实现的抽象方法数** | 多个 | 只能一个（函数式接口） |
| **性能** | 每次创建新类，加载开销大 | 首次构建稍大，后续性能更优 |

```java
// 匿名内部类——this 指向内部类对象
Runnable r1 = new Runnable() {
    @Override
    public void run() {
        System.out.println(this.getClass()); // class Test$1
    }
};

// Lambda——this 指向外部类对象
Runnable r2 = () -> {
    System.out.println(this.getClass()); // class Test（与外部类一致）
};
```

> 🎯 **何时用匿名内部类而非Lambda**：需要实现多个抽象方法时、接口含多个方法时、需要创建带状态的实例时。

---

## 6. 变量捕获（effectively final）

### 6.1 局部变量规则

Lambda中引用的局部变量必须 **effectively final**——初始化后未被重新赋值。

```java
String prefix = "Name: ";    // effectively final
// prefix = "Changed: ";     // 编译错误

Consumer<String> printer = name -> {
    System.out.println(prefix + name); // 只能读，不能改
    // prefix = "New: ";     // 编译错误
};
```

### 6.2 变量类型对比

| 变量类型 | 捕获规则 | 可修改性 | 原因 |
|----------|----------|----------|------|
| **局部变量** | 必须 effectively final | 不可修改 | 线程安全：局部变量在栈上，Lambda可能在另一线程执行 |
| **成员变量** | 通过this自由访问 | 可修改 | 成员变量在堆上，所有线程可见 |
| **静态变量** | 通过类名自由访问 | 可修改 | 静态变量在方法区，全局共享 |

```java
public class VariableCapture {
    private int instanceVar = 1;              // 成员变量—可修改
    private static int staticVar = 2;          // 静态变量—可修改
    public void test() {
        int localVar = 3;                      // 局部变量—不可修改
        Consumer<String> lambda = s -> {
            instanceVar++;    // OK
            staticVar++;      // OK
            // localVar++;    // 编译错误
            System.out.println(s + instanceVar + staticVar + localVar);
        };
    }
}
```

> 💡 Lambda捕获局部变量本质上是**值拷贝**，设计为effectively final可避免并发修改的竞态条件。

---

## 7. 方法引用的四种形式

方法引用是Lambda的进一步简化——当Lambda体仅调用某个已有方法时，用 `::` 运算符直接指向该方法。

### 7.1 四种形式

| 类型 | 语法 | 等价Lambda | 场景 |
|------|------|-----------|------|
| **静态方法引用** | `类名::静态方法` | `(args) -> 类名.静态方法(args)` | 工具类、Math运算 |
| **特定对象的实例方法** | `对象::实例方法` | `(args) -> 对象.实例方法(args)` | 外部对象方法调用 |
| **任意对象的实例方法** | `类名::实例方法` | `(obj, args) -> obj.实例方法(args)` | 首参数作为调用者 |
| **构造器引用** | `类名::new` | `(args) -> new 类名(args)` | 工厂、集合创建 |

### 7.2 示例

```java
// 1. 静态方法引用
Function<String, Integer> parser = Integer::parseInt;      // s -> Integer.parseInt(s)
BinaryOperator<Double> maxFinder = Math::max;               // (a, b) -> Math.max(a, b)

// 2. 特定对象的实例方法引用
List<String> list = Arrays.asList("a", "b");
list.forEach(System.out::println);                          // s -> System.out.println(s)

// 3. 任意对象的实例方法引用
Function<String, Integer> strLen = String::length;          // s -> s.length()
BiPredicate<String, String> equals = String::equals;        // (a, b) -> a.equals(b)
```

### 7.3 三段式对比

```java
List<String> names = Arrays.asList("Alice", "Bob", "Charlie");

// 匿名内部类
names.sort(new Comparator<String>() {
    @Override
    public int compare(String a, String b) { return a.compareToIgnoreCase(b); }
});
// Lambda
names.sort((a, b) -> a.compareToIgnoreCase(b));
// 方法引用
names.sort(String::compareToIgnoreCase);

// Stream管道对比
names.stream().map(s -> s.length()).collect(Collectors.toList());       // Lambda
names.stream().map(String::length).collect(Collectors.toList());        // 方法引用
```

> ⚠️ 方法引用不是必须的——Lambda体包含条件判断或复合操作时，应保留Lambda以保证可读性。

---

## 8. 构造器引用

使用 `类名::new` 替代Lambda中的 `new` 调用，编译器根据函数式接口的方法签名自动匹配构造器。

```java
// 无参构造器
Supplier<List<String>> listCreator = ArrayList::new;        // () -> new ArrayList<>()

// 单参构造器
Function<String, File> fileCreator = File::new;             // name -> new File(name)

// 双参构造器
BiFunction<String, Integer, AbstractMap.SimpleEntry<String, Integer>>
    entryCreator = AbstractMap.SimpleEntry::new;

// Stream实战：收集到指定集合
List<String> collected = names.stream()
    .filter(s -> s.length() > 3)
    .collect(Collectors.toCollection(ArrayList::new));

// 对象映射（构造器引用自动匹配）
class Person { String name; Person(String name) { this.name = name; } }
List<Person> people = names.stream()
    .map(Person::new)          // name -> new Person(name)
    .collect(Collectors.toList());
```

> 💡 编译器根据函数式接口方法签名自动选择匹配的构造器参数个数。

---

## 9. Lambda底层实现：invokedynamic

### 9.1 编译原理

```java
// 匿名内部类——编译生成 Test$1.class
Runnable r = new Runnable() {
    @Override public void run() { System.out.println("Hello"); }
};

// Lambda——不生成class文件
Runnable r = () -> System.out.println("Hello");
```

### 9.2 invokedynamic 机制

| 阶段 | 行为 | 说明 |
|------|------|------|
| **编译期** | 编译器将Lambda翻译为 `invokedynamic` 指令 | 指令包含bootstrap method链接信息 |
| **首次调用** | JVM调用 `LambdaMetafactory.metafactory()` | 动态生成函数式接口的实现类 |
| **后续调用** | 直接调用已生成的实现类 | 只生成一次，性能接近普通方法调用 |

```java
// 底层大致等价（运行时动态生成）
final class $$Lambda$1 implements Runnable {
    @Override
    public void run() { System.out.println("Hello"); }
}
```

### 9.3 优势对比

| 特性 | 匿名内部类 | Lambda（invokedynamic） |
|------|-----------|------------------------|
| class文件 | 每个匿名类生成独立 `.class` 文件 | 不生成额外class文件 |
| 类加载 | 每次使用都要加载新类 | 运行时动态生成，按需创建 |
| JIT优化 | 每个匿名类独立编译优化 | 同一函数式接口复用同一策略 |
| 内存占用 | 每实例持有外部类引用 | 更轻量，无额外元数据开销 |

> 🎯 invokedynamic机制使Lambda在**性能**和**灵活性**上全面优于匿名内部类，是Java语言在JVM层面的重要进化。

---

## 10. 函数式编程思想

### 10.1 核心原则

| 原则 | 说明 | Java实践 |
|------|------|----------|
| **纯函数** | 相同输入相同输出，无副作用 | Lambda不修改外部变量 |
| **不可变性** | 创建新数据而非修改原数据 | Stream不改变原集合 |
| **函数是一等公民** | 函数可赋值、传参、返回 | 函数式接口作为参数/返回值 |
| **声明式编程** | 关注"做什么"而非"怎么做" | Stream链式API表达意图 |

```java
// 命令式（怎么做）
List<String> r1 = new ArrayList<>();
for (String s : list) { if (s.startsWith("A")) r1.add(s.toUpperCase()); }

// 声明式（做什么）——函数式风格
List<String> r2 = list.stream()
    .filter(s -> s.startsWith("A"))
    .map(String::toUpperCase)
    .collect(Collectors.toList());
```

### 10.2 map / filter / reduce 三大抽象

```java
List<Integer> nums = Arrays.asList(1, 2, 3, 4, 5);

nums.stream().map(n -> n * 2).collect(Collectors.toList());    // [2,4,6,8,10]  映射
nums.stream().filter(n -> n % 2 == 0).collect(Collectors.toList()); // [2,4]      过滤
nums.stream().reduce(0, Integer::sum);                         // 15            归约
```

### 10.3 函数式 vs 面向对象

| 维度 | 函数式 | 面向对象 |
|------|--------|----------|
| 数据与行为 | 分离，通过函数组合 | 封装在对象中 |
| 状态管理 | 不可变数据，无副作用 | 可变状态，消息传递 |
| 并发安全 | 天然安全（无共享可变状态） | 需锁/原子类等机制 |
| 组合方式 | 函数组合（compose/andThen） | 继承/多态 |

---

## 11. Lambda实战应用场景

### 11.1 集合操作（配合Stream）

```java
// 筛选 + 排序 + 收集
List<String> cheapNames = products.stream()
    .filter(p -> p.getPrice() < 100)
    .sorted(Comparator.comparing(Product::getPrice))
    .map(Product::getName)
    .collect(Collectors.toList());
```

### 11.2 线程创建

```java
new Thread(() -> System.out.println("Lambda线程")).start();
```

### 11.3 回调与事件处理

```java
button.addActionListener(e -> System.out.println("按钮点击"));
asyncFetch(data -> System.out.println("收到：" + data));
```

### 11.4 策略模式内联化

```java
// 传统策略模式 → Lambda直接传递策略
List<Integer> nums = Arrays.asList(3, 1, 4, 1, 5, 9);
nums.sort((a, b) -> a - b);                    // 升序策略
nums.sort((a, b) -> b - a);                    // 降序策略

Function<Order, Double> vipPricing = o -> o.getPrice() * o.getQty() * 0.8;
double amount = calculate(order, vipPricing);
```

### 11.5 集合内部迭代

```java
Map<String, Integer> scores = new HashMap<>();
scores.put("Alice", 95);
scores.forEach((name, score) -> System.out.println(name + ": " + score));

scores.computeIfAbsent("Charlie", k -> 0);     // 不存在则计算
scores.merge("Alice", 5, Integer::sum);         // 合并已有值
```

---

## 12. 常见陷阱与最佳实践

### 12.1 常见陷阱

| 陷阱 | 错误示例 | 说明 |
|------|----------|------|
| **流重复使用** | `stream.forEach(...); stream.count();` | 一个流只能消费一次 |
| **Lambda修改局部变量** | `int x=0; list.forEach(s->x++);` | 局部变量必须effectively final |
| **并行流线程安全** | `list.parallelStream().forEach(list::add);` | 非线程安全集合导致数据竞争 |
| **方法引用滥用** | `.map(s->s.toString())` 应改用 `.map(Object::toString)` | 简单调用用方法引用更清晰 |

### 12.2 最佳实践

```java
// 1. 优先方法引用
list.stream().map(String::toUpperCase).forEach(System.out::println);

// 2. Lambda体超过3行提取为方法
list.forEach(this::processItem);   // 优于 inline 复杂Lambda

// 3. 参数命名有意义
students.stream().filter(s -> s.getAge() > 18);  // 优于 x -> x.getAge()

// 4. 复杂管道拆分为多行
List<String> result = students.stream()
    .filter(Student::isActive)
    .map(Student::getName)
    .sorted()
    .collect(Collectors.toList());
```

### 12.3 性能注意事项

- **避免装箱开销**：`mapToInt()` 代替 `map()`，用 `IntPredicate` 代替 `Predicate<Integer>`
- **小数据集**（<1000）：普通for循环可能更快
- **并行流**：仅在**数据量大 + CPU密集型 + 无状态**时使用

> ⚠️ 并行流并非万能——数据量小或涉及IO时，并行化的开销反而导致性能下降。

---

## 13. 面试高频考点

### 13.1 高频面试题

| 问题 | 答案要点 |
|------|----------|
| **Lambda的使用前提？** | 必须搭配函数式接口（仅一个抽象方法） |
| **Lambda修改局部变量为什么报错？** | effectively final保证线程安全 |
| **Lambda与匿名内部类的this区别？** | Lambda指向外部类，匿名内部类指向自身 |
| **函数式接口能否包含多个方法？** | 抽象方法只能一个，默认/静态方法不限 |
| **Lambda底层实现？** | invokedynamic + LambdaMetafactory |
| **方法引用有哪几种形式？** | 四种：静态方法、特定对象实例、任意对象实例、构造器 |
| **Consumer和Supplier区别？** | Consumer有入参无返回；Supplier无入参有返回 |

### 13.2 手撕代码题型

```java
// 1. 用Lambda实现自定义排序
list.sort(Comparator.comparingInt(String::length));

// 2. 用Stream实现分页
List<Integer> page = list.stream()
    .skip((pageNo - 1) * pageSize)
    .limit(pageSize)
    .collect(Collectors.toList());

// 3. 自定义函数式接口
@FunctionalInterface
interface TriFunction<A, B, C, R> { R apply(A a, B b, C c); }
TriFunction<Integer, Integer, Integer, Integer> sum3 = (a, b, c) -> a + b + c;
```

### 13.3 知识体系总览

```text
Lambda 与方法引用
├── 基础语法          (参数) -> {表达式}  + 四种简写规则
├── 函数式接口        @FunctionalInterface + 四大核心接口
├── 方法引用          静态方法:: / 实例方法:: / 构造器::new
├── 底层原理          invokedynamic + LambdaMetafactory
├── 核心概念          effectively final / 行为参数化 / 声明式编程
└── 实战应用          Stream / 集合迭代 / 线程回调 / 策略模式
```

> 🎯 Lambda表达式是Java函数式编程的基石，理解其语法、原理和最佳实践是掌握Stream API、CompletableFuture、Optional等Java 8+特性的前提。从匿名内部类到Lambda再到方法引用，是Java代码从"冗长"到"简洁"再到"优雅"的进化之路。
