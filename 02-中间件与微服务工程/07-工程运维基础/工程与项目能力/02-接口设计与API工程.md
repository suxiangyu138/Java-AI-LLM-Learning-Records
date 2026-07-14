# 接口设计与 API 工程

## 📌 定位
**课外自学 | 第一~二梯队 | 后端协作核心能力**

接口是后端与前端、后端与后端之间的"合同"——设计得好，协作顺畅；设计得差，天天扯皮。

## 🎯 核心章节

### 1. RESTful API 设计规范（⭐ 核心）
- **URL 设计**：资源名用名词复数——`GET /users`、`GET /users/123`、`POST /users`
- **HTTP 方法语义**：
  - GET：获取（幂等，安全——不应修改资源）
  - POST：创建（非幂等）
  - PUT：全量更新（幂等）
  - PATCH：部分更新
  - DELETE：删除（幂等）
- **状态码的正确使用**：
  - 200 OK(常规成功)、201 Created(创建成功)、204 No Content(删除成功)
  - 400 Bad Request(参数错误)、401 Unauthorized(未认证)、403 Forbidden(无权限)、404 Not Found
  - 422 Unprocessable Entity(请求格式正确但语义错误——推荐用于参数校验失败)
  - 500 Internal Server Error(服务端错误)

### 2. 统一响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": 1716902400,
  "traceId": "abc-123"
}
```
- **必要性**：前端/客户端统一处理——只判断code，不同值走不同逻辑

### 3. 接口文档
- **Swagger / OpenAPI 3.0**：注解生成文档 `@ApiOperation/@ApiParam`——同步即文档
- **Knife4j（Java增强版Swagger）**：UI更友好
- **文档即规范**：先定义接口文档→前后端各自开发→联调验证

### 4. 接口安全
- **认证**：JWT在Authorization头——`Bearer eyJhbG...`
- **防篡改**：请求参数加签(HMAC-SHA256+timestamp+nonce防重放)
- **限流**：接口级(登录1分钟5次)+用户级——Sentinel/Guava RateLimiter

### 5. API 版本管理
- **URL版本**：`/api/v1/users`——最直观
- **Header版本**：`Accept: application/vnd.myapi.v1+json`——URL干净
- **兼容性原则**：新增字段兼容旧版(不删不改已有字段)

### 6. GraphQL（了解）
- **优点**：前端决定要哪些字段→避免过度/不足获取
- **缺点**：复杂度集中在服务端(N+1问题)、缓存困难
- **适用场景**：前端需求多变、移动端省流量

## ✅ 学习建议
- 接口规范是团队协作的基础——建议从项目初期就定好规范
- 用Swagger自动生成文档——省时省力且不落后
- 参考GitHub API / Stripe API——业界最佳实践的接口设计
