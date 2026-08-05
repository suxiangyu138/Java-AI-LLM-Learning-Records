# JavaWeb从入门到精通 面试宝典
> 基于JavaWeb(2026)课程大纲，全面覆盖Servlet/JSP/HTTP/会话管理/三层架构等JavaWeb面试高频考点，助力备战大厂面试

## 目录
1. [一、基础概念速答（20题）](#一基础概念速答20题)
2. [二、深度原理剖析（15题）](#二深度原理剖析15题)
3. [三、实战场景题（12题）](#三实战场景题12题)
4. [四、手写代码题（8题）](#四手写代码题8题)
5. [五、系统设计题（5题）](#五系统设计题5题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板（Top 5）](#七面试回答模板top-5)
8. [八、快速查漏补缺Checklist](#八快速查漏补缺checklist)

---

## 一、基础概念速答（20题）

### 1.1 BS架构与CS架构的优缺点
| 对比维度 | BS（Browser/Server） | CS（Client/Server） |
|----------|---------------------|---------------------|
| 部署维护 | 零部署，浏览器即可，维护成本低 | 需安装客户端，升级成本高 |
| 跨平台性 | 浏览器兼容即可，跨平台强 | 需为不同平台开发不同客户端 |
| 用户体验 | 受限于浏览器能力，体验一般 | 可充分利用本地资源，体验好 |
| 安全性 | 数据在服务器，相对安全 | 客户端存储数据，易被逆向 |
| 网络依赖 | 必须在线，不可离线使用 | 部分功能可离线运行 |
| 开发成本 | 开发周期短，成本低 | 开发周期长，成本高 |

> 💡 现代趋势：大型应用多采用BS为主、CS为辅的混合架构，或使用PWA弥补BS离线能力的不足。

### 1.2 URL、URI、URN的区别
- **URI（Uniform Resource Identifier）**：统一资源标识符，最宽泛的概念，用于标识某个资源
- **URL（Uniform Resource Locator）**：统一资源定位符，包含访问协议和位置信息，是URI的子集
- **URN（Uniform Resource Name）**：统一资源名称，仅标识资源名称不包含位置，是URI的子集

```
URI 分为 URL 和 URN
URL = 协议 + 主机 + 端口 + 路径  例：https://example.com:8080/app
URN = 命名空间 + 名称            例：urn:isbn:0-486-27557-4
```

### 1.3 HTTP请求协议结构
```
请求行：   GET /app/demo HTTP/1.1
请求头：   Host: localhost:8080
           User-Agent: Mozilla/5.0
           Accept: text/html
           Cookie: JSESSIONID=ABC123
空格行：   （空行）
请求体：   username=admin&password=123  （仅POST请求有）
```

### 1.4 HTTP响应协议结构
```
状态行：   HTTP/1.1 200 OK
响应头：   Content-Type: text/html; charset=UTF-8
           Set-Cookie: JSESSIONID=XYZ789
           Content-Length: 1024
空格行：   （空行）
响应体：   <html><body>...</body></html>
```

### 1.5 常见HTTP状态码
| 状态码 | 含义 | 说明 |
|--------|------|------|
| 200 | OK | 请求成功 |
| 302 | Found | 重定向，配合Location头使用 |
| 304 | Not Modified | 缓存有效，使用本地缓存 |
| 400 | Bad Request | 请求参数有误 |
| 401 | Unauthorized | 未认证，需要登录 |
| 403 | Forbidden | 已认证但无权限 |
| 404 | Not Found | 资源不存在 |
| 405 | Method Not Allowed | 请求方法不支持 |
| 500 | Internal Server Error | 服务器内部异常 |
| 502 | Bad Gateway | 网关错误 |
| 503 | Service Unavailable | 服务不可用（过载/维护） |

> 🎯 **面试重点**：301 vs 302（永久重定向 vs 临时重定向）；401 vs 403（未认证 vs 无权限）

### 1.6 GET与POST请求的区别
| 对比维度 | GET | POST |
|----------|-----|------|
| 数据位置 | 放在URL查询参数中 | 放在请求体中 |
| 数据长度 | URL长度限制（约2KB-8KB） | 理论上无限制 |
| 安全性 | 数据暴露在URL，不安全 | 相对安全，不在URL中 |
| 缓存 | 浏览器会缓存GET请求 | 不会缓存 |
| 幂等性 | 幂等（多次请求结果相同） | 非幂等 |
| 书签收藏 | 可以收藏带参数的URL | 无法收藏 |
| 编码类型 | application/x-www-form-urlencoded | multipart/form-data 或 application/x-www-form-urlencoded |
| 后退/刷新 | 无害 | 会提示重新提交表单 |

> 💡 选择原则：查询操作用GET，增删改用POST。RESTful规范中PUT（幂等）和DELETE也可用于增删。

### 1.7 URL编码（Percent-Encoding）
```java
// URL编码：将中文字符和特殊字符转换为 % 后跟两位十六进制
// 例如："中文" -> "%E4%B8%AD%E6%96%87"

// Java中编码和解码
import java.net.URLEncoder;
import java.net.URLDecoder;

String encoded = URLEncoder.encode("中文参数", "UTF-8");
// 输出: %E4%B8%AD%E6%96%87%E5%8F%82%E6%95%B0

String decoded = URLDecoder.decode(encoded, "UTF-8");
// 输出: 中文参数
```

> ⚠️ Tomcat 8.0+ 默认使用 UTF-8 解码URL，8.0以下使用 ISO-8859-1，需手动配置 URIEncoding="UTF-8"。

### 1.8 转发（Forward）与重定向（Redirect）的区别
| 对比维度 | 转发 Forward | 重定向 Redirect |
|----------|-------------|-----------------|
| 浏览器URL | 不变（服务器内部跳转） | 改变（浏览器发起新请求） |
| 请求次数 | 1次请求（服务器内部转发） | 2次请求（浏览器重新请求） |
| 数据共享 | 共享同一个request域对象 | 不共享request，只能用session |
| 目标限制 | 只能转发到本站点内部资源 | 可以重定向到任何外部URL |
| 代码实现 | `req.getRequestDispatcher().forward(req, resp)` | `resp.sendRedirect(url)` |
| 执行位置 | 服务器端执行 | 浏览器端执行 |

```java
// 转发：在服务器内部完成，浏览器无感知
request.getRequestDispatcher("/WEB-INF/views/success.jsp")
       .forward(request, response);

// 重定向：告诉浏览器重新请求新地址
response.sendRedirect(request.getContextPath() + "/login");
```

> 🎯 选择原则：需要携带数据到下一页用转发；需要避免重复提交用重定向（POST-Redirect-GET模式）。

### 1.9 Servlet生命周期
Servlet生命周期由Servlet容器（Tomcat）管理，核心方法如下：

| 阶段 | 方法 | 调用次数 | 说明 |
|------|------|---------|------|
| 加载实例化 | 构造器 | 1次 | Tomcat通过反射创建实例 |
| 初始化 | `init(ServletConfig config)` | 1次 | 执行资源初始化，可设置load-on-startup |
| 处理请求 | `service(ServletRequest req, ServletResponse resp)` | N次 | 每次请求都调用 |
| 销毁 | `destroy()` | 1次 | 容器关闭或应用卸载时调用 |

```java
@WebServlet(urlPatterns = "/demo", loadOnStartup = 1) // 服务器启动时实例化
public class DemoServlet extends HttpServlet {
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        // 数据库连接池、全局配置等初始化
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        resp.setContentType("text/html;charset=UTF-8");
        resp.getWriter().write("<h1>Hello Servlet</h1>");
    }
    
    @Override
    public void destroy() {
        // 释放资源：关闭数据库连接等
    }
}
```

> 💡 `loadOnStartup` 为正数时在容器启动阶段实例化，为负数或未设置时在首次请求时实例化。

### 1.10 GenericServlet与HttpServlet的关系
```
Servlet（接口）
    |
    |--- GenericServlet（抽象类，实现Servlet和ServletConfig接口）
            |
            |--- HttpServlet（抽象类，根据HTTP方法分发）
```

- **GenericServlet**：通用的、与协议无关的Servlet抽象，实现了ServletConfig接口，提供了`getInitParameter()`等便捷方法，应用了**适配器模式**和**缺省适配器模式**
- **HttpServlet**：基于HTTP协议的Servlet，覆写了`service()`方法，根据请求方法类型（GET/POST/PUT/DELETE等）自动分发给对应的`doXxx()`方法，应用了**模板方法模式**

### 1.11 ServletConfig与ServletContext的区别
| 对比维度 | ServletConfig | ServletContext |
|----------|--------------|---------------|
| 作用范围 | 单个Servlet | 整个Web应用（所有Servlet共享） |
| 获取方式 | `getServletConfig()` | `getServletContext()` |
| 配置方式 | `<init-param>` | `<context-param>` |
| 生命周期 | 随Servlet创建而创建 | 随Web应用启动而创建，关闭而销毁 |
| 典型用途 | 当前Servlet的个性化配置 | 全局共享配置，如数据库连接信息 |

```xml
<!-- web.xml 配置示例 -->
<context-param>
    <param-name>globalEncoding</param-name>
    <param-value>UTF-8</param-value>
</context-param>

<servlet>
    <servlet-name>myServlet</servlet-name>
    <servlet-class>com.example.MyServlet</servlet-class>
    <init-param>
        <param-name>pageSize</param-name>
        <param-value>20</param-value>
    </init-param>
</servlet>
```

### 1.12 JavaWeb三大域对象
| 域对象 | 类型 | 作用范围 | 生命周期 | 典型用途 |
|--------|------|---------|---------|---------|
| HttpServletRequest | 请求域 | 一次请求（含转发） | 请求开始到结束 | 请求参数、表单数据 |
| HttpSession | 会话域 | 一次会话（多次请求） | 会话建立到超时/销毁 | 用户登录状态、购物车 |
| ServletContext | 应用域 | 整个Web应用 | 应用启动到关闭 | 全局配置、计数器 |

```java
// 设置域属性
request.setAttribute("key", "value");      // 请求域
request.getSession().setAttribute("user", user); // 会话域
getServletContext().setAttribute("count", 100);  // 应用域

// 获取域属性
request.getAttribute("key");
request.getSession().getAttribute("user");
getServletContext().getAttribute("count");
```

### 1.13 Filter过滤器生命周期
Filter生命周期与Servlet类似，由容器管理：

| 阶段 | 方法 | 说明 |
|------|------|------|
| 初始化 | `init(FilterConfig config)` | 应用启动时调用1次 |
| 过滤 | `doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)` | 每次请求匹配时调用 |
| 销毁 | `destroy()` | 应用关闭时调用1次 |

```java
@WebFilter(urlPatterns = "/*")
public class EncodingFilter implements Filter {
    
    @Override
    public void init(FilterConfig config) {
        // 读取配置参数
    }
    
    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("text/html;charset=UTF-8");
        chain.doFilter(req, resp); // 放行，调用下一个Filter或目标Servlet
    }
    
    @Override
    public void destroy() {
        // 清理资源
    }
}
```

### 1.14 常见的Listener监听器
| 监听器接口 | 监听对象 | 事件类型 |
|-----------|---------|---------|
| `ServletContextListener` | 应用上下文 | 初始化和销毁 |
| `ServletContextAttributeListener` | 应用域属性 | 增删改属性 |
| `HttpSessionListener` | 会话 | 创建和销毁 |
| `HttpSessionAttributeListener` | 会话域属性 | 增删改属性 |
| `ServletRequestListener` | 请求 | 创建和销毁 |
| `ServletRequestAttributeListener` | 请求域属性 | 增删改属性 |
| `HttpSessionBindingListener` | 对象绑定到会话 | 绑定和解绑 |

```java
@WebListener
public class AppInitListener implements ServletContextListener {
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();
        // 应用启动时加载全局配置
        ctx.setAttribute("appName", "MyApp");
        System.out.println("Web应用已启动");
    }
    
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("Web应用已关闭");
    }
}
```

### 1.15 Cookie与Session的区别
| 对比维度 | Cookie | Session |
|----------|--------|---------|
| 存储位置 | 浏览器（客户端） | 服务器（内存/文件/数据库） |
| 存储容量 | 单个Cookie最大4KB | 理论上无限制 |
| 存储类型 | 只能存储字符串 | 可以存储任意对象 |
| 生命周期 | 可设置过期时间，持久化 | 默认30分钟不活动自动销毁 |
| 安全性 | 明文传输，可被篡改 | 存储在服务端，相对安全 |
| 网络传输 | 每次请求自动带在请求头中 | 通过Cookie中的JSESSIONID关联 |
| 禁用后 | 可用URL重写技术（`response.encodeURL()`） | 无法正常使用（需URL重写） |

### 1.16 Token认证机制
```
客户端                 服务器
  |                     |
  |-- 登录请求 -------->|
  |                     |-- 验证用户名密码
  |                     |-- 生成Token（Base64编码存储）
  |<-- 返回Token -------|
  |                     |
  |-- 请求 + Token ---->|
  |                     |-- 验证Token有效性
  |<-- 返回数据 --------|
```

Token核心机制：
- **Series机制**：系列号，用于辨识是否同一条登录记录（防篡改）
- **Fingerprint机制**：指纹，基于用户代理、IP等信息加密生成（防伪造）
- **Base64编码**：将二进制Token数据编码为字符串，便于在HTTP头中传输

### 1.17 Base64编码
```java
import java.util.Base64;

// 编码
String original = "admin:123456";
String encoded = Base64.getEncoder().encodeToString(original.getBytes("UTF-8"));
// 输出: YWRtaW46MTIzNDU2

// 解码
byte[] decoded = Base64.getDecoder().decode(encoded);
String originalStr = new String(decoded, "UTF-8");
// 输出: admin:123456
```

> ⚠️ Base64是编码（Encoding）而非加密（Encryption），可被逆向解码还原原文。敏感信息不能仅靠Base64保护，需配合哈希或加密算法。

### 1.18 SLF4J + Logback日志配置
```xml
<!-- logback.xml 配置示例 -->
<configuration>
    <!-- 控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- 文件输出 -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/app.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/app.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%date %level [%thread] %logger{10} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

日志级别（从低到高）：`TRACE < DEBUG < INFO < WARN < ERROR`

### 1.19 三层架构与MVC的区别与联系
| 对比维度 | 三层架构（3-Tier Architecture） | MVC模式 |
|----------|-------------------------------|---------|
| 分层 | Controller / Service / DAO | Model / View / Controller |
| 关注点 | 职责分层（控制层/业务层/数据层） | 组件交互（数据模型/展示/控制） |
| 粒度 | 架构级别（大粒度） | 设计模式级别（小粒度） |
| 关系 | 三层架构中的Controller层内部可使用MVC | MVC的三部分通常位于三层架构的Controller层 |

```
三层架构：
┌──────────────┐
│ Controller层  │ ← 接收请求、响应、参数校验 ← 这一层内部可使用MVC
├──────────────┤
│ Service层    │ ← 核心业务逻辑、事务管理
├──────────────┤
│ DAO层        │ ← 数据访问、数据库CRUD
└──────────────┘
```

### 1.20 DTO、VO、Entity、POJO的区别
| 概念 | 全称 | 用途 | 特点 |
|------|------|------|------|
| POJO | Plain Old Java Object | 普通Java对象 | 最简单的JavaBean，无约束 |
| Entity | Entity | 数据库实体 | 与数据库表字段一一对应 |
| DTO | Data Transfer Object | 数据传输对象 | 跨层数据传输，包含所需字段 |
| VO | View Object | 视图对象 | 展示层封装，用于前端展示 |

```java
// Entity：与数据库表对应
public class AccountEntity {
    private Long id;
    private String accountNo;
    private BigDecimal balance;
    // getter/setter
}

// DTO：跨层传输（如前端传入的参数）
public class TransferDTO {
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    // getter/setter
}

// VO：返回给前端的展示数据
public class TransferVO {
    private String transactionNo;
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private String status;
    private LocalDateTime time;
    // getter/setter
}
```

---

## 二、深度原理剖析（15题）

### 2.1 Tomcat服务器架构
```
Tomcat Server
 └── Service
      ├── Connector（连接器）   ← 监听端口，解析HTTP请求为ServletRequest
      │    ├── HTTP/1.1 Connector（8080端口）
      │    └── AJP Connector（8009端口）
      └── Engine（引擎）
           └── Host（虚拟主机）
                └── Context（Web应用上下文）
                     └── Wrapper（Servlet包装器）
```

核心组件：
- **Connector**：接收网络请求，解析HTTP协议，将Socket转换为ServletRequest
- **Container**：包含Engine、Host、Context、Wrapper四级容器
- **Thread Pool**：每个请求分配一个工作线程，线程池默认大小200

### 2.2 Servlet线程安全问题
```java
@WebServlet("/unsafe")
public class UnsafeServlet extends HttpServlet {
    // ❌ 危险：成员变量存在线程安全问题
    private int count = 0;
    private String userName;
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        // 多个线程同时访问同一个Servlet实例，count++不是原子操作
        count++;
        userName = req.getParameter("name");
        
        // 这里可能出现线程A的name被线程B覆盖
        resp.getWriter().write("用户: " + userName + ", 计数: " + count);
    }
}
```

**解决方案**：
1. **避免使用成员变量**：使用局部变量（每个线程独享栈空间）
2. **使用ThreadLocal**：将非线程安全的对象绑定到当前线程
3. **加锁同步**：使用 `synchronized`（但会降低并发性能）

```java
// ✅ 推荐：使用局部变量或ThreadLocal
@WebServlet("/safe")
public class SafeServlet extends HttpServlet {
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = 
        ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        // 局部变量——线程安全
        String name = req.getParameter("name");
        String date = DATE_FORMAT.get().format(new Date());
        resp.getWriter().write("用户: " + name + ", 日期: " + date);
    }
}
```

### 2.3 ThreadLocal底层原理
```java
public class ThreadLocal<T> {
    // 每个Thread对象内部维护一个ThreadLocalMap
    // ThreadLocalMap的key是ThreadLocal实例（弱引用），value是存储的值
    
    public T get() {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t); // 获取当前线程的ThreadLocalMap
        if (map != null) {
            Entry e = map.getEntry(this);
            if (e != null) return (T) e.value;
        }
        return setInitialValue();
    }
    
    public void set(T value) {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            map.set(this, value);
        else
            createMap(t, value);
    }
}
```

```
线程1 ──→ ThreadLocalMap ──→ {key=ThreadLocal实例(弱引用), value=值1}
线程2 ──→ ThreadLocalMap ──→ {key=ThreadLocal实例(弱引用), value=值2}
```

> ⚠️ ThreadLocal内存泄漏风险：ThreadLocalMap的key是弱引用（GC时被回收），但value是强引用。如果ThreadLocal实例被回收而线程还在运行（如Tomcat线程池中的线程），value永远无法被访问到但也不会被GC回收。**解决方案**：每次使用后调用 `remove()` 方法。

### 2.4 Filter与经典责任链模式的区别
| 对比维度 | 经典责任链模式 | Filter Chain |
|----------|---------------|-------------|
| 链式结构 | 每个Handler持有下一个Handler引用 | FilterChain统一管理Filter数组 |
| 调用方式 | Handler处理完主动调用下一个 | FilterChain遍历数组依次执行 |
| 控制反转 | 责任节点控制流转 | FilterChain控制流转 |
| 终止条件 | 节点可以终止传递 | 必须调用chain.doFilter()才能继续 |
| 调用链状态 | 节点间传递请求对象 | 共享同一个ServletRequest |

```java
// 经典责任链模式
abstract class Handler {
    protected Handler next;
    public void setNext(Handler next) { this.next = next; }
    public abstract void handle(Request req);
}

class AuthHandler extends Handler {
    public void handle(Request req) {
        // 处理认证
        if (next != null) next.handle(req);
    }
}

// Filter Chain（Tomcat实现）
public final class ApplicationFilterChain implements FilterChain {
    private Filter[] filters = new Filter[0]; // 数组存储
    private int pos = 0;                       // 游标遍历
    
    @Override
    public void doFilter(ServletRequest req, ServletResponse resp) {
        if (pos < filters.length) {
            Filter filter = filters[pos++];
            filter.doFilter(req, resp, this); // 传入this，由Filter回调
        } else {
            servlet.service(req, resp); // 所有Filter执行完毕，调用目标Servlet
        }
    }
}
```

### 2.5 HttpServlet执行原理（模板方法模式）
```java
public abstract class HttpServlet extends GenericServlet {
    
    // 模板方法：由service方法统一调度
    @Override
    public void service(ServletRequest req, ServletResponse resp) 
            throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;
        service(request, response); // 调用重载的service方法
    }
    
    // 模板方法：根据HTTP方法分发
    protected void service(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        String method = req.getMethod();
        
        if (method.equals("GET")) {
            doGet(req, resp);
        } else if (method.equals("POST")) {
            doPost(req, resp);
        } else if (method.equals("PUT")) {
            doPut(req, resp);
        } else if (method.equals("DELETE")) {
            doDelete(req, resp);
        } // ... 其他方法
    }
    
    // 默认实现返回405，子类按需覆写
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
}
```

> 🎯 模板方法模式定义算法骨架（service方法），具体步骤（doGet/doPost等）由子类实现。

### 2.6 适配器模式在Servlet中的应用
```java
// Servlet接口（目标接口）
public interface Servlet {
    void init(ServletConfig config);
    void service(ServletRequest req, ServletResponse res);
    void destroy();
    ServletConfig getServletConfig();
    String getServletInfo();
}

// GenericServlet：适配器，为所有方法提供默认空实现
public abstract class GenericServlet implements Servlet, ServletConfig {
    private ServletConfig config;
    
    // 实现ServletConfig接口的方法，委托给config对象
    public String getInitParameter(String name) {
        return config.getInitParameter(name);
    }
    
    // 开发者只需覆写自己需要的方法
    public void init() { } // 供子类覆写的便捷方法
    
    // 将ServletConfig的初始化逻辑封装好
    public void init(ServletConfig config) {
        this.config = config;
        this.init(); // 调用无参init()，子类无需处理ServletConfig
    }
}
```

> 💡 **缺省适配器模式**：提供接口所有方法的默认空实现（如上例），子类无需实现所有方法，只需选择自己需要的进行覆写。

### 2.7 Session机制深入理解
```java
// Session创建与获取流程
HttpSession session = request.getSession(); // 内部流程如下：

/*
1. 从请求中获取JSESSIONID（从Cookie或URL参数）
2. 如果有JSESSIONID，根据ID查找对应Session对象
   - 找到：直接返回
   - 未找到（过期或伪造）：创建新的Session
3. 如果没有JSESSIONID，创建新Session
4. 在响应中设置Set-Cookie: JSESSIONID=新ID
*/
```

**Cookie禁用时的解决方案**——URL重写：
```java
// 当浏览器禁用了Cookie时，Session无法通过Cookie传递JSESSIONID
// 解决方案：将JSESSIONID追加到URL中
String encodedURL = response.encodeURL("/app/secure/page");
// 如果Cookie可用，返回原URL；如果禁用，返回 /app/secure/page;jsessionid=ABC123

// 重定向时也要编码
String redirectURL = response.encodeRedirectURL("/app/dashboard");
response.sendRedirect(redirectURL);
```

> 🎯 一次完整的会话：从用户打开浏览器访问网站到关闭浏览器（或Session超时）期间的所有请求。

### 2.8 事务管理原理
```java
// JavaWeb中的事务管理（手动控制）
public class TransferService {
    private AccountDao accountDao = new AccountDao();
    
    public void transfer(String from, String to, BigDecimal amount) {
        // 获取数据库连接（应使用ThreadLocal确保同一线程使用同一连接）
        Connection conn = TransactionManager.getConnection();
        try {
            conn.setAutoCommit(false); // 关闭自动提交，开启事务
            
            accountDao.updateBalance(from, amount.negate()); // 扣款
            accountDao.updateBalance(to, amount);            // 加款
            
            conn.commit(); // 提交事务
        } catch (Exception e) {
            conn.rollback(); // 回滚事务
            throw new RuntimeException("转账失败", e);
        } finally {
            conn.setAutoCommit(true); // 恢复自动提交
            conn.close();
        }
    }
}

// ThreadLocal管理Connection，保证事务中的DAO使用同一连接
public class TransactionManager {
    private static final ThreadLocal<Connection> CONN_HOLDER = new ThreadLocal<>();
    
    public static Connection getConnection() {
        Connection conn = CONN_HOLDER.get();
        if (conn == null) {
            conn = DataSourceUtils.getConnection();
            CONN_HOLDER.set(conn);
        }
        return conn;
    }
    
    public static void remove() {
        CONN_HOLDER.remove();
    }
}
```

### 2.9 交易号生成算法
```java
public class TransactionNoGenerator {
    // 交易号格式：yyyyMMddHHmmss + 机器标识 + 4位随机数
    public static String generate() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        
        StringBuilder sb = new StringBuilder();
        sb.append(now.format(formatter));      // 时间部分：20260722143015
        sb.append(String.format("%02d",         // 机器标识（模拟）
            Math.abs(InetAddress.getLocalHost().hashCode()) % 99));
        sb.append(String.format("%04d",         // 4位随机数
            ThreadLocalRandom.current().nextInt(10000)));
        
        return sb.toString();
    }
    // 示例输出：2026072214301521783
}
```

### 2.10 自定义全局异常处理
```java
// 自定义DAO异常
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}

