# 01 - RESTful 设计原则与 URL 规范

> 🎯 URL 是 API 的"门面" — 好的 URL 一眼能看懂做什么，坏的 URL 需要文档才能猜

---

## 目录

1. [REST 六大约束](#1-rest-六大约束)
2. [URL 命名规范](#2-url-命名规范)
3. [资源层级关系](#3-资源层级关系)
4. [过滤、排序、分页](#4-过滤排序分页)

---

## 1. REST 六大约束

| 约束 | 说明 | 实践 |
|------|------|------|
| **客户端-服务端** | 前后端分离，独立演进 | 前端独立部署 + API 解耦 |
| **无状态** | 每个请求包含全部信息 | JWT Token（非 Session） |
| **可缓存** | 响应需声明可否缓存 | `Cache-Control` / `ETag` |
| **统一接口** | URL + HTTP 方法 + 状态码统一 | 本文核心 |
| **分层系统** | 中间层透明代理 | Nginx → Gateway → 微服务 |
| **按需代码** | 服务端可下发可执行代码（可选） | 极少用 |

---

## 2. URL 命名规范

### ✅ 推荐 vs ❌ 避免

```text
✅ 资源用名词复数：
  GET    /api/users           ← 用户列表
  GET    /api/users/1         ← 单个用户
  POST   /api/users           ← 创建用户

❌ URL 中不要出现动词：
  GET    /api/getUsers
  POST   /api/createUser
  GET    /api/queryUser?id=1

✅ 用小写 + 连字符：
  /api/user-orders            ← ✅
  /api/userOrders             ← ❌ 驼峰
  /api/user_orders            ← ❌ 下划线

✅ 不要暴露技术细节：
  /api/users                  ← ✅
  /api/userServlet/query      ← ❌ 暴露 Servlet
  /api/users.php?id=1         ← ❌ 暴露扩展名
```

| 规则 | ✅ | ❌ |
|------|----|----|
| 名词复数 | `/users` | `/user`, `/getUser` |
| 小写连字符 | `/order-items` | `/OrderItems` |
| 无扩展名 | `/users` | `/users.json` |
| 无动词 | `POST /users` | `/createUser` |
| 层级清晰 | `/users/1/orders` | `/getOrders?userId=1` |

---

## 3. 资源层级关系

```text
基础 CRUD：
  GET    /users              # 用户列表
  POST   /users              # 创建用户（Body 带数据）
  GET    /users/1            # 用户详情
  PUT    /users/1            # 全量更新
  PATCH  /users/1            # 部分更新
  DELETE /users/1            # 删除用户

嵌套资源（最多 2 层！）：
  GET    /users/1/orders           # 用户1的所有订单
  GET    /users/1/orders/100       # 用户1的订单100
  POST   /users/1/orders           # 用户1创建订单

非 CRUD 操作（用动词后缀）：
  POST   /orders/100/cancel        # 取消订单
  POST   /orders/100/refund        # 退款
  POST   /accounts/1/activate      # 激活账户
```

> ⚠️ **嵌套不超过 2 层**：`/a/1/b/2/c/3` 太难维护。超过 2 层独立请求：`/c?bId=2`。

---

## 4. 过滤、排序、分页

```text
过滤（Filter）：
  GET /users?status=active               # 精确匹配
  GET /users?age[gte]=18&age[lte]=60     # 范围（约定好操作符）
  GET /users?keyword=张三                 # 模糊搜索

排序（Sort）：
  GET /users?sort=created_at             # 升序
  GET /users?sort=-created_at            # 降序（- 前缀）
  GET /users?sort=age,-created_at        # 多字段

分页（Pagination）：
  GET /users?page=1&size=20              # 分页（推荐）
  GET /users?offset=0&limit=20           # 偏移量

字段选择（Field Selection）：
  GET /users/1?fields=id,name,email      # 仅返回指定字段（可选）
```

### 统一参数命名约定

| 参数 | 推荐 | 备选 | 说明 |
|------|------|------|------|
| 分页 | `page` + `size` | `offset` + `limit` | 推荐 page/size |
| 排序 | `sort=-createdAt` | `orderBy=desc` | — |
| 搜索 | `q` 或 `keyword` | `search` | — |

```java
// Spring Boot 分页
@GetMapping("/users")
public Page<User> list(
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(required = false) String keyword) {
    Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
    return userService.findAll(keyword, pageable);
}
```

> 🎯 **URL 设计口诀**：名词复数、小写连字符、嵌套最多两层、非 CRUD 操作加动词后缀、分页用 page/size、排序用 sort=-field。遵守这些规范的 API 不看文档也能猜出怎么用。
