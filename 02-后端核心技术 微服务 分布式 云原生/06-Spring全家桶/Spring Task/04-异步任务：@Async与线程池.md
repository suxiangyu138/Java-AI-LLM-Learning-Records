# 04 异步任务：@Async 与线程池

> 定时任务只负责"何时触发"，真正耗时的业务要交给异步线程池——@Async 的代理机制、线程池选型、返回值形态与异常处理，是"异步不生效、异常被吞、线程池打满"三大生产事故的完整答案

---

## 📚 目录

1. [@Async 的代理机制与生效三条件](#1-async-的代理机制与生效三条件)
2. [TaskExecutor 体系与线程池选型](#2-taskexecutor-体系与线程池选型)
3. [返回值：void / Future / CompletableFuture](#3-返回值void--future--completablefuture)
4. [异步异常处理与回调](#4-异步异常处理与回调)
5. [事务与 SecurityContext 的传播](#5-事务与-securitycontext-的传播)
6. [异步场景清单：何时该用何时不该用](#6-异步场景清单何时该用何时不该用)

---

## 1. @Async 的代理机制与生效三条件

```java
@Configuration
@EnableAsync          // ① 必须开启（独立于 @EnableScheduling）
public class AsyncConfig { }

@Service
public class NotifyService {

    @Async                      // ② 方法标记
    public void sendSms(String phone) { ... }

    @Async
    public CompletableFuture<String> query() { ... }
}

// ③ 调用必须从容器外部进入（代理生效）
@RestController
public class OrderController {
    @Resource
    private NotifyService notifyService;

    @PostMapping("/order")
    public void create() {
        notifyService.sendSms("138...");   // ✅ 走代理 → 异步
    }
}
```

### 1.1 三大生效条件

| 条件 | 说明 | 失效表现 |
|------|------|---------|
| `@EnableAsync` | 激活代理与线程池装配 | 注解无效，同步执行 |
| 方法被代理调用 | **从外部 Bean 调用**（同类 `this.` 调用绕过代理） | 同步执行（最常见坑） |
| 方法非 private | 代理需能覆写 | 注解被忽略 |

> ⚠️ **自调用失效**：`this.sendSms()` 直接调用内部方法，绕过 Spring 代理——这是 @Async 第一大坑（与 @Transactional 完全同族）。解法：注入自身代理、拆成两个 Bean、或 `AopContext.currentProxy()`。

### 1.2 代理下的行为细节

- 返回类型只能是 `void` / `Future` / `CompletableFuture`（其它类型会忽略异步语义）；
- 异步方法的异常默认由 `AsyncUncaughtExceptionHandler` 处理（void 场景）；
- 代理类默认 CGLIB（Spring 7 起 CGLIB 为默认，无需 `proxyTargetClass` 开关）。

## 2. TaskExecutor 体系与线程池选型

### 2.1 体系结构

```text
TaskExecutor（接口）
├── SyncTaskExecutor           同步执行（等同直接调用）
├── SimpleAsyncTaskExecutor    每任务新线程（不推荐生产；Boot4 虚拟线程场景回归）
├── ThreadPoolTaskExecutor     生产主力：JUC ThreadPoolExecutor 的 Spring 封装
└── TaskExecutorAdapter        适配 java.util.concurrent.Executor

调度协同：Spring Boot 默认用 ExecutorConfigurationSupport 族
```

| 实现 | 特点 | 生产适用 |
|------|------|:---:|
| ThreadPoolTaskExecutor | 完整池语义（核心/最大/队列/拒绝） | ✅ 首选 |
| TaskExecutorAdapter + 虚拟线程 | Boot 4 默认（06 篇） | ✅ Java 21+ |
| SimpleAsyncTaskExecutor | 无池化、每任务一线程 | ❌ 一般不用 |
| SyncTaskExecutor | 同步 | ❌ 仅特殊 |

### 2.2 自定义线程池（生产模板）

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> log.error("async error: {}.{}", 
                method.getDeclaringClass().getSimpleName(), method.getName(), ex);
    }
}
```

> 💡 **配置属性版**：`spring.task.execution.pool.core-size=8`、`max-size=16`、`queue-capacity=200`、`thread-name-prefix=async-`——纯配置即可，无需写类。**Boot 4 + 虚拟线程场景下这些属性同样被忽略**（06 篇）。

### 2.3 拒绝策略选型

| 策略 | 行为 | 适用 |
|------|------|------|
| AbortPolicy（默认） | 抛 RejectedExecutionException | 明确失败（配合降级） |
| CallerRunsPolicy | 提交者线程执行 | **生产推荐**：打满时慢下来而非丢任务 |
| DiscardPolicy | 静默丢弃 | ❌ 不推荐 |
| DiscardOldestPolicy | 丢最老任务 | 最新优先场景 |

> 🎯 **要点**：队列打满 = 系统过载的信号。CallerRuns 让生产者自己干活（背压），Abort 让上层立刻感知——**宁可明确失败+告警，不要静默丢弃**。

## 3. 返回值：void / Future / CompletableFuture

```java
// 1. void：fire-and-forget（最常见）
@Async
public void notify(Long orderId) { ... }

// 2. CompletableFuture：拿结果/组合（推荐）
@Async
public CompletableFuture<StockInfo> loadStock(Long skuId) { ... }

// 3. Future：旧式阻塞等待
@Async
public Future<Boolean> check(Long id) {
    return new AsyncResult<>(true);
}
```

**CompletableFuture 组合（异步编排）：**

```java
CompletableFuture<StockInfo> stockFuture = loadStock(10086);
CompletableFuture<PriceInfo> priceFuture = loadPrice(10086);

// 两个异步结果组合后处理
CompletableFuture.allOf(stockFuture, priceFuture)
        .thenRun(() -> render(stockFuture.join(), priceFuture.join()));
```

> ⚠️ **注意**：`@Async` 返回的 CompletableFuture 由 Spring 包装（AsyncResult 桥接）——**不要在 @Async 方法内部自己 `complete()`**；方法返回的 future 会自动完成。若内部用 `CompletableFuture.supplyAsync(...)` 再包一层，则内层线程不走 Spring 线程池。

## 4. 异步异常处理与回调

### 4.1 三种异常路径

| 场景 | 异常去向 | 处理方式 |
|------|---------|---------|
| `CompletableFuture` 返回 | future 持有异常 | `whenComplete`/`exceptionally` 捕获 |
| `void` 方法 | `AsyncUncaughtExceptionHandler` | 全局 handler 记录+告警（见 2.2） |
| `Future` 返回 | `future.get()` 时抛 ExecutionException | 调用方处理 |

```java
// CompletableFuture 的异常处理（推荐：业务感知）
@Async
public CompletableFuture<Void> sendNotify(Long orderId) {
    try {
        doSend(orderId);
        return CompletableFuture.completedFuture(null);
    } catch (Exception e) {
        return CompletableFuture.failedFuture(e);   // 异常留给调用方处理
    }
}

// 调用侧
sendNotify(1001)
    .exceptionally(ex -> { log.error("notify failed", ex); return null; });
```

> ⚠️ **静默吞异常**：void 方法 + 未配置 `AsyncUncaughtExceptionHandler` → 异常只打默认日志（甚至无日志），业务无感知。**生产必须配全局 handler + 告警**。

### 4.2 事务与 SecurityContext 的传播

| 上下文 | 默认行为 | 传播方案 |
|--------|---------|---------|
| @Transactional | **不传播**（新线程无事务） | 异步方法内部自开事务（@Transactional 在异步方法上） |
| SecurityContext | **不传播**（ThreadLocal） | `DelegatingSecurityContextAsyncTaskExecutor`（Spring Security 体系） |
| RequestAttributes | **不传播** | `DelegatingSecurityContextRunnable` 或手动传递 |
| MDC（日志链路） | 不传播 | Logback MDC + TaskDecorator 包装 |

```java
// TaskDecorator：统一包装线程上下文（MDC/租户/请求头）
executor.setTaskDecorator(runnable -> {
    Map<String, String> mdc = MDC.getCopyOfContextMap();
    String tenant = TenantContext.get();
    return () -> {
        MDC.setContextMap(mdc);
        TenantContext.set(tenant);
        try { runnable.run(); }
        finally { MDC.clear(); TenantContext.clear(); }
    };
});
```

> 🎯 **要点**：异步 = 新线程 = 一切 ThreadLocal 上下文归零。**MDC、租户、安全上下文必须显式传播**（TaskDecorator 是统一入口）——"异步后日志链路断、权限丢失、租户串号"都是没做传播。

## 5. 异步场景清单：何时该用何时不该用

| 适合异步 | 不适合异步 |
|---------|-----------|
| 短信/邮件/推送通知 | 要求同步返回结果的接口 |
| 报表生成/大数据量导出 | 强一致性的写操作（需落库确认） |
| 日志/埋点上报 | 事务边界内必须同步完成的动作 |
| 多源数据并行加载（编排） | 简单直接调用（异步反而增加复杂度） |
| 定时任务内的耗时业务 | 任务少且执行快的场景 |

**异步的取舍原则：**

```text
收益 = 释放调用线程（响应更快）    成本 = 复杂度 + 一致性窗口
用异步的三个前提：
  ① 不要求调用方等待结果（或可编排）
  ② 失败可补偿/可重试（或可告警人工处理）
  ③ 线程池有容量设计（不会打满拖垮主流程）
```

> 🎯 **核心要点**：@Async = 代理切到线程池执行。生效三条件（@EnableAsync + 外部代理调用 + 非 private）、返回值三形态（void/Future/CompletableFuture）、异常三路径（future 持有/全局 handler/调用方捕获）——每一条都对"异步出问题"的排障路径。上下文传播（MDC/安全/事务）与容量设计（拒绝策略）是生产红线。

---

**上一模块**：[03-动态定时任务：SchedulingConfigurer与Trigger](03-动态定时任务：SchedulingConfigurer与Trigger.md)　**下一模块**：[05-线程池配置与性能调优](05-线程池配置与性能调优.md)
