# 07 Responses API：新路线全解

> 定位：OpenAI 的 Agent 原生接口——previous_response_id、内置工具、服务端会话；对比 Chat Completions 并给出迁移与选型（2026-08 基准）

## 📚 目录

1. [Responses API 是什么](#1-responses-api-是什么)
2. [架构对比：两个接口的哲学差异](#2-架构对比两个接口的哲学差异)
3. [状态管理：previous_response_id](#3-状态管理previous_response_id)
4. [内置工具：平台托管执行](#4-内置工具平台托管执行)
5. [自定义函数的调用循环](#5-自定义函数的调用循环)
6. [结构化输出与流式](#6-结构化输出与流式)
7. [迁移对照表：Chat Completions → Responses](#7-迁移对照表chat-completions--responses)
8. [选型决策树与风险](#8-选型决策树与风险)

## 1. Responses API 是什么

**2026 定位**：OpenAI 官方推荐的 Agent 应用默认接口（`/v1/responses`）。Chat Completions 无限期支持（行业标准），但**新高级能力只往 Responses 上发**：

```text
2026 生态事实：
✅ Responses API  = Agent 默认（Agno、Microsoft Agent Framework 等已切默认）
✅ Chat Completions = 行业标准，继续支持新模型（GPT-5.x 全支持）
⚠️ Assistants API = 已弃用，2026 上半年停服（一源：2026-08-26）
❌ 多供应商兼容（DeepSeek 等） = 只有 Chat Completions 形态
```

| 维度 | Chat Completions | Responses API |
|------|-----------------|---------------|
| 端点 | `/v1/chat/completions` | `/v1/responses` |
| 定位 | 对话接口 | **Agent 运行时** |
| 状态 | 客户端全量维护 | 服务端链式（previous_response_id） |
| 工具 | 仅自定义 function | 内置工具 + 自定义 function |
| 循环 | 你自己写 | 内置工具由平台自动循环 |
| 流式 | delta 拼接 | 语义事件 |

> 🎯 **核心要点**：哲学差异一句话——**Chat Completions 是"单次请求→单次响应"的对话模型接口，Responses 是"给个目标、平台托管循环"的 Agent 运行时**。开发者职责从"建循环"变成"定义行为"。

## 2. 架构对比：两个接口的哲学差异

```text
Chat Completions（阶段 1/2 主路径）:
你的代码：请求 → tool_call → 执行 → 回传 → 再请求（循环全自己写）

Responses API:
你的代码：输入目标 + 工具列表 → 平台处理循环
  ├─ 内置工具（web_search 等）→ 平台自动执行并回传
  └─ 自定义 function → 平台返回调用意图，你执行后回传 function_call_output
```

| 差异点 | 影响 |
|--------|------|
| 推理模型上下文 | Responses 跨轮复用推理 KV 缓存（官方称缓存利用率提升 40-80%，SWE-bench ~3% 提升） |
| 状态传递 | 不再每轮重发全量 messages，`previous_response_id` 引用历史 |
| 响应形状 | 类型化 `output` 数组 + `response.output_text` 便捷属性 |
| 状态字段 | `status`（completed/failed/in_progress）+ reasoning token 统计 |

## 3. 状态管理：previous_response_id

```python
from openai import OpenAI

client = OpenAI()

# 第一轮：普通请求
r1 = client.responses.create(
    model="gpt-4o-mini",
    input="北京和东京天气对比？",
    tools=[...],          # 自定义函数工具（扁平结构）
)

# 后续轮：引用上一轮 response id，而不是重发全量历史
r2 = client.responses.create(
    model="gpt-4o-mini",
    previous_response_id=r1.id,     # ★ 状态链式引用
    input="那东京呢？",
)
```

| 特性 | 说明 |
|------|------|
| `previous_response_id` | 服务端自动携带历史上下文 |
| `store: true` | 服务端存储会话（Conversations） |
| 与手写 messages 对比 | 客户端不再维护/重发消息数组（省带宽与编排代码） |
| 注意 | 缓存/成本模型不同——按官方计价核对长会话成本 |

> ⚠️ 依赖平台状态 = 绑定 OpenAI 生态；多供应商混合场景仍走 Chat Completions。

## 4. 内置工具：平台托管执行

| 内置工具 | 能力 | 定价（2026） | 注意 |
|---------|------|------------|------|
| `web_search` | 实时搜索 + 引用标注 | $25-50/千次调用 | 不可控：无源过滤/新鲜度保证/检索透明 |
| `file_search` | 向量库 RAG | 按存储+查询 | 与自建 RAG 对比成本 |
| `code_interpreter` | 沙箱 Python 执行 | 按用量 | 平台托管沙箱 |
| `computer_use` | 虚拟桌面控制 | 预览 | 预览阶段 |
| MCP 托管 | 外部工具接入 | 按用量 | 2026 生态接入 |

```python
response = client.responses.create(
    model="gpt-4o-mini",
    input="今天 AI Agent 有什么大新闻？",
    tools=[{"type": "web_search"}],     # 平台自动搜索、自动回传结果
)
print(response.output_text)              # 含引用标注的最终回答
```

> 💡 **工程权衡**：内置 `web_search` 简单但**不透明且贵**（1 万次/天 ≈ $250-500）。生产 Agent 常用自定义 function 工具路由到第三方搜索（Tavily/Parallel 等）——索引可控、成本可控、token 密度可控。**内置工具是快速原型，自定义工具是生产选择**。

## 5. 自定义函数的调用循环

Responses 下自定义函数仍是"你执行"，但回传格式不同：

```python
# 模型要调工具时，响应含 function_call 项
response = client.responses.create(
    model="gpt-4o-mini",
    input="北京天气？",
    tools=[{"type": "function", "name": "get_weather",
            "parameters": {...}, "description": "..."}],
)

# 1. 取出调用意图
for item in response.output:
    if item.type == "function_call":
        name, args = item.name, item.arguments      # arguments 仍是 JSON 字符串
        call_id = item.call_id                      # ★ 回传凭证

        # 2. 执行你的函数
        result = execute_tool(name, json.loads(args))

        # 3. 以 function_call_output 回传（★ 不是 role: tool 消息）
        response = client.responses.create(
            model="gpt-4o-mini",
            previous_response_id=response.id,
            input=[{"type": "function_call_output",
                    "call_id": call_id,
                    "output": json.dumps(result)}],
        )
```

> ⚠️ **迁移铁律**：Responses 续传**不能用**旧式 `{"role": "tool", "tool_call_id": ...}` 消息——必须用 `function_call_output` 项（`call_id` 配对）。混用直接校验失败。

## 6. 结构化输出与流式

| 能力 | Responses 形态 | 对比 Chat Completions |
|------|---------------|---------------------|
| 结构化输出 | `text.format`（json_schema） | `response_format` |
| 流式 | 语义事件：`response.output_text.delta`、`response.completed` | delta 拼接 |
| 状态/统计 | `status` + `output_tokens_details.reasoning_tokens` | 仅 finish_reason |

```python
# 流式事件示例
with client.responses.stream(model="gpt-4o-mini", input="你好") as s:
    for event in s:
        if event.type == "response.output_text.delta":
            print(event.delta, end="")
        elif event.type == "response.completed":
            print("\n[完成]")
```

## 7. 迁移对照表：Chat Completions → Responses

| 维度 | Chat Completions | Responses |
|------|-----------------|-----------|
| 请求体 | `messages` | `input` |
| 输出上限 | `max_tokens` / `max_completion_tokens` | `max_output_tokens` |
| 结构化 | `response_format` | `text.format` |
| seed | ✅ | ❌（已移除） |
| 多模态文本 | `text` | `input_text` |
| 图片 | `image_url`（嵌套） | `input_image`（旧嵌套形状会校验失败） |
| 工具回传 | `{"role": "tool", "tool_call_id": ...}` | `function_call_output` + `call_id` |
| 响应读取 | `choices[0].message.content` | `output_text`（或 output 数组） |
| 流式 | `choices[0].delta.content` | 语义事件 |
| 状态 | 客户端 messages | `previous_response_id` / `store: true` |

## 8. 选型决策树与风险

```text
需要多供应商兼容（DeepSeek/Qwen/本地）？→ Chat Completions（协议通吃，阶段 1/2 主力）
纯 OpenAI 生态？
├─ 新项目 + 内置工具/托管状态/推理跨轮 → Responses（官方推荐）
├─ 已有 Chat Completions 存量 → 按需迁移（gateway 可双路径并存）
└─ 需要 seed/细粒度控制 → 留在 Chat Completions
```

| 风险 | 说明 |
|------|------|
| 生态锁定 | Responses 是 OpenAI 专属——迁移成本高 |
| 内置工具不透明 | web_search 无源过滤/新鲜度保证 |
| 计价模型差异 | 长会话/内置工具成本需实测核算 |
| Go 等生态滞后 | 部分语言只有 Chat Completions 客户端 |

> 🎯 **核心要点**：Responses API 的正确心态——**它是"Agent 原生接口"的未来方向，但 2026 年多供应商世界仍以 Chat Completions 为事实协议**。会画迁移对照表、会判断何时切，比"无脑迁移"专业得多。

---

**返回总览**：[00-阶段总览：原生 OpenAI-style Function-call](00-阶段总览：原生%20OpenAI-style%20Function-call.md) / **上一模块**：[06-SDK 高级参数全解](06-SDK%20高级参数全解.md) / **下一模块**：[08-实战项目与阶段验收](08-实战项目与阶段验收.md)
