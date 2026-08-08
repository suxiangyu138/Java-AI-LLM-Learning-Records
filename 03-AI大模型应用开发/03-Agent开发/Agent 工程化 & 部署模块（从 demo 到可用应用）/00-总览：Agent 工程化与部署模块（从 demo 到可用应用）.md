# 00 总览：Agent 工程化与部署模块（从 demo 到可用应用）

> 定位：把 Agent 从"能跑的 demo"变成"可上线、可演进、可治理的生产应用"的完整工程化旅程——架构、配置、可观测、测试、评估、安全、部署、版本、治理（2026-08 基准）

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
阶段 4（问题调优与工程化）        = 方法论：评估体系、部署流程、治理框架
调优/排错/安全模块（生产必备）     = 现场手册：调什么参数、怎么查故障、加固哪些项
本模块（工程化 & 部署）           = 施工蓝图：demo → 生产要补哪些工程、按什么顺序建
失败模式体系（11 篇）             = 原理：四大失败为什么发生

一句话分工：本模块回答"怎么把 Agent 工程化并送上线"，
调优模块回答"上线后现场怎么干"，阶段 4 回答"体系怎么搭"
```

> 🎯 **核心价值**：这是五阶段学习路径中"从会写 Agent 到能交付 Agent"的桥梁模块。读完本模块，你应该能独立回答：demo 与生产差在哪（01）、工程代码怎么分层（02）、密钥配置怎么管（03）、上线后怎么看得见（04）、怎么证明它没坏（05/06）、怎么跑得快又便宜（07）、怎么防攻击（08）、怎么部署与扩容（09）、怎么安全地迭代（10）、怎么长期治理（11），并用一个完整案例串起全程（12）。

## 2. 知识体系导图

```text
Agent 工程化 & 部署模块（从 demo 到可用应用）
│
├── 00 总览（本文件）
│
├── 差距篇
│   └── 01 差距分析 ── Demo 与生产的八大分水岭 / 转化清单
│
├── 工程篇（代码怎么建）
│   ├── 02 工程化架构 ── 分层 / 核心抽象 / LLM+确定性代码边界
│   └── 03 配置与密钥管理 ── 环境隔离 / 密钥生命周期 / 动态配置
│
├── 质量篇（怎么证明没坏）
│   ├── 04 可观测性 ── 日志 / Trace / Metrics（OTel GenAI 语义约定）
│   ├── 05 测试体系 ── Mock LLM / 契约测试 / 端到端
│   └── 06 评估体系 ── Eval 门禁 / 统计回归 / LLM-as-Judge
│
├── 性能与安全篇（怎么快、怎么稳、怎么安全）
│   ├── 07 性能与成本 ── Prompt 缓存 / 语义缓存 / 并发流式 / 模型路由
│   ├── 08 安全加固 ── OWASP LLM Top 10 2025 / 最小权限 / 数据治理
│   └── 09 部署架构 ── 同步 / 流式 / 异步队列 / 模型网关 / K8s
│
└── 演进篇（怎么长期迭代）
    ├── 10 版本管理与发布 ── Prompt 注册表 / 金丝雀 / 回滚
    ├── 11 生产治理 ── 配额 / SLA / 成本告警 / 监控闭环
    ├── 12 实战案例 ── 客服 Agent 从 Demo 到上线的完整旅程
    └── 13 面试与自测 ── 面试题 / 自测 / 毕业检查单
