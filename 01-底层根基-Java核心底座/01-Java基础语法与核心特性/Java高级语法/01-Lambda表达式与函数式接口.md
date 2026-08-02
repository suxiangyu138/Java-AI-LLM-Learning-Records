# 01 - Lambda 表达式与函数式接口

> 🎯 Lambda 是 Java 8 最革命性的特性——让 Java 从"一切皆对象"进化到"函数也是一等公民"。Function、Predicate、Consumer、Supplier 四大函数式接口是 Stream API 和现代框架的基础

---

## 目录

1. [Lambda 语法](#1-lambda-语法)
2. [四大函数式接口](#2-四大函数式接口)
3. [方法引用](#3-方法引用)
4. [实战模式](#4-实战模式)

---

## 1. Lambda 语法

```java
// 匿名内部类 → Lambda 的简化历程

// Java 7: 匿名内部类
Runnable r1 = new Runnable() {
    @Override public void run() { System.out.println("Hello"); }
};

// Java 8: Lambda
Runnable r2 = () -> System.out.println("Hello");

// 完整语法：(参数) -> { 表达式 }
(a, b) -> a + b                    // 隐式 return
(a, b) -> { return a + b; }        // 显式 return
(String s) -> s.length()           // 可带类型
s -> s.length()                    // 单参数可省略括号
() -> System.out.println("Hi")     // 无参数必须括号
```

## 2. 四大函数式接口

| 接口 | 方法 | 输入 | 输出 | 场景 |
|------|------|:---:|:---:|------|
| **Function<T,R>** | `R apply(T t)` | 1 | 1 | 转换/映射 |
| **Predicate<T>** | `boolean test(T t)` | 1 | bool | 过滤/判断 |
| **Consumer<T>** | `void accept(T t)` | 1 | 0 | 消费/副作用 |
| **Supplier<T>** | `T get()` | 0 | 1 | 工厂/惰性求值 |

```java
// Function: 转换
Function<String, Integer> strlen = String::length;
int len = strlen.apply("hello");           // 5

// Predicate: 判断
Predicate<String> isEmpty = String::isEmpty;
boolean blank = isEmpty.test("");          // true

// Consumer: 消费
Consumer<String> printer = System.out::println;
printer.accept("Hello Lambda");

// Supplier: 提供
Supplier<Double> random = Math::random;
double val = random.get();

// 组合与链式
Function<Integer, Integer> times2 = x -> x * 2;
Function<Integer, Integer> plus3 = x -> x + 3;
times2.andThen(plus3).apply(5);            // 5*2+3 = 13
times2.compose(plus3).apply(5);            // (5+3)*2 = 16

Predicate<String> notEmpty = isEmpty.negate();
Predicate<String> isA = s -> s.startsWith("A");
isA.and(notEmpty).test("ABC");             // true
```

## 3. 方法引用

```java
// 四种方法引用

// 1. 静态方法引用    ClassName::staticMethod
Function<String, Integer> f1 = Integer::parseInt;

// 2. 实例方法引用    instance::method
String prefix = "Hello ";
Function<String, String> f2 = prefix::concat;

// 3. 类实例方法引用  ClassName::instanceMethod (第一个参数是调用者)
BiFunction<String, String, Boolean> f3 = String::startsWith;

// 4. 构造器引用       ClassName::new
Supplier<ArrayList<String>> f4 = ArrayList::new;
Function<Integer, ArrayList<String>> f5 = ArrayList::new; // 带参数
```

## 4. 实战模式

```java
// 策略模式 → Lambda
Map<String, DiscountStrategy> strategies = Map.of(
    "VIP", price -> price * 0.8,
    "NORMAL", price -> price * 0.95,
    "NEW", price -> price
);
double finalPrice = strategies.get(type).apply(originalPrice);

// 延迟执行
public void logIfDebug(Supplier<String> messageSupplier) {
    if (DEBUG) System.out.println(messageSupplier.get());
}
logIfDebug(() -> expensiveToString(obj));  // 只在 DEBUG 时才执行

// 模板方法 → Lambda
public <T> T withTransaction(Function<Connection, T> action) {
    try (Connection conn = getConnection()) {
        conn.setAutoCommit(false);
        T result = action.apply(conn);
        conn.commit();
        return result;
    } catch (Exception e) {
        conn.rollback();
        throw e;
    }
}
```

## 核心要点回顾

- Lambda = 匿名函数的语法糖，本质是函数式接口的实现
- Function<T,R>(转换) / Predicate(判断) / Consumer(消费) / Supplier(提供)
- 方法引用 > Lambda > 匿名类（简洁性递减）
- 策略模式、模板方法、延迟执行 → Lambda 让代码少 50%+

## 参考资料

1. java.util.function 包文档
2. Effective Java 3rd — Item 42-44
