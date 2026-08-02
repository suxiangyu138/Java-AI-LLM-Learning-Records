# 04 - M3 文本 API 开发实战

> 🎯 M3 的独特优势 — **同时兼容 Anthropic 和 OpenAI 两种 API 格式**。推荐 Anthropic 协议（支持 thinking 块）。本章覆盖双协议接入、thinking 模式、函数调用、长上下文调用

---

## 目录

1. [双协议兼容总览](#1-双协议兼容总览)
2. [Anthropic 协议接入（推荐）](#2-anthropic-协议接入推荐)
3. [OpenAI 协议接入](#3-openai-协议接入)
4. [thinking 与 non-thinking 模式](#4-thinking-与-non-thinking-模式)
5. [函数调用与长上下文](#5-函数调用与长上下文)
6. [价格计算](#6-价格计算)

---

## 1. 双协议兼容总览

```text
M3 同时兼容两种 API 格式：

├── Anthropic 协议（推荐）
│   ├── 支持 thinking 块（深度思考）
│   ├── 与 Claude 生态完全兼容
│   └── 已接入 Claude Code/Roo Code/Cline
│
└── OpenAI 协议
    ├── Chat Completions 格式
    ├── 与 OpenAI 生态完全兼容
    └── 已接入 Cursor 等

切换成本：改 base_url + api_key（零代码迁移）
```

**接入平台：** platform.minimaxi.com（开放平台）

---

## 2. Anthropic 协议接入（推荐）

```python
# ① 安装（Anthropic SDK）
# pip install anthropic

# ② 配置
from anthropic import Anthropic

client = Anthropic(
    api_key=os.getenv("MINIMAX_API_KEY"),
    base_url="https://api.minimaxi.com/v1"     # ★ MiniMax 端点
)

# ③ 调用（与 Claude 完全同构）
response = client.messages.create(
    model="MiniMax-M3",
    max_tokens=8192,
    system="你是 Java 专家，回答简洁。",
    messages=[{"role": "user", "content": "解释 Java 虚拟线程"}]
)
print(response.content[0].text)
```

```python
# 带 thinking 块（复杂推理）
response = client.messages.create(
    model="MiniMax-M3",
    max_tokens=8192,
    thinking={"type": "enabled", "budget_tokens": 4096},   # 深度思考
    messages=[{"role": "user", "content": "分析这段代码的时间复杂度"}]
)

for block in response.content:
    if block.type == "thinking":
        print(f"[思考] {block.thinking}")
    elif block.type == "text":
        print(f"[回答] {block.text}")
```

---

## 3. OpenAI 协议接入

```python
# ① 安装（OpenAI SDK）
# pip install openai

# ② 配置
from openai import OpenAI

client = OpenAI(
    api_key=os.getenv("MINIMAX_API_KEY"),
    base_url="https://api.minimaxi.com/v1"     # ★ 同一端点
)

# ③ 调用
response = client.chat.completions.create(
    model="MiniMax-M3",
    messages=[
        {"role": "system", "content": "你是 Java 专家"},
        {"role": "user", "content": "解释 Spring IoC"}
    ]
)
print(response.choices[0].message.content)

# 流式输出
stream = client.chat.completions.create(
    model="MiniMax-M3",
    messages=[{"role": "user", "content": "写一段快速排序"}],
    stream=True
)
for chunk in stream:
    if chunk.choices[0].delta.content:
        print(chunk.choices[0].delta.content, end="", flush=True)
```

---

## 4. thinking 与 non-thinking 模式

```text
M3 的双思考模式（共享同一套定价，请求时切换）：

├── thinking 模式
│   ├── 适合：复杂推理、Agentic 任务、长程协作
│   ├── 使用 Anthropic 协议的 thinking 块
│   └── 质量更高、耗时更长
│
└── non-thinking 模式
    ├── 适合：快速响应、简单任务
    ├── 响应更快
    └── 适合实时交互
```

```python
# Anthropic 协议切换
# thinking 模式：
thinking={"type": "enabled", "budget_tokens": 4096}
# non-thinking 模式：
# 不传 thinking 参数即可

# 按任务类型动态切换
def smart_call(task_type, messages):
    params = {"model": "MiniMax-M3", "max_tokens": 8192, "messages": messages}
    if task_type in ("code", "debug", "analysis"):
        params["thinking"] = {"type": "enabled", "budget_tokens": 4096}
    return client.messages.create(**params)
```

---

## 5. 函数调用与长上下文

```python
# ① 函数调用（Anthropic 协议）
tools = [{
    "name": "get_weather",
    "description": "查询城市天气",
    "input_schema": {
        "type": "object",
        "properties": {
            "city": {"type": "string", "description": "城市名"}
        },
        "required": ["city"]
    }
}]

response = client.messages.create(
    model="MiniMax-M3",
    max_tokens=4096,
    tools=tools,
    messages=[{"role": "user", "content": "北京天气怎么样？"}]
)

if response.stop_reason == "tool_use":
    for block in response.content:
        if block.type == "tool_use":
            result = execute_tool(block.name, block.input)
            response2 = client.messages.create(
                model="MiniMax-M3",
                max_tokens=4096,
                tools=tools,
                messages=[
                    {"role": "user", "content": "北京天气怎么样？"},
                    {"role": "assistant", "content": response.content},
                    {"role": "user", "content": [
                        {"type": "tool_result", "tool_use_id": block.id, "content": result}
                    ]}
                ]
            )

# ② 长上下文（1M 窗口直接塞入）
with open("large_codebase_summary.txt", encoding="utf-8") as f:
    content = f.read()

response = client.messages.create(
    model="MiniMax-M3",
    max_tokens=8192,
    messages=[{"role": "user", "content": f"分析这份文档：\n\n{content}"}]
)
```

---

## 6. 价格计算

```python
# M3 定价（¥/百万 tokens）
PRICES = {
    "MiniMax-M3":       {"input": 2.1, "output": 8.4, "cache": 0.42},
    "MiniMax-M3-highspeed": {"input": 4.2, "output": 16.8, "cache": 0.42},
    "MiniMax-M2.7":     {"input": 2.1, "output": 8.4, "cache": 0.42},
}

def estimate_cost(model, in_tokens, out_tokens, cache_tokens=0):
    p = PRICES[model]
    return (cache_tokens * p["cache"] + (in_tokens - cache_tokens) * p["input"]
            + out_tokens * p["output"]) / 1_000_000

# 示例：10 万输入（50% 缓存）+ 2 万输出
print(f"成本: ¥{estimate_cost('MiniMax-M3', 100_000, 20_000, 50_000):.4f}")
# ≈ ¥0.021 + ¥0.105 + ¥0.168 = ¥0.29/次
```

**Token Plan 订阅（比按量更划算）：**

| 套餐 | 价格 | 额度 | 对比 |
|------|:---:|------|------|
| Plus | 49 元/月 | 6 亿 token | 同价位用量为 Claude 的 15 倍 |
| Max | 119 元/月 | 18 亿 token | — |
| Ultra | 469 元/月 | 55 亿 token | — |

---

> 🎯 **核心要点**：M3 API 四件事 — **① 双协议兼容（Anthropic 推荐 + OpenAI 备用，同一端点）② thinking 模式（复杂任务开、简单任务关，共享定价）③ 函数调用双协议同构 ④ Token Plan 超值（同价位 15 倍于 Claude）**。价格 $0.30/$1.20 仅为 Claude Opus 的 1/15。

**下一模块**：[05-语音与视频全模态能力](05-语音与视频全模态能力.md) / **返回总览**：[00-总览](00-MiniMax-API知识体系总览.md)
