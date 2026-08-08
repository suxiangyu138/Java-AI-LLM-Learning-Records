# 00 总览：多 Agent（Multi‑Agent）框架知识体系

> 定位：框架层（工程加速）第三站——多 Agent 编排的范式、框架、协议与生产实践：LangGraph、CrewAI、AG2/Agent Framework、Google ADK 怎么选、怎么用（2026-08 基准）

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
Agent 内核理论模块      = 原理：单 Agent 怎么转（必修）
单 Agent 框架模块       = 单个 Agent：LangGraph/OpenAI SDK 等怎么用
低代码 Agent 平台模块   = 拖拽式：不写代码快速出原型
本模块（多 Agent 框架） = 多个 Agent 怎么协作：编排范式 + 框架 + 协议
工程化 & 部署模块       = 生产化：可观测/测试/部署（落地）

一句话分工：本模块回答"什么时候该拆成多个 Agent、用哪个框架编排、怎么防协作失控"
```

> 🎯 **核心价值**：2026 年多 Agent 编排已进入"范式收敛 + 框架分化"阶段——Supervisor/Swarm/Handoff 等编排模式成为业界共识，而框架层经历剧变：**AutoGen 官方进入维护模式**，遗产由 AG2（社区 fork）与 Microsoft Agent Framework（官方继任者）继承；LangGraph 以 ~38% 生产部署份额领跑。本模块把编排范式讲透、把主流框架的机制与版本事实讲清，最后给出可执行的选型与生产实践。

## 2. 知识体系导图

```text
多 Agent（Multi‑Agent）框架知识体系
│
├── 00 总览（本文件）
│
├── 全景篇
│   └── 01 编排全景与选型 ── 市场格局 / AutoGen 三分裂 / 选型决策树
│
├── 范式篇
│   └── 02 编排范式与模式 ── Supervisor / Swarm / Handoff / Router / 层级
│
├── 框架深潜篇
│   ├── 03 LangGraph ── 生产编排标杆：supervisor / subagents / checkpointer
│   ├── 04 CrewAI ── 角色化 Crew：Agent/Task/Crew/Process + Flows
│   ├── 05 AG2 与 Agent Framework ── AutoGen 遗产的两位继承者
│   └── 06 Google ADK 与新兴框架 ── ADK 2.0 / Swarm 遗产 / 轻量方案
│
├── 协议篇
│   └── 07 A2A 与 MCP：多 Agent 协作的协议层
│
├── 生产篇
│   └── 08 多 Agent 生产实践 ── 记忆 / 可观测 / 评估 / 成本 / 防失控
│
├── 对比篇
│   └── 09 跨框架对比与选型实战 ── 同需求多实现 / 迁移路线
│
└── 检验篇
    └── 10 面试与自测 ── 面试题 / 自测 / 毕业检查单
