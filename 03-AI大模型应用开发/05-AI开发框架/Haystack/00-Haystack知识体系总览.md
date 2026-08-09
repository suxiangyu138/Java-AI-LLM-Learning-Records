# Haystack 知识体系总览

> deepset 开源的生产级 RAG/搜索/Agent 框架（Apache 2.0，haystack-ai 2.29）——"强类型组件 + 显式管道（DAG）"：构建期校验连接错误、YAML 序列化可部署、OpenTelemetry 原生可观测，为"严肃的文档问答"而生。

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)
5. [2026 年关键状态](#5-2026-年关键状态)
6. [与同级框架的分工](#6-与同级框架的分工)
7. [学习计划与 FAQ](#7-学习计划与-faq)

## 1. 知识体系导图

```
Haystack（deepset · 生产级 RAG 框架 · Apache 2.0 · haystack-ai）
│
├── 01 概述与定位          生产导向 vs 生态导向
│   ├── "文档问答的最健壮选择"
│   ├── vs LangChain（强类型 vs 松散）
│   └── 适用场景判断
│
├── 02 核心概念            组件 + 管道（DAG）
│   ├── @component 强类型接口
│   ├── pipeline.connect() 构建期校验
│   └── 并行分支与 loops
│
├── 03 快速上手            第一个 RAG 管道
│   ├── 安装（haystack-ai / integrations）
│   └── 最小 RAG 代码全解
│
├── 04 检索与文档存储       30+ 存储 + 混合检索
│   ├── DocumentStore 选型
│   ├── BM25 + 向量 + RRF
│   └── MultiRetriever / Ranker
│
├── 05 生成与 RAG 模式      三种生产模式
│   ├── 标准链路 / 索引查询分离
│   ├── PromptBuilder 模板
│   └── 管道内嵌 Agent
│
├── 06 Agent 能力          工具调用循环
│   ├── Tool / ComponentTool / PipelineTool
│   ├── AgenticLoop（2.10+）
│   └── MCP 与 HITL
│
├── 07 生产化              可部署性
│   ├── YAML 序列化 + CI/CD
│   ├── Hayhooks（K8s）
│   ├── OpenTelemetry 追踪
│   └── 内置评估（MRR/NDCG/faithfulness）
│
└── 08 对比选型与面试       vs LangChain/LlamaIndex/LangGraph
    ├── 选型决策树
    ├── 混用模式（Haystack 检索 + LangGraph Agent）
    └── 面试速记
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|---|---|---|---|
| 01 | 概述与定位 | 生产导向、vs LangChain、适用判断 | 入门必读 |
| 02 | 核心概念 | @component、Pipeline DAG、构建期校验 | 入门必读 |
| 03 | 快速上手 | 安装、最小 RAG 代码全解 | 新手第一步 |
| 04 | 检索与存储 | 30+ 存储、混合检索 RRF、Ranker | 动手核心 |
| 05 | 生成与 RAG 模式 | 三模式、PromptBuilder、索引查询分离 | 进阶 |
| 06 | Agent 能力 | 工具四种形态、AgenticLoop、MCP/HITL | 进阶 |
| 07 | 生产化 | YAML/Hayhooks/OTel/评估 | 工程化 |
| 08 | 对比选型与面试 | 全面对比、决策树、混用模式 | 面试/落地 |

## 3. 学习路线推荐

**路线一：RAG 落地（2 天）**
01 定位 → 02 概念 → 03 上手 → 04 检索 → 05 模式 → 07 生产化

**路线二：Agent 能力（1 天）**
01 → 02 → 06 Agent → 08 对比

**路线三：框架选型视角（半天）**
01 对比 → 08 决策树 → 对照本仓库 LangChain/LangGraph/LlamaIndex 体系

## 4. 核心概念速查

| 概念 | 一句话速记 |
|---|---|
| Haystack | deepset 的生产级 RAG/搜索/Agent 框架（Apache 2.0） |
| haystack-ai | 2.x 的 PyPI 包名（1.x 的 farm-haystack 已维护模式） |
| 组件（Component） | @component 装饰器定义的强类型处理单元 |
| 管道（Pipeline） | 组件组成的 DAG（支持并行分支与 loops） |
| 构建期校验 | connect() 时检查类型匹配——快速失败 |
| DocumentStore | 文档存储抽象（30+ 实现：ES/Qdrant/pgvector 等） |
| Retriever | 检索器（BM25/稠密/混合） |
| Generator | 生成器（LLM 调用） |
| Ranker | 重排序（CrossEncoderRanker） |
| MultiRetriever | 多路检索 + RRF 融合 |
| PromptBuilder | 提示词模板组件 |
| Agent | 一等公民：工具调用循环（模型→工具→模型） |
| Toolset | 工具分组 |
| AgenticLoop | 2.10+ 新增的 Agent 循环组件 |
| SuperComponent | 把管道封装为可复用组件 |
| Hayhooks | 管道部署为 HTTP 服务（Docker/K8s） |
| integrations | 独立集成包（haystack-integrations-*） |

## 5. 2026 年关键状态

| 维度 | 状态（2026-08 基准） |
|---|---|
| 版本线 | 2.x（v2.29.0 为 2026-05 最新稳定版，每 3-6 周一版） |
| 包名 | haystack-ai（2.x）/ farm-haystack（1.x 维护模式） |
| 新增能力 | 2.10：AgenticLoop、MCP 支持、First-class HITL |
| 开源协议 | Apache 2.0 |
| GitHub | 约 17k stars（社区小于 LangChain，聚焦生产） |
| 托管服务 | deepset Cloud / deepset Studio（$99/月起） |
| 2026-04 基准 | 引用准确率 94.2%（高于 LangGraph 的 91.4%） |

## 6. 与同级框架的分工

| 框架 | 定位 | Haystack 的关系 |
|---|---|---|
| LangChain | 通用 LLM 编排 + 最大生态 | 竞品（Haystack 更生产导向） |
| LangGraph | 图式 Agent（复杂状态） | 竞品（复杂 Agent 更强） |
| LlamaIndex | 数据框架（RAG 为主） | 竞品（Haystack 管道更强） |
| OpenAI Agents SDK | OpenAI 生态 Agent | 不同生态 |

> 选型主线：**RAG/文档问答 → Haystack；复杂多步 Agent → LangGraph；最大生态快速原型 → LangChain**。常见混用：Haystack 管"干净地取到正确上下文"（检索管道），LangGraph 管"多步推理编排"（Agent 调用 Haystack 工具）。

## 7. 学习计划与 FAQ

### 学习计划

| 天 | 内容 | 目标 |
|---|---|---|
| Day 1 | 01 定位 + 02 概念 + 03 上手 | 理解管道思想，跑通最小 RAG |
| Day 2 | 04 检索 + 05 模式 + 07 生产化 | 检索调优与生产化 |
| Day 3 | 06 Agent + 08 对比 | Agent 与选型视角 |

### 常见疑问快答

| 疑问 | 回答 |
|---|---|
| 与 LangChain 学哪个？ | RAG 生产选 Haystack；生态/Agent 选 LangChain；思想都学 |
| 需要先学 RAG 吗？ | 需要——Haystack 是 RAG 的组件化，原理在 RAG 阶段 1-3 |
| 2.x 与 1.x 什么关系？ | 2.x 是重写（haystack-ai），1.x 维护模式（farm-haystack） |
| 只支持 Python？ | 是——JS 需求选 LangChain |
| 生产能用吗？ | 能——生产导向就是它的定位（YAML/OTel/评估内建） |

### 学习心态

```
Haystack 的学习主线："把 RAG 阶段 2/3 的知识组件化"
原理你已经会（本仓库 RAG 体系），Haystack 教你怎么生产化
——先学原理再学框架，框架只是原理的工程外壳
```

---

## 参考来源

- [What is Haystack? Deepset's RAG and Agents Framework in 2026（FutureAGI）](https://futureagi.com/blog/what-is-haystack-2026/)
- [Haystack vs LangChain (2026): Pick the Right LLM Framework（GenAI.QA）](https://genai.qa/blog/haystack-vs-langchain/)
- [Haystack 开源框架深度解析：生产级 RAG 与 AI Agent 开发指南（OpenAI Hub）](https://www.openai-hub.net/news/876/)
- [LangGraph vs LlamaIndex vs Haystack (April 2026)（andrew.ooo）](https://andrew.ooo/answers/langgraph-vs-llamaindex-vs-haystack-april-2026/)
- [Haystack 2.0: The Production RAG Pipeline Framework（pristren）](https://pristren.com/blog/haystack-rag-pipeline-framework/)

---

**下一模块**：[01-Haystack概述-生产级RAG框架](01-Haystack概述-生产级RAG框架.md)
