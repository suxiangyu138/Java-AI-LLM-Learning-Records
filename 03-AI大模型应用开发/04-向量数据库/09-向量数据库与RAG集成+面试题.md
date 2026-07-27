# 09 - 向量数据库与RAG集成 + 面试题

> 🎯 向量数据库是 RAG 的心脏 — 集成架构、生产避坑、高频面试题一文打尽

---

## 目录

1. [生产级RAG集成架构](#1-生产级rag集成架构)
2. [多模态向量存储](#2-多模态向量存储)
3. [混合检索架构](#3-混合检索架构)
4. [生产避坑 TOP 6](#4-生产避坑-top-6)
5. [高频面试题](#5-高频面试题)

---

## 1. 生产级RAG集成架构

```text
┌──────────────────────────────────────────────────┐
│  应用层：RAG Service（LangChain/LlamaIndex）      │
├──────────────────────────────────────────────────┤
│  检索层：                                          │
│  ├── 向量检索 (Milvus)                            │
│  ├── 关键词检索 (Elasticsearch)                   │
│  └── RRF 融合                                     │
├──────────────────────────────────────────────────┤
│  存储层：                                          │
│  ├── Milvus → 向量                                │
│  ├── PostgreSQL → 原始文档+元数据                  │
│  └── MinIO → 文件（PDF/图片）                     │
├──────────────────────────────────────────────────┤
│  离线管线：文档→切片→Embedding→入库               │
└──────────────────────────────────────────────────┘
```

---

## 2. 多模态向量存储

```text
文本和图片的 Embedding 在不同向量空间 → 需分开存储

方案 1：分 Collection
  text_collection (1024维，BGE-M3)
  image_collection (768维，CLIP)

方案 2：统一多模态 Embedding
  Jina CLIP → 文本和图片映射到同一 768 维空间
  → 同一 Collection → "搜图片"直接用文本查询
```

---

## 3. 混合检索架构

```python
class HybridSearchEngine:
    def __init__(self, vector_store, es_client):
        self.vector_store = vector_store  # Milvus
        self.es = es_client              # Elasticsearch
    
    def search(self, query, top_k=10):
        # Dense 检索
        dense_results = self.vector_store.search(query, top_k=20)
        # Sparse 检索
        sparse_results = self.es.search(query, size=20)
        # RRF 融合
        return self.rrf_fusion(dense_results, sparse_results)[:top_k]
```

---

## 4. 生产避坑 TOP 6

| # | 坑 | 解决 |
|---|------|------|
| 1 | **未建索引** → 暴力检索太慢 | 生产必须建 HNSW/IVF 索引 |
| 2 | **混用 Embedding 模型** | 入库/查询用同一模型 |
| 3 | **忽略索引更新** → 新增数据查不到 | 手动 `index.add()` 或定时重建 |
| 4 | **未设连接池** → 并发高时超时 | Milvus 连池 min=5, max=20 |
| 5 | **无备份** → 数据丢失 | Milvus-backup 定期备份 |
| 6 | **冷启动慢** | 启动时预加载索引到内存 |

---

## 5. 高频面试题

### Q1: 向量数据库和传统数据库的核心区别？

```text
传统：精确匹配（WHERE x=y）
向量：语义相似度（Top-K 近似最近邻）

向量数据库专为高维向量检索优化（ANN 索引/HNSW/IVF）
```

### Q2: HNSW 为什么快？

```text
HNSW = 多层图结构 → 上层长跳转快速定位区域、底层精细搜索
类比：高速公路→城市道路→社区小路的导航系统
O(log N) 的时间复杂度，比 Flat 的 O(N) 快数百倍
```

### Q3: IVF 的 nprobe 参数怎么调？

```text
nprobe ↑ → 搜索更多簇 → 精度↑ 速度↓
生产建议：nprobe=8~64，根据 Recall 要求动态调整
```

### Q4: Milvus 和 FAISS 怎么选？

```text
FAISS：嵌入式库 → Python 进程内、极致性能、无持久化
Milvus：独立服务 → 分布式、持久化、多租户、SDK丰富
Java后端 → Milvus（有原生 Java SDK）
```

### Q5: 如何保证向量检索的一致性？

```text
① 入库/查询用同一 Embedding 模型
② 向量必须归一化（余弦相似度=内积的前提）
③ 不同模型向量不能混存（独立 Collection）
```

| # | 更多面试题 |
|---|------|
| 6 | 向量数据库的 CAP 如何权衡？ |
| 7 | PQ量化为什么几乎不损失精度？ |
| 8 | 如何进行向量数据的增量更新？ |
| 9 | 多模态数据如何存储和检索？ |
| 10 | 向量数据库的索引应该何时重建？ |

---

## 核心要点回顾

- 生产 RAG = Milvus(向量) + ES(关键词) + PostgreSQL(元数据)
- 入库/查询必须同一 Embedding 模型
- 生产必建索引（HNSW/IVF）
- 混合检索 = 向量 + 关键词 + RRF 融合
- Java 后端首选 Milvus
