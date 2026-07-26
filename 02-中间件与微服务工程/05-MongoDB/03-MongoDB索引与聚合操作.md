# MongoDB 索引与聚合操作
> 索引让查询快如闪电，聚合管道让数据分析得心应手——两个进阶能力的完整指南。

## 目录
1. [索引核心概念](#1-索引核心概念)
2. [索引类型详解](#2-索引类型详解)
3. [索引使用与分析](#3-索引使用与分析)
4. [复合索引设计原则](#4-复合索引设计原则)
5. [聚合管道概念](#5-聚合管道概念)
6. [核心管道阶段详解](#6-核心管道阶段详解)
7. [聚合函数](#7-聚合函数)
8. [聚合优化与最佳实践](#8-聚合优化与最佳实践)
9. [面试核心](#9-面试核心)

---

## 1. 索引核心概念

索引是为查询搭建的高速通道。没有索引时 MongoDB 必须执行全表扫描（COLLSCAN）——依次检查集合中的每一条文档。有了合适的 B-Tree 索引，百万文档也只需约 20 次比较就能定位目标。

### 1.1 索引基础

| 概念 | 说明 |
|------|------|
| 默认索引 | `_id` 字段自动创建唯一索引 |
| 索引结构 | B-Tree（平衡树），支持高效的范围查询 |
| 索引存储 | 独立于数据文件，占用额外磁盘空间 |
| 索引代价 | 写入时需同步维护索引，影响写入性能 |
| 索引选择 | MongoDB 查询优化器自动选择最优索引 |

### 1.2 查看与管理索引

```javascript
// 查看集合上的所有索引
db.users.getIndexes()
// [
//   { "v": 2, "key": { "_id": 1 }, "name": "_id_" },
//   { "v": 2, "key": { "name": 1 }, "name": "name_1" }
// ]

// 查看索引大小（字节）
db.users.totalIndexSize()

// 查看每个索引的详细信息
db.users.aggregate([{ $indexStats: {} }])

// 创建索引
db.users.createIndex({ name: 1 })

// 删除指定索引
db.users.dropIndex("name_1")

// 删除所有索引（除 _id_ 外）
db.users.dropIndexes()

// 后台创建索引（不阻塞读写）
db.users.createIndex({ name: 1 }, { background: true })

// 创建时指定名称
db.users.createIndex({ name: 1, age: -1 }, { name: "idx_name_age" })
```

### 1.3 索引的权衡

```text
索引（Index）
├─ 优点
│  ├─ 查询速度提升 10-1000 倍
│  ├─ 排序不再需要内存排序
│  └─ 覆盖查询无需读取文档
└─ 代价
     ├─ 额外磁盘空间（数据量的 20%-50%）
     ├─ 写入性能下降（每次写操作需维护索引）
     └─ 索引过多影响查询优化器效率
```

---

## 2. 索引类型详解

### 2.1 单字段索引

最简单的索引类型，对单个字段进行排序。

```javascript
// 升序索引
db.users.createIndex({ name: 1 })

// 降序索引
db.users.createIndex({ age: -1 })

// 对单字段而言，升序和降序效果相同（可双向遍历）
db.users.find({ name: "张三" })                          // 走索引
db.users.find({ name: { $gte: "A", $lte: "Z" } })       // 走索引（范围）
```

### 2.2 复合索引

同时对多个字段建立索引，顺序非常重要。

```javascript
// 联合索引：先 name 后 age（升序）
db.users.createIndex({ name: 1, age: -1 })

// 前缀原则适用：
db.users.find({ name: "张三" })                          // ✅ 走索引（前缀）
db.users.find({ name: "张三", age: 25 })                 // ✅ 走索引
db.users.find({ name: "张三" }).sort({ age: -1 })        // ✅ 走索引（排序）
db.users.find({ age: 25 })                               // ❌ 不走索引（非前缀）

// 复合索引支持的查询模式：
// { name: "张三" }                     — 前缀
// { name: "张三", age: 25 }            — 完整
// { name: "张三", age: { $gt: 18 } }   — 前缀 + 范围
// { name: /^张/, age: 25 }             — 前缀 + 范围（正则前缀）
```

### 2.3 多键索引（数组字段）

当索引字段是数组时，MongoDB 自动创建多键索引，为数组中的每个元素创建索引条目。

```javascript
// 数据示例
// { _id: 1, tags: ["Java", "Spring", "MongoDB"] }

// 对数组字段创建索引（与普通索引语法相同）
db.posts.createIndex({ tags: 1 })

// 高效查询数组包含
db.posts.find({ tags: "Java" })              // 走索引 ✅
db.posts.find({ tags: { $all: ["Java", "MongoDB"] } })  // 走索引 ✅

// 复合多键索引（数组字段只能有一个）
// ✅ db.posts.createIndex({ tags: 1, createdAt: -1 })
// ❌ db.posts.createIndex({ tags: 1, authors: 1 })  — 不能有两个数组字段
```

### 2.4 文本索引（全文搜索）

内置的全文搜索引擎，支持中文分词（需安装 ICU）。

```javascript
// 创建文本索引（一个集合只能有一个文本索引）
db.articles.createIndex({
  title: "text",
  content: "text",
  summary: "text"
})

// 或使用通配符（索引所有字符串字段）
db.articles.createIndex({ "$**": "text" })

// 基本搜索（关键词之间是 OR 关系）
db.articles.find({ $text: { $search: "mongodb 教程" } })
// 匹配包含 "mongodb" 或 "教程" 的文档

// 精确短语搜索（双引号）
db.articles.find({ $text: { $search: "\"mongodb 教程\"" } })
// 匹配包含完整短语 "mongodb 教程" 的文档

// 排除词（减号）
db.articles.find({ $text: { $search: "mongodb -mysql" } })
// 匹配包含 mongodb 但不包含 mysql 的文档

// 按相关性排序
db.articles.find(
  { $text: { $search: "mongodb" } },
  { score: { $meta: "textScore" } }
).sort({ score: { $meta: "textScore" } })

// 中文搜索注意事项：
// - 需要安装 ICU（International Components for Unicode）
// - MongoDB 默认的分词对中文支持有限
// - 生产环境中文全文搜索推荐 Elasticsearch
```

### 2.5 地理空间索引

支持球面（2dsphere）和平面（2d）地理空间查询。

```javascript
// 2dsphere 索引（球面几何，最常用）
db.places.createIndex({ location: "2dsphere" })

// 数据格式（GeoJSON）
db.places.insertOne({
  name: "天安门",
  location: {
    type: "Point",
    coordinates: [116.397, 39.908]  // [经度, 纬度]
  }
})

// 附近查询（$near）
db.places.find({
  location: {
    $near: {
      $geometry: { type: "Point", coordinates: [116.4, 39.9] },
      $maxDistance: 1000,     // 最大距离（米）
      $minDistance: 10        // 最小距离（米）
    }
  }
})

// 多边形内查询
db.places.find({
  location: {
    $geoWithin: {
      $geometry: {
        type: "Polygon",
        coordinates: [[
          [116.3, 39.8],
          [116.5, 39.8],
          [116.5, 40.0],
          [116.3, 40.0],
          [116.3, 39.8]
        ]]
      }
    }
  }
})
```

### 2.6 TTL 索引（自动过期）

文档在指定时间后自动删除，适用于会话、验证码、日志等场景。

```javascript
// 文档在 createTime 字段值后 3600 秒自动删除
db.logs.createIndex(
  { createTime: 1 },
  { expireAfterSeconds: 3600 }
)

// TTL 的工作原理：
// - 后台线程每 60 秒检查一次
// - 检查 createTime + expireAfterSeconds < 当前时间
// - 符合条件的文档被删除

// 应用场景：
// - 用户会话（60 分钟过期）
db.sessions.createIndex(
  { lastAccess: 1 },
  { expireAfterSeconds: 3600 }
)

// - 验证码（5 分钟过期）
db.verificationCodes.createIndex(
  { createdAt: 1 },
  { expireAfterSeconds: 300 }
)

// - 日志自动清理（7 天过期）
db.logs.createIndex(
  { createdAt: 1 },
  { expireAfterSeconds: 604800 }
)

// 注意事项：
// - TTL 索引只能用于日期类型字段
// - 删除操作在后台执行，不保证实时删除
// - 删除操作会产生写入负载
// - 不能保证在 expireAfterSeconds 后立即删除（可能有延迟）
```

### 2.7 唯一索引

防止重复数据，与关系型数据库的 UNIQUE 约束类似。

```javascript
// 单字段唯一索引
db.users.createIndex({ email: 1 }, { unique: true })

// 复合唯一索引（组合值唯一）
db.users.createIndex(
  { firstName: 1, lastName: 1 },
  { unique: true }
)

// 测试唯一性
db.users.insertOne({ email: "test@example.com" })  // ✅ 成功
db.users.insertOne({ email: "test@example.com" })  // ❌ E11000 duplicate key error

// 唯一索引与 null
db.users.createIndex({ phone: 1 }, { unique: true, sparse: true })
// sparse: true — 只索引包含该字段的文档，允许存在多个不含 phone 的文档
```

### 2.8 稀疏索引与部分索引

```javascript
// 稀疏索引：只索引包含该字段的文档
db.users.createIndex({ email: 1 }, { sparse: true })

// 部分索引：只索引满足条件的文档（3.2+）
// 只索引 age > 18 的文档
db.users.createIndex(
  { name: 1, age: 1 },
  { partialFilterExpression: { age: { $gt: 18 } } }
)

// 部分索引示例：只索引 VIP 用户
db.users.createIndex(
  { email: 1 },
  { partialFilterExpression: { vipLevel: { $gte: 1 } } }
)

// 对比：稀疏 vs 部分
// 稀疏索引：基于字段存在性
// 部分索引：基于任意过滤条件
```

### 2.9 哈希索引

支持哈希分片键，只支持等值查询，不支持范围查询。

```javascript
db.users.createIndex({ userId: "hashed" })
// 适合：分片键、等值查询
// 不适合：范围查询、排序
```

### 索引类型速查表

| 索引类型 | 创建方式 | 支持查询 | 场景 |
|----------|----------|----------|------|
| 单字段 | `{ field: 1 }` | 等值、范围、排序 | 单条件查询 |
| 复合 | `{ f1: 1, f2: -1 }` | 多条件、排序 | 组合查询 |
| 多键 | 数组字段自动 | 数组包含 | 标签、分类 |
| 文本 | `{ field: "text" }` | 全文搜索 | 文章搜索 |
| 地理空间 | `{ field: "2dsphere" }` | 附近、包含 | LBS 应用 |
| TTL | `{ field: 1 }, { expireAfterSeconds }` | 自动过期 | 会话、日志 |
| 唯一 | `{ field: 1 }, { unique: true }` | 唯一约束 | 邮箱、用户名 |
| 稀疏 | `{ field: 1 }, { sparse: true }` | 存在性查询 | 可选字段 |
| 部分 | `{ field: 1 }, { partialFilterExpression }` | 条件过滤 | 热数据索引 |
| 哈希 | `{ field: "hashed" }` | 等值 | 分片键 |

---

## 3. 索引使用与分析

### 3.1 explain() 查看查询计划

```javascript
// 基本 explain
db.users.find({ name: "张三" }).explain()

// 详细执行统计（最常用）
db.users.find({ name: "张三", age: { $gt: 18 } }).explain("executionStats")

// 查询计划分析器（对比多个索引）
db.users.find({ name: "张三", age: { $gt: 18 } }).explain("allPlansExecution")
```

### 3.2 explain 输出解读

```javascript
// 关键输出字段说明
{
  "queryPlanner": {
    "winningPlan": {                  // 最终选择的执行计划
      "stage": "FETCH",              // 当前阶段
      "inputStage": {
        "stage": "IXSCAN",           // 索引扫描（理想情况）
        "keyPattern": { "name": 1 },
        "indexName": "name_1",
        "direction": "forward"
      }
    },
    "rejectedPlans": [...]            // 被拒绝的候选计划
  },
  "executionStats": {
    "executionSuccess": true,
    "nReturned": 100,                 // 返回文档数
    "executionTimeMillis": 5,         // 执行时间（ms）
    "totalKeysExamined": 120,         // 扫描索引条目数
    "totalDocsExamined": 100,         // 扫描文档数
    "executionStages": {
      "stage": "IXSCAN",
      "nReturned": 100,
      "totalKeysExamined": 120,
      "totalDocsExamined": 100
    }
  }
}
```

### 3.3 执行阶段说明

| Stage | 含义 | 说明 |
|-------|------|------|
| `COLLSCAN` | 全表扫描 | 性能差，需要加索引 |
| `IXSCAN` | 索引扫描 | 良好的索引使用 |
| `FETCH` | 根据索引取文档 | 根据索引指针读取文档数据 |
| `SHARD_MERGE` | 合并分片结果 | 分片集群查询 |
| `SORT` | 内存排序 | 无索引排序，大数据量可能超限 |
| `SORT_MERGE` | 合并排序 | 多个分片结果排序合并 |
| `IDHACK` | `_id` 查询 | 直接使用 `_id` 索引 |
| `TEXT` | 文本索引查询 | 全文搜索 |
| `GEO_NEAR` | 地理空间查询 | 附近查询 |

### 3.4 Covered Query（覆盖查询）

当查询条件和返回字段都在同一个索引中时，MongoDB 可以直接从索引返回结果，**无需读取文档**，这是性能最优的情况。

```javascript
// 索引：{ name: 1, age: 1, email: 1 }

// 覆盖查询 ✅
db.users.find(
  { name: "张三" },
  { _id: 0, name: 1, age: 1, email: 1 }
)
// explain: stage: IXSCAN，没有 FETCH 阶段
// totalDocsExamined: 0（没有读取文档！）

// 非覆盖查询 ❌
db.users.find(
  { name: "张三" },
  { _id: 0, name: 1, age: 1, address: 1 }
)
// address 不在索引中，需要 FETCH 阶段读取文档

// 覆盖查询的三个条件：
// 1. 查询字段都在索引中
// 2. 返回字段都在索引中
// 3. _id 必须显式排除（_id: 0）
```

### 3.5 索引命中分析

```javascript
// 检查索引是否被使用
const exp = db.users.find({ name: "张三", age: { $gt: 18 } }).explain("executionStats")
const stage = exp.executionStats.executionStages.stage
const docsExamined = exp.executionStats.totalDocsExamined
const docsReturned = exp.executionStats.nReturned

// 理想：IXSCAN，docsExamined ≈ docsReturned
// 不理想：COLLSCAN，docsExamined >> docsReturned
```

### 3.6 常见索引失效场景

| 场景 | 原因 | 解决方案 |
|------|------|----------|
| 正则前导通配 | `{ name: /张/ }` | 改为 `{ name: { $regex: /^张/ } }` |
| 否定查询 | `{ field: { $ne: 1 } }` | 效果差，考虑其他方案 |
| 运算符非前缀 | 复合索引未用前缀字段 | 调整查询或索引顺序 |
| `$where` 查询 | JS 表达式 | 改用聚合管道 |
| 集合数据量小 | 全表扫描比索引快 | 无需处理 |

---

## 4. 复合索引设计原则

### 4.1 ESR 原则（等值-排序-范围）

复合索引的字段顺序由查询模式决定，遵循 **ESR** 原则：

```
E — Equality（等值）：等值匹配的字段放最前面
S — Sort（排序）：排序字段放第二
R — Range（范围）：范围查询放最后
```

```javascript
// 查询模式：city="北京" AND age > 18 ORDER BY name
// 索引设计：
//   E（等值）: city
//   S（排序）: name
//   R（范围）: age
db.users.createIndex({ city: 1, name: 1, age: 1 })

// 索引工作原理：
// 1. 精确匹配 city="北京" → 快速定位到北京的数据块
// 2. 索引已按 name 排序 → 直接遍历，不需要额外排序
// 3. 在已排序的结果中过滤 age > 18 → 范围扫描
```

**错误示例：**

```javascript
// 查询：city="北京" AND age > 18 ORDER BY name

// ❌ 错误：{ age: 1, city: 1, name: 1 }
// 范围放在第一位，无法利用索引排序

// ❌ 错误：{ city: 1, age: 1, name: 1 }
// 范围字段在排序字段前，无法覆盖排序

// ✅ 正确：{ city: 1, name: 1, age: 1 }
// 等值-排序-范围
```

### 4.2 索引选择性与区分度

```javascript
// 区分度高的字段放前面

// 好：唯一性高
db.users.createIndex({ email: 1, name: 1, createdAt: 1 })
// email 几乎唯一，能快速缩小范围

// 差：唯一性低
db.users.createIndex({ gender: 1, city: 1, age: 1 })
// gender 只有两个值，区分度差，不如把 city 放前面
```

### 4.3 索引数量平衡

| 因素 | 建议 |
|------|------|
| 单个集合索引数 | 不超过 5-10 个 |
| 复合索引字段数 | 不超过 5 个字段 |
| 读多写少场景 | 可多建索引提升查询 |
| 写多读少场景 | 尽量少建索引，保证写入性能 |
| 监控指标 | `totalIndexSize()` 和写入延迟 |

### 4.4 索引设计流程

```text
1. 收集慢查询
   └→ db.system.profile.find().sort({ millis: -1 }).limit(10)

2. 分析查询模式
   └→ 提取等值条件、排序字段、范围条件

3. 按 ESR 设计索引
   └→ E（等值）→ S（排序）→ R（范围）

4. 测试索引效果
   └→ explain("executionStats") 查看 IXSCAN

5. 删除无用索引
   └→ db.collection.dropIndex("old_index")
```

---

## 5. 聚合管道概念

### 5.1 什么是聚合管道

聚合管道（Aggregation Pipeline）将数据处理分成多个阶段（Stage），数据依次流过每个阶段，每个阶段对数据进行变换和加工：

```text
Collection
   └→ [$match] 过滤
       └→ [$group] 分组
           └→ [$sort] 排序
               └→ [$project] 投影
                   └→ 结果
```

### 5.2 与 SQL 对照

| SQL | 聚合管道 | 说明 |
|-----|----------|------|
| WHERE | `$match` | 过滤文档 |
| GROUP BY | `$group` | 分组聚合 |
| ORDER BY | `$sort` | 排序 |
| SELECT | `$project` | 投影、计算字段 |
| LIMIT | `$limit` | 限制数量 |
| LEFT JOIN | `$lookup` | 跨集合关联 |
| UNWIND | `$unwind` | 数组拆解为多行 |
| HAVING | `$match`（后置） | 分组后过滤 |
| DISTINCT | `$group` + `$addToSet` | 去重 |
| UNION ALL | `$unionWith` | 合并集合 |

### 5.3 聚合管道 vs find

| 维度 | find | 聚合管道 |
|------|------|----------|
| 功能 | 查询 + 简单过滤 | 复杂数据加工、统计分析 |
| 多阶段 | 不支持 | 支持，任意组合 |
| 计算字段 | 有限 | 丰富（`$multiply`, `$concat` 等） |
| 跨集合 | 不支持 | `$lookup` 支持 |
| 数组展开 | 不支持 | `$unwind` 支持 |
| 内存限制 | 无特殊 | 默认 100MB，大容量需 `allowDiskUse` |

---

## 6. 核心管道阶段详解

### 6.1 $match — 过滤

**作用**：过滤文档，类似 SQL 的 WHERE。应尽量放在管道开头，减少后续阶段处理的数据量。

```javascript
// 基本过滤
db.orders.aggregate([
  { $match: { status: "completed", amount: { $gte: 100 } } }
])

// 复杂条件
db.orders.aggregate([
  { $match: {
    $or: [
      { status: "completed", amount: { $gte: 1000 } },
      { status: "pending", createdAt: { $gte: ISODate("2024-01-01") } }
    ]
  }}
])

// 性能最佳实践：
// - $match 放管道最前面
// - $match 中的字段尽量走索引
// - $match 尽早过滤掉不需要的数据
```

### 6.2 $group — 分组统计

**作用**：按指定字段分组，对每组数据进行聚合计算，类似 SQL 的 GROUP BY。

```javascript
// 基本分组统计
db.orders.aggregate([
  { $group: {
    _id: "$category",               // 分组字段（$ 引用字段）
    totalAmount: { $sum: "$amount" },
    avgAmount:   { $avg: "$amount" },
    count:       { $sum: 1 },
    maxAmount:   { $max: "$amount" },
    minAmount:   { $min: "$amount" }
  }}
])
// 输出：
// { "_id": "电子产品", "totalAmount": 50000, "count": 100, ... }
// { "_id": "服装", "totalAmount": 30000, "count": 200, ... }

// 多字段分组
db.orders.aggregate([
  { $group: {
    _id: {
      category: "$category",
      year: { $year: "$createdAt" },
      month: { $month: "$createdAt" }
    },
    totalAmount: { $sum: "$amount" },
    count: { $sum: 1 }
  }}
])

// 不分组（统计整个集合）
db.orders.aggregate([
  { $group: {
    _id: null,                       // null 表示不分组
    totalAmount: { $sum: "$amount" },
    avgAmount: { $avg: "$amount" },
    totalCount: { $sum: 1 }
  }}
])
```

### 6.3 $sort — 排序

**作用**：对文档进行排序，类似 SQL 的 ORDER BY。

```javascript
// 单字段排序
db.orders.aggregate([
  { $group: { _id: "$category", total: { $sum: "$amount" } } },
  { $sort: { total: -1 } }           // 按总额降序
])

// 多字段排序
db.orders.aggregate([
  { $sort: { category: 1, amount: -1 } }
])

// 排序优化技巧
// - 如果 $sort 跟在 $match 后面，可以使用索引
// - 大数据量排序可能消耗大量内存
// - 尽量在管道早期排序，减少后续数据处理
```

### 6.4 $project — 投影与计算

**作用**：选择/重命名/计算字段，类似 SQL 的 SELECT。

```javascript
// 基本投影（1=显示，0=隐藏）
db.orders.aggregate([
  { $project: {
    _id: 0,
    orderId: 1,
    customerName: 1,
    totalAmount: 1
  }}
])

// 计算新字段
db.orders.aggregate([
  { $project: {
    orderId: 1,
    total: { $multiply: ["$price", "$quantity"] },
    discountedPrice: { $round: [{ $multiply: ["$price", 0.9] }, 2] },
    tax: { $multiply: [{ $divide: ["$price", 100] }, 13] },
    fullName: { $concat: ["$firstName", " ", "$lastName"] },
    orderYear: { $year: "$createdAt" }
  }}
])

// 条件表达式
db.orders.aggregate([
  { $project: {
    orderId: 1,
    amount: 1,
    level: {
      $switch: {
        branches: [
          { case: { $gte: ["$amount", 1000] }, then: "VIP" },
          { case: { $gte: ["$amount", 500] }, then: "白银" }
        ],
        default: "普通"
      }
    },
    isExpensive: { $cond: [{ $gte: ["$amount", 1000] }, true, false] }
  }}
])
```

### 6.5 $lookup — 跨集合关联

**作用**：左外连接两个集合，类似 SQL 的 LEFT JOIN。

```javascript
// 基本关联
db.orders.aggregate([
  { $lookup: {
    from: "products",                 // 关联的目标集合
    localField: "productId",          // 本集合关联字段
    foreignField: "_id",              // 目标集合关联字段
    as: "productInfo"                 // 结果字段名（数组）
  }}
])
// 结果：每个订单增加 productInfo 字段，包含匹配的产品信息

// 关联后展开（常用模式）
db.orders.aggregate([
  { $lookup: {
    from: "products",
    localField: "productId",
    foreignField: "_id",
    as: "product"
  }},
  { $unwind: "$product" },           // 数组转对象
  { $project: {
    _id: 0,
    orderId: 1,
    productName: "$product.name",
    price: "$product.price"
  }}
])

// 带管道的 $lookup（5.0+）
db.orders.aggregate([
  { $lookup: {
    from: "products",
    let: { pid: "$productId" },
    pipeline: [
      { $match: { $expr: { $eq: ["$_id", "$$pid"] } } },
      { $project: { name: 1, price: 1, _id: 0 } }
    ],
    as: "product"
  }}
])

// 性能注意事项：
// - $lookup 性能较差，大集合谨慎使用
// - 被关联集合的 foreignField 必须有索引
// - 推荐使用反范式设计替代关联
```

### 6.6 $unwind — 数组拆解

**作用**：将数组字段拆解为多条文档，每条文档包含数组中的一个元素。

```javascript
// 数据示例
// { _id: 1, orderId: "O001", items: [
//   { product: "A", quantity: 2 },
//   { product: "B", quantity: 1 }
// ]}

// 基本拆解
db.orders.aggregate([
  { $unwind: "$items" }
])
// 输出：
// { _id: 1, orderId: "O001", items: { product: "A", quantity: 2 } }
// { _id: 1, orderId: "O001", items: { product: "B", quantity: 1 } }

// 保留空数组（默认会删除空数组文档）
db.orders.aggregate([
  { $unwind: { path: "$items", preserveNullAndEmptyArrays: true } }
])

// 拆解后统计
db.orders.aggregate([
  { $unwind: "$items" },
  { $group: {
    _id: "$items.product",
    totalSold: { $sum: "$items.quantity" }
  }},
  { $sort: { totalSold: -1 } }
])
```

### 6.7 $bucket — 分桶

**作用**：将文档分到不同桶中（类似于直方图）。

```javascript
// 按金额分桶
db.orders.aggregate([
  { $bucket: {
    groupBy: "$amount",
    boundaries: [0, 100, 500, 1000, 5000],   // 分桶边界 [0,100), [100,500), ...
    default: "5000+",                         // 超出边界的分到 default
    output: {
      count: { $sum: 1 },
      totalAmount: { $sum: "$amount" },
      avgAmount: { $avg: "$amount" }
    }
  }}
])
// 输出：
// { "_id": 0,     "count": 50, "totalAmount": 2500,  "avgAmount": 50 }
// { "_id": 100,   "count": 80, "totalAmount": 24000, "avgAmount": 300 }
// { "_id": 500,   "count": 30, "totalAmount": 22500, "avgAmount": 750 }
// { "_id": 1000,  "count": 10, "totalAmount": 15000, "avgAmount": 1500 }
// { "_id": "5000+", "count": 2, "totalAmount": 12000, "avgAmount": 6000 }

// 按日期分桶
db.orders.aggregate([
  { $bucket: {
    groupBy: { $year: "$createdAt" },
    boundaries: [2020, 2021, 2022, 2023, 2024],
    output: { count: { $sum: 1 }, total: { $sum: "$amount" } }
  }}
])
```

### 6.8 $facet — 多维度聚合

**作用**：在同一管道中同时进行多组不同的聚合处理。

```javascript
db.orders.aggregate([
  { $facet: {
    // 按类别统计
    byCategory: [
      { $group: { _id: "$category", count: { $sum: 1 }, total: { $sum: "$amount" } } }
    ],
    // 按状态统计
    byStatus: [
      { $group: { _id: "$status", count: { $sum: 1 } } }
    ],
    // 总额
    totals: [
      { $group: { _id: null, totalAmount: { $sum: "$amount" }, avgAmount: { $avg: "$amount" } } }
    ]
  }}
])
// 输出一个文档，包含三个数组字段
```

### 6.9 $addFields — 添加字段

**作用**：向文档中添加新字段，不影响原有字段。

```javascript
db.orders.aggregate([
  { $addFields: {
    totalAmount: { $multiply: ["$price", "$quantity"] },
    tax: { $multiply: [{ $divide: ["$price", 100] }, 13] },
    statusDesc: {
      $switch: {
        branches: [
          { case: { $eq: ["$status", "pending"] }, then: "待处理" },
          { case: { $eq: ["$status", "completed"] }, then: "已完成" }
        ],
        default: "未知"
      }
    }
  }}
])
```

---

## 7. 聚合函数

### 7.1 累加器（Accumulator）

| 函数 | 说明 | 类似 SQL |
|------|------|----------|
| `$sum` | 求和 / 计数 | `SUM()` / `COUNT()` |
| `$avg` | 平均值 | `AVG()` |
| `$min` | 最小值 | `MIN()` |
| `$max` | 最大值 | `MAX()` |
| `$first` | 分组内第一个值 | — |
| `$last` | 分组内最后一个值 | — |
| `$push` | 收集到数组 | — |
| `$addToSet` | 收集到数组（去重） | — |
| `$stdDevPop` | 总体标准差 | `STDDEV_POP()` |
| `$stdDevSamp` | 样本标准差 | `STDDEV_SAMP()` |

### 7.2 算术表达式

| 函数 | 说明 | 示例 |
|------|------|------|
| `$add` | 加法 | `{ $add: ["$price", 10] }` |
| `$subtract` | 减法 | `{ $subtract: ["$total", "$discount"] }` |
| `$multiply` | 乘法 | `{ $multiply: ["$price", "$quantity"] }` |
| `$divide` | 除法 | `{ $divide: ["$total", 100] }` |
| `$mod` | 取模 | `{ $mod: ["$amount", 100] }` |
| `$round` | 四舍五入 | `{ $round: ["$amount", 2] }` |
| `$abs` | 绝对值 | `{ $abs: "$difference" }` |

### 7.3 字符串表达式

| 函数 | 说明 | 示例 |
|------|------|------|
| `$concat` | 拼接 | `{ $concat: ["$first", " ", "$last"] }` |
| `$toLower` | 转小写 | `{ $toLower: "$name" }` |
| `$toUpper` | 转大写 | `{ $toUpper: "$name" }` |
| `$substr` | 截取 | `{ $substr: ["$phone", 0, 3] }` |
| `$trim` | 去空格 | `{ $trim: { input: "$name" } }` |

### 7.4 日期表达式

| 函数 | 说明 | 返回 |
|------|------|------|
| `$year` | 提取年份 | 2024 |
| `$month` | 提取月份 | 1-12 |
| `$dayOfMonth` | 提取日 | 1-31 |
| `$dayOfWeek` | 星期几 | 1（周日）-7 |
| `$hour` | 小时 | 0-23 |
| `$dateToString` | 格式化 | "2024-07-26" |

### 7.5 条件表达式

```javascript
// $cond — if-then-else
{ $cond: { if: { $gte: ["$amount", 1000] }, then: "VIP", else: "普通" } }
{ $cond: [{ $gte: ["$amount", 1000] }, "VIP", "普通"] }

// $switch — 多条件
{ $switch: {
  branches: [
    { case: { $gte: ["$amount", 1000] }, then: "钻石" },
    { case: { $gte: ["$amount", 500] }, then: "黄金" },
    { case: { $gte: ["$amount", 100] }, then: "白银" }
  ],
  default: "普通"
}}

// $ifNull — 默认值
{ $ifNull: ["$nickname", "$username", "匿名用户"] }
// 取第一个非 null 值
```

---

## 8. 聚合优化与最佳实践

### 8.1 性能优化原则

| 优化手段 | 说明 | 收益 |
|----------|------|------|
| `$match` 放最前面 | 尽早过滤数据，减少后续阶段处理量 | 高 |
| `$project` 尽早使用 | 减少管道中传递的字段 | 中 |
| 先 `$match` 后 `$sort` | 可以利用索引排序 | 高 |
| 限制文档数 | `$limit` 尽早使用 | 中 |
| `$lookup` 关联字段建索引 | 大幅提升关联性能 | 高 |
| 避免大数组 `$unwind` | 数据量可能爆炸 | 中 |
| 使用 `allowDiskUse` | 大数据量聚合超过 100MB 内存限制时 | 必要 |

### 8.2 内存限制

```javascript
// 默认每个聚合阶段最多使用 100MB 内存
// 超过限制会报错

// 启用磁盘使用（性能下降但不会报错）
db.orders.aggregate([
  { $group: { _id: "$category", total: { $sum: "$amount" } } }
], { allowDiskUse: true })

// 设置缓存排序
db.orders.aggregate([
  { $sort: { amount: -1 } }
], { allowDiskUse: true })
```

### 8.3 完整聚合示例

```javascript
// 场景：电商订单分析
// 需求：统计 2024 年每个品类的月销售情况，包括销售额、订单数、平均客单价

db.orders.aggregate([
  // 第 1 步：过滤数据，缩小范围
  { $match: {
    status: "completed",
    createdAt: {
      $gte: ISODate("2024-01-01"),
      $lt: ISODate("2025-01-01")
    }
  }},

  // 第 2 步：拆解订单商品数组
  { $unwind: "$items" },

  // 第 3 步：添加计算字段
  { $addFields: {
    itemTotal: { $multiply: ["$items.price", "$items.quantity"] },
    month: { $month: "$createdAt" }
  }},

  // 第 4 步：按品类和月份分组
  { $group: {
    _id: {
      category: "$items.category",
      month: "$month"
    },
    totalSales: { $sum: "$itemTotal" },
    orderCount: { $sum: 1 },
    avgOrderValue: { $avg: "$itemTotal" },
    maxOrderValue: { $max: "$itemTotal" }
  }},

  // 第 5 步：按销售额降序排序
  { $sort: { totalSales: -1 } },

  // 第 6 步：格式化输出
  { $project: {
    _id: 0,
    category: "$_id.category",
    month: "$_id.month",
    totalSales: { $round: ["$totalSales", 2] },
    orderCount: 1,
    avgOrderValue: { $round: ["$avgOrderValue", 2] },
    maxOrderValue: { $round: ["$maxOrderValue", 2] }
  }}
])
```

### 8.4 聚合管道与索引

```javascript
// $match 和 $sort 可以利用索引

// 有索引 { status: 1, createdAt: -1 }
db.orders.aggregate([
  { $match: { status: "completed" } },    // 走索引 ✅
  { $sort: { createdAt: -1 } },            // 走索引 ✅
  { $group: { _id: "$category", total: { $sum: "$amount" } } }
])

// 无索引的情况
db.orders.aggregate([
  { $match: { status: "completed" } },    // COLLSCAN ❌
  { $group: { ... } }
])
```

### 8.5 常见聚合模式

**模式 1：分页 + 统计**

```javascript
db.orders.aggregate([
  { $match: { status: "completed" } },
  { $facet: {
    metadata: [{ $count: "total" }],
    data: [
      { $sort: { createdAt: -1 } },
      { $skip: 0 },
      { $limit: 20 }
    ]
  }}
])
```

**模式 2：实时排名**

```javascript
// 商品销售 Top 10
db.orders.aggregate([
  { $unwind: "$items" },
  { $group: {
    _id: "$items.productId",
    totalSold: { $sum: "$items.quantity" }
  }},
  { $sort: { totalSold: -1 } },
  { $limit: 10 },
  { $lookup: {
    from: "products",
    localField: "_id",
    foreignField: "_id",
    as: "product"
  }},
  { $unwind: "$product" },
  { $project: {
    _id: 0,
    productName: "$product.name",
    totalSold: 1
  }}
])
```

**模式 3：滚动聚合**

```javascript
// 最近 30 天每日新增用户
db.users.aggregate([
  { $match: {
    createdAt: { $gte: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000) }
  }},
  { $group: {
    _id: { $dateToString: { format: "%Y-%m-%d", date: "$createdAt" } },
    count: { $sum: 1 }
  }},
  { $sort: { _id: 1 } }
])
```

---

## 9. 面试核心

| 问题 | 答案 |
|------|------|
| 复合索引设计原则？ | ESR：等值字段放最前，排序字段第二，范围字段最后 |
| explain 中 IXSCAN 和 COLLSCAN 区别？ | IXSCAN=走索引，COLLSCAN=全表扫描 |
| 聚合管道的 `$match` 为什么要放开头？ | 减少后续阶段处理的数据量，提升性能还能利用索引 |
| `$lookup` 类似 SQL 的什么？ | LEFT JOIN，但关联性能差，推荐反范式设计 |
| 覆盖查询是什么？ | 查询条件和返回字段都在索引中，无需读取文档 |
| TTL 索引的超时精确吗？ | 不精确，后台每 60s 检查一次，有延迟 |
| 什么是 `$unwind`？ | 将数组拆解为多条文档 |
| 聚合管道的默认内存限制？ | 100MB，超过需 `allowDiskUse: true` |
| 什么是 `$bucket`？ | 分桶聚合，类似直方图 |
| `$facet` 的用途？ | 同一管道同时进行多组不同聚合 |
| 如何查看索引使用情况？ | `.explain("executionStats")` |
| 索引对写入性能的影响？ | 每次写入需维护索引，索引越多写入越慢 |
| 多键索引的限制？ | 复合索引中只能有一个数组字段 |
| 什么场景用 `sparse` 索引？ | 字段只在一部分文档中存在，节省空间 |

> **极简总结**：索引设计用 ESR，查询分析用 explain。聚合管道 `$match` 打头阵，`$lookup` 谨慎使用。
