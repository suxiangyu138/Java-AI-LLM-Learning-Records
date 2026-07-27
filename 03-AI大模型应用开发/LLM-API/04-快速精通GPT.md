# 🚀 快速精通 GPT（OpenAI）

> **核心摘要**：OpenAI GPT 系列是当前最主流、生态最成熟的闭源大语言模型。本文涵盖 GPT 模型全景图、API 完整实战（基础对话、流式输出、Function Calling、Structured Outputs、Vision）、进阶能力（Prompt Caching、Batch API、推理模型）、成本优化与 Prompt 工程最佳实践。

> **前置阅读**：[[02-大模型API调用实践]]、[[01-LLM API协议与核心概念]]

---

## 目录

1. [GPT 是什么](#1-gpt-是什么)
2. [模型全景图](#2-模型全景图)
3. [核心概念深入](#3-核心概念深入)
4. [API 完整实战](#4-api-完整实战)
5. [进阶能力](#5-进阶能力)
6. [成本与优化](#6-成本与优化)
7. [Prompt 工程](#7-prompt-工程)
8. [最佳实践](#8-最佳实践)

---

## 1. GPT 是什么

**GPT**（Generative Pre-trained Transformer）是 OpenAI 开发的生成式预训练 Transformer 模型系列。从 GPT-1（2018）到 GPT-5.5（2026），持续定义大语言模型的能力边界。

### 核心能力

- **文本生成**：续写、总结、翻译、改写、创意写作
- **代码能力**：生成、解释、重构、调试、语言转换
- **推理能力**：逻辑推导、数学证明、多步规划
- **多模态理解**：图片输入（GPT-4o / GPT-4.1）
- **工具使用**：Function Calling，调用外部 API
- **结构化输出**：严格符合 JSON Schema

---

## 2. 模型全景图

### 2.1 主力模型

| 模型 | 定位 | Context | 速度 | 多模态 | 典型场景 |
|---|---|---|---|---|---|
| **GPT-5.5** | 最新旗舰 | 1M tokens | 中 | 是 | 最强综合能力，复杂 Agent |
| **GPT-4.1** | 编码旗舰 | 1M tokens | 中 | 是 | 代码生成、审查、重构 |
| **GPT-4o** | 多模态主力 | 128K tokens | 快 | 全文/图/音 | 日常对话、图片分析 |
| **GPT-4o-mini** | 经济首选 | 128K tokens | 极快 | 是 | 分类、标签、批处理 |
| **o3** | 深度推理 | 200K tokens | 慢 | 否 | 数学竞赛、科学推理 |
| **o4-mini** | 轻量推理 | 200K tokens | 快 | 否 | 代码审查、逻辑校验 |

### 2.2 模型选择决策树

```
需要多模态？
├── 是 → GPT-4.1（编码+多模态）或 GPT-4o（纯多模态）
└── 否 → 需要深度推理？
    ├── 是 → o3（最强）或 o4-mini（轻量）
    └── 否 → 简单任务/批处理？
        ├── 是 → GPT-4o-mini
        └── 否 → GPT-4.1（综合最强）
```

---

## 3. 核心概念深入

### 3.1 Token 计算

| 类型 | 换算关系 |
|---|---|
| 1 个英文单词 | ≈ 1.3 tokens |
| 1 个中文字 | ≈ 1.5~2 tokens |
| 1000 tokens | ≈ 750 英文单词 / 500 中文字 |

```python
import tiktoken
enc = tiktoken.encoding_for_model("gpt-4.1")
tokens = enc.encode("Hello, 你好世界")
print(f"Token 数: {len(tokens)}")
```

### 3.2 Context Window

| 窗口大小 | 实际承载 | 适用模型 |
|---|---|---|
| 128K | ≈ 300 页 PDF | GPT-4o, GPT-4o-mini |
| 200K | ≈ 500 页 PDF | o3, o4-mini |
| 1M | ≈ 2500 页 PDF | GPT-4.1, GPT-5.5 |

> **注意**：上下文超过 70% 填充率时模型可能出现"Lost in the Middle"效应，建议分块+索引策略。

### 3.3 Temperature 与 Top-P

| 场景 | Temperature | 说明 |
|---|---|---|
| 数学、代码、分类 | 0.0 | 确定性强，输出一致 |
| 日常对话 | 0.7 | 平衡创造性 |
| 写作、头脑风暴 | 1.0+ | 随机性大 |

> **建议**：只调节 Temperature，Top-P 保持默认 1.0。

---

## 4. API 完整实战

### 4.1 环境准备

```bash
pip install openai
```

```python
from openai import OpenAI
import os

client = OpenAI(api_key=os.environ.get("OPENAI_API_KEY"))
```

### 4.2 基础对话

```python
response = client.chat.completions.create(
    model="gpt-4.1",
    messages=[
        {"role": "system", "content": "你是 Java 技术专家。"},
        {"role": "user", "content": "volatile 关键字的作用是什么？"}
    ],
    temperature=0.5,
    max_tokens=2048
)

answer = response.choices[0].message.content
usage = response.usage
print(f"输入: {usage.prompt_tokens} tokens, 输出: {usage.completion_tokens} tokens")
```

### 4.3 流式输出

```python
stream = client.chat.completions.create(
    model="gpt-4.1",
    messages=[{"role": "user", "content": "写一个 Java 二分查找"}],
    stream=True
)

full_response = ""
for chunk in stream:
    content = chunk.choices[0].delta.content
    if content:
        full_response += content
        print(content, end="", flush=True)
```

### 4.4 多轮对话

```python
messages = [{"role": "system", "content": "你是 Java 面试官。"}]

def ask(question):
    messages.append({"role": "user", "content": question})
    r = client.chat.completions.create(model="gpt-4.1", messages=messages)
    reply = r.choices[0].message.content
    messages.append({"role": "assistant", "content": reply})
    return reply

ask("解释 HashMap 的工作原理")
ask("那 JDK 8 中 HashMap 有什么优化？")
ask("为什么链表转红黑树的阈值是 8？")
```

### 4.5 Function Calling

```python
tools = [
    {
        "type": "function",
        "function": {
            "name": "get_current_weather",
            "description": "获取指定城市当前的天气情况",
            "parameters": {
                "type": "object",
                "properties": {
                    "location": {"type": "string", "description": "城市名"},
                    "unit": {"type": "string", "enum": ["celsius", "fahrenheit"]}
                },
                "required": ["location"]
            }
        }
    }
]

response = client.chat.completions.create(
    model="gpt-4.1",
    messages=[{"role": "user", "content": "北京今天多少度？"}],
    tools=tools,
    tool_choice="auto"
)

msg = response.choices[0].message
if msg.tool_calls:
    for tool_call in msg.tool_calls:
        func_name = tool_call.function.name
        func_args = json.loads(tool_call.function.arguments)
        print(f"模型要调用: {func_name}({func_args})")
```

> **重点**：Function Calling 的 function description 应清晰说明"何时调用"、"参数含义"、"返回值格式"，引导模型正确选择工具。

### 4.6 Structured Outputs

```python
from pydantic import BaseModel
from typing import Optional, Literal
import json

class CodeReview(BaseModel):
    overall_score: float
    summary: str
    issues: list[str]
    suggestion: Optional[str] = None
    risk_level: Literal["low", "medium", "high", "critical"]

response = client.chat.completions.create(
    model="gpt-4.1",
    messages=[{"role": "user", "content": "审查这段代码：..."}],
    response_format={
        "type": "json_schema",
        "json_schema": {
            "name": "code_review",
            "schema": CodeReview.model_json_schema(),
            "strict": True
        }
    }
)

result = json.loads(response.choices[0].message.content)
```

### 4.7 图片理解（Vision）

```python
import base64

def encode_image(path: str) -> str:
    with open(path, "rb") as f:
        return base64.b64encode(f.read()).decode("utf-8")

image_b64 = encode_image("screenshot.png")

response = client.chat.completions.create(
    model="gpt-4.1",
    messages=[{
        "role": "user",
        "content": [
            {"type": "text", "text": "这张架构图中有哪些组件？"},
            {"type": "image_url", "image_url": {
                "url": f"data:image/png;base64,{image_b64}",
                "detail": "high"
            }}
        ]
    }]
)
```

> **注意**：`detail` 参数控制图片分辨率——`low`（512x512 低成本）适合图标，`high` 适合文字密集型图表。

---

## 5. 进阶能力

### 5.1 Prompt Caching

GPT-4.1 / GPT-4o 系列**自动**缓存重复的前缀内容（需 >= 1024 tokens），无需额外代码：

```python
response = client.chat.completions.create(
    model="gpt-4.1",
    messages=messages
)
print(response.usage.prompt_tokens_details)  # 查看缓存命中情况
```

### 5.2 Batch API

适合不需要实时响应的场景（数据标注、批量翻译），**费用减半**，24 小时内完成：

```python
import json

tasks = [
    {"custom_id": "task-1", "method": "POST", "url": "/v1/chat/completions",
     "body": {"model": "gpt-4o-mini", "messages": [
         {"role": "user", "content": "Translate to Chinese: Hello World"}]}},
]

with open("batch_input.jsonl", "w") as f:
    for task in tasks:
        f.write(json.dumps(task) + "\n")

batch_input = client.files.create(file=open("batch_input.jsonl", "rb"), purpose="batch")
batch = client.batches.create(
    input_file_id=batch_input.id,
    endpoint="/v1/chat/completions",
    completion_window="24h"
)
```

### 5.3 推理模型（o3 / o4-mini）

```python
response = client.chat.completions.create(
    model="o4-mini",
    messages=[{"role": "user", "content": "三个盒子概率问题..."}],
    max_completion_tokens=5000  # 注意：不是 max_tokens
)
```

> **注意**：推理模型不支持 `temperature`、`top_p`、system prompt。使用 `max_completion_tokens` 而非 `max_tokens`。

---

## 6. 成本与优化

### 6.1 定价速览

| 模型 | Input $/1M tokens | Output $/1M tokens |
|---|---|---|
| GPT-5.5 | $2.50 | $10.00 |
| GPT-4.1 | $2.00 | $8.00 |
| GPT-4o | $2.50 | $10.00 |
| GPT-4o-mini | $0.15 | $0.60 |
| o3 | $10.00 | $40.00 |
| o4-mini | $1.10 | $4.40 |

### 6.2 降本策略

1. **选对模型**：简单分类用 GPT-4o-mini，代码生成用 GPT-4.1
2. **缩短 System Prompt**：删减冗余指令
3. **限制 `max_tokens`**：避免生成不必要的长文本
4. **Batch API**：非实时任务费用减半
5. **Prompt Caching**：重复前缀自动缓存，费用减半

```python
def trim_history(messages, max_tokens=50000):
    """上下文裁剪：保留 system + 最近 N 轮"""
    kept = [messages[0]] if messages[0]["role"] == "system" else []
    total = 0
    for msg in reversed(messages[1:]):
        total += len(msg["content"]) // 2
        if total > max_tokens:
            break
        kept.insert(1, msg)
    return kept
```

---

## 7. Prompt 工程

### 7.1 五大黄金法则

1. **角色明确**："你是 XX 专家，擅长 YY"
2. **任务具体**：不要"写得好一点"，要"用 Java 17 + 不可变类"
3. **格式限定**："输出 JSON，包含 name, age, skills"
4. **给出例子**：Few-shot 提供 1~3 个范例
5. **约束边界**："只回答 Java 相关，否则回复'超出范围'"

### 7.2 实战模板

```python
# 模板：代码生成
CODE_GEN = """
## 角色
{role}

## 任务
{task}

## 技术栈
{tech_stack}

## 要求
{requirements}

## 输出格式
{output_format}
"""
```

---

## 8. 最佳实践

### 8.1 错误处理

```python
import time
from openai import RateLimitError, APITimeoutError, AuthenticationError

def call_with_retry(messages, model="gpt-4.1", max_retries=3):
    for attempt in range(max_retries):
        try:
            return client.chat.completions.create(
                model=model, messages=messages, timeout=60
            )
        except RateLimitError:
            if attempt < max_retries - 1:
                time.sleep(2 ** attempt * 5)
            else:
                raise
        except APITimeoutError:
            if attempt < max_retries - 1:
                time.sleep(2)
            else:
                raise
        except AuthenticationError:
            raise  # API Key 问题，不重试
```

### 8.2 安全红线

- API Key 放环境变量，不硬编码
- API Key 不要提交到 git（`.env` 加入 `.gitignore`）
- AI 生成的代码必须人工审查后才可执行

### 8.3 日志与监控

```python
import logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("gpt-api")

def logged_chat(messages, model="gpt-4.1", **kwargs):
    start = time.time()
    response = client.chat.completions.create(model=model, messages=messages, **kwargs)
    elapsed = time.time() - start
    logger.info(f"{model} | {elapsed:.1f}s | "
                f"输入 {response.usage.prompt_tokens}t | "
                f"输出 {response.usage.completion_tokens}t")
    return response
```

---

## 核心要点回顾

- GPT 模型家族覆盖从经济型（GPT-4o-mini）到旗舰推理（o3）的全场景
- 核心 API 操作：基础对话、流式输出、多轮对话、Function Calling、Structured Outputs、Vision
- Function Calling 让模型能调用外部工具，Structured Outputs 保证 JSON 格式严格符合 Schema
- 降本三要素：选对模型、Prompt Caching、Batch API
- Prompt 工程五大法则：角色明确、任务具体、格式限定、给出例子、约束边界

## 参考资料

1. OpenAI 官方文档：https://platform.openai.com/docs
2. OpenAI Cookbook：https://cookbook.openai.com/
3. OpenAI Tokenizer：https://platform.openai.com/tokenizer
4. OpenAI Pricing：https://openai.com/api/pricing/
5. openai Python SDK：https://pypi.org/project/openai/
