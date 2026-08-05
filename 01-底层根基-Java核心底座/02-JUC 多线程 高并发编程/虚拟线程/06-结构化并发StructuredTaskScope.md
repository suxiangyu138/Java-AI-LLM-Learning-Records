# 结构化并发 StructuredTaskScope

> 虚拟线程解决了"线程不够用"，结构化并发解决"并发难管理"——把一组相关子任务的生命周期、错误处理与取消统一收拢到父作用域内。JDK 26 已到第六次预览（JEP 525），是 Loom 三件套中最后成熟的一块拼图

---

## 📚 目录

1. [非结构化并发的四大问题](#1-非结构化并发的四大问题)
2. [JEP 演进史：从孵化到六览](#2-jep-演进史从孵化到六览)
3. [核心 API 与基本用法（JDK 25/26 形态）](#3-核心-api-与基本用法jdk-2526-形态)
4. [错误处理与取消机制](#4-错误处理与取消机制)
5. [超时控制：joinUntil 与 onTimeout](#5-超时控制joinuntil-与-ontimeout)
6. [自定义 Joiner（源码级）](#6-自定义-joinersource-级)
7. [与 CompletableFuture 的选型](#7-与-completablefuture-的选型)
8. [使用前提与版本窗口](#8-使用前提与版本窗口)
9. [核心要点与思考题](#9-核心要点与思考题)

---

## 1. 非结构化并发的四大问题

先看传统写法的问题：

```java
// 传统：两个"独立"的 Future，生命周期完全脱管
ExecutorService pool = Executors.newFixedThreadPool(2);
Future<User> userF = pool.submit(() -> fetchUser(id));
Future<Order> orderF = pool.submit(() -> fetchOrder(id));
User user = userF.get();       // 若 fetchOrder 抛异常，fetchUser 还在后台跑
Order order = orderF.get();    // 若 fetchUser 抛异常，fetchOrder 还在后台跑
pool.shutdown();
```

| 问题 | 表现 |
|------|------|
| **线程泄漏** | 子任务与父任务无结构关系；父任务失败退出时，子任务无人取消，继续占资源运行 |
| **错误传播困难** | 需要手动 `future.get()` 逐个检查；一个失败不自动联动其他 |
| **取消不可靠** | 没有统一的取消通道，父取消必须手动 `future.cancel()` 每个子任务 |
| **可观测性差** | 转储/监控中无法看出"哪些线程属于同一请求"的父子关系 |

> 🎯 **核心要点**：非结构化并发 = 子任务的"生老病死"与父任务无关。结构化并发的核心承诺是——**父任务结束（无论正常还是异常）时，所有子任务必定已结束或已被取消**。

## 2. JEP 演进史：从孵化到六览

| JEP | 版本 | 阶段 | 关键变化 |
|-----|:---:|:---:|---------|
| JEP 428 | JDK 19 | 孵化器 | `StructuredExecutor` 初版 |
| JEP 437 | JDK 20 | 孵化器 | 改名 `StructuredTaskScope` |
| JEP 453 | JDK 21 | 预览 | `ShutdownOnFailure` / `ShutdownOnSuccess` 策略类 |
| JEP 462 | JDK 22 | 预览 | 增量完善（`fork(Callable)` 等） |
| JEP 480 | JDK 23 | 预览 | 增量完善 |
| JEP 499 | JDK 24 | 预览 | 增量完善 |
| **JEP 505** | **JDK 25** | 预览 | **引入 `Joiner` 抽象**：公开构造器改为静态工厂 `StructuredTaskScope.open(...)`；子任务自动继承 ScopedValue |
| **JEP 525** | **JDK 26** | 预览 | **`onTimeout()`** 超时处理、`allSuccessfulOrThrow()` 直接返回 `List<T>`、`anySuccessfulResultOrThrow()` 改名 `anySuccessfulOrThrow()`、`UnaryOperator<Configuration>` 配置修改器 |
| JEP 533 | JDK 27+ | 规划 | 第七次预览，预计 JDK 29 附近转正 |

> ⚠️ **版本窗口**：API 形态随版本剧烈变化（JDK 21 的策略类 → JDK 25 的 Joiner → JDK 26 的返回值变化）。**本文以 JDK 26（JEP 525）为准**，JDK 21-24 用户请参考对应版本文档。

## 3. 核心 API 与基本用法（JDK 25/26 形态）

### 3.1 API 全景

```text
StructuredTaskScope
├── static open()                                  // 默认 Joiner：全部成功，否则取消
├── static open(Joiner<T> joiner)                  // 指定结果合并策略
├── static open(Joiner<T>, UnaryOperator<Configuration>)  // + 配置修改（JDK 26）
├── <U> Subtask<U> fork(Callable<U>)               // 创建子任务（默认虚拟线程执行）
├── List<Subtask<T>> join()                        // 等待全部子任务结束
├── List<Subtask<T>> joinUntil(Instant deadline)   // 带超时等待（JDK 26 中返回 List<T> 取决于 Joiner）
└── close()                                        // 取消所有未结束子任务

Joiner<T>（结果合并策略，JDK 25+）
├── allSuccessfulOrThrow()                         // 全成功→List<T>；任一失败→取消其余+抛异常
├── anySuccessfulOrThrow()                         // 首个成功→返回；其余取消（原 anySuccessfulResultOrThrow，JDK 26 改名）
├── allSuccessful(Consumer<Subtask<T>>)            // 全成功后自定义消费结果
├── anySuccessful(Consumer<Subtask<T>>)
└── custom(...)                                    // 自定义策略（onTimeout 在此组合）
```

### 3.2 经典模式：并行请求组装（allSuccessfulOrThrow）

```java
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;

record OrderData(User user, Order order) {}

OrderData loadOrderData(long userId) throws Exception {
    try (var scope = StructuredTaskScope.open(Joiner.allSuccessfulOrThrow())) {
        var user = scope.fork(() -> fetchUser(userId));    // 子任务1：虚拟线程
        var order = scope.fork(() -> fetchOrder(userId));  // 子任务2：虚拟线程
        scope.join();      // 等全部；任一失败 → 自动取消另一个，join 抛异常
        return new OrderData(user.get(), order.get());     // Subtask.get() 取结果
    } // close()：兜底取消（如 join 抛异常退出时）
}
```

**执行保证**（这是结构化并发的核心契约）：

```text
1. fork 的任务默认在虚拟线程上运行（无需管理执行器）；
2. join() 返回时，所有子任务必已结束（成功/失败/被取消）；
3. 任一子任务失败 → 其余未完成子任务被自动取消（interrupt）；
4. try-with-resources 的 close()：若提前退出，取消所有未完成子任务；
5. 因此：作用域关闭后，不存在"还在跑的孤儿任务"——线程泄漏不可能发生。
```

### 3.3 模式二：任意成功即返回（anySuccessfulOrThrow）

```java
// 场景：多数据中心取最快结果、多路推荐源取第一个成功
String fastest = null;
try (var scope = StructuredTaskScope.open(Joiner.anySuccessfulOrThrow())) {
    scope.fork(() -> query("dc-1", userId));
    scope.fork(() -> query("dc-2", userId));
    scope.join();                       // 首个成功即返回；其余自动取消
    // JDK 26：join() 直接返回结果列表；首个成功结果在列表首个位置
}
```

## 4. 错误处理与取消机制

### 4.1 失败如何传播

```java
try (var scope = StructuredTaskScope.open(Joiner.allSuccessfulOrThrow())) {
    scope.fork(() -> { throw new IllegalStateException("订单服务挂了"); });
    scope.fork(() -> { Thread.sleep(10_000); return 1; });   // 还活着
    scope.join();   // ← 抛异常！
}
```

```text
失败传播链：
子任务抛异常 → JVM 记录失败原因 → 触发 shutdown()（关闭作用域接受新任务）
           → 取消其他未完成子任务（interrupt()）
           → join() 抛出该异常（AllSuccessful 策略：抛第一个失败子任务的异常）
           → 调用者 catch 后统一处理（重试、降级、返回默认值）
```

> 💡 **与 Future.get() 的区别**：传统写法"谁失败谁报错，其他任务照跑"；结构化并发是**全有或全无（fail-fast）**——一个子任务失败，整体失败，不留半成品。

### 4.2 Subtask 状态与结果读取

| Subtask 方法 | 语义 |
|-------------|------|
| `state()` | `SUCCESS` / `FAILED` / `CANCELLED`（父作用域关闭时） |
| `get()` | 成功返回结果；失败**抛出对应异常** |
| `getOrThrow()` | 失败时抛 `ExecutionException` 包装（JDK 25 形态） |
| `isDone()` | 子任务是否已结束 |

```java
// 自定义消费场景：部分失败也要处理成功者
try (var scope = StructuredTaskScope.open(Joiner.allSuccessful(subtask -> {
    if (subtask.state() == Subtask.State.SUCCESS) {
        metrics.record(subtask.get());
    }
}))) { ... }
```

## 5. 超时控制：joinUntil 与 onTimeout

### 5.1 基础形态：joinUntil

```java
try (var scope = StructuredTaskScope.open(Joiner.allSuccessfulOrThrow())) {
    scope.fork(() -> slowExternalCall());          // 可能 10s 才返回
    scope.joinUntil(Instant.now().plusSeconds(2)); // 2s 超时
    // 超时 → 抛 TimeoutException；作用域关闭 → 慢任务被取消
}
```

### 5.2 新形态：onTimeout（JDK 26，JEP 525）

默认 Joiner 在超时时一律抛 `TimeoutException`。`onTimeout()` 允许自定义 Joiner 在超时时**返回部分结果或降级值**：

```java
// 自定义 Joiner：超时返回空列表（降级），而非抛异常
Joiner<String> timeoutTolerant = Joiner.custom(
    () -> List.of(),                             // 作用域打开时的初始值
    (builder, subtask) -> builder.add(subtask.getOrThrow()),   // 每次成功累加
    () -> "timeout",                             // onTimeout：超时时的降级结果（JDK 26）
    results -> results                           // 终结函数
);
try (var scope = StructuredTaskScope.open(timeoutTolerant)) {
    scope.fork(() -> fetchRecommendation(userId));
    var result = scope.joinUntil(deadline);      // 超时返回 "timeout"，不抛异常
}
```

> 🎯 **核心要点**：`onTimeout()` 让"超时策略"从"只能失败"扩展为"可降级"——这是超时处理语义的实质性升级，也是 JEP 525 最重要的新能力。

## 6. 自定义 Joiner（源码级）

`Joiner` 接口（JDK 25 预览，JDK 26 补充 onTimeout）核心形状：

```java
public interface Joiner<T> {
    // 作用域打开时调用，返回"结果合并状态"的初始值
    R initial() { return null; }

    // 每个子任务成功完成时调用，把结果并入合并状态
    R onResult(R results, Subtask<? extends T> subtask) { return results; }

    // 每个子任务失败时调用
    R onError(R results, Subtask<? extends T> subtask) { return results; }

    // 所有子任务结束时调用，把合并状态转成最终结果（或抛异常）
    T onComplete(R results) throws Exception { return null; }

    // JDK 26：超时到达时调用，返回降级结果或抛 TimeoutException
    T onTimeout() { return null; }
}
```

```java
// 用途示例：并行抓取 N 个价格源，聚合成功者，忽略失败者
Joiner<Price> bestPriceJoiner = Joiner.custom(
    () -> null,
    (best, s) -> best == null || s.get().price() < best.price() ? s.get() : best,
    (best, s) -> best,                                  // 失败忽略
    best -> best                                        // 返回最低价
);
```

## 7. 与 CompletableFuture 的选型

| 维度 | StructuredTaskScope | CompletableFuture |
|------|:---:|:---:|
| 生命周期管理 | 结构化：父作用域关闭 = 子任务必结束 | 非结构化：孤儿任务无人管 |
| 取消 | 自动联动取消 | 手动 cancel，无法中断正在跑的代码 |
| 错误处理 | join() 统一抛出、fail-fast | 各自 exceptionally，链式散落 |
| 结果合并 | Joiner 集中定义 | 无原生聚合，手写 thenCombine |
| 调度 | 默认虚拟线程 | 默认公共 ForkJoinPool（可指定执行器） |
| 转正状态 | 预览（JDK 26 六览） | 转正（JDK 9+） |
| 适用 | **同作用域内的相关子任务**（一个请求拆分） | 跨层异步流水线、事件驱动、回调链 |

> 💡 **共存而非替代**：一个请求内的并行子任务用 `StructuredTaskScope`（生命周期一致、要取消联动）；跨请求、跨模块的异步编排（消息驱动、流水线）仍用 CompletableFuture。结构化并发聚焦"同生共死"的父-子关系。

## 8. 使用前提与版本窗口

### 8.1 预览 API 的启用方式

```bash
# JDK 21+：编译与运行都需要 --enable-preview
javac --release 26 --enable-preview OrderService.java
java  --enable-preview OrderService
```

```java
// 代码中无需 import 特殊包（java.util.concurrent 标准包）
import java.util.concurrent.StructuredTaskScope;
```

### 8.2 注意

| 事项 | 说明 |
|------|------|
| 仍是预览 | JDK 26 为第六次预览（JEP 525），**默认关闭**；预计 JDK 29 附近转正 |
| API 不稳定 | 每次预览都有 API 变更（构造器→工厂、Stream→List、改名），升级 JDK 需同步改代码 |
| 生产可用性 | 预览 API 生产可用需接受"升级兼容成本"；多数团队等转正再上 |
| 子任务继承 | JDK 25 起子任务自动继承父线程的 ScopedValue（07 章），上下文传递成为一等公民 |

## 9. 核心要点与思考题

> 🎯 **本章三句话总结**：
> 1. 结构化并发解决"子任务生命周期脱管"：作用域关闭时所有子任务必已结束或取消，线程泄漏在结构上不可能发生；
> 2. API 已演进六次（JEP 428→525）：JDK 25 引入 Joiner 抽象，JDK 26 新增 onTimeout 降级超时、allSuccessfulOrThrow 直接返回 List、anySuccessfulOrThrow 改名；
> 3. 它与虚拟线程天然互补：子任务默认跑虚拟线程 + ScopedValue 自动继承，共同构成"每请求一个作用域"的新编程范式。

**思考题**：

1. 结构化并发凭什么保证"不会线程泄漏"？（→ 3.2 的执行保证）
2. `allSuccessfulOrThrow` 与 `anySuccessfulOrThrow` 的取消行为差异？（→ 3.3/4.1）
3. JDK 26 的 `onTimeout()` 解决了什么之前做不到的事？（→ 5.2）
4. 生产项目现在（JDK 26 时代）该不该用结构化并发？（→ 8.2）

---

**下一模块**：[07-ScopedValue作用域值](07-ScopedValue作用域值.md)｜**返回总览**：[00-虚拟线程知识体系总览](00-虚拟线程知识体系总览.md)
