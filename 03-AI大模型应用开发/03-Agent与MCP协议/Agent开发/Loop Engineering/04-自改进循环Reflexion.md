# 04 - 自改进循环：Reflexion 与自我纠错

> 🎯 Reflexion 是 Agent "从错误中学习"的工程实现——执行 → 评估 → 反思 → 改进，四个环节构成一个完整的自改进闭环。这也是 AI Agent 从"能用"进化到"可靠"的关键一步

---

## 目录

1. [Reflexion 核心循环](#1-reflexion-核心循环)
2. [自改进的四种模式](#2-自改进的四种模式)
3. [代码实现](#3-代码实现)
4. [性能实证](#4-性能实证)

---

## 1. Reflexion 核心循环

```text
Reflexion 循环：

  Attempt → Evaluate → Reflect → Retry
  ───┬───   ───┬────   ───┬───   ──┬──
    执行       打分      分析     改进重试

完整的自改进故事：
  Agent 尝试解答：x² + 4x + 4 = 0
  → 输出: x = -4 (错误！)

  Evaluate: 代入验证 16 - 16 + 4 = 4 ≠ 0 ❌
  Reflect:  "我直接给出了 x = -4，但没有用求根公式..."
  Retry:    用求根公式 → x = -2（正确！✅）
```

### 1.1 循环状态机

```text
           ┌──────────┐
           │ ATTEMPT  │ ← 第一次尝试（或改进后重试）
           └────┬─────┘
                │
           ┌────▼─────┐
           │ EVALUATE │ ← 环境反馈 / 自我评估 / 外部校验
           └────┬─────┘
                │
           ┌────▼─────┐    成功
           │  PASS?   │────────→ 返回结果 ✅
           └────┬─────┘
                │ 失败
           ┌────▼─────┐
           │ REFLECT  │ ← 分析失败原因，生成改进计划
           └────┬─────┘
                │
           ┌────▼─────┐
           │  RETRY   │ ← 用反思结果指导重试
           └────┬─────┘
                │
                └──→ 回到 ATTEMPT（最多 3 轮）
```

## 2. 自改进的四种模式

### 2.1 环境反馈型（最可靠）

```text
场景：代码生成

Attempt: 生成代码
Evaluate: 运行测试（客观反馈）
Reflect:  "第 3 行的 list index out of range → 边界条件没处理"
Retry: 加上边界检查

→ 反馈来自客观执行结果，最可靠
```

### 2.2 自我评估型（最常用）

```text
场景：文本生成

Attempt: 写一段产品描述
Evaluate: "作为产品经理，评估这段描述是否清晰、吸引人"
Reflect:  "缺少具体的参数、没有提到独特卖点"
Retry: 加上参数和卖点

→ 反馈来自 LLM 自身判断，可用但需二次验证
```

### 2.3 多轮辩论型（最严谨）

```text
场景：逻辑推理

Agent A: "答案是 X，因为理由 1、2、3"
Agent B: "你的理由 2 有问题，因为..."
Agent A: "你说得对，修正为..."
→ 最终答案（经过辩论验证）
```

### 2.4 外部校验型（最安全）

```text
场景：医疗建议

Attempt: 分析症状给出建议
Evaluate: 对照医学知识库检查事实性
Reflect:  "第 2 条建议与指南冲突"
Retry: 修正建议
→ Human-in-the-Loop 在关键环节确认
```

## 3. 代码实现

```python
class ReflexionAgent:
    def __init__(self, llm, max_rounds=3):
        self.llm = llm
        self.max_rounds = max_rounds
        self.memory = []  # 跨轮反思记忆

    def run(self, task: str) -> str:
        attempt = self._first_attempt(task)

        for round in range(self.max_rounds):
            # 1. 评估
            evaluation = self._evaluate(task, attempt)

            # 2. 检查是否通过
            if evaluation["pass"]:
                return attempt

            # 3. 反思
            reflection = self._reflect(task, attempt, evaluation)
            self.memory.append(reflection)

            # 4. 重试（携带反思记忆）
            attempt = self._retry(task, self.memory)

        return attempt  # 返回最后一轮的结果（即使不完美）

    def _evaluate(self, task: str, attempt: str) -> dict:
        prompt = f"""评估以下任务完成情况：

任务：{task}
结果：{attempt}

请判断：
1. 任务是否完成？(yes/no)
2. 具体问题是什么？
3. 严重程度？(critical/major/minor)

返回 JSON"""
        return self.llm.chat_json(prompt)

    def _reflect(self, task, attempt, evaluation) -> str:
        prompt = f"""任务失败了。请分析根本原因并给出改进方案：

任务：{task}
上次尝试：{attempt}
失败原因：{evaluation['issues']}

反思应包含：
1. 根本原因是什么？
2. 下次应该怎么改进？
3. 需要什么额外信息？
"""
        return self.llm.chat(prompt)
```

### 3.1 带外部校验的精简版

```python
class VerifiedReflexionAgent(ReflexionAgent):

    def _evaluate(self, task: str, attempt: str) -> dict:
        # 先自评
        self_eval = super()._evaluate(task, attempt)

        # 如有外部校验器（如代码运行、数学验算），追加校验
        if hasattr(self, 'external_validator'):
            ext_result = self.external_validator(attempt)
            self_eval["pass"] = self_eval["pass"] and ext_result["pass"]
            self_eval["issues"] += "\n" + ext_result.get("error", "")

        return self_eval
```

## 4. 性能实证

```text
Reflexion 在多个 Benchmark 上的提升：

HumanEval (代码生成)：
  无 Reflexion: 67.0%
  + Reflexion:  88.2%  (+21.2%)

ALFWorld (具身Agent)：
  无 Reflexion: 63.5%
  + Reflexion:  97.3%  (+33.8%)

HotPotQA (问答)：
  无 Reflexion: 34.2%
  + Reflexion:  51.2%  (+17.0%)
```

### 反思轮次 vs 边际收益

```text
轮次   准确率提升    边际收益
Round 1  +15-25%    极高 ← 第一轮反思最有效
Round 2  +5-10%     中等
Round 3  +1-3%      下降
Round 4+ <1%         几乎无效 ← 建议 max_rounds=3
```

## 核心要点回顾

- Reflexion = Attempt → Evaluate → Reflect → Retry 闭环
- 四种评估模式：环境反馈(最可靠) > 自评(最常用) > 辩论(最严谨) > 外部校验(最安全)
- max_rounds=3 最经济：第 1 轮反思收益最大，第 4 轮后边际收益趋零
- 关键实现：跨轮反思记忆积累 + 每轮携带历史教训
- HumanEval 上 Reflexion 提升 +21.2%——这是不换模型直接提升的

## 参考资料

1. Reflexion 论文 (Shinn et al., 2023)
2. Self-Refine 论文 (Madaan et al., 2023)
3. CRITIC 论文 (Gou et al., 2023)
