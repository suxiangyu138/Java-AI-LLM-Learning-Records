# 05 AG2 与 Microsoft Agent Framework：AutoGen 遗产的继承者

> 定位：框架深潜第三站——AutoGen 三分裂后两位继承者的机制与选型：AG2（社区 fork，事件驱动重构）与 Microsoft Agent Framework（官方继任者，1.0 GA）（2026-08 基准）

## 📚 目录

1. [AutoGen 遗产回顾：为什么它值得了解](#1-autogen-遗产回顾为什么它值得了解)
2. [两位继承者的定位](#2-两位继承者的定位)
3. [AG2：事件驱动的开源 AgentOS](#3-ag2事件驱动的开源-agentos)
4. [AG2 核心机制](#4-ag2-核心机制)
5. [Microsoft Agent Framework：官方继任者](#5-microsoft-agent-framework官方继任者)
6. [MAF 核心能力](#6-maf-核心能力)
7. [三者对照与迁移决策](#7-三者对照与迁移决策)
8. [核心要点](#8-核心要点)

## 1. AutoGen 遗产回顾：为什么它值得了解

AutoGen 是多 Agent 对话模式的**开创者**（2023 年发布），贡献了群聊（GroupChat）、辩论、HumanProxy 等范式，至今仍是研究论文的常用框架。但：

```text
2025-09-30  v0.7.5 ── 最后一个带新功能的版本
2025-10     官方宣布 AutoGen + Semantic Kernel 合并 → Agent Framework
2026-02     Release Candidate；AutoGen/SK 确认进入维护模式
2026-04-03  Agent Framework 1.0 GA（Python/.NET）
2026 Q3     预期 AutoGen 经典 API 弃用
```

> ⚠️ **对学习者的意义**：经典 AutoGen 的**概念遗产**（对话式多 Agent、GroupChat、HumanProxy）仍值得学——它定义的问题被所有后继者继承；但**新项目一律不要用经典 AutoGen**。

## 2. 两位继承者的定位

| 维度 | AG2 | Microsoft Agent Framework (MAF) |
|------|-----|-------------------------------|
| 出身 | AutoGen 原班人马（Chi Wang/Qingyun Wu）社区 fork | 微软官方（AutoGen + Semantic Kernel 合并） |
| 定位 | "The Open-Source AgentOS"，事件驱动重构 | 微软官方 Agent 开发栈（.NET/Azure 生态） |
| 许可 | Apache 2.0 | MIT |
| 语言 | Python | Python + .NET/C# 双栈 |
| 状态 | 活跃开发，向 v1.0 演进 | 1.0 GA（2026-04-03），生产 SLA + LTS |
| 适合 | 存量 AutoGen 用户无缝续接、研究 | Azure/.NET 企业新项目 |
| 生态数据 | ~4.3k stars，维护者少 | 微软官方支持 |

> 🎯 **选型一句话**：想要 AutoGen 风格的延续 → AG2；想要微软官方支持的生产级 SDK → MAF。

## 3. AG2：事件驱动的开源 AgentOS

- **`pip install autogen` 现在解析到 AG2**——存量用户的迁移路径几乎无缝（v0.2 API 兼容）
- 从 AutoGen 的**对话驱动**重构为**事件驱动**：核心是 MemoryStream pub/sub 事件总线
- 版本路线：v0.12 → v0.13 → v0.14 → v1.0（2026 已到 v0.13.3/v20260328a 线）
- 多供应商 LLM：OpenAI、Anthropic、Gemini、Qwen、Ollama
- 持续修复 CVE（安全更新活跃）

## 4. AG2 核心机制

| 机制 | 说明 |
|------|------|
| MemoryStream | pub/sub 事件总线：Agent 之间通过事件流解耦通信（取代直连对话） |
| 依赖注入 | 组件可替换（LLM/工具/记忆/存储） |
| 类型化工具 | 强类型工具定义 |
| 对话模式保留 | GroupChat、辩论、HumanProxy 等 AutoGen 遗产仍在 |
| 存储后端 | 多后端（文件/数据库/Redis 等） |

```python
# AG2 的对话模式（延续 AutoGen v0.2 API）
from autogen import ConversableAgent

assistant = ConversableAgent("assistant", llm_config={"model": "gpt-4o"})
user = ConversableAgent("user", human_input_mode="TERMINATE")
result = user.initiate_chat(assistant, message="分析这份报告")
```

```python
# AG2 的事件驱动（MemoryStream）
from autogen import AgentRuntime
from autogen.memorystream import MemoryStream

stream = MemoryStream()
runtime = AgentRuntime(stream)
# Agent 订阅/发布事件，通过事件流协作（2026 新架构）
```

| 注意 | 说明 |
|------|------|
| 维护规模 | 维护者核心小组小（原班人马），文档变动频繁（实测反馈） |
| 定位差 | 事件驱动是亮点也是学习成本——传统对话模式迁移到事件流需要心智转换 |

## 5. Microsoft Agent Framework：官方继任者

| 维度 | 2026 事实 |
|------|----------|
| 版本 | 1.0 GA（2026-04-03，Python/.NET 同时）；公开预览 2025-10 → RC 2026-02 |
| 许可 | MIT |
| 构成 | AutoGen 的编排能力 + Semantic Kernel 的企业集成能力 |
| 承诺 | 稳定 API、生产 SLA、长期支持（LTS） |
| 互操作 | 原生 MCP + A2A |
| 生态 | Azure AI Foundry / .NET 深度集成 |

## 6. MAF 核心能力

| 能力 | 说明 |
|------|------|
| 多 Agent 编排 | 继承 AutoGen 的群聊/层级编排 |
| 多供应商模型 | Semantic Kernel 的模型抽象（OpenAI/Anthropic/本地等） |
| 企业集成 | Azure 服务（托管身份/密钥保管库/部署） |
| A2A | 原生支持 Agent 间协作协议 |
| MCP | 原生工具接入 |
| Python/.NET 对称 | 双栈 API 对称，跨团队一致 |

```python
# MAF 简例（Python，概念性）
from agent_framework import Agent, AgentRuntime, GroupChat

sales_agent = Agent(name="sales", system_prompt="你是销售专家", model="gpt-4o")
support_agent = Agent(name="support", system_prompt="你是售后专家", model="gpt-4o")
chat = GroupChat([sales_agent, support_agent])
await chat.run("客户咨询套餐")
```

## 7. 三者对照与迁移决策

| 场景 | 选择 |
|------|------|
| 存量 AutoGen v0.2 项目续接 | **AG2**（API 兼容，无缝） |
| Azure/.NET 企业新项目 | **MAF**（官方支持 + SLA） |
| 研究/群聊/辩论范式 | AG2（或经典 AutoGen 只读参考） |
| 新项目（无微软绑定） | **LangGraph / CrewAI**（01 篇决策树） |

```text
迁移检查清单（AutoGen → 新框架）：
□ 盘点用到的 AutoGen 能力（GroupChat? HumanProxy? 工具调用?）
□ 语言栈（Python 续 AG2 / 需要 .NET 上 MAF）
□ 云生态绑定（Azure → MAF）
□ 成本模型（群聊 20+ 调用/交互 —— 重构时评估是否真需要多 Agent）
```

> 💡 **最重要的建议**：存量 AutoGen 迁移的**第一问不是"迁到哪个框架"，而是"这个交互真的需要多 Agent 吗"**——AutoGen 群聊单交互 20+ LLM 调用，多数场景单 Agent + 工具链更划算。

## 8. 核心要点

> 🎯 **核心要点**：
> 1. AutoGen 遗产 = 对话式多 Agent 范式（群聊/辩论/HumanProxy）——概念值得学，代码别用
> 2. **AG2** = 原班人马社区 fork，事件驱动重构（MemoryStream），`pip install autogen` 已指向它，存量用户无缝续接
> 3. **MAF** = 官方继任者（AutoGen + Semantic Kernel），1.0 GA（2026-04-03），MIT，Azure/.NET 企业首选
> 4. 迁移第一问：这个交互真的需要多 Agent 吗？——群聊模式成本极高

---

**上一模块**：[04 CrewAI](04-CrewAI：角色化%20Crew%20与%20Flows.md)　**下一模块**：[06 Google ADK 与新兴框架](06-Google%20ADK%20与新兴框架：Swarm%20遗产与轻量方案.md)　**返回总览**：[00 总览](00-总览：多%20Agent（Multi‑Agent）框架知识体系.md)

## 【参考来源】

- [Microsoft Deprecates AutoGen for Agent Framework (AgentMarketCap)](https://agentmarketcap.ai/blog/2026/04/05/microsoft-deprecates-autogen-agent-framework-pivot)
- [AutoGen v0.4 vs AG2: The Microsoft Community Split Explained (Blck Alpaca)](https://blckalpaca.at/en/knowledge-base/ai-agents/ai-agent-frameworks-comparison/autogen-vs-ag2)
- [AutoGen Explained: Status, Architecture and Alternatives [2026] (Atlan)](https://atlan.com/know/ai-agent/what-is-autogen/)
- [AG2 — Framework (LLM Explorer)](https://llm-explorer.com/agent/ag2)
- [Microsoft AutoGen (AgentMarketCap)](https://agentmarketcap.ai/agents/microsoft-autogen)
- [Microsoft AutoGen Review 2026 (AIAgentSquare)](https://aiagentsquare.com/agents/autogen)
- [Multi-Agent Orchestration Frameworks 2026 (Presenc AI)](https://presenc.ai/research/multi-agent-orchestration-frameworks-2026)
