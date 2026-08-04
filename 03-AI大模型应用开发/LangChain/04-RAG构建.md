# 04 - RAG 构建

> **核心摘要**：RAG（检索增强生成）是 1.x 最成熟的落地场景——七步流水线（加载→切块→嵌入→存储→检索→重排→生成）。2026 主流方案已从 Naive RAG 演进到 **Agentic RAG / Agentic GraphRAG**（检索决策交给 LLM，2026 WAIC 焦点）。

> **前置阅读**：[[02-LCEL表达式语言]] | [[03-模型与输出解析]]

---

## 📚 目录

1. [RAG 全景与七步流程](#1-rag-全景与七步流程)
2. [文档加载](#2-文档加载)
3. [文本切块](#3-文本切块)
4. [向量化与存储](#4-向量化与存储)
5. [检索策略](#5-检索策略)
6. [重排 Rerank](#6-重排-rerank)
7. [四种方案演进](#7-四种方案演进)
8. [LCEL 完整实现](#8-lcel-完整实现)
9. [评估与优化](#9-评估与优化)
10. [核心要点](#10-核心要点)

---

## 1. RAG 全景与七步流程

> **背景**：LLM 知识截止日期 + 幻觉 + 无私有数据 → RAG 把「外部知识」注入生成过程。
> **目的**：掌握从原始文档到可问答系统的完整流水线。
> **适用范围**：企业知识库、客服、文档问答——RAG 是 LLM 应用最高频生产形态。

```text
RAG 七步流水线（2026 标准）
┌────────────────────────────────────────────────┐
│ ① 加载    DocumentLoader     PDF/Word/网页→Document │
│ ② 切块    TextSplitter       长文档→语义完整小块      │
│ ③ 嵌入    Embeddings        文本→向量（1536/1024 维） │
│ ④ 存储    VectorStore       向量库（Chroma/Milvus…） │
│ ⑤ 检索    Retriever         相似度/MRR → TopK 块      │
│ ⑥ 重排    Reranker          精排 → 去噪保序（可选）    │
│ ⑦ 生成    Prompt|Model      检索块+问题 → 有据回答     │
└────────────────────────────────────────────────┘
                 ↑ ③④ 离线构建索引（一次性）
                 ↑ ①②⑤⑥⑦ 在线问答（每次查询）
```

**RAG 解决的三类问题**：

| 问题 | 说明 | RAG 的对策 |
|------|------|-----------|
| 知识截止 | 模型不知道 2026 新知识 | 检索最新文档 |
| 私有数据 | 企业文档不外传 | 检索本地库，不训练模型 |
| 幻觉 | 模型编造事实 | 答案必须基于检索块（可溯源） |

---

## 2. 文档加载

```python
# 各类加载器（langchain-community / 官方包）
from langchain_community.document_loaders import (
    PyPDFLoader,      # PDF（配合 pypdf）
    TextLoader,       # 纯文本
    WebBaseLoader,    # 网页（配合 BeautifulSoup）
)
from langchain_community.document_loaders import Docx2txtLoader  # Word

# 一个文件 = 一个 Document（含 page_content + metadata）
loader = PyPDFLoader("docs/产品手册.pdf")
docs = loader.load()          # → list[Document]
print(docs[0].metadata)       # {'source': 'docs/产品手册.pdf', 'page': 1, ...}
```

> 💡 **metadata 是检索精度的暗器**：把「来源、页码、章节、作者、时间」写进 metadata，生成时展示引用出处、检索时可按字段过滤（如只检索 2026 年的文档）。

---

## 3. 文本切块

> 🎯 **切块是 RAG 质量的第一决定因素**——块太小语义断裂，块太大噪声稀释相似度。

```python
from langchain_text_splitters import RecursiveCharacterTextSplitter

splitter = RecursiveCharacterTextSplitter(
    chunk_size=800,        # 目标块大小（字符）
    chunk_overlap=100,     # 块间重叠（保语义连续）
    separators=["\n\n", "\n", "。", "！", "？", " ", ""],  # 按优先级切
)
chunks = splitter.split_documents(docs)  # Document → 多个小 Document
```

**切块策略对比**：

| 策略 | 原理 | 适用 | 块大小建议 |
|------|------|------|-----------|
| RecursiveCharacter | 按分隔符递归切 | 通用首选 | 400-1000 字符 |
| Token-based | 按 token 数切 | 精确控制上下文 | 512-1024 token |
| Markdown/HTML | 按标题结构切 | 结构化文档 | 按章节 |
| Semantic | 按语义相似度边界切 | 高质量需求 | 动态 |

**切块调优原则**：

```text
├── 过小（<200）：语义片段碎，检索到「半个概念」
├── 过大（>1500）：噪声多，命中率稀释
├── 重叠：10-20% 为宜，防跨块概念被切断
├── 中文：在 "。" "！" "？" 断句处切（比空格切更贴合）
└── 金句：先定「一句话能回答」的粒度，再反推 chunk_size
```

---

## 4. 向量化与存储

```python
# 嵌入模型（向量维度：OpenAI 3072 / text-embedding-3-small 1536 / BGE 1024）
from langchain_openai import OpenAIEmbeddings

embeddings = OpenAIEmbeddings(model="text-embedding-3-small")

# 向量库（示例 Chroma；生产常用 Milvus/Qdrant/pgvector）
from langchain_chroma import Chroma

vectorstore = Chroma.from_documents(
    documents=chunks,
    embedding=embeddings,
    collection_name="product_docs",
    persist_directory="./chroma_db",   # 本地持久化
)

# 之后复用（不重复嵌入）
vectorstore = Chroma(
    embedding=embeddings,
    collection_name="product_docs",
    persist_directory="./chroma_db",
)
```

**向量库选型**：

| 方案 | 部署 | 适合 | 要点 |
|------|------|------|------|
| Chroma | 嵌入式/本地 | 原型、小规模 | 零运维 |
| FAISS | 嵌入式 | 中等规模 | 单机内存 |
| Milvus | 分布式服务 | 生产大数据量 | 分片/索引完善 |
| pgvector | 装在 Postgres | 已有 PG 的团队 | 与业务数据同库 |
| Elasticsearch | 服务 | 全文+向量混合 | 关键词召回强 |

> ⚠️ **嵌入模型前后必须一致**——索引时用 A 模型、查询时用 B 模型，向量空间不同，检索全废。升级嵌入模型必须**全量重建索引**。

---

## 5. 检索策略

```python
retriever = vectorstore.as_retriever(
    search_type="similarity",     # 相似度（默认）
    search_kwargs={"k": 4},       # TopK
)

# 方案二：MMR（多样性——去重保多样，避免 4 块全讲同一段）
retriever = vectorstore.as_retriever(
    search_type="mmr",
    search_kwargs={"k": 4, "fetch_k": 20, "lambda_mult": 0.7},
)

# 方案三：MultiQuery（拆子问题——"RAG 怎么做" → 多个角度的查询）
from langchain.retrievers.multi_query import MultiQueryRetriever

retriever = MultiQueryRetriever.from_llm(
    retriever=vectorstore.as_retriever(), llm=model,
)

# 方案四：ParentDocument（检索小块、回填大块上下文）
from langchain.retrievers.parent_document_retriever import ParentDocumentRetriever
```

**四种检索策略对比**：

| 策略 | 解决什么问题 | 代价 |
|------|-------------|------|
| Similarity | 基线 | 无 |
| MMR | 结果冗余（4 块同一主题） | 额外计算 |
| MultiQuery | 查询表述不佳 | 多次向量查询 |
| ParentDocument | 小块精确命中但上下文不足 | 存储两套索引 |

> 🎯 **2026 最佳实践**：基础相似度 + ParentDocument + 重排，已能覆盖 90% 生产场景；Agentic RAG 再解决「多源检索、按需检索」问题。

---

## 6. 重排 Rerank

> 🎯 **粗排（向量 TopK=20）→ 精排（重排 TopK=4）**——向量相似 ≠ 语义相关，重排用交叉编码器（cross-encoder）逐对打分，显著提升准确率。

```python
# 粗排取 20 → 精排留 4
retriever = vectorstore.as_retriever(search_kwargs={"k": 20})

# 重排器（Cohere / BGE-reranker / 自建 cross-encoder）
from langchain_cohere import CohereRerank

reranker = CohereRerank(model="rerank-multilingual-v3.0", top_n=4)

# 组装（LCEL 链内使用）
chain = (
    {"context": retriever | reranker, "question": RunnablePassthrough()}
    | prompt
    | model
    | StrOutputParser()
)
```

**为什么重排有效**：

```text
├── 向量检索：双塔模型，query/文档各自编码（快但粗）
├── 交叉编码器：query×文档 拼接打分（慢但准，逐对看全上下文）
├── 权衡：粗排 20 条仅毫秒级，精排 20 条秒级 → 先粗后精
└── 中文场景：多语言重排模型（Cohere v3.0 / bge-reranker-v2-m3）效果显著
```

---

## 7. 四种方案演进

> 🎯 **RAG 已从「流水线」演进到「智能体化」**——决策权逐步交给 LLM：

| 方案 | 架构 | 检索决策 | 适合场景 | 2026 状态 |
|------|------|---------|---------|----------|
| **Naive RAG** | 固定七步流水线 | 无（每次都查） | 单一数据源、问题简单 | 入门基线 |
| **Advanced RAG** | 加重排/多查询/混合检索 | 无 | 生产质量要求高 | 主流标配 |
| **Agentic RAG** | Retriever 封装为 Tool | **LLM 决定查不查、查什么** | 多源、问题依赖上下文 | 推荐方案 |
| **Agentic GraphRAG** | 知识图谱 + 检索 + 反思闭环 | LLM + 图谱推理 | 实体关系密集（金融/科研） | WAIC 2026 焦点 |

**Agentic RAG 核心代码**（Retriever 即工具）：

```python
from langchain.agents import create_agent

# 把检索器封装成工具 → 模型按需决定是否检索
@tool
def search_knowledge(query: str) -> str:
    """当问题涉及公司产品/政策时调用（输出相关文档片段）"""
    docs = retriever.invoke(query)
    return "\n\n".join(d.page_content for d in docs)

agent = create_agent(
    model="openai:gpt-5.5",
    tools=[search_knowledge],
)

# 问「你好」→ 不检索，直接聊；问「退款政策」→ 自动检索再答
```

> ⚠️ **Agentic RAG 的代价**：多一次模型决策轮次（延迟+成本），简单固定场景反而用 Naive 更省——**用复杂度换决策灵活性，按场景取舍**。

---

## 8. LCEL 完整实现

```python
"""完整 RAG 问答链（Advanced RAG 基线，约 30 行）"""
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser
from langchain_openai import ChatOpenAI, OpenAIEmbeddings
from langchain_chroma import Chroma

# ── ① 离线：加载 → 切块 → 嵌入 → 存储（一次性）
from langchain_text_splitters import RecursiveCharacterTextSplitter

splitter = RecursiveCharacterTextSplitter(chunk_size=800, chunk_overlap=100)
chunks = splitter.split_documents(PyPDFLoader("手册.pdf").load())
vectorstore = Chroma.from_documents(chunks, OpenAIEmbeddings(model="text-embedding-3-small"))

# ── ② 在线：检索 → 提示 → 生成（LCEL 链）
PROMPT = ChatPromptTemplate.from_messages([
    ("system", "你是客服助手。只依据【上下文】回答，找不到依据就明确说不知道。\n\n【上下文】\n{context}"),
    ("user", "{question}"),
])

retriever = vectorstore.as_retriever(search_kwargs={"k": 4})
model = ChatOpenAI(model="gpt-5.5", temperature=0.1)

chain = (
    {"context": retriever, "question": RunnablePassthrough()}
    | PROMPT
    | model
    | StrOutputParser()
)

print(chain.invoke("退款流程是什么？"))
```

**LCEL 里 dict 赋值的含义**（常被忽略）：

```python
{"context": retriever, "question": RunnablePassthrough()}
# → 左边是「给模板的变量名」，右边是「取数据的 Runnable」
# → retriever 输出 → context 变量；原输入透传 → question 变量
```

---

## 9. 评估与优化

> 🎯 **RAG 不出效果，先定位再动手**——七步每一环都可能成为瓶颈：

```text
RAG 失败排查表（按出现频率排序）
├── ① 检索没召回对 → 换切块粒度/加重排/换嵌入模型
│    └── 验证：单独跑 retriever.invoke(问题)，肉眼看 TopK 是否相关
├── ② 召回对但答错 → 提示词问题（未强约束"只依据上下文"）
├── ③ 答案对但引用错 → metadata 溯源不完整
├── ④ 全都不行 → 嵌入模型太弱 / 文档本身质量差
└── 金句：先验检索，再调生成——别一上来就改 Prompt
```

**RAG 评估维度**（RAGAS 框架指标）：

| 指标 | 含义 | 改进方向 |
|------|------|---------|
| 忠实度（Faithfulness） | 答案是否忠于上下文 | 提示词约束 + 生成策略 |
| 相关性（Relevance） | 检索块是否切题 | 切块/重排/嵌入 |
| 上下文精度 | 有用块占比 | 重排 + 去噪 |
| 答案完整性 | 是否答全 | 多查询 + 多块融合 |

> 💡 **评估集建设**：30-50 条「问题 + 标准答案 + 对应文档」的黄金集，用 LangSmith 跑回归——每次改切块/提示词后重跑，防劣化。

---

## 10. 核心要点

> 🎯 **核心要点**：
> 1. **七步流水线**：加载→切块→嵌入→存储→检索→重排→生成
> 2. **切块是质量第一决定因素**：400-1000 字符 + 10-20% 重叠 + 中文断句
> 3. **嵌入模型必须前后一致**，升级需全量重建索引
> 4. **粗排+精排组合**：向量 TopK=20 → Rerank TopK=4
> 5. **方案演进**：Naive → Advanced → Agentic → Agentic GraphRAG——决策权逐步交给 LLM
> 6. **Agentic RAG** = Retriever 封装 Tool + create_agent，按需检索
> 7. **评估先行**：先验检索再调生成，用 RAGAS + 黄金集防劣化

---

**下一模块**：[05-Agent开发](05-Agent开发.md) | **返回总览**：[00-LangChain知识体系总览](00-LangChain知识体系总览.md)
