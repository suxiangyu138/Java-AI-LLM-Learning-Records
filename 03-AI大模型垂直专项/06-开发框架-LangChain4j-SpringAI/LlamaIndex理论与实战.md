# LlamaIndex 理论与实战

> **定位**：专为 LLM 设计的数据框架，充当 LLM 与私有数据之间的"桥梁"。核心价值：让 LLM 从"通用问答工具"升级为"懂私有数据的专属助手"。

---

## 目录

1. [核心定位与价值](#1-核心定位与价值)
2. [核心理论](#2-核心理论)
3. [核心组件](#3-核心组件)
4. [实战入门](#4-实战入门)
5. [实战进阶](#5-实战进阶)
6. [企业级案例与常见问题](#6-企业级案例与常见问题)

---

## 1. 核心定位与价值

### 1.1 三大核心场景

| 场景 | 说明 | 典型案例 |
|------|------|----------|
| **多源异构数据统一检索** | PDF + 数据库 + API 统一检索 | 金融投研：同时查研报 PDF + SQL 财务库 + 新闻 API |
| **长文档深度检索** | 千页手册/财报精准定位 | 汽车维修手册：输入故障码→分层排查步骤 |
| **复杂问题分步推理** | 拆解子问题→分别检索→整合答案 | 市场分析：自动拆分、检索、整合为结构化报告 |

### 1.2 LlamaIndex vs LangChain

| 维度 | LlamaIndex | LangChain |
|------|-----------|-----------|
| **核心定位** | 数据框架，专注数据摄入→索引→检索全生命周期 | 通用 LLM 编排工具，专注"链"与"代理"编排 |
| **核心优势** | 多源数据处理、复杂索引、精准检索 | 流程编排灵活、生态丰富、多 Agent |
| **适用场景** | 私有知识库、长文档检索、多源关联查询 | 多工具联动、Agent 开发、复杂工作流 |

> 简单选型：私有数据处理 + 精准检索 → LlamaIndex；灵活编排 LLM 与多工具 → LangChain。两者可结合使用。

---

## 2. 核心理论

### 2.1 三大核心理论

| 理论 | 核心思想 | 解决的问题 |
|------|----------|-----------|
| **向量空间模型（VSM）** | 文本→向量→语义相似度检索 | 非关键词匹配，捕捉语义关系 |
| **文档原子化拆分** | Document → Node（500-1000 字符片段），元数据维护上下文关联 | 适配 LLM 上下文窗口限制 |
| **RAG 理论** | 检索先行→将上下文+查询→输入 LLM 生成 | 解决幻觉问题，答案可追溯验证 |

### 2.2 五大索引类型

| 索引 | 机制 | 适用场景 | 复杂度 |
|------|------|----------|:---:|
| **VectorStoreIndex** ⭐ | Node→向量嵌入→相似度匹配 Top-k | 标准问答、语义搜索（90% 场景） | 低 |
| **SummaryIndex** | Node 顺序列表→遍历或 LLM 合成摘要 | 文档摘要、全篇综合查询 | 低 |
| **TreeIndex** | 分层树状→父节点摘要，自顶向下遍历 | 长文档层次化查询 | 中 |
| **KeywordTableIndex** | 关键词→倒排索引→精确匹配 | 精确术语查询（零件号、法条） | 低 |
| **PropertyGraphIndex** | 向量+知识图谱，节点属性+边关系 | 复杂推理、混合检索 | 高 |

---

## 3. 核心组件

```text
数据摄入 → 索引构建 → 检索 → 查询/聊天引擎 → LLM 生成答案
    │           │         │          │
  Reader     Indexes  Retrievers  Query/Chat Engine
  Parser
```

| 组件 | 核心模块 | 作用 |
|------|----------|------|
| **数据摄入** | SimpleDirectoryReader + LlamaParse | 多源数据统一加载解析（300+ 连接器） |
| **索引** | VectorStoreIndex 等五种 | Node→结构化存储 |
| **检索** | 相似性/分层/混合/知识图谱四种策略 | 精准获取 Top-k 相关 Node |
| **查询引擎** | Query Engine（无状态）/ Chat Engine（有状态） | 单次问答 / 多轮对话 |
| **辅助** | Embedding Models / LLM / 向量数据库 | 支撑全流程 |

---

## 4. 实战入门

### 4.1 环境搭建

```bash
pip install llama-index-core llama-index-readers-file openai chromadb pypdf
```

```python
import os
os.environ["OPENAI_API_KEY"] = "你的Key"
```

### 4.2 构建 PDF 知识库

```python
from llama_index.core import SimpleDirectoryReader, VectorStoreIndex
from llama_index.vector_stores.chroma import ChromaVectorStore
import chromadb

# ① 加载 PDF
documents = SimpleDirectoryReader(
    input_dir="./docs",
    file_extractor={"pdf": "PyPDFReader"}
).load_data()

# ② 创建向量存储
chroma_client = chromadb.PersistentClient(path="./chroma_db")
chroma_collection = chroma_client.get_or_create_collection("pdf_kb")
vector_store = ChromaVectorStore(chroma_collection=chroma_collection)

# ③ 构建索引
index = VectorStoreIndex.from_documents(
    documents, vector_store=vector_store, show_progress=True)

# ④ 查询
query_engine = index.as_query_engine(similarity_top_k=3)
response = query_engine.query("请总结该PDF的核心内容")
print(response.response)
# 查看来源
for node in response.source_nodes:
    print(f"相似度: {node.score:.4f} | {node.node.text[:200]}...")
```

### 4.3 多源数据（PDF + CSV）

```python
reader = SimpleDirectoryReader(
    input_dir="./docs",
    file_extractor={"pdf": "PyPDFReader", "csv": "PandasCSVReader"}
)
documents = reader.load_data()
# 后续索引构建和查询与单文档一致
```

---

## 5. 实战进阶

### 5.1 Node 拆分优化

```python
from llama_index.core.node_parser import SentenceSplitter

node_parser = SentenceSplitter(
    chunk_size=500,        # 片段字符数
    chunk_overlap=50,      # 相邻片段重叠字符
    separator="。"         # 中文分隔符
)
nodes = node_parser.get_nodes_from_documents(documents)
index = VectorStoreIndex(nodes, vector_store=vector_store)
```

### 5.2 替换开源模型（脱离 OpenAI）

```python
from llama_index.core import Settings
from llama_index.embeddings.huggingface import HuggingFaceEmbedding

Settings.embed_model = HuggingFaceEmbedding(
    model_name="BAAI/bge-large-zh-v1.5", embed_batch_size=10)
# 后续构建索引和查询引擎代码不变
```

### 5.3 混合检索（向量 + 关键词）

```python
from llama_index.core.retrievers import HybridRetriever

vector_retriever = VectorIndexRetriever(index=index, similarity_top_k=3)
keyword_retriever = KeywordTableRetriever(index=index, top_k=3)
hybrid_retriever = HybridRetriever(
    vector_retriever=vector_retriever,
    keyword_retriever=keyword_retriever,
    alpha=0.7  # 向量检索权重
)
query_engine = index.as_query_engine(retriever=hybrid_retriever)
```

| 优化方向 | 方法 |
|----------|------|
| Node 拆分 | 调整 `chunk_size`/`chunk_overlap`/`separator` |
| 模型替换 | BGE（中文）/ OpenAI Embeddings（英文） |
| 检索策略 | 混合检索 / 知识图谱索引 |
| 生产部署 | Pinecone/Milvus 替代本地 Chroma + Docker/K8s |

---

## 6. 企业级案例与常见问题

### 典型案例

| 行业 | 场景 | 效果 |
|------|------|------|
| **金融** | 智能投研：10 万+ 文档自动分析生成报告 | 解析准确率 98.7%，效率提升 300% |
| **医疗** | 临床决策支持：200 万+ 医学文档 | 覆盖 50+ 专科，多模态辅助诊断 |
| **制造** | 设备维护知识库 | 故障率降 45%，维修效率提升 50% |

### 常见问题

| 问题 | 原因 | 解决 |
|------|------|------|
| 检索不准确 | Node 拆分不合理/策略不当 | 调整 `chunk_size` + 混合检索 + 换适配嵌入模型 |
| 响应慢 | 向量库性能不足 | 换 Pinecone/Milvus + 异步批量处理 |
| 特殊格式加载失败 | 解析器不支持 | 用 LlamaParse 处理复杂 PDF + OCR |
| 开源模型不如 OpenAI | 参数规模小 | 换大模型（Llama 70B）+ 私有数据微调 |

---

> 🎯 **核心总结**：LlamaIndex 不是 LLM 替代者，而是 LLM 的"数据增强工具"。核心竞争力 = 数据处理 + 检索精准度。VectorStoreIndex 覆盖 90% 场景；进阶用混合检索 + 知识图谱索引。
