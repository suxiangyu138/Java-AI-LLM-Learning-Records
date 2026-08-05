# Anthropic Messages API 协议基础
> Anthropic Messages API 是 Anthropic 定义的 RESTful 接口格式，已成为第三方服务商的事实兼容标准

## 📚 目录
1. [请求格式](#1-请求格式)
2. [响应格式](#2-响应格式)
3. [流式输出 (SSE)](#3-流式输出-sse)
4. [Tool Use 格式](#4-tool-use-格式)
5. [认证方式](#5-认证方式)

---

## 1. 请求格式

### 1.1 基本请求结构

```json
POST https://api.anthropic.com/v1/messages
Content-Type: application/json
x-api-key: sk-ant-xxxxxxxxxx
anthropic-version: 2023-06-01
```

```json
{
  "model": "claude-sonnet-5-20251001",
  "max_tokens": 1024,
  "messages": [
    {
      "role": "user",
      "content": "Hello, Claude!"
    }
  ]
}
```

### 1.2 完整请求字段

| 字段 | 类型 | 必需 | 说明 |
|------|------|:---:|------|
| `model` | string | ✅ | 模型名称 |
| `messages` | array | ✅ | 对话消息数组 |
| `max_tokens` | int | ⚠️ | 最大输出 token（部分兼容服务改为可选） |
| `system` | string/array | ❌ | 系统提示词（Anthropic 特有，非 `messages` 内） |
| `temperature` | float | ❌ | 采样温度 (0-2)，默认 1.0 |
| `top_p` | float | ❌ | 核采样，默认 1.0 |
| `top_k` | int | ❌ | Top-K 采样 |
| `stop_sequences` | array | ❌ | 停止词数组 |
| `stream` | bool | ❌ | 是否流式输出 |
| `metadata` | object | ❌ | 用户标识等元数据 |
| `tools` | array | ❌ | 工具定义数组 |
| `tool_choice` | object | ❌ | 工具选择策略 |

### 1.3 消息格式

每条消息包含 `role` 和 `content`：

```json
{"role": "user", "content": "Hello"}
{"role": "assistant", "content": "Hi there!"}
```

**多内容块（Multipart Content）**：

```json
{
  "role": "user",
  "content": [
    {"type": "text", "text": "What's in this image?"},
    {"type": "image", "source": {"type": "base64", "media_type": "image/jpeg", "data": "..."}},
    {"type": "text", "text": "Also describe the next one:"},
    {"type": "image", "source": {"type": "url", "url": "https://..."}}
  ]
}
```

> 💡 **兼容要点**：第三方服务通常将 `system` 参数映射为内部 system prompt 或拼接为首条 user 消息。部分服务不支持 `top_k`、`metadata` 等字段。

---

## 2. 响应格式

### 2.1 非流式响应

```json
{
  "id": "msg_01ABCDEF123456",
  "type": "message",
  "role": "assistant",
  "content": [
    {
      "type": "text",
      "text": "Hello! How can I help you today?"
    }
  ],
  "model": "claude-sonnet-5-20251001",
  "stop_reason": "end_turn",
  "stop_sequence": null,
  "usage": {
    "input_tokens": 10,
    "output_tokens": 12
  }
}
```

### 2.2 响应字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 消息唯一 ID |
| `type` | string | 固定为 `"message"` |
| `role` | string | 固定为 `"assistant"` |
| `content` | array | Content block 数组 |
| `model` | string | 实际使用的模型 |
| `stop_reason` | string | 停止原因：`end_turn` / `max_tokens` / `stop_sequence` / `tool_use` |
| `stop_sequence` | string/null | 触发停止的序列 |
| `usage.input_tokens` | int | 输入 token 数 |
| `usage.output_tokens` | int | 输出 token 数 |

### 2.3 Content Block 类型

| 类型 | 说明 | 包含字段 |
|------|------|---------|
| `text` | 文本内容 | `text` |
| `tool_use` | 工具调用 | `id`, `name`, `input` |
| `tool_result` | 工具结果 | `tool_use_id`, `content`, `is_error` |
| `thinking` | 思考过程 | `thinking`, `signature`（仅 Claude） |

---

## 3. 流式输出 (SSE)

### 3.1 事件类型

Anthropic 流式协议使用 Server-Sent Events，包含以下事件：

| 事件类型 | 说明 | 关键字段 |
|---------|------|---------|
| `message_start` | 消息开始 | `message.id`, `message.model` |
| `content_block_start` | Content block 开始 | `index`, `content_block.type` |
| `content_block_delta` | Content block 增量 | `index`, `delta.type`, `delta.text/thinking` |
| `content_block_stop` | Content block 结束 | `index` |
| `message_delta` | 消息增量 | `stop_reason`, `usage.output_tokens` |
| `message_stop` | 消息结束 | （无额外数据） |
| `ping` | 心跳保活 | （间隔约 5 分钟） |

### 3.2 流式示例

```text
event: message_start
data: {"type":"message_start","message":{"id":"msg_01...","type":"message","role":"assistant","content":[],"model":"claude-sonnet-5-20251001","stop_reason":null,"stop_sequence":null,"usage":{"input_tokens":15,"output_tokens":1}}}

event: content_block_start
data: {"type":"content_block_start","index":0,"content_block":{"type":"text","text":""}}

event: content_block_delta
data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"Hello"}}

event: content_block_delta
data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"!"}}

event: content_block_stop
data: {"type":"content_block_stop","index":0}

event: message_delta
data: {"type":"message_delta","delta":{"stop_reason":"end_turn","stop_sequence":null},"usage":{"output_tokens":12}}

event: message_stop
data: {"type":"message_stop"}
```

> 💡 **兼容要点**：第三方服务的流式格式可能略有差异。部分服务（如 DeepSeek）将 `thinking` block 映射为 `reasoning_content` 字段。建议使用 SDK 层的 stream 解析，而非手动解析 SSE。

---

## 4. Tool Use 格式

### 4.1 工具定义

```json
{
  "tools": [
    {
      "name": "get_weather",
      "description": "获取指定城市的当前天气",
      "input_schema": {
        "type": "object",
        "properties": {
          "location": {
            "type": "string",
            "description": "城市名称"
          }
        },
        "required": ["location"]
      }
    }
  ],
  "tool_choice": {"type": "auto"}
}
```

### 4.2 Tool Choice 策略

| 策略 | 说明 |
|------|------|
| `{"type": "auto"}` | 模型自行决定是否调用工具（默认） |
| `{"type": "any"}` | 模型必须调用工具（至少一个） |
| `{"type": "tool", "name": "xxx"}` | 强制调用指定工具 |

### 4.3 工具调用与结果

**模型返回的 tool_use block**：

```json
{
  "type": "tool_use",
  "id": "toolu_01ABCDEF",
  "name": "get_weather",
  "input": {"location": "北京"}
}
```

**用户提交的 tool_result block**：

```json
{
  "role": "user",
  "content": [
    {
      "type": "tool_result",
      "tool_use_id": "toolu_01ABCDEF",
      "content": "北京当前气温 28°C，晴"
    }
  ]
}
```

> 💡 **兼容要点**：并非所有兼容服务都支持 Tool Use。DeepSeek V3/V4、OpenAI 兼容端点支持；部分轻量服务可能不支持。检查服务商文档确认。

---

## 5. 认证方式

### 5.1 Anthropic 原生

```
x-api-key: sk-ant-xxxxxxxxx
anthropic-version: 2023-06-01
```

### 5.2 兼容服务通用

多数兼容服务采用简化认证：

```
Authorization: Bearer sk-xxxxxxxxx
```

或

```
x-api-key: sk-xxxxxxxxx
```

> 💡 部分网关（如 one-api）支持同时接受 `x-api-key`、`Authorization` 或自定义 Header。

---

> 🎯 **核心要点**：Anthropic Messages API 的核心差异在于 `system` 独立于 `messages`、`content` 为 block 数组、以及独特的 SSE 流式事件体系。兼容服务通常简化或映射这些特性，SDK 级别的 base_url 切换是最佳实践。

---

**下一模块**：[02-主流兼容服务商与配置](02-主流兼容服务商与配置.md) / [**返回总览**](00-Anthropic兼容接口总览.md)
