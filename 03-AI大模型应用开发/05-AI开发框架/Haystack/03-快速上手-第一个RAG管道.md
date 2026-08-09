# 03 快速上手：第一个 RAG 管道

> 从零跑通 Haystack 2.x：安装（haystack-ai + integrations）、索引管道 + 查询管道两个最小 RAG 管道、代码全解——体验"构建期校验"与"管道即图"。

## 📚 目录

1. [安装与环境](#1-安装与环境)
2. [最小索引管道](#2-最小索引管道)
3. [最小查询管道（RAG）](#3-最小查询管道rag)
4. [代码全解](#4-代码全解)
5. [运行验证](#5-运行验证)
6. [常见报错排查](#6-常见报错排查)
7. [面试高频问法](#7-面试高频问法)
8. [扩展练习](#8-扩展练习)

## 1. 安装与环境

### 安装命令

```bash
pip install haystack-ai

# 按需集成（独立包）
pip install haystack-ai "haystack-integrations-integrations-elasticsearch"
pip install "haystack-integrations-llms-openai"       # OpenAI
pip install "haystack-integrations-llms-ollama"       # 本地模型
pip install "haystack-integrations-embeddings-sentence-transformers"
pip install "haystack-integrations-vector-stores-qdrant"
```

### 依赖选择

| 需求 | 包 |
|---|---|
| 核心框架 | haystack-ai |
| 本地向量库快速验证 | chromadb / qdrant integrations |
| LLM | OpenAI/DeepSeek（openai 兼容）或 Ollama 本地 |
| Embedding | sentence-transformers（本地） |

> 包名注意：2.x 是 `haystack-ai`；1.x 是 `farm-haystack`（维护模式）——教程对不上版本时先查这个。

## 2. 最小索引管道

```python
"""索引管道：文档 → 转换 → 分割 → 嵌入 → 写入存储"""
from haystack import Pipeline
from haystack.components.converters import TextFileToDocument
from haystack.components.preprocessors import DocumentSplitter
from haystack.components.writers import DocumentWriter
from haystack.document_stores.in_memory import InMemoryDocumentStore
from haystack_integrations.components.embeddings.sentence_transformers import (
    SentenceTransformersDocumentEmbedder,
)

# 1. 文档存储（原型用内存；生产换 ES/Qdrant/pgvector）
document_store = InMemoryDocumentStore()

# 2. 构建索引管道
indexing = Pipeline()
indexing.add_component("converter", TextFileToDocument())
indexing.add_component("splitter", DocumentSplitter(split_by="word", split_length=200))
indexing.add_component("embedder", SentenceTransformersDocumentEmbedder(model="BAAI/bge-small-zh-v1.5"))
indexing.add_component("writer", DocumentWriter(document_store))

# 3. 连接（构建期校验）
indexing.connect("converter.documents", "splitter.documents")
indexing.connect("splitter.documents", "embedder.documents")
indexing.connect("embedder.documents", "writer.documents")

# 4. 运行
indexing.run({"converter": {"sources": ["docs/产品说明.txt"]}})
print(f"索引完成，存储文档数：{document_store.count_documents()}")
```

## 3. 最小查询管道（RAG）

```python
"""查询管道：问题 → 嵌入 → 检索 → 拼提示 → LLM 生成"""
from haystack import Pipeline
from haystack.components.builders import PromptBuilder
from haystack_integrations.components.retrievers.chroma import ChromaEmbeddingRetriever
from haystack_integrations.components.embeddings.sentence_transformers import (
    SentenceTransformersTextEmbedder,
)
from haystack_integrations.components.llms.openai import OpenAIGenerator

# 提示模板
prompt_template = """根据【参考资料】回答问题，不要编造；没有则说明"知识库中没有"。

【参考资料】
{% for doc in documents %}
[资料{{ loop.index }}] {{ doc.content }}
{% endfor %}

【问题】
{{ query }}"""

# 构建查询管道
qa = Pipeline()
qa.add_component("embedder", SentenceTransformersTextEmbedder(model="BAAI/bge-small-zh-v1.5"))
qa.add_component("retriever", ChromaEmbeddingRetriever(document_store=document_store, top_k=5))
qa.add_component("prompt", PromptBuilder(template=prompt_template))
qa.add_component("llm", OpenAIGenerator(
    model="deepseek-chat",
    api_base_url="https://api.deepseek.com",   # OpenAI 兼容
    api_key="YOUR_KEY",
))

# 连接
qa.connect("embedder.embedding", "retriever.query_embedding")
qa.connect("retriever.documents", "prompt.documents")
qa.connect("prompt", "llm")

# 运行
result = qa.run({"embedder": {"text": "产品的退款政策是什么？"}})
print(result["llm"]["replies"][0])
```

## 4. 代码全解

### 索引管道（离线）

| 组件 | 职责 | 输出 |
|---|---|---|
| TextFileToDocument | 文本文件 → Document | documents |
| DocumentSplitter | 文档分割（word=200） | documents |
| SentenceTransformersDocumentEmbedder | 文档向量化 | documents（带 embedding） |
| DocumentWriter | 写入存储 | — |

### 查询管道（在线）

| 组件 | 职责 |
|---|---|
| SentenceTransformersTextEmbedder | 问题向量化 |
| ChromaEmbeddingRetriever | 向量检索 Top-5 |
| PromptBuilder | 模板拼上下文（Jinja2 语法） |
| OpenAIGenerator | LLM 生成（DeepSeek 兼容） |

### 关键连接（管道即图）

```
索引：converter → splitter → embedder → writer
查询：embedder → retriever → prompt → llm
```

### 与阶段 2 手写 RAG 的映射

| 手写（RAG 阶段 2） | Haystack 组件 |
|---|---|
| 文档加载 | TextFileToDocument |
| 切片 | DocumentSplitter |
| Embedding | SentenceTransformers*Embedder |
| 向量库 | DocumentStore（Chroma 等） |
| 检索 | Retriever |
| Prompt 组装 | PromptBuilder |
| LLM 生成 | Generator |

> 理解：Haystack 把 RAG 阶段 2 的每个环节封装为组件——**原理完全一致，框架提供生产化外壳**（校验/序列化/观测）。

## 5. 运行验证

### 验证清单

| 项 | 检查 |
|---|---|
| 索引管道 | 文档数 > 0（count_documents） |
| 构建期校验 | 连接错误在运行前抛出（好现象） |
| 检索命中 | 打印 retriever 的 documents 检查相关性 |
| 生成引用 | 回答有依据（来自资料） |
| 拒答生效 | 库外问题触发"知识库中没有" |

### 调试打印

```python
# 单独看检索结果（先验证检索再调生成）
result = qa.run({"embedder": {"text": "问题"}},
                include_outputs_from={"retriever"})
for doc in result["retriever"]["documents"]:
    print(f"分数={doc.score:.3f} 内容={doc.content[:50]}...")
```

## 6. 常见报错排查

| 报错 | 原因 | 修复 |
|---|---|---|
| Component ... not connected | 组件未连接 | 检查 connect 完整性 |
| Cannot connect ... type mismatch | 类型不匹配 | 看输出/输入类型（构建期校验报错） |
| 模型下载失败 | HF 网络 | 镜像或本地路径 |
| API key 错误 | 密钥/端点 | 检查 OpenAIGenerator 配置 |
| 检索结果为空 | 索引未跑/存储为空 | 先跑索引管道 |
| farm-haystack 教程对不上 | 1.x/2.x 混淆 | 确认包名 haystack-ai |

### 排错三问

```
① 索引跑了吗？（存储里有文档？）
② 连接对吗？（构建期报错看类型）
③ 模型通了吗？（单独调 Generator）
```

## 7. 面试高频问法

| 问题 | 回答要点 |
|---|---|
| 安装命令？ | pip install haystack-ai + integrations 按需 |
| 两个管道怎么分？ | 索引（离线）+ 查询（在线），共享存储 |
| 与手写 RAG 关系？ | 原理一致，组件封装 + 生产化外壳 |
| PromptBuilder 模板？ | Jinja2 语法，documents 循环 |
| 怎么换 DeepSeek？ | OpenAIGenerator + api_base_url |
| 检索不对先查什么？ | 先单独看 retriever 输出（分层定位） |

### 面试加分表达

> "Haystack 的 RAG 最小闭环是两个管道：索引管道（转换→分割→嵌入→写入）和查询管道（嵌入→检索→拼提示→生成），共享同一个 DocumentStore。每个环节都是标准组件——我把手写 RAG 的每个函数换成组件，就拿到了构建期校验和 YAML 序列化的生产能力。"

## 8. 扩展练习

| 练习 | 内容 | 掌握 |
|---|---|---|
| 1 | 换成 Qdrant/ES 存储（04 篇） | DocumentStore 抽象 |
| 2 | 加 MultiRetriever 混合检索 | 混合 + RRF |
| 3 | 加 CrossEncoderRanker 精排 | 粗召回+精排 |
| 4 | 管道 dumps 成 YAML（07 篇） | 序列化 |
| 5 | 加自定义组件（查询改写） | 自定义能力 |
| 6 | 用 Agent 包装检索（06 篇） | Agent 化 RAG |

### 练习验证

```
每个练习完成后跑同一测试问题集：
① 检索命中率变化
② 回答质量变化
③ 管道结构（图）变化
记录在"累积提升表"（RAG 阶段 3 方法）
```

> 🎯 核心要点：安装 haystack-ai + integrations 按需；两个最小管道（索引/查询）共享存储；组件与手写 RAG 一一映射（原理不变、外壳生产化）；构建期校验让连接错误运行前暴露；排错顺序"索引→连接→模型"三问；六个扩展练习把检索调优全部组件化。

---

**下一模块**：[04-检索与文档存储](04-检索与文档存储.md) / **返回总览**：[00-Haystack知识体系总览](00-Haystack知识体系总览.md)
