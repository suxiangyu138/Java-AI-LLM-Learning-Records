# MongoDB 聚合操作（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | MongoDB 聚合管道详解
> **版本**：MongoDB 6.x/7.x
> **核心场景**：数据统计、报表、分组分析、多表关联

---

## 一、聚合管道概念

聚合管道（Aggregation Pipeline）将数据处理分成多个阶段，数据依次流过：

```
Collection → [$match] → [$group] → [$sort] → [$project] → 结果
             过滤        分组        排序        投影
```

---

## 二、核心管道阶段

| 阶段 | 作用 | 类比 SQL |
|---|---|---|
| `$match` | 过滤文档，放开头减少数据量 | WHERE |
| `$group` | 按字段分组，计算统计值 | GROUP BY |
| `$sort` | 排序 | ORDER BY |
| `$project` | 选择/计算/重命名字段 | SELECT |
| `$limit` | 限制返回数量 | LIMIT |
| `$skip` | 跳过文档 | OFFSET |
| `$lookup` | 跨集合关联查询 | LEFT JOIN |
| `$unwind` | 拆解数组字段 | - |
| `$addFields` | 添加新字段 | - |
| `$count` | 统计数量 | COUNT |

---

## 三、聚合函数

| 函数 | 说明 |
|---|---|
| `$sum` | 求和 |
| `$avg` | 求平均值 |
| `$min` / `$max` | 最小/最大值 |
| `$count` | 计数（7.x 新增） |
| `$first` / `$last` | 分组中的第一个/最后一个值 |
| `$push` | 将值收集到数组 |
| `$addToSet` | 收集到数组，自动去重 |

---

## 四、经典聚合示例

### 4.1 $match + $group（最常用组合）

```javascript
// 需求：统计各城市的成年用户数，按人数降序，只显示人数 > 10 的
db.users.aggregate([
  { $match: { age: { $gte: 18 } } },
  { $group: {
      _id: "$city",
      count: { $sum: 1 },
      avgAge: { $avg: "$age" },
      maxAge: { $max: "$age" }
  }},
  { $match: { count: { $gt: 10 } } },
  { $sort: { count: -1 } }
])
// SQL 等价:
// SELECT city, COUNT(*) AS count, AVG(age) AS avgAge, MAX(age) AS maxAge
// FROM users WHERE age >= 18
// GROUP BY city HAVING count > 10 ORDER BY count DESC
```

### 4.2 $project 字段转换

```javascript
db.users.aggregate([
  { $match: { age: { $gte: 18 } } },
  { $project: {
      nickname: "$name",            // 重命名
      age: 1,
      adult: { $gte: ["$age", 18] },  // 计算字段
      fullAddress: {                 // 拼接
        $concat: ["$address.city", "市", "$address.street"]
      },
      _id: 0                          // 隐藏 _id
  }}
])
```

### 4.3 $lookup 跨集合关联

```javascript
// 用户表 users: { _id, name, ... }
// 订单表 orders: { _id, userId, amount, ... }

// 需求：查每个用户及其所有订单
db.users.aggregate([
  { $match: { age: { $gte: 18 } } },
  { $lookup: {
      from: "orders",               // 关联的集合
      localField: "_id",            // users 的字段
      foreignField: "userId",       // orders 的字段
      as: "orderList"               // 结果放到此字段（数组）
  }},
  { $project: {
      name: 1,
      orderCount: { $size: "$orderList" },
      totalAmount: { $sum: "$orderList.amount" }
  }}
])
```

**注意**：`$lookup` 是 MongoDB 里最重的操作，大数据量下慎用。

### 4.4 $unwind 拆解数组

```javascript
// 原始文档：{ _id: 1, name: "张三", tags: ["Java", "Spring", "Docker"] }
// 需求：统计每个标签对应的人数

db.users.aggregate([
  { $unwind: "$tags" },       // 一行变三行
  // 结果：{ name: "张三", tags: "Java" }, { name: "张三", tags: "Spring" }, ...
  { $group: {
      _id: "$tags",
      count: { $sum: 1 }
  }},
  { $sort: { count: -1 } }
])
```

### 4.5 $bucket 分桶统计

```javascript
// 按年龄段分桶
db.users.aggregate([
  { $bucket: {
      groupBy: "$age",
      boundaries: [0, 18, 30, 45, 60, 200],
      default: "其他",
      output: {
        count: { $sum: 1 },
        names: { $push: "$name" }
      }
  }}
])
```

---

## 五、聚合管道优化

### 5.1 阶段顺序优化

```javascript
// ✅ $match 放开头，$sort 放中间，$project/$limit 放最后
db.users.aggregate([
  { $match: { age: { $gte: 18 } } },    // 先过滤
  { $sort: { age: -1 } },               // 再排序
  { $project: { name: 1 } }             // 最后投影
])
```

### 5.2 使用索引加速

```javascript
// $match 和 $sort 阶段可以利用索引
db.users.createIndex({ age: 1, city: 1 })

db.users.aggregate([
  { $match: { age: { $gte: 18 } } },   // ✅ 走索引
  { $sort: { city: 1 } }               // ✅ 走索引
])
```

### 5.3 允许磁盘使用

大数据量聚合超过 100MB 内存限制时：

```javascript
db.users.aggregate([...], { allowDiskUse: true })
```

---

## 六、聚合操作 vs MapReduce

| 维度 | Aggregation Pipeline | MapReduce |
|---|---|---|
| **性能** | 快（C++ 实现） | 慢（JS 引擎） |
| **易用性** | 声明式，易读 | 复杂，JS 函数 |
| **场景** | 日常统计 | 已被聚合管道替代 |
| **推荐** | ✅ 首选 | ❌ 不推荐 |

---

## 七、常用聚合快捷方法

```javascript
// 统计数量（比 find + count 快）
db.users.countDocuments({ age: { $gte: 18 } })

// 预估数量（非常快，但可能有误差，基于元数据）
db.users.estimatedDocumentCount()

// 去重获取所有城市
db.users.distinct("city")

// 去重 + 条件
db.users.distinct("city", { age: { $gte: 18 } })
```

---

## 八、面试核心要点

1. **聚合管道 vs 普通查询？** 管道可以链式处理，实现分组/统计/关联等复杂操作
2. **$lookup 性能如何？** 类似 SQL LEFT JOIN，大数据量下很慢，避免频繁使用
3. **$match 放什么位置？** 尽量放开头，减少后续阶段的数据量
4. **超过 100MB 内存怎么办？** 开启 `allowDiskUse: true`
5. **$unwind 干什么？** 将数组字段拆成多行，配合分组做标签统计等

---

## 九、极简总结

```
聚合管道 = 数据流水线
$match = WHERE（放最前面，能用索引）
$group = GROUP BY + 聚合函数（sum/avg/min/max）
$lookup = LEFT JOIN（大表慎用）
$unwind = 数组拆行
$project = SELECT（选字段、改名、计算）
优化 = $match 放开头 + 建索引 + 必要时 allowDiskUse
```
