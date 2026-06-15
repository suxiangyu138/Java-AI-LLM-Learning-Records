# MongoDB 数据类型与 CRUD 操作（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | MongoDB 基础操作速查大全
> **版本**：MongoDB 6.x/7.x
> **核心场景**：日常开发中的增删改查操作

---

## 一、BSON 核心数据类型

| 类型 | 说明 | 示例 |
|---|---|---|
| **String** | UTF-8 字符串 | `"name": "MongoDB"` |
| **Integer** | 32 位整数 | `"age": 25` |
| **Long** | 64 位整数 | `"count": NumberLong("9000000000")` |
| **Double** | 浮点数（默认） | `"score": 95.5` |
| **Boolean** | 布尔值 | `"isActive": true` |
| **Date** | UTC 日期 | `"createTime": ISODate("2024-01-15T08:00:00Z")` |
| **ObjectId** | 默认主键，12字节 | `_id: ObjectId("60d...")` |
| **Array** | 数组，元素可不同类型 | `"tags": ["Java", "MongoDB"]` |
| **Embedded Document** | 嵌套文档 | `"address": {"city": "BJ", "street": "xxx"}` |
| **Null** | 空值 | `"desc": null` |
| **Binary Data** | 二进制（图片/文件） | `"avatar": BinData(0, "xxx")` |
| **Decimal128** | 高精度小数（金融） | `"amount": NumberDecimal("99.99")` |

---

## 二、数据库操作

```javascript
// 切换/创建数据库（use 不存在的库 + 插入数据 = 自动创建）
use mydb

// 查看当前数据库
db

// 查看所有数据库（只显示有数据的）
show dbs

// 查看当前库统计
db.stats()

// 删除当前数据库
db.dropDatabase()
```

---

## 三、集合操作

```javascript
// 创建集合
db.createCollection("users")

// 创建固定大小集合（capped，类似环形队列，常用于日志）
db.createCollection("logs", { capped: true, size: 10485760, max: 10000 })

// 查看所有集合
show collections

// 查看集合统计
db.users.stats()

// 删除集合
db.users.drop()
```

---

## 四、CRUD 操作

### 4.1 插入（Create）

```javascript
// 插入单个文档
db.users.insertOne({
  name: "张三",
  age: 25,
  email: "zhangsan@example.com",
  tags: ["Java", "Spring"],
  address: { city: "北京", street: "长安街" }
})
// 返回: { acknowledged: true, insertedId: ObjectId("...") }

// 插入多个文档
db.users.insertMany([
  { name: "李四", age: 30, email: "lisi@example.com" },
  { name: "王五", age: 28, email: "wangwu@example.com" }
])

// 指定 _id（不指定则自动生成 ObjectId）
db.users.insertOne({ _id: "custom_id_001", name: "赵六" })
```

### 4.2 查询（Read）

```javascript
// 查询所有
db.users.find()

// 等值查询
db.users.find({ name: "张三" })
db.users.find({ age: 25 })

// 返回指定字段（1=显示, 0=隐藏, _id 默认显示）
db.users.find({}, { name: 1, age: 1, _id: 0 })

// 查询单个
db.users.findOne({ name: "张三" })

// 排序（1=升序, -1=降序）
db.users.find().sort({ age: -1 })

// 分页
db.users.find().skip(10).limit(20)

// 统计数量
db.users.countDocuments({ age: { $gt: 18 } })

// 去重
db.users.distinct("city")

// 存在判断
db.users.find({ email: { $exists: true } })
```

### 4.3 更新（Update）

```javascript
// 更新单个文档（$set 修改指定字段）
db.users.updateOne(
  { name: "张三" },        // 条件
  { $set: { age: 26, city: "上海" } }  // 更新内容
)

// 更新多个文档
db.users.updateMany(
  { age: { $lt: 18 } },
  { $set: { status: "minor" } }
)

// 数值自增
db.users.updateOne(
  { name: "张三" },
  { $inc: { loginCount: 1 } }
)

// 追加数组元素
db.users.updateOne(
  { name: "张三" },
  { $push: { tags: "Docker" } }
)

// 删除字段
db.users.updateOne(
  { name: "张三" },
  { $unset: { tempField: "" } }
)

// 替换整个文档（覆盖除 _id 外的所有字段）
db.users.replaceOne(
  { name: "张三" },
  { name: "张三", age: 27, city: "深圳" }
)

// upsert：有则更新，无则插入
db.users.updateOne(
  { name: "新用户" },
  { $set: { age: 22 } },
  { upsert: true }
)
```

