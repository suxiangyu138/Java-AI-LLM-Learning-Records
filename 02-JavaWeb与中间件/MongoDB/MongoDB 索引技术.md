# MongoDB 索引技术（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | MongoDB 索引详解与优化
> **版本**：MongoDB 6.x/7.x
> **核心场景**：查询优化、性能提升、索引设计

---

## 一、索引核心概念

索引本质是**为查询搭建的高速通道**，避免全表扫描。

```
没有索引：全表扫描 → 百万文档 = 遍历百万次
有索引：B-Tree 查找 → 百万文档 = 约 20 次比较
```

`_id` 字段默认自动创建唯一索引。

---

## 二、索引类型

### 2.1 单字段索引

```javascript
// 升序索引
db.users.createIndex({ name: 1 })

// 降序索引
db.users.createIndex({ age: -1 })
```

### 2.2 复合索引

```javascript
// 联合索引：先 name 后 age
db.users.createIndex({ name: 1, age: -1 })

// 前缀原则：这个索引可以服务以下查询
db.users.find({ name: "张三" })                    // ✅ 走索引
db.users.find({ name: "张三", age: 25 })           // ✅ 走索引
db.users.find({ age: 25 })                         // ❌ 不走索引（非前缀）
```

**复合索引排序顺序影响覆盖范围，把区分度最高的字段放前面。**

### 2.3 多键索引（数组字段）

```javascript
// tags 是数组字段，自动为每个元素建索引
db.posts.createIndex({ tags: 1 })

// 查询单个 tag 走索引
db.posts.find({ tags: "Java" })
```

### 2.4 地理空间索引

```javascript
// 2dsphere：真实地理位置（经纬度）
db.locations.createIndex({ position: "2dsphere" })

// 查询附近 5 公里
db.locations.find({
  position: {
    $near: {
      $geometry: { type: "Point", coordinates: [116.4074, 39.9042] },
      $maxDistance: 5000  // 米
    }
  }
})

// 格式要求：
// GeoJSON: { type: "Point", coordinates: [经度, 纬度] }
```

### 2.5 文本索引（全文搜索）

```javascript
// 创建文本索引
db.articles.createIndex({ title: "text", content: "text" })

// 全文搜索
db.articles.find({ $text: { $search: "MongoDB 性能优化" } })

// 按相关度排序
db.articles.find(
  { $text: { $search: "MongoDB" } },
  { score: { $meta: "textScore" } }
).sort({ score: { $meta: "textScore" } })
```

**注意**：一个集合只能有一个文本索引。中文分词效果不如 ES。

### 2.6 哈希索引

```javascript
// 用于分片键，使数据均匀分布
db.users.createIndex({ userId: "hashed" })
// 只支持等值查询
db.users.find({ userId: "user123" })  // ✅
db.users.find({ userId: { $gt: "user123" } })  // ❌ 不支持范围
```

### 2.7 TTL 索引（自动过期）

```javascript
// 创建在 createTime 上，30 分钟后自动删除
db.sessions.createIndex(
  { createTime: 1 },
  { expireAfterSeconds: 1800 }
)
// 适用场景：验证码、临时会话、缓存
```

---

## 三、索引属性选项

```javascript
// 唯一索引
db.users.createIndex({ email: 1 }, { unique: true })

// 后台创建（不阻塞业务）
db.users.createIndex({ name: 1 }, { background: true })

// 部分索引（只索引满足条件的文档）
db.users.createIndex(
  { age: 1 },
  { partialFilterExpression: { age: { $gte: 18 } } }
)

// 稀疏索引（只索引有该字段的文档）
db.users.createIndex({ phone: 1 }, { sparse: true })

// 命名（便于管理）
db.users.createIndex({ name: 1, age: -1 }, { name: "idx_name_age" })
```

---

## 四、索引管理

