# Spring 家族全体成员

## 概述

Spring 生态是 Java 后端开发的核心基础设施。本文按 **核心 → Web → 数据 → 消息 → 安全 → 微服务/云原生 → AI → 其他** 八大板块对 Spring 生态组件进行系统归类，便于面试复习与技术选型快速查阅。

---

## 一、核心全家桶（地基）

| 组件 | 定位 | 核心能力 |
|------|------|---------|
| **Spring Framework** | 整个家族的根 | IoC 容器、AOP、事务管理、JDBC 模板、事件机制——所有上层框架均构建于其上 |
| **Spring Boot** | 快速开发框架 | 自动配置（`@EnableAutoConfiguration`）、内嵌服务器（Tomcat/Jetty/Undertow）、starter 起步依赖、Actuator 监控端点 |
| **Spring Cloud** | 微服务全家桶 | 基于 Spring Boot 的分布式系统解决方案：服务发现、配置管理、负载均衡、熔断降级、网关路由 |

### 层次关系

```
Spring Cloud（微服务治理）
    └── Spring Boot（快速开发 + 自动配置）
            └── Spring Framework（IoC + AOP + 事务）
```

---

## 二、Web 层

| 组件 | 定位 | 技术特点 |
|------|------|---------|
| **Spring MVC** | 传统 Servlet 型 Web 框架 | 基于 Servlet API，同步阻塞模型，DispatcherServlet 核心调度，适合大多数 Web 场景 |
| **Spring WebFlux** | 响应式 Web 框架 | 基于 Reactor，非阻塞 I/O，适用于高并发、事件驱动场景（如 API Gateway、实时推送） |

### 选型对比

| 维度 | Spring MVC | Spring WebFlux |
|------|:---:|:---:|
| 底层模型 | Servlet（同步阻塞） | Netty/Undertow（异步非阻塞） |
| 数据库支持 | JDBC、JPA（完善） | R2DBC（生态发展中） |
| 适用场景 | 常规 Web 应用、CRUD | 高并发网关、流式处理、实时服务 |
| 学习成本 | 低 | 中高（需理解响应式编程范式） |

---

## 三、数据访问层

| 组件 | 对应数据库/场景 | 核心能力 |
|------|---------------|---------|
| **Spring JDBC** | 关系型数据库 | 最基础的 JDBC 模板（JdbcTemplate），轻量、直接 |
| **Spring Data JPA** | 关系型数据库 | 基于 Hibernate，Repository 接口自动实现，几乎不用写 SQL |
| **Spring Data Redis** | Redis | RedisTemplate、RedisRepository，简化 KV 操作 |
| **Spring Data MongoDB** | MongoDB | MongoTemplate，简化文档型数据库操作 |
| **Spring Data Elasticsearch** | Elasticsearch | ElasticsearchRestTemplate，简化全文检索与聚合操作 |
| **MyBatis-Spring / MyBatis-Plus** | 关系型数据库 | Spring 整合 MyBatis，MapperScannerConfigurer 自动扫描，MyBatis-Plus 提供增强 CRUD |

> **Spring Data 系列命名规范**：`Spring Data + 数据库名` 为 Spring 官方封装；第三方框架（如 MyBatis）有其独立的整合包。

---

## 四、消息与集成

| 组件 | 对应消息中间件 | 核心能力 |
|------|--------------|---------|
| **Spring AMQP** | RabbitMQ | RabbitTemplate、`@RabbitListener` 注解、消息确认与重试 |
| **Spring Kafka** | Kafka | KafkaTemplate、`@KafkaListener` 注解、批量消费 |
| **Spring JMS** | ActiveMQ / 传统 MQ | JmsTemplate，传统 Java 消息服务标准接口 |

### 消息组件选型

| 场景 | 推荐组件 | 原因 |
|------|---------|------|
| 业务解耦、可靠消息 | Spring AMQP + RabbitMQ | 可靠性高，路由灵活 |
| 大数据流、日志采集 | Spring Kafka + Kafka | 高吞吐，持久化强 |
| 传统企业系统对接 | Spring JMS | 符合 JMS 标准，兼容老系统 |

---

## 五、安全与权限

