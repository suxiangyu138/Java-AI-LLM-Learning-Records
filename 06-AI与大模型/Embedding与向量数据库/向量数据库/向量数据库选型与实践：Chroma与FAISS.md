# 向量数据库选型与实践：Chroma 与 FAISS

> **所属阶段**：阶段二 — RAG 知识库应用开发
> **前置知识**：Embedding 向量化原理
> **核心问题**：百万级向量如何高效存储和检索？

---

## 1. 为什么需要向量数据库

传统数据库（MySQL、ES）基于**精确匹配**或**关键词匹配**，无法处理语义搜索：

```
用户问："怎么优化慢查询" 
MySQL 只能匹配包含"慢查询"的记录
向量数据库能找到："SQL性能调优" "索引优化方案"（语义相近）
```

### 向量数据库核心能力
- 高维向量存储
- 近似最近邻搜索（ANN，Approximate Nearest Neighbor）
- 元数据过滤

---

## 2. 向量数据库对比

| 特性 | Chroma | FAISS | Milvus |
|------|--------|-------|--------|
| 定位 | 轻量级，适合原型 | 高效检索库 | 分布式，适合生产 |
| 部署 | `pip install` | `pip install` | Docker / K8s |
| 持久化 | 内置 | 需自行实现 | 内置 |
| 元数据过滤 | 支持 | 不支持（需自行实现） | 支持 |
| 分布式 | 不支持 | 不支持 | 支持 |
| 适用场景 | 原型开发、小规模 | 大规模检索、研究 | 企业级生产 |

---

## 3. Chroma 实战

### 3.1 安装

```bash
pip install chromadb
```

### 3.2 基础操作

```python
import chromadb
from chromadb.utils import embedding_functions

# 初始化客户端
client = chromadb.PersistentClient(path="./chroma_db")

# 使用 Ollama Embedding 函数
ef = embedding_functions.OllamaEmbeddingFunction(
    model_name="bge-m3",
    url="http://localhost:11434/api/embeddings"
)

# 创建集合
collection = client.get_or_create_collection(
    name="java_docs",
    embedding_function=ef,
    metadata={"description": "Java 技术文档"}
)

# 添加文档
documents = [
    "Spring IoC 容器负责管理 Bean 的生命周期和依赖关系",
    "MySQL InnoDB 引擎支持事务和行级锁",
    "Redis 是基于内存的键值存储，常用于缓存",
]
collection.add(
    documents=documents,
    ids=["doc_1", "doc_2", "doc_3"],
    metadatas=[
        {"topic": "Spring", "level": "intermediate"},
        {"topic": "MySQL", "level": "advanced"},
        {"topic": "Redis", "level": "beginner"}
    ]
)

# 语义搜索
results = collection.query(
    query_texts=["怎么管理对象依赖"],
    n_results=2,
    where={"topic": "Spring"}  # 元数据过滤
)
print(results["documents"])   # [["Spring IoC..."]]
print(results["distances"])   # [[0.123]]  — 距离越小越相关
```

### 3.3 完整 RAG 检索管道

```python
class ChromaRetriever:
    """基于 Chroma 的文档检索器"""

    def __init__(self, collection_name, persist_dir="./chroma_db"):
        self.client = chromadb.PersistentClient(path=persist_dir)
        self.ef = embedding_functions.OllamaEmbeddingFunction(
            model_name="bge-m3"
        )
        self.collection = self.client.get_or_create_collection(
            name=collection_name,
            embedding_function=self.ef
        )

    def index_documents(self, documents, metadatas=None):
        """批量索引文档"""
        ids = [f"doc_{i}" for i in range(len(documents))]
        self.collection.add(
            documents=documents,
            ids=ids,
            metadatas=metadatas
        )

    def search(self, query, top_k=5, filter_meta=None):
        """语义检索"""
        kwargs = {"query_texts": [query], "n_results": top_k}
        if filter_meta:
            kwargs["where"] = filter_meta
        results = self.collection.query(**kwargs)
        return list(zip(
            results["documents"][0],
            results["distances"][0]
        ))
```

