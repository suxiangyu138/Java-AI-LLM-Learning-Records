# 06 - Broker 与集群管理

> 🎯 Broker 是 Kafka 集群的骨架，Controller 是大脑 — 理解 Controller 选举、副本同步、故障恢复，才能运维好生产级 Kafka 集群

---

## 目录

1. [Broker 概述](#1-broker-概述)
2. [Controller 机制](#2-controller-机制)
3. [KRaft 共识协议](#3-kraft-共识协议)
4. [分区副本管理](#4-分区副本管理)
5. [Leader 选举与故障恢复](#5-leader-选举与故障恢复)
6. [集群运维操作](#6-集群运维操作)
7. [Broker 关键配置](#7-broker-关键配置)

---

## 1. Broker 概述

### 1.1 Broker 是什么

```text
Broker = 一个 Kafka 服务进程

每个 Broker：
  ✅ 处理生产者/消费者请求
  ✅ 管理存储的 Partition 副本
  ✅ 与其他 Broker 通信（副本同步/Controller 选举）
  ✅ 向 Controller 注册自己
```

### 1.2 集群拓扑

```text
Kafka 集群（3 个 Broker, 3 分区, 副本=3）

┌──────────────────────────────────────────────────────┐
│  Controller（Broker 1 当选）                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │
│  │  Broker 1    │  │  Broker 2    │  │  Broker 3    │  │
│  │              │  │              │  │              │  │
│  │ P0 (Leader)  │  │ P0 (Follower)│  │ P0 (Follower)│  │
│  │ P1 (Follower)│  │ P1 (Leader)  │  │ P1 (Follower)│  │
│  │ P2 (Follower)│  │ P2 (Follower)│  │ P2 (Leader)  │  │
│  └─────────────┘  └─────────────┘  └─────────────┘  │
└──────────────────────────────────────────────────────┘
```

### 1.3 Broker 内部组件

```text
Broker 进程内部：
┌───────────────────────────────────────────────────┐
│  SocketServer（NIO 网络层）                        │
│  ├── Acceptor Thread（接收连接）                   │
│  ├── Processor Thread（读写处理）                  │
│  └── RequestChannel（请求队列）                    │
├───────────────────────────────────────────────────┤
│  KafkaApis（请求处理器）                           │
│  ├── Produce Request Handler                      │
│  ├── Fetch Request Handler                        │
│  ├── JoinGroup / SyncGroup Handler                │
│  └── ...                                          │
├───────────────────────────────────────────────────┤
│  ReplicaManager（副本管理器）                      │
│  ├── LogManager（日志管理）                        │
│  └── Partition（分区实例）                         │
├───────────────────────────────────────────────────┤
│  KafkaController（如果是 Controller 节点）         │
│  └── 集群元数据管理 + 分区分配                     │
└───────────────────────────────────────────────────┘
```

---

## 2. Controller 机制

### 2.1 Controller 职责

```text
Kafka Controller 是集群的"大脑"，负责：
  ✅ Broker 上下线管理
  ✅ Topic 创建/删除（分区 + 副本分配）
  ✅ Partition Leader 选举
  ✅ 分区副本重分配（迁移）
  ✅ 元数据广播到其他 Broker

一个集群同时只有 1 个 Controller
```

### 2.2 Controller 选举（ZK 模式）

```text
ZooKeeper 模式下的 Controller 选举：

① 所有 Broker 在 ZK /controller 节点争抢创建临时节点
② 创建成功者 → Controller
③ Controller 在 ZK 注册 Watcher
④ Controller 宕机 → 临时节点消失 → 其他 Broker 争抢 → 新 Controller

问题：
  ❌ ZK 依赖 → 运维复杂
  ❌ Controller 切换时元数据全量加载 → 长时间不可用
  ❌ ZK 写入压力大（每个 Broker 注册、Segment 信息变更）
```

### 2.3 Controller 故障影响

```text
Controller 宕机期间：
  → 无法创建/删除 Topic
  → 无法处理 Broker 上下线
  → Partition Leader 不切换
  → 已存在的生产/消费不受影响 ✅

Controller 切换期间（ZK 模式）：
  → 集群短暂不可管理（几秒到几十秒）
  → KRaft 模式切换更快（元数据已在日志中）
```

---

## 3. KRaft 共识协议

### 3.1 为什么需要 KRaft

```text
ZooKeeper 模式的痛点：
  ❌ 运维两套系统（Kafka + ZK）
  ❌ 元数据一致性依赖 ZK
  ❌ Controller 切换慢（需全量加载 ZK 元数据）
  ❌ Kafka 受限于 ZK 的写入能力（ZK 写不适合高并发）

KRaft 方案：
  ✅ Kafka 自带元数据管理，无需 ZooKeeper
  ✅ 元数据日志（Metadata Log）基于 Raft 共识
  ✅ Controller 切换毫秒级（元数据已在本地日志中）
  ✅ 支持百万级分区（ZK 模式下受限于 ZK 性能）
```

### 3.2 KRaft 架构

```text
KRaft 架构中的角色：

┌────────────────────────────────────────────────────────┐
│                  Quorum Controller（Raft 集群）          │
│  ┌──────────────────┐  ┌──────────────────┐            │
│  │ Controller Node 1  │  │ Controller Node 2  │  ...   │
│  │ (Active Leader)    │  │ (Follower)         │        │
│  └────────┬───────────┘  └──────────────────┘           │
├───────────┼────────────────────────────────────────────┤
│           │ 元数据变更通过 Raft 日志复制                  │
├───────────┼────────────────────────────────────────────┤
│           ↓                                            │
│  ┌──────────────────┐  ┌──────────────────┐            │
│  │ Broker Node 1    │  │ Broker Node 2    │  ...       │
│  │ (也可能是         │  │                  │            │
│  │  Controller)     │  │                  │            │
│  └──────────────────┘  └──────────────────┘            │
└────────────────────────────────────────────────────────┘

process.roles 配置：
  broker           → 纯 Broker
  controller       → 纯 Controller（仅 KIP-500 模式）
  broker,controller → 混合模式（推荐，生产常用）
```

### 3.3 KRaft 与 ZK 模式对比

| 维度 | ZK 模式 | KRaft 模式 |
|------|---------|------------|
| **外部依赖** | ZooKeeper（3~5 节点） | 无 |
| **Controller 切换** | 几十秒（需加载 ZK） | 毫秒级 |
| **分区数上限** | ~20 万（ZK 限制） | 百万级 |
| **运维复杂度** | 高（两套系统） | 低 |
| **元数据一致性** | ZK 保证 | Raft 保证 |
| **迁移路径** | — | 支持 ZK→KRaft 在线迁移 |
| **成熟度** | 非常成熟 | 3.3+ 稳定 |

---

## 4. 分区副本管理

### 4.1 副本分布规则

```text
副本分配算法（默认）：
  ① 首个副本随机落在某个 Broker（轮询）
  ② 后续副本按间隔 = (numBrokers / replicationFactor) 分配
  ③ 同一 Partition 的副本不在同一 Broker
  ④ 尽可能让每个 Broker 上的 Leader 数均衡

示例（3 Broker, 3 Partition, RF=3）：
  Broker 1: P0(L) P1(F) P2(F)  → 各 1 个 Leader ✅
  Broker 2: P0(F) P1(L) P2(F)
  Broker 3: P0(F) P1(F) P2(L)
```

### 4.2 副本同步过程

```text
Follower 如何同步 Leader：

1. Follower → Leader：FetchRequest（我拉到了 offset=X）
2. Leader → Follower：FetchResponse（这里有 offset X+1 到 Y 的消息）
3. Follower 写入本地日志 → 更新 LEO
4. Leader 收到 Follower 的 Fetch 确认 → 推进 HW

关键参数：
  ✅ replica.fetch.wait.max.ms=500    → Follower 长轮询等待时间
  ✅ replica.fetch.max.bytes=1MB      → 单次 Fetch 最大字节
  ✅ replica.socket.timeout.ms=30000  → 连接超时
```

### 4.3 首选 Leader（Preferred Leader）

```text
每个 Partition 有首选 Leader（preferred leader）：
  → 副本分配时第一个分配的副本
  → 集群正常时应该是 Leader

auto.leader.rebalance.enable=true（默认）
  → 自动将 Leader 切回首选副本

手动触发：
  kafka-leader-election.sh --bootstrap-server localhost:9092 \
    --election-type preferred --all-topic-partitions
```

---

## 5. Leader 选举与故障恢复

### 5.1 Leader 选举过程

```text
场景：Leader Broker 宕机

① Controller 感知（ZK Watch / KRaft 事件）
② 从 ISR 中选新 Leader（按 AR 顺序，AR 靠前的优先）
③ 广播 LeaderAndIsr 请求给相关 Broker
④ Follower 升级为 Leader
⑤ Producer/Consumer 收到元数据更新 → 重连新 Leader

⚠️ ISR 为空时：
  unclean.leader.election.enable=true（默认 false）
    → 允许非 ISR 副本当选 Leader → 可能丢数据
  unclean.leader.election.enable=false（推荐）
    → 宁可不可用，也不丢数据
```

### 5.2 故障场景分析

| 场景 | 影响 | 恢复 |
|------|------|------|
| **Follower 宕机** | ISR 缩小，无数据丢失 | 重启后追数据，重入 ISR |
| **Leader 宕机（ISR 非空）** | 短暂不可用，无丢数据 | ISR 中选新 Leader |
| **Leader 宕机（ISR 为空）** | 不可用（等待 ISR 恢复） | 手动复活或开启 unclean 选举 |
| **Broker 宕机** | 其上 Leader 分区切换 | 重启后恢复 |
| **磁盘故障** | 数据永久丢失 | 重建 Broker，从其他副本复制 |

### 5.3 分区迁移（Reassignment）

```bash
# 生成迁移计划
kafka-reassign-partitions.sh --bootstrap-server localhost:9092 \
  --topics-to-move-json-file topics.json \
  --broker-list "0,1,2" --generate

# 执行迁移
kafka-reassign-partitions.sh --bootstrap-server localhost:9092 \
  --reassignment-json-file reassign.json --execute

# 查看进度
kafka-reassign-partitions.sh --bootstrap-server localhost:9092 \
  --reassignment-json-file reassign.json --verify
```

---

## 6. 集群运维操作

### 6.1 Broker 上下线

```bash
# 1. 优雅下线（推荐）
# 先迁移所有分区 Leader
# 然后停止 Broker 进程

# 下线前检查
kafka-broker-api-versions.sh --bootstrap-server localhost:9092

# 2. 新增 Broker
# 配置新 Broker，确保 broker.id 唯一
# 启动 → 自动注册到集群
# 如有需要，手动迁移分区到新 Broker

# 3. 查看集群成员
kafka-metadata.sh --snapshot /data/kafka/.../__cluster_metadata-0/*.log \
  --command "broker.list()"
```

### 6.2 滚动升级

```text
Kafka 支持在线滚动升级（零停机）

步骤：
  ① 依次重启每个 Broker
  ② 先升级 Follower，最后升级 Leader
  ③ 确认上一个 Broker 恢复后再重启下一个
  
版本兼容性：
  ✅ Kafka 支持跨版本通信（如 2.8 ↔ 3.x）
  ✅ 建议不要跨度太大（< 2 个大版本）
  ✅ 新版本特性需所有 Broker 升级后开启
```

### 6.3 均衡检查

```bash
# 查看 Leader 分布
kafka-topics.sh --bootstrap-server localhost:9092 --describe --under-replicated-partitions

# 查看副本不足的分区
kafka-topics.sh --bootstrap-server localhost:9092 --describe --under-replicated-partitions

# 查看离线分区
kafka-topics.sh --bootstrap-server localhost:9092 --describe --unavailable-partitions
```

---

## 7. Broker 关键配置

### 7.1 生产必配清单

```properties
# ========== 必配 ==========
broker.id=每个节点唯一
listeners=PLAINTEXT://实际IP:9092
advertised.listeners=PLAINTEXT://对外IP:9092
log.dirs=/data/kafka/data1,/data/kafka/data2  # 多磁盘

# ========== 副本 ==========
default.replication.factor=3
min.insync.replicas=2
offsets.topic.replication.factor=3
transaction.state.log.replication.factor=3
transaction.state.log.min.isr=2

# ========== 存储 ==========
log.retention.hours=168              # 7 天
log.segment.bytes=1073741824         # 1GB
log.retention.check.interval.ms=300000 # 5 分钟检查一次

# ========== 网络 ==========
num.network.threads=8                # 网络线程数
num.io.threads=16                    # I/O 线程数
socket.send.buffer.bytes=102400      # 发送缓冲区
socket.receive.buffer.bytes=102400   # 接收缓冲区

# ========== 杂项 ==========
auto.create.topics.enable=false      # 禁止自动创建 Topic
delete.topic.enable=true             # 允许删除 Topic
unclean.leader.election.enable=false # 禁止非 ISR 选举
```

### 7.2 JVM 调优

```bash
# KAFKA_HEAP_OPTS（生产建议）
export KAFKA_HEAP_OPTS="-Xms6g -Xmx6g -XX:MetaspaceSize=96m
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=20
  -XX:InitiatingHeapOccupancyPercent=35
  -XX:G1HeapRegionSize=16M
  -XX:MinMetaspaceFreeRatio=50
  -XX:MaxMetaspaceFreeRatio=80"

# GC 日志
export KAFKA_GC_LOG_OPTS="-Xlog:gc*:file=/var/log/kafka/gc.log:time,tags:filecount=10,filesize=100M"
```

### 7.3 OS 调优

```bash
# /etc/sysctl.conf
vm.swappiness=1              # 尽量不用 swap
vm.dirty_ratio=60            # 脏页比例（Kafka 写多）
vm.dirty_background_ratio=10
net.core.rmem_max=16777216   # Socket 接收缓冲最大
net.core.wmem_max=16777216   # Socket 发送缓冲最大

# 文件描述符
# /etc/security/limits.conf
kafka soft nofile 65536
kafka hard nofile 65536
```

---

## 集群运维速查

| 操作 | 命令 |
|------|------|
| 查看集群 Broker | `kafka-broker-api-versions.sh --bootstrap-server ...` |
| 查看复制不足分区 | `kafka-topics.sh --describe --under-replicated-partitions` |
| 查看消费组 | `kafka-consumer-groups.sh --list` |
| 查看积压 | `kafka-consumer-groups.sh --describe --group xxx` |
| 迁移分区 | `kafka-reassign-partitions.sh --generate/--execute/--verify` |
| 选举 Leader | `kafka-leader-election.sh --election-type preferred` |
| 修改 Topic 配置 | `kafka-configs.sh --alter --entity-type topics` |
| 查看 Broker 配置 | `kafka-configs.sh --describe --entity-type brokers` |
