# LangGraph 实战：状态图与多 Agent 工作流

> **所属阶段**：阶段三 — Agent 智能体开发
> **前置知识**：LangChain Agent 基础
> **核心目标**：构建复杂的多步骤、多 Agent 协作工作流

---

## 1. 为什么需要 LangGraph

### LangChain Agent 的局限

```
LangChain Agent: 线性循环（Think → Act → Observe → Think → ...）
对于复杂工作流力不从心：
  - 条件分支："如果 A 则走 B 流程，否则走 C 流程"
  - 并行执行："同时查数据库和搜索网络"
  - 状态持久化："记住上一步的结果并传给下一步"
```

### LangGraph 解决方案

LangGraph 用**有向图**建模工作流，每个节点是一个处理步骤，边定义流转规则：

```
         ┌──────────┐
         │  START   │
         └────┬─────┘
              │
         ┌────▼─────┐
         │  分析任务  │
         └────┬─────┘
              │
      ┌───────┴───────┐
      │ 是否需要工具？  │
      └───┬───────┬───┘
    No     │       │    Yes
  ┌────────▼──┐ ┌──▼────────┐
  │  生成回答  │ │  调用工具   │
  └────────┬──┘ └──┬────────┘
           │       │
           └───┬───┘
               │
         ┌─────▼─────┐
         │    END    │
         └───────────┘
```

---

## 2. 核心概念

| 概念 | 说明 | 类比 |
|------|------|------|
| State | 在节点间传递的共享数据 | 工作流中的上下文对象 |
| Node | 处理函数，接收 State 返回更新 | 工作流中的一个步骤 |
| Edge | 节点间的连接 | 流程图中的箭头 |
| Conditional Edge | 根据条件动态路由 | if-else 分支 |

---

## 3. 最小可运行示例

```bash
pip install langgraph langchain-openai
```

```python
from typing import TypedDict, Literal
from langgraph.graph import StateGraph, END
from langchain_openai import ChatOpenAI
from langchain_core.tools import tool

# 1. 定义 State
class AgentState(TypedDict):
    messages: list
    next_action: str
    result: str

# 2. 定义工具
@tool
def search_docs(query: str) -> str:
    """搜索技术文档"""
    return f"文档搜索结果: {query} 的最佳实践是..."

@tool
def execute_code(code: str) -> str:
    """执行代码"""
    return f"执行结果: 代码运行成功"

llm = ChatOpenAI(model="deepseek-chat", api_key="sk-xxx",
                  base_url="https://api.deepseek.com")
tools = [search_docs, execute_code]
llm_with_tools = llm.bind_tools(tools)

# 3. 定义节点
def analyze(state: AgentState) -> AgentState:
    """分析用户意图，决定下一步"""
    last_msg = state["messages"][-1]
    response = llm.invoke(f"分析此任务需要什么操作: {last_msg}")
    state["next_action"] = "use_tools" if "搜索" in response.content or "执行" in response.content else "answer"
    return state

def use_tools(state: AgentState) -> AgentState:
    """调用工具"""
    last_msg = state["messages"][-1]
    response = llm_with_tools.invoke([{"role": "user", "content": last_msg}])
    # 实际执行 tool_calls...
    state["result"] = "工具调用完成"
    return state

def generate_answer(state: AgentState) -> AgentState:
    """生成最终回答"""
    state["result"] = "这是最终答案"
    return state

# 4. 构建图
def build_workflow():
    workflow = StateGraph(AgentState)

    workflow.add_node("analyze", analyze)
    workflow.add_node("use_tools", use_tools)
    workflow.add_node("generate_answer", generate_answer)

    workflow.set_entry_point("analyze")

    # 条件边：根据分析结果路由
    def route_after_analyze(state):
        return "use_tools" if state["next_action"] == "use_tools" else "generate_answer"

    workflow.add_conditional_edges("analyze", route_after_analyze, {
        "use_tools": "use_tools",
        "generate_answer": "generate_answer"
    })
    workflow.add_edge("use_tools", END)
    workflow.add_edge("generate_answer", END)

    return workflow.compile()

# 5. 运行
app = build_workflow()
result = app.invoke({
    "messages": ["帮我搜索 Spring Boot 自动配置原理"],
    "next_action": "",
    "result": ""
})
print(result["result"])
```

