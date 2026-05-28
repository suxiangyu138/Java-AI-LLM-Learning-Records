# RAG 混合检索与 Rerank 优化

> **核心认知**：单一向量检索有盲区（精确关键词匹配差），混合检索 + Rerank 是当前 RAG 系统的工业级标配。
> **前置阅读**：`快速学会 RAG.md`、`Chroma与FAISS入门实战.md`

---

## 1. 为什么需要混合检索？

```
            向量检索                          关键词检索
        语义相似度匹配                     BM25 / TF-IDF 精确匹配
              │                                  │
        "Spring 怎么用"                   "Spring Boot 3.2"
        → 能找到相关文档                   → 精确命中版本号
              │                                  │
              └──────────┬───────────────────────┘
                         ↓
                     混合检索
                   权重融合结果
```

**向量检索的盲区**：
- 精确关键词（版本号 "3.2.1"、错误码 "ERR-5001"）
- 专有名词缩写（"DI" = 依赖注入，向量可能理解不了）
- 代码片段搜索

**关键词检索的盲区**：
- 同义词（"数据库" vs "DB"）
- 跨语言（"如何连接数据库" vs "How to connect to database"）
- 语义改写（"怎么做数据持久化" vs "MyBatis 配置教程"）

---

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
        self.corpus: list[list[str]] = []  # 分词后的文档列表
        self.documents: list[str] = []      # 原始文档
        self.bm25: BM25Okapi = None

    def index(self, documents: list[str]):
        """构建 BM25 索引"""
        self.documents = documents
        # 中文分词
        self.corpus = [
            list(jieba.cut(doc)) for doc in documents
        ]
        self.bm25 = BM25Okapi(self.corpus)

    def search(self, query: str, k: int = 5) -> list[tuple[str, float]]:
        tokenized_query = list(jieba.cut(query))
        scores = self.bm25.get_scores(tokenized_query)
        # 归一化分数到 [0, 1]
        if scores.max() > 0:
            scores = scores / scores.max()
        top_indices = scores.argsort()[-k:][::-1]
        return [(self.documents[i], float(scores[i])) for i in top_indices]
```

---

## 3. 混合检索实现

### 3.1 核心架构

```python
from typing import Protocol


class Retriever(Protocol):
    """检索器统一接口"""
    def search(self, query: str, k: int) -> list[tuple[str, float]]:
        """返回 [(文档内容, 归一化得分)]"""
        ...


class HybridRetriever:
    """混合检索器：融合向量检索 + 关键词检索"""

    def __init__(
        self,
        vector_retriever: Retriever,
        keyword_retriever: Retriever,
        alpha: float = 0.7  # 向量检索权重（0.7 = 更看重语义）
    ):
        self.vector = vector_retriever
        self.keyword = keyword_retriever
        self.alpha = alpha

    def search(self, query: str, k: int = 5) -> list[dict]:
        # 各检索 Top-K（多取一些做融合）
        vector_results = self.vector.search(query, k=20)
        keyword_results = self.keyword.search(query, k=20)

        # 加权融合
        merged_scores: dict[str, dict] = {}

        for content, score in vector_results:
            merged_scores[content] = {
                "content": content,
                "vector_score": score,
                "keyword_score": 0.0,
                "final_score": self.alpha * score
            }

        for content, score in keyword_results:
            if content in merged_scores:
                merged_scores[content]["keyword_score"] = score
                merged_scores[content]["final_score"] += (1 - self.alpha) * score
            else:
                merged_scores[content] = {
                    "content": content,
                    "vector_score": 0.0,
                    "keyword_score": score,
                    "final_score": (1 - self.alpha) * score
                }

        # 按融合分数排序
        sorted_results = sorted(
            merged_scores.values(),
            key=lambda x: x["final_score"],
            reverse=True
        )
        return sorted_results[:k]
```

### 3.2 自适应权重

```python
class AdaptiveHybridRetriever(HybridRetriever):
    """根据 Query 特征自适应调整权重"""

    def _detect_query_type(self, query: str) -> str:
        """检测查询类型"""
        # 包含版本号、错误码等 → 关键词为主
        if re.search(r"[A-Z]+-\d+|\d+\.\d+\.\d+", query):
            return "exact"
        # 自然语言问句 → 语义为主
        if any(kw in query for kw in ["怎么", "如何", "是什么", "为什么", "区别"]):
            return "semantic"
        return "mixed"

    def search(self, query: str, k: int = 5) -> list[dict]:
        qtype = self._detect_query_type(query)
        if qtype == "exact":
            self.alpha = 0.3  # 更看重关键词
        elif qtype == "semantic":
            self.alpha = 0.8  # 更看重语义
        # else: keep default alpha
        return super().search(query, k)
```

---

## 4. Rerank 重排序

### 4.1 为什么检索后还要 Rerank？

```
初检 Top-20（快速粗筛）  →  Rerank Top-5（精准排序）  →  注入 LLM
    向量/关键词检索               Cross-Encoder              生成答案
    速度 : 50ms                  速度 : 200ms
    精度 : 中等                   精度 : 高
```

**关键原理**：初检用 Bi-Encoder（Query 和 Doc 独立编码，速度快），Rerank 用 Cross-Encoder（Query + Doc 联合编码，精度高但慢），只对 Top-K 候选做 Rerank。

### 4.2 BGE Reranker 实战

```bash
pip install FlagEmbedding
```

```python
from FlagEmbedding import FlagReranker


