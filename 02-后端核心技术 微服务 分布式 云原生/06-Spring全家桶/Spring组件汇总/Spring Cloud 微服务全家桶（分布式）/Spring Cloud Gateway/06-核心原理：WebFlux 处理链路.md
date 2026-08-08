# 06 核心原理：WebFlux 处理链路

> 原理篇：**一次网关请求的内部旅程——HandlerMapping 路由匹配 → WebHandler 过滤链 → 代理转发**——谓词/过滤器模型的底层实现、Netty 非阻塞语义、与 Zuul 的原理对比；面试核心区。

---

## 📚 目录

1. [请求旅程总览](#1-请求旅程总览)
2. [第一站：HandlerMapping 路由匹配](#2-第一站handlermapping-路由匹配)
3. [第二站：过滤链组装（GatewayFilter + GlobalFilter）](#3-第二站过滤链组装gatewayfilter--globalfilter)
4. [第三站：转发执行（NettyRoutingFilter）](#4-第三站转发执行nettyroutingfilter)
5. [非阻塞语义：为什么高性能](#5-非阻塞语义为什么高性能)
6. [vs Zuul：原理对比](#6-vs-zuul原理对比)
7. [性能模型与调优方向](#7-性能模型与调优方向)

---

## 1. 请求旅程总览

```text
一次 GET /orders/1 的完整旅程（WebFlux 栈）：

客户端
  │ HTTP
  ▼
Reactor Netty Server（事件循环线程，非阻塞）
  │
  ▼
① RoutePredicateHandlerMapping（WebFlux 的 HandlerMapping 之一）
  │    遍历路由 → 谓词逐个求值（AND）
  │    命中？→ 记录 route 到 exchange 属性
  ▼
② GatewayWebHandler（WebHandler 实现）
  │    组装过滤链：GlobalFilter + 路由 GatewayFilter（按 order 排序）
  │    chain.filter(exchange) 逐个执行
  │      ├── AuthGlobalFilter（认证）
  │      ├── RouteToRequestUrlFilter（拼 URL）
  │      ├── LoadBalancerClientFilter（lb:// 解析实例）
  │      └── 路由过滤器链（改写/限流/熔断）
  ▼
③ NettyRoutingFilter
  │    用 Reactor Netty HttpClient 转发请求到下游
  ▼
下游服务 → 响应原路返回（过滤链反向执行，改响应头等）
```

> 🎯 一句话本质：**Gateway = 一个特殊的 WebFlux HandlerMapping + WebHandler**——路由匹配就是 HandlerMapping 的匹配逻辑，过滤链就是 WebHandler 的处理逻辑——**没有魔法，是 Spring WebFlux 扩展点**。

## 2. 第一站：HandlerMapping 路由匹配

### 2.1 模型

```text
Spring WebFlux 请求处理：DispatcherHandler 按 HandlerMapping 找 Handler
  └── Gateway 注册了 RoutePredicateHandlerMapping：
        ├── 遍历所有 Route（顺序）
        ├── 逐个执行 Route 的谓词（AND）
        ├── 全部 true → 命中，返回 RoutePredicateHandlerMapping.RouteHandler
        └── 无命中 → 404（没有 Handler）
```

| 组件 | 职责 |
|------|------|
| RoutePredicateHandlerMapping | 路由匹配（谓词求值） |
| RoutePredicateHandlerMapping.RouteHandler | 命中的"处理器"（触发 WebHandler 过滤链） |
| ServerWebExchange | 请求上下文（属性贯穿过滤链，谓词提取的变量放这） |

### 2.2 关键数据结构：ServerWebExchange

```java
// 谓词提取的变量：Path=/orders/{segment} → segment=1 存在这里
exchange.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE);   // Map<String,String>
// 命中的路由
exchange.getAttribute(GATEWAY_ROUTE_ATTR);                  // Route
// 目标 URL（过滤器改写后）
exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
```

> 💡 自定义过滤器经常操作 exchange 属性——**属性是谓词、过滤器、转发之间的"共享内存"**。

## 3. 第二站：过滤链组装（GatewayFilter + GlobalFilter）

### 3.1 组装过程

```text
GatewayWebHandler 收到命中的 Route：
  ① 取该 Route 的 GatewayFilter 列表（yaml/DSL 配置的）
  ② 取所有 GlobalFilter（按 order 排序）
  ③ 合并：GlobalFilter + Route 过滤器 → 按 order 升序排序 → 链式执行
  ④ chain.filter(exchange) → 下一个；最后一个过滤器调用 NettyRoutingFilter 转发
```

### 3.2 责任链模型

```java
public interface GatewayFilter {
    Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain);
    // 每个过滤器：处理 → chain.filter(exchange) 传给下一个
    // 短路：不调 chain.filter = 请求终止（如认证失败返回 401）
}
```

| 特性 | 说明 |
|------|------|
| 响应式链 | 整个链路 Mono 组合，非阻塞 |
| 短路 | 任意过滤器可终止（认证失败/限流 429/熔断兜底） |
| 双向 | 下游响应返回时经过同一链（修改响应头/体） |

## 4. 第三站：转发执行（NettyRoutingFilter）

### 4.1 转发实现

```text
NettyRoutingFilter（GlobalFilter，order 默认最后）：
  ├── 拿 GATEWAY_REQUEST_URL_ATTR（目标 URL，前面过滤器已拼好）
  ├── 用 Reactor Netty HttpClient（连接池复用）发起转发
  ├── 流式转发：请求体/响应体都是 Flux 流（不整体缓冲）
  └── 响应写回客户端（同样流式）
```

### 4.2 三类转发过滤器

| 过滤器 | 处理目标 | 说明 |
|--------|---------|------|
| LoadBalancerClientFilter | `lb://order-service` | 查注册中心 → 选实例 → 替换为 http://ip:port |
| NettyRoutingFilter | `http(s)://...` | 默认 HTTP 转发（Netty HttpClient） |
| ForwardRoutingFilter | `forward:/xxx` | 网关本地转发（fallbackUri 用它） |
| WebsocketRoutingFilter | `ws://` | WebSocket 转发 |

> 🎯 面试必答：**"lb:// 是怎么变成真实地址的？"**——**LoadBalancerClientFilter**（GlobalFilter）拦截 `lb://` 前缀：通过 Spring Cloud LoadBalancer 向注册中心查服务实例列表 → 按策略（默认轮询）选一个 → 把 URL 改写为 `http://ip:port` 写入 GATEWAY_REQUEST_URL_ATTR → 最后 NettyRoutingFilter 用真实地址转发。

## 5. 非阻塞语义：为什么高性能

### 5.1 对比阻塞模型

```text
传统 Servlet（Zuul 1.x / WebMVC 网关）：
  每请求占用一个线程（线程 = 1MB 栈），等待下游时线程挂起
  1000 并发 = 1000 线程（资源耗尽风险）

WebFlux（Gateway 默认）：
  少量事件循环线程（默认 = CPU 核数×2）处理所有请求
  等待下游时线程不阻塞（回调/异步），同一线程服务其他请求
  1000 并发 = 少量线程 + 大量事件
```

### 5.2 高性能的代价

| 维度 | 阻塞模型 | 非阻塞模型 |
|------|---------|-----------|
| 高并发线程开销 | 高（每请求一线程） | 低（事件循环复用） |
| 编程模型 | 同步直觉 | 响应式（Mono/Flux 学习曲线） |
| 阻塞代码 | 天然 | **必须避免**（在事件循环线程里做阻塞 IO = 灾难） |
| 调试 | 直观堆栈 | 异步堆栈难读 |
| 5.0 的 WebMVC 方案 | — | **虚拟线程版**：阻塞代码 + 接近非阻塞性能（[09 篇](09-WebMVC网关与ProxyExchange（5.0新特性）.md)） |

> ⚠️ **网关性能红线**：WebFlux 网关里**不要写阻塞代码**（Thread.sleep、同步 JDBC、同步 Redis）——阻塞事件循环线程等于自废武功；必须阻塞的场景用 `publishOn` 切线程池或选 WebMVC 栈。

## 6. vs Zuul：原理对比

| 维度 | Gateway（WebFlux） | Zuul 1.x（已废弃） | Zuul 2.x |
|------|-------------------|-------------------|----------|
| 模型 | WebFlux HandlerMapping | Servlet Filter | Netty 事件循环 |
| 阻塞/非阻塞 | **非阻塞** | 阻塞（每请求一线程） | 非阻塞 |
| 路由模型 | **谓词 + 过滤器工厂**（声明式） | filter 链（代码式） | filter 链 |
| 性能 | 高（事件循环） | 低（线程开销） | 高 |
| 配置 | yaml/DSL | Java 代码 | Java 代码 |
| 生态 | Spring Cloud 官方 | 2019 停止维护 | 未集成 Spring Cloud |
| 响应式 | ✅ | ❌ | 部分 |

> 🎯 面试必答：**"为什么弃 Zuul 选 Gateway？"**——三个理由：**① 性能模型**（Zuul 1 阻塞每请求一线程，Gateway 事件循环非阻塞）；**② 编程模型**（Zuul 过滤器全 Java 代码，Gateway 声明式谓词+过滤器 yaml 可配）；**③ 生态**（Zuul 1 停止维护、Zuul 2 未集成 Spring Cloud，Gateway 是官方继任者）。

## 7. 性能模型与调优方向

### 7.1 性能决定因素

```text
网关吞吐瓶颈排序：
  ① 过滤器里的阻塞操作（最致命：阻塞事件循环）
  ② 下游响应时间（网关 = 下游 RT + 网关开销）
  ③ 转发连接池（Netty HttpClient 连接复用率）
  ④ 路由匹配复杂度（谓词越多越慢，微秒级）
  ⑤ 限流/熔断/日志等横切成本（Redis 调用、日志 IO）
```

### 7.2 调优方向

| 方向 | 手段 |
|------|------|
| 连接池 | `spring.cloud.gateway.server.webflux.httpclient.pool.*`（max-connections 等） |
| 线程模型 | 默认 CPU×2；事件循环线程数与 CPU 绑定 |
| 响应时间 | 下游加超时配置（`httpclient.response-timeout`）防无限等待 |
| 日志 | 访问日志开/关权衡（IO 开销） |
| 过滤链 | 移除无用过滤器；认证/限流前置（失败早短路） |

> 💡 性能验证：**压测看三指标**——P99 延迟（网关开销应 <5ms）、吞吐（QPS）、线程/内存占用；**网关开销异常大时先查过滤器阻塞**（[10 篇](10-生产实践与选型避坑.md) 调优）。

---

**下一模块**：[07-网关级限流与熔断](07-网关级限流与熔断.md)　**返回总览**：[00-Spring Cloud Gateway知识体系总览](00-Spring Cloud Gateway知识体系总览.md)

**【参考来源】**：[Spring Cloud Gateway 5.0.2（How It Works）](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/)、[Spring Cloud Gateway 5.0.2（Global Filters）](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/)、[Spring 框架核心（WebFlux 底座）](../../../../Spring框架核心/)
