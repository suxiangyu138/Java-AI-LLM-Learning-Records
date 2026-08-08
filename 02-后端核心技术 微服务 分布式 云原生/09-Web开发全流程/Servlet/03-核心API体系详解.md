# Servlet 核心 API 体系详解
> 继承层次源码剖析 + HttpServletRequest/HttpServletResponse 全量 API 分类 + ServletConfig/ServletContext/四大域对象——从接口到实现一次讲透

## 📚 目录
1. [继承层次与源码逻辑](#1-继承层次与源码逻辑)
2. [HttpServletRequest 全 API](#2-httpservletrequest-全-api)
3. [HttpServletResponse 全 API](#3-httpservletresponse-全-api)
4. [ServletConfig 与 ServletContext](#4-servletconfig-与-servletcontext)
5. [四大域对象对比](#5-四大域对象对比)
6. [Servlet 6.0/6.1 API 新增点](#6-servlet-6061-api-新增点)

## 1. 继承层次与源码逻辑

```text
                Servlet（接口：规范五方法）
                ├── init(ServletConfig) / service(ServletRequest, ServletResponse)
                ├── destroy() / getServletConfig() / getServletInfo()
                          │
                GenericServlet（抽象类：实现 Servlet + ServletConfig）
                ├── 保存 config 引用，提供无参 init() 钩子
                ├── 便捷方法：getInitParameter() / getServletContext() / log()
                └── service() 空实现（留给子类）
                          │
                   HttpServlet（抽象类：HTTP 专用）
                   ├── service(req, resp) 按方法分发到 doGet/doPost/doPut/...
                   ├── 7 个 doXxx 模板方法
                   └── getLastModified() 等 HTTP 语义方法
                          │
                  业务 Servlet（开发者编写，@WebServlet 声明）
```

### 1.1 分层职责

| 层级 | 职责 | 关键点 |
|------|------|--------|
| `Servlet` 接口 | 定义生命周期契约 | 与协议无关，Request/Response 是通用类型 |
| `GenericServlet` | 协议无关的骨架实现 | **保存 ServletConfig 引用**、空 service() |
| `HttpServlet` | HTTP 协议适配层 | **方法分发**是它唯一的"魔法" |
| 业务 Servlet | 实现 doGet/doPost 等 | 覆盖 HttpServlet 的模板方法 |

### 1.2 源码级关键逻辑

```java
// GenericServlet.init(ServletConfig) —— 为什么覆盖有参 init 必须 super
public void init(ServletConfig config) throws ServletException {
    this.config = config;      // 1. 保存 config
    this.init();               // 2. 回调无参 init() 钩子
}

// HttpServlet.service() —— 分发核心（6.x 实现，含 405 语义）
protected void service(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
    String method = req.getMethod();
    // GET/HEAD/OPTIONS/TRACE 走 doGet 族；未覆盖的 doXxx 默认返回 405
    if (method.equals(METHOD_GET)) {
        doGet(req, resp);
    } else if (method.equals(METHOD_HEAD)) {
        doHead(req, resp);
    } else if (method.equals(METHOD_POST)) {
        doPost(req, resp);
    } else if (method.equals(METHOD_PUT)) {
        doPut(req, resp);
    } else if (method.equals(METHOD_DELETE)) {
        doDelete(req, resp);
    } else if (method.equals(METHOD_OPTIONS)) {
        doOptions(req, resp);
    } else if (method.equals(METHOD_TRACE)) {
        doTrace(req, resp);
    } else {
        resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
    }
}
```

> 🎯 **核心认知**：HttpServlet 本身**不处理任何业务**——它只是把 HTTP 方法翻译成 Java 方法调用的适配层。Spring MVC 的 `DispatcherServlet` 覆盖了 `service()` 改为 `doService()` 并接管全部请求，本质是"换掉了分发逻辑"。

## 2. HttpServletRequest 全 API

### 2.1 请求行（URL 拆解）

```java
// URL: http://localhost:8080/app/user/list?page=1&size=20
req.getMethod();           // GET
req.getScheme();           // http
req.getServerName();       // localhost
req.getServerPort();       // 8080
req.getContextPath();      // /app        ← 应用上下文
req.getServletPath();      // /user/list  ← 匹配 Servlet 的路径
req.getPathInfo();         // null（精确匹配时）；/xxx（/* 匹配时的余下部分）
req.getQueryString();      // page=1&size=20
req.getRequestURI();       // /app/user/list
req.getRequestURL();       // http://localhost:8080/app/user/list
req.getProtocol();         // HTTP/1.1
req.getRequestId();        // 容器生成的本请求唯一 ID（Servlet 6.0 新增）
req.getProtocolRequestId();// 协议层 ID（HTTP/2 为 stream ID，6.0 新增）
```

> 💡 `getRequestURI()` vs `getRequestURL()`：前者是**路径**（无协议主机），后者是**完整 URL**——做鉴权白名单时用 URI（不易被 host 头污染），做绝对链接时用 URL。

### 2.2 请求头

```java
req.getHeader("User-Agent");          // 单值
req.getHeaders("Accept-Language");    // 多值（Enumeration）
req.getHeaderNames();                 // 所有头名
req.getIntHeader("Content-Length");   // int 值
req.getDateHeader("If-Modified-Since"); // 日期值（毫秒）
req.getContentType();                 // "application/json;charset=UTF-8"
req.getContentLength();               // body 字节数，-1 未知
req.getCharacterEncoding();           // 字符编码
```

### 2.3 请求参数（参数 vs 属性）

| 类型 | 存储 | 类型 | 来源 |
|------|------|:---:|------|
| **Parameter** | 只读 | 仅 String | Query String / 表单 Body / multipart 表单字段 |
| **Attribute** | 可读写 | 任意对象 | 开发者 setAttribute 传递（转发传参） |

```java
// ═══ Parameter ═══
req.getParameter("username");          // 单值，不存在返回 null
req.getParameterValues("hobby");       // 多值（checkbox）
req.getParameterNames();               // 所有参数名
req.getParameterMap();                 // Map<String, String[]>（不可修改视图）
// ⚠️ Servlet 6.1 澄清：参数解析失败（如编码错误）→ 抛 IllegalStateException
// ⚠️ JSON body 不是参数！getParameter 返回 null，必须读流

// ═══ Attribute ═══
req.setAttribute("user", userObj);
req.getAttribute("user");              // 不存在返回 null
req.removeAttribute("user");
req.getAttributeNames();
```

> ⚠️ **参数解析的触发时机**：`getParameter` 首次调用时才惰性解析；且**一次解析后缓存**，之后 `setCharacterEncoding` 不再生效——所以编码设置必须在第一次 getParameter **之前**。

### 2.4 请求体与流

```java
// 字符流（文本 body）
BufferedReader reader = req.getReader();
String body = reader.lines().collect(Collectors.joining("\n"));

// 字节流（二进制）
ServletInputStream in = req.getInputStream();
// 3.1+：非阻塞模式用 ReadListener；6.1+ 支持 ByteBuffer 读
int len; byte[] buf = new byte[8192];
while ((len = in.read(buf)) != -1) { ... }

// ⚠️ getReader() 与 getInputStream() 互斥，只能二选一！
```

### 2.5 会话与安全

```java
req.getSession();               // 获取或创建 Session
req.getSession(false);          // 只获取，不创建（登录校验标配）
req.changeSessionId();          // 轮换 Session ID（登录成功后防固定攻击）
req.isRequestedSessionIdValid();
req.isUserInRole("admin");      // 声明式安全（配合容器安全域）
req.getRemoteUser();            // 已认证用户名
req.authenticate(resp);         // 触发容器认证流程
req.login(username, password);  // 编程式登录（Servlet 3.0+）
```

### 2.6 分发与异步

```java
req.getRequestDispatcher("/target");  // 转发/包含（详见 04）
req.startAsync();                     // 进入异步模式（详见 06）
req.isAsyncStarted();
req.getAsyncContext();
```

## 3. HttpServletResponse 全 API

### 3.1 状态码

```java
resp.setStatus(200);                  // 普通状态（无错误页）
resp.sendError(404, "用户不存在");     // 错误状态 → 容器走错误页机制
resp.sendError(401);                  // 错误页分发恒用 GET（6.1 澄清）

// 常用常量：SC_OK=200 SC_CREATED=201 SC_NO_CONTENT=204 SC_PARTIAL_CONTENT=206
// SC_MOVED_PERMANENTLY=301 SC_FOUND=302（SC_MOVED_TEMPORARILY 保留兼容）
// SC_SEE_OTHER=303 SC_NOT_MODIFIED=304 SC_BAD_REQUEST=400 SC_UNAUTHORIZED=401
// SC_FORBIDDEN=403 SC_NOT_FOUND=404 SC_METHOD_NOT_ALLOWED=405
// SC_CONFLICT=409 SC_INTERNAL_SERVER_ERROR=500 SC_SERVICE_UNAVAILABLE=503
```

> 💡 6.1 细节：`SC_FOUND` 成为 302 的推荐常量名，`SC_MOVED_TEMPORARILY` 仅为兼容保留；`sendError` 与 `setStatus` 的区别在于前者触发**错误页分发**（web.xml `<error-page>`）。

### 3.2 响应头

```java
resp.setHeader("X-Frame-Options", "DENY");   // 覆盖已有值
resp.addHeader("Set-Cookie", "a=1");         // 追加多值
resp.setIntHeader("Content-Length", 1024);
resp.setDateHeader("Last-Modified", ts);
resp.setContentType("application/json;charset=UTF-8");
resp.setCharacterEncoding("UTF-8");          // 6.1+ 有 Charset 重载
resp.setLocale(Locale.CHINA);                // 影响 Content-Language 与编码默认值
```

### 3.3 输出流（Writer 与 OutputStream 互斥）

```java
// 字符输出：文本/HTML/JSON
PrintWriter w = resp.getWriter();
w.write("{\"code\":200}");

// 字节输出：图片/PDF/文件
ServletOutputStream os = resp.getOutputStream();
os.write(bytes);
// 3.1+：WriteListener 非阻塞写；6.1+：write(ByteBuffer)

// ⚠️ getWriter() 与 getOutputStream() 同一响应只能二选一，先调用者锁定
```

### 3.4 重定向（6.1 重大增强）

```java
// 传统：永远 302
resp.sendRedirect("/app/login");

// 6.1 新增重载：自定义状态码 + 是否清空缓冲区
resp.sendRedirect("/app/login");                     // 302，清缓冲区
resp.sendRedirect("/login", 301, true);              // 永久重定向（SEO 迁移）
resp.sendRedirect("/order/123", SC_SEE_OTHER, false);// 303 保留缓冲内容

// 抽象方法：sendRedirect(String location, int sc, boolean clearBuffer)
// 默认实现：sendRedirect(location) = sendRedirect(location, SC_FOUND, true)
```

| 场景 | 推荐状态码 | 说明 |
|------|:---:|------|
| 表单提交后（PRG） | 302 / 303 | 刷新不重复提交 |
| 永久迁移（SEO） | 301 | 搜索引擎更新索引 |
| 登录后跳转 | 302 / 303 | 避免缓存中间页 |

> ⚠️ 重定向本质 = `Location` 头 + 状态码：6.1 之前无法自定义状态码，导致 301 场景只能手写 `setStatus` + `setHeader("Location")`——现在有官方 API 了。

## 4. ServletConfig 与 ServletContext

### 4.1 ServletConfig——单 Servlet 局部配置

```java
ServletConfig config = getServletConfig();   // GenericServlet 已持有
config.getServletName();                     // "UserServlet"
config.getInitParameter("pageSize");         // 本 Servlet 的初始化参数
config.getInitParameterNames();
config.getServletContext();                  // 可由此拿到全局上下文
```

### 4.2 ServletContext——应用全局上下文（唯一）

```java
ServletContext ctx = getServletContext();

// 1. 全局初始化参数（web.xml <context-param>）
ctx.getInitParameter("appName");

// 2. 全局属性（所有组件共享，注意并发）
ctx.setAttribute("onlineCount", 100);

// 3. 资源访问
ctx.getResourceAsStream("/WEB-INF/config.properties"); // 流式读取（推荐）
ctx.getRealPath("/WEB-INF/x");   // ⚠️ 打 War 外置部署时可能返回 null，别依赖
ctx.getResource("/static/logo.png");  // 返回 URL

// 4. 其他
ctx.getMimeType("report.pdf");     // "application/pdf"
ctx.getServerInfo();               // "Apache Tomcat/11.0.6"
ctx.getMajorVersion();             // 6（Servlet 版本）
ctx.log("应用日志");               // 容器日志系统
ctx.getContextPath();              // "/app"
ctx.setSessionTimeout(30);         // 分钟
ctx.addListener(MyListener.class); // 编程式注册监听器
ctx.addServlet("x", XServlet.class).addMapping("/x");  // 编程式注册（详见 05）
```

> 🎯 **口诀**：Config 管"我自己的参数"，Context 管"全应用的资源"；Config 能拿 Context，Context 拿不到 Config。**Context 全局属性是并发热点**——高并发计数器用 `AtomicInteger`，别用裸 `int`。

## 5. 四大域对象对比

| 域对象 | 范围 | 生命周期 | 典型用途 |
|--------|------|---------|---------|
| **PageContext** | 单个 JSP 页 | 页面渲染结束 | JSP 标签内部共享（现代已少用） |
| **Request** | 一次请求 | 请求结束即销毁 | 转发传参、Filter 标记、traceId 传递 |
| **Session** | 一次会话 | 默认 30 分钟无操作过期 | 登录态、购物车 |
| **Application (ServletContext)** | 整个应用 | 启动 → 关闭 | 全局配置、在线人数 |

> 🎯 **选择原则：能用小不用大**——Request > Session > Application。Session 别放大对象（每次请求都要序列化/传输）；Application 别放易变数据（全局竞争）。

## 6. Servlet 6.0/6.1 API 新增点

| API | 版本 | 说明 |
|-----|:---:|------|
| `ServletRequest.getRequestId()` | 6.0 | 容器级唯一请求 ID，日志链路天然对齐 |
| `ServletRequest.getProtocolRequestId()` | 6.0 | 协议层 ID（HTTP/2 stream / HTTP/1.1 内部 ID） |
| `Cookie.setAttribute(name, value)` | 6.0 | 通用 Cookie 属性（SameSite/Partitioned 等） |
| `Cookie.getAttributes()` | 6.0 | 读取全部属性 |
| `HttpServletResponse.sendRedirect(location, sc[, clearBuffer])` | 6.1 | 重定向状态码控制 |
| `ServletInputStream.read(ByteBuffer)` | 6.1 | ByteBuffer 非阻塞读 |
| `ServletOutputStream.write(ByteBuffer)` | 6.1 | ByteBuffer 非阻塞写 |
| `HttpSessionIdListener.sessionIdChanged(event, oldId)` | 6.1 | 会话 ID 变更监听 |
| `RequestDispatcher.ERROR_QUERY_STRING` | 6.1 | 错误分发时取原始 query string |
| Charset 重载系列 | 6.1 | `setCharacterEncoding(Charset)` 等编译期安全重载 |
| 新 HTTP 状态码常量 | 6.1 | 补齐 103/425/428/429 等 |

> 🎯 面试差异化：能主动讲出 6.0/6.1 的 API 增量（尤其 `getRequestId` 与 `sendRedirect` 重载），比背老八股高一个段位——版本细节见 [01-规范演进史与版本矩阵](01-规范演进史与版本矩阵.md)。

---

**下一模块**：[04-请求处理与路径映射](04-请求处理与路径映射.md) / **返回总览**：[00-Servlet总览](00-Servlet总览.md)
