# 04 Lambda 底层原理与字节码

> Lambda 不是匿名内部类的语法糖——它是"编译期埋下 invokedynamic 占位、运行期由 LambdaMetafactory 动态生成实现类"的**延迟链接**机制

---

## 📚 目录

1. [核心结论先行](#1-核心结论先行)
2. [javac 编译产物剖析](#2-javac-编译产物剖析)
3. [invokedynamic 与常量池](#3-invokedynamic-与常量池)
4. [LambdaMetafactory 工作流程](#4-lambdametafactory-工作流程)
5. [CallSite MethodHandle MethodType 三件套](#5-callsite-methodhandle-methodtype-三件套)
6. [捕获型与非捕获型 Lambda](#6-捕获型与非捕获型-lambda)
7. [运行时生成的类到底是什么](#7-运行时生成的类到底是什么)
8. [this 语义与作用域](#8-this-语义与作用域)
9. [签名适配与桥接](#9-签名适配与桥接)
10. [序列化与 SerializedLambda](#10-序列化与-serializedlambda)
11. [性能特征与 JIT](#11-性能特征与-jit)
12. [javap 实验手册](#12-javap-实验手册)
13. [常见误解澄清](#13-常见误解澄清)

---

## 1. 核心结论先行

### 1.1 一张图看懂全流程

```text
编译期（javac）                          运行期（JVM 首次执行到该行）
─────────────────────────────           ─────────────────────────────────────
Runnable r = () -> foo();
        │                                
        ├─► ① Lambda 体抽取为               ④ 遇到 invokedynamic
        │      private static             │  发现调用点未链接
        │      lambda$m$0()               ▼
        │                                 ⑤ 执行 BootstrapMethod
        ├─► ② 生成 invokedynamic 指令        LambdaMetafactory.metafactory(...)
        │      引用 BSM #0                 │
        │                                 ▼
        └─► ③ BootstrapMethods 属性        ⑥ 运行时生成实现类（隐藏类）
               记录 metafactory +          │  implements Runnable
               三个 MethodType +           ▼
               implMethod 句柄            ⑦ 返回 CallSite，绑定实例/工厂
                                          │
                                          ▼
                                          ⑧ 后续执行直接走已链接目标
                                             （链接只发生一次）
```

### 1.2 六条必须记住的结论

| # | 结论 | 常见错误说法 |
|:-:|------|-------------|
| 1 | Lambda **不在编译期**生成 `Outer$1.class` 文件 | ❌"Lambda 就是匿名内部类" |
| 2 | Lambda 体通常被抽取为**同类内的合成方法** `lambda$xxx$0` | ❌"Lambda 体没有对应方法" |
| 3 | 实现类在**运行期**由 `LambdaMetafactory` 生成 | ❌"Lambda 永远不生成实现类" |
| 4 | **非捕获** Lambda 的实例**在当前实现中**会被复用 | ❌"非捕获 Lambda 一定单例"（规范未保证） |
| 5 | **捕获** Lambda 每次求值**通常**产生新实例 | ❌"Lambda 都是单例" |
| 6 | Lambda 中的 `this` 指向**外围类实例** | ❌"this 指向 Lambda 自身" |

> ⚠️ **规范与实现的边界**：`LambdaMetafactory` 的 JavaDoc 明确说明——它**不保证**返回实例的同一性（identity）、可变性或可序列化性。因此"非捕获 Lambda 缓存复用"是 **HotSpot 当前实现的行为**，是有意留给 JVM 的优化空间，**不是语言规范承诺**。写代码时绝不能依赖 `==` 比较 Lambda 实例。

### 1.3 为什么设计成这样

| 设计目标 | 用匿名内部类实现的问题 | invokedynamic 方案的优势 |
|---------|---------------------|----------------------|
| 类文件膨胀 | 每个 Lambda 一个 class 文件 | 编译期零额外 class 文件 |
| 启动性能 | 大量小类需加载验证 | 未执行到的 Lambda **永不链接** |
| 未来演进 | 翻译策略固化在字节码里 | 策略在 JDK 内部，可**随版本优化** |
| 实例复用 | `new` 语义强制每次新建 | 可由运行时决定是否缓存 |

> 🎯 **最关键的一点是第三条**：invokedynamic 把"Lambda 如何变成对象"这个决策从**编译期字节码**推迟到**运行时 JDK 库代码**。这意味着 JDK 可以在不重新编译用户代码的前提下改进 Lambda 实现（事实上从 Java 8 到 Java 15 的隐藏类改造正是如此发生的）。

---

## 2. javac 编译产物剖析

> 本节所有字节码均为 **JDK 25.0.2 实测输出**。

### 2.1 实验源码

```java
// LambdaDemo.java
import java.util.function.*;

public class LambdaDemo {
    private int field = 100;

    Supplier<String>  stateless()               { return () -> "hello"; }
    Supplier<String>  captureLocal(String pre)  { return () -> pre + "!"; }
    Supplier<Integer> captureThis()             { return () -> field + 1; }
    Function<String, Integer> staticRef()       { return Integer::parseInt; }
    Consumer<String>  boundRef()                { return System.out::println; }
    Function<String, String> unboundRef()       { return String::trim; }
    Supplier<StringBuilder> ctorRef()           { return StringBuilder::new; }
    IntFunction<String[]> arrayRef()            { return String[]::new; }
}
```

### 2.2 编译产物：只有一个 class 文件

```bash
javac LambdaDemo.java && ls -la *.class
```

```text
-rw-r--r--  3461  LambdaDemo.class      ← 只有这一个，没有 LambdaDemo$1.class
```

对照匿名内部类：

```java
// AnonVsLambda.java
public class AnonVsLambda {
    Runnable anon()   { return new Runnable() { public void run() { System.out.println("anon"); } }; }
    Runnable lambda() { return () -> System.out.println("lambda"); }
}
```

```text
-rw-r--r--   694  AnonVsLambda$1.class   ← 匿名内部类产生独立 class 文件
-rw-r--r--  1075  AnonVsLambda.class     ← Lambda 未产生额外文件
```

> 🎯 这是"Lambda ≠ 匿名内部类"最直观的证据：**同一个文件里两种写法，只有匿名类多出一个 class 文件**。

### 2.3 调用点：清一色 invokedynamic

```bash
javap -c -p LambdaDemo.class
```

```text
java.util.function.Supplier<java.lang.String> stateless();
  Code:
     0: invokedynamic #13,  0   // InvokeDynamic #0:get:()Ljava/util/function/Supplier;
     5: areturn
                                //  ↑ 调用点签名为空 → 非捕获

java.util.function.Supplier<java.lang.String> captureLocal(java.lang.String);
  Code:
     0: aload_1                 // 把要捕获的局部变量压栈
     1: invokedynamic #17,  0   // InvokeDynamic #1:get:(Ljava/lang/String;)Ljava/util/function/Supplier;
     6: areturn
                                //  ↑ 签名带 String → 捕获了一个变量

java.util.function.Supplier<java.lang.Integer> captureThis();
  Code:
     0: aload_0                 // 把 this 压栈
     1: invokedynamic #20,  0   // InvokeDynamic #2:get:(LLambdaDemo;)Ljava/util/function/Supplier;
     6: areturn
                                //  ↑ 签名带 LambdaDemo → 捕获了 this
```

> 💡 **看调用点签名就知道捕获了什么**：`invokedynamic` 的**参数类型列表 = 捕获变量列表**，**返回类型 = 目标函数式接口**。这是分析 Lambda 最快的方法。

### 2.4 合成方法：Lambda 体的真实归宿

```text
private static java.lang.String lambda$stateless$0();
  descriptor: ()Ljava/lang/String;
  flags: (0x100a) ACC_PRIVATE, ACC_STATIC, ACC_SYNTHETIC     ← 静态：不需要 this
  Code:
     0: ldc  #60    // String hello
     2: areturn

private static java.lang.String lambda$captureLocal$0(java.lang.String);
  flags: (0x100a) ACC_PRIVATE, ACC_STATIC, ACC_SYNTHETIC     ← 静态：捕获值作为参数传入
  Code:
     0: aload_0
     1: invokedynamic #56,  0   // makeConcatWithConstants（字符串拼接也用 indy）
     6: areturn

private java.lang.Integer lambda$captureThis$0();
  flags: (0x1002) ACC_PRIVATE, ACC_SYNTHETIC                 ← 实例方法！没有 ACC_STATIC
  Code:
     0: aload_0
     1: getfield  #7    // Field field:I
     4: iconst_1
     5: iadd
     6: invokestatic  #50   // Integer.valueOf
     9: areturn
```

**合成方法命名规则**：`lambda$` + 外围方法名 + `$` + 该方法内的序号。

| 特征 | 含义 |
|------|------|
| `ACC_SYNTHETIC` | 编译器生成，不在源码中，反射时可通过 `isSynthetic()` 过滤 |
| `ACC_PRIVATE` | 仅本类可见；`LambdaMetafactory` 通过带私有权限的 `Lookup` 访问 |
| 有/无 `ACC_STATIC` | **访问了实例状态就是实例方法**，否则是静态方法 |

### 2.5 哪些情况不生成合成方法

| 写法 | 是否生成 `lambda$` 方法 | 原因 |
|------|:---:|------|
| `() -> "hello"` | ✅ 生成 | Lambda 体需要载体 |
| `Integer::parseInt` | ❌ 不生成 | 直接引用已有方法 |
| `System.out::println` | ❌ 不生成 | 直接引用已有方法 |
| `String::trim` | ❌ 不生成 | 直接引用已有方法 |
| `StringBuilder::new` | ❌ 不生成 | 直接引用 `<init>` |
| `String[]::new` | ✅ 生成 | `anewarray` 是指令不是方法 |

实测中 `LambdaDemo` 共 8 个函数式表达式，只生成了 **4 个**合成方法（3 个 Lambda + 1 个数组引用）。

> ⚠️ **这解释了一个常见误区**：有人说"方法引用比 Lambda 少一层调用所以更快"——从编译产物看确实少了一层转发方法，但 JIT 内联后这层转发通常被完全消除，实测差异不可测量（见第 11 节）。

---

## 3. invokedynamic 与常量池

### 3.1 五种方法调用指令定位

| 指令 | 目标确定时机 | 可否改变 | 引入版本 |
|------|-------------|:---:|:---:|
| `invokestatic` | 类加载解析期 | 否 | 1.0 |
| `invokespecial` | 类加载解析期 | 否 | 1.0 |
| `invokevirtual` | 运行期虚分派（但方法表编译期定） | 否 | 1.0 |
| `invokeinterface` | 运行期接口分派 | 否 | 1.0 |
| **`invokedynamic`** | **首次执行时由 BSM 决定** | **是** | **7 (JSR 292)** |

`invokedynamic` 的独特之处：**字节码里不写死目标方法**，而是写一个"引导方法（Bootstrap Method, BSM）"的索引，由 BSM 在运行时返回真正的调用目标。

### 3.2 指令格式

```text
invokedynamic  #13,  0
               │     │
               │     └── 固定为 0（历史遗留，供未来扩展）
               └──────── 指向常量池 CONSTANT_InvokeDynamic_info
```

`CONSTANT_InvokeDynamic_info` 包含两部分：

```text
CONSTANT_InvokeDynamic_info {
    u2 bootstrap_method_attr_index;   // → BootstrapMethods 属性表的下标
    u2 name_and_type_index;           // → 方法名 + 调用点签名
}
```

对应 javap 的注释输出：

```text
invokedynamic #17,  0   // InvokeDynamic #1:get:(Ljava/lang/String;)Ljava/util/function/Supplier;
                        //               │   │   └── 调用点签名（捕获参数 → 接口类型）
                        //               │   └────── 接口的抽象方法名
                        //               └────────── BootstrapMethods 表下标
```

### 3.3 BootstrapMethods 属性

这是理解 Lambda 底层的**核心数据结构**。JDK 25 实测：

```text
BootstrapMethods:
  0: #142 REF_invokeStatic java/lang/invoke/LambdaMetafactory.metafactory:(
             Ljava/lang/invoke/MethodHandles$Lookup;
             Ljava/lang/String;
             Ljava/lang/invoke/MethodType;
             Ljava/lang/invoke/MethodType;
             Ljava/lang/invoke/MethodHandle;
             Ljava/lang/invoke/MethodType;
         )Ljava/lang/invoke/CallSite;
    Method arguments:
      #92 ()Ljava/lang/Object;                                         ← samMethodType
      #94 REF_invokeStatic LambdaDemo.lambda$stateless$0:()Ljava/lang/String;  ← implMethod
      #97 ()Ljava/lang/String;                                         ← instantiatedMethodType
```

**每个 Lambda / 方法引用在 BootstrapMethods 表中占一项**，实测 `LambdaDemo` 有 9 项（0–7 为 Lambda/方法引用，8 为字符串拼接的 `StringConcatFactory`）。

### 3.4 三个 MethodType 参数的分工

这是最容易搞混的地方：

| 参数 | 含义 | `Integer::parseInt` 实例 |
|------|------|------------------------|
| **samMethodType** | 函数式接口抽象方法的**擦除后**签名 | `(Object) -> Object` |
| **implMethod** | 真正要调用的方法句柄 | `REF_invokeStatic Integer.parseInt:(String)int` |
| **instantiatedMethodType** | 泛型**实例化后**的签名 | `(String) -> Integer` |

```text
3: REF_invokeStatic LambdaMetafactory.metafactory:(...)
  Method arguments:
    #105 (Ljava/lang/Object;)Ljava/lang/Object;         ← samMethodType（擦除）
    #106 REF_invokeStatic java/lang/Integer.parseInt:(Ljava/lang/String;)I   ← implMethod
    #111 (Ljava/lang/String;)Ljava/lang/Integer;        ← instantiatedMethodType（实例化）
```

三者的**差值决定了适配层要做什么**：

```text
samMethodType         (Object)   -> Object
                         │           ▲
                    ① checkcast     │ ③ 装箱 int→Integer
                         ▼           │
instantiatedMethodType (String)  -> Integer
                         │           ▲
                    ② 直接传递      │
                         ▼           │
implMethod            (String)   -> int
```

> 🎯 生成的实现类里，`apply(Object)` 方法会：**checkcast 到 String → 调用 parseInt 得到 int → Integer.valueOf 装箱 → 返回**。这三步适配代码就是从三个 MethodType 的差值推导出来的。

### 3.5 常量池中的 MethodHandle 种类

```text
#94  = MethodHandle  6:#95   // REF_invokeStatic       LambdaDemo.lambda$stateless$0
#101 = MethodHandle  5:#102  // REF_invokeVirtual      LambdaDemo.lambda$captureThis$0
#115 = MethodHandle  5:#116  // REF_invokeVirtual      PrintStream.println
#128 = MethodHandle  8:#129  // REF_newInvokeSpecial   StringBuilder."<init>"
```

| Kind | 名称 | 出现场景 |
|:---:|------|---------|
| 5 | `REF_invokeVirtual` | 捕获 this 的 Lambda、实例方法引用 |
| 6 | `REF_invokeStatic` | 非捕获/仅捕获局部变量的 Lambda、静态方法引用 |
| 7 | `REF_invokeSpecial` | `super::method` |
| 8 | `REF_newInvokeSpecial` | 构造器引用 |
| 9 | `REF_invokeInterface` | 接口默认方法引用 |

> 💡 **Kind 6 还是 Kind 5，一眼看出是否捕获 this**：合成方法是 `static` 的（Kind 6）说明 Lambda 体没碰实例状态；是实例方法（Kind 5）说明捕获了 `this`。

---

## 4. LambdaMetafactory 工作流程

### 4.1 方法签名

```java
public static CallSite metafactory(
        MethodHandles.Lookup caller,        // ① 调用方的 Lookup（携带私有访问权限）
        String interfaceMethodName,         // ② 要实现的方法名，如 "get" / "apply"
        MethodType factoryType,             // ③ 调用点签名：(捕获参数...) -> 函数式接口
        MethodType interfaceMethodType,     // ④ samMethodType（擦除后）
        MethodHandle implementation,        // ⑤ implMethod
        MethodType dynamicMethodType        // ⑥ instantiatedMethodType
) throws LambdaConversionException
```

前三个参数由 **JVM 自动传入**（BSM 标准约定），后三个来自 BootstrapMethods 的 `Method arguments`。

### 4.2 完整链接时序

```text
【首次执行到 invokedynamic 指令】
   │
   ├─ ① JVM 检查该调用点是否已链接
   │       已链接 → 跳到 ⑧
   │
   ├─ ② 解析常量池，取出 BSM 及其静态参数
   │
   ├─ ③ JVM 构造 Lookup（含调用类的私有访问权限）
   │       这是能访问 private lambda$xxx$0 的关键
   │
   ├─ ④ 调用 LambdaMetafactory.metafactory(...)
   │       │
   │       ├─ 校验：implMethod 签名能否适配到 instantiatedMethodType
   │       │        失败 → 抛 LambdaConversionException
   │       │
   │       ├─ 交由 InnerClassLambdaMetafactory 生成实现类字节码
   │       │        - implements 目标函数式接口
   │       │        - 捕获参数 → final 字段 + 构造器
   │       │        - 接口方法 → 适配 + 转发到 implMethod
   │       │
   │       ├─ 通过 Lookup.defineHiddenClass 定义为隐藏类（JDK 15+）
   │       │
   │       └─ 返回 CallSite：
   │              非捕获 → ConstantCallSite(常量句柄，返回缓存的单例)
   │              捕获   → ConstantCallSite(构造器句柄，每次 new)
   │
   ├─ ⑤ JVM 把返回的 CallSite 永久绑定到该 invokedynamic 调用点
   │
   ├─ ⑥ 调用 CallSite.dynamicInvoker() 获得目标 MethodHandle
   │
   ├─ ⑦ 执行：栈上的捕获参数 → 传给目标句柄 → 得到函数式接口实例
   │
   └─ ⑧ 【后续执行】直接走已绑定目标，无任何额外开销
```

> ⚠️ **链接只发生一次，但每次执行仍会调用目标句柄**。对捕获型 Lambda 来说，"目标句柄"是构造器，所以每次执行都 `new` 一个实例；对非捕获型来说是返回常量，所以复用同一实例。

### 4.3 altMetafactory：可序列化与多接口场景

当 Lambda 需要**序列化**或**实现多个接口**时，编译器改用 `altMetafactory`：

```text
BootstrapMethods:
  0: #226 REF_invokeStatic java/lang/invoke/LambdaMetafactory.altMetafactory:(
             Ljava/lang/invoke/MethodHandles$Lookup;
             Ljava/lang/String;
             Ljava/lang/invoke/MethodType;
             [Ljava/lang/Object;                       ← 变长参数，承载额外标志
         )Ljava/lang/invoke/CallSite;
    Method arguments:
      #194 (Ljava/lang/Object;)Ljava/lang/Object;
      #195 REF_invokeVirtual java/lang/String.length:()I
      #198 (Ljava/lang/String;)Ljava/lang/Integer;
      #200 5                                            ← flags = FLAG_SERIALIZABLE(1) | FLAG_BRIDGES(4)
      #201 0                                            ← 额外标记接口数量 = 0
```

| flags 位 | 常量 | 值 | 含义 |
|:---:|------|:---:|------|
| bit 0 | `FLAG_SERIALIZABLE` | 1 | 生成 `writeReplace` 方法 |
| bit 1 | `FLAG_MARKERS` | 2 | 附加标记接口 |
| bit 2 | `FLAG_BRIDGES` | 4 | 生成桥接方法 |

> 💡 实测 flags = 5 = `SERIALIZABLE(1) + BRIDGES(4)`。之所以带 BRIDGES，是因为示例中的 `SFun<T,R> extends Function<T,R>, Serializable` 继承了父接口的方法，需要桥接方法适配（见第 9 节）。

### 4.4 metafactory vs altMetafactory 对比

| 维度 | `metafactory` | `altMetafactory` |
|------|--------------|-----------------|
| 参数形式 | 固定 6 个 | 3 固定 + 变长 `Object[]` |
| 触发条件 | 普通 Lambda | 可序列化 / 多标记接口 / 需桥接 |
| 生成类特征 | 仅实现目标接口 | 可能额外实现 `Serializable`、含 `writeReplace` |
| 性能 | 基准 | 链接稍慢，运行时无差异 |

---

## 5. CallSite MethodHandle MethodType 三件套

### 5.1 三者关系

```text
        ┌─────────────────────────────────────────┐
        │  CallSite（调用点）                       │
        │  ─ 代表一个 invokedynamic 位置            │
        │  ─ 持有一个可变/不可变的 target           │
        │                                          │
        │      target ──────► MethodHandle         │
        │                     （方法句柄）           │
        │                     ─ 可直接执行的引用     │
        │                     ─ 可组合/变换          │
        │                                          │
        │                     type() ──► MethodType│
        │                                （方法类型）│
        │                                ─ 参数+返回 │
        └─────────────────────────────────────────┘
```

### 5.2 CallSite 三种实现

| 类型 | target 可变 | 使用场景 | Lambda 用哪个 |
|------|:---:|---------|:---:|
| `ConstantCallSite` | 否 | 目标永不改变 | ✅ |
| `MutableCallSite` | 是 | 可重新绑定，如动态语言 | ❌ |
| `VolatileCallSite` | 是（跨线程立即可见） | 多线程重绑定 | ❌ |

> 🎯 Lambda 使用 `ConstantCallSite`——这一点很重要：**目标不可变意味着 JIT 可以放心地把它当常量做激进内联优化**，这是 Lambda 性能能追平匿名内部类的关键。

### 5.3 MethodType

`MethodType` 描述"参数类型列表 + 返回类型"，**不含方法名**，是不可变值对象。

```java
import java.lang.invoke.*;

MethodType mt1 = MethodType.methodType(int.class, String.class);      // (String)int
MethodType mt2 = MethodType.methodType(void.class);                    // ()void
MethodType mt3 = MethodType.fromMethodDescriptorString(
        "(Ljava/lang/String;)I", ClassLoader.getSystemClassLoader());   // 从描述符构造

System.out.println(mt1);                       // (String)int
System.out.println(mt1.erase());               // (Object)int      擦除引用类型
System.out.println(mt1.wrap());                // (String)Integer  基本类型装箱
System.out.println(mt1.parameterCount());      // 1
```

### 5.4 MethodHandle 手动模拟 Lambda 链接

理解原理最好的方式是**手写一遍 `LambdaMetafactory` 的调用**：

```java
// ManualLambda.java
import java.lang.invoke.*;
import java.util.function.*;

public class ManualLambda {
    public static void main(String[] args) throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        // implMethod：要包装的目标方法 Integer.parseInt(String) -> int
        MethodHandle impl = lookup.findStatic(
                Integer.class, "parseInt",
                MethodType.methodType(int.class, String.class));

        CallSite site = LambdaMetafactory.metafactory(
                lookup,                                                     // ① caller
                "apply",                                                    // ② 接口方法名
                MethodType.methodType(Function.class),                      // ③ 工厂签名：()->Function（无捕获）
                MethodType.methodType(Object.class, Object.class),          // ④ samMethodType（擦除）
                impl,                                                       // ⑤ implMethod
                MethodType.methodType(Integer.class, String.class));        // ⑥ instantiatedMethodType

        @SuppressWarnings("unchecked")
        Function<String, Integer> f =
                (Function<String, Integer>) site.getTarget().invokeExact();

        System.out.println("结果 = " + f.apply("123"));
        System.out.println("类名 = " + f.getClass().getName());
        System.out.println("隐藏类 = " + f.getClass().isHidden());
    }
}
```

这段代码手工完成了编译器 + JVM 在 `Function<String,Integer> f = Integer::parseInt;` 背后做的全部工作。

> 💡 **捕获型的区别**：如果要模拟捕获，把参数 ③ 改为 `MethodType.methodType(Function.class, String.class)`（工厂接收一个捕获参数），然后 `site.getTarget().invokeExact("captured")` 传入。

---

## 6. 捕获型与非捕获型 Lambda

### 6.1 分类判定

| 类型 | 定义 | 合成方法修饰 | 调用点签名 |
|------|------|------------|-----------|
| **非捕获**（无状态） | 不引用任何外部可变上下文 | `static` | `()` 空参 |
| **捕获局部变量** | 引用 effectively final 局部变量 | `static` | 带被捕获变量 |
| **捕获 this** | 访问实例字段/实例方法 | 实例方法（无 static） | 带外围类型 |

```java
Supplier<String>  a = () -> "hello";      // 非捕获
Supplier<String>  b = () -> pre + "!";    // 捕获局部变量 pre
Supplier<Integer> c = () -> field + 1;    // 捕获 this（field 是实例字段）
Supplier<Integer> d = () -> CONST + 1;    // 非捕获！静态常量不算捕获
```

> ⚠️ **易错点**：访问 `static` 字段**不构成捕获**（静态字段通过 `getstatic` 直接访问，无需上下文）；访问实例字段**构成对 this 的捕获**（哪怕只读一个字段，捕获的也是整个 `this` 引用）。

### 6.2 实例复用的实测行为

```java
// IdentityTest.java 关键片段
static Supplier<String> nonCapturing()          { return () -> "x"; }
static Supplier<String> capturing(String s)     { return () -> s; }
static Runnable siteA() { return () -> {}; }
static Runnable siteB() { return () -> {}; }
```

JDK 25 实测输出：

```text
非捕获 同一调用点 两次: same=true
  class=IdentityTest$$Lambda/0x000000003a040210
  isHidden=true
  interfaces=[interface java.util.function.Supplier]
捕获 同参数 两次: same=false
  class=IdentityTest$$Lambda/0x000000003a040438
两个不同调用点(代码相同): sameClass=false
  A=IdentityTest$$Lambda/0x000000003a041000
  B=IdentityTest$$Lambda/0x000000003a041228
方法引用 同一方法 不同调用点: same=false sameClass=false
```

**四条关键观察**：

| 观察 | 结论 |
|------|------|
| 非捕获同调用点 `same=true` | 当前实现复用同一实例 |
| 捕获型 `same=false` | 即使捕获值相同，也是新实例（**不做值缓存**） |
| 不同调用点 `sameClass=false` | **每个调用点生成独立的实现类**，代码相同也不共享 |
| 方法引用不同调用点不共享 | 同上，链接是**按调用点**而非按目标方法 |

> ⚠️ **绝对不要依赖这些行为**：
> - 不要用 `==` 比较 Lambda
> - 不要把 Lambda 当作 map 的 key 后期望能取回
> - 不要依赖"非捕获必单例"做资源管理
>
> `LambdaMetafactory` JavaDoc 原文明确不保证同一性。上述均为 HotSpot 当前实现行为。

### 6.3 为什么捕获型不能缓存

```java
for (int i = 0; i < 3; i++) {
    int captured = i;
    list.add(() -> System.out.println(captured));   // 必须是 3 个不同实例
}
```

每个实例持有**不同的捕获值**，必须是独立对象。生成的实现类形如：

```java
// 概念等价代码（实际为运行时生成的隐藏类）
final class Outer$$Lambda implements Runnable {
    private final int arg$1;                       // 捕获值 → final 字段
    private Outer$$Lambda(int arg$1) { this.arg$1 = arg$1; }
    @Override public void run() {
        Outer.lambda$m$0(arg$1);                   // 转发到合成方法
    }
}
```

而非捕获型没有字段，所有实例行为完全一致，**可以安全复用一个**：

```java
final class Outer$$Lambda implements Runnable {
    // 无字段
    @Override public void run() { Outer.lambda$m$0(); }
}
```

### 6.4 effectively final 的底层原因

```java
int count = 0;
Runnable r = () -> System.out.println(count);   // ✅
count++;                                         // ❌ 编译错误：count 必须是 effectively final
```

**根本原因是值捕获（capture by value）**：

```text
局部变量 count 存在于【调用栈帧】中
        │
        │  方法返回后，栈帧销毁
        ▼
Lambda 实例可能存活到方法返回之后（如被 return 出去、放入集合、提交给线程池）
        │
        ▼
只能【复制值】到实例字段，无法引用栈上的变量
        │
        ▼
若允许修改 count，则出现"两份副本不同步"的语义混乱
        │
        ▼
Java 的选择：编译期强制 effectively final，从根上杜绝歧义
```

> 💡 **对比其他语言**：JavaScript / Kotlin 支持引用捕获（把变量提升到堆对象）。Java 选择值捕获是为了**避免隐式的堆分配和并发可见性问题**——如果允许修改，多线程下 `count` 的可见性就需要额外的内存屏障语义，这会让简单的 Lambda 变得昂贵且难以推理。

**绕过写法及其代价**：

```java
// 方式一：数组（不推荐，无线程安全保证）
int[] counter = {0};
Runnable r = () -> counter[0]++;

// 方式二：AtomicInteger（推荐，线程安全）
AtomicInteger counter = new AtomicInteger();
Runnable r = () -> counter.incrementAndGet();

// 方式三：重新设计（最佳）—— 用返回值而非副作用
long count = list.stream().filter(pred).count();
```

### 6.5 捕获成本对照

| 捕获形态 | 实例分配 | 额外内存 | 说明 |
|---------|:---:|---------|------|
| 非捕获 | 首次一次 | 0 | 后续复用 |
| 捕获 1 个 int | 每次求值 | 对象头 + 4B | 可能被 JIT 标量替换消除 |
| 捕获 this | 每次求值 | 对象头 + 引用 | **注意隐式持有外围对象** |

> ⚠️ **内存泄漏风险**：捕获 `this` 的 Lambda 会**强引用外围对象**。如果把它注册到长生命周期的监听器列表而忘记注销，外围对象（可能是一个大 Activity / Controller / Bean）无法回收。

```java
class BigService {
    private byte[] cache = new byte[100 * 1024 * 1024];
    void register() {
        // ❌ 捕获 this，EventBus 持有 → BigService 及其 100MB cache 无法回收
        EventBus.subscribe(e -> this.handle(e));

        // ✅ 只捕获需要的字段，不捕获 this
        final Handler h = this.handler;
        EventBus.subscribe(e -> h.handle(e));
    }
}
```

---

## 7. 运行时生成的类到底是什么

### 7.1 类名格式的版本演进

```text
外围类名 + "$$Lambda" + 分隔符 + 标识
```

| JDK 版本 | 类名示例 | 类型 |
|:---:|---------|------|
| 8 – 14 | `IdentityTest$$Lambda$1/0x00000008000a1c40` | 匿名类（VM Anonymous Class） |
| 15 – 20 | `IdentityTest$$Lambda$14/0x0000000800c0a000` | 隐藏类（Hidden Class, JEP 371） |
| 21+ | `IdentityTest$$Lambda/0x000000003a040210` | 隐藏类（去掉了序号） |

JDK 25 实测：

```text
class=IdentityTest$$Lambda/0x000000003a040210
isHidden=true
interfaces=[interface java.util.function.Supplier]
```

> ⚠️ **不要在代码中解析或依赖这个类名格式**——它在 Java 8 / 15 / 21 都发生过变化。日志里看到 `$$Lambda` 只用于人工诊断。

### 7.2 隐藏类（Hidden Class）的特性

JEP 371（Java 15）引入隐藏类，Lambda 实现类随之迁移：

| 特性 | 说明 | 对 Lambda 的意义 |
|------|------|----------------|
| 不可被名称发现 | `Class.forName` 找不到 | 避免污染类名空间 |
| 不可作为字段/参数类型 | 无法在字节码中引用 | 防止外部依赖内部实现 |
| 可被 GC 单独卸载 | 与定义它的 `Lookup` 生命周期绑定 | **动态生成的类可回收** |
| 支持 `@Stable` 等 VM 注解 | 内部优化 | JIT 优化更激进 |

> 💡 **为什么这件事重要**：Java 8 用的 VM Anonymous Class 是 HotSpot 的私有机制，属于"内部黑魔法"。JEP 371 把它标准化为公开 API 后，Lambda 实现从"依赖 VM 私有特性"变成"依赖标准 API"，同时改善了元空间回收行为。这正是第 1.3 节所说"invokedynamic 让 JDK 能在不重编译用户代码的前提下改进实现"的实证。

### 7.3 类的加载与卸载

```bash
# 观察 Lambda 相关类的加载
java -Xlog:class+load=info IdentityTest 2>&1 | grep -i lambda | head -5
```

实测输出：

```text
[0.025s][info][class,load] java.lang.invoke.LambdaForm source: shared objects file
[0.043s][info][class,load] IdentityTest source: file:/.../lambdalab/
[0.043s][info][class,load] java.lang.invoke.LambdaMetafactory source: shared objects file
[0.044s][info][class,load] java.lang.invoke.LambdaForm$NamedFunction source: shared objects file
[0.044s][info][class,load] java.lang.invoke.LambdaForm$Kind source: shared objects file
```

> 💡 可以看到 `LambdaMetafactory`、`LambdaForm` 等基础设施在**第一个 Lambda 链接时**才被加载——这也是"应用第一个 Lambda 有额外启动开销"的来源（见 11.3）。

### 7.4 关于 dumpProxyClasses 的重要提醒

许多老教程会写：

```bash
java -Djdk.internal.lambda.dumpProxyClasses=. YourClass
```

> ⚠️ **JDK 25 实测：该属性已不再产出文件**（dump 目录为空）。这个属性是 JDK 内部实现细节，随隐藏类改造已失效。

**当前可用的替代观测手段**：

| 手段 | 命令 | 能看到什么 |
|------|------|-----------|
| 类加载日志 | `java -Xlog:class+load=info` | Lambda 类何时被定义 |
| 反射检查 | `obj.getClass().isHidden()` | 确认是隐藏类 |
| 接口列表 | `obj.getClass().getInterfaces()` | 确认实现的接口 |
| BSM 分析 | `javap -v -p` | 编译期完整信息（**最可靠**） |
| JFR | `-XX:StartFlightRecording` | 类加载/编译事件统计 |

> 🎯 **实践建议**：与其纠结于 dump 运行时生成的类，不如吃透 `javap -v` 输出的 BootstrapMethods——那里包含了推导出实现类结构所需的**全部信息**（接口、方法名、捕获参数、适配签名）。

---

## 8. this 语义与作用域

### 8.1 词法作用域 vs 独立作用域

| 维度 | 匿名内部类 | Lambda |
|------|-----------|--------|
| `this` | 指向匿名类实例 | 指向**外围类实例** |
| 作用域 | 引入新作用域 | **不引入**新作用域（词法作用域） |
| 同名局部变量 | 可以遮蔽（shadow） | **不允许**，编译错误 |
| `super` | 指向匿名类父类（通常 Object） | 指向外围类父类 |

```java
public class ThisDemo {
    private String name = "Outer";

    void test() {
        Runnable anon = new Runnable() {
            private String name = "Anon";
            @Override public void run() {
                System.out.println(this.name);          // "Anon"
                System.out.println(ThisDemo.this.name); // "Outer"
            }
        };

        Runnable lambda = () -> {
            System.out.println(this.name);              // "Outer"（this 就是 ThisDemo 实例）
            // System.out.println(ThisDemo.this.name);  // 同样是 "Outer"
        };
    }
}
```

### 8.2 变量遮蔽的编译错误

```java
void test() {
    int x = 10;
    Runnable anon = new Runnable() {
        public void run() { int x = 20; }    // ✅ 合法，新作用域
    };
    Runnable lambda = () -> {
        int x = 20;                          // ❌ 编译错误：已在方法中定义了变量 x
    };
}
```

> 🎯 **这是"Lambda 不是匿名内部类"在源码层面的直接证据**：Lambda 与外围方法**共享同一个变量作用域**，参数名和局部变量名都不能与外围重复。

### 8.3 字节码层面的解释

```text
captureThis() 的合成方法：
private java.lang.Integer lambda$captureThis$0();
  flags: (0x1002) ACC_PRIVATE, ACC_SYNTHETIC     ← 没有 ACC_STATIC
  Code:
     0: aload_0                                  ← aload_0 就是 this
     1: getfield  #7    // Field field:I
```

合成方法是**外围类的实例方法**，`aload_0` 加载的就是外围类的 `this`。运行时生成的实现类把捕获的外围对象作为字段存起来，调用时传给这个实例方法——**从头到尾没有第二个 `this` 的概念**。

---

## 9. 签名适配与桥接

### 9.1 泛型擦除带来的适配需求

```java
Function<String, Integer> f = Integer::parseInt;
```

擦除后接口方法是 `Object apply(Object)`，而实际方法是 `int parseInt(String)`。生成的实现类必须做三重适配：

```java
// 生成类的概念等价代码
final class Demo$$Lambda implements Function {
    @Override
    public Object apply(Object arg) {              // ← samMethodType：(Object)Object
        return Integer.valueOf(                     // ③ 装箱 int → Integer
                Integer.parseInt((String) arg));    // ① checkcast  ② 调用
    }
}
```

| 适配动作 | 触发条件 | 来源 |
|---------|---------|------|
| `checkcast` | 擦除类型 → 实际类型 | samMethodType vs instantiatedMethodType 参数差异 |
| 装箱 / 拆箱 | 基本类型 ↔ 包装类型 | implMethod vs instantiatedMethodType |
| 返回值适配 | 返回类型不同或需丢弃 | 同上 |
| 可变参数打包 | 引用 varargs 方法 | implMethod 是 varargs |

> ⚠️ **性能提示**：装箱适配是**真实开销**。这就是 `java.util.function` 提供 `IntFunction` / `IntPredicate` / `ToIntFunction` 等原始类型特化接口的原因——它们让 `instantiatedMethodType` 直接使用基本类型，消除装箱适配。

### 9.2 桥接方法（FLAG_BRIDGES）

当函数式接口**继承**自另一个泛型接口时，需要桥接方法：

```java
interface SFun<T, R> extends Function<T, R>, Serializable {}
SFun<String, Integer> f = String::length;
```

此时 flags 中带上 `FLAG_BRIDGES(4)`（实测 flags = 5 = 1 + 4），`LambdaMetafactory` 会在生成类中额外产出桥接方法，保证通过父接口 `Function` 调用时也能正确分派。

### 9.3 适配失败的运行时异常

| 异常 | 触发场景 |
|------|---------|
| `LambdaConversionException` | 链接期签名不兼容（通常是分离编译后接口变更） |
| `BootstrapMethodError` | BSM 执行抛异常，包装后抛出 |
| `NoSuchMethodError` | implMethod 在运行时不存在（版本不一致） |

> 💡 **典型踩坑场景**：A 模块编译时依赖接口 `v1`，运行时替换为 `v2` 且抽象方法签名改了 → 链接时 `LambdaConversionException`。**注意这个错误发生在首次执行到该 Lambda 时，而非类加载时**，所以可能在生产运行很久后才暴露。

---

## 10. 序列化与 SerializedLambda

### 10.1 Lambda 默认不可序列化

```java
Function<String, Integer> plain = String::length;
new ObjectOutputStream(out).writeObject(plain);
```

实测结果：

```text
非 Serializable Lambda 序列化 -> java.io.NotSerializableException
```

### 10.2 让 Lambda 可序列化的两种方式

```java
// 方式一：交叉转型（inline）
Runnable r = (Runnable & Serializable) () -> System.out.println("hi");

// 方式二：定义可序列化的函数式接口（推荐）
@FunctionalInterface
interface SFun<T, R> extends Function<T, R>, Serializable {}

SFun<String, Integer> f = String::length;
```

### 10.3 SerializedLambda：序列化的载体

可序列化 Lambda 的生成类中会包含 `writeReplace()` 方法，序列化时替换为 `SerializedLambda` 对象。

```java
SFun<String, Integer> f = String::length;
Method m = f.getClass().getDeclaredMethod("writeReplace");
m.setAccessible(true);
SerializedLambda sl = (SerializedLambda) m.invoke(f);
```

JDK 25 实测输出：

```text
capturingClass    = SerTest
functionalIfc     = SerTest$SFun
ifcMethodName     = apply
ifcMethodSig      = (Ljava/lang/Object;)Ljava/lang/Object;
implClass         = java/lang/String
implMethodName    = length
implMethodSig     = ()I
implMethodKind    = 5
capturedArgCount  = 0
序列化字节数       = 576
反序列化后调用     = 4
```

| 字段 | 含义 |
|------|------|
| `capturingClass` | 书写 Lambda 的类 |
| `functionalInterfaceClass` | 目标函数式接口 |
| `implClass` / `implMethodName` / `implMethodSig` | 真实实现方法的完整坐标 |
| `implMethodKind` | MethodHandle 种类（5 = invokeVirtual） |
| `capturedArgs` | 捕获的参数值（会被一并序列化） |

> 💡 **`SerializedLambda` 是"方法坐标 + 捕获值"的字符串描述**，反序列化时用这些信息**重新链接**——不是恢复一个对象，而是重新走一遍 metafactory 流程。

### 10.4 三大风险

**风险一：序列化体积大**

一个空捕获的方法引用序列化后 **576 字节**（实测），而一个普通 POJO 可能只需几十字节。原因是 `SerializedLambda` 要携带完整的类名、方法名、签名字符串。

**风险二：版本脆弱性**

| 变更 | 后果 |
|------|------|
| 重命名被引用的方法 | 反序列化失败 |
| 修改方法签名 | 反序列化失败 |
| **调整同一方法内 Lambda 的顺序** | `lambda$m$0` → `lambda$m$1`，**反序列化失败** |
| 编译器版本变化导致合成方法名不同 | 可能失败 |

> ⚠️ **第三条最阴险**：仅仅在方法里**多加一个 Lambda**或**调换两个 Lambda 的位置**，合成方法名的序号就会变，此前序列化的数据全部失效。这是纯粹的实现细节泄漏到持久化格式。

**风险三：反序列化安全**

`SerializedLambda` 在反序列化时会**触发方法链接与调用能力构造**。反序列化不可信数据时，攻击者可能构造指向危险方法的 `SerializedLambda`。

> ⚠️ **安全准则**：**永远不要反序列化来自不可信来源的 Lambda**。JDK 内部对 `SerializedLambda` 的 `readResolve` 有 capturingClass 校验（要求由捕获类的 `$deserializeLambda$` 方法处理），但这不构成对不可信输入的完整防护。Java 原生序列化本身在处理不可信数据时就是高危操作。

### 10.5 实用场景与替代方案

**唯一广泛使用的正当场景**：MyBatis-Plus / JPA 等框架用可序列化 Lambda 实现**类型安全的字段名提取**。

```java
// MyBatis-Plus 的 LambdaQueryWrapper
wrapper.eq(User::getName, "张三");
// 框架内部通过 SerializedLambda 拿到 implMethodName = "getName"
// 再转换为数据库列名 "name"
```

这个场景之所以安全：**只在本地内存中提取方法名，从不跨进程传输，也不反序列化外部数据**。

| 场景 | 是否推荐用可序列化 Lambda |
|------|:---:|
| 框架内提取字段名（本地） | ✅ 推荐 |
| 跨 JVM 传输行为（如分布式任务） | ❌ 用命名类 + 参数 |
| 持久化到数据库/缓存 | ❌ 版本脆弱，绝对避免 |
| RPC 传参 | ❌ 安全风险 |

> 🎯 **需要跨进程传递"行为"时的正确做法**：传递**标识符 + 参数**（如策略名 `"DISCOUNT_VIP"` + 参数），接收方按标识符查找本地实现。永远不要传递序列化的函数对象。

---

## 11. 性能特征与 JIT

### 11.1 三种写法的性能对比

| 维度 | 匿名内部类 | Lambda（非捕获） | Lambda（捕获） | 方法引用 |
|------|:---:|:---:|:---:|:---:|
| 编译产物 | 独立 class 文件 | 合成方法 | 合成方法 | 通常无额外产物 |
| 类加载时机 | 首次使用 | 首次链接 | 首次链接 | 首次链接 |
| 实例分配 | **每次 new** | 首次后复用 | 每次求值 | 视绑定与否 |
| 首次调用开销 | 类加载 | **链接（较高）** | 链接（较高） | 链接（较高） |
| 稳态性能 | 基准 | **持平** | 持平 | 持平 |

> 🎯 **稳态下四者性能基本一致**——JIT 内联后，Lambda 的转发层被完全消除。差异主要在**启动阶段**和**是否分配实例**。

### 11.2 JIT 为何能优化得这么好

| 优化 | 生效原因 |
|------|---------|
| **激进内联** | `ConstantCallSite` 的 target 不可变，可当常量处理 |
| **去虚化** | 单一实现类，接口调用可去虚化为直接调用 |
| **标量替换** | 捕获型 Lambda 若不逃逸，实例分配被消除 |
| **锁消除** | 不逃逸对象上的同步被移除 |

```text
优化链路示例：
list.stream().map(String::trim).count()
     │
     ├─ ① 接口调用 Function.apply → 去虚化（只有一个实现类）
     ├─ ② 内联生成类的 apply → 内联 String.trim
     ├─ ③ Lambda 实例不逃逸 → 标量替换消除分配
     └─ ④ 最终机器码 ≈ 手写循环调用 trim()
```

> ⚠️ **去虚化失效的情况**：如果同一个调用点被多种 Lambda 实现"污染"（megamorphic call site），JIT 无法去虚化，性能会下降。典型场景是**通用工具方法接收各种不同的 Lambda**：

```java
// 这个 apply 调用点会被大量不同实现类污染
static <T, R> R helper(T t, Function<T, R> f) { return f.apply(t); }
```

### 11.3 启动开销：第一个 Lambda 最贵

| 阶段 | 开销 |
|------|------|
| 加载 `java.lang.invoke` 基础设施 | **仅第一次，数十毫秒量级** |
| 每个调用点首次链接 | 生成类 + 定义类，微秒~毫秒量级 |
| 后续执行 | 接近零 |

从实测的类加载日志可见，`LambdaMetafactory`、`LambdaForm` 等在第一个 Lambda 链接时集中加载。

**对启动敏感场景的缓解手段**：

| 手段 | 说明 | 适用 |
|------|------|------|
| AppCDS | 归档已加载类 | 通用 |
| **Lambda 预链接归档** | CDS 归档中包含 Lambda 代理类（JDK 15+ 动态归档支持） | Spring Boot 等 |
| AOT / GraalVM Native Image | 编译期完成链接 | 极致启动 |
| 减少启动路径 Lambda | 冷启动关键路径改用普通方法 | 针对性优化 |

> 💡 Spring Boot 3 的 CDS 支持与 GraalVM Native Image 支持，很大程度上就是在解决包括 Lambda 链接在内的启动开销。

### 11.4 真正需要关注的性能点

| 关注点 | 影响 | 建议 |
|-------|------|------|
| **装箱** | 高频循环中显著 | 用 `IntPredicate` 等特化接口 |
| **捕获导致的分配** | 高频调用路径 | 尽量写成非捕获 |
| **调用点污染** | 去虚化失效 | 避免过度通用的高频工具方法 |
| **Stream 开销** | 小集合上 Stream 慢于 for | 小数据量用普通循环 |

```java
// ❌ 每次调用都装箱
Function<Integer, Integer> f = x -> x * 2;
for (int i = 0; i < 1_000_000; i++) f.apply(i);   // 200 万次装箱/拆箱

// ✅ 原始类型特化，零装箱
IntUnaryOperator g = x -> x * 2;
for (int i = 0; i < 1_000_000; i++) g.applyAsInt(i);
```

> ⚠️ **不要过早优化**：上述差异在 JIT 标量替换生效后往往也会消失。**先用 JMH 测量再优化**，不要凭直觉改写代码降低可读性。

---

## 12. javap 实验手册

### 12.1 命令速查

| 命令 | 用途 |
|------|------|
| `javap -c Xxx.class` | 反汇编方法字节码 |
| `javap -p -c Xxx.class` | **加上 private/synthetic 成员**（看 `lambda$` 必需） |
| `javap -v -p Xxx.class` | 完整信息：常量池 + BootstrapMethods |
| `javap -v -p X.class \| sed -n '/^BootstrapMethods/,$p'` | 只看 BSM 段（最常用） |
| `javap -s -p Xxx.class` | 显示内部类型签名 |

> ⚠️ **`-p` 不能省**：Lambda 合成方法是 `ACC_PRIVATE ACC_SYNTHETIC`，不加 `-p` 完全看不到。这是初学者最常见的困惑来源。

### 12.2 实验一：捕获分析三步法

```bash
javac LambdaDemo.java

# 步骤 1：看调用点签名 → 判断捕获了什么
javap -c -p LambdaDemo.class | grep -A1 invokedynamic

# 步骤 2：看合成方法修饰 → 判断是否捕获 this
javap -p LambdaDemo.class | grep 'lambda\$'

# 步骤 3：看 BSM → 确认 implMethod 与适配签名
javap -v -p LambdaDemo.class | sed -n '/^BootstrapMethods/,$p'
```

**判读口诀**：

```text
调用点签名  ()               → 非捕获
调用点签名  (LString;)       → 捕获局部变量
调用点签名  (LOuter;)        → 捕获 this
合成方法带 static            → 未捕获 this
合成方法不带 static          → 捕获了 this
无 lambda$ 合成方法          → 方法引用（数组引用除外）
```

### 12.3 实验二：验证实例复用行为

```java
// IdentityTest.java
import java.util.function.*;

public class IdentityTest {
    static Supplier<String> nonCapturing()      { return () -> "x"; }
    static Supplier<String> capturing(String s) { return () -> s; }
    static Runnable siteA() { return () -> {}; }
    static Runnable siteB() { return () -> {}; }

    public static void main(String[] args) {
        Supplier<String> a1 = nonCapturing(), a2 = nonCapturing();
        System.out.println("非捕获: same=" + (a1 == a2));
        System.out.println("  class=" + a1.getClass().getName());
        System.out.println("  isHidden=" + a1.getClass().isHidden());

        Supplier<String> b1 = capturing("p"), b2 = capturing("p");
        System.out.println("捕获: same=" + (b1 == b2));

        System.out.println("不同调用点 sameClass="
                + (siteA().getClass() == siteB().getClass()));
    }
}
```

```bash
javac IdentityTest.java && java IdentityTest
```

> 💡 **换不同 JDK 版本跑一遍**，观察类名格式差异（`$$Lambda$1/0x...` vs `$$Lambda/0x...`），亲手确认"不能依赖类名格式"这条结论。

### 12.4 实验三：对比匿名内部类

```bash
javac AnonVsLambda.java && ls *.class
# 预期：AnonVsLambda$1.class（匿名类）+ AnonVsLambda.class
#       Lambda 没有独立文件

javap -c -p AnonVsLambda\$1.class     # 匿名类是一个完整的类
javap -c -p AnonVsLambda.class        # Lambda 是 invokedynamic + 合成方法
```

### 12.5 实验四：观察序列化 Lambda 的 altMetafactory

```bash
javac SerTest.java
javap -v -p SerTest.class | sed -n '/^BootstrapMethods/,$p' | head -10
# 预期看到 altMetafactory 与额外的 flags 参数（如 5）
```

### 12.6 实验五：类加载观测

```bash
# 看 Lambda 基础设施何时加载
java -Xlog:class+load=info IdentityTest 2>&1 | grep -i lambda | head

# 看隐藏类定义（JDK 15+）
java -Xlog:class+load=info IdentityTest 2>&1 | grep '\$\$Lambda'
```

> ⚠️ **不要用 `-Djdk.internal.lambda.dumpProxyClasses`**：JDK 25 实测该属性已不产出文件。老教程中的这条命令已过时。

---

## 13. 常见误解澄清

### 13.1 误解对照表

| # | 误解 | 事实 |
|:-:|------|------|
| 1 | Lambda 就是匿名内部类的语法糖 | 编译产物、`this` 语义、作用域规则、实例化机制**全都不同** |
| 2 | Lambda 永远不生成实现类 | **运行期**由 `LambdaMetafactory` 生成隐藏类；只是**编译期**不生成文件 |
| 3 | Lambda 实例一定被缓存复用 | 仅**非捕获**型在**当前实现**中复用；规范**不保证** |
| 4 | 捕获值相同的 Lambda 会共享实例 | 不会，实测 `same=false` |
| 5 | 相同代码的 Lambda 共享实现类 | 不会，**按调用点**生成，实测 `sameClass=false` |
| 6 | Lambda 中 `this` 指向 Lambda 自己 | 指向**外围类实例** |
| 7 | invokedynamic 每次调用都很慢 | **只有首次链接**有开销，之后与普通调用相当 |
| 8 | 方法引用一定比 Lambda 快 | 稳态下基本无差异 |
| 9 | 可以用 `dumpProxyClasses` 导出 Lambda 类 | JDK 25 实测已失效 |
| 10 | Lambda 天然可序列化 | 默认**不可**序列化，需显式声明 |
| 11 | effectively final 是编译器偷懒 | 是**值捕获**模型的必然要求 |
| 12 | Lambda 比匿名类省内存 | 非捕获型省；捕获型同样每次分配 |

### 13.2 面试标准答法模板

**问："Lambda 底层是怎么实现的？"**

> Lambda 在编译期**不会**生成独立的 class 文件。javac 把 Lambda 体抽取为当前类的一个私有合成方法 `lambda$方法名$序号`，并在原位置生成一条 `invokedynamic` 指令，同时在 class 文件的 `BootstrapMethods` 属性中记录引导方法 `LambdaMetafactory.metafactory` 及三个关键参数：擦除后的接口签名、实现方法句柄、实例化后的签名。
>
> 运行期首次执行到这条指令时，JVM 调用引导方法，由 `LambdaMetafactory` 动态生成一个实现目标函数式接口的类——Java 15 之后是隐藏类——并返回一个 `ConstantCallSite`。调用点被永久绑定后，后续执行不再有链接开销。
>
> 是否捕获外部变量决定了两件事：捕获的变量会成为生成类的 final 字段，因此**捕获型每次求值都要新建实例**；**非捕获型在当前实现中会复用同一实例**，但要强调这是 HotSpot 的实现行为，`LambdaMetafactory` 的文档明确不保证实例同一性，所以代码里不能依赖。
>
> 这套设计相比"编译成匿名内部类"的最大价值是：**翻译策略从字节码中解耦出来，放到了 JDK 库里**，所以 JDK 能在不重新编译用户代码的前提下持续改进 Lambda 的实现——从 Java 8 的 VM 匿名类演进到 Java 15 的隐藏类就是实证。

**追问："那 `this` 为什么指向外围类？"**

> 因为 Lambda **不引入新的作用域**，它是词法作用域的。字节码上能直接验证：捕获实例状态的 Lambda，其合成方法是外围类的**实例方法**（没有 `ACC_STATIC`），方法体里 `aload_0` 加载的就是外围类的 `this`。从头到尾不存在"Lambda 自己的 this"这个概念。作用域共享还有一个可观察的推论——Lambda 内不能声明与外围方法同名的局部变量，会编译报错，而匿名内部类可以。

---

## 版本与参考

| 项 | 说明 |
|----|------|
| 实测环境 | JDK 25.0.2 LTS（`javac 25.0.2`，HotSpot 64-Bit Server VM） |
| 关键版本节点 | Java 7 引入 `invokedynamic`；Java 8 用于 Lambda；Java 15 迁移隐藏类（JEP 371） |
| 核心 API | `java.lang.invoke.LambdaMetafactory` / `CallSite` / `MethodHandle` / `MethodType` / `SerializedLambda` |
| 规范文档 | JVMS §6.5 invokedynamic；§4.7.23 BootstrapMethods；JLS §15.27 |
| 结论适用范围 | 概念结论（indy 机制、捕获模型、this 语义）在 Java 8–25 通用；**具体类名格式与工具行为以 JDK 25 实测为准，跨版本请自行验证** |

---

**上一模块**：[03-Lambda方法引用深度解析](./03-Lambda方法引用深度解析.md)
**下一模块**：[05-Lambda实战模式与Stream协同](./05-Lambda实战模式与Stream协同.md)
**返回总览**：[00-Lambda表达式知识体系总览](./00-Lambda表达式知识体系总览.md)