| 组件 | 核心能力 |
|------|---------|
| **Spring Security** | 认证（Authentication）与授权（Authorization）全栈方案，支持表单登录、JWT、OAuth2、OIDC、RBAC 权限模型 |
| **OAuth2 / OIDC** | 第三方登录与开放授权协议，Spring Security 内置完整的 OAuth2 Client / Resource Server 支持 |

---

## 六、微服务与云原生（Spring Cloud 子项目）

### 6.1 核心组件

| 组件 | 所属生态 | 核心能力 |
|------|---------|---------|
| **Spring Cloud Netflix** | Spring Cloud（老一代） | Eureka（注册中心）、Ribbon（负载均衡）、Feign（声明式 HTTP）、Hystrix（熔断） |
| **Spring Cloud Alibaba** | Spring Cloud（国内主流） | Nacos（注册中心+配置中心）、Sentinel（限流熔断）、Seata（分布式事务）、RocketMQ（消息队列） |
| **Spring Cloud Gateway** | Spring Cloud | API 网关：路由转发、限流过滤、跨域处理、负载均衡 |
| **Spring Cloud Config** | Spring Cloud | 统一配置中心，支持 Git/SVN/本地文件作为配置源 |
| **Spring Cloud OpenFeign** | Spring Cloud | 声明式 HTTP 客户端，接口 + 注解方式调用远程服务 |

### 6.2 新旧两代对比

| 维度 | Netflix 系（老） | Alibaba 系（新，国内主流） |
|------|:---:|:---:|
| 注册中心 | Eureka（已停更） | Nacos（活跃维护） |
| 负载均衡 | Ribbon（已停更） | Spring Cloud LoadBalancer |
| 熔断降级 | Hystrix（已停更） | Sentinel（更灵活的控制台） |
| 分布式事务 | 无原生方案 | Seata（AT/TCC/Saga 模式） |
| 配置中心 | Spring Cloud Config | Nacos Config（实时刷新更便捷） |

> **当前推荐**：新项目统一使用 Spring Cloud Alibaba 生态。

---

## 七、AI 大模型

| 组件 | 核心能力 |
|------|---------|
| **Spring AI** | Spring 官方大模型集成框架，统一抽象层对接 ChatGPT、通义千问、文心一言等主流模型，内置 RAG、Function Calling、Prompt 模板管理、向量存储抽象 |

---

## 八、其他常用组件

| 组件 | 用途 | 典型场景 |
|------|------|---------|
| **Spring Batch** | 批处理框架 | 定时大批量数据读写（如日终对账、数据迁移、报表生成） |
| **Spring Shell** | 命令行应用框架 | 运维工具、管理脚本、交互式 CLI 工具 |
| **Spring HATEOAS** | RESTful 超媒体增强 | 在 API 响应中添加资源链接，构建真正符合 REST 成熟度的服务 |

---

## 九、生态全景速查

### 依赖关系地图

```
                          Spring AI
                            │
      ┌─────────────────────┼─────────────────────┐
      │                     │                     │
  Spring Cloud        Spring Security        Spring Batch
  (微服务治理)         (认证授权)             (批处理)
      │                     │                     │
      └─────────────────────┼─────────────────────┘
                            │
                       Spring Boot
                   (自动配置 + Starter)
                            │
             ┌──────────────┼──────────────┐
             │              │              │
        Spring MVC    Spring WebFlux  Spring Data
         (同步Web)     (响应式Web)     (数据访问)
             │              │              │
             └──────────────┼──────────────┘
                            │
                    Spring Framework
                   (IoC + AOP + 事务)
```

### 按功能速查

| 功能域 | 核心组件 |
|--------|---------|
| 根基 | Spring Framework |
| 快速开发 | Spring Boot |
| Web 层 | Spring MVC / WebFlux |
| 数据层 | Spring Data JPA / Redis / MongoDB / Elasticsearch + MyBatis 整合 |
| 消息 | Spring AMQP / Spring Kafka |
| 安全 | Spring Security |
| 微服务 | Spring Cloud Alibaba（Nacos / Sentinel / Seata / Gateway / OpenFeign） |
| AI | Spring AI |
| 批处理 | Spring Batch |

---

*最后更新：2026-07-15*
