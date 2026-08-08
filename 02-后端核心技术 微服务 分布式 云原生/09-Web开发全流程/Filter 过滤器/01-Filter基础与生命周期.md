# 01-Filter 基础与生命周期
> Filter 接口三方法、生命周期、执行时机、第一个 Filter——"请求进出的统一关卡"

## 📚 目录
1. [Filter 接口](#1-filter-接口)
2. [生命周期三阶段](#2-生命周期三阶段)
3. [doFilter 的执行时机](#3-dofilter-的执行时机)
4. [第一个 Filter 实战](#4-第一个-filter-实战)
5. [Filter 的作用范围](#5-filter-的作用范围)
6. [核心要点](#6-核心要点)
7. [参考来源](#7-参考来源)

## 1. Filter 接口

```java
public interface Filter {
    // ① 初始化（容器启动时执行一次）
    default void init(FilterConfig filterConfig) throws ServletException {}

    // ② 核心：每次请求执行
    void doFilter(ServletRequest request, ServletResponse response,
                   FilterChain chain) throws IOException, ServletException;

    // ③ 销毁（容器关闭时执行一次）
    default void destroy() {}
}
```

| 方法 | 时机 | 次数 | 用途 |
|------|------|------|------|
| `init` | 容器启动 | 1 次 | 初始化资源 |
| `doFilter` | 每次请求 | N 次 | 拦截逻辑 |
| `destroy` | 容器关闭 | 1 次 | 释放资源 |

> 🎯 **三方法记忆**：**init 一次、doFilter 每次、destroy 一次**——与 Servlet 生命周期同构（容器管理）。

## 2. 生命周期三阶段

```text
① 初始化（init）：
   容器启动时创建 Filter 实例并调用 init
   只能初始化一次（单例）

② 服务（doFilter）：
   每次请求经过时调用
   拦截 + 放行（chain.doFilter）

③ 销毁（destroy）：
   容器关闭时调用（释放资源）
```

| 阶段 | 触发 | 注意 |
|------|------|------|
| init | 容器启动 | 不要在 init 做重操作（启动慢） |
| doFilter | 每次请求 | 核心逻辑在这 |
| destroy | 容器关闭 | 释放连接/资源 |

> 💡 Filter 是**单例**：init/destroy 只执行一次——**实例变量注意线程安全**（共享状态要小心，与 Servlet 同规则）。

## 3. doFilter 的执行时机

```text
doFilter 内的三个关键位置：
  ① chain.doFilter 之前：请求前置（进入业务前）
  ② chain.doFilter：放行（进入下一个 Filter 或 Servlet）
  ③ chain.doFilter 之后：响应后置（业务完成后）

注意：不调用 chain.doFilter = 请求被拦截（不放行）
```

```java
public void doFilter(ServletRequest request, ServletResponse response,
                     FilterChain chain) throws IOException, ServletException {
    // ① 请求前置：进入业务前
    long start = System.currentTimeMillis();
    System.out.println("Filter 前置：请求开始");

    // ② 放行：进入下一个 Filter 或 Servlet
    chain.doFilter(request, response);

    // ③ 响应后置：业务完成后
    System.out.println("Filter 后置：耗时 " + (System.currentTimeMillis() - start) + "ms");
}
```

> 🎯 **三位置语义**：前置（拦截判断）、放行（必须调用）、后置（收尾统计）——**不调 chain.doFilter = 请求到此为止**（登录校验 Filter 的拦截就是这么实现的）。

## 4. 第一个 Filter 实战

```java
// 方式 1：注解（传统 Servlet 项目）
@WebFilter("/*")                // 拦截所有请求
public class LogFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
        System.out.println("LogFilter init");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        long start = System.currentTimeMillis();

        // 放行
        chain.doFilter(request, response);

        System.out.println("请求耗时: " + (System.currentTimeMillis() - start) + "ms");
    }

    @Override
    public void destroy() {
        System.out.println("LogFilter destroy");
    }
}
```

```xml
<!-- 方式 2：web.xml 配置（传统项目） -->
<filter>
    <filter-name>logFilter</filter-name>
    <filter-class>com.demo.LogFilter</filter-class>
</filter>
<filter-mapping>
    <filter-name>logFilter</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>
```

| 注册方式 | 适用 |
|----------|------|
| `@WebFilter` 注解 | Servlet 3.0+（需扫描） |
| web.xml | 传统 XML 项目 |
| Spring Boot | 见 [04](04-SpringBoot注册Filter.md) |

## 5. Filter 的作用范围

```text
url-pattern 匹配规则：
  /*           所有请求
  /api/*       /api 前缀
  *.jsp        所有 JSP
  /login       精确路径
  /api/orders  精确路径
```

| 拦截范围 | 配置 |
|----------|------|
| 全部 | `/*` |
| 指定前缀 | `/api/*` |
| 指定后缀 | `*.do` |
| 精确路径 | `/login` |

> ⚠️ **匹配纪律**：`/*` 会拦截静态资源（CSS/JS/图片）——静态资源通常不经过业务 Filter（或 Filter 内放行静态路径）。

## 6. 核心要点

> 🎯 **核心要点**：
> - 三方法：init 一次 / doFilter 每次 / destroy 一次（容器管理生命周期）；
> - doFilter 三位置：前置（拦截）、放行（chain.doFilter 必须调）、后置（收尾）；
> - **不调 chain.doFilter = 拦截**（登录校验的实现基础）；
> - Filter 单例：实例变量注意线程安全；
> - 注册三方式：@WebFilter / web.xml / Spring Boot（见 04）；
> - 范围匹配：/* 全拦、/api/* 前缀、精确路径——注意静态资源。

## 7. 参考来源

- [Jakarta Servlet 规范（Filter）](https://jakarta.ee/specifications/servlet/)
- [Baeldung：Servlet Filters](https://www.baeldung.com/java-servlet-filters)

---

**下一模块**：[02-FilterChain责任链](02-FilterChain责任链.md)　/　**返回总览**：[00-总览](00-Filter过滤器总览.md)
