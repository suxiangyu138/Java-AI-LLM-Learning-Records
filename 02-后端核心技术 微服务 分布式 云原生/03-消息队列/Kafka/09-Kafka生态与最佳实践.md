# 09 - Kafka 生态与最佳实践

> 🎯 Kafka 不只是消息队列 — Streams、Connect、MirrorMaker 组成完整的数据流平台。掌握生态 + 避坑指南，才算真正的 Kafka 工程师

---

## 目录

1. [Kafka 生态全景](#1-kafka-生态全景)
2. [Kafka Streams](#2-kafka-streams)
3. [Kafka Connect](#3-kafka-connect)
4. [MirrorMaker 跨集群复制](#4-mirrormaker-跨集群复制)
5. [消息可靠性实践](#5-消息可靠性实践)
6. [生产避坑指南](#6-生产避坑指南)
7. [高频面试题精选](#7-高频面试题精选)

---

## 1. Kafka 生态全景

```text
                      Apache Kafka 生态
┌───────────────────────────────────────────────────────────┐
│                                                           │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
│  │ Streams   │  │ Connect  │  │ Mirror   │  │ ksqlDB   │  │
│  │流处理      │  │数据管道   │  │ Maker    │  │流式 SQL  │  │
│  │ (Lib)     │  │ (节点式)  │  │跨集群复制 │  │          │  │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘  │
│                                                           │
│  ┌──────────────────────────────────────────────────────┐ │
│  │                  Kafka Core                          │ │
│  │  Broker / Topic / Partition / Producer / Consumer    │ │
│  └──────────────────────────────────────────────────────┘ │
│                                                           │
│  周边：                                                   │
│  ┌────────┐ ┌──────┐ ┌──────┐ ┌──────────┐ ┌────────┐   │
│  │ Schema  │ │ Kafka│ │Cruise│ │ Flink    │ │ Spring  │   │
│  │Registry │ │  UI  │ │Ctrl  │ │ Connector│ │ Kafka   │   │
│  └────────┘ └──────┘ └──────┘ └──────────┘ └────────┘   │
└───────────────────────────────────────────────────────────┘
```

---

## 2. Kafka Streams

### 2.1 是什么

```text
Kafka Streams = 构建在 Kafka 之上的轻量级流处理库

特点：
  ✅ 纯 Java 库（不是独立集群）
  ✅ 部署在应用进程中（无需额外资源）
  ✅ Exactly-Once 语义
  ✅ 有状态处理（窗口、聚合、Join）
  ✅ 弹性伸缩（基于消费组）

vs Flink / Spark：
  → Streams 轻量、无需集群，适合嵌入微服务
  → Flink 强大、独立集群，适合企业级大数据管道
```

### 2.2 核心概念

```java
// Word Count — Streams 版的 Hello World
StreamsBuilder builder = new StreamsBuilder();

KStream<String, String> textLines = builder.stream("input-topic");

KTable<String, Long> wordCounts = textLines
    .flatMapValues(text -> Arrays.asList(text.toLowerCase().split("\\W+")))
    .groupBy((key, word) -> word)
    .count();

wordCounts.toStream().to("output-topic", Produced.with(Serdes.String(), Serdes.Long()));

KafkaStreams streams = new KafkaStreams(builder.build(), props);
streams.start();
```

### 2.3 Streams DSL 速查

| 操作 | 说明 | 示例 |
|------|------|------|
| `filter` | 过滤 | `.filter((k, v) -> v > 0)` |
| `map` | 转换 | `.map((k, v) -> KeyValue.pair(newK, newV))` |
| `flatMap` | 1→N 转换 | `.flatMap((k, v) -> list)` |
| `groupBy` | 分组 | `.groupBy((k, v) -> newK)` |
| `count` | 计数 | `.count()` → KTable |
| `reduce` | 聚合 | `.reduce((v1, v2) -> v1 + v2)` |
| `join` | 流-流/流-表 Join | `.join(table, joiner, window)` |
| `windowedBy` | 加窗 | `.windowedBy(TimeWindows.of(...))` |
| `to` | 输出到 Topic | `.to("output")` |

### 2.4 窗口类型

```text
┌────────────────────────────────────────────────────┐
│  翻滚窗口（Tumbling Window）                        │
│  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐                │
│  │ w1  │ │ w2  │ │ w3  │ │ w4  │ ← 不重叠          │
│  └─────┘ └─────┘ └─────┘ └─────┘                │
│  TimeWindows.of(Duration.ofMinutes(5))            │
├────────────────────────────────────────────────────┤
│  跳跃窗口（Hopping Window）                        │
│  ┌──────────┐                                     │
│  │   w1     │                                     │
│  │   ┌──────────┐  ← 有重叠                       │
│  │   │   w2     │                                 │
│  └───┴──────────┴──                               │
│  TimeWindows.of(Duration.ofMinutes(5))            │
│              .advanceBy(Duration.ofMinutes(1))    │
├────────────────────────────────────────────────────┤
│  会话窗口（Session Window）                        │
│  ┌───┐    ┌──┐    ┌──────┐ ← 无活动间隙            │
│  │s1 │    │s2│    │ s3   │                         │
│  └───┘    └──┘    └──────┘                         │
│  SessionWindows.with(Duration.ofMinutes(30))      │
└────────────────────────────────────────────────────┘
```

---

## 3. Kafka Connect

### 3.1 架构

```text
Kafka Connect = 数据集成框架（"连接器"）

┌──────────┐          ┌──────────────────┐          ┌──────────┐
│  MySQL   │─────────→│  Source Connector │          │  MySQL   │
│  MongoDB │          │  (从外部系统读取)   │          │  ES      │
│  Files   │          │  ┌──────────────┐ │          │  HDFS    │
└──────────┘          │  │ Kafka Connect │ │          └──────────┘
                      │  │   (Worker)    │ │               ↑
┌──────────┐          │  └──────────────┘ │          ┌──────────┐
│  Kafka   │←────────│  Sink Connector    │─────────→│  Kafka   │
│  Topic   │────────→│  (写入外部系统)     │←─────────│  Topic   │
└──────────┘          └──────────────────┘          └──────────┘

Worker 模式：
  standalone  → 单进程（测试）
  distributed → 分布式（生产），自动负载均衡、容错
```

### 3.2 常用连接器

| 连接器 | 类型 | 用途 |
|--------|:---:|------|
| **Debezium MySQL** | Source | MySQL binlog → Kafka（CDC） |
| **JDBC Sink** | Sink | Kafka → 关系型数据库 |
| **Elasticsearch Sink** | Sink | Kafka → ES |
| **S3 Sink** | Sink | Kafka → AWS S3 / 对象存储 |
| **FileStream** | Source/Sink | 文件 ↔ Kafka（测试用） |

### 3.3 Debezium CDC 示例

```json
{
  "name": "mysql-connector",
  "config": {
    "connector.class": "io.debezium.connector.mysql.MySqlConnector",
    "database.hostname": "localhost",
    "database.port": "3306",
    "database.user": "debezium",
    "database.password": "dbz",
    "database.server.id": "184054",
    "database.server.name": "my-app",
    "database.include.list": "mydb",
    "table.include.list": "mydb.users,mydb.orders",
    "database.history.kafka.bootstrap.servers": "localhost:9092",
    "database.history.kafka.topic": "schema-changes.mydb"
  }
}
```

---

## 4. MirrorMaker 跨集群复制

### 4.1 使用场景

```text
┌─────────────────┐         MirrorMaker 2        ┌─────────────────┐
│  DC-1 (北京)     │ ─────────── MM2 ───────────→ │  DC-2 (上海)     │
│  Kafka Cluster A │ ←────────── MM2 ──────────── │  Kafka Cluster B │
└─────────────────┘                              └─────────────────┘

典型场景：
  ✅ 灾备（DR）— 跨数据中心复制
  ✅ 数据聚合 — 多个集群数据汇总到中心集群
  ✅ 迁移 — 逐步迁移流量到新集群
  ✅ 隔离 — 分集群分业务，按需同步
```

### 4.2 MirrorMaker 2 架构

```text
MM2（基于 Kafka Connect 框架）

核心组件：
  ├── MirrorSourceConnector → 源集群 → 目标集群
  ├── MirrorCheckpointConnector → 同步消费位移
  └── MirrorHeartbeatConnector → 心跳检测可达性

MM2 特性：
  ✅ Offset 同步（切集群时 Consumer 不重复消费）
  ✅ Topic 配置同步
  ✅ 防止循环复制（源标记避免 A→B→A）
  ✅ 基于 Connect 分布式框架 → 水平扩展
```

---

## 5. 消息可靠性实践

### 5.1 不丢消息清单

```text
生产者端：
  ✅ acks=all（等待所有 ISR 确认）
  ✅ retries=MAX_INT（无限重试）
  ✅ enable.idempotence=true（去重）
  ✅ min.insync.replicas≥2（至少 2 个副本确认）
  ❌ acks=0 或 acks=1（有丢风险）

Broker 端：
  ✅ replication.factor=3（副本数 ≥ 3）
  ✅ min.insync.replicas=2（ISR 最少 2 个）
  ✅ unclean.leader.election.enable=false（禁止乱选举）
  ✅ 至少 3 台 Broker

消费者端：
  ✅ enable.auto.commit=false（手动提交）
  ✅ 先处理消息，再提交位移
  ✅ 业务逻辑幂等
  ✅ close 前 commitSync()
```

### 5.2 不重复消息清单

```text
业务层幂等设计：
  ✅ 数据库主键/唯一索引 → INSERT ... ON DUPLICATE KEY UPDATE
  ✅ Redis SET NX / 版本号
  ✅ 消息中携带业务唯一 ID → 消费前检查 Redis/DB 是否已处理
  ✅ Kafka 事务（跨 Topic 场景）

不可依赖的：
  ❌ enable.idempotence=true（仅单 Producer 会话内有用，跨重启无效）
  ❌ 消费组位移（只防不丢，不防不重）
```

### 5.3 顺序性保证

```text
全局有序（几乎不可能，也无需追求）：
  全量消息 → 1 个 Partition → 1 个 Consumer
  代价：吞吐上限 = 单分区吞吐

分区有序（推荐）：
  相同业务 Key → 相同 Partition → 单 Consumer 消费
  ✅ 生产者: key=orderId → orderId 相同的到同一分区
  ✅ 消费者: max.poll.records 别太大，避免拉取过多乱序
  ✅ 幂等模式下允许 max.in.flight > 1
  ❌ 无幂等时: max.in.flight.requests.per.connection = 1
```

---

## 6. 生产避坑指南

### 6.1 十大高频坑

| # | 坑 | 原因 | 解决 |
|---|------|------|------|
| 1 | **auto.create.topics.enable=true** | 拼写错误的 Topic 被自动创建 | 生产设为 false |
| 2 | **advertised.listeners 配错** | 绑定了 localhost/内网 IP | 配置为客户端可达地址 |
| 3 | **消费积压无视** | 未监控 Consumer Lag | Prometheus + 积压告警 |
| 4 | **分区数 < 消费者数** | 多余 Consumer 闲置 | 分区数 ≥ 最大消费者数 |
| 5 | **磁盘满导致集群不可用** | 没有磁盘监控 | 磁盘告警 + 保留策略 |
| 6 | **单条消息过大** | 默认 1MB 限制 | 调整 message.max.bytes |
| 7 | **频繁 Rebalance** | poll 间隔超时、GC 暂停 | 调整 max.poll.interval.ms |
| 8 | **ZK 元数据膨胀** | 大量 Partition → ZK 压力 | 升级 KRaft |
| 9 | **压缩类型不一致** | Producer/Broker 压缩类型不同 | 统一使用同一种压缩 |
| 10 | **未限制 topics 删除** | 手误删了生产 Topic | ACL + 操作审批 |

### 6.2 容量规划公式

```text
磁盘容量 = 消息速率 × 消息大小 × 保留时间 × 副本数 × 1.3（安全系数）

示例：
  日活 1000 万用户，每人每天产生 100 条日志，每条 1KB
  保留 7 天，副本数 3
  
  消息速率 = 10,000,000 × 100 / 86400 ≈ 11,574 条/秒
  日数据量 = 11,574 × 1KB × 86400 ≈ 1 TB/天
  7 天总数据量 = 1TB × 7 × 3（副本）≈ 21 TB
  加上压缩（lz4 约压缩 50%）≈ 10.5 TB
  安全系数 × 1.3 ≈ 14 TB

Broker 数量 = ceil(吞吐 / 单 Broker 吞吐)
  → 单 Broker ≈ 200 MB/s（SASL/SSL 减半）
  → 目标吞吐 800 MB/s → 4 个 Broker（加冗余 5~6 个）
```

### 6.3 Kafka 不适合的场景

| 场景 | 为什么不适合 | 替代方案 |
|------|-------------|----------|
| 延迟 <1ms 的消息 | Kafka 批量+网络 → 最低 2~5ms | ZeroMQ / Chronicle Queue |
| 优先级消息 | 无优先级支持 | RabbitMQ（支持消息优先级） |
| 定时/延迟消息 | 无原生延迟队列 | RocketMQ（18 级延迟）、Scheduler |
| 小数据量业务解耦 | 重运维，牛刀杀鸡 | RabbitMQ |
| 大量小 Topic | Controller 压力大 | 合并 Topic（Key 区分业务） |
| 消息确认后删除 | Kafka 日志追加、不可变 | RabbitMQ |

---

## 7. 高频面试题精选

### Q1: Kafka 为什么这么快？

```text
① 顺序写磁盘 — 比随机内存写还快（无磁头寻道）
② 零拷贝（sendfile） — 数据不经过用户态，DMA 直传
③ 页缓存 — 读写都在内存中，减少磁盘 IO
④ 批量处理 — Producer 批量发送、Consumer 批量拉取
⑤ 分区并行 — Partition 级别并发
⑥ 高效的二进制协议 — 比文本协议（HTTP/AMQP）更节省带宽
```

### Q2: ISR 伸缩是同步还是异步？

```text
ISR 伸缩由 Leader 异步维护。

Follower 加入 ISR：
  → Follower 追上 Leader 的 LEO → Leader 通知 Controller 加入

Follower 踢出 ISR：
  → Follower 超过 replica.lag.time.max.ms（30s）未追上
  → Leader 将其移出 ISR

为什么不是同步？
  → ISR 伸缩不应阻塞生产请求
  → 因此 acks=all 时，min.insync.replicas 决定了最小确认数
```

### Q3: Rebalance 过程是怎样的？

```text
1. FIND_COORDINATOR — Consumer 找到 GroupCoordinator
2. JOIN_GROUP — Consumer 发送 JoinGroup 请求，选举 Leader Consumer
3. SYNC_GROUP — Leader Consumer 制定分区分配方案，Coordinator 下发
4. HEARTBEAT — 心跳保活

触发条件：Consumer 增删、Topic 分区变化、超时未 poll

优化：使用 CooperativeStickyAssignor 渐进式 Rebalance
```

### Q4: Kafka 怎么保证消息不丢失？

```text
生产者：acks=all, retries=MAX, enable.idempotence=true
Broker：replication.factor≥3, min.insync.replicas≥2,
        unclean.leader.election.enable=false
消费者：手动提交位移，先处理后提交
```

### Q5: Kafka 的消息是 Push 还是 Pull？

```text
Kafka 采用 Pull 模式（消费者主动拉取）。

Push 模式的问题：
  → Broker 不知道 Consumer 的处理能力 → 可能压垮 Consumer
  → Consumer 处理慢时会造成消息堆积在 Consumer 端

Pull 模式的优势：
  → Consumer 按自己速率消费（背压天然支持）
  → 批量拉取，吞吐更高

Pull 的问题：
  → 循环空轮询（fetch.min.bytes + fetch.max.wait.ms 优化）
```

### Q6: __consumer_offsets 是什么？

```text
内部 Topic，存储消费组的位移信息

Key:   GroupID + Topic + Partition
Value: 上次提交的 Offset + 元数据

特点：
  ✅ Compact 策略（只保留最新位移）
  ✅ 默认 50 个分区
  ✅ 副本数由 offsets.topic.replication.factor 控制
```

### Q7: ZK 模式 vs KRaft 模式选哪个？

```text
新集群：无脑选 KRaft（3.3+ 稳定）
旧集群：评估迁移（Kafka 提供在线迁移工具）

KRaft 优势：
  ✅ 运维简单（少一套 ZK）
  ✅ Controller 切换快
  ✅ 支持更多分区
  ✅ 单进程部署

ZK 模式遗民：
  → Kafka 3.x 仍支持 ZK，但已 deprecated
  → Kafka 4.0 将彻底移除 ZK 支持
```

### 更多面试题

| # | 问题 | 关键词 |
|---|------|--------|
| 8 | Kafka 消息是绝对有序的吗？ | 分区内有序、跨分区无序 |
| 9 | Kafka 如何实现 Exactly-Once？ | 幂等+事务、isolation.level=read_committed |
| 10 | Kafka 和 RocketMQ 怎么选？ | 吞吐 vs 事务消息、流处理 vs 业务解耦 |
| 11 | 如何监控 Kafka 集群？ | JMX → Prometheus → Grafana |
| 12 | Kafka 分区数规划公式？ | 吞吐量/单分区吞吐，消费者并发度 |
| 13 | HW 与 LEO 的区别？ | HW 消费者可见上限，LEO 下一条写入位置 |
| 14 | Kafka 日志清理机制？ | delete（时间/大小）、compact（Key 去重） |
| 15 | 零拷贝原理？ | sendfile、DMA 拷贝、不经过用户空间 |

---

## 总结：Kafka 学习体系回顾

```text
┌──────────────┬─────────────────────────────────────┐
│  入门级       │  What/Why/QuickStart                 │
│  (01-02)     │  消息演进 / 安装部署 / CLI 操作       │
├──────────────┼─────────────────────────────────────┤
│  原理级       │  Partition / ISR / HW / 存储模型      │
│  (03-05)     │  Producer/Consumer 内部机制            │
├──────────────┼─────────────────────────────────────┤
│  运维级       │  Controller / KRaft / 副本管理         │
│  (06-07)     │  性能调优 / JMX 监控 / 压测            │
├──────────────┼─────────────────────────────────────┤
│  工程级       │  Spring Kafka / 错误处理 / 事务       │
│  (08-09)     │  Streams / Connect / MirrorMaker      │
└──────────────┴─────────────────────────────────────┘

关键是动手 — 搭集群、写代码、压测、看监控。
Kafka 是"理解越深、用得越稳"的系统，别只停留在 API 调用层。
```
