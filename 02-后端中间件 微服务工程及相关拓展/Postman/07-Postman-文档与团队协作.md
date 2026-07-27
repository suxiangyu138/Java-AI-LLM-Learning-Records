# Postman 文档与团队协作

> 📄 API 文档自动生成与发布、Workspace 团队空间、Collection 版本管理、导入导出 —— 让 API 成为团队共享资产

---

## 📚 目录

1. [API 文档生成](#1-api-文档生成)
2. [文档增强技巧](#2-文档增强技巧)
3. [Workspace 团队协作](#3-workspace-团队协作)
4. [版本管理与 Fork](#4-版本管理与-fork)
5. [导入与导出](#5-导入与导出)
6. [Postman API](#6-postman-api)

---

## 1. API 文档生成

### 1.1 自动生成文档

```text
方式：Collection 详情页 → "View documentation"

自动从 Collection 提取：
  ├── 请求方法 + URL + 描述
  ├── 请求参数（Headers / Query Params / Body）
  ├── 请求示例（Example）
  ├── 响应示例（Example）
  └── 脚本说明（Tests / Pre-request Script 注释）

生成后得到文档 URL：
  https://documenter.getpostman.com/view/{{collectionId}}
```

### 1.2 发布与共享

```text
发布选项：
  ├── Public：任何人都能查看（适合 Open API）
  ├── Team：仅团队成员可查看
  └── Private：仅自己可查看

文档中包含：
  ├── 环境选择器（切换 Dev/Staging/Prod 的参数示例）
  ├── 语言选择器（curl / Python / Node.js / Java 代码示例）
  └── "Run in Postman" 按钮（一键导入到 Postman）
```

---

## 2. 文档增强技巧

### 2.1 给请求添加 Markdown 描述

```markdown
<!-- 在请求的 Description 中使用 Markdown -->

# 用户注册接口

## 功能说明
创建新用户账号，支持邮箱和手机号两种注册方式。

## 业务规则
- 用户名 3-20 位，字母数字下划线
- 邮箱需验证唯一性
- 密码至少 8 位，含字母+数字

## 错误码说明
| 错误码 | 说明 |
|-------|------|
| 1001 | 用户名已存在 |
| 1002 | 邮箱已被注册 |
| 1003 | 密码格式不符合要求 |
```

### 2.2 Example 结构化命名

```text
Request Example 命名规范：
  ✅ "正常创建 - 邮箱注册"
  ✅ "正常创建 - 手机号注册"
  ✅ "异常 - 用户名已存在"
  ✅ "异常 - 密码过短"
  ✅ "边界 - 用户名恰好20位"

→ 文档中会显示为清晰的分组下拉列表
→ 前端/测试可以直接看到所有情况的示例
```

### 2.3 文档自定义域名

```text
Postman 付费版支持自定义域名发布文档：
  → https://api-docs.yourcompany.com

文档风格自定义：
  ├── Logo 和品牌色
  ├── 首页介绍
  └── 导航结构
```

---

## 3. Workspace 团队协作

### 3.1 Workspace 类型

| 类型 | 可见性 | 适用 |
|------|:----:|------|
| **Personal** | 仅自己 | 个人项目 |
| **Private** | 受邀成员 | 内部团队 |
| **Team** | 团队成员 | 部门协作 |
| **Partner** | 团队 + 外部伙伴 | 跨组织协作 |
| **Public** | 所有人 | 开源项目 |

### 3.2 团队协作功能

```text
Workspace 内协作功能：

  ├── 实时同步
  │   └── 团队成员修改 Collection → 所有成员自动同步
  │
  ├── 评论
  │   └── 在任何请求上 @同事 进行讨论
  │
  ├── 权限控制
  │   ├── Editor：可编辑 Collection + Environment
  │   └── Viewer：仅查看 + 发送请求
  │
  ├── 活动日志
  │   └── 谁在什么时候修改了哪个接口
  │
  └── 集成
      └── 关联 GitHub / Jira / Slack
```

---

## 4. 版本管理与 Fork

### 4.1 Collection 版本控制

```text
Postman 内置版本管理（非 Git）：

  ├── 每次修改自动保存一个版本点
  ├── 支持版本回滚
  ├── 支持 Changelog 查看差异
  └── 支持 Fork + Pull Request 工作流

Fork 工作流（类似 GitHub）：
  1. 开发者 Fork Collection 到自己的 Workspace
  2. 修改、测试
  3. 提交 Pull Request（Merge Request）
  4. Reviewer 查看差异 → Approve → Merge
  5. 主 Collection 更新
```

### 4.2 Git 集成

```bash
# 导出为 JSON，提交到 Git 仓库
# Collection JSON 结构：
{
  "info": {
    "name": "User Service API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Get Users",
      "request": {
        "method": "GET",
        "url": { "raw": "{{baseUrl}}/users", "host": ["{{baseUrl}}"], "path": ["users"] }
      }
    }
  ],
  "variable": [
    { "key": "baseUrl", "value": "http://localhost:8080" }
  ]
}

# 推荐 Git 管理：
# → Collection JSON + Environment JSON + Globals JSON
# → CI/CD 中使用 Newman 运行
```

---

## 5. 导入与导出

### 5.1 支持的导入格式

| 格式 | 来源 | 说明 |
|------|------|------|
| **Postman Collection JSON** | Postman | 最完整 |
| **OpenAPI / Swagger** | Swagger UI | 业界标准 |
| **cURL** | 终端 | 一键转为 Postman 请求 |
| **RAML / WADL** | 其他 API 规范 | 自动转换 |
| **HAR** | 浏览器 Network | 抓包导入 |

```bash
# cURL 导入示例：直接粘贴这段 → Import → Paste Raw Text
curl -X POST https://api.example.com/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Alice", "email": "alice@test.com"}'

# Postman 自动解析出 Method, URL, Headers, Body！
```

### 5.2 导出场景

```bash
# 导出为 JSON，用于 Git 版本管理 + Newman CI 执行
Collection → ... → Export → Collection v2.1

# 导出为 OpenAPI 3.0
# 需要转换工具，或使用 Postman API 转换
```

---

## 6. Postman API

```text
Postman 提供了丰富的 REST API 来管理 Postman 中的资源：

  GET    https://api.getpostman.com/collections         → 列出所有集合
  GET    https://api.getpostman.com/collections/{{uid}}  → 获取单个集合
  PUT    https://api.getpostman.com/collections/{{uid}}  → 更新集合
  POST   https://api.getpostman.com/mocks               → 创建 Mock Server
  GET    https://api.getpostman.com/monitors             → 列出所有 Monitor
  POST   https://api.getpostman.com/monitors/{{uid}}/run → 手动触发 Monitor

使用场景：
  ✅ 程序化更新 Collection（如自动生成接口文档）
  ✅ CI/CD 动态创建 Mock Server
  ✅ 定时触发 Monitor
```

```bash
# Postman API 示例：运行一个 Monitor
curl -X POST "https://api.getpostman.com/monitors/{{monitorId}}/run" \
  -H "X-API-Key: {{postmanApiKey}}"
```

---

> 🎯 **核心要点**：文档不是"写完接口再补"——利用 Example + Markdown 描述，让文档随 Collection 自动生成。Workspace + Fork + PR 让 API 管理像代码一样严谨。

---

**返回总览**：[00-Postman知识体系总览](./00-Postman知识体系总览.md)

---

*创建于：2026年7月*
