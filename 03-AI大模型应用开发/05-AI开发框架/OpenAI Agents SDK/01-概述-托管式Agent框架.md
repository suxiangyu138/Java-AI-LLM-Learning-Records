# 01 概述：托管式 Agent 框架

> OpenAI Agents SDK 的定位一句话："框架替你跑 Agent 循环"——你定义 Agent、工具、护栏、交接，Runner 托管整个循环直到产出最终输出；与 Responses API 深度集成。

## 📚 目录

1. [OpenAI Agents SDK 是什么](#1-openai-agents-sdk-是什么)
2. [托管式流程的含义](#2-托管式流程的含义)
3. [发展简史：Swarm → SDK](#3-发展简史swarm--sdk)
4. [与主流框架的对比](#4-与主流框架的对比)
5. [适用场景判断](#5-适用场景判断)
6. [面试高频问法](#6-面试高频问法)
7. [常见误区](#7-常见误区)
8. [经典应用场景](#8-经典应用场景)

## 1. OpenAI Agents SDK 是什么

| 维度 | 说明 |
|---|---|
| 出品方 | OpenAI（官方） |
| 定位 | 生产级 Agent 开发框架 |
| 前身 | Swarm（实验框架） |
| 许可 | MIT |
| Python 版本 | v0.19.3（2026） |
| 底层 API | Responses API（深度集成） |
| 规模 | 22k stars |

一句话定义：

> SDK 用三大原语构建 Agent：Agent（指令+工具+护栏）、Handoffs（移交控制权）、Guardrails（输入输出校验）——Runner 在循环中运行直到产出最终输出或交接。

## 2. 托管式流程的含义

### 与传统编排的差异

```
手写循环：自己写 while 循环（模型调用→工具→结果→再调用）
托管循环：Runner 替你跑（定义好 Agent/工具/护栏即可）
```

| 维度 | 手写循环 | 托管式（SDK） |
|---|---|---|
| 循环控制 | 自己写 | Runner 内建 |
| 工具循环 | 自己处理 | 自动（调用→回传→再调用） |
| 流式 | 自己拼 | 一等公民（run_streamed） |
| 追踪 | 自己接 | 每次运行自动生成 trace |
| 错误处理 | 自己写 | 内建（护栏/tripwire） |

### 托管的边界

```
SDK 托管：循环执行、工具调用、流式、追踪
不托管：检索/RAG（需自建或组合 LlamaIndex/Haystack）
```

## 3. 发展简史：Swarm → SDK

| 时间 | 事件 | 意义 |
|---|---|---|
| 2024 | Swarm 实验框架 | "无头"多 Agent 实验（教学用） |
| 2025-03 | **Agents SDK 发布** | Swarm 生产级继任者 |
| 2025 | 快速迭代 | Handoffs/Guardrails/Sessions 成熟 |
| 2026-04 | **Sandbox Agents（Beta）** | 容器化执行环境 |
| 2026 | v0.19.x | Programmatic Tool Calling 等 |

### 与 Swarm 的关系

```
Swarm = 概念验证（handoffs 思想）
SDK = 生产级实现（+Guardrails/Sessions/追踪）
思想延续：handoffs 是核心（04 篇详述）
```

## 4. 与主流框架的对比

| 维度 | OpenAI SDK | LangGraph | AutoGen |
|---|---|---|---|
| 编排范式 | 托管循环 | 图/状态机 | 对话驱动 |
| 核心抽象 | Agent/Runner | 图（节点/边） | 对话参与者 |
| 多 Agent | Handoffs | 图原生 | 群聊 |
| 护栏 | **原生（Guardrails）** | 需自建 | 弱 |
| 会话 | **原生（Sessions）** | 需自建 | 需自建 |
| 流式 | 一等公民 | 支持 | 支持 |
| 追踪 | 内置（仪表盘/OTel） | LangSmith | 需自建 |
| 模型 | Responses API（可 LiteLLM） | 多模型 | 多模型 |
| 学习曲线 | **低（托管）** | 中（图概念） | 中（对话思想） |

### 关键差异

```
SDK = "告诉框架怎么跑"（声明式 + 托管）
LangGraph = "自己画图"（显式控制）
AutoGen = "让模型聊出流程"（对话驱动）
三种编排哲学的代表
```

## 5. 适用场景判断

### 适合 SDK

| 场景 | 为什么 |
|---|---|
| OpenAI 生态项目 | Responses API 深度集成 |
| 快速上手的 Agent 应用 | 托管循环（少写样板） |
| 需要内置护栏 | Guardrails 原生 |
| 多 Agent 交接协作 | Handoffs 一等公民 |
| 流式交互 | 一等公民 |

### 不适合 SDK

| 场景 | 为什么 |
|---|---|
| 复杂状态机/图编排 | LangGraph 更专 |
| 需要检索/RAG | SDK 无检索层（组合 LlamaIndex） |
| 非 OpenAI 生态且要深度定制 | LiteLLM 可接但生态绑定轻 |
| 细粒度循环控制 | 托管意味着"框架说了算" |

### 判断口诀

```
OpenAI 生态 + 托管式 → SDK
显式图/状态机 → LangGraph
对话式多 Agent → AutoGen
RAG 生产 → Haystack（检索可作 SDK 工具）
```

## 6. 面试高频问法

| 问题 | 回答要点 |
|---|---|
| SDK 是什么？ | OpenAI 官方 Agent 框架（Swarm 继任者） |
| 三大原语？ | Agent/Handoffs/Guardrails |
| 托管式流程？ | Runner 跑循环，开发者定义 Agent/工具/护栏 |
| 与 Swarm 关系？ | Swarm 是实验，SDK 是生产级实现 |
| 与 LangGraph 区别？ | 托管循环 vs 显式图 |
| 2026 新能力？ | Sandbox Agents（Beta）、Programmatic Tool Calling |

### 面试加分表达

> "OpenAI Agents SDK 的托管式流程是它最大的差异化：Runner 替你跑 Agent 循环（工具调用、流式、追踪都是内建的），你只需要声明 Agent/工具/护栏/交接。三大原语里 Guardrails 和 Sessions 是其他框架要自建的能力——它是'少写样板、快速上线'路线的代表。"

## 7. 常见误区

| 误区 | 真相 |
|---|---|
| "只是 Swarm 改名" | 生产级重写（护栏/会话/追踪全新增） |
| "锁定 OpenAI" | LiteLLM 100+ Provider |
| "托管 = 全自动" | 循环托管，安全/上限自己配 |
| "不能做 RAG" | 无内置检索，组合检索框架作工具 |
| "API 稳定" | 快速演进（v0.19.x）——锁版本防漂移 |
| "与 LangGraph 互斥" | 哲学不同，按场景选（可混用） |

## 8. 经典应用场景

| 场景 | SDK 的姿势 |
|---|---|
| 客服多 Agent | Handoffs 领域分工（政策/订单/投诉） |
| 代码助手 | 工具调用 + 沙箱执行 |
| 数据问答 Agent | 检索框架作工具（RAG Agent） |
| 自动化流程 | 工具编排 + HITL 审批 |
| 内容工作流 | 流水线 Handoffs（需求→设计→实现） |

### 学习视角（结合本仓库）

```
学 SDK 的正确姿势：
① 原理前置：Function Calling 体系（工具循环）
② 对照手写：每个原语找"手写版本"（03 篇对照表）
③ 框架对比：LangGraph/AutoGen/Haystack/LlamaIndex 体系
④ 组合思维：SDK 编排 + 检索框架工具 + MCP 生态
核心：SDK 托管的是循环，循环的原理在 Function Calling 体系
```

### 场景落地骨架（客服）

```
① 领域 Agent：政策/订单/投诉（各管一段）
② 主 Agent + Handoffs（按问题类型交接）
③ Typed Handoffs 传订单结构化数据
④ 输入护栏（拦截无效请求）+ 输出护栏（PII）
⑤ Session 按用户隔离（多轮）
⑥ Tracing 全链路 + HITL 高危审批
```

> 🎯 核心要点：SDK = OpenAI 官方托管式 Agent 框架（Swarm 继任者，v0.19.3）；三大原语（Agent/Handoffs/Guardrails）；托管循环（Runner）vs 显式图（LangGraph）vs 对话（AutoGen）是三种编排哲学；适合 OpenAI 生态 + 快速上线；无检索层（组合 LlamaIndex/Haystack）；六个误区 + 客服落地骨架校准认知。

---

**下一模块**：[02-核心概念-Agent-Runner-工具](02-核心概念-Agent-Runner-工具.md) / **返回总览**：[00-OpenAI-Agents-SDK知识体系总览](00-OpenAI-Agents-SDK知识体系总览.md)
