# 01 Agent 定义与边界：Task / Workflow / Agent 三分法

> 定位：一切 Agent 学习的起点——2026 年共识定义、判定测试、Anthropic 三分法（Task/Workflow/Agent）与"不要什么都做成 Agent"（2026-08 基准）

## 📚 目录

1. [2026 共识定义](#1-2026-共识定义)
2. [判定测试：Agent 与 Workflow 的分界](#2-判定测试agent-与-workflow-的分界)
3. [Anthropic 三分法：Task / Workflow / Agent](#3-anthropic-三分法task--workflow--agent)
4. [为什么"不要什么都做成 Agent"](#4-为什么不要什么都做成-agent)
5. [2025-2026 生产现实](#5-2025-2026-生产现实)
6. [混合形态：高层 Agent + 确定性模块](#6-混合形态高层-agent--确定性模块)
7. [核心要点](#7-核心要点)

## 1. 2026 共识定义

```text
AI Agent = 处在循环中的 LLM，配工具与记忆，由模型决定下一步

关键拆分：
├─ LLM（模型）   = 决策者：基于当前状态选下一步
├─ 循环（Loop）   = 机制：推理→行动→观察→再推理
├─ 工具（Tools）  = 行动能力：数据库/API/代码执行/搜索
├─ 记忆（Memory） = 状态：进度/结果/剩余任务
└─ 模型决策       = 判据：下一步是模型选的，不是代码硬编码的
```

> 🎯 **一句话**：Agent 的核心不是"智能"而是"**循环**"——"intelligence lives in the loop"（智能活在循环里）：没有单一工具规划整个任务，是循环本身在承担智能。

## 2. 判定测试：Agent 与 Workflow 的分界

```text
判定测试：下一步由谁决定？
├─ 模型基于观察结果决定下一步 → Agent
└─ 下一步被代码硬编码（预定义路径）→ Workflow

反例说明：
检索 → 生成 → 摘要（线性链）           = Workflow（路径固定）
退款分诊（按观察分支：查单/退款/升级）  = Agent（路径由模型定）
```

| 维度 | Workflow | Agent |
|------|---------|-------|
| 控制流 | 预定义代码路径 | 模型自主决定 |
| 谁拥有管线 | 人（human owns the plumbing） | 模型（model owns the plumbing） |
| 确定性 | 高（可预测可审计） | 低（非确定） |
| 成本 | 低（无探索） | 高（探索昂贵） |
| 适合 | 可预映射决策树的任务 | 无法预映射路径的开放式任务 |

> 💡 **Anthropic 判据**："如果任务的不确定性允许你预映射决策树，就做成 Workflow——比任何 Agent 都更准、更可控、更便宜。"

## 3. Anthropic 三分法：Task / Workflow / Agent

Anthropic 2024-12 提出、2026 延续的三层分类（Barry Zhang，AI Engineer Summit）：

| 层级 | 定义 | 模型调用 | 例子 |
|------|------|:---:|------|
| Task | 单次模型调用 | 1 | 单次翻译、单次分类 |
| Workflow | 预定义控制流中的多次调用 | 2-N | 检索→生成→摘要、路由分发 |
| Agent | 模型用工具循环、自主轨迹 | 不定 | 退款分诊、代码修复、调研 |

```text
系统设计第一决策：我的需求是 Task、Workflow 还是 Agent？
→ 90% 的生产需求是 Task/Workflow（确定性、可审计、便宜、可控）
→ Agent 只留给"路径无法预映射"的开放式问题
```

**Anthropic 的 Agent 使用四条件**（缺一不用）：
1. 任务模糊到无法预映射决策树
2. 价值足以覆盖 token 消耗（Agent 探索 = 昂贵）
3. 瓶颈能力扎实（错误会在循环中累积放大）
4. 错误成本可控

## 4. 为什么"不要什么都做成 Agent"

| 理由 | 说明 |
|------|------|
| 成本 | Agent 探索（试错路径）成本数倍于确定性流程 |
| 可靠性 | 循环中错误累积：单步 90% 准确 × 10 步 ≈ 35% |
| 可审计性 | 非确定性轨迹难以审计与回放 |
| 可测试性 | 预定义路径可穷举测试，Agent 路径不可穷举 |
| 延迟 | 多轮循环延迟显著高于单/少次调用 |

> 🎯 **行业结论（2026）**："Don't build agents for everything"（Anthropic 第一法则）——生产系统设计从 Task 开始，复杂度不够就不升级 Agent。

## 5. 2025-2026 生产现实

| 事实 | 数据/结论 |
|------|----------|
| 生产之战结果 | **Workflow 赢了 2025**——全自主 Agent 仍限于窄领域探索 |
| 企业多 Agent 失败率 | 41-86.7%（主要为规格不清与协调缺失，非模型能力） |
| 观测数据 | 89% 团队有可观测，仅 52% 有 evals |
| 演进路径 | Anthropic：workflow → single agent → agent workflows → multi-agent systems |
| 多 Agent 成本证据 | Claude Research coordinator-executor：性能 +90.2%，token ×15 |

> ⚠️ **启示**：别被"全自主多 Agent"叙事带偏——生产主线是 Workflow + 局部 Agent 化；多 Agent 是演进路径的终点而非起点。

## 6. 混合形态：高层 Agent + 确定性模块

2026 成熟系统的模式：

```text
┌─────────────────────────────────────┐
│ 高层 Agent（定目标/编排/异常处理）     │
│     │ 委派                        │
│     ▼                             │
│ 确定性模块（关键计算/成熟流程）       │
│    · 支付校验（规则引擎）            │
│    · 数据管道（代码路径）            │
│    · 审批流（固定流程）              │
└─────────────────────────────────────┘

原则：Workflows don't replace agent autonomy;
      they shape where and how agents apply it.
（Workflow 不取代 Agent 自主性，而是塑造它在哪用、怎么用）
```

> 💡 **设计心智**：把系统拆成"确定性骨架 + 自主性局部"——每个环节先问"这段路径能预映射吗"：能 → 确定性代码；不能 → Agent。

## 7. 核心要点

> 🎯 **核心要点**：
> 1. Agent = 循环中的 LLM + 工具 + 记忆，**模型决定下一步**——智能活在循环里
> 2. 判定测试：下一步是模型选的还是代码硬编码的——硬编码 = Workflow
> 3. Anthropic 三分法：Task（1 次调用）/ Workflow（预定义多次）/ Agent（自主循环）——设计第一决策
> 4. "Don't build agents for everything"：90% 需求是 Workflow；Agent 四条件缺一不用
> 5. 2025 生产之战 Workflow 赢了；成熟系统 = 高层 Agent + 确定性模块的混合形态

---

**上一模块**：[00 总览](00-总览：Agent%20内核理论知识体系.md)　**下一模块**：[02 Agent 循环深潜](02-Agent%20循环深潜：五阶段循环与五要素.md)　**返回总览**：[00 总览](00-总览：Agent%20内核理论知识体系.md)

## 【参考来源】

- [How Anthropic Thinks About Agents, Workflows, and Tasks (2026)](https://shellypalmer.com/2026/04/how-anthropic-thinks-about-agents-workflows-and-tasks/)
- [Common workflow patterns for AI agents (Anthropic)](https://claude.com/blog/common-workflow-patterns-for-ai-agents-and-when-to-use-them)
- [Not Everything Should Be an Agent (go-micro)](https://go-micro.dev/blog/18)
- [What Is an AI Agent Loop? (FutureAGI)](https://futureagi.com/blog/loop-engineering/what-is-ai-agent-loop/)
- [The Current State of Agentic AI (MachineLearningMastery)](https://machinelearningmastery.com/the-current-state-of-agentic-ai/)
- [Agentic Workflow vs. Autonomous Agent (MachineLearningMastery)](https://machinelearningmastery.com/agentic-workflow-vs-autonomous-agent-whats-the-difference/)
- [Agentic Workflow 与 Agent 选型踩坑实录（阿里云开发者）](https://developer.aliyun.com/article/1747461)
- [The AI Agents Stack (2026 Edition) (O'Reilly Radar)](https://www.oreilly.com/radar/the-ai-agents-stack-2026-edition/)
