# 01 DispatcherServlet 请求处理全链路

> 所有 SpringMVC 请求都经过同一条管道：DispatcherServlet 分发 → HandlerMapping 找处理器 → HandlerAdapter 绑定参数调方法 → 消息转换写响应 → 异常解析兜底——本模块把这条链路每个环节的职责与扩展点一次讲透

---

## 📚 目录

1. [前端控制器模式与请求生命周期](#1-前端控制器模式与请求生命周期)
2. [DispatcherServlet 初始化与自动配置](#2-dispatcherservlet-初始化与自动配置)
3. [HandlerMapping：请求到处理器的映射](#3-handlermapping请求到处理器的映射)
4. [HandlerAdapter：处理器执行与返回值处理](#4-handleradapter处理器执行与返回值处理)
5. [HandlerExceptionResolver 与 ViewResolver](#5-handlerexceptionresolver-与-viewresolver)
6. [一次请求的完整时序](#6-一次请求的完整时序)
7. [链路中的扩展点汇总](#7-链路中的扩展点汇总)

---

## 1. 前端控制器模式与请求生命周期

```text
请求 → Servlet 容器 → DispatcherServlet（前端控制器）
  ├── ① HandlerMapping 选择处理器（HandlerExecutionChain）
  ├── ② 拦截器 preHandle（见 06 篇）
  ├── ③ HandlerAdapter 执行处理器（参数绑定 → 业务 → 返回值）
  ├── ④ 拦截器 postHandle
  ├── ⑤ 异常 → HandlerExceptionResolver
  ├── ⑥ 返回值 → 消息转换器 / ViewResolver → 响应
  └── ⑦ 拦截器 afterCompletion
```

| 环节 | 组件 | 职责 |
|:---:|------|------|
| 分发 | DispatcherServlet | 请求的统一入口、按序调用各组件 |
| 映射 | HandlerMapping | 从请求找到 Handler + 拦截器链 |
| 执行 | HandlerAdapter | 参数解析、方法调用、返回值处理 |
| 异常 | HandlerExceptionResolver | 异常 → 响应 |
| 响应 | HttpMessageConverter / ViewResolver | 返回值 → HTTP 报文 |

> 🎯 **核心要点**：DispatcherServlet 本身不处理业务——它是"调度者"：把请求按固定顺序交给各策略组件。**每个环节都是一个可替换/可扩展的策略接口**，理解这点就理解了 SpringMVC 的全部可定制性。

## 2. DispatcherServlet 初始化与自动配置

### 2.1 初始化（Boot 自动完成）

```text
Boot 4 自动配置（DispatcherServletAutoConfiguration）：
  ① 创建 DispatcherServlet Bean（注册到 Servlet 容器，映射 "/"）
  ② 创建 DispatcherServletRegistrationBean（生命周期管理）
  ③ 装配 WebMvcConfigurer 与 MVC 组件（HandlerMapping/Adapter/Converter...）
```

```yaml
spring:
  mvc:
    servlet:
      load-on-startup: 1        # 启动即初始化（默认）
    pathmatch:
      matching-strategy: path_pattern_parser   # 7.x 默认 PathPattern
```

### 2.2 两种配置方式

| 方式 | 场景 |
|------|------|
| `@Configuration + WebMvcConfigurer`（推荐） | 定制 MVC 行为（拦截器/转换器/静态资源） |
| `@EnableWebMvc` | **完全接管**（关闭 Boot 自动配置，慎用） |

> ⚠️ **注意**：`@EnableWebMvc` 会关闭 Boot 的 MVC 自动配置——大多数场景不需要；只在"需要彻底替换默认行为"时使用。

## 3. HandlerMapping：请求到处理器的映射

### 3.1 映射器家族与解析顺序

```text
HandlerMapping（按优先级排序）：
  RequestMappingHandlerMapping   ← 注解 @RequestMapping（主用）
  WelcomePageHandlerMapping      ← 欢迎页
  SimpleUrlHandlerMapping        ← 显式 URL 映射（视图控制器等）
```

### 3.2 匹配过程

```java
// 请求：GET /api/users/1001  Accept: application/json

// ① 找出候选方法（RequestMappingInfo 匹配：路径/方法/头/参数/内容类型）
@GetMapping("/api/users/{id}")
public User getUser(@PathVariable Long id) { ... }

// ② 多候选时按"特异性"打分（精确 > 变量 > 通配）
//    /api/users/{id}        vs  /api/users/special  → 精确的赢
// ③ 组合成 HandlerExecutionChain（处理器 + 拦截器链）
```

**匹配失败的典型响应：**

| 场景 | 状态码 |
|------|:---:|
| 无处理器匹配 | 404 |
| 方法不允许（GET vs POST） | 405 |
| 媒体类型不支持（produces/consumes 不满足） | 406 |
| 参数缺失 | 400 |

> 💡 **匹配规则优先级**（面试点）：路径精确度 > 方法约束 > 参数/头约束 > 媒体类型约束；**变量与通配的命中由"模板特异性"打分决定**。

## 4. HandlerAdapter：处理器执行与返回值处理

### 4.1 适配器家族

```text
RequestMappingHandlerAdapter    ← 注解方法（主用）
HttpRequestHandlerAdapter       ← HttpRequestHandler
SimpleControllerHandlerAdapter  ← Controller 接口（旧式）
```

### 4.2 执行流程（RequestMappingHandlerAdapter）

```text
① 解析参数：MethodArgumentResolver 逐个解析（@PathVariable/@RequestBody/模型...）
   （03 篇详解参数绑定）
② 调用处理器方法（AOP 代理 → 业务逻辑）
③ 处理返回值：HandlerMethodReturnValueHandler
   @ResponseBody → HttpMessageConverter 写响应（04 篇）
   ModelAndView → ViewResolver 渲染
   @ResponseStatus → 状态码
   String → 视图名
④ 若方法抛异常 → 交给 HandlerExceptionResolver（05 篇）
```

| 返回值类型 | 处理方式 |
|-----------|---------|
| `@ResponseBody` 对象 | 消息转换器写 JSON/XML |
| `String`（无 @ResponseBody） | 视图名（JSP/Thymeleaf） |
| `ResponseEntity<T>` | 完整响应（状态码+头+体） |
| `ModelAndView` | 视图 + 模型 |
| `void` | 自行写响应或走视图 |
| `ProblemDetail` | RFC 7807 错误体（05 篇） |

## 5. HandlerExceptionResolver 与 ViewResolver

### 5.1 异常解析链（05 篇详解）

```text
HandlerExceptionResolver 顺序：
  ExceptionHandlerExceptionResolver   ← @ExceptionHandler/@RestControllerAdvice（主用）
  ResponseStatusExceptionResolver     ← @ResponseStatus / ResponseStatusException
  DefaultHandlerExceptionResolver     ← 框架异常 → 标准状态码（400/404/405...）
```

### 5.2 ViewResolver（服务端渲染场景）

```text
ViewResolver 链（按序尝试）：
  ContentNegotiatingViewResolver → ThymeleafViewResolver → InternalResourceViewResolver
```

> 💡 前后端分离时代 ViewResolver 已边缘化（@RestController 直接返回 JSON），但**理解其存在**能解释"为什么 String 返回会去找视图"这类经典问题。

## 6. 一次请求的完整时序

```text
请求 GET /api/users/1001

1. Servlet 容器 → Filter 链（Spring Security 等，见 06 篇）
2. DispatcherServlet.doDispatch()
3. getHandler() → RequestMappingHandlerMapping → HandlerExecutionChain
4. 拦截器 preHandle（顺序执行，任一返回 false 则中止）
5. handle() → RequestMappingHandlerAdapter
   a. resolveArguments：解析 @PathVariable id=1001（Converter 转 Long）
   b. 调用 getUser() → 业务层 → User 对象
   c. handleReturnValue：@ResponseBody → Jackson 序列化
6. HttpMessageConverter.write() → JSON 响应
7. 拦截器 postHandle / afterCompletion
8. 响应返回客户端（200 + JSON）
```

## 7. 链路中的扩展点汇总

| 环节 | 扩展方式 | 场景 |
|------|---------|------|
| 参数解析 | `HandlerMethodArgumentResolver` | 自定义注解取参数（登录用户注入） |
| 返回值处理 | `HandlerMethodReturnValueHandler` | 自定义返回包装 |
| 消息转换 | `HttpMessageConverter` / Advice | 自定义格式、加密、日志 |
| 异常处理 | `@RestControllerAdvice` / `HandlerExceptionResolver` | 全局错误体 |
| 拦截器 | `HandlerInterceptor` / `WebMvcConfigurer.addInterceptors` | 鉴权/审计 |
| 请求映射 | 自定义 `HandlerMapping` | 特殊路由规则 |

> 🎯 **核心要点**：SpringMVC 的请求链路是一条**固定顺序的策略管道**——映射（找谁）→ 适配（怎么调）→ 转换（怎么回）→ 异常（怎么兜）。所有"接口行为"问题（为什么 404/406、为什么参数绑不上、为什么异常不按我的格式返回）都能在这条链路上定位到具体环节。

---

**上一模块**：[00-SpringMVC知识体系总览](00-SpringMVC知识体系总览.md)　**下一模块**：[02-注解驱动开发：@Controller与请求映射](02-注解驱动开发：@Controller与请求映射.md)
