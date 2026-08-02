# 00 - Spring Boot 总览与核心概念

> 定位：Spring Boot 知识体系入口——框架定位、约定优于配置、版本现状（2026-07）、核心术语、模块导航

## 📚 目录

1. [Spring Boot 是什么](#1-spring-boot-是什么)
2. [三大核心理念](#2-三大核心理念)
3. [版本现状与选型（2026-07 验证）](#3-版本现状与选型2026-07-验证)
4. [核心术语表](#4-核心术语表)
5. [模块导航](#5-模块导航)

---

## 1. Spring Boot 是什么

```
Spring Boot = 简化 Spring 开发的框架（Pivotal/Broadcom）

解决的问题：
  Spring 配置地狱（XML 时代）→ 自动配置
  部署繁琐 → 内嵌服务器（jar 直接跑）
  生态整合难 → Starter 一键引入

定位：
  Java 后端事实标准（微服务/单体皆可）
  Spring Cloud 微服务的基石

⚠️ 面试必答：
"Spring Boot = 约定优于配置 + 自动配置
 + Starter 生态——开箱即用、
 独立运行（内嵌 Tomcat）、生产就绪。"
```

---

## 2. 三大核心理念

```
① 约定优于配置（Convention over Configuration）：
   默认配置 + 按需覆盖（约定目录/命名/依赖）

② 自动配置（Auto-Configuration）：
   根据 classpath 依赖自动装配 Bean
   （引入 spring-boot-starter-web → 自动配 Tomcat/MVC）

③ Starter 依赖管理：
   一个 starter 引入一组兼容依赖
   （版本由 Boot BOM 统一管理）

⚠️ 面试必答：
"Boot 三理念——约定优于配置（少写配置）、
 自动配置（依赖即装配）、Starter（一键引入）。
 自动配置原理是面试第一考点（见 01 篇）。"
```

---

## 3. 版本现状与选型（2026-07 验证）

### 3.1 版本线

| 版本 | 发布时间 | 基线 | 状态 |
|------|:---:|------|:---:|
| Spring Boot 3.5.x | 2025 | Java 17+、Jakarta EE 10 | 支持至 2026-11 |
| **Spring Boot 4.0** | 2025-11 | Java 17+、Jakarta EE 11 | ✅ 大版本重构 |
| **Spring Boot 4.1.0** | 2026-06-10 | Java 17+（推荐 21/25） | ✅ 最新 |

### 3.2 Boot 4 核心变化（面试必答）

| 变化 | 说明 |
|------|------|
| 模块化拆分 | 6.2MB 单体 jar → **47 个轻量模块**（启动快、镜像小） |
| Jakarta EE 11 | Servlet 6.1（Tomcat 11）、javax.* 彻底退出 |
| 虚拟线程 | Java 21/25 适配，万级并发 |
| @HttpServiceClient | 声明式 HTTP 客户端（官方替代 Feign） |
| 内置弹性 | @Retryable/@ConcurrencyLimit（替代 Resilience4j） |
| OTel Starter | 一个依赖搞定全链路追踪 |
| 性能 | 启动快 33%（4.2s→2.8s）、镜像小 19% |

```
⚠️ 升级注意：
  Boot 3.x → 4.x：Java 17+、Jakarta EE 11、
  部分属性改名（官方 Migrator 工具辅助）
  3.5.x 支持到 2026-11（新项目直接 4.x）

⚠️ 面试必答：
"Boot 4 最大变化——47 模块拆分 +
 Jakarta EE 11 + 虚拟线程 + 官方
 声明式 HTTP 客户端；性能启动快 33%。"
```

---

## 4. 核心术语表

| 术语 | 一句话定义 | 详见 |
|------|-----------|------|
| 自动配置 | 依赖即装配（AutoConfiguration） | 01 |
| Starter | 依赖集合（web/data-jpa/security） | 01 |
| 条件装配 | @ConditionalOnClass/Property | 01 |
| application.yml | 配置文件（约定位置） | 02 |
| @ConfigurationProperties | 类型安全配置绑定 | 02 |
| Profile | 环境隔离（dev/prod） | 02 |
| Spring MVC | Web 框架（控制器） | 03 |
| 全局异常处理 | @RestControllerAdvice | 03 |
| Spring Data JPA | ORM 数据访问 | 04 |
| MyBatis | SQL 映射框架 | 04 |
| 事务管理 | @Transactional | 04 |
| @SpringBootTest | 集成测试启动类 | 05 |
| MockMvc | MVC 层测试 | 05 |
| Spring Security | 认证授权框架 | 06 |
| Actuator | 生产监控端点 | 07 |
| 优雅关闭 | graceful shutdown | 07 |
| 配置中心 | Nacos/Spring Cloud Config | 08 |
| 服务发现 | 注册中心（Nacos/Eureka） | 08 |

---

## 5. 模块导航

| 序号 | 模块 | 核心内容 | 定位 |
|------|------|---------|------|
| 00 | [本总览](00-SpringBoot总览与核心概念.md) | 理念、版本、术语 | 入口 |
| 01 | [自动配置原理](01-SpringBoot自动配置原理.md) | AutoConfiguration、条件装配、Starter 机制 | 原理（面试核心） |
| 02 | [配置体系](02-SpringBoot配置体系.md) | yml、绑定、Profile、配置优先级 | 配置 |
| 03 | [Web 开发](03-SpringBootWeb开发.md) | MVC、RESTful、参数绑定、异常、HTTP 客户端 | 实战 |
| 04 | [数据访问](04-SpringBoot数据访问.md) | JPA/MyBatis、事务、连接池、多数据源 | 实战 |
| 05 | [测试体系](05-SpringBoot测试体系.md) | @SpringBootTest、MockMvc、Testcontainers | 测试 |
| 06 | [安全与认证](06-SpringBoot安全与认证.md) | Security、JWT、OAuth2、方法级安全 | 安全 |
| 07 | [生产运维与可观测](07-SpringBoot生产运维与可观测.md) | Actuator、日志、优雅关闭、OTel、部署 | 运维 |
| 08 | [微服务进阶与面试题](08-SpringBoot微服务进阶与面试题.md) | 配置中心、服务发现、事件、面试十问 | 进阶 |

---

> 🎯 **本体系学习建议**：Spring Boot 面试主线——**自动配置原理**（01，第一考点）、**配置体系**（02）、**Web/数据**（03-04 实战）、**测试/安全/运维**（05-07）、**微服务**（08）。先懂原理再练实战。

---

**下一篇**：[01-SpringBoot自动配置原理](01-SpringBoot自动配置原理.md)
