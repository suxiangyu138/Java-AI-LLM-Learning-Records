# LLM 大脑是什么

> 概念速记：Agent 的推理决策核心。完整版见 [Agent 四大核心组件 02 篇](..%2F..%2F..%2FAgent%20四大核心组件%2F02-LLM大脑：推理决策核心.md)。

## 核心定义

LLM 大脑 = Agent 中负责"想"的部分：接收感知输入与记忆，进行推理与决策，输出行动指令。它不执行工具、不保存记忆——**只做决策**；执行（工具）、存储（记忆）由其他组件负责（[感知模块](..%2F..%2F进阶工程化组件（生产环境必备）%2F感知模块%20Perception%2F00-感知模块Perception总览.md)、[工具体系](..%2F..%2F..%2FAgent%20子组件专项学习%2FTool%20工具开发与注册%2F00-Tool工具开发与注册总览.md)、[记忆系统](..%2F..%2F..%2FAgent%20子组件专项学习%2FMemory%20记忆系统%2F00-Memory记忆系统总览.md)）。

| 问题 | 答案 |
|---|---|
| 大脑做什么 | 推理（想清楚）+ 决策（做什么/下一步） |
| 大脑不做什么 | 不执行工具、不保存记忆、不感知原始输入 |
| 输入 | 感知装配的上下文 + 记忆召回 |
| 输出 | 行动指令（工具调用/回复）或计划 |

## 与 Agent 的关系

- **大脑是"决策者"，不是"执行者"**——模型只发出结构化请求，宿主执行（Tool 01 篇决策-执行分离）。
- **推理模型 ≠ 完整 Agent**——推理模型提高每步决策质量，但多步任务仍要循环（Thought-Action-Observation）；"如果计划需要多次工具调用，你仍然需要循环"（2026 共识）。
- **五组件之一**——感知/推理规划/记忆/行动/反馈组成连续循环，大脑居中调度（完整版 01 篇）。

## 2026 一句话认知

> 🎯 **"LLM 大脑 = 推理模型（thinking tokens）+ 决策循环（ReAct 模式）"**——thinking 让单步更准，循环让多步能成；两者缺一，Agent 不完整。

## 面试速记

| 问题 | 一句话答案 |
|---|---|
| LLM 大脑是什么？ | Agent 的推理决策核心——想什么、怎么想、做什么决定 |
| 大脑不做什么？ | 不执行/不记忆/不感知——只决策 |
| 推理模型是 Agent 吗？ | 不是——提高单步质量，循环仍需 |
| 大脑输出什么？ | 行动指令（工具调用/计划/回复） |
| 与工具分工？ | 模型建议，宿主执行（决策-执行分离） |

---

**下一模块**：[02-推理与决策机制](02-推理与决策机制.md)　**返回总览**：[00-LLM 大脑最简定义总览](00-LLM大脑最简定义总览.md)

## 参考来源

- [Reasoning model as agent planner（The Neural Base）](https://theneuralbase.com/reasoning-models/learn/intermediate/reasoning-model-as-agent-planner/)
- [What Are ReAct Agents? (2026 Guide)（Respan）](https://www.respan.ai/articles/what-is-react-agents)
- [AI Reasoning Models Explained: Test-Time Compute (2026)（Taskade）](https://www.taskade.com/blog/reasoning-models)
