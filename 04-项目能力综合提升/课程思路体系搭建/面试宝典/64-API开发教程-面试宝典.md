# API开发教程 面试宝典
> 从零掌握 API 核心概念，涵盖 HTTP 协议、RESTful 设计、认证授权、安全防护等面试高频考点，基于 FastAPI / Spring Boot 双栈实战

## 目录
1. [一、基础概念速答](#一基础概念速答15-20题)
2. [二、深度原理剖析](#二深度原理剖析10-15题)
3. [三、实战场景题](#三实战场景题8-12题)
4. [四、手写代码题](#四手写代码题5-8题)
5. [五、系统设计题](#五系统设计题3-5题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践表格)
7. [七、面试回答模板](#七面试回答模板-top-5)
8. [八、快速查漏补缺 Checklist](#八快速查漏补缺-checklist)

---

## 一、基础概念速答（15-20题）

### 1.1 什么是 API？API 的全称是什么？
API 全称 **Application Programming Interface**（应用程序编程接口），是一组定义好的协议、工具和约定，允许不同软件系统之间相互通信。API 定义了请求的格式、数据传输方式以及响应的结构，是前后端分离、微服务架构和第三方集成的核心基础。

### 1.2 什么是 RESTful API？
RESTful API 是遵循 **REST**（Representational State Transfer，表述性状态转移）架构风格的 API。核心原则包括：
- **资源导向**：每个 URL 代表一个资源（`/users`, `/orders`）
- **无状态**：每个请求包含所有必要信息，服务器不保存客户端状态
- **统一接口**：使用标准 HTTP 方法（GET、POST、PUT、DELETE、PATCH）
- **表现形式**：资源可以有多种表现形式（JSON、XML）

> 💡 RESTful 强调"资源"而非"动作"——URL 中不应出现动词（如 `/getUser`），而应使用 HTTP 方法表达动作。

### 1.3 HTTP 请求格式包含哪些部分？
| 部分 | 说明 | 示例 |
|------|------|------|
| **请求行** | HTTP 方法 + 路径 + 协议版本 | `GET /api/users HTTP/1.1` |
| **请求头** | 键值对，传递元数据 | `Content-Type: application/json` |
| **空行** | 分隔请求头与请求体 | `\r\n` |
| **请求体** | 可选，POST/PUT 时携带数据 | `{"name":"Alice"}` |

### 1.4 HTTP 响应格式包含哪些部分？
| 部分 | 说明 | 示例 |
|------|------|------|
| **状态行** | 协议版本 + 状态码 + 状态文本 | `HTTP/1.1 200 OK` |
| **响应头** | 键值对，传递元数据 | `Content-Type: application/json` |
| **空行** | 分隔响应头与响应体 | `\r\n` |
| **响应体** | 返回的数据内容 | `{"id":1,"name":"Alice"}` |

### 1.5 HTTP 常见状态码有哪些？
| 状态码 | 含义 | 说明 |
|--------|------|------|
| **200** | OK | 请求成功 |
| **201** | Created | 资源创建成功（POST） |
| **204** | No Content | 请求成功，无返回体（DELETE） |
| **301** | Moved Permanently | 永久重定向 |
| **302** | Found | 临时重定向 |
| **400** | Bad Request | 客户端请求错误（参数校验失败） |
| **401** | Unauthorized | 未认证（未提供或无效的凭证） |
| **403** | Forbidden | 已认证但无权限 |
| **404** | Not Found | 资源不存在 |
| **405** | Method Not Allowed | HTTP 方法不允许 |
| **409** | Conflict | 资源冲突（如重复创建） |
| **422** | Unprocessable Entity | 语义错误（如验证失败） |
| **429** | Too Many Requests | 请求频率超限 |
| **500** | Internal Server Error | 服务器内部错误 |
| **502** | Bad Gateway | 网关错误 |
| **503** | Service Unavailable | 服务暂时不可用 |

### 1.6 HTML 响应与 JSON 响应的区别？
| 对比维度 | HTML 响应 | JSON 响应 |
|----------|-----------|-----------|
| **本质** | 展示型数据（含结构和样式） | 结构化数据（纯数据） |
| **解析难度** | 需 DOM 解析，开销大 | 可直接序列化/反序列化 |
| **跨平台性** | 仅适用于浏览器 | 任何语言/平台均可解析 |
| **数据体积** | 较大（含标签和样式） | 较小（仅数据 + 少量结构） |
| **前后端分离** | 不支持（强耦合） | 天然支持 |
| **API 场景** | 传统服务端渲染 | 现代 RESTful / 微服务 |

> 🎯 **面试重点**：JSON 响应已成为现代 API 的事实标准，HTML 响应仅用于 SSR（服务端渲染）场景。

### 1.7 RESTful API 的 URL 命名规范有哪些？
| 规范 | 正确示例 | 错误示例 |
|------|----------|----------|
| **使用名词复数** | `/api/users` | `/api/getUser` |
| **用 HTTP 方法表示动作** | `DELETE /api/users/1` | `/api/deleteUser?id=1` |
| **层级关系用斜杠** | `/api/users/1/orders` | `/api/getUserOrders?userId=1` |
| **查询参数用于过滤** | `/api/users?role=admin` | `/api/getAdminUsers` |
| **下划线 vs 连字符** | `/api/user-profiles` | `/api/user_profiles` |

### 1.8 GET 和 POST 的区别？
| 维度 | GET | POST |
|------|-----|------|
| **语义** | 获取资源 | 创建资源 |
| **参数位置** | URL 查询参数 | 请求体 |
| **幂等性** | 幂等 ✅ | 非幂等 ❌ |
| **缓存** | 可被缓存 | 不可缓存 |
| **安全性** | 参数暴露在 URL | 参数在请求体中 |
| **长度限制** | URL 有长度限制（约 2KB） | 无限制 |
| **书签/分享** | 支持 | 不支持 |

### 1.9 PUT 和 PATCH 的区别？
| 维度 | PUT | PATCH |
|------|-----|-------|
| **语义** | 全量替换 | 部分更新 |
| **幂等性** | 幂等 ✅ | 可能非幂等 ❌ |
| **请求体** | 完整资源对象 | 仅包含要修改的字段 |
| **使用场景** | 更新整个资源 | 修改某个/某些字段 |

### 1.10 什么是幂等性（Idempotency）？
幂等性指**多次执行同一操作的结果与执行一次相同**。在 HTTP 中：
- **幂等方法**：GET、PUT、DELETE、HEAD、OPTIONS
- **非幂等方法**：POST（每次创建新资源）、PATCH（增量更新可能非幂等）

> ⚠️ 幂等性在处理重试和网络异常时至关重要——支付接口必须保证幂等，防止重复扣款。

### 1.11 RESTful API 中的无状态性（Stateless）指什么？
无状态性要求**每个请求都包含服务器处理所需的所有信息**，服务器不保存任何客户端上下文。这意味着：
- 会话状态存储在客户端（如 JWT Token）
- 任何服务器实例都可以处理任意请求（水平扩展自然支持）
- 请求之间无依赖关系

### 1.12 什么是 API 版本控制？有哪些策略？
| 策略 | 示例 | 优点 | 缺点 |
|------|------|------|------|
| **URL 路径** | `/api/v1/users` | 最直观、易于路由 | 打破 REST 原则（URL 变化） |
| **查询参数** | `/api/users?version=1` | 实现简单 | 参数易被忽略 |
| **请求头** | `Accept: application/vnd.myapp.v1+json` | 符合 REST 原则，URL 不变 | 不够直观，调试困难 |
| **自定义 Header** | `X-API-Version: 1` | 简单，URL 不变 | 非标准做法 |

> 💡 业界主流推荐 **URL 路径方式**（`/api/v1/...`），兼具直观性和可维护性。版本号建议使用整数（v1、v2）而非语义化版本（1.0.0）。

### 1.13 Swagger / OpenAPI 是什么？
- **OpenAPI**：一个用于描述 RESTful API 的规范（原称 Swagger 规范）
- **Swagger**：实现 OpenAPI 规范的一套工具集
- **核心价值**：
  - 自动生成 API 文档（可交互的 Swagger UI）
  - 代码生成（从 OpenAPI 规范生成客户端 SDK）
  - API 测试（直接在文档页面发送请求）

### 1.14 什么是 HATEOAS？
**HATEOAS**（Hypermedia As The Engine Of Application State）是 REST 架构风格的约束之一。核心思想：API 的响应中应包含**相关的操作链接**，客户端通过这些链接动态发现可用操作，而非硬编码 URL。

```json
{
  "id": 1,
  "name": "Alice",
  "links": [
    { "rel": "self", "href": "/api/users/1" },
    { "rel": "orders", "href": "/api/users/1/orders" },
    { "rel": "update", "href": "/api/users/1", "method": "PUT" }
  ]
}
```

> 🎯 HATEOAS 在实践中应用较少，但面试中常作为 REST 理论深度考察点出现。

### 1.15 Webhook 与轮询（Polling）的区别？
| 维度 | Webhook | Polling |
|------|---------|---------|
| **通信方向** | 服务器推送给客户端 | 客户端主动拉取 |
| **实时性** | 事件发生后立即通知 | 取决于轮询间隔 |
| **资源消耗** | 低（有事件才触发） | 高（大量空轮询） |
| **实现复杂度** | 需暴露回调地址 | 简单 |
| **可靠性** | 需重试机制处理失败 | 每次都是新的请求 |

### 1.16 什么是 CORS？如何解决？
**CORS**（Cross-Origin Resource Sharing，跨域资源共享）是一种浏览器安全机制，限制不同域名之间的资源共享。解决方案：

| 方案 | 说明 |
|------|------|
| **@CrossOrigin 注解** | Spring Boot 中单个 Controller 启用跨域 |
| **全局 CORS 配置** | 实现 `WebMvcConfigurer` 统一配置 |
| **反向代理** | Nginx 配置跨域转发 |
| **CORS 过滤器** | 自定义 Filter 设置响应头 |

### 1.17 什么是 API 网关？常见功能？
API 网关是系统的**统一入口**，负责请求路由、流量管理和安全防护。核心功能：

| 功能 | 说明 |
|------|------|
| **路由转发** | 将请求转发到对应微服务 |
| **认证鉴权** | 统一认证、Token 校验 |
| **限流熔断** | 防止流量冲垮后端服务 |
| **日志监控** | 统一采集访问日志 |
| **协议转换** | HTTP 转 gRPC、MQTT 等 |
| **缓存** | 缓存高频查询响应 |

常见实现：Spring Cloud Gateway、Kong、Nginx + Lua、APISIX。

### 1.18 gRPC 与 REST 的对比？
| 维度 | REST | gRPC |
|------|------|------|
| **协议** | HTTP/1.1 或 HTTP/2 | HTTP/2 |
| **数据格式** | JSON / XML（文本） | Protobuf（二进制） |
| **接口定义** | 文档驱动（OpenAPI） | 代码驱动（.proto 文件） |
| **性能** | 较慢（文本序列化） | 极快（二进制、流式传输） |
| **浏览器支持** | 原生支持 | 需 gRPC-Web 代理 |
| **流式通信** | 仅 Server-Sent Events | 双向流原生支持 |
| **适用场景** | 外部 API、Web 应用 | 微服务内部通信、高性能场景 |

### 1.19 GraphQL 与 REST 的对比？
| 维度 | REST | GraphQL |
|------|------|---------|
| **数据获取** | 固定结构（过取/欠取） | 客户端精确指定所需字段 |
| **端点数量** | 多个端点（每个资源一个） | 单一端点 |
| **版本控制** | URL/Header 版本号 | 无需版本号，可扩展字段 |
| **缓存** | HTTP 缓存天然支持 | 需额外实现 |
| **学习曲线** | 低 | 中度 |
| **复杂查询** | 需多次请求或自定义端点 | 一次查询即可关联多资源 |

### 1.20 API 认证的常见方式有哪些？
| 认证方式 | 原理 | 适用场景 |
|----------|------|----------|
| **Basic Auth** | Base64 编码 `username:password` | 内部服务、简单场景 |
| **API Key** | 请求头携带固定 Key | 第三方开放 API |
| **JWT** | 自包含的 JSON Token（含签名） | 前后端分离、移动端 |
| **OAuth2** | 授权码流程，第三方授权 | 社交登录、开放平台 |
| **Session-Cookie** | 服务端存储会话，客户端存 Cookie | 传统 Web 应用 |
| **Bearer Token** | 请求头 `Authorization: Bearer <token>` | 通用 Token 传递方式 |

---

## 二、深度原理剖析（10-15题）

### 2.1 JWT 的结构与工作原理

**JWT（JSON Web Token）** 由三个部分组成，用 `.` 分隔：

```
header.payload.signature
```

| 部分 | 内容 | 示例 |
|------|------|------|
| **Header** | 算法类型 + Token 类型 | `{"alg":"HS256","typ":"JWT"}` |
| **Payload** | 声明（Claims）：sub、iat、exp、自定义字段 | `{"sub":"123","name":"Alice","iat":1516239022}` |
| **Signature** | 对 Header + Payload 的签名 | `HMACSHA256(base64UrlEncode(header) + "." + base64UrlEncode(payload), secret)` |

**工作流程**：
1. 用户登录 → 服务端验证凭证 → 签发 JWT
2. 客户端存储 JWT（localStorage / Cookie）
3. 后续请求携带 JWT（`Authorization: Bearer <token>`）
4. 服务端验证签名和有效期 → 提取用户信息

**JWT 优点**：
- 无状态：服务器无需存储会话
- 跨域友好：可跨多个服务使用
- 自包含：含用户信息和权限

**JWT 缺点**：
- 不可撤销（除非用黑名单）
- Payload 仅 Base64 编码，不可存放敏感信息
- Token 体积较大

> ⚠️ JWT 的 Payload 只是 Base64 编码，不是加密！任何人都可以解码阅读内容，**绝不可存放密码等敏感信息**。

### 2.2 JWT vs OAuth2 的区别与联系

| 维度 | JWT | OAuth2 |
|------|-----|--------|
| **本质** | 一种 Token 格式 | 一种授权框架 |
| **关系** | JWT 可作为 OAuth2 的 Access Token 格式 | OAuth2 不限定 Token 格式 |
| **关注点** | 如何安全传递用户信息 | 如何授权第三方访问资源 |
| **是否含用户信息** | 是（自包含） | 否（只是令牌，后端查询） |
| **单独使用** | 适用于前后端分离的登录认证 | 适用于第三方开放平台 |

> 🎯 **一句话总结**：JWT 是一种 Token 格式，OAuth2 是一种授权流程。两者可以结合使用——用 OAuth2 协议获取 JWT 格式的 Access Token。

### 2.3 OAuth2 的四种授权模式

| 授权模式 | 流程 | 适用场景 | 安全性 |
|----------|------|----------|--------|
| **授权码模式（Authorization Code）** | 用户授权 → 返回 code → 后端换 token | 有后端的 Web 应用 | 最高（令牌不暴露给浏览器） |
| **简化模式（Implicit）** | 直接返回 access_token（已废弃） | 纯前端应用（已不推荐） | 低 |
| **密码模式（Resource Owner Password Credentials）** | 用户名 + 密码直接换 token | 第一方应用、高度信任场景 | 中 |
| **客户端凭证模式（Client Credentials）** | 客户端 ID + Secret 换 token | 服务间通信、无用户场景 | 高 |

> 💡 **授权码模式 + PKCE**（Proof Key for Code Exchange）是目前移动端和 SPA 应用的标准推荐方案。

### 2.4 RESTful API 的六大约束

| 约束 | 说明 |
|------|------|
| **客户端-服务端分离** | UI 与数据存储分离，各自独立演进 |
| **无状态** | 每个请求包含全部必要信息 |
| **可缓存** | 响应隐式或显式标记为可缓存/不可缓存 |
| **统一接口** | 固定的资源操作方式（HTTP 方法 + 状态码） |
| **分层系统** | 客户端无法直接知道是否连接到端服务器 |
| **按需代码（可选）** | 服务器可向客户端传输可执行代码 |

### 2.5 Token 刷新机制（Refresh Token）

**刷新流程**：
1. 登录时返回 Access Token（短期，如 30 分钟）+ Refresh Token（长期，如 7 天）
2. Access Token 过期时，客户端用 Refresh Token 换取新的 Access Token
3. Refresh Token 也可滚动更新（旧 Refresh Token 失效）

```java
// 登录接口返回双 Token
{
  "access_token": "eyJhbG...",
  "refresh_token": "dGhpcyBp...",
  "token_type": "Bearer",
  "expires_in": 1800
}
```

| 对比 | Access Token | Refresh Token |
|------|--------------|---------------|
| **有效期** | 短（15-60 分钟） | 长（7-30 天） |
| **使用频率** | 每次请求携带 | 仅在过期时使用 |
| **存储位置** | 内存 / localStorage | 更安全的 HttpOnly Cookie |
| **安全风险** | 泄露影响范围小 | 泄露可长期获取新 Token |

### 2.6 限流算法对比

| 算法 | 原理 | 优点 | 缺点 |
|------|------|------|------|
| **固定窗口** | 固定时间窗口内计数 | 实现简单 | 窗口边界突增流量（临界问题） |
| **滑动窗口** | 按时间戳记录请求，滑动计数 | 平滑、无临界问题 | 需记录每个请求时间戳 |
| **令牌桶** | 固定速率放入令牌，请求消耗令牌 | 允许突发流量 | 突发可能超过实际速率 |
| **漏桶** | 请求进入队列，固定速率处理 | 严格平滑流量 | 无法应对突发 |

```java
// 令牌桶算法示例（Spring Boot + Redis）
public boolean tryAcquire(String key, int permits, int rate, int capacity) {
    String script = """
        local key = KEYS[1]
        local now = redis.call('TIME')[1]
        local lastRefillTime = redis.call('GET', key .. ':time') or now
        local tokens = redis.call('GET', key .. ':tokens') or capacity
        local elapsed = now - lastRefillTime
        tokens = math.min(capacity, tokens + elapsed * rate)
        redis.call('SET', key .. ':time', now)
        if tokens >= permits then
            redis.call('SET', key .. ':tokens', tokens - permits)
            return 1
        end
        return 0
    """;
    return redisTemplate.execute(redisScript, List.of(key), permits);
}
```

### 2.7 缓存策略：ETag、Last-Modified、Cache-Control

| 策略 | 说明 | 实现方式 |
|------|------|----------|
| **Cache-Control** | 指定缓存策略（max-age、no-cache、private） | 响应头设置 |
| **ETag** | 资源内容的哈希标识，配合 `If-None-Match` | 返回 304 Not Modified |
| **Last-Modified** | 资源最后修改时间，配合 `If-Modified-Since` | 返回 304 Not Modified |

```java
// Spring Boot ETag 配置
@Configuration
public class ETagConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ETagInterceptor());
    }
}

// 或者使用 ShallowEtagHeaderFilter
@Bean
public ShallowEtagHeaderFilter shallowEtagHeaderFilter() {
    return new ShallowEtagHeaderFilter();
}
```

### 2.8 分页策略：Cursor-based vs Offset-based

| 维度 | Offset-based（偏移量分页） | Cursor-based（游标分页） |
|------|--------------------------|------------------------|
| **原理** | `OFFSET 100 LIMIT 20` | `WHERE id > 100 LIMIT 20` |
| **稳定性** | 数据插入/删除导致页码偏移 | 稳定（基于游标） |
| **性能** | 大偏移量时性能下降 | 始终高效（走索引） |
| **随机跳页** | 支持（直接跳到第 N 页） | 不支持（只能翻页） |
| **适用场景** | 后台管理、数据量小 | 社交 Feed、大数据量 |
| **实现** | `GET /api/users?page=3&size=20` | `GET /api/users?cursor=100&limit=20` |

```java
// Offset-based 分页（Spring Boot + JPA）
@GetMapping("/users")
public Page<User> getUsers(@RequestParam int page, @RequestParam int size) {
    return userRepository.findAll(PageRequest.of(page, size));
}

// Cursor-based 分页
@GetMapping("/users/cursor")
public List<User> getUsersByCursor(@RequestParam(required = false) Long cursor,
                                    @RequestParam(defaultValue = "20") int limit) {
    return userRepository.findByIdGreaterThan(cursor, PageRequest.of(0, limit));
}
```

### 2.9 幂等性保障方案

**幂等键（Idempotency Key）** 机制：

1. 客户端生成唯一幂等键（UUID），放入请求头 `Idempotency-Key`
2. 服务端首次处理时，缓存幂等键和响应结果
3. 相同幂等键再次请求时，直接返回缓存的响应

```java
// 幂等性校验过滤器
@Component
public class IdempotencyFilter extends OncePerRequestFilter {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String idempotencyKey = request.getHeader("Idempotency-Key");
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            throw new IllegalArgumentException("Missing Idempotency-Key header");
        }

        String cacheKey = "idempotency:" + idempotencyKey;
        Object cachedResponse = redisTemplate.opsForValue().get(cacheKey);
        if (cachedResponse != null) {
            response.getWriter().write(cachedResponse.toString());
            return;
        }

        // 设置幂等键到请求属性，以便在 Controller 处理完成后缓存响应
        request.setAttribute("IDEMPOTENCY_KEY", cacheKey);
        chain.doFilter(request, response);
    }
}
```

### 2.10 统一错误处理设计

```java
// 标准错误响应体
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiError {
    private int status;          // HTTP 状态码
    private String code;         // 业务错误码
    private String message;      // 错误描述
    private String path;         // 请求路径
    private LocalDateTime timestamp;  // 时间戳
    private Map<String, String> details;  // 字段级验证错误
}

// 全局异常处理器
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidationException(MethodArgumentNotValidException ex,
                                               HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
        return new ApiError(400, "VALIDATION_ERROR", "参数校验失败",
                request.getRequestURI(), LocalDateTime.now(), errors);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFoundException(ResourceNotFoundException ex,
                                             HttpServletRequest request) {
        return new ApiError(404, "NOT_FOUND", ex.getMessage(),
                request.getRequestURI(), LocalDateTime.now(), null);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleGenericException(Exception ex, HttpServletRequest request) {
        return new ApiError(500, "INTERNAL_ERROR", "服务器内部错误",
                request.getRequestURI(), LocalDateTime.now(), null);
    }
}

// 自定义业务异常
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " 未找到，id: " + id);
    }
}
```

---

## 三、实战场景题（8-12题）

### 3.1 如何处理大量并发请求导致的服务崩溃？
**思路**：限流 + 熔断 + 降级 + 异步
1. **限流**：接入层（Nginx）和应用层（Redis + 令牌桶）双重限流
2. **熔断**：使用 Sentinel / Resilience4j 监控错误率，触发熔断
3. **降级**：返回兜底数据（缓存 / 默认值），保证核心功能可用
4. **异步**：写操作改为 MQ 异步处理，削峰填谷

> 💡 面试中要区分"限流"（阻止请求进入）和"熔断"（停止调用下游失败服务）。

### 3.2 接口响应太慢，如何排查和优化？
**排查步骤**：
1. **APM 监控**（SkyWalking / Pinpoint）：定位耗时瓶颈
2. **慢 SQL 分析**：通过数据库慢查询日志定位
3. **链路追踪**：定位远程调用耗时

**优化手段**：
| 层面 | 方案 |
|------|------|
| **数据库** | 加索引、SQL 优化、读写分离、分表分库 |
| **缓存** | Redis 缓存热点数据，缓存穿透/击穿/雪崩防护 |
| **业务** | 异步处理、批量接口替代循环调用 |
| **网络** | 压缩响应（gzip）、连接池调优 |
| **计算** | 并行流、线程池、CompletableFuture |

```java
// 并行调用多个独立服务
public UserDetailVO getUserDetail(Long userId) {
    CompletableFuture<UserInfo> userInfo = 
        CompletableFuture.supplyAsync(() -> userService.getUser(userId));
    CompletableFuture<List<Order>> orders = 
        CompletableFuture.supplyAsync(() -> orderService.getOrders(userId));
    CompletableFuture<List<Coupon>> coupons = 
        CompletableFuture.supplyAsync(() -> couponService.getCoupons(userId));

    return CompletableFuture.allOf(userInfo, orders, coupons)
            .thenApply(v -> new UserDetailVO(
                userInfo.join(), orders.join(), coupons.join()))
            .join();
}
```

### 3.3 如何设计一个安全的登录接口？
1. **传输安全**：必须使用 HTTPS，防止中间人攻击
2. **密码安全**：BCrypt 加密存储（不可逆），绝不存明文
3. **验证码**：登录失败超过 N 次后要求图形验证码，防暴力破解
4. **JWT 签发**：生成 Access Token（短有效）+ Refresh Token（长有效）
5. **设备指纹**：记录登录设备信息，异常登录告警
6. **登录历史**：记录 IP、时间、设备，支持"下线其他设备"

```java
@PostMapping("/auth/login")
public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    // 1. 验证码校验
    captchaService.verify(request.getCaptchaKey(), request.getCaptchaCode());

    // 2. 用户验证
    User user = userService.login(request.getUsername(), request.getPassword());

    // 3. 签发 JWT
    String accessToken = jwtService.generateAccessToken(user.getId(), user.getRoles());
    String refreshToken = jwtService.generateRefreshToken(user.getId());

    // 4. 记录登录日志
    loginLogService.record(user.getId(), request.getIp(), request.getDeviceInfo());

    return ResponseEntity.ok(new LoginResponse(accessToken, refreshToken));
}
```

### 3.4 如何设计第三方开放 API（如支付回调）？
| 安全策略 | 实现 |
|----------|------|
| **签名校验** | 请求参数 + AppSecret 进行 HMAC-SHA256 签名 |
| **重放防护** | 请求体中包含 `nonce`（随机数）+ `timestamp`（时间戳） |
| **幂等处理** | 使用 `out_trade_no`（商户订单号）作为幂等键 |
| **IP 白名单** | 限制回调 IP 为第三方固定 IP 段 |
| **异步通知** | 7 天阶梯式重试（15s / 30s / 1min / 5min / 30min / ...） |

### 3.5 如何处理大文件上传？
1. **分片上传**：前端将文件切分为 5MB 的切片，逐个上传
2. **断点续传**：每个切片携带切片索引和文件 MD5，服务端记录已上传切片
3. **秒传**：上传前先计算文件 MD5，检查服务端是否已存在
4. **异步合并**：所有切片上传完成后，异步合并文件
5. **流式处理**：使用 `MultipartFile` 的流式 API，避免 OOM

```java
// 分片上传状态查询
@GetMapping("/upload/status")
public UploadStatus getUploadStatus(@RequestParam String fileMd5) {
    Set<Integer> uploadedChunks = redisTemplate.opsForSet()
            .members("upload:" + fileMd5);
    return new UploadStatus(uploadedChunks);
}

// 分片上传
@PostMapping("/upload/chunk")
public ResponseEntity<Void> uploadChunk(
        @RequestParam String fileMd5,
        @RequestParam int chunkIndex,
        @RequestParam MultipartFile file) {
    // 存储切片到临时目录
    file.transferTo(new File(uploadDir + "/" + fileMd5 + "/" + chunkIndex));
    // 记录已上传切片
    redisTemplate.opsForSet().add("upload:" + fileMd5, chunkIndex);
    return ResponseEntity.ok().build();
}
```

### 3.6 如何处理接口的敏感数据脱敏？
| 数据类型 | 脱敏方式 | 示例 |
|----------|----------|------|
| **手机号** | 中间四位替换为 `****` | `138****1234` |
| **身份证** | 前六后四显示 | `110101****1234` |
| **银行卡** | 仅显示后四位 | `**** **** **** 5678` |
| **邮箱** | @ 前部分隐藏 | `a***@example.com` |
| **密码** | 绝不可返回 | 始终返回 `null` |

```java
// 使用 Jackson 序列化注解实现脱敏
@JsonSerialize(using = PhoneDesensitizeSerializer.class)
private String phone;

public class PhoneDesensitizeSerializer extends JsonSerializer<String> {
    @Override
    public void serialize(String value, JsonGenerator gen,
                          SerializerProvider provider) throws IOException {
        if (value != null && value.length() == 11) {
            gen.writeString(value.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"));
        } else {
            gen.writeString(value);
        }
    }
}
```

### 3.7 如何保证消息推送的可靠性（Webhook）？
1. **签名验证**：推送请求携带 HMAC 签名，接收方校验
2. **重试机制**：指数退避重试（1s → 2s → 4s → 8s → ... 最大 3 天）
3. **去重**：每条消息携带唯一 `event_id`，接收方幂等处理
4. **回调日志**：记录每次推送请求和响应
5. **死信队列**：超过最大重试次数的消息进入死信队列，人工处理
6. **健康检查**：定期发送 ping 请求检测接收方可达性

### 3.8 如何设计一个短链接服务？
| 模块 | 方案 |
|------|------|
| **短码生成** | 62 进制转换（0-9 a-z A-Z）或雪花算法 + Base62 |
| **存储** | Redis（热点）+ MySQL（持久化） |
| **重定向** | `302` 临时重定向（可追踪点击数） |
| **过期策略** | TTL + 定时清理过期链接 |
| **防滥用** | 单 IP 创建频率限制、链接白名单/黑名单 |

---

## 四、手写代码题（5-8题）

### 4.1 Spring Boot REST Controller（员工管理系统）

```java
@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Validated
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public ResponseEntity<Page<EmployeeDTO>> listEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String department) {
        Page<EmployeeDTO> employees = employeeService.findAll(page, size, department);
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeDTO> getEmployee(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.findById(id));
    }

    @PostMapping
    public ResponseEntity<EmployeeDTO> createEmployee(
            @Valid @RequestBody CreateEmployeeRequest request) {
        EmployeeDTO employee = employeeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(employee);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeDTO> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        return ResponseEntity.ok(employeeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<EmployeeDTO> updateStatus(
            @PathVariable Long id,
            @RequestBody @Valid StatusUpdateRequest request) {
        return ResponseEntity.ok(employeeService.updateStatus(id, request.getStatus()));
    }
}
```

### 4.2 JWT 认证过滤器

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/v1/auth/login", "/api/v1/auth/register",
            "/api/v1/auth/refresh", "/swagger-ui/**", "/v3/api-docs/**");

    @Autowired
    private JwtService jwtService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return WHITE_LIST.stream().anyMatch(pattern ->
                path.startsWith(pattern.replace("/**", "")));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AuthenticationException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        try {
            // 1. 验证 Token 签名和有效期
            Claims claims = jwtService.validateToken(token);

            // 2. 构建认证对象
            Long userId = claims.get("userId", Long.class);
            List<String> roles = claims.get("roles", List.class);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId, null,
                            roles.stream().map(SimpleGrantedAuthority::new).toList());

            // 3. 设置到 SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            throw new AuthenticationException("Token has expired");
        } catch (JwtException e) {
            throw new AuthenticationException("Invalid token");
        }
    }
}
```

### 4.3 Swagger / OpenAPI 配置

```java
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "员工管理系统 API",
        version = "v1.0",
        description = "RESTful 员工管理接口文档",
        contact = @Contact(name = "技术团队", email = "dev@company.com"),
        license = @License(name = "Apache 2.0")
    ),
    externalDocs = @ExternalDocumentation(
        description = "完整 API 规范文档",
        url = "https://wiki.company.com/api-guide"
    )
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/api/v1/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .pathsToMatch("/api/admin/**")
                .build();
    }
}
```

### 4.4 全局 CORS 配置

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("https://*.company.com", "http://localhost:*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("X-Total-Count", "X-RateLimit-Remaining")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

### 4.5 统一响应体封装

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(201, "created", data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null, LocalDateTime.now());
    }

    // 分页响应
    public static <T> ApiResponse<PageData<T>> page(Page<T> page) {
        PageData<T> pageData = new PageData<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
        return success(pageData);
    }
}

@Data
@AllArgsConstructor
public class PageData<T> {
    private List<T> content;
    private long total;
    private int totalPages;
    private int currentPage;
    private int pageSize;
}
```

