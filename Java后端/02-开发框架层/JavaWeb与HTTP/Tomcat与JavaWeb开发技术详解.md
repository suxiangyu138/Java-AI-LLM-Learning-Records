Tomcat与JavaWeb开发技术详解

---------------------------------------------------------------------------------------------------------------------------------------
一、Web核心运作模型：客户端-服务器（C/S）架构
1. 核心角色
    - 客户端：发起请求的终端（浏览器、APP、Postman等）
    - 服务器：接收并处理请求，返回响应的服务端程序（Tomcat、Nginx、Apache等）
    - 网络：连接客户端与服务器的通信链路（HTTP/HTTPS协议）
2. 核心流程（一次完整Web请求）
    客户端发起请求 → 网络传输请求 → 服务器接收请求 → 服务器处理请求 → 服务器返回响应 → 客户端接收并渲染响应

---------------------------------------------------------------------------------------------------------------------------------------
二、一次浏览器访问网页的完整流程（以访问https://www.example.com为例）
```
1. 域名解析（DNS）
   - 浏览器缓存 → 系统缓存 → 路由器缓存 → 本地DNS服务器 → 根DNS服务器 → 顶级域DNS服务器 → 权威DNS服务器
   - 最终将域名转换为服务器IP地址（如192.168.1.100）
2. 建立TCP连接（三次握手）
   - 客户端：发送SYN包，请求建立连接
   - 服务器：返回SYN+ACK包，确认请求
   - 客户端：发送ACK包，连接建立完成
3. 发送HTTP/HTTPS请求
   - 请求格式：请求行 + 请求头 + 空行 + 请求体（可选）
   - 示例（GET请求）：
     GET /index.html HTTP/1.1
     Host: www.example.com
     User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)
     Accept: text/html,application/xhtml+xml
4. 服务器处理请求
   - Web服务器（Nginx）接收请求，转发给应用服务器（Tomcat）
   - 应用服务器执行业务逻辑（查询数据库、处理数据）
   - 生成响应数据（HTML/CSS/JS/图片等）
5. 服务器返回HTTP响应
   - 响应格式：状态行 + 响应头 + 空行 + 响应体
   - 示例：
     HTTP/1.1 200 OK
     Content-Type: text/html; charset=UTF-8
     Content-Length: 1024
     <!DOCTYPE html>
     <html>
       <head><title>Example</title></head>
       <body>Hello World</body>
     </html>
6. 关闭TCP连接（四次挥手，可选）
   - 若请求头包含Connection: keep-alive，连接会复用，不立即关闭
   - 否则执行四次挥手关闭连接
7. 客户端渲染页面
   - 浏览器解析HTML，构建DOM树
   - 解析CSS，构建CSSOM树
   - 合并DOM与CSSOM，生成渲染树
   - 布局（Layout）：计算元素位置和大小
   - 绘制（Paint）：将像素渲染到屏幕
   - 若有JS，执行JS代码，可能修改DOM/CSSOM
   - 若有静态资源（图片、字体），发起新的请求加载
```

---------------------------------------------------------------------------------------------------------------------------------------
三、HTTP协议核心（Web通信的基础）
1. 协议特点
    - 无状态：每次请求独立，服务器不保存客户端状态（需Cookie/Session维持状态）
    - 无连接：默认一次请求对应一次连接（HTTP/1.1引入keep-alive支持长连接）
    - 基于文本：请求和响应均为文本格式，易解析
    - 灵活：支持多种请求方法、数据格式
2. 请求方法（常用）
    - GET：获取资源（幂等，无请求体，参数在URL）
    - POST：提交数据（非幂等，有请求体，参数在请求体）
    - PUT：更新资源（幂等，替换整个资源）
    - DELETE：删除资源（幂等）
    - HEAD：仅获取响应头
    - OPTIONS：获取服务器支持的请求方法
3. 响应状态码
    - 1xx：信息类，请求已接收，继续处理
    - 2xx：成功，请求处理完成（200 OK、201 Created）
    - 3xx：重定向，需进一步操作（301永久重定向、302临时重定向、304缓存未修改）
    - 4xx：客户端错误（400参数错误、401未授权、403禁止访问、404资源不存在）
    - 5xx：服务器错误（500内部错误、502网关错误、503服务不可用）
4. HTTPS与HTTP的区别
    - HTTPS = HTTP + SSL/TLS：在TCP之上增加加密层
    - 核心优势：数据加密、身份认证、防篡改
    - 额外流程：TCP连接建立后，先完成SSL/TLS握手（交换证书、协商加密算法、生成会话密钥）

---------------------------------------------------------------------------------------------------------------------------------------
四、Web服务器与应用服务器的分工
1. Web服务器（静态资源）
   - 代表：Nginx、Apache、IIS
   - 功能：处理HTTP请求、返回静态资源（HTML/CSS/JS/图片）、反向代理、负载均衡、缓存
   - 特点：轻量、高性能、并发能力强
2. 应用服务器（动态资源）
   - 代表：Tomcat、Jetty、JBoss、WebLogic
   - 功能：运行后端代码（Java/PHP/Python）、处理动态请求、与数据库交互、生成动态响应
   - 特点：支持应用运行环境，可处理复杂业务逻辑
