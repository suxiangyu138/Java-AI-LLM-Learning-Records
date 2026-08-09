# 02 核心概念：Agent、Runner 与工具

> SDK 的运行三件套：Agent 定义"做什么"（指令+工具+护栏）、Runner 决定"怎么跑"（循环+流式+追踪）、工具提供"能做什么"（函数/MCP/子 Agent）。

## 📚 目录

1. [核心组件总览](#1-核心组件总览)
2. [Agent：指令 + 工具 + 护栏](#2-agent指令--工具--护栏)
3. [Runner：托管循环](#3-runner托管循环)
4. [工具：三类形态](#4-工具三类形态)
5. [运行结果对象](#5-运行结果对象)
6. [循环机制详解](#6-循环机制详解)
7. [面试高频问法](#7-面试高频问法)
8. [上下文变量与动态指令](#8-上下文变量与动态指令)
9. [常见误区](#9-常见误区)

## 1. 核心组件总览

| 组件 | 职责 | 类比 |
|---|---|---|
| Agent | 定义角色（指令/工具/护栏） | 员工手册 |
| Runner | 执行循环（调用模型/工具/流式） | 主管（托管流程） |
| 工具 | 外部能力（函数/MCP/子 Agent） | 工具箱 |
| Sessions | 对话历史持久化 | 档案 |
| Guardrails | 输入输出校验 | 门卫 |

```
运行流程：Runner 循环：
模型调用 → 工具调用？→ 执行工具 → 回传 → 再调用
→ 无工具调用/交接 → 产出最终输出
```

## 2. Agent：指令 + 工具 + 护栏

### Agent 定义

```python
from agents import Agent

agent = Agent(
    name="SalesAgent",
    instructions="你是销售助手：查询库存、回答政策问题。",   # 系统指令
    tools=[query_inventory, check_price],                    # 工具
    handoffs=[policy_agent],                                 # 交接（04 篇）
    input_guardrails=[...],                                  # 输入护栏（05 篇）
    output_guardrails=[...],                                 # 输出护栏
    model="gpt-4o-mini",                                     # 模型
)
```

### Agent 的可配置项

| 配置 | 说明 |
|---|---|
| instructions | 系统指令（角色定义） |
| tools | 可用工具列表 |
| handoffs | 可交接的 Agent 列表 |
| guardrails | 输入/输出护栏 |
| model | 模型（可换/可 LiteLLM） |
| output_type | 结构化输出（Pydantic） |
| context | 上下文变量（local 应用状态） |

### 设计原则（回顾 Function Calling 体系）

```
指令写清"何时用什么工具"
工具 description 必写（模型决策依据）
输出类型用 Pydantic（结构化）
```

## 3. Runner：托管循环

### 运行方式

```python
from agents import Runner

# 同步运行
result = Runner.run_sync(agent, "查询 A001 库存")

# 流式运行（一等公民）
result = Runner.run_streamed(agent, "查询 A001 库存")
async for event in result.stream_events():
    if event.type == "raw_response_event":
        print(event.data.delta, end="")   # 流式输出
```

### Runner 的托管职责

| 职责 | 说明 |
|---|---|
| 循环控制 | 模型→工具→模型（直到结束） |
| 工具执行 | 自动调用与结果回传 |
| 流式 | run_streamed 原生 |
| 追踪 | 每次运行自动生成 trace |
| 会话 | 传入 session 恢复历史 |
| 轮次限制 | max_turns 兜底（防死循环） |

### max_turns（生产必设）

```python
result = Runner.run_sync(
    agent, query,
    max_turns=10,        # 防死循环（生产纪律）
)
```

> ⚠️ 托管循环不设上限 = 无底洞——max_turns 是 SDK 的第一生产纪律。

## 4. 工具：三类形态

| 形态 | 用法 | 适用 |
|---|---|---|
| @tool 函数 | 装饰器包装 Python 函数 | 常规工具 |
| MCP 工具 | 消费 MCP 服务器 | 生态复用 |
| Agents as Tools | 子 Agent 作工具 | 嵌套任务 |

### @tool 示例

```python
from agents import function_tool

@function_tool
def query_inventory(product_id: str) -> str:
    """查询商品库存"""
    inventory = {"A001": 100}
    return f"库存：{inventory.get(product_id, 0)} 件"
```

### 工具选择（与 Function Calling 一致）

```
一工具一职责 / description 三件事（做什么/何时用/边界）
参数少而精 / 错误转消息 / 幂等
```

## 5. 运行结果对象

### RunResult 结构

```python
result = Runner.run_sync(agent, query)

result.final_output      # 最终输出（文本或结构化）
result.final_agent       # 产出结果的 Agent
result.items             # 完整运行项（消息/工具调用）
result.new_items         # 本轮新增项
result.last_response     # 最后模型响应
result.raw_responses     # 所有原始响应
result.last_response_id  # 用于会话续接
```

### 关键字段用途

| 字段 | 用途 |
|---|---|
| final_output | 答案/结果 |
| items/new_items | 追踪与调试 |
| last_response_id | 会话续接（传 session） |

## 6. 循环机制详解

### 完整循环

```
① Runner 调用模型（携带消息历史）
② 模型返回：文本 / 工具调用 / 交接请求
③ 工具调用 → Runner 执行 → 结果作为消息回传 → 回到①
④ 交接 → 切换 Agent → 回到①
⑤ 无工具调用且无交接 → 产出 final_output → 结束
```

### 循环的终止

| 终止方式 | 说明 |
|---|---|
| 自然结束 | 模型不再调用工具/交接 |
| max_turns | 轮次上限（兜底） |
| Tripwire | 护栏触发（立即中止） |

### 与手写循环的关系

```
手写（Function Calling 体系五步循环）→ SDK 托管
原理完全一致：模型决策 + 框架执行 + 结果回传
SDK 把循环、流式、追踪全部内建
```

## 7. 面试高频问法

| 问题 | 回答要点 |
|---|---|
| Agent 定义什么？ | 指令/工具/护栏/交接/模型 |
| Runner 托管什么？ | 循环/工具执行/流式/追踪 |
| max_turns 为什么必设？ | 防死循环（托管循环的兜底） |
| 工具三种形态？ | @tool/MCP/Agents as Tools |
| RunResult 关键字段？ | final_output/items/last_response_id |
| 循环怎么终止？ | 自然结束/max_turns/tripwire |

### 面试加分表达

> "SDK 的核心是托管循环：Runner 替你做模型→工具→模型的迭代，工具执行、流式、追踪都内建。我理解它的循环机制就是 Function Calling 五步循环的托管版——原理一样，样板少了。生产上 max_turns 必须设，不然托管循环就是无底洞。"

## 8. 上下文变量与动态指令

### context 机制

```python
agent = Agent(
    name="SupportAgent",
    instructions="你是客服助手，当前用户等级：{{user_tier}}",   # 模板变量
    context={"user_tier": "VIP"},    # 应用级上下文（每次运行传入）
)

result = Runner.run_sync(
    agent, query,
    context={"user_tier": "VIP"},    # 运行时覆盖
)
```

### context 的用途

| 用途 | 例子 |
|---|---|
| 用户信息 | 等级/地区/语言 |
| 会话参数 | 权限/预算 |
| 动态配置 | 环境/开关 |

### 与 Session 的区别（2026 澄清）

```
context = 本地应用上下文（每次运行传入，不持久）
session = 对话状态（跨运行持久化）
（详见 06 篇——混用会导致应用数据泄漏）
```

## 9. 常见误区

| 误区 | 真相 |
|---|---|
| "Agent = LLM" | Agent = 指令+工具+护栏+交接的封装 |
| "Runner 是配置" | 是执行器（托管循环/流式/追踪） |
| "工具只能函数" | @tool/MCP/子 Agent 三形态 |
| "context 会持久化" | 每次运行传入（不持久） |
| "max_turns 可省" | 生产必设（第一纪律） |

> 🎯 核心要点：Agent 定义（指令/工具/护栏/交接）+ Runner 托管（循环/工具/流式/追踪）+ 工具三形态（@tool/MCP/子 Agent）；循环机制 = Function Calling 的托管版；终止三方式（自然/max_turns/tripwire）；max_turns 是第一生产纪律；context（应用级，不持久）vs session（对话级，持久化）要分清；RunResult 的 last_response_id 是会话续接关键。

---

**下一模块**：[03-快速上手-第一个Agent](03-快速上手-第一个Agent.md) / **返回总览**：[00-OpenAI-Agents-SDK知识体系总览](00-OpenAI-Agents-SDK知识体系总览.md)
