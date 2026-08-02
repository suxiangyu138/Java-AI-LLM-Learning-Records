# 01 - AI Agent 开发框架对比

> 🎯 2026 年 Python Agent 框架格局已显著收敛 — 本文覆盖 10+ 主流框架的定位、核心能力、GitHub 数据、选型决策树。核心判断：模型原生能力在增强，框架"胶水"价值在降低

---

## 目录

1. [框架全景图](#1-框架全景图)
2. [六大核心框架详解](#2-六大核心框架详解)
3. [其他值得关注的框架](#3-其他值得关注的框架)
4. [选型决策树](#4-选型决策树)
5. [2026 框架趋势](#5-2026-框架趋势)

---

## 1. 框架全景图

```text
Python Agent 框架分层（2026）
│
├── 基础设施层
│   ├── LangChain（≈126k ★）— 通用AI底座，工具/RAG/集成最全
│   ├── LlamaIndex（≈42k ★）— RAG为核心的数据框架
│   └── HuggingFace Transformers — 模型训练/推理基座
│
├── 编排控制层
│   ├── LangGraph（≈24k ★）— 状态机有向图，生产级可控流程
│   ├── CrewAI（≈43k ★）— 角色分工式多Agent，上手最快
│   └── AutoGen → Microsoft Agent Framework（≈43k ★）— 微软系，双模编排
│
├── 轻量原生层
│   ├── OpenAI Agents SDK — 极简原语（Agent/Handoff/Guardrails）
│   ├── PydanticAI — 类型安全，FastAPI 风格
│   └── Google ADK — Gemini深度适配，云原生
│
└── 平台工具层
    ├── Dify — 开源LLMOps，低代码可视化
    └── Flowise/Langflow — 拖放式构建LangChain流
```

---

## 2. 六大核心框架详解

### 2.1 LangChain — 通用 AI 底座（≈126k ★）

| 维度 | 详情 |
|------|------|
| 核心能力 | LLM 统一封装、Prompt 模板、工具调用、向量库集成、记忆、链式调用 |
| 优势 | 生态最全、插件/第三方集成极多、兼容所有大模型 |
| 短板 | 多 Agent 编排弱、API 频繁 breaking change、过度抽象 |
| **2026 用法** | 从"全套"转为"工具箱"——只用 RAG/工具集成，Agent 逻辑改用原生 Function Calling |

### 2.2 LangGraph — 生产级可控工作流（≈24k ★）

| 维度 | 详情 |
|------|------|
| 核心设计 | State + Node + Edge 有向图，支持循环/条件/断点续跑/回溯 |
| 持久化 | SQLite/PostgreSQL Checkpointer、Human-in-the-loop |
| 落地案例 | Uber、Klarna、Replit、Elastic、LinkedIn |
| 短板 | 学习曲线高（2-3 周出生产代码）、简单场景过度设计 |

### 2.3 LlamaIndex Workflows — RAG 之王（≈42k ★）

| 维度 | 详情 |
|------|------|
| 核心特色 | 300+ 数据连接器、LlamaParse（130+ 格式）、混合检索、重排序 |
| 优势 | **RAG 能力行业顶尖**、文档处理一站式、轻量 |
| 短板 | 多 Agent 协作弱于 LangGraph/CrewAI |
| 适用 | 知识库问答、企业检索助手 |

### 2.4 CrewAI — 角色分工极简 Agent（≈43k ★）

| 维度 | 详情 |
|------|------|
| 核心抽象 | Agent（角色+目标+背景故事）+ Task + Crew（团队） |
| 执行模式 | 顺序执行、层级管理（Manager 统筹）、Flows 事件驱动 |
| 优势 | **声明式开发**、代码极简、几分钟搭建多 Agent 团队 |
| 短板 | 复杂分支弱、底层状态自定义难、版本 0.x API 变动 |

### 2.5 Microsoft Agent Framework — 企业级（≈43k ★）

| 维度 | 详情 |
|------|------|
| 来源 | 2025.10 合并 AutoGen + Semantic Kernel |
| 双模编排 | Agent 动态推理 + Workflow 确定性流程 |
| 标准支持 | 原生 MCP + A2A + AG-UI 三大标准 |
| 企业能力 | Azure AI Foundry 集成、Entra ID 认证、OpenTelemetry |

### 2.6 OpenAI Agents SDK — 极简原生

| 维度 | 详情 |
|------|------|
| 核心原语 | Agent、Handoff（转交）、Guardrails（输出校验） |
| 内置能力 | Agent 循环、沙盒文件系统、全链路追踪、MCP 支持 |
| 短板 | **重度依赖 OpenAI 生态**，第三方模型适配有限 |

---

## 3. 其他值得关注的框架

| 框架 | 一句话 | 适用 |
|------|--------|------|
| **PydanticAI** | 类型安全轻量 Agent（Pydantic 团队出品） | 结构化输出、类型敏感场景 |
| **Google ADK** | Gemini 深度适配、云原生全生命周期 | Google Cloud 用户 |
| **Agno**（原Phidata） | 全栈 Agent 运行时（≈39k★）、100+工具 | 一站式部署+运维 |
| **Deep Agents** | 深度自主 Agent、长程规划 | 科研/复杂推理 |
| **Haystack** | 生产级 NLP/RAG 管道、内置评估 | 企业搜索/RAG |
| **Dify** | 开源 LLMOps 低代码可视化、私有化部署完善 | 非技术团队、国内模型适配 |

---

## 4. 选型决策树

```text
你是 Python 团队 + 主要需求是？

├── Agent 生产级可控流程 → LangGraph
├── 多角色团队快速原型 → CrewAI
├── 知识库/RAG/文档问答   → LlamaIndex
├── GPT 全系 + 轻量 Agent  → OpenAI Agents SDK
├── 类型安全 + 结构化输出  → PydanticAI
├── .NET/Azure 企业系统   → Microsoft Agent Framework
├── Google 云 + Gemini    → Google ADK
├── 深度自主推理/科研     → Deep Agents
├── 低代码/非技术团队     → Dify
└── 生产级 NLP 管道       → Haystack
```

---

## 5. 2026 框架趋势

```text
① MCP 标准化：Anthropic 2024年提出，2026年几乎所有框架已支持
② A2A 协议兴起：Google 2025年提出，Agent-to-Agent通信标准化
③ 框架收敛：微软合并 AutoGen+Semantic Kernel；OpenAI推出Agents SDK
④ "去LangChain化"：用原生 Function Calling 代替框架"胶水"
⑤ 框架选型铁律：能用原生API解决的，别引入框架
```

---

> 🎯 **核心要点**：Agent 框架选型三问 — **① 团队技术栈**（Python→LangGraph/CrewAI、.NET→MS Agent Framework、Google→ADK）**② 核心场景**（RAG→LlamaIndex、流程控制→LangGraph、快速多角色→CrewAI）**③ 简单优先**（先试原生 Function Calling，不够再上框架）。

**下一模块**：[02-模型推理与服务引擎](02-模型推理与服务引擎.md) / **返回总览**：[00-生态总览](00-Python生态知识体系总览.md)
