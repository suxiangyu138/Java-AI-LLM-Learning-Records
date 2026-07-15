# MongoDB 详细知识点

> **定位**：开源的文档型 NoSQL 数据库，BSON 格式存储，无固定 schema，高灵活、高扩展。核心层级：数据库 → 集合 → 文档。

---

## 目录

1. [核心定义与价值](#1-核心定义与价值)
2. [核心架构](#2-核心架构)
3. [数据类型与 CRUD](#3-数据类型与-crud)
4. [索引技术](#4-索引技术)
5. [聚合操作](#5-聚合操作)
6. [高可用与高扩展](#6-高可用与高扩展)
7. [安全与备份恢复](#7-安全与备份恢复)
8. [应用场景](#8-应用场景)
9. [MySQL 对比](#9-mysql-对比)

---

## 1. 核心定义与价值

| 特性 | 说明 |
|------|------|
| 类型 | 文档型 NoSQL，BSON（Binary JSON） |
| 层级 | 数据库 → 集合 → 文档 |
| 核心特点 | 无固定 schema、字段可动态扩展 |
| 存储引擎 | WiredTiger（默认，支持压缩+事务） |

### 五大核心价值

| 价值 | 说明 |
|------|------|
| **非结构化数据** | 天然支持 JSON/嵌套/数组等灵活结构 |
| **敏捷开发** | 无需 DDL，字段随时添加修改 |
| **分布式扩展** | 原生分片集群，GB→TB 线性扩展 |
| **高性能** | 内存映射 + WAL 日志预写 |
| **强大查询** | 聚合管道 + 地理空间 + 全文检索 |

---

## 2. 核心架构

### 层级结构

```text
MongoDB 实例（端口 27017）
  └── 数据库（Database）
        └── 集合（Collection） ← 类似 MySQL 表，但无固定 schema
              └── 文档（Document） ← BSON 格式，_id 主键
```

### 集群组件

| 组件 | 组成 | 作用 |
|------|------|------|
| **副本集** | 1 Primary + N Secondary + 可选 Arbiter | 高可用：主节点读写，从节点备份，自动故障转移 |
| **分片集群** | Shard + Config Server + Mongos | 水平扩展：分片存数据，Mongos 路由请求 |
| **MongoDB Atlas** | 官方云托管 | 免部署维护 |

---

## 3. 数据类型与 CRUD

### 核心数据类型

| 类型 | 说明 | 示例 |
|------|------|------|
| String | UTF-8 文本 | `"name": "MongoDB"` |
| Number | Int32/Int64/Double | `"age": 20` |
| Boolean | true/false | `"isActive": true` |
| Date | 毫秒时间戳 | `new Date()` |
| ObjectId | 12 字节主键（时间戳+机器+进程+序列） | 自动生成 |
| Array | 多值集合 | `"tags": ["A", "B"]` |
| Embedded | 嵌套文档 | `"addr": {"city": "BJ"}` |
| Binary | 图片/视频二进制 | — |

### CRUD 速查

```javascript
// 数据库
use mydb                          // 切换/创建
show dbs                          // 查看所有
db.dropDatabase()                 // 删除

// 集合
db.createCollection("users")     // 创建
show collections                  // 查看
db.users.drop()                   // 删除

// 插入
db.users.insertOne({name: "张三", age: 20})
db.users.insertMany([{...}, {...}])

// 查询
db.users.find({age: {$gt: 18}}).sort({age: -1}).limit(10).skip(20)
db.users.findOne({name: "张三"})

// 更新
db.users.updateOne({name: "张三"}, {$set: {age: 21}})
db.users.updateMany({...}, {$set: {...}})
db.users.replaceOne({...}, {新文档})

// 删除
db.users.deleteOne({...})
db.users.deleteMany({...})
```

### 查询运算符

| 类别 | 运算符 |
|------|--------|
| 比较 | `$eq` `$ne` `$gt` `$lt` `$gte` `$lte` |
| 逻辑 | `$and` `$or` `$not` `$nor` |
| 数组 | `$in` `$nin` `$all` `$size` |
| 其他 | `$exists` `$regex` |

---

## 4. 索引技术

### 索引类型速查

| 类型 | 创建示例 | 适用 |
|------|----------|------|
| **单字段** | `createIndex({name: 1})` | 最常见的单字段查询 |
| **复合** | `createIndex({name: 1, age: -1})` | 多字段联合查询（前缀匹配） |
| **多键** | `createIndex({tags: 1})` | 数组字段自动创建 |
| **地理空间** | `createIndex({pos: "2dsphere"})` | LBS 附近搜索 |
| **文本** | `createIndex({title: "text"})` | 全文检索 |
| **哈希** | `createIndex({id: "hashed"})` | 分片键等值查询 |

### 索引操作

```javascript
db.users.createIndex({name: 1}, {unique: true, background: true})
db.users.getIndexes()                          // 查看
db.users.dropIndex("name_1")                   // 删除
db.users.explain("executionStats")             // 分析查询计划
```

### 优化原则

| 原则 | 说明 |
|------|------|
| 避免过度索引 | 索引占用空间 + 降低写入速度 |
| 前缀匹配 | `{a:1, b:1}` 查 `a` 可用索引，查 `b` 不可 |
| 避免失效 | 函数操作、类型转换、后缀模糊匹配导致失效 |
| 唯一索引 | 用户名/手机号等唯一约束字段 |

---

## 5. 聚合操作

### 聚合管道阶段

| 阶段 | 作用 | 类似 SQL |
|------|------|----------|
| `$match` | 筛选条件 | `WHERE` |
| `$group` | 分组统计 | `GROUP BY` |
| `$project` | 筛选/重命名字段 | `SELECT` |
| `$sort` | 排序 | `ORDER BY` |
| `$skip` / `$limit` | 分页 | `LIMIT OFFSET` |
| `$lookup` | 跨集合关联 | `LEFT JOIN` |
| `$unwind` | 数组拆分 | — |

### 聚合函数

| 函数 | 作用 |
|------|------|
| `$sum` | 求和/计数 |
| `$avg` / `$min` / `$max` | 平均/最小/最大 |
| `$concat` / `$substr` | 字符串拼接/截取 |
| `$year` / `$month` / `$dayOfMonth` | 日期提取 |

### 示例

```javascript
db.user.aggregate([
  { $match: { age: { $gt: 18 } } },
  { $group: { _id: "$gender", count: { $sum: 1 }, avgAge: { $avg: "$age" } } },
  { $project: { gender: "$_id", count: 1, avgAge: 1, _id: 0 } },
  { $sort: { count: -1 } }
])
```

---

## 6. 高可用与高扩展

### 副本集（高可用）

| 角色 | 说明 |
|------|------|
| **Primary** | 唯一可读写，记录 oplog |
| **Secondary** | 只读，同步 oplog，参与选举 |
| **Arbiter** | 仅选举，不存数据 |

> 选举：Raft 协议，多数投票。建议 3 节点（1 主 2 从），避免偶数（脑裂）。

### 分片集群（水平扩展）

| 组件 | 作用 |
|------|------|
| **Shard** | 存实际数据（每个分片是一个副本集） |
| **Config Server** | 存储集群配置（建议 3 节点） |
| **Mongos** | 路由，客户端只与 Mongos 交互 |

### 分片策略

| 策略 | 特点 |
|------|------|
| **范围分片** | 适合范围查询，易数据倾斜 |
| **哈希分片** | 数据均匀，只支持等值查询 |

---

## 7. 安全与备份恢复

### 安全机制

| 措施 | 说明 |
|------|------|
| 访问控制 | `--auth` + 角色权限（read/readWrite/dbAdmin/root） |
| SSL/TLS | 传输加密 |
| 存储加密 | WiredTiger 引擎加密 |
| 字段级加密 | 敏感字段单独加密 |

### 备份

```bash
mongodump --host 127.0.0.1 --port 27017 --db test --out /backup
```

### 恢复

```bash
mongorestore --host 127.0.0.1 --port 27017 --db test /backup/test
```

---

## 8. 应用场景

| 场景 | 说明 | 案例 |
|------|------|------|
| **互联网应用** | 用户画像、商品管理、内容管理 | 腾讯、阿里 |
| **IoT** | 设备数据高并发写入 | — |
| **实时分析** | 聚合管道统计报表 | — |
| **LBS** | 附近的人/商家 | Uber |
| **内容存储** | 博客/文章/图片 | — |
| **金融** | 交易日志 + 加密合规 | — |

---

## 9. MySQL 对比

| 维度 | MongoDB | MySQL |
|------|---------|-------|
| **数据模型** | 文档模型（BSON），无固定 schema | 关系模型（表-行-列），固定 schema |
| **关联** | 嵌套文档 + `$lookup`，较弱 | 外键 + JOIN，强关联 |
| **事务** | 多文档事务，性能略弱 | ACID，成熟稳定 |
| **扩展** | 原生分片集群，横向扩展强 | 分库分表，复杂度高 |
| **查询** | 聚合+全文检索+地理空间 | 复杂 SQL + JOIN |
| **适用** | 非结构化、高并发写入、快速迭代 | 结构化、强事务、强关联 |

---

> 🎯 **核心总结**：MongoDB = 文档型 NoSQL，BSON 存储。核心优势：灵活 schema + 原生分片 + 聚合管道。副本集保高可用，分片集群保高扩展。标配姿势：互联网业务数据 + IoT + LBS + 内容管理。
