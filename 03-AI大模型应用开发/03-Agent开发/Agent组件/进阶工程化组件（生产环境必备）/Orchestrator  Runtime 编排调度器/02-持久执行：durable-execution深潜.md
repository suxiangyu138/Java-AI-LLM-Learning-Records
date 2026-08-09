# 持久执行：durable execution 深潜

> 持久执行 = 运行时保证 Agent 工作流在崩溃/重启/网络故障后**从精确断点恢复**。2026 机制定型：**事件日志 + 快照双机制**（日志可重放、快照加速恢复）；**重放语义**（重放事件日志重建状态，不信任快照）；**checkpoint 六义务**（remit-contract 形式化）。本章给持久执行完整深潜。

## 1. 机制：事件日志 + 快照

| 机制 | 作用 | 特点 |
|---|---|---|
| 事件日志 | 记录每个已发生的步骤（LLM 调用/工具结果） | 可重放、追加式、审计友好 |
| 快照 | 定期保存完整状态 | 加速恢复（不用全量重放） |
| 组合 | 快照 + 增量日志 | 恢复 = 加载快照 + 重放增量 |

> 🎯 核心要点：**日志与快照缺一不可**——只有日志恢复慢（全量重放），只有快照丢增量（崩溃前未快照部分）；2026 标准是"混合恢复"（快照 + 事件重放）。

## 2. 重放语义：恢复的正确姿势

| 原则 | 说明 |
|---|---|
| 不信任快照 | 重启应**重放事件日志**重建状态——快照可能陈旧/损坏 |
| 跳过已完成 | 重放时已完成 LLM/工具调用**不再重新执行**（防重复计费+副作用） |
| 前缀延续 | 从最后完成的 activity 之后继续（remit-contract 义务一） |
| 效果恰好一次 | 重放不重复副作用（义务二） |

> ⚠️ 2026 关键：**重放 ≠ 重跑**——Diagrid Catalyst 的核心卖点"重启后重放记录的历史、从最后完成的 activity 恢复，已完成的 LLM/工具调用绝不重新发出"（防 token 重复计费与重复副作用）；框架级"重跑整个 checkpoint"是错误模式。

## 3. 非确定性重放：最难的问题

| 挑战 | 机制 | 解法 |
|---|---|---|
| 非确定节点输出 | LLM 每次推理不同 | 记录输出——重放时读记录不重推理 |
| 推理失败为主错误 | 推理本身失败 | 重试预算 + 重放边界（05 篇） |
| 非幂等重试 | 重试执行副作用 | 幂等包装（05 篇） |
| 版本漂移 | 模型/提示/工具 schema/沙箱镜像变更 | **版本哈希固定**（重放危害） |

> 🎯 核心要点：**持久执行的核心纪律——"非确定性有重放边界"**：LLM 调用/工具副作用被记录成确定性事件，重放只回放记录；非确定性只在"新执行"中发生，不在"重放"中发生（Zylos 五层架构）。

## 4. Checkpoint 六义务（remit-contract，2026 形式化）

| 义务 | 内容 |
|---|---|
| 前缀延续 | 不重跑已完成工作 |
| 效果恰好一次 | 副作用不重复 |
| Fork 确定性 | 分支执行确定性 |
| Checkpoint 有效性 | checkpoint 可验证 |
| 消费一次 | 事件消费一次 |
| 恢复确定性 | 恢复路径确定 |

> 💡 remit-contract 的意义：**把"恢复正确性"从经验变成机器可检查契约**（Rust 核心 + Verus 模型 + TLA+ 规范 + LangGraph checkpointer shim）——2026 起"持久执行"有了可验证的定义，选型可对照六义务。

## 5. 持久执行 vs 框架 checkpoint

| 维度 | 框架 checkpoint（LangGraph 等） | 持久执行运行时（Temporal/Catalyst） |
|---|---|---|
| 恢复 | 手动/依赖设计 | 自动（事件历史重放） |
| 崩溃 | 单进程死亡杀 run | run 本身存活（任意 worker 恢复） |
| 重试 | 手动配置 | 内建（retries/timeouts/signals） |
| 副作用 | 不防护 | 重放跳过已完成（恰好一次） |
| 审计 | 弱 | 事件日志天然审计 |
| 选型 | <30s 短任务 | >30s/3+ 系统/生产副作用（08 篇） |