class Reranker:
    """重排序器"""

    def __init__(self, model_name: str = "BAAI/bge-reranker-v2-m3"):
        self.reranker = FlagReranker(
            model_name,
            use_fp16=True  # GPU 加速
        )

    def rerank(self, query: str, candidates: list[str], top_k: int = 5) -> list[dict]:
        """对候选文档重排序"""
        # 构建 (query, doc) 对
        pairs = [[query, doc] for doc in candidates]

        # 计算相关性分数
        scores = self.reranker.compute_score(pairs, normalize=True)

        # 排序
        ranked = sorted(
            zip(candidates, scores),
            key=lambda x: x[1],
            reverse=True
        )

        return [
            {"content": doc, "score": float(score)}
            for doc, score in ranked[:top_k]
        ]
```

### 4.3 完整 RAG 检索链路（混合检索 + Rerank）

```python
class FullRAGPipeline:
    """完整的 RAG 检索流水线"""

    def __init__(self):
        self.hybrid_retriever = HybridRetriever(
            vector_retriever=ChromaVectorRetriever(collection),
            keyword_retriever=BM25Retriever(),
            alpha=0.7
        )
        self.reranker = Reranker()

    def retrieve(self, query: str, top_k: int = 5) -> list[dict]:
        # 第 1 步：混合检索，召回 Top-20 候选
        candidates = self.hybrid_retriever.search(query, k=20)
        candidate_texts = [c["content"] for c in candidates]

        # 第 2 步：Rerank 重排序，取 Top-K
        results = self.reranker.rerank(query, candidate_texts, top_k=top_k)

        # 第 3 步：合并元数据
        for result in results:
            matched = next(
                (c for c in candidates if c["content"] == result["content"]),
                None
            )
            if matched:
                result["metadata"] = matched.get("metadata", {})
                result["vector_score"] = matched.get("vector_score")
                result["keyword_score"] = matched.get("keyword_score")

        return results

    def generate(self, query: str) -> str:
        """检索 + 生成答案"""
        results = self.retrieve(query)
        context = "\n\n".join(
            f"[参考{i+1}] {r['content']}"
            for i, r in enumerate(results)
        )
        prompt = f"""根据以下参考资料回答问题。如果资料中找不到答案，说"未找到相关信息"。

{context}

问题: {query}
答案:"""
        return llm_client.chat(prompt)
```

---

## 5. 检索效果评估

### 5.1 评估指标

```python
def evaluate_retrieval(
    retriever,
    test_queries: list[dict]
) -> dict:
    """
    test_queries = [
        {"query": "...", "relevant_docs": ["doc_id_1", "doc_id_2"]},
        ...
    ]
    """
    total_hits = 0
    total_relevant = 0
    precision_sum = 0
    recall_sum = 0

    for case in test_queries:
        results = retriever.search(case["query"], k=5)
        retrieved_ids = [r.get("id") for r in results]
        relevant_ids = case["relevant_docs"]

        hits = len(set(retrieved_ids) & set(relevant_ids))
        total_hits += hits
        total_relevant += len(relevant_ids)

        precision = hits / len(retrieved_ids) if retrieved_ids else 0
        recall = hits / len(relevant_ids) if relevant_ids else 0
        precision_sum += precision
        recall_sum += recall

    n = len(test_queries)
    return {
        "precision@5": precision_sum / n,
        "recall@5": recall_sum / n,
        "hit_rate": total_hits / total_relevant if total_relevant else 0,
    }
```

### 5.2 不同策略对比实验

```python
def ab_test_retrieval(test_queries: list[dict]):
    """对比不同检索策略的效果"""
    strategies = {
        "纯向量检索": PureVectorRetriever(collection),
        "纯关键词检索": BM25Retriever(),
        "混合检索(alpha=0.5)": HybridRetriever(vector, keyword, alpha=0.5),
        "混合检索(alpha=0.7)": HybridRetriever(vector, keyword, alpha=0.7),
        "混合+Rerank": FullRAGPipeline(),
    }

    for name, retriever in strategies.items():
        metrics = evaluate_retrieval(retriever, test_queries)
        print(f"\n{name}:")
        print(f"  Precision@5: {metrics['precision@5']:.1%}")
        print(f"  Recall@5:   {metrics['recall@5']:.1%}")
        print(f"  Hit Rate:   {metrics['hit_rate']:.1%}")

# 典型结果示例：
# 纯向量检索:         Precision@5: 68%  Recall@5: 52%
# 纯关键词检索:       Precision@5: 45%  Recall@5: 38%
# 混合检索(0.7):      Precision@5: 78%  Recall@5: 65%
# 混合+Rerank:        Precision@5: 92%  Recall@5: 85%
```

---

## 6. 优化技巧总结

| 优化方向 | 具体手段 | 预期提升 |
|----------|----------|----------|
| 混合检索 | 向量 + BM25 加权融合 | Recall +15-25% |
| Rerank | Cross-Encoder 重排 Top-20 | Precision@5 +10-20% |
| 分块策略 | 小 chunk 检索 + 大 chunk 上下文扩展 | 答案完整度 +20% |
| Query 改写 | LLM 改写模糊问题为多个精确查询 | Recall +5-15% |
| 元数据过滤 | 按时间/来源/标签预过滤 | 噪音 -30% |

---

## 快速调试检查清单

- [ ] BM25 的中文分词是否正确？（检查 `jieba.cut` 结果）
- [ ] 向量和关键词的得分是否都做了归一化（0-1）？
- [ ] Reranker 的输入是 `[[query, doc], ...]` 格式？
- [ ] Alpha 值是否根据场景调整过？（精确查询降 alpha，语义查询升 alpha）
- [ ] 混合检索的候选数量是否足够？（建议 Top-20 给 Rerank，而非直接 Top-5）
