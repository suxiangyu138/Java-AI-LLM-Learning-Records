# 01 - HTTP 协议规范详解

> 🎯 HTTP 是万维网的基石 — HTTP/1.1 的长连接、HTTP/2 的多路复用、HTTP/3 的 QUIC，理解协议演进才能写出高性能的 Web 应用

---

## 目录

1. [HTTP 协议演进](#1-http-协议演进)
2. [报文结构](#2-报文结构)
3. [请求方法与幂等性](#3-请求方法与幂等性)
4. [关键 Header 字段](#4-关键-header-字段)
5. [Cookie 与 Session](#5-cookie-与-session)
6. [缓存机制](#6-缓存机制)

---

## 1. HTTP 协议演进

| 版本 | 传输 | 连接 | 特点 |
|------|------|------|------|
| **HTTP/1.0** | TCP | 短连接（每个请求一次握手） | 效率低 |
| **HTTP/1.1** | TCP | 长连接（keep-alive） | 队头阻塞（Head-of-Line Blocking） |
| **HTTP/2** | TCP | 多路复用（1 连接 N 流） | 二进制帧、头部压缩、Server Push |
| **HTTP/3** | QUIC（UDP） | 0-RTT 握手 | 无队头阻塞、连接迁移 |

```text
HTTP/1.1 队头阻塞：
  连接1: [Req1 → Wait... → Res1] → [Req2 → Wait... → Res2]
  → Req3 必须等 Req2 完成，即使 Req3 的资源已就绪

HTTP/2 多路复用：
  连接1: [Stream1: Req1 → Res1 fragments]
         [Stream2: Req2 → Res2 fragments]  ← 交错传输
         [Stream3: Req3 → Res3 fragments]
  → Stream 之间不阻塞
```

---

## 2. 报文结构

### 请求报文

```text
GET /api/users/1 HTTP/1.1                    ← 请求行
Host: api.example.com                         ← 请求头
Authorization: Bearer eyJhbGci...
Content-Type: application/json
Accept: application/json
                                               ← 空行
{ "name": "张三" }                             ← 请求体（GET 通常无）
```

### 响应报文

```text
HTTP/1.1 200 OK                               ← 状态行
Content-Type: application/json                 ← 响应头
Content-Length: 95
Cache-Control: max-age=3600
Set-Cookie: SESSIONID=abc123; HttpOnly; Secure
                                               ← 空行
{ "id": 1, "name": "张三", "email": "zhangsan@example.com" }  ← 响应体
```

---

## 3. 请求方法与幂等性

| 方法 | 幂等 | 安全 | 说明 | RESTful 示例 |
|------|:---:|:---:|------|-------------|
| **GET** | ✅ | ✅ | 获取资源 | `GET /users/1` |
| **POST** | ❌ | ❌ | 创建资源 | `POST /users` |
| **PUT** | ✅ | ❌ | 全量更新 | `PUT /users/1` |
| **PATCH** | ❌ | ❌ | 部分更新 | `PATCH /users/1` |
| **DELETE** | ✅ | ❌ | 删除资源 | `DELETE /users/1` |
| **HEAD** | ✅ | ✅ | 仅获取响应头 | `HEAD /users/1` |
| **OPTIONS** | ✅ | ✅ | 查询支持的方法 | 预检请求 |

> ⚠️ **幂等 vs 非幂等**：GET/PUT/DELETE 执行 N 次结果相同 → 可安全重试。POST/PATCH 每次可能产生不同结果 → 不可盲目重试。

---

## 4. 关键 Header 字段

### 请求头

| Header | 说明 | 示例 |
|--------|------|------|
| `Authorization` | 认证信息 | `Bearer <jwt>` |
| `Content-Type` | 请求体类型 | `application/json` |
| `Accept` | 期望的响应类型 | `application/json` |
| `User-Agent` | 客户端标识 | `Mozilla/5.0 ...` |
| `Cookie` | 发送 Cookie | `SESSIONID=abc123` |
| `Referer` | 来源页面 | `https://example.com/page` |
| `X-Forwarded-For` | 原始客户端 IP（代理后） | `10.0.1.100` |

### 响应头

| Header | 说明 | 示例 |
|--------|------|------|
| `Content-Type` | 响应体类型 | `application/json; charset=utf-8` |
| `Cache-Control` | 缓存策略 | `max-age=3600, public` |
| `Set-Cookie` | 设置 Cookie | `SESSIONID=abc; HttpOnly; Secure` |
| `Location` | 重定向地址 | `https://example.com/new` |
| `Access-Control-Allow-Origin` | CORS 允许源 | `*` 或 `https://example.com` |
| `Strict-Transport-Security` | HSTS | `max-age=31536000` |

---

## 5. Cookie 与 Session

| 属性 | 说明 | 安全建议 |
|------|------|----------|
| `HttpOnly` | 禁止 JS 访问（防 XSS） | ✅ 必设 |
| `Secure` | 仅 HTTPS 传输 | ✅ 必设 |
| `SameSite` | 跨站请求控制 | `Strict` / `Lax` / `None` |
| `Domain` | 作用域 | 限定范围 |
| `Path` | 路径限制 | `/` |
| `Max-Age` | 过期时间（秒） | 合理设置 |

```java
// Spring Boot 设置安全 Cookie
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody LoginRequest req) {
    // ...
    ResponseCookie cookie = ResponseCookie.from("token", jwt)
        .httpOnly(true)
        .secure(true)
        .sameSite("Strict")
        .maxAge(7200)
        .path("/")
        .build();
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(result);
}
```

---

## 6. 缓存机制

### 强缓存 vs 协商缓存

| 类型 | Header | 行为 |
|------|--------|------|
| **强缓存** | `Cache-Control: max-age=3600` | 1 小时内直接用缓存，不发请求 |
| **强缓存** | `Expires: Thu, 15 Jan 2025 10:00:00 GMT` | 过期前直接用缓存（HTTP/1.0） |
| **协商缓存** | `ETag` / `If-None-Match` | 发请求，资源未变返回 304 |
| **协商缓存** | `Last-Modified` / `If-Modified-Since` | 发请求，资源未变返回 304 |

```text
缓存决策流程：
  1. Cache-Control: max-age 是否过期？
     ├── 否 → 强缓存命中（200 from disk cache）
     └── 是 → 发请求，带 If-None-Match / If-Modified-Since
          ├── 304 Not Modified → 协商缓存命中
          └── 200 → 返回新数据 + 新 Cache-Control
```

```java
// Spring Boot 缓存控制
@GetMapping("/users/{id}")
public ResponseEntity<User> getUser(@PathVariable Long id) {
    User user = userService.get(id);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
        .eTag(String.valueOf(user.getVersion()))  // 用版本号做 ETag
        .body(user);
}
```

> 🎯 **后端口诀**：GET=幂等可缓存、POST=非幂等不可重试、HttpOnly+Secure=Cookie 标配、304=协商缓存省钱。HTTP/2 多路复用是性能标配。
