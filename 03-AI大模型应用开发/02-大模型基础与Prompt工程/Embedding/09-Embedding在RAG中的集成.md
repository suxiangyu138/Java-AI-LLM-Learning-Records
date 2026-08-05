# 09 - Embedding 在 RAG 中的集成

> 🎯 Embedding 是 RAG 的"眼睛" — 从文档入库到用户查询，Embedding 贯穿 RAG 全链路。理解整条链路才能做对端到端优化

---

## 目录

1. [RAG 全链路中的 Embedding](#1-rag-全链路中的-embedding)
2. [离线索引阶段](#2-离线索引阶段)
3. [在线检索阶段](#3-在线检索阶段)
4. [Embedding 质量对 RAG 精度的影响](#4-embedding-质量对-rag-精度的影响)
5. [RAG 完整代码示例](#5-rag-完整代码示例)
6. [常见问题与调优](#6-常见问题与调优)

---

## 1. RAG 全链路中的 Embedding

```text
RAG = Retrieval Augmented Generation = 检索增强生成

全链路中 Embedding 出现在两个关键节点：

┌─────────────────────────────────────────────────────────┐
│                  离线索引阶段（入库）                     │
│                                                          │
│  文档 → 切片 → Embedding → 向量数据库                     │
│               ⬆                                         │
│           Embedding 模型                                 │
├─────────────────────────────────────────────────────────┤
│                  在线检索阶段（查询）                     │
│                                                          │
│  用户问题 → Embedding → 向量检索 → 相关片段 → LLM → 回答  │
│               ⬆                                         │
│           Embedding 模型                                 │
└─────────────────────────────────────────────────────────┘

两个阶段必须使用同一个 Embedding 模型！
不同模型的向量空间不同 → 不能混用
```

---

## 2. 离线索引阶段

### 2.1 文档入库流程

```python
from langchain_text_splitters import RecursiveCharacterTextSplitter

class DocumentIndexer:
    """文档入库器 — 离线索引"""
    
    def __init__(self, embedder, vector_store):
        self.embedder = embedder      # Embedding 模型
        self.vector_store = vector_store  # 向量数据库
        self.splitter = RecursiveCharacterTextSplitter(
            chunk_size=500,
            chunk_overlap=50
        )
    
    def index_document(self, doc_id: str, content: str, metadata: dict = None):
        """索引单个文档"""
        # ① 切片
        chunks = self.splitter.split_text(content)
        print(f"文档 {doc_id}: 切为 {len(chunks)} 块")
        
        # ② Embedding
        vectors = self.embedder.embed_documents(chunks)
        
        # ③ 存储到向量数据库
        for i, (chunk, vector) in enumerate(zip(chunks, vectors)):
            self.vector_store.add(
                id=f"{doc_id}_{i}",
                text=chunk,
                vector=vector,
                metadata={
                    "doc_id": doc_id,
                    "chunk_index": i,
                    **(metadata or {})
                }
            )
        
        return len(chunks)
    
    def index_batch(self, documents: list[dict]):
        """批量索引"""
        total = 0
        for doc in documents:
            total += self.index_document(
                doc["id"], doc["content"], doc.get("metadata")
            )
        print(f"总计索引 {total} 个块")
        return total
```

### 2.2 增量更新策略

```text
文档更新时的索引策略：

① 全量重建（简单可靠）
   删除旧索引 → 重新入库所有文档
   适用：文档量 < 10 万、更新频率低（天/周级）

② 增量更新（高效复杂）
   检测变更的文档 → 删除旧块 → 插入新块
   适用：文档量 > 10 万、更新频率高（小时级）

③ 版本化索引（推荐）
   每个版本独立索引 → 切换 alias 到新版本
   旧版本保留 N 天 → 可回滚
   适用：生产环境
```

---

## 3. 在线检索阶段

### 3.1 查询流程

```python
class RAGRetriever:
    """RAG 检索器"""
    
    def __init__(self, embedder, vector_store, llm=None):
        self.embedder = embedder
        self.vector_store = vector_store
        self.llm = llm
    
    def retrieve(self, query: str, top_k=5):
        """检索相关文档片段"""
        # ① 查询向量化
        query_vec = self.embedder.embed_query(query)
        
        # ② 向量检索
        results = self.vector_store.search(query_vec, top_k=top_k)
        
        return results
    
    def generate(self, query: str, top_k=5) -> str:
        """RAG 完整流程：检索 + 生成"""
        # 检索
        context_chunks = self.retrieve(query, top_k)
        
        # 拼接上下文
        context = "\n\n---\n\n".join([
            f"[来源 {i+1}] {chunk['text']}"
            for i, chunk in enumerate(context_chunks)
        ])
        
        # LLM 生成
        prompt = f"""基于以下参考资料回答问题。如果资料中没有相关信息，请明确说明。

参考资料：
{context}

问题：{query}

回答："""
        
        if self.llm:
            return self.llm(prompt)
        else:
            return f"检索到 {len(context_chunks)} 个相关片段（请配置 LLM 以生成回答）"
```

### 3.2 检索后处理

```python
def post_process_results(results, query, min_score=0.7):
    """检索后处理"""
    
    # ① 相似度过滤
    filtered = [r for r in results if r["score"] >= min_score]
    
    # ② 去重（同一文档的相邻块 → 合并）
    deduped = []
    seen_docs = set()
    for r in filtered:
        doc_id = r["metadata"]["doc_id"]
        if doc_id not in seen_docs:
            deduped.append(r)
            seen_docs.add(doc_id)
    
    # ③ 重排序（可选，用更强的 Cross-Encoder 重排）
    # reranked = cross_encoder_rerank(query, deduped)
    
    return deduped[:5]  # 最终返回 Top-5
```

---

## 4. Embedding 质量对 RAG 精度的影响

### 4.1 量化实验

```text
同一个 RAG 系统，仅切换 Embedding 模型的精度差异：

  模型                    NDCG@5    召回率@5
  ───────────────────────────────────────
  m3e-base (768d)         0.452       0.681
  BGE-M3 (1024d)          0.587       0.823
  Qwen-Embedding (1024d)  0.601       0.841
  text-embedding-3-large  0.623       0.858

结论：Embedding 模型的选择能造成 20%+ 的精度差异！
```

### 4.2 影响精度的关键因素

| 因素 | 影响程度 | 优化建议 |
|------|:---:|------|
| **模型选择** | ⭐⭐⭐⭐⭐ | 中文选 BGE-M3/Qwen-Embedding |
| **切片策略** | ⭐⭐⭐⭐ | 递归切片 + 10% overlap |
| **向量维度** | ⭐⭐⭐ | 512-1024 维是最佳平衡点 |
| **查询改写** | ⭐⭐⭐ | 将口语化问题改写为搜索语句 |
| **混合检索** | ⭐⭐⭐⭐ | Dense + Sparse 融合 |

---

## 5. RAG 完整代码示例

```python
# ===== 最小可运行 RAG 系统 =====
import numpy as np
from openai import OpenAI

class MiniRAG:
    """最小 RAG 系统 — 理解原理用"""
    
    def __init__(self, api_key):
        self.client = OpenAI(api_key=api_key)
        self.chunks = []
        self.vectors = []
    
    def embed(self, text):
        resp = self.client.embeddings.create(
            model="text-embedding-3-small",
            input=[text.replace("\n", " ")]
        )
        return resp.data[0].embedding
    
    def index(self, documents):
        """离线索引"""
        splitter = RecursiveCharacterTextSplitter(
            chunk_size=500, chunk_overlap=50
        )
        for doc in documents:
            chunks = splitter.split_text(doc["content"])
            for chunk in chunks:
                self.chunks.append({
                    "text": chunk,
                    "source": doc["title"]
                })
                self.vectors.append(self.embed(chunk))
        self.vectors = np.array(self.vectors)
        print(f"索引完成：{len(self.chunks)} 个块")
    
    def ask(self, question, top_k=3):
        """在线检索 + 生成"""
        # ① 检索
        q_vec = np.array(self.embed(question))
        scores = np.dot(self.vectors, q_vec) / (
            np.linalg.norm(self.vectors, axis=1) * np.linalg.norm(q_vec)
        )
        top_idx = np.argsort(scores)[-top_k:][::-1]
        
        # ② 构建上下文
        context = "\n\n".join([
            f"[来源:{self.chunks[i]['source']}] {self.chunks[i]['text']}"
            for i in top_idx
        ])
        
        # ③ LLM 生成
        resp = self.client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[{
                "role": "user",
                "content": f"参考资料：\n{context}\n\n问题：{question}"
            }]
        )
        return resp.choices[0].message.content

# 使用
rag = MiniRAG(api_key="sk-xxx")
rag.index([
    {"title": "MySQL 文档", "content": "InnoDB 引擎支持事务..."},
    {"title": "Redis 文档", "content": "Redis 是内存数据库..."},
])
answer = rag.ask("MySQL 的默认存储引擎是什么？")
```

---

## 6. 常见问题与调优

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 检索结果不相关 | Embedding 模型不适配语言/领域 | 换用 BGE-M3 或 Qwen-Embedding |
| 漏检重要信息 | 切片太大 → 向量被稀释 | 减小 chunk_size 到 300-500 |
| 回答包含无关信息 | 检索结果夹杂噪声 | 加相似度阈值过滤（如 min_score=0.7） |
| 领域术语匹配差 | Dense 向量对专有名词弱 | 启用 Sparse 检索（BGE-M3） |
| 首次加载慢 | 未预计算向量 | 启动时加载预计算好的向量文件 |

---

## 核心要点回顾

- Embedding 贯穿 RAG 全链路：离线索引 + 在线检索
- 两个阶段必须使用同一 Embedding 模型
- 检索后处理（过滤、去重、重排序）可显著提升精度
- 模型选择是影响 RAG 精度的最大变量（可差 20%+）
- 混合检索（Dense + Sparse）是精度天花板最高的方案
