# HTTP 协议与浏览器原理
> 后端开发者必须掌握的 HTTP 协议细节与浏览器工作机制，理解请求从浏览器到服务器的完整链路

## 目录

1. [HTTP 请求与响应结构](#1-http-请求与响应结构)
2. [HTTP 方法深度解析](#2-http-方法深度解析)
3. [状态码速查表](#3-状态码速查表)
4. [请求头详解](#4-请求头详解)
5. [响应头详解](#5-响应头详解)
6. [Content-Type 深度解析](#6-content-type-深度解析)
7. [HTTP 缓存机制](#7-http-缓存机制)
8. [Cookie 与 Session 机制](#8-cookie-与-session-机制)
9. [HTTP/1.1 vs HTTP/2 vs HTTP/3](#9-http11-vs-http2-vs-http3)
10. [HTTPS/TLS 握手简化](#10-httpstls-握手简化)
11. [浏览器页面加载过程](#11-浏览器页面加载过程)
12. [浏览器渲染流程](#12-浏览器渲染流程)
13. [完整链路追踪：浏览器 -> SpringBoot](#13-完整链路追踪浏览器---springboot)

---

## 1. HTTP 请求与响应结构

### 1.1 请求消息结构

```
POST /api/users HTTP/1.1                  ← 请求行（方法 + URI + 协议版本）
Host: api.example.com                     ← 请求头（key: value）
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Content-Length: 42
User-Agent: Mozilla/5.0 (Windows NT 10.0)
Accept: application/json
Cache-Control: no-cache

{ "name": "张三", "age": 25 }              ← 请求体（可选，POST/PUT/PATCH 携带）
```

| 组成部分 | 说明 | 后端关注点 |
|---------|------|-----------|
| 请求行 | `METHOD /path HTTP/version` | 路由匹配、方法校验 |
| 请求头 | 键值对，携带元数据 | 认证、缓存、内容协商 |
| 空行 | `\r\n` 分隔头部和 body | 解析边界 |
| 请求体 | 实际传输的数据 | 反序列化、校验 |

### 1.2 响应消息结构

```
HTTP/1.1 200 OK                           ← 状态行（协议版本 + 状态码 + 状态描述）
Content-Type: application/json            ← 响应头
Cache-Control: max-age=3600
Set-Cookie: sessionId=abc123; HttpOnly; Path=/
X-Request-Id: req-001
Access-Control-Allow-Origin: *

{ "id": 1, "name": "张三" }                 ← 响应体
```

> 💡 后端通过 `Content-Type` 告诉浏览器如何解析响应体，通过 `Cache-Control` 控制浏览器缓存行为。

---

## 2. HTTP 方法深度解析

| 方法 | 幂等 | 安全 | 可缓存 | 请求体 | 后端语义 |
|------|------|------|--------|--------|---------|
| GET | 是 | 是 | 是 | 不应有 | 查询资源 |
| POST | 否 | 否 | 可缓存（罕见） | 有 | 创建资源 |
| PUT | 是 | 否 | 否 | 有 | 全量替换资源 |
| PATCH | 否 | 否 | 否 | 有 | 部分更新资源 |
| DELETE | 是 | 否 | 否 | 可有可无 | 删除资源 |
| HEAD | 是 | 是 | 是 | 不应有 | 仅获取响应头 |
| OPTIONS | 是 | 是 | 否 | 不应有 | CORS 预检请求 |
| TRACE | 是 | 是 | 否 | 不应有 | 诊断调试 |

### 2.1 核心概念定义

- **幂等**：同一个请求执行多次，服务端资源状态不变（多次 DELETE 返回 404 也是幂等）
- **安全**：请求不会修改服务端资源（GET/HEAD/OPTIONS 不会改变数据）
- **可缓存**：响应可以被浏览器或代理缓存

### 2.2 实战要点

```java
// Spring Boot 中 REST 接口的标准映射
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/{id}")            // 查询 - 幂等、安全
    public User getUser(@PathVariable Long id) { ... }

    @PostMapping                    // 创建 - 非幂等、非安全
    public User createUser(@RequestBody User user) { ... }

    @PutMapping("/{id}")            // 全量更新 - 幂等
    public User updateUser(@PathVariable Long id, @RequestBody User user) { ... }

    @PatchMapping("/{id}")          // 部分更新 - 非幂等
    public User patchUser(@PathVariable Long id, @RequestBody Map<String, Object> fields) { ... }

    @DeleteMapping("/{id}")         // 删除 - 幂等
    public void deleteUser(@PathVariable Long id) { ... }
}
```

> ⚠️ PATCH 通常不是幂等的：例如 `PATCH /users/1` 请求体为 `{ "count": "count+1" }`，重复执行会导致 count 递增。PUT 是幂等的，因为它是全量替换。

### 2.3 OPTIONS 与 CORS 预检

浏览器在发送"非简单请求"（如自定义头、非 GET/POST 方法）之前，会先发一个 OPTIONS 预检请求：

```
OPTIONS /api/users HTTP/1.1
Origin: https://frontend.example.com
Access-Control-Request-Method: PUT
Access-Control-Request-Headers: Authorization, Content-Type
```

服务端需要响应允许的跨域策略：

```
HTTP/1.1 204 No Content
Access-Control-Allow-Origin: https://frontend.example.com
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
Access-Control-Allow-Headers: Authorization, Content-Type
Access-Control-Max-Age: 3600
```

---

## 3. 状态码速查表

### 2xx 成功

| 状态码 | 含义 | 后端使用场景 |
|--------|------|-------------|
| 200 OK | 请求成功 | GET 查询成功、PUT 更新成功 |
| 201 Created | 资源创建成功 | POST 创建资源后返回，附带 `Location` 头指向新资源 URI |
| 204 No Content | 请求成功但无返回体 | DELETE 删除成功、某些 PUT 操作 |

```java
@PostMapping
public ResponseEntity<User> createUser(@RequestBody User user) {
    User saved = service.save(user);
    return ResponseEntity
        .created(URI.create("/api/users/" + saved.getId()))  // 201 + Location 头
        .body(saved);
}

@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();  // 204
}
```

### 3xx 重定向

| 状态码 | 含义 | 后端场景 |
|--------|------|----------|
| 301 Moved Permanently | 永久重定向 | API 版本迁移，搜索引擎会更新 URL |
| 302 Found | 临时重定向 | 登录后跳转、短链接 |
| 304 Not Modified | 资源未修改 | 缓存验证，配合 `ETag` / `Last-Modified` |

> ⚠️ 后端 API 设计应避免重定向，301/302 通常用于 Web 页面跳转。304 是后端实现缓存的核心机制。

### 4xx 客户端错误

| 状态码 | 含义 | 后端场景 |
|--------|------|----------|
| 400 Bad Request | 请求格式错误 | 参数校验失败、JSON 解析失败 |
| 401 Unauthorized | 未认证 | 未提供 token 或 token 过期 |
| 403 Forbidden | 无权限 | 已认证但无操作权限 |
| 404 Not Found | 资源不存在 | URI 路由不匹配、资源 ID 不存在 |
| 405 Method Not Allowed | 方法不允许 | GET 端点被 POST 请求 |
| 409 Conflict | 资源冲突 | 唯一键冲突、版本冲突（乐观锁） |
| 415 Unsupported Media Type | 格式不支持 | 请求的 Content-Type 服务端不支持 |
| 429 Too Many Requests | 请求过频繁 | 限流触发 |

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
    return ResponseEntity.badRequest()
        .body(new ErrorResponse(400, "参数校验失败", e.getBindingResult().getAllErrors()));
}
```

### 5xx 服务端错误

| 状态码 | 含义 | 后端场景 |
|--------|------|----------|
| 500 Internal Server Error | 服务器内部错误 | 未捕获异常、空指针、数据库宕机 |
| 502 Bad Gateway | 网关错误 | Nginx 无法连接后端服务（后端挂了） |
| 503 Service Unavailable | 服务不可用 | 服务过载、正在重启、熔断 |
| 504 Gateway Timeout | 网关超时 | 后端响应超时，Nginx 主动断开 |

> 💡 502 和 504 是运维层面的信号：502 表示连接失败（服务没启动），504 表示连接上了但响应太慢（超时配置问题）。

---

## 4. 请求头详解

| 请求头 | 示例 | 后端用途 |
|--------|------|---------|
| `Content-Type` | `application/json` | 解析请求体的格式 |
| `Accept` | `application/json` | 内容协商，决定响应格式 |
| `Authorization` | `Bearer eyJhbG...` | JWT token 认证 |
| `Cookie` | `sessionId=abc123; lang=zh` | Session 维持 |
| `User-Agent` | `Mozilla/5.0 ...` | 客户端识别（日志、设备适配） |
| `Cache-Control` | `no-cache` | 缓存策略控制 |
| `If-Modified-Since` | `Wed, 21 Oct 2023 07:28:00 GMT` | 条件请求，配合 `Last-Modified` |
| `If-None-Match` | `"etag-value-123"` | 条件请求，配合 `ETag` |
| `Origin` | `https://frontend.example.com` | CORS 跨域来源 |
| `Referer` | `https://example.com/page1` | 请求来源页面 |
| `X-Request-Id` | `req-uuid-001` | 链路追踪 ID |

```java
// Spring Boot 中读取请求头
@GetMapping("/info")
public Map<String, String> getHeaders(@RequestHeader HttpHeaders headers) {
    return Map.of(
        "user-agent", headers.getFirst("User-Agent"),
        "authorization", headers.getFirst("Authorization"),
        "x-request-id", headers.getFirst("X-Request-Id")
    );
}
```

> 💡 `X-Request-Id` 是微服务链路追踪的重要头。前端每次请求生成唯一 ID，后端透传（gateway 自动注入），方便跨服务追踪日志。

---

## 5. 响应头详解

| 响应头 | 示例 | 后端用途 |
|--------|------|---------|
| `Content-Type` | `application/json; charset=utf-8` | 告知浏览器响应体格式 |
| `Set-Cookie` | `sessionId=abc123; HttpOnly; Secure` | 向浏览器写入 Cookie |
| `Cache-Control` | `public, max-age=3600` | 缓存策略 |
| `ETag` | `"v2-user-123"` | 资源版本标识，配合 `If-None-Match` |
| `Location` | `/api/users/123` | 重定向目标 URI（201 创建时使用） |
| `Access-Control-Allow-Origin` | `*` 或具体域名 | CORS 跨域允许来源 |
| `Access-Control-Expose-Headers` | `X-Total-Count` | 允许前端读取的自定义头 |
| `X-Request-Id` | `req-001` | 回传请求追踪 ID |
| `Retry-After` | `120` | 429 限流时告知客户端何时重试 |

---

## 6. Content-Type 深度解析

### 6.1 application/json

> 前后端分离项目最常用的格式。

```javascript
// 前端发送
fetch('/api/users', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name: '张三', age: 25 })
});

// Spring Boot 接收
@PostMapping("/api/users")
public User create(@RequestBody User user) { ... }
```

### 6.2 application/x-www-form-urlencoded

> 传统的 HTML 表单提交格式，body 是 `key=value&key2=value2` 格式。

```
POST /api/login HTTP/1.1
Content-Type: application/x-www-form-urlencoded

username=zhangsan&password=123456&rememberMe=true
```

```java
// Spring Boot 接收
@PostMapping("/api/login")
public String login(@RequestParam String username,
                    @RequestParam String password,
                    @RequestParam(defaultValue = "false") boolean rememberMe) { ... }
```

### 6.3 multipart/form-data

> 文件上传必须使用此格式，每个字段有独立的 boundary 分隔。

```javascript
// 前端文件上传
const formData = new FormData();
formData.append('file', fileInput.files[0]);
formData.append('description', '用户头像');

fetch('/api/upload', {
    method: 'POST',
    body: formData  // 浏览器自动设 Content-Type: multipart/form-data; boundary=----...
});
```

```java
// Spring Boot 接收
@PostMapping("/api/upload")
public String upload(@RequestParam("file") MultipartFile file,
                     @RequestParam("description") String description) {
    String originalFilename = file.getOriginalFilename();
    long size = file.getSize();
    byte[] content = file.getBytes();
    // ...
}
```

---

## 7. HTTP 缓存机制

### 7.1 强缓存（本地缓存，不发请求）

| Cache-Control 值 | 含义 | 后端设置场景 |
|------------------|------|-------------|
| `max-age=3600` | 资源在 1 小时内有效 | 静态资源（JS/CSS/图片） |
| `public` | 允许所有代理缓存 | CDN 内容 |
| `private` | 仅浏览器可缓存 | 用户私有数据 |
| `no-cache` | 每次使用前向服务器验证 | 动态 API |
| `no-store` | 完全不缓存 | 敏感数据（订单、支付） |

```java
// Spring Boot 设置强缓存
@GetMapping("/static/image.png")
public ResponseEntity<Resource> getImage() {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
        .body(resource);
}
```

### 7.2 协商缓存（发请求，服务器判断是否返回 304）

```
第一次请求：
  请求头：（无缓存相关头）
  响应头: ETag: "v1-user-123", Last-Modified: Mon, 01 Jan 2024 12:00:00 GMT, Cache-Control: no-cache

第二次请求（缓存验证）：
  请求头: If-None-Match: "v1-user-123", If-Modified-Since: Mon, 01 Jan 2024 12:00:00 GMT
  响应头：304 Not Modified（空 body，节省带宽）
```

```java
// Spring Boot 中开启协商缓存（Spring MVC 自动支持）
@GetMapping("/api/users/{id}")
public ResponseEntity<User> getUser(@PathVariable Long id,
                                    WebRequest webRequest) {
    User user = service.findById(id);
    
    // 检查 ETag，如果未变化返回 304
    String etag = "v" + user.getVersion() + "-user-" + id;
    if (webRequest.checkNotModified(etag)) {
        return null;  // Spring 自动返回 304
    }
    
    return ResponseEntity.ok().eTag(etag).body(user);
}
```

> 🎯 **缓存策略选择指南**：
> - 静态资源（JS/CSS/图片）：`Cache-Control: public, max-age=31536000, immutable` + 版本化文件名（如 `app.a1b2c3.js`）
> - 动态 API 数据：`Cache-Control: no-cache` + ETag 协商缓存
> - 敏感数据：`Cache-Control: no-store`

---

## 8. Cookie 与 Session 机制

### 8.1 Set-Cookie 属性

```
Set-Cookie: sessionId=abc123; 
            Domain=.example.com;        ← 作用域名
            Path=/api;                  ← 作用路径
            HttpOnly;                   ← JS 不可读取（防 XSS）
            Secure;                     ← 仅 HTTPS 传输
            SameSite=Lax;               ← 防 CSRF（Strict | Lax | None）
            Max-Age=86400;              ← 过期秒数
            Expires=Wed, 21 Oct 2024... ← 过期日期（旧式，优先用 Max-Age）
```

### 8.2 Session vs Token

| 特性 | Session | JWT Token |
|------|---------|-----------|
| 存储位置 | 服务端内存/Redis | 客户端 Cookie/LocalStorage |
| 扩展性 | 需要集中式 Session 存储（Redis） | 无状态，天然支持水平扩展 |
| 撤销能力 | 可直接删除 Session | 需维护黑名单（或使用短有效期的 token + refresh_token） |
| 跨域 | 受限，Cookie 受同源策略限制 | 可放在 Authorization 头，不受同源限制 |
| 安全 | 依赖 HttpOnly Cookie | 注意 XSS 泄露风险 |

```java
// Session 方式（传统 Servlet）
@PostMapping("/api/login")
public String login(@RequestParam String username, HttpSession session) {
    session.setAttribute("userId", 123L);
    session.setMaxInactiveInterval(3600);  // 1 小时过期
    return "ok";
}

// Token 方式（Spring Security + JWT）
@PostMapping("/api/login")
public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req) {
    User user = authenticate(req.getUsername(), req.getPassword());
    String token = Jwts.builder()
        .setSubject(user.getId().toString())
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + 3600_000))  // 1h 过期
        .signWith(SignatureAlgorithm.HS256, secretKey)
        .compact();
    
    return ResponseEntity.ok(new AuthResponse(token));
}
```

> ⚠️ 后端使用 JWT 时务必注意：不要将敏感数据放入 payload（Base64 解码即可读取）；设置合理的过期时间；使用 refresh_token 机制避免频繁登录。

---

## 9. HTTP/1.1 vs HTTP/2 vs HTTP/3

| 特性 | HTTP/1.1 | HTTP/2 | HTTP/3 |
|------|----------|--------|--------|
| 传输协议 | TCP | TCP | QUIC（基于 UDP） |
| 多路复用 | 无（队头阻塞） | 有（单 TCP 多请求） | 有（无队头阻塞） |
| 头部压缩 | 无 | HPACK | QPACK |
| 服务器推送 | 无 | 有（主动推送资源） | 有 |
| 连接建立 | TCP 三次握手 | TCP + TLS 协商 | 0-1 RTT |
| 二进制传输 | 否（文本） | 是（帧） | 是（帧） |
| 浏览器支持 | 全支持 | ~98% | ~95% |

### 9.1 对后端开发的影响

```yaml
# HTTP/1.1 的性能瓶颈：
# - 一个连接同时只能发一个请求（队头阻塞）
# - 浏览器最多对同一域名开 6 个连接
# - 每次请求都有完整的头部

# HTTP/2 的改进（后端可能需要的配置）：
# - Nginx 开启 HTTP/2：listen 443 ssl http2;
# - 多路复用：不需要做雪碧图、域名分片等 hack
# - 服务器推送：API + 关联资源可以一起推送（实际很少用）

# HTTP/3 的优势：
# - 丢包不影响其他流（TCP 队头阻塞在 HTTP/3 中消失）
# - 连接迁移（手机切 WiFi 不断连）
```

> 💡 对于后端开发者，HTTP/2 和 HTTP/3 基本是透明的——底层协议升级，应用层代码无需改动。唯一注意：HTTP/2 的服务器推送（Server Push）实际上很少用，且已被 Chrome 逐步移除支持。

---

## 10. HTTPS/TLS 握手简化

### 10.1 握手过程（TLS 1.3，仅 1-RTT）

```
Client                                Server
  |                                     |
  |--- ClientHello (支持的加密套件) ---->|
  |                                     |
  |<--- ServerHello + 证书 + 公钥 ------|
  |                                     |
  |--- 密钥交换（ECDHE） + 完成 -------->|  ← 至此已可加密通信
  |                                     |
  |<--- 加密的 "Finished" --------------|
  |                                     |
  |========== 加密通信开始 ==============|
```

### 10.2 后端需要关注的 TLS 配置

```yaml
# application.yml - Spring Boot HTTPS 配置
server:
  port: 443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: myapp
```

### 10.3 HSTS（HTTP Strict Transport Security）

```java
// 强制浏览器始终使用 HTTPS 访问
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.headers(headers -> headers
            .httpStrictTransportSecurity(hsts -> hsts
                .includeSubDomains(true)
                .maxAgeInSeconds(31536000)
                .preload(true)
            )
        );
        return http.build();
    }
}
```

> ⚠️ HSTS 的 `includeSubDomains` 务必谨慎：一旦浏览器收到此头，所有子域名无法再通过 HTTP 访问。如有子域名尚未配置 HTTPS，将导致不可用。

---

## 11. 浏览器页面加载过程

### 完整链路（7 步）

```
┌──────────┐    ① DNS 解析    ┌──────────────┐
│ 用户在   │ ───────────────> │ DNS 服务器    │
│ 地址栏   │                  │ 返回 IP       │
│ 输入 URL  │ <─────────────── │ 如 142.250.80│
└──────────┘                  └──────────────┘
     │
     ▼
┌──────────┐    ② TCP 连接    ┌──────────────┐
│ 浏览器   │ ──SYN─> SYN-ACK─>│ 服务器        │
│          │ <──ACK──────────│ (Nginx/Tomcat)│
└──────────┘                  └──────────────┘
     │
     ▼
┌──────────┐    ③ TLS 握手    ┌──────────────┐
│ (若HTTPS)│ ────握手过程───> │ 服务器        │
└──────────┘                  └──────────────┘
     │
     ▼
┌──────────┐    ④ 发送 HTTP   ┌──────────────┐
│ 浏览器   │ ── GET /index ──>│ 后端服务器    │
└──────────┘                  └──────────────┘
     │
     ▼
┌──────────┐    ⑤ 服务器处理   ┌──────────────┐
│ 等待     │ <── 200 + HTML ──│ Controller →  │
│ 响应     │                  │ Service → DB  │
└──────────┘                  └──────────────┘
     │
     ▼
┌──────────┐    ⑥ 解析 HTML   ┌──────────────┐
│ 解析 HTML│ ── 发现 CSS/JS ─>│ 再次发起      │
│ 构建DOM  │                  │ HTTP 请求     │
└──────────┘                  └──────────────┘
     │
     ▼
┌──────────┐    ⑦ 渲染页面    ┌──────────────┐
│ 构建渲染 │                  │ 用户看到      │
│ 树 → 绘制│                  │ 完整页面      │
└──────────┘                  └──────────────┘
```

### DNS 解析详细步骤

```
浏览器缓存 → 操作系统缓存 → hosts 文件 → 本地 DNS 服务器 → 根 DNS → 顶级域 DNS → 权威 DNS
```

> 💡 首次 DNS 解析通常 20-120ms。后端关注点：使用 CDN 缩短 DNS 距离；配置 DNSPod 等实现智能 DNS 解析；TTL 设置合理（默认 600s）。

---

## 12. 浏览器渲染流程

> 后端开发者不需要理解具体渲染细节，但需要知道后端数据是如何变成用户看到的页面的。

```
① 解析 HTML → DOM 树（Document Object Model）
② 解析 CSS → CSSOM 树（CSS Object Model）
③ 合并 DOM + CSSOM → Render Tree（渲染树）
④ Layout（布局）：计算每个元素的位置和大小
⑤ Paint（绘制）：将像素渲染到屏幕上
⑥ Compositing（合成）：分层合并（GPU 加速）
```

### 后端导致的渲染问题

| 后端问题 | 前端表现 | 解决方案 |
|----------|---------|---------|
| API 响应慢 | 页面白屏时间过长 | 优化 SQL、加缓存、异步处理 |
| 数据量过大 | 渲染卡顿、页面空白 | 分页、增量加载、压缩传输 |
| 响应格式错误 | 页面解析异常、空白 | 统一 API 规范、严格校验 |
| 缺少关键字段 | 页面局部空白 | 完善异常处理，返回默认值 |

---

## 13. 完整链路追踪：浏览器 --> SpringBoot

以一个完整的用户登录场景为例：

### Step 1: 用户在浏览器提交登录表单

```html
<!-- 前端 HTML -->
<form id="loginForm">
  <input name="username" value="admin">
  <input name="password" value="123456">
  <button type="submit">登录</button>
</form>
<script>
  document.getElementById('loginForm').onsubmit = async (e) => {
    e.preventDefault();
    
    const res = await fetch('/api/login', {
      method: 'POST',
      headers: { 
        'Content-Type': 'application/json',
        'X-Request-Id': crypto.randomUUID()
      },
      body: JSON.stringify({
        username: 'admin',
        password: '123456'
      })
    });
    
    if (res.ok) {
      const data = await res.json();
      localStorage.setItem('token', data.token);
      window.location.href = '/dashboard';
    }
  };
</script>
```

### Step 2: Nginx 反向代理

```nginx
# nginx.conf
server {
    listen 443 ssl http2;
    server_name api.example.com;
    
    location /api/ {
        proxy_pass http://backend:8080;
        proxy_set_header X-Request-Id $request_id;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### Step 3: SpringBoot 后端处理

```java
@RestController
@RequestMapping("/api")
public class AuthController {

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody @Valid LoginRequest req,
            @RequestHeader("X-Request-Id") String requestId) {
        
        log.info("[{}] Login attempt: {}", requestId, req.getUsername());
        
        // 1. 认证
        User user = authService.authenticate(req.getUsername(), req.getPassword());
        
        // 2. 生成 JWT
        String token = jwtService.generateToken(user);
        
        // 3. 记录登录日志
        loginLogService.save(user.getId(), requestId);
        
        log.info("[{}] Login success: {}", requestId, user.getId());
        
        return ResponseEntity.ok()
            .header("X-Request-Id", requestId)
            .body(new LoginResponse(token, user.getUsername()));
    }
}
```

### Step 4: 完整请求-响应周期

```
时间线：

0ms      用户点击登录按钮
1ms      浏览器处理表单，构建 JSON body
2ms      浏览器检查是否有可用 TCP 连接
3ms      DNS 解析（如缓存有效则为 0ms）
10ms     TCP 连接建立（如使用 Keep-Alive 则为 0ms）
15ms     TLS 握手（如已建立则为 0ms）
20ms     浏览器发送 HTTP 请求
25ms     Nginx 接收请求 → 转发给 SpringBoot
28ms     SpringBoot: 反序列化 JSON → 参数校验
30ms     AuthService.authenticate() → 查数据库
35ms     JwtService.generateToken() → 签名
38ms     SpringBoot: 序列化响应 → 返回
42ms     Nginx 接收响应 → 返回给浏览器
45ms     浏览器收到 200 响应
46ms     JavaScript 解析 JSON
48ms     localStorage.setItem('token')
50ms     window.location.href = '/dashboard'
55ms     浏览器开始加载 /dashboard 页面...
```

> 🎯 **后端开发者要做好的环节**：
> 1. 服务端处理时间尽量控制在 50ms 以内（超过 100ms 用户能感知到延迟）
> 2. 添加 X-Request-Id 用于全链路追踪
> 3. 响应体不要过大（超过 1MB 考虑分页或压缩）
> 4. 合理使用 HTTP 缓存减少重复请求
> 5. 避免 N + 1 查询导致响应时间随数据量线性增长
