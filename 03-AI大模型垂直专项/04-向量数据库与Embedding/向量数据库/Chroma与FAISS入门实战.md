# 🗄️ Chroma 与 FAISS 入门实战

> **核心摘要**：向量数据库是 RAG 系统的"记忆引擎"。本文从零开始实战 Chroma（零配置原型）与 FAISS（高性能生产）两大向量检索方案，涵盖安装部署、核心操作、完整 RAG 检索器封装，以及性能优化与选型指南。

---

## 一、为什么需要向量数据库

```
用户问题 → Embedding → [0.1, 0.3, -0.2, ...] → 向量数据库查询
                                                     ↓
                                         Top-K 最相似的文档片段
                                                     ↓
                                         注入 Prompt → LLM 生成答案
```

> **关键数字**：在 100 万条向量中做 Top-K 近似检索，Chroma 约 50ms，FAISS 约 10ms，全量遍历约 2000ms。

### 前置阅读

- [[快速吃透 Embedding]] — Embedding 概念基础
- [[快速学会 主流向量数据库「全覆盖」]] — 向量数据库全景概览
- [[向量数据库选型与实践：Chroma与FAISS]] — 深入选型对比

---

## 二、Chroma：零配置向量数据库

### 2.1 安装与启动

```bash
pip install chromadb
```

```python
import chromadb

# 无需额外部署，直接 Python 调用
client = chromadb.Client()  # 内存模式（原型）
# client = chromadb.PersistentClient(path="./chroma_db")  # 持久化模式（推荐）
```

### 2.2 核心操作

```python
import chromadb
from chromadb.utils import embedding_functions

# 1. 创建 Client
client = chromadb.PersistentClient(path="./chroma_db")

# 2. 定义 Embedding 函数（用开源模型，免费）
ef = embedding_functions.SentenceTransformerEmbeddingFunction(
    model_name="BAAI/bge-small-zh-v1.5"  # 中文 Embedding 模型
)

# 3. 创建 Collection（类似数据库的"表"）
collection = client.get_or_create_collection(
    name="java_docs",
    embedding_function=ef,
    metadata={"hnsw:space": "cosine"}  # 余弦相似度
)

# 4. 插入文档（自动向量化）
documents = [
    "Spring Boot 是一个基于 Spring 框架的快速开发脚手架",
    "MyBatis 是一款优秀的持久层框架，支持自定义 SQL",
    "Redis 是一个高性能的 key-value 内存数据库",
]
collection.add(
    documents=documents,
    ids=["doc_1", "doc_2", "doc_3"],
    metadatas=[
        {"source": "spring-boot.md", "topic": "Spring"},
        {"source": "mybatis.md", "topic": "ORM"},
        {"source": "redis.md", "topic": "Cache"},
    ]
)

# 5. 查询
results = collection.query(
    query_texts=["如何连接数据库？"],
    n_results=2  # 返回 Top-2
)
print(results["documents"][0])
# ['MyBatis 是一款优秀的持久层框架...', 'Redis 是一个高性能的...']
print(results["distances"][0])  # 距离越近 = 越相关
# [0.234, 0.891]
```

### 2.3 带过滤条件的查询

```python
# 只从特定主题的文档中检索
results = collection.query(
    query_texts=["缓存怎么做？"],
    n_results=3,
    where={"topic": "Cache"}  # 元数据过滤
)
```

### 2.4 完整 RAG 检索链路

```python
class ChromaRAG:
    """基于 Chroma 的 RAG 检索器"""

    def __init__(self, collection_name: str, persist_path: str = "./chroma_db"):
        self.client = chromadb.PersistentClient(path=persist_path)
        self.ef = embedding_functions.SentenceTransformerEmbeddingFunction(
            model_name="BAAI/bge-small-zh-v1.5"
        )
        self.collection = self.client.get_or_create_collection(
            name=collection_name,
            embedding_function=self.ef
        )

    def add_documents(self, docs: list[str], metadatas: list[dict] = None):
        """批量添加文档"""
        ids = [f"doc_{i}" for i in range(len(docs))]
        self.collection.add(documents=docs, ids=ids, metadatas=metadatas)

    def search(self, query: str, k: int = 5) -> list[dict]:
        """检索并返回带元数据的结果"""
        results = self.collection.query(query_texts=[query], n_results=k)
        return [
            {
                "content": doc,
                "metadata": meta,
                "score": 1 - dist  # 距离 → 相似度
            }
            for doc, meta, dist in zip(
                results["documents"][0],
                results["metadatas"][0],
                results["distances"][0]
            )
        ]

    def build_context(self, query: str, k: int = 5) -> str:
        """构建 Prompt 上下文"""
        results = self.search(query, k)
        context_parts = []
        for i, r in enumerate(results, 1):
            context_parts.append(
                f"[参考{i}] (来源: {r['metadata'].get('source', 'unknown')})\n{r['content']}"
            )
        return "\n\n".join(context_parts)
```

