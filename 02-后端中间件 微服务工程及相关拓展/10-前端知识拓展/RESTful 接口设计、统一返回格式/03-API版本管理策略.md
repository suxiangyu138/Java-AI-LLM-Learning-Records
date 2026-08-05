# 03 - API 版本管理策略

> 🎯 API 一定会变 — 如何让老客户端不崩溃、新客户端用上最新功能，版本管理是 API 设计的必修课

---

## 目录

1. [四种版本管理方式](#1-四种版本管理方式)
2. [不兼容变更处理](#2-不兼容变更处理)
3. [版本生命周期](#3-版本生命周期)
4. [Spring Boot 版本管理实现](#4-spring-boot-版本管理实现)

---

## 1. 四种版本管理方式

| 方式 | 示例 | 优点 | 缺点 | 推荐 |
|------|------|------|------|:---:|
| **URL 路径** | `/api/v1/users` | 直观、缓存友好 | URL 中有版本 | ⭐ 最常用 |
| **请求头** | `Accept: application/vnd.api.v1+json` | URL 干净 | 不直观、调试不便 | ⭐ |
| **查询参数** | `/api/users?version=1` | 简单 | 污染 URL | ❌ |
| **域名** | `v1.api.example.com` | 完全隔离 | 运维复杂 | 大版本 |

### URL 路径版本（⭐ 推荐）

```text
/api/v1/users          ← 版本 1
/api/v2/users          ← 版本 2（可能返回结构不同）

规则：
  → 仅不兼容变更才升大版本（v1 → v2）
  → 向后兼容的变更不加新版本
  → 最多维护 2 个活跃版本（当前 + 上一版）
```

---

## 2. 不兼容变更处理

| 变更类型 | 是否兼容 | 需要升版本 |
|----------|:---:|:---:|
| 新增接口 | ✅ | ❌ |
| 新增可选字段（请求/响应） | ✅ | ❌ |
| 新增必填字段（请求） | ❌ | ✅ |
| 删除字段（响应） | ❌ | ✅ |
| 修改字段类型 | ❌ | ✅ |
| 修改 URL 路径 | ❌ | ✅ |

```text
例子：用户接口 v1 → v2

v1: GET /api/v1/users/1
  { "id": 1, "name": "张三", "phone": "13800138000" }

v2: GET /api/v2/users/1
  { "id": 1, "name": "张三",
    "phone": { "number": "13800138000", "verified": true },  ← 字段类型变了！
    "avatar": "https://..." }                                 ← 新增字段

→ 手机号从 String 变成 Object → 不兼容 → 必须升 v2
```

---

## 3. 版本生命周期

```text
每个版本的生命周期：

  [开发] → [发布] → [维护] → [废弃通知] → [下线]

时间线：
  v2 发布后 → v1 进入维护期（只修 Bug，不加功能）
  v3 发布后 → v1 标记废弃（响应头加 Deprecation: true + Sunset: date）
  v1 下线 → 提前至少 1 个月通知
```

```java
// 废弃提示 — 响应头方式
@GetMapping("/v1/users")
public ResponseEntity<List<User>> listV1(HttpServletResponse response) {
    response.setHeader("Deprecation", "true");
    response.setHeader("Sunset", "Sat, 01 Mar 2025 00:00:00 GMT");
    response.setHeader("Link", "</api/v2/users>; rel=\"successor-version\"");
    return ResponseEntity.ok(userService.list());
}
```

---

## 4. Spring Boot 版本管理实现

```java
// ⭐ 推荐：按版本分包
// controller/
//   v1/
//     UserController.java
//   v2/
//     UserController.java

@RestController
@RequestMapping("/api/v1/users")
public class UserControllerV1 { ... }

@RestController
@RequestMapping("/api/v2/users")
public class UserControllerV2 { ... }
```

```java
// 版本间复用 Service 层（Controller 不同，Service 共享）
@RestController
@RequestMapping("/api/v2/users")
public class UserControllerV2 {
    private final UserService userService;  // ← 与 v1 共享

    @GetMapping("/{id}")
    public UserV2DTO getUser(@PathVariable Long id) {
        User user = userService.getById(id);    // ← 复用核心逻辑
        return UserV2DTO.from(user);            // ← 返回新版本 DTO
    }
}
```

```java
// 自定义注解 + 拦截器（Header 版本方式）
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiVersion {
    int value();
}

@ApiVersion(1)
@GetMapping("/users")
public List<UserV1DTO> listV1() { ... }

@ApiVersion(2)
@GetMapping("/users")
public List<UserV2DTO> listV2() { ... }

// HandlerMapping 根据请求头 Accept: application/vnd.api.v2+json 路由
```

> 🎯 **版本管理铁律**：新增字段不升版本、删除/改类型必升版本、最多维护 2 个活跃版、废弃提前 1 月通知、响应头加 Sunset。