3. 典型架构
   客户端 → Nginx（静态资源/反向代理） → Tomcat（动态请求） → 数据库（MySQL/Redis）

---------------------------------------------------------------------------------------------------------------------------------------
五、动态Web页面与静态Web页面的区别
1. 静态Web页面
   - 内容固定：由HTML/CSS/JS组成，存储在服务器硬盘
   - 响应快：服务器直接返回文件，无需处理
   - 示例：纯HTML页面、图片、CSS文件
   - 缺点：内容修改需手动更新文件
2. 动态Web页面
   - 内容动态生成：服务器根据请求参数、数据库数据生成HTML
   - 示例：电商商品页、用户个人中心、搜索结果页
   - 技术：Java(Servlet/JSP)、PHP、Python(Django/Flask)、Node.js
   - 优点：内容实时更新，交互性强

---------------------------------------------------------------------------------------------------------------------------------------
六、Cookie与Session（维持用户状态）
1. Cookie
   - 存储位置：客户端浏览器（本地文件）
   - 特点：大小限制（4KB）、可设置过期时间、随请求自动发送
   - 用途：保存用户偏好、记住登录状态、跟踪用户行为
   - 示例：登录后服务器返回Set-Cookie: user=123; Path=/; Max-Age=3600
2. Session
   - 存储位置：服务器内存/数据库/Redis
   - 特点：基于Cookie（SessionID存储在Cookie）、无大小限制、更安全
   - 流程：
     客户端首次请求 → 服务器创建Session，生成SessionID → 服务器返回Set-Cookie: JSESSIONID=abc123 → 客户端后续请求携带JSESSIONID → 服务器通过SessionID找到对应Session

---------------------------------------------------------------------------------------------------------------------------------------
七、现代Web进阶：AJAX与前后端分离
1. AJAX（异步JavaScript和XML）
   - 核心：异步请求，无需刷新页面即可获取数据
   - 技术：XMLHttpRequest、Fetch API、Axios
   - 流程：
     页面加载完成 → JS发起AJAX请求 → 服务器返回JSON数据 → JS更新页面局部内容
   - 示例（Fetch API）：
     fetch('/api/data')
       .then(response => response.json())
       .then(data => {
         document.getElementById('content').innerText = data.msg;
       });
2. 前后端分离架构
   - 前端：独立部署，负责页面渲染和交互（Vue/React/Angular）
   - 后端：提供API接口，返回JSON数据（Spring Boot/Express/Django）
   - 通信：前端通过AJAX/HTTP请求调用后端API
   - 优势：前后端解耦，开发效率高，便于多端复用（Web/APP/小程序）

---------------------------------------------------------------------------------------------------------------------------------------

### 总结
1. Web核心是客户端与服务器的HTTP通信，核心流程为：DNS解析→TCP连接→请求→处理→响应→渲染
2. HTTP是无状态协议，依赖Cookie/Session维持用户状态，HTTPS提供加密保障
3. Web服务器处理静态资源，应用服务器处理动态请求，现代架构多采用前后端分离
4. 一次完整的页面访问涉及网络、协议、服务器、客户端渲染等多个环节的协同工作
5. 动态Web的核心是服务器根据请求动态生成响应，而非返回固定文件

---------------------------------------------------------------------------------------------------------------------------------------
一、Tomcat基本定义
1. 官方定位：Apache Tomcat是开源的Java Servlet容器，同时支持JavaServer Pages (JSP)、Expression Language (EL)和WebSocket技术
2. 核心角色：轻量级应用服务器，主要用于运行Java Web应用程序（Servlet/JSP）
3. 归属：Apache软件基金会旗下项目，开源免费，跨平台（Windows/Linux/Mac）
4. 定位：属于中间件，介于Web服务器（Nginx/Apache）和数据库之间，处理动态Java请求

---------------------------------------------------------------------------------------------------------------------------------------
二、Tomcat核心特性
1. 轻量级：体积小（核心包仅几十MB）、启动快、资源占用低
2. 兼容性：完全兼容Servlet和JSP规范（支持Servlet 5.0、JSP 3.0等最新规范）
3. 可扩展性：支持插件扩展、自定义配置、集群部署
4. 模块化：核心功能拆分为多个模块（Catalina、Coyote、Jasper等）
5. 易部署：支持WAR包部署、热部署（无需重启更新应用）
6. 内置功能：自带HTTP服务器（可直接对外提供服务）、管理控制台、日志系统

---------------------------------------------------------------------------------------------------------------------------------------
三、Tomcat核心组件
1. 核心模块
   - Catalina：Tomcat的核心引擎，负责Servlet容器管理、请求处理
   - Coyote：HTTP/1.1和TCP连接处理模块，负责网络通信
   - Jasper：JSP解析引擎，将JSP文件编译为Servlet类
   - Realm：安全认证模块，处理用户登录认证
   - Connector：连接器，监听指定端口，接收客户端请求（默认8080端口）
2. 容器结构（从大到小）
   - Server：整个Tomcat实例，包含一个或多个Service
   - Service：包含一个Connector和一个Engine，关联连接器与引擎
   - Engine：处理一个Service下所有Connector的请求，默认引擎为Catalina
   - Host：虚拟主机，对应一个域名（默认localhost）
   - Context：Web应用上下文，对应一个具体的Web应用（默认ROOT应用）

