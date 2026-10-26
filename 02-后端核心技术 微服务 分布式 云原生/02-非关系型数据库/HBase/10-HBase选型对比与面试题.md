# 10-HBase 选型对比与面试题

> 选型不是"哪个技术更先进"，而是"哪个技术的约束刚好匹配业务"。HBase 的约束是：只能按 RowKey 查询、没有跨行事务、没有二级索引。匹配就非常合适，不匹配就非常痛苦。本篇给出横向对比、决策树与 12 道面试题的标准答法。

---

## 目录

1. [选型决策树](#1-选型决策树)
2. [与 Cassandra 对比](#2-与-cassandra-对比)
3. [与 MongoDB 对比](#3-与-mongodb-对比)
4. [与 Redis 对比](#4-与-redis-对比)
5. [与 ClickHouse 对比](#5-与-clickhouse-对比)
6. [与 Elasticsearch 对比](#6-与-elasticsearch-对比)
7. [与 MySQL 分库分表对比](#7-与-mysql-分库分表对比)
8. [CAP 与一致性定位](#8-cap-与一致性定位)
9. [成本模型](#9-成本模型)
10. [面试题 12 道](#10-面试题-12-道)
11. [答题模板](#11-答题模板)
12. [小结](#12-小结)

---

## 1. 选型决策树

```text
你的查询能否用 RowKey 表达？
├── 不能（多字段任意组合、全文检索）
│   ├── 需要全文/多条件检索 → Elasticsearch
│   ├── 需要 OLAP 聚合 → ClickHouse / Doris
│   └── 需要事务 → MySQL
│
├── 能，但数据量 < 1TB
│   ├── 需要事务 → MySQL
│   └── 灵活文档结构 → MongoDB
│
└── 能，且数据量 > 1TB / 写入吞吐高
    ├── 需要强事务 → MySQL 分库分表（成本高）
    ├── 需要多数据中心写入 → Cassandra
    ├── 需要超低延迟缓存 → Redis
    └── 需要海量随机读写 + 水平扩展 → HBase
```

三个判断问题：

```text
问题一：数据量和写入速率是否超过单机关系库的舒适区？
问题二：最高频查询是否能编码成 RowKey 前缀？
问题三：是否能接受无跨行事务、无二级索引、无 JOIN？
```

三个都答"是"，HBase 是合理选择；有一个答"否"，先看替代方案。

---

## 2. 与 Cassandra 对比

| 维度 | HBase | Cassandra |
|------|-------|-----------|
| 架构 | Master + RegionServer + ZK + HDFS | 无中心，P2P，每个节点对等 |
| 一致性 | CP（Region 级强一致） | 可调（ONE/QUORUM/ALL） |
| 多数据中心 | 复制异步，双活需设计 | 原生多 DC，写入本地 |
| 写入 | 依赖 HDFS 与 ZK | 本地磁盘，无外部依赖 |
| 运维复杂度 | 高（HDFS + ZK + HBase） | 中（无外部依赖） |
| 生态 | Hadoop 生态紧密 | CQL、驱动丰富 |
| 适合 | Hadoop 体系内的海量存储 | 多地域、高可用写入、去中心化 |

```text
一句话：
  HBase 背靠 Hadoop，适合已有 HDFS 体系的离线+在线混合场景
  Cassandra 无中心、多 DC 原生，适合全球化部署与高可用写入
```

---

## 3. 与 MongoDB 对比

| 维度 | HBase | MongoDB |
|------|-------|---------|
| 数据模型 | 列族，稀疏宽表 | 文档（BSON），嵌套结构 |
| 查询 | RowKey 点查、前缀扫描 | 任意字段索引、聚合管道、全文 |
| 事务 | 单行 | 多文档事务（4.0+） |
| 二级索引 | 无（需自建/Phoenix） | 丰富（单字段、复合、文本、地理） |
| 扩展 | Region 自动分裂 | 分片集群 |
| 适合 | 超大规模、稀疏、写多读少 | 灵活结构、中等规模、查询多样 |

```text
一句话：
  MongoDB 是"通用文档数据库"，查询灵活但单机/分片规模有限
  HBase 是"海量列存"，规模极大但查询能力受限
```

---

## 4. 与 Redis 对比

| 维度 | HBase | Redis |
|------|-------|-------|
| 存储介质 | 磁盘（HDFS） | 内存 |
| 延迟 | 毫秒级 | 亚毫秒级 |
| 容量 | PB 级 | 受内存限制（通常 < 1TB） |
| 数据结构 | 列族 | String/List/Hash/Set/ZSet/Stream |
| 持久化 | HDFS 多副本 | RDB/AOF |
| 定位 | 海量明细存储 | 缓存、会话、计数、队列 |

```text
常见组合：
  Redis 做热点缓存与计数 → HBase 做明细落地
  读：先 Redis，未命中查 HBase，回填
  写：写 HBase，失效 Redis（或异步更新）
```

---

## 5. 与 ClickHouse 对比

| 维度 | HBase | ClickHouse |
|------|-------|-----------|
| 定位 | OLTP 点查 + 明细存储 | OLAP 列式分析 |
| 写入 | 随机写、单行更新 | 批量写、少更新 |
| 查询 | RowKey 点查、范围扫描 | 全表/分区扫描 + 聚合 |
| 聚合 | 无 | 极强（向量化、预聚合） |
| 更新/删除 | 支持（版本/墓碑） | 弱（Mutation 代价高） |
| 适合 | 在线服务、明细查询 | 报表、即席分析、日志分析 |

```text
一句话：
  需要"查一行"用 HBase，需要"算一堆"用 ClickHouse
  两者常组合：HBase 服务在线，ClickHouse 服务分析
```

---

## 6. 与 Elasticsearch 对比

| 维度 | HBase | Elasticsearch |
|------|-------|---------------|
| 索引 | 仅 RowKey | 倒排索引，任意字段 |
| 查询 | 点查、前缀扫描 | 全文、多条件、聚合、排序 |
| 写入 | 高吞吐顺序写 | 中等（索引构建开销） |
| 存储成本 | 低（压缩列存） | 高（倒排 + 正排 + 副本） |
| 更新 | 追加版本 | 文档级更新（底层也是重建段） |
| 适合 | 明细主存 | 检索层 |

```text
标准组合：
  HBase = 明细 + 真相源
  ES = 索引 + 检索
  查询：ES 找 ID → HBase 取明细
```

---

## 7. 与 MySQL 分库分表对比

| 维度 | HBase | MySQL 分库分表 |
|------|-------|---------------|
| 事务 | 单行 | 跨库需分布式事务 |
| 查询 | RowKey | SQL，灵活但受分片键限制 |
| 扩展 | 自动分裂、加机器 | 手动扩容、数据迁移 |
| 运维 | 复杂（ZK/HDFS） | 相对成熟，但分片中间件复杂 |
| 数据量 | PB 级 | 十亿级可行，再往上成本高 |
| 适合 | 海量、简单查询 | 强事务、复杂查询、中等规模 |

```text
判断线：
  < 10 亿行且需要事务 → MySQL（必要时分库分表）
  > 10 亿行且查询能用 RowKey 表达 → HBase
```

---

## 8. CAP 与一致性定位

HBase 的 CAP 定位需要精确表述，面试常考：

```text
分区容忍（P）：必须满足（分布式系统前提）

一致性（C）与可用性（A）的取舍：
  HBase 在 Region 级别偏 CP：
    Region 由一台 RS 服务，单行读写强一致
    RS 故障时该 Region 短暂不可用，等待恢复
  但不是全局 CP：
    跨 Region 无事务、无全局快照
    跨集群复制是异步（最终一致）
```

三层一致性：

| 范围 | 一致性 |
|------|--------|
| 单行 | 强一致（写返回即可读） |
| 单 Region 多行 | 无事务，可能部分可见 |
| 跨 Region / 跨集群 | 最终一致 |

> **要点**：说 HBase 是 CP 系统不算错，但必须补一句"Region 级强一致，跨行无事务"。只说 CP 会被追问到答不上来。

---

## 9. 成本模型

选型不能只看功能，还要算三年总成本：

```text
成本 = 硬件 + 运维人力 + 开发适配 + 故障损失

HBase 的成本特点：
  硬件：普通 PC 服务器即可，横向扩展便宜
  运维：需要 HDFS + ZK + HBase 三套技能，人力成本高
  开发：RowKey 设计、二级索引自建、无 SQL，开发成本高
  故障：恢复流程复杂（WAL Split、hbck2）

MySQL 分库分表的成本特点：
  硬件：中高配机器，垂直扩展贵
  运维：中间件 + 多实例，复杂度也高
  开发：SQL 熟悉，成本低
  故障：单库故障影响面可控

结论：
  数据量没到 HBase 的必要门槛时，MySQL 的总成本更低
  HBase 的规模效应在 PB 级才明显
```

---

## 10. 面试题 12 道

### 题 1：HBase 的读写流程

**详细版**

写：客户端按 RowKey 查路由缓存，未命中则查 `hbase:meta`；请求到 RegionServer 后获取行锁，先追加 WAL 并同步到 HDFS，再写入 MemStore 的跳表；MemStore 达到阈值后冻结，后台刷成 HFile；返回客户端成功。

读：先查 MemStore（最新数据），再查 BlockCache，未命中则按 HFile 从新到旧查：Bloom Filter 判断是否存在、块索引定位 Data Block、块内二分查找 KeyValue；最后按时间戳合并多版本，返回最新。

**简短版**

写是 WAL → MemStore → HFile，读是 MemStore → BlockCache → Bloom → HFile。核心是"写先落日志再写内存，读先查内存再查磁盘"。

**追问预案**

- 问：为什么先写 WAL 再写 MemStore？答：MemStore 在内存，进程崩溃即丢；WAL 在 HDFS 多副本，可重放恢复。
- 问：读为什么要查多个 HFile？答：LSM 的不可变文件机制，同一 key 的不同版本分散在多个文件，必须合并。
- 问：怎么减少读的文件数？答：Compaction 合并、Bloom Filter 跳过、BlockCache 缓存热点、控制单 Store 文件数。

### 题 2：RowKey 怎么设计

**详细版**

四步：第一，定查询模式，把最高频条件放前缀；第二，控长度，10-100 字节，因为 RowKey 在每个 Cell 中重复存储；第三，防热点，用业务前缀 + 反转时间戳，必要时加盐或哈希；第四，配合预分区，建表时切 16-64 个 Region。时间序列场景的标准解是"实体 ID + Long.MAX_VALUE - 时间戳"，让最新数据排在区间头部。

**简短版**

唯一、短、分布均匀、匹配查询。时间序列用"实体 ID + 反转时间戳"，建表预分区，热点严重时加盐或哈希。

**追问预案**

- 问：加盐后怎么查？答：知道盐值直接查；不知道则要查所有桶（读放大），或用索引表记录。
- 问：为什么不能自增 ID 做 RowKey？答：写入全压最后一个 Region，形成热点。
- 问：RowKey 太长有什么问题？答：每个 Cell 重复存储，存储、索引、缓存同步放大。

### 题 3：HBase 为什么写快

**详细版**

因为它把随机写改成了顺序写。写入先追加 WAL（HDFS 顺序写），再写内存跳表，MemStore 批量刷成 HFile（顺序写），后台 Compaction 合并。整个过程没有 B+ 树的"找页、改页、分裂"随机 IO。代价是读放大（要查多个文件）和 Compaction 开销。

**简短版**

LSM-Tree 把随机写转成顺序写：WAL 顺序追加、MemStore 内存缓冲、批量刷 HFile。写吞吐接近磁盘带宽，代价是读放大与 Compaction。

**追问预案**

- 问：写放大来自哪里？答：WAL、刷写、Compaction 反复重写同一份数据。
- 问：读放大怎么缓解？答：Bloom Filter、块索引、BlockCache、Compaction 减少文件数。
- 问：为什么不用 B+ 树？答：B+ 树随机写对磁盘不友好，海量写入下吞吐上不去。

### 题 4：HBase 的架构

**详细版**

四层：ZooKeeper 存 meta 表位置、做 Master 选举与 RS 注册；HMaster 管 schema、Region 分配、负载均衡，不参与读写；RegionServer 托管 Region，处理读写、MemStore、WAL、Split、Compaction；HDFS 存 HFile 与 WAL，提供三副本。客户端首次经 ZK 找到 meta，之后缓存路由直连 RS。

**简短版**

ZK 管协调，Master 管分配，RegionServer 管读写，HDFS 管存储。客户端直连 RS，Master 不碰数据。

**追问预案**

- 问：Master 挂了能读写吗？答：能，读写不经过 Master；但不能建表、改表、迁移 Region。
- 问：RS 挂了怎么恢复？答：ZK 会话超时 → Master 感知 → WAL Split → Region 重新分配 → 重放 WAL。
- 问：meta 表存什么？答：每个 Region 的 RowKey 区间与所在 RS 地址。

### 题 5：HBase 的 Compaction

**详细版**

分 minor 和 major。Minor 合并部分小文件，控制文件数量、降低读放大，但不清理过期版本与墓碑。Major 合并一个 Store 的所有文件，真正清理多版本、TTL 过期数据与墓碑。Major 开销大，生产通常关闭自动 major，改在低峰期调度。Compaction 与用户写入争抢 HDFS 带宽，追不上会导致文件数超限、写入阻塞。

**简短版**

Minor 合并小文件，Major 全量合并并清理墓碑与过期版本。Major 要错峰调度，否则会和写入抢 IO。

**追问预案**

- 问：删除数据后为什么磁盘不降？答：删除是写墓碑，只有 major compaction 才物理清理。
- 问：文件数超限会怎样？答：`blockingStoreFiles` 触发写入阻塞，put 延迟飙升。
- 问：怎么减少 Compaction 压力？答：控制 Region 大小、减少列族、限流、错峰 major。

### 题 6：HBase 能做什么、不能做什么

**详细版**

能做：海量稀疏数据的随机读写、RowKey 点查、前缀范围扫描、高写吞吐、水平扩展。不能做：跨行事务、JOIN、二级索引、聚合分析、全文检索、小数据量场景。判断标准是"查询能否用 RowKey 表达"。

**简短版**

能做海量 RowKey 点查与前缀扫描；不能做事务、JOIN、二级索引、聚合。查询不能用 RowKey 表达，就不该选 HBase。

**追问预案**

- 问：需要二级索引怎么办？答：索引表、Phoenix 全局索引、或 ES 外部索引。
- 问：需要事务怎么办？答：单行用 CheckAndMutate；跨行用 MySQL 或分布式事务框架。
- 问：需要聚合怎么办？答：Spark/ClickHouse 离线算，结果回写 HBase 或另存。

### 题 7：HBase 的 MemStore 与 BlockCache

**详细版**

MemStore 是每个 Region 每个列族的内存写缓冲，跳表结构，达到阈值刷成 HFile。BlockCache 是读缓存，分 LRU（堆内）、Bucket（堆外）、Combined（默认，索引在堆内、数据在堆外）。两者争抢 RS 内存，配比按读写比例调整：写多 MemStore 50-60%，读多 BlockCache 55-65%。BucketCache 用堆外内存，能减少 GC 压力，但要预留物理内存。

**简短版**

MemStore 管写缓冲，BlockCache 管读缓存。写多给 MemStore 多，读多给 BlockCache 多；堆外 Bucket 降 GC，但要算好物理内存。

**追问预案**

- 问：MemStore 太大会怎样？答：恢复时间长、内存压力大、可能触发 Full GC。
- 问：BlockCache 命中率低怎么办？答：加 BucketCache、调块大小、避免 Scan 污染、冷数据关缓存。
- 问：为什么索引块放堆内？答：访问频繁、体积小，放堆内延迟低。

### 题 8：HBase 的 Region 分裂

**详细版**

Region 按 RowKey 区间切分，达到阈值后由 RegionServer 发起分裂：创建两个子 Region 的目录，父 Region 标记 SPLIT，Master 分配子 Region，后台 Compaction 真正分离数据。分裂策略有 IncreasingToUpperBound、Stepping、ConstantSize、KeyPrefix 等。分裂期间短暂不可写。避免频繁分裂的手段是预分区与合理的 Region 大小（10-30GB）。

**简短版**

Region 按 RowKey 区间切，达到阈值自动分裂成两个，Master 负责分配。分裂有短暂抖动，预分区可以减少运行期分裂。

**追问预案**

- 问：分裂会丢数据吗？答：不会，子 Region 引用父 HFile，后台合并。
- 问：Region 太小有什么问题？答：文件多、管理开销大、Compaction 频繁。
- 问：Region 太大会怎样？答：分裂与恢复时间长，单点影响面大。

### 题 9：HBase 的 WAL

**详细版**

WAL 是预写日志，每个 RegionServer 默认一个，所有 Region 的写入共享。写操作先追加 WAL 再写 MemStore，保证 RS 宕机后内存数据可恢复。同步策略有 SYNC_WAL（默认）、ASYNC_WAL、SKIP_WAL。RS 故障后 Master 执行 WAL Split，按 Region 分组日志片段，重新分配后各 RS 重放。WAL 文件数达到上限会阻塞写入。

**简短版**

WAL 保证宕机不丢数据：先写日志再写内存。故障时按 Region 拆分日志、重放恢复。SKIP_WAL 只用于可重放场景。

**追问预案**

- 问：WAL Split 为什么慢？答：要按 Region 分组、搬运日志、逐个重放。
- 问：异步 WAL 会丢多少？答：取决于刷盘间隔，通常最近若干毫秒到秒级。
- 问：怎么减少 WAL 压力？答：批量写、同机部署、合理 Durability。

### 题 10：HBase 的写热点与读热点

**详细版**

写热点来自 RowKey 单调递增或高度集中，导致写入全压一个 Region。解决：RowKey 加盐/哈希/反转/业务前缀+时间后缀，建表预分区，业务倾斜时拆热点行。读热点来自所有请求查同一批 RowKey，解决：热点数据加 Redis 缓存、拆分热点行、副本读（HBase 原生不支持多副本读，需要业务层分片）。

**简短版**

写热点靠 RowKey 打散 + 预分区；读热点靠缓存 + 拆分热点行。识别看单 RS 的 CPU/IO 与 Region 请求分布。

**追问预案**

- 问：加盐后范围查询怎么办？答：查所有桶再合并，或建索引表。
- 问：预分区数量怎么定？答：数据量 / 单 Region 目标大小，通常 16-64 个。
- 问：热点能彻底消除吗？答：不能，只能缓解；极热实体需要业务拆分。

### 题 11：HBase 与 MySQL 怎么选

**详细版**

看三点：数据量与写入速率、查询是否能用 RowKey 表达、是否需要跨行事务。数据量十亿级以下且需要事务，MySQL 分库分表更合适；超过十亿行、写入吞吐高、查询是点查或前缀扫描，HBase 更合适。还要算总成本：HBase 硬件便宜但运维与开发成本高，MySQL 开发成本低但扩展成本高。

**简短版**

要事务、要 SQL、十亿级以下选 MySQL；海量、RowKey 查询、写吞吐高选 HBase。规模没到门槛时 MySQL 总成本更低。

**追问预案**

- 问：HBase 能替代 MySQL 吗？答：不能，事务与查询能力差太远。
- 问：MySQL 分库分表能到 PB 吗？答：理论可以，但运维与扩容成本极高。
- 问：两者能组合吗？答：常见组合是 MySQL 管交易，HBase 管明细与历史归档。

### 题 12：HBase 生产故障怎么排查

**详细版**

先分层：客户端（超时、重试）、网络、RegionServer（RPC 队列、CPU）、JVM（GC、内存）、MemStore/BlockCache、Compaction、HDFS。常用指标：MemStore 大小、StoreFile 数、Compaction 队列、BlockCache 命中率、RIT、GC 停顿。常见故障：写入阻塞（MemStore 满/文件数超限）、RIT 不结束（分配卡住）、RS 消失（堆外 OOM）、读延迟高（缓存不足/磁盘瓶颈）。工具：HBase UI、jstack、jstat、iostat、hbck2。

**简短版**

先定位层：GC、内存、Compaction、HDFS 还是 Region 分布。看四个指标：MemStore、文件数、Compaction 队列、RIT。再对症处理，不盲目加机器。

**追问预案**

- 问：写入突然变慢，第一步看什么？答：看 RS 日志有没有 Blocking updates，再看 MemStore 与文件数。
- 问：RS 进程消失怎么查？答：dmesg 看 OOM Killer，检查 BucketCache 是否超过物理内存。
- 问：RIT 一直不结束怎么办？答：看 Master 日志的分配异常，用 hbck2 检查，必要时手动 assign。

---

## 11. 答题模板

面试回答 HBase 问题时，用"定位 → 机制 → 取舍 → 场景"四段式：

```text
第一段：一句话定位
  "HBase 是 HDFS 之上的分布式列存，核心能力是 RowKey 点查与海量写入。"

第二段：机制
  "它用 LSM-Tree 把随机写转成顺序写：WAL → MemStore → HFile → Compaction。"

第三段：取舍
  "代价是读放大、Compaction 开销、无跨行事务、无二级索引。"

第四段：场景
  "所以它适合日志、轨迹、画像这类海量且查询能用 RowKey 表达的场合；
   报表用 ClickHouse，检索用 ES，交易用 MySQL。"
```

这个模板的好处是：任何追问都能落回"机制"或"取舍"，不会答成背参数。

---

## 12. 小结

| 对比对象 | 一句话结论 |
|---------|-----------|
| Cassandra | 无中心多 DC vs Hadoop 生态海量存储 |
| MongoDB | 灵活文档查询 vs 超大规模列存 |
| Redis | 内存缓存 vs 磁盘明细 |
| ClickHouse | OLAP 聚合 vs OLTP 点查 |
| Elasticsearch | 检索层 vs 明细层 |
| MySQL 分库分表 | 事务与 SQL vs 海量扩展 |
| CAP | Region 级 CP，跨行无事务 |

**下一模块**：无　**返回总览**：[00-HBase知识体系总览](00-HBase知识体系总览.md)
