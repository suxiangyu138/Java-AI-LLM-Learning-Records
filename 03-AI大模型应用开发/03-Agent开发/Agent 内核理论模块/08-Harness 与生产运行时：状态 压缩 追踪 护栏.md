# 08 Harness 与生产运行时：状态 / 压缩 / 追踪 / 护栏

> 定位：运行时原理——生产 Agent 的 Harness 能力矩阵：状态持久化、上下文压缩、任务清单、渐进式技能加载、工具审批与 2026"护栏独立学科"转向（2026-08 基准）

## 📚 目录

1. [Harness 能力矩阵](#1-harness-能力矩阵)
2. [状态持久化与恢复](#2-状态持久化与恢复)
3. [上下文压缩：运行时第一职责](#3-上下文压缩运行时第一职责)
4. [任务清单与双模式](#4-任务清单与双模式)
5. [渐进式技能加载](#5-渐进式技能加载)
6. [工具审批门](#6-工具审批门)
7. [追踪与可观测](#7-追踪与可观测)
8. [护栏：2026 独立学科](#8-护栏2026-独立学科)
9. [核心要点](#9-核心要点)

## 1. Harness 能力矩阵

2026 生产 Harness（Claude Agent SDK 等）的九项职责：

| 能力 | 解决的问题 | 代表实现 |
|------|-----------|---------|
| 状态持久化 | 崩溃/中断后恢复 | 每模型调用后保存状态 |
| 上下文压缩 | 窗口溢出失忆 | 摘要/丢弃/截断 |
| 任务清单 | 长任务无路径 | todo provider |
| 双模式 | 规划与执行混杂 | plan/execute 追踪 |
| 会话笔记 | 跨会话知识丢失 | file memory |
| 渐进式技能加载 | 工具/技能全载爆上下文 | SKILL.md 按需加载 |
| 工具审批 | 敏感操作无门禁 | tool approval gates |
| 追踪 | 失败不可定位 | OpenTelemetry |
| 终止 | 无限循环烧钱 | 轮次/时间/花费上限 |

> 🎯 **一句话**：Harness = 让 Agent 循环"生产可活"的九项工程——框架的差异主要在此（这也是各框架竞争的实质）。

## 2. 状态持久化与恢复

| 实践 | 说明 |
|------|------|
| 每调用保存 | 每次模型调用后持久化状态（消息/工作状态） |
| 崩溃恢复 | 从检查点恢复（对应 LangGraph checkpointer 原理） |
| 会话恢复 | 按会话 ID 恢复完整上下文 |
| 2026 前沿 | 两阶段初始化 + git 式状态持久化（长运行 harness） |

> 💡 **原理对应**：LangGraph 的 checkpointer、CrewAI 的 checkpointing 都是本节的工程实现——原理一致（每节点保存），实现各异（详见[框架层](../Agent%20开发框架层（工程加速）/单%20Agent%20框架/00-总览：单%20Agent%20框架知识体系.md)）。

## 3. 上下文压缩：运行时第一职责

```text
压缩三招（05 篇详述，此处给运行时策略）：
① 摘要触发阈值：消息达窗口 80% 触发
② 丢弃：工具结果/中间推理按重要性淘汰
③ 截断：单条超长内容截断/摘要

2026 数据点：MCP 代码执行包装用"渐进式发现"
将 token 消耗削减 ~98.7% → 压缩是成本杠杆，不只是生存机制
```

## 4. 任务清单与双模式

| 能力 | 说明 |
|------|------|
| todo provider | 维护任务清单（已完成/进行中/剩余），模型可读写 |
| plan 模式 | 先规划（不执行）→ 用户确认 |
| execute 模式 | 执行已批准计划 |
| 意义 | 长任务的方向锚 + 用户控制点（HITL） |

```text
todo 与记忆的关系：
Working 记忆（结构化状态）≈ todo 的实现载体
→ 区别：todo 面向模型决策，Working 面向整体状态
```

## 5. 渐进式技能加载

```text
问题：工具/技能全量载入 → 上下文爆炸（100 个工具 = 窗口大半）
方案：渐进式加载（2025-2026 共识）：
① 常驻：技能名 + 一句话描述（轻量索引）
② 触发：模型决定调用时 → 加载完整 body
③ 按需：相关文件在读取时才注入

对应实现：
Claude Skills（SKILL.md 渐进披露）、工具语义发现（向量索引注册表）
```

> 🎯 **渐进式加载是"大工具集 Agent"的生存前提**——100+ 工具不渐进加载等于自爆上下文。

## 6. 工具审批门

| 维度 | 说明 |
|------|------|
| 是什么 | 敏感操作（支付/删除/写生产库）需人工/策略批准 |
| 实现 | Harness 层拦截 + 审批流（HITL interrupt） |
| 与 LLM 过滤的区别 | 拦在执行层（工具授权），不是输出层（内容过滤） |
| 2026 基线 | 敏感动作默认门禁；审批记录入审计 |

> 💡 **工具审批 = 幂等外的第二道安全线**：幂等防重试副作用，审批防越权动作。

## 7. 追踪与可观测

| 维度 | 说明 |
|------|------|
| 最小追踪 | 每次模型调用/工具调用埋点（traceId 贯穿） |
| 标准 | OpenTelemetry（2026 事实标准） |
| 诊断能力 | 回放轨迹（哪一步失败/哪一步烧钱） |
| 数据对比 | 89% 团队有可观测 vs 仅 52% 有 evals——**评估是缺口** |

> ⚠️ **生产启示**：追踪是"能看见"，评估是"能判断好坏"——2026 年多数团队卡在第一层；评估基线（任务完成率/工具调用正确率/升级正确率/P95 延迟）应补上。

## 8. 护栏：2026 独立学科

```text
2026 护栏的演化：
旧（2024）：LLM 输入/输出过滤（内容层）
新（2026）：工具调用授权 / 速率限制 / 步数与花费预算 /
           验证 Agent 实际做了什么（执行层治理）

原则：护栏执行在工具执行层，而非输出层——
      因为危害发生在工具动作，不是文本输出
```

| 护栏类型 | 拦截点 | 示例 |
|---------|-------|------|
| 工具授权 | 执行前 | 禁止写生产库的工具 |
| 速率限制 | 执行前 | 每分钟 API 调用上限 |
| 步数/花费预算 | 循环中 | max_turns + 花费上限 |
| 行为验证 | 执行后 | 校验实际执行结果 |
| 意图拦截 | 决策前 | intent guard（意图检测） |

## 9. 核心要点

> 🎯 **核心要点**：
> 1. Harness 九项职责：持久化/压缩/任务清单/双模式/会话笔记/渐进加载/审批/追踪/终止
> 2. **渐进式技能加载是大工具集 Agent 的生存前提**（全载 = 爆上下文）
> 3. 压缩是成本杠杆（渐进发现可省 ~98.7% token）
> 4. 护栏 2026 转向执行层治理：工具授权/速率/预算/行为验证——危害在工具动作不在文本
> 5. 可观测领先评估（89% vs 52%）——评估基线是生产补课重点

---

**上一模块**：[07 Workflow 模式与 Agent 的边界](07-Workflow%20模式与%20Agent%20的边界：Anthropic%20六模式.md)　**下一模块**：[09 模型原生智能转向](09-模型原生智能转向：native%20reasoning%20与%20test-time%20compute.md)　**返回总览**：[00 总览](00-总览：Agent%20内核理论知识体系.md)

## 【参考来源】

- [The AI Agents Stack (2026 Edition) (O'Reilly Radar)](https://www.oreilly.com/radar/the-ai-agents-stack-2026-edition/)
- [The Agent Loop Decoded (Oracle)](https://blogs.oracle.com/developers/the-agent-loop-decoded-three-levels-every-agent-engineer-must-know)
- [Common workflow patterns for AI agents (Anthropic)](https://claude.com/blog/common-workflow-patterns-for-ai-agents-and-when-to-use-them)
- [Microsoft Agent Framework Makeover: Claws, Loops and Harnesses (Visual Studio Magazine)](https://visualstudiomagazine.com/articles/2026/07/22/microsoft-agent-framework-makeover-claws-loops-and-harnesses.aspx)
- [The Current State of Agentic AI (MachineLearningMastery)](https://machinelearningmastery.com/the-current-state-of-agentic-ai/)
- [How Anthropic Thinks About Agents, Workflows, and Tasks (2026)](https://shellypalmer.com/2026/04/how-anthropic-thinks-about-agents-workflows-and-tasks/)