---------------------------------------------------------------------------------------------------------------------------------------
四、Tomcat部署方式
1. WAR包部署
   - 将Web应用打包为.war文件，放入Tomcat的webapps目录
   - 启动Tomcat自动解压并部署，访问路径：http://IP:8080/包名
   - 示例：将demo.war放入webapps，访问http://localhost:8080/demo
2. 目录部署
   - 将Web应用的目录直接复制到webapps目录
   - 示例：webapps/demo/WEB-INF/...，访问路径同上
3. 配置文件部署
   - 在conf/Catalina/localhost目录下创建XXX.xml文件
   - 配置文件指定应用路径和目录位置，实现自定义部署路径
   - 示例（demo.xml）：
     <Context docBase="D:/web/demo" path="/demo" reloadable="true"/>
4. 热部署
   - 修改conf/context.xml，设置reloadable="true"
   - 应用代码修改后自动重新加载，无需重启Tomcat（开发环境常用）

---------------------------------------------------------------------------------------------------------------------------------------
五、Tomcat核心配置文件
1. conf/server.xml：核心配置文件
   - 配置Service、Connector、Engine、Host等核心组件
   - 常用配置：修改端口（默认8080）、配置线程池、设置连接超时
2. conf/web.xml：全局Web应用配置
   - 配置MIME类型、默认Servlet、错误页面、安全约束
3. conf/context.xml：全局上下文配置
   - 配置数据源、资源链接、热部署等全局参数
4. conf/tomcat-users.xml：用户认证配置
   - 配置管理控制台的用户名、密码、角色（manager-gui、admin-gui）
5. 应用内WEB-INF/web.xml：单个应用的配置
   - 配置Servlet、Filter、Listener、初始化参数

---------------------------------------------------------------------------------------------------------------------------------------
六、Tomcat运行模式
1. 独立运行模式
   - 直接启动Tomcat（startup.bat/startup.sh）
   - Tomcat自带HTTP服务器，监听8080端口，直接处理客户端请求
   - 优点：简单易用，适合开发环境
   - 缺点：高并发场景性能不足
2. 反向代理模式（生产环境常用）
   - Nginx/Apache作为前端Web服务器，处理静态资源
   - 动态请求通过反向代理转发给Tomcat处理
   - 架构：客户端 → Nginx（80/443端口） → Tomcat（8080端口）
   - 优点：静态资源处理高效，可实现负载均衡、动静分离

---------------------------------------------------------------------------------------------------------------------------------------
七、Tomcat常用端口
1. 8080：默认HTTP端口，用于接收HTTP请求
2. 8005：关闭端口，发送SHUTDOWN指令可关闭Tomcat
3. 8009：AJP端口，用于与Apache HTTP Server通信
4. 8443：HTTPS端口，用于接收HTTPS请求（需配置SSL证书）

---------------------------------------------------------------------------------------------------------------------------------------
八、Tomcat启动与停止
1. Windows系统
   - 启动：bin/startup.bat
   - 停止：bin/shutdown.bat
2. Linux/Mac系统
   - 启动：bin/startup.sh
   - 停止：bin/shutdown.sh
   - 强制停止：kill -9 <Tomcat进程ID>
3. 验证启动成功
   - 访问http://localhost:8080，出现Tomcat默认页面
   - 查看logs/catalina.out日志，无ERROR信息

---------------------------------------------------------------------------------------------------------------------------------------
九、Tomcat适用场景
1. 开发环境：Java Web应用开发、调试（Spring MVC、Struts等框架）
2. 中小型项目：访问量中等的Web应用部署（电商、企业官网、后台管理系统）
3. 集群部署：多台Tomcat组成集群，配合Nginx实现负载均衡
4. 嵌入式使用：可嵌入Java程序中作为内置服务器（Spring Boot内置Tomcat）

---------------------------------------------------------------------------------------------------------------------------------------
十、Tomcat与其他服务器的区别
1. Tomcat vs Nginx/Apache
   - Tomcat：应用服务器，处理动态Java请求（Servlet/JSP）
   - Nginx/Apache：Web服务器，处理静态资源，擅长高并发、反向代理
2. Tomcat vs JBoss/WebLogic
   - Tomcat：轻量级，仅实现Servlet/JSP规范，无EJB、JMS等企业级功能
   - JBoss/WebLogic：重量级应用服务器，支持完整Java EE规范，功能更全但资源占用高

---------------------------------------------------------------------------------------------------------------------------------------

### 总结
1. Tomcat是开源轻量级Java Web应用服务器，核心功能是运行Servlet/JSP程序
2. 核心组件包括Catalina（引擎）、Coyote（连接器）、Jasper（JSP解析），默认8080端口提供HTTP服务
3. 部署方式有WAR包、目录、配置文件三种，生产环境常配合Nginx做反向代理
4. 适合开发环境和中小型项目，轻量、易用、开源免费是其核心优势
5. 核心配置文件为server.xml、web.xml、context.xml，可自定义端口、部署路径、认证等参数

---------------------------------------------------------------------------------------------------------------------------------------
一、环境准备
1. 基础环境
   - JDK 8及以上（配置JAVA_HOME环境变量）
   - Tomcat 8/9/10（解压即可用，配置CATALINA_HOME可选）
   - 开发工具：IDEA/Eclipse（推荐IDEA）
