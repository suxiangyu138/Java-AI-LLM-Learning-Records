# 什么是 LLM Agent 总览

> 定位：「LLM AI Agent 相关理论」之**开篇地基**——回答"Agent 到底是什么"：一句话定义 + 三层精确拆解 + 核心循环 + 边界（什么不算 Agent）+ 演进史 + 2026 行业共识 + 分类场景。与 [经典 Agent 范式（理论，面试高频）](..%2F经典%20Agent%20范式（理论，面试高频）%2F00-经典Agent范式理论总览.md)（范式本体）和 [区分概念（面试容易混淆）](..%2F区分概念（面试容易混淆）%2F00-区分概念总览.md)（概念对辨析）分工：本体系讲**定义与本质**。2026 一句话：**"Agent 不是模型更聪明了，而是系统让模型在自主闭环里'干活'"**。

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [Agent 定义一图速记](#3-agent-定义一图速记)
4. [学习路线推荐](#4-学习路线推荐)
5. [2026 生态基准](#5-2026-生态基准)

## 1. 知识体系导图

```text
什么是 LLM Agent（定义地基，面试必背）
├── 01 定义与本质：一句话说清 Agent        一句话定义 / 三层拆解 / 本质是运行范式
├── 02 Agent 公式：模型+工具+循环          为什么缺一不可 / Augmented LLM / 局限
├── 03 核心循环：ReAct 与 Loop 工程         Thought-Action-Observation / 终止条件 / 四原语
├── 04 Agent 的边界：什么不算 Agent          vs Chatbot / Workflow / 程序 / agentwashing
├── 05 四大能力组件：规划-记忆-工具-反思      Lilian Weng 四元组 / 五层结构 / 协作循环
├── 06 定义演进史：从 2022 到 2026          时间线 / Gartner Hype Cycle / Agent 时刻
├── 07 2026 行业共识：官方定义盘点            Anthropic / OpenAI / Gartner / HF / WAIC
├── 08 分类与应用场景：从客服到编码           分类维度 / 三模式 / 场景选择
└── 09 面试高频问答冲刺                      16 题 + 答题范式
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|---|---|---|---|
| 01 | [定义与本质：一句话说清 Agent](01-定义与本质：一句话说清Agent.md) | 一句话定义、三层拆解（Model/Scaffolding/Harness）、本质是运行范式 | 全部（地基） |
| 02 | [Agent 公式：模型+工具+循环](02-Agent公式：模型+工具+循环.md) | 公式拆解、Augmented LLM、同一模型不同 Agent 差异之谜 | 全部（面试必背） |
| 03 | [核心循环：ReAct 与 Loop 工程](03-核心循环：ReAct与Loop工程.md) | Thought-Action-Observation、停止条件、Claude Code 四原语 | Agent 工程师 |
| 04 | [Agent 的边界：什么不算 Agent](04-Agent的边界：什么不算Agent.md) | vs Chatbot/Workflow/程序、agentwashing、何时不需要 Agent | 全部（面试高频） |
| 05 | [四大能力组件：规划-记忆-工具-反思](05-四大能力组件：规划-记忆-工具-反思.md) | Lilian Weng 四元组、五层结构、组件协作 | 全部（组件入门） |
| 06 | [定义演进史：从 2022 到 2026](06-定义演进史：从2022到2026.md) | ReAct→AutoGPT→Agent 时刻、Gartner 五阶段 | 面试/前沿 |
| 07 | [2026 行业共识：官方定义盘点](07-2026行业共识：官方定义盘点.md) | Anthropic/OpenAI/Gartner/HF/WAIC、MCP 标准、Loop 工程 | 面试必背 |
| 08 | [分类与应用场景：从客服到编码](08-分类与应用场景：从客服到编码.md) | 分类维度、单/多 Agent 三模式、场景选择与 40% 预测 | 架构师 |
| 09 | [面试高频问答冲刺](09-面试高频问答冲刺.md) | 16 题 + 答题范式 | 面试前 |

## 3. Agent 定义一图速记

```text
一句话定义：
  Agent = 以 LLM 为认知核心 + 通过工具连接外部世界 + 在"感知—思考—行动—观察"
          的自主闭环中持续推进目标，直到完成或到达终止条件

三层精确拆解（Hugging Face 工程师口径）：
  Model         = 裸 LLM（只会出文本，无记忆、无循环）
  Scaffolding   = 模型"看到"的一切（系统提示词 / 工具描述 / 输出格式）
  Harness       = 让模型"跑起来"的循环引擎（调用模型 / 处理工具请求 / 判断何时停止）
  Agent = Model + Scaffolding + Harness ≈ Model + Harness

硬指标（判断是不是 Agent）：
  ① 自主闭环——自己决定下一步做什么（不是每步等人点击）
  ② 动态控制——LLM 动态决定流程与工具调用（不是代码预定义路径）
  ③ 环境反馈——每步拿到 ground truth（工具返回/执行结果）再决策
  ④ 自主终止——目标达成或停止条件触发，才结束

2026 一句话：价值不在 Benchmark 跑分，而在真实业务流中
  "自主规划 + 工具调用 + 闭环执行"的成功率（WAIC 2026 共识）
```

## 4. 学习路线推荐

| 路线 | 路径 | 目标 |
|---|---|---|
| 面试速成（半天） | 00 → 01 → 04 → 07 → 09 | 能一句话定义、说清边界与共识 |
| 理论深潜（2 天） | 01-03 → 04-05 → 06-07 → 09 | 能拆公式、画循环、讲演进 |
| 工程视角（3 天） | 全量 + 03 重点 | 能判断"什么场景值得做成 Agent" |

## 5. 2026 生态基准

> 📅 基准窗口：2026-08。版本与事实以各模块【参考来源】为准。

- **定义共识收敛**：2026 年"什么是 Agent"的共识已稳定——**以 LLM 为认知核心，通过工具调用连接外部世界，在感知-思考-行动-观察的自主闭环中推进目标直至完成**。更精确的三层拆解（Hugging Face 工程师）：**Agent = Model + Scaffolding + Harness**；社区日常简化为 **Agent = Model + Harness**（Harness = 除模型以外的一切）。
- **Agent 时刻（Gartner）**：Gartner 预测 **2026 年 40% 企业应用将内置任务型 Agent**（2025 年不足 5%）；2035 年 Agentic AI 驱动约 30% 全球企业应用软件收入（超 $4500 亿，2025 年 2%）；2026 CIO 调查仅 17% 组织已部署 Agent，但 60%+ 预计两年内落地——**所有新兴技术中最激进的采纳曲线**。同时 Gartner 发布首个《2026 Hype Cycle for Agentic AI》：Agentic AI 处于**期望膨胀峰值**，完全自主 Agent"对多数企业场景尚未就绪"。
- **官方定义齐备**：Anthropic——"LLM 动态掌控自身流程与工具使用"（vs 工作流=代码预定义路径）；OpenAI——"能智能完成任务、可追求开放目标的系统"（Agents SDK 2025.03）；Gartner——任务型自主 Agent 是助手向"独立执行端到端任务"的进化，并警告 **agentwashing**（把人工依赖型助手包装成 Agent 的市场夸大）。
- **Loop 工程成为共识**：2026 年 5-6 月 Loop 工程从概念转为主流——Google 的 Addy Osmani 系统性提出该概念，Anthropic 公开 Claude Code 四种 loop 原语（**turn-based 回合制 / goal-based 目标驱动 / time-based 定时触发 / proactive 主动式**）；行业共识从"写好提示词"转向"设计让模型持续工作的机制"（Prompt 工程 → Harness/Loop 工程）。
- **MCP 成事实标准**：Anthropic 发起的 Model Context Protocol 通过统一 JSON-RPC 把工具抽象为即插即用资源，解决"AI-工具连接碎片化"；2026 世界人工智能大会（WAIC）共识——衡量 AI 价值的不再是跑分，而是真实业务流中自主规划、工具调用及闭环执行的成功率。
- **与体系分工**：[经典 Agent 范式](..%2F经典%20Agent%20范式（理论，面试高频）%2F00-经典Agent范式理论总览.md) 讲范式本体（ReAct/Plan-Execute/Reflexion 等）；[区分概念](..%2F区分概念（面试容易混淆）%2F00-区分概念总览.md) 讲概念对辨析（LLM vs Chatbot vs Workflow vs Agent 等）；[Agent 四大核心组件](..%2F..%2FAgent%20四大核心组件%2F00-Agent四大核心组件总览.md) 讲组件深潜（LLM 大脑/规划/记忆/工具/反思/行动）；[主流 Agent 范式](..%2F..%2F主流%20Agent%20范式%2F00-主流Agent范式知识体系总览.md) 讲实践/工程/框架视角。本体系讲**定义与本质**——Agent 是什么、边界在哪、业界怎么定义。

---

**下一模块**：[01-定义与本质：一句话说清 Agent](01-定义与本质：一句话说清Agent.md)

## 参考来源

- [LLM Agent: Definition, Examples & FutureAGI Guide (2026)](https://futureagi.com/glossary/llm-agent/)
- [AI Agent 到底是什么？剥开营销外衣，看清它的骨架与血肉（阿里云开发者）](https://developer.aliyun.com/article/1752314)
- [一文看懂 AI Agent 的 13 大概念：涵盖 Harness、Scaffold、Tool 和 Skill 等（智东西）](https://zhidx.com/p/562998.html)
- [Agent Harness, Scaffold, Loop, Skill: 2026 Glossary](https://clawvard.school/blog/agent-harness-scaffold-glossary)
- [全球 Agent 都在卷的「Loop 工程」：AI 自己干活、监工和返工（OFweek）](https://m.ofweek.com/ai/2026-07/ART-201717-8420-30693072.html)
- [Gartner Predicts 40% of Enterprise Apps Will Feature Task-Specific AI Agents by 2026](https://www.gartner.com/en/newsroom/press-releases/2025-08-26-gartner-predicts-40-percent-of-enterprise-apps-will-feature-task-specific-ai-agents-by-2026-up-from-less-than-5-percent-in-2025)
- [2026 Hype Cycle for Agentic AI | Gartner](https://www.gartner.com/en/articles/hype-cycle-for-agentic-ai)
- [Building effective agents（Anthropic 官方工程博客）](https://www.anthropic.com/engineering/building-effective-agents)
- [1 What are LLM Agents and Multi-Agent Systems? · Build a Multi-Agent System (from Scratch)（Manning）](https://livebook.manning.com/book/build-a-multi-agent-system-from-scratch/chapter-1/v-2/)
- [全球 Agent 都在卷的「Loop 工程」（ofweek 中文报道）](https://m.ofweek.com/ai/2026-07/ART-201717-8420-30693072.html)
