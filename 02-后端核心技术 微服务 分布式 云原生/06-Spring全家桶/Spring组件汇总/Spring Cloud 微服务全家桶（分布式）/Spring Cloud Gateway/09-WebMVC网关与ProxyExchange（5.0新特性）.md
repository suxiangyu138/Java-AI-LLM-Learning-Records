# 09 WebMVC 网关与 Proxy Exchange（5.0 新特性）

> 新特性篇：**5.0 最大的架构变化——WebFlux/WebMVC 双栈**：为什么拆、WebMVC 网关怎么用、与 WebFlux 的差异、Proxy Exchange 嵌入式代理、双栈选型决策——Boot 4 时代的"网关技术栈选择"。

---

## 📚 目录

1. [背景：虚拟线程时代为什么拆双栈](#1-背景虚拟线程时代为什么拆双栈)
2. [WebMVC 网关快速上手](#2-webmvc-网关快速上手)
3. [WebMVC vs WebFlux：能力差异表](#3-webmvc-vs-webflux能力差异表)
4. [WebMVC 网关开发要点](#4-webmvc-网关开发要点)
5. [Proxy Exchange：嵌入式代理](#5-proxy-exchange嵌入式代理)
6. [双栈选型决策树](#6-双栈选型决策树)

---

## 1. 背景：虚拟线程时代为什么拆双栈

### 1.1 历史脉络

```text
Spring Cloud Gateway 诞生（2018）：WebFlux + Netty 非阻塞
  ├── 优势：事件循环扛高并发
  └── 代价：响应式编程学习曲线陡峭，团队上手慢

Java 21 虚拟线程（2023+）：
  ├── 传统"每请求一线程"的阻塞模型也能扛高并发（线程成本趋近于零）
  └── 意味着：WebMVC 阻塞代码 + 虚拟线程 ≈ WebFlux 非阻塞性能

Spring Cloud Gateway 5.0（2025.11 Oakwood）：
  └── 官方正式拆双栈：路由模型不变，运行时二选一
      WebFlux（原有用户） / WebMVC + 虚拟线程（新选择）
```

> 🎯 一句话：**5.0 双栈 = "非阻塞派"与"阻塞派"各得其所**——虚拟线程让阻塞式编程重新有了高性能资格，官方不再强制 WebFlux。

### 1.2 官方拆分方式（issue #3858）

```text
旧（4.x）                         新（5.0）
spring-cloud-gateway-server  →  spring-cloud-gateway-server-webflux
                                spring-cloud-gateway-server-webmvc
spring-cloud-starter-gateway →  spring-cloud-starter-gateway-server-webflux（废弃警告）
                                spring-cloud-starter-gateway-server-webmvc
属性前缀：
  spring.cloud.gateway.*      →  spring.cloud.gateway.server.webflux.*
  spring.cloud.gateway.mvc.*  →  spring.cloud.gateway.server.webmvc.*
```

## 2. WebMVC 网关快速上手

### 2.1 依赖

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway-server-webmvc</artifactId>
    <!-- 5.0.x：Servlet + 虚拟线程栈 -->
</dependency>
<!-- 需要注册中心路由时照常加 discovery starter -->
```

### 2.2 路由配置（属性前缀不同）

```yaml
spring:
  cloud:
    gateway:
      server:
        webmvc:                  # ★ 不是 webflux！
          routes:
            - id: order-service
              uri: lb://order-service
              predicates:
                - Path=/orders/**
              filters:
                - StripPrefix=1
```

> 💡 **路由模型完全一致**（yaml 结构、谓词、过滤器概念相同）——WebMVC 用户几乎零学习成本迁移。

### 2.3 虚拟线程启用

```yaml
spring:
  threads:
    virtual:
      enabled: true              # ★ Boot 4 全局虚拟线程开关
```

```text
虚拟线程网关的语义：
  ├── 每个请求分配一个虚拟线程（便宜，创建/销毁开销低）
  ├── 阻塞代码（同步 JDBC/Redis）不再浪费平台线程
  └── 代码还是传统 MVC 风格（同步、直觉）
```

## 3. WebMVC vs WebFlux：能力差异表

### 3.1 过滤器能力差异（官方文档实证）

| 过滤器 | WebFlux | WebMVC | 说明 |
|--------|:---:|:---:|------|
| Add/Set/Remove 头与参数 | ✅ | ✅ | 通用 |
| StripPrefix / RewritePath / SetPath / PrefixPath | ✅ | ✅ | 通用 |
| ModifyRequestBody / ModifyResponseBody | ✅ | ✅ | 通用 |
| CircuitBreaker | ✅ | ✅ | 通用（底层不同实现） |
| Retry | ✅ | ✅ | 通用 |
| **RequestRateLimiter** | ✅ | ✅ | 通用（Redis） |
| **LoadBalancer** | ❌（由 GlobalFilter 处理） | ✅ 过滤器 | WebMVC 独有过滤器形态 |
| **StripContextPath** | ❌ | ✅ | WebMVC 独有 |
| SaveSession | ✅ | ✅ | 通用 |
| TokenRelay | ✅ | ✅ | 通用 |
| SecureHeaders | ✅ | ✅ | 通用 |
| **JsonToGrpc** | ✅ | ❌ | WebFlux 独有 |
| **LocalResponseCache** | ✅ | ❌ | WebFlux 独有 |
| **RequestHeaderToRequestUri** | ✅ | ❌ | WebFlux 独有 |
| **RemoveJsonAttributesResponseBody** | ✅ | ❌ | WebFlux 独有 |
| **RewriteRequestParameter** | ✅ | ❌ | WebFlux 独有 |
| **SaveSession 前的 HttpHeadersFilters** | ✅ | ✅ | 均有但实现不同 |

> ⚠️ **迁移注意**：WebFlux 独有过滤器（JsonToGrpc/LocalResponseCache 等）在 WebMVC 栈不可用——**迁移前对照本表检查在用过滤器**。

### 3.2 架构差异

| 维度 | WebFlux | WebMVC |
|------|---------|--------|
| 运行时 | Reactor Netty | Servlet（Tomcat/Jetty）+ 虚拟线程 |
| 编程模型 | 响应式（Mono/Flux） | 阻塞式（同步） |
| 高并发 | 事件循环 | 虚拟线程（需 Boot 4 + JDK 21） |
| 与 Servlet 生态 | 隔离 | ✅ 原生兼容（Servlet Filter/旧库） |
| 学习成本 | 高 | 低（传统 MVC 开发经验直接复用） |
| 定制谓词/过滤器 | WebFlux API | MVC API（写法不同） |

## 4. WebMVC 网关开发要点

### 4.1 自定义谓词/过滤器（MVC 风格）

```java
// WebMVC 网关的请求谓词（GatewayRequestPredicate）
@Component
public class CustomRequestPredicate implements GatewayRequestPredicate {
    @Override
    public boolean test(ServerWebExchange exchange) {
        return exchange.getRequest().getHeaders().containsKey("X-Custom");
    }
}

// 过滤器（GatewayHandlerFilterFunction）
@Component
public class CustomFilter implements GatewayHandlerFilterFunction {
    @Override
    public ServerWebExchange apply(ServerWebExchange exchange, GatewayHandlerFilterFunction chain) {
        // 同步式处理（可以直接做阻塞操作——虚拟线程兜底）
        return chain.apply(exchange);
    }
}
```

> 💡 WebMVC 开发体验：**过滤器是同步方法签名**（返回 ServerWebExchange），不需要 Mono 包装——对 MVC 团队非常友好。

### 4.2 与 Servlet 生态协同

```java
// 可以用传统 Servlet Filter / Interceptor（WebMVC 网关原生兼容）
@Configuration
public class ServletConfig {
    @Bean
    public Filter myServletFilter() {
        return (request, response, chain) -> chain.doFilter(request, response);
    }
}
```

### 4.3 性能真相（官方没说的坑）

| 前提 | 说明 |
|------|------|
| 虚拟线程 ≠ 零成本 | 虚拟线程挂起/恢复有开销；极致压测 WebFlux 仍略优 |
| 必须 JDK 21+ | 虚拟线程是 JDK 21 特性，老 JDK 用不了 |
| 阻塞代码随便写？ | 虚拟线程扛得住，但**平台线程池里的阻塞仍要避免**（如 @Bean 初始化线程） |
| 兼容性 | 阻塞式第三方库（同步 JDBC 等）反而比 WebFlux 好接 |

> 🎯 面试必答：**"5.0 的 WebMVC 网关和 WebFlux 网关怎么选？"**——**团队是 MVC 派且不熟响应式 → WebMVC + 虚拟线程**（同步代码 + JDK 21 高并发，兼容 Servlet 生态）；**已有 WebFlux 基建/追求极致性能/需要响应式独有过滤器（JsonToGrpc 等）→ WebFlux**；两者路由模型一致，迁移成本主要在过滤器差异。

## 5. Proxy Exchange：嵌入式代理

### 5.1 概念

```text
Proxy Exchange = 应用内代理（不是独立网关进程）
  ├── 在 WebMVC/WebFlux 应用里，通过注解方法直接转发请求到下游
  ├── 适用：不需要独立网关、只想给部分接口做转发
  └── 5.0 拆分：proxyexchange-webflux / proxyexchange-webmvc 两个 starter
```

### 5.2 用法（WebMVC 示例）

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway-proxyexchange-webmvc</artifactId>
</dependency>
```

```java
@RestController
public class ProxyController {
    @GetMapping("/proxy/orders/{id}")
    public ResponseEntity<?> proxyOrders(@PathVariable Long id, ProxyExchange<?> proxy) {
        return proxy.uri("lb://order-service/orders/" + id).get();   // ★ 一行转发
    }
}
```

### 5.3 ProxyExchange 能力

| 能力 | 说明 |
|------|------|
| 动态转发 | `proxy.uri(target)` 任意目标 |
| 头/参数控制 | proxy.header(...) / proxy.uri 拼参 |
| 响应包装 | 返回 ResponseEntity |
| 安全 | 代理方法可加 Spring Security 注解 |
| 对比独立网关 | 轻量（无路由表管理）、灵活（方法级控制） |

> 🎯 选型：**"有独立网关需求的 → server starter；只要个别接口转发 → Proxy Exchange"**——Proxy Exchange 是"应用内轻代理"，不是独立网关的替代品。

## 6. 双栈选型决策树

```text
新项目 / 存量迁移网关技术栈选择：

团队有 WebFlux/响应式经验？
  ├── 是 → spring-cloud-starter-gateway-server-webflux
  │         └── 理由：事件循环极致性能、响应式过滤器全家桶（JsonToGrpc 等）
  └── 否 → spring-cloud-starter-gateway-server-webmvc
            └── 理由：MVC 代码 + 虚拟线程（JDK 21+），Servlet 生态兼容

需要 JsonToGrpc / LocalResponseCache / 极致 P99？
  ├── 需要 → WebFlux（这些过滤器 MVC 没有）
  └── 不需要 → 均可（按团队能力选）

现有 4.x WebFlux 网关升级？
  └── 直接迁 webflux starter（换坐标+前缀，行为不变）
```

| 决策维度 | WebFlux 胜 | WebMVC 胜 |
|---------|-----------|----------|
| 极致性能 | ✅（压测 P99 略优） | — |
| 团队技术栈 | 响应式团队 | MVC 团队 |
| Servlet 生态兼容 | ❌ | ✅（旧 Filter/中间件） |
| 响应式独有过滤器 | ✅ | ❌ |
| 开发效率 | 响应式曲线 | 同步直觉 |
| JDK 要求 | 17+ | 21+（虚拟线程） |

> 🎯 最终建议：**2026 年新网关项目：MVC 团队选 WebMVC + 虚拟线程（开发效率优先），性能敏感/已有响应式基建选 WebFlux**——两者都是官方主流，不存在"必须 WebFlux"了。

---

**下一模块**：[10-生产实践与选型避坑](10-生产实践与选型避坑.md)　**返回总览**：[00-Spring Cloud Gateway知识体系总览](00-Spring Cloud Gateway知识体系总览.md)

**【参考来源】**：[Spring Cloud Gateway 5.0.2（Server Web MVC）](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webmvc.html)、[Spring Cloud Gateway 5.0.2（Proxy Exchange）](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-proxyexchange.html)、[Spring Cloud 2025.1.0（Oakwood）发布公告](https://spring.io/blog/2025/11/25/spring-cloud-2025-1-0-aka-oakwood-has-been-released/)