2. 环境验证
   - 打开命令行，输入java -version，显示JDK版本则配置成功
   - 启动Tomcat，访问http://localhost:8080，出现Tomcat默认页面则成功

---------------------------------------------------------------------------------------------------------------------------------------
二、手动创建第一个JavaWeb应用（无IDE，理解底层结构）
1. 创建Web应用目录结构
   新建文件夹（如HelloWeb），按以下结构创建目录：
   HelloWeb/
   └── WEB-INF/
       ├── classes/ （存放编译后的class文件）
       ├── lib/     （存放第三方jar包，暂无）
       └── web.xml  （核心配置文件）
   └── index.html   （静态页面，可选）
2. 编写Servlet类（核心动态组件）
   - 新建文件：HelloWeb/WEB-INF/classes/com/example/HelloServlet.java
   ```java
   package com.example;
   import javax.servlet.*;
   import javax.servlet.http.*;
   import java.io.IOException;
   import java.io.PrintWriter;
   // 自定义Servlet，继承HttpServlet
   public class HelloServlet extends HttpServlet {
       // 处理GET请求
       @Override
       protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
           // 设置响应内容类型
           response.setContentType("text/html;charset=UTF-8");
           // 获取输出流，向浏览器写数据
           PrintWriter out = response.getWriter();
           // 输出内容
           out.println("<html>");
           out.println("<head><title>第一个JavaWeb应用</title></head>");
           out.println("<body>");
           out.println("<h1>Hello JavaWeb!</h1>");
           out.println("<p>请求路径：" + request.getContextPath() + "</p>");
           out.println("</body>");
           out.println("</html>");
       }
   }
   ```
3. 编译Servlet类
   - 找到Tomcat安装目录下的lib/servlet-api.jar
   - 命令行进入HelloWeb/WEB-INF/classes目录，执行编译命令：
     javac -cp "Tomcat安装目录/lib/servlet-api.jar" com/example/HelloServlet.java
   - 编译成功后，classes目录下会生成com/example/HelloServlet.class文件
4. 配置web.xml（映射Servlet访问路径）
   - 新建文件：HelloWeb/WEB-INF/web.xml
   ```xml
   <?xml version="1.0" encoding="UTF-8"?>
   <web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee
            http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd"
            version="4.0">
       <!-- 注册Servlet -->
       <servlet>
           <servlet-name>HelloServlet</servlet-name>
           <servlet-class>com.example.HelloServlet</servlet-class>
       </servlet>
       <!-- 映射Servlet访问路径 -->
       <servlet-mapping>
           <servlet-name>HelloServlet</servlet-name>
           <url-pattern>/hello</url-pattern>
       </servlet-mapping>
       <!-- 设置默认首页 -->
       <welcome-file-list>
           <welcome-file>index.html</welcome-file-list>
       </welcome-file-list>
   </web-app>
   ```
5. 可选：创建静态首页index.html
   - 新建文件：HelloWeb/index.html
   ```html
   <!DOCTYPE html>
   <html>
   <head>
       <meta charset="UTF-8">
       <title>首页</title>
   </head>
   <body>
       <h1>欢迎访问第一个JavaWeb应用</h1>
       <a href="hello">访问HelloServlet</a>
   </body>
   </html>
   ```
6. 部署应用到Tomcat
   - 将HelloWeb文件夹复制到Tomcat的webapps目录下
   - 启动Tomcat（bin/startup.bat/startup.sh）
7. 访问应用
   - 访问静态首页：http://localhost:8080/HelloWeb
   - 访问Servlet：http://localhost:8080/HelloWeb/hello
   - 成功显示"Hello JavaWeb!"则应用运行正常

---------------------------------------------------------------------------------------------------------------------------------------
三、使用IDEA创建JavaWeb应用（推荐，简化流程）
1. 新建JavaWeb项目
   - 打开IDEA，选择New Project → 选择Java → 勾选Web Application → 命名项目（如HelloWeb）
   - 配置Tomcat：File → Project Structure → Modules → 选择项目 → Dependencies → 添加Library → 选择Tomcat库
2. 编写Servlet类
   - 在src目录下创建包com.example，新建HelloServlet.java（代码同手动创建的Servlet）
3. 配置Servlet（两种方式）
   方式1：web.xml配置（同手动创建的web.xml，IDEA已自动生成WEB-INF/web.xml）
   方式2：注解配置（Servlet 3.0+支持，无需web.xml）
   ```java
   package com.example;
   import javax.servlet.annotation.WebServlet;
   import javax.servlet.http.*;
   import java.io.IOException;
   import java.io.PrintWriter;
   // 注解映射访问路径，替代web.xml配置
   @WebServlet("/hello")
   public class HelloServlet extends HttpServlet {
       @Override
       protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
           response.setContentType("text/html;charset=UTF-8");
           PrintWriter out = response.getWriter();
           out.println("<h1>Hello JavaWeb (IDEA版)</h1>");
       }
   }
   ```
4. 配置Tomcat运行环境
   - 点击IDEA右上角Add Configuration → 选择Tomcat Server → Local
   - 配置Tomcat安装目录 → 选择Deployment → 添加项目 → 应用上下文路径设为/HelloWeb
5. 运行项目
   - 点击运行按钮，IDEA自动部署应用并启动Tomcat
   - 访问http://localhost:8080/HelloWeb/hello，查看结

