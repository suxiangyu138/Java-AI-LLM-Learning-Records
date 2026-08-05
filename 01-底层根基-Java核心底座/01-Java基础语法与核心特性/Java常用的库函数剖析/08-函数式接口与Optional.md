# 08 函数式接口与 Optional

> Function 家族五兄弟是 Stream 的积木，方法引用是 Lambda 的语法糖，Optional 是"可能为空"的类型安全表达——但 Optional 也有反模式

---

## 📚 目录

1. [Function 家族五兄弟](#1-function-家族五兄弟)
2. [方法引用四种形态](#2-方法引用四种形态)
3. [Optional 的创建与取值](#3-optional-的创建与取值)
4. [Optional 的链式转换](#4-optional-的链式转换)
5. [Optional 反模式](#5-optional-反模式)
6. [函数速查表](#6-函数速查表)

---

## 1. Function 家族五兄弟

**java.util.function 的核心五兄弟**（与 `Lambda表达式/` 联动）：

```java
// ① Function<T, R>：转换（一个入参一个返回）
Function<String, Integer> len = String::length;
len.apply("hello");                    // 5

// ② Predicate<T>：判断（返回 boolean）
Predicate<String> nonEmpty = s -> !s.isEmpty();
nonEmpty.test("a");                    // true

// ③ Consumer<T>：消费（无返回）
Consumer<String> print = System.out::println;
print.accept("hi");

// ④ Supplier<T>：供应（无入参返回）
Supplier<LocalDate> today = LocalDate::now;
today.get();                           // 2026-08-05

// ⑤ BiFunction<T, U, R>：双参转换
BiFunction<Integer, Integer, Integer> add = Integer::sum;
add.apply(1, 2);                       // 3
```

**组合方法**（Stream 之外的复用）：

```java
// Function 组合：andThen / compose
Function<String, Integer> len = String::length;
Function<Integer, String> str = String::valueOf;
len.andThen(str).apply("hello");       // "5"（先 len 后 str）
str.compose(len).apply("hello");       // "5"（先 len 后 str，反向）

// Predicate 组合：and / or / negate
Predicate<String> hasA = s -> s.contains("a");
Predicate<String> hasB = s -> s.contains("b");
hasA.and(hasB).test("ab");             // true
hasA.or(hasB).test("a");               // true
hasA.negate().test("x");               // true
```

> 🎯 **核心要点**：五兄弟 = "**Function 转换/Predicate 判断/Consumer 消费/Supplier 供应/BiFunction 双参**"——**"test/apply/get/accept"四个调用动词**是记忆锚；组合方法（andThen/and/or）让函数可复用（`Lambda表达式/02` 全家桶深挖）。

---

## 2. 方法引用四种形态

**方法引用 = Lambda 的语法糖**（可读性优先时）：

```java
// ① 静态方法：类::staticMethod
Function<String, Integer> f1 = Integer::parseInt;     // = s -> Integer.parseInt(s)

// ② 实例方法（特定对象）：instance::method
String prefix = "prefix: ";
Function<String, String> f2 = prefix::concat;          // = s -> prefix.concat(s)

// ③ 实例方法（任意对象）：类::instanceMethod
Function<String, Integer> f3 = String::length;         // = s -> s.length()
BiPredicate<String, String> f4 = String::equals;       // = (a, b) -> a.equals(b)

// ④ 构造器：类::new
Supplier<List<String>> f5 = ArrayList::new;            // = () -> new ArrayList<>()
Function<String, BigDecimal> f6 = BigDecimal::new;     // = s -> new BigDecimal(s)

// 数组构造：int[]::new
IntFunction<int[]> f7 = int[]::new;                    // = n -> new int[n]
```

**方法引用 vs Lambda 的选择**：

```text
方法引用：已有现成方法 → 更简洁可读（首选）
Lambda：逻辑简单（比较/条件）→ 更直观
准则：能引用就引用，不能引用才写 Lambda
```

> 🎯 **核心要点**：方法引用四形态 = "**静态/特定实例/任意实例/构造器**"——**"String::length 与 instance::concat 的区别（参数位置）"是易混点**；准则 = 能引用优先（`Lambda表达式/03` 深挖）。

---

## 3. Optional 的创建与取值

**Optional = "可能为空"的类型安全容器**（JDK 8，`Java高级语法/03` 联动）：

```java
// 创建三兄弟
Optional<String> a = Optional.of("value");       // 非 null（null 抛 NPE！）
Optional<String> b = Optional.ofNullable(maybeNull);   // 可 null（推荐）
Optional<String> c = Optional.empty();           // 明确空

// 取值（三选一，避免 get 裸用）
String v1 = opt.orElse("默认值");                // 空 → 默认值（总是创建）
String v2 = opt.orElseGet(() -> expensive());    // 空 → 延迟计算（✅ 推荐）
String v3 = opt.orElseThrow(() -> new BizException("缺失"));  // 空 → 抛异常

// 判断
opt.isPresent();        // 是否存在
opt.isEmpty();          // 是否为空（JDK 11）
opt.ifPresent(v -> use(v));    // 存在才消费

// ❌ 反模式：get() 裸用（空时抛 NoSuchElementException）
String v = opt.get();   // 危险！
```

**orElse vs orElseGet**（高频面试）：

```java
String v = opt.orElse(defaultValue);        // defaultValue 总是被创建（即使非空）
String v = opt.orElseGet(() -> create());   // create() 只在空时执行（懒加载）
// → orElseGet 避免无谓创建（昂贵默认值时必须用）
```

> 🎯 **核心要点**：Optional = "**ofNullable 创建 + orElse/orElseThrow 取值**"——**"orElse 总是创建 vs orElseGet 懒加载"与"get 裸用是反模式"两个考点**；创建三兄弟（of/ofNullable/empty）是入门第一课。

---

## 4. Optional 的链式转换

**map / flatMap：Optional 的流式转换**：

```java
// map：转换（空则跳过）
Optional<String> name = user
        .map(User::getAddress)        // Optional<Address>
        .map(Address::getCity);       // Optional<String>

// ⚠️ map 的坑：嵌套 Optional
Optional<Order> order = ...;
order.map(o -> findDetail(o))          // 返回 Optional<Optional<Detail>>！
        .flatMap(Optional::identity);  // 拍平 → Optional<Detail> ✅

// flatMap：拍平（避免嵌套）
order.flatMap(o -> findDetail(o));     // ✅ 直接得到 Optional<Detail>

// filter：条件过滤
user.filter(u -> u.getAge() >= 18)
    .ifPresent(u -> allow(u));

// 实战链式：多级取值 + 默认
String city = user.map(User::getAddress)
        .map(Address::getCity)
        .orElse("未知");
```

**链式的最佳实践**：

```text
① 深链取值：map 链（每级空自动短路）
② 嵌套 Optional：flatMap（避免 Optional<Optional<>>）
③ 过滤：filter
④ 终止：orElse/orElseThrow/ifPresent
→ Optional 链是"null 安全的地图导航"
```

> 🎯 **核心要点**：链式 = "**map 转换 + flatMap 拍平 + filter 过滤 + 终止取值**"——**"map 产生嵌套 Optional、flatMap 拍平"是最核心考点**；深链取值用 map 链替代逐层 if 判空。

---

## 5. Optional 反模式

**Optional 的错误用法**（2026 共识）：

| # | 反模式 | 问题 | 正确 |
|:-:|--------|------|------|
| 1 | **字段声明 Optional** | 不可序列化（违反 JavaBeans） | 字段用普通类型 |
| 2 | **方法参数 Optional** | 调用方被迫包装 | 参数判空（requireNonNull） |
| 3 | **get() 裸用** | 空抛 NoSuchElement | orElse/orElseThrow |
| 4 | **集合/数组包 Optional** | `Optional<List>` 双重语义 | 空集合表示无 |
| 5 | **isPresent + get** | 可用 ifPresent 替代 | 链式 |
| 6 | **性能敏感循环** | 包装开销 | 直接判空 |
| 7 | **返回值语义混乱** | 有时 Optional 有时 null | 统一 Optional 或统一 null |

```java
// 反模式演示：字段 Optional（违反规范）
public class User {
    private Optional<String> nickname;    // ❌ 不可序列化 + 语义混乱
    private String nickname;              // ✅ 普通字段，getter 返回 Optional
}
public Optional<String> getNickname() {   // ✅ 返回值用 Optional（方法级）
    return Optional.ofNullable(nickname);
}
```

**Optional 的使用边界**（什么时候用）：

```text
✅ 方法返回值：可能缺失（findXxx、parseXxx）
❌ 字段/参数/集合元素：不用（null 或空集合表达）
→ "Optional 是返回类型，不是类型系统"——Effective Java 观点
```

> 🎯 **核心要点**：Optional 边界 = "**返回值可用、字段/参数/集合不可用**"——**"Optional 作为返回类型（find 类方法）是唯一推荐场景"**是面试标准答案；七大反模式清单覆盖误用全貌。

---

## 6. 函数速查表

**函数式与 Optional 速查**（开发/面试快查）：

| 场景 | 函数 | 坑点 |
|------|------|------|
| 转换 | Function.apply | 组合用 andThen |
| 判断 | Predicate.test | 组合用 and/or |
| 消费 | Consumer.accept | 无返回 |
| 供应 | Supplier.get | 懒加载 |
| 方法引用 | 类::method | 四形态 |
| 创建 Optional | ofNullable | of(null) 抛 NPE |
| 取值 | orElse/orElseThrow | get 反模式 |
| 懒默认 | orElseGet | orElse 总是创建 |
| 转换 | map | 嵌套需 flatMap |
| 拍平 | flatMap | Optional<Optional> 解套 |

**一句话总结**：

```text
函数式五兄弟：Function/Predicate/Consumer/Supplier/BiFunction
方法引用四形态：静态/实例/任意/构造
Optional 三件事：ofNullable 创建、orElse 取值、map 链转换
Optional 边界：返回值用、字段参数不用（七反模式）
```

> 🎯 **核心要点**：速查 10 项 = "**函数式 + Optional 的完整考点**"——**"Optional 返回值专用 + 链式（map/flatMap/orElse）"两条主线**；方法引用与函数式组合（`Lambda表达式/02-03`）联动深挖。

---

**下一模块**：[09-面试高频考点与总结](./09-面试高频考点与总结.md) / **返回总览**：[00-库函数剖析知识体系总览](./00-库函数剖析知识体系总览.md)
