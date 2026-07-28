# 05 - ChatGPT API 开发实战

> 🎯 OpenAI API = 所有 LLM API 的事实标准。一个 `chat.completions.create()` 打天下，流式+FC+JSON+Vision 四大增强模式

---

## 目录

1. [API 概述](#1-api-概述)
2. [核心 API 全解](#2-核心-api-全解)
3. [Function Calling 实战](#3-function-calling-实战)
4. [成本计算与优化](#4-成本计算与优化)
5. [错误处理与重试](#5-错误处理与重试)
6. [Java 后端调用示例](#6-java-后端调用示例)

---

## 1. API 概述

OpenAI API 提供 Chat Completions、Embeddings、TTS、Vision 等接口。Chat Completions 是核心——对话、代码生成、JSON 输出、工具调用全部通过这一个端点。

**兼容性**：OpenAI API 格式已成为行业标准，Ollama、vLLM、DeepSeek 全部提供 OpenAI 兼容接口。学一套代码，通吃所有 LLM。

---

## 2. 核心 API 全解

```python
from openai import OpenAI
client = OpenAI()

# ① 基础调用
resp = client.chat.completions.create(
    model="gpt-4o-mini",
    messages=[{"role": "user", "content": "解释 Java 多态"}],
    temperature=0.7, max_tokens=512
)
print(resp.choices[0].message.content)

# ② 流式输出（逐 Token 返回）
stream = client.chat.completions.create(
    model="gpt-4o-mini", messages=[...], stream=True
)
for chunk in stream:
    if chunk.choices[0].delta.content:
        print(chunk.choices[0].delta.content, end="")

# ③ JSON Mode（强制输出合法 JSON）
resp = client.chat.completions.create(
    model="gpt-4o", messages=[...],
    response_format={"type": "json_object"}
)
data = json.loads(resp.choices[0].message.content)

# ④ Vision 图片理解
resp = client.chat.completions.create(
    model="gpt-4o",
    messages=[{"role":"user","content":[
        {"type":"text","text":"描述这张架构图"},
        {"type":"image_url","image_url":{"url":"https://example.com/arch.png"}}
    ]}]
)
```

---

## 3. Function Calling 实战

```python
tools = [{
    "type": "function",
    "function": {
        "name": "get_weather",
        "description": "获取指定城市的实时天气信息",
        "parameters": {
            "type": "object",
            "properties": {
                "city": {"type": "string", "description": "城市名称，如北京"}
            },
            "required": ["city"]
        }
    }
}]

# LLM 自动决定是否调用工具
resp = client.chat.completions.create(
    model="gpt-4o", messages=messages, tools=tools
)

# 检查是否有 tool_calls
if resp.choices[0].message.tool_calls:
    tool_call = resp.choices[0].message.tool_calls[0]
    func_name = tool_call.function.name
    func_args = json.loads(tool_call.function.arguments)
    # 执行工具 → 结果送回 LLM
```

---

## 4. 成本计算与优化

```python
def estimate_cost(model, prompt_tokens, completion_tokens):
    rates = {
        "gpt-4o": (5/1e6, 15/1e6),
        "gpt-4o-mini": (0.15/1e6, 0.6/1e6),
    }
    return prompt_tokens * rates[model][0] + completion_tokens * rates[model][1]

# 典型对话（1000 tokens 输入 + 500 tokens 输出）：
# GPT-4o: ~$0.012 | GPT-4o-mini: ~$0.0004
```

**优化策略**：① 简单任务用 gpt-4o-mini（省 30×）；② 流式输出减少等待；③ 缓存重复查询；④ Prompt 精简（去除冗余上下文）。

---

## 5. 错误处理与重试

```python
from tenacity import retry, stop_after_attempt, wait_exponential

@retry(
    wait=wait_exponential(min=1, max=60),  # 1s→2s→4s→8s...→60s
    stop=stop_after_attempt(5),
    retry=retry_if_exception_type(RateLimitError)
)
def call_llm(prompt):
    return client.chat.completions.create(model="gpt-4o-mini", messages=[...])
```

**不可重试的错误**：认证失败（401）、参数错误（400）、权限不足（403）→ 直接抛异常排查。

---

## 6. Java 后端调用示例

```java
// 使用 Spring RestTemplate + OpenAI 兼容格式
RestTemplate rest = new RestTemplate();
HttpHeaders headers = new HttpHeaders();
headers.setBearerAuth("sk-xxx");

Map<String, Object> body = Map.of(
    "model", "gpt-4o-mini",
    "messages", List.of(Map.of("role", "user", "content", "解释Java多态")),
    "temperature", 0.7, "max_tokens", 512
);

String resp = rest.postForObject(
    "https://api.openai.com/v1/chat/completions",
    new HttpEntity<>(body, headers), String.class
);
```

> 🎯 OpenAI API = 一个端点打天下。流式交互、FC 调工具、JSON Mode 结构化、Vision 看图片。代码→debug→审查全搞定。