### 4.6 自定义注解实现接口幂等性

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    String key() default "";
    int ttl() default 60;  // 幂等键有效期（秒）
}

@Aspect
@Component
public class IdempotentAspect {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        // 从请求头获取幂等键
        HttpServletRequest request = ((ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes()).getRequest();
        String idempotencyKey = request.getHeader("Idempotency-Key");
        if (idempotencyKey == null) {
            throw new IllegalArgumentException("Missing Idempotency-Key header");
        }

        String key = "idempotent:" + idempotent.key() + ":" + idempotencyKey;

        // SET NX 实现原子性
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, "PENDING", Duration.ofSeconds(idempotent.ttl()));

        if (Boolean.FALSE.equals(success)) {
            String result = redisTemplate.opsForValue().get(key);
            if ("PENDING".equals(result)) {
                throw new RuntimeException("请求正在处理中");
            }
            return JSON.parseObject(result, Object.class);
        }

        try {
            Object result = joinPoint.proceed();
            redisTemplate.opsForValue().set(key, JSON.toJSONString(result),
                    Duration.ofSeconds(idempotent.ttl()));
            return result;
        } catch (Exception e) {
            redisTemplate.delete(key);
            throw e;
        }
    }
}
```

### 4.7 验证码校验实现

```java
@RestController
@RequestMapping("/api/v1/captcha")
public class CaptchaController {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @GetMapping
    public ResponseEntity<CaptchaVO> getCaptcha() {
        // 生成 4 位数字验证码
        String code = String.format("%04d", new Random().nextInt(10000));
        String key = UUID.randomUUID().toString();

        // 存储到 Redis，5 分钟过期
        redisTemplate.opsForValue().set("captcha:" + key, code, 5, TimeUnit.MINUTES);

        // 生成 Base64 图片（实际项目中用 EasyCaptcha / Kaptcha 库）
        String base64Image = generateImage(code);

        return ResponseEntity.ok(new CaptchaVO(key, base64Image));
    }

