# Tomcat：一个简单的Servlet服务器

## 一、前言：Tomcat在Java后端生态中的核心定位

对于Java后端开发者而言，Tomcat是绕不开的核心组件——它不仅是最常用的Servlet容器，更是连接Java Web应用与底层操作系统的"桥梁"。不同于Spring Boot内嵌的简化版Tomcat，独立部署的Tomcat完整实现了Java Servlet、JavaServer Pages（JSP）等规范，是中小型Java Web项目的首选服务器，也是理解Java Web底层运行机制的关键载体。

## 二、核心理论：Tomcat的架构设计与核心组件

Tomcat的本质是"Servlet容器 + HTTP服务器"，其设计遵循"分层架构 + 组件化思想"，核心目标是高效处理HTTP请求，并将请求转发给对应的Servlet进行业务逻辑处理。

### 2.1 Tomcat整体架构（分层视角）

| 层级 | 说明 |
|------|------|
| **基础层（Server）** | Tomcat的顶层组件，代表整个Tomcat服务器，负责管理所有Service的生命周期 |
| **服务层（Service）** | 包含Connector（连接器）和Engine（引擎），是Tomcat处理请求的核心单元 |
| **容器层（Engine/Host/Context/Wrapper）** | 负责管理Servlet的生命周期，采用"父子容器"结构 |
| **应用层（Servlet/JSP）** | 后端开发者编写的业务代码层 |

**容器层详解**：
- **Engine**：引擎，一个Service只有一个Engine，负责将请求分配给对应的Host
- **Host**：虚拟主机，对应一个域名（如localhost），管理该域名下的所有Web应用
- **Context**：Web应用上下文，对应一个独立的Java Web项目（如war包部署的应用）
- **Wrapper**：Servlet包装器，每个Wrapper对应一个Servlet实例，管理生命周期

### 2.2 核心组件详解

#### Connector（连接器）：请求的"接收者"

Connector的核心职责是监听指定端口（默认8080），接收客户端HTTP请求，解析为Tomcat内部的Request对象，再转发给Engine处理。

**后端关注的两个核心配置**：
- **协议（protocol）**：默认HTTP/1.1，也可配置为NIO2（非阻塞IO，适合高并发场景）
- **线程池（Executor）**：Connector通过线程池处理请求，可优化线程池参数解决高并发线程耗尽问题

#### Engine（引擎）：请求的"分配者"

Engine是容器层的顶层组件，核心职责是根据请求的"Host头部"（如localhost、www.example.com），将请求分配给对应的Host。

#### Context（上下文）：Web应用的"容器"

Context对应一个Java Web应用，核心职责包括：
- 加载Web应用的配置文件（web.xml）
- 管理Web应用的资源（静态资源、JSP文件）
- 管理Servlet的生命周期

#### Wrapper（包装器）：Servlet的"管家"

Wrapper是Tomcat中最底层的容器组件，每个Wrapper对应一个Servlet实例，负责管理Servlet的完整生命周期：
- **初始化（init）**：Tomcat启动时或第一次请求时调用Servlet的init方法
- **执行（service）**：每次请求到来时调用Servlet的service方法
- **销毁（destroy）**：Tomcat停止时调用Servlet的destroy方法

> **注意**：Servlet是单实例多线程的，即一个Servlet类只有一个实例，Tomcat通过线程池为每个请求分配一个线程，因此编写Servlet时需避免使用全局变量（线程安全问题）。

### 2.3 Tomcat与Servlet规范的关系

Servlet是Java EE的规范（接口），定义了Servlet的生命周期、请求处理方式等标准；而Tomcat是Servlet规范的"实现类"，提供了Servlet运行所需的容器环境。

## 三、底层原理：Tomcat处理HTTP请求的完整链路

### 3.1 请求处理完整流程（分步解析）

1. **客户端发送HTTP请求**：客户端通过HTTP协议向Tomcat的Connector监听的端口发送请求
2. **Connector接收并解析请求**：Connector的线程池分配线程，接收请求数据，解析为Request和Response对象
3. **Connector转发请求到Engine**：将解析后的对象转发给当前Service的Engine
4. **Engine分配请求到Host**：根据Request中的"Host头部"找到对应的Host
5. **Host分配请求到Context**：根据"请求路径"找到对应的Context（Web应用上下文）
6. **Context匹配Servlet**：根据请求路径匹配web.xml中的Servlet映射，找到对应的Wrapper
7. **Wrapper调用Servlet处理请求**：检查Servlet实例是否已初始化，调用service方法
8. **返回响应**：响应数据通过Response对象封装，依次向上传递，最终通过Connector返回给客户端

### 3.2 核心原理关键点

- **请求路由逻辑**：Engine → Host → Context → Wrapper的层层匹配
- **Servlet生命周期与线程安全**：单实例多线程，避免使用全局变量
- **请求与响应的封装**：Tomcat的Request和Response对象分别实现了ServletRequest和ServletResponse接口

## 四、实战落地：从手写简易Servlet服务器到Tomcat实战

### 4.1 实战1：手写简易Servlet服务器（理解核心思想）

#### 定义Servlet规范

```java
public interface MyServlet {
    void init();
    void service(MyRequest request, MyResponse response);
    void destroy();
}
```

#### 实现请求与响应封装

