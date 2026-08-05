# 04 - Kimi API 开发实战

> 🎯 Kimi API 完全兼容 OpenAI 协议 — 改 base_url 即可接入。本章覆盖快速接入、深度思考（reasoning_content）、Function Calling 循环、上下文缓存、结构化输出、视觉输入六大实战场景

---

## 目录

1. [快速接入：OpenAI 兼容三行配置](#1-快速接入openai-兼容三行配置)
2. [深度思考：reasoning_content 字段](#2-深度思考reasoning_content-字段)
3. [Function Calling 完整循环](#3-function-calling-完整循环)
4. [上下文缓存：省 75% 成本](#4-上下文缓存省-75-成本)
5. [结构化输出：JSON Schema](#5-结构化输出json-schema)
6. [视觉输入：图片理解](#6-视觉输入图片理解)
7. [生产架构与安全清单](#7-生产架构与安全清单)

---

## 1. 快速接入：OpenAI 兼容三行配置

```python
# ① 安装（官方 OpenAI SDK 即可）
# pip install openai

# ② 配置（只需改 base_url 和 api_key）
from openai import OpenAI

client = OpenAI(
    api_key=os.getenv("MOONSHOT_API_KEY"),   # platform.kimi.com 创建
    base_url="https://api.moonshot.cn/v1"     # 国内端点
    # base_url="https://api.moonshot.ai/v1"   # 国际端点
)

# ③ 调用
response = client.chat.completions.create(
    model="kimi-k3",                          # kimi-k3 / kimi-k2.6 / kimi-k2.7-code
    messages=[{"role": "user", "content": "用一句话解释什么是 MoE"}]
)
print(response.choices[0].message.content)
```

```javascript
// Node.js 同样兼容
import OpenAI from "openai";
const client = new OpenAI({
  apiKey: process.env.MOONSHOT_API_KEY,
  baseURL: "https://api.moonshot.cn/v1"
});
```

> ⚠️ **安全**：API Key 仅限服务端使用，严禁硬编码或暴露在浏览器前端。

---

## 2. 深度思考：reasoning_content 字段

Kimi 的思考内容与最终回答分离返回：

```python
# ① 流式获取思考 + 回答
response = client.chat.completions.create(
    model="kimi-k3",                          # 思考默认开启
    messages=[{"role": "user", "content": "解这道数学题：x²+2x-3=0"}],
    stream=True
)

for chunk in response:
    delta = chunk.choices[0].delta
    if delta.reasoning_content:               # ★ 思考内容
        print(f"[思考] {delta.reasoning_content}", end="")
    if delta.content:                         # ★ 最终回答
        print(f"[回答] {delta.content}", end="")
```

**关键注意点：**

| 注意 | 说明 |
|------|------|
| **思考不可关闭** | K3 的 `reasoning_effort` 实测仅支持 max 档 |
| **多轮必须回传思考** | 上一轮 assistant 消息**必须原样回传**（含 reasoning_content），否则后续输出质量不稳定 |
| **解析只读 content** | 结构化解析**切勿解析 reasoning_content** |
| **耗时长** | 思考 token 可能占输出的 73% → 流式返回 + 客户端超时按分钟级设置 |

```python
# ② 多轮对话正确姿势（完整回传上一轮 assistant 消息）
messages.append({
    "role": "assistant",
    "content": prev_content,
    "reasoning_content": prev_reasoning     # ★ 必须带上！
})
```

---

## 3. Function Calling 完整循环

```python
# ① 定义工具（最多 128 个）
tools = [{
    "type": "function",
    "function": {
        "name": "get_weather",
        "description": "查询指定城市的实时天气",
        "parameters": {
            "type": "object",
            "properties": {
                "city": {"type": "string", "description": "城市名"}
            },
            "required": ["city"]
        }
    }
}]

# ② 标准 Agent 循环（模型不执行工具！）
messages = [{"role": "user", "content": "北京今天天气怎么样？"}]

for turn in range(8):                        # ★ 硬性轮次上限，防死循环
    response = client.chat.completions.create(
        model="kimi-k3",
        messages=messages,
        tools=tools,
        tool_choice="auto"                   # auto/none/required
    )
    msg = response.choices[0].message

    if msg.tool_calls:                       # ③ 模型要调工具
        messages.append(msg)                 # ★ 必须追加完整 assistant 消息（含 tool_calls）
        for tc in msg.tool_calls:
            result = execute_tool(tc.function.name, tc.function.arguments)  # ④ 服务端执行
            messages.append({                # ⑤ 追加 tool 结果
                "role": "tool",
                "tool_call_id": tc.id,       # ★ tool_call_id 必须匹配
                "content": result
            })
    else:                                    # ⑥ 模型停止调工具
        print(msg.content)
        break
```

**Function Calling 安全清单：**

| 安全项 | 做法 |
|--------|------|
| **Allowlist 分发** | 只执行白名单内的工具，禁止 eval/动态导入 |
| **Schema 二次校验** | 工具参数在服务端再次验证 |
| **轮次上限** | 硬性设置（如 8 轮），防死循环 |
| **敏感操作审批** | 删除/支付类操作人工审批 |
| **工具结果限大小** | 限制返回结果，防上下文爆炸 |
| **日志脱敏** | 工具参数中的敏感信息脱敏 |

**流式 + 工具调用注意：** 流式模式下 `tool_calls` 参数按 index 分片累积 — **必须等分片收集完整后再解析 JSON**，不能从第一个分片就执行。

---

## 4. 上下文缓存：省 75% 成本

```text
Kimi 上下文缓存自动开启，无需任何请求参数：
├── 重复的长前缀（字节级完全一致）→ 命中缓存
├── 命中价远低于未命中价（约 9 倍差距）
└── K2.6 前缀缓存命中后输入成本降约 75%
```

```python
# 命中量查看（三个接口字段不同）
# Chat Completions：
usage.prompt_tokens_details.cached_tokens
# Responses：
usage.input_tokens_details.cached_tokens
# Messages（Anthropic 兼容）：
usage.cache_read_input_tokens

# 缓存最大化策略：
# ① 保持 system prompt 稳定（开头长前缀一致 → 命中率高）
# ② 知识库内容放前面，动态内容放后面
# ③ 缓存写入是异步的 → 紧接的立即重复请求可能未命中
```

**成本实测（K3）：** 3.3 万 token 知识库前缀 — 首次约 $0.099（未命中 $3/百万），命中后约 $0.011（$0.30/百万）→ **省 9 倍**。

---

## 5. 结构化输出：JSON Schema

```python
# Chat Completions 严格 JSON Schema 模式
response = client.chat.completions.create(
    model="kimi-k3",
    messages=[{"role": "user", "content": "提取订单信息：2026年8月1日，用户张三，购买3台MacBook Pro"}],
    response_format={
        "type": "json_schema",
        "json_schema": {
            "name": "order_extraction",
            "strict": True,                # ★ 严格模式
            "schema": {
                "type": "object",
                "properties": {
                    "date":     {"type": "string"},
                    "customer": {"type": "string"},
                    "product":  {"type": "string"},
                    "quantity": {"type": "integer"}
                },
                "required": ["date", "customer", "product", "quantity"]
            }
        }
    }
)
print(response.choices[0].message.content)
# → {"date":"2026-08-01","customer":"张三","product":"MacBook Pro","quantity":3}
```

> ⚠️ **Messages 端点（Anthropic 兼容）不支持结构化输出** — 字段被静默忽略返回自由文本。需要严格 JSON 时用 Chat Completions 或 Responses。

---

## 6. 视觉输入：图片理解

```python
import base64

# 图片转 base64（★ API 不接受公共图片 URL，必须 base64 或上传）
with open("screenshot.png", "rb") as f:
    img_b64 = base64.b64encode(f.read()).decode()

response = client.chat.completions.create(
    model="kimi-k3",                        # 视觉需 K3（或 K2.6）
    messages=[{
        "role": "user",
        "content": [
            {"type": "text", "text": "这个页面的布局有什么问题？"},
            {"type": "image_url",
             "image_url": {"url": f"data:image/png;base64,{img_b64}"}}
        ]
    }]
)
print(response.choices[0].message.content)
```

**视觉典型场景：** 截图调试（定位 UI Bug 给 CSS 修复）、图表理解、文档 OCR。

---

## 7. 生产架构与安全清单

```text
生产架构（Kimi 官方推荐）：

前端 → 后端 → Kimi API
        │
        ├── 输入校验（参数验证/防注入）
        ├── 鉴权（用户身份/权限）
        ├── 限流（防滥用）
        ├── 日志（审计）
        │
        ←── 后端 ←────────────
        ├── 解析（结构化输出）
        ├── 工具执行（allowlist 分发）
        ├── 安全校验（工具结果二次验证）
        ├── 存储（会话/记忆）
        └── 保护（脱敏/审批）

原则：Kimi 负责推理与生成；
     决策、验证、执行、存储与保护由你的后端负责。
```

**完整安全检查清单：**

| # | 检查项 |
|---|--------|
| 1 | API Key 环境变量存储，不硬编码 |
| 2 | 多轮对话完整回传 reasoning_content |
| 3 | Function Calling 硬性轮次上限（8 轮） |
| 4 | 工具 allowlist 分发 + schema 二次校验 |
| 5 | 敏感操作人工审批 + 幂等键 |
| 6 | 工具结果大小限制 + 日志脱敏 |
| 7 | 流式 tool_calls 分片收集完整再解析 |
| 8 | 结构化解析只读 content，不读 reasoning_content |
| 9 | 客户端超时按分钟级设置（思考耗时长） |

---

> 🎯 **核心要点**：Kimi API 六大实战 — **① OpenAI 兼容（改 base_url）② 深度思考（reasoning_content 必须回传）③ Function Calling 循环（模型不执行工具，8 轮上限）④ 上下文缓存（自动开启，前缀一致省 75%）⑤ JSON Schema 严格模式（Messages 端点不支持）⑥ 视觉 base64（不接受公共 URL）**。生产原则一句话：**Kimi 负责推理，你的后端负责决策/验证/执行/保护**。

**下一模块**：[05-Kimi-Agent与智能体集群](05-Kimi-Agent与智能体集群.md) / **返回总览**：[00-总览](00-Kimi知识体系总览.md)
