# 00 - Spring Cloud 微服务知识体系总览

> 🎯 Spring Cloud 是 Java 微服务的事实标准 — 从注册发现到配置管理、从远程调用到熔断限流、从网关路由到链路追踪，一站式微服务全家桶

---

## 目录

1. [知识全景](#1-知识全景)
2. [文件导航](#2-文件导航)
3. [精通级学习路线](#3-精通级学习路线)

---

## 1. 知识全景

```
Spring Cloud 微服务精通体系（11个文件）
│
├── 🏗️ 基础入门（01-03）
│   ├── 01-SpringCloud概述与组件生态.md   # 版本选型/Netflix vs Alibaba/技术栈全景图
│   ├── 02-注册中心-Nacos服务发现.md      # Nacos安装/服务注册/发现/健康检查/namespace
│   └── 03-配置中心-Nacos配置管理.md      # 动态刷新/多环境/共享配置/灰度发布
│
├── 🔧 远程调用（04-05）
│   ├── 04-远程调用-OpenFeign与负载均衡.md  # Feign声明式调用/LoadBalancer/超时重试
│   └── 05-服务容错-Sentinel熔断降级.md     # 流控/熔断/热点/系统规则/持久化
│
├── 🌐 网关与链路（06-07）
│   ├── 06-API网关-SpringCloudGateway.md   # 路由/Predicate/Filter/限流/跨域
│   └── 07-链路追踪-Sleuth与Zipkin.md       # TraceId传递/采样/集成SkyWalking
│
├── ⚙️ 进阶主题（08-10）
│   ├── 08-分布式事务-Seata集成.md           # AT/TCC模式实战/TC部署/全局锁
│   ├── 09-服务治理与最佳实践.md             # 版本管理/网关聚合/异常处理/生产Checklist
│   └── 10-SpringCloudAlibaba全栈实战.md     # Docker-Compose全栈/微服务完整Demo
│
└── 📌 00-SpringCloud微服务知识体系总览.md      # ← 本文件
```

---

## 2. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | SpringCloud微服务知识体系总览 | 全景导航 + 学习路线 | — |
| 01 | SpringCloud概述与组件生态 | Spring Boot/Cloud版本对应、Netflix vs Alibaba、技术栈全景图 | ⭐⭐ |
| 02 | 注册中心-Nacos服务发现 | 安装部署、服务注册发现、namespace隔离、健康检查 | ⭐⭐ |
| 03 | 配置中心-Nacos配置管理 | 动态刷新、多环境(dev/test/prod)、共享配置、灰度发布 | ⭐⭐⭐ |
| 04 | 远程调用-OpenFeign与负载均衡 | Feign 声明式调用、LoadBalancer、超时重试、拦截器 | ⭐⭐ |
| 05 | 服务容错-Sentinel熔断降级 | 流控规则/QPS vs 线程/熔断降级/热点/系统规则/持久化 | ⭐⭐⭐ |
| 06 | API网关-SpringCloudGateway | 路由断言/过滤器/限流/跨域/聚合文档 | ⭐⭐⭐ |
| 07 | 链路追踪-Sleuth与Zipkin | TraceId/SpanId 传递、Zipkin 部署、集成 SkyWalking | ⭐⭐ |
| 08 | 分布式事务-Seata集成 | AT 模式/TCC 模式/TC 部署/全局事务锁 | ⭐⭐⭐⭐ |
| 09 | 服务治理与最佳实践 | 版本管理/异常处理/优雅上下线/生产 Checklist | ⭐⭐⭐ |
| 10 | SpringCloudAlibaba全栈实战 | Docker-Compose 全栈部署、完整微服务 Demo | ⭐⭐ |

---

## 3. 精通级学习路线

### 🟢 L1：搭一个微服务（1天）

```
01-概述 → 02-Nacos注册中心 → 04-OpenFeign调用 → 06-网关
产出：能搭建注册中心、服务间远程调用、通过网关访问
```

### 🔵 L2：生产级治理（1天）

```
03-配置中心 → 05-Sentinel容错 → 07-链路追踪
产出：配置动态刷新、熔断降级限流、全链路追踪
```

### 🟣 L3：分布式事务（半天）

```
08-Seata分布式事务
产出：掌握 AT 模式、能处理跨服务数据一致性
```

### 🟡 L4：全栈实战（半天）

```
09-服务治理最佳实践 → 10-全栈实战
产出：独立构建完整的 Spring Cloud 微服务项目
```

---

> 🎯 **Spring Cloud 是 Java 后端的核心框架之一** — 面试必问、工作必用。Spring Cloud Alibaba 是当前国内生产环境的主流选择。