---

## 4. FAISS 实战

### 4.1 安装

```bash
pip install faiss-cpu    # CPU 版
# 或
pip install faiss-gpu    # GPU 版
```

### 4.2 基础操作

```python
import faiss
import numpy as np

# 准备数据
dimension = 768  # 向量维度
index = faiss.IndexFlatIP(dimension)  # 内积索引（等价余弦相似度）

# 添加向量
vectors = np.random.random((1000, dimension)).astype("float32")
faiss.normalize_L2(vectors)  # 归一化以便用内积做余弦相似度
index.add(vectors)

# 检索
query = np.random.random((1, dimension)).astype("float32")
faiss.normalize_L2(query)
distances, indices = index.search(query, k=5)
print(f"Top-5 indices: {indices}")
print(f"Similarities: {distances}")
```

### 4.3 索引类型选择

```python
# 精确搜索 — 小规模 (<10万)
index = faiss.IndexFlatL2(dimension)

# IVF 倒排索引 — 中规模 (10万-1000万)
quantizer = faiss.IndexFlatL2(dimension)
index = faiss.IndexIVFFlat(quantizer, dimension, nlist=100)
index.train(vectors)  # 需要训练
index.add(vectors)

# HNSW 图索引 — 高精度需求
index = faiss.IndexHNSWFlat(dimension, M=32)

# PQ 乘积量化 — 内存受限
index = faiss.IndexPQ(dimension, m=8, nbits=8)
index.train(vectors)
index.add(vectors)
```

### 4.4 封装为检索器

```python
class FAISSRetriever:
    """基于 FAISS 的高性能检索器"""

    def __init__(self, dimension, index_type="flat"):
        self.dimension = dimension
        self.index_type = index_type
        self.index = self._create_index()
        self.texts = []          # id → text 映射
        self.is_trained = False

    def _create_index(self):
        if self.index_type == "flat":
            return faiss.IndexFlatIP(self.dimension)
        elif self.index_type == "ivf":
            quantizer = faiss.IndexFlatIP(self.dimension)
            return faiss.IndexIVFFlat(quantizer, self.dimension, 100)
        raise ValueError(f"Unknown index: {self.index_type}")

    def add(self, texts, vectors):
        vecs = np.array(vectors).astype("float32")
        faiss.normalize_L2(vecs)
        if self.index_type == "ivf" and not self.is_trained:
            self.index.train(vecs)
            self.is_trained = True
        self.index.add(vecs)
        self.texts.extend(texts)

    def search(self, query_vector, top_k=5):
        q = np.array([query_vector]).astype("float32")
        faiss.normalize_L2(q)
        distances, indices = self.index.search(q, top_k)
        results = []
        for i, d in zip(indices[0], distances[0]):
            if i >= 0 and i < len(self.texts):
                results.append((self.texts[i], float(d)))
        return results

    def save(self, path):
        faiss.write_index(self.index, f"{path}/index.faiss")
        with open(f"{path}/texts.json", "w") as f:
            json.dump(self.texts, f, ensure_ascii=False)
```

---

## 5. 选型决策树

```
需要元数据过滤？
├── 是 → Chroma（原型）/ Milvus（生产）
└── 否 → 数据量？
    ├── < 10万 → Chroma（最简单）
    ├── 10万-100万 → FAISS（最快）
    └── > 100万 → Milvus（分布式）
```

---

## 6. 性能优化要点

| 优化项 | 方法 | 效果 |
|--------|------|------|
| 降维 | PCA 降维到 256/512 | 内存减半，速度提升 |
| 量化 | PQ 乘积量化 | 内存降低 10-30 倍 |
| 索引 | HNSW / IVF | 检索从 O(N) → O(log N) |
| 批处理 | 批量写入 > 单条写入 | 写入快 10-100 倍 |
| 归一化 | L2 归一化后使用内积 | 内积等价余弦相似度 |
