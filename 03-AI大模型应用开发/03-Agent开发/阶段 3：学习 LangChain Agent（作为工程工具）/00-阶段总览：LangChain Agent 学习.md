# 00 阶段总览：LangChain Agent 学习

> 定位：五阶段学习路径第三站——用 LangChain 1.0 框架替代手写循环，看懂框架每层封装，学会"手写 vs 原生 vs 框架"三向选型（2026-08 基准）

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
阶段 1：手写极简 Function-call Agent ──── 已会：协议五步、核心循环（60 行）
    ↓
阶段 2：原生 OpenAI-style Function-call ── 已会：strict/并行/流式/Responses
    ↓
阶段 3：学习 LangChain Agent（本阶段）─── 用框架替换手写，对比三向选型
    │     ① 看懂框架封装了什么（= 你手写过的那些）
    │     ② 学会新 API（create_agent）与生态（LangGraph/LangSmith）
    │     ③ 知道何时用框架、何时不用
    ↓
阶段 4：问题调优与工程化 ──────────────── 失败模式治理、记忆、评估、生产化
    ↓
阶段 5：进阶 ─────────────────────────── 多 Agent、MCP、复杂架构
```

> 🎯 **本阶段核心价值**：阶段 1/2 让你拿到了"底层心智模型"，本阶段让你拿到"工程效率"。框架的价值不是"帮你写循环"——那是你 60 行就能写完的——而是**把记忆、持久化、流式、可观测、多 Agent 这些生产能力变成一行配置**。

## 2. 学习目标与验收标准

### 2.1 目标

| 目标 | 说明 |
|------|------|
| 生态地图 | LangChain 1.0 组件体系：core/agents/LangGraph/LangSmith 各自定位 |
| 工具定义 | `@tool` 装饰器、docstring 铁律、schema 自动生成 |
| 官方新 API | `create_agent` 全参数；知道 create_react_agent 已弃用、AgentExecutor 维护期 |
| 底层原理 | 能画出 create_agent 内部的 LangGraph 状态机（agent 节点/tools 节点/条件路由） |
| 生产能力 | 记忆（checkpointer）、流式（stream）、结构化输出（response_format） |
| 三向选型 | 手写 vs 原生 vs 框架的决策矩阵 |

### 2.2 毕业检查单

| # | 检查项 |
|:---:|--------|
| 1 | 用 `@tool` 定义 3 个工具，docstring 遵守三原则 |
| 2 | 用 `create_agent(model, tools, system_prompt)` 跑通 Agent（不是 AgentExecutor） |
| 3 | 能画出 create_agent 内部的状态机结构（agent/tools/条件边） |
| 4 | 说出 create_react_agent 与 AgentExecutor 的 2026 状态（弃用/维护期） |
| 5 | checkpointer 记忆 + agent.stream() 流式都跑通过 |
| 6 | 用 response_format 做过结构化输出 |
| 7 | 完成三向对比表（手写/原生/框架）并给出选型结论 |
| 8 | 实战项目验收通过 |

## 3. 前置要求

| 前置 | 程度 |
|------|------|
| 阶段 1 全部 | 毕业（协议与循环心智） |
| 阶段 2 全部 | 毕业（原生能力心智） |
| Python | 熟练 |
| Pydantic v2 | 基础（阶段 2 已用） |

> 💡 没有阶段 1/2 的底子也能照抄 LangChain 代码，但**你会分不清"框架报错"与"协议问题"**——这也是本阶段排在第三的原因。

## 4. 知识体系导图

```text
阶段 3：LangChain Agent（作为工程工具）
│
├── 00 阶段总览（本文件）
│
├── 01 生态全景：LangChain 1.0 组件地图 ── 五大组件 / 与手写对比 / 版本窗口
│
├── 02 @tool 工具定义 ── 装饰器 / docstring 铁律 / schema 自动生成 / 与注册表对比
│
├── 03 create_agent：官方新 API ── 全参数 / 弃用史（react_agent/AgentExecutor）/ 中间件
│
├── 04 底层原理：LangGraph 状态机 ── 节点/边/条件路由 / create_agent 内部 / 何时下沉
│
├── 05 记忆与持久化 ── checkpointer / store / 跨会话 / 与手写记忆对比
│
├── 06 流式与可观测 ── agent.stream / stream_mode / LangSmith / 调试三板斧
│
├── 07 结构化输出与错误处理 ── response_format / with_structured_output / 循环保护
│
└── 08 实战项目与阶段验收 ── 重写阶段 2 项目 / 三向对比 / 验收 / 衔接阶段 4
```

## 5. 模块导航

| 序号 | 模块 | 核心内容 | 学习时间 |
|:---:|------|---------|:---:|
| 01 | 生态全景 | 五大组件地图、版本窗口 | 0.5h |
| 02 | @tool 工具定义 | 装饰器、docstring 铁律 | 0.5h |
| 03 | create_agent 新 API | 全参数、弃用史、中间件 | 1h |
| 04 | LangGraph 状态机 | 三要素、内部结构、下沉时机 | 1h |
| 05 | 记忆与持久化 | checkpointer/store | 0.5h |
| 06 | 流式与可观测 | stream、LangSmith、调试 | 0.5h |
| 07 | 结构化输出与错误处理 | response_format、循环保护 | 0.5h |
| 08 | 实战项目与验收 | 重写、三向对比、毕业 | 2h |

## 6. 与关联体系的分工

| 体系 | 分工 | 本阶段怎么用 |
|------|------|------------|
| [Function Calling 函数调用【Agent 基石】](../../02-大模型基础与Prompt工程/Function%20Calling%20函数调用【Agent%20基石】/00-FunctionCalling知识体系总览.md) | 协议理论 | 看懂框架底层行为时对照 |
| 阶段 1 手写循环 | 协议与循环心智 | 本阶段所有"框架封装"都能还原成你写过的代码 |
| 阶段 2 原生能力 | 原生 API 能力 | 框架的 bind_tools/response_format 就是原生能力的一层壳 |
| [失败模式体系](../幻觉、格式错误、安全、token%20超限/00-Agent四大失败模式知识体系总览.md) | 失败治理 | 框架的 max_iterations/parsing 容错只是防御的一环，完整治理在阶段 4 |

## 7. 2026 版本窗口

> 本阶段以 2026-08 为基准：
>
> - **LangChain 1.0 GA：2025-10-22**（与 LangGraph 1.0 同步发布）
> - **`create_agent` 是新官方 API**：`from langchain.agents import create_agent`，内部自动生成 LangGraph 状态机
> - **`create_react_agent` 已在 LangGraph v1 弃用**，迁移到 `create_agent`
> - **`AgentExecutor` 维护模式直到 2026-12**：官方明确"Do NOT use for new code"
> - **迁移路径**：AgentExecutor → create_agent（最简）→ StateGraph（需要节点级控制时）
> - LangSmith 为官方可观测平台（本阶段以自建打印为主，LangSmith 入门即可）
> - 本阶段示例模型：`ChatOpenAI`（OpenAI/DeepSeek 兼容端点均可用）

## 【参考来源】

- docs.langchain.com: What's new in LangGraph v1 / LangGraph v1 migration guide
- atlan.com: LangChain vs LangGraph: Key Differences（2026）
- developer.aliyun.com: 从 LangChain 到 LangGraph 构建可控 Agent 的工程实践
- developer.baidu.com: LangChain 1.0 正式发布
- The Neural Base: Streaming agent steps（LangChain Advanced Course）
- microsoft/langchain-for-beginners: Building Agents with create_agent()
- github.com/magnus919/agent-skills: langchain SKILL.md（AgentExecutor 维护期提示）

---

**下一模块**：[01-生态全景：LangChain 1.0 组件地图](01-生态全景：LangChain%201.0%20组件地图.md)
