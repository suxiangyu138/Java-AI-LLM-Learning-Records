# Postman 基础入门与核心界面

> 🔰 从零开始掌握 Postman 界面布局、HTTP 请求构建、响应解析、常用功能面板 —— 基础操作的完整指南

---

## 📚 目录

1. [Postman 是什么](#1-postman-是什么)
2. [界面布局](#2-界面布局)
3. [构建 HTTP 请求](#3-构建-http-请求)
4. [请求参数详解](#4-请求参数详解)
5. [响应解析](#5-响应解析)
6. [快捷键速查](#6-快捷键速查)

---

## 1. Postman 是什么

```text
Postman = API 开发全生命周期管理平台

核心能力：
  ├── API 客户端      → 发送 HTTP 请求，查看响应
  ├── 自动化测试      → 脚本断言 + 批量运行 + CI/CD 集成
  ├── Mock Server    → 前端无后端也能联调
  ├── API 文档       → 自动生成漂亮的接口文档
  ├── API 监控       → 定时检查 API 可用性与性能
  └── 团队协作       → 共享接口、环境、测试脚本

竞品对比：
  Insomnia      → 更轻量，设计简洁，开源
  Hoppscotch    → Web 版，开源免费，功能较基础
  Apifox        → 国产，集成 Mock + 文档 + JMeter
  Bruno         → 本地优先，Git 友好，开源
```

---

## 2. 界面布局

```text
┌─────────────────────────────────────────────────────────┐
│  Header: 导航栏                                          │
│  [Workspace] [Collections] [APIs] [Environments] [+]    │
├───────────────┬─────────────────────────────────────────┤
│               │                                         │
│  左侧边栏      │       主工作区                           │
│               │  ┌─────────────────────────────────┐    │
│  • Collections│  │  GET  [https://api.example.com] │    │
│    ├── Users  │  │  ┌──────────┬──────────────────┤    │
│    ├── Orders │  │  │  Params   │  Authorization  │    │
│    └── ...    │  │  ├──────────┼──────────────────┤    │
│               │  │  │  Headers  │  Body            │    │
│  • APIs       │  │  ├──────────┼──────────────────┤    │
│               │  │  │  Pre-req  │  Tests           │    │
│  • History    │  │  ├──────────┼──────────────────┤    │
│               │  │  │  Settings                   │    │
│               │  │  └─────────────────────────────┘    │
│               │  ┌─────────────────────────────────┐    │
│               │  │        响应面板                   │    │
│               │  │  Status: 200 OK | Time: 45ms    │    │
│               │  │  ┌──────┬──────┬──────┬──────┐ │    │
│               │  │  │ Body │Cookie│Header│Tests │ │    │
│               │  │  ├──────┴──────┴──────┴──────┘ │    │
│               │  │  │ { "id": 1, "name": "..." }  │    │
│               │  └─────────────────────────────────┘    │
│               │                                         │
├───────────────┴─────────────────────────────────────────┤
│  Footer: 状态栏  Connection | Proxy | Cookie             │
└─────────────────────────────────────────────────────────┘
```

---

## 3. 构建 HTTP 请求

### 3.1 HTTP 方法

| 方法 | 语义 | 幂等 | 安全 | 典型场景 |
|:----:|------|:---:|:---:|---------|
| **GET** | 获取资源 | ✅ | ✅ | 查询用户列表 |
| **POST** | 创建资源 | ❌ | ❌ | 新增用户 |
| **PUT** | 全量更新 | ✅ | ❌ | 更新用户全部字段 |
| **PATCH** | 部分更新 | ❌ | ❌ | 更新用户部分字段 |
| **DELETE** | 删除资源 | ✅ | ❌ | 删除用户 |

### 3.2 请求 URL 结构

```text
完整 URL 结构：

  https://api.example.com:8080/v1/users?page=1&size=20#section
  └─┬─┘ └──────┬──────┘└─┬┘ └──┬──┘ └─────┬──────┘ └──┬──┘
  协议     主机名      端口   路径      查询参数      片段

Postman 中可以使用 {{变量}} ：
  {{baseUrl}}/{{version}}/users → https://api.example.com/v1/users
```

---

## 4. 请求参数详解

### 4.1 Params（查询参数）

```
GET /users?page=1&size=20&status=active

在 Params 标签页以键值对形式填写：
  Key          Value        Description
  page         1            页码
  size         20           每页条数
  status       active       状态过滤
```

### 4.2 Headers

| 常见请求头 | 说明 |
|-----------|------|
| `Content-Type` | 请求体格式：application/json, multipart/form-data |
| `Authorization` | 认证信息：Bearer token, Basic auth |
| `Accept` | 期望的响应格式 |
| `User-Agent` | 客户端标识 |

### 4.3 Body（请求体）

| 格式 | Content-Type | 说明 |
|------|-------------|------|
| **none** | - | GET 请求，无请求体 |
| **form-data** | multipart/form-data | 表单 + 文件上传 |
| **x-www-form-urlencoded** | application/x-www-form-... | 纯表单 |
| **raw** | application/json (最常用) | JSON / XML / Text |
| **binary** | - | 二进制文件 |
| **GraphQL** | application/json | GraphQL 查询 |

```json
// raw → JSON（最常用）
{
    "name": "张三",
    "email": "zhangsan@example.com",
    "age": 25
}
```

---

## 5. 响应解析

### 5.1 响应组成

```text
HTTP 响应：
├── Status Code   → 200, 201, 400, 401, 500...
├── Headers       → Content-Type, Set-Cookie...
├── Body          → JSON / HTML / XML / Binary
├── Cookies       → 服务端设置的 Cookie
├── Time          → 响应耗时（ms）
└── Size          → 响应体大小
```

### 5.2 响应体视图

| 视图 | 说明 |
|------|------|
| **Pretty** | 格式化显示（JSON/XML/HTML 语法高亮） |
| **Raw** | 原始文本 |
| **Preview** | 网页预览（HTML 响应） |
| **Visualize** | 自定义可视化（HTML/CSS/JS 模板渲染） |

### 5.3 常用状态码速查

| 码 | 含义 | 说明 |
|:--:|------|------|
| 200 | OK | 请求成功 |
| 201 | Created | 创建成功 |
| 204 | No Content | 成功但无响应体（DELETE 常见） |
| 301 | Moved Permanently | 永久重定向 |
| 302 | Found | 临时重定向 |
| 400 | Bad Request | 参数错误 |
| 401 | Unauthorized | 未认证 |
| 403 | Forbidden | 无权限 |
| 404 | Not Found | 资源不存在 |
| 500 | Internal Server Error | 服务端错误 |
| 502 | Bad Gateway | 网关错误 |
| 503 | Service Unavailable | 服务不可用 |

---

## 6. 快捷键速查

| 快捷键 | 功能 |
|--------|------|
| `Ctrl + Enter` | 发送请求 |
| `Ctrl + S` | 保存请求 |
| `Ctrl + G` | 跳转到... |
| `Ctrl + /` | 注释/取消注释（脚本中） |
| `Ctrl + D` | 复制当前 Tab |
| `Ctrl + Tab` | 切换 Tab |
| `Ctrl + Shift + N` | 新建请求 |

---

> 🎯 **下一步**：[02-Collection与变量管理](./02-Postman-Collection与变量管理.md) → 学会用变量和集合组织你的 API

---

*创建于：2026年7月*
