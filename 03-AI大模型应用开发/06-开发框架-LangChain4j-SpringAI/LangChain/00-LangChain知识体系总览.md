# 00 - LangChain 知识体系总览

> 🔗 LangChain 是 Python AI 应用开发的主流框架——2025.10 发布 v1.0 GA，2026.07 已迭代到 **1.3.14**。官方分工：**LangChain（Agent 框架——模型/工具/循环抽象）+ LangGraph（编排运行时——状态/持久化/HITL）**。选型黄金法则：「需要 while 循环用 LangGraph，只是 for 循环用 LangChain」

> 🎯 本系列共 **7 篇**，从生态全景到 LCEL、从模型封装到 RAG、从 Agent 开发到 LangGraph 编排，覆盖 LangChain 全链路

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)

---

## 1. 知识体系导图

```
LangChain体系（7个文件）
│
├── 🏗️ 生态篇（01）
│   └── 01-LangChain 1.x生态全景.md  # v1.0/分包格局/框架分工
│
├── ⚙️ 核心篇（02-03）
│   ├── 02-LCEL表达式语言.md         # 管道符/并行/流式/Runnable
│   └── 03-模型与输出解析.md         # Provider/结构化输出/ContentBlocks
│
├── 📚 RAG 篇（04）
│   └── 04-RAG构建.md               # 七步流程/四种方案/Agentic GraphRAG
│
├── 🤖 Agent 篇（05）
│   └── 05-Agent开发.md             # create_agent/工具定义/多Agent
│
├── 🧩 编排篇（06）
│   └── 06-LangGraph编排.md         # 状态机/Checkpoint/HITL/性能
│
└── 📌 实战篇（07）
    └── 07-实战选型与面试.md        # 选型决策/生产组合/面试
```

## 2. 模块导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | 知识体系总览 | 全景 + 路线 + 速查 | — |
| 01 | 1.x 生态全景 | v1.0/分包/分工 | ⭐⭐⭐⭐⭐ |
| 02 | LCEL | 管道/并行/流式 | ⭐⭐⭐⭐⭐ |
| 03 | 模型与输出 | 结构化输出 | ⭐⭐⭐⭐ |
| 04 | RAG 构建 | 七步/四方案 | ⭐⭐⭐⭐⭐ |
| 05 | Agent 开发 | create_agent/工具 | ⭐⭐⭐⭐⭐ |
| 06 | LangGraph | 状态机/HITL | ⭐⭐⭐⭐ |
| 07 | 实战选型 | 选型/面试 | ⭐⭐⭐⭐ |

## 3. 学习路线推荐

```text
🟢 基础（1.5h）：01-生态 → 02-LCEL → 03-模型
🔵 实战（2h）：04-RAG → 05-Agent（两大核心场景）
🟣 编排（1h）：06-LangGraph
🔴 冲刺（30min）：07-选型与面试
```

## 4. 核心概念速查

| 概念 | 一句话解释 |
|------|-----------|
| **LangChain 1.x** | 2025.10 v1.0 GA，2026.07 → 1.3.14——Agent 框架（模型/工具/循环抽象） |
| **LangGraph** | 编排运行时（状态/持久化/HITL）——不是替代是分工 |
| **LCEL** | LangChain 表达式语言——`prompt \| model \| parser` 管道 |
| **create_agent** | 1.x 统一 Agent API（替代 AgentExecutor/create_react_agent） |
| **Runnable** | 可执行协议（invoke/ainvoke/stream 三态同源） |
| **ContentBlocks** | 1.x 统一结构化输出（区分 reasoning 与 text） |
| **ProviderStrategy** | Pydantic 结构化输出策略 |
| **@tool** | 工具装饰器（docstring = 触发说明书） |
| **Agent-RAG** | Retriever 封装 Tool——LLM 按需检索 |
| **Agentic GraphRAG** | 知识图谱 + 反思闭环（2026 WAIC 焦点） |
| **Checkpointer** | LangGraph 状态持久化（崩溃恢复） |
| **HITL** | Human-in-the-Loop（interrupt_before 人工审批） |

## 5. 一图看懂：LangChain vs LangGraph（2026 分工）

```text
同一技术栈的两个抽象层
┌─────────────────────────────────────────────┐
│ LangChain（Agent 框架）                      │
│ ├── 模型封装（Provider 集成）                 │
│ ├── 工具抽象（@tool/工具注册）                │
│ ├── 组件（Retriever/Prompt/Parser）           │
│ └── create_agent（高层 Agent）                │
├─────────────────────────────────────────────┤
│ LangGraph（编排运行时）                       │
│ ├── StateGraph（显式状态机）                  │
│ ├── Checkpointer（持久化/崩溃恢复）            │
│ ├── 条件边（分支/循环）                       │
│ └── HITL（人工审批门）                        │
└─────────────────────────────────────────────┘
选型黄金法则：
├── 工作流需要 while 循环/HITL → LangGraph
├── 只是 for 循环/直线（≤3 步）→ LangChain（LCEL 10 行）
└── 生产典型组合：LangChain 组件 + LangGraph 编排
```

---

> 🎯 **一句话定位**：LangChain 能力 = **生态（1.x 分工）× LCEL（管道表达）× RAG/Agent（两大场景）× LangGraph（编排）**——本系列带你从「会调 API」到「能编排 Agent」。

---

**下一模块**：[01-LangChain 1.x生态全景](01-LangChain%201.x生态全景.md)
