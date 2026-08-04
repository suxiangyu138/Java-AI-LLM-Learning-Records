# 05 - 符号在 Java 中的语义与应用

> **核心摘要**：每个符号在 Java 中有专属语义——`@` 是注解、`::` 是方法引用、`->` 是 Lambda、`<>` 是菱形泛型。本文按符号逐一拆解 Java 语义、高频应用场景与易错点，形成「符号 → Java 用法」的完整映射。

> **前置阅读**：[[02-标点符号与分隔符]]、[[03-运算符符号体系]]

---

## 📚 目录

1. [Java 符号语义总表](#1-java-符号语义总表)
2. [@ 注解符号](#2--注解符号)
3. [-> 与 :: Lambda 家族](#3---与--lambda-家族)
4. [泛型符号 <>](#4-泛型符号)
5. [Java 独有符号速查](#5-java-独有符号速查)
6. [符号的代码风格规范](#6-符号的代码风格规范)
7. [核心要点](#7-核心要点)

---

## 1. Java 符号语义总表

> **背景**：Java 语言设计者给每个符号赋予了明确语义——理解符号的 Java 身份是读 Java 代码的基础。
> **目的**：形成「见到符号 → 立即反应 Java 语义」的映射。
> **适用范围**：Java 全部；部分符号（:: 方法引用）为 Java 8+ 新特性。

| 符号 | Java 语义 | 示例 |
|:---:|---------|------|
| `@` | 注解（Annotation） | `@Override` |
| `::` | 方法引用（Method Reference） | `System.out::println` |
| `->` | Lambda 箭头 | `(x) -> x * 2` |
| `<>` | 菱形运算符（泛型推断） | `new ArrayList<>()` |
| `...` | 可变参数（Varargs） | `String... args` |
| `?` | 通配符（泛型） | `List<?>` |
| `extends`/`super` | 泛型边界（关键字非符号） | `T extends Number` |
| `#` | 无（Java 不用 # 注释） | Python 才用 |
| `_` | 下划线（Java 9+ 弃用单下划线） | `user_name` |
| `$` | 合法标识符字符（可出现在名字中） | `a$b`（不推荐） |

---

## 2. @ 注解符号

### 2.1 注解的本质

> **背景**：`@` 是 Java 的注解符号——给代码加「元数据」，编译器/框架读取后执行逻辑。
> **目的**：声明式编程（写注解声明意图，框架执行）。
> **适用范围**：Spring（@Controller/@Autowired）、标准库（@Override/@Deprecated）、自定义注解。
> **不适用场景**：运行时逻辑（注解是声明不是逻辑）。

```java
// 标准注解
@Override                    // 重写检查（编译期验证）
@Deprecated                  // 标记废弃（编译警告）
@SuppressWarnings("unchecked")  // 抑制警告

// Spring 注解（2026 主流用法）
@RestController               // 声明 REST 控制器
@RequestMapping("/api/users") // 路由映射
@Autowired                   // 依赖注入
@Transactional               // 事务管理
@Service                     // 服务层组件

// 自定义注解（元注解控制）
@Target(ElementType.METHOD)   // 注解可用的位置
@Retention(RetentionPolicy.RUNTIME)  // 保留策略
public @interface RateLimit {
    int limit() default 100;   // 注解参数
}
```

### 2.2 注解的易错点

```text
注解常见错误
├── ⚠️ 注解拼写错误 → 编译错误（@Overrid 不是 @Override）
├── ⚠️ @Override 忘写 → 重写方法没标记（可维护性问题）
├── ⚠️ Spring 注解忘加依赖 → 启动失败（组件扫描不到）
├── ⚠️ 注解参数类型错误 → 编译错误
└── 排查：@ComponentScan 扫描范围 → 注解失效
```

---

## 3. `->` 与 `::` Lambda 家族

### 3.1 Lambda 箭头 `->`（Java 8+）

```java
// Lambda：匿名函数的简写
// 传统匿名类
Runnable r1 = new Runnable() {
    @Override
    public void run() { System.out.println("run"); }
};

// Lambda（-> 箭头）
Runnable r2 = () -> System.out.println("run");

// 带参数
Comparator<Integer> cmp = (a, b) -> a - b;
Function<Integer, Integer> square = x -> x * x;   // 单参数可省略括号

// 流操作（高频场景）
list.stream()
    .filter(x -> x > 10)          // 过滤
    .map(x -> x * 2)              // 映射
    .forEach(System.out::println); // 方法引用
```

### 3.2 方法引用 `::`（Java 8+）

```java
// 方法引用：Lambda 的进一步简写
// 静态方法引用
Function<String, Integer> f1 = Integer::parseInt;   // = s -> Integer.parseInt(s)

// 实例方法引用（对象）
list.forEach(System.out::println);    // = x -> System.out.println(x)

// 构造器引用
Supplier<List<String>> s1 = ArrayList::new;   // = () -> new ArrayList<>()

// 使用规则：方法签名与函数式接口匹配
// 用不到 :: 时就用 Lambda——两者等价
```

### 3.3 Lambda 易错点

```text
Lambda 常见错误
├── ⚠️ 捕获外部变量需 final/effectively final（不可变）
├── ⚠️ this 语义：Lambda 内 this 是「外层对象」（匿名类是自身）
├── ⚠️ 返回值类型：代码块 Lambda 需 return（{ return x*2; }）
└── ⚠️ 与匿名类的区别：作用域与 this 不同（面试题）
```

---

## 4. 泛型符号 <>

### 4.1 泛型基础

```java
// 泛型（尖括号声明类型参数）
List<String> names = new ArrayList<>();     // 菱形 <> 自动推断
Map<String, Integer> scores = new HashMap<>();

// 泛型方法
public <T> T first(List<T> list) { return list.get(0); }

// 通配符 ?
List<?> any = ...;               // 任意类型（只读）
List<? extends Number> nums;     // 上界（Number 子类）
List<? super Integer> ints;      // 下界（Integer 父类）
```

### 4.2 泛型易错点

```text
泛型常见错误
├── ⚠️ raw type：List list = ...（裸类型——编译警告，丢失类型安全）
├── ⚠️ 泛型数组：new T[10] 非法（类型擦除）
├── ⚠️ 静态上下文：静态方法/字段不能引用类泛型
├── ⚠️ instanceof 泛型：obj instanceof List<String> 非法（用 ? 通配）
└── ⚠️ 菱形 <> 与比较 < > 混淆：泛型声明里的 < 不是小于
```

---

## 5. Java 独有符号速查

### 5.1 符号速查（Java 专属）

| 符号 | Java 语义 | 场景 |
|:---:|---------|------|
| `...` | 可变参数 | `void f(String... args)`——调用可传 0-N 个 |
| `?` | 泛型通配符 | `List<?>`（未知类型） |
| `::` | 方法引用 | `Integer::parseInt` |
| `->` | Lambda | `(x) -> x+1` |
| `<>` | 菱形推断 | `new ArrayList<>()` |
| `@` | 注解 | `@Override` |
| `;` | 语句结束 | 每条语句必须 |
| `//` `/* */` `/** */` | 三种注释 | 单行/块/Javadoc |
| `+=` 等复合赋值 | 赋值 + 运算 | `x += 1` |
| `instanceof` | 类型判断 | `obj instanceof String` |

### 5.2 注释符号详解（Java 三种）

```java
// 单行注释（最常用）
int x = 1;    // 行尾注释

/* 块注释（多行） */
/*
 * 传统块注释风格
 * 每行星号
 */

/** Javadoc（生成文档） */
/**
 * 计算订单总价
 * @param orders 订单列表
 * @return 总金额
 */
public BigDecimal total(List<Order> orders) { ... }

// ⚠️ 注释陷阱：
// ① 块注释不能嵌套（/* /* */ 提前结束）
// ② 注释掉的代码 → Git 恢复（别留注释代码）
// ③ Javadoc 的 @param/@return 标签与注解 @ 不同！
```

---

## 6. 符号的代码风格规范

### 6.1 团队规范（符号使用）

```text
Java 符号使用规范（Checkstyle 常见规则）
├── ① 运算符空格：a + b（不是 a+b）——可读性
├── ② 大括号：K&R 风格（左花括号同行尾）
├── ③ 缩进：4 空格（团队统一，配合 .editorconfig）
├── ④ 分号：每条语句必须有（无省略）
├── ⑤ 通配符导入：import java.util.* 禁止（显式导入）
├── ⑥ 魔法数字：禁止裸数字（用常量）
├── ⑦ 命名符号：_ 分隔 vs camelCase（Java 用 camelCase）
└── ⑧ 泛型：优先 List<String> 而非 raw type
```

### 6.2 命名规范中的符号

```text
符号在命名中的作用
├── _ 下划线：常量（MAX_SIZE）/测试方法名（should_return_true_when_x）
├── $ 美元：合法但禁止（编译器生成内部类用 $）
├── camelCase：变量/方法（orderService）
├── PascalCase：类名（OrderService）
├── UPPER_SNAKE：常量（MAX_RETRY_COUNT）
└── 符号规则：Java 标识符 = 字母/数字/_/$，不能数字开头
```

---

## 7. 核心要点

> 🎯 **核心要点**：
> 1. 符号 → Java 语义映射：@ 注解 / -> Lambda / :: 方法引用 / <> 泛型 / ... 可变参数 / ? 通配符
> 2. 注解是声明式编程的核心（Spring 全家桶靠 @ 驱动）——`@Override` 拼写错误是最高频注解错误
> 3. Lambda 三易错：捕获变量 effectively final / this 语义 / 代码块需 return
> 4. 泛型三易错：raw type 警告 / 泛型数组非法 / instanceof 不能带具体泛型
> 5. 三种注释（// /* */ /** */）——Javadoc 的 @param 是标签不是注解
> 6. 代码风格：运算符空格 + K&R 大括号 + camelCase——团队规范靠 Checkstyle/.editorconfig 强制

---

**下一模块**：[06-符号陷阱与实战排查](06-符号陷阱与实战排查.md) | **返回总览**：[00-键盘符号与编程符号知识体系总览](00-键盘符号与编程符号知识体系总览.md)
