# MongoDB 核心架构（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | MongoDB 架构原理深度解析
> **版本**：MongoDB 6.x/7.x
> **核心场景**：理解 MongoDB 底层运行机制，为集群部署和性能优化打基础

---

## 一、总体架构

```
┌─────────────────────────────────────────────┐
│              客户端 / 驱动                    │
│     Java Driver / mongosh / Compass          │
└──────────────────┬──────────────────────────┘
                   ↓
┌─────────────────────────────────────────────┐
│            查询引擎（Query Engine）           │
│      解析 → 优化 → 执行计划 → 返回结果         │
└──────────────────┬──────────────────────────┘
                   ↓
┌─────────────────────────────────────────────┐
│          存储引擎（Storage Engine）            │
│           WiredTiger（默认）                   │
│     内存 Cache + 磁盘 B-Tree + Journal       │
└─────────────────────────────────────────────┘
```

---

## 二、单机核心组件

### 2.1 存储引擎 — WiredTiger

```
写请求
  ↓
内存 Cache（默认物理内存 50%）
  ↓ 每 60s 或 2GB
Checkpoint → 磁盘 B-Tree
  ↓ 同步写
Journal（WAL 预写日志，每 100ms 刷盘）
```

| 特性 | 说明 |
|---|---|
| **存储格式** | B-Tree 数据结构 |
| **压缩** | snappy（默认）/ zlib / zstd |
| **并发控制** | MVCC（多版本并发控制），文档级锁 |
| **Checkpoint** | 每 60s 或 2GB 数据写入，持久化数据 |
| **Journal** | 预写日志（WAL），100ms 刷盘一次 |

### 2.2 Journal（预写日志）

防止宕机数据丢失的机制：

```
写入流程：
1. 写操作先写入 Journal（100ms 间隔刷盘）
2. 数据写入内存 Cache
3. 满足条件后 Checkpoint 持久化到磁盘
4. 若在两次 Checkpoint 之间宕机 → 重启后从 Journal 恢复
```

### 2.3 查询引擎

```
查询请求 → 解析器(Parser) → 查询优化器(Optimizer)
                              ↓
                   选择使用哪个索引 / 全表扫描
                              ↓
                   生成执行计划 → 执行 → 返回
```

```javascript
// 查看查询计划
db.users.find({name: "zhangsan"}).explain("executionStats")
// 返回：
// - winningPlan: 最终选择的计划
// - totalDocsExamined: 扫描了多少文档
// - executionTimeMillis: 执行时间
```

---

## 三、集群架构组件

### 3.1 副本集（Replica Set）—— 高可用

```
┌───────────────────────────────────┐
│           副本集架构                │
│                                    │
│  ┌──────────┐    oplog 同步        │
│  │ Primary  │──────────┬──────────┐│
│  │ (读写)   │          │          ││
│  └──────────┘          ↓          ↓│
│                   ┌─────────┐ ┌────────┐│
│                   │Secondary│ │Secondary││
│                   │ (只读)  │ │ (只读) ││
│                   └─────────┘ └────────┘│
│                         ↑              │
│                    ┌─────────┐          │
│                    │ Arbiter │ (仅投票) │
│                    └─────────┘          │
└───────────────────────────────────┘
```

| 角色 | 职责 |
|---|---|
| **Primary** | 唯一可读写，记录 oplog |
| **Secondary** | 只读，同步 oplog，故障时可选举为 Primary |
| **Arbiter** | 仅投票，不存数据，用于打破平局 |

oplog 是一个**有上限的集合**（capped collection），记录所有写操作：

```javascript
// 查看 oplog
use local
db.oplog.rs.find().sort({$natural: -1}).limit(1)
```

### 3.2 分片集群（Sharded Cluster）—— 高扩展

```
                    客户端
                       ↓
┌──────────────────────────────────┐
│           Mongos（路由）          │
│     解析请求 → 查配置 → 转发      │
└───────┬──────────┬───────────────┘
        ↓          ↓           ↓
   ┌────────┐ ┌────────┐ ┌────────┐
   │ Shard1 │ │ Shard2 │ │ Shard3 │   ← 每个 Shard 是一个副本集
   │(副本集) │ │(副本集) │ │(副本集) │
   └────────┘ └────────┘ └────────┘
        ↑          ↑           ↑
   ┌─────────────────────────────────┐
   │        Config Server（配置）      │
   │    存储分片路由规则（也是副本集）    │
   └─────────────────────────────────┘
```

| 组件 | 职责 |
|---|---|
| **Shard** | 数据分片，每个 Shard 是一个副本集 |
| **Config Server** | 存储集群元数据（分片路由规则） |
| **Mongos** | 路由层，客户端只与它交互 |

---

## 四、数据分片机制

### 4.1 分片键（Shard Key）

数据根据分片键拆分到不同 Shard：

```javascript
// 为集合启用分片
sh.shardCollection("mydb.users", { userId: "hashed" })
```

### 4.2 分片策略

| 策略 | 原理 | 适用 |
|---|---|---|
| **范围分片** | 按分片键值范围：Shard1 存 [min-1000), Shard2 [1000-2000) | 范围查询多 |
| **哈希分片** | `hash(key)` 分布：数据均匀打到各 Shard | 等值查询多 |

### 4.3 Chunk 拆分与迁移

```
Chunk = 分片内的一段数据（默认 64MB）

当 Chunk 过大 → 自动拆分为两个 Chunk
当各 Shard 的 Chunk 数不均衡 → 自动迁移 Chunk

整个过程对客户端透明
```

---

## 五、数据读写流程

### 5.1 写入流程

```
客户端 → Mongos → 查 Config Server（确定目标 Shard）→ 写对应 Shard 的 Primary
                                                         ↓
                                           Primary 的 oplog → Secondary 同步
```

### 5.2 读取流程

```
读取配置（readPreference）：
- primary（默认）：只读主节点，数据最新
- primaryPreferred：优先主节点，主不可用则读从
- secondary：只读从节点（容许延迟）
- secondaryPreferred：优先从节点
- nearest：网络延迟最低的节点
```

---

## 六、存储监控

```javascript
// 数据库整体统计
db.stats()
// 返回：collections, objects, dataSize, storageSize, indexes, indexSize

// 集合统计
db.users.stats()

// 查看存储引擎状态
db.serverStatus().wiredTiger
```

---

## 七、面试核心要点

1. **WiredTiger 三个关键特性？** 文档级并发 + 压缩存储 + 多文档事务
2. **Primary 和 Secondary 怎么同步？** Primary 记 oplog → Secondary 拉取重放
3. **Mongos 干什么的？** 请求路由，客户端不直接访问 Shard
4. **范围分片 vs 哈希分片？** 范围适合范围查询但易热点，哈希分布均匀但只适合等值查
5. **数据写入后多久可读？** 默认立刻可读（从 Primary 读），从 Secondary 读取决于同步延迟

---

## 八、极简总结

```
单机 = WiredTiger（Cache + B-Tree + Journal）
副本集 = Primary(读写) + Secondary(只读) + 自动故障转移
分片 = Mongos 路由 + Config Server 配置 + Shard 副本集
oplog = 副本集同步的心脏
Chunk = 数据分片的单位，默认 64MB
```
