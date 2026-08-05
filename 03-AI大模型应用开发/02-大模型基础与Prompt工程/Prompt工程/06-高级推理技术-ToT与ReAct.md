# 06 - 高级推理技术：ToT 与 ReAct

> 🎯 CoT 是直线推理，ToT 是树状探索，ReAct 是思考+行动的交替 — 这些高级技术是 Agent 的底层引擎

---

## 目录

1. [Tree-of-Thoughts ToT](#1-tree-of-thoughts-tot)
2. [ReAct：推理 + 行动](#2-react推理--行动)
3. [Reflexion：反思式自我改进](#3-reflexion反思式自我改进)
4. [Plan-and-Solve](#4-plan-and-solve)
5. [技术对比与选型](#5-技术对比与选型)

---

## 1. Tree-of-Thoughts ToT

### 1.1 原理

```text
ToT (Tree of Thoughts) = 探索多条推理路径，选择最优

CoT：一条直线 → 只探索一种思路
ToT：一棵树 → 每个节点探索多条思路 → 选最优 → 继续探索

流程：
  ① 生成候选思路（BFS/DFS 扩展）
  ② 评估每条思路的可行性
  ③ 选择最有希望的思路深入
  ④ 回溯或继续，直到找到满意答案
```

### 1.2 ToT Prompt 示例

```text
ToT Prompt（解决 24 点游戏：用 4, 9, 10, 13 算出 24）：

  "我们要用数字 4, 9, 10, 13 通过加减乘除算出 24。
   
  第一步：请提出 3 种不同的第一步运算方案。
  对每个方案，给出中间结果和剩余的数字。
   
  方案 1：...
  方案 2：...
  方案 3：...
   
  第二步：评估每个方案是否能得到 24。
  选择最有希望的 2 个方案继续。
   
  第三步：对选中的方案，继续探索下一步运算。
  ...直到得到 24 或确定不可能。"
```

### 1.3 ToT vs CoT

| 维度 | CoT | ToT |
|------|:---:|:---:|
| **探索方式** | 单链 | 多链树状 |
| **能否回溯** | ❌ | ✅ |
| **适用任务** | 有明确步骤的推理 | 需要探索+选择的推理 |
| **Token 消耗** | 低 | 高（多路径 × 多次调用） |
| **延迟** | 低 | 高 |

---

## 2. ReAct：推理 + 行动

### 2.1 原理

```text
ReAct = Reasoning + Acting = 思考 + 行动交替

传统 CoT：只在脑中推理 → 无法获取外部信息
ReAct： 思考 → 行动（查资料/调API）→ 观察结果 → 再思考 → 再行动

这是 AI Agent 的核心范式！

循环模式：
  Thought: 我需要知道 X 才能回答这个问题
  Action: 搜索 "X"
  Observation: 搜索结果显示...
  Thought: 根据搜索结果，答案是...
  Action: 给出最终回答
```

### 2.2 ReAct Prompt 模板

```text
ReAct Prompt（带工具调用的 Agent）：

  "你可以使用以下工具：
  - search(query): 搜索互联网
  - calculator(expr): 计算数学表达式
  - get_weather(city): 查询天气

  请用以下格式回答：

  Question: [用户问题]
  Thought: [你的思考过程]
  Action: [工具名称][工具输入]
  Observation: [工具返回结果]
  ... (可重复 Thought-Action-Observation)
  Thought: 我现在有足够的信息回答
  Final Answer: [最终回答]

  ---

  Question: 北京今天的天气适合户外运动吗？
  Thought: 我需要先查北京今天的天气
  Action: get_weather[北京]
  Observation: 北京今天晴天，温度 22°C，风力 2 级
  Thought: 天气条件很好，适合户外运动
  Final Answer: 北京今天晴天，22°C，微风，非常适合户外运动！"
```

### 2.3 ReAct 与 Function Calling 的关系

```text
ReAct Prompt：用自然语言描述 Thought-Action-Observation 循环
  → 手工编排，灵活但需要自己解析 Action

Function Calling (OpenAI / Claude)：
  → LLM 原生支持工具调用
  → 返回结构化的函数名+参数
  → 更可靠，推荐使用

两者本质相同：让 LLM 决定何时调用外部工具
Function Calling 是 ReAct 的"工业化版本"
```

---

## 3. Reflexion：反思式自我改进

### 3.1 原理

```text
Reflexion = 执行任务 → 自我评估 → 反思失败原因 → 重试

  ① Actor：执行任务（如写代码）
  ② Evaluator：评估结果（如运行测试）
  ③ Self-Reflection：分析失败原因 → 生成"经验教训"
  ④ 带着经验重新执行 → 迭代改进

与人类学习类比：
  做题 → 对答案 → 看错题分析 → 下次不犯同样错误
```

```text
Reflexion Prompt：

  "上次你写的代码没有通过测试，错误信息如下：
  {test_error}
  
  请反思失败原因：
  1. 你误解了需求的哪个部分？
  2. 你忽略了什么边界条件？
  3. 下次如何避免？
  
  基于以上反思，重新编写代码。"
```

---

## 4. Plan-and-Solve

### 4.1 原理

```text
Plan-and-Solve = 先制定计划，再按计划执行

CoT：边想边做（可能走偏）
Plan-and-Solve：先想好全盘计划，再逐步执行

步骤：
  ① 分析问题 → 制定详细计划（步骤列表）
  ② 按计划顺序执行每一步
  ③ 汇总结果

优点：对多步骤复杂任务更可靠（全局视角）
```

```text
Plan-and-Solve Prompt：

  "问题：设计一个支持百万并发的短链接服务

  第一步：先制定完整计划
  请输出一个编号的执行计划，覆盖以下维度：
  - 短链接生成算法
  - 存储方案
  - 高并发方案
  - 扩展性设计
  
  第二步：对计划中的每个步骤，逐一展开详细设计
  请先确认计划是否合理，再开始执行。"
```

---

## 5. 技术对比与选型

| 技术 | 核心思想 | 适用场景 | Token 消耗 |
|------|----------|----------|:---:|
| **CoT** | 单链推理 | 数学/逻辑推理 | 低 |
| **ToT** | 树状探索+回溯 | 需要多方案对比 | 高 |
| **ReAct** | 思考+行动交替 | **Agent/工具调用** | 中 |
| **Reflexion** | 自我反思+重试 | 代码生成/调试 | 高 |
| **Plan-and-Solve** | 先计划后执行 | 复杂多步任务 | 中 |

```text
选型建议：
  → 数学/逻辑题 → CoT (最简单)
  → 需要搜索/调用 API → ReAct (Agent 标配)
  → 需要多方案对比 → ToT
  → 代码生成跑不通 → Reflexion
  → 复杂多步骤任务 → Plan-and-Solve
```
