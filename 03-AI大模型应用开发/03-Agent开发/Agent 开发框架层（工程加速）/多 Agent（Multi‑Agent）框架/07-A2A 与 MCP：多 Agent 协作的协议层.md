# 07 A2A 与 MCP：多 Agent 协作的协议层

> 定位：协议篇——多 Agent 协作的两层协议：MCP（Agent↔工具）与 A2A（Agent↔Agent）；2026 年协议收敛趋势与跨框架互操作实践（2026-08 基准）

## 📚 目录

1. [两层协议的边界](#1-两层协议的边界)
2. [MCP：Agent 与工具的统一协议](#2-mcpagent-与工具的统一协议)
3. [A2A：Agent 与 Agent 的协作协议](#3-a2aagent-与-agent-的协作协议)
4. [A2A 核心机制](#4-a2a-核心机制)
5. [2026 框架的协议支持矩阵](#5-2026-框架的协议支持矩阵)
6. [跨框架互操作实战](#6-跨框架互操作实战)
7. [协议选型与落地建议](#7-协议选型与落地建议)
8. [核心要点](#8-核心要点)

## 1. 两层协议的边界

```text
2026 协议栈分工（行业共识）：

MCP（Model Context Protocol）   A2A（Agent-to-Agent）
┌──────────────────────────┐   ┌──────────────────────────┐
│ Agent ↔ 工具/资源/能力     │   │ Agent ↔ Agent（任务/状态）  │
│ 单个 Agent 的能力扩展       │   │ 多个 Agent 的协作编排      │
│ 97M+ 月下载（2026）        │   │ 2025 提出，2026 标准化推进  │
│ 事实标准（工具层）          │   │ 跨厂商编排标准（演进中）     │
└──────────────────────────┘   └──────────────────────────┘

一句话：MCP 解决"Agent 怎么用工具"，A2A 解决"Agent 怎么找对方、交任务、同步状态"
```

> 🎯 **为什么分层**：工具的协议（MCP）与协作的协议（A2A）解耦——同一个 Agent 既可以通过 MCP 接任意工具，也可以通过 A2A 与其他厂商的 Agent 协作。2026 年两者已成为框架的标配能力。

## 2. MCP：Agent 与工具的统一协议

（详细机制见 [MCP 协议体系](../../Agent与MCP协议/)——本篇从多 Agent 视角看其角色）

| 在多 Agent 中的角色 | 说明 |
|--------------------|------|
| 工具共享 | 多个 Agent 共享同一 MCP 工具服务器（工具注册表集中管理） |
| 子 Agent 即工具 | LangGraph subagents 模式：子 Agent 包装为 @tool——工具协议与 Agent 协议同构 |
| 技能封装 | 低代码平台/Langflow 导出流程为 MCP Server 供 Agent 调用（跨体系互操作） |
| 安全边界 | 按 Agent 授权工具子集（最小权限） |

## 3. A2A：Agent 与 Agent 的协作协议

| 维度 | 2026 事实 |
|------|----------|
| 提出 | 2025-04（Google 主导 + 50+ 参与方） |
| 2026 状态 | 标准化推进中；ADK/CrewAI 原生支持领先 |
| 解决 | Agent 发现（Agent Cards）、任务委派、状态同步、能力协商 |
| 对比 MCP | MCP 是工具协议（请求-响应）；A2A 是协作协议（任务生命周期） |
| 意义 | **跨厂商编排**：LangGraph 编排者可委派 Google ADK Agent、CrewAI Crew |

```text
A2A 解决的核心问题（单框架内的多 Agent 不需要它）：
框架内   = 内存中的对象调用（快但锁定在单一框架）
跨框架   = A2A 协议（网络级互操作，慢但解耦）
→ 2026 主流是"框架内编排为主，A2A 连接跨框架组件"
```

## 4. A2A 核心机制

| 机制 | 说明 |
|------|------|
| Agent Card | 每个 Agent 的机器可读能力声明（发现机制） |
| Task 生命周期 | 提交任务 → 进行中 → 完成/失败（状态机） |
| 消息流 | 流式任务更新（与 LLM 流式输出对齐） |
| 更新机制 | poll / stream / push 三种（2026 各框架实现中） |
| 安全 | 端点鉴权（凭证作用域，如 CrewAI Plus tokens） |

```text
A2A 协作时序（概念）：
编排者 --Agent Card 发现--> 目标 Agent 能力
编排者 --提交 Task---------> 目标 Agent
目标 Agent --流式更新--------> 编排者
编排者 --收集结果------------> 汇总/下一环节
```

## 5. 2026 框架的协议支持矩阵

| 框架 | MCP | A2A | 备注 |
|------|:---:|:---:|------|
| LangGraph | ●●● | ◐（可集成） | MCP 深度集成；A2A 靠生态/适配 |
| CrewAI | ●●● | ●●● | A2A 原生（async 链、poll/stream/push） |
| Microsoft Agent Framework | ●●● | ●●● | 1.0 起原生双协议 |
| Google ADK | ●●● | ●●● | **A2A 规范联合开发者**，Agent Cards 自动生成 |
| AG2 | ●● | ◐ | 工具生态为主 |
| OpenAI Agents SDK | ●●● | ◐ | MCP 原生；A2A 观望 |

> ⚠️ 定性判断（2026-08 检索基准）；A2A 仍是演进中标准——跨厂商生产部署前先验证目标框架的 A2A 实现成熟度。

## 6. 跨框架互操作实战

```text
场景：LangGraph 编排者 + CrewAI Crew + ADK Agent + MCP 工具

┌──────────────┐   A2A    ┌──────────────┐
│ LangGraph     │────────▶│ ADK Agent     │（Google 栈）
│ 编排者（主管）  │────────▶│ CrewAI Crew   │（角色化流水线）
│              │────────▶│ MCP 工具服务器 │（工具共享）
└──────────────┘
```

| 层 | 连接方式 | 注意 |
|----|---------|------|
| 框架内 | 对象调用（最快） | 锁定单一框架 |
| 跨框架 | A2A 协议 | 网络开销；任务状态需同步 |
| 工具共享 | MCP | 工具注册表统一管理 |
| 平台流程 | MCP 导出（Langflow 等） | 低代码平台流程变 Agent 工具 |

> 💡 **2026 务实路线**：90% 场景**单框架内编排**（选一个框架吃透）；A2A 只在"必须接入异构组件"（Google 栈、Crew 流水线、供应商 Agent）时启用。

## 7. 协议选型与落地建议

| 问题 | 答案 |
|------|------|
| 我的 Agent 需要更多工具？ | 接 MCP（工具层事实标准） |
| 我的多 Agent 要不要 A2A？ | 同框架内不需要；跨厂商/异构编排才需要 |
| 先学哪个？ | 先 MCP（基础，全框架通用）；再 A2A（按需） |
| 落地顺序 | 框架内编排跑通 → MCP 接工具 → 需要时 A2A 接异构 Agent |
| 安全基线 | MCP 工具按 Agent 最小权限授权；A2A 端点鉴权（凭证作用域 token） |

## 8. 核心要点

> 🎯 **核心要点**：
> 1. 协议分层：MCP = Agent↔工具（事实标准，97M+ 月下载）；A2A = Agent↔Agent（标准化推进中）
> 2. A2A 四机制：Agent Card 发现 / Task 生命周期 / 流式更新 / poll-stream-push
> 3. 2026 框架标配：MAF/ADK/CrewAI 原生双协议；LangGraph MCP 深度 + A2A 生态
> 4. 务实路线：单框架内编排为主（90% 场景），A2A 只用于异构接入；先 MCP 后 A2A

---

**上一模块**：[06 Google ADK 与新兴框架](06-Google%20ADK%20与新兴框架：Swarm%20遗产与轻量方案.md)　**下一模块**：[08 多 Agent 生产实践](08-多%20Agent%20生产实践：记忆%20可观测%20评估%20成本%20防失控.md)　**返回总览**：[00 总览](00-总览：多%20Agent（Multi‑Agent）框架知识体系.md)

## 【参考来源】

- [State of AI Agents — March 2026 (GitHub)](https://github.com/zzhiyuann/state-of-ai-agents)
- [Multi-Agent Orchestration Frameworks 2026 (Presenc AI)](https://presenc.ai/research/multi-agent-orchestration-frameworks-2026)
- [Best Multi-Agent Frameworks 2026 (FutureAGI)](https://futureagi.com/blog/best-multi-agent-frameworks-2026/)
- [The best AI agent frameworks in 2026 (LangChain)](https://www.langchain.com/resources/ai-agent-frameworks)
- [An Agent Is a Service: Where Agent Frameworks Are Going (go-micro)](https://go-micro.dev/blog/32)
- [Agentic-AI-Orchestration ch07: Swarm architecture (GitHub)](https://github.com/zahurul-islam/Agentic-AI-Orchestration/blob/main/ch07-swarm-ai-architecture/starter/support_swarm.py)
