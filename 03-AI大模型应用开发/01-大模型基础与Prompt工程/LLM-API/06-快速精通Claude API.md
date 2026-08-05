# 06 - 快速精通 Claude API（Anthropic）

> 🎯 Claude 系列是 Anthropic 打造的安全优先、推理能力顶尖的闭源大模型。其 API 以 Tool Use（工具调用）、200K 超长上下文、Computer Use（计算机操作）三大核心能力著称，是企业级 Agent 开发的首选之一

> **前置阅读**：[[02-大模型API调用实践]]、[[01-LLM API协议与核心概念]]

---

## 目录

1. [Claude 模型全景](#1-claude-模型全景)
2. [API 基础调用](#2-api-基础调用)
3. [Tool Use 工具调用](#3-tool-use-工具调用)
4. [超长上下文实战](#4-超长上下文实战)
5. [流式输出与 SSE](#5-流式输出与-sse)
6. [最佳实践](#6-最佳实践)

---

## 1. Claude 模型全景

| 模型 | 定位 | 上下文 | 特点 |
|------|------|:---:|------|
| **Claude Opus 4.8** | 旗舰推理 | 200K | 最高推理能力，复杂 Agent |
| **Claude Sonnet 5** | 性能均衡 | 200K | 性价比最优，日常主力 |
| **Claude Haiku 4.5** | 轻量极速 | 200K | 最低延迟，简单任务 |

### 核心差异化

| 能力 | 说明 |
|------|------|
| **Tool Use** | 原生函数调用，支持并行+递归+结构化输出 |
| **200K 上下文** | 一次可处理 ~500 页书或整个代码仓库 |
| **Computer Use** | 操作计算机（点击/输入/截图），自动化先锋 |
| **Prompt Caching** | 缓存长 system prompt 或文档，成本降 90% |
| **安全性** | Constitutional AI 训练，更难被越狱 |

## 2. API 基础调用

### 2.1 Python SDK

```python
# 安装
# pip install anthropic

import anthropic

client = anthropic.Anthropic(api_key="sk-ant-api03-xxx")

# 基础对话
response = client.messages.create(
    model="claude-sonnet-5-20251001",
    max_tokens=1024,
    system="You are a Java backend expert. Always provide code in Java 17+.",
    messages=[
        {"role": "user", "content": "How to handle N+1 query in Spring Data JPA?"}
    ]
)
print(response.content[0].text)
```

### 2.2 Messages API 格式

```
与 OpenAI Chat Completions API 的关键差异：

OpenAI:
  messages: [{"role": "system", "content": "..."},
             {"role": "user", "content": "..."}]

Claude:
  system: "..."                ← System prompt 是独立参数
  messages: [{"role": "user", "content": "..."}]
```

| 维度 | OpenAI | Claude |
|------|--------|--------|
| System Prompt | `role: "system"` in messages | 独立 `system` 参数 |
| 最大 max_tokens | 4096-16384 | **必须显式设置**（最大 8192-32768） |
| 停止序列 | `stop` 参数 | `stop_sequences` 参数 |
| 多模态 | `image_url` | `source.type: "base64"` |

> ⚠️ **关键差异**：Claude 的 `max_tokens` 是**必填参数**且必须 > 0，不设会报错

### 2.3 Java SDK

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.anthropic</groupId>
    <artifactId>anthropic-java</artifactId>
    <version>0.8.0</version>
</dependency>
```

```java
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.*;

AnthropicClient client = AnthropicOkHttpClient.builder()
    .apiKey("sk-ant-api03-xxx")
    .build();

MessageCreateParams params = MessageCreateParams.builder()
    .model(Model.CLAUDE_SONNET_5_20251001)
    .maxTokens(1024)
    .system("You are a Java expert.")
    .addUserMessage("Explain virtual threads in Java 21")
    .build();

Message message = client.messages().create(params);
System.out.println(message.content().get(0).asText().text());
```

## 3. Tool Use 工具调用

### 3.1 定义工具

```python
tools = [
    {
        "name": "get_weather",
        "description": "Get the current weather for a location",
        "input_schema": {
            "type": "object",
            "properties": {
                "location": {
                    "type": "string",
                    "description": "City name, e.g. San Francisco"
                },
                "unit": {
                    "type": "string",
                    "enum": ["celsius", "fahrenheit"]
                }
            },
            "required": ["location"]
        }
    }
]

response = client.messages.create(
    model="claude-sonnet-5-20251001",
    max_tokens=1024,
    tools=tools,
    messages=[{"role": "user", "content": "What's the weather in Tokyo?"}]
)
```

### 3.2 处理 Tool Use 响应

```python
# 检查响应是否包含 tool_use
if response.stop_reason == "tool_use":
    for block in response.content:
        if block.type == "tool_use":
            tool_name = block.name
            tool_args = block.input
            tool_id = block.id

            # 执行工具
            result = execute_tool(tool_name, tool_args)

            # 回填结果
            response = client.messages.create(
                model="claude-sonnet-5-20251001",
                max_tokens=1024,
                tools=tools,
                messages=[
                    {"role": "user", "content": "What's the weather in Tokyo?"},
                    {"role": "assistant", "content": response.content},
                    {
                        "role": "user",
                        "content": [{
                            "type": "tool_result",
                            "tool_use_id": tool_id,
                            "content": json.dumps(result)
                        }]
                    }
                ]
            )
```

### 3.3 Claude vs OpenAI Function Calling

| 维度 | Claude Tool Use | OpenAI Function Calling |
|------|:---:|:---:|
| 格式 | `tool_use` block (自定义) | `tool_calls` JSON |
| 并行调用 | ✅ 原生支持 | ✅ 原生支持 |
| 工具定义 | `input_schema` (JSON Schema) | `parameters` (JSON Schema) |
| 结果回填 | `tool_result` block | `role: "tool"` message |
| 强制调用 | ❌ 建议而不强制 | ✅ `tool_choice` 可强制 |
| 准确率 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |

## 4. 超长上下文实战

```python
# 将整本书上传并提问
with open("design-patterns.pdf", "rb") as f:
    pdf_data = base64.b64encode(f.read()).decode()

response = client.messages.create(
    model="claude-sonnet-5-20251001",
    max_tokens=2048,
    messages=[{
        "role": "user",
        "content": [
            {
                "type": "document",
                "source": {
                    "type": "base64",
                    "media_type": "application/pdf",
                    "data": pdf_data
                }
            },
            {"type": "text", "text": "Summarize all design patterns in this book"}
        ]
    }]
)
```

> 💡 200K 上下文意味着你可以把整个微服务的代码仓库丢给 Claude 做 code review

## 5. 流式输出与 SSE

```python
with client.messages.stream(
    model="claude-sonnet-5-20251001",
    max_tokens=1024,
    messages=[{"role": "user", "content": "Write a haiku about coding"}]
) as stream:
    for text in stream.text_stream:
        print(text, end="", flush=True)
        # 处理不同的流事件
        # text_delta: 文本增量
        # content_block_start: 开始新内容块
        # message_stop: 消息完成
```

## 6. 最佳实践

| 实践 | 说明 |
|------|------|
| **System Prompt 详细化** | Claude 比 GPT 更依赖良好的 system prompt |
| **将示例放在 messages 前段** | Claude 是"开头加权"型 attention |
| **`max_tokens` 必须设** | 且要比预期输出多 20% buffer |
| **长文档用 Prompt Caching** | 成本降低 90%，适合固定知识库 |
| **Tool Use 结果精炼** | 只回填工具返回的关键信息，不要 dump 整个 JSON |

## 核心要点回顾

- Claude API 的 system prompt 是独立参数，max_tokens 必填
- Tool Use 用 `tool_use` block → `tool_result` 回填 → 循环
- 200K 上下文 + 原生 PDF 处理 = 文档分析王者
- Computer Use 是 Claude 独有的"操作电脑"能力
- Java SDK 需要额外依赖 `anthropic-java`
- 成本：Opus > Sonnet > Haiku，选 Sonnet 性价比最高

## 参考资料

1. Anthropic API 官方文档 — docs.anthropic.com
2. Anthropic Cookbook — github.com/anthropics/anthropic-cookbook
3. Claude Tool Use 指南
