# 01 - Servlet 核心技术深度解析

> Java Web 的基石：Servlet 生命周期、核心 API、请求/响应处理、配置方式与线程安全——掌握 Servlet 才能真正理解 Spring MVC 的底层原理。

---

## 目录

1. [Servlet 概述与 CGI 对比](#1-servlet-概述与-cgi-对比)
2. [Servlet 生命周期](#2-servlet-生命周期)
3. [Servlet 体系结构](#3-servlet-体系结构)
4. [HttpServletRequest 详解](#4-httpservletrequest-详解)
5. [HttpServletResponse 详解](#5-httpservletresponse-详解)
6. [ServletConfig 与 ServletContext](#6-servletconfig-与-servletcontext)
7. [请求转发与重定向](#7-请求转发与重定向)
8. [Servlet 配置方式演进](#8-servlet-配置方式演进)
9. [路径映射规则](#9-路径映射规则)
10. [线程安全与单例多线程模型](#10-线程安全与单例多线程模型)
11. [Servlet 3.0+ 新特性](#11-servlet-30-新特性)
12. [常见面试题](#12-常见面试题)

---

## 1. Servlet 概述与 CGI 对比

### 1.1 什么是 Servlet

Servlet 是运行在 Web 服务器（Servlet 容器）中的 Java 类，用于处理客户端 HTTP 请求并生成响应。它是 Java EE 规范的核心组件，也是 Spring MVC 的底层基石。

```
┌──────────┐    HTTP Request    ┌──────────────┐    ┌──────────┐
│  Browser  │ ─────────────────> │ Servlet 容器  │ ──> │ Servlet  │
│  (客户端) │ <───────────────── │  (Tomcat等)   │ <── │  (业务)   │
└──────────┘    HTTP Response   └──────────────┘    └──────────┘
```

### 1.2 Servlet vs CGI

| 维度 | CGI | Servlet |
|------|-----|---------|
| **进程模型** | 每个请求创建新进程 | 单实例多线程 |
| **资源开销** | 极高（进程创建/销毁） | 低（线程复用） |
| **性能** | 差，不适合高并发 | 好，支持高并发 |
| **可移植性** | 依赖操作系统 | JVM 跨平台 |
| **共享数据** | 进程间通信困难 | ServletContext 共享 |
| **内存占用** | 每请求独立内存空间 | 堆内存共享 |

> 🎯 CGI 每个请求 fork 一个进程 → 1000 并发 = 1000 个进程；Servlet 用线程池处理 → 1000 并发可能只需几十个线程。

### 1.3 Servlet 容器职责

| 职责 | 说明 |
|------|------|
| **通信支持** | 封装 Socket 通信，解析 HTTP 协议 |
| **生命周期管理** | 控制 Servlet 的创建、初始化、服务、销毁 |
| **多线程支持** | 为每个请求分配线程调用 `service()` 方法 |
| **声明式安全** | 通过配置实现认证与授权 |
| **JSP 支持** | 将 JSP 编译为 Servlet |

---

## 2. Servlet 生命周期

### 2.1 五个阶段

```
  ┌──────┐    ┌──────┐    ┌────────┐    ┌────────┐    ┌──────┐
  │ 加载  │───>│ 实例化│───>│  初始化 │───>│  服务   │───>│ 销毁  │
  │ Load  │    │ New   │    │ init()  │    │service()│    │destroy│
  └──────┘    └──────┘    └────────┘    └────────┘    └──────┘
    类加载      调用       首次请求      每次请求       容器关闭
             无参构造    时调用一次   调用一次        时调用一次
```

### 2.2 生命周期方法详解

```java
public class LifecycleServlet extends HttpServlet {

    // 1. 初始化：Servlet 创建后首次被访问时调用，只执行一次
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);  // 必须调用父类，保存 config 引用
        System.out.println("Servlet 初始化完成，可在此加载资源");
        // 典型场景：加载配置、初始化连接池、预热缓存
    }

    // 2. 服务：每次请求到达时调用
    //    由容器自动调用，开发者不要手动调用
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // 父类 GenericServlet.service() 会根据 HTTP 方法分发
        // GET  → doGet()   POST  → doPost()
        // PUT  → doPut()   DELETE → doDelete()
        super.service(req, resp);
    }

    // 3. 处理 GET 请求
    //    不安全：参数暴露在 URL 中；幂等：多次调用结果相同
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("text/html;charset=UTF-8");
        resp.getWriter().write("<h1>处理 GET 请求</h1>");
    }

    // 4. 处理 POST 请求
    //    安全（参数在 Body 中）；非幂等（通常）
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // 处理表单提交、JSON 数据等
        req.setCharacterEncoding("UTF-8");  // POST 必须在读取参数前设编码
        String username = req.getParameter("username");
        resp.getWriter().write("Received: " + username);
    }

    // 5. 销毁：容器关闭或重新部署时调用，只执行一次
    @Override
    public void destroy() {
        System.out.println("Servlet 即将销毁，释放资源");
        // 典型场景：关闭连接池、清理临时文件、注销 JMX
        // 注意：此时可能还有线程在执行 service()，需保证安全的资源释放
    }
}
```

### 2.3 关键时间点

| 阶段 | 触发时机 | 调用次数 | 可覆盖 |
|------|---------|---------|--------|
| `init()` | 首次请求（或 `load-on-startup`） | **1次** | 是 |
| `service()` | 每次请求 | **N次** | 慎重 |
| `doGet/doPost` | 每次请求（按方法分发） | **N次** | 是 |
| `destroy()` | 容器关闭 | **1次** | 是 |
| 构造方法 | 首次请求时 | **1次** | 不需 |

> ⚠️ `init(ServletConfig)` 与 `init()`：如果覆盖 `init(ServletConfig)` 必须调用 `super.init(config)`，否则 `getServletConfig()` 返回 null。推荐只覆盖无参 `init()`。

---

## 3. Servlet 体系结构

### 3.1 继承层次

```
                    Servlet (接口)
                    ├── init(ServletConfig)
                    ├── service(ServletRequest, ServletResponse)
                    ├── destroy()
                    └── getServletConfig() / getServletInfo()
                           │
                    GenericServlet (抽象类，与协议无关)
                    ├── 实现 Servlet、ServletConfig 接口
                    ├── 提供无参 init() 方便覆盖
                    └── 空实现 service()，留给子类
                           │
                      HttpServlet (抽象类，HTTP 专用)
                      ├── service(HttpServletRequest, HttpServletResponse)
                      ├── doGet()  / doPost()  / doPut()
                      ├── doDelete() / doHead() / doOptions()
                      └── doTrace() — 7个方法对应7种HTTP方法
```

### 3.2 源码关键逻辑

```java
// HttpServlet.service() 核心分发逻辑（简化版）
protected void service(HttpServletRequest req, HttpServletResponse resp) {
    String method = req.getMethod();

    if (method.equals("GET")) {
        doGet(req, resp);
    } else if (method.equals("POST")) {
        doPost(req, resp);
    } else if (method.equals("PUT")) {
        doPut(req, resp);
    } else if (method.equals("DELETE")) {
        doDelete(req, resp);
    } else if (method.equals("HEAD")) {
        doHead(req, resp);   // 默认调用 doGet 并丢弃 body
    } else if (method.equals("OPTIONS")) {
        doOptions(req, resp); // 返回支持的 HTTP 方法
    } else if (method.equals("TRACE")) {
        doTrace(req, resp);  // 回显请求（安全风险，通常禁用）
    } else {
        resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
    }
    // 注意：PATCH 未在规范中，需自行处理
}
```

> ⚠️ 覆盖 `service()` 方法后，HTTP 方法分发逻辑将失效——所有请求都进入你的 service()。除非有特殊需求（如自定义协议），否则只覆盖 doXxx()。

---

## 4. HttpServletRequest 详解

### 4.1 核心方法分类

#### 请求行信息

```java
req.getMethod();          // GET / POST / PUT / DELETE
req.getRequestURI();      // /app/user/list
req.getRequestURL();      // http://localhost:8080/app/user/list
req.getProtocol();        // HTTP/1.1
req.getScheme();          // http / https
req.getContextPath();     // /app (应用上下文路径)
req.getServletPath();     // /user/list
req.getQueryString();     // page=1&size=20
```

> 💡 URL 拆解示例：`http://localhost:8080/app/user/list?page=1`
> - `getScheme()` → `http`
> - `getServerName()` → `localhost`，`getServerPort()` → `8080`
> - `getContextPath()` → `/app`
> - `getServletPath()` → `/user/list`
> - `getQueryString()` → `page=1`

#### 请求头获取

```java
req.getHeader("User-Agent");        // 获取指定请求头
req.getHeaders("Accept-Language");  // 获取多值请求头（返回 Enumeration）
req.getHeaderNames();               // 所有请求头名称
req.getIntHeader("Content-Length"); // 获取整数型请求头
req.getDateHeader("If-Modified-Since"); // 获取日期型请求头

// 常用快捷方法
req.getContentType();     // "application/json;charset=UTF-8"
req.getContentLength();   // body 字节数，-1 表示未知
req.getCharacterEncoding(); // UTF-8（POST 需先 setCharacterEncoding）
```

#### 请求参数获取

```java
// ═══ 核心方法 ═══
req.getParameter("username");         // 获取单个参数值，不存在返回 null
req.getParameterValues("hobby");      // 获取多值参数（如 checkbox）
req.getParameterNames();              // 所有参数名称（Enumeration）
req.getParameterMap();                // Map<String, String[]> 所有参数

// ═══ 重要细节 ═══
// 1. GET 请求参数从 Query String 解析
// 2. POST application/x-www-form-urlencoded 参数从 Body 解析
// 3. POST multipart/form-data：getParameter() 返回 null，需用 Part API
// 4. POST application/json：getParameter() 返回 null，需读 InputStream
```

#### 请求体与流操作

```java
// 字符流：用于读取纯文本 Body
BufferedReader reader = req.getReader();
String body = reader.lines().collect(Collectors.joining("\n"));

// 字节流：用于读取二进制数据（文件上传等）
ServletInputStream inputStream = req.getInputStream();
byte[] buffer = new byte[1024];
int len;
ByteArrayOutputStream baos = new ByteArrayOutputStream();
while ((len = inputStream.read(buffer)) != -1) {
    baos.write(buffer, 0, len);
}
byte[] bodyBytes = baos.toByteArray();

// ⚠️ getReader() 和 getInputStream() 互斥，只能选其一调用
```

### 4.2 请求转发与包含

```java
// 获取转发器
RequestDispatcher rd = req.getRequestDispatcher("/target");
// 相对于当前请求路径（不以/开头）或 Context Root（以/开头）

// 转发：将请求交给另一个组件处理
rd.forward(req, resp);  // 只能调用一次，调用后原 Servlet 的响应被清空

// 包含：将另一个组件的输出包含到当前响应中
rd.include(req, resp);  // 可以多次调用，每个组件的输出拼接
```

### 4.3 属性（Attribute）传递

```java
// 在同一个请求范围内传递对象（不同于参数，参数只能是 String）
req.setAttribute("user", userObject);   // 设置属性
req.getAttribute("user");               // 获取属性，不存在返回 null
req.removeAttribute("user");            // 移除属性
req.getAttributeNames();                // 所有属性名

// 典型场景：Forward 时在 Servlet 间传递业务对象
```

### 4.4 国际化与编码

```java
// GET 请求参数编码（Tomcat 8+ 默认 UTF-8，之前需配置）
// server.xml Connector 添加 URIEncoding="UTF-8"

// POST 请求参数编码 — 必须在第一次 getParameter() 之前设置！
req.setCharacterEncoding("UTF-8");

// 获取客户端 Locale
req.getLocale();      // zh_CN
req.getLocales();     // 按优先级排列的 Locale 列表
```

---

## 5. HttpServletResponse 详解

### 5.1 响应状态行

```java
// 设置状态码
resp.setStatus(HttpServletResponse.SC_OK);             // 200
resp.setStatus(HttpServletResponse.SC_CREATED);         // 201
resp.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY); // 302
resp.setStatus(HttpServletResponse.SC_NOT_FOUND);       // 404
resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500

// 快捷方法：发送错误并跳转到错误页面
resp.sendError(HttpServletResponse.SC_NOT_FOUND, "用户不存在");
// 调用后不应再写入内容，容器会转发到 <error-page> 配置的页面
```

### 5.2 响应头设置

```java
// 通用头设置
resp.setHeader("X-Custom-Header", "value");   // 设置（覆盖已有值）
resp.addHeader("Set-Cookie", "session=abc");  // 追加（可多次调用）
resp.setIntHeader("Expires", 3600);           // 整数头
resp.setDateHeader("Last-Modified", System.currentTimeMillis()); // 日期头

// 常用头设置
resp.setContentType("application/json;charset=UTF-8");  // MIME 类型
resp.setContentLength(data.length);                      // 响应体长度
resp.setCharacterEncoding("UTF-8");                      // 字符编码

// 缓存控制
resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
resp.setHeader("Pragma", "no-cache");    // HTTP/1.0 兼容
resp.setDateHeader("Expires", 0);        // 立即过期

// CORS 跨域（简化版）
resp.setHeader("Access-Control-Allow-Origin", "*");
resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
```

### 5.3 响应体输出

```java
// ═══ 字符输出：文本、HTML、JSON ═══
PrintWriter writer = resp.getWriter();
resp.setContentType("application/json;charset=UTF-8");
writer.write("{\"code\":200,\"msg\":\"success\"}");
writer.flush(); // 强制刷新缓冲区

// ═══ 字节输出：图片、PDF、文件下载 ═══
ServletOutputStream outputStream = resp.getOutputStream();
resp.setContentType("application/octet-stream");
resp.setHeader("Content-Disposition", "attachment; filename=\"report.pdf\"");
outputStream.write(fileBytes);
outputStream.flush();

// ⚠️ getWriter() 和 getOutputStream() 互斥，只能选其一！
```

### 5.4 重定向

```java
// 方式一：手动设置状态码和 Location 头
resp.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY); // 302
resp.setHeader("Location", "/app/login.html");

// 方式二：快捷方法（推荐）
resp.sendRedirect("/app/login.html");
// 等价于 302 + Location，容器自动处理相对路径
```

---

## 6. ServletConfig 与 ServletContext

### 6.1 ServletConfig — 每个 Servlet 独有

```java
// 获取方式
ServletConfig config = getServletConfig();  // GenericServlet 已持有引用

// API
config.getServletName();              // "MyServlet"
config.getInitParameter("encoding");  // 获取初始化参数
config.getInitParameterNames();       // 所有初始化参数名
config.getServletContext();           // 获取 ServletContext

// 注解配置初始化参数
@WebServlet(
    urlPatterns = "/user",
    initParams = {
        @WebInitParam(name = "encoding", value = "UTF-8"),
        @WebInitParam(name = "pageSize", value = "20")
    }
)
```

### 6.2 ServletContext — 整个 Web 应用共享

```java
// ServletContext = 应用全局上下文，一个应用只有一个
ServletContext context = getServletContext();
// 也可通过 request 获取：req.getServletContext()

// ═══ 1. 全局初始化参数 ═══
// web.xml:
// <context-param>
//     <param-name>appName</param-name>
//     <param-value>MyApp</param-value>
// </context-param>
context.getInitParameter("appName");
context.getInitParameterNames();

// ═══ 2. 域对象（Attribute）—— 全局共享数据 ═══
context.setAttribute("onlineCount", 100);
context.getAttribute("onlineCount");     // 不存在返回 null
context.removeAttribute("onlineCount");

// ═══ 3. 资源路径 ═══
context.getRealPath("/WEB-INF/config.properties");  // 物理路径
context.getResourceAsStream("/WEB-INF/config.properties"); // 输入流
// 注意：getRealPath() 在打 War 包部署时可能返回 null

// ═══ 4. 获取 MIME 类型 ═══
context.getMimeType("image.png");  // "image/png"

// ═══ 5. 日志 ═══
context.log("应用启动成功");

// ═══ 6. 获取 Servlet 容器信息 ═══
context.getServerInfo();       // "Apache Tomcat/9.0.80"
context.getServletContextName(); // Web 应用名称
context.getMajorVersion();     // 4 (Servlet 4.0)
```

### 6.3 四大域对象对比

| 域对象 | 范围 | 生命周期 | 典型用途 |
|--------|------|---------|---------|
| **PageContext** | 当前 JSP 页面 | 页面渲染完毕 | JSP 页面内数据共享 |
| **HttpServletRequest** | 一次请求 | 请求结束 | 请求转发传参、过滤器标记 |
| **HttpSession** | 一次会话 | 会话过期（默认30分钟） | 用户登录信息、购物车 |
| **ServletContext** | 整个应用 | 应用启动到关闭 | 全局配置、在线人数统计 |

> 🎯 选择原则：能用小范围不用大范围。Request > Session > Application，减少内存占用和并发冲突。

---

## 7. 请求转发与重定向

### 7.1 核心区别

| 维度 | 转发 (Forward) | 重定向 (Redirect) |
|------|---------------|-------------------|
| **行为** | 服务器内部跳转 | 浏览器重新发起请求 |
| **URL 变化** | 不变（浏览器看不到） | 变为目标地址 |
| **请求次数** | **1 次** | **2 次**（第一次 302） |
| **能否跨域** | ❌ 只能应用内 | ✅ 可以跳转到外部 URL |
| **Request 对象** | 同一个（可共享 Attribute） | 不同的（数据丢失） |
| **速度** | 快 | 慢（多一次网络往返） |
| **状态码** | — | 301（永久）/ 302（临时） |

### 7.2 代码对比

```java
// ═══ Forward — 服务器内部转发 ═══
@WebServlet("/forwardDemo")
public class ForwardServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // 在转发前设置 Attribute，目标 Servlet 可以获取
        req.setAttribute("message", "来自 ForwardServlet 的数据");

        // 转发到另一个 Servlet 或 JSP
        req.getRequestDispatcher("/result").forward(req, resp);
        // ⚠️ forward 之后的代码不会执行（但会编译通过，容易遗漏逻辑）
        // 如果 forward 前已通过 resp 写入未刷新的内容，forward 时会清空
    }
}

// ═══ Redirect — 浏览器重新跳转 ═══
@WebServlet("/redirectDemo")
public class RedirectServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        // 方式一：手动
        resp.setStatus(302);
        resp.setHeader("Location", "/app/result");

        // 方式二：快捷方法
        resp.sendRedirect("/app/result");

        // ⚠️ sendRedirect() 前不能有内容已提交到浏览器
        // ⚠️ 路径不以 / 开头时为相对路径
    }
}
```

### 7.3 路径问题的坑

```java
// ═══ Forward 路径规则 ═══
// 以 / 开头 → 相对于 Context Root
req.getRequestDispatcher("/WEB-INF/result.jsp").forward(req, resp);
// 不以 / 开头 → 相对于当前 Servlet 映射路径
// 当前 Servlet 映射为 /admin/user
req.getRequestDispatcher("detail.jsp").forward(req, resp);
// → 解析为 /admin/detail.jsp

// ═══ Redirect 路径规则 ═══
// 以 / 开头 → 相对于服务器根路径（不包含 Context Path）
resp.sendRedirect("/app/result");   // 正确：完整路径
resp.sendRedirect("/result");       // 错误：丢失了 /app
// 推荐始终写完整路径，避免歧义
```

### 7.4 典型应用场景

| 场景 | 选择 | 原因 |
|------|------|------|
| 表单提交后查询结果 | **Redirect** | 避免刷新时重复提交（PRG 模式） |
| 登录验证失败回登录页 | **Forward** | 传递错误信息，URL 不变 |
| 访问受保护资源跳转登录 | **Redirect** | 浏览器需要更新地址栏 |
| Servlet → JSP 渲染 | **Forward** | 高效，共享 Request 数据 |
| 跨站跳转 | **Redirect** | Forward 不支持外部 URL |

> 🎯 **PRG 模式（Post-Redirect-Get）**：POST 处理完 → `sendRedirect` 到结果页 → 浏览器 GET 结果页。防止用户刷新时重复提交表单。

---

## 8. Servlet 配置方式演进

### 8.1 web.xml 配置（Servlet 2.x）

```xml
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee" version="4.0">

    <!-- Servlet 声明 -->
    <servlet>
        <servlet-name>UserServlet</servlet-name>
        <servlet-class>com.example.UserServlet</servlet-class>
        <init-param>
            <param-name>encoding</param-name>
            <param-value>UTF-8</param-value>
        </init-param>
        <load-on-startup>1</load-on-startup>  <!-- 启动时加载，数字越小越优先 -->
    </servlet>

    <!-- Servlet 映射 -->
    <servlet-mapping>
        <servlet-name>UserServlet</servlet-name>
        <url-pattern>/user/*</url-pattern>
    </servlet-mapping>

</web-app>
```

### 8.2 注解配置（Servlet 3.0+）

```java
@WebServlet(
    name = "UserServlet",                    // Servlet 名称，可选
    urlPatterns = {"/user", "/member"},     // 等价于 urlPatterns，不能与 value 同时用
    // value = "/user",                     // 等价于 urlPatterns
    initParams = {
        @WebInitParam(name = "encoding", value = "UTF-8"),
        @WebInitParam(name = "pageSize", value = "20")
    },
    loadOnStartup = 1,                      // 启动时加载
    asyncSupported = true                   // 支持异步处理
)
public class UserServlet extends HttpServlet { }
```

### 8.3 编程式注册（Servlet 3.0+ 动态注册）

```java
// 在 ServletContainerInitializer 或 ServletContextListener 中动态注册
@HandlesTypes(MyAppInitializer.class)
public class AppInitializer implements ServletContainerInitializer {
    @Override
    public void onStartup(Set<Class<?>> c, ServletContext ctx) {
        // 动态注册 Servlet
        ServletRegistration.Dynamic userServlet =
            ctx.addServlet("UserServlet", UserServlet.class);
        userServlet.addMapping("/user");
        userServlet.setInitParameter("encoding", "UTF-8");
        userServlet.setLoadOnStartup(1);

        // 动态注册 Filter
        FilterRegistration.Dynamic authFilter =
            ctx.addFilter("AuthFilter", AuthFilter.class);
        authFilter.addMappingForUrlPatterns(
            EnumSet.of(DispatcherType.REQUEST), true, "/admin/*");
    }
}
```

### 8.4 load-on-startup 详解

```java
// 值含义：
//   负数/不设置 → 懒加载，首次请求时才创建（默认）
//   0或正数    → 启动时创建，数字越小越早初始化
//   相同数字   → 容器自定义顺序（不规范，避免使用相同值）

// 使用场景：
//   - 需要预加载资源的 Servlet（如初始化连接池）
//   - 启动时需执行定时任务的 Servlet
//   - Spring MVC 的 DispatcherServlet（必须设为 1）
```

---

## 9. 路径映射规则

### 9.1 四种映射方式

| 映射方式 | 示例 | 匹配规则 | 优先级 |
|---------|------|---------|--------|
| **精确匹配** | `/user/login` | 完全匹配 URL | **最高** |
| **路径匹配** | `/user/*` | 以 `/user/` 开头 | 中 |
| **扩展名匹配** | `*.do` | 以 `.do` 结尾 | 中 |
| **缺省匹配** | `/` | 匹配所有请求 | **最低** |

```java
// 匹配优先级（最长的匹配优先）
// 请求：/user/login
// 候选：/user/login (精确) > /user/* (路径) > *.do (扩展名) > / (缺省)

// ⚠️ 不能同时使用路径匹配和扩展名匹配
// @WebServlet("/user/*.do")  // 非法！部署时抛异常
```

### 9.2 特殊映射

```java
// 缺省 Servlet — 处理所有静态资源和不匹配的请求
@WebServlet("/")
public class DefaultServlet extends HttpServlet { }

// 空字符串 — 映射到 Context Root
@WebServlet("")
public class IndexServlet extends HttpServlet { }

// / 和 /* 的区别
// /     → 覆盖容器的 DefaultServlet（不处理 .jsp）
// /*    → 覆盖所有（包括 .jsp），可能导致 JSP 无法访问
```

### 9.3 Servlet 映射与 Filter 的协作

```java
// Filter 拦截 /* → Servlet 映射 /api/* → 实际路径 /api/user/list
// 请求 /api/user/list：
//   1. Filter 匹配 /*（所有请求都经过）
//   2. Servlet 匹配 /api/*（路径匹配）

// 请求 /static/style.css：
//   1. Filter 匹配 /*
//   2. Servlet 不匹配，由 DefaultServlet 处理静态资源
```

---

## 10. 线程安全与单例多线程模型

### 10.1 Servlet 的线程模型

```
┌───────────────────────────────────────────┐
│              Servlet 容器                   │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐ │
│  │  线程-1  │  │  线程-2  │  │  线程-N  │ │
│  │service() │  │service() │  │service() │ │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘ │
│       │              │              │       │
│       └──────────────┼──────────────┘       │
│                      ▼                      │
│          ┌──────────────────┐               │
│          │ 单例 Servlet 实例  │               │
│          │ - 成员变量不安全   │               │
│          │ - 局部变量安全     │               │
│          └──────────────────┘               │
└───────────────────────────────────────────┘
```

### 10.2 线程安全规则

```java
@WebServlet("/unsafe")
public class UnsafeServlet extends HttpServlet {
    // ❌ 危险：成员变量 — 被所有线程共享
    private String sharedData;       // 线程不安全
    private Connection conn;         // 线程不安全（连接不可重入）
    private StringBuilder sb = new StringBuilder(); // 线程不安全

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        // ✅ 安全：局部变量 — 每个线程独立栈帧
        String localData = req.getParameter("data");  // 线程安全
        int localCount = 0;                            // 线程安全

        // ❌ 危险：写共享成员变量
        sharedData = localData;    // 竞态条件！另一个线程可能覆盖

        // ✅ 安全：只读共享成员变量（必须保证初始化后不变）
        // 在 init() 中初始化且之后不再修改的成员变量是安全的
    }

    // ✅ 安全做法：使用 ThreadLocal
    private ThreadLocal<String> threadLocalData = new ThreadLocal<>();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        threadLocalData.set(req.getParameter("data"));
        // ... 处理
        threadLocalData.remove();  // ⚠️ 务必清理，避免内存泄漏
    }
}
```

### 10.3 线程安全方案对比

| 方案 | 复杂度 | 性能 | 适用场景 |
|------|--------|------|---------|
| **避免成员变量** | 低 | 好 | 首选，应作为默认习惯 |
| **局部变量** | 低 | 好 | 所有请求相关数据 |
| **ThreadLocal** | 中 | 较好 | 线程级上下文（如当前用户） |
| **synchronized** | 中 | 差 | 有共享状态必须同步时 |
| **SingleThreadModel** | 低 | 极差 | ❌ 已废弃，不要用 |

> ⚠️ **SingleThreadModel**：Servlet 2.x 的接口，容器为每个请求创建新实例。性能极差，Servlet 2.4 已废弃。绝对不要使用！

### 10.4 Spring MVC 的线程安全

```java
// Spring MVC Controller 默认也是单例，同样需要注意线程安全：
@RestController
public class UserController {
    // ❌ 危险：成员变量
    private String currentUser;  // 多线程共享，不安全

    // ✅ 安全：通过方法参数接收（实际上是 Request 域属性）
    @GetMapping("/user")
    public String getUser(@RequestParam String userId) {
        return userId;  // userId 是局部变量，线程安全
    }
}
```

---

## 11. Servlet 3.0+ 新特性

### 11.1 异步处理

```java
@WebServlet(value = "/async", asyncSupported = true)
public class AsyncServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        // 进入异步模式
        AsyncContext asyncContext = req.startAsync();
        // 设置超时（毫秒），超时后自动调用 complete()
        asyncContext.setTimeout(30000);

        // 提交异步任务到线程池
        asyncContext.start(() -> {
            try {
                // 模拟耗时业务（远程调用、消息队列等）
                Thread.sleep(2000);
                // 异步完成后写入响应
                asyncContext.getResponse().getWriter().write("Async Result");
                // 通知容器异步处理完成
                asyncContext.complete();
            } catch (Exception e) {
                // 异常处理
                e.printStackTrace();
            }
        });
        // 此时 doGet 方法返回，容器线程释放回线程池
        // 但仍保持客户端连接，等待异步任务完成
    }
}
```

### 11.2 文件上传（Part API）

```java
@WebServlet("/upload")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024,     // 1MB 内存阈值
    maxFileSize = 1024 * 1024 * 10,      // 单个文件最大 10MB
    maxRequestSize = 1024 * 1024 * 50,   // 整个请求最大 50MB
    location = "/tmp/upload"             // 临时存储目录
)
public class UploadServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // 获取上传的文件
        Part filePart = req.getPart("file");  // 根据表单 name 获取
        String fileName = filePart.getSubmittedFileName();
        String contentType = filePart.getContentType();
        long size = filePart.getSize();

        // 保存到目标路径
        String savePath = "/app/data/uploads/" + fileName;
        filePart.write(savePath);

        // 获取其他表单字段
        String description = req.getParameter("description");
        // ⚠️ getParameter 在 multipart 请求中可用（Servlet 3.0+）

        // 获取所有文件
        for (Part part : req.getParts()) {
            String name = part.getName();           // 表单字段名
            String submittedName = part.getSubmittedFileName(); // 文件名
            if (submittedName != null) {
                // 这是一个文件
                part.write("/uploads/" + submittedName);
            } else {
                // 这是一个普通表单字段
                String value = req.getParameter(name);
            }
        }
    }
}
```

### 11.3 Web 碎片化（web-fragment.xml）

```xml
<!-- 第三方 jar 包中的 META-INF/web-fragment.xml -->
<web-fragment>
    <servlet>
        <servlet-name>ThirdPartyServlet</servlet-name>
        <servlet-class>com.thirdparty.Servlet</servlet-class>
    </servlet>
    <servlet-mapping>
        <servlet-name>ThirdPartyServlet</servlet-name>
        <url-pattern>/third/*</url-pattern>
    </servlet-mapping>
</web-fragment>
```

> 💡 jar 包可自带 `web-fragment.xml`，应用启动时自动合并，无需手动配置。类似 Spring Boot 的自动配置思想。

---

## 12. 常见面试题

### Q1：Servlet 生命周期？

> 加载 → 实例化（构造方法） → 初始化（`init()`，1次） → 服务（`service()`→`doGet/doPost`，N次） → 销毁（`destroy()`，1次）。

### Q2：Forward 和 Redirect 的区别？

> Forward 是服务器内部跳转（1次请求，URL 不变，可共享 Request）；Redirect 是浏览器跳转（2次请求，URL 变，数据丢失）。详见第7节。

### Q3：Servlet 是线程安全的吗？

> **默认不是**。Servlet 是单例多线程模型，成员变量被所有请求线程共享，存在线程安全问题。应避免使用成员变量，或使用 ThreadLocal。

### Q4：doGet 和 doPost 的区别？

> GET 参数在 URL 中（有长度限制、不安全），POST 参数在 Body 中（无限制、相对安全）。GET 幂等（应只查询），POST 非幂等（通常用于增删改）。

### Q5：ServletContext 和 ServletConfig 的区别？

> ServletConfig 是每个 Servlet 独有（局部配置），ServletContext 是整个应用共享（全局上下文）。Config 通过 `getServletContext()` 可以获取 Context，但反之不行。
