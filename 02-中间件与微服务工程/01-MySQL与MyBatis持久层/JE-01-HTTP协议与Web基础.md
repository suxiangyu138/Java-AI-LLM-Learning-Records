# HTTP 协议与 Web 基础

> **网络是 Web 应用的血管 —— 不理解 HTTP 协议，就无法真正理解 Web 开发。**

---

## 目录

- [1. HTTP 协议概述](#1-http-协议概述)
- [2. HTTP/1.1 请求与响应格式](#2-http11-请求与响应格式)
- [3. HTTP 方法详解](#3-http-方法详解)
- [4. HTTP 状态码完整参考](#4-http-状态码完整参考)
- [5. HTTP 首部（Headers）详解](#5-http-首部headers详解)
- [6. Cookie 机制](#6-cookie-机制)
- [7. Session 机制](#7-session-机制)
- [8. CORS（跨域资源共享）](#8-cors跨域资源共享)
- [9. HTTPS 与 TLS](#9-https-与-tls)
- [10. HTTP/2 与 HTTP/3](#10-http2-与-http3)
- [11. RESTful API 设计](#11-restful-api-设计)
- [12. 认证机制](#12-认证机制)
- [13. 开发调试工具](#13-开发调试工具)
- [14. 最佳实践与常见陷阱](#14-最佳实践与常见陷阱)
- [15. 面试题](#15-面试题)

---

## 1. HTTP 协议概述

### 什么是 HTTP？

**HTTP**（HyperText Transfer Protocol，超文本传输协议）是 Web 通信的基石。它定义了客户端（通常是浏览器）与服务器之间如何交换数据。

```
┌─────────────────┐          HTTP 请求          ┌─────────────────┐
│                  │ ──────────────────────────> │                  │
│   客户端         │                              │    服务器        │
│   (浏览器/App)   │ <────────────────────────── │   (Web Server)   │
│                  │          HTTP 响应          │                  │
└─────────────────┘                              └─────────────────┘
```

### HTTP 协议的核心特性

| 特性 | 说明 |
|------|------|
| **应用层协议** | 建立在 TCP/IP 之上，默认端口 80（HTTPS 为 443） |
| **请求-响应模式** | 客户端发起请求，服务器返回响应 |
| **无状态** | 每个请求之间相互独立，服务器不记忆客户端状态 |
| **可扩展** | 通过 Header 机制可以灵活扩展功能 |
| **媒体独立** | 通过 Content-Type 可以传输任意类型数据 |

### HTTP 协议版本演进

| 版本 | 发布年份 | 核心改进 | 现状 |
|------|---------|---------|------|
| HTTP/0.9 | 1991 | 只有 GET 方法，纯文本 | 历史 |
| HTTP/1.0 | 1996 | 引入 Header、状态码、多种方法 | 极少使用 |
| HTTP/1.1 | 1997 | 持久连接、管道化、分块传输、Host 头 | **广泛使用** |
| HTTP/2 | 2015 | 二进制分帧、多路复用、头部压缩、服务器推送 | 主流浏览器支持 |
| HTTP/3 | 2022 | 基于 QUIC (UDP)、0-RTT、无队头阻塞 | 逐步推广 |

---

## 2. HTTP/1.1 请求与响应格式

### 2.1 请求消息格式

```
┌──────────────────────────────────────────────────┐
│  请求行:  METHOD URI VERSION  \r\n               │
│  GET /api/users HTTP/1.1                          │
├──────────────────────────────────────────────────┤
│  请求头:  HEADER: VALUE  \r\n                     │
│  Host: example.com                                │
│  User-Agent: Mozilla/5.0                          │
│  Accept: application/json                         │
│  Authorization: Bearer xxx                        │
├──────────────────────────────────────────────────┤
│  空行:  \r\n                                      │
├──────────────────────────────────────────────────┤
│  请求体（可选）                                    │
│  {"username": "john", "password": "123456"}       │
└──────────────────────────────────────────────────┘
```

**实际抓包示例（GET 请求）：**

```
GET /api/users?page=1&size=10 HTTP/1.1
Host: api.example.com
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36
Accept: application/json, text/plain, */*
Accept-Language: zh-CN,zh;q=0.9,en;q=0.8
Accept-Encoding: gzip, deflate, br
Connection: keep-alive
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

**实际抓包示例（POST 请求）：**

```
POST /api/users/login HTTP/1.1
Host: api.example.com
Content-Type: application/json
Content-Length: 45
Authorization: Basic dXNlcjpwYXNz

{"username":"admin","password":"123456"}
```

### 2.2 响应消息格式

```
┌──────────────────────────────────────────────────┐
│  状态行:  VERSION STATUS_CODE REASON_PHRASE \r\n  │
│  HTTP/1.1 200 OK                                  │
├──────────────────────────────────────────────────┤
│  响应头:  HEADER: VALUE  \r\n                     │
│  Content-Type: application/json                   │
│  Content-Length: 234                              │
│  Set-Cookie: sessionId=abc123; HttpOnly           │
│  Cache-Control: max-age=3600                      │
├──────────────────────────────────────────────────┤
│  空行:  \r\n                                      │
├──────────────────────────────────────────────────┤
│  响应体（可选）                                    │
│  {"id":1,"username":"admin","token":"xxx"}        │
└──────────────────────────────────────────────────┘
```

**实际抓包示例（成功响应）：**

```
HTTP/1.1 200 OK
Content-Type: application/json; charset=utf-8
Content-Length: 127
Cache-Control: no-cache
Date: Mon, 01 Jan 2024 12:00:00 GMT

{"id":1,"username":"john","email":"john@example.com"}
```

**实际抓包示例（错误响应）：**

```
HTTP/1.1 401 Unauthorized
WWW-Authenticate: Bearer realm="api"
Content-Type: application/json; charset=utf-8
Content-Length: 52

{"error":"unauthorized","message":"Token expired"}
```

---

## 3. HTTP 方法详解

### 3.1 方法对照表

| 方法 | 语义 | 幂等 | 安全 | 请求体 | 响应体 | 典型用途 |
|------|------|------|------|--------|--------|---------|
| **GET** | 获取资源 | 是 | 是 | 无 | 资源表示 | 查询列表/详情 |
| **POST** | 创建资源 | 否 | 否 | 资源数据 | 创建的资源 | 注册、发布 |
| **PUT** | 完整替换 | 是 | 否 | 完整资源 | 更新的资源 | 更新完整信息 |
| **PATCH** | 部分更新 | 否 | 否 | 差异数据 | 更新的资源 | 修改部分字段 |
| **DELETE** | 删除资源 | 是 | 否 | 无 | 结果/无 | 删除资源 |
| **HEAD** | 获取响应头 | 是 | 是 | 无 | 无（仅有头） | 检查资源是否存在 |
| **OPTIONS** | 查看支持方法 | 是 | 是 | 无 | 允许的方法 | CORS 预检请求 |

> **幂等**：多次执行产生相同的效果（资源状态不变）。**安全**：不会修改资源状态。

### 3.2 方法详解

#### GET

获取指定资源，是 Web 中最常用的方法。

```http
GET /api/users/123 HTTP/1.1
Host: example.com
Accept: application/json
```

**关键特性：**
- 请求参数通过 URL query string 传递：`?page=1&size=10`
- 不应该有请求体（部分 HTTP 客户端会忽略 GET 请求的 body）
- 可被缓存、加入书签
- 浏览器地址栏直接访问
- **敏感数据不应该放在 URL 参数中**（会被记录在服务器日志）

#### POST

提交数据给服务器，通常用于创建资源。

```http
POST /api/users HTTP/1.1
Host: example.com
Content-Type: application/json

{
    "username": "john_doe",
    "email": "john@example.com",
    "password": "securePass123!"
}
```

**关键特性：**
- 数据在请求体中传输
- 不会限制数据长度
- 支持多种 Content-Type（JSON、form-data、x-www-form-urlencoded）
- 非幂等：重复提交会创建多个资源（需要额外处理重复提交问题）

#### PUT

完整替换指定资源。如果资源不存在，部分实现会创建它（取决于设计）。

```http
PUT /api/users/123 HTTP/1.1
Host: example.com
Content-Type: application/json

{
    "username": "john_updated",
    "email": "john_new@example.com",
    "password": "newPassword456!"
}
```

**关键特性：**
- 幂等：多次调用结果相同
- 需要发送**完整**的资源表示
- 客户端决定资源的 URI（通常由服务端告知后客户端使用）

#### PATCH

对资源进行部分修改，比 PUT 更高效。

```http
PATCH /api/users/123 HTTP/1.1
Host: example.com
Content-Type: application/json

{
    "email": "new_email@example.com"
}
```

**与 PUT 的区别：**

```
PUT  /api/users/123  请求体: {完整用户信息}   替换整个用户
PATCH /api/users/123  请求体: {email更新}     只更新邮箱字段
```

#### DELETE

删除指定资源。

```http
DELETE /api/users/123 HTTP/1.1
Host: example.com
Authorization: Bearer xxx
```

**关键特性：**
- 幂等：第一次删除返回 200/204，第二次删除通常返回 404
- 通常返回 204 No Content（无响应体）或 200 OK（删除后的确认信息）
- 删除可以是**逻辑删除**（设置 status=0）或**物理删除**（从数据库移除）

---

## 4. HTTP 状态码完整参考

### 4.1 1xx：信息性响应

| 状态码 | 原因短语 | 含义 | 典型场景 |
|--------|---------|------|---------|
| 100 | Continue | 客户端应继续请求 | 客户端发送 Expect: 100-continue |
| 101 | Switching Protocols | 切换协议 | WebSocket 升级握手 |
| 102 | Processing | 服务器正在处理（WebDAV） | 长时间操作 |
| 103 | Early Hints | 提前提示资源链接 | 提前加载关键资源 |

### 4.2 2xx：成功

| 状态码 | 原因短语 | 含义 | 典型场景 |
|--------|---------|------|---------|
| **200** | OK | 请求成功 | GET 成功返回资源 |
| **201** | Created | 资源创建成功 | POST 创建用户成功 |
| **202** | Accepted | 请求已接受但未完成 | 异步任务提交 |
| 203 | Non-Authoritative Info | 非权威信息 | 代理修改了响应 |
| **204** | No Content | 请求成功但无内容 | DELETE 成功、PUT 更新成功 |
| 205 | Reset Content | 重置内容 | 表单重置 |
| 206 | Partial Content | 部分内容 | 范围请求、断点续传 |

### 4.3 3xx：重定向

| 状态码 | 原因短语 | 含义 | 典型场景 |
|--------|---------|------|---------|
| **301** | Moved Permanently | 永久重定向 | 域名变更、HTTP → HTTPS 跳转（SEO 友好） |
| **302** | Found | 临时重定向 | 登录后跳转、功能维护中临时跳转 |
| 303 | See Other | 查看其他位置 | POST 后重定向到 GET 资源（Post/Redirect/Get 模式） |
| **304** | Not Modified | 资源未修改 | 条件请求缓存验证（If-Modified-Since / If-None-Match） |
| 307 | Temporary Redirect | 临时重定向（保持方法） | 与 302 类似，但保持请求方法不变 |
| 308 | Permanent Redirect | 永久重定向（保持方法） | 与 301 类似，但保持请求方法不变 |

> **301 vs 302 vs 307 核心区别：**
> - 301/302：浏览器可能会将 POST 改为 GET（取决于实现）
> - 307/308：**保证**请求方法和 body 不变

### 4.4 4xx：客户端错误

| 状态码 | 原因短语 | 含义 | 典型场景 |
|--------|---------|------|---------|
| **400** | Bad Request | 请求格式错误 | 参数校验失败、JSON 解析错误 |
| **401** | Unauthorized | 未认证 | 未登录、Token 过期 |
| 402 | Payment Required | 需要付款（保留） | 极少使用 |
| **403** | Forbidden | 无权限 | 已登录但无权访问 |
| **404** | Not Found | 资源不存在 | URL 错误、资源已删除 |
| **405** | Method Not Allowed | 请求方法不允许 | GET 资源却发了 DELETE |
| **406** | Not Acceptable | 不可接受 | 无法生成 Accept 指定的类型 |
| 407 | Proxy Auth Required | 需要代理认证 | 代理服务器要求认证 |
| **408** | Request Timeout | 请求超时 | 客户端太久未发送数据 |
| **409** | Conflict | 资源冲突 | 用户名已存在、版本冲突 |
| **410** | Gone | 资源已永久删除 | 与 404 类似，但明确表示资源曾被存在过 |
| **411** | Length Required | 需要 Content-Length | 未设置 Content-Length 头 |
| **412** | Precondition Failed | 预处理条件失败 | If-Match 条件不满足（乐观锁） |
| **413** | Payload Too Large | 请求体太大 | 上传文件超过限制 |
| **415** | Unsupported Media Type | 不支持的媒体类型 | Content-Type 不被支持 |
| **422** | Unprocessable Entity | 语义错误 | 请求格式正确但语义错误（如邮箱格式不正确） |
| **429** | Too Many Requests | 请求过多 | 限流触发 |

### 4.5 5xx：服务器错误

| 状态码 | 原因短语 | 含义 | 典型场景 |
|--------|---------|------|---------|
| **500** | Internal Server Error | 服务器内部错误 | 代码异常、数据库错误 |
| **501** | Not Implemented | 未实现 | 服务器不支持请求的方法 |
| **502** | Bad Gateway | 网关错误 | 代理服务器收到上游无效响应 |
| **503** | Service Unavailable | 服务不可用 | 服务器过载、停机维护 |
| **504** | Gateway Timeout | 网关超时 | 代理服务器等待上游超时 |
| 505 | HTTP Version Not Supported | 不支持的版本 | HTTP/2 请求发给了仅 HTTP/1.1 的服务器 |
| 507 | Insufficient Storage | 存储空间不足 | 磁盘满了 |
| 508 | Loop Detected | 检测到循环重定向 | 无限重定向循环 |

### 4.6 状态码选择快速决策树

```
请求正常处理完成？
├── 是 → 有响应体？ → 是 → 200 OK
│                   → 否 → 204 No Content
├── 是（创建资源）→ 201 Created
└── 否
    ├── 客户端问题？
    │   ├── 请求格式错误 → 400 Bad Request
    │   ├── 未认证 → 401 Unauthorized
    │   ├── 无权限 → 403 Forbidden
    │   ├── 资源不存在 → 404 Not Found
    │   ├── 冲突 → 409 Conflict
    │   └── 频率过高 → 429 Too Many Requests
    └── 服务器问题？
        ├── 代码异常 → 500 Internal Server Error
        ├── 服务不可用 → 503 Service Unavailable
        └── 网关异常 → 502 / 504
```

---

## 5. HTTP 首部（Headers）详解

### 5.1 通用首部

适用于请求和响应：

| 首部 | 含义 | 示例 |
|------|------|------|
| `Date` | 消息创建时间 | `Date: Mon, 01 Jan 2024 12:00:00 GMT` |
| `Cache-Control` | 缓存控制指令 | `Cache-Control: max-age=3600, no-cache` |
| `Connection` | 连接管理 | `Connection: keep-alive` |
| `Transfer-Encoding` | 传输编码 | `Transfer-Encoding: chunked` |

### 5.2 请求首部

| 首部 | 含义 | 示例 |
|------|------|------|
| `Host` | 目标主机（HTTP/1.1 必需） | `Host: api.example.com` |
| `User-Agent` | 客户端标识 | `User-Agent: Mozilla/5.0 ...` |
| `Accept` | 可接受的媒体类型 | `Accept: application/json` |
| `Accept-Language` | 可接受的语言 | `Accept-Language: zh-CN,zh;q=0.9` |
| `Accept-Encoding` | 可接受的编码 | `Accept-Encoding: gzip, deflate, br` |
| `Content-Type` | 请求体类型 | `Content-Type: application/json` |
| `Content-Length` | 请求体长度（字节） | `Content-Length: 123` |
| `Authorization` | 认证凭证 | `Authorization: Bearer eyJ...` |
| `Cookie` | Cookie 数据 | `Cookie: sessionId=abc123; theme=dark` |
| `Referer` | 请求来源 URL | `Referer: https://example.com/login` |
| `Origin` | 请求来源域名（CORS） | `Origin: https://example.com` |
| `If-Modified-Since` | 条件请求 | `If-Modified-Since: Mon, 01 Jan 2024 12:00:00 GMT` |
| `If-None-Match` | 条件请求（ETag） | `If-None-Match: "abc123"` |
| `Range` | 范围请求 | `Range: bytes=0-1023` |

### 5.3 响应首部

| 首部 | 含义 | 示例 |
|------|------|------|
| `Content-Type` | 响应体类型 | `Content-Type: application/json; charset=utf-8` |
| `Content-Length` | 响应体长度 | `Content-Length: 234` |
| `Content-Encoding` | 响应体编码 | `Content-Encoding: gzip` |
| `Set-Cookie` | 设置 Cookie | `Set-Cookie: sessionId=abc123; HttpOnly; Secure` |
| `WWW-Authenticate` | 认证质询 | `WWW-Authenticate: Bearer realm="api"` |
| `Location` | 重定向 URL | `Location: https://example.com/new-page` |
| `ETag` | 资源实体标签 | `ETag: "abc123"` |
| `Last-Modified` | 最后修改时间 | `Last-Modified: Mon, 01 Jan 2024 12:00:00 GMT` |
| `Cache-Control` | 缓存策略 | `Cache-Control: public, max-age=3600` |
| `Expires` | 过期时间 | `Expires: Tue, 02 Jan 2024 12:00:00 GMT` |

### 5.4 CORS 相关首部

| 首部 | 含义 | 示例 |
|------|------|------|
| `Access-Control-Allow-Origin` | 允许的源 | `Access-Control-Allow-Origin: https://example.com` |
| `Access-Control-Allow-Methods` | 允许的方法 | `Access-Control-Allow-Methods: GET, POST, PUT` |
| `Access-Control-Allow-Headers` | 允许的头 | `Access-Control-Allow-Headers: Content-Type, Authorization` |
| `Access-Control-Allow-Credentials` | 允许凭证 | `Access-Control-Allow-Credentials: true` |
| `Access-Control-Max-Age` | 预检结果缓存时间 | `Access-Control-Max-Age: 3600` |
| `Access-Control-Expose-Headers` | 暴露给客户端的头 | `Access-Control-Expose-Headers: X-Total-Count` |

### 5.5 Content-Type 详细对照

| Content-Type | 用途 | 示例 |
|-------------|------|------|
| `application/json` | JSON 数据 | REST API 请求/响应 |
| `application/x-www-form-urlencoded` | 表单提交（默认） | HTML 表单提交 |
| `multipart/form-data` | 文件上传 | 带文件的表单提交 |
| `text/html; charset=utf-8` | HTML 页面 | 网页响应 |
| `text/plain` | 纯文本 | 文本响应 |
| `application/xml` | XML 数据 | SOAP API |
| `application/octet-stream` | 二进制流 | 文件下载 |
| `image/jpeg`, `image/png` | 图片 | 图片资源 |
| `application/pdf` | PDF 文件 | PDF 下载 |

---

## 6. Cookie 机制

### 6.1 什么是 Cookie？

Cookie 是服务器发送给客户端的一小段数据，客户端会在后续请求中携带它，用于**状态管理**。

```
服务器                     浏览器
  │                         │
  │  Set-Cookie: session    │
  │  =abc123; Path=/;       │
  │  HttpOnly               │  ← 服务器在响应中设置 Cookie
  │────────────────────────>│
  │                         │  ← 浏览器存储 Cookie
  │                         │
  │  GET /api/users         │
  │  Cookie: session=abc123 │  ← 浏览器在后续请求中自动携带
  │<────────────────────────│
  │                         │
```

### 6.2 Set-Cookie 属性详解

```
Set-Cookie: <name>=<value>; [属性1]; [属性2]; ...
```

| 属性 | 说明 | 示例 |
|------|------|------|
| `Domain` | 指定哪些域名可以接收 Cookie | `Domain=.example.com`（所有子域名） |
| `Path` | 指定哪些路径可以发送 Cookie | `Path=/`（所有路径） |
| `Expires` | 过期日期（绝对时间） | `Expires=Wed, 21 Oct 2025 07:28:00 GMT` |
| `Max-Age` | 过期秒数（相对时间，优先级高于 Expires） | `Max-Age=3600`（1小时后过期） |
| `HttpOnly` | 禁止 JavaScript 访问（防 XSS） | `HttpOnly` |
| `Secure` | 仅通过 HTTPS 发送 | `Secure` |
| `SameSite` | 控制跨站请求时是否发送 | `SameSite=Lax`（默认值） |

### 6.3 SameSite 属性详解（CSRF 防护关键）

| SameSite 值 | 行为 | 安全级别 |
|-------------|------|---------|
| `Strict` | **所有跨站请求都不发送** Cookie | 最安全，但用户体验差（点外部链接也无法保持登录） |
| `Lax` | **GET 请求**（顶级导航）发送，POST 不发送 | 默认值，平衡安全与体验 |
| `None` | 所有跨站请求都发送（**必须同时设置 Secure**） | 不安全，仅跨域需要时使用 |

> Chrome 80+ 默认将未设置 SameSite 的 Cookie 视为 Lax。

### 6.4 Cookie 大小限制

| 限制项 | 限制值 |
|--------|--------|
| 单个 Cookie 大小 | 4KB（4096 字节） |
| 每个域名 Cookie 数量 | 浏览器不同，通常 20-50 个 |
| 总 Cookie 大小 | 通常 4KB-8KB |

### 6.5 Java 中操作 Cookie

```java
// 创建 Cookie
Cookie cookie = new Cookie("theme", "dark");
cookie.setMaxAge(3600);          // 1小时过期
cookie.setPath("/");             // 整个站点可用
cookie.setHttpOnly(true);        // 禁止 JS 访问
cookie.setSecure(true);          // 仅 HTTPS
cookie.setDomain(".example.com"); // 子域名共享
response.addCookie(cookie);

// 读取 Cookie
Cookie[] cookies = request.getCookies();
if (cookies != null) {
    for (Cookie c : cookies) {
        if ("theme".equals(c.getName())) {
            String theme = c.getValue();
        }
    }
}

// 删除 Cookie
Cookie cookie = new Cookie("theme", null);
cookie.setMaxAge(0);  // 立即过期
cookie.setPath("/");
response.addCookie(cookie);
```

---

## 7. Session 机制

### 7.1 什么是 Session？

Session 是服务器端维护的用户状态信息。由于 HTTP 是无状态的，Session 机制通过一个唯一的标识符（Session ID）来关联客户端的多次请求。

### 7.2 Session 工作流程

```
客户端                                  服务器
  │                                      │
  │  POST /api/login                     │  第一次请求
  │─────────────────────────────────────>│
  │                                      │ 创建 Session
  │                                      │ 生成 JSESSIONID=abc123
  │  Set-Cookie: JSESSIONID=abc123       │
  │  ; Path=/; HttpOnly                  │
  │<─────────────────────────────────────│
  │                                      │
  │  GET /api/profile                    │  后续请求
  │  Cookie: JSESSIONID=abc123           │
  │─────────────────────────────────────>│
  │                                      │ 根据 JSESSIONID 查找 Session
  │                                      │ 返回用户数据
  │<─────────────────────────────────────│
```

### 7.3 Java Servlet Session API

```java
// 获取 Session（如果没有则创建）
HttpSession session = request.getSession();  // 默认：不存在则创建
HttpSession session = request.getSession(false); // 不存在则返回 null

// Session 属性操作
session.setAttribute("user", userObj);       // 存储用户信息
User user = (User) session.getAttribute("user"); // 获取用户信息
session.removeAttribute("user");             // 移除属性

// Session 管理
String sessionId = session.getId();           // 获取 Session ID
long creationTime = session.getCreationTime();// 创建时间
long lastAccess = session.getLastAccessedTime(); // 最后访问时间
int maxInactive = session.getMaxInactiveInterval(); // 超时时间（秒）

// 设置超时时间
session.setMaxInactiveInterval(1800);         // 30分钟无操作过期
// web.xml 配置：
// <session-config>
//     <session-timeout>30</session-timeout>
// </session-config>

// 使 Session 失效（注销）
session.invalidate();
```

### 7.4 Session 存储方式

| 存储方式 | 优点 | 缺点 | 适用场景 |
|---------|------|------|---------|
| **本地内存** | 简单、快速 | 重启丢失、无法分布式共享 | 单机应用、开发环境 |
| **数据库** | 持久化、可共享 | 读写慢、增加数据库压力 | 小规模集群 |
| **Redis** | 快速、持久化、分布式 | 引入额外中间件 | **生产环境主流方案** |
| **Memcached** | 快速 | 不支持持久化 | 可接受丢失的场景 |

### 7.5 分布式 Session 问题

在集群部署时，用户的请求可能被分发到不同服务器：

```
用户请求 → 负载均衡器
    ├── 服务器 A（本地内存：JSESSIONID=abc → 用户数据）
    └── 服务器 B（本地内存：无此 Session → 未登录！）
```

**解决方案：**

| 方案 | 原理 | 优点 | 缺点 |
|------|------|------|------|
| **Session 粘滞** | 同一用户的请求始终发送到同一台服务器 | 简单 | 负载不均、服务器宕机丢失 |
| **Session 复制** | 各服务器之间同步 Session | 兼容性好 | 网络开销大、有延迟 |
| **集中式存储** | Session 存储在 Redis/DB 中 | 可靠性高、扩展性好 | 引入中间件、增加网络延迟 |
| **无状态 Token** | 用户数据编码在 Token 中（如 JWT） | 彻底无状态 | Token 无法主动失效 |

**生产环境推荐：** Redis 集中式 Session 存储或 JWT 无状态认证。

### 7.6 Session vs Cookie 对比

| 特性 | Cookie | Session |
|------|--------|---------|
| 数据存储位置 | 客户端（浏览器） | 服务器端 |
| 数据大小限制 | 4KB 左右 | 取决于服务器（通常无限制） |
| 安全性 | 存储在客户端，可被篡改 | 存储在服务端，更安全 |
| 存储内容 | 少量状态数据 | 任意对象数据 |
| 有效期 | 可定义（Expires/Max-Age） | 可定义（超时时间） |
| 性能影响 | 每次请求携带 | 需要查询存储 |
| 跨域支持 | 受 Same-Origin 限制 | 可通过集中式存储解决 |

---

## 8. CORS（跨域资源共享）

### 8.1 什么是跨域？

**同源策略**是浏览器的一个安全机制，它限制了一个源（origin）的文档或脚本如何与另一个源的资源进行交互。

```
同源条件：协议 + 域名 + 端口 三者完全一致

同源：  https://example.com:443/page1
       https://example.com:443/page2

不同源：https://example.com          → http://example.com      （协议不同）
        https://example.com          → https://api.example.com （子域名不同）
        https://example.com          → https://example.com:8080（端口不同）
```

### 8.2 CORS 解决方案

```mermaid
sequenceDiagram
    participant Browser
    participant Frontend
    participant Backend

    Note over Browser: 跨域请求
    Browser->>Backend: OPTIONS /api/users (Preflight)
    Backend-->>Browser: 200 OK<br/>Access-Control-Allow-Origin: *<br/>Access-Control-Allow-Methods: GET, POST
    Note over Browser: 预检通过，发送实际请求
    Browser->>Backend: GET /api/users
    Backend-->>Browser: 200 OK<br/>Access-Control-Allow-Origin: https://frontend.example.com
```

### 8.3 简单请求 vs 预检请求

**简单请求**（不会触发预检）：

- 方法：GET、HEAD、POST
- 头：Accept、Accept-Language、Content-Language、Content-Type（限 application/x-www-form-urlencoded、multipart/form-data、text/plain）

**触发预检的条件**（满足任意一条）：

```
1. 使用 PUT、DELETE、PATCH 等方法
2. Content-Type 为 application/json 等非简单类型
3. 包含自定义请求头（如 Authorization、X-Custom-Header）
```

### 8.4 后端配置 CORS 示例

**Servlet Filter 实现：**

```java
@WebFilter("/*")
public class CorsFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;

        // 允许的来源（生产环境应指定具体域名）
        String origin = request.getHeader("Origin");
        response.setHeader("Access-Control-Allow-Origin",
                origin != null ? origin : "*");
        response.setHeader("Access-Control-Allow-Methods",
                "GET, POST, PUT, DELETE, PATCH, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers",
                "Content-Type, Authorization, X-Requested-With");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Max-Age", "3600");

        // 预检请求直接返回
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(request, response);
    }
}
```

### 8.5 CORS 常见问题

| 问题 | 原因 | 解决 |
|------|------|------|
| `Access-Control-Allow-Origin: *` 但浏览器报错 | 使用凭证（Cookie/Authorization）时不能用 * | 指定具体 origin |
| 预检请求出现 405/403 | 服务器未正确处理 OPTIONS 请求 | 确保 OPTIONS 请求返回 200 |
| 自定义 Header 不被允许 | 未在 Access-Control-Allow-Headers 中声明 | 添加相应的 Header |
| 多个 Origin 需要支持 | Access-Control-Allow-Origin 只支持单个值 | 根据请求动态设置 |

---

## 9. HTTPS 与 TLS

### 9.1 为什么需要 HTTPS？

```
HTTP 的三大风险：
┌─────────────────────────────────────────────────────────┐
│                                                          │
│  窃听风险：数据在传输过程中被截获                         │
│  ┌──────────────────────────────────────────────┐        │
│  │ 客户端 →  [恶意路由器]  → 服务器              │        │
│  │              ↓                                │        │
│  │          明文密码被窃取                        │        │
│  └──────────────────────────────────────────────┘        │
│                                                          │
│  篡改风险：数据在传输过程中被修改                         │
│  ┌──────────────────────────────────────────────┐        │
│  │ "转给我100元"  →  [中间人]  → "转给我10000元" │        │
│  └──────────────────────────────────────────────┘        │
│                                                          │
│  冒充风险：客户端与伪装的服务器通信                         │
│  ┌──────────────────────────────────────────────┐        │
│  │ 客户端 →  [假银行网站]  → 输入密码            │        │
│  └──────────────────────────────────────────────┘        │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

HTTPS = HTTP + **TLS**（传输层安全协议）解决了以上三大风险。

### 9.2 对称加密 vs 非对称加密

| 特性 | 对称加密 | 非对称加密 |
|------|---------|-----------|
| 密钥 | 加密和解密使用同一个密钥 | 公钥加密、私钥解密 |
| 算法 | AES、DES、3DES | RSA、ECC、DSA |
| 速度 | **非常快**（适合加密大量数据） | **慢**（适合加密少量数据） |
| 密钥分发 | 难题（如何安全传输密钥？） | 容易（公钥可公开） |
| 安全性 | 密钥泄露则全盘泄露 | 私钥不泄露即安全 |

**HTTPS 的智慧：** 使用非对称加密来安全地协商对称密钥，然后使用对称加密来加密实际通信数据。

### 9.3 TLS 1.2 握手过程

```
客户端                              服务器
  │                                    │
  │  1. ClientHello                    │
  │     支持的 TLS 版本、密码套件       │
  │     随机数 random_C               │
  │───────────────────────────────────>│
  │                                    │
  │  2. ServerHello                    │
  │     选定的 TLS 版本、密码套件       │
  │     随机数 random_S               │
  │<───────────────────────────────────│
  │                                    │
  │  3. Certificate                    │
  │     服务器证书（含公钥）            │
  │<───────────────────────────────────│
  │                                    │
  │  4. ServerHelloDone                │
  │<───────────────────────────────────│
  │                                    │
  │  5. ClientKeyExchange              │
  │     用服务器公钥加密的 Pre-Master   │
  │───────────────────────────────────>│
  │                                    │
  │  双方计算会话密钥:                  │
  │  MasterSecret = PRF(PreMaster      │
  │                  + random_C        │
  │                  + random_S)       │
  │                                    │
  │  6. ChangeCipherSpec               │
  │     后续通信将加密                  │
  │───────────────────────────────────>│
  │                                    │
  │  7. Finished (加密的握手哈希)       │
  │───────────────────────────────────>│
  │                                    │
  │  8. ChangeCipherSpec               │
  │<───────────────────────────────────│
  │                                    │
  │  9. Finished (加密的握手哈希)       │
  │<───────────────────────────────────│
  │                                    │
  │  10. 加密的应用数据                 │
  │  <───────────────────────────────> │
```

**TLS 1.2 握手共需 2 个 RTT（往返时间）。**

### 9.4 TLS 1.3 握手过程

```
客户端                              服务器
  │                                    │
  │  1. ClientHello                    │
  │     支持的 TLS 版本                 │
  │     密码套件列表                    │
  │     随机数 random_C                │
  │     密钥共享（KeyShare）            │
  │───────────────────────────────────>│
  │                                    │
  │  2. ServerHello                    │
  │     选定的 TLS 版本                 │
  │     密码套件                        │
  │     随机数 random_S                │
  │     密钥共享（KeyShare）            │
  │     Certificate                    │
  │     Finished                       │
  │<───────────────────────────────────│
  │                                    │
  │  3. Finished + 应用数据             │
  │───────────────────────────────────>│
  │                                    │
  │  4. 加密的应用数据                  │
  │  <────────────────────────────────>│
```

**TLS 1.3 握手仅需 1 个 RTT**，且删除不安全的密码套件。

### 9.5 证书链验证

```
┌─────────────────────────────┐
│  根证书（Root CA）           │  ← 内置在操作系统/浏览器中
│  └── 签署                    │
│      ┌──────────────────────┤
│      │  中间证书（Intermediate）│  ← CA 签发
│      │  └── 签署              │
│      │      ┌───────────────┤
│      │      │  服务器证书     │  ← 运维申请
│      │      │  CN=example.com │
│      │      │  公钥、域名     │
│      │      │  有效期         │
│      └──────┴───────────────┘
└─────────────────────────────┘
```

验证过程：

```
1. 检查证书是否过期
2. 检查域名是否匹配证书中的 CN/SAN
3. 使用上级证书的公钥验证下级证书的签名
4. 逐级向上直到内置的根证书
5. 检查证书是否被吊销（CRL/OCSP）
```

### 9.6 HTTPS 在 Java 中的配置

**使用 HttpsURLConnection：**

```java
// 信任所有证书（不推荐生产使用）
TrustManager[] trustAllCerts = new TrustManager[]{
    new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() { return null; }
        public void checkClientTrusted(X509Certificate[] certs, String authType) { }
        public void checkServerTrusted(X509Certificate[] certs, String authType) { }
    }
};

SSLContext sc = SSLContext.getInstance("TLS");
sc.init(null, trustAllCerts, new SecureRandom());
HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

URL url = new URL("https://api.example.com");
HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
// ...处理响应
```

---

## 10. HTTP/2 与 HTTP/3

### 10.1 HTTP/1.1 的性能瓶颈

```
问题1：队头阻塞（Head-of-Line Blocking）
  ┌─────┐  ┌─────┐  ┌─────┐
  │请求1│->│请求2│  │请求3│  请求2 必须等请求1 完成
  └─────┘  └─────┘  └─────┘

虽然 HTTP/1.1 支持管道化（pipelining），但响应顺序必须与请求顺序一致

问题2：每个域名最多 6-8 个并发连接
  浏览器为每个域名开启多个 TCP 连接 → 资源竞争

问题3：头部冗余
  每次请求都包含大量重复的首部信息
  如 Cookie、User-Agent 等每次请求都重复发送
```

### 10.2 HTTP/2 核心改进

**二进制分帧：**

```
HTTP/1.1：文本协议
  GET /api/users HTTP/1.1\r\n
  Host: example.com\r\n
  \r\n

HTTP/2：二进制协议
  ┌─────────┬──────────┬──────────┐
  │ Length  │  Type    │  Flags   │
  ├─────────┴──────────┴──────────┤
  │  Stream Identifier (31 bits)  │
  ├───────────────────────────────┤
  │  Frame Payload                │
  └───────────────────────────────┘
```

**多路复用（Multiplexing）：**

```
HTTP/1.1：
TCP 连接 1: [请求1][请求2][请求3]...  → 串行

HTTP/2：
TCP 连接 1: [请求1][请求2][请求1][请求3]...  → 并行交错
             流1   流2   流1   流3
多个请求在同一个 TCP 连接上交错传输，无队头阻塞
```

**头部压缩（HPACK）：**

```
客户端：                      服务器：
  Header: Host: example.com    维护相同的静态/动态表
  Header: Cookie: xxx          索引替换重复字段
  
  第一次：发送完整 header
  后续：只发送索引号 → 大幅减少传输量
```

**服务器推送（Server Push）：**

```
客户端请求 index.html
  │
服务器返回 index.html
  │
服务器主动推送 style.css 和 app.js（无需客户端请求）
```

### 10.3 HTTP/3 核心改进

HTTP/3 将底层传输协议从 TCP 改为 **QUIC**（基于 UDP）：

```
HTTP/2 over TCP：
  ┌──────────┐
  │  HTTP/2  │
  ├──────────┤
  │  TLS     │
  ├──────────┤
  │  TCP     │ ← TCP 的队头阻塞问题无法避免
  ├──────────┤
  │  IP      │
  └──────────┘

HTTP/3 over QUIC：
  ┌──────────┐
  │  HTTP/3  │
  ├──────────┤
  │  QUIC    │ ← 基于 UDP，内建 TLS 1.3
  ├──────────┤
  │  UDP     │
  ├──────────┤
  │  IP      │
  └──────────┘
```

**QUIC 核心优势：**

| 特性 | 说明 |
|------|------|
| **0-RTT 握手** | 已连接过的服务器可直接发送数据（无握手延迟） |
| **无队头阻塞** | 某个流丢包不影响其他流 |
| **连接迁移** | 切换网络时连接不会断开（IP 变化不影响） |
| **内建 TLS 1.3** | 加密是内建的，没有纯文本的 QUIC |
| **更快的错误恢复** | 前向纠错（FEC）减少重传 |

### 10.4 版本对比总结

| 特性 | HTTP/1.1 | HTTP/2 | HTTP/3 |
|------|---------|--------|--------|
| 底层协议 | TCP | TCP | QUIC (UDP) |
| 数据格式 | 文本 | 二进制帧 | 二进制帧 |
| 多路复用 | 无（需多连接） | 有 | 有 |
| 队头阻塞 | 存在 | TCP 级别存在 | 无 |
| 头部压缩 | 无 | HPACK | QPACK |
| 服务器推送 | 无 | 有 | 有 |
| 连接建立时间 | 2 RTT (TCP+TLS) | 2 RTT | 0-1 RTT |
| 浏览器支持 | 全部 | 95%+ | 85%+ |

---

## 11. RESTful API 设计

### 11.1 REST 设计原则

**REST**（Representational State Transfer）是一种 API 设计风格，核心原则：

```
1. 资源导向（Resource-Oriented）
   API 围绕"资源"设计，每个资源有唯一的 URI

2. 使用标准 HTTP 方法
   GET（查）/ POST（增）/ PUT（全量改）/ DELETE（删）/ PATCH（部分改）

3. 无状态
   每个请求包含所有必要信息，服务器不保存客户端状态

4. 统一接口
   所有资源遵循相同的接口规范

5. 超媒体驱动（HATEOAS）
   响应中包含相关资源的链接
```

### 11.2 URL 命名规范

```
正确示范：                   错误示范：
  GET    /users              GET    /getUsers
  GET    /users/123          GET    /getUserById?id=123
  POST   /users              POST   /createUser
  PUT    /users/123          POST   /updateUser
  DELETE /users/123          GET    /deleteUser?id=123
  GET    /users/123/orders   GET    /getUserOrders?userId=123
```

**命名规则：**

| 规则 | 正确 | 错误 |
|------|------|------|
| 使用名词（不是动词） | `/users` | `/getUsers` |
| 复数形式 | `/users` | `/user` |
| 小写字母 | `/users` | `/Users` |
| 连字符分隔 | `/order-items` | `/orderItems` 或 `/order_items` |
| 层级表示关系 | `/users/123/orders` | `/orders?userId=123` |
| 查询参数用于过滤 | `/users?role=admin` | `/users/admin` |

### 11.3 请求/响应设计

**统一的请求结构：**

```http
# 列表查询
GET /api/users?page=1&size=20&sort=createdAt,desc&role=admin
Accept: application/json

# 详情查询
GET /api/users/123
Accept: application/json

# 创建
POST /api/users
Content-Type: application/json
Accept: application/json

{
    "username": "john",
    "email": "john@example.com",
    "password": "securePass123"
}

# 更新
PUT /api/users/123
Content-Type: application/json

{
    "username": "john_updated",
    "email": "john_new@example.com",
    "password": "newPass456"
}

# 部分更新
PATCH /api/users/123
Content-Type: application/json

{
    "email": "newemail@example.com"
}

# 删除
DELETE /api/users/123
```

**统一的响应结构：**

```json
// 成功响应
{
    "code": 200,
    "message": "success",
    "data": {
        "id": 1,
        "username": "john",
        "email": "john@example.com"
    },
    "timestamp": 1704067200000
}

// 分页响应
{
    "code": 200,
    "message": "success",
    "data": {
        "content": [
            { "id": 1, "username": "john" },
            { "id": 2, "username": "jane" }
        ],
        "page": 1,
        "size": 20,
        "totalElements": 100,
        "totalPages": 5,
        "last": false,
        "first": true
    }
}

// 错误响应
{
    "code": 400,
    "message": "Validation failed",
    "errors": [
        {
            "field": "email",
            "message": "Email format is invalid"
        },
        {
            "field": "password",
            "message": "Password must be at least 8 characters"
        }
    ],
    "timestamp": 1704067200000
}
```

### 11.4 API 版本化策略

| 策略 | 示例 | 优点 | 缺点 |
|------|------|------|------|
| **URL 路径** | `/api/v1/users` | 直观、易于路由 | 污染 URL |
| **请求头** | `Accept: application/vnd.example.v1+json` | 干净 URL | 调试不方便 |
| **查询参数** | `/api/users?version=1` | 实现简单 | 参数容易被忽略 |

**推荐：URL 路径版本化**——最清晰、最常用。

```http
# 版本 v1（旧版）
GET /api/v1/users
Accept: application/json

# 版本 v2（新版，字段有变化）
GET /api/v2/users
Accept: application/json
```

### 11.5 HATEOAS（超媒体作为应用状态引擎）

HATEOAS 是 REST 的成熟度第 3 级，响应中包含相关资源的链接，客户端通过链接导航。

```json
{
    "id": 123,
    "username": "john",
    "email": "john@example.com",
    "links": [
        {
            "rel": "self",
            "href": "/api/users/123",
            "method": "GET"
        },
        {
            "rel": "orders",
            "href": "/api/users/123/orders",
            "method": "GET"
        },
        {
            "rel": "update",
            "href": "/api/users/123",
            "method": "PUT"
        },
        {
            "rel": "delete",
            "href": "/api/users/123",
            "method": "DELETE"
        }
    ]
}
```

### 11.6 RESTful API 设计完整示例

**用户管理 API：**

```
# 用户管理
GET    /api/users                    # 用户列表（分页+过滤）
POST   /api/users                    # 创建用户
GET    /api/users/{id}               # 用户详情
PUT    /api/users/{id}               # 更新用户（完整）
PATCH  /api/users/{id}               # 更新用户（部分）
DELETE /api/users/{id}               # 删除用户

# 用户的子资源
GET    /api/users/{id}/orders        # 用户的订单列表
POST   /api/users/{id}/orders        # 为用户创建订单
GET    /api/users/{id}/orders/{oid}  # 用户指定订单详情

# 特殊操作（用 POST + 动词）
POST   /api/users/{id}/activate      # 激活用户
POST   /api/users/{id}/deactivate    # 停用用户
POST   /api/users/{id}/reset-password # 重置密码
POST   /api/auth/login               # 登录（不是资源操作）
POST   /api/auth/refresh             # 刷新 Token
```

---

## 12. 认证机制

### 12.1 HTTP Basic Authentication

最简单的认证方式，将 `用户名:密码` 用 Base64 编码后放在请求头中。

```http
GET /api/users HTTP/1.1
Authorization: Basic dXNlcjpwYXNz
# "user:pass" 的 Base64 编码
```

**优点：** 实现简单

**缺点：**
- 用户名和密码仅 Base64 编码（不是加密），相当于明文传输
- **必须配合 HTTPS 使用**
- 无法主动注销
- 每次请求都需要发送密码

### 12.2 Token 认证（Bearer Token）

用户登录后，服务器签发一个 Token，客户端在后续请求中携带 Token。

```http
POST /api/auth/login
Content-Type: application/json

{
    "username": "admin",
    "password": "123456"
}

---
HTTP/1.1 200 OK

{
    "access_token": "eyJhbGciOiJIUzI1NiIs...",
    "token_type": "Bearer",
    "expires_in": 3600,
    "refresh_token": "dGhpcyBpcyBh..."
}

---
GET /api/users
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

**JWT（JSON Web Token）结构：**

```
JWT = Header.Payload.Signature

Header:     {"alg": "HS256", "typ": "JWT"}
Payload:    {"sub": "123", "name": "John", "iat": 1516239022}
Signature:  HMACSHA256(base64(Header) + "." + base64(Payload), secret)

完整 Token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.
            eyJzdWIiOiIxMjM0NTY3ODkwIn0.
            dGhpcyBpcyBhIHNpZ25hdHVyZQ==
```

**Java JWT 示例（使用 jjwt 库）：**

```java
// 生成 Token
String secretKey = "your-256-bit-secret";

String token = Jwts.builder()
    .setSubject(userId.toString())
    .claim("username", user.getUsername())
    .claim("role", user.getRole())
    .setIssuedAt(new Date())
    .setExpiration(new Date(System.currentTimeMillis() + 3600_000))
    .signWith(SignatureAlgorithm.HS256, secretKey)
    .compact();

// 验证 Token
Claims claims = Jwts.parser()
    .setSigningKey(secretKey)
    .parseClaimsJws(token)
    .getBody();

Long userId = Long.parseLong(claims.getSubject());
String username = claims.get("username", String.class);
```

### 12.3 OAuth 2.0 授权流程（核心四种模式）

```
OAuth 2.0 核心角色：
┌──────────┐     ┌──────────┐     ┌──────────┐
│  资源拥有者  │     │  客户端    │     │  授权服务器  │
│  (User)   │     │  (App)   │     │  (Auth)  │
└──────────┘     └──────────┘     └──────────┘
                                    ┌──────────┐
                                    │  资源服务器  │
                                    │  (API)   │
                                    └──────────┘
```

| 授权模式 | 适用场景 | 安全性 | 复杂度 |
|---------|---------|--------|--------|
| **授权码（Authorization Code）** | 有后端的 Web 应用 | 最高 | 高 |
| **隐式（Implicit，已废弃）** | SPA 应用（已不推荐） | 低 | 低 |
| **密码（Password Credentials）** | 受信任的内部应用 | 中 | 低 |
| **客户端凭证（Client Credentials）** | 服务间调用（无用户） | 高 | 低 |

**授权码模式流程：**

```
1. 用户点击"使用 GitHub 登录"
2. 客户端重定向到 GitHub 授权页面
   GET https://github.com/oauth/authorize?
       response_type=code&
       client_id=YOUR_CLIENT_ID&
       redirect_uri=https://yourapp.com/callback&
       scope=user:email

3. 用户登录 GitHub 并授权
4. GitHub 重定向回客户端
   GET https://yourapp.com/callback?code=AUTHORIZATION_CODE

5. 客户端用授权码换取 Token
   POST https://github.com/oauth/access_token
   client_id=YOUR_CLIENT_ID&
   client_secret=YOUR_CLIENT_SECRET&
   code=AUTHORIZATION_CODE&
   redirect_uri=https://yourapp.com/callback

6. GitHub 返回 Access Token
   {"access_token":"gho_xxx","token_type":"bearer","scope":"user:email"}

7. 客户端用 Token 访问 API
   GET https://api.github.com/user
   Authorization: Bearer gho_xxx
```

### 12.4 JWT vs Session 认证对比

| 特性 | Session 认证 | JWT Token |
|------|-------------|-----------|
| 状态存储 | 服务端存储 Session | 客户端存储 Token（无状态） |
| 扩展性 | 需集中式 Session 存储 | 天然支持分布式 |
| 主动失效 | 可以（删除 Session） | 不可以（等待过期） |
| 数据大小 | Session ID 很小 | Token 较大（含 payload） |
| 吊销能力 | 即时吊销 | 需要黑名单（有状态化） |
| 实现复杂度 | 简单 | 中等 |
| 典型场景 | 传统 Web 应用 | REST API、微服务 |

---

## 13. 开发调试工具

### 13.1 浏览器 DevTools Network 面板

浏览器开发者工具的 Network 面板是最常用的 HTTP 调试工具。

```
DevTools → Network

关键功能：
├── 请求列表：查看所有网络请求
│   ├── Name: 请求 URL
│   ├── Status: 状态码
│   ├── Type: 资源类型
│   ├── Initiator: 发起者（哪个代码发起的？）
│   ├── Size: 响应大小（实际大小/压缩后大小）
│   ├── Time: 总耗时
│   ├── Waterfall: 时间线瀑布图
│
├── 请求详情（点击某个请求）
│   ├── Headers: 请求头 + 响应头
│   ├── Payload / Request: 请求体
│   ├── Preview: 响应预览（格式化显示）
│   ├── Response: 原始响应
│   ├── Initiator: 调用栈
│   ├── Timing: 请求各阶段耗时
│   │   ├── Queued: 排队时间
│   │   ├── DNS Lookup: DNS 解析
│   │   ├── TCP Connection: TCP 连接
│   │   ├── TLS Handshake: TLS 握手
│   │   ├── Request Sent: 发送请求
│   │   ├── Waiting (TTFB): 等待首字节
│   │   └── Content Download: 内容下载
│   └── Cookies: 请求 Cookie + 响应 Set-Cookie
│
├── 过滤功能
│   ├── XHR/Fetch: 只显示 AJAX 请求
│   ├── JS/CSS/Img: 按资源类型过滤
│   └── 搜索: 搜索请求内容
│
└── 其他
    ├── Preserve log: 页面跳转后保留日志
    ├── Disable cache: 禁用缓存
    └── Throttling: 模拟慢网速
```

### 13.2 curl 命令详解

```bash
# 基础 GET 请求
curl https://api.example.com/users

# 显示响应头
curl -i https://api.example.com/users

# 只显示响应头（HEAD 方法）
curl -I https://api.example.com/users

# 显示详细通信过程（包含 TLS 握手）
curl -v https://api.example.com/users

# POST JSON 数据
curl -X POST https://api.example.com/users \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"123456"}'

# 带查询参数
curl "https://api.example.com/users?page=1&size=20"

# 带 Cookie
curl -b "sessionId=abc123" https://api.example.com/profile

# 设置 Cookie
curl -c cookies.txt https://api.example.com/login

# 文件上传
curl -F "file=@/path/to/photo.jpg" https://api.example.com/upload

# 设置超时
curl --connect-timeout 5 --max-time 10 https://api.example.com

# 跟随重定向
curl -L https://short.url/abc

# 自定义 User-Agent
curl -A "Mozilla/5.0 (Windows NT 10.0; Win64; x64)" https://example.com

# 下载文件
curl -o output.zip https://example.com/file.zip

# 带认证
curl -u username:password https://api.example.com/protected
curl -H "Authorization: Bearer token123" https://api.example.com/protected

# 只检查响应状态码
curl -o /dev/null -s -w "%{http_code}" https://api.example.com

# 测量请求时间
curl -o /dev/null -s -w "\
  DNS: %{time_namelookup}s\n\
  TCP: %{time_connect}s\n\
  TLS: %{time_appconnect}s\n\
  TTFB: %{time_starttransfer}s\n\
  Total: %{time_total}s\n" https://api.example.com
```

### 13.3 Postman 使用技巧

```
Postman 常用功能：
├── 集合（Collections）: 组织 API 请求
│   ├── 文件夹分类
│   ├── 继承认证配置
│   └── 导出/分享
├── 环境（Environments）
│   ├── 变量: {{base_url}}, {{token}}
│   ├── 多环境: dev/staging/prod 切换
│   └── 预置脚本: Pre-request Script
├── 测试脚本（Tests）
│   ├── 状态码断言
│   ├── JSON Schema 验证
│   └── 自动化测试
├── 高级功能
│   ├── Monitor: API 监控
│   ├── Mock Server: 模拟接口
│   ├── API Documentation: 自动生成文档
│   └── GraphQL 支持
└── 快捷键
    ├── Ctrl + Enter: 发送请求
    ├── Ctrl + S: 保存
    └── Ctrl + D: 复制请求
```

---

## 14. 最佳实践与常见陷阱

### 14.1 最佳实践

| 类别 | 建议 |
|------|------|
| **URL 设计** | 使用名词复数、小写、连字符；层级清晰；查询参数用于过滤 |
| **状态码** | 正确使用 HTTP 状态码，不要所有响应都返回 200 |
| **安全性** | 始终使用 HTTPS；敏感数据不在 URL 中传递；Cookie 设置 HttpOnly + Secure |
| **性能** | 启用 Gzip 压缩；合理使用缓存（Cache-Control、ETag）；减少重定向 |
| **幂等性** | POST 非幂等要做好去重；PUT/DELETE 应幂等 |
| **错误处理** | 统一错误响应格式；错误信息有实际帮助；不暴露敏感信息 |
| **API 版本** | 及时废弃旧版本；给客户端足够的迁移时间 |
| **分页** | 始终为列表接口加分页；支持排序和过滤 |

### 14.2 常见陷阱

| 陷阱 | 问题 | 正确做法 |
|------|------|---------|
| **200 表示一切** | 即使 4xx/5xx 错误也返回 200，把错误码放 body 里 | 正确使用 HTTP 状态码 |
| **暴露敏感信息** | 错误信息直接暴露 SQL 语句、堆栈 | 使用通用错误信息，内部记录详细日志 |
| **Session 在分布式下失效** | 用户登录后请求被路由到其他服务器 | 使用 Redis 集中式 Session |
| **CSRF 防护缺失** | 跨站请求伪造攻击 | 使用 SameSite Cookie 或 CSRF Token |
| **Cookie 路径/域配置错误** | Cookie 在某些路径下不生效 | 明确设置 Path 和 Domain |
| **CORS 配置过松** | `Access-Control-Allow-Origin: *` 在生产环境使用 | 指定具体的前端域名 |
| **分页默认值过大** | 不传分页参数时返回所有数据 | 设置合理的默认分页大小 |
| **API 不含版本号** | 升级 API 导致旧客户端崩溃 | 从一开始就加入版本号 |

### 14.3 安全性检查清单

```
HTTPS 安全配置：
□ TLS 1.2/1.3 启用，禁用 SSL 2.0/3.0、TLS 1.0/1.1
□ 使用安全的密码套件（禁用 RC4、3DES、CBC 模式）
□ HSTS 头配置（Strict-Transport-Security）
□ 证书链完整

Cookie 安全：
□ HttpOnly 属性（防 XSS 读取 Cookie）
□ Secure 属性（仅 HTTPS 发送）
□ SameSite=Lax 或 SameSite=Strict
□ 敏感操作使用独立的 CSRF Token

API 安全：
□ 速率限制（Rate Limiting）
□ 输入验证（参数校验、Content-Type 验证）
□ 输出编码（防 XSS）
□ 不暴露服务器版本信息
□ 日志不记录密码等敏感数据
□ 使用 PreparedStatement 防 SQL 注入
```

---

## 15. 面试题

### 基础题

**Q1: HTTP 和 HTTPS 有什么区别？**

A: 区别如下：
- 加密：HTTPS 使用 TLS 协议加密通信，HTTP 是明文
- 端口：HTTP 默认 80，HTTPS 默认 443
- 证书：HTTPS 需要 CA 证书
- SEO：搜索引擎优先收录 HTTPS 页面
- 性能：HTTPS 有 TLS 握手开销（但 TLS 1.3 已大幅优化）

**Q2: GET 和 POST 有什么区别？**

A:
- 参数传递：GET 通过 URL query string，POST 通过请求体
- 数据长度：GET 受 URL 长度限制（通常 2KB-8KB），POST 不受限
- 安全性：GET 参数暴露在 URL 中，POST 在请求体中相对安全（但都不如 HTTPS）
- 缓存：GET 请求可被浏览器缓存，POST 通常不缓存
- 幂等性：GET 幂等，POST 非幂等
- 编码：GET 只能 URL 编码，POST 支持多种编码

**Q3: 什么是 Cookie？什么是 Session？它们的区别？**

A:
- Cookie 存储在客户端（浏览器），Session 存储在服务器端
- Cookie 大小限制 4KB，Session 大小取决于服务器
- Cookie 不够安全（可被篡改），Session 相对安全
- 两者配合使用：Session ID 通过 Cookie 传递

**Q4: 什么是跨域？如何解决？**

A: 浏览器的同源策略阻止不同源的页面访问资源。解决方案：
- CORS（主流）：服务器设置响应头
- JSONP：仅支持 GET 请求（已过时）
- 反向代理：Nginx 转发（同源代理）
- WebSocket：不受同源策略限制

### 进阶题

**Q5: HTTPS 的完整握手过程是怎样的？**

A: TLS 1.2 握手：
1. ClientHello：客户端发送支持的 TLS 版本、密码套件、随机数
2. ServerHello：服务器选择 TLS 版本、密码套件、返回随机数
3. Certificate：服务器发送证书（含公钥）
4. ClientKeyExchange：客户端用公钥加密 Pre-Master Secret
5. 双方用随机数 + Pre-Master 计算会话密钥
6. ChangeCipherSpec + Finished：切换加密通信

TLS 1.3 将以上过程压缩到 1 个 RTT。

**Q6: JWT 的优缺点？**

A: 优点：
- 无状态，天然支持分布式
- 包含用户信息，减少数据库查询
- 跨域友好（只要密钥一致即可）

缺点：
- 无法主动注销（需等过期）
- Payload 只 Base64 编码，不能放敏感信息
- Token 体积较大，增加请求大小
- 刷新 Token 增加复杂度

**Q7: 分布式 Session 问题如何解决？**

A: 三种主流方案：
1. **Redis 集中式存储**：所有服务器的 Session 读写 Redis
2. **Session 粘滞**：负载均衡按用户 IP 固定路由
3. **JWT 无状态**：用户信息编码在 Token 中，服务器不保存状态

**Q8: RESTful API 中 POST 和 PUT 的区别？**

A:
- POST 用于创建资源，服务器决定资源 ID；PUT 用于更新/替换资源，客户端指定资源 ID
- POST 非幂等，多次创建产生多个资源；PUT 幂等
- POST /users（创建），PUT /users/123（更新）

**Q9: 什么是 CORS 预检请求？什么情况会触发？**

A: 浏览器在发送"非简单请求"前，会先发送一个 OPTIONS 请求询问服务器是否允许。
触发条件（满足之一）：
- 使用 PUT/DELETE/PATCH 等方法
- Content-Type 不是 application/x-www-form-urlencoded、multipart/form-data、text/plain
- 包含自定义请求头（如 Authorization）

**Q10: HTTP/2 相比 HTTP/1.1 有哪些改进？**

A:
- **二进制分帧**：不再是文本协议，解析更高效
- **多路复用**：同一 TCP 连接并发传输多个请求
- **头部压缩**：HPACK 算法减少冗余头部
- **服务器推送**：服务器主动推送相关资源
- **请求优先级**：可设置请求优先级

**Q11: SameSite Cookie 的三个值有什么区别？**

A:
- `Strict`：严格模式，跨站请求一律不发送 Cookie（最安全，但用户从外部链接访问时无法保持登录）
- `Lax`：宽松模式，GET 导航请求发送 Cookie，POST 等不发送（**浏览器默认值**）
- `None`：关闭 SameSite 保护，任意跨站请求都发送 Cookie（必须同时设置 Secure，即仅 HTTPS）

**Q12: HTTP 状态码 301 和 302 有什么区别？**

A:
- 301 Moved Permanently：永久重定向，搜索引擎会更新 URL
- 302 Found：临时重定向，搜索引擎保留原 URL
- 使用上：域名变更用 301，登录后跳转用 302，Post/Redirect/Get 模式用 303
- 注意：301/302 在某些浏览器中会将 POST 改为 GET，如果需保持请求方法不变，应使用 307/308

### 实用场景题

**Q13: 用户在浏览器输入 URL 到页面加载完成，经历了什么？**

A:
1. DNS 解析：域名 → IP 地址
2. TCP 连接：三次握手
3. TLS 握手（HTTPS）
4. 发送 HTTP 请求
5. 服务器处理请求，返回 HTTP 响应
6. 浏览器解析 HTML
7. 浏览器发送获取 CSS、JS、图片等资源的请求
8. 页面渲染（DOM 树 → CSSOM 树 → 渲染树 → 布局 → 绘制）

**Q14: 在 Java Web 应用中，如何在 Filter 中读取请求体多次？**

A: 默认情况下，Servlet 的请求体只能读取一次（InputStream 只能消费一次）。解决方案是使用**包装器**：

```java
public class RepeatableRequestWrapper extends HttpServletRequestWrapper {
    private byte[] body;

    public RepeatableRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        // 缓存请求体
        this.body = request.getInputStream().readAllBytes();
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream bais = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override public boolean isFinished() { return bais.available() == 0; }
            @Override public boolean isReady() { return true; }
            @Override public int read() { return bais.read(); }
            @Override public void setReadListener(ReadListener listener) {}
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }
}
```

---

## 参考资源

- [MDN Web Docs: HTTP](https://developer.mozilla.org/zh-CN/docs/Web/HTTP) -- Mozilla 官方 HTTP 文档
- [RFC 7230-7235](https://tools.ietf.org/html/rfc7230) -- HTTP/1.1 规范
- [RFC 7540](https://tools.ietf.org/html/rfc7540) -- HTTP/2 规范
- [RFC 8446](https://tools.ietf.org/html/rfc8446) -- TLS 1.3 规范
- [RFC 7519](https://tools.ietf.org/html/rfc7519) -- JSON Web Token (JWT)
- [RFC 6749](https://tools.ietf.org/html/rfc6749) -- OAuth 2.0 授权框架
- 《图解 HTTP》 -- 上野宣，HTTP 入门最佳书籍
- 《HTTP 权威指南》 -- David Gourley，HTTP 深度参考

---

*最后更新: 2026-05-31*
