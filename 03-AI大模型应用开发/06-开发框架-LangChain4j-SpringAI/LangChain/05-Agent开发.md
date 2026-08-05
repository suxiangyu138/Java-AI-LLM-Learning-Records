# 05 - Agent 开发

> **核心摘要**：1.x Agent 开发只有一条路——`create_agent(model, tools, ...)`，内部自动生成 LangGraph 状态机（ReAct 循环），开箱即得流式、持久化、HITL、结构化输出。配套 **Middleware 中间件**插拔横切能力（摘要/PII/重试/人工审批），v1.3 新增 typed-projection 事件流式。

> **前置阅读**：[[03-模型与输出解析]] | [[04-RAG构建]]

---

## 📚 目录

1. [Agent 的本质：ReAct 循环](#1-agent-的本质react-循环)
2. [create_agent 统一 API](#2-create_agent-统一-api)
3. [工具定义最佳实践](#3-工具定义最佳实践)
4. [记忆与持久化](#4-记忆与持久化)
5. [Middleware 中间件](#5-middleware-中间件)
6. [结构化输出](#6-结构化输出)
7. [事件流式 stream_events（v1.3）](#7-事件流式-stream_eventsv13)
8. [多 Agent 协作](#8-多-agent-协作)
9. [生产化清单](#9-生产化清单)
10. [核心要点](#10-核心要点)

---

## 1. Agent 的本质：ReAct 循环

> **背景**：0.x 有十几种 Agent 类型（ReAct/Plan-Execute/Self-Ask……），选型地狱；1.x 统一为 create_agent。
> **目的**：理解 Agent 主循环，才能用好 create_agent 的参数。
> **适用范围**：所有需要「模型自主决策 + 调用工具」的应用。

```text
Agent 主循环（ReAct：Reason + Act）
┌────────────┐  思考    ┌────────────┐  调用   ┌────────────┐
│  LLM 模型  │ ──────→ │  工具选择  │ ─────→ │   工具执行  │
│            │ ←────── │            │ ←───── │            │
└────────────┘  行动观察 └────────────┘  结果回填 └────────────┘
      ↑                                        │
      └──────── 循环直到模型声明「结束」←───────┘

create_agent 内部自动实现这个循环（LangGraph 状态机）
├── 谁循环：模型自己决定（调工具 / 直接回答）
├── 何时停：模型输出最终答案（无 tool_calls）
└── 你能控制：工具集、模型、中间件、状态持久化
```

**for 循环 vs while 循环（Agent 的分界）**：

| | LCEL（for） | Agent（while） |
|--|:---:|:---:|
| 步骤数 | 固定（编译期确定） | 运行时决定（1-N 步） |
| 分支 | 代码写死 | 模型根据观察决策 |
| 调试 | 路径确定 | 路径随输入变化 |
| 典型 | QA 管道 | 多工具工作流 |

---

## 2. create_agent 统一 API

> 🎯 **1.x 唯一 Agent 入口**——AgentExecutor / create_react_agent 已弃用（维护至 2026.12），新代码一律 create_agent。

```python
from langchain.agents import create_agent
from langgraph.checkpoint.memory import InMemorySaver

def get_weather(city: str) -> str:
    """查询城市天气"""
    return f"{city}：25°C，晴"

agent = create_agent(
    model="anthropic:claude-sonnet-4-6",   # 统一模型字符串
    tools=[get_weather],                   # 工具列表
    prompt="你是一位天气助手，回答简洁，用摄氏度。",
    # 可选能力：
    checkpointer=InMemorySaver(),          # 持久化（多轮记忆）
    response_format=Answer,                # 结构化输出
    middleware=[...],                      # 中间件
    state_schema=SupportState,             # 自定义状态（高级）
)

result = agent.invoke({
    "messages": [{"role": "user", "content": "上海天气如何？"}],
})
print(result["messages"][-1].content)      # 最终回答
```

**参数速查**：

| 参数 | 作用 | 不传时的默认 |
|------|------|-------------|
| `model` | 必选；支持字符串或 ChatModel 对象 | — |
| `tools` | 工具列表（可空 = 纯对话） | [] |
| `prompt` | 系统提示（可含 {tool_names} 占位） | 默认提示 |
| `checkpointer` | 状态持久化 | 无（每次独立） |
| `response_format` | Pydantic 结构化输出 | 纯文本 |
| `middleware` | 横切能力注入 | 无 |
| `state_schema` | 自定义状态字段（TypedDict/Pydantic） | 默认消息流 |

> 💡 **系统提示里的工具说明**：1.x 自动注入工具 schema；`prompt` 里只需写「角色 + 行为规则 + 边界」，不要手动罗列工具参数。

---

## 3. 工具定义最佳实践

> 🎯 **工具质量 = Agent 上限**——docstring 是模型唯一的「说明书」，写不好工具等于模型瞎猜。

```python
from langchain_core.tools import tool

@tool
def get_weather(city: str, unit: str = "celsius") -> str:
    """根据城市名查询实时天气。

    Args:
        city: 中文或英文城市名，如 "上海"、"Beijing"。
        unit: 温度单位，celsius（摄氏）或 fahrenheit（华氏）。

    Returns:
        天气状况与温度，如 "25°C，晴转多云"。
    """
    ...
```

**工具写作四原则**：

```text
├── ① 写清「何时调用」：开头第一句说明触发场景（"当问题涉及……时调用"）
├── ② 参数说明要具体：枚举合法值，注明格式（中文/英文、单位）
├── ③ 返回结构明确：让模型知道拿到的数据长什么样
├── ④ 边界说透：何时不该调（"仅限国内城市"）、失败返回什么
└── 金句：把工具当「给实习生写的交接文档」
```

**结构化工具（参数复杂时用 Pydantic）**：

```python
from pydantic import BaseModel, Field

class BookInput(BaseModel):
    flight_no: str = Field(description="航班号，如 CA1234")
    date: str = Field(description="日期 YYYY-MM-DD")

@tool(args_schema=BookInput)
def check_flight(flight_no: str, date: str) -> str:
    """查询航班动态。注意：仅支持 30 天内的航班。"""
    ...
```

> ⚠️ **工具失败的三种处理**：① 返回错误说明字符串（模型可读，可自我修正重试）；② 抛异常（v1.3 ToolErrorMiddleware 拦截转换）；③ 返回 None 占位。生产推荐 ①+② 组合——异常交给 ToolRetryMiddleware 重试，仍失败则回退。

---

## 4. 记忆与持久化

> 🎯 **checkpointer = Agent 的记忆**——传入后同 `thread_id` 的对话自动恢复，崩溃/重启不丢状态（这是 create_agent 底层 LangGraph 的能力）。

```python
from langgraph.checkpoint.memory import InMemorySaver
from langgraph.checkpoint.sqlite import SqliteSaver   # 生产用持久化存储

agent = create_agent(
    model="openai:gpt-5.5",
    tools=[...],
    checkpointer=SqliteSaver.from_conn_string("agent.db"),
)

# 多轮对话：同一 thread_id = 同一会话
config = {"configurable": {"thread_id": "user-1001"}}
agent.invoke({"messages": [{"role": "user", "content": "我在上海工作"}]}, config)
agent.invoke({"messages": [{"role": "user", "content": "我住哪个城市？"}]}, config)
# → 正确回答：上海
```

**Saver 选型**：

| Saver | 存储 | 场景 |
|-------|------|------|
| `InMemorySaver` | 内存 | 测试/单进程 |
| `SqliteSaver` | SQLite 文件 | 单机生产 |
| `PostgresSaver` | PostgreSQL | 多实例生产（推荐） |
| 自定义 CheckpointSaver | 任意 | 对接企业存储 |

**上下文管理边界**：

```text
长对话的问题（2026 现实）
├── 上下文越长 → 越贵、越慢、越容易漂移
├── SummarizationMiddleware：接近 token 上限自动摘要压缩历史
├── 分段策略：定期归档旧消息，保留最近 N 轮 + 摘要
└── 金句：记忆 ≠ 全量保留——「该忘的忘掉」也是记忆设计
```

---

## 5. Middleware 中间件

> 🎯 **中间件 = 横切能力的插拔式注入**——模型调用前/后、工具调用前/后、Agent 生命周期钩子，不侵入核心代码。

```python
# ① 内置中间件（开箱即用）
from langchain.agents.middleware import (
    SummarizationMiddleware,        # 长对话自动摘要
    HumanInTheLoopMiddleware,       # 高风险操作人工审批
    PIIMiddleware,                  # 敏感信息脱敏
    ToolRetryMiddleware,            # 工具失败指数退避重试（v1.2+）
    ToolErrorMiddleware,            # 工具异常标准化（v1.3）
)

agent = create_agent(
    model="...",
    tools=tools,
    middleware=[
        PIIMiddleware(),                    # 输入脱敏（如身份证/手机号）
        ToolRetryMiddleware(max_retries=2), # 失败重试
        SummarizationMiddleware(),          # 上下文压缩
    ],
)
```

**钩子一览（自定义中间件）**：

| 钩子 | 时机 | 典型用途 |
|------|------|---------|
| `@before_agent` | Agent 开始前 | 初始化、鉴权 |
| `@before_model` | 模型调用前 | 注入/改写请求 |
| `@wrap_model_call` | 包住模型调用 | 日志、限流、结构化 schema 注入 |
| `@wrap_tool_call` | 包住工具调用 | 权限校验、审计 |
| `@after_model` | 模型返回后 | 结果校验、降级 |
| `@after_agent` | Agent 结束后 | 清理、统计 |

```python
from langchain.agents.middleware import wrap_model_call

@wrap_model_call
async def log_model(request, handler):
    """所有模型调用前打日志（工厂级横切）"""
    result = await handler(request)
    print(f"[model] {request.model} → {result}")
    return result
```

> ⚠️ **中间件顺序敏感**：列表顺序即执行顺序（先注册先包裹）；HITL 中间件建议放在**外层**（最优先拦截）。

---

## 6. 结构化输出

> 🎯 **create_agent 的 response_format 在主循环内直接产出结构体**——相比「先聊天再解析」省一次 LLM 调用，且模型可自主选择「调工具」或「原生结构化」策略。

```python
from pydantic import BaseModel
from langchain.agents import create_agent

class CustomerSupport(BaseModel):
    intent: str                 # 意图分类
    sentiment: float            # 情感分 -1~1
    reply: str                  # 回复草稿

agent = create_agent(
    model="openai:gpt-5.5",
    tools=tools,
    response_format=CustomerSupport,
)
result = agent.invoke({"messages": [{"role": "user", "content": "我要退订！"}]})
out = result["structured_response"]
# → CustomerSupport(intent="cancellation", sentiment=-0.8, reply="很抱歉……")
```

**与 LCEL 结构化输出的分工**：

| 场景 | 方式 |
|------|------|
| 纯生成链的结构化输出 | LCEL + PydanticOutputParser（见 [[03-模型与输出解析]]） |
| Agent（带工具循环）的结构化输出 | `response_format`（主循环内） |
| 需要 strict 约束跨厂商 | `ProviderStrategy(schema=..., strict=True)` |

---

## 7. 事件流式 stream_events（v1.3）

> 🎯 **v1.3 新增 typed-projection 事件流**——按通道（messages/tool_calls/values）分别消费，不再靠 `stream_mode` 分支判断，前端打字机 + 工具状态展示的最佳姿势。

```python
from langchain.agents import create_agent
from langgraph.checkpoint.memory import InMemorySaver
from langchain_core.utils.uuid import uuid7

agent = create_agent(
    model="anthropic:claude-sonnet-4-6",
    tools=[get_weather],
    checkpointer=InMemorySaver(),
)
config = {"configurable": {"thread_id": str(uuid7())}}

stream = agent.stream_events(
    {"messages": [{"role": "user", "content": "上海和北京天气对比"}]},
    config=config,
    version="v3",            # ← v1.3 事件流版本
)

for kind, item in stream.interleave("messages", "tool_calls"):
    if kind == "messages":
        for token in item.text:
            print(token, end="", flush=True)
    elif kind == "tool_calls":
        print(f"\n🔧 {item.tool_name}({item.input})")
        for delta in item.output_deltas:
            print(delta, end="", flush=True)
        print(f"\n→ {item.output}")

final_state = stream.output   # 结束后的完整状态
```

**流式三态对比**：

| API | 通道 | 适用 |
|-----|------|------|
| `agent.stream()` | 全量状态块 | 服务端调试 |
| `agent.stream_events(version="v3")` | 分通道投影（messages/tool_calls/values） | 前端交互（推荐） |
| 旧 `stream_mode="messages"` | 单通道元组 | 0.x 兼容代码 |

---

## 8. 多 Agent 协作

> 🎯 **单 Agent 解决 80% 场景；多 Agent 在「职责分明 + 需要交接」时才值得**——每个 Agent 配独立工具集，用 handoffs 交接。

```python
from langchain.agents import create_agent

# 客服 = 多 Agent 分工（官方推荐模式）
billing_agent = create_agent(
    model="...", tools=[check_invoice, refund_policy],
    prompt="你是账单客服：查询账单/退款政策",
    middleware=[HumanInTheLoopMiddleware()],   # 退款前人工审批
)
support_agent = create_agent(
    model="...", tools=[troubleshoot_steps, check_warranty],
    prompt="你是技术支持：排查故障/查保修",
)

# 交接（handoffs）：support_agent 判断属于账单问题 → 交给 billing_agent
from langchain.agents.handoffs import handoff

@tool
def transfer_to_billing():
    """当用户询问账单、发票、退款时调用本工具进行交接"""
    return handoff(billing_agent)
```

**多 Agent 模式速查**：

| 模式 | 结构 | 适用 | 复杂度 |
|------|------|------|:---:|
| 单 Agent | 一个大脑 + N 工具 | 大多数场景 | ⭐ |
| 多 Agent 分工 | 各配工具集 + 交接 | 职责清晰的大型客服/运营 | ⭐⭐⭐ |
| Supervisor | 主管 Agent 调度下属 | 任务可拆分、需要编排 | ⭐⭐⭐⭐ |
| 层级协作 | 主管 → 组长 → 执行 | 复杂流水线 | ⭐⭐⭐⭐⭐ |

> ⚠️ **多 Agent 的代价**：token 消耗翻倍、错误传递放大、调试困难（每个 Agent 都要 trace）。**能用单 Agent 解决就不要拆**——先单后多，是官方反复强调的原则。

---

## 9. 生产化清单

```text
Agent 上线前 checklist（2026 生产标准）
├── □ 可观测：LangSmith trace 全量接入（每个 Agent 独立 run_name）
├── □ 重试：ToolRetryMiddleware + with_retry（只重试可重试异常）
├── □ 安全：PIIMiddleware 脱敏 + 工具权限校验（wrap_tool_call 鉴权）
├── □ 审批：高风险工具挂 HumanInTheLoopMiddleware
├── □ 限额：max_tokens / 工具调用次数上限（防失控循环）
├── □ 持久化：Sqlite/Postgres Saver + thread_id 管理
├── □ 评估：黄金集回归（每轮 prompt 变更跑一遍）
├── □ 成本：LangSmith token 聚合看板，按模型分层（贵模型给复杂工具）
└── □ 降级：主模型不可用 → with_fallbacks 切便宜模型（延迟可接受）
```

**常见故障与对策**：

| 故障现象 | 根因 | 对策 |
|---------|------|------|
| Agent 循环调同一工具 | 工具返回不可解析/未指明失败 | 返回错误说明字符串 + ToolRetry 上限 |
| 调用不存在工具（幻觉） | 模型乱编工具名 | 1.x JS 已支持动态恢复；Python 升级到最新 1.3.x |
| 上下文暴涨 | 长对话 + 大工具结果 | SummarizationMiddleware + 工具输出截断 |
| 回答不一致 | 提示词冲突/工具说明模糊 | LangSmith 对比 trace，迭代 docstring |
| 成本超预算 | 循环次数多/模型贵 | 控制 max_tool_calls + 分级模型 |

---

## 10. 核心要点

> 🎯 **核心要点**：
> 1. **create_agent 是 1.x 唯一 Agent 入口**——ReAct 循环自动实现，AgentExecutor 别再用
> 2. **工具 docstring = 模型说明书**——写好「何时调/参数格式/返回结构/边界」
> 3. **checkpointer 即记忆**：Sqlite/Postgres Saver + thread_id 多轮会话
> 4. **中间件插拔横切能力**：PII/Summary/HITL/ToolRetry 内置可用，钩子可自定义
> 5. **response_format** 主循环内结构化输出，省一次调用
> 6. **stream_events v3**（1.3+）分通道流式——前端交互首选
> 7. **多 Agent 慎重**：先单后多，职责清晰才拆
> 8. **生产 checklist**：可观测/重试/安全/限额/评估五件套

---

**下一模块**：[06-LangGraph编排](06-LangGraph编排.md) | **返回总览**：[00-LangChain知识体系总览](00-LangChain知识体系总览.md)
