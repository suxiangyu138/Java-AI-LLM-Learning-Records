# Spring Cloud Alibaba 微服务组件体系总览

> Spring Cloud Alibaba 是国内 Java 微服务的事实标准——Nacos 注册配置二合一、Sentinel 流量防卫、Seata 分布式事务、RocketMQ 消息驱动。这不是 Spring Cloud 的"替代品"，而是面向生产的高阶进化。

---

## 📚 目录

1. [组件全景图](#1-组件全景图)
2. [与 Spring Cloud Netflix 的关系](#2-与-spring-cloud-netflix-的关系)
3. [模块导航](#3-模块导航)
4. [核心概念速查](#4-核心概念速查)
5. [学习路线推荐](#5-学习路线推荐)
6. [版本兼容矩阵](#6-版本兼容矩阵)

---

## 1. 组件全景图

```
Spring Cloud Alibaba 组件体系
│
├── 🧭 服务治理 ──────────────────────────────────────┐
│   │                                                  │
│   ├── Nacos（注册中心 + 配置中心）                    │
│   │   ├── 服务注册与发现（替代 Eureka）               │
│   │   │   ├── CP 模式（Raft）vs AP 模式（Distro）    │
│   │   │   ├── 临时实例 vs 持久化实例                  │
│   │   │   └── 健康检查（HTTP/TCP/MySQL）             │
│   │   │                                              │
│   │   ├── 配置中心（替代 Spring Cloud Config）        │
│   │   │   ├── 动态刷新（@RefreshScope）              │
│   │   │   ├── 灰度发布（Beta/IP 灰度）               │
│   │   │   ├── 配置加密（KMS/自定义）                 │
│   │   │   └── namespace / group / dataId 三层模型    │
│   │   │                                              │
│   │   └── 集群架构                                   │
│   │       ├── Distro 协议（AP → 临时实例同步）       │
│   │       ├── Raft 协议（CP → 持久化实例/配置同步）   │
│   │       └── 寻址机制（地址服务器/文件/独立）       │
│   │                                                  │
│   └── Dubbo（RPC 框架，可选集成）                     │
│       └── Dubbo + Nacos 注册中心                     │
│                                                      │
├── 🛡️ 服务容错 ──────────────────────────────────────┤
│   │                                                  │
│   └── Sentinel（替代 Hystrix）                        │
│       ├── 核心架构：Slot Chain 责任链                 │
│       ├── 流控规则（QPS / 并发线程 / 匀速器）         │
│       ├── 熔断降级（慢调用 / 异常比例 / 异常数）      │
│       ├── 热点规则 / 系统规则 / 授权规则              │
│       ├── 规则持久化（Nacos / Apollo / 本地文件）     │
│       └── 集群流控（Token Server / 独立模式）        │
│                                                      │
├── 🔄 分布式事务 ────────────────────────────────────┤
│   │                                                  │
│   └── Seata                                          │
│       ├── AT 模式：自动挡（Undo Log 回滚）            │
│       ├── TCC 模式：手动挡（Try-Confirm-Cancel）      │
│       ├── Saga 模式：长事务补偿                       │
│       ├── XA 模式：强一致性                          │
│       ├── TC Server 高可用（DB/Redis/RAFT 存储）     │
│       └── 全局锁与隔离级别                            │
│                                                      │
├── 📨 消息驱动 ──────────────────────────────────────┤
│   │                                                  │
│   └── Spring Cloud Stream + RocketMQ                 │
│       ├── RocketMQ Binder                            │
│       ├── 事务消息 + 微服务事件总线                   │
│       └── 消息轨迹与死信队列                          │
│                                                      │
└── 🔌 生态扩展 ──────────────────────────────────────┤
    ├── Spring Cloud Gateway + Sentinel 网关流控       │
    ├── SkyWalking 链路追踪 + Nacos 整合                │
    ├── Sidecar：非 Java 服务接入 Nacos                 │
    └── 灰度路由与流量染色                              │
```

---

## 2. 与 Spring Cloud Netflix 的关系

```text
Netflix 组件状态（2024）：
  Eureka    → 停维（进入维护模式）
  Hystrix   → 停维（推荐用 Sentinel/Resilience4j）
  Ribbon    → 停维（推荐 Spring Cloud LoadBalancer）
  Zuul      → 停维（推荐 Spring Cloud Gateway）
  Archaius  → 停维

Alibaba 组件状态（2024）：
  Nacos     → ✅ 活跃维护，生产验证
  Sentinel  → ✅ 活跃维护
  Seata     → ✅ 活跃维护
  RocketMQ  → ✅ Apache 顶级项目
```

| 功能 | Netflix（已停维） | Alibaba（活跃） | Spring 官方替代 |
|------|:---:|:---:|:---:|
| 注册中心 | Eureka | **Nacos** | — |
| 配置中心 | Archaius + Config | **Nacos Config** | Config Server |
| 熔断降级 | Hystrix | **Sentinel** | Resilience4j |
| 负载均衡 | Ribbon | — | **Spring Cloud LoadBalancer** |
| 网关 | Zuul | — | **Spring Cloud Gateway** |
| 分布式事务 | — | **Seata** | — |
| 消息驱动 | — | **RocketMQ** | Kafka / RabbitMQ |

> 💡 Spring Cloud Alibaba 不是要"替代 Netflix 全家桶"，而是填补了 Netflix 没有的空白（配置中心、分布式事务、消息），同时对已停维组件（Eureka、Hystrix）提供了更高性能的替代。

---

## 3. 模块导航

| 序号 | 模块 | 核心内容 | 难度 |
|:---:|------|---------|:---:|
| 01 | [Nacos 内核架构与一致性协议](./01-Nacos内核架构与一致性协议.md) | AP/CP 双模式、Distro 协议、Raft 协议、寻址机制、数据模型 | ⭐⭐⭐⭐ |
| 02 | [Nacos 配置中心高阶特性](./02-Nacos配置中心高阶特性.md) | 灰度发布、配置加密、SPI 插件、长轮询机制、namespace 最佳实践 | ⭐⭐⭐ |
| 03 | [Sentinel 核心原理与扩展机制](./03-Sentinel核心原理与扩展机制.md) | Slot Chain 责任链、流控算法内核、SPI 扩展点、与 Resilience4j 对比 | ⭐⭐⭐⭐ |
| 04 | [Sentinel 生产级规则治理](./04-Sentinel生产级规则治理.md) | 规则持久化到 Nacos、集群流控架构、Gateway 网关流控、监控埋点 | ⭐⭐⭐ |
| 05 | [Seata 分布式事务内核](./05-Seata分布式事务内核.md) | AT 模式 Undo Log 机制、全局锁原理、TCC/Saga 深度解析、隔离级别 | ⭐⭐⭐⭐⭐ |
| 06 | [生态整合与生产最佳实践](./06-生态整合与生产最佳实践.md) | RocketMQ Stream、Dubbo+Nacos、Sidecar 异构接入、版本升级、生产 Checklist | ⭐⭐⭐ |

---

## 4. 核心概念速查

| 概念 | 所属组件 | 说明 |
|------|:---:|------|
| **CP / AP 模式** | Nacos | 一致性 vs 可用性的选择；持久化实例用 CP（Raft），临时实例用 AP（Distro） |
| **Distro 协议** | Nacos | 阿里自研的 AP 协议，用于临时实例数据同步 |
| **namespace** | Nacos | 租户级隔离，生产标配 `dev / test / prod` 三环境 |
| **灰度发布** | Nacos Config | 配置先在部分 IP 生效，验证后全量发布 |
| **Slot Chain** | Sentinel | 责任链模式，7 个核心 Slot 串联处理：NodeSelector → ClusterBuilder → Statistic → Flow → Degrade → System → Authority |
| **令牌桶 / 漏桶** | Sentinel | 流控算法；Sentinel 的 WarmUp 用令牌桶，排队等待用漏桶 |
| **集群流控** | Sentinel | Token Server 统一分配令牌，解决单机流控不准确的分布式问题 |
| **Undo Log** | Seata AT | 事务提交前自动记录数据的"反向 SQL"，回滚时执行 Undo Log 恢复原值 |
| **全局锁** | Seata AT | 全局事务期间防止其他事务修改同一行数据，写隔离 |
| **TCC** | Seata | Try（预留资源）→ Confirm（提交）→ Cancel（释放），业务层实现 |
| **RocketMQ Binder** | Stream | 将 RocketMQ 适配为 Spring Cloud Stream 的消息中间件 |

---

## 5. 学习路线推荐

### 路线 A：快速上手（2-3 天）

```
01-Nacos内核（前半：服务注册部分）→ 02-Nacos配置中心 → 03-Sentinel原理（前半：规则部分）→ 06-生态整合（生产Checklist）
```

适合需要快速将 Alibaba 组件部署上线的开发者。

### 路线 B：内核深入（5-7 天，全部模块）

```
01 → 02 → 03 → 04 → 05 → 06
```

适合需要理解组件内核原理、排查深层次问题、做二次开发的工程师。

### 路线 C：架构师视角（3-4 天）

```
01（CP/AP 选型）→ 05（事务模式选型）→ 06（生态整合 + 版本升级）
```

适合技术选型和架构决策的架构师。

---

## 6. 版本兼容矩阵

### Spring Cloud Alibaba ↔ Spring Cloud ↔ Spring Boot

| SCA 版本 | Spring Cloud | Spring Boot | Nacos | Sentinel | Seata |
|------|------|------|:---:|:---:|:---:|
| **2023.0.1** | 2023.0.x | 3.2.x | 2.3.x | 1.8.6 | 1.8.x |
| **2022.0.0** | 2022.0.x | 3.0.x - 3.1.x | 2.2.x | 1.8.6 | 1.7.x |
| **2021.0.5** | 2021.0.x | 2.6.x - 2.7.x | 2.1.x | 1.8.5 | 1.5.x |
| **2.2.10** | Hoxton.SR12 | 2.3.x - 2.4.x | 2.0.x | 1.8.3 | 1.4.x |

> ⚠️ **选版原则**：生产环境优先选 SCA 2023.0.x + Spring Boot 3.2.x（最新稳定线）。如果还在 Java 8/11，选 SCA 2021.0.x + Spring Boot 2.7.x。

### 组件版本依赖链

```text
Spring Boot 3.2.x
  └── Spring Cloud 2023.0.x
        └── Spring Cloud Alibaba 2023.0.1.x
              ├── spring-cloud-starter-alibaba-nacos-discovery  (Nacos 2.3.x client)
              ├── spring-cloud-starter-alibaba-nacos-config
              ├── spring-cloud-starter-alibaba-sentinel         (Sentinel 1.8.6)
              ├── spring-cloud-starter-alibaba-seata            (Seata 1.8.x)
              └── spring-cloud-starter-stream-rocketmq          (RocketMQ 5.x)
```

---

> 🎯 **核心要点**：Spring Cloud Alibaba 不是 Spring Cloud Netflix 的"换皮"，而是补齐了 Netflix 缺失的配置中心、分布式事务、消息驱动三大核心能力。掌握 Nacos 的 AP/CP 选择和 Seata 的事务模式选型，是架构师的关键决策点。

---

**下一模块**：[01 - Nacos 内核架构与一致性协议](./01-Nacos内核架构与一致性协议.md)
