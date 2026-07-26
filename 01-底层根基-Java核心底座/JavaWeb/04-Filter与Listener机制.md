# 04 - Filter 与 Listener 机制

> Filter 拦截器链和 Listener 监听器是 Java Web 的 AOP 基石。Filter 处理横切关注点（编码、权限、日志），Listener 感知容器生命周期事件——二者组合构成 Spring 拦截器和事件机制的原型。

---

## 目录

1. [Filter 过滤器](#1-filter-过滤器)
2. [Filter 链与执行顺序](#2-filter-链与执行顺序)
3. [Filter 实战场景](#3-filter-实战场景)
4. [Listener 监听器](#4-listener-监听器)
5. [八大 Listener 详解](#5-八大-listener-详解)
6. [观察者模式在 Web 中的应用](#6-观察者模式在-web-中的应用)
7. [Filter/Listener vs Spring 对应物](#7-filterlistener-vs-spring-对应物)
8. [常见面试题](#8-常见面试题)

---

## 1. Filter 过滤器

### 1.1 什么是 Filter

> Filter 是 Servlet 规范中的**可插拔组件**，在请求到达 Servlet **之前**和响应返回客户端 **之后**对请求/响应进行预处理和后处理。它是 Java Web 中实现 AOP（面向切面编程）思想的原生机制。

```
请求流程（带 Filter 链）：

Client ──请求──> Filter1 ──> Filter2 ──> Filter3 ──> Servlet
                   │            │            │
Client <──响应── Filter1 <── Filter2 <── Filter3 <── Servlet

每个 Filter 可以在请求到达下一个组件之前（pre-processing）
和响应从下一个组件返回之后（post-processing）执行逻辑
```

### 1.2 Filter 接口与生命周期

```java
public interface Filter {
    // 1. 初始化：容器启动时调用，只执行一次
    default void init(FilterConfig filterConfig) throws ServletException { }

    // 2. 过滤：每次匹配的请求到达时调用
    void doFilter(ServletRequest request, ServletResponse response,
                  FilterChain chain) throws IOException, ServletException;

    // 3. 销毁：容器关闭时调用，只执行一次
    default void destroy() { }
}
```

### 1.3 Filter 基本示例

```java
@WebFilter("/*")  // 拦截所有请求
public class LoggingFilter implements Filter {

    @Override
    public void init(FilterConfig config) {
        // 获取初始化参数
        String logLevel = config.getInitParameter("logLevel");
        // 初始化资源（如日志文件）
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // ═══ 前置处理（请求到达 Servlet 之前） ═══
        long start = System.currentTimeMillis();
        String uri = req.getRequestURI();
        String method = req.getMethod();
        System.out.println("→ [" + method + "] " + uri + " - 开始");

        // ═══ 调用下一个 Filter 或 Servlet ═══
        // ⚠️ 必须调用！否则请求链中断，Servlet 不会执行
        chain.doFilter(request, response);

        // ═══ 后置处理（响应从 Servlet 返回之后） ═══
        long elapsed = System.currentTimeMillis() - start;
        int status = resp.getStatus();
        System.out.println("← [" + method + "] " + uri
            + " - " + status + " - " + elapsed + "ms");
    }

    @Override
    public void destroy() {
        // 释放资源
    }
}
```

> ⚠️ `chain.doFilter()` 是链的关键——不调用则请求中断！Filter 链按注册顺序执行，`doFilter()` 前后的代码分别对应前置和后置处理。

---

## 2. Filter 链与执行顺序

### 2.1 执行模型

```
时序图（调用栈）：

doFilter of FilterA  ─────────────────────────────────────┐
  │  [A 前置：权限检查]                                      │
  │  chain.doFilter() ──> doFilter of FilterB ────────┐   │
  │                         │  [B 前置：编码设置]        │   │
  │                         │  chain.doFilter() ──>   │   │
  │                         │    [C 前置：日志记录]      │   │
  │                         │    chain.doFilter() ──>  │   │
  │                         │      Servlet.service()   │   │
  │                         │    <── 返回               │   │
  │                         │  [C 后置：记录耗时]        │   │
  │                         <── 返回                    │   │
  │                       [B 后置：清理]                 │   │
  <── 返回                                               │   │
  [A 后置：统计]                                          │   │
────────────────────────────────────────────────────────┘

执行顺序：A前 → B前 → C前 → Servlet → C后 → B后 → A后
类似栈：先进后出（FILO）
```

### 2.2 Filter 执行顺序规则

| 配置方式 | 顺序规则 |
|---------|---------|
| **web.xml** | 按 `<filter-mapping>` 的声明顺序 |
| **@WebFilter 注解** | 按 Filter **类名的字典序**（不可靠，不推荐依赖） |
| **编程式注册** | 按 `addMapping` 的调用顺序（明确可控） |

```java
// ⚠️ 重要：如果多个 Filter 有顺序依赖，不要依赖注解！
// 应使用 web.xml 或编程式注册明确指定顺序

// web.xml 中明确顺序：
// <filter-mapping>
//     <filter-name>charsetFilter</filter-name>   <!-- 最先执行 -->
//     <url-pattern>/*</url-pattern>
// </filter-mapping>
// <filter-mapping>
//     <filter-name>authFilter</filter-name>      <!-- 第二个执行 -->
//     <url-pattern>/*</url-pattern>
// </filter-mapping>

// 或编程式注册：
@Override
public void onStartup(ServletContext ctx) {
    ctx.addFilter("CharsetFilter", CharsetFilter.class)
       .addMappingForUrlPatterns(null, false, "/*");
    ctx.addFilter("AuthFilter", AuthFilter.class)
       .addMappingForUrlPatterns(null, false, "/*");
}
```

### 2.3 DispatcherType 分发类型

```java
// Filter 默认只拦截普通请求（REQUEST），可配置拦截更多类型：
@WebFilter(
    urlPatterns = "/*",
    dispatcherTypes = {
        DispatcherType.REQUEST,   // 普通请求（默认）
        DispatcherType.FORWARD,   // forward 转发的请求
        DispatcherType.INCLUDE,   // include 包含的请求
        DispatcherType.ERROR,     // 错误页面的请求
        DispatcherType.ASYNC      // 异步请求
    }
)
```

---

## 3. Filter 实战场景

### 3.1 字符编码 Filter

```java
@WebFilter("/*")
public class CharsetFilter implements Filter {
    private String encoding = "UTF-8";

    @Override
    public void init(FilterConfig config) {
        String param = config.getInitParameter("encoding");
        if (param != null) encoding = param;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        request.setCharacterEncoding(encoding);
        response.setCharacterEncoding(encoding);
        response.setContentType("text/html;charset=" + encoding);
        chain.doFilter(request, response);
    }
}

// Spring Boot 中一行搞定：
// spring.http.encoding.charset=UTF-8
// spring.http.encoding.enabled=true
// spring.http.encoding.force=true
```

### 3.2 XSS 防护 Filter

```java
@WebFilter("/*")
public class XssFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        // 使用包装类转义危险字符
        chain.doFilter(new XssRequestWrapper((HttpServletRequest) request), response);
    }
}

// HttpServletRequestWrapper 包装 — 装饰器模式
public class XssRequestWrapper extends HttpServletRequestWrapper {
    public XssRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return sanitize(value);
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) return null;
        return Arrays.stream(values).map(this::sanitize).toArray(String[]::new);
    }

    private String sanitize(String value) {
        if (value == null) return null;
        return value.replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }
}
```

### 3.3 CORS 跨域 Filter

```java
@WebFilter("/*")
public class CorsFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletResponse resp = (HttpServletResponse) response;
        resp.setHeader("Access-Control-Allow-Origin", "https://example.com");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
        resp.setHeader("Access-Control-Max-Age", "3600");

        // OPTIONS 预检请求直接返回，不进入 Servlet
        HttpServletRequest req = (HttpServletRequest) request;
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(request, response);
    }
}
```

### 3.4 请求频率限制 Filter

```java
@WebFilter("/api/*")
public class RateLimitFilter implements Filter {
    // IP → (窗口起始时间, 请求计数)
    private final ConcurrentHashMap<String, long[]> counter = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS = 100;   // 每个窗口最多请求数
    private static final long WINDOW_MS = 60_000;  // 时间窗口 60 秒

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String ip = req.getRemoteAddr();

        long now = System.currentTimeMillis();
        long[] record = counter.compute(ip, (k, v) -> {
            if (v == null) return new long[]{now, 1};
            if (now - v[0] > WINDOW_MS) return new long[]{now, 1}; // 新窗口
            v[1]++;
            return v;
        });

        if (record[1] > MAX_REQUESTS) {
            resp.setStatus(429); // Too Many Requests
            resp.getWriter().write("{\"error\":\"请求过于频繁，请稍后再试\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
```

### 3.5 响应压缩 Filter

```java
@WebFilter("/*")
public class GzipFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // 检查客户端是否支持 Gzip
        String acceptEncoding = req.getHeader("Accept-Encoding");
        if (acceptEncoding != null && acceptEncoding.contains("gzip")) {
            // 使用包装类拦截响应输出 → 压缩后再发送
            GzipResponseWrapper wrapper = new GzipResponseWrapper(resp);
            chain.doFilter(request, wrapper);
            wrapper.finish();  // 完成压缩并写入原始响应
        } else {
            chain.doFilter(request, response);
        }
    }
}
```

---

## 4. Listener 监听器

### 4.1 什么是 Listener

> Listener 是基于**观察者模式**的接口，监听 Web 应用中的三大类事件——ServletContext（应用级）、HttpSession（会话级）、ServletRequest（请求级）的生命周期和属性变化。

### 4.2 三大类事件

```
ServletContext 事件（应用级）
  ├── 生命周期：contextInitialized / contextDestroyed
  └── 属性变化：attributeAdded / attributeReplaced / attributeRemoved

HttpSession 事件（会话级）
  ├── 生命周期：sessionCreated / sessionDestroyed
  ├── 属性变化：attributeAdded / attributeReplaced / attributeRemoved
  ├── 钝化/活化：sessionWillPassivate / sessionDidActivate（序列化到磁盘）
  └── 绑定监听：valueBound / valueUnbound（对象感知自己被绑定到 Session）

ServletRequest 事件（请求级）
  ├── 生命周期：requestInitialized / requestDestroyed
  └── 属性变化：attributeAdded / attributeReplaced / attributeRemoved
```

---

## 5. 八大 Listener 详解

### 5.1 ServletContextListener（⭐最常用）

```java
@WebListener
public class AppStartupListener implements ServletContextListener {

    // 应用启动时调用 — 常用于初始化资源
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();
        System.out.println("=== 应用启动： " + ctx.getContextPath() + " ===");

        // 典型初始化工作：
        // 1. 加载全局配置
        // 2. 初始化数据库连接池
        // 3. 启动定时任务
        // 4. 预热缓存
        // 5. 注册 JMX MBean
    }

    // 应用关闭时调用 — 常用于释放资源
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("=== 应用关闭，释放资源 ===");
        // 1. 关闭连接池
        // 2. 停止定时任务
        // 3. 清理临时文件
        // 4. 注销 JMX MBean
    }
}
```

### 5.2 ServletContextAttributeListener

```java
@WebListener
public class ContextAttributeListener
        implements ServletContextAttributeListener {

    @Override
    public void attributeAdded(ServletContextAttributeEvent event) {
        System.out.println("Context 属性添加: "
            + event.getName() + " = " + event.getValue());
    }

    @Override
    public void attributeReplaced(ServletContextAttributeEvent event) {
        System.out.println("Context 属性替换: "
            + event.getName() + "，旧值=" + event.getValue());
        // event.getValue() 返回的是旧值（替换前的值）
    }

    @Override
    public void attributeRemoved(ServletContextAttributeEvent event) {
        System.out.println("Context 属性移除: "
            + event.getName() + "，当时值=" + event.getValue());
    }
}
```

### 5.3 HttpSessionListener — 在线人数统计

```java
@WebListener
public class OnlineCounterListener implements HttpSessionListener {

    private static final AtomicInteger onlineCount = new AtomicInteger(0);

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        int count = onlineCount.incrementAndGet();
        se.getSession().getServletContext()
          .setAttribute("onlineCount", count);
        System.out.println("新用户上线，当前在线：" + count);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        int count = onlineCount.decrementAndGet();
        se.getSession().getServletContext()
          .setAttribute("onlineCount", count);
        System.out.println("用户下线，当前在线：" + count);
    }
}
```

### 5.4 HttpSessionAttributeListener

```java
@WebListener
public class SessionAttributeListener
        implements HttpSessionAttributeListener {

    @Override
    public void attributeAdded(HttpSessionBindingEvent event) {
        if ("currentUser".equals(event.getName())) {
            System.out.println("用户登录: " + event.getValue());
        }
    }

    @Override
    public void attributeRemoved(HttpSessionBindingEvent event) {
        if ("currentUser".equals(event.getName())) {
            System.out.println("用户登出: " + event.getValue());
        }
    }

    @Override
    public void attributeReplaced(HttpSessionBindingEvent event) {
        // 替换时：event.getValue() 返回旧值
    }
}
```

### 5.5 HttpSessionBindingListener — 对象感知绑定

```java
// 不需要 @WebListener 注解！让实体类自己实现此接口
public class User implements HttpSessionBindingListener {
    private String username;

    // 对象被放入 Session 时自动调用
    @Override
    public void valueBound(HttpSessionBindingEvent event) {
        System.out.println(username + " 被绑定到 Session: " + event.getSession().getId());
        // 可以在这里做：记录登录日志、触发欢迎消息等
    }

    // 对象从 Session 移除时自动调用
    @Override
    public void valueUnbound(HttpSessionBindingEvent event) {
        System.out.println(username + " 从 Session 解绑");
        // 可以在这里做：清理资源、记录离线时间等
    }
}

// 使用时无需任何额外代码，对象放入 Session 即自动触发：
session.setAttribute("currentUser", user);  // → valueBound()
session.removeAttribute("currentUser");      // → valueUnbound()
```

### 5.6 HttpSessionActivationListener — 钝化/活化

```java
// 实现 Serializable + HttpSessionActivationListener
public class ShoppingCart implements HttpSessionActivationListener, Serializable {
    private static final long serialVersionUID = 1L;
    private List<Item> items;

    // Session 被钝化到磁盘前调用（服务器正常关闭时）
    @Override
    public void sessionWillPassivate(HttpSessionEvent se) {
        System.out.println("购物车即将被序列化到磁盘");
        // 清理不可序列化的字段（如数据库连接）
    }

    // Session 从磁盘活化后调用（服务器重启完成时）
    @Override
    public void sessionDidActivate(HttpSessionEvent se) {
        System.out.println("购物车从磁盘恢复");
        // 重新初始化不可序列化的字段
    }
}
```

### 5.7 ServletRequestListener

```java
@WebListener
public class RequestLogListener implements ServletRequestListener {

    @Override
    public void requestInitialized(ServletRequestEvent sre) {
        HttpServletRequest req = (HttpServletRequest) sre.getServletRequest();
        req.setAttribute("_requestStart", System.currentTimeMillis());
    }

    @Override
    public void requestDestroyed(ServletRequestEvent sre) {
        HttpServletRequest req = (HttpServletRequest) sre.getServletRequest();
        long start = (long) req.getAttribute("_requestStart");
        long elapsed = System.currentTimeMillis() - start;
        // 慢请求告警
        if (elapsed > 3000) {
            System.err.println("⚠ SLOW: " + req.getRequestURI() + " " + elapsed + "ms");
        }
    }
}
```

### 5.8 八大 Listener 速查表

| 接口 | 事件类型 | 监听对象 | 常用度 |
|------|---------|---------|--------|
| `ServletContextListener` | 生命周期 | 应用启停 | ⭐⭐⭐⭐⭐ |
| `ServletContextAttributeListener` | 属性变更 | Context 属性 | ⭐⭐ |
| `HttpSessionListener` | 生命周期 | Session 创建/销毁 | ⭐⭐⭐⭐ |
| `HttpSessionAttributeListener` | 属性变更 | Session 属性 | ⭐⭐⭐ |
| `HttpSessionBindingListener` | 绑定通知 | 对象自己感知 | ⭐⭐ |
| `HttpSessionActivationListener` | 钝化/活化 | Session 持久化 | ⭐ |
| `ServletRequestListener` | 生命周期 | 请求开始/结束 | ⭐⭐⭐ |
| `ServletRequestAttributeListener` | 属性变更 | Request 属性 | ⭐ |

---

## 6. 观察者模式在 Web 中的应用

```
观察者模式在 Java Web 的三层体现：

┌─────────────────────────────────────────────────┐
│  设计模式层                                       │
│  Observer / Observable → 通用观察者模式            │
├─────────────────────────────────────────────────┤
│  Servlet 规范层                                   │
│  Listener 接口族 — 事件源（Context/Session/Request）│
│  → 监听器（实现对应接口）                           │
├─────────────────────────────────────────────────┤
│  Spring 层                                       │
│  ApplicationEvent / @EventListener → 更灵活       │
│  - ContextRefreshedEvent                         │
│  - RequestHandledEvent                           │
│  - 自定义事件                                     │
└─────────────────────────────────────────────────┘
```

---

## 7. Filter/Listener vs Spring 对应物

| Servlet 规范 | Spring 对应 | 说明 |
|-------------|------------|------|
| `Filter` | `HandlerInterceptor` | Spring 更细粒度（Controller 前后） |
| `ServletContextListener` | `ApplicationListener<ContextRefreshedEvent>` | 启动完成后的回调 |
| `HttpSessionListener` | `HttpSessionEventPublisher` | Spring Security 的并发会话控制 |
| `ServletRequestListener` | `RequestContextListener` | 暴露 Request 到当前线程 |
| `@WebFilter` | `@Component + FilterRegistrationBean` | Spring Boot 注册 Filter |
| `@WebListener` | `@Component + @EventListener` | Spring 注解驱动事件 |

```java
// Spring Boot 中注册 Filter 的三种方式：

// 方式一：@Component（默认拦截 /*）
@Component
public class MyFilter implements Filter { }

// 方式二：FilterRegistrationBean（精确控制顺序和路径）
@Bean
public FilterRegistrationBean<MyFilter> myFilter() {
    FilterRegistrationBean<MyFilter> bean = new FilterRegistrationBean<>();
    bean.setFilter(new MyFilter());
    bean.addUrlPatterns("/api/*");
    bean.setOrder(1);  // 数字越小越先执行
    return bean;
}

// 方式三：@WebFilter + @ServletComponentScan
@SpringBootApplication
@ServletComponentScan  // 开启对 @WebFilter/@WebListener/@WebServlet 的扫描
public class Application { }
```

---

## 8. 常见面试题

### Q1：Filter 和 Interceptor 的区别？

> Filter 是 Servlet 规范，工作在容器层，可拦截所有请求；Interceptor 是 Spring 框架的，工作在 Spring MVC 的 Handler 前后，只能拦截 Controller。Filter 在 Interceptor 之前执行。

### Q2：多个 Filter 的执行顺序如何控制？

> web.xml 按 `<filter-mapping>` 声明顺序；注解按类名字典序（不可靠）；Spring Boot 的 `@Order` 或 `FilterRegistrationBean.setOrder()` 明确指定。详见第2节。

### Q3：Listener 有哪些类型？最常用哪个？

> 三类八种：ServletContext（2种）、HttpSession（4种，含 Binding/Activation）、ServletRequest（2种）。最常用 `ServletContextListener`（初始化资源）和 `HttpSessionListener`（统计在线人数）。详见第5节。

### Q4：如何在 Filter 中获取 Spring Bean？

> 方式一：`WebApplicationContextUtils.getWebApplicationContext(servletContext).getBean(Xxx.class)`。方式二：Spring Boot 中用 `FilterRegistrationBean` 注册，通过构造器注入。方式三：继承 `GenericFilterBean` 或 `OncePerRequestFilter`。

### Q5：Filter 和 Listener 能在同一个地方同时使用吗？

> 可以，各司其职。Filter 拦截请求/响应流，Listener 监听生命周期事件。例如：Listener 初始化连接池 + Filter 从池中获取连接 → 绑定到请求 → 完成后归还。
