# 第9步：检索增强生成(RAG)基础

> **阶段目标：** 理解RAG的核心概念和架构,掌握基础RAG系统的搭建方法，理解搜索增强和数据接入技术  
> **预计学时：** 2-3周（每天3-4小时）  
> **前置要求：** Prompt Engineering + Embedding基础 + API调用  

---

## 📚 目录

- [9.1 RAG为什么重要](#91-rag为什么重要)
- [9.2 RAG核心架构](#92-rag核心架构)
- [9.3 文档处理管线](#93-文档处理管线)
- [9.4 检索系统](#94-检索系统)
- [9.5 生成增强](#95-生成增强)
- [9.6 构建端到端RAG系统](#96-构建端到端rag系统)
- [9.7 RAG评估方法](#97-rag评估方法)
- [9.8 阶段练习](#98-阶段练习)
- [9.9 常见问题](#99-常见问题)

---

## 9.1 RAG为什么重要

### 9.1.1 LLM的三大局限

```
局限1: 知识截止日期
  LLM训练完成后知识就冻结了
  "最新的GPT-5有什么功能？" → 无法回答（如果训练数据截止在2024）

局限2: 幻觉问题
  LLM可能自信地生成虚假信息
  "XX公司的CEO是谁？" → 可能编造一个不存在的人

局限3: 私有知识盲区
  LLM不知道你公司的内部文档
  "我们公司的报销流程是什么？" → 完全不知道

RAG = 给LLM配一个"实时外挂知识库"
     每次提问先查资料，再基于资料回答
```

### 9.1.2 RAG vs 微调

```
            RAG                          微调(Fine-tuning)
            ═══                          ══════════════════
知识注入:    检索外部文档                   写入模型参数
更新速度:    即时（更新文档即可）           需要重新训练
成本:       低（只需Embedding+检索）       高（需要GPU训练）
准确性:     可追溯（知道来自哪篇文档）      不可追溯
适用场景:    知识密集型、频繁更新           风格/格式学习、领域适应

实践中：RAG和微调经常结合使用
```

---

## 9.2 RAG核心架构

### 9.2.1 Naive RAG流程

```
用户问题："我们的退货政策是什么？"

1. 查询
   └→ Embedding("退货政策是什么") → 查询向量

2. 检索 (Retrieval)
   └→ 在向量数据库中找到最相关的文档片段
      返回: ["退货政策：购买后7天内...", "换货：30天内..."]

3. 增强 (Augmented)
   └→ 把检索到的文档拼接到Prompt中
      Prompt = "基于以下文档回答：\n\n[文档1]...\n[文档2]...\n\n问题：..."

4. 生成 (Generation)
   └→ LLM生成最终回答
      "根据退货政策，您可以在购买后7天内无条件退货..."
```

### 9.2.2 数据流全景

```
                    离线阶段（索引构建）
                    ════════════════
    文档 
     ↓
    文档加载器 (PDF, DOCX, Markdown, Web...)
     ↓
    文本分割器 (Chunking) 
     ↓
    Embedding模型 → 文档向量
     ↓
    向量数据库 (存储向量+原文)

                    在线阶段（查询检索）
                    ════════════════
    用户查询
     ↓
    Embedding模型 → 查询向量
     ↓
    向量数据库检索 → Top-K相关文档
     ↓
    Prompt构建 → 拼接文档+问题
     ↓
    LLM生成 → 最终回答
```

---

## 9.3 文档处理管线

### 9.3.1 文档加载

```python
from langchain.document_loaders import (
    PyPDFLoader,        # PDF
    Docx2txtLoader,     # Word
    TextLoader,         # 纯文本
    CSVLoader,          # CSV
    UnstructuredMarkdownLoader,  # Markdown
    WebBaseLoader,      # 网页
)
from langchain.text_splitter import (
    RecursiveCharacterTextSplitter,
    TokenTextSplitter,
    MarkdownHeaderTextSplitter,
)

# ========== 多格式文档加载 ==========
class DocumentLoader:
    """统一文档加载器"""
    
    LOADERS = {
        '.pdf': PyPDFLoader,
        '.docx': Docx2txtLoader,
        '.txt': TextLoader,
        '.md': UnstructuredMarkdownLoader,
        '.csv': CSVLoader,
    }
    
    @classmethod
    def load(cls, file_path: str) -> List[Document]:
        """自动识别格式并加载"""
        import os
        ext = os.path.splitext(file_path)[1].lower()
        
        if ext not in cls.LOADERS:
            raise ValueError(f"不支持的格式: {ext}")
        
        loader = cls.LOADERS[ext](file_path)
        return loader.load()
    
    @classmethod
    def load_directory(cls, dir_path: str) -> List[Document]:
        """加载目录中所有支持的文档"""
        docs = []
        for root, _, files in os.walk(dir_path):
            for file in files:
                ext = os.path.splitext(file)[1].lower()
                if ext in cls.LOADERS:
                    try:
                        file_docs = cls.load(os.path.join(root, file))
                        docs.extend(file_docs)
                        print(f"✓ 加载: {file} ({len(file_docs)}段)")
                    except Exception as e:
                        print(f"✗ 失败: {file} - {e}")
        return docs
```

### 9.3.2 文本分割 (Chunking)

这是RAG最关键的步骤之一！

```python
# ========== Chunking策略 ==========

# 策略1: 递归字符分割（最常用）
recursive_splitter = RecursiveCharacterTextSplitter(
    chunk_size=500,        # 每段500字符
    chunk_overlap=50,      # 段与段重叠50字符
    separators=["\n\n", "\n", "。", "！", "？", ".", "!", "?", " ", ""],
    # 优先按段落分 → 按行分 → 按句子分 → 按空格分 → 按字符分
)

# 策略2: Token级别分割（对LLM更精确）
token_splitter = TokenTextSplitter(
    chunk_size=256,        # 每段256 tokens
    chunk_overlap=20,      # 重叠20 tokens
)

# 策略3: Markdown结构分割（保留文档结构）
markdown_splitter = MarkdownHeaderTextSplitter(
    headers_to_split_on=[
        ("#", "标题1"),
        ("##", "标题2"),
        ("###", "标题3"),
    ]
)

# ========== Chunking最佳实践 ==========
"""
1. chunk_size 选择：
   - 简单QA: 256-512
   - 一般场景: 500-1000
   - 长文档分析: 1000-2000
   
2. chunk_overlap：
   - 通常设为chunk_size的10-20%
   - 防止关键信息正好在分割边界上
   
3. 保留元数据：
   - 来源文档名
   - 页码/位置
   - 章节标题
   - 创建时间
"""

# ========== 智能分割器 ==========
class SmartChunker:
    """智能文档分割器 — 考虑语义完整性"""
    
    def __init__(self, chunk_size: int = 500, chunk_overlap: int = 50):
        self.splitter = RecursiveCharacterTextSplitter(
            chunk_size=chunk_size,
            chunk_overlap=chunk_overlap,
            separators=["\n\n", "\n", "。", "！", "？", ".", "!", "?"],
        )
    
    def split_documents(self, docs: List[Document]) -> List[Document]:
        """分割文档，同时添加丰富的元数据"""
        chunks = self.splitter.split_documents(docs)
        
        for i, chunk in enumerate(chunks):
            # 添加来源信息
            chunk.metadata["chunk_id"] = i
            chunk.metadata["chunk_size"] = len(chunk.page_content)
            
            # 添加时间戳
            if "source" in chunk.metadata:
                chunk.metadata["indexed_at"] = time.time()
        
        return chunks
```

---

## 9.4 检索系统

### 9.4.1 基础向量检索

```python
from sentence_transformers import SentenceTransformer
import numpy as np

class SimpleVectorStore:
    """简单向量存储 — 理解检索原理（生产环境用Milvus/Chroma）"""
    
    def __init__(self, embedding_model: str = "all-MiniLM-L6-v2"):
        self.encoder = SentenceTransformer(embedding_model)
        self.documents = []      # 原文列表
        self.embeddings = None   # 向量矩阵
        self.metadata = []       # 元数据列表
    
    def add_documents(self, documents: List[str], 
                      metadata: List[dict] = None):
        """添加文档到存储"""
        self.documents.extend(documents)
        
        if metadata:
            self.metadata.extend(metadata)
        else:
            self.metadata.extend([{}] * len(documents))
        
        # 编码为新向量
        new_embeddings = self.encoder.encode(documents)
        
        if self.embeddings is None:
            self.embeddings = new_embeddings
        else:
            self.embeddings = np.vstack([self.embeddings, new_embeddings])
    
    def search(self, query: str, top_k: int = 5) -> List[dict]:
        """搜索最相关的文档"""
        # 编码查询
        query_embedding = self.encoder.encode([query])[0]
        
        # 计算余弦相似度
        similarities = np.dot(self.embeddings, query_embedding) / (
            np.linalg.norm(self.embeddings, axis=1) * np.linalg.norm(query_embedding)
        )
        
        # 取Top-K
        top_indices = np.argsort(similarities)[-top_k:][::-1]
        
        results = []
        for idx in top_indices:
            results.append({
                "content": self.documents[idx],
                "score": float(similarities[idx]),
                "metadata": self.metadata[idx],
            })
        
        return results
```

### 9.4.2 混合检索 (Hybrid Search)

```python
"""
为什么需要混合检索？

纯向量检索的局限：
- 对专有名词、精确匹配不敏感
- "ACME-3000型号" 和 "ACME3000型号" 语义上极相似，
  但如果向量模型没训练过，可能检索不到

混合检索 = 向量检索 + 关键词检索(BM25)

工作流程：
1. 向量检索：返回语义最相关的 Top-20
2. BM25检索：返回关键词匹配的 Top-20
3. 融合排序：合并去重，重新排名
4. 返回最终 Top-K

这在企业场景中特别重要，当用户查询包含：
- 产品型号: "SKU-12345"
- 工单号:   "TICKET-2024-001"
- 日期:     "2024年1月"
- 人名:     "张三"
"""

class HybridRetriever:
    """混合检索器"""
    
    def __init__(self, vector_store, bm25_index):
        self.vector_store = vector_store
        self.bm25 = bm25_index
    
    def search(self, query: str, top_k: int = 5,
               vector_weight: float = 0.7) -> List[dict]:
        """混合检索"""
        # 向量检索
        vector_results = self.vector_store.search(query, top_k=top_k * 2)
        
        # BM25关键词检索
        bm25_results = self.bm25.search(query, top_k=top_k * 2)
        
        # RRF (Reciprocal Rank Fusion) 融合
        return self._rrf_fusion(vector_results, bm25_results, 
                                top_k, vector_weight)
    
    def _rrf_fusion(self, results_a: list, results_b: list,
                    top_k: int, weight_a: float) -> list:
        """
        RRF算法: Reciprocal Rank Fusion
        
        对每个文档计算: score = Σ 1/(k + rank_i)
        其中k是平滑常数（通常k=60）
        """
        k = 60
        scores = {}
        contents = {}
        
        # 从向量结果计算分数
        for rank, r in enumerate(results_a):
            doc_id = r.get("id", r["content"])
            scores[doc_id] = scores.get(doc_id, 0) + weight_a / (k + rank + 1)
            contents[doc_id] = r
        
        # 从BM25结果计算分数
        for rank, r in enumerate(results_b):
            doc_id = r.get("id", r["content"])
            scores[doc_id] = scores.get(doc_id, 0) + (1 - weight_a) / (k + rank + 1)
            if doc_id not in contents:
                contents[doc_id] = r
        
        # 排序返回
        sorted_ids = sorted(scores.keys(), key=lambda x: scores[x], reverse=True)
        
        results = []
        for doc_id in sorted_ids[:top_k]:
            r = contents[doc_id].copy()
            r["fusion_score"] = scores[doc_id]
            results.append(r)
        
        return results
```

### 9.4.3 重排序 (Re-ranking)

```python
"""
重排序: RAG系统的"质检员"

问题：向量检索返回20个候选文档，但它们可能不够精准
解决：用一个更强的模型（通常是Cross-Encoder）对候选重新打分

Cross-Encoder vs Bi-Encoder:
- Bi-Encoder: query和doc分开编码，然后算相似度 → 快但不精确
- Cross-Encoder: query和doc一起送入模型 → 慢但更精确

典型流程：
粗排 (向量检索, Top-100) → 精排 (Cross-Encoder, Top-10) → 最终Top-5
"""

class Reranker:
    """基于Cross-Encoder的重排序器"""
    
    def __init__(self, model_name: str = "BAAI/bge-reranker-large"):
        from transformers import AutoModelForSequenceClassification, AutoTokenizer
        self.tokenizer = AutoTokenizer.from_pretrained(model_name)
        self.model = AutoModelForSequenceClassification.from_pretrained(model_name)
        self.model.eval()
    
    def rerank(self, query: str, documents: List[str], 
               top_k: int = 5) -> List[dict]:
        """对文档重排序"""
        import torch
        
        pairs = [[query, doc] for doc in documents]
        
        with torch.no_grad():
            inputs = self.tokenizer(
                pairs, padding=True, truncation=True,
                return_tensors='pt', max_length=512,
            )
            scores = self.model(**inputs).logits.squeeze(-1)
        
        # 排序
        ranked_indices = torch.argsort(scores, descending=True)[:top_k]
        
        results = []
        for idx in ranked_indices:
            results.append({
                "content": documents[idx],
                "score": float(scores[idx]),
            })
        
        return results
```

---

## 9.5 生成增强

### 9.5.1 Prompt构建策略

```python
class RAGPromptBuilder:
    """RAG Prompt构建器"""
    
    @staticmethod
    def build_qa_prompt(query: str, retrieved_docs: List[dict]) -> str:
        """构建问答Prompt"""
        # 格式化检索到的文档
        context_parts = []
        for i, doc in enumerate(retrieved_docs):
            source = doc.get("metadata", {}).get("source", "未知来源")
            context_parts.append(
                f"[文档{i+1}] (来源: {source})\n{doc['content']}"
            )
        
        context = "\n\n".join(context_parts)
        
        prompt = f"""
你是一个基于文档的问答助手。请严格基于以下文档回答用户问题。

# 参考文档
{context}

# 用户问题
{query}

# 回答要求
1. 严格基于文档内容回答，不要编造信息
2. 如果文档中没有相关信息，明确说"根据现有文档，无法回答该问题"
3. 引用具体文档编号，如"根据[文档1]..."
4. 如果多个文档信息冲突，指出冲突并提供不同来源的说法
5. 用清晰的结构化方式组织回答
"""
        return prompt
    
    @staticmethod
    def build_analysis_prompt(query: str, retrieved_docs: List[dict]) -> str:
        """构建分析类Prompt"""
        # 用于需要综合分析的场景
        pass
    
    @staticmethod
    def build_summary_prompt(query: str, retrieved_docs: List[dict]) -> str:
        """构建总结类Prompt"""
        # 用于长文档总结
        pass
```

### 9.5.2 引用追踪

```python
"""
生产级RAG系统的必备功能：能够追溯每个回答的来源

用户问：这个产品的保修期是多久？
系统答：根据[产品手册第3页]，保修期为购买后2年。
          [来源: product_manual.pdf, Page 3]

这样用户可以自己验证，也方便审计和调试。
"""

class CitationTracker:
    """引用追踪器"""
    
    def __init__(self):
        self.citations = []
    
    def add_citation(self, doc_id: str, source: str, 
                     page: int = None, relevance: float = None):
        self.citations.append({
            "doc_id": doc_id,
            "source": source,
            "page": page,
            "relevance": relevance,
        })
    
    def to_context_string(self) -> str:
        """生成可注入Prompt的引用格式"""
        return "\n".join([
            f"[{i+1}] {c['source']}" + 
            (f", 第{c['page']}页" if c.get('page') else "")
            for i, c in enumerate(self.citations)
        ])
```

---

## 9.6 构建端到端RAG系统

```python
class RAGSystem:
    """
    完整的RAG系统
    
    特性：
    - 多格式文档加载
    - 智能文本分割
    - 混合检索
    - 重排序
    - 引用追踪
    - 来源验证
    """
    
    def __init__(self, 
                 embedding_model: str = "BAAI/bge-large-zh-v1.5",
                 llm_client=None,
                 vector_store=None,
                 chunk_size: int = 500,
                 chunk_overlap: int = 50,
                 top_k: int = 5):
        
        self.encoder = SentenceTransformer(embedding_model)
        self.llm = llm_client
        self.chunk_size = chunk_size
        self.chunk_overlap = chunk_overlap
        self.top_k = top_k
        
        # 文档分割器
        self.splitter = RecursiveCharacterTextSplitter(
            chunk_size=chunk_size,
            chunk_overlap=chunk_overlap,
            separators=["\n\n", "\n", "。", "！", "？", ".", "?"],
        )
        
        # 向量存储（简易版）
        self.vector_store = SimpleVectorStore(embedding_model)
        
        # Prompt构建器
        self.prompt_builder = RAGPromptBuilder()
    
    def index_documents(self, file_paths: List[str]):
        """索引文档（离线阶段）"""
        all_docs = []
        
        for file_path in file_paths:
            print(f"加载: {file_path}")
            docs = DocumentLoader.load(file_path)
            all_docs.extend(docs)
        
        print(f"共加载 {len(all_docs)} 个文档段")
        
        # 分割
        chunks = self.splitter.split_documents(all_docs)
        print(f"分割为 {len(chunks)} 个chunks")
        
        # 存储到向量数据库
        texts = [c.page_content for c in chunks]
        metadata = [c.metadata for c in chunks]
        self.vector_store.add_documents(texts, metadata)
        
        print(f"✓ 索引完成，共 {len(texts)} 个文档块")
    
    def query(self, question: str) -> dict:
        """查询RAG系统（在线阶段）"""
        # Step 1: 检索
        retrieved = self.vector_store.search(question, top_k=self.top_k)
        
        # Step 2: 构建Prompt
        prompt = self.prompt_builder.build_qa_prompt(question, retrieved)
        
        # Step 3: 生成回答
        response = self.llm.chat.completions.create(
            model="gpt-4o",
            messages=[{"role": "user", "content": prompt}],
            temperature=0.3,  # 低温度，减少幻觉
        )
        
        answer = response.choices[0].message.content
        
        # Step 4: 收集来源引用
        sources = []
        for doc in retrieved:
            sources.append({
                "content": doc["content"][:200],  # 截断展示
                "source": doc.get("metadata", {}).get("source", ""),
                "score": doc["score"],
            })
        
        return {
            "question": question,
            "answer": answer,
            "sources": sources,
            "prompt_tokens": response.usage.prompt_tokens,
            "completion_tokens": response.usage.completion_tokens,
        }


# ========== 使用示例 ==========
rag = RAGSystem(
    embedding_model="BAAI/bge-large-zh-v1.5",
    llm_client=client,
    chunk_size=500,
    top_k=5,
)

# 索引文档
rag.index_documents([
    "docs/product_manual.pdf",
    "docs/faq.docx",
    "docs/policy.txt",
])

# 查询
result = rag.query("产品的保修期是多久？")
print(f"回答: {result['answer']}\n")
print("参考来源:")
for i, src in enumerate(result['sources']):
    print(f"  [{i+1}] {src['source']} (相关度: {src['score']:.3f})")
```

---

## 9.7 RAG评估方法

```python
"""
RAG系统评估的三个维度：

1. 检索质量
   - Recall@K: Top-K结果中包含正确答案的比例
   - MRR (Mean Reciprocal Rank): 第一个相关文档的平均排名倒数
   - NDCG: 考虑排序位置的检索质量

2. 生成质量
   - Faithfulness: 回答是否忠于检索到的文档（不编造）
   - Answer Relevance: 回答是否切题
   - Context Relevance: 检索的文档是否和问题相关

3. 系统性能
   - 检索延迟
   - 端到端延迟
   - Token使用量
"""

# ========== RAGAS评估示例 ==========
from ragas import evaluate
from ragas.metrics import (
    faithfulness,          # 忠实度
    answer_relevancy,      # 回答相关性
    context_relevancy,     # 上下文相关性
    context_recall,        # 上下文召回
)

# 准备评估数据
eval_data = {
    "question": ["退货政策是什么？"],
    "answer": ["购买后7天内可以无条件退货..."],
    "contexts": [["退货政策：购买后7天内无条件退货..."]],
    "ground_truth": ["7天内可以退货"],
}

# result = evaluate(eval_data, metrics=[faithfulness, answer_relevancy])
```

---

## 9.8 阶段练习

### 练习1：搭建文档问答系统
选一个你熟悉的领域文档，搭建完整的RAG Q&A系统。

### 练习2：Chunking对比实验
对比不同chunk_size (256/512/1024) 对检索效果的影响。

### 练习3：检索策略对比
在同一个文档集上，对比纯向量检索和混合检索的效果差异。

---

## 9.9 常见问题

### Q1: RAG和直接把文档放进Prompt有什么区别？

**答：** 根本区别在于Token限制：
- 直接放：一篇10页文档≈5000 tokens → 可以放进去，但5篇就满了
- RAG：索引1000篇文档 → 只取最相关的5段≈2500 tokens放进Prompt

RAG解决的是"大海捞针"的问题。

### Q2: 如何选择Embedding模型？

| 场景 | 推荐 |
|------|------|
| 中文 | BAAI/bge-large-zh-v1.5 |
| 英文 | text-embedding-3-large |
| 多语言 | paraphrase-multilingual-MiniLM-L12-v2 |
| 代码 | codebert-base |
| 轻量 | all-MiniLM-L6-v2 (384维) |

### Q3: RAG一定会准确吗？

不一定。可能的问题：
- 检索不到相关文档（召回失败）
- 检索到无关文档（误导模型）
- 文档本身信息过时或错误
- LLM忽略文档信息（Context Ignoring）

持续评估和优化是RAG系统的常态。

---

> **✅ 阶段完成检查清单：**
> - [ ] 理解RAG的检索→增强→生成三阶段流程
> - [ ] 能搭建完整的文档加载+分割+索引管线
> - [ ] 掌握了向量检索的基本原理
> - [ ] 了解混合检索和重排序的必要性
> - [ ] 能构建端到端的RAG问答系统
> - [ ] 完成3个阶段练习
>
> **下一步：** [第10步：向量数据库与数据加载](../10-向量数据库与数据加载/README.md)
