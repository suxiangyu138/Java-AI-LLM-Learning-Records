# 05 Lambda 实战模式与 Stream 协同

> 一句话定位：把 Lambda 用在策略、模板、责任链和异步编排等真实边界上，同时控制副作用、异常、线程安全与可读性。

---

## 📚 目录

1. [Lambda 的工程使用边界](#1-lambda-的工程使用边界)
2. [策略模式与规则表](#2-策略模式与规则表)
3. [Execute Around 模板模式](#3-execute-around-模板模式)
4. [责任链与观察者](#4-责任链与观察者)
5. [Supplier 惰性计算与重试](#5-supplier-惰性计算与重试)
6. [与 Stream 协同](#6-与-stream-协同)
7. [与 Optional 和 CompletableFuture 协同](#7-与-optional-和-completablefuture-协同)
8. [副作用、并发与性能](#8-副作用并发与性能)
9. [业务重构清单与综合案例](#9-业务重构清单与综合案例)

---

## 1. Lambda 的工程使用边界

Lambda 最适合表达短小、局部、可命名为一个动作的行为。业务代码中不要把十几行分支、事务和异常处理全部塞进箭头函数。

| 适合 Lambda | 更适合命名方法/类 |
|---|---|
| 集合过滤、映射、排序 | 复杂领域规则 |
| 一次性回调 | 需要独立测试的核心算法 |
| 策略注册 | 需要多个协作字段的对象 |
| 延迟工厂 | 长流程事务编排 |
| 组合简单校验器 | 跨模块公共能力 |

```java
// 好：动作短，变量名表达意图
users.stream()
        .filter(User::enabled)
        .map(User::toView)
        .toList();

// 复杂逻辑应提取为命名方法
private boolean eligibleForPromotion(User user, Instant now) {
    return user.enabled() && user.registeredAt().plus(30, ChronoUnit.DAYS).isBefore(now);
}
```

> 🎯 代码短不等于设计好；Lambda 的价值是把变化点作为参数传入，而不是隐藏业务复杂度。

---

## 2. 策略模式与规则表

### 2.1 用函数注册策略

```java
enum Channel { APP, WEB, PARTNER }

record Order(String id, Channel channel, BigDecimal amount) {}

class ShippingService {
    private final Map<Channel, Function<Order, BigDecimal>> prices = Map.of(
            Channel.APP, order -> order.amount().compareTo(BigDecimal.valueOf(100)) >= 0
                    ? BigDecimal.ZERO : BigDecimal.TEN,
            Channel.WEB, order -> order.amount().multiply(new BigDecimal("0.05")),
            Channel.PARTNER, order -> BigDecimal.valueOf(8)
    );

    BigDecimal calculate(Order order) {
        Function<Order, BigDecimal> strategy = prices.get(order.channel());
        if (strategy == null) {
            throw new IllegalArgumentException("unsupported channel: " + order.channel());
        }
        return strategy.apply(order);
    }
}
```

策略表比大量 `if/else` 更容易增加新渠道，但策略键和缺省行为必须明确。若每个策略都需要配置、依赖和审计信息，应升级为实现接口的领域对象。

### 2.2 规则链

```java
record DiscountContext(BigDecimal amount, boolean member, boolean newUser) {}

List<UnaryOperator<DiscountContext>> rules = List.of(
        context -> context.member()
                ? new DiscountContext(context.amount().multiply(new BigDecimal("0.90")), true, context.newUser())
                : context,
        context -> context.newUser()
                ? new DiscountContext(context.amount().subtract(BigDecimal.TEN), context.member(), true)
                : context
);

UnaryOperator<DiscountContext> combined = rules.stream()
        .reduce(UnaryOperator.identity(),
                (first, second) -> value -> second.apply(first.apply(value)));
DiscountContext result = combined.apply(
        new DiscountContext(BigDecimal.valueOf(200), true, false));
```

规则需要顺序、可观测性和失败处理时，建议使用 `Rule` 接口并给每条规则配置名称，而不是只保存匿名 Lambda。

---

## 3. Execute Around 模板模式

### 3.1 把固定流程包起来

```java
@FunctionalInterface
interface SqlWork<T> {
    T execute(Connection connection) throws SQLException;
}

<T> T withConnection(DataSource dataSource, SqlWork<T> work) throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
        connection.setAutoCommit(false);
        try {
            T result = work.execute(connection);
            connection.commit();
            return result;
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } catch (RuntimeException e) {
            connection.rollback();
            throw e;
        }
    }
}

// 调用方只描述变化部分
User user = withConnection(dataSource, connection -> loadUser(connection, id));
```

模板方法统一资源、事务和日志，Lambda 只负责业务动作。生产代码应根据项目异常类型完善回滚失败、连接关闭异常和重试边界。

### 3.2 文件处理模板

```java
@FunctionalInterface
interface ReaderAction<T> {
    T apply(BufferedReader reader) throws IOException;
}

static <T> T readFile(Path path, ReaderAction<T> action) throws IOException {
    try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
        return action.apply(reader);
    }
}

List<String> lines = readFile(path, reader -> reader.lines().toList());
```

不要在模板中吞异常；资源生命周期应由模板拥有，Lambda 不应擅自关闭传入资源。

---

## 4. 责任链与观察者

### 4.1 责任链

```java
@FunctionalInterface
interface Handler<T> {
    Optional<T> handle(T input);
}

static <T> Function<T, Optional<T>> firstMatch(List<Handler<T>> handlers) {
    return input -> handlers.stream()
            .map(handler -> handler.handle(input))
            .filter(Optional::isPresent)
            .findFirst()
            .orElseGet(Optional::empty);
}
```

每个处理器应明确“未处理”和“处理失败”的区别。若必须停止传播、记录原因或异步处理，用带结果状态的类型代替裸 `Optional`。

### 4.2 观察者回调

```java
class EventBus<T> {
    private final CopyOnWriteArrayList<Consumer<T>> listeners = new CopyOnWriteArrayList<>();

    void subscribe(Consumer<T> listener) {
        listeners.add(Objects.requireNonNull(listener));
    }

    void publish(T event) {
        listeners.forEach(listener -> listener.accept(event));
    }
}
```

回调集合需要定义线程安全、重复订阅、异常隔离和注销策略。一个监听器抛异常是否阻断其他监听器，应由 `publish` 的契约决定。

```java
void publishIsolated(T event) {
    listeners.forEach(listener -> {
        try {
            listener.accept(event);
        } catch (RuntimeException ex) {
            logger.warn("listener failed", ex);
        }
    });
}
```

异常隔离不是无条件吞异常；至少应记录、计数或交给错误处理器。

---

## 5. Supplier 惰性计算与重试

### 5.1 惰性默认值

```java
static Config loadDefaultConfig() {
    System.out.println("load default");
    return Config.defaultConfig();
}

Config config = Optional.ofNullable(loadFromEnvironment())
        .orElseGet(YourService::loadDefaultConfig);
```

`orElse` 的参数会先求值，`orElseGet` 的 Supplier 只在缺失时执行。昂贵计算、网络访问和对象创建应优先使用惰性形式。

### 5.2 可控重试包装器

```java
@FunctionalInterface
interface CheckedSupplier<T> {
    T get() throws Exception;
}

static <T> T retry(CheckedSupplier<T> action, int attempts, Duration delay)
        throws Exception {
    Exception last = null;
    for (int i = 1; i <= attempts; i++) {
        try {
            return action.get();
        } catch (Exception ex) {
            last = ex;
            if (i < attempts) {
                Thread.sleep(delay.toMillis());
            }
        }
    }
    throw last;
}
```

生产重试需要指数退避、最大延迟、可重试异常白名单、幂等性和取消机制；不能把所有异常都盲目重试。

---

## 6. 与 Stream 协同

### 6.1 流水线结构

```java
List<UserView> activeViews = users.stream()
        .filter(User::enabled)
        .map(User::toView)
        .sorted(Comparator.comparing(UserView::name))
        .toList();
```

中间操作通常惰性执行，终端操作才触发遍历。每个 Lambda 最好是无状态、无外部副作用的函数。

```java
Map<Channel, List<Order>> grouped = orders.stream()
        .collect(Collectors.groupingBy(Order::channel));
```

### 6.2 `map`、`flatMap` 与 `reduce`

```java
List<String> tags = users.stream()
        .flatMap(user -> user.tags().stream())
        .distinct()
        .toList();

BigDecimal total = orders.stream()
        .map(Order::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
```

`reduce` 的累加器和组合器应满足结合律，尤其是并行执行时；有副作用的累加器应改用合适的 Collector。

### 6.3 避免副作用收集

```java
// 不推荐：并行或重构时容易出现线程安全和顺序问题
List<String> names = new ArrayList<>();
users.forEach(user -> names.add(user.name()));

// 推荐：让 Stream 管理结果容器
List<String> safeNames = users.stream()
        .map(User::name)
        .toList();
```

`forEach` 更适合终端副作用（例如发送已准备好的通知），不应被当作通用的可变累加器。

---

## 7. 与 Optional 和 CompletableFuture 协同

### 7.1 Optional 的函数式链

```java
String city = Optional.ofNullable(user)
        .map(User::address)
        .map(Address::city)
        .filter(cityName -> !cityName.isBlank())
        .orElse("unknown");
```

Optional 适合表达返回值可能缺失，不建议作为实体字段、序列化 DTO 字段或所有方法参数。

### 7.2 CompletableFuture 编排

```java
CompletableFuture<User> userFuture = CompletableFuture.supplyAsync(
        () -> userService.load(id), executor);

CompletableFuture<UserView> viewFuture = userFuture
        .thenApply(User::toView)
        .exceptionally(error -> {
            logger.error("load user failed", error);
            return UserView.fallback(id);
        });
```

| 方法 | 作用 | 是否扁平化 |
|---|---|:---:|
| `thenApply` | 同步转换结果 | 否 |
| `thenCompose` | 串联另一个 Future | 是 |
| `thenCombine` | 合并两个独立 Future | 不适用 |
| `handle` | 同时处理结果和异常 | 不适用 |

不要在 Lambda 中调用 `join()` 阻塞公共线程池；应尽量保持异步链，并为执行器、超时和取消建立边界。

```java
CompletableFuture<Profile> profile = userFuture.thenCompose(
        user -> profileService.loadAsync(user.id()));
```

---

## 8. 副作用、并发与性能

### 8.1 副作用清单

| 副作用 | 典型风险 | 建议 |
|---|---|---|
| 修改外部集合 | 并发修改、顺序依赖 | 使用 Collector 或线程安全容器 |
| 访问共享字段 | 可见性、竞态 | 明确同步/原子性 |
| 网络/磁盘 IO | 阻塞、重试风暴 | 在专用执行器执行 |
| 修改输入对象 | 调用方难预测 | 明确所有权或复制 |
| 日志和计数 | 重复执行、并行乱序 | 说明至少一次/顺序语义 |

### 8.2 `parallelStream` 不是免费加速

并行流适合数据规模足够大、任务独立且计算密集的场景。IO、共享可变状态、顺序敏感或小数据集通常不适合。

```java
// 只有在确认工作负载和线程模型后才考虑
long count = values.parallelStream()
        .mapToLong(this::cpuHeavyCalculation)
        .sum();
```

并行流默认使用公共 ForkJoinPool，可能与应用中其他任务争用资源；需要隔离执行器时应显式设计，而不是期待并行流自动理解业务线程池。

### 8.3 性能判断

捕获型 Lambda 需要携带捕获值，非捕获型可能被运行时复用；这些是实现细节，不应作为业务逻辑依赖。性能优化优先检查装箱、重复遍历、数据库访问和不必要对象创建，再用 JMH 验证微优化。

---

## 9. 业务重构清单与综合案例

### 9.1 重构前检查

1. 这个变化点是否真的是一个行为，而不是一组状态？
2. 是否可以用命名方法表达，而不是写长 Lambda？
3. 目标函数式接口是否准确表达异常、输入和输出？
4. Lambda 是否捕获了大对象或外部 `this`？
5. 是否依赖执行顺序、线程上下文或副作用？
6. 是否需要日志、指标、超时、重试和取消？
7. 是否会被 Stream/CompletableFuture 重复执行？
8. 是否有单元测试覆盖空值、异常和边界输入？

### 9.2 综合案例：可组合订单处理

```java
record ProcessResult(String orderId, boolean accepted, String reason) {}

Function<Order, ProcessResult> process = order -> {
    if (order.amount().signum() < 0) {
        return new ProcessResult(order.id(), false, "negative amount");
    }
    return new ProcessResult(order.id(), true, "accepted");
};

List<ProcessResult> results = orders.stream()
        .filter(Objects::nonNull)
        .map(process)
        .toList();
```

实际项目中，可将校验拆成命名规则、把外部 IO 放在服务边界，并通过指标记录拒绝原因：

```java
Map<String, Long> rejectionCounts = results.stream()
        .filter(result -> !result.accepted())
        .collect(Collectors.groupingBy(ProcessResult::reason, Collectors.counting()));
```

> 🎯 **模块结论**：Lambda 是“变化行为”的载体；Stream 是数据流水线，Optional 是缺失值表达，CompletableFuture 是异步编排。把状态、资源、异常和线程边界留在可命名的组件中，代码才可维护。

---

**上一模块**：[04-Lambda底层原理与字节码](./04-Lambda底层原理与字节码.md)  
**下一模块**：[06-Lambda面试高频题精讲](./06-Lambda面试高频题精讲.md)  
**返回总览**：[00-Lambda表达式知识体系总览](./00-Lambda表达式知识体系总览.md)
