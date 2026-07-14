# JavaWeb 完整知识体系

> **文档定位**：Java 后端技术参考文档 | JavaWeb 全景知识体系  
> **核心定义**：JavaWeb 是使用 Java 技术开发基于 Web 的应用程序的总称，核心是通过 Java 实现浏览器与服务器的交互  
> **架构模式**：B/S（浏览器/服务器）架构  
> **后续升级**：Spring MVC / Spring Boot 是对 JavaWeb 底层技术的封装和简化

---

## 目录

- [一、JavaWeb 核心概念](#一javaweb-核心概念)
- [二、HTTP 协议](#二http-协议)
- [三、Servlet](#三servlet)
- [四、Cookie 与 Session](#四cookie-与-session)
- [五、JSP](#五jsp)
- [六、Filter（过滤器）](#六filter过滤器)
- [七、Listener（监听器）](#七listener监听器)
- [八、文件上传与下载](#八文件上传与下载)
- [九、Web 应用部署与配置](#九web-应用部署与配置)

---

## 一、JavaWeb 核心概念

### 技术栈全景

| 层级 | 包含内容 |
|------|----------|
| **基础** | HTML/CSS/JavaScript、HTTP 协议、JavaSE 核心（IO、多线程、集合） |
| **核心** | Servlet、JSP、Filter、Listener |
| **框架** | Spring MVC、MyBatis |
| **容器** | Tomcat、Jetty、JBoss、WebLogic |

### 执行流程

```
浏览器 HTTP 请求 → Tomcat 接收 → 解析并转发给 Servlet/JSP 
→ 处理请求（操作数据库）→ 生成响应（HTML/JSON）→ 返回浏览器渲染
```

---

## 二、HTTP 协议

### 核心特点

| 特点 | 说明 |
|------|------|
| **无状态** | 协议本身不记录请求关联，需 Cookie/Session 维持状态 |
| **请求-响应模型** | 一次请求对应一次响应 |
| **基于 TCP/IP** | 默认端口 80（HTTP）/ 443（HTTPS） |
| **多方法支持** | GET、POST、PUT、DELETE、HEAD、OPTIONS |

### 请求与响应结构

| 部分 | 请求 | 响应 |
|------|------|------|
| **首行** | `GET /index.html HTTP/1.1` | `HTTP/1.1 200 OK` |
| **头** | `User-Agent`、`Content-Type`、`Cookie` | `Content-Type`、`Set-Cookie`、`Location` |
| **体** | POST 请求参数（GET 无请求体） | HTML、JSON、图片等 |

### 常见状态码

| 类别 | 示例 |
|------|------|
| **2xx 成功** | `200 OK`、`201 Created` |
| **3xx 重定向** | `301` 永久、`302` 临时、`304` 未修改 |
| **4xx 客户端错误** | `400` 参数错误、`401` 未授权、`403` 禁止、`404` 不存在 |
| **5xx 服务端错误** | `500` 内部错误、`503` 服务不可用 |

### GET vs POST

| 对比 | GET | POST |
|------|-----|------|
| 参数位置 | URL 中 | 请求体 |
| 长度限制 | 受限 | 无限制 |
| 安全性 | 低 | 高 |
| 缓存 | 可缓存 | 不可缓存 |
| 幂等性 | 幂等 | 非幂等 |

---

## 三、Servlet

### 核心定义

Servlet 是运行在服务器端的 Java 类，用于处理客户端 HTTP 请求并生成响应，是 JavaWeb 的核心组件。

### 生命周期

```
加载与实例化 → init()（一次）→ service() / doGet() / doPost()（每次请求）→ destroy()（一次）
```

> **注意**：Servlet 默认单例，多个请求共享同一实例，避免定义成员变量。

### 三种实现方式

| 方式 | 说明 | 推荐度 |
|------|------|--------|
| 实现 `Servlet` 接口 | 最基础，重写所有方法 | ⭐⭐ |
| 继承 `GenericServlet` | 简化，仅重写 `service()` | ⭐⭐⭐ |
| 继承 `HttpServlet` | 推荐，重写 `doGet()`/`doPost()` | ⭐⭐⭐⭐⭐ |

```java
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/hello")
public class HelloServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        resp.setContentType("text/html;charset=utf-8");
        resp.getWriter().println("<h1>Hello Servlet!</h1>");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        req.setCharacterEncoding("utf-8");
        doGet(req, resp);  // 复用 GET 逻辑
    }
}
```

### 核心对象速查

| 对象 | 常用方法 |
|------|----------|
| **HttpServletRequest** | `getParameter()`、`getSession()`、`getCookies()`、`setAttribute()`、`getRequestDispatcher()` |
| **HttpServletResponse** | `setContentType()`、`getWriter()`、`getOutputStream()`、`sendRedirect()`、`addCookie()` |
| **ServletConfig** | 获取初始化参数、Servlet 名称 |
| **ServletContext** | `getRealPath()`、`setAttribute()`、`getAttribute()`（全局共享） |

### 请求转发 vs 重定向

| 对比 | 请求转发（forward） | 重定向（sendRedirect） |
|------|---------------------|------------------------|
| 请求次数 | 1 次 | 2 次 |
| 地址栏 | 不变 | 变化 |
| 共享 request 数据 | ✅ | ❌ |
| 跳转范围 | 仅应用内部 | 可跳到外部 URL |

```java
// 转发
req.getRequestDispatcher("/index.jsp").forward(req, resp);

// 重定向
resp.sendRedirect("/login.jsp");
```

---

## 四、Cookie 与 Session

### Cookie（客户端状态管理）

```java
// 创建 Cookie
Cookie cookie = new Cookie("username", "zhangsan");
cookie.setMaxAge(3600 * 24);  // 1 天
cookie.setPath("/");
resp.addCookie(cookie);

// 获取 Cookie
Cookie[] cookies = req.getCookies();

// 删除 Cookie（过期时间设为 0）
Cookie cookie = new Cookie("username", "");
cookie.setMaxAge(0);
resp.addCookie(cookie);
```

### Session（服务器端状态管理）

```java
HttpSession session = req.getSession();
session.setAttribute("user", user);
User user = (User) session.getAttribute("user");
session.removeAttribute("user");
session.invalidate();  // 销毁会话
```

### Cookie vs Session

| 对比 | Cookie | Session |
|------|--------|---------|
| **存储位置** | 客户端 | 服务器 |
| **安全性** | 低（可篡改） | 高 |
| **容量** | ~4KB | 无限制 |
| **有效期** | 可长期 | 默认会话级 |

---

## 五、JSP

> JSP 本质是 Servlet（会被编译为 `.java` / `.class` 文件），用于简化页面开发。

### 核心语法

| 元素 | 语法 | 说明 |
|------|------|------|
| **代码片段** | `<% ... %>` | 嵌入 Java 代码 |
| **输出表达式** | `<%= ... %>` | 等价于 `out.print()` |
| **page 指令** | `<%@ page ... %>` | 配置编码、导入包、错误页 |
| **include 指令** | `<%@ include file="header.jsp" %>` | 静态包含（编译时合并） |
| **taglib 指令** | `<%@ taglib prefix="c" uri="..." %>` | 引入 JSTL 标签库 |

### JSP 内置对象

| 对象 | 说明 | 作用域 |
|------|------|--------|
| `pageContext` | 页面上下文 | page |
| `request` | 请求对象 | request |
| `session` | 会话对象 | session |
| `application` | 应用上下文 | application |
| `response` | 响应对象 | — |
| `out` | 输出流 | — |

### EL 表达式

```jsp
${user.username}                    <!-- 自动查找四大域 -->
${user.age > 18 ? "成年" : "未成年"} <!-- 运算 -->
${param.id}                         <!-- 获取请求参数 -->
```

### JSTL 核心标签

```jsp
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:if test="${user.age > 18}"><p>成年</p></c:if>

<c:forEach items="${userList}" var="user" varStatus="status">
    <td>${status.index + 1}</td>
    <td>${user.username}</td>
</c:forEach>
```

---

## 六、Filter（过滤器）

Filter 用于拦截请求/响应，实现统一处理。

```java
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;

@WebFilter("/*")  // 拦截所有请求
public class EncodingFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        request.setCharacterEncoding("utf-8");
        response.setCharacterEncoding("utf-8");
        chain.doFilter(request, response);  // 放行
    }
}
```

| 常用场景 | 说明 |
|----------|------|
| 统一编码过滤 | 解决中文乱码 |
| 登录验证 | 拦截未登录用户 |
| 日志记录 | 记录请求 URL、访问时间、IP |
| 权限控制 | 根据用户角色拦截 |

---

## 七、Listener（监听器）

监听 Web 应用中事件的组件。

| 监听器 | 触发时机 |
|--------|----------|
| **ServletContextListener** | 应用上下文创建/销毁（启动时初始化，关闭时释放） |
| **HttpSessionListener** | 会话创建/销毁（统计在线人数） |
| **ServletRequestListener** | 请求创建/销毁 |

```java
@WebListener
public class AppContextListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("Web 应用启动");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("Web 应用关闭");
    }
}
```

---

## 八、文件上传与下载

### 文件上传（Apache Commons FileUpload）

```java
@WebServlet("/upload")
public class FileUploadServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        DiskFileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        upload.setFileSizeMax(10 * 1024 * 1024);  // 10MB

        List<FileItem> items = upload.parseRequest(req);
        for (FileItem item : items) {
            if (!item.isFormField()) {
                String path = req.getServletContext().getRealPath("/uploads");
                item.write(new File(path, item.getName()));
            }
        }
    }
}
```

### 文件下载

```java
@WebServlet("/download")
public class FileDownloadServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        String fileName = req.getParameter("fileName");
        String path = req.getServletContext().getRealPath("/uploads/" + fileName);

        resp.setContentType("application/octet-stream");
        resp.setHeader("Content-Disposition", 
            "attachment;filename=" + URLEncoder.encode(fileName, "utf-8"));

        FileInputStream in = new FileInputStream(path);
        ServletOutputStream out = resp.getOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
    }
}
```

---

## 九、Web 应用部署与配置

### web.xml 配置示例

```xml
<!-- Servlet 配置 -->
<servlet>
    <servlet-name>HelloServlet</servlet-name>
    <servlet-class>com.example.HelloServlet</servlet-class>
    <load-on-startup>1</load-on-startup>
</servlet>
<servlet-mapping>
    <servlet-name>HelloServlet</servlet-name>
    <url-pattern>/hello</url-pattern>
</servlet-mapping>

<!-- 欢迎页 -->
<welcome-file-list>
    <welcome-file>index.jsp</welcome-file>
</welcome-file-list>

<!-- 错误页 -->
<error-page>
    <error-code>404</error-code>
    <location>/404.jsp</location>
</error-page>
```

### 部署方式

| 方式 | 说明 |
|------|------|
| **WAR 包部署** | 打包为 `.war`，放入 `webapps` 目录 |
| **解压部署** | 解压到 `webapps` 目录，Tomcat 自动识别 |
| **外置配置** | 通过 `server.xml` 配置 Context |

---

## 总结

| 维度 | 核心要点 |
|------|----------|
| **核心组件** | Servlet 是核心，JSP 本质是 Servlet，Filter/Listener 扩展请求处理 |
| **通信基础** | HTTP 协议，需掌握请求/响应结构、状态码、GET/POST 区别 |
| **状态管理** | Cookie（客户端）+ Session（服务器）解决 HTTP 无状态问题 |
| **开发趋势** | 优先使用注解替代 web.xml，框架（Spring MVC）是对 Servlet 的封装 |
