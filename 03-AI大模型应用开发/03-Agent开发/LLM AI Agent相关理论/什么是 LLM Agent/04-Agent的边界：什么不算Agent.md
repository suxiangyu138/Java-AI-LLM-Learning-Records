# Agent 的边界：什么不算 Agent

> 理论篇：**"是 Agent"很好认，"不是 Agent"才值得辨析**——Agent vs Chatbot vs Workflow vs 普通程序四大边界，Gartner 的 agentwashing 警告，以及"什么时候根本不需要 Agent"（Anthropic 简单性原则）。概念对深度辨析见 [区分概念体系 01-LLM-vs-Chatbot-vs-Workflow-vs-Agent](../区分概念（面试容易混淆）/01-LLM-vs-Chatbot-vs-Workflow-vs-Agent.md)。

## 1. 边界总图

```text
四种形态的"自主程度"阶梯：
  普通程序 ──► Chatbot ──► Workflow ──► Agent
  代码决定一切   模型输出文本    代码编排步骤    模型决定流程与工具
  （无 LLM）   （无工具无循环）  （固定路径）    （动态路径，2026 共识）
```

| 形态 | 谁决定流程 | 有无工具 | 有无循环 | 是否 Agent |
|---|---|---|---|---|
| 普通程序/脚本 | 代码（写死） | 有（代码调用） | 无 | ❌ 没有模型参与决策 |
| Chatbot | 模型（但只答不问） | 一般无 | 无 | ❌ 单次生成，无行动 |
| Workflow | 代码（预先定义路径） | 有 | 有（但路径固定） | ❌ 步骤由代码编排 |
| **Agent** | **模型（动态掌控）** | **有** | **有（动态路径）** | ✅ |

> 🎯 核心要点（Anthropic 官方口径）：**Workflows 是"LLM 与工具被代码预定义路径编排"；Agents 是"LLM 动态掌控自身流程与工具使用"**。分界线不在"用没用模型"，在"流程谁说了算"。

## 2. 边界一：Agent vs Chatbot

| 维度 | Chatbot | Agent |
|---|---|---|
| 任务 | 回答单条消息 | 完成目标（多步） |
| 工具 | 一般没有 | 必须要有（查数据/执行/写文件） |
| 状态 | 可有多轮对话上下文 | 有状态 + 行动计划 |
| 终止 | 对话自然结束 | 目标达成或停止条件触发 |
| 本质 | 生成器 | 闭环执行器 |

> 💡 一句话：**Chatbot 回答"你问我答"，Agent 承接"帮我办成"**。能调工具 ≠ Agent；多轮对话 ≠ Agent——两者必须同时具备自主行动与循环决策。

## 3. 边界二：Agent vs Workflow

| 维度 | Workflow | Agent |
|---|---|---|
| 路径 | 代码预定义（固定顺序） | 模型动态决定（不可预写） |
| 例子 | 检索→生成→总结（RAG 管道）；提示链 | 自主调研、多文件代码修改 |
| 可预测性 | 高（步骤确定） | 低（步骤取决于中途观察） |
| 适用 | 定义明确、可拆分固定子任务 | 无法预测所需步骤数的开放问题 |
| 错误传播 | 可控 | 自主性带来**复合错误（compounding errors）**风险 |

Anthropic 的五种工作流模式（全部**不是** Agent，但常被误认）：

| 工作流 | 机制 |
|---|---|
| Prompt chaining（提示链） | 任务拆成固定步骤序列，可加程序化检查门 |
| Routing（路由） | 先分类输入，再分发到专门化下游（如简单→快模型，难→强模型） |
| Parallelization（并行化） | Sectioning 并行独立子任务 / Voting 多次运行取多样输出 |
| Orchestrator-workers（编排器-工人） | 中央 LLM 动态拆解任务、分派 worker、综合结果 |
| Evaluator-optimizer（评估-优化） | 一个 LLM 生成、另一个循环评估反馈 |

> ⚠️ 面试易混点：**Orchestrator-workers 常被叫"Agent"，但 Anthropic 把它归为工作流**——因为"编排逻辑"是可预定义的模式；只有当 worker 各自拥有动态闭环时才是多 Agent 系统（详见 [区分概念 07-Agent-vs-多智能体-vs-子Agent](../区分概念（面试容易混淆）/07-Agent-vs-多智能体-vs-子Agent.md)）。

## 4. 边界三：普通程序 vs Agent

