# 11 — CompletableFuture异步编程
> 定位：掌握CompletableFuture的创建（supplyAsync/runAsync）、链式组合（thenApply/thenCompose/thenCombine）、异常处理（exceptionally/handle/whenComplete）及多任务编排（allOf/anyOf）实战

## 目录
1. [异步编程概述与Future的局限](#1-异步编程概述与future的局限)
2. [CompletableFuture的创建方式](#2-completablefuture的创建方式)
3. [任务完成后的处理：thenApply/thenAccept/thenRun](#3-任务完成后的处理thenapplythenacceptthenrun)
4. [任务组合：thenCompose与thenCombine](#4-任务组合thencompose与thencombine)
5. [双任务竞速：applyToEither/acceptEither/runAfterEither](#5-双任务竞速applytoeitheraccepteitherrunaftereither)
6. [异常处理：exceptionally/handle/whenComplete](#6-异常处理exceptionallyhandlewhencomplete)
7. [多任务编排：allOf与anyOf](#7-多任务编排allof与anyof)
8. [自定义线程池与异步任务](#8-自定义线程池与异步任务)
9. [CompletableFuture API速查表](#9-completablefuture-api速查表)
10. [实战：电商多服务并行调用](#10-实战电商多服务并行调用)
11. [面试高频考点](#11-面试高频考点)

---

## 1. 异步编程概述与Future的局限

### 1.1 什么是异步编程

异步编程允许任务在后台执行而不阻塞当前线程，任务完成后通过回调通知结果。Java异步编程的演进：

| 阶段 | 代表技术 | 特点 |
|------|---------|------|
| 原始阶段 | Thread + Runnable | 手动创建线程，无返回值 |
| 回调阶段 | ExecutorService + Future | 有返回值，但get()阻塞 |
| 增强阶段 | FutureTask + Callable | 可取消，可判断完成状态 |
| **声明式阶段** | **CompletableFuture (JDK 8+)** | 声明式链式调用，非阻塞回调，任务编排 |

### 1.2 Future的四大局限

```java
/**
 * Future 的核心缺陷
 */
public class FutureLimitations {
    public static void main(String[] args) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(3);

        // 问题1: get() 阻塞主线程
        Future<String> future = executor.submit(() -> {
            Thread.sleep(2000); return "Result";
        });
        String result = future.get(); // 主线程阻塞 2 秒！

        // 问题2: 无法链式组合 — 不能做到 future.thenApply(...).thenAccept(...)

        // 问题3: 多个 Future 难以编排 — 没有内置 allOf/anyOf
        Future<Integer> f1 = executor.submit(() -> 100);
        Future<Integer> f2 = executor.submit(() -> 200);
        // 只能手动轮询: while(!f1.isDone() || !f2.isDone()) { ... }

        // 问题4: 异常处理只能 try-catch，没有回调式异常处理
        Future<Integer> risky = executor.submit(() -> 1 / 0);
        try { risky.get(); } catch (ExecutionException e) { /* 只能捕获包装异常 */ }

        executor.shutdown();
    }
}
```

> ⚠️ Future的get()是阻塞调用，高并发场景浪费线程资源。多个Future的依赖关系需手动编排，代码冗长。

### 1.3 CompletableFuture核心优势

| 特性 | Future | CompletableFuture |
|------|--------|-------------------|
| 结果获取 | get() 阻塞 | 回调通知，非阻塞 |
| 链式调用 | 不支持 | thenApply / thenCompose 等 |
| 任务组合 | 手动轮询 | allOf / anyOf / thenCombine |
| 异常处理 | try-catch | exceptionally / handle / whenComplete |
| 手动完成 | 不支持 | complete() / completeExceptionally() |
| 线程池控制 | 固定 | 可指定/默认ForkJoinPool |

---

## 2. CompletableFuture的创建方式

### 2.1 四种创建方式

```java
public class CreateCompletableFuture {
    public static void main(String[] args) throws Exception {
        // 方式1: 已完成的 Future（测试/默认值）
        CompletableFuture<String> completed =
                CompletableFuture.completedFuture("Hello");
        System.out.println(completed.get());

        // 方式2: supplyAsync — 有返回值（最常用）
        CompletableFuture<String> supply = CompletableFuture.supplyAsync(() -> {
            sleep(1000); return "Task Result";
        });
        System.out.println(supply.get());

        // 方式3: runAsync — 无返回值
        CompletableFuture<Void> run = CompletableFuture.runAsync(() -> {
            sleep(500); System.out.println("Runnable executed");
        });
        run.get();

        // 方式4: 手动创建并complete
        CompletableFuture<String> manual = new CompletableFuture<>();
        new Thread(() -> { sleep(1000); manual.complete("Manual"); }).start();
        System.out.println(manual.get());
    }
    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### 2.2 supplyAsync vs runAsync

| 方法 | 参数 | 返回值 | 适用场景 |
|------|------|--------|---------|
| `supplyAsync(Supplier<U>)` | Supplier（有返回值） | `CompletableFuture<U>` | 需要计算结果的异步任务 |
| `runAsync(Runnable)` | Runnable（无返回值） | `CompletableFuture<Void>` | 仅执行操作，不关心返回值 |

> 💡 `completedFuture()` 常用于快速失败或默认值场景，例如缓存命中直接返回，无需异步执行。

---

## 3. 任务完成后的处理：thenApply/thenAccept/thenRun

### 3.1 三种回调对比

```java
public class CallbackComparison {
    public static void main(String[] args) {
        CompletableFuture<String> future =
                CompletableFuture.supplyAsync(() -> "Hello");

        // 1. thenApply: 转换结果（Function, 有返回值）
        CompletableFuture<Integer> applied = future.thenApply(String::length);

        // 2. thenAccept: 消费结果（Consumer, 无返回值）
        CompletableFuture<Void> accepted = future.thenAccept(
                s -> System.out.println("Accepted: " + s));

        // 3. thenRun: 执行动作（Runnable, 不关心结果）
        CompletableFuture<Void> run = future.thenRun(
                () -> System.out.println("Ran (no result)"));

        System.out.println("Length: " + applied.join());
        accepted.join();
        run.join();
    }
}
```

### 3.2 链式调用实战：订单处理流水线

```java
public class OrderPipeline {
    public static void main(String[] args) {
        CompletableFuture.supplyAsync(() -> fetchUser(1001))
                .thenApply(user -> fetchOrders(user))
                .thenApply(orders -> calculateTotal(orders))
                .thenAccept(total -> System.out.println("Total: " + total))
                .exceptionally(ex -> { System.err.println("Error: " + ex); return null; })
                .join();
    }
    static String fetchUser(int id) { sleep(500); return "User-" + id; }
    static String fetchOrders(String user) { sleep(500); return "Orders for " + user; }
    static int calculateTotal(String orders) { return orders.length() * 10; }
    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### 3.3 同步 vs 异步变体

每个回调方法有三种变体：

| 方法 | 执行线程 | 使用场景 |
|------|---------|---------|
| `thenApply(fn)` | 前一个任务的线程 | 快速操作，不阻塞 |
| `thenApplyAsync(fn)` | 默认ForkJoinPool | 耗时操作，需异步 |
| `thenApplyAsync(fn, Executor)` | 自定义线程池 | 指定线程池执行 |

> 💡 轻量操作（如简单类型转换）用同步变体，避免线程切换开销。IO或耗时计算务必用Async变体。

---

## 4. 任务组合：thenCompose与thenCombine

### 4.1 thenCompose — 展平嵌套（flatMap）

当异步任务返回另一个CompletableFuture时，用`thenCompose`避免嵌套：

```java
public class ThenComposeDemo {
    public static void main(String[] args) {
        // ❌ thenApply 产生嵌套: CompletableFuture<CompletableFuture<String>>
        CompletableFuture<CompletableFuture<String>> nested =
                CompletableFuture.supplyAsync(() -> "Token-123")
                        .thenApply(token -> fetchUser(token));

        // ✅ thenCompose 展平: CompletableFuture<String>
        CompletableFuture<String> flat =
                CompletableFuture.supplyAsync(() -> "Token-456")
                        .thenCompose(token -> fetchUser(token));
        System.out.println(flat.join());
    }
    static CompletableFuture<String> fetchUser(String token) {
        return CompletableFuture.supplyAsync(() -> "User for " + token);
    }
}
```

### 4.2 thenCombine — 合并独立任务

```java
public class ThenCombineDemo {
    public static void main(String[] args) {
        // 两个独立并行任务
        CompletableFuture<String> userFuture =
                CompletableFuture.supplyAsync(() -> { sleep(800); return "ZhangSan"; });
        CompletableFuture<String> productFuture =
                CompletableFuture.supplyAsync(() -> { sleep(600); return "MacBook"; });

        // 合并结果
        String result = userFuture.thenCombine(
                productFuture, (user, product) -> user + " ordered " + product
        ).join();
        System.out.println(result); // ZhangSan ordered MacBook
    }
    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### 4.3 thenCompose vs thenCombine

| 方法 | 关系 | 类比Stream | 适用场景 |
|------|------|-----------|---------|
| `thenCompose` | **依赖**：B依赖A的结果 | `flatMap()` | 先登录再查询订单 |
| `thenCombine` | **独立并行**：A和B同时执行 | `zip()` | 同时查用户和商品 |
| `thenApply` | **转换**：A的结果转成B | `map()` | 用户ID转User对象 |

> 💡 **口诀**："依赖用compose，独立用combine"。

---

## 5. 双任务竞速：applyToEither/acceptEither/runAfterEither

### 5.1 三种竞速方法

```java
public class EitherCompletionDemo {
    public static void main(String[] args) {
        CompletableFuture<String> fast = CompletableFuture.supplyAsync(
                () -> { sleep(500); return "Fast Result"; });
        CompletableFuture<String> slow = CompletableFuture.supplyAsync(
                () -> { sleep(1000); return "Slow Result"; });

        // applyToEither: 取先完成的转换
        String winner = fast.applyToEither(slow,
                result -> "Processed: " + result).join();
        System.out.println(winner);

        // acceptEither: 取先完成的消费（无返回值）
        fast.acceptEither(slow, r -> System.out.println("Got: " + r)).join();

        // runAfterEither: 任一完成就执行动作
        fast.runAfterEither(slow, () -> System.out.println("One done")).join();
    }
    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### 5.2 应用场景：缓存 vs 数据库竞速

```java
/**
 * 多数据源查询：缓存 vs 数据库，谁快用谁
 */
public class CacheVsDbRace {
    public static void main(String[] args) {
        CompletableFuture<String> cache = CompletableFuture.supplyAsync(
                () -> { sleep(200); return "Cached Data"; });      // 200ms
        CompletableFuture<String> db = CompletableFuture.supplyAsync(
                () -> { sleep(800); return "DB Data"; });          // 800ms

        String result = cache.applyToEither(db, data -> {
            System.out.println("Source: " + (data.equals("Cached Data") ? "Cache" : "DB"));
            return data;
        }).join();
        System.out.println("Result: " + result);
    }
    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

---

## 6. 异常处理：exceptionally/handle/whenComplete

### 6.1 三种处理方式

```java
public class ExceptionHandlingDemo {
    public static void main(String[] args) {
        // 1. exceptionally — 仅异常时触发，返回兜底值
        CompletableFuture.supplyAsync(() -> {
            if (Math.random() > 0.5) throw new RuntimeException("Oops!");
            return "Success";
        }).exceptionally(ex -> {
            System.err.println("exceptionally: " + ex.getMessage());
            return "Fallback";
        }).thenAccept(r -> System.out.println("Result1: " + r)).join();

        // 2. handle — 无论是否异常都触发，可恢复
        CompletableFuture.supplyAsync(() -> {
            if (Math.random() > 0.5) throw new RuntimeException("Error!");
            return "OK";
        }).handle((result, ex) -> {
            if (ex != null) { System.err.println("handle: " + ex.getMessage()); return "Recovered"; }
            return "Handled: " + result;
        }).thenAccept(r -> System.out.println("Result2: " + r)).join();

        // 3. whenComplete — 总是触发，不改变结果（适合清理/日志）
        CompletableFuture.supplyAsync(() -> {
            if (Math.random() > 0.5) throw new RuntimeException("Boom!");
            return "Data";
        }).whenComplete((result, ex) -> {
            System.out.println("whenComplete: result=" + result + ", ex="
                    + (ex != null ? ex.getMessage() : "null"));
            // ⚠️ 不能修改返回值！
        }).join();
    }
}
```

### 6.2 异常处理机制对比

| 方法 | 参数 | 能否恢复 | 触发条件 |
|------|------|---------|---------|
| `exceptionally(fn)` | `Function<Throwable, T>` | ✅ 返回兜底值 | **仅**异常时 |
| `handle(fn)` | `BiFunction<T, Throwable, T>` | ✅ 处理异常 | 总是触发 |
| `whenComplete(c)` | `BiConsumer<T, Throwable>` | ❌ 不影响结果 | 总是触发 |

### 6.3 异常传播链

```java
/**
 * 异常沿调用链向下传播直到被处理
 * 
 * supplyAsync → thenApply → thenApply → handle
 *    ⬇ 异常     ⬇ 跳过     ⬇ 跳过     ⬇ 处理
 *   抛出异常    不执行      不执行     捕获恢复 → 后续链继续执行
 */
public class ExceptionPropagation {
    public static void main(String[] args) {
        CompletableFuture.supplyAsync(() -> { System.out.println("Stage 1"); return "100"; })
                .thenApply(s -> { System.out.println("Stage 2"); return Integer.parseInt(s); })
                .thenApply(n -> { System.out.println("Stage 3"); return n * 2; })
                .exceptionally(ex -> { System.out.println("Caught: " + ex.getMessage()); return -1; })
                .thenApply(n -> { System.out.println("Stage 4: recovered=" + n); return n; })
                .join();
    }
}
```

> ⚠️ `whenComplete` 不会改变结果值。需要修改结果用 `handle`。`whenComplete` 适合做资源清理和日志记录。

---

## 7. 多任务编排：allOf与anyOf

### 7.1 allOf — 等待所有任务完成

```java
public class AllOfDemo {
    public static void main(String[] args) {
        CompletableFuture<String> t1 = CompletableFuture.supplyAsync(
                () -> { sleep(800); return "Task1"; });
        CompletableFuture<String> t2 = CompletableFuture.supplyAsync(
                () -> { sleep(500); return "Task2"; });
        CompletableFuture<String> t3 = CompletableFuture.supplyAsync(
                () -> { sleep(300); return "Task3"; });

        long start = System.currentTimeMillis();
        CompletableFuture.allOf(t1, t2, t3).join(); // 等待全部完成
        System.out.println("All done in " + (System.currentTimeMillis() - start) + "ms");

        // allOf 不保留各任务结果，需手动收集
        String combined = java.util.stream.Stream.of(t1, t2, t3)
                .map(CompletableFuture::join)
                .collect(java.util.stream.Collectors.joining(", "));
        System.out.println("Results: " + combined);
    }
    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
```

### 7.2 allOf 结果收集工具方法

```java
/**
 * 将 List<CompletableFuture<T>> 聚合为 CompletableFuture<List<T>>
 */
public class AllOfCollector {
    public static <T> CompletableFuture<java.util.List<T>> sequence(
            java.util.List<CompletableFuture<T>> futures) {
        return CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(java.util.stream.Collectors.toList()));
    }

    public static void main(String[] args) {
        java.util.List<CompletableFuture<String>> futures = new java.util.ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            int id = i;
            futures.add(CompletableFuture.supplyAsync(() -> {
                sleep((long)(Math.random() * 500));
                return "Task-" + id;
            }));
        }
        System.out.println("All: " + sequence(futures).join());
    }
    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
```

### 7.3 anyOf — 任意一个完成即返回

```java
public class AnyOfDemo {
    public static void main(String[] args) {
        CompletableFuture<String> a = CompletableFuture.supplyAsync(
                () -> { sleep(800); return "Service A"; });
        CompletableFuture<String> b = CompletableFuture.supplyAsync(
                () -> { sleep(500); return "Service B"; });
        CompletableFuture<String> c = CompletableFuture.supplyAsync(
                () -> { sleep(300); return "Service C"; });

        long start = System.currentTimeMillis();
        Object fastest = CompletableFuture.anyOf(a, b, c).join();
        System.out.println("Fastest: " + fastest +
                " (" + (System.currentTimeMillis() - start) + "ms)");
    }
    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
```

### 7.4 allOf vs anyOf

| 方法 | 策略 | 返回类型 | 使用场景 |
|------|------|---------|---------|
| `allOf` | 全部完成 | `CompletableFuture<Void>`（手动收集） | 并行初始化、批量处理 |
| `anyOf` | 任一完成 | `CompletableFuture<Object>`（最快结果） | 服务竞速、缓存预加载、超时降级 |

### 7.5 JDK 8 兼容的超时控制

```java
/**
 * JDK 8 兼容方案：通过 applyToEither 实现超时
 * (JDK 9+ 原生支持 orTimeout / completeOnTimeout)
 */
public class TimeoutControl {
    public static <T> CompletableFuture<T> timeoutAfter(long timeout, TimeUnit unit) {
        CompletableFuture<T> result = new CompletableFuture<>();
        Executors.newSingleThreadScheduledExecutor()
                .schedule(() -> result.completeExceptionally(
                        new TimeoutException("Timeout")), timeout, unit);
        return result;
    }

    public static void main(String[] args) {
        CompletableFuture<String> slow = CompletableFuture.supplyAsync(() -> {
            sleep(2000); return "Slow";
        });
        CompletableFuture<String> wrapped = slow.applyToEither(
                timeoutAfter(1, TimeUnit.SECONDS), r -> r);
        try { System.out.println(wrapped.join()); }
        catch (Exception e) { System.out.println("Timed out: " + e.getCause().getMessage()); }
    }
    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
```

---

## 8. 自定义线程池与异步任务

### 8.1 默认线程池的问题

```java
/**
 * ForkJoinPool.commonPool() 的陷阱
 */
public class DefaultPoolPitfall {
    public static void main(String[] args) {
        // commonPool 并行度 = CPU核心数 - 1（如8核机器只有7个线程）
        System.out.println("Parallelism: " +
                ForkJoinPool.commonPool().getParallelism());

        // 问题1: IO密集型任务会很快耗尽线程（HTTP调用占线程等待）
        // 问题2: 与Parallel Stream共用线程池，互相影响
        // 问题3: 阻塞操作拖垮整个池
    }
}
```

### 8.2 自定义线程池最佳实践

```java
public class CustomThreadPoolConfig {
    // 业务专用的异步线程池 — 独立于 commonPool
    private static final ThreadPoolExecutor ASYNC_POOL =
            new ThreadPoolExecutor(
                    8,                          // corePoolSize
                    16,                         // maximumPoolSize（IO密集型可大些）
                    60, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(1000), // 有界队列防OOM
                    r -> { Thread t = new Thread(r); t.setName("async-" + t.getId());
                           t.setDaemon(true); return t; },
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );

    public static void main(String[] args) {
        CompletableFuture<String> future = CompletableFuture
                .supplyAsync(() -> {
                    System.out.println("Thread: " + Thread.currentThread().getName());
                    return "Result";
                }, ASYNC_POOL);

        future.thenApplyAsync(String::toUpperCase, ASYNC_POOL)
              .thenAcceptAsync(System.out::println, ASYNC_POOL)
              .join();

        // 监控
        System.out.printf("Active=%d, Queue=%d, Completed=%d%n",
                ASYNC_POOL.getActiveCount(), ASYNC_POOL.getQueue().size(),
                ASYNC_POOL.getCompletedTaskCount());
        ASYNC_POOL.shutdown();
    }
}
```

### 8.3 线程池配置建议

| 任务类型 | 核心线程数 | 最大线程数 | 队列策略 |
|---------|-----------|-----------|---------|
| CPU密集型 | 核心数+1 | 核心数*2 | 有界队列 |
| IO密集型 | 核心数*2 | 核心数*10 | 有界队列 |
| 混合型 | 核心数*2 | 动态调整 | 有界队列 |

> ⚠️ **禁止**使用`Executors.newCachedThreadPool()`或`Executors.newFixedThreadPool()`的默认工厂——无界队列或无限线程可能造成OOM。务必通过`ThreadPoolExecutor`手动创建。

---

## 9. CompletableFuture API速查表

### 9.1 工厂方法（创建）

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `completedFuture(T value)` | `CompletableFuture<T>` | 创建已完成的Future |
| `supplyAsync(Supplier<U>)` | `CompletableFuture<U>` | 异步执行，有返回值 |
| `supplyAsync(Supplier<U>, Executor)` | `CompletableFuture<U>` | 指定线程池异步执行 |
| `runAsync(Runnable)` | `CompletableFuture<Void>` | 异步执行，无返回值 |
| `runAsync(Runnable, Executor)` | `CompletableFuture<Void>` | 指定线程池执行 |

### 9.2 回调与转换

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `thenApply(fn)` | `Function<T, U>` | `CompletableFuture<U>` | 转换结果（有返回值） |
| `thenAccept(consumer)` | `Consumer<T>` | `CompletableFuture<Void>` | 消费结果（无返回值） |
| `thenRun(action)` | `Runnable` | `CompletableFuture<Void>` | 执行动作（不关心结果） |

### 9.3 组合方法

| 方法 | 参数 | 说明 | 类比 |
|------|------|------|------|
| `thenCompose(fn)` | `Function<T, CompletionStage<U>>` | 依赖组合（flatMap） | `flatMap()` |
| `thenCombine(other, fn)` | `CompletionStage, BiFunction` | 独立组合（zip） | `zip()` |
| `thenAcceptBoth(other, c)` | `CompletionStage, BiConsumer` | 消费两个结果 | — |
| `runAfterBoth(other, a)` | `CompletionStage, Runnable` | 都完成后执行 | — |

### 9.4 竞速方法

| 方法 | 参数 | 说明 |
|------|------|------|
| `applyToEither(other, fn)` | `CompletionStage, Function` | 谁快用谁的结果转换 |
| `acceptEither(other, c)` | `CompletionStage, Consumer` | 谁快消费谁的结果 |
| `runAfterEither(other, a)` | `CompletionStage, Runnable` | 任一完成就执行 |

### 9.5 多任务编排

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `allOf(CompletableFuture<?>...)` | `CompletableFuture<Void>` | 全部完成（手动收集结果） |
| `anyOf(CompletableFuture<?>...)` | `CompletableFuture<Object>` | 任一完成（取最快结果） |

### 9.6 异常处理

| 方法 | 触发条件 | 能否恢复 |
|------|---------|---------|
| `exceptionally(fn)` | 仅异常 | ✅ 返回兜底值 |
| `handle(fn)` | 总是 | ✅ |
| `whenComplete(c)` | 总是 | ❌ 不影响结果 |

### 9.7 结果获取与手动控制

| 方法 | 是否阻塞 | 说明 |
|------|---------|------|
| `get()` | ✅ 阻塞 | 获取结果，抛受检异常 |
| `get(timeout, unit)` | ✅ 阻塞 | 超时获取结果 |
| `join()` | ✅ 阻塞 | 获取结果，抛非受检异常（推荐） |
| `getNow(T defaultValue)` | ❌ 不阻塞 | 未完成则返回默认值 |
| `complete(T value)` | ❌ | 手动成功完成 |
| `completeExceptionally(ex)` | ❌ | 手动异常完成 |
| `cancel(mayInterrupt)` | ❌ | 取消任务 |
| `isDone()` | ❌ | 是否完成 |

> 💡 生产环境推荐用 `join()` 替代 `get()`，因为 `join()` 抛 `CompletionException`（非受检），无需方法签名声明。

---

## 10. 实战：电商多服务并行调用

```java
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.List;
import java.util.ArrayList;

/**
 * 电商下单：并行调用多个微服务 + 结果聚合
 *
 * ┌────────── 用户请求 ──────────┐
 * │         ↓ 并行执行            │
 * │  ┌──────┬──────┬──────┬──────┐│
 * │  │用户  │库存  │优惠券│物流  ││
 * │  │200ms │300ms │500ms │400ms ││
 * │  └──────┴──────┴──────┴──────┘│
 * │         ↓ 结果聚合             │
 * │      ┌──────────┐            │
 * │      │  Order   │            │
 * │      └──────────┘            │
 * └──────────────────────────────┘
 */
public class ECommerceParallelDemo {

    private static final ThreadPoolExecutor ASYNC_POOL =
            new ThreadPoolExecutor(
                    8, 16, 60, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(200),
                    r -> { Thread t = new Thread(r);
                           t.setName("ecom-" + t.getId());
                           t.setDaemon(true); return t; },
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();

        // 1. 并行调用各微服务
        CompletableFuture<UserInfo> userFuture = getUserInfo(1001L);
        CompletableFuture<Inventory> inventoryFuture = checkInventory("SKU-888");
        CompletableFuture<Coupon> couponFuture = getBestCoupon(1001L);
        CompletableFuture<Shipping> shippingFuture = estimateShipping("收货地址XXX");

        // 2. 等待全部完成并聚合
        Order order = CompletableFuture
                .allOf(userFuture, inventoryFuture, couponFuture, shippingFuture)
                .thenApplyAsync(v -> aggregateOrder(
                        userFuture.join(),
                        inventoryFuture.join(),
                        couponFuture.join(),
                        shippingFuture.join()
                ), ASYNC_POOL)
                .get(5, TimeUnit.SECONDS);

        System.out.println("=== Order Created ===");
        System.out.println(order);
        System.out.println("Total time: " + (System.currentTimeMillis() - start) + "ms");
        ASYNC_POOL.shutdown();
    }

    // ---- 模拟微服务调用 ----
    static CompletableFuture<UserInfo> getUserInfo(Long userId) {
        return async("UserService", 200,
                () -> new UserInfo(userId, "ZhangSan", "VIP"));
    }
    static CompletableFuture<Inventory> checkInventory(String sku) {
        return async("InventoryService", 300,
                () -> new Inventory(sku, true, 99.00));
    }
    static CompletableFuture<Coupon> getBestCoupon(Long userId) {
        return async("CouponService", 500,
                () -> new Coupon("DISCOUNT-10", 10.0, 100.0));
    }
    static CompletableFuture<Shipping> estimateShipping(String address) {
        return async("ShippingService", 400,
                () -> new Shipping(address, "SF-Express", 15.0, "预计3天到达"));
    }

    private static <T> CompletableFuture<T> async(
            String name, long delay, Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            long s = System.currentTimeMillis();
            try { Thread.sleep(delay); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            T result = supplier.get();
            System.out.printf("[%s] done in %dms%n", name,
                    System.currentTimeMillis() - s);
            return result;
        }, ASYNC_POOL);
    }

    private static Order aggregateOrder(UserInfo u, Inventory i, Coupon c, Shipping s) {
        double discount = (i.price >= c.minAmount) ? c.discount : 0;
        double total = i.price - discount + s.fee;
        return new Order("ORDER-" + System.currentTimeMillis(), u, i.sku,
                total, c.code, s.method, s.estimate);
    }

    // ---- 数据类 ----
    record UserInfo(Long id, String name, String level) {}
    record Inventory(String sku, boolean inStock, double price) {}
    record Coupon(String code, double discount, double minAmount) {}
    record Shipping(String address, String method, double fee, String estimate) {}
    record Order(String orderId, UserInfo user, String sku, double total,
                 String coupon, String shippingMethod, String estimate) {
        @Override public String toString() {
            return String.format("Order[%s, user=%s, sku=%s, total=%.2f, coupon=%s, ship=%s]",
                    orderId, user.name(), sku, total, coupon, shippingMethod);
        }
    }
}
```

> 🎯 **实战要点**：电商场景是CompletableFuture最经典的应用。核心模式：**allOf并行发起微服务调用 → thenApplyAsync聚合结果**。

---

## 11. 面试高频考点

### 基础问题

**Q1: CompletableFuture和Future的区别？**
- Future的get()阻塞，CompletableFuture回调通知非阻塞
- Future不支持组合编排，CompletableFuture支持allOf/anyOf/thenCombine
- Future异常靠try-catch，CompletableFuture支持exceptionally/handle/whenComplete
- CompletableFuture支持手动complete/completeExceptionally

**Q2: thenApply 和 thenCompose 的区别？**
- `thenApply`：T→U 映射转换（map），一层的转换
- `thenCompose`：当转换函数返回CompletableFuture时展平嵌套（flatMap），避免 `CF<CF<U>>`

**Q3: exceptionally / handle / whenComplete 的区别？**
- `exceptionally`：仅异常触发，**可恢复**，返回兜底值
- `handle`：总是触发，**可恢复**，同时处理结果和异常
- `whenComplete`：总是触发，**不能改变结果**，适合清理/日志

### 进阶问题

**Q4: allOf返回Void，如何收集所有子任务的结果？**
```java
// Stream 收集（此时join不阻塞，因为已全部完成）
List<String> results = Stream.of(f1, f2, f3)
        .map(CompletableFuture::join)
        .collect(Collectors.toList());
```

**Q5: CompletableFuture 默认用什么线程池？有何问题？**
- 默认使用 `ForkJoinPool.commonPool()`，并行度 = CPU核心数 - 1
- 问题：IO任务耗尽线程；与Parallel Stream互相影响；阻塞任务拖垮整个池
- **建议：业务场景必须自定义线程池**，通过 `supplyAsync(supplier, executor)` 传入

**Q6: JDK 8 如何实现 CompletableFuture 超时？**
```java
// 使用 applyToEither + 延迟异常 Future
CompletableFuture<String> timeout = slowService.applyToEither(
        timeoutAfter(1, TimeUnit.SECONDS), r -> r);
```

**Q7: thenApply 和 thenApplyAsync 的区别？**
- `thenApply`：在前一个任务线程同步执行（无线程切换），适合轻量操作
- `thenApplyAsync`：提交到线程池异步执行，适合IO/耗时操作

**Q8: 异常在链中如何传播？**
- 异常沿调用链向下传播，后续同步回调（thenApply/thenAccept）全部跳过
- 直到遇到 `exceptionally` 或 `handle` 恢复
- 恢复后后续链式调用继续正常执行

### 深度问题

**Q9: 如何实现 CompletableFuture 的重试机制？**
```java
public static <T> CompletableFuture<T> retry(
        Supplier<CompletableFuture<T>> supplier, int maxRetries) {
    CompletableFuture<T> cf = supplier.get();
    for (int i = 0; i < maxRetries; i++) {
        cf = cf.exceptionally(ex -> null)
              .thenCompose(ignored -> supplier.get());
    }
    return cf;
}
```

**Q10: 大量 CompletableFuture 如何控制并发度？**
```java
Semaphore semaphore = new Semaphore(10); // 最多10并发
CompletableFuture.supplyAsync(() -> {
    semaphore.acquire();
    try { return doHeavyWork(); }
    finally { semaphore.release(); }
});
```

> 🎯 **面试重点**：三种回调对比、thenCompose vs thenCombine、allOf结果收集、自定义线程池必要性、异常处理机制。能手写电商并行调用demo是加分项。

---

## 总结

CompletableFuture 解决了传统Future的四大痛点：**阻塞等待**（回调驱动）、**无法组合**（丰富编排API）、**异常处理笨重**（声明式处理）、**线程池不可控**（灵活指定）。掌握了创建、链式调用、组合编排、异常处理和线程池定制这五大能力，就能在实战中熟练运用声明式异步编程。

> **学习路径**：创建和基础回调（supplyAsync/thenApply/thenAccept）→ 任务组合（thenCompose/thenCombine）→ 异常处理 → 多任务编排（allOf/anyOf）→ 电商案例整合

---

*最后更新: 2026-07-26 | 适用于 JDK 8/11/17/21*
