# 03-常用 Filter 场景
> 六大高频场景：登录校验、请求日志、字符编码、CORS、限流、XSS 过滤——"Filter 最能体现价值的六个地方"

## 📚 目录
1. [场景一：登录校验 Filter](#1-场景一登录校验-filter)
2. [场景二：请求日志 Filter](#2-场景二请求日志-filter)
3. [场景三：字符编码 Filter](#3-场景三字符编码-filter)
4. [场景四：CORS Filter](#4-场景四cors-filter)
5. [场景五：限流 Filter](#5-场景五限流-filter)
6. [场景六：XSS 过滤 Filter](#6-场景六xss-过滤-filter)
7. [核心要点](#7-核心要点)
8. [参考来源](#8-参考来源)

## 1. 场景一：登录校验 Filter

```java
// 登录校验：未登录拦截（最经典场景）
public class LoginFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // ① 白名单（登录/静态资源放行）
        String uri = req.getRequestURI();
        if (uri.startsWith("/login") || uri.startsWith("/static/")) {
            chain.doFilter(request, response);      // 放行
            return;
        }

        // ② 登录判断（Session 中是否有用户）
        HttpSession session = req.getSession(false);
        Object user = session == null ? null : session.getAttribute("user");

        if (user == null) {
            // ③ 未登录：拦截（API 返回 401，页面重定向）
            if (uri.startsWith("/api/")) {
                resp.setStatus(401);
                resp.getWriter().write("{\"code\":401,\"msg\":\"未登录\"}");
            } else {
                resp.sendRedirect("/login.html");
            }
            return;                                // 不调用 chain.doFilter = 拦截
        }

        chain.doFilter(request, response);          // 已登录：放行
    }
}
```

| 要点 | 说明 |
|------|------|
| 白名单先行 | 登录/静态资源放行 |
| `getSession(false)` | 不创建新 Session |
| API vs 页面 | 401 响应 vs 重定向 |
| 不调 chain | 拦截（401/重定向） |

> 🎯 **登录 Filter 是"拦截"的教科书**：白名单放行 + 未登录拦截（不调 chain.doFilter）+ 已登录放行——与 [会话技术](../Cookie%20%26%20Session（会话技术）/00-会话技术总览.md) 体系联动。

## 2. 场景二：请求日志 Filter

```java
// 请求日志：耗时 + 方法 + URI + 状态码
public class LogFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;

        long start = System.currentTimeMillis();
        chain.doFilter(request, response);          // 放行（等业务完成）
        long cost = System.currentTimeMillis() - start;

        // 后置：日志（此时状态码已确定）
        log.info("{} {} 状态={} 耗时={}ms",
                req.getMethod(), req.getRequestURI(),
                ((HttpServletResponse) response).getStatus(), cost);
    }
}
```

| 日志字段 | 说明 |
|----------|------|
| 方法 + URI | 请求标识 |
| 状态码 | 后置获取 |
| 耗时 | 全链路（含 Filter 链） |
| 用户/IP | 审计（配合 X-Forwarded-For） |

> 💡 日志 Filter 放**最外层**（order 最小）——耗时统计才包含整个 Filter 链；配合监控体系做慢请求定位（见[日志监控指标](../日志监控指标/00-日志监控指标总览.md)）。

## 3. 场景三：字符编码 Filter

```java
// 字符编码：解决乱码（框架已内置，了解原理）
public class EncodingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        request.setCharacterEncoding("UTF-8");      // 请求解码
        response.setCharacterEncoding("UTF-8");     // 响应编码
        response.setContentType("text/html;charset=UTF-8");
        chain.doFilter(request, response);
    }
}
```

> 💡 **框架现状**：Spring Boot 内置 `CharacterEncodingFilter`（默认 UTF-8）——**自定义编码 Filter 一般不需要**；理解原理即可（乱码时检查它是否被禁用/顺序被破坏）。

## 4. 场景四：CORS Filter

```java
// CORS：跨域处理（前后端分离场景）
public class CorsFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletResponse resp = (HttpServletResponse) response;

        resp.setHeader("Access-Control-Allow-Origin", "*");          // 或具体域名
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.setHeader("Access-Control-Max-Age", "3600");

        // 预检请求直接返回（不进入业务）
        if ("OPTIONS".equalsIgnoreCase(((HttpServletRequest) request).getMethod())) {
            resp.setStatus(204);
            return;
        }
        chain.doFilter(request, response);
    }
}
```

> 🎯 **CORS 三件套**：Allow-Origin + Allow-Methods + 预检 OPTIONS 直接 204——与 [Nginx 体系](../Nginx%20基础/05-HTTP模块与rewrite.md) 的 CORS 配置同理（网关层做 vs 应用层做，二选一避免重复）。

## 5. 场景五：限流 Filter

```java
// 简单 IP 限流（分布式场景用 Redis 计数）
public class RateLimitFilter implements Filter {

    // 简化：内存计数（生产用 Redis + 令牌桶）
    private final ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();
    private static final int MAX_PER_MINUTE = 60;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        String ip = request.getRemoteAddr();
        AtomicInteger counter = counters.computeIfAbsent(ip, k -> new AtomicInteger());

        if (counter.incrementAndGet() > MAX_PER_MINUTE) {
            ((HttpServletResponse) response).setStatus(429);       // 限流
            response.getWriter().write("{\"code\":429,\"msg\":\"请求过于频繁\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
```

> ⚠️ **限流 Filter 的定位**：应用层限流（简单/单机）；**生产级限流在网关层**（Nginx limit_req / Sentinel）——应用层 Filter 限流适合"接口级"简单场景（详见 [Sentinel 体系](../../../06-Spring全家桶/Spring组件汇总/Spring%20Cloud%20微服务全家桶（分布式）/Spring%20Cloud%20Alibaba（阿里实现，属于%20Spring%20Cloud%20生态实现）/Sentinel：流量控制熔断降级/00-Sentinel总览.md)）。

## 6. 场景六：XSS 过滤 Filter

```java
// XSS 过滤：拦截恶意脚本（简化版）
public class XssFilter implements Filter {

    // 过滤危险关键字
    private static final Pattern XSS_PATTERN = Pattern.compile(
            "<script|javascript:|onerror=|onclick=", Pattern.CASE_INSENSITIVE);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        // 检查参数（简化）
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String value = request.getParameter(names.nextElement());
            if (value != null && XSS_PATTERN.matcher(value).find()) {
                ((HttpServletResponse) response).setStatus(403);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
```

> 💡 **XSS 防护的完整体系**：Filter 过滤（输入）+ 输出编码（框架默认）+ HttpOnly（Cookie，见会话体系）+ CSP——**Filter 是其中一环，不是全部**。

## 7. 核心要点

> 🎯 **核心要点**：
> - 六大场景：登录（拦截教科书）、日志（最外层统计）、编码（框架内置）、CORS（预检 204）、限流（429）、XSS（输入过滤）；
> - 登录 Filter 三动作：白名单放行、未登录拦截（不调 chain）、已登录放行；
> - 日志 Filter 放最外层（order 最小）才统计全链路；
> - CORS/限流：网关层与应用层二选一（避免重复）；
> - XSS 是体系不是单点：Filter + 编码 + HttpOnly + CSP；
> - 每个 Filter 单一职责 + 顺序明确（见 02/04）。

## 8. 参考来源

- [Jakarta Servlet 规范（Filter 示例）](https://jakarta.ee/specifications/servlet/)
- [OWASP：XSS 防护](https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html)
- [Spring Boot CORS 文档](https://docs.spring.io/spring-boot/reference/web/reactive.html)

---

**下一模块**：[04-SpringBoot注册Filter](04-SpringBoot注册Filter.md)　/　**返回总览**：[00-总览](00-Filter过滤器总览.md)
