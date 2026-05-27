# Spring 拦截器与过滤器详解

## Filter（过滤器）

属于 Servlet 规范，由 Servlet 容器（Tomcat）管理，不依赖 Spring。

**执行时机**：请求进入 Servlet 之前、响应返回客户端之前。

```java
@WebFilter(urlPatterns = "/*")
public class AuthFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        // 前置处理：如记录请求日志、校验 Token
        System.out.println("Request: " + request.getRequestURI());
        
        chain.doFilter(req, res);  // 放行
        
        // 后置处理
    }
}
```

**典型场景：**
- 字符编码设置（`CharacterEncodingFilter`）
- 请求日志记录
- XSS 防护、SQL 注入过滤
- CORS 处理
- 简单限流（IP 级别）

## Interceptor（拦截器）

Spring MVC 提供，由 Spring 容器管理，可注入 Bean。

```java
@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        // 在 Controller 方法执行前调用
        // 返回 false 则中断请求
        String token = request.getHeader("Authorization");
        if (token == null) {
            throw new UnauthorizedException("未登录");
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           ModelAndView modelAndView) {
        // Controller 方法执行后，视图渲染前
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler, Exception ex) {
        // 视图渲染完毕后（无论是否异常都会执行）
        // 适合做资源清理
    }
}
```

注册拦截器：
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")       // 拦截
                .excludePathPatterns("/api/auth/login", "/api/public/**"); // 放行
    }
}
```

**典型场景：**
- 登录认证 + 权限校验
- 操作日志（记录谁在什么时候做了什么）
- 请求耗时统计
- 统一参数处理（如用户信息注入）
- 接口防刷

## 两者对比

| 维度 | Filter | Interceptor |
|------|--------|-------------|
| 规范 | Servlet 规范 | Spring MVC |
| 容器管理 | Servlet 容器 | Spring IOC |
| 能否注入 Bean | 不能直接注入 | 可以 |
| 作用范围 | 所有请求（含静态资源） | 仅 Controller 请求 |
| 执行顺序 | 先执行 | 后执行 |
| 控制粒度 | 只能控制是否放行 | 可精确到 Controller + 方法 |
| 能否获取 handler | 不能 | 能（哪个方法被调用） |

## 执行顺序

```
请求进入
  → Filter 链（按照 order 排序）
    → DispatcherServlet
      → Interceptor.preHandle()
        → Controller 方法
      → Interceptor.postHandle()
      → 视图渲染
      → Interceptor.afterCompletion()
  → Filter 链返回
→ 响应给客户端
```

## 异常处理差异

- **Filter** 中的异常会被容器捕获，不会进入 Spring 的 `@RestControllerAdvice`。需要自己在 Filter 内 try-catch 并写入 response。
- **Interceptor** 中的异常会被 Spring 的异常处理器捕获，更推荐在 Interceptor 层做业务拦截。

## 实战建议

**用 Filter 处理：**
- 编码、请求体包装（`ContentCachingRequestWrapper`）
- 全量请求日志
- CORS 配置

**用 Interceptor 处理：**
- 登录态校验
- 权限控制
- 请求耗时埋点
- 用户上下文注入（`UserContextHolder`）

两者各司其职，不要混用。Filter 做基础设施，Interceptor 做业务拦截。
