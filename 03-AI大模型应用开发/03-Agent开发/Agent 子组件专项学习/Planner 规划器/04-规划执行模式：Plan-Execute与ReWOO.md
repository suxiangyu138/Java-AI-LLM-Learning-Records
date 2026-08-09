# 规划执行模式：Plan-Execute 与 ReWOO

> 规划器怎么与执行器协作？2026 年两条主线：**Plan-Execute（动态，观察驱动）**与 **ReWOO/Blueprint（静态，一次规划）**——选型的本质是"计划能不能预先定死"。

## 1. 两种模式的本质区别

| 维度 | Plan-Execute | ReWOO / Blueprint |
|---|---|---|
| 规划时机 | 先规划，执行中重规划 | 一次规划全 DAG |
| LLM 调用 | 多（每步决策+重规划） | ~2 次（规划 1 + 收尾 1） |
| 应变能力 | ✅ 观察驱动调整 | ❌ 不能应变 |
| 成本 | 中-高 | **低（省 ~20 倍）** |
| 适用 | 调试-直到-通过类 | 可预知 DAG 类 |

> 🎯 核心要点：**静态计划便宜但"瞎"（看不见新情况），动态计划贵但"活"**——选型 = 任务可预知度评估：能预先写全步骤的用 ReWOO，必须边做边看的用 Plan-Execute。

## 2. Plan-Execute 完整循环

```text
① Planner：目标 → 步骤列表（03 篇契约）
② Executor：取第一个 pending 步骤 → 执行（工具）
③ Observer：收集观察（结果/状态/错误）
④ Evaluator：步骤完成？（success_criteria）
⑤ Replanner：完成→下一步 / 偏离→更新计划 / 全完→收尾
```

| 角色 | 模型档位 | 调用次数 | 职责 |
|---|---|---|---|
| Planner | 前沿推理 | 1-2 | 初始计划 |
| Executor | 便宜（14B 可） | 每步 1 | 单步执行 |
| Replanner | 中档 | 每步 1（或按需） | 进度评估+重规划 |

> 💡 2026 成本模式：**"前沿规划 + 便宜执行"是 Plan-Execute 的标准成本优化**——规划质量决定上限，执行成本决定下限，两者用不同档模型是常态。

## 3. ReWOO 的实现模式

```text
ReWOO 流程（省 LLM 调用的关键）：
  ① Planner 一次输出工具 DAG（含每步工具与参数模板）
  ② 无 LLM 执行：按 DAG 逐个调工具，结果填入变量（#E1, #E2...）
  ③ Solver 一次调用：把全部结果汇总成答案
  合计 ~2-3 次 LLM 调用（vs Plan-Execute 每步 1 次）
```

| ReWOO 要点 | 说明 |
|---|---|
| 变量占位 | 步骤结果以 #En 变量传递（不重述） |
| 确定性执行 | 步骤间无 LLM 判断（快且省） |
| 模板化 | 参数模板预填（如 search: #E1 的结果） |
| 局限 | 无法处理"结果决定下一步" |

## 4. 选型矩阵（2026 实测）

| 任务类型 | 推荐模式 | 理由 |
|---|---|---|
| 拉取 N 个独立源 | ReWOO/Blueprint | 静态 DAG，省 20 倍 |
| 生成 N 个文件 | ReWOO | 可预知 |
| 调试-直到-通过 | Plan-Execute | 观察驱动 |
| 分支决策（结果决定路径） | Plan-Execute | 需应变 |
| 混合（探索+执行） | 混合（阶段粗+阶段内 ReWOO） | 两头收益 |

```text
决策问题："计划在第一步执行前能完整写出来吗？"
  能 → ReWOO（省）；不能 → Plan-Execute（稳）
  部分能 → 混合（能的部分 DAG，不能的部分动态）
```

## 5. Plan-Execute-Reflect（PER）：2026 生产标配

```text
Plan → Execute → Reflect（评估+更新计划）循环
  Reflect = 评估步骤结果 + 决定继续/重规划/收尾
  （OpenSearch/CloudWeGo 均已内置该模式）
```

| PER 变体 | 特点 |
|---|---|
| 简单 PER | Reflect 只判断"继续 or 重规划" |
| 带记忆 PER | 反思结论入记忆（跨任务） |
| 分级 PER | 每 N 步小结 + 失败时深反思 |

> 💡 PER 与[反思组件](..%2F..%2FAgent%20四大核心组件%2F04-反思组件：自我评估与纠错.md)的关系：Reflect 是"轻量反思"（只评估进度），完整反思（重规划策略）是反思组件的职责——**PER 的 Reflect 是规划器自己的内置检查**。

## 6. 模式切换的动态决策

