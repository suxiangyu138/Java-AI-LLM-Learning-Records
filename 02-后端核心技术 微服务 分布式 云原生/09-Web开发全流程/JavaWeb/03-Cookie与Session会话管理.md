# 03 - Cookie 与 Session 会话管理

> HTTP 是无状态协议，Cookie 与 Session 是 Java Web 中维持会话状态的两大基石。深入理解它们的原理、安全风险与分布式方案，是后端开发的分水岭。

---

## 目录

1. [HTTP 无状态与会话需求](#1-http-无状态与会话需求)
2. [Cookie 深入详解](#2-cookie-深入详解)
3. [Session 深入详解](#3-session-深入详解)
4. [Cookie vs Session 核心对比](#4-cookie-vs-session-核心对比)
5. [Session 安全专题](#5-session-安全专题)
6. [分布式 Session 方案](#6-分布式-session-方案)
7. [Token 认证（JWT）入门](#7-token-认证jwt入门)
8. [Servlet API 实战](#8-servlet-api-实战)
9. [常见面试题](#9-常见面试题)

---

## 1. HTTP 无状态与会话需求

### 1.1 问题场景

```
用户 A：登录 → 浏览商品 → 加入购物车 → 下单
用户 B：登录 → 查看订单 → 修改密码

HTTP 请求之间互相独立，服务器无法区分 "这个请求来自用户 A 还是 B"
→ 需要一种机制在多个请求之间维持用户身份 = 会话（Session）
```

### 1.2 会话跟踪技术演进

| 技术 | 原理 | 缺陷 | 状态 |
|------|------|------|------|
| **URL 重写** | `url;jsessionid=xxx` 拼接在 URL 上 | 暴露 ID、书签泄漏、丑陋 | ❌ 淘汰 |
| **隐藏表单域** | `<input type="hidden" name="token">` | 每个页面都要带、易丢失 | ❌ 淘汰 |
| **Cookie** | 浏览器自动携带的小数据 | 大小限制、可禁用 | ✅ 主流 |
| **Token Header** | `Authorization: Bearer xxx` | 需前端主动携带 | ✅ 主流 |

---

## 2. Cookie 深入详解

### 2.1 什么是 Cookie

> Cookie 是服务器发送到浏览器并保存在本地的一小段数据（≤4KB），浏览器之后的每次请求都会自动携带到同一服务器。

```
┌──────────┐                          ┌──────────┐
│  Browser  │ 1. GET /login            │  Server  │
│          │ ────────────────────────> │          │
│          │ 2. Set-Cookie: uid=abc    │          │
│          │ <──────────────────────── │          │
│  ┌─────┐ │                          │          │
│  │Cookie│ │ 3. GET /order            │          │
│  │uid=ab│ │    Cookie: uid=abc       │          │
│  └─────┘ │ ────────────────────────> │          │
└──────────┘                          └──────────┘
```

### 2.2 Cookie 属性详解

```http
Set-Cookie: sessionId=abc123;           ← 键值对
            Max-Age=3600;              ← 有效期（秒），0=立即删除
            Domain=.example.com;        ← 生效域名范围
            Path=/app;                 ← 生效路径范围
            Secure;                    ← 仅 HTTPS 传输
            HttpOnly;                  ← 禁止 JavaScript 访问
            SameSite=Lax               ← 跨站请求控制
```

| 属性 | 说明 | 推荐值 |
|------|------|--------|
| **Name=Value** | 键值对，Value 建议 URL 编码 | `JSESSIONID=xxx` |
| **Max-Age** | 存活秒数。正数=持久、0=立即删除、负数=会话级（浏览器关闭删除） | 根据需求 |
| **Expires** | 过期日期（HTTP-date 格式） | 与 Max-Age 二选一 |
| **Domain** | 生效域名。不设=当前域名；设 `.example.com` = 所有子域名 | 按需 |
| **Path** | 生效路径。`/` = 全站 | `/` |
| **Secure** | 仅 HTTPS 发送 | ✅ 必须开启 |
| **HttpOnly** | JavaScript 不可读写（防 XSS 窃取） | ✅ 必须开启 |
| **SameSite** | 跨站请求是否携带 | `Lax`（推荐） |

### 2.3 SameSite 详解

```
SameSite=Strict  → 完全禁止跨站发送（最严格）
  场景：从外部链接点击进入 → 首次请求不带 Cookie → 可能显示未登录

SameSite=Lax     → 允许顶层导航 GET 请求（推荐）
  允许：<a href> 点击、<link rel="prerender">、GET 表单
  禁止：POST 表单、<img>、<iframe>、AJAX 跨站请求

SameSite=None    → 始终发送（必须同时设置 Secure）
  场景：第三方嵌入（如 iframe 中的支付组件）
```

### 2.4 Java Servlet 操作 Cookie

```java
// ═══ 创建 Cookie ═══
Cookie cookie = new Cookie("username", URLEncoder.encode("张三", "UTF-8"));
cookie.setMaxAge(7 * 24 * 60 * 60);  // 7 天
cookie.setPath("/");
cookie.setHttpOnly(true);             // 防止 XSS 读取
cookie.setSecure(true);               // 仅 HTTPS（生产环境）
cookie.setDomain(".example.com");     // 跨子域
resp.addCookie(cookie);

// ═══ 读取 Cookie ═══
Cookie[] cookies = req.getCookies();  // 可能为 null！
if (cookies != null) {
    for (Cookie c : cookies) {
        if ("username".equals(c.getName())) {
            String value = URLDecoder.decode(c.getValue(), "UTF-8");
        }
    }
}

// ═══ 删除 Cookie ═══
Cookie deleteCookie = new Cookie("username", "");
deleteCookie.setMaxAge(0);   // 0 = 立即过期
deleteCookie.setPath("/");   // 必须与创建时 path 一致
resp.addCookie(deleteCookie);
```

### 2.5 Cookie 的限制

| 限制项 | 值 | 说明 |
|--------|-----|------|
| **单条大小** | ≤ 4KB | 包括 Name 和 Value |
| **单域名总数** | 20~50 条 | 不同浏览器略有差异 |
| **总条数** | 约 3000 条 | 超出后旧 Cookie 被清除 |
| **字符限制** | ASCII | 中文需 URL 编码 |

> ⚠️ Cookie 不适合存大数据。需要存复杂数据时，存 Session ID 即可，实际数据放服务端。

---

## 3. Session 深入详解

### 3.1 Session 原理

```
1. 用户首次访问 → 服务器创建 Session 对象，分配唯一 JSESSIONID
2. 服务器通过 Set-Cookie 将 JSESSIONID 发给浏览器
3. 浏览器后续请求自动带上 Cookie: JSESSIONID=xxx
4. 服务器根据 JSESSIONID 找到对应的 Session 对象
5. Session 超时或手动销毁 → 从内存中移除

┌─────────┐                           ┌──────────────┐
│ Browser  │──GET /login──────────────>│   Session 管理 │
│         │<──Set-Cookie:JSESSIONID=1 │  ┌──────────┐ │
│         │──GET /order───────────────>│  │id=1      │ │
│         │   Cookie:JSESSIONID=1     │  │user=张三  │ │
│         │<──200 OK──────────────────│  │cart=[...] │ │
└─────────┘                           │  └──────────┘ │
                                       │  ┌──────────┐ │
                                       │  │id=2       │ │
                                       │  │user=李四  │ │
                                       │  └──────────┘ │
                                       └──────────────┘
```

### 3.2 Session 生命周期

```java
// ═══ 创建 ═══
// 方式一：自动创建（默认，请求时无有效 JSESSIONID 即创建）
// 方式二：手动创建
HttpSession session = req.getSession();     // 有则返回，无则创建
HttpSession session = req.getSession(true); // 同上（默认）
HttpSession session = req.getSession(false);// 有则返回，无则返回 null

// ═══ 什么时候不会创建 Session ═══
// 1. JSP 页面默认自动创建（page 指令 session="false" 可禁止）
// 2. 只访问静态资源（html/css/js）
// 3. req.getSession(false) 且不存在有效 Session

// ═══ 销毁 ═══
session.invalidate();  // 立即销毁（如用户注销登录）
// 或等待超时自动销毁

// ═══ 超时配置 ═══
// web.xml:
// <session-config>
//     <session-timeout>30</session-timeout>  <!-- 分钟 -->
// </session-config>

// 编程式设置：
session.setMaxInactiveInterval(30 * 60);  // 秒

// Spring Boot:
// server.servlet.session.timeout=30m
```

### 3.3 Session 核心 API

```java
HttpSession session = req.getSession();

// ═══ 属性操作（类似 Map） ═══
session.setAttribute("user", user);
User user = (User) session.getAttribute("user");
session.removeAttribute("user");
Enumeration<String> names = session.getAttributeNames();

// ═══ 元数据 ═══
String id = session.getId();              // JSESSIONID 值
long createdTime = session.getCreationTime();   // 创建时间戳
long lastAccessTime = session.getLastAccessedTime(); // 最后访问时间
boolean isNew = session.isNew();          // 是否是刚创建的（首次请求）
int interval = session.getMaxInactiveInterval(); // 超时时间（秒）

// ═══ ServletContext ═══
ServletContext ctx = session.getServletContext(); // 获取上下文
```

### 3.4 Session 存储结构

```java
// Tomcat 默认实现：StandardSession
// 内部结构（简化）：
public class StandardSession implements HttpSession {
    private String id;                           // Session ID
    private long creationTime;                   // 创建时间
    private long lastAccessedTime;               // 最后访问时间
    private int maxInactiveInterval = 1800;      // 超时（秒）
    private ConcurrentMap<String, Object> attributes = new ConcurrentHashMap<>();
    private transient Manager manager;           // Session 管理器
    private boolean isNew;                       // 是否新建
    // ...
}

// Manager 管理所有 Session：
// StandardManager   → 内存存储（默认）+ 可持久化到文件
// PersistentManager → 可持久化到文件或 JDBC
// DeltaManager      → 集群 Session 复制
```

---

## 4. Cookie vs Session 核心对比

| 维度 | Cookie | Session |
|------|--------|---------|
| **存储位置** | 客户端（浏览器） | 服务端（内存/Redis/DB） |
| **安全性** | 较低（可被篡改、窃取） | 较高（数据在服务端） |
| **容量** | ≤ 4KB/条 | 无明确限制（受内存制约） |
| **数据类型** | String | 任意 Object |
| **性能** | 不占用服务端资源 | 占用内存，分布式需额外开销 |
| **有效期** | 可持久化（Max-Age） | 默认 30 分钟无操作过期 |
| **跨域** | 可跨子域（Domain） | 不直接跨域 |
| **禁用影响** | 用户可禁用 | Session 依赖 Cookie 传 ID |

### 典型场景选择

| 场景 | 推荐方案 | 原因 |
|------|---------|------|
| 登录状态 | **Session** | 安全、复杂数据 |
| "记住我" | **Cookie + Token** | 长期有效 |
| 购物车（未登录） | **Cookie** | 无须服务端存储 |
| 购物车（已登录） | **Session/Redis** | 跨设备同步 |
| 用户偏好（主题/语言） | **Cookie** | 非敏感、客户端即时可用 |
| 权限信息 | **Session** | 安全敏感 |

---

## 5. Session 安全专题

### 5.1 Session 固定攻击（Session Fixation）

```
攻击流程：
1. 攻击者访问网站，获取 JSESSIONID=ATTACKER123
2. 攻击者构造链接 https://example.com/login?JSESSIONID=ATTACKER123
3. 受害者点击链接，用攻击者的 JSESSIONID 登录
4. 攻击者用同一个 JSESSIONID 访问 → 以受害者身份操作

防护方案：
// ⭐ 登录成功后立即更换 Session ID
req.changeSessionId();  // Servlet 3.1+ 内置方法

// 或手动实现：
req.getSession().invalidate();
req.getSession(true);  // 创建新 Session
```

### 5.2 Session 劫持（Session Hijacking）

```
攻击方式：
1. XSS 窃取：注入脚本读取 document.cookie → 发送到攻击者服务器
2. 网络嗅探：在非 HTTPS 连接中截获 Cookie
3. 中间人攻击：HTTPS 降级为 HTTP

防护：
// 1. 对 JSESSIONID Cookie 设置安全属性
<CookieProcessor className="org.apache.tomcat.util.http.LegacyCookieProcessor"
                 sameSiteCookies="strict"/>

// 2. 代码中设置
Cookie cookie = new Cookie("JSESSIONID", session.getId());
cookie.setHttpOnly(true);  // 防 XSS 读取
cookie.setSecure(true);     // 仅 HTTPS 传输
cookie.setPath("/");

// 3. 绑定客户端特征（IP、User-Agent）辅助校验
String storedIP = (String) session.getAttribute("_CLIENT_IP");
String currentIP = getClientIP(req);
if (!currentIP.equals(storedIP)) {
    session.invalidate();  // IP 变化，强制重新登录
}
// ⚠️ IP 校验会误杀正常切换网络的用户（WiFi ↔ 4G），需权衡
```

### 5.3 安全实践清单

| 措施 | 说明 |
|------|------|
| ✅ `HttpOnly=true` | 防止 JavaScript 读取 Cookie |
| ✅ `Secure=true` | 仅在 HTTPS 传输 |
| ✅ 登录后换 Session ID | 防止 Session 固定 |
| ✅ 设置合理的超时 | 降低劫持窗口期 |
| ✅ 敏感操作二次验证 | 修改密码、支付等 |
| ✅ 同 IP/UA 辅助校验 | 但不要作为唯一依据 |
| ❌ 不要将密码存 Session | 即使加密也不行 |
| ❌ 不要用可预测的 Session ID | Tomcat 默认生成足够安全 |

---

## 6. 分布式 Session 方案

### 6.1 为什么需要分布式 Session？

```
负载均衡下：同一用户的两次请求可能落到不同服务器
┌──────────┐        ┌──────────────┐
│  Nginx   │──请求1──>│  Tomcat-A    │ Session 在 A 上
│ (轮询)    │        └──────────────┘
│          │──请求2──>┌──────────────┐
└──────────┘        │  Tomcat-B    │ ❌ 找不到 Session！
                     └──────────────┘
```

### 6.2 四种方案对比

#### 方案一：Nginx IP Hash（粘性会话）

```nginx
upstream backend {
    ip_hash;  # 同一 IP 始终路由到同一台服务器
    server 192.168.1.10:8080;
    server 192.168.1.11:8080;
}
```

| 优点 | 缺点 |
|------|------|
| 零配置、零依赖 | 服务器宕机 Session 丢失 |
| 性能最好 | IP 变化后重新登录 |
| | 负载不均衡（NAT 网络下） |

#### 方案二：Session 复制

> Tomcat 集群间通过多播互相广播 Session 变更。详见 02-Tomcat 讲义第10节。

| 优点 | 缺点 |
|------|------|
| 不需要外部依赖 | 网络开销大（N² 广播） |
| 应用无感 | 内存浪费（每台都存全部 Session） |
| | 不适合大规模集群（>4 台） |

#### 方案三：Redis 集中存储（⭐ 推荐）

```java
// Spring Session + Redis 配置
// 依赖：spring-session-data-redis
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)
public class SessionConfig {
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory();
        factory.setHostName("redis.example.com");
        factory.setPort(6379);
        return factory;
    }
}
```

```yaml
# application.yml
spring:
  session:
    store-type: redis
    timeout: 30m
    redis:
      namespace: spring:session
      flush-mode: on_save  # immediate | on_save
  redis:
    host: localhost
    port: 6379
```

#### 方案四：JWT 无状态（⭐ 推荐）

```
JWT = JSON Web Token，将用户信息编码到 Token 中
服务端不存 Session，通过签名验证 Token 合法性

Header.Payload.Signature
xxxxxxxx.yyyyyyyy.zzzzzzzz
```

| 方案 | 存储位置 | 水平扩展 | 注销 | 适合场景 |
|------|---------|---------|------|---------|
| IP Hash | 服务端 | 困难 | 简单 | 小型应用 |
| Session 复制 | 服务端 | 困难 | 简单 | ❌不推荐 |
| **Redis Session** | 外部 | 简单 | 简单 | 传统 Web 应用 |
| **JWT** | 客户端 | 天然支持 | 需黑名单 | 前后端分离/API |

---

## 7. Token 认证（JWT）入门

### 7.1 JWT 结构

```
┌─────────────────────────────────────────────────┐
│ eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9           │ ← Header(Base64)
│ {"alg":"HS256","typ":"JWT"}                     │
├─────────────────────────────────────────────────┤
│ eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IuW8oOS4iSIs │ ← Payload(Base64)
│ ImlhdCI6MTUxNjIzOTAyMn0                         │ {"sub":"123","name":"张三","iat":1516239022}
├─────────────────────────────────────────────────┤
│ SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c    │ ← Signature(HMAC-SHA256)
└─────────────────────────────────────────────────┘
```

### 7.2 Session vs JWT 决策

```
┌─────────────────────────────────────────────────────┐
│                 认证方案选择决策树                       │
│                                                      │
│ 是否需要服务端主动注销（踢人）？                         │
│   ├── 是 → Session（Redis 方案）                      │
│   └── 否 → 继续判断                                   │
│                                                      │
│ 是否纯 API（前后端分离、移动端）？                       │
│   ├── 是 → JWT                                       │
│   └── 否 → Session                                   │
│                                                      │
│ 是否需要跨域/多服务共享？                               │
│   ├── 是 → JWT                                       │
│   └── 否 → Session                                   │
└─────────────────────────────────────────────────────┘
```

---

## 8. Servlet API 实战

### 8.1 登录完整示例

```java
@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // 1. 验证用户
        User user = userService.login(username, password);
        if (user == null) {
            req.setAttribute("error", "用户名或密码错误");
            req.getRequestDispatcher("/login.jsp").forward(req, resp);
            return;
        }

        // 2. 登录成功 — 更换 Session ID（防 Session 固定）
        req.changeSessionId();

        // 3. 将用户信息存入 Session
        HttpSession session = req.getSession();
        session.setAttribute("currentUser", user);
        session.setAttribute("loginTime", System.currentTimeMillis());

        // 4. 处理"记住我"
        String remember = req.getParameter("remember");
        if ("on".equals(remember)) {
            String token = generateRememberToken(user);
            Cookie rememberCookie = new Cookie("remember_token", token);
            rememberCookie.setMaxAge(30 * 24 * 60 * 60);  // 30天
            rememberCookie.setHttpOnly(true);
            rememberCookie.setSecure(true);
            rememberCookie.setPath("/");
            resp.addCookie(rememberCookie);
        }

        // 5. 重定向到首页（PRG 模式）
        resp.sendRedirect(req.getContextPath() + "/index");
    }
}
```

### 8.2 登录拦截 Filter

```java
@WebFilter("/*")
public class AuthFilter implements Filter {
    // 白名单路径
    private static final Set<String> WHITE_LIST = Set.of(
        "/login", "/register", "/static/", "/api/public/");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getRequestURI().substring(req.getContextPath().length());

        // 白名单放行
        if (isWhiteListed(path)) {
            chain.doFilter(request, response);
            return;
        }

        // 检查 Session
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("currentUser") == null) {
            // 未登录 → 重定向到登录页
            resp.sendRedirect(req.getContextPath() + "/login?redirect="
                + URLEncoder.encode(path, "UTF-8"));
            return;
        }

        // 已登录 → 放行
        chain.doFilter(request, response);
    }

    private boolean isWhiteListed(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }
}
```

---

## 9. 常见面试题

### Q1：Cookie 和 Session 的区别？

> Cookie 存客户端（≤4KB，String，不安全）；Session 存服务端（无大小限制，Object，安全）。Session 通常依赖 Cookie 传递 JSESSIONID。详见第4节。

### Q2：分布式环境下 Session 如何共享？

> 四种方案：Nginx ip_hash（简单但有风险）、Session 复制（不推荐）、Redis 集中存储（推荐，Spring Session）、JWT 无状态（推荐，前后端分离）。详见第6节。

### Q3：Session 和 Cookie 的安全性如何保障？

> Session：HttpOnly + Secure + 登录后换 ID + 合理超时。Cookie：敏感数据不存、加密 + 签名防篡改。详见第5节。

### Q4：如果浏览器禁用 Cookie，Session 还能用吗？

> 默认不能（JSESSIONID 依赖 Cookie）。备选方案：URL 重写（`resp.encodeURL()`），但安全性差、已基本弃用。现代实践用 Token Header（Authorization: Bearer xxx）替代。

### Q5：Session 超时后会发生什么？

> 服务端 Session 对象被清除，下次请求 `req.getSession()` 会创建新 Session（isNew=true）。客户端旧 JSESSIONID 对应的数据已丢失。