// 全局异常处理Filter
@WebFilter("/*")
public class ExceptionHandlerFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        try {
            chain.doFilter(req, resp);
        } catch (DataAccessException e) {
            // 记录日志
            log.error("数据库操作异常", e);
            // 跳转到错误页面
            req.setAttribute("error", "系统繁忙，请稍后重试");
            req.getRequestDispatcher("/WEB-INF/error/500.jsp").forward(req, resp);
        } catch (BusinessException e) {
            // 业务异常（如余额不足）
            req.setAttribute("error", e.getMessage());
            req.getRequestDispatcher("/WEB-INF/error/business-error.jsp").forward(req, resp);
        } catch (Exception e) {
            log.error("系统未知异常", e);
            req.setAttribute("error", "系统异常");
            req.getRequestDispatcher("/WEB-INF/error/500.jsp").forward(req, resp);
        }
    }
}
```

### 2.11 默认Servlet（DefaultServlet）
Tomcat内置的DefaultServlet用于处理静态资源（HTML、CSS、JS、图片等）。

```java
// 默认Servlet的映射路径为 "/"
// 当请求没有匹配到任何自定义Servlet时，由DefaultServlet处理

// 如果自定义了 "/" 映射的Servlet，静态资源将无法访问
// 解决方案：在自定义Servlet中放行静态资源请求
@WebServlet("/")
public class MyDefaultServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        String path = req.getRequestURI();
        if (path.endsWith(".css") || path.endsWith(".js") || path.endsWith(".png")) {
            // 静态资源转交给DefaultServlet处理
            getServletContext().getNamedDispatcher("default").forward(req, resp);
        } else {
            // 动态请求由当前Servlet处理
        }
    }
}
```

### 2.12 过滤器路径匹配规则
| 路径模式 | 匹配示例 | 说明 |
|---------|---------|------|
| `/*` | 所有请求 | 匹配所有路径 |
| `/api/*` | `/api/user`、`/api/order/1` | 匹配以 `/api` 开头的所有路径 |
| `*.do` | `/user.do`、`/login.do` | 匹配以 `.do` 结尾的路径 |
| `/` | 默认Servlet | 仅匹配默认Servlet路径 |
| `/exact/path` | `/exact/path` | 精确匹配 |

### 2.13 自定义404/500错误页面
```xml
<!-- web.xml 配置错误页面 -->
<error-page>
    <error-code>404</error-code>
    <location>/WEB-INF/errors/404.jsp</location>
</error-page>
<error-page>
    <error-code>500</error-code>
    <location>/WEB-INF/errors/500.jsp</location>
</error-page>
<error-page>
    <exception-type>java.lang.ArithmeticException</exception-type>
    <location>/WEB-INF/errors/arithmetic-error.jsp</location>
</error-page>
```

### 2.14 阿尔戈恩（Argon2）加密算法在登录中的应用
```java
// 阿尔戈恩是OWASP推荐的密码哈希算法，抗GPU攻击
// 实际项目中可使用Spring Security或专门的密码库
// 这里演示概念：

// 1. 用户注册时对密码做哈希
public class PasswordEncoder {
    // 伪代码：实际使用 Argon2PasswordEncoder
    public static String encode(String rawPassword) {
        // 生成盐值并计算哈希
        return argon2Hash(rawPassword); // 返回格式: $argon2id$v=19$m=65536,t=3,p=4$...salt...$...hash...
    }
    
    // 2. 登录验证时比对
    public static boolean matches(String rawPassword, String encodedHash) {
        // 从encodedHash中提取参数并验证
        return argon2Verify(rawPassword, encodedHash);
    }
}
```

### 2.15 Session与Token对比选型
| 对比维度 | Session | Token（如JWT） |
|----------|---------|----------------|
| 存储位置 | 服务端内存/Redis | 客户端（浏览器/App） |
| 扩展性 | 水平扩展需共享Session（Redis） | 天然支持水平扩展（无状态） |
| 跨域支持 | 困难（Cookie受同源策略限制） | 天然支持跨域 |
| 实时撤销 | 服务端直接删除Session即可 | 需维护黑名单或短TTL |
| 安全性 | CSRF风险（基于Cookie） | XSS风险（存储在localStorage） |
| 移动端 | 支持不友好 | 天然适配移动端 |
| 性能 | 每次请求查询服务端状态 | 无需服务端查询（JWT自包含） |

> 🎯 传统Web应用（服务端渲染）推荐Session；前后端分离/移动端推荐Token。

---

## 三、实战场景题（12题）

### 3.1 登录功能完整实现流程
```java
@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String rememberMe = req.getParameter("rememberMe"); // "on" 或 null
        
        // 1. 验证用户名密码
        User user = userService.login(username, password);
        
        if (user != null) {
            // 2. 登录成功，将用户信息存入Session
            HttpSession session = req.getSession();
            session.setAttribute("currentUser", user);
            
            // 3. 如果勾选了"记住我"，写入Cookie
            if ("on".equals(rememberMe)) {
                Cookie cookie = new Cookie("rememberUser", username);
                cookie.setMaxAge(7 * 24 * 60 * 60); // 7天
                cookie.setPath(req.getContextPath());
                resp.addCookie(cookie);
            }
            
            resp.sendRedirect(req.getContextPath() + "/dept/list");
        } else {
            // 4. 登录失败
            req.setAttribute("error", "用户名或密码错误");
            req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
        }
    }
}
```

### 3.2 检查登录状态的过滤器
```java
@WebFilter(urlPatterns = {"/dept/*", "/user/*"}) // 需要登录的资源
public class LoginCheckFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;
        
        // 1. 检查Session中是否有用户
        HttpSession session = request.getSession(false); // 不创建新Session
        User user = (session != null) ? 
            (User) session.getAttribute("currentUser") : null;
        
        if (user != null) {
            // 已登录，放行
            chain.doFilter(req, resp);
        } else {
            // 未登录，重定向到登录页
            request.setAttribute("msg", "请先登录");
            request.getRequestDispatcher("/WEB-INF/views/login.jsp")
                   .forward(request, response);
        }
    }
}
```

### 3.3 Cookie实现7天免登录
```java
// 1. 登录成功后创建记住用户的Cookie
Cookie cookie = new Cookie("rememberUser", username);
cookie.setMaxAge(7 * 24 * 60 * 60); // 有效期7天
cookie.setPath(request.getContextPath()); // 设置路径
cookie.setHttpOnly(true); // 禁止JavaScript访问，提高安全性
response.addCookie(cookie);

// 2. 登录页从Cookie中读取用户名，自动填充
@WebServlet("/login/page")
public class LoginPageServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        // 从Cookie中获取记住的用户名
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("rememberUser".equals(c.getName())) {
                    req.setAttribute("rememberedUser", c.getValue());
                    break;
                }
            }
        }
        req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
    }
}

// 3. 退出登录时删除Cookie
@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        // 清除Session
        HttpSession session = req.getSession(false);
        if (session != null) session.invalidate();
        
        // 删除Cookie
        Cookie cookie = new Cookie("rememberUser", "");
        cookie.setMaxAge(0); // 立即过期
        cookie.setPath(req.getContextPath());
        resp.addCookie(cookie);
        
        resp.sendRedirect(req.getContextPath() + "/login/page");
    }
}
```

### 3.4 部门管理CRUD完整流程
```java
// Entity
public class Dept {
    private Long id;
    private String deptNo;
    private String deptName;
    private String location;
    // getter/setter
}

// DAO接口
public interface DeptDao {
    List<Dept> findAll();
    Dept findById(Long id);
    int save(Dept dept);
    int update(Dept dept);
    int deleteById(Long id);
}

// Service
public class DeptService {
    private DeptDao deptDao = new DeptDaoImpl();
    
    @Transactional
    public void addDept(Dept dept) {
        // 业务校验
        if (dept.getDeptName() == null || dept.getDeptName().trim().isEmpty()) {
            throw new BusinessException("部门名称不能为空");
        }
        deptDao.save(dept);
    }
    
    @Transactional
    public void deleteDept(Long id) {
        // 检查是否有子部门或关联员工
        deptDao.deleteById(id);
    }
}

// Controller
@WebServlet("/dept/*")
public class DeptServlet extends HttpServlet {
    private DeptService deptService = new DeptService();
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws IOException, ServletException {
        String pathInfo = req.getPathInfo();
        
        if (pathInfo == null || "/list".equals(pathInfo)) {
            // 查询所有部门
            List<Dept> list = deptService.findAll();
            req.setAttribute("deptList", list);
            req.getRequestDispatcher("/WEB-INF/views/dept/list.jsp").forward(req, resp);
            
        } else if (pathInfo.startsWith("/detail/")) {
            Long id = Long.parseLong(pathInfo.split("/")[2]);
            Dept dept = deptService.findById(id);
            req.setAttribute("dept", dept);
            req.getRequestDispatcher("/WEB-INF/views/dept/detail.jsp").forward(req, resp);
            
        } else if ("/delete".equals(pathInfo)) {
            Long id = Long.parseLong(req.getParameter("id"));
            deptService.deleteDept(id);
            resp.sendRedirect(req.getContextPath() + "/dept/list");
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws IOException {
        String type = req.getParameter("type");
        
        if ("save".equals(type)) {
            Dept dept = new Dept();
            dept.setDeptNo(req.getParameter("deptNo"));
            dept.setDeptName(req.getParameter("deptName"));
            dept.setLocation(req.getParameter("location"));
            deptService.addDept(dept);
            
        } else if ("update".equals(type)) {
            Dept dept = new Dept();
            dept.setId(Long.parseLong(req.getParameter("id")));
            dept.setDeptNo(req.getParameter("deptNo"));
            dept.setDeptName(req.getParameter("deptName"));
            dept.setLocation(req.getParameter("location"));
            deptService.updateDept(dept);
        }
        
        resp.sendRedirect(req.getContextPath() + "/dept/list");
    }
}
```

### 3.5 Thymeleaf整合Servlet
```java
// Thymeleaf工具类（封装为BaseServlet）
public class ThymeleafUtil {
    private static final TemplateEngine engine;
    
    static {
        // ServletContextTemplateResolver 从 WEB-INF/templates/ 加载模板
        ServletContextTemplateResolver resolver = 
            new ServletContextTemplateResolver();
        resolver.setPrefix("/WEB-INF/templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setTemplateMode(TemplateMode.HTML);
        
        engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
    }
    
    public static void render(String template, 
                              HttpServletRequest req, 
                              HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html;charset=UTF-8");
        WebContext ctx = new WebContext(req, resp, req.getServletContext());
        engine.process(template, ctx, resp.getWriter());
    }
}

// 使用示例
@WebServlet("/dept/list")
public class DeptListServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws IOException {
        List<Dept> deptList = deptService.findAll();
        req.setAttribute("deptList", deptList);
        ThymeleafUtil.render("dept/list", req, resp);
    }
}
```

### 3.6 Thymeleaf模板常用语法
```html
<!-- 变量表达式 -->
<p th:text="${dept.deptName}">默认名称</p>

<!-- 条件判断 -->
<p th:if="${dept.location != null}">位置: [[${dept.location}]]</p>
<p th:unless="${dept.location != null}">位置未填写</p>

<!-- 循环遍历 -->
<table>
    <tr th:each="dept, stat : ${deptList}">
        <td th:text="${stat.count}">序号</td>
        <td th:text="${dept.deptNo}">编号</td>
        <td th:text="${dept.deptName}">名称</td>
        <td>
            <a th:href="@{/dept/detail/{id}(id=${dept.id})}">查看</a>
        </td>
    </tr>
</table>

<!-- 表单绑定 -->
<form th:action="@{/dept/save}" method="post">
    <input type="text" name="deptName" th:value="${dept?.deptName}" />
</form>

<!-- 片段复用 -->
<div th:replace="~{common/header :: header}"></div>

<!-- 内联JS -->
<script th:inline="javascript">
    var currentUser = [[${session.currentUser}]];
    console.log("当前用户: " + currentUser.username);
</script>
```

### 3.7 Token令牌实现"记住我"功能
```java
// Token数据结构
public class RememberMeToken {
    private String series;    // 系列号：每次登录生成，不变
    private String token;     // Token值：每次访问生成（可变）
    private String fingerprint; // 指纹：基于UA、IP等生成
    private Date createTime;
}

// Token生成与验证
public class TokenService {
    
    // 生成Token（登录成功后调用）
    public RememberMeToken generateToken(HttpServletRequest req, User user) {
        RememberMeToken t = new RememberMeToken();
        t.setSeries(generateSeries());       // 固定系列号
        t.setToken(generateTokenValue());     // 随机Token值
        t.setFingerprint(createFingerprint(req)); // 计算指纹
        t.setCreateTime(new Date());
        
        // 将token存储在数据库中（series作为查询键）
        tokenDao.save(user.getId(), t);
        
        // 将series:token写入Cookie
        Cookie cookie = new Cookie("rememberMe", 
            Base64.getEncoder().encodeToString(
                (t.getSeries() + ":" + t.getToken()).getBytes()));
        cookie.setMaxAge(30 * 24 * 60 * 60); // 30天
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        resp.addCookie(cookie);
        
        return t;
    }
    
    // 验证Token（自动登录时调用）
    public User validateToken(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        String cookieValue = findRememberMeCookie(cookies);
        if (cookieValue == null) return null;
        
        String decoded = new String(Base64.getDecoder().decode(cookieValue));
        String[] parts = decoded.split(":");
        String series = parts[0];
        String token = parts[1];
        
        // 从数据库查询
        RememberMeToken stored = tokenDao.findBySeries(series);
        if (stored == null) return null; // 已被删除
        
        // 验证指纹
        if (!stored.getFingerprint().equals(createFingerprint(req))) {
            // 指纹不匹配！可能是Cookie被盗用
            // 删除所有该用户的Token（安全措施）
            tokenDao.deleteBySeries(series);
            return null;
        }
        
        if (!stored.getToken().equals(token)) {
            // Token不匹配！可能是系列号被窃取
            // 安全处理：删除该系列所有Token
            return null;
        }
        
        // Token验证通过，刷新Token值
        String newToken = generateTokenValue();
        stored.setToken(newToken);
        tokenDao.update(series, newToken);
        
        return userDao.findById(stored.getUserId());
    }
    
    // 创建指纹（基于用户代理和IP）
    private String createFingerprint(HttpServletRequest req) {
        String ua = req.getHeader("User-Agent");
        String ip = req.getRemoteAddr();
        String data = ua + "|" + ip;
        return DigestUtils.sha256Hex(data); // 使用SHA-256哈希
    }
}
```

### 3.8 文件上传与预览
```java
@WebServlet("/upload")
@MultipartConfig(
    location = "D:/uploads",
    maxFileSize = 1024 * 1024 * 5,      // 5MB
    maxRequestSize = 1024 * 1024 * 20,   // 20MB
    fileSizeThreshold = 1024 * 1024      // 1MB
)
public class UploadServlet extends HttpServlet {
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        Part filePart = req.getPart("file");
        String fileName = filePart.getSubmittedFileName();
        
        // 防止路径穿越攻击
        fileName = new File(fileName).getName();
        // 生成唯一文件名
        String savedName = UUID.randomUUID() + "_" + fileName;
        
        // 保存文件
        String uploadPath = getServletContext().getRealPath("/uploads");
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) uploadDir.mkdirs();
        
        filePart.write(uploadPath + File.separator + savedName);
        
        req.setAttribute("message", "上传成功");
        req.setAttribute("fileUrl", "uploads/" + savedName);
        req.getRequestDispatcher("/WEB-INF/views/upload-result.jsp")
           .forward(req, resp);
    }
}
```

### 3.9 三层架构实现账户转账（完整流程）
```java
// 1. DAO层
public interface AccountDao {
    Account findByAccountNo(String accountNo);
    int updateBalance(String accountNo, BigDecimal amount);
}

public class AccountDaoImpl implements AccountDao {
    // 使用ThreadLocal获取当前线程的Connection（保证事务一致性）
    private Connection getConnection() {
        return ConnectionHolder.getConnection();
    }
    
    @Override
    public int updateBalance(String accountNo, BigDecimal amount) {
        String sql = "UPDATE t_account SET balance = balance + ? WHERE account_no = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setString(2, accountNo);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("更新余额失败", e);
        }
    }
}

// 2. Service层
public class AccountService {
    private AccountDao accountDao = new AccountDaoImpl();
    private TransactionManager txManager = new TransactionManager();
    
    public void transfer(TransferDTO dto) {
        try {
            txManager.beginTransaction();  // conn.setAutoCommit(false)
            
            // 扣款
            int rows = accountDao.updateBalance(
                dto.getFromAccount(), dto.getAmount().negate());
            if (rows != 1) {
                throw new BusinessException("扣款失败，账户不存在");
            }
            
            // 加款
            rows = accountDao.updateBalance(
                dto.getToAccount(), dto.getAmount());
            if (rows != 1) {
                throw new BusinessException("加款失败，收款账户不存在");
            }
            
            txManager.commit(); // 提交事务
        } catch (Exception e) {
            txManager.rollback(); // 回滚事务
            throw new BusinessException("转账失败", e);
        } finally {
            txManager.close();
        }
    }
}

// 3. Controller层
@WebServlet("/transfer")
public class TransferServlet extends HttpServlet {
    private AccountService accountService = new AccountService();
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws IOException {
        TransferDTO dto = new TransferDTO();
        dto.setFromAccount(req.getParameter("fromAccount"));
        dto.setToAccount(req.getParameter("toAccount"));
        dto.setAmount(new BigDecimal(req.getParameter("amount")));
        
        TransferVO vo = new TransferVO();
        try {
            accountService.transfer(dto);
            vo.setSuccess(true);
            vo.setTransactionNo(TransactionNoGenerator.generate());
        } catch (BusinessException e) {
            vo.setSuccess(false);
            vo.setErrorMsg(e.getMessage());
        }
        
        req.setAttribute("result", vo);
        req.getRequestDispatcher("/WEB-INF/views/transfer-result.jsp")
           .forward(req, resp);
    }
}
```

### 3.10 SSRF防御与路径安全
当处理用户提供的URL/路径时，需防止路径穿越和SSRF攻击：

```java
// 防止路径穿越（Path Traversal）
public String sanitizeFileName(String fileName) {
    // 去掉路径信息，只保留文件名
    File file = new File(fileName);
    String name = file.getName();
    
    // 过滤危险字符
    name = name.replaceAll("[\\\\/:*?\"<>|]", "");
    
    // 防止空文件名
    if (name.isEmpty()) {
        throw new IllegalArgumentException("文件名不合法");
    }
    return name;
}
```

### 3.11 多环境配置切换
```java
// 通过ServletContext的初始化参数实现环境切换
@WebListener
public class EnvironmentLoaderListener implements ServletContextListener {
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();
        String env = ctx.getInitParameter("spring.profiles.active");
        
        if (env == null) env = "dev";
        
        // 加载对应环境的配置文件
        Properties props = new Properties();
        try (InputStream is = getClass().getResourceAsStream("/application-" + env + ".properties")) {
            props.load(is);
            ctx.setAttribute("config", props);
        } catch (IOException e) {
            throw new RuntimeException("加载配置文件失败", e);
        }
    }
}
```

### 3.12 记录操作日志的Filter
```java
@WebFilter("/*")
public class AccessLogFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(AccessLogFilter.class);
    
    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        
        long start = System.currentTimeMillis();
        
        try {
            chain.doFilter(req, resp);
        } finally {
            long duration = System.currentTimeMillis() - start;
            log.info("{} {} 耗时:{}ms 用户:{}", 
                request.getMethod(), 
                request.getRequestURI(), 
                duration,
                request.getSession().getAttribute("currentUser"));
        }
    }
}
```

---

## 四、手写代码题（8题）

### 4.1 手写ThreadLocal的Connection管理器
```java
public class ConnectionManager {
    private static final ThreadLocal<Connection> CONNECTION_HOLDER = 
        new ThreadLocal<>();
    
    public static Connection getConnection() {
        Connection conn = CONNECTION_HOLDER.get();
        if (conn == null) {
            try {
                conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/mydb", "root", "password");
                CONNECTION_HOLDER.set(conn);
            } catch (SQLException e) {
                throw new RuntimeException("获取数据库连接失败", e);
            }
        }
        return conn;
    }
    
    public static void remove() {
        Connection conn = CONNECTION_HOLDER.get();
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                // ignore
            }
            CONNECTION_HOLDER.remove();
        }
    }
}
```

### 4.2 手写一个简单的Filter实现请求日志
```java
@WebFilter("/*")
public class LoggingFilter implements Filter {
    
    @Override
    public void init(FilterConfig filterConfig) {
        System.out.println("LoggingFilter 初始化");
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
            FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        
        // 前置增强
        long startTime = System.currentTimeMillis();
        System.out.println("[请求开始] " + req.getMethod() + " " + req.getRequestURI());
        
        chain.doFilter(request, response); // 放行
        
        // 后置增强
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("[请求结束] " + req.getRequestURI() + " 耗时: " + duration + "ms");
    }
    
    @Override
    public void destroy() {
        System.out.println("LoggingFilter 销毁");
    }
}
```

### 4.3 手写一个Listener统计在线人数
```java
@WebListener
public class OnlineUserListener implements HttpSessionListener, 
        HttpSessionAttributeListener {
    
    private static int onlineCount = 0;
    
    @Override
    public void sessionCreated(HttpSessionEvent se) {
        onlineCount++;
        updateOnlineCount(se.getSession());
    }
    
    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        onlineCount--;
        updateOnlineCount(se.getSession());
    }
    
    @Override
    public void attributeAdded(HttpSessionBindingEvent se) {
        if ("currentUser".equals(se.getName())) {
            updateOnlineCount(se.getSession());
        }
    }
    
    private void updateOnlineCount(HttpSession session) {
        session.getServletContext().setAttribute("onlineCount", onlineCount);
    }
}
```

### 4.4 手写一个编码过滤器解决中文乱码
```java
@WebFilter("/*")
public class EncodingFilter implements Filter {
    
    private String encoding = "UTF-8";
    
    @Override
    public void init(FilterConfig config) {
        String e = config.getInitParameter("encoding");
        if (e != null) encoding = e;
    }
    
    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, 
            FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;
        
        // 设置请求编码
        request.setCharacterEncoding(encoding);
        
        // 设置响应编码
        response.setCharacterEncoding(encoding);
        response.setContentType("text/html;charset=" + encoding);
        
        chain.doFilter(req, resp);
    }
    
    @Override
    public void destroy() { }
}
```

> ⚠️ GET请求乱码处理：Tomcat 8.0以下需配置 `server.xml` 中 Connector 的 `URIEncoding="UTF-8"`；8.0+默认UTF-8。

### 4.5 手写一个分页工具类
```java
public class PageResult<T> {
    private List<T> data;       // 当前页数据
    private int pageNum;        // 当前页码
    private int pageSize;       // 每页条数
    private int totalCount;     // 总记录数
    private int totalPages;     // 总页数
    private int startRow;       // 起始行
    
    public PageResult(int pageNum, int pageSize, int totalCount, List<T> data) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.totalCount = totalCount;
        this.data = data;
        this.totalPages = (int) Math.ceil((double) totalCount / pageSize);
        this.startRow = (pageNum - 1) * pageSize;
    }
    
    public boolean isHasPrevious() { return pageNum > 1; }
    public boolean isHasNext() { return pageNum < totalPages; }
}

// DAO中的分页查询
public List<User> findPage(int pageNum, int pageSize, String keyword) {
    String sql = "SELECT * FROM t_user WHERE username LIKE ? LIMIT ?, ?";
    int start = (pageNum - 1) * pageSize;
    // 执行查询...
}

public int count(String keyword) {
    String sql = "SELECT COUNT(*) FROM t_user WHERE username LIKE ?";
    // 执行查询...
}
```

### 4.6 手写一个验证用户是否登录的Filter
```java
@WebFilter(urlPatterns = {"/admin/*", "/user/profile", "/order/*"})
public class AuthenticationFilter implements Filter {
    
    // 白名单：不需要登录即可访问的路径
    private static final Set<String> WHITE_LIST = Set.of(
        "/login", "/register", "/captcha", "/public"
    );
    
    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, 
            FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;
        
        String path = request.getRequestURI().replaceFirst(
            request.getContextPath(), "");
        
        // 白名单路径放行
        for (String whitePath : WHITE_LIST) {
            if (path.startsWith(whitePath)) {
                chain.doFilter(req, resp);
                return;
            }
        }
        
        // 检查登录状态
        HttpSession session = request.getSession(false);
        boolean loggedIn = (session != null && 
            session.getAttribute("currentUser") != null);
        
        if (loggedIn) {
            chain.doFilter(req, resp);
        } else {
            // 记录原始URL，登录后跳回
            String originalUrl = request.getRequestURI();
            String query = request.getQueryString();
            if (query != null) originalUrl += "?" + query;
            
            response.sendRedirect(
                request.getContextPath() + "/login?redirect=" + 
                URLEncoder.encode(originalUrl, "UTF-8"));
        }
    }
}
```

### 4.7 手写一个配置可读的ServletContext工具
```java
// 在Listener中加载配置
@WebListener
public class ConfigLoaderListener implements ServletContextListener {
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();
        Properties props = new Properties();
        
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("app.properties")) {
            props.load(is);
            
            // 将配置存入ServletContext
            for (String key : props.stringPropertyNames()) {
                ctx.setAttribute("config." + key, props.getProperty(key));
            }
        } catch (IOException e) {
            throw new RuntimeException("加载配置文件失败", e);
        }
    }
}

// 在Servlet中读取配置
String dbUrl = (String) getServletContext().getAttribute("config.db.url");
String dbUser = (String) getServletContext().getAttribute("config.db.username");
```

### 4.8 手写一个简易的MVC分发器
```java
@WebServlet("/controller/*")
public class DispatcherServlet extends HttpServlet {
    
    private Map<String, Controller> controllerMap = new HashMap<>();
    
    @Override
    public void init() {
        // 注册路由（模拟Spring MVC的@RequestMapping）
        controllerMap.put("/dept/list", new DeptListController());
        controllerMap.put("/dept/save", new DeptSaveController());
        controllerMap.put("/user/login", new LoginController());
    }
    
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        String path = req.getPathInfo();
        
        Controller controller = controllerMap.get(path);
        if (controller != null) {
            try {
                String viewPath = controller.handleRequest(req, resp);
                // 结果视图转发
                if (viewPath != null) {
                    req.getRequestDispatcher("/WEB-INF/views/" + viewPath + ".jsp")
                       .forward(req, resp);
                }
            } catch (Exception e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            }
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "路由不存在: " + path);
        }
    }
    
    // Controller接口
    interface Controller {
        String handleRequest(HttpServletRequest req, HttpServletResponse resp) 
            throws Exception;
    }
}
```

---

## 五、系统设计题（5题）

### 5.1 设计一个百万级用户量的登录系统
**关键设计点：**

| 关注点 | 设计方案 |
|--------|---------|
| 密码存储 | Argon2/BCrypt哈希，加盐，不可逆 |
| Session共享 | Redis存储Session，所有Tomcat实例连接同一Redis集群 |
| 验证码 | 集成行为验证码（滑块/点选），防止撞库攻击 |
| 限流防护 | 单IP每分钟最多5次登录尝试 |
| 并发控制 | 同一账号同一时间只能一个设备登录（踢下线机制） |
| 安全审计 | 记录登录日志（时间、IP、设备、操作结果） |

```
┌──────────┐     ┌──────────┐     ┌──────────┐
│  浏览器    │────▶│  Nginx   │────▶│ Tomcat 1 │
└──────────┘     │ (负载均衡) │     ├──────────┤
                 │          │     │ Tomcat 2 │
                 └──────────┘     ├──────────┤
                                  │ Tomcat 3 │
                                  └────┬─────┘
                                       │
                           ┌───────────▼───────────┐
                           │       Redis集群        │
                           │   (Session共享 + 缓存) │
                           └───────────────────────┘
```

### 5.2 设计一个通用的权限管理框架
```java
// RBAC模型（Role-Based Access Control）
// 用户 → 角色 → 权限（资源 + 操作）

// 权限注解示例
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    String value();        // 权限标识，如 "dept:delete"
    String description() default "";
}

// 权限检查Filter
@WebFilter("/*")
public class PermissionFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, 
            FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpSession session = request.getSession(false);
        
        if (session == null) {
            chain.doFilter(req, resp);
            return;
        }
        
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            chain.doFilter(req, resp);
            return;
        }
        
        // 获取用户权限列表（从缓存/数据库）
        Set<String> permissions = getPermissions(user.getId());
        
        // 将权限放入请求属性，供后续Servlet/MVC使用
        request.setAttribute("_permissions", permissions);
        
        chain.doFilter(req, resp);
    }
}
```

### 5.3 设计一个支持高并发的秒杀系统
**核心要点：**

1. **请求拦截**：Filter层限流，令牌桶算法，单用户限购
2. **库存扣减**：数据库行锁 `UPDATE stock SET count = count - 1 WHERE id = ? AND count > 0`
3. **异步削峰**：使用消息队列（RabbitMQ/Kafka）排队处理
4. **页面静态化**：商品详情页静态化，CDN加速
5. **接口防刷**：隐藏秒杀接口地址，需先获取Token

```java
// 事务层扣库存（必须保证原子性）
@WebServlet("/seckill")
public class SecKillServlet extends HttpServlet {
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws IOException {
        // 1. 校验秒杀Token
        String killToken = req.getParameter("killToken");
        if (!validateKillToken(killToken)) {
            resp.getWriter().write("{\"code\": -1, \"msg\": \"非法请求\"}");
            return;
        }
        
        // 2. 执行秒杀（原子扣库存 + 生成订单）
        try {
            // 使用数据库行级锁防止超卖
            int rows = seckillDao.decrementStock(productId);
            if (rows > 0) {
                orderDao.createOrder(userId, productId);
                resp.getWriter().write("{\"code\": 1, \"msg\": \"秒杀成功\"}");
            } else {
                resp.getWriter().write("{\"code\": 0, \"msg\": \"库存不足\"}");
            }
        } catch (Exception e) {
            resp.getWriter().write("{\"code\": -2, \"msg\": \"系统繁忙\"}");
        }
    }
}

// DAO层使用行级锁防超卖
public int decrementStock(Long productId) {
    String sql = "UPDATE t_product SET stock = stock - 1 " +
                 "WHERE id = ? AND stock > 0";
    // 返回影响行数：1表示成功，0表示库存不足
}
```

### 5.4 设计一个分布式Session共享方案
**基于Redis的Session共享：**

```java
// Filter实现：拦截所有请求，从Redis加载Session
@WebFilter("/*")
public class RedisSessionFilter implements Filter {
    private JedisPool jedisPool;
    
    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, 
            FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        
        // 1. 从Cookie或请求参数中获取Session ID
        String sessionId = findSessionId(request);
        
        // 2. 从Redis加载Session数据
        Map<String, Object> sessionData = loadFromRedis(sessionId);
        
        // 3. 创建自定义的Session包装器
        HttpSession session = new RedisSessionWrapper(sessionId, sessionData, jedisPool);
        
        // 4. 使用包装后的Session
        chain.doFilter(new SessionRequestWrapper(request, session), resp);
    }
}

class RedisSessionWrapper implements HttpSession {
    private String id;
    private Map<String, Object> data;
    private JedisPool jedisPool;
    
    @Override
    public Object getAttribute(String name) {
        return data.get(name);
    }
    
    @Override
    public void setAttribute(String name, Object value) {
        data.put(name, value);
        // 同步更新到Redis
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hset("session:" + id, name, serialize(value));
            jedis.expire("session:" + id, 1800); // 30分钟超时
        }
    }
}
```

### 5.5 设计一个API网关的过滤器链
```java
// 模拟Spring Cloud Gateway的过滤器链设计
public interface GatewayFilter {
    void filter(RequestContext ctx, GatewayFilterChain chain);
}

public class GatewayFilterChain {
    private List<GatewayFilter> filters;
    private int index = 0;
    
    public GatewayFilterChain(List<GatewayFilter> filters) {
        this.filters = filters;
    }
    
    public void next(RequestContext ctx) {
        if (index < filters.size()) {
            filters.get(index++).filter(ctx, this);
        }
    }
}

// 具体过滤器示例
public class RateLimitFilter implements GatewayFilter {
    @Override
    public void filter(RequestContext ctx, GatewayFilterChain chain) {
        String ip = ctx.getIp();
        if (isRateLimited(ip)) {
            ctx.setResponseCode(429); // Too Many Requests
            return; // 不继续执行chain
        }
        chain.next(ctx);
    }
}

public class AuthFilter implements GatewayFilter {
    @Override
    public void filter(RequestContext ctx, GatewayFilterChain chain) {
        String token = ctx.getHeader("Authorization");
        if (token == null || !validateToken(token)) {
            ctx.setResponseCode(401);
            return;
        }
        chain.next(ctx);
    }
}
```

---

## 六、常见坑点与最佳实践

### 6.1 JavaWeb常见坑点汇总

| 坑点 | 问题描述 | 解决方案 |
|------|---------|---------|
| Servlet成员变量线程不安全 | 多个线程共享同一个Servlet实例的成员变量 | 使用局部变量或ThreadLocal |
| GET请求中文乱码 | Tomcat 8.0以下默认ISO-8859-1解码URL | 配置 URIEncoding="UTF-8" |
| POST请求中文乱码 | 未设置请求编码 | `request.setCharacterEncoding("UTF-8")` |
| 转发后CSS/JS不生效 | 转发URL不变，相对路径可能失效 | 使用绝对路径（`${pageContext.request.contextPath}`） |
| Session过多导致OOM | 未合理设置Session超时时间 | 缩短超时时间，集群使用Redis |
| Cookie路径问题 | Cookie设置了路径，其他路径无法访问 | 统一设置 path="/" |
| 文件上传文件名冲突 | 同名文件会被覆盖 | UUID生成唯一文件名 |
| 过滤器顺序混乱 | 多个Filter的执行顺序不符合预期 | web.xml方式按声明顺序，注解方式按类名排序 |
| 数据库连接未释放 | 连接泄露导致数据库连接池耗尽 | 使用try-with-resources或finally关闭 |
| 重定向后取不到request属性 | 重定向是两次请求，request不复存在 | 改用session或重定向URL中拼接参数 |

### 6.2 JavaWeb最佳实践清单

| 类别 | 最佳实践 |
|------|---------|
| 编码 | 全栈统一使用UTF-8（JSP、Servlet、数据库、文件） |
| 异常处理 | 自定义业务异常（BusinessException），使用Filter统一处理异常 |
| 日志 | 使用SLF4J接口 + Logback实现，合理配置日志级别 |
| 分层 | 严格遵循 Controller → Service → DAO 三层分层，禁止跨层调用 |
| 事务 | Service层统一管理事务，DAO层不涉及事务控制 |
| 参数校验 | Controller层做基本校验（非空、格式），Service层做业务校验 |
| 密码存储 | 使用Argon2/BCrypt等慢哈希算法，禁止MD5/SHA-1明文存储 |
| Session管理 | 登录成功后开启新Session（`session.invalidate()`后创建新Session） |
| 资源释放 | 数据库连接、IO流等必在finally中释放 |
| 路径 | 统一使用绝对路径（以 `/` 开头） |

### 6.3 性能优化建议

| 优化策略 | 具体措施 |
|---------|---------|
| 数据库 | 连接池（HikariCP）、索引优化、SQL慢查询分析 |
| 缓存 | 热点数据使用Redis或本地缓存（Caffeine） |
| 静态资源 | Nginx直接处理静态资源，不经过Tomcat |
| 异步处理 | 耗时操作（发邮件、生成报表）提交到线程池异步执行 |
| 会话管理 | 大型应用使用Redis共享Session |
| 前端优化 | 页面静态化、CDN加速、减少HTTP请求数 |

---

## 七、面试回答模板（Top 5）

### 7.1 "请描述Servlet的生命周期"
```
回答模板：
"Servlet的生命周期由Servlet容器（如Tomcat）管理，分为三个阶段：
第一，初始化阶段。容器通过反射创建Servlet实例，调用init()方法。
可以通过@WebServlet(loadOnStartup=1)让容器在启动时初始化，否则在首次请求时初始化。
第二，请求处理阶段。每次请求到达时，容器调用service()方法，
该方法内部根据HTTP方法类型（GET/POST等）自动分发给对应的doGet()/doPost()方法处理。
第三，销毁阶段。容器关闭或卸载应用时，调用destroy()方法释放资源。"

要点：init()只调用1次，service()调用N次，destroy()调用1次。
```

### 7.2 "转发和重定向有什么区别"
```
回答模板：
"转发和重定向是页面跳转的两种方式，核心区别有三点：
第一，URL变化。转发是服务器内部跳转，URL不变；
重定向是浏览器重新发起请求，URL变为新地址。
第二，请求次数。转发只有1次请求，重定向有2次请求。
第三，数据共享。转发可以共享request域中的数据；
重定向是两次请求，无法共享request域，只能通过Session传递。
在使用场景上：需要带数据到下一页用转发，
需要避免重复提交（如表单提交后）用重定向。
"

要点：2次请求 vs 1次请求、URL变不变、request能否共享
```

### 7.3 "Session和Cookie的区别"
```
回答模板：
"Session和Cookie都是会话跟踪技术，主要区别如下：
存储位置：Cookie存储在浏览器端，Session存储在服务器端（内存/Redis）。
安全性：Cookie明文传输，容易被篡改；Session数据在服务端，相对安全。
存储容量：单个Cookie最大4KB，Session理论上无限制。
存储类型：Cookie只能存字符串，Session可以存任意对象。
生命周期：Cookie可以设置长期有效，Session默认30分钟不活动自动销毁。
当浏览器禁用Cookie时，Session无法通过默认方式工作，需要配合URL重写技术。
在实际项目中，通常将重要数据放在Session中，
非敏感数据（如用户偏好设置）可以放在Cookie中。"

要点：客户端vs服务端、安全性差异、禁用Cookie的解决方案
```

### 7.4 "如何解决Servlet的线程安全问题"
```
回答模板：
"Servlet是单实例多线程的，默认情况下存在线程安全问题。
解决方案有以下几种：
第一，避免使用成员变量，全部使用局部变量。
因为局部变量在栈空间中，每个线程独享。
第二，如果必须使用成员变量，使用ThreadLocal进行线程隔离，
将变量绑定到当前线程。
第三，使用synchronized加锁同步，但会降低并发性能。
在实际项目中，优先使用第一种方式。
如果需要在多个方法间共享数据，推荐使用ThreadLocal。
需要注意的是，使用ThreadLocal后务必在请求结束时调用remove()方法，
否则在Tomcat的线程池环境下可能导致内存泄漏。"

要点：单例多线程、局部变量/ThreadLocal/synchronized、内存泄漏
```

### 7.5 "你对三层架构的理解"
```
回答模板：
"三层架构是一种分层设计思想，将应用分为三层：
第一，Controller层（表示层/控制层），负责接收请求、参数校验、
调用Service层、处理响应（视图转发或返回JSON）。
第二，Service层（业务逻辑层），封装核心业务逻辑，管理事务，
做业务校验，不直接操作数据库。
第三，DAO层（数据访问层），负责与数据库交互，执行SQL语句，完成CRUD操作。
三层架构的优点：解耦——每一层各司其职，改动不影响其他层；
复用——Service层可为多个Controller复用；
可测试性——每层可以独立进行单元测试。
注意：三层架构和MVC是不同维度的概念，
三层架构是系统级别的分层架构，
MVC是Controller层内部的设计模式。"

要点：分层职责、解耦/复用/可测试、与MVC的区别
```

---

## 八、快速查漏补缺Checklist

### 8.1 面试前必会清单

**必须能口述清楚：**
- [ ] BS架构 vs CS架构优缺点
- [ ] URL/URI/URN关系图解
- [ ] HTTP请求协议结构
- [ ] GET与POST区别（至少说出5点）
- [ ] 转发 vs 重定向（3点核心区别）
- [ ] Servlet生命周期（3个阶段）
- [ ] Session vs Cookie vs Token
- [ ] 三层架构分层与职责
- [ ] ThreadLocal原理（内存泄漏）

**必须能手写：**
- [ ] 一个完整的Servlet（doGet/doPost）
- [ ] 一个编码过滤器（EncodingFilter）
- [ ] 登录检查过滤器（LoginCheckFilter）
- [ ] ThreadLocal管理数据库连接
- [ ] 一个Listener（在线人数统计）

**必须理解：**
- [ ] 适配器模式（GenericServlet）
- [ ] 模板方法模式（HttpServlet）
- [ ] 责任链模式（Filter Chain）
- [ ] 线程安全问题及解决方案
- [ ] Token机制（series/fingerprint）
- [ ] 事务管理（ThreadLocal + Connection）

### 8.2 各章节重点图

```
Tomcat架构图：
Server → Service → Connector + Engine → Host → Context → Wrapper(Servlet)

Servlet执行流程：
浏览器 → HTTP请求 → Tomcat Connector → Servlet.service() → doGet/doPost → 响应

Filter责任链：
Filter1 → Filter2 → Filter3 → Servlet → Filter3 → Filter2 → Filter1 → 响应
```

> 🎯 本面试宝典覆盖了JavaWeb从入门到精通的160节课程核心知识点，建议结合课程大纲对照学习，重点练习手写代码部分，面试前优先完成Checklist中的"必须能口述清楚"和"必须能手写"项目。
