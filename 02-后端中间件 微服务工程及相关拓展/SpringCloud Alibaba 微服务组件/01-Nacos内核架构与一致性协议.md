# 01 - Nacos 内核架构与一致性协议

> Nacos = 注册中心 + 配置中心。但真正让它区别于 Eureka 的，是同时支持 CP（Raft）和 AP（Distro）两种一致性协议——一个组件满足不同场景的一致性需求。

---

## 📚 目录

1. [Nacos 整体架构](#1-nacos-整体架构)
2. [AP 模式：Distro 协议深度解析](#2-ap-模式distro-协议深度解析)
3. [CP 模式：Raft 协议在 Nacos 中的实现](#3-cp-模式raft-协议在-nacos-中的实现)
4. [服务实例模型：临时 vs 持久化](#4-服务实例模型临时-vs-持久化)
5. [Nacos 集群与寻址机制](#5-nacos-集群与寻址机制)
6. [健康检查机制内核](#6-健康检查机制内核)
7. [CP vs AP 选型决策](#7-cp-vs-ap-选型决策)

---

## 1. Nacos 整体架构

### 1.1 分层架构

```text
┌─────────────────────────────────────────────────┐
│                  Nacos Server                     │
│                                                  │
│  ┌──────────────────────────────────────────┐   │
│  │           API 层 (HTTP / gRPC)             │   │
│  │   /v1/ns/instance   /v1/cs/configs        │   │
│  └──────────────────┬───────────────────────┘   │
│                     │                            │
│  ┌──────────────────▼───────────────────────┐   │
│  │           核心服务层                       │   │
│  │  ┌─────────────┐  ┌────────────────────┐ │   │
│  │  │ Naming Svc  │  │  Config Svc        │ │   │
│  │  │ (注册中心)   │  │  (配置中心)         │ │   │
│  │  │             │  │                    │ │   │
│  │  │ 服务注册     │  │ 配置 CRUD          │ │   │
│  │  │ 服务发现     │  │ 配置监听(长轮询)    │ │   │
│  │  │ 健康检查     │  │ 灰度发布           │ │   │
│  │  └──────┬──────┘  └─────────┬──────────┘ │   │
│  └─────────┼──────────────────┼─────────────┘   │
│            │                  │                   │
│  ┌─────────▼──────────────────▼─────────────┐   │
│  │          一致性协议层                       │   │
│  │  ┌──────────────┐  ┌──────────────────┐  │   │
│  │  │ Distro (AP)  │  │   Raft (CP)      │  │   │
│  │  │ 临时实例同步  │  │ 持久化实例+配置   │  │   │
│  │  └──────────────┘  └──────────────────┘  │   │
│  └─────────────────────┬───────────────────┘   │
│                        │                        │
│  ┌─────────────────────▼───────────────────┐   │
│  │             存储层                        │   │
│  │  ┌──────────┐  ┌──────────────────────┐ │   │
│  │  │ Derby    │  │  MySQL (生产推荐)     │ │   │
│  │  │ (单机)   │  │  (集群必需)          │ │   │
│  │  └──────────┘  └──────────────────────┘ │   │
│  └─────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
```

### 1.2 Nacos 的数据模型

```text
Nacos 配置中心三层模型：
  Namespace（命名空间）— 租户级隔离
    └── Group（分组）— 环境/业务隔离
          └── DataId（数据ID）— 具体配置文件

例如：
  namespace: prod (生产环境)
    └── group: ORDER_SERVICE
          └── dataId: order-service.yml

  namespace: dev (开发环境)
    └── group: DEFAULT_GROUP
          └── dataId: application.yml
```

---

## 2. AP 模式：Distro 协议深度解析

### 2.1 为什么需要 Distro

```text
问题：临时实例（K8s Pod 自注册），实例变化频繁
  → 用 Raft（需多数派确认）太慢，且不需要强一致性
  → 需要 AP：高可用 + 最终一致性

Distro 的设计目标：
  ✅ 每个 Nacos 节点都可以独立处理写请求
  ✅ 写请求只需一个节点确认即可返回
  ✅ 异步同步到其他节点，最终一致
  ✅ 节点间对等（无 Leader），避免选举延迟
```

### 2.2 Distro 写流程

```text
Client 向 Nacos-Server-A 注册服务实例

Step 1: Client → Server-A
  POST /v1/ns/instance  (注册/心跳)

Step 2: Server-A 写入本地
  → 更新内存注册表 (ConcurrentHashMap)
  → 写入本地存储 (Derby/MySQL)

Step 3: Server-A → Client
  ✅ 立即返回成功（不等待其他节点确认！）

Step 4: Server-A 异步同步到其他节点
  → Distro 协议：生成 distro 任务
  → 异步发送到 Server-B, Server-C
  → 对端校验 checksum，不一致则全量同步

关键特性：
  → 写延迟 = 单节点写入延迟（~1ms）
  → 可用性：任一节点可独立接受写入
  → 一致性：最终一致（不一致窗口 ~500ms-2s）
```

### 2.3 Distro 数据一致性校验

```java
// Nacos Distro 一致性校验的核心逻辑（简化版）
public class DistroConsistencyChecker {

    // 每个节点定期对自己负责的 data 做 checksum
    // 然后发送给其他节点做对比
    @Scheduled(fixedDelay = 2000)  // 每 2 秒
    public void checkConsistency() {
        for (String dataKey : getResponsibleDataKeys()) {
            String localChecksum = computeChecksum(dataKey);

            // 发送给所有其他节点
            for (Server other : getOtherServers()) {
                // RPC: 请检查你的 checksum 是否和我一致？
                CheckResult result = distroClient.check(other, dataKey, localChecksum);

                if (!result.isConsistent()) {
                    // 不一致 → 触发全量同步
                    syncFullData(dataKey, other);
                }
            }
        }
    }
}
```

### 2.4 Distro vs Gossip

```text
┌──────────┬──────────────────┬─────────────────────┐
│   特性    │   Distro (Nacos)  │   Gossip (Consul)   │
├──────────┼──────────────────┼─────────────────────┤
│ 写响应    │ 立即（单节点确认）│ 多数派确认后返回     │
│ 一致性    │ 最终一致          │ 最终一致            │
│ 收敛速度  │ 快（主动checksum）│ 较慢（传播式）       │
│ 写延迟    │ ~1ms              │ ~5-20ms             │
│ 适用场景  │ 实例注册/心跳     │ 分布式状态同步       │
│ 复杂度    │ 中等              │ 较低                │
└──────────┴──────────────────┴─────────────────────┘
```

---

## 3. CP 模式：Raft 协议在 Nacos 中的实现

### 3.1 为什么需要 Raft

```text
场景 1：持久化实例（固定 IP 的物理机/虚拟机）
  实例变化少，但注册信息必须一致 → 每台机器看到的实例列表必须相同

场景 2：配置管理
  配置数据必须强一致 → 用户改了配置，必须确认写入成功，不能丢

Raft 的设计目标：
  ✅ 强一致性：Leader 提案 → 多数派确认 → 提交
  ✅ 自动选举：Leader 宕机 → 新 Leader 选出
  ✅ 日志复制：顺序 commit，不会出现脑裂
```

### 3.2 Nacos 中的 Raft 实现

```text
Nacos 使用 SOFAJRaft（蚂蚁金服开源的高性能 Java Raft 库）

关键配置：
  nacos.core.protocol.raft.data_path = ${nacos.home}/data/raft
  nacos.core.protocol.raft.election_timeout_ms = 1000
  nacos.core.protocol.raft.snapshot_interval_secs = 1800

Raft 组：
  Nacos 中配置管理是一个 Raft Group
  持久化服务实例是另一个 Raft Group
  两个 Group 独立选举 Leader
```

### 3.3 Raft 写流程

```text
Client 向 Nacos 集群发布配置：

Step 1: Client → 任意 Nacos 节点
  POST /v1/cs/configs  (发布配置)

Step 2: 接收节点转发到 Leader
  如果接收节点不是 Leader → HTTP 302 重定向到 Leader
  或 → gRPC 转发请求到 Leader

Step 3: Leader 写入 Raft Log
  Leader 将配置变更写入本地 Raft Log

Step 4: Leader → Followers（复制日志）
  并行发送 AppendEntries RPC 到所有 Follower
  等待至少 N/2+1 个节点确认（包括自己）

Step 5: Leader commit → 返回成功
  多数派确认后，Leader 标记日志为 committed
  应用到状态机（写入 MySQL + 更新内存缓存）

Step 6: Followers 异步 commit
  Leader 在下一次心跳中告知 Followers 最新的 commitIndex
  Followers 将日志应用到自己的状态机
```

---

## 4. 服务实例模型：临时 vs 持久化

### 4.1 两种实例类型

```yaml
# 临时实例（默认，适合 K8s/自动扩缩容）
spring:
  cloud:
    nacos:
      discovery:
        ephemeral: true   # ← 临时实例
        # 特点：
        #   - Distro 协议同步（AP）
        #   - 心跳检测（5秒一次）
        #   - 15秒无心跳 → 自动剔除
        #   - 服务下线不保留记录

# 持久化实例（适合物理机/固定 IP）
spring:
  cloud:
    nacos:
      discovery:
        ephemeral: false  # ← 持久化实例
        # 特点：
        #   - Raft 协议同步（CP）
        #   - 服务端主动健康检查（HTTP/TCP/MySQL）
        #   - 不健康时只标记状态，不剔除
        #   - 适合需要感知"挂了但还在"的场景
```

### 4.2 对比

```text
┌──────────────┬─────────────────────┬─────────────────────┐
│    特性       │    临时实例 (AP)     │   持久化实例 (CP)    │
├──────────────┼─────────────────────┼─────────────────────┤
│ 一致性协议    │ Distro              │ Raft                │
│ 注册方式      │ SDK 自动注册         │ 手动/API 注册       │
│ 健康检查      │ 心跳（被动）         │ 服务端主动探测       │
│ 不健康时      │ 立即剔除             │ 保留实例，标记不健康  │
│ 实例列表      │ 最终一致             │ 强一致              │
│ 适用          │ K8s / Docker        │ 物理机 / 固定IP     │
│ 典型场景      │ 微服务（随时扩缩）    │ DNS / 数据库/缓存    │
└──────────────┴─────────────────────┴─────────────────────┘
```

---

## 5. Nacos 集群与寻址机制

### 5.1 集群部署架构

```text
生产标准：3 节点 Nacos + MySQL 集群

┌─────────────┐   ┌─────────────┐   ┌─────────────┐
│ Nacos-Svr-1 │   │ Nacos-Svr-2 │   │ Nacos-Svr-3 │
│ 192.168.1.10│   │ 192.168.1.11│   │ 192.168.1.12│
│  :8848      │   │  :8848      │   │  :8848      │
└──────┬──────┘   └──────┬──────┘   └──────┬──────┘
       │                 │                 │
       └─────────────────┼─────────────────┘
                         │
                  ┌──────▼──────┐
                  │   MySQL     │  (集群/主从)
                  │ nacos_config│
                  └─────────────┘

前置：Nginx/HAProxy 做负载均衡
  upstream nacos_cluster {
      server 192.168.1.10:8848;
      server 192.168.1.11:8848;
      server 192.168.1.12:8848;
  }
```

### 5.2 三种寻址模式

```text
模式 1：单机寻址（standalone）
  nacos.core.mode=standalone
  → 开发/测试用，不需要 MySQL

模式 2：地址服务器寻址（生产推荐）
  → 部署一个简单的 HTTP 服务，返回集群 IP 列表
  → Nacos 启动时调用这个服务获取集群拓扑
  → 好处：IP 变化只改地址服务器，不改 Nacos 配置

模式 3：配置文件寻址
  cluster.conf:
    192.168.1.10:8848
    192.168.1.11:8848
    192.168.1.12:8848
  → IP 变化需更新所有节点
```

---

## 6. 健康检查机制内核

### 6.1 临时实例：客户端心跳

```java
// Nacos Discovery Client 心跳机制
// 每 5 秒发送一次心跳，15 秒无心跳即标记不健康，30 秒剔除

// 默认配置
spring.cloud.nacos.discovery.heart-beat-interval = 5000  // 心跳间隔(ms)
spring.cloud.nacos.discovery.heart-beat-timeout  = 15000 // 心跳超时
spring.cloud.nacos.discovery.ip-delete-timeout  = 30000  // 实例剔除时间
```

### 6.2 持久化实例：服务端主动探测

```text
Nacos 支持三种健康检查协议：

1. HTTP 探测
   → GET /health → 200 OK 即健康
   → 适合：HTTP 服务

2. TCP 探测
   → TCP Connect 指定端口 → 连接成功即健康
   → 适合：通用服务

3. MySQL 探测
   → SELECT 1 → 有结果即健康
   → 适合：数据库
```

---

## 7. CP vs AP 选型决策

```text
你的场景是？
│
├── 微服务实例管理（K8s/Docker，频繁变化）
│   └── → AP (Distro) + 临时实例
│       理由：高可用 > 强一致，短暂不一致可接受
│
├── 基础服务注册（数据库、缓存、固定 IP）
│   └── → CP (Raft) + 持久化实例
│       理由：实例列表必须准确，不能出现"查到了但连不上"
│
├── 配置管理
│   └── → CP (Raft)
│       理由：配置绝对不能丢，必须确认写入
│
└── 混合场景
    └── → 默认配置如下：
        discovery.ephemeral=true   （AP 临时实例）
        config 自动走 CP（Raft）
        → Nacos 自动切换，无需手动指定
```

---

> 🎯 **核心要点**：Nacos 的 CP/AP 双模式是它区别于 Eureka（纯 AP）和 Consul（纯 CP）的核心优势。微服务实例注册用 AP（Distro），配置管理和持久化实例用 CP（Raft），一个组件解决两种一致性需求。理解 Distro 的异步同步窗口（~500ms-2s）和 Raft 的多数派提交，是排查 Nacos 一致性问题的关键。

---

**下一模块**：[02 - Nacos 配置中心高阶特性](./02-Nacos配置中心高阶特性.md)  
**返回总览**：[00 - 组件体系总览](./00-SpringCloudAlibaba组件体系总览.md)
