# Future 与 Promise：可组合的异步值

> 第二代异步模型把"回调过程"变成"可组合的值"：`CompletableFuture`（Java）与 `Promise`（JS）提供链式组合子与集中错误处理。但"值的组合"带来三个新问题——函数着色、futurelock、静默失败。本章讲透这代模型的完整账本，正好承接协程登场的理由

---

## 📚 目录

1. [异步值的读写两侧：Future 与 Promise](#1-异步值的读写两侧future-与-promise)
2. [CompletableFuture：组合子家族](#2-completablefuture组合子家族)
3. [JS Promise：链式与三态](#3-js-promise链式与三态)
4. [函数着色问题：生态分裂的根源](#4-函数着色问题生态分裂的根源)
5. [futurelock：异步死锁新形态](#5-futurelock异步死锁新形态)
6. [静默失败：未处理拒绝](#6-静默失败未处理拒绝)
7. [核心要点与思考题](#7-核心要点与思考题)

---

## 1. 异步值的读写两侧：Future 与 Promise

异步值模型有两个角色：**Future（读侧）**——未来结果的只读容器，调用方拿它"取结果/挂回调"；**Promise（写侧）**——由执行者持有，用 `set_value`/`resolve` 把结果填进去：

```javascript
// Promise 两侧：写侧 resolve，读侧 then
const promise = new Promise((resolve, reject) => {
    doAsyncWork(data => resolve(data));   // 写侧：完成时填值
});
promise.then(d => console.log(d));        // 读侧：结果到达时回调
```

```java
// Java 的 CompletableFuture 同时是 Future 与 Promise（可读可写）
CompletableFuture<String> f = new CompletableFuture<>();
f.complete("done");              // 写侧
f.thenAccept(System.out::println); // 读侧
```

模型的核心价值：**异步结果第一次成为"值"**——可以被存储、传递、组合、作为返回值。回调时代的"结果无处安放"被解决：方法签名里写 `Future<Order>` 就是契约"稍后给你订单"。

## 2. CompletableFuture：组合子家族

Java 8 的 `CompletableFuture` 把组合子家族补齐，异步编排第一次有了"函数式"表达（与「函数式编程」体系 09 章的 CompletableFuture 章节对应，此处给全景）：

| 组合子 | 语义 | 类比 |
|--------|------|------|
| `thenApply(f)` | 结果变换（同步） | map |
| `thenCompose(f)` | 结果触发新异步任务 | flatMap |
| `thenCombine(f, fn)` | 两任务并行、结果合并 | zip |
| `allOf` / `anyOf` | 多任务全部/任一完成 | Promise.all/race |
| `exceptionally(f)` / `handle(f)` | 失败分支恢复 | catch |
| `orTimeout` / `completeOnTimeout` | 超时控制（Java 9+） | 超时 |

```java
CompletableFuture<OrderDetail> detail = 
    orderFuture.thenCompose(o -> userFuture(o).thenCombine(itemFuture(o), OrderDetail::new))
               .orTimeout(3, TimeUnit.SECONDS)
               .exceptionally(ex -> OrderDetail.fallback());
```

**2026 的定位变化**：虚拟线程定稿后，CompletableFuture 从"Java 异步的主方案"降级为"并行扇出与事件驱动专项"——**顺序编排用虚拟线程同步代码，并行聚合用 CompletableFuture/结构化并发**（JEP 525）。但它的组合子思想（map/flatMap/zip）是理解后续所有异步抽象的钥匙，也直接影响了 JS Promise 与 Kotlin 协程的 API 设计。

**CompletableFuture 的两个易错点**也在此一并钉死（面试高频）：**一是 thenApply 里返回 Future**——变换函数返回 `CompletableFuture<R>` 必须用 `thenCompose` 拍平，否则产生 `CompletableFuture<CompletableFuture<R>>` 的嵌套地狱（与 Optional 的 map/flatMap 陷阱同构，见「函数式编程」06 章）；**二是默认线程池共享**——`supplyAsync(fn)` 不传 executor 时跑在公共 ForkJoinPool，与 parallel stream 抢线程，生产必须显式传自有线程池。**"拍平"与"线程池"是 CompletableFuture 使用纪律的两个钉子**，钉不住就是线上事故。

## 3. JS Promise：链式与三态

JS Promise 的核心是**三态模型**：pending（进行中）→ fulfilled（成功）或 rejected（失败），且**状态一旦确定不可变**：

```javascript
fetch("/api/order")
    .then(r => r.json())                    // 链式：每步返回新 Promise
    .then(renderOrder)
    .catch(err => showError(err))           // 集中错误处理
    .finally(() => hideSpinner());          // 无论成败都执行
```

Promise 的两个设计亮点：

- **链式即顺序**：`.then` 返回新 Promise，链的书写顺序 = 执行顺序——回调时代的金字塔变成平铺
- **错误冒泡**：链上任一环节 reject，跳过后续 then 直达 catch——**"错误处理集中化"是 Promise 相对回调最实质的进步**

局限（推动协程）：Promise 依然是"回调的包装"——`await` 语法（async/await）出现后，Promise 的链式写法逐渐让位于"同步外观"（06 章）；但 `await` 的底层依然是 Promise，**JS 的 async/await 是 Promise 的语法糖而非替代**——这与 Java 虚拟线程（全新调度）有本质区别。

## 4. 函数着色问题：生态分裂的根源

**函数着色（function coloring）**：同步函数与异步函数"颜色不同"，同步代码不能直接调用异步函数（需要 await/回调），异步函数无处不在——**颜色会传染**：

```text
同步函数（无色）：可以被任何人调用
异步函数（着色）：只能被异步代码调用——调用者也被染色，一路传染到入口
```

后果：**生态分裂**——Python 的 `requests`（同步）不能直接用在 asyncio 里，只能 `httpx`（异步）；Java 的同步 JDBC 驱动在异步栈里阻塞事件循环，只能换异步驱动；第三方库要么双写（requests/httpx），要么被生态排挤。**着色问题把"异步的技术决策"变成"整个生态的维护成本"**。

两类解法（2026 格局）：

- **透明轻量线程**（虚拟线程/goroutine）：同步代码 + 轻量线程 = 无色并发——**彻底绕过着色问题**，Java/Go 因此"生态不分裂"
- **显式协程**（asyncio/JS）：着色是显式契约——生态被迫异步化，但换来"挂起即状态机"的控制力与低内存

判断：**"你的生态是同步主导还是异步主导"直接决定着色问题是否致命**——Java 生态同步主导，虚拟线程是正确解；Python 生态 AI 场景异步主导，asyncio 是正确解。

## 5. futurelock：异步死锁新形态

**futurelock**（2026 年评论文章正式命名的异步死锁形态）：异步代码中"等待链成环"且**栈不可见**——比传统死锁更难诊断：

```java
// 形态一：异步等异步成环
CompletableFuture<Void> a = new CompletableFuture<>();
CompletableFuture<Void> b = a.thenRun(() -> { /* 依赖 b */ });
// a 完成需要 b 的执行，b 的执行依赖 a 完成——环

// 形态二：阻塞式等待混入事件循环
future.get();                 // 在事件循环线程里阻塞等待——整个循环停摆
```

诊断困难的原因：**await/阻塞点之间没有完整调用栈**（挂起时栈已拆散），传统 `jstack` 只能看到"当前恢复点"，看不到"谁在等谁"。2026 年的实战工具：**线程转储增强**（JDK 21+ 对虚拟线程/CompletableFuture 的栈跟踪改善）、异步 profiler（09 章）的等待链分析、以及**设计级预防**：事件循环线程内禁止阻塞等待、等待链必须有超时（`orTimeout` 是 futurelock 的头号解药）。

## 6. 静默失败：未处理拒绝

Promise/CompletableFuture 的第二个新坑：**未处理的拒绝（unhandled rejection）静默消失**——`f.exceptionally(...)` 漏挂，失败就无声无息：

- JS：`unhandledrejection` 事件（Node 15+ 默认直接抛错）——语言层面兜底
- Java：CompletableFuture 的异常若无人消费，**静默吞掉**（仅靠"任务没执行"的诡异现象暴露）；`whenComplete`/`exceptionally` 至少要打日志

```java
// 危险：异常无人消费——线上表现为"任务没执行"
CompletableFuture.runAsync(() -> riskyOperation());

// 安全：每个 Future 都有终止分支
CompletableFuture.runAsync(() -> riskyOperation())
    .exceptionally(ex -> { log.error("job failed", ex); return null; });
```

工程纪律：**"每个 Future 必须有一个终止分支"**（成功消费 or 失败处理）应进 review 清单——静默失败是异步系统"数据丢失与业务错乱"的头号隐蔽来源（09 章排查清单再展开）。

**Promise 三态与 Future 完成语义的对照**再补一笔（面试对比题常客）：两者都是"一次性容器"——Promise 的 fulfilled/rejected 不可逆，CompletableFuture 的 complete/completeExceptionally 同样只能一次；区别在**组合层**——Promise 只有 then/catch 链与 all/race 静态方法，CompletableFuture 有完整的组合子家族（thenCompose/thenCombine/handle）与显式完成能力（complete/obtrudeValue），**"可写"是 Java 版本的独有设计**（Promise 的写侧只属于执行者，CompletableFuture 任何人都能 complete）——这把"异步结果"从"封闭的未来"变成"可编程的完成器"，是编排类框架（Netty 的 Promise 扩展）依赖它的原因。

> 🎯 **核心要点**：Future/Promise 把异步变成可组合的值（map/flatMap/zip/catch 组合子），链式顺序与集中错误处理是实质进步；三新坑——着色问题分裂生态（透明轻量线程是解法）、futurelock 栈不可见（超时是解药）、静默失败（终止分支纪律）——构成协程与虚拟线程登场的完整理由。

## 7. 核心要点与思考题

> 🎯 **本章三句话总结**：
> 1. 异步值模型：Future 读侧 + Promise 写侧，组合子家族（thenApply/thenCompose/allOf）让异步可编排；
> 2. 着色问题 = 同步/异步不可互调，生态被迫双写；虚拟线程/goroutine 透明绕过，显式协程选择接受；
> 3. futurelock（等待成环 + 栈丢失）与静默失败（未处理拒绝）是这代模型的新债——超时 + 终止分支是两条纪律。

**思考题**：

1. thenApply 与 thenCompose 的区别？（→ 2 节）
2. JS 的 await 是协程还是 Promise 语法糖？（→ 3 节）
3. 为什么 Java 选虚拟线程而 Python 选 asyncio？（→ 4 节）
4. futurelock 为什么比传统死锁难诊断？（→ 5 节）

---

**下一模块**：[06-协程与async-await：同步写法的异步执行](06-协程与async-await：同步写法的异步执行.md)｜**返回总览**：[00-异步编程全方位剖析知识体系总览](00-异步编程全方位剖析知识体系总览.md)

---

## 参考来源

- [What Async Promised and What it Delivered（2026-04）](https://github.com/jerrylususu/bookmark-summary/blob/ccfe3c98f57883406697466446cd3a35db88da9a/202604/2026-04-25-what-async-promised-and-what-it-delivered-%E2%80%94-causality.md)——futurelock 与结构成本论述
- [异步非阻塞的三种实现（阿里云开发者）](https://developer.aliyun.com/article/1731890)——Promise 模型综述
- [Coroutines vs Virtual Threads（Deep Engineering）](https://deepengineering.net/p/coroutines-vs-virtual-threads-and)——着色问题讨论
- [OpenJDK amber-dev 讨论：callback hell vs coloring（2026-02）](https://mail.openjdk.org/pipermail/amber-dev/2026-February/009645.html)——着色问题最新讨论
