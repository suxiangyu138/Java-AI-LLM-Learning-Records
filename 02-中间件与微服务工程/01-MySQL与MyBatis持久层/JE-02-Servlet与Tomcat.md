# Servlet 与 Tomcat

> **没有 Servlet 就没有 Java Web 开发。所有上层框架（Spring MVC、JAX-RS）都是建立在 Servlet 规范之上的抽象。**

---

## 目录

- [1. Web 服务器与 Servlet 容器](#1-web-服务器与-servlet-容器)
- [2. Servlet 规范版本演进](#2-servlet-规范版本演进)
- [3. Servlet 生命周期](#3-servlet-生命周期)
- [4. Servlet API 详解](#4-servlet-api-详解)
- [5. Servlet 配置：web.xml vs 注解](#5-servlet-配置webxml-vs-注解)
- [6. Filter 过滤器](#6-filter-过滤器)
- [7. Listener 监听器](#7-listener-监听器)
- [8. RequestDispatcher](#8-requestdispatcher)
- [9. Tomcat 架构深度解析](#9-tomcat-架构深度解析)
- [10. Tomcat 连接器（Connector）](#10-tomcat-连接器connector)
- [11. Tomcat 类加载机制](#11-tomcat-类加载机制)
- [12. Embedded Tomcat（嵌入式 Tomcat）](#12-embedded-tomcat嵌入式-tomcat)
- [13. JSP 基础（了解）](#13-jsp-基础了解)
- [14. Thymeleaf 模板引擎](#14-thymeleaf-模板引擎)
- [15. 实战：Servlet + Thymeleaf 登录注册系统](#15-实战servlet--thymeleaf-登录注册系统)
- [16. 最佳实践与常见陷阱](#16-最佳实践与常见陷阱)
- [17. 面试题](#17-面试题)

---

## 1. Web 服务器与 Servlet 容器

### 1.1 什么是 Web 服务器？

Web 服务器接收 HTTP 请求并返回 HTTP 响应。主要功能：

```
Web 服务器的职责：
┌─────────────────────────────────────────────────────┐
│                    Web Server                         │
│                                                      │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │
│  │  静态资源    │  │  HTTP 协议  │  │  连接管理    │  │
│  │  处理        │  │  解析/封装  │  │  线程池      │  │
│  └─────────────┘  └─────────────┘  └─────────────┘  │
│                                                      │
│  ┌──────────────────────────────────────────────┐    │
│  │  如果请求需要动态处理 → 交给 Servlet 容器      │    │
│  └──────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────┘
```

### 1.2 什么是 Servlet 容器？

Servlet 容器是 Web 服务器的一部分（或与 Web 服务器集成），负责：

```
Servlet 容器的职责：
┌─────────────────────────────────────────────────────┐
│                  Servlet Container                    │
│                                                      │
│  1. Servlet 生命周期管理                              │
│     init() → service() → destroy()                   │
│                                                      │
│  2. 请求/响应对象封装                                 │
│     原始的 HTTP 请求 → HttpServletRequest            │
│     原始的 HTTP 响应 → HttpServletResponse           │
│                                                      │
│  3. 多线程请求处理                                    │
│     每个请求一个线程 → 调用 servlet.service()         │
│                                                      │
│  4. 安全管理                                         │
│     web.xml 安全约束、角色映射                        │
│                                                      │
│  5. 会话管理                                         │
│     Session 创建/销毁/超时                           │
│                                                      │
│  6. JSP 解析                                         │
│     JSP → Servlet → 编译 → 执行                       │
└─────────────────────────────────────────────────────┘
```

### 1.3 常见 Web 服务器 / Servlet 容器

| 产品 | 类型 | Servlet 版本 | 特点 |
|------|------|-------------|------|
| **Tomcat** | Servlet 容器 + Web 服务器 | 6.0 (Tomcat 10.x) | 轻量、主流、内嵌 Spring Boot |
| **Jetty** | Servlet 容器 + Web 服务器 | 6.0 | 更轻量、适合嵌入式 |
| **Undertow** | Servlet 容器 + Web 服务器 | 6.0 | 高并发、JBoss 出品 |
| **WildFly** (JBoss) | 完整 Java EE 应用服务器 | 6.0 | 全栈、EJB 支持 |
| **WebLogic** | 完整 Java EE 应用服务器 | 6.0 | Oracle 商业、企业级 |
| **WebSphere** | 完整 Java EE 应用服务器 | 6.0 | IBM 商业、大型企业 |

> **生产环境选择建议：**
> - 微服务架构 → **Tomcat**（内嵌在 Spring Boot 中）
> - 高并发 Web → **Undertow**
> - 嵌入式场景 → **Jetty**

---

## 2. Servlet 规范版本演进

### 2.1 版本时间线

| Servlet 版本 | 发布年份 | Java EE / Jakarta EE | 核心特性 |
|-------------|---------|---------------------|---------|
| 2.3 | 2000 | J2EE 1.3 | Filter、Listener |
| 2.4 | 2003 | J2EE 1.4 | web.xml Schema、Filter 增强 |
| 2.5 | 2005 | Java EE 5 | 注解支持雏形 |
| **3.0** | 2009 | Java EE 6 | **@WebServlet、@WebFilter、@WebListener**、可插拔、异步处理 |
| **3.1** | 2013 | Java EE 7 | **非阻塞 I/O**、HTTP 协议升级、安全增强 |
| **4.0** | 2017 | Java EE 8 | **HTTP/2 支持**、Server Push |
| **5.0** | 2020 | Jakarta EE 9 | **包名迁移**：javax.servlet → jakarta.servlet |
| **6.0** | 2022 | Jakarta EE 10 | 最低 Java 11、请求/响应对象增强 |

### 2.2 关键版本里程碑详解

**Servlet 3.0 (2009)：Web.xml 时代的终结**

```java
// 以前（Servlet 2.5）：必须配置 web.xml
// web.xml
// <servlet>
//     <servlet-name>userServlet</servlet-name>
//     <servlet-class>com.example.UserServlet</servlet-class>
// </servlet>
// <servlet-mapping>
//     <servlet-name>userServlet</servlet-name>
//     <url-pattern>/users</url-pattern>
// </servlet-mapping>

// Servlet 3.0+：使用注解
@WebServlet("/users")
public class UserServlet extends HttpServlet {
    // ...
}
```

**Servlet 3.1 (2013)：非阻塞 I/O**

```java
// 非阻塞读取请求体
AsyncContext ctx = request.startAsync();
ServletInputStream input = request.getInputStream();
input.setReadListener(new ReadListener() {
    @Override
    public void onDataAvailable() {
        // 数据可用时回调
    }
    @Override
    public void onAllDataRead() {
        ctx.complete();
    }
    @Override
    public void onError(Throwable t) {
        // 错误处理
    }
});
```

**Servlet 4.0 (2017)：HTTP/2 支持**

```java
// Server Push（服务器推送）
if (request.isPushSupported()) {
    PushBuilder pushBuilder = request.newPushBuilder();
    pushBuilder.path("css/style.css").push();
    pushBuilder.path("js/app.js").push();
}
```

**Servlet 5.0/6.0：Jakarta EE 迁移**

```java
// 4.0 及之前 (Java EE)
import javax.servlet.http.HttpServlet;
import javax.servlet.annotation.WebServlet;

// 5.0+ (Jakarta EE)
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.annotation.WebServlet;
```

---

## 3. Servlet 生命周期

### 3.1 生命周期图示

```
                 ┌──────────────────────┐
                 │   Servlet 加载        │
                 │   (类加载器加载 class) │
                 └──────────┬───────────┘
                            │
                 ┌──────────▼───────────┐
                 │  实例化（构造方法）    │
                 │  第一次请求或启动时    │
                 └──────────┬───────────┘
                            │
                 ┌──────────▼───────────┐
                 │  init(ServletConfig)  │  ← 只调用一次
                 │  初始化参数、资源      │
                 └──────────┬───────────┘
                            │
              ┌─────────────┼─────────────┐
              │             │             │
           ┌──▼──┐      ┌──▼──┐      ┌──▼──┐
           │请求1│      │请求2│      │请求N│    ← 多线程并发
           └──┬──┘      └──┬──┘      └──┬──┘
              │             │             │
           ┌──▼────────────▼────────────▼──┐
           │  service(request, response)    │  ← 每次请求调用
           │  根据 method 分发:              │
           │  doGet / doPost / doPut / ...  │
           └───────────────────────────────┘
                            │
                 ┌──────────▼───────────┐
                 │     destroy()         │  ← 只调用一次
                 │  容器关闭/卸载时      │
                 └──────────────────────┘
```

### 3.2 init() 详解

```java
@WebServlet("/config-demo")
public class ConfigServlet extends HttpServlet {

    private String uploadPath;
    private int maxFileSize;

    @Override
    public void init(ServletConfig config) throws ServletException {
        // 必须调用父类方法
        super.init(config);

        // 读取 web.xml 中的 init-param
        // 方式1：从 ServletConfig 读取
        uploadPath = config.getInitParameter("uploadPath");
        maxFileSize = Integer.parseInt(config.getInitParameter("maxFileSize"));

        // 方式2：从 ServletContext 读取（全局参数）
        ServletContext context = config.getServletContext();
        String globalConfig = context.getInitParameter("globalConfig");

        // 初始化资源
        System.out.println("Servlet 初始化完成, 上传路径: " + uploadPath);
    }

    @Override
    public void init() throws ServletException {
        // 如果不需要 ServletConfig，可以用无参的 init()
        // 此方法会被 GenericServlet 重写为调用 init(ServletConfig)
    }
}
```

**init() 的执行时机：**

```
load-on-startup 配置：
  web.xml: <load-on-startup>1</load-on-startup>
  注解:    @WebServlet(value="/demo", loadOnStartup=1)

  数值含义：
   - 负数或未配置：第一次请求时初始化（懒加载）
   0 或正数：服务器启动时按数值从小到大初始化
```

### 3.3 service() 详解

```java
@Override
public void service(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
    // HttpServlet 的 service() 默认实现：
    String method = req.getMethod();
    if ("GET".equals(method)) {
        doGet(req, resp);
    } else if ("POST".equals(method)) {
        doPost(req, resp);
    } else if ("PUT".equals(method)) {
        doPut(req, resp);
    } else if ("DELETE".equals(method)) {
        doDelete(req, resp);
    } else {
        // 其他方法返回 405 Method Not Allowed
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
}

// 如果不想区分 HTTP 方法，可以重写 service()
@WebServlet("/gateway")
public class GatewayServlet extends HttpServlet {
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // 统一处理所有 HTTP 方法
        resp.setContentType("application/json");
        String method = req.getMethod();
        String path = req.getPathInfo();
        // 路由分发...
    }
}
```

### 3.4 destroy() 详解

```java
@Override
public void destroy() {
    // 释放资源：关闭数据库连接、停止线程池等
    System.out.println("Servlet 正在销毁...");
    // 不需要调用 super.destroy()
}
```

**destroy() 触发条件：**
- Tomcat 正常关闭
- 应用重新部署
- 从容器中移除

### 3.5 Servlet 线程安全问题

**关键：** Servlet 是**单实例多线程**的。只有一个 Servlet 实例，但多个线程可同时执行 service()。

```java
@WebServlet("/thread-safety")
public class UnsafeServlet extends HttpServlet {

    // 危险：实例变量会被多个线程共享
    private int counter = 0;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        // 线程不安全！
        counter++;
        resp.getWriter().write("Counter: " + counter);
    }
}
```

**安全做法：**

```java
@WebServlet("/thread-safety")
public class SafeServlet extends HttpServlet {

    // 安全：局部变量（每个请求有自己的栈）
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        int counter = 0;  // 每个请求独立
        counter++;
        resp.getWriter().write("Counter: " + counter);
    }

    // 安全：使用 ThreadLocal
    private static final ThreadLocal<User> currentUser = new ThreadLocal<>();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        User user = currentUser.get();
        // 每个线程有自己独立的 User 副本
        currentUser.remove();
    }

    // 安全：无状态对象
    private final UserService userService = new UserService();
    // UserService 内部不持有与服务方法无关的状态
}

// ★ 核心原则：Servlet 中不要使用实例变量存储请求相关的数据
```

---

## 4. Servlet API 详解

### 4.1 HttpServletRequest

```java
@WebServlet("/request-demo")
public class RequestDemoServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        // ========== 请求行信息 ==========
        String method = req.getMethod();              // GET / POST ...
        String requestURI = req.getRequestURI();      // /app/users/123
        StringBuffer requestURL = req.getRequestURL();// http://host/app/users/123
        String queryString = req.getQueryString();    // page=1&size=10
        String protocol = req.getProtocol();          // HTTP/1.1
        String contextPath = req.getContextPath();    // /app (应用上下文路径)
        String servletPath = req.getServletPath();    // /users
        String pathInfo = req.getPathInfo();          // /123

        // ========== 参数读取 ==========
        // GET 参数：URL query string
        // POST 参数：表单数据
        String username = req.getParameter("username");
        String[] hobbies = req.getParameterValues("hobby");  // 多值参数
        Map<String, String[]> paramMap = req.getParameterMap();

        // ========== 请求头 ==========
        String userAgent = req.getHeader("User-Agent");
        String contentType = req.getHeader("Content-Type");
        String authorization = req.getHeader("Authorization");
        String referer = req.getHeader("Referer");
        String origin = req.getHeader("Origin");

        // 遍历所有请求头
        Enumeration<String> headerNames = req.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            String value = req.getHeader(name);
        }

        // ========== Cookie ==========
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("JSESSIONID".equals(cookie.getName())) {
                    String sessionId = cookie.getValue();
                }
            }
        }

        // ========== Session ==========
        HttpSession session = req.getSession();          // 获取/创建 Session
        HttpSession existing = req.getSession(false);    // 仅获取（不创建）
        session.setAttribute("key", "value");
        Object value = session.getAttribute("key");
        String sessionId = session.getId();
        session.invalidate();                             // 使 Session 失效

        // ========== 请求体读取（POST/PUT） ==========
        // 读字符串
        BufferedReader reader = req.getReader();
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            body.append(line);
        }

        // 读二进制
        ServletInputStream inputStream = req.getInputStream();
        byte[] bytes = inputStream.readAllBytes();

        // ========== 请求属性（服务器内部传递数据） ==========
        req.setAttribute("user", new User());            // 设置属性
        Object user = req.getAttribute("user");          // 获取属性
        req.removeAttribute("user");                     // 移除属性

        // ========== 国际化 ==========
        Locale locale = req.getLocale();                 // 客户端首选语言
        Enumeration<Locale> locales = req.getLocales();  // 所有语言

        // ========== 字符编码 ==========
        req.setCharacterEncoding("UTF-8");               // 设置请求体编码
        // 注意：必须在读取参数之前设置

        // ========== 其他 ==========
        String remoteAddr = req.getRemoteAddr();         // 客户端 IP
        int remotePort = req.getRemotePort();            // 客户端端口
        String serverName = req.getServerName();         // 服务器主机名
        int serverPort = req.getServerPort();            // 服务器端口
        boolean isSecure = req.isSecure();               // 是否 HTTPS
        String scheme = req.getScheme();                 // http/https
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        // 读取 JSON 请求体
        BufferedReader reader = req.getReader();
        String jsonBody = reader.lines().collect(Collectors.joining());

        // 手动 JSON 解析（生产环境使用 Jackson/Gson）
        // ObjectMapper mapper = new ObjectMapper();
        // User user = mapper.readValue(jsonBody, User.class);
    }
}
```

### 4.2 HttpServletResponse

```java
@WebServlet("/response-demo")
public class ResponseDemoServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        // ========== 设置状态码 ==========
        resp.setStatus(HttpServletResponse.SC_OK);       // 200
        // 或者直接指定数字
        resp.setStatus(200);
        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "用户不存在");  // 404

        // ========== 设置响应头 ==========
        resp.setHeader("Content-Type", "application/json;charset=utf-8");
        resp.setHeader("Cache-Control", "no-cache");
        resp.setHeader("X-Custom-Header", "custom-value");

        // 快捷方法
        resp.setContentType("application/json;charset=utf-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentLength(1024);                     // Content-Length
        resp.setDateHeader("Last-Modified", System.currentTimeMillis());

        // ========== 添加 Cookie ==========
        Cookie cookie = new Cookie("theme", "dark");
        cookie.setMaxAge(3600);          // 1 小时
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        resp.addCookie(cookie);

        // 快捷设置 Session Cookie
        HttpSession session = req.getSession();

        // ========== 重定向 ==========
        resp.sendRedirect("/login");               // 302 临时重定向
        resp.sendRedirect("https://www.example.com");

        // ========== 输出响应体 ==========
        // 方式1：字符输出（文本）
        resp.setContentType("text/html;charset=utf-8");
        PrintWriter writer = resp.getWriter();
        writer.println("<html>");
        writer.println("<body><h1>Hello Servlet</h1></body>");
        writer.println("</html>");
        writer.flush();

        // 方式2：二进制输出（文件下载等）
        resp.setContentType("application/octet-stream");
        resp.setHeader("Content-Disposition", "attachment; filename=\"report.pdf\"");
        ServletOutputStream out = resp.getOutputStream();
        byte[] fileBytes = readFile();  // 读取文件
        out.write(fileBytes);
        out.flush();
    }

    // ========== 响应头设置注意事项 ==========
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        // 正确顺序：
        resp.setContentType("application/json");    // 1. 先设置头
        resp.setCharacterEncoding("UTF-8");         // 2. 再设置编码

        // 获取输出流
        PrintWriter writer = resp.getWriter();

        // 输出 JSON
        writer.write("{\"status\":\"ok\"}");

        // ★ 注意：
        // 1. getWriter() 和 getOutputStream() 不能同时调用
        // 2. 响应头必须在 getWriter()/getOutputStream() 之前设置
        // 3. 响应提交后设置的响应头将无效
        // 4. 提交后不能再设置响应头（状态码会变为 500）
    }

    private byte[] readFile() {
        return new byte[0];
    }
}
```

### 4.3 ServletConfig vs ServletContext

```java
// ServletConfig：单个 Servlet 的配置
@WebServlet(value = "/config-demo", initParams = {
    @WebInitParam(name = "appName", value = "MyApp"),
    @WebInitParam(name = "version", value = "1.0")
})
public class ConfigDemoServlet extends HttpServlet {

    @Override
    public void init(ServletConfig config) {
        String appName = config.getInitParameter("appName");
        String version = config.getInitParameter("version");
        // 获取 Servlet 名称
        String servletName = config.getServletName();
        // 获取 ServletContext
        ServletContext context = config.getServletContext();
    }
}

// ServletContext：整个 Web 应用的共享配置
// 在 web.xml 中配置：
// <context-param>
//     <param-name>globalUploadPath</param-name>
//     <param-value>/data/uploads</param-value>
// </context-param>
//
// 或者在任意 Servlet/Filter/Listener 中访问：

@WebServlet("/context-demo")
public class ContextDemoServlet extends HttpServlet {

    @Override
    public void init() {
        ServletContext context = getServletContext();

        // 读取全局初始化参数
        String uploadPath = context.getInitParameter("globalUploadPath");

        // 设置/获取应用级属性
        context.setAttribute("appStartTime", System.currentTimeMillis());
        context.setAttribute("dataSource", dataSource);
        Object ds = context.getAttribute("dataSource");

        // 获取资源路径
        String realPath = context.getRealPath("/WEB-INF/config.properties");
        // 开发环境返回 null（未打包的 WAR）
        // 生产环境返回实际文件系统路径

        // 获取资源流
        InputStream is = context.getResourceAsStream("/WEB-INF/config.properties");

        // 获取 MIME 类型
        String mimeType = context.getMimeType("report.pdf"); // application/pdf

        // 日志记录
        context.log("应用初始化完成");

        // 转发请求
        // context.getRequestDispatcher("/index.jsp").forward(req, resp);
    }
}
```

| 特性 | ServletConfig | ServletContext |
|------|--------------|---------------|
| 作用域 | 单个 Servlet | 整个 Web 应用 |
| 访问方式 | getServletConfig() | getServletContext() |
| 初始化参数 | `@WebInitParam` 或 `<init-param>` | `<context-param>` |
| 生命周期 | 与 Servlet 一致 | 与应用一致（最久） |
| 线程安全 | 单个 Servlet 访问 | 多 Servlet 并发访问 |

---

## 5. Servlet 配置：web.xml vs 注解

### 5.1 web.xml 方式（传统，Servlet 2.5-）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee
                             http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd"
         version="4.0">

    <!-- 应用元信息 -->
    <display-name>My Web Application</display-name>
    <description>Enterprise Web Application</description>

    <!-- 全局初始化参数 -->
    <context-param>
        <param-name>uploadPath</param-name>
        <param-value>/data/uploads</param-value>
    </context-param>

    <!-- Servlet 声明 -->
    <servlet>
        <servlet-name>userServlet</servlet-name>
        <servlet-class>com.example.UserServlet</servlet-class>
        <init-param>
            <param-name>pageSize</param-name>
            <param-value>20</param-value>
        </init-param>
        <load-on-startup>1</load-on-startup>
        <async-supported>true</async-supported>
    </servlet>

    <!-- Servlet 映射 -->
    <servlet-mapping>
        <servlet-name>userServlet</servlet-name>
        <url-pattern>/users/*</url-pattern>
    </servlet-mapping>

    <!-- Filter -->
    <filter>
        <filter-name>encodingFilter</filter-name>
        <filter-class>com.example.EncodingFilter</filter-class>
        <init-param>
            <param-name>encoding</param-name>
            <param-value>UTF-8</param-value>
        </init-param>
    </filter>
    <filter-mapping>
        <filter-name>encodingFilter</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>

    <!-- Listener -->
    <listener>
        <listener-class>com.example.AppContextListener</listener-class>
    </listener>

    <!-- Session 配置 -->
    <session-config>
        <session-timeout>30</session-timeout>
        <cookie-config>
            <http-only>true</http-only>
            <secure>true</secure>
        </cookie-config>
        <tracking-mode>COOKIE</tracking-mode>
    </session-config>

    <!-- 欢迎页面 -->
    <welcome-file-list>
        <welcome-file>index.html</welcome-file>
        <welcome-file>index.jsp</welcome-file>
    </welcome-file-list>

    <!-- 错误页面 -->
    <error-page>
        <error-code>404</error-code>
        <location>/error/404.html</location>
    </error-page>
    <error-page>
        <exception-type>java.lang.Exception</exception-type>
        <location>/error/500.jsp</location>
    </error-page>

    <!-- 请求编码 -->
    <request-character-encoding>UTF-8</request-character-encoding>
    <response-character-encoding>UTF-8</response-character-encoding>

</web-app>
```

### 5.2 注解方式（Servlet 3.0+，推荐）

```java
// ========== WebServlet ==========
@WebServlet(
    name = "userServlet",                    // 可选，Servlet 名称
    value = "/users",                        // 或 urlPatterns
    urlPatterns = {"/users", "/users/*"},    // URL 匹配模式
    loadOnStartup = 1,                       // 启动时加载
    initParams = {
        @WebInitParam(name = "pageSize", value = "20"),
        @WebInitParam(name = "sortField", value = "id")
    },
    asyncSupported = true                    // 是否支持异步处理
)
public class UserServlet extends HttpServlet {
    // ...
}

// ========== WebFilter ==========
@WebFilter(
    filterName = "encodingFilter",
    urlPatterns = "/*",
    initParams = {
        @WebInitParam(name = "encoding", value = "UTF-8")
    },
    dispatcherTypes = {
        DispatcherType.REQUEST,
        DispatcherType.FORWARD,
        DispatcherType.INCLUDE
    }
)
public class EncodingFilter implements Filter {
    // ...
}

// ========== WebListener ==========
@WebListener
public class AppContextListener implements ServletContextListener {
    // ...
}

// ========== WebInitParam ==========
// 可使用 @WebInitParam 替代 web.xml <init-param>
```

### 5.3 完全注解：web.xml 的替代

从 Servlet 3.0 开始，可以**完全不需要 web.xml**：

```java
// 实现 ServletContainerInitializer 接口
// 或使用 Spring Boot 的自动配置方式

// 方式1：ServletContainerInitializer
public class MyWebAppInitializer implements ServletContainerInitializer {
    @Override
    public void onStartup(Set<Class<?>> c, ServletContext ctx) {
        // 编程式添加 Servlet
        ServletRegistration.Dynamic servlet = ctx.addServlet("userServlet", UserServlet.class);
        servlet.addMapping("/users/*");
        servlet.setInitParameter("pageSize", "20");
        servlet.setLoadOnStartup(1);

        // 编程式添加 Filter
        FilterRegistration.Dynamic filter = ctx.addFilter("encodingFilter", EncodingFilter.class);
        filter.addMappingForUrlPatterns(null, true, "/*");
        filter.setInitParameter("encoding", "UTF-8");

        // 编程式添加 Listener
        ctx.addListener(AppContextListener.class);
    }
}
```

**在 META-INF/services 中注册：**
```
# 文件: META-INF/services/jakarta.servlet.ServletContainerInitializer
com.example.MyWebAppInitializer
```

---

## 6. Filter 过滤器

### 6.1 Filter 工作机制

```
请求到达 → Filter1 → Filter2 → ... → FilterN → Servlet
                                                      ↓
响应返回 ← Filter1 ← Filter2 ← ... ← FilterN ← 处理完成
```

Filter 是**链式结构**，按配置顺序依次执行。

### 6.2 Filter 生命周期与 API

```java
@WebFilter(filterName = "demoFilter", urlPatterns = "/*")
public class DemoFilter implements Filter {

    // 生命周期：init → doFilter（多次） → destroy

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 初始化：读取配置参数
        String encoding = filterConfig.getInitParameter("encoding");
        FilterConfig config = filterConfig; // 获取 ServletContext
        ServletContext context = filterConfig.getServletContext();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // 请求处理前（前置处理）
        long start = System.currentTimeMillis();

        // 放行：调用下一个 Filter 或目标 Servlet
        chain.doFilter(request, response);

        // 响应处理后（后置处理）
        long duration = System.currentTimeMillis() - start;
        System.out.printf("%s %s - %dms%n",
            req.getMethod(), req.getRequestURI(), duration);
    }

    @Override
    public void destroy() {
        // 清理资源
    }
}
```

### 6.3 实用 Filter 示例

**编码过滤器：**

```java
@WebFilter(filterName = "encodingFilter", urlPatterns = "/*",
           initParams = @WebInitParam(name = "encoding", value = "UTF-8"))
public class EncodingFilter implements Filter {

    private String encoding;

    @Override
    public void init(FilterConfig config) {
        encoding = config.getInitParameter("encoding");
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp,
                         FilterChain chain) throws IOException, ServletException {
        // 设置请求和响应的编码
        req.setCharacterEncoding(encoding);
        resp.setCharacterEncoding(encoding);
        resp.setContentType("text/html;charset=" + encoding);
        chain.doFilter(req, resp);
    }

    @Override
    public void destroy() {}
}
```

**登录校验过滤器：**

```java
@WebFilter(filterName = "loginFilter",
           urlPatterns = {"/admin/*", "/profile/*"})
public class LoginFilter implements Filter {

    // 白名单：不需要登录即可访问的路径
    private static final Set<String> WHITE_LIST = Set.of(
        "/login", "/register", "/api/login", "/api/register",
        "/static/", "/css/", "/js/", "/images/"
    );

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        String path = request.getServletPath();

        // 白名单路径直接放行
        if (isWhiteListed(path)) {
            chain.doFilter(request, response);
            return;
        }

        // 检查用户是否登录
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            // 未登录 → 重定向到登录页面
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // 已登录 → 放行
        chain.doFilter(request, response);
    }

    private boolean isWhiteListed(String path) {
        for (String pattern : WHITE_LIST) {
            if (path.startsWith(pattern)) return true;
        }
        return false;
    }
}
```

**性能监控过滤器：**

```java
@WebFilter(filterName = "performanceFilter", urlPatterns = "/*")
public class PerformanceFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;

        long start = System.nanoTime();
        chain.doFilter(req, resp);
        long duration = System.nanoTime() - start;

        // 慢请求告警（超过 500ms）
        if (duration > 500_000_000) {
            System.err.printf("[SLOW] %s %s - %dms%n",
                request.getMethod(), request.getRequestURI(),
                duration / 1_000_000);
        }
    }
}
```

### 6.4 Filter vs Interceptor（预备 Spring 知识）

| 特性 | Filter（Servlet） | Interceptor（Spring MVC） |
|------|------------------|--------------------------|
| 规范 | Servlet 规范 | Spring MVC 框架 |
| 作用范围 | 所有 Web 资源（Servlet、JSP、静态资源） | 仅 Spring MVC 的 Controller |
| 依赖 | 依赖 Servlet API | 依赖 Spring 容器 |
| 访问 Controller 数据 | 不能 | 可以（ModelAndView 等） |
| 能获取 IOC 容器中的 Bean | 不能（除非特殊配置） | 可以 |

---

## 7. Listener 监听器

### 7.1 Listener 类型

| 监听器接口 | 作用域 | 监听的事件 | 典型用途 |
|-----------|--------|-----------|---------|
| **ServletContextListener** | 应用级别 | Web 应用启动/关闭 | 初始化连接池、加载配置 |
| **ServletContextAttributeListener** | 应用级别 | 应用属性增删改 | 监控属性变更 |
| **HttpSessionListener** | 会话级别 | Session 创建/销毁 | 统计在线人数 |
| **HttpSessionAttributeListener** | 会话级别 | Session 属性增删改 | 监控用户登录/登出 |
| **ServletRequestListener** | 请求级别 | 请求创建/销毁 | 请求日志、计时 |
| **ServletRequestAttributeListener** | 请求级别 | 请求属性增删改 | 监控请求数据 |

### 7.2 Listener 示例

**ServletContextListener：初始化连接池**

```java
@WebListener
public class AppContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        System.out.println("=== 应用启动 ===");

        // 初始化 HikariCP 连接池
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/myapp");
        config.setUsername("root");
        config.setPassword("password");
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);

        HikariDataSource dataSource = new HikariDataSource(config);

        // 存储在 ServletContext 中，全局共享
        context.setAttribute("dataSource", dataSource);
        context.setAttribute("appStartTime", System.currentTimeMillis());

        System.out.println("=== 数据源初始化完成 ===");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();

        // 关闭连接池
        DataSource ds = (DataSource) context.getAttribute("dataSource");
        if (ds instanceof HikariDataSource) {
            ((HikariDataSource) ds).close();
        }

        System.out.println("=== 应用关闭，资源已释放 ===");
    }
}
```

**HttpSessionListener：统计在线用户**

```java
@WebListener
public class SessionListener implements HttpSessionListener {

    // 使用应用级属性存储在线用户统计
    private static final String ONLINE_COUNT = "onlineCount";

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        HttpSession session = se.getSession();
        ServletContext context = session.getServletContext();

        // 原子性的在线计数
        AtomicInteger count = (AtomicInteger) context.getAttribute(ONLINE_COUNT);
        if (count == null) {
            count = new AtomicInteger(0);
            context.setAttribute(ONLINE_COUNT, count);
        }

        int online = count.incrementAndGet();
        System.out.printf("Session 创建: %s, 当前在线: %d%n",
            session.getId(), online);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        HttpSession session = se.getSession();
        ServletContext context = session.getServletContext();

        AtomicInteger count = (AtomicInteger) context.getAttribute(ONLINE_COUNT);
        if (count != null) {
            int online = count.decrementAndGet();
            System.out.printf("Session 销毁: %s, 当前在线: %d%n",
                session.getId(), online);
        }
    }
}
```

**ServletRequestListener：请求日志**

```java
@WebListener
public class RequestLogListener implements ServletRequestListener {

    @Override
    public void requestInitialized(ServletRequestEvent sre) {
        HttpServletRequest request = (HttpServletRequest) sre.getServletRequest();
        request.setAttribute("startTime", System.currentTimeMillis());

        // 日志记录（生产环境使用 logback/log4j）
        System.out.printf("[REQUEST] %s %s from %s%n",
            request.getMethod(),
            request.getRequestURI(),
            request.getRemoteAddr());
    }

    @Override
    public void requestDestroyed(ServletRequestEvent sre) {
        HttpServletRequest request = (HttpServletRequest) sre.getServletRequest();
        Long startTime = (Long) request.getAttribute("startTime");

        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            System.out.printf("[REQUEST] %s %s - %dms%n",
                request.getMethod(),
                request.getRequestURI(),
                duration);
        }
    }
}
```

---

## 8. RequestDispatcher

### 8.1 forward vs include

```java
@WebServlet("/dispatcher-demo")
public class DispatcherDemoServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // ========== forward：服务器内部转发 ==========
        // 浏览器 URL 不变，一次请求，一次响应
        // 可以共享 request 属性

        // 方式1：通过 ServletContext
        RequestDispatcher dispatcher1 = getServletContext()
            .getRequestDispatcher("/WEB-INF/views/user/profile.jsp");
        dispatcher1.forward(req, resp);
        // 注意：只能转发到当前应用内的资源

        // 方式2：通过 request
        RequestDispatcher dispatcher2 = req
            .getRequestDispatcher("/user/detail?id=123");
        dispatcher2.forward(req, resp);

        // ========== include：包含另一个资源 ==========
        // 类似 forward，但执行完后会回到原 Servlet 继续执行
        RequestDispatcher dispatcher = req
            .getRequestDispatcher("/common/header.html");
        dispatcher.include(req, resp);

        // include 返回后会继续执行
        PrintWriter out = resp.getWriter();
        out.println("<div>主内容</div>");
    }
}
```

### 8.2 forward vs redirect 对比

| 特性 | forward | sendRedirect |
|------|---------|-------------|
| 类型 | 服务器端内部转发 | 客户端重定向 |
| 浏览器 URL | 不改变 | 改变为目标 URL |
| 请求次数 | 1 次 | 2 次（302 + 目标请求） |
| 共享数据 | 可以（request 属性） | 不可以（除非用 Session） |
| 目标范围 | 同一应用内 | 任意 URL（可跨域） |
| 资源类型 | Servlet/JSP/HTML | 任意 URL |
| 路径写法 | 应用内路径（如 /users） | 相对或绝对 URL |

```
forward：客户端                   服务器 A
          │                      │
          │  请求 /users/123      │
          │─────────────────────>│ forward /user/detail
          │                      │─────────────┐
          │                      │ 内部转发     │
          │                      │<─────────────┘
          │  响应（200）           │
          │<─────────────────────│

redirect：客户端                   服务器 A
          │                      │
          │  请求 /users/123      │
          │─────────────────────>│
          │  302 Location: /login│
          │<─────────────────────│
          │                      │
          │  请求 /login（浏览器自动）│
          │─────────────────────>│
```

**选择原则：**
- 用 `forward` 的场景：服务器内部跳转、需要共享 request 数据、URL 不应暴露
- 用 `redirect` 的场景：登录成功后跳转、防止表单重复提交（Post/Redirect/Get）、跳转到外部 URL

---

## 9. Tomcat 架构深度解析

### 9.1 Tomcat 整体架构

```
┌──────────────────────────────────────────────────────────────┐
│                         Tomcat Server                         │
│  ┌────────────────────────────────────────────────────────┐  │
│  │                      Service                            │  │
│  │  ┌──────────────────┐   ┌──────────────────────────┐   │  │
│  │  │    Connector      │   │  Engine                  │   │  │
│  │  │  (HTTP/1.1,       │   │  ┌─────────────────────┐ │   │  │
│  │  │   AJP, HTTPS)     │──>│  │  Host (localhost)    │ │   │  │
│  │  │                   │   │  │  ┌─────────────────┐ │ │   │  │
│  │  │  Thread Pool:     │   │  │  │  Context (/app) │ │ │   │  │
│  │  │  NIO/NIO2/APR     │   │  │  │  ┌───────────┐  │ │ │   │  │
│  │  └──────────────────┘   │  │  │  │  Wrapper   │  │ │ │   │  │
│  │  ┌──────────────────┐   │  │  │  │ (Servlet)  │  │ │ │   │  │
│  │  │    Connector      │   │  │  │  └───────────┘  │ │ │   │  │
│  │  │  (HTTP/1.1)       │──>│  │  │  ┌───────────┐  │ │ │   │  │
│  │  └──────────────────┘   │  │  │  │  Wrapper   │  │ │ │   │  │
│  │                         │  │  │  │ (Servlet)  │  │ │ │   │  │
│  │                         │  │  │  └───────────┘  │ │ │   │  │
│  │                         │  │  └─────────────────┘ │ │   │  │
│  │                         │  └─────────────────────┘ │   │  │
│  └─────────────────────────┘──────────────────────────┘   │  │
└──────────────────────────────────────────────────────────────┘
```

### 9.2 组件详解

**Server：** Tomcat 实例的顶层组件，代表整个 Tomcat 进程。

**Service：** 将 Connector 和 Engine 组合在一起的容器。一个 Server 可以有多个 Service。

**Connector：** 处理网络连接，解析 HTTP 请求，生成 Request/Response 对象。

**Engine：** Servlet 容器核心，处理所有请求。每个 Service 只有一个 Engine。

**Host：** 代表一个虚拟主机（域名）。根据请求的 Host 头选择。例如 `localhost`、`www.example.com`。

**Context：** 代表一个 Web 应用。每个 Context 对应一个 WAR 包或应用目录。

**Wrapper：** 代表一个 Servlet。是容器层级中最底层。

### 9.3 配置示例（server.xml）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Server port="8005" shutdown="SHUTDOWN">

    <!-- 全局资源 -->
    <GlobalNamingResources>
        <Resource name="UserDatabase"
                  auth="Container"
                  type="org.apache.catalina.UserDatabase"
                  description="User database"
                  factory="org.apache.catalina.users.MemoryUserDatabaseFactory"
                  pathname="conf/tomcat-users.xml"/>
    </GlobalNamingResources>

    <!-- Service 1: HTTP 服务 -->
    <Service name="Catalina">

        <!-- HTTP Connector -->
        <Connector port="8080"
                   protocol="HTTP/1.1"
                   connectionTimeout="20000"
                   maxConnections="10000"
                   maxThreads="200"
                   minSpareThreads="10"
                   acceptCount="100"
                   redirectPort="8443"/>

        <!-- HTTPS Connector -->
        <Connector port="8443"
                   protocol="org.apache.coyote.http11.Http11NioProtocol"
                   maxThreads="150"
                   SSLEnabled="true">
            <SSLHostConfig>
                <Certificate certificateKeystoreFile="conf/keystore.jks"
                             certificateKeystorePassword="changeit"
                             type="RSA"/>
            </SSLHostConfig>
        </Connector>

        <!-- AJP Connector（用于 Apache HTTP Server 集成） -->
        <Connector port="8009"
                   protocol="AJP/1.3"
                   redirectPort="8443"/>

        <!-- Engine -->
        <Engine name="Catalina" defaultHost="localhost">

            <!-- Host -->
            <Host name="localhost"
                  appBase="webapps"
                  unpackWARs="true"
                  autoDeploy="true">

                <!-- 阀门：访问日志 -->
                <Valve className="org.apache.catalina.valves.AccessLogValve"
                       directory="logs"
                       prefix="localhost_access_log"
                       suffix=".txt"
                       pattern="%h %l %u %t &quot;%r&quot; %s %b"/>

                <!-- 阀门：请求过滤 -->
                <Valve className="org.apache.catalina.valves.RemoteAddrValve"
                       deny="10\.0\.0\.\d+"/>
            </Host>
        </Engine>
    </Service>
</Server>
```

---

## 10. Tomcat 连接器（Connector）

### 10.1 I/O 模型演进

| I/O 模型 | 类名 | 版本 | 特点 |
|---------|------|------|------|
| **BIO** | Http11Protocol | Tomcat 7 默认 | 每个连接一个线程，连接数少时简单 |
| **NIO** | Http11NioProtocol | Tomcat 8+ 默认 | 非阻塞 I/O，少量线程处理大量连接 |
| **NIO2** | Http11Nio2Protocol | 可选 | 异步 I/O，性能略优于 NIO |
| **APR** | Http11AprProtocol | 需安装 APR 库 | 使用 C 语言 Native 库，性能最高 |

### 10.2 NIO 线程模型

```
┌─────────────────────────────────────────────────────────────┐
│                   Tomcat NIO Thread Model                    │
│                                                              │
│  Acceptor（1-N 个线程）                                      │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  接受新 TCP 连接，放到队列中                           │   │
│  └──────────────────────────────────────────────────────┘   │
│                           │                                  │
│  Poller（1-2 个线程）                                       │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  轮询 socket 的可读/可写事件                           │   │
│  │  当数据可读时，将事件封装后交给 Worker 线程             │   │
│  └──────────────────────────────────────────────────────┘   │
│                           │                                  │
│  Worker（线程池）                                           │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Thread 1  │  Thread 2  │  Thread 3  │  ...          │   │
│  │  doGet()   │  doPost()  │  doPut()   │               │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 10.3 连接器关键配置

```xml
<Connector port="8080"
           protocol="org.apache.coyote.http11.Http11NioProtocol"
           connectionTimeout="20000"         # 连接超时（毫秒）
           maxConnections="10000"            # 最大连接数
           maxThreads="200"                  # 最大工作线程数
           minSpareThreads="10"              # 最小空闲线程数
           acceptCount="100"                 # 等待队列长度
           processorCache="200"              # 处理器缓存数量
           socket.txBufSize="8192"           # 发送缓冲区大小
           socket.rxBufSize="8192"           # 接收缓冲区大小
           socket.appReadBufSize="8192"      # 应用层读缓冲区
           socket.appWriteBufSize="8192"     # 应用层写缓冲区
           tcpNoDelay="true"                 # 禁用 Nagle 算法
           compression="on"                  # 启用压缩
           compressionMinSize="2048"         # 最小压缩大小（字节）
           compressableMimeType="text/html,text/xml,text/plain"
           />
```

**参数调优建议：**

```
生产环境常用配置：
  maxThreads:          200-1000（取决于 CPU 核心数和业务类型）
  maxConnections:     10000（取决于是 CPU 密集型还是 IO 密集型）
  acceptCount:        100-1000（排队长度，超过则拒绝连接）
  connectionTimeout:  20000-60000（毫秒）

公式参考：
  IO 密集型：maxThreads = CPU 核心数 * 2
  计算密集型：maxThreads = CPU 核心数 + 1
  混合型：    maxThreads = CPU 核心数 * 2 * (1 + 等待时间/计算时间)
```

### 10.4 Pipeline 和 Valve 机制

Tomcat 使用了 Pipeline-Valve（管道-阀门）模式处理请求：

```
Connector 接收到请求
  │
  Engine Pipeline: EngineValve
  │  ↓ 认证、虚拟主机路由
  Host Pipeline: HostValve
  │  ↓ 访问日志、访问控制
  Context Pipeline: ContextValve
  │  ↓ FilterChain
  Wrapper Pipeline: WrapperValve
  │  ↓
  Servlet.service()
```

**自定义 Valve（Tomcat 级别，不同与 Servlet Filter）：**

```java
public class CustomValve extends ValveBase {
    @Override
    public void invoke(Request request, Response response)
            throws IOException, ServletException {
        // 前置处理
        long start = System.currentTimeMillis();

        // 调用下一个 Valve
        getNext().invoke(request, response);

        // 后置处理
        long duration = System.currentTimeMillis() - start;
        System.out.println("Request processed in " + duration + "ms");
    }
}
```

**在 server.xml 中配置 Valve：**

```xml
<Host name="localhost" appBase="webapps">
    <Valve className="com.example.CustomValve"/>
</Host>
```

---

## 11. Tomcat 类加载机制

### 11.1 类加载器层次结构

```
┌───────────────────────────────────────────────┐
│              Bootstrap ClassLoader              │
│  JVM 核心类：rt.jar、java.lang.*               │
└───────────────────────┬───────────────────────┘
                        │
┌───────────────────────▼───────────────────────┐
│              Extension ClassLoader              │
│  JRE 扩展：jre/lib/ext/*.jar                   │
└───────────────────────┬───────────────────────┘
                        │
┌───────────────────────▼───────────────────────┐
│              System ClassLoader                 │
│  Tomcat 启动类：$CATALINA_HOME/lib/*.jar       │
│  (catalina.jar、servlet-api.jar 等)             │
└───────────────────────┬───────────────────────┘
                        │
┌───────────────────────▼───────────────────────┐
│           Common ClassLoader                   │
│  $CATALINA_HOME/lib/*.jar（Tomcat 公共库）     │
└──────┬────────────────────────────────┬───────┘
       │                                │
┌──────▼──────┐                ┌───────▼───────┐
│ WebappClass │                │ WebappClass    │
│ Loader      │                │ Loader         │
│ (应用A)      │                │ (应用B)         │
│ /webapps/A/*│                │ /webapps/B/*   │
└─────────────┘                └───────────────┘
```

### 11.2 与传统双亲委派模型的区别

传统 JDK 的双亲委派模型：**先让父 ClassLoader 加载，父加载不了才自己加载。**

Tomcat 打破了这一规则：**WebappClassLoader 先自己加载，自己加载不了才委托给父加载器。**

```
// 传统双亲委派：
要求类加载请求 → 检查是否已加载 → 委托给父加载器 → 父加载不了再自己加载

// Tomcat WebappClassLoader：
要求类加载请求 → 检查是否已加载 → 先自己加载
→ 如果自己的 WEB-INF/classes 或 WEB-INF/lib 中有 → 直接使用
→ 自己的没有 → 委托给父加载器（common）

// 例外：J2SE 核心类、servlet-api.jar 始终由父加载器加载
// 这是为了防止应用覆盖核心 API
```

### 11.3 为什么这样设计？

```
场景：同一台 Tomcat 部署两个应用
  app1.war 使用 Spring 4.x
  app2.war 使用 Spring 5.x

传统的双亲委派：System ClassLoader 加载了一个 Spring 版本，另一个版本无法加载
Tomcat 的 WebappClassLoader：每个应用独立加载自己的 Spring 版本 → 互不影响

这就是 "类隔离" 的目的：
  - 每个 Web 应用有独立的类空间
  - 同一个类不同版本可以共存
  - 应用之间互相隔离
```

### 11.4 常见类加载问题

```java
// 问题1：ClassCastException
// 当同一个类被不同 ClassLoader 加载时，JVM 认为是不同类

// 举例：
// 应用的 WEB-INF/lib/ 和 Tomcat 的 lib/ 下都有同一个 jar
// → 该 jar 中的同一个类被两个 ClassLoader 加载
// → 应用传递该类实例给 Tomcat → ClassCastException!

// 问题2：ClassNotFoundException
// 某些 jar 放在 Tomcat lib 下（公共），应用找不到
// 或者某些 jar 放在应用 WEB-INF/lib 下，Tomcat 组件找不到

// 解决方案：
// 共享库 → $CATALINA_HOME/lib/
// 私有库 → WEB-INF/lib/
// 确保 jar 不重复放置
```

---

## 12. Embedded Tomcat（嵌入式 Tomcat）

### 12.1 什么是 Embedded Tomcat？

嵌入式 Tomcat 是指在 Java 应用中直接启动 Tomcat 实例，而不是将应用部署到独立的 Tomcat 中。Spring Boot 的内嵌 Tomcat 就是基于此实现。

### 12.2 使用示例

```java
public class EmbeddedTomcatDemo {
    public static void main(String[] args) throws Exception {
        // 1. 创建 Tomcat 实例
        Tomcat tomcat = new Tomcat();

        // 2. 设置端口
        tomcat.setPort(8080);

        // 3. 添加应用上下文
        String contextPath = "/app";
        String docBase = new File(".").getAbsolutePath();
        Context ctx = tomcat.addWebapp(contextPath, docBase);

        // 4. 添加 Servlet
        Wrapper wrapper = Tomcat.addServlet(ctx, "hello", new HelloServlet());
        wrapper.addMapping("/hello");

        // 或者添加默认 Servlet
        Tomcat.addDefaultServlet(ctx);

        // 5. 配置 Connector
        Connector connector = tomcat.getConnector();
        connector.setProperty("maxThreads", "200");
        connector.setProperty("connectionTimeout", "20000");

        // 6. 启动
        tomcat.start();
        System.out.println("Tomcat 启动成功: http://localhost:8080/app");

        // 7. 等待请求
        tomcat.getServer().await();
    }
}

@WebServlet("/hello")
public static class HelloServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("text/plain;charset=utf-8");
        resp.getWriter().write("Hello from Embedded Tomcat!");
    }
}
```

### 12.3 Maven 依赖

```xml
<dependency>
    <groupId>org.apache.tomcat.embed</groupId>
    <artifactId>tomcat-embed-core</artifactId>
    <version>9.0.82</version>
</dependency>
<dependency>
    <groupId>org.apache.tomcat.embed</groupId>
    <artifactId>tomcat-embed-jasper</artifactId>
    <version>9.0.82</version>
</dependency>
```

### 12.4 Embedded Tomcat vs 独立 Tomcat

| 特性 | 独立 Tomcat | Embedded Tomcat |
|------|------------|----------------|
| 部署方式 | WAR 包部署到 Tomcat | 可执行 JAR（java -jar） |
| 配置方式 | server.xml、context.xml | 编程式配置 |
| 启动速度 | 慢（先启动 Tomcat） | 快（随应用启动） |
| 运维复杂度 | 需要运维管理 Tomcat | 管理简单（应用即是服务器） |
| 版本控制 | 由运维管控 | 由 Maven/Gradle 管理 |
| 热部署 | 支持（Manager App） | 不支持（需重启应用） |
| 资源隔离 | 多应用在同一 Tomcat | 每个应用独立 JVM |
| 适用场景 | 传统企业应用 | 微服务（Spring Boot） |

---

## 13. JSP 基础（了解）

### 13.1 什么是 JSP？

JSP（Java Server Pages）是一种在 HTML 中嵌入 Java 代码的技术。JSP 本质上就是 Servlet——容器将 JSP 编译为 Servlet 再执行。

```
JSP 页面
  │
  ↓ 编译
_JSP.java（继承 HttpJspBase → HttpServlet）
  │
  ↓ 编译
_JSP.class
  │
  ↓ 执行
_jspService(request, response) ← 对应 service() 方法
```

### 13.2 JSP 语法元素

**指令（Directives）：**

```jsp
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.*, com.example.User" %>
<%@ page isELIgnored="false" %>
<%@ page errorPage="/error.jsp" %>

<%@ include file="/common/header.jsp" %>    <!-- 静态包含 -->

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
```

**脚本元素：**

```jsp
<%-- 1. 脚本片段（Scriptlet）：不推荐使用 --%>
<%
    String username = (String) session.getAttribute("user");
    List<User> userList = userService.findAll();
    for (User user : userList) {
        out.println("<li>" + user.getName() + "</li>");
    }
%>

<%-- 2. 表达式（Expression） --%>
<p>当前时间: <%= new java.util.Date() %></p>
<p>用户名: <%= request.getParameter("username") %></p>

<%-- 3. 声明（Declaration）：声明方法或成员变量 --%>
<%!
    private int counter = 0;
    private String formatDate(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }
%>
```

**JSTL + EL（推荐方式，避免 Scriptlet）：**

```jsp
<%-- JSTL 核心标签库 --%>
<c:if test="${not empty user}">
    <p>欢迎, ${user.username}</p>
</c:if>

<c:forEach items="${userList}" var="user" varStatus="status">
    <tr>
        <td>${status.index + 1}</td>
        <td>${user.username}</td>
        <td>${user.email}</td>
        <td>
            <c:choose>
                <c:when test="${user.status == 1}">启用</c:when>
                <c:otherwise>禁用</c:otherwise>
            </c:choose>
        </td>
    </tr>
</c:forEach>

<c:if test="${empty userList}">
    <tr><td colspan="4">暂无数据</td></tr>
</c:if>
```

### 13.3 为什么 JSP 已经过时？

| 问题 | 说明 |
|------|------|
| **前后端耦合** | Java 代码混在 HTML 中，无法前后端分离 |
| **维护困难** | 大型项目 JSP 文件难以组织 |
| **性能问题** | JSP 编译为 Servlet，每次修改需要重新编译 |
| **模板功能弱** | 相比 Thymeleaf/FreeMarker，语法不够优雅 |
| **测试困难** | JSP 需要在容器中测试，无法单元测试 |
| **前后端分离** | 现代 Web 开发使用 REST API + 前端框架（React/Vue） |

**现代替代方案：**
- 服务端渲染：**Thymeleaf**（Spring Boot 官方推荐）、**FreeMarker**
- 前后端分离：**Vue/React + REST API**

---

## 14. Thymeleaf 模板引擎

### 14.1 为什么选择 Thymeleaf？

Thymeleaf 是 Spring Boot 官方推荐的模板引擎，相比 JSP 的优势：

```
Thymeleaf 的特点：
1. 自然模板：HTML 文件可以直接在浏览器中打开（原型即页面）
2. 语法优雅：使用 HTML 属性，不侵入标签内容
3. 与 Spring MVC 深度集成
4. 国际化（i18n）支持良好
5. 支持布局/片段复用
```

### 14.2 核心语法

**标准表达式：**

```html
<!-- 变量表达式 -->
<p th:text="${user.username}">默认用户名</p>

<!-- 选择表达式（*{...} 配合 th:object） -->
<div th:object="${user}">
    <p th:text="*{username}">用户名</p>
    <p th:text="*{email}">邮箱</p>
</div>

<!-- URL 表达式 -->
<a th:href="@{/users/{id}(id=${user.id})}">详情</a>
<a th:href="@{/users(page=${page}, size=${size})}">分页</a>

<!-- 消息表达式（国际化） -->
<p th:text="#{welcome.message}">欢迎信息</p>

<!-- 片段表达式 -->
<div th:replace="~{common/header :: header}"></div>
```

**常用属性：**

```html
<!-- th:text：文本替换 -->
<span th:text="${user.role}">普通用户</span>

<!-- th:utext：不转义 HTML（小心 XSS！） -->
<div th:utext="${article.content}">文章内容</div>

<!-- th:each：循环 -->
<tr th:each="user : ${userList}">
    <td th:text="${user.id}">1</td>
    <td th:text="${user.username}">username</td>
</tr>

<!-- th:if / th:unless：条件 -->
<div th:if="${user.role == 'admin'}">
    <a href="/admin">管理后台</a>
</div>
<div th:unless="${#lists.isEmpty(userList)}">
    共有 <span th:text="${#lists.size(userList)}">0</span> 个用户
</div>

<!-- th:switch / th:case：多分支 -->
<div th:switch="${user.role}">
    <p th:case="'admin'">管理员</p>
    <p th:case="'editor'">编辑</p>
    <p th:case="*">普通用户</p>
</div>

<!-- th:attr：设置任意属性 -->
<img th:attr="src=${user.avatar}, alt=${user.username}" />

<!-- th:classappend：追加 CSS 类 -->
<tr th:classappend="${user.status == 0 ? 'disabled' : ''}">

<!-- th:checked / th:selected：表单元素状态 -->
<input type="checkbox" th:checked="${user.active}" />

<!-- th:field：表单绑定（配合 Spring MVC 使用） -->
<input type="text" th:field="*{username}" />
```

### 14.3 模板布局

```html
<!-- common/layout.html：布局模板 -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title th:text="${title}">页面标题</title>
    <link rel="stylesheet" th:href="@{/css/common.css}" />
    <th:block th:replace="~{common/header :: head-css}"></th:block>
</head>
<body>
    <!-- 公共头部 -->
    <div th:replace="~{common/header :: header}"></div>

    <!-- 主内容区域：由子页面填充 -->
    <div th:replace="~{::content}">
        <div th:fragment="content">默认内容</div>
    </div>

    <!-- 公共尾部 -->
    <div th:replace="~{common/footer :: footer}"></div>
</body>
</html>

<!-- user/list.html：用户列表页面 -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      th:replace="~{common/layout :: layout(title='用户管理')}">
<body>
<div th:fragment="content">
    <table>
        <thead>
            <tr>
                <th>ID</th>
                <th>用户名</th>
                <th>邮箱</th>
                <th>状态</th>
                <th>操作</th>
            </tr>
        </thead>
        <tbody>
            <tr th:each="user : ${users}">
                <td th:text="${user.id}">1</td>
                <td th:text="${user.username}">john</td>
                <td th:text="${user.email}">john@example.com</td>
                <td th:text="${user.status == 1 ? '启用' : '禁用'}">启用</td>
                <td>
                    <a th:href="@{/users/{id}(id=${user.id})}">查看</a>
                    <a th:href="@{/users/{id}/edit(id=${user.id})}">编辑</a>
                </td>
            </tr>
        </tbody>
    </table>
</div>
</body>
</html>
```

### 14.4 内置工具对象

```html
<!-- #dates：日期处理 -->
<span th:text="${#dates.format(date, 'yyyy-MM-dd HH:mm')}">2024-01-01 12:00</span>
<span th:text="${#dates.createNow()}">当前时间</span>

<!-- #strings：字符串处理 -->
<span th:text="${#strings.toUpperCase(user.username)}">USERNAME</span>
<span th:text="${#strings.abbreviate(content, 50)}">截取前50字符</span>

<!-- #lists：集合处理 -->
<span th:text="${#lists.size(list)}">集合大小</span>
<span th:if="${#lists.contains(list, item)}">包含</span>

<!-- #arrays：数组处理 -->
<span th:text="${#arrays.length(arr)}">数组长度</span>

<!-- #numbers：数字格式化 -->
<span th:text="${#numbers.formatDecimal(price, 1, 2)}">1,234.56</span>

<!-- #calendars：日历 -->
<span th:text="${#calendars.format(cal, 'yyyy-MM')}">2024-01</span>

<!-- #ctx：上下文对象 -->
<span th:text="${#ctx.request.servletPath}">请求路径</span>
```

### 14.5 Java 中配置 Thymeleaf

```java
// 独立使用 Thymeleaf（非 Spring 环境）
public class ThymeleafConfig {
    private static final TemplateEngine templateEngine;

    static {
        TemplateEngine engine = new TemplateEngine();

        // 模板解析器
        ServletContextTemplateResolver resolver =
            new ServletContextTemplateResolver();
        resolver.setPrefix("/WEB-INF/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(true);      // 生产环境启用缓存
        resolver.setCacheTTLMs(3600000L); // 缓存 1 小时

        engine.setTemplateResolver(resolver);
        templateEngine = engine;
    }

    public static TemplateEngine getEngine() {
        return templateEngine;
    }

    public static String render(String template, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        return templateEngine.process(template, context);
    }
}
```

---

## 15. 实战：Servlet + Thymeleaf 登录注册系统

### 15.1 项目结构

```
src/
├── main/
│   ├── java/com/example/
│   │   ├── config/
│   │   │   └── ThymeleafConfig.java          # Thymeleaf 配置
│   │   ├── filter/
│   │   │   ├── EncodingFilter.java            # 编码过滤器
│   │   │   └── AuthFilter.java                # 登录校验过滤器
│   │   ├── listener/
│   │   │   └── AppContextListener.java        # 启动监听器
│   │   ├── model/
│   │   │   └── User.java                      # 用户模型
│   │   ├── dao/
│   │   │   └── UserDao.java                   # 数据访问
│   │   ├── service/
│   │   │   └── UserService.java               # 业务逻辑
│   │   └── servlet/
│   │       ├── HomeServlet.java               # 首页
│   │       ├── RegisterServlet.java           # 注册
│   │       ├── LoginServlet.java              # 登录
│   │       └── LogoutServlet.java             # 退出
│   │
│   └── webapp/
│       ├── WEB-INF/
│       │   └── templates/
│       │       ├── index.html                 # 首页模板
│       │       ├── login.html                 # 登录页
│       │       ├── register.html              # 注册页
│       │       ├── profile.html               # 个人信息
│       │       └── error.html                 # 错误页
│       └── static/
│           └── css/
│               └── style.css
```

### 15.2 核心代码实现

**LoginServlet.java：**

```java
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private UserService userService;
    private TemplateEngine templateEngine;

    @Override
    public void init() {
        userService = new UserService();
        templateEngine = ThymeleafConfig.getEngine();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        // 已登录用户直接跳转首页
        if (req.getSession(false) != null
                && req.getSession().getAttribute("user") != null) {
            resp.sendRedirect(req.getContextPath() + "/");
            return;
        }

        // 渲染登录页面
        Context context = new Context();
        context.setVariable("error", req.getParameter("error"));
        resp.setContentType("text/html;charset=utf-8");
        templateEngine.process("login", context, resp.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // 参数校验
        if (username == null || password == null
                || username.trim().isEmpty() || password.trim().isEmpty()) {
            resp.sendRedirect(req.getContextPath()
                + "/login?error=用户名和密码不能为空");
            return;
        }

        // 登录验证
        try {
            User user = userService.login(username, password);
            if (user != null) {
                // 登录成功，创建 Session
                HttpSession session = req.getSession();
                session.setAttribute("user", user);
                session.setMaxInactiveInterval(3600); // 1 小时

                // 重定向到首页（Post/Redirect/Get 模式）
                resp.sendRedirect(req.getContextPath() + "/");
            } else {
                resp.sendRedirect(req.getContextPath()
                    + "/login?error=用户名或密码错误");
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect(req.getContextPath()
                + "/login?error=系统错误，请稍后重试");
        }
    }
}

// RegisterServlet，UserService，UserDao 等模式类似
```

**register.html：**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>用户注册</title>
    <link rel="stylesheet" th:href="@{/static/css/style.css}" />
</head>
<body>
    <div class="container">
        <h1>用户注册</h1>

        <div th:if="${error}" class="alert alert-error" th:text="${error}">
            错误信息
        </div>

        <form action="/register" method="post">
            <div class="form-group">
                <label for="username">用户名</label>
                <input type="text" id="username" name="username"
                       required minlength="3" maxlength="50"
                       placeholder="3-50个字符" />
            </div>

            <div class="form-group">
                <label for="email">邮箱</label>
                <input type="email" id="email" name="email"
                       required placeholder="请输入邮箱" />
            </div>

            <div class="form-group">
                <label for="password">密码</label>
                <input type="password" id="password" name="password"
                       required minlength="6"
                       placeholder="至少6个字符" />
            </div>

            <div class="form-group">
                <label for="confirmPassword">确认密码</label>
                <input type="password" id="confirmPassword" name="confirmPassword"
                       required />
            </div>

            <button type="submit">注册</button>
            <p>已有账号？<a href="/login">立即登录</a></p>
        </form>
    </div>
</body>
</html>
```

---

## 16. 最佳实践与常见陷阱

### 16.1 最佳实践

| 类别 | 建议 |
|------|------|
| **Servlet** | 避免使用实例变量存储请求数据（线程不安全）；使用 @WebServlet 注解代替 web.xml |
| **Filter** | 对不需要过滤的路径使用白名单设计；注意 Filter 的执行顺序 |
| **Session** | 不要存储大量数据在 Session 中；分布式环境使用 Redis 集中存储 |
| **请求体** | 如果多个 Filter 需要读取请求体，使用 HttpServletRequestWrapper 缓存 body |
| **路径处理** | 始终使用 `request.getContextPath()` 拼接路径，不要硬编码 |
| **字符编码** | Encoding Filter 要放在 Filter 链的最前面 |
| **资源释放** | 使用 try-with-resources 确保 IO 资源关闭 |
| **错误处理** | 配置全局错误页面，不要暴露堆栈信息 |
| **安全** | 所有密码使用 BCrypt 加密；输出使用 HTML 转义防 XSS |

### 16.2 常见陷阱

| 陷阱 | 问题 | 解决 |
|------|------|------|
| **响应提交后设置头** | 获取 Writer/OutputStream 后修改响应头无效 | 所有响应头在获取流之前设置 |
| **请求体读取一次** | Filter 读取 InputStream 后 Servlet 读不到 | 使用 HttpServletRequestWrapper 缓存 body |
| **Session 不一致** | 分布式环境本地 Session 不共享 | 使用 Redis 集中式 Session |
| **Filter 顺序错误** | Encoding Filter 未在第一个 | Filter 按类名字母顺序执行（注解方式） |
| **中文乱码** | 请求/响应编码未设置 | Encoding Filter 统一设置 UTF-8 |
| **资源路径错误** | forward/redirect 路径写法不对 | forward 用 `/app/users`，redirect 用相对或绝对 URL |
| **getWriter 和 getOutputStream** | 同时调用会抛 IllegalStateException | 根据返回类型选择其中一种 |
| **大量数据在 Session** | 占用服务器内存，GC 压力大 | Session 只保存用户 ID 等少量数据 |

---

## 17. 面试题

### 基础题

**Q1: Servlet 生命周期是怎样的？**

A: 四个阶段：
1. **加载和实例化**：类加载器加载 Servlet 类，调用构造器创建实例
2. **初始化**：调用 `init(ServletConfig)` 方法，只执行一次
3. **请求处理**：每次请求调用 `service()` 方法，根据 HTTP 方法分发到 `doGet()`/`doPost()` 等
4. **销毁**：容器关闭或应用卸载时调用 `destroy()` 方法，只执行一次

**Q2: Forward 和 Redirect 有什么区别？**

A:
- Forward 是服务器端内部跳转，浏览器 URL 不变，一次请求，可以共享 request 属性
- Redirect 是客户端跳转，浏览器 URL 变为目标地址，两次请求，不能共享 request 属性
- Forward 只能在本应用内，Redirect 可跨域

**Q3: Filter 的作用是什么？如何配置？**

A: Filter 用于对请求/响应进行预处理和后处理。典型用途：编码设置、登录校验、日志记录、XSS 过滤。
配置方式：`@WebFilter` 注解（Servlet 3.0+）或 web.xml 配置。

**Q4: ServletContext 和 ServletConfig 的区别？**

A:
- ServletConfig：单个 Servlet 的配置信息，通过 `getInitParameter()` 获取
- ServletContext：整个 Web 应用的上下文，可以获取全局参数、设置应用级属性、获取资源路径等

**Q5: Tomcat 的架构是怎样的？**

A: Server → Service → (Connector + Engine) → Host → Context → Wrapper。
- Connector：处理网络连接和协议解析
- Engine：处理所有请求的 Servlet 容器
- Host：虚拟主机
- Context：Web 应用
- Wrapper：Servlet

### 进阶题

**Q6: HttpServletRequest.getParameter() 和 getInputStream() 有什么区别？**

A:
- `getParameter()`：用于读取表单提交的数据（GET 的 query string + POST 的 form data），内部会解析请求体
- `getInputStream()`：用于读取原始的请求体二进制流，适合 JSON/XML/文件上传等
- **两者只能调用一个**，如果先调了 `getParameter()` 再调 `getInputStream()`，getInputStream() 可能返回空

**Q7: 什么是 Servlet 线程安全问题？如何解决？**

A: Servlet 是单实例多线程，多个请求同时访问同一个 Servlet 实例，如果有共享可变状态，就会产生线程安全问题。
解决：
1. 不使用实例变量存储请求相关数据
2. 使用局部变量（每个请求有独立栈）
3. 使用 ThreadLocal
4. 使用同步机制（但会降低性能）

**Q8: Tomcat 为什么打破双亲委派模型？**

A: 为了实现应用隔离。不同的 Web 应用可能依赖同一 jar 的不同版本，如果使用传统双亲委派，只有一个版本会被加载。
Tomcat 的 WebappClassLoader 先自己加载 WEB-INF/ 下的类，找不到才委托给父加载器，实现了"类隔离"。

**Q9: 如何实现请求体重复读取？**

A: 使用 HttpServletRequestWrapper 包装原始请求：
1. 在构造器中读取并缓存请求体（byte[]）
2. 重写 getInputStream() 和 getReader()，每次都从缓存中读取

**Q10: Tomcat 的 BIO/NIO/NIO2/APR 有什么区别？**

A:
- BIO：阻塞 I/O，一个连接一个线程，简单但连接数多时性能差
- NIO：非阻塞 I/O，少量线程处理大量连接，**Tomcat 8+ 默认**
- NIO2：异步 I/O，性能略优于 NIO
- APR：使用 C 语言本地库，需要安装 APR，性能最高但维护复杂

**Q11: JSP 中 include 指令和 include 动作的区别？**

A:
- `<%@ include file="..." %>`：编译时包含（静态包含），被包含文件的内容在编译时直接嵌入
- `<jsp:include page="..." />`：运行时包含（动态包含），每个请求单独执行被包含的文件

**Q12: 如何自定义一个 Tomcat Valve？**

A: 继承 `ValveBase` 类，重写 `invoke()` 方法，在 server.xml 的 `<Host>` 或 `<Context>` 中配置。Valve 在 Tomcat 层面拦截请求（比 Servlet Filter 更底层）。

**Q13: Thymeleaf 中 th:text 和 th:utext 的区别？**

A:
- `th:text`：转义 HTML 标签，防止 XSS 攻击，安全
- `th:utext`：不转义 HTML 标签，显示原始 HTML，不安全（仅用于信任的内容）

**Q14: 实战：如果需要在 Filter 中修改响应内容，怎么做？**

A: 使用 HttpServletResponseWrapper 包装响应：
1. 创建一个 CaptureResponseWrapper，用 ByteArrayOutputStream 捕获输出
2. 在 Filter 中调用 chain.doFilter() 后，获取捕获的内容
3. 修改内容后，通过原始响应输出

```java
public class ResponseModifyFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse resp,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) resp;
        CaptureResponseWrapper wrapper = new CaptureResponseWrapper(response);

        chain.doFilter(req, wrapper);

        // 获取原始响应内容
        String content = wrapper.getCaptureAsString();
        // 修改内容
        String modified = content.replace("</body>", "<script>alert('injected')</script></body>");
        // 输出修改后的内容
        response.getWriter().write(modified);
    }

    private static class CaptureResponseWrapper extends HttpServletResponseWrapper {
        private final ByteArrayOutputStream capture;
        private ServletOutputStream output;
        private PrintWriter writer;

        public CaptureResponseWrapper(HttpServletResponse response) {
            super(response);
            capture = new ByteArrayOutputStream(response.getBufferSize());
        }

        @Override
        public ServletOutputStream getOutputStream() {
            if (writer != null) throw new IllegalStateException("getWriter already called");
            if (output == null) {
                output = new ServletOutputStream() {
                    @Override public boolean isReady() { return true; }
                    @Override public void setWriteListener(WriteListener listener) {}
                    @Override public void write(int b) { capture.write(b); }
                };
            }
            return output;
        }

        public String getCaptureAsString() throws UnsupportedEncodingException {
            return capture.toString("UTF-8");
        }
    }
}
```

---

## 参考资源

- [Servlet 6.0 Specification](https://jakarta.ee/specifications/servlet/6.0/) -- Jakarta Servlet 官方规范
- [Apache Tomcat 官方文档](https://tomcat.apache.org/tomcat-10.0-doc/index.html) -- Tomcat 详细文档
- [Thymeleaf 官方文档](https://www.thymeleaf.org/documentation.html) -- Thymeleaf 模板引擎
- 《深入分析 Java Web 技术内幕》 -- 许令波，Java Web 原理经典
- 《Tomcat 架构解析》 -- 理解 Tomcat 源码架构
- [Baeldung: Guide to Java Servlet](https://www.baeldung.com/Intro-to-Servlets) -- Servlet 入门指南

---

*最后更新: 2026-05-31*
