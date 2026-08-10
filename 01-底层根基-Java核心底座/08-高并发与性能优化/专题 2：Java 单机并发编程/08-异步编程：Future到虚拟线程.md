# 08 异步编程：Future 到虚拟线程

> 异步范式二十年演进——Future 的局限、CompletableFuture 编排、虚拟线程 + 结构化并发（JEP 525），以及 2026 的选型决策

---

## 📚 目录

1. [Future：第一代异步的局限](#1-future第一代异步的局限)
2. [CompletableFuture：编排与错误处理](#2-completablefuture编排与错误处理)
3. [JDK 25 变更：默认执行器统一](#3-jdk-25-变更默认执行器统一)
4. [虚拟线程 + 结构化并发（JEP 525）](#4-虚拟线程--结构化并发jep-525)
5. [2026 异步选型决策](#5-2026-异步选型决策)

---

## 1. Future：第一代异步的局限

Future 提供"异步结果容器"：任务在别的线程跑，主线程 `get()` 阻塞取结果。局限是结构性的：

- **阻塞即反模式**：`get()` 把异步变成同步，且无超时配置时可能永远挂住；
- **无法编排**：任务 A 完成后接 B、A 和 B 并行后合 C——Future 只能层层 `get()` 串起来，异常处理散布在各层；
- **无法感知完成回调**：只能轮询 isDone 或阻塞等待。

这些痛点催生了 Guava ListenableFuture（回调）与 JDK 8 的 CompletableFuture（函数式编排）。

## 2. CompletableFuture：编排与错误处理

CompletableFuture 把"结果 + 编排 + 异常"装进一个对象。API 分三族，心智模型是"流式管道"：

- **创建**：`supplyAsync(Supplier)`、`runAsync(Runnable)`、`completedFuture(v)`。
- **串联**：`thenApply`（同步转换）、`thenCompose`（结果继续是异步任务，扁平化——避免 CF<CF<T>> 嵌套）、`thenAccept`（消费，无返回值）、`thenRun`（跑完执行，忽略结果）。
- **并行合并**：`thenCombine`（两个结果合并）、`allOf`（全部完成，返回 CF<Void>，需自行 getEach 取结果）、`anyOf`（任意完成）。`anyOf` 返回 Object 需强转——用带类型的 `orTimeout` + 泛型封装更稳。

命名里带 `Async` 后缀的方法（`thenApplyAsync`）在 `ForkJoinPool.commonPool` 或指定 Executor 上执行，不带后缀的在**当前线程**执行（调用链线程）。这个差异是回调地狱与线程切换的源头：`thenApply` 链上的同步转换会占用调用线程（IO 线程池的线程），高频场景要显式选 Async + 专用 Executor，否则 IO 线程被纯计算任务霸占，吞吐不升反降。

**错误处理是生产重心**：`exceptionally`（异常转成结果）、`handle`（成败都处理，两参版本）、`whenComplete`（旁观，不改结果）。注意**依赖链的异常会一路传播**：中间任何一环抛异常，后续 thenApply 直接跳过，直到遇 exceptionally/handle 才兜住——所以兜底要放在链尾，且异常类型要精确捕获，防"成功分支被异常分支覆盖"。三个细节：`orTimeout` 超时抛的是 `TimeoutException`，会沿链传播，兜底时要把"超时"与"业务异常"分开处理（超时意味着任务可能还在跑，重试要谨慎）；`join()` 抛 CompletionException 包装原始异常，`get()` 抛 ExecutionException——取根因要 `getCause()`；async 链里每层异常都吞掉的话，链尾只能看到最后一个异常，排查时要在每层入口留日志。

```java
CompletableFuture<User> f = CompletableFuture.supplyAsync(() -> userApi.get(id))
    .thenCompose(u -> CompletableFuture.supplyAsync(() -> orderApi.list(u)))
    .orTimeout(3, TimeUnit.SECONDS)
    .exceptionally(e -> { log.error("查询失败", e); return User.empty(); });
```

## 3. JDK 25 变更：默认执行器统一

CompletableFuture 的 `supplyAsync` 不传 Executor 时用 ForkJoinPool.commonPool，历史上在**单核/双核机器上会退化为 ThreadPerTaskExecutor**——每个任务新建系统线程，HttpClient 内部异步调用在低配机器上曾因此线程爆炸。JDK 25 起行为统一：**无显式 Executor 的异步方法一律提交 commonPool**，不再按核数走退化路径。这条变更提醒两点：生产代码显式传 Executor 仍是更优实践（可控性）；依赖默认池的代码要清楚 commonPool 的并行度 = 核数减 1，高吞吐场景会排队。

## 4. 虚拟线程 + 结构化并发（JEP 525）

虚拟线程（JDK 21 GA）把"一个任务一个线程"变成常态：IO 密集任务不再需要回调编排，**顺序阻塞代码直接跑在百万级并发下**——代码可读性回到同步时代，吞吐达到异步上限。与之配套的上下文传递用 [ScopedValue（JDK 25 转正）](../../02-JUC%20多线程%20高并发编程/虚拟线程/07-ScopedValue作用域值.md) 替代 ThreadLocal，作用域绑定、自动继承、零泄漏。

结构化并发 [JEP 525](https://openjdk.org/jeps/525)（JDK 26 第六次预览，需 --enable-preview）解决"子任务生命周期失控"：子任务必须在父作用域内 fork/join，作用域退出时未完成子任务自动取消——错误处理、线程泄漏、可观测性一次解决：

```java
try (var scope = StructuredTaskScope.open()) {
    Future<String> a = scope.fork(() -> userApi.get(id));     // 默认虚拟线程
    Future<String> b = scope.fork(() -> orderApi.list(id));
    // JDK 26: 全部成功才返回结果列表，任一失败即取消其余
    List<String> results = scope.join().allSuccessfulOrThrow();
}
```

JDK 26 的 Joiner API 补齐了完成策略：`anySuccessfulOrThrow()`（冗余服务竞争）、`awaitAll()`（等全部无论成败）、`allUntil(predicate)`（按条件提前收网）。相比 CompletableFuture 的链式编排，结构化并发让**并发结构等于代码结构**——异常与取消的语义是确定性的，这正是 2026 被力推的原因（[Java Concurrency in 2026](https://java-news.net/java-concurrency-in-2026-the-end-of-the-wild-west) 视其为"野西时代的终结"）。

结构化并发从 JEP 428（孵化）到 JEP 453（预览）、JEP 462、JEP 480 到 JDK 26 的 JEP 525 已六轮迭代，核心 API 经历了从 `ShutdownOnFailure/ShutdownOnSuccess` 类到 `StructuredTaskScope.open()` + `Joiner` 的收敛——趋势是"少而稳"：默认策略覆盖 90% 场景（全部成功或失败取消），定制策略留给 Joiner。需要 `--enable-preview` 说明它尚未转正（转正预计在 2027 年 JDK 29 LTS 前），生产可用但要注意每次 JDK 升级的 API 微调。

## 5. 2026 异步选型决策

- **单个 IO 调用**：直接同步阻塞调用，跑在虚拟线程上（Tomcat + spring.threads.virtual.enabled=true）——不需要任何异步 API。
- **多任务并行聚合**（fan-out/join）：默认 `StructuredTaskScope`（虚拟线程 + 作用域取消）；JDK 26 前用 `scope.join()` + 手动检查，或过渡用 CompletableFuture.allOf。
- **复杂依赖编排/回调链**（现有代码）：CompletableFuture 仍胜任，注意显式 Executor 与链尾兜底；迁移到结构化并发时逐段替换。
- **响应式高吞吐（WebFlux 等）**：极端场景保留，但 2026 共识是"先虚拟线程，不够再响应式"——虚拟线程的阻塞模型覆盖了 99% 的业务需求，且心智负担低一个量级。
- **Context 传递**：ThreadLocal 在虚拟线程池/结构化并发下会串数据，新代码用 ScopedValue；存量 ThreadLocal 在明确作用域内使用 + 及时 remove。判据很简单：ThreadLocal 的生命周期绑定线程，虚拟线程成千上万且复用载入线程，ThreadLocal 要么数据错乱（复用）要么泄漏（不清理）；ScopedValue 的生命周期绑定代码块，进入作用域绑定、退出自动销毁，天然匹配请求级上下文（用户身份、traceId）。

选型的另一面是**团队与存量**：全虚拟线程化意味着对库的阻塞语义审查（JDBC、HTTP 客户端、锁），小团队新项目可以激进，存量系统按模块逐步迁移并压测对比。技术选型没有银弹，但 2026 的方向已经明确——同步阻塞代码 + 虚拟线程优于回调编排，除非场景真在"回调带来的零拷贝收益"这一端。

> 🎯 **核心要点**：异步的终点不是回调，而是"用虚拟线程让同步代码跑出异步吞吐"；结构化并发（JEP 525）把子任务生命周期绑进代码块，2026 的 fan-out 首选；CompletableFuture 退居"存量编排"与"JDK 26 前过渡"角色。

---

**下一模块**：[09 并发性能调优与故障排查](09-并发性能调优与故障排查.md)

**返回总览**：[00-总览](00-总览.md)
