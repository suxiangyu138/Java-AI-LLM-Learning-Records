# 01 新兴框架全景：2026 轻量 Agent SDK 地图

> 定位：本体系第一站——2026 年新兴 Agent SDK 的整体趋势、分类地图、代表性项目与"值得关注 vs 噪音"的市场信号（2026-08 基准）

## 📚 目录

1. [核心趋势：从 prompt chains 到 execution runtimes](#1-核心趋势从-prompt-chains-到-execution-runtimes)
2. [分类地图](#2-分类地图)
3. [轻量框架阵营速览](#3-轻量框架阵营速览)
4. [前沿范式速览](#4-前沿范式速览)
5. [国产生态速览](#5-国产生态速览)
6. [市场信号：什么在火](#6-市场信号什么在火)
7. [值得关注 vs 噪音：判断框架](#7-值得关注-vs-噪音判断框架)
8. [核心要点](#8-核心要点)

## 1. 核心趋势：从 prompt chains 到 execution runtimes

2026 年新兴框架的最重要趋势——**重心从"提示词链"转向"执行运行时"**：

```text
2024-2025 的新框架         2026 的新框架
──────────────────────────────────────────────
围绕 Prompt 编排            围绕执行运行时（runtime）
无状态调用链                状态/检查点/可恢复执行（一等公民）
外部包装护栏                策略内建（intent guard/tool approval）
绑定单一云/单一模型          多环境一致（本地/容器/边缘/CI）
重型依赖                    零依赖/百行核心

本质：2026 年"框架"的竞争点 = 执行可靠性 + 治理 + 轻量，
     不再是"提示词怎么写"
```

> 🎯 **一句话**：新兴 SDK 在回答一个问题——"主流框架太重，Agent 运行时的最小可靠形态是什么？"

## 2. 分类地图

| 类别 | 代表 | 定位 |
|------|------|------|
| 微框架（Micro） | PocketFlow、tiny-agents、Micro-Agent | 百行核心、零依赖、专一用途 |
| 代码执行型 | smolagents（轻量标杆）、GenericAgent | Agent 写代码执行而非 JSON 工具调用 |
| 生产运行时 | Flue、lantern、vv-agent、reyn | 轻量但生产化（沙箱/部署/回放） |
| 模块化编排 | Deer-Flow 2.0 | 基于 LangGraph 的上层编排 |
| 自进化 | Prime Agent（RLM） | Agent 自我改进（提示词/技能/子 Agent CRUD） |
| CLI Agent 生态 | 百炼 CLI、Qoder、OpenClaw、Hermes | 终端 Agent + 平台能力 CLI 化 |
| Agent OS/运行时 | Sulala、Nexus、CUGA | "Agent 操作系统"架构叙事 |
| 协议/规范 | A2A v1.0、UCP、OSSA | 互操作标准（07 篇） |
| 语言侧 SDK | A2A Java SDK、Quarkus | 非 Python 生态接入 |

## 3. 轻量框架阵营速览

| 框架 | 语言 | 体量 | 一句话 |
|------|------|------|--------|
| smolagents | Python | <1K 行核心 | 代码执行型轻量标杆（深潜见[单 Agent 模块 04](../单%20Agent%20框架/04-Smolagents：代码执行型%20Agent.md)） |
| tiny-agents | JS/Python | ~50 行 | JSON 配置 + MCP 即 Agent，最快出 demo |
| PocketFlow | 7 语言 | 100 行 | 零依赖零锁定，"100 行 LLM 框架" |
| Flue | TypeScript | 轻量 | Astro 团队；五原语；Cloudflare Workers ~12ms 冷启动 |
| TinyAgent | Python | 小型 | 流式优先、provider 无关、事件驱动 |
| Micro-Agent | Python | <3K 行 | 垂域 Agent 最短路径（FastAPI 服务化） |
| GenericAgent | Python | ~3K 行 | 技能结晶：成功任务固化为可复用技能树 |

## 4. 前沿范式速览

| 范式 | 代表 | 说明 |
|------|------|------|
| 自进化 Agent（RLM） | Prime Agent | 模型在 REPL 中自我改进；运行时 CRUD 自己的提示词/技能/子 Agent |
| 策略内建 | CUGA | 治理（Intent Guard/Tool Approval）内建于运行时而非包装层 |
| 沙箱即一等公民 | Flue、Deer-Flow | 本地/Docker/K8s/边缘一致执行 |
| 技能结晶 | GenericAgent | 成功任务回放固化，确定性复用 |
| Agent 即服务 | Micro-Agent | 垂域 Agent 直接以 API 交付 |

> 💡 **前沿判断**：自进化（RLM）是最有争议也最有想象力的方向（Prime Agent 的 ARC-AGI-3 95.5% 宣称引发社区争议）——了解概念即可，生产采用需谨慎验证。

## 5. 国产生态速览

| 项目 | 归属 | 2026 事实 |
|------|------|----------|
| Deer-Flow 2.0 | 字节跳动 | 35.3k stars，GitHub Trending #1（24h）；LangGraph 之上的模块化多 Agent 编排 |
| 百炼 CLI | 阿里云 | 2026-05-29 开源（modelstudioai/cli）；150+ 模型 + 全套能力 CLI 化 |
| Qoder | 阿里 | 编码 Agent；百炼 CLI 原生支持对象之一 |
| OpenClaw | 开源 | 本地 CLI Agent；Coze 3.0/百炼 CLI 均可接入 |
| Hermes Agent | NousResearch | A2A v1.0 协议插件（2026） |
| Agency | 国产轻量 | 早期项目（单 star，无 License）——观望 |

> 🎯 **国产生态信号**：2026 年云厂商（阿里百炼）与字节（Deer-Flow、Coze）都在做"**平台能力开放化 + 本地 Agent 接入**"——CLI 化与开源是主路径。

## 6. 市场信号：什么在火

| 信号 | 含义 |
|------|------|
| GitHub Trending 被轻量/自进化框架霸榜（Deer-Flow、Prime Agent） | 社区对"重框架"疲劳，关注执行可靠性与前沿能力 |
| agent-framework-radar 类索引项目出现 | 新框架供给过剩，出现"框架雷达"需求 |
| 三协议栈（MCP/A2A/UCP）成为叙事 | 互操作从"选框架"变为"选协议" |
| A2A v1.0：150+ 组织、三云支持 | Agent 互操作进入生产期 |
| 轻量框架多语言移植（PocketFlow 7 语言） | "框架语言无关"成为卖点 |

## 7. 值得关注 vs 噪音：判断框架

```text
评估一个新框架，问 5 个问题：
① 解决了吗？——对比五大主流框架，它解决了什么真实问题
② 有维护吗？——star/commit/issue 响应（警惕刷星与停更）
③ 生产证据？——有无真实生产部署（不是 demo）
④ 锁定如何？——导出/迁移成本（流程能否带走）
⑤ 生态信号？——许可证、安全审计、社区活跃度

噪音特征：无 license、单维护者长期无更新、宣称数字无第三方验证、
         与主流框架能力重叠且无差异化
值得关注特征：明确问题定位、可验证基准、活跃社区、独特机制
```

> ⚠️ **雷达式心态**：新兴 SDK 模块的目的不是"推荐使用"，而是"**建立评估框架**"——具体项目用 09 篇的评估方法决定采用与否。

## 8. 核心要点

> 🎯 **核心要点**：
> 1. 2026 趋势：新兴框架从 prompt chains 转向 **execution runtimes**——执行可靠性 + 治理 + 轻量是竞争点
> 2. 分类九宫格：微框架/代码执行/生产运行时/模块化编排/自进化/CLI 生态/Agent OS/协议/语言侧
> 3. 国产动态：字节 Deer-Flow 2.0（35.3k stars）+ 阿里百炼 CLI——平台能力开源化 CLI 化
> 4. 新兴框架的价值 = 雷达与评估框架，不是"必须使用"——5 问评估法过滤噪音

---

**上一模块**：[00 总览](00-总览：新兴%20SDK%20知识体系.md)　**下一模块**：[02 轻量哲学标杆](02-轻量哲学标杆：smolagents%20与%20tiny-agents.md)　**返回总览**：[00 总览](00-总览：新兴%20SDK%20知识体系.md)

## 【参考来源】

- [agent-framework-radar: Live index of the newest agent frameworks (GitHub)](https://github.com/linny006/agent-framework-radar)
- [ByteDance Open-Sources Deer-Flow 2.0, Tops GitHub Trending (Pandaily)](https://pandaily.com/byte-dance-open-sources-deer-flow-2-0-tops-git-hub-trending)
- [开源Agent框架刷爆ARC-AGI-3（36氪）](https://www.36kr.com/p/3929369029868677)
- [阿里云开源百炼CLI（财联社）](https://www.cls.cn/detail/2385243)
- [SmolAgents: Definition & FutureAGI Guide (2026)](https://futureagi.com/glossary/smolagents/)
- [Flue Review: Astro Team's TypeScript Agent Framework (2026)](https://dev.to/andrew-ooo/flue-review-astro-teams-typescript-agent-framework-2026-16og)
- [PocketFlow: 100-line LLM framework (GitHub)](https://github.com/the-pocket/PocketFlow)
- [Build real agentic apps using CUGA (HuggingFace/IBM Research)](https://huggingface.co/blog/ibm-research/cuga-apps)
- [The Three-Protocol Agent Stack: MCP, A2A, and UCP (AgentMarketCap)](https://agentmarketcap.ai/blog/2026/04/12/three-protocol-agent-interoperability-stack-mcp-a2a-ucp-2026)
- [Agency: A Lightweight Chinese-Made Framework (AICHINA.news)](https://aichina.news/blog/agency-a-lightweight-chinese-made-framework-for-building-ai-agents-m2ofcu/)
