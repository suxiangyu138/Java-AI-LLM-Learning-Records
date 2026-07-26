# 00 - Apache Dubbo 知识体系总览

> 🎯 Dubbo 是阿里开源的高性能 RPC 框架，国内微服务通信的事实标准 — 从 RPC 原理到服务治理、从 SPI 机制到 Triple 协议，Java 后端必学必会

---

## 目录

1. [知识全景](#1-知识全景)
2. [文件导航](#2-文件导航)
3. [精通级学习路线](#3-精通级学习路线)

---

## 1. 知识全景

```
Apache Dubbo 精通体系（8个文件）
│
├── 🏗️ 基础入门（01-02）
│   ├── 01-Dubbo概述与RPC原理.md       # RPC核心/Dubbo架构/调用流程/生态定位
│   └── 02-Dubbo快速入门与配置.md       # 第一个Demo/XML→注解→Spring Boot
│
├── 🔧 核心机制（03-04）
│   ├── 03-注册中心与服务发现.md         # ZooKeeper/Nacos/应用级vs接口级
│   └── 04-协议与序列化机制.md           # Dubbo协议/Triple/HTTP2/Hessian/Protobuf
│
├── ⚙️ 服务治理（05-06）
│   ├── 05-负载均衡与容错策略.md         # 7种LB/6种Cluster/降级/限流
│   └── 06-高级特性-SPI与Filter链.md    # 自适应SPI/Filter链/泛化调用/本地存根
│
├── 🚀 进阶实战（07）
│   ├── 07-Dubbo3新特性与Spring生态.md   # Triple/应用级发现/Spring Cloud Alibaba
│   └── 08-生产最佳实践与故障排查.md        # 配置调优/线程模型/直连调试/常见故障
│
└── 📌 00-Dubbo知识体系总览.md            # ← 本文件
```

---

## 2. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | Dubbo知识体系总览 | 全景导航 + 学习路线 | — |
| 01 | Dubbo概述与RPC原理 | RPC 原理/调用链路/Dubbo 架构/与 gRPC 对比 | ⭐⭐ |
| 02 | Dubbo快速入门与配置 | Demo/XML→注解→Spring Boot/配置项/多协议 | ⭐ |
| 03 | 注册中心与服务发现 | ZK/Nacos/接口级vs应用级/元数据中心 | ⭐⭐⭐ |
| 04 | 协议与序列化机制 | Dubbo2/Hessian2/Triple(Protobuf)/HTTP2 | ⭐⭐⭐ |
| 05 | 负载均衡与容错策略 | Random/RoundRobin/LeastActive/Failover/Failfast | ⭐⭐ |
| 06 | SPI与Filter链 | 自适应SPI/AOP/Filter链/泛化调用/上下文隐式传参 | ⭐⭐⭐⭐ |
| 07 | Dubbo3新特性与Spring生态 | Triple协议/应用级发现/Spring Cloud Alibaba | ⭐⭐⭐ |
| 08 | 生产最佳实践与故障排查 | 线程模型调优/超时重试/直连调试/常见故障 | ⭐⭐⭐ |

---

## 3. 精通级学习路线

### 🟢 L1：能写一个 Dubbo Demo（半天）

```
01-RPC原理 → 02-快速入门(Demo)
产出：能编写 Provider + Consumer 并用 Dubbo 远程调用
```

### 🔵 L2：理解服务治理（1天）

```
03-注册中心 → 05-负载均衡与容错 → 04-协议与序列化
产出：能选型注册中心、配置负载策略、理解 Dubbo 协议原理
```

### 🟣 L3：掌握高级特性（半天）

```
06-SPI与Filter链 → 泛化调用 → 隐式传参
产出：能扩展 Dubbo、开发自定义 Filter、实现全链路追踪
```

### 🟡 L4：生产落地与Dubbo3升级（半天）

```
07-Dubbo3新特性 → 08-生产最佳实践
产出：能升级到 Dubbo3、配置生产级线程模型、独立排查故障
```

---

> 🎯 **Dubbo 是国内 Java 微服务的 RPC 标配** — 阿里出品、高性能、服务治理完善、Spring Cloud Alibaba 集成。国内大厂面试必问。
