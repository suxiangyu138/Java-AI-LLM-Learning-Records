# 05 - Embedding 与向量索引

> 🎯 Embedding 决定检索精度，向量数据库决定检索速度 — 选对模型和索引类型，RAG 效果能差 30% 以上

> 📂 Embedding 完整知识见 [Embedding/ 目录](../../01-大模型基础与Prompt工程/Embedding/)

---

## 目录

1. [Embedding 模型选择](#1-embedding-模型选择)
2. [向量数据库集成](#2-向量数据库集成)
3. [索引构建与优化](#3-索引构建与优化)
4. [大规模向量管理](#4-大规模向量管理)

---

## 1. Embedding 模型选择

```text
RAG 场景的 Embedding 选型：

  中文首选：BGE-M3（Dense+Sparse 混合）
  长文本：Qwen-Embedding（32K token）
  快速原型：text-embedding-3-small（OpenAI API）
  高精度：text-embedding-3-large（OpenAI API）
  本地免费：BGE-M3 via Ollama
```

### 查询和文档用同一模型

```text
⚠️ 绝对不要混用！

  入库用 BGE-M3 → 查询必须用 BGE-M3
  入库用 text-embedding-3 → 查询必须用 text-embedding-3

  不同模型的向量空间不同 → 混用 = 检索结果随机
```

---

## 2. 向量数据库集成

### 2.1 Chroma（原型首选）

```python
import chromadb
from chromadb.utils import embedding_functions

# 创建客户端（内存模式）
client = chromadb.Client()

# 创建集合
collection = client.create_collection(
    name="my_docs",
    embedding_function=embedding_functions.OllamaEmbeddingFunction(
        model_name="bge-m3"
    )
)

# 添加文档
collection.add(
    documents=["文档内容1", "文档内容2"],
    ids=["doc1", "doc2"]
)

# 检索
results = collection.query(
    query_texts=["查询内容"],
    n_results=5
)
```

### 2.2 Milvus（生产推荐）

```python
from pymilvus import MilvusClient

client = MilvusClient("milvus_demo.db")  # 嵌入式或连接服务

# 创建集合
client.create_collection(
    collection_name="rag_docs",
    dimension=1024  # BGE-M3 的维度
)

# 插入向量
client.insert(
    collection_name="rag_docs",
    data=[{
        "id": i,
        "vector": embedding,
        "text": text,
        "source": source
    } for i, (embedding, text, source) in enumerate(data)]
)

# 检索
results = client.search(
    collection_name="rag_docs",
    data=[query_vector],
    limit=5,
    output_fields=["text", "source"]
)
```

---

## 3. 索引构建与优化

### FAISS 索引类型选择

| 索引 | 精度 | 速度 | 内存 | 适用规模 |
|------|:---:|:---:|:---:|:---:|
| IndexFlatIP | 100% | 慢 | 高 | <10万 |
| IndexIVFFlat | ~95% | 中 | 中 | 10万-1000万 |
| IndexIVFPQ | ~90% | 快 | 低 | 100万-10亿 |
| IndexHNSW | ~98% | 最快 | 高 | <100万 |

```python
import faiss

dim = 1024
# HNSW — 最常用，速度快精度高
index = faiss.IndexHNSWFlat(dim, 32)  # 32 连接数
index.add(vectors)
```

---

## 4. 大规模向量管理

```text
百万级向量 → 内存不够 → 解决方案：

  ① 量化压缩：IVF+PQ → 内存减少 10-30×
  ② 磁盘索引：DiskANN → 热数据内存 + 冷数据 SSD
  ③ 分布式：Milvus 集群 → 向量分片到多节点
  ④ 过滤索引：先按元数据过滤 → 再在小范围内精确检索
```

---

## 核心要点回顾

- 入库和查询必须用同一个 Embedding 模型
- Chroma 原型、Milvus 生产、FAISS 嵌入式
- HNSW 索引：最快、精度 ~98%、适合 <100万 向量
- 大规模向量用 IVF+PQ 量化压缩
