# 01 - Tomcat 架构与请求处理流程

> 定位：Catalina 容器层级、请求处理九步流程、Servlet 规范三组件、生命周期——Tomcat 面试第一核心

## 📚 目录

1. [整体架构：Connector + Container](#1-整体架构connector--container)
2. [Container 层级：Engine/Host/Context/Wrapper](#2-container-层级enginehostcontextwrapper)
3. [请求处理完整流程](#3-请求处理完整流程)
4. [Servlet 规范三组件](#4-servlet-规范三组件)
5. [生命周期管理](#5-生命周期管理)

---

## 1. 整体架构：Connector + Container

```
Tomcat 两大核心组件：
  Connector（连接器）：接收请求、解析协议（Coyote）
  Container（容器）：处理请求、执行 Servlet（Catalina）

请求路径：
  客户端 → Connector（解析 HTTP）→ Container（执行逻辑）→ 响应返回

⚠️ 面试必答：
"Tomcat = Connector + Container 两层——
 Connector 管'通信'（HTTP 解析/IO 模型）、
 Container 管'业务'（Servlet 执行）。
 一个 Container 可配多个 Connector（HTTP/HTTPS/AJP）。"
```

```
┌─────────────────────────────────────────┐
│                Tomcat                    │
│  ┌────────────┐    ┌────────────────┐    │
│  │ Connector  │    │   Catalina     │    │
│  │ (Coyote)   │───▶│   Container    │    │
│  │ HTTP/HTTPS │    │  Engine→Host   │    │
│  │ AJP        │    │  →Context→Wrap │    │
│  └────────────┘    └────────────────┘    │
└─────────────────────────────────────────┘
```

---

## 2. Container 层级：Engine/Host/Context/Wrapper

### 2.1 四级容器

| 层级 | 作用 | 对应配置 |
|------|------|---------|
| **Engine** | 引擎（最顶层容器） | `<Engine>`（唯一） |
| **Host** | 虚拟主机（域名隔离） | `<Host name="example.com">` |
| **Context** | 应用（一个 Web 应用） | `<Context path="/app">` |
| **Wrapper** | Servlet 包装 | 每个 Servlet 一个 |

```
层级关系：
  Engine
   └─ Host（虚拟主机，可多个）
       └─ Context（Web 应用，可多个）
           └─ Wrapper（Servlet，可多个）

⚠️ 面试必答：
"四级容器从大到小——Engine（引擎）、
 Host（域名）、Context（应用）、
 Wrapper（Servlet）；请求按层级
 逐级下传找到目标 Servlet。"
```

### 2.2 请求定位过程

```
请求 /app/login：
  ① Engine：接受所有请求
  ② Host：按域名匹配（localhost）
  ③ Context：按路径前缀匹配（/app）
  ④ Wrapper：按 Servlet 映射匹配（/login → LoginServlet）
```

---

## 3. 请求处理完整流程

### 3.1 九步流程（面试必背）

```
① 客户端发送 HTTP 请求
② Connector 的 Acceptor 线程接收连接
③ 解析请求（请求行/头/体）→ 封装 Request/Response 对象
④ 交给 Container：Engine → Host → Context → Wrapper
⑤ 经过 Pipeline 的 Valve 链（拦截器）
⑥ 执行应用 Filter 链（过滤器）
⑦ 调用 Servlet.service() → doGet/doPost
⑧ 响应沿原路径返回
⑨ Connector 发送响应给客户端

⚠️ 面试必答：
"请求九步——连接接收、解析封装、
 容器定位、Valve 拦截、Filter 链、
 Servlet 执行、响应返回。
 核心记忆：Connector 收 → 四级容器定位
 → Valve/Filter → Servlet。"
```

### 3.2 类加载与单实例

```
⚠️ Servlet 是单实例多线程：
  每个 Servlet 只实例化一次
  多个线程并发调用 service()

⚠️ 面试必答：
"Servlet 单实例多线程——线程安全问题
 在 Servlet 内共享字段（用局部变量/
 ThreadLocal）。实例化一次、服务多次。"
```

---

## 4. Servlet 规范三组件

### 4.1 Servlet

```java
// Servlet 生命周期方法
public class HelloServlet extends HttpServlet {
    // ① init：初始化（一次）
    @Override public void init() { }

    // ② service：服务（每次请求，多线程并发）
    @Override protected void doGet(HttpServletRequest req,
                                   HttpServletResponse resp) {
        resp.setContentType("text/html;charset=UTF-8");
        resp.getWriter().write("<h1>你好</h1>");
    }

    // ③ destroy：销毁（一次）
    @Override public void destroy() { }
}
```

### 4.2 Filter（过滤器）

```java
// 过滤器：请求预处理/响应后处理（登录校验/日志/编码）
public class AuthFilter implements Filter {
    @Override public void doFilter(ServletRequest req,
                                   ServletResponse resp,
                                   FilterChain chain) {
        HttpServletRequest request = (HttpServletRequest) req;
        // ① 请求前逻辑（校验/编码）
        if (request.getSession().getAttribute("user") == null) {
            ((HttpServletResponse) resp).sendRedirect("/login");
            return;                        // 拦截
        }
        // ② 放行到下一个 Filter/Servlet
        chain.doFilter(req, resp);
        // ③ 响应后逻辑
    }
}
```

### 4.3 Listener（监听器）

```java
// 监听器：监听事件（应用启动/会话创建/属性变化）
public class AppListener implements ServletContextListener {
    @Override public void contextInitialized(ServletContextEvent sce) {
        // 应用启动：初始化数据源/加载配置
    }
    @Override public void contextDestroyed(ServletContextEvent sce) {
        // 应用关闭：清理资源
    }
}
```

| 组件 | 时机 | 典型用途 |
|------|------|---------|
| Servlet | 请求处理 | 业务逻辑 |
| Filter | 请求前后 | 鉴权、日志、编码、压缩 |
| Listener | 生命周期事件 | 初始化、清理、会话管理 |

> 🎯 **要点**：三组件分工——Servlet 管业务、Filter 管拦截（链式）、Listener 管事件。Filter 链顺序 = web.xml 声明顺序。

---

## 5. 生命周期管理

### 5.1 Servlet 生命周期

```
加载 → 实例化 → init（一次）→ service（多次）→ destroy（一次）

⚠️ 触发时机：
  init：首次请求时（或 load-on-startup 提前）
  destroy：应用停止/热部署时

⚠️ 面试必答：
"Servlet 生命周期五阶段——加载、实例化、
 init（一次）、service（每次请求）、
 destroy（一次）。单实例多线程是关键特征。"
```

### 5.2 应用生命周期（Listener 视角）

```
应用部署 → contextInitialized（初始化）
   → 请求服务
   → contextDestroyed（销毁）

⚠️ 面试必答：
"应用生命周期与 ServletContextListener
 挂钩——初始化放数据源/配置、
 销毁放清理（连接池关闭）。"
```

---

> 🎯 **核心要点**：Tomcat 架构 = **Connector + Container 双层**（通信 vs 业务）+ **四级容器**（Engine→Host→Context→Wrapper）+ **九步请求流程**（收→解析→定位→拦截→执行）+ **三组件**（Servlet/Filter/Listener）+ **生命周期**（init 一次/service 多次）。"请求怎么走"是 Tomcat 面试第一问。

---

**返回总览**：[00-Tomcat总览与核心概念](00-Tomcat总览与核心概念.md) | **下一篇**：[02-Tomcat配置文件详解](02-Tomcat配置文件详解.md)