### 4.4 删除（Delete）

```javascript
// 删除单个
db.users.deleteOne({ name: "张三" })

// 删除多个
db.users.deleteMany({ age: { $lt: 18 } })

// 清空集合
db.users.deleteMany({})          // 逐条删，慢
db.users.drop()                  // 直接删集合，快
```

---

## 五、查询条件运算符

### 5.1 比较运算符

| 运算符 | 含义 | 示例 |
|---|---|---|
| `$eq` | 等于 | `{ age: { $eq: 25 } }` |
| `$ne` | 不等于 | `{ age: { $ne: 25 } }` |
| `$gt` | 大于 | `{ age: { $gt: 18 } }` |
| `$gte` | 大于等于 | `{ age: { $gte: 18 } }` |
| `$lt` | 小于 | `{ age: { $lt: 60 } }` |
| `$lte` | 小于等于 | `{ age: { $lte: 60 } }` |
| `$in` | 在列表中 | `{ city: { $in: ["北京", "上海"] } }` |
| `$nin` | 不在列表中 | `{ city: { $nin: ["北京", "上海"] } }` |

### 5.2 逻辑运算符

```javascript
// AND（多条件默认就是 AND）
db.users.find({ age: { $gt: 18 }, city: "北京" })

// 显式 $and
db.users.find({
  $and: [
    { age: { $gt: 18 } },
    { city: "北京" }
  ]
})

// OR
db.users.find({
  $or: [
    { city: "北京" },
    { city: "上海" }
  ]
})

// NOT
db.users.find({ age: { $not: { $gt: 18 } } })

// NOR（既不也不）
db.users.find({ $nor: [{ city: "北京" }, { age: { $gt: 60 } }] })
```

### 5.3 数组运算符

```javascript
// 数组包含指定元素
db.posts.find({ tags: "Java" })

// 数组包含所有指定元素
db.posts.find({ tags: { $all: ["Java", "MongoDB"] } })

// 数组精确匹配（顺序也必须一致）
db.posts.find({ tags: ["Java", "MongoDB"] })

// 数组长度
db.posts.find({ tags: { $size: 3 } })
```

### 5.4 正则表达式

```javascript
// 模糊搜索
db.users.find({ name: /zhang/ })

// 忽略大小写
db.users.find({ name: /zhang/i })

// 匹配开头
db.users.find({ name: /^张/ })
```

---

## 六、高级查询技巧

```javascript
// 嵌套文档查询
db.users.find({ "address.city": "北京" })

// 数组嵌套查询
db.orders.find({ "items.productId": "P001" })

// 条件 + 字段筛选 + 排序 + 分页 组合
db.users.find(
  { age: { $gte: 18, $lte: 60 } },       // 条件
  { name: 1, age: 1, city: 1, _id: 0 }   // 字段
).sort({ age: -1 })                      // 排序
 .skip(0)                                 // 跳过
 .limit(20)                               // 限制
```

---

## 七、面试核心要点

1. **MongoDB 默认主键是什么？** ObjectId，12 字节，分布式唯一
2. **$set vs replaceOne 区别？** $set 只更新指定字段，replaceOne 覆盖整个文档
3. **upsert 是什么意思？** 有则更新，无则插入
4. **嵌套文档怎么查？** 用点号：`"address.city": "北京"`
5. **capped collection 是什么？** 固定大小的环形队列集合，适合日志

---

## 八、极简总结

```
insertOne/insertMany = 插入
find({条件}, {字段}) = 查询
updateOne/updateMany + $set/$inc/$push = 更新
deleteOne/deleteMany = 删除
$gt/$lt/$in/$and/$or = 条件组合
嵌套文档 = address.city 点号访问
```
