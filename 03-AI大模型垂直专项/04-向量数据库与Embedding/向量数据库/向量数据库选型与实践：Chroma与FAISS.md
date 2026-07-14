# 🗄️ 向量数据库选型与实践：Chroma 与 FAISS

> **核心摘要**：本文深入对比 Chroma（轻量级向量数据库）与 FAISS（高性能检索库）的架构差异与适用场景，提供从安装部署、基础操作到完整检索器封装的实战代码，以及面向不同数据规模的选型决策树。

---

## 一、为什么需要向量数据库

传统数据库（MySQL、ES）基于**精确匹配**或**关键词匹配**，无法处理语义搜索。

> **示例**：
> - 用户问："怎么优化慢查询"
> - MySQL 只能匹配包含"慢查询"的记录
> - 向量数据库能找到："SQL 性能调优"、"索引优化方案"（语义相近）

### 向量数据库核心能力

- **高维向量存储**：支持 768/1024 维等高维向量数据
- **近似最近邻搜索（ANN）**：在海量向量中快速找到最相似的 Top-K 结果
- **元数据过滤**：在向量检索基础上叠加结构化条件过滤

### 前置阅读

- [[快速学会 主流向量数据库「全覆盖」]] — 向量数据库全景概览
- [[快速上手Milvus]] — Milvus 生产级部署与实战

---

## 二、向量数据库对比

| 特性 | Chroma | FAISS | Milvus |
|---|---|---|---|
| 定位 | 轻量级，适合原型开发 | 高效检索库 | 分布式，适合生产 |
| 部署 | `pip install` | `pip install` | Docker / K8s |
| 持久化 | 内置（PersistentClient） | 需自行实现 | 内置 |
| 元数据过滤 | 支持 | 不支持（需自行实现） | 支持 |
| 分布式 | 不支持 | 不支持 | 支持 |
| 适用场景 | 原型开发、小规模 | 大规模检索、研究 | 企业级生产 |

---

## 三、Chroma 实战

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
print(results["distances"])   # [[0.123]] — 距离越小越相关
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

## 四、FAISS 实战

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

> **重点**：FAISS 本身不提供持久化与元数据过滤功能。生产环境中需自行管理向量到文本的映射关系，并实现文件序列化机制。

---

## 五、选型决策树

```
需要元数据过滤？
├── 是 → Chroma（原型）/ Milvus（生产）
└── 否 → 数据量？
    ├── < 10万 → Chroma（最简单）
    ├── 10万-100万 → FAISS（最快）
    └── > 100万 → Milvus（分布式）
```

---

## 六、性能优化要点

| 优化项 | 方法 | 效果 |
|---|---|---|
| 降维 | PCA 降维到 256/512 | 内存减半，速度提升 |
| 量化 | PQ 乘积量化 | 内存降低 10-30 倍 |
| 索引 | HNSW / IVF | 检索从 O(N) → O(log N) |
| 批处理 | 批量写入 > 单条写入 | 写入快 10-100 倍 |
| 归一化 | L2 归一化后使用内积 | 内积等价余弦相似度 |

> **注意**：Java 后端若需使用 FAISS，可考虑通过 JNI 调用或使用 [[DJL 核心知识点]] 中介绍的 DJL 框架进行向量计算。

---

## 核心要点回顾

- Chroma 适合原型开发和小规模数据，支持元数据过滤和内置持久化
- FAISS 是高性能检索库，适合大规模离线检索，但需自行实现持久化和元数据管理
- 选型决策核心：是否需元数据过滤 → 数据规模 → 生产可用性
- 性能优化可从降维、量化、索引结构、批处理四个维度入手
- Java 后端集成可通过 DJL 或 JNI 方式实现 FAISS 调用

## 参考资料

1. Chroma 官方文档：https://docs.trychroma.com
2. FAISS GitHub 仓库：https://github.com/facebookresearch/faiss
3. FAISS 索引类型指南：https://github.com/facebookresearch/faiss/wiki/Guidelines-to-choose-an-index
4. ChromaDB Python 参考：https://pypi.org/project/chromadb/
