# 02 - Messages API 核心协议

> 🎯 Messages API 是 Claude 的唯一对话端点 — `POST /v1/messages`，稳定的两角色交替结构。本章覆盖请求/响应全参数、多轮对话、系统提示、Token 计数、多模型支持

---

## 目录

1. [端点与认证](#1-端点与认证)
2. [请求结构详解](#2-请求结构详解)
3. [响应结构与 stop_reason](#3-响应结构与-stop_reason)
4. [系统提示与多轮对话](#4-系统提示与多轮对话)
5. [Token 计数与费用估算](#5-token-计数与费用估算)
6. [Bedrock / Vertex AI 部署差异](#6-bedrock--vertex-ai-部署差异)

---

## 1. 端点与认证

```bash
# 基本信息
Base URL:  https://api.anthropic.com
API 版本:  2023-06-01（通过 anthropic-version 请求头指定）
端点:      POST /v1/messages（主要）
          POST /v1/messages/batches（批量）
          POST /v1/messages/count_tokens（Token 计数）
           GET /v1/models（模型列表）
```

```python
# Python SDK 认证
from anthropic import Anthropic

client = Anthropic(api_key=os.getenv("ANTHROPIC_API_KEY"))

# 或通过 Bedrock：
# client = AnthropicBedrock(aws_access_key=..., aws_secret_key=...)
# 或通过 Vertex AI：
# client = AnthropicVertex(project_id=..., region=...)
```

---

## 2. 请求结构详解

```python
response = client.messages.create(
    model="claude-sonnet-4-6",         # ① 模型 ID
    max_tokens=4096,                    # ② ★ 必填！最大输出 Token 数
    system="你是 Java 后端专家。回答简洁，给出代码示例。",  # ③ 系统提示
    messages=[                          # ④ 消息数组（user/assistant 交替）
        {"role": "user", "content": "解释 Spring IoC"}
    ],
    # ===== 可选参数 =====
    temperature=0.7,                    # ⑤ 采样温度（0-1）
    # ⚠️ Opus 4.7 不支持 temperature/top_p/top_k → 400 错误
    stop_sequences=["END"],             # ⑥ 停止序列（最多 5 个）
    stream=False                        # ⑦ 是否流式
)

print(response.content[0].text)
```

### 必需参数速查

| 参数 | 类型 | 说明 |
|------|------|------|
| `model` | string | 模型 ID：`claude-sonnet-4-6` / `claude-opus-4-7` / `claude-haiku-4-5-20251001` |
| `max_tokens` | integer | **必填**。最大输出 Token 数（含 thinking tokens） |
| `messages` | array | **必填**。对话消息数组，`role` 为 `user` 或 `assistant` |

### 常用可选参数

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `system` | string/array | — | 系统提示（支持文本和 content block） |
| `temperature` | float | 0 | 采样温度。⚠️ **Opus 4.7 移除此参数（400 报错）** |
| `stop_sequences` | array | — | 最多 5 个停止序列 |
| `stream` | bool | false | 是否启用 SSE 流式 |
| `metadata` | object | — | 自定义元数据（`user_id` 用于监控） |

### 消息格式

```python
# ① 简单文本
{"role": "user", "content": "解释 Java 多态"}

# ② 多内容块（文本 + 图片）
{"role": "user", "content": [
    {"type": "text", "text": "这张图里有什么？"},
    {"type": "image", "source": {
        "type": "base64",
        "media_type": "image/png",
        "data": base64_image
    }}
]}

# ③ 必须是 user/assistant 交替
# ❌ 两个连续的 user 消息 → 400 错误
# ✅ user → assistant → user → assistant
```

---

## 3. 响应结构与 stop_reason

```python
# 完整响应对象
response = {
    "id": "msg_abc123",
    "type": "message",
    "role": "assistant",
    "model": "claude-sonnet-4-6",
    "content": [                        # 内容块数组
        {"type": "text", "text": "Spring IoC 是一种设计原则..."}
    ],
    "stop_reason": "end_turn",          # ★ 关键：为什么停止
    "stop_sequence": None,
    "usage": {
        "input_tokens": 25,
        "output_tokens": 150,
        "cache_creation_input_tokens": 0,
        "cache_read_input_tokens": 0
    }
}
```

### stop_reason 详解

| stop_reason | 含义 | 下一步 |
|-------------|------|--------|
| `end_turn` | 正常完成 | 对话结束 |
| `max_tokens` | 达到 `max_tokens` 上限 | 调大 `max_tokens` 后继续 |
| `stop_sequence` | 命中停止序列 | 根据业务处理 |
| **`tool_use`** | **Claude 想调用工具** | 执行工具 → 回传 `tool_result` → 继续 |

---

## 4. 系统提示与多轮对话

### 4.1 系统提示最佳实践

```python
# ① 顶层 system 参数（推荐，语义清晰）
client.messages.create(
    system="你是 Java 专家。回答简洁，给出可运行代码。",
    messages=[...]
)

# ② 多行系统提示
client.messages.create(
    system=[
        {"type": "text", "text": "你是 Java 专家。"},
        {"type": "text", "text": "代码风格：2空格缩进、camelCase命名。",
         "cache_control": {"type": "ephemeral"}}  # 标记为缓存点
    ],
    messages=[...]
)
```

### 4.2 多轮对话模式

```python
messages = []

# 第一轮
messages.append({"role": "user", "content": "解释 Java 多态"})
resp = client.messages.create(messages=messages, ...)
messages.append({"role": "assistant", "content": resp.content})

# 第二轮（★ 直接追加，无需手动拼接上下文）
messages.append({"role": "user", "content": "给一个代码示例"})
resp2 = client.messages.create(messages=messages, ...)
```

---

## 5. Token 计数与费用估算

```python
# 请求前估算 Token 数
count = client.messages.count_tokens(
    model="claude-sonnet-4-6",
    system="你是 Java 专家。",
    messages=[{"role": "user", "content": "解释多态"}]
)
print(count.input_tokens)  # 输入 Token 数

# 请求后查看实际消耗
print(response.usage.input_tokens)            # 输入
print(response.usage.output_tokens)           # 输出
print(response.usage.cache_read_input_tokens) # 缓存命中量
```

---

## 6. Bedrock / Vertex AI 部署差异

| 维度 | Direct API | Amazon Bedrock | Google Vertex AI |
|------|------------|----------------|------------------|
| Base URL | api.anthropic.com | AWS SDK | Google SDK |
| API Key | ANTHROPIC_API_KEY | AWS IAM | GCP SA |
| 数据位置 | US | 选 AWS Region | 选 GCP Region |
| 可用性 | 全球 | AWS 区域 | GCP 区域 |
| 价格 | 列表价 | 可能略高 | 可能略高 |

---

> 🎯 **核心要点**：Messages API 三个关键 — **① `max_tokens` 必填（必须 > thinking_tokens + 预期答案长度）② `stop_reason: "tool_use"` 触发工具循环 ③ Opus 4.7 移除了 temperature/top_p/top_k（设置→400 错误）**。

**下一模块**：[03-Extended-Thinking深度思考](03-Extended-Thinking深度思考.md) / **返回总览**：[00-总览](00-Claude-API知识体系总览.md)
