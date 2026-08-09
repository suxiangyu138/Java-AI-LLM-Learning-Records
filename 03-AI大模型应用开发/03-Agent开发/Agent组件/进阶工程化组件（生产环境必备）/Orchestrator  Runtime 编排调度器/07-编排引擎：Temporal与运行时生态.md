# 编排引擎：Temporal 与运行时生态

> 编排引擎 = 持久执行与工作流保证的"重型武器"。2026 生态：**Temporal（久经考验的通用引擎）**、Google Agent Executor（Agent 专用开源）、Diagrid Catalyst（可验证执行）三足鼎立 + 轻量派（Kitaru/contd.ai）与控制平面（Kheish）。本章给引擎生态全景与选型。

## 1. Temporal：通用持久执行标杆

| 项 | 说明 |
|---|---|
| 机制 | 事件历史 + **确定性重放**（工作流代码确定性、副作用封装 Activities） |
| 保证 | 崩溃/重启/网络故障后工作流完成（run 本身存活） |
| 内建 | 重试/超时/signals/approval 工作流 |
| 案例 | Uber/Netflix/Snap 久经考验；支撑 OpenAI/Cursor/Lovable/Block 的 Agent 体验 |
| 限制 | ~2MB gRPC 负载（大上下文需外部存储）；非 AI 专用（无 token 成本追踪） |

> 🎯 核心要点：**Temporal 的确定性重放 = "工作流代码本身活着"**——不是 checkpoint 状态活着，是执行历史活着；LLM 调用必须包在 Activities 里（副作用与确定性分离）。

## 2. Google Agent Executor：Agent 专用开源运行时（2026-05）

| 能力 | 说明 |
|---|---|
| 持久执行 | 事件日志 + 快照——中断/HITL 确认后自动恢复 |
| 安全隔离 | 默认拒绝沙箱（GKE Sandbox/Kata）——未信任 LLM 代码无网络 |
| 会话一致性 | 单写者（06 篇） |
| 连接恢复 | 客户端重连回填（03 篇） |
| 轨迹分支 | checkpoint 处实验备选路径（03 篇） |
| 配套 | Agent Substrate——K8s agent-first 计算层（毫秒级工具调用规模） |

> 💡 Agent Executor 的意义：**"运行时标准化"的开源信号**——harness 无关（LangGraph/ADK/Gemini Managed Agents/A2A 协议 Agent 都可跑）；"上面的工具必须开源，否则没人信任"（行业逻辑）。

## 3. Diagrid Catalyst 2.0：可验证执行（2026-07）

| 能力 | 说明 |
|---|---|
| 持久执行 | 每 LLM 调用/工具调用为 activity——重启重放、绝不重发已完成 |
| **加密可验证** | Dapr 1.18 加密历史签名 + 执行血统传播 + 工作流证明——合规链 |
| 覆盖 | LangGraph/MAF/ADK/Strands/OpenAI Agents/CrewAI/Pydantic AI |
| 部署 | 多云/主权/气隙；10x 开源 Dapr 性能；百万级并发工作流 |

> 🎯 核心要点：**Catalyst 把"执行"变成"可验证的链"**——每个步骤加密签名可追溯（审计/合规的新基准）；"持久 + 可验证"是 2026 运行时能力的最高形态（02 篇恰好一次 + 加密）。

## 4. 轻量派与控制平面

| 引擎 | 定位 |
|---|---|
| Kitaru | ZenML 基于：checkpoint/replay/resume/wait()/版本化部署——"运行时层在你的 Agent 栈之下" |
| contd.ai | 轻量多租户：可恢复默认 + 认知保存点 + 混合恢复 + 自动去重（Python/TS/Go/Java SDK） |
| Kheish | 控制平面：调用者/执行分离 + 日志中断生存 + 追加式审计——"Agent 界的 K8s" |
| Continuum | 基础设施档案：Temporal（持久工作流）+ Redis（会话状态）+ 向量库 |

> 💡 定位分层：**引擎管"执行生存"，控制平面管"治理身份"**——Kheish 的不变量"Agent 的执行持久、有界、可问责，独立于任何调用者"是控制平面的宣言（Multi-Agent 08 篇问责联动）。

## 5. 运行时选型决策