> ⚠️ 2026 实证（Diagrid 审查）：**MAF checkpointing 是"显式设计的最完整框架级方案"但仍非持久执行**——resume 全手动、无自动失败检测（无心跳/租约/watchdog）、无重复执行防护（两进程可并发恢复同一 checkpoint）、superstep 粒度丢并行子步——**"框架 checkpoint ≠ 持久执行"**。

## 6. 事件日志的设计规范

| 规范 | 内容 |
|---|---|
| 追加式 | append-only 事件存储（审计与重放共用） |
| 事件粒度 | 每次 LLM 调用/工具调用/交接一个事件 |
| 幂等记录 | 事件本身可重放（消费一次） |
| 保留策略 | 与合规对齐（观测 08 篇保留分层） |
| 血统 | 执行血统传播（Catalyst 加密签名，07 篇） |

> 💡 事件日志三重价值：**恢复（重放）+ 审计（轨迹）+ 调试（回放）**——"一个日志三个用途"是持久执行的经济性来源（观测 02 篇 trace 同源）。

## 7. 常见误区

| 误区 | 真相 |
|---|---|
| "快照够恢复" | 重放事件日志重建——不信任快照 |
| "重放=重跑" | 重放跳过已完成——防重复计费与副作用 |
| "框架 checkpoint=持久" | MAF 缺陷实证——无自动恢复/无重复防护 |
| "持久执行免费" | 事件日志存储 + 重放设计成本 |
| "非确定性没法处理" | 重放边界——记录输出不重推理 |
| "版本不用管" | 版本漂移是重放危害——哈希固定 |
| "恢复正确性靠经验" | 六义务机器可检查（remit-contract） |

## 8. 面试速记

| 问题 | 一句话答案 |
|---|---|
| 持久执行？ | 崩溃后从精确断点恢复——事件日志+快照 |
| 双机制？ | 日志可重放、快照加速——混合恢复 |
| 重放语义？ | 重放日志重建状态，跳过已完成——不重跑 |
| 不信任什么？ | 快照——重放是恢复正确姿势 |
| 非确定性怎么处理？ | 重放边界——记录输出不重推理 |
| 六义务？ | 前缀延续/恰好一次/fork 确定性/有效性/消费一次/恢复确定性 |
| MAF 缺陷？ | 手动 resume/无失败检测/无重复防护/superstep 粒度 |
| 框架 vs 运行时？ | 框架手动恢复，运行时事件重放自动 |
| 事件日志三价值？ | 恢复+审计+调试 |
| 版本漂移？ | 重放危害——哈希固定 |
| remit-contract？ | 六义务机器可检查（Verus+TLA+） |
| 选型界限？ | <30s 框架够，>30s/副作用运行时 |

---

**下一模块**：[03-执行引擎：checkpoint 与恢复](03-执行引擎：checkpoint与恢复.md)　**返回总览**：[00-Orchestrator Runtime 编排调度器总览](00-OrchestratorRuntime编排调度器总览.md)

## 参考来源

- [Durable Execution for AI Agent Runtimes（Zylos Research）](https://zylos.ai/zh/research/2026-04-24-durable-execution-agent-runtimes/)
- [Still Not Durable: MS Agent Framework & Strands（Diagrid）](https://www.diagrid.io/blog/still-not-durable-how-microsoft-agent-framework-and-strands-agents-repeat-the-same-mistake)
- [Agent Executor, Google's distributed Agent Runtime（Google Cloud Blog）](https://cloud.google.com/blog/products/ai-machine-learning/agent-executor-googles-distributed-agent-runtime/)
- [Diagrid Catalyst 2.0 Brings Durable, Verifiable Execution（Diagrid）](https://www.diagrid.io/press/catalyst-2-0-durable-verifiable-execution)
- [remit-contract（PyPI）](https://pypi.org/project/remit-contract/)
