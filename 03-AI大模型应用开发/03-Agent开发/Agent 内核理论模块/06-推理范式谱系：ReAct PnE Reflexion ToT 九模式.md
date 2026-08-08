# 06 推理范式谱系：ReAct / PnE / Reflexion / ToT 九模式

> 定位：范式地图——2026 年 Agent 推理范式的九种模式总览与选型：ReAct（默认但不普适）、Plan-and-Execute、Reflexion、Tree-of-Thoughts 等（2026-08 基准）

## 📚 目录

1. [九大范式总表](#1-九大范式总表)
2. [ReAct：默认范式](#2-react默认范式)
3. [规划类：PnE 与 Plan-Execute-Replan](#3-规划类pne-与-plan-execute-replan)
4. [反思类：Reflexion](#4-反思类reflexion)
5. [分解类：Self-Ask 与 ToT](#5-分解类self-ask-与-tot)
6. [执行类：Direct tool-call 与 ReWOO](#6-执行类direct-tool-call-与-rewoo)
7. [2026 前沿：Native reasoning + ReAct](#7-2026-前沿native-reasoning--react)
8. [范式选型决策树](#8-范式选型决策树)
9. [核心要点](#9-核心要点)

## 1. 九大范式总表

| 范式 | 循环形态 | 适合 | 成本 |
|------|---------|------|:---:|
| ReAct | thought → action → observation → thought | 开放式探索任务 | 中 |
| Plan-and-Execute | 一次规划 → 顺序执行 | 可预测工作流 | 低 |
| Reflexion | ReAct + 自批评循环 | 自改进 Agent | 高 |
| Self-Ask | 子问题分解 | 多跳问答 | 中 |
| Direct tool-call | 模型 → 工具 → 回答 | 低风险查询 | 最低 |
| Tree-of-Thoughts | 分支与剪枝推理 | 难题推理 | 极高 |
| Native reasoning + ReAct | 模型原生 CoT + 工具调用 | 2026 前沿组合 | 高 |
| ReWOO | 预规划全部调用 → 统一执行 | 可并行安全工作流 | 低 |
| Plan-Execute-Replan | 规划 → 执行 → 失败重规划 | 长程任务 | 高 |

> 🎯 **一句话**：范式 = 循环形状的九种变体——选型看"任务可预测性 × 成本预算"两个维度。

## 2. ReAct：默认范式

| 维度 | 说明 |
|------|------|
| 来源 | Yao et al. (ICLR 2023)——一切现代 Agent 的祖先 |
| 机制 | 推理与行动交替：思考 → 工具 → 观察 → 再思考 |
| 优势 | 通用、灵活、可追踪推理过程 |
| 2026 状态 | 默认但不普适——**跳过它**的三种情况： |

```text
ReAct 的三种"不该用"（2026 共识）：
① 单次确定性工具调用（纯开销——Direct tool-call 足够）
② 已知计划的流程（用 PnE 或 DAG 编排，ReAct 多余）
③ 延迟敏感的语音 Agent（多轮思考太慢）
```

## 3. 规划类：PnE 与 Plan-Execute-Replan

| 范式 | 机制 | 适合 | 局限 |
|------|------|------|------|
| Plan-and-Execute | 先规划一次，再顺序执行步骤 | 可预测工作流 | 计划失效无修正 |
| Plan-Execute-Replan | 执行中失败即重新规划 | 长程任务 | 成本高、需重规划触发设计 |

```text
PnE 示例：市场报告生成
规划：搜索数据 → 分析 → 写报告 → 格式化
执行：按计划逐步执行（无探索）

PER 示例：长程运维
执行步骤失败 → 触发重规划 → 新计划继续
```

## 4. 反思类：Reflexion

| 维度 | 说明 |
|------|------|
| 机制 | ReAct + 自批评：失败后显式反思 → 经验写回 → 重试 |
| 适合 | 自改进 Agent、可重试任务（编码/写作） |
| 成本 | 高（反思轮次 × 重试轮次） |
| 2026 备注 | 外部反思循环被"模型原生推理"部分吸收（09 篇） |

> 💡 **反思的价值**：把失败变成学习信号——但每次反思都多一轮调用，要控制反思深度与触发条件（不是每次失败都全量反思）。

## 5. 分解类：Self-Ask 与 ToT

| 范式 | 机制 | 适合 | 成本警告 |
|------|------|------|---------|
| Self-Ask | 大问题拆子问题，逐个子答再汇总 | 多跳问答 | 子问题爆炸 |
| Tree-of-Thoughts | 多分支并行推理 + 剪枝 | 数学/逻辑难题 | **指数级成本** |

> ⚠️ **ToT 的生产现实**：理论价值高，成本指数级——生产优先用"原生推理模型的隐式多分支"（测试时计算）替代显式 ToT。

## 6. 执行类：Direct tool-call 与 ReWOO

| 范式 | 机制 | 适合 |
|------|------|------|
| Direct tool-call | 模型直接调用工具得答案（无思考循环） | 低风险查询：查价格/天气/状态 |
| ReWOO | 规划阶段一次列全部工具调用，执行阶段统一跑 | 可并行、无依赖、可安全并行的调用集 |

```text
ReWOO 的价值：规划与执行分离 →
① 省去"思考-执行-观察"往返（少 N 次模型调用）
② 并行执行独立调用（省延迟）
③ 规划可审计（先看计划再执行）
```

## 7. 2026 前沿：Native reasoning + ReAct

| 维度 | 说明 |
|------|------|
| 组合 | 模型原生 CoT（thinking 通道）+ ReAct 工具循环 |
| 区别 | 推理不再消耗"用户可见上下文"（独立通道） |
| 效果 | 每轮循环推理更丰富，轨迹更高效 |
| 代价 | 推理 token 计费（成本更高） |
| 推理深度 | 可调（off → xhigh）——按任务难度配深度 |

> 💡 **前沿心智**：Native reasoning 不改变循环形状，改变的是"每轮推理质量 × 成本"的权衡曲线——强推理模型可减少循环轮数，总成本未必更高。

## 8. 范式选型决策树

```text
任务形状是什么？
├─ 单次查询 → Direct tool-call
├─ 固定流程可预测 → PnE（或 DAG 编排）
├─ 开放式探索 → ReAct
├─ 长程需修正 → Plan-Execute-Replan
├─ 可重试质量任务 → Reflexion
├─ 多跳问答 → Self-Ask
├─ 可并行独立调用 → ReWOO
├─ 难题推理 → 原生推理模型（隐式 ToT）或显式 ToT
└─ 2026 通用 → Native reasoning + ReAct（默认组合）
```

## 9. 核心要点

> 🎯 **核心要点**：
> 1. 九大范式 = 循环形状的变体；选型看"可预测性 × 成本"
> 2. **ReAct 是默认但不普适**：单次调用/PnE 可覆盖/延迟敏感三种场景跳过它
> 3. 规划类（PnE/PER）适合可预测与长程；反思类（Reflexion）高成本换质量
> 4. ToT 显式成本指数级——用原生推理隐式多分支替代
> 5. 2026 默认组合：Native reasoning + ReAct；范式深潜见[主流 Agent 范式](../主流%20Agent%20范式/)

---

**上一模块**：[05 记忆架构](05-记忆架构：四层记忆与上下文管理.md)　**下一模块**：[07 Workflow 与 Agent 的边界](07-Workflow%20模式与%20Agent%20的边界：Anthropic%20六模式.md)　**返回总览**：[00 总览](00-总览：Agent%20内核理论知识体系.md)

## 【参考来源】

- [What Is ReAct? Definition & Guide (FutureAGI 2026)](https://futureagi.com/glossary/react-pattern/)
- [The Current State of Agentic AI (MachineLearningMastery)](https://machinelearningmastery.com/the-current-state-of-agentic-ai/)
- [In-the-Flow Agentic System Optimization (ICLR 2026)](https://mlanthology.org/iclr/2026/li2026iclr-intheflow/)
- [Agentic design patterns: ReAct (GitHub)](https://raw.githubusercontent.com/gtesei/agentic_design_patterns/refs/heads/main/foundational_design_patterns/8_react/pi.md)
- [Do Agents Think Deeper? Mechanistic Investigation of Sequential Planning (ACM)](https://dl.acm.org/doi/10.1007/978-981-92-3403-5_12)
