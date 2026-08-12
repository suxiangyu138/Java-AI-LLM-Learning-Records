# Optional 与错误即值范式

> 空值与错误是函数式管线的两个"隐形杀手"。Optional 把 null 从"运行时检查"升级为"类型化容器"，Result/Either 把异常从"控制流"降级为"数据"。前者 Java 8 就有，后者用 sealed 接口（JDK 17+）几行即可实现——2026 年 Java 原生版 Option 模式已是成熟实践

---

## 📚 目录

1. [Optional 的范式意义](#1-optional-的范式意义)
2. [正确链式与常见反模式](#2-正确链式与常见反模式)
3. [Optional 禁忌清单](#3-optional-禁忌清单)
4. [错误即值：sealed interface 实现 Result](#4-错误即值sealed-interface-实现-result)
5. [错误即值 vs 异常：选型判据](#5-错误即值-vs-异常选型判据)
6. [第三方 Option 库的 2026 现状](#6-第三方-option-库的-2026-现状)
7. [核心要点与思考题](#7-核心要点与思考题)

---

## 1. Optional 的范式意义

`Optional<T>` 是一个"可空的类型化容器"：值要么存在（`of`），要么不存在（`empty`）。它的范式价值不在 API 本身，而在**把 null 从隐形约定变成显式类型**：

- **签名即文档**：`Optional<User> findByEmail(String email)` 直接声明"可能没有结果"——调用方被迫面对这种可能性，而不是在 NPE 出现时才想起
- **组合而非分支**：链式 `map/flatMap/filter` 让"有则处理、无则跳过"写成表达式，替代 `if (u != null) { if (u.getAddr() != null) … }` 的嵌套
- **与 Stream 同构**：Optional 是"最多一个元素"的 Stream，`map` 语义完全一致——管线思维在空值上复用（Java 9 的 `stream()` 正式打通了两者）

```java
// 命令式：三层嵌套判空
String city = null;
if (user != null) {
    Address addr = user.getAddress();
    if (addr != null) city = addr.getCity();
}

// 函数式：一条链，空值自动短路
String city = Optional.ofNullable(user)
        .map(User::getAddress)
        .map(Address::getCity)
        .orElse("unknown");
```

## 2. 正确链式与常见反模式

正确姿势是**全程 map/flatMap/filter 链式 + 终点取值**，终点四选一：`orElse(默认值)`、`orElseGet(Supplier)`、`orElseThrow(异常)`、`or(另一个Optional)`。

```java
String name = repo.find(id)                          // Optional<User>
        .filter(u -> u.isActive())
        .map(User::displayName)
        .orElseGet(() -> "anonymous");

BigDecimal total = optAmount.orElseThrow(() -> new BizException("amount missing"));
```

反模式（高频出现在存量代码里，review 必抓）：

- `isPresent() + get()` 两段式——链式能表达的意图，别退回 if
- `orElse(expensive())`——`orElse` 的参数**无条件求值**，昂贵默认值应换 `orElseGet`（惰性，08 章详述）
- `map` 返回 `Optional` 的嵌套——`Optional<Optional<X>>` 要用 `flatMap` 拍平
- 用 `ifPresent` 做副作用后继续处理——它返回 void，管线中断

也要承认 Optional 链的**可读性上限**：超过四个操作符的链（`map→flatMap→filter→orElseThrow` 再叠加）开始难以阅读，每步失败原因也说不清。此时两种选择：拆成带名字的中间变量（每步一个 `Optional<X> step = ...`），或直接换守卫链——**链式适合"路径单一、语义简单"的判空，守卫链适合"多条件分支、需要原因"的校验**。判据是 review 时"读一遍能否复述语义"。

```java
// ❌ orElse 陷阱：默认值每次都构建
return find(id).orElse(new User());        // 即使有值也 new 了

// ✅ 惰性默认值
return find(id).orElseGet(User::new);
```

## 3. Optional 禁忌清单

Optional 是"返回类型"而非"万能容器"，四条红线（Java 8 至今官方 javadoc 明确声明，2026 无变化）：

| 禁忌 | 原因 | 正确做法 |
|------|------|---------|
| 做实体字段 | 序列化框架（含多数 ORM）不支持；字段级空值本应由 null 表达 | 字段用 null + `@Nullable` |
| 做方法参数 | 让调用方被迫包一层，纯增样板；语义上"必传"才用参数 | 参数用 null 或拆成重载 |
| 做集合元素 | 集合自身已表达"没有"，`Optional` 嵌套无信息增量 | `Stream<Optional<T>>` → `flatMap(Optional::stream)` |
| 存业务状态 | "无值"和"错误"是两回事，Optional 表达不了原因 | 用 Result 模式（下节） |

> 💡 **关键区分**：Optional 表达**缺失（absence）**，不表达**失败（failure）**。"查无此人"是缺失，"数据库连不上"是失败——后者若也用 Optional 吞掉，错误原因就丢了，这是 2026 年代码评审中最常纠正的设计错误。

## 4. 错误即值：sealed interface 实现 Result

**错误即值（errors as values）**：把成功值与失败原因打包成返回值，调用方用模式匹配分支处理。JDK 17 的 sealed 接口（JEP 409）让它成为 Java 原生范式——编译期穷尽性检查保证"所有失败分支都被处理"：

```java
sealed interface Result<T> permits Ok, Err {}

record Ok<T>(T value) implements Result<T> {}
record Err<T>(String reason) implements Result<T> {}

// 使用：switch 模式匹配（JDK 21 定稿），编译器强制覆盖 Ok 与 Err
switch (result) {
    case Ok<Order>(var order) -> orderService.record(order);
    case Err<Order>(var reason) -> log.error("failed: " + reason);
}
```

功能增强的两个方向：

- **错误分类**：`Err` 携带错误码/异常类型（`record Err<T>(BizCode code, String reason)`），业务层据此决定重试、告警或返回用户错误
- **链式组合**：为 Result 加 `map/flatMap/onSuccess/onFailure`，让"可能失败"的步骤也能进管线（实现与 Optional 同构，代码约 20 行）

```java
interface Result<T> {
    default <R> Result<R> map(Function<T, R> fn) {
        return this instanceof Ok<T>(var v) ? new Ok<>(fn.apply(v)) : (Result<R>) this;
    }
    default <R> Result<R> flatMap(Function<T, Result<R>> fn) {
        return this instanceof Ok<T>(var v) ? fn.apply(v) : (Result<R>) this;
    }
}
```

用组合后的 Result 编排"多步可能失败"的业务链，是它相对 try-catch 的核心优势——**失败即短路，且短路原因显式可读**：

```java
// 下单链路：校验 → 扣库存 → 生成订单 → 预支付。任一步失败即返回原因，无异常栈、无 try-catch
Result<PayOrder> r = validate(cart)
        .flatMap(this::deductStock)      // 失败：库存不足 → 直接短路
        .flatMap(this::createOrder)
        .flatMap(this::preparePayment);

if (r instanceof Err<PayOrder>(var reason)) {
    log.warn("order rejected: {}", reason);   // 业务失败是"预期事件"，不是"异常故障"
}
```

注意与 02 章边界模型的呼应：这条链全部在确定性核心内——没有 IO、没有异常、失败以值传递；真正写库、发支付请求的副作用留到链外由调用方执行。

Result 的并行编排同样成立：多条独立链并行执行（CompletableFuture 或虚拟线程，09 章），各自返回 `Result<T>`，最后 `allOf` 聚合——**失败依然以值汇聚**，每个分支的原因可单独读取，比"并行里抛异常、汇总栈"清晰得多。这也是"错误即值"在并发场景下的额外红利：异常在线程间传递靠包装，值的传递则零负担。

## 5. 错误即值 vs 异常：选型判据

2026 年的共识不是"谁取代谁"，而是**按错误性质分流**：

| 维度 | 错误即值（Result） | 异常 |
|------|-------------------|------|
| 可预期性 | 业务可预期的失败（校验不过、库存不足） | 意外故障（IO 错误、NPE） |
| 组合性 | 进管线、可 flatMap、可并行 | 打断管线，只能 try-catch |
| 性能 | 零成本（返回对象） | 栈快照有开销（仅失败路径） |
| 显式性 | 签名强制处理 | 可能漏 catch（非受检） |
| 适用层 | 领域/应用核心（确定性核心内） | 边界适配（IO、框架） |

实践判据：**调用方能不能合理地"忽略"这个失败？** 能——用 Result 强迫处理；不能——异常表达"没人能处理的意外"更诚实。典型分工：仓库层查询用 `Optional` 表缺失、服务层业务校验用 `Result` 表失败、框架边界（Controller/定时任务）用异常统一兜底。这正呼应 02 章的边界模型：核心内部全是值，边界才允许抛出。

还有一个常被忽视的维度——**高频失败路径的性能**：每次抛异常都会创建异常对象并采集栈快照（代价是普通返回值的几十到上百倍），而"校验失败"在秒级流量下可能每毫秒都在发生——全部走异常，GC 压力与 CPU 开销叠加，且日志里塞满"预期内的失败"。用 Result 后，高频失败变成普通返回，栈只在真正意外（IO 故障、程序错误）时采集。这个论据对"异常派"开发者通常最有力：**错误即值不只是在风格上更函数式，在高频路径上是量级级的开销差异**。

## 6. 第三方 Option 库的 2026 现状

Java 生态的 Option/Either 库（Vavr 前身 Javaslang 等）曾在 Java 8 时代填补空白。2026 年的现实：

- JDK 原生能力（Optional + sealed Result + 模式匹配）已覆盖 90% 需求，第三方库的附加价值（元组、模式匹配 DSL）被 JDK 渐进吸收
- 从 2026-08 检索情况看，**Vavr 的维护状态与 JDK 25/26 兼容性未见权威确认**——新项目不建议引入，存量项目谨慎升级，避免把"标准范式"押在低活跃库上
- 判断标准：先问"JDK 能不能做"，答案是否再引第三方（元组这类 JDK 至今没有的场景才考虑）

> 🎯 **核心要点**：Optional 管"缺失"，Result（sealed + record + 模式匹配）管"失败"——前者 Java 8，后者 JDK 17 后的原生手法。两者组合，确定性核心内的代码可以做到"无异常、无 null、全显式"。

## 7. 核心要点与思考题

> 🎯 **本章三句话总结**：
> 1. Optional 的价值是类型化缺失语义：链式 map/flatMap 替代嵌套判空，`orElse` 注意无条件求值陷阱；
> 2. Optional 四条红线：字段/参数/集合元素/业务状态，缺失 ≠ 失败，失败要用 Result；
> 3. sealed interface + record + switch 模式匹配实现原生 Result，业务失败走值、意外故障走异常，边界分层清晰。

**思考题**：

1. `orElse(new User())` 和 `orElseGet(User::new)` 差在哪？（→ 2 节）
2. `Stream<Optional<T>>` 怎么变 `Stream<T>`？为什么用 flatMap 而不是 filter+get？（→ 3 节）
3. sealed 的 Result 和普通 class Result 比，多给编译器什么能力？（→ 4 节）
4. "库存不足"该用 Result 还是异常？为什么？（→ 5 节）

---

**下一模块**：[07-函数式与Stream范式结合](07-函数式与Stream范式结合.md)｜**返回总览**：[00-函数式编程知识体系总览](00-函数式编程知识体系总览.md)

---

## 参考来源

- [Optional（Oracle JDK 26 文档）](https://docs.oracle.com/en/java/javase/26/docs/api/java.base/java/util/Optional.html)——javadoc 红线声明
- [Functional Programming in Java — Complete Guide（dev.to, 2026）](https://dev.to/devcorner/functional-programming-in-java-complete-guide-for-modern-backend-engineers-5gbc)——Optional 最佳实践与 Result 模式
- [Functional Programming Patterns in Java（2026）](https://softwarepatternslexicon.com/java/functional-programming-patterns-in-java/)——错误即值模式
