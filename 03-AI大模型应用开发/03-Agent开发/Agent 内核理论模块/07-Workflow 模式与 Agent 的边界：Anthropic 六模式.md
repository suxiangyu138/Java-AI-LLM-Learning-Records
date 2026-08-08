# 07 Workflow 模式与 Agent 的边界：Anthropic 六模式

> 定位：设计决策——Anthropic 的六个可组合架构模式（增广 LLM/链式/路由/并行/编排者-工人/评估者-优化者）与"Workflow 不取代 Agent"的设计原则（2026-08 基准）

## 📚 目录

1. [六模式总览](#1-六模式总览)
2. [增广 LLM 与提示链](#2-增广-llm-与提示链)
3. [路由与并行化](#3-路由与并行化)
4. [编排者-工人：Workflow 中的 Agent](#4-编排者-工人workflow-中的-agent)
5. [评估者-优化者：生成循环](#5-评估者-优化者生成循环)
6. [2026 生产工作流三类型](#6-2026-生产工作流三类型)
7. [设计原则：Workflow 塑造 Agent 的用武之地](#7-设计原则workflow-塑造-agent-的用武之地)
8. [六模式 × 适用场景速查](#8-六模式--适用场景速查)
9. [核心要点](#9-核心要点)

## 1. 六模式总览

Anthropic 提出的六个可组合架构模式（2024-12，2026 延续）：

| 模式 | 机制 | 典型场景 |
|------|------|---------|
| 增广 LLM | LLM + 检索/工具/记忆（单 Agent 内） | 知识增强问答 |
| 提示链（Chaining） | 前步输出 → 后步输入（线性链） | 清洗→翻译→摘要 |
| 路由（Routing） | 一次分类 → 分发专精处理 | 意图分诊 |
| 并行化（Parallelization） | 任务并行 fan-out / 投票 | 独立子任务 |
| 编排者-工人（Orchestrator-Workers） | 编排者拆解 → 工人执行 → 汇总 | 复杂多步任务 |
| 评估者-优化者（Evaluator-Optimizer） | 生成 → 评估 → 迭代优化 | 写作/翻译精修 |

> 🎯 **一句话**：六个模式是 Workflow 与 Agent 之间的**组合积木**——从最简单的链到含自主环节的编排，可混合嵌套。

## 2. 增广 LLM 与提示链

| 模式 | 机制 | 关键点 |
|------|------|--------|
| 增广 LLM | LLM + 检索/工具/记忆作为"单步能力" | 所有模式的基础积木（每步都可增广） |
| 提示链 | 固定顺序：输出 → 下一输入 | 简单可靠；延迟累积；无回退 |

```text
提示链示例：文档处理
清洗（LLM）→ 翻译（LLM）→ 摘要（LLM）
每步输出校验后传下一步（质量门禁可插）
```

## 3. 路由与并行化

| 模式 | 机制 | 优势 |
|------|------|------|
| 路由 | 一次分类调用决定走哪条专精路径 | 低成本、可测试（分类器可控） |
| 并行化（fan-out） | 同一任务并行多路处理取汇总 | 提速 |
| 并行化（投票） | 多路独立尝试投票选优 | 质量提升（多视角） |

```text
路由示例：客服
分类（意图）→ 售后路径 / 技术路径 / 销售路径
（分类错误即路由错误——Router 比 Agent 便宜且可控）

并行示例：
fan-out：翻译成 5 种语言并行
投票：3 个独立回答投票取多数
```

## 4. 编排者-工人：Workflow 中的 Agent

| 维度 | 说明 |
|------|------|
| 机制 | 编排者（LLM）拆解任务 → 分配工人（LLM/Agent）→ 汇总 |
| 与多 Agent 的区别 | 图结构固定（Workflow 骨架），节点内可自主（Agent 局部） |
| 2026 相关 | 对应多 Agent 模块的 Supervisor 模式（代码实现见[多 Agent 模块 02](../Agent%20开发框架层（工程加速）/多%20Agent（Multi‑Agent）框架/02-多%20Agent%20编排范式与模式：Supervisor%20Swarm%20Handoff%20Router.md)） |
| 适用 | 复杂多步、子任务可分解的任务 |

```text
编排者-工人示例：代码库分析
编排者：拆解为 5 个文件分析任务
工人：并行分析各文件（可含工具调用）
编排者：汇总分析 → 生成报告
```

> 💡 **定位**：编排者-工人是"Workflow 骨架 + Agent 局部自主"的最佳代表——图固定（可审计），节点自主（灵活）。

## 5. 评估者-优化者：生成循环

| 维度 | 说明 |
|------|------|
| 机制 | 生成器产出 → 评估者打分/反馈 → 生成器迭代 |
| 适合 | 有明确质量标准、可自动评估的任务（写作/翻译/代码） |
| 成本 | 每次迭代 2 次调用（生成 + 评估） |
| 终止 | 达到质量标准或迭代上限 |

```text
评估者-优化者示例：文案优化
生成器：写初稿
评估者：按评分标准打分 + 改进建议
生成器：按建议改稿
→ 循环直到分数达标或迭代上限
```

> ⚠️ **适用前提**：必须有可自动评估的标准——质量主观且无法打分时，评估者循环会空转（此时用人工评估或放弃循环）。

## 6. 2026 生产工作流三类型

Anthropic 2025-2026 "Common workflow patterns" 补充的生产分类：

| 类型 | 说明 | 取舍 |
|------|------|------|
| Sequential（顺序） | 固定顺序执行（默认） | 延迟换准确 |
| Parallel（并行） | 独立任务并行 fan-out | 吞吐换复杂度 |
| Evaluator-optimizer（评估-优化） | 生成/评估迭代 | 质量换成本 |

> 🎯 **生产默认**：能顺序就顺序（最简单）；确需提速用并行；质量不足才上评估循环。

## 7. 设计原则：Workflow 塑造 Agent 的用武之地

```text
核心原则："Workflows don't replace agent autonomy;
          they shape where and how agents apply it."
（Workflow 不取代 Agent 自主性，而是塑造它在哪用、怎么用）

含义：
① 每一步都可以是增广 LLM（检索/工具）
② 整体编排走预定义路径（确定性/可审计）
③ Agent 的自主性只出现在"路径无法预映射"的局部
→ 六模式中"编排者-工人"与"评估者-优化者"是 Agent 的最佳落点
```

## 8. 六模式 × 适用场景速查

| 场景 | 模式 | 为什么 |
|------|------|--------|
| 知识问答 | 增广 LLM（RAG） | 单步即可 |
| 文档流水线 | 提示链 | 固定顺序 |
| 客服分诊 | 路由 | 分类可控 |
| 多语言翻译 | 并行 fan-out | 独立提速 |
| 代码库分析 | 编排者-工人 | 可分解 |
| 文案精修 | 评估者-优化者 | 可评分 |
| 退款分诊（路径未知） | Agent（跳出现有模式） | 无法预映射 |

## 9. 核心要点

> 🎯 **核心要点**：
> 1. Anthropic 六模式：增广 LLM / 提示链 / 路由 / 并行化 / 编排者-工人 / 评估者-优化者——可组合嵌套
> 2. 每步都可"增广"（检索/工具/记忆）——这是模式与 Agent 的接口
> 3. **编排者-工人**是"Workflow 骨架 + Agent 局部自主"的最佳代表
> 4. 评估者-优化者必须有可自动评估的标准，否则空转
> 5. 设计原则：Workflow 塑造 Agent 的用武之地——图固定保审计，节点自主保灵活

---

**上一模块**：[06 推理范式谱系](06-推理范式谱系：ReAct%20PnE%20Reflexion%20ToT%20九模式.md)　**下一模块**：[08 Harness 与生产运行时](08-Harness%20与生产运行时：状态%20压缩%20追踪%20护栏.md)　**返回总览**：[00 总览](00-总览：Agent%20内核理论知识体系.md)

## 【参考来源】

- [How Anthropic Thinks About Agents, Workflows, and Tasks (2026)](https://shellypalmer.com/2026/04/how-anthropic-thinks-about-agents-workflows-and-tasks/)
- [Common workflow patterns for AI agents (Anthropic)](https://claude.com/blog/common-workflow-patterns-for-ai-agents-and-when-to-use-them)
- [Agentic Workflow vs. Autonomous Agent (MachineLearningMastery)](https://machinelearningmastery.com/agentic-workflow-vs-autonomous-agent-whats-the-difference/)
- [The Current State of Agentic AI (MachineLearningMastery)](https://machinelearningmastery.com/the-current-state-of-agentic-ai/)
- [Agentic Workflow 与 Agent 选型踩坑实录（阿里云开发者）](https://developer.aliyun.com/article/1747461)
- [Anthropic《Building effective agents》深度解读（CSDN）](https://blog.csdn.net/Rocky6688/article/details/162515550)
