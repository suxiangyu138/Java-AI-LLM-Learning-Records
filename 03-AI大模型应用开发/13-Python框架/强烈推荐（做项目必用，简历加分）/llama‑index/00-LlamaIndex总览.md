# 00 - LlamaIndex 总览

> 强烈推荐：LlamaIndex（数据框架）——做 RAG 必用、简历加分——"LangChain 管编排，LlamaIndex 管数据——2026 年 RAG 的事实标准"

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [与主体系的分工](#3-与主体系的分工)
4. [学习路线推荐](#4-学习路线推荐)
5. [核心概念速查](#5-核心概念速查)
6. [常见误区](#6-常见误区)
7. [一周学习计划示例](#7-一周学习计划示例)
8. [快速自测 10 题](#8-快速自测-10-题)

---

## 1. 知识体系导图

```text
LlamaIndex（本体系 11 篇——强烈推荐）
├── 定位层：01 LlamaIndex 是什么（数据框架/RAG 事实标准）
├── 数据层：02 核心抽象（Document/Node/Settings）
│          03 文档加载（LlamaHub/LlamaParse）
│          04 切分与索引构建（IngestionPipeline/VectorStoreIndex）
├── 检索层：05 检索与查询引擎（QueryEngine）
│          06 高级检索（混合检索/重排/路由）
│          07 知识图谱（PropertyGraphIndex/GraphRAG）
├── 智能层：08 Agent 与事件驱动工作流（Workflows/AgentWorkflow）
│          09 记忆与可观测性（Memory/OTel/LlamaTrace）
└── 验收层：10 阶段实战与自测（RAG 项目 + Agent + 20 题）
```

## 2. 模块导航

| 篇 | 模块 | 核心内容 | 核心产出 |
|:---:|------|---------|---------|
| 00 | 总览 | 导图/分工/路线/速查/误区 | 学习计划 |
| 01 | LlamaIndex 是什么 | 定位/2026 版本/生态四件套 | 认知 |
| 02 | 核心抽象 | Document/Node/Settings | 懂数据模型 |
| 03 | 文档加载 | LlamaHub/LlamaParse 四档 | 会接数据 |
| 04 | 索引构建 | 切分策略/IngestionPipeline/持久化 | 会建索引 |
| 05 | 查询引擎 | Retriever/response_mode/聊天引擎 | 会问答 |
| 06 | 高级检索 | 混合检索/重排/融合 | 会提质量 |
| 07 | 知识图谱 | PropertyGraphIndex/GraphRAG | 会做图谱 |
| 08 | Agent 工作流 | Workflows/AgentWorkflow | 会做 Agent |
| 09 | 记忆与观测 | 记忆/OTel/LlamaTrace | 会观测 |
| 10 | 实战与自测 | 完整项目 + 20 题 | 毕业产出 |

## 3. 与主体系的分工

**与 LangChain 体系（`../LangChain/00-LangChain总览.md`）的分工**：LangChain 是"编排框架"（模型/提示词/工具/链的组装），LlamaIndex 是"数据框架"（文档加载/切分/索引/检索）——**"LangChain 管'拿信息之后做什么'，LlamaIndex 管'怎么拿到对的信息'——纯 RAG 用 LlamaIndex 更省代码、质量更高；复杂 Agent 用 LangGraph；两者可混用（LlamaIndex 当检索层嵌进 LangChain Agent）"**。

**与 vLLM 体系（`../了解即可（知道能干什么，不需要深挖源码）/vLLM/00-vLLM总览.md`）的分工**：vLLM 是"模型服务"（部署推理引擎），LlamaIndex 是"应用框架"——**"LlamaIndex 应用 → 调 vLLM 的 OpenAI 兼容端点——应用层与模型层配合"**。

**与 RAG 拓展优化深化（`../../../04-RAG检索增强生成/`）的分工**：那个体系讲"RAG 原理与范式演进"（召回/重排/评估/Agentic RAG），本体系讲"LlamaIndex 这个框架怎么用"——**"原理课 vs 框架课——先懂 RAG 概念（那个体系），再上手 LlamaIndex（本体系）"**。

**与 Function Calling（`../../../02-大模型基础与Prompt工程/Function%20Calling%20函数调用【Agent%20基石】/`）的分工**：那是"工具调用协议层"的原理课，LlamaIndex 的 Agent 直接复用协议——**"协议是地基，Workflows/AgentWorkflow 是上层封装（08 篇）"**。

**与向量数据库（`../../../08-向量数据库/`）的分工**：那个体系讲"向量库原理与选型"（HNSW/IVF/Chroma/Milvus 对比），本体系把向量库当"可插拔存储"用——**"向量库是 LlamaIndex 的下游——选型看那个体系，接入看本体系 04 篇"**。

**与 Python 异步 + FastAPI（`../../../01-Python语言/Python%20异步%20+%20FastAPI/`）的分工**：LlamaIndex 全链路异步（aquery/aretrieve）——**"FastAPI 服务暴露 RAG 接口是标准组合（10 篇实战）——异步姿势两个体系相互印证"**。

**2026-08 基线**：llama-index-core **0.14.23**（周更节奏）；TypeScript 版 llamaindex 0.12.1；**300+ LlamaHub 集成**；40K+ GitHub 星标；**0.13.0 起移除 HybridQueryEngine**（改用 QueryFusionRetriever）、PropertyGraphIndex.from_documents 需显式 mode；**QueryPipeline 弃用改 Workflows**——**"老教程（0.10 以前）API 变化大——以官方文档为准"**。

## 4. 学习路线推荐

**路线一：标准路线（5-7 天）**——01 → 10 逐篇 + 每篇动手——**毕业标准：做一个 LlamaIndex RAG 项目（加载 + 切分 + 索引 + 查询引擎 + 重排）**。

**路线二：速成路线（3-4 天）**——01 → 02 → 04 → 05 → 08 → 10（跳过 03/06/07/09 精读）——适合已有 RAG 经验者，先跑通最小闭环再补高级检索。

**路线三：项目驱动路线**——先定项目（企业知识库问答/文档 Agent）→ 按需查篇目——**"强烈推荐的体系 = 项目驱动的使用"**。

**路线选择判断**：有 RAG 概念基础 → 速成线；零基础且时间充裕 → 标准线；本周要交项目 → 项目驱动线——**"任何路线都要在第 7 天完成 10 篇实战——毕业标准不变"**。

## 5. 核心概念速查

| 概念 | 一句话 | 对应篇 |
|------|--------|:---:|
| Document | 原始文档（文本 + 元数据） | 02 |
| Node | 切分后的最小检索单元（带 embedding） | 02 |
| Settings | 全局配置（llm/embed_model/chunk_size） | 02 |
| LlamaHub | 300+ 官方集成库（Readers/向量库/工具） | 03 |
| LlamaParse | 云解析服务（PDF/表格/复杂版式） | 03 |
| IngestionPipeline | 加载→切分→嵌入→入库的流水线 | 04 |
| VectorStoreIndex | 默认索引（向量检索） | 04 |
| Retriever | 检索器（取 top-k 节点） | 05 |
| QueryEngine | 检索 + LLM 合成的问答引擎 | 05 |
| QueryFusionRetriever | 多路检索融合（混合检索） | 06 |
| Reranker | 重排器（精排：召回宽、重排准） | 06 |
| PropertyGraphIndex | 属性图索引（GraphRAG） | 07 |
| Workflows | 事件驱动工作流（0.14 编排标准） | 08 |
| AgentWorkflow | 多 Agent 编排（handoff/共享状态） | 08 |
| LlamaTrace | 官方可观测性平台 | 09 |

## 6. 常见误区

**误区一：LlamaIndex = LangChain 替代品**——"二选一"——**正确：定位不同——LlamaIndex 数据优先（RAG 深度好），LangChain 编排优先（Agent 生态大）——纯 RAG 选 LlamaIndex，复杂 Agent 选 LangChain，可混用**（01 篇）。

**误区二：每次查询都重新建索引**——"from_documents 反复调"——**正确：from_documents 只用于首次构建——后续 from_vector_store 加载，否则重复切分/嵌入烧钱**（04 篇）。

**误区三：默认 chunk_size 直接用**——"512 就 512"——**正确：切分策略按文档类型调——语义切分（SemanticSplitter）对长文档质量更高；embedding 模型决定检索上限**（04 篇）。

**误区四：向量检索一步到位**——"top_k 取 5 直接喂 LLM"——**正确：生产配方是"召回宽（top20）+ 重排精（rerank top5）"——'Rerank before you generate' 是最小生产配置**（06 篇）。

**误区五：老教程照抄**——"网上 0.10 教程直接用"——**正确：0.13.0 起 HybridQueryEngine 移除、PropertyGraphIndex 要显式 mode——API 变化大，以官方文档为准**（06/07 篇）。

**误区六：LlamaIndex 只能做 RAG**——"文档问答专用"——**正确：2026 年它已是完整 Agent 框架——事件驱动 Workflows + 多 Agent（08 篇）——RAG 只是入场券**（01 篇——"RAG 库 → 编排框架的演进"）。

**误区七：必须用 LlamaParse/向量库**——"没有云服务就做不了"——**正确：开源核心免费够起步——本地解析 + 内存索引能跑通原型；LlamaParse/向量库是生产选择不是入门门槛**（03/04 篇）。

## 7. 一周学习计划示例

| 天 | 学习内容 | 动手任务 | 验收 |
|:---:|---------|---------|------|
| 1 | 01/02 定位 + 核心抽象 | 装 llama-index + 读文档成 Node | 懂数据模型 |
| 2 | 03/04 加载 + 索引 | 10 个文档入库 + 持久化 | 索引能建能存 |
| 3 | 05 查询引擎 | 问答 + 调 response_mode | 问答可用 |
| 4 | 06 高级检索 | 加 BM25 + 重排对比效果 | 质量提升 |
| 5 | 07 知识图谱 | 小语料建 PropertyGraphIndex | 图谱能查 |
| 6 | 08 工作流 | 跑通官方 AgentWorkflow 示例 | 多步流程 |
| 7 | 09/10 观测 + 实战 | 完整项目 + 自测 20 题 | 毕业 |

## 8. 快速自测 10 题

1. LlamaIndex 一句话定位？（数据框架——RAG 事实标准）
2. Document 和 Node 什么关系？（文档切分后的最小检索单元）
3. 建索引的三要素？（切分器 + embedding 模型 + 向量存储）
4. from_documents 什么时候用？（仅首次构建）
5. response_mode 有哪些？（compact/refine/tree_summarize/simple_summarize）
6. 生产检索最小配置？（召回宽 + 重排精——Rerank before you generate）
7. 混合检索在 0.13+ 怎么写？（QueryFusionRetriever 融合多路）
8. PropertyGraphIndex 解决什么问题？（跨文档多跳关系查询）
9. Workflows 和 QueryPipeline 什么关系？（后者已弃用——事件驱动替代）
10. 可观测性用什么？（OTel + LlamaTrace/Arize Phoenix/Langfuse）

---

**参考来源**：

- [LlamaIndex 官方文档](https://docs.llamaindex.ai/)
- [LlamaIndex Releases（run-llama/llama_index）](https://github.com/run-llama/llama_index/releases)
- [LlamaIndex Newsletter 2026-02-10](https://www.llamaindex.ai/blog/llamaindex-newsletter-2026-02-10)
- [LangChain vs LlamaIndex（官方对比）](https://www.langchain.com/resources/langchain-vs-llamaindex)
- [Workflow System（DeepWiki）](https://deepwiki.com/run-llama/llama_index/5.3-multi-agent-orchestration)
- [LangChain（姊妹体系——编排框架）](../LangChain/00-LangChain总览.md)
