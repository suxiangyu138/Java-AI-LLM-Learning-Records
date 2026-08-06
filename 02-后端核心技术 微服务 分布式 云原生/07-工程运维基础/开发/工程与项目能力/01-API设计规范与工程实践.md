# API 设计规范与工程实践

> 在分布式系统与前后端分离架构中，API 是不同服务之间的"合同"——设计规范则协作高效，设计混乱则故障频发。本章从 RESTful 核心原则出发，覆盖统一响应、版本管理、安全防护、文档生成等完整工程实践。

---

## 目录

1. [RESTful API 设计原则](#1-restful-api-设计原则)
2. [统一响应格式设计](#2-统一响应格式设计)
3. [API 版本管理策略](#3-api-版本管理策略)
4. [分页设计规范](#4-分页设计规范)
5. [异常处理与错误码体系](#5-异常处理与错误码体系)
6. [API 安全防护](#6-api-安全防护)
7. [API 文档与 OpenAPI 规范](#7-api-文档与-openapi-规范)
8. [幂等性设计](#8-幂等性设计)
9. [HATEOAS 与超媒体驱动](#9-hateoas-与超媒体驱动)
10. [GraphQL vs REST 对比](#10-graphql-vs-rest-对比)
11. [完整实战：用户管理 API 设计](#11-完整实战用户管理-api-设计)
12. [业界参考：Stripe 与 GitHub 的 API 设计哲学](#12-业界参考stripe-与-github-的-api-设计哲学)

---

## 1. RESTful API 设计原则

### 1.1 资源命名规范

REST（Representational State Transfer）以资源为中心，每条 URL 代表一个资源或资源集合。

| 规范 | 正确示例 | 错误示例 |
|------|----------|----------|
| **名词复数** | `/users`, `/orders` | `/getUser`, `/user` |
| **层级关系用斜杠** | `/users/123/orders` | `/ordersByUser?id=123` |
| **连字符分隔单词** | `/user-profiles` | `/user_profiles`, `/userProfiles` |
| **小写字母** | `/api/v1/products` | `/api/v1/Products` |
| **查询参数用于过滤/排序** | `?status=active&page=1` | `/getActiveUsers/page1` |
| **动作转名词** | `/payments` + `POST` | `/makePayment` |

> 💡 资源名一定是名词复数形式，不要用动词。动词通过 HTTP 方法表达。

### 1.2 HTTP 方法语义详解

| 方法 | 作用 | 幂等 | 安全（不修改资源） | 请求体 | 响应码 |
|------|------|------|-------------------|--------|--------|
| `GET` | 获取资源 | 是 | 是 | 无 | 200 |
| `POST` | 创建资源 | 否 | 否 | 有 | 201 |
| `PUT` | 全量替换资源 | 是 | 否 | 有 | 200/204 |
| `PATCH` | 部分更新资源 | 否* | 否 | 有 | 200 |
| `DELETE` | 删除资源 | 是 | 否 | 可选 | 204 |

> ⚠️ `PATCH` 的幂等性取决于实现——如果使用 JSON Merge Patch (`{"field": "value"}`) 则是幂等的，如果使用 JSON Patch (`[{"op": "add", ...}]`) 则可能非幂等。

### 1.3 状态码选择决策表

面对一个 API 响应，按照以下决策树选择最合适的状态码：

```
请求到达服务器
├── 请求未到达 → 客户端网络问题（不是 HTTP 状态码的职责）
└── 请求到达服务器
    ├── 服务器内部错误 → 500 Internal Server Error
    ├── 网关/代理错误
    │   ├── 上游超时 → 504 Gateway Timeout
    │   └── 上游不可达 → 502 Bad Gateway
    ├── 服务暂不可用（限流/熔断） → 503 Service Unavailable
    └── 请求处理成功
        ├── 请求格式错误（JSON 解析失败） → 400 Bad Request
        ├── 参数校验失败（语义错误）
        │   ├── 业务规则不允许 → 422 Unprocessable Entity ★
        │   └── 参数格式不对 → 400 Bad Request
        ├── 未认证（未登录） → 401 Unauthorized
        ├── 已认证但无权限 → 403 Forbidden
        ├── 资源不存在 → 404 Not Found
        ├── 资源冲突（重复创建） → 409 Conflict
        ├── 资源被移除（Gone） → 410 Gone
        ├── 请求频率过高 → 429 Too Many Requests
        └── 成功
            ├── GET → 200 OK + body
            ├── POST → 201 Created + Location header
            ├── PUT → 200 OK 或 204 No Content
            ├── PATCH → 200 OK
            └── DELETE → 204 No Content
```

> 🎯 关键原则：**不要在所有成功场景都用 200**。`201 Created` 和 `204 No Content` 提供额外语义，让调用方无需解析 body 即可知道结果。

### 1.4 常用状态码速查表

| 状态码 | 含义 | 典型场景 |
|--------|------|----------|
| `200 OK` | 请求成功 | GET、PUT、PATCH 返回数据 |
| `201 Created` | 创建成功 | POST 创建资源后返回 |
| `204 No Content` | 成功无返回体 | DELETE 删除成功、PUT 更新成功 |
| `301 Moved Permanently` | 永久重定向 | API 迁移到新 URL |
| `304 Not Modified` | 资源未变更 | 条件请求（ETag/If-Modified-Since） |
| `400 Bad Request` | 请求格式错误 | JSON 解析失败、参数类型错误 |
| `401 Unauthorized` | 未认证 | 未提供 JWT 或 JWT 过期 |
| `403 Forbidden` | 无权限 | 已认证但角色不够 |
| `404 Not Found` | 资源不存在 | URL 路径错误、ID 不存在 |
| `405 Method Not Allowed` | 方法不允许 | GET 请求了只支持 POST 的端点 |
| `409 Conflict` | 资源冲突 | 用户名已存在、版本冲突 |
| `410 Gone` | 资源已永久移除 | 已下架的旧版本资源 |
| `422 Unprocessable Entity` | 请求语义错误 | 参数校验失败（推荐使用） |
| `429 Too Many Requests` | 请求过多 | 触发限流 |
| `500 Internal Server Error` | 服务端内部错误 | 未捕获异常、数据库宕机 |
| `502 Bad Gateway` | 网关错误 | Nginx 无法连接上游服务 |
| `503 Service Unavailable` | 服务暂不可用 | 服务正在重启、熔断开启 |
| `504 Gateway Timeout` | 网关超时 | 上游服务响应超时 |

> 💡 `422 Unprocessable Entity` 和 `400 Bad Request` 的区别：400 表示请求**格式**本身无法解析（如 JSON 语法错误），422 表示请求格式正确但**语义上**不满足业务约束（如邮箱格式非法、金额为负数）。

---

## 2. 统一响应格式设计

### 2.1 ApiResponse 泛型封装

所有 API 响应使用统一的泛型包装，保证前端/客户端只需解析一个结构：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": "2026-07-26T12:00:00Z",
  "traceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;
    private String timestamp;
    private String traceId;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
            200, "success", data,
            Instant.now().toString(),
            MDC.get("traceId")
        );
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(
            code, message, null,
            Instant.now().toString(),
            MDC.get("traceId")
        );
    }
}
```

> 💡 `traceId` 用于分布式链路追踪，结合 MDC（Mapped Diagnostic Context）在请求入口处生成，贯穿整个调用链。出现问题时，前端提供 traceId 即可快速定位日志。

### 2.2 分页响应包装

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      { "id": 1, "name": "Alice" },
      { "id": 2, "name": "Bob" }
    ],
    "page": 1,
    "size": 20,
    "totalElements": 156,
    "totalPages": 8,
    "first": true,
    "last": false
  },
  "timestamp": "2026-07-26T12:00:00Z",
  "traceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

```java
@Data
@AllArgsConstructor
public class PageDTO<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
}
```

### 2.3 错误码枚举体系

```java
public enum ErrorCode {
    // 通用错误码 (1xxx)
    SUCCESS(200, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证，请先登录"),
    FORBIDDEN(403, "无权访问该资源"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    REQUEST_TIMEOUT(408, "请求超时"),
    CONFLICT(409, "资源冲突"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),

    // 业务错误码 (2xxx)
    USER_NOT_FOUND(2001, "用户不存在"),
    USER_PASSWORD_ERROR(2002, "密码错误"),
    USER_DISABLED(2003, "账户已被禁用"),
    USER_EXISTS(2004, "用户名已存在"),

    ORDER_NOT_FOUND(2101, "订单不存在"),
    ORDER_STATUS_INVALID(2102, "订单状态不允许操作"),
    ORDER_EXPIRED(2103, "订单已过期"),

    // 系统错误码 (5xxx)
    INTERNAL_ERROR(5000, "服务器内部错误"),
    SERVICE_UNAVAILABLE(5001, "服务暂不可用"),
    GATEWAY_TIMEOUT(5002, "网关超时"),
    DATABASE_ERROR(5003, "数据库操作失败"),
    THIRD_PARTY_ERROR(5004, "第三方服务调用失败");

    private final int code;
    private final String message;
}
```

---

## 3. API 版本管理策略

### 3.1 三种主流版本策略对比

| 策略 | 实现方式 | 示例 | 优点 | 缺点 |
|------|----------|------|------|------|
| **URL 路径版本** | URL 中带 `/v1/` | `GET /api/v1/users` | 最直观，浏览器/rcurl 直接测试，无需自定义 Header | URL 不纯净，版本扩散到所有 URL |
| **请求头版本** | `Accept` 头带版本号 | `Accept: application/vnd.myapi.v1+json` | URL 干净，符合 REST 哲学 | 浏览器/curl 默认不带，调试不友好 |
| **查询参数版本** | `?version=1` | `GET /api/users?version=1` | 容易测试，URL 不变 | 污染查询参数含义，缓存困难 |

### 3.2 推荐实践

**URL 路径版本（推荐）**：最直观，适合大多数团队。

```text
GET /api/v1/users
GET /api/v2/users
```

**媒体类型版本（进阶）**：适合 REST 纯化论者，或对外 API 平台。

```text
# 请求头
Accept: application/vnd.myapi.v1+json

# 响应头
Content-Type: application/vnd.myapi.v1+json
```

### 3.3 兼容性原则

| 兼容类型 | 允许操作 | 不允许操作 |
|----------|----------|------------|
| **向后兼容** | 新增字段（有默认值）、新增端点、放宽校验 | 删除字段、修改字段类型、修改端点 URL |
| **向后不兼容** | 需要开新版本 | — |

> ⚠️ 版本管理的最佳实践：**能不版本就不版本**。通过向后兼容的演进（仅新增字段），避免维护多套版本的成本。PayPal API 和 GitHub API 都用了这种方式。

---

## 4. 分页设计规范

### 4.1 Offset-based 分页（推荐默认）

```text
GET /api/v1/users?page=1&size=20&sort=createdAt,desc
```

| 参数 | 含义 | 默认值 | 说明 |
|------|------|--------|------|
| `page` | 页码（从 0 或 1 开始） | 1 | 建议从 1 开始，更符合直觉 |
| `size` | 每页条数 | 20 | 设置最大上限（如 100）防止滥用 |
| `sort` | 排序字段 | — | `field,direction` 格式 |

**Spring Boot 实现**：

```java
@GetMapping("/users")
public ApiResponse<PageDTO<UserVO>> listUsers(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size,
        Pageable pageable) {
    Page<User> userPage = userService.list(pageable);
    PageDTO<UserVO> dto = PageDTO.from(userPage.map(UserVO::from));
    return ApiResponse.success(dto);
}
```

### 4.2 Cursor-based 分页（适合大数据量/实时数据）

```text
GET /api/v1/messages?cursor=MTYyNzQwODAwMDAwMA&limit=20
```

| 特性 | Offset-based | Cursor-based |
|------|-------------|--------------|
| 性能（深分页） | 差（`OFFSET 100000` 扫描大量行） | 好（直接定位游标位置） |
| 数据一致性 | 插入/删除导致页码偏移 | 稳定，不受数据变更影响 |
| 随机跳页 | 支持 | 不支持（只能"上一页/下一页"） |
| 实现复杂度 | 低 | 高（需要编码/解码 cursor） |
| 适用场景 | 后台管理、数据量 < 10w | 实时 feed、无限滚动、大数据量 |

> 💡 **cursor 编码**：将 `id + 排序值` 组合 Base64 编码作为 cursor 传递，服务端解码后用 `WHERE (createdAt, id) > (?, ?)` 方式查询。

---

## 5. 异常处理与错误码体系

### 5.1 异常层次结构

```
RuntimeException
├── BusinessException          # 业务异常（如余额不足）
│   ├── UserNotFoundException
│   ├── InsufficientBalanceException
│   └── OrderStatusException
├── ValidationException        # 参数校验异常
├── AuthenticationException    # 认证异常
├── AuthorizationException     # 权限异常
└── ThirdPartyException        # 第三方服务异常
```

```java
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
```

### 5.2 全局异常处理（@ControllerAdvice）

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException e) {
        String messages = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ApiResponse.error(422, messages);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ApiResponse<Void> handleAccessDenied(AccessDeniedException e) {
        return ApiResponse.error(403, "无权访问该资源");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleUnknown(Exception e) {
        log.error("未捕获异常", e);
        return ApiResponse.error(5000, "服务器内部错误");
    }
}
```

### 5.3 参数校验（Bean Validation）

```java
@Data
public class CreateUserRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度需在3-50之间")
    private String username;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotNull(message = "年龄不能为空")
    @Min(value = 0, message = "年龄不能为负数")
    @Max(value = 150, message = "年龄不能超过150")
    private Integer age;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
```

---

## 6. API 安全防护

### 6.1 JWT 认证

**JWT 结构**：`header.payload.signature`

```text
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.
eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.
SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```

**请求方式**：

```text
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

| 要素 | 推荐配置 |
|------|----------|
| 签名算法 | HS256（对称）或 RS256（非对称，推荐） |
| 过期时间 | Access Token: 15-30 分钟；Refresh Token: 7-30 天 |
| 存储方式 | Access Token 存内存/HttpOnly Cookie；Refresh Token 存 HttpOnly Cookie |
| 关键声明 | `sub`(用户ID), `iat`(签发时间), `exp`(过期时间), `roles`(角色) |

> ⚠️ JWT 不要存 `localStorage` —— XSS 攻击可窃取。推荐 HttpOnly Cookie + CSRF Token 方案。

### 6.2 请求签名校验（HMAC）

适用于服务间通信（无需用户登录的场景），防止请求被篡改和重放：

```text
# 请求头
X-Timestamp: 1721980800000
X-Nonce: 7a8f3e2d-1b5c-4a6d-9e8f-0c1d2e3f4a5b
X-Signature: 3a8f1e2d4c5b6a7d8e9f0c1b2a3d4e5f6a7b8c9d
```

**签名生成算法**：

```java
public class SignUtil {
    public static String sign(String secretKey, String method, String path,
                              String body, long timestamp, String nonce) {
        String content = method + "\n" + path + "\n"
                + body + "\n" + timestamp + "\n" + nonce;
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(
                secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        byte[] hash = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
```

**防重放攻击**：服务端记录 `nonce`（30 分钟内不重复），或校验 `timestamp` 与服务器时间差不超过 5 分钟。

### 6.3 限流（Token Bucket 算法）

```java
@Component
public class RateLimiterAspect {
    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimit)")
    public Object limit(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        String key = rateLimit.key();
        RateLimiter limiter = limiters.computeIfAbsent(
                key, k -> RateLimiter.create(rateLimit.tokensPerSecond()));
        if (!limiter.tryAcquire(rateLimit.timeout(), TimeUnit.MILLISECONDS)) {
            throw new BusinessException(429, "请求过于频繁，请稍后重试");
        }
        return pjp.proceed();
    }
}
```

### 6.4 CORS 跨域配置

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("https://your-frontend.com")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

> ⚠️ 生产环境不要使用 `allowedOrigins("*")`，应明确指定允许的域名。

---

## 7. API 文档与 OpenAPI 规范

### 7.1 SpringDoc OpenAPI 3.0 注解

```java
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "用户管理", description = "用户的增删改查接口")
public class UserController {

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取用户", description = "获取单个用户的详细信息")
    @ApiResponse(responseCode = "200", description = "成功返回用户信息")
    @ApiResponse(responseCode = "404", description = "用户不存在")
    public ApiResponse<UserVO> getUser(
            @Parameter(description = "用户ID", required = true, example = "123")
            @PathVariable Long id) {
        return ApiResponse.success(userService.getById(id));
    }

    @PostMapping
    @Operation(summary = "创建用户")
    public ApiResponse<UserVO> createUser(
            @RequestBody @Valid CreateUserRequest request) {
        UserVO user = userService.create(request);
        return ApiResponse.success(user);
    }
}
```

### 7.2 Knife4j 增强配置

```yaml
# application.yml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html

knife4j:
  enable: true
  setting:
    language: zh-CN
    enable-footer: false
```

### 7.3 文档管理最佳实践

| 阶段 | 做法 | 工具/方法 |
|------|------|-----------|
| **设计阶段** | 先定义 OpenAPI 规范，再生成代码 | OpenAPI Spec → Swagger Codegen / OpenAPI Generator |
| **开发阶段** | 注解驱动，文档与代码同步 | SpringDoc + Knife4j |
| **测试阶段** | 通过 Swagger UI 直接调试接口 | Swagger UI / Postman / Insomnia |
| **发布阶段** | 导出静态文档给前端/测试 | Swagger 静态 HTML / Redoc |

---

## 8. 幂等性设计

### 8.1 什么是幂等

**幂等（Idempotent）**：同一个请求执行多次与执行一次的效果相同。

| HTTP 方法 | 天然幂等 | 说明 |
|-----------|----------|------|
| `GET` | 是 | 查询不会改变资源状态 |
| `PUT` | 是 | 全量更新，同一请求多次结果相同 |
| `DELETE` | 是 | 删除已删除的资源返回相同结果 |
| `POST` | **否** | 重复 POST 会创建多条记录 |
| `PATCH` | 取决于实现 | 部分更新，需自行保证幂等 |

### 8.2 POST 幂等实现方案

**方案一：幂等令牌（Idempotency-Key）**

```text
POST /api/v1/payments
Idempotency-Key: 7a8f3e2d-1b5c-4a6d-9e8f-0c1d2e3f4a5b
```

```java
public class PaymentService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public PaymentResult createPayment(PaymentRequest request, String idempotencyKey) {
        // 1. 检查幂等键是否已存在
        String key = "idempotency:" + idempotencyKey;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            return (PaymentResult) redisTemplate.opsForValue().get(key);
        }

        // 2. 执行创建逻辑
        PaymentResult result = doCreatePayment(request);

        // 3. 缓存结果（带过期时间，如 24 小时）
        redisTemplate.opsForValue().set(key, result, 24, TimeUnit.HOURS);

        return result;
    }
}
```

**方案二：数据库唯一约束**

```sql
CREATE TABLE payment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    biz_id VARCHAR(64) NOT NULL UNIQUE,  -- 业务幂等键
    created_at DATETIME NOT NULL,
    INDEX idx_biz_id (biz_id)
);
```

> 💡 **Stripe API 的做法**：Stripe 要求每个 POST 请求携带 `Idempotency-Key` 头，同个 Key 多次请求只生效一次，返回相同结果。这是对外 API 幂等设计的最佳参考。

---

## 9. HATEOAS 与超媒体驱动

### 9.1 概念

**HATEOAS**（Hypermedia As The Engine Of Application State）是 REST 的成熟度模型中的第 3 级（最高级）：

```
Level 0: 使用 HTTP 作为隧道（SOAP）
Level 1: 引入资源概念（多个端点）
Level 2: 使用正确的 HTTP 方法 + 状态码（REST 基础）
Level 3: 响应中包含超媒体链接（HATEOAS）
```

### 9.2 HATEOAS 示例

```json
GET /api/v1/orders/123

{
  "id": 123,
  "status": "pending",
  "amount": 99.99,
  "_links": {
    "self": { "href": "/api/v1/orders/123" },
    "pay": { "href": "/api/v1/orders/123/payments", "method": "POST" },
    "cancel": { "href": "/api/v1/orders/123", "method": "DELETE" },
    "customer": { "href": "/api/v1/users/456" }
  }
}
```

```java
@GetMapping("/{id}")
public EntityModel<OrderVO> getOrder(@PathVariable Long id) {
    OrderVO order = orderService.getById(id);
    return EntityModel.of(order,
        linkTo(methodOn(OrderController.class).getOrder(id)).withSelfRel(),
        linkTo(methodOn(PaymentController.class).payOrder(id)).withRel("pay"),
        linkTo(methodOn(OrderController.class).cancelOrder(id)).withRel("cancel"));
}
```

### 9.3 什么时候用

| 场景 | 推荐 |
|------|------|
| **对外公共 API** | 适合——客户端可动态发现操作，无需硬编码 URL |
| **内部微服务** | 不适合——增加复杂度，收益有限 |
| **简单 CRUD** | 不适合——过度设计 |
| **工作流/状态机** | 适合——不同状态有不同可用操作，链接自动引导 |

---

## 10. GraphQL vs REST 对比

### 10.1 核心差异

| 维度 | REST | GraphQL |
|------|------|---------|
| **数据获取** | 服务端固定返回结构，可能 over-fetching/under-fetching | 客户端精确指定所需字段 |
| **端点数量** | 每资源一个端点，N 个资源 N 个端点 | 单端点 `/graphql` |
| **版本管理** | URL/Header 版本 | 无需版本，通过 deprecated 演进 |
| **缓存** | 天然支持 HTTP 缓存（ETag, Cache-Control） | 需要自定义缓存方案 |
| **性能** | N+1 问题少（可通过 JOIN 优化） | N+1 问题突出（需要 DataLoader） |
| **学习曲线** | 低 | 中高 |
| **工具生态** | 成熟（Postman, Swagger, curl） | 较新（GraphiQL, Apollo） |
| **文件上传** | 原生支持（multipart/form-data） | 需额外扩展 |
| **批量操作** | 需设计批量端点 | 支持 mutation 中批量 |

### 10.2 选择指南

```
你的 API 服务于什么场景？
├── 第三方开发者 / 公共 API → REST（标准化、缓存友好、工具丰富）
├── 内部管理后台 / BFF 层
│   ├── 前端需求变化快，需要灵活数据 → GraphQL
│   └── 需求稳定，CRUD 为主 → REST
├── 移动端（省流量） → GraphQL（选择需要字段）
└── 实时数据 / 订阅 → GraphQL Subscription / WebSocket
```

---

## 11. 完整实战：用户管理 API 设计

### 11.1 接口总览

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| `POST` | `/api/v1/users` | 创建用户 | 公开（注册） |
| `GET` | `/api/v1/users` | 分页查询用户列表 | ADMIN |
| `GET` | `/api/v1/users/{id}` | 获取单个用户详情 | 本人/ADMIN |
| `PUT` | `/api/v1/users/{id}` | 全量更新用户信息 | 本人/ADMIN |
| `PATCH` | `/api/v1/users/{id}` | 部分更新用户信息 | 本人/ADMIN |
| `DELETE` | `/api/v1/users/{id}` | 删除用户 | ADMIN |
| `GET` | `/api/v1/users/{id}/profile` | 获取用户档案 | 本人/ADMIN |
| `PUT` | `/api/v1/users/{id}/password` | 修改密码 | 本人 |

### 11.2 请求与响应示例

**POST 创建用户**：

```http
POST /api/v1/users
Content-Type: application/json
Idempotency-Key: 7a8f3e2d-1b5c-4a6d

{
  "username": "alice_wang",
  "email": "alice@example.com",
  "password": "SecurePass123!",
  "nickname": "Alice",
  "phone": "13800138000",
  "age": 28
}
```

```http
HTTP/1.1 201 Created
Location: /api/v1/users/42
Content-Type: application/json

{
  "code": 200,
  "message": "success",
  "data": {
    "id": 42,
    "username": "alice_wang",
    "email": "alice@example.com",
    "nickname": "Alice",
    "phone": "13800138000",
    "age": 28,
    "status": "ACTIVE",
    "createdAt": "2026-07-26T12:00:00Z"
  },
  "timestamp": "2026-07-26T12:00:00Z",
  "traceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

**GET 分页查询**：

```http
GET /api/v1/users?page=1&size=20&sort=createdAt,desc&status=ACTIVE
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": 42,
        "username": "alice_wang",
        "email": "alice@example.com",
        "nickname": "Alice",
        "status": "ACTIVE",
        "createdAt": "2026-07-26T12:00:00Z"
      }
    ],
    "page": 1,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  },
  "timestamp": "2026-07-26T12:00:00Z",
  "traceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

**PATCH 部分更新**：

```http
PATCH /api/v1/users/42
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...

{
  "nickname": "Alice Wang",
  "phone": "13900139000"
}
```

**DELETE 删除用户**：

```http
DELETE /api/v1/users/42
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

```http
HTTP/1.1 204 No Content
```

**错误响应示例**：

```http
HTTP/1.1 422 Unprocessable Entity

{
  "code": 422,
  "message": "email: 邮箱格式不正确; username: 用户名长度需在3-50之间",
  "data": null,
  "timestamp": "2026-07-26T12:00:00Z",
  "traceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

### 11.3 Controller 完整实现

```java
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "用户管理", description = "用户的增删改查接口")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(summary = "创建用户")
    public ResponseEntity<ApiResponse<UserVO>> createUser(
            @RequestBody @Valid CreateUserRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        UserVO user = userService.create(request, idempotencyKey);
        URI location = URI.create("/api/v1/users/" + user.getId());
        return ResponseEntity.created(location)
                .body(ApiResponse.success(user));
    }

    @GetMapping
    @Operation(summary = "分页查询用户列表")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageDTO<UserVO>> listUsers(
            @Valid @ModelAttribute UserQuery query) {
        return ApiResponse.success(userService.list(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取用户详情")
    public ApiResponse<UserVO> getUser(@PathVariable Long id) {
        return ApiResponse.success(userService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "全量更新用户")
    public ApiResponse<UserVO> updateUser(
            @PathVariable Long id,
            @RequestBody @Valid UpdateUserRequest request) {
        return ApiResponse.success(userService.update(id, request));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "部分更新用户")
    public ApiResponse<UserVO> patchUser(
            @PathVariable Long id,
            @RequestBody Map<String, Object> fields) {
        return ApiResponse.success(userService.patch(id, fields));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/password")
    @Operation(summary = "修改密码")
    public ApiResponse<Void> changePassword(
            @PathVariable Long id,
            @RequestBody @Valid ChangePasswordRequest request) {
        userService.changePassword(id, request);
        return ApiResponse.success(null);
    }

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleUserNotFound(UserNotFoundException e) {
        return ApiResponse.error(404, e.getMessage());
    }
}
```

---

## 12. 业界参考：Stripe 与 GitHub 的 API 设计哲学

### 12.1 Stripe API （支付领域标杆）

Stripe API 被公认为 RESTful API 设计的黄金标准：

```http
# 创建支付意图（幂等）
POST /v1/payment_intents
Idempotency-Key: 7a8f3e2d-1b5c-4a6d
Authorization: Bearer sk_test_...

{
  "amount": 2000,
  "currency": "usd",
  "payment_method_types": ["card"],
  "metadata": {"order_id": "123"}
}

# 响应中的分页（cursor-based）
GET /v1/payment_intents?limit=10&starting_after=pi_abc123
```

**Stripe 设计原则**：

| 原则 | 说明 |
|------|------|
| **错误码字段丰富** | 每个错误有 `type`、`code`、`param`、`detail`，便于程序化处理 |
| **必填幂等键** | 所有 POST 请求都必须传 `Idempotency-Key` |
| **扩展字段通过 metadata** | 避免频繁 Schema 变更 |
| **递增字段，不删不改** | 向后兼容，不轻易开新版本 |
| **列表分页默认 cursor-based** | 使用 `starting_after` / `ending_before` |

### 12.2 GitHub API （协作平台标杆）

```http
# REST API
GET /api/v3/repos/octocat/Hello-World/issues

# GraphQL API
POST /api/graphql
Authorization: bearer ghp_...
{
  "query": "query { repository(owner: \"octocat\", name: \"Hello-World\") { issues(first: 10) { nodes { title } } } }"
}
```

**GitHub 设计原则**：

| 原则 | 说明 |
|------|------|
| **预览期新功能** | 通过 `Accept` 头中的自定义媒体类型控制 |
| **分页头部** | 使用 `Link` 头中的 `rel="next"` / `rel="last"`，而非 body 中的分页字段 |
| **条件请求** | 使用 `ETag` + `If-None-Match` 实现缓存，返回 `304 Not Modified` 节省带宽 |
| **限流信息在响应头** | `X-RateLimit-Limit`、`X-RateLimit-Remaining`、`X-RateLimit-Reset` |
| **两套 API** | REST（通用）+ GraphQL（灵活），让用户按需选择 |

> 🎯 **核心启示**：好的 API 设计不是功能堆砌，而是定义清晰的**契约**——让调用方在无需阅读文档的情况下，通过 HTTP 语义、状态码和响应结构就能正确使用你的 API。

---

> **参考资源**
> - [Microsoft REST API Guidelines](https://github.com/microsoft/api-guidelines)
> - [Google API Design Guide](https://cloud.google.com/apis/design)
> - [JSON:API Specification](https://jsonapi.org/)
> - [OpenAPI 3.0 Specification](https://spec.openapis.org/oas/v3.0.3)
