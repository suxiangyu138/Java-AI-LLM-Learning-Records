# 经典 Agent 范式理论总览

> 定位：「LLM AI Agent 相关理论」之「经典范式」——**理论机理 + 面试高频**：ReAct/Plan-and-Execute/Reflexion 三大经典 + CoT/ToT/GoT 推理家族 + 2026 范式路由新理论。与 [主流 Agent 范式](..%2F..%2F主流%20Agent%20范式%2F00-主流Agent范式知识体系总览.md)（实践/工程/框架视角）分工：本体系讲**理论本体与面试表述**。2026 一句话：**"没有单一范式主导，范式选择是每任务的决策（范式路由）"**。

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [范式地图：一图速记](#3-范式地图一图速记)
4. [学习路线推荐](#4-学习路线推荐)
5. [2026 生态基准](#5-2026-生态基准)

## 1. 知识体系导图

```text
经典 Agent 范式（理论，面试高频）
├── 01 范式演进史：从提示工程到 Agent      三代演进 / 时间线 / 关键论文
├── 02 ReAct：推理与行动交织                奠基范式 / 优劣 / 失败案例
├── 03 Plan-and-Execute：先计划后执行        Planner-Executor / re-plan 门 / 实证
├── 04 Reflexion 与自我修正范式              批评循环 / oracle 前提 / 成本
├── 05 推理范式家族：CoT-ToT-GoT             CoT 奠基 / 树搜索 / PAL / 2026 算法
├── 06 范式路由：2026 新理论                 Select-then-Solve / 路由器 / 融合
├── 07 范式对比与选型理论                    对比矩阵 / 五生产形态 / 选型决策
└── 08 面试高频问答冲刺                      15 题 + 答题范式
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|---|---|---|---|
| 01 | [范式演进史：从提示工程到 Agent](01-范式演进史：从提示工程到Agent.md) | 三代演进、时间线、关键论文 | 全部（地基） |
| 02 | [ReAct：推理与行动交织](02-ReAct：推理与行动交织.md) | 机制、优劣、$47K 失败案例 | 全部（面试必背） |
| 03 | [Plan-and-Execute：先计划后执行](03-Plan-and-Execute：先计划后执行.md) | Planner/Executor、re-plan 门、实证 | Agent 工程师 |
| 04 | [Reflexion 与自我修正范式](04-Reflexion与自我修正范式.md) | 批评循环、oracle 前提、成本 | Agent 工程师 |
| 05 | [推理范式家族：CoT-ToT-GoT](05-推理范式家族：CoT-ToT-GoT.md) | CoT 奠基、ToT 树搜索、PAL、2026 算法 | 前沿关注者 |
| 06 | [范式路由：2026 新理论](06-范式路由：2026新理论.md) | Select-then-Solve、路由器、范式融合 | 前沿关注者 |
| 07 | [范式对比与选型理论](07-范式对比与选型理论.md) | 对比矩阵、五生产形态、选型决策 | 架构师 |
| 08 | [面试高频问答冲刺](08-面试高频问答冲刺.md) | 15 题 + 答题范式 | 面试前 |

## 3. 范式地图：一图速记

```text
范式家族一图（按"思考深度 × 外部动作"）：
  纯推理：Direct → CoT → ToT/GoT → 推理模型（内部搜索）
  推理+行动：ReAct（交织）→ Plan-Execute（先计划）→ ReWOO（计划无观察依赖）
  自我修正：Reflexion（批评+记忆）→ Self-Refine（自批评）→ CRITIC（外部批评）
  2026 路由：Select-then-Solve（每任务选范式——学习式路由器）
```

## 4. 学习路线推荐

| 路线 | 路径 | 目标 |
|---|---|---|
| 面试速成（1 天） | 00 → 02 → 07 → 08 | 能答范式对比与选型 |
| 理论深潜（1 周） | 01-03 → 04-05 → 07 → 08 | 能讲清机制与演进 |
| 前沿（2 周） | 全量 + 06 | 能谈范式路由与融合 |

## 5. 2026 生态基准

> 📅 基准窗口：2026-08。版本与事实以各模块【参考来源】为准。

- **三大经典不变，地位调整**：ReAct（2022，奠基）/Plan-and-Execute（2023，Wang 等）/Reflexion（2023，Shinn 等）仍是理论支柱；但 **2026 推理原生模型让显式 ReAct 脚手架"部分冗余"**——推理内建模型的显式外部循环可能适得其反；ReAct 保留给信息收集类任务（快模型搜 API 聚合）。
- **范式路由成为新理论**：Select-then-Solve（arXiv 2604.06753，2026）——六范式（Direct/CoT/ReAct/Plan-Execute/Reflection/ReCode）× 四前沿模型 × 十基准（~18K 运行）：**ReAct 在 GAIA 超 Direct 44pp、CoT 在 HumanEval 降 15pp——没有单一范式主导**；oracle 每任务选择超最佳固定范式 **17.1pp**；学习式轻量路由器 47.6%→53.1%；零样本自路由只对 GPT-5 有效——**范式选择应是被学习路由器的每任务决策**。
- **推理范式家族成熟**：CoT（2022 奠基——2026 前沿模型"推理内建"，显式提示不再帮助甚至有害，但对小模型仍大增益）；ToT（2023，Game of 24 4%→74%——推理模型内部搜索通常打败外部 ToT，外部 ToT 保留给可观察分支/可转向搜索/成本上限场景）；GoT（+62% 质量 -31% 计算）；PAL（72% vs 65.6% CoT）；SwiReasoning（ICLR 2026：隐式/显式切换 +1.8-3.1%、token 效率 +57-79%）。
- **生产形态五类**：单 Agent ReAct、Plan-and-Execute、层级监督（路由到专家）、maker-checker（行动者+验证者，降幻觉）、网络/swarm（对等共享 scratchpad）；**混合是常态**（前沿规划 + 便宜执行、CoT+ReAct、ToT+Reflexion）。
- **成本与失败实证**：四 LangChain Agent 循环 11 天账单 **$47,000**（ReAct 无限循环风险）；Plan-and-Execute 实证 +27% 完成/-42% 工具调用/-35% 时间（vs ReAct）但计划脆弱需 re-plan 门；Reflexion 每轮一次调用——高价值任务才值。
- **与体系分工**：[主流 Agent 范式](..%2F..%2F主流%20Agent%20范式%2F00-主流Agent范式知识体系总览.md) 讲实践/工程/框架（范式总览/五工作流/选型实践）；本体系讲理论本体/经典论文/面试表述；[Planner 规划器](..%2F..%2FAgent%20子组件专项学习%2FPlanner%20规划器%2F00-Planner规划器总览.md) 与 [Reflection 反思模块](..%2F..%2FAgent%20子组件专项学习%2FReflection%20反思模块%2F00-Reflection反思模块总览.md) 管组件深潜。

---

**下一模块**：[01-范式演进史：从提示工程到 Agent](01-范式演进史：从提示工程到Agent.md)

## 参考来源

- [ReAct, Plan-and-Execute, or Reflection? The Three Agent Patterns Every Engineer Needs in 2026（dev.to）](https://dev.to/gabrielanhaia/react-plan-and-execute-or-reflection-the-three-agent-patterns-every-engineer-needs-in-2026-355p)
- [Select-then-Solve: Paradigm Routing as Inference-Time Optimization for LLM Agents（arXiv 2604.06753）](https://arxiv.org/html/2604.06753)
- [LLM Agent Architectures in 2026: Core Components and Patterns（FutureAGI）](https://futureagi.com/blog/llm-agent-architectures-core-components/)
- [What is Tree of Thoughts Prompting? Branching Reasoning in 2026（FutureAGI）](https://futureagi.com/blog/what-is-tree-of-thoughts-prompting-2026/)
- [What Is Chain-of-Thought Prompting? (2026)（Respan）](https://www.respan.ai/articles/what-is-chain-of-thought-prompting)
- [Towards reasoning era: a survey of long chain-of-thought（Semantic Scholar）](https://www.semanticscholar.org/paper/Towards-reasoning-era%3A-a-survey-of-long-for-large-Chen-Qin/f078092a132049b931419847200ca570ec99cfa2)
- [Tree of Thoughts as a Classical Heuristic Search Problem（arXiv 2605.28566）](https://arxiv-org.ezproxy.obspm.fr/html/2605.28566v1)
- [Agent 范式演进：从 ReAct 到 Plan-Act-Observe-Reflect（CSDN）](https://blog.csdn.net/2301_80117363/article/details/163595772)
