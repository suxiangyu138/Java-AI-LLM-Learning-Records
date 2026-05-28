JavaWeb

--------------------------------------------------------------------------------------------------------------------------------------
一、JavaWeb核心概念
1. 定义
    JavaWeb是使用Java技术开发基于Web的应用程序的总称，核心是通过Java实现浏览器与服务器的交互，遵循B/S（浏览器/服务器）架构。
2. 核心技术栈
    - 基础：HTML/CSS/JavaScript、HTTP协议、JavaSE核心（IO、多线程、集合）
    - 核心：Servlet、JSP、Filter、Listener
    - 框架：Spring MVC、Struts2（已淘汰）、MyBatis
    - 容器：Tomcat、Jetty、JBoss、WebLogic
3. 执行流程
    浏览器发送HTTP请求 → Web服务器（如Tomcat）接收请求 → 解析请求并转发给对应的Servlet/JSP → 处理请求（如操作数据库）→ 生成响应（HTML/JSON）→ 服务器返回响应 → 浏览器渲染响应内容

--------------------------------------------------------------------------------------------------------------------------------------
二、HTTP协议（超文本传输协议）
1. 核心特点
    - 无状态：协议本身不记录请求之间的关联，需通过Cookie/Session维持状态
    - 基于请求-响应模型：一次请求对应一次响应
    - 可基于TCP/IP：默认端口80（HTTP）/443（HTTPS）
    - 支持多种请求方法：GET、POST、PUT、DELETE、HEAD、OPTIONS等
2. HTTP请求结构
    - 请求行：请求方法 + 请求URL + 协议版本（如GET /index.html HTTP/1.1）
    - 请求头：键值对形式，包含客户端信息、请求参数等（如User-Agent、Content-Type、Cookie）
    - 请求体：POST请求的参数存放位置（GET请求参数在URL中，无请求体）
3. HTTP响应结构
    - 状态行：协议版本 + 状态码 + 状态描述（如HTTP/1.1 200 OK）
    - 响应头：键值对形式，包含服务器信息、响应内容信息（如Content-Type、Set-Cookie、Location）
    - 响应体：服务器返回的内容（HTML、JSON、图片等）
4. 常见HTTP状态码
    - 2xx：成功（200 OK：请求成功；201 Created：资源创建成功）
    - 3xx：重定向（301 Moved Permanently：永久重定向；302 Found：临时重定向；304 Not Modified：资源未修改）
    - 4xx：客户端错误（400 Bad Request：请求参数错误；401 Unauthorized：未授权；403 Forbidden：禁止访问；404 Not Found：资源不存在）
    - 5xx：服务器错误（500 Internal Server Error：服务器内部异常；503 Service Unavailable：服务不可用）
5. GET与POST区别
    - GET：参数在URL中，长度受限，安全性低，可缓存，幂等（多次请求结果一致）
    - POST：参数在请求体，长度无限制，安全性高，不可缓存，非幂等（如提交表单、创建资源）

--------------------------------------------------------------------------------------------------------------------------------------
三、Servlet（服务器端小程序）
1. 核心定义
    Servlet是运行在服务器端的Java类，用于处理客户端HTTP请求并生成响应，是JavaWeb的核心组件。
2. 生命周期
    - 加载与实例化：服务器启动时（或首次请求时）创建Servlet实例（默认单例）
    - 初始化：调用init()方法，仅执行一次，用于初始化资源（如加载配置、连接数据库）
    - 处理请求：调用service()方法（根据请求方法分发到doGet()/doPost()），每次请求执行一次
    - 销毁：服务器关闭时调用destroy()方法，仅执行一次，用于释放资源（如关闭连接、清理缓存）
3. 实现方式
    ① 实现Servlet接口（最基础）
