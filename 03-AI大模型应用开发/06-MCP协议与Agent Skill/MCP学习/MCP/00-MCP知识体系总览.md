# 00 - MCP 知识体系总览

> 🎯 MCP（Model Context Protocol）是 Anthropic 提出的 LLM 与外部工具/数据交互的开放标准协议 — 一次开发 MCP Server，所有 LLM Client 复用

---

## 目录

1. [知识全景](#1-知识全景)
2. [文件导航](#2-文件导航)
3. [学习路线](#3-学习路线)

---

## 1. 知识全景

```
MCP 知识体系（9个文件）
│
├── 🏗️ 基础篇（01-03）
│   ├── 01-MCP协议规范与生命周期.md       # JSON-RPC 2.0/协议生命周期/Capabilities协商
│   ├── 02-Resources资源原语详解.md        # 资源定义/URI模板/内容类型/订阅机制
│   └── 03-Tools工具原语详解.md            # 工具Schema/调用流程/错误处理/进度通知
│
├── 🔧 传输篇（04）
│   └── 04-传输层与通信机制.md             # stdio/HTTP+SSE/Streamable HTTP 三种传输方式
│
├── 🚀 开发篇（05-06）
│   ├── 05-MCP Server开发实战.md           # Python/TypeScript/Java 三语言 Server 开发
│   └── 06-MCP Client开发实战.md           # Client 连接/Session 管理/工具路由
│
├── 📋 进阶篇（07-08）
│   ├── 07-MCP认证与安全.md                # OAuth 2.0/Authorization/传输安全/沙箱
│   └── 08-MCP高级特性与生态.md            # Sampling/Elicitation/Roots/Multi-Server/生态
│
└── 📌 00-MCP知识体系总览.md                 # ← 本文件
```

---

## 2. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | 知识体系总览 | 全景 + 路线 | — |
| 01 | 协议规范与生命周期 | JSON-RPC 2.0/初始化协商/Capabilities/协议版本演进 | ⭐⭐⭐⭐ |
| 02 | Resources 资源原语 | 资源URI模板/内容类型/分页/订阅更新/资源层级 | ⭐⭐⭐ |
| 03 | Tools 工具原语 | 工具Schema/调用-响应/流式输出/错误码/Logging | ⭐⭐⭐⭐ |
| 04 | 传输层与通信机制 | stdio本地/HTTP+SSE远程/Streamable HTTP/传输选型 | ⭐⭐⭐ |
| 05 | Server 开发实战 | Python(fastmcp)/TypeScript(SDK)/Java(Spring AI) 完整示例 | ⭐⭐⭐⭐ |
| 06 | Client 开发实战 | 多Server连接/Session管理/工具发现与路由/重连机制 | ⭐⭐⭐ |
| 07 | 认证与安全 | OAuth 2.0授权码/Token管理/传输加密/输入净化 | ⭐⭐⭐ |
| 08 | 高级特性与生态 | Sampling服务端请求/Elicitation/Roots/Multi-Server编排 | ⭐⭐⭐ |

---

## 3. 学习路线

### 🟢 L1：理解 MCP 是什么（20 分钟）

```
01-协议规范 → 02-Resources → 03-Tools
产出：理解 JSON-RPC、三大原语（Resources/Tools/Prompts）、协议生命周期
```

### 🔵 L2：掌握传输与通信（20 分钟）

```
04-传输层
产出：理解 stdio/HTTP+SSE/Streamable HTTP 的区别与选型
```

### 🟣 L3：能写 MCP Server/Client（1 小时）

```
05-Server开发 → 06-Client开发
产出：能用 Python/TS/Java 写出生产级 MCP Server 和 Client
```

### 🟡 L4：生产落地（30 分钟）

```
07-认证安全 → 08-高级特性
产出：理解 OAuth 认证、掌握多Server编排、融入 Claude Code/IDE 生态
```
