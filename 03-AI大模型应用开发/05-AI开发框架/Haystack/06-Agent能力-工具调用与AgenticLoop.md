# 06 Agent 能力：工具调用与 AgenticLoop

> Haystack 的 Agent 是一等公民：完整工具调用循环（模型→工具→模型）、四种工具形态（Tool/ComponentTool/PipelineTool/Toolset）、2.10 新增 AgenticLoop——偏保守务实的 Agent 实现。

## 📚 目录

1. [Agent 设计理念](#1-agent-设计理念)
2. [工具调用循环](#2-工具调用循环)
3. [四种工具形态](#3-四种工具形态)
4. [Agent 使用示例](#4-agent-使用示例)
5. [AgenticLoop（2.10+）](#5-agenticloop210)
6. [MCP 与 HITL](#6-mcp-与-hitl)
7. [面试高频问法](#7-面试高频问法)
8. [Agent 管道化示例](#8-agent-管道化示例)
9. [常见误区](#9-常见误区)

## 1. Agent 设计理念

### 偏保守务实

```
Haystack 的 Agent 哲学：
代码可预测、行为可解释、管道可审查
（延续框架整体的生产导向）
```

### 与其他框架的 Agent 对比

| 框架 | Agent 风格 |
|---|---|
| Haystack | 工具调用循环，保守务实，可嵌套 |
| LangGraph | 状态机，深度可定制 |
| AutoGen | 对话式多 Agent |
| OpenAI Agents SDK | 托管式流程 |

### Agent 在 Haystack 中的定位

```
Agent = 普通组件（可嵌进更大管道）
嵌套：管道 → Agent → 管道工具（PipelineTool）→ 子管道
```

## 2. 工具调用循环

### 循环机制

```
模型 → 生成工具调用（函数名 + 参数）
→ 框架执行工具
→ 结果回传模型
→ 模型继续（再调用或给出最终答案）
→ 直到无工具调用/达到上限
```

### 与手写工具循环的关系

```
原理 = Function Calling 体系的"五步循环"（本仓库 Function Calling 体系）
Haystack 把它封装为 Agent 组件（内部循环，外部一个组件）
```

### 循环的终止

| 终止方式 | 说明 |
|---|---|
| 无工具调用 | 模型给出最终答案（自然出口） |
| 轮次上限 | max_iterations（兜底） |
| 错误处理 | 工具错误转消息回传 |

## 3. 四种工具形态

| 形态 | 是什么 | 适用 |
|---|---|---|
| Tool | Python 函数包装 | 简单函数 |
| ComponentTool | 组件包装为工具 | 检索器/生成器等组件 |
| PipelineTool | 整个管道包装为工具 | 子任务管道 |
| Toolset | 工具分组 | 给 Agent 配一组相关工具 |

### 定义示例

```python
from haystack.components.tools import Tool, ComponentTool, PipelineTool
from haystack.components.agents import Toolset

# 1. Tool：Python 函数
def query_inventory(product_id: str) -> str:
    """查询库存"""
    return f"库存：{100} 件"

inventory_tool = Tool(function=query_inventory, name="query_inventory")

# 2. ComponentTool：检索器组件
retriever_tool = ComponentTool(
    component=retriever,
    name="search_knowledge",
    description="检索知识库",
)

# 3. PipelineTool：子管道
qa_tool = PipelineTool(
    pipeline=qa_pipeline,
    name="ask_sales_question",
    description="回答销售政策问题",
)

# 4. Toolset：分组
sales_tools = Toolset(tools=[inventory_tool, retriever_tool, qa_tool])
```

### 工具设计原则（与 Function Calling 一致）

```
一工具一职责 / description 写清"何时用" / 参数少而精
错误转消息（模型可读） / 幂等（可重试）
```

## 4. Agent 使用示例

```python
from haystack.components.agents import Agent

agent = Agent(
    generator=llm,
    tools=sales_tools,                       # 工具集
    system_prompt=(
        "你是销售助手："
        "查询库存用 query_inventory；"
        "政策问题用 ask_sales_question。"
    ),
    max_iterations=10,                       # 兜底
)

# Agent 是普通组件，可单独运行或进管道
result = agent.run({"messages": [{"role": "user", "content": "A001 有货吗？"}]})
print(result["messages"][-1]["content"])
```

### 使用要点

| 要点 | 说明 |
|---|---|
| system_prompt | 说明工具分工（模型决策依据） |
| max_iterations | 终止兜底（防死循环） |
| 工具描述 | 影响模型"何时调哪个" |

## 5. AgenticLoop（2.10+）

### 是什么

```
2.10 新增的 Agent 循环组件
（提供更明确的循环控制/可观测性，面向生产）
```

### 与 Agent 的关系

```
Agent：核心工具循环组件（基础）
AgenticLoop：进阶循环（更多控制/状态管理）
```

### 意义

```
2026 年 Agent 能力路线：
2.10 之前：Agent 组件（工具循环）
2.10 之后：AgenticLoop（生产化循环）+ MCP + HITL
表明 Haystack 从"RAG 框架"走向"RAG + Agent 框架"
```

## 6. MCP 与 HITL

### MCP 支持（2.10+）

```
Haystack 支持 MCP 工具：
把 MCP 服务器的工具接入 Agent（生态复用）
方式：MCP 工具 → Tool 包装 → Agent tools
```

### HITL（人工介入）

```
First-class HITL（2.10+）：
- 工具可标记"需要人类确认"
- 执行前暂停，等待人工批准
- 对应"审批是防线"原则（高风险操作人审）
```

### 安全基线（回顾 Function Calling 体系）

```
① 工具白名单（只注册需要的）
② 危险工具 HITL（写库/发消息/花钱）
③ 轮次兜底（max_iterations）
④ 错误转消息（不抛异常）
⑤ 审计日志（每次调用记录）
```

## 7. 面试高频问法

| 问题 | 回答要点 |
|---|---|
| Haystack Agent 是什么？ | 工具调用循环组件（模型→工具→模型） |
| 四种工具形态？ | Tool/ComponentTool/PipelineTool/Toolset |
| 与 LangGraph Agent 区别？ | 保守务实可嵌套 vs 状态机深度定制 |
| AgenticLoop 是什么？ | 2.10+ 进阶循环组件 |
| HITL 怎么做？ | 工具标记人工确认，执行前暂停 |
| 为什么 Agent 是组件？ | 可嵌进更大管道（管道→Agent→管道工具） |

### 面试加分表达

> "Haystack 的 Agent 设计延续它的生产哲学：工具调用循环封装为普通组件，可嵌套进更大管道（PipelineTool 让子管道成为工具）；2.10 加了 AgenticLoop、MCP 和 First-class HITL——危险操作标记人工确认。相比 LangGraph 的深度定制，它更保守但更可预测。"

## 8. Agent 管道化示例

```python
"""Agent 嵌入管道：检索工具 + 决策 + 生成"""
from haystack import Pipeline
from haystack.components.agents import Agent
from haystack.components.tools import ComponentTool

# 1. 检索器包装为工具
retriever_tool = ComponentTool(
    component=retriever,
    name="search_knowledge",
    description="检索知识库，参数 query 是问题文本",
)

# 2. Agent（工具循环）
agent = Agent(
    generator=llm,
    tools=[retriever_tool],
    system_prompt=(
        "你是知识库问答助手："
        "先调用 search_knowledge 获取资料，再基于资料回答；"
        "资料不足时说明'知识库中没有'。"
    ),
    max_iterations=5,
)

# 3. Agent 是组件 → 可进更大管道
pipeline = Pipeline()
pipeline.add_component("router", ConditionalRouter(...))   # 简单问题直答
pipeline.add_component("agent", agent)                     # 复杂问题走 Agent
pipeline.connect("router.agent", "agent.messages")
```

### 管道化价值

```
① 路由分流：简单问题不进 Agent（省成本）
② 组合：Agent 与其他组件共存（管道即图）
③ 可观测：Agent 调用在 OTel 追踪里可见
```

## 9. 常见误区

| 误区 | 真相 |
|---|---|
| "Agent 取代管道" | Agent 是管道的一个组件（可嵌套） |
| "工具越多越好" | 工具多 = 决策难 + 上下文大（按需注册） |
| "max_iterations 可不设" | 必须设（死循环兜底） |
| "Agent 一定比管道强" | 简单流程管道更可控更便宜 |
| "HITL 拖慢一切" | 只对高危操作设（低频） |

> 🎯 核心要点：Agent = 工具调用循环组件（保守务实、可嵌套）；四种工具形态（Tool/ComponentTool/PipelineTool/Toolset）实现"管道→Agent→子管道"嵌套；AgenticLoop/MCP/HITL 是 2.10 的生产化补全；管道化价值 = 路由分流 + 组合 + 可观测；安全基线五条（白名单/HITL/轮次兜底/错误转消息/审计）。

---

**下一模块**：[07-生产化-YAML-Hayhooks与可观测](07-生产化-YAML-Hayhooks与可观测.md) / **返回总览**：[00-Haystack知识体系总览](00-Haystack知识体系总览.md)
