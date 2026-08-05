# 04 Schema 设计与数据写入

> Schema 是 Milvus 的"建表语句"，写入是数据管道的入口——设计得好不好，决定检索能不能快、数据能不能管

---

## 📚 目录

1. [Schema 设计最佳实践（字段清单模板）](#1-schema-设计最佳实践字段清单模板)
2. [写入 API：insert / upsert / delete](#2-写入-apiinsert--upsert--delete)
3. [批量导入与数据管道](#3-批量导入与数据管道)
4. [数据一致性保证](#4-数据一致性保证)
5. [写入性能验证与数据核对](#5-写入性能验证与数据核对)
6. [3.0 在线 Schema 演进](#6-30-在线-schema-演进)

---

## 1. Schema 设计最佳实践（字段清单模板）

**RAG 知识库的标准 Schema 模板**（生产级字段清单）：

```python
from pymilvus import FieldSchema, DataType

fields = [
    # ① 主键：外部系统 ID 作主键（幂等锚点，见 02 模块）
    FieldSchema(name="id", dtype=DataType.VARCHAR, max_length=128, is_primary=True),

    # ② 溯源字段：检索结果要能回指原文
    FieldSchema(name="document_id", dtype=DataType.VARCHAR, max_length=64),
    FieldSchema(name="chunk_index", dtype=DataType.INT64),      # 分块序号
    FieldSchema(name="title", dtype=DataType.VARCHAR, max_length=512),
    FieldSchema(name="content", dtype=DataType.VARCHAR, max_length=65535),  # 或 TEXT（3.0）

    # ③ 元数据：灵活扩展（可过滤）
    FieldSchema(name="metadata", dtype=DataType.JSON),

    # ④ 向量：稠密（语义检索）
    FieldSchema(name="embedding", dtype=DataType.FLOAT_VECTOR, dim=1536),
    # ⑤ 稀疏（全文检索，2.5+ 混合检索用）
    FieldSchema(name="sparse", dtype=DataType.SPARSE_FLOAT_VECTOR),
]

schema = CollectionSchema(fields=fields,
        description="RAG 文档知识库",
        enable_dynamic_field=True)      # 动态字段兜底（偶发字段）
```

**设计五原则**：

| 原则 | 说明 |
|------|------|
| 主键 = 外部 ID | 文档管道幂等（重跑 upsert 不重复） |
| 检索字段显式声明 | `output_fields` 只返回需要字段（**不返回 embedding**——维度大浪费带宽） |
| 过滤字段进 Schema | 频繁过滤的字段（status/tenant）必须显式定义（可建标量索引） |
| 全文检索字段用 TEXT | 3.0 起正文用 TEXT（BM25 原生支持） |
| 动态字段做兜底 | `enable_dynamic_field=True` 但**不依赖它做过滤**（$meta 不可索引） |

> 🎯 **核心要点**：Schema 设计 = "**主键（幂等）+ 溯源（document_id/chunk_index）+ 过滤字段（可索引）+ 向量（稠/稀）**"四件套——模板照抄再按业务调整，生产 Schema 不踩坑。

---

## 2. 写入 API：insert / upsert / delete

```python
# ① insert：新增（主键冲突会报错或忽略，视配置）
collection.insert([
    {"id": "doc_001:0", "document_id": "doc_001", "chunk_index": 0,
     "title": "Spring AI 入门", "content": "...",
     "embedding": [0.1, 0.2, ...], "sparse": {1: 0.5, 100: 0.8}}
])

# ② upsert：主键存在更新、不存在插入（2.4+，原子）—— 增量更新首选
collection.upsert([{...}])          # 文档修改后重跑管道，直接覆盖

# ③ delete：按表达式删除
collection.delete(expr='document_id == "doc_001"')      # 删整个文档的所有 chunk
collection.delete(expr='id in ["doc_001:0", "doc_001:1"]')

# ④ flush：强制落盘（默认异步，等持久化可 flush 或等待）
collection.flush()
```

**写入的工程注意**：

| 注意 | 说明 |
|------|------|
| 异步落盘 | insert 默认异步——写入后立即检索可能查不到（一致性级别，见 02 模块） |
| 批量写入 | 大批量用 `insert(data_list)` 一次传（内部自动分批）——**别逐条 insert**（性能差） |
| 主键冲突 | insert 重复主键会失败/忽略——**增量更新用 upsert** |
| embedding 对齐 | 向量维数必须与 Schema 声明一致（1536 维模型输出 1536 维向量） |
| 稀疏向量格式 | 字典形式 `{token_id: weight}`——由 BM25 内置函数或 Splade 生成 |

> 🎯 **核心要点**：写入三 API 的分工 = "**首次入库存 insert、增量更新用 upsert、删除用表达式**"——RAG 知识库的日常运维（新增/修改/删除文档）就是这三个调用的组合。

---

## 3. 批量导入与数据管道

**全量导入 vs 流式写入**（大数据量场景）：

```python
# 方式 1：批量写入（百万级以内，代码简单）
for batch in chunks(data, 1000):        # 每批 1000 条
    collection.insert(batch)

# 方式 2：bulk 导入（亿级，从对象存储文件导入）
# Milvus 支持从 JSON/Parquet/NumPy 文件 bulk 导入（BulkWriter 生成文件 → import）
from pymilvus.bulk_writer import bulk_import
# 流程：数据管道生成 Parquet/JSON → 上传 S3 → bulk_import 触发导入任务

# 方式 3：数据湖直读（3.0 外部集合，见 01 模块）—— 零拷贝
# 数据在 Lance/Iceberg/Parquet → 直接建外部集合 → 增量同步
```

**RAG 知识库的完整数据管道**（标准链路）：

```text
文档源（PDF/Word/Markdown）
  → 解析与清洗（去页眉页脚/图片）
  → 切分（chunk：段落边界 + 重叠，见 RAG 体系 03 模块）
  → Embedding（稠密向量生成）
  → 稀疏向量（BM25 内置函数自动生成 / Splade）
  → upsert 到 Milvus（主键 = 文档ID:chunk序号）
  → （文档更新）重跑管道 → upsert 覆盖 + 删除旧 chunk
```

> 🎯 **核心要点**：写入规模决定方式——**百万级代码批量、亿级 bulk 导入、湖仓场景 3.0 外部集合**。管道的主键设计（幂等）让"重跑"成为安全操作（见 02 模块第 4 节）。

---

## 4. 数据一致性保证

**写入后何时可检索**——理解 Milvus 的写入可见性：

```text
insert → 写入 DataNode 内存（可查部分数据？不——）
  → flush/异步落盘为段（Segment）→ 段被标记可查询
  → 索引构建完成 → 查询走索引
关键认知：
① 2.x 中：默认"写入后（flush 前）部分可见"，正式可查依赖 flush 与一致性级别
② Bounded 一致性（默认）：写入约 5s 内全局可见
③ 强一致：写入立即可见（代价：查询延迟升高）
```

**工程策略**：

```python
# 方案 A：默认 Bounded —— 大多数 RAG 场景够用
# 写入后等待 5s（或轮询），再查询

# 方案 B：需要"写入即查"（如收藏后立即搜）
collection.insert(data, consistency_level="Strong")   # 本次写入强一致
# 或查询时指定：search(..., consistency_level="Strong")

# 方案 C：批处理场景——flush 后查询
collection.insert(batch)
collection.flush()          # 强制落盘（批处理管道标准动作）
```

> 🎯 **核心要点**：**"写入后搜不到"不是 bug，是一致性设计**——RAG 后台管道（低频写入）用默认 Bounded 即可；交互式"写入即查"场景用 Strong/Session；批处理用 flush。理解三档可见性，少踩"怎么查不到"的坑。

---

## 5. 写入性能验证与数据核对

**写入后的三个验证动作**（管道 CI 必备）：

```python
# ① 数量核对：写入数 == 实体数（防丢数据）
collection.flush()
assert collection.num_entities == expected_count

# ② 抽样核对：取回一条验证字段完整
result = collection.query(expr=f'id == "{sample_id}"', output_fields=["title", "content"])
assert result[0]["title"] == expected_title

# ③ 检索验证：写入数据能被搜到（验证 embedding 正确）
hits = collection.search(data=[sample_vec], anns_field="embedding", limit=1)
assert hits[0][0].id == sample_id
```

**数据质量检查点**（管道侧）：

| 检查 | 手段 | 目的 |
|------|------|------|
| 空内容 chunk | 切分后过滤过短文本 | 防脏数据入库 |
| Embedding 维度 | 与 Schema dim 断言 | 防维度不匹配报错 |
| 重复主键 | upsert 幂等设计 | 防管道重跑重复 |
| 稀疏向量 | BM25 函数自动生成 | 混合检索可用性 |

> 💡 **"写入即验证"是数据管道的铁律**——数量 + 抽样 + 检索三连验证，把问题拦在管道里而不是等用户检索时暴露。

---

## 6. 3.0 在线 Schema 演进

**3.0 的 Schema 变化**（1.x/2.x 的"建后不可改"成为历史）：

```text
2.x：Collection 创建后字段不可增删 → 字段演进 = 重建集合 + 迁移数据
3.0：在线 Schema 演进
  → ALTER Collection ADD COLUMN / DROP COLUMN
  → 运行时执行，无需重建索引、不中断服务
  → 配合 Backfill（字段新增后的历史数据回填）
```

**3.0 的其他 Schema 相关增强**（衔接 01 模块）：

| 能力 | 说明 |
|------|------|
| 可空向量字段 | 六种向量类型均支持 NULL（检索自动跳过，无存储成本） |
| TEXT / BLOB 一等类型 | 原文与二进制直接入库 |
| 全局主键去重 | 跨分片唯一（管道幂等更可靠） |
| 实体级 TTL | TIMESTAMPTZ 字段控制生命周期（合规/数据过期） |
| 自定义词典/同义词 | FileResource 机制（中文与领域检索增强） |

> 🎯 **核心要点**：3.0 的 Schema = "**在线演进 + 更多类型 + 全局去重 + 实体 TTL**"——字段演进不再需要"重建迁移"（2.x 最大运维痛点的解法）。**升级 3.0 的最大收益往往在运维侧**（在线演进 + 快照，见 08 模块）。

---

**下一模块**：[05-向量索引与参数调优](./05-向量索引与参数调优.md) / **返回总览**：[00-Milvus知识体系总览](./00-Milvus知识体系总览.md)
