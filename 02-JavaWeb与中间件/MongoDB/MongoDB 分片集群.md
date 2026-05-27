# MongoDB 分片集群（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | MongoDB 分片集群详解
> **版本**：MongoDB 6.x/7.x
> **核心场景**：海量数据横向扩展、高并发读写

---

## 一、分片集群架构

```
                   Client
                      ↓
         ┌───────────────────────┐
         │    Mongos（路由层）     │  — 接收请求，转发分片
         │    * 可部署多个         │
         └────┬──────┬──────┬────┘
              ↓      ↓      ↓
      ┌────────┐ ┌────────┐ ┌────────┐
      │ Shard1 │ │ Shard2 │ │ Shard3 │  — 每个 Shard 是副本集
      │ (副本集)│ │ (副本集)│ │ (副本集)│
      └────────┘ └────────┘ └────────┘
              ↑      ↑      ↑
         ┌─────────────────────────┐
         │  Config Server（副本集）  │  — 存储分片路由元数据
         │  3 节点                  │
         └─────────────────────────┘
```

---

## 二、分片键（Shard Key）

### 2.1 分片键决定数据如何分布

```javascript
// 为集合启用分片
sh.shardCollection("mydb.users", { userId: "hashed" })
sh.shardCollection("mydb.orders", { createTime: 1 })
```

### 2.2 分片键选择标准

| 标准 | 说明 |
|---|---|
| **高基数** | 不同值多，数据分布均匀 |
| **查询频繁** | 常用查询能定位到单个分片（Targeted Query） |
| **非单调递增** | 避免数据全落到最后一个分片 |

### 2.3 两种分片策略

```javascript
// 1. 范围分片（Range Sharding）
sh.shardCollection("mydb.logs", { timestamp: 1 })
// Shard1: [2024-01 ~ 2024-06)
// Shard2: [2024-06 ~ 2024-12)
// ✅ 范围查询高效
// ❌ 写入热点（所有新建文档写最后一个 Shard）

// 2. 哈希分片（Hashed Sharding）
sh.shardCollection("mydb.users", { userId: "hashed" })
// hash(userId) 均匀分布到各 Shard
// ✅ 数据均匀分布，避免热点
// ❌ 不支持范围查询
```

### 2.4 分片键选择对比

| 维度 | 范围分片 | 哈希分片 |
|---|---|---|
| 数据分布 | 可能不均匀 | 均匀 |
| 范围查询 | 高效（Targeted） | 广播到所有 Shard |
| 等值查询 | 可精确定位 | 可精确定位 |
| 写入热点 | 容易产生 | 无 |
| 适用场景 | 时序数据 | 用户数据 |

---

## 三、Chunk 机制

### 3.1 什么是 Chunk

```
Shard = 多个 Chunk（数据块）的集合
Chunk = 分片键一个范围的数据（默认 64MB）

Shard1: [min, "abc") ["abc", "mno")
Shard2: ["mno", "xyz") ["xyz", max)
```

### 3.2 Chunk 拆分与迁移

```
1. Chunk 超过 64MB → MongoDB 自动拆分为两个
2. 各 Shard Chunk 数量不均 → Balancer 自动迁移
3. Chunk 迁移 = 从源 Shard 剪贴到目标 Shard（对客户端透明）
```

### 3.3 Balancer 配置

```javascript
// 查看 Balancer 状态
sh.isBalancerRunning()

// 停止 Balancer（维护时段）
sh.stopBalancer()

// 开启 Balancer
sh.startBalancer()

// 设置 Balancer 运行窗口（如凌晨）
sh.setBalancerWindow("02:00", "06:00")
```

---

## 四、分片集群部署

### 4.1 最小部署拓扑

```
3 个 Config Server（副本集） + 2 个 Shard（各为副本集） + 2 个 Mongos
```

### 4.2 配置启动

```bash
# Config Server 启动（添加 --configsvr 参数）
mongod --configsvr --replSet configReplSet --dbpath /data/config --port 27019 &

# Shard 启动（添加 --shardsvr 参数）
mongod --shardsvr --replSet shardReplSet1 --dbpath /data/shard1 --port 27018 &
mongod --shardsvr --replSet shardReplSet2 --dbpath /data/shard2 --port 27018 &

# Mongos 启动（连接 Config Server）
mongos --configdb configReplSet/host1:27019,host2:27019,host3:27019 --port 27017 &
```

### 4.3 添加 Shard

```javascript
// 连接到 Mongos
mongosh --port 27017

// 添加 Shard
sh.addShard("shardReplSet1/host1:27018,host2:27018")
sh.addShard("shardReplSet2/host3:27018,host4:27018")

// 查看 Shard 状态
sh.status()
```

---

## 五、分片集群查询行为

```javascript
// 在 Mongos 上执行
db.users.find({ userId: "user123" })
```

两种查询类型：

| 类型 | 说明 | 性能 |
|---|---|---|
| **Targeted Query** | 包含分片键，精确路由到 1 个 Shard | 高效 |
| **Scatter-Gather** | 不含分片键，广播到所有 Shard | 低效 |

```
// ✅ Targeted：包含分片键（userId）
db.users.find({ userId: "user123" })

// ❌ Scatter：不含分片键
db.users.find({ name: "张三" })
```

**结论**：查询时尽量带分片键，避免全分片扫描。

---

## 六、分片集群管理

```javascript
// 查看分片状态
sh.status()

// 查看所有数据库分片情况
sh.status().databases

// 查看 Chunk 分布
use config
db.chunks.find({ ns: "mydb.users" }).count()

// 手动均衡（分片键需为范围分片）
sh.splitAt("mydb.users", { userId: "user5000" })
sh.moveChunk("mydb.users", { userId: "user5000" }, "shardReplSet2")
```

---

## 七、Zone 分片（数据中心感知）

```javascript
// 为 Shard 打标签（如按机房）
sh.addShardTag("shardReplSet1", "bj")
sh.addShardTag("shardReplSet2", "sh")

// 为数据范围指定 Zone
sh.addTagRange("mydb.users",
  { userId: MinKey }, { userId: "M" }, "bj")    // A-M 存在北京
sh.addTagRange("mydb.users",
  { userId: "M" }, { userId: MaxKey }, "sh")     // N-Z 存在上海
```

---

## 八、分片键选错怎么办

分片键一旦确定**无法直接修改**：

```
方案：
1. 创建新集合（正确分片键）
2. 用 mongodump/mongorestore 或聚合 $out 迁移数据
3. 业务切换使用新集合
```

---

## 九、面试核心要点

1. **Mongos 干什么？** 路由请求，客户端不直接访问 Shard
2. **分片键能改吗？** 不能，要改只能新建集合迁移
3. **Targeted vs Scatter-Gather？** Targeted 包含分片键只查 1 Shard，Scatter 广播所有 Shard
4. **范围分片 vs 哈希分片？** 范围适合时序/范围查询，哈希均匀但等值查
5. **什么时候才需要分片？** 单节点无法满足容量或性能需求（通常 TB 级）

---

## 十、极简总结

```
分片 = Mongos 路由 + Config Server 配置 + Shard 副本集
分片键 = 数据分布的依据，后续不可修改
哈希分片 = 数据均匀，适合等值
范围分片 = 支持范围，易热点
Chunk = 最小迁移单元，默认 64MB
查询时尽量带分片键 = Targeted Query 精准高效
```
