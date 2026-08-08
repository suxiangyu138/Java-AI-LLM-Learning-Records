# 04 Agent 循环 v1：手写核心循环

> 定位：本阶段的主角——一个完整可运行的 Agent 循环，从请求到执行的每一步都亲手实现（2026-08 基准）

## 📚 目录

1. [循环的完整形态](#1-循环的完整形态)
2. [v1 完整代码（可直接运行）](#2-v1-完整代码可直接运行)
3. [循环六步拆解](#3-循环六步拆解)
4. [终止条件设计](#4-终止条件设计)
5. [为什么助手消息必须整体追加](#5-为什么助手消息必须整体追加)
6. [DeepSeek thinking 模式接入](#6-deepseek-thinking-模式接入)
7. [运行与验证](#7-运行与验证)

## 1. 循环的完整形态

```text
┌─────────────────────────────────────────────────────────┐
│  while 循环：                                             │
│                                                          │
│  ① 发送 messages + tools → 模型                          │
│  ② 模型返回 message（content 或 tool_calls）              │
│  ③ 把 assistant message 整体追加进 messages               │
│  ④ 有 tool_calls？                                       │
│     ├─ 否 → 输出 content，循环结束 ✅                     │
│     └─ 是 → 逐条解析 → 执行函数 → 结果追加为 tool 消息      │
│            → 回到 ①（下一轮）                             │
│  ⑤ 轮数超限/异常 → 兜底退出                              │
└─────────────────────────────────────────────────────────┘
```

## 2. v1 完整代码（可直接运行）

```python
"""agent_v1.py — 手写极简 Function-call Agent（零框架）"""
import json
import os
from dotenv import load_dotenv
from openai import OpenAI

load_dotenv()

client = OpenAI(
    api_key=os.getenv("DEEPSEEK_API_KEY"),
    base_url="https://api.deepseek.com",
)
MODEL = "deepseek-v4-flash"
MAX_TURNS = 10          # 最大循环轮数（兜底，防死循环）


# ─── ① 工具声明 ───────────────────────────────────────────
TOOLS = [
    {
        "type": "function",
        "function": {
            "name": "get_weather",
            "description": "查询指定城市的当前天气。当用户询问天气、气温、降雨时使用。",
            "parameters": {
                "type": "object",
                "properties": {
                    "city": {"type": "string", "description": "城市名，如 北京、Shanghai"},
                    "unit": {"type": "string", "enum": ["celsius", "fahrenheit"]},
                },
                "required": ["city"],
            },
        },
    },
]


# ─── ② 工具执行层（真函数）─────────────────────────────────
def get_weather(city: str, unit: str = "celsius") -> dict:
    """模拟天气查询（阶段 1 用假数据，阶段 7 换成真实 API）"""
    return {"city": city, "temperature": 22, "unit": unit, "condition": "sunny"}


def execute_tool(name: str, args: dict) -> str:
    """按名字分发执行，返回 JSON 字符串作为工具结果"""
    if name == "get_weather":
        result = get_weather(**args)
    else:
        result = {"error": f"未知工具: {name}"}
    return json.dumps(result, ensure_ascii=False)


# ─── ③ 核心循环 ────────────────────────────────────────────
def run_agent(user_message: str) -> str:
    messages = [{"role": "user", "content": user_message}]

    for turn in range(MAX_TURNS):
        # ① 请求模型
        response = client.chat.completions.create(
            model=MODEL,
            messages=messages,
            tools=TOOLS,
            tool_choice="auto",
        )
        msg = response.choices[0].message

        # ② 助手消息整体追加（含 tool_calls / reasoning_content）
        messages.append(msg)

        # ③ 没有工具调用 → 这就是最终回答
        if not msg.tool_calls:
            return msg.content or "（模型未返回内容）"

        # ④ 有工具调用 → 逐条执行并回传
        for tool_call in msg.tool_calls:
            name = tool_call.function.name
            try:
                args = json.loads(tool_call.function.arguments)
            except json.JSONDecodeError:
                args = {}
            result = execute_tool(name, args)
            messages.append({
                "role": "tool",
                "tool_call_id": tool_call.id,      # ★ 原样使用
                "content": result,
            })

    return "（已达最大轮数，循环终止）"


if __name__ == "__main__":
    print(run_agent("北京现在天气怎么样？"))
```

## 3. 循环六步拆解

| 步 | 代码位置 | 关键点 |
|:---:|---------|--------|
| ① 请求 | `chat.completions.create` | 每轮都带 `tools=TOOLS` |
| ② 追加助手消息 | `messages.append(msg)` | **整体追加对象**，不手动重建 |
| ③ 终止判断 | `if not msg.tool_calls` | 无工具调用 → 回答即最终答案 |
| ④ 解析参数 | `json.loads(...)` | 包 try/except（坏 JSON 兜底） |
| ⑤ 执行 | `execute_tool(name, args)` | 名字分发（05 篇升级为注册表） |
| ⑥ 回传 | 追加 tool 消息 | id 原样 + 结果 JSON 字符串 |

> 🎯 **核心要点**：整个 Agent 的"智能"只有两步——模型判断"该调哪个工具"（②③），你的代码执行并回传（④⑤⑥）。循环本身只是胶水。

## 4. 终止条件设计

| 条件 | 判断 | 作用 |
|------|------|------|
| 正常终止 | `finish_reason == "stop"` 且无 tool_calls | 模型给出最终回答 |
| 轮数兜底 | `for turn in range(MAX_TURNS)` | 防死循环（模型反复调工具不收敛） |
| 异常兜底 | try/except 包裹 create | 网络/限流错误向上抛或重试（06 篇） |

**轮数上限经验值**：简单工具 5-10 轮足够；复杂任务（多步搜索）20-25 轮。

> ⚠️ **死循环是手写 Agent 的第一事故**：模型可能"卡在"反复调用同一工具。轮数上限是保命索——生产版本还必须加成本熔断（见失败模式体系 06/08 篇）。

## 5. 为什么助手消息必须整体追加

```text
❌ 错误写法：messages.append({"role": "assistant", "content": "我要查天气"})
   → 丢失 tool_calls！模型下一轮不知道"自己已经要过工具"，协议断裂 → 400

✅ 正确写法：messages.append(msg)（response.choices[0].message 原对象）
   → 完整保留 role / content / tool_calls / reasoning_content
```

| 字段 | 丢失后果 |
|------|---------|
| `tool_calls` | 服务端校验失败（tool 消息找不到对应 assistant 调用） |
| `reasoning_content`（DeepSeek thinking） | 多轮后 400 |
| `tool_call.id` | 工具结果无法配对 |

## 6. DeepSeek thinking 模式接入

循环跑通后，加一行即可开启思考：

```python
response = client.chat.completions.create(
    model="deepseek-v4-pro",          # 或 v4-flash（支持 thinking）
    messages=messages,
    tools=TOOLS,
    tool_choice="auto",
    extra_body={"thinking_mode": "thinking"},   # 关键：SDK 透传
)

msg = response.choices[0].message
print("思考过程:", msg.reasoning_content)        # 新增字段（仅 thinking 模式有）

messages.append(msg)   # ★ 整体追加——reasoning_content 随之回传，不额外处理
```

| 要点 | 说明 |
|------|------|
| 启用 | `extra_body={"thinking_mode": "thinking"}`（值可为 `thinking` / `thinking_max`） |
| 字段 | `msg.reasoning_content`（思考内容，与 content 平级） |
| 回传 | 整体追加 `msg` 即可，**不要手动重建消息** |
| 性能 | thinking 模式更慢更贵——按需开启（复杂任务开，简单问答关） |

> ⚠️ **顺序坑**：先验证 thinking 关闭时循环全通，再开 thinking。两种模式混合排错 = 变量太多。

## 7. 运行与验证

```bash
python agent_v1.py
# 期望输出：北京现在天气怎么样？→ [模型调 get_weather] → 最终回答
```

**验证用例三连**：

| 用例 | 期望 |
|------|------|
| "北京现在天气怎么样？" | 触发工具 → 回答含天气数据 |
| "你好，你是谁？" | 不触发工具 → 直接回答 |
| "北京和上海天气对比？" | （并行）两次工具调用 → 对比回答 |

**调试提示**：如果结果不对，先打印响应原始 JSON（03 篇第 7 节）与 messages 追加历史（03 篇第 3 节）——99% 的问题在"消息没按铁律追加"。

> 🎯 **核心要点**：v1 循环只有 ~60 行，但它已经是一个**完整可用的 Agent**——理解它的每一行，你就理解了大模型 Agent 的最小本质。后续所有工程能力（注册表、重试、流式、记忆）都是在这个骨架上长肌肉。

---

**返回总览**：[00-阶段总览：手写极简Function-call Agent](00-阶段总览：手写极简Function-call%20Agent.md) / **上一模块**：[03-模型返回：tool_calls 解析](03-模型返回：tool_calls%20解析.md) / **下一模块**：[05-工具执行层：注册表与错误处理](05-工具执行层：注册表与错误处理.md)
