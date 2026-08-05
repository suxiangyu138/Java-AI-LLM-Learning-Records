# 01 - HTTP 协议与网络基础

> 定位：HTTP 版本演进、请求响应结构、状态码体系、HTTPS/TLS、Cookie 机制——Web 通信的底层语言

## 📚 目录

1. [HTTP 协议版本演进](#1-http-协议版本演进)
2. [请求与响应结构](#2-请求与响应结构)
3. [状态码体系](#3-状态码体系)
4. [HTTPS 与 TLS](#4-https-与-tls)
5. [Cookie 机制](#5-cookie-机制)
6. [常见 HTTP 头](#6-常见-http-头)

---

## 1. HTTP 协议版本演进

| 版本 | 特点 | 问题 |
|------|------|------|
| HTTP/1.0 | 每请求新连接 | 连接开销大 |
| HTTP/1.1 | 持久连接 + 管道化 | 队头阻塞 |
| HTTP/2 | 多路复用 + 头部压缩 | TCP 层队头阻塞 |
| HTTP/3 | QUIC（UDP）+ 0-RTT | 生态普及中 |

```
⚠️ 面试必答：
"HTTP 演进主线——1.1 持久连接（解决连接开销）、
 2.0 多路复用（解决队头阻塞）、
 3.0 QUIC（解决 TCP 队头阻塞 + 快速握手）。"
```

---

## 2. 请求与响应结构

### 2.1 请求结构

```
请求行：方法 路径 协议版本
   GET /api/users?id=1 HTTP/1.1
请求头：
   Host: example.com
   User-Agent: Chrome/130
   Accept: application/json
   Authorization: Bearer xxx
请求体（POST/PUT）：
   {"name":"张三"}
```

### 2.2 响应结构

```
状态行：协议版本 状态码 描述
   HTTP/1.1 200 OK
响应头：
   Content-Type: application/json
   Set-Cookie: session=xxx
响应体：
   {"code":0,"data":{...}}
```

| HTTP 方法 | 语义 | 幂等 |
|-----------|------|:---:|
| GET | 查询 | ✅ |
| POST | 创建 | ❌ |
| PUT | 全量更新 | ✅ |
| PATCH | 部分更新 | ❌ |
| DELETE | 删除 | ✅ |

> 🎯 **要点**：请求 = 行 + 头 + 体；响应 = 行 + 头 + 体。**方法语义 + 幂等性**是 API 设计基础（GET/PUT/DELETE 幂等可重试）。

---

## 3. 状态码体系

| 分类 | 范围 | 含义 |
|------|:---:|------|
| 2xx | 200-299 | 成功（200 OK/201 Created/204 No Content） |
| 3xx | 300-399 | 重定向（301 永久/302 临时/304 缓存） |
| 4xx | 400-499 | 客户端错误（400 参数/401 未认证/403 无权限/404 不存在） |
| 5xx | 500-599 | 服务端错误（500 内部/502 网关/503 不可用/504 超时） |

```
⚠️ 高频辨析：
  401 vs 403：未认证（没登录）vs 无权限（权限不够）
  301 vs 302：永久重定向（SEO）vs 临时重定向
  502 vs 504：网关收到无效响应 vs 网关超时

⚠️ 面试必答：
"状态码四类——2xx 成功、3xx 重定向、
 4xx 客户端错、5xx 服务端错；
 401/403、301/302、502/504 是辨析重点。"
```

---

## 4. HTTPS 与 TLS

### 4.1 HTTPS 是什么

```
HTTPS = HTTP + TLS（加密传输）

TLS 握手流程：
  ① 客户端 Hello（支持的加密套件）
  ② 服务器 Hello + 证书（公钥）
  ③ 客户端验证证书 + 生成对称密钥
  ④ 用公钥加密密钥发给服务器
  ⑤ 双方用对称密钥加密通信

⚠️ 面试必答：
"HTTPS 两层——证书验证身份（非对称）、
 密钥加密数据（对称）；
 握手的关键是'安全地交换对称密钥'。"
```

### 4.2 HTTPS 全链路

```
全链路 HTTPS：
  浏览器 ←→ Nginx（443，证书在 Nginx）
  Nginx ←→ Tomcat（内网 HTTP 或 TLS）
  
⚠️ 面试必答：
"证书通常部署在 Nginx 层——
 内部链路（Nginx → Tomcat）可 HTTP；
 全链路加密（mTLS）用于金融等强安全场景。"
```

---

## 5. Cookie 机制

### 5.1 Cookie 是什么

```
Cookie = 服务器下发、浏览器存储的小文本（≤4KB）

流程：
  ① 服务器响应 Set-Cookie: session_id=abc
  ② 浏览器存储
  ③ 后续请求自动携带 Cookie 头

属性：
  Domain/Path：作用域
  Expires/Max-Age：过期
  HttpOnly：⚠️ JS 不可读（防 XSS 窃取）
  Secure：仅 HTTPS 传输
  SameSite：CSRF 防护（Lax/Strict）

⚠️ 面试必答：
"Cookie 四属性关键——HttpOnly（防 XSS）、
 Secure（防明文）、SameSite（防 CSRF）、
 Domain/Path（作用域）。"
```

### 5.2 Cookie vs 其他存储

| 存储 | 大小 | 传输 | 场景 |
|------|:---:|:---:|------|
| Cookie | 4KB | 随请求 | 会话标识 |
| localStorage | 5MB | 不传输 | 前端数据 |
| sessionStorage | 5MB | 不传输 | 会话级 |

---

## 6. 常见 HTTP 头

| 头 | 作用 | 安全相关 |
|-----|------|:---:|
| Content-Type | 内容类型（JSON/表单） | |
| Authorization | 认证令牌（Bearer JWT） | ✅ |
| Cache-Control | 缓存策略（max-age） | |
| CORS 头 | 跨域（Access-Control-*） | ✅ |
| X-Forwarded-For | 真实客户端 IP | ⚠️ 可伪造 |
| Content-Security-Policy | CSP 内容安全策略 | ✅ 防 XSS |
| Strict-Transport-Security | HSTS 强制 HTTPS | ✅ |

```
⚠️ 面试必答：
"安全头三件套——CSP（防 XSS 脚本执行）、
 HSTS（强制 HTTPS）、CORS 配置（跨域白名单）；
 认证靠 Authorization 头（JWT）。"
```

---

> 🎯 **核心要点**：HTTP 体系 = **版本演进**（1.1 持久 → 2.0 复用 → 3.0 QUIC）+ **请求响应结构**（行/头/体）+ **状态码四类**（401/403、301/302 辨析）+ **HTTPS**（证书验身份 + 密钥加密）+ **Cookie 四属性**（HttpOnly/Secure/SameSite）+ **安全头**（CSP/HSTS/CORS）。"一次请求的协议层"全在这。

---

**返回总览**：[00-Web开发生态总览](00-Web开发生态总览.md) | **下一篇**：[02-Servlet容器与MVC框架](02-Servlet容器与MVC框架.md)
