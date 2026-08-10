# 03 - Schema 设计与数据模型

> 定位：建库前的唯一一次机会——Schema 基本不可变——DataType 全家族、字段属性、主键与向量维度设计——"先设计后建库"是 pymilvus 的第一纪律

---

## 📚 目录

1. [为什么 Schema 设计是头等大事](#1-为什么-schema-设计是头等大事)
2. [DataType 全家族](#2-datatype-全家族)
3. [字段属性详解](#3-字段属性详解)
4. [create_schema 与 add_field](#4-create_schema-与-add_field)
5. [设计规范：四问定 schema](#5-设计规范四问定-schema)
6. [Schema 变更能力](#6-schema-变更能力)
7. [常见坑](#7-常见坑)
8. [练习 5 题](#8-练习-5-题)

---

## 1. 为什么 Schema 设计是头等大事

Milvus 是**强 schema** 向量库：集合的字段结构在建库时定死，**主键类型、向量维度、字段数据类型、字段属性（null 与否、默认值）建了就不能改**——2.6 只能加可空标量字段（`add_collection_field`），3.0 扩展了"删标量字段与非末尾向量字段"（`drop_collection_field`），但**改类型、改维度、加向量字段、改主键仍然不可能**，只能删库重建。

这与传统数据库（ALTER TABLE 很平常）形成鲜明对比，意味着：**schema 设计错误 = 数据迁移或重建**。商品向量库建完发现"embedding 维度该是 768 不是 512"——删了重灌。所以设计纪律是：**先想清楚字段全集、主键方案、维度、分区键，再建库**——"一次建对，省掉未来全部重建"。

## 2. DataType 全家族

字段类型用 `DataType` 枚举（v2 强制枚举，不用字符串）：

| 类别 | 类型 | 说明 |
|------|------|------|
| 整数 | `INT64` / `INT32` | 主键几乎必用 INT64 |
| 浮点 | `FLOAT` / `DOUBLE` | 标量数值 |
| 文本 | `VARCHAR`（限长）/ `TEXT`（3.0 不限长） | 标题/内容 |
| 布尔 | `BOOL` | 开关标记 |
| JSON | `JSON` | 灵活元数据（键值对） |
| 数组 | `ARRAY`（element_type 必填） | 标签列表/ID 列表 |
| 稠密向量 | `FLOAT_VECTOR` / `FLOAT16_VECTOR` / `BFLOAT16_VECTOR` | 主力向量 |
| 稀疏向量 | `SPARSE_FLOAT_VECTOR` | 词袋/BM25 特征 |
| 二进制向量 | `BINARY_VECTOR` | 极低维哈希向量 |
| 结构 | `ARRAY<JSON>` 等嵌套（2.6.3 起 Array of structs） | 复杂嵌套数据 |

**选型要点**：向量字段一个集合可多个（多向量检索，如"标题向量 + 内容向量"）；稀疏向量与稠密向量可共存（混合检索的基础，07 篇 hybrid_search）；`VARCHAR` 必须给 `max_length`；`ARRAY` 必须给 `element_type`——**这两个属性漏配是建库报错高频原因**。

## 3. 字段属性详解

`add_field()` 时每个字段可带属性，核心四组：

**主键组**：`is_primary=True` 声明主键（默认 INT64 或 VARCHAR）；`auto_id=True` 让服务器自动生成主键（插入时不用传）。**2.6.3 起 auto_id 与自定义主键可以并存**——`auto_id=True` 时仍可指定自己的主键值（用于"业务 ID 就是主键"的场景），这是近年重要增强。

**容量组**：`max_length`（VARCHAR/ARRAY 必填）、`dim`（向量维度，稠密必填；稀疏向量不填 dim）。

**约束组**：`is_nullable=True`（允许空值，2.6 起标量字段可用）、`default_value`（缺省值）。注意：**主键字段不能 nullable、不能 default**。

**分区组**：`is_partition_key=True` 声明分区键——按该字段值自动分区的关键设计（多租户场景：tenant_id 设分区键，按租户隔离查询）。

**3.0 新增**：`TEXT` 字段（不限长文本）、字段级 `Ttl`（实体过期）。这些属性的取舍直接决定未来的查询能力——**"null 与 default 是一旦上线就难改的合同条款"**。

## 4. create_schema 与 add_field

完整 schema 定义的标准姿势（SDK v2 风格——一切走 client）：

```python
from pymilvus import MilvusClient, DataType

schema = MilvusClient.create_schema(auto_id=False, enable_dynamic_field=True)
schema.add_field(field_name="id", datatype=DataType.INT64, is_primary=True)
schema.add_field(field_name="title", datatype=DataType.VARCHAR, max_length=512)
schema.add_field(field_name="category", datatype=DataType.VARCHAR, max_length=64,
                 is_partition_key=True)
schema.add_field(field_name="tags", datatype=DataType.ARRAY,
                 element_type=DataType.VARCHAR, max_length=256)
schema.add_field(field_name="meta", datatype=DataType.JSON)
schema.add_field(field_name="embedding", datatype=DataType.FLOAT_VECTOR, dim=1024)
```

`create_schema()` 两个关键参数：**`auto_id`**（True 时服务器生成主键）、**`enable_dynamic_field`**（True 时允许插入不在 schema 里的字段——未声明字段自动进 `$meta` 动态字段，**生产 RAG 场景强烈建议开启**：文档来源、分片序号等临时字段不用预先声明）。schema 对象建好后传给 `create_collection(schema=schema)`（04 篇）——**schema 是"图纸"，create_collection 是"开工"**。

## 5. 设计规范：四问定 schema

设计任何集合前先回答四个问题，答案即 schema：

**第一问：主键用什么？**——业务已有稳定 ID（订单号、文档 ID）用 VARCHAR 主键或 INT64 承载；没有就用 `auto_id=True` 让服务器生成。**主键必须稳定且唯一**——它是 upsert/delete 的依据（05 篇）。

**第二问：向量怎么切？**——一个文档放一个 embedding 还是一个 chunk 一个 embedding？**chunk 级向量是 RAG 标准**（检索粒度 = 答案粒度）；多向量（标题 + 正文）需要时加第二个向量字段，**但维度在建库时定死——embedding 模型换了的代价是重建集合**。

**第三问：哪些字段要"可查询"？**——查询/过滤要用的字段（标题、时间、类目）建成结构化列（VARCHAR/INT64/JSON/ARRAY）；只存不查的放 JSON 或动态字段。**"检索字段列化，附属信息 JSON 化"是 schema 设计的黄金法则**。

**第四问：要不要分区/多租户？**——按 tenant_id/类目分区用 `is_partition_key`（查询自动限定分区，性能 + 隔离）；分区键建库定死，**后加分区键 = 重建集合**。

## 6. Schema 变更能力

| 操作 | 2.6 | 3.0 |
|------|-----|-----|
| 加可空标量字段 | `add_collection_field` 支持 | 支持 |
| 删标量字段/非末尾向量字段 | 不支持 | `drop_collection_field` 支持 |
| 改字段类型/维度/主键 | 不支持 | 不支持（重建） |
| 加向量字段 | 不支持 | 不支持（重建） |

**结论**：3.0 的 schema 变更只是"微调能力"，**结构性设计（主键/维度/分区键/字段类型）仍然一次性定死**——设计阶段投入的时间是未来所有重建成本的折现。

**3.0 的 drop_collection_field 代码姿势**（删除字段的显式操作）：

```python
client.drop_collection_field(collection_name="docs", field_name="obsolete_field")
```

约束：可删标量字段与非末尾向量字段；**函数生成的输出字段随函数删除而移除**；删除后客户端 schema cache 可能过期（09 篇坑四：重建客户端兜底）——**删字段是"擦除"不是"改"：能删的是不再需要的列，不能删的依然是主键/维度/类型**。

**一个完整的商品库 schema 设计示例**（四问的落地样例）：

```python
schema = MilvusClient.create_schema(auto_id=False, enable_dynamic_field=True)
schema.add_field("product_id", datatype=DataType.INT64, is_primary=True)     # 问1：主键
schema.add_field("title", datatype=DataType.VARCHAR, max_length=256)         # 问3：可查询
schema.add_field("price", datatype=DataType.FLOAT)                            # 问3：范围查询
schema.add_field("category_id", datatype=DataType.INT64, is_partition_key=True)  # 问4：分区键
schema.add_field("tags", datatype=DataType.ARRAY, element_type=DataType.VARCHAR,
                 max_length=128)                                              # 问3：数组包含过滤
schema.add_field("embedding", datatype=DataType.FLOAT_VECTOR, dim=1024)       # 问2：向量
# 附属信息（评分/上架时间/来源）走动态字段或 JSON，不建列
```

**对照理解**：四问的答案逐行落在 add_field 上——**"设计文档 = schema 代码"**，写 schema 就是写设计决策。

## 7. 常见坑

**坑一：VARCHAR 忘配 max_length、ARRAY 忘配 element_type**——建库直接报错（第 2 节已预警）。

**坑二：主键字段设了 nullable 或 default**——非法组合，报错；主键就是"必须有值且唯一"。

**坑三：embedding 模型没定就建库**——维度锁死后再换模型 = 删库重建；**先定 embedding 模型与向量维度，再写 schema 代码**。

**坑四：动态字段没开，插入 extra 字段报错**——没开 `enable_dynamic_field` 时插入 schema 外的字段会失败；不确定未来加什么字段就开（默认行为可控，查询时 `$meta` 可过滤）。

## 8. 练习 5 题

1. 为什么说 schema 设计是"一次机会"？2.6 和 3.0 各自能改什么？
2. DataType 家族里稠密/稀疏/二进制向量各适合什么场景？
3. auto_id 与自定义主键在 2.6.3 后如何共存？业务主键场景怎么选？
4. "检索字段列化，附属信息 JSON 化"具体指什么？动态字段解决什么？
5. 四问定 schema 是哪四问？为什么"先定 embedding 模型再写代码"？

> 🎯 **核心要点**：Schema 设计 = **DataType 选型（向量/标量/JSON/ARRAY）+ 字段属性（主键/维度/null/分区键）+ 四问设计法（主键、向量切分、可查询字段、分区）**；能力边界：**结构性设计建库定死，3.0 只给微调**——"设计阶段一小时，省掉未来无数重建"。

---

**下一模块**：[04-集合管理.md](04-集合管理.md) / **返回总览**：[00-milvus‑python总览.md](00-milvus‑python总览.md)
