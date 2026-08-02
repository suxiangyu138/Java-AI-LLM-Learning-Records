# 02 函数式接口全家桶

> 一句话定位：以函数描述符为坐标系掌握 `java.util.function`，让 Lambda 选型、组合、泛型边界和性能取舍都有明确依据。

---

## 📚 目录

1. [函数式接口与 SAM 判定](#1-函数式接口与-sam-判定)
2. [@FunctionalInterface 注解](#2-functionalinterface-注解)
3. [Function 输入转换](#3-function-输入转换)
4. [Predicate 条件判断](#4-predicate-条件判断)
5. [Consumer 消费动作](#5-consumer-消费动作)
6. [Supplier 延迟提供](#6-supplier-延迟提供)
7. [Bi 接口与多参数行为](#7-bi-接口与多参数行为)
8. [Operator 运算接口](#8-operator-运算接口)
9. [原始类型特化接口](#9-原始类型特化接口)
10. [组合方法与执行顺序](#10-组合方法与执行顺序)
11. [自定义函数式接口设计](#11-自定义函数式接口设计)
12. [选型速查与练习](#12-选型速查与练习)

---

## 1. 函数式接口与 SAM 判定

### 1.1 函数式接口是什么

函数式接口（functional interface）是**恰好拥有一个抽象方法**的接口。这个唯一抽象方法也叫函数描述符（function descriptor），它决定了 Lambda 的参数、返回值和受检异常边界。

```java
@FunctionalInterface
interface Formatter {
    String format(String input);
}

Formatter upper = value -> value.toUpperCase(Locale.ROOT);
```

函数式接口仍然是普通接口，可以拥有常量、`default` 方法、`static` 方法和从 `Object` 继承的公共方法；“一个抽象方法”不等于“接口只能写一行方法”。

| 成员类型 | 是否计入 SAM 抽象方法数量 | 说明 |
|---|:---:|---|
| 普通抽象实例方法 | 是 | 必须参与唯一性判定 |
| `default` 方法 | 否 | 已经提供实现 |
| `static` 方法 | 否 | 属于接口本身，不是实例抽象方法 |
| `private` 方法 | 否 | Java 9+ 接口内部辅助方法 |
| `Object` 的 `public` 方法 | 否 | `equals`、`hashCode`、`toString` 等不增加 SAM 数量 |

### 1.2 `Object` 方法不破坏函数式接口

```java
@FunctionalInterface
interface NamedTask {
    void run();

    // Object 的公共方法不会算作新的抽象方法
    boolean equals(Object other);
    int hashCode();
    String toString();
}

NamedTask task = () -> System.out.println("run");
```

这里 `run()` 仍然是唯一需要 Lambda 提供实现的抽象方法。接口声明 `equals` 等方法只是对 `Object` 方法的重新声明，不改变函数式接口资格。

> ⚠️ **豁免只覆盖 `Object` 的 `public` 方法**。`clone()` 与 `finalize()` 在 `Object` 里是 **protected**，接口重声明它们等于把 protected 提升为 public，构成了**新契约**，因此计入抽象方法数：
>
> ```java
> // ❌ 实测报错：WithClone 不是函数接口
> //              在 接口 WithClone 中找到多个非覆盖抽象方法
> @FunctionalInterface
> interface WithClone {
>     void run();
>     Object clone();      // protected 方法，不享受豁免 → 第二个抽象方法
> }
> ```
>
> 记忆理由：`equals`/`hashCode`/`toString` 的实现**每个类都从 `Object` 白拿**，声明它们等于没提要求；而 `clone()` 提升可见性后，实现类并没有一个 public 的现成实现可用。

另一个反直觉的推论：**只声明 `Object` 方法的接口不是函数式接口**，因为豁免之后抽象方法数是 **0**，而要求是恰好 1 个。

```java
// ❌ 抽象方法数 = 0，不满足"恰好一个"
// @FunctionalInterface
interface OnlyObjectMethods {
    boolean equals(Object o);
    int hashCode();
}
```

### 1.3 继承关系中的 SAM 合并

判定要看继承后最终可见的抽象方法集合，而不是简单数源代码中的方法行数。

```java
interface Parent {
    void execute();
}

@FunctionalInterface
interface Child extends Parent {
    // 继承 execute()，没有新增抽象方法
}

Child child = () -> System.out.println("child");
```

两个接口声明签名相同的抽象方法，会合并为一个契约：

```java
interface Left {
    Object value();
}

interface Right {
    String value(); // 协变返回值，可与 Left 合并
}

@FunctionalInterface
interface Both extends Left, Right {
}
```

如果继承后留下两个无法合并的抽象方法，接口就不是函数式接口：

```java
interface A { void a(); }
interface B { void b(); }

// ❌ 有两个不同的抽象方法
// interface NotFunctional extends A, B {}
```

> 🎯 **判定口诀**：先剔除 `default/static/private`，再剔除 `Object` 公共方法，最后对继承来的同签名方法做合并；剩下一个抽象契约才是 SAM。

---

## 2. @FunctionalInterface 注解

### 2.1 注解是编译期护栏

`@FunctionalInterface` 不是 Lambda 运行时必需条件，而是告诉编译器和维护者：“这个接口设计上应该保持函数式”。如果接口后来新增第二个抽象方法，编译器会立即报错。

```java
@FunctionalInterface
interface Validator<T> {
    boolean test(T value);

    default Validator<T> negate() {
        return value -> !test(value);
    }
}
```

以下写法会被拒绝：

```java
// ❌ @FunctionalInterface
// interface Broken {
//     void first();
//     void second();
// }
```

没有注解的接口只要符合 SAM 规则仍能接收 Lambda：

```java
interface LegacyAction {
    void run();
}

LegacyAction action = () -> System.out.println("compatible");
```

### 2.2 设计接口时保留演进空间

`default` 方法可以为接口增加能力而不破坏已有 Lambda 实现，但不要用大量默认方法把一个简单接口变成难以理解的“小型框架”。抽象方法命名应表达业务动作，而不是机械复制 `apply`。

```java
@FunctionalInterface
interface RetryableOperation<T> {
    T execute() throws Exception;

    default String description() {
        return "retryable operation";
    }
}
```

---

## 3. Function 输入转换

### 3.1 `Function<T, R>`

`Function<T, R>` 表示“接收一个 `T`，返回一个 `R`”，抽象方法是 `R apply(T t)`。它是 Stream `map`、对象转换器和策略映射中最常见的接口。

```java
Function<String, Integer> length = String::length;
Function<User, UserView> toView = user -> new UserView(user.id(), user.name());

int size = length.apply("lambda");
```

```java
record User(long id, String name) {}
record UserView(long id, String displayName) {}
```

### 3.2 `compose` 与 `andThen`

```java
Function<String, String> trim = String::trim;
Function<String, Integer> parse = Integer::parseInt;

Function<String, Integer> trimThenParse = parse.compose(trim);
Function<String, String> parseThenFormat = parse.andThen(String::valueOf);

System.out.println(trimThenParse.apply(" 42 "));  // 42（int）
System.out.println(parseThenFormat.apply("42"));  // "42"（String）
```

> ⚠️ 注意 `parseThenFormat` 的声明类型必须是 `Function<String, String>` 而不是 `Function<String, Integer>`。`andThen` 会把返回类型换成后一个函数的返回类型（这里 `String::valueOf` 返回 `String`），写错会得到一条很难读的报错：`推论变量 V 具有不兼容的上限：等式约束条件 Integer，下限 String`。

| 写法 | 执行顺序 | 组合结果 | 结果类型 |
|---|---|---|---|
| `f.compose(g)` | 先 `g`，后 `f` | `f(g(x))` | 取 `f` 的返回类型 |
| `f.andThen(g)` | 先 `f`，后 `g` | `g(f(x))` | 取 `g` 的返回类型 |

`compose` 和 `andThen` 只负责串联，不会自动处理 `null`、异常或线程切换。

### 3.3 `identity`

```java
Function<String, String> same = Function.identity();
List<String> copy = names.stream()
        .collect(Collectors.toMap(Function.identity(), Function.identity()))
        .keySet()
        .stream()
        .toList();
```

`identity()` 适合表达“键和值都使用元素本身”等语义；如果直接写 `x -> x` 也没错，但方法名可以让意图更明确。

---

## 4. Predicate 条件判断

### 4.1 基本用法

`Predicate<T>` 表示接收 `T` 并返回 `boolean`，抽象方法为 `boolean test(T t)`。

```java
Predicate<String> notBlank = value -> value != null && !value.isBlank();
Predicate<Integer> adult = age -> age >= 18;

if (notBlank.test("Java")) {
    System.out.println("valid");
}
```

### 4.2 `and`、`or`、`negate` 的短路

```java
Predicate<String> hasText = value -> {
    System.out.println("check text");
    return value != null && !value.isBlank();
};
Predicate<String> longEnough = value -> {
    System.out.println("check length");
    return value.length() >= 8;
};

Predicate<String> valid = hasText.and(longEnough);
```

组合规则遵循布尔短路：`and` 左侧为 `false` 时不执行右侧；`or` 左侧为 `true` 时不执行右侧。不要把包含写操作、网络请求或日志依赖的行为随意放进 Predicate。

```java
Predicate<String> acceptable = hasText
        .and(longEnough)
        .or(value -> "admin".equals(value));
Predicate<String> ordinary = acceptable.negate();
```

### 4.3 `isEqual` 与 `not`

```java
Predicate<String> isJava = Predicate.isEqual("Java");
Predicate<String> isNotJava = isJava.negate();

// Java 11+：Predicate.not(String::isBlank)
List<String> values = List.of("Java", "", "Stream");
List<String> nonBlank = values.stream()
        .filter(Predicate.not(String::isBlank))
        .toList();
```

`Predicate.isEqual(target)` 对 `null` 有安全处理，适合把相等判断作为可组合策略；Java 11 的 `Predicate.not` 用于方法引用取反。

---

## 5. Consumer 消费动作

### 5.1 `Consumer<T>`

`Consumer<T>` 接收一个参数并产生副作用，抽象方法为 `void accept(T t)`，典型用途是打印、持久化、发送消息和执行回调。

```java
Consumer<String> printer = System.out::println;
Consumer<User> audit = user -> auditLog("user=" + user.id());

static void auditLog(String message) {
    System.out.println(message);
}
```

Consumer 的返回值是 `void`，不要把计算结果偷偷塞进外部可变变量，否则调用方很难判断状态来源。

### 5.2 `andThen`

```java
Consumer<String> validate = value -> System.out.println("validate: [" + value + "]");
Consumer<String> save = value -> System.out.println("save: [" + value + "]");
Consumer<String> pipeline = validate.andThen(save);

pipeline.accept("  hello  ");
// validate: [  hello  ]
// save: [  hello  ]
```

> ⚠️ **`andThen` 串起来的是"两个都收到同一个入参"，不是"前一个的输出喂给后一个"**。`Consumer` 返回 `void`，根本没有输出可传递。所以下面这种写法是个静默失效的陷阱：
>
> ```java
> Consumer<String> trimPrint = String::trim;      // 合法但毫无作用：trim 结果被丢弃
> trimPrint.andThen(save).accept("  hello  ");
> // 实测输出：save: [  hello  ]  ← 空格仍在，trim 白做了
> ```
>
> 想让"处理结果"往下传，必须用 `Function.andThen`：
>
> ```java
> Function<String, String> trim = String::trim;
> trim.andThen(value -> { System.out.println("save: [" + value + "]"); return value; })
>     .apply("  hello  ");                        // save: [hello]
> ```

执行顺序是先前者后后者；如果前者抛异常，后者不会执行。`Consumer.andThen(null)` 会在**组合时**（而非调用时）就抛 `NullPointerException`。

### 5.3 `BiConsumer`

```java
BiConsumer<String, Integer> put = (key, value) -> System.out.println(key + "=" + value);
Map<String, Integer> scores = new HashMap<>();
scores.forEach((key, value) -> System.out.println(key + ":" + value));
```

---

## 6. Supplier 延迟提供

### 6.1 `Supplier<T>`

`Supplier<T>` 不接收参数，只在调用 `get()` 时提供一个 `T`。它常用于工厂、默认值、延迟日志、重试任务和依赖注入。

```java
Supplier<Instant> clock = Instant::now;
Supplier<List<String>> newList = ArrayList::new;

List<String> values = newList.get();
```

Supplier 表示“如何获得值”，不是“值已经计算好了”。

```java
static String eagerDefault() {
    System.out.println("default computed");
    return "default";
}

static String choose(String value, Supplier<String> fallback) {
    return value != null ? value : fallback.get();
}

String result = choose("actual", () -> eagerDefault()); // fallback 不执行
```

对比 `orElse` 与 `orElseGet`：

```java
Optional<String> optional = Optional.of("actual");
String a = optional.orElse(eagerDefault());       // 参数先求值，仍会打印
String b = optional.orElseGet(() -> eagerDefault()); // Supplier 仅在空时执行
```

### 6.2 `BooleanSupplier` 等零参数特化

```java
BooleanSupplier ready = () -> System.currentTimeMillis() > 0;
IntSupplier randomInt = () -> ThreadLocalRandom.current().nextInt();
LongSupplier timestamp = System::currentTimeMillis;
DoubleSupplier ratio = () -> 0.75d;
```

---

## 7. Bi 接口与多参数行为

### 7.1 常见 Bi 接口

| 接口 | 函数描述符 | 常见场景 |
|---|---|---|
| `BiFunction<T,U,R>` | `(T,U) -> R` | 两个对象转换为结果 |
| `BiConsumer<T,U>` | `(T,U) -> void` | Map 写入、二元回调 |
| `BiPredicate<T,U>` | `(T,U) -> boolean` | 比较、权限判断 |
| `BinaryOperator<T>` | `(T,T) -> T` | 同类型合并 |

```java
BiFunction<Integer, Integer, Integer> add = Integer::sum;
BiPredicate<String, String> startsWith = String::startsWith;
BiConsumer<String, Integer> logPair = (key, value) -> System.out.println(key + "=" + value);
```

JDK 只提供到 `Bi` 的标准接口。三参数及以上通常应设计领域对象、记录类或自定义接口，而不是创建 `TriFunction` 体系导致参数语义消失。

### 7.2 `BiFunction.andThen`

```java
BiFunction<Integer, Integer, Integer> add = Integer::sum;
Function<Integer, String> format = value -> "total=" + value;
BiFunction<Integer, Integer, String> formatted = add.andThen(format);

System.out.println(formatted.apply(2, 3));
```

`BiFunction` 没有 `compose`，因为它只有一个 `andThen` 结果方向；如需对两个输入分别预处理，可先定义两个 `Function`，再用 Lambda 组装。

```java
Function<String, Integer> parse = Integer::parseInt;
BiFunction<String, String, Integer> sum = (left, right) ->
        parse.apply(left.trim()) + parse.apply(right.trim());
```

---

## 8. Operator 运算接口

### 8.1 一元与二元 Operator

Operator 是 Function 的同类型特化：

| 接口 | 等价关系 | 函数描述符 |
|---|---|---|
| `UnaryOperator<T>` | `Function<T,T>` | `T -> T` |
| `BinaryOperator<T>` | `BiFunction<T,T,T>` | `(T,T) -> T` |

```java
UnaryOperator<String> normalize = value -> value.trim().toLowerCase(Locale.ROOT);
BinaryOperator<Integer> max = Integer::max;

System.out.println(normalize.apply(" Java "));
System.out.println(max.apply(10, 20));
```

### 8.2 `minBy` 与 `maxBy`

```java
BinaryOperator<User> younger = BinaryOperator.minBy(
        Comparator.comparingInt(User::age));

User selected = younger.apply(new User(1, "A", 20), new User(2, "B", 18));
```

```java
record User(long id, String name, int age) {}
```

`minBy` 返回的是一个二元运算策略，不会改变输入对象；比较器相等时返回第一个参数。比较器本身必须满足稳定、可传递的比较约定。

---

## 9. 原始类型特化接口

### 9.1 为什么需要特化

泛型不能直接使用基本类型。`Function<Integer, Integer>` 在计算过程中可能产生装箱和拆箱；数据量大或处于紧循环时，应考虑 `IntFunction`、`ToIntFunction`、`IntUnaryOperator` 等特化接口。

```java
Function<Integer, Integer> boxedSquare = value -> value * value;
IntUnaryOperator primitiveSquare = value -> value * value;
ToIntFunction<String> length = String::length;
IntFunction<String> label = value -> "value=" + value;
```

### 9.2 特化接口分类

| 方向 | 常见接口 | 描述 |
|---|---|---|
| 基本类型输入 | `IntFunction<R>`、`LongFunction<R>`、`DoubleFunction<R>` | `int -> R` |
| 基本类型输出 | `ToIntFunction<T>`、`ToLongFunction<T>`、`ToDoubleFunction<T>` | `T -> int/long/double` |
| 基本类型到基本类型 | `IntUnaryOperator`、`IntBinaryOperator` | 避免两端装箱 |
| 基本类型判断 | `IntPredicate`、`LongPredicate`、`DoublePredicate` | `primitive -> boolean` |
| 基本类型消费 | `IntConsumer`、`LongConsumer`、`DoubleConsumer` | 基本类型 -> void |
| 基本类型提供 | `IntSupplier`、`LongSupplier`、`DoubleSupplier` | `() -> primitive` |

### 9.3 特化不是无条件更快

| 选择 | 优点 | 代价 |
|---|---|---|
| 泛型接口 | API 统一、泛型组合方便 | 可能装箱；表达语义更通用 |
| 原始特化 | 减少基本类型装箱 | 类型数量更多，组合边界更窄 |

在 Stream 中，`mapToInt` 通常比 `map(...).mapToInt(...)` 更直接：

```java
int total = users.stream()
        .mapToInt(User::age)
        .sum();
```

不要仅凭接口名称做性能断言；应结合分配、数据规模、JIT 和基准测试验证。

---

## 10. 组合方法与执行顺序

### 10.1 组合关系总表

| 接口 | 组合方法 | 逻辑/执行顺序 |
|---|---|---|
| `Function` | `compose` | 参数函数先执行 |
| `Function` | `andThen` | 当前函数先执行 |
| `Predicate` | `and` | 左真才执行右侧 |
| `Predicate` | `or` | 左假才执行右侧 |
| `Predicate` | `negate` | 结果取反 |
| `Consumer` | `andThen` | 前者先执行，成功后执行后者 |
| `BiFunction` | `andThen` | 二元函数完成后转换结果 |

### 10.2 用日志验证顺序

```java
Function<String, String> first = value -> {
    System.out.println("first");
    return value.trim();
};
Function<String, String> second = value -> {
    System.out.println("second");
    return value.toUpperCase(Locale.ROOT);
};

String result = first.andThen(second).apply(" java ");
// first
// second
// result = JAVA
```

组合函数应该尽量保持纯粹；如果每一步都修改外部集合、依赖时间或访问网络，执行顺序和异常传播会变得难以推断。

### 10.3 空参数和异常传播

```java
Function<String, String> trim = String::trim;
// trim.andThen(null); // NullPointerException

Predicate<String> valid = value -> value.length() > 0;
// valid.test(null);   // 由 Lambda 内部代码抛出 NPE，而不是 Predicate 自动兜底
```

组合 API 不会自动提供空安全。空值策略应在入口统一处理，或使用 `Optional`/明确的业务校验器表达。

---

## 11. 自定义函数式接口设计

### 11.1 何时不直接用 JDK 接口

优先使用标准接口，但以下情况值得自定义：

1. 方法名需要表达业务语义，例如 `PriceRule#discount`，比 `Function#apply` 更清晰。
2. 需要声明受检异常。
3. 需要携带领域文档、默认方法或泛型边界。
4. 需要避免调用方把一个通用函数误用于错误业务位置。

```java
@FunctionalInterface
interface PriceRule {
    BigDecimal discount(Order order, Customer customer);
}

@FunctionalInterface
interface CheckedParser<T> {
    T parse(String text) throws ParseException;
}
```

### 11.2 设计清单

| 设计项 | 建议 |
|---|---|
| 抽象方法 | 只有一个；命名使用业务动词 |
| 泛型 | 明确输入/输出边界，避免无意义的多层通配符 |
| 异常 | 统一声明或提供适配器，不要让调用方猜测异常 |
| 默认方法 | 只放稳定、通用的组合逻辑 |
| 可变性 | 文档说明传入对象是否会被修改 |
| 并发 | 说明实现是否要求线程安全 |
| 命名 | 用 `Rule`、`Resolver`、`Factory`、`Handler` 等体现角色 |

### 11.3 自定义接口的组合

```java
@FunctionalInterface
interface Rule<T> {
    boolean matches(T value);

    default Rule<T> and(Rule<? super T> other) {
        Objects.requireNonNull(other);
        return value -> matches(value) && other.matches(value);
    }

    default Rule<T> negate() {
        return value -> !matches(value);
    }
}

Rule<String> nonBlank = value -> value != null && !value.isBlank();
Rule<String> shortText = value -> value.length() <= 20;
Rule<String> accepted = nonBlank.and(shortText);
```

---

## 12. 选型速查与练习

### 12.1 选型表

| 需求 | 首选接口 | 函数描述符 |
|---|---|---|
| 一个输入转换成一个输出 | `Function<T,R>` | `R apply(T)` |
| 一个输入判断真假 | `Predicate<T>` | `boolean test(T)` |
| 一个输入触发动作 | `Consumer<T>` | `void accept(T)` |
| 无输入延迟提供值 | `Supplier<T>` | `T get()` |
| 两个输入计算结果 | `BiFunction<T,U,R>` | `R apply(T,U)` |
| 两个输入判断 | `BiPredicate<T,U>` | `boolean test(T,U)` |
| 同类型输入输出 | `UnaryOperator<T>` | `T apply(T)` |
| 两个同类型值合并 | `BinaryOperator<T>` | `T apply(T,T)` |
| 高强度基本类型计算 | `Int/Long/Double*` | 避免不必要装箱 |
| 受检异常或业务语义 | 自定义接口 | 明确契约 |

### 12.2 常见误区

| 误区 | 正确认识 |
|---|---|
| `@FunctionalInterface` 是运行时必需 | 它主要提供编译期校验 |
| 一个接口只能有一个方法 | 只要求一个抽象实例方法；可有 default/static 方法 |
| `Function` 就能表示任意多参数 | JDK 标准只到 `BiFunction`，更多参数应建模 |
| `Predicate.and` 会执行两侧 | 有短路；左侧结果可能阻止右侧执行 |
| Supplier 一创建就计算值 | `get()` 调用时才执行 Lambda 体 |
| 原始特化接口一定更快 | 需结合调用频率和基准测试判断 |
| Consumer 不能修改对象 | 可以，但副作用必须写清楚并控制线程模型 |

### 12.3 动手练习

1. 分别用 `Function`、`UnaryOperator` 和 `IntUnaryOperator` 实现字符串/整数规范化，比较函数描述符。
2. 写一个三阶段 `Function` 流程，打印 `compose` 和 `andThen` 的顺序。
3. 用 `Predicate` 实现“非空且长度不超过 20，或管理员账号”的校验，并验证短路。
4. 定义带受检异常的 `CheckedFunction<T,R>`，实现转换为标准 `Function` 的适配器。
5. 设计一个订单折扣 `Rule` 接口，提供 `and`、`or`、`negate` 组合，并说明副作用约束。

> 🎯 **模块结论**：先根据函数描述符选接口，再根据语义选择组合方式；标准接口解决通用性，自定义接口解决业务可读性、异常和契约边界。

---

**上一模块**：[01-Lambda语法精讲与变量捕获](./01-Lambda语法精讲与变量捕获.md)  
**下一模块**：[03-Lambda方法引用深度解析](./03-Lambda方法引用深度解析.md)  
**返回总览**：[00-Lambda表达式知识体系总览](./00-Lambda表达式知识体系总览.md)
