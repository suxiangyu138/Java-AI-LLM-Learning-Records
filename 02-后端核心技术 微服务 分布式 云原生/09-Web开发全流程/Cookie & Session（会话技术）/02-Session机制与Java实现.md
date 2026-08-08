# 02-Session 机制与 Java 实现
> Session 原理、SessionID 流转、HttpSession API、Servlet/JSP/Spring 使用——"数据在服务器，标识在 Cookie"

## 📚 目录
1. [Session 是什么](#1-session-是什么)
2. [Session 与 Cookie 的关系](#2-session-与-cookie-的关系)
3. [HttpSession API](#3-httpsession-api)
4. [Servlet 中获取 Session](#4-servlet-中获取-session)
5. [Spring 中的 Session 使用](#5-spring-中的-session-使用)
6. [Session 存储的数据类型要求](#6-session-存储的数据类型要求)
7. [核心要点](#7-核心要点)
8. [参考来源](#8-参考来源)

## 1. Session 是什么

```text
Session = 服务器端保存的会话数据
  HTTP 无状态 → 服务器用 Session 保存"用户状态"（登录信息/购物车）
  标识：SessionID（通常存 Cookie）

流程：
① 首次请求（无 SessionID）→ 服务器创建 Session + 生成 ID
② 下发 Set-Cookie: JSESSIONID=xxx
③ 后续请求携带 JSESSIONID → 服务器找到对应 Session
④ 无状态请求变成了"有状态会话"
```

| Session 特点 | 说明 |
|--------------|------|
| 数据在服务器 | 客户端只持有 ID |
| 生命周期 | 创建到失效（超时/登出） |
| 容量 | 服务器内存（或分布式存储，见 04） |
| 安全 | 数据不暴露给客户端（只有 ID） |

> 🎯 **Session 的本质**：**"数据在服务器，标识在客户端"**——Cookie 存 SessionID（小、可公开），用户数据存服务器（大、需保密）。

## 2. Session 与 Cookie 的关系

| 维度 | Cookie | Session |
|------|--------|---------|
| 存储位置 | 客户端 | 服务器 |
| 数据可见 | 浏览器可读（除 HttpOnly） | 客户端不可见 |
| 大小 | ~4KB 限制 | 服务器资源（内存） |
| 生命周期 | Max-Age 控制 | 服务器超时控制 |
| 依赖 | 独立 | **通常依赖 Cookie 传 ID** |

```text
Session 与 Cookie 的配合：
  Session 数据（服务器）← SessionID（Cookie 传递）
  没有 Cookie → Session 无法识别（除非 URL 重写，见 03）

也可以说：Session 是"存数据的地方"，Cookie 是"传标识的通道"
```

> 💡 一句话关系：**Session 用 Cookie 做"身份凭证的快递"，数据本身留在服务器**——这也是"Session 比 Cookie 安全（数据层面）"的原因。

## 3. HttpSession API

```java
// 获取 Session（核心 API）
HttpSession session = request.getSession();           // 有则取，无则创建
HttpSession session = request.getSession(false);      // 有则取，无则 null（不创建）

// 存数据
session.setAttribute("user", user);
session.setAttribute("cart", cartList);

// 取数据
User user = (User) session.getAttribute("user");
String name = (String) session.getAttribute("name");

// 移除
session.removeAttribute("user");

// 登出/失效
session.invalidate();          // 使 Session 失效（登出标准姿势）

// 信息
String id = session.getId();                       // SessionID
long createTime = session.getCreationTime();       // 创建时间
int maxInactive = session.getMaxInactiveInterval(); // 超时（秒）
session.setMaxInactiveInterval(1800);              // 设置 30 分钟超时
```

| 方法 | 说明 |
|------|------|
| `getSession()` | 有取无建 |
| `getSession(false)` | 有取无 null（判断是否已登录） |
| `setAttribute/getAttribute` | 存/取数据 |
| `invalidate()` | 失效（登出） |
| `setMaxInactiveInterval` | 超时时间 |

## 4. Servlet 中获取 Session

```java
// 登录成功：存入 Session
@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if ("admin".equals(username) && "123456".equals(password)) {
            HttpSession session = req.getSession();       // 创建/获取 Session
            session.setAttribute("user", username);       // 存入登录状态
            resp.sendRedirect("/index.jsp");              // 跳转
        } else {
            resp.sendRedirect("/login.jsp?error=1");
        }
    }
}

// 其他接口：判断是否登录
@WebServlet("/profile")
public class ProfileServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        HttpSession session = req.getSession(false);      // 不创建
        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect("/login.jsp");              // 未登录跳登录
            return;
        }
        // 已登录：处理业务
    }
}
```

```jsp
<!-- JSP 中直接使用 session 内置对象 -->
<%
    String user = (String) session.getAttribute("user");
    if (user == null) { response.sendRedirect("login.jsp"); return; }
%>
<p>欢迎，<%= user %></p>
```

> 🎯 **登录态判断的标准姿势**：`getSession(false)` + 检查属性——**false 参数避免"每次请求都创建新 Session"**（无 Cookie 的请求 getSession() 会一直新建，浪费且异常）。

## 5. Spring 中的 Session 使用

```java
// Spring MVC：方法参数注入
@RestController
public class UserController {

    @GetMapping("/profile")
    public String profile(HttpSession session) {         // 自动注入
        Object user = session.getAttribute("user");
        return user == null ? "未登录" : "用户: " + user;
    }

    // 或 @SessionAttribute（直接取属性）
    @GetMapping("/cart/count")
    public int cartCount(@SessionAttribute(name = "cart", required = false) List<Item> cart) {
        return cart == null ? 0 : cart.size();
    }
}
```

```yaml
# Spring Boot 配置 Session 超时（默认 30 分钟）
server:
  servlet:
    session:
      timeout: 30m            # 30 分钟
      cookie:
        http-only: true
        secure: true
        same-site: lax        # 显式配置（防默认值回归，Issue #48830）
```

| Spring 方式 | 说明 |
|-------------|------|
| `HttpSession` 参数注入 | 直接操作 |
| `@SessionAttribute` | 取属性（可 required） |
| `session.setAttribute` | 存数据 |
| Boot 配置 | timeout/cookie 属性显式化 |

## 6. Session 存储的数据类型要求

```text
分布式 Session（Redis 存储）时序列化要求：
  ① 存 Session 的对象必须可序列化（implements Serializable）
  ② 避免存大对象（Session 内存/网络开销）
  ③ 避免存不可序列化组件（数据库连接等）

单体内存 Session 的限制较少，但为了未来扩展——
"存 Session 的对象一律可序列化"是好习惯
```

> ⚠️ **序列化纪律**（为分布式做准备）：Session 里只放"业务数据对象"（用户信息/购物车 DTO），不放"连接/服务"类——否则切到 Redis 存储时直接报错。

## 7. 核心要点

> 🎯 **核心要点**：
> - Session = 服务器存数据 + Cookie 传 SessionID（"数据在服务器，标识在客户端"）；
> - `getSession()` 有取无建 vs `getSession(false)` 有取无 null（登录判断用 false）；
> - 登出标准姿势：`session.invalidate()`；
> - 超时默认 30 分钟（可配置），决定"多久不操作掉线"；
> - Spring：参数注入 + @SessionAttribute + Boot 配置（cookie 属性显式化）；
> - 序列化纪律：Session 对象可序列化（为分布式 Session 做准备，见 04）。

## 8. 参考来源

- [Jakarta Servlet 规范（HttpSession）](https://jakarta.ee/specifications/servlet/)
- [Spring Boot Session 配置文档](https://docs.spring.io/spring-boot/reference/web/servlet.html)
- [MDN：Session 与 Cookie](https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Session)

---

**下一模块**：[03-会话传递与生命周期](03-会话传递与生命周期.md)　/　**返回总览**：[00-总览](00-会话技术总览.md)
