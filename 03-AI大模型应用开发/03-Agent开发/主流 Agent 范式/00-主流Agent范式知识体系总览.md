# 主流Agent范式知识体系总览
> Agent 构建的完整范式地图：工作流 vs Agent、五大工作流模式、ReAct/Plan-and-Execute/Reflexion、Agentic Reasoning 与 Model-Native Harness（截至 2026-08）。

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)

---

## 1. 知识体系导图

```
主流 Agent 范式
│
├── 01-范式总览：工作流 vs Agent
│     Anthropic 分类 → 何时用 Agent → 可靠性复合
│
├── 02-五大工作流模式
│     Prompt Chaining → Routing → Parallelization → Orchestrator → Evaluator
│
├── 03-ReAct 范式
│     Thought-Action-Observation → 实现 → 优劣 → 适用边界
│
├── 04-Plan-and-Execute 范式
│     Planner/Executor → 混合变体 → 成本分析 → 分水岭
│
├── 05-反思与自我改进范式
│     Reflexion → Self-Refine → Tree of Thoughts → 验证器
│
├── 06-Agentic Reasoning 与模型原生范式
│     o1/R1/Thinking → Model-Native Harness → 2025-2026 转变
│
├── 07-多Agent协作范式
│     Orchestrator-Workers → Debate → A2A → 团队模式
│
├── 08-范式选择与工程实践
│     选择框架 → 安全护栏 → 成本控制 → 生产化
│
└── 09-框架与生态
      LangGraph → CrewAI → OpenAI Agents SDK → 运行时对比
```

模块间依赖：01 为总纲（工作流 vs Agent 的分界）；02-05 为经典范式；06 为 2025-2026 新范式；07 为多 Agent；08 为综合实践；09 为工具生态。与 `Agent开发/`（P-A-M-E 体系）、`Agent与MCP协议/`（MCP 工具协议）、`03-Multi-Agent与MCP协议/` 目录构成 Agent 知识族。

---

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|---------|---------|
| 01 | 范式总览：工作流 vs Agent | Anthropic 分类、毕业条件、可靠性 | 入门 |
| 02 | 五大工作流模式 | 五种确定性编排模式 | 核心必读 |
| 03 | ReAct 范式 | 推理-行动循环、实现、边界 | 核心必读 |
| 04 | Plan-and-Execute | 计划-执行分离、成本、分水岭 | 进阶 |
| 05 | 反思与自我改进 | Reflexion、ToT、验证器 | 进阶 |
| 06 | Agentic Reasoning | o1/R1/Thinking、Model-Native | 高级/前沿 |
| 07 | 多Agent协作 | Orchestrator、Debate、A2A | 进阶 |
| 08 | 范式选择与工程实践 | 选择框架、护栏、成本 | 核心必读 |
| 09 | 框架与生态 | LangGraph/CrewAI/Agents SDK | 进阶 |

---

## 3. 学习路线推荐

**路线 A：Agent 构建入门（1-2 天，推荐给 AI 工程师）**
01 → 02 → 03。掌握"工作流 vs Agent"的分界、五大工作流、ReAct 原理——足以开始构建生产级 Agent。

**路线 B：范式全景（3-4 天）**
路线 A + 04 + 05 + 07。补齐 Plan-and-Execute、反思范式、多 Agent——能按任务特征选择范式。

**路线 C：前沿与生产（面向架构师）**
路线 B + 06 + 08 + 09。Agentic Reasoning 与 Model-Native Harness 转变、生产化护栏、框架选型。

> 💡 与知识库关系：本目录与 `Agent开发/`（P-A-M-E 方法论）、`Agent与MCP协议/`（工具协议）、`03-Multi-Agent与MCP协议/`（深度模块）构成 Agent 知识族；`03-AI大模型应用开发` 的模型层（DeepSeek R1 等推理模型）是范式演进的底层驱动。

---

## 4. 核心概念速查

| 概念 | 一句话定义 | 关键事实（2025-2026） |
|------|-----------|---------------------|
| Workflow | 代码路径编排 LLM（确定性） | 大多数生产"Agent"实为工作流 |
| Agent | LLM 自主决定流程（动态） | 需步骤预算 + 人工检查点 |
| ReAct | Thought→Action→Observation 循环 | ICLR 2023；~20 行实现 |
| Plan-and-Execute | 计划器 + 执行器分离 | 4-5 步为分水岭 |
| Reflexion | 语言反馈自我改进 | HumanEval 91% pass@1 |
| ToT | 树状搜索 + 自评回溯 | Game of 24: 4%→74% |
| Agentic Reasoning | 推理模型内化计划-执行-反思 | o1/R1/Claude Thinking |
| Model-Native Harness | 模型原生编排（2026 方向） | 手写 ReAct 循环被视为 legacy |
| 可靠性复合 | 每步成功率随步数衰减 | 95%/步 → 10 步 60% |
| Orchestrator | 中央 LLM 动态分解委派 | 分解不确定时使用 |
| Evaluator-optimizer | 生成-批评循环 | 有明确标准时使用 |
| MCP | 工具连接标准协议 | Agent 的工具接口 |

> 🎯 **核心要点**：Agent 范式的 2025-2026 主线 = **从"外部编排代码"转向"更强模型内化循环"**——推理模型（o1/R1/Thinking）把计划-执行-反思放进思维链，手写 ReAct 循环逐渐成为 legacy，Model-Native Harness 成为共识。但 Anthropic 的告诫依然有效：**先用最简单的工作流，确定性管道能解决就不要用 Agent**；范式选择的核心是"任务能否预先写成清单"（能 → Plan-and-Execute，不能 → ReAct/Agent）。

---

**下一模块**：[01-范式总览：工作流 vs Agent](01-范式总览：工作流-vs-Agent.md)　**返回上级**：[03-Agent与MCP协议](../)
