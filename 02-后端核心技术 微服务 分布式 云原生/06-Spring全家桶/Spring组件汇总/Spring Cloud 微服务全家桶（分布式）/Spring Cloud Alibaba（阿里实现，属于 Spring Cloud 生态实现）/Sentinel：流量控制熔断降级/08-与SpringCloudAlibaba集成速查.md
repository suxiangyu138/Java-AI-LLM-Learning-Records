# 08 Sentinel 与 Spring Cloud Alibaba 集成速查

> 工程落地篇：`@SentinelResource` 全属性表、**OpenFeign 熔断降级**（fallback/fallbackFactory）、Spring Cloud Gateway 网关限流、WebFlux 响应式、Dubbo 适配——**"注解怎么写、Feign 怎么兜底"的完整答案**。

---

## 📚 目录

1. [集成全景](#1-集成全景)
2. [@SentinelResource 注解全属性](#2-sentinelresource-注解全属性)
3. [OpenFeign 熔断降级](#3-openfeign-熔断降级)
4. [Spring Cloud Gateway 限流](#4-spring-cloud-gateway-限流)
5. [WebFlux 响应式支持](#5-webflux-响应式支持)
6. [Dubbo 与其他适配器](#6-dubbo-与其他适配器)
7. [集成高频坑](#7-集成高频坑)

---

## 1. 集成全景

```text
SCA 生态中 Sentinel 的挂载点：
    网关层：Spring Cloud Gateway 适配器（限流）
    服务层：@SentinelResource（接口/方法）+ URL 自动埋点（Spring MVC）
    调用层：OpenFeign（fallback 兜底）→ 下游服务
    响应式：WebFlux 适配器
    RPC：Dubbo 适配器
```

| 挂载点 | 适配方式 | 配置 |
|--------|---------|------|
| Spring MVC | 自动埋点（URL 即资源） | starter 自动装配 |
| 方法/接口 | `@SentinelResource` 注解 | sentinel-annotation-aspectj（starter 自带） |
| OpenFeign | `feign.sentinel.enabled: true` | 需显式开启 |
| Gateway | `sentinel-spring-cloud-gateway-adapter` | 网关依赖 + 规则 |
| WebFlux | `sentinel-webflux-adapter` | 响应式服务 |
| Dubbo | `sentinel-apache-dubbo-adapter` | 消费方/提供方自动埋点 |

## 2. @SentinelResource 注解全属性

```java
@SentinelResource(
        value = "order:create",                    // ★ 资源名（规则用它匹配）
        entryType = EntryType.OUT,                 // 入口类型（IN=入口流量/OUT=出口）
        blockHandler = "createBlock",              // 限流/熔断/授权兜底（BlockException）
        blockHandlerClass = OrderHandlers.class,   // blockHandler 外置类（方法必须 static）
        fallback = "createFallback",               // 业务异常兜底（Throwable）
        fallbackClass = OrderHandlers.class,       // fallback 外置类（方法必须 static）
        defaultFallback = "defaultBlock",          // 通用兜底（无需原方法参数）
        exceptionsToIgnore = {IllegalArgumentException.class}  // 忽略的异常（原样抛出）
)
public Order create(OrderDTO dto) { ... }
```

| 属性 | 处理对象 | 优先级 | 方法签名要求 |
|------|---------|--------|-------------|
| `blockHandler` | BlockException（限流/熔断/授权） | 最高（对应规则触发） | 同参同返 + 可加 BlockException |
| `fallback` | 业务异常（Throwable 族） | 次之 | 同参同返 + 可加 Throwable |
| `defaultFallback` | 所有异常（除 ignore） | 兜底 | 无需原方法参数 + 可加 Throwable |
| `blockHandlerClass`/`fallbackClass` | 外置兜底类 | — | 对应方法必须 **static** |
| `exceptionsToIgnore` | 忽略列表 | — | 忽略的异常原样抛，不进 fallback |

```java
// 兜底方法规范（同返回值 + 原方法参数 + 可选异常参数）
public Order createBlock(OrderDTO dto, BlockException e) { return Order.degraded(dto); }
public Order createFallback(OrderDTO dto, Throwable t)   { return Order.degraded(dto); }
```

> ⚠️ **注解三铁律**：① **只能标实现类方法**（不能标接口）；② 兜底方法必须 `public`，与原方法**同参数同返回**；③ 外置类（`xxClass`）时方法必须 static——三条违反任何一条，兜底静默不生效（[09 篇](09-集成地图与常见问题.md) 坑 #3）。

## 3. OpenFeign 熔断降级

### 3.1 开启与兜底写法

```yaml
feign:
  sentinel:
    enabled: true        # ★ 开启 Feign + Sentinel 熔断（默认 true）
```

```java
// 方式 A：fallback 类（简单场景）
@FeignClient(name = "stock-service", fallback = StockClientFallback.class)
public interface StockClient {
    @GetMapping("/stock/{skuId}") Stock getStock(@PathVariable String skuId);
}

@Component
public class StockClientFallback implements StockClient {
    @Override
    public Stock getStock(String skuId) {
        return Stock.UNKNOWN;          // 降级兜底值
    }
}

// 方式 B：fallbackFactory（可拿到异常原因，推荐）
@FeignClient(name = "stock-service", fallbackFactory = StockClientFallbackFactory.class)
public interface StockClient { ... }

@Component
public class StockClientFallbackFactory implements FallbackFactory<StockClient> {
    @Override
    public StockClient create(Throwable cause) {
        log.error("stock-service 调用失败", cause);
        return skuId -> Stock.UNKNOWN;      // 按异常类型返回不同兜底
    }
}
```

### 3.2 Feign 降级机制

| 场景 | 触发 | 兜底 |
|------|------|------|
| 下游限流/熔断 | BlockException | 有 fallback → 走 fallback；无 → 抛异常 |
| 下游超时/连接失败 | 业务异常 | 同上 |
| 两者都无 | — | **异常直接抛给调用方**（无默认兜底） |

> ⚠️ **Feign 无兜底 = 异常上抛**：不配 fallback 时被限流/熔断的调用直接抛 `BlockException`/`SentinelClientException`——**生产必须给关键 Feign 配 fallback 或 fallbackFactory**（[04 篇](04-熔断降级速查.md) 的"默认无兜底"同样适用）。

## 4. Spring Cloud Gateway 限流

```xml
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-spring-cloud-gateway-adapter</artifactId>
    <version>1.8.9</version>
</dependency>
```

```java
// 网关配置类：注册 Sentinel Gateway 拦截器 + 自定义 API 分组
@Configuration
public class GatewayConfig {
    @Bean
    public SentinelGatewayFilter sentinelGatewayFilter() {
        return new SentinelGatewayFilter();          // 全局过滤：route/API 作为资源
    }

    // 网关规则定义（也可以走 Nacos 数据源，与 06 篇一致）
    @PostConstruct
    public void initRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();
        rules.add(new GatewayFlowRule("order-service")      // 按 routeId 限流
                .setCount(100).setIntervalSec(1));
        rules.add(new GatewayFlowRule("api-order")          // 按 API 分组限流
                .setCount(50).setIntervalSec(1));
        GatewayRuleManager.loadRules(rules);
    }
}
```

| 资源类型 | 说明 |
|---------|------|
| **route 资源** | 按路由 ID 限流（`routeId`） |
| **API 分组资源** | 自定义 API 定义（路径匹配集合）统一限流 |

> 💡 网关限流的定位：**第一道闸门**——网关层把流量挡在服务网格之外，服务内再按资源细限；注意网关适配器规则与客户端规则是**两套体系**（GatewayRuleManager vs 普通 RuleManager）。

## 5. WebFlux 响应式支持

```xml
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-webflux-adapter</artifactId>
    <version>1.8.9</version>
</dependency>
```

```java
// 响应式入口自动埋点（URL 即资源），规则与注解用法一致
// 拦截后返回的兜底可通过自定义 BlockRequestHandler 控制
@Bean
public BlockRequestHandler blockRequestHandler() {
    return (exchange, t) -> ServerResponse
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .body(BodyInserters.fromValue("{\"code\":429}"));
}
```

> ⚠️ WebFlux 注意：`@SentinelResource` 用于**阻塞方法**；响应式流内限流用**适配器自动埋点**（flux 包），两者不混用——反应式资源在订阅执行时统计。

## 6. Dubbo 与其他适配器

| 适配器 | 坐标 | 说明 |
|--------|------|------|
| Dubbo | `sentinel-apache-dubbo-adapter` | 消费方/提供方自动埋点（接口名:方法名 为资源） |
| Sofa RPC | `sentinel-sofa-rpc-adapter` | Sofa 生态 |
| RocketMQ | `sentinel-mq-rocketmq-adapter`（社区） | 消费限流 |
| gRPC | `sentinel-grpc-adapter`（社区/生态） | gRPC 拦截器 |

```xml
<!-- Dubbo 消费者 + 提供者都加适配器，资源名 = 接口全限定名:方法名 -->
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-apache-dubbo-adapter</artifactId>
    <version>1.8.9</version>
</dependency>
```

## 7. 集成高频坑

| 坑 | 现象 | 处理 |
|----|------|------|
| Feign 兜底不生效 | 配了 fallback 但抛异常 | `feign.sentinel.enabled: true` 没开 / 降级 Bean 被 Spring 管理冲突（放 configuration） |
| 注解兜底不生效 | 限流了但没走 blockHandler | 兜底方法签名不符 / 标在接口上 / 外置类方法非 static |
| 资源名不一致 | 规则加了但拦不住 | 控制台规则资源名 = 注解 value（含包名大小写） |
| Gateway 规则不生效 | 网关加了规则没反应 | 用的是 GatewayRuleManager 体系；检查 routeId 名称 |
| 链路模式失效 | 链路规则拦不了 | `web-context-unify: false` 未配置 |
| 限流返回 500 | 无 blockHandler 无 fallback | 所有资源配兜底（默认 Blocked 页也会 429/提示） |

---

**下一模块**：[09-集成地图与常见问题](09-集成地图与常见问题.md)　**返回总览**：[00-Sentinel知识体系总览](00-Sentinel知识体系总览.md)

**【参考来源】**：[Sentinel 官方文档-OpenFeign 适配](https://sentinelguard.io/zh-cn/docs/open-feign.html)、[Sentinel 官方文档-Spring Cloud Gateway](https://sentinelguard.io/zh-cn/docs/api-gateway-flow-control.html)、[Sentinel 整合 OpenFeign 避坑（阿里云开发者）](https://developer.aliyun.com/article/945552)
