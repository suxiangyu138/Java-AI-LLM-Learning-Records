# DispatcherServlet 请求处理链路
> 一个 Servlet 如何支撑整个 MVC：doDispatch 源码十步、四大组件（HandlerMapping/HandlerAdapter/HandlerExceptionResolver/ViewResolver）、拦截器链与异步返回类型——Spring MVC 的"心脏解剖"

## 📚 目录
1. [DispatcherServlet 的本质](#1-dispatcherservlet-的本质)
2. [doDispatch 源码十步](#2-dodispatch-源码十步)
3. [HandlerMapping 家族](#3-handlermapping-家族)
4. [HandlerAdapter 与参数解析](#4-handleradapter-与参数解析)
5. [返回值解析与消息转换](#5-返回值解析与消息转换)
6. [拦截器链执行顺序](#6-拦截器链执行顺序)
7. [异常处理链](#7-异常处理链)
8. [异步返回类型](#8-异步返回类型)
9. [Boot 4：内置 API 版本管理](#9-boot-4内置-api-版本管理)

## 1. DispatcherServlet 的本质

```text
DispatcherServlet extends HttpServlet
  ├─ 继承自 FrameworkServlet（覆盖 service → doService）
  ├─ 映射路径 "/"（缺省 Servlet，见 Servlet 体系 §4）
  └─ 把一个 Servlet 变成"路由器"：
        URL → HandlerMapping → HandlerAdapter → 方法执行 → 返回值 → 响应
```

> 🎯 与 [Servlet 体系](../Servlet/08-生产实践与面试题.md) 的关系：DispatcherServlet 没有发明新协议——它只是**重写了 HttpServlet 的分发逻辑**（把"HTTP 方法 → doXxx"升级为"URL → Controller 方法"）。理解 Servlet 再读源码，一切顺理成章。

## 2. doDispatch 源码十步

```java
// DispatcherServlet.doDispatch()（Framework 7 源码，核心骨架）
protected void doDispatch(HttpServletRequest request, HttpServletResponse response) {
    HttpServletRequest processedRequest = request;
    HandlerExecutionChain mappedHandler = null;
    boolean multipartRequestParsed = false;

    // ① 检查是否是 multipart 请求（文件上传包装 request）
    processedRequest = checkMultipart(request);
    multipartRequestParsed = (processedRequest != request);

    // ② 找 Handler：遍历 HandlerMapping 链，第一个返回非 null 的胜出
    mappedHandler = getHandler(processedRequest);

    // ③ 找 HandlerAdapter：适配当前 Handler 类型
    HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());

    // ④ 处理 Last-Modified（缓存协商）
    String method = request.getMethod();
    boolean isGet = "GET".equals(method);
    if (isGet || "HEAD".equals(method)) {
        long lastModified = ha.getLastModified(request, mappedHandler.getHandler());
        // ... 304 逻辑
    }

    // ⑤ 执行拦截器 preHandle（顺序执行，false 则短路）
    if (!mappedHandler.applyPreHandle(processedRequest, response)) {
        return;   // preHandle 返回 false → 请求到此结束
    }

    // ⑥ 执行 Handler（Controller 方法）→ 拿到 ModelAndView
    ModelAndView mv = ha.handle(processedRequest, response, mappedHandler.getHandler());

    // ⑦ 异步处理：返回 AsyncWebRequest 相关对象时直接返回（见 §8）
    if (asyncManager.isConcurrentHandlingStarted()) {
        return;
    }

    // ⑧ 默认视图名处理（ModelAndView 无视图名时）
    applyDefaultViewName(processedRequest, mv);

    // ⑨ 执行拦截器 postHandle（逆序执行）
    mappedHandler.applyPostHandle(processedRequest, response, mv);

    // ⑩ 视图渲染 + 拦截器 afterCompletion（finally 语义）
    processDispatchResult(processedRequest, response, mappedHandler, mv, dispatchException);
}
```

| 步骤 | 组件 | 作用 |
|:---:|------|------|
| ① | MultipartResolver | 文件上传包装（StandardServletMultipartResolver） |
| ② | HandlerMapping | 找到"谁能处理这个 URL" |
| ③ | HandlerAdapter | 找到"怎么执行这个 Handler" |
| ④ | - | HTTP 缓存协商 |
| ⑤ | HandlerInterceptor | preHandle 拦截（鉴权等） |
| ⑥ | HandlerAdapter | 真正执行 Controller 方法 |
| ⑨ | HandlerInterceptor | postHandle 后置（异常时不执行） |
| ⑩ | ViewResolver | 视图渲染；afterCompletion 收尾（恒执行） |

> ⚠️ **postHandle 与 afterCompletion 的差别**：Controller 抛异常时，postHandle **不执行**，afterCompletion **必执行**（finally 语义）——资源清理（MDC 清除、ThreadLocal.remove）必须放 afterCompletion，不能放 postHandle。

## 3. HandlerMapping 家族

| HandlerMapping | 匹配对象 | 场景 |
|----------------|---------|------|
| `RequestMappingHandlerMapping` | @RequestMapping/@GetMapping 方法 | **主路由（90% 请求）** |
| `SimpleUrlHandlerMapping` | 显式 URL → Handler | 手动映射（视图控制器） |
| `ResourceHttpRequestHandler` | 静态资源 | 静态文件（见 06） |
| `WelcomePageHandlerMapping` | `/` | 欢迎页 |
| `PathPatternParser` | 路径模式 | Framework 7 默认匹配器 |

```java
// Boot 4 / Framework 7 关键行为变更：尾斜杠不再等价
@GetMapping("/users")
public List<User> list() { ... }

// 请求 /users/   → 404（Boot 3 时代 200）
// 需要旧行为 → 用 UrlHandlerFilter（Framework 提供）恢复
```

> 🎯 匹配顺序即注册顺序：请求先过 RequestMappingHandlerMapping（业务路由），未命中再走静态资源——所以**业务路径与静态资源重名时，业务优先**。

## 4. HandlerAdapter 与参数解析

```java
// RequestMappingHandlerAdapter 内部的两大解析链：
// ① 参数解析器链（HandlerMethodArgumentResolver）—— 按顺序，第一个"支持"的胜出
// ② 返回值处理器链（HandlerMethodReturnValueHandler）
```

| 常见参数解析器 | 解析的注解/类型 |
|---------------|----------------|
| `PathVariableMethodArgumentResolver` | @PathVariable |
| `RequestParamMethodArgumentResolver` | @RequestParam、简单类型、MultipartFile |
| `RequestBodyMethodArgumentResolver` | @RequestBody（JSON → 对象，经消息转换器） |
| `RequestHeaderMethodArgumentResolver` | @RequestHeader |
| `CookieValueMethodArgumentResolver` | @CookieValue |
| `ModelAttributeMethodProcessor` | @ModelAttribute、对象绑定 |
| `ServletModelAttributeMethodProcessor` | 表单对象绑定 |

```java
@RestController
public class UserController {
    @PostMapping("/users")
    public User create(@Valid @RequestBody UserCreateDTO dto) {  // 解析链：RequestBody → Jackson 反序列化
        return userService.create(dto);
    }
}
```

> ⚠️ 参数绑定失败的处理：@RequestBody JSON 格式错误 → `HttpMessageNotReadableException` → 默认 400；@Valid 校验失败 → `MethodArgumentNotValidException` → 400。两者都由 [05-错误处理机制深潜](05-错误处理机制深潜.md) 的 @ExceptionHandler 兜底统一格式。

## 5. 返回值解析与消息转换

```text
Controller 返回 → HandlerMethodReturnValueHandler（按返回类型选处理器）
  ├─ @ResponseBody / @RestController → RequestResponseBodyMethodProcessor
  │     └─ HttpMessageConverter 链：按 contentType + 类型匹配
  │           ├─ MappingJackson2HttpMessageConverter（Jackson 3，默认）
  │           ├─ StringHttpMessageConverter（text/plain）
  │           └─ ByteArrayHttpMessageConverter
  ├─ String（视图名）→ ViewNameMethodReturnValueHandler → ViewResolver
  ├─ ModelAndView → 视图渲染
  └─ void / ResponseEntity → 直接写响应
```

| 返回类型 | 处理器 | 行为 |
|---------|--------|------|
| 对象 + @ResponseBody | RequestResponseBodyMethodProcessor | JSON 序列化（Jackson 3） |
| ResponseEntity<T> | HttpEntityMethodProcessor | 自定义状态码 + body |
| String | ViewNameMethodReturnValueHandler | 视图名（前后端分离时是 JSON 字符串，易踩坑） |
| ModelAndView | ModelAndViewMethodReturnValueHandler | 视图渲染 |
| void | 无返回 | 空 200 |

> ⚠️ **@RestController 方法返回 String 返回的是 JSON 字符串还是视图名？** 是 JSON 字符串（@RestController 自带 @ResponseBody）——"返回 String 会被当视图名"只发生在 @Controller 时代。前后端分离项目全用 @RestController，无歧义。

### 5.1 Jackson 3 定制（Boot 4 破坏性变更）

```java
// Boot 4 默认 Jackson 3（tools.jackson 包）：日期 ISO-8601、字段字母序、
// FAIL_ON_NULL_FOR_PRIMITIVES=true（基本类型字段 JSON 里缺值直接报错！）

// 全局定制：消息转换器定制 Bean（替代废弃的 HttpMessageConverters）
@Configuration
public class JacksonConfig {
    @Bean
    public ServerHttpMessageConvertersCustomizer jsonCustomizer() {
        return converters -> converters.add(new MappingJackson2HttpMessageConverter(
                JsonMapper.builder()
                        .enable(SerializationFeature.INDENT_OUTPUT)
                        .build()));
    }
}

// 应急开关：恢复 Jackson 2 默认行为（迁移期临时用）
spring.jackson.use-jackson2-defaults=true
```

| Jackson 3 破坏点 | 影响 |
|-----------------|------|
| 日期默认 ISO-8601（非 `yyyy-MM-dd HH:mm:ss`） | 前端解析格式变化 |
| 字段字母序（非声明序） | JSON 字段顺序变化 |
| 基本类型缺值报错 | 前端漏传必填字段直接 400/500 |
| 包名 tools.jackson | 显式 import com.fasterxml.jackson 的代码需改 |

## 6. 拦截器链执行顺序

```text
请求 → Filter 链（容器层）→ DispatcherServlet → preHandle 1 → preHandle 2
      → Controller 方法 → postHandle 2 → postHandle 1
      → 视图渲染 → afterCompletion 2 → afterCompletion 1 → Filter 链返回
```

| 回调 | 时机 | 异常时 | 典型用途 |
|------|------|:---:|---------|
| preHandle | Controller 前 | 返回 false 短路 | 鉴权、限流 |
| postHandle | Controller 后、渲染前 | **不执行** | 响应头、日志 |
| afterCompletion | 渲染后（finally） | **必执行** | 清理资源、耗时统计 |

```java
// 拦截器注册：WebMvcConfigurer（详见 03）
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/login", "/static/**");
    }
}
```

> 🎯 与 Filter 的分工：**Filter 在容器层**（进 Servlet 前，能拦一切含静态资源）、**Interceptor 在 MVC 层**（能拿到 HandlerMethod 与 ModelAndView）。Boot 中的注册与顺序控制详见 [04-Filter与Interceptor注册与顺序](04-Filter与Interceptor注册与顺序.md)。

## 7. 异常处理链

```text
Controller 抛异常
  → HandlerExceptionResolver 链（按顺序，第一个处理的胜出）：
      ① ExceptionHandlerExceptionResolver —— @ControllerAdvice / @ExceptionHandler（推荐）
      ② ResponseStatusExceptionResolver —— @ResponseStatus、ResponseStatusException
      ③ DefaultHandlerExceptionResolver —— Spring MVC 内置异常（404/405/415/400 等）
  → 都未处理 → 抛给容器 → /error 兜底（见 05）
```

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) { ... }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException e) { ... }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) { ... }   // 兜底：隐藏细节 + 记录日志
}
```

> 💡 面试深度：能说出"@ExceptionHandler 的处理入口其实是 `ExceptionHandlerExceptionResolver`，内部按 `@ControllerAdvice` 排序、按异常类型做精确匹配（子类优先）"就是源码级理解。

## 8. 异步返回类型

| 返回类型 | 行为 | 适用 |
|---------|------|------|
| `Callable<T>` | 容器线程池执行，期间释放请求线程 | 简单异步 |
| `WebAsyncTask<T>` | Callable + 超时/回调 | 带超时的异步 |
| `DeferredResult<T>` | 外部线程完成任务后 setResult | **MQ 消费/事件驱动**（典型） |
| `ResponseBodyEmitter` | 流式多次写响应 | 大结果分块推送 |
| `SseEmitter` | **SSE 服务端推送** | 实时通知/进度 |
| `StreamingResponseBody` | 响应体流式写 | 大文件/流式下载 |

```java
// DeferredResult：请求线程立即释放，业务完成后再响应（配 MQ/线程池）
@GetMapping("/async/order")
public DeferredResult<OrderVO> asyncOrder(@RequestParam String orderId) {
    DeferredResult<OrderVO> result = new DeferredResult<>(30_000L);  // 30s 超时
    orderService.submitAsync(orderId, result::setResult);           // 回调写结果
    return result;    // Controller 立即返回，容器线程释放
}

// SSE：服务端推送（前端 EventSource 消费）
@GetMapping(value = "/sse/notify", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter sseNotify() {
    SseEmitter emitter = new SseEmitter(60_000L);
    taskExecutor.execute(() -> {
        try {
            emitter.send(SseEmitter.event().data("progress:50%"));
            emitter.send(SseEmitter.event().data("progress:100%"));
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    });
    return emitter;
}
```

> ⚠️ 异步请求必须显式完成（complete/超时/错误），否则连接挂账；异步场景决策与避坑详见 [Servlet 异步体系](../Servlet/06-异步处理与非阻塞IO.md) §7（机制同源，Boot 只是返回类型包装）。

## 9. Boot 4：内置 API 版本管理

```yaml
# Boot 4 新增：MVC/WebFlux 原生 API 版本管理
spring:
  mvc:
    apiversion:
      enabled: true
      header-name: X-API-Version        # 方式一：请求头版本
      # parameter-name: version          # 方式二：查询参数
      # media-type-prefix: application/vnd.demo.v   # 方式三：媒体类型
```

| 方式 | 配置 | 示例 |
|------|------|------|
| 请求头 | `header-name` | `X-API-Version: v2` |
| 查询参数 | `parameter-name` | `/api/users?version=v2` |
| 媒体类型 | `media-type-prefix` | `Accept: application/vnd.demo.v2+json` |

> 💡 定位：这是 Boot 4 对"接口多版本共存"痛点的官方解法（此前靠自定义 HandlerMapping 或网关层路由）；面试可作差异化加分点，生产多版本场景可直接启用。

---

**下一模块**：[03-WebMvcConfigurer定制全解](03-WebMvcConfigurer定制全解.md) / **返回总览**：[00-SpringBootWeb总览](00-SpringBootWeb总览.md)
