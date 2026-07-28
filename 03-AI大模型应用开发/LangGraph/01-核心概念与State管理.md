# 01 - 核心概念与 State 管理

> LangGraph 的三个核心抽象：State（状态容器）、Node（计算节点）、Edge（控制流）。State 是灵魂——它既是数据总线，也是图的"停止信号"。

---

## 📚 目录

1. [LangGraph 三要素](#1-langgraph-三要素)
2. [State 深度解析](#2-state-深度解析)
3. [Node 的类型与设计](#3-node-的类型与设计)
4. [Edge 的四种形态](#4-edge-的四种形态)
5. [StateGraph 构建全流程](#5-stategraph-构建全流程)
6. [MessageGraph：消息专用的简化版](#6-messagegraph消息专用的简化版)
7. [State 设计模式与最佳实践](#7-state-设计模式与最佳实践)

---

## 1. LangGraph 三要素

```text
LangGraph = State + Node + Edge

┌──────────────────────────────────────────┐
│              StateGraph                   │
│                                          │
│   ┌──────┐    Edge    ┌──────────┐      │
│   │ Node │───────────→│   Node   │      │
│   │  A   │            │    B     │      │
│   └──────┘            └────┬─────┘      │
│                            │             │
│                      Conditional        │
│                        Edge             │
│                      ┌──┴──┐            │
│                      ▼     ▼            │
│                  ┌────┐ ┌────┐          │
│                  │ C  │ │ D  │          │
│                  └────┘ └────┘          │
│                                          │
│   所有节点共享同一个 State               │
└──────────────────────────────────────────┘
```

| 要素 | 比喻 | 职责 |
|------|------|------|
| **State** | 工作台｜所有节点共享的数据，图执行过程中的"唯一真相源" |
| **Node** | 工人｜接收 State → 处理 → 返回 State 更新 |
| **Edge** | 传送带｜决定数据在节点间如何流转（顺序/分支/并行） |

---

## 2. State 深度解析

### 2.1 State 的本质

```python
# State 是一个带 Reducer 的字典
# Reducer = 当多个节点同时更新同一个 key 时，如何合并

from typing import TypedDict, Annotated
import operator

class AgentState(TypedDict):
    # Annotated[类型, Reducer]
    # operator.add → 追加模式（list 拼接）
    messages: Annotated[list, operator.add]

    # 没有 Annotated → 覆盖模式（后者覆盖前者）
    current_step: str

    # 自定义 Reducer
    tool_results: Annotated[dict, merge_dict]
```

### 2.2 Reducer 机制（最重要！）

```python
from typing import Annotated
import operator

# ===== 内置 Reducer =====

# 1. operator.add — 列表追加（最常用！）
messages: Annotated[list, operator.add]
# 节点A返回 {"messages": [msg1]}
# 节点B返回 {"messages": [msg2]}
# 结果：messages = [msg1, msg2]（拼接，非覆盖）

# 2. 无 Annotated → 最后写入者胜
current_step: str
# 节点A返回 {"current_step": "thinking"}
# 节点B返回 {"current_step": "acting"}
# 结果：current_step = "acting"（覆盖）

# ===== 自定义 Reducer =====

# 合并字典
def merge_dict(left: dict, right: dict) -> dict:
    """后者覆盖同名字段，非同名字段保留"""
    return {**left, **right}

tool_results: Annotated[dict, merge_dict]

# 始终替换（显式覆盖）
def always_replace(old, new):
    return new

# 二元操作（AND / OR）
def logical_or(old: bool, new: bool) -> bool:
    return old or new

error_occurred: Annotated[bool, logical_or]

# 取最大值
def take_max(old, new):
    return max(old, new)

max_score: Annotated[float, take_max]
```

### 2.3 Reducer 决策树

```text
你的 State 字段需要什么合并策略？
│
├── 消息/事件列表 → Annotated[list, operator.add]
│   （保留所有历史）
│
├── 计数/分数 → Annotated[int, lambda a,b: a+b] 或 自定义
│   （累加 / 取最大值）
│
├── 状态标记 → 直接声明，无 Annotated
│   （新值覆盖旧值，如 current_step, is_done）
│
└── 嵌套字典 → 自定义 merge 函数
    （部分更新，如 tool_results）
```

### 2.4 State Schema 三种声明方式

```python
# 方式 1：TypedDict（推荐，风格最 Pythonic）
from typing import TypedDict, Annotated
import operator

class State(TypedDict):
    messages: Annotated[list, operator.add]
    next_step: str

# 方式 2：Pydantic BaseModel（类型校验更强）
from pydantic import BaseModel

class State(BaseModel):
    messages: Annotated[list, operator.add] = []
    next_step: str = "init"

# 方式 3：Dataclass（Python 3.7+）
from dataclasses import dataclass, field

@dataclass
class State:
    messages: Annotated[list, operator.add] = field(default_factory=list)
    next_step: str = "init"
```

> 💡 **推荐**：首选 TypedDict（LangGraph 官方文档风格）。需要强类型校验时用 Pydantic。

---

## 3. Node 的类型与设计

### 3.1 Node 就是一个函数

```python
# Node 签名：接收 State → 返回 State 的增量更新
def my_node(state: AgentState) -> dict:
    """
    参数：当前完整 State
    返回：dict — 只包含你"想更新"的字段（partial update）
    """
    # 读取 State
    msgs = state["messages"]

    # 处理逻辑...
    result = llm.invoke(msgs)

    # 返回增量 — 不要返回整个 State！
    return {
        "messages": [result],      # operator.add 追加
        "current_step": "done"     # 覆盖
    }
```

### 3.2 四种典型 Node

```python
# 1. LLM Node：调用大模型
def call_model(state: AgentState) -> dict:
    """最核心的节点类型"""
    response = llm_with_tools.invoke(state["messages"])
    return {"messages": [response]}

# 2. Tool Node：执行工具
# 使用预构建的 ToolNode
from langgraph.prebuilt import ToolNode

tool_node = ToolNode(tools=[get_weather, search_db, send_email])

# 或者自定义
def execute_tools(state: AgentState) -> dict:
    last_msg = state["messages"][-1]
    results = []
    for tc in last_msg.tool_calls:
        tool = tools_by_name[tc["name"]]
        result = tool.invoke(tc["args"])
        results.append(
            ToolMessage(content=str(result), tool_call_id=tc["id"])
        )
    return {"messages": results}

# 3. Function Node：纯逻辑处理
def validate_input(state: AgentState) -> dict:
    """Node 不一定是 LLM！纯 Python 也 OK"""
    user_msg = state["messages"][-1]
    if len(user_msg.content) < 10:
        return {
            "messages": [AIMessage(content="请提供更详细的信息")],
            "current_step": "rejected"
        }
    return {"current_step": "validated"}

# 4. Subgraph Node：嵌套子图
# 将另一个编译好的 Graph 作为一个 Node
subgraph = sub_agent_graph.compile()
main_graph.add_node("sub_agent", subgraph)
```

### 3.3 Node 的返回值规则

```python
# ✅ 正确：返回 dict，只包含要更新的字段
def good_node(state):
    return {"messages": [new_msg]}  # 只返回变了的部分

# ✅ 正确：可以返回多个 field
def good_node2(state):
    return {
        "messages": [new_msg],
        "current_step": "next"
    }

# ✅ 正确：不修改任何 State（旁路节点）
def side_effect_node(state):
    log_to_mlflow(state)  # 副作用
    return {}  # 或 return None

# ❌ 错误：不要返回完整 State
def bad_node(state):
    state["messages"].append(new_msg)
    return state  # 这会引起非预期行为！
```

---

## 4. Edge 的四种形态

### 4.1 普通边（Normal Edge）

```python
# 固定流转：A 执行完 → 始终 → B
graph.add_edge("node_a", "node_b")

# 多个边：A → B → C
graph.add_edge("node_a", "node_b")
graph.add_edge("node_b", "node_c")
```

### 4.2 条件边（Conditional Edge）

```python
# A 执行完 → 根据 State 动态决定 → B 或 C 或 END
def route_after_llm(state: AgentState) -> str:
    """
    返回值必须是字符串：下一个节点名 或 END
    """
    last_msg = state["messages"][-1]

    if hasattr(last_msg, "tool_calls") and last_msg.tool_calls:
        return "tools"      # → 去执行工具
    elif state.get("error_occurred"):
        return "error_handler"
    else:
        return END          # → 图结束

# 注册条件边
graph.add_conditional_edges(
    "llm",                  # 从哪个节点出发
    route_after_llm,        # 路由函数
    {                       # 路由表（可选，做文档用）
        "tools": "tools",
        "error_handler": "error_handler",
        END: END
    }
)
```

### 4.3 并行边（Parallel / Fan-out）

```python
# Send API：同一个节点并行发到多个目标
from langgraph.graph import Send

def continue_to_experts(state: AgentState):
    """
    返回 Send 对象列表 → 每个 Send 启动一个并行实例
    """
    tasks = state.get("pending_tasks", [])
    return [
        Send("expert_agent", {"task": task})
        for task in tasks
    ]

graph.add_conditional_edges("dispatcher", continue_to_experts)
# dispatcher 执行完 → 并行启动 N 个 expert_agent

# 每个 expert_agent 完成后，结果自动合并到 State
# （通过 defined reducer）
```

### 4.4 START 和 END 哨兵

```python
from langgraph.graph import StateGraph, START, END

graph = StateGraph(AgentState)

# 入口：START → 第一个节点
graph.add_edge(START, "init")

# 出口：最后一个节点 → END
graph.add_edge("final", END)

# 等价写法
graph.set_entry_point("init")  # 设置入口
graph.set_finish_point("final")  # 设置出口（可选）
```

---

## 5. StateGraph 构建全流程

### 5.1 最小可运行示例

```python
from langgraph.graph import StateGraph, START, END
from typing import TypedDict, Annotated
import operator

# ① 定义 State
class State(TypedDict):
    messages: Annotated[list, operator.add]

# ② 定义 Node
def hello(state: State) -> dict:
    return {"messages": [AIMessage(content="Hello, LangGraph!")]}

# ③ 构建 Graph
builder = StateGraph(State)
builder.add_node("hello", hello)
builder.add_edge(START, "hello")
builder.add_edge("hello", END)

# ④ 编译（⚠️ 必须 compile 后才能使用）
graph = builder.compile()

# ⑤ 调用
result = graph.invoke({"messages": [HumanMessage(content="Hi")]})
# result = {
#     "messages": [
#         HumanMessage("Hi"),
#         AIMessage("Hello, LangGraph!")
#     ]
# }
```

### 5.2 编译参数

```python
# 基础编译
graph = builder.compile()

# 带 Checkpointer（启用持久化）
from langgraph.checkpoint.memory import MemorySaver
graph = builder.compile(checkpointer=MemorySaver())

# 带断点（Human-in-the-Loop）
graph = builder.compile(
    checkpointer=MemorySaver(),
    interrupt_before=["tools"],  # 执行 tools 前暂停
    interrupt_after=["llm"]      # 执行 llm 后暂停
)
```

### 5.3 调用方式

```python
# invoke：同步阻塞调用，等待全部执行完
result = graph.invoke(
    {"messages": [HumanMessage("Hello")]},
    {"configurable": {"thread_id": "conversation-1"}}
)

# ainvoke：异步调用
result = await graph.ainvoke(input_dict, config)

# stream：流式返回中间状态
for event in graph.stream(input_dict, config):
    # event 是每一步的 State 快照
    print(event)

# astream：异步流式
async for event in graph.astream(input_dict, config):
    print(event)
```

---

## 6. MessageGraph：消息专用的简化版

### 6.1 vs StateGraph

```python
from langgraph.graph import MessageGraph

# MessageGraph = StateGraph 的预配置版本
# State 固定为：{"messages": Annotated[list, operator.add]}
# Node 的输入/输出必须是：messages 列表

builder = MessageGraph()
builder.add_node("llm", call_llm)       # 输入 [msg1,msg2] → 输出 [reply]
builder.add_node("tools", execute_tools) # 输入 [...,ToolCall] → 输出 [ToolResult]
builder.add_edge("llm", "tools")
builder.add_conditional_edges("tools", should_continue)

graph = builder.compile()
```

```text
StateGraph vs MessageGraph：
┌──────────────────┬─────────────────────┬─────────────────────┐
│      特性         │     StateGraph       │    MessageGraph     │
├──────────────────┼─────────────────────┼─────────────────────┤
│ State 灵活性      │ 任意自定义 Schema     │ 固定 messages list  │
│ 适用场景          │ 复杂 Agent 工作流     │ 简单对话 Agent      │
│ 学习曲线          │ 稍陡（需设计 State）  │ 极低（开箱即用）     │
│ 可扩展性          │ ⭐⭐⭐⭐⭐              │ ⭐⭐⭐               │
│ 推荐              │ 生产/复杂项目         │ 原型/教学            │
└──────────────────┴─────────────────────┴─────────────────────┘
```

> 💡 **建议**：学习时从 MessageGraph 入门，理解概念后立即切换到 StateGraph（它是 LangGraph 的真正形态）。

---

## 7. State 设计模式与最佳实践

### 7.1 经典 State Schema

```python
# 模式 1：通用 Agent State（最常用）
class AgentState(TypedDict):
    messages: Annotated[list, operator.add]    # 对话历史
    current_step: str                          # 当前步骤
    tool_results: Annotated[dict, merge_dict]  # 工具执行结果

# 模式 2：RAG State
class RAGState(TypedDict):
    query: str                                 # 用户查询
    retrieved_docs: Annotated[list, operator.add]  # 检索到的文档
    filtered_docs: list                        # 过滤后的文档
    final_answer: str                          # 最终答案

# 模式 3：Multi-Agent State
class MultiAgentState(TypedDict):
    messages: Annotated[list, operator.add]
    active_agent: str                          # 当前活跃 Agent
    agent_outputs: Annotated[dict, merge_dict] # 各 Agent 输出
    next_agent: str                            # 下一个 Agent
    is_complete: bool                          # 是否完成

# 模式 4：Human-in-the-Loop State
class ApprovalState(TypedDict):
    messages: Annotated[list, operator.add]
    draft: str                                 # AI 生成的草稿
    approved: bool                             # 是否审批通过
    feedback: str                              # 人工反馈
    revision_count: int                        # 修改次数
```

### 7.2 State 设计原则

```text
✅ 原则 1：消息列表用 operator.add
   messages: Annotated[list, operator.add]
   → 永远不会丢消息，这是 LangGraph 的默认选择

✅ 原则 2：单值字段不加 Annotated
   current_step: str
   → 自然覆盖，最新值即当前步骤

✅ 原则 3：复杂聚合用自定义 Reducer
   tool_results: Annotated[dict, custom_merge]
   → 明确合并逻辑

✅ 原则 4：保持 State 扁平
   ❌ state["agent_a"]["output"]["text"]
   ✅ state["agent_a_text"]
   → 扁平 State 更容易理解和调试

✅ 原则 5：用 bool 字段做循环终止条件
   is_complete: bool
   → 在条件边中检查，决定是否结束循环
```

### 7.3 常见错误

```python
# ❌ 错误 1：在 Node 中修改 State 的 list
def bad_node(state: AgentState) -> dict:
    state["messages"].append(new_msg)  # 不要直接修改！
    return {"messages": state["messages"]}  # 整个替换

# ✅ 正确：返回增量
def good_node(state: AgentState) -> dict:
    return {"messages": [new_msg]}  # operator.add 负责追加

# ❌ 错误 2：Redundant return None
def bad_node(state):
    return None  # 会被当作 {}, OK 但不必要

# ✅ 正确：返回空字典
def good_node(state):
    return {}

# ❌ 错误 3：忘记 Reducer 导致消息丢失
class BadState(TypedDict):
    messages: list  # 没有 Annotated！覆盖模式！

# ✅ 正确
class GoodState(TypedDict):
    messages: Annotated[list, operator.add]
```

---

> 🎯 **核心要点**：State 是 LangGraph 的基石，70% 的设计时间花在 State Schema 上。关键决策只有两个——这个字段是 `operator.add`（追加）还是覆盖？我的图什么条件下算"完成"？

---

**下一模块**：[02 - 条件分支与循环控制流](./02-条件分支与循环控制流.md)  
**返回总览**：[00 - LangGraph 知识体系总览](./00-LangGraph知识体系总览.md)
