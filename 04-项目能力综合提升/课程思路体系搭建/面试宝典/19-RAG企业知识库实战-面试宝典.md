# RAG 企业知识库实战 面试宝典
> 基于课程大纲全面覆盖面试高频考点 — 从 Naive RAG 到 Modular RAG 全架构体系，涵盖 Milvus/Chroma/Qdrant 向量数据库、BGE/BM25 双路检索、Rerank 重排序、RAGAS/TruLens 评估，以及企业级知识库落地全流程

## 目录
1. [一、基础概念速答（18题）](#一基础概念速答18题)
2. [二、深度原理剖析（12题）](#二深度原理剖析12题)
3. [三、实战场景题（10题）](#三实战场景题10题)
4. [四、手写代码题（8题）](#四手写代码题8题)
5. [五、系统设计题（5题）](#五系统设计题5题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板（Top 5）](#七面试回答模板top-5)
8. [八、快速查漏补缺 Checklist](#八快速查漏补缺-checklist)

---

## 一、基础概念速答（18题）

### 1. 什么是 RAG？核心解决了什么问题？
> RAG（Retrieval-Augmented Generation）是一种"先检索、后生成"的技术架构，让 LLM 在生成回答前先从外部知识库检索相关信息作为上下文。

| 解决的核心问题 | 说明 |
|---------------|------|
| 知识滞后性 | 训练数据截止后无法更新，RAG 通过外部知识库实现"即插即用" |
| 幻觉（Hallucination） | 检索结果作为锚定，强制模型基于事实生成 |
| 可解释性 | 回答可溯源到具体文档，满足审计要求 |
| 部署成本 | 无需重新微调即可更新知识 |

> 💡 面试常问："RAG 和 Fine-tuning 有什么区别？" RAG 更新知识无需训练，适合高频变化的场景；微调适合固定风格的深度定制。

### 2. RAG 的完整工作流程是什么？
> 一套标准的 RAG 流程包含 **Ingestion（数据摄入）→ Chunking（文本分割）→ Embedding（向量化）→ Indexing（索引构建）→ Retrieval（检索）→ Re-ranking（重排序）→ Generation（生成）** 七个核心步骤。

```
离线阶段:
  知识文档 → PDF解析 → Chunk分割 → Embedding向量化 → 存储至Vector DB
在线阶段:
  用户Query → Query向量化 → Vector DB检索(Top-K) → Rerank → LLM生成 → 返回回答
```

### 3. Naive RAG、Advanced RAG、Modular RAG 有什么区别？
| 名称 | 架构特点 | 代表技术 | 适用场景 |
|------|---------|----------|----------|
| **Naive RAG** | 线性流程：检索 → 合并 → 生成 | LangChain 基础链 | 简单问答、原型验证 |
| **Advanced RAG** | 检索前/后优化 | 层次索引、句子窗口、HyDE、查询重写、Prompt 压缩 | 高精度问答场景 |
| **Modular RAG** | 可编排的工作流模式 | 顺序/条件/分支/迭代/递归/FLARE/TOC | 复杂业务逻辑 |

> 🎯 面试高频："你在项目中用了哪种 RAG 架构？" 回答策略：先说 Naive RAG 的局限性（单轮检索精度不足），再引出 Advanced 和 Modular 的优化策略。

### 4. 什么是 Chunking？常见的分块策略有哪些？
> Chunking（文本分割）是将长文档切分为便于检索的短文本块，直接决定检索精度。

| 分块策略 | 原理 | 优缺点 | chunk_size 建议 |
|----------|------|--------|-----------------|
| **Fixed-size（固定长度）** | 按字符/token 数切分 | 简单高效，但可能切断语义 | 256-1024 tokens |
| **Semantic（语义分割）** | 按段落/章节边界切分 | 保留语义完整性，复杂度高 | 按文档结构 |
| **Recursive（递归分割）** | 按分隔符层级递归 | 兼顾结构与长度 | 200-512 tokens |
| **Document-aware（文档感知）** | 利用标题/表格结构分割 | 精度最高，依赖解析质量 | 按层级 |

```python
# RecursiveCharacterTextSplitter 实践
from langchain.text_splitter import RecursiveCharacterTextSplitter

splitter = RecursiveCharacterTextSplitter(
    chunk_size=512,
    chunk_overlap=50,           # 保留重叠避免切割关键信息
    separators=["\n\n", "\n", "。", "，", " ", ""],
    length_function=len,
)
chunks = splitter.split_text(document)
```

> 💡 chunk_overlap 一般设置为 chunk_size 的 10%-20%，推荐 50-100 tokens。

### 5. 什么是 Dense Embedding 和 Sparse Embedding？
| 类型 | 代表模型 | 向量特点 | 优势 | 劣势 |
|------|----------|----------|------|------|
| **Dense（稠密）** | BGE-Large, text-embedding-3, M3E | 低维稠密向量（768维） | 语义理解强 | 无法匹配精确关键词 |
| **Sparse（稀疏）** | BM25, SPLADE | 高维稀疏向量（词汇维度） | 精确关键词匹配 | 无语义理解能力 |

> 企业最佳实践：**双路检索（Dual-Channel Retrieval）** = Dense + Sparse 混合，兼顾语义和精确匹配。

### 6. 什么是双路检索（Dual-Channel Retrieval）？
> 同时使用稠密向量检索（语义）和稀疏向量检索（关键词），然后通过权重融合或 Rerank 合并结果。

```python
# 双路检索核心逻辑
class DualChannelRetriever:
    def __init__(self, dense_retriever, sparse_retriever, alpha=0.7):
        self.dense = dense_retriever    # BGE-Large 等
        self.sparse = sparse_retriever  # BM25 等
        self.alpha = alpha              # 稠密检索权重

    def search(self, query: str, top_k: int = 5):
        dense_results = self.dense.search(query, k=top_k * 2)
        sparse_results = self.sparse.search(query, k=top_k * 2)
        # 加权融合 Reciprocal Rank Fusion (RRF)
        return self._rrf_fusion(dense_results, sparse_results, top_k)
```

### 7. 什么是 Rerank？为什么需要 Rerank？
> Rerank（重排序）是对初检结果进行二次精排的过程。

```
初检阶段：向量检索 Top-20（快速粗筛，毫秒级）
Rerank 阶段：Cross-Encoder 对 (query, doc) 打分（慢但精准，百毫秒级）
输出阶段：取 Top-5 送入 LLM
```

| 对比 | Bi-Encoder | Cross-Encoder |
|------|-----------|---------------|
| 编码方式 | Query 和 Doc 独立编码 | Query 和 Doc 拼接编码 |
| 计算速度 | 快（可预计算） | 慢（需实时计算） |
| 精度 | 中 | 高 |
| 适用阶段 | 初检（Retrieval） | Rerank（重排序） |
| 代表模型 | BGE-Large、text-embedding-3 | bge-reranker-v2-m3 |

### 8. 向量数据库选型对比：Milvus vs Chroma vs Qdrant
| 特性 | Milvus | Chroma | Qdrant |
|------|--------|--------|--------|
| 部署模式 | 分布式集群 | 轻量嵌入式 | 单机/集群 |
| 数据规模 | 亿级 | 百万级 | 千万级 |
| 索引类型 | IVF、HNSW、DiskANN | HNSW | HNSW |
| 混合检索 | 支持 Vector + Scalar | 仅过滤 | 支持 Vector + Payload |
| Java SDK | 官方支持 | 社区 | 官方支持 |
| 适用场景 | 企业级知识库 | 快速原型 | 中等规模生产 |

> 💡 Milvus 是企业级首选，Chroma 适合开发调试，Qdrant 是前两者的折中方案。

### 9. 什么是 Self-RAG？什么是 Corrective RAG？
| 类型 | 核心思想 | 特点 |
|------|----------|------|
| **Self-RAG** | LLM 自我反思检索结果相关性，决定是否需要重新检索 | 内省机制，自我纠正 |
| **Corrective RAG** | 独立模块评估检索质量，触发"纠正-重新检索-过滤"流程 | 外挂评估器+纠错机制 |
| **GraphRAG** | 基于知识图谱组织文档，利用图结构增强检索 | 实体关系推理，全局视角 |
| **Agentic RAG** | Agent 自主决策调用哪些工具、何时检索、如何融合 | 高度灵活，适合复杂任务 |

### 10. 稠密向量检索的相似度计算方法有哪些？
| 方法 | 公式 | 适用范围 | 特点 |
|------|------|----------|------|
| 余弦相似度 | cos(A,B) = A·B / (|A|×|B|) | 通用语义匹配 | 无量纲，方向敏感 |
| 欧氏距离 | d(A,B) = sqrt(Σ(Ai-Bi)²) | L2 归一化后的向量 | 对向量模长敏感 |
| 点积 | A·B = Σ(Ai×Bi) | 已归一化嵌入 | 等价于余弦相似度 |
| 曼哈顿距离 | d = Σ|Ai-Bi| | 高维稀疏向量 | 计算简单，不常用 |

### 11. 什么是 ANN 索引？常见的 ANN 算法有哪些？
> 近似最近邻搜索（Approximate Nearest Neighbor, ANN）牺牲少量精度换取极速检索，是向量数据库的核心技术。

| 算法 | 原理 | Milvus 实现 | 适用场景 |
|------|------|-------------|----------|
| **IVF** | 聚类倒排 + 聚类内搜索 | IVF_FLAT, IVF_PQ, IVF_SQ8 | 百万级，均衡方案 |
| **HNSW** | 分层可导航小世界图 | HNSW | 千万级，速度快，精度高 |
| **DiskANN** | 基于 SSD 的图索引 | DiskANN | 亿级，低成本存储 |
| **FLAT** | 暴力搜索（精确 KNN） | FLAT | 小数据量高精度 |

> 🎯 面试必问：HNSW 参数 efConstruction（构建质量）、efSearch（搜索范围）、M（邻居数）。

### 12. 什么是 Embedding 模型？主流模型有哪些？
> Embedding 模型将文本映射到高维向量空间，语义相似的文本距离更近。

| 模型 | 维度 | 最大 Token | 特点 | 语言 |
|------|------|-----------|------|------|
| **BGE-Large** (BAAI) | 1024 | 512 | 国产开源最强，中文友好 | 中英双语 |
| **M3E** (Moka) | 768 | 8192 | 长文本支持好 | 中文为主 |
| **text-embedding-3-small** | 1536 | 8191 | OpenAI 闭源，通用性强 | 多语言 |
| **text-embedding-3-large** | 3072 | 8191 | 高精度 | 多语言 |
| **E5-mistral-7b-instruct** | 4096 | 4096 | 大参数量，精度高 | 英文为主 |

### 13. 什么是 BM25？BM25 的原理是什么？
> BM25（Best Matching 25）是关键词检索的经典算法，基于 TF-IDF 改进，引入文档长度归一化。

```python
# BM25 核心公式
# Score(D,Q) = Σ IDF(qi) × TF(qi, D) × (k1 + 1) / (TF + k1 × (1 - b + b × |D|/avgdl))
# k1=1.5(词频饱和度), b=0.75(长度惩罚系数)
```

> BM25 的优点是精确关键词匹配、无需 GPU，缺点是无法处理同义词和语义改写。

### 14. RAG 评估指标有哪些？什么是 RAGAS？
> RAGAS（Retrieval Augmented Generation Assessment）是业界标准 RAG 评估框架。

| 评估维度 | 指标 | 说明 |
|----------|------|------|
| **检索质量** | MRR、Recall、Precision、NDCG | 检索结果的相关性 |
| **生成质量** | Faithfulness、Answer Relevance | 回答忠实度和相关性 |
| **端到端** | Context Precision、Context Recall | 上下文信息质量 |
| **工具** | RAGAS、TruLens、DeepEval、MTEB | 自动化评估框架 |

```python
# RAGAS 评估示例
from ragas import evaluate
from ragas.metrics import faithfulness, answer_relevancy, context_recall, context_precision

result = evaluate(
    dataset=eval_dataset,  # 包含 question, answer, contexts, ground_truth
    metrics=[
        faithfulness,           # 回答是否忠实于检索到的上下文
        answer_relevancy,        # 回答与问题的相关性
        context_recall,          # 检索上下文是否覆盖了真实答案
        context_precision,       # 检索上下文中相关信息的密度
    ]
)
```

### 15. 什么是 HyDE（假设文档嵌入）？
> HyDE（Hypothetical Document Embedding）流程：先用 LLM 基于 Query 生成一个"假设答案"，再用假设答案进行向量检索。因为假设答案包含完整的语义信息，比原始 Query 更容易匹配到相似文档。

```python
# HyDE 核心逻辑
def hyde_retrieve(query: str, llm, retriever, k: int = 5):
    # 1. 先生成一个假设文档
    prompt = f"请根据问题'{query}'生成一段假设性的回答文档："
    hypothetical_doc = llm.invoke(prompt)

    # 2. 用假设文档去检索
    results = retriever.search(hypothetical_doc, k=k)
    return results
```

### 16. 什么是句子窗口检索（Sentence Window Retrieval）？
> 检索时只匹配句子级别，但返回时提供窗口上下文（前后若干句子），兼顾检索精度和上下文丰富度。

```
检索粒度：    句子（精准命中）
返回粒度：    句子 + 前后 N 句窗口（完整上下文）
LLM 输入：    窗口内的完整段落
```

### 17. 什么是层次索引（Hierarchical Indexing）？
> 构建两层索引：文档级摘要索引（检索粗筛）+ 块级索引（精确定位）。检索时先在摘要层找到相关文档，再在文档内定位具体内容。

```
Level 1: [文档摘要索引]  → 快速找到相关文档
Level 2: [文档内块索引]  → 精确定位相关内容
```

### 18. 什么是 Modular RAG 的条件模式、分支模式？
| 模式 | 说明 | 适用场景 |
|------|------|----------|
| **顺序模式** | 按固定流程依次执行各模块 | 标准问答流程 |
| **条件模式** | 根据条件判断执行不同分支 | 查天气 vs 查知识库 |
| **分支模式** | 并行执行多个检索路径后合并 | 多源知识库检索 |
| **迭代模式** | 循环检索直到满足条件 | 多轮澄清式问答 |
| **递归模式** | 逐层深入子问题检索 | 复杂问题分解 |

---

## 二、深度原理剖析（12题）

### 1. Embedding 原理：word2vec 与 CBOW 详解
> Word2Vec 是 Embedding 的奠基性工作，包含 CBOW 和 Skip-gram 两种架构。

**CBOW（Continuous Bag of Words）**：根据上下文词预测中心词。
```
输入：[w(t-2), w(t-1), w(t+1), w(t+2)] → 投影层(求和平均) → 输出: w(t)
```

| 对比 | CBOW | Skip-gram |
|------|------|-----------|
| 预测方向 | 上下文→中心词 | 中心词→上下文 |
| 训练速度 | 快 | 慢 |
| 低频词效果 | 一般 | 好 |
| 适用场景 | 大语料、高频词 | 小语料、低频词 |

> 理解 word2vec 对理解当前 Transformer-based embedding 模型（BERT、BGE）的基础原理至关重要。

### 2. Cross-Encoder vs Bi-Encoder 深度对比
> Bi-Encoder 是 RAG 初检的首选，Cross-Encoder 是 Rerank 的核心。

**Bi-Encoder**：
```python
# Query 和 Document 分别编码
query_vec = encoder(query)   # [1, 768]
doc_vec   = encoder(doc)     # [1, 768]
score     = cosine_similarity(query_vec, doc_vec)
# 可预计算 doc_vec，推理快，适合大规模检索
```

**Cross-Encoder**：
```python
# Query 和 Document 拼接编码
pair = f"[CLS]{query}[SEP]{doc}[SEP]"
score = classifier(encoder(pair))  # [1, 1] 直接输出相似度分数
# 不可预计算，推理慢，但精度远高于 Bi-Encoder
```

> 🎯 面试回答："Bi-Encoder 用于第一阶段的候选召回（Top-100→Top-20），Cross-Encoder 用于第二阶段的精准排序（Top-20→Top-5），两者构成经典的检索→精排漏斗。"

### 3. 向量数据库如何实现相似性搜索？K-means 与肘部法则
> K-means 是向量数据库中 IVF（Inverted File）索引的基础。

```python
import numpy as np
from sklearn.cluster import KMeans

# 使用肘部法则确定 K 值
def find_optimal_k(vectors: np.ndarray, max_k: int = 50):
    inertias = []
    for k in range(1, max_k + 1):
        kmeans = KMeans(n_clusters=k, random_state=42, n_init=10)
        kmeans.fit(vectors)
        inertias.append(kmeans.inertia_)

    # 肘部法则：选择 inertia 下降速度减缓的点
    # 常用于 IVF 索引的 nlist 参数确定
    return optimal_k

# IVF 索引流程：粗量化（聚类）→ 倒排 → 检索时只在最近聚类搜索
```

> IVF 索引参数：nlist（聚类中心数，越大检索越慢但精度越高），nprobe（搜索时访问的聚类数）。

### 4. BGE-Large Embedding 模型详解
> BGE（BAAI General Embedding）是北京智源研究院开源的通用嵌入模型。

| 版本 | 维度 | 特点 |
|------|------|------|
| BGE-Small | 384 | 轻量快速 |
| BGE-Base | 768 | 精度与速度均衡 |
| BGE-Large | 1024 | 高精度，中文SOTA |
| BGE-M3 | 1024 | 多语言+多粒度+多函数 |

```python
# BGE-Large 使用示例
from sentence_transformers import SentenceTransformer

model = SentenceTransformer("BAAI/bge-large-zh-v1.5")

# BGE 需要给 query 加前缀
query = model.encode("为这个句子生成向量：企业知识库管理系统", normalize_embeddings=True)
docs = model.encode(["文档1内容", "文档2内容"], normalize_embeddings=True)

# 余弦相似度
similarity = query @ docs.T
```

> 💡 BGE 要求 Query 前加 "为这个句子生成向量：" 前缀，文档不需要，这是提升精度的关键细节。

### 5. Embedding 模型微调：LlamaIndex 实践
> 使用领域数据微调 Embedding 模型，可显著提升 RAG 在特定领域的召回率。

```python
# 使用 LlamaIndex 微调 BGE 模型
from llama_index.finetuning import EmbeddingAdapterFinetuneEngine
from llama_index.core.evaluation import EmbeddingQAFinetuneDataset

# 1. 准备训练数据（query, relevant_docs 对）
train_dataset = EmbeddingQAFinetuneDataset(
    queries={"q1": "什么是RAG?", "q2": "Milvus如何部署"},
    corpus={"d1": "RAG是检索增强生成...", "d2": "Milvus部署需要Docker..."},
    relevant_docs={"q1": ["d1"], "q2": ["d2"]}
)

# 2. 微调
finetune_engine = EmbeddingAdapterFinetuneEngine(
    train_dataset,
    model_id="BAAI/bge-base-zh-v1.5",
    epochs=3,
    batch_size=16,
)
finetune_engine.finetune()

# 3. 保存微调模型
finetune_engine.save_model("./fine_tuned_bge")
```

> 关键参数：epochs(3-5)、learning_rate(1e-5 ~ 5e-5)、batch_size(16-32)。小样本（100-500 对）即可见到显著效果。

### 6. RAG 评估指标详解：MRR、NDCG、Hit Rate
| 指标 | 全称 | 计算方式 | 取值范围 | 说明 |
|------|------|----------|----------|------|
| **MRR** | Mean Reciprocal Rank | 1/N × Σ(1/rank_i) | [0, 1] | 评估第一个正确答案的排名 |
| **NDCG** | Normalized Discounted Cumulative Gain | DCG / IDCG | [0, 1] | 考虑排序位置的增益衰减 |
| **Hit Rate** | Hit Rate @ K | 命中数 / 总查询数 | [0, 1] | Top-K 中是否有相关结果 |
| **Recall@K** | Recall @ K | 命中相关数 / 总相关数 | [0, 1] | Top-K 召回覆盖率 |

```python
# MRR 计算示例
def mrr(relevant_docs: list[list[int]], retrieved_docs: list[list[int]]):
    """relevant_docs: 每个query的相关文档索引列表
       retrieved_docs: 每个query检索到的文档索引列表"""
    total = 0
    for rel, ret in zip(relevant_docs, retrieved_docs):
        for rank, doc_id in enumerate(ret, 1):
            if doc_id in rel:
                total += 1.0 / rank
                break
    return total / len(relevant_docs)
```

### 7. MTEB 排行榜和 Embedding 模型评估
> MTEB（Massive Text Embedding Benchmark）是当前最权威的 Embedding 评估标准，涵盖 8 大任务 58 个数据集。

| 评估任务 | 说明 | 指标 |
|----------|------|------|
| 分类（Classification） | 文本分类准确率 | Accuracy |
| 聚类（Clustering） | 文本聚类质量 | V-Measure |
| 配对分类（Pair Classification） | 句子对相似度 | AP, Spearman |
| 重排序（Reranking） | 检索结果重排 | MAP, MRR |
| 检索（Retrieval） | 信息检索 | NDCG@10, MRR |
| 语义相似度（STS） | 语义文本相似度 | Spearman |
| 摘要（Summarization） | 摘要相关性 | Spearman |

> 中文场景重点关注：MTEB-Chinese 排行中 BGE 系列、M3E、Stella 等模型的表现。

### 8. 什么是 RankGPT？与传统 Rerank 的区别？
> RankGPT 利用 LLM 的排序能力对候选文档进行重排序，属于 Listwise Rerank（列表级），而传统 Cross-Encoder 是 Pointwise（逐点打分）。

| 方法 | 类型 | 延迟 | 精度 | 成本 |
|------|------|------|------|------|
| BGE-Reranker | Pointwise | 低 | 高 | 低（可本地部署） |
| RankGPT | Listwise | 高 | 更高 | 高（需调用 GPT） |
| cohere Rerank | Pointwise | 中 | 高 | 中（API 付费） |

```python
# RankGPT 排序提示词示例
prompt = """
请将以下文档根据与问题的相关性排序（从最相关到最不相关）。
问题：{query}
文档：{documents}
输出排序后的文档索引，用逗号分隔："""
```

> 💡 生产环境优先选择 Cross-Encoder Reranker（如 bge-reranker-v2-m3），RankGPT 可作为效果标杆但延迟过高。

### 9. 什么是 Prompt 压缩？为什么需要？
> Prompt 压缩（如 LLMLingua、LongLLMLingua）减少注入 LLM 的检索上下文体积，保留关键信息。

| 方法 | 压缩比 | 精度损失 | 速度 |
|------|--------|----------|------|
| LLMLingua | 2-5x | 小 | 快 |
| LongLLMLingua | 3-10x | 中 | 中 |
| Selective Context | 2-3x | 极小 | 快 |

> 适用场景：长文档检索导致 Context 超长、Token 成本高、LLM 在处理超长 Context 时"迷失在中间"。

### 10. 什么是 FLARE（主动检索增强生成）？
> FLARE（Forward-Looking Active Retrieval Augmented Generation）在生成过程中主动判断是否需要检索，是一种迭代式 RAG 方法。

```python
# FLARE 的核心逻辑
def flare_generate(query, llm, retriever, max_steps=5):
    current_text = ""
    for step in range(max_steps):
        # 1. 预生成下一句
        next_sentence = llm.predict_next_sentence(current_text)

        # 2. 判断是否需要检索
        if contains_low_confidence_tokens(next_sentence):
            # 3. 用预生成结果检索
            context = retriever.search(next_sentence, k=3)
            # 4. 重新生成更可靠的句子
            next_sentence = llm.generate_with_context(current_text, context)

        # 5. 追加结果
        current_text += next_sentence
    return current_text
```

### 11. 什么是 Fusion（融合检索）？有什么常用方法？
> Fusion 指将多个检索通道的结果合并为一个有序列表。

| 方法 | 原理 | 特点 |
|------|------|------|
| **RRF**（Reciprocal Rank Fusion） | 集成排序：score = Σ 1/(k + rank) | 无需分数标准化，鲁棒性好 |
| **CC**（Combined Sum） | 分数加权求和 | 需分数归一化，调参敏感 |
| **DBSF**（Distribution-Based Score Fusion） | 基于分数分布融合 | 适合异构检索器 |

```python
def reciprocal_rank_fusion(results: list[list[str]], k: int = 60, top_k: int = 10):
    """RRF 融合多个检索结果"""
    scores = {}
    for rank_list in results:
        for rank, doc_id in enumerate(rank_list, 1):
            scores[doc_id] = scores.get(doc_id, 0) + 1.0 / (k + rank)
    # 按分数降序排列
    sorted_docs = sorted(scores.items(), key=lambda x: x[1], reverse=True)
    return [doc for doc, _ in sorted_docs[:top_k]]
```

### 12. 向量数据库的相似性搜索算法原理
> 向量搜索算法分为精确搜索和近似搜索两大类。

```text
精确搜索 (KNN)
  - 暴力计算所有向量距离 → O(n×d) 复杂度
  - 适用：数据量 < 1万

近似搜索 (ANN)
  - 空间划分：KD-Tree, R-Tree
  - 量化压缩：PQ (Product Quantization), SQ (Scalar Quantization)
  - 图结构：HNSW (Hierarchical Navigable Small World)
  - 哈希：LSH (Locality Sensitive Hashing)
  - 适用：数据量 > 10万

Milvus 默认索引：
  - CPU: IVF_FLAT（平衡速度与精度）
  - GPU: IVF_PQ（高吞吐）
  - 超高维度: HNSW（低延迟高精度）
```

> 🎯 面试重点："HNSW 为什么快？" 多层图的顶层稀疏连接（快速定位区域），底层密集连接（精确搜索），搜索时从顶层逐层下降。

---

## 三、实战场景题（10题）

### 1. 你的 PDF 解析方案是什么？如何处理复杂表格和图片？
> PDF 解析是 RAG 知识库的第一个技术难点。

| 工具 | 优势 | 劣势 | 适用场景 |
|------|------|------|----------|
| PyMuPDF (fitz) | 轻量、速度快 | 表/图提取弱 | 纯文本 PDF |
| Unstructured | 支持表格、图片提取 | 需服务器部署 | 复杂格式 PDF |
| Markdown 解析 | 保留文档结构 | 只支持 MD 源文件 | 技术文档仓库 |
| LlamaParse | 专为 RAG 优化，支持多模态 | 云服务、付费 | 企业级高精度 |

```python
# 使用 Unstructured 解析 PDF
from unstructured.partition.pdf import partition_pdf

elements = partition_pdf(
    "document.pdf",
    strategy="hi_res",     # hi_res / fast / auto
    infer_table_structure=True,
    extract_images_in_pdf=True,
)

# 按类型处理
for element in elements:
    if element.category == "Title":
        pass  # 处理标题，可用于 Chunk 合并
    elif element.category == "Table":
        pass  # 表格转 Markdown 格式
    elif element.category == "Figure":
        pass  # 图片描述提取或多模态处理
```

> 💡 表格处理最佳实践：提取表格后转为 Markdown 格式，保留行列结构，再将其作为独立的 Document 存储。

### 2. 如何设计知识库的增量更新策略？
> 企业知识库需要持续更新，不能每次都全量重建。

| 策略 | 实现方式 | 适用场景 |
|------|----------|----------|
| 全量重建 | 删除旧索引，重新 Embedding + 写入 | 模型升级、重大变更 |
| 增量插入 | 新文档直接插入，对旧文档无影响 | 日常新增文档 |
| 增量更新 | 修改文档时，删除旧向量 + 重写新向量 | 文档版本更新 |
| 定时同步 | 定时扫描源目录，执行增量更新 | 文件系统对接 |

```python
# Milvus 增量插入示例
from pymilvus import Collection

collection = Collection("knowledge_base")
collection.load()

# 增量插入新的文档向量
collection.insert([
    [new_ids],
    [new_vectors],
    [new_chunks],
    [new_metadata],
])
```

### 3. 如何调优 Chunking 参数（chunk_size, overlap, separator）？
> Chunking 参数直接影响检索精度，需要根据文档类型和业务场景调优。

```python
# Chunking 参数调优实验框架
import numpy as np
from ragas.metrics import context_recall

def tune_chunking(documents, chunk_sizes, overlaps, separators):
    best_config = None
    best_score = 0

    for size in chunk_sizes:
        for overlap in overlaps:
            for sep in separators:
                # 1. 用当前配置分割文档
                chunks = RecursiveCharacterTextSplitter(
                    chunk_size=size,
                    chunk_overlap=overlap,
                    separators=sep
                ).split_documents(documents)

                # 2. 构建索引并检索
                store = build_vector_store(chunks)
                results = retrieval_test(store)

                # 3. 评估检索效果
                score = evaluate_context_recall(results)
                if score > best_score:
                    best_score = score
                    best_config = (size, overlap, sep)

    return best_config

# 推荐搜索范围
chunk_sizes = [256, 384, 512, 768, 1024]
overlaps   = [0, 50, 100, 128]
```

| 参数 | 推荐范围 | 调优方向 |
|------|----------|----------|
| chunk_size | 256-1024 tokens | 大 → 上下文丰富但精度降；小 → 精度高但上下文不足 |
| chunk_overlap | 10%-20% 的 chunk_size | 越大 → 信息冗余但完整；越小 → 节省 Token |
| separators | ["\n\n", "\n", "。", "，"] | 按文档结构层级配置 |

### 4. 如何降低 RAG 系统的幻觉？
> RAG 虽然能显著降低幻觉，但无法完全消除。

| 方法 | 原理 | 效果 |
|------|------|------|
| 阈值过滤 | 检索相似度低于阈值时拒绝回答 | 降低错误回答率 |
| Prompt 约束 | "仅基于检索内容回答，不知道就说不知道" | 减少模型编造 |
| 多通道检索 | 不同策略检索结果交叉验证 | 提高信息可信度 |
| 答案溯源 | 返回结果时附带引用段落 | 用户可验证 |
| 知识库审核 | 定期清理过时/错误文档 | 源头治理 |
| 二次校验 | LLM 自我检查回答是否基于上下文 | Self-RAG 方式 |

```python
# Prompt 约束示例
rag_prompt = """请基于以下检索到的文档内容回答问题。
如果你在文档中找不到相关信息，请回答"抱歉，我在知识库中未找到相关信息"。

【文档内容】
{context}

【问题】
{query}

【要求】
1. 仅使用文档内容回答
2. 如果文档内容不足以回答，明确说明
3. 引用具体文档来源"""
```

### 5. 如何处理超长文档（如几百页的 PDF）？
> 超长文档需要分层处理和特殊的分块策略。

1. **文档级摘要**：为每个文档生成摘要并索引
2. **分层 Chunking**：先分章节 → 再分段落 → 最后分块
3. **滑动窗口**：检索时命中任意块，返回窗口范围的上下文
4. **HyDE 增强**：先生成假设文档再检索
5. **表格特殊处理**：独立提取表格，作为独立 Document 存储

```python
def process_long_document(pdf_path: str, chunk_size: int = 512):
    # 1. 解析为结构化元素
    elements = partition_pdf(pdf_path, strategy="hi_res")

    # 2. 按标题合并为段落
    merged_chunks = merge_by_title(elements)

    # 3. 对超长段落二次分割
    final_chunks = []
    for chunk in merged_chunks:
        if len(chunk) > chunk_size:
            sub_chunks = split_with_overlap(chunk, chunk_size, overlap=50)
            final_chunks.extend(sub_chunks)
        else:
            final_chunks.append(chunk)
    return final_chunks
```

### 6. 如何选择 Top-K 的值？
> Top-K 是检索精度的关键参数，不同场景需要不同的 K 值。

| 场景 | K 值 | 原因 |
|------|------|------|
| 简单事实问答 | K=3 | 信息量小，足以确认答案 |
| 复杂推理问答 | K=5~8 | 需要多角度信息交叉验证 |
| 摘要/报告生成 | K=10~20 | 需要广泛信息覆盖 |
| 代码/术语检索 | K=3~5 | 精确匹配为主 |

> 💡 生产环境推荐：初检 K=20（粗筛），Rerank 后取 Top-5 送入 LLM（精排）。

### 7. 知识库问答系统中，用户查询意图不明确怎么办？
> 用户提问可能模糊或缺少上下文，需要多轮交互或查询重写。

| 方案 | 实现方式 | 场景 |
|------|----------|------|
| **查询重写** | LLM 将模糊问题改写为检索友好形式 | "那个系统怎么用" → "XX管理系统的用户手册" |
| **多轮上下**问 | 结合对话历史理解当前 Query | 连续提问场景 |
| **子查询分解** | 复杂问题拆解为多个子查询 | "A和B的区别是什么" |
| **消歧反问** | 主动向用户确认意图 | "您指的是哪个系统？" |

```python
# 查询重写
def rewrite_query(history: list[dict], current_query: str, llm) -> str:
    prompt = f"""基于对话历史，将用户的最新问题改写成适合检索的独立问题。
对话历史：
{history}
用户最新问题：{current_query}
改写后的独立检索问题："""
    return llm.invoke(prompt)
```

### 8. 如何评估 RAG 系统的检索质量？
> 需要构建标注数据集，从多个维度评估。

```python
from ragas.metrics import (
    faithfulness, answer_relevancy,
    context_recall, context_precision
)
from datasets import Dataset

# 构建评估数据集
eval_data = {
    "question": ["RAG的完整流程是什么？", "Milvus如何部署？"],
    "answer": ["RAG包含检索和生成...", "Milvus支持Docker部署..."],
    "contexts": [
        ["RAG是检索增强生成..."],
        ["Milvus部署文档..."]
    ],
    "ground_truth": [  # 标准答案
        "RAG的完整流程包括...",
        "Milvus使用Docker Compose部署..."
    ]
}
dataset = Dataset.from_dict(eval_data)

# 运行评估
result = evaluate(
    dataset=dataset,
    metrics=[faithfulness, answer_relevancy, context_recall, context_precision]
)
print(result)
```

### 9. 你的双路检索 alpha 权重如何确定？
> 稠密检索权重 alpha 的确定需要实验调优。

| 方法 | 说明 | 适用场景 |
|------|------|----------|
| 固定权重 | alpha=0.7（语义为主） | 通用场景 |
| 实验调优 | 搜索空间 [0.1, 0.9] 步长 0.1，取最优 | 领域特定 |
| 自适应权重 | 根据 Query 类型动态调整 | 混合类型查询 |
| 集成方法 | 使用 RRF 替代加权求和 | 无需调参 |

```python
def tune_alpha(dense_retriever, sparse_retriever, eval_queries, eval_docs):
    best_alpha, best_mrr = 0.5, 0
    for alpha in [x / 10 for x in range(1, 10)]:
        hybrid = DualChannelRetriever(dense_retriever, sparse_retriever, alpha)
        mrr = evaluate_mrr(hybrid, eval_queries, eval_docs)
        if mrr > best_mrr:
            best_mrr = mrr
            best_alpha = alpha
    return best_alpha, best_mrr
```

### 10. 如何处理 RAG 系统中的权限和数据安全？
> 企业知识库必须考虑不同用户的数据访问权限。

| 方案 | 实现 | 复杂度 |
|------|------|--------|
| **文档级权限** | 按文档元数据过滤搜索结果 | 低 |
| **字段级权限** | 同一文档不同用户看到不同字段 | 中 |
| **内容动态脱敏** | LLM 生成后脱敏关键信息 | 高 |
| **知识库分库** | 按权限级别拆分多个知识库 | 中 |

```python
# Milvus 标量过滤实现权限控制
from pymilvus import Collection

collection = Collection("knowledge_base")
collection.load()

# 只检索用户有权限的文档
search_params = {
    "metric_type": "IP",
    "offset": 0,
    "ignore_growing": False,
    "params": {"nprobe": 10}
}
results = collection.search(
    data=[query_vector],
    anns_field="vector",
    param=search_params,
    limit=5,
    expr=f"permission_level <= {user_permission}",  # 标量过滤
    output_fields=["content", "source"]
)
```

---

## 四、手写代码题（8题）

### 1. 手写完整 RAG Pipeline
> 面试常考：从数据加载到生成回答的完整端到端链路。

```python
import numpy as np
from typing import List, Dict, Any

class RAGPipeline:
    """端到端 RAG Pipeline"""

    def __init__(self, embedding_model, llm, vector_store, retriever):
        self.embedding_model = embedding_model
        self.llm = llm
        self.vector_store = vector_store
        self.retriever = retriever

    def ingest(self, documents: List[str], metadata: List[Dict] = None):
        """数据摄入：分割 → Embedding → 存储"""
        chunks = []
        for i, doc in enumerate(documents):
            doc_chunks = self._chunk_document(doc)
            chunks.extend(doc_chunks)

        # 向量化
        vectors = self.embedding_model.encode(chunks, normalize_embeddings=True)

        # 存储到向量库
        self.vector_store.add(vectors, chunks, metadata or [{}] * len(chunks))

    def query(self, question: str, k: int = 5) -> Dict[str, Any]:
        """检索 + 生成"""
        # 1. Query 向量化
        query_vec = self.embedding_model.encode(
            [question], normalize_embeddings=True
        )[0]

        # 2. 检索 Top-K
        results = self.retriever.search(query_vec, k=k * 2)

        # 3. Rerank（可选）
        reranked = self._rerank(question, results, top_k=k)

        # 4. 构建 Prompt
        context = "\n\n".join([r["content"] for r in reranked])
        prompt = f"""基于以下文档回答问题：
{context}

问题：{question}
回答："""

        # 5. LLM 生成
        answer = self.llm.invoke(prompt)

        return {
            "answer": answer,
            "sources": reranked,
            "context": context
        }

    def _chunk_document(self, doc: str, chunk_size: int = 512, overlap: int = 50) -> List[str]:
        """递归字符分割"""
        if len(doc) <= chunk_size:
            return [doc]
        chunks = []
        start = 0
        while start < len(doc):
            end = start + chunk_size
            chunks.append(doc[start:end])
            start = end - overlap
        return chunks

    def _rerank(self, query: str, results: List[Dict], top_k: int) -> List[Dict]:
        """简单的 Cross-Encoder Rerank"""
        # 实际项目中替换为 bge-reranker-v2-m3
        scores = [self._compute_relevance(query, r["content"]) for r in results]
        ranked = sorted(zip(results, scores), key=lambda x: x[1], reverse=True)
        return [r for r, _ in ranked[:top_k]]

    def _compute_relevance(self, query: str, doc: str) -> float:
        """计算 query 和文档的相关性分数"""
        q_vec = self.embedding_model.encode([query], normalize_embeddings=True)[0]
        d_vec = self.embedding_model.encode([doc], normalize_embeddings=True)[0]
        return float(q_vec @ d_vec)
```

### 2. 手写 BM25 检索器

```python
import math
from collections import Counter
from typing import List, Tuple

class BM25Retriever:
    """BM25 检索器实现"""

    def __init__(self, k1: float = 1.5, b: float = 0.75):
        self.k1 = k1
        self.b = b
        self.corpus: List[str] = []
        self.doc_freqs: List[Counter] = []
        self.idf: dict = {}
        self.avgdl: float = 0.0
        self.doc_count: int = 0

    def fit(self, corpus: List[str]):
        """训练 BM25 模型"""
        self.corpus = corpus
        self.doc_count = len(corpus)
        total_terms = 0

        for doc in corpus:
            terms = self._tokenize(doc)
            self.doc_freqs.append(Counter(terms))
            total_terms += len(terms)

        self.avgdl = total_terms / self.doc_count if self.doc_count > 0 else 0

        # 计算 IDF
        df = Counter()
        for doc_freq in self.doc_freqs:
            df.update(doc_freq.keys())

        self.idf = {
            term: math.log(1 + (self.doc_count - freq + 0.5) / (freq + 0.5))
            for term, freq in df.items()
        }

    def search(self, query: str, top_k: int = 5) -> List[Tuple[str, float]]:
        """检索 Top-K 最相关文档"""
        query_terms = self._tokenize(query)
        scores = []

        for i, doc_freq in enumerate(self.doc_freqs):
            score = 0
            doc_len = sum(doc_freq.values())

            for term in query_terms:
                if term in self.idf:
                    tf = doc_freq.get(term, 0)
                    numerator = tf * (self.k1 + 1)
                    denominator = tf + self.k1 * (1 - self.b + self.b * doc_len / self.avgdl)
                    score += self.idf[term] * numerator / denominator

            scores.append(score)

        # 取 Top-K
        top_indices = sorted(
            range(len(scores)), key=lambda i: scores[i], reverse=True
        )[:top_k]

        return [(self.corpus[i], scores[i]) for i in top_indices if scores[i] > 0]

    def _tokenize(self, text: str) -> List[str]:
        """中文分词（简单实现，生产中替换为 jieba）"""
        try:
            import jieba
            return list(jieba.cut(text))
        except ImportError:
            return list(text)
```

### 3. 手写 HNSW 索引的核心逻辑

```python
import numpy as np
from typing import List, Tuple, Set

class HNSWIndex:
    """简化的 HNSW 索引实现（面试展示核心思想）"""

    def __init__(self, dim: int, M: int = 16, ef_construction: int = 200):
        self.dim = dim
        self.M = M                    # 每层最大邻居数
        self.M_max = M * 2            # 上层最大邻居数
        self.ef_construction = ef_construction
        self.vectors: List[np.ndarray] = []
        self.graphs: List[List[Set[int]]] = []  # 每层的邻接图
        self.levels: List[int] = []    # 每个节点的层数
        self.enter_point: int = -1     # 入口节点

    def add(self, vector: np.ndarray) -> int:
        """添加向量到索引"""
        idx = len(self.vectors)
        self.vectors.append(vector)

        # 随机决定层数（指数衰减）
        level = 0
        while np.random.random() < 0.5 and level < 10:
            level += 1

        self.levels.append(level)

        # 扩展图结构
        while len(self.graphs) <= level:
            self.graphs.append([])
        for l in range(level + 1):
            while len(self.graphs[l]) <= idx:
                self.graphs[l].append(set())

        if self.enter_point == -1:
            self.enter_point = idx
            return idx

        # 从顶层搜索到 level+1 层
        curr = self.enter_point
        for l in range(len(self.graphs) - 1, level, -1):
            curr = self._search_layer(vector, curr, 1, l)[0][0]

        # 在 level 到 0 层插入
        for l in range(min(level, len(self.graphs) - 1), -1, -1):
            neighbors = self._search_layer(vector, curr, self.ef_construction, l)
            # 选择 M 个最近邻
            selected = self._select_neighbors(neighbors, self.M)
            self.graphs[l][idx] = set(n for n, _ in selected)

            # 双向连接
            for neighbor, _ in selected:
                self.graphs[l][neighbor].add(idx)
                # 如果邻居边数超限，裁剪
                if len(self.graphs[l][neighbor]) > self.M_max:
                    self._shrink_neighbors(l, neighbor)

            curr = neighbors[0][0]

        if level > len(self.graphs) - 1:
            self.enter_point = idx

        return idx

    def search(self, query: np.ndarray, k: int = 10) -> List[Tuple[int, float]]:
        """搜索 Top-K 最近邻"""
        if self.enter_point == -1:
            return []

        curr = self.enter_point
        # 从最高层搜索到第 0 层
        for l in range(len(self.graphs) - 1, 0, -1):
            curr = self._search_layer(query, curr, 1, l)[0][0]

        # 在第 0 层搜索 Top-K
        return self._search_layer(query, curr, k, 0)

    def _search_layer(self, query: np.ndarray, entry: int, ef: int, level: int) -> List[Tuple[int, float]]:
        """单层搜索"""
        visited = {entry}
        candidates = {entry}
        results = [(entry, self._distance(query, self.vectors[entry]))]
        results.sort(key=lambda x: x[1])

        while candidates:
            # 找 candidates 中最近的点
            nearest = min(candidates, key=lambda c: self._distance(query, self.vectors[c]))
            candidates.remove(nearest)

            # 如果最远的已有结果比最近的 candidate 还近，停止
            if len(results) >= ef and self._distance(query, self.vectors[nearest]) > results[-1][1]:
                break

            for neighbor in self.graphs[level][nearest]:
                if neighbor not in visited:
                    visited.add(neighbor)
                    dist = self._distance(query, self.vectors[neighbor])

                    if len(results) < ef or dist < results[-1][1]:
                        results.append((neighbor, dist))
                        results.sort(key=lambda x: x[1])
                        results = results[:ef]
                        candidates.add(neighbor)

        return results[:ef]

    def _distance(self, a: np.ndarray, b: np.ndarray) -> float:
        return np.linalg.norm(a - b)

    def _select_neighbors(self, candidates: List[Tuple[int, float]], M: int) -> List[Tuple[int, float]]:
        return sorted(candidates, key=lambda x: x[1])[:M]

    def _shrink_neighbors(self, level: int, idx: int):
        neighbors = list(self.graphs[level][idx])
        if len(neighbors) <= self.M_max:
            return
        # keep M closest
        sorted_neighbors = sorted(
            neighbors,
            key=lambda n: self._distance(self.vectors[idx], self.vectors[n])
        )
        self.graphs[level][idx] = set(sorted_neighbors[:self.M])
```

### 4. 手写 RRF（Reciprocal Rank Fusion）融合检索

```python
def rrf_fusion(
    result_lists: List[List[Dict[str, Any]]],
    k: int = 60,
    top_k: int = 10
) -> List[Dict[str, Any]]:
    """对多个检索结果执行 RRF 融合排序"""
    scores = {}

    for rank_list in result_lists:
        for rank, item in enumerate(rank_list, 1):
            doc_id = item.get("id", item.get("content"))
            # RRF 核心公式: score += 1/(k + rank)
            scores[doc_id] = scores.get(doc_id, 0.0) + 1.0 / (k + rank)

    # 按融合后分数降序排列
    ranked = sorted(scores.items(), key=lambda x: x[1], reverse=True)

    # 构建返回结果
    doc_map = {}
    for rank_list in result_lists:
        for item in rank_list:
            doc_id = item.get("id", item.get("content"))
            doc_map[doc_id] = item

    return [doc_map[doc_id] for doc_id, _ in ranked[:top_k]]

# 使用示例
vector_results = dense_retriever.search("RAG是什么", k=20)
keyword_results = bm25_retriever.search("RAG是什么", k=20)
graph_results = graph_retriever.search("RAG是什么", k=20)

final_results = rrf_fusion([vector_results, keyword_results, graph_results], k=60, top_k=5)
```

### 5. 手写 Embedding 模型微调（对比学习）

```python
import torch
import torch.nn.functional as F
from torch.utils.data import DataLoader, Dataset
from transformers import AutoTokenizer, AutoModel

class ContrastiveLearningDataset(Dataset):
    """对比学习训练数据：每个样本包含(query, positive, negative)"""
    def __init__(self, queries, positives, negatives, tokenizer, max_len=128):
        self.queries = queries
        self.positives = positives
        self.negatives = negatives
        self.tokenizer = tokenizer
        self.max_len = max_len

    def __len__(self):
        return len(self.queries)

    def __getitem__(self, idx):
        q = self.tokenizer(
            self.queries[idx], padding="max_length",
            truncation=True, max_length=self.max_len,
            return_tensors="pt"
        )
        p = self.tokenizer(
            self.positives[idx], padding="max_length",
            truncation=True, max_length=self.max_len,
            return_tensors="pt"
        )
        n = self.tokenizer(
            self.negatives[idx], padding="max_length",
            truncation=True, max_length=self.max_len,
            return_tensors="pt"
        )
        return q, p, n

def contrastive_loss(query_emb, positive_emb, negative_emb, margin=0.3):
    """对比学习损失函数 (Triplet Loss)"""
    pos_distance = F.pairwise_distance(query_emb, positive_emb)
    neg_distance = F.pairwise_distance(query_emb, negative_emb)
    loss = torch.mean(F.relu(pos_distance - neg_distance + margin))
    return loss

def train_embedding_model(model, train_loader, epochs=3, lr=2e-5):
    """微调 Embedding 模型"""
    optimizer = torch.optim.AdamW(model.parameters(), lr=lr)

    for epoch in range(epochs):
        total_loss = 0
        for batch_q, batch_p, batch_n in train_loader:
            optimizer.zero_grad()

            def forward(inputs):
                outputs = model(**inputs)
                return F.normalize(outputs.last_hidden_state[:, 0, :], dim=-1)

            q_emb = forward(batch_q)
            p_emb = forward(batch_p)
            n_emb = forward(batch_n)

            loss = contrastive_loss(q_emb, p_emb, n_emb)
            loss.backward()
            optimizer.step()
            total_loss += loss.item()

        print(f"Epoch {epoch+1}, Loss: {total_loss / len(train_loader):.4f}")

    return model
```

### 6. 手写 Query 重写和子查询分解

```python
from typing import List

def query_rewrite(raw_query: str, history: List[str], llm) -> str:
    """将模糊查询改写为检索友好的独立 Query"""
    prompt = f"""你是一个 Query 改写专家。请将用户问题改写成适合向量检索的独立问题。

对话历史：
{chr(10).join(history) if history else "无"}

原始问题：{raw_query}

改写要求：
1. 如果涉及代词（它、这个、那个），替换为具体名词
2. 补充上下文信息使问题完整
3. 保持原始意图不变
4. 输出仅改写后的问题

改写结果："""
    return llm.invoke(prompt).strip()

def decompose_query(complex_query: str, llm) -> List[str]:
    """将复杂问题分解为多个子查询"""
    prompt = f"""请将以下复杂问题分解为多个原子子查询，每个子查询是一个独立的检索问题。

复杂问题：{complex_query}

要求：
- 每个子查询只问一个具体方面
- 子查询之间不重叠
- 用编号列出

示例：
输入：Java和Python在Web开发中的优缺点是什么？
输出：
1. Java在Web开发中的优势
2. Java在Web开发中的劣势
3. Python在Web开发中的优势
4. Python在Web开发中的劣势

分解结果："""
    result = llm.invoke(prompt)
    sub_queries = []
    for line in result.strip().split("\n"):
        line = line.strip()
        if line and (line[0].isdigit() or line.startswith("-")):
            sub_queries.append(line.split(". ", 1)[-1] if ". " in line else line[2:])
    return sub_queries
```

### 7. 手写 RAGAS 评估工具调用

```python
from datasets import Dataset
from ragas import evaluate
from ragas.metrics import (
    faithfulness,
    answer_relevancy,
    context_recall,
    context_precision
)
from ragas.llms import LangchainLLMWrapper
from ragas.embeddings import LangchainEmbeddingsWrapper

def evaluate_rag_pipeline(pipeline, test_questions: List[str], ground_truths: List[str]):
    """评估 RAG Pipeline 的检索和生成质量"""
    answers = []
    contexts = []

    for question in test_questions:
        result = pipeline.query(question)
        answers.append(result["answer"])
        contexts.append([r["content"] for r in result["sources"]])

    # 构建 HuggingFace Dataset
    eval_dataset = Dataset.from_dict({
        "question": test_questions,
        "answer": answers,
        "contexts": contexts,
        "ground_truth": ground_truths,
    })

    # 设置评估模型
    from langchain_openai import ChatOpenAI
    from langchain_openai import OpenAIEmbeddings

    evaluator_llm = LangchainLLMWrapper(ChatOpenAI(model="gpt-4"))
    evaluator_emb = LangchainEmbeddingsWrapper(OpenAIEmbeddings(model="text-embedding-3-small"))

    # 运行评估
    result = evaluate(
        dataset=eval_dataset,
        metrics=[
            faithfulness,
            answer_relevancy,
            context_recall,
            context_precision,
        ],
        llm=evaluator_llm,
        embeddings=evaluator_emb,
    )

    return result.to_pandas()

# 运行评估
# df = evaluate_rag_pipeline(my_rag, test_qs, ground_truths)
# print(df.describe())
```

### 8. 手写 Milvus 集合创建与管理

```python
from pymilvus import (
    connections, FieldSchema, CollectionSchema,
    DataType, Collection, utility
)

class MilvusManager:
    """Milvus 集合管理工具"""

    def __init__(self, host: str = "localhost", port: str = "19530"):
        connections.connect(host=host, port=port)

    def create_collection(self, name: str, dim: int = 768):
        """创建知识库集合"""
        # 定义字段
        fields = [
            FieldSchema(name="id", dtype=DataType.INT64, is_primary=True, auto_id=True),
            FieldSchema(name="vector", dtype=DataType.FLOAT_VECTOR, dim=dim),
            FieldSchema(name="content", dtype=DataType.VARCHAR, max_length=65535),
            FieldSchema(name="source", dtype=DataType.VARCHAR, max_length=512),
            FieldSchema(name="permission_level", dtype=DataType.INT32),
            FieldSchema(name="create_time", dtype=DataType.INT64),
        ]

        schema = CollectionSchema(fields, description="RAG知识库集合")
        collection = Collection(name=name, schema=schema)

        # 创建索引
        index_params = {
            "metric_type": "IP",        # IP (内积) 或 L2 (欧氏距离)
            "index_type": "HNSW",
            "params": {"M": 16, "efConstruction": 200}
        }
        collection.create_index(
            field_name="vector",
            index_params=index_params
        )

        return collection

    def insert_documents(self, collection_name: str, vectors, contents, metadata):
        """批量插入文档"""
        collection = Collection(collection_name)
        collection.load()

        mr = collection.insert([
            vectors,
            contents,
            metadata.get("sources", [""] * len(vectors)),
            metadata.get("permissions", [0] * len(vectors)),
            metadata.get("timestamps", [0] * len(vectors)),
        ])

        collection.flush()
        return mr

    def search(self, collection_name: str, query_vector, top_k: int = 10,
               expr: str = None, output_fields: List[str] = None):
        """检索"""
        collection = Collection(collection_name)
        collection.load()

        search_params = {
            "metric_type": "IP",
            "params": {"nprobe": 10},
        }

        results = collection.search(
            data=[query_vector],
            anns_field="vector",
            param=search_params,
            limit=top_k,
            expr=expr,                      # 标量过滤表达式
            output_fields=output_fields or ["content", "source"],
        )

        return results

    def drop_collection(self, name: str):
        """删除集合"""
        utility.drop_collection(name)
```

---

## 五、系统设计题（5题）

### 1. 设计一个企业级 RAG 知识库系统架构
> 面试官考察：全链路认知、模块拆解、技术选型、高可用设计。

**系统架构**（分层设计）：

| 层级 | 组件 | 技术选型 | 职责 |
|------|------|----------|------|
| **数据源层** | 文件系统、Wiki、DB、API | MinIO / NAS / MySQL | 多源数据接入 |
| **数据处理层** | PDF 解析、Chunking、Embedding | Unstructured + BGE-Large + LlamaIndex | 数据清洗与向量化 |
| **存储层** | 向量库 + 文档库 + 缓存 | Milvus + MySQL/ES + Redis | 向量+标量混合存储 |
| **检索层** | 双路检索 + Rerank | BM25 + BGE检索 + BGE-Reranker | 精确召回+精准排序 |
| **生成层** | Prompt + LLM + 输出控制 | Qwen/DeepSeek + LangChain | 基于检索结果回答 |
| **评估层** | RAGAS + 用户反馈 + 监控 | TruLens + Prometheus + ELK | 持续迭代优化 |
| **接入层** | Web UI + API + 权限控制 | FastAPI + React + RBAC | 对外服务 |

**高可用设计要点**：
1. 向量数据库主从复制（Milvus 集群）
2. LLM 服务多副本 + 负载均衡
3. Redis 缓存高频 Query 结果
4. 熔断降级：检索超时时返回兜底回答
5. 知识库定时同步 + 增量更新

### 2. 如何设计一个针对百万级文档的知识库检索系统？
> 海量数据下的检索设计：索引策略、分库分表、缓存层次。

| 挑战 | 解决方案 | 说明 |
|------|----------|------|
| 检索延迟 | 建立 ANN 索引（HNSW/IVF） | Nprobe 参数调优，延迟目标 < 200ms |
| 数据量超单机 | 分片（Sharding）+ 分区（Partitioning） | Milvus 按 doc_type 分区 |
| 向量维度高 | IVFPQ 向量压缩 | 精度损失 < 5%，存储减少 80% |
| 热点查询 | Redis 缓存 Top-1000 热门结果 | 缓存命中率 > 30% |
| 冷启动 | 全量构建后冷热分离 | 热数据 HNSW，冷数据 DiskANN |

```text
系统吞吐估算（百万级文档，每文档 10 个 Chunk）：
- 总向量数：1000 万
- 单向量维度：768
- 存储空间：1000w × 768 × 4bytes ≈ 30GB
- 单次检索时间：HNSW 约 50-100ms
- 并发 QPS：32 核机器可支持 > 500 QPS
```

### 3. 设计多租户 RAG 知识库的权限体系
> 企业级 RAG 必须支持多租户隔离和细粒度权限控制。

| 方案 | 实现方式 | 性能影响 | 复杂度 |
|------|----------|----------|--------|
| **Collection 隔离** | 每个租户独立 Milvus Collection | 无 | 低 |
| **Partition 隔离** | 同 Collection 不同 Partition | 极小 | 中 |
| **标量过滤** | 同 Collection，检索时加 `expr` 过滤 | 随权限粒度增大 | 高 |
| **混合方案** | Collection 隔离 + 内部 Role 标量过滤 | 中 | 中 |

```python
# 标量过滤实现多租户权限
def search_with_permission(
    collection: Collection, query_vector, user_id: str, role: str, top_k: int = 5
):
    """基于用户权限过滤检索结果"""
    # 权限表达式：用户只能看到其有权限的文档
    if role == "admin":
        expr = None  # 管理员可见全部
    elif role == "editor":
        expr = f"permission <= 2"
    else:
        expr = f"permission <= 1 AND user_id = '{user_id}'"

    return collection.search(
        data=[query_vector],
        anns_field="vector",
        param={"metric_type": "IP", "params": {"nprobe": 10}},
        limit=top_k,
        expr=expr,
        output_fields=["content", "source", "permission"]
    )
```

### 4. 如何设计一个 RAG 系统的 A/B 测试框架？
> 持续优化 RAG 需要线上 A/B 实验能力。

| 实验维度 | 变量 | 评估指标 |
|----------|------|----------|
| Chunking 策略 | Fixed-size vs Semantic vs Recursive | Recall@K, Context Precision |
| Embedding 模型 | BGE vs M3E vs text-embedding-3 | MRR, NDCG@10 |
| 检索策略 | 纯向量 vs 混合检索 vs HyDE | Faithfulness, Context Recall |
| Rerank 策略 | 有 vs 无；BGE vs Cross-Encoder | Answer Relevancy |
| LLM 模型 | Qwen vs DeepSeek vs GPT-4 | 用户满意度 |

```python
class RAGABTestFramework:
    """RAG A/B 测试框架"""

    def __init__(self, base_config: dict, experiments: dict):
        self.base_config = base_config
        self.experiments = experiments

    def get_pipeline(self, user_id: str) -> RAGPipeline:
        """根据用户 ID 分桶"""
        bucket = hash(user_id) % 100

        if bucket < 10:  # 对照组（10%）
            return self._build_pipeline(self.base_config)
        elif bucket < 30:  # 实验组 A（20%）：BGE-M3 替代 BGE-Large
            config = {**self.base_config, "embedding": "BAAI/bge-m3"}
            return self._build_pipeline(config)
        elif bucket < 50:  # 实验组 B（20%）：增加 Rerank
            config = {**self.base_config, "rerank": True}
            return self._build_pipeline(config)
        else:  # 默认组（50%）
            return self._build_pipeline(self.base_config)

    def log_result(self, user_id: str, query: str, result: dict, feedback: int):
        """记录实验结果"""
        bucket = hash(user_id) % 100
        # 写入评估数据存储
        log_entry = {
            "user_id": user_id,
            "bucket": bucket,
            "query": query,
            "faithfulness": result.get("faithfulness"),
            "user_feedback": feedback,
            "timestamp": datetime.now(),
        }
        self._write_to_log(log_entry)
```

### 5. 设计一个 RAG 问答系统的多轮对话策略
> 多轮对话 RAG 需要解决：上下文引用、历史压缩、Query 消歧。

```text
系统流程：
1️⃣  Query 理解阶段
    - 结合历史上下文重写当前 Query
    - 识别 Query 类型（事实型/推理型/对比型）
    - 提取关键实体和意图

2️⃣  检索策略选择阶段
    - 事实型：单路检索（Top-3）
    - 推理型：多路检索 + Rerank（Top-5）
    - 对比型：子查询分解 + 合并

3️⃣  上下文管理阶段
    - 历史窗口管理：保留最近 N 轮
    - Token 预算：Context = 检索结果 + 最近 3 轮 + 系统 Prompt
    - 长对话压缩：LLM 摘要历史

4️⃣  生成与验证阶段
    - 基于检索上下文 + 对话历史生成回答
    - 答案与检索结果一致性校验
    - 引用标注：标记回答依据的文档 ID
```

```python
class MultiTurnRAG:
    """多轮对话 RAG 系统"""

    def __init__(self, retriever, reranker, llm, max_history=6):
        self.retriever = retriever
        self.reranker = reranker
        self.llm = llm
        self.max_history = max_history
        self.history = []

    def chat(self, user_input: str) -> str:
        # 1. 查询重写（消除代词指代）
        rewritten = self._rewrite_with_history(user_input)

        # 2. 检索
        raw_results = self.retriever.search(rewritten, k=20)

        # 3. Rerank
        reranked = self.reranker.rerank(rewritten, raw_results, top_k=5)

        # 4. 构建带历史的 Prompt
        context = "\n\n".join([r["content"] for r in reranked])
        prompt = self._build_prompt(context, rewritten)

        # 5. 生成回答
        answer = self.llm.invoke(prompt)

        # 6. 更新历史
        self.history.append({"user": user_input, "assistant": answer})
        if len(self.history) > self.max_history:
            self.history = self.history[-self.max_history:]

        return answer

    def _rewrite_with_history(self, query: str) -> str:
        if not self.history:
            return query
        history_text = "\n".join(
            f"用户：{h['user']}\n助手：{h['assistant']}"
            for h in self.history[-3:]
        )
        prompt = f"""基于对话历史重写用户问题为独立检索问题。
对话历史：
{history_text}
当前问题：{query}
重写后："""
        return self.llm.invoke(prompt).strip()
```

---

## 六、常见坑点与最佳实践

### RAG 全链路常见坑点

| 阶段 | 常见坑点 | 问题表现 | 最佳实践 |
|------|----------|----------|----------|
| **PDF 解析** | 中文 PDF 乱码 | 检索不到中文内容 | 使用 Unstructured hi_res 模式 + 中文字体包 |
| **Chunking** | chunk_size 过小 | 答案片段化、信息不完整 | 根据文档类型调优，推荐 512-1024 tokens |
| **Chunking** | overlap 为 0 | 关键信息恰好被切分边界截断 | 设置 overlap = chunk_size * 15% |
| **Embedding** | 忘记加 Query 前缀 | BGE 检索精度下降 5-10% | BGE 需要 "为这个句子生成向量：" 前缀 |
| **Embedding** | 向量未归一化 | 余弦相似度和内积结果不一致 | encode 时设置 normalize_embeddings=True |
| **检索** | Top-K 固定不变 | 简单问题信息冗余，复杂问题信息不足 | 根据 query 长度动态调整 K 值 |
| **检索** | 只使用向量检索 | 精确匹配失败（版本号、错误码） | 必须加 BM25 关键词检索构成双路检索 |
| **Rerank** | 使用 Bi-Encoder 做 Rerank | 精度提升有限 | Rerank 必须用 Cross-Encoder 架构 |
| **Rerank** | 对全部候选文档 Rerank | 延迟过高 | 先粗筛 Top-50 ~ Top-100，再 Rerank Top-5 |
| **生成** | Prompt 未约束"不知道就说不知道" | 幻觉率高 | 显式在 Prompt 中要求不知道时明确说明 |
| **生成** | Temperature 过高 | 回答不稳定、不可控 | RAG 场景推荐 temperature=0.1 ~ 0.3 |
| **评估** | 只用离线指标不用在线指标 | 离线好但线上体验差 | 离线 RAGAS + 在线用户满意度双评估 |
| **部署** | Milvus 索引参数未调优 | 检索慢、召回率低 | 根据数据量选择 HNSW（M=16, ef=200）或 IVF |
| **部署** | 全量重建频率太高 | 系统资源浪费 | 增量更新 + 定期全量重建（低峰期） |

### RAG 技术选型决策树

```text
需要构建 RAG 系统？
├─ 数据量 < 10万 Chunk
│  ├─ 快速原型 → Chroma + BGE-Base + LangChain
│  └─ 生产环境 → Qdrant + BGE-Large + FastAPI
├─ 数据量 10万 ~ 1000万
│  ├─ 中文为主 → Milvus + BGE-Large + JSF Hybrid
│  ├─ 英文为主 → Pinecone + text-embedding-3 + LangChain
│  └─ 多语言   → Milvus + BGE-M3 + RRF Fusion
└─ 数据量 > 1000万
   ├─ 企业内部 → Milvus Cluster + DiskANN + 分库分表
   └─ SaaS 服务 → 自建向量引擎 + GPU 集群
```

### 性能优化 Checklist

| 优化项 | 预期效果 | 实现难度 |
|--------|----------|----------|
| HNSW 索引代替 IVF | 检索延迟降低 50-80% | 低（改索引参数） |
| IVFPQ 向量压缩 | 存储减少 80%，精度损失 < 5% | 低 |
| 批量 Embedding | 处理速度提升 10x | 低 |
| Redis 缓存高频 Query | 平均延迟降低 60%+ | 中 |
| 模型量化（INT8） | 显存减少 50%，推理快 2x | 中 |
| 异步检索 + 合并 | 多通道检索延迟减半 | 中 |
| GPU 加速 Embedding | 向量化速度提升 20x+ | 低（设置 device="cuda"） |

---

## 七、面试回答模板（Top 5）

### 模板 1："介绍一下你们做的 RAG 知识库项目"
> 适用场景：项目介绍、技术面开场

**回答结构（STAR 法则）**：
```
S（背景）：我们为某大型企业构建了内部知识库问答系统，包含 50 万+ 技术文档，
          覆盖产品手册、运维文档、FAQ 等多种格式。

T（挑战）：文档格式多样（PDF、Word、Markdown），包含大量表格和代码片段；
          检索精度要求高，支持复杂的多轮对话。

A（方案）：采用 Milvus 向量数据库 + BGE-Large 稠密嵌入 + BM25 稀疏嵌入的
          双路检索架构，使用 Unstructured 进行 PDF 解析，引入 BGE-Reranker
          进行二次排序，最终通过 DeepSeek 模型生成回答。系统基于 Modular RAG
          的条件模式实现多知识库路由。

R（成果）：检索 MRR 达到 0.85，回答 Faithfulness 达到 0.92，用户满意度 4.3/5.0，
          日均处理 5000+ 查询，平均响应时间 1.2 秒。
```

### 模板 2："RAG 和 Fine-tuning 如何选择？"
> 适用场景：技术选型讨论

```text
RAG 适合：
  - 知识频繁更新（如产品文档、政策法规）
  - 需要答案可溯源、可验证
  - 覆盖大量长尾知识
  - 快速上线验证需求

Fine-tuning 适合：
  - 固定风格输出（如客服语气、报告格式）
  - 模型行为优化（如指令遵循、JSON 输出）
  - 领域术语生成（如医疗、法律专用术语）

最佳实践：
  RAG + Fine-tuning 协同使用。Fine-tuning 让模型学会更好的指令遵循和输出格式，
  RAG 提供最新、最准的知识内容。两者互补而非互斥。
```

### 模板 3："如何降低 RAG 系统的幻觉？"
> 最常被问到的 RAG 问题之一

**回答要点（从数据到生成的全链路控制）**：
```text
1. 源头治理：确保知识库数据质量，定期清理过时/错误文档
2. 检索优化：调优 Chunking 参数和 Top-K 值，确保检索到足够相关上下文
3. Rerank 过滤：Cross-Encoder 过滤掉不相关内容，保证 LLM 输入质量
4. Prompt 约束：显式要求"仅基于文档回答，不知道就说不知道"
5. 温度控制：temperature 设为 0.1-0.3，减少模型自由发挥
6. 阈值拒绝：检索分数低于阈值（如 0.7）时拒绝回答
7. 答案校验：用另一个 LLM 验证回答是否基于检索内容
8. 溯源展示：回答时附上引用文档，让用户自查
```

### 模板 4："解释一下双路检索的原理和实现"
> 考察混合检索的理解深度

```text
双路检索 = 稠密向量检索（语义理解）+ 稀疏向量检索（精准匹配）。

稠密检索使用 BGE 等 Embedding 模型将文本转为 768 维稠密向量，
擅长同义词、语义改写、跨语言等语义匹配，但无法精确匹配版本号、错误码等。

稀疏检索使用 BM25 算法，基于词频-逆文档频率计算相关性，
擅长精确关键词匹配，如合同编号 V2.1.3、错误码 ERR_500，但无法理解同义词。

实现上，一个 Query 同时走两路检索，各自返回 Top-20 结果，
然后用 RRF（Reciprocal Rank Fusion）融合排序，最终取 Top-5。

关键参数：alpha（稠密权重）需要通过实验调优，
精确查询场景 alpha=0.3-0.5，语义查询场景 alpha=0.7-0.8。
```

### 模板 5："RAG 项目中有哪些困难？你是怎么解决的？"
> 考察实际落地的工程经验和思考深度

```text
困难 1：PDF 中的表格和图片提取不全，导致表格数据丢失
  解决方案：用 Unstructured hi_res 模式提取表格为 Markdown，
  图片提取后生成描述文本。对复杂表格做字段级标注。

困难 2：中文长文档的 Chunking 参数调优
  解决方案：构建 Chunking 参数搜索实验，在 50 个标注 Query 上
  测试 chunk_size=256/512/768 和 overlap=0/50/100 的组合，
  选择 Recall@5 最高的配置。

困难 3：检索到不相关内容导致幻觉
  解决方案：引入 BGE-Reranker 对初检结果二次排序，
  设置相似度阈值 0.65，低于阈值时触发二次检索。最终
  Faithfulness 从 0.78 提升到 0.92。

困难 4：生产环境检索延迟过高
  解决方案：从 IVF 切换到 HNSW 索引，部署 GPU Embedding 服务，
  增加 Redis 缓存层，P95 延迟从 3.2 秒降至 0.8 秒。
```

---

## 八、快速查漏补缺 Checklist

### 基础概念
- [ ] 能清晰解释 RAG 的定义和核心价值
- [ ] 掌握 RAG 的三层架构（数据层、检索层、生成层）
- [ ] 区分 Naive RAG / Advanced RAG / Modular RAG
- [ ] 理解 Self-RAG、Corrective RAG、GraphRAG、Agentic RAG 的核心区别
- [ ] 知道 Chunking 的四种策略及参数含义
- [ ] 了解 Dense Embedding vs Sparse Embedding

### 检索技术
- [ ] 能写出 BM25 核心公式并解释参数含义
- [ ] 掌握双路检索原理和 RRF 融合算法
- [ ] 理解 Cross-Encoder vs Bi-Encoder 的架构差异
- [ ] 了解 Rerank 在 RAG 中的位置和作用
- [ ] 掌握 HNSW 索引原理和参数意义（M, efConstruction, efSearch）
- [ ] 知道 IVF 索引的 nlist 和 nprobe 参数
- [ ] 了解 ANN vs KNN 的区别

### Embedding 与模型
- [ ] 理解 word2vec、CBOW 原理
- [ ] 了解主流 Embedding 模型的参数（BGE、M3E、text-embedding-3）
- [ ] 知道如何使用 LlamaIndex 微调 Embedding 模型
- [ ] 掌握 MTEB 评估标准和主要评估任务
- [ ] 了解余弦相似度、欧氏距离、点积的区别

### 向量数据库
- [ ] 掌握 Milvus 核心操作：Collection、Index、Insert、Search
- [ ] 了解 Milvus vs Chroma vs Qdrant 的选型差异
- [ ] 能写标量过滤实现权限控制
- [ ] 知道 HNSW 索引调优方法
- [ ] 了解 K-means 和肘部法则确定聚类数

### 评估
- [ ] 掌握 RAGAS 四大指标：Faithfulness、Answer Relevancy、Context Recall、Context Precision
- [ ] 了解 MRR、NDCG、Hit Rate 计算方式
- [ ] 知道 TruLens 评估框架
- [ ] 能构建标注数据集进行离线评估
- [ ] 了解在线评估指标（用户满意度、问题解决率）

### 系统设计
- [ ] 能设计完整的端到端 RAG 架构
- [ ] 理解增量更新与全量重建策略
- [ ] 掌握多租户权限设计
- [ ] 了解多轮对话 RAG 的实现要点
- [ ] 知道 LLM hallucination 的全链路控制方法
- [ ] 能设计 A/B 测试框架

### 代码能力
- [ ] 能手写完整 RAG Pipeline
- [ ] 能手写 BM25 检索器
- [ ] 能手写 RRF 融合检索
- [ ] 能手写 Milvus 集合创建与管理
- [ ] 能手写 Embedding 模型微调
- [ ] 能手写 Query 重写和子查询分解

### 企业落地
- [ ] 了解 PDF 解析方案（Unstructured、PyMuPDF、LlamaParse）
- [ ] 了解表格和图片特殊处理方式
- [ ] 知道 Chunking 参数调优实验方法
- [ ] 了解生产环境性能优化手段
- [ ] 了解企业级权限和数据安全方案
- [ ] 了解 RAG 行业落地最佳实践

---

> 🎯 **备战要点总结**：
> 1. **面试核心**：RAG 全链路流程概念、双路检索+Rerank原理、Embedding模型对比、手写Pipeline
> 2. **高频代码**：RAG Pipeline、BM25、RRF融合、Milvus操作、Query重写
> 3. **系统设计**：多租户权限、多轮对话、百万级检索、A/B测试
> 4. **加分项**：HyDE/FLARE等高级RAG技术、Embedding微调、RAGAS评估、RankGPT
