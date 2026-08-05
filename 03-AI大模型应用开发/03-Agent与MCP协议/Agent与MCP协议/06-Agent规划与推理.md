# 06 - Agent 规划与推理

> 🎯 规划能力是 Agent 质量的"倍增器" — 同样的工具集，规划好的 Agent 能以一半的步数完成两倍复杂的任务

---

## 目录

1. [规划范式对比](#1-规划范式对比)
2. [Plan-and-Execute](#2-plan-and-execute)
3. [ReWOO：无观察推理](#3-rewoo无观察推理)
4. [反思与自我改进](#4-反思与自我改进)
5. [规划失败的处理](#5-规划失败的处理)

---

## 1. 规划范式对比

| 范式 | 核心思想 | 适用 | 步数 |
|------|----------|------|:---:|
| **ReAct** | 思考-行动-观察交替 | 需要外部信息的任务 | 多 |
| **Plan-Execute** | 先制定计划再执行 | 步骤明确的任务 | 中 |
| **ReWOO** | 计划+占位符，一次性执行 | 高效率场景 | **少** |
| **LLMCompiler** | 并行DAG执行 | 独立子任务 | 最少 |

---

## 2. Plan-and-Execute

```python
# 先规划，再执行
def plan_and_execute(goal, llm, tools):
    # ① 制定计划
    plan_prompt = f"""为以下目标制定执行计划。每个步骤说明需要什么工具和输入。
目标：{goal}

可用工具：{list(tools.keys())}

输出格式：
步骤1: [描述] → 工具: [工具名] → 输入: [参数]
步骤2: [描述] → 工具: [工具名] → 输入: [依赖步骤1的输出]
..."""
    
    plan = llm(plan_prompt)
    steps = parse_plan(plan)
    
    # ② 逐步执行
    results = {}
    for step in steps:
        result = execute_step(step, results, tools)
        results[step.id] = result
        
        # ③ 每步后检查是否按预期
        if not result.success:
            return replan(goal, completed=results, failed_at=step)
    
    return synthesize_results(goal, results, llm)
```

---

## 3. ReWOO：无观察推理

```text
ReWOO (Reasoning WithOut Observation)：
  节省 Token 的规划方式

ReAct 的问题：
  每次 Action 后必须 Observation → 一次 LLM 调用
  N 步任务 = 2N 次 LLM 调用 → Token 消耗大

ReWOO 的改进：
  ① 先做完整计划，用占位符 #E1, #E2 标记每步的输出
  ② 一次性执行所有工具（无 LLM 参与）
  ③ 用执行结果填充占位符 → 最终一次 LLM 调用合成答案

  优势：LLM 调用从 2N 降到 2（规划+合成）→ Token 节省 50%+
```

```python
# ReWOO 示例
def rewoo_execute(goal, llm, tools):
    # ① 规划（一次 LLM 调用）
    plan = llm(f"""制定计划，用 #E{n} 引用前面步骤的结果。
目标：{goal}
计划：
#E1 = search("2026年GDP增长率")
#E2 = calculator(#E1 * 1.05)
""")
    
    # ② 批量执行工具（无需 LLM）
    results = {}
    for step in parse_steps(plan):
        # 替换占位符
        resolved_input = resolve_placeholders(step.input, results)
        results[step.id] = execute_tool(step.tool, resolved_input)
    
    # ③ 合成答案（一次 LLM 调用）
    return llm(f"根据以下结果回答：{goal}\n{results}")
```

---

## 4. 反思与自我改进

```text
Reflexion = Agent 的"复盘能力"

流程：
  ① 执行任务 → 记录过程
  ② 评估结果 → 成功/失败？
  ③ 反思失败原因 → 生成"经验教训"
  ④ 将经验存入长期记忆
  ⑤ 下次类似任务 → 检索经验 → 避免重蹈覆辙
```

```python
def reflexion_loop(task, agent, evaluator, max_attempts=3):
    for attempt in range(max_attempts):
        result = agent.execute(task)
        score = evaluator.evaluate(result)
        
        if score >= 0.8:
            return result
        
        # 反思失败原因
        reflection = agent.reflect(
            f"任务未达标(得分{score})。分析失败原因并改进方案。"
        )
        agent.memory.remember(reflection)  # 存入经验
    
    return "任务无法在最大尝试次数内完成"
```

---

## 5. 规划失败的处理

```text
规划失败的常见处理：

① 超时保护：最大执行步数（如 15 步）→ 超时后返回已完成的部分
② 循环检测：相同操作重复 3 次 → 强制切换策略
③ 降级策略：复杂规划失败 → 退回简单 ReAct
④ 人工介入：关键步骤阻塞 → 请求人工决策
```

---

## 6. 规划与长任务执行

**长任务（Long-running Task）的规划挑战**——2026 年 Agent 从"秒级任务"走向"分钟-小时级任务"：

```text
短任务：单次推理 + 1-2 次工具调用（秒级）
长任务：多步规划 + 大量工具调用 + 跨会话（分钟-小时级）

长任务的三大问题：
① 上下文膨胀：中间结果越积越多 → 超窗口/成本爆炸
   → 中间结果摘要化（每步完成后压缩）
② 中断恢复：任务跑到一半失败/断开
   → MCP Tasks（2026 官方扩展）：返回持久化任务句柄，可断点恢复轮询
③ 进度可观测：用户看不到进展
   → 步骤级状态上报 + 流式输出
```

**规划策略的 2026 选型**：

| 场景 | 策略 | 原因 |
|------|------|------|
| 任务可预分解 | Plan-Execute | 结构清晰、可审核 |
| 未知结构 | ReAct 动态 | 探索式 |
| 长任务 + 可恢复 | **MCP Tasks** | 协议级持久化句柄 |
| 需要人类确认 | 规划中插审批门 | 安全（见 10 号） |

> 🎯 **核心要点**：规划能力 2026 的分水岭 = "**长任务支持**"——上下文管理（摘要化）+ 断点恢复（MCP Tasks）+ 进度可观测三件套，是 Agent 从"Demo"到"生产自动化"的关键（MCP Tasks 细节见 12 号文件）。

---

## 7. 规划质量评估

**规划能力的度量**（2026 实践——规划也要可评估）：

| 维度 | 指标 | 说明 |
|------|------|------|
| 计划正确性 | 步骤覆盖率 | 黄金计划 vs Agent 计划的重合 |
| 执行效率 | 冗余步骤率 | 走弯路/重复调用 |
| 恢复能力 | 失败重规划成功率 | 计划失败后能否重新规划 |
| 资源消耗 | 规划 token 数 | 规划成本（Plan 阶段） |

**规划失败的三种模式与修复**：

```text
① 过度规划：计划太细、执行时条件已变
   → 计划粒度放宽（只定关键节点）+ 执行时再细化
② 规划不足：漏关键步骤
   → 复盘（Reflexion 式）：失败后回溯计划，补漏
③ 计划漂移：执行偏离计划无法拉回
   → 检查点机制：每步与计划比对，偏差超阈值重新规划
```

> 🎯 **核心要点**：规划不是"一次生成计划"而是"**计划-执行-校验-重规划**的闭环"——评估规划质量（覆盖/冗余/恢复）并内置重规划机制，是规划能力的生产化关键。

---

## 核心要点回顾

- ReAct 灵活但 Token 消耗大，ReWOO 效率高但不适合需要反馈的场景
- Plan-Execute 适合步骤明确的任务
- Reflexion = 从失败中学习 → 持续改进
- 规划必须有兜底：超时/循环检测/降级/人工介入
