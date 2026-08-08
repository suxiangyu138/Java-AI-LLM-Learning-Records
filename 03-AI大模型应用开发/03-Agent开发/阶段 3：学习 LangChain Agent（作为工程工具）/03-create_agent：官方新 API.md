# 03 create_agent：官方新 API

> 定位：2026 年 LangChain 构建 Agent 的唯一官方入口——全参数解析、弃用史、与 AgentExecutor 的迁移对照（2026-08 基准）

## 📚 目录

1. [为什么是 create_agent](#1-为什么是-create_agent)
2. [最小示例：三行起一个 Agent](#2-最小示例三行起一个-agent)
3. [全参数解析](#3-全参数解析)
4. [弃用史：AgentExecutor → create_react_agent → create_agent](#4-弃用史agentexecutor--create_react_agent--create_agent)
5. [中间件系统](#5-中间件系统)
6. [何时下沉 StateGraph](#6-何时下沉-stategraph)
7. [常见坑速查](#7-常见坑速查)

## 1. 为什么是 create_agent

2026 的官方定位：

```text
create_agent（langchain.agents）     = 官方推荐入口，内部生成 LangGraph 状态机
create_react_agent（langgraph.prebuilt） = 已弃用（LangGraph v1），迁移到 create_agent
AgentExecutor（langchain.agents）    = 维护期至 2026-12，官方禁止新代码使用
StateGraph（langgraph）              = 底层引擎，需要节点级控制时手动使用
```

| 特性 | create_agent |
|------|-------------|
| 一行启动 | `create_agent(model, tools, system_prompt)` |
| 内置能力 | 流式、checkpointer、可观测、中间件 |
| 底层 | 自动构建 LangGraph 状态机（04 篇解剖） |
| 状态 | 标准 ReAct 循环（agent 节点 + tools 节点 + 条件路由） |

> 🎯 **核心要点**：create_agent = "你要的循环能力，我打包成参数给你"——你阶段 1 手写的 60 行循环、阶段 2 手写的流式/记忆，在这里都是参数。

## 2. 最小示例：三行起一个 Agent

```python
from langchain.agents import create_agent
from langchain_openai import ChatOpenAI
from langchain_core.tools import tool

@tool
def get_weather(city: str) -> str:
    """查询指定城市的当前天气。当用户询问天气、气温、降雨时使用。"""
    return f"{city} 当前 22℃，晴天"

model = ChatOpenAI(
    model="deepseek-v4-flash",               # OpenAI 兼容端点通吃
    base_url="https://api.deepseek.com",     # DeepSeek 只需改 base_url
    api_key="sk-...",                        # 生产用环境变量
    temperature=0.1,                          # 工具场景低温度（阶段 2 教训）
)

agent = create_agent(
    model=model,
    tools=[get_weather],
    system_prompt="你是一个天气助手，用中文回答，简洁明了。",
)

result = agent.invoke({"messages": [("user", "北京今天天气如何？")]})
print(result["messages"][-1].content)
```

**invoke 输入输出**：

```text
输入：{"messages": [("user", "问题")]}（框架内部转成标准消息）
输出：{"messages": [ ...完整消息历史... ]}（最后一条 = 最终回答）
```

> 💡 **与手写对照**：输入/输出都是 `messages` 数组——协议没变，只是框架替你维护了追加铁律。

## 3. 全参数解析

| 参数 | 作用 | 对应手写/原生 |
|------|------|--------------|
| `model` | 任意支持工具调用的 ChatModel | client |
| `tools` | @tool 列表 | 工具注册表 |
| `system_prompt` | 系统提示词（str 或 PromptTemplate） | system 消息 |
| `checkpointer` | 跨轮记忆（05 篇） | 手写消息历史持久化 |
| `store` | 跨会话存储 | 外部记忆 |
| `response_format` | 最终结构化输出（Pydantic schema） | response_format |
| `version="v2"` | 并行工具执行（Send API） | asyncio.gather（阶段 2） |
| `recursion_limit` | 循环步数上限 | MAX_TURNS |
| `pre_model_hook` / `post_model_hook` | 模型调用前后钩子 | 日志/拦截 |
| 中间件 | 请求级横切（见第 5 节） | 装饰器/包装函数 |

```python
agent = create_agent(
    model=model,
    tools=tools,
    system_prompt="...",
    checkpointer=checkpointer,          # 记忆
    response_format=SummarySchema,      # 结构化输出
    version="v2",                       # 并行工具
    recursion_limit=25,                 # 防死循环（默认可能更高）
)
```

> ⚠️ **recursion_limit 别省**：默认值可能偏大——生产环境显式设置（10-25），否则工具反复失败时循环烧钱（阶段 1 教训的框架版）。

## 4. 弃用史：AgentExecutor → create_react_agent → create_agent

| 时代 | API | 2026 状态 | 为什么换代 |
|------|-----|----------|-----------|
| 2023-2024 | AgentExecutor + tools | **维护期至 2026-12** | 循环逻辑硬编码、难扩展 |
| 2024-2025 | create_react_agent（LangGraph） | **已弃用** | 被 langchain.create_agent 统一 |
| 2025.10+ | **create_agent** | ✅ 官方入口 | 内置状态机 + 中间件 + 全参数 |

**AgentExecutor → create_agent 迁移对照**：

```text
❌ 老代码（维护期）：
from langchain.agents import AgentExecutor, create_react_agent
executor = AgentExecutor(agent=create_react_agent(llm, tools, prompt), tools=tools)

✅ 新代码（2026）：
from langchain.agents import create_agent
agent = create_agent(model=llm, tools=tools, system_prompt=prompt_text)
```

| 迁移注意 | 说明 |
|---------|------|
| 记忆参数 | AgentExecutor 的 memory → create_agent 的 checkpointer |
| max_iterations | → recursion_limit |
| 中间步骤 | → stream(stream_mode="values") 或钩子 |
| 兼容提示 | 老教程代码直接改 import 大多可跑，但行为检查一遍 |

> ⚠️ **2026 判断框架资历的试金石**：面试/文档看到 `AgentExecutor` 或 `create_react_agent` 作为推荐 API → 该资料已过时。

## 5. 中间件系统

LangGraph v1 引入的**灵活中间件**（create_agent 的一大卖点）：

```python
from langgraph.agents.middleware import Middleware

class LogMiddleware(Middleware):
    """示例：记录每次模型调用"""
    async def on_call(self, input):
        print(f"[模型调用] {input}")
        return input

agent = create_agent(model, tools, middleware=[LogMiddleware()])
```

| 中间件用途 | 示例 |
|-----------|------|
| 日志/追踪 | 记录输入输出 |
| 注入 | 向状态注入上下文 |
| 拦截 | 请求级过滤/改写 |
| 限流 | 速率控制 |

> 💡 中间件 = 你手写时"包一层装饰器"的框架化——横切关注点不再散落各处。

## 6. 何时下沉 StateGraph

```text
create_agent 够用吗？（默认答案：够）
├─ 需要节点级状态检查 / 中途打断注入（human-in-the-loop）
├─ 需要条件重试逻辑 / 多 Agent 交接
└─ 需要自定义图拓扑（非标准 ReAct）
       ↓ 满足任一 → 下沉 StateGraph（04 篇）
```

| 场景 | create_agent | StateGraph |
|------|:---:|:---:|
| 标准工具循环 | ✅ | 杀鸡用牛刀 |
| 人类审批节点 | ❌ | ✅ |
| 多 Agent Supervisor | ❌ | ✅ |
| 状态中途检查 | ❌ | ✅ |

## 7. 常见坑速查

| 坑 | 现象 | 修复 |
|----|------|------|
| 导错包 | `create_agent` 找不到 | `from langchain.agents import create_agent` |
| 老教程照抄 | AgentExecutor 行为不符 | 迁移对照表（第 4 节） |
| 不设 recursion_limit | 死循环烧钱 | 显式设置 10-25 |
| tools 传了 dict 而非 @tool | 报类型错 | 统一用 @tool 装饰器 |
| 输入格式错 | invoke 报错 | `{"messages": [("user", ...)]}` |
| 忘装 langgraph | create_agent 底层缺依赖 | `pip install langgraph` |

> 🎯 **核心要点**：create_agent 的正确心智 = **"参数化的生产级循环"**。用熟它之后，阶段 1 手写的循环代码就变成了你的"内部解剖图"——你知道每个参数背后在跑什么。

---

**返回总览**：[00-阶段总览：LangChain Agent 学习](00-阶段总览：LangChain%20Agent%20学习.md) / **上一模块**：[02-@tool 工具定义](02-%40tool%20工具定义.md) / **下一模块**：[04-底层原理：LangGraph 状态机](04-底层原理：LangGraph%20状态机.md)
