# 03 快速上手：第一个 RAG 应用

> 从零跑通 LlamaIndex：安装（llama-index 核心 + 集成包）、最小 RAG（VectorStoreIndex 四行建库 + QueryEngine 一行查询）、代码全解——体验"基础 RAG 代码比 LangChain 少 30-40%"。

## 📚 目录

1. [安装与环境](#1-安装与环境)
2. [最小 RAG 应用](#2-最小-rag-应用)
3. [代码全解](#3-代码全解)
4. [配置模型（OpenAI 兼容）](#4-配置模型openai-兼容)
5. [运行验证](#5-运行验证)
6. [常见报错排查](#6-常见报错排查)
7. [面试高频问法](#7-面试高频问法)
8. [扩展练习](#8-扩展练习)
9. [与手写 RAG 的对照](#9-与手写-rag-的对照)

## 1. 安装与环境

### 安装命令

```bash
# 核心库
pip install llama-index

# 按需集成（llama-index-integrations-*）
pip install llama-index-vector-stores-qdrant        # Qdrant
pip install llama-index-llms-openai                 # OpenAI 兼容 LLM
pip install llama-index-embeddings-openai           # OpenAI Embedding
pip install llama-index-embeddings-huggingface      # 本地 BGE
```

### 包结构

```
llama-index（核心：索引/查询引擎/Workflows）
llama-index-integrations-*（按需集成：LLM/存储/读取器）
llama-index-core（纯核心，无默认模型）
```

> 包名规律：`llama-index-<类别>-<厂商>`（llms/embeddings/vector-stores/readers）。

## 2. 最小 RAG 应用

```python
"""LlamaIndex 最小 RAG：建库 + 查询"""
from llama_index.core import SimpleDirectoryReader, VectorStoreIndex

# 1. 读取文档（连接器）
documents = SimpleDirectoryReader("./data").load_data()

# 2. 建索引（解析 → 嵌入 → 索引，一条语句）
index = VectorStoreIndex.from_documents(documents)

# 3. 查询引擎（检索 → 生成）
query_engine = index.as_query_engine()

# 4. 查询
response = query_engine.query("产品的退款政策是什么？")
print(response.response)
print("\n来源：")
for node in response.source_nodes:
    print(f"- {node.metadata.get('file_name', '?')} (分数 {node.score:.3f})")
```

**4 行核心代码完成 RAG**——这是 LlamaIndex"少 30-40% 代码"的直观体现。

## 3. 代码全解

| 行 | 做了什么 | 对应 RAG 阶段 2 |
|---|---|---|
| SimpleDirectoryReader | 多格式读取 | 文档加载 |
| from_documents | 切分+嵌入+建索引 | 切片+向量化+入库 |
| as_query_engine | 检索+生成封装 | ask() 函数 |
| query() | 执行查询 | 检索→Prompt→生成 |

### 默认配置说明

```
默认嵌入：OpenAI text-embedding-ada-002（需 OPENAI_API_KEY）
默认存储：内存（生产换 Qdrant/LlamaCloud）
默认 LLM：OpenAI（可换 DeepSeek 等 OpenAI 兼容）
```

## 4. 配置模型（OpenAI 兼容）

```python
from llama_index.core import Settings
from llama_index.llms.openai import OpenAI
from llama_index.embeddings.openai import OpenAIEmbedding

# DeepSeek（OpenAI 兼容）
Settings.llm = OpenAI(
    model="deepseek-chat",
    api_base="https://api.deepseek.com",   # 关键：兼容端点
    api_key="YOUR_KEY",
)

# 本地 BGE 嵌入（中文场景）
from llama_index.embeddings.huggingface import HuggingFaceEmbedding
Settings.embed_model = HuggingFaceEmbedding(model_name="BAAI/bge-small-zh-v1.5")

# 全局 Settings 生效（一次配置全链路）
```

### Settings 机制

```
Settings = LlamaIndex 的全局配置中心
llm / embed_model / node_parser / 等
一次设置，全框架生效（不用逐处传参）
```

## 5. 运行验证

### 验证清单

| 项 | 检查 |
|---|---|
| 建库 | 无报错；文档数可见（index.ref_doc_info） |
| 查询 | 回答有依据（不是瞎编） |
| 溯源 | source_nodes 有来源（file_name + 分数） |
| 同义改写 | 换问法仍能命中（语义检索） |
| 库外问题 | 观察行为（Prompt 约束生效与否） |

### 调试打印

```python
# 先看检索再谈生成（分层定位，RAG 阶段 2 纪律）
retriever = index.as_retriever(similarity_top_k=5)
nodes = retriever.retrieve("退款政策")
for n in nodes:
    print(f"{n.score:.3f} {n.text[:60]}...")
```

## 6. 常见报错排查

| 报错 | 原因 | 修复 |
|---|---|---|
| OPENAI_API_KEY not found | 默认模型要 Key | 配 Settings（DeepSeek/本地） |
| ModuleNotFoundError | 集成包未装 | pip install llama-index-* |
| 索引为空 | 目录没文档/格式不支持 | 检查 data 目录与连接器 |
| 检索结果差 | 嵌入模型与文档不匹配 | 中文换 BGE |
| 回答瞎编 | Prompt 约束弱 | 自定义 query_engine prompt |
| Workflows 超时 | 步骤耗时 | timeout 参数调大 |

### 排错三问

```
① 模型配了吗？（Settings.llm / embed_model）
② 数据进了吗？（SimpleDirectoryReader 读到了吗）
③ 检索对吗？（先 retriever 单独测）
```

## 7. 面试高频问法

| 问题 | 回答要点 |
|---|---|
| 最小 RAG 几行？ | 4 行（读取/建索引/引擎/查询） |
| Settings 是什么？ | 全局配置（llm/embed_model 一次设置全链路） |
| 怎么换 DeepSeek？ | Settings.llm = OpenAI(model, api_base=deepseek) |
| 默认模型要 Key 吗？ | 要（OpenAI 默认）；可全换本地/兼容 |
| 怎么溯源？ | response.source_nodes |
| 比 LangChain 少代码？ | 基础 RAG 少 30-40%（封装深度） |

### 面试加分表达

> "LlamaIndex 的 RAG 上手是 4 行代码：读取、建索引、查询引擎、查询——比 LangChain 同类少 30-40%。关键在 Settings 全局配置：一次配好 llm 和 embed_model（DeepSeek 兼容端点 + 中文 BGE），全链路生效。溯源靠 source_nodes 自带。"

## 8. 扩展练习

| 练习 | 内容 | 掌握 |
|---|---|---|
| 1 | 换 Qdrant 存储（vector-stores-qdrant） | 存储抽象 |
| 2 | 加 SentenceTransformerRerank 精排 | 重排（04 篇） |
| 3 | 用 HierarchicalNodeParser 层级分块 | 父子索引 |
| 4 | 自定义 text_qa_template 强约束 | Prompt 工程 |
| 5 | 跑 FaithfulnessEvaluator 评估 | 内置评估 |
| 6 | Workflows 封装查询流程 | 事件驱动 |

## 9. 与手写 RAG 的对照

| 手写环节（RAG 阶段 2） | LlamaIndex |
|---|---|
| 文档加载（load_text） | SimpleDirectoryReader |
| 切片（fixed_chunk） | NodeParser（内置多种） |
| Embedding（model.encode） | Settings.embed_model |
| 向量库（FAISS add） | Index（from_documents） |
| 检索（index.search） | retriever（as_retriever） |
| Prompt 组装（build_prompt） | text_qa_template |
| LLM 生成（generate） | Settings.llm |
| 溯源（meta 打印） | source_nodes |

> 对照结论：**每一行手写代码都有 LlamaIndex 对应物**——原理不变，封装升级；先手写（阶段 2）再框架（本体系），理解不会丢。

> 🎯 核心要点：最小 RAG = 4 行（读取/建索引/引擎/查询）；Settings 是全局配置中心（一次配置全链路）；DeepSeek 走 OpenAI 兼容（api_base 指向 deepseek.com）；中文嵌入换 BGE；排错三问（模型/数据/检索）；代码量少 30-40% 是它的封装红利；六练习 + 手写对照表加深理解。

---

**下一模块**：[04-索引与查询引擎](04-索引与查询引擎.md) / **返回总览**：[00-LlamaIndex知识体系总览](00-LlamaIndex知识体系总览.md)
