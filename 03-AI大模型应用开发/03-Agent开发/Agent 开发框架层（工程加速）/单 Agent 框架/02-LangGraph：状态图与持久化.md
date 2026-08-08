# 02 LangGraph：状态图与持久化

> 定位：状态机范式的代表——图 + 共享状态 + checkpointing，复杂状态流与人工介入的第一选择（v1.2.0，2026-08 基准）

## 📚 目录

1. [范式与版本现状](#1-范式与版本现状)
2. [高层入口：create_agent](#2-高层入口create_agent)
3. [StateGraph：图的核心抽象](#3-stategraph图的核心抽象)
4. [Checkpointing：持久化与恢复](#4-checkpointing持久化与恢复)
5. [Human-in-the-Loop：interrupt 机制](#5-human-in-the-loopinterrupt-机制)
6. [Checkpoint vs Store：短期与长期记忆](#6-checkpoint-vs-store短期与长期记忆)
7. [2026 实践要点与陷阱](#7-2026-实践要点与陷阱)

## 1. 范式与版本现状

```text
范式：显式状态机——把 Agent 循环画成图
  节点（Node）   = 工作单元（LLM 调用/工具执行/代码逻辑）
  边（Edge）     = 转移（顺序/条件分支/循环）
  共享状态（State）= 图中所有节点可读写的类型化对象

版本：v1.x（2026-05 达 v1.2.0），LTS 稳定线
  核心图 API 与执行模型不变，重点是类型安全与开发体验
  MIT 协议，Python 与 JS/TS 双实现
```

> 🎯 **生态位**：当你的问题能被"状态 + 转移"描述，且需要暂停/恢复/回放时，LangGraph 是唯一把"第 7 步挂了怎么办"当一等公民回答的框架。简单聊天用它就是杀鸡用牛刀。

## 2. 高层入口：create_agent

```text
2026 变化：create_react_agent（旧高层入口）已弃用
新推荐：langchain 包中的 create_agent（构建于 LangGraph）

用法：传入模型 + 工具 + 可选 prompt，返回编译好的图
适用：标准 ReAct 式单 Agent——循环 LLM 调用与工具调用直到出答案
```

```python
from langchain_openai import ChatOpenAI
from langchain.agents import create_agent
from langgraph.checkpoint.memory import InMemorySaver

agent = create_agent(
    model=ChatOpenAI(model="gpt-5", temperature=0),
    tools=[search_orders, refund_apply],
    system_prompt="你是电商客服，查询订单请用工具，退款必须走审批。",
)
config = {"configurable": {"thread_id": "session-123"}}  # 会话标识
result = agent.invoke({"messages": [{"role": "user", "content": "查一下 #8823 订单"}]}, config)
```

**什么时候下沉到 StateGraph**：需要条件路由（意图分流）、人工介入（HITL）、显式循环控制、多个 Agent 编排时——高层入口给不了的拓扑，用 StateGraph 自己画。

## 3. StateGraph：图的核心抽象

### 3.1 最小图结构

```python
from typing import TypedDict, Annotated
from langgraph.graph import StateGraph, START, END
from langgraph.graph.message import add_messages

class AgentState(TypedDict):
    messages: Annotated[list, add_messages]   # 追加式 reducer
    pending_approval: dict | None             # 审批状态

graph = StateGraph(AgentState)
graph.add_node("llm", call_llm)          # 节点 = 函数(state) -> 部分状态
graph.add_node("tools", run_tools)
graph.add_edge(START, "llm")
graph.add_conditional_edge("llm", route, {"tool": "tools", "answer": END})
graph.add_edge("tools", "llm")           # 循环
app = graph.compile()
```

### 3.2 三个核心概念

| 概念 | 说明 | 陷阱 |
|------|------|------|
| 节点 | 纯函数 `(state) -> partial_state` | 不要原地 mutate state——破坏 checkpointing |
| Reducer | 状态合并规则（如 `add_messages` 追加） | 默认覆盖；追加型字段必须显式声明 reducer |
| 条件边 | 根据 state 选择下一节点 | 路由函数要在"已知集合"内返回（未定义目标报错） |

> 💡 **Agent 循环的本质**（与 07 章呼应）：`llm → tools → llm → ... → END` 的条件环——LangGraph 把它变成显式图，其他框架把它藏在内部。

## 4. Checkpointing：持久化与恢复

### 4.1 机制与后端选型

```text
checkpointer = 每次节点转移保存图状态（按 thread_id 索引）
没有 checkpointer：每次 invoke 独立，无记忆，HITL 不可用

后端选型：
  InMemorySaver  开发/测试（进程重启即丢，勿用于生产）
  SqliteSaver    低量单进程服务
  PostgresSaver  生产标准（独立包 langgraph-checkpoint-postgres）
```

```python
from langgraph.checkpoint.postgres import PostgresSaver
app = graph.compile(checkpointer=PostgresSaver.from_conn_string(DSN))
```

### 4.2 恢复语义（必踩细节）

| 细节 | 说明 |
|------|------|
| thread_id 必须一致 | resume 时传相同 config（含 thread_id）；不一致 = 新执行从 START 开始，绕过检查点 |
| 状态是合并不是替换 | resume 的 input_state 与检查点状态 merge——只改审批字段，其余保留 |
| checkpoint_id 是 ULID | 最新状态 = `ORDER BY checkpoint_id DESC LIMIT 1` |
| 时间旅行 | `get_state_history` 查看历史状态；`updateState` 在断点修改状态 |

## 5. Human-in-the-Loop：interrupt 机制

### 5.1 两种接入方式

```text
方式 1：interrupt() 节点内调用（推荐）
  payload = 任意 JSON 可序列化值——传"审批卡片"（表单应该渲染什么）

方式 2：compile 参数
  interrupt_before=["node"]  /  interrupt_after=["node"]
  在指定节点前/后暂停（需 checkpointer）
  注意：列了不存在的节点名会被静默忽略；名称区分大小写
```

```python
def approve_refund(state: AgentState):
    decision = interrupt({"amount": state["refund_amount"], "order": state["order_id"]})
    # 人工恢复后从这里继续，decision 就是 Command(resume=...) 传来的值
    if decision.get("approve"):
        return {"pending_approval": None}
    return {"pending_approval": "rejected"}

# 恢复：Command(resume=...) 从精确检查点继续，不重跑前面步骤
app.invoke(Command(resume={"approve": True}), config)
```

### 5.2 四种人工决策（前端协议）

| 决策 | 含义 |
|------|------|
| `approve` | 批准，继续执行 |
| `reject` + message | 拒绝，工具不执行，Agent 收到拒绝消息 |
| `edit` + editedAction | 用修改后的参数执行工具 |
| `respond` + message | 不执行工具，人工消息作为工具结果返回 |

> 🎯 **2026 共识（与工程化模块 08 篇呼应）**：HITL 只用于高影响动作（支付、邮件、数据库写、昂贵 API）。低后果决策全自动——把 HITL 当默认会拖垮体验。

## 6. Checkpoint vs Store：短期与长期记忆

```text
Checkpointer = 短期、thread 级记忆
  会话连续性、HITL、时间旅行、故障恢复

Store（BaseStore）= 跨 thread 长期记忆
  用户偏好、事实、可复用知识（独立存储层）

2026 最佳实践：开发用 MemorySaver，生产用 PostgresSaver + Store 管长期知识
```

## 7. 2026 实践要点与陷阱

### 7.1 实践要点

```text
- 生产必须有 checkpointer（崩溃恢复全部进度）
- 节点幂等：恢复同一审批两次 = 副作用执行两次（扣款/发信）——要加去重
- HITL 稀疏使用；长任务用子图隔离上下文
- 可观测：与 LangSmith 原生集成；或 OTel 埋点（工程化模块 04 篇）
```

### 7.2 陷阱速查

| 陷阱 | 症状 | 修复 |
|------|------|------|
| 原地 mutate state | checkpoint 记录不到变更 | 节点返回新状态对象 |
| 无 checkpointer | 崩溃丢全部进度、HITL 不工作 | compile(checkpointer=...) |
| resume 忘传 config | 从 START 重新执行（绕过了断点） | 相同 thread_id 的相同 config |
| 状态覆盖而非合并 | 只改了审批字段，别的字段丢了 | 用 reducer 或部分状态返回 |
| 过度 HITL | 每一步都等人 | 只拦不可逆/高影响 |
| 高层入口不够用 | 路由/HITL 写不出来 | 下沉 StateGraph |

---

## 【参考来源】

- [LangChain Docs: What's new in LangGraph v1](https://docs.langchain.com/oss/javascript/releases/langgraph-v1)
- [futureagi.com: What is LangGraph? Stateful Agent Graphs Explained in 2026](https://futureagi.com/blog/what-is-langgraph-2026/)
- [LangChain Docs: Checkpointers（Python）](https://docs.langchain.com/oss/python/langgraph/checkpointers)
- [LangChain Docs: Persistence](https://docs.langchain.com/oss/javascript/langgraph/persistence)
- [LangChain Docs: Human-in-the-Loop](https://docs.langchain.com/oss/python/langchain/frontend/human-in-the-loop)
- [The Neural Base: What human-in-the-loop adds to automation](https://theneuralbase.com/langgraph/learn/intermediate/what-human-in-the-loop-adds-to-automation/)
- [Hindsight: LangGraph Short-Term State vs Long-Term Memory](https://hindsight.vectorize.io/guides/2026/07/17/guide-langgraph-state-vs-long-term-memory)

---

**返回总览**：[00-总览：单 Agent 框架知识体系](00-总览：单%20Agent%20框架知识体系.md)

**下一模块**：[03-OpenAI Agents SDK：轻量多原语](03-OpenAI%20Agents%20SDK：轻量多原语.md)
