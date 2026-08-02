# 05 - Java 新特性与 JDK 演进高频题

> 🎯 2026 年面试新特性权重持续上升 — Java 8 是底线，Java 17/21（LTS）是关键，虚拟线程/结构化并发是加分项。本章按版本演进梳理必考点

---

## 目录

1. [Java 8：Lambda 与 Stream（必考底线）](#1-java-8lambda-与-stream必考底线)
2. [Java 9-11：模块化与局部变量推断](#2-java-9-11模块化与局部变量推断)
3. [Java 14-17：record 与 sealed](#3-java-14-17record-与-sealed)
4. [Java 21 LTS：虚拟线程与顺序集合](#4-java-21-lts虚拟线程与顺序集合)
5. [Java 23-25：结构化并发与新 LTS](#5-java-23-25结构化并发与新-lts)
6. [版本演进与升级策略](#6-版本演进与升级策略)

---

## 1. Java 8：Lambda 与 Stream（必考底线）

### 1.1 Lambda 表达式原理？

**参考答案：**
- Lambda 是**函数式接口**（只有一个抽象方法的接口，@FunctionalInterface）的实例
- 编译时：invokedynamic 指令 + 运行时生成实现类（MethodHandle 调用目标方法）
- **捕获的局部变量必须 final 或 effectively final**（变量在捕获后不被修改）

```java
// 函数式接口
Runnable r = () -> System.out.println("run");        // 无参
Comparator<String> c = (a, b) -> a.length() - b.length();  // 有参
```

**追问：** 和匿名内部类的区别？→ 匿名类编译期生成类文件、this 指向匿名类；Lambda 运行时生成、this 指向外部类、无独立作用域（变量名不能与外层冲突但作用域相同）。方法引用？→ `类::方法` 语法糖（静态/实例/构造引用）。

### 1.2 Stream API 的核心特性与陷阱？

| 特性 | 说明 | 示例 |
|------|------|------|
| 惰性求值 | 中间操作不执行，终止操作才触发 | filter/map/sorted |
| 不可变 | 不修改源数据，返回新流 | — |
| 流水线 | 中间操作链 + 终止操作 | collect/forEach/count |
| 并行流 | parallelStream 自动分治 | ⚠️ 慎用 |

```java
// 经典链式写法
List<String> names = users.stream()
    .filter(u -> u.getAge() > 18)          // 过滤
    .map(User::getName)                     // 映射
    .sorted()                               // 排序
    .distinct()                             // 去重
    .collect(Collectors.toList());          // 终止

// 分组：按状态分组
Map<String, List<Order>> byStatus = orders.stream()
    .collect(Collectors.groupingBy(Order::getStatus));
```

**追问：**
- 并行流为什么慎用？→ ① 共享可变状态线程不安全 ② 数据量小时分治开销反超 ③ 公共 ForkJoinPool 被大任务拖垮。**数据量大 + 纯函数无状态**才考虑
- Stream 会滥用吗？→ 简单循环用增强 for 更可读；Stream 用在不简单过滤/分组/聚合
- peek 的坑？→ 中间操作非终止不触发，用于调试可能"不执行"

### 1.3 Optional 与新时间 API？

```java
// Optional：显式处理空值，防 NPE 链
Optional.ofNullable(user).map(User::getAddress)
    .map(Address::getCity).orElse("未知");

// 新时间 API（线程安全 — LocalDateTime 不可变，Date/SimpleDateFormat 不安全）
LocalDateTime now = LocalDateTime.now();
LocalDateTime target = now.plusDays(7).withHour(0);
// DateTimeFormatter 线程安全；SimpleDateFormat 非线程安全（每个线程一个或 ThreadLocal）
```

**追问：** Optional 的误用？→ `Optional.of(null)` 直接 NPE；把 Optional 当字段/参数传（设计上只做返回值）。为什么新时间 API 更好？→ 不可变线程安全 + 明确时区（ZonedDateTime）+ 精确的 Duration/Period 计算。

---

## 2. Java 9-11：模块化与局部变量推断

### 2.1 JDK9 模块化（JPMS）？

**参考答案：** module-info.java 声明模块依赖与导出包，实现强封装（没导出谁都访问不了 — 即使反射也不行，除非 open）。

```java
// module-info.java
module com.example.order {
    requires com.example.common;      // 依赖模块
    exports com.example.order.api;    // 导出包（对外可见）
    opens com.example.order.internal; // 开放给反射框架（Spring 需要）
}
```

**追问：** 为什么 Spring 项目很少用模块化？→ 大量反射场景需要 open 声明，改造成本高、收益（强封装）对应用层不明显；JDK 自身和库层受益最大。

### 2.2 var 局部变量推断（JDK10）？

```java
var list = new ArrayList<String>();   // 推断为 ArrayList<String>
// 注意：var 不是动态类型（var ≠ JavaScript 的 var），编译期推断，类型固定
// 限制：不能用于方法参数/返回值、字段；lambda 参数不可
```

**追问：** 用 var 的争议？→ 提高可读性（少写重复类型）但也降低可读性（类型隐藏）；规范建议：局部变量类型明显时用，链式调用中间结果慎用。

---

## 3. Java 14-17：record 与 sealed

### 3.1 record 是什么？（JDK16 正式）

**参考答案：** 不可变数据载体 — 自动生成构造器、getter（字段同名）、equals/hashCode/toString。

```java
// 传统 POJO：几十行样板代码
public record User(Long id, String name, Integer age) {
    // 紧凑构造器：可加校验
    public User {
        if (age < 0) throw new IllegalArgumentException("age 非法");
    }
    // 自定义方法
    public String displayName() { return name + "(" + id + ")"; }
}
```

**追问：** 和 Lombok @Data 的区别？→ record 是语言级（字节码层面），不可变、final 字段、无 setter；Lombok 是编译期注解处理。record 适合什么？→ DTO、VO、事件对象、返回结果封装。record 的字段能改吗？→ 不可变，但组件类型自身可变（如 List 字段仍可 add）。

### 3.2 sealed 密封类与模式匹配（JDK17）？

```java
// sealed：限制继承范围 —— 替代"乱继承"
public sealed interface Shape permits Circle, Rectangle, Triangle {
    double area();
}
public final class Circle implements Shape { ... }
public record Rectangle(double w, double h) implements Shape { ... }

// 模式匹配 for switch（JDK21 正式，JDK17 preview）
String desc = switch (shape) {
    case Circle c    -> "圆：" + c.radius();
    case Rectangle r -> "矩形：" + r.area();
    default          -> "未知";
};
```

**追问：** sealed 解决什么？→ 防止继承爆炸，编译器可穷举（配合 switch 模式匹配无需 default 也能编译通过 — 穷举检查）。switch 表达式 vs 语句？→ 表达式有返回值、箭头语法无 fall-through、必须穷举或用 default。

---

## 4. Java 21 LTS：虚拟线程与顺序集合

### 4.1 虚拟线程（JEP 444，与 03 模块 9 节联动）

**一句话：** 轻量级线程，M:N 映射，专治 IO 密集型高并发 — 百万连接不再需要"线程池调优"。

```java
// Spring Boot 3.2+ 一行启用
spring.threads.virtual.enabled=true
```

**追问：** 虚拟线程时代线程池还有意义吗？→ 连接数/限流仍需信号量控制；CPU 密集任务仍用平台线程池。pinned 问题？→ 虚拟线程在 synchronized 块内执行阻塞 IO 会"钉住"载体线程（阻塞平台线程），应优先用 ReentrantLock。

### 4.2 Sequenced Collections（JEP 431，见 02 模块 6 节）

### 4.3 记录模式（Record Patterns，JDK21 正式）？

```java
// 配合 instanceof 解构 record
if (obj instanceof User(Long id, String name)) {
    System.out.println(id + ":" + name);   // 直接解构，无需强转
}
```

**追问：** 与传统的 instanceof + 强转？→ 类型检查 + 解构一步完成，代码更安全简洁。

---

## 5. Java 23-25：结构化并发与新 LTS

### 5.1 结构化并发（Structured Concurrency，JDK 24 转正）？

**参考答案：** 把并发任务组织成"作用域结构" — 子任务的生命周期与父作用域绑定，**要么全部完成，要么全部取消**，杜绝"泄漏的线程"。

```java
// 传统：任务 A 抛异常，B 还在后台跑（线程泄漏、结果丢失）
// 结构化并发：A/B 任一失败 → 其余自动取消
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    Future<String> user = scope.fork(() -> fetchUser());
    Future<String> order = scope.fork(() -> fetchOrder());
    scope.join();                    // 等待全部（或首个失败）
    scope.throwIfFailed();           // 有失败则抛出
    return user.resultNow() + order.resultNow();
}
```

**追问：** 解决什么问题？→ 并发任务的**错误传播与取消**：传统 CompletableFuture 组合在"一个失败、其他继续"时很难统一取消；结构化并发的 scope 自动取消所有未完成任务。与虚拟线程的关系？→ 天生搭档：虚拟线程解决"创建成本"，结构化并发解决"生命周期管理"。

### 5.2 Scoped Values（JDK 24 转正）？

**参考答案：** 线程内/作用域内共享不可变数据的新方案，替代 ThreadLocal 的"继承 + 清理"痛点（不泄漏、无需 remove）。

```java
// 定义作用域值
private static final ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();
// 绑定并执行（绑定只在作用域内有效，结束自动消失）
ScopedValue.where(REQUEST_ID, "req-123").run(() -> doWork());
// 子线程/虚拟线程中可继承读取
```

**追问：** vs ThreadLocal？→ ThreadLocal 有泄漏风险、跨线程传递需 Inheritable/Transmittable；ScopedValues 作用域化绑定、自动清理、配合虚拟线程传递。

### 5.3 JDK 25（2025 年 9 月，新 LTS）看点？

| 方向 | 内容 |
|------|------|
| 版本节奏 | 每 2 年一个 LTS：8 → 11 → 17 → 21 → **25** |
| 特性基调 | 在 21/24 基础上收敛：虚拟线程、结构化并发、Scoped Values 持续完善 |
| 生态配套 | Spring Boot 4 / Spring Framework 7 已全面对齐新 LTS |
| 对面试影响 | "你用什么 Java 版本？为什么？"成为必问 — 版本意识本身是考点 |

**追问：** 应该用哪个版本？→ 新项目选 **21（LTS，生态成熟）** 或直接 **25（新 LTS）**；存量 8 项目评估迁移收益（新特性 + 性能 + 安全补丁窗口）。

---

## 6. 版本演进与升级策略

### 6.1 LTS 时间线与关键特性速查？

| 版本 | LTS | 关键特性 |
|------|:---:|----------|
| Java 8 | ✅ | Lambda/Stream/Optional/新时间 API（行业基线） |
| Java 11 | ✅ | var（10）/HTTP Client/垃圾收集器优化 |
| Java 17 | ✅ | record（16）/sealed/switch 模式匹配 preview/强封装 |
| Java 21 | ✅ | **虚拟线程**/Sequenced Collections/记录模式/switch 模式正式 |
| Java 24 | ❌ | 结构化并发、Scoped Values 转正 |
| Java 25 | ✅ | 新 LTS（2025.09） |

### 6.2 升级到 17/21 的常见坑？

| 坑 | 说明 | 对策 |
|----|------|------|
| 强封装 | 反射访问 JDK 内部类（如 sun.misc）被拒 | 加 --add-opens 或改实现 |
| 移除了 JVM 参数 | -XX:MaxPermSize 等已删除 | 迁移文档核对启动参数 |
| Lombok/代理 | 旧版本不兼容新版本字节码 | 升级依赖（Lombok 1.18.30+） |
| 老 GC 设置 | -XX:+UseConcMarkSweepGC 报错 | CMS 已移除 → 换 G1/ZGC |
| 日期/时区数据 | 三方库依赖旧 tzdata | 更新依赖或 -Duser.timezone 核对 |

**追问：** 升级收益怎么量化？→ 性能（G1/ZGC 停顿、逃逸分析增强）、安全（长期补丁）、新特性（虚拟线程直接提升 IO 并发上限）、招聘市场认可度（2026 面试默认 Java 17+ 起步）。

---

> 🎯 **核心要点**：新特性模块的面试策略 = **"8 是底线、17 是默认、21 是亮点"**。Java 8 的四件套（Lambda/Stream/Optional/时间 API）必须讲透；17 的 record/sealed 能写代码；21 的虚拟线程能讲清楚"解决了什么、适合什么、怎么启用" — 这就是 2026 面试的差异化得分点。

**下一模块**：[06-深度追问与场景实战](06-深度追问与场景实战.md) / **返回总览**：[00-Java核心面试大全总览](00-Java核心面试大全总览.md)
