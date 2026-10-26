# HBase 知识体系总览

> HBase 是建立在 HDFS 之上的分布式列式存储，用 LSM-Tree 把"海量稀疏数据的随机读写"做成了可水平扩展的在线服务。本体系从数据模型出发，走通架构、存储、读写、RowKey 设计、部署运维、Java 集成到选型面试的完整链路，定位是"会用它、会调它、能判断什么时候不该用它"。

---

## 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)
5. [版本基准与时效说明](#5-版本基准与时效说明)
6. [与其他目录的关系](#6-与其他目录的关系)

---

## 1. 知识体系导图

```text
HBase 知识体系（11 篇）
│
├── 入门与模型（01）
│   └── 01-HBase定位与数据模型.md
│       定位边界 / 表-行-列族-列-单元格 / 稀疏存储 / 多版本 / 与关系模型对照
│
├── 原理篇（02-04）
│   ├── 02-架构原理与组件协作.md
│   │   HMaster / RegionServer / ZooKeeper / HDFS / Region 分裂与迁移
│   ├── 03-数据存储与LSM-Tree.md
│   │   WAL / MemStore / HFile / Bloom Filter / Compaction 三层合并
│   └── 04-读写流程与缓存机制.md
│       写路径 / 读路径 / BlockCache 两级 / 短路读 / 一致性模型
│
├── 设计篇（05）
│   └── 05-RowKey与Schema设计.md
│       RowKey 长度与散列 / 热点规避 / 预分区 / 列族设计 / 版本与 TTL
│
├── 运维篇（06-07）
│   ├── 06-集群部署与运维.md
│   │   三种部署形态 / 配置基线 / 启停 / 监控指标 / 扩容缩容
│   └── 07-性能调优与故障排查.md
│       JVM 与 GC / MemStore 与 BlockCache 配比 / Compaction 调优 / RIT 与常见故障
│
├── 开发篇（08-09）
│   ├── 08-Java客户端与Spring集成.md
│   │   Connection / Table / BufferedMutator / Scan 与 Filter / 协处理器 / Spring Boot
│   └── 09-生态集成与数据迁移.md
│       Phoenix / Hive / Spark / Flink / Kafka / BulkLoad / 跨集群复制
│
└── 选型与面试（10）
    └── 10-选型对比与面试题.md
        vs Cassandra/MongoDB/Redis/ClickHouse/ES / CAP 定位 / 12 道面试题
```

---

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|---------|---------|
| 00 | 知识体系总览 | 全景地图、学习路线、概念速查 | 全部 |
| 01 | 定位与数据模型 | 解决什么问题、五层数据模型、稀疏与多版本 | 初学者 |
| 02 | 架构原理与组件协作 | 四大组件职责、Region 生命周期、故障转移 | 中级 |
| 03 | 数据存储与 LSM-Tree | WAL/MemStore/HFile、Compaction、布隆过滤器 | 中级 |
| 04 | 读写流程与缓存机制 | 写路径七步、读路径五步、BlockCache、一致性 | 中高级 |
| 05 | RowKey 与 Schema 设计 | 热点、散列、预分区、列族、TTL | 中高级 |
| 06 | 集群部署与运维 | 部署形态、配置、监控、扩缩容 | 运维/后端 |
| 07 | 性能调优与故障排查 | GC、内存配比、Compaction、RIT | 高级 |
| 08 | Java 客户端与 Spring 集成 | 客户端 API、批量写、过滤器、协处理器 | 后端开发 |
| 09 | 生态集成与数据迁移 | Phoenix、Spark、Flink、BulkLoad、复制 | 数据开发 |
| 10 | 选型对比与面试题 | 横向对比、CAP、答题模板 | 面试冲刺 |

---

## 3. 学习路线推荐

### 3.1 后端开发路线（求职主线，建议 8-12 小时）

```text
01 定位与数据模型
  → 02 架构原理
  → 04 读写流程（先建立"写 WAL、读缓存"的直觉）
  → 05 RowKey 设计（面试与实战最高频）
  → 08 Java 客户端
  → 10 选型与面试题
```

这条路线跳过部署与调优细节，目标是"能讲清楚 HBase 为什么快、怎么设计表、Java 里怎么写"。

### 3.2 运维与 SRE 路线

```text
01 → 02 → 03 存储结构 → 06 部署运维 → 07 调优排障 → 09 生态集成
```

重点是 Region 分布、Compaction、GC、RIT 处理与容量规划。

### 3.3 数据平台路线

```text
01 → 05 RowKey 设计 → 09 生态集成（BulkLoad/Spark/Flink/Phoenix）→ 07 调优
```

重点是批量导入、离线分析与实时写入链路的配合。

---

## 4. 核心概念速查

| 概念 | 一句话解释 | 关键数字/边界 |
|------|-----------|--------------|
| RowKey | 行的唯一标识，按字典序排序 | 建议 10-100 字节，最长 64KB |
| Column Family | 列的组，物理存储单元 | 建议 1-3 个，过多导致文件句柄膨胀 |
| Column Qualifier | 列族下的列名，动态可加 | 无需预定义 schema |
| Cell | 行、列族、列、版本号定位的值 | 多版本按时间戳倒序 |
| Region | 表的水平分片，调度的基本单位 | 建议 10-50GB |
| RegionServer | 托管 Region 的进程 | 单机 20-200 个 Region |
| HMaster | 管理元数据、Region 分配、负载均衡 | 可 HA，不承载读写 |
| ZooKeeper | 集群协调、meta 表位置 | 3 或 5 节点 |
| WAL | 预写日志，保证宕机不丢数据 | 可同步/异步刷盘 |
| MemStore | 内存写缓冲，按列族独立 | 默认 128MB，达阈值刷 HFile |
| HFile | HDFS 上的有序存储文件 | 不可变，Compaction 合并 |
| BlockCache | 读缓存，LRU 或 BucketCache | 与 MemStore 争堆内存 |
| Compaction | 合并 HFile、清理多版本与删除标记 | Minor 小合并、Major 全合并 |
| Bloom Filter | 判断"行/列是否一定不存在" | ROW 或 ROWCOL 两级 |
| Split | Region 分裂 | 按行数或大小触发 |
| RIT | Region In Transition，迁移中状态 | 长时间不结束即为故障 |

> **核心要点**：HBase 的性能上限由三件事决定——RowKey 是否打散、列族是否克制、读写是否命中缓存。架构与调优的所有手段，最终都在服务这三个目标。

---

## 5. 版本基准与时效说明

| 项目 | 2026-10 现状 |
|------|-------------|
| 最新稳定版 | HBase 3.0.0，2026-08-05 正式 GA |
| 维护中的 2.x | 2.5.15、2.6.6（2026-06-09） |
| 3.0 关键变化 | 强制 JDK 17（不再支持 JDK 8/11）；Hadoop 3 基线；清理废弃 API；异步客户端成熟 |
| ZooKeeper | 3.8/3.9 系列，HBase 3.0 仍依赖 ZK 做协调 |
| 客户端 | hbase-client 3.0.0，Java 侧推荐 Connection 单例 + 异步 Table |
| 生态 | Phoenix 5.2/5.3、Hive 4.x、Spark 4.x、Flink 2.x 均提供 HBase connector |

> **提示**：本体系所有 API 与配置以 HBase 3.0.0 为准；标注 2.x 仍适用的内容会单独说明。3.0 升级前必须确认 JDK 17、Hadoop 3、下游 connector 三者的兼容矩阵。

---

## 6. 与其他目录的关系

| 相关体系 | 关系 |
|---------|------|
| [Redis](../Redis/) | Redis 是内存 KV 缓存，HBase 是磁盘级海量列存；前者拼延迟，后者拼容量与随机读写 |
| [MongoDB](../MongoDB/) | MongoDB 文档模型适合灵活结构，HBase 适合超宽表与稀疏数据；两者都走 LSM 但查询能力不同 |
| [Neo4j](../Neo4j/) | 图关系查询用 Neo4j，HBase 不擅长多跳关系 |
| [Elasticsearch](../../04-ELK/Elasticsearch/) | ES 做全文检索与聚合，HBase 做点查与范围扫描；常见组合是 HBase 存明细、ES 存索引 |
| [Kafka](../../03-消息队列/Kafka/) | Kafka 做实时管道，HBase 做落地存储；Flink 消费 Kafka 写 HBase 是标准实时链路 |
| [分布式系统理论](../../08-分布式系统与架构/分布式系统理论/) | HBase 是 CAP 中偏 CP 的典型：Region 级强一致，跨 Region 无事务 |
| [MySQL](../../01-关系型数据库/MySQL/) | MySQL 承担交易型强事务，HBase 承担海量明细与离线分析 |

---

**下一模块**：[01-HBase定位与数据模型](01-HBase定位与数据模型.md)　**返回总览**：[00-HBase知识体系总览](00-HBase知识体系总览.md)
