# 第10步：向量数据库与数据加载

> **阶段目标：** 掌握主流向量数据库的选型和使用，理解大规模数据导入和管理的最佳实践  
> **预计学时：** 1-2周（每天3-4小时）  
> **前置要求：** RAG基础 + Embedding概念  

---

## 📚 目录

- [10.1 为什么需要向量数据库](#101-为什么需要向量数据库)
- [10.2 Chroma：轻量级向量数据库](#102-chroma轻量级向量数据库)
- [10.3 Milvus：生产级向量数据库](#103-milvus生产级向量数据库)
- [10.4 FAISS：高性能向量检索库](#104-faiss高性能向量检索库)
- [10.5 数据导入策略](#105-数据导入策略)
- [10.6 向量数据库选型指南](#106-向量数据库选型指南)
- [10.7 阶段练习](#107-阶段练习)

---

## 10.1 为什么需要向量数据库

### 10.1.1 传统数据库 vs 向量数据库

```
传统数据库（MySQL/PostgreSQL/MongoDB）
  SELECT * FROM products WHERE name = 'iPhone 15'
  精确匹配：找到 → 名称完全等于 "iPhone 15" 的行
  无法处理：找到 → "和iPhone 15相似的手机"

向量数据库（Chroma/Milvus/FAISS）
  SELECT * FROM documents WHERE embedding ~= query_embedding
  语义匹配：找到 → 与查询"最新的苹果手机"语义最相似的文档
```

### 10.1.2 核心概念

```
向量数据库 = 存储 + 索引 + 检索

┌─────────────────────────────────────────────┐
│ Collection (集合)                            │
│  ├── Document 1: [0.1, 0.3, ..., -0.2]     │ ← 向量
│  │   + metadata: {"source": "doc.pdf", ...} │ ← 元数据
│  │   + text: "原始文本内容..."              │ ← 原文
│  ├── Document 2: [0.4, -0.1, ..., 0.7]     │
│  └── Document N: [...]                      │
└─────────────────────────────────────────────┘

关键操作：
├── Insert: 插入向量+元数据
├── Search: 按相似度搜索
├── Delete: 删除向量
├── Update: 更新向量
└── Filter: 按元数据过滤
```

---

## 10.2 Chroma：轻量级向量数据库

### 10.2.1 快速上手

```python
import chromadb
from chromadb.config import Settings
from chromadb.utils import embedding_functions

# ========== 创建客户端 ==========
# 持久化模式（推荐）
client = chromadb.PersistentClient(path="./chroma_db")

# 内存模式（仅用于测试）
# client = chromadb.Client()

# ========== 创建/获取Collection ==========
# 使用内置Embedding函数
openai_ef = embedding_functions.OpenAIEmbeddingFunction(
    api_key="sk-xxx",
    model_name="text-embedding-3-small",
)

# 或使用Sentence Transformers
sentence_ef = embedding_functions.SentenceTransformerEmbeddingFunction(
    model_name="BAAI/bge-large-zh-v1.5",
)

collection = client.get_or_create_collection(
    name="my_knowledge_base",
    embedding_function=sentence_ef,
    metadata={"description": "我的知识库"},
)

# ========== 添加文档 ==========
documents = [
    "Python是一种高级编程语言，由Guido van Rossum于1991年创建。",
    "大语言模型(LLM)是基于Transformer架构的大规模神经网络。",
    "RAG(检索增强生成)是一种结合检索和生成的AI技术。",
    "向量数据库专门用于存储和检索高维向量数据。",
    "FastAPI是一个现代化的Python Web框架，性能出色。",
]

metadatas = [
    {"source": "wiki", "topic": "programming"},
    {"source": "blog", "topic": "ai"},
    {"source": "blog", "topic": "ai"},
    {"source": "wiki", "topic": "database"},
    {"source": "wiki", "topic": "programming"},
]

ids = [f"doc_{i}" for i in range(len(documents))]

collection.add(
    documents=documents,
    metadatas=metadatas,
    ids=ids,
)

print(f"✓ 已添加 {collection.count()} 个文档")
```

### 10.2.2 查询与过滤

```python
# ========== 语义搜索 ==========
results = collection.query(
    query_texts=["什么是AI技术？"],
    n_results=3,
)

print("\n搜索结果：")
for i, (doc_id, doc, distance) in enumerate(zip(
    results['ids'][0],
    results['documents'][0],
    results['distances'][0],
)):
    print(f"  [{i+1}] {doc_id}: {doc[:50]}... (距离: {distance:.4f})")

# ========== 带元数据过滤的搜索 ==========
results = collection.query(
    query_texts=["编程相关的内容"],
    n_results=3,
    where={"source": "wiki"},  # 只搜wiki来源
)

# 复杂过滤
results = collection.query(
    query_texts=["数据库技术"],
    n_results=3,
    where={
        "$and": [
            {"topic": {"$in": ["database", "programming"]}},
        ]
    },
)

# ========== 获取文档 ==========
doc = collection.get(ids=["doc_0"])
print(f"\n文档 doc_0: {doc['documents']}")

# ========== 更新文档 ==========
collection.update(
    ids=["doc_0"],
    documents=["Python是一种高级编程语言，由Guido van Rossum于1991年创建。被广泛用于AI开发。"],
)

# ========== 删除文档 ==========
# collection.delete(ids=["doc_0"])
# collection.delete(where={"source": "test"})
```

### 10.2.3 Chroma 完整集成

```python
class ChromaKnowledgeBase:
    """基于Chroma的知识库管理"""
    
    def __init__(self, persist_dir: str = "./chroma_db",
                 embedding_model: str = "BAAI/bge-large-zh-v1.5"):
        self.client = chromadb.PersistentClient(path=persist_dir)
        self.ef = embedding_functions.SentenceTransformerEmbeddingFunction(
            model_name=embedding_model
        )
        self.collections = {}
    
    def create_kb(self, name: str, description: str = "") -> chromadb.Collection:
        """创建知识库"""
        collection = self.client.get_or_create_collection(
            name=name,
            embedding_function=self.ef,
            metadata={"description": description, "created_at": str(time.time())},
        )
        self.collections[name] = collection
        return collection
    
    def add_documents(self, kb_name: str, documents: List[str],
                      metadatas: List[dict] = None,
                      ids: List[str] = None,
                      batch_size: int = 100) -> int:
        """批量添加文档"""
        collection = self.client.get_collection(kb_name)
        
        if ids is None:
            base_id = f"{kb_name}_{int(time.time())}"
            ids = [f"{base_id}_{i}" for i in range(len(documents))]
        
        if metadatas is None:
            metadatas = [{}] * len(documents)
        
        total_added = 0
        # 分批添加（处理大量数据时）
        for i in range(0, len(documents), batch_size):
            batch_docs = documents[i:i+batch_size]
            batch_meta = metadatas[i:i+batch_size]
            batch_ids = ids[i:i+batch_size]
            
            collection.add(
                documents=batch_docs,
                metadatas=batch_meta,
                ids=batch_ids,
            )
            total_added += len(batch_docs)
        
        return total_added
    
    def search(self, kb_name: str, query: str, top_k: int = 5,
               where: dict = None) -> List[dict]:
        """搜索知识库"""
        collection = self.client.get_collection(kb_name)
        
        results = collection.query(
            query_texts=[query],
            n_results=top_k,
            where=where,
        )
        
        formatted = []
        for i in range(len(results['ids'][0])):
            formatted.append({
                "id": results['ids'][0][i],
                "content": results['documents'][0][i],
                "metadata": results['metadatas'][0][i],
                "score": 1 - results['distances'][0][i],  # 距离转相似度
            })
        
        return formatted
    
    def list_knowledge_bases(self) -> List[dict]:
        """列出所有知识库"""
        kbs = []
        for col in self.client.list_collections():
            kbs.append({
                "name": col.name,
                "count": col.count(),
                "metadata": col.metadata,
            })
        return kbs
```

---

## 10.3 Milvus：生产级向量数据库

### 10.3.1 架构概览

```
Milvus 架构（分布式）

┌────────────────────────────────────────────┐
│              SDK / API 层                   │
│       pymilvus / RESTful API               │
├────────────────────────────────────────────┤
│              Coordinator (协调器)            │
│    Root Coord │ Query Coord │ Data Coord   │
├──────────────┼─────────────┼──────────────┤
│   Meta Store │  Query Node │  Data Node   │
│   (etcd)     │  (检索)     │  (存储)      │
├──────────────┴─────────────┴──────────────┤
│         Object Storage (MinIO/S3)          │
│         Message Queue (Pulsar/Kafka)       │
└────────────────────────────────────────────┘

Milvus vs Chroma:
┌──────────┬────────────┬─────────────┐
│          │   Chroma   │   Milvus    │
├──────────┼────────────┼─────────────┤
│ 规模     │ 百万级     │ 十亿级      │
│ 分布式   │ ❌         │ ✅          │
│ 安装     │ pip install │ Docker集群  │
│ 适用     │ 原型/小项目 │ 生产环境    │
│ 性能     │ 中         │ 高          │
│ 运维难度 │ 低         │ 高          │
└──────────┴────────────┴─────────────┘
```

### 10.3.2 Milvus实战

```python
from pymilvus import (
    connections, Collection, CollectionSchema,
    FieldSchema, DataType, utility,
)

# ========== 连接到Milvus ==========
connections.connect(
    alias="default",
    host='localhost',
    port='19530',
)

# ========== 定义Schema ==========
# Milvus需要显式定义Schema（比Chroma更严格，但性能更好）

doc_id = FieldSchema(
    name="id", dtype=DataType.INT64,
    is_primary=True, auto_id=True,
)
doc_text = FieldSchema(
    name="text", dtype=DataType.VARCHAR,
    max_length=65535,
)
doc_vector = FieldSchema(
    name="embedding", dtype=DataType.FLOAT_VECTOR,
    dim=1024,  # 向量维度（必须和Embedding模型一致）
)
doc_source = FieldSchema(
    name="source", dtype=DataType.VARCHAR,
    max_length=256,
)
doc_timestamp = FieldSchema(
    name="created_at", dtype=DataType.INT64,
)

schema = CollectionSchema(
    fields=[doc_id, doc_text, doc_vector, doc_source, doc_timestamp],
    description="文档知识库",
    enable_dynamic_field=True,  # 允许额外字段
)

# ========== 创建Collection ==========
collection_name = "enterprise_knowledge"
if utility.has_collection(collection_name):
    utility.drop_collection(collection_name)

collection = Collection(name=collection_name, schema=schema)

# ========== 创建索引 ==========
index_params = {
    "metric_type": "COSINE",   # 余弦相似度
    "index_type": "IVF_FLAT",  # 索引类型
    "params": {"nlist": 1024},
}

collection.create_index(
    field_name="embedding",
    index_params=index_params,
)

print(f"✓ Collection '{collection_name}' 创建完成")

# ========== 插入数据 ==========
import numpy as np
from sentence_transformers import SentenceTransformer

encoder = SentenceTransformer("BAAI/bge-large-zh-v1.5")

documents = [
    "Milvus是一个开源的向量数据库...",
    "索引类型包括IVF_FLAT、IVF_SQ8、HNSW等...",
    "相似度度量支持欧氏距离、余弦相似度...",
]
embeddings = encoder.encode(documents)

entities = [
    documents,                                    # text字段
    embeddings.tolist(),                          # embedding字段
    ["manual"] * len(documents),                  # source字段
    [int(time.time())] * len(documents),          # created_at字段
]

collection.insert(entities)
collection.flush()  # 持久化

print(f"✓ 已插入 {collection.num_entities} 条数据")

# ========== 搜索 ==========
collection.load()  # 加载到内存

query_embedding = encoder.encode(["什么是向量数据库？"])[0]

search_params = {
    "metric_type": "COSINE",
    "params": {"nprobe": 16},  # 搜索的聚类数
}

results = collection.search(
    data=[query_embedding.tolist()],
    anns_field="embedding",
    param=search_params,
    limit=3,
    output_fields=["text", "source"],  # 返回的字段
)

print("\n搜索结果:")
for hits in results:
    for hit in hits:
        print(f"  ID: {hit.id}, 距离: {hit.distance:.4f}")
        print(f"  文本: {hit.entity.get('text')[:80]}...")
```

### 10.3.3 Milvus索引策略

```python
"""
Milvus索引类型选择：

1. IVF_FLAT
   - 基于聚类的倒排索引
   - 精度高，速度中等
   - 适合：百万级数据
   - 参数：nlist (聚类数, 推荐4×sqrt(N))

2. IVF_SQ8 / IVF_PQ
   - 向量量化压缩
   - 内存占用小，精度略低
   - 适合：内存受限场景
   - 压缩比：SQ8=4x, PQ=8-16x

3. HNSW (Hierarchical Navigable Small World)
   - 图索引，速度最快
   - 内存占用大
   - 适合：追求极致查询速度
   - 参数：M(连接数), efConstruction(构建质量)

4. IVF_HNSW
   - IVF+HNSW混合
   - 平衡方案

选择决策：
┌──────────────┬──────────┬──────────┬──────────┐
│   场景        │  推荐索引 │  精度    │  速度    │
├──────────────┼──────────┼──────────┼──────────┤
│ 原型开发      │ IVF_FLAT │ ★★★★★   │ ★★★     │
│ 内存受限      │ IVF_SQ8  │ ★★★★    │ ★★★     │
│ 极致性能      │ HNSW     │ ★★★★★   │ ★★★★★   │
│ 十亿级       │ IVF_PQ   │ ★★★     │ ★★★★    │
└──────────────┴──────────┴──────────┴──────────┘
"""
```

---

## 10.4 FAISS：高性能向量检索库

### 10.4.1 FAISS特点

```python
"""
FAISS (Facebook AI Similarity Search)

Chroma/Milvus = 数据库 (存储+管理+检索)
FAISS = 检索引擎 (只做检索，不管存储)

使用场景：
- 不需要持久化（内存检索）
- 对检索速度有极致要求
- 嵌入式/离线场景
- 作为Chroma/Milvus的底层引擎
"""

import faiss
import numpy as np

# ========== 创建FAISS索引 ==========
dim = 768  # Embedding维度
nlist = 100  # 聚类数

# 量化器
quantizer = faiss.IndexFlatIP(dim)  # 内积 = 余弦相似度(归一化后)

# IVF索引（先聚类再搜索）
index = faiss.IndexIVFFlat(quantizer, dim, nlist, faiss.METRIC_INNER_PRODUCT)

# ========== 训练（IVF需要训练）==========
# 生成训练数据
train_vectors = np.random.randn(10000, dim).astype('float32')
# 归一化（使得内积=余弦相似度）
faiss.normalize_L2(train_vectors)

index.train(train_vectors)

# ========== 添加向量 ==========
data_vectors = np.random.randn(5000, dim).astype('float32')
faiss.normalize_L2(data_vectors)

index.add(data_vectors)
index.nprobe = 10  # 搜索时探测的聚类数

print(f"✓ FAISS索引包含 {index.ntotal} 个向量")

# ========== 搜索 ==========
query = np.random.randn(1, dim).astype('float32')
faiss.normalize_L2(query)

distances, indices = index.search(query, k=5)

print(f"Top-5 索引: {indices[0]}")
print(f"Top-5 距离: {distances[0]}")

# ========== 保存/加载 ==========
# faiss.write_index(index, "my_index.faiss")
# index = faiss.read_index("my_index.faiss")
```

---

## 10.5 数据导入策略

### 10.5.1 大规模数据导入

```python
class BulkDataLoader:
    """
    大规模数据导入器
    
    处理：
    - CSV/JSON文件（GB级别）
    - 数据库同步
    - API数据拉取
    - 增量更新
    """
    
    def __init__(self, vector_store, embedding_model, batch_size: int = 100):
        self.store = vector_store
        self.encoder = embedding_model
        self.batch_size = batch_size
    
    def load_from_csv(self, file_path: str, text_column: str,
                      metadata_columns: List[str] = None):
        """从CSV批量导入"""
        import pandas as pd
        
        total = 0
        # 分块读取（不一次性加载到内存）
        for chunk in pd.read_csv(file_path, chunksize=self.batch_size):
            documents = chunk[text_column].tolist()
            
            # 提取元数据
            metadatas = []
            if metadata_columns:
                for _, row in chunk.iterrows():
                    meta = {col: row[col] for col in metadata_columns}
                    metadatas.append(meta)
            else:
                metadatas = [{}] * len(documents)
            
            # 批量编码和写入
            embeddings = self.encoder.encode(documents)
            self.store.add_documents(documents, embeddings, metadatas)
            
            total += len(documents)
            print(f"已导入 {total} 条记录...")
        
        return total
    
    def load_from_database(self, connection_string: str, query: str):
        """从关系数据库同步"""
        import sqlalchemy
        
        engine = sqlalchemy.create_engine(connection_string)
        
        # 流式读取
        with engine.connect() as conn:
            result = conn.execution_options(stream_results=True).execute(
                sqlalchemy.text(query)
            )
            
            batch = []
            for row in result:
                batch.append(dict(row))
                
                if len(batch) >= self.batch_size:
                    self._process_batch(batch)
                    batch = []
            
            # 处理剩余
            if batch:
                self._process_batch(batch)
```

### 10.5.2 增量更新策略

```python
class IncrementalUpdater:
    """
    增量更新管理器
    
    策略：
    1. 基于时间戳：只更新最近修改的文档
    2. 基于Hash：只更新内容变化的文档
    3. 基于版本号：管理文档版本
    """
    
    def __init__(self, vector_store):
        self.store = vector_store
        self.update_log = []
    
    def sync_documents(self, documents: List[dict]):
        """
        增量同步文档
        
        documents: [{"id": ..., "text": ..., "metadata": {...}, "updated_at": ...}, ...]
        """
        new_docs = []
        updated_docs = []
        deleted_ids = []
        
        for doc in documents:
            existing = self.store.get_by_id(doc["id"])
            
            if existing is None:
                new_docs.append(doc)
            elif existing["hash"] != doc.get("hash", hash(doc["text"])):
                updated_docs.append(doc)
        
        # 处理新增
        if new_docs:
            self.store.add_documents(new_docs)
            print(f"✓ 新增 {len(new_docs)} 个文档")
        
        # 处理更新（先删后加）
        if updated_docs:
            self.store.delete(ids=[d["id"] for d in updated_docs])
            self.store.add_documents(updated_docs)
            print(f"✓ 更新 {len(updated_docs)} 个文档")
        
        self.update_log.append({
            "timestamp": time.time(),
            "new": len(new_docs),
            "updated": len(updated_docs),
        })
```

---

## 10.6 向量数据库选型指南

```python
"""
选型决策树：

1. 数据量 < 10万？
   └→ Yes: Chroma (最简单，零配置)
   
2. 数据量 10万 - 100万，需要持久化？
   └→ Yes: Chroma/PGVector (单机够用)
   
3. 数据量 > 100万，需要高可用？
   └→ Yes: Milvus/Qdrant/Weaviate (分布式)
   
4. 极致性能，不需要持久化？
   └→ Yes: FAISS (纯检索引擎)
   
5. 已有PostgreSQL？
   └→ Yes: PGVector (复用现有基础设施)

6. 云原生？
   └→ Yes: Pinecone/Zilliz Cloud (免运维)

┌──────────────┬──────────┬──────────┬───────────┬──────────┐
│              │  Chroma  │  Milvus  │  FAISS    │  PGVector│
├──────────────┼──────────┼──────────┼───────────┼──────────┤
│ 安装复杂度    │ ⭐       │ ⭐⭐⭐⭐  │ ⭐        │ ⭐⭐     │
│ 扩展性       │ ⭐⭐     │ ⭐⭐⭐⭐⭐ │ ⭐⭐      │ ⭐⭐⭐   │
│ 功能丰富度    │ ⭐⭐⭐   │ ⭐⭐⭐⭐  │ ⭐        │ ⭐⭐     │
│ 社区活跃度    │ ⭐⭐⭐⭐ │ ⭐⭐⭐⭐⭐ │ ⭐⭐⭐⭐  │ ⭐⭐⭐   │
│ 学习成本      │ ⭐       │ ⭐⭐⭐   │ ⭐⭐      │ ⭐       │
│ 适用阶段      │ 开发/测试│ 生产环境  │ 研究/原型 │ 已有PG  │
└──────────────┴──────────┴──────────┴───────────┴──────────┘
"""
```

---

## 10.7 阶段练习

### 练习1：Chroma知识库搭建
用Chroma搭建一个个人知识库，导入至少50篇文档，测试搜索效果。

### 练习2：向量数据库性能对比
在相同数据集上对比Chroma和FAISS的插入和查询性能。

### 练习3：增量同步Pipeline
实现一个自动检测文件变化并更新向量库的Pipeline。

---

> **✅ 阶段完成检查清单：**
> - [ ] 理解向量数据库和传统数据库的本质区别
> - [ ] 能用Chroma搭建完整的知识库
> - [ ] 了解Milvus的Schema定义和索引创建
> - [ ] 掌握FAISS的基础使用
> - [ ] 能根据数据规模选择合适的向量数据库
> - [ ] 完成3个阶段练习
>
> **下一步：** [第11步：AI全栈开发基础](../11-AI全栈开发基础/README.md)
