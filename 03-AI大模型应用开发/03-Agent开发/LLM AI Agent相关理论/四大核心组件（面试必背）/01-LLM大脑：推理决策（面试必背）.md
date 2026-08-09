# LLM 大脑：推理决策（面试必背）

> 背诵篇：四大核心组件之「LLM 大脑」——30 秒背诵卡 + 4 个面试高频题 + thinking tokens 与成本路由。深化见 [Agent 四大核心组件 02 篇](..%2F..%2FAgent%20四大核心组件%2F02-LLM大脑：推理决策核心.md)，速记见 [LLM 大脑最简定义](..%2F..%2FAgent组件%2F四大基础核心组件（最简定义）%2FLLM大脑（推理决策模块）%2F00-LLM大脑最简定义总览.md)。

## 1. 30 秒背诵卡

| 概念 | 背诵版一句话 |
|---|---|
| LLM 大脑 | Agent 的推理决策核心——"想什么、怎么想、做什么决定" |
| 与 Agent 关系 | 大脑管决策质量，Harness/循环管系统怎么动——**两者分工，互不替代** |
| 决策循环 | Thought（想）→ Action（做）→ Observation（看）→ 循环（ReAct 模式） |
| 推理模型 | 先内部思考再回答的模型（thinking tokens 隐藏但计费） |
| Thinking tokens | 推理质量的新计算轴——按任务难度"调旋钮"是成本控制正解 |
| 普通 vs 推理模型 | 多步可验证任务用推理；查找/摘要用快速模型（路由） |
| 2026 阵容 | OpenAI GPT-5 内建路由 / Claude budget_tokens / Gemini thinkingBudget / DeepSeek-R1 |
| 核心洞察 | 推理模型提高"每步决策质量"，**不消除** Agent 循环 |

## 2. 面试高频问题（背诵版）

**Q1. LLM 在 Agent 里扮演什么角色？为什么说它是"大脑"？**
> 答：LLM 是 Agent 唯一的通用推理组件——理解指令、拆解目标、根据反馈调整策略，这些是规则代码做不到的。叫"大脑"是因为它决定"下一步做什么"，但大脑不会行动：工具给手脚、循环给自主性、记忆给上下文——**大脑 + Harness 才是完整 Agent**（2026 共识：Agent = Model + Scaffolding + Harness）。

**Q2. 推理模型（reasoning model）和普通模型有什么区别？Agent 该怎么选？**
> 答：区别在 test-time compute——推理模型回答前先内部思考（thinking tokens，5-60 秒，隐藏但计费）。选择标准：**多步可验证任务**（数学、代码调试、工具调用序列——一个坏计划浪费每次工具调用）用推理模型；**查找/摘要/分类**（成本延迟主导）用快速模型。2026 最佳实践是**按任务难度路由**，不是全用推理模型（GPT-5 内建路由匹配此前推理质量、省 50-80% 输出 token）。

**Q3. thinking tokens 的成本问题怎么控制？**
> 答：三个手段——① **按难度路由**：70% 常见问题走快速路径、25% 中等任务给中等预算、5% 新问题给完整思考，可省 ~60% 成本无质量回退；② **调旋钮**：Claude budget_tokens（≥1024）、Gemini thinkingBudget、OpenAI reasoning_effort——thinking 已是"拨盘"不是"开关"；③ **收益递减纪律**：no→medium 提升陡、high→max 几乎平（Snell 计算最优）——**超过任务难度的思考是浪费**。提醒：Agent 开推理通常多发 2-5 倍 token。

**Q4. 推理模型会不会取代 Agent 循环？**
> 答：不会——只改变"思考发生的位置"。推理模型把**计划、自纠错、工具选择**部分内建（多步编码 Agent 从 8-12 次串行调用降到 2-3 次），但**执行、外部反馈、终止判断仍需循环**；编排层缩小但保留治理/验证/可观测（金句："**never let a reasoning model make unvalidated state changes**"——绝不让推理模型做未经验证的状态变更）。经验法则：单请求若 >4 次串行模型调用，值得用推理模型重新基准。

## 3. 避坑与易混点

| 坑 | 澄清 |
|---|---|
| "最强模型 = 最好的 Agent" | 模型同质化 2026——差异在 Harness/Scaffolding，不在模型 |
| "thinking 打开就完事" | 全量开 thinking 是 2026 最大成本错误——必须路由 + 旋钮 |
| "推理模型 = 自主 Agent" | 推理内建 ≠ 自主行动——没有工具与循环仍是"更聪明的对话" |
| "thinking tokens 可以省" | 隐藏但计费（Opus 4.7 输出+thinking $75/M）——预算要看总账 |
| 大脑 vs 循环 | 推理模型提高每步质量，不消除 Thought-Action-Observation |

## 4. 面试速记

| 问题 | 一句话答案 |
|---|---|
| 大脑的角色？ | 推理决策核心——想什么、怎么想、做什么决定 |
| 推理 vs 普通模型？ | 多步可验证用推理；查找摘要用快速——按难度路由 |
| thinking 成本控制？ | 路由（70/25/5）+ 旋钮（budget_tokens/effort）+ 收益递减纪律 |
| 推理模型取代循环吗？ | 不——内建思考但保留执行/反馈/终止；编排层缩小不消失 |
| 2026 金句？ | 绝不让推理模型做未经验证的状态变更 |

---

**下一模块**：[02-规划组件：任务分解（面试必背）](02-规划组件：任务分解（面试必背）.md)　**返回总览**：[00-四大核心组件面试总览](00-四大核心组件面试总览.md)

## 参考来源

- [AI Reasoning Models Explained: Test-Time Compute (2026)（Taskade）](https://www.taskade.com/blog/reasoning-models)
- [Reasoning Models Are Rewiring Agent Architecture（turion.ai）](https://turion.ai/blog/reasoning-models-agent-architecture-2026/)
- [Adaptive Compute Allocation — Reasoning（Agent Patterns Catalog）](https://www.agentpatternscatalog.org/patterns/adaptive-compute-allocation/)
- [LLM Powered Autonomous Agents（Lilian Weng，OpenAI）](https://lilianweng.github.io/posts/2023-06-23-agent/)
