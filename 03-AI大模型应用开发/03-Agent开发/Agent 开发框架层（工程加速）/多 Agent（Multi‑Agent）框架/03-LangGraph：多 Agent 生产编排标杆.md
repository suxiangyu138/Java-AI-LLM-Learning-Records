# 03 LangGraph：多 Agent 生产编排标杆

> 定位：框架深潜第一站——生产部署份额第一（~38%）的多 Agent 编排：supervisor/subagents/swarm 三种实现方式、checkpointer 持久化、time-travel 调试与 2026 架构变化（2026-08 基准）

## 📚 目录

1. [为什么是生产标杆](#1-为什么是生产标杆)
2. [2026 架构变化：subagents 模式取代 supervisor 包](#2-2026-架构变化subagents-模式取代-supervisor-包)
3. [三种多 Agent 实现方式](#3-三种多-agent-实现方式)
4. [Supervisor 实现（经典）](#4-supervisor-实现经典)
5. [subagents 模式（2026 推荐）](#5-subagents-模式2026-推荐)
6. [Swarm 实现](#6-swarm-实现)
7. [checkpointer：持久化与 time-travel](#7-checkpointer持久化与-time-travel)
8. [状态设计：共享 State 与 reducer](#8-状态设计共享-state-与-reducer)
9. [生产配套：LangSmith 与监控](#9-生产配套langsmith-与监控)
10. [局限与适用边界](#10-局限与适用边界)
11. [核心要点](#11-核心要点)

## 1. 为什么是生产标杆

| 维度 | 2026 事实 |
|------|----------|
| 市场 | ~38% 生产多 Agent 部署（2026 Q1 估计） |
| 定位 | 图状态机编排：节点 + 边 + 共享 State + checkpointer |
| 语言 | Python + JavaScript 双栈 |
| 版本 | SDK 0.3.14（2026-05）；独立于 LangChain 运行（生态耦合可选） |
| 独有能力 | **time-travel 调试**——"步骤 7 失败了会发生什么"有一等公民答案 |
| 可观测 | LangSmith 追踪最成熟 |

> 🎯 **一句话**：LangGraph 把多 Agent 编排表达为**可持久化、可回放、可审计的状态机**——这是它成为生产标杆的根本：其他框架把编排藏在提示词里，LangGraph 把编排显式画成图。

## 2. 2026 架构变化：subagents 模式取代 supervisor 包

```text
2025          langgraph-supervisor 包（create_supervisor）── 官方推荐
2026          ⚠️ 弃用：不再积极维护，迁移到 subagents 模式

迁移映射表（官方指南）：
create_supervisor + worker 作为图节点
    → create_agent + worker 包装为 @tool（subagent.invoke(...)）
create_handoff_tool 自定义路由
    → 自定义 @tool 调用子 Agent
嵌套 Supervisor
    → 子 Agent 作为 @tool 调用其他子 Agent
interrupt/resume 流程
    → 子 Agent 工具内的 interrupt 会向上传播，
      外部用 Command(resume=result) 恢复
```

> 💡 **为什么弃用**：supervisor 包把路由逻辑放在模型提示词里，难以调试与复用；subagents 模式把子 Agent 变成普通工具，统一了"工具调用"与"子 Agent 调用"两种心智模型——编排放图里，调用是工具。

## 3. 三种多 Agent 实现方式

| 方式 | 机制 | 适合 |
|------|------|------|
| Supervisor（图节点） | 编排者节点 + Worker 节点，条件边路由 | 经典中心化（迁移中） |
| **subagents（工具包装）** | Worker 包装为 @tool，编排者工具调用 | **2026 官方推荐** |
| Handoffs/Swarm | Agent 作为图节点 + `Command` 转交工具 | 去中心化移交 |

三者共享同一 API 面、无废弃依赖——差异在"编排控制流放哪"：图边（显式）还是工具调用（隐式）。

## 4. Supervisor 实现（经典）

```python
from langgraph.graph import StateGraph, START, END

class State(TypedDict):
    messages: Annotated[list, add_messages]
    next_agent: str

builder = StateGraph(State)
builder.add_node("supervisor", supervisor_node)   # 编排者：选 next_agent
builder.add_node("researcher", research_node)     # Worker 1
builder.add_node("writer", writer_node)           # Worker 2
builder.add_edge(START, "supervisor")
# 条件边：supervisor 决定路由到哪个 Worker 或结束
builder.add_conditional_edges("supervisor", route_by_next_agent,
    {"researcher": "researcher", "writer": "writer", "finish": END})
```

| 要点 | 说明 |
|------|------|
| 编排者节点 | 一次 LLM 调用返回 `next_agent` 字段（结构化输出） |
| 条件边 | `route_by_next_agent` 读 State 决定下一节点 |
| 规模 | 3-10 个 Agent 最佳 |

## 5. subagents 模式（2026 推荐）

```python
from langchain.agents import create_agent

# 1. Worker：普通 Agent
researcher = create_agent(model=fast_model, tools=[search_tool],
                          system_prompt="你是调研员…")
writer = create_agent(model=fast_model, system_prompt="你是写手…")

# 2. 编排者：把 Worker 包装成工具
@tool
def call_researcher(question: str) -> str:
    """当需要调研时调用"""
    return researcher.invoke({"messages": [("user", question)]})

supervisor = create_agent(model=strong_model, tools=[call_researcher, call_writer],
                          system_prompt="你是编排者，按需调用工具…")
```

| 优点 | 说明 |
|------|------|
| 统一心智 | 子 Agent 与工具无差别——编排者只做"选工具" |
| 复用 | 子 Agent 可被任意编排者调用 |
| interrupt 传播 | 子 Agent 内 interrupt 向上传播，外部 `Command(resume=...)` 恢复 |
| 调试 | 追踪天然嵌套（工具调用栈） |

## 6. Swarm 实现

```python
# 方式一：@langchain/langgraph-swarm（JS 包 createSwarm）
from langgraph.prebuilt import create_agent
from langgraph_swarm import create_swarm, create_handoff_tool

sales_agent = create_agent(model, tools=[create_handoff_tool(agent_name="support_agent", description="售后问题转交")])
support_agent = create_agent(model, tools=[create_handoff_tool(agent_name="sales_agent")])

swarm = create_swarm([sales_agent, support_agent], default_agent=sales_agent)
```

```python
# 方式二：图节点 + Command 转交（Python 标准做法）
@tool
def transfer_to_support(context_vars: dict) -> Command:
    """售后问题转交支持 Agent"""
    return Command(goto="support_agent", update={"messages": context_vars})

builder.add_node("sales_agent", create_agent(model, tools=[transfer_to_support]))
```

| 2026 细节 | 说明 |
|-----------|------|
| last active agent 记忆 | JS swarm 包：多轮对话自动回到正确专精 Agent |
| trace 通道 | `Annotated[list, operator.add]` 记录移交历史 |
| 护栏 | 循环检测 / 死胡同 / hop 预算 / 人工升级（见 02 篇第 9 节） |

## 7. checkpointer：持久化与 time-travel

| 能力 | 说明 |
|------|------|
| 机制 | 每节点执行后保存 State 快照 |
| 短时记忆 | `MemorySaver`（线程内） |
| 长时记忆 | `InMemoryStore`/`PostgresSaver`（跨会话） |
| **time-travel** | 回退到任意 checkpoint → 修改 State → 重放——"步骤 7 失败会发生什么"可实测 |
| 必须性 | **没有 checkpointer 的 swarm 会"忘记"上一个活跃 Agent**——多 Agent 场景 checkpointer 是强制项 |

```python
graph = builder.compile(checkpointer=MemorySaver())
# 恢复：thread_id 定位会话；get_state/list_state 查看快照
```

> 🎯 **checkpointer 是 LangGraph 相对所有竞品的最强差异化能力**——CrewAI 2026 也加了 checkpointing（v1.14.2+），但 time-travel 调试仍只有 LangGraph 是一等公民。

## 8. 状态设计：共享 State 与 reducer

```python
from typing import Annotated, TypedDict
from langgraph.graph import add_messages

class State(TypedDict):
    messages: Annotated[list, add_messages]   # 消息自动合并
    next_agent: str                            # 路由字段
    trace: Annotated[list, operator.add]       # 移交历史（append 语义）
    metadata: dict                             # 业务元数据
```

| 设计原则 | 说明 |
|---------|------|
| 消息通道 | `add_messages` 自动合并多 Agent 消息历史 |
| 路由字段 | `next_agent`/`active` 供条件边读取 |
| reducer 语义 | 自定义通道合并（追加/覆盖/汇总） |
| 最小共享 | 只共享必要上下文，避免状态爆炸 |

## 9. 生产配套：LangSmith 与监控

| 配套 | 作用 |
|------|------|
| LangSmith 追踪 | 多 Agent 调用链可视化（嵌套工具调用） |
| LangGraph Studio | 图调试/回放的可视化工具 |
| interrupt() | 人工审批节点（HITL） |
| 预算护栏 | Token 上限、PII 检测、人工兜底 |

## 10. 局限与适用边界

| 局限 | 表现 | 应对 |
|------|------|------|
| 学习曲线 | 状态机模型（节点/边/reducer/checkpointer）较陡 | 官方教程 + starter-kit |
| 复杂图难调试 | 大图难追踪 | Studio + 显式命名节点 |
| LangChain 耦合 | 生态耦合（虽可独立运行） | 评估耦合成本 |
| 表达范式偏状态机 | 群聊/辩论类场景不顺手 | 此类场景考虑 AG2 |

> 🎯 **适用结论**：长流程、分支重试、人工介入、需要审计回放的生产系统 → LangGraph 是 2026 年第一选择；快速原型（数小时出活）则 CrewAI 更快（04 篇）。

## 11. 核心要点

> 🎯 **核心要点**：
> 1. LangGraph = 多 Agent 编排的状态机表达：节点/边/共享 State/checkpointer 四要素
> 2. **2026 弃用 langgraph-supervisor 包** → subagents 模式（Worker 包装为 @tool），统一"工具调用=子 Agent 调用"
> 3. checkpointer 是强制项（swarm 无它即失忆）；time-travel 调试是独有能力
> 4. 生产部署份额 ~38% 第一，但状态机学习曲线真实存在——原型期 CrewAI 更快

---

**上一模块**：[02 编排范式与模式](02-多%20Agent%20编排范式与模式：Supervisor%20Swarm%20Handoff%20Router.md)　**下一模块**：[04 CrewAI](04-CrewAI：角色化%20Crew%20与%20Flows.md)　**返回总览**：[00 总览](00-总览：多%20Agent（Multi‑Agent）框架知识体系.md)

## 【参考来源】

- [Migrate from langgraph-supervisor - LangChain Docs](https://docs.langchain.com/oss/python/migrate/langgraph-supervisor)
- [LangGraph Multi-Agent Patterns (langgraph-101 / DeepWiki)](https://deepwiki.com/langchain-ai/langgraph-101/6-utilities)
- [@langchain/langgraph-swarm (npm)](https://www.npmjs.com/package/@langchain/langgraph-swarm)
- [langgraph-starter-kit: 7 patterns, MCP integration (GitHub)](https://github.com/ac12644/langgraph-starter-kit)
- [Multi-Agent Orchestration Frameworks 2026 (Presenc AI)](https://presenc.ai/research/multi-agent-orchestration-frameworks-2026)
- [Agentic-AI-Orchestration ch07: Swarm architecture (GitHub)](https://github.com/zahurul-islam/Agentic-AI-Orchestration/blob/main/ch07-swarm-ai-architecture/starter/support_swarm.py)
- [ai-system-design-guide: Multi-Agent Orchestration (GitHub)](https://github.com/ombharatiya/ai-system-design-guide/blob/main/07-agentic-systems/04-multi-agent-orchestration.md)
