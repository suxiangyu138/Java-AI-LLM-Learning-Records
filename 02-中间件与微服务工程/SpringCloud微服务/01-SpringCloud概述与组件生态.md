# 01 - Spring Cloud 概述与组件生态

> 🎯 Spring Cloud 不是单个框架，而是一套微服务解决方案的集合 — 理解 Netflix 老一代到 Alibaba 新一代的演进，选对版本和技术栈是项目成功的第一步

---

## 目录

1. [Spring Cloud 是什么](#1-spring-cloud-是什么)
2. [版本演进与选型](#2-版本演进与选型)
3. [Netflix 技术栈 vs Alibaba 技术栈](#3-netflix-技术栈-vs-alibaba-技术栈)
4. [Spring Boot / Cloud / Alibaba 版本对应](#4-spring-boot--cloud--alibaba-版本对应)

---

## 1. Spring Cloud 是什么

> Spring Cloud 是**基于 Spring Boot 的微服务开发工具集**，提供配置管理、服务发现、负载均衡、熔断器、网关等一系列分布式系统解决方案。

### 1.1 核心组件全景图

```
外部请求 → [Gateway 网关] → [LoadBalancer 负载均衡]
                                ↓
              [Nacos 注册中心] ← Service-A → Service-B
              [Nacos 配置中心]     │              │
              [Sentinel 熔断限流]   │              │
              [Seata 分布式事务]    └── Feign 远程调用
              [Sleuth+Zipkin 链路追踪]
```

| 分类 | 组件 | Netflix | Alibaba 替代 | 说明 |
|------|------|:---:|:---:|------|
| 注册中心 | Nacos | Eureka | ✅ | Nacos 注册+配置一体 |
| 配置中心 | Nacos Config | Config+Bus | ✅ | 动态刷新，无需 Bus |
| 远程调用 | OpenFeign | Feign | — | 声明式 HTTP 客户端 |
| 负载均衡 | LoadBalancer | Ribbon | — | Spring 官方替代 |
| 熔断降级 | Sentinel | Hystrix | ✅ | 可视化控制台 |
| 网关 | Gateway | Zuul | — | 基于 WebFlux |
| 链路追踪 | Micrometer Tracing | Sleuth | — | 集成 Zipkin/SkyWalking |
| 分布式事务 | Seata | — | ✅ | AT/TCC/Saga 模式 |

---

## 2. 版本演进与选型

### 2.1 Spring Cloud 版本命名

```text
Spring Cloud 版本采用伦敦地铁站命名，字母顺序递进：

Hoxton (H) → 2020.0 (Ilford) → 2021.0 (Jubilee) → 2022.0 (Kilburn)
→ 2023.0 (Leyton) → 2024.0 (?)

2020.0 起改用日历版本号：YYYY.MINOR.MICRO
```

### 2.2 关键版本变化

| 版本 | Spring Boot | 重要变化 |
|------|:---:|------|
| Hoxton.SR12 | 2.3 / 2.4 | Netflix 全系进入维护 |
| 2020.0.x | 2.4 / 2.5 | 移除 Ribbon/Hystrix，引入 LoadBalancer |
| 2021.0.x | 2.6 / 2.7 | Spring Cloud Gateway 3.x, Sleuth 3.x |
| **2022.0.x** | **3.0** | ⭐ 支持 Spring Boot 3 + JDK 17 |
| 2023.0.x | 3.1 / 3.2 | 支持 GraalVM Native、Virtual Threads |

---

## 3. Netflix 技术栈 vs Alibaba 技术栈

| 组件 | Netflix 方案 | Alibaba 方案 | 推荐 |
|------|-------------|-------------|:---:|
| 注册中心 | Eureka（停维） | **Nacos** | Alibaba |
| 配置中心 | Config + Bus + MQ | **Nacos Config** | Alibaba |
| 熔断降级 | Hystrix（停维） | **Sentinel** | Alibaba |
| 分布式事务 | — | **Seata** | Alibaba |
| 远程调用 | **OpenFeign** | Feign + Dubbo | 并存 |
| 网关 | **Gateway** | Gateway + Dubbo Proxy | 并存 |
| 负载均衡 | LoadBalancer | **Dubbo LB** | 并存 |

```yaml
# ⭐ 当前生产推荐
推荐组合：Spring Boot 3.2 + Spring Cloud 2023.0 + Spring Cloud Alibaba 2023.0

核心组件：
  - Nacos 2.x: 注册中心 + 配置中心
  - OpenFeign: 远程调用
  - Spring Cloud LoadBalancer: 负载均衡
  - Sentinel: 熔断降级限流
  - Spring Cloud Gateway: API 网关
  - Micrometer Tracing: 链路追踪
```

---

## 4. Spring Boot / Cloud / Alibaba 版本对应

| Spring Boot | Spring Cloud | Spring Cloud Alibaba | Nacos | Sentinel |
|:---:|------|------|------|------|
| 3.2.x | 2023.0.x | 2023.0.x | 2.3.x | 1.8.6 |
| 3.0.x ~ 3.1.x | 2022.0.x | 2022.0.x | 2.2.x | 1.8.6 |
| 2.7.x | 2021.0.x | 2021.0.x | 2.1.x | 1.8.5 |
| 2.6.x | 2021.0.x | 2021.0.x | 2.0.x | 1.8.3 |

### Maven BOM 管理

```xml
<dependencyManagement>
    <dependencies>
        <!-- Spring Cloud BOM -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2023.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <!-- Spring Cloud Alibaba BOM -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-alibaba-dependencies</artifactId>
            <version>2023.0.1.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

> 🎯 **选型结论**：Spring Boot 3.2 + Spring Cloud 2023.0 + Spring Cloud Alibaba 2023.0 + Nacos 2.3。新项目一律选 Alibaba 方案替代已停维的 Netflix 组件。
