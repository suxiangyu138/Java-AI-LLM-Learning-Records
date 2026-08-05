# 分层存储与Functions
> Tiered Storage（冷数据卸载对象存储）、Pulsar Functions（轻量无服务器计算）、IO 连接器：Pulsar 的存储经济与流处理能力。

---

## 📚 目录

1. [分层存储：为什么](#1-分层存储为什么)
2. [Tiered Storage 机制](#2-tiered-storage-机制)
3. [分层存储工程实践](#3-分层存储工程实践)
4. [Pulsar Functions](#4-pulsar-functions)
5. [Functions 工程实践](#5-functions-工程实践)
6. [IO 连接器与生态](#6-io-连接器与生态)

---

## 1. 分层存储：为什么

### 1.1 存储成本的矛盾

```text
消息平台的存储需求：
  流式语义：消息需保留（可重放）
  保留时间：从小时级到月/年级
  数据量：TB-PB 级（长期保留）

BookKeeper 存储的代价：
  Bookie 磁盘成本高（SSD/复制）
  副本（E:Qw:Qa = 3 副本）
  → 长期保留的成本线性增长

分层存储的解法：
  热数据（近期）：BookKeeper（低延迟）
  冷数据（历史）：对象存储（低成本）
  → 存储成本下降 10-100 倍
```

### 1.2 分层存储的定位

```text
Tiered Storage：
  消息在 BookKeeper 保留一段时间后
  自动卸载（offload）到对象存储（S3/GCS/Azure Blob）

特性：
  消费者无感知（读取透明：自动从 BK/对象存储取）
  游标不变（订阅可消费任何保留期消息）
  卸载策略可配置（时间/大小阈值）

对比 Kafka 分层（2024+ 也在演进）：
  Kafka：分层存储逐步支持（KIP-405 等）
  Pulsar：Tiered Storage 成熟（多年生产验证）
```

> 🎯 核心认知：**分层存储 = "热数据 BookKeeper + 冷数据对象存储"的存储经济**——长期保留的流式语义（重放/审计）不再需要昂贵的 BK 磁盘，对象存储成本低 10-100 倍，且消费者完全无感知。

---

## 2. Tiered Storage 机制

### 2.1 卸载流程

```text
卸载（Offload）：
  BookKeeper 中的 ledger 达到条件（时间/大小）
  → 转移到对象存储（S3/GCS/Azure）

读取路径（消费者）：
  游标定位 → 数据在 BK？→ 直接读
              → 数据已卸载？→ 从对象存储读
  → 对消费者透明（无感知）

元数据：
  已卸载 ledger 的元数据保留（定位对象存储位置）
  → 卸载不影响游标与订阅

配置：
  namespace 级（offload 策略）
  全局（broker 配置）
```

### 2.2 配置示例

```bash
# 配置对象存储（broker.conf）
managedLedgerOffloadDriver=s3
managedLedgerOffloadBucket=my-pulsar-offload
# 或 gcs / azureblob

# 命名空间卸载策略
pulsar-admin namespaces set-offload-policy orders/us-east \
  --driver s3 --bucket my-pulsar-offload

# 卸载阈值（如 10GB 或 7 天后）
pulsar-admin namespaces set-offload-threshold orders/us-east \
  --size 10G --time 7d
```

```text
卸载策略：
  阈值触发：ledger 大小/时间达到 → 自动卸载
  手动：pulsar-admin topics offload（按需）

注意：
  卸载 = 移动（BK 中的数据删除？——可配置保留）
  读取未卸载数据无额外延迟
  读取已卸载数据有对象存储延迟（秒级）
```

### 2.3 读取的性能影响

```text
分层读取的延迟：
  热数据（BK）：毫秒级（正常）
  冷数据（对象存储）：秒级（首次读取）

优化：
  读取缓存（Broker 侧）
  预热（分析任务前）
  游标位置（近期数据在 BK）

适用场景分析：
  实时消费（近期）→ 无影响（BK 中）
  历史重放（审计/分析）→ 可接受（秒级）
  流式重放（从头）→ 大量对象存储读取（规划）
```

---

## 3. 分层存储工程实践

### 3.1 场景与收益

| 场景 | 收益 |
|------|------|
| 长期保留（审计/合规） | 存储成本大降（对象存储） |
| 事件溯源（可重放） | 无限保留（经济可行） |
| 分析回放（历史数据） | 无需迁移（透明读取） |
| 流式重放（新消费者） | 从头消费（游标不变） |

```text
典型配置：
  热窗口：7 天（BK 中，低延迟）
  冷数据：S3（无限保留，低成本）
  成本对比：
    BK：SSD + 3 副本（贵）
    S3：对象存储（便宜 10-100 倍）
```

### 3.2 成本规划

```text
分层存储的成本模型：
  热数据：BK 容量（SSD × 副本）
  冷数据：对象存储容量 + 读取费用
  卸载流量：BK → S3（一次性）

规划要点：
  热窗口大小（BK 容量规划）
  保留总时长（对象存储容量）
  读取模式（对象存储读取费用）

监控：
  已卸载数据量
  冷读频率（对象存储费用信号）
  卸载积压（卸载慢于写入？）
```

### 3.3 与保留策略的配合

```text
分层 vs 保留（配合使用）：
  Retention：消息保留多久（逻辑）
  Offload：BK 数据何时转对象存储（物理）

组合策略：
  保留 1 年 + 7 天后卸载
  → 前 7 天 BK（低延迟）+ 后 355 天 S3（低成本）
  → 全周期可消费（流式语义完整）

注意：
  卸载不影响保留（已卸载消息仍保留在对象存储）
  保留到期 → 对象存储数据删除
```

---

## 4. Pulsar Functions

### 4.1 什么是 Functions

```text
Pulsar Functions：轻量无服务器计算
  消息处理的"内建函数"：
    输入：一个 topic 的消息
    处理：用户函数（Java/Python/Go）
    输出：另一个 topic（或日志/DB）

定位：
  轻量流处理（单条/窗口）
  无需独立流处理集群（对比 Flink/Kafka Streams）

典型用途：
  消息转换（格式/字段）
  过滤/路由（条件分发）
  聚合（窗口计数）
  集成（写 DB/调用 API）
```

### 4.2 Functions 模型

```java
// Java Function 示例（消息转换）
public class ToUpperCase implements Function<String, String> {
    @Override
    public String process(String input, Context ctx) {
        return input.toUpperCase();
    }
}
```

```text
部署方式：
  本地运行（开发）
  集群运行（Pulsar 管理：FNRuntime）
  线程/进程/容器（隔离级别）

特性：
  无状态（可水平扩展）
  自动负载均衡（函数实例分布）
  自动重试（失败消息）
  背压（消费速率控制）

与流处理框架的边界：
  Functions：轻量（单消息/简单窗口）
  Flink/Kafka Streams：重量（复杂状态/流 SQL）
  → 按复杂度选择
```

---

## 5. Functions 工程实践

### 5.1 创建与部署

```bash
# 部署函数（Java jar）
pulsar-admin functions create \
  --name to-upper \
  --jar target/to-upper.jar \
  --inputs persistent://orders/us-east/raw-events \
  --output persistent://orders/us-east/upper-events \
  --classname com.example.ToUpperCase

# 更新/删除
pulsar-admin functions update --name to-upper ...
pulsar-admin functions delete --name to-upper
```

### 5.2 使用场景

| 场景 | 函数示例 |
|------|---------|
| 数据清洗 | 字段校验/标准化 |
| 转换 | JSON → Avro（或反向） |
| 路由 | 按条件分发到不同 topic |
| 富化 | 查询补充数据（DB/API） |
| 告警 | 阈值检测 → 告警 topic |
| 聚合 | 窗口计数（1 分钟窗口） |

```text
工程注意：
  函数无状态 → 状态放外部（Redis/DB）
  幂等设计（消息重投）
  错误处理（死信/重试）
  资源（并行度/内存配置）

对比 Kafka Streams：
  Pulsar Functions：简单函数（无状态优先）
  Kafka Streams：状态流处理（KTable/窗口）
  → 复杂流处理用 Kafka Streams/Flink（Pulsar 可对接）
```

### 5.3 Functions 与生态

```text
Functions 的生态定位：
  轻量处理（内建，无需额外集群）
  与 Connectors 配合（IO 集成）
  与分层存储配合（历史数据再处理）

2026 状态：
  Functions 成熟稳定（生产验证）
  复杂流处理 → 对接 Flink（Pulsar 的 Flink 连接器）
  → Functions 负责"轻"，Flink 负责"重"
```

---

## 6. IO 连接器与生态

### 6.1 连接器（Connectors）

```text
Pulsar IO：消息与外部系统的桥接
  两类：
    Source（输入）：外部 → Pulsar
    Sink（输出）：Pulsar → 外部

内置连接器（2025-2026）：
  数据库：JDBC、Cassandra、MongoDB、ClickHouse
  存储：S3、HDFS
  消息：Kafka（Kafka 协议兼容）
  流处理：Flink、Spark
  其他：Elasticsearch、Redis

示例（Sink 到 S3）：
  pulsar-admin sinks create \
    --sink-type cloud-storage \
    --inputs persistent://orders/us-east/events \
    --sink-config '{"provider":"s3","bucket":"events"}'
```

### 6.2 生态演进

```text
2025-2026 生态变化（版本相关）：
  v4.2 移除：Flume、Twitter IO 连接器（维护成本）
  → 生态聚焦（主流连接器持续）

协议兼容：
  Kafka 协议兼容（KOP：Kafka-on-Pulsar）
    → Kafka 客户端可直接连 Pulsar（迁移路径）
  MQTT（IoT）
  AMQP

生态定位：
  Pulsar 的"连接器生态" < Kafka Connect（规模）
  但核心集成齐全（DB/存储/流处理）
  → 大数据生态主流对接可用
```

### 6.3 与消息队列理论体系的呼应

```text
本模块与《消息队列理论与实战》的关系：
  分层存储 → 理论 05 模块（延迟/死信之外的成本维度）
  Functions → 理论 08 模块（生态与选型）
  连接器 → 理论 01 模块（消息队列的分类与集成）

Pulsar 的特色组合：
  多租户（04）＋ 跨地域（05）＋ 分层存储 ＋ Functions
  = "云原生消息平台"的完整能力栈
```

> 🎯 **核心要点**：Pulsar 的存储与计算扩展能力 = 分层存储（BookKeeper 热 + 对象存储冷，成本降 10-100 倍）+ Functions（轻量无服务器处理）+ IO 连接器（外部系统桥接）。工程三要点：**热窗口规划（BK 容量）、冷读模式监控（对象存储费用）、Functions 保持无状态幂等**。复杂流处理对接 Flink（Functions 管轻、Flink 管重）。

---

## 7. 小结

| 问题 | 答案 |
|------|------|
| 分层存储解决什么？ | 长期保留成本（BK 热 + S3 冷） |
| 卸载机制？ | ledger 达阈值 → 对象存储（消费者无感知） |
| 冷读延迟？ | 秒级（热数据毫秒级无影响） |
| Functions 是什么？ | 轻量无服务器消息处理（转换/过滤/聚合） |
| 连接器？ | Source/Sink（DB/存储/流处理） |
| 复杂流处理？ | 对接 Flink（Functions 管轻） |

**下一模块**：[07-Java客户端与工程实践](07-Java客户端与工程实践.md)　**返回总览**：[00-Pulsar知识体系总览](00-Pulsar知识体系总览.md)