```

## 3. 模块导航

| 序号 | 模块 | 核心内容 | 场景 |
|:---:|------|---------|------|
| 01 | 编排全景与选型 | 市场格局、AutoGen 三分裂、选型决策树 | 先读（建立全局） |
| 02 | 编排范式与模式 | Supervisor/Swarm/Handoff/Router 四模式 | 决定"怎么拆"时 |
| 03 | LangGraph | supervisor/subagents/checkpointer/time-travel | 需要生产级状态编排时 |
| 04 | CrewAI | 角色化 Crew、Flows、checkpointing | 快速原型/内容流水线时 |
| 05 | AG2 与 Agent Framework | AutoGen 遗产继承、MAF 1.0 GA | 存量 AutoGen 迁移时 |
| 06 | ADK 与新兴框架 | ADK 2.0、Swarm 遗产、轻量方案 | Google 生态/轻量编排时 |
| 07 | A2A 与 MCP | 协议层在多 Agent 协作的角色 | 跨框架互操作时 |
| 08 | 多 Agent 生产实践 | 记忆/可观测/评估/成本/防失控 | 上线前必读 |
| 09 | 跨框架对比与选型 | 同需求多实现、迁移路线 | 动手前/迁移时 |
| 10 | 面试与自测 | 面试题、自测、毕业检查单 | 求职/自检 |

## 4. 学习路线推荐

**路线 A：快速上手（2 天）**——01 → 02 → 03（或 04，按生产/原型偏好）→ 09
> 先懂范式再精一个框架，最后用对比核对选型。

**路线 B：全面掌握（1 周）**——01 → 02 → 03 → 04 → 05 → 06 → 07 → 08 → 09
> 全部框架过一遍，重点对比"编排范式 × 框架机制"两个维度。

**路线 C：面试冲刺（2 天）**——10 → 01 → 02 → 08
> 以题为纲：先会答"多 Agent 值不值得/怎么防失控"类问题，再补细节。

## 5. 核心概念速查

| 概念 | 一句话 | 关键数字 |
|------|--------|---------|
| 多 Agent 编排 | 多个 Specialist Agent 通过某种控制流协作完成复杂任务 | 2026 主流：Supervisor/Swarm/Handoff/Router |
| Supervisor 模式 | 中心化编排：一个 orchestrator 路由分发任务给 Worker | 适合 3-10 个 Agent，企业最常见 |
| Swarm 模式 | 去中心化：Agent 之间用 handoff 工具直接移交 | 适合 5-15 个 Agent，需防循环移交 |
| subagents 模式 | 2026 LangGraph 推荐：子 Agent 包装为 @tool 调用 | 取代 langgraph-supervisor 包 |
| LangGraph | 图状态机编排，生产部署份额第一 | ~38% 生产部署（2026 Q1）；SDK 0.3.14（2026-05） |
| CrewAI | 角色化 Crew 抽象，原型最快 | v1.15.2（2026 年中）；~52.4k stars；宣称 60% Fortune 500 采用 |
| AutoGen 维护模式 | 官方停止新功能（2025-10 起） | v0.7.5 最后新功能（2025-09-30）；2026 Q3 API 弃用预期 |
| AG2 | AutoGen 社区 fork（原班人马），Apache 2.0 | 向 v1.0 演进；pip install autogen 已指向 AG2 |
| Microsoft Agent Framework | AutoGen + Semantic Kernel 合并的官方继任者 | 1.0 GA（2026-04-03），Python/.NET，MIT |
| Google ADK | 代码优先，Google 云原生 | ADK 2.0 workflow runtime（2026-05/06）；Java/Go 1.0 |
| A2A | Agent 间协作协议（跨厂商） | 2026 标准化；ADK/CrewAI 原生支持 |
| checkpointing | 每节点保存状态，支持恢复/分叉/时间旅行 | LangGraph 与 CrewAI 2026 均有 |
| 防失控三件套 | 循环检测 / hop 预算 / 人工升级 | Swarm 生产必备 |

## 6. 与关联体系的分工

| 体系 | 分工 | 本模块怎么用 |
|------|------|------------|
| [Agent 内核理论模块](../../Agent%20内核理论模块/) | Agent 原理、循环概念 | 多 Agent 的基础是单 Agent 循环 |
| [单 Agent 框架](../单%20Agent%20框架/00-总览：单%20Agent%20框架知识体系.md) | 单 Agent 框架的机制与选型 | 本模块的 Worker/Agent 实现基于单 Agent 框架（如 LangGraph create_agent） |
| [低代码 Agent 平台](../低代码%20Agent%20平台（原型快速验证）/00-总览：低代码%20Agent%20平台知识体系.md) | 平台级多 Agent（Coze 3.0 团队协作等） | 低代码 vs 代码框架的多 Agent 编排对照 |
| [主流 Agent 范式](../../主流%20Agent%20范式/) | 工作流/单 Agent/多 Agent 范式 | 何时该上多 Agent 的判断依据 |
| [Agent与MCP协议](../../Agent与MCP协议/) | MCP 协议原理 | 本模块 07 篇扩展 A2A，MCP 是其基础 |
| [工程化 & 部署模块](../../Agent%20工程化%20&%20部署模块（从%20demo%20到可用应用）/00-总览：Agent%20工程化与部署模块（从%20demo%20到可用应用）.md) | 可观测/测试/部署 | 多 Agent 生产化的落地（08 篇） |
| [Function Calling 体系](../../../02-大模型基础与Prompt工程/Function%20Calling%20函数调用【Agent%20基石】/00-FunctionCalling知识体系总览.md) | 工具调用协议层 | handoff 本质是特殊工具调用 |

## 7. 2026 版本窗口

> 本模块以 2026-08 为基准，以下为检索到的版本事实（详见各篇【参考来源】）：
>
> - **LangGraph**：生产部署份额 ~38%（2026 Q1 估计）；SDK 0.3.14（2026-05）；`langgraph-supervisor` 包弃用 → subagents 模式（`create_agent` + 子 Agent 包装为 `@tool`）；`@langchain/langgraph-swarm` npm v1.0.2
> - **CrewAI v1.15.2**（2026 年中）：四大原语（Agent/Task/Crew/Process）+ Flows；v1.14.2 起 checkpointing/forking；A2A 增强；plan-execute 模式（宣称幻觉循环 -40-60%）；Crew Studio 自动化构建器
> - **AutoGen 三分裂**（2026 最大事件）：经典 AutoGen 维护模式（最后新功能 v0.7.5，2025-09-30）；AG2 社区 fork 向 v1.0 演进；Microsoft Agent Framework 1.0 GA（2026-04-03，Python/.NET，MIT）
> - **Google ADK**：Java 1.0 / Go 1.0（2026 初）；ADK 2.0 workflow runtime（2026-05/06）补确定性执行/Fan-out/重试/HITL
> - **协议**：A2A 跨厂商标准化推进中（ADK/CrewAI 原生支持）；MCP 月下载量 ~9700 万
> - **市场**：Gartner 预测 2026 年底 40% 企业应用含任务特定 AI Agent（2025 初 <5%）
> - 未确认项：CrewAI "60% Fortune 500 / 60M 月执行"、5.76x 性能对比等第三方数据，标注为方向性参考

---

**下一模块**：[01 编排全景与选型](01-多%20Agent%20编排全景与选型：2026%20框架地图.md)　**返回上级**：[Agent 开发框架层（工程加速）](../)

## 【参考来源】

- [LangGraph Multi-Agent Patterns (langgraph-101)](https://deepwiki.com/langchain-ai/langgraph-101/6-utilities)
- [Migrate from langgraph-supervisor - LangChain Docs](https://docs.langchain.com/oss/python/migrate/langgraph-supervisor)
- [CrewAI Changelog](https://docs.crewai.com/v1.15.2/en/changelog)
- [CrewAI Studio: The Automated Agent Builder](https://crewai.com/blog/crew-studio-automated-agent-builder)
- [Microsoft Deprecates AutoGen: What the Pivot Means](https://agentmarketcap.ai/blog/2026/04/05/microsoft-deprecates-autogen-agent-framework-pivot)
- [AutoGen v0.4 vs AG2: The Microsoft Community Split Explained](https://blckalpaca.at/en/knowledge-base/ai-agents/ai-agent-frameworks-comparison/autogen-vs-ag2)
- [Multi-Agent Orchestration Frameworks 2026 (Presenc AI)](https://presenc.ai/research/multi-agent-orchestration-frameworks-2026)
- [Best Multi-Agent Frameworks 2026 (FutureAGI)](https://futureagi.com/blog/best-multi-agent-frameworks-2026/)
- [State of AI Agents — March 2026](https://github.com/zzhiyuann/state-of-ai-agents)
- [The best AI agent frameworks in 2026 (LangChain)](https://www.langchain.com/resources/ai-agent-frameworks)
