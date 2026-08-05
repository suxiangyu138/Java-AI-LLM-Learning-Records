# 01 - Spring 家族版图全览

> 🎯 Spring 生态 17+ 成员一图看懂 — 每个家族成员的定位、核心模块、适用场景。目标是"提到任何 Spring 项目，立刻说出它在家族中的位置"

---

## 目录

1. [家族总览：一张图](#1-家族总览一张图)
2. [核心层：Spring Framework](#2-核心层spring-framework)
3. [开发层：Spring Boot 与周边](#3-开发层spring-boot-与周边)
4. [数据层：Spring Data 家族](#4-数据层spring-data-家族)
5. [安全层：Spring Security](#5-安全层spring-security)
6. [微服务层：Spring Cloud 与 Alibaba](#6-微服务层spring-cloud-与-alibaba)
7. [AI 层：Spring AI](#7-ai-层spring-ai)
8. [其他重要成员](#8-其他重要成员)

---

## 1. 家族总览：一张图

```text
┌─────────────────────────────────────────────────────────┐
│                    Spring 生态家族                       │
├───────────────┬─────────────────────────────────────────┤
│ 核心层         │ Spring Framework（IoC容器 + AOP + 事务）  │
│               │   └─ Spring MVC（Web） / WebFlux（响应式）│
├───────────────┼─────────────────────────────────────────┤
│ 开发层         │ Spring Boot（自动配置 + 起步依赖）        │
│               │   └─ Boot 生态：Actuator/Config/Test     │
├───────────────┼─────────────────────────────────────────┤
│ 数据层         │ Spring Data（JPA/Redis/MongoDB/ES）     │
│               │   └─ 配套：Spring JDBC / MyBatis（第三方）│
├───────────────┼─────────────────────────────────────────┤
│ 安全层         │ Spring Security + Authorization Server │
├───────────────┼─────────────────────────────────────────┤
│ 微服务层       │ Spring Cloud（Nacos/Feign/Gateway/...） │
│               │   └─ Spring Cloud Alibaba（国产组件）     │
├───────────────┼─────────────────────────────────────────┤
│ AI 层          │ Spring AI（ChatClient/RAG/Agent/MCP）   │
├───────────────┼─────────────────────────────────────────┤
│ 周边层         │ Batch/Integration/Modulith/GraphQL/     │
│               │ Session/Shell/StateMachine/Authorization │
└───────────────┴─────────────────────────────────────────┘
```

---

## 2. 核心层：Spring Framework

| 模块 | 定位 | 核心能力 | 对标 |
|------|------|----------|------|
| spring-core / context | **IoC 容器** | Bean 管理、依赖注入、生命周期 | 一切框架的地基 |
| spring-aop / aspects | AOP | 切面、动态代理、声明式事务基础 | 拦截器体系 |
| spring-tx | 事务抽象 | @Transactional、声明式事务 | 数据库事务管理 |
| spring-webmvc | 传统 Web | Servlet 栈、@RestController | JavaWeb 三件套的替代 |
| spring-webflux | 响应式 Web | WebFlux、Reactive Streams、高并发 IO | Node.js 风格异步 |
| spring-context-support | 集成 | 定时任务（Quartz）、缓存抽象、Mail | 工具集成层 |

**追问：** Framework 和 Boot 的关系？→ Framework 是核心引擎（手动配置复杂）；Boot 是"开箱即用"的封装层 — **Boot 依赖 Framework，Framework 可脱离 Boot 单独用**。

---

## 3. 开发层：Spring Boot 与周边

### 3.1 Spring Boot 四大核心

| 核心机制 | 说明 | 记忆点 |
|----------|------|--------|
| 自动配置（AutoConfiguration） | 根据 classpath 依赖自动装配 Bean | 以约定代替配置 |
| 起步依赖（Starter） | 一个依赖引入完整能力组 | `spring-boot-starter-web` |
| 外部化配置 | application.yml + 优先级体系 | 环境隔离（dev/prod） |
| Actuator | 生产监控端点（health/metrics） | 运维标配 |

### 3.2 Boot 周边成员

| 成员 | 定位 | 场景 |
|------|------|------|
| Spring Boot Test | 集成测试体系（@SpringBootTest） | 单元/集成测试 |
| Spring Boot DevTools | 热重启 | 本地开发 |
| Spring Boot CLI | 命令行快速原型 | 脚本化 Demo |
| Spring Boot Config | 外部配置服务（云原生方向） | 新项目可关注 |

**追问：** Boot 4 和 Boot 3 的核心差异一句话？→ 基线升级（Jakarta EE 11 + Java 17+）+ 自动配置模块化 + 虚拟线程默认 + AOT 一等公民（详见 03 文件）。

---

## 4. 数据层：Spring Data 家族

| 项目 | 目标数据源 | 核心特性 | 备注 |
|------|-----------|----------|------|
| Spring Data JPA | 关系型数据库 | Repository 接口自动实现、规范方法名 | 依托 Hibernate |
| Spring Data Redis | Redis | RedisTemplate、缓存注解 | 配合 Redisson |
| Spring Data MongoDB | MongoDB | Document 映射、聚合查询 | NoSQL |
| Spring Data Elasticsearch | ES | 仓储接口、查询 DSL | 检索引擎 |
| Spring Data JDBC | 关系型 | 轻量 JDBC 抽象（无 JPA 魔法） | 简单 CRUD |
| Spring JDBC（JdbcTemplate） | 关系型 | 模板化 JDBC | 最底层 |

> 💡 **国内现状**：互联网企业关系型持久层常用 **MyBatis/MyBatis-Plus**（非 Spring 官方）— 因为 SQL 可控、优化方便；Spring Data JPA 更符合"领域模型优先"的官方风格。两者选型见 05 文件。

---

## 5. 安全层：Spring Security

| 能力 | 说明 | 2026 现状 |
|------|------|-----------|
| 认证 | 表单/OAuth2/OIDC/JWT | Security 7 增强 OIDC、现代化加密默认值 |
| 授权 | 方法级 @PreAuthorize、URL 规则 | 零信任微服务安全模式 |
| 扩展 | 过滤器链架构、自定义 AuthenticationProvider | MFA 改进 |
| 生态 | Spring Authorization Server（授权服务器） | 可替代自建 OAuth 服务 |

**追问：** Spring Security 的过滤器链是什么？→ SecurityFilterChain 组织多个 Filter（认证→授权→异常处理），Spring Boot 自动装配默认链，可自定义排序。JWT vs Session？→ 无状态（水平扩展友好）vs 有状态（可主动吊销），微服务多选 JWT。

---

## 6. 微服务层：Spring Cloud 与 Alibaba

### 6.1 Spring Cloud 官方组件（2026）

| 组件 | 职责 | Boot 4 时代的趋势 |
|------|------|-------------------|
| Spring Cloud Commons | 抽象层 | — |
| OpenFeign | 声明式 HTTP 客户端 | **正被 @HttpServiceClient 替代**（Boot 4） |
| Gateway | 网关（非阻塞） | 保持主流 |
| LoadBalancer | 客户端负载均衡 | 替代已退役的 Ribbon |
| Config / Bus | 配置中心与总线 | 常被 Nacos 替代（国内） |
| Sleuth → Micrometer Tracing | 链路追踪 | OpenTelemetry 统一 |
| Circuit Breaker | 熔断抽象 | 内置 @Retryable/@ConcurrencyLimit 分流 |

### 6.2 Spring Cloud Alibaba（国内主流）

| 组件 | 职责 | 说明 |
|------|------|------|
| Nacos | 注册中心 + 配置中心 | 国内微服务标配 |
| Sentinel | 熔断/限流/降级 | 比 Hystrix 灵活 |
| Seata | 分布式事务 | AT/TCC/SAGA 模式 |
| RocketMQ | 消息（经 Spring Messaging 整合） | 阿里生态 |
| Spring AI Alibaba | AI 集成（通义千问/百炼） | 2026 已 1.0 GA |

**追问：** Spring Cloud 官方 vs Alibaba 怎么选？→ 国内业务多选 Alibaba（Nacos 生态成熟、文档中文）；出海/国际化项目选官方（Eureka→Consul/Gateway 全官方）。底层可混搭（Nacos 注册 + 官方 Gateway）。

---

## 7. AI 层：Spring AI

| 能力 | 说明 |
|------|------|
| ChatClient | 统一模型 API：20+ 模型（OpenAI/Anthropic/DeepSeek/通义/Ollama） |
| RAG | 20+ 向量库抽象 + ETL 管道 + Advisor |
| Agent | 5 种工作流模式 + MCP 工具生态 |
| MCP | 客户端/服务端 Starter，Spring 团队是 MCP 协议 Java SDK 提供方 |
| 记忆 | 短期窗口 + 长期向量记忆 + 压缩 |
| 版本 | 1.0/1.1 稳定线（Boot 3）+ 2.0 前沿线（Boot 4，Java 21+） |

> 💡 详见本体系 04 文件 — Spring AI 是 Java 开发者进入 AI 应用层的官方路径，与 LangChain4j 互为竞争。

---

## 8. 其他重要成员

| 成员 | 定位 | 典型场景 |
|------|------|----------|
| Spring Batch | 批处理框架 | 定时大规模数据迁移、ETL |
| Spring Integration | 企业集成模式（EIP） | 消息流、外部系统编排 |
| Spring Modulith | 模块化单体架构 | 单体 + 模块边界（2026 重点推荐方向） |
| Spring GraphQL | GraphQL API | 灵活查询 API |
| Spring Session | 分布式会话 | Session 存入 Redis |
| Spring Shell | 命令行应用 | 运维工具、交互式 CLI |
| Spring StateMachine | 状态机 | 订单状态流转 |
| Spring Authorization Server | OAuth2 授权服务器 | 统一认证服务 |
| Spring Cloud Stream | 消息驱动微服务 | 绑定 MQ（RabbitMQ/Kafka） |
| Spring AI Alibaba | 国产模型 AI 集成 | 通义千问/百炼/MultiAgent |

**追问：** Spring Modulith 解决什么？→ 微服务"过度拆分"问题：模块化单体 = 单体部署 + 模块强边界（跨模块只能通过公开 API），拆分的成本低、收益高 — 2026 年"先 Modulith 后微服务"成为新共识。

---

> 🎯 **核心要点**：Spring 家族记忆法 — **核心（Framework）→ 开发（Boot）→ 数据（Data）→ 安全（Security）→ 微服务（Cloud/Alibaba）→ AI（Spring AI）→ 周边（Batch/Modulith/…）** 七个层次。面试被问"Spring 生态有哪些"时，按层回答比背清单高一个段位。

**下一模块**：[02-Spring版本演进与兼容矩阵](02-Spring版本演进与兼容矩阵.md) / **返回总览**：[00-Spring生态知识体系总览](00-Spring生态知识体系总览.md)
