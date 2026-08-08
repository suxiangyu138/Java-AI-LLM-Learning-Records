# 09 模型原生智能转向：native reasoning 与 test-time compute

> 定位：前沿理论——2026 年 Agent 智能的理论转向：从外部编排工作流到模型原生 agentic（native reasoning、test-time compute、RL 驱动）、in-the-flow 优化与"开发智能而非应用智能"（2026-08 基准）

## 📚 目录

1. [理论转向：从工程智能到模型原生](#1-理论转向从工程智能到模型原生)
2. [Native reasoning：推理成为模型原生能力](#2-native-reasoning推理成为模型原生能力)
3. [Test-time compute：测试时计算内化](#3-test-time-compute测试时计算内化)
4. [RL 驱动：LLM + RL + Task](#4-rl-驱动llm--rl--task)
5. [In-the-flow 优化：循环内学习](#5-in-the-flow-优化循环内学习)
6. [对外部框架的影响](#6-对外部框架的影响)
7. [工程视角：仍要学的原理](#7-工程视角仍要学的原理)
8. [核心要点](#8-核心要点)

## 1. 理论转向：从工程智能到模型原生

```text
2026 理论主线（ICIS2026 等共识）：
旧：外部编排工作流 —— 规划/工具/记忆由外部代码协调
    "engineering workflows that apply intelligence"
新：模型原生 agentic —— 推理/规划/工具使用/记忆内化于基础模型
    "building models that develop intelligence through experience"

推动力：强化学习（RL）—— 从静态模仿到结果驱动学习
统一视角：LLM + RL + Task
```

> 🎯 **一句话**：2026 的理论转向 = 智能从"工程脚手架"迁移到"模型本身"——外部框架的角色从"模拟认知"收缩为"路由/状态/环境执行"。

## 2. Native reasoning：推理成为模型原生能力

| 维度 | 说明 |
|------|------|
| 机制 | 模型原生生成隐藏推理 token（thinking 通道） |
| 对比旧法 | Prompt 式 "Let's think step by step" 是 2022 workaround |
| 2026 模型 | GPT-5.x / Claude Opus 4.7 / Gemini 3 Pro / MAI-Thinking-1 |
| 内容通道 | thinking（内部）/ text（用户）/ toolCall（动作）分离 |
| 深度可调 | off → xhigh（按任务难度配） |

```text
Native reasoning 的内部工作流（以 MAI-Thinking-1 为例）：
读指令/上下文 → 内部思考块识别子问题 → 决定是否用工具并规划
→ 调工具收输出更新内部状态 → 迭代到完成 → 输出干净最终答案
（只有最终答案到用户；思考与工具轨迹用于调试/评估/安全）
```

## 3. Test-time compute：测试时计算内化

| 能力 | 说明 |
|------|------|
| 隐藏推理 | 模型生成多步内部推理（不占用户可见上下文） |
| 分支探索 | 内部探索多个解路径（隐式 ToT） |
| 自纠正 | 生成后自校验修正 |
| 含义 | 外部"强制分步思考"脚手架（Plan-and-Execute 循环等）冗余化 |

> 💡 **成本结构变化**：测试时计算 = 推理 token 计费——成本从"循环轮数"部分转移到"每轮推理深度"；总成本权衡仍是"少轮 × 贵轮 vs 多轮 × 廉轮"。

## 4. RL 驱动：LLM + RL + Task

| 维度 | 说明 |
|------|------|
| 传统 | 监督模仿（静态行为复制） |
| 2026 | 强化学习：结果驱动（outcome-driven）——从执行反馈学习 |
| 统一视角 | LLM + RL + Task：模型通过经验发展智能 |
| Agentic RL | 基于 Agent 实际执行反馈持续迭代（阿里云百炼配套技术栈同思路） |

```text
RL 驱动的意义：
模型不只是"会模仿"——而是在任务循环中通过奖惩信号优化策略
→ "从应用智能到开发智能"的路径
```

## 5. In-the-flow 优化：循环内学习

| 维度 | 事实 |
|------|------|
| 代表 | AgentFlow（ICLR 2026） |
| 机制 | 协调 planner/executor/verifier/generator 四模块 + 进化记忆；**循环内优化 planner** |
| 算法 | Flow-GRPO：把多轮优化转为可解的单轮策略更新（稀疏奖励信用分配） |
| 结果 | 7B 规模超过 GPT-4o；搜索/agentic/数学基准 +14-15% |

> 💡 **研究含义**：训练与推理的界限模糊——多轮循环中的执行反馈直接优化策略（在线学习），不再"训练完就冻结"。

## 6. 对外部框架的影响

| 旧职责 | 2026 走向 |
|--------|----------|
| 强制分步思考（LangChain PnE/Reflexion 循环） | 被原生推理吸收，冗余化 |
| 显式 ToT 分支 | 被隐式分支（test-time compute）替代 |
| 规划模拟 | 收缩到"重规划/可审计"场景 |
| 路由/状态/环境执行 | **保留且加强**（Harness 增厚） |

> 🎯 **框架角色的 2026 定位**："orchestration layer's role is shifting to routing, state management, and environment execution rather than simulating cognition"——框架不再模拟认知，而是管路由/状态/环境。

## 7. 工程视角：仍要学的原理

```text
理论转向 ≠ 工程原理失效：
① 循环结构不变：原生推理改变每轮质量，不改变 Reason→Act→Observe 结构
② 终止工程更重要：推理更深的模型 = 每轮更贵 → max_turns 成本压力更大
③ 记忆/压缩不变：窗口仍是限制资源（上下文工程 = 绑定约束）
④ 工具层不变：MCP/工具设计仍是 Agent 能力的上限
⑤ 评估升级：思考轨迹可评估（ReasoningQuality 等，10 篇）

学习建议：理解理论转向（为什么框架变薄），
         但工程技能（循环/终止/记忆/工具/Harness）不贬值
```

## 8. 核心要点

> 🎯 **核心要点**：
> 1. 理论转向：从"外部编排工作流"到"模型原生 agentic"——LLM + RL + Task
> 2. Native reasoning：推理独立通道（thinking），prompt 式 Thought token 成历史
> 3. Test-time compute：隐式分支/自纠正内化——外部分步思考脚手架冗余化
> 4. In-the-flow（AgentFlow/Flow-GRPO）：循环内优化策略，7B 超 GPT-4o（研究前沿）
> 5. 工程视角：循环/终止/记忆/工具原理不贬值——框架从"模拟认知"转向"管路由/状态/环境"

---

**上一模块**：[08 Harness 与生产运行时](08-Harness%20与生产运行时：状态%20压缩%20追踪%20护栏.md)　**下一模块**：[10 面试与自测](10-面试与自测.md)　**返回总览**：[00 总览](00-总览：Agent%20内核理论知识体系.md)

## 【参考来源】

- [The Current State of Agentic AI (MachineLearningMastery)](https://machinelearningmastery.com/the-current-state-of-agentic-ai/)
- [In-the-Flow Agentic System Optimization for Effective Planning and Tool Use (ICLR 2026)](https://mlanthology.org/iclr/2026/li2026iclr-intheflow/)
- [Do Agents Think Deeper? Mechanistic Investigation of Sequential Planning (ACM)](https://dl.acm.org/doi/10.1007/978-981-92-3403-5_12)
- [Reward-Driven LLM Agent Workflows: POMDP Routing and Self-Correction (arXiv)](https://arxiv-org.ezproxy.obspm.fr/html/2607.17038v1)
- [ICIS2026: Jitao Sang 特邀报告（模型原生转向）](http://www.intsci.ac.cn/ICIS/Speakers/Invited/202601/t20260130_822804.html)
- [MAI-Thinking-1 + Mem0: Long-Term Memory for Reasoning Models (Mem0)](https://mem0.ai/blog/how-mai-thinking-1-works)
- [The AI Agents Stack (2026 Edition) (O'Reilly Radar)](https://www.oreilly.com/radar/the-ai-agents-stack-2026-edition/)
