# 06 - API 网关：Spring Cloud Gateway

> 🎯 Spring Cloud Gateway 基于 WebFlux（响应式），性能远超 Zuul 1.x — 统一鉴权、路由转发、限流熔断、跨域处理，微服务的第一道关卡

---

## 目录

1. [Gateway 概述](#1-gateway-概述)
2. [路由配置实战](#2-路由配置实战)
3. [自定义过滤器](#3-自定义过滤器)
4. [限流与熔断集成](#4-限流与熔断集成)
5. [跨域与聚合文档](#5-跨域与聚合文档)

---

## 1. Gateway 概述

### 依赖

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
<!-- ⚠️ Gateway 基于 WebFlux，不能引入 spring-boot-starter-web -->
```

### 三大核心

```
Route（路由）：
  id + 目标 URI + Predicates（断言）+ Filters（过滤器）

Predicate（断言）：
  匹配条件 — Path=/api/**、Header、Cookie、Method、Host 等

Filter（过滤器）：
  修改请求/响应 — AddRequestHeader、StripPrefix、RateLimiter 等
```

---

## 2. 路由配置实战

```yaml
spring:
  cloud:
    gateway:
      routes:
        # ═══ 用户服务 ═══
        - id: user-service
          uri: lb://user-service              # ⭐ lb:// = 负载均衡
          predicates:
            - Path=/api/users/**
          filters:
            - StripPrefix=1

        # ═══ 订单服务 ═══
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/orders/**
          filters:
            - StripPrefix=1

        # ═══ 限流示例 ═══
        - id: order-rate-limit
          uri: lb://order-service
          predicates:
            - Path=/api/orders/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter:
                  replenishRate: 100       # 每秒 100 令牌
                  burstCapacity: 200       # 突发 200
                key-resolver: "#{@ipKeyResolver}"   # 按 IP 限流

      # ═══ 全局跨域 ═══
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOriginPatterns: "*"
            allowedMethods: "*"
            allowedHeaders: "*"
            allowCredentials: true
```

### 内置 Predicate 工厂

| Predicate | 示例 |
|-----------|------|
| Path | `Path=/api/users/**` |
| Header | `Header=X-Request-Id, \d+` |
| Method | `Method=GET,POST` |
| Query | `Query=name, ^\w+$` |
| Host | `Host=**.example.com` |
| Cookie | `Cookie=token, ^[a-z0-9]+` |
| Weight | `Weight=group1, 90`（灰度） |

---

## 3. 自定义过滤器

### 3.1 GlobalFilter（⭐ 鉴权场景）

```java
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 白名单跳过鉴权
        if (path.startsWith("/api/auth/login") || path.startsWith("/api/public")) {
            return chain.filter(exchange);
        }

        // 校验 Token
        String token = request.getHeaders().getFirst("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return unauthorized(exchange, "缺少认证信息");
        }

        try {
            // 解析 JWT，注入用户信息到 Header
            String userId = JwtUtil.parseUserId(token);
            ServerHttpRequest newRequest = request.mutate()
                .header("X-User-Id", userId)
                .build();
            return chain.filter(exchange.mutate().request(newRequest).build());
        } catch (Exception e) {
            return unauthorized(exchange, "Token 无效");
        }
    }

    @Override
    public int getOrder() { return -100; }  // 优先级
}
```

### 3.2 GatewayFilter 工厂

```java
@Component
public class LoggingGatewayFilterFactory
    extends AbstractGatewayFilterFactory<LoggingGatewayFilterFactory.Config> {

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            long start = System.currentTimeMillis();
            ServerHttpRequest request = exchange.getRequest();
            log.info("{} {} {}", config.prefix, request.getMethod(), request.getURI());

            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                long elapsed = System.currentTimeMillis() - start;
                log.info("{} 耗时: {}ms", config.prefix, elapsed);
            }));
        };
    }
}

// yaml 中使用
filters:
  - Logging=GATEWAY
```

---

## 4. 限流与熔断集成

### 4.1 Redis 限流

```java
@Bean
public KeyResolver ipKeyResolver() {
    return exchange -> Mono.just(
        exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
    );
}
```

### 4.2 Sentinel 网关熔断

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-alibaba-sentinel-gateway</artifactId>
</dependency>
```

```yaml
spring:
  cloud:
    sentinel:
      scg:
        fallback:
          mode: response                    # 降级返回 JSON
          response-status: 429
          response-body: '{"code":429,"msg":"请求过于频繁"}'
```

---

## 5. 跨域与聚合文档

### 5.1 Knife4j 聚合 API 文档

```yaml
# Gateway 聚合多个服务的 Swagger 文档
spring:
  cloud:
    gateway:
      routes:
        - id: user-service-api
          uri: lb://user-service
          predicates:
            - Path=/user-service/v3/api-docs
          filters:
            - RewritePath=/user-service/(?<segment>.*), /$\{segment}

        - id: order-service-api
          uri: lb://order-service
          predicates:
            - Path=/order-service/v3/api-docs
          filters:
            - RewritePath=/order-service/(?<segment>.*), /$\{segment}
```

> 🎯 **最佳实践**：Gateway 统一鉴权、按 Path 路由、Sentinel 网关层限流降级、Knife4j 聚合 API 文档。Gateway 自身做到轻量无状态，业务逻辑留给后端服务。
