# 07 Agent 循环通用架构：框架的底层共识

> 定位：把五大框架剥开——所有单 Agent 框架共享的 agent loop 骨架、终止条件、上下文管理（2026-08 基准）

## 📚 目录

1. [Agent Loop：一切框架的共同骨架](#1-agent-loop一切框架的共同骨架)
2. [循环的三层实现形态](#2-循环的三层实现形态)
3. [工具定义与执行的共同模式](#3-工具定义与执行的共同模式)
4. [终止条件全景](#4-终止条件全景)
5. [上下文管理共识](#5-上下文管理共识)
6. [错误处理共识](#6-错误处理共识)
7. [五框架共同模式映射表](#7-五框架共同模式映射表)

## 1. Agent Loop：一切框架的共同骨架

```text
所有框架（无论状态图/原语/代码执行/harness）底层都是同一个循环：

  1. 接收 prompt + system + 工具定义 + 历史
  2. 模型评估 → 输出文本 和/或 工具调用请求
  3. 框架执行工具、收集结果回传
  4. 重复 2-3（每一轮 = 一个 turn）
  5. 模型输出无工具调用的文本 → 循环结束，返回结果

框架的差别 = 这个循环的"暴露程度"与"附加能力"：
  Claude Agent SDK  循环封装在 query() 内，消息流暴露
  OpenAI Agents SDK Runner 内部循环，五原语附加
  LangGraph         循环显式画成图（节点/边/状态）
  Pydantic AI       agent.run() 内部循环，类型化 I/O 附加
  Smolagents        循环 + 代码执行合并（一次执行多步）
```

> 🎯 **结论**：会手写一遍循环（Function Calling 体系 03 篇），任何框架都只是"循环的托管版"——框架概念迁移时，先找它的循环在哪。

## 2. 循环的三层实现形态

| 形态 | 谁写循环 | 代表 | 适用 |
|------|---------|------|------|
| 手动循环 | 你 | Messages API `while stop_reason == "tool_use"` | 完全控制、无框架依赖 |
| 框架循环 | 框架（runner） | OpenAI Agents SDK / Pydantic AI / Claude Agent SDK | 默认选择 |
| 显式图 | 框架 + 你画图 | LangGraph | 复杂状态流/HITL/分支 |

```text
升级路径：手动循环 → 框架循环（省心）→ 显式图（要控制）
降级理由：框架循环不够用才画图；简单任务用图是过度设计
```

## 3. 工具定义与执行的共同模式

| 模式 | 五框架共同点 | 差异点 |
|------|------------|--------|
| Schema 生成 | 从函数签名/类型标注自动生成 | Pydantic AI 用 Pydantic；Smolagents `@tool`；Claude Agent SDK `@tool`+JSON Schema |
| 描述即触发条件 | description 决定何时调用 | 2026 共识：写"何时用"而非"干什么"（工程化模块 02 篇工具契约） |
| 错误回传 | 失败以 `is_error` 结构化回传，模型自救 | 框架都支持；`retry: false` 防死循环是业务层职责 |
| 并行执行 | 只读工具并发、改状态工具串行 | Claude Agent SDK 用 `readOnlyHint`；OpenAI 并行默认开 |
| 权限拦截 | 执行前钩子/批准回调 | LangGraph interrupt；Claude Agent SDK hooks/canUseTool；OpenAI guardrails |
| 幂等 | 重试不产生副作用 | 框架不保证——业务层实现（工程化模块 02 篇契约） |

## 4. 终止条件全景

| 条件 | 触发 | 处理 |
|------|------|------|
| 正常结束 | 模型输出无工具调用（`end_turn`） | 返回最终结果 |
| 轮次上限 | `max_turns` / `max_steps` | 返回部分结果或 resume |
| 成本上限 | `max_budget_usd` | 返回部分结果 |
| 输出超限 | `max_tokens` | 提高上限或流式 |
| 拒绝 | `refusal`（安全分类器） | 检查 stop_details，走 fallback（见 Function Calling 体系） |
| 暂停 | `pause_turn`（服务端工具迭代上限） | 追加 assistant 轮次重发，服务端自动续 |
| 死循环 | 同一工具连续调用 | 框架上限兜底 + 业务层 `retry:false` |

> ⚠️ **生产必配**：轮次上限与成本上限在所有框架里都是"默认不设"——不设 = 开放式 prompt 跑到天荒地老（工程化模块 11 篇治理呼应）。

## 5. 上下文管理共识

```text
共识 1：上下文窗口会话内不重置——历史持续累积
共识 2：稳定前缀自动 prompt caching（system + 工具定义）
  → 工具增删/排序变化 = 缓存失效（工程化模块 07 篇铁律）
共识 3：长会话自动压缩（摘要旧历史）——Claude Agent SDK compact_boundary、
      Pydantic AI OpenAICompaction/AnthropicCompaction
共识 4：工具按需加载（tool search / defer_loading）——大工具集不预载全量 schema
共识 5：子 Agent 隔离——子任务新开上下文，主上下文只收摘要/最终报告

策略排序（2026）：工具精选 > 子 Agent 隔离 > 自动压缩 > 手动摘要
```

## 6. 错误处理共识

```text
1. 工具错误结构化回传（is_error + 可读信息）→ 模型能自救
2. 网络/限流错误：SDK 层指数退避重试（429/5xx）
3. 框架层不吞错误：query() 抛异常、runner 报错、图节点抛错——都要 catch
4. 审批拒绝：模型收到"拒绝 + 理由"消息后调整方案（HITL 四决策之一）
5. 降级链：主模型失败 → fallback 模型 → 转人工（工程化模块 11 篇）
```

## 7. 五框架共同模式映射表

| 概念 | LangGraph | OpenAI Agents SDK | Smolagents | Pydantic AI | Claude Agent SDK |
|------|-----------|------------------|-----------|-------------|------------------|
| 循环载体 | StateGraph 显式图 | Runner 内部 | Agent 内部 | agent.run() | query() 消息流 |
| 工具定义 | @tool/langchain tools | function_tool | @tool | @agent.tool | @tool + MCP |
| 终止上限 | recursion_limit | max_turns（SDK 无原生→自配） | max_steps | max_turns | max_turns/max_budget_usd |
| 人工介入 | interrupt()/Command(resume) | guardrails/handoff 人工 Agent | 无原生（回调） | 无原生（deps 回调） | hooks + canUseTool + AskUserQuestion |
| 持久化 | checkpointer（一等公民） | Sessions 多后端 | 无（自建） | message_history（轻） | session_id resume/fork |
| 长期记忆 | Store/BaseStore | 无内置 | 无内置 | 无内置（外部） | memory（AgentDefinition）/外部 |
| 可观测 | LangSmith/OTel | Tracing 内置（可换 OTel） | 无内置 | Logfire/OTel 内置 | 事件流 + OTel |
| MCP | 原生支持 | 一等支持 | 工具层接入 | 原生客户端 | 原生 + 进程内 server |
| 压缩 | 无原生（自建） | 无原生 | 无原生 | OpenAI/Anthropic Compaction | 自动压缩原生 |
| 结构化输出 | 模型层 schema | output_type（Pydantic 可选） | 无强制 | 一等公民 | output_format json_schema |

> 💡 **读表方法**：选框架 = 看你最在意的那一行（持久化→LangGraph；类型→Pydantic AI；现成 harness→Claude Agent SDK；轻量→OpenAI SDK；代码任务→Smolagents），其余行用工程手段补齐。

---

## 【参考来源】

- [Claude Agent SDK: How the agent loop works（官方）](https://code.claude.com/docs/en/agent-sdk/agent-loop)
- [futureagi.com: What is LangGraph? Stateful Agent Graphs Explained in 2026](https://futureagi.com/blog/what-is-langgraph-2026/)
- [futureagi.com: What is the OpenAI Agents SDK? Loops and Handoffs in 2026](https://futureagi.com/blog/what-is-openai-agents-sdk-2026/)
- [Pydantic: Pydantic AI v2: capabilities, a leaner core, and the Harness](https://pydantic.dev/articles/pydantic-ai-v2)
- [morphllm.com: AI Agent Frameworks (2026 Update)](https://www.morphllm.com/ai-agent-framework)
- 循环原理基础：见 [Function Calling 体系 03 篇](../../../02-大模型基础与Prompt工程/Function%20Calling%20函数调用【Agent%20基石】/03-调用循环工程.md)

---

**返回总览**：[00-总览：单 Agent 框架知识体系](00-总览：单%20Agent%20框架知识体系.md)

**下一模块**：[08-跨框架能力对比：HITL、持久化、可观测、MCP](08-跨框架能力对比：HITL、持久化、可观测、MCP.md)
