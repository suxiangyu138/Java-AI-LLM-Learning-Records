# MongoDB 实战场景与最佳实践（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | MongoDB 生产级实战指南
> **版本**：MongoDB 6.x/7.x
> **核心场景**：用户画像、内容管理、IoT、LBS、日志存储

---

## 一、MongoDB 使用时机判断

| 场景 | 用 MongoDB？ | 说明 |
|---|---|---|
| 用户画像（标签多变） | ✅ 适合 | 字段灵活，不同用户不同标签 |
| 内容管理（文章/商品） | ✅ 适合 | 结构化+非结构化，嵌套文档自然表达 |
| IoT 时序数据 | ✅ 适合 | 高吞吐写入，分片集群横向扩展 |
| LBS 附近的人 | ✅ 适合 | 地理空间索引天然支持 |
| 日志存储 | ✅ 适合 | JSON 格式，capped collection |
| 金融交易（强制事务） | ❌ 不适合 | 事务弱于关系型数据库 |
| 复杂多层 JOIN | ❌ 不适合 | $lookup 性能差 |
| 简单 CRUD 后台 | ❌ 不适合 | MySQL 更成熟稳定 |

---

## 二、经典场景：用户画像系统

### 2.1 Schema 设计

```javascript
// 用户画像 — 不同用户字段完全不同，Schema-less 优势
{
  _id: ObjectId("..."),
  userId: "user_10001",
  basics: {
    name: "张三",
    age: 25,
    gender: "male",
    city: "北京"
  },
  // 多变标签：每个用户标签完全不同
  tags: {
    "消费能力": "高",
    "兴趣": ["科技", "游戏", "健身"],
    "设备": "iOS",
    "活跃度": "高频",
    "最近行为": "多次浏览电子产品"
  },
  metrics: {
    loginCount: 156,
    avgSessionTime: 1200,
    orderCount: 23,
    totalSpent: 15800.00
  },
  createTime: ISODate("2024-01-15T08:00:00Z"),
  updateTime: ISODate("2024-06-20T10:30:00Z")
}
```

### 2.2 Java 实体与查询

```java
@Data
@Document(collection = "user_profile")
public class UserProfile {
    @Id
    private String id;
    private String userId;
    private Map<String, Object> basics;    // 基础信息
    private Map<String, Object> tags;      // 多变标签
    private Map<String, Object> metrics;   // 指标
}

// 查询：标签"消费能力" = "高" 的用户
db.user_profile.find({ "tags.消费能力": "高" })

// Java 端
Query query = new Query(Criteria.where("tags.消费能力").is("高"));
List<UserProfile> highValueUsers = mongoTemplate.find(query, UserProfile.class);
```

---

## 三、经典场景：内容管理系统（CMS）

### 3.1 Schema 设计（嵌套 vs 引用）

```javascript
// ✅ 推荐：嵌套文档（读为主，数据不常变）
{
  _id: ObjectId("..."),
  title: "MongoDB 性能优化指南",
  author: {
    name: "张三",
    avatar: "https://cdn.example.com/avatar/123.jpg",
    level: "高级"
  },                                           // 作者信息嵌套
  content: "...",
  tags: ["MongoDB", "性能优化", "NoSQL"],
  comments: [                                   // 评论嵌套（适量）
    { user: "李四", content: "写得很好", time: ISODate("...") },
    { user: "王五", content: "很有帮助", time: ISODate("...") }
    // 若评论极多（数千+），则应单开集合用引用
  ],
  stats: {
    views: 5000,
    likes: 320,
    shares: 45
  },
  publishTime: ISODate("2024-06-01T10:00:00Z")
}
```

### 3.2 设计原则

| 关系类型 | 方案 | 适用场景 |
|---|---|---|
| **一对一** | 嵌套文档 | 作者信息 |
| **一对少**（几十条） | 嵌套文档 | 评论、点赞列表 |
| **一对多**（成千上万） | 引用（存 ID） | 订单明细 |
| **多对多** | 引用 | 用户收藏 |

---

## 四、经典场景：LBS 附近的人

