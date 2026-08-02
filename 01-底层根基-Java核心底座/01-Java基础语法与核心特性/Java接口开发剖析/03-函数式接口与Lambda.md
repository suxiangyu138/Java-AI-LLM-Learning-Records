# 函数式接口与 Lambda

> @FunctionalInterface 是接口最优雅的应用——一个方法就是一个行为，一个 Lambda 就是一个实现，让代码从"怎么做"变成"做什么"

## 📚 目录

1. [@FunctionalInterface 核心解析](#1)
2. [java.util.function 四大核心接口](#2)
3. [特化接口与扩展](#3)
4. [函数式接口组合与高阶模式](#4)
5. [方法引用完全指南](#5)
6. [实战：用 Lambda 取代设计模式](#6)

---

## 1. @FunctionalInterface 核心解析 {#1}

### 1.1 什么是函数式接口？

```text
函数式接口 (Functional Interface / SAM Interface)：
    只有一个抽象方法的接口

@FunctionalInterface 注解：
    ├── 编译器验证：确保接口只有一个抽象方法
    ├── 文档信号：告诉使用者"这是一个函数式接口"
    ├── 可以有 default / static / private 方法
    └── Object 的 public 方法不算入抽象方法计数
```

```java
// ── 有效的函数式接口 ──
@FunctionalInterface
public interface Calculator {
    int compute(int a, int b);  // ← 唯一的抽象方法

    // ✅ 可以有这些：
    default int add(int a, int b) { return a + b; }
    static Calculator of(String type) { return ...; }
    private void helper() { }
    String toString();           // Object 方法，不计入
    boolean equals(Object obj);  // Object 方法，不计入
}

// ❌ 编译错误：多个抽象方法
@FunctionalInterface
public interface Invalid {
    void doA();
    void doB();  // 编译错误！
}
```

### 1.2 函数式接口 ≠ Lambda

```
函数式接口 (FI)：Java 类型系统概念——只有一个抽象方法的接口
Lambda：Java 语法概念——函数式接口的简洁实现方式

Lambda 的目标类型必须是函数式接口
但函数式接口的实现不一定要用 Lambda（也可以匿名类/具名类）
```

```java
// ── 三种实现方式 ──
Calculator calc;

// 1. Lambda（最简洁）
calc = (a, b) -> a + b;

// 2. 匿名内部类（冗长）
calc = new Calculator() {
    @Override public int compute(int a, int b) { return a + b; }
};

// 3. 方法引用（更简洁）
calc = Integer::sum;

// 4. 具名实现类（正式项目）
calc = new Adder();  // class Adder implements Calculator
```

### 1.3 Lambda 语法速查

```java
// ── Lambda 语法形式 ──
() -> expr                    // 无参，单表达式
() -> { statements; }         // 无参，语句块
(param) -> expr               // 单参（括号可省略：param -> expr）
(p1, p2) -> expr              // 多参
(Type p1, Type p2) -> expr    // 带类型声明（通常可省略）

// ── 实际例子 ──
Runnable r = () -> System.out.println("Hello");

Consumer<String> c = s -> System.out.println(s);

BinaryOperator<Integer> add = (a, b) -> a + b;

Comparator<String> byLength = (s1, s2) -> {
    int diff = s1.length() - s2.length();
    return diff != 0 ? diff : s1.compareTo(s2);
};
```

---

## 2. java.util.function 四大核心接口 {#2}

### 2.1 全景速查

```text
┌─────────────────────────────────────────────────────┐
│          java.util.function 四大金刚                  │
│                                                     │
│  Function<T,R>  输入 T → 输出 R    转换/映射         │
│  Consumer<T>    输入 T → 无输出    消费/副作用       │
│  Predicate<T>   输入 T → boolean   测试/过滤         │
│  Supplier<T>    无输入 → 输出 T    提供/工厂         │
│                                                     │
│  记忆口诀：                                           │
│  Function 有进有出，Consumer 只进不出                  │
│  Predicate 判真假，Supplier 无中生有                   │
└─────────────────────────────────────────────────────┘
```

### 2.2 Function<T, R> —— 转换/映射

```java
// ── Function<T,R>: 输入 T，返回 R ──
Function<String, Integer> strLength = String::length;
int len = strLength.apply("hello");  // 5

// ── BiFunction<T,U,R>: 两个输入，一个输出 ──
BiFunction<Integer, Integer, String> format = (a, b) -> a + " + " + b + " = " + (a + b);

// ── Stream 中的应用 ──
List<String> names = users.stream()
    .map(User::getName)           // Function<User, String>
    .toList();
```

### 2.3 Consumer<T> —— 消费/副作用

```java
// ── Consumer<T>: 输入 T，无返回（副作用）──
Consumer<String> printer = System.out::println;
printer.accept("Hello World");

// ── BiConsumer<T,U>: 两个输入 ──
BiConsumer<String, Integer> repeat = (s, n) -> {
    for (int i = 0; i < n; i++) System.out.println(s);
};

// ── Stream 中的应用 ──
users.forEach(u -> log.info("User: {}", u));  // Consumer<User>

// ── 设置对象属性 ──
Consumer<User> setName = u -> u.setName("Default");
users.forEach(setName);
```

### 2.4 Predicate<T> —— 测试/过滤

```java
// ── Predicate<T>: 输入 T，返回 boolean ──
Predicate<String> isEmpty = String::isEmpty;
Predicate<String> isLong = s -> s.length() > 10;

// ── 组合 ──
Predicate<String> valid = isEmpty.negate()        // 非空
    .and(s -> s.length() >= 3)                     // 且长度 ≥ 3
    .or(s -> s.equals("admin"));                   // 或是 admin

// ── Stream 中的应用 ──
List<User> adults = users.stream()
    .filter(u -> u.getAge() >= 18)                // Predicate<User>
    .toList();

// ── Collection 中的应用 (JDK 11+) ──
users.removeIf(u -> u.getAge() < 0);              // 移除无效用户
```

### 2.5 Supplier<T> —— 提供/工厂

```java
// ── Supplier<T>: 无输入，返回 T ──
Supplier<Double> random = Math::random;
Supplier<User> newUser = () -> new User("anonymous");

// ── 延迟计算 ──
public User getUser(Long id, Supplier<User> fallbackLoader) {
    return cache.get(id).orElseGet(fallbackLoader);  // 缓存没有才调用
}

// ── 日志延迟求值 ──
// ❌ 总是执行 expensiveFormat()
log.debug("User: {}", expensiveFormat(user));

// ✅ 仅当 DEBUG 级别开启时才执行
log.atDebug().log("User: {}", () -> expensiveFormat(user));

// ── Optional.orElseGet ──
User user = optionalUser.orElseGet(() -> createDefaultUser());
```

---

## 3. 特化接口与扩展 {#3}

### 3.1 基本类型特化

```java
// ── 为什么需要特化？──
// Function<Integer, Integer> f = x -> x * 2;
// 每次调用都要装箱/拆箱 → 性能损失！

// ✅ IntFunction / LongFunction / DoubleFunction
IntFunction<String> intToString = i -> "Value: " + i;

// ✅ ToIntFunction / ToLongFunction / ToDoubleFunction
ToIntFunction<String> strLength = String::length;  // 返回 int（不装箱）

// ✅ IntUnaryOperator / LongUnaryOperator / DoubleUnaryOperator
IntUnaryOperator doubler = x -> x * 2;    // int → int，零装箱

// ✅ IntPredicate / LongPredicate / DoublePredicate
IntPredicate isPositive = x -> x > 0;

// ✅ IntSupplier / LongSupplier / DoubleSupplier
IntSupplier threadId = () -> Thread.currentThread().hashCode();

// ✅ IntConsumer / LongConsumer / DoubleConsumer
IntConsumer printInt = System.out::println;

// ✅ IntBinaryOperator / LongBinaryOperator / DoubleBinaryOperator
IntBinaryOperator max = Math::max;
```

### 3.2 扩展函数式接口

```java
// ── UnaryOperator<T> = Function<T,T> ──
UnaryOperator<String> upper = String::toUpperCase;
// 等价于 Function<String, String>

// ── BinaryOperator<T> = BiFunction<T,T,T> ──
BinaryOperator<Integer> sum = Integer::sum;
BinaryOperator<Integer> maxBy = BinaryOperator.maxBy(Integer::compareTo);

// Stream.reduce 中大量使用 BinaryOperator
int result = IntStream.of(1, 2, 3, 4, 5).reduce(0, Integer::sum);
```

### 3.3 自定义函数式接口

```java
// ── 何时需要自定义？──
// 1. 需要描述性名称 → @FunctionalInterface 本身就是文档
// 2. 需要特定的异常签名
// 3. 需要特定的 default 方法组合

// ── 示例1: 可抛异常的函数式接口 ──
@FunctionalInterface
public interface ThrowingFunction<T, R, E extends Exception> {
    R apply(T t) throws E;

    // 将"抛异常版"转为标准 Function
    static <T, R> Function<T, R> unchecked(ThrowingFunction<T, R, ?> f) {
        return t -> {
            try {
                return f.apply(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}

// ── 示例2: 带描述的三元函数（当 BiFunction 不够用时）──
@FunctionalInterface
public interface TriFunction<T, U, V, R> {
    R apply(T t, U u, V v);

    default <W> TriFunction<T, U, V, W> andThen(Function<? super R, ? extends W> after) {
        return (t, u, v) -> after.apply(apply(t, u, v));
    }
}
```

---

## 4. 函数式接口组合与高阶模式 {#4}

### 4.1 Function 链式组合

```java
// ── andThen: 先 f 后 g ──
Function<String, String> trim = String::trim;
Function<String, String> upper = String::toUpperCase;
Function<String, String> process = trim.andThen(upper);
process.apply("  hello  ");  // "HELLO"

// ── compose: 先 g 后 f ──
Function<String, Integer> length = String::length;
Function<User, String> getName = User::getName;
Function<User, Integer> nameLength = length.compose(getName);
// 等价于: user → length.apply(getName.apply(user))

// ── 构建处理管道 ──
Function<String, User> parseJson = jsonParser::parse;
Function<User, UserDTO> enrich = userService::enrich;
Function<UserDTO, String> toXml = xmlSerializer::serialize;

Function<String, String> pipeline = parseJson
    .andThen(enrich)
    .andThen(toXml);

String xml = pipeline.apply(jsonInput);
```

### 4.2 Predicate 逻辑组合

```java
// ── 组合：and / or / negate ──
Predicate<User> isAdult = u -> u.getAge() >= 18;
Predicate<User> isActive = User::isActive;
Predicate<User> hasValidEmail = u -> u.getEmail() != null
    && u.getEmail().contains("@");

// 构建复合条件
Predicate<User> eligibleUser = isAdult
    .and(isActive)
    .and(hasValidEmail.negate().or(u -> u.getEmail() == null));

// Stream 中使用
users.stream()
    .filter(isAdult.and(isActive))
    .forEach(System.out::println);
```

### 4.3 Consumer 链式组合

```java
// ── andThen: 顺序执行多个 Consumer ──
Consumer<User> saveToDb = userRepository::save;
Consumer<User> sendEmail = emailService::sendWelcome;
Consumer<User> log = u -> log.info("Created: {}", u);

Consumer<User> onUserCreated = saveToDb
    .andThen(sendEmail)
    .andThen(log);

onUserCreated.accept(newUser);
```

### 4.4 柯里化 (Currying)

```java
// ── 将多参数函数转为单参数函数链 ──
public static <T, U, V> Function<T, Function<U, V>> curry(
        BiFunction<T, U, V> bif) {
    return t -> u -> bif.apply(t, u);
}

// 使用
BiFunction<Double, Double, Double> converter = (rate, amount) -> rate * amount;
Function<Double, Function<Double, Double>> curried = curry(converter);

Function<Double, Double> usdToCny = curried.apply(7.25);  // 汇率 7.25
double cny = usdToCny.apply(100);   // 725.0 = 100 USD
double cny2 = usdToCny.apply(200);  // 1450.0 = 200 USD
```

---

## 5. 方法引用完全指南 {#5}

### 5.1 四种形式

```text
1. 静态方法引用    ClassName::staticMethod
2. 实例方法引用    instance::instanceMethod
3. 特定类型方法引用 ClassName::instanceMethod  (第一个参数是调用者)
4. 构造器引用      ClassName::new

规则：方法引用的参数和返回值必须与函数式接口匹配
```

```java
// ── 1. 静态方法引用 ──
Function<String, Integer> parseInt = Integer::parseInt;
// 等价于: s -> Integer.parseInt(s)

// ── 2. 实例方法引用 ──
String prefix = "Hello, ";
Function<String, String> concat = prefix::concat;
// 等价于: s -> prefix.concat(s)

// ── 3. 特定类型方法引用 ──
// 第一个参数是方法调用的接收者
Function<String, String> toLower = String::toLowerCase;
// 等价于: s -> s.toLowerCase()
// 注意：这里 String 是类型，不是实例！参数 s 成为接收者

Comparator<String> byLength = String::compareTo;
// 等价于: (s1, s2) -> s1.compareTo(s2)

// ── 4. 构造器引用 ──
Supplier<User> userFactory = User::new;
// 等价于: () -> new User()

Function<String, User> namedUserFactory = User::new;
// 等价于: name -> new User(name)

// ── 数组构造器引用 ──
IntFunction<String[]> arrayFactory = String[]::new;
String[] arr = arrayFactory.apply(10);  // new String[10]
```

### 5.2 方法引用 vs Lambda

```java
// ── 什么时候用方法引用？（更简洁、更可读）──
// ✅ 方法引用：直接引用已有方法
users.stream().map(User::getName)           // 方法引用
users.stream().forEach(System.out::println) // 方法引用

// ✅ Lambda：需要额外逻辑
users.stream().map(u -> u.getName().trim())              // Lambda
users.stream().filter(u -> u.getAge() > 18)              // Lambda
users.stream().map(u -> "User: " + u.getName())          // Lambda

// 规则：如果 Lambda 体是"直接调用一个方法"，就用方法引用
```

---

## 6. 实战：用 Lambda 取代设计模式 {#6}

```java
// ── 策略模式 → Lambda ──
// ❌ 传统：每种策略一个类
class VipDiscount implements DiscountStrategy {
    public double apply(double price) { return price * 0.8; }
}
// ✅ Lambda：策略就是函数
Map<String, Function<Double, Double>> discounts = Map.of(
    "vip",     price -> price * 0.8,
    "normal",  price -> price * 0.95,
    "student", price -> price * 0.75
);
double finalPrice = discounts.get(userType).apply(100.0);

// ── 命令模式 → Lambda ──
// ❌ 传统：每个命令一个类
// ✅ Lambda：命令就是 Runnable
Map<String, Runnable> commands = Map.of(
    "start",  () -> server.start(),
    "stop",   () -> server.stop(),
    "status", () -> server.printStatus()
);

// ── 观察者模式 → Consumer ──
List<Consumer<Event>> listeners = new ArrayList<>();
listeners.add(e -> log.info("Event: {}", e));
listeners.add(e -> metrics.record(e));
// 通知
listeners.forEach(l -> l.accept(event));

// ── 工厂模式 → Supplier ──
Map<String, Supplier<Payment>> paymentFactories = Map.of(
    "wechat",  WeChatPay::new,
    "alipay",  Alipay::new,
    "card",    CardPay::new
);
Payment p = paymentFactories.get(type).get();
```

> 🎯 **核心要点**：函数式接口 + Lambda 不是要替代所有设计模式，而是让**简单的事情回归简单**——当一个模式退化到"只定义一个方法"时，用 Lambda 是实现它的最简方式。

---

**返回总览：** [00-Java接口开发剖析总览](./00-Java接口开发剖析总览.md) | **下一模块：** [04-接口设计原则与契约](./04-接口设计原则与契约.md)
