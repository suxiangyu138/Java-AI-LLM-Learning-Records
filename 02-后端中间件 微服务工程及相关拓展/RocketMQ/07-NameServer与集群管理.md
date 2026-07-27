# 07 - NameServer 与集群管理

> 🎯 NameServer 是 RocketMQ 中最"轻"的组件 — 无状态、无共识、彼此不通信。理解这个"极简"设计，才能理解 RocketMQ 的架构哲学

---

## 目录

1. [NameServer 设计哲学](#1-nameserver-设计哲学)
2. [NameServer 工作原理](#2-nameserver-工作原理)
3. [路由发现与故障感知](#3-路由发现与故障感知)
4. [集群部署模式](#4-集群部署模式)
5. [DLedger — Raft 自动主从切换](#5-dledger--raft-自动主从切换)
6. [5.x Controller 模式](#6-5x-controller-模式)
7. [高可用架构对比](#7-高可用架构对比)
8. [集群运维实战](#8-集群运维实战)

---

## 1. NameServer 设计哲学

### 1.1 核心设计理念

```text
NameServer 设计三原则：

① 无状态（Stateless）
   → 不持久化任何数据
   → 所有数据来自 Broker 心跳上报
   → 重启即可恢复，无需数据迁移

② 去中心化
   → NameServer 之间不通信
   → 无主从之分，无选举机制
   → 每个 NameServer 拥有完整路由表

③ 最终一致性
   → Broker 同时向所有 NameServer 注册
   → 路由信息可能有短暂不一致
   → 客户端通过重试机制容错
```

### 1.2 NameServer vs ZooKeeper vs Nacos

| 维度 | NameServer | ZooKeeper | Nacos |
|------|------------|-----------|-------|
| **一致性** | 最终一致 | CP（强一致） | AP/CP 可切换 |
| **共识协议** | 无 | ZAB | Raft/Distro |
| **节点通信** | 互不通信 | 选举+同步 | 去中心化 |
| **持久化** | 全内存 | 磁盘+快照 | DB+本地文件 |
| **CAP 偏向** | AP | CP | AP/CP |
| **部署节点** | 2~N（推荐2+） | 3/5 奇数 | 2~N |
| **运维复杂度** | 极低 | 中等 | 中等 |
| **适用场景** | RocketMQ 专属 | 通用协调服务 | 注册+配置中心 |

---

## 2. NameServer 工作原理

### 2.1 核心数据结构

```java
// NameServer 路由信息管理
public class RouteInfoManager {
    // Topic → Queue 分布
    private final HashMap<String, List<QueueData>> topicQueueTable;
    // Broker 信息（名称→地址）
    private final HashMap<String, BrokerData> brokerAddrTable;
    // 集群 → Broker 名称列表
    private final HashMap<String, Set<String>> clusterAddrTable;
    // Broker 存活状态（地址→最后心跳时间）
    private final HashMap<String, BrokerLiveInfo> brokerLiveTable;
    // Filter Server 列表（5.x 废弃）
    private final HashMap<String, List<String>> filterServerTable;
}
```

### 2.2 路由注册

```text
Broker 启动时注册流程：

① Broker 启动 → 读取 namesrvAddr 配置
② 向每个 NameServer 发送注册请求
   请求体：clusterName, brokerName, brokerId, brokerAddr,
            Topic配置（读写Queue数、权限等）
③ NameServer 收到 → 写入内存路由表
④ Broker 每 30s 发送心跳 → 更新 brokerLiveTable 时间戳

NameServer 如何判断 Broker 离线？
  每 10s 扫描 brokerLiveTable → 最后心跳 > 120s → 标记死亡
  → 清除该 Broker 的路由信息
```

### 2.3 路由剔除

```text
Broker 被剔除的三种情况：

① 正常下线
   Broker 发送 UNREGISTER_BROKER 请求 → NameServer 立即删除路由

② 异常宕机
   心跳超时 120s → NameServer 扫描线程发现 → 删除路由

③ NameServer 宕机重启
   内存清空 → 等待 Broker 心跳后重建路由表
```

---

## 3. 路由发现与故障感知

### 3.1 客户端路由发现

```text
Producer/Consumer 如何获取路由：

① 启动时 → 随机选一个 NameServer → 获取全量路由
② 每 30s → 定时向 NameServer 拉取路由更新
③ 检测到 Broker 不可用 → 立即从 NameServer 拉取最新路由

路由信息包括：
  → Topic 有哪些 MessageQueue
  → 每个 MessageQueue 在哪个 Broker 上
  → Broker Master/Slave 地址
  → 各 Broker 的读写权限
```

### 3.2 故障延迟与隔离

```java
// Producer 端故障感知
producer.setSendLatencyFaultEnable(true);

// 当向 Broker 发送失败时：
// ① 标记该 Broker 不可用（一段时间）
// ② 切换到其他 Broker 的 Queue
// ③ 定时拉取 NameServer 路由 → 确认是否有新 Broker

// 故障延迟时间段：
// Broker 发送超时/失败 → 设为不可用 × 秒 → 到期后重新尝试
```

---

## 4. 集群部署模式

### 4.1 单 Master（测试）

```text
┌──────────┐     ┌──────────┐
│NameServer│     │  Broker  │
│  node1   │◄───→│  Master  │
└──────────┘     └──────────┘

✅ 最简单，开发测试
❌ 无高可用，宕机即不可用
```

### 4.2 多 Master（无 Slave）

```text
┌──────────┐ ┌──────────┐
│NameServer│ │NameServer│
│  node1   │ │  node2   │
└────┬─────┘ └────┬─────┘
     │            │
┌────▼─────┐ ┌───▼──────┐
│ Broker-A │ │ Broker-B │
│ (Master) │ │ (Master) │
└──────────┘ └──────────┘

✅ 读写高可用（多 Master 分摊）
✅ 配置简单
❌ Master 宕机 → 该机器上数据暂时不可消费
```

### 4.3 多 Master 多 Slave（异步复制）

```text
┌──────────┐ ┌──────────┐
│NameServer│ │NameServer│
└────┬─────┘ └────┬─────┘
     │            │
┌────▼──────────┐ ┌───────────────┐
│  Broker-A     │ │  Broker-B     │
│ ┌──────┐     │ │ ┌──────┐      │
│ │Master│─────│─│─│Slave │      │  交叉部署
│ └──────┘     │ │ └──────┘      │
│ ┌──────┐     │ │ ┌──────┐      │
│ │Slave │◄────│─│─│Master│      │
│ └──────┘     │ │ └──────┘      │
└──────────────┘ └───────────────┘

✅ 高可用（Master 宕机可手动切 Slave）
✅ 主从异步复制，性能好
✅ ⭐ 生产推荐（经典配置）
❌ Master 宕机丢失少量异步数据
❌ 主从切换需手动
```

### 4.4 多 Master 多 Slave（同步复制）

```text
Master ──同步刷盘──→ Slave（等待确认后才返回 Producer）

✅ 强一致，不丢数据
❌ 延迟高（同步刷盘 + 同步复制）
❌ 吞吐低
```

---

## 5. DLedger — Raft 自动主从切换

### 5.1 DLedger 解决的问题

```text
传统主从的痛点：
  ❌ Master 宕机 → 手动切换（或脚本切换） → 分钟级
  ❌ 切换过程中可能有数据丢失（异步复制）
  ❌ 切换后需要修改配置，运维成本高

DLedger（RocketMQ 4.5+）：
  ✅ 基于 Raft 协议，自动选主
  ✅ 切换秒级完成
  ✅ 数据强一致（Raft Log）
  ✅ 无需外部组件
```

### 5.2 DLedger 架构

```text
DLedger 三节点 Raft 组：

┌──────────────────────────────────────────┐
│           Raft Group (3 nodes)           │
│                                          │
│  ┌─────────────┐  ┌─────────────┐       │
│  │   Leader    │  │  Follower   │  ...  │
│  │  (Master)   │  │  (Slave)    │       │
│  └──────┬──────┘  └──────┬──────┘       │
│         │                │              │
│         └──── Raft Log ──┘              │
└──────────────────────────────────────────┘

DLedger 替换了 CommitLog：
  原本的 CommitLog 写入 → 改为写入 DLedger 的 Raft Log
```

### 5.3 DLedger vs 传统主从

| 维度 | 传统主从 | DLedger |
|------|----------|---------|
| **选主** | 手动/脚本 | 自动(Raft) |
| **切换时间** | 分钟级 | 秒级 |
| **数据一致性** | 可能丢失(Async) | 强一致(Raft) |
| **最小节点** | 2 | 3 |
| **外部依赖** | 无 | 无 |
| **运维复杂度** | 低→高(手动切换) | 中(自动) |
| **性能** | 高(Async) | 中(Raft开销) |

---

## 6. 5.x Controller 模式

### 6.1 Controller 模式简介

```text
RocketMQ 5.x 引入 Controller — DLedger 的升级替代

Controller 职责：
  ✅ Broker 主备切换管理
  ✅ 选主决策（Raft 共识）
  ✅ 管理 Broker 的 Active/Standby 状态

与 DLedger 的区别：
  → DLedger: 存储和选主耦合（DLedger 直接管理 CommitLog）
  → Controller: 存储和选主分离（Controller 只管选主）
  → 更好的可扩展性（支持更多 Broker）
```

### 6.2 Controller 架构

```text
┌─────────────────────────────────────────────────────┐
│              Controller 集群（Raft）                   │
│     C1(Leader)     C2(Follower)    C3(Follower)     │
│          │                                        │
│          │ 管理 Broker 状态，决定主备                  │
├──────────┼─────────────────────────────────────────┤
│     ┌────▼─────┐  ┌──────────┐  ┌──────────┐       │
│     │Broker-A1 │  │Broker-A2 │  │Broker-A3 │       │
│     │(Master)  │  │(Slave)   │  │(Slave)   │       │
│     └──────────┘  └──────────┘  └──────────┘       │
│                                                     │
│     ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│     │Broker-B1 │  │Broker-B2 │  │Broker-B3 │       │
│     │(Master)  │  │(Slave)   │  │(Slave)   │       │
│     └──────────┘  └──────────┘  └──────────┘       │
└─────────────────────────────────────────────────────┘

Controller 通过 Raft 保证选主决策的一致性
Broker 专注于消息存储（无需关心选主）
```

---

## 7. 高可用架构对比

| 方案 | 自动切换 | 数据一致 | 最小节点 | 成熟度 | 推荐 |
|------|:---:|:---:|:---:|:---:|:---:|
| **传统主从(Async)** | ❌ | ❌(可能丢) | 2 | ⭐⭐⭐⭐⭐ | 老项目 |
| **传统主从(Sync)** | ❌ | ✅ | 2 | ⭐⭐⭐ | 金融 |
| **DLedger** | ✅ | ✅ | 3 | ⭐⭐⭐⭐ | 4.x 推荐 |
| **Controller(5.x)** | ✅ | ✅ | 3(Controller)+2(Broker) | ⭐⭐⭐ | 5.x 推荐 |

---

## 8. 集群运维实战

### 8.1 日常运维命令

```bash
# 查看集群状态
sh bin/mqadmin clusterList -n localhost:9876
# 输出：
# Cluster Name: DefaultCluster
# Broker Name    BID  Addr         VERSION       InTPS OutTPS ...
# broker-a       0    10.0.0.1:10911  V4_9_8      100   200
# broker-a       1    10.0.0.2:10911  V4_9_8      0     100

# 查看 NameServer 状态
sh bin/mqadmin getNameServerConfig -n localhost:9876
```

### 8.2 Broker 上下线

```bash
# 1. 优雅下线
# 首先停止写入（禁用 Broker）
sh bin/mqadmin wipeWritePerm -b broker-a -n localhost:9876
# 等待消费完积压后，停进程
kill <broker-pid>

# 2. 新增 Broker
# 配置新的 broker.conf，brokerName=new-broker
# 启动 Broker
nohup sh bin/mqbroker -c conf/new-broker.conf &
# 自动向 NameServer 注册，无需额外操作
```

### 8.3 主从切换

```bash
# 传统模式：手动切换（Master 宕机时）
# 1. 停掉原 Master 进程
# 2. 修改 Slave 配置: brokerId=0, brokerRole=ASYNC_MASTER
# 3. 重启 Slave → 成为新 Master
# ⚠️ 不推荐线上手动切换，请使用 DLedger/Controller

# DLedger 模式：自动切换（无需手动操作）
# Controller 模式：自动切换（无需手动操作）
```

### 8.4 监控指标

| 指标 | 说明 | 告警 |
|------|------|:---:|
| `BrokerTps` | 每秒消息数 | 突降 |
| `BrokerPutNums` | 写入总量 | |
| `BrokerGetNums` | 消费总量 | |
| `DiffTotal` | 消费积压 | >阈值 |
| `RuntimeEagleEye` | Broker 运行状态 | 异常 |
| `diskUtil` | 磁盘使用率 | >80% |

---

## 集群管理速查

| 场景 | 方案 | 推荐度 |
|------|------|:---:|
| 开发测试 | 单 Master | ⭐⭐⭐⭐⭐ |
| 中小规模生产 | 多 Master 多 Slave（异步） | ⭐⭐⭐⭐⭐ |
| 金融级 | 多 Master 多 Slave（同步）或 DLedger | ⭐⭐⭐⭐ |
| 4.x 自动切换 | DLedger | ⭐⭐⭐⭐ |
| 5.x 新项目 | Controller 模式 | ⭐⭐⭐⭐ |
| 跨机房 | 多 NameServer + 异步复制 | ⭐⭐⭐ |