---

## 三、FAISS：高性能向量检索

### 3.1 安装与基础使用

```bash
pip install faiss-cpu  # CPU 版
# pip install faiss-gpu  # GPU 版（需 CUDA）
```

```python
import faiss
import numpy as np

# FAISS 不会自动 Embedding，需要自己处理向量
dimension = 768  # bge-small-zh 输出 768 维

# 1. 创建索引
index = faiss.IndexFlatIP(dimension)  # 内积（需归一化=余弦相似度）
# IndexFlatL2: 欧氏距离（越小越相似）

# 2. 添加向量
def add_vectors(index: faiss.Index, texts: list[str], embed_fn):
    """批量 Embedding + 插入 FAISS"""
    vectors = embed_fn(texts)  # shape: (N, 768)
    vectors = vectors / np.linalg.norm(vectors, axis=1, keepdims=True)  # 归一化
    index.add(vectors.astype(np.float32))
    return index

# 3. 查询
def search(index: faiss.Index, query: str, embed_fn, k: int = 5):
    query_vec = embed_fn([query])
    query_vec = query_vec / np.linalg.norm(query_vec, axis=1, keepdims=True)
    distances, indices = index.search(query_vec.astype(np.float32), k)
    return distances[0], indices[0]  # 相似度 + 文档索引
```

### 3.2 索引类型选择

```python
# IndexFlatIP — 精确检索，适合 < 10w 向量
index = faiss.IndexFlatIP(768)

# IndexIVFFlat — 倒排索引，适合 10w-1000w 向量，速度提升 10-100x
quantizer = faiss.IndexFlatIP(768)
index = faiss.IndexIVFFlat(quantizer, 768, nlist=100)
index.train(vectors)  # 必须先训练！
index.add(vectors)

# IndexHNSWFlat — 图索引，适合高精度 + 大规模
index = faiss.IndexHNSWFlat(768, M=32)  # M=连接数，越大越准但越慢

# 保存与加载
faiss.write_index(index, "faiss_index.bin")
index = faiss.read_index("faiss_index.bin")
```

### 3.3 完整 FAISS RAG 检索器

```python
import faiss
import numpy as np
from sentence_transformers import SentenceTransformer


class FAISSRAG:
    """基于 FAISS 的高性能 RAG 检索器"""

    def __init__(self, model_name: str = "BAAI/bge-small-zh-v1.5"):
        self.embed_model = SentenceTransformer(model_name)
        self.dim = self.embed_model.get_sentence_embedding_dimension()
        self.index: faiss.Index = None
        self.documents: list[str] = []
        self.metadatas: list[dict] = []

    def build_index(self, documents: list[str], metadatas: list[dict] = None):
        """构建索引"""
        self.documents = documents
        self.metadatas = metadatas or [{}] * len(documents)

        vectors = self.embed_model.encode(
            documents,
            normalize_embeddings=True,  # 输出归一化向量
            show_progress_bar=True
        )

        self.index = faiss.IndexFlatIP(self.dim)
        self.index.add(vectors.astype(np.float32))

    def search(self, query: str, k: int = 5) -> list[dict]:
        query_vec = self.embed_model.encode(
            [query],
            normalize_embeddings=True
        )
        distances, indices = self.index.search(query_vec.astype(np.float32), k)

        results = []
        for dist, idx in zip(distances[0], indices[0]):
            if idx < 0 or idx >= len(self.documents):
                continue
            results.append({
                "content": self.documents[idx],
                "metadata": self.metadatas[idx],
                "score": float(dist)
            })
        return results
```

> **注意**：FAISS 是纯向量检索库，不提供元数据过滤功能。若需在 FAISS 上层实现元数据过滤，需自行维护倒排索引或二次过滤逻辑。

---

## 四、Chroma vs FAISS 选型指南

