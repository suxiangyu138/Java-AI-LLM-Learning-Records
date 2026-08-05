# 04 - API 网关

> **核心摘要**：网关是微服务的「总入口」——路由、鉴权、限流、日志四大职责。2026 年标准：Spring Cloud Gateway（响应式吞吐量为 Zuul 1.5 倍）+ 双层网关（K8s Ingress + Gateway）。本文覆盖 Gateway 核心、过滤器顺序、超时铁律与生产配置。

> **前置阅读**：[[02-服务通信与调用]]、[[03-服务注册与发现]]

---

## 📚 目录

1. [网关的四大职责](#1-网关的四大职责)
2. [Gateway vs Zuul](#2-gateway-vs-zuul)
3. [路由配置](#3-路由配置)
4. [过滤器链](#4-过滤器链)
5. [网关限流](#5-网关限流)
6. [网关超时铁律](#6-网关超时铁律)
7. [双层网关架构](#7-双层网关架构)
8. [生产配置模板](#8-生产配置模板)
9. [核心要点](#9-核心要点)

---

## 1. 网关的四大职责

> **背景**：微服务数量多——客户端不能直连每个服务；网关是统一入口。
> **目的**：理解网关的职责边界（做什么/不做什么）。
> **适用范围**：所有微服务架构。

```text
网关四大职责
├── ① 路由：请求分发到正确服务
│   ├── 按路径/方法/头/参数匹配
│   └── lb:// 从注册中心获取实例
├── ② 鉴权：身份认证（统一入口做）
│   ├── 解析 Token → 透传用户信息
│   └── ⚠️ 权限校验由业务服务做（网关不耦合业务）
├── ③ 限流：保护后端（第一道防线）
│   ├── 令牌桶/漏桶
│   └── 防突刺流量压垮服务
└── ④ 日志：访问日志/监控埋点

网关「不做」的事（2026 边界）
├── ❌ 业务逻辑（不聚合数据/不转换业务字段）
├── ❌ 权限细节（角色/资源判断——服务自己做）
└── ❌ 数据存储
→ 金句：网关管「入口治理」——业务语义留给服务
```

---

## 2. Gateway vs Zuul

| 维度 | Spring Cloud Gateway | Zuul 1.x |
|------|:---:|:---:|
| 底层 | **WebFlux（响应式非阻塞）** | Servlet（阻塞） |
| 吞吐量 | **Zuul 的 1.5 倍** | 基准 |
| 异步 | ✅ 原生 | ❌ |
| Sentinel 集成 | ✅ 原生 | 需适配 |
| 2026 定位 | **标准选择** | 过时 |

```text
为什么选 Gateway（2026）
├── ① 响应式：非阻塞（高并发下吞吐量优势明显）
├── ② Sentinel 集成：流控规则直接配
├── ③ 长连接支持：WebSocket
├── ④ Spring Cloud 官方推荐（Zuul 1 维护停滞）
└── ⑤ 注意：响应式模型（WebFlux）——不是 Servlet（无 Tomcat 概念）
```

---

## 3. 路由配置

```yaml
# application.yml（Gateway 路由配置）
spring:
  cloud:
    gateway:
      routes:
        # ① 基本路由（lb:// 从注册中心）
        - id: order-service
          uri: lb://order-service          # 负载均衡到注册服务
          predicates:
            - Path=/api/orders/**          # 路径匹配
          filters:
            - StripPrefix=1                # 去掉 /api

        # ② 多条件匹配
        - id: payment-service
          uri: lb://payment-service
          predicates:
            - Path=/api/payments/**
            - Method=POST                  # 方法匹配
            - Header=X-Version, v2         # 头匹配

        # ③ 灰度路由（Header 染色）
        - id: order-v2
          uri: lb://order-service
          predicates:
            - Path=/api/orders/**
            - Header=X-Canary, v2          # 灰度流量走 v2
```

```text
Route Predicate 匹配策略（2026）
├── Path：路径匹配（最常见）
├── Method：HTTP 方法
├── Header：请求头（灰度染色用）
├── Query：查询参数
├── Cookie：Cookie 值
├── Host：域名匹配
├── Weight：权重路由（灰度）
└── After/Before/Between：时间窗口
→ 金句：Predicates 决定「路由到哪」——组合使用
```

---

## 4. 过滤器链

### 4.1 过滤器类型

```text
Gateway 过滤器两种
├── ① GlobalFilter：全局（所有路由生效）
│   ├── 鉴权/限流/日志（横切）
├── ② GatewayFilter：路由级（配置中指定）
│   ├── StripPrefix/AddRequestHeader（特定路由）
└── 执行时机：pre（转发前）/post（响应后）

过滤器顺序（2026 推荐）
├── ① 鉴权 → ② 限流 → ③ 日志 → ④ 路由
└── 原因：
    ├── 鉴权最前（未认证不浪费）
    ├── 限流次之（保护后端）
    ├── 日志记录（可追溯）
    └── 路由最后（转发）
```

### 4.2 鉴权过滤器示例

```java
@Component
public class AuthFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // ① 放行白名单（登录/健康检查）
        String path = exchange.getRequest().getURI().getPath();
        if (WHITE_LIST.contains(path)) {
            return chain.filter(exchange);
        }

        // ② 解析 Token（JWT）
        String token = exchange.getRequest().getHeaders()
            .getFirst("Authorization");
        if (token == null || !jwtService.validate(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();   // 401
        }

        // ③ 透传用户信息（网关只做认证！）
        String userId = jwtService.getUserId(token);
        ServerHttpRequest mutated = exchange.getRequest().mutate()
            .header("X-User-Id", userId)
            .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        return -100;      // 最前执行（负值优先）
    }
}
```

> 🎯 **鉴权边界**：网关只做「身份认证」（你是谁）——「权限校验」（你能做什么）由业务服务判断。

---

## 5. 网关限流

### 5.1 令牌桶算法

```yaml
# 网关限流（RequestRateLimiter + Redis）
spring:
  cloud:
    gateway:
      routes:
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/orders/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10     # 每秒补 10 个令牌
                redis-rate-limiter.burstCapacity: 20     # 突发容量 20
                redis-rate-limiter.requestedTokens: 1    # 每次取 1 个
            - name: RequestRateLimiter
              args:
                key-resolver: "#{@userKeyResolver}"      # 按用户限流
```

### 5.2 限流 Key 设计

```java
// 限流维度（按用户/IP/接口）
@Bean
public KeyResolver userKeyResolver() {
    return exchange -> {
        String userId = exchange.getRequest().getHeaders()
            .getFirst("X-User-Id");
        return Mono.just(userId != null ? userId : "anonymous");
    };
}

// 限流降级响应（返回 429）
@Bean
public RequestRateLimiter requestRateLimiter() {
    // 自定义降级：返回 429 + JSON 提示
}

// ⚠️ 限流要点
// ① 维度：按用户/IP/接口（防单一用户刷爆）
// ② Redis：分布式限流（多实例共享）
// ③ 降级：429 + 友好提示（不是 500）
// ④ 金句：限流是第一道防线——保护后端不被打爆
```

---

## 6. 网关超时铁律

> ⚠️ **网关超时 > 下游所有服务超时的最大值**（高频生产事故）：

```yaml
# 网关超时配置（⚠️ 必须大于下游最大超时）
spring:
  cloud:
    gateway:
      httpclient:
        connect-timeout: 5000        # 连接超时 5s
        response-timeout: 30s        # 响应超时 30s（⚠️ 大于下游）
      # 或按路由：
      routes:
        - id: order-service
          uri: lb://order-service
          metadata:
            response-timeout: 30000  # 路由级 30s
            connect-timeout: 3000
```

```text
超时铁律的原因
├── ① 网关超时 < 服务超时：
│   ├── 服务还在处理（2s 内返回）
│   ├── 但网关 1s 就超时了
│   └── 正常请求被「提前截断」→ 客户端拿到超时
├── ② 铁律：网关超时 ≥ 下游最大超时 + 余量
├── ③ 链路示例：
│   ├── order-service 超时 5s
│   ├── payment-service 超时 10s
│   └── 网关超时 ≥ 15s（所有下游最大值 + 余量）
└── ④ 金句：网关超时 = 「下游超时的最大值」——不是随便设
```

---

## 7. 双层网关架构

> 🎯 **2026 标准：K8s Ingress + Spring Cloud Gateway 双层**：

```text
双层网关职责划分
├── 外层：K8s Ingress
│   ├── TLS 终止（证书）
│   ├── 域名路由（DNS → 集群）
│   ├── 全局限流
│   └── 静态资源（Nginx 能力）
├── 内层：Spring Cloud Gateway
│   ├── 业务路由（lb:// 注册中心）
│   ├── 认证鉴权（JWT 解析）
│   ├── 请求转换（头/参数）
│   └── 业务限流（按用户/接口）
└── 好处：
    ├── 职责分离（基础设施 vs 业务语义）
    ├── 各层独立演进
    └── 金句：Ingress 管「基础设施」、Gateway 管「业务语义」

架构
用户 → DNS → K8s Ingress（TLS/域名）→ Gateway（鉴权/路由）→ 微服务
```

---

## 8. 生产配置模板

```yaml
# 网关生产配置要点（汇总）
server:
  port: 8080

spring:
  application:
    name: api-gateway
  cloud:
    nacos:
      discovery:
        server-addr: nacos:8848
    gateway:
      routes:
        - id: order-service
          uri: lb://order-service
          predicates: [Path=/api/orders/**]
          filters:
            - StripPrefix=1
            - name: RequestRateLimiter
              args: { redis-rate-limiter.replenishRate: 10,
                      redis-rate-limiter.burstCapacity: 20 }
      httpclient:
        connect-timeout: 3000
        response-timeout: 15s        # ⚠️ > 下游最大超时

logging:
  level:
    org.springframework.cloud.gateway: INFO

# 依赖：gateway + nacos-discovery + redis-reactive + jwt
```

---

## 9. 核心要点

> 🎯 **核心要点**：
> 1. 网关四职责：路由/鉴权/限流/日志——**不做业务逻辑/权限细节**
> 2. Gateway（WebFlux 响应式）吞吐量为 Zuul 1.5 倍——2026 标准
> 3. 过滤器顺序：鉴权 → 限流 → 日志 → 路由（getOrder 控制）
> 4. 限流：令牌桶 + Redis（分布式）+ 按用户/接口维度 + 429 降级
> 5. **超时铁律**：网关超时 > 下游所有服务超时最大值（防提前截断）
> 6. 双层网关：Ingress（TLS/域名）+ Gateway（鉴权/路由）——职责分离
> 7. 鉴权边界：网关认证（你是谁）、服务授权（你能做什么）

---

**下一模块**：[05-熔断限流降级](05-熔断限流降级.md) | **返回总览**：[00-微服务架构知识体系总览](00-微服务架构知识体系总览.md)
