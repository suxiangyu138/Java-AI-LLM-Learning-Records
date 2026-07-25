# MongoDB 面试问答清单
> 🎯 基于项目实战清单，涵盖面试高频问题与完美解答方案，帮助你在面试中脱颖而出。

## 目录
1. [基础概念与核心原理](#1-基础概念与核心原理)
2. [项目实战深度问答](#2-项目实战深度问答)
3. [进阶与系统设计](#3-进阶与系统设计)
4. [场景题与故障排查](#4-场景题与故障排查)

---

## 1. 基础概念与核心原理

> 💡 面试官在这一环节考察你的基本功是否扎实

### Q1：MongoDB 和 MySQL 的核心区别是什么？什么场景选 MongoDB？
**面试官意图：** 考察数据存储选型能力，判断你是否能根据业务场景选择合适的数据库。

**完美解答：**

| 对比维度 | MongoDB | MySQL |
|----------|---------|-------|
| **数据模型** | 文档型（BSON JSON） | 关系型（行 + 列） |
| **Schema** | 动态无模式（同集合内文档结构可不同） | 固定模式（表结构预先定义） |
| **关联查询** | 弱（不支持 JOIN，需用 `$lookup` 或应用层关联） | 强（各种 JOIN 灵活关联） |
| **扩展方式** | 原生水平扩展（分片 + 副本集） | 以垂直扩展为主，分库分表较复杂 |
| **事务支持** | 支持（4.0+ 多文档事务），但较弱 | 强 ACID 事务 |
| **适合数据结构** | 嵌套文档、数组、动态字段 | 结构化、范式化数据 |
| **写入性能** | 高（无事务开销时可达到很高吞吐） | 中（事务、锁、约束的开销） |

**选型建议：**

| 选择 MongoDB 的场景 | 选择 MySQL 的场景 |
|--------------------|-------------------|
| 内容管理（文章、评论、帖子） | 用户账户、权限系统 |
| 日志系统、埋点数据 | 订单、支付流水 |
| 产品目录、商品 SKU（多变的属性） | 财务数据、强一致场景 |
| 实时分析、聚合报表 | ERP、CRM 等企业系统 |
| IoT 设备数据 | 库存管理、复杂事务 |

> 💡 **面试金句**："MongoDB 用空间换灵活，MySQL 用约束换安全。当数据模型变化频繁、关联查询少、写入压力大时，MongoDB 更合适；当数据强一致性、复杂事务、复杂关联查询是刚需时，MySQL 不可替代。"

---

### Q2：MongoDB 的文档模型设计中，嵌套和引用怎么选择？
**面试官意图：** 考察 MongoDB 数据建模的核心能力，这是用好 MongoDB 的关键之一。

**完美解答：**

**嵌套与引用对比：**

| 维度 | 嵌套（Embedding） | 引用（Reference） |
|------|------------------|------------------|
| **结构** | 子文档内嵌到父文档中 | 父文档中存子文档 ID |
| **读取** | 一次查询获取全部数据 | 可能需要二次查询（`$lookup`） |
| **写入** | 修改嵌套数据需更新整个文档 | 分别更新，互不影响 |
| **数据大小** | 单个文档建议不超过 16MB | 无限制 |
| **数据冗余** | 有冗余（数据重复） | 无冗余（规范化） |

**选择原则：**

```
是否经常需要一起读取全部数据？
├── 是 → 采用嵌套
│   ├── 例子：用户 + 地址（每次登录都查地址）
│   └── 例子：文章 + 前几条评论（首页展示）
└── 否 → 采用引用
    ├── 例子：用户 + 订单（分开查询）
    └── 例子：文章 + 完整评论列表（按需分页加载）
```

**嵌套文档示例：**
```json
{
  "_id": "user_001",
  "name": "张三",
  "profile": {
    "age": 28,
    "avatar": "https://xxx.com/avatar.jpg"
  },
  "addresses": [
    { "type": "home", "city": "北京", "detail": "朝阳区xx路" },
    { "type": "work", "city": "北京", "detail": "海淀区yy路" }
  ]
}
```

**引用文档示例：**
```json
// 用户文档
{
  "_id": "user_001",
  "name": "张三"
}

// 帖子文档（引用用户）
{
  "_id": "post_001",
  "title": "MongoDB 入门指南",
  "author_id": "user_001",  // 引用
  "content": "..."
}
```

> 🎯 **核心原则**：**一起读就嵌套，分开读就引用。** 嵌套不是免费的——冗余数据的一致性需要业务层保证。

---

### Q3：MongoDB 的聚合管道（Aggregation Pipeline）是什么？常用阶段有哪些？
**面试官意图：** 考察对 MongoDB 核心数据处理能力的理解，聚合管道是面试高频考点。

**完美解答：**

**聚合管道是一系列数据处理阶段组成的链式处理流程，类似 Unix 的管道操作 `|`。** 数据依次经过每个阶段，每个阶段对上一步的结果进行变换。

**常用聚合阶段：**

| 阶段 | 作用 | 相当于 SQL | 使用频率 |
|------|------|-----------|:--------:|
| `$match` | 过滤文档 | `WHERE` | ⭐⭐⭐⭐⭐ |
| `$group` | 分组聚合 | `GROUP BY` | ⭐⭐⭐⭐⭐ |
| `$sort` | 排序 | `ORDER BY` | ⭐⭐⭐⭐ |
| `$project` | 投影（选字段、计算新字段） | `SELECT` | ⭐⭐⭐⭐ |
| `$lookup` | 左外关联（类似 JOIN） | `LEFT JOIN` | ⭐⭐⭐ |
| `$unwind` | 数组展开（一个数组元素变成一条文档） | UNNEST | ⭐⭐⭐ |
| `$limit` | 限制文档数 | `LIMIT` | ⭐⭐⭐ |
| `$skip` | 跳过文档数 | `OFFSET` | ⭐⭐⭐ |
| `$bucket` | 分桶聚合 | `CASE WHEN + GROUP BY` | ⭐⭐ |
| `$addFields` | 添加字段 | 计算列 | ⭐⭐ |

**实战示例——电商订单统计：**
```javascript
db.orders.aggregate([
  // Stage 1: 过滤近 30 天的已支付订单
  { $match: { 
      status: "paid", 
      pay_time: { $gte: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000) }
  }},
  // Stage 2: 按分类分组统计
  { $group: {
      _id: "$category",
      total_sales: { $sum: "$amount" },
      order_count: { $sum: 1 },
      avg_amount: { $avg: "$amount" },
      max_amount: { $max: "$amount" }
  }},
  // Stage 3: 按销售额降序排列
  { $sort: { total_sales: -1 }},
  // Stage 4: 计算占比
  { $project: {
      category: "$_id",
      total_sales: 1,
      order_count: 1,
      avg_amount: { $round: ["$avg_amount", 2] },
      percentage: { $round: [{ $multiply: [{ $divide: ["$total_sales", { $sum: "$total_sales" }] }, 100] }, 1] }
  }},
  // Stage 5: 取 Top 10
  { $limit: 10 }
]);
```

> 💡 **注意**：聚合管道中阶段的顺序直接影响性能。**`$match` 和 `$limit` 尽量放在前面**，尽早过滤掉不需要的数据，减少后续阶段处理的数据量。

---

## 2. 项目实战深度问答

> 💡 面试官会深挖你的项目细节，验证你"真的做过"而不是"背过"

### Q4：描述你设计的一个 MongoDB 社区论坛系统的数据模型
**面试官意图：** 考察综合建模能力和真实项目经验。

**完美解答：**

**社区论坛的核心模块和文档设计：**

```json
// 1. 用户文档（引用设计）
{
  "_id": ObjectId("..."),
  "username": "张三",
  "email": "zhangsan@example.com",
  "password_hash": "$2b$12$...",
  "avatar": "https://xxx.com/avatar.jpg",
  "role": "user",  // user / admin / moderator
  "stats": {
    "post_count": 42,
    "comment_count": 156,
    "follower_count": 89
  },
  "created_at": ISODate("2024-01-15T08:00:00Z"),
  "is_deleted": false  // 软删除
}

// 2. 帖子文档（部分嵌套 + 部分引用）
{
  "_id": ObjectId("..."),
  "title": "MongoDB 数据建模最佳实践",
  "content": "在实际项目中...",
  "author_id": ObjectId("user_xxx"),
  "tags": ["MongoDB", "NoSQL", "数据建模"],
  "is_pinned": false,
  "is_essence": true,
  "status": "published",
  "stats": {
    "views": 10240,
    "likes": 567,
    "comments": 89
  },
  // 嵌套前 3 条热门评论做展示
  "hot_comments_preview": [
    {
      "user_id": ObjectId("user_yyy"),
      "username": "李四",
      "content": "写得非常好！",
      "likes": 23,
      "created_at": ISODate("2024-01-16T10:00:00Z")
    }
  ],
  "created_at": ISODate("2024-01-15T10:00:00Z"),
  "updated_at": ISODate("2024-01-16T12:00:00Z"),
  "is_deleted": false
}

// 3. 评论文档（独立集合 + 引用）
{
  "_id": ObjectId("..."),
  "post_id": ObjectId("post_xxx"),
  "author_id": ObjectId("user_yyy"),
  "content": "写得非常好！",
  "parent_id": null,          // 父评论 ID（二级回复用）
  "reply_to": null,           // 回复的用户 ID
  "likes": 23,
  "status": "visible",
  "created_at": ISODate("2024-01-16T10:00:00Z"),
  "is_deleted": false
}

// 4. 点赞关系文档（独立的关联集合）
{
  "_id": ObjectId("..."),
  "target_type": "post",     // post / comment
  "target_id": ObjectId("post_xxx"),
  "user_id": ObjectId("user_yyy"),
  "created_at": ISODate("2024-01-16T10:00:00Z")
}
// 唯一复合索引：{ target_type: 1, target_id: 1, user_id: 1 }
```

**设计理由：**

| 设计点 | 原因 |
|--------|------|
| **评论独立集合 + 引用** | 评论可能成千上万，嵌套到帖子中会导致文档过大（超过 16MB） |
| **hot_comments_preview 部分嵌套** | 首页展示需要前几条评论，单独查一次太浪费，冗余存储但减少了查询 |
| **用户统计数据冗余** | 每次展示帖子都统计一次发帖数太慢，冗余存储定期更新 |
| **点赞关系独立集合 + 唯一索引** | 原子更新 `$inc` 帖子点赞数快速统计，唯一索引保证不重复点赞 |
| **软删除** | 所有核心数据都标记删除，保留历史 |

> 💡 **核心权衡**：这个设计是有意冗余的（用户统计数据、热门评论 preview），用写入时的维护成本换取了读取时的高性能，是 MongoDB 的典型设计思路。

---

### Q5：如何处理 MongoDB 中的并发更新冲突？比如扣减库存、点赞操作
**面试官意图：** 考察对 MongoDB 原子操作和并发控制的理解。

**完美解答：**

**MongoDB 的原子操作（文档级别的原子性）：**

在 MongoDB 中，对**单个文档**的更新是原子性的，可以利用这一点实现高效并发控制：

**1. 商品库存扣减（原子操作，避免超卖）：**
```javascript
// 原子扣减库存：$inc 是原子操作
db.products.updateOne(
  { _id: "product_001", stock: { $gt: 0 } },  // 条件：库存 > 0
  { $inc: { stock: -1 } }
);

// 检查结果
// matchedCount 为 1 表示扣减成功，为 0 表示库存不足
```

```java
// Java 实现
public boolean deductStock(String productId) {
    Query query = new Query(Criteria.where("_id").is(productId)
        .and("stock").gt(0));
    Update update = new Update().inc("stock", -1);

    UpdateResult result = mongoTemplate.updateFirst(query, update, "products");
    return result.getMatchedCount() > 0;
}
```

**2. 点赞操作（防止重复点赞 + 原子更新）：**
```java
@Service
public class LikeService {

    @Autowired
    private MongoTemplate mongoTemplate;

    public boolean toggleLike(String userId, String postId) {
        // 唯一索引保证：{ target_type: 1, target_id: 1, user_id: 1 }
        // 只有不存在才能插入成功
        Query query = new Query(Criteria.where("target_type").is("post")
            .and("target_id").is(postId).and("user_id").is(userId));

        if (!mongoTemplate.exists(query, "likes")) {
            // 插入点赞关系
            LikeDoc like = new LikeDoc("post", postId, userId);
            try {
                mongoTemplate.insert(like, "likes");
                // 原子更新点赞数
                mongoTemplate.updateFirst(
                    new Query(Criteria.where("_id").is(postId)),
                    new Update().inc("stats.likes", 1),
                    "posts"
                );
                return true; // 已点赞
            } catch (DuplicateKeyException e) {
                // 已存在，幂等处理
                return true;
            }
        } else {
            // 取消点赞：删除关系文档
            mongoTemplate.remove(query, "likes");
            mongoTemplate.updateFirst(
                new Query(Criteria.where("_id").is(postId)),
                new Update().inc("stats.likes", -1),
                "posts"
            );
            return false; // 取消点赞
        }
    }
}
```

**3. 乐观锁版库存扣减（适合秒杀场景）：**
```javascript
// 使用 version 字段做乐观锁
db.products.updateOne(
  { _id: "product_001", stock: { $gt: 0 }, version: 1 }, // 版本号匹配
  { $inc: { stock: -1 }, $set: { version: 2 } }
);
// 如果 version 不匹配（被其他线程修改过），更新失败，重试
```

> ⚠️ **注意**：MongoDB 的文档级原子性意味着：如果你把库存和商品信息放在同一个文档中，更新库存和更新商品信息可以在一次操作中完成。但如果用引用设计（库存独立集合），则需要事务或补偿机制。

---

### Q6：你的后台日志收集系统是如何实现的？百万级日志写入怎么优化？
**面试官意图：** 考察 MongoDB 在大数据量写入场景下的实战经验。

**完美解答：**

**日志系统架构设计：**
```
应用服务 → 日志收集 API → MQ 缓冲 → 批量写入 MongoDB → 聚合分析
```

**核心实现：**

```java
// 1. 日志文档结构
@Document(collection = "app_logs")
@CompoundIndex(def = "{'level': 1, 'timestamp': -1}")
@CompoundIndex(def = "{'app_name': 1, 'timestamp': -1}")
public class AppLog {
    @Id
    private String id;
    private String appName;    // 应用名
    private String level;      // INFO / WARN / ERROR
    private String className;  // 类名
    private String message;    // 日志内容
    private String traceId;    // 链路 ID
    private long costMs;       // 耗时（ms）
    private Date timestamp;
    private Map<String, Object> extra; // 扩展字段
}

// 2. 批量写入优化（避免逐条插入）
@Component
public class LogBatchWriter {
    private static final int BATCH_SIZE = 500;
    private final List<AppLog> buffer = new ArrayList<>();

    @Autowired
    private MongoTemplate mongoTemplate;

    @Scheduled(fixedDelay = 1000) // 每 1 秒批量写入
    public void flush() {
        List<AppLog> batch;
        synchronized (buffer) {
            if (buffer.isEmpty()) return;
            batch = new ArrayList<>(buffer);
            buffer.clear();
        }
        // 批量插入（性能远高于逐条 insert）
        mongoTemplate.insert(batch, AppLog.class);
    }

    public void addLog(AppLog log) {
        synchronized (buffer) {
            buffer.add(log);
            // 如果达到批量阈值，立即触发写入
            if (buffer.size() >= BATCH_SIZE) {
                mongoTemplate.insert(new ArrayList<>(buffer), AppLog.class);
                buffer.clear();
            }
        }
    }
}
```

**性能优化效果对比：**

| 策略 | 10 万条写入时间 | 说明 |
|------|:--------------:|------|
| 逐条插入 | 45 秒 | 每次 insert 都有网络开销 |
| 批量 500 条 | 2.5 秒 | 减少了 95% + 的网络往返 |
| 批量 + 无索引写入 | 1.8 秒 | 建索引后再创建索引更快 |
| 批量 + 写关注 w=0 | 1.2 秒 | 不等待确认，性能最高 |

**索引优化建议：**
```javascript
// 按时间和级别查询，复合索引要按最左前缀原则
db.app_logs.createIndex({ timestamp: -1, level: 1 });

// TTL 索引：自动清理 30 天前的日志
db.app_logs.createIndex(
  { timestamp: 1 },
  { expireAfterSeconds: 30 * 24 * 3600 }
);

// 通配符索引（适合搜索任意字段）
db.app_logs.createIndex({ "$**": "text" });
```

> 📊 **实际效果**：使用批量写入 + TTL 自动清理 + 复合索引，单台 MongoDB 可以稳定支撑每天 2000 万条日志写入，查询延迟在 200ms 以内。

---

## 3. 进阶与系统设计

> 💡 这是拉开差距的环节，考察架构思维

### Q7：MongoDB 的副本集（Replica Set）是如何工作的？故障转移过程是怎样的？
**面试官意图：** 考察对 MongoDB 高可用机制的理解深度。

**完美解答：**

**副本集架构：**

```
┌─────────────────────────────────────┐
│           客户端（Driver）             │
│   连接 Primary 读写 / 从 Secondary 读  │
└────────────────┬────────────────────┘
                 │
    ┌────────────┼────────────┐
    ↓            ↓            ↓
┌─────────┐ ┌─────────┐ ┌─────────┐
│ Primary │→│Secondary│→│Secondary│
│（主）   │←│（从）   │←│（从）   │
│ 可读写  │ │ 只读    │ │ 只读    │
└─────────┘ └─────────┘ └─────────┘
    ↓            ↓            ↓
  心跳 ⇄       心跳 ⇄       心跳 ⇄
    ↓                        ↓
┌─────────────────────────────────────┐
│            Arbiter（可选）            │
│        参与选举，不存数据              │
└─────────────────────────────────────┘
```

**故障转移过程：**

```
1. Primary 宕机
   → Secondary 节点在 10 秒（默认）内未收到心跳
   → 触发选举

2. 选举过程
   → 剩余节点互相通信，确认谁的数据最新
   → 优先级最高的节点发起选举
   → 获得大多数投票（超过半数）的节点成为新 Primary

3. 客户端切换
   → 驱动自动检测到 Primary 变更
   → 自动将读写请求切换到新 Primary
   → 对业务代码透明

4. 原 Primary 恢复
   → 作为 Secondary 重新加入副本集
   → 从新 Primary 同步数据
```

**写关注级别（Write Concern）：**
```javascript
// 写入只需要 Primary 确认（性能最好，但有数据丢失风险）
db.orders.insertOne(order, { writeConcern: { w: 1 } });

// 写入需要 Primary + 1 个 Secondary 确认（推荐）
db.orders.insertOne(order, { writeConcern: { w: "majority" } });

// 写入需要全部节点确认（性能最差，最安全）
db.orders.insertOne(order, { writeConcern: { w: "all" } });
```

> ⚠️ **关键知识**：副本集最少需要 3 个节点（或者 2 个节点 + 1 个仲裁节点）。因为选举需要大多数（超过半数）节点同意，3 节点最多允许 1 个节点宕机。

---

### Q8：MongoDB 的分片（Sharding）集群是如何工作的？
**面试官意图：** 考察 MongoDB 水平扩展方案的理解，这是处理海量数据的核心架构能力。

**完美解答：**

**分片集群组件：**

| 组件 | 角色 | 部署数量 | 说明 |
|------|------|:--------:|------|
| **mongos** | 路由节点 | 2+ | 入口网关，把请求路由到对应分片 |
| **Config Server** | 配置节点 | 3（副本集） | 存储元数据和分片路由信息 |
| **Shard** | 数据节点 | 2+（各副本集） | 实际存储数据的节点组 |

**分片键选择策略：**

| 分片键策略 | 说明 | 优点 | 缺点 |
|-----------|------|------|------|
| **范围分片** | 按值的范围划分（如 1-100 → shard1，101-200 → shard2） | 范围查询友好 | 可能数据分布不均 |
| **哈希分片** | 对分片键做哈希，均匀分布到所有分片 | 数据均匀分布 | 范围查询需广播到所有分片 |

**最佳实践分片键设计：**
```javascript
// 使用复合分片键，平衡写入和查询
sh.shardCollection("shop.orders", { "user_id": "hashed", "order_time": 1 });

// 分片键必须出现在所有查询中，否则会广播到全部分片
// 查询时要带上 user_id
db.orders.find({ user_id: 12345, order_time: { $gte: ... } });
```

**分片选择原则：**
```
选择分片键的三个标准：
1. 高基数：分片键取值范围要大（不要用布尔值）
2. 分布均匀：避免热点（不要用时间戳做单字段分片键）
3. 查询友好：80% 的查询都能包含分片键

推荐：{ "user_id": "hashed" } ——最好的通用选择
```

> 💡 **面试金句**："分片是 MongoDB 扩展能力的精髓，但分片键的选择决定了成败。哈希分片保证写入均匀，复合分片键兼顾查询效率。千万不要用单调递增的字段（如时间戳）做范围分片——会产生严重的热点问题，所有写入都打到一个分片。"

---

## 4. 场景题与故障排查

> 💡 考察实际解决问题的能力

### Q9：MongoDB 查询变慢怎么排查？索引设计和查询优化有哪些要点？
**面试官意图：** 考察 MongoDB 性能优化的实际能力。

**完美解答：**

**排查流程：**

**第一步：找到慢查询**
```javascript
// 开启慢查询日志
db.setProfilingLevel(1, { slowms: 200 }); // 记录超过 200ms 的查询

// 查看慢日志
db.system.profile.find().sort({ ts: -1 }).limit(10).pretty();
```

**第二步：分析执行计划**
```javascript
// 查看查询是否走了索引
db.orders.find({ user_id: 12345, status: "paid" })
  .sort({ create_time: -1 })
  .explain("executionStats");

// 关注字段：
// winningPlan.stage: COLLSCAN（全表扫描！） / IXSCAN（索引扫描）✅
// executionStats.totalDocsExamined: 扫描的文档数
// executionStats.totalKeysExamined: 扫描的索引键数
// executionStats.executionTimeMillis: 执行时间
```

**第三步：常见问题和解决方案**

| 问题 | 现象 | 解决 |
|------|------|------|
| **全表扫描** | stage = COLLSCAN | 建索引 `createIndex({ user_id: 1 })` |
| **排序未走索引** | stage = SORT（内存排序） | 建复合索引覆盖排序字段 `createIndex({ user_id: 1, create_time: -1 })` |
| **索引选择性差** | 扫描了大量索引键但只返回少量文档 | 建复合索引增加筛选性 |
| **内存排序超限** | 32MB 内存排序限制，大数据集排序慢 | 建排序字段索引，让 MongoDB 走索引排序 |
| **返回字段太多** | 回传大量不需要的字段 | 用投影 `projection` 只返回需要的字段 |

**复合索引最佳实践——ESR 原则：**
```javascript
// ESR 原则：Equality - Sort - Range
// 1. 等值查询字段放最前面
// 2. 排序字段放中间
// 3. 范围查询字段放最后

// 优化前（需要两次查找：先查索引，再内存排序）
db.orders.find({ status: "paid", amount: { $gte: 100 } })
  .sort({ create_time: -1 });

// 优化后——符合 ESR 原则的索引
db.orders.createIndex({ status: 1, create_time: -1, amount: 1 });
// status（等值）→ create_time（排序）→ amount（范围）
```

> 🎯 **核心结论**：90% 的慢查询问题都是索引问题。先用 `explain("executionStats")` 定位，再看是否符合 ESR 原则。

---

### Q10：MongoDB 中如何实现事务？多文档事务有哪些限制？
**面试官意图：** 考察对 MongoDB 事务特性的理解，以及在需要事务的场景下如何应对。

**完美解答：**

**MongoDB 4.0+ 开始支持多文档事务（副本集），4.2+ 支持分片集群事务。**

**事务示例：**
```javascript
// Node.js 示例
const session = client.startSession();
session.startTransaction();

try {
  const users = client.db("shop").collection("users");
  const orders = client.db("shop").collection("orders");

  // 扣减用户余额
  users.updateOne(
    { _id: userId, balance: { $gte: 100 } },
    { $inc: { balance: -100 } },
    { session }
  );

  // 创建订单
  orders.insertOne(
    { user_id: userId, amount: 100, status: "paid" },
    { session }
  );

  // 所有操作成功 → 提交
  await session.commitTransaction();
} catch (error) {
  // 任一操作失败 → 回滚
  await session.abortTransaction();
} finally {
  session.endSession();
}
```

**Java 实现：**
```java
@Service
public class OrderService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Transactional
    public void createOrder(Order order) {
        // 扣减用户余额
        Query query = new Query(Criteria.where("_id").is(order.getUserId())
            .and("balance").gte(order.getAmount()));
        Update update = new Update().inc("balance", -order.getAmount());
        UpdateResult result = mongoTemplate.updateFirst(query, update, "users");

        if (result.getMatchedCount() == 0) {
            throw new RuntimeException("余额不足");
        }

        // 创建订单
        mongoTemplate.insert(order, "orders");
        // 任一异常 → 全部回滚
    }
}
```

**多文档事务的限制：**

| 限制 | 说明 |
|------|------|
| **事务超时** | 默认 60 秒，超过自动中止 |
| **文档大小** | 事务中更新的文档不能超过 16MB |
| **操作限制** | 事务中不能创建/删除集合或索引 |
| **批量操作** | 批量操作在事务中的行为不同（`insertMany` 等） |
| **性能影响** | 事务比非事务操作慢 10-20% |

> 💡 **实用建议**：尽量用文档级原子操作替代事务。MongoDB 和 MySQL 不同，事务不是它的强项。90% 的场景可以通过 `$inc`、`$push`、`$set` 等原子操作和文档嵌套来解决，只有真正跨文档、跨集合的强一致性需求才用事务。

---

### Q11：MongoDB 内存占用高怎么办？如何优化内存使用？
**面试官意图：** 考察 MongoDB 运维经验，内存管理是生产环境中常见的问题。

**完美解答：**

**MongoDB 的内存管理机制：**

MongoDB 会尽可能使用系统可用内存作为缓存（WiredTiger Cache），默认占用系统内存的 50%（`cache_size_gb` 可配置）。这是设计如此，不是内存泄漏。

**排查和优化方案：**

**1. 查看内存使用情况：**
```javascript
// 查看 WiredTiger 缓存统计
db.serverStatus().wiredTiger.cache;

// 关注指标：
// "bytes currently in the cache" - 当前缓存大小
// "percentage overhead" - 缓存开销
// "eviction triggered" - 淘汰触发次数（高说明缓存不够）
```

**2. 常见问题及优化：**

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| **缓存命中率低** | 数据量远大于缓存 | `db.serverStatus().wiredTiger.cache["cache hit ratio"]`，如果低于 80% 考虑扩容 |
| **频繁淘汰** | 缓存太小 | 调整 `storage.wiredTiger.engineConfig.cacheSizeGB`（不要超过 80% 总内存） |
| **内存碎片** | 频繁的文档移动 | 运行 `compact` 压缩集合 |
| **集合太多** | 每个集合都有元数据占用内存 | 控制集合数量，避免动态创建集合 |

**3. 内存控制配置（生产推荐）：**
```yaml
# mongod.conf
storage:
  wiredTiger:
    engineConfig:
      cacheSizeGB: 8  # 限制 WiredTiger 缓存为 8GB（总内存 16GB 时）
      journalCompressor: snappy
```

> ⚠️ **注意**：MongoDB 默认会使用系统 50% 的内存作为缓存，但如果你在同一台机器上跑其他服务（如应用服务），一定要显式配置 `cacheSizeGB`，否则可能导致其他服务 OOM。

---

## 💎 面试加分金句

- "MongoDB 的文档模型设计需要在数据冗余和查询性能之间做权衡——嵌套减少查询次数但引入数据冗余，引用保持数据一致性但需要额外查询。"
- "MongoDB 4.0 之后支持了多文档事务，但我不建议把 MongoDB 当 MySQL 用。事务性能开销大，能用原子操作解决的问题尽量不用事务。"
- "聚合管道是 MongoDB 数据分析的核心能力，`$match` 和 `$limit` 尽量前移，这是优化聚合性能最重要的原则。"
- "MongoDB 擅长写入和读取灵活的半结构化数据，是 MySQL 的有力补充而不是替代品。微服务架构中，混用 MySQL + MongoDB + Redis 是常见组合。"
- "分片键的选择决定了集群的命运——一个好的分片键能让数据均匀分布，一个坏的分片键会导致热点和性能灾难。"

## 📋 高频追问清单

| 追问方向 | 应对策略 |
|----------|----------|
| MongoDB 的 _id 是怎么生成的？ | ObjectId：4 字节时间戳 + 5 字节机器标识 + 2 字节 PID + 3 字节计数器 |
| MongoDB 索引的底层数据结构？ | B 树（B-Tree），不是 B+ 树 |
| MongoDB 和 Redis 的区别？ | MongoDB 是持久化文档数据库，Redis 是内存缓存数据库 |
| 什么是 WiredTiger？ | MongoDB 默认的存储引擎，支持压缩、文档级锁 |
| 为什么要避免使用大型数组？ | 数组是文档的一部分，频繁修改数组会导致文档移动，增加碎片 |
| MongoDB 的 limit 和 skip 性能怎么样？ | limit 好、skip 差（skip 会丢弃前面的文档，大数据量 skip 不如用范围查询） |
| MongoDB 中 mapReduce 还能用吗？ | 可以被聚合管道替代，聚合管道性能更好 |
| 什么是 MongoDB 的 oplog？ | 副本集的变更日志，类似 MySQL 的 binlog |

## 🔗 关联知识点

- [MySQL必做项目清单-面试问答](#) — MySQL vs MongoDB 选型对比
- [Redis必做项目清单-面试问答](#) — Redis + MongoDB 多级缓存架构
- [Docker必做项目清单-面试问答](#) — MongoDB 副本集容器化部署
