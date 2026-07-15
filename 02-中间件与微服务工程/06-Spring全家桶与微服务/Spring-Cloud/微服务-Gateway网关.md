# 微服务 - Spring Cloud Gateway 网关

> **定位**：微服务架构的"统一入口"——所有客户端请求经网关再转发。基于 Spring WebFlux（非阻塞 IO），性能优于 Zuul。

---

## 目录

1. [核心概念](#1-核心概念)
2. [实操落地](#2-实操落地)
3. [核心功能](#3-核心功能)
4. [常见问题](#4-常见问题)

---

## 1. 核心概念

### 三要素

| 概念 | 说明 | 示例 |
|------|------|------|
| **Route** | 路由，定义转发规则 | ID + URI + Predicate |
| **Predicate** | 断言，请求匹配条件 | `Path=/order/**` |
| **Filter** | 过滤器，请求/响应处理 | Token 校验、日志 |

### 请求流转

```text
客户端 → Gateway → Predicate 匹配路由 → GlobalFilter → GatewayFilter → 转发微服务 → 返回
```

### Gateway vs Zuul

| 维度 | Gateway | Zuul 1.x |
|------|---------|----------|
| 架构 | WebFlux 非阻塞 | Servlet 阻塞 |
| 性能 | ✅ 高并发 | ❌ 一般 |
| 维护 | ✅ 官方活跃 | ❌ 已停更 |

---

## 2. 实操落地

### 依赖

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

> ⚠️ 禁止引入 `spring-boot-starter-web`，与 WebFlux 冲突！

### 核心配置

```yaml
spring:
  application:
    name: gateway-service
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
      routes:
        - id: order-service-route
          uri: lb://order-service
          predicates:
            - Path=/order/**
          filters:
            - StripPrefix=1           # /order/create → /create
        - id: user-service-route
          uri: lb://user-service
          predicates:
            - Path=/user/**
          filters:
            - StripPrefix=1
server:
  port: 8080
```

---

## 3. 核心功能

### 统一认证（GlobalFilter）

```java
@Bean
@Order(-1)
public GlobalFilter authFilter() {
    return (exchange, chain) -> {
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        // 解析用户信息 → 请求头传递
        return chain.filter(exchange.mutate()
            .request(r -> r.header("X-User-Id", userId)).build());
    };
}
```

### 限流（Sentinel）

```yaml
- name: Sentinel
  args:
    resource: order-service
    grade: QPS
    count: 100
```

### 全局日志

```java
@Bean
@Order(0)
public GlobalFilter logFilter() {
    return (exchange, chain) -> {
        long start = System.currentTimeMillis();
        return chain.filter(exchange).doFinally(s -> {
            long time = System.currentTimeMillis() - start;
            log.info("{} {} → {} ({}ms)", method, path, status, time);
        });
    };
}
```

---

## 4. 常见问题

| 问题 | 方案 |
|------|------|
| 启动失败 "Circular view path" | 删除 `spring-boot-starter-web` 依赖 |
| 404 Not Found | 检查 Path 断言 + 微服务是否注册 + StripPrefix 配置 |
| 过滤器不执行 | 加 `@Bean` + 设合理 `@Order` 值 |
| 限流不生效 | 检查 Sentinel 依赖 + 控制台 + 规则参数 |
