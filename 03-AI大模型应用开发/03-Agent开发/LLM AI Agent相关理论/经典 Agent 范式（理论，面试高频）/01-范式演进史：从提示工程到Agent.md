# 范式演进史：从提示工程到 Agent

> 理论篇：Agent 范式三代演进与关键论文时间线——面试"范式从哪来"的标准答案。

## 1. 三代演进

| 代 | 阶段 | 核心思想 | 代表 |
|---|---|---|---|
| 第一代 | 提示工程 | 让模型"更会答"（不改变交互形态） | CoT（2022）/Few-shot |
| 第二代 | 推理范式 | 让模型"更会想"（结构化思考结构） | ToT（2023）/GoT（2023）/PAL |
| 第三代 | Agent 范式 | 让模型"会做事"（推理+行动循环） | ReAct（2022）/Plan-Execute（2023）/Reflexion（2023） |

> 🎯 核心要点：**三代不是替代是叠加**——Agent 范式内部仍用推理范式（ReAct 的 Thought 可用 CoT）、推理范式不改变"对话"形态、提示工程是基础层；面试答演进史要讲"叠加关系"而非"取代关系"。

## 2. 关键论文时间线

| 年份 | 论文/工作 | 贡献 |
|---|---|---|
| 2022 | Chain-of-Thought（Wei 等） | 推理范式奠基——中间步骤 |
| 2022 | ReAct（Yao 等） | **Agent 范式奠基**——推理+行动交织 |
| 2023 | Plan-and-Solve（Wang 等） | 先计划后执行（LangChain 推广） |
| 2023 | Tree of Thoughts（Yao 等） | 树搜索推理（Game of 24 4%→74%） |
| 2023 | Reflexion（Shinn 等） | 语言化自我批评（HumanEval 91%） |
| 2023 | Self-Refine（Madaan 等） | 自生成反馈改进 |
| 2023 | Graph of Thoughts（Besta 等） | 图结构推理（+62% 质量） |
| 2023 | PAL（Gao 等） | 程序辅助推理（GSM8K 72%） |
| 2024 | ReWOO | 计划先行、无观察依赖（确定性环境） |
| 2025-26 | 推理模型（o 系列/R1/Claude thinking） | 推理内建——外部脚手架部分冗余 |
| 2026 | Select-then-Solve | **范式路由**——每任务选范式（新理论） |

## 3. 推理模型对范式演进的冲击（2025-26）

| 变化 | 机制 |
|---|---|
| 显式 CoT 提示不再帮助 | 前沿模型推理内建——"let's think step by step"有时有害（HumanEval -15pp） |
| 外部 ReAct 脚手架部分冗余 | 推理模型内部多步推理——外部循环可能适得其反 |
| ToT 被内部搜索超越 | 推理模型前向传播内做树搜索（数千 thinking tokens） |
| 外部范式保留场景 | 可观察分支/可转向搜索/成本上限/小模型 |

> ⚠️ 2026 理论共识：**"范式没有消失，位置变了"**——显式范式从"主引擎"变成"针对特定场景的工程工具"（05 篇 ToT 三 yes 规则）；范式演进的下一个问题是"何时显式、何时内建"（06 篇路由）。

## 4. 演进的内在逻辑

```text
演进主线（面试可背）：
  答得更准（提示工程）→ 想得更深（推理范式）→ 做得更多（Agent 范式）
  → 选得更对（范式路由，2026）
  驱动：模型能力（推理内建）→ 显式脚手架需求下降
       → 范式选择成为"配置问题"而非"架构问题"
```

> 🎯 一句话：**"范式演进 = 模型内建能力的边界外推"**——模型会推理了，脚手架就让位；模型不会，脚手架就是必需品；2026 的范式路由回答"该用哪个"的新问题。

## 5. 面试速记

| 问题 | 一句话答案 |
|---|---|
| 三代演进？ | 提示工程（会答）→ 推理范式（会想）→ Agent 范式（会做）——叠加非替代 |
| 奠基论文？ | CoT（2022）/ReAct（2022）/Plan-Solve（2023）/ToT（2023）/Reflexion（2023） |
| 推理模型冲击？ | 推理内建——显式 CoT/ReAct 部分冗余 |
| 外部范式保留场景？ | 可观察/可转向/成本上限/小模型 |
| 演进主线？ | 答得准→想得深→做得多→选得对 |
| 范式消失了吗？ | 没有——位置从主引擎变工程工具 |
| 2026 新问题？ | 何时显式、何时内建（范式路由） |
| 与主流范式体系分工？ | 该体系讲实践，本体系讲理论 |

---

**下一模块**：[02-ReAct：推理与行动交织](02-ReAct：推理与行动交织.md)　**返回总览**：[00-经典 Agent 范式理论总览](00-经典Agent范式理论总览.md)

## 参考来源

- [ReAct, Plan-and-Execute, or Reflection?（dev.to）](https://dev.to/gabrielanhaia/react-plan-and-execute-or-reflection-the-three-agent-patterns-every-engineer-needs-in-2026-355p)
- [Towards reasoning era: a survey of long chain-of-thought（Semantic Scholar）](https://www.semanticscholar.org/paper/Towards-reasoning-era%3A-a-survey-of-long-for-large-Chen-Qin/f078092a132049b931419847200ca570ec99cfa2)
- [What Is Chain-of-Thought Prompting? (2026)（Respan）](https://www.respan.ai/articles/what-is-chain-of-thought-prompting)
- [Agent 范式演进：从 ReAct 到 Plan-Act-Observe-Reflect（CSDN）](https://blog.csdn.net/2301_80117363/article/details/163595772)
- [智能体经典范式深度解析（CSDN）](https://blog.csdn.net/2502_94273177/article/details/163042096)
