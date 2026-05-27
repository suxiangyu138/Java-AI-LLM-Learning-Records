# RESTful API 设计规范与实践

## 核心概念

REST (Representational State Transfer) 是一种面向资源的 API 设计风格，不是协议。

**六大约束：**
- **客户端-服务器**：前后端分离，各司其职
- **无状态**：每个请求包含所有需要的信息，服务器不保存客户端状态
- **可缓存**：响应应显式标注是否可缓存
- **统一接口**：资源的标识、操作、自描述消息、HATEOAS
- **分层系统**：客户端不知道是直连服务器还是经过中间层
- **按需代码**（可选）：服务器可下发可执行代码给客户端

## URL 设计规范

```
# 资源用名词复数
GET    /api/users          # 获取用户列表
GET    /api/users/{id}     # 获取单个用户
POST   /api/users          # 创建用户
PUT    /api/users/{id}     # 全量更新用户
PATCH  /api/users/{id}     # 部分更新用户
DELETE /api/users/{id}     # 删除用户

# 关联资源
GET    /api/users/{id}/orders        # 某用户的所有订单
GET    /api/users/{id}/orders/{oid}  # 某用户的某个订单
```

**命名规则：**
- 全部小写，单词用 `-` 连接：`/api/order-items`
- 不要用动词：`/getUser` → 用 `GET /users/{id}`
- 不要暴露数据库表名
- 层级不宜超过 3 层
- 查询参数用于过滤、排序、分页：`/api/users?role=admin&sort=name&page=1&size=20`

## HTTP 方法

| 方法 | 语义 | 幂等 | 安全 |
|------|------|------|------|
| GET | 查询 | 是 | 是 |
| POST | 创建 | 否 | 否 |
| PUT | 全量替换 | 是 | 否 |
| PATCH | 部分更新 | 否 | 否 |
| DELETE | 删除 | 是 | 否 |

## HTTP 状态码

**常用组合：**
- `200 OK` — GET/PUT 成功
- `201 Created` — POST 创建成功，响应头带 `Location`
- `204 No Content` — DELETE 成功，无响应体
- `400 Bad Request` — 参数校验失败
- `401 Unauthorized` — 未认证（没登录）
- `403 Forbidden` — 无权限（已登录但权限不够）
- `404 Not Found` — 资源不存在
- `409 Conflict` — 资源冲突（如重复创建）
- `422 Unprocessable Entity` — 参数格式正确但语义错误
- `500 Internal Server Error` — 服务端未知异常

## 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": 1715184000000
}
```

**分页响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [...],
    "total": 100,
    "page": 1,
    "size": 20,
    "pages": 5
  }
}
```

## 版本管理

三种常见方式：
- **URL 路径**：`/api/v1/users`（最直观）
- **请求头**：`Accept: application/vnd.api.v1+json`
- **查询参数**：`/api/users?version=1`

推荐 URL 路径方式，对调用方最友好。

## 常见问题

**批量操作：**
```
POST /api/users/batch-delete
Body: { "ids": [1, 2, 3] }
```
当确实无法映射为资源操作时，允许动词后缀。

**搜索接口：**
```
GET /api/users?keyword=张三&status=active
```
复杂搜索可用 `POST /api/users/search` + body 传条件。

**登录/登出：**
```
POST /api/auth/login
POST /api/auth/logout
POST /api/auth/refresh-token
```
Auth 不是 CRUD 资源，用动词更合理。

## Spring Boot 实践要点

- Controller 加 `@RestController`
- `@RequestMapping("/api/v1/users")` 做版本前缀
- `@GetMapping("/{id}")`、`@PostMapping` 等明确 HTTP 方法
- `@RequestBody` 接收 JSON，`@PathVariable` 取路径参数
- `@RequestParam` 取查询参数，设好 `required` 和 `defaultValue`
- `@Valid` / `@Validated` 做参数校验
- 统一异常处理 `@RestControllerAdvice`
