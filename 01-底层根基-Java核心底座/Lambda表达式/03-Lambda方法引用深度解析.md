# 03 方法引用深度解析

> 方法引用不是"更短的 Lambda"，而是把一个**已存在的方法**直接适配成函数式接口实例——理解它的关键是"签名适配"与"隐式首参数"

---

## 📚 目录

1. [从 Lambda 到方法引用](#1-从-lambda-到方法引用)
2. [四种方法引用全景](#2-四种方法引用全景)
3. [目标类型与签名适配机制](#3-目标类型与签名适配机制)
4. [Class::instanceMethod 的隐式首参数](#4-classinstancemethod-的隐式首参数)
5. [构造器引用与数组构造器引用](#5-构造器引用与数组构造器引用)
6. [歧义与编译错误实录](#6-歧义与编译错误实录)
7. [求值时机陷阱](#7-求值时机陷阱)
8. [方法引用与 Lambda 的选择准则](#8-方法引用与-lambda-的选择准则)
9. [动手实验](#9-动手实验)
10. [速查与自测](#10-速查与自测)

---

## 1. 从 Lambda 到方法引用

### 1.1 一句话本质

当一个 Lambda 的**唯一动作就是调用某个已有方法，且参数原样透传**时，它可以被改写成方法引用。

```java
// 这三行完全等价
Function<String, Integer> f1 = s -> Integer.parseInt(s);   // Lambda
Function<String, Integer> f2 = Integer::parseInt;          // 方法引用
Function<String, Integer> f3 = new Function<>() {          // 匿名内部类
    @Override public Integer apply(String s) { return Integer.parseInt(s); }
};
```

### 1.2 什么情况**不能**改写

| 场景 | 示例 | 原因 |
|------|------|------|
| 参数需要加工 | `s -> Integer.parseInt(s.trim())` | 不是原样透传 |
| 参数顺序调换 | `(a, b) -> compare(b, a)` | 顺序不一致 |
| 需要额外常量参数 | `s -> s.split(",")` | `","` 不来自形参 |
| 需要多条语句 | `s -> { log(s); return f(s); }` | 不是单一方法调用 |
| 返回值需再包装 | `s -> Optional.of(parse(s))` | 存在两层调用 |

> 🎯 **核心判据**：把 Lambda 体写成 `方法名(形参1, 形参2, ...)` 且形参**按原顺序、原样、全部**传入，才能用 `::`。

### 1.3 `::` 不是运算符

`::` 是**方法引用分隔符**，不参与表达式运算。`A::b` 本身没有独立类型，它是一个 **poly expression（多态表达式）**——必须有目标类型才能确定含义。

```java
var x = Integer::parseInt;   // ❌ 编译错误：无法推断类型
// 错误：无法推断 x 的类型（方法引用需要显式目标类型）

Function<String, Integer> ok = Integer::parseInt;   // ✅ 有目标类型
```

---

## 2. 四种方法引用全景

### 2.1 总览表

| # | 种类 | 语法 | 等价 Lambda | 典型示例 |
|:-:|------|------|------------|---------|
| 1 | **静态方法引用** | `类名::静态方法` | `(a,b) -> 类名.方法(a,b)` | `Integer::parseInt` |
| 2 | **绑定实例方法引用** | `对象::实例方法` | `(a,b) -> 对象.方法(a,b)` | `System.out::println` |
| 3 | **未绑定实例方法引用** | `类名::实例方法` | `(x,a) -> x.方法(a)` | `String::trim` |
| 4 | **构造器引用** | `类名::new` | `(a,b) -> new 类名(a,b)` | `ArrayList::new` |

> 额外形态：`数组类型[]::new`（数组构造器）、`super::方法`、`this::方法`。

### 2.2 类别 1：静态方法引用

```java
Function<String, Integer>  parse  = Integer::parseInt;      // (String)->Integer
BinaryOperator<Integer>    max    = Integer::max;           // (Integer,Integer)->Integer
Supplier<Long>             now    = System::currentTimeMillis;
Predicate<String>          blank  = StringUtils::isBlank;   // 自定义工具类
```

参数映射最直白：**函数式接口的每个参数，按顺序对应静态方法的每个参数**。

```text
Function<String, Integer>.apply(String s)  →  Integer.parseInt(String s)
                          └───┬──┘                          └───┬──┘
                              └──────────── 一一对应 ─────────────┘
```

### 2.3 类别 2：绑定实例方法引用（接收者已确定）

接收者（receiver）在**书写方法引用时就已确定**，是一个具体对象。

```java
PrintStream out = System.out;
Consumer<String> printer = out::println;      // 接收者 = out 这个对象

String prefix = "LOG: ";
Predicate<String> startsWith = prefix::startsWith;  // 接收者 = prefix
// 等价于 s -> prefix.startsWith(s)   注意：不是 s.startsWith(prefix)！

List<String> list = new ArrayList<>();
Consumer<String> adder = list::add;           // 接收者 = list
```

> ⚠️ **最容易搞反的点**：`prefix::startsWith` 中 `prefix` 是**调用者**不是**参数**。
> `prefix::startsWith` ≡ `s -> prefix.startsWith(s)`，而不是 `s -> s.startsWith(prefix)`。

绑定形式还包括 `this::` 和 `super::`：

```java
class Child extends Base {
    @Override String name() { return "Child"; }

    Supplier<String> viaThis()  { return this::name; }    // 虚分派 → "Child"
    Supplier<String> viaSuper() { return super::name; }   // 非虚分派 → "Base"
}
```

实测输出（JDK 25）：

```text
super::name -> Base
this::name  -> Child
```

`super::name` 在字节码中通过 `invokespecial` 调用父类实现，**绕过虚分派**：

```text
private java.lang.String lambda$superRef$0();
  Code:
     0: aload_0
     1: invokespecial #14    // Method SuperRef$Base.name:()Ljava/lang/String;
     4: areturn
```

### 2.4 类别 3：未绑定实例方法引用（接收者由参数提供）

```java
Function<String, String>    trim  = String::trim;        // x -> x.trim()
Function<String, Integer>   len   = String::length;      // x -> x.length()
BiPredicate<String, String> sw    = String::startsWith;  // (x,y) -> x.startsWith(y)
Comparator<String>          cmp   = String::compareTo;   // (x,y) -> x.compareTo(y)
```

这是四类中**唯一会"多出一个参数"**的形态，详见第 4 节。

### 2.5 类别 4：构造器引用

```java
Supplier<List<String>>          newList = ArrayList::new;       // () -> new ArrayList<>()
Function<Integer, List<String>> sized   = ArrayList::new;       // n -> new ArrayList<>(n)
Function<String, Integer>       boxed   = Integer::new;         // s -> new Integer(s)（已废弃）
BiFunction<String, Integer, User> user  = User::new;            // (n,a) -> new User(n,a)
```

> 💡 同一个 `ArrayList::new` 在不同目标类型下会绑定到**不同的构造器重载**——这正是"目标类型驱动"的直接体现。

### 2.6 四类的字节码指纹

用 `javap -v` 查看 `BootstrapMethods` 的 `MethodHandle` 种类，可以直接区分四类引用（JDK 25 实测）：

| 方法引用写法 | MethodHandle Kind | 常量池标记 |
|-------------|:----------------:|-----------|
| `Integer::parseInt`（静态） | 6 | `REF_invokeStatic` |
| `System.out::println`（绑定） | 5 | `REF_invokeVirtual` |
| `String::trim`（未绑定） | 5 | `REF_invokeVirtual` |
| `StringBuilder::new`（构造器） | 8 | `REF_newInvokeSpecial` |
| `super::name` | 7 | `REF_invokeSpecial` |

```text
// javap -v 实测片段
#106 = MethodHandle  6:#107  // REF_invokeStatic java/lang/Integer.parseInt:(Ljava/lang/String;)I
#115 = MethodHandle  5:#116  // REF_invokeVirtual java/io/PrintStream.println:(Ljava/lang/String;)V
#123 = MethodHandle  5:#124  // REF_invokeVirtual java/lang/String.trim:()Ljava/lang/String;
#128 = MethodHandle  8:#129  // REF_newInvokeSpecial java/lang/StringBuilder."<init>":()V
```

> ⚠️ **绑定与未绑定在 MethodHandle Kind 上都是 5**，区分它们看的是 **invokedynamic 的调用点签名**：绑定形式会把接收者作为捕获参数传入（`(Ljava/io/PrintStream;)Ljava/util/function/Consumer;`），未绑定形式的调用点签名为空（`()Ljava/util/function/Function;`）。

---

## 3. 目标类型与签名适配机制

### 3.1 编译器做的三件事

编译器面对 `目标类型 f = A::b;` 时依次执行：

```text
① 确定目标类型的函数描述符（唯一抽象方法的签名）
        Function<String,Integer>  →  descriptor: (String) -> Integer

② 在 A 中搜索名为 b 的、能被"适配"到该描述符的方法
        搜索两条路径：
          路径一：把 A::b 当静态/绑定引用   → 参数 n 个全给方法
          路径二：把 A::b 当未绑定实例引用  → 第 1 个参数当接收者，剩 n-1 个给方法

③ 校验返回值、受检异常、可见性、泛型是否兼容
```

### 3.2 签名适配的四条兼容规则

| 规则 | 说明 | 示例 |
|------|------|------|
| **参数个数** | 必须精确匹配（考虑隐式首参数后） | `String::length` 可适配 `(String)->Integer` |
| **参数类型** | 接口参数需能**赋值给**方法参数（协变入参放宽） | `(Object)->void` 不能适配 `println(String)` |
| **返回值** | 方法返回值需能赋值给接口返回值；**方法有返回值而接口返回 void 是允许的**（丢弃） | `list::add` 可适配 `Consumer` |
| **受检异常** | 方法抛出的受检异常必须在接口方法的 `throws` 列表内 | 见 6.4 |

**返回值可丢弃**是一条常被忽略的规则：

```java
List<String> list = new ArrayList<>();
Consumer<String> c = list::add;   // ✅ add 返回 boolean，Consumer 返回 void，允许丢弃
Function<String, Boolean> f = list::add;  // ✅ 也可以保留返回值
```

### 3.3 自动装箱/拆箱参与适配

```java
Function<String, Integer> f = Integer::parseInt;
```

`parseInt` 返回 **`int`**，而 `Function` 的返回类型是 **`Integer`**。适配层自动插入装箱。字节码中 BSM 参数清晰记录了这一"擦除签名 ↔ 实际签名"的转换：

```text
3: REF_invokeStatic LambdaMetafactory.metafactory:(...)
  Method arguments:
    #105 (Ljava/lang/Object;)Ljava/lang/Object;      ← samMethodType：擦除后的接口签名
    #106 REF_invokeStatic Integer.parseInt:(Ljava/lang/String;)I   ← implMethod：真实方法（返回 int）
    #111 (Ljava/lang/String;)Ljava/lang/Integer;     ← instantiatedMethodType：实例化后签名（返回 Integer）
```

> 🎯 三个 `MethodType` 参数的分工：`samMethodType` 是**泛型擦除后**的接口签名，`instantiatedMethodType` 是**泛型实例化后**的签名，二者的差值就是运行时需要插入的 **checkcast + 装箱/拆箱**适配代码。

### 3.4 目标类型不同 → 同一写法含义不同

```java
// 同样是 Amb::make，目标类型决定选中哪个方法
static class Amb {
    static String make(String s) { return "STATIC:" + s; }
    String make()                { return "INSTANCE"; }
}

Function<String, String> f1 = Amb::make;   // → 静态方法
Function<Amb,    String> f2 = Amb::make;   // → 未绑定实例方法
```

实测输出：

```text
Function<String,String> Amb::make -> STATIC:in
Function<Amb,String>    Amb::make -> INSTANCE
```

> 💡 这**不是**歧义错误——两个目标类型分别只有一条路径可走，编译器能唯一确定。真正的歧义见 6.2。

---

## 4. Class::instanceMethod 的隐式首参数

### 4.1 核心心智模型

`Class::instanceMethod` 会把函数式接口的**第一个参数当作方法的接收者（this）**，其余参数依次传给方法。

```text
BiPredicate<String, String> p = String::startsWith;

接口签名:  test( String a , String b )
                    │          │
                    │          └──────────────┐
                    └── 变成接收者              │ 变成实参
                            ▼                  ▼
实际调用:              a . startsWith        ( b )
```

### 4.2 参数个数换算表

| 目标接口 | 接口参数数 n | 方法实际参数数 | 说明 |
|---------|:---:|:---:|------|
| `Function<String,Integer>` = `String::length` | 1 | 0 | 1 个参数全部用作接收者 |
| `BiFunction<String,String,Boolean>` = `String::startsWith` | 2 | 1 | 首参当接收者，余 1 个实参 |
| `Comparator<String>` = `String::compareTo` | 2 | 1 | 同上 |

> 🎯 **换算公式**：未绑定实例方法引用中，`接口参数数 = 方法参数数 + 1`。

### 4.3 绑定 vs 未绑定的经典对照

这是面试与实战中最高频的混淆点：

```java
String prefix = "ERROR";

// 绑定：prefix 是接收者
Predicate<String> p1 = prefix::startsWith;
// ≡ s -> prefix.startsWith(s)
System.out.println(p1.test("ERR"));      // "ERROR".startsWith("ERR") → true

// 未绑定：参数是接收者
BiPredicate<String, String> p2 = String::startsWith;
// ≡ (a, b) -> a.startsWith(b)
System.out.println(p2.test("ERROR", "ERR"));   // true
```

| 维度 | `prefix::startsWith` | `String::startsWith` |
|------|---------------------|---------------------|
| `::` 左侧 | **对象**表达式 | **类型**名 |
| 接收者来源 | 书写时固定 | 运行时由首参提供 |
| 接口参数数 | = 方法参数数 | = 方法参数数 + 1 |
| 是否捕获 | **是**（捕获接收者） | 否 |

### 4.4 静态与实例引用语法不可混用

```java
static class A {
    void inst() {}
    static void stat() {}
}
A a = new A();
Runnable r = a::stat;   // ❌
```

实测编译错误：

```text
错误: 不兼容的类型: 方法引用无效
        Runnable r = a::stat;
                     ^
    在绑定查找中找到意外的静态 方法 stat()
```

> ⚠️ **规则**：静态方法只能用 `类名::`，实例方法在 `类名::` 形式下必须走"隐式首参数"路径。不能用对象引用静态方法。

---

## 5. 构造器引用与数组构造器引用

### 5.1 普通构造器引用

```java
record User(String name, int age) {}

Supplier<StringBuilder>            sb   = StringBuilder::new;
Function<Integer, StringBuilder>   sbN  = StringBuilder::new;    // 选中 (int) 构造器
BiFunction<String, Integer, User>  u    = User::new;
```

构造器引用的字节码使用 `REF_newInvokeSpecial`（Kind = 8），且**不生成 `lambda$` 合成方法**——直接指向 `<init>`：

```text
6: REF_invokeStatic LambdaMetafactory.metafactory:(...)
  Method arguments:
    #92  ()Ljava/lang/Object;
    #128 REF_newInvokeSpecial java/lang/StringBuilder."<init>":()V   ← 直接指向构造器
    #132 ()Ljava/lang/StringBuilder;
```

### 5.2 泛型类的构造器引用

```java
Supplier<List<String>>  s1 = ArrayList::new;          // 由目标类型推断 <String>
Supplier<List<String>>  s2 = ArrayList<String>::new;  // 显式指定，等价
// Supplier<List<String>> s3 = ArrayList<>::new;      // ❌ 不允许菱形语法
```

> ⚠️ 构造器引用**不支持菱形 `<>`**，要么完全省略类型参数（靠推断），要么写全。

### 5.3 数组构造器引用

```java
IntFunction<String[]>  arr  = String[]::new;    // n -> new String[n]
IntFunction<int[][]>   arr2 = int[][]::new;     // n -> new int[n][]
```

最经典的用途是 `Stream.toArray`：

```java
String[] result = list.stream()
                      .filter(s -> !s.isBlank())
                      .toArray(String[]::new);   // 而不是 toArray() 返回 Object[]
```

### 5.4 一个反直觉的编译产物差异

数组构造器引用与普通构造器引用的编译产物**不同**——数组形式**会生成合成方法**（JDK 25 实测）：

```text
private static java.lang.String[] lambda$arrayRef$0(int);
  descriptor: (I)[Ljava/lang/String;
  flags: (0x100a) ACC_PRIVATE, ACC_STATIC, ACC_SYNTHETIC
  Code:
     0: iload_0
     1: anewarray     #48    // class java/lang/String
     4: areturn
```

对应的 BSM 指向的是这个合成方法，而非某个"数组构造器"：

```text
7: REF_invokeStatic LambdaMetafactory.metafactory:(...)
  Method arguments:
    #134 (I)Ljava/lang/Object;
    #136 REF_invokeStatic LambdaDemo.lambda$arrayRef$0:(I)[Ljava/lang/String;   ← 合成方法
    #139 (I)[Ljava/lang/String;
```

> 💡 **原因**：JVM 中"创建数组"是 `anewarray` **指令**，不是可被 MethodHandle 直接引用的方法。编译器必须包一层合成方法承载这条指令。而 `StringBuilder::new` 对应真实存在的 `<init>` 方法，可直接引用。

### 5.5 数组引用不支持的形态

| 写法 | 是否合法 | 说明 |
|------|:---:|------|
| `String[]::new` | ✅ | 一维数组构造 |
| `int[][]::new` | ✅ | 创建首维，等价 `new int[n][]` |
| `String[]::clone` | ✅ | 数组的 `clone()` 可引用 |
| `String[]::length` | ❌ | `length` 是字段不是方法 |

---

## 6. 歧义与编译错误实录

> 本节全部错误信息为 **JDK 25 `javac` 实测输出**（中文语言环境）。

### 6.1 错误一：参数个数不匹配

```java
BiFunction<String, String, Integer> bf = String::length;
```

```text
错误: 不兼容的类型: 方法引用无效
        BiFunction<String, String, Integer> bf = String::length;
                                                 ^
    无法将 类 String中的 方法 length应用到给定类型
      需要: 没有参数
      找到:    String,String
      原因: 实际参数列表和形式参数列表长度不同
```

**诊断**：`BiFunction` 有 2 个参数，走未绑定路径时首参当接收者，还剩 1 个实参，但 `length()` 需要 0 个。

### 6.2 错误二：静态与实例方法真歧义

前面 3.4 说过目标类型通常能消歧，但当**两条路径的签名完全相同**时就真的歧义了：

```java
static class Amb {
    static String make(Amb a) { return "STATIC"; }   // 静态：(Amb)->String
    String make()             { return "INSTANCE"; } // 未绑定：(Amb)->String
}
Function<Amb, String> fn = Amb::make;
```

```text
错误: 不兼容的类型: 方法引用无效
        Function<Amb,String> fn = Amb::make;
                                  ^
    对make的引用不明确
      Amb 中的方法 make(Amb) 和 Amb 中的方法 make() 都匹配
```

> ⚠️ **规避方式**：改写成 Lambda 显式表达意图——`a -> Amb.make(a)` 或 `a -> a.make()`。

### 6.3 错误三：重载方法 + 重载目标类型的双向歧义

```java
interface F1 { String apply(String s); }
interface F2 { String apply(Integer i); }
static void take(F1 f) {}
static void take(F2 f) {}
static String id(String s)  { return s; }
static String id(Integer i) { return i.toString(); }

take(Amb4::id);   // 两侧都重载
```

```text
错误: 对take的引用不明确
        take(Amb4::id);
        ^
  Amb4 中的方法 take(F1) 和 Amb4 中的方法 take(F2) 都匹配
```

> 💡 **注意错误点在 `take` 上而不是 `id` 上**——编译器连"该选哪个重载的 `take`"都无法确定，因为 `Amb4::id` 是**不精确（inexact）方法引用**，在重载决议阶段不提供有效的类型信息。
>
> **修复**：显式转型 `take((F1) Amb4::id)`，或改用带类型的 Lambda `take((String s) -> Amb4.id(s))`。

### 6.4 错误四：受检异常不兼容

```java
static String risky(String s) throws IOException { return s; }
Function<String, String> fn = Err4::risky;
```

```text
错误: 函数表达式中抛出的类型 IOException 不兼容
        Function<String,String> fn = Err4::risky;
                                     ^
```

**三种修复方案对比**：

| 方案 | 代码 | 适用场景 |
|------|------|---------|
| 换目标接口 | 定义 `interface ThrowingFunction<T,R> { R apply(T t) throws Exception; }` | 你能控制接口定义 |
| Lambda 内包装 | `s -> { try { return risky(s); } catch (IOException e) { throw new UncheckedIOException(e); } }` | 用 JDK 标准接口 |
| 工具方法转换 | `Function<String,String> fn = unchecked(Err4::risky);` | 大量此类调用，统一处理 |

包装工具的通用实现：

```java
@FunctionalInterface
interface ThrowingFunction<T, R> {
    R apply(T t) throws Exception;
}

static <T, R> Function<T, R> unchecked(ThrowingFunction<T, R> f) {
    return t -> {
        try {
            return f.apply(t);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    };
}

// 使用
Function<String, String> safe = unchecked(Err4::risky);
```

### 6.5 可见性规则

方法引用**遵循常规访问控制**，判定位置是"书写方法引用的那段代码"：

| 被引用方法 | 在同类中 | 在同包其他类 | 在子类 | 在无关类 |
|-----------|:---:|:---:|:---:|:---:|
| `public` | ✅ | ✅ | ✅ | ✅ |
| `protected` | ✅ | ✅ | ✅（受 `super::`/自身实例限制） | ❌ |
| 包私有 | ✅ | ✅ | 仅同包 | ❌ |
| `private` | ✅ | ❌ | ❌ | ❌ |

> 💡 引用私有方法在同一类内完全合法。编译器生成的 `lambda$xxx$0` 合成方法本身就是 `ACC_PRIVATE ACC_SYNTHETIC`，运行时由 `LambdaMetafactory` 通过带**私有访问权限的 `Lookup`** 完成链接——这也是 Lambda 不需要像早期匿名内部类那样生成 `access$000` 桥接方法的原因。

### 6.6 泛型方法引用

```java
static <T> List<T> wrap(T t) { return List.of(t); }

Function<String, List<String>> f = SuperRef::wrap;           // 类型推断
Function<String, List<String>> g = SuperRef::<String>wrap;   // 显式类型参数
```

> ⚠️ 显式类型参数写在 `::` **之后**：`Class::<T>method`，不是 `Class<T>::method`（后者是给类指定类型参数）。绝大多数场景推断即可，显式形式主要用于推断失败时兜底。

### 6.7 编译错误速查表

| 错误信息关键词 | 根因 | 优先检查 |
|--------------|------|---------|
| `实际参数列表和形式参数列表长度不同` | 参数个数错 | 是否漏算隐式首参数 |
| `在绑定查找中找到意外的静态 方法` | 用对象引用静态方法 | 改成 `类名::` |
| `对 xxx 的引用不明确` | 静态/实例双路径匹配 | 改写为 Lambda |
| `对 take 的引用不明确`（错在调用方） | 重载方法 + 重载参数 | 加显式转型 |
| `函数表达式中抛出的类型 X 不兼容` | 受检异常 | 包装或换接口 |
| `无法推断 x 的类型` | 用了 `var` | 声明具体函数式接口类型 |

---

## 7. 求值时机陷阱

### 7.1 绑定接收者是"立即求值"的

这是方法引用与 Lambda 最重要的**语义差异**，不只是写法区别：

```java
String s = null;
Supplier<Integer> sup = s::length;   // ← 这一行就抛 NPE！还没调用 get()

String t = null;
Supplier<Integer> lam = () -> t.length();   // ← 这一行正常，调用 get() 时才 NPE
```

实测输出：

```text
创建方法引用时立即 NPE: java.lang.NullPointerException
Lambda 创建成功，未抛异常
调用时才 NPE
```

字节码层面能看到编译器**显式插入了空检查**：

```text
java.util.function.Consumer<java.lang.String> boundRef();
  Code:
     0: getstatic     #27    // Field java/lang/System.out:Ljava/io/PrintStream;
     3: dup
     4: invokestatic  #33    // Method java/util/Objects.requireNonNull  ← 立即空检查
     7: pop
     8: invokedynamic #39,  0   // InvokeDynamic #4:accept:(Ljava/io/PrintStream;)Ljava/util/function/Consumer;
    13: areturn
```

> ⚠️ **实战影响**：`obj::method` 中的 `obj` 表达式在**创建方法引用时求值一次并锁定**。如果 `obj` 后续被重新赋值，方法引用**仍指向旧对象**。

```java
StringBuilder sb = new StringBuilder("A");
Consumer<String> c = sb::append;     // 锁定当前的 sb 对象
sb = new StringBuilder("B");         // 重新赋值不影响 c
c.accept("X");                       // 追加到最初那个 "A" 对象
```

### 7.2 表达式接收者只求值一次

```java
Consumer<String> c = getLogger()::info;   // getLogger() 只在这一行执行一次
// 对比
Consumer<String> l = s -> getLogger().info(s);   // 每次 accept 都调用 getLogger()
```

| 写法 | `getLogger()` 调用次数 | 适用场景 |
|------|:---:|------|
| `getLogger()::info` | 1 次（创建时） | 接收者稳定、希望复用 |
| `s -> getLogger().info(s)` | 每次调用 | 接收者可能变化、需动态获取 |

> 🎯 当接收者是**有副作用的表达式**或**可能变化的引用**时，方法引用与 Lambda **不等价**，不能机械替换。

---

## 8. 方法引用与 Lambda 的选择准则

### 8.1 决策流程

```text
能否写成"单一方法调用 + 参数原样透传"？
    │
    ├── 否 ──────────────────────────────────► 用 Lambda
    │
    └── 是
         │
         ├── 接收者表达式有副作用/会变化？ ──── 是 ──► 用 Lambda（语义不同）
         │
         ├── 存在重载歧义、需要显式转型？ ───── 是 ──► 用 Lambda（更清晰）
         │
         ├── 方法名无法自解释语义？ ─────────── 是 ──► 用 Lambda 或先重命名方法
         │
         └── 否 ─────────────────────────────► 用方法引用
```

### 8.2 优先方法引用的场景

| 场景 | 推荐写法 | 而非 |
|------|---------|------|
| 流式映射已有方法 | `.map(String::trim)` | `.map(s -> s.trim())` |
| 打印/日志消费 | `.forEach(System.out::println)` | `.forEach(s -> System.out.println(s))` |
| 收集到容器 | `.toArray(String[]::new)` | `.toArray(n -> new String[n])` |
| 比较器 | `Comparator.comparing(User::getAge)` | `Comparator.comparing(u -> u.getAge())` |
| 工厂 | `Optional.ofNullable(x).orElseGet(ArrayList::new)` | `orElseGet(() -> new ArrayList<>())` |

### 8.3 优先 Lambda 的场景

| 场景 | 推荐写法 | 原因 |
|------|---------|------|
| 参数需加工 | `s -> Integer.parseInt(s.trim())` | 非原样透传 |
| 方法名不达意 | `x -> Helper.p(x)` → 建议先改名 | 可读性优先于简洁 |
| 存在重载歧义 | `(String s) -> id(s)` | 显式类型消歧 |
| 需要空安全/异常处理 | `s -> { try {...} catch {...} }` | 逻辑无法压缩 |
| 接收者动态获取 | `s -> currentLogger().info(s)` | 与 `::` 语义不同 |

### 8.4 关于性能的准确表述

> ⚠️ **不要宣称"方法引用比 Lambda 快"**。

| 情形 | 实际差异 |
|------|---------|
| 语义等价的静态方法引用 vs Lambda | 编译产物几乎相同（Lambda 多一个 `lambda$` 合成方法转发），JIT 内联后**基本无差异** |
| 未绑定实例方法引用 vs 等价 Lambda | 方法引用**省掉一层**合成方法转发，理论上略优，实测通常不可测量 |
| 绑定实例方法引用 | 会**捕获接收者**，与非捕获 Lambda 相比多一次实例分配（详见 04） |

真正影响性能的是**是否捕获**（决定能否复用实例）与**是否装箱**，而不是"用了 `::` 还是 `->`"。

> 🎯 **结论**：按**可读性**选择，不要按臆想的性能选择。

### 8.5 团队规范建议

```text
✅ 推荐
  - 单一方法调用一律用 ::
  - Comparator.comparing / thenComparing 链一律用 ::
  - 需要 3 行以上逻辑时，抽成命名方法 + 方法引用（而不是写长 Lambda）

❌ 避免
  - 为了用 :: 而重构出语义不清的辅助方法
  - 在接收者可能为 null 的地方用 obj::method（会提前 NPE）
  - 在重载密集的 API 上使用不精确方法引用（增加维护脆弱性）
```

---

## 9. 动手实验

### 9.1 实验环境

```bash
# 检查版本（本文档实测基于 JDK 25）
java -version
javac -version
```

### 9.2 实验一：观察四类引用的 MethodHandle 种类

```java
// MethodRefLab.java
import java.util.function.*;

public class MethodRefLab {
    Function<String, Integer> staticRef()  { return Integer::parseInt; }
    Consumer<String>          boundRef()   { return System.out::println; }
    Function<String, String>  unboundRef() { return String::trim; }
    Supplier<StringBuilder>   ctorRef()    { return StringBuilder::new; }
    IntFunction<String[]>     arrayRef()   { return String[]::new; }
}
```

```bash
javac MethodRefLab.java
# 查看 BootstrapMethods 段——四类引用的 MethodHandle Kind 一目了然
javap -v -p MethodRefLab.class | sed -n '/^BootstrapMethods/,$p'
```

**预期观察点**：

1. 静态引用 → `REF_invokeStatic`（Kind 6），**无** `lambda$` 合成方法
2. 绑定引用 → `REF_invokeVirtual`（Kind 5），调用点签名**带**接收者参数
3. 未绑定引用 → `REF_invokeVirtual`（Kind 5），调用点签名**为空**
4. 构造器引用 → `REF_newInvokeSpecial`（Kind 8），直接指向 `<init>`
5. 数组引用 → `REF_invokeStatic` 指向**合成方法** `lambda$arrayRef$0`

### 9.3 实验二：验证绑定引用的立即求值

```java
// EagerLab.java
import java.util.function.*;

public class EagerLab {
    public static void main(String[] args) {
        String s = null;
        try {
            Supplier<Integer> sup = s::length;
            System.out.println("不应到这里");
        } catch (NullPointerException e) {
            System.out.println("创建时即 NPE");
        }

        String t = null;
        Supplier<Integer> lam = () -> t.length();
        System.out.println("Lambda 创建成功");
        try { lam.get(); } catch (NullPointerException e) {
            System.out.println("调用时才 NPE");
        }
    }
}
```

```bash
javac EagerLab.java && java EagerLab
```

### 9.4 实验三：复现三类编译错误

```bash
# 逐个编译以下片段，对照第 6 节的错误信息
javac Err1.java   # 参数个数不匹配
javac Err2.java   # 对象引用静态方法
javac Err4.java   # 受检异常不兼容
```

> 💡 **技巧**：加 `-Xdiags:verbose` 可让 javac 输出更详细的候选方法列表，排查歧义时非常有用。
>
> ```bash
> javac -Xdiags:verbose Amb3.java
> ```

### 9.5 实验四：确认目标类型驱动重载选择

```java
// TargetLab.java
import java.util.function.*;
import java.util.*;

public class TargetLab {
    public static void main(String[] args) {
        Supplier<List<String>>          s0 = ArrayList::new;   // () 构造器
        Function<Integer, List<String>> s1 = ArrayList::new;   // (int) 构造器
        Function<Collection<String>, List<String>> s2 = ArrayList::new;  // (Collection) 构造器

        System.out.println(s0.get());
        System.out.println(s1.apply(10));
        System.out.println(s2.apply(List.of("a", "b")));
    }
}
```

```bash
javac TargetLab.java && java TargetLab
# 再看 BootstrapMethods，会发现三个不同的 REF_newInvokeSpecial 指向不同构造器重载
javap -v -p TargetLab.class | grep -A3 "REF_newInvokeSpecial"
```

---

## 10. 速查与自测

### 10.1 五分钟速查卡

```text
┌───────────────────────────────────────────────────────────┐
│ 静态      Integer::parseInt   ≡ s      -> Integer.parseInt(s) │
│ 绑定      out::println        ≡ s      -> out.println(s)      │
│ 未绑定    String::trim        ≡ x      -> x.trim()            │
│ 未绑定2   String::startsWith  ≡ (x, y) -> x.startsWith(y)     │
│ 构造器    ArrayList::new      ≡ ()     -> new ArrayList<>()   │
│ 数组      String[]::new       ≡ n      -> new String[n]       │
│ this      this::name          ≡ ()     -> this.name()         │
│ super     super::name         ≡ ()     -> super.name()        │
└───────────────────────────────────────────────────────────┘

隐式首参数公式（仅 类名::实例方法）：
    接口参数数 = 方法参数数 + 1
```

### 10.2 自测 10 题

| # | 问题 | 答案要点 |
|:-:|------|---------|
| 1 | `String::length` 能适配几个参数的接口？ | 1 个（首参当接收者，方法 0 参） |
| 2 | `prefix::startsWith` 等价的 Lambda？ | `s -> prefix.startsWith(s)`，**不是** `s -> s.startsWith(prefix)` |
| 3 | `list::add` 能赋给 `Consumer<String>` 吗？ | 能，返回值 `boolean` 可丢弃 |
| 4 | `var f = Integer::parseInt;` 合法吗？ | 不合法，方法引用需要显式目标类型 |
| 5 | 构造器引用能用菱形 `<>` 吗？ | 不能，`ArrayList<>::new` 非法 |
| 6 | 数组引用会生成合成方法吗？ | 会，因为 `anewarray` 是指令不是方法 |
| 7 | `s::length` 中 `s` 为 null 何时抛 NPE？ | 创建方法引用时立即抛 |
| 8 | `super::name` 用哪条字节码指令？ | `invokespecial`，绕过虚分派 |
| 9 | 静态/实例同名何时才真歧义？ | 两条适配路径签名完全相同时 |
| 10 | 方法引用一定比 Lambda 快吗？ | 不一定，JIT 内联后基本无差异 |

### 10.3 常见误解澄清

| 误解 | 事实 |
|------|------|
| "`::` 只是语法糖，和 Lambda 完全等价" | 绑定接收者的**求值时机**不同，语义有实质差异 |
| "方法引用不生成任何合成方法" | 数组构造器引用会生成；普通 Lambda 通常也会生成 |
| "`Class::method` 里 Class 是接收者" | 未绑定形式中接收者来自**第一个参数** |
| "方法引用比 Lambda 性能好" | 语义等价时基本无差异，真正影响性能的是捕获与装箱 |
| "编译器靠方法名就能确定引用哪个方法" | 靠**目标类型 + 方法名**共同确定，缺一不可 |

---

## 版本与参考

| 项 | 说明 |
|----|------|
| 实测环境 | JDK 25.0.2 LTS（`javac 25.0.2`），错误信息为中文语言环境输出 |
| 语言规范 | JLS §15.13 Method Reference Expressions；§15.12.2 重载决议 |
| 核心 API | `java.lang.invoke.LambdaMetafactory`、`MethodHandles.Lookup` |
| 兼容性 | 四类方法引用自 Java 8 起语义稳定；本文结论在 Java 8–25 通用，字节码细节以 JDK 25 为准 |
| 扩展阅读 | Effective Java 第 3 版 Item 43「方法引用优先于 Lambda」（并注意其"可读性优先"的前提） |

---

**上一模块**：[02-Lambda函数式接口全家桶](./02-Lambda函数式接口全家桶.md)
**下一模块**：[04-Lambda底层原理与字节码](./04-Lambda底层原理与字节码.md)
**返回总览**：[00-Lambda表达式知识体系总览](./00-Lambda表达式知识体系总览.md)
