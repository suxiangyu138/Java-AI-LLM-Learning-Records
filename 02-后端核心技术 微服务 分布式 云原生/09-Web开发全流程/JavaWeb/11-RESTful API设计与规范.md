# 11 - RESTful API 设计与规范

> RESTful API 不是"把 URL 写对就行了"——资源命名、状态码使用、版本管理、HATEOAS，每一个细节都决定了 API 的可维护性和开发体验。

---

## 目录

1. [REST 核心原则](#1-rest-核心原则)
2. [URL 设计规范](#2-url-设计规范)
3. [状态码与错误处理](#3-状态码与错误处理)
4. [API 版本管理](#4-api-版本管理)

---

## 1. REST 核心原则

```text
REST = Representational State Transfer

六大约束：
1. 客户端-服务器：分离关注点，独立演进
2. 无状态：每个请求包含所有必要信息
3. 可缓存：响应明确标记是否可缓存
4. 统一接口：资源标识 + 表现层操作 + 自描述消息
5. 分层系统：中间层透明（代理/网关/缓存）
6. 按需代码（可选）：下载并执行客户端脚本

富婆原则（Richardson Maturity Model）：
  Level 0: 单一 URI + 单一方法（RPC 风格）
  Level 1: 资源 URI（每个资源有独立地址）
  Level 2: HTTP 动词（GET/POST/PUT/DELETE 语义正确）← 大多数 API 到这里
  Level 3: HATEOAS（响应包含下一步链接）
```

## 2. URL 设计规范

```text
✅ 好的 URL 设计：
  GET     /api/v1/users              # 获取用户列表
  GET     /api/v1/users/123          # 获取单个用户
  GET     /api/v1/users/123/orders   # 获取用户订单（嵌套资源）
  POST    /api/v1/users              # 创建用户
  PUT     /api/v1/users/123          # 完整更新
  PATCH   /api/v1/users/123          # 部分更新
  DELETE  /api/v1/users/123          # 删除用户

❌ 差的 URL 设计：
  GET  /api/getUserList              # 动词在 URL 中
  POST /api/createUser               # 同上
  GET  /api/users?id=123             # 应该用路径参数
```

### 查询参数规范

```text
过滤:    GET /users?status=active&role=admin
排序:    GET /users?sort=created_at&order=desc
分页:    GET /users?page=2&size=20
字段:    GET /users?fields=id,name,email
搜索:    GET /users?q=john
```

```java
// Spring Boot 实现
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @GetMapping
    public Page<UserResponse> list(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String status
    ) {
        return userService.findPage(page, size, status);
    }
}
```

## 3. 状态码与错误处理

```text
标准状态码速查：
  200 OK            → GET/PUT 成功
  201 Created       → POST 创建成功（带上 Location header）
  204 No Content    → DELETE 成功（无 body）
  400 Bad Request   → 参数错误
  401 Unauthorized  → 未认证（需要登录）
  403 Forbidden     → 无权限（已登录但没权限）
  404 Not Found     → 资源不存在
  409 Conflict      → 资源冲突（重复创建等）
  422 Unprocessable → 语义错误（参数格式对但内容不对）
  429 Too Many      → 限流
  500 Internal Error → 服务器错误（不要暴露细节）
```

```java
// 统一错误响应格式
public class ApiError {
    private int status;
    private String error;
    private String message;
    private String path;
    private Instant timestamp;

    // 工厂方法
    public static ApiError of(HttpStatus status, String message, String path) {
        return new ApiError(status.value(), status.getReasonPhrase(), message, path, Instant.now());
    }
}

// 全局异常处理
@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ApiError.of(HttpStatus.BAD_REQUEST, msg, ""));
    }
}
```

## 4. API 版本管理

```text
四种版本策略：

1. URL 路径（最常用）：
   /api/v1/users  →  /api/v2/users

2. 请求头：
   Accept: application/vnd.company.v2+json

3. 查询参数：
   /api/users?version=2

4. 域名：
   v1.api.example.com / v2.api.example.com

推荐：URL 路径版本（最直观、最易调试、缓存友好）
```

```text
API 文档：
  → Spring REST Docs（测试驱动、自动生成）
  → Swagger/OpenAPI 3.0（交互式文档）
  → 文档即代码 → 永远不会和实现脱节

HATEOAS（超媒体引擎）：
  {
    "id": 123,
    "name": "Alice",
    "_links": {
      "self": "/users/123",
      "orders": "/users/123/orders",
      "update": "/users/123",
      "delete": "/users/123"
    }
  }
  → 客户端可以动态发现下一步操作
```

## 核心要点回顾

- URL 用名词复数 + HTTP 动词组合（不用动词在 URL）
- 201 创建成功 → 返回 `Location` header
- 错误统一格式：status + error + message + timestamp
- 版本：URL 路径（`/v1`）最简单有效
- 文档：Spring REST Docs > Swagger（文档不脱节）

## 参考资料

1. REST API 设计规范 — Microsoft / Google API Design Guide
2. Richardson Maturity Model (Martin Fowler)
3. Spring HATEOAS 官方文档
