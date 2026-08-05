# 00 - Hook 知识体系总览

> 🎯 AI 应用开发中的 Hook（钩子/拦截器）— 两层含义：一层是 AI 平台的 Webhook 事件驱动机制，另一层是 AI Agent 框架中的生命周期拦截器（Middleware/Advisor/Interceptors）。本体系覆盖 Webhook 工程实战 + 三大框架的 Hook 机制深度对比

---

## 目录

1. [知识全景](#1-知识全景)
2. [文件导航](#2-文件导航)
3. [双线分层：Webhook vs Agent Hooks](#3-双线分层webhook-vs-agent-hooks)
4. [与现有 AI 体系的关系](#4-与现有-ai-体系的关系)
5. [学习路线推荐](#5-学习路线推荐)

---

## 1. 知识全景

```
Hook 知识体系（7 个文件 — 双线并行：Webhook 工程 + Agent 框架拦截器）
│
├── 🌐 Webhook 线（01-02）
│   ├── 01-Webhook基础与事件驱动机制.md    # HTTP 回调/签名验证/幂等/重试/安全
│   └── 02-AI平台Webhook实战.md            # Coze/Dify/n8n 三平台 Webhook 集成+对比
│
├── 🧠 Agent 框架 Hook 线（03-05）
│   ├── 03-LangChain Agent Hooks与Middleware.md  # before_model/after_model/wrap_model_call
│   ├── 04-Spring AI Advisor拦截器机制.md         # CallAdvisor/StreamAdvisor/洋葱模型
│   └── 05-Spring AI Alibaba Hooks与Interceptors.md # ModelHook/AgentHook/ModelInterceptor
│
├── 🔥 实战（06）
│   └── 06-Agent Hooks生产实践与框架对比.md        # 四框架横向对比 + 安全护栏 + 成本控制
│
└── 📌 00-Hook知识体系总览.md                       # ← 本文件
```

---

## 2. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | Hook 总览 | 全景导航 + 双线分层 + 学习路线 | — |
| 01 | Webhook 基础 | HTTP 回调/签名(HMAC-SHA256)/幂等/防重放/异步处理 | ⭐⭐ |
| 02 | AI 平台 Webhook 实战 | Coze/Dify/n8n 集成架构 + 统一认证 + 触发器设计 | ⭐⭐⭐ |
| 03 | LangChain Hooks | before_model/after_model/wrap_model_call AgentMiddleware | ⭐⭐⭐ |
| 04 | Spring AI Advisor | CallAdvisor/StreamAdvisor/洋葱模型/ToolCallingAdvisor | ⭐⭐⭐ |
| 05 | Alibaba Hooks+Interceptors | ModelHook/AgentHook/ModelInterceptor/ToolInterceptor | ⭐⭐ |
| 06 | 生产实践与对比 | 四框架横向对比 + 安全护栏 + 成本控制 + 人工介入 | ⭐⭐⭐ |

---

## 3. 双线分层：Webhook vs Agent Hooks

```text
                    ┌─ Webhook（平台层）
                    │   AI 平台通过 HTTP 回调与外部系统集成
                    │   代表：n8n Webhook Trigger / Dify API / Coze Callback URL
                    │   核心：签名验证、幂等、异步、安全
                    │
Hook（钩子）─────────┤
                    │
                    └─ Agent Hooks（框架层）
                        在 Agent/模型/工具调用的生命周期节点插入拦截逻辑
                        代表：LangChain Middleware / Spring AI Advisor / Alibaba Hooks+Interceptors
                        核心：洋葱模型、顺序控制、状态共享、安全护栏
```

| 对比维度 | Webhook | Agent Hooks |
|----------|---------|-------------|
| 层次 | 平台间集成（外部通信） | 框架内拦截（内部生命周期） |
| 协议 | HTTP POST + JSON Payload | 框架 API（Python/Java 接口） |
| 触发方 | 外部系统推送 | Agent 执行流程自动触发 |
| 复杂度 | 网络/安全/重试/幂等 | 状态管理/顺序控制/递归循环 |

---

## 4. 与现有 AI 体系的关系

| 现有系统 | 本体系补充 |
|----------|-----------|
| `Agent Skills/`（8 文件） | Skills 定义工具能力；Hooks 在 Skills 调用前后做拦截（安全校验/重试/护栏） |
| `Loop Engineering/`（12 文件） | 各种循环（ReAct/Tool Calling/自改进）；Hooks 是每个循环节点的可插拔拦截器 |
| `Multi-Agent与MCP协议/` | MCP 提供工具；Hooks 在工具调用时做拦截 |

> 💡 **核心关系**：Agent Skills 定义"做什么"、Loop Engineering 定义"怎么循环"、**Hooks 定义"在什么时候拦截/增强"**。

---

## 5. 学习路线推荐

### 🟢 工程先行（半天）
```
01-Webhook 基础 → 02-AI 平台 Webhook 实战
产出：能搭建 AI 平台 Webhook 集成、知道怎么验证签名/防重放
```

### 🔵 框架纵深（1 天）
```
03-LangChain Hooks → 04-Spring AI Advisor → 05-Alibaba
产出：理解四个框架的 Hook 机制、会写自定义 Middleware/Advisor
```

### 🔴 生产落地（1.5 天）
```
06-生产实践 → 回到 Agent Skills + Loop Engineering 交叉阅读
产出：能在自己的 Agent 项目里加入安全护栏、成本控制、人工审批钩子
```

---

> 🎯 **核心要点**：Hook 在 AI 开发中 = **"在正确的时间点插入正确的逻辑"**。Webhook 在平台层做外部事件驱动、Agent Hooks 在框架层做内部生命周期拦截。2026 年生产级 Agent 的标配 = **安全护栏 Hook + 成本控制 Hook + 人工审批 Hook + 可观测性 Hook**。

**下一模块**：[01-Webhook基础与事件驱动机制](01-Webhook基础与事件驱动机制.md)
