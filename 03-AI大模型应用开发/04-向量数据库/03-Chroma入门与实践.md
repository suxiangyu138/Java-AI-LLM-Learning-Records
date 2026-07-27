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

## 核心要点回顾

- Chroma = AI 原生嵌入式向量库 → 原型首选
- 5 行代码搭建 RAG：create_collection → add → query
- 持久化模式支持磁盘存储
- 元数据过滤支持复杂查询条件
