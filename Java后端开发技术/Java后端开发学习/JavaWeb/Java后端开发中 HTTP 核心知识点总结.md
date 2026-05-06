# Java 后端开发中 HTTP 核心知识点总结

> **文档定位**：Java 后端企业级技术文档 | HTTP 协议  
> **核心特点**：无状态、请求-响应模型、基于 TCP/IP

---

## 一、核心概念

### 1.1 HTTP 定义

HTTP（HyperText Transfer Protocol）是基于 TCP/IP 的无状态应用层协议，用于客户端与服务器之间的通信。

### 1.2 核心特点

| 特点 | 说明 |
|------|------|
| **无状态** | 每次请求独立，默认不保存会话 |
| **无连接** | HTTP 1.0 每次请求建立新连接；1.1 支持 `Keep-Alive` 长连接 |

### 1.3 版本演进

| 版本 | 核心特性 |
|------|----------|
| **HTTP 1.1**（主流） | 长连接、管道化请求 |
| **HTTP 2** | 多路复用、二进制帧、头部压缩 |
| **HTTP 3** | 基于 UDP 的 QUIC 协议 |

---

## 二、底层原理

### 2.1 HTTP 请求报文结构

```
请求行：GET /user/1 HTTP/1.1
请求头：Host: api.example.com
       User-Agent: ...
       Content-Type: application/json
       Cookie: ...
空行：
请求体：（POST/PUT 时的参数数据）
```

**常用请求方法**：`GET`（查询）、`POST`（提交）、`PUT`（更新）、`DELETE`（删除）、`HEAD`（仅获取响应头）

### 2.2 HTTP 响应报文结构

```
状态行：HTTP/1.1 200 OK
响应头：Content-Type: application/json
       Set-Cookie: ...
       Cache-Control: max-age=3600
空行：
响应体：{"id":1, "name":"张三"}
```

**状态码分类**：

| 范围 | 含义 | 示例 |
|------|------|------|
| 1xx | 提示信息 | 100 Continue |
| 2xx | **成功** | 200 OK、201 Created |
| 3xx | 重定向 | 301 永久、302 临时、304 缓存 |
| 4xx | **客户端错误** | 400 参数错误、401 未授权、403 禁止、404 不存在 |
| 5xx | **服务器错误** | 500 内部异常、503 不可用 |

---

## 三、代码实现

### 3.1 Servlet 处理 HTTP

```java
@WebServlet("/hello")
public class HelloServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain;charset=utf-8");
        resp.getWriter().write("Hello HTTP!");
    }
}
```

**核心 API**：`req.getMethod()`、`req.getParameter()`、`resp.setStatus()`、`resp.getWriter().write()`

### 3.2 Spring MVC 处理 HTTP

```java
@RestController
public class UserController {

    @GetMapping("/user/{id}")
    public String getUserById(@PathVariable Integer id) {
        return "User ID: " + id;
    }

    @PostMapping("/user")
    public User createUser(@RequestBody User user) {
        return userService.save(user);
    }
}
```

**核心注解**：`@RequestMapping`、`@GetMapping/@PostMapping`、`@RequestParam`、`@RequestBody`、`@ResponseBody`

---

## 四、实战要点

| 要点 | 说明 |
|------|------|
| **Content-Type** | JSON 用 `application/json`，表单用 `application/x-www-form-urlencoded` |
| **Cache-Control** | `max-age=3600` 静态资源缓存，`no-cache` 禁止缓存 |
| **Cookie vs Token** | Cookie 自动携带但跨域受限，Token 手动携带但更灵活 |

---

## 五、避坑总结

| 坑点 | 正确做法 |
|------|----------|
| **GET 请求体被忽略** | GET 参数只能在 URL 中 |
| **中文乱码** | 设置 `charset=utf-8` |
| **请求体重复读取** | 用 `ContentCachingRequestWrapper` |
| **POST 无请求体** | 检查 `Content-Type` 是否正确 |

---

## 六、企业级最佳实践

- RESTful API 设计：URL 用名词复数 `/users`，HTTP 方法表示操作
- 统一响应格式：`{"code":200, "data":..., "message":"success"}`
- HTTPS 全站加密：生产环境强制 HTTPS
- 接口版本化：`/api/v1/user`、`/api/v2/user`
