# 00 Spring Cloud Gateway 知识体系总览

> 组件卡片：Spring Cloud Gateway 是什么、版本现状、能做什么、与对照体系如何衔接——**基于 Spring WebFlux/Netty 的官方 API 网关（5.0 起双栈：WebFlux + WebMVC）**，统一流量入口，提供路由转发 + 横切能力（认证/限流/熔断/监控）；对照体系见 [Spring Cloud CircuitBreaker](../Spring Cloud CircuitBreaker/00-Spring Cloud CircuitBreaker知识体系总览.md)、[Sentinel：流量控制熔断降级](../Spring Cloud Alibaba（阿里实现，属于 Spring Cloud 生态实现）/Sentinel：流量控制熔断降级/00-Sentinel知识体系总览.md) 与 [后端分布式常用名词通俗解释](../../../../../09-Web开发全流程/后端 分布式常用名词通俗解释/)。

---

## 📚 目录

1. [组件一句话定位](#1-组件一句话定位)
2. [版本现状（2026-08）](#2-版本现状2026-08)
3. [能力地图](#3-能力地图)
4. [与对照体系的映射](#4-与对照体系的映射)
5. [快速上手 3 步](#5-快速上手-3-步)
6. [速查导航](#6-速查导航)
7. [学习路线推荐](#7-学习路线推荐)
8. [核心概念速查](#8-核心概念速查)

---

## 1. 组件一句话定位

**Spring Cloud Gateway 是 Spring Cloud 官方的 API 网关**——所有外部/内部流量先打到网关，由网关按**路由规则**（谓词匹配 + 过滤器链）转发到下游微服务，并在转发过程中统一施加**认证、限流、熔断、日志、跨域**等横切能力——是微服务架构的**唯一流量入口（门面）**。

```text
核心心智模型：
              ┌─────────────────────────────────────────────┐
   客户端 ───► │            Spring Cloud Gateway             │
   (App/Web)  │  路由(Route)：id + uri + 谓词 + 过滤器链      │
              │  ├── Predicate 匹配（Path/Host/Header...）   │
              │  ├── GatewayFilter 链（改写/限流/熔断/认证）   │
              │  └── GlobalFilter 链（跨所有路由的横切）      │
              └──────────────┬──────────────────────────────┘
                             ▼ 转发
              ┌────────┬─────┴─────┬────────┐
              ▼        ▼           ▼        ▼
        order-service  payment   user      ...（下游微服务）
```

| 维度 | 说明 |
|------|------|
| 所属 | Spring Cloud 官方组件（基于 Spring WebFlux/Netty，5.0 起双栈） |
| 版本线 | **5.x 主线（5.0.2 当前主流，2025.1/Oakwood，Boot 4）**；4.3.x 供 Boot 3.5 |
| 技术栈 | **WebFlux + Reactor Netty（非阻塞默认）**；5.0 新增 **WebMVC + 虚拟线程**版本 |
| 路由定义 | yaml / Java Fluent DSL / DiscoveryClient（注册中心自动发现） |
| 核心能力 | 路由转发、谓词匹配、过滤器链、限流、熔断、TLS、跨域 |
| 定位 | 只做"入口流量治理"——不注册服务、不存配置（配套 Nacos/Eureka + Config） |

### 1.1 Gateway 解决什么问题

**① 统一入口**：微服务拆散后，客户端不该知道 50 个服务地址——所有请求打网关，网关按路径/域名/头路由到具体服务（[02 篇](02-快速开始与路由配置速查.md)）。

**② 横切能力下沉**：认证鉴权、限流熔断、日志监控、跨域——**做一次，全部服务生效**，而不是每个服务重复实现（[04 篇](04-过滤器工厂全景（上）.md)）。

**③ 流量治理**：灰度分流（Weight）、蓝绿（谓词组合）、网关级限流熔断（[07 篇](07-网关级限流与熔断.md)）。

| 维度 | 没有网关 | 有网关 |
|------|---------|--------|
| 客户端接入 | 记住所有服务地址，服务拆分即灾难 | 只认识一个网关地址 |
| 认证 | 每个服务各做一套 | 网关统一认证（TokenRelay/全局过滤器） |
| 限流熔断 | 每个服务各自为战 | 网关统一限流熔断（入口保护） |
| 灰度/版本 | 手动改 DNS/路由 | Weight/Header 谓词随时切 |
| 服务变更 | 客户端感知地址变化 | 网关 + 注册中心，客户端无感 |

> 🎯 判断标准一句话：**"微服务数量 > 3 且有外部客户端（App/Web/第三方）？"**——是，上网关；纯内部调用链（服务间 Feign）不需要网关。

### 1.2 适用与不适用场景

| 场景 | 适合？ | 说明 |
|------|:---:|------|
| 外部流量统一入口 | ✅ | 主战场（App/Web/第三方 API） |
| 认证鉴权下沉 | ✅ | 全局过滤器统一处理 |
| 网关级限流熔断 | ✅ | RequestRateLimiter + CircuitBreaker 过滤器 |
| 灰度发布/版本路由 | ✅ | Weight/Header/API Versioning 谓词 |
| 内部服务间调用 | ❌ | 那是 Feign/OpenFeign 的领域（性能损耗不值） |
| 服务注册发现 | ❌ | Nacos/Eureka 的职责（Gateway 消费它） |
| 配置管理 | ❌ | Config/Nacos 的职责 |
| 高吞吐消息转发 | ❌ | MQ 的领域 |

> ⚠️ **最大认知误区**：把网关当"万能代理"或"唯一的服务间通信通道"——**网关是外部入口的门面，内部调用走 Feign 直连**；所有流量都绕网关会造成单点瓶颈与链路放大（[10 篇](10-生产实践与选型避坑.md) 边界）。

### 1.3 与网关方案对照

| 方案 | 技术 | 特点 | 生态 |
|------|------|------|------|
| **Spring Cloud Gateway** | WebFlux/Netty（5.0 双栈） | 谓词+过滤器模型、Spring 生态无缝 | **官方 Cloud** |
| Zuul 1.x | Servlet 阻塞 | 已停止维护（2019） | 遗留 |
| Zuul 2.x | Netty | 开源但 Spring 官方未集成 | 边缘 |
| SCA 网关（Spring Cloud Alibaba） | 基于 Spring Cloud Gateway + Nacos | 与 SCA 生态集成 | 阿里 |
| Nginx/Kong | OpenResty/Lua | 高性能 L7 代理，非 Java 生态 | 独立 |
| 自研（K8s Ingress/Envoy） | 云原生 | 云原生首选，Java 生态弱 | 云原生 |

> 🎯 面试必答：**"Gateway 和 Zuul 什么区别？"**——Zuul 1.x 是 **Servlet 阻塞模型**（同步、每请求一线程），已停止维护；Gateway 是 **WebFlux + Netty 非阻塞模型**（少量线程扛高并发），且提供**谓词+过滤器**的声明式路由模型、内置限流熔断——**Spring Cloud 官方网关的继任者就是 Gateway**，5.0 起还多了 WebMVC 版（虚拟线程时代的"阻塞式同性能"选择，[09 篇](09-WebMVC网关与ProxyExchange（5.0新特性）.md)）。

## 2. 版本现状（2026-08）

| 版本线 | 状态 | 关键点 |
|--------|------|--------|
| **5.x（5.0.2 主流）** | **当前主线** | Spring Cloud **2025.1.x（Oakwood）**、Boot 4.0/4.1、Framework 7；**WebFlux/WebMVC 双栈拆分**（starter 改名）、JSpecify、API Versioning 谓词、Framework Retry 过滤器 |
| 4.3.x（4.3.5） | 存量（EOL） | Spring Cloud 2025.0.x（Northfields）、Boot 3.5；OSS 已于 2026-06-30 结束 |
| 4.2.x（4.2.7） | 存量（EOL） | Spring Cloud 2024.0.x（Moorgate）、Boot 3.4 |
| 4.1.x（4.1.9） | 存量 | Spring Cloud 2023.0.x（Leyton）、Boot 3.2/3.3 |
| 4.0.x | 老存量 | Spring Cloud 2022.0.x（Kilburn）、Boot 3.0/3.1 |
| 3.x | 老存量 | Spring Cloud 2020.0/2021.0、Boot 2.4-2.7 |
| 2.x | 废弃 | Hoxton 时代（2.0 起取代 Zuul） |

> ⚠️ **版本策略（2026 起）**：**新项目 Boot 4.x + Cloud 2025.1.x + Gateway 5.0.x**；存量 Boot 3.5 用 4.3.x（**OSS 已 EOL**）；**5.0 有 starter 与属性前缀变更，升级不是换版本号那么简单**（[01 篇](01-模块清单与版本矩阵.md) 迁移要点）。

### 2.1 5.0 关键变化（4.3.x → 5.0.x 迁移要点）

| 变化 | 说明 |
|------|------|
| **双栈拆分** | 旧 `spring-cloud-starter-gateway` 废弃 → `spring-cloud-starter-gateway-server-webflux`（响应式，原行为）+ `spring-cloud-starter-gateway-server-webmvc`（Servlet+虚拟线程，新） |
| **属性前缀改名** | `spring.cloud.gateway.*` → `spring.cloud.gateway.server.webflux.*`；`spring.cloud.gateway.mvc.*` → `spring.cloud.gateway.server.webmvc.*`（可用 properties-migrator 过渡） |
| Framework Retry 过滤器 | 新增基于 Spring Framework 7 retry 的重试过滤器 |
| API Versioning 谓词 | Server WebFlux 新增版本路由谓词 |
| JSpecify | 全公共 API null-safety 注解 |
| Bucket4jRateLimiter | 桶式限流支持 |
| 旧 starter 警告 | 用旧坐标启动打 deprecation 警告，未来移除 |

### 2.2 关键演进时间线

| 版本 | 里程碑 |
|------|--------|
| 2.0（2018） | Gateway 诞生，WebFlux/Netty，取代 Zuul |
| 3.0（2020-2021） | 对齐 Boot 2.4-2.7，Ilford/Jubilee 线 |
| 4.0（2022.12） | **Boot 3 首线**：jakarta、AOT/原生镜像 |
| 4.1-4.3（2023-2025） | Leyton/Moorgate/Northfields 维护线 |
| 5.0（2025.11） | **Oakwood 大版本**：**WebFlux/WebMVC 双栈**、starter 重构、Framework Retry 过滤器、API Versioning |

## 3. 能力地图

| 能力域 | 能力点 | 对应机制 |
|--------|--------|---------|
| 路由 | 路由匹配与转发 | Route + Predicate + Filter（yaml/Java/Discovery） |
| 匹配 | 请求多维匹配 | 14+ 谓词工厂（Path/Host/Header/Weight/时间/版本...） |
| 改写 | 请求/响应改写 | Add/Set/Remove 头与参数、RewritePath、ModifyBody |
| 转发 | 负载均衡转发 | lb:// + LoadBalancer 集成（[02 篇](02-快速开始与路由配置速查.md)） |
| 限流 | 网关级限流 | RequestRateLimiter（Redis 令牌桶）、Bucket4j |
| 熔断 | 网关级熔断降级 | CircuitBreaker 过滤器（基于 SCCB，[07 篇](07-网关级限流与熔断.md)） |
| 重试 | 转发失败重试 | Retry 过滤器（5.0 起 Framework Retry 新实现） |
| 会话 | 会话与 SSO | SaveSession、TokenRelay（OAuth2） |
| 安全 | 请求安全头 | SecureHeaders（HSTS/XSS 等 10+ 安全头） |
| TLS | 网关 HTTPS 终结 | 标准 Boot SSL + 下游信任配置 |
| 可观测 | 端点与监控 | Actuator API（routes/refresh）、访问日志 |
| 扩展 | 自定义 | 自定义谓词/过滤器（Java）、GlobalFilter |
| 多实例 | 路由共享 | Redis 路由共享（多网关实例同路由表） |

### 3.1 能力边界：Gateway 不做什么

| 能力边界 | 谁来做 | 说明 |
|---------|--------|------|
| 注册发现 | Nacos/Eureka/Consul | Gateway **消费**注册中心（DiscoveryClient 路由） |
| 配置管理 | Spring Cloud Config / Nacos | Gateway 本身也是普通客户端 |
| 业务逻辑 | 下游服务 | 网关不做业务，只做转发与横切 |
| 分布式事务 | Seata | 与网关无关 |
| 消息解耦 | MQ | 与网关无关 |

> ⚠️ **常见归因错误**：网关转发失败怪"路由配置"——**排查路径："谓词匹配没匹配上（404）→ 路由存在与否（/actuator/gateway/routes）→ 下游可达性（lb:// 服务名）→ 过滤器报错"**（[10 篇](10-生产实践与选型避坑.md) 排错表）。

## 4. 与对照体系的映射

| 速查文档 | 对照/深度体系 |
|---------|-------------|
| 05-过滤器（CircuitBreaker） | [Spring Cloud CircuitBreaker](../Spring Cloud CircuitBreaker/00-Spring Cloud CircuitBreaker知识体系总览.md)（网关熔断的底层实现） |
| 07-限流熔断 | [Sentinel：流量控制熔断降级](../Spring Cloud Alibaba（阿里实现，属于 Spring Cloud 生态实现）/Sentinel：流量控制熔断降级/00-Sentinel知识体系总览.md)（SCA 网关限流对照） |
| 02-路由 | [Spring Cloud LoadBalancer](../Spring Cloud LoadBalancer/)（lb:// 负载均衡）、[Nacos 注册中心](../Spring Cloud Alibaba（阿里实现，属于 Spring Cloud 生态实现）/Nacos：注册中心 + 配置中心/00-Nacos知识体系总览.md)（Discovery 路由） |
| 06-原理 | [Spring 框架核心](../../../../Spring框架核心/)（Reactor/WebFlux 底座）、[后端分布式常用名词通俗解释](../../../../../09-Web开发全流程/后端 分布式常用名词通俗解释/)（网关/限流白话） |
| 10-选型 | [后端分布式常用名词通俗解释](../../../../../09-Web开发全流程/后端 分布式常用名词通俗解释/)（Nginx vs 网关架构对比） |

> 💡 分工约定：**本系列回答"网关怎么配、路由怎么定义、限流熔断怎么做、双栈怎么选"**；熔断底层的状态机原理在 [CircuitBreaker 系列](../Spring Cloud CircuitBreaker/00-Spring Cloud CircuitBreaker知识体系总览.md)，限流的服务端方案在 [Sentinel 系列](../Spring Cloud Alibaba（阿里实现，属于 Spring Cloud 生态实现）/Sentinel：流量控制熔断降级/00-Sentinel知识体系总览.md) 对照。

## 5. 快速上手 3 步

**① 加依赖**（5.0 起用新坐标）：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway-server-webflux</artifactId>
    <!-- 5.0.x：WebFlux 栈（原 starter 已废弃）
         若选 WebMVC 栈：spring-cloud-starter-gateway-server-webmvc（见 09 篇） -->
</dependency>
```

**② 配一条路由**（application.yml）：

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: order-service           # ★ 路由唯一 ID
              uri: lb://order-service     # ★ 转发目标（lb:// = 注册中心负载均衡）
              predicates:                 # ★ 匹配条件（谓词，AND 组合）
                - Path=/orders/**
              filters:                    # ★ 转发前的横切处理（可选）
                - StripPrefix=1           # 去掉一级路径前缀
                - AddRequestHeader=X-Env, gateway
```

**③ 启动访问**：

```text
GET http://localhost:8080/orders/1
  → 匹配 Path=/orders/** → StripPrefix=1 → 转发到 lb://order-service/1
```

> 🎯 跑通即及格：**① 起网关 + 下游服务 + 注册中心；② 访问网关路径拿到下游响应（转发成功）；③ `/actuator/gateway/routes` 能看到路由定义**——三件事都通，网关主链路打通（[02 篇](02-快速开始与路由配置速查.md)）。

### 5.1 三步验证与排障起点

| 步骤 | 做法 | 预期 |
|------|------|------|
| ① 路由生效 | GET 网关路径 | 转发到下游并返回数据 |
| ② 路由可见 | GET /actuator/gateway/routes | 路由列表含 order-service（谓词/过滤器 JSON） |
| ③ 匹配验证 | 错误路径请求 | 404（谓词没匹配），确认谓词规则 |

> 💡 排障起点：**先看 `/actuator/gateway/routes`（路由在不在）→ 再看谓词匹配（404 vs 500）→ 最后看下游可达性**——404 是网关没匹配，500 是转发了但下游/过滤器出错（[10 篇](10-生产实践与选型避坑.md) 排错表）。

## 6. 速查导航

| 文档 | 内容 |
|------|------|
| [01-模块清单与版本矩阵](01-模块清单与版本矩阵.md) | **双栈拆分**（新旧 starter 坐标）、属性前缀迁移、版本矩阵、5.0 变化 |
| [02-快速开始与路由配置速查](02-快速开始与路由配置速查.md) | 路由四要素、yaml 定义、三种路由方式（yaml/Java/Discovery）、验证 |
| [03-路由谓词工厂速查](03-路由谓词工厂速查.md) | 14+ 谓词全清单、配置语法、组合语义 |
| [04-过滤器工厂全景（上）](04-过滤器工厂全景（上）.md) | 请求/响应改写类过滤器（头/路径/参数/体/状态码/重试） |
| [05-过滤器工厂全景（下）与全局过滤器](05-过滤器工厂全景（下）与全局过滤器.md) | 防护类（熔断/限流/重试）+ 会话类 + 自定义 + GlobalFilter + 顺序 |
| [06-核心原理：WebFlux 处理链路](06-核心原理：WebFlux 处理链路.md) | HandlerMapping 匹配 → WebHandler 过滤链 → 转发、vs Zuul |
| [07-网关级限流与熔断](07-网关级限流与熔断.md) | RequestRateLimiter（Redis 令牌桶）、Bucket4j、CircuitBreaker 过滤器 |
| [08-高级特性速查](08-高级特性速查.md) | TLS/CORS/Discovery 路由/Java DSL/Actuator/共享路由/AOT/性能 |
| [09-WebMVC网关与ProxyExchange（5.0新特性）](09-WebMVC网关与ProxyExchange（5.0新特性）.md) | 双栈背景（虚拟线程）、WebMVC 差异、ProxyExchange、选型 |
| [10-生产实践与选型避坑](10-生产实践与选型避坑.md) | vs Nginx/Kong、坑位表、调优、升级路径（4.3→5.0） |

### 6.1 阅读顺序建议

- **第一次接触**：02（路由跑通）→ 03（谓词）→ 04/05（过滤器）；
- **项目实战**：02 → 07（限流熔断）→ 08 高级 → 10 避坑；
- **面试冲刺**：06 原理 → 03 谓词 → 00 vs Zuul；
- **Boot 4 新特性**：09 WebMVC 双栈（5.0 亮点）。

### 6.2 前置知识建议

| 前置主题 | 所在文档 | 与本系列的关系 |
|---------|---------|--------------|
| 熔断原理 | [Spring Cloud CircuitBreaker](../Spring Cloud CircuitBreaker/00-Spring Cloud CircuitBreaker知识体系总览.md) | 网关 CircuitBreaker 过滤器的底层 |
| 负载均衡 | [Spring Cloud LoadBalancer](../Spring Cloud LoadBalancer/) | lb:// 前缀的转发实现 |
| 注册中心 | [Nacos 注册中心](../Spring Cloud Alibaba（阿里实现，属于 Spring Cloud 生态实现）/Nacos：注册中心 + 配置中心/00-Nacos知识体系总览.md) | DiscoveryClient 路由 |
| WebFlux/Reactor | [Spring 框架核心](../../../../Spring框架核心/) | 网关的非阻塞底座 |

## 7. 学习路线推荐

| 路线 | 适用 | 顺序 |
|------|------|------|
| 入门速查 | 会用 Spring Boot | 00 总览 → 02 路由 → 03 谓词 → 跑通 |
| 项目实践 | 上生产 | 02 → 07 限流熔断 → 08 高级 → 10 避坑 |
| 面试冲刺 | 全考点 | 06 原理 → 03 谓词 → 00 vs Zuul |
| 架构设计 | 网关选型负责人 | 09 双栈 → 10 vs Nginx → 07 防护设计 |

## 8. 核心概念速查

| 术语 | 一句话解释 |
|------|-----------|
| Route（路由） | id + uri + 谓词 + 过滤器 四要素的最小路由单元 |
| Predicate（谓词） | 路由匹配条件（Path/Host/Header...），AND 组合 |
| GatewayFilter | 路由级过滤器（限流/熔断/改写） |
| GlobalFilter | 全局过滤器（跨所有路由的横切） |
| uri: lb:// | 从注册中心按服务名负载均衡转发 |
| StripPrefix | 剥掉路径前缀 |
| RewritePath | 正则重写路径 |
| RequestRateLimiter | 网关限流过滤器（Redis 令牌桶） |
| CircuitBreaker 过滤器 | 网关熔断（基于 SCCB，配 fallbackUri 兜底） |
| Retry 过滤器 | 转发失败重试（5.0 起 Framework Retry 实现） |
| TokenRelay | OAuth2 token 透传 |
| WebFlux / WebMVC | 5.0 双栈：非阻塞 / Servlet+虚拟线程 |
| ProxyExchange | 嵌入式代理（注解方法内转发） |
| DiscoveryClient 路由 | 注册中心驱动的路由（lb:// 前提） |

---

**下一模块**：[01-模块清单与版本矩阵](01-模块清单与版本矩阵.md)　**返回总览**：本页

**【参考来源】**：[Spring Cloud Gateway 5.0.2 官方参考文档](https://docs.spring.io/spring-cloud-gateway/reference/)、[Spring Cloud 2025.1.0（Oakwood）发布公告](https://spring.io/blog/2025/11/25/spring-cloud-2025-1-0-aka-oakwood-has-been-released/)、[spring-cloud-gateway GitHub Releases](https://github.com/spring-cloud/spring-cloud-gateway/releases)、[Spring Cloud 2025.0.0（Northfields）发布公告](https://spring.io/blog/2025/05/29/spring-cloud-2025-0-0-is-abvailable)
