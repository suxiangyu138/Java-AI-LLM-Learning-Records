# Tomcat：Servlet容器（理论+实战）

## 一、Servlet容器核心定位（理论基础）

在Java Web技术体系中，Servlet容器的核心定位是"Servlet规范的实现者、Web组件的管理者、请求处理的调度者"，与Java后端开发的日常工作深度绑定：

1. **规范落地**：Servlet容器（Tomcat）严格遵循Java Servlet规范，将规范中定义的Servlet、Filter、Listener等接口的抽象逻辑落地为可执行的底层代码。
2. **组件管理**：Servlet容器负责Web组件的全生命周期管理——从Servlet的实例化、初始化，到请求触发时的执行，再到容器关闭时的销毁。
3. **请求调度**：承接Connector传递的HTTP请求，通过路径映射找到对应的Servlet，协调Filter、Listener的执行顺序。

## 二、Servlet容器核心理论：架构与组件（源码级拆解）

### 2.1 Servlet容器的分层架构

| 容器层级 | 核心职责 | 源码核心类 | 后端开发关联场景 |
|----------|----------|------------|-----------------|
| **Engine（引擎）** | 管理多个Host，负责请求的主机路由 | `StandardEngine` | 多虚拟主机部署 |
| **Host（虚拟主机）** | 管理多个Context，对应一个域名 | `StandardHost` | 配置localhost映射、生产多域名 |
| **Context（Web应用上下文）** | 管理多个Wrapper，负责应用加载、初始化 | `StandardContext` | 每个Spring Boot应用对应一个Context |
| **Wrapper（Servlet包装器）** | 对应一个Servlet，负责全生命周期管理 | `StandardWrapper` | Spring MVC的DispatcherServlet由Wrapper管理 |

### 2.2 Servlet生命周期管理

Servlet的生命周期分为4个阶段，全程由Servlet容器（Wrapper）自动调度：

#### 实例化（Instantiation）

当客户端第一次请求某个Servlet时，Wrapper容器通过反射机制实例化Servlet对象；若配置了`load-on-startup`，则在Context启动时自动实例化。

```java
// StandardWrapper源码（简化版）
public Servlet allocate() throws ServletException {
    if (instance == null) {
        synchronized (this) {
            if (instance == null) {
                instance = loadServlet();         // 反射实例化Servlet
                instance.init(servletConfig);     // 调用init方法
            }
        }
    }
    return instance;
}
```

#### 初始化（Initialization）

容器调用`init(ServletConfig config)`方法，完成Servlet的初始化。开发者可重写init方法实现自定义初始化逻辑（如初始化数据库连接、加载缓存）。init方法仅执行一次。

#### 请求处理（Service）

每当客户端发起请求，容器调用Servlet的`service()`方法。Servlet会根据请求方式（GET/POST），自动调用`doGet`、`doPost`方法。

#### 销毁（Destruction）

当Servlet容器关闭时，容器调用Servlet的`destroy()`方法，释放占用的资源。destroy方法仅执行一次。

### 2.3 关键配置：load-on-startup

**注解方式（Spring Boot常用）**：
```java
@WebServlet(urlPatterns = "/custom", loadOnStartup = 1)
public class CustomServlet extends HttpServlet {
    @Override
    public void init(ServletConfig config) throws ServletException {
        System.out.println("CustomServlet初始化完成");
    }
}
```

**XML配置方式**：
```xml
<servlet>
    <servlet-name>CustomServlet</servlet-name>
    <servlet-class>com.example.CustomServlet</servlet-class>
    <load-on-startup>1</load-on-startup>
</servlet>
```

> **注意**：load-on-startup的数值越小，初始化优先级越高；若取值为负数，则默认在首次请求时初始化。

### 2.4 Servlet容器的请求调度机制

1. **请求适配**：Connector通过CoyoteAdapter将HTTP请求转换为ServletRequest对象，传递给Engine容器
2. **路径映射与路由**：Engine → Host → Context → Wrapper层层路由，通过Mapper组件根据请求URL找到对应的Wrapper
3. **组件协同执行**：先执行Filter链，再调用Servlet的service方法，最后执行Listener相关逻辑

## 三、Servlet容器实战：配置、优化与问题排查

