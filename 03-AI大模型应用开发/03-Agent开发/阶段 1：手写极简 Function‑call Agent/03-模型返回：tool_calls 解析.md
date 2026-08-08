# 03 模型返回：tool_calls 解析

> 定位：模型说"我要调工具"时返回了什么——tool_calls 结构解剖、消息追加铁律、并行调用（2026-08 基准）

## 📚 目录

1. [finish_reason：读懂模型的"结束语"](#1-finish_reason读懂模型的结束语)
2. [tool_calls 结构解剖](#2-tool_calls-结构解剖)
3. [消息追加铁律](#3-消息追加铁律)
4. [arguments 解析与防御](#4-arguments-解析与防御)
5. [并行调用：一次返回多个工具](#5-并行调用一次返回多个工具)
6. [思考链：thinking 与 reasoning_content](#6-思考链thinking-与-reasoning_content)
7. [调试利器：打印原始响应](#7-调试利器打印原始响应)

## 1. finish_reason：读懂模型的"结束语"

每次响应都带 `finish_reason`，它是循环的控制信号：

| 值 | 含义 | Agent 循环处理 |
|----|------|---------------|
| `"stop"` | 正常生成完毕（本轮没有工具调用） | 输出最终回答，**循环结束** |
| `"tool_calls"` | 模型要调工具 | 解析 tool_calls → 执行 → 回传 → 继续循环 |
| `"length"` | 输出被 max_tokens 截断 | 视为异常：重试或报告 |
| `"content_filter"` | 内容被过滤 | 报告（很少见） |

> 🎯 **核心要点**：`finish_reason == "tool_calls"` 是"模型要工具"，`"stop"` 是"对话可结束"——循环的终止条件就是这二者的判断。

## 2. tool_calls 结构解剖

当模型决定调工具时，响应长这样：

```json
{
  "choices": [{
    "message": {
      "role": "assistant",
      "content": null,
      "tool_calls": [
        {
          "id": "call_abc123",           // 唯一 ID，必须原样使用
          "type": "function",
          "function": {
            "name": "get_weather",       // 要调的工具名
            "arguments": "{\"city\": \"北京\", \"unit\": \"celsius\"}"  // ★ JSON 字符串，不是对象！
          }
        }
      ]
    },
    "finish_reason": "tool_calls"
  }]
}
```

| 字段 | 注意点 |
|------|--------|
| `tool_calls[].id` | 工具结果的"回执凭证"，**逐字符原样使用** |
| `function.name` | 与 tools 声明中的 name 对应 |
| `function.arguments` | **JSON 字符串**（双引号内），必须 `json.loads` 解析 |
| `content` | 此时通常为 `null`（模型不解释，直接调） |

## 3. 消息追加铁律

调完工具后再次请求时，**消息顺序必须完整追加**：

```text
messages 顺序（一次完整工具调用后）：
1. {"role": "system", ...}             ← 系统提示
2. {"role": "user", ...}               ← 用户问题
3. {"role": "assistant", "tool_calls": [...]}   ← ★ 模型调用意图（原样追加）
4. {"role": "tool", "tool_call_id": "call_abc123", "content": "..."}  ← ★ 工具结果
```

**三条铁律**：

| # | 铁律 | 违反后果 |
|:---:|------|---------|
| 1 | assistant 消息（含 tool_calls）必须追加 | 400 报错 / 模型不知道自己在调工具 |
| 2 | tool 消息必须带原样的 `tool_call_id` | `BadRequestError`：找不到对应调用 |
| 3 | 每条 tool_calls 必须有对应 tool 消息 | 缺失对应关系直接报错 |

> ⚠️ **tool_call_id 造假现场**：手写循环最常见的 bug 是拼接 id（如 `"call_" + name`）——**id 是服务端生成的，只能原样取用，绝不重建**。

## 4. arguments 解析与防御

arguments 是 JSON **字符串**，解析必须防御：

```python
import json

def parse_arguments(raw: str) -> dict:
    """解析模型返回的参数字符串，失败返回空 dict（由调用方决定策略）"""
    try:
        args = json.loads(raw)
        if not isinstance(args, dict):
            return {}
        return args
    except json.JSONDecodeError as e:
        print(f"[警告] arguments 解析失败: {e} | 原文: {raw}")
        return {}
```

| 防御点 | 说明 |
|--------|------|
| 坏 JSON | 模型偶发输出被截断/带杂质 → try/except |
| 非 dict | 模型返回了数组/字符串 → 类型校验 |
| 多余字段 | schema 外的字段 → 执行层过滤（**绝不 kwargs 全量透传**） |

## 5. 并行调用：一次返回多个工具

模型可以在一次响应里返回 0-N 个 tool_calls（如"北京和上海天气"各调一次）：

```python
# 模型返回 2 个 tool_calls 的场景
for tool_call in msg.tool_calls:
    name = tool_call.function.name
    args = parse_arguments(tool_call.function.arguments)
    result = execute(name, args)          # 05 篇的实现
    messages.append({
        "role": "tool",
        "tool_call_id": tool_call.id,     # 各自配对，一一对应
        "content": json.dumps(result, ensure_ascii=False)
    })
```

**并行调用的两个纪律**：

| 纪律 | 原因 |
|------|------|
| 工具结果按 id 一一配对 | 顺序错乱 = 张冠李戴 |
| 有状态工具串行执行 | 并行执行会竞争（如"先建再删"） |

> 💡 只读/无依赖工具可并行（省延迟）；有状态/有依赖的工具**串行**——阶段 1 全部串行执行，最简单可靠。

## 6. 思考链：thinking 与 reasoning_content

DeepSeek V4 开 thinking 模式时，响应多一个 `reasoning_content` 字段（模型先思考后回答/调工具）：

| 要点 | 说明 |
|------|------|
| 启用方式 | `extra_body={"thinking_mode": "thinking"}`（OpenAI SDK 透传） |
| 字段位置 | `message.reasoning_content`（与 `content` 平级） |
| 回传铁律 | **多轮工具调用时必须把 reasoning_content 一起回传**，否则 400 |
| 存储 | 完整保存 assistant 消息对象（含 reasoning_content），不重建 |

```python
response = client.chat.completions.create(
    model="deepseek-v4-pro",
    messages=messages,
    tools=tools,
    extra_body={"thinking_mode": "thinking"},   # SDK 透传 DeepSeek 专属参数
)

msg = response.choices[0].message
# msg 对象整体追加回 messages（自动携带 reasoning_content）
messages.append(msg)
```

> ⚠️ **阶段 1 建议**：先把 thinking 关掉（默认 non-thinking）跑通主循环；thinking 模式留到循环跑通后再开。少一个变量，多一分可控。

## 7. 调试利器：打印原始响应

协议期最常见的困惑是"模型到底返回了什么"——**打印原始 JSON**：

```python
from openai import OpenAI
import json

def dump_response(response) -> None:
    """调试用：把响应序列化打印（生产环境不要打全量，可能含敏感数据）"""
    model_dump = response.model_dump() if hasattr(response, "model_dump") else response
    print(json.dumps(model_dump, ensure_ascii=False, indent=2))

# 使用：循环每一步后调用
response = client.chat.completions.create(...)
dump_response(response)
```

**调试三板斧**：

```text
① 打印原始响应 JSON（不经过任何封装）
② 打印追加后的 messages（确认顺序与字段完整）
③ 打印每次循环轮次与 finish_reason（确认终止逻辑）
```

> 🎯 **核心要点**：tool_calls 解析 = "读协议、原样回传"。三件套记忆：**id 原样、arguments 是字符串、assistant 消息必须追加**——三个都做对，循环就通了。

---

**返回总览**：[00-阶段总览：手写极简Function-call Agent](00-阶段总览：手写极简Function-call%20Agent.md) / **上一模块**：[02-工具声明：tools 与 JSON Schema](02-工具声明：tools%20与%20JSON%20Schema.md) / **下一模块**：[04-Agent 循环 v1：手写核心循环](04-Agent%20循环%20v1：手写核心循环.md)
