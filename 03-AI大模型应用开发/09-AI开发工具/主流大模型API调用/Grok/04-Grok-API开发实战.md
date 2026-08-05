# 04 - Grok API 开发实战

> 🎯 Grok API 完全兼容 OpenAI 协议 — 改 base_url 即用。本章覆盖快速接入、对话/流式、函数调用、实时搜索工具、价格与免费额度

---

## 目录

1. [快速接入：OpenAI 兼容两行配置](#1-快速接入openai-兼容两行配置)
2. [对话与流式输出](#2-对话与流式输出)
3. [函数调用](#3-函数调用)
4. [实时搜索工具调用](#4-实时搜索工具调用)
5. [价格计算与免费额度](#5-价格计算与免费额度)

---

## 1. 快速接入：OpenAI 兼容两行配置

```python
# ① 安装（官方 OpenAI SDK）
# pip install openai

# ② 配置（只需改 base_url 和 api_key）
from openai import OpenAI

client = OpenAI(
    api_key=os.getenv("XAI_API_KEY"),           # platform.x.ai 创建
    base_url="https://api.x.ai/v1"               # ★ Grok 端点
)

# ③ 调用
response = client.chat.completions.create(
    model="grok-4.5",                            # grok-4.5 / grok-4-1-fast / grok-4.3 / grok-4.20
    messages=[{"role": "user", "content": "解释一下什么是 RAG"}]
)
print(response.choices[0].message.content)
```

> 💡 也可通过聚合网关 `https://api.ofox.ai/v1` 一个 Key 访问 Grok/Claude/GPT 等 100+ 模型。

---

## 2. 对话与流式输出

```python
# ① 基础对话（带系统提示）
response = client.chat.completions.create(
    model="grok-4.5",
    messages=[
        {"role": "system", "content": "你是 Java 专家，回答简洁"},
        {"role": "user", "content": "解释 Java 虚拟线程"}
    ]
)
print(response.choices[0].message.content)

# ② 流式输出
stream = client.chat.completions.create(
    model="grok-4.5",
    messages=[{"role": "user", "content": "写一首关于春天的诗"}],
    stream=True
)
for chunk in stream:
    if chunk.choices[0].delta.content:
        print(chunk.choices[0].delta.content, end="", flush=True)

# ③ 超长上下文（2M 窗口：整库代码/长文档）
response = client.chat.completions.create(
    model="grok-4-1-fast",                        # 2M 上下文
    messages=[{"role": "user", "content": "分析这个仓库的结构并总结"}]
    # 无需 RAG/分块，直接塞入大文件
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
    model="grok-4.5",
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
    response2 = client.chat.completions.create(model="grok-4.5", messages=messages, tools=tools)
    print(response2.choices[0].message.content)
```

---

## 4. 实时搜索工具调用

```python
# ★ Grok 独家：X 搜索 + Web 搜索内置工具
tools = [
    {"type": "function", "function": {
        "name": "search_x",
        "description": "搜索 X/Twitter 实时帖子（独家能力）",
        "parameters": {
            "type": "object",
            "properties": {
                "query": {"type": "string"},
                "since": {"type": "string", "description": "起始日期 YYYY-MM-DD"},
                "until": {"type": "string"}
            },
            "required": ["query"]
        }
    }},
    {"type": "function", "function": {
        "name": "web_search",
        "description": "实时 Web 搜索",
        "parameters": {
            "type": "object",
            "properties": {"query": {"type": "string"}},
            "required": ["query"]
        }
    }}
]

# 舆情监控示例
response = client.chat.completions.create(
    model="grok-4.5",
    tools=tools,
    messages=[{"role": "user", "content": "搜索最近 3 天关于'Spring Boot 4'的推文，总结开发者反馈"}]
)
# Grok 自动：① search_x("Spring Boot 4") ② web_search 补充 ③ 汇总分析
```

**内置工具计费：** Web 搜索 / X 搜索 / 代码执行 $2.50-5.00/千次成功调用。

---

## 5. 价格计算与免费额度

```python
# Grok 定价（$/百万 tokens）
PRICES = {
    "grok-4.5":        (2.00, 6.00),
    "grok-4-1-fast":   (0.20, 0.50),
    "grok-4.3":        (1.25, 2.50),
    "grok-4.20":       (2.00, 6.00),
}

def estimate_cost(model, in_tokens, out_tokens):
    in_p, out_p = PRICES[model]
    return (in_tokens * in_p + out_tokens * out_p) / 1_000_000

# 示例：每天 10 万输入 + 2 万输出（grok-4-1-fast）
print(estimate_cost("grok-4-1-fast", 100_000, 20_000))  # ≈ $0.03/天
```

**免费额度：**
```text
├── 新用户注册：$25 免费额度
├── Data Sharing 计划：每月最高 $150 额外额度
└── 无免费 API 层（纯按 token 计费）
```

---

> 🎯 **核心要点**：Grok API 三件事 — **① OpenAI 兼容（base_url: api.x.ai/v1）② 函数调用同构（改两行配置即用）③ 独家 X 搜索工具（舆情监控/实时分析）**。省钱秘诀：2M 超长文档用 4.1 Fast（$0.20），比 DeepSeek 同级还低。

**下一模块**：[05-Grok多智能体与Agent能力](05-Grok多智能体与Agent能力.md) / **返回总览**：[00-总览](00-Grok-API知识体系总览.md)
