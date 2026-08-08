# 06 Google ADK 与新兴框架：Swarm 遗产与轻量方案

> 定位：框架深潜第四站——Google ADK（代码优先、多语言、云原生）、OpenAI Swarm 遗产（极简 handoff 范式）、以及"单 Agent 工具化"轻量方案（2026-08 基准）

## 📚 目录

1. [Google ADK 定位](#1-google-adk-定位)
2. [ADK 2.0：workflow runtime 补齐确定性](#2-adk-20workflow-runtime-补齐确定性)
3. [ADK 核心能力](#3-adk-核心能力)
4. [ADK 适用边界](#4-adk-适用边界)
5. [OpenAI Swarm 遗产：极简 handoff 范式](#5-openai-swarm-遗产极简-handoff-范式)
6. [轻量方案：单 Agent 工具化嵌套](#6-轻量方案单-agent-工具化嵌套)
7. [新兴框架扫描](#7-新兴框架扫描)
8. [核心要点](#8-核心要点)

## 1. Google ADK 定位

| 维度 | 2026 事实 |
|------|----------|
| 定位 | 代码优先（code-first）、生产就绪的 Agent 开发工具包 |
| 语言 | **Python / Java / Go / TypeScript**（最广覆盖）；Java 1.0、Go 1.0 于 2026 初发布；Kotlin/Android beta |
| 云 | 原生 Vertex AI 集成，可部署到 Vertex AI Agent Engine / Cloud Run / GKE |
| 协议 | 原生 MCP + A2A（**联合开发 A2A 规范的成员**），自动生成 Agent Cards |
| 内置 | Memory Bank、pytest 评估工具、Web UI 调试器 |
| 市场 | 生产部署 ~4%（2026 Q1 估计），Google 系产品内部在用 |

> 🎯 **一句话**：ADK = "Google 生态的代码优先 Agent 框架"——多语言 SDK 与 Vertex AI 深度集成是独有优势；**离开 Google 生态价值快速衰减**。

## 2. ADK 2.0：workflow runtime 补齐确定性

ADK 早期最大短板是**确定性编排缺失**（Agent 循环不可控）。2026-05/06 的 ADK 2.0 workflow runtime 补齐：

| 能力 | 说明 |
|------|------|
| 确定性执行 | 显式工作流定义（不再只有自由 Agent 循环） |
| Fan-out | 并行分发子任务 |
| 重试 | 节点级重试策略 |
| HITL | 人工介入节点 |

```text
ADK 1.x：只有 Agent 自由循环（规划-执行-反思）
ADK 2.0：+ 确定性 workflow runtime（节点/并行/重试/HITL）
→ 对齐 LangGraph/CrewAI Flows 的能力基线（2026 框架共识：两类执行都要有）
```

## 3. ADK 核心能力

| 能力 | 说明 |
|------|------|
| 多语言对称 | Python/Java/Go/TS 四栈 API 对称（企业中多语言团队加分） |
| Vertex AI 集成 | 部署链路（Agent Engine 托管/评估/监控）开箱即用 |
| A2A 原生 | 与 Google 联合推动 A2A 标准；跨厂商编排（LangGraph 编排者 → ADK Agent） |
| 内置评估 | pytest 风格 eval 工具 |
| 调试 | Web UI 调试器（会话/状态可视化） |
| 记忆 | 内置 Memory Bank |

```python
# ADK 简例（Python，概念性）
from google.adk.agents import Agent
from google.adk.tools import FunctionTool

search_agent = Agent(name="search", model="gemini-2.5-pro",
                     tools=[FunctionTool(web_search)])
analysis_agent = Agent(name="analysis", model="gemini-2.5-flash")

# ADK 2.0 workflow：确定性编排
workflow = Workflow(nodes=[search_agent, analysis_agent], fan_out=True)
```

## 4. ADK 适用边界

| 优势场景 | 劣势场景 |
|---------|---------|
| 已在 Google Cloud/Gemini 栈 | 非 Google 生态（价值衰减快） |
| 多语言团队（Java/Go/Python/TS） | 多供应商模型需适配器（如 LiteLLM 包装，有实测延迟） |
| 需要 A2A 跨厂商互操作 | 学习曲线陡 |
| 企业级部署（Vertex Agent Engine） | 部分 SDK 仍 beta；Cloud Run 会话持久化需显式外部存储 |

> 🎯 **适用结论**：ADK 的选型判据是**生态**——已在 Google 栈或用多语言 SDK，选 ADK；否则 LangGraph/CrewAI 更通用。

## 5. OpenAI Swarm 遗产：极简 handoff 范式

OpenAI Swarm（2024 实验项目，官方已归档）虽已退役，其**极简 handoff 心智模型**被整个行业吸收：

| Swarm 概念 | 2026 去向 |
|-----------|----------|
| Agent = 指令 + 工具 + 函数 | 成为各家通用抽象 |
| Handoffs = 移交即函数返回 | OpenAI Agents SDK Handoffs 原生化；LangGraph `create_handoff_tool` |
| 极简循环（run loop 几十行） | 范式保留在 OpenAI Agents SDK |
| 无持久化/无状态 | 各家补 checkpointer（2026 基线能力） |

```text
Swarm 的历史地位：证明了"多 Agent 可以极简"——
  handoff 本质是特殊工具调用，不需要重量级编排框架。
→ 现代实现：OpenAI Agents SDK（五原语）、LangGraph handoff、CrewAI sequential
```

> 💡 **学习价值**：Swarm 源码（~2000 行）是理解 handoff 范式的最佳教材——先读它，再学框架的封装。

## 6. 轻量方案：单 Agent 工具化嵌套

2026 年被低估的"多 Agent"实现——**不需要多 Agent 框架**：

```text
方案：单 Agent（Claude Agent SDK / OpenAI Agents SDK）+ 子 Agent 包装为工具
机制：Agent 把"另一个 Agent"当工具调用
优点：无需编排框架；状态/记忆由外层 Agent 管理；成本可控
实现：LangGraph subagents 模式同思路；Claude Agent SDK 工具嵌套
```

| 对比 | 轻量嵌套 | 专用多 Agent 框架 |
|------|---------|-----------------|
| 上手 | 已有单 Agent 即可 | 学框架 |
| 控制流 | 靠外层 Agent 决策 | 显式图/流程 |
| 持久化 | 手动 | checkpointer |
| 适合 | 2-4 个角色、简单协作 | 复杂编排、10+ Agent |

## 7. 新兴框架扫描

| 框架 | 2026 状态 | 关注点 |
|------|----------|--------|
| OpenAI Agents SDK | 活跃（五原语成熟，见单 Agent 模块） | Handoffs 原生；Assistants API 弃用后官方推荐 |
| Claude Agent SDK | 活跃（完整 harness） | 多 Agent 靠工具嵌套，无原生编排层 |
| AgentOS / 其他 | 概念期 | "Agent 操作系统"叙事 vs 实用框架差距 |
| 语言侧框架（LangChain4j/Spring AI） | Java 生态多 Agent 雏形 | Java 学习者对照（见 [Java AI 生态](../../../05-AI开发框架/Java%20AI生态/)） |

> ⚠️ 新兴框架评估原则：2026 框架格局已收敛（LangGraph/CrewAI/MAF/ADK 四强 + OpenAI/Claude 原生），**新框架先看"解决了四强没解决的问题吗"**，再投精力。

## 8. 核心要点

> 🎯 **核心要点**：
> 1. Google ADK：多语言（Py/Java/Go/TS）+ Vertex AI 原生 + A2A 联合方——Google 生态判据
> 2. ADK 2.0 workflow runtime（2026-05/06）补齐确定性执行，对齐 2026 框架共识（自由循环 + 确定性工作流双轨）
> 3. Swarm 遗产 = 极简 handoff 范式：handoff 是特殊工具调用——已融入 Agents SDK/LangGraph
> 4. 别忘轻量方案：单 Agent + 子 Agent 工具化嵌套，多数 2-4 角色场景不需要专用多 Agent 框架

---

**上一模块**：[05 AG2 与 Agent Framework](05-AG2%20与%20Microsoft%20Agent%20Framework：AutoGen%20遗产的继承者.md)　**下一模块**：[07 A2A 与 MCP](07-A2A%20与%20MCP：多%20Agent%20协作的协议层.md)　**返回总览**：[00 总览](00-总览：多%20Agent（Multi‑Agent）框架知识体系.md)

## 【参考来源】

- [Best Multi-Agent Frameworks 2026: 7 Platforms Ranked (FutureAGI)](https://futureagi.com/blog/best-multi-agent-frameworks-2026/)
- [Multi-Agent Orchestration Frameworks 2026 (Presenc AI)](https://presenc.ai/research/multi-agent-orchestration-frameworks-2026)
- [AI Agent Frameworks (2026 Update): 8 SDKs Compared (MorphLLM)](https://www.morphllm.com/ai-agent-framework)
- [State of AI Agents — March 2026 (GitHub)](https://github.com/zzhiyuann/state-of-ai-agents)
- [An Agent Is a Service: Where Agent Frameworks Are Going (go-micro)](https://go-micro.dev/blog/32)
- [Migrate from langgraph-supervisor - LangChain Docs](https://docs.langchain.com/oss/python/migrate/langgraph-supervisor)
