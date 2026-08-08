# 02 Supervisor 模式工程

> 定位：2026 企业最常用多 Agent 模式的完整实现——官方迁移路径（工具包裹子 Agent）、Command 路由、状态设计（2026-08 基准）

## 📚 目录

1. [2026 官方推荐：工具包裹子 Agent](#1-2026-官方推荐工具包裹子-agent)
2. [完整实现代码](#2-完整实现代码)
3. [Command 路由：状态更新 + 流转一体](#3-command-路由状态更新--流转一体)
4. [状态设计：SupervisorState](#4-状态设计supervisorstate)
5. [弃用迁移：langgraph-supervisor → create_agent](#5-弃用迁移langgraph-supervisor--create_agent)
6. [与阶段 4 生产化融合](#6-与阶段-4-生产化融合)
7. [常见坑速查](#7-常见坑速查)

## 1. 2026 官方推荐：工具包裹子 Agent

> ⚠️ **2026 重要变更**：`langgraph-supervisor` 包**已停止维护**。官方迁移路径：

```text
❌ 旧方式（langgraph-supervisor）：
create_supervisor(...)           # 子 Agent 作为图节点 + handoff 工具

✅ 新方式（官方推荐）：
子 Agent 包装成 @tool 函数 → 监督者用 create_agent 调用它们
```

| 维度 | 旧（弃用） | 新（推荐） |
|------|-----------|-----------|
| 子 Agent 形态 | 图节点 | **工具** |
| 路由 | create_handoff_tool | 自定义 @tool 调 `subagent.invoke()` |
| 监督者 | create_supervisor | **create_agent**（复用阶段 3 技能） |
| 嵌套 | 复杂 | 天然支持（工具里套工具） |
| HITL | 需专门处理 | interrupt 在工具层自动上抛 |

> 🎯 **核心要点**：**"子 Agent 即工具"**——把阶段 3 的 create_agent 整个当成一个 @tool。监督者"调工具"就是"委托子任务"。统一模型：一切皆工具，工具可嵌套。

## 2. 完整实现代码

```python
"""supervisor_v2.py — 2026 官方推荐 Supervisor（工具包裹子 Agent）"""
from langchain.agents import create_agent
from langchain_core.tools import tool
from langchain_openai import ChatOpenAI
from langgraph.checkpoint.memory import InMemorySaver
import json

# ─── 子 Agent（专业 Worker）───────────────────────────────
search_agent = create_agent(
    model=ChatOpenAI(model="deepseek-v4-flash", base_url="https://api.deepseek.com"),
    tools=[web_search_tool],            # 该 Agent 只带搜索工具
    system_prompt="你是研究员，负责搜索并总结信息。只返回研究结论。",
    checkpointer=InMemorySaver(),
)

write_agent = create_agent(
    model=ChatOpenAI(model="deepseek-v4-pro"),
    tools=[],                           # 写手不需要工具
    system_prompt="你是作家，基于研究材料撰写高质量报告。",
)

# ─── 子 Agent 包装成工具（2026 官方迁移路径）────────────────
@tool
def research_topic(topic: str) -> str:
    """研究一个主题。当需要事实性信息、最新数据时调用研究员。

    Args:
        topic: 要研究的主题
    """
    result = search_agent.invoke({"messages": [("user", f"研究：{topic}")]})
    return result["messages"][-1].content

@tool
def write_report(material: str) -> str:
    """基于材料撰写报告。当需要成文输出时调用作家。

    Args:
        material: 研究材料（研究员产出）
    """
    result = write_agent.invoke({"messages": [("user", f"基于以下材料写报告：\n{material}")]})
    return result["messages"][-1].content

# ─── 监督者（一个 create_agent，工具 = 子 Agent）────────────
supervisor = create_agent(
    model=ChatOpenAI(model="deepseek-v4-pro", temperature=0.2),
    tools=[research_topic, write_report],    # ★ 子 Agent 就是工具
    system_prompt=(
        "你是团队主管。流程：先派研究员收集信息，再派作家成文。\n"
        "职责分明：研究工具只负责研究，写作工具只负责写作。"
    ),
    checkpointer=InMemorySaver(),
    recursion_limit=30,
)

# ─── 调用 ─────────────────────────────────────────────────
def run_supervisor(question: str, session: str = "s-1") -> str:
    result = supervisor.invoke(
        {"messages": [("user", question)]},
        config={"configurable": {"thread_id": session}},
    )
    return result["messages"][-1].content

# 监督者内部发生了什么：
# 监督者: "先研究" → research_topic 工具 → search_agent 内部循环
#        → 研究结论回传 → "再写作" → write_report → write_agent 内部循环
#        → 最终报告
```

| 设计要点 | 说明 |
|---------|------|
| 职责分离 | 每个子 Agent 只带自己的工具与 prompt |
| 模型分级 | 监督者用强模型，Worker 按需（研究用 flash 省钱，写作用 pro） |
| 嵌套 | research_topic 内部还能再套子 Agent（层级） |
| 超时/熔断 | 子 Agent 继承阶段 4 治理（包装时加熔断） |

## 3. Command 路由：状态更新 + 流转一体

需要手动 StateGraph 时，用 Command 替代条件边：

```python
"""command_routing.py — Command 路由（图级 Supervisor 可选）"""
from langgraph.types import Command
from langgraph.graph import StateGraph, START, END

class SuperState(dict):
    messages: list
    next: str                 # 下一个节点
    results: dict             # 各子 Agent 产出

def supervisor_node(state):
    # 决策：返回 Command（更新状态 + 指定下一节点）
    decision = route_llm(state)          # LLM 决定派谁
    return Command(
        update={"next": decision["target"]},
        goto=decision["target"],         # ★ 流转即返回
    )

def worker_a(state):
    result = agent_a.invoke(...)
    return Command(update={"results": {"a": result}}, goto="supervisor")  # 回到监督者

# 图：节点 + START → supervisor；worker 边回到 supervisor
g = StateGraph(SuperState)
g.add_node("supervisor", supervisor_node)
g.add_node("worker_a", worker_a)
g.add_edge(START, "supervisor")
g.add_edge("worker_a", "supervisor")     # Worker 完成回到监督者（迭代审查循环）
```

| Command 优势 | 说明 |
|-------------|------|
| 状态更新 + 流转一体 | 不用分别写 update 和条件边 |
| 动态路由 | goto 由运行期决定 |
| 迭代循环 | Worker 完成自动回监督者（可做多轮审查） |

## 4. 状态设计：SupervisorState

```python
"""state.py — 多 Agent 状态设计（typed state + 累加器）"""
from typing import TypedDict, Annotated, operator

class WorkerResult(TypedDict):
    worker: str
    content: str

class SupervisorState(TypedDict):
    messages: Annotated[list, operator.add]        # 累加：所有消息自动追加
    results: Annotated[list[WorkerResult], operator.add]  # ★ 累加器：收集各 Worker 产出
    current_agent: str                             # 当前执行者（调试）
    plan: str                                      # 监督者的任务分解计划
```

| 状态字段 | 用途 | reducer |
|---------|------|---------|
| messages | 对话历史 | operator.add（自动追加） |
| **results** | 各子 Agent 产出收集 | operator.add（累加不覆盖） |
| current_agent | 调试/可观测 | 覆盖（每步更新） |
| plan | 任务分解 | 覆盖 |

> 💡 **累加器陷阱**：results 用 operator.add 才不会"后一个覆盖前一个"——多 Agent 收集结果的标准姿势。

## 5. 弃用迁移：langgraph-supervisor → create_agent

| 旧（弃用） | 新（推荐） | 迁移动作 |
|-----------|-----------|---------|
| `create_supervisor(model, agents=[...])` | `create_agent(model, tools=[...])` | 子 Agent 包装成 @tool |
| `create_handoff_tool()` | 自定义 @tool 内 `subagent.invoke()` | 手写委托工具 |
| 子 Agent 作为节点 | 子 Agent 作为工具 | 改包装方式 |
| handoff 路由 | Command 或工具调用 | 简化 |

**迁移检查表**：

```text
① 子 Agent 全部改为 @tool 函数（内部 create_agent）
② 监督者用 create_agent（工具列表 = 子 Agent 工具）
③ 删除 create_supervisor / create_handoff_tool 依赖
④ 需要节点级控制时：手动 StateGraph + Command
```

> ⚠️ **判断过时资料的试金石（多 Agent 版）**：教程还在教 `create_supervisor` / `langgraph-supervisor` → 2026 已过时。

## 6. 与阶段 4 生产化融合

| 生产化要素 | 在 Supervisor 中的落点 |
|-----------|----------------------|
| 三重熔断 | 监督者层 + 每个子 Agent 工具内（双层） |
| 评估 | 子 Agent 单测 + 监督者路由评估（06 篇五模式） |
| 可观测 | 监督者 span + 子 Agent span（层级 trace） |
| HITL | interrupt 在子 Agent 工具内上抛到监督者层 |
| 成本 | 监督者 + 全部子 Agent 成本汇总（预算按会话） |

> ⚠️ **多 Agent 的成本陷阱**：一次用户请求 = 监督者 1 轮 + 子 Agent N 轮——成本翻倍起步。预算熔断必须按"整棵调用树"计算，不是单个 Agent。

## 7. 常见坑速查

| 坑 | 现象 | 修复 |
|----|------|------|
| 子 Agent 工具描述差 | 监督者不知道何时派谁 | 工具 docstring 写"何时用"（02 篇阶段 3 铁律） |
| 状态被覆盖 | 后结果顶掉前结果 | results 用 operator.add 累加器 |
| 无限迭代 | 监督者反复派活 | recursion_limit + 迭代计数进状态 |
| 上下文爆炸 | 子 Agent 结果全进主上下文 | 子 Agent 只回摘要，全文落盘 |
| 成本失控 | 单请求成本翻 3-5 倍 | 整树预算 + 熔断 |
| 依赖弃用包 | create_supervisor 报错/停更 | 迁移到工具包裹（第 5 节） |
| 路由失败无兜底 | 监督者乱派 | 路由决策校验 + 默认回退分支 |

> 🎯 **核心要点**：Supervisor 工程 = **"子 Agent 即工具"的 create_agent 组合**。2026 的简化让多 Agent 不再需要专用库——你阶段 3 学的 create_agent 就是全部基础。状态、路由、熔断、成本是四个最容易翻车的点。

---

**返回总览**：[00-阶段总览：进阶](00-阶段总览：进阶.md) / **上一模块**：[01-多 Agent 架构全景](01-多%20Agent%20架构全景.md) / **下一模块**：[03-Handoffs 模式与编排反模式](03-Handoffs%20模式与编排反模式.md)
