# 02-HBase 架构原理与组件协作

> HBase 的架构可以压缩成一句话：ZooKeeper 管"谁在哪"，HMaster 管"怎么分"，RegionServer 管"读写"，HDFS 管"存"。客户端不走 HMaster，而是拿到路由表后直连 RegionServer。理解这条路由链路，就理解了 HBase 为什么能线性扩展。

---

## 目录

1. [整体架构](#1-整体架构)
2. [客户端 Client](#2-客户端-client)
3. [ZooKeeper 的角色](#3-zookeeper-的角色)
4. [HMaster 的职责](#4-hmaster-的职责)
5. [RegionServer 的职责](#5-regionserver-的职责)
6. [HDFS 的角色](#6-hdfs-的角色)
7. [客户端寻址全链路](#7-客户端寻址全链路)
8. [Region 生命周期](#8-region-生命周期)
9. [Region 分裂策略](#9-region-分裂策略)
10. [负载均衡与故障转移](#10-负载均衡与故障转移)
11. [HBase 3.0 架构层面变化](#11-hbase-30-架构层面变化)
12. [小结](#12-小结)

---

## 1. 整体架构

```text
                    ┌──────────────────────────────┐
                    │          Client              │
                    │  连接池 / 路由缓存 / 重试      │
                    └──────────────┬───────────────┘
                                   │
              ┌────────────────────┼────────────────────┐
              │ 首次寻址            │ 读写直连             │ 管理操作
              ▼                    ▼                    ▼
    ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
    │   ZooKeeper      │  │  RegionServer    │  │   HMaster        │
    │  /hbase/meta-region-server              │  │  schema / 分配    │
    │  master 选举      │  │  ┌────────────┐  │  │  负载均衡 / 分裂  │
    │  RS 注册与心跳    │  │  │  Region    │  │  │  Region 迁移      │
    └──────────────────┘  │  │  MemStore  │  │  └────────┬─────────┘
                          │  │  HLog(WAL) │  │           │
                          │  └────────────┘  │           │
                          └────────┬─────────┘           │
                                   │                     │
                    ┌──────────────▼─────────────────────▼─────────┐
                    │                  HDFS                        │
                    │   HFile 三副本 / WAL 持久化 / 机架感知          │
                    └──────────────────────────────────────────────┘
```

四条重要事实：

1. **HMaster 不参与数据读写**。它是控制面，不是数据面。HMaster 挂掉后，已存在的表仍可正常读写（只是不能建表、改表、分裂迁移）。
2. **客户端直连 RegionServer**。首次通过 ZooKeeper 找到 `hbase:meta` 表，之后缓存路由，后续读写不再经过 ZK。
3. **数据副本由 HDFS 提供**。RegionServer 之间不做数据复制，一台 RS 挂掉，数据仍在 HDFS 上，靠 WAL 回放恢复内存中的数据。
4. **ZooKeeper 是外部依赖**。HBase 3.0 仍未内置 Raft 元数据层，协调工作继续由 ZooKeeper 承担。

---

## 2. 客户端 Client

客户端不是"薄客户端"，它承担了大量工作：

```text
客户端职责：
  ① 维护 Connection（重量级，进程内单例）
  ② 缓存 Region 路由（table → RegionServer 映射）
  ③ 定位 meta 表（首次经 ZooKeeper）
  ④ 请求重试与故障切换（Region 迁移、RS 宕机）
  ⑤ 批量写缓冲（BufferedMutator）
  ⑥ 扫描的分批拉取（Scan 每次返回一批，客户端续拉）
```

客户端缓存的关键是"路由表"：

```text
缓存条目：RowKey 区间 → RegionServer 地址
失效时机：
  - 请求返回 NotServingRegionException（Region 已迁移）
  - 请求返回 RegionMovedException
  - meta 表变更
处理方式：
  - 清除本地缓存 → 重新查 meta → 重试（有上限）
```

> **要点**：`Connection` 是重量级对象，内部持有线程池与连接池，必须进程内复用。每次请求 `ConnectionFactory.createConnection()` 是典型的性能事故。

---

## 3. ZooKeeper 的角色

ZooKeeper 在 HBase 里只做协调，不存业务数据：

| 作用 | 说明 |
|------|------|
| 记录 meta 表位置 | `/hbase/meta-region-server` 节点存放 `hbase:meta` 所在 RegionServer |
| Master 选举 | 多个 HMaster 抢 `/hbase/master` 临时节点，抢到者为 Active |
| RegionServer 注册 | 每台 RS 在 `/hbase/rs` 下创建临时节点，宕机后会话超时自动消失 |
| 集群状态 | `/hbase/replication`、`/hbase/table`、`/hbase/rsgroup` 等 |
| 分布式锁 | Region 分配、表锁等用 ZK 临时节点实现 |

ZooKeeper 集群规模：

```text
生产建议：3 节点（小集群）或 5 节点（大集群）
  必须奇数，容忍 (n-1)/2 台故障
  与 HBase 集群共用 ZK 时必须使用独立 znode 根目录

关键参数：
  zookeeper.session.timeout：默认 90000ms，RS 心跳超时判定
  hbase.zookeeper.quorum：ZK 地址列表
  zookeeper.znode.parent：默认 /hbase
```

> **警告**：ZK 会话超时设置过小会导致"假死"误判（GC 停顿超过超时 → RS 被踢出集群 → Region 迁移风暴）。大堆 RegionServer 建议把 `zookeeper.session.timeout` 调到 120000-180000ms，并配合 G1 GC 控制停顿。

---

## 4. HMaster 的职责

HMaster 是集群的"控制面"，负责元数据与调度，不处理用户读写：

```text
HMaster 职责清单：
  ① 建表 / 删表 / 改表（schema 变更）
  ② Region 分配（启动时、RS 故障后）
  ③ Region 迁移与负载均衡
  ④ Region 分裂的协调（RS 发起，Master 记录）
  ⑤ 权限管理（ACL）
  ⑥ 命名空间与配额管理
  ⑦ 清理无效的 WAL 与 HFile
  ⑧ 处理 RegionServer 上下线
```

HMaster 高可用：

```text
部署多个 HMaster 实例 → ZK 选举 → 一个 Active，其余 Backup
Active 宕机 → Backup 在秒级接管
Backup 不提供服务，但保持与 ZK 的会话

配置：
  hbase.master.info.port：Web UI 端口（默认 16010）
  hbase.master.port：RPC 端口（默认 16000）
```

HMaster 不是单点故障的关键原因：读写路径不经过它。生产上仍然建议部署 2 个 HMaster，因为控制面长时间不可用会导致 Region 无法恢复、无法建表、无法迁移。

---

## 5. RegionServer 的职责

RegionServer 是数据面，一台机器一个 RS 进程，托管多个 Region：

```text
RegionServer 内部结构（单个 RS）：

  ┌─────────────────────────────────────────────────────┐
  │ RegionServer                                         │
  │                                                      │
  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │
  │  │  Region 1   │  │  Region 2   │  │  Region N   │  │
  │  │  MemStore   │  │  MemStore   │  │  MemStore   │  │
  │  │  StoreFile  │  │  StoreFile  │  │  StoreFile  │  │
  │  └─────────────┘  └─────────────┘  └─────────────┘  │
  │                                                      │
  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │
  │  │  BlockCache │  │  HLog(WAL)  │  │  RPC 服务    │  │
  │  │  （读缓存）  │  │  （写日志）  │  │  （请求处理） │  │
  │  └─────────────┘  └─────────────┘  └─────────────┘  │
  └─────────────────────────────────────────────────────┘
```

RegionServer 的具体工作：

| 工作 | 说明 |
|------|------|
| 处理 get/put/delete/scan | 读写请求的实际执行者 |
| 维护 MemStore | 每个 Region 的每个列族一个 MemStore |
| 刷写 StoreFile | MemStore 达阈值后刷成 HFile |
| Compaction | 合并 HFile、清理过期版本与墓碑 |
| Region Split | 达到阈值后分裂，通知 Master |
| WAL 写入 | 写操作先追加 HLog，再写 MemStore |
| 心跳 | 定期向 ZK 与 Master 汇报 |
| 短路读 | 本地 DataNode 时绕过网络直接读本地块 |

RegionServer 的堆内存分配是调优核心（第 07 篇展开）：

```text
总堆 = MemStore 池 + BlockCache 池 + 其他（索引、对象开销）

经验配比：
  写多读少：MemStore 50-60%，BlockCache 30-40%
  读多写少：BlockCache 50-60%，MemStore 30-40%
  均衡场景：各 40% 左右
```

---

## 6. HDFS 的角色

HBase 不自己管理副本，所有持久化数据都在 HDFS：

```text
HDFS 上存放的内容：
  /hbase/data/<namespace>/<table>/<region>/<columnfamily>/<hfile>
  /hbase/WALs/<rs-host>/<wal-file>
  /hbase/oldWALs/（归档）
  /hbase/hbase.id、/hbase/hbase.version（集群标识）

副本与容错：
  默认 3 副本 → 单盘/单机故障不影响数据
  机架感知 → 副本分布在不同机架
```

HBase 与 HDFS 的部署关系：

```text
标准部署：RegionServer 与 DataNode 同机（数据本地化）
  → 短路读（Short-Circuit Read）生效，读本地块不走网络
  → 写 HFile 时优先写本地副本

分离部署（计算存储分离）：
  RegionServer 独立于 DataNode
  → 失去数据本地化，读放大，延迟上升
  → 仅在云上对象存储（S3/OSS）场景考虑

HBase 3.0 与 Hadoop 3：
  要求 Hadoop 3.x（HDFS 纠删码、多 NameNode 联邦可用）
  JDK 17 为强制要求
```

---

## 7. 客户端寻址全链路

一次 `get` 请求的完整寻址过程：

```text
第 1 步：客户端启动，连接 ZooKeeper
  → 读取 /hbase/meta-region-server
  → 得到 hbase:meta 表所在 RegionServer 地址

第 2 步：查询 hbase:meta 表
  → meta 表结构：RowKey = 表名,Region起始Key,时间戳
  → 每个 Row 记录：Region 区间、RegionServer 地址、Region 状态
  → 客户端二分查找目标 RowKey 落在哪个 Region

第 3 步：缓存路由
  → 本地 Map<RegionName, RegionServer> 缓存
  → 后续请求直接命中

第 4 步：直连 RegionServer
  → 发送 Get 请求（protobuf）
  → RS 在 Region 内查 MemStore + BlockCache + HFile

第 5 步：异常处理
  → NotServingRegionException → 清缓存、重新查 meta、重试
  → 重试次数上限后抛异常给业务
```

meta 表本身也会分裂，但不会无限增长：

```text
hbase:meta 只有一张，按表名 + Region 起点分区
  → 表很多时 meta 也会分成多个 Region
  → meta 的位置由 ZooKeeper 记录（不会递归）

客户端启动只需两次网络跳转：
  ZK（找 meta）→ meta RS（找目标 Region）→ 目标 RS（读写）
```

> **提示**：客户端的 `hbase.client.retries.number`（默认 15）与 `hbase.client.pause`（默认 100ms）决定故障时的重试耐心。跨机房或高延迟场景要适当调大，但必须配合业务超时，避免请求堆积。

---

## 8. Region 生命周期

Region 从创建到销毁的状态机：

```text
NEW → ASSIGNING → OPENING → OPEN → SERVING
                                  │
                                  ├── SPLITTING → SPLIT → 两个子 Region
                                  ├── CLOSING → CLOSED
                                  ├── OFFLINE
                                  └── MERGING → MERGED

RIT（Region In Transition）：
  处于中间状态（ASSIGNING/OPENING/CLOSING/SPLITTING）的 Region
  正常几秒内结束；长时间停留即故障
```

触发行为的时机：

| 行为 | 触发者 | 说明 |
|------|--------|------|
| 分配（Assign） | HMaster | 建表、RS 故障、Region 迁移 |
| 打开（Open） | RegionServer | 加载 HFile、重放 WAL、恢复 MemStore |
| 分裂（Split） | RegionServer | 达到大小/行数阈值 |
| 合并（Merge） | HMaster/手动 | 小 Region 合并，减少管理开销 |
| 下线（Close） | HMaster | 迁移、关机、表禁用 |

Region 打开时的恢复过程：

```text
① 从 HDFS 读取该 Region 的 HFile 列表
② 读取该 Region 的 WAL（如果存在未刷写数据）
③ 重放 WAL 到 MemStore
④ 构建 Bloom Filter 与索引
⑤ 标记 OPEN，向 Master 汇报
```

---

## 9. Region 分裂策略

分裂策略决定"什么时候把一个 Region 切成两个"：

| 策略 | 触发条件 | 适用场景 |
|------|---------|---------|
| `IncreasingToUpperBoundRegionSplitPolicy` | 2.x 默认；Region 大小达到 `min(4 * regionCount² * flushSize, maxFileSize)` | 通用，初期快速分裂，后期稳定 |
| `SteppingSplitPolicy` | 3.x 默认候选；首次分裂阈值小，之后按倍数增长 | 新表快速铺开 |
| `ConstantSizeRegionSplitPolicy` | 固定大小（`hbase.hregion.max.filesize`） | 预分区后稳定运行 |
| `KeyPrefixRegionSplitPolicy` | 按 RowKey 前缀保持同前缀在一个 Region | 前缀有业务含义（如租户 ID） |
| `DelimitedKeyPrefixRegionSplitPolicy` | 按分隔符切分前缀 | RowKey 用 `|`、`_` 分隔 |

分裂过程：

```text
① RegionServer 检测到 Region 超过阈值
② 在本地创建两个 daughter Region 的目录（引用父 HFile）
③ 父 Region 标记 SPLIT，停止写入
④ Master 将两个子 Region 分配到 RS（可能是其他机器）
⑤ 子 Region 后台 Compaction 真正分离数据
⑥ 父 Region 清理

关键点：
  分裂不是"复制数据"，而是"元数据切分 + 后台合并"
  分裂期间短暂不可写（毫秒到秒级）
```

预分区（第 05 篇展开）是避免"初期全表写一个 Region"的标准手段：

```text
create 'app:order', 'info', SPLITS => ['1', '2', '3', '4', '5', '6', '7', '8', '9']
```

---

## 10. 负载均衡与故障转移

### 10.1 负载均衡

HMaster 定期执行 balancer，把 Region 从负载高的 RS 迁到低的 RS：

```text
触发条件：
  hbase.balancer.period（默认 300000ms，5 分钟）
  Region 分布标准差超过阈值

两种均衡：
  - 按 Region 数量均衡（默认）
  - 按 Region 大小均衡（StochasticLoadBalancer，默认实现）

限制：
  迁移有成本（关闭 → 打开 → 重放 WAL）
  频繁均衡会放大延迟，生产上可调大周期或限制并发迁移数
```

### 10.2 RegionServer 故障转移

一台 RegionServer 宕机的完整链路：

```text
① RS 进程停止心跳
② ZooKeeper 会话超时（默认 90s），删除该 RS 的临时节点
③ HMaster 感知 → 将该 RS 上的 Region 标记为 OFFLINE
④ Master 执行 WAL Split（分布式日志拆分）：
     - 把该 RS 的 WAL 按 Region 分组
     - 每个 Region 的 WAL 片段放到对应目录
⑤ Master 把这些 Region 重新分配给其他 RS
⑥ 新 RS 打开 Region，重放对应 WAL，恢复 MemStore
⑦ 对外恢复读写

恢复时间：
  取决于 WAL 大小与 Region 数量，通常几十秒到几分钟
  大 WAL 场景（写多）可能更久
```

> **警告**：WAL Split 是故障恢复的瓶颈。如果单台 RS 上有大量小 Region 或超大 WAL，恢复时间会显著拉长。生产上要控制单 RS 的 Region 数量（建议 20-200）、定期观察 WAL 大小。

---

## 11. HBase 3.0 架构层面变化

2026-08-05 GA 的 HBase 3.0.0 是 2.x 之后的首个大版本，架构与运行时有两处硬变化：

```text
① JDK 17 强制
   不再支持 JDK 8/11
   迁移前必须确认：Hadoop 3.x、Phoenix、Spark/Flink connector 是否支持 JDK 17

② Hadoop 3 基线
   HDFS 纠删码、多 NameNode 联邦、容器化部署（YARN/K8s）成为标准

③ API 清理
   移除长期废弃的客户端 API
   升级前用官方兼容性报告（2.0.0 → 3.0.0RC0）逐项核对

④ 异步客户端成熟
   CompletableFuture 风格的 Table/Admin API 成为推荐用法
   适合高并发网关型写入

⑤ 运维工具链
   hbck2 继续作为一致性检查与修复工具
   RegionServer 分组（RSGroup）用于资源隔离
```

需要注意的兼容性事项：

| 事项 | 说明 |
|------|------|
| 客户端版本 | 3.0 客户端连 2.x 服务端不保证兼容，反之亦然；升级先升服务端再升客户端 |
| 协处理器 | 自定义 Coprocessor 依赖内部 API，升级必须回归 |
| Phoenix | 需使用支持 HBase 3.0 的版本线 |
| ZooKeeper | 3.8/3.9，仍需外部部署 |
| 配置文件 | 2.x 配置大多沿用，但 `hbase-site.xml` 中废弃项会被忽略或报错 |

> **提示**：生产升级路径建议 2.5.x → 2.6.x → 3.0.0，每步做兼容性验证与回归；不要从 2.4 直接跳 3.0。

---

## 12. 小结

| 问题 | 答案 |
|------|------|
| 四大组件 | Client、ZooKeeper、HMaster、RegionServer、HDFS |
| 谁管路由 | ZooKeeper 存 meta 位置，meta 表存 Region 路由，Client 缓存 |
| HMaster 管什么 | schema、Region 分配、负载均衡、权限；不参与读写 |
| RegionServer 管什么 | 读写、MemStore、WAL、Split、Compaction |
| 数据副本谁做 | HDFS 三副本，RS 之间不复制 |
| Region 怎么切 | 按 RowKey 区间，达到阈值自动分裂 |
| RS 宕机怎么恢复 | ZK 感知 → Master 分配 → WAL Split → 重放 → 恢复 |
| 为什么能扩展 | 控制面与数据面分离，客户端直连，加 RS 即加吞吐 |
| 3.0 硬门槛 | JDK 17、Hadoop 3、API 清理 |

**下一模块**：[03-HBase数据存储与LSM-Tree](03-HBase数据存储与LSM-Tree.md)　**返回总览**：[00-HBase知识体系总览](00-HBase知识体系总览.md)