| 信号 | 切换动作 |
|---|---|
| 执行中发现计划可预知 | 剩余步骤转 ReWOO（省） |
| 执行中频繁偏离 | 剩余步骤转 Plan-Execute（稳） |
| 子任务独立可并行 | 该子任务用 DAG（并行） |
| 子任务探索性强 | 该子任务用动态（重规划） |

> 💡 动态切换是 2026 年成本优化的前沿实践——**"计划级 ReWOO + 步骤级动态"的混合**，兼顾省与稳（对应 02 篇"阶段粗+阶段内细"的粒度哲学）。

## 7. 常见坑速查

| 坑 | 现象 | 解法 |
|---|---|---|
| 可预知任务用动态 | 每步重复 LLM 调用 | ReWOO 静态化 |
| 不可预知任务用静态 | 计划迅速过时 | Plan-Execute |
| Executor 用前沿模型 | 成本翻倍 | 执行器降档 |
| 无 Reflect | 跑偏到结束 | PER 循环 |
| ReWOO 变量不传递 | 步骤结果丢失 | #En 占位 |
| 无终止判定 | 步骤耗尽仍不收尾 | 最后一步产出答案 |
| 无计划版本 | 重规划无法回放 | 版本链（03 篇） |

> 🎯 核心要点：模式选择是规划器的"架构决策"——**先问"可预知吗"，再选模式；能静态别动态，该动态别硬静态**；生产常用混合（计划 DAG + 步骤动态），两头收益都拿。

## 8. 常见误区

| 误区 | 真相 |
|---|---|
| "动态规划永远更好" | 可预知任务动态=每步白烧 LLM 调用 |
| "ReWOO 省 token 就万能" | 不能应变，观察驱动任务必翻车 |
| "执行器也用前沿模型" | 执行是模式化工作，14B 开源够用 |
| "PER 就是 ReAct 改名" | PER 有显式计划与验证，ReAct 无 |
| "模式选一次定终身" | 执行中可按信号动态切换 |
| "无 Reflect 也能跑" | 跑偏到结束才发现的代价更高 |

## 9. 模式组合实战（两个典型配方）

| 配方 | 组合 | 适用 |
|---|---|---|
| 调研配方 | 计划 DAG（并行搜索）→ 动态汇总 → 收尾 | 研究报告类 |
| 流水线配方 | 静态 ReWOO 全链 → 失败时切换动态 | 批量处理类 |

```text
调研配方示例（省与活的结合）：
  Planner 一次出 4 路并行搜索 DAG（ReWOO 执行，无 LLM）
  → 结果汇聚后 → 进入动态阶段（Plan-Execute，观察驱动分析）
  → Solver 收尾（1 次 LLM 汇总）
  合计 ~3 次 LLM 调用 vs 全动态 ~12+ 次
```

## 10. 面试速记

| 问题 | 一句话答案 |
|---|---|
| Plan-Execute vs ReWOO？ | 动态（观察驱动，贵活）vs 静态（一次 DAG，省 20 倍不能应变） |
| 怎么选？ | 问"计划能否预先写全"——能 ReWOO，不能 Plan-Execute |
| PER 是什么？ | Plan-Execute-Reflect：Reflect 评估进度决定继续/重规划/收尾 |
| 执行器用便宜模型的理由？ | 执行是模式化工作，规划质量决定上限执行成本决定下限 |
| 混合模式怎么做？ | 计划 DAG（静态执行）+ 关键步骤动态（重规划）——两头收益 |
| ReWOO 的关键机制？ | #En 变量占位传递结果，步骤间无 LLM 判断 |
| 动态切换的依据？ | 执行中发现可预知 → 转 ReWOO（省）；频繁偏离 → 转动态（稳） |
| 模式选择的成本量级？ | ReWOO ~2 次 LLM 调用 vs Plan-Execute 每步 1 次（10 步任务差 5-10 倍） |
| PER 的 Reflect 是什么？ | 轻量进度评估（继续/重规划/收尾）——不是完整反思，是规划器内置检查 |

---

**下一模块**：[05-重规划：观察驱动的计划修正](05-重规划：观察驱动的计划修正.md)　**返回总览**：[00-Planner 规划器总览](00-Planner规划器总览.md)

## 参考来源

- [Plan-Execute Agent（CloudWeGo Eino）](https://www.cloudwego.io/docs/eino/core_modules/eino_adk/agent_implementation/plan_execute/)
- [Plan-execute-reflect agents（OpenSearch）](https://docs.opensearch.org/latest/ml-commons-plugin/agents-tools/agents/plan-execute-reflect/)
- [Beyond ReAct: A Planner-Centric Framework（AAAI 2026）](https://ojs.aaai.org/index.php/AAAI/article/view/40676)
- [Choosing a Reasoning Strategy（Reactive Agents）](https://docs.reactiveagents.dev/guides/choosing-strategies/)
