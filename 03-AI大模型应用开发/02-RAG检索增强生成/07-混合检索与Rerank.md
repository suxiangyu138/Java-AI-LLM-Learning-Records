# 07 - 混合检索与 Rerank

> 🎯 Dense 检索 + BM25 + Rerank = RAG 检索精度天花板。这一步是 80 分到 95 分的跨越

---

## 目录

1. [为什么需要混合检索](#1-为什么需要混合检索)
2. [BM25 关键词检索集成](#2-bm25-关键词检索集成)
3. [RRF 融合排序](#3-rrf-融合排序)
4. [Cross-Encoder 重排序](#4-cross-encoder-重排序)
5. [完整混合检索 Pipeline](#5-完整混合检索-pipeline)

---

## 1. 为什么需要混合检索

```text
Dense 向量检索（语义）：
  "轿车" → 能匹配 "汽车"、"小轿车" ✅
  "Spring Boot 3.2" → 难以精确匹配版本号 ❌

BM25 关键词检索（精确）：
  "Spring Boot 3.2" → 精确命中 ✅
  "轿车" → 匹配不到 "汽车" ❌

混合检索 = 两者互补 → 精度天花板最高
```

---

## 2. BM25 关键词检索集成

```python
from langchain_community.retrievers import BM25Retriever

# 创建 BM25 检索器
bm25_retriever = BM25Retriever.from_documents(docs)
bm25_retriever.k = 10

# 同时使用 Dense + BM25
dense_results = vectorstore.similarity_search(query, k=10)
bm25_results = bm25_retriever.get_relevant_documents(query)
```

### BM25 + Dense 双路检索

```python
class HybridRetriever:
    def __init__(self, dense_retriever, bm25_retriever):
        self.dense = dense_retriever
        self.bm25 = bm25_retriever
    
    def retrieve(self, query, top_k=10):
        # 双路检索
        dense_docs = self.dense.get_relevant_documents(query)
        sparse_docs = self.bm25.get_relevant_documents(query)
        
        # RRF 融合
        return self.rrf_fusion(dense_docs, sparse_docs, top_k)
```

---

## 3. RRF 融合排序

```text
RRF (Reciprocal Rank Fusion) = 倒数排名融合

核心公式：
  RRF_score(d) = Σ 1 / (k + rank_i(d))

  rank_i(d)：文档 d 在第 i 个检索结果中的排名
  k=60：平滑参数（经验最优值）

优势：无需调参、鲁棒性强、效果稳定
```

```python
def rrf_fusion(result_lists, k=60, top_n=10):
    """RRF 融合多个检索结果"""
    scores = {}
    
    for results in result_lists:
        for rank, doc in enumerate(results, 1):
            doc_id = doc.page_content[:100]  # 用内容前100字符做ID
            scores[doc_id] = scores.get(doc_id, 0) + 1.0 / (k + rank)
    
    # 按 RRF 分数排序
    sorted_items = sorted(scores.items(), key=lambda x: x[1], reverse=True)
    return sorted_items[:top_n]
```

---

## 4. Cross-Encoder 重排序

### 4.1 原理

```text
Bi-Encoder (Dense 检索) vs Cross-Encoder (重排序)：

  Bi-Encoder：Q 和 D 分别编码 → 点积求相似度
    → 快（D 可预计算向量），但精度有限

  Cross-Encoder：Q 和 D 拼接一起输入模型 → 输出相似度分数
    → 慢（每个 Q-D 对都要过模型），但精度高

最佳实践：Bi-Encoder 粗筛（Top-50）+ Cross-Encoder 精排（Top-5）
```

### 4.2 使用 Cross-Encoder 重排序

```python
from sentence_transformers import CrossEncoder

# 加载 Cross-Encoder 模型
reranker = CrossEncoder('BAAI/bge-reranker-v2-m3')

def rerank(query, docs, top_k=5):
    """对检索结果重排序"""
    pairs = [[query, doc.page_content] for doc in docs]
    scores = reranker.predict(pairs)
    
    # 按 Cross-Encoder 分数重排
    scored_docs = list(zip(scores, docs))
    scored_docs.sort(reverse=True)
    
    return [doc for _, doc in scored_docs[:top_k]]

# 使用
initial_docs = retriever.get_relevant_documents(query)  # Top-20 粗筛
final_docs = rerank(query, initial_docs)                 # Cross-Encoder 精排 Top-5
```

### 4.3 Cross-Encoder 模型选择

| 模型 | 语言 | 说明 |
|------|:---:|------|
| **BGE-Reranker-v2-m3** | 多语言 | **首选**，与 BGE-M3 配套 |
| **BGE-Reranker-v2-minicpm** | 中英 | 轻量快速 |
| **Cohere Rerank** | 多语言 | 商业 API，效果好 |
| **Jina Reranker** | 多语言 | 开源，多语言支持好 |

---

## 5. 完整混合检索 Pipeline

```python
class ProductionRAGRetriever:
    def __init__(self, dense_retriever, bm25_retriever, reranker):
        self.dense = dense_retriever
        self.bm25 = bm25_retriever
        self.reranker = reranker
    
    def retrieve(self, query, top_k=5):
        # ① 双路粗筛（各取 20 个）
        dense_docs = self.dense.get_relevant_documents(query)
        bm25_docs = self.bm25.get_relevant_documents(query)
        
        # ② RRF 融合 → 取 Top-20
        merged = self.rrf_fusion([dense_docs, bm25_docs])[:20]
        
        # ③ Cross-Encoder 精排 → Top-K
        pairs = [[query, doc.page_content] for doc in merged]
        scores = self.reranker.predict(pairs)
        scored = sorted(zip(scores, merged), reverse=True)
        
        return [doc for _, doc in scored[:top_k]]

# 效果提升：
#   纯 Dense：NDCG@5 = 0.65
#   + BM25 (RRF)：NDCG@5 = 0.73
#   + Rerank：NDCG@5 = 0.81 → 提升 25%
```

---

## 核心要点回顾

- Dense（语义）+ BM25（精确）= 混合检索 → 1+1>2
- RRF 融合：简单、鲁棒、无需调参（k=60）
- Cross-Encoder 重排序：粗筛→精排 → 精度提升 10-15%
- 中文 Reranker 首选 BGE-Reranker-v2-m3
- 完整链路：Bi-Encoder 粗筛(Top-50) → RRF 融合 → Cross-Encoder 精排(Top-5)
