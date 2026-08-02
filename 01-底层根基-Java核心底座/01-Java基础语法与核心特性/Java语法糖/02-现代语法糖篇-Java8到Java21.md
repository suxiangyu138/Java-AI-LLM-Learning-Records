# 02 现代语法糖篇 — Java 8 到 Java 21

> Lambda、Stream、Record、模式匹配、虚拟线程——Java 8 后的语法糖让Java从"啰嗦的语言"变成了"现代化的语言"

## 📚 目录

1. [Lambda 表达式 (Java 8)](#1-lambda-表达式-java-8)
2. [Stream API (Java 8)](#2-stream-api-java-8)
3. [Optional (Java 8)](#3-optional-java-8)
4. [局部变量类型推断 var (Java 10)](#4-局部变量类型推断-var-java-10)
5. [Switch 表达式 + 模式匹配 (Java 14/17/21)](#5-switch-表达式--模式匹配-java-141721)
6. [Record 类 (Java 16)](#6-record-类-java-16)
7. [文本块 Text Blocks (Java 15)](#7-文本块-text-blocks-java-15)
8. [字符串模板 String Templates (Java 21 Preview)](#8-字符串模板-string-templates-java-21-preview)
9. [虚拟线程 Virtual Threads (Java 21)](#9-虚拟线程-virtual-threads-java-21)
10. [模式匹配 for instanceof (Java 16)](#10-模式匹配-for-instanceof-java-16)

---

## 1. Lambda 表达式 (Java 8)

### 1.1 Lambda 不是匿名内部类的语法糖

很多面试者会说"Lambda是匿名内部类的语法糖"——**这是错误的**。二者在底层实现上有本质区别。

| 对比项 | 匿名内部类 | Lambda 表达式 |
|--------|-----------|---------------|
| 编译产物 | 生成独立 `ClassName$n.class` 文件 | 不生成独立 class 文件 |
| `this` 引用 | 指向匿名内部类实例 | 指向外部类实例 |
| 作用域 | 内部类可以定义自己的局部变量 | 与外部方法共享作用域 |
| 捕获变量 | 对成员变量无限制 | 必须 effectively final |
| 底层实现 | `new` 指令创建对象 | `invokedynamic` 指令动态生成 |

```java
// 匿名内部类 —— 编译后会产生 Extra$1.class 文件
Runnable r1 = new Runnable() {
    @Override
    public void run() {
        System.out.println(this.getClass()); // class Main$1
    }
};

// Lambda —— 编译后无额外 class 文件，运行时动态生成
Runnable r2 = () -> {
    System.out.println(this.getClass()); // class Main（指向外部类）
};
```

### 1.2 invokedynamic + LambdaMetafactory 机制

Lambda 表达式编译为 `invokedynamic` 指令，运行时通过 `LambdaMetafactory` 生成函数式接口的实现。

```java
// 源代码
list.forEach(item -> System.out.println(item));

// 编译后字节码等价逻辑（伪代码）
invokedynamic #run, Bootstrap(MethodHandles.Lookup, "accept", FunctionType, 
    MethodHandles.lambda$main$0(MethodHandles, MethodType))
// 编译期生成的静态方法
private static void lambda$main$0(String item) {
    System.out.println(item);
}
```

**延迟绑定优势**：Lambda 的调用策略在**运行时**才确定，而非编译时。这意味着 JVM 可以选择不同的生成策略（如缓存复用、内联展开），这是匿名内部类做不到的。

### 1.3 SAM 接口与 @FunctionalInterface

单一抽象方法（Single Abstract Method, SAM）接口是 Lambda 的目标类型。`@FunctionalInterface` 是编译期校验注解，防止接口被误修改。

```java
@FunctionalInterface
public interface Comparator<T> {
    int compare(T o1, T o2);
    // Object 的 public 方法不计数
    boolean equals(Object obj);
    // default 和 static 方法不计数
    default Comparator<T> reversed() { ... }
}
```

> 💡 **可被 Lambda 赋值的接口类型**：接口有且仅有一个**抽象方法**（不计 Object 的 public 方法和 default/static 方法）。

### 1.4 方法引用的本质

方法引用是 Lambda 的"语法糖之上的语法糖"，编译器会将其解糖为等价的 Lambda 表达式。

| 类型 | 语法 | 等价 Lambda | 示例 |
|------|------|-------------|------|
| 静态方法引用 | `Class::staticMethod` | `(args) -> Class.staticMethod(args)` | `String::valueOf` |
| 实例方法引用（特定对象） | `instance::method` | `(args) -> instance.method(args)` | `System.out::println` |
| 实例方法引用（任意对象） | `Class::instanceMethod` | `(obj, args) -> obj.method(args)` | `String::length` |
| 构造器引用 | `Class::new` | `(args) -> new Class(args)` | `ArrayList::new` |

```java
// 四种方法引用实战
List<String> names = Arrays.asList("Alice", "Bob", "Charlie");

// 静态方法引用
names.forEach(System.out::println);

// 实例方法引用（特定对象）
names.sort(String::compareToIgnoreCase); // 等价于 (a, b) -> a.compareToIgnoreCase(b)

// 实例方法引用（任意对象）：对象作为第一个参数
long count = names.stream().filter(String::isEmpty).count(); // String.isEmpty()
// 注意：这里的 String::isEmpty 等价于 (s) -> s.isEmpty()，不是静态调用

// 构造器引用
Stream<String> stream = names.stream();
List<String> list = stream.collect(Collectors.toCollection(ArrayList::new));
```

### 1.5 变量捕获（effectively final）

Lambda 可以访问外部局部变量，但该变量必须是 **effectively final**（初始化后不再赋值）。这不同于匿名内部类要求显式 final（Java 8 之前）。

```java
String prefix = "Hello: "; // effectively final（没被重新赋值）
list.forEach(item -> System.out.println(prefix + item));

// 如果尝试修改 prefix，编译报错
// prefix = "Hi: "; // ❌ Variable used in lambda should be effectively final
```

> 💡 **等效原则**：被 Lambda 捕获的变量会被复制到 Lambda 的实现中（类似匿名内部类的值捕获），因此原始变量的修改无法反映到 Lambda 内——编译器索性禁止修改。

### 🎯 核心要点

- Lambda 编译为 `invokedynamic`，运行时动态生成实现，**不是**匿名内部类的语法糖
- 方法引用是 Lambda 的进一步简化，字节码层面完全等价
- 捕获的局部变量必须 effectively final，但成员变量不受此限制
- `@FunctionalInterface` 是编译期校验注解，没有它也能用 Lambda，但有它更安全

---

## 2. Stream API (Java 8)

### 2.1 惰性求值 vs 及早求值

Stream 最核心的设计思想是**操作流水线化**——中间操作（Intermediate Operations）构建流水线，终端操作（Terminal Operations）触发执行。

```java
list.stream()
    .filter(x -> x > 10)       // 中间操作，惰性求值——什么都不做
    .map(x -> x * 2)           // 中间操作，惰性求值——仍然什么都不做
    .collect(Collectors.toList()); // 终端操作——触发整个流水线执行
```

| 操作类型 | 方法 | 是否惰性 | 作用 |
|----------|------|:--------:|------|
| **中间操作**（无状态） | `filter`, `map`, `flatMap`, `peek` | 是 | 构建操作链，不触发执行 |
| **中间操作**（有状态） | `distinct`, `sorted`, `limit`, `skip` | 是 | 需要维护状态，可能阻塞后续操作 |
| **终端操作**（短路） | `anyMatch`, `findFirst`, `findAny` | 否 | 满足条件即终止，支持短路 |
| **终端操作**（非短路） | `collect`, `forEach`, `reduce`, `count` | 否 | 遍历全部元素 |

### 2.2 并行流的 ForkJoinPool 陷阱

```java
// 所有并行流共享 ForkJoinPool.commonPool()
list.parallelStream()
     .filter(x -> heavyComputation(x))
     .collect(Collectors.toList());

// 阻塞 commonPool 的极端案例
list.parallelStream()
    .forEach(x -> {
        // 如果每个元素都调用 parallelStream() 内部又用了 parallel
        // 会导致 commonPool 线程耗尽，造成死锁
    });
```

> ⚠️ **并行流陷阱**：
> 1. commonPool 默认线程数 = `Runtime.getRuntime().availableProcessors() - 1`
> 2. I/O 密集型任务不适合并行流（会阻塞 commonPool 线程）
> 3. 可使用自定义 ForkJoinPool 隔离并行流：`ForkJoinPool customPool = new ForkJoinPool(10); customPool.submit(() -> stream.parallel().collect(...)).get();`
> 4. 数据量小时，并行流反而更慢（线程创建 + 任务拆分开销）

### 2.3 Stream 性能：什么时候用 Stream？

```text
性能对比（粗略基准）：
┌────────────────────────────────────────────────────────┐
│ 集合大小  │ for循环   │ Stream串行  │ Stream并行       │
│──────────│──────────│────────────│──────────────────│
│ 10       │ ★★★★★    │ ★★★        │ ★ (并行开销太大) │
│ 1,000    │ ★★★★     │ ★★★★       │ ★★★              │
│ 100,000  │ ★★★      │ ★★★★       │ ★★★★★ (数据量大) │
│ CPU密集  │ -         │ -          │ 显著加速         │
└────────────────────────────────────────────────────────┘
```

**最佳实践**：

| 场景 | 推荐 | 原因 |
|------|------|------|
| 小集合（< 1000） | for 循环 | Stream 有方法调用开销和 Lambda 创建开销 |
| 大集合 + CPU 密集计算 | 并行流 | 充分利用多核，数据量大时分摊并行开销 |
| 大集合 + I/O 密集型 | 普通 Stream + 自定义线程池 | 避免阻塞 commonPool |
| 复杂链式操作（filter-map-collect） | Stream | 代码可读性远胜 for 循环 |
| 需要 break/return 提前退出 | for 循环 | Stream 短路操作有限（只有 findFirst/anyMatch 等） |

> 🎯 **核心要点**：Stream 的优势在于**声明式编程 + 可读性**，而非极端性能。在集合操作逻辑复杂时优先用 Stream，在性能敏感的循环热点用 for 循环。

### 😡 面试官追问

**Q: Stream 的 forEach 和 Collection 的 forEach 有什么区别？**

A: `Collection.forEach()` 基于 `Iterable` 的增强 for 循环，在调用者线程中顺序执行；`Stream.forEach()` 在 Stream 的流水线中执行，如果 Stream 是并行的，则元素处理顺序不保证。另外 `Stream.forEach()` 受 Stream 操作约束（不能复用），而 `Collection.forEach()` 只是循环语法糖。

---

## 3. Optional (Java 8)

### 3.1 Optional 不是用来替代 null 检查的

这是最常见的误解。Optional 的设计初衷是**作为方法的返回值类型来表达"可能缺失"的结果**，而不是消除所有 null 检查。

```java
// 错误用法：把 null 检查从对象移到 Optional
public void process(User user) {
    Optional.ofNullable(user).ifPresent(u -> {
        // ...
    });
}
// 这种用法没有任何收益，还不如直接 if (user != null)

// 正确用法：在方法签名中表达"可能没有返回值"
public Optional<User> findById(Long id) {
    User user = userRepository.selectById(id);
    return Optional.ofNullable(user); // 返回可能为空的容器
}

// 调用方清楚知道返回值可能缺失
findById(100L).orElseThrow(() -> new UserNotFoundException(100L));
```

**Optional 的设计位置**：

| 位置 | 推荐使用 | 原因 |
|------|:--------:|------|
| 方法返回值 | ✅ | 明确告知调用方"可能没有结果" |
| 方法参数 | ❌ | 增加调用方负担，违背设计初衷 |
| 类的字段 | ❌ | Optional 未实现 `Serializable`，序列化报错 |
| 集合元素 | ❌ | 集合本身已经可以表达"空"，Optional 嵌套集合毫无必要 |

### 3.2 orElse vs orElseGet 的陷阱

```java
// orElse：无论 Optional 是否为空，都会执行括号内的表达式
String result1 = optional.orElse(expensiveComputation()); // 每次都执行 expensiveComputation()

// orElseGet：只有 Optional 为空时才执行表达式（懒加载）
String result2 = optional.orElseGet(() -> expensiveComputation()); // 非空时跳过

// 验证代码
public static String expensiveComputation() {
    System.out.println("执行了耗时计算...");
    return "default";
}

Optional<String> nonEmpty = Optional.of("hello");
String r1 = nonEmpty.orElse(expensiveComputation());   // 输出：执行了耗时计算...
String r2 = nonEmpty.orElseGet(() -> expensiveComputation()); // 不输出！

// 实际对比：创建默认值有额外开销时，orElseGet 性能远优于 orElse
```

> ⚠️ **记忆口诀**：`orElse` 当场算，`orElseGet` 空的来。

### 3.3 序列化问题

```java
public class User implements Serializable {
    private Long id;
    // ❌ Optional 没有实现 Serializable，这行会触发 NotSerializableException
    private Optional<String> email = Optional.empty();
}

// 正确做法：保持字段为原始类型，Optional 只在返回值使用
public class User implements Serializable {
    private Long id;
    private String email; // 普通字段

    // ✅ 在 getter 中返回 Optional
    public Optional<String> getEmail() {
        return Optional.ofNullable(email);
    }
}
```

### 🎯 核心要点

- Optional **不是**用来消除 null 检查，而是作为返回值表达"可能为空"的语义契约
- `orElse` 每次都会计算参数（容易隐藏性能问题），`orElseGet` 惰性求值
- Optional 不实现 Serializable，不能用作字段类型
- 不要用 Optional 包装集合类型（`Optional<List>` → 直接用 `List` 加 `Collections.emptyList()`）

---

## 4. 局部变量类型推断 var (Java 10)

### 4.1 var 不是 JavaScript 的 var

Java 的 `var` 是**强类型、编译时推断**的关键字，与 JavaScript 的动态类型 `var` 完全不同。

```java
// Java var —— 编译时完全确定类型，运行时就是该类型
var name = "Hello";  // 编译后等价于 String name = "Hello"
name = 42;           // ❌ 编译错误！String 不能赋值为 int

// JavaScript var —— 动态类型，运行时可变
// var name = "Hello";
// name = 42; // ✅ 可以
```

| 特性 | Java var | JavaScript var |
|------|:--------:|:--------------:|
| 类型检查 | 编译时强类型 | 运行时动态类型 |
| 类型可变 | 不可变 | 可变 |
| 运行时开销 | 无（编译期擦除） | 无 |
| 作用域 | 局部变量/增强 for | 全局/函数作用域 |

### 4.2 var 的使用原则

```java
// ✅ 应该用 var 的场景 —— 类型一目了然
var list = new ArrayList<String>();           // 明显是 ArrayList<String>
var map = new HashMap<String, List<User>>();  // 避免写出冗长的泛型类型
var stream = list.stream().filter(x -> x.length() > 5); // Stream 链式操作

// ❌ 不应该用 var 的场景 —— 类型不清晰
var result = someService.complexMethod();     // 看不出来返回什么类型！
var value = getItem();                        // 需要查方法签名才知道类型

// ✅ var + 泛型的优雅写法
// 传统写法（泛型嵌套导致代码臃肿）
Map<String, List<Map<String, Integer>>> data = new HashMap<>();

// var 写法（清晰且简洁）
var data = new HashMap<String, List<Map<String, Integer>>>();
```

**面试口诀**：`var` 在**右侧类型明显**时使用，在**右侧方法不明确**时不要用。好的 `var` 用法让代码更简洁，差的用法让代码更难懂。

### 4.3 var 的限制

```java
// ❌ 不允许
var x;                    // 必须初始化
var y = null;             // 不能推断 null 类型
var f = () -> "hello";    // Lambda 需要显式目标类型
var arr = {1, 2, 3};      // 数组初始化器不能推断

// ✅ 允许
var arr2 = new int[]{1, 2, 3}; // 显式创建数组
var result = (Runnable) () -> {}; // 强制类型转换后可以
```

### 🎯 核心要点

- `var` 是编译期特性，字节码中类型已固定，运行时无性能影响
- 用 `var` 提升可读性的前提是**不牺牲类型透明度**
- `var` 不能用于字段、方法参数、返回类型——仅限局部变量

---

## 5. Switch 表达式 + 模式匹配 (Java 14/17/21)

### 5.1 从 Switch 语句到 Switch 表达式

Java 14 正式引入 Switch 表达式，这是对 Java 6 以来 switch 语法的一次彻底重写。

```java
// 传统 switch 语句——有穿透、有变量作用域问题
String result;
switch (day) {
    case MONDAY:
    case FRIDAY:
        result = "work";
        break;
    case SATURDAY:
    case SUNDAY:
        result = "rest";
        break;
    default:
        result = "unknown";
}

// Switch 表达式——无穿透、直接赋值、完备性检查
String result = switch (day) {
    case MONDAY, FRIDAY -> "work";
    case SATURDAY, SUNDAY -> "rest";
    default -> "unknown";
};
```

### 5.2 yield 关键字 vs return

```java
// 需要复杂逻辑时用 yield 返回值（不是 return！）
String result = switch (day) {
    case MONDAY -> {
        System.out.println("周一来了");
        yield "work"; // yield 是 switch 表达式的"返回值"
    }
    case SATURDAY, SUNDAY -> "rest";
    default -> {
        // 不能在 switch 表达式里用 return——return 是退出方法
        yield "unknown";
    }
};
```

> ⚠️ **注意**：`yield` 是**受限关键字**——仅在 switch 表达式中有特殊含义，可以用作变量名（但不推荐）。而 `return` 永远表示退出当前方法，不能用在 switch 表达式中返回值。

### 5.3 箭头语法的 fall-through 避免

传统 switch 语句的 **fall-through**（无 break 导致穿透执行）是 Java 中 Bug 率最高的语法之一。箭头语法（`->`）天然避免了这一点：

```java
// 传统 switch —— 忘记 break 导致 Bug
switch (x) {
    case 1:
        System.out.println("one");
        // 忘记 break，继续执行 case 2！
    case 2:
        System.out.println("two");
        break;
}

// 箭头语法 —— 没有 fall-through
switch (x) {
    case 1 -> System.out.println("one");
    case 2 -> System.out.println("two");
    // 自动 break，不会穿透到 case 2
}
```

### 5.4 Pattern Matching for switch (Java 17 Preview → 21 正式)

这是 Java 21 最重要的语法升级之一，让 switch 从"值匹配"升级为"类型匹配 + 条件匹配"。

```java
// Java 21 正式版 —— 模式匹配 switch
public String formatValue(Object obj) {
    return switch (obj) {
        case null -> "null";                    // 匹配 null（Java 17+ 特性）
        case Integer i -> "整数: " + i;          // 类型匹配
        case Long l -> "长整数: " + l;
        case Double d -> "浮点数: " + d;
        case String s when s.length() > 10 -> "长字符串: " + s.substring(0, 10) + "...";
        case String s -> "短字符串: " + s;
        case int[] arr -> "数组长度: " + arr.length;
        default -> "未知类型: " + obj.getClass().getName();
    };
}
```

**守卫模式（Guarded Pattern）**：`when` 子句添加额外条件

```java
// without when —— 需要嵌套 if
return switch (obj) {
    case String s -> {
        if (s.length() > 5) yield "长";
        else yield "短";
    }
    default -> "其他";
};

// with when —— 条件直接写在 case 上
return switch (obj) {
    case String s when s.length() > 5 -> "长";
    case String s -> "短";
    default -> "其他";
};
```

**Record 解构（Record Pattern）**：Java 21 正式引入

```java
// 定义 Record
record Point(int x, int y) {}
record Line(Point start, Point end) {}

// Record 模式解构
public static String describe(Object obj) {
    return switch (obj) {
        case Point(int x, int y) -> "点(" + x + ", " + y + ")";
        case Line(Point(int x1, int y1), Point(int x2, int y2)) -> 
            "线段从(" + x1 + "," + y1 + ")到(" + x2 + "," + y2 + ")";
        case null -> "null";
        default -> "未知";
    };
}
```

### 5.5 密封类（Sealed Class）与模式匹配的配合

模式匹配 switch 要求**穷尽所有可能**。有了密封类，编译器可以检查 switch 是否覆盖了所有子类，无需 default 分支。

```java
// 密封类
public sealed interface Shape permits Circle, Rectangle, Triangle {}

record Circle(double radius) implements Shape {}
record Rectangle(double width, double height) implements Shape {}
record Triangle(double base, double height) implements Shape {}

// 编译器知道 Shape 只有三个子类，可以省略 default
public double area(Shape shape) {
    return switch (shape) {
        case Circle c -> Math.PI * c.radius() * c.radius();
        case Rectangle r -> r.width() * r.height();
        case Triangle t -> t.base() * t.height() / 2;
        // 不需要 default——所有 Shape 子类都已覆盖
    };
}
```

> 💡 **密封类 + 模式匹配** = 代数数据类型（ADT）风格，这是 Java 向函数式语言学习的重要成果。

### 🎯 核心要点

- Switch 表达式消除了 fall-through、支持直接赋值、具有完备性检查
- `yield` 在 switch 表达式的块中返回值，`return` 退出方法，二者不能混用
- 模式匹配使 switch 支持类型匹配、守卫条件、Record 解构
- 密封类配合模式匹配可实现穷尽性检查，无需 default 分支

---

## 6. Record 类 (Java 16)

### 6.1 Record 编译后是什么？

Record 是数据载体的终极语法糖。以下面代码为例，看编译器做了什么：

```java
// 源代码：一行定义数据载体
public record Point(int x, int y) {}
```

```text
// javap -p Point 反编译结果：
public class Point extends java.lang.Record {
    private final int x;         // 私有 final 字段
    private final int y;
    
    // 全参数构造器
    public Point(int x, int y);
    
    // 自动生成的 accessor（注意！不是 getX()/getY()，而是 x()/y()）
    public final int x();
    public final int y();
    
    // 自动实现 equals —— 逐字段比较
    public final boolean equals(Object o);
    
    // 自动实现 hashCode —— 所有字段参与
    public final int hashCode();
    
    // 自动实现 toString
    public final String toString();
}
```

**编译器自动生成的内容**：

| 生成项 | 说明 |
|--------|------|
| 私有 final 字段 | 每个组件对应一个 `private final` 字段 |
| 规范构造器 | 接收所有组件的构造器 |
| Accessor 方法 | 方法名与组件名一致（`x()` 而非 `getX()`） |
| `equals()` | 基于所有组件字段比较 |
| `hashCode()` | 基于所有组件字段生成 |
| `toString()` | 格式为 `Point[x=1, y=2]` |

### 6.2 Record vs Lombok @Data

| 对比项 | Record | Lombok @Data |
|--------|:------:|:------------:|
| 继承能力 | 不能继承，但可实现接口 | 可以继承其他类 |
| 字段可变性 | `private final`，不可变 | 默认可变（可添加 `@Setter`） |
| 第三方依赖 | 不需要（JDK 内置） | 需要 Lombok 插件 + 注解处理器 |
| IDE 兼容性 | 原生支持 | 需要插件 |
| 构造器定制 | 紧凑构造器 | `@AllArgsConstructor` |
| equals/hashCode | 自动基于组件 | `@EqualsAndHashCode` |
| Accessor 命名 | `field()` | `getField()`（JavaBean 风格） |
| 反序列化 | 需要特殊处理 | 需要额外配置 |

```java
// Record + 紧凑构造器
public record Point(int x, int y) {
    // 紧凑构造器：对参数进行校验和归一化
    public Point {
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("坐标不能为负数: " + x + ", " + y);
        }
        // 不需要 this.x = x; —— 编译器自动生成
    }
}

// 相当于自动展开为：
// public Point(int x, int y) {
//     if (x < 0 || y < 0) throw ...;
//     this.x = x;
//     this.y = y;
// }
```

### 6.3 Record vs Kotlin data class

```kotlin
// Kotlin data class
data class User(val name: String, val age: Int)
```

```java
// Java Record
public record User(String name, int age) {}
```

| 对比项 | Java Record | Kotlin data class |
|--------|:-----------:|:-----------------:|
| 声明行数 | 1 | 1 |
| 解构声明 | 不支持（需模式匹配） | 支持 `val (name, age) = user` |
| copy() | 不支持 | 支持 `user.copy(name = "new")` |
| 默认值 | 不支持 | 支持 `val name: String = "unknown"` |
| 继承 | 不能继承 | 不能继承 |
| 数据类理念 | 值语义（面向值） | 数据容器 |

### 6.4 Record 的典型应用场景

```java
// 1. 值对象/传输对象（DTO）
public record UserDTO(Long id, String name, String email) {}

// 2. 复合 Key（Map 的 Key 需要不可变 + equals/hashCode）
public record CacheKey(String prefix, Long id) {}

// 3. API 响应
public record ApiResponse<T>(int code, String message, T data) {}

// 4. 配置对象
public record DataSourceConfig(String url, String username, int poolSize) {}

// 5. 局部数据聚合（方法内临时组合数据）
public record NameCount(String name, long count) {}
```

### 6.5 Record 的局限性

```java
// ❌ 不能继承其他类
public record Point(int x, int y) extends Entity { } // 编译错误

// ❌ 不能修改字段（不可变）
Point p = new Point(1, 2);
// p.x = 3; // 编译错误，final 字段

// ❌ 不能声明实例字段（只能在参数列表中声明）
public record Point(int x, int y) {
    private int z; // 编译错误！Record 不允许额外实例字段
}

// ✅ 可以声明静态字段和静态方法
public record Point(int x, int y) {
    public static Point ORIGIN = new Point(0, 0);
    
    public static Point of(int x, int y) {
        return new Point(x, y);
    }
    
    // ✅ 可以添加实例方法
    public double distance() {
        return Math.sqrt(x * x + y * y);
    }
}
```

### 🎯 核心要点

- Record 编译为 `final class extends java.lang.Record`，自动生成构造器、accessor、equals/hashCode/toString
- 紧凑构造器用于参数校验和归一化，语法简洁
- 适合值对象/DTO/复合 Key，但不适合需要继承或可变状态的场景
- Record 的 accessor 命名是 `field()` 而非 `getField()`，不符合 JavaBean 规范，在有些框架中需特殊适配

---

## 7. 文本块 Text Blocks (Java 15)

### 7.1 语法与本质

文本块用三重双引号 `"""` 定义，编译后就是普通 `String`，不产生新的字符串类型。

```java
// 传统写法（大量转义和换行符）
String json = "{\n" +
              "  \"name\": \"Alice\",\n" +
              "  \"age\": 30\n" +
              "}";

// 文本块（干净、直观）
String json = """
    {
      "name": "Alice",
      "age": 30
    }
    """;

// 编译后两个字符串 equals 为 true
System.out.println(json.equals(jsonTraditional)); // true
```

### 7.2 缩进处理规则

文本块的缩进从**闭合 `"""` 的位置**算起：

```java
// 闭合 """ 的位置决定了每行的最小缩进
String html = """
        <html>
            <body>
                <p>Hello</p>
            </body>
        </html>
        """;
// 闭合 """ 前面有 8 个空格，编译器会去掉每行公共的 8 个空格
// 结果：
// <html>
//     <body>
//         <p>Hello</p>
//     </body>
// </html>

// 使用 stripIndent() 或调整闭合三引号位置可以控制缩进
```

**缩进算法**（由 `String::stripIndent` 实现）：

1. 移除开头和结尾的空行
2. 计算所有行**非空行**的公共前导空白长度
3. 从每行中移除该公共空白

### 7.3 转义序列

Java 15 为文本块新增了两个转义序列：

```java
// 1. \行尾续行 —— 取消换行符
String sql = """
    SELECT id, name, email \
    FROM users \
    WHERE status = 'active' \
    ORDER BY name
    """;
// 实际内容（无换行）：
// SELECT id, name, email FROM users WHERE status = 'active' ORDER BY name

// 2. \s 强制空格
String formatted = """
    left\sright
    """;
// 实际内容（\s 保留这个空格，即使行尾）：
// "left right\n"
```

> 💡 `\` 行尾续行在大 SQL、长 URL 和拼接日志模板时非常有用。`\s` 让 IDE 的"尾部空格自动清理"不会破坏格式化。

### 🎯 核心要点

- 文本块编译后就是普通 `String`，运行时零开销
- 缩进由闭合 `"""` 的位置决定，代码格式不影响字符串实际内容
- 新增 `\` 续行和 `\s` 空格转义，精细控制格式

---

## 8. 字符串模板 String Templates (Java 21 Preview)

### 8.1 STR 模板处理器

字符串模板是 Java 21 的预览特性，支持在字符串中嵌入表达式。

```java
// 传统字符串拼接
String msg = "Hello, " + name + "! You are " + age + " years old.";

// String.format
String msg = String.format("Hello, %s! You are %d years old.", name, age);

// MessageFormat
String msg = MessageFormat.format("Hello, {0}! You are {1} years old.", name, age);

// 字符串模板（Java 21 Preview）
String msg = STR."Hello, \{name}! You are \{age} years old.";
```

**STR 是内置模板处理器**，表达式可以是任意 Java 表达式：

```java
// 嵌入方法调用
String msg = STR."Today is \{LocalDate.now()}";

// 嵌入三元表达式
String status = STR."User is \{isActive ? "active" : "inactive"}";

// 嵌入复杂的表达式
String result = STR."总和 = \{items.stream().mapToInt(Integer::intValue).sum()}";

// 多行模板（结合文本块）
String html = STR."""
    <html>
        <body>
            <h1>\{title}</h1>
            <p>\{content}</p>
        </body>
    </html>
    """;
```

### 8.2 安全优势

```java
// 传统 SQL 拼接 —— SQL 注入风险
String query = "SELECT * FROM users WHERE name = '" + userInput + "'";
// 如果 userInput = "'; DROP TABLE users; --" → 灾难！

// 使用模板处理器的安全性（自定义）
String query = SQL."""
    SELECT * FROM users WHERE name = \{userInput}
    """;
// 如果模板处理器实现了参数化查询，则自动转义

// 防 XSS 示例
var safe = HTML."""
    <div>\{userContent}</div>
    """; // HTML 模板处理器自动转义 < > & 等字符
```

### 8.3 自定义模板处理器

```java
// 自定义 JSON 模板处理器（将特殊字符转义）
StringProcessor JSON = StringTemplate.Processor.of(
    st -> {
        StringBuilder sb = new StringBuilder();
        for (String fragment : st.fragments()) {
            sb.append(fragment);
            Object value = st.values().iterator().next();
            // 对 value 进行 JSON 转义
            sb.append(escapeJson(value != null ? value.toString() : "null"));
        }
        return sb.toString();
    }
);

String name = "Alice \"Smith\"";
String json = JSON."""
    {"name": \{name}, "age": 30}
    """;
```

### 🎯 核心要点

- 字符串模板是 Java 21 预览特性，将来可能调整
- `STR` 是内置处理器，支持任意表达式和文本块结合
- 模板处理器的真正价值在于**安全性**——可以防止 SQL 注入和 XSS
- 自定义处理器可实现 DSL 风格的格式化逻辑

---

## 9. 虚拟线程 Virtual Threads (Java 21)

### 9.1 虚拟线程不是语法糖

虚拟线程（Virtual Threads / Project Loom）是 Java 21 正式特性，它是**平台级并发模型的重构**，不是语法糖。但它常作为"Java 新特性"与语法糖一起被面试官考察。

| 对比 | 平台线程（Platform Thread） | 虚拟线程（Virtual Thread） |
|------|:---------------------------:|:--------------------------:|
| 底层对应 | OS 线程 | JVM 管理的纤程（Fiber） |
| 创建成本 | 高（MB 级栈空间） | 极低（KB 级栈空间） |
| 最大数量 | 几千（受限 OS） | 数百万（受限内存） |
| 阻塞行为 | 阻塞 OS 线程 | 挂起到 JVM 堆 |
| API 兼容性 | 完全兼容 | 完全兼容（无需改写） |

### 9.2 API 对比

```java
// 传统线程池方式
ExecutorService executor = Executors.newFixedThreadPool(10);
executor.submit(() -> {
    // 处理任务，最多 10 个并发
});

// 虚拟线程方式（Java 21）—— 每个请求开一个虚拟线程
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
executor.submit(() -> {
    // 处理任务，可创建数百万虚拟线程
});

// 或者直接创建
Thread vThread = Thread.startVirtualThread(() -> {
    System.out.println("在虚拟线程中运行");
});
```

### 9.3 虚拟线程的阻塞优化

```java
// 传统模型中，每个 HTTP 请求对应一个 OS 线程
// 当请求发生 I/O 阻塞（数据库查询、RPC 调用），OS 线程被挂起
// 1000 并发 = 1000 OS 线程（明显开销）

// 虚拟线程模型中，阻塞时虚拟线程自动挂起，底层 OS 线程去执行其他虚拟线程
// 1000 并发 ≈ 少量 OS 线程 + 1000 个廉价虚拟线程

public CompletableFuture<User> handleRequest(Request req) {
    // 每个请求创建一个虚拟线程
    return CompletableFuture.supplyAsync(() -> {
        // 这里调用 blocking I/O —— 虚拟线程自动 yield
        var user = userService.findById(req.userId());  // 虚拟线程挂起，不占 OS 线程
        var order = orderService.findByUserId(user.id());// 虚拟线程恢复，再挂起
        return new User(user, order);
    }, Executors.newVirtualThreadPerTaskExecutor());
}
```

### 🎯 核心要点

- 虚拟线程是 JVM 管理的轻量级线程，创建成本极低，适合 I/O 密集型任务
- API 与现有代码完全兼容，只需将 `new Thread()` 替换为 `Thread.startVirtualThread()`
- **核心优势**：阻塞操作不再阻塞 OS 线程，极大提升并发吞吐量
- **注意事项**：虚拟线程不适合 CPU 密集型计算（没有并行加速）；避免 synchronized 块（会钉住平台线程）；慎用线程池（虚拟线程应每任务创建，不需要池化）

---

## 10. 模式匹配 for instanceof (Java 16)

### 10.1 一步完成判断 + 转型

传统 instanceof 需要两步：先判断类型，再强制转换。模式匹配将其合并为一步。

```java
// 传统写法 —— 冗余且容易出错
if (obj instanceof String) {
    String s = (String) obj; // 手动强制转换
    System.out.println(s.length());
}

// 模式匹配 instanceof（Java 16 正式版）
if (obj instanceof String s) {  // 判断 + 变量声明一步完成
    System.out.println(s.length()); // 直接使用 s
}
```

### 10.2 作用域规则

```java
// 模式变量 s 在条件为 true 后才能使用
if (obj instanceof String s && s.length() > 5) {
    // ✅ 短路与 (&&) 保证 s 非空后访问 s.length()
    System.out.println(s);
}

// ❌ 编译错误
// if (obj instanceof String s || s.length() > 5) { } 
// 短路或 (||) 下 s 可能未定义

// 传统写法 vs 模式匹配
// 传统：需要在方法开始时声明所有可能的类型变量
public void process(Object obj) {
    if (obj instanceof String) {
        String s = (String) obj;
        // ...
    }
}

// 模式匹配：变量只在匹配的作用域内可见
public void process(Object obj) {
    if (obj instanceof String s) {
        // s 在作用域内
    }
    // s 不在作用域内
}
```

### 10.3 嵌套模式匹配

```java
// 模式匹配 instanceof 与 Record 配合
record Address(String city, String street) {}

if (obj instanceof Address(var city, var street)) {
    // 直接解构 Record
    System.out.println(city + ": " + street);
}

// 等价于传统写法
if (obj instanceof Address) {
    Address addr = (Address) obj;
    System.out.println(addr.city() + ": " + addr.street());
}
```

### 🎯 核心要点

- `if (obj instanceof Type v)` 合并类型判断 + 变量声明 + 强制转换
- 模式变量作用域受短路运算符限制（仅在 `&&` 而非 `||` 中可用）
- 与 Record 配合可实现嵌套解构，代码更简洁
- 这是模式匹配在 Java 中最简单的应用，为 switch 模式匹配打下基础

---

**下一模块**：[03 语法糖实现原理与字节码揭秘](./03-语法糖实现原理与字节码揭秘.md) | **返回总览**：[总览](./00-Java语法糖知识体系总览.md)
