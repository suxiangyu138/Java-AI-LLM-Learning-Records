# 04 - GLM API 开发实战

> 🎯 GLM API 完全兼容 OpenAI 协议 — 改 base_url 即用。本章覆盖快速接入、对话/流式/结构化输出、Function Calling、GLM-5 的思维链调用、价格计算

---

## 目录

1. [快速接入：OpenAI 兼容三行配置](#1-快速接入openai-兼容三行配置)
2. [基础对话与流式输出](#2-基础对话与流式输出)
3. [结构化输出与 Function Calling](#3-结构化输出与-function-calling)
4. [思维链调用](#4-思维链调用)
5. [价格计算与预算控制](#5-价格计算与预算控制)
6. [错误处理速查](#6-错误处理速查)

---

## 1. 快速接入：OpenAI 兼容三行配置

```python
# ① 安装（官方 OpenAI SDK 即可）
# pip install openai

# ② 配置（只需改 base_url 和 api_key）
from openai import OpenAI

client = OpenAI(
    api_key=os.getenv("ZHIPU_API_KEY"),      # open.bigmodel.cn 控制台创建
    base_url="https://open.bigmodel.cn/api/paas/v4"  # ★ 智谱端点
)

# ③ 调用
response = client.chat.completions.create(
    model="glm-5",                            # glm-5 / glm-5.1 / glm-5-turbo / glm-5.2
    messages=[{"role": "user", "content": "解释一下 DSA 稀疏注意力"}]
)
print(response.choices[0].message.content)
```

```javascript
// Node.js 同样兼容
import OpenAI from "openai";
const client = new OpenAI({
  apiKey: process.env.ZHIPU_API_KEY,
  baseURL: "https://open.bigmodel.cn/api/paas/v4"
});
```

> ⚠️ **安全**：API Key 仅限服务端使用，环境变量存储，严禁硬编码。

---

## 2. 基础对话与流式输出

```python
# ① 基础对话
response = client.chat.completions.create(
    model="glm-5",
    max_tokens=4096,
    messages=[
        {"role": "system", "content": "你是 Java 专家，回答简洁"},
        {"role": "user", "content": "解释 Spring IoC"}
    ]
)
print(response.choices[0].message.content)

# ② 流式输出（SSE 打字机效果）
stream = client.chat.completions.create(
    model="glm-5",
    max_tokens=4096,
    messages=[{"role": "user", "content": "写一段快速排序的 Go 代码"}],
    stream=True
)
for chunk in stream:
    if chunk.choices[0].delta.content:
        print(chunk.choices[0].delta.content, end="", flush=True)

# ③ 多轮对话
messages = [
    {"role": "system", "content": "你是助手"},
    {"role": "user", "content": "什么是 RAG？"},
    {"role": "assistant", "content": "RAG 是检索增强生成..."},
    {"role": "user", "content": "举一个实际应用例子"}   # 直接追加
]
```

---

## 3. 结构化输出与 Function Calling

```python
# ① 结构化输出（JSON 模式）
response = client.chat.completions.create(
    model="glm-5",
    messages=[{"role": "user", "content": "提取订单信息：8月1日张三购买3台MacBook"}],
    response_format={"type": "json_object"}   # 强制 JSON
)
import json
data = json.loads(response.choices[0].message.content)
# {"customer": "张三", "product": "MacBook", "quantity": 3, "date": "8月1日"}

# ② Function Calling（GLM 原生支持）
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
    model="glm-5",
    messages=messages,
    tools=tools
)
msg = response.choices[0].message

if msg.tool_calls:                              # ③ 模型要调工具
    messages.append(msg)
    for tc in msg.tool_calls:
        result = execute_tool(tc.function.name, tc.function.arguments)
        messages.append({
            "role": "tool",
            "tool_call_id": tc.id,              # ★ 必须匹配
            "content": result
        })
    # ④ 继续
    response2 = client.chat.completions.create(model="glm-5", messages=messages, tools=tools)
```

---

## 4. 思维链调用

```python
# GLM-5 的核心卖点：结构化推理/思维链

# ① 思考提示词（激发思维链）
response = client.chat.completions.create(
    model="glm-5",
    max_tokens=8192,
    messages=[{
        "role": "user",
        "content": """请逐步分析这个问题，最后给出结论：
        一家公司月营收 500 万，成本 350 万，税率 20%。
        ① 利润多少？ ② 税后利润多少？ ③ 利润率多少？
        请展示每一步计算过程。"""
    }]
)
print(response.choices[0].message.content)

# ② 复杂 Agent 任务（多步推理）
response = client.chat.completions.create(
    model="glm-5",
    max_tokens=16384,                          # 长程任务给足输出
    messages=[{
        "role": "user",
        "content": "分析这个代码库的性能瓶颈并给出优化方案（需要先分析结构→定位热点→设计优化）"
    }]
)
```

**GLM-5 思维链使用建议：**
| 场景 | 提示词策略 |
|------|-----------|
| 数学/金融计算 | "请展示每一步计算过程" |
| 代码审查 | "按严重程度逐步审查" |
| 法律/合同分析 | "先提取关键条款，再逐条分析风险" |
| Agent 任务 | "先规划步骤，再逐步执行" |

---

## 5. 价格计算与预算控制

```python
# GLM-5 定价：¥4/百万输入 + ¥18/百万输出
# GLM-5.2 定价：¥8/百万输入 + ¥28/百万输出（缓存 ¥2）

def estimate_cost(model: str, input_tokens: int, output_tokens: int) -> float:
    prices = {
        "glm-5":     (4, 18),
        "glm-5.1":   (6, 24),
        "glm-5-turbo": (5, 22),
        "glm-5.2":   (8, 28),
    }
    in_p, out_p = prices.get(model, (4, 18))
    return (input_tokens * in_p + output_tokens * out_p) / 1_000_000

# 示例：每天 10 万输入 + 2 万输出（glm-5）
print(estimate_cost("glm-5", 100_000, 20_000))   # ≈ ¥0.76/天
```

---

## 6. 错误处理速查

| 错误码 | 含义 | 处理 |
|--------|------|------|
| 400 | 参数错误 | 检查模型 ID / 参数格式 |
| 401 | API Key 无效 | 检查 ZHIPU_API_KEY |
| 429 | 限流 | 指数退避重试（1s→2s→4s） |
| 500 | 服务端错误 | 重试 |
| 1301 | 上下文超限 | 截断/压缩上下文 |

---

> 🎯 **核心要点**：GLM API 接入三件事 — **① OpenAI 兼容（改 base_url 为 open.bigmodel.cn/api/paas/v4）② 模型 ID：glm-5/glm-5.1/glm-5-turbo/glm-5.2 ③ 思维链提示词发挥 GLM-5 的推理强项**。Function Calling 与 OpenAI 完全同构（tool_calls → 执行 → tool_result 回传）。

**下一模块**：[05-GLM-Agent与长程任务](05-GLM-Agent与长程任务.md) / **返回总览**：[00-总览](00-GLM-API知识体系总览.md)