    public void verify(String key, String code) {
        String cacheKey = "captcha:" + key;
        String cachedCode = redisTemplate.opsForValue().get(cacheKey);
        if (cachedCode == null) {
            throw new BusinessException("验证码已过期");
        }
        if (!cachedCode.equalsIgnoreCase(code)) {
            throw new BusinessException("验证码错误");
        }
        // 验证后立即删除，防止重复使用
        redisTemplate.delete(cacheKey);
    }
}
```

### 4.8 基于 Spring Event 的异步通知机制

```java
// 事件定义
public class OrderCreatedEvent extends ApplicationEvent {
    private final Long orderId;
    private final Long userId;
    public OrderCreatedEvent(Object source, Long orderId, Long userId) {
        super(source);
        this.orderId = orderId;
        this.userId = userId;
    }
}

// 事件发布
@Service
public class OrderService {
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public Order createOrder(CreateOrderRequest request) {
        Order order = saveOrder(request);
        eventPublisher.publishEvent(new OrderCreatedEvent(this, order.getId(), order.getUserId()));
        return order;
    }
}

// 事件监听（异步）
@Component
public class OrderEventListener {
    @EventListener
    @Async
    public void handleOrderCreated(OrderCreatedEvent event) {
        // 发送短信通知
        smsService.sendOrderConfirmation(event.getUserId(), event.getOrderId());
        // 记录审计日志
        auditLogService.log("order_created", event.getOrderId());
        // 推送消息到 MQ
        messageQueueService.send("order.topic", event.getOrderId());
    }
}
```

---

## 五、系统设计题（3-5题）

### 5.1 设计一个短 URL 生成系统

**需求**：长链接转短链接，支持过期时间，支持访问统计。

**核心设计**：

| 模块 | 技术选型 | 说明 |
|------|----------|------|
| **短码生成** | 雪花算法 ID → Base62 编码 | 6-7 位短码，< B 量级唯一 ID |
| **存储** | Redis + MySQL | Redis 加速读，MySQL 持久化 |
| **读写策略** | 写 DB 同时写 Redis | 读请求查 Redis，缓存未命中查 DB 并回填 |
| **重定向** | Nginx + Lua / Spring Boot | 302 临时重定向，记录点击日志 |
| **过期清理** | Redis TTL + 定时任务 | TTL 自动过期，定时任务清理 DB |

**接口定义**：
```
POST /api/v1/shorten     // 创建短链接
  Request:  { "url": "https://...", "expireIn": 86400 }
  Response: { "shortUrl": "https://s.co/Ab3XyZ", "expireAt": "2026-07-23T12:00:00" }

