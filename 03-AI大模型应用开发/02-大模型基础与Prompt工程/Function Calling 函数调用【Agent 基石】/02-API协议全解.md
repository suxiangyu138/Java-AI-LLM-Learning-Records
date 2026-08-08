# 02 API 协议全解
> 事实标准的完整形态：tools 声明、tool_calls 响应、参数全解、流式增量与三家协议对照

## 📚 目录
1. [请求侧：tools 声明的完整形态](#1-请求侧tools-声明的完整形态)
2. [响应侧：tool_calls 的完整形态](#2-响应侧tool_calls-的完整形态)
3. [执行结果回传：tool 消息](#3-执行结果回传tool-消息)
4. [参数全解：tool_choice 与 parallel_tool_calls](#4-参数全解tool_choice-与-parallel_tool_calls)
5. [流式：增量 chunk 的组装](#5-流式增量-chunk-的组装)
6. [Anthropic Tool Use 协议对照](#6-anthropic-tool-use-协议对照)
7. [Responses API 形态（OpenAI 新范式）](#7-responses-api-形态openai-新范式)
8. [核心要点](#8-核心要点)

---

## 1. 请求侧：tools 声明的完整形态

```python
import json
from openai import OpenAI

client = OpenAI()

tools = [
    {
        "type": "function",
        "function": {
            "name": "get_weather",
            "description": "获取指定城市的当前天气。当用户询问温度、天气状况时使用。",
            "parameters": {                      # JSON Schema
                "type": "object",
                "properties": {
                    "city": {
                        "type": "string",
                        "description": "城市名，如 北京、上海",
                    },
                    "unit": {
                        "type": "string",
                        "enum": ["celsius", "fahrenheit"],
                        "default": "celsius",
                    },
                },
                "required": ["city"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_stock_price",
            "description": "获取股票实时价格。当用户询问股价、涨跌时使用。",
            "parameters": {
                "type": "object",
                "properties": {
                    "symbol": {"type": "string", "description": "股票代码，如 AAPL"},
                },
                "required": ["symbol"],
            },
        },
    },
]

resp = client.chat.completions.create(
    model="gpt-5.2",
    messages=[{"role": "user", "content": "北京今天冷吗？顺便看看苹果股价"}],
    tools=tools,                       # 工具列表
    tool_choice="auto",                # 调用策略
    parallel_tool_calls=True,          # 允许并行（默认）
)
```

| 字段 | 说明 | 注意 |
|------|------|------|
| `type` | 固定 `"function"` | 未来可能有其他类型 |
| `name` | 函数名（模型按此调用） | 大小写/下划线敏感，别用空格 |
| `description` | 给模型看的说明 | **写"做什么 + 什么时候用"**（04 章） |
| `parameters` | JSON Schema | 越精确调用越准 |

> 💡 工具定义每个请求都要发送（OpenAI Chat Completions 无服务端注册），**每个工具定义 ≈ 100-200 输入 token/请求**——这是成本与缓存优化的核心对象（09 章）。

## 2. 响应侧：tool_calls 的完整形态

```python
# 模型可能返回两种响应之一：
# ① 不需要工具 → 正常 content 文本
# ② 需要工具 → content 可能为空（或说明文字），tool_calls 非空

message = resp.choices[0].message
if message.tool_calls:
    for tc in message.tool_calls:        # ⭐ 必须循环！可能有多个
        fn = tc.function
        print(tc.id)                     # 调用 ID（回传结果时用）
        print(fn.name)                   # 工具名
        print(fn.arguments)              # ⚠️ 参数是 JSON 字符串，需自己解析
else:
    print(message.content)
```

```json
// 响应中的 tool_calls（JSON 视角）
{
  "choices": [{
    "message": {
      "role": "assistant",
      "content": null,
      "tool_calls": [
        {
          "id": "call_abc123",
          "type": "function",
          "function": {
            "name": "get_weather",
            "arguments": "{\"city\": \"北京\", \"unit\": \"celsius\"}"   // ← 字符串！
          }
        },
        {
          "id": "call_def456",
          "type": "function",
          "function": {
            "name": "get_stock_price",
            "arguments": "{\"symbol\": \"AAPL\"}"
          }
        }
      ]
    },
    "finish_reason": "tool_calls"
  }]
}
```

> ⚠️ **两个经典坑**：(1) `arguments` 是 **JSON 字符串**不是对象——直接 `arguments["city"]` 会崩，必须 `json.loads`；(2) `tool_calls` 是**数组**——永远循环处理，只取 `[0]` 的代码在复合问题触发并行调用时直接崩溃（OpenAI 官方文档明确建议：假设一定会有多个调用）。

## 3. 执行结果回传：tool 消息

```python
# 把执行结果作为 role=tool 的消息追加，再次请求
results = []
for tc in message.tool_calls:
    fn = tc.function
    result = execute_tool(fn.name, json.loads(fn.arguments))   # 你的执行器
    results.append({
        "role": "tool",
        "tool_call_id": tc.id,          # ⭐ 用调用 ID 与 tool_calls 配对
        "content": json.dumps(result, ensure_ascii=False),     # 结果转 JSON 字符串
    })

# 第二轮请求：assistant 消息（含 tool_calls）+ tool 结果消息
resp2 = client.chat.completions.create(
    model="gpt-5.2",
    messages=[
        {"role": "user", "content": "北京今天冷吗？顺便看看苹果股价"},
        message,                      # ① 上轮 assistant 消息（必须原样带上 tool_calls）
        *results,                     # ② 工具结果
    ],
    tools=tools,
)
```

| 消息角色 | 职责 | 铁律 |
|---------|------|------|
| `user` | 用户输入 | 首轮必有 |
| `assistant` | 模型输出（含 tool_calls） | **必须原样回传**，带 tool_calls 的那条不能删 |
| `tool` | 工具执行结果 | 与 `tool_call_id` 配对，**一个调用一条结果** |
| `system` | 系统指令 | 可选，放最前 |

> 🎯 **核心要点**：循环的"记忆" = **消息历史**。每一轮都要把"assistant 的调用指令 + tool 的执行结果"追加进 messages 再请求——**任何一条不配对（多结果/漏结果/错 id）都会导致 400 或模型困惑**。

## 4. 参数全解：tool_choice 与 parallel_tool_calls

### 4.1 tool_choice 四种取值

| 值 | 行为 | 典型场景 |
|----|------|---------|
| `"auto"`（默认） | 模型自行决定调不调、调哪个 | 通用对话 |
| `"required"` | **至少调用一个工具** | 路由器/必须查询的场景 |
| `{"type": "function", "function": {"name": "X"}}` | **必须调用指定工具** | 提取/分类（结构化输出） |
| `"none"` | 禁用工具调用 | 纯聊天/最终总结阶段 |

```python
# 场景一：必须查实时行情（DeepSeek 官方建议：不要只靠 auto）
resp = client.chat.completions.create(..., tool_choice="required")

# 场景二：强制用指定工具做提取
resp = client.chat.completions.create(
    ...,
    tool_choice={"type": "function", "function": {"name": "extract_entity"}},
)

# 场景三：工具结果回填后，最终回答关掉工具
resp = client.chat.completions.create(..., tool_choice="none")
```

> 💡 **选型原则（OpenAI 官方）**：选任务允许的**最紧**设置——留的自由度越多，模型越可能误用。提取类任务永远强制指定工具，查询类任务用 required，纯聊天才用 auto。

### 4.2 parallel_tool_calls

| 值 | 行为 | 适用 |
|----|------|------|
| `True`（默认） | 一次响应返回多个 tool_calls | 独立查询（天气+股价） |
| `False` | 一次只调一个 | ⚠️ 有依赖的步骤、strict 严格模式、危险操作 |

**必须关掉并行的三个场景**：

1. **有顺序依赖**：工具 B 需要工具 A 的结果（先查 customer_id 再查订单）；
2. **strict 严格模式**：OpenAI 官方明确——**严格模式的结构保证在并行调用下不成立**；
3. **副作用/危险操作**：冻结卡片、取消订阅、退款——绝不能三连发，要"确认-执行"模式（05 章）。

> ⚠️ 注意：并非所有模型接受 `parallel_tool_calls` 参数（部分老模型会报错）；推理模型（o 系列）对工具调用的处理与其内部推理循环耦合，先查模型文档。

## 5. 流式：增量 chunk 的组装

```python
# 流式下 tool_calls 是"增量拼接"的：name 一次给全，arguments 按 chunk 拼
stream = client.chat.completions.create(
    model="gpt-5.2",
    messages=messages,
    tools=tools,
    stream=True,
)

tool_calls = {}          # index -> {"id":..., "name":..., "arguments": ""}
for chunk in stream:
    delta = chunk.choices[0].delta
    if delta.tool_calls:
        for tc in delta.tool_calls:
            idx = tc.index                      # 多个并行调用用 index 区分
            if idx not in tool_calls:
                tool_calls[idx] = {"id": tc.id, "name": tc.function.name or "", "arguments": ""}
            tool_calls[idx]["arguments"] += (tc.function.arguments or "")   # 增量拼接
```

| 流式要点 | 说明 |
|---------|------|
| `tc.index` | 并行调用时每个 chunk 标注属于哪个调用 |
| `arguments` 增量 | 按 chunk 追加拼接，结束时才是完整 JSON |
| `name` 只出现在首个 chunk | 不能假设每 chunk 都有 |
| 边流边执行 | 可先等 arguments 拼完再执行（07 章流式专题） |

> ⚠️ 流式 + tool_calls 的坑：**有的实现（如某些推理模型）先出 reasoning 再出 tool_calls**；`finish_reason="tool_calls"` 是"该结束拼装了"的信号。流式组装务必以 index 维度聚合（07 章完整代码）。

## 6. Anthropic Tool Use 协议对照

```python
# Anthropic 格式：tools 平级字段 + 响应为块结构（block）
import anthropic

client = anthropic.Anthropic()

resp = client.messages.create(
    model="claude-sonnet-5",
    max_tokens=1024,
    system="你是客服助手",
    messages=[{"role": "user", "content": "北京天气？"}],
    tools=[
        {
            "name": "get_weather",
            "description": "获取城市天气",
            "input_schema": {                     # ⚠️ 字段名是 input_schema 不是 parameters
                "type": "object",
                "properties": {
                    "city": {"type": "string", "description": "城市名"},
                },
                "required": ["city"],
            },
            "cache_control": {"type": "ephemeral"},   # ⭐ 工具定义缓存断点（09 章）
        }
    ],
)

# 响应：content 块数组，其中 type="tool_use" 的块是调用
for block in resp.content:
    if block.type == "tool_use":
        print(block.id, block.name, block.input)    # ⚠️ input 已是 dict，不需要 json.loads
        # 执行...
        tool_result = {
            "role": "user",
            "content": [
                {
                    "type": "tool_result",
                    "tool_use_id": block.id,        # 配对 id
                    "content": json.dumps(result),
                }
            ],
        }
```

| 维度 | OpenAI | Anthropic |
|------|--------|-----------|
| Schema 字段名 | `parameters` | `input_schema` |
| 调用块 | `message.tool_calls[]` | `content[].type == "tool_use"` |
| 参数形态 | JSON **字符串**（要 loads） | 已经是 **dict** |
| 结果回传 | `role: "tool"` 消息 | `role: "user"` + `tool_result` 块 |
| 配对键 | `tool_call_id` | `tool_use_id` |
| 严格模式 | 有（strict） | 无（靠提示 + 校验） |
| 工具缓存 | 自动（prompt caching 自动生效） | 显式 `cache_control` 断点 |

> 🎯 **核心要点**：两家协议**概念一一对应，命名各不相同**。统一封装层（LLM-API/09 已有）是跨平台标准做法——把"解析调用/执行/回传"抽象成接口，平台差异收敛在适配器里。

## 7. Responses API 形态（OpenAI 新范式）

2025 年起 OpenAI 主推 Responses API（Chat Completions 保持兼容但不再加新特性）：

```python
from openai import OpenAI
client = OpenAI()

resp = client.responses.create(
    model="gpt-5.2",
    input="北京天气？",
    tools=[{"type": "function", "name": "get_weather", "description": "...", "parameters": {...}}],
)

# 调用在顶层（不再嵌在 message 里）
for item in resp.output:
    if item.type == "function_call":        # 顶层类型判断
        call_id = item.call_id              # 配对键
        name = item.name
        args = json.loads(item.arguments)

# 结果回传：call_id 键控的 function_call_output
resp2 = client.responses.create(
    model="gpt-5.2",
    input=[
        *resp.output,                        # 原样带上上轮输出
        {"type": "function_call_output", "call_id": call_id, "output": json.dumps(result)},
    ],
    tools=tools,
)
```

| 维度 | Chat Completions | Responses |
|------|-----------------|-----------|
| 调用位置 | `message.tool_calls[]` | 顶层 `output[]` 中 `function_call` |
| 配对键 | `tool_call_id` | `call_id` |
| 结果消息 | `role: "tool"` | `function_call_output` 块 |
| 内置工具 | 无 | 原生（web_search、code_interpreter 等） |
| 状态 | 兼容维护 | ⭐ 新功能主阵地 |

> 💡 2026 新项目建议：OpenAI 生态直接用 **Responses API**；DeepSeek/Qwen/国产模型用 **Chat Completions**（它们兼容的是 Chat 格式）；两者并存时封装层按供应商分发。

## 8. 核心要点

| 序号 | 要点 |
|:---:|------|
| 1 | 请求侧：tools = type/name/description/parameters(JSON Schema) |
| 2 | 响应侧：tool_calls 是数组、arguments 是 JSON 字符串——**循环处理 + json.loads** |
| 3 | 回传：assistant 消息原样带 + role=tool 按 tool_call_id 配对 |
| 4 | tool_choice：auto/required/强制指定/none——选最紧的 |
| 5 | 并行调用默认开；有依赖/strict/危险操作时关掉 |
| 6 | 流式：按 index 增量拼 arguments，finish_reason=tool_calls 收尾 |
| 7 | Anthropic：input_schema/tool_use 块/input 已是 dict/tool_use_id——封装层收敛差异 |
| 8 | Responses API：顶层 function_call + call_id，OpenAI 新功能主阵地 |

---

**下一模块**：[03-调用循环工程](03-调用循环工程.md) / **返回总览**：[00-FunctionCalling知识体系总览](00-FunctionCalling知识体系总览.md)

## 参考来源

- [OpenAI 官方：Function Calling Guide](https://platform.openai.com/docs/guides/function-calling)
- [OpenAI 官方：Parallel Tool Calls 指南](https://platform.openai.com/docs/guides/function-calling#parallel-function-calling)
- [OpenAI 官方：Responses API](https://platform.openai.com/docs/api-reference/responses)
- [Anthropic 官方：Tool Use 文档](https://docs.anthropic.com/en/docs/build-with-claude/tool-use)
- [Stackademic：OpenAI Function Calling 全指南（tool_calls 循环处理）](https://blog.stackademic.com/openai-function-calling-full-guide-75d9e14db3de)
