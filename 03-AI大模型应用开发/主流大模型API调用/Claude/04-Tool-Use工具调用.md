# 04 - Tool Use 工具调用

> 🎯 Claude 的 Tool Use = LLM 不执行工具，而是返回 `tool_use` 指令 → 客户端执行 → 回传结果。这是 Agent 循环的核心引擎，支持并行调用、服务端内置工具

---

## 目录

1. [Tool Use 的基本循环](#1-tool-use-的基本循环)
2. [工具定义与空安全校验](#2-工具定义与空安全校验)
3. [并行工具调用](#3-并行工具调用)
4. [服务端内置工具](#4-服务端内置工具)
5. [Agentic Loop 完整模板](#5-agentic-loop-完整模板)
6. [工具调用 + Thinking 组合](#6-工具调用--thinking-组合)

---

## 1. Tool Use 的基本循环

```python
# ① 定义工具
tools = [{
    "name": "get_weather",
    "description": "获取指定城市的天气信息",
    "input_schema": {
        "type": "object",
        "properties": {
            "city": {
                "type": "string",
                "description": "城市名称，如 Beijing、上海"
            },
            "unit": {
                "type": "string",
                "enum": ["celsius", "fahrenheit"],
                "description": "温度单位"
            }
        },
        "required": ["city"]
    }
}]

# ② 发请求 + 工具
response = client.messages.create(
    model="claude-sonnet-4-6",
    max_tokens=1024,
    tools=tools,
    messages=[{"role": "user", "content": "北京今天天气怎么样？"}]
)

# ③ 检查是否需要调工具
if response.stop_reason == "tool_use":
    for block in response.content:
        if block.type == "tool_use":
            tool_name = block.name          # "get_weather"
            tool_input = block.input         # {"city": "Beijing", "unit": "celsius"}
            tool_id = block.id               # ★ 必须原样返回

        # ④ 执行工具（客户端）
        result = execute_tool(tool_name, tool_input)

        # ⑤ 回传结果
        messages.append({"role": "user", "content": [{
            "type": "tool_result",
            "tool_use_id": tool_id,        # ★ 必须匹配！
            "content": result
        }]})

        # ⑥ 继续对话
        response2 = client.messages.create(
            model="claude-sonnet-4-6",
            max_tokens=1024,
            tools=tools,
            messages=messages
        )
```

**规则速记：** 模型不执行工具 → `stop_reason == "tool_use"` → 客户端执行 → `tool_result` 回传 → 继续。

---

## 2. 工具定义与空安全校验

```python
# 工具定义最佳实践（使用 Pydantic 生成 JSON Schema）
from pydantic import BaseModel, Field

class GetWeatherInput(BaseModel):
    city: str = Field(..., description="城市名称，如 北京、Shanghai")
    unit: str = Field(
        default="celsius",
        description="温度单位", enum=["celsius", "fahrenheit"]
    )

# 通过 Pydantic schema() 自动生成符合规范的 JSON Schema
tool_schema = GetWeatherInput.model_json_schema()
```

```python
# 工具选择模式
tool_choice = {
    "type": "auto"}              # ① 自动决定（默认）
    # "type": "any"}            # ② 强制至少调一个工具
    # "type": "tool",           # ③ 强制调指定工具
    #   "name": "get_weather"}
    # "type": "none"}           # ④ 禁止调用工具（纯文本回复）
```

**工具定义规范：**
- `description` 要精确描述功能的边界（而非泛泛的"搜索"）
- `input_schema` 的每个参数都加 `description`（Claude 据此理解参数含义）
- 必填参数放 `required`（空安全）

---

## 3. 并行工具调用

```python
# Claude 4+ 支持并行调用多个工具（一次返回多个 tool_use）
# 场景："北京和上海的天气各是什么？"
# → Claude 一次返回两个 tool_use block：

# 并行执行所有工具（用 asyncio 加速）
import asyncio

if response.stop_reason == "tool_use":
    tool_results = []
    tasks = []

    for block in response.content:
        if block.type == "tool_use":
            tasks.append(asyncio.create_task(
                execute_tool_async(block.name, block.input)
            ))
            tool_ids.append(block.id)

    results = await asyncio.gather(*tasks)

    # 回传所有结果
    content = []
    for tool_id, result in zip(tool_ids, results):
        content.append({
            "type": "tool_result",
            "tool_use_id": tool_id,
            "content": result
        })
    messages.append({"role": "user", "content": content})
```

---

## 4. 服务端内置工具

```python
# Claude 提供多种服务端工具（Anthropic 侧执行，不需要客户端代码）

# ① Web Search（网络搜索）
tools = [{"type": "web_search_20250305"}]
# 定价：$10/1000 次搜索

# ② Web Fetch（网页抓取）
tools = [{
    "type": "web_fetch_20250703",
    "allowed_domains": ["docs.example.com"],    # 可选域名白名单
    "blocked_domains": ["internal.example.com"]  # 可选域名黑名单
}]

# ③ Code Execution（代码执行）
tools = [{"type": "code_execution_20250703"}]
# Claude 可以执行 Python 代码来验证逻辑

# ④ Text Editor（文本编辑）
tools = [{"type": "text_editor_20250703"}]

# ⑤ Bash（命令执行）
tools = [{"type": "bash_20250703"}]

# ⑥ Computer Use（桌面操控）
tools = [{
    "type": "computer_20251124",
    "display_width_px": 1920,
    "display_height_px": 1080
}]

# 混合使用：客户端工具 + 服务端工具
tools = [
    {"type": "web_search_20250305"},      # 服务端搜索
    {"name": "query_database", ...}       # 客户端工具
]
```

---

## 5. Agentic Loop 完整模板

```python
def agentic_loop(user_query, max_turns=8):
    """通用 Agent 循环模板"""
    messages = [{"role": "user", "content": user_query}]

    for turn in range(max_turns):
        response = client.messages.create(
            model="claude-sonnet-4-6",
            max_tokens=4096,
            thinking={"type": "adaptive"},  # ★ 搭配思考
            output_config={"effort": "high"},
            tools=tools,
            messages=messages
        )

        # ① 追加 assistant 消息（含内容+tool_use）
        messages.append({
            "role": "assistant",
            "content": response.content
        })

        # ② 检查是否需要调工具
        if response.stop_reason == "tool_use":
            tool_results = []
            for block in response.content:
                if block.type == "tool_use":
                    # 安全校验
                    if block.name not in ALLOWED_TOOLS:
                        tool_results.append({
                            "type": "tool_result",
                            "tool_use_id": block.id,
                            "content": f"Error: 不允许的工具 {block.name}",
                            "is_error": True
                        })
                        continue

                    try:
                        result = execute_tool(block.name, block.input)
                        tool_results.append({
                            "type": "tool_result",
                            "tool_use_id": block.id,
                            "content": str(result)
                        })
                    except Exception as e:
                        tool_results.append({
                            "type": "tool_result",
                            "tool_use_id": block.id,
                            "content": f"Error: {e}",
                            "is_error": True
                        })

            messages.append({"role": "user", "content": tool_results})
        else:
            # ③ 结束 → 返回最终回复
            return response.content[0].text

    return "达到最大轮次，任务未完成"
```

---

## 6. 工具调用 + Thinking 组合

```python
# ★ Adaptive Thinking 与 Tool Use 天然组合
# Claude 在以下节点自动思考：
# ① 调用工具前 — "需要调哪个工具？为什么？"
# ② 看到工具结果后 — "结果说明了什么？是否还需要更多工具？"
# ③ 最终回复前 — "如何整理工具结果给出最佳回答？"

response = client.messages.create(
    model="claude-opus-4-7",
    max_tokens=8192,
    thinking={"type": "adaptive", "display": "summarized"},
    output_config={
        "effort": "xhigh",                  # ★ Agent 任务用 xhigh
        "task_budget": {                   # ★ 整个循环的 Token 预算（Opus 4.7 beta）
            "type": "tokens",
            "total": 50000                 # 最小 20000
        }
    },
    tools=tools,
    messages=[...]
)
```

**Opus 4.7 + task_budget 的优势：** Claude 获得整个 Agent 循环的"Token 预算"意识 → 更合理地分配思考和调用 → 避免过早放弃或过度循环。

---

> 🎯 **核心要点**：Tool Use 三法则 — **① `stop_reason == "tool_use"` → 执行工具 → `tool_result` 回传（`tool_use_id` 必须匹配）② Agent 循环上限 8 轮 + 工具 Allowlist 分发 ③ Adaptive Thinking + effort xhigh 是最强 Agent 组合**。生产 Agent 必须加：工具白名单校验、轮次上限、is_error 标记（让 Claude 知道失败）。

**下一模块**：[05-流式输出与Computer-Use](05-流式输出与Computer-Use.md) / **返回总览**：[00-总览](00-Claude-API知识体系总览.md)