---------------------------------------------------------------------------------------------------------------------------------------
四、核心知识点解释
1. Web应用目录结构
   - WEB-INF：受保护目录，浏览器无法直接访问，存放核心配置和类文件
   - classes：存放编译后的Java类（Servlet、工具类等）
   - lib：存放第三方jar包（如数据库驱动）
   - web.xml：Web应用的核心配置文件，映射Servlet、配置过滤器等
2. Servlet核心
   - Servlet是运行在服务器端的Java程序，处理客户端请求并返回响应
   - 继承HttpServlet，重写doGet/doPost方法处理GET/POST请求
   - HttpServletRequest：封装客户端请求信息（参数、请求头、Cookie等）
   - HttpServletResponse：封装服务器响应信息（设置响应头、输出内容）
3. 访问路径说明
   - http://localhost:8080/：Tomcat根路径
   - /HelloWeb：应用上下文路径（对应webapps下的HelloWeb文件夹）
   - /hello：Servlet的映射路径（对应@WebServlet("/hello")或web.xml中的url-pattern）
4. 乱码解决
   - 响应乱码：response.setContentType("text/html;charset=UTF-8")
   - 请求乱码（POST）：request.setCharacterEncoding("UTF-8")

---------------------------------------------------------------------------------------------------------------------------------------
五、常见问题排查
1. 404错误：页面未找到
   - 检查应用上下文路径是否正确（如HelloWeb是否拼写错误）
   - 检查Servlet映射路径是否正确（如/hello是否写成/Hello）
   - 检查Tomcat是否已部署应用（webapps目录下是否有HelloWeb文件夹）
2. 500错误：服务器内部错误
   - 查看Tomcat日志（logs/catalina.out），定位代码错误
   - 检查Servlet类是否编译成功，包名是否正确
   - 检查web.xml配置是否有语法错误
3. 编译错误：找不到HttpServlet类
   - 确认已引入servlet-api.jar（Tomcat的lib目录下）
   - IDEA中确认已添加Tomcat库依赖

---------------------------------------------------------------------------------------------------------------------------------------

### 总结
1. 第一个JavaWeb应用核心是Servlet，实现客户端请求的动态处理
2. 手动创建需遵循Web目录结构，编译Servlet并配置web.xml；IDEA可简化流程，支持注解配置
3. 部署方式：将应用目录复制到Tomcat的webapps，启动Tomcat即可访问
4. 核心路径：IP:端口/上下文路径/Servlet映射路径（如localhost:8080/HelloWeb/hello）
5. 常见问题：404（路径错误）、500（代码/配置错误）、类找不到（依赖缺失）

---------------------------------------------------------------------------------------------------------------------------------------
一、Servlet基本定义
1. 核心概念
    Servlet是运行在服务器端的Java程序，用于处理客户端（浏览器/APP）的HTTP请求，并返回动态响应，是JavaWeb的核心技术。
2. 本质
    Servlet是一个接口（javax.servlet.Servlet），自定义Servlet需实现该接口或继承其实现类（如HttpServlet）。
3. 核心作用
    - 接收客户端请求参数（GET/POST）
    - 处理业务逻辑（调用服务层、操作数据库）
    - 生成动态响应（HTML/JSON/XML）
    - 与服务器交互（获取会话、上下文信息）

---------------------------------------------------------------------------------------------------------------------------------------
二、Servlet核心接口与类
1. 核心接口
    - Servlet：所有Servlet的根接口，定义核心生命周期方法
  void init(ServletConfig config)：初始化方法，仅调用一次
  void service(ServletRequest req, ServletResponse res)：处理请求，每次请求调用
  void destroy()：销毁方法，服务器关闭时调用
  ServletConfig getServletConfig()：获取配置信息
  String getServletInfo()：获取Servlet信息
    - ServletRequest：封装客户端请求信息
    - ServletResponse：封装服务器响应信息
    - ServletConfig：获取Servlet初始化参数和上下文
    - ServletContext：Web应用上下文，全局共享数据
2. 常用实现类
    - GenericServlet：实现Servlet接口，通用Servlet，无HTTP相关方法
    - HttpServlet：继承GenericServlet，专门处理HTTP请求，重写doGet/doPost等方法

---------------------------------------------------------------------------------------------------------------------------------------
三、Servlet生命周期（核心）
```
1. 加载阶段：服务器启动或首次请求时，加载Servlet类到内存
2. 实例化阶段：创建Servlet对象（默认单例）
3. 初始化阶段：调用init()方法，仅执行一次，可初始化资源（如数据库连接）
4. 服务阶段：每次请求调用service()方法，HttpServlet中service会根据请求方法（GET/POST）调用对应的doGet/doPost
5. 销毁阶段：服务器关闭时调用destroy()方法，释放资源（如关闭连接）
```
示例：生命周期演示
```java
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;
@WebServlet("/lifeCycle")
public class LifeCycleServlet implements Servlet {
    // 构造方法（实例化阶段）
    public LifeCycleServlet() {
        System.out.println("1. Servlet实例化");
    }
    // 初始化阶段
    @Override
    public void init(ServletConfig config) throws ServletException {
        System.out.println("2. Servlet初始化");
    }
    // 服务阶段
    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        System.out.println("3. 处理请求");
        res.setContentType("text/html;charset=UTF-8");
        res.getWriter().write("Servlet生命周期演示");
    }
    // 销毁阶段
    @Override
    public void destroy() {
        System.out.println("4. Servlet销毁");
    }
    @Override
    public ServletConfig getServletConfig() {
        return null;
    }
    @Override
    public String getServletInfo() {
        return null;
    }
}
```
注意：Servlet默认单例，多个请求共享同一个Servlet对象，需注意线程安全（避免定义成员变量）。