```java
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.io.PrintWriter;
// 注解配置Servlet，替代web.xml
@WebServlet("/HelloServlet")
public class HelloServlet implements Servlet {
    @Override
    public void init(ServletConfig config) throws ServletException {
        // 初始化操作
        System.out.println("Servlet初始化");
    }
    @Override
    public ServletConfig getServletConfig() {
        return null;
    }
    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        // 设置响应内容类型
        res.setContentType("text/html;charset=utf-8");
        PrintWriter out = res.getWriter();
        out.println("<html>");
        out.println("<head><title>Hello Servlet</title></head>");
        out.println("<body>");
        out.println("Hello Servlet!");
        out.println("</body>");
        out.println("</html>");
    }
    @Override
    public String getServletInfo() {
        return "HelloServlet v1.0";
    }
    @Override
    public void destroy() {
        // 销毁操作
        System.out.println("Servlet销毁");
    }
}
```
② 继承GenericServlet（简化，重写service()）
```java
import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.io.PrintWriter;
@WebServlet("/GenericHelloServlet")
public class GenericHelloServlet extends GenericServlet {
    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        res.setContentType("text/html;charset=utf-8");
        PrintWriter out = res.getWriter();
        out.println("Hello GenericServlet!");
    }
}
```
③ 继承HttpServlet（推荐，处理HTTP请求）
```java
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
@WebServlet("/HttpHelloServlet")
public class HttpHelloServlet extends HttpServlet {
    // 处理GET请求
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html;charset=utf-8");
        PrintWriter out = resp.getWriter();
        out.println("Hello HttpServlet GET!");
    }
    // 处理POST请求
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 设置请求编码（解决中文乱码）
        req.setCharacterEncoding("utf-8");
        resp.setContentType("text/html;charset=utf-8");
        // 获取请求参数
        String username = req.getParameter("username");
        PrintWriter out = resp.getWriter();
        out.println("Hello HttpServlet POST! 用户名：" + username);
    }
}
```
4. 核心对象
    ① HttpServletRequest（请求对象）：封装客户端请求信息
    - 常用方法：
  - getParameter(String name)：获取单个请求参数
  - getParameterValues(String name)：获取多个同名参数（如复选框）
  - getRequestDispatcher(String path)：获取请求转发器
  - setAttribute(String name, Object value)：设置请求域属性
  - getAttribute(String name)：获取请求域属性
  - getCookies()：获取客户端Cookie
  - getSession()：获取Session对象
    ② HttpServletResponse（响应对象）：封装服务器响应信息
    - 常用方法：
  - setContentType(String type)：设置响应内容类型
  - setCharacterEncoding(String charset)：设置响应编码
  - getWriter()：获取字符输出流（输出文本）
  - getOutputStream()：获取字节输出流（输出图片/文件）
  - sendRedirect(String path)：重定向
  - addCookie(Cookie cookie)：添加Cookie到响应
    ③ ServletConfig：Servlet配置对象，获取初始化参数、Servlet名称等
    ④ ServletContext：全局上下文对象（应用级），共享整个Web应用的资源
  - 常用方法：
    - getInitParameter(String name)：获取全局初始化参数
    - setAttribute(String name, Object value)：设置应用域属性
    - getAttribute(String name)：获取应用域属性
    - getRealPath(String path)：获取文件真实路径
5. 请求转发与重定向
    ① 请求转发（服务器内部跳转）
    - 特点：一次请求，地址栏不变，共享请求域数据，只能跳转到应用内部资源
    - 示例：
```java
// 在Servlet中转发到index.jsp
req.getRequestDispatcher("/index.jsp").forward(req, resp);
```
② 重定向（客户端跳转）
- 特点：两次请求，地址栏变化，不共享请求域数据，可跳转到外部资源
- 示例：
```java
// 重定向到登录页
resp.sendRedirect("/login.jsp");
```

--------------------------------------------------------------------------------------------------------------------------------------
四、Cookie与Session（状态管理）
1. Cookie（客户端状态管理）
    - 定义：服务器发送到客户端浏览器的小型文本文件，存储在客户端，用于记录用户状态
    - 特点：大小受限（约4KB），可设置过期时间，支持路径限制
    - 核心操作：
