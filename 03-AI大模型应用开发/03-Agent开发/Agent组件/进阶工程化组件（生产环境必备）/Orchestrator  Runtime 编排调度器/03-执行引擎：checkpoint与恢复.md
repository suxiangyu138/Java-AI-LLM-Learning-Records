# 执行引擎：checkpoint 与恢复

> 执行引擎 = 运行时的心脏：循环执行 + checkpoint + 恢复。2026 关键能力：**连接恢复**（客户端断线后回填）、**轨迹分支**（checkpoint 处测试备选路径）、**毫秒级沙箱 checkpoint**（DeltaBox）。本章给执行引擎完整设计。

## 1. 执行循环与 checkpoint 落点

```text
执行循环（含 checkpoint 点）：
  LLM 调用 ──→ [checkpoint: 输入/输出/token]
    → 工具调用 ──→ [checkpoint: 工具名/参数/结果]
    → 状态更新 ──→ [checkpoint: 会话状态]
    → 下一轮
```

| checkpoint 点 | 记录 | 恢复用途 |
|---|---|---|
| LLM 调用 | 提示/响应/token/成本 | 重放跳过重推理（02 篇） |
| 工具调用 | 参数/结果/耗时 | 重放跳过重执行（防副作用） |
| 状态更新 | 会话状态快照 | 精确恢复 |
| 交接 | 任务/上下文（Multi-Agent 05 篇） | 跨 Agent 恢复 |

> 🎯 核心要点：**checkpoint 粒度决定恢复精度**——superstep 级（MAF）丢并行子步；事件级（Temporal/Agent Executor）精确到每次调用；粒度越粗恢复越"约等于"。

## 2. 连接恢复：断线不丢会话

| 能力 | 机制 |
|---|---|
| 重连回填 | 客户端重连后从最后看到的序列回填响应（Agent Executor） |
| 断点续跑 | 网络抖动不影响工作流（事件日志） |
| 幂等消费 | 客户端重复收响应不重复副作用 |

> 💡 连接恢复是"体验层"的持久性——工作流内部已恢复，但客户端要知道"从哪继续看"；回填机制让用户重连后看到完整上下文而非从头。

## 3. 轨迹分支：checkpoint 的调试超能力

| 项 | 说明 |
|---|---|
| 机制 | checkpoint 处测试备选执行路径——不丢原上下文 |
| 类比 | 时间旅行调试（Multi-Agent 07 篇 LangGraph time-travel）的运行时版 |
| 用途 | 实验备选提示/参数/路径；事故重演；A/B |
| 前提 | checkpoint 有效 + fork 确定性（02 篇六义务） |

> 🎯 核心要点：**轨迹分支把"调试"变成"实验"**——从 checkpoint 分叉试新路径，原轨迹保留；与评估体系联动（备选路径入评测集）。"时间旅行第一次生产失败就值回票价"。

## 4. 毫秒级 checkpoint：DeltaBox（2026）

| 项 | 说明 |
|---|---|
| 场景 | 有状态 Agent 的沙箱 checkpoint/回滚 |
| 机制 | GSD（global snapshot daemon）写控制 FIFO——毫秒级快照 |
| 用途 | 崩溃恢复的持久后备 + 冷路径恢复 |
| 意义 | checkpoint 从"秒级/分钟级"走向"毫秒级"——高状态频率 Agent 可用 |

> ⚠️ 毫秒级 checkpoint 的代价：**快照频率 × 存储**——高频快照适合高状态 Agent（交易/交互式），低频场景事件日志足够；按状态变化率选 checkpoint 策略。

## 5. 恢复模式对比

| 模式 | 机制 | 适用 |
|---|---|---|
| 快照恢复 | 加载最近快照 | 状态大、增量小 |
| 事件重放 | 从头/快照重放日志（02 篇） | 审计/跨 worker |
| 混合恢复 | 快照 + 增量重放 | 2026 标准 |
| 毫秒快照 | GSD 高频（§4） | 高频状态变化 |

> 🎯 核心要点：**恢复模式 = 状态大小 × 崩溃频率 × 恢复时延要求**——交易型要毫秒级（DeltaBox）、工作流型混合恢复（Temporal）、调试型重放（轨迹分支）。

## 6. 执行引擎的故障处理

| 故障 | 处理 |
|---|---|
| 进程死亡 | 事件日志重放——run 在任意 worker 恢复（02 篇） |
| 网络抖动 | 连接恢复 + 重试（05 篇） |
| 节点超时 | 超时重试（非幂等注意，05 篇） |
| 并发冲突 | 单写者（06 篇） |
| 沙箱崩溃 | 沙箱隔离重启（09 篇） |

> 💡 与 Tool 06 篇衔接：循环内单调用故障（结构化错误返回）是"软故障"；运行时故障（进程/网络/沙箱）是"硬故障"——软硬分离处理：软故障模型自纠，硬故障运行时恢复。

## 7. 常见误区

| 误区 | 真相 |
|---|---|
| "checkpoint 越细越好" | 粒度×存储成本——按恢复精度需求选 |
| "快照足够" | 混合恢复——快照+增量重放 |
| "断线=重来" | 连接恢复回填——客户端续看 |
| "调试只能重跑" | 轨迹分支——从 checkpoint 实验 |
| "毫秒 checkpoint 万能" | 高频快照贵——按状态变化率选 |
| "superstep 粒度够" | 丢并行子步——事件级才是精确 |
| "恢复=重放全量" | 混合：快照加速 + 增量重放 |

## 8. 面试速记

| 问题 | 一句话答案 |
|---|---|
| checkpoint 落点？ | LLM/工具/状态/交接四处 |
| 粒度决定什么？ | 恢复精度——superstep 丢并行子步 |
| 连接恢复？ | 重连回填最后序列——客户端续看 |
| 轨迹分支？ | checkpoint 处测试备选路径——调试变实验 |
| DeltaBox？ | 毫秒级沙箱 checkpoint（GSD 控制 FIFO） |
| 恢复三模式？ | 快照/重放/混合——按状态大小选 |
| 硬故障 vs 软故障？ | 运行时恢复硬故障，模型自纠软故障 |
| 时间旅行？ | LangGraph time-travel 的运行时版 |
| 与评估联动？ | 备选路径入评测集 |
| 毫秒 checkpoint 代价？ | 快照频率×存储——按状态变化率选 |
| 事件级恢复？ | 精确到每次调用——重放跳过 |
| 与 02 篇关系？ | checkpoint 是持久执行的实施点 |

---

**下一模块**：[04-调度与并发：程序级调度](04-调度与并发：程序级调度.md)　**返回总览**：[00-Orchestrator Runtime 编排调度器总览](00-OrchestratorRuntime编排调度器总览.md)

## 参考来源

- [Agent Executor, Google's distributed Agent Runtime（Google Cloud Blog）](https://cloud.google.com/blog/products/ai-machine-learning/agent-executor-googles-distributed-agent-runtime/)
- [DeltaBox: Scaling Stateful AI Agents with Millisecond-Level Sandbox Checkpoint/Rollback（arXiv 2605.22781）](https://arxiv-org.ezproxy.obspm.fr/html/2605.22781v1)
- [Still Not Durable: MS Agent Framework & Strands（Diagrid）](https://www.diagrid.io/blog/still-not-durable-how-microsoft-agent-framework-and-strands-agents-repeat-the-same-mistake)
- [Durable Execution for AI Agent Runtimes（Zylos Research）](https://zylos.ai/zh/research/2026-04-24-durable-execution-agent-runtimes/)
