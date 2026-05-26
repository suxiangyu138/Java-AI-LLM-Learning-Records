# LangGraph 状态图与多 Agent 协作

> **核心认知**：单 Agent 能做的有限，复杂任务需要把工作拆分成多个步骤/多个 Agent 协作。LangGraph 用**有向图**来编排 Agent 的执行流程。
> **前置阅读**：`AI Agent核心知识点.md`、`ReAct模式与Function-Calling实战.md`

---

## 1. 为什么需要 LangGraph？

```
LangChain 的局限                    LangGraph 的能力
─────────────────                   ─────────────────
线性 Chain: A → B → C              状态图:  A → B → C
                                              ↘ D ↗
无法循环 √
无法条件分支 √                      循环 + 条件路由 + 并行 + 人机协同
Agent 执行不可控 √                  每一步都是可观察、可中断的 Node
```

---

## 2. LangGraph 核心概念

### 2.1 三大要素

```
State（状态）：贯穿整个图的数据对象，每个 Node 读取 State，输出更新后的 State
Node（节点）：一个执行单元 = Python 函数，接收 State → 返回 State 更新
Edge（边）：节点之间的连接，可以是直线、条件分支、或循环
```

### 2.2 最简单的 Graph

```bash
pip install langgraph langchain-openai
```

```python
from typing import TypedDict
from langgraph.graph import StateGraph, END


# 1. 定义 State
class AgentState(TypedDict):
    query: str
    context: str
    answer: str
    step_count: int


# 2. 定义 Node 函数
def retrieve(state: AgentState) -> AgentState:
    """检索相关知识"""
    print(f"[Step {state['step_count']}] 检索中...")
    # 模拟检索
    return {
        "context": f"关于 '{state['query']}' 的检索结果...",
        "step_count": state["step_count"] + 1
    }

def generate(state: AgentState) -> AgentState:
    """基于检索结果生成回答"""
    print(f"[Step {state['step_count']}] 生成回答...")
    # 实际调用 LLM
    return {
        "answer": f"根据检索结果，{state['query']} 的答案是...",
        "step_count": state["step_count"] + 1
    }

# 3. 构建 Graph
graph = StateGraph(AgentState)

graph.add_node("retrieve", retrieve)
graph.add_node("generate", generate)

graph.set_entry_point("retrieve")  # 入口
graph.add_edge("retrieve", "generate")
graph.add_edge("generate", END)    # 出口

app = graph.compile()

# 4. 运行
result = app.invoke({"query": "Spring Boot 如何配置多数据源？", "step_count": 1})
print(result["answer"])
```

---

## 3. 条件路由（核心特性）

```python
from langgraph.graph import StateGraph, END


def router(state: AgentState) -> str:
    """根据状态决定下一步走向"""
    if state["context"]:  # 有检索结果 → 生成答案
        return "generate"
    elif state["step_count"] > 3:  # 重试过多次 → 直接回答
        return "fallback"
    else:
        return "rewrite_query"  # 改写查询重新检索


graph = StateGraph(AgentState)

graph.add_node("retrieve", retrieve)
graph.add_node("generate", generate)
graph.add_node("rewrite_query", rewrite_query)
graph.add_node("fallback", fallback_answer)

graph.set_entry_point("retrieve")

# 条件边：根据 router 返回值决定走向
graph.add_conditional_edges(
    "retrieve",
    router,
    {
        "generate": "generate",
        "rewrite_query": "rewrite_query",
        "fallback": "fallback",
    }
)
graph.add_edge("rewrite_query", "retrieve")  # 改写后重新检索（循环）
graph.add_edge("generate", END)
graph.add_edge("fallback", END)
```

---

## 4. 多 Agent 协作架构

### 4.1 Supervisor 模式（推荐）

```
                      ┌──────────────────┐
                      │   Supervisor      │
                      │   (调度 Agent)     │
                      └──┬───────┬──────┬─┘
                         │       │      │
              ┌──────────▼┐ ┌────▼──┐ ┌─▼─────────┐
              │ Developer │ │ Test │ │ Reviewer   │
              │ Agent     │ │Agent │ │ Agent      │
              └───────────┘ └──────┘ └────────────┘
```

