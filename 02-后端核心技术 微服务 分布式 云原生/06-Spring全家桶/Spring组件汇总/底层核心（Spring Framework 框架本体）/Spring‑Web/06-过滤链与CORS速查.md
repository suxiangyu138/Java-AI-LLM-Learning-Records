# 06 过滤链与 CORS 速查

> spring-web 内建过滤器、Filter 排序机制、CORS 配置与 PreFlightRequestFilter（7.1）——"请求进来先过谁"的答案

---

## 📚 目录

1. [Servlet Filter vs Spring 过滤器](#1-servlet-filter-vs-spring-过滤器)
2. [spring-web 内建过滤器](#2-spring-web-内建过滤器)
3. [过滤器排序与注册](#3-过滤器排序与注册)
4. [CORS：跨域配置速查](#4-cors跨域配置速查)
5. [CORS 与安全组件配合](#5-cors-与安全组件配合)

---

## 1. Servlet Filter vs Spring 过滤器

```text
请求链路（Servlet 栈）：
  Servlet 容器 → Filter 链（Servlet Filter，原生）
              → DispatcherServlet（Spring 入口）
                  → HandlerInterceptor（Spring 层，见 MVC 系列）

  spring-web 提供的是"Servlet 层面的 Spring 过滤器"：
  实现 jakarta.servlet.Filter + Spring 管理（Ordered/Bean）
  → 在 DispatcherServlet 之前执行（比拦截器更早）
```

| 维度 | Servlet Filter | HandlerInterceptor |
|------|---------------|-------------------|
| 层面 | Servlet API（spring-web 提供） | Spring MVC（webmvc 模块） |
| 执行点 | DispatcherServlet 之前 | 处理器调用前后 |
| 能力 | 拦截所有请求（含静态资源） | 只拦映射到控制器的 |
| 典型 | 编码、CORS、日志 | 鉴权、审计、性能 |
| 顺序 | Order 值 | MappedInterceptor 顺序 |

> 🎯 **核心要点**：**过滤器比拦截器早且广**——跨域/编码等"协议级"处理必须过滤器（静态资源也要过）；业务级鉴权/审计用拦截器或 Security 过滤器链（更后）。

### 1.1 Filter 链的完整链路与注册方式

```text
请求的完整过滤器经历（Servlet 栈）：
  Tomcat 连接器 → 容器过滤器链（按注册顺序）
    → CharacterEncodingFilter（编码）
    → CorsFilter / PreFlightRequestFilter（跨域）
    → Spring Security FilterChainProxy（安全）
    → 自定义业务过滤器
    → RequestContextFilter（线程绑定，可选）
  → DispatcherServlet（Spring 入口，之后是拦截器/控制器）

过滤器是 Servlet 规范的原生机制：
  不依赖 Spring 也能跑（web.xml 或注解注册）
  Spring 提供的是"Bean 化 + 排序"的集成层
```

| 注册方式 | 写法 | 适用 |
|----------|------|------|
| `@Component` + Filter 接口 | 最简（Boot 自动注册） | 无顺序要求 |
| `FilterRegistrationBean` | 显式排序 + 路径 | 生产标准姿势 |
| `@WebFilter` + `@ServletComponentScan` | Servlet 注解 | 遗留代码 |
| 编程式 `ServletContextInitializer` | Boot 内注册 | 需要动态决定 |

> 💡 Boot 4 下 `@Component` 过滤器默认 Order = 最低优先级（`Ordered.LOWEST_PRECEDENCE`）——**大多数"过滤器不生效"是排序问题**，规范姿势是 FilterRegistrationBean + 显式 Order。

## 2. spring-web 内建过滤器

| 过滤器 | 职责 | 说明 |
|--------|------|------|
| `CharacterEncodingFilter` | 请求/响应字符集 | 乱码头号克星；Boot 默认装配 UTF-8 |
| `HiddenHttpMethodFilter` | `_method` 参数 → PUT/DELETE | 表单无法原生 PUT/DELETE 时 |
| `FormContentFilter` | PUT/DELETE 解析表单体 | 与 HiddenHttpMethod 搭配 |
| `CorsFilter` | CORS 跨域处理 | 独立于 MVC 配置的过滤器实现 |
| `PreFlightRequestFilter`（**7.1 新增**） | 专门处理 CORS 预检（OPTIONS） | 与 CorsFilter 二选一/协同 |
| `RequestContextFilter` | 请求绑定到线程（RequestContextHolder） | Boot 用监听器替代 |
| `OrderedFilter` / `OrderedRequestContextFilter` | 提供顺序的基类 | 组合排序 |

> 💡 **乱码排查第一站**：确认 `CharacterEncodingFilter` 存在且 charset=UTF-8（Boot 4 默认）——请求/响应乱码先查它，再看连接器/驱动配置（[Spring-JDBC-08](../Spring‑JDBC/08-集成地图与常见问题.md) 的数据乱码是另一条链）。

### 2.1 内建过滤器机制细节

| 过滤器 | 关键机制 | 配置项 |
|--------|----------|--------|
| `CharacterEncodingFilter` | 请求/响应编码统一 | `setEncoding("UTF-8")`、`setForceEncoding(true)` |
| `HiddenHttpMethodFilter` | `_method` 参数改写请求方法 | `setMethodParam("_method")`（可改名） |
| `FormContentFilter` | 解析 PUT/DELETE 表单体 | 依赖 FormHttpMessageConverter |
| `CorsFilter` | 委托 CorsConfigurationSource 判定 | source 注册路径 |
| `PreFlightRequestFilter`（7.1） | 独立处理 OPTIONS 预检 | 见 4 节 |
| `RequestContextFilter` | 绑定/释放 RequestAttributes | 无需配置 |

**OncePerRequestFilter 的语义**（内建过滤器大多继承它）：

```text
为什么叫 OncePerRequest：
  Servlet 规范允许过滤器被重复调用（forward/include 场景）
  OncePerRequestFilter 用请求属性做标记 → 同一次请求只执行一次 doFilterInternal
  → 编码/CORS 等"幂等性敏感"过滤器不会重复设置
```

> 💡 自写过滤器建议继承 `OncePerRequestFilter`（Boot 下几乎所有 Spring 过滤器都这么做）——避免 forward 场景重复执行；有顺序需求再实现 `OrderedFilter`。

### 2.2 内建过滤器配置示例

```java
// 编码过滤器定制（Boot 默认 UTF-8；强制编码场景）
@Configuration
public class EncodingConfig {
    @Bean
    public FilterRegistrationBean<CharacterEncodingFilter> encodingFilter() {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true);        // 请求+响应都强制，忽略客户端声明

        FilterRegistrationBean<CharacterEncodingFilter> reg =
                new FilterRegistrationBean<>(filter);
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return reg;
    }
}

// HiddenHttpMethodFilter：表单提交 REST 风格
@Bean
public HiddenHttpMethodFilter hiddenHttpMethodFilter() {
    HiddenHttpMethodFilter filter = new HiddenHttpMethodFilter();
    filter.setMethodParam("_method");        // <input type="hidden" name="_method" value="PUT">
    return filter;
}
```

| 过滤器 | 典型问题 | 解法 |
|--------|---------|------|
| CharacterEncodingFilter | 响应中文乱码 | forceEncoding(true) + UTF-8 |
| HiddenHttpMethodFilter | 表单只能 GET/POST | `_method` 隐藏域 |
| FormContentFilter | PUT 表单体读不到 | 与 HiddenHttpMethod 搭配（Boot 自动配） |
| RequestContextFilter | 请求上下文不绑定 | Boot 用监听器替代，无需手配 |

> ⚠️ 编码过滤器**请求/响应两侧**都管：请求侧解码、响应侧编码——只配一侧会在另一个方向乱码；`forceEncoding` 会覆盖客户端声明的 charset（按需权衡）。

### 2.3 自定义过滤器完整示例

```java
// 生产级自定义过滤器：耗时日志 + 响应时间头
public class TimingFilter extends OncePerRequestFilter implements OrderedFilter {
    private final Clock clock = Clock.systemUTC();

    @Override
    protected void doFilterInternal(HttpServletRequest req,
            HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        long start = clock.millis();
        try {
            chain.doFilter(req, res);                 // 放行（后续过滤器+Servlet）
        } finally {
            long cost = clock.millis() - start;
            res.setHeader("X-Process-Time", String.valueOf(cost));
            log.info("{} {} -> {} ({}ms)", req.getMethod(),
                    req.getRequestURI(), res.getStatus(), cost);
        }
    }

    @Override
    public int getOrder() { return 10; }              // Security(-100) 之后、业务之前
}

// 注册（Boot 4 标准姿势）
@Configuration
public class FilterConfig {
    @Bean
    public FilterRegistrationBean<TimingFilter> timingFilter() {
        FilterRegistrationBean<TimingFilter> reg =
                new FilterRegistrationBean<>(new TimingFilter());
        reg.addUrlPatterns("/api/*");                 // 只拦 API 路径
        reg.setOrder(10);
        return reg;
    }
}
```

> 💡 自定义过滤器要点：继承 `OncePerRequestFilter`（幂等）+ 实现 `OrderedFilter`（可排序）+ FilterRegistrationBean 注册（生效）——**三件套齐了才算一个合格的 Spring 过滤器**。

## 3. 过滤器排序与注册

```java
// Boot 4：@Bean + ServletRegistrationBean 方式注册并排序
@Configuration
public class FilterConfig {
    @Bean
    public FilterRegistrationBean<TraceFilter> traceFilter() {
        FilterRegistrationBean<TraceFilter> reg =
                new FilterRegistrationBean<>(new TraceFilter());
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);   // 最前（编码之后可调整）
        reg.addUrlPatterns("/*");
        return reg;
    }
}

// 或直接实现 OrderedFilter
public class TraceFilter extends OncePerRequestFilter implements OrderedFilter {
    @Override
    public int getOrder() { return Ordered.HIGHEST_PRECEDENCE + 10; }
    // doFilterInternal 实现
}
```

| 排序要点 | 说明 |
|---------|------|
| 顺序 | `Ordered.HIGHEST_PRECEDENCE` 小值先执行 |
| 约定 | CharacterEncodingFilter 最前；CORS 次之；自定义在两者之间按需 |
| Boot 默认链 | CharacterEncoding → Cors → FormContent → RequestContext（各自动配置） |
| 与 Security | Spring Security 的 FilterChainProxy 也在过滤器链中（Order=-100 附近） |

> ⚠️ 过滤器顺序错误是"配置不生效"的常见原因：CORS 过滤器必须在业务过滤器**之前**（否则跨域请求被业务过滤器拦下）；编码过滤器必须在**一切读取请求体之前**。

### 3.1 排序数值速查与常见错误

| 组件 | 典型 Order | 说明 |
|------|-----------|------|
| CharacterEncodingFilter | HIGHEST_PRECEDENCE（-2147483648 起） | 最前 |
| CorsFilter（Boot 自动配置） | 0 | CORS 在业务之前 |
| Spring Security FilterChainProxy | -100 | 比大多数业务过滤器早 |
| 自定义业务过滤器 | 10-100 | 在 Security 之后 |
| RequestContextFilter | HIGHEST_PRECEDENCE + 10 | 绑定线程 |

**过滤器顺序错的典型症状**：

| 症状 | 原因 | 修法 |
|------|------|------|
| CORS 响应头不出现 | CORS 在业务过滤器之后，请求被提前返回 | CORS 排最前（Order 小） |
| 乱码 | 编码过滤器在读取请求体之后 | 编码排最前 |
| Security 拦截预检 | FilterChainProxy 在 CORS 前且未放行 OPTIONS | Security 侧配置（见 5 节） |
| 自定义过滤器不生效 | 没注册（Boot 需要注册） | FilterRegistrationBean 注册 |
| 过滤器执行两次 | 非 OncePerRequestFilter 且 forward 场景 | 继承 OncePerRequestFilter |

> ⚠️ **Boot 自动配置陷阱**：Boot 的自动配置过滤器（如 CharacterEncodingFilter）优先级很高——自定义过滤器想"先于"它们，Order 必须更小；直接 `new` 的过滤器不进链（必须注册）。

### 3.2 过滤器、拦截器与安全链的协作

```text
三层拦截的执行顺序与分工：
  ① Servlet Filter（spring-web / 容器）
     编码、CORS、日志、请求绑定——"协议级"处理
  ② Spring Security 过滤器链（FilterChainProxy，在 ① 之中）
     认证、授权、CSRF——"安全级"处理
  ③ HandlerInterceptor（spring-webmvc）
     鉴权后置、审计、性能——"业务级"处理

  协作原则：
  过滤器不信任请求 → Security 做认证 → 拦截器做业务前检查
  跨域(CORS)必须在 Security 之前（预检不被拦）
  编码必须在一切读取之前
```

| 协作点 | 谁先谁后 | 为什么 |
|--------|---------|--------|
| 编码 vs 业务 | 编码最前 | 后续读取才不乱码 |
| CORS vs Security | CORS 在前 | 预检 OPTIONS 不能被 Security 拦 |
| Security vs 拦截器 | Security 在前 | 未认证请求到不了控制器 |
| 拦截器 vs 转换器 | 拦截器在前 | 拦截器可改 body（包装请求） |

> 💡 面试经典追问"过滤器里改请求体怎么做"——答 `HttpServletRequestWrapper`（缓存 body）+ 过滤器内包装传递；这正说明过滤器层的"协议级"定位：它可以在请求到达控制器前做任何包装。

## 4. CORS：跨域配置速查

```java
// 方式一：MVC 全局配置（webmvc 模块，注解驱动推荐）
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("https://app.example.com")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}

// 方式二：注解式（方法/类级）
@CrossOrigin(origins = "https://app.example.com", maxAge = 3600)
@RestController
public class OrderController { ... }

// 方式三：CorsFilter 全局过滤器（非 MVC 环境）
UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
CorsConfiguration config = new CorsConfiguration();
config.setAllowedOrigins(List.of("https://app.example.com"));
config.setAllowedMethods(List.of("*"));
source.registerCorsConfiguration("/api/**", config);
// CorsFilter bean 注册（7.1 可选 PreFlightRequestFilter）
```

| 配置项 | 含义 | 注意 |
|--------|------|------|
| `allowedOrigins` | 允许的来源 | `*` 与 `allowCredentials=true` **互斥**（浏览器规范） |
| `allowCredentials` | 允许携带 Cookie | 用通配来源时禁止 |
| `allowedMethods` | 允许方法 | 预检会问 |
| `maxAge` | 预检缓存秒数 | 减少预检请求 |
| `exposedHeaders` | 暴露给前端的响应头 | 默认只暴露基础头 |

> ⚠️ **跨域三问排查**：① 请求有没有过预检（OPTIONS）→ 没有则过滤器没生效；② 响应有没有 `Access-Control-Allow-Origin` → 没有则 CORS 配置未命中路径；③ 带 Cookie 时报错 → `allowCredentials` + 具体来源（非 `*`）。

### 4.1 预检请求细节与 PreFlightRequestFilter（7.1）

**什么请求会触发预检**（浏览器规范）：

| 触发预检 | 不触发（简单请求） |
|----------|------------------|
| 自定义头（X-Auth-Token 等） | 标准简单头（Accept/Content-Type 等） |
| Content-Type 非三类 | text/plain、multipart/form-data、application/x-www-form-urlencoded |
| 非 GET/POST/HEAD | GET/POST/HEAD |
| 带 Cookie 的跨域 | 不带 Cookie |

```text
预检流程（OPTIONS）：
  浏览器 → OPTIONS /api/order（带 Origin + Access-Control-Request-* 头）
  服务端 → 200 + Access-Control-Allow-* 头（有效期 maxAge）
  浏览器 → 通过后发真实请求
  预检失败 = 真实请求不会发出（前端只看到 CORS 错误，看不到业务错误）
```

| 方式 | 适用 | 说明 |
|------|------|------|
| CorsFilter（6.x 起） | 通用 | 处理预检 + 真实请求加头 |
| PreFlightRequestFilter（7.1） | 分离关注点 | 只处理 OPTIONS 预检；真实请求的 CORS 头由其他配置负责 |
| MVC addCorsMappings | MVC 环境 | 最简，覆盖 /api/** |

> 💡 **排查口诀**：前端报 CORS 错误先问"预检过没过"——DevTools 看 Network 里有没有 OPTIONS 请求、响应有没有 `Access-Control-Allow-Origin`；**预检本身 404/403 也是常见坑**（Security 拦截）。

### 4.2 CORS 配置项深入

| 配置项 | 说明 | 常见坑 |
|--------|------|--------|
| `allowedOrigins` | 精确来源列表 | 不能带路径（`https://a.com/` 不合法） |
| `allowedOriginPatterns` | 模式匹配（`https://*.example.com`） | 与 credentials 兼容（比 `*` 安全） |
| `allowedMethods` | 方法列表 | 忘了 OPTIONS 预检会失败（其实浏览器只问真实方法） |
| `allowedHeaders` | 允许的请求头 | 与前端实际发的头不一致 → 预检失败 |
| `exposedHeaders` | 暴露给前端的响应头 | 默认只暴露基础头——自定义头（X-Trace-Id）要加 |
| `allowCredentials` | 凭证模式 | 与通配来源互斥 |
| `maxAge` | 预检缓存 | 秒级；0 表示每次都预检 |
| 预检响应 200 但无 Allow-* 头 | 配置未命中路径 | 检查 registerCorsConfiguration 的路径模式 |
| 同源请求也走 CORS 逻辑吗 | 不走（无 Origin 头或同源） | CORS 只对跨域请求生效——同源不受影响 |
| `Vary: Origin` | 缓存区分 | 配合 CDN/浏览器缓存必配 |

```java
// 模式匹配示例（多子域后台）
CorsConfiguration config = new CorsConfiguration();
config.setAllowedOriginPatterns(List.of("https://*.example.com"));
config.setAllowCredentials(true);      // 模式匹配可与凭证共存 ★
config.setExposedHeaders(List.of("X-Trace-Id", "Content-Disposition"));
```

> ⚠️ **exposedHeaders 漏配**是最隐蔽的坑：后端明明返回了 `X-Trace-Id`，前端 JS 却读不到（浏览器只暴露白名单头）——跨域调试"头丢了"先看它。

## 5. CORS 与安全组件配合

```text
CORS 在 Spring Security 下的正确姿势（常见误区）：
  ❌ 只配 MVC 的 addCorsMappings —— Security 过滤器链会先拦下预检
  ✅ Security 侧配置 CorsConfigurationSource + 授权规则放行 OPTIONS
      http.cors(withDefaults())            // 启用 Security 的 CORS 处理器
      http.authorizeHttpRequests(auth ->
          auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()  // 预检放行
              .anyRequest().authenticated());

  Spring Security 7.1 支持 PreFlightRequestFilter 的预检过滤（见 Security 系列）
```

> 🎯 **核心要点**：跨域是"过滤器链问题"不是"控制器问题"——**Security 在前则 Security 必须先认识 CORS**；预检（OPTIONS）请求默认不带认证信息，必须放行；MVC 配置与 Security 配置二选一为主（Security 的 `cors()` 会读取 MVC 的 CorsConfigurationSource，可复用）。

### 5.1 完整配置示例与面试追问

```java
// 生产级 CORS 完整配置（Security 侧 + 具体来源）
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("https://app.example.com",   // 具体来源，不用 *
                "https://admin.example.com"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Trace-Id"));
        config.setAllowCredentials(true);          // 允许 Cookie（具体来源才合法）
        config.setMaxAge(3600L);                   // 预检缓存 1 小时

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults())       // 使用上面的 source
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().authenticated());
        return http.build();
    }
}
```

| 面试追问 | 回答要点 |
|----------|---------|
| CORS 是服务端还是浏览器机制 | 服务端响应头 + 浏览器强制——服务端不报错，浏览器拦截 |
| allowedOrigins 为什么不能和 credentials 同用 `*` | 浏览器规范：凭证模式下通配来源无意义（直接拒绝） |
| 预检结果能缓存吗 | maxAge 控制；过期后浏览器重新预检 |
| 本地开发怎么办 | allowedOriginPatterns（模式匹配）或环境配置切换 |
| CORS 与 CSRF 什么关系 | 不同问题：CORS 管"能不能跨域读"，CSRF 管"请求是否伪造"——CORS 不能替代 CSRF 防护 |
| 过滤器链里多个 CORS 配置会怎样 | 先执行者加头后，后者跳过（已处理标记）——配置冲突表现为头缺失 |
| 为什么不建议前端代理解决 CORS | 代理能绕开浏览器同源，但掩盖了真实跨域语义——上线环境仍要正确配置 |
| OPTIONS 请求要打日志吗 | 建议打（预检频率可观测）——排除"预检风暴"（maxAge 过小） |
| 内建过滤器与自定义过滤器怎么排 | 内建用 Boot 属性/自动配置，自定义用 FilterRegistrationBean——Order 统一比较 |
| 静态资源也走过滤器链吗 | 走（Servlet 层先于 DispatcherServlet）——协议级过滤器覆盖静态资源 |
| 过滤器里能拿到 Spring Bean 吗 | 能——注册为 @Bean 后由容器注入（过滤器本身是 Bean） |
| 为什么 CORS 配置了却不生效 | 顺序（被业务拦）/路径（未命中）/缓存（浏览器缓存旧策略）三查 |
| PreFlightRequestFilter 与 CorsFilter 冲突吗 | 可协同——一个管预检、一个管真实请求加头；重复配置会有冗余头，按需二选一 |
| 拦截器里处理跨域可以吗 | 不行——拦截器在 DispatcherServlet 之后，预检（OPTIONS）未必到控制器层 |
| 生产要不要把 maxAge 调大 | 要——预检是额外往返，合理缓存（如 1h）显著降延迟；改配置后要等缓存过期 |

### 5.2 生产实践与快速记忆卡

| 实践 | 要点 |
|------|------|
| 来源白名单放配置 | 环境隔离（dev/test/prod 各自 origins） |
| 预检别过度放行 | OPTIONS 只放行到 CORS 层，不要放行到业务 |
| 加 Vary: Origin | 响应头随 Origin 变化——缓存需按 Origin 区分 |
| 监控 CORS 报错 | 前端错误上报时带预检信息，快速定位 |
| 新老过滤器并存 | CorsFilter 与 PreFlightRequestFilter 二选一为主，避免重复处理 |

```text
一分钟记忆卡：
  链：容器 Filter → DispatcherServlet → Interceptor（过滤早且广）
  顺序：编码最前 → CORS → Security(-100) → 业务
  预检：非简单请求先 OPTIONS；maxAge 缓存；凭证模式禁通配
  排查：没 OPTIONS = 过滤器没生效；没 Allow-Origin = 配置没命中；报凭证错 = allowCredentials
```

---

**下一模块**：[07-异步与流式速查](07-异步与流式速查.md)　**返回总览**：[00-Spring Web组件总览](00-Spring Web组件总览.md)
