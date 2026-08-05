# 03 - Chroma 入门与实践

> 🎯 Chroma = AI 原生的嵌入式向量数据库 — 5 行代码搭建 RAG 原型，Python 进程内运行，零配置部署

---

## 目录

1. [Chroma 简介](#1-chroma-简介)
2. [快速上手](#2-快速上手)
3. [Collection 管理](#3-collection-管理)
4. [元数据过滤](#4-元数据过滤)
5. [LangChain 集成](#5-langchain-集成)

---

## 1. Chroma 简介

```text
Chroma = 开源的 AI 原生向量数据库

定位：嵌入式向量库 → 类似 SQLite 之于关系数据库

优势：
  ✅ pip install 即用，零配置
  ✅ Python 原生，无需独立服务
  ✅ 内置 Embedding 函数
  ✅ LangChain/LlamaIndex 官方支持

劣势：
  ⚠️ 单机，不支持分布式
  ⚠️ 百万级向量性能下降
```

---

## 2. 快速上手

```python
import chromadb

# 创建客户端
client = chromadb.Client()  # 内存模式
# client = chromadb.PersistentClient(path="./chroma_db")  # 持久化

# 创建 Collection
collection = client.create_collection(name="my_docs")

# 添加文档（Chroma 自动生成 Embedding！）
collection.add(
    documents=["Spring Boot 自动配置原理", "MySQL 索引优化实践"],
    metadatas=[{"source": "spring.md"}, {"source": "mysql.md"}],
    ids=["doc1", "doc2"]
)

# 检索
results = collection.query(
    query_texts=["怎么配置 Spring Boot？"],
    n_results=3
)
print(results["documents"][0])
```

### 使用指定 Embedding 函数

```python
import chromadb.utils.embedding_functions as ef

# OpenAI Embedding
openai_ef = ef.OpenAIEmbeddingFunction(
    api_key="sk-xxx",
    model_name="text-embedding-3-small"
)

# Ollama 本地
ollama_ef = ef.OllamaEmbeddingFunction(
    model_name="bge-m3"
)

collection = client.create_collection(
    name="docs",
    embedding_function=ollama_ef
)
```

---

## 3. Collection 管理

```python
# 创建带自定义距离函数
collection = client.create_collection(
    name="docs",
    metadata={"hnsw:space": "cosine"}  # cosine / l2 / ip
)

# 查询所有
collections = client.list_collections()

# 获取/删除
collection = client.get_collection("docs")
client.delete_collection("docs")

# 统计
print(f"文档数: {collection.count()}")
```

---

## 4. 元数据过滤

```python
# 插入时带元数据
collection.add(
    documents=["doc1", "doc2", "doc3"],
    metadatas=[
        {"type": "java", "date": "2026-01"},
        {"type": "python", "date": "2026-03"},
        {"type": "java", "date": "2026-06"}
    ],
    ids=["1", "2", "3"]
)

# 按元数据过滤检索
results = collection.query(
    query_texts=["how to config"],
    where={"type": "java"},           # 只搜 Java 文档
    where_document={"$contains": "config"}  # 文本包含
)
```

---

## 5. LangChain 集成

```python
from langchain_community.vectorstores import Chroma
from langchain_community.embeddings import OllamaEmbeddings

embeddings = OllamaEmbeddings(model="bge-m3")

# 从文档创建
vectorstore = Chroma.from_documents(
    documents=docs,
    embedding=embeddings,
    persist_directory="./chroma_db"
)

# 检索
retriever = vectorstore.as_retriever(
    search_type="similarity",  # similarity / mmr
    search_kwargs={"k": 5}
)

results = retriever.get_relevant_documents("查询内容")
```

---

## 8. Chroma 的定位与 2026 替代方案

**Chroma 的边界（2026 再确认）**：

```text
Chroma 擅长：开发原型、教学、小规模（<100 万向量）本地应用
Chroma 不适合：生产级多用户/高并发/大规模 → 生产请用 Milvus/Qdrant

2026 替代方案矩阵：
  原型/教学 → Chroma / FAISS（最简）
  已有 Postgres → pgvector（零新增基础设施）
  中小生产（自托管）→ Qdrant（性价比最优）
  混合检索刚需 → Weaviate（原生 BM25+向量 RRF）
  亿级/高 QPS → Milvus（云原生）
  全托管 → Pinecone（Serverless）
```

**从 Chroma 迁移到生产的路径**：

```text
① 先 Chroma 跑通原型（Embedding/检索逻辑验证）
② 评估数据规模与并发（决定目标库）
③ 迁移：重新入库（切分/Embedding 复用）+ 换 SDK
   → Chroma 的抽象（Collection/Query）与主流库对齐，迁移成本低
④ 注意：Embedding 维度与索引参数保持一致（重入库成本高）
```

> 🎯 **核心要点**：Chroma 的价值 = "**原型验证器**"（快、零配置）——2026 生产选型时它是"起点"不是"终点"；**"原型 Chroma、生产换库"是标准路径**（换库成本主要在重入库，提前设计好 Schema 可降）。

---

## 9. Chroma 常用操作速查

**Chroma API 速查**（开发高频操作）：

```python
import chromadb

client = chromadb.PersistentClient(path="./chroma")   # 持久化（非临时）

# Collection 操作
col = client.get_or_create_collection("kb", metadata={"hnsw:space": "cosine"})
col.upsert(ids=["1"], documents=["文档一"], metadatas=[{"source": "doc1"}])

# 检索
result = col.query(query_texts=["问题"], n_results=5, where={"source": "doc1"})

# 更新与删除
col.update(ids=["1"], documents=["新内容"])
col.delete(ids=["1"])

# 工具：查看数据
client.list_collections()        # 列出集合
col.count()                      # 实体数
```

**LangChain 集成速查**：

```python
from langchain_chroma import Chroma
from langchain_openai import OpenAIEmbeddings

vector_store = Chroma(
    collection_name="kb",
    embedding_function=OpenAIEmbeddings(model="text-embedding-3-small"),
    persist_directory="./chroma"
)
docs = vector_store.similarity_search(question, k=5)      # 检索
vector_store.add_documents(docs)                           # 入库
```

> 💡 开发期三件套：`PersistentClient`（数据不丢）+ `upsert`（幂等更新）+ `where` 过滤（元数据检索）——覆盖 90% 原型需求。

---

## 核心要点回顾

- Chroma = AI 原生嵌入式向量库 → 原型首选
- 5 行代码搭建 RAG：create_collection → add → query
- 持久化模式支持磁盘存储
- 元数据过滤支持复杂查询条件
