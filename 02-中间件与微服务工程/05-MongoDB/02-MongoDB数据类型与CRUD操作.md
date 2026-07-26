# MongoDB 数据类型与 CRUD 操作
> BSON 数据类型 + 增删改查完整速查——每日开发中最常用的 MongoDB 操作大全。

## 目录
1. [BSON 核心数据类型](#1-bson-核心数据类型)
2. [数据库与集合操作](#2-数据库与集合操作)
3. [插入文档](#3-插入文档)
4. [查询文档](#4-查询文档)
5. [更新文档](#5-更新文档)
6. [删除文档](#6-删除文档)
7. [查询条件运算符](#7-查询条件运算符)
8. [高级查询技巧](#8-高级查询技巧)
9. [批量操作与原子性](#9-批量操作与原子性)
10. [完整 CRUD 速查表](#10-完整-crud-速查表)

---

## 1. BSON 核心数据类型

### 1.1 数据类型速查表

| 类型 | 类型序号 | 说明 | 示例 |
|------|----------|------|------|
| String | 2 | UTF-8 字符串 | `"name": "MongoDB"` |
| Integer | 16 | 32 位整数 | `"age": 25` |
| Long | 18 | 64 位整数 | `"count": NumberLong("9000000000")` |
| Double | 1 | 浮点数（JS 数字默认） | `"score": 95.5` |
| Boolean | 8 | 布尔值 | `"isActive": true` |
| Date | 9 | UTC 日期，毫秒精度 | `"createTime": ISODate("2024-01-15T08:00:00Z")` |
| ObjectId | 7 | 默认主键，12 字节 | `_id: ObjectId("60d21b4667d0d8992e610c85")` |
| Array | 4 | 数组，元素可不同类型 | `"tags": ["Java", "MongoDB"]` |
| Embedded Document | 3 | 嵌套文档 | `"address": {"city": "BJ"}` |
| Null | 10 | 空值 | `"desc": null` |
| Binary Data | 5 | 二进制数据 | `BinData(0, "..."`)` |
| Decimal128 | 19 | 高精度小数（金融使用） | `"amount": NumberDecimal("99.99")` |
| Timestamp | 17 | 内部时间戳 | `"ts": Timestamp(1, 1712563200)` |
| Regular Expression | 11 | 正则表达式 | `"pattern": /^abc/i` |
| JavaScript | 13 | JS 代码 | `"func": Code("function() { return 1; }")` |
| Min/Max Key | -1/127 | 比较极值 | `$minKey / $maxKey` |

### 1.2 数值类型注意事项

```javascript
// JS Shell 中数字默认是 Double
db.numbers.insertOne({ val: 42 })              // Double
db.numbers.insertOne({ val: NumberInt(42) })    // Integer 32
db.numbers.insertOne({ val: NumberLong(42) })   // Integer 64

// 金融计算必须用 Decimal128（避免浮点精度误差）
db.orders.insertOne({ amount: NumberDecimal("19.99") })

// 浮点数精度问题
db.numbers.find({ val: { $eq: 0.1 + 0.2 } })   // 可能找不到！
// 应使用
db.numbers.find({ val: { $eq: NumberDecimal("0.3") } })
```

---

## 2. 数据库与集合操作

### 2.1 数据库操作

```javascript
// 切换/创建数据库（use + 插入数据 = 隐式创建）
use mydb

// 查看所有数据库
show dbs
// admin    100 kB
// config   100 kB
// local    100 kB
// mydb      72 kB

// 查看当前数据库
db

// 查看数据库统计
db.stats()
// {
//   db: "mydb",
//   collections: 2,
//   views: 0,
//   objects: 100,
//   avgObjSize: 256,
//   dataSize: 25600,
//   storageSize: 32768,
//   indexes: 2,
//   indexSize: 16384,
//   totalSize: 49152
// }

// 删除当前数据库
db.dropDatabase()
```

### 2.2 集合操作

```javascript
// 隐式创建（插入文档时自动创建）
db.users.insertOne({ name: "张三" })

// 显式创建
db.createCollection("users")

// 创建固定大小集合（capped collection，环形队列，适合日志）
db.createCollection("logs", {
  capped: true,
  size: 10485760,        // 10MB 上限，单位字节
  max: 10000              // 最多 10000 条
})

// capped 集合特性：
// - 按插入顺序存储，自动覆盖旧数据
// - 不能删除文档（只能 drop 整个集合）
// - 天然支持 TTL（时间维度自动淘汰）

// 查看所有集合
show collections

// 查看集合统计
db.users.stats()

// 查看集合详细信息
db.users.stats().wiredTiger

// 删除集合
db.users.drop()

// 重命名集合
db.users.renameCollection("user_profiles")
```

---

## 3. 插入文档

### 3.1 insertOne — 插入单个文档

```javascript
// 基本插入
db.users.insertOne({
  name: "张三",
  age: 25,
  email: "zhangsan@example.com",
  tags: ["Java", "Spring"],
  address: {
    city: "北京",
    street: "长安街",
    zip: "100000"
  },
  createdAt: new Date()
})
// 返回：{ acknowledged: true, insertedId: ObjectId("...") }

// 指定 _id（不指定则自动生成 ObjectId）
db.users.insertOne({
  _id: "user_001",
  name: "李四",
  age: 30
})

// insertOne 返回结果
// {
//   acknowledged: true,
//   insertedId: "user_001"
// }
```

### 3.2 insertMany — 批量插入

```javascript
// 批量插入
db.users.insertMany([
  { name: "王五", age: 28, email: "wangwu@example.com" },
  { name: "赵六", age: 22, email: "zhaoliu@example.com" },
  { name: "孙七", age: 35, email: "sunqi@example.com" }
])

// 有序插入（默认有序：遇到错误就停止）
db.users.insertMany([
  { _id: 1, name: "A" },
  { _id: 1, name: "B" },  // 重复 _id，报错停止
  { _id: 3, name: "C" }   // 不会插入
])

// 无序插入（跳过错误继续）
db.users.insertMany([
  { _id: 1, name: "A" },
  { _id: 1, name: "B" },  // 重复 _id，跳过
  { _id: 3, name: "C" }   // 会插入
], { ordered: false })

// 批量插入返回
// {
//   acknowledged: true,
//   insertedIds: { "0": ObjectId("..."), "1": ObjectId("..."), "2": ObjectId("...") }
// }
```

### 3.3 insert 与 save（历史方法，不推荐）

```javascript
// insert（MongoDB 4.0+ 废弃，用 insertOne/insertMany 替代）
db.users.insert({ name: "旧方法" })

// save（有 _id 则更新，无 _id 则插入）
db.users.save({ _id: "key", name: "upsert 行为" })
db.users.save({ name: "无_id_插入" })
```

### 3.4 插入验证

```javascript
// 查看集合中文档总数
db.users.countDocuments()

// 验证特定文档
db.users.findOne({ name: "张三" })
```

---

## 4. 查询文档

### 4.1 基础查询

```javascript
// 查询所有文档
db.users.find()

// 美化输出
db.users.find().pretty()

// 等值查询
db.users.find({ name: "张三" })

// IN 查询
db.users.find({ age: { $in: [25, 30, 35] } })

// 查询单个文档
db.users.findOne({ name: "张三" })

// 统计数量
db.users.countDocuments()
db.users.countDocuments({ age: { $gte: 18 } })

// 去重查询
db.users.distinct("city")
```

### 4.2 投影（Projection）—— 控制返回字段

```javascript
// 1 = 显示该字段，0 = 隐藏该字段
// _id 默认显示，除非显式指定 _id: 0

// 只返回 name 和 age，不返回 _id
db.users.find(
  { age: { $gt: 18 } },
  { name: 1, age: 1, _id: 0 }
)

// 排除 email 和 address
db.users.find(
  {},
  { email: 0, address: 0 }
)

// 注意：1 和 0 不能混用（除 _id 外）
// ❌ 错误：{ name: 1, email: 0 }  — 不能同时包含包含和排除
// ✅ 正确：{ name: 1, age: 1, _id: 0 }  — 只包含需要的字段
```

### 4.3 排序与分页

```javascript
// 排序：1 = 升序，-1 = 降序
db.users.find().sort({ age: -1 })          // 年龄从大到小
db.users.find().sort({ age: -1, name: 1 }) // 先按年龄降序，同岁按名字升序

// 分页
db.users.find()
  .sort({ createdAt: -1 })
  .skip(0)     // 跳过前 0 条
  .limit(20)   // 返回 20 条

// 第二页
db.users.find()
  .sort({ createdAt: -1 })
  .skip(20)    // 跳过前 20 条
  .limit(20)   // 返回 20 条

// 注意事项：
// - skip 在数据量大时性能差（需扫描跳过的文档）
// - 大数据量分页推荐使用 _id 或时间戳范围查询
```

### 4.4 字段存在性查询

```javascript
// 字段存在
db.users.find({ email: { $exists: true } })

// 字段不存在
db.users.find({ email: { $exists: false } })

// 字段类型
db.users.find({ age: { $type: "int" } })
db.users.find({ age: { $type: "double" } })
db.users.find({ age: { $type: ["int", "double"] } })

// 字段为空
db.users.find({ email: null })
// 注意：null 匹配不存在和显式设为 null 的字段
```

### 4.5 正则查询

```javascript
// 模糊匹配（MongoDB 正则走索引需注意性能）
db.users.find({ name: /张/ })               // 包含"张"
db.users.find({ name: /^张/ })              // 以"张"开头
db.users.find({ name: /张$/ })              // 以"张"结尾
db.users.find({ name: /张/i })              // 忽略大小写

// 使用 $regex 运算符
db.users.find({ name: { $regex: /张/ } })
db.users.find({ name: { $regex: "张", $options: "i" } })

// 正则 + 其他条件
db.users.find({
  name: /张/,
  age: { $gte: 18 }
})
```

### 4.6 查询游标操作

```javascript
// 游标遍历（大数据量时避免一次性加载到内存）
const cursor = db.users.find({ age: { $gt: 18 } })

// 获取下一个文档
cursor.next()

// 检查是否还有下一个
cursor.hasNext()

// 转为数组
cursor.toArray()

// 游标遍历
cursor.forEach(doc => {
  printjson(doc)
})

// 游标超时（默认 10 分钟无操作自动关闭）
// 在循环中持续操作不会超时
```

---

## 5. 更新文档

### 5.1 更新操作符速查

| 操作符 | 说明 | 示例 |
|--------|------|------|
| `$set` | 设置/覆盖字段值 | `{ $set: { age: 26 } }` |
| `$unset` | 删除字段 | `{ $unset: { tempField: "" } }` |
| `$inc` | 数值自增/自减 | `{ $inc: { loginCount: 1 } }` |
| `$mul` | 数值乘法 | `{ $mul: { price: 0.9 } }` |
| `$min` | 取最小值 | `{ $min: { age: 18 } }` |
| `$max` | 取最大值 | `{ $max: { age: 60 } }` |
| `$rename` | 字段重命名 | `{ $rename: { "oldName": "newName" } }` |
| `$push` | 数组追加元素 | `{ $push: { tags: "Docker" } }` |
| `$pull` | 数组移除元素 | `{ $pull: { tags: "Docker" } }` |
| `$addToSet` | 数组追加（去重） | `{ $addToSet: { tags: "Docker" } }` |
| `$pop` | 数组首尾弹出 | `{ $pop: { items: -1 } }` |
| `$each` | 批量追加数组 | `{ $push: { tags: { $each: ["A","B"] } } }` |
| `$slice` | 限制数组长度 | `{ $push: { tags: { $each: ["A"], $slice: -10 } } }` |
| `$position` | 指定数组插入位置 | `{ $push: { tags: { $each: ["A"], $position: 0 } } }` |

### 5.2 updateOne — 更新单个文档

```javascript
// 更新匹配的第一个文档
db.users.updateOne(
  { name: "张三" },          // 过滤条件
  { $set: { age: 26, city: "上海" } }  // 更新操作
)

// 数值自增
db.users.updateOne(
  { name: "张三" },
  { $inc: { loginCount: 1, score: -5 } }
)

// 数组追加
db.users.updateOne(
  { name: "张三" },
  { $push: { tags: "Docker" } }
)

// 数组去重追加
db.users.updateOne(
  { name: "张三" },
  { $addToSet: { tags: "Docker" } }
)

// 删除字段
db.users.updateOne(
  { name: "张三" },
  { $unset: { tempField: "" } }
)

// 返回更新后的文档
db.users.findOneAndUpdate(
  { name: "张三" },
  { $set: { age: 26 } },
  { returnDocument: "after" }
)
```

### 5.3 updateMany — 批量更新

```javascript
// 批量更新字段
db.users.updateMany(
  { age: { $lt: 18 } },
  { $set: { status: "minor" } }
)

// 批量自增
db.users.updateMany(
  { role: "vip" },
  { $inc: { bonusPoints: 100 } }
)

// 批量重命名字段
db.users.updateMany(
  {},
  { $rename: { "tel": "phone" } }
)
```

### 5.4 upsert — 有则更新无则插入

```javascript
// upsert: true — 找不到匹配文档时创建新文档
db.users.updateOne(
  { email: "newuser@example.com" },
  {
    $set: { name: "新用户", age: 22, createdAt: new Date() }
  },
  { upsert: true }
)
// 返回：{ matchedCount: 0, upsertedCount: 1, upsertedId: ObjectId("...") }

// upsert + 组合条件
db.analytics.updateOne(
  { page: "/home", date: "2024-07-26" },
  {
    $inc: { views: 1 },
    $setOnInsert: { createdAt: new Date() }
  },
  { upsert: true }
)
// $setOnInsert：只在插入时设置，更新时不修改
```

### 5.5 replaceOne — 替换整个文档

```javascript
// 替换整个文档（_id 不变，其他字段全部替换）
db.users.replaceOne(
  { name: "张三" },
  {
    name: "张三",
    age: 27,
    city: "深圳",
    updatedAt: new Date()
  }
  // 没有指定的字段会被删除！
)
```

### 5.6 更新数组内嵌文档

```javascript
// 数据示例
// {
//   _id: 1,
//   name: "张三",
//   scores: [
//     { subject: "语文", score: 85 },
//     { subject: "数学", score: 92 }
//   ]
// }

// 更新数组中特定元素（使用位置 $ 运算符）
db.students.updateOne(
  { _id: 1, "scores.subject": "数学" },
  { $set: { "scores.$.score": 95 } }
)

// 更新所有匹配数组元素（$[]）
db.students.updateOne(
  { _id: 1 },
  { $set: { "scores.$[].status": "passed" } }
)

// 使用 arrayFilters 精确匹配
db.students.updateOne(
  { _id: 1 },
  { $set: { "scores.$[elem].score": 99 } },
  { arrayFilters: [{ "elem.subject": "数学" }] }
)
```

### 5.7 更新选项参数

```javascript
// findOneAndUpdate 的完整选项
db.users.findOneAndUpdate(
  { name: "张三" },
  { $set: { age: 26 } },
  {
    projection: { name: 1, age: 1, _id: 0 },  // 返回字段
    sort: { createdAt: -1 },                   // 排序
    returnDocument: "after",                   // "after" | "before"
    upsert: true,                              // 无则插入
    maxTimeMS: 5000                            // 超时时间
  }
)
```

---

## 6. 删除文档

### 6.1 deleteOne — 删除单个文档

```javascript
// 删除第一个匹配的文档
db.users.deleteOne({ name: "张三" })

// 返回结果
// { acknowledged: true, deletedCount: 1 }
```

### 6.2 deleteMany — 批量删除

```javascript
// 删除所有符合条件的文档
db.users.deleteMany({ age: { $lt: 18 } })

// 删除所有文档（保留集合）
db.users.deleteMany({})

// 清空集合的两种方式对比
// db.users.deleteMany({})  — 逐条删，慢，保留索引
// db.users.drop()          — 直接删集合，快，全丢

// 删除指定日期之前的数据
db.logs.deleteMany({ createdAt: { $lt: new Date("2024-01-01") } })
```

### 6.3 findOneAndDelete — 查找并删除

```javascript
// 删除并返回被删除的文档
const deletedDoc = db.users.findOneAndDelete(
  { name: "张三" }
)
```

### 6.4 删除注意事项

```javascript
// 注意事项：
// - deleteOne 只删第一个匹配的，即使多个匹配
// - 删除操作不可回滚
// - 删集合（drop）比逐条删（deleteMany）快得多
// - capped 集合不能删除文档，只能 drop 整个集合
```

---

## 7. 查询条件运算符

### 7.1 比较运算符

| 运算符 | 含义 | 示例 |
|--------|------|------|
| `$eq` | 等于 | `{ age: { $eq: 25 } }` 或 `{ age: 25 }` |
| `$ne` | 不等于 | `{ age: { $ne: 25 } }` |
| `$gt` | 大于 | `{ age: { $gt: 18 } }` |
| `$gte` | 大于等于 | `{ age: { $gte: 18 } }` |
| `$lt` | 小于 | `{ age: { $lt: 60 } }` |
| `$lte` | 小于等于 | `{ age: { $lte: 60 } }` |
| `$in` | 在列表中 | `{ city: { $in: ["北京", "上海"] } }` |
| `$nin` | 不在列表中 | `{ city: { $nin: ["北京"] } }` |

```javascript
// 范围查询组合
db.users.find({ age: { $gte: 18, $lte: 60 } })

// 日期范围查询
db.orders.find({
  createdAt: {
    $gte: ISODate("2024-01-01"),
    $lt: ISODate("2024-02-01")
  }
})

// String 比较（按字典序）
db.users.find({ name: { $gte: "A", $lte: "Z" } })
```

### 7.2 逻辑运算符

```javascript
// AND（多条件默认即为 AND）
db.users.find({ age: { $gt: 18 }, city: "北京" })

// OR
db.users.find({
  $or: [
    { city: { $in: ["北京", "上海"] } },
    { age: { $gt: 60 } }
  ]
})

// NOR（不满足任何条件）
db.users.find({
  $nor: [
    { age: { $lt: 18 } },
    { status: "blocked" }
  ]
})

// NOT（取反）
db.users.find({ age: { $not: { $gt: 18 } } })

// 组合逻辑
db.users.find({
  $and: [
    { $or: [{ age: { $lt: 18 } }, { age: { $gt: 60 } }] },
    { status: "active" }
  ]
})
```

### 7.3 数组运算符

```javascript
// 数组包含指定元素
db.posts.find({ tags: "Java" })

// 数组包含所有指定元素（AND 关系）
db.posts.find({ tags: { $all: ["Java", "MongoDB"] } })

// 数组包含任意指定元素
db.posts.find({ tags: { $in: ["Java", "MongoDB"] } })

// 数组不包含
db.posts.find({ tags: { $nin: ["Java"] } })

// 数组长度
db.posts.find({ tags: { $size: 3 } })

// 数组元素匹配多个条件
db.posts.find({ tags: { $elemMatch: { $gte: "A", $lte: "M" } } })

// 数组索引查询
db.posts.find({ "tags.0": "Java" })  // 第一个元素是 Java
```

### 7.4 元素运算符

```javascript
// 字段存在
db.users.find({ email: { $exists: true } })

// 字段类型
db.users.find({ age: { $type: "number" } })
db.users.find({ age: { $type: ["int", "double"] } })

// 字段为 null 或不存在
db.users.find({ email: null })
```

---

## 8. 高级查询技巧

### 8.1 嵌套文档查询

```javascript
// 数据示例
// {
//   name: "张三",
//   address: {
//     city: "北京",
//     district: "海淀区",
//     detail: {
//       street: "中关村大街",
//       building: "创业大厦"
//     }
//   }
// }

// 点号访问嵌套字段（必须加引号）
db.users.find({ "address.city": "北京" })
db.users.find({ "address.detail.street": "中关村大街" })

// 精确匹配嵌套文档（顺序和字段必须完全一致）
db.users.find({ address: { city: "北京", district: "海淀区", detail: { street: "中关村大街", building: "创业大厦" } } })
```

### 8.2 数组嵌套查询

```javascript
// 数据示例
// {
//   _id: 1,
//   items: [
//     { productId: "P001", quantity: 2, price: 99.9 },
//     { productId: "P002", quantity: 1, price: 199.9 }
//   ]
// }

// 数组中嵌套文档的精确匹配
db.orders.find({ "items.productId": "P001" })

// 数组中嵌套文档的多条件匹配（单个元素内）
db.orders.find({
  items: {
    $elemMatch: {
      productId: "P001",
      quantity: { $gte: 2 }
    }
  }
})

// 无 $elemMatch：条件可以分散在不同元素上
db.orders.find({
  "items.productId": "P001",
  "items.quantity": { $gte: 2 }
})
// 注意：这匹配的是任意元素满足 productId=P001 且任意元素满足 quantity>=2
// 不一定是同一个元素的组合
```

### 8.3 文本搜索

```javascript
// 先创建文本索引
db.articles.createIndex({ title: "text", content: "text" })

// 基本搜索
db.articles.find({ $text: { $search: "mongodb 教程" } })

// 搜索并排序（按文本相关性）
db.articles.find(
  { $text: { $search: "mongodb 教程" } },
  { score: { $meta: "textScore" } }
).sort({ score: { $meta: "textScore" } })

// 短语搜索（双引号）
db.articles.find({ $text: { $search: "\"mongodb 教程\"" } })

// 排除词（减号）
db.articles.find({ $text: { $search: "mongodb -mysql" } })
```

### 8.4 查询性能提示

```javascript
// 1. 限制返回字段（减少网络传输）
db.users.find({}, { name: 1, email: 1 })

// 2. 使用 limit 限制结果数
db.users.find().limit(100)

// 3. 合理安排查询条件顺序（索引区分度高的放前面）
// 查询：city="北京" AND age > 18
// 索引：{ city: 1, age: 1 }
// 等值条件（city）放前面 -> 快速缩小范围

// 4. 尽量避免正则前导通配
// ❌ db.users.find({ name: /张/ })    — 不走索引
// ✅ db.users.find({ name: /^张/ })   — 可走索引
```

---

## 9. 批量操作与原子性

### 9.1 bulkWrite — 批量写入

```javascript
// 批量混合操作（原子执行）
db.users.bulkWrite([
  { insertOne: { document: { name: "新用户", age: 20 } } },
  { updateOne: {
    filter: { name: "张三" },
    update: { $set: { age: 26 } }
  }},
  { deleteOne: {
    filter: { name: "赵六" }
  }},
  { replaceOne: {
    filter: { name: "李四" },
    replacement: { name: "李四", age: 31, city: "广州" }
  }}
])

// 有序/无序
db.users.bulkWrite([...], { ordered: false })
// ordered: true（默认） — 顺序执行，遇到错误停止
// ordered: false — 无序执行，跳过错误继续
```

### 9.2 原子性说明

```javascript
// 单个文档级别的操作是原子的
// insertOne / updateOne / deleteOne 都是原子操作

// 批量操作（bulkWrite）在同一集合中可视为原子
// 但涉及多个文档的单独操作并非整体原子

// 多文档原子性的解决方案：
// 1. MongoDB 4.0+ 多文档事务（类似 RDBMS）
// 2. 嵌套文档（将相关数据放在一个文档中）
// 3. 乐观锁（版本号控制）
```

### 9.3 乐观锁实现

```javascript
// 使用版本号字段实现乐观锁
// 初始数据：{ _id: 1, name: "张三", balance: 100, version: 1 }

// 更新时检查版本号
const result = db.accounts.updateOne(
  { _id: 1, version: 1 },          // 检查版本号
  { $inc: { balance: -50 }, $set: { version: 2 } }
)

if (result.modifiedCount === 0) {
  // 版本冲突，需要重试
  print("并发冲突，请重试")
}
```

---

## 10. 完整 CRUD 速查表

### 10.1 插入

| 操作 | 方法 | 说明 |
|------|------|------|
| 单条插入 | `db.col.insertOne({...})` | 返回 insertedId |
| 批量插入 | `db.col.insertMany([...])` | 有序/无序 |
| 批量混合 | `db.col.bulkWrite([...])` | insert/update/delete 混用 |

### 10.2 查询

| 操作 | 方法 | 说明 |
|------|------|------|
| 所有文档 | `db.col.find()` | 返回游标 |
| 单条 | `db.col.findOne(filter)` | 返回文档或 null |
| 条件查询 | `db.col.find({field: value})` | 等值匹配 |
| 范围查询 | `db.col.find({field: {$gt: 10}})` | 比较运算符 |
| 投影 | `.find(filter, {field: 1})` | 控制返回字段 |
| 排序 | `.sort({field: -1})` | 1 升序，-1 降序 |
| 分页 | `.skip(n).limit(m)` | skip 大数据量慎用 |
| 计数 | `db.col.countDocuments(filter)` | 精确计数 |

### 10.3 更新

| 操作 | 方法 | 说明 |
|------|------|------|
| 单条更新 | `db.col.updateOne(filter, update)` | 更新第一个匹配 |
| 批量更新 | `db.col.updateMany(filter, update)` | 更新所有匹配 |
| 替换文档 | `db.col.replaceOne(filter, replacement)` | 完全替换（除 _id） |
| 查找并更新 | `db.col.findOneAndUpdate(filter, update)` | 返回更新前/后文档 |
| 字段设置 | `{ $set: { field: value } }` | 设置或覆盖字段 |
| 数值增减 | `{ $inc: { field: 1 } }` | 原子增减 |
| 数组追加 | `{ $push: { field: value } }` | 追加到数组 |
| 数组移除 | `{ $pull: { field: value } }` | 移除匹配元素 |
| 删除字段 | `{ $unset: { field: "" } }` | 移除字段 |
| 有则更新无则插入 | `{ upsert: true }` | 参数选项 |

### 10.4 删除

| 操作 | 方法 | 说明 |
|------|------|------|
| 单条删除 | `db.col.deleteOne(filter)` | 删除第一个匹配 |
| 批量删除 | `db.col.deleteMany(filter)` | 删除所有匹配 |
| 清空集合 | `db.col.deleteMany({})` | 保留集合和索引 |
| 删除集合 | `db.col.drop()` | 连集合带索引全删 |
| 查找并删除 | `db.col.findOneAndDelete(filter)` | 返回被删文档 |

### 10.5 常见查询模板

```javascript
// 带分页的完整查询
db.users.find(
  { age: { $gte: 18, $lte: 60 }, city: "北京" },
  { name: 1, age: 1, city: 1, _id: 0 }
).sort({ age: -1 }).skip(0).limit(20)

// 日期范围 + 排序
db.orders.find({
  createdAt: {
    $gte: ISODate("2024-01-01"),
    $lt: ISODate("2024-02-01")
  },
  status: "completed"
}).sort({ amount: -1 })

// 聚合式更新（upsert 统计）
db.pageviews.updateOne(
  { page: "/home", date: "2024-07-26" },
  { $inc: { views: 1 } },
  { upsert: true }
)
```

---

## 面试核心

| 问题 | 答案 |
|------|------|
| `$set` vs `replaceOne` 区别？ | `$set` 只更新指定字段，`replaceOne` 覆盖整个文档（`_id` 除外） |
| upsert 是什么？ | 有则更新，无则插入，param `{ upsert: true }` |
| 嵌套文档怎么查？ | 用点号：`"address.city": "北京"` |
| capped collection 是什么？ | 固定大小环形队列集合，适合日志 |
| `$push` vs `$addToSet` 区别？ | `$push` 总是添加，`$addToSet` 去重后添加 |
| `$pull` vs `$pop` 区别？ | `$pull` 按值移除，`$pop` 按位置移除 |
| 如何更新数组中的特定元素？ | 使用位置运算符 `$` 或 `arrayFilters` |
| ObjectId 包含哪些信息？ | 时间戳 + 机器标识 + 进程标识 + 随机计数器 |
| 单个操作是否是原子的？ | 单个文档级别的操作（insertOne/updateOne/deleteOne）是原子的 |
| 如何做乐观锁？ | 使用版本号字段，更新时 where version = oldVersion |

> **极简总结**：insertOne/Many 插入，find + 条件查询，update + `$set`/`$inc`/`$push` 更新，deleteOne/Many 删除，点号访问嵌套字段。
