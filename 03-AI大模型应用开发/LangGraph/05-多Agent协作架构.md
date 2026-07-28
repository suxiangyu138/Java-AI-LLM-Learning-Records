# 05 - 多 Agent 协作架构

> 单个 Agent 能力有限。多 Agent 协作是处理复杂任务的关键——LangGraph 提供了从 Supervisor 到 Swarm 四种协作模式。

---

## 📚 目录

1. [多 Agent 协作模式全景](#1-多-agent-协作模式全景)
2. [Supervisor 模式：主 Agent 分配任务](#2-supervisor-模式主-agent-分配任务)
3. [Hierarchical 模式：层级委派](#3-hierarchical-模式层级委派)
4. [Swarm / Handoff 模式：Agent 间交接](#4-swarm--handoff-模式agent-间交接)
5. [Map-Reduce 模式：并行子任务](#5-map-reduce-模式并行子任务)
6. [模式选型指南](#6-模式选型指南)
7. [多 Agent 通信协议](#7-多-agent-通信协议)

---

## 1. 多 Agent 协作模式全景

```text
四种经典模式：

1. Supervisor（监督者）
   主Agent统一调度，将任务分发给各专业子Agent
   ┌───────┐
   │Supervisor│──→ Agent A
   │         │──→ Agent B
   │         │──→ Agent C
   └───────┘

2. Hierarchical（层级委派）
   多级 Agent：上层下达目标，下层执行并汇报
   Manager → TeamLead → Worker1, Worker2

3. Swarm / Handoff（交接）
   平级 Agent，对话可热交接（handoff）
   Agent A ←→ Agent B ←→ Agent C

4. Map-Reduce（并行）
   拆解任务 → 并行执行 → 汇总结果
```

---

## 2. Supervisor 模式：主 Agent 分配任务

### 2.1 架构设计

```python
"""
Supervisor Agent 架构：
  ├── Supervisor Agent（路由器）
  │   └── 职责：理解用户意图，决定交给哪个 Worker
  ├── Worker Agents（执行者）
  │   ├── CodeAgent：写代码
  │   ├── ResearchAgent：搜索信息
  │   └── MathAgent：数学计算
  └── 通信机制：Supervisor 通过 State 传递任务
"""
```

### 2.2 完整实现

```python
from langgraph.graph import StateGraph, START, END
from langgraph.prebuilt import create_react_agent
from langgraph.types import Command
from typing import Literal

# ① 定义共享 State
class SupervisorState(TypedDict):
    messages: Annotated[list, operator.add]
    next_agent: str  # Supervisor 决定下一步交给谁

# ② 创建 Worker Agent（每个都是独立的 ReAct Agent）
code_agent = create_react_agent(
    llm, [write_code, run_tests],
    state_schema=SupervisorState
)
research_agent = create_react_agent(
    llm, [web_search, fetch_document],
    state_schema=SupervisorState
)
math_agent = create_react_agent(
    llm, [calculator, plot_graph],
    state_schema=SupervisorState
)

# ③ Supervisor Node：决定路由
# 方案 A：LLM 决策（推荐）
def supervisor_node(state: SupervisorState) -> dict:
    """让 LLM 决定下一步交给谁"""
    system_prompt = """你是任务调度器。根据用户需求，选择最合适的 Agent：

    - code_agent：写代码、调试、代码审查
    - research_agent：搜索信息、文档查询
    - math_agent：数学计算、数据分析、图表
    - FINISH：任务已完成

    只返回 Agent 名称，不要解释。"""

    # 用结构化输出强制 LLM 返回选择
    response = llm_with_structure.invoke([
        SystemMessage(content=system_prompt),
        *state["messages"][-5:]  # 最近 5 条消息做上下文
    ])
    return {"next_agent": response.next_agent}

# 方案 B：规则路由（简单场景）
def rule_based_supervisor(state: SupervisorState) -> dict:
    last_msg = state["messages"][-1].content.lower()
    if any(kw in last_msg for kw in ["代码", "code", "bug"]):
        return {"next_agent": "code_agent"}
    elif any(kw in last_msg for kw in ["搜索", "search", "查"]):
        return {"next_agent": "research_agent"}
    elif any(kw in last_msg for kw in ["计算", "算", "math"]):
        return {"next_agent": "math_agent"}
    else:
        return {"next_agent": "FINISH"}

# ④ 构建 Supervisor 图
builder = StateGraph(SupervisorState)

# 添加 Supervisor
builder.add_node("supervisor", supervisor_node)

# 添加 Worker Agent（以编译好的子图作为节点）
builder.add_node("code_agent", code_agent)
builder.add_node("research_agent", research_agent)
builder.add_node("math_agent", math_agent)

# 路由逻辑
def route_to_agent(state: SupervisorState) -> str:
    return state["next_agent"]

builder.add_conditional_edges(
    "supervisor",
    route_to_agent,
    {
        "code_agent": "code_agent",
        "research_agent": "research_agent",
        "math_agent": "math_agent",
        "FINISH": END
    }
)

# Worker 执行完后回到 Supervisor（继续判断是否完成）
builder.add_edge("code_agent", "supervisor")
builder.add_edge("research_agent", "supervisor")
builder.add_edge("math_agent", "supervisor")

builder.add_edge(START, "supervisor")

graph = builder.compile()
```

### 2.3 执行流程

```text
用户："帮我写一个快速排序，并计算它的时间复杂度"

执行 →
supervisor → 决策：code_agent（需要写代码）
  code_agent → 生成快速排序代码
  → 回到 supervisor

supervisor → 查看进度：还有数学部分
supervisor → 决策：math_agent（计算复杂度）
  math_agent → 分析 O(n log n)
  → 回到 supervisor

supervisor → 所有任务完成
supervisor → 决策：FINISH
→ END
```

---

## 3. Hierarchical 模式：层级委派

### 3.1 架构设计

```text
三层架构：
┌─────────────────────────────────────────┐
│           Manager Agent（L1）            │
│   理解业务目标，拆解为子目标，分配给 L2   │
├──────────┬──────────────┬───────────────┤
│  PM Agent │  Tech Agent  │  QA Agent    │  ← L2: Team Lead
│   (L2)    │   (L2)       │   (L2)       │
├─────┬─────┼─────┬─────────┼──────┬────────┤
│ W1  │ W2  │ W3  │  W4     │  W5  │  W6    │  ← L3: Worker
└─────┴─────┴─────┴─────────┴──────┴────────┘
```

### 3.2 实现要点

```python
# 层级 Agent 的本质：递归的 Subgraph
# L3 Worker → L2 的子图 → L1 的子图

def build_l3_worker(role: str) -> CompiledGraph:
    """构建 L3 Worker Agent"""
    return create_react_agent(llm, tools_for_role[role])

def build_l2_team_lead(domain: str, workers: list[str]) -> CompiledGraph:
    """构建 L2 Team Lead，管理多个 Worker"""
    builder = StateGraph(TeamState)

    # 添加 Worker
    for w in workers:
        builder.add_node(w, build_l3_worker(w))

    # 添加 Team Lead（分发任务）
    builder.add_node("lead", team_lead_node(domain))
    # Team Lead 将任务分解，分发给 worker
    # ...

    return builder.compile()

def build_l1_manager() -> CompiledGraph:
    """构建 L1 Manager"""
    builder = StateGraph(ManagerState)

    # L2 Team Lead 作为子图节点
    builder.add_node("pm_team", build_l2_team_lead("pm", ["analyst", "writer"]))
    builder.add_node("tech_team", build_l2_team_lead("tech", ["coder", "architect"]))
    builder.add_node("qa_team", build_l2_team_lead("qa", ["tester", "reviewer"]))

    # Manager 负责目标拆分和分配
    builder.add_node("manager", manager_node)
    # ...
    return builder.compile()
```

---

## 4. Swarm / Handoff 模式：Agent 间交接

### 4.1 核心概念

```text
Handoff = Agent A 在执行过程中，发现自己的能力不够，
          主动将"对话控制权"交给 Agent B

对话上下文（messages）随 handoff 一起传递
用户完全无感——从用户视角看，是一个 Agent 在持续对话
```

### 4.2 实现

```python
from langgraph.prebuilt import create_react_agent
from langgraph.types import Command

# ① 每个 Agent 都是一个独立的 ReAct Agent
# 都有一个 handoff 工具：调用它 = 把控制权交给另一个 Agent

def make_handoff_tool(agent_name: str):
    """创建 handoff 工具"""
    def handoff_to_agent():
        """将对话控制权移交给另一个 Agent"""
        # 这个函数不会被真正调用
        # LangGraph 通过检测 tool_call 名称来识别 handoff
        return f"Transferring to {agent_name}"
    handoff_to_agent.__name__ = f"transfer_to_{agent_name}"
    return handoff_to_agent

# ② 创建 Swarm Agents
# 每个 Agent 除了自己的工具，还有到其他 Agent 的 handoff 工具
code_agent = create_react_agent(
    llm,
    tools=[write_code, run_tests, make_handoff_tool("researcher"), make_handoff_tool("math_agent")]
)

researcher = create_react_agent(
    llm,
    tools=[web_search, make_handoff_tool("code_agent"), make_handoff_tool("math_agent")]
)

# ③ 路由函数：检测 handoff
def swarm_router(state):
    last_msg = state["messages"][-1]
    if hasattr(last_msg, "tool_calls"):
        for tc in last_msg.tool_calls:
            name = tc["name"]
            if name.startswith("transfer_to_"):
                target = name.replace("transfer_to_", "")
                # 返回 Command 实现 "热交换"
                return Command(
                    goto=target,
                    update={"messages": [ToolMessage(
                        content=f"Transferring to {target}",
                        tool_call_id=tc["id"]
                    )]}
                )
    return END

# ④ 构建 Swarm 图
# 所有 Agent 平级，通过 handoff 互相跳转
builder = StateGraph(MultiAgentState)
builder.add_node("code_agent", code_agent)
builder.add_node("researcher", researcher)
builder.add_node("math_agent", math_agent)

# 所有 Agent 的出口都经过 swarm_router
builder.add_conditional_edges("code_agent", swarm_router)
builder.add_conditional_edges("researcher", swarm_router)
builder.add_conditional_edges("math_agent", swarm_router)
```

### 4.3 Handoff vs Supervisor

```text
┌──────────────┬─────────────────────┬─────────────────────┐
│    特性       │   Supervisor        │   Handoff/Swarm     │
├──────────────┼─────────────────────┼─────────────────────┤
│ 控制方式      │ 集中式（主Agent决策）│ 分布式（Agent自行判断）│
│ 路由决策者    │ Supervisor          │ 当前 Agent           │
│ 用户感知      │ 可能感知切换         │ 感知是一个Agent      │
│ 适用场景      │ 明确的任务分工       │ 模糊边界、自由对话    │
│ 实现复杂度    │ 中等                │ 较高（需处理上下文）  │
│ 灵活度        │ ⭐⭐⭐              │ ⭐⭐⭐⭐⭐            │
└──────────────┴─────────────────────┴─────────────────────┘
```

---

## 5. Map-Reduce 模式：并行子任务

### 5.1 架构

```python
from langgraph.graph import Send

class MapReduceState(TypedDict):
    # 输入
    complex_query: str
    sub_tasks: Annotated[list, operator.add]

    # Map 结果
    partial_results: Annotated[list, operator.add]

    # Reduce 结果
    final_answer: str

# Step 1: 拆解任务
def decomposer(state: MapReduceState) -> dict:
    """将复杂任务拆解为子任务列表"""
    response = llm.invoke(
        f"将以下复杂问题拆解为可并行执行的子任务（最多 5 个）：\n{state['complex_query']}\n"
        f"返回 JSON 数组，每个元素是子任务描述"
    )
    tasks = json.loads(response.content)
    return {"sub_tasks": tasks}

# Step 2: 并行分发（使用 Send API）
def dispatcher(state: MapReduceState) -> list[Send]:
    """为每个子任务创建一个并行 Worker"""
    return [
        Send("worker", {"task": task, "index": i})
        for i, task in enumerate(state["sub_tasks"])
    ]

# Step 3: Worker 处理
def worker(state: MapReduceState) -> dict:
    """处理单个子任务"""
    result = llm.invoke(f"请回答以下子问题：\n{state['task']}")
    return {"partial_results": [{
        "task_index": state["index"],
        "task": state["task"],
        "answer": result.content
    }]}

# Step 4: 汇总
def reducer(state: MapReduceState) -> dict:
    """汇总所有子任务结果，生成最终答案"""
    partials = "\n".join([
        f"Q{i}: {r['task']}\nA{i}: {r['answer']}"
        for r in state["partial_results"]
    ])
    final = llm.invoke([
        SystemMessage(content="综合以下子问题的答案，给出完整回答："),
        HumanMessage(content=partials)
    ])
    return {"final_answer": final.content}
```

### 5.2 执行时间线

```text
单 Agent 顺序执行：       ████████████████████████████ (60s)

Map-Reduce 并行执行：
  decomposer              ██            (2s)
  worker_1                ████████      (8s)  ← 并行！
  worker_2                ██████████    (10s) ← 并行！
  worker_3                ██████        (6s)  ← 并行！
  reducer                         ████  (4s)
  总耗时：                         16s  (62% 加速)
```

---

## 6. 模式选型指南

```text
你的多 Agent 场景是？
│
├── 任务可以明确分类（代码/搜索/计算）
│   └── → Supervisor 模式
│       优点：简单清晰，容易 debug
│       例：编程助手（代码Agent + 搜索Agent + 测试Agent）
│
├── 对话式交互，Agent 能力边界模糊
│   └── → Swarm / Handoff 模式
│       优点：用户体验流畅，Agent 自行判断
│       例：通用客服（售前Agent ↔ 技术Agent ↔ 投诉Agent）
│
├── 需要层级审批/多级决策
│   └── → Hierarchical 模式
│       优点：符合组织架构，权限清晰
│       例：合同审批（起草员 → 律师 → 经理）
│
├── 任务可并行化（多个独立子问题）
│   └── → Map-Reduce 模式
│       优点：大幅降低延迟
│       例：多源调研（同时搜索 5 个数据源）
│
└── 组合！实际场景通常是混合模式
    └── Supervisor 的某个 Worker 内部用 Map-Reduce
    └── Hierarchical 的 L1 用 Supervisor，L2 用 Handoff
```

---

## 7. 多 Agent 通信协议

### 7.1 通信方式对比

```text
方式 1：State 传递（LangGraph 原生）
  Agent A → 写入 shared State → Agent B 读取
  优点：简单，可追溯
  缺点：所有 Agent 共享同一个 State Schema

方式 2：Tool Message（OpenAI 格式）
  Agent A → tool_call → ToolMessage → Agent B
  优点：标准格式，兼容所有模型
  缺点：Tool 定义需要同步

方式 3：Shared Memory（外部存储）
  Agent A → 写入 Redis/DB → Agent B 读取
  优点：解耦，支持跨进程
  缺点：需要额外的存储基础设施
```

### 7.2 State-based 通信最佳实践

```python
# ✅ 好的 State 设计（支持多 Agent 通信）
class MultiAgentState(TypedDict):
    # 共享对话历史
    messages: Annotated[list, operator.add]

    # Agent 间消息（专用 channel）
    agent_messages: Annotated[list, operator.add]

    # 任务分配
    current_agent: str
    assigned_task: str

    # 执行结果
    agent_results: Annotated[dict, merge_dict]
    # {"code_agent": "生成的代码", "reviewer": "审查意见"}

    # 状态跟踪
    completed_agents: Annotated[set, union_sets]
    pending_handoffs: list

# 每个 Agent 只关心自己需要的字段
# 通过命名约定避免字段冲突
```

---

> 🎯 **核心要点**：Supervisor 模式是最高频的多 Agent 模式，简单可靠。Swarm/Handoff 模式让用户体验最优（感觉不到 Agent 切换）。实际系统中，通常是多种模式的组合——顶层的 Supervisor 分配任务，每个 Worker 内部可能用 Map-Reduce 加速。

---

**下一模块**：[06 - Java 生态：LangGraph4j](./06-Java生态LangGraph4j.md)  
**返回总览**：[00 - LangGraph 知识体系总览](./00-LangGraph知识体系总览.md)
