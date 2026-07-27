# 02 - HTTP 方法与状态码选择

> 🎯 方法选错 = 语义混乱、状态码乱用 = 前端不知道该怎么处理 — 掌握 5 个方法 + 10 个状态码，覆盖 95% 的接口

---

## 目录

1. [HTTP 方法选择指南](#1-http-方法选择指南)
2. [状态码选择指南](#2-状态码选择指南)
3. [经典场景映射表](#3-经典场景映射表)
4. [常见错误用法](#4-常见错误用法)

---

## 1. HTTP 方法选择指南

| 方法 | 含义 | 幂等 | 请求体 | 响应体 |
|------|------|:---:|:---:|:---:|
| **GET** | 获取资源 | ✅ | ❌ | ✅ |
| **POST** | 创建资源 | ❌ | ✅ | ✅ (201) |
| **PUT** | 全量替换 | ✅ | ✅ | ❌ (204) |
| **PATCH** | 部分更新 | ❌ | ✅ | ❌ (204) |
| **DELETE** | 删除资源 | ✅ | ❌ | ❌ (204) |

### 场景决策

```text
获取用户列表     → GET    /users
获取用户详情     → GET    /users/1
创建用户         → POST   /users          (Body: 用户数据)
全量替换用户     → PUT    /users/1         (Body: 完整用户数据)
部分更新用户     → PATCH  /users/1         (Body: 仅变更字段)
删除用户         → DELETE /users/1
搜索用户         → GET    /users?keyword=张  (非 POST /users/search)

取消订单（非CRUD） → POST   /orders/1/cancel
```

### PUT vs PATCH vs POST

```text
PUT: 全量替换 → 需要传所有字段，未传字段设为 null
  PUT /users/1   { "name": "张三", "age": null, "email": null }
  ↑ 如果只传 name，age 和 email 会被清空！

PATCH: 部分更新 → 只传要改的字段
  PATCH /users/1  { "name": "张四" }
  ↑ age 和 email 保持不变

POST: 创建新资源 → 服务端分配 ID
  POST /users  { "name": "新用户" }
  → 返回 201 + Location: /users/3
```

---

## 2. 状态码选择指南

### 只记这 10 个

| 状态码 | 含义 | 何时用 |
|:---:|------|------|
| **200** | OK | GET/PUT/PATCH 成功返回数据 |
| **201** | Created | POST 创建资源成功 |
| **204** | No Content | DELETE 成功 / PUT 不返回数据 |
| **301** | 永久重定向 | URL 永久变更 |
| **400** | Bad Request | 参数错误/校验失败 |
| **401** | Unauthorized | 未登录/Token 缺失 |
| **403** | Forbidden | 已登录但无权限 |
| **404** | Not Found | 资源不存在 |
| **409** | Conflict | 资源冲突（如重复创建） |
| **500** | Internal Server Error | 服务端未知错误 |

### 场景选择速查

| 场景 | 方法 | 状态码 |
|------|:---:|:---:|
| 查询成功 | GET | 200 |
| 创建成功 | POST | 201 + Location 头 |
| 更新成功 | PUT/PATCH | 200（返回数据）或 204（不返回） |
| 删除成功 | DELETE | 204 |
| 参数校验失败 | ANY | 400 |
| 未登录 | ANY | 401 |
| 权限不足 | ANY | 403 |
| 资源不存在 | GET/PUT/DELETE | 404 |
| 重复创建（唯一键冲突） | POST | 409 |
| 服务器异常 | ANY | 500 |

---

## 3. 经典场景映射表

```text
用户注册：
  POST /users  { name, email, password }
  → 201 Created  { id: 3, name: "张三" }

用户登录：
  POST /auth/login  { email, password }
  → 200 OK  { accessToken, refreshToken }

获取当前用户：
  GET /users/me  (Header: Authorization)
  → 200 OK  { id: 1, name: "张三" }

修改密码：
  PATCH /users/1/password  { oldPassword, newPassword }
  → 204 No Content

资源不存在：
  GET /users/999
  → 404 Not Found  { code: 404, msg: "用户不存在" }

无权限访问：
  GET /admin/users
  → 403 Forbidden  { code: 403, msg: "无管理员权限" }
```

---

## 4. 常见错误用法

| ❌ 错误 | ✅ 正确 | 问题 |
|---------|---------|------|
| `POST /users/1` | `PATCH /users/1` | POST 不是幂等的 |
| `GET /users?action=delete&id=1` | `DELETE /users/1` | GET 不应有副作用 |
| 所有场景返回 200 | 用正确的状态码 | 前端无法区分成功/失败类型 |
| 未登录返回 500 | 401 | 混淆了服务端错误和认证 |
| 参数错误返回 200 + { error: "..." } | 400 | 语义不对 |

> 🎯 **铁律**：GET=获取 DELETE=删除 非幂等用 POST、创建成功返回 201、参数错 400、未登录 401、无权限 403、不存在 404、服务错 500。后端不乱用状态码是对前端最大的尊重。