```text
运行时选型：
├─ 通用持久执行（久经考验/大负载）
│   └─ Temporal（Activities 封装 LLM 调用）
├─ Agent 专用开源（默认拒绝沙箱/轨迹分支）
│   └─ Google Agent Executor
├─ 合规要求可验证执行
│   └─ Diagrid Catalyst 2.0（加密签名）
├─ 轻量自托管
│   └─ Kitaru / contd.ai
└─ 治理控制平面
    └─ Kheish（叠加在引擎之上）
```

| 维度 | 决策 |
|---|---|
| 成熟度 | Temporal（Uber/Netflix 级） |
| Agent 原生 | Agent Executor（沙箱/轨迹分支） |
| 合规链 | Catalyst（加密签名） |
| 轻量 | Kitaru/contd.ai |
| 治理 | Kheish 叠加 |

> 🎯 核心要点：**运行时不是单选**——"引擎 + 控制平面 + 计算层"可组合（Agent Executor + Substrate、Temporal + Kheish）；2026 生产栈 = 一个引擎 + 一个治理层 + 可观测。

## 6. 运行时选型前五测试（08 篇详述）

| 测试 | 内容 |
|---|---|
| 持久性 | 杀 worker 中途——验证从最后 checkpoint 恢复 |
| 重试 | 现实失败类（provider 5xx vs 工具超时——不同退避） |
| 并行扇出 | 100-1000 并发子任务 |
| 可观测 | 每步 span（输入/输出/模型/延迟/成本/重试数） |
| 成本投影 | 重试尾支配 p99——规模成本 |

> ⚠️ 五测试是选型的"体检"——**不跑五测试的选型是猜**；尤其"重试尾支配 p99 支出"决定了规模成本模型（05 篇）。

## 7. 常见误区

| 误区 | 真相 |
|---|---|
| "Temporal 是 Agent 框架" | 通用持久引擎——Agent 逻辑还要框架（08 篇 pair pattern） |
| "LLM 调用直接进工作流" | 必须 Activities——确定性重放要求 |
| "持久执行=可验证" | Catalyst 加加密层才可验证——普通持久无合规链 |
| "开源引擎选一个" | 引擎+控制平面+计算层可组合 |
| "运行时解决治理" | Kheish 类控制平面才管身份/问责 |
| "轻量=能力全" | Kitaru/contd.ai 定位轻量——重型需求上 Temporal/Catalyst |
| "选型靠文档" | 五测试——持久/重试/扇出/观测/成本 |

## 8. 面试速记

| 问题 | 一句话答案 |
|---|---|
| Temporal 机制？ | 事件历史+确定性重放——工作流代码本身活着 |
| LLM 调用封装？ | Activities——副作用与确定性分离 |
| Temporal 案例？ | Uber/Netflix/OpenAI/Cursor |
| Agent Executor？ | Google 2026-05 开源——持久/沙箱/单写者/轨迹分支 |
| Agent Substrate？ | K8s agent-first 计算层——毫秒级工具调用 |
| Catalyst 2.0？ | 加密历史签名+血统+工作流证明——可验证执行 |
| Kheish？ | 控制平面——调用者/执行分离+审计 |
| 轻量派？ | Kitaru（ZenML）/contd.ai（多租户） |
| 运行时组合？ | 引擎+控制平面+计算层 |
| 合规选型？ | Catalyst（加密链） |
| 选型前提？ | 五测试——不跑是猜 |
| 行业逻辑？ | 工具开源、基础设施赚钱 |

---

**下一模块**：[08-框架与运行时分层：LangGraph 组合](08-框架与运行时分层：LangGraph组合.md)　**返回总览**：[00-Orchestrator Runtime 编排调度器总览](00-OrchestratorRuntime编排调度器总览.md)

## 参考来源

- [LangGraph vs Temporal: AI Agent Orchestration Compared（LangChain）](https://www.langchain.com/resources/langgraph-vs-temporal)
- [Agent Executor, Google's distributed Agent Runtime（Google Cloud Blog）](https://cloud.google.com/blog/products/ai-machine-learning/agent-executor-googles-distributed-agent-runtime/)
- [Diagrid Catalyst 2.0 Brings Durable, Verifiable Execution（Diagrid）](https://www.diagrid.io/press/catalyst-2-0-durable-verifiable-execution)
- [Temporal vs LangGraph: Durable AI Agent Workflow Guide（Cordum）](https://cordum.io/blog/temporal-vs-langgraph)
- [kheish: Orchestration engine for long-running, tool-using AI agents（GitHub）](https://github.com/graniet/kheish)
- [kitaru（PyPI）](https://pypi.org/project/kitaru/0.13.1/)
