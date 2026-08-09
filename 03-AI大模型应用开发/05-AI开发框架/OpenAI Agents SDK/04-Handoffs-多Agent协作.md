# 04 Handoffs：多 Agent 协作

> SDK 多 Agent 的核心机制：Handoffs 是一等字段（Agent 直接声明可交接对象）、Typed Handoffs 传递结构化数据、Agents as Tools 让子 Agent 作工具——"交接是 SDK 的灵魂"。

## 📚 目录

1. [为什么交接是核心](#1-为什么交接是核心)
2. [Handoffs 机制](#2-handoffs-机制)
3. [Typed Handoffs](#3-typed-handoffs)
4. [Agents as Tools](#4-agents-as-tools)
5. [多 Agent 架构模式](#5-多-agent-架构模式)
6. [交接的评估](#6-交接的评估)
7. [面试高频问法](#7-面试高频问法)
8. [Handoffs 工程细节](#8-handoffs-工程细节)
9. [常见误区](#9-常见误区)

## 1. 为什么交接是核心

### Swarm 思想的延续

```
Swarm 的核心思想：Agent 之间"移交控制权"（handoffs）
SDK 把它生产级化：一等字段 + 类型化 + 可追踪
"交接"成为 SDK 多 Agent 编排的灵魂
```

### 交接 vs 工具调用

| 维度 | 工具调用 | Handoffs |
|---|---|---|
| 语义 | "用一下这个能力" | "这件事交给你了" |
| 控制权 | 调用完继续 | 移交后由对方主导 |
| 上下文 | 结果回传 | 完整对话移交 |
| 适用 | 单步能力 | 角色分工 |

## 2. Handoffs 机制

### 基本用法

```python
from agents import Agent, Runner

# 1. 定义可交接的 Agent（子角色）
policy_agent = Agent(
    name="PolicyAgent",
    instructions="你是政策专家：回答退换货政策问题。",
)

# 2. 主 Agent 声明 handoffs（一等字段）
sales_agent = Agent(
    name="SalesAgent",
    instructions="你是销售助手：政策问题交接给 PolicyAgent。",
    handoffs=[policy_agent],        # 一等字段（非工具包装）
)

# 3. 运行（模型决定是否交接）
result = Runner.run_sync(sales_agent, "退换货政策是什么？")
print(result.final_output)          # PolicyAgent 的回答
print(result.final_agent.name)      # PolicyAgent（可验证谁答的）
```

### 交接流程

```
模型调用 handoff 工具 → Runner 切换 Agent（保留对话）
→ 新 Agent 继续 → 直到产出最终输出
（历史分区：v0.19 修复嵌套交接的历史管理）
```

### 交接设计要点

```
① instructions 写清"什么情况交接给谁"（模型决策依据）
② 交接对象要"专"：一个 Agent 一个领域
③ final_agent 验证交接是否按预期发生
```

## 3. Typed Handoffs

### 解决什么

```
普通交接：新 Agent 要"总结"上下文（信息损失 + token 浪费）
Typed：结构化数据直接传递（免总结）
```

### 用法

```python
from pydantic import BaseModel
from agents import Agent, handoff

class OrderInfo(BaseModel):
    order_id: str
    amount: float
    status: str

# 定义带输入类型的交接（传递结构化数据）
order_handoff = handoff(
    order_agent,
    input_type=OrderInfo,        # 交接 payload 的 schema
)

# 主 Agent 使用
sales_agent = Agent(
    name="SalesAgent",
    instructions="订单查询交接给 OrderAgent，附订单信息。",
    handoffs=[order_handoff],
)
```

### Typed 的价值

```
① 消除"总结一切"的反模式（信息保真）
② 结构化 payload（Pydantic 校验）
③ 下游 Agent 直接消费结构化数据
```

## 4. Agents as Tools

### 是什么

```
父 Agent 将子 Agent 作为工具调用（非 handoff）：
子 Agent 执行 → 结果返回父 Agent → 父 Agent 继续
```

### 与 Handoffs 的区别

| 维度 | Handoffs | Agents as Tools |
|---|---|---|
| 控制权 | 移交（对方主导） | 保留（父 Agent 主导） |
| 上下文 | 完整对话 | 子 Agent 指令工具对父不可见 |
| 适用 | 领域切换 | 子任务执行 |
| 嵌套 | 支持 | 支持（多层） |

### 用法

```python
from agents import Agent, Runner
from agents.tools import agent_as_tool

research_agent = Agent(
    name="ResearchAgent",
    instructions="调研并返回结论摘要。",
)

# 子 Agent 包装为工具
research_tool = agent_as_tool(research_agent)

main_agent = Agent(
    name="MainAgent",
    instructions="需要调研时调用 research 工具。",
    tools=[research_tool],
)
```

### 使用场景

```
① 子任务委派（检索/计算/调研）
② 多级嵌套（父→子→孙）
③ 与 RAG 组合（检索 Agent 作工具）
```

## 5. 多 Agent 架构模式

### 模式一：领域交接（客服）

```
Router/接待 Agent
  ├─ handoff → 政策 Agent（政策问题）
  ├─ handoff → 订单 Agent（订单问题）
  └─ handoff → 投诉 Agent（投诉问题）
```

### 模式二：主管-下属（工具）

```
主管 Agent
  ├─ tool → 检索 Agent（子任务）
  ├─ tool → 分析 Agent
  └─ 汇总产出
```

### 模式三：流水线（顺序交接）

```
需求 Agent → 设计 Agent → 实现 Agent → 评审 Agent
（每步交接，类似 RAG 阶段 3 的轮流模式）
```

### 模式选型

```
领域分明 → Handoffs（交接）
子任务明确 → Agents as Tools（委派）
固定流程 → 流水线交接（顺序）
```

## 6. 交接的评估

### 评估什么

```
交接正确性（HandoffCorrectness）：
模型是否把任务交接给了正确的 Agent？
评分建议：≥0.90（不是只评最终字符串）
```

### 为什么单独评估交接

```
最终答案对 ≠ 交接正确：
可能主 Agent 自己硬答（不该接的接了）
交接错误 → 下游全错（结构性问题）
```

### 评估方法

```
① 构造"应交接"测试集（领域标记）
② 检查 final_agent 是否等于期望 Agent
③ 统计交接准确率（≥0.90）
④ 错误交接分析（模型为何选错）
```

## 7. 面试高频问法

| 问题 | 回答要点 |
|---|---|
| Handoffs 是什么？ | 移交控制权（一等字段，非工具包装） |
| 与工具调用区别？ | 控制权移交 vs 单步能力调用 |
| Typed Handoffs？ | 结构化数据传递（input_type schema） |
| Agents as Tools？ | 子 Agent 作工具（父保留控制） |
| 交接怎么评估？ | HandoffCorrectness ≥0.90 |
| 三种多 Agent 模式？ | 领域交接/主管下属/流水线 |

### 面试加分表达

> "Handoffs 是 SDK 的灵魂：Agent 一等字段声明可交接对象，模型按指令决定交接——领域问题移交领域 Agent。Typed Handoffs 用 input_type 传递结构化数据，消除'总结一切'的反模式。评估要单独测交接正确性（HandoffCorrectness ≥0.90），最终答案对不代表交接对。"

## 8. Handoffs 工程细节

### 历史分区（v0.19 修复）

```
嵌套交接的历史管理：
主 Agent 历史与子 Agent 历史分区保存
避免上下文混淆（谁说了什么要清楚）
```

### 交接的输入过滤

```
handoff 可配 input filter：
控制交接时传递哪些历史（防上下文膨胀）
v0.19 修复 falsey 值处理
```

### 交接的观测

```
Tracing 记录 HandoffSpanData：
何时交接 / 交给谁 / 传递了什么
（可观测 = 交接问题可定位）
```

### 交接设计检查单

```
① 指令写清交接条件（模型决策依据）
② 交接对象领域单一（一个 Agent 一件事）
③ Typed 优先（结构化传递免总结）
④ 评估 HandoffCorrectness ≥0.90
⑤ 追踪确认（final_agent 验证）
```

## 9. 常见误区

| 误区 | 真相 |
|---|---|
| "Handoffs 是工具" | 一等字段（控制权移交） |
| "交接要总结" | Typed Handoffs 免总结（结构化） |
| "子 Agent 越多越好" | 每个交接是决策点——够用即可 |
| "交接后父 Agent 结束" | 取决于模式（Agents as Tools 父继续） |
| "最终答案对 = 交接对" | 要单独评估交接正确性 |

> 🎯 核心要点：Handoffs = 移交控制权（一等字段）；Typed 结构化传递（免总结）；Agents as Tools = 子 Agent 作工具（父保留控制）；三种多 Agent 模式（领域交接/主管下属/流水线）；交接正确性要单独评估（≥0.90）；工程细节（历史分区/输入过滤/HandoffSpanData 追踪）；final_agent 可验证交接。

---

**下一模块**：[05-Guardrails-安全护栏](05-Guardrails-安全护栏.md) / **返回总览**：[00-OpenAI-Agents-SDK知识体系总览](00-OpenAI-Agents-SDK知识体系总览.md)
