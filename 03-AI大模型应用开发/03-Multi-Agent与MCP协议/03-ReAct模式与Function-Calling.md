# 03 - ReAct 模式与 Function Calling

> 🎯 ReAct = Reasoning + Acting，是 Agent 的核心推理范式。Function Calling 是 ReAct 的工业化实现，让 LLM 能可靠地调用外部工具

---

## 目录

1. [ReAct 原理](#1-react-原理)
2. [Function Calling 实战](#2-function-calling-实战)
3. [工具调用解析与执行](#3-工具调用解析与执行)
4. [OpenAI vs Anthropic 工具调用对比](#4-openai-vs-anthropic-工具调用对比)

---

## 1. ReAct 原理

```text
ReAct = Reasoning + Acting = 思考与行动交替

  传统 CoT：纯思考 → 只在大脑里推理，不接触外部
  ReAct：思考 → 行动（获取外部信息）→ 观察 → 再思考

  类比：
    CoT = 闭卷考试（只能靠记忆推理）
    ReAct = 开卷考试（可以查资料、按计算器、问专家）
```

### ReAct 循环

```text
Question: 北京和上海的天气哪个更适合户外运动？

Thought: 我需要查两个城市的天气
Action: get_weather[北京]
Observation: 北京 22°C，晴天，微风

Thought: 还需要上海的天气
Action: get_weather[上海]
Observation: 上海 28°C，阴天，湿度 85%

Thought: 北京晴天22°C更适合户外运动
Final Answer: 北京天气更适合户外运动（晴天22°C vs 上海阴天28°C高湿度）
```

---

## 2. Function Calling 实战

### 2.1 OpenAI Function Calling

```python
from openai import OpenAI
import json

client = OpenAI()

# ① 定义工具
tools = [{
    "type": "function",
    "function": {
        "name": "get_weather",
        "description": "获取指定城市的天气信息",
        "parameters": {
            "type": "object",
            "properties": {
                "city": {
                    "type": "string",
                    "description": "城市名称，如'北京'、'上海'"
                }
            },
            "required": ["city"]
        }
    }
}]

# ② 第一轮：LLM 决定调用工具
response = client.chat.completions.create(
    model="gpt-4o",
    messages=[{"role": "user", "content": "北京天气怎么样？"}],
    tools=tools,
    tool_choice="auto"  # LLM 自主决定是否调用
)

# ③ 解析工具调用
tool_call = response.choices[0].message.tool_calls[0]
func_name = tool_call.function.name  # "get_weather"
func_args = json.loads(tool_call.function.arguments)  # {"city": "北京"}

# ④ 执行工具
def get_weather(city):
    # 实际调用天气 API
    return f"{city}：22°C，晴天"

result = get_weather(**func_args)

# ⑤ 第二轮：将工具结果送回 LLM 生成最终回答
messages = [
    {"role": "user", "content": "北京天气怎么样？"},
    response.choices[0].message,  # 包含 tool_calls 的 assistant 消息
    {"role": "tool", "tool_call_id": tool_call.id, "content": result}
]

final_response = client.chat.completions.create(
    model="gpt-4o", messages=messages
)
print(final_response.choices[0].message.content)
```

### 2.2 并行工具调用

```python
# LLM 同时调用多个工具
response = client.chat.completions.create(
    model="gpt-4o",
    messages=[{"role": "user", "content": "北京和上海今天天气对比"}],
    tools=tools
)

# 可能返回多个 tool_calls
for tool_call in response.choices[0].message.tool_calls:
    print(f"调用: {tool_call.function.name}({tool_call.function.arguments})")
# 调用: get_weather({"city": "北京"})
# 调用: get_weather({"city": "上海"})
```

---

## 3. 工具调用解析与执行

```python
class ToolExecutor:
    def __init__(self):
        self.tools = {}  # name → (func, schema)
    
    def register(self, func, schema):
        self.tools[func.__name__] = (func, schema)
    
    def get_schemas(self):
        return [s for _, s in self.tools.values()]
    
    def execute(self, name, args):
        if name not in self.tools:
            return json.dumps({"error": f"未知工具: {name}"})
        try:
            func, _ = self.tools[name]
            result = func(**args)
            return json.dumps({"success": True, "data": result})
        except Exception as e:
            return json.dumps({"success": False, "error": str(e)})

# 注册工具
executor = ToolExecutor()
executor.register(get_weather, {
    "type": "function",
    "function": {"name": "get_weather", ...}
})
```

---

## 4. OpenAI vs Anthropic 工具调用对比

| 维度 | OpenAI | Anthropic (Claude) |
|------|--------|---------------------|
| **定义方式** | `tools` 参数 + JSON Schema | `tools` 参数 + JSON Schema |
| **调用能力** | 并行调用 ✅ | 并行调用 ✅ |
| **强制调用** | `tool_choice="required"` | `tool_choice={"type": "any"}` |
| **返回格式** | `response.tool_calls` | `response.content[].input` |
| **流式工具调用** | ✅ | ✅ |

```python
# Anthropic 工具调用
import anthropic

client = anthropic.Anthropic()
response = client.messages.create(
    model="claude-sonnet-4-20250514",
    messages=[{"role": "user", "content": "北京天气？"}],
    tools=[{
        "name": "get_weather",
        "description": "获取天气",
        "input_schema": {
            "type": "object",
            "properties": {"city": {"type": "string"}},
            "required": ["city"]
        }
    }]
)
```

---

## 核心要点回顾

- ReAct = 思考→行动→观察→思考 的循环
- Function Calling 是 ReAct 的工业化实现 → 结构化的工具调用
- 工具 Schema 的 description 决定了 LLM 能否正确选择工具
- 并行工具调用可大幅减少多步任务的时间
