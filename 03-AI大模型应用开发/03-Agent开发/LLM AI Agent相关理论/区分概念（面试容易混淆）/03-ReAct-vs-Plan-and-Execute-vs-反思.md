# ReAct vs Plan-and-Execute vs 反思

> 辨析篇：三大 Agent 范式的面试混淆点。理论深潜见 [经典范式 02-04 篇](..%2F经典%20Agent%20范式（理论，面试高频）%2F02-ReAct：推理与行动交织.md)。

## 1. 三范式一句话

| 范式 | 一句话 | 时间 |
|---|---|---|
| ReAct | 边想边做（推理+行动交织） | 2022 |
| Plan-and-Execute | 先计划后执行（角色分离） | 2023 |
| 反思（Reflexion） | 做后复盘（批评+记忆） | 2023 |

> 🎯 核心要点：**三者不是同层概念**——ReAct 是"执行形态"、Plan-Execute 是"执行形态的变体"、反思是"叠加在循环上的修正机制"——反思通常叠加在 ReAct/Plan-Execute 之上（不是并列第三范式）。

## 2. 混淆点 1：ReAct vs Plan-and-Execute

| 维度 | ReAct | Plan-Execute |
|---|---|---|
| 决策时机 | 每步（边想边做） | 计划阶段一次（先想后做） |
| 全局视图 | ❌ | ✅（计划可见可审） |
| 成本 | N 步 × 强模型 | 1×强 + N×便宜 |
| 失败模式 | 无限循环 | 计划脆弱（re-plan 门兜底） |
| 适用 | 控制流未知/动态 | 长程可分解/静态 |
| 实证 | 基线 | +27% 完成/-42% 工具调用 |

> 💡 面试判据：**"控制流未知用 ReAct，长程可分解用 Plan-Execute"**——别背参数，讲清"决策时机"这个本质区别。

## 3. 混淆点 2：反思 vs 重试

| 维度 | 反思 | 重试 |
|---|---|---|
| 触发 | 失败 + 关键步骤后 | 失败 |
| 动作 | 评估→分析→改进 | 原样再跑 |
| 例子 | "我考虑合规风险了吗" | 网络超时重发 |
| 治什么 | 脑子没想全 | 手抖（瞬时故障） |

> ⚠️ 面试必答：**"重试治瞬时故障（要同样结果），反思治推理缺陷（要不同结果）"**——transient 重试重放、sampling 重试重生成（Orchestrator 05 篇）；把反思当重试 = 每轮都从零开始。

## 4. 混淆点 3：反思 vs 重规划

| 维度 | 反思 | 重规划 |
|---|---|---|
| 改什么 | 策略（做法） | 路线（计划） |
| 层级 | 单步质量 | 多步路径 |
| 触发 | 输出质量差 | 执行失败/计划失效 |
| 关系 | 先反思后重规划（反思发现计划问题→重规划修正） | — |

> 💡 面试表述：**"反思改'怎么做'，重规划改'走哪条路'"**——Planner 05 篇（重规划）与 Reflection（反思）的分工；级联：反思发现计划失效 → 触发重规划。

## 5. 组合关系（2026 生产）

```text
生产组合：
  ReAct/Plan-Execute（执行形态）
    + 反思（关键步骤后质量把关，有 oracle 时）
    + 重规划（失败/计划失效时）
    = 混合 Agent（2026 默认）
```

> 🎯 核心要点：**"三个概念是层不是选项"**——执行形态（ReAct 或 Plan-Execute）打底、反思叠加质量、重规划兜底失败；2026 的范式路由在"执行形态"层选（经典范式 06 篇），反思/重规划是通用增强层。

## 6. 面试速记

| 问题 | 一句话答案 |
|---|---|
| 三者同层吗？ | 不是——反思/重规划是增强层，叠加在执行形态上 |
| ReAct vs Plan-Execute？ | 决策时机：每步 vs 计划一次 |
| 反思 vs 重试？ | 先评再改 vs 原样再跑 |
| 反思 vs 重规划？ | 改策略（怎么做）vs 改路线（走哪条） |
| 重试治什么？ | 瞬时故障（要同样结果） |
| 反思治什么？ | 推理缺陷（要不同结果） |
| 组合？ | 执行形态 + 反思 + 重规划 = 混合 Agent |
| 级联？ | 反思发现计划失效→重规划 |
| 范式路由选哪层？ | 执行形态层 |
| 2026 默认？ | 混合——不是单选 |

---

**下一模块**：[04-记忆 vs 上下文 vs 知识库](04-记忆-vs-上下文-vs-知识库.md)　**返回总览**：[00-区分概念总览](00-区分概念总览.md)

## 参考来源

- [ReAct, Plan-and-Execute, or Reflection?（dev.to）](https://dev.to/gabrielanhaia/react-plan-and-execute-or-reflection-the-three-agent-patterns-every-engineer-needs-in-2026-355p)
- [经典范式深潜：ReAct、Plan-and-Solve 与 Reflection（CSDN）](https://blog.csdn.net/2502_94273177/article/details/163042096)
- [Reflexion（Shinn et al., 2023）](https://arxiv.org/abs/2303.11366)
- [Implement agent reflection and planning cycles（Microsoft Learn）](https://learn.microsoft.com/zh-cn/training/modules/aaai-design-agentic-loops-azure-ai-agent-service/4-implement-agent-reflection-planning-cycles)
