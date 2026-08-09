# Orchestrator Runtime 编排调度器知识体系总览

> 定位：Agent 生产工程化组件之「编排器与运行时」——Agent 的"操作系统"：执行引擎、持久执行、程序级调度、幂等重试、会话一致性、沙箱隔离。2026 核心共识：**"持久性、编排与可恢复性是任何企业生产 Agent 的真正阻碍"**——Agent 非确定性打破了经典可靠性假设（checkpoint/重试/幂等全部失效）；Google Agent Executor 开源与 Temporal/LangGraph 互补分层是两大标志事件。总览做索引与速查，子主题各一篇。

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)
5. [2026 生态基准](#5-2026-生态基准)

## 1. 知识体系导图

```text
Orchestrator Runtime 编排调度器
├── 01 运行时全景：编排器与运行时的分工       三层分工 / 持久三问题 / 运行时层次
├── 02 持久执行：durable execution 深潜       事件日志+快照 / 重放 / 恢复语义
├── 03 执行引擎：checkpoint 与恢复             checkpoint 粒度 / 轨迹分支 / DeltaBox
├── 04 调度与并发：程序级调度                 程序级调度 / OS 原语 / 工具边界暂停
├── 05 幂等与重试：非确定性世界               transient vs sampling / 幂等键 / 预算
├── 06 状态与会话一致性                        单写者 / 存储三模式 / SecretRef
├── 07 编排引擎：Temporal 与运行时生态         Temporal / Agent Executor / Catalyst
├── 08 框架与运行时分层：LangGraph 组合        pair pattern / 五测试 / 选型条件
├── 09 沙箱与隔离                              安全沙箱 / 默认拒绝 / TDX / 最小权限
└── 10 生产冲刺：落地清单与面试               12 避坑 / 面试题
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|---|---|---|---|
| 01 | [运行时全景：编排器与运行时的分工](01-运行时全景：编排器与运行时的分工.md) | 三层分工、持久三问题 | 全部（地基） |
| 02 | [持久执行：durable execution 深潜](02-持久执行：durable-execution深潜.md) | 事件日志+快照、重放语义 | 架构师 |
| 03 | [执行引擎：checkpoint 与恢复](03-执行引擎：checkpoint与恢复.md) | checkpoint 粒度、轨迹分支 | 落地开发者 |
| 04 | [调度与并发：程序级调度](04-调度与并发：程序级调度.md) | 程序级调度、OS 原语、队列 | 架构师 |
| 05 | [幂等与重试：非确定性世界](05-幂等与重试：非确定性世界.md) | 重试分类、幂等键、预算 | 落地开发者 |
| 06 | [状态与会话一致性](06-状态与会话一致性.md) | 单写者、存储三模式、SecretRef | 架构师 |
| 07 | [编排引擎：Temporal 与运行时生态](07-编排引擎：Temporal与运行时生态.md) | Temporal、Agent Executor、Catalyst | 选型决策者 |
| 08 | [框架与运行时分层：LangGraph 组合](08-框架与运行时分层：LangGraph组合.md) | pair pattern、五测试、选型 | 架构师 |
| 09 | [沙箱与隔离](09-沙箱与隔离.md) | 安全沙箱、默认拒绝、TDX | 安全关注者 |
| 10 | [生产冲刺：落地清单与面试](10-生产冲刺：落地清单与面试.md) | 12 避坑、面试题、落地清单 | 面试/上线前 |

## 3. 学习路线推荐

| 路线 | 路径 | 目标 |
|---|---|---|
| 入门（1 天） | 01 → 03 → 05 → 10 | 能处理 checkpoint 与重试 |
| 进阶（1 周） | 01-02 → 04 → 07 → 10 | 能选型运行时、设计调度 |
| 高级（2 周） | 全量 + 06 → 08-09 | 能做持久执行与隔离治理 |

## 4. 核心概念速查

| 概念 | 一句话定义 |
|---|---|
| 运行时（Runtime） | Agent 的执行基础设施：循环引擎、状态、调度、恢复 |
| 持久执行（Durable Execution） | 崩溃/重启/网络故障后自动恢复——事件日志 + 快照重放 |
| 程序级调度 | 以"LLM 轮→工具→轮"整个程序为调度单位（非单请求） |
| 工具边界暂停/恢复 | 在工具调用自然暂停点挂起（比取消/排队更优的背压） |
| OS 启发原语 | 准入控制/限流追踪/AIMD 背压/令牌预算/优先级队列 |
| Lane-aware FIFO | 每会话车道并发上限 + 全局 cap——防同会话并发写 |
| 幂等键 | 操作唯一键——重试时返回缓存结果（Stripe 模式） |
| Transient vs Sampling 重试 | 瞬态故障（要同样结果）vs 输出质量差（要不同结果）——相反处理 |
| 单写者一致性 | 并发组件更新共享会话状态时防破坏的内建机制 |
| 事件日志 + 快照 | 持久执行双机制：日志可重放、快照加速恢复 |
| 轨迹分支 | checkpoint 处测试备选路径——不丢原上下文 |
| 沙箱默认拒绝 | 未信任 LLM 生成代码的默认拒绝网络姿态（GKE Sandbox/Kata） |
| 重放语义 | 重放事件日志重建状态——不信任快照 |
| Checkpoint 六义务 | 前缀延续/效果恰好一次/fork 确定性/有效性/消费一次/恢复确定性（remit-contract） |
| 五测试 | 持久性/重试/并行扇出/可观测/成本投影（选型前必跑） |

## 5. 2026 生态基准

> 📅 基准窗口：2026-08。版本与事实以各模块【参考来源】为准。

- **运行时元年**："durability, orchestration, resumability 是企业生产 Agent 的真正阻碍"——**Google Agent Executor（2026-05 开源）**：事件日志+快照持久执行、默认拒绝沙箱（GKE Sandbox/Kata）、单写者会话一致性、连接恢复、轨迹分支；配套 **Agent Substrate**（K8s 基础 agent-first 计算层，毫秒级工具调用规模）。
- **可验证执行**：Diagrid Catalyst 2.0（2026-07，Dapr 1.18）：加密历史签名 + 执行血统 + 工作流证明——**每个执行步骤可加密签名追溯**（合规链）；支持 LangGraph/MAF/ADK/Strands/OpenAI Agents/CrewAI/Pydantic AI，气隙部署，10x 性能。
- **非确定性打破经典可靠性**："重试为确定性世界构建"（2026 实证）：LLM 每次重试推理不同——重试操作不是原操作；**$847 双收费事故**（Stripe 超时重试未查首次是否到账）；区分 transient（要同样结果）与 sampling（要不同结果）重试；幂等键模式成为标准（agent-ledger 哈希去重、SafeAgent SQLite guard 实测拦截 6 次重复动作）。
- **程序级调度成型**：NVIDIA ThunderAgent/Dynamo——以 (REASONING|ACTING)×(ACTIVE|PAUSED) 调度整个 Agent 程序（解决缓存占用爆炸 + 工具边界背压）；HiveMind 五个 OS 原语（准入/AIMD 背压/令牌预算/优先级队列）；OpenClaw Command Queue 车道感知 FIFO + 溢出策略；DynAMO 拓扑 DAG 并行（中位延迟 -1.6x、方差 -60%）。
- **Temporal/LangGraph 互补定型**："框架在引擎内"（LangGraph 管 Agent 逻辑 + Temporal 管持久性）成为 2026 生产形态；Temporal LangGraph 插件（图节点为 Activity、每节点 checkpoint）；LangGraph Platform 改名 **LangSmith Deployment**（2025-10）；选型五测试（durability 测试/重试分类/并行扇出 100-1000/可观测/成本投影——重试尾支配 p99）。
- **MAF checkpointing 缺陷编目**（Diagrid 审查）：resume 全手动、无自动失败检测（无心跳/租约/watchdog）、无重复执行防护（两进程可并发恢复同一 checkpoint）、superstep 粒度丢并行子步——**框架 checkpoint ≠ 持久执行**（"MS Agent Framework 与 Strands 重蹈同样错误"）。
- **行业格局**：云厂商开源工具层、基础设施赚钱（Google Agent Executor、MS MAF、AWS Bedrock AgentCore）——"上面的工具必须开源，否则没人信任"；运行时基础设施不解决治理（问责/可解释/策略仍要监管层）。
- **与体系分工**：[Multi-Agent 协作组件 02 篇](..%2FMulti-Agent协作组件（多智能体）%2F02-架构拓扑：编排vs编排舞.md) 讲多 Agent 拓扑中的编排概念；本体系深潜"编排器/运行时本体"（执行引擎/持久化/调度/幂等/沙箱）；[Tool 06 篇](..%2F..%2F..%2FAgent%20子组件专项学习%2FTool%20工具开发与注册%2F06-工具执行：执行器与调用循环.md) 讲执行循环内单调用纪律；[观测&可观测组件](..%2F观测%26可观测组件%2F00-观测可观测组件总览.md) 管运行时可观测。

---

**下一模块**：[01-运行时全景：编排器与运行时的分工](01-运行时全景：编排器与运行时的分工.md)

## 参考来源

- [Agent Executor, Google's distributed Agent Runtime（Google Cloud Blog）](https://cloud.google.com/blog/products/ai-machine-learning/agent-executor-googles-distributed-agent-runtime/)
- [Google adds open source Agent Executor（Computerworld）](https://www.computerworld.com/article/4176809/google-adds-open-source-agent-executor-to-support-ai-agents-in-production-3.html)
- [Diagrid Catalyst 2.0 Brings Durable, Verifiable Execution（Diagrid）](https://www.diagrid.io/press/catalyst-2-0-durable-verifiable-execution)
- [Still Not Durable: MS Agent Framework & Strands（Diagrid）](https://www.diagrid.io/blog/still-not-durable-how-microsoft-agent-framework-and-strands-agents-repeat-the-same-mistake)
- [LangGraph vs Temporal: AI Agent Orchestration Compared（LangChain）](https://www.langchain.com/resources/langgraph-vs-temporal)
- [Temporal's LangGraph Plugin adds Durable Execution（Temporal）](https://temporal.io/blog/temporal-langgraph-plugin-durable-execution)
- [Retries Were Built for a Deterministic World（TinyFish）](https://current.tinyfish.ai/issue/latest/practitioners-corner/article/18051)
- [Durable Execution for AI Agent Runtimes（Zylos Research）](https://zylos.ai/zh/research/2026-04-24-durable-execution-agent-runtimes/)
- [HiveMind: OS-Inspired Scheduling for Concurrent LLM Agent Workloads（Semantic Scholar）](https://www.semanticscholar.org/paper/HiveMind%3A-OS-Inspired-Scheduling-for-Concurrent-LLM-Agyemang-Kponyo/dc20dbd9309bca5f3180f24c3dbf83b1e29d0e9b)
- [DeltaBox: Scaling Stateful AI Agents with Millisecond-Level Sandbox Checkpoint/Rollback（arXiv 2605.22781）](https://arxiv-org.ezproxy.obspm.fr/html/2605.22781v1)