```java
// 创建Cookie
Cookie cookie = new Cookie("username", "zhangsan");
// 设置过期时间（秒），-1表示会话级（浏览器关闭失效），0表示删除
cookie.setMaxAge(3600 * 24);
// 设置路径（仅该路径下的资源可访问）
cookie.setPath("/");
// 添加到响应
resp.addCookie(cookie);
// 获取Cookie
Cookie[] cookies = req.getCookies();
if (cookies != null) {
    for (Cookie c : cookies) {
        if ("username".equals(c.getName())) {
            String value = c.getValue();
        }
    }
}
// 删除Cookie（设置过期时间为0）
Cookie cookie = new Cookie("username", "");
cookie.setMaxAge(0);
resp.addCookie(cookie);
```
2. Session（服务器端状态管理）
    - 定义：服务器为每个客户端创建的会话对象，存储在服务器，通过Cookie（JSESSIONID）关联客户端
    - 特点：存储容量大，安全，默认过期时间30分钟（可配置）
    - 核心操作：
```java
// 获取Session（无则创建）
HttpSession session = req.getSession();
// 获取Session（无则返回null）
// HttpSession session = req.getSession(false);
// 设置Session属性
session.setAttribute("user", user);
// 获取Session属性
User user = (User) session.getAttribute("user");
// 移除Session属性
session.removeAttribute("user");
// 设置过期时间（秒）
session.setMaxInactiveInterval(1800);
// 手动销毁Session
session.invalidate();
// 获取SessionID（对应客户端Cookie的JSESSIONID）
String sessionId = session.getId();
```
3. Cookie与Session区别
    - 存储位置：Cookie在客户端，Session在服务器
    - 安全性：Cookie低（可篡改），Session高
    - 容量：Cookie受限，Session无限制（受服务器内存影响）
    - 有效期：Cookie可长期存储，Session默认会话级（或配置过期时间）
    - 性能：Cookie不占用服务器资源，Session占用服务器内存

--------------------------------------------------------------------------------------------------------------------------------------
五、JSP（Java服务器页面）
1. 核心定义
    JSP是嵌入Java代码的HTML页面，本质是Servlet（JSP会被编译为.java/.class文件），用于简化页面开发。
2. JSP语法
    ① 脚本元素
    - <% 代码片段 %>：嵌入Java代码（如变量、循环、判断）
    - <%= 表达式 %>：输出表达式结果（等价于out.print()）
    - <%! 声明 %>：声明类的成员变量/方法（少用）
    ② 指令元素
    - page指令：配置JSP页面（编码、导入包、错误页等）
  ```jsp
  <%@ page language="java" contentType="text/html;charset=utf-8" import="java.util.List,com.example.User" errorPage="error.jsp" %>
  ```
- include指令：静态包含（编译时包含，合并为一个Servlet）
  ```jsp
  <%@ include file="header.jsp" %>
  ```
- taglib指令：引入标签库（如JSTL）
  ```jsp
  <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
  ```
③ 动作元素
- <jsp:include>：动态包含（运行时包含，各自编译为Servlet）
  ```jsp
  <jsp:include page="footer.jsp" />
  ```
- <jsp:forward>：请求转发
  ```jsp
  <jsp:forward page="login.jsp" />
  ```
- <jsp:param>：传递参数
  ```jsp
  <jsp:include page="user.jsp">
      <jsp:param name="id" value="1" />
  </jsp:include>
  ```
3. JSP内置对象（无需声明直接使用）
    - pageContext：页面上下文，获取其他内置对象，页面域
    - request：请求对象，请求域
    - session：会话对象，会话域
    - application：应用上下文，应用域
    - response：响应对象
    - out：输出流
    - page：当前JSP对象（等价于this）
    - config：ServletConfig对象
    - exception：异常对象（需page指令设置isErrorPage="true"）
4. 四大域对象（作用域从小到大）
    - page域（pageContext）：仅当前JSP页面有效
    - request域（request）：一次请求有效（转发有效，重定向无效）
    - session域（session）：一次会话有效（浏览器关闭前）
    - application域（application）：整个Web应用有效（服务器重启前）
5. EL表达式（表达式语言）
    - 作用：简化JSP中Java代码，获取域对象属性
    - 语法：${表达式}
    - 示例：
  ```jsp
  <%-- 获取request域的user对象的username属性 --%>
  ${requestScope.user.username}
  <%-- 简化（自动查找四大域） --%>
  ${user.username}
  <%-- 运算 --%>
  ${1 + 2}
  ${user.age > 18 ? "成年" : "未成年"}
  <%-- 获取参数 --%>
  ${param.id}
  <%-- 获取Cookie --%>
  ${cookie.username.value}
  ```
