# 微服务 - Spring Cloud 方案

> **定位**：Spring Cloud 是 Java 后端微服务治理的标准方案——组件化、标准化，覆盖服务注册发现、负载均衡、熔断降级、配置中心、网关路由。

---

## 目录

1. [核心架构](#1-核心架构)
2. [核心组件](#2-核心组件)
3. [落地步骤](#3-落地步骤)
4. [常见问题](#4-常见问题)

---

## 1. 核心架构

```text
接入层：Gateway（统一入口、路由、限流）
  │
核心治理层：Eureka/Nacos + Ribbon + Feign + Hystrix/Sentinel + Config
  │
微服务实例层：多个 Spring Boot 应用（用户服务、订单服务...）
```

| 层级 | 职责 |
|------|------|
| **接入层** | 统一入口、路由转发、权限控制 |
| **治理层** | 注册发现、负载均衡、熔断降级、配置管理 |
| **实例层** | 业务逻辑、数据访问（Spring Boot） |

---

## 2. 核心组件

| 组件 | 作用 | 替代方案 |
|------|------|----------|
| **Eureka / Nacos** | 服务注册与发现 | Consul |
| **Ribbon** | 客户端负载均衡 | Spring Cloud LoadBalancer |
| **Feign** | 声明式服务调用 | Dubbo |
| **Hystrix / Sentinel** | 熔断降级 | Resilience4j |
| **Spring Cloud Config / Nacos** | 配置中心 | Apollo |
| **Spring Cloud Gateway** | API 网关 | Zuul(停更) |

### 调用链路

```text
客户端 → Gateway → Nacos(发现) → Feign(Ribbon负载均衡) → 目标服务
                                    ↓
                              Hystrix/Sentinel 熔断兜底
```

---

## 3. 落地步骤

| 步骤 | 操作 |
|:----:|------|
| 1 | 搭建父工程（统一版本管理） |
| 2 | 搭建 Eureka/Nacos 注册中心（集群部署） |
| 3 | 搭建 Config/Nacos 配置中心 |
| 4 | 搭建 Gateway 网关 |
| 5 | 开发微服务实例（集成 Eureka Client + Feign + Hystrix） |
| 6 | 容器化部署（Docker + K8s）+ 监控（SkyWalking + Prometheus） |

---

## 4. 常见问题

| 问题 | 方案 |
|------|------|
| 版本冲突 | Boot/Cloud 版本严格匹配，父工程统一管理 |
| 调用延迟高 | Feign 连接池 + Ribbon 权重算法 + Redis 缓存 |
| 熔断不合理 | 核心服务 50% 阈值、降级返回兜底数据 |
| 配置动态更新不生效 | `@RefreshScope` + `spring-cloud-starter-bus-amqp` |
