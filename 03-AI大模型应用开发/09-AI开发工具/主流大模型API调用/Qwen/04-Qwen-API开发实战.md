# 04 - Qwen API 开发实战

> 🎯 Qwen API 通过阿里云百炼平台（Model Studio）提供 — 完全兼容 OpenAI 协议。本章覆盖快速接入、对话/流式、函数调用、多模态、价格计算与免费额度

---

## 目录

1. [快速接入：百炼平台三行配置](#1-快速接入百炼平台三行配置)
2. [对话与流式输出](#2-对话与流式输出)
3. [函数调用](#3-函数调用)
4. [多模态与长视频](#4-多模态与长视频)
5. [价格计算与免费额度](#5-价格计算与免费额度)

---

## 1. 快速接入：百炼平台三行配置

```python
# ① 开通百炼（bailian.aliyun.com）→ 创建 API Key
# ② 安装 OpenAI SDK

# ③ 配置（只需改 base_url 和 api_key）
from openai import OpenAI

client = OpenAI(
    api_key=os.getenv("DASHSCOPE_API_KEY"),      # 百炼控制台创建
    base_url="https://dashscope.aliyuncs.com/compatible-mode/v1"  # ★ 百炼兼容端点
)

# ④ 调用
response = client.chat.completions.create(
    model="qwen3.5-plus",                        # qwen3.5-plus / qwen3.5-flash / qwen3-max
    messages=[{"role": "user", "content": "解释什么是 MoE 架构"}]
)
print(response.choices[0].message.content)
```

> 💡 百炼平台已上线 **100+ 款国内外主流模型 API**（Qwen/GLM/Kimi/MiniMax/DeepSeek）— 一个 Key 调用全家。

---

## 2. 对话与流式输出

```python
# ① 基础对话（带系统提示）
response = client.chat.completions.create(
    model="qwen3.5-plus",
    max_tokens=4096,
    messages=[
        {"role": "system", "content": "你是 Java 专家，回答简洁"},
        {"role": "user", "content": "解释 Java 虚拟线程"}
    ]
)
print(response.choices[0].message.content)

# ② 流式输出
stream = client.chat.completions.create(
    model="qwen3.5-plus",
    messages=[{"role": "user", "content": "写一首关于编程的诗"}],
    stream=True
)
for chunk in stream:
    if chunk.choices[0].delta.content:
        print(chunk.choices[0].delta.content, end="", flush=True)

# ③ 思考模式（qwen3 系列支持思考/非思考切换）
response = client.chat.completions.create(
    model="qwen3.5-plus",
    messages=[{"role": "user", "content": "解决这道数学题并展示步骤"}],
    extra_body={"enable_thinking": True}        # 思考模式
)
```

---

## 3. 函数调用

```python
# 函数调用（OpenAI 完全同构）
tools = [{
    "type": "function",
    "function": {
        "name": "get_weather",
        "description": "查询城市天气",
        "parameters": {
            "type": "object",
            "properties": {
                "city": {"type": "string", "description": "城市名"}
            },
            "required": ["city"]
        }
    }
}]

messages = [{"role": "user", "content": "北京天气怎么样？"}]
response = client.chat.completions.create(
    model="qwen3.5-plus",
    messages=messages,
    tools=tools
)
msg = response.choices[0].message

if msg.tool_calls:
    messages.append(msg)
    for tc in msg.tool_calls:
        result = execute_tool(tc.function.name, tc.function.arguments)
        messages.append({
            "role": "tool",
            "tool_call_id": tc.id,               # ★ 必须匹配
            "content": result
        })
    response2 = client.chat.completions.create(model="qwen3.5-plus", messages=messages, tools=tools)
    print(response2.choices[0].message.content)
```

---

## 4. 多模态与长视频

```python
# ① 图片理解（原生多模态）
import base64

with open("diagram.png", "rb") as f:
    img_b64 = base64.b64encode(f.read()).decode()

response = client.chat.completions.create(
    model="qwen3.5-plus",
    messages=[{
        "role": "user",
        "content": [
            {"type": "text", "text": "分析这张架构图的问题"},
            {"type": "image_url", "image_url": {
                "url": f"data:image/png;base64,{img_b64}"
            }}
        ]
    }]
)

# ② 长视频理解（可解析最长 2 小时视频）
response = client.chat.completions.create(
    model="qwen3.5-plus",
    messages=[{
        "role": "user",
        "content": [
            {"type": "text", "text": "总结这个视频的主要内容"},
            {"type": "video_url", "video_url": {"url": "https://example.com/video.mp4"}}
        ]
    }]
)

# ③ 1M 上下文：长文档直接塞入
with open("large_doc.txt", encoding="utf-8") as f:
    doc = f.read()

response = client.chat.completions.create(
    model="qwen3.5-plus",
    messages=[{"role": "user", "content": f"分析这份文档：\n\n{doc}"}]
)
```

---

## 5. 价格计算与免费额度

```python
# Qwen 定价（¥/百万 tokens）
PRICES = {
    "qwen3.5-plus":   {"input": 0.8, "output": 4.8},
    "qwen3.5-flash":  {"input": 0.2, "output": None},
    "qwen3-max":      {"input": 2.5, "output": 10.0},
    "qwen-long":      {"input": 0.5, "output": 2.0},
}

def estimate_cost(model, in_tokens, out_tokens):
    p = PRICES[model]
    return (in_tokens * p["input"] + out_tokens * p["output"]) / 1_000_000

# 示例：10 万输入 + 2 万输出（qwen3.5-plus）
print(f"成本: ¥{estimate_cost('qwen3.5-plus', 100_000, 20_000):.4f}")
# = ¥0.08 + ¥0.096 = ¥0.176/次

# 免费额度：
# ① 新用户开通百炼：7000 万+ 免费 Token
# ② 各模型另有 100 万 Token 90 天免费额度
```

**Batch 调用：** 5 折（离线批量处理）。

---

> 🎯 **核心要点**：Qwen API 四件事 — **① 百炼平台（base_url: dashscope.aliyuncs.com/compatible-mode/v1）② OpenAI 兼容（改 base_url 即用）③ 多模态（图片/2 小时视频/1M 文档）④ 极致低价（Flash ¥0.2/百万）**。免费额度 7000 万+ Token 起步，新用户零成本试水。

**下一模块**：[05-Qwen-Agent与Coding-Plan](05-Qwen-Agent与Coding-Plan.md) / **返回总览**：[00-总览](00-Qwen-API知识体系总览.md)
