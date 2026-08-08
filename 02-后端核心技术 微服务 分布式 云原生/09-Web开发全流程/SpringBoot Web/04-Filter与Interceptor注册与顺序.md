# Filter 与 Interceptor 注册与顺序
> Boot 中 Filter 的三条注册路径、OncePerRequestFilter、拦截器注册与顺序编排，以及 Filter / Interceptor / AOP 三层拦截的选型决策——请求横切能力的完整地图

## 📚 目录
1. [Filter 注册三路径](#1-filter-注册三路径)
2. [OncePerRequestFilter 与经典基类](#2-onceperrequestfilter-与经典基类)
3. [顺序控制：FilterRegistrationBean vs @Order](#3-顺序控制filterregistrationbean-vs-order)
4. [HandlerInterceptor 注册与顺序](#4-handlerinterceptor-注册与顺序)
5. [Filter / Interceptor / AOP 三层分工](#5-filter--interceptor--aop-三层分工)
6. [典型组合场景](#6-典型组合场景)

## 1. Filter 注册三路径

```java
// ═══ 路径一：FilterRegistrationBean（Boot 推荐，可注入 + 可排序）═══
@Configuration
public class FilterConfig {
    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilter() {
        FilterRegistrationBean<TraceIdFilter> bean =
                new FilterRegistrationBean<>(new TraceIdFilter());
        bean.addUrlPatterns("/*");                              // 拦截范围
        bean.setOrder(1);                                       // 顺序：越小越靠前
        return bean;
    }

    @Bean
    public FilterRegistrationBean<LoginFilter> loginFilter() {
        FilterRegistrationBean<LoginFilter> bean =
                new FilterRegistrationBean<>(new LoginFilter());
        bean.addUrlPatterns("/api/*");
        bean.setOrder(2);
        return bean;
    }
}

// ═══ 路径二：@WebFilter + @ServletComponentScan（注解扫描，无法排序）═══
@WebFilter(urlPatterns = "/api/*", filterName = "apiLog")
public class ApiLogFilter implements Filter { }

@ServletComponentScan(basePackages = "com.demo.web.filter")   // 启动类上
@SpringBootApplication
public class App { }

// ═══ 路径三：Filter 直接注册为 Bean（Boot 自动包装）═══
@Bean
public SomeFilter someFilter() {          // Boot 检测到 Filter 类型 Bean
    return new SomeFilter();              // 自动包装为 FilterRegistrationBean
}                                          // 顺序用 @Order 注解控制
```

| 路径 | 优点 | 缺点 | 适用 |
|------|------|------|------|
| FilterRegistrationBean | 可注入、可排序、可条件化 | 代码多一层 | **生产首选** |
| @WebFilter + 扫描 | 声明式、就近 | 无法注入、**无法排序** | 简单场景/遗留代码 |
| 直接 @Bean | 简洁 | 顺序控制弱（@Order） | 单体 Filter |

> ⚠️ **路径二的致命短板**：@WebFilter 的 Filter 是容器直接实例化（非 Spring Bean），**不能注入依赖**；且多个 @WebFilter 顺序不确定。需要依赖或顺序的 Filter 一律走路径一。

## 2. OncePerRequestFilter 与经典基类

```java
// 标准基类：保证一次请求只执行一次
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        MDC.put("traceId", traceId);                 // 日志上下文
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("traceId");                   // 必清：线程池复用防串号
        }
    }
}
```

> 🎯 为什么是 OncePerRequestFilter：**转发/异步请求会让 Filter 执行多次**（REQUEST/ASYNC/ERROR 各过一次链），基类内部用"已执行标记"保证只跑一遍业务逻辑；`shouldNotFilter` 可按请求跳过（如静态资源）。

| 经典场景 | 放 Filter（OncePerRequestFilter） | 原因 |
|---------|-------------------------------|------|
| traceId 注入 | ✅ | 必须拦最早（Filter 在最外层） |
| 请求体日志 | ✅ | 需要包装 Request（Body 单次读） |
| 登录校验 | ✅/Interceptor | 见 §5 决策表 |
| 响应耗时统计 | ✅ | 包住整个链路 |

## 3. 顺序控制：FilterRegistrationBean vs @Order

```text
Filter 顺序决定因素（优先级从高到低）：
  ① FilterRegistrationBean.setOrder() 显式指定
  ② @Order 注解（仅"直接 @Bean"路径生效）
  ③ 无顺序 → 注册顺序不确定（勿依赖！）
```

| 方式 | 生效范围 | 推荐 |
|------|---------|------|
| `bean.setOrder(1)` | 路径一专属 | ✅ 首选 |
| `@Order(1)` | 路径三（Bean） | 次选 |
| 无 | 路径二（注解） | ❌ 顺序随机 |

```java
// 生产铁律：所有有顺序要求的 Filter 统一走 FilterRegistrationBean 并编号
// 1 = traceId（最外层）
// 2 = 编码（框架已有 CharacterEncodingFilter，一般无需自定义）
// 3 = CORS
// 4 = 登录鉴权
// 5 = 限流
```

> ⚠️ 与容器内置 Filter 的协调：Boot 内置 Filter（CharacterEncodingFilter 等）的默认顺序见 `OrderedCharacterEncodingFilter`（`Ordered.HIGHEST_PRECEDENCE` 附近）——自定义 Filter 想比它们更外/更内，用 `setOrder` 显式越过即可。

## 4. HandlerInterceptor 注册与顺序

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 顺序 = 注册顺序（无 @Order 机制）
        registry.addInterceptor(new TraceIdInterceptor());     // 1
        registry.addInterceptor(new LoginInterceptor())        // 2
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/login");
    }
}
```

```text
一次带 2 个拦截器的请求：
preHandle 顺序：TraceId → Login（注册顺序）
postHandle 逆序：Login → TraceId
afterCompletion 逆序：Login → TraceId
```

| 拦截器与 Filter 的时序总览 | |
|------|------|
| 整体位置 | Filter（容器层）→ DispatcherServlet → Interceptor（MVC 层）→ Controller |
| Filter 能看到 | 所有请求（含静态资源、错误页） |
| Interceptor 能看到 | 已路由到 Handler 的请求 + HandlerMethod 对象 |

> 💡 Interceptor 想拿 HandlerMethod 做精细化控制（如方法级注解鉴权）是 Filter 做不到的——这就是 `@RequiresPermission` 类注解拦截器存在的理由。

## 5. Filter / Interceptor / AOP 三层分工

| 维度 | Filter | HandlerInterceptor | AOP |
|------|:---:|:---:|:---:|
| 层 | 容器（Servlet 规范） | MVC 框架 | Spring 容器 |
| 时机 | 进 Servlet 前/响应后 | Controller 前/后/完成 | 目标方法调用前后（任意 Bean） |
| 能拦 | 一切请求 | Controller 映射请求 | Spring Bean 方法 |
| 访问 | ServletRequest/Response | HandlerMethod、ModelAndView | 方法参数、返回值、注解 |
| 依赖注入 | 需 FilterRegistrationBean | ✅ 天然 Bean | ✅ 天然 Bean |
| 异常可见 | 部分（dispatch 时） | ✅ afterCompletion | ✅ @AfterThrowing |
| 典型场景 | 编码/traceId/CORS/压缩 | 鉴权/日志/参数校验 | 事务/审计/限流注解 |

### 选型决策树

```text
需要拦截"所有请求"（含静态资源）？ ── 是 → Filter
需要拿到 HandlerMethod / ModelAndView？ ── 是 → Interceptor
需要切"Spring Bean 方法"（含 Service 层）？ ── 是 → AOP
只是登录校验 + 能控制范围 → Interceptor（首选）
```

> 🎯 经验结论：**鉴权用 Interceptor（范围可控 + 能拿 HandlerMethod）、traceId/编码用 Filter（必须最外）、事务/审计用 AOP（切方法）**——三者不是竞争关系，是分工。与 [Filter 体系对比章节](../Filter%20过滤器/05-Filter-vs-Interceptor-vs-AOP.md) 结论一致，本体系补 Boot 侧注册细节。

## 6. 典型组合场景

### 场景一：全链路 traceId + 登录鉴权 + 接口耗时

```text
Filter（traceId，/*，order=1）
  └─ Filter（登录鉴权，/api/*，order=2）        ← 或放 Interceptor（范围更精细）
       └─ Interceptor（耗时统计，/api/**）      ← preHandle 记开始、afterCompletion 记耗时
            └─ AOP（@LogAudit 审计注解，切 Service）
```

### 场景二：请求体日志（Filter 包装 + 拦截器补充）

```java
// Filter：包装 Request 实现 Body 单次读 + 记录出入参
public class RequestLogFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(...) {
        ContentCachingRequestWrapper requestWrapper =
                new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper =
                new ContentCachingResponseWrapper(response);
        filterChain.doFilter(requestWrapper, responseWrapper);
        // 日志：requestWrapper.getContentAsByteArray() / responseWrapper...
        responseWrapper.copyBodyToResponse();   // ⚠️ 包装类必须回写，否则客户端收不到响应！
    }
}
```

> ⚠️ 请求/响应包装的两个铁律：**① 包装类要一路传下去**（后续组件必须继续用包装对象）；**② ContentCachingResponseWrapper 必须 copyBodyToResponse()**——否则响应被吞，客户端拿到空响应（高频故障）。

---

**下一模块**：[05-错误处理机制深潜](05-错误处理机制深潜.md) / **返回总览**：[00-SpringBootWeb总览](00-SpringBootWeb总览.md)
