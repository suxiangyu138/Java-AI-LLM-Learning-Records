# 00 - FastAPI 知识体系总览

> 🎯 FastAPI 是 Python 生态最快的 API 框架 — 原生异步、自动 OpenAPI、类型安全。AI/LLM 应用（MCP Server、RAG API、Agent 后端）的首选框架

---

## 目录

1. [知识全景](#1-知识全景)
2. [文件导航](#2-文件导航)
3. [学习路线](#3-学习路线)

---

## 1. 知识全景

```
FastAPI 知识体系（9个文件）
│
├── 🏗️ 基础篇（01-02）
│   ├── 01-核心概念与快速上手.md          # 路由/路径参数/查询参数/Dependency Injection/自动文档
│   └── 02-请求与响应模型.md              # Pydantic BaseModel/Field验证/Response Model/嵌套模型
│
├── 🔧 异步篇（03-04）
│   ├── 03-异步编程与并发处理.md           # async/await/coroutine/BackgroundTasks/线程池
│   └── 04-流式响应与实时通信.md           # StreamingResponse/SSE/WebSocket/LLM Token流式输出
│
├── 🗄️ 数据篇（05）
│   └── 05-数据库与ORM集成.md              # SQLAlchemy async/Tortoise-ORM/连接池/迁移
│
├── 🔒 安全篇（06）
│   └── 06-认证与安全.md                  # JWT/OAuth2 Password Flow/API Key/CORS/限流
│
├── 🚀 工程篇（07）
│   └── 07-测试与部署.md                  # TestClient/pytest/Docker/uvicorn-gunicorn/K8s
│
├── 🤖 AI实战篇（08）
│   └── 08-FastAPI-AI应用实战.md           # MCP Server/RAG API/Agent后端/LLM中间件
│
└── 📌 00-FastAPI知识体系总览.md              # ← 本文件
```

---

## 2. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | 知识体系总览 | 全景 + 路线 | — |
| 01 | 核心概念与快速上手 | 路由/路径参数/查询参数/DI/自动文档 | ⭐⭐⭐⭐ |
| 02 | 请求与响应模型 | Pydantic v2/Field/validator/Response Model | ⭐⭐⭐⭐ |
| 03 | 异步编程与并发处理 | coroutine/await/run_in_executor/BackgroundTasks | ⭐⭐⭐⭐ |
| 04 | 流式响应与实时通信 | StreamingResponse/SSE/WebSocket/LLM流式 | ⭐⭐⭐⭐ |
| 05 | 数据库与ORM集成 | SQLAlchemy 2.0 async/连接池/session管理 | ⭐⭐⭐ |
| 06 | 认证与安全 | JWT/OAuth2/API Key/CORS/Rate Limit | ⭐⭐⭐ |
| 07 | 测试与部署 | TestClient/pytest/Docker/gunicorn/K8s | ⭐⭐⭐ |
| 08 | AI应用实战 | MCP Server构建/RAG API/Agent后端/LLM Proxy | ⭐⭐⭐⭐ |

---

## 3. 学习路线

### 🟢 L1：30 分钟上手

```
01-核心概念 → 02-请求响应模型
产出：能写带路由、参数校验、自动文档的 REST API
```

### 🔵 L2：40 分钟深入

```
03-异步编程 → 04-流式响应
产出：理解 async/await、能写 SSE 流式 LLM 输出接口
```

### 🟣 L3：40 分钟工程化

```
05-数据库 → 06-认证安全 → 07-测试部署
产出：能构建生产级 API（JWT + 数据库 + Docker）
```

### 🟡 L4：实战 AI 应用（40 分钟）

```
08-AI应用实战
产出：能搭建 MCP Server、RAG API、Agent 后端服务
```
