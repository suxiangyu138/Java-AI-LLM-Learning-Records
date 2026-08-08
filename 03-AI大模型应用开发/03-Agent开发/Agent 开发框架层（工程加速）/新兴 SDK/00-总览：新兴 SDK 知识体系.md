# 00 总览：新兴 SDK 知识体系

> 定位：框架层（工程加速）第四站——五大主流框架之外的新兴轻量 SDK、前沿范式与协议新进展：微框架光谱、Deer-Flow、自进化 Agent、CLI Agent 生态、A2A v1.0（2026-08 基准）

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
Agent 内核理论模块      = 原理：Agent 是什么（必修）
单 Agent 框架模块       = 五大主流框架（LangGraph/OpenAI SDK/Smolagents 等）
低代码 Agent 平台模块   = 拖拽式平台（Dify/Coze 等）
多 Agent 框架模块       = 多 Agent 编排（Supervisor/Swarm 等）
本模块（新兴 SDK）      = 主流之外：轻量微框架 / 前沿范式 / 协议新进展
工程化 & 部署模块       = 生产化（落地）

一句话分工：本模块回答"2026 年还有什么新东西值得关注——
            轻量框架怎么选、前沿范式（自进化/Agent OS）是什么、
            协议（A2A/UCP）进展到哪了"
```

> 🎯 **核心价值**：2026 年新兴 Agent 生态从"prompt chains"转向"execution runtimes"——轻量哲学（sub-MB 运行时、百行核心）对抗重型框架；自进化 Agent、Agent OS、A2A v1.0 等前沿快速演进。本模块不是"主流框架的重复"，而是**雷达**：识别值得关注的新框架、判断哪些只是噪音、看懂协议层的新标准。

## 2. 知识体系导图

```text
新兴 SDK 知识体系
│
├── 00 总览（本文件）
│
├── 全景篇
│   ├── 01 新兴框架全景 ── 2026 轻量 SDK 地图 / 趋势 / 市场信号
│   └── 02 轻量哲学标杆 ── smolagents vs tiny-agents（代码执行 vs 配置驱动）
│
├── 微框架与前沿篇
│   ├── 03 微框架光谱 ── PocketFlow / Flue / TinyAgent / Micro-Agent / GenericAgent
│   ├── 04 Deer-Flow 2.0 ── 字节开源，LangGraph 之上的模块化编排
│   ├── 05 自进化前沿 ── Prime Agent / RLM harness / Continual Harness
│   └── 06 CLI Agent 生态 ── 百炼 CLI / Qoder / OpenClaw / Hermes
│
├── 协议与运行时篇
│   ├── 07 协议新进展 ── A2A v1.0 / UCP / OSSA / 三协议栈
│   └── 08 Agent OS 与运行时 ── Sulala / Nexus / CUGA / 策略内建
│
├── 决策篇
│   └── 09 新兴框架评估与选型 ── 评估雷达 / 何时用 vs 五大主流
│
└── 检验篇
    └── 10 面试与自测 ── 面试题 / 自测 / 毕业检查单
