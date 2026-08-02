# 02 - Responses API 核心协议

> 🎯 Responses API（`v1/responses`）是 OpenAI 新一代开发范式 — 内置工具调用循环、多智能体执行、跨轮次推理上下文。本章覆盖端点对比、参数详解、与 Chat Completions 的关系

---

## 目录

1. [Responses vs Chat Completions](#1-responses-vs-chat-completions)
2. [端点与认证](#2-端点与认证)
3. [请求参数详解](#3-请求参数详解)
4. [支持的 Tools 全景](#4-支持的-tools-全景)
5. [编程式工具调用](#5-编程式工具调用)
6. [多智能体执行（Beta）](#6-多智能体执行beta)

---

## 1. Responses vs Chat Completions

| 对比 | Responses API（新） | Chat Completions（旧） |
|------|---------------------|----------------------|
| 端点 | `v1/responses` | `v1/chat/completions` |
| 状态 | **新一代范式** | 仍支持（兼容） |
| 工具循环 | **内置**（自动管理） | 手动管理 |
| 推理上下文 | `reasoning.context` 跨轮次持久 | 无 |
| 多智能体 | ✅（Beta） | ❌ |
| 视觉细节 | original/auto 保留原尺寸 | 有限 |
| 适用 | **新项目首选** | 存量项目 |

---

## 2. 端点与认证

```bash
# 基本信息
Base URL:  https://api.openai.com/v1
认证:      Authorization: Bearer $OPENAI_API_KEY
端点:      POST /responses（主要）
          POST /responses/{id}（继续）
          POST /chat/completions（兼容）
          POST /batches（批量）
          POST /fine_tuning/jobs（微调）
```

```python
from openai import OpenAI
client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

response = client.responses.create(
    model="gpt-5.6-terra",
    reasoning={"effort": "low"},
    input="Summarize this support thread and flag any refund request."
)
print(response.output_text)
```

---

## 3. 请求参数详解

```python
response = client.responses.create(
    # ===== 模型 =====
    model="gpt-5.6-terra",            # gpt-5.6-sol / terra / luna / gpt-5.5

    # ===== 输入 =====
    input="...",                       # 字符串或消息数组
    instructions="你是 Java 专家",      # 系统指令（替代 system role）
    context={"prior_context": "..."},  # 可选：跨请求上下文

    # ===== 推理控制 =====
    reasoning={
        "effort": "medium",            # none/low/medium/high/xhigh/max
        "mode": "standard"             # standard | pro（质量优先）
    },

    # ===== 工具 =====
    tools=[
        {"type": "web_search"},
        {"type": "file_search", "vector_store_ids": [...]},
        {"type": "code_interpreter"},
        {"type": "function", "name": "get_weather", ...}
    ],
    tool_choice="auto",

    # ===== 输出 =====
    max_output_tokens=8192,
    stream=True,
    text={"format": {"type": "json_schema", "name": "...", "schema": {...}}}
)
```

**关键参数对比（vs Chat Completions）：**

| 概念 | Responses API | Chat Completions |
|------|---------------|------------------|
| 系统指令 | `instructions` | `system` role |
| 消息 | `input` | `messages` |
| 推理 | `reasoning.effort/mode` | `reasoning_effort` |
| 结构化输出 | `text.format` | `response_format` |
| 工具选择 | `tool_choice` | 同 |

---

## 4. 支持的 Tools 全景

| 工具 | 类型 ID | 说明 |
|------|---------|------|
| **Web Search** | `web_search` | 联网搜索（$0.01/次） |
| **File Search** | `file_search` | 向量检索（需 vector_store） |
| **Image Generation** | `image_generation` | 图片生成 |
| **Code Interpreter** | `code_interpreter` | 代码执行 |
| **Hosted Shell** | `hosted_shell` | 托管 shell |
| **Apply Patch** | `apply_patch` | 补丁应用 |
| **Skills** | `skills` | 技能系统 |
| **Computer Use** | `computer_use` | 桌面操控 |
| **MCP** | `mcp` | 外部 MCP 服务器 |
| **Tool Search** | `tool_search` | 工具搜索 |
| **Function** | `function` | 自定义函数 |

```python
# 工具组合示例（客服机器人）
response = client.responses.create(
    model="gpt-5.6-terra",
    tools=[
        {"type": "web_search"},
        {"type": "file_search", "vector_store_ids": ["vs_abc123"]},
        {"type": "code_interpreter"}
    ],
    input="查一下我们最新的退款政策（搜索公司文档），并在网上搜索相关法规"
)
```

---

## 5. 编程式工具调用

**GPT-5.6 的新能力 — 模型编写 JavaScript 在隔离 V8 运行时中编排工具调用：**

```text
传统工具调用：
  模型 → 返回 tool_call → 你的代码执行 → 回传结果 → 循环

编程式工具调用（新）：
  模型 → 编写 JavaScript 代码（在隔离的 V8 沙箱中执行）
  → 代码内部编排多个工具调用
  → 无网络访问（安全隔离）
  → 一次完成复杂的工具编排逻辑

适合：
├── 多步骤工具编排（先查库存再计算价格）
├── 条件分支（根据结果决定调哪个工具）
└── 数据处理（工具结果需要加工后再用）
```

---

## 6. 多智能体执行（Beta）

```python
# 多智能体执行（Beta）— 模型内部协调多个子智能体
response = client.responses.create(
    model="gpt-5.6-sol",
    reasoning={
        "effort": "high",
        "mode": "pro"                    # Pro 模式：质量优先
    },
    input="分析这份财务报表，生成报告，并起草给董事会的邮件"
)
# Sol 的 Ultra 模式默认协调 4 个子智能体并行工作
# 最高可扩展至 16 个（以更高算力换取更强结果和更快速度）
```

**Ultra 模式的子智能体分工示例：**
```text
主智能体（Ultra 协调者）
├── Analyst-A：财务数据分析
├── Analyst-B：行业对比研究
├── Writer：报告撰写
└── Reviewer：质量审查
→ 汇总整合 → 最终交付
```

---

> 🎯 **核心要点**：Responses API 三个新能力 — **① 内置工具循环（不再手动管理）② 编程式工具调用（模型写 JS 在 V8 沙箱编排）③ 多智能体执行（Ultra 4-16 子 Agent）**。迁移建议：新项目直接用 Responses API；存量 Chat Completions 项目仍可兼容，逐步迁移。

**下一模块**：[03-Reasoning-推理控制](03-Reasoning-推理控制.md) / **返回总览**：[00-总览](00-GPT-API知识体系总览.md)
