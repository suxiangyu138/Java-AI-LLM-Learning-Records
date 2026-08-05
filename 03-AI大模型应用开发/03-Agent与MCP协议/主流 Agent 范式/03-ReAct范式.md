# ReAct范式
> Thought-Action-Observation 循环、二十行实现、优劣与适用边界：Agent 构建的基石范式。

---

## 📚 目录

1. [ReAct 原理](#1-react-原理)
2. [核心实现](#2-核心实现)
3. [与其他方法的对照](#3-与其他方法的对照)
4. [优势与局限](#4-优势与局限)
5. [适用边界](#5-适用边界)
6. [工程实践](#6-工程实践)

---

## 1. ReAct 原理

### 1.1 定义

```text
ReAct（Reason + Act）：推理与行动交替的循环
  论文：Yao et al., ICLR 2023

循环结构：
  Thought（推理）→ Action（行动/工具）→ Observation（观察）

三个关键：
  ① Thought：显式推理（当前状态/下一步）
  ② Action：调用工具（或给出最终答案）
  ③ Observation：工具返回结果（喂回推理）

核心价值：
  推理（Thought）与行动（Action）相互增强
  → 思考引导行动、观察修正思考
```

### 1.2 循环的展开

```text
一个 ReAct 循环的示例（信息检索任务）：

  Thought: 我需要查找"张三"的信息
  Action: search(query="张三")
  Observation: 找到 3 条结果，第一条是...
  Thought: 第一条符合需求，需要更多细节
  Action: get(url="...")
  Observation: 页面包含...
  Thought: 信息足够，可以回答了
  Action: finish(answer="...")

关键：每一步的 Thought 都基于前一步的 Observation
  → "scratchpad"（思维记录）随循环增长
```

> 🎯 核心认知：**ReAct = "边想边做"**——推理指导行动（Thought→Action）、观察修正推理（Observation→Thought）。它让 LLM 的工具使用"有理由"而非"瞎试"，是 Agent 构建的最基础范式。

---

## 2. 核心实现

### 2.1 二十行实现（Anthropic 参考）

```python
# 简化版 ReAct 循环（Anthropic 的参考实现 ~20 行）
def react_loop(model, tools, messages, max_steps=10):
    for step in range(max_steps):
        # ① 调用模型（带工具定义 + 历史消息）
        response = model.invoke(messages, tools=tools)

        # ② 有工具调用 → 执行 → 观察 → 继续循环
        if response.tool_calls:
            messages.append(response)
            for call in response.tool_calls:
                result = tools[call.name].invoke(call.args)
                messages.append({
                    "role": "tool",
                    "tool_call_id": call.id,
                    "content": str(result),
                })
            continue

        # ③ 无工具调用 → 模型给出了最终回答 → 结束
        return response.content
```

```text
实现的关键点：
  ① 工具定义（JSON Schema）传给模型
  ② 模型返回 tool_calls（结构化）或直接回答
  ③ 工具结果作为 Observation 喂回
  ④ 步骤预算（max_steps）防止死循环

Anthropic 的经验（Claude Code）：
  "在工具 schema 上花的时间比主提示还多"
  → 工具设计（schema/描述/边界）决定 Agent 质量
```

### 2.2 Java 实现要点

```java
// Java 侧 ReAct 循环（概念等价）
// ① 工具注册（函数描述 + 执行器）
Map<String, Function<String, String>> tools = Map.of(
    "search", query -> searchService.search(query),
    "get_url", url -> httpClient.fetch(url)
);

// ② 循环（LLM 调用 + 工具执行）
for (int step = 0; step < MAX_STEPS; step++) {
    ChatResponse resp = llm.chat(messages, toolDefinitions);
    if (resp.hasToolCalls()) {
        messages.add(resp.asToolMessage());
        for (ToolCall call : resp.toolCalls()) {
            String result = tools.get(call.name()).apply(call.arguments());
            messages.add(toolResult(call.id(), result));  // Observation
        }
    } else {
        return resp.text();   // 最终回答
    }
}
throw new AgentException("超过步骤上限");
```

---

## 3. 与其他方法的对照

### 3.1 ReAct vs 其他推理方法

| 方法 | 机制 | 特点 |
|------|------|------|
| CoT（思维链） | 纯推理（无工具） | 简单但无法获取外部信息 |
| ReAct | 推理 + 工具交替 | 可获取信息、可行动 |
| Plan-and-Execute | 先计划后执行 | 计划显式（04 模块） |
| Reflexion | ReAct + 自我批评 | 失败后反思重试（05 模块） |

```text
论文对比（Yao et al.）：
  ReAct > CoT（HotpotQA/Fever：需要外部信息）
  ReAct > 模仿学习/RL（ALFWorld/WebShop：高 34/10 分）
  → 工具使用是 ReAct 的核心优势

对比 Plan-and-Execute：
  ReAct：每步动态决策（灵活，成本高）
  PnE：计划先行（可审计，可能计划错）
  → 4-5 步分水岭（04 模块）
```

### 3.2 ReAct 与推理模型的演进

```text
2025-2026 的重要变化：
  推理模型（o1/R1/Claude Thinking）内部已有
  "计划-执行-反思"的隐式循环（06 模块）

影响：
  显式 ReAct 循环的必要性降低
  模型内部推理 + 工具调用 = 简化循环
  → Model-Native Harness 趋势（06 模块）

但 ReAct 思想仍然成立：
  推理与行动交替（无论内外）
  工具调用循环（模型或框架执行）
  → ReAct 是"思想"，实现方式在演进
```

---

## 4. 优势与局限

### 4.1 优势

| 优势 | 说明 |
|------|------|
| 简单 | 循环结构清晰（~20 行） |
| 可审计 | Thought 记录可查（推理透明） |
| 适应动态 | 每步根据观察决策 |
| 成本可控 | 简单任务短循环 |
| 工具灵活 | 任意工具接入 |

### 4.2 局限

| 局限 | 说明 |
|------|------|
| 长程漂移 | 20+ 步后推理质量下降 |
| 上下文膨胀 | scratchpad 增长 → 成本/噪声 |
| 循环风险 | 无预算可能死循环 |
| 调试困难 | 长链路错误定位难 |
| 成本随步数增长 | 10 步 ≈ 5 步的 2-3 倍 token |

```text
局限的量化（2025-2026 检索共识）：
  短任务（<5 步）：ReAct 表现好
  长任务（20+ 步）：漂移明显（推理退化）
  → 步数预算 + 中途检查点（缓解）

对比 Plan-and-Execute：
  PnE 的长任务更稳定（计划先行）
  → 长任务选 PnE（04 模块）
```

---

## 5. 适用边界

### 5.1 适用场景

```text
ReAct 适合（Anthropic 建议）：
  ① 步骤无法预先枚举（探索型）
  ② 需要动态决策（每步看结果）
  ③ 任务 < 5 步（短任务）
  ④ 需要可审计推理（Thought 记录）

典型场景：
  信息检索（多步搜索）
  简单工具调用（查询/计算）
  客服对话（动态响应）
  代码小任务（单文件修改）
```

### 5.2 不适用场景

```text
ReAct 不适合：
  ① 长任务（20+ 步）——漂移
  ② 流程可预先计划——用 PnE
  ③ 需要严格可靠——用工作流
  ④ 高成本敏感——计划先行

选择判断（08 模块决策树）：
  "能预先写清单吗？"
    能 → Plan-and-Execute
    不能且 <5 步 → ReAct
    不能且长任务 → 混合（ReAct + 检查点）
```

### 5.3 变体与增强

```text
ReAct 的工程变体：
  ① Bounded ReAct：第 N 步强制检查点/重计划
  ② ReAct + 记忆：历史总结（上下文控制）
  ③ ReAct + 验证：每步输出校验
  ④ ReAct + Reflexion：失败后反思（05 模块）

2025-2026 趋势：
  纯 ReAct 循环逐渐让位于：
    强推理模型 + 简化循环（06 模块）
    或 混合范式（ReAct + PnE 元素）
  → ReAct 是基础，混合是实践
```

---

## 6. 工程实践

### 6.1 生产配置清单

```text
ReAct 生产化五件事：
  ① 步骤预算（max_steps：10-20 上限）
  ② 工具设计（schema 精确、描述清晰）
  ③ 上下文管理（历史压缩/总结）
  ④ 失败处理（工具异常 → 观察返回错误）
  ⑤ 可观测性（每步 Thought/Action 日志）

护栏（08 模块详述）：
  成本预算（token/调用数）
  敏感操作确认（人工检查点）
  工具白名单（严格 schema）
```

### 6.2 调试方法

```text
ReAct 调试三板斧：
  ① 查看 scratchpad（Thought 序列定位漂移点）
  ② 工具调用日志（Action/Observation 核对）
  ③ 分步重放（单步测试）

常见问题定位：
  推理漂移 → 上下文过长（压缩）
  循环 → 预算/退出条件缺失
  工具错用 → schema 设计（描述/边界）
  观察未用 → 提示引导（明确要求利用观察）
```

### 6.3 与框架的结合

```text
框架中的 ReAct：
  LangGraph：ReAct 节点图（可插拔）
  OpenAI Agents SDK：内置工具循环
  LangChain4j（Java）：AiServices 工具调用
  Spring AI：ChatClient 工具支持

注意：
  框架封装了循环 → 理解原理仍必要
  （调试/调优/选型依赖原理理解）
```

> 🎯 **核心要点**：ReAct = "边想边做"的基石范式——Thought 指导 Action、Observation 修正 Thought，二十行实现、可审计、适应动态。适用边界清晰：**<5 步的探索型任务**（短、动态、需工具）；长任务漂移、成本随步数增长是硬伤（Plan-and-Execute 补位）。2025-2026 的演进：推理模型内化循环后，显式 ReAct 简化，但"推理与行动交替"的思想永恒——工程重点转向工具设计与护栏。

---

## 7. 小结

| 问题 | 答案 |
|------|------|
| ReAct 是什么？ | Thought→Action→Observation 交替循环 |
| 核心实现？ | ~20 行（工具调用或直接回答） |
| 对比 CoT？ | ReAct 有工具（可获取外部信息） |
| 优势？ | 简单/可审计/动态适应 |
| 局限？ | 长程漂移/上下文膨胀/成本增长 |
| 适用？ | <5 步探索型任务；长任务用 PnE |

**下一模块**：[04-Plan-and-Execute范式](04-Plan-and-Execute范式.md)　**返回总览**：[00-主流Agent范式知识体系总览](00-主流Agent范式知识体系总览.md)