6. JSTL（JSP标准标签库）
    - 作用：替代JSP中的脚本片段，使页面更简洁
    - 核心标签（c标签）：
  ```jsp
  <%-- 导入核心标签库 --%>
  <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
  <%-- 条件判断 --%>
  <c:if test="${user.age > 18}">
      <p>成年</p>
  </c:if>
  <%-- 多条件判断 --%>
  <c:choose>
      <c:when test="${user.gender == '男'}">
          <p>男性</p>
      </c:when>
      <c:when test="${user.gender == '女'}">
          <p>女性</p>
      </c:when>
      <c:otherwise>
          <p>未知</p>
      </c:otherwise>
  </c:choose>
  <%-- 循环遍历 --%>
  <c:forEach items="${userList}" var="user" varStatus="status">
      <tr>
          <td>${status.index + 1}</td>
          <td>${user.username}</td>
          <td>${user.age}</td>
      </tr>
  </c:forEach>
  <%-- 设置域属性 --%>
  <c:set var="msg" value="Hello JSTL" scope="request" />
  <%-- 移除域属性 --%>
  <c:remove var="msg" scope="request" />
  ```

--------------------------------------------------------------------------------------------------------------------------------------
六、Filter（过滤器）
1. 核心定义
    Filter是运行在服务器端的组件，用于拦截请求/响应，实现统一处理（如编码过滤、登录验证、日志记录）。
2. 执行流程
    客户端请求 → Filter拦截 → 处理请求 → 转发/放行到目标资源 → 处理响应 → 返回客户端
3. 实现方式
```java
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import java.io.IOException;
// 拦截所有请求
@WebFilter("/*")
public class EncodingFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 初始化操作
    }
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // 设置请求编码
        request.setCharacterEncoding("utf-8");
        // 设置响应编码
        response.setCharacterEncoding("utf-8");
        response.setContentType("text/html;charset=utf-8");
        // 放行请求（执行下一个Filter或目标资源）
        chain.doFilter(request, response);
        // 响应处理（放行后执行）
        System.out.println("响应处理完成");
    }
    @Override
    public void destroy() {
        // 销毁操作
    }
}
```
4. 常用场景
    - 统一编码过滤：解决中文乱码
    - 登录验证：拦截未登录用户访问受保护资源
    - 日志记录：记录请求URL、访问时间、客户端IP
    - 权限控制：根据用户角色拦截请求

--------------------------------------------------------------------------------------------------------------------------------------
七、Listener（监听器）
1. 核心定义
    Listener是监听Web应用中事件（如上下文创建、会话创建、请求到达）的组件，用于响应事件并执行操作。
2. 常用监听器类型
    ① ServletContextListener：监听应用上下文的创建/销毁
```java
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
@WebListener
public class AppContextListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // 应用启动时执行（如初始化配置、加载缓存）
        System.out.println("Web应用启动");
        sce.getServletContext().setAttribute("appName", "JavaWebDemo");
    }
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // 应用关闭时执行（如释放资源、保存数据）
        System.out.println("Web应用关闭");
    }
}
```
② HttpSessionListener：监听会话的创建/销毁
```java
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
@WebListener
public class SessionListener implements HttpSessionListener {
    @Override
    public void sessionCreated(HttpSessionEvent se) {
        // 会话创建时执行（如统计在线人数）
        System.out.println("会话创建：" + se.getSession().getId());
    }
    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        // 会话销毁时执行（如减少在线人数）
        System.out.println("会话销毁：" + se.getSession().getId());
    }
}
```
③ ServletRequestListener：监听请求的创建/销毁
④ 属性监听器（ServletContextAttributeListener、HttpSessionAttributeListener、ServletRequestAttributeListener）：监听域属性的添加/移除/替换

--------------------------------------------------------------------------------------------------------------------------------------
八、文件上传与下载
1. 文件上传
    - 核心依赖：commons-fileupload、commons-io（需导入jar包）
    - 实现示例：
