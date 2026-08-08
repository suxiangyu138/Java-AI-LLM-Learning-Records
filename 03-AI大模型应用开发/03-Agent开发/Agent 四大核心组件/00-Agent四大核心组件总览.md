# Agent 四大核心组件知识体系总览

> 定位：Agent 的"解剖学"——把 Agent 拆成四大核心组件：LLM 大脑（推理决策）、规划反思、记忆、工具行动。与"范式"（行为模式）和"循环"（动态过程）互补，本体系讲"一个 Agent 由什么构成、组件之间怎么协作"。

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)
5. [2026 生态基准](#5-2026-生态基准)

## 1. 知识体系导图

```text
Agent 四大核心组件（解剖学视角）
├── 01 四大组件全景：Agent 解剖学        组件定义 / 协作循环 / 与范式-循环的分工
├── 02 LLM 大脑：推理决策核心            模型选型 / 推理能力 / 决策风格
├── 03 规划组件：任务分解与行动编排       Plan-and-Execute / 分解质量 / ReWOO
├── 04 反思组件：自我评估与纠错           Reflexion / oracle 条件 / act-then-reflect
├── 05 记忆组件：短中长时记忆架构         工作记忆 / 长期记忆 / 写入与召回
├── 06 工具组件：Function Calling 与注册   工具定义 / 注册表 / 校验 / MCP
├── 07 行动组件：执行与反馈闭环           执行器 / 观察反馈 / 重试终止 / 护栏
├── 08 四大组件协作循环：完整 Agent 运行时  ReAct 中的协同 / 任务走查 / 状态管理
├── 09 组件化实现：框架中的四大组件         LangChain / LangGraph / Spring AI / 自研
└── 10 生产实践与面试冲刺                 组件监控 / 预算 / 12 避坑 / 面试题
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|---|---|---|---|
| 01 | [四大组件全景：Agent 解剖学](01-四大组件全景：Agent解剖学.md) | 四组件定义、协作循环、体系分工 | 全部（地基） |
| 02 | [LLM 大脑：推理决策核心](02-LLM大脑：推理决策核心.md) | 模型选型、推理模型、决策风格 | 全部（地基） |
| 03 | [规划组件：任务分解与行动编排](03-规划组件：任务分解与行动编排.md) | Plan-and-Execute、分解质量、ReWOO | Agent 工程师 |
| 04 | [反思组件：自我评估与纠错](04-反思组件：自我评估与纠错.md) | Reflexion、oracle 条件、评判器 | Agent 工程师 |
| 05 | [记忆组件：短中长时记忆架构](05-记忆组件：短中长时记忆架构.md) | 工作记忆、长期记忆、写入召回 | Agent 工程师 |
| 06 | [工具组件：Function Calling 与工具注册](06-工具组件：Function-Calling与工具注册.md) | 工具定义、注册表、校验、MCP | 落地开发者 |
| 07 | [行动组件：执行与反馈闭环](07-行动组件：执行与反馈闭环.md) | 执行器、观察反馈、重试终止、护栏 | 落地开发者 |
| 08 | [四大组件协作循环：完整 Agent 运行时](08-四大组件协作循环：完整Agent运行时.md) | ReAct 协同、任务走查、状态管理 | 架构师 |
| 09 | [组件化实现：框架中的四大组件](09-组件化实现：框架中的四大组件.md) | LangChain/LangGraph/Spring AI 映射 | 落地开发者 |
| 10 | [生产实践与面试冲刺](10-生产实践与面试冲刺.md) | 组件监控、预算、12 避坑、面试题 | 面试/上线前 |

## 3. 学习路线推荐

| 路线 | 路径 | 目标 |
|---|---|---|
| 入门（1 天） | 01 → 02 → 06 → 10 | 理解四组件构成，能用工具组件搭最小 Agent |
| 进阶（1 周） | 01-02 → 03 → 04 → 08 → 10 | 掌握规划与反思，能设计完整运行时 |
| 高级（2 周） | 全量 + 05 → 07 → 09 | 组件级调优与框架映射，能自研轻量实现 |

## 4. 核心概念速查

| 概念 | 一句话定义 |
|---|---|
| LLM 大脑 | Agent 的推理决策核心（模型+提示词），产出计划/行动/评估 |
| 规划组件 | 把目标分解为可执行步骤（任务分解），Plan-and-Execute 的核心 |
| 反思组件 | 评估执行结果并改进（Reflexion），**需可验证的 oracle 否则变自我确认** |
| 记忆组件 | 跨轮保持信息：工作记忆（上下文）+ 长期记忆（外部存储） |
| 工具组件 | 连接外部世界的接口：Function Calling 声明、注册、执行、错误回传 |
| 行动组件 | 实际执行动作（调工具/改文件/发消息）并收集观察反馈 |
| Plan-and-Execute | 规划与执行分离：规划器（贵模型）出步骤，执行器（便宜模型）干活 |
| Replan | 执行中发现偏离 → 重新规划（观察驱动的动态规划） |
| ReWOO / Blueprint | 一次性规划出工具 DAG 再执行（~2 次 LLM 调用，省 20 倍，不能应变） |
| Reflexion | 无权重更新的自我批评（verbal RL）；HumanEval 80%→91% |
| Act-then-reflect | 先产出再自评改进的反思模式（反思≠重试） |
| Oracle（可验证基准） | 测试集/编译器/标准答案——决定反思是否值得加 |
| 工作记忆 | 当前任务上下文（LLM 上下文窗口），需管理预算 |
| 长期记忆 | 跨会话持久存储（向量库/关系表/文件） |
| Step Budget | 任务步骤/成本上限（防 50-500 次 LLM 调用的失控级联） |

## 5. 2026 生态基准

> 📅 基准窗口：2026-08。版本与事实以各模块【参考来源】为准。

- **四组件共识确立**：2026 多份调查（Redis 架构博客、LLM Agents Survey 2026-07、arXiv 2601.01743）收敛为"规划 + 记忆 + 工具 + 反思/推理"，LLM 为底座；FutureAGI 给出六层模型（模型核心/记忆/工具/规划器/运行时/可观测）。
- **Plan-Execute-Reflect 成生产标配**：OpenSearch ML Commons 注册 Agent 类型（Planner/Executor/Reflect 三角色预制提示词 + memory_id 追踪）；CloudWeGo Eino 官方实现"规划→执行→重规划"循环；Microsoft 给出 plan-then-act / act-then-reflect / iterative refinement 模式表。
- **成本分工成熟**：规划器用前沿模型、执行器用便宜模型——"成本下降、质量不降"；ReWOO/Blueprint 一次规划工具 DAG、~2 次 LLM 调用、便宜约 20 倍，代价是**不能应变**（未知流程仍要 Plan-Execute）。
- **反思的边界共识**：Reflexion 在可验证 oracle（测试集/编译器）下有效（HumanEval 80%→91%）；**没有 oracle 的开放式生成，反思退化为"自我确认偏差"**——2026 年最佳实践是"有 oracle 才加反思"。
- **策略选型矩阵**（2026 实测）：ReAct（4B+ 模型，3-10 轮，快）< Blueprint/ReWOO（~2 调用，静态任务）< Plan-Execute-Reflect（14B+，5-15 轮，观察驱动）< Reflexion（8B+，3-8 轮，准确率关键）< ToT（14B+，5-20 轮，探索性）。
- **记忆产品化**：Mem0/Letta/Zep 成为独立产品（分层记忆 + 记忆管理器，衔接[数据库交互](..%2FAgent%20配套技术生态模块%2F数据库交互%2F00-数据库交互总览.md) 05 篇）；上下文溢出仍列 2026 头号挑战。
- **工程警示**：工具 schema 设计比模型选择更重要（Anthropic 花在工具提示词上的时间多于主提示词）；无预算约束的 Agent 可级联 50-500 次 LLM 调用（$5-50）——**Step Budget 强制**。
- **共识**：四大组件不是四选一，是**一个循环里的四个角色**——大脑决策、规划分解、记忆提供上下文、工具执行，反思在每轮后校准。

---

**下一模块**：[01-四大组件全景：Agent 解剖学](01-四大组件全景：Agent解剖学.md)

## 参考来源

- [LLM Agent Architectures in 2026: Core Components and Patterns（FutureAGI）](https://futureagi.com/blog/llm-agent-architectures-core-components/)
- [AI Agent Systems: Architectures, Applications, and Evaluation（arXiv 2601.01743）](https://arxiv-org.ezproxy.obspm.fr/abs/2601.01743)
- [LLM Agents: A Survey（2026-07，preprints.org）](https://www.preprints.org/manuscript/202608.0265)
- [Agentic AI architecture examples（Redis Blog）](https://redis.io/blog/agentic-ai-architecture-examples.md)
- [Plan-execute-reflect agents（OpenSearch Docs）](https://docs.opensearch.org/latest/ml-commons-plugin/agents-tools/agents/plan-execute-reflect/)
- [Plan-Execute Agent（CloudWeGo Eino）](https://www.cloudwego.io/docs/eino/core_modules/eino_adk/agent_implementation/plan_execute/)
- [Choosing a Reasoning Strategy（Reactive Agents）](https://docs.reactiveagents.dev/guides/choosing-strategies/)
- [实现智能体反思与计划周期（Microsoft Learn）](https://learn.microsoft.com/zh-cn/training/modules/aaai-design-agentic-loops-azure-ai-agent-service/4-implement-agent-reflection-planning-cycles)
- [Efficient Tool Use in LLM Agents: A Survey（HKUST 2026）](https://cse.hkust.edu.hk/pg/defenses/Summer26/jyucm-02-06-2026.html)
