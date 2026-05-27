# MongoDB 核心概念与核心价值（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | MongoDB 基础概念速查
> **版本**：MongoDB 6.x/7.x
> **核心场景**：文档存储、用户画像、内容管理、IoT 数据、实时分析

---

## 一、MongoDB 是什么

- **MongoDB**：开源**文档型 NoSQL** 数据库，以 **BSON（Binary JSON）** 格式存储数据
- 核心特点：**无固定 Schema、高灵活性、天然分布式、横向可扩展**
- 层级：Database → Collection → Document

### 1.1 MongoDB vs MySQL 核心差异

| 维度 | MongoDB | MySQL |
|---|---|---|
| **数据模型** | 文档模型（BSON），无固定 Schema | 关系模型（表-行-列），固定 Schema |
| **层级结构** | Database > Collection > Document | Database > Table > Row |
| **Schema** | 动态，字段随时增减 | 固定，需 ALTER TABLE |
| **关联** | 嵌套文档 / `$lookup` | JOIN / 外键 |
| **事务** | 支持（多文档/跨分片），性能略弱 | ACID 强事务，成熟稳定 |
| **扩展** | 原生分片，横向扩展 | 分库分表，复杂度高 |

---

## 二、核心价值（为什么用 MongoDB）

### 2.1 解决传统数据库痛点

| 痛点 | MySQL 问题 | MongoDB 方案 |
|---|---|---|
| **非结构化数据** | 存 JSON 字符串 / 宽表 | BSON 原生支持，含嵌套文档 |
| **字段多态** | ALTER TABLE 锁表 | 同一集合不同文档字段不同 |
| **快速迭代** | 需求变更需改表 | 无需维护 Schema |
| **横向扩展** | 分库分表复杂 | 原生分片集群 |
| **高并发写** | 行锁竞争 | WiredTiger 文档级锁 |

### 2.2 适合场景

```
✅ 用户画像     —— 字段多变，每个用户标签不同
✅ 内容管理     —— 文章/商品属性各异
✅ IoT 时序数据 —— 海量写入 + 横向扩展
✅ 实时分析     —— 聚合管道 + 索引
✅ LBS 应用     —— 地理空间索引
✅ 日志存储     —— JSON 格式 + 高吞吐
```

---

## 三、BSON 数据格式

BSON = Binary JSON，扩展了更多数据类型：

```json
{
  "_id": ObjectId("60d21b4667d0d8992e610c85"),
  "username": "zhangsan",
  "age": 25,
  "tags": ["Java", "Spring", "MongoDB"],
  "address": {
    "city": "Beijing",
    "street": "长安街 100 号"
  },
  "create_time": ISODate("2024-01-15T08:00:00Z"),
  "is_active": true,
  "score": 95.5,
  "avatar": BinData(0, "base64EncodedData...")
}
```

---

## 四、核心层级结构

```
MongoDB Instance（实例，端口 27017）
├── admin（系统库：权限）
├── local（系统库：本地数据）
├── config（系统库：分片配置）
├── db_user（业务库）
│   ├── user_profile（集合）
│   │   ├── {_id: 1, name: "张三", ...}（文档）
│   │   └── {_id: 2, name: "李四", ...}（文档）
│   └── user_log（集合）
│       └── ...
└── db_order（业务库）
    └── ...
```

| 层级 | 说明 | 类比 MySQL |
|---|---|---|
| **Instance** | 单个 MongoDB 进程，默认端口 27017 | MySQL Server |
| **Database** | 逻辑隔离，有独立权限 | Database |
| **Collection** | 文档的集合，无 Schema | Table |
| **Document** | BSON 格式键值对，有唯一 `_id` | Row |

---

## 五、ObjectId 主键

MongoDB 默认主键类型，12 字节：

```
ObjectId：507f1f77bcf86cd799439011
        ├─ 4 字节 Timestamp（时间戳）
        ├─ 5 字节 Machine ID（机器标识）
        ├─ 3 字节 Process ID（进程标识）
        └─ 3 字节 Counter（自增计数）

优势：分布式唯一，不依赖自增序列，天然有序
```

---

## 六、WiredTiger 存储引擎

MongoDB 3.2+ 默认引擎（已取代 MMAPv1）：

| 特性 | 说明 |
|---|---|
| **压缩存储** | snappy/zlib 压缩，节省 50%-80% 空间 |
| **文档级并发** | 文档级锁，高并发写入 |
| **多文档事务** | 支持 ACID 事务 |
| **内存管理** | 默认使用 50% 物理内存作为 Cache |
| **预写日志** | Journal（WAL）保障 crash 后数据恢复 |

---

## 七、面试核心要点

1. **MongoDB 是什么类型数据库？** 文档型 NoSQL，BSON 格式
2. **与 MySQL 最大的区别？** 无固定 Schema、横向扩展容易、事务弱于 MySQL
3. **BSON 是什么？** Binary JSON，比 JSON 多了日期、二进制等类型
4. **_id 默认类型是什么？** ObjectId，12 字节，分布式唯一
5. **什么时候用 MongoDB？** 字段多变、海量写入、需要灵活 Schema

---

## 八、极简总结

```
MongoDB = 文档型 NoSQL，存 BSON
层级 = Database > Collection > Document
优势 = 无 Schema、嵌套文档、分片集群、高写入
WiredTiger = 默认引擎（压缩 + 文档级锁 + 事务）
ObjectId = 分布式唯一主键，12 字节
```