---

## 4. 多 Agent 协作

### 4.1 架构设计

```
┌────────────────────────────────────────────┐
│               Supervisor Agent              │
│          (任务分发 + 结果汇总)                │
└────┬──────────────┬──────────────┬─────────┘
     │              │              │
┌────▼─────┐  ┌─────▼────┐  ┌─────▼──────┐
│  开发     │  │  测试     │  │  运维      │
│  Agent   │  │  Agent   │  │  Agent     │
│ (写代码)  │  │ (写测试)  │  │ (部署+监控) │
└──────────┘  └──────────┘  └────────────┘
```

### 4.2 实现

```python
from langgraph.graph import StateGraph, END
from langgraph.prebuilt import create_react_agent

class MultiAgentState(TypedDict):
    task: str
    code: str
    tests: str
    deploy_status: str
    final_report: str

# 每个 Agent 是独立的 ReAct Agent
dev_agent = create_react_agent(llm, [write_code_tool, search_tool])
test_agent = create_react_agent(llm, [run_test_tool, analyze_tool])
ops_agent = create_react_agent(llm, [deploy_tool, monitor_tool])

def dev_node(state):
    result = dev_agent.invoke({"messages": [f"实现: {state['task']}"]})
    state["code"] = result["messages"][-1].content
    return state

def test_node(state):
    result = test_agent.invoke({"messages": [f"测试这段代码: {state['code']}"]})
    state["tests"] = result["messages"][-1].content
    return state

def ops_node(state):
    result = ops_agent.invoke({"messages": [f"部署并监控: {state['code']}"]})
    state["deploy_status"] = result["messages"][-1].content
    return state

def report_node(state):
    state["final_report"] = f"""
    ## 任务报告
    代码: {state['code']}
    测试: {state['tests']}
    部署: {state['deploy_status']}
    """
    return state

# 构建多 Agent 工作流
workflow = StateGraph(MultiAgentState)
workflow.add_node("dev", dev_node)
workflow.add_node("test", test_node)
workflow.add_node("ops", ops_node)
workflow.add_node("report", report_node)

workflow.set_entry_point("dev")
workflow.add_edge("dev", "test")
workflow.add_edge("test", "ops")
workflow.add_edge("ops", "report")
workflow.add_edge("report", END)

app = workflow.compile()
```

---

## 5. 人机协同（Human-in-the-Loop）

```python
from langgraph.checkpoint import MemorySaver
from langgraph.graph import StateGraph, END

# 添加检查点，支持人工审核
workflow = StateGraph(AgentState)
# ... 添加节点 ...

# 在关键步骤添加中断点（await human approval）
workflow.add_node("review", human_review_node)

memory = MemorySaver()
app = workflow.compile(
    checkpointer=memory,
    interrupt_before=["review"]  # 在执行 review 前暂停
)

# 运行到中断点
config = {"configurable": {"thread_id": "task_001"}}
result = app.invoke(initial_state, config)

# 人工审核后继续
app.invoke(None, config)  # 从检查点恢复执行
```

---

## 6. 实战模式总结

| 模式 | 适用场景 | LangGraph 实现 |
|------|----------|---------------|
| 顺序链 | 固定步骤流程 | `add_edge(A, B)` |
| 条件分支 | 根据结果决定路径 | `add_conditional_edges` |
| 并行执行 | 多任务同时进行 | `Send` API 分发 |
| 循环迭代 | 持续优化直到满足条件 | 边连回上游节点 |
| 人机协同 | 需要人工审核 | `interrupt_before` |
