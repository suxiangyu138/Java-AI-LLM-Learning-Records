# MongoDB 副本集与分片集群
> 副本集解决高可用，分片集群解决水平扩展——MongoDB 从单机到企业级集群的完整方案。

## 目录
1. [副本集架构](#1-副本集架构)
2. [oplog 核心机制](#2-oplog-核心机制)
3. [故障转移与读写策略](#3-故障转移与读写策略)
4. [分片集群架构](#4-分片集群架构)
5. [分片键与策略](#5-分片键与策略)
6. [Chunk 拆分与迁移](#6-chunk-拆分与迁移)
7. [集群数据读写流程](#7-集群数据读写流程)

---

## 1. 副本集架构

### 1.1 架构图

```text
              Client
                 ↓
          ┌─────────────┐
          │  Primary     │ ← 读写
          │ (主节点)     │
          └──────┬───────┘
       oplog 同步
    ┌─────────┼─────────┐
    ↓         ↓         ↓
┌───────┐ ┌───────┐ ┌────────┐
│Secondary│Secondary│ Arbiter │
│ (只读)  │ (只读)  │ (仅投票)│
└───────┘ └───────┘ └────────┘
```

### 1.2 节点角色

| 角色 | 数量建议 | 职责 |
|------|:--------:|------|
| **Primary** | 1 | 唯一可读写，记录 oplog |
| **Secondary** | 1-2+ | 同步 oplog，提供只读查询，故障时参与选举 |
| **Arbiter** | 0-1 | 仅投票，不存数据，节省磁盘资源 |

> 💡 Arbiter 常用于偶数个 Secondary 时打破选举平局，它不持有数据副本，适合资源受限环境。

### 1.3 副本集配置

```javascript
// 初始化副本集（在 Primary 节点上执行）
rs.initiate({
  _id: "rs0",
  members: [
    { _id: 0, host: "192.168.1.10:27017", priority: 2 },
    { _id: 1, host: "192.168.1.11:27017", priority: 1 },
    { _id: 2, host: "192.168.1.12:27017", arbiterOnly: true }
  ]
})

// 查看副本集状态
rs.status()
rs.conf()
```

### 1.4 Priority 与投票权重

- **Priority** 决定谁能成为 Primary，priority 越高越优先。
- 优先级为 0 的节点无法成为 Primary，适合仅作灾备的节点。
- 默认所有非 Arbiter 的节点 priority = 1。

```javascript
// 设置 priority 决定选举权重
cfg = rs.conf()
cfg.members[0].priority = 3   // 节点 0 优先成为主
cfg.members[1].priority = 1
cfg.members[2].priority = 0   // 永不成为主
rs.reconfig(cfg)
```

### 1.5 副本集心跳检测

```text
每 2 秒节点间互相发送心跳（heartbeat），
用于检测节点存活状态和主节点连通性。
```

```javascript
// 查看心跳配置
rs.conf().settings

// 自定义心跳间隔和选举超时
rs.reconfig({
  _id: "rs0",
  members: [...],
  settings: {
    heartbeatIntervalMillis: 2000,      // 默认 2s
    electionTimeoutMillis: 10000        // 默认 10s
  }
})
```

### 1.6 副本集常用命令速查

| 命令 | 说明 |
|------|------|
| `rs.initiate(cfg)` | 初始化副本集 |
| `rs.status()` | 查看副本集状态 |
| `rs.conf()` | 查看副本集配置 |
| `rs.reconfig(cfg)` | 重新配置副本集 |
| `rs.add(host)` | 添加节点 |
| `rs.remove(host)` | 移除节点 |
| `rs.isMaster()` | 检查当前节点是否是主节点 |
| `rs.printReplicationInfo()` | 查看 oplog 信息 |

---

## 2. oplog 核心机制

### 2.1 什么是 oplog

oplog（Operation Log）是 Primary 记录所有写操作的**有上限集合**（capped collection），存放在 `local.oplog.rs`：

```javascript
use local
db.oplog.rs.find().sort({ $natural: -1 }).limit(1).pretty()

// 示例 oplog 条目
{
  "ts": Timestamp(1705312000, 1),   // 操作时间戳
  "op": "i",                        // i=insert, u=update, d=delete
  "ns": "mydb.users",               // 命名空间
  "o": { "_id": ..., "name": "张三" }, // 操作内容
  "o2": { "_id": ... }               // 条件（update/delete 时）
}
```

各操作类型含义：

| op 值 | 操作 | 含义 |
|:-----:|------|------|
| `i` | insert | 插入文档 |
| `u` | update | 更新文档 |
| `d` | delete | 删除文档 |
| `c` | command | 命令（createIndex, drop 等）|
| `n` | no-op | 无操作（心跳占位）|

### 2.2 Secondary 同步流程

```text
1. Primary 执行写操作，记录到 oplog
2. Secondary 持续拉取 Primary 的 oplog（异步拉取）
3. Secondary 重放 oplog 中的操作，保持数据一致
4. 如果 Secondary 落后太多（oplog 被覆盖）→ 需要全量重新同步
```

### 2.3 oplog 大小配置

```yaml
# mongod.conf
replication:
  replSetName: rs0
  oplogSizeMB: 10240    # 10GB，建议根据写入量调整
```

> ⚠️ oplog 太小 → 从节点可能追不上，需要重新全量同步。建议设置足够大（几千 MB 到几十 GB）。

查看当前 oplog 状态：

```javascript
rs.printReplicationInfo()

// 输出示例
// configured oplog size:   10240 MB
// log length start to end: 348932 seconds (4.04 days)
// oplog first event time:  Mon Jan 01 2026 ...
// oplog last event time:   Fri Jan 05 2026 ...
// now:                     Fri Jan 05 2026 ...
```

### 2.4 延迟节点（Delayed Secondary）

```javascript
// 延迟同步，用于误操作恢复
rs.add({
  host: "192.168.1.20:27017",
  priority: 0,
  hidden: true,
  slaveDelay: 3600    // 延迟 3600 秒（1 小时）同步
})
```

延迟节点常用于防御误操作（如误 drop 集合），给运维留出恢复窗口。

### 2.5 隐藏节点（Hidden Secondary）

```javascript
rs.add({
  host: "192.168.1.21:27017",
  priority: 0,
  hidden: true         // 隐藏节点，不对客户端可见
})
```

隐藏节点不参与读请求分发，适合用作专门备份或报表节点。

---

## 3. 故障转移与读写策略

### 3.1 自动故障转移

```text
1. Primary 宕机或网络不可达（超过 electionTimeoutMillis，默认 10s）
2. 剩余的 Secondary 节点发起选举
3. 得票最多的 Secondary 成为新 Primary
4. 旧 Primary 恢复后降级为 Secondary

选举算法：Raft 共识算法
```

### 3.2 选举触发条件

| 条件 | 说明 |
|------|------|
| Primary 心跳超时 | Secondary 检测到 Primary 无响应，超过选举超时时间 |
| Primary 主动下线 | rs.stepDown() 平滑降级 |
| 网络分区 | 多数节点与 Primary 失联 |

```javascript
// 手动降级 Primary（用于维护）
rs.stepDown(60)   // 60 秒内不允许重新当选
```

### 3.3 读偏好（Read Preference）

```javascript
// mongosh 设置
db.getMongo().setReadPref("secondary")

// Java 设置
MongoClientSettings.builder()
  .readPreference(ReadPreference.secondaryPreferred())
  .build();

// Spring Boot 配置
spring.data.mongodb.uri = mongodb://host:27017/mydb?readPreference=secondaryPreferred
```

| 模式 | 说明 | 适用场景 |
|------|------|----------|
| `primary`（默认） | 只读主节点，数据最最新 | 强一致性要求 |
| `primaryPreferred` | 优先主节点，主不可用时读从 | 读多写少，容忍短暂从延迟 |
| `secondary` | 只读从节点，容许延迟 | 报表分析、批量导出 |
| `secondaryPreferred` | 优先从节点，从不可用时读主 | 读写分离，减轻主库压力 |
| `nearest` | 网络延迟最低的节点 | 跨地域部署，降低延迟 |

### 3.4 写关注（Write Concern）

```javascript
// 写关注：等待多数节点确认
db.users.insertOne(
  { name: "张三" },
  { writeConcern: { w: "majority", wtimeout: 5000 } }
)
```

| 级别 | 说明 | 安全性 | 性能 |
|------|------|:------:|:----:|
| `w: 1` | Primary 确认即返回 | ⭐⭐ | 高 |
| `w: majority` | 多数节点确认 | ⭐⭐⭐ | 中 |
| `w: 0` | 不等待确认（最快） | ⭐ | 极高 |
| `j: true` | Journal 写入磁盘后确认 | ⭐⭐⭐ | 低 |

> 🎯 **读写策略黄金法则**：读偏好用 secondaryPreferred 实现读写分离，写关注用 majority 保证数据安全——性能和一致性取得平衡。

### 3.5 读关注（Read Concern）

```javascript
db.users.find().readConcern("majority")
```

| 级别 | 说明 |
|------|------|
| `local`（默认） | 读取本地最新数据，可能被回滚 |
| `majority` | 读取已被多数节点确认的数据，不会被回滚 |
| `linearizable` | 线性一致性，最严格 |

---

## 4. 分片集群架构

### 4.1 架构图

```text
                   Client
                      ↓
         ┌───────────────────────┐
         │     Mongos（路由层）   │  — 接收请求，转发分片
         │     * 可部署多个      │
         └────┬──────┬──────┬────┘
              ↓      ↓      ↓
      ┌────────┐ ┌────────┐ ┌────────┐
      │ Shard1 │ │ Shard2 │ │ Shard3 │  — 每个 Shard 是副本集
      │ (副本集)│ │ (副本集)│ │ (副本集)│
      └────────┘ └────────┘ └────────┘
              ↑      ↑      ↑
         ┌─────────────────────────┐
         │  Config Server（副本集）  │  — 存储分片路由元数据
         │  3 节点                 │
         └─────────────────────────┘
```

### 4.2 三个核心组件

| 组件 | 职责 | 部署要求 |
|------|------|----------|
| **Mongos** | 请求路由层，客户端入口，无状态 | 至少 2 个（避免单点），无状态可水平扩展 |
| **Config Server** | 存储集群元数据（分片路由规则、Chunk 分布）| 3 节点副本集，必须开启 journal |
| **Shard** | 数据存储层，每个 Shard 是独立副本集 | 每 Shard 至少 2 节点（主从架构）|

> 💡 Config Server 是整个集群的"大脑"，一旦宕机整个集群不可用，务必用 3 节点副本集高可用部署。

### 4.3 分片集群部署步骤

```javascript
// 1. 启动 Config Server 副本集
// mongod --configsvr --replSet configRS --dbpath /data/configdb

// 2. 启动各 Shard 副本集
// mongod --shardsvr --replSet shard1 --dbpath /data/shard1

// 3. 启动 Mongos 路由
// mongos --configdb configRS/host1:27019,host2:27019,host3:27019

// 4. 登录 Mongos 添加分片
sh.addShard("shard1/host1:27018,host2:27018,host3:27018")
sh.addShard("shard2/host1:27017,host2:27017,host3:27017")

// 5. 启用分片
sh.enableSharding("mydb")
sh.shardCollection("mydb.users", { userId: "hashed" })
```

### 4.4 Mongos 路由过程

```text
1. 客户端连接 Mongos（对外暴露）
2. Mongos 查询 Config Server 获取分片元数据（缓存到本地）
3. Mongos 将请求转发到对应 Shard 的 Primary
4. Shard 执行操作，返回结果
5. Mongos 将结果返回给客户端
```

Mongos 自身不存储数据，通过路由表（从 Config Server 获取并缓存）决定请求发送到哪个 Shard。

---

## 5. 分片键与策略

### 5.1 启用分片

```javascript
// 启用数据库分片
sh.enableSharding("mydb")

// 为集合指定分片键
sh.shardCollection("mydb.users", { userId: "hashed" })
sh.shardCollection("mydb.orders", { createTime: 1 })
```

### 5.2 分片键选择标准

| 标准 | 说明 | 反面例子 |
|------|------|----------|
| **高基数** | 不同值多，数据分布均匀 | 性别（只有男女，无法分片）|
| **查询频繁** | 常用查询能定位到单个分片 | 从不作为查询条件的字段 |
| **非单调递增** | 避免数据全落到最后一个分片 | 自增 ID、时间戳 |

> ⚠️ 分片键一旦选定不可修改（除非重新导出导入），务必谨慎选择。

### 5.3 范围分片 vs 哈希分片

| 策略 | 原理 | 适用 | 缺点 |
|------|------|------|------|
| **范围分片** | 按值范围：Shard1 [min-1000), Shard2 [1000-2000) | 范围查询多 | 单调递增键易热点 |
| **哈希分片** | `hash(key)` 均匀分布到各 Shard | 等值查询多 | 范围查询需全分片扫描 |

```javascript
// 范围分片
sh.shardCollection("mydb.orders", { createTime: 1 })
// 查询 2026-01 的数据 → 只扫描对应分片
db.orders.find({ createTime: { $gte: ISODate("2026-01-01"), $lt: ISODate("2026-02-01") } })

// 哈希分片
sh.shardCollection("mydb.users", { userId: "hashed" })
// 查询单个用户 → 精确定位到分片
db.users.find({ userId: "u12345" })
// 范围查询 → 所有分片扫描
db.users.find({ userId: { $gt: "u1000", $lt: "u2000" } })   // 性能差
```

### 5.4 复合分片键

```javascript
// 组合多个字段做分片
sh.shardCollection("mydb.orders", { status: 1, createTime: 1 })
```

复合分片键可以在范围查询场景下做到更精细的数据分布，同时支持前缀字段的等值查询直接定位。

### 5.5 分片键选择建议

| 业务场景 | 推荐分片键 | 策略 |
|----------|------------|:----:|
| 用户数据 | userId / 手机号 | 哈希 |
| 订单数据 | 用户 ID + 时间 | 范围 |
| IoT 时序数据 | 设备 ID | 哈希 |
| 内容管理系统 | 文章 ID | 哈希 |
| 日志系统 | 时间戳 | 范围（配合 TTL 索引）|

---

## 6. Chunk 拆分与迁移

### 6.1 什么是 Chunk

```text
Chunk = 分片内的一段数据（默认 64MB，可配置）
整个数据集按分片键范围切割成多个 Chunk，均匀分布在各个 Shard 上
```

### 6.2 Chunk 拆分

```text
写入导致 Chunk 超过 64MB → 自动拆分为两个 Chunk
Chunk 拆分是逻辑操作，不需要移动数据
```

### 6.3 Chunk 迁移

```text
各 Shard 的 Chunk 数不均衡 → 自动迁移 Chunk 到其他 Shard
整个过程对客户端完全透明（在线迁移）

迁移条件：
  - 某个 Shard 的 Chunk 数量超过阈值
  - Balancer 处于开启状态（默认开启）
```

### 6.4 Balancer 控制

```javascript
// 查看 Chunk 分布
sh.status()

// 手动均衡
sh.startBalancer()    // 默认开启
sh.stopBalancer()     // 维护时关闭

// 设置均衡窗口（业务低峰期再均衡）
db.settings.updateOne(
  { _id: "balancer" },
  { $set: {
    activeWindow: {
      start: "02:00",   // 凌晨 2 点开始
      stop: "06:00"     // 凌晨 6 点结束
    }
  }},
  { upsert: true }
)
```

### 6.5 Chunk 大小配置

```javascript
// 修改 Chunk 大小（默认 64MB，范围 1MB-1024MB）
use config
db.settings.save({ _id: "chunksize", value: 128 })
```

| Chunk 大小 | 优点 | 缺点 |
|:----------:|------|------|
| 小（1-32MB）| 迁移更平滑，分布更均匀 | 元数据量大，路由表膨胀 |
| 大（64-256MB）| 元数据少，管理成本低 | 迁移耗时长，不均匀可能性增大 |

---

## 7. 集群数据读写流程

### 7.1 写入流程

```text
客户端 → Mongos → 查 Config Server（确定目标 Shard）→ 写 Shard 的 Primary
                                                         ↓
                                           Primary 的 oplog → Secondary 同步
```

带 Zone 标签的写入：

```text
客户端 → Mongos → 判断 Shard Tag → 路由到指定 Zone 的 Shard → 写入
```

### 7.2 读取流程

```text
客户端 → Mongos → 查 Config Server → 分发到各 Shard → 汇总返回
```

带聚合的读取：

```text
客户端 → Mongos → 下发聚合到各 Shard（并行执行）
                 ↓
          各 Shard 执行局部聚合
                 ↓
          Mongos 合并结果 → 返回客户端
```

### 7.3 分布式查询分析

| 查询类型 | 路由方式 | 性能 |
|----------|----------|:----:|
| 包含分片键等值查询 | 精确路由到目标 Shard | 最优 |
| 包含分片键范围查询 | 路由到部分 Shard | 较好 |
| 不包含分片键查询 | 广播到所有 Shard（Scatter-Gather）| 最差 |

> ⚠️ 避免 Scatter-Gather 操作：所有未携带分片键的查询会广播到每个 Shard，严重降低性能。务必在查询条件中包含分片键。

### 7.4 Java 连接副本集/集群

```yaml
# 副本集连接
spring.data.mongodb.uri = mongodb://admin:admin123@host1:27017,host2:27017,host3:27017/mydb?replicaSet=rs0

# 分片集群连接（直连 Mongos）
spring.data.mongodb.uri = mongodb://mongos1:27017,mongos2:27017/mydb
```

```java
// Java MongoClient 连接副本集
MongoClientSettings settings = MongoClientSettings.builder()
    .applyToClusterSettings(builder ->
        builder.hosts(Arrays.asList(
            new ServerAddress("host1", 27017),
            new ServerAddress("host2", 27017),
            new ServerAddress("host3", 27017)
        )))
    .build();
MongoClient client = MongoClients.create(settings);
```

### 7.5 Zone 分片（标签感知分片）

```javascript
// 为分片打标签
sh.addShardTag("shard1", "east")
sh.addShardTag("shard2", "west")

// 为分片键范围绑定标签
sh.addTagRange("mydb.users", { userId: MinKey }, { userId: MaxKey }, "east")
```

Zone 分片可实现数据就近存储（如将华东用户数据存储到华东机房的分片上）。

---

## 8. 副本集 vs 分片集群对比

| 维度 | 副本集 | 分片集群 |
|------|--------|----------|
| **解决的问题** | 高可用（防止单点故障） | 水平扩展（突破单机容量）|
| **数据存储** | 全量冗余 | 数据分片存储 |
| **读写能力** | 一写多读，读可扩展 | 读写均可扩展 |
| **复杂度** | 低 | 高 |
| **适用数据量** | 单机容量以内 | 超过单机容量 |
| **客户端接入** | 直接连接 Primary/Secondary | 连接 Mongos |

> 🎯 **选型决策**：数据量 < 单机容量 → 副本集（简单可靠）；数据量 > 单机容量 → 分片集群（水平扩展）。

---

## 面试核心

**Q: 副本集最少几个节点？** 最少三个：一个 Primary、两个 Secondary；或者两个 Secondary + 一个 Arbiter。

**Q: 副本集怎么实现高可用？** Primary 故障 → 剩余 Secondary 通过选举产生新 Primary → 旧 Primary 恢复后降级为 Secondary。

**Q: Primary 和 Secondary 怎么同步？** Primary 写 oplog → Secondary 拉取重放。

**Q: oplog 用的是什么集合？** `local.oplog.rs`，是一个 capped collection（有上限集合）。

**Q: 读偏好 primary vs secondary 区别？** primary 读主（数据最新），secondary 读从（可能延迟但有负载均衡）。

**Q: Mongos 干什么的？** 请求路由，客户端不直接访问 Shard。

**Q: Config Server 是什么？** 存储分片路由元数据，必须用副本集（3 节点）。

**Q: 范围分片 vs 哈希分片？** 范围适合范围查询但易热点，哈希分布均匀但只适合等值查询。

**Q: Chunk 是什么？** 数据分片单位，默认 64MB，达到阈值自动拆分，不均衡时自动迁移。

**Q: 分片键选错了怎么办？** 分片键不可修改，只能导出数据 → 重建集合 → 重新分片 → 导入数据。

> 🎯 **极简总结**：副本集 = Primary(读写) + Secondary(只读) + 自动故障转移。分片 = Mongos 路由 + Config Server 配置 + Shard（副本集）。oplog = 同步心脏。Chunk = 数据分片单位（默认 64MB）。