GET /{shortCode}          // 访问短链接（重定向到原始 URL）
  Response: 302 → Location: https://...

GET /api/v1/analytics/{shortCode}  // 获取访问统计
  Response: { "totalClicks": 1024, "dailyClicks": [...] }
```

### 5.2 设计一个支持海量请求的 API 网关

**核心功能**：

```yaml
# 网关路由配置（Spring Cloud Gateway）
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/v1/users/**
          filters:
            - name: RequestRateLimiter
              args:
                key-resolver: "#{@ipKeyResolver}"
                redis-rate-limiter:
                  replenishRate: 100
                  burstCapacity: 200
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/v1/orders/**
          filters:
            - StripPrefix=1
            - name: CircuitBreaker
              args:
                name: orderService
                fallbackUri: forward:/fallback/orders
```

**限流 + 熔断 + 降级**架构：

```
客户端 → Nginx（Lua 限流）→ API Gateway（令牌桶限流）
          ↓                           ↓
    Sentinel 熔断监控         Resilience4j 熔断器
          ↓                           ↓
    降级响应（缓存/默认值）         微服务集群
```

### 5.3 设计 RESTful 电商订单 API

**资源模型**：
```
/users          → 用户管理
/users/{id}/addresses → 用户地址
/products       → 商品列表
/products/{id}  → 商品详情
/products/{id}/reviews → 商品评价
/orders         → 订单列表
/orders/{id}    → 订单详情
/orders/{id}/items → 订单明细
/cart/items     → 购物车
/payments/{orderId} → 支付信息
```

**订单流程设计**：
```java
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    @PostMapping
    public ApiResponse<OrderVO> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        // 幂等创建订单
        OrderVO order = orderService.create(request);
        // 异步发送创建事件
        eventPublisher.publishEvent(new OrderCreatedEvent(order.getId()));
        return ApiResponse.created(order);
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<Void> cancelOrder(@PathVariable Long orderId) {
        // 订单状态机校验（待支付 → 已取消）
        orderService.cancel(orderId);
        return ApiResponse.success(null);
    }

    @GetMapping("/{orderId}/status")
    public ApiResponse<OrderStatusVO> getOrderStatus(@PathVariable Long orderId) {
        // 查询订单最新状态
        return ApiResponse.success(orderService.getStatus(orderId));
    }
}
```

### 5.4 设计一个第三方开放平台 API

**认证流程**：
```
1. 开发者注册应用 → 获取 AppId + AppSecret
2. 用户授权 → 跳转 OAuth2 授权页
3. 回调获取 authorization_code
4. code 换取 access_token（JWT 格式）
5. 携带 access_token 调用开放 API
```

**安全机制**：
| 机制 | 实现 |
|------|------|
| **签名算法** | `sign = HMAC-SHA256(appId + timestamp + nonce + body, appSecret)` |
| **防重放** | timestamp 5 分钟有效 + nonce 去重（Redis 记录） |
| **访问频率** | 按 AppId 限流（QPS 配额），超额返回 429 |
| **数据权限** | access_token scope 控制可访问的资源范围 |

---

## 六、常见坑点与最佳实践（表格）

### 6.1 API 设计常见坑点

| 坑点 | 问题描述 | 最佳实践 |
|------|----------|----------|
| **URL 中出现动词** | `/api/getUser`、`/api/deleteUser` | 使用 HTTP 方法表达动作，URL 只包含名词 |
| **状态码滥用** | 所有请求都返回 200，通过 body 中的 `code` 判断 | 正确使用 HTTP 状态码（201、400、404、500） |
| **忽略分页** | 一次性返回全量数据 | 所有列表接口必须分页，默认 20 条/页 |
| **明文传输密码** | 登录请求使用 HTTP | 强制 HTTPS，密码字段加密传输 |
| **版本控制缺失** | 接口变更直接修改原 URL | 使用 `/api/v1/...` 管理版本 |
| **枚举值硬编码** | 前端硬编码状态值 | 通过 API 返回枚举定义，支持国际化 |
| **没有统一错误格式** | 每个接口返回不同结构的错误 | 全局统一使用 `ApiError` 结构 |
| **忽略幂等性** | 网络重试导致重复下单 | POST 操作使用 `Idempotency-Key` |

### 6.2 RESTful API 最佳实践

| 实践 | 说明 |
|------|------|
| **使用名词复数** | `/api/users` 而非 `/api/user` |
| **嵌套合理** | 不超过三层：`/users/{id}/orders/{orderId}/items` |
| **查询参数过滤** | `/api/users?role=admin&status=active&page=0&size=20` |
| **字段选择** | 支持 `?fields=id,name,email` 减少传输量 |
| **HATEOAS 链接** | 响应中包含相关操作链接（可选） |
| **限流响应头** | `X-RateLimit-Limit`、`X-RateLimit-Remaining`、`X-RateLimit-Reset` |
| **Content-Type 协商** | 支持 `Accept: application/json` 和 `Accept: application/xml` |

### 6.3 API 安全最佳实践

| 实践 | 说明 |
|------|------|
| **强制 HTTPS** | 所有 API 必须通过 HTTPS 访问 |
| **JWT 黑名单** | 用户登出时加入 Redis 黑名单，设置过期时间与 Token 一致 |
| **短有效期 Token** | Access Token 15-60 分钟，配合 Refresh Token |
| **输入校验** | 所有输入参数进行白名单校验，防止 SQL 注入和 XSS |
| **CORS 白名单** | 明确指定允许的域名，不使用通配符 |
| **速率限制** | 按 IP 和用户双重维度限流 |
| **敏感信息脱敏** | 手机号、身份证等返回前脱敏 |
| **日志脱敏** | 日志中不记录密码、Token 等敏感信息 |

### 6.4 性能优化最佳实践

| 实践 | 说明 |
|------|------|
| **连接池** | 数据库连接池（HikariCP）、HTTP 连接池配置合理大小 |
| **缓存** | 热点数据使用 Redis 缓存，缓存穿透/击穿/雪崩防护 |
| **压缩** | 启用 Gzip 压缩，大响应体积减少 70%+ |
| **批量接口** | 提供 `/api/batch` 接口减少请求次数 |
| **异步非阻塞** | 耗时的写操作使用 MQ 异步处理 |
| **数据库索引** | 查询字段必须加索引，SQL 执行计划分析 |
| **懒加载** | JPA 关联查询使用 `@EntityGraph` 或 Fetch Join 避免 N+1 |

---

## 七、面试回答模板（Top 5）

### 7.1 "请介绍一下 RESTful API 的设计原则"

> **RESTful API 的核心设计原则主要有六点。第一，资源导向——每个 URL 代表一个资源，比如 `/users` 代表用户集合，`/users/1` 代表单个用户，URL 中不包含动词。第二，使用标准的 HTTP 方法来操作资源——GET 获取、POST 创建、PUT 全量更新、PATCH 部分更新、DELETE 删除。第三，无状态性——服务器不保存客户端状态，每个请求都包含所有必要信息，这对水平扩展非常重要。第四，统一接口——资源通过统一的 URL 和 HTTP 方法访问，返回统一的 JSON 结构。第五，可缓存——通过 Cache-Control、ETag 等机制提升性能。第六，分层系统——客户端不需要知道通信链路上的中间节点。在实践中，我们还应该注意 URL 命名使用名词复数、正确使用 HTTP 状态码、统一错误响应格式，以及接口版本控制。"

### 7.2 "JWT 的工作原理是什么？有什么优缺点？"

> **JWT 全称 JSON Web Token，由三部分组成：Header、Payload、Signature。Header 指定签名算法，Payload 包含用户信息和声明 Claims，Signature 是对前两部分的签名用于防篡改。工作流程是：用户登录成功后，后端生成 JWT 返回给客户端；客户端将其存储在本地，后续请求通过 Authorization: Bearer 头携带；后端收到请求后验证签名的完整性和有效期，从中提取用户信息。优点是无状态、跨域友好、自包含。缺点是 Token 一旦签发无法撤销（除非建立黑名单机制）、Payload 只是 Base64 编码而非加密不能存敏感信息、Token 体积较大。在实际项目中，我们通常将 Access Token 设置为短有效期，配合 Refresh Token 进行续期。**

### 7.3 "如何设计一个高可用的 API 接口？"

> **设计高可用的 API 接口需要从多个层面考虑。在接入层，Nginx 做负载均衡和限流，防止流量冲击。在应用层，使用分布式限流组件如 Sentinel 进行精准限流和熔断降级，保证核心业务不受影响。在数据层，使用 Redis 缓存热点数据，数据库做读写分离和分表分库。在代码层面，使用连接池、异步处理、并行调用等优化手段。此外，还需要完善的监控告警体系——包括接口 QPS、响应时间、错误率等指标的实时监控。最后，幂等性和重试机制也是高可用设计的重要部分，防止网络抖动导致的数据不一致。**

### 7.4 "OAuth2 的授权码流程是怎样的？"

> **授权码模式是 OAuth2 中安全性最高的授权方式。流程如下：首先，用户在前端点击"第三方登录"，前端将用户重定向到授权服务器的登录页面，携带 client_id、redirect_uri 和 response_type=code 参数。第二，用户在授权页面上登录并确认授权。第三，授权服务器通过回调地址返回一个 authorization_code。第四，后端拿着这个 code 连同 client_secret 向授权服务器请求 access_token。第五，授权服务器验证 code 和 secret 后返回 access_token 和 refresh_token。整个过程的关键在于 authorization_code 和最终的 access_token 都是通过后端直接交换的，不暴露给浏览器或前端，安全性最高。现在移动端和 SPA 应用还会配合 PKCE 扩展进一步增强安全性。**

### 7.5 "如何保证接口的幂等性？"

> **接口幂等性保证的核心方案是使用幂等键。具体来说，客户端在每次请求时生成一个全局唯一的幂等键 UUID，放在请求头 Idempotency-Key 中携带。服务端收到请求后，先检查 Redis 中是否已存在该幂等键——如果存在则直接返回上次的响应结果；如果不存在则设置并处理请求。我的项目中通常使用 AOP 注解的方式实现，通过 @Idempotent 注解标记需要幂等性保证的接口，在切面中自动处理幂等键的校验和响应缓存。同时需要注意，幂等键也要设置有效期，防止 Redis 内存泄漏。对于数据库层面，可以通过唯一索引配合 INSERT ... ON DUPLICATE KEY UPDATE 来实现。**

---

## 八、快速查漏补缺 Checklist

### 8.1 基础概念
- [ ] 理解 RESTful 六大约束
- [ ] 掌握 HTTP 请求/响应格式
- [ ] 熟记 HTTP 状态码（200、201、204、301、400、401、403、404、500）
- [ ] 区分 HTML 响应和 JSON 响应
- [ ] 理解幂等性（GET/PUT/DELETE 幂等，POST 非幂等）
- [ ] 掌握 URL 命名规范（名词复数、层级关系）
- [ ] 区分 GET/POST/PUT/PATCH/DELETE

### 8.2 认证与安全
- [ ] JWT 结构（Header、Payload、Signature）
- [ ] JWT 的签发和验证流程
- [ ] OAuth2 四种授权模式及适用场景
- [ ] Refresh Token 刷新机制
- [ ] CORS 原理及解决方案
- [ ] API 签名校验（HMAC-SHA256）
- [ ] 常见安全攻击防护（SQL 注入、XSS、CSRF）

### 8.3 高级特性
- [ ] 限流算法（令牌桶、漏桶、滑动窗口、固定窗口）
- [ ] 缓存策略（Cache-Control、ETag、Last-Modified）
- [ ] 分页策略（Offset-based vs Cursor-based）
- [ ] 分片上传和断点续传
- [ ] Webhook 重试和签名机制
- [ ] API 版本控制策略
- [ ] gRPC vs REST vs GraphQL

### 8.4 实践能力
- [ ] 能手写 Spring Boot REST Controller
- [ ] 能手写 JWT 认证过滤器
- [ ] 能配置 Swagger / OpenAPI
- [ ] 能配置全局 CORS
- [ ] 能实现全局异常处理器
- [ ] 能实现幂等性 AOP 注解
- [ ] 能封装统一响应体

### 8.5 系统设计
- [ ] 短链接系统设计
- [ ] API 网关设计
- [ ] 电商订单 API 设计
- [ ] 第三方开放平台 API 设计
- [ ] 高可用 API 设计思路

> 🎯 **面试策略**：先过一遍 Checklist，标记自己不熟悉的知识点重点复习。概念题用"总-分-总"结构回答（先给出结论，再分点展开，最后总结升华）。代码题先写核心逻辑框架再补细节。系统设计题先明确需求边界，再画架构图，最后深入某个模块。

---

> 本文档基于 API 开发教程课程大纲整理，涵盖面试高频考点，适用于 Java 后端开发、全栈开发等岗位的 API 相关面试准备。
