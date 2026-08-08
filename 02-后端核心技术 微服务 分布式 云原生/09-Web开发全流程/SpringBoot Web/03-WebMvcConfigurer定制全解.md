# WebMvcConfigurer 定制全解
> MVC 定制的唯一入口：拦截器、CORS、静态资源、消息转换器、参数解析器、格式化器、路径匹配——一个接口十几个方法，全部吃透

## 📚 目录
1. [WebMvcConfigurer 全景](#1-webmvcconfigurer-全景)
2. [拦截器注册 addInterceptors](#2-拦截器注册-addinterceptors)
3. [CORS：addCorsMappings vs 全局 CorsFilter](#3-corsaddcorsmappings-vs-全局-corsfilter)
4. [静态资源 addResourceHandlers](#4-静态资源-addresourcehandlers)
5. [消息转换器 configureMessageConverters](#5-消息转换器-configuremessageconverters)
6. [参数解析器与返回值处理器](#6-参数解析器与返回值处理器)
7. [格式化器与数据绑定](#7-格式化器与数据绑定)
8. [路径匹配与视图控制器](#8-路径匹配与视图控制器)
9. [常见定制场景速查表](#9-常见定制场景速查表)

## 1. WebMvcConfigurer 全景

```java
// 定制 MVC 的标准姿势：实现接口 + 覆盖需要的 default 方法（其余自动配置保持不变）
@Configuration
public class WebConfig implements WebMvcConfigurer {
    // 按需覆盖，不需要的全部忽略
}
```

| 方法 | 定制内容 | 章节 |
|------|---------|:---:|
| `addInterceptors` | 拦截器注册 | §2 |
| `addCorsMappings` | 应用内 CORS 规则 | §3 |
| `addResourceHandlers` | 静态资源映射 | §4 |
| `configureMessageConverters` / `extendMessageConverters` | HTTP 消息转换器 | §5 |
| `addArgumentResolvers` | 自定义参数解析器 | §6 |
| `addReturnValueHandlers` | 自定义返回值处理器 | §6 |
| `addFormatters` | 格式化器（日期/枚举） | §7 |
| `configurePathMatch` | 路径匹配策略 | §8 |
| `addViewControllers` | 无逻辑视图控制器 | §8 |
| `configureViewResolvers` | 视图解析器 | §8 |
| `configureContentNegotiation` | 内容协商 | - |
| `configureAsyncSupport` | 异步支持配置 | - |
| `addArgumentResolvers` | 参数解析器 | §6 |

> 🎯 黄金法则：**只覆盖你需要的方法**——实现接口但空覆盖，或覆盖后不调用 super，会**破坏自动配置的默认行为**（典型翻车点见 §5）。

## 2. 拦截器注册 addInterceptors

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor())          // 登录校验
                .addPathPatterns("/api/**")                     // 拦截范围
                .excludePathPatterns("/api/login", "/api/public/**");  // 白名单

        registry.addInterceptor(new TraceIdInterceptor())
                .addPathPatterns("/**");                        // 日志 traceId
    }
}
```

| 规则 | 说明 |
|------|------|
| 多个拦截器 | 按注册顺序执行 preHandle，postHandle/afterCompletion 逆序 |
| 顺序控制 | 无法用 @Order 控制（按注册顺序）——需要顺序时注册 Bean 而非匿名 |
| 拦截范围 | 用 `addPathPatterns`/`excludePathPatterns`（Ant 风格 `**` 全匹配） |
| 白名单遗漏 | 忘记 exclude 登录接口 → 登录死循环（经典故障） |
| 静态资源 | Interceptor 默认**不拦截**静态资源（若请求走 ResourceHttpRequestHandler） |

> ⚠️ 对比 Filter 的拦截范围：Filter 用 `addMappingForUrlPatterns` 且**会拦静态资源**；Interceptor 面向"已路由到 Controller"的请求。鉴权放 Filter 还是 Interceptor，见 [04-Filter与Interceptor注册与顺序](04-Filter与Interceptor注册与顺序.md) 的三层分工表。

## 3. CORS：addCorsMappings vs 全局 CorsFilter

```java
// 方式一：MVC 层配置（addCorsMappings，只对 Controller 映射生效）
@Override
public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
            .allowedOrigins("https://admin.example.com")     // 白名单源（别用 *）
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)                          // 带 Cookie
            .maxAge(3600);                                   // 预检缓存 1h
}

// 方式二：容器层 Filter（CorsFilter，拦一切含静态资源/错误页）
@Bean
public CorsFilter corsFilter() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("https://admin.example.com"));
    config.setAllowedMethods(List.of("*"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return new CorsFilter(source);
}
```

| 对比 | addCorsMappings | CorsFilter |
|------|:---:|:---:|
| 层 | MVC | 容器（Filter） |
| 覆盖范围 | Controller 路由 | 所有请求（含静态/错误页） |
| 预检处理 | MVC 自动 | Filter 层自动 |
| 与安全框架协作 | Spring Security 需放行预检 | 需在 Security Filter 前 |
| 生产推荐 | ✅ 业务接口 | 网关/静态资源混合场景 |

> 🎯 配套结论：**跨域规则放网关层（Nginx/网关）与应用层二选一**，别两层都配导致预检重复/冲突——与 [Nginx 体系](../Nginx/00-Nginx知识体系总览.md) 的 CORS 章节同理。

## 4. 静态资源 addResourceHandlers

```java
@Override
public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // 自定义目录映射（上传文件目录千万别放 classpath 内！）
    registry.addResourceHandler("/files/**")
            .addResourceLocations("file:/data/uploads/")      // 磁盘路径（file: 前缀）
            .setCachePeriod(3600);                            // 秒

    // 版本化资源（缓存控制最佳实践）
    registry.addResourceHandler("/static/**")
            .addResourceLocations("classpath:/static/")
            .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic());
}
```

| 要点 | 说明 |
|------|------|
| 默认静态目录 | classpath:/static/、/public/、/resources/、/META-INF/resources/ + 根路径 |
| 自定义映射 | `addResourceLocations` 支持 classpath: 与 file: 前缀 |
| 缓存控制 | `setCachePeriod`/`setCacheControl`（配合版本文件名防缓存错乱） |
| Boot 4 | `PathRequest.toStaticResources()` 新增 `/fonts/**` 默认放行 |
| 与上传联动 | 上传文件落磁盘 → `file:/data/uploads/` 映射 → 可下载 |

## 5. 消息转换器 configureMessageConverters

```java
// ⚠️ 两大陷阱先记住：
// ① configureMessageConverters 是"整体替换"——不调 super 会丢掉默认转换器！
// ② 正确姿势：extendMessageConverters（追加）或 Boot 4 的 Customizer Bean

// 姿势一：extendMessageConverters（追加/调整，推荐）
@Override
public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    converters.stream()
            .filter(c -> c instanceof MappingJackson2HttpMessageConverter)
            .findFirst()
            .ifPresent(c -> ((MappingJackson2HttpMessageConverter) c).setObjectMapper(customObjectMapper()));
}

// 姿势二（Boot 4 官方推荐）：ServerHttpMessageConvertersCustomizer Bean
// HttpMessageConverters 在 Boot 4 已废弃，改用独立 Customizer
@Bean
public ServerHttpMessageConvertersCustomizer customJsonConverter() {
    return converters -> {
        converters.add(new MappingJackson2HttpMessageConverter(
                JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build()));
    };
}
```

> ⚠️ **configureMessageConverters 是替换语义**：覆盖它且不调 `super.configureMessageConverters(converters)` → 默认转换器全丢 → JSON 返回 406。**能 extend 就不 configure**，Boot 4 直接上 Customizer。

## 6. 参数解析器与返回值处理器

```java
// 自定义参数解析器（如：@LoginUser 注入当前登录用户）
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        // 从 Session/请求属性取当前用户
        return webRequest.getAttribute("currentUser", RequestAttributes.SCOPE_REQUEST);
    }
}

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new LoginUserArgumentResolver());   // 追加（不动默认解析器）
    }
}

// 使用：Controller 直接注入
@GetMapping("/me")
public UserVO me(@LoginUser User user) { return ...; }
```

> 💡 原理：自定义解析器插入 `HandlerMethodArgumentResolver` 链，在默认解析器之前/之后按序尝试（`supportsParameter` 决定谁能处理）。**这是"@LoginUser 注入"这类框架手法的标准实现**。

## 7. 格式化器与数据绑定

```java
@Override
public void addFormatters(FormatterRegistry registry) {
    // 全局日期格式（@DateTimeFormat 优先于全局）
    registry.addFormatterForFieldType(LocalDate.class,
            new LocalDateFormatter(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    registry.addFormatterForFieldType(LocalDateTime.class,
            new LocalDateTimeFormatter(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
}

// 枚举转换（前端传 code → 枚举）
public class StatusEnumFormatter implements Formatter<StatusEnum> {
    @Override
    public StatusEnum parse(String text, Locale locale) {
        return StatusEnum.fromCode(Integer.parseInt(text));
    }
    @Override
    public String print(StatusEnum object, Locale locale) { return String.valueOf(object.getCode()); }
}
```

> 💡 数据绑定三层：**格式化器（字符串↔对象）→ 转换器（Converter，类型↔类型）→ 绑定器（WebDataBinder）**。Controller 参数绑定、@ModelAttribute 表单绑定都走这条链。

## 8. 路径匹配与视图控制器

```java
// 路径匹配（Boot 4 / Framework 7）
@Override
public void configurePathMatch(PathMatchConfigurer configurer) {
    // ⚠️ setUseTrailingSlashMatch 已在 Framework 7 移除！
    // /foo 与 /foo/ 不再等价；需旧行为 → UrlHandlerFilter
    configurer.setPathMatcher(new AntPathMatcher());   // 降级 Ant（默认 PathPattern）
    // configurer.addPathPrefix("/api", c -> c.isAnnotationPresent(RestController.class));  // 批量前缀
}

// 视图控制器（无 Controller 逻辑的转发）
@Override
public void addViewControllers(ViewControllerRegistry registry) {
    registry.addViewController("/").setViewName("forward:/index.html");
    registry.addViewController("/login").setViewName("login");
}
```

| 版本 | 默认匹配器 | 尾斜杠 |
|------|-----------|:---:|
| Boot 3（Framework 6） | PathPatternParser | `/foo` == `/foo/`（可配置） |
| **Boot 4（Framework 7）** | PathPatternParser | **`/foo` ≠ `/foo/`（配置项已移除）** |

> ⚠️ Boot 3 → 4 的典型 404 根因：旧客户端/网关习惯性带尾斜杠请求 → Boot 4 直接 404。迁移方案：客户端去掉尾斜杠，或加 `UrlHandlerFilter` 兼容。

## 9. 常见定制场景速查表

| 需求 | 用哪个方法/Bean |
|------|----------------|
| 登录校验拦截 | addInterceptors（排除登录接口） |
| 跨域 | addCorsMappings（业务）/ CorsFilter（全局） |
| 静态资源外置目录 | addResourceHandlers + file: |
| JSON 序列化规则 | Boot 4：ServerHttpMessageConvertersCustomizer |
| 枚举/日期全局转换 | addFormatters |
| @LoginUser 注入 | addArgumentResolvers |
| 视图转发（无逻辑） | addViewControllers |
| 拦截器用 Bean 管理顺序 | 拦截器注册为 @Bean + addInterceptors 注入 |
| 内容协商（xml/json 按 Accept） | configureContentNegotiation |
| 异步超时/线程池 | configureAsyncSupport |

> 🎯 **面试一句话**：WebMvcConfigurer 是"**自动配置的门户**"——Boot 的 WebMvcAutoConfiguration 检测到自定义 WebMvcConfigurer 后，把默认配置与自定义配置合并（`WebMvcConfigurerComposite` 组合模式），所以只覆盖需要的方法、其余默认保留。

---

**下一模块**：[04-Filter与Interceptor注册与顺序](04-Filter与Interceptor注册与顺序.md) / **返回总览**：[00-SpringBootWeb总览](00-SpringBootWeb总览.md)