---------------------------------------------------------------------------------------------------------------------------------------
四、HttpServlet核心用法
1. 基础结构
```java
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
@WebServlet("/httpServletDemo")
public class HttpServletDemo extends HttpServlet {
    // 处理GET请求
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 设置响应编码，解决乱码
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        // 获取请求参数
        String name = request.getParameter("name");
        if (name == null) {
            name = "默认用户";
        }
        // 输出响应
        out.write("<h1>Hello " + name + "</h1>");
    }
    // 处理POST请求
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 设置请求编码，解决POST参数乱码
        request.setCharacterEncoding("UTF-8");
        // 复用GET逻辑
        doGet(request, response);
    }
}
```
2. 核心方法
    - doGet(HttpServletRequest req, HttpServletResponse resp)：处理GET请求
    - doPost(HttpServletRequest req, HttpServletResponse resp)：处理POST请求
    - doPut/doDelete：处理PUT/DELETE请求
    - init()：重写初始化逻辑
    - destroy()：重写销毁逻辑
3. 请求与响应操作
    （1）HttpServletRequest（请求）
    - 获取参数：
  String getParameter(String name)：获取单个参数
  String[] getParameterValues(String name)：获取多值参数（如复选框）
  Map<String, String[]> getParameterMap()：获取所有参数
    - 获取请求信息：
  String getMethod()：获取请求方法（GET/POST）
  String getRequestURI()：获取请求URI
  String getContextPath()：获取应用上下文路径
  String getRemoteAddr()：获取客户端IP
    （2）HttpServletResponse（响应）
    - 设置响应：
  void setContentType(String type)：设置响应类型（如text/html、application/json）
  void setCharacterEncoding(String charset)：设置响应编码
  PrintWriter getWriter()：获取字符输出流（输出文本）
  ServletOutputStream getOutputStream()：获取字节输出流（输出图片/文件）
    - 重定向：
  void sendRedirect(String url)：重定向到指定URL（客户端跳转，地址栏变化）

---------------------------------------------------------------------------------------------------------------------------------------
五、Servlet配置方式
1. 注解配置（Servlet 3.0+推荐）
```java
// 基础配置：映射路径
@WebServlet("/demo")
// 多路径映射
@WebServlet({"/demo1", "/demo2"})
// 完整配置
@WebServlet(
    name = "DemoServlet",          // Servlet名称
    urlPatterns = "/demo",         // 映射路径
    loadOnStartup = 1,             // 服务器启动时加载（默认首次请求加载）
    initParams = {                 // 初始化参数
        @WebInitParam(name = "key1", value = "value1"),
        @WebInitParam(name = "key2", value = "value2")
    }
)
```
2. XML配置（web.xml）
```xml
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee" version="4.0">
    <servlet>
        <servlet-name>DemoServlet</servlet-name>
        <servlet-class>com.example.DemoServlet</servlet-class>
        <!-- 初始化参数 -->
        <init-param>
            <param-name>key1</param-name>
            <param-value>value1</param-value>
        </init-param>
        <!-- 启动时加载 -->
        <load-on-startup>1</load-on-startup>
    </servlet>
    <servlet-mapping>
        <servlet-name>DemoServlet</servlet-name>
        <url-pattern>/demo</url-pattern>
    </servlet-mapping>
</web-app>
```

---------------------------------------------------------------------------------------------------------------------------------------
六、ServletContext（应用上下文）
1. 核心作用：全局共享数据、获取应用信息、读取资源文件
2. 使用示例
```java
@WebServlet("/contextDemo")
public class ContextDemoServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 获取ServletContext对象
        ServletContext context = getServletContext();
        // 1. 设置全局共享数据
        context.setAttribute("appName", "第一个JavaWeb应用");
        // 2. 获取全局数据
        String appName = (String) context.getAttribute("appName");
        // 3. 获取初始化参数（web.xml中配置）
        String version = context.getInitParameter("appVersion");
        // 4. 获取应用路径
        String contextPath = context.getContextPath();
        // 5. 读取资源文件（WEB-INF/config.properties）
        String path = context.getRealPath("/WEB-INF/config.properties");
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write("应用名称：" + appName + "<br>版本：" + version);
    }
}
```
web.xml配置全局参数：
```xml
<context-param>
    <param-name>appVersion</param-name>
    <param-value>1.0</param-value>
</context-param>
```

---------------------------------------------------------------------------------------------------------------------------------------
七、Servlet常见问题
1. 乱码问题
    - 响应乱码：response.setContentType("text/html;charset=UTF-8")
    - POST请求参数乱码：request.setCharacterEncoding("UTF-8")
    - GET请求参数乱码：修改Tomcat的conf/server.xml，添加URIEncoding="UTF-8"
  <Connector port="8080" URIEncoding="UTF-8"/>
