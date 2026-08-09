# 可观测全景：Agent 为什么需要观测

> 可观测 = 回答"Agent 到底做了什么、做得对不对、是变好还是变坏"。2026 核心认知：**Agent 非确定性**——同一提示可能走不同工具路线，模型更新/工具变更/流量演变导致行为静默漂移；传统日志/指标观测量不出"决策好不好"。本章立起全景：四支柱、session 单位、与相邻体系分工。

## 1. 为什么传统可观测不够

| 传统软件假设 | Agent 现实 |
|---|---|
| 确定性行为，错误可复现 | 非确定：同一提示不同工具路线 |
| 日志记"发生了什么" | 还要知道"决策好不好、值不值得" |
| 错误率/延迟衡量健康 | 单步失败可恢复、整体漂移不可见 |
| 请求是观测单位 | **会话是观测单位**——request 健康但会话漂移是常态 |

> 🎯 核心要点：Agent 可观测的独特对象是**"决策质量"**——不只是"步骤失败没"，而是"该调的工具调了吗、该跳过的跳过了吗、整体在进步还是退化"。

## 2. 四支柱：2026 可观测框架

| 支柱 | 职责 | 回答的问题 |
|---|---|---|
| Trace | 端到端执行树（提示/模型/工具/子 Agent） | 做了什么、顺序是什么 |
| Evaluate | 单轮与多轮质量/安全/任务完成打分 | 做得好不好 |
| Monitor | 实时问题检测：告警与仪表盘 | 现在出事了吗 |
| Optimize | 生产信号 → 证据支撑的改进 | 接下来改什么 |

> ⚠️ 四支柱是**连续能力**不是一次部署：Trace 是地基，Evaluate 是质量层，Monitor 是实时闸，Optimize 是闭环（09 篇）。只做 Trace 的团队="有录像没裁判"。

## 3. Session：观测的基本单位

| 维度 | Request 视角 | Session 视角 |
|---|---|---|
| 单位 | 单次调用 | 跨轮会话轨迹 |
| 成本 | 单调用 token | **会话级聚合成本**（意外账单多在此层） |
| 质量 | 单步对错 | 跨轮失败模式（第 1 轮识别问题、第 2 轮查到策略、第 3 轮没用上） |
| 告警 | 错误/延迟 | 会话漂移、轮次超限 |

> 🎯 核心要点：**request 健康、会话漂移是常见假象**——第 1 轮正确、第 2 轮检索对、第 3 轮应用失败，三个 request 各自健康，整个会话失败。只有会话轨迹暴露此模式（02 篇 threads 关联）。

## 4. 四类指标总览（04 篇展开）

| 类别 | 指标 | 作用 |
|---|---|---|
| 基础设施 | 延迟（P99）、错误率、token 用量 | 系统健康 |
| 成本 | 会话级 token/API 费用 | 预算控制 |
| 循环迭代 | 每会话轮次数、工具调用频率/失败率 | 提示回归/坏工具检测 |
| 质量 | 任务完成、幻觉率、接地性（eval） | 决策质量 |

> 💡 关键洞察：**搜索工具失败 30% 不显示在总体延迟里，却毁掉回答质量**——工具级失败率是循环类指标的重点（02 篇工具 span）。

## 5. 与相邻体系的分工

| 体系 | 分工 |
|---|---|
| 日志监控指标（02 层） | JVM/Java/OTel 基础设施层：日志框架、JVM 指标、Loki/EFK |
| 本体系（Agent 观测） | LLM/Agent 应用层：gen_ai 语义、Agent 追踪、成本、评估 |
| 安全护栏 Guardrails | 拦截（主动）；可观测是观察（被动）——拦截决策与结果都发遥测 |
| Feedback & Evaluation | 评估是质量层的一部分（本体系 06 篇覆盖评估视角） |
| 感知模块 | 感知管道成本/信噪比是观测对象（感知 02 篇） |

