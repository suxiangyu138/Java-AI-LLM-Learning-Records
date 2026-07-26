# 07 - API 网关与服务网格

> 🎯 API 网关是微服务的统一入口（北向流量），Service Mesh 是服务间的通信层（东西向流量）— 两者结合构建完整的微服务流量治理体系

---

## 目录

1. [API 网关概述](#1-api-网关概述)
2. [Spring Cloud Gateway 实战](#2-spring-cloud-gateway-实战)
3. [流量治理：限流 / 熔断 / 灰度](#3-流量治理限流--熔断--灰度)
4. [Service Mesh 概述](#4-service-mesh-概述)
5. [Istio 核心架构](#5-istio-核心架构)
6. [网关 vs Service Mesh 对比](#6-网关-vs-service-mesh-对比)

---

## 1. API 网关概述

### 1.1 为什么需要网关

```
无网关（微服务直连）：
  Client ──→ Service-A:8080
         ──→ Service-B:8081     ← 客户端知道所有服务地址
         ──→ Service-C:8082

有网关（统一入口）：
  Client ──→ Gateway:80
              ├── /users/** → user-service
              ├── /orders/** → order-service
              └── /products/** → product-service
```

### 1.2 网关核心能力

| 能力 | 说明 | 实现 |
|------|------|------|
| **路由转发** | 将请求按路径/Header 路由到不同服务 | Route Predicates |
| **统一鉴权** | 在网关层校验 Token/Cookie | GlobalFilter |
| **限流** | 按 IP/用户/接口限流 | RequestRateLimiter |
| **熔断降级** | 后端故障时快速失败 | CircuitBreaker |
| **灰度发布** | 按权重/Header 分流到不同版本 | 自定义 LoadBalancer |
| **日志/监控** | 统一记录请求日志 | AccessLog + Micrometer |

---

## 2. Spring Cloud Gateway 实战

### 2.1 核心三要素

```
Route（路由）：
  id + 目标 URI + Predicates（断言）+ Filters（过滤器）

Predicate（断言）：
  → 匹配条件：Path=/api/**、Header、Cookie、Method 等

Filter（过滤器）：
  → 修改请求/响应：AddRequestHeader、StripPrefix、RateLimiter 等
```

### 2.2 配置实战

```yaml
spring:
  cloud:
    gateway:
      routes:
        # ═══ 路由规则 ═══
        - id: user-service
          uri: lb://user-service               # 负载均衡
          predicates:
            - Path=/api/users/**
          filters:
            - StripPrefix=1                     # 去掉 /api 前缀
            - AddRequestHeader=X-Gateway, true  # 添加自定义头

        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/orders/**
          filters:
            - name: RequestRateLimiter          # 限流
              args:
                redis-rate-limiter:
                  replenishRate: 10             # 每秒令牌数
                  burstCapacity: 20             # 突发容量
            - name: CircuitBreaker              # 熔断
              args:
                name: orderCircuitBreaker
                fallbackUri: forward:/fallback/order

      # ═══ 全局过滤器（鉴权） ═══
      default-filters:
        - DedupeResponseHeader=Access-Control-Allow-Origin
```

### 2.3 自定义过滤器

```java
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        // 解析 JWT，注入用户信息到 Header
        return chain.filter(exchange);
    }
}
```

---

## 3. 流量治理：限流 / 熔断 / 灰度

### 3.1 限流算法

| 算法 | 原理 | 特点 |
|------|------|------|
| **计数器** | 固定窗口内计数，超过阈值拒绝 | 简单但有临界突刺问题 |
| **滑动窗口** | 将固定窗口拆分为小格，平滑计算 | 精确但内存开销大 |
| **令牌桶** | 固定速率放令牌，请求消耗令牌 | ⭐ 允许突发，最常用 |
| **漏桶** | 请求进桶，固定速率流出 | 严格平滑，不允许突发 |

```text
令牌桶工作原理：
  ┌──────────┐
  │  令牌生成  │ → 速率 R（10 tokens/s）
  │  (refill) │
  └─────┬────┘
        ↓
  ┌──────────┐     请求来 → 取令牌成功 → 通过
  │  令牌桶   │ ←─────────  取令牌失败 → 拒绝
  │ (容量 C) │
  └──────────┘
```

### 3.2 灰度发布

```yaml
# 金丝雀发布：90% 流量到稳定版，10% 到新版本
spring:
  cloud:
    gateway:
      routes:
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/orders/**
        - id: order-service-canary
          uri: lb://order-service-canary
          predicates:
            - Path=/api/orders/**
            - Header=X-Canary, true           # Header 匹配 → 灰度版本
```

```text
灰度策略：
├── 权重灰度（Weight）：v1 90% + v2 10%
├── Header 灰度：X-Version: v2 → 路由到新版本
├── IP 灰度：内部 IP → 新版本，外部 IP → 旧版本
└── 用户灰度：白名单用户 → 新版本
```

---

## 4. Service Mesh 概述

### 4.1 为什么需要 Service Mesh

```
微服务的通信管理困境：
  → 每个服务都要处理：服务发现、负载均衡、重试、超时、熔断、限流、监控
  → 这些逻辑与业务代码耦合

Service Mesh 的解耦方案：
  → 将通信逻辑下沉到 Sidecar 代理（与应用容器共享 Pod）
  → 应用只管业务逻辑，Sidecar 处理所有通信问题
```

### 4.2 架构模型

```
         Service A                  Service B
  ┌──────────────────┐      ┌──────────────────┐
  │   Business Logic  │      │   Business Logic  │
  │   (业务代码)       │      │   (业务代码)       │
  └────────┬─────────┘      └────────┬─────────┘
           │                         │
           │ localhost               │ localhost
           │                         │
  ┌────────▼─────────┐      ┌────────▼─────────┐
  │  Envoy Sidecar   │◄────►│  Envoy Sidecar   │
  │ (服务发现/负载均衡  │ mTLS │ (重试/熔断/限流    │
  │  遥测/追踪/安全)   │      │  日志/监控)       │
  └──────────────────┘      └──────────────────┘
           │                         │
      Data Plane                Data Plane
           └──────────┬──────────────┘
                      │
              ┌───────▼───────┐
              │    Istiod     │  ← Control Plane
              │ (配置/策略分发) │
              └───────────────┘
```

---

## 5. Istio 核心架构

### 5.1 核心组件

| 组件 | 层 | 职责 |
|------|:---:|------|
| **Envoy** | Data Plane | Sidecar 代理，拦截所有进出流量 |
| **Istiod** | Control Plane | 配置分发、服务发现、证书管理 |

### 5.2 核心能力

| 能力 | 说明 | 实现 |
|------|------|------|
| **流量管理** | 路由、负载均衡、超时、重试、熔断 | VirtualService + DestinationRule |
| **安全** | 服务间 mTLS + 认证授权 | PeerAuthentication + AuthorizationPolicy |
| **可观测性** | 自动生成 Metrics/Logs/Traces | Envoy 内置遥测 |
| **灰度发布** | 权重路由、金丝雀、A/B 测试 | VirtualService weight |

```yaml
# Istio VirtualService — 金丝雀发布
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: user-service
spec:
  hosts:
    - user-service
  http:
    - match:
        - headers:
            version:
              exact: v2
      route:
        - destination:
            host: user-service
            subset: v2
    - route:
        - destination:
            host: user-service
            subset: v1
          weight: 90
        - destination:
            host: user-service
            subset: v2
          weight: 10
```

---

## 6. 网关 vs Service Mesh 对比

| 维度 | API 网关 | Service Mesh |
|------|----------|--------------|
| 流量方向 | 北向（外部→内部） | 东西向（内部→内部） |
| 部署位置 | 集群入口 | 每个 Pod 的 Sidecar |
| 典型实现 | Spring Cloud Gateway / Kong / Nginx | Istio / Linkerd |
| 侵入性 | 应用无侵入 | Sidecar 注入（对应用透明） |
| 复杂度 | ⭐⭐ | ⭐⭐⭐⭐ |
| 适用规模 | 所有微服务 | 大型微服务集群（50+ 服务） |

### 选型建议

```
小规模微服务（< 20 服务）：
  Spring Cloud Gateway + Sentinel → 够用，运维简单

中大规模（> 50 服务 + K8s）：
  Gateway + Istio → 网关管入口，Mesh 管内部

超大规模（> 200 服务）：
  全 Mesh 化 → Isito 统一治理，网关简化
```

> 🎯 **原则**：在真正需要之前不要引入 Service Mesh。先从 API 网关开始，当微服务数量多到手动管理通信策略成为瓶颈时，再考虑 Service Mesh。
