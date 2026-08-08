# 00 阶段总览：原生 OpenAI-style Function-call

> 定位：五阶段学习路径第二站——把手写循环升级为"原生能力驱动"：Structured Output、并行工程化、流式全组合、Responses API 新路线（2026-08 基准）

## 📚 目录

1. [本阶段在五阶段路径中的位置](#1-本阶段在五阶段路径中的位置)
2. [学习目标与验收标准](#2-学习目标与验收标准)
3. [前置要求](#3-前置要求)
4. [知识体系导图](#4-知识体系导图)
5. [模块导航](#5-模块导航)
6. [与关联体系的分工](#6-与关联体系的分工)
7. [2026 版本窗口](#7-2026-版本窗口)

## 1. 本阶段在五阶段路径中的位置

```text
阶段 1：手写极简 Function-call Agent ──── 已会：协议五步、核心循环、注册表、重试
    ↓
阶段 2：原生 OpenAI-style Function-call（本阶段）── 原生能力全面掌握
    │     Structured Output / JSON 提取 / 并行工程化 / 流式全组合 / Responses API
    ↓
阶段 3：学习 LangChain Agent ──────────── 用框架替换手写，对比原生 vs 框架
    ↓
阶段 4：问题调优与工程化 ──────────────── 失败模式治理、记忆、评估、生产化
    ↓
阶段 5：进阶 ─────────────────────────── 多 Agent、MCP、复杂架构
```

> 🎯 **本阶段核心价值**：阶段 1 解决"能跑"，本阶段解决"跑得专业"——格式保证从"prompt 建议"升级为"协议约束"（Structured Output），速度从串行升级为并行，体验从整包返回升级为流式，接口从对话模型升级为 Agent 运行时（Responses）。

## 2. 学习目标与验收标准

### 2.1 目标

| 目标 | 说明 |
|------|------|
| 原生能力地图 | 掌握 Chat Completions 原生能力的完整边界（9 大升级点） |
| Structured Output | 会用 strict 模式 + `.parse()`，懂其硬约束与局限 |
| 并行工程化 | 会用 asyncio 并发执行工具，能正确做依赖分析与结果配对 |
| 流式全组合 | 流式 + 工具 + 并行组合下的正确拼装 |
| Responses API | 理解新路线：对比、迁移、选型 |

### 2.2 毕业检查单

| # | 检查项 |
|:---:|--------|
| 1 | 能用 `.parse()` + Pydantic 做结构化输出，不用手写 `json.loads` |
| 2 | 能说出 strict 模式的 5 条硬约束（全必填/无附加/顶层对象/深度/无 $ref） |
| 3 | 用 asyncio.gather 并行执行过独立工具调用，按 id 配对结果 |
| 4 | 流式 + 工具调用组合跑通（按 index 拼装 + 增量累积） |
| 5 | 会处理 refusal（拒绝）与截断场景 |
| 6 | 能画出 Chat Completions vs Responses 的对照表并给出选型理由 |
| 7 | 完成实战项目并通过验收 |

## 3. 前置要求

| 前置 | 程度 |
|------|------|
| 阶段 1 全部内容 | 毕业通过（见阶段 1 的 08 篇检查单） |
| Python asyncio | 基础了解（`async/await`、`asyncio.gather`） |
| Pydantic v2 | 基础模型定义（`BaseModel`、字段类型） |

## 4. 知识体系导图

```text
阶段 2：原生 OpenAI-style Function-call
│
├── 00 阶段总览（本文件）
│
├── 01 原生能力全景 ── 9 大升级点 / 与阶段 1 对照 / 能力地图
│
├── 02 Structured Output 深度 ── strict 硬约束 / null-union / .parse() / refusal
│
├── 03 JSON 提取模式与工具组合 ── JSON Mode vs json_schema / tool_choice 强制 / 组合矩阵
│
├── 04 并行调用工程化 ── parallel_tool_calls / asyncio.gather / 依赖分析 / 结果配对
│
├── 05 流式全组合 ── 流式+工具+并行 / 事件拼装 / partial JSON 解析
│
├── 06 SDK 高级参数全解 ── 采样参数 / seed / 频率惩罚 / thinking 组合 / 成本控制
│
├── 07 Responses API：新路线全解 ── 架构对比 / previous_response_id / 内置工具 / 迁移
│
└── 08 实战项目与阶段验收 ── 原生能力组合项目 / 验收 / 衔接阶段 3
```

## 5. 模块导航

| 序号 | 模块 | 核心内容 | 学习时间 |
|:---:|------|---------|:---:|
| 01 | 原生能力全景 | 9 大升级点、对照表 | 0.5h |
| 02 | Structured Output 深度 | strict 硬约束、Pydantic 集成、refusal | 1h |
| 03 | JSON 提取模式 | JSON Mode 对比、提取场景、组合矩阵 | 0.5h |
| 04 | 并行调用工程化 | asyncio、依赖分析、部分失败 | 1h |
| 05 | 流式全组合 | 事件拼装、partial JSON | 1h |
| 06 | SDK 高级参数 | 采样参数全解、thinking 组合 | 0.5h |
| 07 | Responses API | 新路线全解、迁移、选型 | 1.5h |
| 08 | 实战项目与验收 | 组合项目、毕业检查 | 2h |

## 6. 与关联体系的分工

| 体系 | 分工 | 本阶段怎么用 |
|------|------|------------|
| [Function Calling 函数调用【Agent 基石】](../../02-大模型基础与Prompt工程/Function%20Calling%20函数调用【Agent%20基石】/00-FunctionCalling知识体系总览.md) | 协议全景/工具设计/生产化理论 | 本阶段是"原生 API 能力实战版"；理论篇 06-08 与本阶段 02-07 对照学习 |
| [幻觉、格式错误、安全、token 超限](../幻觉、格式错误、安全、token%20超限/00-Agent四大失败模式知识体系总览.md) | 失败模式治理 | 本阶段 02 讲"如何不发生格式错误"；04 篇讲"发生了怎么办" |
| 阶段 1 手写极简 Agent | 协议与循环基本功 | 本阶段所有能力都在阶段 1 骨架上生长 |

## 7. 2026 版本窗口

> 本阶段以 2026-08 为基准：
>
> - **Structured Outputs（strict）**：Chat Completions `response_format: {"type": "json_schema", "strict": true}`；实测 ~99.7% 语法 / ~99.5% schema 合规；模型物理上无法生成违规 token（约束解码）
> - **Responses API**：OpenAI 已将其设为 Agent 应用**推荐默认**；Chat Completions 无限期支持；Assistants API 2026 上半年弃用（一源称 2026-08-26 停服）
> - **DeepSeek V4**：兼容 OpenAI 协议（阶段 1 已验证）；本阶段的 strict/parse 为 OpenAI 专属能力，DeepSeek 用兼容形态（见 02 篇兼容矩阵）
> - **并行调用**：2026 实测并行工具调用延迟降 1.6-1.8×（DynAMO）；asyncio.gather 为生产推荐（ThreadPoolExecutor 受 GIL 限制）
> - 本阶段代码以 OpenAI `gpt-4o-mini` 或 DeepSeek V4 双轨示例

## 【参考来源】

- respan.ai: OpenAI Structured Outputs vs JSON Mode（2026）
- ossaihub: Structured Outputs Strict Mode Code Starter
- cadence.withremote.ai: Structured outputs in production: lessons learned
- callsphere.ai: JSON Schemas + Structured Outputs on OpenAI and Anthropic（2026）
- parallel.ai / microsoft learn: Responses API vs Chat Completions 迁移
- agno.com: OpenAI agents now default to the Responses API
- arXiv 2606.19382: DynAMO（并行调度 1.6-1.8× 收益）
- The Neural Base: Parallel tool calls（依赖分析、ID 配对、部分失败）
- arXiv 2605.15077: Future-based Asynchronous Function Calling

---

**下一模块**：[01-原生能力全景](01-原生能力全景.md)
