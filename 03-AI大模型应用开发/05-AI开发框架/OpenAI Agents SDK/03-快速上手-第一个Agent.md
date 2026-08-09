# 03 快速上手：第一个 Agent

> 从零跑通 OpenAI Agents SDK：安装（openai-agents）、最小 Agent + 工具 + Runner 完整代码、运行验证与排错——"少写样板"的直观体验。

## 📚 目录

1. [安装与环境](#1-安装与环境)
2. [最小 Agent 应用](#2-最小-agent-应用)
3. [代码全解](#3-代码全解)
4. [流式与结构化输出](#4-流式与结构化输出)
5. [运行验证](#5-运行验证)
6. [常见报错排查](#6-常见报错排查)
7. [面试高频问法](#7-面试高频问法)
8. [扩展练习](#8-扩展练习)
9. [与手写循环的对照](#9-与手写循环的对照)

## 1. 安装与环境

### 安装命令

```bash
pip install openai-agents

# 追踪需要（可选，默认发 OpenAI 仪表盘）
# export OPENAI_API_KEY=sk-xxx
```

### 环境要求

| 项 | 说明 |
|---|---|
| Python | 3.9+ |
| API Key | OPENAI_API_KEY（或 LiteLLM 换其他模型） |
| 底层 | Responses API（SDK 内置） |

> 💡 Provider 无关：通过 LiteLLM 可接 100+ 模型（不锁定 OpenAI）。

## 2. 最小 Agent 应用

```python
"""OpenAI Agents SDK 最小 Agent：工具调用"""
import asyncio
from agents import Agent, Runner, function_tool

# 1. 工具
@function_tool
def query_inventory(product_id: str) -> str:
    """查询商品库存（模拟）"""
    inventory = {"A001": 100, "A002": 50}
    return f"商品 {product_id} 库存：{inventory.get(product_id, 0)} 件"

# 2. Agent（指令 + 工具）
agent = Agent(
    name="SalesAgent",
    instructions="你是销售助手：查询库存时调用 query_inventory。",
    tools=[query_inventory],
)

# 3. Runner 运行（托管循环）
async def main():
    result = await Runner.run(agent, "A001 有货吗？")
    print(result.final_output)

if __name__ == "__main__":
    asyncio.run(main())
```

**核心 3 步：定义工具 → 定义 Agent → Runner 跑**——托管式流程的直观体验。

## 3. 代码全解

| 段 | 做了什么 | 关键点 |
|---|---|---|
| @function_tool | 函数包装为工具 | description 从 docstring 自动提取 |
| Agent(...) | 定义角色 | instructions = 系统指令 |
| Runner.run | 托管循环 | 自动：模型→工具→结果→再调用 |
| final_output | 取最终输出 | 不含中间过程 |

### 运行时的幕后

```
① Runner 调模型（Responses API）
② 模型返回工具调用（query_inventory + A001）
③ Runner 执行工具 → 结果回传
④ 模型基于结果回答 → final_output
（托管循环 = Function Calling 五步的自动化）
```

## 4. 流式与结构化输出

### 流式（一等公民）

```python
async def stream_demo():
    result = Runner.run_streamed(agent, "A002 有货吗？")
    async for event in result.stream_events():
        if event.type == "raw_response_event":
            print(event.data.delta, end="", flush=True)
    print()
```

### 结构化输出（Pydantic）

```python
from pydantic import BaseModel
from agents import Agent, Runner

class InventoryResult(BaseModel):
    product_id: str
    stock: int
    available: bool

agent = Agent(
    name="InventoryAgent",
    instructions="查询库存并输出结构化结果。",
    tools=[query_inventory],
    output_type=InventoryResult,       # 强制结构化
)

result = Runner.run_sync(agent, "A001")
print(result.final_output.model_dump())
# {"product_id": "A001", "stock": 100, "available": True}
```

## 5. 运行验证

### 验证清单

| 项 | 检查 |
|---|---|
| 工具触发 | 模型调用了 query_inventory（非直接编造） |
| 结果正确 | 库存数字与工具返回一致 |
| 循环终止 | 有 final_output（未死循环） |
| 流式 | 逐字输出正常 |
| 结构化 | output_type 生效（Pydantic 校验） |

### 调试打印

```python
result = Runner.run_sync(agent, "A001 有货吗？")
# 看完整运行项（工具调用是否发生）
for item in result.new_items:
    print(type(item).__name__)
```

## 6. 常见报错排查

| 报错 | 原因 | 修复 |
|---|---|---|
| Missing API key | OPENAI_API_KEY 未设 | 环境变量 |
| AuthenticationError | Key 无效 | 检查 Key |
| 工具未触发 | description 不清 | 工具 docstring 写"何时用" |
| 无限循环 | 无 max_turns | Runner.run(max_turns=10) |
| 结构化输出失败 | 模型未遵循 | 换强模型或简化 schema |
| 流式无输出 | 事件类型判断错 | 检查 raw_response_event |

### 排错三问

```
① Key 配了吗？（API 连通性）
② 工具 description 清楚吗？（模型知不知道何时用）
③ max_turns 设了吗？（防死循环）
```

## 7. 面试高频问法

| 问题 | 回答要点 |
|---|---|
| 最小 Agent 几步？ | 3 步：工具/Agent/Runner |
| 托管循环怎么工作的？ | 模型→工具→结果→再调用（自动） |
| 流式怎么做？ | run_streamed（一等公民） |
| 结构化输出？ | output_type（Pydantic） |
| 怎么接非 OpenAI 模型？ | LiteLLM（Provider 无关） |
| 排错顺序？ | Key→工具描述→max_turns |

### 面试加分表达

> "上手 SDK 的体验是'少写样板'：定义工具、定义 Agent、Runner 跑——托管循环自动完成模型→工具→结果的迭代。流式和 Pydantic 结构化输出都是一等公民。生产纪律是 max_turns 必设，不然循环就是无底洞。"

## 8. 扩展练习

| 练习 | 内容 | 掌握 |
|---|---|---|
| 1 | 加第二个工具（check_price） | 多工具决策 |
| 2 | 加 Handoffs（04 篇） | 多 Agent 交接 |
| 3 | 加输入护栏（05 篇） | 安全防线 |
| 4 | 用 Session 做多轮（06 篇） | 状态管理 |
| 5 | 接 LiteLLM 换 DeepSeek | 多 Provider |
| 6 | 接 LlamaIndex 检索工具 | RAG Agent |

## 9. 与手写循环的对照

| 手写环节（Function Calling 体系） | SDK |
|---|---|
| 工具定义（JSON Schema） | @function_tool（自动生成） |
| 模型调用（messages 循环） | Runner.run（托管） |
| 工具执行（执行+回传） | Runner 自动 |
| 终止判断（无调用/上限） | 自然结束/max_turns |
| 流式（SSE 拼接） | run_streamed（一等公民） |
| 错误处理 | Guardrails/Tripwire |

> 对照结论：**手写五步循环的每一环都有 SDK 对应物**——原理不变，托管化；先手写（FC 体系）再 SDK，理解不丢。

> 🎯 核心要点：最小 Agent 三步（工具/Agent/Runner）；托管循环 = Function Calling 的自动化；流式（run_streamed）与结构化（output_type）一等公民；LiteLLM 支持多 Provider；排错三问（Key/工具描述/max_turns）；六练习 + 手写对照表（原理不变，托管化）。

---

**下一模块**：[04-Handoffs-多Agent协作](04-Handoffs-多Agent协作.md) / **返回总览**：[00-OpenAI-Agents-SDK知识体系总览](00-OpenAI-Agents-SDK知识体系总览.md)
