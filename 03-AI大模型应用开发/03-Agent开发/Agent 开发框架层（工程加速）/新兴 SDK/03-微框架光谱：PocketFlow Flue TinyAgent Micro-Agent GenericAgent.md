# 03 微框架光谱：PocketFlow / Flue / TinyAgent / Micro-Agent / GenericAgent

> 定位：微框架篇——五个值得关注的轻量框架的机制、差异与适用：从"100 行零依赖"到"垂域服务化"的光谱（2026-08 基准）

## 📚 目录

1. [光谱总览](#1-光谱总览)
2. [PocketFlow：100 行 LLM 框架](#2-pocketflow100-行-llm-框架)
3. [Flue：生产型 TypeScript 框架](#3-flue生产型-typescript-框架)
4. [TinyAgent：流式优先微框架](#4-tinyagent流式优先微框架)
5. [Micro-Agent：垂域服务化](#5-micro-agent垂域服务化)
6. [GenericAgent：技能结晶自进化](#6-genericagent技能结晶自进化)
7. [横向对比](#7-横向对比)
8. [核心要点](#8-核心要点)

## 1. 光谱总览

| 框架 | 语言 | 体量 | 核心卖点 | 定位 |
|------|------|------|---------|------|
| PocketFlow | 7 语言 | 100 行 | 零依赖零锁定 | 教学/嵌入/反重型 |
| Flue | TypeScript | 轻量 | 五原语 + 多环境部署 | 生产运行时 |
| TinyAgent | Python | 小型 | 流式优先 + 事件驱动 | 轻量 Agent 循环 |
| Micro-Agent | Python | <3K 行 | 垂域最短路径（API 交付） | Agent 即服务 |
| GenericAgent | Python | ~3K 行 | 技能结晶（确定性复用） | 本地自进化 |

> 🎯 **光谱逻辑**：从"最轻（教学/嵌入）"到"最生产（服务化）"——按使用目的选位置，不是按流行度。

## 2. PocketFlow：100 行 LLM 框架

| 维度 | 事实 |
|------|------|
| 定位 | "100-line LLM framework"，零依赖、零厂商锁定 |
| 能力 | 单/多 Agent、工作流、RAG、流式、护栏 |
| 移植 | TypeScript / Java / C++ / Go / Rust / PHP 七语言 |
| 卖点 | 反 LangChain（40.5 万行）与 CrewAI（1.8 万行）的"瘦身宣言" |
| 适合 | 教学、嵌入现有系统、需要读源码掌控一切的场景 |

> 💡 **价值不在性能在可读性**：100 行意味着开发者可以读懂每一个字符——适合学习"Agent 循环到底是什么"（与阶段 1 手写极简 Agent 呼应）。

## 3. Flue：生产型 TypeScript 框架

| 维度 | 事实 |
|------|------|
| 出身 | Astro 团队（2026） |
| 五原语 | Agents（持久会话）、Workflows、Sandboxes（本地/virtual/Daytona 容器）、Skills（SKILL.md 包）、Subagents（模型委派控成本） |
| 部署 | Node.js、Cloudflare Workers（Durable Objects，~12ms 冷启动）、GitHub Actions、GitLab CI、Render |
| 特点 | 沙箱与 Skills 一等公民；多环境一致执行 |

> 🎯 **Flue 的差异化**：把"沙箱 + 技能包 + 子 Agent 成本控制"做成原语——代表 2026 年"生产运行时"类轻量框架的方向。

## 4. TinyAgent：流式优先微框架

| 维度 | 事实 |
|------|------|
| 定位 | 小型模块化 Python 框架（beta，2026-06-21 更新） |
| 灵感 | smolagents（最小抽象）+ Pi（对话循环） |
| 特点 | 流式优先架构、工具结构化输出、事件驱动更新、provider 无关（任意 OpenAI 兼容端点） |
| 细节 | Anthropic 风格 prompt caching、可选 Rust `_alchemy` 绑定、全类型提示 |

> 💡 **关注点**：流式优先 + provider 无关是轻量框架的趋势方向——"任意 OpenAI 兼容端点"意味着 OpenRouter/本地/云全可接。

## 5. Micro-Agent：垂域服务化

| 维度 | 事实 |
|------|------|
| 定位 | 面向垂域应用的轻量框架（<3K 行），"最短路径交付领域 Agent API 服务" |
| 架构 | LLM 层（LiteLLM 统一：OpenAI/DeepSeek/Claude/Ollama）、ReAct 引擎（Think→Act→Observe）+ SubAgent 任务分发 + REPL 沙箱 |
| 内置 | 会话记忆、Skills（领域知识注入）、内置 RAG、MCP/工具、SSE 流式、多 LLM 配置 |
| 交付 | FastAPI 服务（`uvicorn api.app:app`）+ Docker |

> 🎯 **Micro-Agent 代表"Agent 即服务"模式**：垂域 Agent 不是库而是 API——与[Python 异步 + FastAPI](../../../01-Python语言/Python%20异步%20+%20FastAPI/)体系的交付形态一致。

## 6. GenericAgent：技能结晶自进化

| 维度 | 事实 |
|------|------|
| 定位 | 自进化本地电脑 Agent（~3K 行，MIT，单人维护） |
| 核心机制 | **技能结晶（skill crystallization）**：成功任务运行固化为可复用技能，技能树随使用增长 → 重复任务确定性回放 |
| 模型 | 任意 OpenAI 兼容端点（GPT-5、Ollama、vLLM、OpenRouter） |
| 适合 | 单机持久技能记忆场景 |

> ⚠️ **单人维护风险**：MIT + 单人维护 = 依赖 bus factor——生产采用前确认维护活跃度（09 篇评估法）。

## 7. 横向对比

| 维度 | PocketFlow | Flue | TinyAgent | Micro-Agent | GenericAgent |
|------|:---:|:---:|:---:|:---:|:---:|
| 体量 | ★★★★★ | ★★★★ | ★★★★ | ★★★ | ★★★ |
| 生产就绪 | ★★ | ★★★★ | ★★★ | ★★★★ | ★★★ |
| 独特机制 | 多语言 100 行 | 五原语+边缘部署 | 流式+事件 | 垂域 API 交付 | 技能结晶 |
| 生态 | 自建 | Astro 团队 | beta | 垂域 | 单人 |
| 最佳场景 | 学习/嵌入 | TS 生产 | 轻量循环 | 领域 API | 单机复用 |

## 8. 核心要点

> 🎯 **核心要点**：
> 1. 微框架光谱：教学（PocketFlow）→ 生产（Flue）→ 轻量循环（TinyAgent）→ 垂域 API（Micro-Agent）→ 自进化（GenericAgent）
> 2. 2026 方向：沙箱/技能包/成本控制成为原语（Flue 五原语）；流式+provider 无关（TinyAgent）；Agent 即 API（Micro-Agent）
> 3. 共同哲学：最小抽象 + 执行可靠性优先——与 LangChain 式重型框架的对抗是主旋律
> 4. 采用前用 09 篇评估法验证（尤其单人维护与 beta 项目）

---

**上一模块**：[02 轻量哲学标杆](02-轻量哲学标杆：smolagents%20与%20tiny-agents.md)　**下一模块**：[04 Deer-Flow 2.0](04-Deer-Flow%202.0：字节开源的模块化多%20Agent%20编排.md)　**返回总览**：[00 总览](00-总览：新兴%20SDK%20知识体系.md)

## 【参考来源】

- [PocketFlow: 100-line LLM framework. Let Agents build Agents! (GitHub)](https://github.com/the-pocket/PocketFlow)
- [Flue Review: Astro Team's TypeScript Agent Framework (2026)](https://dev.to/andrew-ooo/flue-review-astro-teams-typescript-agent-framework-2026-16og)
- [TinyAgent (tiny-agent-os) on PyPI (Socket)](https://socket.dev/pypi/package/tiny-agent-os)
- [Micro-Agent: lightweight framework for vertical domain apps (GitHub)](https://github.com/fdueblab/Micro-Agent)
- [GenericAgent vs SmoLAgents — Minimal Python Agent Frameworks in 2026](https://aicoolies.com/comparisons/genericagent-vs-smolagents)
- [agent-framework-radar: Live index of newest agent frameworks (GitHub)](https://github.com/linny006/agent-framework-radar)
