# 平台选型：Langfuse vs LangSmith vs Phoenix

> 平台 = 观测数据的消费端。2026 三巨头：**Langfuse（开源默认）/ LangSmith（LangChain 原生）/ Phoenix（OTel 原生）**——选型核心是"运行时耦合度 × 成本 × 自托管需求"；**"开源"≠"OSI 开源"是采购陷阱**（Phoenix ELv2）。本章给三平台深潜与决策框架。

## 1. 三平台定位

| 维度 | Langfuse | LangSmith | Arize Phoenix |
|---|---|---|---|
| 定位 | 开源框架无关默认 | LangChain 生态原生 | OTel/OpenInference 原生 |
| 许可 | **MIT 核心**（企业目录商业） | 闭源 | **ELv2（source-available，非 OSI）** |
| 核心优势 | 成本/自托管/框架无关 | 零配置深度追踪 | 单容器自托管 + 8 span kinds |
| 自托管 | 多服务栈（web+worker+PG+ClickHouse+Redis+S3） | Self-Hosted v0.13（2026-01-16，企业级） | 单容器 + OTel collector |
| 事件上限 | 按单位计费 | 按 base trace | **无事件上限** |

> 🎯 核心要点：**"开源"要问许可**——Phoenix 是 source-available（ELv2），Langfuse 核心 MIT 但企业目录分离——采购合规场景这两个都算"可审计代码"但不都算 OSI 开源。

## 2. 定价对比（2026，规模差距巨大）

| 平台 | 免费层 | 首个付费层 | 1M events/月 估算 |
|---|---|---|---|
| Langfuse | 50k 单位/月 | Core $29/月（100k 单位，无限用户） | **~$101/月** |
| LangSmith | 5k base traces + 1 席位 | Plus $39/席位（100k traces） | **~$2,514/月**（按席位扩展差） |
| Phoenix | 自托管免费 | Arize AX Pro $50/月 | 自托管无事件费 |

> ⚠️ 注意计量口径不可比：Langfuse 计"单位"（traces/observations/scores/evals），LangSmith 计"base traces"——不是 1:1。但趋势明确：**规模上 Langfuse/自托管 Phoenix 便宜一个数量级**（LangSmith 按席位定价对大团队不友好）。

## 3. 能力对比

| 能力 | Langfuse | LangSmith | Phoenix |
|---|---|---|---|
| Trace 浏览器 | 强 | 强（大图复杂） | 强 |
| 提示管理 | 深度 | 有（Prompt Hub） | CLI 命令（2026-01） |
| 评估 | 弱（heuristic+judge，无自研 judge 族） | 强（在线 eval + LLM judge） | 强（OpenInference + eval 工具） |
| 运行时护栏 | 无 | 无 | 无（三者都要另接） |
| 数据集/实验 | 有（Experiments CI/CD 2026-05） | 深度（数据集+实验） | 有（notebook 友好） |
| MCP 工具面 | **最广（52 工具）** | 覆盖 runs/threads/prompts | get-spans 原始 span 访问 |
| 生态耦合 | 框架无关 | LangChain 深度 | OTel 生态 |

> 💡 MCP 时代的平台差异：**Langfuse 52 个 MCP 工具 vs Phoenix get-spans 原始访问 vs LangSmith fetch_runs（FQL 树过滤，100 trace 重建需 109 次调用）**——Agent 工具化消费观测数据的深度不同（2026 对比结论）。

## 4. 2026 平台更新

| 平台 | 更新 | 日期 |
|---|---|---|
| Langfuse | **Experiments CI/CD**（GitHub Actions 跑实验检查） | 2026-05/06 |
| Langfuse | 被 ClickHouse 收购 | 2026-01 |
| LangSmith | **Agent Builder 升级为 Fleet**（无代码 Agent 构建） | 2026-03-19 |
| LangSmith | Self-Hosted v0.13（企业级自托管） | 2026-01-16 |
| Phoenix | CLI prompt 命令（终端工作流） | 2026-01-22 |

## 5. 决策框架