```java
public class MyRequest {
    private String requestPath;
    private String method;

    public MyRequest(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String requestLine = reader.readLine();
        if (requestLine != null && !requestLine.isEmpty()) {
            String[] parts = requestLine.split(" ");
            this.method = parts[0];
            this.requestPath = parts[1];
        }
    }
    // getter方法...
}

public class MyResponse {
    private OutputStream outputStream;

    public MyResponse(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void write(String content) throws IOException {
        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html;charset=UTF-8\r\n" +
                "Content-Length: " + content.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
                "\r\n" + content;
        outputStream.write(response.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }
}
```

#### 实现Servlet容器

```java
public class MyServletContainer {
    private Map<String, MyServlet> servletMap = new HashMap<>();

    public void registerServlet(String path, Class<? extends MyServlet> servletClass) 
            throws InstantiationException, IllegalAccessException {
        MyServlet servlet = servletClass.newInstance();
        servlet.init();
        servletMap.put(path, servlet);
    }

    public void handleRequest(InputStream inputStream, OutputStream outputStream) 
            throws IOException, InstantiationException, IllegalAccessException {
        MyRequest request = new MyRequest(inputStream);
        MyResponse response = new MyResponse(outputStream);
        String requestPath = request.getRequestPath();
        MyServlet servlet = servletMap.get(requestPath);
        if (servlet != null) {
            servlet.service(request, response);
        } else {
            response.write("404 Not Found");
        }
    }
}
```

#### 启动服务器

```java
public class MyTomcat {
    private int port;
    private MyServletContainer container = new MyServletContainer();

    public MyTomcat(int port) {
        this.port = port;
    }

    public void start() throws IOException, InstantiationException, IllegalAccessException {
        container.registerServlet("/demo", DemoServlet.class);
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("简易Servlet服务器启动，监听端口：" + port);
        while (true) {
            Socket socket = serverSocket.accept();
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();
            container.handleRequest(inputStream, outputStream);
            inputStream.close();
            outputStream.close();
            socket.close();
        }
    }

    public static void main(String[] args) throws Exception {
        new MyTomcat(8080).start();
    }
}
```

#### 自定义Servlet

```java
public class DemoServlet implements MyServlet {
    @Override
    public void init() {
        System.out.println("DemoServlet初始化...");
    }

    @Override
    public void service(MyRequest request, MyResponse response) throws IOException {
        String content = "<h1>Hello, 简易Servlet服务器！</h1>" +
                "<p>请求路径：" + request.getRequestPath() + "</p>" +
                "<p>请求方法：" + request.getMethod() + "</p>";
        response.write(content);
    }

    @Override
    public void destroy() {
        System.out.println("DemoServlet销毁...");
    }
}
```

### 4.2 实战2：Tomcat日常部署与配置优化

#### Tomcat部署Web应用（3种方式）

**方式1：自动部署（最常用）**：将war包放到Tomcat的webapps目录下，Tomcat启动时自动解压并部署。

**方式2：配置Context标签（自定义路径）**：修改`conf/server.xml`，在Host标签内添加Context标签：
```xml
<Context path="/myapp" docBase="D:/java/webapp/demo" reloadable="true"/>
```

**方式3：独立部署（推荐生产环境）**：在`conf/Catalina/localhost`目录下创建XML文件（如myapp.xml）：
```xml
<Context docBase="D:/java/webapp/demo" reloadable="false"/>
```

#### 核心配置优化

**线程池优化**：
```xml
<Executor name="tomcatThreadPool" 
          namePrefix="catalina-exec-" 
          maxThreads="200"
          minSpareThreads="20"
          maxIdleTime="60000"
          queueCapacity="100"
          prestartminSpareThreads="true"/>

<Connector executor="tomcatThreadPool"
           port="8080"
           protocol="org.apache.coyote.http11.Http11Nio2Protocol"
           connectionTimeout="20000"
           redirectPort="8443"
           maxConnections="10000"
           acceptCount="1000"
           enableLookups="false"
           URIEncoding="UTF-8"/>
```

### 4.3 实战3：Tomcat常见问题排查

#### 问题1：端口被占用（启动失败）

- Windows：`netstat -ano | findstr 8080`，找到进程ID，在任务管理器中结束
- Linux：`netstat -tulpn | grep 8080`，执行`kill -9 进程ID`
- 或修改Tomcat的server.xml中的端口

#### 问题2：404请求不存在（请求路由失败）

排查优先级：
1. 检查访问路径是否包含Context的path和Servlet的映射路径
2. 检查Web应用是否部署成功（查看webapps目录）
3. 检查web.xml配置是否正确

#### 问题3：500服务器内部错误（Servlet业务逻辑异常）

- 查看Tomcat的logs目录，catalina.out日志记录了完整的异常堆栈
- 根据异常堆栈定位到对应的Servlet方法，排查代码错误
- 检查WEB-INF/lib目录下的依赖包是否完整

## 五、总结

对于Java后端开发者而言，Tomcat不仅是一个"服务器工具"，更是理解Java Web底层运行机制的"钥匙"。

**核心要点**：
- Tomcat是"Servlet容器 + HTTP服务器"，实现了Java Servlet规范
- 请求处理链路：Connector接收请求 → Engine → Host → Context → Wrapper → Servlet
- 实战重点：掌握Web应用部署方式、核心配置优化（线程池、Connector），能快速排查常见问题

> 深入理解Tomcat，不仅能提升后端开发的效率（快速排查问题、优化性能），更能帮助开发者理解Spring Boot内嵌Tomcat的原理，为后续学习分布式架构、微服务打下基础。