2. 线程安全问题
    - 原因：Servlet单例，多个请求共享成员变量
    - 解决：避免定义成员变量，使用局部变量；必要时加锁（不推荐，影响性能）
3. 重定向与转发区别
    - 重定向（sendRedirect）：客户端跳转，地址栏变化，两次请求，无法共享request数据
    - 转发（RequestDispatcher）：服务器端跳转，地址栏不变，一次请求，可共享request数据
  示例：request.getRequestDispatcher("/index.html").forward(request, response);

---------------------------------------------------------------------------------------------------------------------------------------

### 总结
1. Servlet是JavaWeb核心，运行在服务器端，处理HTTP请求并返回响应，生命周期为：实例化→初始化→服务→销毁（单例）。
2. HttpServlet是最常用实现类，重写doGet/doPost处理不同请求，通过HttpServletRequest/HttpServletResponse操作请求和响应。
3. 配置方式有注解（Servlet3.0+）和XML两种，注解更简洁。
4. ServletContext实现全局数据共享，需注意乱码和线程安全问题。
5. 核心应用：接收参数、处理业务、生成动态响应、跳转页面。

---------------------------------------------------------------------------------------------------------------------------------------
一、Bookstore应用核心定位
1. 应用类型
    Bookstore（网上书店）是JavaWeb领域的经典实战项目，属于中小型企业级电商类应用，常作为JavaWeb、SSM、Spring Boot等技术栈的入门实战标杆项目。
2. 核心目标
    模拟线上书店的完整业务闭环，实现书籍展示、用户管理、购物车操作、订单处理等核心电商场景功能，技术层面综合应用JavaWeb核心技术，体现企业级应用的基础开发思路。
3. 适用场景
    - 新手学习：通过实战掌握JavaWeb全栈开发流程，涵盖前端、后端、数据库的完整交互；
    - 企业参考：作为中小型图书类网站、轻量级电商平台的基础架构模板；
    - 面试场景：入门级实战项目，可体现开发者的JavaWeb基础开发能力和业务逻辑设计能力。

---------------------------------------------------------------------------------------------------------------------------------------
二、Bookstore应用核心功能模块
1. 用户模块
    - 注册功能：用户填写账号、密码、邮箱、手机号等信息，完成注册流程，包含前端数据格式校验、后端重复账号校验、密码加密存储等逻辑；
    - 登录功能：验证账号密码正确性，通过Session保存用户登录状态，支持Cookie实现"记住密码"功能；
    - 个人中心：支持查看和修改个人基本信息、查询历史订单记录、重置登录密码。
2. 书籍模块
    - 书籍列表：分页展示全量书籍，支持按书籍分类、销量、价格区间、上架时间等条件筛选；
    - 书籍详情：展示书籍封面、书名、作者、定价、库存、书籍简介、读者评价等信息；
    - 书籍分类：按小说、科技、教育、历史等类别划分书籍，支持分类筛选展示；
    - 搜索功能：支持按书名、作者、ISBN号等关键词模糊搜索书籍。
3. 购物车模块
    - 添加购物车：将指定书籍加入购物车，支持选择购买数量，加入前校验书籍库存是否充足；
    - 购物车管理：修改购物车内书籍的购买数量、删除指定商品、清空购物车；
    - 价格计算：自动计算购物车内所有商品的总价、优惠金额（如有折扣）、实付金额。
4. 订单模块
    - 生成订单：从购物车选择商品生成订单，填写收货地址、联系电话、支付方式等信息，生成后扣减对应书籍库存；
    - 订单列表：分页展示用户所有订单，支持按订单创建时间、订单状态筛选；
    - 订单详情：查看订单包含的商品列表、商品单价、数量、订单总价、收货信息、物流状态；
    - 订单状态管理：支持订单状态流转，包含待付款、已付款、已发货、已完成、已取消等状态。
5. 后台管理模块（可选）
    - 书籍管理：新增、编辑、删除书籍信息，修改书籍库存、定价、封面图片；
    - 订单管理：查看全平台订单，修改订单状态（如确认付款、标记发货、取消订单）；
    - 用户管理：查看所有注册用户，支持禁用/启用用户账号、重置用户密码；
    - 分类管理：新增书籍分类、编辑分类名称、删除未关联书籍的分类。

---------------------------------------------------------------------------------------------------------------------------------------
三、Bookstore应用技术架构
1. 入门级架构（传统JavaWeb模式）
    - 前端技术：HTML、CSS、JavaScript、JSP、EL表达式、JSTL标签库；
    - 后端技术：Servlet、Filter、Listener、JDBC（原生）/DBUtils（简化数据库操作）；
    - 数据库：MySQL（存储用户、书籍、购物车、订单等核心数据）；
    - 服务器：Tomcat（部署Web应用）；
    - 核心技术应用：
  - 请求处理：Servlet接收前端请求，处理业务逻辑后跳转至JSP页面展示数据；
  - 数据交互：JDBC建立数据库连接，实现数据的增删改查操作；
  - 状态管理：Session保存用户登录状态，Cookie实现记住密码、购物车临时存储；
  - 分页功能：通过SQL的LIMIT关键字实现书籍、订单的分页查询；
  - 数据校验：前端JS校验表单格式，后端Java代码二次校验数据合法性。
