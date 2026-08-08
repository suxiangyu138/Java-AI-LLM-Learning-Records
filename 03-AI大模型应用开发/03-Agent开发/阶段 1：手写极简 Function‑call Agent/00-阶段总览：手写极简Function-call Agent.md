# 00 阶段总览：手写极简 Function-call Agent

> 定位：五阶段学习路径第一站——不靠框架，用 OpenAI 兼容 API 手写一个最小可用的 Function-call Agent 循环，把每个字节都看懂（2026-08 基准）

## 📚 目录

1. [本阶段在五阶段路径中的位置](#1-本阶段在五阶段路径中的位置)
2. [学习目标与验收标准](#2-学习目标与验收标准)
3. [前置要求](#3-前置要求)
4. [知识体系导图](#4-知识体系导图)
5. [模块导航](#5-模块导航)
6. [与关联知识体系的分工](#6-与关联知识体系的分工)
7. [2026 版本窗口](#7-2026-版本窗口)

## 1. 本阶段在五阶段路径中的位置

```text
阶段 1：手写极简 Function-call Agent（本阶段）── 亲手实现核心循环，理解协议
    ↓
阶段 2：原生 OpenAI-style Function-call ──────── 深入原生能力（并行/流式/strict/结构化输出）
    ↓
阶段 3：学习 LangChain Agent（作为工程工具）─── 用框架替代手写，对比优缺点
    ↓
阶段 4：问题调优与工程化 ─────────────────────── 失败模式、记忆、评估、生产化
    ↓
阶段 5：进阶 ─────────────────────────────────── 多 Agent、MCP、复杂架构
```

> 🎯 **本阶段的核心价值**：阶段 3 用框架时，你能看穿框架的每一层封装；阶段 4 调优时，你知道问题出在协议层还是应用层。**手写一遍 = 拿到 Agent 的底层心智模型**。

## 2. 学习目标与验收标准

### 2.1 目标

| 目标 | 说明 |
|------|------|
| 理解协议 | tools 声明 → 模型返回 tool_calls → 执行 → tool 消息回传 的完整协议 |
| 手写循环 | 不依赖 LangChain 等框架，实现完整 Agent 循环 |
| 踩过坑 | 亲自踩：tool_call_id 不匹配、arguments 坏 JSON、thinking 回传、消息追加遗漏 |
| 能调试 | 遇到报错能定位是协议问题还是代码问题 |

### 2.2 毕业检查单（全部通过才进阶段 2）

| # | 检查项 |
|:---:|--------|
| 1 | 能解释 tool_calls 协议五步（声明→解析→执行→回传→再生成） |
| 2 | 手写循环跑通"用户提问 → 模型调工具 → 工具结果回传 → 最终回答" |
| 3 | 循环有终止条件（无 tool_calls 即停 + 最大轮数兜底） |
| 4 | 处理过 arguments 解析失败（坏 JSON） |
| 5 | 处理过工具执行抛异常（错误转 JSON 回传） |
| 6 | 知道 tool_call_id 必须原样使用 |
| 7 | 完成实战项目（多工具命令行助手）并通过四验收 |
| 8 | 能说出阶段 2 要学什么（原生能力的边界在哪） |

## 3. 前置要求

| 前置 | 程度 | 说明 |
|------|------|------|
| Python 基础 | 熟练 | 函数、字典、while 循环、try/except |
| HTTP/API 基础 | 了解 | 知道请求/响应、JSON 格式 |
| 大模型 API 经验 | 少量 | 调过一次 chat/completions 即可 |
| 本仓库理论体系 | 可选 | 可先读 [Function Calling 函数调用【Agent 基石】](../../02-大模型基础与Prompt工程/Function%20Calling%20函数调用【Agent%20基石】/00-FunctionCalling知识体系总览.md) 的 01/02 篇加深理解 |

## 4. 知识体系导图

```text
阶段 1：手写极简 Function-call Agent
│
├── 00 阶段总览（本文件）
│
├── 01 环境准备与第一声对话 ── OpenAI 兼容 API / DeepSeek V4 / SDK vs requests / key 管理
│
├── 02 工具声明：tools 与 JSON Schema ── 三要素 / strict / 描述纪律 / 常见坑
│
├── 03 模型返回：tool_calls 解析 ── finish_reason / 结构解剖 / 消息追加铁律 / 并行调用
│
├── 04 Agent 循环 v1：手写核心循环 ── 完整可运行代码 / 终止条件 / thinking 回传坑
│
├── 05 工具执行层：注册表与错误处理 ── 注册表 / 参数校验 / 异常转 JSON 回传
│
├── 06 健壮性工程：重试超时与流式 ── 429 退避 / 超时 / 流式 tool_calls 拼接
│
├── 07 实战项目：命令行智能助手 ── 多工具端到端 / 四验收 / 扩展任务
│
└── 08 阶段自测与衔接 ── 自测题 / 报错速查表 / 毕业检查单 / 通往阶段 2
```

## 5. 模块导航

| 序号 | 模块 | 核心内容 | 学习时间 |
|:---:|------|---------|:---:|
| 01 | 环境准备与第一声对话 | API 对接、SDK、key 安全、首个 chat 请求 | 0.5h |
| 02 | 工具声明 | tools 数组、JSON Schema、strict、描述三原则 | 0.5h |
| 03 | 模型返回解析 | tool_calls 结构、消息追加铁律、并行 | 0.5h |
| 04 | Agent 循环 v1 | 完整手写循环、终止条件、thinking 坑 | 1h |
| 05 | 工具执行层 | 注册表、校验、异常处理 | 0.5h |
| 06 | 健壮性工程 | 重试、超时、流式拼接 | 1h |
| 07 | 实战项目 | 多工具命令行助手端到端 | 2h |
| 08 | 自测与衔接 | 自测题、报错速查、毕业检查单 | 0.5h |

## 6. 与关联知识体系的分工

| 体系 | 分工 | 本阶段怎么用 |
|------|------|------------|
| [Function Calling 函数调用【Agent 基石】](../../02-大模型基础与Prompt工程/Function%20Calling%20函数调用【Agent%20基石】/00-FunctionCalling知识体系总览.md)（11 篇理论） | 协议全景、设计规范、生产化、面试 | 本阶段是"动手版"——同题不同侧重点；理论篇讲"为什么"，本阶段讲"怎么写" |
| [Tool 工具开发与注册](../Agent%20子组件专项学习/)（Agent 子组件） | 工具侧深度（设计/注册/安全） | 阶段 1 只用最小注册表，深化见该体系 |
| [幻觉、格式错误、安全、token 超限](../幻觉、格式错误、安全、token%20超限/00-Agent四大失败模式知识体系总览.md)（11 篇失败模式） | 四类失败原理与治理 | 阶段 1 只需"踩坑并知道"，系统治理在阶段 4 |

## 7. 2026 版本窗口

> 本阶段以 2026-08 为基准：
>
> - **DeepSeek V4 API**（2026-04-23 上线）：`deepseek-v4-pro` / `deepseek-v4-flash` / `deepseek-v4-r1`，完全兼容 OpenAI SDK（只需改 `base_url`）
> - **OpenAI Chat Completions** `tools`/`tool_calls` 仍是"最小实现"的规范模式（2026 教程一致采用）；Responses API 是另一条更面向 Agent 的新路线，不在本阶段范围
> - **thinking 模式**：DeepSeek V4 多轮工具调用时 `reasoning_content` 必须回传（详见 04 篇），V4-Flash 原生支持并行工具调用
> - OpenAI SDK 1.x（`openai>=1.0`），Chat Completions 为 2026 稳定主接口

## 【参考来源】

- OpenAI 官方 Cookbook：How to call functions with chat models
- DeepSeek V4 API Guide（apidog.com，2026-04）
- dev.to: Building Production AI Agents with DeepSeek V4 API（2026）
- floatboat.ai: DeepSeek Agent Function Calling Hands-On Guide
- CAMEL PR #4026：DeepSeek V4 thinking mode tool calls 的 reasoning_content 回传要求

---

**下一模块**：[01-环境准备与第一声对话](01-环境准备与第一声对话.md)
