# LlamaIndex 知识体系总览

> 检索为第一抽象的数据框架（MIT，49k stars，月下载 2500 万）——300+ 数据连接器、四大索引类型、事件驱动 Workflows、LlamaParse 文档解析（v2 四级）——"RAG 瓶颈不是模型而是数据管道"的最佳实践框架。

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
LlamaIndex（数据框架 · MIT · 49k stars · 月下载 2500 万）
│
├── 01 概述与定位          检索为第一抽象
│   ├── "RAG 瓶颈不是模型而是数据管道"
│   ├── 五大核心抽象
│   └── vs LangChain / Haystack
│
├── 02 核心概念            五大抽象详解
│   ├── 连接器（300+ 源）/ 节点解析器
│   ├── 索引（向量/摘要/关键词/图谱）
│   ├── 查询引擎 / Workflows
│   └── 数据流全景
│
├── 03 快速上手            第一个 RAG 应用
│   ├── 安装（llama-index 核心 + 集成）
│   └── VectorStoreIndex 最小代码全解
│
├── 04 索引与查询引擎       检索核心
│   ├── 四种索引对比与选型
│   ├── QueryEngine / 子问题分解
│   ├── 层级分块 / 混合搜索
│   └── 高级检索（HyDE/Self-RAG/RAPTOR）
│
├── 05 Workflows 与 Agent   事件驱动编排
│   ├── Workflows 1.0（事件传递非 DAG）
│   ├── LlamaAgents（文档处理 Agent）
│   └── vs LangGraph（基准数据）
│
├── 06 LlamaParse           文档解析核心资产
│   ├── v2 四级定价（Fast/Agentic 等）
│   ├── Parse API v2 / Retrieval Harness
│   └── 视觉布局保留 / 版本固定
│
├── 07 LlamaCloud 与生产化   平台与可观测
│   ├── 产品矩阵（Parse/Extract/Index/Sheets）
│   ├── 内置评估器 / OTel / LlamaTrace
│   └── 合规与定价
│
└── 08 对比选型与面试        三方对比 + 混用
    ├── vs LangChain / Haystack / LangGraph
    ├── 基准数据（6ms 开销/代码少 30-40%）
    ├── 混用模式（LlamaIndex 检索 + LangGraph 编排）
    └── 面试速记
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|---|---|---|---|
| 01 | 概述与定位 | 检索第一抽象、五大抽象、框架对比 | 入门必读 |
| 02 | 核心概念 | 连接器/解析器/索引/查询引擎/Workflows | 入门必读 |
| 03 | 快速上手 | 安装、最小 RAG 代码全解 | 新手第一步 |
| 04 | 索引与查询引擎 | 四索引、QueryEngine、高级检索 | 动手核心 |
| 05 | Workflows 与 Agent | 事件驱动编排、LlamaAgents、基准 | 进阶 |
| 06 | LlamaParse | v2 四级、API v2、Retrieval Harness | 数据核心 |
| 07 | LlamaCloud 与生产化 | 产品矩阵、评估器、合规定价 | 工程化 |
| 08 | 对比选型与面试 | 三方对比、混用、面试速记 | 面试/落地 |

## 3. 学习路线推荐

**路线一：RAG 落地（2 天）**
01 定位 → 02 概念 → 03 上手 → 04 索引查询 → 07 生产化

**路线二：数据/解析专精（1.5 天）**
01 → 06 LlamaParse（2026 竞争资产）→ 07 → 08

**路线三：编排视角（1 天）**
05 Workflows → 08 对比 → 对照 LangGraph/Haystack 体系

## 4. 核心概念速查

| 概念 | 一句话速记 |
|---|---|
| LlamaIndex | 检索为第一抽象的数据框架（RAG 数据管道） |
| 数据连接器 | 300+ 数据源接入（LlamaHub 生态） |
| 节点解析器 | 分块策略（NodeParser） |
| 索引 | 向量/摘要/关键词/知识图谱四种 |
| 查询引擎 | 检索到响应的流水线（QueryEngine） |
| Workflows | 事件驱动异步编排（1.0 于 2025-06 GA） |
| LlamaAgents | 文档处理 Agent 一键部署（发票/合同模板） |
| LlamaParse | 智能文档解析服务（v2 四级：Fast/Agentic 等） |
| LlamaCloud | 托管平台（过渡更名中，以 LlamaParse 为核心） |
| LlamaExtract | schema 结构化提取（免训练） |
| Retrieval Harness | 文件系统检索原语（Hybrid Retrieve/Grep/Read） |
| 层级分块 | hierarchical chunking（内置） |
| 内置评估器 | 忠实度/相关性评估（免独立平台） |
| LlamaTrace | 可观测仪表板 |
| 积分（credits） | LlamaParse 计费单位（1000 积分 ≈ $1.25） |

