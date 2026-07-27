# 00 - Agent与MCP知识体系总览

> 🎯 Agent 是 LLM 从"聊天"到"干活"的跨越 — 规划+工具+记忆+行动四大组件。MCP 是 Agent 与外部工具的"USB 协议"

---

## 目录

1. [知识全景](#1-知识全景)
2. [文件导航](#2-文件导航)
3. [学习路线](#3-学习路线)

---

## 1. 知识全景

```
Agent与MCP体系（12个文件）
│
├── 🏗️ 基础篇（01-03）
│   ├── 01-Agent概述与核心概念.md          # Agent定义/四大组件/LLM vs Agent/Skill vs Agent
│   ├── 02-Agent架构-规划-记忆-工具-行动.md  # 四大组件深度解析/Agent设计模式
│   └── 03-ReAct模式与Function-Calling.md    # ReAct/Function Calling/Tool Use/OpenAI工具调用
│
├── 🔧 深入篇（04-06）
│   ├── 04-Agent工具系统设计.md             # 工具注册/描述/参数Schema/错误处理/沙箱
│   ├── 05-Agent记忆系统.md                 # 短期/长期/工作记忆/向量记忆/摘要记忆
│   └── 06-Agent规划与推理.md               # Plan-Execute/ReWOO/LLMCompiler/反思机制
│
├── 🚀 工程篇（07-09）
│   ├── 07-多Agent协作模式.md               # 顺序/层级/辩论/群集/多Agent通信
│   ├── 08-LangGraph与Agent框架.md           # LangGraph/AutoGen/CrewAI/Dify对比
│   └── 09-MCP协议深度解析.md                # MCP架构/Client-Server/资源-工具-提示/实战
│
├── 📋 生产篇（10-11）
│   ├── 10-Agent安全-评估与测试.md           # 权限控制/沙箱/注入防御/测试策略/可观测性
│   └── 11-Agent生产实战与面试题.md          # 生产架构/避坑/Java集成/面试题
│
└── 📌 00-Agent与MCP知识体系总览.md           # ← 本文件
```

---

## 2. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | 知识体系总览 | 全景 + 路线 | — |
| 01 | Agent概述与核心概念 | LLM→Agent跃迁/四大组件/Skill vs Agent | ⭐⭐⭐⭐ |
| 02 | Agent架构详解 | 规划/记忆/工具/行动四大组件+设计模式 | ⭐⭐⭐⭐ |
| 03 | ReAct模式与Function-Calling | ReAct/Function Call/OpenAI工具调用实战 | ⭐⭐⭐⭐ |
| 04 | Agent工具系统设计 | 工具注册/Schema/沙箱/错误处理 | ⭐⭐⭐ |
| 05 | Agent记忆系统 | 工作记忆/长期记忆/向量记忆/RAG记忆 | ⭐⭐⭐ |
| 06 | Agent规划与推理 | Plan-Execute/ReWOO/反思/Self-Refine | ⭐⭐⭐ |
| 07 | 多Agent协作模式 | 顺序/层级/辩论/多Agent通信/任务分配 | ⭐⭐⭐ |
| 08 | LangGraph与Agent框架 | LangGraph/AutoGen/CrewAI/Dify对比选型 | ⭐⭐⭐ |
| 09 | MCP协议深度解析 | Client-Server/Resources/Tools/Prompts/Java SDK | ⭐⭐⭐⭐ |
| 10 | Agent安全-评估与测试 | 权限/沙箱/注入/测试策略/可观测性 | ⭐⭐ |
| 11 | Agent生产实战与面试题 | Java集成架构/避坑/15道面试题 | ⭐⭐⭐ |

---

## 3. 学习路线

### 🟢 L1：理解 Agent（30分钟）

```
01-概述 → 02-架构
产出：理解 Agent = LLM + 规划 + 工具 + 记忆 + 行动
```

### 🔵 L2：核心机制（1小时）

```
03-ReAct+Function Calling → 04-工具系统 → 05-记忆系统
产出：能写 Function Calling 代码、理解工具注册和记忆管理
```

### 🟣 L3：进阶技能（1小时）

```
06-规划推理 → 07-多Agent协作 → 08-框架 → 09-MCP
产出：掌握多Agent设计、能用 LangGraph/MCP 搭建 Agent 系统
```

### 🟡 L4：生产+面试（30分钟）

```
10-安全评估 → 11-实战面试
产出：理解安全沙箱、覆盖面试题
```
