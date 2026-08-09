# Plan-and-Execute：先计划后执行

> 理论篇：第二代 Agent 范式（Wang 等，2023，LangChain 推广）——"想清楚再做"的理论深潜。

## 1. 核心机制：角色分离

```text
Plan-and-Execute：
  Planner（强模型/推理模型）→ 编号步骤列表（一次性）
    → Executor（便宜小模型）→ 逐步执行
    → 结果回传 → 执行完/需要修正 → re-plan 门
```

| 角色 | 职责 | 选型 |
|---|---|---|
| Planner | 分解目标、定顺序 | 前沿/推理模型（分解收益大） |
| Executor | 逐步执行、调工具 | 便宜/小模型 |
| Re-plan 门 | 执行停滞/低置信时重新计划 | 确定性逻辑（BabyAGI/Devin） |

> 🎯 核心要点：**成本模型 = 1×强模型 + N×便宜模型**——相比 ReAct 每步用强模型重决策，Plan-Execute 把"强思考"压缩到计划阶段一次；2026 混合形态：前沿推理模型规划 + 便宜模型执行（与 LLM 大脑 05 篇按轮路由同源）。

## 2. 实证数据（vs ReAct）

| 指标 | Plan-and-Execute |
|---|---|
| 任务完成率 | **+27%** |
| 工具调用次数 | **-42%**（一次规划 + N 次执行 vs 反复重决策） |
| 执行时间 | **-35%** |

> 💡 实证解读：**收益来自"消除重复决策"**——ReAct 每步都重想（浪费），Plan-Execute 一次想清（省）；但代价是"计划脆弱"（03 篇 §3）——先计划后执行在静态/可预测任务收益最大。

## 3. 计划脆弱与 re-plan 门

| 问题 | 机制 | 防御 |
|---|---|---|
| 计划脆弱 | Planner 在未见工具输出时承诺——意外结果使后续步骤失效 | **re-plan 门**（每 K 步/低置信触发） |
| 假设过期 | 环境变化使计划失真 | 重规划（Planner 06 篇） |
| 过度承诺 | 计划过细导致执行僵化 | 计划粒度适中（步骤级而非动作级） |

> ⚠️ 2026 工程标准：**re-plan 门是 Plan-Execute 的标配纠错**（BabyAGI 核心循环、Devin 执行停滞时重规划）——"计划是假设，执行是验证"（本体系 06 篇 Planning 组件联动）；无 re-plan 门的 Plan-Execute = 僵化流水线。

## 4. 适用边界（面试对比）

| 维度 | ReAct | Plan-and-Execute |
|---|---|---|
| 任务时长 | 短-中 | 长程 |
| 控制流 | 未知（探索） | 可分解（依赖明确） |
| 环境 | 动态（需即时反馈） | 静态/可预测 |
| 成本 | 每步强模型 | 1×强 + N×便宜 |
| 失败模式 | 无限循环 | 计划脆弱（re-plan 兜底） |
| 全局视图 | 无 | ✅（计划可见可审） |

> 🎯 一句话：**"控制流未知用 ReAct，长程可分解用 Plan-Execute"**——2026 实证 Plan-Execute 在基准上普遍胜出（+27% 完成），但计划脆弱是固有代价——re-plan 门决定它能否生产落地。

## 5. 2026 状态

| 演进 | 说明 |
|---|---|
| 混合 planner/executor 主导 | 前沿推理模型规划 + 便宜执行（2026 最强模式） |
| ReWOO 分支 | 计划先行、无观察依赖——确定性/静态环境（省 token） |
| 与反思融合 | 关键步骤后加 Reflect（关键步质量） |
| 范式路由 | 长程任务优先选 Plan-Execute（06 篇） |

## 6. 面试速记

| 问题 | 一句话答案 |
|---|---|
| Plan-Execute 是什么？ | 先计划后执行——Planner 出步骤、Executor 执行 |
| 成本模型？ | 1×强模型 + N×便宜模型 |
| 实证数据？ | +27% 完成、-42% 工具调用、-35% 时间 |
| 计划脆弱？ | 承诺在未见工具输出——re-plan 门兜底 |
| re-plan 门？ | 每 K 步/低置信触发重新计划（BabyAGI/Devin） |
| 适用？ | 长程可分解、静态可预测 |
| 与 ReAct 区别？ | 先计划 vs 边想边做 |
| 2026 形态？ | 前沿规划 + 便宜执行（混合） |
| ReWOO？ | 计划先行无观察依赖——确定性环境 |
| 无 re-plan 门后果？ | 僵化流水线 |

---

**下一模块**：[04-Reflexion 与自我修正范式](04-Reflexion与自我修正范式.md)　**返回总览**：[00-经典 Agent 范式理论总览](00-经典Agent范式理论总览.md)

## 参考来源

- [ReAct, Plan-and-Execute, or Reflection?（dev.to）](https://dev.to/gabrielanhaia/react-plan-and-execute-or-reflection-the-three-agent-patterns-every-engineer-needs-in-2026-355p)
- [Plan-and-Solve Prompting（Wang et al., 2023）](https://arxiv.org/abs/2305.04091)
- [智能体经典范式深度解析（CSDN）](https://blog.csdn.net/2502_94273177/article/details/163042096)
- [LLM Agent Architectures in 2026（FutureAGI）](https://futureagi.com/blog/llm-agent-architectures-core-components/)
