# 🔍 RAG 混合检索与 Rerank 优化

> **核心摘要**：单一向量检索存在精确关键词匹配盲区，混合检索 + Rerank 是当前 RAG 系统的工业级标配。本文深入讲解 BM25 关键词检索、混合检索实现、BGE Reranker 重排序，以及完整的检索评估方法论。

**前置阅读**：[[AI-RAG-快速学会]] | [[AI-RAG-BGE-M3+Milvus链路]]

---

## 1. 为什么需要混合检索？

| 检索方式 | 优势 | 盲区 |
|----------|------|------|
| **向量检索** | 语义相似度匹配 | 精确关键词（版本号、错误码）、专有名词缩写、代码片段 |
| **关键词检索**（BM25） | 精确术语匹配 | 同义词、跨语言、语义改写 |

**混合检索**通过权重融合，同时兼顾语义相关性和精准匹配。

## 2. BM25 关键词检索

```bash
pip install rank-bm25
```

```python
import jieba
from rank_bm25 import BM25Okapi

class BM25Retriever:
    """基于 BM25 的精确关键词检索"""
    def __init__(self):
        self.corpus: list[list[str]] = []
        self.documents: list[str] = []
        self.bm25: BM25Okapi = None

    def index(self, documents: list[str]):
        self.documents = documents
        self.corpus = [list(jieba.cut(doc)) for doc in documents]
        self.bm25 = BM25Okapi(self.corpus)

    def search(self, query: str, k: int = 5) -> list[tuple[str, float]]:
        tokenized_query = list(jieba.cut(query))
        scores = self.bm25.get_scores(tokenized_query)
        if scores.max() > 0:
            scores = scores / scores.max()
        top_indices = scores.argsort()[-k:][::-1]
        return [(self.documents[i], float(scores[i])) for i in top_indices]
```

## 3. 混合检索实现

### 3.1 核心架构

```python
class HybridRetriever:
    """混合检索器：融合向量检索 + 关键词检索"""
    def __init__(self, vector_retriever, keyword_retriever, alpha: float = 0.7):
        self.vector = vector_retriever
        self.keyword = keyword_retriever
        self.alpha = alpha  # 向量检索权重

    def search(self, query: str, k: int = 5) -> list[dict]:
        vector_results = self.vector.search(query, k=20)
        keyword_results = self.keyword.search(query, k=20)
        # 加权融合逻辑...
```

### 3.2 自适应权重

```python
class AdaptiveHybridRetriever(HybridRetriever):
    """根据 Query 特征自适应调整权重"""
    def _detect_query_type(self, query: str) -> str:
        if re.search(r"[A-Z]+-\d+|\d+\.\d+\.\d+", query):
            return "exact"      # 版本号/错误码 → 关键词为主
        if any(kw in query for kw in ["怎么", "如何", "是什么"]):
            return "semantic"   # 自然语言问句 → 语义为主
        return "mixed"
```

> **重点**：Alpha 值应根据场景调整——精确查询降 alpha（如 0.3），语义查询升 alpha（如 0.8）。

## 4. Rerank 重排序

### 4.1 为什么需要 Rerank？

```
初检 Top-20（快速粗筛） → Rerank Top-5（精准排序） → 注入 LLM
   向量/关键词检索             Cross-Encoder            生成答案
   速度：50ms                 速度：200ms
   精度：中等                  精度：高
```

初检用 **Bi-Encoder**（速度快），Rerank 用 **Cross-Encoder**（精度高但慢），只对 Top-K 候选做 Rerank。

### 4.2 BGE Reranker 实战

```bash
pip install FlagEmbedding
```

```python
from FlagEmbedding import FlagReranker

class Reranker:
    def __init__(self, model_name: str = "BAAI/bge-reranker-v2-m3"):
        self.reranker = FlagReranker(model_name, use_fp16=True)

    def rerank(self, query: str, candidates: list[str], top_k: int = 5) -> list[dict]:
        pairs = [[query, doc] for doc in candidates]
        scores = self.reranker.compute_score(pairs, normalize=True)
        ranked = sorted(zip(candidates, scores), key=lambda x: x[1], reverse=True)
        return [{"content": doc, "score": float(score)} for doc, score in ranked[:top_k]]
```

### 4.3 完整 RAG 检索链路

```python
class FullRAGPipeline:
    def __init__(self):
        self.hybrid_retriever = HybridRetriever(vector_retriever, keyword_retriever, alpha=0.7)
        self.reranker = Reranker()

    def retrieve(self, query: str, top_k: int = 5) -> list[dict]:
        candidates = self.hybrid_retriever.search(query, k=20)
        candidate_texts = [c["content"] for c in candidates]
        return self.reranker.rerank(query, candidate_texts, top_k=top_k)
```

## 5. 检索效果评估

### 5.1 评估指标

| 指标 | 说明 |
|------|------|
| **Precision@5** | 检索结果中相关文档的比例 |
| **Recall@5** | 相关文档中被检索到的比例 |
| **Hit Rate** | 总体命中率 |

### 5.2 不同策略对比实验（典型结果）

| 策略 | Precision@5 | Recall@5 |
|------|-------------|----------|
| 纯向量检索 | 68% | 52% |
| 纯关键词检索 | 45% | 38% |
| 混合检索（alpha=0.7） | 78% | 65% |
| 混合 + Rerank | **92%** | **85%** |

## 6. 优化技巧总结

| 优化方向 | 具体手段 | 预期提升 |
|----------|----------|----------|
| 混合检索 | 向量 + BM25 加权融合 | Recall +15-25% |
| Rerank | Cross-Encoder 重排 Top-20 | Precision@5 +10-20% |
| 分块策略 | 小 chunk 检索 + 大 chunk 上下文扩展 | 答案完整度 +20% |
| Query 改写 | LLM 改写模糊问题 | Recall +5-15% |
| 元数据过滤 | 按时间/来源/标签预过滤 | 噪音 -30% |

---

## 核心要点回顾

- 混合检索 = 向量检索 + BM25 加权融合，弥补单一检索盲区
- Rerank 用 Cross-Encoder 对 Top-K 候选精准排序
- 典型效果：混合 + Rerank 可达 Precision@5 > 90%
- Alpha 权重应自适应调整，精确查询更重关键词

## 参考资料

1. [[AI-RAG-BGE-M3+Milvus链路]]
2. [[AI-RAG-核心知识点2]]
3. [[个人技术文档问答系统：RAG实战项目]]
