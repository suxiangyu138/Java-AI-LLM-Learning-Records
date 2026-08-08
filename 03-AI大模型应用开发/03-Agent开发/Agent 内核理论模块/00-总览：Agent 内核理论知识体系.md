# 00 总览：Agent 内核理论知识体系

> 定位：Agent 学习的**必修原理层**——Agent 是什么、Agent 循环怎么转、为什么这样转：定义边界、循环机制、终止条件、记忆架构、推理范式、模型原生智能（2026-08 基准）

## 📚 目录

1. [本模块的定位](#1-本模块的定位)
2. [知识体系导图](#2-知识体系导图)
3. [模块导航](#3-模块导航)
4. [学习路线推荐](#4-学习路线推荐)
5. [核心概念速查](#5-核心概念速查)
6. [与关联体系的分工](#6-与关联体系的分工)
7. [2026 版本窗口](#7-2026-版本窗口)

## 1. 本模块的定位

```text
本模块（内核理论）    = 原理：Agent 是什么、循环怎么转（必修）
四大核心组件模块      = 组件：工具/记忆/规划/执行 怎么做
主流 Agent 范式模块   = 模式：ReAct/PnE 等范式深潜
Agent 开发框架层      = 工具：框架/平台/多 Agent/新兴 SDK
工程化 & 部署模块     = 生产化：可观测/测试/部署

一句话分工：本模块回答"Agent 的底层机制是什么、为什么这样设计、
            什么时候该用它"——所有上层模块的原理基础
```

> 🎯 **核心价值**：2026 年 Agent 内核理论的共识已清晰——**"Agent = 处在循环中的 LLM，配工具与记忆，由模型决定下一步"**；判定测试是"下一步是否由模型基于观察决定"（硬编码就是 Workflow 不是 Agent）。本模块把循环机制（五阶段/五要素）、终止条件（工程成败关键）、记忆架构、推理范式与"模型原生智能"转向讲透——这是理解一切框架/平台的底层地图。

## 2. 知识体系导图

```text
Agent 内核理论知识体系
│
├── 00 总览（本文件）
│
├── 定义篇
│   └── 01 Agent 定义与边界 ── Task / Workflow / Agent 三分法 / 判定测试
│
├── 机制篇
│   ├── 02 Agent 循环深潜 ── 五阶段循环 / 五要素 / ReAct 本质
│   ├── 03 终止条件与循环控制 ── max_turns / 防御纵深 / early-stopping
│   ├── 04 核心组件四件套 ── Model / Memory / Planner / Tools + Harness
│   └── 05 记忆架构 ── 四层记忆 / 上下文管理 / 压缩
│
├── 范式篇
│   ├── 06 推理范式谱系 ── ReAct / PnE / Reflexion / ToT 等九模式
│   └── 07 Workflow 与 Agent 的边界 ── Anthropic 三分类与六模式
│
├── 运行时篇
│   ├── 08 Harness 与生产运行时 ── 状态/压缩/追踪/护栏
│   └── 09 模型原生智能转向 ── native reasoning / test-time compute / RL
│
└── 检验篇
    └── 10 面试与自测 ── 面试题 / 自测 / 毕业检查单
```

## 3. 模块导航

| 序号 | 模块 | 核心内容 | 场景 |
|:---:|------|---------|------|
| 01 | 定义与边界 | Task/Workflow/Agent 三分法、判定测试 | 先读（建立概念） |
| 02 | 循环深潜 | 五阶段循环、五要素、消息缓冲 | 理解"循环怎么转" |
| 03 | 终止条件 | max_turns、防御纵深、early-stopping | 工程成败关键 |
| 04 | 核心组件 | Model/Memory/Planner/Tools + Harness | 组件职责分工 |
| 05 | 记忆架构 | 四层记忆、上下文压缩 | 记忆设计 |
| 06 | 推理范式谱系 | 九大范式与选型 | 范式地图 |
| 07 | Workflow 边界 | Anthropic 三分类、六模式、何时用 Agent | 系统设计决策 |
| 08 | Harness 运行时 | 状态/压缩/追踪/护栏 | 生产运行时 |
| 09 | 模型原生智能 | native reasoning、test-time compute、RL | 前沿理解 |
| 10 | 面试与自测 | 面试题、自测、毕业检查单 | 求职/自检 |

## 4. 学习路线推荐

**路线 A：快速入门（1 天）**——01 → 02 → 03
> 概念 + 循环 + 终止——三个最小必修，即可进入框架学习。

**路线 B：完整原理（1 周）**——01 → 02 → 03 → 04 → 05 → 06 → 07 → 08
> 原理全链路：定义→机制→组件→范式→运行时。

**路线 C：面试冲刺（2 天）**——10 → 01 → 03 → 06 → 07
> 以题为纲：先会答"什么是 Agent/循环怎么转/怎么防失控"类问题。

## 5. 核心概念速查

| 概念 | 一句话 | 关键事实 |
|------|--------|---------|
| AI Agent | 处在循环中的 LLM，配工具与记忆，由模型决定下一步 | 2026 共识定义 |
| 判定测试 | 下一步是否由模型基于观察决定——硬编码 = Workflow | Agent 与 Workflow 分界 |
| Agent 循环 | Reason → Act → Execute → Observe → Repeat/Stop | 一切 Agent 的底层 |
| 五要素 | 消息缓冲/工具注册表/停止条件/轮次预算/观察格式化 | 缺一 = 聊天机器人 |
| max_turns | 轮次硬上限，最重要的安全控制 | 生产典型 15-25（客服 8-12/研究 20-30） |
| early-stopping | 到上限后无工具再合成一次最优答案 | 防"静默失败" |
| 记忆四层 | Scratchpad / Working / Episodic / Long-term | 层级记忆架构 |
| 压缩 | 上下文管理（摘要/丢弃）是记忆核心 | "多数失败源于缺压缩而非缺向量" |
| ReAct | Reason + Act 交替循环（Yao et al. ICLR 2023） | 一切现代 Agent 的变体 |
| 九大范式 | ReAct/PnE/Reflexion/Self-Ask/ToT/ReWOO 等 | 2026 成熟分类 |
| Task/Workflow/Agent | Anthropic 三分法（2024-12 提出，2026 延续） | 系统设计第一决策 |
| Harness | 循环的运行时外壳：状态/压缩/追踪/护栏 | "智能活在循环里" |
| native reasoning | 推理从 prompt 技巧变为模型原生通道（thinking） | 2026 推理模型转向 |
| test-time compute | 模型原生测试时计算（隐藏推理 token） | 外部 scaffolding 冗余化 |
| 可评估可观测 | 每 (thought,action,observation) 打分 | 89% 可观测 vs 52% evals |

## 6. 与关联体系的分工

| 体系 | 分工 | 本模块怎么用 |
|------|------|------------|
| [四大核心组件模块](../Agent%20四大核心组件/) | 工具/记忆/规划/执行的组件实现 | 本模块给原理，那边给组件怎么做 |
| [主流 Agent 范式](../主流%20Agent%20范式/) | 范式深潜（ReAct/PnE/反思等） | 本模块 06 篇给谱系总览，那边逐范式深潜 |
| [Agent 开发框架层](../Agent%20开发框架层（工程加速）/单%20Agent%20框架/00-总览：单%20Agent%20框架知识体系.md) | 框架/平台实现 | 框架是"循环 + Harness"的实现（02/08 篇对应） |
| [Agent 工程化 & 部署](../Agent%20工程化%20&%20部署模块（从%20demo%20到可用应用）/00-总览：Agent%20工程化与部署模块（从%20demo%20到可用应用）.md) | 可观测/测试/部署 | 08 篇 Harness 是其原理基础 |
| [Function Calling 体系](../../02-大模型基础与Prompt工程/Function%20Calling%20函数调用【Agent%20基石】/00-FunctionCalling知识体系总览.md) | 工具调用协议 | 循环的 Act/Execute 环节基于此 |
| [Agent与MCP协议](../Agent与MCP协议/) | 工具协议 | 工具注册表的 2026 标准形态 |

## 7. 2026 版本窗口

> 本模块以 2026-08 为基准，核心理论事实（详见各篇【参考来源】）：
>
> - **定义共识**：Agent = "LLM in a loop with tools and memory, model picks the next action"；判定测试 = 下一步是否模型基于观察决定
> - **Anthropic 框架**（2024-12 提出，2026 延续）：Task / Workflow / Agent 三分法；"Don't build agents for everything"；六模式（augmented LLM/链式/路由/并行/编排者-工人/评估者-优化者）
> - **2025 生产之战 Workflow 赢了**：全自主多 Agent 企业失败率 41-86.7%（规格不清/协调缺失）；混合形态（高层 Agent + 确定性模块）成为成熟系统模式
> - **终止工程**：max_turns 15-25 典型；防御纵深（多机制重叠）；early-stopping-generate 模式
> - **记忆**：四层架构；"多数 Agent 失败源于缺压缩而非缺向量"；会话内"学习"= 检索非权重更新
> - **原生推理**：2026 推理模型（GPT-5.x/Claude Opus 4.7/Gemini 3 Pro/MAI-Thinking-1）推理走独立通道（thinking/text/toolCall），prompt 式 Thought token 成历史
> - **理论转向**：从外部编排工作流 → 模型原生 agentic（LLM + RL + Task）；test-time compute 内化；in-the-flow 优化（ICLR 2026 AgentFlow）
> - **评估缺口**：89% 团队有可观测 vs 仅 52% 有 evals；新基准 Context-Bench/Recovery-Bench/Terminal-Bench

---

**下一模块**：[01 Agent 定义与边界](01-Agent%20定义与边界：Task%20Workflow%20Agent%20三分法.md)　**返回上级**：[03-Agent开发](../)

## 【参考来源】

- [What Is an AI Agent Loop? The Core Architecture Behind Autonomous Agents (FutureAGI)](https://futureagi.com/blog/loop-engineering/what-is-ai-agent-loop/)
- [How Anthropic Thinks About Agents, Workflows, and Tasks (2026)](https://shellypalmer.com/2026/04/how-anthropic-thinks-about-agents-workflows-and-tasks/)
- [Common workflow patterns for AI agents (Anthropic)](https://claude.com/blog/common-workflow-patterns-for-ai-agents-and-when-to-use-them)
- [The Agent Loop Decoded: Three Levels Every Agent Engineer Must Know (Oracle)](https://blogs.oracle.com/developers/the-agent-loop-decoded-three-levels-every-agent-engineer-must-know)
- [The AI Agents Stack (2026 Edition) (O'Reilly Radar)](https://www.oreilly.com/radar/the-ai-agents-stack-2026-edition/)
- [Not Everything Should Be an Agent (go-micro)](https://go-micro.dev/blog/18)
- [Agent Loop: Definition, Examples & Guide (FutureAGI 2026)](https://futureagi.com/glossary/agent-loop/)
- [The Current State of Agentic AI (MachineLearningMastery)](https://machinelearningmastery.com/the-current-state-of-agentic-ai/)
- [In-the-Flow Agentic System Optimization (ICLR 2026)](https://mlanthology.org/iclr/2026/li2026iclr-intheflow/)
- [What Is ReAct? Definition & Guide (FutureAGI 2026)](https://futureagi.com/glossary/react-pattern/)
