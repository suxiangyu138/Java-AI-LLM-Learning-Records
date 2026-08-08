# 03 OpenAI Agents SDK：轻量多原语

> 定位：轻量原语范式的代表——Agent/Handoffs/Guardrails/Sessions/Tracing 五个原语半天上手（v0.19.0，2026-08 基准）

## 📚 目录

1. [定位与版本现状](#1-定位与版本现状)
2. [五原语：Agent 与 Runner](#2-五原语agent-与-runner)
3. [Handoffs：任务移交](#3-handoffs任务移交)
4. [Guardrails：输入输出护栏](#4-guardrails输入输出护栏)
5. [Sessions：会话持久化](#5-sessions会话持久化)
6. [Tracing：内置可观测](#6-tracing内置可观测)
7. [2026 新能力与陷阱](#7-2026-新能力与陷阱)

## 1. 定位与版本现状

```text
定位：从实验项目 Swarm 演进而来的官方 Agent 框架
  理念：最小原语集——不替你编排复杂流程，把每个能力做薄做透
  版本：Python v0.19.0（2026-07-27）/ TS @openai/agents v0.12.0
  协议：MIT/Apache 2.0 双许可；~27k stars（2026-06）

重要背景：Assistants API 2026 年中弃用
  → 新项目一律走 Agents SDK 或 Responses API
```

> 🎯 **生态位**：深度绑定 OpenAI 生态、想要"一个下午上手、官方维护、够用就好"的团队。handoff 链天然对应客服分流场景（前台 → 专员 → 升级）。

## 2. 五原语：Agent 与 Runner

### 2.1 Agent 定义

```python
from agents import Agent, Runner

agent = Agent(
    name="OrderAssistant",
    instructions="你是订单助手。查询用 search_orders；退款必须调 refund_apply 并说明理由。",
    model="gpt-5",
    tools=[search_orders, refund_apply],
    handoffs=[refund_agent, human_escalation],   # 见 3 节
)
result = await Runner.run(agent, "查一下订单 #8823")
print(result.final_output)
```

| 字段 | 作用 |
|------|------|
| instructions | system prompt |
| tools | 工具列表（function_tool / hosted tools / MCP） |
| handoffs | 可移交的子 Agent |
| guardrails | 输入/输出护栏（见 4 节） |
| model | 模型（默认绑 OpenAI；AnyLLM/LiteLLM 兼容已改进） |

### 2.2 Runner 与运行

```text
Runner.run(agent, input)     单次运行
Runner.run_streamed(...)     流式（事件：模型输出/工具调用/移交）
run 之间默认无状态 → 用 Sessions（5 节）保留历史
```

> 💡 **为什么轻量**：没有图、没有显式状态机——循环（LLM → 工具 → LLM）由 Runner 内部完成，你的心智负担只剩"Agent 怎么分、怎么交"。

## 3. Handoffs：任务移交

```text
场景：客服分流——意图识别后把任务移交给最合适的子 Agent
机制：Agent A 调用 handoff 工具 → Runner 切换上下文到 Agent B
      B 用 B 的 instructions/tools 继续，结果回到用户

注意：handoff 是"上下文切换"不是"子 Agent 并行"——
  移交后 A 的上下文不再参与；需要并行用 TaskRunner（进阶）
```

```python
triage = Agent(
    name="Triage",
    instructions="分类意图：订单查询转 order_agent，退款申请转 refund_agent，其余自己答。",
    handoffs=[order_agent, refund_agent],
)
```

**选型对照**：单 Agent 简单任务不需要 handoff；多 Agent 场景才用（多 Agent 框架模块会深挖）。

## 4. Guardrails：输入输出护栏

```text
定位：校验层，不是安全替代品（2026 共识：护栏是第二道防线，
     真正的安全在 instructions/工具定义里）

输入护栏：模型调用前检查（提示注入/政策检查）
  可 run_in_parallel（与模型调用并行，不增加延迟）
输出护栏：生成后校验最终输出（如 PII 清洗）
tripwire：护栏触发 → 抛异常中止运行（可捕获转降级）
```

```python
from agents import InputGuardrail, GuardrailFunctionOutput
from pydantic import BaseModel

class SafetyOutput(BaseModel):
    is_injection: bool
    reasoning: str

async def injection_guardrail(ctx, agent, input_data):
    result = await Runner.run(injection_detector, input_data, output_type=SafetyOutput)
    return GuardrailFunctionOutput(
        output_info=result.final_output,
        tripwire_triggered=result.final_output.is_injection,
    )

agent = Agent(..., input_guardrails=[injection_guardrail])
```

> ⚠️ 注意范围：工具护栏主要作用于 `function_tool`；handoff 与 hosted tools 不走同一管线。

## 5. Sessions：会话持久化

```text
机制：Runner 运行前读取历史、运行后写入新消息/工具条目
  自动完成跨 run 的对话连续性

后端（2026）：
  SQLiteSession         本地开发/小应用
  AsyncSQLiteSession    异步版
  RedisSession          多实例共享状态（低延迟）
  SQLAlchemySession     已有关系库
  OpenAI Conversations  托管历史（OpenAI 侧）
  EncryptedSession      加密 + TTL

2026-04-15 更新：sessions 支持 memory 与 snapshots
```

```python
from agents import Agent, Runner, SQLiteSession
session = SQLiteSession(conversation_id="conv-123")   # 或 RedisSession
result = await Runner.run(agent, "刚查的订单，帮我申请退款", session=session)
```

## 6. Tracing：内置可观测

```text
默认开启：记录完整执行工作流
  trace（一次任务）→ spans：
    Task → Agent → Generation / Function / Guardrail / Handoff

默认发往 OpenAI Traces 面板
trace processor 抽象可换后端：
  Datadog / FutureAGI / Langfuse / 任意 OTel（traceai-openai-agents）

2026-08 注意：v0.19.0 强化了对敏感 payload 的脱敏
（Realtime/RunState/MCP 诊断默认不落原始数据）
```

> 与工程化模块 04 篇呼应：Agent 可观测要 trace-first——SDK 内置 tracing 开箱即用，生产只需接 OTel 导出。

## 7. 2026 新能力与陷阱

### 7.1 新能力

| 能力 | 版本 | 说明 |
|------|------|------|
| Programmatic Tool Calling | v0.19.0 | 支持模型生成 JS 协调多个工具（一次调用管一串） |
| Sandbox execution | 2026-04-15 | 原生沙箱执行长任务 |
| MCP 一等支持 | 2026-04-15 | `mcp_servers` 配置 + MCP 工具 |
| AGENTS.md 支持 | 2026-04-15 | 项目级指令文件自动加载 |
| `agents.decorators` | v0.19.0 | 更短的 `@tool` 别名、类型化 settings |

### 7.2 陷阱

| 陷阱 | 说明 |
|------|------|
| OpenAI 优先 | 非 OpenAI 模型兼容有改进但非一等公民；深度绑定走 OpenAI |
| 类型安全弱 | 工具输入输出无 Pydantic 强制（对比 05 篇 Pydantic AI） |
| 复杂流程自担 | 图/状态机/持久化机制需要自己编排（或上 LangGraph） |
| 无内置长期记忆 | sessions 管会话连续性，跨会话知识自建 |
| 版本演进快 | v0.x 仍在 minor 版本快速迭代——锁版本 |

---

## 【参考来源】

- [futureagi.com: What is the OpenAI Agents SDK? Loops and Handoffs in 2026](https://futureagi.com/blog/what-is-openai-agents-sdk-2026/)
- [OpenAI Agents SDK Python releases（v0.19.0）](https://github.com/openai/openai-agents-python/releases/tag/v0.19.0)
- [OpenAI Agents SDK（JS）CHANGELOG](https://github.com/openai/openai-agents-js/blob/main/packages/agents-core/CHANGELOG.md)
- [Blck Alpaca: OpenAI Agents SDK: What It Can Do and When It Fits](https://blckalpaca.at/en/knowledge-base/ai-agents/ai-agent-frameworks-comparison/openai-agents-sdk)
- [DEV.co: OpenAI Agents SDK: Multi-Agent Python Framework](https://dev.co/ai/frameworks/openai-agents-python)
- [openclawhub.tools: OpenAI Agents SDK Review（Praktical multi-agent workflows）](https://openclawhub.tools/tool/openai-agents-sdk/)

---

**返回总览**：[00-总览：单 Agent 框架知识体系](00-总览：单%20Agent%20框架知识体系.md)

**下一模块**：[04-Smolagents：代码执行型 Agent](04-Smolagents：代码执行型%20Agent.md)
