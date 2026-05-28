# 大模型 API 调用实战：多平台统一调用与流式输出

> **前提**：已完成 Python 基础语法学习
> **目标**：掌握主流大模型 API 调用方式，理解 HTTP POST、JSON 解析与流式输出

---

## 1. 核心概念

### API 调用本质

所有大模型 API 调用都是标准的 **HTTP POST 请求**：

```
客户端 → POST JSON → API 端点 → 模型推理 → 返回 JSON（或 SSE 流）
```

### 关键参数

| 参数 | 说明 | 典型值 |
|------|------|--------|
| `model` | 模型名称 | `deepseek-chat`, `gpt-4o`, `qwen-turbo` |
| `messages` | 对话消息列表 | `[{"role": "user", "content": "..."}]` |
| `temperature` | 随机性控制（0-2） | 0.0 精确，0.7 创造性 |
| `max_tokens` | 最大输出长度 | 1024, 4096 |
| `stream` | 是否流式输出 | `true` / `false` |

---

## 2. 主流平台 API 调用

### 2.1 OpenAI / 兼容 API（最通用）

```python
from openai import OpenAI

client = OpenAI(
    api_key="sk-xxx",
    base_url="https://api.openai.com/v1"  # 可替换为兼容端点
)

# 非流式调用
response = client.chat.completions.create(
    model="gpt-4o",
    messages=[
        {"role": "system", "content": "你是Java技术专家"},
        {"role": "user", "content": "解释Spring IoC原理"}
    ]
)
print(response.choices[0].message.content)
```

### 2.2 DeepSeek API（国产低成本）

```python
from openai import OpenAI

client = OpenAI(
    api_key="sk-你的DeepSeek密钥",
    base_url="https://api.deepseek.com"
)

response = client.chat.completions.create(
    model="deepseek-chat",
    messages=[{"role": "user", "content": "用Java写一个单例模式"}],
    temperature=0.7,
    max_tokens=2048
)
```

### 2.3 通义千问（阿里云）

```python
from openai import OpenAI

client = OpenAI(
    api_key="sk-你的通义千问密钥",
    base_url="https://dashscope.aliyuncs.com/compatible-mode/v1"
)

response = client.chat.completions.create(
    model="qwen-turbo",
    messages=[{"role": "user", "content": "解释分布式事务"}]
)
```

### 2.4 统一封装：多平台切换

```python
class LLMClient:
    """统一的大模型调用客户端"""

    PLATFORMS = {
        "deepseek": {
            "base_url": "https://api.deepseek.com",
            "default_model": "deepseek-chat"
        },
        "qwen": {
            "base_url": "https://dashscope.aliyuncs.com/compatible-mode/v1",
            "default_model": "qwen-turbo"
        },
        "openai": {
            "base_url": "https://api.openai.com/v1",
            "default_model": "gpt-4o"
        }
    }

    def __init__(self, platform="deepseek", api_key=None):
        config = self.PLATFORMS[platform]
        self.client = OpenAI(
            api_key=api_key,
            base_url=config["base_url"]
        )
        self.default_model = config["default_model"]

    def chat(self, messages, model=None, **kwargs):
        return self.client.chat.completions.create(
            model=model or self.default_model,
            messages=messages,
            **kwargs
        )

# 使用
llm = LLMClient(platform="deepseek", api_key="sk-xxx")
resp = llm.chat([{"role": "user", "content": "Hello"}])
```

---

## 3. 流式输出（SSE）

流式输出基于 **Server-Sent Events (SSE)**，模型逐 token 返回结果，提升用户体验。

```python
def stream_chat(prompt, client, model="deepseek-chat"):
    """流式对话，逐字打印"""
    response = client.chat.completions.create(
        model=model,
        messages=[{"role": "user", "content": prompt}],
        stream=True
    )

    full_content = ""
    for chunk in response:
        if chunk.choices[0].delta.content:
            content = chunk.choices[0].delta.content
            print(content, end="", flush=True)
            full_content += content
    return full_content
```

### Flask 中实现 SSE 流式接口

```python
from flask import Flask, Response, request, stream_with_context
import json
from openai import OpenAI

app = Flask(__name__)
client = OpenAI(api_key="sk-xxx", base_url="https://api.deepseek.com")

@app.route("/chat/stream", methods=["POST"])
def chat_stream():
    prompt = request.json.get("prompt")

    def generate():
        response = client.chat.completions.create(
            model="deepseek-chat",
            messages=[{"role": "user", "content": prompt}],
            stream=True
        )
        for chunk in response:
            if chunk.choices[0].delta.content:
                data = json.dumps(
                    {"content": chunk.choices[0].delta.content},
                    ensure_ascii=False
                )
                yield f"data: {data}\n\n"
        yield "data: [DONE]\n\n"

    return Response(
        stream_with_context(generate()),
        mimetype="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no"
        }
    )
```

---

## 4. 多轮对话与上下文管理

```python
class Conversation:
    """多轮对话管理器"""

    def __init__(self, system_prompt=None, max_history=10):
        self.messages = []
        if system_prompt:
            self.messages.append({"role": "system", "content": system_prompt})
        self.max_history = max_history

    def add_user_message(self, content):
        self.messages.append({"role": "user", "content": content})
        self._trim()

    def add_assistant_message(self, content):
        self.messages.append({"role": "assistant", "content": content})
        self._trim()

    def _trim(self):
        """保留 system prompt + 最近 N 轮对话"""
        system_msgs = [m for m in self.messages if m["role"] == "system"]
        other_msgs = [m for m in self.messages if m["role"] != "system"]
        self.messages = system_msgs + other_msgs[-(self.max_history * 2):]

    def get_messages(self):
        return self.messages

# 使用
conv = Conversation(system_prompt="你是Java后端技术专家")
conv.add_user_message("什么是AOP？")
# ... 调用 API ...
conv.add_assistant_message("AOP是面向切面编程...")
conv.add_user_message("在Spring中怎么用？")
# 对话历史自动保留
```

---

## 5. 常见问题与最佳实践

### Token 计算
```python
# 粗略估算：1 token ≈ 0.75 英文单词 ≈ 0.5 中文字
def estimate_tokens(text):
    return len(text) // 2  # 中文粗略估算
```

### 错误处理
```python
def safe_chat(client, messages, max_retries=3):
    import time
    for attempt in range(max_retries):
        try:
            return client.chat.completions.create(
                model="deepseek-chat",
                messages=messages
            )
        except Exception as e:
            if "rate_limit" in str(e).lower():
                time.sleep(2 ** attempt)  # 指数退避
            elif attempt == max_retries - 1:
                raise
```

### 成本控制
- DeepSeek：约 0.001 元/1K tokens（极低成本）
- 通义千问：百万 tokens 免费额度
- 生产环境：启用缓存减少重复调用
