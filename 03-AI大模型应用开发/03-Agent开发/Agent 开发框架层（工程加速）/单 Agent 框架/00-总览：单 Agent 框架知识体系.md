# 00 总览：单 Agent 框架知识体系

> 定位：框架层（工程加速）第一站——五大主流单 Agent 开发框架的机制、API、选型与迁移（2026-08 基准）

## 📚 目录

1. [本模块的定位](#1-本模块的定位)
2. [知识体系导图](#2-知识体系导图)
3. [模块导航](#3-模块导航)
4. [学习路线推荐](#4-学习路线推荐)
5. [核心概念速查](#5-核心概念速查)
6. [与关联体系的分工](#6-与关联体系的分工)
7. [2026 版本窗口](#7-2026-版本窗口)

## 1. 本模块的定位

```text
Agent 内核理论模块    = 原理：Agent 是什么、循环怎么转（本模块之前的必修）
四大核心组件模块      = 组件：工具/记忆/规划/执行 怎么做
本模块（单 Agent 框架）= 工具：选哪个框架、每个框架怎么用、怎么对比迁移
多 Agent 框架模块     = 更复杂编排（本模块的进阶）
工程化 & 部署模块     = 生产化：可观测/测试/部署（本模块的落地）

一句话分工：本模块回答"用哪个框架写单个 Agent，为什么"
```

> 🎯 **核心价值**：2026 年单 Agent 开发已进入"框架多元化"时代——LangGraph、OpenAI Agents SDK、Smolagents、Pydantic AI、Claude Agent SDK 各占一个生态位。本模块把每个框架的**核心机制**讲透（不是抄文档），再给出一张可执行的选型决策图，最后用同一需求在多个框架下的实现对比完成迁移训练。

## 2. 知识体系导图

```text
单 Agent 框架知识体系
│
├── 00 总览（本文件）
│
├── 全景篇
│   └── 01 框架全景与选型 ── 五大框架地图 / 选型决策树 / 多框架组合
│
├── 框架深潜篇（每个框架：核心机制 / API / 陷阱 / 适用）
│   ├── 02 LangGraph ── 状态图 / checkpointing / HITL interrupt
│   ├── 03 OpenAI Agents SDK ── 五原语 / Sessions / Guardrails
│   ├── 04 Smolagents ── 代码执行范式 / 五层沙箱
│   ├── 05 Pydantic AI ── 类型安全 / Capabilities / Dynamic Workflows
│   └── 06 Claude Agent SDK ── Claude Code 作为库 / 完整 harness
│
├── 原理篇
│   └── 07 Agent 循环通用架构 ── 五框架的底层共识 / 上下文管理 / 终止条件
│
├── 对比篇
│   ├── 08 跨框架能力对比 ── HITL / 持久化 / 可观测 / MCP 集成
│   └── 09 框架选型与迁移实战 ── 同需求五实现 / 迁移决策
│
└── 检验篇
    └── 10 面试与自测 ── 面试题 / 自测 / 毕业检查单
```

## 3. 模块导航

| 序号 | 模块 | 核心内容 | 场景 |
|:---:|------|---------|------|
| 01 | 框架全景与选型 | 框架地图、选型决策树 | 先读（建立全局） |
| 02 | LangGraph | 状态图、checkpointer、interrupt | 需要复杂状态/持久化时 |
| 03 | OpenAI Agents SDK | 五原语、Sessions、Guardrails | 用 OpenAI 模型、要轻量时 |
| 04 | Smolagents | 代码执行范式、沙箱五层 | 任务可写代码解决时 |
| 05 | Pydantic AI | 类型安全、Capabilities | 结构化数据/类型安全优先时 |
| 06 | Claude Agent SDK | query API、内置工具、hooks | 要现成完整 harness 时 |
| 07 | 循环通用架构 | 五框架底层共识 | 理解"框架在做什么" |
| 08 | 跨框架能力对比 | HITL/持久化/可观测/MCP 四维 | 框架间做抉择时 |
| 09 | 选型与迁移实战 | 同需求五实现、迁移决策 | 动手前/迁移时 |
| 10 | 面试与自测 | 面试题、自测、毕业检查单 | 求职/自检 |

## 4. 学习路线推荐

**路线 A：快速上手（2 天）**——01 → 07 → 按需求选一个框架深潜（02-06 选一）→ 09
> 先懂框架在做什么，再精一个，最后用案例核对。

**路线 B：全面掌握（1 周）**——01 → 07 → 02 → 03 → 04 → 05 → 06 → 08 → 09
> 五大框架全部过一遍，重点对比机制差异。

**路线 C：面试冲刺（2 天）**——10 → 01 → 07 → 08
> 以题为纲：先会答"框架选择"类问题，再补机制细节。

## 5. 核心概念速查

| 概念 | 一句话 | 关键数字 |
|------|--------|---------|
| 单 Agent 框架 | 提供 agent loop + 工具执行 + 上下文管理的开发库 | 五大主流（2026） |
| LangGraph | 状态机范式：图 + 共享状态 + checkpoint | v1.2.0（2026-05）；`create_react_agent` 已弃用 → `create_agent` |
| Checkpointing | 每节点保存状态，支持暂停/恢复/时间旅行 | 生产用 `PostgresSaver` |
| interrupt() | 图内暂停等人工输入，`Command(resume=...)` 恢复 | HITL 四决策：approve/reject/edit/respond |
| OpenAI Agents SDK | 轻量原语：Agent/Handoffs/Guardrails/Sessions/Tracing | v0.19.0（2026-07）；Assistants API 2026 年中弃用 |
| Guardrails | 输入/输出校验层，可 tripwire 中断运行 | 输入可并行检查（run_in_parallel） |
| Smolagents | 代码执行范式：Agent 写 Python 而非 JSON 工具调用 | 1.0 稳定（2026-06）；多步任务省 ~30% 步数与调用 |
| 五层沙箱 | AST 解释 + import/函数白名单 + dunder 保护 + 操作上限 | 1000 万操作 / 100 万次循环上限 |
| Pydantic AI | 类型安全范式：Pydantic 校验输入输出 | v2.0.0（2026-06-23）；v2.26.0（2026-08-06） |
| Capability | v2 核心原语：工具+hooks+指令+模型设置打包可插拔 | 40+ 内置 Capability |
| Claude Agent SDK | Claude Code 打包为库：完整 harness 内置工具 | `query()` / `ClaudeSDKClient` / `ClaudeAgentOptions` |
| 自动压缩 | 上下文将满时自动摘要旧历史 | 触发后发 `compact_boundary` 事件 |
| 工具搜索 | 大工具集按需加载 schema，不预载全量 | LangGraph/Pydantic AI/Agent SDK 均已支持 |
| 多框架组合 | 2026 共识：不同框架管不同层（如 LangGraph 编排 + Pydantic AI 结构化输出） | MCP/A2A 协议统一互操作 |

## 6. 与关联体系的分工

| 体系 | 分工 | 本模块怎么用 |
|------|------|------------|
| [Agent 内核理论模块](../../Agent%20内核理论模块/) | Agent 原理、循环概念 | 本模块假设原理已懂，直接讲框架 |
| [四大核心组件模块](../../Agent%20四大核心组件/) | 工具/记忆/规划/执行组件设计 | 框架内的工具/记忆实现对应这些概念 |
| [多 Agent（Multi-Agent）框架](../多%20Agent（Multi‑Agent）框架/) | 多 Agent 编排框架 | 单 Agent 掌握后再进阶 |
| [新兴 SDK](../新兴%20SDK/) | 更小的新框架/协议 | 本模块只覆盖五大主流 |
| [工程化 & 部署模块](../../Agent%20工程化%20&%20部署模块（从%20demo%20到可用应用）/00-总览：Agent%20工程化与部署模块（从%20demo%20到可用应用）.md) | 可观测/测试/部署 | 框架选完后的生产化 |
| [Java AI 生态（LangChain4j/Spring AI）](../../../05-AI开发框架/Java%20AI生态/00-Java%20AI生态总览.md) | Java 侧 Agent 框架 | Java 学习者对照（本模块以 Python 为主） |
| [Function Calling 体系](../../../02-大模型基础与Prompt工程/Function%20Calling%20函数调用【Agent%20基石】/00-FunctionCalling知识体系总览.md) | 协议层原理 | 框架的工具调用循环基于此 |

## 7. 2026 版本窗口

> 本模块以 2026-08 为基准：
>
> - **LangGraph v1.x**（2026-05 达 v1.2.0）：LTS 稳定线；高层入口 `create_react_agent` 弃用，由 langchain 包的 `create_agent` 取代；核心仍是 StateGraph + checkpoint + interrupt
> - **OpenAI Agents SDK v0.19.0**（2026-07-27）：五原语成熟（Agent/Handoffs/Guardrails/Sessions/Tracing）；Sessions 多后端（SQLite/Redis/SQLAlchemy/加密）；Assistants API 2026 年中弃用，新项目一律 Agents SDK
> - **Smolagents 1.0**（2026-06 首个稳定版）：CodeAgent/ToolCallingAgent 双 API 稳定；沙箱安全五层；远程沙箱（E2B/Docker/Modal）为生产推荐
> - **Pydantic AI v2.0.0**（2026-06-23 稳定，v2.26.0 于 2026-08-06）：Capability 原语重构；Dynamic Workflows（子 Agent 即异步函数）；`openai:` 模型名走 Responses API
> - **Claude Agent SDK v2.x**：`query()` 单次 + `ClaudeSDKClient` 持续会话双入口；内置 Claude Code 全部工具；权限六模式；自动压缩；hooks 完备
> - **选型共识**：无单一赢家；生产系统多框架组合（如 LangGraph 编排 + Pydantic AI 结构化工具输出）；MCP/A2A 互操作协议让框架间切换成本下降；简单 Agent 优先 Pydantic AI/Smolagents/OpenAI SDK，复杂状态流优先 LangGraph，要现成完整 harness 用 Claude Agent SDK

## 【参考来源】

- [morphllm.com: AI Agent Frameworks (2026 Update): 8 SDKs Compared](https://www.morphllm.com/ai-agent-framework)
- [LangChain Docs: What's new in LangGraph v1](https://docs.langchain.com/oss/javascript/releases/langgraph-v1)
- [futureagi.com: What is LangGraph? Stateful Agent Graphs Explained in 2026](https://futureagi.com/blog/what-is-langgraph-2026/)
- [futureagi.com: What is the OpenAI Agents SDK? Loops and Handoffs in 2026](https://futureagi.com/blog/what-is-openai-agents-sdk-2026/)
- [OpenAI Agents SDK Python releases（v0.19.0）](https://github.com/openai/openai-agents-python/releases/tag/v0.19.0)
- [futureagi.com: Evaluating smolagents in 2026: Code-as-Action Eval](https://futureagi.com/blog/evaluating-smolagents-2026/)
- [Pydantic AI v2.0.0 发布说明](https://github.com/pydantic/pydantic-ai/releases/tag/v2.0.0)
- [Pydantic: Pydantic AI v2: capabilities, a leaner core, and the Harness](https://pydantic.dev/articles/pydantic-ai-v2)
- [Claude Agent SDK Overview（官方文档）](https://code.claude.com/docs/en/agent-sdk/overview)
- [rmednitzer/agents: ADR 0001 runtime selection（Pydantic AI 选型理由）](https://github.com/rmednitzer/agents/blob/main/docs/adr/0001-runtime-selection.md)

---

**下一模块**：[01-框架全景与选型：2026 框架地图](01-框架全景与选型：2026%20框架地图.md)
