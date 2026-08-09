# 框架与运行时分层：LangGraph 组合

> 分层 = 2026 生产形态的答案：**"框架在引擎内"**——LangGraph 管 Agent 逻辑（图/工具路由），Temporal 管持久执行（run 本身存活）。2026 共识：**两者不是竞争是互补**（"像比较 React、PostgreSQL 和 nginx"）；选型条件清晰（30 秒/3+ 系统/副作用）。本章给分层架构与选型完整设计。

## 1. LangGraph vs Temporal：不是竞争是分层

| 维度 | LangGraph | Temporal |
|---|---|---|
| 角色 | Agent 逻辑层（图/工具路由） | 持久执行层（orchestration） |
| 状态模型 | 图状态 + checkpoint（节点间存数据） | 事件历史 + 确定性重放（**执行本身存活**） |
| 失败恢复 | 手动（单进程死亡杀 run） | 内建（run 任意 worker 恢复） |
| AI 特性 | LLM 可观测/eval/数百 MB 负载 | 无（2MB 负载限制） |
| 确定性 | 原生容纳非确定性 LLM | 工作流代码确定性（LLM 封装 Activities） |

> 🎯 核心要点：**一个管"Agent 怎么想"，一个管"流程怎么活"**——2026 生产形态"框架在引擎内"：LangGraph 写 Agent 逻辑，跑在 Temporal 上获得持久性；两生态已直接集成而非竞争。

## 2. 选型条件：什么时候加引擎

| 条件 | LangGraph 单独 | 加 Temporal |
|---|---|---|
| 时长 | <30s | >30s |
| 外部系统 | 1-2 个 | 3+ 个 |
| 崩溃容忍 | 可重跑 | 必须保证完成 |
| 副作用 | 无/可逆 | 生产副作用（支付/部署/消息） |
| 失败风险 | 低 | 3+ 工具调用 + 重试 = 中高风险（LangGraph 单独） |

> ⚠️ 2026 共识：**"大多数编排工作流不需要 Temporal 的耐久性等级"**——短任务/只读/单步：LangGraph checkpoint 够；跨 30 秒、3+ 系统、生产副作用：必须引擎。过度与不足同错（01 篇）。

## 3. Temporal LangGraph 插件（2026 Public Preview）

| 项 | 说明 |
|---|---|
| 机制 | LangGraph 图作为 Temporal Workflow——每节点为 Activity、每节点 checkpoint |
| 支持 | Graph API（StateGraph）+ Functional API（@entrypoint/@task） |
| 能力 | interrupts/Send/continue-as-new + LangSmith 集成（Python/TS） |
| 价值 | **LangGraph 代码零重写获得持久执行**——"如果使用 LangGraph，也应该使用 Temporal" |

> 💡 插件的意义：**把"框架在引擎内"变成配置而非重构**——持久执行从"架构决策"降为"集成选项"；这是 2026 框架/运行时收敛的样板。

## 4. LangGraph Platform → LangSmith Deployment

| 项 | 说明 |
|---|---|
| 变化 | 2025-10 托管 LangGraph Platform 改名 LangSmith Deployment |
| 定价 | 席位 + 部署 + run 计价 |
| 开源不变 | LangGraph 本身开源；托管控制平面与 LangSmith 计划绑定 |
| 影响 | 选型时"开源 LangGraph"与"托管平台"要分开评估 |

> ⚠️ 命名变更的教训：**平台产品改名 = 定位与定价重构**——选型文档/评估要基于当前产品（2026-08 口径），历史名字（LangGraph Platform）仅作迁移参考。

## 5. 选型五测试（跑完再定）

| 测试 | 内容 | 通过标准 |
|---|---|---|
| 1. 持久性 | 杀 worker 中途 | 从最后 checkpoint 自动恢复 |
| 2. 重试分类 | provider 5xx vs 工具超时 | 不同退避（05 篇） |
| 3. 并行扇出 | 100-1000 并发子任务 | 稳定（04 篇准入） |
| 4. 可观测 | 每步 span 带输入/输出/模型/延迟/成本/重试数 | OTel 完整（观测 03 篇） |
| 5. 成本投影 | 重试尾支配 p99 | 规模成本模型（05 篇） |

> 🎯 核心要点：**五测试是"体检"不是"选型表"**——文档对比无法替代杀 worker 实测；尤其第 5 项（重试尾 p99）是规模决策的隐藏变量。

## 6. 治理第三层：别指望框架或引擎

| 层 | 管什么 | 谁 |
|---|---|---|
| 框架 | Agent 逻辑 | LangGraph/CrewAI |
| 引擎 | 持久执行 | Temporal/Agent Executor |
| **治理** | 策略/审批/审计 | Cordum 类独立层 |

> ⚠️ 2026 警示：**LangGraph 和 Temporal 都不执行策略/审批门/审计轨迹**——高风险副作用的治理要第三层（Guardrails 09 篇治理内核联动）；"框架+引擎"解决执行，不解决"该不该执行"。

## 7. 常见误区

| 误区 | 真相 |
|---|---|
| "LangGraph vs Temporal 二选一" | 互补分层——框架在引擎内 |
| "全部上引擎" | 大多数工作流不需要——30s 规则 |
| "插件=完全替代" | 插件是集成——Graph API 与 Functional API 都要评估 |
| "平台名无关紧要" | LangSmith Deployment 改名=定价重构——按当前口径 |
| "文档选型够" | 五测试——杀 worker 实测 |
| "框架+引擎=治理" | 治理第三层——策略/审批/审计另需 |
| "LLM 直进工作流" | Activities 封装——确定性重放要求 |

## 8. 面试速记

| 问题 | 一句话答案 |
|---|---|
| pair pattern？ | 框架在引擎内——LangGraph 逻辑 + Temporal 持久 |
| 类比？ | 像比较 React/PostgreSQL/nginx——不同问题 |
| 选型条件？ | >30s/3+ 系统/生产副作用——加引擎 |
| 30s 规则？ | 短任务 LangGraph checkpoint 够 |
| LangGraph 插件？ | 图节点为 Activity——零重写获持久 |
| Platform 改名？ | LangSmith Deployment（2025-10）——定价重构 |
| 五测试？ | 持久/重试/扇出/观测/成本 |
| 重试尾？ | 支配 p99——成本投影必需 |
| 治理第三层？ | 框架+引擎不执行策略/审批/审计 |
| 与 07 篇关系？ | 07 讲引擎选型，本篇讲框架与引擎的分层 |
| 确定性要求？ | LLM 封装 Activities——工作流代码确定 |
| 选型方式？ | 五测试实测——不是文档对比 |

---

**下一模块**：[09-沙箱与隔离](09-沙箱与隔离.md)　**返回总览**：[00-Orchestrator Runtime 编排调度器总览](00-OrchestratorRuntime编排调度器总览.md)

## 参考来源

- [LangGraph vs Temporal: AI Agent Orchestration Compared（LangChain）](https://www.langchain.com/resources/langgraph-vs-temporal)
- [Temporal's LangGraph Plugin adds Durable Execution（Temporal）](https://temporal.io/blog/temporal-langgraph-plugin-durable-execution)
- [Temporal vs LangGraph: Durable AI Agent Workflow Guide（Cordum）](https://cordum.io/blog/temporal-vs-langgraph)
- [LangGraph vs Temporal vs Cordum: Which Do AI Agents Actually Need?（Cordum）](https://cordum.io/blog/langgraph-vs-temporal-vs-cordum)
- [Best AI Agent Orchestration Platforms in 2026（FutureAGI）](https://futureagi.com/blog/best-ai-agent-orchestration-platforms-2026/)
