# 01 Lambda 语法精讲与变量捕获

> Lambda 不是"省略了 new 的匿名类"，而是一段**没有独立类型、必须由上下文赋予类型**的代码——理解目标类型与捕获规则，才能解释它所有的"奇怪"编译错误

---

## 📚 目录

1. [从匿名内部类到 Lambda](#1-从匿名内部类到-lambda)
2. [完整语法与简化规则](#2-完整语法与简化规则)
3. [目标类型与重载决议](#3-目标类型与重载决议)
4. [类型推断规则](#4-类型推断规则)
5. [变量捕获与 effectively final](#5-变量捕获与-effectively-final)
6. [this super 与作用域](#6-this-super-与作用域)
7. [返回值兼容性与受检异常](#7-返回值兼容性与受检异常)
8. [常见误区速查](#8-常见误区速查)
9. [动手练习](#9-动手练习)

---

## 1. 从匿名内部类到 Lambda

### 1.1 问题的起点：样板代码淹没意图

Java 8 之前，"把一段行为传给方法"只能靠匿名内部类。真正有意义的代码只有一行，外面却裹了五行仪式：

```java
// Java 7 写法：6 行代码，只有第 4 行是业务意图
List<String> names = new ArrayList<>(List.of("Charlie", "alice", "Bob"));
Collections.sort(names, new Comparator<String>() {
    @Override
    public int compare(String a, String b) {
        return a.compareToIgnoreCase(b);   // ← 唯一的"信息"
    }
});
```

这里的 `new Comparator<String>() { ... }`、`@Override`、`public int compare(String a, String b)` 全是**类型系统要求的仪式**，而不是程序员想表达的东西。行为本身其实只是"给我两个 String，返回它们忽略大小写的比较结果"。

### 1.2 三段式演进

```java
List<String> names = new ArrayList<>(List.of("Charlie", "alice", "Bob"));

// 阶段一：匿名内部类——显式类型 + 显式方法签名
names.sort(new Comparator<String>() {
    @Override
    public int compare(String a, String b) { return a.compareToIgnoreCase(b); }
});

// 阶段二：Lambda——只保留"参数 → 结果"
names.sort((a, b) -> a.compareToIgnoreCase(b));

// 阶段三：方法引用——连参数名都不需要了
names.sort(String::compareToIgnoreCase);

System.out.println(names);   // [alice, Bob, Charlie]
```

| 维度 | 匿名内部类 | Lambda | 方法引用 |
|------|-----------|--------|---------|
| 语法噪音 | 高（类型 + 方法签名 + 大括号） | 低（参数 + 箭头） | 最低（`::`） |
| 是否有独立类型 | 有（`Outer$1`） | **没有**，靠目标类型赋予 | 没有 |
| 编译产物 | 独立 `.class` 文件 | `invokedynamic` 指令 | `invokedynamic` 指令 |
| `this` 指向 | 匿名类实例自身 | **外围类实例** | 不适用 |
| 能否遮蔽外层局部变量 | 能（独立作用域） | **不能**（共享外层作用域） | 不适用 |
| 可实现抽象方法数 | 任意多个 | 恰好 1 个 | 恰好 1 个 |
| 可否有实例字段 | 可以（带状态） | 不可以（无状态） | 不可以 |

> 🎯 **核心要点**：Lambda 与匿名内部类**语法目的相似，语义并不等价**。表格里 `this`、作用域、编译产物这三行的差异，是后面所有"为什么这里报错"的根源。

### 1.3 一个关键认知：Lambda 没有自己的类型

下面这行代码**无法编译**，而且错误信息很能说明问题：

```java
// var 无法推断 Lambda 的类型
var f = (String s) -> s.length();
// 错误：lambda expression needs an explicit target-type
```

`(String s) -> s.length()` 这段代码**本身不是任何类型**。它可以是 `Function<String,Integer>`，也可以是 `ToIntFunction<String>`，还可以是你自己写的 `interface Len { int of(String s); }`。编译器必须从**上下文**（赋值目标、方法参数、返回位置、强制转换）里拿到一个函数式接口，才能决定它到底是什么。这个上下文类型就叫**目标类型（target type）**，是第 3 节的主题。

```java
// 同一段 Lambda，三个不同的类型
Function<String, Integer>  f1 = s -> s.length();
ToIntFunction<String>      f2 = s -> s.length();
Comparator<String>         f3 = (a, b) -> a.length() - b.length();

// 想让它"就地"有类型，必须显式强转
Object o = (Runnable) () -> System.out.println("cast 赋予了类型");
```

> ⚠️ 这也解释了为什么 `Object o = () -> {};` 编译失败——`Object` 不是函数式接口，无法充当目标类型。

---

## 2. 完整语法与简化规则

### 2.1 语法骨架

```java
(参数列表) -> 函数体
```

箭头 `->` 左边是**形参**，右边是**表达式或语句块**。仅此而已，其余全是简写规则。

### 2.2 参数部分的三种写法

| 写法 | 示例 | 说明 |
|------|------|------|
| 显式类型 | `(String s, int n) -> ...` | 类型全写，最啰嗦但最明确 |
| 隐式类型（推断） | `(s, n) -> ...` | 从目标类型推断，最常用 |
| `var` 参数（Java 11+） | `(var s, var n) -> ...` | 为了能加注解/修饰符 |

三条硬性约束：

```java
// 1. 无参数时括号不可省
Runnable r = () -> System.out.println("必须写空括号");

// 2. 单个"隐式类型"参数才可以省括号
Function<String, Integer> ok1 = s -> s.length();          // ✅
Function<String, Integer> ok2 = (s) -> s.length();        // ✅
Function<String, Integer> ok3 = (String s) -> s.length(); // ✅ 显式类型必须带括号
// Function<String,Integer> bad = String s -> s.length(); // ❌ 编译错误

// 3. 类型标注必须"全有或全无"，不能混用
BiFunction<String, String, String> okAll   = (String a, String b) -> a + b;   // ✅
BiFunction<String, String, String> okNone  = (a, b) -> a + b;                 // ✅
// BiFunction<String,String,String> bad2 = (String a, b) -> a + b;            // ❌ 不能一半一半
```

> 💡 `var` 参数存在的唯一理由是**给参数加注解或 `final`**。`(@NonNull var s) -> ...` 是合法的，而 `(@NonNull s) -> ...` 不是。同样，`var` 也遵守"全有或全无"：`(var a, String b)` 非法。

### 2.3 函数体部分：表达式体 vs 语句块体

```java
// 表达式体：单个表达式，值即返回值，不写 return，不写分号
Function<Integer, Integer> square = x -> x * x;

// 语句块体：花括号包裹，有返回值就必须显式 return，每条语句带分号
Function<Integer, String> classify = x -> {
    if (x < 0)  return "负数";
    if (x == 0) return "零";
    return "正数";
};
```

| 特征 | 表达式体 | 语句块体 |
|------|---------|---------|
| 花括号 | 无 | 必须 |
| `return` | 禁止写 | 有返回值时必须写 |
| 语句分号 | 无 | 每条都要 |
| 多条语句 | 不支持 | 支持 |
| 可读性 | 短逻辑最佳 | 超过 3 行建议抽方法 |

一个容易踩的坑是"半简化"：

```java
// ❌ 有花括号就不能省 return
// Function<Integer,Integer> bad = x -> { x * x; };

// ❌ 没花括号就不能写 return
// Function<Integer,Integer> bad2 = x -> return x * x;

// ✅ 两种正确写法
Function<Integer, Integer> good1 = x -> x * x;
Function<Integer, Integer> good2 = x -> { return x * x; };
```

### 2.4 简化流程图

```text
起点：(String s) -> { return s.trim().toLowerCase(); }
  │
  ├─ 省略参数类型（目标类型可推断）
  │     (s) -> { return s.trim().toLowerCase(); }
  │
  ├─ 单个隐式参数，省略括号
  │     s -> { return s.trim().toLowerCase(); }
  │
  └─ 函数体只有一条 return，省略 {} 和 return
        s -> s.trim().toLowerCase()          ← 终点
```

> 🎯 **核心要点**：简化不是"随便删"，而是四条独立规则的叠加——**类型可推断则省类型；单隐式参数则省括号；单表达式则省花括号；省花括号则必须同时省 `return`**。

---

## 3. 目标类型与重载决议

### 3.1 什么样的上下文能提供目标类型

Lambda 只能出现在能提供目标类型的位置，这些位置在规范里叫 **poly expression context**：

| 上下文 | 示例 |
|--------|------|
| 变量赋值 | `Runnable r = () -> {};` |
| 方法实参 | `list.forEach(x -> print(x));` |
| 返回语句 | `return () -> 42;`（方法返回类型为函数式接口） |
| 强制类型转换 | `(Runnable) () -> {}` |
| 数组初始化 | `Runnable[] rs = { () -> {}, () -> {} };` |
| 三元表达式分支 | `flag ? (Runnable) a : b`（整体需有目标类型） |
| Lambda 体内的嵌套 | `() -> (Supplier<String>) () -> "x"` |

### 3.2 重载决议：同一段 Lambda 选中不同方法

当重载方法的参数是不同函数式接口时，编译器靠 **Lambda 的形状（参数个数 + 是否有返回值）** 来挑：

```java
import java.util.concurrent.Callable;

public class OverloadDemo {
    static void exec(Runnable r)          { System.out.println("选中 Runnable"); }
    static void exec(Callable<String> c)  { System.out.println("选中 Callable"); }

    public static void main(String[] args) {
        exec(() -> System.out.println("hi"));   // 输出：选中 Runnable —— 函数体是 void 调用
        exec(() -> "abc");                      // 输出：选中 Callable —— 函数体产生值
    }
}
```

（以上输出经 JDK 25 实际编译运行验证。）

判定依据：

| Lambda 形状 | 匹配的接口 | 理由 |
|------------|-----------|------|
| `() -> voidMethod()` | `Runnable` | 表达式无值，只能 void 兼容 |
| `() -> "abc"` | `Callable<String>` | 表达式有值且非语句，void 不兼容 |
| `() -> list.add(x)` | 两者形状都合法 → **看返回类型是否匹配** | `add` 返回 `boolean`，既能当值也能当语句 |

第三行是需要小心的地方，但结论比"一定歧义"更微妙——**还要看泛型实参能否对上**。以下三组均为 JDK 25 实测结果：

```java
List<String> list = new ArrayList<>();

// 情形 A：Callable<String> —— boolean 对不上 String，只有 Runnable 可行
static void exec2(Runnable r)         { System.out.println("2:Runnable"); }
static void exec2(Callable<String> c) { System.out.println("2:Callable<String>"); }
exec2(() -> list.add("x"));           // 输出：2:Runnable（无歧义）

// 情形 B：Callable<Boolean> —— 两者都可行，编译器选中"更精确"的 Callable
static void exec(Runnable r)          { System.out.println("Runnable"); }
static void exec(Callable<Boolean> c) { System.out.println("Callable<Boolean>"); }
exec(() -> list.add("x"));            // 输出：Callable<Boolean>（不报歧义！）

// 情形 C：改成语句块，值被显式丢弃 → 只能是 Runnable
exec(() -> { list.add("x"); });       // 输出：Runnable
```

情形 B 值得记住：**编译器并不会摆手说"我搞不清"，而是按 JLS §15.12.2.5 判定"有返回值的接口比 void 接口更精确"，静默选中 `Callable`**。这比报错更危险——行为悄悄变了。`ExecutorService.submit(() -> list.add("y"))` 就是活例子，它选中的是 `submit(Callable<Boolean>)`，`future.get()` 拿到 `true` 而不是 `null`（实测确认）。

真正会报 `对 xxx 的引用不明确` 的，是**两个接口的形状与返回类型都能匹配**的时候：

```java
// ❌ 实测报错：Supplier<String> 和 Callable<String> 都匹配
static void run(Supplier<String> s) { }
static void run(Callable<String> c) { }
// run(() -> "abc");

// ❌ 实测报错：Predicate<String> 和 Function<String,Boolean> 都匹配
static void p(Predicate<String> f) { }
static void p(Function<String, Boolean> f) { }
// p(s -> s.isEmpty());

// ✅ 统一解法：强制类型转换指明目标类型
run((Supplier<String>) () -> "abc");
```

> ⚠️ **设计建议**：写 API 时**不要用不同函数式接口重载同名方法**——这是 Effective Java Item 44 的明确告诫。理由正是上面这组实验：有时报歧义（编译期就拦住你），更糟的情况是**不报歧义但选了你没想到的那个重载**。宁可起两个名字（`execute` / `submitTask`）。
>
> 顺带一个相关陷阱：`void h(Function<String,Integer> f)` 与 `void h(Function<Integer,Integer> f)` **根本无法共存**，编译器直接报"名称冲突：具有相同疑符（erasure）"——泛型擦除后两者签名一致。想重载必须换用不同的接口类型（如 `Function` 与 `ToIntFunction`），但那又会掉进上面的歧义坑。

### 3.3 泛型方法 + Lambda 的推断链

Lambda 参与泛型推断时，类型信息是**双向流动**的：

```java
// map 的签名：<R> Stream<R> map(Function<? super T, ? extends R> mapper)
List<String> words = List.of("java", "lambda");

// T = String 由 stream 确定 → s 推断为 String
// s.length() 返回 int → 装箱 → R 推断为 Integer
List<Integer> lengths = words.stream()
        .map(s -> s.length())
        .collect(Collectors.toList());
```

当推断链断裂时，需要手动给出**显式类型参数**或**显式参数类型**：

```java
// 空集合 + 链式调用，R 无从推断
// ❌ List<String> r = Collections.<String>emptyList().stream().map(x -> x).collect(...);

// ✅ 显式方法类型参数
List<Object> r1 = Stream.<Object>of("a", 1).collect(Collectors.toList());

// ✅ 给 Lambda 参数写上类型，帮助编译器
Function<String, Integer> f = (String s) -> s.length();
```

---

## 4. 类型推断规则

### 4.1 推断从目标类型倒推形参

编译器的工作顺序是"**先定接口，再定方法，最后定参数类型**"：

```text
Comparator<String> c = (a, b) -> a.compareTo(b);

步骤 1：目标类型 = Comparator<String>
步骤 2：唯一抽象方法 = int compare(String o1, String o2)
步骤 3：形参对位 → a : String, b : String
步骤 4：校验函数体 → a.compareTo(b) 返回 int，与 compare 返回类型一致 ✅
```

因此**同一段 Lambda 文本，在不同目标类型下参数类型完全不同**：

```java
Comparator<String>  byStr = (a, b) -> a.compareTo(b);   // a, b 是 String
Comparator<Integer> byInt = (a, b) -> a.compareTo(b);   // a, b 是 Integer
```

### 4.2 推断失败的四类典型场景

| 场景 | 错误代码 | 原因 | 修法 |
|------|---------|------|------|
| `var` 声明 | `var f = x -> x;` | 无目标类型 | 写出接口类型 |
| 赋给 `Object` | `Object o = () -> {};` | 非函数式接口 | 强转 `(Runnable)` |
| 重载歧义 | `run(() -> "abc")` | 多个接口形状与返回类型都匹配 | 强转指定接口 |
| 泛型断链 | 空流 / 深层嵌套 | 无从确定 `R` | 显式类型参数 |

### 4.3 推断与重载解析的交互顺序

一个反直觉的点：**重载解析只看 Lambda 的"形状"，不看函数体内部能否编译通过**。所谓形状，指的是**参数个数**与**是否 void 兼容**。

```java
static void h(Function<String, Integer> f) { }
static void h(ToIntFunction<String> f)     { }

// ❌ 实测报错："对 h 的引用不明确"，而不是任何关于函数体的抱怨
// h(s -> s.length());
```

这里两个候选的形状完全相同（1 个参数、有返回值），编译器无从选择，直接判歧义——**它不会"试着把函数体套进每个候选、看哪个能过"**。

反过来，当形状能区分候选时，函数体的错误才会被报出来：

```java
static void exec2(Runnable r)         { }
static void exec2(Callable<String> c) { }

// 形状先筛掉 Callable<String>（boolean 对不上 String），锁定 Runnable
exec2(() -> list.add("x"));           // ✅ 通过
```

> 💡 **排错口诀**：看到 `引用不明确 / ambiguous`，去数**参数个数与返回值有无**，别盯着函数体改；看到 `不兼容的类型 / incompatible types`，才该检查函数体的返回值和类型。两类错误的诊断路径完全不同。
>
> 还有第三类容易混淆的：`名称冲突…具有相同疑符`。它跟 Lambda 无关，是**方法声明本身**过不了泛型擦除检查（如 `Function<String,Integer>` 与 `Function<Integer,Integer>` 同名重载），报错位置在方法定义行而不是调用行。

---

## 5. 变量捕获与 effectively final

### 5.1 三类变量，三种待遇

```java
public class CaptureDemo {
    private int instanceField = 1;          // 实例字段
    private static int staticField = 2;     // 静态字段

    public void demo() {
        int localVar = 3;                   // 局部变量

        Runnable r = () -> {
            instanceField++;                // ✅ 可读可写
            staticField++;                  // ✅ 可读可写
            System.out.println(localVar);   // ✅ 可读
            // localVar++;                  // ❌ 编译错误
        };
        r.run();
        System.out.println(instanceField + " " + staticField);  // 2 3
    }
}
```

| 变量类型 | 存储位置 | Lambda 能否读 | Lambda 能否写 | 约束 |
|---------|---------|:---:|:---:|------|
| 局部变量 / 形参 | 虚拟机栈 | ✅ | ❌ | 必须 effectively final |
| 实例字段 | 堆（对象内） | ✅ | ✅ | 隐式捕获 `this` |
| 静态字段 | 堆（Class 对象） | ✅ | ✅ | 无捕获，直接访问 |
| 数组元素 / 对象内部状态 | 堆 | ✅ | ✅ | 引用本身需 effectively final |

关键区别在于**捕获的是什么**：局部变量捕获的是**值的拷贝**（栈帧会消失，必须复制走）；实例字段访问的其实是**捕获了 `this` 引用**，然后通过引用读写堆上的对象。

### 5.2 effectively final 的准确定义

> **effectively final**：变量没有被声明为 `final`，但**初始化之后再没有被赋值过**，因此"加上 `final` 也能编译通过"。

这不是"值不变"，而是"**引用/变量槽不再被重新赋值**"。区分这两者是理解下一小节的关键。

实测的编译错误信息（JDK 25）：

```java
int count = 0;
Runnable r = () -> System.out.println(count);
count = 1;
// 错误: 从lambda 表达式引用的本地变量必须是最终变量或实际上的最终变量
```

注意**赋值发生在 Lambda 之后也一样报错**——编译器检查的是变量在整个作用域内是否只赋值一次，与代码位置无关。

### 5.3 边界案例：什么算"只赋值一次"

```java
// ✅ 分支赋值：每条路径只赋值一次，仍是 effectively final
int a;
if (args.length == 0) { a = 1; } else { a = 2; }
Supplier<Integer> s1 = () -> a;        // 合法，实测通过

// ✅ 增强 for：循环变量每次迭代都是"新变量"
List<Supplier<String>> subs = new ArrayList<>();
for (String x : List.of("A", "B")) {
    subs.add(() -> x);                 // 合法
}
subs.forEach(su -> System.out.print(su.get() + " "));   // 输出：A B

// ❌ 传统 for：i 在同一变量槽上反复自增，不是 effectively final
for (int i = 0; i < 2; i++) {
    // subs.add(() -> i);              // 编译错误
    int copy = i;                      // ✅ 每轮新建局部变量
    nums.add(() -> copy);
}
```

> 🎯 **核心要点**：增强 for 的循环变量在**每次迭代都是一个全新的变量**，所以可以捕获；传统 for 的 `i` 是**同一个变量被反复修改**，所以不行。这个差异经常在面试里被问到。

### 5.4 "绕过"限制的两种写法及其代价

因为限制的是**变量槽**而不是**堆上的值**，所以可以用可变容器"逃逸"：

```java
// 写法一：数组技巧
int[] counter = {0};
Runnable r = () -> counter[0]++;       // ✅ counter 引用没变，改的是堆上数组元素
r.run(); r.run();
System.out.println(counter[0]);        // 2

// 写法二：可变对象
StringBuilder sb = new StringBuilder("start");
Runnable r2 = () -> sb.append("-more");
r2.run();
System.out.println(sb);                // start-more
```

> ⚠️ **能做不等于该做**。这两种写法**绕过了限制，但没有绕过限制想防的问题**：如果 Lambda 被提交到线程池或并行流，`counter[0]++` 就是彻头彻尾的数据竞争。语言帮你挡住了栈变量，挡不住堆上的共享可变状态。
>
> 需要并发计数请用 `AtomicInteger`/`LongAdder`；需要在流里累积请用 `Collectors.counting()`、`reduce`、`Collectors.toList()` 这类**规约**操作，而不是在 `forEach` 里往外部集合塞数据。

### 5.5 为什么语言要这么设计

| 角度 | 解释 |
|------|------|
| 生命周期 | 方法返回后栈帧销毁，Lambda 可能还活着，只能复制值 |
| 实现复杂度 | 若允许修改，需把变量提升到堆（Java 选择了不做） |
| 并发语义 | 避免"看起来改了外层变量，实际改的是副本"这类幽灵 bug |
| 一致性 | 与匿名内部类的既有规则保持一致（Java 8 前要求显式 `final`） |

Java 8 做的实际改动很小：把"**必须写 `final`**"放宽成"**行为上是 final 就行**"，省掉了满屏的 `final` 关键字，语义没变。

---

## 6. this super 与作用域

### 6.1 this 的指向差异（实测验证）

```java
public class ThisDemo {
    private String name = "Outer";

    void test() {
        Runnable lambda = () -> System.out.println("lambda this = " + this.name);

        Runnable anon = new Runnable() {
            @Override public void run() {
                // 这里的 this 是匿名类实例，访问不到 name，需要 ThisDemo.this.name
                System.out.println("anon this  = " + ThisDemo.this.name);
            }
        };

        lambda.run();   // lambda this = Outer
        anon.run();     // anon this  = Outer（必须显式限定才拿得到）
    }

    public static void main(String[] args) { new ThisDemo().test(); }
}
```

| 表达式 | 在 Lambda 中 | 在匿名内部类中 |
|--------|-------------|---------------|
| `this` | 外围类实例 | 匿名类实例 |
| `Outer.this` | 外围类实例（同 `this`） | 外围类实例 |
| `super.m()` | 外围类的父类方法 | 匿名类实现的接口/父类方法 |
| `this.getClass()` | 外围类的 Class | 匿名类的 Class（`getSimpleName()` 返回空串） |

> 💡 实测细节：匿名内部类的 `getClass().getSimpleName()` 返回的是**空字符串**（匿名类没有简单名），`getName()` 才会给出 `ThisDemo$1`。用它做日志时容易踩坑。

### 6.2 作用域：Lambda 不引入新的变量作用域

这是比 `this` 更容易被忽略、也更常撞见的差异。

```java
int x = 1;

// ❌ Lambda 与外层共享作用域，不能定义同名变量
// Consumer<Integer> c = n -> { int x = 5; System.out.println(x); };
// 错误: 已在方法 main(String[])中定义了变量 x

// ✅ 匿名内部类有独立作用域，可以遮蔽
Runnable r = new Runnable() {
    @Override public void run() {
        int x = 5;                                  // 合法，遮蔽外层
        System.out.println("anon x=" + x);          // anon x=5
    }
};
r.run();
System.out.println("outer x=" + x);                 // outer x=1
```

参数名同样受此约束：

```java
int s = 0;
// ❌ 参数名 s 与外层局部变量 s 冲突
// Function<String, Integer> f = s -> s.length();
```

> 🎯 **核心要点**：Lambda 是**词法作用域（lexical scope）**的——它在语法上"就地展开"，与外围代码共享同一个作用域；匿名内部类则是一个**真正的新类体**，自带独立作用域。记住这一条，`variable is already defined` 这个错误就不再神秘。

### 6.3 静态上下文中的限制

```java
public class StaticDemo {
    private int field = 1;

    static void staticMethod() {
        // ❌ 静态方法里没有 this，Lambda 自然也捕获不到实例字段
        // Runnable r = () -> System.out.println(field);
    }

    void instanceMethod() {
        Runnable r = () -> System.out.println(field);   // ✅ 隐式捕获 this
        r.run();
    }
}
```

> ⚠️ **内存泄漏预警**：只要 Lambda 访问了任何实例字段（哪怕只读一个），它就**持有整个外围对象的强引用**。把这种 Lambda 注册到全局监听器、静态缓存或长生命周期的线程池队列里，外围对象就永远无法回收。
>
> 规避方式：把需要的字段**先复制到局部变量**，让 Lambda 只捕获那个值。
>
> ```java
> // ❌ 捕获整个 this
> registry.register(() -> handle(this.config));
> // ✅ 只捕获需要的值
> Config snapshot = this.config;
> registry.register(() -> handle(snapshot));
> ```

---

## 7. 返回值兼容性与受检异常

### 7.1 void 兼容性：一个宽松的特例

规范允许**表达式体 Lambda 的返回值被静默丢弃**，只要该表达式是"语句表达式"（方法调用、赋值、自增等）：

```java
List<String> list = new ArrayList<>();

// list.add 返回 boolean，但赋给 Consumer（void）合法——返回值被丢弃
Consumer<String> c = s -> list.add(s);      // ✅ 实测通过

// 同一段代码也能当作有返回值使用
Supplier<Boolean> sp = () -> list.add("x"); // ✅
```

但这个宽容有边界——**只对"语句表达式"生效**。语句表达式是指那些**单独加个分号就能成为一条语句**的表达式：方法调用、赋值、自增自减、`new`。其余表达式（算术运算、字面量、三元）不行：

```java
// ✅ 方法调用是语句表达式，返回值可静默丢弃
Consumer<String> ok = s -> s.length();          // 实测通过

// ❌ 算术表达式不是语句表达式
// Consumer<Integer> bad = n -> 1 + 1;
// 实测错误: lambda 主体与 void 函数接口不兼容
//          (请考虑使用块 lambda 主体, 或者改为使用语句表达式)
```

对照记忆：`s.length();` 单独写一行是合法语句（哪怕毫无意义），而 `1 + 1;` 单独写一行是编译错误——这正是 void 兼容性的判据。

| 函数体形式 | 赋给 `Supplier<String>` | 赋给 `Runnable` | 说明 |
|-----------|:---:|:---:|------|
| `() -> "abc"` | ✅ | ❌ | 字面量不是语句表达式 |
| `() -> list.add("x")` | ❌ 类型不符 | ✅ | 方法调用，返回值可丢弃 |
| `() -> { return "abc"; }` | ✅ | ❌ | 显式返回值与 void 冲突 |
| `() -> { list.add("x"); }` | ❌ 缺返回值 | ✅ | 值已被语句块丢弃 |
| `() -> System.out.println("x")` | ❌ 无值 | ✅ | `void` 方法调用 |
| `() -> { }` | ❌ 缺返回值 | ✅ | 实测错误："缺少返回值" |

### 7.2 语句块体必须"所有路径都有返回值"

```java
// ❌ 缺少 else 分支的返回
// Function<Integer,String> bad = x -> { if (x > 0) return "正"; };

// ✅ 补全所有路径
Function<Integer, String> good = x -> {
    if (x > 0) return "正";
    return "非正";
};
```

规则与普通方法完全一致：编译器做**明确返回（definite return）**分析。

### 7.3 受检异常：Lambda 最疼的痛点

函数式接口的抽象方法**声明了什么异常，Lambda 就只能抛什么异常**。`java.util.function` 里的接口一个 `throws` 都没有，于是：

```java
static String risky() throws Exception { return "x"; }

// ❌ 实测错误：未报告的异常错误Exception; 必须对其进行捕获或声明以便抛出
// Supplier<String> s = () -> risky();
```

三种解法，按推荐度排序：

**解法一：就地 try-catch（简单场景）**

```java
Supplier<String> s = () -> {
    try {
        return risky();
    } catch (Exception e) {
        throw new IllegalStateException("调用 risky 失败", e);
    }
};
```

缺点是每个 Lambda 都要写一遍，噪音大。

**解法二：自定义可抛异常的函数式接口（推荐）**

```java
@FunctionalInterface
interface ThrowingSupplier<T, E extends Exception> {
    T get() throws E;
}

/** 把可抛检查异常的 Supplier 包装成标准 Supplier */
static <T> Supplier<T> unchecked(ThrowingSupplier<T, ? extends Exception> supplier) {
    return () -> {
        try {
            return supplier.get();
        } catch (RuntimeException e) {
            throw e;                                   // 运行时异常原样抛出
        } catch (Exception e) {
            throw new IllegalStateException(e);        // 检查异常包装
        }
    };
}

// 使用：调用点重新变得干净
Supplier<String> s = unchecked(MyClass::risky);
System.out.println(s.get());
```

**解法三：返回结果对象而非抛异常（函数式风格）**

```java
// 用 Optional 表达"可能失败"，把异常降级为空值
static <T> Optional<T> tryGet(ThrowingSupplier<T, ? extends Exception> supplier) {
    try {
        return Optional.ofNullable(supplier.get());
    } catch (Exception e) {
        return Optional.empty();
    }
}

Optional<String> result = tryGet(MyClass::risky);
result.ifPresentOrElse(System.out::println, () -> System.out.println("失败"));
```

| 解法 | 适用场景 | 代价 |
|------|---------|------|
| 就地 try-catch | 一两处、异常处理逻辑各不相同 | 代码噪音 |
| `unchecked` 包装器 | 大量同类调用（IO、反射） | 异常类型信息丢失 |
| `Optional` / Result | 失败是正常业务分支 | 丢失异常细节，需另行记录 |

> ⚠️ 千万别写 `catch (Exception e) { }` 这种空吞。包装时**务必把原异常作为 cause 传下去**，否则线上排查会直接失去堆栈。

### 7.4 非受检异常自由通过

```java
Function<String, Integer> parser = Integer::parseInt;
// parser.apply("abc");   // 抛 NumberFormatException（RuntimeException），编译无碍
```

运行时异常不受 `throws` 约束，会沿着调用栈正常向上传播。这也意味着：**Stream 流水线中任何一个 Lambda 抛运行时异常，整条流水线立即终止**。

---

## 8. 常见误区速查

| # | 误区 | 真相 |
|:-:|------|------|
| 1 | Lambda 就是匿名内部类的语法糖 | 不是。编译产物、`this`、作用域全都不同，底层走 `invokedynamic` |
| 2 | Lambda 里 `this` 指向 Lambda 自己 | 指向**外围类实例**，Lambda 没有自己的 `this` |
| 3 | effectively final 意味着"值不能变" | 意味着"**变量不能重新赋值**"，对象内部状态照样可改 |
| 4 | 用数组绕过限制就安全了 | 绕过了编译检查，**没绕过并发问题** |
| 5 | Lambda 可以定义与外层同名的局部变量 | 不能，共享外层作用域；匿名内部类才可以 |
| 6 | 加了 `@FunctionalInterface` 才能用 Lambda | 不需要，注解只是编译期校验，见 [02 模块](./02-Lambda函数式接口全家桶.md) |
| 7 | 传统 for 的 `i` 可以直接被 Lambda 捕获 | 不行，需要复制到新局部变量；增强 for 的变量才可以 |
| 8 | Lambda 可以抛任意受检异常 | 只能抛**目标方法签名声明**的异常 |
| 9 | `var f = x -> x;` 能推断出类型 | 不能，Lambda 无独立类型，必须有目标类型 |
| 10 | 只读一个字段的 Lambda 很轻量 | 它捕获了**整个 `this`**，可能导致外围对象无法回收 |
| 11 | 函数式接口重载有歧义时编译器一定报错 | **不一定**。`Runnable` vs `Callable<Boolean>` 会静默选中 `Callable` |
| 12 | 泛型接口方法都能用 Lambda 实现 | 抽象方法本身是**泛型方法**时不行，只能用方法引用/匿名类 |

### 8.1 编译错误速查表

下列错误信息均为 **JDK 25 中文环境实测输出**（英文环境为对应的 `error:` 文案）。

| 错误信息（中文 JDK） | 根因 | 定位章节 |
|---------------------|------|---------|
| 从 lambda 表达式引用的本地变量必须是最终变量或实际上的最终变量 | 捕获了被重新赋值的局部变量 | [5.2](#52-effectively-final-的准确定义) |
| 已在方法 xxx 中定义了变量 x | Lambda 参数/局部变量与外层重名 | [6.2](#62-作用域lambda-不引入新的变量作用域) |
| 未报告的异常错误 Exception; 必须对其进行捕获或声明以便抛出 | 抛出接口未声明的受检异常 | [7.3](#73-受检异常lambda-最疼的痛点) |
| 无法推断本地变量 f 的类型（lambda 表达式需要显式目标类型） | 用 `var` 接收 Lambda | [1.3](#13-一个关键认知lambda-没有自己的类型) |
| 不兼容的类型: Object 不是函数接口 | 赋给了非函数式接口 | [1.3](#13-一个关键认知lambda-没有自己的类型) |
| 对 xxx 的引用不明确…都匹配 | 重载候选形状与返回类型均可匹配 | [3.2](#32-重载决议同一段-lambda-选中不同方法) |
| 名称冲突: …具有相同疑符 | 方法声明本身泛型擦除后重复 | [3.2](#32-重载决议同一段-lambda-选中不同方法) |
| lambda 主体与 void 函数接口不兼容 | 表达式体不是"语句表达式" | [7.1](#71-void-兼容性一个宽松的特例) |
| lambda 表达式中的返回类型错误: 缺少返回值 | 语句块体缺 `return` | [7.2](#72-语句块体必须所有路径都有返回值) |
| lambda 表达式的函数描述符无效…为泛型方法 | 目标接口的抽象方法是**泛型方法** | [8.2](#82-一个冷门但硬核的规则泛型方法-sam) |

### 8.2 一个冷门但硬核的规则：泛型方法 SAM

**Lambda 无法实现泛型方法**，但方法引用和匿名内部类可以。这是实测验证过的：

```java
@FunctionalInterface
interface GenericSam { <T> T pick(T a, T b); }

static <T> T first(T a, T b) { return a; }

public static void main(String[] args) {
    // ❌ 错误: lambda 表达式的函数描述符无效 —— 方法 (T,T)T 为泛型方法
    // GenericSam g = (a, b) -> a;

    // ✅ 方法引用可以
    GenericSam g = MyClass::first;
    System.out.println(g.pick("A", "B"));    // A

    // ✅ 匿名内部类也可以
    GenericSam anon = new GenericSam() {
        @Override public <T> T pick(T a, T b) { return b; }
    };
    System.out.println(anon.pick("A", "B")); // B
}
```

原因：Lambda 的形参类型必须在编译期**确定**，而泛型方法的类型参数要到**每个调用点**才确定，Lambda 无法为"每个 T"都生成一份实现。

> 💡 这条规则的实践含义：**设计自定义函数式接口时，把泛型放在接口上（`interface Picker<T>`），不要放在方法上（`<T> T pick(...)`）**，否则接口无法用 Lambda 实现，等于废掉了一半的价值。

---

## 9. 动手练习

### 练习 1：判断能否编译

逐个说明下面每段代码能否通过编译，不能则给出原因。

```java
// (a)
int n = 10;
Supplier<Integer> s1 = () -> n;
n = 20;

// (b)
List<Runnable> tasks = new ArrayList<>();
for (int i = 0; i < 3; i++) { tasks.add(() -> System.out.println(i)); }

// (c)
Map<String, Integer> map = new HashMap<>();
Consumer<String> c = k -> map.put(k, 1);

// (d)
String msg = "hi";
Function<String, Integer> f = msg -> msg.length();

// (e)
Object o = () -> System.out.println("run");
```

<details>
<summary>参考答案</summary>

- **(a) 不能**。`n` 在 Lambda 之后被重新赋值，不是 effectively final。赋值位置在 Lambda 之后也一样报错。
- **(b) 不能**。传统 for 的 `i` 反复自增。改法：循环体内 `int idx = i;` 再捕获 `idx`。
- **(c) 能**。`map` 引用本身没变，`put` 改的是堆上对象；且 `put` 是语句表达式，返回值可丢弃，void 兼容成立。
- **(d) 不能**。参数名 `msg` 与外层局部变量重名，Lambda 不引入新作用域。
- **(e) 不能**。`Object` 不是函数式接口。改法：`Object o = (Runnable) () -> ...;`

</details>

### 练习 2：重构受检异常

下面代码把每个文件读成字符串，异常处理淹没了主逻辑。请用自定义函数式接口 + 包装器重构。

```java
List<String> paths = List.of("a.txt", "b.txt");
List<String> contents = new ArrayList<>();
for (String p : paths) {
    try {
        contents.add(Files.readString(Path.of(p)));
    } catch (IOException e) {
        throw new UncheckedIOException(e);
    }
}
```

<details>
<summary>参考思路</summary>

```java
@FunctionalInterface
interface ThrowingFunction<T, R, E extends Exception> {
    R apply(T t) throws E;
}

static <T, R> Function<T, R> unchecked(ThrowingFunction<T, R, ? extends Exception> f) {
    return t -> {
        try {
            return f.apply(t);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("处理 " + t + " 失败", e);
        }
    };
}

// 调用点回归声明式
List<String> contents = paths.stream()
        .map(unchecked(p -> Files.readString(Path.of(p))))
        .collect(Collectors.toList());
```

注意 `catch (RuntimeException e) { throw e; }` 这一段——不加的话运行时异常会被多包一层，堆栈变脏。

</details>

### 练习 3：解释输出

```java
public class Puzzle {
    private String name = "field";

    void run() {
        String name = "local";
        Runnable lambda = () -> System.out.println(this.name + " / " + name);
        Runnable anon = new Runnable() {
            private String name = "anon";
            @Override public void run() {
                System.out.println(this.name + " / " + Puzzle.this.name);
            }
        };
        lambda.run();
        anon.run();
    }
}
```

<details>
<summary>参考答案</summary>

```text
field / local
anon / field
```

- Lambda 中 `this` = `Puzzle` 实例，故 `this.name` 是 `"field"`；裸 `name` 按词法作用域取到外层局部变量 `"local"`。
- 匿名类中 `this` = 匿名类实例，`this.name` 是它自己的字段 `"anon"`；要拿外围字段必须写 `Puzzle.this.name`。
- 这也印证了：匿名类可以有**自己的字段**，Lambda 不能。

</details>

### 练习 4：设计题

为"带重试的操作执行器"设计接口与实现，要求：

1. 支持任意返回值类型；
2. 允许被执行的操作抛受检异常；
3. 可配置最大重试次数与重试间隔；
4. 全部重试失败后抛出携带原始异常的运行时异常。

<details>
<summary>参考实现</summary>

```java
@FunctionalInterface
interface RetryableTask<T> {
    T execute() throws Exception;
}

static <T> T withRetry(RetryableTask<T> task, int maxAttempts, long delayMillis) {
    Exception last = null;
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
        try {
            return task.execute();
        } catch (Exception e) {
            last = e;
            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();      // 恢复中断状态
                    throw new IllegalStateException("重试被中断", ie);
                }
            }
        }
    }
    throw new IllegalStateException("重试 " + maxAttempts + " 次后仍失败", last);
}

// 使用
String body = withRetry(() -> httpClient.get("/api/user"), 3, 500);
```

要点：`attempt` 是循环变量但**没被 Lambda 捕获**，所以自增合法；`InterruptedException` 必须恢复中断标志位，这是 JUC 的基本规范。

</details>

---

## 小结

```text
Lambda 语法与捕获的四条主线
│
├── 类型从哪来？   → 目标类型（赋值/传参/返回/强转）赋予，Lambda 自身无类型
├── 参数怎么定？   → 目标接口的唯一抽象方法逐位对齐，可省略可 var
├── 外部变量怎么用？→ 局部变量值拷贝且须 effectively final；字段经 this 引用可读写
└── 语义边界在哪？  → this 指外围、无独立作用域、异常受签名约束
```

> 🎯 **一句话收束**：Lambda 的所有"怪异"编译错误，追根溯源只有两个原因——**它没有自己的类型**（所以需要目标类型），**它没有自己的作用域和 this**（所以共享外围环境）。把这两点想透，第 8 节的十个误区会自动瓦解。

---

**上一模块**：[00-Lambda表达式知识体系总览](./00-Lambda表达式知识体系总览.md)  
**下一模块**：[02-Lambda函数式接口全家桶](./02-Lambda函数式接口全家桶.md)  
**返回总览**：[00-Lambda表达式知识体系总览](./00-Lambda表达式知识体系总览.md)