```javascript
// 1. 创建地理空间索引
db.locations.createIndex({ position: "2dsphere" })

// 2. 插入位置数据
db.locations.insertOne({
  userId: "user_001",
  position: { type: "Point", coordinates: [116.4074, 39.9042] }  // [经度, 纬度]
})

// 3. 搜索附近 5 公里内的人
db.locations.find({
  position: {
    $near: {
      $geometry: { type: "Point", coordinates: [116.4074, 39.9042] },
      $maxDistance: 5000
    }
  }
})

// 4. 按距离排序 + 限制
db.locations.aggregate([
  { $geoNear: {
      near: { type: "Point", coordinates: [116.4074, 39.9042] },
      distanceField: "distance",
      maxDistance: 10000,
      spherical: true
  }},
  { $limit: 20 }
])
```

---

## 五、经典场景：IoT 时序数据

### 5.1 写入优化

```javascript
// 使用无序批量写入（ordered: false 忽略部分失败继续写入）
db.sensor_data.insertMany([
  { deviceId: "D001", ts: ISODate("..."), temp: 25.3, humidity: 60 },
  { deviceId: "D001", ts: ISODate("..."), temp: 25.5, humidity: 61 }
], { ordered: false })
```

### 5.2 分片策略

```javascript
// 分片键：deviceId（哈希） + ts（范围） → 复合分片键
sh.shardCollection("iot.sensor_data", { deviceId: "hashed", ts: 1 })

// 大多数查询：按 deviceId + 时间范围
// → 精准定位到 1 个分片（Targeted Query）
```

### 5.3 数据过期（TTL）

```javascript
// 30 天后自动删除（raw 数据只保留一段时间）
db.sensor_data.createIndex({ ts: 1 }, { expireAfterSeconds: 2592000 })
```

---

## 六、经典场景：日志存储

```javascript
// capped collection — 固定大小，自动淘汰旧数据
db.createCollection("app_logs", {
  capped: true,
  size: 104857600,    // 100MB
  max: 100000         // 最多 10 万条
})

// 写入（自然顺序）
db.app_logs.insertOne({
  level: "ERROR",
  service: "order-service",
  message: "支付超时",
  traceId: "abc123",
  timestamp: new Date()
})

// 查询最近日志（自然顺序就是插入顺序）
db.app_logs.find().sort({ $natural: -1 }).limit(100)
```

---

## 七、Schema 设计最佳实践

```
1. 尽量嵌套，少用引用     → 减少 JOIN，一次查询拿全数据
2. 子文档不要无限增长     → 评论太多就拆到单独集合
3. 数据同时存 + 算     → 存 totalSpent，不要每次都聚合算
4. 字段名尽量短         → MongoDB 存字段名本身，"n" 比 "name" 省空间
5. 合理使用数组         → 数组元素不要超过几千
6. 文档不要超过 16MB     → 大文件用 GridFS
```

---

## 八、技术难点与解决方案

| 难点 | 解决方案 |
|---|---|
| **数据倾斜** | 选高基数分片键，哈希分片，监控 Chunk 分布 |
| **事务性能** | 减少事务范围，优先单文档原子操作 |
| **$lookup 慢** | 改用嵌套设计，或预聚合后存快照 |
| **索引过多** | 仅高频查询建索引，定期 `explain` 分析 |
| **内存不足** | 增大 WiredTiger Cache 或升级内存 |
| **同步延迟大** | 增大 oplog，提升网络带宽，SSD |

---

## 九、SpringBoot 完整项目结构

```
src/main/java/com/example/demo/
├── config/MongoConfig.java          // 连接池、审计
├── entity/User.java                 // 文档实体
├── repository/UserRepository.java  // Repository 接口
├── service/UserService.java         // 业务逻辑
└── controller/UserController.java   // REST API
```

---

## 十、面试核心要点

1. **MongoDB 适合什么场景？** 字段多变、海量写入、无固定 Schema、LBS
2. **嵌套 vs 引用怎么选？** 一对少嵌套，一对多引用，读多嵌套
3. **为什么字段名要短？** 每个文档都存字段名，字段名长 = 浪费磁盘
4. **capped collection 是什么？** 固定大小环形队列，适合日志
5. **MongoDB 能替代 MySQL 吗？** 不能完全替代，各擅胜场

---

## 十一、极简总结

```
用户画像 = Schema-less 优势，标签随便加
CMS = 嵌套文档为主，减少 JOIN
LBS = 2dsphere 索引 + $near/$geoNear
IoT = 哈希分片键 + TTL 自动过期
日志 = capped collection 自动淘汰
Schema 设计 = 能嵌套不全引用，数组别太多，字段名短点
MongoDB + MySQL = 混合架构，各取所长
```
