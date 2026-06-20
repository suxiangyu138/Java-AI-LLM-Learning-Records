# 🌐 Web 基础与 RESTful API 设计

> HTTP 协议、RESTful 规范、跨域、认证、文件传输，构建专业 API 的全部知识。

---

## 目录

1. [HTTP 协议详解](#1-http-协议详解)
2. [RESTful API 设计规范](#2-restful-api-设计规范)
3. [用户认证机制对比](#3-用户认证机制对比)
4. [CORS 跨域问题彻底解决](#4-cors-跨域问题彻底解决)
5. [HTTPS 与网络安全](#5-https-与网络安全)
6. [WebSocket 实时通信](#6-websocket-实时通信)
7. [文件上传与下载](#7-文件上传与下载)
8. [API 版本管理](#8-api-版本管理)
9. [API 文档与接口管理](#9-api-文档与接口管理)
10. [浏览器渲染与前后端交互](#10-浏览器渲染与前后端交互)
11. [Servlet 与 Tomcat 原理](#11-servlet-与-tomcat-原理)
12. [常见面试题](#12-常见面试题)

---

## 1. HTTP 协议详解

### 1.1 HTTP 请求/响应结构

```
HTTP 请求格式：
┌──────────────────────────────────┐
│ 请求行                           │
│ GET /api/users?page=1&size=20 HTTP/1.1
├──────────────────────────────────┤
│ 请求头（Headers）                 │
│ Host: api.example.com            │
│ User-Agent: Mozilla/5.0 ...      │
│ Accept: application/json         │
│ Authorization: Bearer xxx        │
│ Content-Type: application/json   │
├──────────────────────────────────┤
│ 空行（\\r\\n）                    │
├──────────────────────────────────┤
│ 请求体（Body，GET 请求为空）      │
│ {"name": "张三", "age": 25}      │
└──────────────────────────────────┘

HTTP 响应格式：
┌──────────────────────────────────┐
│ 状态行                           │
│ HTTP/1.1 200 OK                  │
├──────────────────────────────────┤
│ 响应头                           │
│ Content-Type: application/json   │
│ Content-Length: 123              │
│ Cache-Control: no-cache          │
├──────────────────────────────────┤
│ 空行                             │
├──────────────────────────────────┤
│ 响应体                           │
│ {"code":200,"data":{...}}        │
└──────────────────────────────────┘
```

### 1.2 HTTP 方法完整对比

| 方法 | 语义 | 幂等 | 安全 | 请求体 | 缓存 | 典型场景 |
|------|------|------|------|--------|------|---------|
| **GET** | 获取资源 | ✅ | ✅ | ❌ | ✅ | 查询 |
| **POST** | 创建资源 | ❌ | ❌ | ✅ | ❌ | 新增、登录 |
| **PUT** | 全量替换 | ✅ | ❌ | ✅ | ❌ | 覆盖更新 |
| **PATCH** | 部分更新 | ❌ | ❌ | ✅ | ❌ | 只改部分字段 |
| **DELETE** | 删除资源 | ✅ | ❌ | ❌ | ❌ | 删除 |
| **HEAD** | 获取响应头 | ✅ | ✅ | ❌ | ✅ | 检查资源是否存在 |
| **OPTIONS** | 查询支持的方法 | ✅ | ✅ | ❌ | ❌ | CORS 预检请求 |
| **TRACE** | 追踪请求路径 | ✅ | ✅ | ❌ | ❌ | 调试（通常禁用） |

```
幂等性：同样请求执行多次，结果相同
- GET：查 10 次结果一样 → 幂等
- POST：创建 10 次得到 10 条 → 不幂等
- PUT：全量替换 10 次还是那样 → 幂等
- DELETE：删第 1 次成功，第 2 次 404 → 幂等（服务端状态相同）

安全性：请求不改变服务端数据
- GET：只读 → 安全
- POST/PUT/DELETE：修改数据 → 不安全
```

### 1.3 HTTP 状态码

```
1xx 信息性
  100 Continue          请求没问题，继续发送
  101 Switching Protocols WebSocket 升级

2xx 成功
  200 OK               请求成功（最常见）
  201 Created          资源创建成功（POST 成功）
  204 No Content       请求成功，但没有返回内容（DELETE 成功后常用）

3xx 重定向
  301 Moved Permanently  永久重定向（SEO 权重重定向）
  302 Found              临时重定向（会改为 GET，不推荐）
  307 Temporary Redirect  临时重定向（保持原方法）
  308 Permanent Redirect  永久重定向（保持原方法）
  304 Not Modified        资源未修改（缓存相关，配合 If-Modified-Since）

4xx 客户端错误
  400 Bad Request       请求参数有误
  401 Unauthorized      未认证（需要登录）
  403 Forbidden         无权限（已登录但权限不够）
  404 Not Found         资源不存在
  405 Method Not Allowed 请求方法不支持（如给 GET 接口发 POST）
  406 Not Acceptable    不接受此媒体类型
  409 Conflict          资源冲突（如重复创建）
  413 Payload Too Large  请求体太大
  415 Unsupported Media Type 不支持的 Content-Type
  422 Unprocessable Entity 参数格式正确但语义有误
  429 Too Many Requests 请求频率超限

5xx 服务器错误
  500 Internal Server Error 服务器内部错误
  502 Bad Gateway        网关或代理收到了无效响应
  503 Service Unavailable 服务暂时不可用（过载/维护）
  504 Gateway Timeout    网关超时
```

### 1.4 HTTP 版本演进

```
HTTP/1.0 (1996)：
  - 每个请求单独 TCP 连接
  - 无 Host 头（一个 IP 一个网站）

HTTP/1.1 (1997) — 至今最常用：
  - 持久连接（Connection: keep-alive，复用 TCP）
  - 管道化（Pipelining，不等待响应就发下一个请求）
  - Host 头（虚拟主机，一个 IP 多网站）
  - 分块传输编码（Chunked Transfer Encoding）
  - 问题：队头阻塞（Head-of-Line Blocking）← 一个响应慢会影响后面的

HTTP/2 (2015)：
  - 二进制分帧（不再用文本协议）
  - 多路复用（同一 TCP 连接上并行传输多个请求/响应）
  - 头部压缩（HPACK 算法）
  - 服务器推送（Server Push）
  - 问题：TCP 层面的队头阻塞仍然存在

HTTP/3 (2022)：
  - 基于 QUIC（UDP 协议，0-RTT 连接建立）
  - 真正解决队头阻塞（不同流之间完全独立）
  - 连接迁移（切换网络不断开）
  - 目前 CDN 和主流浏览器已广泛支持
```

### 1.5 常用请求头

```http
# 认证
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Authorization: Basic dXNlcjpwYXNz

# 内容协商
Content-Type: application/json
Content-Type: application/x-www-form-urlencoded
Content-Type: multipart/form-data; boundary=----
Accept: application/json
Accept-Encoding: gzip, deflate, br

# 缓存控制
Cache-Control: no-cache
Cache-Control: max-age=3600
If-Modified-Since: Wed, 21 Oct 2024 07:28:00 GMT
ETag: "33a64df551425fcc55e4d42a148795d9f25f89d4"

# 条件请求
If-None-Match: "xxx"
If-Match: "xxx"

# 跨域
Origin: https://example.com

# 安全
X-XSS-Protection: 1; mode=block
X-Frame-Options: DENY
X-Content-Type-Options: nosniff

# 上下文
Referer: https://example.com/page
User-Agent: Mozilla/5.0 (...)
X-Forwarded-For: 10.0.0.1
X-Real-IP: 10.0.0.1
```

---

## 2. RESTful API 设计规范

### 2.1 设计原则

```
核心原则：
1. 以资源为中心（URL 用名词，不用动词）
   ✅ GET /api/users
   ❌ GET /api/getUsers

2. HTTP 方法表达操作意图
   GET → 查，POST → 增，PUT/PATCH → 改，DELETE → 删

3. 无状态（每个请求包含所有信息，不依赖服务器 Session）

4. 资源层级化（用路径表达资源关系）
   GET /api/users/123/orders    # 用户 123 的所有订单
   GET /api/users/123/orders/456  # 用户 123 的订单 456

5. 统一响应格式

6. HATEOAS（超媒体驱动，提供可用的下一步操作链接）
   实际项目中较少完全实现，但理念重要
```

### 2.2 URL 设计规范

```
资源命名规则：
  - 名词复数：/api/users, /api/orders（不是 /api/user, /api/order）
  - 用小写字母 + 连字符：/api/user-profiles（不是 /api/userProfiles）
  - 避免深层嵌套（最多 3 层）
    深层：/api/users/1/orders/2/items/3/comments/4
    扁平：/api/comments?userId=1&orderId=2&itemId=3

非 CRUD 操作：
  - 用动词后缀（将动作作为资源的子资源）
    POST /api/orders/123/pay         # 支付
    POST /api/orders/123/cancel      # 取消
    POST /api/users/123/reset-password # 重置密码

查询参数：
  GET /api/users?page=1&size=20           # 分页
  GET /api/users?sort=createTime,desc     # 排序
  GET /api/users?status=active&deptId=5   # 过滤
  GET /api/users?q=张三                   # 搜索
  GET /api/users?fields=id,name,email     # 字段选择（稀疏字段集）
```

### 2.3 统一响应格式设计

```json
// 成功响应
{
    "code": 200,
    "message": "success",
    "data": {
        "id": 1,
        "userName": "张三",
        "createTime": "2024-06-12T10:30:00"
    },
    "timestamp": 1718167800000
}

// 分页列表
{
    "code": 200,
    "message": "success",
    "data": {
        "records": [...],
        "total": 1000,
        "page": 1,
        "pageSize": 20,
        "totalPages": 50
    },
    "timestamp": 1718167800000
}

// 错误响应
{
    "code": 40001,
    "message": "用户名已存在",
    "data": null,
    "timestamp": 1718167800000
}
```

### 2.4 企业级错误码设计

```
错误码设计建议：5 位数字
  第 1-2 位：系统/模块编码
  第 3 位：错误级别（1=系统错误, 2=业务错误, 3=参数错误）
  第 4-5 位：具体错误

示例：
  10001  → 01 系统 + 0 系统错误 + 01 = 用户系统未捕获异常
  10101  → 01 系统 + 1 系统错误 + 01 = 用户系统数据库异常
  10201  → 01 系统 + 2 业务错误 + 01 = 用户不存在
  10301  → 01 系统 + 3 参数错误 + 01 = 用户名格式错误
  20201  → 02 系统 + 2 业务错误 + 01 = 订单不存在
```

### 2.5 常见设计误区

```
❌ 把所有返回都设为 200（包括错误场景）
   GET /api/users/999 → 200 { "code": 404, "message": "用户不存在" }
   问题：CDN/代理 无法根据 HTTP 状态码做处理
✅ GET /api/users/999 → 404 { "code": 40401, "message": "用户不存在" }

❌ URL 中暴露数据库结构
   GET /api/table/sys_user?column=user_name&value=张三
✅ GET /api/users?name=张三

❌ 用动词命名
   POST /api/createUser
✅ POST /api/users

❌ 嵌套过深
   GET /api/users/1/orders/2/items/3/attachments/4
✅ GET /api/attachments/4?userId=1&orderId=2&itemId=3
```

---

## 3. 用户认证机制对比

### 3.1 Cookie-Session 模式

```
┌──────────────────────────────────────────┐
│ 1. 用户登录提交用户名密码                  │
│ 2. 服务端验证 → 创建 Session → 存到 Redis  │
│ 3. 通过 Set-Cookie 返回 SessionId         │
│ 4. 浏览器自动在后续请求中带 Cookie         │
│ 5. 服务端取 Cookie 中的 SessionId 查 Redis │
│ 6. 登出时删除 Redis 中的 Session           │
└──────────────────────────────────────────┘

优点：
  - Session 存在服务端，可随时失效
  - Cookie HttpOnly 防 XSS

缺点：
  - 服务端需要存储（有状态）
  - 分布式需要 Session 同步或 Redis
  - 跨域不友好（Cookie 受域名限制）
  - 移动端/App 不友好

适用场景：传统 MVC 服务端渲染应用
```

### 3.2 JWT Token 模式

```
┌──────────────────────────────────────────┐
│ 1. 用户登录 → 服务端验证 → 生成 JWT 返回   │
│ 2. 客户端存储 Token（localStorage/内存）   │
│ 3. 请求时手动设置 Header：                 │
│    Authorization: Bearer <token>          │
│ 4. 服务端验证签名 → 提取用户信息            │
│ 5. 刷新：Access Token 短(15min)            │
│          Refresh Token 长(7d)              │
└──────────────────────────────────────────┘

JWT 结构：
  Header.Payload.Signature

  Header:  Base64Url({ "alg": "RS256", "typ": "JWT" })
  Payload: Base64Url({
    "iss": "api.example.com",    // 签发者
    "sub": "12345",              // 用户 ID（主题）
    "name": "张三",
    "iat": 1718167800,           // 签发时间
    "exp": 1718254200,           // 过期时间
    "aud": "web-app",            // 接收方
    "roles": ["admin", "user"]   // 自定义数据（不要放敏感信息！）
  })
  Signature: RSASHA256(Header.Payload, PrivateKey)

优点：
  - 无状态（服务端不存，验证只需公钥解密）
  - 跨域友好（不依赖 Cookie）
  - 适用于 App/移动端/SPA

缺点：
  - 不能主动失效（黑名单 + 短 TTL 缓解）
  - Payload 只是 Base64 编码（不是加密！不要放敏感数据）
  - Token 体积比 SessionId 大
```

### 3.3 OAuth 2.0 + OIDC

```
OAuth 2.0 是授权协议，OIDC (OpenID Connect) 是基于 OAuth 2.0 的认证协议

四种授权模式：

1. 授权码模式（Authorization Code）+ PKCE
   适用：有后端的 Web 应用、SPA（配合 PKCE）、移动 App
   流程：
     → 重定向到授权服务器
     → 用户登录并授权
     → 获取授权码
     → 后端用授权码 + code_verifier 换 Token
     → 获得 Access Token + (可能) Refresh Token

2. 客户端凭证模式（Client Credentials）
   适用：服务间调用（微服务 A → 微服务 B）
   流程：直接用 client_id + client_secret 换 Token

3. 设备授权模式（Device Code）
   适用：输入受限设备（电视、打印机）

4. Token 交换（Token Exchange）
   适用：用自己的 Token 换下游服务的 Token

OAuth 2.1（简化 + 安全加强）：
  - 废弃隐式模式（Implicit）
  - 废弃密码模式（ROPC）
  - PKCE 对所有客户端都是强制要求
  - 不再支持 Bearer Token 在 URL 查询参数中
```

---

## 4. CORS 跨域问题彻底解决

### 4.1 为什么有跨域限制？

```
浏览器的同源策略（Same-Origin Policy）：
  源（Origin）= 协议 + 域名 + 端口，三者完全相同才算同源

  https://example.com:443
  https://example.com:443/api  → 同源（路径不同不影响）
  http://example.com:443       → 不同源（协议不同）
  https://api.example.com:443  → 不同源（子域名不同）

限制内容：
  ✅ 可以跨域引用的：Script、CSS、IMG、Video、iframe 引入
  ❌ 限制的：AJAX/Fetch 请求、Cookie/LocalStorage 访问

CORS（Cross-Origin Resource Sharing）：
  一套机制让服务器告诉浏览器"我允许哪些源跨域访问我"
```

### 4.2 CORS 响应头详解

```http
# 响应头
Access-Control-Allow-Origin: https://example.com
Access-Control-Allow-Origin: *          # 允许所有源（不能与 credentials 共存）

Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With
Access-Control-Max-Age: 3600            # 预检请求缓存时间（秒）

Access-Control-Allow-Credentials: true  # 允许携带 Cookie
Access-Control-Expose-Headers: X-Total-Count, X-Request-Id  # 暴露自定义响应头
```

### 4.3 Spring Boot 解决跨域

```java
// 方案 1：注解（单个 Controller）
@RestController
@CrossOrigin(origins = "https://example.com", maxAge = 3600)
public class UserController { }

// 方案 2：全局配置（推荐）
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")              // 匹配的路径
            .allowedOrigins("https://example.com")  // 允许的域名
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}

// 方案 3：CorsFilter
@Bean
public CorsFilter corsFilter() {
    CorsConfiguration config = new CorsConfiguration();
    config.addAllowedOriginPattern("*");
    config.addAllowedMethod("*");
    config.addAllowedHeader("*");
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return new CorsFilter(source);
}

// ⚠️ 生产环境不要 allowedOrigins("*") + allowCredentials(true)
//    这两个是互斥的！带凭证时必须指定具体域名
```

### 4.4 预检请求 (Preflight)

```
何时触发预检请求？
  不是"所有跨域请求"都发预检，满足以下条件才发：

  1. 使用了非简单的方法（PUT、DELETE、PATCH 等）
  2. 使用了非简单的请求头（Authorization、Content-Type: application/json 等）
  3. 读取了非简单响应头

"简单请求"定义（不触发预检）：
  - 方法：GET、HEAD、POST
  - 请求头：Accept、Accept-Language、Content-Language、Content-Type
    （且 Content-Type 只能是：text/plain、multipart/form-data、
      application/x-www-form-urlencoded）
  - 无 ReadableStream

流程：
  浏览器自动先发 OPTIONS 请求
  → 服务器返回 Access-Control-Allow-* 头
  → 浏览器检查通过 → 再发真实的请求
  → 不通过 → 拦截，真实请求不发

优化：Access-Control-Max-Age: 86400（24 小时内不发预检）
```

---

## 5. HTTPS 与网络安全

### 5.1 HTTP vs HTTPS

```
HTTPS = HTTP + TLS（传输层安全）

TLS 握手过程（简化）：
  1. Client Hello：客户端发送支持的加密套件 + 随机数 1
  2. Server Hello：服务端选一个加密套件 + 随机数 2 + 证书（含公钥）
  3. 客户端验证证书（是否受信任 CA 签发、域名是否匹配、是否过期）
  4. 客户端生成 Premaster Secret → 用公钥加密 → 发给服务端
  5. 服务端用私钥解密得到 Premaster Secret
  6. 双方用随机数 1+2+Premaster Secret 生成相同的对称密钥
  7. 后续通信使用对称密钥加密

TLS 1.2：需要 2 个 RTT
TLS 1.3：只需 1 个 RTT（简化握手），支持 0-RTT（恢复连接）
```

### 5.2 Web 安全相关 Headers

```http
# 严格传输安全（只能通过 HTTPS 访问）
Strict-Transport-Security: max-age=31536000; includeSubDomains

# XSS 防护
X-XSS-Protection: 1; mode=block

# 禁止 MIME 类型嗅探
X-Content-Type-Options: nosniff

# 禁止被 iframe 嵌入（防点击劫持）
X-Frame-Options: DENY  # 完全禁止
X-Frame-Options: SAMEORIGIN  # 允许同源嵌入

# 内容安全策略（CSP，防 XSS）
Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline'

# 引用来源策略
Referrer-Policy: strict-origin-when-cross-origin

# 权限策略
Permissions-Policy: camera=(), microphone=()
```

---

## 6. WebSocket 实时通信

### 6.1 四种实时通信方案对比

```
短轮询（Short Polling）：
  客户端定时发请求（setInterval）
  缺点：延迟高、无效请求多、浪费带宽

长轮询（Long Polling）：
  客户端发请求 → 服务端 hold 住 → 有数据/超时才返回 → 再发请求
  缺点：服务端资源消耗大、连接管理复杂

Server-Sent Events（SSE）：
  服务端 → 客户端单向推送（HTTP 流）
  优点：基于 HTTP、自动重连、简单
  缺点：只是单向（服务器 → 客户端）、连接数受限

WebSocket：
  全双工通信（双向实时）
  通过 HTTP Upgrade 握手建立 TCP 连接
  优点：低延迟、双向、二进制/文本都支持
  缺点：负载均衡复杂、需要额外心跳保活
```

### 6.2 Spring WebSocket

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .withSockJS();  // 降级方案
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");  // 订阅前缀
        registry.setApplicationDestinationPrefixes("/app");  // 发送前缀
    }
}

@Controller
public class ChatController {
    @MessageMapping("/chat.send")  // /app/chat.send
    @SendTo("/topic/public")
    public ChatMessage sendMessage(ChatMessage message) {
        return message;
    }

    @MessageMapping("/chat.private")
    public void sendPrivateMessage(ChatMessage message) {
        // 点对点发送
        messagingTemplate.convertAndSendToUser(
            message.getTo(), "/queue/private", message);
    }
}
```

---

## 7. 文件上传与下载

### 7.1 文件上传

```java
@RestController
public class FileController {
    // 小文件上传
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error(400, "文件为空");
        }
        // 安全检查
        String originalFilename = file.getOriginalFilename();
        checkFileType(originalFilename);  // 白名单校验后缀
        checkFileSize(file.getSize());    // 大小限制

        // 存储
        String storedName = UUID.randomUUID() + getExtension(originalFilename);
        Path path = Paths.get(uploadPath, storedName);
        Files.copy(file.getInputStream(), path);
        return Result.success(storedName);
    }

    // 大文件分片上传
    @PostMapping("/upload/chunk")
    public Result<?> uploadChunk(
            @RequestParam("file") MultipartFile chunk,
            @RequestParam("identifier") String fileMd5,   // 整个文件的 MD5
            @RequestParam("chunkNumber") int chunkNumber,
            @RequestParam("totalChunks") int totalChunks) {
        // 存到临时目录：{uploadPath}/chunks/{fileMd5}/{chunkNumber}
        // 全部上传完成后合并
    }
}

// MultipartFile 相关配置
spring.servlet.multipart.max-file-size=10MB       # 单个文件大小
spring.servlet.multipart.max-request-size=100MB   # 总请求大小
```

### 7.2 文件下载

```java
@GetMapping("/download/{fileId}")
public ResponseEntity<Resource> download(@PathVariable String fileId) {
    FileInfo fileInfo = fileService.getFileInfo(fileId);
    Path path = Paths.get(uploadPath, fileInfo.getStoredName());
    Resource resource = new UrlResource(path.toUri());

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .contentLength(fileInfo.getSize())
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + URLEncoder.encode(fileInfo.getOriginalName(), "UTF-8") + "\"")
        .body(resource);
}
```

---

## 8. API 版本管理

```
方案 1：URL 版本（最直观，推荐）
  GET /api/v1/users
  GET /api/v2/users

方案 2：请求头版本
  GET /api/users
  Accept: application/vnd.company.v2+json
  // 或自定义 header：API-Version: 2

方案 3：请求参数
  GET /api/users?version=2

商业 API 推荐 URL 版本（如 Stripe、GitHub）：
  - 直观、易调试
  - 可对跨版本请求做统计

版本兼容原则（v1 → v2）：
  - 添加字段 → 兼容
  - 删除字段 → 不兼容，需要 v2
  - 改变字段语义 → 不兼容
  - 改变默认行为 → 不兼容
```

---

## 9. API 文档与接口管理

### 9.1 Knife4j (Swagger 增强版)

```java
// Spring Boot 3 + SpringDoc OpenAPI
// Knife4j 是在 SpringDoc 基础上的美化
@Configuration
public class Knife4jConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("用户服务 API")
                .description("用户服务接口文档")
                .version("v1.0")
                .contact(new Contact().name("开发团队").email("team@example.com")))
            .addSecurityItem(new SecurityRequirement().addList("JWT"))
            .components(new Components()
                .addSecuritySchemes("JWT", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}

// Controller 注解
@Tag(name = "用户管理", description = "用户的增删改查")
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Operation(summary = "获取用户详情", description = "根据 ID 获取用户详情")
    @Parameter(name = "id", description = "用户 ID", required = true)
    @GetMapping("/{id}")
    public Result<User> getUser(@PathVariable Long id) { ... }
}
```

### 9.2 接口管理平台

```
自建方案：
  - Swagger/Knife4j 生成文档 + 手动维护
  - 问题：多人协作时文档与代码不一致

专业工具：
  - YApi（开源，可视化管理）
  - Apifox / ApiPost（国产，Postman + Swagger + Mock）
  - SwaggerHub（官方 SaaS）
  - Postman Collection 导出

最佳实践：
  1. 代码中写注解 → 自动生成文档
  2. 接口管理平台导入 → 多团队共享
  3. CI 中检查文档与代码是否同步
  4. 文档即契约（Consumer-Driven Contracts）
```

---

## 10. 浏览器渲染与前后端交互

### 10.1 三种渲染模式

```
服务端渲染（SSR / MVC）：
  浏览器 → Controller → Thymeleaf/JSP 渲染 HTML → 返回完整页面
  适用：传统 Web 应用、需要 SEO 的内容页面

客户端渲染（CSR / SPA）：
  浏览器 → 加载 JS → 执行 JS → AJAX 请求 API → 渲染 DOM
  适用：后台管理系统、交互复杂的应用

服务端生成 + 客户端水合（SSG/SSR + Hydration）：
  Next.js / Nuxt.js：首屏服务端渲染，后续交互客户端接管
  适用：需要 SEO + 复杂交互（电商、内容平台）
```

### 10.2 前后端分离实践

```
接口约定（核心！）：
  1. 明确接口路径、参数、响应格式（文档先行）
  2. Mock 数据（开发阶段用 Mock，不用等后端）：
     - Mock.js / MSW
     - Apifox 生成 Mock
  3. 前端 BFF 层（Backend for Frontend）：
     - 前端自己维护一个中间层，聚合后端接口
     - 减少前端请求次数

前端请求封装（Axios 示例）：
  axios.create({
      baseURL: '/api',
      timeout: 15000,
      headers: { 'Content-Type': 'application/json' }
  })
  // 请求拦截器：自动加 Token
  // 响应拦截器：统一错误处理、Token 过期刷新
```

---

## 11. Servlet 与 Tomcat 原理

### 11.1 Servlet 容器架构

```
Tomcat 组件层次：
Server（服务器实例）
  └── Service（服务）
       ├── Connector（连接器，处理网络连接）
       │   └── ProtocolHandler（HTTP/AJP 协议解析）
       └── Engine（Servlet 引擎）
            └── Host（虚拟主机，如 www.example.com）
                 └── Context（Web 应用，如 /app）
                      └── Wrapper（单个 Servlet）

请求处理流程：
  1. Connector 接收 TCP 连接 → 解析 HTTP 请求
  2. 封装为 HttpServletRequest / HttpServletResponse
  3. 交给 Engine → Host → Context → Wrapper
  4. Filter Chain → Servlet.service()
  5. 返回响应

Tomcat 线程模型：
  - BIO（阻塞 IO）：一个连接一个线程（过时）
  - NIO（非阻塞 IO）：Poller 线程轮询已就绪连接（默认）
  - APR（本地库）：C 语言实现的 IO，最高性能（需额外安装）
```

### 11.2 Spring Boot 中的嵌入式 Tomcat

```yaml
# 嵌入式 Tomcat 调优
server:
  tomcat:
    max-connections: 10000          # 最大连接数
    accept-count: 100               # 连接队列长度（满了后拒绝）
    threads:
      max: 200                      # 最大工作线程数
      min-spare: 10                 # 最小空闲线程数
    connection-timeout: 60000       # 连接超时 ms
    keep-alive-timeout: 10000       # Keep-Alive 超时 ms
    max-keep-alive-requests: 100    # Keep-Alive 最大请求数
```

---

## 12. 常见面试题

### Q1: GET 和 POST 的区别？

```
1. 语义不同
   GET 获取资源 / POST 创建资源

2. 参数位置
   GET 参数在 URL Query String / POST 在请求体中

3. 长度限制
   GET URL 长度受浏览器/服务器限制（约 2K~8K）/ POST 理论上无限制

4. 安全性
   GET 参数暴露在 URL（不适合传密码）/ POST 在 Body 中（但都不加密，HTTPS 才行）

5. 幂等性
   GET 幂等 / POST 不幂等

6. 缓存
   GET 请求可缓存 / POST 默认不缓存

7. 浏览器行为
   GET 后退/刷新无害 / POST 后退时浏览器会提示重新提交数据
```

### Q2: 什么是 CSRF 攻击？如何防范？

```
CSRF（Cross-Site Request Forgery，跨站请求伪造）：
  用户登录 A 网站 → A 网站种了 Cookie
  → 用户访问恶意网站 B
  → B 网站的 JS/表单自动向 A 网站发请求（自动带上 Cookie）
  → A 网站以为是用户自己的操作

防范：
  - SameSite Cookie：SameSite=Strict/Lax（现代浏览器默认）
  - CSRF Token：表单中嵌入随机 Token
  - Referer/Origin Header 校验：检查请求来源
  - 敏感操作双重确认
  - 关键操作验证码

注意：前后端分离 + JWT 一定程度上避免了 CSRF（因为 Token 不通过 Cookie 自动携带）
但如果 JWT 存在 Cookie 中，仍然存在 CSRF 风险
```

### Q3: 301 和 302 重定向的区别？

```
301 Moved Permanently（永久重定向）：
  - 搜索引擎会把权重/索引转移到新 URL
  - 浏览器会缓存重定向（下次直接跳转，不发请求）

302 Found（临时重定向）：
  - 搜索引擎不转移权重
  - 浏览器不缓存
  - 问题：浏览器将 POST 请求改为 GET（不符合 RFC 但这是历史原因）
  - 替代：307 Temporary Redirect（保持原 HTTP 方法）
```

> **上一篇：** [02-构建工具Maven与Gradle详解](./02-构建工具Maven与Gradle详解.md)
>
> **下一篇：** [04-Spring全家桶深度详解](./04-Spring全家桶深度详解.md)
