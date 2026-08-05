# 06 - Pinecone 与云原生向量库

> 🎯 不想运维向量数据库？Pinecone、Qdrant Cloud、Weaviate Cloud — 云原生方案让你专注业务而非基础设施

---

## 目录

1. [云原生向量库对比](#1-云原生向量库对比)
2. [Pinecone](#2-pinecone)
3. [Qdrant](#3-qdrant)
4. [Weaviate](#4-weaviate)
5. [云 vs 自建抉择](#5-云-vs-自建抉择)

---

## 1. 云原生向量库对比

| 维度 | Pinecone | Qdrant | Weaviate | Zilliz Cloud |
|------|----------|--------|----------|-------------|
| **定位** | 纯向量检索 SaaS | 向量库+过滤 | 向量库+AI集成 | Milvus 托管版 |
| **部署** | 仅云 | 云/自建 | 云/自建 | 仅云 |
| **过滤** | 元数据过滤 | 强过滤 | GraphQL过滤 | 标量过滤 |
| **多模态** | ❌ | ❌ | ✅ 内置向量化 | ❌ |
| **免费层** | ✅ | ✅ | ✅ | ✅ |
| **向量维度** | 最高 20000 | 无限制 | 无限制 | 无限制 |

---

## 2. Pinecone

```python
from pinecone import Pinecone

pc = Pinecone(api_key="xxx")

# 创建索引
pc.create_index(
    name="rag-docs",
    dimension=1024,
    metric="cosine",
    spec={"serverless": {"cloud": "aws", "region": "us-east-1"}}
)

# 连接索引
index = pc.Index("rag-docs")

# 插入向量
index.upsert(vectors=[
    {"id": "doc1", "values": embedding1,
     "metadata": {"source": "handbook.pdf"}}
])

# 检索
results = index.query(
    vector=query_embedding,
    top_k=5,
    filter={"source": "handbook.pdf"},
    include_metadata=True
)
```

---

## 3. Qdrant

```python
from qdrant_client import QdrantClient
from qdrant_client.models import Distance, VectorParams

client = QdrantClient(host="localhost", port=6333)

# 创建 Collection
client.create_collection(
    collection_name="docs",
    vectors_config=VectorParams(size=1024, distance=Distance.COSINE)
)

# 插入
client.upsert(
    collection_name="docs",
    points=[{
        "id": 1,
        "vector": embedding,
        "payload": {"text": "doc content", "source": "file.pdf"}
    }]
)

# 带过滤检索
client.search(
    collection_name="docs",
    query_vector=query_vec,
    query_filter={"must": [{"key": "source", "match": {"value": "file.pdf"}}]},
    limit=5
)
```

---

## 4. Weaviate

```python
import weaviate

client = weaviate.Client("http://localhost:8080")

# 创建 Class（带内置向量化）
client.schema.create_class({
    "class": "Document",
    "vectorizer": "text2vec-transformers",  # 自动向量化！
    "properties": [
        {"name": "text", "dataType": ["text"]},
        {"name": "source", "dataType": ["string"]}
    ]
})

# 插入（自动向量化）
client.data_object.create(
    data_object={"text": "doc content"},
    class_name="Document"
)

# 语义检索（无需手动 Embedding！）
response = client.query.get("Document", ["text"]) \
    .with_near_text({"concepts": ["search query"]}) \
    .with_limit(5).do()
```

---

## 5. 云 vs 自建抉择

| 维度 | 云服务 (Pinecone) | 自建 (Milvus) |
|------|:---:|:---:|
| **运维成本** | 零 | 需专人 |
| **数据隐私** | ❌ 数据上传 | ✅ 完全本地 |
| **扩展性** | 自动 | 手动 |
| **成本** | 按量付费 | 硬件+人力 |
| **延迟** | 受网络 | 本地极低 |

```text
推荐策略：
  → 创业/MVP → Pinecone（零运维快速上线）
  → 数据敏感 → 自建 Milvus/Qdrant
  → 混合 → 开发用 Pinecone，生产切自建
```

---

## 8. 云向量库选型 2026

**Pinecone 的 2026 定位与竞争**：

```text
Pinecone：全托管 Serverless 向量库（最早纯托管玩家）
  优势：零运维、快速验证、Serverless 弹性
  劣势：厂商锁定、规模化成本高、自托管场景用不了

2026 云/托管方案对比：
  Pinecone —— 全托管首选（快速上线，接受锁定）
  Zilliz Cloud —— 开源 Milvus 的托管版（与自建同代码）
  Qdrant Cloud —— 自托管同源（可迁移）
  pgvector on RDS —— 已有 Postgres 的云托管（零新增）
  Elastic Cloud —— 搜索 + 向量一体（已有 ES 团队）
```

**托管 vs 自建的决策框架（2026）**：

```text
托管（Pinecone/Zilliz Cloud）：
  收益：零运维、弹性、SLA
  代价：成本（规模化后高）、锁定、数据出域（合规）
自建（Milvus/Qdrant on K8s）：
  收益：成本可控、数据主权、灵活
  代价：运维团队（组件多）、升级维护

决策：合规要求/成本敏感 → 自建；快速上线/无运维 → 托管
```

> 🎯 **核心要点**：云向量库 2026 = "**托管（省运维）vs 自建（省成本/主权）**"的经典权衡——**Pinecone 是托管标杆、Zilliz Cloud 是开源同源、pgvector 是零新增**；选型先回答合规与预算。

---

## 9. 云向量库对比速查

**Pinecone / Qdrant / Weaviate / Zilliz 速查**：

| 维度 | Pinecone | Qdrant | Weaviate | Zilliz Cloud |
|------|:--------:|:------:|:--------:|:------------:|
| 部署 | 全托管 | 自托管/云 | 自托管/云 | 托管（Milvus 同源） |
| 语言 | — | Rust | Go | Go（Milvus） |
| 混合检索 | 支持 | **原生最强** | **最成熟（BM25+RRF）** | 支持 |
| 过滤 | 支持 | **原生过滤索引** | 支持 | 支持 |
| 开源 | ❌ | ✅ | ✅ | ✅（Milvus） |
| 规模 | Serverless | 千万-亿 | 千万级 | 亿级 |
| 定位 | 快速上线 | 性价比之王 | 一体化 | 开源同源托管 |

**选型速查（2026）**：

```text
快速验证/无运维 → Pinecone（接受锁定与规模化成本）
自托管性能/过滤 → Qdrant（Rust 高效 + 原生过滤）
混合检索一体化 → Weaviate（嵌入+检索+生成）
开源同源托管 → Zilliz Cloud（与自建 Milvus 无缝迁移）
已有 Postgres → pgvector（连云库都不用加）
```

> 🎯 **核心要点**：云向量库选型 = "**托管（省运维）/自建（省成本）+ 混合检索（Weaviate 强）+ 过滤（Qdrant 强）+ 同源（Zilliz）**"四维速查——**"开源同源托管"（Zilliz）是"既要托管又要自主"的折中答案**。

---

## 核心要点回顾

- Pinecone = 纯向量 SaaS，零运维，适合快速上线
- Qdrant = 强过滤能力，云/自建双模式
- Weaviate = 内置向量化，GraphQL 接口
- 数据敏感选自建，追求效率选云服务
