# LLM vs Chatbot vs Workflow vs Agent

> 辨析篇：四大概念的第一判据——**谁控制循环**。面试最高频概念对。

## 1. 四分类对比（2026 定义）

| 维度 | LLM | Chatbot | Workflow | Agent |
|---|---|---|---|---|
| 本质 | 文本生成引擎 | 对话+暂停等待 | 确定性预定义步骤 | 模型控制循环 |
| 谁控制循环 | — | 人类 | 开发者 | **模型** |
| 调用工具 | ❌ | 少（检索为主） | ✅ 预定义序列 | ✅ 模型选择 |
| 多步目标 | ❌ | ❌ | ✅ 固定路径 | ✅ 自适应路径 |
| 确定性 | ✅ | ✅ | ✅ | ❌（非确定） |
| 一图区分 | "写解释" | "答完就等" | "脚本+判断烘焙" | "替你做" |

> 🎯 核心要点：**LLM 是组件不是系统**（可被其他三类内嵌）；Chatbot 答完就等用户；Workflow 每条分支预编码（LLM 只是流水线的一步）；Agent 由模型决定"下一步做什么"——**"工作流让人跑得更快，Agent 替你跑"**。

## 2. Agent 三问测试（判据）

| 问题 | 说明 |
|---|---|
| ① 能不经用户提示决定下一步吗？ | 自主决策 |
| ② 能用改状态的工具吗（创建/更新/提交）？ | 行动能力（非只读检索） |
| ③ 能验证结果并调整计划吗？ | 自我修正 |

> ⚠️ 2026 实用规则：**三问多数"否"= 助手/工作流，即使市场叫它 Agent**——"能写邮件但不能发送/记 CRM/三天后跟进 = Chatbot 不是 Agent"（CloudThat 2026）。

## 3. 定义特征：授权的自主性

| 项 | 说明 |
|---|---|
| 不是智能 | 是**在显式边界内的自主**（规划/工具/验证） |
| 组成 | LLM（决策）+ 工具（行动）+ 记忆（状态）+ 控制循环 |
| 典型模式 | ReAct 循环（Reason→Act→Observe→repeat）直到目标达成/判定不可达 |
| 非确定性 | 同输入可能不同路径（可观测/评估必需） |

> 💡 面试金句：**"Agent 的定义特征是授权的自主性（authorized autonomy），不是智能"**——边界（护栏）与自主（循环）缺一不可（Guardrails 体系联动）。

## 4. 2026 为什么 Agent 能用了

| 栈迁移 | 说明 |
|---|---|
| 推理 | 一次性完成 → 推理模型（规划/自检/中途恢复） |
| 接口 | 单轮问答 → **run loops**（多步到退出条件） |
| 工具 | 定制函数调用 → MCP 标准协议 |
| 上下文 | 4K-8K → 100K-1M（装下整个仓库） |
| 经济 | 长任务背景运行可负担 |

> 🎯 核心要点：**"Agent 的难点不在模型，在 harness"**——循环/会话状态/工具权限/护栏/遥测；同一模型的两个 Agent 可靠性可以天差地别，只因 harness 不同（Harness Engineering 体系联动）。

## 5. 选型（2026 共识）

| 场景 | 选型 |
|---|---|
| 可预测流程、错误成本高 | Workflow（便宜/可靠/可审计） |
| 需要对话 UX | Chatbot |
| 开放式、无法预定义 | Agent（战略赌注） |
| 多数团队 | **不该建 Agent**——先验证简单方案 |

> ⚠️ 2026 反模式：**"为 Agent 而 Agent"**——简单 Q&A 用 Agent = 零收益加延迟/成本/失败面；"可预测流程（<10 分支）构建 Workflow，它更便宜更快更可靠"（CloudThat）。

## 6. 面试速记

| 问题 | 一句话答案 |
|---|---|
| 第一判据？ | 谁控制循环（人类/开发者/模型） |
| LLM vs Agent？ | LLM 写解释，Agent 独立完成多步任务 |
| Chatbot vs Agent？ | 答完就等 vs 自主推进 |
| Workflow vs Agent？ | 路径预定义 vs 模型决定 |
| 三问测试？ | 自主决策/改状态工具/验证调整 |
| 定义特征？ | 授权的自主性——不是智能 |
| 2026 为什么可用？ | 推理+run loops+MCP+长上下文+经济（栈迁移非单突破） |
| 难点在哪？ | harness——不是模型 |
| 选型？ | 可预测→Workflow、对话→Chatbot、开放式→Agent |
| 多数团队？ | 不该建 Agent——先验证简单方案 |

---

**下一模块**：[02-函数调用 vs 工具 vs MCP vs Skills](02-函数调用-vs-工具-vs-MCP-vs-Skills.md)　**返回总览**：[00-区分概念总览](00-区分概念总览.md)

## 参考来源

- [Agents, Chatbots, and Workflows（CloudThat）](https://www.cloudthat.com/resources/blog/agents-chatbots-and-workflows-stop-calling-everything-an-agent)
- [Agents 101: Reasoning, Actions & Autonomy（TokenJam）](https://tokenjam.dev/blog/2026-05-08-agents-101)
- [Agentic AI Cheat Sheet（eWeek）](https://www.eweek.com/news/agentic-ai-cheat-sheet/)
- [AI agents in business 2026（Hauer Power）](https://www.hauerpower.com/en/insights-posts/ai-agents-in-business-what-they-are-how-they-work)
