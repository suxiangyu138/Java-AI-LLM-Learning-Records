# OpenAI Agents SDK 知识体系总览

> OpenAI 官方 Agent 框架（Swarm 的生产级继任者，Python v0.19.3）——托管式流程：Runner 驱动 Agent 循环、Handoffs 移交控制权、Guardrails 输入输出护栏、Sessions 跨运行持久化，与 Responses API 深度集成。

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)
5. [2026 年关键状态](#5-2026-年关键状态)
6. [与同级框架的分工](#6-与同级框架的分工)
7. [学习计划与 FAQ](#7-学习计划与-faq)
8. [核心数据速记](#8-核心数据速记)

## 1. 知识体系导图

```
OpenAI Agents SDK（官方 · Swarm 继任者 · MIT · 22k stars）
│
├── 01 概述与定位          托管式 Agent 流程
│   ├── Swarm → SDK 的演进
│   ├── 三大原语（Agent/Handoff/Guardrail）
│   └── vs LangGraph / AutoGen
│
├── 02 核心概念            Agent / Runner / 工具
│   ├── Agent（指令+工具+护栏）
│   ├── Runner 循环（run_sync/run_streamed）
│   └── 工具（@tool/MCP/Responses API）
│
├── 03 快速上手            第一个 Agent
│   ├── 安装（openai-agents）
│   ├── 最小 Agent 代码全解
│   └── 运行验证与排错
│
├── 04 Handoffs 多 Agent   移交控制权
│   ├── 一等字段（非工具包装）
│   ├── Typed Handoffs（结构化传递）
│   └── Agents as Tools / 嵌套
│
├── 05 Guardrails 护栏     输入输出双防线
│   ├── 输入护栏（省钱门控）
│   ├── 输出护栏 / Tripwire
│   └── 成本优化（60-80% token）
│
├── 06 Sessions 状态管理    跨运行持久化
│   ├── InMemory / SQLite / Redis
│   ├── RunState 与恢复
│   └── 多轮对话落地
│
├── 07 生产化               追踪/HITL/Sandbox
│   ├── Tracing（OpenAI 仪表盘/OTel）
│   ├── HITL 审批 / max_turns
│   ├── Sandbox Agents（2026-04 Beta）
│   └── MCP / 多 Provider
│
└── 08 对比选型与面试        五方对比 + 选型
    ├── vs LangGraph / AutoGen / Haystack / LlamaIndex
    ├── 选型决策树
    └── 面试速记
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|---|---|---|---|
| 01 | 概述与定位 | 托管式流程、三大原语、框架对比 | 入门必读 |
| 02 | 核心概念 | Agent/Runner/工具、循环机制 | 入门必读 |
| 03 | 快速上手 | 安装、最小 Agent 代码全解 | 新手第一步 |
| 04 | Handoffs | 一等字段、Typed、Agents as Tools | 多 Agent 核心 |
| 05 | Guardrails | 输入输出护栏、Tripwire、成本 | 安全核心 |
| 06 | Sessions | 会话持久化三实现、状态恢复 | 工程化 |
| 07 | 生产化 | 追踪/HITL/Sandbox/MCP | 生产落地 |
| 08 | 对比选型与面试 | 五方对比、决策树、速记 | 面试/选型 |

## 3. 学习路线推荐

**路线一：Agent 上手（1.5 天）**
01 定位 → 02 概念 → 03 上手 → 04 Handoffs

**路线二：生产强化（1.5 天）**
05 Guardrails → 06 Sessions → 07 生产化 → 08 对比

**路线三：选型视角（半天）**
01 对比 → 08 决策树 → 对照 LangGraph/AutoGen 体系

## 4. 核心概念速查

| 概念 | 一句话速记 |
|---|---|
| OpenAI Agents SDK | OpenAI 官方 Agent 框架（Swarm 生产级继任者） |
| Agent | 带指令、工具、护栏的 LLM 单元 |
| Runner | 运行循环（run_sync/run_streamed）——托管式流程 |
| Handoffs | 移交控制权给另一个 Agent（一等字段） |
| Typed Handoffs | 结构化数据传递（input_type 定义 schema） |
| Guardrails | 输入/输出校验器（双类型） |
| Tripwire | 护栏触发立即中止（抛异常） |
| Sessions | 跨运行对话历史持久化（InMemory/SQLite/Redis） |
| RunState | 运行状态（可恢复） |
| max_turns | 最大轮次（防死循环，生产必设） |
| Agents as Tools | 子 Agent 作为工具被调用（非 handoff） |
| Programmatic Tool Calling | v0.19：JavaScript 协调工具调用 |
| Sandbox Agents | 2026-04 Beta：容器化执行环境 |
| HITL | 人工审批（run 级别） |
| Tracing | 内置追踪（OpenAI 仪表盘/可切 OTel） |
| Responses API | SDK 底层的模型 API |

## 5. 2026 年关键状态

| 维度 | 状态（2026-08 基准） |
|---|---|
| Python 版本 | v0.19.3（API 快速演进中） |
| JS 版本 | @openai/agents 0.12.0（成熟度略低） |
| 定位 | OpenAI 官方主推、Swarm 继任者 |
| 规模 | 22k stars、MIT |
| 新能力 | v0.19 Programmatic Tool Calling；2026-04 Sandbox Agents（Beta） |
| 架构 | 与 Responses API 深度集成、Provider 无关（LiteLLM 100+ LLM） |

## 6. 与同级框架的分工

| 框架 | 定位 | OpenAI Agents SDK 的关系 |
|---|---|---|
| LangGraph | 图式 Agent（状态机） | 竞品（显式图 vs 托管循环） |
| AutoGen | 对话式多 Agent | 竞品（对话编排 vs 托管流程） |
| Haystack | 生产 RAG 管道 | 互补（SDK 无检索层） |
| LlamaIndex | 数据框架 | 互补（检索可作 SDK 工具） |
| **OpenAI Agents SDK** | **托管式 Agent 流程** | 本体系 |

> 选型主线：**OpenAI 生态 + 托管式流程 → SDK；显式图/复杂状态 → LangGraph；对话式编排 → AutoGen；RAG 生产 → Haystack；数据/检索 → LlamaIndex**。SDK 与检索框架可组合（检索引擎包装为工具）。

## 7. 学习计划与 FAQ

### 学习计划

| 天 | 内容 | 目标 |
|---|---|---|
| Day 1 | 01 定位 + 02 概念 + 03 上手 | 理解托管式流程，跑通最小 Agent |
| Day 2 | 04 Handoffs + 05 Guardrails | 多 Agent 与安全 |
| Day 3 | 06 Sessions + 07 生产化 + 08 对比 | 状态、生产与选型 |

### 常见疑问快答

| 疑问 | 回答 |
|---|---|
| 与 LangGraph 学哪个？ | OpenAI 生态托管式选 SDK；显式状态机选 LangGraph |
| 需要先学 Function Calling？ | 需要——SDK 循环 = FC 的托管版 |
| 只支持 OpenAI 模型？ | 否——LiteLLM 接 100+ 模型 |
| 能做 RAG 吗？ | 无内置检索层——组合 LlamaIndex/Haystack 作工具 |
| 生产成熟吗？ | 成熟——追踪/HITL/Sandbox 内建；Sandbox 是 Beta |
| API 稳定吗？ | 快速演进中（v0.19.x）——锁版本 |

## 8. 核心数据速记

| 数据 | 数值 |
|---|---|
| Python 版本 | v0.19.3（2026） |
| JS 版本 | @openai/agents 0.12.0 |
| stars | 约 22k |
| 输入护栏省钱 | 60-80% token（拦截率高时） |
| HandoffCorrectness 目标 | ≥0.90 |
| Sandbox 启动延迟 | 2-4 秒 |
| 多 Provider | LiteLLM 100+ |
| max_turns | 生产必设 |

---

## 参考来源

- [What is the OpenAI Agents SDK? Loops and Handoffs in 2026（FutureAGI）](https://futureagi.com/blog/what-is-openai-agents-sdk-2026/)
- [OpenAI Agents SDK: Building Production AI Agents (2026)（Dev.to）](https://dev.to/stacknotice/openai-agents-sdk-building-production-ai-agents-2026-4d04)
- [Evaluating OpenAI Agents SDK: The Handoff Is the Test (2026)（FutureAGI）](https://futureagi.com/blog/evaluating-openai-agents-sdk-2026/)
- [OpenAI Agents SDK: Deep Dive for Production Agent Builders（Turion）](https://turion.ai/blog/framework-deep-dive-openai-agents-sdk/)
- [openai-agents-python Releases（GitHub）](https://github.com/openai/openai-agents-python/releases/tag/v0.19.3)

---

**下一模块**：[01-概述-托管式Agent框架](01-概述-托管式Agent框架.md)