```

## 3. 模块导航

| 序号 | 模块 | 核心内容 | 场景 |
|:---:|------|---------|------|
| 01 | 差距分析 | 八大分水岭、demo 地狱、转化清单 | 先读（建立全局） |
| 02 | 工程化架构 | 分层、核心抽象、LLM+确定性代码 | 开始重构代码时 |
| 03 | 配置与密钥管理 | 环境隔离、密钥生命周期、动态配置 | 搭项目骨架时 |
| 04 | 可观测性 | OTel GenAI 语义约定、Langfuse、三支柱 | 上线前必装 |
| 05 | 测试体系 | Mock LLM、契约、E2E、故障注入 | 写代码期间 |
| 06 | 评估体系 | Eval 门禁、统计回归、LLM-as-Judge | 每次改 prompt/模型 |
| 07 | 性能与成本 | 缓存经济学、并发流式、模型路由 | 延迟高/账单贵时 |
| 08 | 安全加固 | OWASP 2025、最小权限、审批门 | 上线前必做 |
| 09 | 部署架构 | 同步/流式/异步队列、网关、K8s | 送上线时 |
| 10 | 版本管理与发布 | Prompt 注册表、金丝雀、回滚 | 首次灰度时 |
| 11 | 生产治理 | 配额、SLA、成本告警、反馈闭环 | 上线稳定后 |
| 12 | 实战案例 | 客服 Agent 完整旅程、检查单 | 全流程参考 |
| 13 | 面试与自测 | 面试题、20 自测、毕业检查单 | 求职/自检 |

## 4. 学习路线推荐

**路线 A：快速送上线（2 天）**——01 → 09 → 04 → 08 → 10
> 先把部署形态、可观测、安全、发布定下来，其余边跑边补。

**路线 B：完整工程化（1 周）**——01 → 02 → 03 → 05 → 06 → 07 → 09 → 10 → 11 → 12
> 全链路施工：代码 → 质量 → 性能 → 部署 → 演进，最后用案例核对。

**路线 C：面试冲刺（2 天）**——13 → 01 → 06 → 09 → 10
> 以题为纲反查知识点，重点掌握"为什么要"级别的论述。

## 5. 核心概念速查

| 概念 | 一句话 | 关键数字 |
|------|--------|---------|
| Demo 与生产的分水岭 | demo 证明"可行"，生产证明"可靠、可算、可管" | 八大差距（01 篇） |
| LLM + 确定性代码 | LLM 管感知/推理/意图，代码管事务/计算/正确性 | 混合架构 2026 共识 |
| OTel GenAI 语义约定 | 跨厂商 LLM trace 字段标准（`gen_ai.*`） | 2025 稳定，1.28+ |
| trace-first 排错 | 先看 trace 再猜原因 | prompt hash 秒判变更 |
| 三层评估门禁 | 确定性检查 → NLI 分类器 → LLM-as-Judge 级联 | 前两层 $0~$0.001/次 |
| 统计回归门禁 | 滚动基线 + Welch t-test 判显著漂移 | p<0.05、\|delta\|≥0.03、100-200 例/路由 |
| LLM-as-Judge 方差 | judge 判决逐次波动，CI 中天然"flaky" | 5-50¢/次、100ms-3s |
| Prompt Caching | 稳定前缀命中缓存 | 输入成本省 ~85-90%、TTFT 降 50-70% |
| 语义缓存 | 向量相似度命中"意思相近"的请求 | 命中 60-85%、延迟 1.67s→0.052s |
| 缓存感知路由 | 按 KV 缓存前缀哈希路由（不盲轮询） | 轮询下命中率可跌至 8%、TTFT p90 差 57× |
| 过度自主权（LLM06） | 权限太多 + 一次注入 = 真实破坏 | OWASP 2025 最危险面 |
| 系统提示泄漏（LLM07） | 假设 prompt 必泄漏：密钥绝不进 prompt | 用 canary token 监测 |
| 异步 Agent 架构 | API 先回 job_id，队列 + worker 执行，SSE 回放进度 | "盯着屏幕就流式，回头再来就队列" |
| Prompt 版本化 | 版本注册表 + label 部署，回滚=指针移动 | 金丝雀默认 5% 流量 |
| 回滚缓存陷阱 | 回滚后旧版指纹的缓存会"幽灵服务" | 回滚必须失效缓存 |
| 无界消耗（LLM10） | 无预算上限 = 成本失控 | 每 key 硬预算、循环上限 |

## 6. 与关联体系的分工

| 体系 | 分工 | 本模块怎么用 |
|------|------|------------|
| [阶段 4：问题调优与工程化](../阶段%204：问题调优与工程化/00-阶段总览：问题调优与工程化.md)（10 篇） | 评估/部署/治理方法论 | 本模块是其"工程落地方案"：方法论 → 具体做法 |
| [Agent 调优、排错与安全模块](../Agent%20调优、排错与安全模块（生产必备）/00-总览：Agent%20调优、排错与安全模块（生产必备）.md)（11 篇） | 生产现场操作手册 | 上线后调优/排错/加固查它，本模块负责"上线前" |
| [失败模式体系](../幻觉、格式错误、安全、token%20超限/00-Agent四大失败模式知识体系总览.md)（11 篇） | 失败原理与治理方案 | 安全/质量设计时引用其原理 |
| [LiteLLM 多模型适配](../../02-大模型基础与Prompt工程/LiteLLM%20多模型适配/00-LiteLLM知识体系总览.md)（11 篇） | 模型网关/路由/成本专项 | 09/11 篇引用其 Proxy 配置细节 |
| [部署工具](../../07-模型部署与工程化/部署工具/00-部署工具知识体系总览.md)（12 篇） | Docker/K8s/vLLM/MLOps 工具 | 09 篇假设工具知识已具备 |
| [Agent 开发框架层](../Agent%20开发框架层（工程加速）/) | LangGraph 等框架实现 | 02 篇的分层可映射到各框架 |
| [阶段 5：进阶](../阶段%205：进阶/00-阶段总览：进阶.md)（10 篇） | 多 Agent/MCP 架构 | 多 Agent 的工程化以本模块为基础 |

## 7. 2026 版本窗口

> 本模块以 2026-08 为基准：
>
> - **工程化共识**：LLM + 确定性代码混合架构成为 2026 生产标配；golden set（50-100 题）作为发布门禁；"eval 套件即规范、分数即发布判决"的 eval-driven development 成为主流工作流
> - **可观测性**：OpenTelemetry GenAI 语义约定 2025 年稳定（1.28+，Datadog 已支持至 1.37）；Langfuse 2026-01 被 ClickHouse 收购但仍保持 MIT 开源；OpenInference + OTLP + 仓库汇是 2026 中位生产形态
> - **评估市场**：eval/可观测市场 2025 年 $1.97B → 2026 年 $2.69B（36.3% CAGR）；Braintrust 获 $80M Series B；Humanloop 被 Anthropic 收购团队后关停——单供应商押注的警示
> - **回归门禁**：统计回归门禁取代固定阈值（滚动基线 + Welch t-test + 效果量）；PR 时运行 eval 并阻断降级成为标配（DeepEval/Promptfoo/Braintrust Action）
> - **缓存经济学**：Prompt Caching 输入成本省 ~85-90%；缓存感知路由成为 2026 延迟优化第一大杠杆（盲轮询命中率可跌至 8%、TTFT p90 差 57×）；语义缓存命中 60-85%
> - **安全**：OWASP LLM Top 10 2025（2025-11 发布）聚焦 agentic 时代——过度自主权（LLM06）升级、系统提示泄漏/向量弱点/无界消耗三个新条目；EU AI Act 第 14 条 2026-08-02 生效；MCP STDIO RCE 事件（≥10 CVE）后工具供应链安全基线升级
> - **异步部署**：job 队列 + SSE 事件回放（`Last-Event-ID`/游标）成为长任务标准形态；Execution Registry 模式解耦"执行生命周期"与"连接生命周期"
> - **版本管理**：prompt 从代码迁出进版本注册表（label 部署、回滚=指针移动）成为标准；金丝雀默认 5% 流量；回滚必须失效缓存（防止幽灵服务旧版本）

## 【参考来源】

- [MLflow: Building Production-Ready AI Agents in 2026](https://mlflow.org/articles/building-production-ready-ai-agents-in-2026/)
- [Mindflow: The Production AI Agent Reality Check: 9 Engineering Practices That Actually Work](https://mindflow.io/blog/the-production-ai-agent-reality-check-9-engineering-practices-that-actually-work)
- [Google Developers Blog: Build Better AI Agents: 5 Developer Tips from the Agent Bake-Off](https://developers.googleblog.com/build-better-ai-agents-5-developer-tips-from-the-agent-bake-off/)
- [Langfuse: LLM Observability via OpenTelemetry](https://python-sdk-v3.docs-snapshot.langfuse.com/integrations/native/opentelemetry/) / [Langfuse vs. Datadog（2026-07）](https://langfuse.com/resources/engineering/langfuse-vs-datadog)
- [OTel 官方：GenAI Semantic Conventions（2025 稳定，1.28+）](https://opentelemetry.io/docs/specs/semconv/gen-ai/)
- futureagi.com: [Agent Evaluation Frameworks in 2026](https://futureagi.com/blog/agent-evaluation-frameworks-2026/) / [LLM Observability 2026](https://futureagi.com/blog/what-is-llm-observability-2026/) / [CI-CD for AI Agents 2026](https://futureagi.com/blog/ci-cd-for-ai-agents-best-practices-2026/) / [Prompt Versioning and Lifecycle Management in 2026](https://futureagi.com/blog/prompt-versioning-lifecycle-management-2026/)
- [respan.ai: Best LLM Evaluation Tools in 2026](https://www.respan.ai/articles/best-llm-evaluation-tools)
- [Atlas research（2026-06）: Evals in CI/CD](https://github.com/Laoujin/Atlas/blob/main/research/2026-06-04-evals-how-do-you-know-your-ai-works-session-blueprint/evals-in-ci-cd/index.md) / [Eval Framework Landscape 2026](https://github.com/Laoujin/Atlas/blob/main/research/2026-06-09-evals-vibes-don-t-scale-a-complete-technical-session-blueprint/eval-framework-landscape-2026/index.md)（统计回归门禁、三层级联）
- [viqus.ai: Async Agent Architectures: Queue-Based vs Streaming](https://viqus.ai/blog/async-agent-architectures-queue-vs-streaming)
- [futureagi.com: OWASP LLM Top 10 2025 Risks & Mitigations](https://futureagi.com/blog/owasp-llm-top-10-2025-risks-mitigations-2026/)
- [futureagi.com: Prompt Versioning and Lifecycle Management in 2026](https://futureagi.com/blog/prompt-versioning-lifecycle-management-2026/) / [LLM Deployment Best Practices 2026](https://futureagi.com/blog/llm-deployment-best-practices-2026/)

---

**下一模块**：[01-差距分析：Demo 与生产的分水岭](01-差距分析：Demo%20与生产的分水岭.md)