2. 进阶级架构（企业主流模式）
    - 前端技术：Vue.js、Element UI（或Ant Design），采用前后端分离开发模式；
    - 后端技术：Spring Boot、Spring MVC、MyBatis/MyBatis-Plus；
    - 数据库：MySQL（核心数据存储）、Redis（缓存热门书籍、用户购物车、登录令牌）；
    - 构建工具：Maven（依赖管理、项目构建）；
    - 服务器：Spring Boot内置Tomcat（简化部署）；
    - 核心技术应用：
  - 接口开发：Spring MVC提供RESTful API，供前端调用；
  - 数据访问：MyBatis/MyBatis-Plus简化SQL编写，配合PageHelper实现分页；
  - 安全认证：Spring Security实现用户登录认证、权限控制；
  - 前后端交互：Axios发送AJAX请求调用后端API，Vue Router实现前端路由跳转；
  - 部署方式：打包为Jar包直接运行，支持Docker容器化部署。

---------------------------------------------------------------------------------------------------------------------------------------
四、核心业务流程（以书籍购买为例）
1. 游客浏览阶段：未登录用户访问书籍列表页，可浏览书籍分类、书籍详情，支持按条件筛选和搜索书籍；
2. 登录校验阶段：用户点击"加入购物车"时，系统校验登录状态，未登录则跳转至登录页面；
3. 购物车操作阶段：登录用户选择书籍数量后加入购物车，购物车数据存入数据库/Redis，支持在购物车页面修改数量、删除商品；
4. 订单生成阶段：用户选择购物车内商品结算，填写收货地址、支付方式，生成订单并扣减书籍库存，订单状态初始化为待付款；
5. 支付阶段：用户完成支付（模拟支付或对接第三方支付接口），系统修改订单状态为已付款；
6. 订单履约阶段：后台管理员确认订单后修改状态为已发货，用户确认收货后订单状态更新为已完成；
7. 异常处理阶段：用户取消未付款订单，系统恢复对应书籍库存，订单状态改为已取消。

---------------------------------------------------------------------------------------------------------------------------------------
五、核心数据存储设计
1. 用户数据存储
    - 核心字段：用户ID（主键、自增）、用户名（唯一）、加密后的密码、邮箱、手机号、创建时间；
    - 存储规则：密码通过MD5或BCrypt加密后存储，避免明文泄露；用户名做唯一索引，防止重复注册。
2. 书籍数据存储
    - 核心字段：书籍ID（主键、自增）、书籍名称、作者、定价、库存数量、分类ID（关联分类表）、封面图片路径、书籍简介；
    - 存储规则：分类ID关联书籍分类表，封面图片路径存储服务器相对路径，书籍简介采用文本类型存储。
3. 购物车数据存储
    - 核心字段：购物车ID（主键、自增）、用户ID（关联用户表）、书籍ID（关联书籍表）、购买数量、创建时间；
    - 存储规则：用户ID+书籍ID做联合唯一索引，避免同一书籍重复加入购物车。
4. 订单数据存储
    - 核心字段：订单编号（主键）、用户ID（关联用户表）、订单总价、收货地址、订单状态、创建时间；
    - 存储规则：订单编号采用自定义规则生成（如时间戳+随机数），避免重复；订单状态用数字枚举（0-待付款、1-已付款等）。
5. 订单项数据存储
    - 核心字段：订单项ID（主键、自增）、订单编号（关联订单表）、书籍ID（关联书籍表）、购买数量、购买时单价；
    - 存储规则：记录购买时的书籍单价，避免后续书籍价格变动影响订单数据。

---------------------------------------------------------------------------------------------------------------------------------------
六、Bookstore应用的学习价值
1. 技术能力提升
    - 掌握JavaWeb核心技术的整合应用：理解Servlet、JSP、Filter等组件的协作方式，熟悉JDBC操作数据库的流程；
    - 理解MVC设计模式：通过Servlet（控制器）+ JavaBean（模型）+ JSP（视图）的模式，掌握分层开发思路；
    - 熟悉数据库设计：掌握电商场景下的表结构设计、外键关联、索引设计原则；
    - 了解前后端交互：掌握表单提交、AJAX请求、数据格式校验的完整流程；
    - 进阶学习：从传统模式过渡到前后端分离架构，理解RESTful API设计、缓存使用、权限控制等企业级技术点。
2. 业务能力提升
    - 理解电商核心流程：掌握用户登录、购物车、订单生成、状态流转等电商基础业务逻辑；
    - 培养问题解决思维：处理库存扣减、订单并发、数据一致性等实际业务问题；
    - 掌握项目开发流程：从需求分析、技术选型、代码开发到部署测试的完整流程。

---------------------------------------------------------------------------------------------------------------------------------------

### 总结
1. Bookstore应用是JavaWeb入门实战的经典项目，覆盖用户、书籍、购物车、订单等核心电商模块，技术架构分为传统JavaWeb模式和前后端分离的企业主流模式；
2. 核心业务围绕书籍购买流程展开，包含浏览、加购、下单、支付、履约等完整环节，数据存储涵盖用户、书籍、购物车、订单等核心维度；
3. 学习该项目可掌握JavaWeb核心技术的整合应用，理解电商基础业务逻辑，是从基础语法过渡到实战开发的关键项目。