> 🎯 核心要点：三层分工——**基础设施可观测（02 层）+ 应用可观测（本体系）+ 拦截治理（Guardrails）**。LLM 应用层观测的前提是基础设施层通了（OTel 传输），两层是上下游。

## 6. 观测的 2026 原则

| 原则 | 内容 |
|---|---|
| 不信自报 | 遥测契约采集工具结果，不信 Agent 自述（06 篇） |
| 采样有策略 | tail-based：错误/低分/高成本 100%，干净 1-10%（08 篇） |
| 完整度先行 | span 完整度 <95% 仪表盘撒谎——先补属性（03 篇） |
| 会话关联 | conversation.id 传播到下游系统 span（02 篇） |
| 成本上 span | 每模型调用 span 带成本（05 篇） |
| 评估采样 | LLM-judge 10-20% 采样线上流量（06 篇） |
| 闭环 | 生产失败 → eval 样本/实验/治理事件，不是 Slack 线程（09 篇） |

## 7. 常见误区

| 误区 | 真相 |
|---|---|
| "日志=可观测" | 日志记"发生什么"，可观测还答"好不好"（Logging vs Observability 2026 专题） |
| "请求健康=系统健康" | 会话漂移是常见假象——session 是单位 |
| "错误率管一切" | 决策质量靠 eval，错误率看不见"选错工具但没报错" |
| "Trace 一次接完" | 四支柱连续——Trace 只是地基 |
| "观测是运维的事" | 决策质量观测是产品/算法的事——优化闭环是 2026 核心 |
| "基础设施观测够了" | 02 层管 JVM 健康，管不了 gen_ai 语义与 Agent 轨迹 |
| "告警只看延迟" | 质量告警（评估分阈值）是 Agent 特有告警（09 篇） |

## 8. 面试速记

| 问题 | 一句话答案 |
|---|---|
| 为什么传统可观测不够？ | Agent 非确定性——要观测"决策好不好"不是"步骤失败没" |
| 四支柱？ | Trace（做了什么）/Evaluate（好不好）/Monitor（出事没）/Optimize（改什么） |
| 为什么 session 是单位？ | request 健康会话漂移——跨轮失败只有轨迹能暴露 |
| 四类指标？ | 基础设施/成本/循环迭代/质量 |
| 工具失败率为什么不显示在延迟？ | 单工具 30% 失败被总体平滑——工具级指标要单独看 |
| 与 02 层分工？ | 02 管 JVM 基础设施，本体系管 LLM/Agent 应用层 |
| 与 Guardrails 分工？ | 护栏拦截（主动），可观测观察（被动）——拦截决策发遥测 |
| 2026 原则第一条？ | 不信 Agent 自报——遥测契约 |
| 观测对象独特点？ | 决策质量（该调的工具调了吗） |
| 闭环是什么？ | 生产失败→eval 样本/实验，不是 Slack 线程 |
| 完整度问题？ | <95% 仪表盘撒谎——补属性是最快收益 |
| 质量告警？ | 评估分阈值告警——Agent 特有告警类型 |

---

**下一模块**：[02-追踪：Trace 执行树](02-追踪：Trace执行树.md)　**返回总览**：[00-观测&可观测组件总览](00-观测可观测组件总览.md)

## 参考来源

- [Build 2026: From observability to ROI for AI agents（Microsoft Foundry）](https://devblogs.microsoft.com/foundry/build-2026-from-observability-to-roi-for-ai-agents-on-any-framework/)
- [AI Agent Observability: Tracing, Testing, and Improving Agents（LangChain）](https://www.langchain.com/resources/agent-observability)
- [Logging vs LLM Observability in 2026（FutureAGI）](https://futureagi.com/blog/logging-vs-llm-observability-2026/)
- [Inside Observe: The Six Surfaces of Production Agent Observability in 2026（FutureAGI）](https://futureagi.com/blog/observe-surfaces-tour/)
- [Monitoring Agentic AI in Production: 2026 Guide（MLflow）](https://mlflow.org/articles/monitoring-agentic-ai-in-production-2026-guide/)
