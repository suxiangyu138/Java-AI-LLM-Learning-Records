

## 一、JavaWeb 核心整体图

本质上，JavaWeb 就是“浏览器 ⇄ Web 服务器 ⇄ Java 程序 ⇄ 数据库”的一整条链路，你要掌握的核心模块包括： [cloud.baidu](https://cloud.baidu.com/article/2769118)

- HTTP 协议与 Web 服务器（Tomcat）  
- JavaWeb 基础组件：Servlet、Filter、Listener  
- JSP / 模板渲染与 MVC 模式  
- 会话管理：Cookie、Session  
- 表单、参数与请求转发/重定向  
- JDBC / 连接池（为后续 MyBatis/JPA 打基础）  
- 静态资源与前端三件套（HTML/CSS/JavaScript）基础  
- 安全与常见问题（编码、XSS、CSRF、文件上传等）  

***

## 二、HTTP 协议与 Tomcat

这是所有 JavaWeb 的**底座**，你要能画出一次请求从浏览器走到 Servlet 再返回的路径。 [blog.csdn](https://blog.csdn.net/weixin_43741711/article/details/121780768)

1. HTTP 核心知识点  
- 请求方法：GET、POST（重点），以及 PUT、DELETE 等 REST 常用方法。 [blog.csdn](https://blog.csdn.net/m0_73980567/article/details/150848456)
- 状态码：200、302、304、400、401、403、404、500 的语义要清楚。 [developer.aliyun](https://developer.aliyun.com/article/1403789)
- 报文结构：请求行、请求头、请求体；响应行、响应头、响应体。 [cloud.baidu](https://cloud.baidu.com/article/2769118)

2. Tomcat 与 Web 应用结构  
- 认识 Web 应用目录结构：webapp、WEB-INF、web.xml、静态资源目录。 [blog.csdn](https://blog.csdn.net/weixin_43741711/article/details/121780768)
- 会在 Tomcat 中部署一个简单工程，理解 ContextPath 的含义和 URL 映射规则。 [blog.csdn](https://blog.csdn.net/m0_73980567/article/details/150848456)

***

## 三、Servlet / Filter / Listener（JavaWeb 三大件）

这是 JavaWeb 后端的核心：Servlet 处理请求，Filter 做请求链拦截，Listener 做“监听事件/生命周期”。 [blog.csdn](https://blog.csdn.net/suhfui/article/details/131749868)

1. Servlet 核心  
- 定义：运行在服务器端、处理 HTTP 请求并返回响应的 Java 程序，是 JavaWeb 的基石。 [developer.aliyun](https://developer.aliyun.com/article/1265222)
- 生命周期：加载实例化 → init → service(doGet/doPost) → destroy。 [developer.aliyun](https://developer.aliyun.com/article/1265222)
- 映射配置：  
  - 基于 web.xml 的 <servlet> + <servlet-mapping>。 [blog.csdn](https://blog.csdn.net/weixin_43741711/article/details/121780768)
  - 基于注解的 @WebServlet(urlPatterns = "/xxx")。 [blog.csdn](https://blog.csdn.net/m0_73980567/article/details/150848456)

2. HttpServletRequest 与 HttpServletResponse  
- Request 常用方法：getParameter、getParameterValues、getHeader、getMethod、getRequestURL、setAttribute/getAttribute、getRequestDispatcher().forward。 [cloud.baidu](https://cloud.baidu.com/article/2769118)
- Response 常用方法：setContentType、setHeader、getWriter/OutputStream、sendRedirect。 [cloud.baidu](https://cloud.baidu.com/article/2769118)

3. Filter（过滤器）  
- 定义：对请求/响应进行预处理/后处理的组件，典型场景有登录校验、统一编码、权限控制、日志记录等。 [blog.csdn](https://blog.csdn.net/weixin_43741711/article/details/121780768)
- 执行链：多个 Filter 形成责任链，可以按顺序依次执行。 [blog.csdn](https://blog.csdn.net/m0_73980567/article/details/150848456)

4. Listener（监听器）  
- 监听 ServletContext / Session / Request 的创建与销毁、属性变化等。 [blog.csdn](https://blog.csdn.net/weixin_43741711/article/details/121780768)
- 常见用途：在线人数统计、应用启动初始化资源等。 [blog.csdn](https://blog.csdn.net/m0_73980567/article/details/150848456)

***

## 四、JSP / MVC / 会话管理

虽然现在主流项目都用前后端分离 + 模板引擎，但 JSP/MVC/Session 仍然是基础面试高频点。 [cloud.tencent](https://cloud.tencent.com/developer/article/2462182)

1. JSP 与视图层  
- JSP 的本质是 Servlet，编写 JSP 最终会编译成 Servlet 类。 [blog.csdn](https://blog.csdn.net/qq_36437620/article/details/77184837)
- 常用特性：表达式、脚本、指令、EL 表达式和 JSTL 标签库。 [blog.csdn](https://blog.csdn.net/qq_36437620/article/details/77184837)

2. MVC 模式（在 JavaWeb 中如何落地）  
- Model：JavaBean / Service，负责业务逻辑和数据处理。 [cloud.tencent](https://cloud.tencent.com/developer/article/2462182)
- View：JSP/HTML，负责展示数据。 [cloud.tencent](https://cloud.tencent.com/developer/article/2462182)
- Controller：Servlet，负责接收请求、调用业务层、选择视图。 [cloud.tencent](https://cloud.tencent.com/developer/article/2462182)

3. 会话管理（Cookie / Session）  
- Cookie：保存在浏览器端的少量文本信息，用于标识用户、记住登录等。 [cloud.baidu](https://cloud.baidu.com/article/2769118)
- Session：保存在服务器端的会话数据，通过 SessionID 和 Cookie 关联。 [blog.csdn](https://blog.csdn.net/qq_36437620/article/details/77184837)
- 常见知识点：Session 生命周期、无状态 HTTP 如何通过 Cookie+Session 实现有状态会话。 [cloud.baidu](https://cloud.baidu.com/article/2769118)

4. 请求转发与重定向  
- 转发（forward）：一次请求，在服务器内部跳转，地址栏不变，能共享 request 域数据。 [blog.csdn](https://blog.csdn.net/weixin_43741711/article/details/121780768)
- 重定向（sendRedirect）：两次请求，客户端再次发起，地址栏改变，不能共享 request 域数据，但可以用 Session。 [cloud.baidu](https://cloud.baidu.com/article/2769118)

***

## 五、JDBC / 连接池 / 常见数据库访问模式

这里是从“能写页面”升级到“能做完整业务”的关键。 [cnblogs](https://www.cnblogs.com/ezan/p/18577425)

1. 原生 JDBC 基本流程  
- 导入驱动 → 注册驱动 → 获取 Connection → 准备 SQL → 创建 Statement/PreparedStatement → 执行 SQL → 处理 ResultSet → 关闭资源。 [cnblogs](https://www.cnblogs.com/ezan/p/18577425)
- PreparedStatement 防 SQL 注入，能够复用预编译。 [cnblogs](https://www.cnblogs.com/ezan/p/18577425)

2. 连接池思想（为后续 Druid/HikariCP/MyBatis 铺路）  
- 为什么要用连接池：建立数据库连接代价昂贵，频繁创建销毁会严重损耗性能。 [developer.aliyun](https://developer.aliyun.com/article/1570532)
- 常见连接池：C3P0、Druid、HikariCP 等。 [developer.aliyun](https://developer.aliyun.com/article/1570532)

3. DAO 层与三层架构  
- Web 层（Servlet/JSP） → Service 层（业务逻辑） → DAO 层（数据库访问）。 [congzhou09.github](https://congzhou09.github.io/knowledge/Java-web-%E6%8A%80%E6%9C%AF%E4%B8%8E%E6%9E%B6%E6%9E%84%E6%BC%94%E8%BF%9B%E5%8E%86%E5%8F%B2.html)
- 目标：Controller 不直接写 SQL，避免逻辑混乱。 [congzhou09.github](https://congzhou09.github.io/knowledge/Java-web-%E6%8A%80%E6%9C%AF%E4%B8%8E%E6%9E%B6%E6%9E%84%E6%BC%94%E8%BF%9B%E5%8E%86%E5%8F%B2.html)

***

## 六、前端基础与 Ajax（后端必须懂的那一部分）

你作为后端不需要精通前端，但必须能配合调接口、做简单页面联调。 [w3cschool](https://www.w3cschool.cn/article/54609829.html?fcode=article_64250021)

1. 前端三件套（只需掌握到能写简单页面）  
- HTML：常用标签、表单、表格、超链接。 [w3cschool](https://www.w3cschool.cn/article/54609829.html?fcode=article_64250021)
- CSS：基础布局、常用选择器。 [w3cschool](https://www.w3cschool.cn/article/54609829.html?fcode=article_64250021)
- JavaScript：DOM 操作、事件处理、简单表单校验。 [w3cschool](https://www.w3cschool.cn/article/54609829.html?fcode=article_64250021)

2. Ajax 与异步请求  
- Ajax 作用：在不刷新整页的情况下，与服务器进行异步数据交换，更新部分页面内容。 [blog.csdn](https://blog.csdn.net/qq_36437620/article/details/77184837)
- 能手写一个简单的 Ajax 请求，配合后端 Servlet 返回 JSON 数据。 [blog.csdn](https://blog.csdn.net/qq_36437620/article/details/77184837)

***

## 七、常见安全与综合能力点

这些是做项目、答辩、面试时的加分项。 [developer.aliyun](https://developer.aliyun.com/article/1570532)

- 编码与乱码问题：统一编码过滤器（例如UTF-8）、前后端 charset 保持一致。 [blog.csdn](https://blog.csdn.net/weixin_43741711/article/details/121780768)
- 表单重复提交、幂等设计的简单实践。 [developer.aliyun](https://developer.aliyun.com/article/1570532)
- XSS / CSRF 概念与基本防护思路（转义输出、CSRF Token 等）。 [blog.csdn](https://blog.csdn.net/m0_73980567/article/details/150848456)
- 文件上传与下载的基本实现与安全注意点（文件大小限制、类型校验）。 [blog.csdn](https://blog.csdn.net/m0_73980567/article/details/150848456)

***

## 八、适合你的学习顺序 & 下步安排

结合你现在 Java 后端 + AI 的目标，我建议你按“从底层到框架”的顺序推进 JavaWeb： [github](https://github.com/h2pl/JavaTutorial/blob/master/docs/JavaWeb/%E8%B5%B0%E8%BF%9BJavaWeb%E6%8A%80%E6%9C%AF%E4%B8%96%E7%95%8C%EF%BC%9AJavaWeb%E7%9A%84%E7%94%B1%E6%9D%A5%E5%92%8C%E5%9F%BA%E7%A1%80%E7%9F%A5%E8%AF%86.md)

1）先：HTTP + Tomcat + Servlet/Filter/Listener（能写简单登录/拦截）  
2）再：JSP + MVC + Session/Cookie（能做一个完整小网站）  
3）然后：JDBC + 连接池 + 三层架构（业务 + 数据库打通）  
4）最后：用 Spring MVC / Spring Boot 重构上面的小项目  