| 维度 | Chroma | FAISS |
|---|---|---|
| 部署复杂度 | 零配置，`pip install` 即可 | 简单，但需自己管理 Embedding |
| 查询速度 | 中等（10w 向量 ~50ms） | 极快（10w 向量 ~10ms） |
| 元数据管理 | 内置，支持过滤 | 需自行维护映射关系 |
| 持久化 | 内置 | 需手动 save/load |
| 适用阶段 | 原型验证、小规模应用 | 生产环境、大规模检索 |
| 社区生态 | 与 LangChain 深度集成 | Meta 出品，学术引用广 |

> **推荐策略**：原型阶段用 Chroma → 确认方案可行 → 数据量 > 10w 或性能敏感时迁移到 FAISS / Milvus。

---

## 五、实战：文档分块 + Chroma 索引完整链路

```python
from langchain.text_splitter import RecursiveCharacterTextSplitter
import chromadb


def build_rag_index(markdown_files: list[str]) -> chromadb.Collection:
    """从 Markdown 文档构建 RAG 索引"""

    # 1. 读取文档
    all_chunks = []
    all_metadatas = []
    for file_path in markdown_files:
        with open(file_path, "r", encoding="utf-8") as f:
            content = f.read()

        # 2. 文本分割
        splitter = RecursiveCharacterTextSplitter(
            chunk_size=500,
            chunk_overlap=50,
            separators=["\n## ", "\n### ", "\n", "。", ".", " "]
        )
        chunks = splitter.split_text(content)

        # 3. 记录来源
        all_chunks.extend(chunks)
        all_metadatas.extend([
            {"source": file_path, "chunk_index": i}
            for i in range(len(chunks))
        ])

    # 4. 构建 Chroma 索引
    client = chromadb.PersistentClient(path="./rag_index")
    ef = embedding_functions.SentenceTransformerEmbeddingFunction(
        model_name="BAAI/bge-small-zh-v1.5"
    )
    collection = client.get_or_create_collection(
        name="tech_docs",
        embedding_function=ef
    )
    collection.add(
        documents=all_chunks,
        ids=[f"chunk_{i}" for i in range(len(all_chunks))],
        metadatas=all_metadatas
    )

    print(f"索引完成：{len(all_chunks)} 个文档片段")
    return collection
```

---

## 六、性能优化要点

1. **批量插入**：一次 `add()` 1000 条比 1000 次 `add()` 1 条快 50+ 倍
2. **向量缓存**：对已索引的文档做 Embedding，避免重复计算
3. **维度压缩**：用 PCA 将 768 维降到 256 维，速度翻倍，精度损失 < 2%
4. **索引预热**：FAISS 首次查询慢（冷启动），先执行一次 dummy query

> **重点**：对于 Java 后端项目，若需集成向量检索，推荐使用 Milvus（提供原生 Java SDK），而非 Chroma/FAISS，后者缺乏成熟的 Java 生态支持。

---

## 快速调试检查清单

- [ ] Embedding 模型是否正确加载？测试 `model.encode(["test"])` 返回 `(1, dim)` 的数组
- [ ] 插入和查询用的是否是**同一个** Embedding 函数/模型？
- [ ] 向量是否做了**归一化**？（余弦相似度需要归一化）
- [ ] FAISS 查询前是否调用了 `np.float32` 类型转换？
- [ ] Chroma `query_texts` 参数是 `list[str]`，不要只传 `str`

---

## 核心要点回顾

- Chroma 零配置即可使用，内置持久化和元数据过滤，适合原型验证与学习
- FAISS 性能极佳（10w 向量 ~10ms），但需自行管理 Embedding、持久化和元数据
- 选型推荐：原型用 Chroma，生产大规模用 FAISS/Milvus
- 文档分块 + 向量索引是 RAG 系统的标准实践
- 性能优化关键：批量插入、向量缓存、维度压缩、索引预热

## 参考资料

1. Chroma 官方文档：https://docs.trychroma.com
2. FAISS GitHub 仓库：https://github.com/facebookresearch/faiss
3. FAISS 索引选择指南：https://github.com/facebookresearch/faiss/wiki/Guidelines-to-choose-an-index
4. SentenceTransformers 模型库：https://www.sbert.net/docs/pretrained_models.html
5. LangChain Text Splitters：https://python.langchain.com/docs/modules/data_connection/document_transformers/
