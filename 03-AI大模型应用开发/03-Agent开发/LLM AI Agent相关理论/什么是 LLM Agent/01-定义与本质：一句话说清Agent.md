# 定义与本质：一句话说清 Agent

> 理论篇：「LLM Agent 到底指什么」——从一句话定义到三层精确拆解（Model/Scaffolding/Harness），再到"本质是运行范式而非产品"。面试第一问"什么是 Agent"的标准答案。

## 1. 一句话定义

> 🎯 **核心要点（面试背诵版）**：**Agent 是以 LLM 为认知核心，通过工具调用连接外部世界，在"感知—思考—行动—观察"的自主闭环中持续推进目标，直到完成或到达终止条件的系统。**

拆开这句话，四个要件缺一不可：

| 要件 | 含义 | 缺了会怎样 |
|---|---|---|
| 以 LLM 为认知核心 | 决策者是模型，不是规则代码 | 退化为传统程序 |
| 通过工具调用连接外部世界 | 能查 API、跑代码、读数据库 | 退化为聊天机器人（只能输出文本） |
| 自主闭环 | 自己决定下一步，持续多步直至完成 | 退化为"输入一次→输出一次"的普通调用 |
| 到达终止条件才结束 | 有停止机制（目标达成/上限/人工介入） | 死循环烧钱（ReAct 无限循环 11 天 $47K 案例） |

## 2. 三层精确拆解：Model / Scaffolding / Harness

社区最精确的定义来自 Hugging Face 工程师的梳理（2026 年被广泛引用）——**Agent 不是"模型 + 工具"两个东西，而是三层**：

| 层 | 是什么 | 包括什么 | 关键点 |
|---|---|---|---|
| **Model** | 裸的大语言模型 | 模型本身，权重与推理 | 没有记忆、没有循环、只会出文本 |
| **Scaffolding** | 模型"看到"的一切 | 系统提示词、工具描述（JSON Schema）、输出格式约束、上下文示例 | 决定模型"认为自己是什么、有什么能力" |
| **Harness** | 让模型"跑起来"的循环引擎 | 调用模型的代码、工具请求分发、工具结果回填、停止条件判断、重试与容错 | 决定系统"会不会动、怎么动、何时停" |

公式：**Agent = Model + Scaffolding + Harness**

> 💡 社区日常简化说法是 **Agent = Model + Harness**——Harness 被当成"除了模型以外的一切"（Scaffolding 只是 Harness 的一部分）。这个简化的价值：**同一模型，换一套 Harness，就是完全不同的 Agent 产品**——这正是"模型同质化时代，Agent 差异化在工程"的原因。

## 3. Agent 的本质：运行范式，不是产品

2026 年对"Agent 是什么"的一个重要澄清：**Agent 不是一个具体产品或一种算法，而是一种"运行范式"**。

| 视角 | 错误理解 | 正确理解 |
|---|---|---|
| 产品视角 | "XX 软件就是 Agent" | Agent 是形态描述——任何软件内部可以跑 Agent 范式 |
| 算法视角 | Agent = 某种新模型 | 模型没有变，变的是"系统如何用模型" |
| 交互视角 | Agent = 更聪明的 Chatbot | Chatbot 是"一次问答"，Agent 是"目标驱动的多步闭环" |
| 商业视角 | 用了大模型就叫 Agent | **Agent 的硬指标是自主闭环**——自己决定下一步，不是每步等人点击 |

核心区分（面试必答）：普通大模型应用是 **"输入一次 → 生成一次"**；Agent 是 **"目标驱动 → 多步行动 → 持续反馈 → 结果交付"** 的自主闭环。

## 4. 两个常见误区

| 误区 | 澄清 |
|---|---|
| "用了 LLM 的程序都是 Agent" | 单纯 RAG 问答机器人**没有自主循环，不算 Agent**——它是"检索→生成"的固定流程（Workflow），步骤由代码预定义 |
| "能多轮对话就是 Agent" | 多轮对话只是有状态；Agent 必须**能决定调用什么工具、在环境反馈基础上继续行动** |

> ⚠️ 2026 年 Gartner 专门警告 **agentwashing（Agent 洗白）**：把本质上依赖人工的 AI 助手包装成"自主 Agent"的市场夸大行为。面试被问"如何看待市面上一堆 Agent 产品"时，用"自主闭环 + 动态控制"两个硬指标来筛，就是加分回答。

## 5. 面试速记

| 问题 | 一句话答案 |
|---|---|
| 什么是 LLM Agent？ | 以 LLM 为认知核心，通过工具连接外部世界，在感知-思考-行动-观察的自主闭环中推进目标直至完成的系统 |
| 最精确的三层拆解？ | Agent = Model + Scaffolding + Harness |
| 社区简化公式？ | Agent = Model + Harness（Harness = 除模型以外的一切） |
| Model/Scaffolding/Harness 各是什么？ | 裸模型 / 模型看到的一切（提示词、工具描述、格式）/ 循环引擎（调用、分发、停止） |
| Agent 的本质？ | 一种运行范式，不是产品、不是算法 |
| Agent 的硬指标？ | 自主闭环（自己决定下一步）+ 动态控制（模型决定流程与工具） |
| 单纯 RAG 问答机器人是 Agent 吗？ | 不是——无自主循环，是固定流程（Workflow） |
| 什么是 agentwashing？ | 把人工依赖型助手包装成自主 Agent 的市场夸大（Gartner） |
| 与 Chatbot 的本质区别？ | Chatbot 一次问答；Agent 目标驱动的多步行动闭环 |

---

**下一模块**：[02-Agent 公式：模型+工具+循环](02-Agent公式：模型+工具+循环.md)　**返回总览**：[00-什么是 LLM Agent 总览](00-什么是LLM%20Agent总览.md)

## 参考来源

- [LLM Agent: Definition, Examples & FutureAGI Guide (2026)](https://futureagi.com/glossary/llm-agent/)
- [AI Agent 到底是什么？剥开营销外衣，看清它的骨架与血肉（阿里云开发者）](https://developer.aliyun.com/article/1752314)
- [一文看懂 AI Agent 的 13 大概念：涵盖 Harness、Scaffold、Tool 和 Skill 等（智东西）](https://zhidx.com/p/562998.html)
- [Agent Harness, Scaffold, Loop, Skill: 2026 Glossary](https://clawvard.school/blog/agent-harness-scaffold-glossary)
- [1 What are LLM Agents and Multi-Agent Systems? · Build a Multi-Agent System (from Scratch)（Manning）](https://livebook.manning.com/book/build-a-multi-agent-system-from-scratch/chapter-1/v-2/)
- [Gartner Predicts 40% of Enterprise Apps Will Feature Task-Specific AI Agents by 2026](https://www.gartner.com/en/newsroom/press-releases/2025-08-26-gartner-predicts-40-percent-of-enterprise-apps-will-feature-task-specific-ai-agents-by-2026-up-from-less-than-5-percent-in-2025)
