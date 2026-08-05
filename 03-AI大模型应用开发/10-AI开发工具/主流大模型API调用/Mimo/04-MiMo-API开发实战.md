# 04 - MiMo API 开发实战

> 🎯 MiMo API 完全兼容 OpenAI 协议 — 改 base_url 即用。本章覆盖快速接入、对话/流式、函数调用、长文本调用、价格计算与 Token Plan

---

## 目录

1. [快速接入：OpenAI 兼容三行配置](#1-快速接入openai-兼容三行配置)
2. [对话与流式输出](#2-对话与流式输出)
3. [函数调用](#3-函数调用)
4. [长文本调用实战](#4-长文本调用实战)
5. [价格计算与 Token Plan](#5-价格计算与-token-plan)

---

## 1. 快速接入：OpenAI 兼容三行配置

```python
# ① 安装（官方 OpenAI SDK）
# pip install openai

# ② 配置（只需改 base_url 和 api_key）
from openai import OpenAI

client = OpenAI(
    api_key=os.getenv("XIAOMI_API_KEY"),         # 小米开放平台创建
    base_url="https://api.xiaomimimo.com/v1"      # ★ MiMo 端点
)

# ③ 调用
response = client.chat.completions.create(
    model="MiMo-V2.5-Pro",                        # MiMo-V2.5-Pro / MiMo-V2.5
    messages=[{"role": "user", "content": "解释一下什么是 1M 上下文"}]
)
print(response.choices[0].message.content)
```

> 💡 也可通过火山引擎等第三方平台调用（OpenAI 兼容协议，改 base_url 即用）。

---

## 2. 对话与流式输出

```python
# ① 基础对话（带系统提示）
response = client.chat.completions.create(
    model="MiMo-V2.5-Pro",
    max_tokens=4096,
    messages=[
        {"role": "system", "content": "你是 Java 专家，回答简洁"},
        {"role": "user", "content": "解释 Java 虚拟线程"}
    ]
)
print(response.choices[0].message.content)

# ② 流式输出
stream = client.chat.completions.create(
    model="MiMo-V2.5-Pro",
    messages=[{"role": "user", "content": "写一段快速排序"}],
    stream=True
)
for chunk in stream:
    if chunk.choices[0].delta.content:
        print(chunk.choices[0].delta.content, end="", flush=True)
```

---

## 3. 函数调用

```python
# 函数调用（OpenAI 完全同构）
tools = [{
    "type": "function",
    "function": {
        "name": "search_docs",
        "description": "搜索企业文档库",
        "parameters": {
            "type": "object",
            "properties": {
                "query": {"type": "string", "description": "搜索关键词"},
                "limit": {"type": "integer", "description": "返回条数"}
            },
            "required": ["query"]
        }
    }
}]

messages = [{"role": "user", "content": "查一下公司的差旅报销制度"}]
response = client.chat.completions.create(
    model="MiMo-V2.5-Pro",
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
            "tool_call_id": tc.id,                # ★ 必须匹配
            "content": result
        })
    response2 = client.chat.completions.create(model="MiMo-V2.5-Pro", messages=messages, tools=tools)
    print(response2.choices[0].message.content)
```

> 💡 **V2.5 改进**：并行 tool calls 的 JSON 格式合规性提升 — 多工具并行调用更稳定。

---

## 4. 长文本调用实战

```python
# ① 直接塞入长文档（1M 上下文，无需 RAG 分块）
with open("annual_report.txt", "r", encoding="utf-8") as f:
    report_text = f.read()          # 可到 50 万 token

response = client.chat.completions.create(
    model="MiMo-V2.5-Pro",
    max_tokens=8192,
    messages=[{
        "role": "user",
        "content": f"请分析这份年度报告（约{len(report_text)//1000}千字）：\n1. 营收变化趋势\n2. 风险点\n3. 明年建议\n\n报告内容：\n{report_text}"
    }]
)
print(response.choices[0].message.content)

# ② 大海捞针式检索（长文本中找信息）
response = client.chat.completions.create(
    model="MiMo-V2.5-Pro",
    messages=[{
        "role": "user",
        "content": f"在这份 50 万字的合同中，找到关于'数据安全'的所有条款并总结。\n\n合同内容：{contract_text}"
    }]
)
# 800K 范围检索准确率 95%+ → 长文定位可靠
```

---

## 5. 价格计算与 Token Plan

```python
# MiMo 定价（¥/百万 tokens，2026.05.27 永久降价后）
PRICES = {
    "MiMo-V2.5-Pro": {"input": 3.00, "output": 6.00, "cache_hit": 0.025},
    "MiMo-V2.5":     {"input": 1.00, "output": 2.00, "cache_hit": 0.020},
}

def estimate_cost(model, in_tokens, out_tokens, cache_hit_tokens=0):
    p = PRICES[model]
    hit_cost = cache_hit_tokens * p["cache_hit"] / 1_000_000
    miss_cost = (in_tokens - cache_hit_tokens) * p["input"] / 1_000_000
    out_cost = out_tokens * p["output"] / 1_000_000
    return hit_cost + miss_cost + out_cost

# 示例：500K 长文档（80% 缓存命中）+ 20K 输出
cost = estimate_cost("MiMo-V2.5-Pro", 500_000, 20_000, 400_000)
print(f"成本: ¥{cost:.4f}")
# 缓存命中 40 万 × 0.025 + 未命中 10 万 × 3 + 输出 2 万 × 6
# = ¥0.01 + ¥0.30 + ¥0.12 = ¥0.43/次
```

**Token Plan 订阅：**

| 套餐 | 价格 | 说明 |
|------|:---:|------|
| Lite | ¥39/月 | 基础额度（降价后提升 5-8 倍） |
| Max | ¥659/月 | 最高额度 |

---

> 🎯 **核心要点**：MiMo API 三件事 — **① OpenAI 兼容（改 base_url 即用）② 长文本直接塞入（1M 上下文 + 800K 检索 95%）③ 缓存命中极便宜（¥0.025/百万）**。长文档场景的最佳姿势：整库/整文直接传入 + 稳定前缀触发缓存命中 + 成本比国际旗舰低 10-100 倍。

**下一模块**：[05-长文本与RAG场景实战](05-长文本与RAG场景实战.md) / **返回总览**：[00-总览](00-Mimo-API知识体系总览.md)
