# Servlet 详细知识点剖析（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | Servlet 核心知识  
> **核心定位**：Java Web 开发的底层基础组件，Spring MVC 的底层依赖  
> **前置基础**：Java 基础、HTTP 协议

---

## 一、核心概念

### 1.1 什么是 Servlet

Servlet（Server Applet）是 Java EE（现 Jakarta EE）规范定义的 **服务器端 Java 程序**，运行在 Web 容器（Tomcat、Jetty）中，核心作用是接收 HTTP 请求 → 处理业务逻辑 → 生成 HTTP 响应。

### 1.2 核心定位

| 定位 | 说明 |
|------|------|
| **客户端-服务器中间层** | 连接 HTTP 请求与后端业务逻辑 |
| **替代 CGI** | 跨平台、高性能、多线程，避免每请求创建进程 |
| **框架底层基础** | Spring MVC 的 `DispatcherServlet` 基于 Servlet 规范 |

### 1.3 核心特性

- **跨平台**：基于 Java，可运行在所有支持 JVM 的系统
- **多线程**：每个请求分配独立线程，单实例处理多请求
- **单例**：默认单例模式，多线程共享一个实例
- **容器管理**：由 Web 容器负责加载、初始化、销毁

---

## 二、底层原理

### 2.1 Servlet 生命周期

```
加载 → 实例化 → 初始化(init) → 服务(service/doGet/doPost) → 销毁(destroy) → 回收
         ↑              ↑                  ↑                       ↑
      仅一次          仅一次            每次请求                 仅一次
```

| 阶段 | 方法 | 执行次数 | 核心逻辑 |
|------|------|----------|----------|
| **加载** | — | 1 次 | 类加载器加载 Servlet 类 |
| **实例化** | 无参构造 | 1 次 | 反射创建实例（必须有无参构造） |
| **初始化** | `init(config)` | 1 次 | 加载配置、初始化连接池等 |
| **服务** | `service()` → `doGet/doPost()` | N 次 | 处理请求，多线程并发 |
| **销毁** | `destroy()` | 1 次 | 释放资源 |

### 2.2 核心类层次结构

```
Servlet（接口）
  └── GenericServlet（抽象类，实现除 service() 外的所有方法）
        └── HttpServlet（抽象类，HTTP 专用，最常用）
              └── 自定义 Servlet（重写 doGet/doPost）
```

---

## 三、代码实现

### 3.1 核心 API 速查

| 接口/类 | 作用 |
|----------|------|
| `Servlet` | 顶层接口，定义 5 个核心方法 |
| `GenericServlet` | 抽象类，协议无关 |
| **`HttpServlet`** | HTTP 专用，最常用 |
| `ServletConfig` | Servlet 专属配置对象 |
| `ServletContext` | Web 应用全局上下文对象（所有 Servlet 共享） |

### 3.2 自定义 Servlet

```java
/**
 * 自定义 Servlet —— 继承 HttpServlet。
 */
@WebServlet("/hello")
public class HelloServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("text/html;charset=UTF-8");
        resp.getWriter().write("<h1>Hello Servlet!</h1>");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doGet(req, resp);
    }
}
```

### 3.3 web.xml 配置

```xml
<servlet>
    <servlet-name>hello</servlet-name>
    <servlet-class>com.example.HelloServlet</servlet-class>
    <load-on-startup>1</load-on-startup>  <!-- 启动时初始化，值越小越优 -->
</servlet>
<servlet-mapping>
    <servlet-name>hello</servlet-name>
    <url-pattern>/hello</url-pattern>
</servlet-mapping>
```

### 3.4 ServletContext 全局共享

```java
// 设置全局属性
ServletContext context = getServletContext();
context.setAttribute("appName", "MyApp");

// 获取全局属性
String appName = (String) context.getAttribute("appName");
```

---

## 四、实战要点

| 要点 | 说明 |
|------|------|
| **线程安全** | Servlet 单例多线程，避免使用可修改的成员变量 |
| **必须有无参构造** | Web 容器通过反射调用无参构造创建实例 |
| **init 执行时机** | `load-on-startup` ≥ 0 启动时初始化，负数首次请求时 |
| **未重写 doXXX** | 默认返回 405 Method Not Allowed |

### 4.1 `@WebServlet` vs `web.xml`

| 维度 | `@WebServlet`（注解） | `web.xml`（XML） |
|------|----------------------|------------------|
| 简洁度 | 高 | 低 |
| 动态修改 | 需重新编译 | 修改 XML 即可 |
| Servlet 版本 | 3.0+ | 全部版本 |

---

## 五、避坑总结

| 坑点 | 解决 |
|------|------|
| **成员变量线程不安全** | 避免定义可修改的成员变量；若必须使用，加 `synchronized` |
| **无参构造缺失** | 自定义有参构造后必须显式提供无参构造 |
| **destroy 非可靠** | 容器异常崩溃时 `destroy()` 不执行，关键资源不能仅依赖此方法 |
| **doPost 未调用 doGet** | 在 `doPost` 中调用 `doGet(req, resp)` 统一处理 |

---

## 六、企业级最佳实践

- **现代开发直接用 Spring MVC**：Servlet 是底层，日常开发用 `@RestController` 替代
- **理解生命周期**：有助排查 Spring Boot 中 Filter/Servlet 的初始化异常
- **Servlet 3.0+ 注解优先**：`@WebServlet`、`@WebFilter`、`@WebListener` 替代 web.xml
- **线程安全设计**：不用成员变量，使用局部变量或线程安全的并发集合
