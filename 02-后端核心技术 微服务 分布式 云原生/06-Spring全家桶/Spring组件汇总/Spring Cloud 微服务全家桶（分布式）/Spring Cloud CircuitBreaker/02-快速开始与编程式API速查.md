# 02 快速开始与编程式 API 速查

> 实操篇：依赖+最小配置、**`CircuitBreakerFactory` 编程式 API**（run/fallback 全形态）、响应式 API、Customizer 三种配置法、三步验证——SCCB 官方没有注解，**编程式 API 是主开发方式**（注解见 [05 篇](05-注解开发与降级速查.md)）。

---

## 📚 目录

1. [环境准备与依赖](#1-环境准备与依赖)
2. [最小可跑配置](#2-最小可跑配置)
3. [编程式 API 全形态](#3-编程式-api-全形态)
4. [响应式 API（WebFlux）](#4-响应式-apiwebflux)
5. [Customizer：Java 配置三形态](#5-customizerjava-配置三形态)
6. [配置优先级总图](#6-配置优先级总图)
7. [三步验证与排障](#7-三步验证与排障)

---

## 1. 环境准备与依赖

### 1.1 版本组合（2026-08 主线）

| 项 | 版本 |
|----|------|
| Spring Boot | **4.0.x/4.1.x** |
| Spring Cloud | **2025.1.x（Oakwood）** |
| Spring Cloud CircuitBreaker | **5.0.x**（BOM 锁定，不手写） |
| Resilience4J | 2.3.0（BOM 锁定） |

### 1.2 依赖清单

```xml
<!-- 父级版本由你的 Boot 项目已有配置决定，只需加： -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>

<!-- 若用 Feign：再加 OpenFeign starter（熔断集成自动生效，见 05 篇） -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>

<!-- 若用响应式：换 reactor 版本 -->
<!-- <artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId> -->
```

> 💡 最少依赖：只加一个 starter 即可用 `CircuitBreakerFactory`——**Factory Bean 由 spring-cloud-commons 自动装配**，无需任何注解启用。

## 2. 最小可跑配置

### 2.1 只配超时就能跑（application.yml）

```yaml
resilience4j:
  timelimiter:
    configs:
      default:
        timeout-duration: 3s          # 调用超过 3s 视为失败
        cancel-running-future: true   # 超时后中断被挂起的线程（默认 true）
  circuitbreaker:
    configs:
      default:
        sliding-window-size: 20
        minimum-number-of-calls: 5    # 小流量测试：5 次即可判定
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
```

> ⚠️ **注意**：`configs.default` 是所有熔断器的兜底配置；不配 = Resilience4J 出厂默认（窗口 100、阈值 50%、最少 100 次、OPEN 60s）——**测试期必配 `minimum-number-of-calls` 调小**，否则流量不够永不熔断（[06 篇](06-配置属性速查.md) 全属性）。

### 2.2 三种配置粒度的最小对应

| 你想配什么 | 写法 |
|-----------|------|
| 所有熔断器统一 | `resilience4j.circuitbreaker.configs.default.*` |
| 某个实例单独 | `resilience4j.circuitbreaker.instances.<名字>.*` |
| 实例继承默认再覆盖 | `instances.<名字>` 只写要覆盖的键（未写的走 default） |

## 3. 编程式 API 全形态

### 3.1 核心：create → run（含 fallback）

```java
@Service
public class OrderQueryService {
    private final CircuitBreakerFactory cbFactory;   // 自动装配（无需 new）
    private final RestTemplate restTemplate;

    public OrderQueryService(CircuitBreakerFactory cbFactory, RestTemplate restTemplate) {
        this.cbFactory = cbFactory;
        this.restTemplate = restTemplate;
    }

    public String getOrder(Long id) {
        return cbFactory.create("orderService")                       // ★ 创建熔断器（名字=yaml 实例名）
                .run(                                                 // ★ 执行受保护调用
                    () -> restTemplate.getForObject(
                            "http://order-service/orders/" + id, String.class),
                    throwable -> "订单服务暂时不可用，请稍后再试");       // ★ fallback：收到异常就返回兜底
    }
}
```

### 3.2 run 的五种形态

```java
CircuitBreaker cb = cbFactory.create("orderService");

// ① 只有调用，无 fallback —— 失败时抛 NoFallbackAvailableException（包装原异常）
String r1 = cb.run(() -> restTemplate.getForObject(url, String.class));

// ② 调用 + fallback（最常用）—— fallback 拿到触发异常的 Throwable
String r2 = cb.run(supplier, throwable -> "兜底结果：" + throwable.getMessage());

// ③ fallback 里做日志/埋点，再抛异常（不吞掉）
String r3 = cb.run(supplier, throwable -> {
    log.warn("orderService 调用失败，熔断状态={}", cb.getState(), throwable);
    throw new BusinessException("orderService down", throwable);   // 业务层再决定怎么处理
});

// ④ 调用返回 null 也算失败吗？—— 不算！Resilience4J 默认只记录"异常"，null 正常返回
//    要按结果判失败：recordResultPredicate（见 06 篇）

// ⑤ 分组创建：同一套规则共享到多个实例（create(id, group)，见 3.3）
CircuitBreaker cbGroup = cbFactory.create("orderService-v2", "orderGroup");
```

> 🎯 核心要点：
> - **fallback 是"异常就兜底"，与熔断状态无关**——CLOSED 状态下一次调用抛异常，同样走 fallback（不是只有熔断才降级）；
> - **熔断器实例是重量级对象**（状态机+滑动窗口），`create()` 有缓存，**别每次请求都 create 新名字**（同名返回同一个）；
> - fallback 里**不要抛异常**（否则调用方看到的不是兜底而是新异常，降级失效，见 [08 篇](08-生产实践与选型避坑.md)）。

### 3.3 分组（group）创建

```java
// create(id, group)：id 是熔断器名，group 决定走哪套"配置组"
// 配置查找顺序：instances.<id> → configs.<group> → configs.default
CircuitBreaker cb = cbFactory.create("orderService-v1", "orderGroup");

// 场景：两个上游都调 order 服务，想共用一套 group 配置，但分别统计熔断
cbFactory.create("orderService-v1", "orderGroup");
cbFactory.create("orderService-v2", "orderGroup");
```

```yaml
resilience4j:
  circuitbreaker:
    configs:
      orderGroup:                      # group 配置（被实例继承）
        sliding-window-size: 100
        failure-rate-threshold: 40
    instances:
      orderService-v1:                 # 实例配置：覆盖 group 中同名字段
        failure-rate-threshold: 60     # 这个实例更宽容
```

## 4. 响应式 API（WebFlux）

### 4.1 依赖切换

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId>
</dependency>
```

### 4.2 ReactiveCircuitBreakerFactory

```java
@Service
public class ReactiveOrderService {
    private final ReactiveCircuitBreakerFactory cbFactory;
    private final WebClient webClient;

    public ReactiveOrderService(ReactiveCircuitBreakerFactory cbFactory, WebClient webClient) {
        this.cbFactory = cbFactory;
        this.webClient = webClient;
    }

    public Mono<String> getOrder(Long id) {
        return webClient.get().uri("http://order-service/orders/{id}", id)
                .retrieve().bodyToMono(String.class)
                .transform(it -> cbFactory.create("orderService")     // ★ transform 包裹
                        .run(it, throwable -> Mono.just("订单服务暂不可用")));
    }
}
```

### 4.3 与阻塞式差异对照

| 维度 | 阻塞式 | 响应式 |
|------|--------|--------|
| Factory | `CircuitBreakerFactory` | `ReactiveCircuitBreakerFactory` |
| run 参数 | Supplier / Function | Mono/Flux + fallback 函数 |
| 包裹方式 | 直接调用 | `.transform()` 或 `transformDeferred()` |
| 超时实现 | TimeLimiter 中断线程 | 取消订阅（基于响应式超时） |
| 适用 | Feign/RestTemplate | WebClient/WebFlux 链路 |

> ⚠️ **别混用**：阻塞式 starter 的 Factory 对 Mono 无效（run 会直接执行订阅），响应式链路全程用 reactive 版本。

## 5. Customizer：Java 配置三形态

> Customizer 是 Java 侧的配置入口（@Bean 定义即生效），**优先级低于 yaml 属性**（同键以 yaml 为准，见 [06 篇](06-配置属性速查.md)）。

### 5.1 形态一：默认配置（所有熔断器）

```java
@Configuration
public class CircuitBreakerConfig {
    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id ->
                new Resilience4JConfigBuilder(id)
                        .circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
                        .timeLimiterConfig(TimeLimiterConfig.custom()
                                .timeoutDuration(Duration.ofSeconds(4))
                                .build())
                        .build());
    }
}
```

### 5.2 形态二：指定实例配置

```java
@Bean
public Customizer<Resilience4JCircuitBreakerFactory> slowCustomizer() {
    return factory -> factory.configure(builder -> builder
            .circuitBreakerConfig(CircuitBreakerConfig.custom()
                    .failureRateThreshold(30)
                    .slidingWindowSize(50)
                    .build())
            .timeLimiterConfig(TimeLimiterConfig.custom()
                    .timeoutDuration(Duration.ofSeconds(2))
                    .build()),
            "slow");        // ★ 只对名为 "slow" 的熔断器生效
}
```

### 5.3 形态三：创建后回调（事件监听/额外定制）

```java
@Bean
public Customizer<Resilience4JCircuitBreakerFactory> eventCustomizer() {
    return factory -> factory.addCircuitBreakerCustomizer(
            circuitBreaker -> circuitBreaker.getEventPublisher()
                    .onStateTransition(event ->
                            log.info("{}: {} → {}", event.getCircuitBreakerName(),
                                    event.getStateTransition().getFromState(),
                                    event.getStateTransition().getToState())),
            "slow");        // 第 2 参不写 = 应用到所有实例
}
```

### 5.4 响应式版 Customizer

```java
@Bean
public Customizer<ReactiveResilience4JCircuitBreakerFactory> reactiveCustomizer() {
    return factory -> factory.configureDefault(id ->
            new Resilience4JConfigBuilder(id)
                    .circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
                    .timeLimiterConfig(TimeLimiterConfig.custom()
                            .timeoutDuration(Duration.ofSeconds(3)).build())
                    .build());
}
```

> 💡 补充 Customizer 还有：`addBulkheadCustomizer` / `addThreadPoolBulkheadCustomizer`（隔离配置）、`configureExecutorService`（线程池定制）——见 [04 篇](04-弹性能力全景：隔离超时重试限流.md)。

## 6. 配置优先级总图

```text
优先级（高 → 低）：
   ① 方法/调用点配置（resilience4j.instances.<id> 或 configure(id))   ← 最具体
   ② 分组配置（configs.<group>，create(id, group) 时生效）
   ③ 默认配置（configs.default / configureDefault / 出厂默认）        ← 兜底
   ④ yaml 属性 优先于 Customizer Bean（同键冲突时以 yaml 为准）
```

| 场景 | 该用哪种 |
|------|---------|
| 规则要改不重启（接配置中心） | yaml 属性（可被 Config 动态刷新） |
| 复杂逻辑（谓词、动态阈值） | Customizer（Java 代码） |
| 团队约定统一模板 | configs.default |
| 某服务特殊 | instances.<id> 或 configure(id) |

## 7. 三步验证与排障

### 7.1 验证清单

| 步骤 | 做法 | 预期结果 |
|------|------|---------|
| ① 正常链路 | 起下游，连续调用 | 返回真实数据；actuator/health 显示 `circuitBreakers: {up: N}` |
| ② 触发熔断 | 停下游，连打超过 minimum-number-of-calls 次 | 日志 `CircuitBreaker 'orderService' recorded a state transition from CLOSED to OPEN` |
| ③ 熔断生效 | OPEN 期间调用 | **毫秒级**返回 fallback（不等待超时）；`/actuator/health` 显示该熔断器 `outOfService` |
| ④ 恢复 | 起下游，等 wait-duration-in-open-state 后调用 | HALF_OPEN 试探一次成功 → 状态回 CLOSED，恢复正常 |

### 7.2 排障起点

| 现象 | 大概率原因 | 处理 |
|------|-----------|------|
| 永远不熔断 | `minimum-number-of-calls` 默认 100，测试流量不够 | yaml 配小（如 5），或压足流量 |
| 熔断"不生效"（还在等超时） | **create 的 id 与 instances.<id> 名字不一致**（走默认配置） | 打印 `cb.getName()` 核对 |
| fallback 没触发 | fallback 里又抛异常 / run 用了无 fallback 形态 | 见 [08 篇](08-生产实践与选型避坑.md) |
| 响应式不生效 | 阻塞/响应式 starter 混用 | 全链路用 reactor starter |
| 改配置没变化 | 优先级理解反了（Customizer 覆盖了 yaml？） | 同键 yaml 优先；重启生效 |

> 💡 观察状态机的快捷方式：把 `logging.level.io.github.resilience4j=DEBUG` 打开，状态转换日志直接可见；生产建议用事件监听收集到监控（[08 篇](08-生产实践与选型避坑.md)）。

---

**下一模块**：[03-熔断原理：状态机与滑动窗口](03-熔断原理：状态机与滑动窗口.md)　**返回总览**：[00-Spring Cloud CircuitBreaker知识体系总览](00-Spring Cloud CircuitBreaker知识体系总览.md)

**【参考来源】**：[Spring Cloud Commons 5.0.2 Circuit Breaker 文档](https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-circuitbreaker.html)、[Spring Cloud CircuitBreaker 5.0.2 官方参考文档](https://docs.spring.io/spring-cloud-circuitbreaker/reference/)、[spring-cloud-commons 源码（circuitbreaker 包）](https://github.com/spring-cloud/spring-cloud-commons/tree/main/spring-cloud-commons/src/main/java/org/springframework/cloud/client/circuitbreaker)
