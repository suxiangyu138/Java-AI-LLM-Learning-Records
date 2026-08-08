# 03 终止条件与循环控制：max_turns 与防御纵深

> 定位：工程成败关键——Agent 循环的终止工程：四类停止机制、max_turns 数值基线、防御纵深原则、early-stopping 模式与停滞检测（2026-08 基准）

## 📚 目录

1. [为什么终止是成败关键](#1-为什么终止是成败关键)
2. [四类停止机制](#2-四类停止机制)
3. [max_turns：最重要的安全控制](#3-max_turns最重要的安全控制)
4. [early-stopping：到上限不是静默失败](#4-early-stopping到上限不是静默失败)
5. [防御纵深：多机制重叠](#5-防御纵深多机制重叠)
6. [停滞检测：卡死循环的识别](#6-停滞检测卡死循环的识别)
7. [失败模式速查](#7-失败模式速查)
8. [核心要点](#8-核心要点)

## 1. 为什么终止是成败关键

```text
终止工程的两面：
过早终止：模型一次工具失败就放弃 → "Agent 没帮到我"
过晚终止：循环无界运行 → "Agent 烧光预算"
→ 两种失败对用户看起来一样，但在追踪中一目了然

数据证据：
· 单请求 60 次 LLM 调用产生 $9 账单 = 无终止防护的失控案例
· 成功运行的轮次数紧聚；失败运行呈长尾分布（SWE-bench 证据）
→ 轮次分布是诊断 Agent 质量的第一信号
```

> 🎯 **一句话**：终止条件不是"锦上添花"——它是 Agent 从"玩具"到"生产"的分水岭工程。

## 2. 四类停止机制

| 机制 | 触发 | 优先级 |
|------|------|:---:|
| 模型信号 | 模型发出 `finish` / `[DONE]`；assistant 轮次无 tool_calls（`end_turn`/空数组） | 正常结束 |
| 轮次上限 | `max_turns` 达到 | 硬兜底 |
| 每工具失败上限 | 同一工具连续失败 N 次 | 防卡死 |
| 护栏触发 | guardrail/预算/审批拦截 | 安全兜底 |

```text
停止信号识别（2026 API 事实）：
Anthropic：stop_reason === 'end_turn'
OpenAI：   tool_calls 为空数组
→ 无工具调用的 assistant 轮次 = 自然终止信号
```

## 3. max_turns：最重要的安全控制

| 任务类型 | 典型上限 | 说明 |
|---------|:---:|------|
| 客服/支持 | 8-12 | 短任务、快响应 |
| 通用生产 | 15-25 | 默认安全区间 |
| 研究/长任务 | 20-30 | 允许更多探索 |
| 探索性任务 | 50+ | 需明确成本预期 |

> ⚠️ **数值基线**（2026 生产共识）：无特殊理由不超 25 步；每类任务设自己的上限——**上限是成本控制的第一道闸门**。

## 4. early-stopping：到上限不是静默失败

```text
错误做法：达到 max_turns → 静默停止 → 用户收到半截结果

正确做法（early-stopping-generate 模式）：
1. 检测达到上限
2. 追加消息："你已达到最大步骤数，请基于已完成的工作
   直接给出最佳答案"
3. 再调用一次模型（不带工具）合成最终答案
→ 把"半成品"变成"基于部分工作的最佳答案"
```

| 要点 | 说明 |
|------|------|
| 时机 | 任何停止条件触发且无最终答案时 |
| 调用 | 无工具的最后一次合成调用 |
| 价值 | 避免"静默失败"观感；保留已完成工作价值 |
| 成本 | 多一次调用，值得 |

## 5. 防御纵深：多机制重叠

2026 生产共识：**终止不能依赖单一机制**——必须多层重叠：

```text
第 1 层  模型信号（正常完成）
第 2 层  max_turns 轮次上限（全局兜底）
第 3 层  每工具失败上限（卡死防护）
第 4 层  token/花费预算（成本兜底）
第 5 层  超时（挂起防护）
第 6 层  护栏（内容/工具授权拦截）
→ 任意单层失效，系统仍能停止
```

> 💡 **为什么必须多层**：模型可能永远不主动停止；轮次上限可能因长工具调用烧光 token；工具可能挂起——每层覆盖不同失败模式。

## 6. 停滞检测：卡死循环的识别

| 信号 | 识别方法 | 含义 |
|------|---------|------|
| 重复调用同一工具 | 工具调用轨迹分析 | "stuck loop"教科书签名 |
| GoalProgress 平线 | 相邻轮次目标进度指标 | 无前进 = 停滞 |
| StepEfficiency 低 | 浪费轮次 / 最小轮次 | 低效循环 |
| 长尾轮次 | 轮次分布右尾 | 失败的运行特征 |
| 基准证据 | τ-bench 前沿 Agent 中期约 60%+（迭代增长时停滞） | 迭代越多越需监控 |

```text
生产实践：对每轮打分（GoalProgress/StepEfficiency），
监控工具级停滞信号 → 触发护栏或人工介入
```

## 7. 失败模式速查

| 失败模式 | 表现 | 对策 |
|---------|------|------|
| 无界运行 | 无限循环烧钱 | max_turns + 花费预算 |
| 过早放弃 | 一次失败即终止 | 错误即观察 + 重试策略 |
| 卡死循环 | 重复调用坏工具 | 每工具失败上限 |
| 静默半成品 | 到上限无输出 | early-stopping 模式 |
| 挂起 | 工具长时间无响应 | 超时 + 幂等重试 |
| 路径漂移 | 偏离目标绕圈 | GoalProgress 监控 |

## 8. 核心要点

> 🎯 **核心要点**：
> 1. 终止工程 = 过早/过晚终止的平衡——两种失败用户观感相同但追踪可辨
> 2. 四类停止：模型信号 / max_turns / 每工具失败上限 / 护栏
> 3. max_turns 数值基线：客服 8-12、通用 15-25、研究 20-30——成本第一闸门
> 4. **early-stopping 模式**：到上限后无工具再合成一次，防静默失败
> 5. 防御纵深：6 层重叠机制；停滞检测（GoalProgress/重复工具调用）是生产监控标配

---

**上一模块**：[02 Agent 循环深潜](02-Agent%20循环深潜：五阶段循环与五要素.md)　**下一模块**：[04 核心组件四件套](04-核心组件四件套：Model%20Memory%20Planner%20Tools%20与%20Harness.md)　**返回总览**：[00 总览](00-总览：Agent%20内核理论知识体系.md)

## 【参考来源】

- [The AI Agents Stack (2026 Edition) (O'Reilly Radar)](https://www.oreilly.com/radar/the-ai-agents-stack-2026-edition/)
- [Agent Loop: Definition, Examples & Guide (FutureAGI 2026)](https://futureagi.com/glossary/agent-loop/)
- [The Agent Loop Decoded (Oracle)](https://blogs.oracle.com/developers/the-agent-loop-decoded-three-levels-every-agent-engineer-must-know)
- [What Is an AI Agent Loop? (FutureAGI)](https://futureagi.com/blog/loop-engineering/what-is-ai-agent-loop/)
- [How to Build an AI Agent: A Developer's Guide (Rasa)](https://rasa.com/blog/how-to-build-an-ai-agent)
- [The Current State of Agentic AI (MachineLearningMastery)](https://machinelearningmastery.com/the-current-state-of-agentic-ai/)
