# 00 Nacos 知识体系总览

> 组件卡片：Nacos 是什么、版本现状、能做什么、与深度体系如何衔接——**注册中心 + 配置中心双职责**，2026 已从"云原生平台"转型"AI Agent 平台"。对照体系见 [ZooKeeper 知识体系](../../../../../09-Web开发全流程/ZooKeeper/00-ZooKeeper知识体系总览.md)

---

## 📚 目录

1. [组件一句话定位](#1-组件一句话定位)
2. [版本现状（2026-08）](#2-版本现状2026-08)
3. [能力地图](#3-能力地图)
4. [与对照体系的映射](#4-与对照体系的映射)
5. [快速上手 3 步](#5-快速上手-3-步)
6. [速查导航](#6-速查导航)
7. [学习路线推荐](#7-学习路线推荐)
8. [核心概念速查](#8-核心概念速查)

---

## 1. 组件一句话定位

**Nacos（Dynamic Naming and Configuration Service）是阿里巴巴开源的注册中心 + 配置中心一体组件**——服务注册发现（AP 模式为主）与动态配置管理（推送/长轮询）两个能力合二为一，是 Spring Cloud Alibaba 微服务体系的地基；**2026 年 3.x 全面转向 AI Agent 平台**（MCP/Agent/Skill/Prompt 注册 + 分布式锁）。

```text
核心心智模型：
  微服务实例 ──注册──► Nacos Server（AP: Distro / CP: JRaft）
        │                    │
        ├── 服务发现 ◄── 客户端订阅（gRPC 长连接 + 2.x 兼容长轮询）
        │                    │
        └── 配置拉取 ◄── 客户端长轮询（30s 服务端长轮询，变更即时推送）
                            │
                    控制台（管理/鉴权/灰度/回滚） + MySQL 持久化
```

| 维度 | 说明 |
|------|------|
| 所属 | Spring Cloud Alibaba 生态（阿里开源） |
| 版本线 | 3.2.3（2026-07 稳定线）；3.3.0-BETA（2026-08 特性线） |
| 配套 | Spring Cloud Alibaba 2025.0.x（内置 nacos-client 3.0.3，**必须配服务端 3.x**） |
| Java 要求 | Server: 17 / Client: 8+ |
| 双职责 | 注册中心（AP 默认 + CP 可选）+ 配置中心（动态推送） |
| 定位 | 微服务地基 + 2026 AI Agent 注册中心（MCP/Agent/Skill/Prompt） |

### 1.1 Nacos 解决什么问题（双职责）

**① 注册中心**：微服务实例的网络位置（IP:port）随时变化（扩容/缩容/故障），服务消费者不能硬编码——Nacos 提供"注册 + 心跳 + 订阅通知"：实例上线注册、异常下线、消费者实时感知（[03 篇](03-注册中心实战速查.md)）。

**② 配置中心**：配置散落各服务本地文件，改配置要发版重启——Nacos 提供"集中管理 + 动态推送"：配置改在控制台，客户端秒级生效（[04 篇](04-配置中心实战速查.md)）。

| 维度 | 没有 Nacos | 有 Nacos |
|------|-----------|---------|
| 服务地址 | 硬编码/手工维护 | 注册发现自动感知 |
| 实例变化 | 发版/重启/人工改 | 心跳自动上下线 |
| 配置管理 | 本地文件 + 发版 | 控制台修改 + 秒级推送 |
| 多环境 | 每环境一套配置目录 | namespace/group 隔离 |

> 🎯 判断标准一句话：**"服务数量 > 3 个且经常变，或配置要动态调整？"**——是，上 Nacos；单体单服务配置写本地文件就够了。

### 1.2 适用与不适用场景

| 场景 | 适合？ | 说明 |
|------|:---:|------|
| 微服务注册发现（SCA 生态） | ✅ | 主战场（AP 模式，可用性优先） |
| 动态配置管理 | ✅ | 长轮询推送，秒级生效 |
| AI Agent 注册（2026 新） | ✅ | MCP/Agent/Skill/Prompt Registry（[08 篇](08-AI时代新能力速查.md)） |
| 分布式锁（3.3 新） | ⚠️ 新能力 | 实验性，生产仍推荐 Redis/Redisson（[Redis 锁](../../../Spring Data 系列【数据访问层】/Spring Data Redis/06-分布式锁与Lua脚本速查.md)） |
| 强一致数据存储 | ❌ | 不是数据库（CP 模式也非强一致存储） |
| 大数据量配置（> 10MB） | ❌ | 配置中心定位轻量 KV，大文件用对象存储 |

> ⚠️ **最大认知误区**：把 Nacos 当"万能协调器"——它是**注册 + 配置**两个领域的专家；分布式锁/选主/元数据存储各有更专业的工具（Redisson/etcd/ZK）——**别让一个组件干所有活**。

### 1.3 与主流注册中心对照

| 维度 | Nacos 3.x | Eureka | ZooKeeper | Consul |
|------|-----------|--------|-----------|--------|
| 职责 | 注册 + 配置 | 仅注册 | 注册/协调（原生强一致） | 注册 + 配置 + KV |
| 一致性 | **AP 默认（Distro）+ CP 可选（JRaft）** | AP | **CP**（Zab 协议） | CP（Raft） |
| 健康检查 | 心跳（客户端）+ 可扩展 | 心跳 | Session | HTTP/gRPC |
| 配置中心 | ✅ 内建 | ❌ | 可用（znode 当配置） | ✅ 内建 |
| 维护状态 | **活跃（2026 AI 转型）** | 已停更（Netflix） | 活跃 | 活跃 |
| 生态 | SCA 标配 | 老 Spring Cloud | Dubbo 传统 | HashiCorp 系 |

> 🎯 面试必答：**"Eureka 和 Nacos 注册中心什么区别？"**——Eureka 已停更、只有注册能力；Nacos **注册 + 配置一体**、支持 **AP/CP 双模式**（默认 AP 保证可用性，强一致场景切 CP）、健康检查与保护阈值更丰富——**Nacos 是 Eureka 的现代替代**（[09 篇](09-集成地图与常见问题.md) 有迁移清单）。

## 2. 版本现状（2026-08）

| 版本线 | 状态 | 关键点 |
|--------|------|--------|
| **3.3.0-BETA（2026-08-06）** | 特性线 | Agent 注册管理、Agentic Resource Discovery（混合检索）、分布式锁增强、**Boot 4/Jackson 3 兼容** |
| **3.2.3（2026-07-14）** | **稳定线** | 3.2 系列体验修复 |
| 3.2.0（2026-03-27） | 稳定 | **AI Registry 三件套**（MCP/Agent/Skill/Prompt）、SPI 插件架构、移除 UDP PUSH、OIDC SSO |
| 3.1.x | 稳定 | AI Registry 扩展 |
| 3.0.x（2024-10 起） | 稳定 | **CP 模式（JRaft）**、控制台默认鉴权、namespace 默认 public、Boot 3.4/JDK 17 |
| 2.x | 存量维护 | gRPC 长连接（2.0 起）、2.5.1 为 2.x 末线 |

> ⚠️ **版本策略（2026 起）**：**新项目直接 Nacos 3.2.x + Spring Cloud Alibaba 2025.0.x**；存量 2.x 项目规划升级（[06 篇](06-高可用与生产实践速查.md) 迁移清单）——**关键约束：nacos-client 与服务端不要跨大版本**（SCA 2025.0.0.0 内置 client 3.0.3 配 2.x 服务端配置中心不可用，见 [01 篇](01-模块清单.md) 版本矩阵）。

### 2.1 3.x 线关键变化（2.x → 3.0 迁移要点）

| 变化 | 说明 |
|------|------|
| CP 模式（JRaft） | 3.0 引入：强一致场景可选（注册数据 Raft 复制），默认仍 AP |
| **控制台/内部 API 默认鉴权** | 3.0 起默认开启（不再裸奔），生产必须配账号 |
| namespace 默认 public | 1.x 的默认 namespace 统一为 public |
| 移除临时/持久节点区分 | 3.0 简化节点模型（统一持久化） |
| gRPC 全面化 | 客户端主链路 gRPC；2.x 长轮询为兼容保留 |
| **AI Registry** | 3.0 起 MCP/Agent 注册；3.2 完成 Skill/Prompt 三件套 |
| Spring Boot 4/Jackson 3 | 3.3.0-BETA 兼容（服务端自身） |

### 2.2 关键演进时间线

| 版本 | 里程碑 |
|------|--------|
| 0.x（2018） | 阿里内部 VIP 架构开源（注册+配置一体） |
| 1.x | 配置中心成熟、AP（Distro）注册 |
| 2.0（2021-06） | **gRPC 长连接**（替代 1.x 心跳+推送）、性能与容量大幅提升 |
| 2.2/2.3 | 鉴权完善、控制台增强 |
| 3.0（2024-10） | **CP 模式（JRaft）**、默认鉴权、Boot 3.4/JDK 17 |
| 3.1/3.2（2025-2026） | **AI Registry 三件套**、SPI 插件架构、OIDC SSO、移除 UDP PUSH |
| 3.3（2026-08） | Agent 注册管理、混合检索、分布式锁增强、Boot 4 兼容 |

## 3. 能力地图

| 能力域 | 能力点 | 对应 API/机制 |
|--------|--------|-------------|
| 服务注册 | 实例注册/注销/心跳 | `spring-cloud-starter-alibaba-nacos-discovery` |
| 服务发现 | 订阅/拉取/变更通知 | gRPC 长连接订阅 |
| 命名空间/分组 | 多环境/多业务隔离 | namespace（ID）/ group |
| 负载均衡策略 | 权重、保护阈值 | Nacos 控制台 + Spring Cloud LoadBalancer |
| 配置管理 | 增删改查/历史/回滚 | 控制台 + `nacos-config` |
| 配置推送 | 变更秒级生效 | 客户端长轮询 + gRPC |
| 动态刷新 | @Value/@ConfigurationProperties | `@RefreshScope` |
| 鉴权 | 控制台/客户端认证 | 3.0 默认开启（username/password） |
| 权限 | RBAC/命名空间隔离 | 3.x 控制台角色权限 |
| 高可用 | 集群/选举/持久化 | JRaft/Distro + MySQL |
| AI 注册（3.x 新） | MCP/Agent/Skill/Prompt | AI Registry（[08 篇](08-AI时代新能力速查.md)） |
| 分布式锁（3.3 新） | 可重入锁/续期 | 3.3.0-BETA 实验性 |

### 3.1 能力边界：Nacos 不做什么

| 能力边界 | 谁来做 | 说明 |
|---------|--------|------|
| 服务调用/负载均衡执行 | OpenFeign + Spring Cloud LoadBalancer | Nacos 只给地址列表，不负责调用 |
| 网关路由 | Spring Cloud Gateway | 网关从 Nacos 发现下游（[09 篇](09-集成地图与常见问题.md) 联动） |
| 分布式事务 | Seata | SCA 兄弟组件（Seata 目录） |
| 流量治理 | Sentinel | SCA 兄弟组件（Sentinel 目录） |
| 强一致 KV 存储 | etcd/ZK | Nacos 的 CP 模式 ≠ 强一致存储 |

> ⚠️ **常见归因错误**：服务调用失败怪"Nacos 没发现"——排查路径："注册上了吗（控制台服务列表）→ 客户端拿到实例了吗（日志）→ 调用失败是路由/负载/超时哪一环"——Nacos 只保证"地址对"，不保证"调用成"。

## 4. 与对照体系的映射

| 速查文档 | 对照/深度体系 |
|---------|-------------|
| 03-注册中心实战 | [ZooKeeper 知识体系](../../../../../09-Web开发全流程/ZooKeeper/00-ZooKeeper知识体系总览.md)（CP 对照）、[Dubbo（09-Web开发全流程）](../../../../../09-Web开发全流程/Dubbo/) |
| 05-核心原理（一致性） | [ZooKeeper-04 Session 与 Watcher](../../../../../09-Web开发全流程/ZooKeeper/04-Session与Watcher机制.md)（事件通知对照） |
| 06-高可用 | [Redis-05 高可用架构](../../../../../02-非关系型数据库/Redis/05-Redis高可用架构.md)（集群/选举对照思维） |
| 09-集成地图 | Spring Cloud Gateway/LoadBalancer/OpenFeign（同级目录） |

> 💡 分工约定：**本系列回答"Nacos 怎么用、怎么配、怎么排错"**；一致性协议/选举原理的通用知识（Raft 类协议）与 ZooKeeper 对照理解更透（[05 篇](05-核心原理：一致性协议速查.md) 有对照表）。

## 5. 快速上手 3 步

**① 启动 Nacos 服务端**（单机，开发够用）：

```bash
# 方式 A：Docker（推荐开发）
docker run -d --name nacos -p 8848:8848 -p 9848:9848 \
  -e MODE=standalone -e NACOS_AUTH_ENABLE=true nacos/nacos-server:v3.2.3

# 方式 B：下载解压启动（生产用集群模式，见 02 篇）
# startup.sh -m standalone

# 控制台：http://localhost:8848/nacos  （默认账号 nacos/nacos，3.x 默认鉴权开）
```

**② 服务注册（provider）**：

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

```yaml
spring:
  application:
    name: user-service            # ★ 服务名 = 注册名
  cloud:
    nacos:
      server-addr: localhost:8848
      discovery:
        namespace: public         # 命名空间（生产按环境分）
        group: DEFAULT_GROUP
```

**③ 服务发现（consumer）**：`@LoadBalanced RestTemplate` / OpenFeign + `@EnableDiscoveryClient`——直接按服务名调用：

```java
// OpenFeign（SCA 标配）
@FeignClient("user-service")                     // 服务名，实例地址由 Nacos 给
public interface UserClient {
    @GetMapping("/users/{id}") Mono<User> getUser(@PathVariable Long id);
}
```

### 5.1 快速上手补充：配置中心三件套

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
```

```yaml
spring:
  cloud:
    nacos:
      config:
        server-addr: localhost:8848
        file-extension: yaml       # dataId: user-service.yaml（默认）
```

```java
@RefreshScope                           // ★ 动态刷新：配置变更自动更新
@ConfigurationProperties(prefix = "order")
public class OrderProperties { private int timeout; }
```

> 🎯 跑通即及格：**① 控制台服务列表看到 user-service；② 改配置控制台点发布，客户端日志出现"refresh"且 @RefreshScope 字段更新**——两件事都通，注册 + 配置双职责打通（[03/04 篇](04-配置中心实战速查.md) 深入）。

### 5.2 三步验证与排障起点

| 步骤 | 做法 | 预期 |
|------|------|------|
| ① 服务注册 | 启动 provider → 控制台服务列表 | 实例出现（含 IP:port） |
| ② 服务发现 | consumer 调 provider（服务名） | 调用成功 |
| ③ 配置刷新 | 控制台改配置 → 观察客户端 | @RefreshScope 字段更新（日志 `refresh`） |

> 💡 排障起点：**先看控制台（注册没注册、配置在不在），再看客户端日志**（`Registering service with Nacos` / `init nacos ok`）；连接失败查 8848（HTTP）+ **9848（gRPC，2.x 起必开）**（[09 篇](09-集成地图与常见问题.md) 排错表）。

## 6. 速查导航

| 文档 | 内容 |
|------|------|
| [01-模块清单](01-模块清单.md) | artifact 坐标、包结构、依赖边界、**版本兼容矩阵（client↔server）** |
| [02-快速开始与部署速查](02-快速开始与部署速查.md) | 单机/集群部署、MySQL 持久化、控制台、端口清单、Docker/K8s |
| [03-注册中心实战速查](03-注册中心实战速查.md) | 注册发现、命名空间/分组、权重/保护阈值、健康检查、与 LoadBalancer 联动 |
| [04-配置中心实战速查](04-配置中心实战速查.md) | dataId 规则、配置优先级、@RefreshScope、多环境、扩展配置、回滚/灰度 |
| [05-核心原理：一致性协议速查](05-核心原理：一致性协议速查.md) | AP（Distro）/CP（JRaft）双模式、客户端长轮询/gRPC、集群选主、与 ZK 对照 |
| [06-高可用与生产实践速查](06-高可用与生产实践速查.md) | 集群规划（3 节点）、MySQL 主从、容灾、2.x→3.x 升级迁移、容量评估 |
| [07-安全与权限速查](07-安全与权限速查.md) | 鉴权（3.0 默认开）、RBAC、命名空间隔离、TLS、安全基线 |
| [08-AI 时代新能力速查](08-AI时代新能力速查.md) | MCP Registry、Agent/Skill/Prompt Registry、分布式锁（3.3）、Copilot、与 Spring AI 联动 |
| [09-集成地图与常见问题](09-集成地图与常见问题.md) | SCA 全家桶联动、Eureka→Nacos 迁移、2.x→3.x 迁移、高频坑排错表 |

### 6.1 阅读顺序建议

- **第一次接触**：02（部署）→ 03（注册）→ 04（配置），先跑通双职责；
- **项目实战**：02 部署 → 03 注册 → 04 配置 → 06 高可用 → 09 避坑；
- **准备面试**：05（一致性/AP vs CP）→ 03（注册机制）→ 04（配置刷新）→ 01（版本矩阵）；
- **AI 应用开发**：08 篇（MCP/Agent 注册）配合 [Spring AI 系列](../../../../../../03-AI大模型应用开发/)；
- **架构师视角**：06 高可用 → 05 协议 → 07 安全。

### 6.2 前置知识建议

| 前置主题 | 所在文档 | 与本系列的关系 |
|---------|---------|--------------|
| 微服务基础 | Spring Cloud 家族（同级目录） | OpenFeign/LoadBalancer/Gateway 是联动前提 |
| 一致性基础 | [ZooKeeper 知识体系](../../../../../09-Web开发全流程/ZooKeeper/00-ZooKeeper知识体系总览.md) | AP/CP 与选举协议对照 |
| Spring 配置机制 | Spring Cloud Config（同级目录） | 配置中心的对比视角 |
| MCP/AI 概念 | [MCP 协议与 Agent Skill](../../../../../../03-AI大模型应用开发/06-MCP协议与Agent Skill/) | 08 篇 AI Registry 的基础 |

## 7. 学习路线推荐

| 路线 | 适用 | 顺序 |
|------|------|------|
| 入门速查 | 会写微服务 | 00 总览 → 02 部署 → 03 注册 → 04 配置 |
| 项目实践 | 上生产 | 02 部署集群 → 03 → 04 → 06 高可用 → 09 避坑 |
| 面试冲刺 | 全考点 | 05 AP/CP → 03 注册机制 → 04 刷新原理 → 01 版本矩阵 |
| AI 转型 | Agent 应用 | 08 AI Registry → 05 协议（配合 Spring AI） |

## 8. 核心概念速查

| 术语 | 一句话解释 |
|------|-----------|
| 服务注册 | 实例把自己的 IP:port 上报给 Nacos（启动时） |
| 服务发现 | 消费者按服务名拿到实例列表（订阅 + 变更通知） |
| 命名空间（namespace） | 环境/租户级隔离（生产/测试/灰度），**配置的是 ID** |
| 分组（group） | 命名空间内的逻辑分组（默认 DEFAULT_GROUP） |
| dataId | 配置文件唯一标识（`服务名-环境.后缀`） |
| AP 模式 | 可用性优先（Distro 协议，默认） |
| CP 模式 | 一致性优先（JRaft，3.0 起可选） |
| gRPC 长连接 | 2.0 起客户端与服务端的主链路（端口 9848） |
| 长轮询 | 配置客户端拉取机制（30s 服务端挂起，变更即时返回） |
| @RefreshScope | 配置变更后刷新 Bean（动态更新 @Value/@ConfigurationProperties） |
| 保护阈值 | 健康实例比例低于阈值时，向不健康实例分发（防雪崩） |
| AI Registry | 3.x：MCP/Agent/Skill/Prompt 资源注册与发现 |
| MCP | Model Context Protocol：LLM 工具调用协议（08 篇） |

---

**下一模块**：[01-模块清单](01-模块清单.md)　**返回总览**：本页

**【参考来源】**：[Nacos 3.2.1 Release（官方）](https://github.com/alibaba/nacos/releases/tag/3.2.1)、[Nacos 3.3.0-BETA Release（官方）](https://github.com/alibaba/nacos/releases/tag/3.3.0-BETA)、[Nacos 3.0.0 Release（mygit）](https://mygit.top/release/214748628)、[Nacos 配置加载问题说明（SCA Issue #4098）](https://github.com/alibaba/spring-cloud-alibaba/issues/4098)、[Nacos 官方文档](https://nacos.io/docs/latest/)
