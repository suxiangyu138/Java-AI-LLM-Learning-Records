# 07 Framework Retry 与 Spring Retry 对比

> 实现篇：**5.0 新增的 Framework Retry 实现（基于 Spring Framework 7 原生 resilience）** 与 **维护模式的 Spring Retry 实现** 的全面对比——新项目选型必读；官方三实现（Resilience4J / Spring Retry / Framework Retry）怎么选，一篇讲清。

---

## 📚 目录

1. [背景：为什么 5.0 新增 Framework Retry](#1-背景为什么-50-新增-framework-retry)
2. [Spring Framework 7 原生 resilience 速览](#2-spring-framework-7-原生-resilience-速览)
3. [Framework Retry 实现详解](#3-framework-retry-实现详解)
4. [Spring Retry 实现（维护模式）](#4-spring-retry-实现维护模式)
5. [三实现全维度对比](#5-三实现全维度对比)
6. [选型决策树](#6-选型决策树)

---

## 1. 背景：为什么 5.0 新增 Framework Retry

### 1.1 官方原话（2025.1.0 发布公告）

> "A new module was added providing a Spring Cloud Circuitbreaker implementation **built on the new resilience support introduced in Spring Framework 7.0.0**（issue #256）."
> "The **spring-cloud-circuitbreaker-spring-retry** module has been placed in **maintenance only mode** and will be removed when Spring Retry is no longer supported."

| 事实 | 说明 |
|------|------|
| 新增模块 | `spring-cloud-starter-circuitbreaker-framework-retry`（Framework 7 原生 retry 之上加**有状态**熔断语义） |
| Spring Retry 转维护 | 不再加新特性，Spring Retry 停产即移除 |
| 动机 | Spring Framework 7 把"重试/弹性"下沉到核心（spring-retry 独立库的使命结束）；SCCB 顺势提供零依赖的熔断实现 |
| 版本 | 5.0.0 起可用（Boot 4 专属，**3.3.x 没有**） |

### 1.2 生态格局变化

```text
弹性能力归属（2026-08）：
  Spring Framework 7 核心：RetryTemplate / RetryPolicy / @Retryable / @ConcurrencyLimit（原生）
  Spring Cloud CircuitBreaker：
    ├── Resilience4J 实现（默认，全能力）
    ├── Framework Retry 实现（5.0 新增，轻量）
    └── Spring Retry 实现（维护模式，等停产后移除）
  spring-retry 独立库：逐渐退出舞台（被 Framework 7 原生替代）
```

## 2. Spring Framework 7 原生 resilience 速览

### 2.1 新 API 一览

| API | 类型 | 说明 |
|-----|------|------|
| `RetryTemplate` | 编程式 | 原生重试模板（替代 spring-retry 的同名类） |
| `RetryPolicy` | 编程式 | 重试策略：`withMaxRetries` / `withMaxDuration` / `withBackoff` / `forExceptions`，可用 `and()` / `or()` 组合 |
| `@Retryable` | 注解 | 声明式重试（maxRetries/delayString/multiplier/jitterString/includes/predicate） |
| `@ConcurrencyLimit` | 注解 | 并发限制：`ThrottlePolicy.BLOCK`（排队）/ `REJECT`（直接拒） |
| `@EnableResilientMethods` | 注解 | 替代旧 `@EnableRetry`（编程式 RetryTemplate 无需启用） |

```java
// 编程式（Framework 7 原生，零额外依赖）
RetryTemplate template = RetryTemplate.builder()
        .retryPolicy(RetryPolicy.withMaxRetries(3)
                .and(RetryPolicy.withBackoff(Duration.ofMillis(100), 2.0))
                .forExceptions(IOException.class))
        .build();

String result = template.execute(context -> downstream.call());
```

```java
// 声明式
@Retryable(maxRetries = 3, delayString = "100ms", multiplier = 2.0)
public String call() { return downstream.call(); }
```

> 🎯 核心变化：**重试能力从"第三方库"变成"框架核心"**——新项目可以直接用 Framework 7 原生 API，不再需要 spring-retry 依赖；SCCB 的 Framework Retry 实现就是把"有状态熔断"叠加在这套原生重试之上。

## 3. Framework Retry 实现详解

### 3.1 依赖与工厂

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-framework-retry</artifactId>
    <!-- 5.0.x / Cloud 2025.1.x / Boot 4 专属 -->
</dependency>
```

```java
// 注入的工厂类型变了
private final FrameworkRetryCircuitBreakerFactory cbFactory;
// 使用方式与 Resilience4J 完全一致（同一个 CircuitBreaker 接口）
cbFactory.create("orderService").run(supplier, throwable -> "兜底");
```

### 3.2 配置（全部编程式，无 yaml 属性）

```java
@Configuration
public class CircuitBreakerConfig {
    // 默认配置（所有熔断器）
    @Bean
    public Customizer<FrameworkRetryCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id ->
                new FrameworkRetryConfigBuilder(id)
                        .retryPolicy(RetryPolicy.withMaxRetries(3))          // ★ 重试策略（必配）
                        .openTimeout(Duration.ofSeconds(20))                 // OPEN 停留时长（默认 20s）
                        .resetTimeout(Duration.ofSeconds(5))                 // 无失败自动复位（默认 5s）
                        .build());
    }

    // 指定实例
    @Bean
    public Customizer<FrameworkRetryCircuitBreakerFactory> slowCustomizer() {
        return factory -> factory.configure(builder -> builder
                .retryPolicy(RetryPolicy.withMaxRetries(1))
                .openTimeout(Duration.ofSeconds(30))
                .resetTimeout(Duration.ofSeconds(10))
                .build(), "slow");
    }
}
```

| 配置项 | 默认 | 说明 |
|--------|------|------|
| `retryPolicy(RetryPolicy)` | 必配 | Framework 7 原生策略：withMaxRetries / withMaxDuration / withBackoff / forExceptions + and/or 组合 |
| `openTimeout(Duration)` | **20s** | OPEN 停留时长，到点转 HALF_OPEN |
| `resetTimeout(Duration)` | **5s** | 若持续无失败，自动复位回 CLOSED（即使曾 OPEN） |

### 3.3 状态机行为（与 Resilience4J 的差异）

| 行为 | Framework Retry | Resilience4J |
|------|-----------------|--------------|
| CLOSED | 放行 + 按 RetryPolicy 重试；**一次完整调用（重试耗尽）失败 → 立刻 OPEN** | 滑动窗口统计，失败率达标才 OPEN |
| OPEN | 拒绝所有请求（直接 fallback，**不重试**） | 同（快速失败） |
| HALF_OPEN | 放行**单个**试探请求；成功 → CLOSED，失败 → OPEN | 放行 N 个（默认 3） |
| 复位 | `resetTimeout` 内无失败 → 自动 CLOSED（有状态跟踪） | 无此机制（靠 waitDuration + 半开） |
| 统计口径 | **无百分比阈值**（有状态跟踪失败，无滑动窗口） | 滑动窗口 + 失败率阈值 |
| 语义 | 仿 Spring Retry 的 CircuitBreakerRetryPolicy | 完整统计熔断 |

> 🎯 关键差异：**Framework Retry 的熔断是"重试耗尽即熔断"，不是"失败率达标即熔断"**——没有滑动窗口/百分比概念；适合"重试为主、熔断兜底"的轻量语义，不适合需要精细统计的场景。

### 3.4 限制（官方明确）

> "The Framework Retry circuit breaker implementation **does not support reactive applications**. If you need reactive support, use the Resilience4J implementation instead." —— 官方文档原话

| 限制 | 说明 |
|------|------|
| ❌ 响应式 | 不支持（无 reactor starter） |
| ❌ 隔离/限流 | 无 Bulkhead/RateLimiter（只有重试+熔断） |
| ❌ 超时 | 无 TimeLimiter（超时要靠 RetryPolicy 的 withMaxDuration 或业务层） |
| ✅ 注解 | 可直接用 Framework 7 的 @Retryable（原生） |

## 4. Spring Retry 实现（维护模式）

### 4.1 现状

| 维度 | 说明 |
|------|------|
| 模块 | `spring-cloud-starter-circuitbreaker-spring-retry`（3.0 引入，4.x/5.x 保留） |
| 状态 | **维护模式**：只修 bug 不加功能，Spring Retry 停产后随其移除 |
| 原理 | 基于 Spring Retry 的 `RetryTemplate` + `CircuitBreakerRetryPolicy`（stateful retry） |
| 能力 | 重试 + 熔断（无隔离/限流/超时组件） |
| 配置 | `SpringRetryConfigBuilder` + `retryPolicy`（SimpleRetryPolicy / TimeoutRetryPolicy 等） |

### 4.2 典型配置

```java
@Bean
public Customizer<SpringRetryCircuitBreakerFactory> defaultCustomizer() {
    return factory -> factory.configureDefault(id ->
            new SpringRetryConfigBuilder(id)
                    .retryPolicy(new TimeoutRetryPolicy(3000))      // 超时策略
                    .build());
}

// 指定实例
factory.configure(builder -> builder.retryPolicy(new SimpleRetryPolicy(1)), "slow");

// 监听器
factory.addRetryTemplateCustomizers(template -> template.registerListener(
        new RetryListenerSupport() { /* onError/onSuccess */ }));
```

### 4.3 迁移建议（存量用户）

```text
现在用的 Spring Retry 实现 → 未来怎么办：
  ├── 只需"重试+熔断"：迁 Framework Retry（5.0+，零依赖）——配置模型相似（都是 RetryPolicy）
  ├── 需要超时/隔离/限流：迁 Resilience4J（能力对齐，配置模型不同，需重配 yaml）
  └── 维持现状：暂可（维护模式≠不可用），但别新增依赖它
```

## 5. 三实现全维度对比

| 维度 | Resilience4J（默认） | Spring Retry（维护） | Framework Retry（新） |
|------|---------------------|---------------------|----------------------|
| 版本 | 5.0.x 起全版本 | 3.0 起全版本 | **5.0 起（Boot 4 专属）** |
| 熔断统计 | 滑动窗口+失败率/慢调用率 | 状态跟踪（重试耗尽） | 状态跟踪（重试耗尽） |
| 重试 | ✅ Retry 模块 | ✅ 本职 | ✅ Framework 7 原生 |
| 超时 | ✅ TimeLimiter | ⚠️ 需配 TimeoutRetryPolicy | ❌（靠业务/策略） |
| 隔离 | ✅ Bulkhead | ❌ | ❌ |
| 限流 | ✅ RateLimiter | ❌ | ❌ |
| 响应式 | ✅ reactor starter | ❌ | ❌ |
| 注解 | ✅（resilience4j-spring-boot3） | ✅（Spring Retry 注解） | ✅（Framework 7 @Retryable） |
| 配置方式 | yaml 属性 + Customizer | Customizer（编程式） | Customizer（编程式，无 yaml） |
| 依赖重量 | 全家桶（最重） | spring-retry（轻） | **零依赖**（Framework 7 自带） |
| 维护状态 | ✅ 活跃 | ⚠️ 维护模式 | ✅ 新秀 |
| 面试热度 | ★★★ | ★ | ★★（新特性） |

## 6. 选型决策树

```text
新项目（Boot 4）？
  ├── 需要隔离/限流/响应式？ → Resilience4J（默认实现，能力全覆盖）
  ├── 只要"重试+熔断"、想零依赖？ → Framework Retry ★ 官方新方向
  └── 团队熟悉 Spring Retry 注解？ → 可先用 Spring Retry，但评估迁 Framework Retry

存量项目？
  ├── Boot 3.x：只能 3.3.x（无 Framework Retry）→ Resilience4J 或 Spring Retry
  ├── Boot 4 + Spring Retry 实现 → 计划迁 Framework Retry（语义最接近）
  └── Boot 4 + Resilience4J → 维持（官方默认，无迁移压力）
```

> 🎯 一句话结论：**默认 Resilience4J 永不出错；"极简主义"新项目（零依赖、只重试+熔断、纯阻塞）选 Framework Retry 是官方演进方向**——它吃掉的是 Spring Retry 的生态位，不是 Resilience4J 的。

---

**下一模块**：[08-生产实践与选型避坑](08-生产实践与选型避坑.md)　**返回总览**：[00-Spring Cloud CircuitBreaker知识体系总览](00-Spring Cloud CircuitBreaker知识体系总览.md)

**【参考来源】**：[Spring Cloud 2025.1.0（Oakwood）发布公告](https://spring.io/blog/2025/11/25/spring-cloud-2025-1-0-aka-oakwood-has-been-released/)、[Spring Cloud CircuitBreaker 5.0.2（Framework Retry 配置）](https://docs.spring.io/spring-cloud-circuitbreaker/reference/spring-cloud-circuitbreaker-framework-retry.html)、[Spring Cloud CircuitBreaker 5.0.2（Spring Retry 配置）](https://docs.spring.io/spring-cloud-circuitbreaker/reference/spring-cloud-circuitbreaker-spring-retry.html)、[Spring I/O 2026：Core Resilience Features in Spring Framework 7（Juergen Hoeller）](https://2026.springio.net/sessions/core-resilience-features-in-spring-framework-7/)
