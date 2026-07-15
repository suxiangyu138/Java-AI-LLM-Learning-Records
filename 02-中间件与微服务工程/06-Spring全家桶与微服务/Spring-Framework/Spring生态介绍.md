# Spring 生态介绍

> **定位**：以 Spring Framework 为核心，覆盖 Web/数据/安全/微服务/消息/批处理全场景的企业级 Java 解决方案。核心思想：DI + AOP。

---

## 目录

1. [Spring Framework 核心模块](#1-spring-framework-核心模块)
2. [核心子项目](#2-核心子项目)
3. [应用场景](#3-应用场景)

---

## 1. Spring Framework 核心模块

| 模块 | 功能 |
|------|------|
| **Spring Core** | IoC 容器、DI 依赖注入、Bean 管理 |
| **Spring Context** | 上下文环境、国际化、资源加载 |
| **Spring AOP** | 面向切面编程（日志/事务/权限） |
| **Spring JDBC** | `JdbcTemplate` 简化 JDBC 操作 |
| **Spring Web** | Servlet 集成、Spring MVC |
| **Spring Test** | JUnit/TestNG 测试支持 |

---

## 2. 核心子项目

### 2.1 Spring Boot

| 特性 | 说明 |
|------|------|
| **自动配置** | 根据 starter 依赖自动装配组件 |
| **Starter 依赖** | `spring-boot-starter-web` 一键引入全套 Web 依赖 |
| **内嵌服务器** | Tomcat/Jetty/Undertow，`java -jar` 直接运行 |
| **Actuator** | 运行状态监控（健康检查/指标） |

### 2.2 Spring MVC

```text
DispatcherServlet → HandlerMapping → Controller → Service → ViewResolver → View
```

| 组件 | 作用 |
|------|------|
| `DispatcherServlet` | 前端控制器，接收所有请求 |
| `Controller` | 处理器，处理请求并返回结果 |
| `ViewResolver` | 视图解析器 |
| `Interceptor` | 拦截器（权限/日志） |

### 2.3 Spring Data

| 子模块 | 数据源 |
|--------|--------|
| Spring Data JPA | 关系型数据库（JPA） |
| Spring Data Redis | Redis 缓存 |
| Spring Data MongoDB | MongoDB |
| Spring Data Elasticsearch | Elasticsearch 检索 |

### 2.4 Spring Security

| 能力 | 说明 |
|------|------|
| 身份认证 | 用户名密码 / OAuth2.0 / JWT / LDAP |
| 授权管理 | 角色+权限控制 |
| 防护 | CSRF / XSS / 会话固定 |

### 2.5 Spring Cloud

| 组件 | 功能 |
|------|------|
| Eureka / Nacos | 服务注册与发现 |
| Config / Nacos Config | 配置中心 |
| LoadBalancer / Ribbon | 负载均衡 |
| Circuit Breaker / Sentinel | 熔断降级 |
| Gateway | 网关路由 |

### 2.6 其他子项目

| 项目 | 用途 |
|------|------|
| **Spring Batch** | 批处理（数据导入/导出/报表） |
| **Spring AMQP** | RabbitMQ 集成 |
| **Spring Session** | 分布式会话共享 |

---

## 3. 应用场景

| 场景 | 技术栈 |
|------|--------|
| 传统企业应用 | Spring Framework + Spring MVC + Spring Data |
| RESTful API | Spring Boot + Spring MVC |
| 微服务系统 | Spring Boot + Spring Cloud Alibaba |
| 批处理 | Spring Batch |
| 云原生 | Docker + K8s + Spring Cloud |
