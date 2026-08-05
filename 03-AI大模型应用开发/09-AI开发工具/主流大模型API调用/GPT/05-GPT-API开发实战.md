# 05 - GPT API 开发实战

> 🎯 从快速接入到完整 Agent — 对话、流式 SSE、结构化输出、函数调用、多模态。全部使用 Responses API（新范式）+ Python SDK 可运行代码

---

## 目录

1. [快速接入](#1-快速接入)
2. [对话与流式输出](#2-对话与流式输出)
3. [结构化输出](#3-结构化输出)
4. [函数调用完整示例](#4-函数调用完整示例)
5. [多模态输入](#5-多模态输入)
6. [错误处理速查](#6-错误处理速查)

---

## 1. 快速接入

```python
# pip install openai
from openai import OpenAI

client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

# 最简调用（Responses API）
response = client.responses.create(
    model="gpt-5.6-terra",
    input="用一句话解释什么是 RAG"
)
print(response.output_text)

# 指定推理档位
response = client.responses.create(
    model="gpt-5.6-terra",
    reasoning={"effort": "medium"},
    input="分析这段代码的性能问题"
)
```

---

## 2. 对话与流式输出

```python
# ① 基础对话（含指令）
response = client.responses.create(
    model="gpt-5.6-terra",
    instructions="你是 Java 专家，回答简洁并给出代码示例。",
    input="解释 Spring 循环依赖的三级缓存"
)
print(response.output_text)

# ② 流式输出（SSE 打字机）
stream = client.responses.create(
    model="gpt-5.6-terra",
    input="写一首关于编程的诗",
    stream=True
)
for event in stream:
    if event.type == "response.output_text.delta":
        print(event.delta, end="", flush=True)

# ③ 多轮对话（previous_response_id 简化）
first = client.responses.create(
    model="gpt-5.6-terra",
    input="什么是 Java 虚拟线程？"
)
second = client.responses.create(
    model="gpt-5.6-terra",
    previous_response_id=first.id,          # ★ 直接引用
    input="它的局限是什么？"
)
```

---

## 3. 结构化输出

```python
# JSON Schema 严格模式
response = client.responses.create(
    model="gpt-5.6-terra",
    input="提取订单信息：8月1日张三购买3台MacBook，总价29400元",
    text={
        "format": {
            "type": "json_schema",
            "name": "order_extraction",
            "strict": True,
            "schema": {
                "type": "object",
                "properties": {
                    "date":     {"type": "string"},
                    "customer": {"type": "string"},
                    "product":  {"type": "string"},
                    "quantity": {"type": "integer"},
                    "total":    {"type": "number"}
                },
                "required": ["date", "customer", "product", "quantity", "total"]
            }
        }
    }
)
import json
data = json.loads(response.output_text)
# {"date": "2026-08-01", "customer": "张三", "product": "MacBook", "quantity": 3, "total": 29400}

# 或使用 Pydantic 自动生成 Schema
from pydantic import BaseModel

class Order(BaseModel):
    date: str
    customer: str
    product: str
    quantity: int
    total: float

response = client.responses.create(
    model="gpt-5.6-terra",
    input="提取订单信息：...",
    text={"format": {
        "type": "json_schema",
        "name": "order",
        "strict": True,
        "schema": Order.model_json_schema()
    }}
)
```

---

## 4. 函数调用完整示例

```python
# 工具定义
tools = [{
    "type": "function",
    "name": "get_weather",
    "description": "查询城市实时天气",
    "parameters": {
        "type": "object",
        "properties": {
            "city": {"type": "string", "description": "城市名"}
        },
        "required": ["city"]
    }
}]

# ① 第一次请求
response = client.responses.create(
    model="gpt-5.6-terra",
    tools=tools,
    input="北京今天天气怎么样？"
)

# ② 检查工具调用
tool_calls = [item for item in response.output if item.type == "function_call"]
if tool_calls:
    for call in tool_calls:
        # ③ 执行工具
        result = execute_tool(call.name, call.arguments)
        # ④ 回传结果（Responses API 方式）
        response = client.responses.create(
            model="gpt-5.6-terra",
            previous_response_id=response.id,
            tools=tools,
            input=[{
                "type": "function_call_output",
                "call_id": call.call_id,      # ★ 匹配 ID
                "output": result
            }]
        )
    print(response.output_text)  # ⑤ 最终回答
```

---

## 5. 多模态输入

```python
import base64

# 图片理解（base64）
with open("architecture.png", "rb") as f:
    img_b64 = base64.b64encode(f.read()).decode()

response = client.responses.create(
    model="gpt-5.6-terra",
    input=[
        {"type": "message", "role": "user", "content": [
            {"type": "input_text", "text": "分析这个架构图"},
            {"type": "input_image", "image_url": f"data:image/png;base64,{img_b64}"}
        ]}
    ]
)
print(response.output_text)

# 视觉细节设置（保留原尺寸）
response = client.responses.create(
    model="gpt-5.6-terra",
    input=[...],
    reasoning={"effort": "medium", "visual_details": "original"}  # original/auto
)
```

---

## 6. 错误处理速查

| 错误码 | 含义 | 处理 |
|--------|------|------|
| 400 | 参数错误 | 检查模型 ID/工具格式/effort 值 |
| 401 | API Key 无效 | 检查 OPENAI_API_KEY |
| 404 | 模型不存在 | 检查模型 ID（gpt-5.6-terra 等） |
| 429 | 限流 | 指数退避（1s→2s→4s）+ 检查 Tier |
| 500 | 服务端错误 | 重试 |
| 529 | 过载 | 退避重试 |

```python
# 通用重试封装
import time

def call_with_retry(fn, max_retries=5):
    for attempt in range(max_retries):
        try:
            return fn()
        except Exception as e:
            if attempt == max_retries - 1:
                raise
            time.sleep(2 ** attempt + 0.5)  # 指数退避
```

---

> 🎯 **核心要点**：GPT-5.6 实战五件事 — **① 默认 Terra（半价）② Responses API（previous_response_id 简化多轮）③ 结构化输出（text.format JSON Schema）④ 函数调用（function_call_output 回传）⑤ 流式（response.output_text.delta）**。

**下一模块**：[06-生产实践与成本优化](06-生产实践与成本优化.md) / **返回总览**：[00-总览](00-GPT-API知识体系总览.md)
