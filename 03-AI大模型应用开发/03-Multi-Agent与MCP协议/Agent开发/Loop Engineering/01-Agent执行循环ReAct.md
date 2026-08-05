# 01 - Agent 执行循环：ReAct 模式

> 🎯 ReAct（Reasoning + Acting）是 AI Agent 执行循环的经典范式。Agent 不是一次问答，而是 Think → Act → Observe 的多轮迭代——这正是"循环工程"的最核心单元

---

## 目录

1. [ReAct 核心循环](#1-react-核心循环)
2. [设计决策](#2-设计决策)
3. [代码实现](#3-代码实现)
4. [常见问题与优化](#4-常见问题与优化)

---

## 1. ReAct 核心循环

### 1.1 循环定义

```text
ReAct = Reasoning + Acting 的交替循环

初始状态：
  user_query = "北京今天天气怎么样？适合户外运动吗？"

循环开始：
  Step 1:
    Thought: 我需要查询北京的天气
    Action:  get_weather("北京")
    Observe: {"temp": 22, "condition": "晴", "wind": "微风"}

  Step 2:
    Thought: 天气晴朗，温度适宜，微风 → 适合户外运动
    Action:  (无，推理足够)
    Final:   北京今天晴，22°C，微风，非常适合户外运动！
```

### 1.2 循环状态机

```text
                    ┌──────────┐
         ┌─────────→│  THINK   │←─────────┐
         │          └────┬─────┘          │
         │               │                │
    需要更多信息      直接可回答        工具结果返回
         │               │                │
         │          ┌────▼─────┐    ┌────┴─────┐
         └──────────│   ACT    │───→│ OBSERVE  │
                    └──────────┘    └──────────┘
                         │
                    超出max_steps
                         │
                    ┌────▼─────┐
                    │  TIMEOUT  │
                    └──────────┘
```

| 状态 | LLM 角色 | 输出 |
|------|---------|------|
| **Think** | 分析当前信息，决定下一步 | 推理文本 / "需要调用工具X" |
| **Act** | 生成工具调用参数 | `<tool_call>` JSON |
| **Observe** | (不参与，由外部执行) | 工具返回结果 |
| **Final** | 综合所有信息 | 最终自然语言答案 |

## 2. 设计决策

### 2.1 终止条件设计

```text
循环终止的三类条件：

1. 自然终止（最理想）
   模型输出不包含任何工具调用 → 任务完成

2. 安全上限（兜底）
   max_steps = 10    ← 超过 10 轮强制终止
   timeout = 300s    ← 超过 5 分钟强制终止

3. 异常终止（保护）
   连续 3 次相同工具调用 → 死循环检测 → 终止
   Token 预算耗尽 → 返回已完成的部分结果
```

### 2.2 关键参数调优

| 参数 | 默认 | 调优建议 |
|------|:---:|------|
| `max_steps` | 10 | 简单任务 3-5，复杂任务可到 20 |
| `timeout` | 300s | API 延迟 × max_steps × 1.5 |
| `temperature` | 0.3 | Function Calling 场景低温度更稳定 |
| `max_tokens_per_step` | 1024 | 工具调用 512 够，推理 2048+ |

### 2.3 上下文管理

```text
每轮追加的内容：
  Thought: "需要查询天气..."
  Action:  get_weather("北京")
  Observation: {"temp": 22, ...}

→ 10 轮后上下文 = 初始 Prompt + 10 × (T + A + O)

问题：上下文线性膨胀
解决：
  1. 早期轮次 → 摘要压缩（而非原始保留）
  2. Observation 只保留关键字段（不要 dump 整个 API 响应）
  3. 设 max_context_length 保护
```

## 3. 代码实现

### 3.1 最小 ReAct Agent

```python
class ReActAgent:
    def __init__(self, llm, tools, max_steps=10):
        self.llm = llm
        self.tools = tools
        self.max_steps = max_steps

    def run(self, query: str) -> str:
        messages = [{"role": "user", "content": query}]
        steps = []

        for step in range(self.max_steps):
            # 1. Think + Act（LLM 决定）
            response = self.llm.chat(messages, tools=self.tools)

            # 2. 检查终止
            if not response.has_tool_calls:
                return response.content   # ← 最终答案

            # 3. 执行工具
            for tc in response.tool_calls:
                result = self.tools.execute(tc.name, tc.arguments)
                steps.append({
                    "step": step,
                    "tool": tc.name,
                    "args": tc.arguments,
                    "result": result
                })

            # 4. Observe（回填结果）
            messages.append({"role": "assistant", "content": response})
            for tc, result in zip(response.tool_calls, results):
                messages.append({
                    "role": "tool",
                    "tool_call_id": tc.id,
                    "content": str(result)
                })

        return "任务未在 {} 步内完成".format(self.max_steps)
```

### 3.2 带安全保护的增强版

```python
import time

class RobustReActAgent(ReActAgent):

    def run(self, query: str) -> str:
        start_time = time.time()
        last_actions = []           # 死循环检测
        token_count = 0

        for step in range(self.max_steps):
            # --- 安全检查 ---
            if time.time() - start_time > 300:
                return "⚠️ 超时（300s），已执行 {} 步".format(step)

            if len(last_actions) >= 3 and len(set(last_actions[-3:])) == 1:
                return "⚠️ 检测到死循环，最后 3 步重复调用 {}".format(last_actions[-1])

            # --- 正常流程 ---
            response = self.llm.chat(messages, tools=self.tools)
            token_count += response.usage.total_tokens

            if token_count > 50000:
                return "⚠️ Token 预算耗尽"

            if not response.has_tool_calls:
                return response.content

            results = []
            for tc in response.tool_calls:
                result = self.tools.execute(tc.name, tc.arguments)
                results.append(result)
                last_actions.append(tc.name)

            messages = self._append_tool_results(messages, response, results)

        return "达到最大步数 {}".format(self.max_steps)
```

### 3.3 迭代 vs 委托

```text
设计决策：什么时候自己迭代？什么时候委托给子 Agent？

迭代 (Iteration)：
  → 同一 Agent，积累上下文
  → 适合：信息逐步获取（查天气 → 查湿度 → 综合判断）
  → 限制：max_steps

委托 (Delegation)：
  → 子 Agent，独立上下文
  → 适合：独立子任务（主 Agent 规划 → 子 Agent 执行代码审查）
  → 限制：max_depth（嵌套层数）
```

## 4. 常见问题与优化

| 问题 | 根因 | 方案 |
|------|------|------|
| **无限循环** | LLM 执着于某个工具 | 3 次重复 → 提示"尝试不同方法" |
| **过早终止** | LLM 没获取足够信息就回答 | System Prompt 中要求"获取所有必要信息后再回答" |
| **上下文爆炸** | 每轮都追加原始 Observation | 观察结果只保留摘要 |
| **工具幻觉** | 调用不存在的工具 | 工具 Schema 中加 `required: true` |
| **步骤太多** | 任务本身需要很多步 | 设置合理的 max_steps + 进度提示 |

## 核心要点回顾

- ReAct = Think（推理）→ Act（行动）→ Observe（观察）→ 循环
- 终止条件三重保障：自然终止 > 安全上限 > 异常检测
- 死循环检测：连续 3 次相同调用 → 提示变通 → 强制终止
- 迭代（自己来）vs 委托（叫别人）的选择：信息连续 vs 任务独立
- 上下文管理是长循环的核心挑战

## 参考资料

1. ReAct 论文 (Yao et al., 2022)
2. Letta V1 Agent Loop 架构 (2025)
3. Claude Code Agent Loop 实现
