# 03 - Function Calling 工具调用机制

> 🎯 Hermes 的 Function Calling 是其最核心的差异化能力——基于 ChatML + XML 标签的轻量工具调用协议，无框架绑定，可在任意推理引擎上实现 Agent 式递归工具调用

---

## 目录

1. [Function Calling 架构概览](#1-function-calling-架构概览)
2. [工具定义规范](#2-工具定义规范)
3. [工具调用与响应格式](#3-工具调用与响应格式)
4. [递归工具执行循环](#4-递归工具执行循环)
5. [vLLM 生产部署配置](#5-vllm-生产部署配置)
6. [最佳实践与注意事项](#6-最佳实践与注意事项)

---

## 1. Function Calling 架构概览

```
┌──────────┐     System Prompt + Tools     ┌──────────┐
│   User   │ ─────────────────────────────→ │  Hermes  │
│  Query   │                                │  Model   │
└──────────┘                                └────┬─────┘
                                                 │
                      ┌────────────┐              │
                      │  <tool_call> │ ←─────────┘
                      │  JSON Block  │   模型决定调用哪个工具
                      └─────┬──────┘
                            │
              ┌─────────────▼─────────────┐
              │     Tool Executor          │
              │  解析 JSON → 执行函数       │
              └─────────────┬─────────────┘
                            │
                      ┌─────▼──────┐
                      │ <tool_resp> │  执行结果回填
                      └─────┬──────┘
                            │
              ┌─────────────▼─────────────┐
              │  Natural Language Answer  │
              │  "今天的天气是..."          │
              └───────────────────────────┘
```

> 🎯 Hermes Function Calling 的核心原则：**工具定义是人（system prompt），工具选择是模型，工具执行是代码**

## 2. 工具定义规范

### 2.1 System Prompt 完整模板

```
<|im_start|>system
You are a function calling AI model. You are provided with function
signatures within <tools></tools> XML tags. You may call one or more
functions to assist with the user query. Don't make assumptions about
what values to plug into functions. Here are the available tools:
<tools>
{"type": "function", "function": {"name": "get_weather", ...}}
</tools>
Use the following pydantic model json schema for each tool call:
{"properties": {"arguments": {...}, "name": {...}},
 "required": ["arguments", "name"], "title": "FunctionCall", "type": "object"}
For each function call return a json object with function name and
arguments within <tool_call></tool_call> XML tags as follows:
<tool_call>
{"arguments": <args-dict>, "name": <function-name>}
</tool_call><|im_end|>
```

### 2.2 工具 Schema 标准格式

```json
{
  "type": "function",
  "function": {
    "name": "get_weather",
    "description": "Get the current weather for a location",
    "parameters": {
      "type": "object",
      "properties": {
        "location": {
          "type": "string",
          "description": "City name, e.g. 'San Francisco'"
        },
        "unit": {
          "type": "string",
          "enum": ["celsius", "fahrenheit"],
          "description": "Temperature unit",
          "default": "celsius"
        }
      },
      "required": ["location"]
    }
  }
}
```

### 2.3 多工具注册示例

```xml
<tools>
{"type":"function","function":{"name":"get_weather",...}}
{"type":"function","function":{"name":"get_stock_price",...}}
{"type":"function","function":{"name":"send_email",...}}
{"type":"function","function":{"name":"search_database",...}}
</tools>
```

> 💡 工具按 JSON 对象一一列出，顺序无影响。Hermes 模型可同时调用多个工具

## 3. 工具调用与响应格式

### 3.1 单工具调用

**模型输出：**
```xml
<tool_call>
{"name": "get_weather", "arguments": {"location": "Beijing", "unit": "celsius"}}
</tool_call>
```

**执行后回填：**
```xml
<tool_response>
{"name": "get_weather", "content": {"temperature": 22, "condition": "Sunny"}}
</tool_response>
```

### 3.2 并行多工具调用

```xml
<tool_call>
{"tool_calls": [
  {"name": "get_weather", "arguments": {"location": "Beijing"}},
  {"name": "get_weather", "arguments": {"location": "Shanghai"}},
  {"name": "get_stock_price", "arguments": {"symbol": "BABA"}}
]}
</tool_call>
```

> ⚠️ **vLLM 部署关键规则**：单次响应只输出一个 `<tool_call>` 块，多个调用放在同一个 `tool_calls` 数组内。不要在响应前后附加任何其他文本

### 3.3 完整交互示例

```
[User]: 帮我查一下北京天气和特斯拉股价

[Assistant]:
<tool_call>
{"tool_calls": [
  {"name": "get_weather", "arguments": {"location": "Beijing"}},
  {"name": "get_stock_price", "arguments": {"symbol": "TSLA"}}
]}
</tool_call>

[Tool 返回]:
<tool_response>
{"name": "get_weather", "content": {"temp": 18, "condition": "Cloudy"}}
</tool_response>
<tool_response>
{"name": "get_stock_price", "content": {"symbol": "TSLA", "price": 245.30}}
</tool_response>

[Assistant]:
北京今天多云，气温18°C。特斯拉（TSLA）当前股价为 $245.30。
```

## 4. 递归工具执行循环

### 4.1 核心算法

```python
def recursive_tool_loop(messages, tools, max_depth=5):
    """
    Hermes 递归工具调用循环
    - max_depth: 最大递归轮次（默认5），防止无限循环
    """
    for _ in range(max_depth):
        response = model.generate(messages)    # 1. 模型生成

        tool_calls = extract_tool_calls(response)  # 2. 提取 <tool_call>

        if not tool_calls:
            return response                    # 3. 无工具调用 → 最终答案

        tool_responses = []
        for call in tool_calls:
            result = execute_function(         # 4. 执行函数
                call["name"], call["arguments"]
            )
            tool_responses.append(format_tool_response(call["name"], result))

        messages.append({"role": "tool",       # 5. 回填结果
                         "content": tool_responses})
    return "Max recursion depth reached"
```

### 4.2 Hermes-Function-Calling 库用法

```bash
# 安装
pip install hermes-function-calling

# 命令行运行工具调用链
python functioncall.py \
  --model NousResearch/Hermes-3-Llama-3.1-8B \
  --query "Fetch Tesla stock fundamentals" \
  --max_depth 5 \
  --load_in_4bit
```

### 4.3 自定义工具注册

```python
from hermes_function_calling import tool

@tool
def get_stock_price(symbol: str) -> dict:
    """Get the current stock price for a given symbol."""
    # ... API 调用
    return {"symbol": symbol, "price": 245.30}

@tool
def send_email(to: str, subject: str, body: str) -> str:
    """Send an email."""
    # ... 邮件发送
    return f"Email sent to {to}"

# 注册到工具集
def get_openai_tools():
    return [get_stock_price, send_email]
```

## 5. vLLM 生产部署配置

### 5.1 启动命令

```bash
python -m vllm.entrypoints.openai.api_server \
  --model NousResearch/Hermes-3-Llama-3.1-8B \
  --dtype auto \
  --max-model-len 32768 \
  --enable-auto-tool-choice \
  --tool-call-parser hermes \
  --gpu-memory-utilization 0.90 \
  --max-num-seqs 4
```

| 参数 | 说明 | 重要性 |
|------|------|:---:|
| `--tool-call-parser hermes` | ⭐ 启用 Hermes XML 格式解析 | **必须** |
| `--enable-auto-tool-choice` | 模型自主决定是否调用工具 | 推荐 |
| `--max-model-len 32768` | 上下文窗口 | 按需 |
| `--gpu-memory-utilization` | GPU 显存利用率 | 0.85-0.95 |
| `--max-num-seqs` | 最大并发请求数 | 按 GPU 调整 |

### 5.2 vLLM 专用 System Prompt

```
You are a function-calling AI model. You are provided with function
signatures within <tools></tools> XML tags. You may call one or more
functions. Don't assume values for parameters.

<tools>
{{TOOL_DEFINITIONS_JSON}}
</tools>

When a tool is needed, output ONLY a single JSON object in <tool_call>:

<tool_call>
{"name": "<function_name>", "arguments": {...}}
</tool_call>

Rules:
- "name" MUST match exactly one tool in <tools>
- Put ALL parameters inside "arguments"
- No text before or after <tool_call> block
- For multiple calls, use "tool_calls" array in ONE block
- After receiving <tool_response>, you may chain further calls
- When done, output natural language answer ONLY

Follow schemas strictly; never invent parameters.
```

## 6. 最佳实践与注意事项

| 场景 | 建议 | 原因 |
|------|------|------|
| 工具数量 | ≤ 10 个 | 过多工具降低准确率 |
| 工具描述 | 写清楚每个参数的含义和约束 | 减少参数幻觉 |
| max_depth | 默认 5 | 防止死循环 |
| 工具响应格式 | 纯 JSON 或格式化文本 | 模型更容易解析 |
| 错误处理 | 工具异常返回 `{"error": "message"}` | 模型可根据错误重试 |
| 工具调用 + 推理 | DeepHermes 3 不支持同时使用 | 两者训练分开，混合结果不一致 |

> ⚠️ **DeepHermes 3 限制**：Function Calling 和推理模式（`<think>`）**不是同时训练的**，同时启用会导致不可预测的行为（虽然偶尔能工作得更准确）

## 核心要点回顾

- Hermes Function Calling = ChatML + `<tools>` XML + `<tool_call>` JSON
- 工具定义在 system prompt 的 `<tools>` 标签中，标准 OpenAI Function Schema
- 单/多工具调用均用 `<tool_call>` 包裹，结果用 `<tool_response>` 回填
- 递归循环模式（默认 5 轮）实现 Agent 式多步推理
- vLLM 用 `--tool-call-parser hermes` 可无缝对接 OpenAI API 格式
- DeepHermes 3 继承了 Function Calling，但不同时支持推理模式

## 参考资料

1. Nous Research - Hermes-Function-Calling GitHub 仓库
2. HuggingFace - vLLM Tool Calling Guide (Hermes 配置)
3. Ollama Registry - Hermes 3 Tool Calling Template