```

## 3. 模块导航

| 序号 | 模块 | 核心内容 | 场景 |
|:---:|------|---------|------|
| 01 | 新兴框架全景 | 轻量 SDK 地图、2026 趋势、市场信号 | 先读（建立全局） |
| 02 | 轻量哲学标杆 | smolagents vs tiny-agents 两派 | 想用轻量框架时 |
| 03 | 微框架光谱 | PocketFlow/Flue/TinyAgent/Micro-Agent/GenericAgent | 评估微框架时 |
| 04 | Deer-Flow 2.0 | 字节开源、LangGraph 之上 | 关注国产/模块化编排时 |
| 05 | 自进化前沿 | Prime Agent、RLM、Continual Harness | 了解前沿范式时 |
| 06 | CLI Agent 生态 | 百炼 CLI/Qoder/OpenClaw/Hermes | 用 CLI Agent 时 |
| 07 | 协议新进展 | A2A v1.0/UCP/OSSA/三协议栈 | 跨厂商互操作时 |
| 08 | Agent OS 与运行时 | Sulala/Nexus/CUGA、策略内建 | 理解"Agent OS"叙事时 |
| 09 | 评估与选型 | 评估雷达、何时用 vs 主流 | 决策时 |
| 10 | 面试与自测 | 面试题、自测、毕业检查单 | 求职/自检 |

## 4. 学习路线推荐

**路线 A：快速浏览（1 天）**——01 → 02 → 09
> 建立"新兴生态有哪些东西"的全局观 + 评估方法，不深潜。

**路线 B：全面掌握（3-4 天）**——01 → 02 → 03 → 04 → 06 → 07 → 09
> 覆盖轻量框架、国产动态、CLI 生态与协议进展；05/08 按兴趣选读。

**路线 C：面试冲刺（1-2 天）**——10 → 01 → 07 → 09
> 以题为纲：先会答"新兴框架怎么评估/协议进展"类问题。

## 5. 核心概念速查

| 概念 | 一句话 | 关键数字 |
|------|--------|---------|
| 新兴 SDK | 五大主流之外的轻量框架/前沿范式/协议新进展 | 本模块主题 |
| 轻量哲学 | 百行核心、sub-MB 运行时，对抗 LangChain 式重型框架 | smolagents <1K 行；Sulala ~100-150KB |
| tiny-agents | HF 配置驱动：JSON 定义 Agent + MCP，~50 行 JS | `npx @huggingface/tiny-agents` |
| PocketFlow | "100 行 LLM 框架"，零依赖，多语言移植 | TS/Java/C++/Go/Rust/PHP |
| Deer-Flow 2.0 | 字节开源模块化多 Agent 编排（基于 LangGraph） | 35.3k stars；GitHub Trending #1 |
| Prime Agent | 开源"自我改进"RLM harness | ~5k stars；宣称 ARC-AGI-3 95.5% |
| Continual Harness | Agent 运行时 CRUD 自己的提示词/技能/子 Agent | Prime Agent 特征 |
| 百炼 CLI | 阿里云模型能力 CLI 化，原生兼容 Claude Code 等 | modelstudioai/cli（2026-05-29 开源） |
| A2A v1.0 | Agent 间协作协议稳定版（Linux Foundation） | 150+ 组织；三云支持；Agent Card 签名 |
| UCP | 统一计费协议（Agent 商务层） | 2026 三协议栈之一 |
| OSSA | Agent 清单开放规范（工具/A2A/拓扑/可观测声明） | v0.4 |
| Agent OS | Agent 运行时即"操作系统"的架构叙事 | Sulala/Nexus 等探索中 |

## 6. 与关联体系的分工

| 体系 | 分工 | 本模块怎么用 |
|------|------|------------|
| [单 Agent 框架](../单%20Agent%20框架/00-总览：单%20Agent%20框架知识体系.md) | 五大主流框架深潜 | 本模块只覆盖主流之外；smolagents 深潜在那边的 04 篇 |
| [多 Agent（Multi-Agent）框架](../多%20Agent（Multi‑Agent）框架/00-总览：多%20Agent（Multi‑Agent）框架知识体系.md) | 多 Agent 编排框架 | 07 篇协议进展与那边的 07 篇互补（框架视角 vs 协议新进展） |
| [低代码 Agent 平台](../低代码%20Agent%20平台（原型快速验证）/00-总览：低代码%20Agent%20平台知识体系.md) | 平台级验证 | 平台的 MCP/CLI 化对应 06 篇 |
| [Agent与MCP协议](../../Agent与MCP协议/) | MCP 协议原理 | 07 篇在此基础上讲 A2A/UCP 新进展 |
| [工程化 & 部署模块](../../Agent%20工程化%20&%20部署模块（从%20demo%20到可用应用）/00-总览：Agent%20工程化与部署模块（从%20demo%20到可用应用）.md) | 生产化 | 新兴框架能否生产由该模块标准检验（09 篇） |
| [Python 异步 + FastAPI](../../../01-Python语言/Python%20异步%20+%20FastAPI/) | Python 生态 | 微框架多基于 Python/FastAPI 服务化 |

## 7. 2026 版本窗口

> 本模块以 2026-08 为基准，以下为检索到的版本事实（详见各篇【参考来源】）：
>
> - **轻量框架**：smolagents（<1K 行核心，HF 生态）；@huggingface/tiny-agents（~50 行 JS，JSON config + MCP）；PocketFlow（100 行核心，7 语言）；Flue（Astro 团队，五原语，Cloudflare Workers ~12ms 冷启动）；TinyAgent（tiny-agent-os beta，2026-06-21）；Micro-Agent（<3K 行垂域框架）；GenericAgent（技能结晶自进化）
> - **Deer-Flow 2.0**（字节跳动开源）：35.3k stars，24 小时登顶 GitHub Trending；LangGraph 之上的模块化编排（11 层中间件链/动态子 Agent/沙箱执行）
> - **Prime Agent**（Prime Intellect）：开源 RLM harness，宣称 ARC-AGI-3 95.5%；持久 IPython REPL + Continual Harness
> - **CLI 生态**：阿里云百炼 CLI（modelstudioai/cli，2026-05-29 开源，原生支持 Claude Code/Qoder/OpenClaw/Hermes）；Qoder（阿里）；OpenClaw；Hermes Agent（NousResearch，A2A v1.0 插件 2026）
> - **协议**：A2A v1.0 稳定规范（Linux Foundation；150+ 组织；三云原生支持；Agent Card 加密签名；gRPC 传输）；A2A Java SDK 1.0.0.CR1（Quarkus，2026-05，v0.3 兼容层）；UCP（统一计费）；OSSA v0.4
> - **Agent OS**：Sulala Agent OS（Bun，~100-150KB 核心）、Nexus（三协议面）、CUGA（IBM，策略内建）
> - 未确认项：Deer-Flow/Prime Agent 的基准数字为项目方/媒体报道，标注方向性；新兴项目变动快，用前查官方仓库最新状态

---

**下一模块**：[01 新兴框架全景](01-新兴框架全景：2026%20轻量%20Agent%20SDK%20地图.md)　**返回上级**：[Agent 开发框架层（工程加速）](../)

## 【参考来源】

- [agent-framework-radar: Live index of newest agent frameworks (GitHub)](https://github.com/linny006/agent-framework-radar)
- [ByteDance Open-Sources Deer-Flow 2.0, Tops GitHub Trending (Pandaily)](https://pandaily.com/byte-dance-open-sources-deer-flow-2-0-tops-git-hub-trending)
- [开源Agent框架刷爆ARC-AGI-3，「自我改进」的RLM harness引争议（36氪）](https://www.36kr.com/p/3929369029868677)
- [阿里云开源百炼CLI（财联社）](https://www.cls.cn/detail/2385243)
- [The Three-Protocol Agent Stack: MCP, A2A, and UCP (AgentMarketCap)](https://agentmarketcap.ai/blog/2026/04/12/three-protocol-agent-interoperability-stack-mcp-a2a-ucp-2026)
- [A2A Java SDK 1.0.0.CR1 Released (Quarkus)](https://es.quarkus.io/blog/a2a-java-sdk-1-0-0-cr1-released/)
- [SmolAgents: Definition & FutureAGI Guide (2026)](https://futureagi.com/glossary/smolagents/)
- [Build real agentic apps using CUGA (HuggingFace/IBM Research)](https://huggingface.co/blog/ibm-research/cuga-apps)
- [Flue Review: Astro Team's TypeScript Agent Framework (2026)](https://dev.to/andrew-ooo/flue-review-astro-teams-typescript-agent-framework-2026-16og)
- [PocketFlow: 100-line LLM framework (GitHub)](https://github.com/the-pocket/PocketFlow)
