# 06 Lambda 面试高频题精讲

> 一句话定位：用“结论—原理—边界—示例”的方式回答 Lambda 高频题，把语法、函数式接口、方法引用、字节码和工程取舍串成可追问的面试答案。

---

## 📚 目录

1. [面试回答框架](#1-面试回答框架)
2. [基础语法与捕获](#2-基础语法与捕获)
3. [函数式接口与方法引用](#3-函数式接口与方法引用)
4. [底层原理与性能](#4-底层原理与性能)
5. [工程陷阱与协同](#5-工程陷阱与协同)
6. [场景设计题](#6-场景设计题)
7. [重点题追问与代码推演](#7-重点题追问与代码推演)
8. [易错代码题](#8-易错代码题)
9. [速记表与分层自测](#9-速记表与分层自测)

---

## 1. 面试回答框架

### 1.1 四步法

回答 Lambda 题目时可按以下顺序组织：

1. **先给结论**：避免绕圈。
2. **解释机制**：说明目标类型、函数描述符或运行时链接。
3. **给最小例子**：用 5～10 行代码验证。
4. **补边界与工程取舍**：说明异常、线程安全、版本或性能限制。

例如“Lambda 是匿名内部类吗？”可以这样答：

> 不是。两者都能表达回调，但 Lambda 必须转换为函数式接口，编译器通常生成 `invokedynamic` 调用点，运行时由 `LambdaMetafactory` 链接；匿名内部类则有独立匿名类语义。最直观的差异是 Lambda 的 `this` 指向外部实例，而匿名类的 `this` 指向匿名对象。具体实例是否复用由运行时决定，不能依赖身份比较。

---

## 2. 基础语法与捕获

### 2.1 Lambda 必须依赖目标类型

**题 1：为什么 `var f = x -> x + 1` 编译失败？**

**结论**：Lambda 没有独立的函数类型，必须由上下文提供函数式接口目标类型。

```java
Function<Integer, Integer> f = x -> x + 1;
// var f = x -> x + 1; // ❌ 没有目标类型
```

`Function<Integer,Integer>` 和 `ToIntFunction<Integer>` 的函数描述符不同，编译器不能凭箭头表达式自行选择。方法参数、返回值、赋值或显式强转都可以提供目标类型。

### 2.2 什么是函数式接口

**题 2：函数式接口能有 default 方法和 static 方法吗？**

**结论**：可以；只要最终只有一个抽象实例方法即可。

```java
@FunctionalInterface
interface Action {
    void run();
    default String name() { return "action"; }
    static Action empty() { return () -> {}; }
}
```

从 `Object` 继承的 `equals`、`hashCode`、`toString` 不会额外破坏 SAM。继承多个接口时，签名相同的抽象方法可合并，签名不同则不能作为函数式接口。

### 2.3 effectively final 是什么

**题 3：为什么 Lambda 捕获的局部变量必须 final 或 effectively final？**

**结论**：局部变量未显式声明 `final` 也可以捕获，但它从初始化后不能再被赋值。

```java
int offset = 10;
Function<Integer, Integer> add = value -> value + offset;
// offset++; // ❌ 重新赋值后不能捕获
```

Java 采用捕获值的模型，Lambda 可能在方法返回后执行；要求变量绑定稳定可避免“栈变量还是副本”的歧义。它不等于对象不可变，也不等于线程安全。

### 2.4 Lambda 中 this 指向谁

**题 4：Lambda 有没有自己的 `this`？**

**结论**：没有。Lambda 复用外部类实例的 `this`；匿名内部类才产生新的对象作用域。

```java
class Outer {
    Runnable lambda() {
        return () -> System.out.println(this.getClass());
    }
    Runnable anonymous() {
        return new Runnable() {
            public void run() { System.out.println(this.getClass()); }
        };
    }
}
```

`this::method` 按当前对象进行虚分派，`super::method` 绑定父类实现。需要注意 Lambda 捕获外部 `this` 可能延长对象生命周期。

### 2.5 表达式体和语句块体的返回规则

**题 5：为什么块体 Lambda 有时必须写 return？**

**结论**：表达式体隐式返回；语句块体需要显式 `return`，并保证所有正常路径返回兼容值。

```java
Function<Integer, String> f = n -> {
    if (n > 0) {
        return "positive";
    }
    return "not positive";
};
```

无返回值的 `Consumer` 可以写多条语句，但不能返回一个值。Lambda 内的 `return` 只结束当前 Lambda 调用，不会返回外部方法。

---

## 3. 函数式接口与方法引用

### 3.1 四大核心接口如何选

**题 6：`Function`、`Predicate`、`Consumer`、`Supplier` 的区别？**

| 接口 | 输入 | 输出 | 抽象方法 | 典型用途 |
|---|---|---|---|---|
| `Function<T,R>` | 1 | 1 | `R apply(T)` | 转换 |
| `Predicate<T>` | 1 | boolean | `boolean test(T)` | 判断/过滤 |
| `Consumer<T>` | 1 | void | `void accept(T)` | 副作用动作 |
| `Supplier<T>` | 0 | 1 | `T get()` | 延迟提供/工厂 |

```java
Function<String, Integer> length = String::length;
Predicate<String> nonBlank = text -> !text.isBlank();
Consumer<String> print = System.out::println;
Supplier<Instant> clock = Instant::now;
```

### 3.2 `compose` 和 `andThen` 顺序

**题 7：`compose` 与 `andThen` 有何区别？**

**结论**：`f.compose(g)` 执行 `g` 后执行 `f`；`f.andThen(g)` 执行 `f` 后执行 `g`。

```java
Function<String, String> trim = String::trim;
Function<String, Integer> parse = Integer::parseInt;
int value = parse.compose(trim).apply(" 42 ");
```

面试时可写成数学形式：`compose` 是 `f(g(x))`，`andThen` 是 `g(f(x))`。

### 3.3 Predicate 组合是否短路

**题 8：`Predicate.and` 和 `or` 会执行两侧吗？**

**结论**：会遵循布尔短路。`and` 左侧为 false 时不执行右侧；`or` 左侧为 true 时不执行右侧。

```java
Predicate<String> text = value -> value != null && !value.isBlank();
Predicate<String> longText = value -> value.length() > 10;
Predicate<String> valid = text.and(longText);
```

如果两侧都有日志、计数或 IO，实际执行次数必须纳入设计和测试。

### 3.4 方法引用的四种形式

**题 9：方法引用有哪四类？**

| 类型 | 写法 | 等价形式 |
|---|---|---|
| 静态方法 | `Type::staticMethod` | `x -> Type.staticMethod(x)` |
| 绑定实例 | `object::method` | `x -> object.method(x)` |
| 未绑定实例 | `Type::method` | `(obj,x) -> obj.method(x)` |
| 构造器 | `Type::new` | `x -> new Type(x)` |

```java
Function<String, Integer> a = Integer::parseInt;
Consumer<String> b = System.out::println;
Function<String, String> c = String::trim;
Supplier<StringBuilder> d = StringBuilder::new;
```

### 3.5 为什么 `obj::method` 可能提前 NPE

**题 10：绑定实例方法引用和 Lambda 的求值时机一样吗？**

**结论**：不一定。绑定引用创建时会先求值接收者；Lambda 通常在调用函数体时才访问接收者。

```java
String value = null;
// Supplier<Integer> ref = value::length; // 创建引用时 NPE
Supplier<Integer> lambda = () -> value.length(); // 创建成功，get 时 NPE
```

当接收者表达式有副作用、可能为 null 或可能变化时，不要机械改成方法引用。

### 3.6 方法引用为什么需要目标类型

**题 11：`var ref = String::trim` 为什么不行？**

**结论**：方法引用和 Lambda 一样是 poly expression，没有独立类型；目标类型负责确定参数和返回签名。

```java
Function<String, String> ref = String::trim;
// var ref = String::trim; // ❌
```

重载方法或静态/实例两条适配路径都匹配时，可以显式转型，或者改写为带类型参数的 Lambda。

---

## 4. 底层原理与性能

### 4.1 Lambda 是否生成 class 文件

**题 12：Lambda 会生成 `Outer$1.class` 吗？**

**结论**：通常不会像匿名内部类那样在编译期生成 `Outer$1.class`；Java 8+ 通常编译为 `invokedynamic`，运行到调用点时由 `LambdaMetafactory` 负责链接实现。

但不要绝对化：编译器可生成 `lambda$...` 合成方法，运行时可能生成隐藏实现类，具体翻译策略属于实现细节。

### 4.2 invokedynamic 的作用

**题 13：`invokedynamic` 在 Lambda 中解决什么问题？**

**结论**：它提供一个动态调用点，把“如何创建函数式接口实例”的决策推迟到运行时。

流程通常是：

1. `javac` 写入 `invokedynamic` 指令。
2. 常量池的 BootstrapMethods 指向 `LambdaMetafactory`。
3. 首次执行时，JVM 调用 bootstrap 方法建立 `CallSite`。
4. 后续执行使用已链接调用点。

这样 JDK 可以在不改变业务源码的情况下优化实现类生成和实例复用。

### 4.3 捕获型与非捕获型 Lambda

**题 14：非捕获 Lambda 一定是单例吗？**

**结论**：不是语言规范保证。HotSpot 当前实现通常会复用非捕获 Lambda，但代码不能依赖 `==` 或对象身份。

```java
Runnable first = () -> {};
Runnable second = () -> {};
// 即便某些场景 identity 相同，也不应以此写逻辑
```

捕获型 Lambda 需要携带捕获值，通常每次求值可能创建不同实例。实际性能还取决于 JIT 内联、逃逸分析和调用场景。

### 4.4 方法引用一定比 Lambda 快吗

**题 15：`String::trim` 比 `s -> s.trim()` 快吗？**

**结论**：不能这样概括。语义等价时两者通常都能被 JIT 内联，差异往往不可测；真正需要关注的是捕获、装箱、重复分配和算法复杂度。

微基准应使用 JMH，避免用一次 `System.nanoTime()` 得出结论。

### 4.5 `LambdaMetafactory` 的三个 MethodType

**题 16：`samMethodType`、`implMethod`、`instantiatedMethodType` 分别是什么？**

- `samMethodType`：函数式接口抽象方法擦除后的签名。
- `implMethod`：实际要调用的方法句柄。
- `instantiatedMethodType`：泛型实例化后调用方看到的签名。

JVM 可能在适配过程中插入类型转换、装箱拆箱或返回值适配。方法引用文档中的 `javap -v` 实验可用于查看这些信息。

---

## 5. 工程陷阱与协同

### 5.1 受检异常怎么处理

**题 17：为什么 `Function<String,String>` 不能直接引用 `throws IOException` 的方法？**

**结论**：`Function.apply` 没有声明受检异常，方法引用必须符合目标函数描述符的 `throws` 边界。

```java
@FunctionalInterface
interface CheckedFunction<T, R> {
    R apply(T value) throws IOException;
}
```

工程上可定义 Checked 接口，或在边界处把 IOException 包装为 `UncheckedIOException`；必须保留原始 cause，不能空 catch。

### 5.2 Optional 的误用

**题 18：为什么不建议把 Optional 作为实体字段和所有参数？**

**结论**：Optional 主要用于表达方法返回值可能缺失；作为字段会增加序列化、框架绑定和存储映射复杂度，作为参数也不一定比可空值和明确重载更清晰。

```java
String display = Optional.ofNullable(user)
        .map(User::name)
        .orElse("unknown");
```

避免 `optional.get()` 链和把 Optional 当集合泛滥嵌套。

### 5.3 Stream 中能否修改外部集合

**题 19：为什么 `forEach(x -> list.add(x))` 容易出问题？**

**结论**：它引入外部可变状态；并行流下可能线程不安全，顺序流下也让数据流意图不清。

```java
List<String> names = users.stream()
        .map(User::name)
        .toList();
```

需要归约时使用 `collect`、`groupingBy` 或自定义 Collector；不要为了少写几行而隐藏副作用。

### 5.4 parallelStream 适合什么场景

**题 20：parallelStream 是否总比 stream 快？**

**结论**：不是。它适合数据量足够、任务独立、CPU 密集且并行开销可摊薄的场景；IO、共享状态、小数据和顺序敏感逻辑通常不适合。

还要注意默认公共 ForkJoinPool 与应用其他任务竞争，必要时应设计隔离执行器或改用显式 CompletableFuture 编排。

### 5.5 CompletableFuture 中 thenApply 与 thenCompose

**题 21：两者区别是什么？**

```java
CompletableFuture<UserView> view = userFuture.thenApply(User::toView);
CompletableFuture<Profile> profile = userFuture.thenCompose(
        user -> profileService.loadAsync(user.id()));
```

`thenApply` 做普通结果转换，返回 `CompletableFuture<R>`；`thenCompose` 接收返回 Future 的函数并扁平化嵌套，适合异步链。不要在异步 Lambda 中随意 `join()` 阻塞线程。

---

## 6. 场景设计题

### 6.1 设计可组合校验器

**题 22：如何设计一个支持组合和短路的业务规则接口？**

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
```

回答时补充：规则是否纯函数、失败原因如何保留、是否需要异步校验、调用顺序是否稳定，以及如何为每条规则打点。

### 6.2 设计重试模板

**题 23：如何用 Lambda 抽取重试的变化点？**

把“执行动作”抽成 `CheckedSupplier<T>`，模板统一次数、退避、可重试异常和最终异常；不要让 Lambda 决定全局重试策略。

```java
static <T> T retry(CheckedSupplier<T> action, int attempts) throws Exception {
    Exception last = null;
    for (int i = 0; i < attempts; i++) {
        try {
            return action.get();
        } catch (Exception ex) {
            last = ex;
        }
    }
    throw last;
}
```

生产版本还应有最大次数校验、指数退避、幂等性约束、超时和取消。

### 6.3 如何避免 Lambda 捕获大对象

**题 24：Lambda 只访问一个字段，为什么仍可能保留整个外部对象？**

因为 Lambda 捕获的通常是外部 `this`，而不是自动复制被访问字段。长生命周期的监听器、线程任务和缓存可能因此延长外部实例生命周期。

```java
class Service {
    private final String tenantId = "t1";

    Runnable task() {
        String localTenantId = tenantId;
        return () -> System.out.println(localTenantId);
    }
}
```

把需要的不可变值复制为局部变量，可减少不必要的对象关联；同时要评估对象生命周期和取消机制。

### 6.4 什么时候自定义接口胜过 Function

**题 25：为什么不把所有业务行为都写成 Function？**

`Function#apply` 过于通用，无法表达异常、线程安全、输入是否会被修改、失败语义和领域角色。自定义 `PriceRule`、`CommandHandler`、`CheckedParser` 等接口，可以通过命名、文档和默认方法固化契约。

---

## 7. 重点题追问与代码推演

### 7.1 目标类型追问

**追问**：如果 Lambda 没有独立类型，为什么 `Object value = (Runnable) () -> {};` 可以编译？

**答**：先由显式强制转换把 Lambda 赋予 `Runnable` 目标类型，再把创建出的 `Runnable` 对象向上转型为 `Object`。不能直接写 `Object value = () -> {};`，因为 `Object` 不是函数式接口。

```java
Object value = (Runnable) () -> System.out.println("run");
Runnable task = (Runnable) value;
task.run();
```

### 7.2 effectively final 追问

**追问**：把局部变量放进单元素数组，是否就解决了 Lambda 捕获限制？

**答**：只绕过了变量重新赋值的编译检查，没有解决共享可变状态的线程安全问题。顺序执行可以使用，但并行流或异步任务应使用归约、Collector 或明确的并发容器，并说明状态所有权。

```java
int[] total = {0};
List.of(1, 2, 3).forEach(value -> total[0] += value);
// total[0] 是数组内容变化，变量 total 本身没有重新赋值
```

### 7.3 `this` 语义追问

**追问**：Lambda 访问实例字段时，能否说它只捕获这个字段而不捕获外部对象？

**答**：通常不能这样说。访问实例字段意味着需要外部实例作为接收者，Lambda 往往捕获外围 `this`；长生命周期回调因此可能延长整个对象的生命周期。若只需稳定值，可先复制到局部变量再捕获。

```java
class Service {
    private final String tenantId = "t1";

    Runnable callback() {
        String snapshot = tenantId;
        return () -> System.out.println(snapshot);
    }
}
```

### 7.4 `invokedynamic` 追问

**追问**：`invokedynamic` 是不是每次执行 Lambda 都重新生成一个实现类？

**答**：不是。调用点通常在首次执行时由 bootstrap 方法链接，后续复用已经链接的调用点。非捕获 Lambda 当前实现通常返回可复用实例；捕获 Lambda 的目标句柄通常会根据捕获参数创建实例。类生成策略和实例身份都不是业务代码可依赖的规范承诺。

### 7.5 非捕获 Lambda 追问

**追问**：既然非捕获 Lambda 常被复用，能否用 `==` 判断两个 Lambda 是否相同？

**答**：不能。`LambdaMetafactory` 不保证实例 identity；同一源码表达式、不同调用点或不同 JVM 实现都可能产生不同对象。需要判断业务行为时使用显式枚举、策略标识或 `equals` 语义对象。

```java
Runnable first = () -> {};
Runnable second = () -> {};
// 不要写 if (first == second) { ... }
```

### 7.6 绑定方法引用 NPE 追问

**追问**：`object::method` 和 `() -> object.method()` 什么时候不能互换？

**答**：当接收者可能为 `null`、接收者表达式有副作用，或接收者变量可能重新指向其他对象时不能机械互换。绑定方法引用创建时会固定并检查接收者；Lambda 通常在执行函数体时才读取接收者。

```java
String value = null;
// Supplier<Integer> ref = value::length; // 创建时抛 NPE
Supplier<Integer> lambda = () -> value.length(); // get 时抛 NPE
```

### 7.7 受检异常追问

**追问**：为什么不能给 `Function<String, String>` 直接传入 `throws IOException` 的方法引用？

**答**：因为 `Function.apply` 没有声明受检异常，目标函数描述符不允许实现方法向外抛出 `IOException`。可以自定义声明异常的函数式接口，或在边界处捕获并包装为 `UncheckedIOException`，同时保留原始 cause。

```java
@FunctionalInterface
interface CheckedFunction<T, R> {
    R apply(T value) throws IOException;
}
```

### 7.8 `parallelStream` 追问

**追问**：把 `stream()` 改成 `parallelStream()` 是否就能提升性能？

**答**：不一定。只有任务独立、计算密集、数据规模足够且拆分/合并开销可摊薄时才可能收益。IO、共享可变状态、小集合和顺序敏感逻辑通常不适合；还要注意公共 ForkJoinPool 的资源竞争，并用基准测试验证。

```java
long total = values.parallelStream()
        .mapToLong(this::cpuHeavyCalculation)
        .sum();
```

> 🎯 **追问收束**：重点不是背“能不能用 Lambda”，而是能否说明目标类型、捕获边界、异常契约、运行时实现和并发代价。

---

## 8. 易错代码题

### 7.1 推断输出

```java
List<Runnable> tasks = new ArrayList<>();
for (int i = 0; i < 3; i++) {
    int current = i;
    tasks.add(() -> System.out.print(current));
}
tasks.forEach(Runnable::run);
```

**答案**：输出 `012`。每轮创建了新的 effectively final 局部变量 `current`。

### 7.2 识别编译错误

```java
int count = 0;
Runnable task = () -> System.out.println(count);
count++;
```

**答案**：编译失败。`count` 被 Lambda 捕获后又重新赋值，不再是 effectively final。可改为在赋值完成后复制到新变量，或使用明确的并发状态容器。

### 7.3 `orElse` 求值

```java
Optional<String> value = Optional.of("hit");
String result = value.orElse(expensive());
```

**答案**：`expensive()` 仍然会执行，因为普通方法参数在调用 `orElse` 前先求值；需要惰性计算时使用 `orElseGet(YourClass::expensive)`。

### 7.4 方法引用参数

```java
BiPredicate<String, String> starts = String::startsWith;
```

**答案**：第一个参数是接收者，等价于 `(left, prefix) -> left.startsWith(prefix)`。

### 7.5 识别副作用问题

```java
List<Integer> result = new ArrayList<>();
values.parallelStream().forEach(result::add);
```

**答案**：存在并发安全和顺序问题。使用 `values.parallelStream().toList()`，或使用正确的 Collector；是否并行还需基准验证。

---

## 9. 速记表与分层自测

### 8.1 速记表

| 主题 | 一句话 |
|---|---|
| Lambda | 以函数式接口为目标类型的行为表达式 |
| SAM | 最终只有一个抽象实例方法 |
| 目标类型 | 决定参数、返回值和重载含义 |
| 捕获 | 局部变量必须 final/effectively final |
| `this` | Lambda 使用外围实例的 this |
| `Function` | 输入转换输出 |
| `Predicate` | 输入判断真假 |
| `Consumer` | 输入触发动作 |
| `Supplier` | 无输入延迟提供 |
| `compose` | 先执行参数函数 |
| `andThen` | 先执行当前函数 |
| `Class::method` | 未绑定实例时首参数是接收者 |
| `invokedynamic` | 运行期链接 Lambda 调用点 |
| 非捕获实例 | 可能复用，但不保证身份 |
| Stream | 惰性流水线，避免共享副作用 |

### 8.2 初级自测

1. Lambda 能否赋给 `Object`？为什么？
2. `@FunctionalInterface` 的作用是什么？
3. `Predicate.and` 是否短路？
4. `Supplier` 什么时候执行函数体？
5. `Function.compose` 的顺序是什么？

### 8.3 中级自测

1. 解释 `String::length` 的隐式首参数。
2. 说明绑定方法引用和 Lambda 的 NPE 时机差异。
3. 给出受检异常方法适配到标准 `Function` 的一种方案。
4. 解释 `thenApply` 和 `thenCompose`。
5. 说明为什么并行流不应修改普通 `ArrayList`。

### 8.4 高级自测

1. 解释 `invokedynamic`、BootstrapMethod 和 `LambdaMetafactory` 的关系。
2. 对比捕获型和非捕获型 Lambda 的实例化差异。
3. 解释 `samMethodType` 与 `instantiatedMethodType` 的职责。
4. 设计可观测、可取消的 Lambda 重试模板。
5. 说明 Lambda 捕获外部 `this` 对生命周期的影响。

> 🎯 **最终复盘**：面试不要只背“Lambda 是语法糖”。高质量回答应同时说明目标类型、函数描述符、捕获语义、运行时实现和工程边界，并用一个最小例子证明结论。

---

**上一模块**：[05-Lambda实战模式与Stream协同](./05-Lambda实战模式与Stream协同.md)  
**学习闭环**：回到 [00-Lambda表达式知识体系总览](./00-Lambda表达式知识体系总览.md) 按路线复习  
**返回总览**：[00-Lambda表达式知识体系总览](./00-Lambda表达式知识体系总览.md)
