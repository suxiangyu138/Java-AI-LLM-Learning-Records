# 01 - Dubbo 概述与 RPC 原理

> 🎯 理解 RPC 的核心价值是理解 Dubbo 的前提 — 像调用本地方法一样调用远程服务。掌握调用链路、Dubbo 架构分层、与 gRPC/Feign 的定位差异

---

## 目录

1. [RPC 原理](#1-rpc-原理)
2. [Dubbo 架构分层](#2-dubbo-架构分层)
3. [调用流程详解](#3-调用流程详解)
4. [Dubbo 核心角色](#4-dubbo-核心角色)
5. [Dubbo vs gRPC vs Feign](#5-dubbo-vs-grpc-vs-feign)

---

## 1. RPC 原理

> RPC（Remote Procedure Call）：像调用本地方法一样调用远程服务，封装了网络通信的复杂度。

### 1.1 一次 RPC 调用的完整链路

```
Client（调用方）                        Server（服务方）
┌─────────────────┐                ┌─────────────────┐
│ 1. Client Stub   │ ──序列化──→   │ 5. Server Stub  │
│    (代理对象)     │              │    (反序列化)    │
│                  │              │                 │
│    → 封装方法名、  │              │    → 解析方法名、  │
│      参数、类型    │              │      参数、类型    │
└────────┬─────────┘              └────────┬────────┘
         │                                │
    2. 网络传输 ────── TCP/HTTP ──────→  4. 网络接收
         │                                │
         └──────── 3. 二进制数据包 ────────┘

关键步骤：
1. 客户端将调用信息序列化为二进制
2. 通过网络发送到服务端
3. 服务端反序列化 → 反射调用本地方法 → 序列化返回值
4. 响应原路返回
```

### 1.2 RPC 要解决的问题

| 问题 | 解决方案 |
|------|----------|
| 服务地址如何获取？ | 注册中心（ZooKeeper / Nacos） |
| 如何序列化参数？ | 序列化协议（Hessian2 / Protobuf） |
| 如何传输数据？ | 通信协议（Dubbo / HTTP/2 / gRPC） |
| 如何找到目标方法？ | 接口定义（Java Interface 共享） |
| 如何处理失败？ | 容错策略（重试 / 降级 / 熔断） |

---

## 2. Dubbo 架构分层

```text
┌──────────────────────────────────────────────┐
│  Business（业务层）— Service 接口 + 实现类     │
├──────────────────────────────────────────────┤
│  RPC 层 ─ Config（配置）→ Proxy（代理）        │
│          → Registry（注册中心）                │
├──────────────────────────────────────────────┤
│  Remoting 层 ─ Exchange（信息交换）            │
│              → Transport（网络传输）           │
│              → Serialize（序列化）             │
└──────────────────────────────────────────────┘

Dubbo 3.x 的三大注册模型：
  1. 接口级（Interface-level）— Dubbo 2.x 传统方式
  2. 应用级（Application-level）— Dubbo 3.x 推荐，与 Spring Cloud 统一
  3. 元数据中心（Metadata Center）— 存储接口定义等元数据
```

| 层 | 职责 | 核心接口 |
|------|------|----------|
| **Business** | 业务逻辑 | 用户自定义 Service |
| **RPC** | 配置解析、动态代理、注册发现 | `ProxyFactory`、`Registry` |
| **Remoting** | 网络通信、序列化、协议 | `Transporter`、`Serialization` |

---

## 3. 调用流程详解

```
Provider 启动：
  1. 创建 Service 实例
  2. 通过 Registry 注册到注册中心（写入临时节点）
  3. 启动 Netty Server 监听端口

Consumer 启动：
  1. 通过 Registry 订阅服务 → 获取 Provider 地址列表
  2. 创建 Service 的动态代理（Proxy）
  3. 本地调用代理对象 → 触发远程调用

一次远程调用：
  1. Consumer: 代理对象 → Filter 链 → 负载均衡选 Provider
  2. Consumer: 序列化请求 → Netty Client 发送
  3. Provider: Netty Server 接收 → 反序列化 → Filter 链
  4. Provider: 反射调用 Service 实现 → 序列化返回值
  5. Provider: Netty Server 返回 → Consumer 接收 → 反序列化
```

---

## 4. Dubbo 核心角色

| 角色 | 职责 | 部署 |
|------|------|------|
| **Provider** | 暴露服务，注册到注册中心 | 服务提供方 |
| **Consumer** | 调用远程服务，订阅注册中心 | 服务消费方 |
| **Registry** | 服务注册与发现，通知地址变更 | ZK / Nacos / Redis |
| **Monitor** | 统计调用次数、耗时 | Dubbo Admin |
| **Container** | 服务运行容器（Spring 容器） | Spring / Spring Boot |

---

## 5. Dubbo vs gRPC vs Feign

| 维度 | Dubbo | gRPC | Feign (Spring Cloud) |
|------|-------|------|---------------------|
| 定位 | 高性能 RPC | 跨语言 RPC | HTTP 声明式调用 |
| 协议 | Dubbo/Triple(HTTP2) | HTTP/2 | HTTP/1.1 |
| 序列化 | Hessian2/Protobuf | Protobuf | JSON |
| 性能 | ⭐⭐⭐ 极高 | ⭐⭐⭐ 高 | ⭐⭐ 一般 |
| 服务治理 | ⭐⭐⭐ 极丰富(内置) | ⭐ 需外部 | ⭐⭐ Spring Cloud 生态 |
| 跨语言 | Dubbo 3.x Triple ✅ | ✅ 原生支持 | ❌ Java 限定 |
| 生态 | 国内主流 | Google/CNCF | Netflix→Spring |
| **推荐场景** | 国内 Java 微服务 | 跨语言/多端 | Spring Cloud 项目 |

> 🎯 **选型建议**：纯 Java 微服务 → Dubbo（性能+治理最优）；跨语言 → gRPC；已有 Spring Cloud 项目 → Feign。Dubbo 3.x Triple 协议兼容 gRPC，跨语言场景也不输 gRPC。
