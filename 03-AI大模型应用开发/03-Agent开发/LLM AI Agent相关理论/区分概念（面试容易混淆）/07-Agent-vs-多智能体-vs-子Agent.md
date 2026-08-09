# Agent vs 多智能体 vs 子 Agent

> 辨析篇：三个"Agent 规模"概念——**数量与从属关系**。深潜见 [Multi-Agent 协作组件](..%2F..%2FAgent组件%2F进阶工程化组件（生产环境必备）%2FMulti-Agent协作组件（多智能体）%2F00-Multi-Agent协作组件总览.md)。

## 1. 三概念定位

| 概念 | 数量 | 关系 | 一句话 |
|---|---|---|---|
| Agent | 1 | 独立 | 模型控制循环的自主系统 |
| 多智能体（Multi-Agent） | N | 对等/协作 | 多个 Agent 通过协议协作（A2A） |
| 子 Agent（Sub-agent） | 委派 | 从属 | 被主 Agent 委派任务的 Agent（预算隔离） |

> 🎯 核心要点：**子 Agent 是"委派关系"，多智能体是"对等关系"**——同一个 N 个 Agent 的系统里两种关系可并存：主 Agent 委派子 Agent（从属）、两个系统 Agent 对等协作（A2A）。

## 2. 混淆点 1：多智能体 vs 子 Agent

| 维度 | 多智能体 | 子 Agent |
|---|---|---|
| 关系 | 对等（各自目标） | 从属（委派任务） |
| 通信 | A2A/协议（水平） | 框架内委派（垂直） |
| 生命周期 | 独立 | 随父任务 |
| 失败 | 独立失败 | 可重试/替换（无状态） |
| 例子 | 研究 Agent ↔ 合规 Agent | 主 Agent → 研究子 Agent |

> 💡 面试判据：**"对方有自己的目标 = 多智能体；只执行你的任务 = 子 Agent"**——子 Agent 是"预算隔离"工具（主 Agent 上下文不膨胀，Tool 05 篇），多智能体是"能力组合"。

## 3. 混淆点 2：Agent vs 多智能体

| 维度 | 单 Agent | 多智能体 |
|---|---|---|
| 上下文 | 一个窗口（膨胀） | 每 Agent 最小窗口 |
| 工具 | 全部可见（选择负担） | 按域分配（决策负担分治） |
| 成本 | 单循环 | **3-10x token**（交接乘数） |
| 失败 | 单点 | 级联风险（12% 传播错误） |
| 何时选 | 工具 <20/单域 | 多域/权限隔离/并行需求 |

> ⚠️ 2026 铁律：**"单 Agent + 好工具定义通常胜过编排糟糕的多 Agent 团队"**（Multi-Agent 01 篇）——多 Agent 是决策不是时尚；决策矩阵：域 × 权限 × 并行 × 失败隔离四维。

## 4. 混淆点 3：Agent vs 子 Agent 的"子"字

| 误区 | 真相 |
|---|---|
| "子 Agent = 更小模型" | 子 Agent 是"关系"不是"规模"——可用相同模型 |
| "子 Agent 一定有自己记忆" | 无状态子 Agent 可重试可替换（Multi-Agent 01 篇） |
| "子 Agent 自动有安全边界" | 交接是信任边界——零信任（Multi-Agent 08 篇） |

> 🎯 一句话：**"'子'表示从属关系，不表示能力大小"**——子 Agent 的关键属性是"可委派/可替换/预算隔离"（感知 08 篇子 Agent 预算隔离联动）。

## 5. 2026 生产形态

| 形态 | 结构 |
|---|---|
| 单 Agent | 一个循环 + 工具（多数任务够） |
| 主-子 | 主 Agent 委派（预算隔离——研究子 Agent 读 50 文件不占主上下文） |
| 对等多智能体 | A2A 协议协作（跨框架/跨组织） |
| 混合 | 主-子 + 对等并存（最常见） |

> 💡 2026 实证：**"三站式"（Triage 路由 + 窄专家）是生产主流**——Triage Agent 判定路由 + 专家执行；与子 Agent 的区别：Triage 是"路由关系"，子 Agent 是"委派关系"（可能重叠）。

## 6. 面试速记

| 问题 | 一句话答案 |
|---|---|
| 三概念判据？ | 数量 + 关系（对等/从属） |
| 子 Agent vs 多智能体？ | 从属委派 vs 对等协作 |
| 何时用单 Agent？ | 工具 <20/单域——多数任务够 |
| 多 Agent 代价？ | 3-10x token + 级联风险 |
| 决策矩阵？ | 域×权限×并行×失败隔离 |
| "子"表示什么？ | 从属关系——不是能力大小 |
| 子 Agent 价值？ | 预算隔离（主上下文不膨胀） |
| Triage？ | 路由关系（判定派给谁） |
| 交接信任？ | 零信任——内部当外部 |
| 2026 主流？ | 三站式 + 混合（主子+对等并存） |

---

**下一模块**：[08-RAG vs 微调 vs 长上下文 vs Agent](08-RAG-vs-微调-vs-长上下文-vs-Agent.md)　**返回总览**：[00-区分概念总览](00-区分概念总览.md)

## 参考来源

- [Multi-Agent Orchestration: A Survey（MDPI）](https://www.mdpi.com/1999-5903/18/6/326)
- [CrewAI vs LangGraph vs AutoGen 2026（FutureAGI）](https://futureagi.com/blog/crewai-vs-langgraph-vs-autogen-2026/)
- [Context Assembly（Redis）](https://redis.io/blog/context-assembly-building-the-prompt-the-model-sees.md)
- [When AI Agents Collide（NASSCOM）](https://community.nasscom.in/communities/ai/when-ai-agents-collide-multi-agent-orchestration-failure-playbook-2026)
