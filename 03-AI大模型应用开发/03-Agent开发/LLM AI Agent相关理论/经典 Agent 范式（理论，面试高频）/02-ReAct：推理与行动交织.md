# ReAct：推理与行动交织

> 理论篇：Agent 范式奠基者（Yao 等，2022）——面试必背的第一范式。

## 1. 核心机制

```text
ReAct 循环：
  Thought（想：分析现状，决定下一步）
    → Action（做：调用工具/输出）
    → Observation（看：读取结果）
    → 循环 → 直到最终回答
```

| 要素 | 说明 |
|---|---|
| 交织 | 推理与行动在同一循环交替——不分离 |
| 单循环 | 一个 while 循环 + 每次一个 LLM 调用 |
| 可解释 | 每个 Thought 可追溯（推理轨迹透明） |
| 接地 | 行动结果接地推理——减少幻觉 |

> 🎯 核心要点：**ReAct 的本质 = "边想边做"**——用行动结果校正推理（接地），用推理指导行动（选择）；2026 状态：原生工具调用内建在每款前沿模型——文本式 ReAct 提示格式成历史，**底层循环不变**。

## 2. 优势与劣势（面试对比表）

| 优势 | 劣势 |
|---|---|
| 简单（while 循环 + 单 LLM 调用） | **无全局视图**（不知道还剩几步） |
| 灵活（中途自适应） | **无限循环风险**（重复失败调用烧 token） |
| 可解释（Thought 可追溯） | 高 token（每步重发历史） |
| 信息收集任务高效 | 长程规划弱 |

> ⚠️ 2026 实证失败案例：**四 LangChain Agent 循环跑了 11 天，账单 $47,000**——无限循环是 ReAct 的第一生产风险；防御 = 步数上限 + **按工具调用上限**（Agent 倾向重试同一失败工具换措辞——"verifier stall"）+ 断路器（Orchestrator 05 篇）。

## 3. 2026 状态：何时还用 ReAct

| 场景 | 用/不用 |
|---|---|
| 信息收集（快模型搜 API 聚合） | ✅ 保留 |
| 控制流未知的探索任务 | ✅ 默认 |
| 深度分析（证明/复杂代码/多约束规划） | ❌ 推理原生模型 |
| 长程多步任务 | ❌ Plan-and-Execute（03 篇） |
| 高价值可重复任务 | ❌ 加 Reflexion（04 篇） |

> 💡 2026 理论：**"推理原生模型挑战显式 ReAct 脚手架"**——模型内部多步推理让外部 Thought/Action/Observation 循环"部分冗余"；显式循环保留给信息收集（快模型）——"用推理模型做深度分析、用 ReAct 快循环做信息收集"。

## 4. 变体与延伸

| 变体 | 机制 |
|---|---|
| ReAct + CoT | Thought 用思维链（可见推理） |
| Reflexion | ReAct 循环加批评+记忆（04 篇） |
| ReWOO | 计划先行、无观察依赖（确定性/静态环境） |
| ReAct + ToT | 每个 Thought 分支探索（05 篇） |
| 原生化 | 工具调用协议化（OpenAI/Anthropic tool_calls） |

## 5. 面试速记

| 问题 | 一句话答案 |
|---|---|
| ReAct 是什么？ | 推理与行动交织循环（Thought→Action→Observation）——Agent 范式奠基 |
| 优势？ | 简单/灵活/可解释/接地 |
| 劣势？ | 无全局视图/无限循环/高 token/长程弱 |
| $47,000 案例？ | 四 Agent 循环 11 天——无限循环第一生产风险 |
| 2026 状态？ | 原生化——底层循环不变、文本提示成历史 |
| 何时还用？ | 信息收集/控制流未知（快模型） |
| 何时不用？ | 深度分析（推理原生模型） |
| 防循环？ | 步数上限 + 按工具调用上限 + 断路器 |
| 与 Plan-Execute 区别？ | 边想边做 vs 先计划后执行 |
| 与 CoT 区别？ | CoT 只推理，ReAct 推理+行动 |

---

**下一模块**：[03-Plan-and-Execute：先计划后执行](03-Plan-and-Execute：先计划后执行.md)　**返回总览**：[00-经典 Agent 范式理论总览](00-经典Agent范式理论总览.md)

## 参考来源

- [ReAct, Plan-and-Execute, or Reflection?（dev.to）](https://dev.to/gabrielanhaia/react-plan-and-execute-or-reflection-the-three-agent-patterns-every-engineer-needs-in-2026-355p)
- [AI Agent主流范式全解析：从ReAct到Reflexion（百度开发者）](https://developer.baidu.com/article/detail.html?id=7651022)
- [ReAct: Synergizing Reasoning and Acting in Language Models（Yao et al., 2022）](https://arxiv.org/abs/2210.03629)
- [智能体经典范式深度解析（CSDN）](https://blog.csdn.net/2502_94273177/article/details/163042096)