| 维度 | 普通程序 | Agent |
|---|---|---|
| 决策者 | 开发者（代码写死逻辑） | 模型（运行时动态判断） |
| 输入 | 结构化参数 | 自然语言目标 |
| 边界情形 | 分支再多也是预设 | 路径是运行时生成的 |
| 关键测试 | 换一个没见过的输入能自适应对吗？ | 能（这才是 Agent） |

> 💡 判定技巧（面试可用）：**把同一个任务交给系统，如果系统面对意外情况（报错、缺数据、路径变化）能自己调整策略继续推进——是 Agent；如果直接抛异常或按预设分支走——不是。**

## 5. agentwashing：Gartner 的边界警告

| 要点 | 内容 |
|---|---|
| 定义 | 把本质依赖人工的 AI 助手（assistant）包装成"自主 Agent"的市场夸大 |
| 为什么出现 | Agent 是 2026 最大卖点，厂商竞相贴标签 |
| 危害 | 客户以为"放手即可"，实际仍需人工步步确认 |
| 识别方法 | 看自主闭环：能独立规划→执行→按反馈调整吗？有人在每个关键步骤等着确认？ |
| 面试用法 | "市面 Agent 产品鱼龙混杂，我会用'自主闭环 + 动态控制'两个硬指标甄别，警惕 agentwashing" |

## 6. 什么时候**不需要** Agent（反向边界）

Anthropic 明确建议（面试谈架构素养必加分）：

| 场景 | 更优解 | 原因 |
|---|---|---|
| 任务步骤固定 | 单次 LLM 调用 + 检索 + 上下文示例 | 根本不需要 agentic 系统 |
| 路径可预写 | Workflow | 可预测、便宜、好调试 |
| 延迟敏感 | 直接调用 / 工作流 | Agent 循环**以延迟和成本换任务性能** |
| 决策难信任 | 人工在环（human-in-the-loop） | Agent 多轮运行会放大错误 |
| 无环境反馈 | 单次生成 | 循环没有 ground truth 就失去意义 |

> 🎯 设计原则（Anthropic 原话）：**"找到最简单的方案，只有确实能改进结果时才增加复杂度"**——"考虑加复杂度，仅当它能实证地提升产出"。2026 面试高频追问"你什么时候不该用 Agent"，答案就在这张表。

## 7. 面试速记

| 问题 | 一句话答案 |
|---|---|
| Agent 与 Workflow 的分界线？ | 流程谁说了算——代码预定义=Workflow，模型动态掌控=Agent（Anthropic） |
| Chatbot 与 Agent？ | 一次问答 vs 目标驱动的多步行动闭环 |
| 普通程序与 Agent？ | 决策者是人写的代码 vs 运行时由模型动态判断 |
| 五个非 Agent 工作流？ | 提示链/路由/并行化/编排器-工人/评估-优化（Anthropic） |
| agentwashing？ | 把依赖人工的助手包装成自主 Agent 的市场夸大（Gartner） |
| 什么时候不该用 Agent？ | 步骤固定/路径可预写/延迟敏感/决策难信任/无环境反馈——简单优先 |
| 循环的代价？ | 以延迟和成本换任务性能，错误会复合放大 |

---

**下一模块**：[05-四大能力组件：规划-记忆-工具-反思](05-四大能力组件：规划-记忆-工具-反思.md)　**返回总览**：[00-什么是 LLM Agent 总览](00-什么是LLM%20Agent总览.md)

## 参考来源

- [Building effective agents（Anthropic 官方工程博客）](https://www.anthropic.com/engineering/building-effective-agents)
- [AI Agent 到底是什么？剥开营销外衣，看清它的骨架与血肉（阿里云开发者）](https://developer.aliyun.com/article/1752314)
- [LLM Agent: Definition, Examples & FutureAGI Guide (2026)](https://futureagi.com/glossary/llm-agent/)
- [Gartner Predicts 40% of Enterprise Apps Will Feature Task-Specific AI Agents by 2026](https://www.gartner.com/en/newsroom/press-releases/2025-08-26-gartner-predicts-40-percent-of-enterprise-apps-will-feature-task-specific-ai-agents-by-2026-up-from-less-than-5-percent-in-2025)
- [Gartner: 40% of Enterprise Apps will feature task-specific AI Agents by 2026（CXO Insight）](http://www.cxoinsightme.com/future/tech/gartner-40-of-enterprise-apps-will-feature-task-specific-ai-agents-by-2026/)