```text
选型决策树：
├─ 运行时是 LangChain/LangGraph？
│   └─ 是 → LangSmith（原生深度集成、决策分支追踪）
├─ 要求 OSI 开源/自托管/成本可控？
│   └─ 是 → Langfuse（MIT 核心、框架无关）或 Phoenix 自托管（看 span 分类需求）
├─ OTel/OpenInference 纪律优先？
│   └─ 是 → Phoenix（单容器、8 span kinds、无事件上限）
├─ 企业自托管 + LangChain 生态？
│   └─ LangSmith Self-Hosted v0.13（VPC/托管）
└─ 组合模式：trace 层（Langfuse）+ eval 层（LangSmith/Braintrust）双工具
```

> 🎯 核心要点：**双工具是 2026 主流**——"一个 LLM trace 层（prompts/完成/成本）+ 一个评估层"各解决不同问题；别指望单平台全能（Langfuse 评估弱、LangSmith 锁定、Phoenix 无网关护栏）。

## 6. 常见选型陷阱

| 陷阱 | 真相 |
|---|---|
| "OSS=OSI 开源" | Phoenix ELv2 source-available、Langfuse 企业目录商业——采购合规要问许可 |
| "1:1 比价" | 计量口径不同（单位 vs base traces）——按真实用量换算 |
| "框架无关=什么都能接" | LangSmith 在 LangChain 外价值骤降——耦合度是隐形成本 |
| "单平台全能" | 评估弱（Langfuse）/锁定（LangSmith）/无护栏（全部）——双工具组合 |
| "自托管=免费" | Langfuse 多服务栈运维成本、LangSmith 自托管是企业级配置 |
| "MCP 工具数=能力" | 52 vs get-spans——工具形态不同，按消费方式选 |
| "1M events 是理论值" | 生产 Agent 每周百万级 trace 常见——按真实规模定价 |

## 7. 面试速记

| 问题 | 一句话答案 |
|---|---|
| 三平台定位？ | Langfuse 开源默认/LangSmith LangChain 原生/Phoenix OTel 原生 |
| 许可陷阱？ | Phoenix ELv2 source-available 非 OSI；Langfuse 核心 MIT 企业目录商业 |
| 1M events 成本？ | Langfuse ~$101 vs LangSmith ~$2,514——规模差一个数量级 |
| Langfuse 弱项？ | 评估（无自研 judge 族）、无运行时护栏 |
| LangSmith 弱项？ | 锁定 LangChain、按席位定价、闭源 |
| Phoenix 弱项？ | ELv2、OSS 版限功能、无基础设施监控 |
| 2026 更新？ | Langfuse Experiments CI/CD + ClickHouse 收购；LangSmith Fleet + Self-Hosted v0.13；Phoenix CLI |
| 选型第一问？ | 运行时耦合度（LangChain?） |
| 双工具模式？ | trace 层（Langfuse）+ eval 层（LangSmith/Braintrust） |
| MCP 工具面差异？ | Langfuse 52 工具 vs Phoenix get-spans vs LangSmith fetch_runs |
| 自托管足迹？ | Phoenix 单容器 vs Langfuse 多服务栈 |
| 计量口径？ | 单位 vs base traces 不可 1:1 比价 |

---

**下一模块**：[08-采样、脱敏与数据治理](08-采样脱敏与数据治理.md)　**返回总览**：[00-观测&可观测组件总览](00-观测可观测组件总览.md)

## 参考来源

- [Langfuse Alternatives in 2026（FutureAGI）](https://futureagi.com/blog/langfuse-alternatives-2026/)
- [LangSmith Alternatives (2026)（MorphLLM）](https://www.morphllm.com/comparisons/langsmith-alternatives)
- [Phoenix Alternatives in 2026（FutureAGI）](https://futureagi.com/blog/phoenix-alternatives-2026/)
- [Observability MCP comparison（Pydantic）](https://pydantic.dev/articles/observability-tools-agents-want)
- [LLM Observability Platform Buyer's Guide 2026（FutureAGI）](https://futureagi.com/blog/llm-observability-platform-buyers-guide-2026/)
- [Best AI Monitoring Tools in 2026（DevHelm）](https://devhelm.io/blog/best-ai-monitoring-tools)