## 5. 2026 年关键状态

| 维度 | 状态（2026-08 基准） |
|---|---|
| 版本 | v0.14.21（2026-04-21） |
| 规模 | 49k stars、月下载 2500 万、10 亿份文档处理 |
| Workflows | 1.0（2025-06 GA），事件驱动编排成熟 |
| LlamaParse | v2（2025-12）：四级定价 + API v2 + Retrieval Harness |
| LlamaCloud | 过渡更名中（→ LlamaParse），SOC 2 Type 2 |
| 生态定位 | "RAG 瓶颈不是模型而是数据管道"的践行者 |
| 混用趋势 | LlamaIndex（检索层）+ LangGraph（编排层）常见 |

## 6. 与同级框架的分工

| 框架 | 定位 | LlamaIndex 的关系 |
|---|---|---|
| LangChain | 通用 LLM 编排 | 竞品（编排优先 vs 检索优先） |
| Haystack | 生产级 RAG 管道 | 竞品（模块化管道 vs 数据框架） |
| LangGraph | 图式 Agent | 互补（常混用） |
| LlamaIndex | **数据框架（检索第一抽象）** | 本体系 |

> 选型主线：**数据接入/解析/检索 → LlamaIndex；模块化生产管道 → Haystack；通用编排/Agent → LangChain/LangGraph**。2026 实践：很多生产团队用 LlamaIndex 管检索层、LangGraph 管编排层。

## 7. 学习计划与 FAQ

### 学习计划

| 天 | 内容 | 目标 |
|---|---|---|
| Day 1 | 01 定位 + 02 概念 + 03 上手 | 理解数据框架思想，跑通最小 RAG |
| Day 2 | 04 索引 + 05 Workflows + 07 Cloud | 检索与编排、生产化 |
| Day 3 | 06 LlamaParse + 08 对比 | 数据解析与选型视角 |

### 常见疑问快答

| 疑问 | 回答 |
|---|---|
| 与 LangChain 学哪个？ | 数据/检索选 LlamaIndex；编排/生态选 LangChain；常混用 |
| 需要先学 RAG 吗？ | 需要——LlamaIndex 是数据管道封装，原理在 RAG 阶段 1-4 |
| LlamaParse 免费吗？ | 免费层 10K 积分/月；生产按积分（1000 积分 ≈ $1.25） |
| 只支持 Python？ | 核心 Python；SDK 有 TS（llama-cloud） |
| 生产能用吗？ | 能——LlamaCloud 托管 + SOC 2 合规 |

### 学习心态

```
LlamaIndex 的学习主线："把 RAG 数据管道封装到极致"
原理（RAG 阶段 2/3）→ 组件（连接器/解析器/索引）
→ 商业资产（LlamaParse/Cloud）
先学原理再学框架——解析与检索的质量判断力来自原理
```

---

## 参考来源

- [LlamaIndex Newsletter 2026-02-24（官方）](https://www.llamaindex.ai/blog/llamaindex-newsletter-2026-02-24)
- [LlamaParse API v2: New SDKs And Migration Guide（官方）](https://www.llamaindex.ai/blog/announcing-new-llamacloud-sdks-and-parse-api-v2)
- [Enterprise CIO Guide: LlamaIndex Agentic Workflows — Beyond RAG（CallSphere）](https://callsphere.ai/blog/td30-gen-llamaindex-agentic-workflows-2026-ent-cio)
- [LlamaIndex Review 2026: Features, Pricing & Verdict（AIAgentSquare）](https://aiagentsquare.com/agents/llamaindex)
- [LlamaParse Retrieval Harness（官方博客）](http://www.llamaindex.cloud/blog/announcing-retrieval-harness)

---

**下一模块**：[01-LlamaIndex概述-数据框架](01-LlamaIndex概述-数据框架.md)
