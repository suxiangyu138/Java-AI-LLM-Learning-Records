# 02-FilterChain 责任链
> 过滤器链的链式执行、顺序规则、请求/响应包装——"多个 Filter 怎么排队、怎么协作"

## 📚 目录
1. [FilterChain 是什么](#1-filterchain-是什么)
2. [链式执行机制](#2-链式执行机制)
3. [过滤器顺序规则](#3-过滤器顺序规则)
4. [请求包装（Wrapper）](#4-请求包装wrapper)
5. [典型多 Filter 组合](#5-典型多-filter-组合)
6. [核心要点](#6-核心要点)
7. [参考来源](#7-参考来源)

## 1. FilterChain 是什么

```text
FilterChain = 过滤器链（责任链模式）
  多个 Filter 按顺序组成链
  chain.doFilter 依次调用下一个
  最后一个 Filter 放行到 Servlet/Controller
```

```text
请求经过链：
客户端 → Filter1 → Filter2 → Filter3 → Servlet/Controller → 业务
                  ← 响应逆序返回
```

> 🎯 **责任链模式的应用**：**每个 Filter 只做自己的事，然后交给下一个**——解耦公共逻辑（编码/日志/鉴权各管一段）。

## 2. 链式执行机制

```text
执行顺序（洋葱模型）：
Filter1 前置 → Filter2 前置 → Servlet → Filter2 后置 → Filter1 后置

为什么"后置"是逆序？
  chain.doFilter 是递归调用：外层 Filter 的 doFilter 挂起等待内层完成
```

```java
// Filter1（外层）
public void doFilter(req, resp, chain) {
    System.out.println("Filter1 前置");
    chain.doFilter(req, resp);          // 挂起，等内层完成
    System.out.println("Filter1 后置");
}

// Filter2（内层）
public void doFilter(req, resp, chain) {
    System.out.println("Filter2 前置");
    chain.doFilter(req, resp);          // 放行到 Servlet
    System.out.println("Filter2 后置");
}
```

```text
输出：
Filter1 前置
Filter2 前置
（Servlet 业务）
Filter2 后置
Filter1 后置
```

> 🎯 **洋葱模型是面试点**：**前置顺序执行、后置逆序执行**——像剥洋葱（进一层、出一层）；响应包装、耗时统计都利用这个特性。

## 3. 过滤器顺序规则

| 场景 | 顺序决定方式 |
|------|-------------|
| web.xml | `<filter-mapping>` 声明顺序 |
| @WebFilter 注解 | 不保证顺序（需 Boot 方式） |
| Spring Boot | `FilterRegistrationBean.setOrder()` / @Order |

```text
顺序设计原则：
  ① 通用前置（编码/日志）在前
  ② 鉴权/安全在中间
  ③ 业务相关在后
  ④ 后置统计（耗时）可在最外层（包裹全链）
```

| 典型顺序 | Filter |
|----------|--------|
| 1 | CharacterEncodingFilter（编码） |
| 2 | CorsFilter（跨域） |
| 3 | LoginFilter（登录校验） |
| 4 | LogFilter（耗时统计，外层包裹更佳） |

> ⚠️ **顺序是功能正确性的一部分**：编码 Filter 必须最先（否则后续 Filter 读乱码）；鉴权要在业务 Filter 前；**耗时统计 Filter 放最外层**（才能统计全链路）。

## 4. 请求包装（Wrapper）

```text
场景：Filter 想"修改请求"——但 ServletRequest 是接口，不能直接改
解法：包装类（Wrapper）
  继承 HttpServletRequestWrapper
  重写需要修改的方法
  传入 chain.doFilter
```

```java
// 请求包装：读取 Body 后仍可放行（Body 只能读一次问题）
public class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    public CachedBodyRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        // 缓存请求体（InputStream 读一次后缓存）
        this.cachedBody = request.getInputStream().readAllBytes();
    }

    @Override
    public ServletInputStream getInputStream() {
        // 返回可重复读的流
        return new CachedServletInputStream(cachedBody);
    }
}

// Filter 中使用
public void doFilter(req, resp, chain) {
    CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper((HttpServletRequest) req);
    chain.doFilter(wrapper, resp);      // 传递包装后的请求
}
```

| 包装场景 | 说明 |
|----------|------|
| Body 重复读 | 缓存请求体（日志 + 业务都要读） |
| 修改参数 | 包装 getParameter 返回值 |
| 修改响应 | 包装响应（压缩/加密/缓存） |
| 添加头 | 包装 setHeader |

> 💡 **包装是 Filter 的"高级武器"**：请求/响应包装类解决"Body 只能读一次""参数只读"等问题——Spring 的 `ContentCachingRequestWrapper` 就是官方包装类。

## 5. 典型多 Filter 组合

```text
一个真实请求经过的 Filter 链（Spring Boot 典型）：
  CharacterEncodingFilter（编码，框架内置）
  CorsFilter（跨域，自定义）
  LoginFilter（登录校验，自定义）
  XssFilter（XSS 过滤，自定义）
  → DispatcherServlet → Controller
```

```java
// 多 Filter 协作示例：登录校验 + 日志
@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<LoginFilter> loginFilter() {
        FilterRegistrationBean<LoginFilter> bean = new FilterRegistrationBean<>(new LoginFilter());
        bean.addUrlPatterns("/*");
        bean.setOrder(2);                    // 顺序 2
        return bean;
    }

    @Bean
    public FilterRegistrationBean<LogFilter> logFilter() {
        FilterRegistrationBean<LogFilter> bean = new FilterRegistrationBean<>(new LogFilter());
        bean.addUrlPatterns("/*");
        bean.setOrder(1);                    // 顺序 1（外层）
        return bean;
    }
}
```

> 🎯 **组合协作的要点**：每个 Filter **单一职责**（编码只管编码、鉴权只管鉴权）+ **顺序明确**（setOrder）——Filter 链就是"公共逻辑的流水线"。

## 6. 核心要点

> 🎯 **核心要点**：
> - FilterChain = 责任链：chain.doFilter 依次放行，最后一个到 Servlet；
> - 洋葱模型：**前置顺序、后置逆序**（递归挂起特性）；
> - 顺序三原则：编码最前、鉴权居中、耗时统计最外层；
> - 请求包装（Wrapper）：解决 Body 单次读/参数修改（ContentCachingRequestWrapper 是官方示例）；
> - 多 Filter 组合 = 单一职责 + 明确顺序；
> - 不调 chain.doFilter = 拦截（责任链的中断）。

## 7. 参考来源

- [Jakarta Servlet 规范（FilterChain）](https://jakarta.ee/specifications/servlet/)
- [Baeldung：Filter Chain](https://www.baeldung.com/java-servlet-filters)

---

**下一模块**：[03-常用Filter场景](03-常用Filter场景.md)　/　**返回总览**：[00-总览](00-Filter过滤器总览.md)
