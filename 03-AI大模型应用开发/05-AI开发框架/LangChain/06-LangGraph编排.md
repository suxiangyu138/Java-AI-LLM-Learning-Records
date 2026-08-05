# 06 - LangGraph 编排

> **核心摘要**：LangGraph 是 LangChain 生态的**编排运行时**——显式状态机（StateGraph）、持久化（Checkpointer）、人工介入（HITL）、时间旅行（Time Travel）。与 LangChain 不是替代而是**同一技术栈的两层**：create_agent 底层就是 LangGraph 图，需要 while 循环/多步骤/HITL 时直接用 LangGraph。

> **前置阅读**：[[01-LangChain 1.x生态全景]] | [[05-Agent开发]]

---

## 📚 目录

1. [LangGraph 的定位](#1-langgraph-的定位)
2. [核心概念：State / Node / Edge](#2-核心概念state--node--edge)
3. [第一个 StateGraph](#3-第一个-stategraph)
4. [条件边与循环](#4-条件边与循环)
5. [Checkpointer 持久化](#5-checkpointer-持久化)
6. [HITL 人工介入](#6-hitl-人工介入)
7. [Time Travel 时间旅行](#7-time-travel-时间旅行)
8. [create_agent 与 LangGraph 的关系](#8-create_agent-与-langgraph-的关系)
9. [性能与生产化](#9-性能与生产化)
10. [核心要点](#10-核心要点)

---

## 1. LangGraph 的定位

> **背景**：LCEL/Agent 解决「单次流程」；当需要**跨步骤状态、分支循环、人工审批、断点恢复**时，需要显式的图编排。
> **目的**：掌握「图即程序」的编排思维。
> **适用范围**：5-20 步工作流、while 循环、HITL、多 Agent 协作的底层。

```text
LangGraph 解决什么问题
├── ① 显式状态：每一步之间共享 state（不再靠消息隐式传）
├── ② 循环与分支：条件边让图可以「绕圈」和「分岔」
├── ③ 持久化：Checkpointer 存 checkpoint，崩溃/重启恢复
├── ④ 人工介入：HITL 在指定节点暂停等待审批
├── ⑤ 时间旅行：回退到任意检查点重放
└── ⑥ 流式：节点级流式 + 事件流式

与 LangChain 的分工（官方定位）
├── LangChain = 组件（模型/工具/Prompt）+ 高层 Agent（create_agent）
├── LangGraph = 运行时（状态/持久化/编排）
└── 金句：LangGraph 是 create_agent 的「发动机」——想换挡改装时直接拆开用
```

---

## 2. 核心概念：State / Node / Edge

> 🎯 **图的三要素**：State（状态——图中流转的数据）、Node（节点——每个处理步骤）、Edge（边——节点间的连接与条件）。

```python
# ① State：图的「共享内存」（TypedDict 或 Pydantic）
from typing import Annotated, TypedDict
from langgraph.graph import MessagesState
from langgraph.graph.message import add_messages

class WorkflowState(TypedDict):
    messages: Annotated[list, add_messages]  # 消息自动追加（add_reducer）
    step_count: int                           # 普通字段直接覆盖

# 内置 MessagesState = 含 messages 的常用状态
# from langgraph.graph import MessagesState
```

**三个概念的对应关系**：

| 概念 | 类比 | 说明 |
|------|------|------|
| `State` | 全局变量/数据库记录 | 所有节点共享，节点读写 |
| `Node` | 函数 | 输入 state → 处理 → 返回增量更新 |
| `Edge` | 流程线 | 普通边（直线）/条件边（分支循环） |

**Reducers（状态更新器）**：

```text
state 更新的两种默认行为
├── 覆盖：节点返回 {field: value} → 整个字段被替换
├── 追加（Reducer）：Annotated[list, add_messages]
│   ├── 节点返回的 messages 追加到已有消息列表
│   └── 自定义 reducer：写函数合并（如 sum、dedup）
└── 金句：Reducer 决定「新状态如何并进旧状态」——消息流用 add_messages
```

---

## 3. 第一个 StateGraph

```python
from typing import Annotated, TypedDict
from langgraph.graph import StateGraph, START, END
from langgraph.graph.message import add_messages
from langchain_openai import ChatOpenAI

class State(TypedDict):
    messages: Annotated[list, add_messages]

model = ChatOpenAI(model="gpt-5.5")

# ① 节点：普通函数，入参 state，返回增量
def chatbot(state: State) -> dict:
    return {"messages": [model.invoke(state["messages"])]}

# ② 建图：节点注册 + 连线
graph = StateGraph(State)
graph.add_node("chatbot", chatbot)
graph.add_edge(START, "chatbot")    # 起点 → 节点
graph.add_edge("chatbot", END)      # 节点 → 终点（直线流程）

# ③ 编译 + 执行
app = graph.compile()
result = app.invoke({"messages": [{"role": "user", "content": "你好！"}]})
print(result["messages"][-1].content)
```

**StateGraph 构建五步**：

```text
├── ① 定义 State（TypedDict + Reducer）
├── ② 写节点函数（state → 返回增量）
├── ③ graph.add_node(name, fn) 注册
├── ④ add_edge / add_conditional_edges 连线
├── ⑤ compile() → invoke/stream
└── 金句：函数就是节点，图只是「把它们串起来 + 跑起来」
```

> 💡 **节点返回约定**：返回的 dict 只是「增量更新」——没返回的字段保持不变；返回 None 表示不改状态（如纯副作用节点：发邮件、写日志）。

---

## 4. 条件边与循环

> 🎯 **条件边让图活起来**——模型判断「下一步走哪条路」，这就是 while 循环/分支的本质。

```python
# 场景：客服 Agent——先分类，再路由到对应处理节点
from langgraph.graph import StateGraph, START, END

class State(TypedDict):
    messages: Annotated[list, add_messages]
    route: str

def classify(state: State) -> dict:
    # 伪代码：模型输出 "billing" / "support" / "end"
    return {"route": model.invoke(...).content}

def billing(state: State) -> dict: return {"messages": [...]}
def support(state: State) -> dict: return {"messages": [...]}
def done(state: State) -> dict: return {"messages": [...]}

# 路由函数：根据 state 返回「去哪条边」
def route(state: State) -> str:
    return {"billing": "billing", "support": "support", "end": "done"}[state["route"]]

graph = StateGraph(State)
graph.add_node("classify", classify)
graph.add_node("billing", billing)
graph.add_node("support", support)
graph.add_node("done", done)
graph.add_edge(START, "classify")
graph.add_conditional_edges(            # ← 条件边
    "classify",
    route,                              # 决定走哪条路
    {"billing": "billing", "support": "support", "done": "done"},  # 映射表
)
graph.add_edge("billing", END)
graph.add_edge("support", END)
graph.add_edge("done", END)
```

**条件边三要素**：

| 要素 | 说明 |
|------|------|
| 起点节点 | 条件判断发生在哪个节点之后 |
| 路由函数 | `state → key`（返回字符串 key） |
| 路径映射 | `{key: 目标节点}`——key 决定去哪 |

**while 循环实现**（条件边指回自己）：

```python
def route(state: State) -> str:
    return "retry" if state["quality"] < 0.8 else "finalize"
    # 质量不达标 → 回 "refine" 节点再跑一轮（循环！）

graph.add_conditional_edges("evaluate", route,
    {"retry": "refine", "finalize": "finalize"})
graph.add_edge("refine", "evaluate")    # refine → evaluate → (route) → refine...
```

> ⚠️ **循环必须有退出条件**：无限循环 = 成本灾难。实践：① 路由函数里加「最大轮次」判断（state 中计数）；② 全局超时；③ 节点内 token 预算控制。

---

## 5. Checkpointer 持久化

> 🎯 **Checkpointer 在每个节点执行后自动存 checkpoint**——崩溃恢复、多轮会话、HITL 断点都依赖它（create_agent 的记忆也是它）。

```python
from langgraph.checkpoint.memory import InMemorySaver
from langgraph.checkpoint.postgres import PostgresSaver

# 生产：Postgres 持久化（多实例共享）
checkpointer = PostgresSaver.from_conn_string(
    "postgresql://user:pass@host:5432/langgraph")

app = graph.compile(checkpointer=checkpointer)

# 每次调用带 thread_id → 同一会话状态自动加载
config = {"configurable": {"thread_id": "order-42"}}
app.invoke({"messages": [{"role": "user", "content": "开始处理订单"}]}, config)
app.invoke({"messages": [{"role": "user", "content": "继续下一步"}]}, config)  # 接得上

# 查看某线程的历史检查点
for checkpoint in app.get_state_history(config):
    print(checkpoint)
```

**Checkpointer 的关键属性**：

| 属性 | 说明 |
|------|------|
| 存储粒度 | 每节点执行后一个 checkpoint |
| 读取机制 | thread_id 定位会话，只重放最新 |
| 生产选型 | Postgres（多实例）/ Redis / 自研接口 |
| 崩溃恢复 | 新请求同 thread_id → 从最后 checkpoint 继续 |

> ⚠️ **性能代价（约 68%）**：checkpoint 序列化有开销——**高频循环节点别放图里，放节点内部**（一个循环 = 一个节点，而非 N 个节点）。见本章第 9 节。

---

## 6. HITL 人工介入

> 🎯 **HITL（Human-in-the-Loop）是 LangGraph 的招牌能力**——在指定节点「暂停」，等人工审批后再继续；create_agent 的 HumanInTheLoopMiddleware 底层就是它。

```python
from langgraph.graph import StateGraph, START, END
from langgraph.types import Command, interrupt

def request_payment(state: State) -> dict:
    """敏感操作：请求人工审批"""
    # interrupt() 抛出暂停信号，返回给调用方等审批
    approval = interrupt({
        "action": "transfer",
        "amount": state["amount"],
        "question": "确认转账 5000 元？",
    })
    if approval != "APPROVED":
        return {"status": "cancelled"}
    return {"status": "approved"}

# 编译时指定：进入 request_payment 前暂停
app = graph.compile(checkpointer=checkpointer, interrupt_before=["request_payment"])

# 第一段执行 → 停在 request_payment 前，返回待审批信息
result = app.invoke({"messages": [...]}, config)
print(app.get_state(config).next)   # → ['request_payment']（等待中的节点）

# 人工审批后，用 Command 恢复执行
from langgraph.types import Command
app.invoke(
    Command(resume="APPROVED"),     # ← 审批结果注入，图继续跑
    config,
)
```

**HITL 两种实现对比**：

| 方式 | 用法 | 适用 |
|------|------|------|
| `interrupt_before`（编译期） | 静态声明暂停点 | 固定流程的审批门 |
| `interrupt()`（运行时） | 节点内动态触发 | 条件性审批（金额>阈值才暂停） |

> 💡 **interrupt_before vs middleware HITL**：create_agent + HumanInTheLoopMiddleware 适合「工具级审批」（简单）；LangGraph 手写图 + interrupt 适合「流程级审批」（多步工作流、需审计轨迹）。

---

## 7. Time Travel 时间旅行

> 🎯 **回退到任意检查点重放**——排查 Agent 决策错误、尝试「如果当时换条路」的替代路径（官方演示的杀手级功能）。

```python
# ① 获取某线程的历史检查点
history = list(app.get_state_history(config))
past = history[2]                     # 找到想回退的检查点

# ② 回退：用历史配置重新执行
replay_config = {"configurable": {"thread_id": "order-42", "checkpoint_id": past.checkpoint_id}}
app.invoke(Command(resume="SKIP_STEP"), replay_config)

# ③ 时间旅行三用途
# ├── 调试：重放失败路径，观察哪一步决策错
# ├── 分支探索：在检查点分叉，尝试替代方案
# └── 恢复：HITL 拒绝后，从更早节点重新走
```

---

## 8. create_agent 与 LangGraph 的关系

> 🎯 **create_agent 就是预制的 LangGraph 图**——「用 create_agent 起步，需要精细控制时升级到显式图」。

```text
create_agent 内部（自动生成的状态机）
┌─ agent 节点（ReAct 循环）─────────────┐
│  model → 有 tool_calls? → 执行工具    │
│    ↑                    ↓            │
│  └─ 工具结果回填 → 再调 model ──────┘ │
│  直到模型无 tool_calls → 结束          │
└──────────────────────────────────────┘
可注入的能力（create_agent 参数 ↔ LangGraph 概念）
├── checkpointer    ↔ Checkpointer 持久化
├── middleware      ↔ 自定义节点/钩子
├── response_format ↔ 后处理节点
└── state_schema    ↔ State 定义（TypedDict）

升级路径（何时拆开写图）
├── 需要多阶段人工审批 → 显式图 + interrupt
├── 需要并行分支（两个 Agent 同时跑）→ 显式图
├── 需要精确控制状态字段 → state_schema 自定义
└── 只是多个工具一个循环 → 继续用 create_agent（别过度工程）
```

```python
# 显式图里直接用 create_agent 作为节点（组合两种抽象）
from langchain.agents import create_agent

research_agent = create_agent(model="...", tools=[search, scrape])
writing_agent = create_agent(model="...", tools=[], prompt="你是文案编辑……")

def research_node(state): return {"draft": research_agent.invoke(state["messages"])}
def writing_node(state): return {"messages": writing_agent.invoke(state["draft"])}

graph = StateGraph(State)
graph.add_node("research", research_node)
graph.add_node("writing", writing_node)
graph.add_edge(START, "research"); graph.add_edge("research", "writing"); graph.add_edge("writing", END)
```

---

## 9. 性能与生产化

```text
LangGraph 性能基线（官方 benchmark）
├── 相比无状态链：慢约 68%（checkpoint 序列化开销）
├── 优化手段①：高频循环放节点内部（一次图遍历 = 一次 checkpoint）
├── 优化手段②：节点内批量处理（合并多次小步骤为一次大步骤）
├── 优化手段③：去掉不需要的 checkpointer（纯无状态图不装）
└── 优化手段④：subgraph（子图）隔离高频路径，别让主图过度膨胀
```

**生产 checklist**：

| 关注点 | 实践 |
|--------|------|
| 持久化 | Postgres/Redis Checkpointer + thread_id 规范（业务ID） |
| 并发 | 图本身无状态，多实例水平扩展；checkpointer 共享 |
| 监控 | LangSmith trace 每节点耗时/token；告警异常循环 |
| 安全 | 工具节点做权限校验；HITL 覆盖高危操作 |
| 测试 | 无 LLM 单元测试（mock 节点函数）+ 端到端黄金集 |
| 版本 | 图结构变更兼容（新增字段带默认值，别删旧字段） |

**常见坑**：

```text
├── 状态无限增长：messages 越积越多 → 定期裁剪/摘要节点
├── Reducer 用错：普通字段覆盖语义 vs 追加语义混淆 → 数据丢失
├── 循环无出口：路由函数漏了终止条件 → 烧钱
├── thread_id 不传：多轮对话失忆（每个请求都是新会话）
└── 节点里调图：图不能在自己内部再 invoke 自己（用 subgraph）
```

---

## 10. 核心要点

> 🎯 **核心要点**：
> 1. **LangGraph = 显式状态机**：State（共享内存）+ Node（步骤）+ Edge（条件边）
> 2. **与 LangChain 分工**：create_agent 底层就是 LangGraph——够用就高层，要精细控制就拆开写图
> 3. **条件边 = while 循环/分支**：路由函数返回 key 决定走向；循环必须有退出条件
> 4. **Checkpointer = 持久化**：thread_id 定位会话，崩溃恢复；Postgres 生产选型
> 5. **HITL = interrupt**：审批门停得住、Command(resume) 续得上
> 6. **Time Travel**：回退检查点重放/分叉——调试与替代路径探索
> 7. **性能约 -68%**：高频循环放节点内部，别为编排而编排
> 8. **选型法则**：≤3 步直线 → LCEL；多工具循环 → create_agent；多阶段/审批/并行 → LangGraph

---

**下一模块**：[07-实战选型与面试](07-实战选型与面试.md) | **返回总览**：[00-LangChain知识体系总览](00-LangChain知识体系总览.md)
