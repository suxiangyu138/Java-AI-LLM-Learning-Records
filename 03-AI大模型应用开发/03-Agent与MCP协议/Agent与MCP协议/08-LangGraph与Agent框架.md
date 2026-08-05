# 08 - LangGraph 与 Agent 框架

> 🎯 LangGraph = 状态图驱动的 Agent 编排。相比 LangChain 的线性 Chain，LangGraph 支持循环、分支、并行 — 这才是 Agent 真正需要的控制流

---

## 目录

1. [LangGraph 核心概念](#1-langgraph-核心概念)
2. [状态图与节点](#2-状态图与节点)
3. [条件分支与循环](#3-条件分支与循环)
4. [主流 Agent 框架对比](#4-主流-agent-框架对比)
5. [Java 后端 Agent 框架](#5-java-后端-agent-框架)

---

## 1. LangGraph 核心概念

```text
LangGraph = 用有向图定义 Agent 的执行流程

  节点 (Node) = 一个处理步骤（LLM调用/工具执行/判断）
  边 (Edge) = 节点间的流转方向
  状态 (State) = 在节点间传递的共享数据

  普通 Chain：A → B → C（线性）
  LangGraph：A → B → C → 判断 → 回到A（循环！）
                           → 分支 → D（并行！）
```

---

## 2. 状态图与节点

```python
from langgraph.graph import StateGraph, END
from typing import TypedDict, Annotated
import operator

# ① 定义状态
class AgentState(TypedDict):
    messages: Annotated[list, operator.add]  # 追加模式
    next_step: str
    tool_results: dict

# ② 定义节点
def call_llm(state):
    """LLM 推理节点"""
    response = llm.invoke(state["messages"])
    return {"messages": [response]}

def execute_tools(state):
    """工具执行节点"""
    last_msg = state["messages"][-1]
    results = {}
    for tool_call in last_msg.tool_calls:
        results[tool_call.id] = execute_tool(tool_call)
    return {"tool_results": results}

# ③ 构建图
graph = StateGraph(AgentState)
graph.add_node("llm", call_llm)
graph.add_node("tools", execute_tools)
graph.set_entry_point("llm")

# ④ 条件分支
def should_continue(state):
    last_msg = state["messages"][-1]
    if hasattr(last_msg, "tool_calls") and last_msg.tool_calls:
        return "tools"
    return END

graph.add_conditional_edges("llm", should_continue, {
    "tools": "tools",
    END: END
})
graph.add_edge("tools", "llm")  # 工具结果 → 再给 LLM

# ⑤ 编译运行
app = graph.compile()
result = app.invoke({"messages": [HumanMessage(content="北京天气？")]})
```

---

## 3. 条件分支与循环

```python
# 条件分支
def router(state):
    intent = classify_intent(state["messages"][-1])
    if intent == "search":
        return "search_node"
    elif intent == "calculate":
        return "calc_node"
    else:
        return "llm_node"

graph.add_conditional_edges("router_node", router, {
    "search_node": "search_node",
    "calc_node": "calc_node",
    "llm_node": "llm_node"
})

# 并行执行
from langgraph.graph import StateGraph
# LangGraph 支持从一个节点并行发出多个分支
graph.add_edge("splitter", "task_a")
graph.add_edge("splitter", "task_b")
graph.add_edge("splitter", "task_c")
# task_a, task_b, task_c 并行执行 → 汇聚到 merger
```

---

## 4. 主流 Agent 框架对比

| 框架 | 核心机制 | 优势 | 劣势 |
|------|----------|------|------|
| **LangGraph** | 状态图 | 灵活、可控 | 学习曲线 |
| **AutoGen** | 对话式 | 多Agent对话自然 | 流程不可控 |
| **CrewAI** | 角色扮演 | 简单直观 | 灵活性有限 |
| **Dify** | 低代码 | 可视化编排 | 复杂逻辑受限 |
| **Coze** | 低代码 | 插件生态 | 平台绑定 |

```text
选型建议：
  → 需要精确控制流程 → LangGraph
  → 多Agent自由对话 → AutoGen
  → 快速搭建原型 → CrewAI
  → 非开发人员使用 → Dify/Coze
```

---

## 5. Java 后端 Agent 框架

| 方案 | 说明 | 成熟度 |
|------|------|:---:|
| **Spring AI** | Spring 官方 AI 框架 | ⭐⭐⭐ |
| **LangChain4j** | LangChain 的 Java 移植 | ⭐⭐⭐ |
| **Python 服务 + HTTP** | Java→HTTP→Python Agent | ⭐⭐⭐⭐⭐ |

```java
// Spring AI 示例
@RestController
public class AgentController {
    
    @Autowired
    private ChatClient chatClient;
    
    @PostMapping("/agent/ask")
    public String ask(@RequestBody String question) {
        return chatClient.prompt()
            .user(question)
            .tools(new WeatherTool())  // 注册工具
            .call()
            .content();
    }
}
```

---

## 6. Agent 框架选型 2026

**主流框架的 2026 定位**（选型速查）：

| 框架 | 定位 | 特点 | 适用 |
|------|------|------|------|
| LangGraph | 图状态机编排 | 精确控制、持久化、检查点 | 复杂流程/生产 |
| LlamaIndex Agents | 数据/检索中心 | 与 RAG 深度集成 | 知识密集型 |
| AutoGen（AG2） | 对话式多 Agent | 对话驱动协作 | 研究/多角色 |
| CrewAI | 角色化团队 | 简单直观（Role-Task-Process） | 快速原型 |
| **LangChain4j** | **Java 生态** | 与 Spring 集成 | **Java 后端（本仓库重点）** |
| **Claude Code / Cursor** | **编程 Agent** | **2026 最大增长面** | IDE 场景 |

**选型决策（2026 共识）**：

```text
① 需要精确流程控制/生产级 → LangGraph（图状态机 + 检查点）
② Java 后端 → LangChain4j（Spring 生态一体）
③ 快速验证 → CrewAI/简单 ReAct
④ 编程场景 → 直接用 IDE Agent（Claude Code/Cursor），不重复造轮子
⑤ 多 Agent 跨服务 → A2A 协议（见 07 号第 5 节）

重要认知：2026 年"自研 Agent 框架"是反模式——
  框架已成熟（状态管理/检查点/可观测），自研 = 重复造轮子 + 踩坑
```

> 🎯 **核心要点**：框架选型 2026 = "**生产用 LangGraph 级（精确控制）、Java 用 LangChain4j、编程用 IDE Agent、跨服务走 A2A**"——**框架是基础设施，别自研**（除非有明确差异化需求）。

---

## 核心要点回顾

- LangGraph = 状态图驱动的 Agent → 支持循环/分支/并行
- 节点 = 处理步骤，边 = 流转方向，状态 = 共享数据
- 框架选型：LangGraph(灵活)、AutoGen(对话)、CrewAI(简单)
- Java 方案：Python Agent 服务 + HTTP 调用（成熟度最高）
