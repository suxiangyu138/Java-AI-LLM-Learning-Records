# 04 多 Agent 状态与上下文工程

> 定位：多 Agent 系统的"内存管理"——typed state 设计、reducer 累加、上下文传递纪律、子 Agent 隔离（2026-08 基准）

## 📚 目录

1. [多 Agent 状态的三个层次](#1-多-agent-状态的三个层次)
2. [typed state 设计模式](#2-typed-state-设计模式)
3. [reducer 累加器：收集 vs 覆盖](#3-reducer-累加器收集-vs-覆盖)
4. [上下文传递纪律](#4-上下文传递纪律)
5. [子 Agent 隔离：上下文不串扰](#5-子-agent-隔离上下文不串扰)
6. [共享记忆：多 Agent 共同读写](#6-共享记忆多-agent-共同读写)

## 1. 多 Agent 状态的三个层次

```text
层次一：图状态（SupervisorState）—— 编排层共享：计划、路由、收集结果
层次二：子 Agent 内部状态 —— 每个 Agent 自己的 messages（checkpointer 隔离）
层次三：跨会话共享存储（store）—— 长期记忆：事实、偏好、历史结论
```

| 层次 | 生命周期 | 谁可见 | 典型内容 |
|:---:|---------|--------|---------|
| 图状态 | 单次任务 | 全部节点 | plan、results、next |
| 子 Agent 状态 | 子任务 | 仅该子 Agent | 子任务的对话历史 |
| 共享存储 | 长期 | 按需注入 | 用户偏好、业务事实 |

> 🎯 **核心要点**：状态分层的原则 = **"能隔离的别共享，能按需的别常驻"**。子 Agent 的中间对话绝不进主上下文；共享存储只有工具主动读才注入（阶段 4 记忆工程的多 Agent 版）。

## 2. typed state 设计模式

```python
"""state.py — 三种常见多 Agent 状态的 typed 设计"""
from typing import TypedDict, Annotated, Literal, operator


# 模式一：Supervisor 状态
class SupervisorState(TypedDict):
    messages: Annotated[list, operator.add]
    plan: str                                # 任务分解（监督者产出）
    results: Annotated[dict, operator.or_]   # 各 Worker 产出（按名收集）
    next: Literal["worker_a", "worker_b", "END"]


# 模式二：Handoffs 状态
class HandoffState(TypedDict):
    messages: Annotated[list, operator.add]
    context: dict                            # 交接上下文
    current_agent: str                       # 调试
    next_agent: Literal["researcher", "writer", "editor", "END"]


# 模式三：并行 fan-out 状态
class FanoutState(TypedDict):
    tasks: list[str]                         # 任务分片
    outputs: Annotated[list, operator.add]   # 各片结果（顺序无关）
    status: Literal["running", "partial", "complete", "failed"]
```

| 设计纪律 | 说明 |
|---------|------|
| 显式类型 | Literal 限定路由目标（非法路由编译期可见） |
| 累加字段 | 收集类字段必须 reducer |
| 计划字段 | 监督者的分解结果单独存（可审计） |
| 状态字段 | status 做整体进度（供 HITL/熔断判断） |

## 3. reducer 累加器：收集 vs 覆盖

| reducer | 行为 | 用途 |
|---------|------|------|
| `operator.add` | 追加（列表拼接） | 收集所有 Worker 产出 |
| `operator.or_` | 合并（dict 覆盖键） | 按名收集结果 |
| 默认（无 reducer） | 覆盖 | 单写字段（plan/next） |

```python
# 错误示例：无 reducer 的收集字段
class BadState(TypedDict):
    results: list          # ❌ 每个节点返回都会覆盖前一个

# 正确示例：
class GoodState(TypedDict):
    results: Annotated[list, operator.add]   # ✅ 自动累加
```

> ⚠️ **多 Agent 最隐蔽的 bug**：Worker 并行返回结果，最后一个覆盖前面的——"为什么只有最后一个 Agent 的结果？" → 查 reducer。

## 4. 上下文传递纪律

| 纪律 | 做法 | 原因 |
|------|------|------|
| 传必要不传全部 | 子 Agent 只收"任务 + 必要上下文" | 防膨胀、防污染 |
| 大产出转摘要 | Worker 结果 >1K token 先压缩 | 主上下文预算 |
| 全量落盘 | 原始产出写文件/存储，摘要入上下文 | 可恢复 |
| 约束贯穿 | 用户约束（禁止项）必须全程传递 | 防子 Agent 越界 |
| 审计留痕 | 每次交接记录"传了什么" | 排障 |

```python
def prepare_subtask(task: str, global_constraints: list[str],
                    relevant_context: str | None = None) -> dict:
    """给子 Agent 的最小上下文包"""
    return {
        "task": task,
        "constraints": global_constraints,       # 全程约束必须带
        "context": (relevant_context or "")[:2000],   # 精选截断
    }
```

> 💡 与阶段 4 的联动：多 Agent 上下文 = 单 Agent 上下文工程 × 交接次数——**每一次交接都是一次"压缩 + 精选"的机会，不是复制**。

## 5. 子 Agent 隔离：上下文不串扰

**隔离的三重保证**：

```text
① 消息隔离：子 Agent 的 messages 不进主上下文（只有返回摘要）
② checkpointer 隔离：每个子 Agent 独立 thread/checkpointer（不共享会话历史）
③ 工具隔离：子 Agent 只见自己的工具集（最小工具面）
```

```python
# 每个子 Agent 独立 checkpointer（防止串会话）
search_agent = create_agent(..., checkpointer=InMemorySaver())
write_agent = create_agent(..., checkpointer=InMemorySaver())
# 工具包裹时各调各的 config：
# research_topic 内部：search_agent.invoke(..., config={"configurable": {"thread_id": f"sub-{parent_session}"}})
```

| 隔离收益 | 说明 |
|---------|------|
| 防上下文污染 | 研究 Agent 的噪声不干扰写作 Agent |
| 防注入扩散 | 一个子 Agent 被注入不直接传染其它（失败模式 07 篇） |
| 独立生命周期 | 子任务失败可重试，不影响全局 |
| 可审计 | 每层消息可单独回放 |

## 6. 共享记忆：多 Agent 共同读写

需要多个 Agent 读写同一份事实时，用共享存储（阶段 4 store 的多 Agent 版）：

```python
"""shared_memory.py — 多 Agent 共享记忆"""
from langgraph.store.memory import InMemoryStore
from langchain_core.tools import tool, InjectedStore

store = InMemoryStore()     # 生产换 PostgresStore

@tool
def team_remember(key: str, fact: str, store: InjectedStore) -> str:
    """团队共享记忆：写入关键事实，所有 Agent 可读取。"""
    store.put(("team_facts", "default"), key, {"fact": fact})
    return "已写入团队记忆"

@tool
def team_recall(key: str = "", store: InjectedStore) -> str:
    """读取团队共享记忆。"""
    if key:
        item = store.get(("team_facts", "default"), key)
        return item.value["fact"] if item else "无记录"
    items = store.search(("team_facts", "default"))
    return "\n".join(f"- {k}: {i.value['fact']}" for k, i in items.items()) or "暂无"

# 每个子 Agent 都挂这两个工具 → 全团队共享
search_agent = create_agent(..., tools=[search_tool, team_remember, team_recall], store=store)
write_agent  = create_agent(..., tools=[write_tool, team_remember, team_recall], store=store)
```

| 共享记忆纪律 | 原因 |
|------------|------|
| 写入前校验 | 一个 Agent 的错误事实会毒化全队（级联幻觉！） |
| 按需读取 | 不自动注入，工具显式读 |
| 写入审计 | 谁写的、什么时候（可追溯） |
| 冲突策略 | 同键冲突：新覆盖旧 or 版本化（按业务） |

> ⚠️ **共享记忆 = 级联幻觉放大器**：一个子 Agent 的幻觉写入共享记忆 → 全队引用 → CHARM 级联（失败模式 02 篇）。**写入前必须过验证层**。

> 🎯 **核心要点**：多 Agent 状态工程 = **三层分离（图状态/子状态/共享存储）+ reducer 纪律 + 交接压缩 + 子 Agent 隔离**。记忆口诀：能隔离的别共享、能按需的别常驻、能压缩的别复制、能审计的别静默。

---

**返回总览**：[00-阶段总览：进阶](00-阶段总览：进阶.md) / **上一模块**：[03-Handoffs 模式与编排反模式](03-Handoffs%20模式与编排反模式.md) / **下一模块**：[05-MCP 协议接入工程](05-MCP%20协议接入工程.md)
