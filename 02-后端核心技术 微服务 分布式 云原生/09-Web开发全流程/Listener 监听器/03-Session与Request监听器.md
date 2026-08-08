# 03-Session 与 Request 监听器
> 会话创建/销毁监听（在线统计）、请求监听（访问日志）、属性监听（数据变化）——"监听器的高频业务场景"

## 📚 目录
1. [HttpSessionListener：在线人数统计](#1-httpsessionlistener在线人数统计)
2. [会话销毁的触发时机](#2-会话销毁的触发时机)
3. [ServletRequestListener：访问日志](#3-servletrequestlistener访问日志)
4. [属性监听器：数据变化感知](#4-属性监听器数据变化感知)
5. [综合实战：在线用户管理](#5-综合实战在线用户管理)
6. [核心要点](#6-核心要点)
7. [参考来源](#7-参考来源)

## 1. HttpSessionListener：在线人数统计

```java
// 在线人数统计（经典场景）
@WebListener
public class OnlineUserListener implements HttpSessionListener {

    // 在线人数（用 context 存储，全局共享）
    private static final String ONLINE_KEY = "onlineCount";

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        // 会话创建：在线 +1
        ServletContext context = se.getSession().getServletContext();
        Integer count = (Integer) context.getAttribute(ONLINE_KEY);
        context.setAttribute(ONLINE_KEY, (count == null ? 0 : count) + 1);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        // 会话销毁：在线 -1
        ServletContext context = se.getSession().getServletContext();
        Integer count = (Integer) context.getAttribute(ONLINE_KEY);
        context.setAttribute(ONLINE_KEY, Math.max(0, (count == null ? 0 : count) - 1));
    }
}
```

```jsp
<%-- 页面显示在线人数 --%>
在线人数：<%= application.getAttribute("onlineCount") %>
```

> 🎯 **在线统计的原理**：**会话创建 +1、销毁 -1**——但注意"会话 ≠ 在线用户"（同一用户多个会话）；精确统计需结合用户维度（见第 5 节）。

## 2. 会话销毁的触发时机

```text
sessionDestroyed 触发时机：
  ① 超时：超过 maxInactiveInterval（30 分钟无活动）
  ② 主动失效：session.invalidate()（登出）
  ③ 服务器关闭：全部会话销毁

⚠️ 不触发：浏览器关闭（只删 Cookie，服务器 Session 等超时）
```

| 销毁方式 | 触发监听 | 场景 |
|----------|:---:|------|
| 超时 | ✅ | 自然过期 |
| invalidate（登出） | ✅ | 主动登出 |
| 浏览器关闭 | ❌（等超时） | 需等待 |
| 服务器关闭 | ✅ | 全量销毁 |

> ⚠️ **在线统计的误差来源**：浏览器关闭不立即触发销毁——在线数会"虚高"直到超时；**会话超时时间越长，统计越滞后**（联动 [会话体系](../Cookie%20%26%20Session（会话技术）/00-会话技术总览.md)）。

## 3. ServletRequestListener：访问日志

```java
// 请求监听：访问日志 + 耗时统计
@WebListener
public class AccessLogListener implements ServletRequestListener {

    @Override
    public void requestInitialized(ServletRequestEvent sre) {
        HttpServletRequest request = (HttpServletRequest) sre.getServletRequest();
        // 记录请求开始时间（放入 request 属性）
        request.setAttribute("startTime", System.currentTimeMillis());
    }

    @Override
    public void requestDestroyed(ServletRequestEvent sre) {
        HttpServletRequest request = (HttpServletRequest) sre.getServletRequest();
        long start = (Long) request.getAttribute("startTime");
        long cost = System.currentTimeMillis() - start;

        System.out.printf("访问: %s %s 耗时=%dms%n",
                request.getMethod(), request.getRequestURI(), cost);
    }
}
```

| 请求监听用途 | 说明 |
|--------------|------|
| 访问日志 | 全量记录（比 Filter 更靠近容器） |
| 耗时统计 | 开始/结束时间差 |
| 请求计数 | QPS 统计 |
| 资源清理 | 请求结束清理（ThreadLocal 等） |

> 💡 **Listener vs Filter 做日志**：RequestListener 是"容器事件级"（更底层）；Filter 能控制放行——**日志统计用哪个都行，Filter 更常用**（可结合拦截逻辑）。

## 4. 属性监听器：数据变化感知

```java
// 登录状态变化感知（Session 属性监听）
@WebListener
public class LoginAttrListener implements HttpSessionAttributeListener {

    @Override
    public void attributeAdded(HttpSessionBindingEvent se) {
        if ("user".equals(se.getName())) {
            System.out.println("用户登录: " + se.getValue());
            // 登录审计/通知
        }
    }

    @Override
    public void attributeRemoved(HttpSessionBindingEvent se) {
        if ("user".equals(se.getName())) {
            System.out.println("用户登出: " + se.getValue());
        }
    }

    @Override
    public void attributeReplaced(HttpSessionBindingEvent se) {
        if ("user".equals(se.getName())) {
            System.out.println("用户切换: " + se.getValue());
        }
    }
}
```

| 属性监听场景 | 说明 |
|--------------|------|
| 登录/登出感知 | 监听 user 属性增删 |
| 审计 | 属性变化记录 |
| 同步 | 属性变化触发其他动作 |

> 🎯 **属性监听的经典用途**：**监听 Session 的 user 属性变化实现"登录/登出事件"**——传统 JavaWeb 的"登录事件"机制（Spring Security 有更完善方案）。

## 5. 综合实战：在线用户管理

```java
// 在线用户管理（会话 + 属性监听配合）
@WebListener
public class OnlineUserListener
        implements HttpSessionListener, HttpSessionAttributeListener {

    // 在线用户：userId → 会话
    private static final String ONLINE_USERS = "onlineUsers";

    @Override
    public void attributeAdded(HttpSessionBindingEvent se) {
        if ("user".equals(se.getName())) {
            // 登录：加入在线列表
            String userId = String.valueOf(se.getValue());
            ServletContext ctx = se.getSession().getServletContext();
            Map<String, String> online = getOnlineMap(ctx);
            online.put(userId, se.getSession().getId());
            ctx.setAttribute(ONLINE_USERS, online);
        }
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        // 会话销毁：从在线列表移除
        ServletContext ctx = se.getSession().getServletContext();
        Map<String, String> online = getOnlineMap(ctx);
        online.values().removeIf(id -> id.equals(se.getSession().getId()));
        ctx.setAttribute(ONLINE_USERS, online);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getOnlineMap(ServletContext ctx) {
        Map<String, String> online = (Map<String, String>) ctx.getAttribute(ONLINE_USERS);
        if (online == null) {
            online = new ConcurrentHashMap<>();
        }
        return online;
    }
}
```

> ⚠️ **并发安全**：全局在线列表必须用 `ConcurrentHashMap`（多会话并发登录）——Listener 是并发回调，共享数据线程安全是必修课。

## 6. 核心要点

> 🎯 **核心要点**：
> - HttpSessionListener：在线统计（创建 +1、销毁 -1）——注意"会话≠用户"与统计滞后；
> - 销毁时机：超时/invalidate/服务器关闭；**浏览器关闭不立即触发**；
> - ServletRequestListener：访问日志/耗时（比 Filter 更底层）；
> - 属性监听：监听 user 属性实现登录/登出感知（传统 JavaWeb 的登录事件）；
> - 并发纪律：全局共享数据用 ConcurrentHashMap；
> - 现代替代：Spring Security 会话管理/Spring 事件更完善（Listener 用于传统项目）。

## 7. 参考来源

- [Jakarta Servlet 规范（Session 监听）](https://jakarta.ee/specifications/servlet/)
- [Baeldung：Servlet Session Listeners](https://www.baeldung.com/java-servlet-listeners)

---

**下一模块**：[04-SpringBoot注册Listener](04-SpringBoot注册Listener.md)　/　**返回总览**：[00-总览](00-Listener监听器总览.md)
