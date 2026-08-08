# 04-Spring Boot 注册 Filter
> 三种注册方式、顺序控制、OncePerRequestFilter——"Boot 时代的 Filter 正确姿势"

## 📚 目录
1. [三种注册方式对比](#1-三种注册方式对比)
2. [方式一：@WebFilter + @ServletComponentScan](#2-方式一webfilter--servletcomponentscan)
3. [方式二：FilterRegistrationBean（推荐）](#3-方式二filterregistrationbean推荐)
4. [方式三：@Component + @Order](#4-方式三component--order)
5. [顺序控制](#5-顺序控制)
6. [OncePerRequestFilter](#6-onceperrequestfilter)
7. [核心要点](#7-核心要点)
8. [参考来源](#8-参考来源)

## 1. 三种注册方式对比

| 方式 | 写法 | 顺序控制 | 推荐度 |
|------|------|:---:|:---:|
| @WebFilter + 扫描 | 注解 | ❌ 不可控 | ⚠️ |
| **FilterRegistrationBean** | Java 配置 | ✅ setOrder | ✅ 推荐 |
| @Component + @Order | 注解 + 排序 | ✅（有限） | ⚠️ |

> 🎯 **Boot 时代推荐 FilterRegistrationBean**：顺序可控 + 可配置 URL + 可注入依赖——**@WebFilter 在 Boot 中顺序不可控是最大问题**。

## 2. 方式一：@WebFilter + @ServletComponentScan

```java
@WebFilter(urlPatterns = "/*")
public class LoginFilter implements Filter {
    // ...
}
```

```java
@SpringBootApplication
@ServletComponentScan          // 扫描 @WebFilter/@WebServlet/@WebListener
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

| 优点 | 缺点 |
|------|------|
| 简单 | **顺序不可控**（依赖类名/加载顺序） |
| 原生 | 无法配置精确 URL 细节 |
| | 无法注入 Spring Bean（非 Spring 管理的 Filter） |

> ⚠️ **@WebFilter 的坑**：① 顺序不可控（多 Filter 时无法保证执行次序）；② Filter 实例不是 Spring Bean（无法 @Autowired）——**业务需要注入依赖时别用这个方式**。

## 3. 方式二：FilterRegistrationBean（推荐）

```java
@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<LoginFilter> loginFilter() {
        FilterRegistrationBean<LoginFilter> bean = new FilterRegistrationBean<>(new LoginFilter());
        bean.addUrlPatterns("/*");             // 拦截范围
        bean.setOrder(2);                      // 顺序（越小越先）
        bean.setName("loginFilter");           // 名称（可选）
        return bean;
    }

    @Bean
    public FilterRegistrationBean<LogFilter> logFilter() {
        FilterRegistrationBean<LogFilter> bean = new FilterRegistrationBean<>(new LogFilter());
        bean.addUrlPatterns("/*");
        bean.setOrder(1);                      // 外层（先执行）
        return bean;
    }
}
```

```java
// Filter 可以注入 Spring Bean
@Component
public class LoginFilter implements Filter {
    @Autowired
    private UserService userService;           // ✅ 可注入

    @Override
    public void doFilter(...) { ... }
}
```

| 能力 | 说明 |
|------|------|
| `addUrlPatterns` | 拦截范围 |
| `setOrder` | 顺序（越小越先） |
| Spring Bean | Filter 可 @Autowired（推荐 Filter 也注册为 Bean） |
| 启动时注册 | 容器初始化时装配 |

> 🎯 **推荐组合**：**Filter 本身 @Component（可注入）+ FilterRegistrationBean 注册（控顺序）**——既能依赖注入又能精确控制顺序与范围。

## 4. 方式三：@Component + @Order

```java
@Component
@Order(1)                       // 顺序（越小越先）
public class LogFilter implements Filter {
    // ...
}
```

| 优点 | 缺点 |
|------|------|
| 最简单（一个注解） | 顺序基于 Order，但与其他 Filter 注册方式混合时规则复杂 |
| 自动注册 | 只能 /* 全部拦截（无法配置 URL） |

> ⚠️ **@Component Filter 的注意**：默认拦截所有请求（无法配 URL 白名单）；与 FilterRegistrationBean 混用时的顺序规则容易混乱——**统一用一种方式更清晰**。

## 5. 顺序控制

```text
顺序规则（Order 越小越先执行）：
  @Order(1) 最外层（先执行前置，后执行后置）
  @Order(10) 内层

推荐顺序编排：
  1  CharacterEncodingFilter（编码，框架默认）
  2  CorsFilter（跨域）
  3  XssFilter（XSS 过滤）
  4  LoginFilter（登录校验）
  5  RateLimitFilter（限流，可放最外）
```

```java
// 框架内置 Filter 的顺序调整
@Bean
public FilterRegistrationBean<CharacterEncodingFilter> encodingFilter() {
    FilterRegistrationBean<CharacterEncodingFilter> bean =
            new FilterRegistrationBean<>(new CharacterEncodingFilter());
    bean.addUrlPatterns("/*");
    bean.setOrder(-100);               // 框架 Filter 默认 -2147483648 附近
    bean.setName("encodingFilter");
    return bean;
}
```

> 💡 **顺序设计**：编码最先（否则乱码）、CORS 次之、鉴权/安全居中、耗时统计最外层——**"先能让请求通，再管业务"**。

## 6. OncePerRequestFilter

```java
// 问题：请求转发（forward/include）时 Filter 会执行多次
// 解决：OncePerRequestFilter——保证一次请求只执行一次

@Component
public class LoginFilter extends OncePerRequestFilter {     // 继承它

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        // 只执行一次的逻辑
        // ...
        filterChain.doFilter(request, response);
    }
}
```

| 对比 | Filter | OncePerRequestFilter |
|------|--------|---------------------|
| 转发场景 | 执行多次 | **只执行一次** |
| 依赖注入 | 需要注册方式配合 | 天然 Spring Bean |
| 推荐 | 简单场景 | **Spring 场景推荐** |

> 🎯 **OncePerRequestFilter 是 Spring 场景的标准基类**：解决"转发/包含时重复执行"问题 + 天然支持依赖注入——**Boot 项目中自定义 Filter 优先继承它**（Spring Security 的过滤器就是这么做的）。

## 7. 核心要点

> 🎯 **核心要点**：
> - 三方式：@WebFilter（顺序不可控，弃用倾向）、**FilterRegistrationBean（推荐，顺序可控）**、@Component+@Order（简单但范围受限）；
> - 推荐组合：Filter @Component（可注入）+ FilterRegistrationBean（控顺序）；
> - 顺序规则：Order 越小越先；编码最先、CORS 次之、鉴权居中、统计最外；
> - **OncePerRequestFilter 是 Spring 标准基类**：一次请求只执行一次 + 可注入；
> - 统一用一种注册方式（混用顺序易乱）；
> - Boot 项目自定义 Filter：继承 OncePerRequestFilter + FilterRegistrationBean 注册。

## 8. 参考来源

- [Spring Boot Filter 官方文档](https://docs.spring.io/spring-boot/reference/web/servlet.html)
- [Baeldung：Spring Boot Add Filter](https://www.baeldung.com/spring-boot-add-filter)
- [Spring Framework：OncePerRequestFilter](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-servlet/filters.html)

---

**下一模块**：[05-Filter-vs-Interceptor-vs-AOP](05-Filter-vs-Interceptor-vs-AOP.md)　/　**返回总览**：[00-总览](00-Filter过滤器总览.md)
