# 03 - 持久化与 Human-in-the-Loop

> Checkpoint 让 Agent 拥有"记忆和时间旅行"能力；Human-in-the-Loop 把人类判断嵌入 Agent 工作流——这是 LangGraph 区别于所有 Agent 框架的核心差异化能力。

---

## 📚 目录

1. [Checkpoint 机制深度解析](#1-checkpoint-机制深度解析)
2. [三种 Checkpointer 对比](#2-三种-checkpointer-对比)
3. [时间旅行：回溯到任意状态](#3-时间旅行回溯到任意状态)
4. [Human-in-the-Loop 模式](#4-human-in-the-loop-模式)
5. [interrupt() vs interrupt_before vs interrupt_after](#5-interrupt-vs-interrupt_before-vs-interrupt_after)
6. [Command()：人工编辑后恢复执行](#6-command人工编辑后恢复执行)
7. [审批工作流实战](#7-审批工作流实战)

---

## 1. Checkpoint 机制深度解析

### 1.1 什么是 Checkpoint

```text
Checkpoint = 图在每个 Superstep 结束时的完整 State 快照

没有 Checkpoint：
  A → B → C → 超时 → 💥 全部丢失，从头开始

有 Checkpoint：
  A → B → C → 超时 → 💥
  恢复 → Checkpoint[B] ✓ → 从 B 继续，不是从头开始！
```

```python
# 不使用 Checkpoint（默认）
graph = builder.compile()
# → Stateless graph：每步执行后不保存，中断即丢失

# 使用 Checkpoint
from langgraph.checkpoint.memory import MemorySaver
graph = builder.compile(checkpointer=MemorySaver())
# → Stateful graph：每步自动保存，可恢复
```

### 1.2 Thread ID：会话隔离

```python
# 每次调用需要 config，其中 thread_id 是 Checkpoint 的 key
config = {"configurable": {"thread_id": "user-123-session-1"}}

# 第一次调用：从头执行
graph.invoke({"messages": [HumanMessage("你好")]}, config)
# → Checkpoint saved with thread_id="user-123-session-1"

# 第二次调用（同一 thread_id）：从上次的状态继续！
graph.invoke(
    {"messages": [HumanMessage("刚才说到哪了？")]},
    config
)
# → 自动加载之前的状态，messages 继续追加

# 新的 thread_id：全新对话
config2 = {"configurable": {"thread_id": "user-123-session-2"}}
graph.invoke({"messages": [HumanMessage("新话题")]}, config2)
# → 全新状态，不影响 session-1
```

### 1.3 Checkpoint 数据结构

```python
{
    "v": 1,                    # Checkpoint 版本号
    "id": "1ef...",            # Checkpoint ID
    "ts": "2024-01-01T00:00:00Z",  # 时间戳
    "channel_values": {        # State 的完整快照
        "messages": [...],
        "current_step": "tools",
        ...
    },
    "channel_versions": {      # 每个 channel 的版本号
        "messages": 5,
        "current_step": 3
    },
    "versions_seen": {         # 父 Checkpoint 版本
        "agent": {"messages": 4},
        "tools": {"messages": 3}
    },
    "pending_sends": []        # 待处理的 Send（并行用）
}
```

---

## 2. 三种 Checkpointer 对比

### 2.1 MemorySaver

```python
from langgraph.checkpoint.memory import MemorySaver

checkpointer = MemorySaver()
graph = builder.compile(checkpointer=checkpointer)
```

| 特性 | 说明 |
|------|------|
| 存储 | 内存中（Python dict） |
| 持久化 | ❌ 进程重启丢失 |
| 性能 | ⚡ 最快（纯内存） |
| 适用场景 | 开发、测试、原型 |
| **不可用于** | 生产环境 |

### 2.2 SqliteSaver

```python
from langgraph.checkpoint.sqlite import SqliteSaver

checkpointer = SqliteSaver.from_conn_string("checkpoints.db")
graph = builder.compile(checkpointer=checkpointer)
```

| 特性 | 说明 |
|------|------|
| 存储 | 本地 SQLite 文件 |
| 持久化 | ✅ 进程重启保留 |
| 性能 | 🔶 中等（本地文件 IO） |
| 适用场景 | 单机生产、小规模 |
| 限制 | 不支持分布式、无高可用 |

### 2.3 PostgresSaver

```python
from langgraph.checkpoint.postgres import PostgresSaver

checkpointer = PostgresSaver.from_conn_string(
    "postgresql://user:pass@localhost:5432/langgraph"
)
# 首次使用需要建表
await checkpointer.setup()

graph = builder.compile(checkpointer=checkpointer)
```

| 特性 | 说明 |
|------|------|
| 存储 | PostgreSQL |
| 持久化 | ✅ 企业级持久化 |
| 性能 | 🔶 网络 IO（可用连接池优化） |
| 适用场景 | **生产环境首选** |
| 优势 | 高可用、备份、多实例共享 |

### 2.4 选型决策

```text
你的场景？
│
├── 本地开发/学习 → MemorySaver
│
├── 单机小规模生产 → SqliteSaver
│   （个人项目、内部工具）
│
└── 多实例/高可用生产 → PostgresSaver
    （面向用户、需要备份恢复）
```

---

## 3. 时间旅行：回溯到任意状态

### 3.1 查看历史

```python
config = {"configurable": {"thread_id": "session-1"}}

# 获取所有历史 Checkpoint
history = list(graph.get_state_history(config))

for checkpoint in history:
    print(f"Step {checkpoint.config['configurable']['checkpoint_id']}")
    print(f"  messages: {len(checkpoint.values['messages'])}")
    # 最新在前，最旧在后

# 获取当前状态
current_state = graph.get_state(config)
print(f"Current state: {current_state.values}")
```

### 3.2 回放（Replay）

```python
# 从某个历史 Checkpoint 重新执行
checkpoint_id = history[3].config["configurable"]["checkpoint_id"]

# 方式 1：只查看，不执行
replay_config = {
    "configurable": {
        "thread_id": "session-1",
        "checkpoint_id": checkpoint_id
    }
}
state_at_that_point = graph.get_state(replay_config)

# 方式 2：回退到这个点，然后分叉（fork）
fork_config = {
    "configurable": {
        "thread_id": "session-1",
        "checkpoint_id": checkpoint_id,
        "checkpoint_ns": ""  # 创建新分支
    }
}
# 从这个点继续执行会创建新的分支
result = graph.invoke(None, fork_config)
```

### 3.3 分支（Branching）

```python
# 从同一个点分出多个分支
base_checkpoint_id = "1ef..."

# 分支 A：尝试方案 1
result_a = graph.invoke(
    {"messages": [HumanMessage("用方案1解决")]},
    {
        "configurable": {
            "thread_id": "session-1",
            "checkpoint_id": base_checkpoint_id,
            "checkpoint_ns": "branch-a"  # 分叉
        }
    }
)

# 分支 B：尝试方案 2（从同一个起点）
result_b = graph.invoke(
    {"messages": [HumanMessage("用方案2解决")]},
    {
        "configurable": {
            "thread_id": "session-1",
            "checkpoint_id": base_checkpoint_id,
            "checkpoint_ns": "branch-b"  # 另一个分叉
        }
    }
)
```

---

## 4. Human-in-the-Loop 模式

### 4.1 三种交互模式

```text
模式 1：审批（Approval）
  Agent 生成草稿 → ⏸️ 暂停 → 人类审批 → 通过/修改 → 继续

模式 2：编辑（Editing）
  Agent 执行中 → ⏸️ 暂停 → 人类编辑 State → 从编辑点继续

模式 3：引导（Guiding）
  Agent 遇到歧义 → ⏸️ 暂停 → 人类给出方向 → Agent 按方向继续
```

### 4.2 interrupt()：在 Node 内部暂停

```python
from langgraph.types import interrupt

def generate_draft(state: AgentState) -> dict:
    """生成草稿，但需要人类审批"""
    draft = llm.invoke(
        f"请为用户需求写一份方案：{state['requirement']}"
    )

    # ⏸️ 在这里暂停，等待人工审批
    approval = interrupt({
        "message": "请审批以下方案草稿",
        "draft": draft.content
    })

    # 恢复时，approval 是人工传入的值
    if approval.get("approved"):
        return {
            "draft": draft.content,
            "status": "approved"
        }
    else:
        return {
            "draft": draft.content,
            "status": "rejected",
            "feedback": approval.get("feedback", "")
        }
```

### 4.3 interrupt() 工作流程

```python
# 步骤 1：正常调用（图会暂停在 interrupt 处）
config = {"configurable": {"thread_id": "thread-1"}}

# 执行到 interrupt 时会抛出 GraphInterrupt 异常
try:
    result = graph.invoke(
        {"requirement": "需要一个用户登录模块"},
        config
    )
except GraphInterrupt:
    # 这是预期行为！图暂停了
    pass

# 步骤 2：获取当前状态
state = graph.get_state(config)
print(state.values["draft"])  # 草稿内容
# 这会包含 interrupt 中传入的 {"message": ..., "draft": ...}

# 步骤 3：人工审批后，恢复执行
from langgraph.types import Command

graph.invoke(
    Command(resume={"approved": True, "feedback": ""}),
    config
)
# → 从 interrupt 处继续，approval = {"approved": True}

# 步骤 4：查看最终结果
final_state = graph.get_state(config)
print(final_state.values["status"])  # "approved"
```

---

## 5. interrupt() vs interrupt_before vs interrupt_after

### 5.1 三种暂停方式

```python
# 方式 1：interrupt() — 在 Node 内部暂停（代码级）
def my_node(state):
    user_input = interrupt("需要你的确认")
    # 恢复后继续执行
    return {"result": process(user_input)}


# 方式 2：interrupt_before — 在执行某个 Node 前暂停（图级）
graph = builder.compile(
    checkpointer=checkpointer,
    interrupt_before=["tools", "send_email"]
    # → 每次要进入 "tools" 或 "send_email" 节点前，自动暂停
)


# 方式 3：interrupt_after — 在执行某个 Node 后暂停（图级）
graph = builder.compile(
    checkpointer=checkpointer,
    interrupt_after=["agent", "generate_draft"]
    # → 每次 "agent" 或 "generate_draft" 执行完后，自动暂停
)
```

### 5.2 对比与选型

```text
┌──────────────────┬───────────────┬──────────────────┬──────────────────┐
│      特性         │  interrupt()  │ interrupt_before │ interrupt_after  │
├──────────────────┼───────────────┼──────────────────┼──────────────────┤
│ 暂停位置          │ Node 内部     │ Node 之前        │ Node 之后        │
│ 粒度              │ 精细（代码级）│ 粗（图级）       │ 粗（图级）       │
│ 传数据给人类       │ ✅ 任意数据   │ State 自动可读   │ State 自动可读   │
│ 接收人类输入       │ ✅ resume 值  │ ❌ 只暂停        │ ❌ 只暂停        │
│ 可逆性（不动代码） │ ❌ 需改代码   │ ✅ 不改图        │ ✅ 不改图        │
│ 典型场景          │ 审批/编辑     │ 审核类节点       │ 关键节点输出审查 │
└──────────────────┴───────────────┴──────────────────┴──────────────────┘
```

---

## 6. Command()：人工编辑后恢复执行

### 6.1 Command 的核心作用

```python
from langgraph.types import Command

# Command 可以做三件事：
# 1. resume：给 interrupt() 的返回值
# 2. update：编辑 State
# 3. goto：跳转到指定 Node

# 场景 1：批准 + 继续
graph.invoke(
    Command(resume={"approved": True}),
    config
)

# 场景 2：编辑 State + 继续
graph.invoke(
    Command(
        resume={"approved": True},
        update={
            "draft": "人工修改后的版本",  # 覆盖 AI 生成的内容
            "feedback": "请更详细一些"
        }
    ),
    config
)

# 场景 3：跳转到指定 Node 重新执行
graph.invoke(
    Command(
        resume={"approved": False},
        update={"feedback": "重新生成"},
        goto="generator"  # 回退到生成步骤！
    ),
    config
)
```

### 6.2 完整的人机协作流程

```python
# 代码生成 → 人工审查 → 批准/修改/拒绝 的完整链路

class CodeReviewState(TypedDict):
    messages: Annotated[list, operator.add]
    code: str
    review_status: str  # "pending" | "approved" | "rejected"
    feedback: str

# 生成器：生成代码后暂停
def generator(state):
    code = llm.invoke(f"写函数：{state['requirement']}")
    review = interrupt({
        "action": "review_code",
        "code": code.content
    })
    # 人工审查后恢复
    if review["decision"] == "approve":
        return {"code": code.content, "review_status": "approved"}
    elif review["decision"] == "modify":
        return {
            "code": review["modified_code"],
            "review_status": "approved",
            "feedback": review.get("feedback", "")
        }
    else:
        return {"review_status": "rejected", "feedback": review.get("reason", "")}

# 前端/API 集成示例
class CodeReviewAPI:
    def submit_for_review(self, thread_id, requirement):
        config = {"configurable": {"thread_id": thread_id}}
        try:
            graph.invoke({"requirement": requirement}, config)
        except GraphInterrupt:
            state = graph.get_state(config)
            # 返回代码给审查者
            return {
                "status": "pending_review",
                "code": state.values.get("code"),
                "thread_id": thread_id
            }

    def approve(self, thread_id):
        config = {"configurable": {"thread_id": thread_id}}
        graph.invoke(
            Command(resume={"decision": "approve"}),
            config
        )
        return graph.get_state(config).values

    def reject(self, thread_id, reason):
        config = {"configurable": {"thread_id": thread_id}}
        graph.invoke(
            Command(resume={"decision": "reject", "reason": reason}),
            config
        )
        return graph.get_state(config).values

    def modify(self, thread_id, modified_code, feedback=""):
        config = {"configurable": {"thread_id": thread_id}}
        graph.invoke(
            Command(resume={
                "decision": "modify",
                "modified_code": modified_code,
                "feedback": feedback
            }),
            config
        )
        return graph.get_state(config).values
```

---

## 7. 审批工作流实战

### 7.1 多级审批模式

```python
class ApprovalFlowState(TypedDict):
    messages: Annotated[list, operator.add]
    draft: str
    approvals: Annotated[dict, merge_dict]
    # {"tech_lead": "pending", "security": "pending", "manager": "pending"}

def draft_node(state):
    draft = llm.invoke(f"为需求生成方案：{state['requirement']}")
    return {"draft": draft.content}

def tech_review(state):
    result = interrupt({
        "role": "tech_lead",
        "draft": state["draft"],
        "message": "请技术负责人审批技术方案"
    })
    return {"approvals": {"tech_lead": "approved" if result["ok"] else "rejected"}}

def security_review(state):
    result = interrupt({
        "role": "security",
        "draft": state["draft"],
        "message": "请安全负责人审批"
    })
    return {"approvals": {"security": "approved" if result["ok"] else "rejected"}}

def manager_review(state):
    result = interrupt({
        "role": "manager",
        "draft": state["draft"],
        "message": "请项目经理最终审批"
    })
    return {"approvals": {"manager": "approved" if result["ok"] else "rejected"}}

def check_all_approved(state):
    approvals = state["approvals"]
    if all(v == "approved" for v in approvals.values()):
        return "publish"
    return "revision"

# 图结构：
# draft → tech → security → manager → [all ok?] → publish
#                                    → [reject]   → revision → draft
```

### 7.2 并行审批

```python
# 三个审批人同时审批（不需要按顺序等）
from langgraph.graph import Send

def fan_out_approval(state):
    """将审批请求并行发给三个审批人"""
    roles = ["tech_lead", "security", "manager"]
    return [
        Send("approver", {"role": role, "draft": state["draft"]})
        for role in roles
    ]

def approver(state):
    role = state["role"]
    result = interrupt({
        "role": role,
        "draft": state["draft"],
        "message": f"请{role}审批"
    })
    return {"approvals": {role: result["decision"]}}

# tech_lead, security, manager 同时收到审批请求
# 任意一个拒绝 → 回退
# 全部通过 → 发布
```

---

> 🎯 **核心要点**：Checkpoint + Human-in-the-Loop 是 LangGraph 的**杀手特性**。没有这两个能力的 Agent 框架只是"自动脚本"，有了它们才是"协作系统"。记忆（MemorySaver/SqliteSaver/PostgresSaver）选择直接影响生产可靠性。`interrupt()` + `Command(resume=...)` 是 Human-in-the-Loop 的标准范式。

---

**下一模块**：[04 - 流式输出与调试](./04-流式输出与调试.md)  
**返回总览**：[00 - LangGraph 知识体系总览](./00-LangGraph知识体系总览.md)
