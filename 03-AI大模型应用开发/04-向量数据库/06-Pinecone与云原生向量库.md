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

## 核心要点回顾

- Pinecone = 纯向量 SaaS，零运维，适合快速上线
- Qdrant = 强过滤能力，云/自建双模式
- Weaviate = 内置向量化，GraphQL 接口
- 数据敏感选自建，追求效率选云服务