```python
from langgraph.graph import StateGraph, END
from langgraph.prebuilt import create_react_agent
from typing import Literal


class TeamState(TypedDict):
    messages: list[dict]
    next_agent: str
    task_result: str


# 定义各个专业 Agent 的工具集
dev_tools = [search_docs, write_code, run_test]
test_tools = [run_unit_test, check_coverage, generate_test_case]
review_tools = [review_code_style, check_security, check_performance]

# 创建各个 Agent
dev_agent = create_react_agent(llm, dev_tools, system_prompt="你是开发工程师...")
test_agent = create_react_agent(llm, test_tools, system_prompt="你是测试工程师...")
review_agent = create_react_agent(llm, review_tools, system_prompt="你是代码审查员...")


def supervisor(state: TeamState) -> TeamState:
    """Supervisor 决定下一步分配给哪个 Agent"""
    # 根据任务状态分派
    if state.get("task_result") == "code_written":
        return {"next_agent": "test"}
    elif state.get("task_result") == "tested":
        return {"next_agent": "review"}
    elif state.get("task_result") == "approved":
        return {"next_agent": "END"}
    else:
        return {"next_agent": "dev"}


def supervisor_router(state: TeamState) -> Literal["dev", "test", "review", "END"]:
    if state["next_agent"] == "END":
        return "END"
    return state["next_agent"]


graph = StateGraph(TeamState)

graph.add_node("supervisor", supervisor)
graph.add_node("dev", dev_agent)
graph.add_node("test", test_agent)
graph.add_node("review", review_agent)

graph.set_entry_point("supervisor")
graph.add_conditional_edges("supervisor", supervisor_router, {
    "dev": "dev", "test": "test", "review": "review", "END": END
})
graph.add_edge("dev", "supervisor")     # 开发完回到 supervisor
graph.add_edge("test", "supervisor")     # 测试完回到 supervisor
graph.add_edge("review", "supervisor")   # 审查完回到 supervisor
```

### 4.2 顺序协作模式

```
需求分析 → 编码实现 → 单元测试 → 代码审查 → 合并
   Node 1    Node 2     Node 3     Node 4    Node 5
```

```python
graph = StateGraph(CodeState)

graph.add_node("analyze", lambda s: {"design_doc": design(s["requirement"])})
graph.add_node("implement", implement_feature)
graph.add_node("test", run_tests)
graph.add_node("review", code_review)

graph.set_entry_point("analyze")
graph.add_edge("analyze", "implement")
graph.add_edge("implement", "test")

# 测试不通过 → 回到实现
def test_router(state):
    return "implement" if state["test_failed"] else "review"

graph.add_conditional_edges("test", test_router, {
    "implement": "implement",
    "review": "review"
})
graph.add_edge("review", END)
```

---

## 5. 高级模式：人机协同（Human-in-the-Loop）

```python
from langgraph.checkpoint.memory import MemorySaver
from langgraph.types import interrupt


def critical_operation(state: AgentState) -> AgentState:
    """执行关键操作前需人工确认"""
    if state.get("action") == "delete_database":
        # interrupt 暂停执行，等待人工批准
        user_approved = interrupt({
            "question": "确定要删除数据库吗？(yes/no)",
            "action": "delete_database",
            "details": state.get("details")
        })
        if user_approved != "yes":
            return {"status": "cancelled", "message": "用户取消了操作"}

    # 继续执行
    return execute_safe_operation(state)


# 编译时启用 checkpoint
app = graph.compile(checkpointer=MemorySaver())

# 执行到 interrupt 会暂停
config = {"configurable": {"thread_id": "session-1"}}
result = app.invoke({"action": "delete_database"}, config)
# → 暂停，等待人工确认

# 人工确认后继续
app.invoke(Command(resume="yes"), config)
# → 继续执行
```

---

## 6. 实战：智能运维 Agent 工作流

