# Spring 生态体系

> **核心设计理念**：IoC（控制反转）+ AOP（面向切面）+ 约定优于配置。

---

## 1. 生态层级结构

```text
基础层：Spring Framework（IoC/AOP 核心）
    │
应用层：Spring Boot（自动配置 + Starter + 内嵌容器）
    │
分布式层：Spring Cloud（注册发现 + 配置中心 + 熔断 + 网关）
    │
专项能力层：Spring Data / Security / Batch / AMQP（按需集成）
```

---

## 2. 核心组件速查

| 层级 | 组件 | 核心能力 |
|------|------|----------|
| **基础** | Spring Framework | IoC 容器、AOP、JDBC、MVC、事务 |
| **应用** | Spring Boot | 自动配置、Starter、内嵌容器、Actuator |
| **治理** | Spring Cloud | Eureka/Nacos、Gateway、Feign、Sentinel |
| **数据** | Spring Data | JPA、Redis、MongoDB、ES 统一 API |
| **安全** | Spring Security | 认证（OAuth2/JWT）+ 授权 |
| **消息** | Spring AMQP | RabbitMQ 集成 |
| **批处理** | Spring Batch | 数据导入/导出/清洗/报表 |

---

## 3. 学习路径

```text
入门：Spring Framework（IoC/AOP）
    ↓
进阶：Spring Boot + Spring MVC + Spring Data + Spring Security
    ↓
高级：Spring Cloud 微服务 + 分布式事务 + 性能优化
    ↓
实战：单体（Spring Boot）→ 微服务（Spring Cloud）→ 按需集成专项组件
```
