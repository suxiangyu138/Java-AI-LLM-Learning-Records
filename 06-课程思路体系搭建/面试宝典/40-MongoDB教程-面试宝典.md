# MongoDB 面试宝典
> 基于课程大纲全面覆盖面试高频考点，从文档模型、CRUD、索引、聚合管道到副本集、分片集群、安全认证与 Spring Data 整合。

## 目录
1. [一、基础概念速答（20题）](#一基础概念速答20题)
2. [二、深度原理剖析（12题）](#二深度原理剖析12题)
3. [三、实战场景题（10题）](#三实战场景题10题)
4. [四、手写代码/配置文件题（6题）](#四手写代码配置文件题6题)
5. [五、系统设计题（4题）](#五系统设计题4题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板（Top 5）](#七面试回答模板top-5)
8. [八、快速查漏补缺 CheckList](#八快速查漏补缺-checklist)

---

## 一、基础概念速答（20题）

> 以下问题考察 MongoDB 基础知识，通常出现在面试一面的前 15 分钟。

### Q1：什么是 MongoDB？它属于哪类数据库？

**MongoDB** 是一个 **文档型 NoSQL 数据库**（Document-oriented NoSQL Database），使用 **BSON**（Binary JSON）格式存储数据，由 C++ 编写，由 MongoDB Inc. 维护。

| 对比维度 | MongoDB | MySQL（关系型）|
|---------|---------|---------------|
| 数据模型 | 文档（BSON/JSON） | 表（行 + 列）|
| Schema | 无 Schema（灵活） | 固定 Schema（严格）|
| 扩展方式 | 原生水平分片 | 分库分表（中间件）|
| 事务 | 支持多文档事务（>= 4.0） | ACID 事务 |
| 索引 | 丰富（地理/文本/TTL等） | B+Tree 索引为主 |

```json
// BSON 文档示例
{
  "_id": ObjectId("507f1f77bcf86cd799439011"),
  "title": "MongoDB 面试宝典",
  "tags": ["NoSQL", "Database"],
  "views": NumberLong(1024),
  "createdAt": ISODate("2026-07-22T08:00:00Z")
}
```

> 💡 BSON 是 JSON 的二进制序列化格式，支持更多数据类型（如 Date、ObjectId、Binary Data）。

---

### Q2：MongoDB 的 _id 字段是如何生成的？

`_id` 是每个文档的**必需字段**（相当于 MySQL 的 Primary Key），默认类型为 `ObjectId`。

```
ObjectId = 4-byte 时间戳 + 3-byte 机器标识 + 2-byte PID + 3-byte 计数器
```

```java
// ObjectId 结构
// 66234a8f → 时间戳 (4 bytes)
// 9c2e3f   → 机器标识 (3 bytes)
// a1b2     → 进程ID (2 bytes)
// 000001   → 计数器 (3 bytes)

// Java 中生成 ObjectId
org.bson.types.ObjectId id = new ObjectId();
System.out.println(id.toHexString()); // 66234a8f9c2e3fa1b2000001
```

> 💡 ObjectId 在客户端生成，**无需依赖数据库**即可保证全局唯一性，这在分片环境中至关重要。

---

### Q3：MongoDB 支持事务吗？

| MongoDB 版本 | 事务支持 |
|-------------|---------|
| <= 3.6 | 仅单文档原子操作 |
| 4.0+ | 副本集多文档 ACID 事务 |
| 4.2+ | 分片集群多文档 ACID 事务 |

```java
// Spring Data MongoDB 事务示例
@Autowired
private MongoTemplate mongoTemplate;

@Transactional
public void transfer(String fromId, String toId, double amount) {
    Query fromQuery = new Query(Criteria.where("_id").is(fromId));
    Update deduct = new Update().inc("balance", -amount);
    mongoTemplate.updateFirst(fromQuery, deduct, "accounts");

    // 模拟异常 —— 事务回滚
    // if (true) throw new RuntimeException("模拟异常");

    Query toQuery = new Query(Criteria.where("_id").is(toId));
    Update add = new Update().inc("balance", amount);
    mongoTemplate.updateFirst(toQuery, add, "accounts");
}
```

---

### Q4：MongoDB 的 CRUD 操作有哪些？

| 操作 | Shell 命令 | Spring Data MongoDB |
|------|-----------|-------------------|
| **Insert** | `db.collection.insertOne()` / `insertMany()` | `mongoTemplate.insert()` / `mongoTemplate.insertAll()` |
| **Find** | `db.collection.find(query)` | `mongoTemplate.find(query, Class.class)` |
| **Update** | `db.collection.updateOne()` / `updateMany()` | `mongoTemplate.updateFirst()` / `updateMulti()` |
| **Delete** | `db.collection.deleteOne()` / `deleteMany()` | `mongoTemplate.remove()` |

```javascript
// Shell-CRUD 示例
// Create
db.users.insertOne({ name: "Alice", age: 28, tags: ["dev"] });

// Read
db.users.find({ age: { $gte: 25 } }).sort({ age: 1 }).limit(10);

// Update
db.users.updateOne(
  { name: "Alice" },
  { $set: { age: 29 }, $push: { tags: "senior" } }
);

// Delete
db.users.deleteMany({ age: { $lt: 18 } });
```

---

### Q5：什么是 MongoDB 的数据模型（Document Model）？

MongoDB 的数据模型是 **Schema-less（无模式）** 的文档模型，具有以下特点：

- 同一集合中的文档可以有**不同字段**
- 支持**嵌套文档**（Embedded Document）和**数组**
- 数据以 **BSON** 格式存储

```
// 文档模型对比：关系型 vs MongoDB

// MySQL：多表关联查询
// users 表 + addresses 表 + orders 表 → JOIN 查询

// MongoDB：单文档模型
{
  "_id": "u001",
  "name": "张三",
  "addresses": [
    { "city": "北京", "street": "朝阳路" }
  ],
  "orders": [
    { "orderId": "o001", "amount": 99.9, "status": "paid" }
  ]
}
```

> 💡 **内嵌（Embedding）** 优先于 **引用（Referencing）**，但注意 BSON 文档大小限制为 **16MB**。

---

### Q6：MongoDB 的查询操作符有哪些？

| 分类 | 操作符 | 说明 |
|------|--------|------|
| **比较** | `$eq`, `$ne`, `$gt`, `$gte`, `$lt`, `$lte` | 等于/不等于/大于/小于 |
| **范围** | `$in`, `$nin` | 在集合中/不在集合中 |
| **逻辑** | `$and`, `$or`, `$not`, `$nor` | 与/或/非/非或 |
| **元素** | `$exists`, `$type` | 字段存在性/类型检查 |
| **数组** | `$all`, `$elemMatch`, `$size` | 数组操作 |
| **正则** | `$regex` | 正则匹配 |
| **运算** | `$inc`, `$mul`, `$set`, `$unset`, `$push`, `$pull` | 更新操作符 |

```javascript
// 常用查询示例
// 大于且小于
db.products.find({ price: { $gt: 100, $lt: 500 } });

// 使用 $in
db.orders.find({ status: { $in: ["pending", "shipped"] } });

// 正则模糊查询
db.articles.find({ title: { $regex: /MongoDB/, $options: "i" } });

// 字段存在检查
db.users.find({ email: { $exists: true } });

// 数组条件
db.students.find({ scores: { $elemMatch: { subject: "math", score: { $gte: 90 } } } });
```

---

### Q7：MongoDB 索引有哪些类型？

| 索引类型 | 说明 | 适用场景 |
|---------|------|---------|
| **单字段索引 (Single Field)** | 单列索引 | 最基础的查询 |
| **复合索引 (Compound Index)** | 多列组合索引 | 多条件查询 |
| **多键索引 (Multikey Index)** | 针对数组字段的索引 | 数组字段查询 |
| **文本索引 (Text Index)** | 全文搜索 | 内容搜索 |
| **地理空间索引 (2dsphere/2d)** | 地理空间查询 | LBS、地图应用 |
| **TTL 索引** | 文档自动过期 | 会话、日志清理 |
| **哈希索引 (Hashed Index)** | 哈希分片键 | 分片集群 |
| **唯一索引 (Unique Index)** | 保证唯一性 | 业务约束 |

```java
// Spring Data MongoDB 索引定义
@Document(collection = "users")
public class User {
    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    @Indexed(name = "idx_age", background = true)
    private Integer age;

    @CompoundIndex(def = "{'city': 1, 'age': -1}", name = "idx_city_age")
    private String city;

    @TextIndexed
    private String bio; // 用于全文搜索
}
```

---

### Q8：如何查看 MongoDB 查询是否使用了索引？

使用 `explain()` 方法查看执行计划。

```javascript
// 查询执行计划
db.users.find({ age: { $gt: 25 } }).explain("executionStats");

// 输出关键字段
{
  "queryPlanner": {
    "winningPlan": {
      "stage": "IXSCAN",       // 索引扫描 (vs COLLSCAN 全表扫描)
      "indexName": "age_1",    // 使用的索引名
      "direction": "forward"
    }
  },
  "executionStats": {
    "nReturned": 1000,         // 返回文档数
    "totalDocsExamined": 1000, // 检查文档数
    "totalKeysExamined": 1000, // 检查索引键数
    "executionTimeMillis": 5   // 执行时间(ms)
  }
}
```

> ⚠️ **关键判断：** 如果 `stage: "COLLSCAN"` 且集合很大，说明**没有使用索引**，需要优化。

---

### Q9：什么是 Covered Query（覆盖查询）？

当查询的所有字段都在索引中时，MongoDB **无需读取实际文档**，直接从索引返回结果，称为 **覆盖查询**。

```javascript
// 创建覆盖索引
db.users.createIndex({ name: 1, email: 1, age: 1 });

// 覆盖查询：查询字段和投影字段都在索引中
db.users.find(
  { name: "Alice" },
  { email: 1, age: 1, _id: 0 }  // 投影只包含索引字段
).explain();
// → stage: "IXSCAN", 无 FETCH 阶段
```

> 💡 覆盖查询的 `totalDocsExamined = 0`，是性能最优的查询方式。

---

### Q10：MongoDB 聚合框架（Aggregation Pipeline）是什么？

聚合管道由**多个阶段**组成，数据按顺序通过每个阶段处理。

| 阶段 | 作用 | 类似 SQL |
|------|------|---------|
| `$match` | 过滤文档 | `WHERE` |
| `$group` | 分组聚合 | `GROUP BY` + 聚合函数 |
| `$sort` | 排序 | `ORDER BY` |
| `$project` | 字段投影/重塑 | `SELECT` |
| `$lookup` | 左外连接 | `LEFT JOIN` |
| `$unwind` | 数组展开 | 无直接对应 |
| `$limit` | 限制条数 | `LIMIT` |
| `$skip` | 跳过条数 | `OFFSET` |
| `$bucket` | 分桶 | `CASE WHEN` |
| `$facet` | 多维度分析 | 多个 `GROUP BY` |

```javascript
// 聚合管道示例：按城市分组统计用户年龄
db.users.aggregate([
  { $match: { status: "active" } },
  { $group: { _id: "$city", avgAge: { $avg: "$age" }, count: { $sum: 1 } } },
  { $sort: { avgAge: -1 } },
  { $project: { city: "$_id", avgAge: 1, count: 1, _id: 0 } },
  { $limit: 10 }
]);
```

---

### Q11：`$lookup` 如何实现类似 SQL JOIN 的功能？

```javascript
// 订单集合
db.orders.aggregate([
  {
    $lookup: {
      from: "users",              // 关联集合
      localField: "userId",        // 本地字段
      foreignField: "_id",         // 关联字段
      as: "userInfo"               // 输出字段（数组）
    }
  },
  { $unwind: "$userInfo" },        // 展开为对象（可选）
  {
    $project: {
      orderId: 1,
      totalAmount: 1,
      "userInfo.name": 1,
      "userInfo.email": 1
    }
  }
]);
```

```java
// Spring Data MongoDB 聚合
TypedAggregation<Order> agg = Aggregation.newAggregation(
    Order.class,
    lookup("users", "userId", "_id", "userInfo"),
    unwind("userInfo"),
    project("orderId", "totalAmount")
        .and("userInfo.name").as("userName")
);

AggregationResults<Map> results = mongoTemplate.aggregate(agg, Map.class);
```

> ⚠️ `$lookup` 性能比 MySQL JOIN 差，尽量避免在频繁查询的路径上使用。

---

### Q12：什么是 MongoDB 副本集（Replica Set）？

**副本集** 是一组维护相同数据集的 MongoDB 实例，提供**高可用性**和**数据冗余**。

```
┌─────────────────────────────────────────────────┐
│                  副本集 (Replica Set)              │
│                                                   │
│  ┌──────────┐     ┌──────────┐     ┌──────────┐ │
│  │ Primary  │────→│Secondary │────→│Secondary │ │
│  │ (主节点)  │     │ (副本)   │     │ (副本)   │ │
│  └──────────┘     └──────────┘     └──────────┘ │
│       │                                           │
│       └──────────────┐                           │
│                  ┌───┴────┐                       │
│                  │ Arbiter│                       │
│                  │ (仲裁)  │                       │
│                  └────────┘                       │
└─────────────────────────────────────────────────┘
```

**三个角色：**
- **Primary**：可读可写，整个副本集只有**一个**Primary
- **Secondary**：只读（默认），从 Primary 同步数据，**可投票**参与选举
- **Arbiter**：只参与**选举投票**，不存储数据

> 💡 推荐副本集至少 **3 个节点**（1 Primary + 2 Secondary），或 **1 Primary + 1 Secondary + 1 Arbiter**。

---

### Q13：副本集如何选举 Primary？

```
选举触发条件：
1. Primary 心跳超时（默认 10s）
2. Primary 宕机
3. 网络分区导致 Primary 失联

选举算法：Bully 算法（类 Raft 的改进版）
```

**选举原则：**
1. 只有**节点间心跳中断**后会触发选举
2. 拥有**最高 priority** 的节点胜出
3. Priority 相同时，**oplog 最新**（数据最新的）的节点胜出
4. 获得**超过半数**投票的节点成为 Primary

```javascript
// 查看副本集状态
rs.status();
rs.conf();  // 查看配置（包括各节点 priority）

// 手动触发选举
rs.stepDown();
```

---

### Q14：什么是 oplog？

**oplog**（Operations Log）是副本集同步的**核心机制**，存在 `local.oplog.rs` 集合中。

```
特点：
- 存储在 Primary 的 local 数据库中
- 是一个 Capped Collection（固定大小集合）
- 记录了所有写操作（幂等的）
- Secondary 通过拉取 oplog 并重放来同步数据

oplog 示例：
{
  "ts": Timestamp(1721600000, 1),  // 操作时间戳
  "op": "i",                         // 操作类型：i=insert, u=update, d=delete
  "ns": "test.users",                // 命名空间
  "o": { "_id": 1, "name": "Alice" } // 操作内容
}
```

> ⚠️ **oplog 大小默认**：WiredTiger 引擎下为空闲磁盘的 5%（最多 50GB）。oplog 太小会导致 Secondary 追赶不上而被 **「stale」**。

---

### Q15：什么是 Read Preference 和 Write Concern？

```javascript
// Read Preference：指定读请求路由
// primary:         默认，只读 Primary（最一致性）
// primaryPreferred: 优先读 Primary，不可用时读 Secondary
// secondary:       只读 Secondary
// secondaryPreferred: 优先读 Secondary
// nearest:         读网络延迟最低的节点

// Write Concern：指定写入确认级别
// { w: 1 }        默认，Primary 确认即可
// { w: "majority" } 大多数节点确认（强一致性）
// { w: 0 }        无需确认（性能最高）
// { w: 3 }        3 个节点确认
// { j: true }     同时写入日志（Journal）

// Spring Data MongoDB 设置
@Bean
public MongoClientSettings mongoClientSettings() {
    return MongoClientSettings.builder()
        .readPreference(ReadPreference.secondaryPreferred())
        .writeConcern(WriteConcern.MAJORITY)
        .applyToClusterSettings(builder ->
            builder.hosts(singletonList(new ServerAddress("localhost", 27017))))
        .build();
}
```

| 配置 | 一致性 | 可用性 | 延迟 |
|------|--------|--------|------|
| w=1 + readPreference=primary | 强 | 中等 | 低 |
| w=majority + readPreference=secondary | 最终 | 高 | 中等 |
| w=0 + readPreference=nearest | 弱 | 最高 | 最低 |

---

### Q16：MongoDB 分片集群（Sharding）架构是怎样的？

```
┌─────────────────────────────────────────────────────┐
│                   分片集群 (Sharded Cluster)          │
│                                                       │
│  ┌─────────────┐    ┌─────────────┐                  │
│  │   mongos    │    │   mongos    │  ← 路由层         │
│  │  (路由节点)  │    │  (路由节点)  │                  │
│  └──────┬──────┘    └──────┬──────┘                  │
│         │                  │                          │
│         └────────┬─────────┘                          │
│                  │                                    │
│  ┌───────────────┴────────────────┐                  │
│  │        Config Server           │  ← 配置层         │
│  │   (副本集，存储元数据)          │                  │
│  └───────────────┬────────────────┘                  │
│                  │                                    │
│         ┌────────┴────────┐                          │
│         │                 │                          │
│  ┌──────┴──────┐  ┌──────┴──────┐                   │
│  │ Shard 1     │  │ Shard 2     │  ← 数据层          │
│  │ (副本集)    │  │ (副本集)    │                   │
│  └─────────────┘  └─────────────┘                   │
└─────────────────────────────────────────────────────┘
```

**三个组件：**
- **mongos**：路由节点，客户端入口，将请求路由到对应分片
- **Config Server**：存储集群元数据（分片键范围 → 分片映射）
- **Shard**：实际存储数据的节点，每个 Shard 是一个副本集

---

### Q17：什么是分片键（Shard Key）？如何选择？

**分片键**（Shard Key）是决定文档分布到哪个分片的字段，**选择正确与否决定分片集群性能**。

```javascript
// 创建分片集合
sh.shardCollection("mydb.users", { "userId": "hashed" });

// 范围分片
sh.shardCollection("mydb.orders", { "orderDate": 1, "userId": 1 });
```

| 分片策略 | 说明 | 优点 | 缺点 |
|---------|------|------|------|
| **哈希分片** | 对分片键做哈希运算 | 数据分布均匀 | 范围查询效率低 |
| **范围分片** | 按值范围划分 | 范围查询高效 | 可能产生热点 |
| **Zone 分片** | 按地理位置/标签 | 数据本地化 | 配置复杂 |

> 💡 **分片键选择原则**：基数大（高 cardinality）、频率均匀、不易单调增减。

---

### Q18：什么是 Chunk 和 Balancer？

| 概念 | 说明 |
|------|------|
| **Chunk** | MongoDB 分片的基本数据块，默认 **64MB** |
| **Balancer** | 后台进程，自动平衡各分片的 Chunk 数量 |
| **Split** | Chunk 超过阈值时自动分裂 |
| **Move** | Balancer 将 Chunk 从过多节点迁移到较少节点 |

```javascript
// 查看 Chunk 分布
sh.status();

// 手动调整 Chunk 大小
db.settings.save({ _id: "chunksize", value: 128 }); // 改为 128MB

// 禁用 Balancer（通常在维护时）
sh.stopBalancer();
// 恢复
sh.startBalancer();
```

> ⚠️ Balancer 在**业务低峰期**自动运行（默认 0:00-6:00），迁移可能影响性能。

---

### Q19：WiredTiger 存储引擎的特性？

| 特性 | 说明 |
|------|------|
| **MVCC**（多版本并发控制） | 读写不互斥，每个事务一个快照 |
| **压缩** | 支持 Snappy/Zlib/Zstd 压缩，默认 Snappy |
| **缓存** | 使用内存在 OS Page Cache 和 WT Cache 两层 |
| **Cache 大小** | 默认 RAM 的 50%（可通过 `wiredTigerCacheSizeGB` 配置）|
| **Checkpoint** | 每 60s 或 2GB 日志产生一个 Checkpoint |
| **Journal** | WAL（Write-Ahead Log），崩溃恢复保障 |

```javascript
// 查看 WT 引擎状态
db.serverStatus().wiredTiger;

// 配置 WT 缓存大小（在 mongod.conf 中）
// storage:
//   wiredTiger:
//     engineConfig:
//       cacheSizeGB: 4
```

> 💡 WT 使用 **B-Tree** 存储数据，支持**行级锁**（Document-level Concurrency），比 MMAPv1 的集合级锁高很多。

---

### Q20：MongoDB 对比 MySQL，如何选择？

| 维度 | MongoDB | MySQL |
|------|---------|-------|
| 数据模型 | 文档（灵活 Schema） | 关系表（固定 Schema）|
| 事务 | 4.0+ 支持（较弱） | 强 ACID |
| JOIN | `$lookup`（性能一般） | 原生 JOIN（高效）|
| 扩展性 | 原生分片（水平扩展） | 主从复制 + 分库分表 |
| 索引类型 | 丰富（地理/文本/TTL） | B+Tree + 全文 |
| 典型场景 | 日志、IoT、内容管理 | 金融、ERP、强一致性 |
| 一致性 | 最终一致性（默认） | 强一致性 |

**何时选 MongoDB：**
- Schema 频繁变化（敏捷开发）
- 数据量大，需要**原生水平扩展**
- 文档/JSON 结构的天然数据（日志、用户画像）
- 地理位置、全文搜索等需求

**何时不选 MongoDB：**
- 强事务要求（银行转账、库存扣减）
- 复杂的关系查询（多表 JOIN）
- 对一致性要求极高的场景

---

## 二、深度原理剖析（12题）

### Q21：MongoDB 的读/写锁机制是怎样的？

**WiredTiger 存储引擎**：

| 锁级别 | 说明 |
|--------|------|
| **意向锁（Intent Lock）** | 避免操作冲突 |
| **文档级锁（Document-level）** | 写同一文档才互斥 |
| **集合级意向锁** | 修改 Schema 等 DDL 操作时 |

```
// WiredTiger 锁模型
// 读操作：共享锁 + 快照读（MVCC）
// 写操作：排他锁（仅影响目标文档）

// 并发场景：同时修改不同文档 → 互不阻塞
db.users.updateOne({ _id: 1 }, { $set: { name: "A" } }); // OK
db.users.updateOne({ _id: 2 }, { $set: { name: "B" } }); // OK (不冲突)
```

> 💡 MongoDB **不**使用表级锁或库级锁（MMAPv1 曾是集合级锁），这是它高并发写入的关键。

---

### Q22：MongoDB 的 MVCC 是如何实现的？

WiredTiger 通过 **MVCC（Multi-Version Concurrency Control）** 实现读写不互斥：

```
事务流程：
1. 每个事务开始时分配一个时间戳 (Transaction ID)
2. 读取时看到的是该时间戳的快照 (snapshot)
3. 写入创建新版本，旧版本保留在内存/磁盘中
4. 提交时时间戳被全局可见

数据行（文档）在磁盘上的版本链：
[ver@T1] → [ver@T2] → [ver@T3] → ...
                              ↑
                     当前读看到最新提交版本

Snapshot Read：看到一个固定时间点的版本链
```

---

### Q23：WiredTiger 的缓存和淘汰策略？

```
   ┌──────────┐          ┌─────────────┐
   │ WT Cache │←──────→│ OS Page Cache│
   │ (内存)   │  LRU    │  (系统缓存)  │
   └──────────┘          └─────────────┘
        │                        │
        ▼                        ▼
   Internal Page             OS Disk I/O
   (BTree 节点)
```

**淘汰策略：**
- **LRU（最近最少使用）** + 淘汰年龄（eviction age）
- 两个水位线：**eviction_target**（80%）和 **eviction_trigger**（95%）
- Cache 超过 95% 时，**所有写操作阻塞**，直到降到 80% 以下

> ⚠️ 这是生产环境的常见问题：Cache 打满 → 写阻塞 → 雪崩。**务必监控** `wiredTiger.cache.bytes currently in the cache` 和 `percentage overhead`。

---

### Q24：副本集心跳和选举的底层机制？

```javascript
// 心跳参数
// heartbeatIntervalMillis: 2000ms（默认 2s 一次心跳）
// heartbeatTimeoutSecs: 10s（默认 10s 超时）
// electionTimeoutMillis: 10000ms（检测到 Primary 失联后触发选举）

// 选举算法流程
1. Secondary 检测到 Primary 心跳超时（10s）
2. 发起选举请求（以 priority 最高的节点优先）
3. 向其他节点发送 voteRequest
4. 其他节点检查自己的 optime（oplog 时间戳）
5. 如果请求者 optime ≥ 自己的 optime，则投票同意
6. 获得超过半数投票 → 成为 Primary

// 防止双主（Split Brain）：Rollback 机制
// 旧 Primary 重新加入时，其未同步的写入会被 Rollback
```

---

### Q25：MongoDB 数据压缩机制？

| 压缩算法 | 压缩比 | CPU 开销 | 默认 |
|---------|--------|---------|------|
| Snappy | 2-3x | 低 | 默认 |
| Zlib | 3-5x | 高 | 可选 |
| Zstd | 3-4x | 中 | 4.2+ 可选 |

```yaml
# mongod.conf 配置压缩
storage:
  wiredTiger:
    collectionConfig:
      blockCompressor: zstd    # 集合级压缩
    indexConfig:
      prefixCompression: true  # 索引前缀压缩
```

> 💡 生产环境推荐 **Snappy** 平衡压缩率和性能；**Zstd** 是性能与压缩比的最优折中。

---

### Q26：MongoDB 的 Journal（预写日志）如何工作？

```
写入流程（Journal）：
1. 客户端请求写入
2. 写入 In-Memory Snapshot
3. 写入 Journal（WAL，磁盘，默认 100ms 刷盘）
4. 返回客户端写入成功
5. Checkpoint 发生时（60s），内存数据刷入磁盘数据文件

恢复流程：
1. 从最新 Checkpoint 恢复
2. 重放 Journal 中 Checkpoint 之后的写入
3. 恢复到崩溃前的状态

配置：
storage:
  journal:
    enabled: true
    commitIntervalMs: 100  # 刷盘间隔，可配置
```

| `j` 参数 | 行为 | 性能影响 |
|----------|------|---------|
| `j: true` | Journal 写磁盘后才返回 | 较慢，但安全性高 |
| `j: false` | Journal 延迟刷盘 | 更快，但可能丢失 100ms 数据 |

---

### Q27：MongoDB 如何处理大文件（GridFS）？

**GridFS** 是 MongoDB 用于存储和检索超过 **16MB**（BSON 限制）文件的规范。

```
文件 → 分割成 255KB 的 Chunk
   ↓
fs.files    集合：存储文件元数据（文件名、大小、MD5、上传时间）
fs.chunks   集合：存储文件数据块（files_id, n（块索引）, data（Binary））
```

```java
// Spring Data MongoDB GridFS 示例
@Autowired
private GridFsTemplate gridFsTemplate;

// 上传文件
public String uploadFile(MultipartFile file) throws IOException {
    DBObject metaData = new BasicDBObject("type", "avatar");
    ObjectId fileId = gridFsTemplate.store(
        file.getInputStream(),
        file.getOriginalFilename(),
        file.getContentType(),
        metaData
    );
    return fileId.toHexString();
}

// 下载文件
public GridFSDBFile downloadFile(String fileId) {
    Query query = new Query(Criteria.where("_id").is(fileId));
    return gridFsTemplate.findOne(query);
}
```

| 对比 | GridFS | 直接存文件系统 |
|------|--------|--------------|
| 优势 | 与 MongoDB 统一管理、自动分片 | 性能更高 |
| 劣势 | 额外的 Chunk 合并开销 | 需要独立文件系统 |
| 适用 | 小文件托管（<100MB） | 大文件、视频等 |

---

### Q28：MongoDB 安全机制有哪些？

```yaml
# 开启认证
security:
  authorization: enabled
  authenticationMechanisms: SCRAM-SHA-256,SCRAM-SHA-1
```

| 安全层级 | 机制 | 说明 |
|---------|------|------|
| **认证** | **SCRAM**（默认）、**x.509**、**LDAP**、**Kerberos** | 身份验证 |
| **授权** | **RBAC**（Role-Based Access Control）| 权限控制 |
| **TLS/SSL** | 传输加密 | 防止中间人攻击 |
| **审计** | Audit Log | 记录操作日志 |
| **加密** | 静态加密（Enterprise）| 磁盘数据加密 |

```javascript
// 创建用户并授权
db.createUser({
  user: "app_user",
  pwd: "secure_password",
  roles: [
    { role: "readWrite", db: "mydb" },
    { role: "read", db: "logs" }
  ]
});

// 内置角色
// read        → 读取权限
// readWrite   → 读写权限
// dbAdmin     → 数据库管理
// userAdmin   → 用户管理
// clusterAdmin → 集群管理
// root        → 超级管理员
```

---

### Q29：`$unwind` 的工作原理和注意事项？

`$unwind` 将数组字段**展开**为多个文档（每个数组元素一个文档）。

```javascript
// 原始文档
{ "_id": 1, "items": ["a", "b", "c"] }

// 展开后
{ "_id": 1, "items": "a" }
{ "_id": 1, "items": "b" }
{ "_id": 1, "items": "c" }

// 实战：统计文章所有标签的分布
db.articles.aggregate([
  { $unwind: "$tags" },
  { $group: { _id: "$tags", count: { $sum: 1 } } },
  { $sort: { count: -1 } }
]);

// 注意：空数组处理
// 默认行为：空数组的文档被丢弃
// { preserveNullAndEmptyArrays: true } → 保留空数组文档
db.articles.aggregate([
  { $unwind: { path: "$tags", preserveNullAndEmptyArrays: true } }
]);
```

> ⚠️ 大量数据展开可能导致**内存溢出**（Pipeline 默认内存限制 100MB），可使用 `{ allowDiskUse: true }`。

---

### Q30：MongoDB 4.0+ 的多文档事务原理？

```java
// 事务底层原理
// 1. 通过 oplog 实现跨文档原子性
// 2. 使用 WiredTiger 的 snapshot isolation
// 3. 事务中的所有写操作共享同一个提交时间戳

// Java 事务代码（MongoDB Driver 4.x）
try (ClientSession session = client.startSession()) {
    session.startTransaction(TransactionOptions.builder()
        .writeConcern(WriteConcern.MAJORITY)
        .readConcern(ReadConcern.SNAPSHOT)
        .build());

    try {
        collection1.insertOne(session, doc1);
        collection2.insertOne(session, doc2);
        session.commitTransaction();
    } catch (Exception e) {
        session.abortTransaction();
    }
}
```

| 事务属性 | MongoDB 实现 |
|---------|-------------|
| 隔离级别 | **Snapshot Isolation**（快照隔离） |
| 回滚 | 基于 oplog 的逆操作 |
| 事务超时 | 默认 60s（`transactionLifetimeLimitSeconds`）|
| 事务大小 | 受 oplog 大小限制 |

> 💡 MongoDB 事务性能低于 MySQL InnoDB，不要作为默认选择，仅在确实需要时使用。

---

### Q31：MongoDB 的索引原理（B-Tree）？

MongoDB 的索引底层使用 **B-Tree**（不是 B+Tree，MySQL 用的是 B+Tree）。

| 特点 | MongoDB B-Tree | MySQL B+Tree |
|------|---------------|-------------|
| 数据存储 | 所有节点都存数据 | 仅叶节点存数据 |
| 范围查询 | 需要中序遍历 | 叶节点链表，高效 |
| 等值查询 | 高效 | 高效 |
| 叶节点指针 | 无 | 有（双向链表）|

```javascript
// 索引存储结构示意
// B-Tree： [1-2-3]
//         /    |    \
//      [0]   [2-4]  [5-8]

// 复合索引原理
// { name: 1, age: -1 } 的索引结构
// 先按 name 排序，name 相同按 age 降序
// 支持查询：{ name: "A" } → 最优
// 支持查询：{ name: "A", age: { $gt: 20 } } → 最优
// 不支持查询：{ age: { $gt: 20 } } → 无效（没有 name 前缀）
```

> 💡 **最左前缀原则**：复合索引的查询必须从最左边的字段开始。

---

### Q32：MongoDB 参数优化有哪些？

```yaml
# mongod.conf 生产优化配置
operationProfiling:
  mode: slowOp                # 慢查询日志
  slowOpThresholdMs: 100      # 慢查询阈值 100ms

storage:
  wiredTiger:
    engineConfig:
      cacheSizeGB: 8          # WT 缓存（建议 RAM 的 60-80%）
    collectionConfig:
      blockCompressor: snappy  # 压缩算法

net:
  maxIncomingConnections: 1000  # 最大连接数
  serviceExecutor: adaptive     # 线程池自适应

setParameter:
  internalQueryExecMaxBlockingSortBytes: 33554432  # 排序内存限制
  cursorTimeoutMillis: 600000    # 游标超时
```

| 参数 | 推荐值 | 说明 |
|------|--------|------|
| `cacheSizeGB` | RAM × 60%~80% | WT 缓存，**最重要的性能参数** |
| `maxIncomingConnections` | 根据业务 | 默认 65536，建议合理限制 |
| `slowOpThresholdMs` | 100~500ms | 慢查询阈值 |
| `wiredTigerCacheSizeGB` | 不要超过 RAM 的 80% | 给 OS Page Cache 留空间 |

---

## 三、实战场景题（10题）

### Q33：用户评论系统的 MongoDB 设计？

```java
// 评论集合设计
@Document(collection = "comments")
public class Comment {
    @Id
    private String id;
    private String articleId;       // 文章ID
    private String content;         // 评论内容
    private String userId;          // 用户ID
    private String parentId;        // 父评论ID（支持嵌套）
    private Integer likes;          // 点赞数
    private Date createdAt;         // 创建时间
    private String status;          // 0:待审核 1:已发布
}

// 创建索引
// db.comments.createIndex({ articleId: 1, createdAt: -1 });
// db.comments.createIndex({ parentId: 1 });

// 分页查询评论（按文章ID + 时间排序）
public Page<Comment> findByArticleId(String articleId, int page, int size) {
    Query query = new Query(Criteria.where("articleId").is(articleId)
            .and("status").is("1"))
        .with(Sort.by(Sort.Direction.DESC, "createdAt"))
        .skip((page - 1) * size)
        .limit(size);
    List<Comment> comments = mongoTemplate.find(query, Comment.class);
    long count = mongoTemplate.count(query.skip(-1).limit(-1), Comment.class);
    return new PageImpl<>(comments, PageRequest.of(page, size), count);
}

// 点赞操作（使用 $inc 原子操作）
public void likeComment(String commentId) {
    Query query = new Query(Criteria.where("_id").is(commentId));
    Update update = new Update().inc("likes", 1);
    mongoTemplate.updateFirst(query, update, Comment.class);
}
```

---

### Q34：如何实现标签系统的聚合统计？

```javascript
// 需求：统计每个标签的文章数、平均阅读量、最新文章
db.articles.aggregate([
  { $unwind: "$tags" },
  {
    $group: {
      _id: "$tags",
      articleCount: { $sum: 1 },
      avgReads: { $avg: "$reads" },
      latestArticle: { $max: "$publishDate" },
      topReadArticle: { $max: "$reads" }
    }
  },
  { $sort: { articleCount: -1 } },
  { $project: {
      tag: "$_id",
      articleCount: 1,
      avgReads: { $round: ["$avgReads", 0] },
      _id: 0
  }}
]);

// Java 实现
public List<TagStat> getTagStats() {
    Aggregation agg = Aggregation.newAggregation(
        unwind("tags"),
        group("tags")
            .count().as("articleCount")
            .avg("reads").as("avgReads"),
        sort(Sort.Direction.DESC, "articleCount"),
        project("articleCount", "avgReads")
            .and("_id").as("tag")
            .andExclude("_id")
    );
    return mongoTemplate.aggregate(agg, "articles", TagStat.class).getMappedResults();
}
```

---

### Q35：日志系统如何设计 TTL 自动过期？

```javascript
// 创建 TTL 索引（文档写入 7 天后自动删除）
db.logs.createIndex({ "createdAt": 1 }, { expireAfterSeconds: 604800 });

// Java 定义
@Document(collection = "logs")
public class LogEntry {
    @Id
    private String id;
    private String level;       // INFO, WARN, ERROR
    private String message;
    private String source;

    @Indexed(name = "ttl_idx", expireAfterSeconds = 604800) // 7天过期
    private Date createdAt;
}
```

| TTL 参数 | 说明 |
|----------|------|
| `expireAfterSeconds: 0` | 根据时间字段值过期 |
| `expireAfterSeconds: 604800` | 文档写入后 7 天过期 |
| 底层 | 后台每 **60s** 清理一次过期文档 |

> ⚠️ TTL 索引**有 60s 延迟**，不能用于精确的定时任务。

---

### Q36：如何设计商品库存系统？

```java
// 商品集合
@Document(collection = "products")
public class Product {
    @Id
    private String id;
    private String name;
    private BigDecimal price;

    @Version  // 乐观锁注解
    private Long version;

    private Integer stock;         // 库存
    private Integer soldCount;     // 已售
}

// 扣减库存（使用 $inc 原子操作 + 条件检查）
public boolean deductStock(String productId, int quantity) {
    Query query = new Query(Criteria.where("_id").is(productId)
        .and("stock").gte(quantity));  // 库存充足才扣减
    Update update = new Update()
        .inc("stock", -quantity)
        .inc("soldCount", quantity);
    WriteResult result = mongoTemplate.updateFirst(query, update, Product.class);
    return result.getModifiedCount() > 0;
}
```

> 💡 纯 MongoDB 库存方案适合**非金融级**场景（秒杀），金融级建议使用 Redis + MySQL 方案。

---

### Q37：实时排行榜如何实现？

```javascript
// 使用聚合管道计算每日排行榜
db.scores.aggregate([
  { $match: { date: ISODate("2026-07-22") } },
  {
    $group: {
      _id: "$userId",
      totalScore: { $sum: "$score" },
      gameCount: { $sum: 1 }
    }
  },
  { $sort: { totalScore: -1 } },
  { $limit: 100 },
  {
    $lookup: {
      from: "users",
      localField: "_id",
      foreignField: "_id",
      as: "userInfo"
    }
  },
  { $unwind: "$userInfo" },
  {
    $project: {
      rank: { $add: ["$$ROUND", 1] },  // 排名（需用 $setWindowFields）
      userId: "$_id",
      userName: "$userInfo.name",
      totalScore: 1
    }
  }
]);

// 在 5.0+ 版本，可使用 $setWindowFields 实现排名
db.scores.aggregate([
  { $match: { date: ISODate("2026-07-22") } },
  { $group: { _id: "$userId", totalScore: { $sum: "$score" } } },
  { $setWindowFields: {
      sortBy: { totalScore: -1 },
      output: { rank: { $rank: {} } }
  }},
  { $limit: 100 }
]);
```

---

### Q38：地理位置搜索如何实现？

```java
// 创建 2dsphere 索引
@Document(collection = "shops")
public class Shop {
    @Id
    private String id;
    private String name;
    private String category;

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private double[] location;  // [lng, lat]
}

// 附近搜索（按距离排序 + 分页）
public List<Shop> findNearby(double lng, double lat, double maxDistanceKm, int limit) {
    Point point = new Point(lng, lat);
    Distance distance = new Distance(maxDistanceKm, Metrics.KILOMETERS);
    Circle circle = new Circle(point, distance);

    Query query = new Query()
        .addCriteria(Criteria.where("location").withinSphere(circle))
        .with(Sort.by(Sort.Direction.ASC, "location"))
        .limit(limit);

    return mongoTemplate.find(query, Shop.class);
}

// Shell 查询
// db.shops.createIndex({ location: "2dsphere" });
// db.shops.find({
//   location: {
//     $near: {
//       $geometry: { type: "Point", coordinates: [116.4, 39.9] },
//       $maxDistance: 5000  // 5km
//     }
//   }
// });
```

---

### Q39：如何处理 MongoDB 慢查询？

```javascript
// 1. 开启慢查询日志
db.setProfilingLevel(1, 100);  // 记录超过 100ms 的查询

// 2. 查看慢查询
db.system.profile.find().sort({ millis: -1 }).limit(10).pretty();

// 3. 常见优化措施
// 3.1 添加索引
db.users.createIndex({ email: 1 });

// 3.2 使用投影
db.users.find({ status: "active" }, { name: 1, email: 1 });

// 3.3 分批查询，避免大结果集
db.users.find().limit(1000).batchSize(500);

// 3.4 索引命中检查
db.collection.find(query).explain("executionStats");

// 3.5 聚合管道优化（$match 提前过滤）
// 错误：先 $skip 再 $match
// 正确：先 $match 缩小范围，再 $group/$sort
```

> 💡 慢查询优化的第一原则：**查看 explain() 确认是否走了索引**。

---

### Q40：海量数据迁移方案？

```bash
# 1. mongodump/mongorestore（适合小规模）
mongodump --host localhost --db mydb --out ./backup/
mongorestore --host target --db mydb ./backup/mydb/

# 2. mongoexport/mongoimport（适合跨版本/跨平台）
mongoexport --db mydb --collection users --out users.json
mongoimport --db mydb_new --collection users --file users.json

# 3. 对于 TB 级数据，使用 mongosync（MongoDB 官方工具）
# 或自定义 ETL 管道
```

**零停机迁移最佳实践：**

```
1. 搭建目标集群，开启同步
2. 使用双写（应用同时写两个集群）
3. 数据校验（对比文档数、checksum）
4. 读流量灰度切换（10% → 50% → 100%）
5. 停掉旧集群写流量，保留只读作为回退
```

---

### Q41：如何实现 MongoDB 数据备份和恢复？

```java
// 使用 mongodump 和 oplog 实现增量备份

// 全量备份 + oplog
mongodump --host localhost:27017 \
  --oplog \
  --out /backup/full_backup_$(date +%Y%m%d)

// 增量备份（基于时间点）
mongodump --host localhost:27017 \
  --db local \
  --collection oplog.rs \
  --query '{ "ts": { $gt: Timestamp(lastBackupTs, 1) } }' \
  --out /backup/incremental/

// 恢复
mongorestore --host target:27017 \
  --oplogReplay \
  /backup/full_backup/
```

> 💡 生产环境建议使用 **MongoDB Ops Manager** 或 **Atlas** 的自动备份功能。

---

### Q42：Spring Data MongoDB 整合要点？

```xml
<!-- pom.xml 依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
```

```yaml
# application.yml
spring:
  data:
    mongodb:
      uri: mongodb://user:password@localhost:27017/mydb?authSource=admin
      # 或分别配置
      # host: localhost
      # port: 27017
      # database: mydb
      # username: user
      # password: password
```

```java
// MongoRepository 方式
public interface UserRepository extends MongoRepository<User, String> {
    List<User> findByName(String name);
    List<User> findByAgeBetween(int from, int to);
    Page<User> findByStatus(String status, Pageable pageable);
}

// MongoTemplate 方式（更灵活）
@Autowired
private MongoTemplate mongoTemplate;

public void complexQuery() {
    Query query = new Query();
    query.addCriteria(Criteria.where("age").gte(18).lte(60));
    query.addCriteria(Criteria.where("tags").in("java", "mongodb"));
    query.with(Sort.by(Sort.Direction.DESC, "createdAt"));
    query.skip(0).limit(20);

    List<User> users = mongoTemplate.find(query, User.class);
}
```

| 方式 | 优点 | 适用场景 |
|------|------|---------|
| MongoRepository | 快速 CRUD，自动实现 | 简单操作 |
| MongoTemplate | 灵活，支持聚合/复杂查询 | 复杂业务逻辑 |

---

## 四、手写代码/配置文件题（6题）

### Q43：Spring Boot + MongoDB 完整配置

```java
// 1. 依赖引入（pom.xml）
// <dependency>
//     <groupId>org.springframework.boot</groupId>
//     <artifactId>spring-boot-starter-data-mongodb</artifactId>
// </dependency>

// 2. 实体类
@Data
@Document(collection = "employees")
@CompoundIndex(def = "{'department': 1, 'salary': -1}")
public class Employee {
    @Id
    private String id;

    @Indexed
    private String department;

    private String name;
    private Double salary;
    private LocalDate hireDate;

    @Field("email")
    private String email;
}

// 3. 自定义 MongoClientSettings
@Configuration
public class MongoConfig {
    @Bean
    public MongoClientSettings mongoClientSettings() {
        return MongoClientSettings.builder()
            .applyToClusterSettings(builder ->
                builder.hosts(Arrays.asList(
                    new ServerAddress("node1", 27017),
                    new ServerAddress("node2", 27017),
                    new ServerAddress("node3", 27017)
                ))
            )
            .credential(MongoCredential.createScramSha256Credential(
                "app_user", "admin", "password".toCharArray()))
            .readPreference(ReadPreference.secondaryPreferred())
            .writeConcern(WriteConcern.MAJORITY)
            .retryWrites(true)
            .applyToConnectionPoolSettings(builder ->
                builder.maxSize(100).minSize(10)
                    .maxConnectionIdleTime(30, TimeUnit.SECONDS)
            )
            .build();
    }
}
```

---

### Q44：MongoDB Shell 脚本——分页 + 排序

```javascript
// 分页查询工具函数
function paginate(dbName, collName, query, sortField, sortOrder, page, pageSize) {
    var db = db.getSiblingDB(dbName);
    var coll = db.getCollection(collName);

    var sortObj = {};
    sortObj[sortField] = sortOrder || -1;

    var total = coll.countDocuments(query);
    var docs = coll.find(query)
        .sort(sortObj)
        .skip((page - 1) * pageSize)
        .limit(pageSize)
        .toArray();

    return {
        "page": page,
        "pageSize": pageSize,
        "total": total,
        "totalPages": Math.ceil(total / pageSize),
        "data": docs
    };
}

// 使用示例
// paginate("mydb", "users", { status: "active" }, "createdAt", -1, 1, 20);
```

---

### Q45：副本集初始化脚本

```javascript
// 1. 启动三个 mongod 实例
// mongod --replSet rs0 --port 27017 --dbpath data1 --logpath log1.log --fork
// mongod --replSet rs0 --port 27018 --dbpath data2 --logpath log2.log --fork
// mongod --replSet rs0 --port 27019 --dbpath data3 --logpath log3.log --fork

// 2. 登录任意节点，初始化副本集
rs.initiate({
  _id: "rs0",
  members: [
    { _id: 0, host: "localhost:27017", priority: 3 },
    { _id: 1, host: "localhost:27018", priority: 2 },
    { _id: 2, host: "localhost:27019", priority: 1, arbiterOnly: false }
  ]
});

// 3. 添加仲裁节点（可选）
// rs.addArb("localhost:27020");

// 4. 查看状态
rs.status();

// 5. Spring Data 连接副本集
// spring.data.mongodb.uri=mongodb://host1:27017,host2:27018,host3:27019/mydb
```

---

### Q46：分片集群初始化脚本

```javascript
// 1. 启动配置副本集
// mongod --configsvr --replSet configRS --port 27019 --dbpath configData --fork

// 2. 初始化配置副本集
rs.initiate({
  _id: "configRS",
  configsvr: true,
  members: [
    { _id: 0, host: "localhost:27019" }
  ]
});

// 3. 启动两个分片副本集
// mongod --shardsvr --replSet shard1 --port 27020 --dbpath shard1Data --fork
// mongod --shardsvr --replSet shard2 --port 27021 --dbpath shard2Data --fork

// 4. 启动 mongos 路由
// mongos --configdb configRS/localhost:27019 --port 27017 --fork

// 5. 登录 mongos，添加分片
sh.addShard("shard1/localhost:27020");
sh.addShard("shard2/localhost:27021");

// 6. 开启分片
sh.enableSharding("mydb");

// 7. 选择分片策略
sh.shardCollection("mydb.users", { "userId": "hashed" });      // 哈希分片
sh.shardCollection("mydb.orders", { "orderDate": 1, "_id": 1 }); // 范围分片

// 8. 查看分布
sh.status();
```

---

### Q47：聚合管道——电商订单统计

```javascript
// 需求：统计每月各品类销售额 Top 5
db.orders.aggregate([
  // 1. 筛选已支付订单
  { $match: { status: "paid" } },

  // 2. 按月份和品类分组
  {
    $group: {
      _id: {
        year: { $year: "$paidAt" },
        month: { $month: "$paidAt" },
        category: "$category"
      },
      totalSales: { $sum: "$amount" },
      orderCount: { $sum: 1 },
      avgAmount: { $avg: "$amount" }
    }
  },

  // 3. 排序
  { $sort: { "_id.year": -1, "_id.month": -1, totalSales: -1 } },

  // 4. 分组取 Top 5（使用 $push + $slice）
  {
    $group: {
      _id: { year: "$_id.year", month: "$_id.month" },
      topCategories: {
        $push: {
          category: "$_id.category",
          sales: "$totalSales",
          orders: "$orderCount"
        }
      }
    }
  },
  {
    $project: {
      year: "$_id.year",
      month: "$_id.month",
      topCategories: { $slice: ["$topCategories", 5] },
      _id: 0
    }
  }
]);
```

---

### Q48：MongoDB 安全认证配置

```yaml
# mongod.conf 安全配置
security:
  authorization: enabled     # 开启 RBAC 授权
  authenticationMechanisms: SCRAM-SHA-256

# 创建管理员
use admin;
db.createUser({
  user: "admin",
  pwd: "admin123",
  roles: [{ role: "root", db: "admin" }]
});

# 创建应用用户
use mydb;
db.createUser({
  user: "app_user",
  pwd: "app_pass",
  roles: [
    { role: "readWrite", db: "mydb" },
    { role: "read", db: "logs" }
  ],
  authenticationRestrictions: [
    { clientSource: ["192.168.1.0/24"] }  # IP 限制
  ]
});

# Spring Data 认证连接
spring.data.mongodb.uri=mongodb://app_user:app_pass@localhost:27017/mydb?authSource=mydb&authMechanism=SCRAM-SHA-256
```

---

## 五、系统设计题（4题）

### Q49：设计一个短链接系统

```javascript
// MongoDB Schema
// 短链接集合
{
  "_id": "abc123",           // 短码（作为 _id）
  "longUrl": "https://...",
  "userId": "u001",
  "createdAt": ISODate(),
  "expireAt": ISODate(),     // TTL 索引
  "clickCount": 0
}

// 访问日志集合
{
  "_id": ObjectId(),
  "shortCode": "abc123",
  "ip": "192.168.1.1",
  "ua": "Mozilla/5.0...",
  "referer": "https://...",
  "clickedAt": ISODate()
}
```

**设计要点：**
1. 短码生成：Base62 编码（62^6 ≈ 568 亿组合）
2. 使用 `_id` 作为短码，天然唯一索引，无需额外索引
3. 使用 TTL 索引自动清理过期链接
4. 访问日志使用 **Capped Collection** 或**分片**存储
5. 定期聚合统计（按小时/天/周统计点击量）

---

### Q50：设计一个日志收集与分析系统

```javascript
// 日志集合 Schema（写优化）
// 分片键使用时间戳（范围分片）+ 按 _id 哈希
// 索引设计
// { timestamp: -1, level: 1 }
// { source: 1, timestamp: -1 }

// 日志文档
{
  "_id": ObjectId(),
  "timestamp": ISODate(),
  "level": "ERROR",        // INFO, WARN, ERROR, FATAL
  "source": "order-service",
  "traceId": "abc-def-ghi",
  "message": "Order processing failed",
  "stackTrace": "java.lang...",
  "metadata": {
    "userId": "u001",
    "orderId": "o001",
    "duration": 1523        // ms
  }
}
```

**架构要点：**
| 组件 | 技术选型 | 角色 |
|------|----------|------|
| 采集层 | Fluentd/Logstash | 采集日志 |
| 缓冲层 | Kafka | 削峰填谷 |
| 存储层 | MongoDB 分片集群 | 日志存储 |
| 检索层 | Elasticsearch（可选） | 全文搜索 |
| 展示层 | Grafana/Kibana | 可视化 |

---

### Q51：设计一个电子商务订单系统

```javascript
// 订单集合
// 使用 userId 作为分片键（按用户哈希分片）
{
  "_id": ObjectId(),
  "orderNo": "ORD202607220001",  // 业务订单号（唯一索引）
  "userId": "u001",
  "status": "pending",           // pending→paid→shipped→delivered→completed
  "items": [
    { "productId": "p001", "name": "iPhone", "price": 6999, "quantity": 1 }
  ],
  "totalAmount": 6999,
  "address": { "province": "北京", "city": "北京" },
  "paidAt": ISODate(),
  "createdAt": ISODate()
}
```

**关键设计：**
1. 事务：支付回调使用**事务**保证订单状态和库存一致性
2. 索引：`{ userId: 1, createdAt: -1 }`（用户订单查询）
3. 索引：`{ status: 1, createdAt: -1 }`（待处理订单）
4. 分片：`userId`（用户分片，避免跨分片事务）
5. 归档：订单完结 90 天后移至归档集合

---

### Q52：设计 IoT 设备数据采集系统

```javascript
// 时序数据 Schema（桶模型 Bucket Pattern）
// 将 1 小时的数据聚合为一个文档
{
  "deviceId": "d001",
  "bucketStart": ISODate("2026-07-22T08:00:00Z"),
  "bucketEnd": ISODate("2026-07-22T09:00:00Z"),
  "readings": [
    { "t": ISODate("2026-07-22T08:00:05Z"), "temp": 25.3, "humidity": 60 },
    { "t": ISODate("2026-07-22T08:00:10Z"), "temp": 25.4, "humidity": 61 },
    // ...
  ],
  "summary": {
    "avgTemp": 25.6,
    "maxTemp": 30.1,
    "minTemp": 22.3,
    "readingCount": 3600
  }
}

// 索引
// { deviceId: 1, bucketStart: -1 }
// TTL 索引保留 30 天
```

> 💡 **桶模式（Bucket Pattern）**是 MongoDB 处理时序数据的最佳实践，减少文档数约 3600 倍（相比每条记录一个文档）。

---

## 六、常见坑点与最佳实践

### 常见坑点（Pitfalls）

| 坑 | 现象 | 解决方案 |
|----|------|---------|
| **未建索引** | 全表扫描，查询慢 | 用 `explain()` 检查，添加索引 |
| **索引过多** | 写入性能下降 | 合并冗余索引，删除无用索引 |
| **分片键选择错误** | 数据倾斜，热点分片 | 选择高基数且均匀分布的分片键 |
| **忘记 `$unwind` 空数组** | 数据丢失 | 使用 `preserveNullAndEmptyArrays: true` |
| **BSON 文档超 16MB** | 写入报错 | 使用 GridFS 或拆分子文档 |
| **oplog 太小** | Secondary 无法同步 | 设置合理 oplog 大小 |
| **读取偏好不一致** | 读取到旧数据 | 根据一致性需求选择 Read Preference |
| **聚合内存溢出** | 聚合管道报错 | 使用 `allowDiskUse: true` |
| **忽略连接池** | 连接数过高/过低 | 合理配置 `maxPoolSize` 和 `minPoolSize` |
| **TTL 延迟** | 数据过期后未立即删除 | TTL 有 60s 后台间隔，不能做精确定时 |

### 最佳实践（Best Practices）

1. **索引策略**
   - 确保每个查询都有索引
   - 使用**覆盖查询**（Covered Query）减少 IO
   - 复合索引遵循**最左前缀原则**
   - 定期使用 `db.collection.getIndexes()` 审查索引

2. **文档设计**
   - **内嵌（Embed）** 优先，**引用（Reference）** 必要时使用
   - 避免过深的文档嵌套（建议 ≤ 3 层）
   - 使用**枚举**或 **Code** 替代冗余字符串
   - 使用 `_id` 作为分片键的组成部分

3. **写入优化**
   - 批量写入使用 `insertMany()` 或 `bulkWrite()`
   - 控制 Write Concern（非关键数据用 `w: 1`）
   - 使用 `$inc` 等原子操作避免读-改-写模式

4. **读取优化**
   - 总是指定**投影**（`projection`），不返回不需要的字段
   - 大结果集使用**游标**遍历
   - 聚合管道将 `$match` 放在最前面

5. **运维**
   - 监控 `wiredTiger.cache` 水位
   - 设置慢查询阈值（100ms）
   - 定期备份并测试恢复流程
   - 使用 **MongoDB Ops Manager** 或 **Atlas** 管理

---

## 七、面试回答模板（Top 5）

### 模板 1：谈谈 MongoDB 和 MySQL 的区别

> "MongoDB 是一种**文档型 NoSQL 数据库**，而 MySQL 是**关系型数据库**。
>
> 主要区别在于：
> 1. **数据模型**：MongoDB 用 BSON 文档（Schema-less），MySQL 用表结构（固定 Schema）
> 2. **扩展性**：MongoDB 原生支持水平分片，MySQL 需要分库分表中间件
> 3. **事务**：MySQL 强 ACID，MongoDB 4.0+ 也支持多文档事务，但性能不如 MySQL
> 4. **JOIN**：MySQL JOIN 性能好，MongoDB 的 `$lookup` 性能一般
>
> **MongoDB 适用于**：日志系统、内容管理、IoT 数据、用户画像等文档型场景
> **MySQL 适用于**：金融、ERP、库存等强一致性和复杂关系查询场景"

### 模板 2：MongoDB 索引的工作原理

> "MongoDB 索引底层使用 **B-Tree** 数据结构，将索引字段的值按排序顺序组织成树形结构。
>
> 查找流程：
> 1. 从 B-Tree 根节点开始二分查找
> 2. 对比节点中的键值，决定进入哪个子节点
> 3. 直到叶节点找到对应文档的 **磁盘指针**（Record ID）
> 4. 通过指针读取完整文档（除非是覆盖查询）
>
> 复合索引遵循**最左前缀原则**，例如索引 `{a: 1, b: 1, c: 1}` 可以支持 `{a:...}`、`{a:..., b:...}` 的查询，但不能单独支持 `{b:...}`。

### 模板 3：副本集的工作原理

> "MongoDB 副本集通过 **oplog 复制** 和 **心跳选举** 实现高可用。
>
> 写入流程：
> 1. 客户端只能写入 Primary
> 2. Primary 记录写操作到 **oplog**
> 3. Secondary 通过 **tail oplog** 拉取并重放
> 4. 达到指定 Write Concern 后返回客户端
>
> 选举流程：
> 1. Secondary 检测 Primary 心跳超时（默认 10s）
> 2. priority 最高 + oplog 最新的节点发起选举
> 3. 获得超过半数节点投票后成为新 Primary
> 4. 旧 Primary 恢复后会**回滚**未同步的写入"

### 模板 4：分片集群扩展策略

> "MongoDB 分片集群通过 **分片键** 将数据分布到多个分片节点。
>
> 架构三组件：
> - **mongos**：路由节点，将客户端请求转发到对应切片
> - **Config Server**：存储分片键范围到分片的映射
> - **Shard**：每个 Shard 是一个副本集，存储实际数据
>
> 分片策略选择：
> - **哈希分片**：数据分布均匀，适合高吞吐写入场景
> - **范围分片**：范围查询高效，但可能导致热点
>
> 分片键选择原则：高基数、写分布均匀、避免单调递增。"

### 模板 5：MongoDB 写安全级别

> "MongoDB 通过 **Write Concern** 控制写入的持久性级别：
>
> - `w: 0`：不等待确认，性能最高但有丢失风险
> - `w: 1`（默认）：Primary 确认即可
> - `w: majority`：大多数节点确认，安全性最高
> - `j: true`：写入 Journal 后才确认
>
> 生产环境推荐：
> - 核心数据：`w: majority` 或 `w: majority + j: true`
> - 日志等非关键数据：`w: 1` 或 `w: 0`
>
> **读偏好**（Read Preference）决定读请求走哪个节点：
> - `primary`：强一致性但可用性降低
> - `secondary` 或 `secondaryPreferred`：分担读压力"

---

## 八、快速查漏补缺 CheckList

### 基础概念
- [ ] 理解 MongoDB 文档模型和 BSON 格式
- [ ] 掌握 CRUD 基本操作和查询操作符
- [ ] 知道 ObjectId 的生成规则
- [ ] 了解聚合管道各阶段用法
- [ ] 理解 B-Tree 索引原理和最左前缀原则
- [ ] 知道有哪些索引类型（唯一/复合/文本/地理/TTL/哈希）
- [ ] 会用 `explain()` 分析查询性能

### 副本集
- [ ] 知道副本集的三个角色
- [ ] 理解 oplog 同步机制
- [ ] 掌握选举算法和触发条件
- [ ] 理解 Read Preference 和 Write Concern
- [ ] 知道如何配置副本集

### 分片集群
- [ ] 理解分片集群三组件架构
- [ ] 知道 Hash vs Range 分片策略
- [ ] 掌握分片键选择原则
- [ ] 理解 Chunk 和 Balancer 机制
- [ ] 知道分片集群的局限性

### 存储引擎
- [ ] 理解 WiredTiger MVCC 机制
- [ ] 知道 WT 缓存淘汰策略
- [ ] 了解 Journal（WAL）工作流程
- [ ] 知道数据压缩算法选择

### 实战整合
- [ ] 会用 Spring Data MongoDB（Repository + Template）
- [ ] 掌握 GridFS 使用场景
- [ ] 理解安全认证配置（SCRAM + RBAC）
- [ ] 知道 TTL 索引和 Capped Collection
- [ ] 了解 Bucket Pattern 设计模式

### 运维
- [ ] 知道慢查询定位和优化
- [ ] 了解备份恢复策略（mongodump/mongorestore）
- [ ] 理解 WT 缓存监控指标
- [ ] 知道如何合理设置连接池

---

> **面试建议：**
> - 一面：重点准备**基础概念速答** + **实战场景题**，证明会用
> - 二面：重点准备**原理剖析** + **系统设计题**，证明懂原理
> - 三面/终面：重点准备**设计题** + **常见坑点**，证明有架构视野
> - 所有代码示例要求**能在面试现场手写**（Core 级别）
