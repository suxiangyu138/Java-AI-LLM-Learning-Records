# 05 生成与 RAG 模式

> 三种生产级 RAG 模式：标准链路、索引/查询管道分离、管道内嵌 Agent——PromptBuilder 管模板、Generator 管模型调用、模式选型管架构。

## 📚 目录

1. [Generator 生成组件](#1-generator-生成组件)
2. [PromptBuilder 模板](#2-promptbuilder-模板)
3. [模式一：标准 RAG 链路](#3-模式一标准-rag-链路)
4. [模式二：索引/查询分离](#4-模式二索引查询分离)
5. [模式三：管道内嵌 Agent](#5-模式三管道内嵌-agent)
6. [模式选型](#6-模式选型)
7. [面试高频问法](#7-面试高频问法)
8. [模式二生产落地示例](#8-模式二生产落地示例)
9. [常见误区](#9-常见误区)

## 1. Generator 生成组件

### 常用 Generator

| Generator | 用途 |
|---|---|
| OpenAIGenerator | OpenAI 兼容（含 DeepSeek） |
| AnthropicGenerator | Claude |
| HuggingFaceLocalGenerator | 本地模型 |
| OllamaGenerator | Ollama 本地 |

### 配置示例

```python
from haystack_integrations.components.llms.openai import OpenAIGenerator

llm = OpenAIGenerator(
    model="deepseek-chat",
    api_base_url="https://api.deepseek.com",   # OpenAI 兼容端点
    api_key="YOUR_KEY",
    generation_kwargs={
        "temperature": 0.3,     # RAG 忠实引用（阶段 2 纪律）
        "max_tokens": 1024,
    },
)
```

### 生成参数纪律（回顾阶段 2/3）

```
RAG 问答：temperature 0.3 以下（忠实引用）
工具调用：需要结构化输出（json_schema）
成本控制：max_tokens 限制 + 模型分层
```

## 2. PromptBuilder 模板

### Jinja2 模板

```python
from haystack.components.builders import PromptBuilder

prompt_template = """你是基于知识库的问答助手。
严格依据【参考资料】回答，不要编造；没有则回答"知识库中没有相关内容"。

【参考资料】
{% for doc in documents %}
[资料{{ loop.index }}]（来源：{{ doc.meta.get('title', '未知') }}）
{{ doc.content }}
{% endfor %}

【问题】
{{ query }}
【回答】"""

prompt = PromptBuilder(template=prompt_template)
```

### 模板能力

| 能力 | 语法 |
|---|---|
| 循环 | {% for doc in documents %} |
| 条件 | {% if ... %} |
| 变量 | {{ query }} / {{ doc.content }} |
| 元数据 | {{ doc.meta.title }} |

### 模板设计原则（阶段 2 的 06 篇）

```
① 强约束："只依据资料/没有就明说"
② 来源标注：meta.title 带出处
③ 结构化：资料编号（模型可引用"据资料2"）
```

## 3. 模式一：标准 RAG 链路

### 结构

```
query → embedder → retriever → prompt → llm → answer
```

### 适用

```
单管道完成问答（查询时构建）
适合：简单问答服务、快速上线
```

### 局限

```
索引与查询耦合在同一管道 —— 生产要分离（模式二）
```

## 4. 模式二：索引/查询分离

### 结构

```
索引管道（离线）：converter → splitter → embedder → writer
查询管道（在线）：embedder → retriever → prompt → llm
共享：同一个 DocumentStore
```

### 为什么分离（生产关键）

| 理由 | 说明 |
|---|---|
| 解耦部署 | 索引慢任务不进查询链路 |
| 独立扩展 | 索引批处理 vs 查询实时 |
| 增量更新 | 索引管道按需重跑 |
| 故障隔离 | 索引挂了不影响查询（存量数据可答） |

### 工程形态

```
索引：定时任务/CronJob（离线跑）
查询：在线服务（低延迟）
共享存储：DocumentStore（ES/Qdrant 等）
```

> 对应 RAG 阶段 5 的"在线/离线链路分离"——Haystack 用两个管道天然实现。

## 5. 模式三：管道内嵌 Agent

### 结构

```
Agent 作为管道中的一个节点
Agent 可调用工具：Retriever（组件作工具）、Python 函数、MCP
```

### 为什么需要

```
标准 RAG：一次检索定胜负
Agent 化 RAG：Agent 决定检索几次、要不要换查询
（对应阶段 4 的 Agentic RAG 思想）
```

### 实现方式

```python
from haystack.components.agents import Agent
from haystack.components.tools import ComponentTool

# 把检索器包装为 Agent 可调用的工具
retriever_tool = ComponentTool(
    component=retriever,
    name="search_knowledge",
    description="检索知识库，参数 query 为问题",
)

agent = Agent(
    generator=llm,
    tools=[retriever_tool],
    system_prompt="你是问答助手：需要信息时调用 search_knowledge。",
)
```

## 6. 模式选型

| 模式 | 适用 | 不适用 |
|---|---|---|
| 标准链路 | 简单问答/原型 | 生产（耦合） |
| 索引/查询分离 | **生产默认** | 单次批处理 |
| 管道内嵌 Agent | 需要多步检索/决策 | 简单事实问答（杀鸡用牛刀） |

### 选型决策树

```
生产 RAG？
├─ 简单单轮问答 → 模式二（索引/查询分离）
├─ 多步/需决策 → 模式三（内嵌 Agent）
└─ 原型验证 → 模式一（最快跑通）
```

### 组合演进

```
模式一（原型验证）→ 模式二（生产化）→ 模式三（按需加 Agent）
```

## 7. 面试高频问法

| 问题 | 回答要点 |
|---|---|
| Generator 怎么换模型？ | 换 Generator 类（OpenAI/Anthropic/本地） |
| PromptBuilder 模板语法？ | Jinja2（循环/条件/变量） |
| 为什么索引查询分离？ | 解耦部署/独立扩展/增量更新/故障隔离 |
| Agent 怎么嵌进管道？ | Agent 是组件，检索器用 ComponentTool 包装 |
| 三种模式怎么选？ | 原型一、生产二、复杂三 |

### 面试加分表达

> "生产 RAG 我默认用模式二：索引管道（离线批处理）和查询管道（在线低延迟）分离，共享 DocumentStore——增量更新只重跑索引管道，查询服务零影响。需要多步决策时升级模式三，把检索器包装成 ComponentTool 让 Agent 调用。"

## 8. 模式二生产落地示例

```python
"""生产默认：索引/查询分离（共享存储）"""
from haystack import Pipeline
from haystack.components.converters import TextFileToDocument
from haystack.components.preprocessors import DocumentSplitter
from haystack.components.writers import DocumentWriter
from haystack.components.builders import PromptBuilder
from haystack_integrations.components.embeddings.sentence_transformers import (
    SentenceTransformersDocumentEmbedder, SentenceTransformersTextEmbedder,
)
from haystack_integrations.components.retrievers.elasticsearch import ElasticsearchEmbeddingRetriever
from haystack_integrations.components.llms.openai import OpenAIGenerator

# 共享存储（生产：ES/Qdrant，不是 InMemory）
document_store = ElasticsearchDocumentStore(hosts=["localhost:9200"])

# ── 索引管道（离线，可定时跑）──
indexing = Pipeline()
indexing.add_component("converter", TextFileToDocument())
indexing.add_component("splitter", DocumentSplitter(split_by="word", split_length=200))
indexing.add_component("embedder", SentenceTransformersDocumentEmbedder(model="BAAI/bge-small-zh-v1.5"))
indexing.add_component("writer", DocumentWriter(document_store))
indexing.connect("converter.documents", "splitter.documents")
indexing.connect("splitter.documents", "embedder.documents")
indexing.connect("embedder.documents", "writer.documents")

# ── 查询管道（在线服务）──
qa = Pipeline()
qa.add_component("embedder", SentenceTransformersTextEmbedder(model="BAAI/bge-small-zh-v1.5"))
qa.add_component("retriever", ElasticsearchEmbeddingRetriever(document_store=document_store, top_k=5))
qa.add_component("prompt", PromptBuilder(template=prompt_template))
qa.add_component("llm", OpenAIGenerator(model="deepseek-chat", api_base_url="https://api.deepseek.com"))
qa.connect("embedder.embedding", "retriever.query_embedding")
qa.connect("retriever.documents", "prompt.documents")
qa.connect("prompt", "llm")
```

### 落地要点

```
① 索引管道 = 定时任务（CronJob/调度器）
② 查询管道 = HTTP 服务（Hayhooks，07 篇）
③ 增量更新 = 只重跑索引管道（查询零影响）
④ 存储共享 = 同一 DocumentStore 实例
```

## 9. 常见误区

| 误区 | 真相 |
|---|---|
| "一个管道走天下" | 生产必须分离（解耦/扩展/隔离） |
| "模板越简单越好" | 强约束模板是幻觉防线（不能省） |
| "生成参数无所谓" | temperature=0.3 纪律（忠实引用） |
| "Agent 化一定更好" | 简单问答用 Agent 是浪费 |
| "换模型要改管道" | 换 Generator 配置即可 |

> 🎯 核心要点：生成层 = Generator（模型）+ PromptBuilder（Jinja2 模板，强约束 + 来源标注）；三种模式（标准/分离/内嵌 Agent）——生产默认索引查询分离（解耦/扩展/增量/隔离四收益）；Agent 化 RAG 用 ComponentTool 把检索器变成工具；选型"原型一、生产二、复杂三"；模板与生成参数是幻觉防线，不能省。

---

**下一模块**：[06-Agent能力-工具调用与AgenticLoop](06-Agent能力-工具调用与AgenticLoop.md) / **返回总览**：[00-Haystack知识体系总览](00-Haystack知识体系总览.md)