```java
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.List;
@WebServlet("/upload")
public class FileUploadServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 设置编码
        req.setCharacterEncoding("utf-8");
        // 创建文件上传工厂
        DiskFileItemFactory factory = new DiskFileItemFactory();
        // 设置临时文件目录
        factory.setRepository(new File(req.getServletContext().getRealPath("/temp")));
        // 创建文件上传处理器
        ServletFileUpload upload = new ServletFileUpload(factory);
        // 设置文件大小限制（10MB）
        upload.setFileSizeMax(10 * 1024 * 1024);
        try {
            // 解析请求
            List<FileItem> items = upload.parseRequest(req);
            for (FileItem item : items) {
                // 判断是否为普通表单字段
                if (item.isFormField()) {
                    String name = item.getFieldName();
                    String value = item.getString("utf-8");
                    System.out.println(name + "=" + value);
                } else {
                    // 处理文件字段
                    String fileName = item.getName();
                    // 获取文件存储路径
                    String path = req.getServletContext().getRealPath("/uploads");
                    File file = new File(path, fileName);
                    // 写入文件
                    item.write(file);
                    // 删除临时文件
                    item.delete();
                }
            }
            resp.getWriter().println("上传成功");
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().println("上传失败：" + e.getMessage());
        }
    }
}
```
2. 文件下载
```java
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
@WebServlet("/download")
public class FileDownloadServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 获取文件名
        String fileName = req.getParameter("fileName");
        // 解决中文文件名乱码
        fileName = new String(fileName.getBytes("iso-8859-1"), "utf-8");
        // 获取文件真实路径
        String path = req.getServletContext().getRealPath("/uploads/" + fileName);
        File file = new File(path);
        // 设置响应头（强制下载）
        resp.setContentType("application/octet-stream");
        resp.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "utf-8"));
        resp.setContentLength((int) file.length());
        // 读取文件并输出
        FileInputStream in = new FileInputStream(file);
        ServletOutputStream out = resp.getOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
        in.close();
        out.close();
    }
}
```

--------------------------------------------------------------------------------------------------------------------------------------
九、Web应用部署与配置
1. web.xml配置（传统方式，替代注解）
    - 配置Servlet：
```xml
<servlet>
    <servlet-name>HelloServlet</servlet-name>
    <servlet-class>com.example.HelloServlet</servlet-class>
    <!-- 初始化参数 -->
    <init-param>
        <param-name>encoding</param-name>
        <param-value>utf-8</param-value>
    </init-param>
    <!-- 启动优先级（数字越小越先启动） -->
    <load-on-startup>1</load-on-startup>
</servlet>
<servlet-mapping>
    <servlet-name>HelloServlet</servlet-name>
    <url-pattern>/hello</url-pattern>
</servlet-mapping>
```
- 配置Filter：
```xml
<filter>
    <filter-name>EncodingFilter</filter-name>
    <filter-class>com.example.EncodingFilter</filter-class>
</filter>
<filter-mapping>
    <filter-name>EncodingFilter</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>
```
- 配置Listener：
```xml
<listener>
    <listener-class>com.example.AppContextListener</listener-class>
</listener>
```
- 全局配置：
```xml
<!-- 欢迎页 -->
<welcome-file-list>
    <welcome-file>index.jsp</welcome-file>
    <welcome-file>index.html</welcome-file>
</welcome-file-list>
<!-- 错误页 -->
<error-page>
    <error-code>404</error-code>
    <location>/404.jsp</location>
</error-page>
<error-page>
    <exception-type>java.lang.Exception</exception-type>
    <location>/error.jsp</location>
</error-page>
```
2. 部署方式
    - 打包为WAR包：将Web应用打包为.war文件，放入Tomcat的webapps目录
    - 解压部署：将WAR包解压到webapps目录，Tomcat自动识别
    - 外置配置：通过Tomcat的conf/server.xml配置Context，指定应用路径

--------------------------------------------------------------------------------------------------------------------------------------
总结
1. JavaWeb核心是Servlet，JSP本质是Servlet，Filter/Listener用于扩展请求处理能力，Cookie/Session解决无状态问题
2. HTTP协议是JavaWeb的通信基础，需掌握请求/响应结构、状态码、GET/POST区别
3. 开发中优先使用注解替代web.xml配置，核心场景包括请求处理、状态管理、过滤拦截、文件上传下载，框架（如Spring MVC）是对Servlet的封装和简化
