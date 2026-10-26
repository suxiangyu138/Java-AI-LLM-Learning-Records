# 09-HBase 生态集成与数据迁移

> HBase 很少单独出现。它通常处在"实时写入链路的下游、离线分析的数据源、检索系统的明细底座"三个位置。本篇讲清楚它与 Phoenix、Hive、Spark、Flink、Kafka、Elasticsearch 的配合方式，以及批量导入、跨集群迁移、异构数据库同步的落地路径。

---

## 目录

1. [生态全景](#1-生态全景)
2. [Phoenix：SQL on HBase](#2-phoenixsql-on-hbase)
3. [Hive on HBase](#3-hive-on-hbase)
4. [Spark 集成](#4-spark-集成)
5. [Flink 集成](#5-flink-集成)
6. [Kafka 实时链路](#6-kafka-实时链路)
7. [Elasticsearch 组合](#7-elasticsearch-组合)
8. [BulkLoad 批量导入](#8-bulkload-批量导入)
9. [异构数据库同步](#9-异构数据库同步)
10. [HBase 集群间迁移](#10-hbase-集群间迁移)
11. [跨集群复制](#11-跨集群复制)
12. [数据湖与维表场景](#12-数据湖与维表场景)
13. [集成避坑清单](#13-集成避坑清单)
14. [小结](#14-小结)

---

## 1. 生态全景

```text
                     ┌──────────────┐
   业务写入 ────────→ │   Kafka      │
                     └──────┬───────┘
                            │
              ┌─────────────┼─────────────┐
              ▼             ▼             ▼
        ┌──────────┐  ┌──────────┐  ┌──────────┐
        │  Flink   │  │  Spark   │  │ 自研消费  │
        │ 实时计算  │  │ 批处理    │  │ 服务      │
        └────┬─────┘  └────┬─────┘  └────┬─────┘
             │             │             │
             └─────────────┼─────────────┘
                           ▼
                    ┌──────────────┐
                    │    HBase     │
                    │  明细/维表    │
                    └──────┬───────┘
                           │
          ┌────────────────┼────────────────┐
          ▼                ▼                ▼
    ┌──────────┐    ┌──────────┐    ┌──────────┐
    │ Phoenix  │    │   Hive   │    │    ES    │
    │ SQL 查询  │    │ 离线分析  │    │ 全文检索  │
    └──────────┘    └──────────┘    └──────────┘
```

三个典型位置：

| 位置 | 角色 | 技术组合 |
|------|------|---------|
| 实时链路下游 | 明细落地、维表查询 | Kafka → Flink → HBase |
| 离线分析数据源 | 明细表、宽表 | Hive/Spark 读 HBase |
| 检索系统底座 | 明细存储 + 索引 | HBase 存明细，ES 存索引 |

---

## 2. Phoenix：SQL on HBase

### 2.1 定位

Phoenix 是 HBase 上的 SQL 层，把 SQL 编译成 HBase 的 Scan/Get，并维护二级索引：

```text
Phoenix 提供：
  ① SQL 语法（DDL/DML/查询）
  ② JDBC 驱动
  ③ 二级索引（全局/本地）
  ④ 聚合、JOIN、子查询（有限支持）
  ⑤ 查询下推（Filter 转服务端）

不提供：
  完整的 OLTP 事务（事务表是有限支持）
  复杂分析（大 JOIN、窗口函数能力有限）
```

### 2.2 建表与索引

```sql
CREATE TABLE IF NOT EXISTS user_profile (
    user_id   VARCHAR PRIMARY KEY,
    info.name VARCHAR,
    info.city VARCHAR,
    tag.sports VARCHAR
);

CREATE INDEX idx_city ON user_profile (info.city);
```

### 2.3 全局索引 vs 本地索引

| 类型 | 存储 | 写开销 | 读性能 | 适用 |
|------|------|--------|--------|------|
| 全局索引 | 独立索引表 | 高（跨 Region 写） | 好 | 读多写少 |
| 本地索引 | 与数据同 Region | 低 | 中（要扫所有 Region 的索引） | 写多读少 |

### 2.4 Phoenix 的代价

```text
① 写放大：索引越多，写入越慢
② 版本耦合：Phoenix 必须与 HBase 版本严格匹配
③ 运维额外组件：Query Server（可选）
④ 复杂 SQL 性能不可控：优化器能力有限
⑤ 事务局限：跨行事务需要特殊表类型，性能与兼容性有代价

结论：
  简单查询 + 二级索引场景可用
  复杂分析不要指望 Phoenix，交给 Spark/ClickHouse
```

---

## 3. Hive on HBase

Hive 通过 `HBaseStorageHandler` 把 HBase 表映射成 Hive 外部表：

```sql
CREATE EXTERNAL TABLE hbase_user_profile (
    user_id STRING,
    name STRING,
    city STRING
)
STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
WITH SERDEPROPERTIES (
    "hbase.columns.mapping" = ":key,info:name,info:city"
)
TBLPROPERTIES (
    "hbase.table.name" = "app:user_profile"
);
```

用途与限制：

```text
用途：
  用 Hive SQL 对 HBase 数据做离线分析
  与数仓其他表 JOIN

限制：
  全表扫描，性能取决于 HBase 集群
  不适合高频、低延迟查询
  复杂类型映射容易出错
  写入 Hive 表会逐行写 HBase，吞吐低

结论：
  只读分析可用，生产分析更推荐 Spark 直读 HBase
```

---

## 4. Spark 集成

Spark 是读取 HBase 做分析与批量导入的主力。

### 4.1 读 HBase

```scala
val conf = HBaseConfiguration.create()
conf.set("hbase.zookeeper.quorum", "zk1,zk2,zk3")
conf.set(TableInputFormat.INPUT_TABLE, "app:user_profile")

val rdd = sc.newAPIHadoopRDD(
  conf,
  classOf[TableInputFormat],
  classOf[org.apache.hadoop.hbase.io.ImmutableBytesWritable],
  classOf[org.apache.hadoop.hbase.client.Result]
)
```

Spark SQL 方式（HBase Spark Connector）：

```scala
val df = spark.read
  .format("org.apache.hbase.spark")
  .option("hbase.table", "app:user_profile")
  .option("hbase.columns.mapping", "user_id STRING :key, name STRING info:name")
  .load()
```

### 4.2 写 HBase

```scala
df.write
  .format("org.apache.hbase.spark")
  .option("hbase.table", "app:user_profile")
  .option("hbase.columns.mapping", "user_id STRING :key, name STRING info:name")
  .save()
```

### 4.3 Spark 读 HBase 的性能要点

```text
① 分区裁剪
   用 TableInputFormat 的 scan 设置 startRow/stopRow
   否则每个 Spark 分区都可能扫全表

② 列裁剪
   只读需要的列族与列，减少 IO

③ 并发度
   Spark 分区数 ≈ Region 数，避免单分区扫多个 Region

④ 短路读
   Spark Executor 与 DataNode 同机时生效

⑤ 不要用 Spark 做点查
   点查用 HBase Client，Spark 适合批量扫描
```

---

## 5. Flink 集成

Flink 是实时写入 HBase 的标准选择。

### 5.1 Flink HBase Connector

```java
StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

Configuration hbaseConf = new Configuration();
hbaseConf.set("hbase.zookeeper.quorum", "zk1,zk2,zk3");

HBaseConnectionOptions connectionOptions = HBaseConnectionOptions.builder()
        .setConfiguration(hbaseConf)
        .build();

HBaseWriteOptions writeOptions = HBaseWriteOptions.builder()
        .setBufferFlushMaxRows(1000)
        .setBufferFlushInterval(Duration.ofSeconds(1))
        .build();

HBaseMutationConverter<OrderEvent> converter = new HBaseMutationConverter<>() {
    @Override
    public void open() {
    }

    @Override
    public Mutation convertToMutation(OrderEvent event) {
        Put put = new Put(Bytes.toBytes(event.getOrderId()));
        put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("amount"),
                Bytes.toBytes(event.getAmount()));
        return put;
    }
};

SinkFunction<OrderEvent> sink = HBaseSinkFunction.<OrderEvent>builder()
        .setTableName("app:order")
        .setConnectionOptions(connectionOptions)
        .setWriteOptions(writeOptions)
        .setMutationConverter(converter)
        .build();

stream.addSink(sink);
```

### 5.2 实时链路设计要点

```text
① 幂等
   Flink 至少一次语义 + HBase put 幂等（同 RowKey 覆盖）
   → 重复消费不会产生重复行

② 批量
   bufferFlushMaxRows / bufferFlushInterval 控制批量
   太小 → 请求多；太大 → 延迟高、故障丢批多

③ 反压
   HBase 写入变慢会反压到 Flink
   需要监控 checkpoint 时长与背压指标

④ 热点
   Kafka 分区键与 HBase RowKey 要一起设计
   避免同一 key 全压一个 Region

⑤ Exactly-Once
   Flink 两阶段提交 + HBase 不支持事务
   → 通常用"幂等写 + 去重表"实现端到端一致性
```

---

## 6. Kafka 实时链路

Kafka → HBase 是"实时明细落地"的标准架构：

```text
生产端 → Kafka（按业务 key 分区）→ 消费服务/Flink → HBase

设计要点：
  ① Kafka 分区键 = HBase RowKey 的散列前缀
     → 同一实体落同一分区、同一 Region，顺序有保证
  ② 消费位点提交在 HBase 写入成功后
     → 至少一次，配合幂等
  ③ 批量写（BufferedMutator 或 Flink sink buffer）
  ④ 失败重试 + 死信队列
  ⑤ 监控消费延迟（lag）与写入延迟
```

不推荐的模式：

```text
Kafka 每条消息直接单条 put
  → RPC 次数爆炸，RS 线程池打满

Kafka → HBase 无重试
  → 网络抖动直接丢数据

Kafka 分区键与 RowKey 无关
  → 写入乱序、热点、重复难去重
```

---

## 7. Elasticsearch 组合

HBase + ES 是"明细 + 检索"的经典组合：

```text
写路径：
  业务 → HBase（明细，RowKey 点查）
       → ES（索引，全文/多条件检索）
  双写方式：
    应用双写（简单，一致性靠业务）
    CDC（Debezium/Flink CDC）同步（解耦，延迟低）
    定时补偿（最终一致兜底）

读路径：
  ① 用户在 ES 检索 → 得到 RowKey 列表
  ② 按 RowKey 批量 get HBase → 返回明细
```

职责划分：

| 系统 | 存什么 | 查询能力 |
|------|--------|---------|
| HBase | 完整明细、大字段、历史版本 | RowKey 点查、前缀扫描 |
| ES | 检索字段、聚合字段 | 全文、多条件、聚合、排序 |

> **提示**：不要让 ES 存全量明细（存储成本高、更新代价大），也不要让 HBase 做检索（无倒排索引）。两者按"索引 vs 明细"分工，是成本与能力的平衡点。

---

## 8. BulkLoad 批量导入

BulkLoad 绕过 WAL 与 MemStore，直接把 HFile 加载进 HBase，是历史数据导入的最快方式：

```text
原理：
  ① 用 MapReduce/Spark 生成 HFile（按 HBase 内部格式排序）
  ② 把 HFile 直接移动到 Region 的目录
  ③ 通知 RegionServer 加载（不经过写路径）

优势：
  不占 WAL、不写 MemStore、不触发 Compaction
  吞吐远高于逐条 put（10 倍以上）
  适合 TB 级历史数据初始化

限制：
  只能新增，不能更新已有行（会覆盖但语义需自控）
  生成 HFile 需要与目标表 schema 完全一致
  导入期间目标表最好停止写入
```

### 8.1 Spark BulkLoad 示例

```scala
val hbaseConf = HBaseConfiguration.create()
hbaseConf.set(TableOutputFormat.OUTPUT_TABLE, "app:order")

val job = Job.getInstance(hbaseConf)
job.setOutputFormatClass(classOf[HFileOutputFormat2])
job.setOutputKeyClass(classOf[ImmutableBytesWritable])
job.setOutputValueClass(classOf[Result])

val table = connection.getTable(TableName.valueOf("app:order"))
HFileOutputFormat2.configureIncrementalLoad(job, table, connection.getRegionLocator(tableName))

// 生成 (ImmutableBytesWritable, KeyValue) 并写入
```

### 8.2 导入后校验

```text
① 行数对比（源 vs HBase）
② 抽样字段比对
③ Region 分布是否均匀
④ 文件数是否过多（需要 compact）
```

---

## 9. 异构数据库同步

### 9.1 工具选型

| 工具 | 状态 | 适用 |
|------|------|------|
| Sqoop | 2021 年退役（Apache Attic） | 不建议新项目使用 |
| DataX | 阿里开源，活跃 | 离线批量同步 |
| SeaTunnel | Apache 顶级，活跃 | 批流一体同步 |
| Flink CDC | 活跃 | 实时增量同步 |
| Debezium | 活跃 | CDC 采集 |

### 9.2 MySQL → HBase

```text
离线全量：
  DataX / SeaTunnel 读 MySQL → 写 HBase
  大表用 BulkLoad

实时增量：
  Flink CDC 读 binlog → 转换 → 写 HBase

RowKey 设计：
  业务主键 + 必要维度
  避免自增 ID 直接做 RowKey（热点）

字段映射：
  MySQL 的 NULL → HBase 不写该列（稀疏）
  时间类型 → long 毫秒
  金额 → 字符串或 long 分
```

### 9.3 一致性保障

```text
① 全量 + 增量衔接
   先全量导入，记录位点/时间戳，再从该点增量

② 幂等
   RowKey 唯一 + put 覆盖 → 重复同步不产生重复行

③ 对账
   定时对比源表与 HBase 的行数、关键字段校验和

④ 延迟监控
   CDC 位点延迟、写入延迟
```

---

## 10. HBase 集群间迁移

### 10.1 方式对比

| 方式 | 停机 | 数据量 | 适用 |
|------|------|--------|------|
| Snapshot + ExportSnapshot | 短 | 大 | 同版本集群迁移 |
| CopyTable | 无（但影响性能） | 中 | 表级复制 |
| Export/Import | 无 | 中 | 跨版本迁移 |
| Replication | 无 | 大 | 长期双活/容灾 |
| BulkLoad | 短 | 大 | 目标集群初始化 |

### 10.2 Snapshot 迁移步骤

```text
① 源集群建快照
   snapshot 'app:user_profile', 'migrate_20261026'

② 导出快照到目标集群 HDFS
   hbase org.apache.hadoop.hbase.snapshot.ExportSnapshot \
     -snapshot migrate_20261026 \
     -copy-to hdfs://target-cluster/hbase \
     -mappers 20

③ 目标集群恢复
   disable 'app:user_profile'
   restore_snapshot 'migrate_20261026'
   enable 'app:user_profile'

④ 校验
   行数、抽样、Region 分布
```

### 10.3 跨版本迁移

```text
2.x → 3.0 迁移注意：
  ① 先升到 2.6.x，再升 3.0
  ② JDK 17 环境准备
  ③ 客户端与 Coprocessor 重新编译
  ④ 用 Export/Import 而不是直接搬 HFile（HFile 格式可能不兼容）
  ⑤ 迁移后跑 hbck2 校验
```

---

## 11. 跨集群复制

Replication 基于 WAL 异步复制，适合容灾与双活：

```text
原理：
  源集群 RS 把 WAL 条目推送到目标集群
  目标集群重放到对应表

配置步骤：
  ① 源集群 hbase-site.xml 开启 replication
  ② 目标集群建同名表（结构一致）
  ③ 源集群添加 peer：
     add_peer 'peer1', 'zk1,zk2,zk3:/hbase'
  ④ 对表开启复制：
     alter 'app:user_profile', {NAME => 'info', REPLICATION_SCOPE => '1'}

监控：
  replication 延迟（WAL 堆积）
  复制失败日志

限制：
  异步，RPO 秒级
  不支持跨版本大跨度
  冲突解决需要业务设计（双写场景）
```

---

## 12. 数据湖与维表场景

### 12.1 HBase 作为维表

```text
Flink 实时 JOIN HBase 维表：
  主流方式：Lookup Join（每个事件查一次 HBase）
  优化：
    本地缓存（Guava/Caffeine）+ TTL
    异步查询（Async I/O）
    批量查询（按 key 聚合后批量 get）

注意：
  维表 QPS 高时，HBase 点查压力大
  → 加 Redis 缓存层，或把维表同步到 Redis
```

### 12.2 HBase 与 Iceberg/Hudi

```text
HBase 不是数据湖格式，不能替代 Iceberg/Hudi
分工：
  HBase：在线点查、随机读写、低延迟
  Iceberg/Hudi：离线批量分析、ACID 表、时间旅行

组合：
  CDC → HBase（在线服务）
      → Iceberg（离线分析）
  两份数据，各自服务不同场景
```

---

## 13. 集成避坑清单

| 问题 | 后果 | 处理 |
|------|------|------|
| Phoenix 与 HBase 版本不匹配 | 启动失败/查询异常 | 严格按兼容矩阵选版本 |
| Hive 映射列错位 | 数据读错列 | 用 `:key` 显式标记 RowKey |
| Spark 未做分区裁剪 | 每个分区扫全表 | 设置 startRow/stopRow |
| Flink 每条写一次 | RPC 爆炸 | 开 buffer，批量 flush |
| Kafka 分区键与 RowKey 不一致 | 乱序、热点 | 统一散列策略 |
| 双写无补偿 | HBase 与 ES 不一致 | CDC 或定时对账 |
| BulkLoad 后未 compact | 文件数过多 | 导入后触发 compaction |
| 迁移直接拷 HFile | 跨版本失败 | 用 snapshot/export |
| 复制未开 REPLICATION_SCOPE | 数据不同步 | 逐列族开启 |
| 维表查询无缓存 | HBase 被打爆 | 本地缓存 + 异步批量 |

---

## 14. 小结

| 场景 | 推荐方案 |
|------|---------|
| SQL 查询 + 二级索引 | Phoenix（注意版本匹配） |
| 离线分析 | Spark 直读 HBase |
| 实时写入 | Flink HBase Connector + 批量 |
| 实时管道 | Kafka → Flink → HBase |
| 全文检索 | HBase 存明细 + ES 存索引 |
| 历史数据初始化 | BulkLoad |
| 异构同步 | Flink CDC / SeaTunnel / DataX |
| 集群迁移 | Snapshot + ExportSnapshot |
| 容灾双活 | Replication |
| 维表 JOIN | Lookup Join + 本地缓存 |

**下一模块**：[10-HBase选型对比与面试题](10-HBase选型对比与面试题.md)　**返回总览**：[00-HBase知识体系总览](00-HBase知识体系总览.md)
