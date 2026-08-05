# 02 - RAG 技术栈全景

> 🎯 RAG 技术栈 = LangChain/LlamaIndex（编排）+ 向量数据库（存储）+ Embedding（转向量）+ LLM（生成）。选对组合，事半功倍

---

## 目录

1. [技术栈全景图](#1-技术栈全景图)
2. [编排框架对比](#2-编排框架对比)
3. [向量数据库选型](#3-向量数据库选型)
4. [Embedding 模型](#4-embedding-模型)
5. [推荐组合](#5-推荐组合)

---

## 1. 技术栈全景图

```text
RAG 技术栈四层：

┌─────────────────────────────────────────────────┐
│  应用层：LangChain / LlamaIndex / Haystack       │  编排框架
│          → 串联切片→Embedding→检索→生成          │
├─────────────────────────────────────────────────┤
│  检索层：FAISS / Milvus / Chroma / Pinecone      │  向量数据库
│          → 存储向量 + 相似度检索                  │
├─────────────────────────────────────────────────┤
│  向量层：BGE-M3 / Qwen-Embedding / OpenAI        │  Embedding
│          → 文本→向量 + 语义匹配                   │
├─────────────────────────────────────────────────┤
│  生成层：GPT-4o / Qwen / DeepSeek / LLaMA        │  LLM
│          → 基于检索结果生成回答                    │
└─────────────────────────────────────────────────┘
```

---

## 2. 编排框架对比

| 框架 | 定位 | 优势 | 劣势 |
|------|------|------|------|
| **LangChain** | 通用 LLM 应用框架 | 生态最大、组件最全、社区活跃 | 抽象层多、学习曲线陡 |
| **LlamaIndex** | 专注数据索引+RAG | RAG 开箱即用、索引结构丰富 | 不如 LangChain 灵活 |
| **Haystack** | 企业级 NLP Pipeline | Pipeline 设计清晰、生产级 | 社区较小 |

```python
# LangChain 最小 RAG
from langchain_community.document_loaders import TextLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_community.vectorstores import Chroma
from langchain_community.embeddings import OllamaEmbeddings

# ① 加载文档
loader = TextLoader("docs.txt")
docs = loader.load()

# ② 切片
splitter = RecursiveCharacterTextSplitter(chunk_size=500, chunk_overlap=50)
chunks = splitter.split_documents(docs)

# ③ 向量化 + 入库
embeddings = OllamaEmbeddings(model="bge-m3")
vectorstore = Chroma.from_documents(chunks, embeddings)

# ④ 检索 + 生成
retriever = vectorstore.as_retriever(search_kwargs={"k": 5})
# → 配合 LLM 生成回答
```

```python
# LlamaIndex 最小 RAG（更简洁的 API）
from llama_index.core import VectorStoreIndex, SimpleDirectoryReader

documents = SimpleDirectoryReader("docs/").load_data()
index = VectorStoreIndex.from_documents(documents)
query_engine = index.as_query_engine()
response = query_engine.query("公司年假政策是什么？")
```

---

## 3. 向量数据库选型

| 方案 | 类型 | 适用规模 | 部署 | 推荐场景 |
|------|:---:|:---:|------|----------|
| **Chroma** | 嵌入式 | <10万 | 本地进程 | **原型/小项目首选** |
| **FAISS** | 嵌入式库 | <100万 | 本地进程 | 嵌入式高性能 |
| **Milvus** | 独立服务 | 亿级 | Docker/K8s | **生产推荐** |
| **Pinecone** | 云服务 | 不限 | SaaS | 零运维 |
| **Qdrant** | 独立服务 | 百万级 | Docker/K8s | 过滤功能强 |
| **Weaviate** | 独立服务 | 百万级 | Docker/K8s | 自带向量化 |

### 选型决策树

```text
选择向量数据库：

  原型/MVP → Chroma（最简单）
  生产/大数据 → Milvus（最成熟）
  零运维 → Pinecone（SaaS）
  需复杂过滤 → Qdrant / Weaviate
  极致性能+嵌入式 → FAISS
```

---

## 4. Embedding 模型

```text
中文 RAG 推荐 Embedding：
  ① BGE-M3 (BAAI)：Dense+Sparse 混合，中文首选
  ② Qwen-Embedding (阿里)：超长文本 32K，Qwen 生态
  ③ text-embedding-3 (OpenAI)：通用商业，简单可靠

详见 Embedding/ 目录
```

---

## 5. 推荐组合

| 场景 | 推荐组合 | 说明 |
|------|----------|------|
| **个人学习** | LangChain + Chroma + BGE-M3(Ollama) + Qwen | 全部本地、免费 |
| **创业 MVP** | LlamaIndex + Pinecone + text-embedding-3 + GPT-4o-mini | 快速上线、零运维 |
| **企业生产** | LangChain + Milvus + BGE-M3 + Qwen-72B | 可控、高性能 |
| **高精度** | LangChain + Milvus + text-embedding-3-large + GPT-4o | 精度优先 |

---

## 核心要点回顾

- RAG 四层技术栈：编排框架 + 向量库 + Embedding + LLM
- LangChain 生态最大，LlamaIndex RAG 最开箱即用
- Chroma 适合原型，Milvus 适合生产，Pinecone 适合零运维
- 中文 RAG：BGE-M3(Ollama本地) 或 text-embedding-3(OpenAI云端)