### 3.1 实战场景1：Spring Boot集成Tomcat Servlet容器

#### 默认配置

```yaml
server:
  port: 8081
  servlet:
    context-path: /demo
  tomcat:
    uri-encoding: UTF-8
    max-threads: 200
    min-spare-threads: 50
```

#### 自定义Servlet容器配置（代码方式）

```java
@Component
public class TomcatCustomizer implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        factory.setPort(8081);
        factory.setContextPath("/demo");
        factory.addAdditionalTomcatConnectors(createConnector());
        factory.setMaxThreads(200);
        factory.setMinSpareThreads(50);
    }

    private Connector createConnector() {
        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        connector.setPort(8082);
        return connector;
    }
}
```

### 3.2 实战场景2：自定义Servlet/Filter/Listener

#### 自定义Servlet（@WebServlet注解）

```java
@WebServlet(urlPatterns = "/custom/servlet", loadOnStartup = 1)
public class MyCustomServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        resp.getWriter().write("Custom Servlet Response");
    }
}

// 启动类添加@ServletComponentScan
@SpringBootApplication
@ServletComponentScan(basePackages = "com.example.servlet")
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

#### 自定义Filter（接口鉴权实战）

```java
@WebFilter(urlPatterns = "/*", filterName = "AuthFilter")
public class AuthFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String token = req.getHeader("token");
        if (token == null || token.isEmpty()) {
            ((HttpServletResponse) response).setStatus(401);
            response.getWriter().write("未授权，请登录");
            return;
        }
        chain.doFilter(request, response);
    }
}
```

#### 自定义Listener（资源初始化实战）

```java
@WebListener
public class MyServletContextListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext servletContext = sce.getServletContext();
        servletContext.setAttribute("globalConfig", "全局配置信息");
        System.out.println("Servlet容器初始化，全局资源加载完成");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("Servlet容器销毁，全局资源释放");
    }
}
```

### 3.3 实战场景3：Servlet容器性能优化

#### 线程池优化

```yaml
server:
  tomcat:
    max-threads: 200-500       # 根据CPU核心数调整
    min-spare-threads: 50-100  # 最小空闲线程数
    max-connections: 10000     # 最大连接数
    connection-timeout: 30000  # 连接超时时间（30秒）
```

#### 连接配置优化

```yaml
server:
  tomcat:
    keep-alive-timeout: 60000       # 长连接超时时间（60秒）
    keep-alive-max-requests: 100    # 单个长连接最大请求数
    protocol: org.apache.coyote.http11.Http11Nio2Protocol  # 启用NIO2模式
```

#### 资源优化

```yaml
server:
  servlet:
    multipart:
      max-request-size: 10MB   # 单个请求最大大小
      max-file-size: 5MB       # 单个文件上传最大大小
```

### 3.4 实战场景4：Servlet容器常见问题排查

| 问题 | 常见原因 | 解决方案 |
|------|----------|----------|
| **Servlet初始化失败** | 依赖资源未加载、load-on-startup配置冲突、类未被正确扫描 | 检查init方法的资源加载逻辑，调整load-on-startup优先级，检查@ServletComponentScan |
| **请求404** | Servlet路径映射错误、Context上下文路径错误、Servlet未被容器初始化 | 检查urlPatterns、context-path配置，确认Servlet已初始化 |
| **内存泄漏** | Servlet中创建的线程未关闭、Listener初始化的资源未释放、第三方依赖未正确卸载 | 在destroy/contextDestroyed方法中释放资源 |

## 四、总结（Java后端视角）

Tomcat的Servlet容器是Java后端开发的"基础底座"——它不仅是Servlet规范的实现者，更是我们编写的Controller、Filter、Listener运行的核心环境。

**核心要点**：
- 理论层面：掌握Servlet容器的分层架构、Servlet生命周期、请求调度机制
- 实战层面：熟练掌握Spring Boot集成Servlet容器的配置、自定义Web组件的方法、性能优化、问题排查的技巧

> 深入理解Servlet容器，不仅能解决日常开发中的部署、调试问题，更能帮助优化系统性能、避免常见坑点（如内存泄漏、路径映射异常），为后续学习微服务部署、容器化（Docker）等高级场景打下坚实基础。
