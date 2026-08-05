# 00 - Claude API 知识体系总览

> 🎯 Claude API 是 Anthropic 的 Messages API — 编程/Agent/长文档处理的首选模型。本体系覆盖模型家族、Messages API 协议、Extended Thinking、工具调用、流式处理、生产实践六大模块

---

## 目录

1. [知识全景](#1-知识全景)
2. [文件导航](#2-文件导航)
3. [与 Anthropic兼容接口 体系的分工](#3-与-anthropic兼容接口-体系的分工)
4. [学习路线推荐](#4-学习路线推荐)
5. [核心概念速查](#5-核心概念速查)

---

## 1. 知识全景

```
Claude API 知识体系（7个文件 — 模型→协议→思考→工具→流式→生产）
│
├── 🏗️ 模型（01）
│   └── 01-Claude模型家族与选型.md       # Opus 4.7/4.6·Sonnet 4.6·Haiku 4.5 定位/价格/场景
│
├── 📡 协议（02）
│   └── 02-Messages-API核心协议.md        # POST /v1/messages/结构化消息/参数/响应
│
├── 🧠 思考（03）
│   └── 03-Extended-Thinking深度思考.md   # 自适应思考/effort控制/budget_tokens/成本
│
├── 🔧 工具（04）
│   └── 04-Tool-Use工具调用.md            # 工具定义/执行循环/并行工具/Agent模式
│
├── 📡 流式（05）
│   └── 05-流式输出与Computer-Use.md      # SSE流式/思考流式/Computer Use/视觉
│
├── 🔥 生产（06）
│   └── 06-生产最佳实践与成本优化.md       # 缓存/批处理/模型路由/限流/错误处理
│
└── 📌 00-Claude-API知识体系总览.md        # ← 本文件
```

---

## 2. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | 知识体系总览 | 全景导航 + 学习路线 + 速查 | — |
| 01 | 模型家族与选型 | Opus 4.7/4.6·Sonnet 4.6·Haiku 4.5 价格/性能/场景 | ⭐⭐ |
| 02 | Messages API 核心协议 | 端点/参数/系统提示/上下文/多轮/Token计数 | ⭐⭐⭐ |
| 03 | Extended Thinking | 自适应思考/effort/budget_tokens/成本/可视化 | ⭐⭐⭐ |
| 04 | Tool Use 工具调用 | 工具定义/Agent循环/并行/Web Search/Code Exec | ⭐⭐⭐ |
| 05 | 流式与 Computer Use | SSE/思考流式/Computer Use/视觉/Prompt缓存 | ⭐⭐ |
| 06 | 生产最佳实践 | 缓存/批处理/模型路由/限流/错误/成本优化 | ⭐⭐⭐ |

---

## 3. 与 Anthropic兼容接口 体系的分工

| 对比 | 本体系（Claude API） | Anthropic兼容接口体系 |
|------|---------------------|----------------------|
| 定位 | **Claude 原生 API 深度**（Messages API） | **Anthropic 兼容协议的生态**（兼容服务商/SDK） |
| 内容 | 模型/Thinking/Tool Use/Computer Use/生产 | 兼容服务商配置/协议差异/SDK生态 |
| 关系 | 深度单供应商 | 横向多供应商 |

---

## 4. 学习路线推荐

### 🟢 快速上手（1 小时）
```
01-模型家族 → 02-Messages API基础 → 跑通第一个请求
产出：能用 Python/Java 调通 Claude API
```

### 🔵 开发者深入（半天）
```
03-深度思考 → 04-工具调用 → 05-流式输出
产出：实现 Agent 循环 + 自适应思考 + SSE 流式
```

### 🔴 生产落地（1 天）
```
06-生产最佳实践 → 回顾 03-04 的成本控制
产出：缓存+批处理+模型路由+错误重试的完整方案
```

---

## 5. 核心概念速查

| 概念 | 一句话 |
|------|--------|
| Messages API | `POST /v1/messages` — Claude 的唯一对话端点 |
| Adaptive Thinking | 4.6+ 模型的自适应推理（替换手动 budget_tokens） |
| effort 参数 | `low/medium/high/xhigh/max` 控制推理深度 |
| Tool Use | Claude 返回 `tool_use` block → 客户端执行 → 回传 `tool_result` |
| Computer Use | `computer_20251124` 工具让 Claude 操控桌面 GUI |
| Prompt Caching | 缓存长前缀，读取成本降 90% |
| Batch API | 异步处理，50% 折扣，24 小时内返回 |
| Opus 4.7 | 当前旗舰（$5/$25），1M 上下文，xhigh 推理 |

---

> 🎯 **核心要点**：Claude API 的三条核心线 — **① Messages API（稳定的两角色交替结构）② Adaptive Thinking（4.6+ 自动推理深度）③ Tool Use（Agent 循环的核心引擎）**。2026 年最重要的变化：Opus 4.7 移除了采样参数（temperature/top_p/top_k → 400 报错），adaptive thinking 成为唯一思考模式（手动 budget_tokens → 400 报错）。

**下一模块**：[01-Claude模型家族与选型](01-Claude模型家族与选型.md)
