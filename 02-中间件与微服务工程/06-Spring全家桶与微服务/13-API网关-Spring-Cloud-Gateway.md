# 13-API网关-Spring-Cloud-Gateway
> 🎯 P1 就业必备 — Spring Cloud Gateway是微服务架构的流量入口，负责路由转发、鉴权、限流、日志等横切关注点，是微服务网关层的核心组件。基于WebFlux + Netty的非阻塞架构，性能远超传统Zuul 1.x。

---

## 目录
1. [本章总览](#1-本章总览)
2. [分层理论讲解](#2-分层理论讲解)
   - 2.1 [API之路由配置](#21-api之路由配置)
   - 2.2 [Predicate断言工厂](#22-predicate断言工厂)
   - 2.3 [Filter过滤体系](#23-filter过滤体系)
   - 2.4 [Gateway底层Netty非阻塞架构](#24-gateway底层netty非阻塞架构)
   - 2.5 [Gateway vs Zuul 1.x/2.x](#25-gateway-vs-zuul-1x2x)
3. [高频踩坑与误区](#3-高频踩坑与误区)
4. [随堂基础练习](#4-随堂基础练习)
5. [章节综合实操案例](#5-章节综合实操案例)
   - 5.1 [Gateway路由配置完整案例](#51-gateway路由配置完整案例)
   - 5.2 [Gateway + JWT鉴权过滤器](#52-gateway--jwt鉴权过滤器)
   - 5.3 [Gateway + Sentinel限流集成](#53-gateway--sentinel限流集成)
   - 5.4 [CORS跨域配置](#54-cors跨域配置)
6. [分层综合习题](#6-分层综合习题)
7. [本章复盘速记清单](#7-本章复盘速记清单)
8. [精通拓展补充-P2](#8-精通拓展补充-p2)

---

## 1. 本章总览

### 1.1 知识定位

| 项目 | 内容 |
|------|------|
| **归属** | Spring全家桶核心组件 → 层级2 P1就业必备 → Spring Cloud微服务生态 |
| **前置依赖** | Spring Boot、Spring WebFlux基础、Nacos/Eureka服务发现、微服务基础理论 |
| **重要性** | ⭐⭐⭐⭐⭐（微服务网关是流量入口，几乎每个微服务项目必备） |
| **学习难度** | 中等（概念清晰，但Filter/Predicate体系较丰富，底层Netty有一定门槛） |

### 1.2 三层学习目标

| 级别 | 目标 |
|------|------|
| **基础** | 理解网关作用，能搭建Gateway项目，配置yaml路由，理解Route/Predicate/Filter模型 |
| **熟练** | 掌握内置Predicate和Filter的使用，能编写自定义全局过滤器，配置CORS，集成JWT鉴权 |
| **精通** | 深入理解Netty非阻塞架构，编写自定义GatewayFilterFactory，集成Sentinel限流，理解Gateway性能调优 |

### 1.3 本章内容全景

```
Spring Cloud Gateway 知识全景
├── 基础概念
│   ├── 网关是什么：流量入口、统一管理横切关注点
│   ├── 网关 vs 反向代理 vs 负载均衡器
│   └── 网关核心能力：路由 / 鉴权 / 限流 / 日志 / 熔断
│
├── 三大核心模型
│   ├── Route（路由）：id + uri + predicates + filters
│   ├── Predicate（断言）：匹配请求条件，决定是否路由
│   └── Filter（过滤器）：请求/响应拦截与增强
│
├── Predicate体系（内置11种）
│   ├── 时间类：After / Before / Between
│   ├── 请求类：Path / Method / Header / Query / Host / RemoteAddr
│   └── 特殊类：Cookie / Weight
│
├── Filter体系
│   ├── 内置GatewayFilter（~30种）
│   ├── 自定义GatewayFilterFactory
│   └── 自定义GlobalFilter
│
├── 高级集成
│   ├── CORS配置
│   ├── Gateway + Sentinel 限流
│   ├── Gateway + JWT 鉴权
│   └── Gateway + Nacos 动态路由
│
└── 架构原理
    ├── WebFlux + Netty 非阻塞模型
    ├── Gateway vs Zuul 1.x/2.x 对比
    └── Gateway性能调优参数
```

---

## 2. 分层理论讲解

### 2.1 API之路由配置

#### 2.1.1 网关在微服务架构中的位置

```
                    ┌─────────────────────────────────────┐
                    │         客户端 (App/Web/第三方)        │
                    └──────────────┬──────────────────────┘
                                   │
                          ┌───────┴───────┐
                          │   DNS/CDN     │
                          └───────┬───────┘
                                  │
                     ┌────────────┴────────────┐
                     │    Spring Cloud Gateway   │
                     │   (统一入口：路由/鉴权/限流)  │
                     └──┬────┬────┬────┬────┬───┘
                        │    │    │    │    │
                   ┌────┴┐ ┌─┴──┐ ┌┴───┐ ┴┐  ┌┴────┐
                   │用户 │ │商品│ │订单│ │支付│  │...  │
                   │服务 │ │服务│ │服务│ │服务│  │     │
                   └────┘ └────┘ └────┘ └───┘  └────┘
                        │    │    │    │    │
                   ┌────┴────┴────┴────┴────┴────┐
                   │     Nacos/Eureka (注册中心)     │
                   └──────────────────────────────┘
```

**网关的核心职责：**
- **统一路由**：所有服务统一通过网关访问，客户端不直接调用微服务
- **统一鉴权**：在网关层做JWT/OAuth2校验，拦截非法请求
- **统一限流**：按路由或维度做流量控制，保护后端服务
- **统一日志**：记录所有请求的访问日志、响应时间
- **统一跨域**：集中配置CORS，避免每个微服务单独配置
- **协议转换**：外部HTTP → 内部RPC/gRPC

#### 2.1.2 Route路由模型

Route是Gateway中最核心的实体，由三个部分组成：

| 组件 | 说明 | 示例 |
|------|------|------|
| **id** | 路由唯一标识 | `user-service-route` |
| **uri** | 目标服务地址 | `lb://user-service` 或 `http://localhost:8081` |
| **predicates** | 断言数组（AND关系） | `Path=/api/user/**` |
| **filters** | 过滤器数组（有序执行） | `StripPrefix=1`, `AddRequestHeader=X-Token,xxx` |
| **metadata** | 元数据（可选） | 用于自定义逻辑的附加数据 |
| **order** | 优先级（数值越小优先级越高） | `0` |

#### 2.1.3 三种URI配置方式

```yaml
spring:
  cloud:
    gateway:
      routes:
        # 方式1：lb协议 + 服务名（推荐）→ 结合注册中心负载均衡
        - id: user-service
          uri: lb://user-service  # 从Nacos/Eureka动态获取服务地址
          predicates:
            - Path=/api/user/**

        # 方式2：固定URL → 直接转发到指定地址
        - id: fixed-route
          uri: http://192.168.1.100:8080
          predicates:
            - Path=/legacy/**

        # 方式3：websocket协议
        - id: websocket-route
          uri: ws://localhost:8080/ws
          predicates:
            - Path=/ws/**
```

> 💡 `lb://` 前缀表示启用负载均衡，Gateway会通过`LoadBalancerClient`从注册中心获取服务实例列表并进行负载均衡（默认轮询）。

#### 2.1.4 路由匹配优先级

当多个路由规则匹配同一个请求时，按`order`属性决定优先级（数值越小优先级越高）：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: specific-route     # 优先级高
          uri: lb://vip-service
          predicates:
            - Path=/api/user/vip/**
          order: -1

        - id: general-route      # 优先级低
          uri: lb://user-service
          predicates:
            - Path=/api/user/**
          order: 0
```

> ⚠️ 若不指定order，Spring Cloud Gateway按配置顺序匹配，匹配到第一个即终止。所以精确路由应放在通用路由之前。

#### 2.1.5 动态路由配置

**方式一：基于Nacos配置中心动态刷新**

```yaml
# bootstrap.yml
spring:
  cloud:
    nacos:
      config:
        server-addr: 127.0.0.1:8848
        file-extension: yaml
        data-id: gateway-routes.yaml
        group: GATEWAY_GROUP
```

在Nacos配置中心维护路由规则，修改后Gateway自动刷新。

**方式二：基于数据库 + 定时刷新**

```java
@Component
public class DynamicRouteService implements ApplicationEventPublisherAware {

    @Autowired
    private RouteDefinitionWriter routeDefinitionWriter;
    private ApplicationEventPublisher publisher;

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    /**
     * 增加路由
     */
    public String add(RouteDefinition definition) {
        routeDefinitionWriter.save(Mono.just(definition)).subscribe();
        this.publisher.publishEvent(new RefreshRoutesEvent(this));
        return "success";
    }

    /**
     * 更新路由
     */
    public String update(RouteDefinition definition) {
        try {
            delete(definition.getId());
        } catch (Exception e) {
            // 忽略
        }
        routeDefinitionWriter.save(Mono.just(definition)).subscribe();
        this.publisher.publishEvent(new RefreshRoutesEvent(this));
        return "success";
    }

    /**
     * 删除路由
     */
    public String delete(String id) {
        routeDefinitionWriter.delete(Mono.just(id)).subscribe();
        this.publisher.publishEvent(new RefreshRoutesEvent(this));
        return "success";
    }
}
```

#### 2.1.6 路由谓词(Predicate)的工作原理

```
请求到达
  │
  ├─→ Route1: Predicate匹配? ──→ 命中 → Filter链执行 → 转发到URI
  │       │
  │       └─→ 不匹配
  │
  ├─→ Route2: Predicate匹配? ──→ 命中 → Filter链执行 → 转发到URI
  │       │
  │       └─→ 不匹配
  │
  └─→ ... (继续匹配下一个路由)
      │
      全部不匹配 → 返回 404
```

---

### 2.2 Predicate断言工厂

#### 2.2.1 内置Predicate一览

Spring Cloud Gateway内置了11种Predicate，它们是`RoutePredicateFactory`的实现类，用于匹配HTTP请求的各种属性。

| Predicate | 作用 | 配置示例 |
|-----------|------|----------|
| **Path** | 按请求路径匹配 | `Path=/api/user/**,/api/order/**` |
| **Method** | 按HTTP方法匹配 | `Method=GET,POST` |
| **Header** | 按请求头匹配 | `Header=X-Request-Id, \d+` |
| **Query** | 按查询参数匹配 | `Query=token` 或 `Query=page, \d+` |
| **Cookie** | 按Cookie匹配 | `Cookie=sessionId, abc123` |
| **Host** | 按Host头匹配 | `Host=**.example.com` |
| **RemoteAddr** | 按客户端IP匹配 | `RemoteAddr=192.168.1.1/24` |
| **After** | 在指定时间之后 | `After=2025-01-01T00:00:00+08:00[Asia/Shanghai]` |
| **Before** | 在指定时间之前 | `Before=2026-12-31T23:59:59+08:00[Asia/Shanghai]` |
| **Between** | 在指定时间段内 | `Between=2025-06-01T00:00:00+08:00, 2025-09-01T00:00:00+08:00` |
| **Weight** | 按权重比例分流 | `Weight=group1, 80` |

#### 2.2.2 Path Predicate详解

**配置语法：**
```yaml
predicates:
  - Path=/api/user/**, /api/order/**
```

**匹配规则：**

| Pattern | 示例路径 | 是否匹配 |
|----------|----------|---------|
| `/api/user/**` | `/api/user/list` | ✅ |
| `/api/user/**` | `/api/user/detail/1001` | ✅ |
| `/api/user/*` | `/api/user/list` | ✅ |
| `/api/user/*` | `/api/user/detail/1001` | ❌（\*只匹配一级） |
| `/api/{segment}/**` | `/api/user/list` | ✅（支持占位符） |

**Java配置方式：**
```java
@Bean
public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
    return builder.routes()
        .route("path-route", r -> r
            .path("/api/user/**")
            .uri("lb://user-service"))
        .build();
}
```

#### 2.2.3 Method Predicate

```yaml
predicates:
  - Method=GET,POST
```

匹配GET或POST请求，其他方法（PUT、DELETE、PATCH等）不匹配。

#### 2.2.4 Header Predicate

```yaml
# 检查请求头是否存在，且值匹配正则
predicates:
  - Header=X-Request-Id, \d+         # X-Request-Id必须存在且是数字
  - Header=Authorization, Bearer .*  # Authorization必须是Bearer开头
```

> 💡 第二个参数是正则表达式。只写`Header=X-Request-Id`表示只检查Header是否存在，不校验值。

#### 2.2.5 Query Predicate

```yaml
predicates:
  - Query=token          # 请求参数中必须包含token（不校验值）
  - Query=page, \d+      # page参数必须存在且值为数字
  - Query=name, .+       # name参数不能为空
```

#### 2.2.6 Host Predicate

```yaml
predicates:
  - Host=api.example.com           # 精确匹配
  - Host=**.example.com            # 匹配所有example.com子域名
  - Host=*.api.example.com         # 匹配一级子域名
```

#### 2.2.7 Cookie Predicate

```yaml
predicates:
  - Cookie=sessionId, abc123   # Cookie中必须包含sessionId=abc123
  - Cookie=token, .+           # Cookie中必须包含token且值非空
```

#### 2.2.8 After / Before / Between Predicate

```yaml
predicates:
  # 仅接受2025-07-01 00:00:00之后的请求（用于灰度发布定时开放）
  - After=2025-07-01T00:00:00+08:00[Asia/Shanghai]

  # 仅接受2026-12-31 23:59:59之前的请求
  - Before=2026-12-31T23:59:59+08:00[Asia/Shanghai]

  # 仅接受指定时间段内的请求
  - Between=2025-07-01T00:00:00+08:00[Asia/Shanghai], 2025-09-01T00:00:00+08:00[Asia/Shanghai]
```

**时间格式说明：** 必须使用带时区的ZonedDateTime格式。推荐使用 `+08:00[Asia/Shanghai]` 避免夏令时问题。

#### 2.2.9 Weight Predicate（灰度发布）

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: weight-v1
          uri: lb://user-service-v1
          predicates:
            - Path=/api/user/**
            - Weight=user-service-group, 80   # 80%流量
        - id: weight-v2
          uri: lb://user-service-v2
          predicates:
            - Path=/api/user/**
            - Weight=user-service-group, 20   # 20%流量
```

> 🎯 **灰度发布场景**：将20%流量转发到新版本服务，80%到旧版本，验证新版本稳定性后再逐步切量。

#### 2.2.10 RemoteAddr Predicate

```yaml
predicates:
  - RemoteAddr=192.168.1.0/24   # 仅允许内网IP访问
  - RemoteAddr=10.0.0.0/8      # 仅允许10段IP访问
```

#### 2.2.11 Predicate组合规则

多个Predicate之间是 **AND 关系**，必须全部匹配才算命中路由。

```yaml
predicates:
  - Path=/api/admin/**
  - Method=POST
  - Header=X-Admin-Token, .+
  - RemoteAddr=192.168.0.0/16
# 含义：必须是POST请求 + 路径以/api/admin/开头 + 有X-Admin-Token请求头 + 来自内网
```

若要实现 OR 关系，需要将同一个路由拆分为多个路由定义，或者使用自定义Predicate。

---

### 2.3 Filter过滤体系

#### 2.3.1 Filter分类

Spring Cloud Gateway的Filter分为两大类：

```
Gateway Filter 体系
├── GatewayFilter（局部过滤器）
│   ├── 内置 ~30种：AddRequestHeader / StripPrefix / PrefixPath / ...
│   └── 自定义：实现 GatewayFilterFactory 接口
│
├── GlobalFilter（全局过滤器）
│   ├── 系统内置：NettyRoutingFilter / ForwardRoutingFilter / ...
│   └── 自定义：实现 GlobalFilter + Ordered 接口
│
└── 执行顺序：在路由配置中定义 order 属性
    ├── 数字越小越先执行
    └── GlobalFilter 默认所有路由生效
```

> 💡 **GatewayFilter** 作用在某一个路由上，可以通过 `filters:` 配置；**GlobalFilter** 作用于所有路由，需要在代码中定义。

#### 2.3.2 内置GatewayFilter API参考

| Filter Factory | 作用 | 配置示例 |
|---------------|------|----------|
| **AddRequestHeader** | 添加请求头 | `AddRequestHeader=X-Request-Source, gateway` |
| **AddRequestParameter** | 添加请求参数 | `AddRequestParameter=from, gateway` |
| **AddResponseHeader** | 添加响应头 | `AddResponseHeader=X-Response-Source, gateway` |
| **RemoveRequestHeader** | 移除请求头 | `RemoveRequestHeader=X-Internal-Token` |
| **RemoveResponseHeader** | 移除响应头 | `RemoveResponseHeader=X-Internal-Header` |
| **SetRequestHeader** | 设置请求头（覆盖） | `SetRequestHeader=X-User-Id, 1001` |
| **SetResponseHeader** | 设置响应头（覆盖） | `SetResponseHeader=X-Version, v2` |
| **PrefixPath** | 添加路径前缀 | `PrefixPath=/api` |
| **StripPrefix** | 去除路径前缀 | `StripPrefix=1` |
| **RewritePath** | 重写路径（正则） | `RewritePath=/api/user/(?<seg>.*), /$\{seg}` |
| **SetPath** | 直接设置路径 | `SetPath=/default-path` |
| **RedirectTo** | 重定向 | `RedirectTo=302, https://www.example.com` |
| **Retry** | 请求重试 | `Retry=3` |
| **RequestSize** | 请求大小限制 | `RequestSize=5000000` |
| **RequestRateLimiter** | 请求限流 | `RequestRateLimiter=10, 20` |
| **CircuitBreaker** | 熔断降级 | `CircuitBreaker=myCircuitBreaker` |
| **FallbackHeaders** | 熔断降级头信息 | `FallbackHeaders` |
| **CacheRequestBody** | 缓存请求体 | `CacheRequestBody` |
| **DedupeResponseHeader** | 去重响应头 | `DedupeResponseHeader=Access-Control-Allow-Origin` |
| **SecureHeaders** | 添加安全头 | `SecureHeaders` |
| **SaveSession** | 保存Session | `SaveSession` |

#### 2.3.3 常用Filter详解

**① StripPrefix — 去除路径前缀**

```yaml
filters:
  - StripPrefix=1
# 请求 /api/user/list → 转发到后端 /user/list
```

```
客户端请求: GET /api/user/list
       ↓ Gateway (StripPrefix=1)
后端收到:   GET /user/list
```

**② PrefixPath — 添加路径前缀**

```yaml
filters:
  - PrefixPath=/api
# 请求 /user/list → 转发到后端 /api/user/list
```

**③ RewritePath — 正则重写路径**

```yaml
filters:
  - RewritePath=/api/user/(?<segment>.*), /$\{segment}
# 请求 /api/user/detail/1001 → 转发到后端 /detail/1001
```

> ⚠️ 在yaml中，`${segment}` 需要转义为 `$\{segment}`，否则会被Spring解析为占位符。

**④ AddRequestHeader / AddRequestParameter**

```yaml
filters:
  - AddRequestHeader=X-Gateway-Instance, ${spring.cloud.client.hostname}
  - AddRequestParameter=from, gateway
```

常用于在网关层向下游传递认证信息、网关元数据等。

**⑤ Retry — 请求重试**

```yaml
filters:
  - name: Retry
    args:
      retries: 3        # 重试次数
      statuses:         # 哪些状态码触发重试
        - BAD_GATEWAY
        - SERVICE_UNAVAILABLE
      methods:          # 哪些HTTP方法触发重试
        - GET
      series:           # 响应序列
        - SERVER_ERROR
      exceptions:       # 哪些异常触发重试
        - java.io.IOException
        - java.util.concurrent.TimeoutException
```

> ⚠️ Retry Filter 只在GET等幂等方法上启用，不要在POST/PUT上使用，避免重复提交。

**⑥ RequestRateLimiter — 请求限流**

```yaml
filters:
  - name: RequestRateLimiter
    args:
      key-resolver: "#{@userKeyResolver}"          # 限流键解析器Bean
      redis-rate-limiter.replenishRate: 10         # 每秒令牌数
      redis-rate-limiter.burstCapacity: 20         # 令牌桶容量
```

需要引入`spring-boot-starter-data-redis-reactive`依赖，并定义`KeyResolver` Bean。

#### 2.3.4 自定义GlobalFilter

实现`GlobalFilter`接口和`Ordered`接口：

```java
@Component
@Slf4j
public class RequestLogGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 前置处理：请求到达时
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethodValue();
        String ip = request.getRemoteAddress() != null ?
                request.getRemoteAddress().getAddress().getHostAddress() : "unknown";

        long startTime = System.currentTimeMillis();

        // 封装exchange，添加自定义属性
        exchange.getAttributes().put("startTime", startTime);
        exchange.getAttributes().put("requestId", UUID.randomUUID().toString());

        log.info("[Gateway] 收到请求: {} {} | IP: {} | RequestId: {}",
                method, path, ip, exchange.getAttributes().get("requestId"));

        // 执行过滤器链
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            // 后置处理：响应返回时
            long duration = System.currentTimeMillis() - startTime;
            int status = exchange.getResponse().getStatusCode() != null ?
                    exchange.getResponse().getStatusCode().value() : -1;

            log.info("[Gateway] 响应完成: {} {} | Status: {} | 耗时: {}ms",
                    method, path, status, duration);
        }));
    }

    @Override
    public int getOrder() {
        return -1; // 数字越小优先级越高，负值表示在最前面执行
    }
}
```

#### 2.3.5 自定义GatewayFilterFactory

```java
@Component
@Slf4j
public class CheckAuthGatewayFilterFactory
        extends AbstractGatewayFilterFactory<CheckAuthGatewayFilterFactory.Config> {

    public CheckAuthGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String token = request.getHeaders().getFirst(config.getAuthHeader());

            if (token == null || token.isEmpty()) {
                log.warn("[CheckAuth] 缺少认证头: {}", config.getAuthHeader());
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                String body = "{\"code\":401,\"message\":\"Missing auth header: "
                        + config.getAuthHeader() + "\"}";
                DataBuffer buffer = response.bufferFactory()
                        .wrap(body.getBytes(StandardCharsets.UTF_8));
                return response.writeWith(Mono.just(buffer));
            }

            // 将token传递给下游服务
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header(config.getAuthHeader(), token)
                    .build();
            ServerWebExchange modifiedExchange = exchange.mutate()
                    .request(modifiedRequest)
                    .build();

            return chain.filter(modifiedExchange);
        };
    }

    @Data
    public static class Config {
        private String authHeader = "Authorization";
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Collections.singletonList("authHeader");
    }
}
```

**yaml中使用：**
```yaml
filters:
  - CheckAuth=X-Token
```

#### 2.3.6 Filter执行顺序

```
请求 → 所有GlobalFilter(order=-∞) → 路由的GatewayFilter → 路由目标服务
       ↑ order值小优先                                ↑ 顺序执行
       ↓ 响应反向                                      ↓ 响应反向返回
响应 ← 所有GlobalFilter(order=-∞) ← 路由的GatewayFilter ← 路由目标服务
```

> 💡 前置逻辑按order从小到大执行，后置逻辑按order从大到小执行（类似AOP的环绕通知）。

---

### 2.4 Gateway底层Netty非阻塞架构

#### 2.4.1 架构演进：Tomcat → Netty

```
传统Servlet（阻塞I/O）
┌─────────────────────────────────────────┐
│  Tomcat                                 │
│  ┌──────┐  ┌──────┐  ┌──────┐          │
│  │Thread│  │Thread│  │Thread│  ...     │
│  │ ①    │  │ ②    │  │ ③    │          │
│  └──┬───┘  └──┬───┘  └──┬───┘          │
│     │         │         │              │
│  请求1      请求2      请求3           │
│     │(阻塞)    │(阻塞)    │(阻塞)       │
│    DB ────   DB ────   DB ────         │
│  (线程等待)  (线程等待)  (线程等待)      │
└─────────────────────────────────────────┘
问题：线程阻塞浪费资源，连接数受限

Spring WebFlux（非阻塞I/O）
┌─────────────────────────────────────────┐
│  Netty                                  │
│  ┌──────────────────────────────┐       │
│  │     EventLoop Group          │       │
│  │  ┌──────┐  ┌──────┐         │       │
│  │  │Event │  │Event │  ...    │       │
│  │  │Loop① │  │Loop② │         │       │
│  │  └──┬───┘  └──┬───┘         │       │
│  │     │         │             │       │
│  │  请求1      请求2           │       │
│  │     │         │             │       │
│  │  请求3      请求4           │       │
│  │     │         │             │       │
│  │  (所有请求共享事件循环)       │       │
│  └──────────────────────────────┘       │
│  Reactor (Reactive Streams)             │
│  Flux / Mono (异步非阻塞)               │
└─────────────────────────────────────────┘
优势：少量线程处理大量连接，高并发低资源消耗
```

#### 2.4.2 WebFlux核心模型

```
Spring Cloud Gateway
├── Spring WebFlux（反应式Web框架）
│   ├── 基于 Reactor 库（Mono/Flux）
│   ├── 完全异步非阻塞
│   └── 不再依赖 Servlet API
│
├── Netty（底层HTTP服务器）
│   ├── Reactor多线程模型
│   │   ├── Boss Group：接受连接，1~N个线程
│   │   └── Work Group：处理I/O，N个线程（默认CPU核数*2）
│   └── 零拷贝（ByteBuf直接内存）
│
└── Reactor（响应式流实现）
    ├── Mono：0~1个元素的异步序列
    └── Flux：0~N个元素的异步序列
```

#### 2.4.3 关键配置参数

```yaml
spring:
  cloud:
    gateway:
      httpclient:
        # 连接池
        pool:
          type: elastic              # 连接池类型：elastic(弹性)/fixed(固定)
          max-connections: 1000      # 最大连接数
          max-idle-time: 60000      # 最大空闲时间(ms)
          max-life-time: 300000     # 最大存活时间(ms)
          acquire-timeout: 45000    # 获取连接超时(ms)
        # 连接超时
        connect-timeout: 5000       # 连接超时(ms)
        response-timeout: 30s       # 响应超时
        # SSL
        ssl:
          use-insecure-trust-manager: false

      # 全局过滤器默认配置
      default-filters:
        - AddResponseHeader=X-Response-Gateway, Spring-Cloud-Gateway

      # 路由配置
      routes:
        - id: example
          uri: lb://example-service
          predicates:
            - Path=/example/**
```

**Netty服务端配置：**
```yaml
server:
  port: 8080
  netty:
    connection-timeout: 5000
    max-initial-line-length: 4096
    max-chunk-size: 8192
    max-request-size: 10485760  # 10MB
```

#### 2.4.4 请求处理流程源码级分析

```
1. NettyServer 接收 HTTP 请求
   │
2. ReactorHttpHandlerAdapter 适配
   │
3. DispatcherHandler.handle() 分发
   │
4. RoutePredicateHandlerMapping.getHandler()
   │  └─→ 遍历 Route 列表，执行 Predicate 匹配
   │
5. FilteringWebHandler.handle()
   │  └─→ 构建 Filter 链（GlobalFilters + GatewayFilters）
   │      └─→ 执行 filter chain
   │
6. NettyRoutingFilter（核心）
   │  └─→ 通过 HttpClient 转发请求到目标服务
   │      └─→ 使用 Netty 的 EventLoop 异步发送
   │
7. 目标服务返回响应
   │
8. Filter 链反向执行（后置逻辑）
   │
9. 响应写回客户端
```

---

### 2.5 Gateway vs Zuul 1.x/2.x

#### 2.5.1 全面对比

| 对比维度 | Zuul 1.x | Zuul 2.x | Spring Cloud Gateway |
|----------|----------|----------|---------------------|
| **发布时间** | 2013 | 2018 | 2017 |
| **底层I/O模型** | 阻塞BIO（Tomcat） | 非阻塞NIO（Netty） | 非阻塞NIO（Netty + WebFlux） |
| **Servlet API** | 依赖Servlet 2.5+ | 不依赖 | 不依赖（基于WebFlux） |
| **线程模型** | 每个请求一个线程 | EventLoop非阻塞 | EventLoop非阻塞 |
| **性能** | 低（线程阻塞，连接数受限） | 高 | 高（略优于Zuul 2） |
| **长连接** | 不支持（短连接） | 支持 | 支持 |
| **WebSocket** | 不支持 | 支持 | 支持 |
| **社区活跃度** | 维护模式（几乎停更） | 低 | 高（Spring官方维护） |
| **Spring Cloud集成** | Spring Cloud Netflix | 第三方集成 | Spring Cloud官方推荐 |
| **限流** | 需自行实现 | 需自行实现 | 内置RequestRateLimiter |
| **熔断** | 集成Hystrix | 需自行集成 | 集成Spring Cloud CircuitBreaker |
| **配置方式** | Java + properties | properties | Java + yaml（声明式） |
| **动态路由** | 支持（需配合Spring Cloud Bus） | 支持 | 原生支持 |
| **学习曲线** | 低 | 中 | 中（需理解响应式编程） |
| **生产推荐度** | ❌ 不推荐 | ⚠️ 谨慎使用 | ✅ **强烈推荐** |

#### 2.5.2 Zuul 1.x 性能瓶颈

```
Zuul 1.x 线程模型（BIO）
┌─────────────────────────────────┐
│  Tomcat 线程池                   │
│  ┌──────┐  ┌──────┐  ┌──────┐  │
│  │Thread│  │Thread│  │Thread│  │
│  │  1   │  │  2   │  │  3   │  │
│  └──┬───┘  └──┬───┘  └──┬───┘  │
│     │         │         │      │
│  请求1 ──── 请求2 ──── 请求3   │
│     │(I/O)    │(I/O)    │(I/O) │
│  后端服务 ←─ 后端服务 ←─ 后端服务│
│  (线程等待)  (线程等待)  (线程等待)│
└─────────────────────────────────┘

问题1：线程与连接1:1，200个请求需要200个线程
问题2：线程等待I/O时浪费CPU时间片
问题3：高并发下线程上下文切换开销巨大
问题4：默认最大连接数受限（Tomcat默认200）
```

#### 2.5.3 Gateway 性能优势

```
Gateway 线程模型（NIO）
┌─────────────────────────────────────┐
│  Netty EventLoop Group              │
│  ┌───────────┐  ┌───────────┐       │
│  │ EventLoop1│  │ EventLoop2│  ...  │
│  │  (线程1)   │  │  (线程2)   │       │
│  └─────┬─────┘  └─────┬─────┘       │
│        │              │             │
│    ┌───┴───┐      ┌───┴───┐        │
│    │Channel│      │Channel│        │
│    │  ①    │      │  ②    │        │
│    │  ③    │      │  ④    │        │
│    │  ⑤    │      │  ...  │        │
│    └───────┘      └───────┘        │
│  (一个EventLoop管理多个Channel)     │
│  (I/O操作非阻塞，不等待)            │
└─────────────────────────────────────┘

优势：8核CPU → 16个EventLoop线程 → 支持数万并发连接
```

> 🎯 **总结**：Gateway是Spring官方推荐的网关方案，性能优越，功能丰富，社区活跃。Zuul 1.x已进入维护模式，不应在新项目中使用。Zuul 2.x虽然有Netty支持但社区和生态远不如Gateway。

---

## 3. 高频踩坑与误区

### 3.1 常见错误及解决方案

#### 错误1：Gateway启动报错 — 引入spring-boot-starter-web依赖

**错误信息：**
```
IllegalStateException: The following dependencies are required to be excluded from
Spring Cloud Gateway: spring-boot-starter-web
```

**根因：** Gateway基于WebFlux，而`spring-boot-starter-web`基于Servlet（Tomcat），两者冲突。

**解决方案：** 排除spring-boot-starter-web依赖

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>

<!-- 绝对不要再引入这个 -->
<!-- <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency> -->
```

如果项目中存在其他间接引入了spring-boot-starter-web的依赖，需要排除：

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>some-dependency</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

#### 错误2：Gateway的路由不生效，总是404

**错误信息：**
```
访问 http://localhost:8080/api/user/list 返回 404
```

**调试步骤：**
1. 检查yaml缩进是否正确
2. 检查路由id是否重复
3. 检查目标服务是否已注册到Nacos
4. 检查路径匹配规则

**快速诊断：** 开启Gateway的DEBUG日志

```yaml
logging:
  level:
    org.springframework.cloud.gateway: DEBUG
    org.springframework.http.server.reactive: DEBUG
```

#### 错误3：RewritePath中`${segment}`被Spring解析

**错误现象：** `$`符号被当作Spring占位符解析，导致`${segment}`被替换为空字符串。

**根因：** yaml中`${...}`会被Spring Boot解析为属性占位符。

**解决方案：** 使用`$\{segment}`转义：

```yaml
# 错误写法
filters:
  - RewritePath=/api/user/(?<seg>.*), /${seg}

# 正确写法
filters:
  - RewritePath=/api/user/(?<seg>.*), /$\{seg}
```

#### 错误4：StripPrefix和PrefixPath混淆

```yaml
# 请求路径: /api/v1/user/list

# StripPrefix=1 → 去掉第一级路径
filters:
  - StripPrefix=1
# 转发到后端: /v1/user/list

# StripPrefix=2 → 去掉两级路径
filters:
  - StripPrefix=2
# 转发到后端: /user/list

# PrefixPath=/api → 添加前缀
filters:
  - PrefixPath=/api
# 转发到后端: /api/api/v1/user/list （注意：是在原始路径上加前缀）
```

> 💡 同时使用StripPrefix和PrefixPath时，执行顺序是：StripPrefix先执行（去除前缀），然后PrefixPath再执行（添加前缀）。

#### 错误5：跨域问题 — CORS配置无效

**现象：** 前端访问Gateway时报跨域错误，虽然配置了CORS。

**根因：** CORS配置被路由规则覆盖，或者CORS Filter顺序不对。

**解决方案：**

```java
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
```

#### 错误6：Gateway + Sentinel限流不生效

**错误现象：** 配置了Sentinel Gateway限流，但测试时发现不限流。

**可能原因：**
1. 未引入sentinel-spring-cloud-gateway-adapter依赖
2. Sentinel控制台未连接
3. 规则推送给Gateway的方式不对

**检查步骤：**
```yaml
# 1. 确认依赖
# 2. 确认Sentinel控制台配置
spring:
  cloud:
    sentinel:
      transport:
        dashboard: localhost:8080  # Sentinel控制台地址
      eager: true  # 提前初始化
```

#### 错误7：Gateway请求超时

**原因分析：** Gateway默认响应超时时间较短，后端服务处理慢时容易超时。

```yaml
spring:
  cloud:
    gateway:
      httpclient:
        connect-timeout: 5000       # 连接超时5秒
        response-timeout: 30s       # 响应超时30秒
```

或者对特定路由设置超时：

```yaml
- id: timeout-route
  uri: lb://slow-service
  predicates:
    - Path=/api/slow/**
  metadata:
    response-timeout: 60000
    connect-timeout-ms: 10000
```

#### 错误8：Filter修改请求体/响应体不生效

**根因：** 请求体（Body）只能被读取一次，Gateway中默认只能读取一次。

**解决方案：** 使用`CacheRequestBody` Filter或参考如下方式：

```java
@Component
@Slf4j
public class ModifyBodyGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 修改请求体
        ServerRequest serverRequest = ServerRequest.create(exchange,
                new ServerRequest.ServerRequestMessageReaders());
        Mono<String> modifiedBody = serverRequest.bodyToMono(String.class)
                .map(body -> body.replace("old", "new"));

        BodyInserter<String, ReactiveHttpOutputMessage> bodyInserter =
                BodyInserters.fromPublisher(modifiedBody, String.class);

        CachedBodyOutputMessage outputMessage = new CachedBodyOutputMessage(exchange,
                exchange.getRequest().getHeaders());

        return bodyInserter.insert(outputMessage, new BodyInserterContext())
                .then(Mono.defer(() -> {
                    ServerHttpRequest decorator = new ServerHttpRequestDecorator(exchange.getRequest()) {
                        @Override
                        public HttpHeaders getHeaders() {
                            HttpHeaders headers = new HttpHeaders();
                            headers.putAll(super.getHeaders());
                            headers.setContentLength(outputMessage.getCachedBody().block().readableByteCount());
                            return headers;
                        }

                        @Override
                        public Flux<DataBuffer> getBody() {
                            return outputMessage.getBody();
                        }
                    };
                    return chain.filter(exchange.mutate().request(decorator).build());
                }));
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
```

> ⚠️ 修改请求体/响应体是Gateway的进阶操作，涉及`DataBuffer`管理，稍有不慎会导致内存泄漏。建议优先使用Header/Parameter传递信息，仅在必要时修改Body。

### 3.2 误区澄清

**误区1：** Gateway基于Tomcat — **错误**，Gateway基于Netty + WebFlux。

**误区2：** Gateway是Zuul的替代品 — **正确**，Spring官方推荐从Zuul迁移到Gateway。

**误区3：** Gateway所有Filter都是全局的 — **错误**，GatewayFilter作用于特定路由，GlobalFilter才是全局的。

**误区4：** Predicate是AND关系不能改成OR — **正确**，但可以通过Weight或自定义Predicate实现类似OR效果。

**误区5：** Gateway只能做HTTP代理 — **错误**，Gateway还支持TCP、WebSocket、gRPC等协议转发。

**误区6：** Gateway启动必须依赖注册中心 — **错误**，可以用`http://`固定地址，不依赖注册中心。

**误区7：** 用了Gateway就不需要Nginx了 — **不准确**，Gateway是应用层网关，Nginx是负载均衡/反向代理层，通常Nginx在前、Gateway在后，各司其职。

---

## 4. 随堂基础练习

### 练习1: 基础路由配置

**题目：** 请编写Gateway的yaml配置，实现以下路由规则：
1. 所有`/api/user/**`的请求转发到`lb://user-service`服务
2. 所有`/api/order/**`的请求转发到`lb://order-service`服务
3. 为路由添加请求日志头`X-Gateway-Route: user-route`

**答案：**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/user/**
          filters:
            - AddRequestHeader=X-Gateway-Route, user-route

        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/order/**
```

### 练习2: Predicate组合

**题目：** 请编写一个路由配置，只允许 `POST` 方法且请求头包含 `X-Admin-Token` 的`/api/admin/**`请求通过。

**答案：**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: admin-api
          uri: lb://admin-service
          predicates:
            - Path=/api/admin/**
            - Method=POST
            - Header=X-Admin-Token, .+
```

### 练习3: 路径重写

**题目：** 客户端请求 `/api/v2/user/list`，后端服务实际路径为 `/user/list`。请使用两种方式实现。

**答案：**
```yaml
# 方式一：StripPrefix
spring:
  cloud:
    gateway:
      routes:
        - id: user-service-strip
          uri: lb://user-service
          predicates:
            - Path=/api/v2/user/**
          filters:
            - StripPrefix=2

# 方式二：RewritePath
spring:
  cloud:
    gateway:
      routes:
        - id: user-service-rewrite
          uri: lb://user-service
          predicates:
            - Path=/api/v2/user/**
          filters:
            - RewritePath=/api/v2/user/(?<seg>.*), /$\{seg}
```

### 练习4: 灰度发布配置

**题目：** 实现一个灰度发布方案，新版本`v2-user-service`承接10%流量，旧版本`v1-user-service`承接90%流量。

**答案：**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service-v1
          uri: lb://v1-user-service
          predicates:
            - Path=/api/user/**
            - Weight=user-gray, 90

        - id: user-service-v2
          uri: lb://v2-user-service
          predicates:
            - Path=/api/user/**
            - Weight=user-gray, 10
```

---

## 5. 章节综合实操案例

### 5.1 Gateway路由配置完整案例

#### 5.1.1 项目依赖 (pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.example</groupId>
        <artifactId>cloud-demo</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>gateway-server</artifactId>
    <packaging>jar</packaging>
    <description>Spring Cloud Gateway 网关服务</description>

    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2022.0.4</spring-cloud.version>
        <spring-boot.version>3.1.5</spring-boot.version>
    </properties>

    <dependencies>
        <!-- Gateway核心（包含WebFlux + Netty） -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway</artifactId>
        </dependency>

        <!-- 服务发现 Nacos -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>

        <!-- 负载均衡 -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-loadbalancer</artifactId>
        </dependency>

        <!-- 熔断降级 -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId>
        </dependency>

        <!-- Redis（用于限流） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.11.5</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.11.5</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.11.5</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Actuator -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
    </dependencies>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

#### 5.1.2 application.yml 完整配置

```yaml
server:
  port: 8080

spring:
  application:
    name: gateway-server

  cloud:
    # Nacos 注册中心
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
        namespace: dev
        group: DEFAULT_GROUP

    # Gateway 路由配置
    gateway:
      # 全局过滤器
      default-filters:
        - DedupeResponseHeader=Access-Control-Allow-Origin, RETAIN_UNIQUE
        - AddResponseHeader=X-Gateway-Version, 1.0.0

      # 路由规则
      routes:
        # ========== 用户服务 ==========
        - id: user-service
          uri: lb://user-service
          order: 0
          predicates:
            - Path=/api/user/**
          filters:
            - StripPrefix=1
            - AddRequestHeader=X-Source, gateway
            - name: Retry
              args:
                retries: 2
                methods: GET
                statuses: BAD_GATEWAY

        # ========== 订单服务 ==========
        - id: order-service
          uri: lb://order-service
          order: 0
          predicates:
            - Path=/api/order/**
          filters:
            - StripPrefix=1

        # ========== 商品服务 ==========
        - id: product-service
          uri: lb://product-service
          order: 0
          predicates:
            - Path=/api/product/**
          filters:
            - StripPrefix=1
            - name: CircuitBreaker
              args:
                name: productCircuitBreaker
                fallbackUri: forward:/fallback/product

        # ========== 鉴权服务（无需登录） ==========
        - id: auth-service
          uri: lb://auth-service
          order: -1
          predicates:
            - Path=/api/auth/login, /api/auth/register
            - Method=POST

      # HTTP客户端配置
      httpclient:
        connect-timeout: 5000
        response-timeout: 30s
        pool:
          type: elastic
          max-connections: 1000
          max-idle-time: 60000

    # 负载均衡配置
    loadbalancer:
      retry:
        enabled: true
      cache:
        ttl: 5s

# Redis配置（用于限流）
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      timeout: 3000

# Actuator 端点
management:
  endpoints:
    web:
      exposure:
        include: gateway, health, info
  endpoint:
    gateway:
      enabled: true

# 日志
logging:
  level:
    org.springframework.cloud.gateway: INFO
    com.example.gateway: DEBUG
```

#### 5.1.3 GatewayApplication启动类

```java
package com.example.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```

#### 5.1.4 全局路由日志过滤器

```java
package com.example.gateway.filter;

@Component
@Slf4j
public class AccessLogGlobalFilter implements GlobalFilter, Ordered {

    private static final String START_TIME = "startTime";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethodValue();
        String query = request.getURI().getRawQuery();
        String ip = Optional.ofNullable(request.getRemoteAddress())
                .map(addr -> addr.getAddress().getHostAddress())
                .orElse("unknown");
        String requestId = UUID.randomUUID().toString(true).substring(0, 8);

        exchange.getAttributes().put(START_TIME, System.currentTimeMillis());
        exchange.getAttributes().put("requestId", requestId);

        log.info("[{}] --> {} {} {} | IP: {}", requestId, method, path,
                StringUtils.hasText(query) ? "?" + query : "", ip);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long startTime = exchange.getAttributeOrDefault(START_TIME, 0L);
            long duration = System.currentTimeMillis() - startTime;
            HttpStatusCode status = exchange.getResponse().getStatusCode();

            log.info("[{}] <-- {} {} | Status: {} | 耗时: {}ms",
                    requestId, method, path,
                    status != null ? status.value() : "unknown", duration);
        }));
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }
}
```

#### 5.1.5 覆盖率过低异常（Fallback）

```java
package com.example.gateway.controller;

@RestController
@Slf4j
public class FallbackController {

    @RequestMapping("/fallback/product")
    public Mono<Map<String, Object>> productFallback() {
        log.warn("[Fallback] 商品服务熔断降级");
        Map<String, Object> result = new HashMap<>();
        result.put("code", 503);
        result.put("message", "服务暂时不可用，请稍后重试");
        result.put("timestamp", System.currentTimeMillis());
        return Mono.just(result);
    }

    @RequestMapping("/fallback/order")
    public Mono<Map<String, Object>> orderFallback() {
        log.warn("[Fallback] 订单服务熔断降级");
        Map<String, Object> result = new HashMap<>();
        result.put("code", 503);
        result.put("message", "订单服务繁忙，请稍后重试");
        result.put("timestamp", System.currentTimeMillis());
        return Mono.just(result);
    }
}
```

---

### 5.2 Gateway + JWT鉴权过滤器

#### 5.2.1 架构设计

```
客户端请求
  │
  ├── /api/auth/login ──→ 放行，后端认证服务生成JWT
  │
  ├── /api/auth/register → 放行
  │
  └── /api/** (其他) ──→ JWT GlobalFilter 拦截
         │
         ├── 无Token或Token无效 → 返回 401
         │
         └── Token有效 → 解析用户信息，放入Header传递给下游服务
```

#### 5.2.2 JWT工具类

```java
package com.example.gateway.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Slf4j
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expiration;

    public JwtUtil(@Value("${jwt.secret:DefaultSecretKeyForGateway2025MustLength256Bits!}") String secret,
                   @Value("${jwt.expiration:86400000}") long expiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    /**
     * 生成JWT令牌
     */
    public String generateToken(String userId, String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(userId)
                .claim("username", username)
                .claim("role", role)
                .setIssuedAt(now)
                .setIssuer("gateway-server")
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析JWT令牌
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("JWT已过期: {}", e.getMessage());
            throw e;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT解析失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 验证JWT是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 从Token中提取用户ID
     */
    public String getUserIdFromToken(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 从Token中提取角色
     */
    public String getRoleFromToken(String token) {
        return parseToken(token).get("role", String.class);
    }

    /**
     * 从Token中提取用户名
     */
    public String getUsernameFromToken(String token) {
        return parseToken(token).get("username", String.class);
    }
}
```

#### 5.2.3 JWT全局鉴权过滤器

```java
package com.example.gateway.filter;

import com.example.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    /**
     * 白名单路径：不需要认证即可访问
     */
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/auth/captcha",
            "/fallback/**"
    );

    /**
     * 不需要过滤的HTTP方法
     */
    private static final List<String> OPTIONS_METHODS = Arrays.asList(
            HttpMethod.OPTIONS.name()
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethodValue();

        // 1. OPTIONS请求直接放行（CORS预检请求）
        if (OPTIONS_METHODS.contains(method)) {
            log.debug("[JwtAuth] OPTIONS请求放行: {}", path);
            return chain.filter(exchange);
        }

        // 2. 白名单路径直接放行
        for (String whitePath : WHITE_LIST) {
            if (pathMatcher.match(whitePath, path)) {
                log.debug("[JwtAuth] 白名单放行: {}", path);
                return chain.filter(exchange);
            }
        }

        // 3. 提取Token
        String token = extractToken(request);

        if (!StringUtils.hasText(token)) {
            log.warn("[JwtAuth] 缺少Token: {}", path);
            return unauthorized(exchange, "缺少认证令牌，请先登录");
        }

        // 4. 验证Token
        try {
            if (!jwtUtil.validateToken(token)) {
                log.warn("[JwtAuth] Token无效: {}", path);
                return unauthorized(exchange, "认证令牌无效");
            }

            // 5. 解析Token，提取用户信息
            Claims claims = jwtUtil.parseToken(token);
            String userId = claims.getSubject();
            String username = claims.get("username", String.class);
            String role = claims.get("role", String.class);

            log.debug("[JwtAuth] 认证通过: userId={}, username={}, role={}, path={}",
                    userId, username, role, path);

            // 6. 将用户信息放入请求头，传递给下游微服务
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Name", encodeUtf8(username))
                    .header("X-User-Role", role)
                    .header("X-Request-Id", java.util.UUID.randomUUID().toString().substring(0, 8))
                    .build();

            // 移除原始Authorization头，不再向下游传递
            modifiedRequest = new ServerHttpRequestDecorator(modifiedRequest) {
                @Override
                public HttpHeaders getHeaders() {
                    HttpHeaders headers = new HttpHeaders();
                    headers.putAll(super.getHeaders());
                    headers.remove(HttpHeaders.AUTHORIZATION);
                    return headers;
                }
            };

            ServerWebExchange modifiedExchange = exchange.mutate()
                    .request(modifiedRequest)
                    .build();

            return chain.filter(modifiedExchange);

        } catch (ExpiredJwtException e) {
            log.warn("[JwtAuth] Token已过期: {}", path);
            return unauthorized(exchange, "认证令牌已过期，请重新登录");
        } catch (Exception e) {
            log.error("[JwtAuth] 认证异常: {}", e.getMessage());
            return unauthorized(exchange, "认证失败");
        }
    }

    /**
     * 从请求中提取Token
     * 优先从 Authorization Header 提取，其次从 Cookie 提取
     */
    private String extractToken(ServerHttpRequest request) {
        // 方式1：从Authorization Header提取
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // 方式2：从Cookie中提取
        String cookieHeader = request.getHeaders().getFirst(HttpHeaders.COOKIE);
        if (StringUtils.hasText(cookieHeader)) {
            for (String cookie : cookieHeader.split(";")) {
                cookie = cookie.trim();
                if (cookie.startsWith("token=")) {
                    return cookie.substring(6);
                }
            }
        }

        // 方式3：从查询参数提取（用于WebSocket等场景）
        String tokenParam = request.getQueryParams().getFirst("token");
        if (StringUtils.hasText(tokenParam)) {
            return tokenParam;
        }

        return null;
    }

    /**
     * 返回401未授权
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set("X-Error-Code", "AUTH_FAILED");

        String body = String.format(
                "{\"code\":401,\"message\":\"%s\",\"timestamp\":%d}",
                message, System.currentTimeMillis()
        );

        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * 对中文进行编码，避免Header中文乱码
     */
    private String encodeUtf8(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return value;
        }
    }

    @Override
    public int getOrder() {
        return -100; // 在日志过滤器之后，但在其他业务过滤器之前
    }
}
```

#### 5.2.4 白名单配置优化版

```java
package com.example.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "gateway.auth")
@Data
public class AuthProperties {

    /**
     * 白名单路径列表
     */
    private List<String> whiteList = new ArrayList<>();

    /**
     * JWT密钥
     */
    private String jwtSecret = "DefaultSecretKeyForGateway2025MustLength256Bits!";

    /**
     * JWT过期时间（毫秒）
     */
    private long jwtExpiration = 86400000;
}
```

**对应application.yml配置：**
```yaml
gateway:
  auth:
    white-list:
      - /api/auth/login
      - /api/auth/register
      - /api/auth/refresh
      - /api/auth/captcha
      - /fallback/**
    jwt-secret: MyCustomSecretKeyForGateway2025AtLeast256BitsLong!!
    jwt-expiration: 86400000
```

#### 5.2.5 JWT生成接口（模拟认证服务）

```java
package com.example.gateway.controller;

import com.example.gateway.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public Mono<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        // 模拟用户认证（实际应从数据库中验证）
        if ("admin".equals(username) && "123456".equals(password)) {
            String token = jwtUtil.generateToken("1001", username, "ADMIN");
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("token", token);
            result.put("userId", "1001");
            result.put("username", username);
            result.put("role", "ADMIN");
            result.put("expiresIn", 86400);
            return Mono.just(result);
        }

        Map<String, Object> error = new HashMap<>();
        error.put("code", 401);
        error.put("message", "用户名或密码错误");
        return Mono.just(error);
    }

    @PostMapping("/register")
    public Mono<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        // 模拟注册（实际应写入数据库）
        String token = jwtUtil.generateToken("2001", username, "USER");
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("token", token);
        result.put("username", username);
        return Mono.just(result);
    }

    @PostMapping("/verify")
    public Mono<Map<String, Object>> verify(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        boolean valid = jwtUtil.validateToken(token);
        Map<String, Object> result = new HashMap<>();
        result.put("valid", valid);
        if (valid) {
            result.put("userId", jwtUtil.getUserIdFromToken(token));
            result.put("role", jwtUtil.getRoleFromToken(token));
        }
        return Mono.just(result);
    }
}
```

---

### 5.3 Gateway + Sentinel限流集成

#### 5.3.1 架构说明

```
┌──────────┐   规则推送    ┌──────────────┐
│ Sentinel │ ←────────── │  Sentinel     │
│ 客户端    │             │  控制台       │
└────┬─────┘             └──────────────┘
     │
     │ 拦截请求
     ▼
┌─────────────────────────────────────────┐
│         Spring Cloud Gateway             │
│  ┌───────────────────────────────────┐   │
│  │  SentinelGatewayFilter            │   │
│  │  ├── 流控规则检查                  │   │
│  │  ├── 熔断降级规则检查              │   │
│  │  └── 热点参数限流                  │   │
│  └───────────────────────────────────┘   │
└─────────────────────────────────────────┘
```

#### 5.3.2 Maven依赖

```xml
<!-- Sentinel + Gateway 适配 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>

<!-- Sentinel Gateway 扩展 -->
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-spring-cloud-gateway-adapter</artifactId>
</dependency>

<!-- Sentinel Nacos 数据源（可选，用于持久化规则） -->
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-datasource-nacos</artifactId>
</dependency>
```

#### 5.3.3 application.yml 配置

```yaml
spring:
  application:
    name: gateway-server

  cloud:
    sentinel:
      transport:
        dashboard: localhost:8081   # Sentinel 控制台地址
        port: 8719                  # 客户端监控API端口
      eager: true                    # 提前初始化
      filter:
        enabled: false               # 关闭Servlet Filter（Gateway不需要）
      datasource:
        # 网关流控规则 - 从Nacos读取（持久化）
        ds1:
          nacos:
            server-addr: 127.0.0.1:8848
            data-id: ${spring.application.name}-gateway-flow-rules
            group-id: SENTINEL_GROUP
            data-type: json
            rule-type: gw-flow
        # 网关API分组规则
        ds2:
          nacos:
            server-addr: 127.0.0.1:8848
            data-id: ${spring.application.name}-gateway-api-rules
            group-id: SENTINEL_GROUP
            data-type: json
            rule-type: gw-api-group
```

#### 5.3.4 Sentinel配置类

```java
package com.example.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayParamFlowItem;
import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.exception.SentinelGatewayBlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;

import java.util.*;

@Configuration
public class SentinelGatewayConfig {

    private final List<ViewResolver> viewResolvers;
    private final ServerCodecConfigurer serverCodecConfigurer;

    public SentinelGatewayConfig(
            ObjectProvider<List<ViewResolver>> viewResolversProvider,
            ServerCodecConfigurer serverCodecConfigurer) {
        this.viewResolvers = viewResolversProvider.getIfAvailable(Collections::emptyList);
        this.serverCodecConfigurer = serverCodecConfigurer;
    }

    /**
     * 限流异常处理器
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SentinelGatewayBlockExceptionHandler sentinelGatewayBlockExceptionHandler() {
        return new SentinelGatewayBlockExceptionHandler(viewResolvers, serverCodecConfigurer);
    }

    /**
     * Sentinel全局过滤器
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public GlobalFilter sentinelGatewayFilter() {
        return new SentinelGatewayFilter();
    }

    /**
     * 自定义限流响应
     */
    @PostConstruct
    public void initBlockHandlers() {
        BlockRequestHandler blockRequestHandler = (exchange, throwable) -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("code", 429);
            result.put("message", "请求太频繁，请稍后重试");
            result.put("path", exchange.getRequest().getURI().getPath());
            result.put("timestamp", System.currentTimeMillis());

            return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(result));
        };
        GatewayCallbackManager.setBlockHandler(blockRequestHandler);
    }

    /**
     * 定义API分组和限流规则
     */
    @PostConstruct
    public void initGatewayRules() {
        // 1. 定义API分组
        Set<ApiDefinition> apiDefinitions = new HashSet<>();

        // 用户服务API组
        apiDefinitions.add(new ApiDefinition("user_api")
                .setPredicateItems(new HashSet<>(Arrays.asList(
                        new ApiPathPredicateItem()
                                .setPattern("/api/user/**")
                                .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX)
                ))));

        // 订单服务API组
        apiDefinitions.add(new ApiDefinition("order_api")
                .setPredicateItems(new HashSet<>(Arrays.asList(
                        new ApiPathPredicateItem()
                                .setPattern("/api/order/**")
                                .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX)
                ))));

        // 商品服务API组
        apiDefinitions.add(new ApiDefinition("product_api")
                .setPredicateItems(new HashSet<>(Arrays.asList(
                        new ApiPathPredicateItem()
                                .setPattern("/api/product/**")
                                .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX)
                ))));

        // 登录接口（单独限流）
        apiDefinitions.add(new ApiDefinition("login_api")
                .setPredicateItems(new HashSet<>(Arrays.asList(
                        new ApiPathPredicateItem()
                                .setPattern("/api/auth/login")
                                .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_EXACT)
                ))));

        GatewayApiDefinitionManager.loadApiDefinitions(apiDefinitions);

        // 2. 定义网关限流规则
        Set<GatewayFlowRule> rules = new HashSet<>();

        // 用户服务：每秒不超过20个请求
        rules.add(new GatewayFlowRule("user_api")
                .setCount(20)
                .setIntervalSec(1)
        );

        // 订单服务：每秒不超过10个请求
        rules.add(new GatewayFlowRule("order_api")
                .setCount(10)
                .setIntervalSec(1)
        );

        // 商品服务：每秒不超过50个请求，并发限制5
        rules.add(new GatewayFlowRule("product_api")
                .setCount(50)
                .setIntervalSec(1)
                .setBurst(10)
                .setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER)
                .setMaxQueueingTimeoutMs(500)
        );

        // 登录接口：针对IP限流，每秒不超过3个请求
        rules.add(new GatewayFlowRule("login_api")
                .setCount(3)
                .setIntervalSec(1)
                .setParamItem(new GatewayParamFlowItem()
                        .setParseStrategy(SentinelGatewayConstants.PARAM_PARSE_STRATEGY_CLIENT_IP))
        );

        // 商品详情：热点参数限流（参数0是商品ID）
        rules.add(new GatewayFlowRule("product_api")
                .setCount(5)
                .setIntervalSec(1)
                .setParamItem(new GatewayParamFlowItem()
                        .setParseStrategy(SentinelGatewayConstants.PARAM_PARSE_STRATEGY_URL_PARAM)
                        .setFieldName("id"))
        );

        // 针对路由ID限流（sentinel_user_route是路由ID）
        rules.add(new GatewayFlowRule("user-service")
                .setCount(100)
                .setIntervalSec(1)
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_ROUTE_ID)
        );

        GatewayFlowRuleManager.loadRules(rules);
    }
}
```

> 💡 生产环境中，建议通过Sentinel控制台动态配置规则，而不是写死在代码中。使用Nacos数据源可以实现规则持久化。

#### 5.3.5 Sentinel控制台规则配置（Nacos data-id示例）

**`gateway-server-gateway-flow-rules.json`**
```json
[
  {
    "resource": "user_api",
    "count": 20.0,
    "intervalSec": 1,
    "controlBehavior": 0,
    "burst": 0,
    "maxQueueingTimeoutMs": 0,
    "paramItem": null
  },
  {
    "resource": "order_api",
    "count": 10.0,
    "intervalSec": 1,
    "controlBehavior": 0,
    "burst": 0,
    "maxQueueingTimeoutMs": 0,
    "paramItem": null
  },
  {
    "resource": "product_api",
    "count": 50.0,
    "intervalSec": 1,
    "controlBehavior": 2,
    "burst": 10,
    "maxQueueingTimeoutMs": 500,
    "paramItem": null
  },
  {
    "resource": "login_api",
    "count": 3.0,
    "intervalSec": 1,
    "controlBehavior": 0,
    "burst": 0,
    "maxQueueingTimeoutMs": 0,
    "paramItem": {
      "parseStrategy": 2,
      "fieldName": null,
      "pattern": "",
      "matchStrategy": 0
    }
  },
  {
    "resource": "user-service",
    "count": 100.0,
    "intervalSec": 1,
    "resourceMode": 0,
    "controlBehavior": 0,
    "burst": 0,
    "maxQueueingTimeoutMs": 0,
    "paramItem": null
  }
]
```

---

### 5.4 CORS跨域配置

#### 5.4.1 全局CORS配置（推荐方案）

```java
package com.example.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsGlobalConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);                // 允许携带cookie
        config.addAllowedOriginPattern("*");              // 允许所有域名（生产环境应具体指定）
        config.addAllowedHeader("*");                     // 允许所有请求头
        config.addAllowedMethod("*");                     // 允许所有HTTP方法
        config.setMaxAge(3600L);                          // 预检请求缓存1小时
        config.addExposedHeader("X-Request-Id");          // 暴露给前端的响应头
        config.addExposedHeader("X-Error-Code");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
```

#### 5.4.2 yaml方式CORS配置

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allow-credentials: true
            allowed-origin-patterns: "*"
            allowed-headers: "*"
            allowed-methods:
              - GET
              - POST
              - PUT
              - DELETE
              - OPTIONS
            max-age: 3600
            exposed-headers:
              - X-Request-Id
              - X-Error-Code
```

> ⚠️ yaml方式配置CORS时，注意缩进和数组格式。`allowed-origin-patterns` 支持通配符`*`，`allowed-origins` 不支持。

#### 5.4.3 细粒度CORS（按路由配置）

```java
package com.example.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

@Configuration
public class CorsRouteConfig {

    @Bean
    public RouteLocator corsRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("cors-user-route", r -> r
                        .path("/api/user/**")
                        .and()
                        .method(HttpMethod.GET, HttpMethod.POST)
                        .filters(f -> f
                                .addResponseHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://admin.example.com")
                                .addResponseHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
                                .addResponseHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE")
                                .addResponseHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*")
                                .addResponseHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"))
                        .uri("lb://user-service"))
                .build();
    }
}
```

#### 5.4.4 跨域问题排查清单

| 排查项 | 说明 |
|--------|------|
| 是否返回 `Access-Control-Allow-Origin` | 检查响应头 |
| OPTIONS预检是否返回200 | 预检请求需要正确响应 |
| `allowed-origin-patterns` 和 `allowed-origins` 区别 | 前者支持`*`通配，后者不支持 |
| `allowCredentials` 为true时 | `allowed-origin-patterns` 不能为`*`，需具体指定 |
| 多个CORS配置是否冲突 | 使用 `DedupeResponseHeader` 去重 |

---

## 6. 分层综合习题

### 6.1 基础题

**题目1：** 简述Spring Cloud Gateway的三大核心概念（Route / Predicate / Filter）及其关系。

**题目2：** 列出至少5种内置Predicate，并说明各自的匹配场景。

**题目3：** `StripPrefix=2` 的作用是什么？如果一个请求路径为 `/a/b/c/d`，经过该Filter后路径变为什么？

**题目4：** Gateway中如何配置负载均衡转发？`lb://service-name` 的含义是什么？

**题目5：** 编写yaml配置，实现所有`/api/order/**`的GET请求转发到`lb://order-service`。

**题目6：** 什么是GlobalFilter？什么是GatewayFilter？它们有什么区别？

### 6.2 进阶题

**题目7：** 有一个后端服务，接收的路径不带`/api`前缀，但前端请求都带`/api/user/xxx`，要求在Gateway中重写路径，写出两种实现方式。

**参考解答：**
```yaml
# 方式1：StripPrefix=1
filters:
  - StripPrefix=1
# /api/user/list → /user/list

# 方式2：RewritePath
filters:
  - RewritePath=/api/user/(?<seg>.*), /$\{seg}
# /api/user/list → /list （注意这里是/list而不是/user/list）
```

**题目8：** 如何实现Gateway中JWT Token的解析和用户信息传递？请写出核心代码片段。

**题目9：** 配置了一个CORS跨域，但前端依然报跨域错误，请列举至少3个可能的原因和对应的解决方案。

**题目10：** Gateway的Filter执行顺序是如何确定的？如果有一个日志Filter和一个鉴权Filter，应该分别设置什么order值？

**参考解答：**
```java
// 鉴权Filter优先于日志Filter执行
// 鉴权Filter: order = -100（先执行）
// 日志Filter: order = -1（后执行，但计算耗时）

// 但实际上，日志Filter通常在最外层：
// 日志Filter: order = HIGHEST_PRECEDENCE
// 鉴权Filter: order = -100
```

**题目11：** Weight Predicate适用于什么场景？请写出一个灰度发布的配置示例。

**题目12：** Gateway内置的限流Filter `RequestRateLimiter` 基于什么算法？需要引入哪些依赖？

**参考解答：** 基于令牌桶算法（Token Bucket）。需要引入 `spring-boot-starter-data-redis-reactive`，并配置 `KeyResolver` Bean。

**题目13：** Gateway中如何实现动态路由？（至少说出两种方式）

**参考解答：**
1. 基于Nacos配置中心 + `@RefreshScope` 动态刷新
2. 基于数据库 + 实现 `RouteDefinitionRepository` + 定时刷新
3. 基于Nacos + `RouteDefinitionWriter` + `RefreshRoutesEvent`

### 6.3 精通题

**题目14：** 解释Gateway底层Netty的Reactor线程模型，Boss Group和Work Group各自的作用是什么？

**参考解答：**
- **Boss Group**：负责接收客户端连接，通常1个线程足够（master）
- **Work Group**：负责处理I/O读写、编解码、业务处理（worker），默认线程数为CPU核数×2
- Gateway在Work Group中执行所有Filter链和请求转发，使用EventLoop的非阻塞I/O

**题目15：** 如何在Gateway中修改请求体（Request Body）？列举实现步骤和注意事项。

**题目16：** Gateway集成Sentinel实现网关限流时，`ResourceMode` 有哪两种模式？各自对应什么场景？

**参考解答：**
- `RESOURCE_MODE_ROUTE_ID (0)`：按路由ID限流，对该路由所有请求生效
- `RESOURCE_MODE_CUSTOM_API_NAME (1)`：按API分组限流，可以对多个路由进行聚合限流

**题目17：** 假设网关QPS到达5万，但后端服务只能承受1万QPS，你如何设计多级限流方案？

**参考解答：**
```
入口: Nginx层限流（全局速率限制）
  ↓
网关: Sentinel限流（按服务/按API分组/按IP）
  ↓
网关: RequestRateLimiter（令牌桶）
  ↓
后端: 接口级限流（Sentinel或RateLimiter）
  ↓
数据库: 连接池限制
```

**题目18：** 对比Spring Cloud Gateway与Kong、APISIX等API网关的优缺点。

**题目19：** 如何实现Gateway的蓝绿部署（Blue-Green Deployment）？请设计路由策略。

**参考解答：**
```yaml
# 蓝绿部署
spring:
  cloud:
    gateway:
      routes:
        # 蓝环境（生产环境，主流量）
        - id: user-service-blue
          uri: lb://user-service-blue
          predicates:
            - Path=/api/user/**
            - Weight=blue-green, 100   # 全部流量
          order: 10

        # 绿环境（新版本，手动切换）
        - id: user-service-green
          uri: lb://user-service-green
          predicates:
            - Path=/api/user/**
            - Weight=blue-green, 0     # 无流量
          order: 10

# 切换时，将权重改为 0/100 或使用Header匹配
```

**题目20：** 简述Gateway的 `RouteDefinitionWriter` 和 `RouteLocator` 的关系，如何通过代码添加/删除路由？

---

## 7. 本章复盘速记清单

### 7.1 核心概念速记

```
三大核心模型
├── Route (路由)    = id + uri + predicates + filters
├── Predicate (断言) = 请求匹配条件, AND关系
└── Filter (过滤器)  = 请求/响应拦截和处理

Predicate 11种
├── Path / Method / Header / Query / Cookie / Host
├── RemoteAddr / After / Before / Between
└── Weight (灰度分流)
```

### 7.2 常用配置速查

| 场景 | 配置 |
|------|------|
| 路由匹配 | `Path=/api/user/**` |
| 负载均衡 | `uri: lb://service-name` |
| 去掉前缀 | `StripPrefix=1` |
| 添加前缀 | `PrefixPath=/api` |
| 路径重写 | `RewritePath=/**` |
| 跨域CORS | `CorsWebFilter` Bean |
| 限流 | `RequestRateLimiter` + Redis |
| 重试 | `Retry=3` |
| 熔断 | `CircuitBreaker=xxx` |
| 添加请求头 | `AddRequestHeader=X-K, V` |
| 白名单路径 | JwtAuthFilter中配置WHITE_LIST |

### 7.3 常见踩坑速记

| 问题 | 解决 |
|------|------|
| 引入web-starter冲突 | 排除 `spring-boot-starter-web` |
| 路由404 | 检查yaml缩进、uri、Path规则、开启DEBUG日志 |
| RewritePath的`$`被解析 | 使用`$\{segment}`转义 |
| CORS配置不生效 | 使用`CorsWebFilter`Bean方式，或检查Filter顺序 |
| 响应超时 | 配置`httpclient.response-timeout` |
| 请求体只能读一次 | 使用`CacheRequestBody`或`DataBuffer`装饰器 |
| Gateway不支持`spring-boot-starter-web` | Gateway基于WebFlux，不能和Servlet共存 |

### 7.4 架构决策速记

```
Gateway vs Nginx
├── Nginx: L4/L7负载均衡, 静态文件服务, SSL终端, 网关前置
├── Gateway: 应用层网关, 鉴权/限流/路由/日志, 注册中心集成
└── 最佳实践: Nginx → Gateway → 微服务

Gateway vs Zuul 1.x
├── Zuul 1.x: BIO阻塞模型, 性能差, 维护模式
├── Zuul 2.x: Netty非阻塞, 社区不活跃
└── Gateway: Netty + WebFlux, Spring官方推荐, 生态完善
```

### 7.5 面试高频题

```
1. Gateway核心组件是什么？
2. Gateway和Zuul的区别？
3. Gateway底层为什么性能高？（Netty Reactor模型）
4. 如何实现JWT鉴权？
5. 如何实现动态路由？
6. Gateway限流方式有哪些？
7. 灰度发布怎么实现？
8. StripPrefix和RewritePath的区别？
9. Gateway和Nginx的分工？
10. 跨域问题如何解决？
```

---

## 8. 精通拓展补充-P2

### 8.1 Gateway性能调优

#### 8.1.1 Netty参数调优

```yaml
# application.yml
server:
  netty:
    connection-timeout: 5000
    # Netty线程配置
    reactor:
      netty:
        # 工作线程数（默认CPU核数*2）
        ioWorkerCount: 16
        # 是否启用原生传输（epoll/kqueue）
        native: true

spring:
  cloud:
    gateway:
      httpclient:
        pool:
          type: elastic          # 弹性连接池
          max-connections: 2000  # 最大连接数（根据后端服务调整）
          max-idle-time: 60000   # 空闲连接存活时间
        connect-timeout: 3000    # 连接超时
        response-timeout: 10s    # 响应超时
        # TCP参数
        tcp:
          # TCP快速重传
          fastopen: true
          # 禁用Nagle算法（减少延迟）
          nodelay: true
          # TCP Keep-Alive
          keepalive: true

      # 路由缓存
      routes:
        # 缓存路由定义，减少匹配开销
        cache:
          enabled: true
          ttl: 10000  # 10秒
```

#### 8.1.2 JVM参数调优

```bash
# Gateway JVM启动参数
java -Xms2g -Xmx2g \
     -Xmn1g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=100 \
     -XX:+ParallelRefProcEnabled \
     -XX:+PrintGCDetails \
     -XX:+PrintGCDateStamps \
     -XX:+UseContainerSupport \
     -Djava.security.egd=file:/dev/./urandom \
     -jar gateway-server.jar
```

#### 8.1.3 路由匹配优化

```java
/**
 * 自定义RouteDefinitionLocator，使用缓存加速路由匹配
 */
@Component
public class CachingRouteDefinitionLocator implements RouteDefinitionLocator {

    private final RouteDefinitionLocator delegate;
    private final Cache<Long, List<RouteDefinition>> cache;

    public CachingRouteDefinitionLocator(RouteDefinitionLocator delegate) {
        this.delegate = delegate;
        this.cache = Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(10, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        return Flux.defer(() -> {
            List<RouteDefinition> cached = cache.getIfPresent(1L);
            if (cached != null) {
                return Flux.fromIterable(cached);
            }
            return delegate.getRouteDefinitions()
                    .collectList()
                    .doOnNext(definitions -> cache.put(1L, definitions))
                    .flatMapMany(Flux::fromIterable);
        });
    }
}
```

### 8.2 高级路由策略

#### 8.2.1 Header-based 路由

```yaml
spring:
  cloud:
    gateway:
      routes:
        # 移动端请求
        - id: mobile-route
          uri: lb://mobile-service
          predicates:
            - Path=/api/**
            - Header=X-Client-Type, mobile
            - Header=X-App-Version, \d+\.\d+\.\d+

        # Web端请求
        - id: web-route
          uri: lb://web-service
          predicates:
            - Path=/api/**
            - Header=X-Client-Type, web
```

#### 8.2.2 版本化路由

```yaml
spring:
  cloud:
    gateway:
      routes:
        # API v2 路由（部分用户先体验）
        - id: api-v2
          uri: lb://backend-service-v2
          predicates:
            - Path=/api/v2/**
            - Weight=api-version, 10
          filters:
            - RewritePath=/api/v2/(?<seg>.*), /$\{seg}

        # API v1 路由（默认使用）
        - id: api-v1
          uri: lb://backend-service-v1
          predicates:
            - Path=/api/v1/**, /api/**
          filters:
            - RewritePath=/api/v1/(?<seg>.*), /$\{seg}
```

#### 8.2.3 多区域路由

```yaml
spring:
  cloud:
    gateway:
      routes:
        # 中国区
        - id: china-route
          uri: lb://china-service
          predicates:
            - Path=/api/**
            - RemoteAddr=101.0.0.0/8, 110.0.0.0/8, 120.0.0.0/8, 1.0.0.0/8
            - Host=*.example.cn

        # 海外区
        - id: overseas-route
          uri: lb://overseas-service
          predicates:
            - Path=/api/**
```

### 8.3 Gateway与Service Mesh

```
传统微服务网关架构
┌──────┐   ┌─────────┐   ┌──────────────┐
│客户端│ → │ Gateway │ → │ 微服务群     │
└──────┘   └─────────┘   │ ├─ User      │
                          │ ├─ Order     │
                          │ └─ Product   │
                          └──────────────┘

Service Mesh架构
┌──────┐   ┌─────────┐   ┌──────────────┐
│客户端│ → │Gateway  │ → │ 微服务群     │
└──────┘   └─────────┘   │ ├─ User  + Sidecar │
                          │ ├─ Order + Sidecar │
                          │ └─ Product+ Sidecar│
                          └──────────────┘
Gateway → Sidecar = 流量管理下沉到Sidecar
Gateway → 保留：外部流量入口、认证鉴权、API管理
Sidecar → 负责：服务间通信、重试、熔断、可观测性
```

### 8.4 Gateway响应式编程进阶

#### 8.4.1 Mono和Flux在Gateway中的应用

```java
// 异步调用多个后端服务并聚合结果
@Component
public class AggregationGlobalFilter implements GlobalFilter, Ordered {

    @Autowired
    private WebClient webClient;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 原始请求继续
        Mono<Void> originalResponse = chain.filter(exchange);

        // 异步调用外部服务增强响应
        Mono<Map> extraData = webClient.get()
                .uri("http://extra-service/api/enrich")
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorResume(e -> Mono.empty()); // 降级：不阻塞主流程

        // 并行执行
        return Mono.zip(originalResponse, extraData).then();
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
```

#### 8.4.2 背压(Backpressure)控制

```yaml
spring:
  codec:
    max-in-memory-size: 10MB    # 限制最大缓冲区大小
    max-request-size: 10MB
  cloud:
    gateway:
      httpclient:
        response-timeout: 30s
        # 控制响应体背压
        pool:
          type: fixed
          max-connections: 500
          acquire-timeout: 30000
```

### 8.5 Gateway安全加固

```yaml
spring:
  cloud:
    gateway:
      routes:
        # 防SQL注入过滤器（自定义）
        - id: security-route
          uri: lb://backend-service
          predicates:
            - Path=/api/**
          filters:
            - name: SqlInjectionFilter
            - name: XssFilter
            - name: RateLimiter

# 安全头配置
filters:
  - SecureHeaders
  - name: SecureHeaders
    args:
      X-XSS-Protection: "1; mode=block"
      X-Content-Type-Options: nosniff
      X-Frame-Options: DENY
      Strict-Transport-Security: max-age=31536000; includeSubDomains
      Content-Security-Policy: default-src 'self'
```

### 8.6 Gateway监控与可观测性

```yaml
# 集成Micrometer + Prometheus
management:
  endpoints:
    web:
      exposure:
        include: health,info,gateway,metrics,prometheus
  metrics:
    tags:
      application: ${spring.application.name}
    export:
      prometheus:
        enabled: true
```

**Gateway关键监控指标：**

| 指标 | 说明 | PromQL示例 |
|------|------|-----------|
| `gateway.requests` | 请求计数 | `rate(gateway_requests_total[1m])` |
| `gateway.request.duration` | 请求耗时 | `histogram_quantile(0.99, gateway_request_duration_seconds_bucket)` |
| `gateway.routes.count` | 路由数量 | `gateway_routes_count` |
| `gateway.filter.count` | 过滤器计数 | `gateway_filter_count` |
| `gateway.httpclient.connections` | 连接数 | `gateway_httpclient_connections` |

> 🎯 在生产环境中，务必配置Gateway的监控告警，包括：路由404率飙升、请求延迟P99超阈值、限流触发次数、5xx错误率等关键指标。

---

> **本文档定位**：P1 就业必备 / 微服务网关层核心组件。后续将补充P2架构师级内容，包括Gateway + Kubernetes Ingress集成、Gateway gRPC代理、Gateway插件化架构设计等进阶主题。
