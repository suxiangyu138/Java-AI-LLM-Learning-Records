# 04 底层原理：LangGraph 状态机

> 定位：拆开 create_agent 的包装看内部——StateGraph 三要素、create_agent 内部结构解剖、何时手动下沉（2026-08 基准）

## 📚 目录

1. [状态机三要素](#1-状态机三要素)
2. [create_agent 内部结构解剖](#2-create_agent-内部结构解剖)
3. [手写 StateGraph：最小 Agent](#3-手写-stategraph最小-agent)
4. [条件路由：tools_condition 的原理](#4-条件路由tools_condition-的原理)
5. [何时下沉 StateGraph](#5-何时下沉-stategraph)
6. [与手写循环的对应](#6-与手写循环的对应)

## 1. 状态机三要素

LangGraph 的核心抽象只有三个：

```text
State（状态）  ：Agent 运行时共享的数据（如 {"messages": [...]}）
Node（节点）   ：处理函数（如 agent 节点调模型、tools 节点执行工具）
Edge（边）     ：节点间流转（普通边固定走；条件边按函数结果路由）
```

```python
from langgraph.graph import StateGraph, START, END
from typing import TypedDict, Annotated

class AgentState(TypedDict):
    messages: Annotated[list, ...]   # 消息列表（框架内置 reducer 自动追加）

# 节点：普通函数，输入 state，返回要更新的部分
def agent_node(state: AgentState):
    return {"messages": [model.invoke(state["messages"])]}   # 调模型

def tools_node(state: AgentState):
    # 执行模型要求的工具调用（ToolNode 封装）
    ...

# 图：注册节点 + 连线
graph = StateGraph(AgentState)
graph.add_node("agent", agent_node)
graph.add_node("tools", tools_node)
graph.add_edge(START, "agent")          # 入口 → agent
graph.add_conditional_edges(            # agent → 条件路由
    "agent",
    lambda s: "tools" if s["messages"][-1].tool_calls else END,
    {"tools": "tools", "end": END},
)
graph.add_edge("tools", "agent")        # tools → agent（回到循环）
app = graph.compile()
```

> 🎯 **核心要点**：状态机 = **"显式的循环"**。你的阶段 1 while 循环在 LangGraph 里变成图：`START → agent → (条件) → tools → agent → ... → END`——一模一样，只是"可视化"了。

## 2. create_agent 内部结构解剖

`create_agent` 内部自动构建的图（2026 标准 ReAct 拓扑）：

```text
START
  ↓
agent 节点（LLM 调用）
  ↓ 条件路由（tools_condition）
  ├─ 有 tool_calls ──→ tools 节点（ToolNode 批量执行）
  │                      ↓ 结果回传 messages
  │                      └──→ 回到 agent 节点（循环）
  └─ 无 tool_calls ──→ END（最终回答）
```

| 内部组件 | 作用 | 对应你手写的 |
|---------|------|------------|
| agent 节点 | 调模型，追加 assistant 消息 | `chat.completions.create` + append |
| tools 节点（ToolNode） | 解析 tool_calls → 执行 → 错误转回传 | execute_tool + tool 消息追加 |
| tools_condition | 判断是否还有工具调用 | `if not msg.tool_calls: return` |
| 状态（messages） | 全程消息累积 | messages 数组 |
| recursion_limit | 循环上限 | MAX_TURNS |

> 💡 **费曼测试**：create_agent 的每个内部组件都能对应你阶段 1 手写代码里的一段——对不上说明还没吃透（回阶段 1 的 04 篇）。

## 3. 手写 StateGraph：最小 Agent

完整最小示例（理解 create_agent 的替代品）：

```python
from langgraph.graph import StateGraph, START, END, MessagesState
from langgraph.prebuilt import ToolNode
from langchain_openai import ChatOpenAI
from langchain_core.tools import tool

@tool
def get_weather(city: str) -> str:
    """查询指定城市的当前天气。"""
    return f"{city} 22℃ 晴天"

model = ChatOpenAI(model="deepseek-v4-flash",
                   base_url="https://api.deepseek.com", api_key="...")
model = model.bind_tools([get_weather])      # 声明工具（= 原生 tools 参数）

# 节点
def agent(state: MessagesState):
    return {"messages": [model.invoke(state["messages"])]}

# 图
g = StateGraph(MessagesState)
g.add_node("agent", agent)
g.add_node("tools", ToolNode([get_weather]))          # 框架的工具执行节点
g.add_edge(START, "agent")
g.add_conditional_edges("agent", tools_condition)     # 内置条件函数
g.add_edge("tools", "agent")
app = g.compile()

result = app.invoke({"messages": [("user", "北京天气？")]})
print(result["messages"][-1].content)
```

| 与 create_agent 对比 | 手动 StateGraph 多做的 |
|---------------------|----------------------|
| system_prompt | 自己塞进首条 system 消息 |
| checkpointer | 自己传 compile(checkpointer=...) |
| 中间件 | 自己写节点包装 |
| 并行 v2 | 自己处理并行 tool_calls |

## 4. 条件路由：tools_condition 的原理

```python
# langgraph.prebuilt 内置的 tools_condition（概念等价）：
def tools_condition(state: MessagesState):
    last = state["messages"][-1]
    if last.tool_calls:        # 模型要调工具 → 去 tools
        return "tools"
    return END                 # 否则结束

# 正是阶段 1 的：if not msg.tool_calls: return answer
```

> ⚠️ **理解终点**：LangGraph 没有魔法——条件路由就是你手写的 `if not msg.tool_calls`，节点就是你手写的执行函数，状态就是你手写的 messages。

## 5. 何时下沉 StateGraph

| 需求 | create_agent | StateGraph |
|------|:---:|:---:|
| 标准工具循环 | ✅ | ✅（绕远） |
| 人类审批（human-in-the-loop） | ❌ | ✅（interrupt 节点） |
| 多 Agent 交接 | ❌ | ✅（Supervisor 模式） |
| 中途状态检查/注入 | ❌ | ✅（任意节点读 state） |
| 条件重试逻辑 | ⚠️ 钩子能凑 | ✅ |
| 非 ReAct 拓扑（如 Plan-Execute） | ❌ | ✅ |

```text
决策公式：
先用 create_agent（90% 场景）→ 遇到上面任一需求 → 下沉 StateGraph
不要在项目第一天就上 StateGraph（阶段 4/5 会展开多 Agent 与 HITL）
```

> 💡 2026 生产模式（Fintoran 等真实案例）：**LangChain 1.0 create_agent + LangGraph StateGraph（Supervisor + 专业 Agent）**——高层 API 起步，节点级控制处下沉。

## 6. 与手写循环的对应

| 手写代码（阶段 1） | LangGraph 概念 | create_agent 参数 |
|-------------------|---------------|------------------|
| `for turn in range(MAX_TURNS)` | 图执行循环 | recursion_limit |
| `messages.append(msg)` | 状态更新（reducer） | 自动 |
| `if not msg.tool_calls: return` | tools_condition | 自动 |
| `execute_tool` + 回传 | ToolNode | tools 参数 |
| `client.chat.completions.create` | agent 节点 | model 参数 |
| 流式打印 | graph.stream() | 自动支持 |
| 记忆持久化 | checkpointer | checkpointer 参数 |

> 🎯 **核心要点**：状态机不是新知识，是**你手写循环的可视化形式**。学 LangGraph 的正确姿势：先想"这段我手写怎么写"，再看框架怎么表达——两张图重合之日，就是框架真正掌握之时。

---

**返回总览**：[00-阶段总览：LangChain Agent 学习](00-阶段总览：LangChain%20Agent%20学习.md) / **上一模块**：[03-create_agent：官方新 API](03-create_agent：官方新%20API.md) / **下一模块**：[05-记忆与持久化](05-记忆与持久化.md)
