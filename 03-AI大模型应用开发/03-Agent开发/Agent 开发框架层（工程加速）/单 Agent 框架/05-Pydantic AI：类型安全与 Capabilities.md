# 05 Pydantic AI：类型安全与 Capabilities

> 定位：类型安全范式的代表——Pydantic 校验一切输入输出，v2 以 Capability 原语重构（v2.26.0，2026-08 基准）

## 📚 目录

1. [定位与版本现状](#1-定位与版本现状)
2. [Agent 类：类型安全的骨架](#2-agent-类类型安全的骨架)
3. [Capability 原语：v2 的核心重构](#3-capability-原语v2-的核心重构)
4. [Dynamic Workflows：子 Agent 即函数](#4-dynamic-workflows子-agent-即函数)
5. [模型接入与 MCP](#5-模型接入与-mcp)
6. [2026 实践要点与陷阱](#6-2026-实践要点与陷阱)

## 1. 定位与版本现状

```text
定位：Pydantic 团队出品——"先让一个 Agent 正确，再谈编排十个"
  哲学：类型安全是第一公民——输入校验、输出校验、流式校验、开发期抓错
  版本：v2.0.0 稳定（2026-06-23）→ v2.26.0（2026-08-06）
  （v1.71.0 引入 capabilities/AgentSpec 后，v2 是 Capability 原语的正式化）

2026 选型实证：多个团队 ADR 选它做默认运行时——
  类型化 I/O、40+ 模型供应商可移植、原生 MCP、OTel/Logfire 内置
```

> 🎯 **生态位**：结构化数据提取、类型安全的 Python 服务、需要"校验过的工具输出"进上下文的场景。多 Agent 深度编排不是它的强项——配 LangGraph/CrewAI 用。

## 2. Agent 类：类型安全的骨架

```python
from pydantic import BaseModel
from pydantic_ai import Agent, RunContext
from pydantic_ai.models.openai import OpenAIModel

class OrderResult(BaseModel):
    order_id: str
    status: str
    items: list[str]

agent = Agent(
    model=OpenAIModel("gpt-5"),
    system_prompt="你是订单助手，必须输出结构化结果。",
    output_type=OrderResult,          # 输出强类型：不符则校验失败
)

@agent.tool
async def search_orders(ctx: RunContext, order_id: str) -> OrderResult:
    """查询订单。order_id 必填。"""
    return OrderResult(order_id=order_id, status="已发货", items=["A", "B"])

result = await agent.run("查一下 #8823")
print(result.output.order_id)         # 类型化输出，无需手解析
```

| 核心要素 | 说明 |
|---------|------|
| `output_type` | 输出强制校验为 Pydantic 模型 |
| `@agent.tool` | 装饰器定义工具；RunContext 注入依赖（DB 会话/配置） |
| `deps` 依赖注入 | `agent.run(prompt, deps=...)` 类型化传递业务依赖 |
| streaming validation | 流式输出边到边校验 |
| `message_history` | 跨调用保持消息历史（无内置长期记忆——外部管理） |

## 3. Capability 原语：v2 的核心重构

```text
Capability = 可复用、可组合的 Agent 行为单元：
  把 工具 + 生命周期 hooks + 指令 + 模型设置 打包成一个类
  → 插到任意 Agent 上，一个概念触达 Agent 每一层

  Agent 可从 YAML/JSON 规格加载：
  AgentSpec / Agent.from_file
  defer_loading=True 的能力保持一行目录项，模型按需加载
```

**内置 Capabilities 全景**：

| 类别 | Capabilities |
|------|-------------|
| 思考 | `Thinking`（跨供应商 extended thinking，可配置 effort） |
| 生命周期 | `Hooks`（装饰器式钩子） |
| 可观测 | `Instrumentation`（OTel/Logfire） |
| 工具 | `WebSearch`、`WebFetch`、`ImageGeneration`、`XSearch`、`MCP`、`ToolSearch`、`NativeTool`、`SelectModel`、`ResolveModelId`、`PrepareTools`、`PrepareOutputTools`、`PrefixTools` |
| 压缩 | `OpenAICompaction`、`AnthropicCompaction`（供应商原生压缩） |
| 持久执行 | `TemporalDurability`、`DBOSDurability`、`PrefectDurability`（跨故障/重启执行） |

> 💡 **设计意图**：把"记忆系统/护栏/成本追踪/审批流"这些横切能力变成插拔件——你要什么插什么，Agent 本体保持干净。

## 4. Dynamic Workflows：子 Agent 即函数

```text
2026 亮点：Code Mode 升级——编排层让模型写普通 Python
  你给编排器一个命名 Agent 目录
  模型把子 Agent 当异步函数调用：fan-out、链式、循环组合
  一次工具调用内完成，只有最后一行返回上下文

  workflow.reveal(agent)  运行中按需暴露 Agent（不破缓存）
  所有运行落在同一条 OTel/Logfire trace 上
```

```python
from pydantic_ai import Agent
from pydantic_ai.workflows import Workflow

order_agent = Agent(...)   # 子 Agent 就是普通 Agent 对象
refund_agent = Agent(...)

workflow = Workflow(agents={"order": order_agent, "refund": refund_agent})
# 模型生成的代码：async def plan(): r = await order("8823"); ...
result = await workflow.run("查订单并走退款")
```

## 5. 模型接入与 MCP

| 能力 | 说明 |
|------|------|
| 40+ 模型供应商 | Anthropic/OpenAI/Gemini/Ollama/本地 llama.cpp 等 |
| `openai:` 前缀 | v2 起走 **Responses API**（`openai-chat:` 才是 Chat Completions）——迁移注意 |
| 原生 MCP 客户端 | `mcp_servers` 直接挂；MCP 工具按需延迟加载 |
| 工具搜索 | `ToolSearch` 按需发现隐藏工具（大工具集不预载） |
| 可观测 | Logfire/OTel 内置（工程化模块 04 篇标准） |

## 6. 2026 实践要点与陷阱

### 6.1 实践要点

```text
- 生产锁版本：v1→v2 有破坏性变更，pre-1.0 语义版本快速演进
- 类型安全是最大杠杆：工具输出校验 → 上下文干净 → 少幻觉少解析错
- 结构化工具输出与 LangGraph 组合：Pydantic AI 管"输出正确"，图框架管"流程正确"
- 无沙箱：不要拿它跑不可信代码（那是 Smolagents 的活）
```

### 6.2 陷阱速查

| 陷阱 | 说明 |
|------|------|
| 版本漂移 | v1.71 → v2.0 破坏性变更（Capability 正式化）；跟 changelog |
| `openai:` 默认 Responses | 旧代码用 `openai:` 期望 Chat Completions 会行为变化 |
| 多 Agent 编排薄 | "让一个 Agent 正确"优先；编排用别的框架 |
| 无沙箱 | 不提供代码执行隔离 |
| Python only | 无 JS 版 |
| 长期记忆自建 | message_history 管会话，跨会话知识外部管理 |

---

## 【参考来源】

- [Pydantic AI v2.0.0 发布说明（2026-06-23）](https://github.com/pydantic/pydantic-ai/releases/tag/v2.0.0)
- [Pydantic AI v2.26.0 发布说明（2026-08-06）](https://github.com/pydantic/pydantic-ai/releases/tag/v2.26.0)
- [Pydantic: Pydantic AI v2: capabilities, a leaner core, and the Harness](https://pydantic.dev/articles/pydantic-ai-v2)
- [Pydantic: Dynamic Workflows in Pydantic AI: agents that orchestrate agents](https://pydantic.dev/articles/dynamic-workflows)
- [rmednitzer/agents: ADR 0001 runtime selection](https://github.com/rmednitzer/agents/blob/main/docs/adr/0001-runtime-selection.md)
- [larsderidder/framework-analysis: Pydantic AI 框架分析](https://github.com/larsderidder/framework-analysis/blob/main/tier-1/pydantic-ai.md)

---

**返回总览**：[00-总览：单 Agent 框架知识体系](00-总览：单%20Agent%20框架知识体系.md)

**下一模块**：[06-Claude Agent SDK：Claude Code 作为库](06-Claude%20Agent%20SDK：Claude%20Code%20作为库.md)
