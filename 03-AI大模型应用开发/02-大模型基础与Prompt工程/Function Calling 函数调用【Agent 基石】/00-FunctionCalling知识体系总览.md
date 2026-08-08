# Function Calling 函数调用【Agent 基石】知识体系总览
> 从"聊天"到"干活"的分水岭：函数调用的原理、协议、工程循环与生产实践

## 📚 目录
1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [体系定位与分工](#4-体系定位与分工)
5. [核心概念速查](#5-核心概念速查)
6. [参考来源](#6-参考来源)

## 1. 知识体系导图

```text
Function Calling 函数调用【Agent 基石】——11 篇（2026-08 基准：OpenAI gpt-5.x / Claude 5 / DeepSeek V4）
│
├─ 认知层 ─────────────────────────────
│   ├─ 00 总览（本文）
│   ├─ 01 原理与演进（为什么需要 FC/与 JSON 提示对比/Agent 基石）
│   └─ 02 API 协议全解（tools 格式/参数全解/三家协议对照）
│
├─ 工程层 ─────────────────────────────
│   ├─ 03 调用循环工程（Agent loop 状态机/终止/错误/并发）
│   ├─ 04 工具定义与设计规范（JSON Schema/描述/粒度/成本）
│   └─ 05 参数校验与安全（模型输出不可信/严格模式/权限/注入）
│
├─ 深化层 ─────────────────────────────
│   ├─ 06 高级模式（并行/强制调用/路由/工具发现/结构化输出）
│   ├─ 07 流式与长会话（SSE 组装/推理模型回传/上下文压缩）
│   └─ 08 主流模型实现对比（OpenAI/Claude/Gemini/DeepSeek/Qwen）
│
├─ 生产层 ─────────────────────────────
│   └─ 09 生产实践与成本优化（prompt caching/监控/测试/避坑）
│
└─ 实战层 ─────────────────────────────
│   └─ 10 实战项目与面试冲刺（客服 Agent/面试题/自测）
```

> 🎯 一句话定位：**Function Calling 是大模型从"对话引擎"变成"执行引擎"的唯一入口**——模型负责"决定调用什么、参数填什么"，你的代码负责"真正执行并回传结果"。本体系覆盖协议细节、循环工程、安全治理与生产成本全链路。

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|---------|---------|
| 00 | [总览（本文）](00-FunctionCalling知识体系总览.md) | 导图、定位、速查 | 所有人 |
| 01 | [原理与演进](01-原理与演进.md) | 为什么需要/JSON 提示对比/演进史/Agent 基石 | 入门必读 |
| 02 | [API 协议全解](02-API协议全解.md) | tools 格式/参数全解/三家协议对照 | 入门必读 |
| 03 | [调用循环工程](03-调用循环工程.md) | Agent loop/终止/错误/并发/日志 | 重点 |
| 04 | [工具定义与设计规范](04-工具定义与设计规范.md) | JSON Schema/描述/粒度/命名/成本 | 重点 |
| 05 | [参数校验与安全](05-参数校验与安全.md) | 不可信输出/严格模式/权限/注入/审计 | 重点 |
| 06 | [高级模式](06-高级模式.md) | 并行/强制调用/路由/工具发现/结构化输出 | 进阶 |
| 07 | [流式与长会话](07-流式与长会话.md) | SSE 组装/推理回传/截断/上下文压缩 | 进阶 |
| 08 | [主流模型实现对比](08-主流模型实现对比.md) | OpenAI/Claude/Gemini/DeepSeek/Qwen 选型 | 进阶 |
| 09 | [生产实践与成本优化](09-生产实践与成本优化.md) | prompt caching/监控/测试/12 避坑 | 进阶 |
| 10 | [实战项目与面试冲刺](10-实战项目与面试冲刺.md) | 客服 Agent 端到端/20 自测/面试题 | 收尾 |

## 3. 学习路线推荐

| 路线 | 人群 | 路径 |
|------|------|------|
| 快速上手（半天） | 会调 API | 01 → 02 → 03 → 10 |
| 完整学习（2 天） | 系统学习 | 00 → 01 → 02 → 03 → 04 → 05 → 06 → 07 → 08 → 09 → 10 |
| 生产进阶 | 已实现过 FC 但踩坑多 | 04 → 05 → 07 → 09 |
| Agent 开发 | 做 Agent/工具链 | 01 → 03 → 05 → 06 → 09（配合 MCP 体系） |

> 💡 完成标志：**能独立实现一个多工具、带校验与安全治理、成本可控的 Agent 循环**，并说清三家协议差异与推理模型的特殊坑——达标后可直接衔接 Multi-Agent/MCP 深化体系。

## 4. 体系定位与分工

```text
与 LLM-API/09-Function Calling与Tool Use深度实战.md 的分工：
  LLM-API 09 = 速查版（平台格式对比 + 统一封装层 + 生产骨架）
  本体系     = 课程执行版（原理深潜 + 每层工程规范 + 安全与成本专题）

与 OpenAI兼容接口 体系的分工：
  兼容接口 = 协议层（OpenAI 格式的兼容实现/网关）
  本体系   = 应用层（在任意兼容/原生协议上构建 Agent 循环）

与 03-Agent与MCP协议 体系的分工：
  Agent 体系 = 上层范式（ReAct/多 Agent/框架）
  本体系     = 基石层（FC 就是 Agent 的"手"，本体系把这只手打磨到生产级）
```

> 🎯 认知起点：**模型永远不执行你的函数**——它只输出"我要调用 get_weather，参数是 {city: '北京'}"的结构化指令。**执行、校验、安全、重试全是你的代码的职责**。这个边界想清楚，整个体系就顺了。

## 5. 核心概念速查

| 概念 | 一句话 |
|------|--------|
| Function Calling | 模型输出结构化"函数调用指令"（名称 + JSON 参数）的能力 |
| tools | 请求里声明的工具列表（name + description + JSON Schema） |
| tool_calls | 模型响应里的调用指令数组（**一个响应可能多个调用**） |
| tool_choice | 调用策略：auto / required / 指定工具 / none |
| Agent loop | 循环：发请求 → 执行工具 → 回传结果 → 再请求，直到模型不再要工具 |
| JSON Schema | 参数结构声明（模型按此生成参数，不保证 100% 遵守） |
| strict 模式 | OpenAI 严格模式：强制参数符合 Schema（有额外规则） |
| parallel_tool_calls | 是否允许一次响应并行调用多个工具 |
| 结构化输出 | 强制模型输出 JSON 的能力（FC 的"提取"形态） |
| 工具结果回传 | 把执行结果作为 tool 消息返回给模型继续推理 |
| 提示注入 | 数据里夹带指令试图劫持模型（工具结果要当不可信输入） |
| 确认-执行模式 | 危险操作先"提议"再"确认"的两段式工具设计 |
| reasoning_content | DeepSeek 思考模式下必须原样回传的思维链字段 |
| prompt caching | 提示词前缀缓存（工具定义是主要缓存对象，读 90% 折扣） |
| 工具发现 | 大量工具时先搜再加载（OpenAI tool_search / Claude defer_loading） |
| MCP | 模型上下文协议（工具的标准分发方式，与 FC 互补） |

## 6. 参考来源

- [OpenAI 官方：Function Calling 指南](https://platform.openai.com/docs/guides/function-calling)
- [OpenAI 官方：Structured Outputs](https://platform.openai.com/docs/guides/structured-outputs)
- [Anthropic 官方：Tool Use 文档](https://docs.anthropic.com/en/docs/build-with-claude/tool-use)
- [Anthropic 官方博客：Prompt caching is everything](https://claude.com/blog/lessons-from-building-claude-code-prompt-caching-is-everything)
- [DeepSeek API 文档（V4 工具调用与 thinking 模式）](https://api-docs.deepseek.com/)
- [Function Calling 2026 生产模式](https://thepromptbench.com/structured-outputs/function-calling-patterns-that-work/)
- [LLM-API/09-Function Calling与Tool Use深度实战.md](../LLM-API/09-Function%20Calling与Tool%20Use深度实战.md)（速查版）
- [03-Agent开发/主流 Agent 范式](../../03-Agent开发/主流%20Agent%20范式/00-主流Agent范式知识体系总览.md)（上层范式）
- [Python 异步 + FastAPI](../../01-Python语言/Python%20异步%20+%20FastAPI/00-Python异步与FastAPI知识体系总览.md)（Agent 服务端工程）

---

**下一模块**：[01-原理与演进](01-原理与演进.md)