```javascript
// 查看所有索引
db.users.getIndexes()

// 查看索引大小
db.users.totalIndexSize()

// 删除指定索引
db.users.dropIndex("idx_name_age")
db.users.dropIndex({ name: 1, age: -1 })

// 删除所有非 _id 索引
db.users.dropIndexes()

// 重建索引
db.users.reIndex()
```

---

## 五、查询计划分析（explain）

### 5.1 三种模式

```javascript
// 1. queryPlanner（默认）：展示执行计划
db.users.find({ name: "张三" }).explain()

// 2. executionStats：展示实际执行统计（最常用）
db.users.find({ name: "张三" }).explain("executionStats")

// 3. allPlansExecution：展示所有候选计划
db.users.find({ name: "张三" }).explain("allPlansExecution")
```

### 5.2 关键指标解读

```javascript
// executionStats 核心字段
{
  "executionStages": {
    "stage": "IXSCAN",          // COLLSCAN=全表扫描 IXSCAN=索引扫描
    "nReturned": 1,             // 返回文档数
    "totalDocsExamined": 1,     // 扫描文档数（越小越好）
    "totalKeysExamined": 1,     // 扫描索引条目
    "executionTimeMillis": 0,   // 执行时间
    "indexName": "name_1"       // 使用的索引
  }
}
```

### 5.3 判断索引是否生效

```
✅ totalDocsExamined 很小，接近 nReturned → 索引起作用
❌ totalDocsExamined 很大，接近全集数量 → 全表扫描
❌ stage = "COLLSCAN" → 没有用到索引
```

---

## 六、索引优化原则

| 原则 | 说明 |
|---|---|
| **优先高频查询** | 只为常用查询字段建索引 |
| **避免过多索引** | 每个索引都占用空间，写入时需更新所有索引 |
| **复合索引前缀** | 复合索引的查询必须匹配前缀字段 |
| **高区分度优先** | 区分度高的字段（如 email）优先放复合索引前面 |
| **覆盖查询** | 查询字段全被索引覆盖，无需回表 |
| **定期分析** | 定期 explain 分析慢查询，优化索引 |

### 6.1 索引失效场景

```javascript
// ❌ 正则不以 ^ 开头
db.users.find({ name: /三/ })

// ❌ 否定条件
db.users.find({ name: { $ne: "张三" } })

// ❌ 复合索引不按前缀
db.users.createIndex({ name: 1, age: 1 })
db.users.find({ age: 25 })  // 不走索引！

// ❌ 条件中使用函数或类型转换
db.users.find({ $where: "this.age > 20" })
```

---

## 七、慢查询诊断

```javascript
// 开启慢查询 Profile（> 100ms 记录）
db.setProfilingLevel(1, { slowms: 100 })

// 级别说明：
// 0 = 关闭
// 1 = 只记录慢查询
// 2 = 记录所有查询（仅调试用）

// 查看慢查询集合
db.system.profile.find().sort({ ts: -1 }).limit(10).pretty()

// 查看最近最慢的 5 条
db.system.profile.find()
  .sort({ millis: -1 })
  .limit(5)
  .project({ ns: 1, millis: 1, command: 1 })
```

---

## 八、面试核心要点

1. **索引类型有哪些？** 单字段、复合、多键、文本、哈希、地理空间、TTL
2. **复合索引的前缀原则是什么？** 查询必须匹配索引的**前几个字段**才能命中
3. **explain 中 COLLSCAN 意味着什么？** 全表扫描，没走索引
4. **索引有序选项 1 和 -1 有区别吗？** 单字段没区别，复合索引影响范围查询排序
5. **TTL 索引适用场景？** 验证码、Session、临时日志

---

## 九、极简总结

```
_id 默认唯一索引 → 不用重复建
复合索引 = 前缀匹配 → (a,b,c) 查 a = 走索引，查 b = 不走
explain("executionStats") = 看 totalDocsExamined 判断索引有效性
文本索引 = 全集合只能一个，中文不如 ES
TTL 索引 = 数据自动过期（验证码/日志）
```
