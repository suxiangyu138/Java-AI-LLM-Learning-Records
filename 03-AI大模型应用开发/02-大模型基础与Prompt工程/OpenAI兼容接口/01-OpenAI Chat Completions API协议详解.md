# OpenAI Chat Completions API 协议详解

> Chat Completions API 是整个 OpenAI 兼容生态的基石协议——理解它，就掌握了与所有 LLM 对话的通用语言。

---

## 📚 目录

1. [协议概览与设计哲学](#1-协议概览与设计哲学)
2. [请求格式详解](#2-请求格式详解)
3. [响应格式详解](#3-响应格式详解)
4. [流式输出（Streaming）](#4-流式输出streaming)
5. [Tool Calling（函数调用）](#5-tool-calling函数调用)
6. [JSON Mode 与 Structured Output](#6-json-mode-与-structured-output)
7. [Vision 多模态](#7-vision-多模态)
8. [关键参数调优指南](#8-关键参数调优指南)
9. [兼容性矩阵](#9-兼容性矩阵)

---

## 1. 协议概览与设计哲学

### 1.1 核心端点

```text
POST https://api.openai.com/v1/chat/completions
Content-Type: application/json
Authorization: Bearer <api_key>
```

### 1.2 设计原则

| 原则 | 说明 |
|------|------|
| **RESTful** | 标准 HTTP POST，JSON 请求/响应体 |
| **无状态** | 每次请求独立，上下文由 `messages` 数组携带 |
| **SSE 流式** | 可选流式输出，Server-Sent Events 标准协议 |
| **可扩展** | `tool_choice`、`response_format` 等字段逐步演进 |

> 💡 Chat Completions 本质是**无状态推理服务**：你把完整对话历史发过去，模型基于历史生成下一个回复。没有服务端会话概念。

### 1.3 一个最简请求

```bash
curl https://api.openai.com/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OPENAI_API_KEY" \
  -d '{
    "model": "gpt-4o",
    "messages": [
      {"role": "user", "content": "Hello!"}
    ]
  }'
```

---

## 2. 请求格式详解

### 2.1 顶层字段

```json
{
  "model": "gpt-4o",
  "messages": [...],
  "temperature": 0.7,
  "max_tokens": 4096,
  "top_p": 1.0,
  "n": 1,
  "stream": false,
  "stop": ["\n\n"],
  "frequency_penalty": 0.0,
  "presence_penalty": 0.0,
  "user": "user_12345"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `model` | string | ✅ | 模型 ID，如 `gpt-4o`、`deepseek-chat` |
| `messages` | array | ✅ | 对话消息列表（见 2.2） |
| `temperature` | float | ❌ | 0.0~2.0，越高越随机，默认 1.0 |
| `max_tokens` | int | ❌ | 输出 token 上限（含 reasoning tokens） |
| `top_p` | float | ❌ | 核采样，0.0~1.0，默认 1.0 |
| `n` | int | ❌ | 返回几个候选回复，默认 1 |
| `stream` | bool | ❌ | 是否流式输出，默认 false |
| `stop` | string/array | ❌ | 停止符，遇到即终止生成 |
| `frequency_penalty` | float | ❌ | -2.0~2.0，降低重复词概率 |
| `presence_penalty` | float | ❌ | -2.0~2.0，鼓励讨论新话题 |
| `user` | string | ❌ | 终端用户标识（用于滥用监控） |

### 2.2 Messages 数组

消息角色（role）体系：

| role | 说明 | 示例场景 |
|------|------|---------|
| `system` | 系统级指令，设定 AI 行为边界 | "你是一个专业的 Java 后端工程师" |
| `user` | 用户输入 | "这段代码有什么问题？" |
| `assistant` | AI 的回复（含 tool_calls） | 模型生成的内容或函数调用请求 |
| `tool` | 函数调用返回结果 | `{"role": "tool", "tool_call_id": "call_xxx", "content": "..."}` |

```json
{
  "messages": [
    {
      "role": "system",
      "content": "你是 Java 代码审查专家。只回答代码相关问题。"
    },
    {
      "role": "user",
      "content": "这段代码有什么问题？\n```java\npublic void process() {\n    String s = null;\n    s.length();\n}\n```"
    },
    {
      "role": "assistant",
      "content": "这段代码存在空指针异常风险：变量 s 被赋值为 null 后直接调用 length() 方法..."
    },
    {
      "role": "user",
      "content": "如何修复？"
    }
  ]
}
```

> ⚠️ **兼容性注意**：部分兼容实现（如 Ollama 早期版本、某些 llama.cpp 配置）不支持 `system` role，system prompt 会被合并到第一条 user message 中。

### 2.3 Content 的多种形式

```json
// 纯文本（最常见）
{"role": "user", "content": "Hello"}

// 多模态（图片 + 文本）
{
  "role": "user",
  "content": [
    {"type": "text", "text": "这张图片里有什么？"},
    {
      "type": "image_url",
      "image_url": {
        "url": "https://example.com/image.png",
        "detail": "auto"
      }
    }
  ]
}

// base64 图片（推荐，避免外网依赖）
{
  "type": "image_url",
  "image_url": {
    "url": "data:image/png;base64,iVBORw0KGgo..."
  }
}
```

---

## 3. 响应格式详解

### 3.1 非流式响应

```json
{
  "id": "chatcmpl-123abc",
  "object": "chat.completion",
  "created": 1677652288,
  "model": "gpt-4o-2024-05-13",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "Hello! How can I help you today?"
      },
      "finish_reason": "stop",
      "logprobs": null
    }
  ],
  "usage": {
    "prompt_tokens": 9,
    "completion_tokens": 12,
    "total_tokens": 21
  },
  "system_fingerprint": "fp_abc123"
}
```

**finish_reason 枚举值：**

| 值 | 含义 |
|------|------|
| `stop` | 正常结束（遇到 stop 序列或自然完成） |
| `length` | 达到 `max_tokens` 上限，输出被截断 |
| `tool_calls` | 模型请求调用函数 |
| `content_filter` | 内容安全过滤，输出被拦截 |
| `null` | 流式输出中间块 |

### 3.2 Usage 字段

```json
"usage": {
  "prompt_tokens": 150,       // 输入 token 数
  "completion_tokens": 80,    // 输出 token 数
  "total_tokens": 230,        // 总计
  // OpenAI 特有字段（兼容接口通常不返回）：
  "prompt_tokens_details": {
    "cached_tokens": 100      // 命中的缓存 token
  },
  "completion_tokens_details": {
    "reasoning_tokens": 50    // 推理模型（o1/o3）的思考 token
  }
}
```

> ⚠️ **兼容性注意**：`prompt_tokens_details` 和 `completion_tokens_details` 是 OpenAI 特有字段，绝大多数兼容接口不返回。付费计费应依赖 `total_tokens`。

---

## 4. 流式输出（Streaming）

### 4.1 协议说明

设置 `"stream": true` 后，响应变为 SSE（Server-Sent Events）格式：

```text
POST /v1/chat/completions
Accept: text/event-stream

HTTP/1.1 200 OK
Content-Type: text/event-stream
Transfer-Encoding: chunked

data: {"id":"chatcmpl-123","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"role":"assistant","content":""},"finish_reason":null}]}

data: {"id":"chatcmpl-123","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"Hello"},"finish_reason":null}]}

data: {"id":"chatcmpl-123","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"!"},"finish_reason":null}]}

data: {"id":"chatcmpl-123","object":"chat.completion.chunk","choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}

data: [DONE]
```

### 4.2 Chunk 结构

```json
{
  "id": "chatcmpl-123",
  "object": "chat.completion.chunk",
  "created": 1677652288,
  "model": "gpt-4o",
  "choices": [
    {
      "index": 0,
      "delta": {
        "role": "assistant",   // 仅第一个 chunk 有
        "content": "Hello"     // 增量文本
      },
      "finish_reason": null    // 最后一个 chunk 才有值
    }
  ],
  "usage": null                 // 仅最后一个 chunk 可能有（取决于实现）
}
```

### 4.3 Java SSE 解析要点

```java
// 关键：delta.content 可能为 null（tool_calls chunk）
// 关键：content 是增量，需要前端拼接
// 关键：最后一个 chunk 的 finish_reason 非 null

String content = chunk.getChoices().get(0).getDelta().getContent();
if (content != null) {
    // 累加到 StringBuilder
    fullContent.append(content);
}
// 判断是否结束
String finishReason = chunk.getChoices().get(0).getFinishReason();
if (finishReason != null) {
    // 流结束，做清理工作
}
```

---

## 5. Tool Calling（函数调用）

### 5.1 协议演进

```text
2023.06：Function Calling（functions 参数，已废弃）
2023.11：Tool Calling（tools 参数，与 Function Calling 并行）
2024+：  Tool Calling 成为主流，functions 被移除
```

### 5.2 工具定义

```json
{
  "model": "gpt-4o",
  "messages": [...],
  "tools": [
    {
      "type": "function",
      "function": {
        "name": "get_weather",
        "description": "获取指定城市的天气信息",
        "parameters": {
          "type": "object",
          "properties": {
            "location": {
              "type": "string",
              "description": "城市名称，如 Beijing"
            },
            "unit": {
              "type": "string",
              "enum": ["celsius", "fahrenheit"],
              "description": "温度单位"
            }
          },
          "required": ["location"]
        }
      }
    }
  ],
  "tool_choice": "auto"
}
```

**tool_choice 选项：**

| 值 | 说明 |
|------|------|
| `"auto"`（默认） | 模型自行决定是否调用工具 |
| `"none"` | 强制不调用工具 |
| `"required"` | 强制调用工具 |
| `{"type": "function", "function": {"name": "xxx"}}` | 强制调用指定工具 |

### 5.3 工具调用响应

```json
// 模型返回函数调用请求
{
  "choices": [{
    "index": 0,
    "message": {
      "role": "assistant",
      "content": null,
      "tool_calls": [
        {
          "id": "call_abc123",
          "type": "function",
          "function": {
            "name": "get_weather",
            "arguments": "{\"location\": \"Beijing\", \"unit\": \"celsius\"}"
          }
        }
      ]
    },
    "finish_reason": "tool_calls"
  }]
}
```

### 5.4 调用循环

```text
用户提问
  → LLM 返回 tool_calls
    → 你的代码执行函数
      → 将结果以 tool role 发回
        → LLM 生成最终回答
```

```json
// 发送函数执行结果
{
  "model": "gpt-4o",
  "messages": [
    {"role": "user", "content": "北京今天天气怎么样？"},
    {
      "role": "assistant",
      "content": null,
      "tool_calls": [{
        "id": "call_abc123",
        "type": "function",
        "function": {"name": "get_weather", "arguments": "{\"location\":\"Beijing\"}"}
      }]
    },
    {
      "role": "tool",
      "tool_call_id": "call_abc123",
      "content": "{\"temperature\": 35, \"condition\": \"sunny\"}"
    }
  ]
}
```

> ⚠️ **兼容性注意**：Tool Calling 支持差异很大。vLLM/DeepSeek/Qwen 支持良好；Ollama 需较新版本；llama.cpp 需要模型本身支持 function calling 格式。

---

## 6. JSON Mode 与 Structured Output

### 6.1 JSON Mode

```json
{
  "model": "gpt-4o",
  "messages": [
    {
      "role": "system",
      "content": "你是一个 JSON 输出器。始终返回合法 JSON。"
    },
    {
      "role": "user",
      "content": "列出 3 种 Java 集合类及其特点"
    }
  ],
  "response_format": {
    "type": "json_object"
  }
}
```

> ⚠️ **关键限制**：开启 `json_object` 时，system prompt 或 user prompt 中必须出现 `JSON` 关键词，否则 API 返回错误。

### 6.2 Structured Output（OpenAI 特有）

```json
{
  "response_format": {
    "type": "json_schema",
    "json_schema": {
      "name": "collection_info",
      "strict": true,
      "schema": {
        "type": "object",
        "properties": {
          "collections": {
            "type": "array",
            "items": {
              "type": "object",
              "properties": {
                "name": {"type": "string"},
                "thread_safe": {"type": "boolean"},
                "features": {
                  "type": "array",
                  "items": {"type": "string"}
                }
              },
              "required": ["name", "thread_safe", "features"],
              "additionalProperties": false
            }
          }
        },
        "required": ["collections"],
        "additionalProperties": false
      }
    }
  }
}
```

> ⚠️ **兼容性注意**：`json_schema` 是 OpenAI 2024 年新增的严格结构化输出能力，绝大多数兼容接口**不支持**。跨平台通用方案是使用 `json_object` + 详细 schema 在 system prompt 中描述。

### 6.3 跨平台 JSON 输出策略

```text
方案 A：response_format: json_object + system prompt 描述 schema
  适用：GPT-4o, DeepSeek, Qwen 等主流模型 ✅

方案 B：response_format: json_schema（strict mode）
  适用：仅 OpenAI 官方 API ⚠️

方案 C：Tool Calling 强制输出（定义单工具 + tool_choice: required）
  适用：支持 function calling 的所有模型 🎯 推荐跨平台方案

方案 D：Grammar-based sampling（llama.cpp 特有）
  适用：本地 llama.cpp 部署 🔧
```

---

## 7. Vision 多模态

### 7.1 图片输入格式

```json
{
  "model": "gpt-4o",
  "messages": [
    {
      "role": "user",
      "content": [
        {"type": "text", "text": "这张 UML 图描述了什么设计模式？"},
        {
          "type": "image_url",
          "image_url": {
            "url": "data:image/png;base64,iVBORw0KGgo...",
            "detail": "high"
          }
        }
      ]
    }
  ]
}
```

### 7.2 Detail 参数

| 值 | 说明 | Token 消耗 |
|------|------|:---:|
| `auto` | 模型自行判断 | 自适应 |
| `low` | 低分辨率（512x512） | ~85 tokens |
| `high` | 高分辨率（按 tile 计费） | ~170 tokens/tile |

> ⚠️ **兼容性注意**：Vision 支持因模型和部署工具而异。vLLM 需加载多模态模型；Ollama 需 `llava`/`llama3.2-vision` 等专用模型；DeepSeek API 不支持图片输入（截至 2025）。

---

## 8. 关键参数调优指南

### 8.1 Temperature vs Top-p

```text
创意写作、头脑风暴：temperature=0.8~1.2, top_p=0.95
代码生成、翻译：  temperature=0.0~0.3, top_p=1.0  ← 更确定性
对话、客服：      temperature=0.5~0.7, top_p=0.9
数据分析、JSON：  temperature=0.0, top_p=1.0       ← 最确定性
```

> 💡 **黄金规则**：通常**只调 temperature 和 top_p 之一**，不同时修改两者。

### 8.2 max_tokens 设置策略

```java
// 经验公式（适用于 4K 以内的回复）
int maxTokens = Math.min(
    4096,                     // 单次回复上限
    contextWindow - promptTokens - 500  // 留 500 safety margin
);

// 推理模型（o1/o3/DeepSeek-R1）
// max_tokens 包含 reasoning_tokens + output_tokens
// 建议给足够空间：8000~16000
```

### 8.3 Stop 序列

```json
// 常用 stop 序列
"stop": ["\n\n", "Human:", "Assistant:", "User:"]

// 代码生成场景
"stop": ["```", "\n\n\n"]

// JSON 输出场景
"stop": ["}\n"]  // 注意：可能截断嵌套 JSON
```

---

## 9. 兼容性矩阵

| 特性 | OpenAI | DeepSeek | vLLM | Ollama | llama.cpp | 智谱 GLM |
|------|:---:|:---:|:---:|:---:|:---:|:---:|
| Chat Completions | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `system` role | ✅ | ✅ | ✅ | ⚠️ v0.2+ | ✅ | ✅ |
| Streaming (SSE) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Tool Calling | ✅ | ✅ | ✅ | ⚠️ 部分 | ⚠️ 模型 | ✅ |
| JSON Mode | ✅ | ✅ | ✅ | ✅ | ⚠️ grammar | ⚠️ |
| Vision | ✅ | ❌ | ⚠️ 多模态 | ⚠️ 专用模型 | ⚠️ 专用模型 | ✅ |
| `json_schema` | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| `logprobs` | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| `n > 1` | ✅ | ❌ | ⚠️ | ❌ | ❌ | ❌ |
| `seed` | ✅ | ❌ | ✅ | ✅ | ✅ | ⚠️ |

> 🎯 **核心要点**：基础 Chat Completions + Streaming 是所有兼容接口的共同子集——这是可移植代码的**安全区**。Tool Calling 和 Vision 是差异最大的区域，需要做好能力检测和降级。

---

**下一模块**：[02 - 嵌入与多模态接口协议](./02-嵌入与多模态接口协议.md)  
**返回总览**：[00 - OpenAI 兼容接口总览](./00-OpenAI兼容接口总览.md)
