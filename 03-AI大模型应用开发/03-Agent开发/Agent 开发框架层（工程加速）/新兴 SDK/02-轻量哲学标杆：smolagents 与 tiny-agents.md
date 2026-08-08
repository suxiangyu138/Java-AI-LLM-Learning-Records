# 02 轻量哲学标杆：smolagents 与 tiny-agents

> 定位：轻量哲学的两大标杆——代码执行型（smolagents，Agent 写代码）vs 配置驱动型（tiny-agents，JSON+MCP）；两派哲学对比与选型（2026-08 基准）

## 📚 目录

1. [轻量哲学的两个方向](#1-轻量哲学的两个方向)
2. [smolagents：代码执行型标杆](#2-smolagents代码执行型标杆)
3. [tiny-agents：配置驱动型标杆](#3-tiny-agents配置驱动型标杆)
4. [两派哲学对比](#4-两派哲学对比)
5. [生产可靠性争议](#5-生产可靠性争议)
6. [什么时候选哪一派](#6-什么时候选哪一派)
7. [核心要点](#7-核心要点)

## 1. 轻量哲学的两个方向

2026 年轻量框架形成两派，解决同一个问题（"怎么让 Agent 最小化"）的两条路：

```text
代码执行派（smolagents 系）         配置驱动派（tiny-agents 系）
Agent 写 Python 代码执行             Agent 由 JSON 配置 + MCP 定义
核心 <1K 行                          核心 ~50 行（JS）
需要沙箱（E2B/Docker/本地）           无需沙箱（MCP 隔离即边界）
多步推理/工具编排强                  最快出 demo、最简单
生态丰富、可上 Hub                   零环境依赖、任意环境跑
```

> 🎯 **一句话**：smolagents = "让 Agent 会编程"；tiny-agents = "让 Agent 可配置"——前者是能力的进化，后者是部署的退化。

## 2. smolagents：代码执行型标杆

（框架机制深潜见[单 Agent 框架模块 04](../单%20Agent%20框架/04-Smolagents：代码执行型%20Agent.md)——本篇聚焦轻量哲学与生态定位）

| 维度 | 事实 |
|------|------|
| 定位 | Hugging Face 轻量框架（transformers.agents 继任者），核心 <1K 行 |
| 双 Agent | CodeAgent（写代码执行）+ ToolCallingAgent（JSON 工具调用） |
| 沙箱 | E2B / Modal / Docker / Blaxel / Pyodide |
| 集成 | MCP 工具、LiteLLM（HF/本地/云任意模型） |
| 多 Agent | 有限：AgentTool（Agent 作为工具） |
| 生态 | `agent.push_to_hub()` 一键共享 |

```python
# smolagents 简例（概念性）
from smolagents import CodeAgent, HfApiModel

agent = CodeAgent(tools=[web_search], model=HfApiModel())
result = agent.run("调研 2026 年多 Agent 框架格局并总结")
# Agent 生成并执行 Python 代码完成调研
```

## 3. tiny-agents：配置驱动型标杆

| 维度 | 事实 |
|------|------|
| 定位 | "minimal code, maximum interoperability"——~50 行 JS（Python 同思路） |
| 机制 | JSON 配置（agent.json：model/provider/MCP servers）→ 直接运行 |
| 运行 | `npx @huggingface/tiny-agents run "org/agent-name"` |
| 沙箱 | **无内置**——依赖 MCP server 隔离作为安全边界 |
| 亮点模型 | Qwen 3 30B-A3B 工具调用表现突出 |

```json
// agent.json 示例（概念性）
{
  "name": "search-agent",
  "model": { "provider": "openai", "model": "gpt-4o-mini" },
  "mcpServers": [{ "url": "https://mcp.example.com/search" }]
}
```

> 💡 **官方指引**：大多数起步者用 tiny-agents 更快出可用 demo；需要多步推理/代码执行/自定义工具编排时升级到 smolagents——**两派是梯度不是对立**。

## 4. 两派哲学对比

| 维度 | smolagents（代码执行） | tiny-agents（配置驱动） |
|------|----------------------|----------------------|
| Agent 交互形态 | 生成并执行代码 | 工具调用（MCP） |
| 核心体量 | <1K 行 | ~50 行 |
| 沙箱 | 必需（代码执行安全） | 无（MCP 隔离即边界） |
| 上手速度 | 分钟级 | **秒级** |
| 能力上限 | 高（任意代码逻辑） | 中（MCP 工具组合） |
| 安全模型 | 沙箱强隔离 | 依赖 MCP server 自身安全 |
| 生态 | HF Hub 深度集成 | npm/PyPI 即插即用 |
| 生产可靠性 | 社区有疑虑（2026-03 讨论） | 适合简单自动化 |

## 5. 生产可靠性争议

- 2026-03 社区讨论指出 smolagents 存在可靠性问题；Pydantic AI 与 LlamaIndex 被认为更生产可用
- smolagents 的优势区间：**研究、原型、与 Hub 生态深度集成**
- 生产选型建议：评估时用真实工作流测试（见 09 篇两周验证法），不因"轻量"假定可靠性

> ⚠️ **轻量 ≠ 生产就绪**：百行核心的代价是运行时能力（重试/检查点/可观测）由框架自带较少——生产前补齐这些，或选生产运行时类框架（Flue/lantern，03 篇）。

## 6. 什么时候选哪一派

```text
第一问：Agent 需要写代码执行吗？
├─ 需要（复杂逻辑/文件处理/多步计算）→ smolagents 系（代码执行派）
├─ 不需要（查工具/调 API/简单编排）→ tiny-agents 系（配置驱动派）
└─ 不确定 → 先用配置驱动派出 demo，不够再升级

第二问：安全边界怎么定？
├─ 有沙箱条件（Docker/E2B）→ 代码执行派可用
└─ 无沙箱 → 配置驱动派（MCP 隔离）
```

## 7. 核心要点

> 🎯 **核心要点**：
> 1. 轻量哲学两派：代码执行型（smolagents）vs 配置驱动型（tiny-agents）——能力进化 vs 部署退化
> 2. smolagents：<1K 行核心，Agent 写代码执行，沙箱必需；研究/原型/生态集成强
> 3. tiny-agents：~50 行核心，JSON+MCP 即 Agent，秒级上手；适合快速 demo 与简单自动化
> 4. 两派是梯度：先配置驱动出 demo，需要多步推理/代码时升级 smolagents；生产前用真实工作流验证可靠性

---

**上一模块**：[01 新兴框架全景](01-新兴框架全景：2026%20轻量%20Agent%20SDK%20地图.md)　**下一模块**：[03 微框架光谱](03-微框架光谱：PocketFlow%20Flue%20TinyAgent%20Micro-Agent%20GenericAgent.md)　**返回总览**：[00 总览](00-总览：新兴%20SDK%20知识体系.md)

## 【参考来源】

- [SmolAgents: Definition & FutureAGI Guide (2026)](https://futureagi.com/glossary/smolagents/)
- [Smolagents + fastCRW: Web Grounding, Zero Bloat](https://fastcrw.com/blog/smolagents-fastcrw-integration)
- [GenericAgent vs SmoLAgents — Minimal Python Agent Frameworks in 2026](https://aicoolies.com/comparisons/genericagent-vs-smolagents)
- [smolagents tier-2 analysis (framework-analysis)](https://github.com/larsderidder/framework-analysis/blob/main/tier-2/smolagents.md)
- [TinyAgent (tiny-agent-os) on PyPI](https://socket.dev/pypi/package/tiny-agent-os)
- [Micro-Agent: lightweight framework for vertical domain apps (GitHub)](https://github.com/fdueblab/Micro-Agent)