```python
"""
完整运维 Agent 工作流：

用户报告问题 → 分类 Agent（判断类型）→ 诊断 Agent（分析根因）
                                            ↓
                                      修复 Agent（执行修复）
                                            ↓
                                      验证 Agent（验证修复效果）
                                            ↓
                                      回复 Agent（生成报告）
"""

from typing import TypedDict


class OpsState(TypedDict):
    user_report: str
    category: str          # "database" | "network" | "application"
    diagnosis: str
    fix_result: str
    verified: bool
    final_report: str


def classify(state: OpsState) -> OpsState:
    """分类 Agent：判断问题类型"""
    prompt = f"""根据用户报告判断问题类型：
报告：{state['user_report']}

类型选项：database（数据库）, network（网络）, application（应用）
只返回类型名称。"""
    category = llm.invoke(prompt).content.strip()
    return {"category": category}

def diagnose(state: OpsState) -> OpsState:
    """诊断 Agent：根据类型执行不同的诊断逻辑"""
    if state["category"] == "database":
        # 检查数据库连接、慢查询等
        diagnosis = check_database_health()
    elif state["category"] == "network":
        diagnosis = check_network_connectivity()
    else:
        diagnosis = check_application_logs()
    return {"diagnosis": diagnosis}

def fix(state: OpsState) -> OpsState:
    """修复 Agent"""
    prompt = f"根据诊断结果，生成修复方案：\n{state['diagnosis']}"
    fix_plan = llm.invoke(prompt).content
    # 执行修复...
    return {"fix_result": fix_plan}

def verify(state: OpsState) -> OpsState:
    """验证 Agent"""
    # 运行验证测试
    success = run_verification_tests()
    return {"verified": success}

def report(state: OpsState) -> OpsState:
    """回复 Agent"""
    prompt = f"""生成运维报告：
- 问题：{state['user_report']}
- 类型：{state['category']}
- 诊断：{state['diagnosis']}
- 修复：{state['fix_result']}
- 验证：{'通过' if state['verified'] else '未通过'}"""
    report_text = llm.invoke(prompt).content
    return {"final_report": report_text}


# 构建工作流
graph = StateGraph(OpsState)
for name, node in [
    ("classify", classify),
    ("diagnose", diagnose),
    ("fix", fix),
    ("verify", verify),
    ("report", report),
]:
    graph.add_node(name, node)

graph.set_entry_point("classify")
graph.add_edge("classify", "diagnose")
graph.add_edge("diagnose", "fix")
graph.add_edge("fix", "verify")

# 验证不通过则重新修复
graph.add_conditional_edges("verify",
    lambda s: "fix" if not s["verified"] else "report",
    {"fix": "fix", "report": "report"}
)
graph.add_edge("report", END)

ops_app = graph.compile()
```

---

## 7. 生产环境注意事项

| 要点 | 说明 |
|------|------|
| **时限保护** | 每个 Node 设置超时，防止单个步骤卡死 |
| **最大步数** | `graph.compile()` 配合循环时设 `recursion_limit` |
| **Checkpoint** | 持久化状态，支持断点续传和调试回放 |
| **流式输出** | 使用 `app.stream()` 代替 `invoke()`，实时展示进度 |
| **成本追踪** | 记录每个 Node 的 Token 消耗，做成本归因 |

```python
app = graph.compile(
    checkpointer=MemorySaver(),
)
# 设置最大递归次数（防止无限循环）
config = {"configurable": {"thread_id": "1"}, "recursion_limit": 25}

# 流式执行
for event in app.stream(initial_state, config):
    for node_name, output in event.items():
        print(f"[{node_name}] {output}")
```

---

## 快速调试检查清单

- [ ] State 定义是否包含了所有 Node 需要共享的数据？
- [ ] 条件边的路由函数是否覆盖了所有可能状态？（缺少分支会报错）
- [ ] 循环的图是否设置了 `recursion_limit`？
- [ ] 多 Agent 的观察结果是否正确传递给 Supervisor？
- [ ] `interrupt` 暂停后的恢复逻辑是否正确？
