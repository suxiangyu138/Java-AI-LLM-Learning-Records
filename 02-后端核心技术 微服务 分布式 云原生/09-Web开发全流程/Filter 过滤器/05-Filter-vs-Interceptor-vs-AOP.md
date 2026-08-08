# 05-Filter vs Interceptor vs AOP
> 三层拦截机制的定位分工：Filter（容器层）、Interceptor（MVC 层）、AOP（Bean 层）——"面试必问的对比"

## 📚 目录
1. [三层拦截总览](#1-三层拦截总览)
2. [Filter：容器层](#2-filter容器层)
3. [Interceptor：MVC 层](#3-interceptormvc-层)
4. [AOP：Bean 层](#4-aopbean-层)
5. [执行顺序](#5-执行顺序)
6. [选型决策](#6-选型决策)
7. [核心要点](#7-核心要点)
8. [参考来源](#8-参考来源)

## 1. 三层拦截总览

```text
请求经过的三层拦截：
客户端 → [Filter（容器层）] → DispatcherServlet → [Interceptor（MVC 层）]
      → Controller → [AOP（Bean 层）] → 业务方法
```

| 层 | 框架 | 拦截对象 | 能做什么 |
|----|------|---------|---------|
| **Filter** | Servlet 容器 | 所有请求（Servlet 之前） | 编码/CORS/登录（粗粒度） |
| **Interceptor** | Spring MVC | Handler 方法（Controller 前后） | 登录/权限/日志（细粒度） |
| **AOP** | Spring | Bean 方法 | 事务/日志/性能（最细） |

> 🎯 **一句话记忆**：**Filter 拦请求（最外层）、Interceptor 拦 Controller（中间层）、AOP 拦方法（最内层）**——粒度从粗到细，离业务从远到近。

## 2. Filter：容器层

```java
// Servlet 容器层：所有请求（含静态资源）
public class LoginFilter implements Filter {
    public void doFilter(req, resp, chain) {
        // 请求进入 Servlet 容器后的第一道关卡
        chain.doFilter(req, resp);
    }
}
```

| 特点 | 说明 |
|------|------|
| 层级 | Servlet 容器（与 Spring 无关） |
| 拦截范围 | 所有请求（含静态资源） |
| 时机 | DispatcherServlet 之前 |
| 典型场景 | 编码/CORS/登录白名单/限流 |
| 拿到 | ServletRequest（原始） |
| 限制 | 拿不到 HandlerMethod（不知道调哪个方法） |

## 3. Interceptor：MVC 层

```java
// Spring MVC 层：Handler 方法前后
public class LoginInterceptor implements HandlerInterceptor {

    // ① Controller 之前（前置）
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        if (handler instanceof HandlerMethod) {
            // 可以拿到 HandlerMethod（方法/注解信息）
        }
        return true;       // false = 拦截
    }

    // ② Controller 之后、视图渲染前（后置）
    public void postHandle(...) {}

    // ③ 请求完成后（最终，含异常）
    public void afterCompletion(...) {}
}
```

| 特点 | 说明 |
|------|------|
| 层级 | Spring MVC |
| 拦截范围 | Controller 方法（Handler） |
| 时机 | DispatcherServlet 内、Handler 前后 |
| 典型场景 | 登录/权限（可拿注解）、日志 |
| 拿到 | `HandlerMethod`（方法/注解！） |
| 三方法 | preHandle / postHandle / afterCompletion |

> 🎯 **Interceptor 对比 Filter 的核心优势**：**能拿到 HandlerMethod**——可以读取 Controller 方法上的注解（如 @RequireLogin）做细粒度控制；这是 Filter 做不到的。

## 4. AOP：Bean 层

```java
// Spring AOP：Bean 方法级
@Aspect
@Component
public class LogAspect {

    @Around("@annotation(com.demo.LogTime)")     // 只切标注了注解的方法
    public Object logTime(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = pjp.proceed();            // 执行目标方法
        System.out.println("耗时: " + (System.currentTimeMillis() - start) + "ms");
        return result;
    }
}
```

| 特点 | 说明 |
|------|------|
| 层级 | Spring 容器（Bean 代理） |
| 拦截范围 | Bean 方法（Service/Controller/任意） |
| 时机 | 方法执行前后/环绕 |
| 典型场景 | 事务、日志、权限（方法级）、性能 |
| 拿到 | 方法签名/参数/注解 |
| 注意 | 代理限制（自调用失效，见 AOP 体系） |

> 💡 **AOP 与 Interceptor 的区别**：Interceptor 只拦"Controller 的 Handler 方法"；AOP 可切**任意 Bean 的方法**（Service 层日志、Repository 监控）——**层级更深入、范围更广**。

## 5. 执行顺序

```text
完整执行顺序：
① Filter preHandle 之前（doFilter 前置）
② DispatcherServlet 分发
③ Interceptor.preHandle（返回 false 则拦截）
④ Controller 方法（可能被 AOP 环绕）
⑤ Interceptor.postHandle（视图渲染前）
⑥ Interceptor.afterCompletion（请求完成）
⑦ Filter doFilter 后置

三层关系：Filter 包裹 Interceptor，Interceptor 包裹 Controller/AOP
```

```text
返回顺序（洋葱）：
Filter 前置 → Interceptor.preHandle → Controller（AOP 环绕）
  → Interceptor.postHandle → Interceptor.afterCompletion → Filter 后置
```

> 🎯 **面试必答顺序**：**Filter 前置 → Interceptor.preHandle → Controller（AOP）→ Interceptor.postHandle/afterCompletion → Filter 后置**——三层各管一段，外层包裹内层。

## 6. 选型决策

```text
选型决策树：
  需要拦截所有请求（含静态资源）？
    ├─ 是 → Filter（编码/CORS/全局登录白名单）
    └─ 否 ↓
  需要读取 Controller 方法/注解？
    ├─ 是 → Interceptor（细粒度权限/登录）
    └─ 否 ↓
  需要切 Service 等方法？
    ├─ 是 → AOP（事务/方法级日志/性能）
    └─ 否 → 看具体场景
```

| 场景 | 推荐 |
|------|------|
| 编码/CORS | Filter |
| 登录/权限（Controller 级） | **Interceptor**（可拿注解） |
| 登录白名单（全局） | Filter 或 Interceptor |
| 事务 | AOP（@Transactional） |
| 方法级日志/性能 | AOP |
| 需要 HandlerMethod | Interceptor |
| 需要静态资源 | Filter |

> 🎯 **分工总结**：**粗粒度全局（Filter）+ 细粒度控制器（Interceptor）+ 方法级业务（AOP）**——三个层次配合，各自处理适合自己的横切逻辑。

## 7. 核心要点

> 🎯 **核心要点**：
> - 三层定位：Filter（容器层，拦请求）、Interceptor（MVC 层，拦 Controller）、AOP（Bean 层，切方法）；
> - Interceptor 独有能力：拿到 HandlerMethod（读方法注解）；
> - AOP 独有能力：切任意 Bean 方法（Service 层也行）；
> - 执行顺序：Filter → Interceptor.preHandle → Controller（AOP）→ postHandle → afterCompletion → Filter 后置；
> - 选型：编码/CORS 用 Filter、登录权限用 Interceptor、事务/方法日志用 AOP；
> - 面试主线：三层对比表 + 执行顺序 + 各自能拿到什么。

## 8. 参考来源

- [Spring MVC HandlerInterceptor 文档](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-servlet/handlermapping-interceptor.html)
- [Spring AOP 文档](https://docs.spring.io/spring-framework/reference/core/aop.html)
- [Jakarta Servlet Filter 规范](https://jakarta.ee/specifications/servlet/)

---

**下一模块**：[06-生产实践与面试题](06-生产实践与面试题.md)　/　**返回总览**：[00-总览](00-Filter过滤器总览.md)
