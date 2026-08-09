# 核心循环：ReAct 与 Loop 工程

> 理论篇：Agent 的"心跳"——ReAct 循环（Thought-Action-Observation）的机制与终止条件，以及 2026 年成为行业共识的 **Loop 工程**（Claude Code 四种 loop 原语、从 Prompt 工程到 Loop 工程）。

## 1. 循环的本质

Agent 与普通应用的本质差异就在循环：**模型 → 判断 → 行动（调工具）→ 观察结果 → 再判断……直到完成**。最简形态一句话：**"一个 LLM 在 while 循环里调用工具，直到任务完成或停止条件触发。"**

```text
        ┌─────────────────────────────────────┐
        │                                     ▼
  用户目标 ──► LLM 推理（Thought）──► 工具调用（Action）
        ▲                                     │
        │             观察回填（Observation） ◄┘
        │                                     │
        └──（目标已达成？→ 输出最终答案并终止）◄─┘
```

> 🎯 核心要点：**循环的价值 = 每步都能拿到环境 ground truth（工具返回/代码执行结果）来校准下一步**——这是"自主行动"与"一次性生成"的分水岭。

## 2. ReAct：循环的经典范式（2022）

ReAct（Reason + Act，Yao 等，2022）是 Agent 循环的事实标准，精华在三个 token：

| Token | 含义 | 作用 |
|---|---|---|
| **Thought（思考）** | 分析当前状态，决定下一步做什么 | 推理痕迹（可解释、可审计） |
| **Action（动作）** | 从工具列表选择工具并给出参数 | 结构化工具调用（Function Calling） |
| **Observation（观察）** | 工具执行后的返回结果 | 环境反馈，供下一轮 Thought 使用 |

两个方向（面试必背）：

| 方向 | 含义 | 例子 |
|---|---|---|
| **Reason to Act** | 先想后做——推理指导行动 | "用户要上海天气，我需要查天气 API" |
| **Act to Reason** | 做了才知道——行动结果反哺推理 | "API 返回 404，说明接口变了，换城市天气接口" |

> ⚠️ 经典失败案例（面试谈 Agent 风险必引）：某团队四个 LangChain Agent 循环跑了 11 天，账单 **$47,000**——**ReAct 无限循环的真实代价**。终止条件是循环工程的第一要务。

## 3. 终止条件：循环工程的"刹车"

| 终止条件 | 机制 | 典型配置 |
|---|---|---|
| 目标达成检查 | 模型自评"任务已完成"或结构校验通过 | 输出 final answer 的专用工具/格式 |
| 最大迭代上限 | 硬性轮数限制（最可靠） | 通常 8-12 轮（社区经验） |
| Token/成本预算 | 累计消耗到阈值即停 | 按轮次 × 全量上下文的预估预算 |
| 无进展检测 | 连续 N 轮输出无变化/重复同一工具 | 检测循环模式（looping patterns） |
| 时间上限 | 墙上时钟到点即停 | 长任务兜底 |
| 人工介入 | 检查点（checkpoint）征求人类反馈 | 高影响操作必须人工确认 |

> 💡 生产级循环必须**多层终止条件叠加**——目标达成 + 迭代上限 + 成本预算三件套是基线；高影响动作再叠人工确认。

## 4. Loop 工程：2026 年的行业共识

2026 年 5-6 月，"Loop 工程"从概念华丽转身为主流共识：Google 工程负责人 **Addy Osmani** 系统性提出 Loop 工程概念；**Anthropic 公开了 Claude Code 的四种 loop 原语**：

| 原语 | 驱动方式 | 典型场景 |
|---|---|---|
| **Turn-based（回合制）** | 每发一条消息回一条 | 普通对话、问答助手 |
| **Goal-based（目标驱动）** | 给定目标后自己写、测、改直到完成 | 编程 Agent（Claude Code 核心模式） |
| **Time-based（定时触发）** | 按时间自动醒来干活 | 每两小时检查 PR、定时巡检 |
| **Proactive（主动式）** | 自己发现问题、自己开干 | 监控型 Agent（发现异常→自动处理） |

> 🎯 从 Prompt 工程到 Loop 工程（2026 最重要的观念转变）：过去强调"提示词写得越细结果越像样"；2026 共识是——**"你不该再给 Coding Agent 写提示词了，你应该设计 Loop"**。人不再负责每一步怎么问模型，而是设计让模型持续工作的机制：目标怎么给、反馈怎么闭环、边界怎么设、何时该停。

| 时代 | 关注点 | 人负责什么 |
|---|---|---|
| Prompt 工程（2023-25） | 让模型"答得对" | 精心设计每次提问 |
| Loop 工程（2026-） | 让系统"持续干活" | 设计循环机制、反馈闭环、停止边界 |

## 5. 循环的工程实现要点

| 要点 | 细节 |
|---|---|
| 消息角色严格交替 | User → Assistant → Tool → ...（提供商校验，违反直接报错） |
| 工具结果回填 | 每轮观察结果作为 Tool 角色消息送回上下文 |
| 上下文管理 | 轮次多 → 全量上下文爆炸 → 压缩/截断/记忆外置（详见 05 篇） |
| 错误回传 | 工具执行失败 → 把错误信息转 JSON 回传模型，让其自行修复（消除约 80% 卡死） |
| 可观测 | 每轮留痕（thought/action/observation 落日志），事件系统支撑追踪 |
| 双超时 | 单轮 LLM 调用超时 + 整循环总超时 |

## 6. 面试速记

| 问题 | 一句话答案 |
|---|---|
| Agent 循环是什么？ | 模型→思考→调工具→观察结果→再决策，直到完成或终止（LLM 在 while 循环里调工具） |
| ReAct 三个 token？ | Thought（思考）/Action（动作）/Observation（观察） |
| Reason to Act 与 Act to Reason？ | 推理指导行动；行动结果反哺推理 |
| 终止条件有哪些？ | 目标达成 / 最大迭代 / 成本预算 / 无进展 / 时间上限 / 人工介入 |
| 为什么终止条件重要？ | ReAct 无限循环 11 天烧 $47K 的真实案例 |
| Claude Code 四种 loop 原语？ | turn-based / goal-based / time-based / proactive |
| Loop 工程是什么？ | 设计让模型持续工作的机制（目标、反馈、边界、停止）——2026 共识 |
| 从 Prompt 工程到 Loop 工程？ | 不写提示词，设计循环——人负责机制，不负责每步提问 |

---

**下一模块**：[04-Agent 的边界：什么不算 Agent](04-Agent的边界：什么不算Agent.md)　**返回总览**：[00-什么是 LLM Agent 总览](00-什么是LLM%20Agent总览.md)

## 参考来源

- [What Is the AI Agent Loop? The Core Architecture Behind Autonomous AI Systems（Oracle）](https://blogs.oracle.com/developers/what-is-the-ai-agent-loop-the-core-architecture-behind-autonomous-ai-systems)
- [全球 Agent 都在卷的「Loop 工程」：AI 自己干活、监工和返工（OFweek）](https://m.ofweek.com/ai/2026-07/ART-201717-8420-30693072.html)
- [Building effective agents（Anthropic 官方工程博客）](https://www.anthropic.com/engineering/building-effective-agents)
- [LLM Agent: Definition, Examples & FutureAGI Guide (2026)](https://futureagi.com/glossary/llm-agent/)
- [AI Agent 到底是什么？剥开营销外衣，看清它的骨架与血肉（阿里云开发者）](https://developer.aliyun.com/article/1752314)
