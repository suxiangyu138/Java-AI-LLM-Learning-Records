# 03 - HTTP 状态码完全指南

> 🎯 状态码不只是数字 — 2xx 成功、3xx 重定向、4xx 客户端错误、5xx 服务端错误，每种状态码对应一种排查思路

---

## 目录

1. [状态码分类速查](#1-状态码分类速查)
2. [2xx 成功状态码](#2-2xx-成功状态码)
3. [3xx 重定向状态码](#3-3xx-重定向状态码)
4. [4xx 客户端错误排查](#4-4xx-客户端错误排查)
5. [5xx 服务端错误排查](#5-5xx-服务端错误排查)

---

## 1. 状态码分类速查

| 范围 | 类型 | 说明 | 后端关注 |
|:---:|------|------|:---:|
| **1xx** | 信息 | 请求已接收，继续处理 | ⭐ |
| **2xx** | 成功 | 请求成功处理 | ⭐⭐ |
| **3xx** | 重定向 | 需要进一步操作 | ⭐⭐ |
| **4xx** | 客户端错误 | 请求有误 | ⭐⭐⭐ |
| **5xx** | 服务端错误 | 服务器处理失败 | ⭐⭐⭐ |

---

## 2. 2xx 成功状态码

| 状态码 | 含义 | 场景 |
|:---:|------|------|
| **200 OK** | 请求成功 | GET/PUT 返回数据 |
| **201 Created** | 资源已创建 | POST 创建返回新资源 Location |
| **204 No Content** | 成功但无返回体 | DELETE 成功 |
| **206 Partial Content** | 部分内容 | 断点续传、分片下载（Range 头） |

```java
// Spring Boot 返回 201
@PostMapping("/users")
public ResponseEntity<User> create(@RequestBody User user) {
    User created = userService.create(user);
    URI location = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{id}").buildAndExpand(created.getId()).toUri();
    return ResponseEntity.created(location).body(created);
    // → 201 Created + Location: /users/1
}
```

---

## 3. 3xx 重定向状态码

| 状态码 | 含义 | 行为 | 缓存 |
|:---:|------|------|:---:|
| **301** | 永久重定向 | 浏览器记住，下次直接跳 | ✅ 默认缓存 |
| **302** | 临时重定向 | 每次询问 | ❌ |
| **304** | 未修改 | 用缓存，不发新数据 | — |

```nginx
# Nginx 301 重定向（HTTP → HTTPS）
server {
    listen 80;
    return 301 https://$host$request_uri;
}
```

```java
// Spring Boot 重定向
@GetMapping("/old-path")
public ResponseEntity<Void> redirect() {
    return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
        .location(URI.create("/new-path"))
        .build();
}
```

---

## 4. 4xx 客户端错误排查

| 状态码 | 含义 | 原因 | 排查 |
|:---:|------|------|------|
| **400** | 请求错误 | 参数格式不对/JSON 解析失败 | 检查请求体格式 |
| **401** | 未认证 | Token 缺失/过期 | 检查 Authorization 头 |
| **403** | 禁止访问 | 无权限 | 检查角色/权限 |
| **404** | 未找到 | URL 路径错误/资源不存在 | 检查路径映射 |
| **405** | 方法不允许 | GET 调了 POST 接口 | 检查请求方法 |
| **408** | 请求超时 | 请求体发送太慢 | 检查网络/调整超时 |
| **413** | 请求体过大 | 上传文件超过限制 | 调大 max-size |
| **415** | 不支持的媒体类型 | Content-Type 不对 | 检查 `Content-Type: application/json` |
| **429** | 请求过多 | 触发了限流 | 等待后重试 |

```java
// Spring Boot 全局异常 → 状态码映射
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidation() { return Result.fail(400, "参数校验失败"); }

    @ExceptionHandler(AccessDeniedException.class)
    public Result<?> handleAccessDenied() { return Result.fail(403, "无权限"); }

    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<?> handle404() { return Result.fail(404, "接口不存在"); }
}
```

---

## 5. 5xx 服务端错误排查

| 状态码 | 含义 | 常见原因 | 排查方向 |
|:---:|------|----------|----------|
| **500** | 服务器内部错误 | 代码异常/空指针 | 查服务日志 |
| **502** | 网关错误 | 上游服务不可达 | `ss -tlnp` 查端口 |
| **503** | 服务不可用 | 服务过载/所有实例 Down | 查健康检查 |
| **504** | 网关超时 | 上游服务响应太慢 | 查慢 SQL/慢接口 |

```text
502 vs 503 vs 504 快速区分：

502 Bad Gateway：
  Nginx 找不到后端服务 → 端口没监听 / 服务挂了
  → ss -tlnp | grep 8080、查服务状态

503 Service Unavailable：
  后端服务在，但挂了"休息中"的牌子 → 限流 / 熔断 / 线程池满
  → 查 Sentinel 规则、查线程池、查连接池

504 Gateway Timeout：
  后端服务在，但处理太慢 → 慢 SQL / 慢接口 / 死锁
  → 查慢查询日志、查接口 RT、查数据库锁
```

### 排查流程

```bash
# 502 排查
ss -tlnp | grep 8080           # 服务端口是否监听
systemctl status myapp          # 服务是否运行
tail -100 /var/log/myapp.log   # 服务日志

# 504 排查
# 检查上游服务 RT（Nginx access log 的 $upstream_response_time）
tail -f /var/log/nginx/access.log | awk '{print $NF}'
# 查慢 SQL
grep "slow" /var/log/mysql/slow.log
```

> 🎯 **5 秒定位**：400 看参数、401 看 Token、403 看权限、404 看路径、500 看日志、502 看端口、503 看负载、504 看耗时。
