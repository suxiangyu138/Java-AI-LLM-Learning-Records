# 05 - DispatcherServlet 请求处理全链路

> 🎯 一个 HTTP 请求进入 Spring MVC 后的完整旅程 — doDispatch 八阶段 + 参数解析复合器 + HandlerAdapter 适配逻辑 + 返回值处理器分支。附带责任链设计分析

---

## 目录

1. [DispatcherServlet 的初始化](#1-dispatcherservlet-的初始化)
2. [doDispatch 八阶段全流程](#2-dodispatch-八阶段全流程)
3. [HandlerMapping：路由匹配内幕](#3-handlermapping路由匹配内幕)
4. [HandlerAdapter：方法调用三任务](#4-handleradapter方法调用三任务)
5. [参数解析复合器体系](#5-参数解析复合器体系)
6. [返回值处理器与内容协商](#6-返回值处理器与内容协商)
7. [异常处理与拦截器收尾](#7-异常处理与拦截器收尾)

---

## 1. DispatcherServlet 的初始化

### 1.1 继承链

```text
HttpServlet → HttpServletBean → FrameworkServlet → DispatcherServlet
```

### 1.2 initStrategies() — 九大组件一次装配

```java
// FrameworkServlet#initWebApplicationContext → onRefresh()
// → DispatcherServlet#onRefresh() → initStrategies()

protected void initStrategies(ApplicationContext context) {
    initMultipartResolver(context);         // ① 文件上传处理器
    initLocaleResolver(context);            // ② 地区/国际化
    initThemeResolver(context);             // ③ 主题
    initHandlerMappings(context);           // ④ ★ 路由映射器
    initHandlerAdapters(context);           // ⑤ ★ 方法适配器
    initHandlerExceptionResolvers(context); // ⑥ 异常处理器
    initRequestToViewNameTranslator(context);// ⑦ 视图名翻译
    initViewResolvers(context);             // ⑧ 视图解析器
    initFlashMapManager(context);           // ⑨ Flash 属性管理
}
// 其中 multipartResolver、localeResolver、themeResolver
// 从 BeanFactory 中按 BeanName 精确查找，其余按类型查找
```

---

## 2. doDispatch 八阶段全流程

```text
请求入口：service()/doGet()/doPost() → processRequest() → doService() → doDispatch()

┌──────────────────────────────────────────────────────────────────┐
│ doDispatch(HttpServletRequest req, HttpServletResponse resp)      │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│ ① checkMultipart(req) → 判断是否为文件上传请求                    │
│    是 → 包装为 MultipartHttpServletRequest；否 → 直接返回         │
│                                                                  │
│ ② getHandler(req) → ★ 路由匹配                                   │
│    遍历 HandlerMapping 集合 → 找到能处理该请求的 HandlerExecutionChain │
│    包含：HandlerMethod + HandlerInterceptor 链                      │
│    无匹配 → noHandlerFound() → 响应 404                            │
│                                                                  │
│ ③ getHandlerAdapter(handler) → 获取适配器                        │
│    返回 true：继续执行（匹配到了处理器）                            │
│                                                                  │
│ ④ mappedHandler.applyPreHandle(req, resp) → ★ 拦截器前置          │
│    正序执行所有拦截器的 preHandle()                                 │
│    返回 false → 仅执行 afterCompletion（已执行拦截器的收尾）       │
│                                                                  │
│ ⑤ ha.handle(req, resp, handler) → ★ 执行 Controller 方法         │
│    内部三任务：参数解析 → 反射调用 → 返回值处理                     │
│                                                                  │
│ ⑥ mappedHandler.applyPostHandle(req, resp, mv) → 拦截器后置      │
│    逆序执行 postHandle()（异常时不执行）                             │
│                                                                  │
│ ⑦ processDispatchResult → 结果处理                                │
│    有异常 → processHandlerException → 异常解析器链                │
│    REST → mv == null（@ResponseBody 直接写响应流）                 │
│    视图 → mv != null → render(mv) → ViewResolver → 视图渲染       │
│                                                                  │
│ ⑧ triggerAfterCompletion → ★ 拦截器收尾（finally 语义）           │
│    逆序执行 afterCompletion()，无论成功/异常都执行                  │
│    可获取异常信息（参数 ex） — 用于资源清理/日志记录                │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

**执行顺序记忆：** preHandle 正序（先注册先执行）、postHandle/afterCompletion **逆序**（先注册后执行）— 这是"栈"式责任链设计的必然结果。

---

## 3. HandlerMapping：路由匹配内幕

```java
// DispatcherServlet 启动时通过 initHandlerMappings() 装配
// 默认注册两个：
//   ① RequestMappingHandlerMapping（优先级最高，处理 @Controller + @RequestMapping）
//   ② BeanNameUrlHandlerMapping（按 name="/xxx" 匹配）

// 核心方法：AbstractHandlerMethodMapping#getHandlerInternal(HttpServletRequest)
// ① 去除上下文路径 → 得到 lookupPath
// ② 从内部 registry（MappingRegistry）精确匹配：
//    path + method(GET/POST) + headers + params + produces + consumes
// ③ 匹配成功 → 封装为 HandlerMethod（含 bean、方法、参数信息）
// ④ 与拦截器一起包装为 HandlerExecutionChain
```

**追问：** 多个 HandlerMapping 如何协调？→ 按注册顺序遍历（PriorityOrdered → Ordered → 其他），**第一个匹配即返回**。

---

## 4. HandlerAdapter：方法调用三任务

```java
// RequestMappingHandlerAdapter#handleInternal() → invokeHandlerMethod()
// 内部三大任务（ServletInvocableHandlerMethod 执行）：

// 任务1：参数解析
//   → 遍历参数列表 → 为每个参数找到对应的 ArgumentResolver
//   → RequestParamMethodArgumentResolver / PathVariableMethodArgumentResolver
//     / RequestResponseBodyMethodProcessor（负责 @RequestBody 的 JSON 反序列化）
//   → 调用 resolver.resolveArgument() 获取参数值

// 任务2：反射调用
//   → Method.invoke(bean, args...)
//   → 如果返回类型是 Callable/DeferredResult/...，走异步处理

// 任务3：返回值处理
//   → 遍历 ReturnValueHandler 集合
//   → @ResponseBody → RequestResponseBodyMethodProcessor
//     → HttpMessageConverter（Jackson → JSON）→ 直接写入 response
//   → 非 @ResponseBody → 封装为 ModelAndView
```

---

## 5. 参数解析复合器体系

```java
// 核心接口
public interface HandlerMethodArgumentResolver {
    boolean supportsParameter(MethodParameter parameter);
    Object resolveArgument(MethodParameter parameter, ...);
}

// 内置解析器（注册在 RequestMappingHandlerAdapter 中）
```

| 解析器 | 处理的注解/类型 | 核心逻辑 |
|--------|---------------|----------|
| RequestParamMethodArgumentResolver | @RequestParam | 从请求参数取值 + 类型转换 |
| PathVariableMethodArgumentResolver | @PathVariable | 从 URI 模板取值 |
| **RequestResponseBodyMethodProcessor** | **@RequestBody** | 通过 **HttpMessageConverter**（Jackson）将 JSON/XML 流反序列化为 POJO |
| ServletModelAttributeMethodProcessor | 无注解的 POJO | 按字段名绑定请求参数（对象绑定） |
| ServletRequestMethodArgumentResolver | HttpServletRequest 等 | 直接注入 Servlet 原生对象 |

> 💡 **调试技巧** — `HandlerMethodArgumentResolver` 执行前打印 `parameter` 和 `resolver.getClass()`，可看清每个参数走哪个解析器。

---

## 6. 返回值处理器与内容协商

```java
// 核心接口
public interface HandlerMethodReturnValueHandler {
    boolean supportsReturnType(MethodParameter returnType);
    void handleReturnValue(Object returnValue, ...);
}
```

### 6.1 两种响应路径

```text
路径①：REST API（@ResponseBody / @RestController）
  → handler.handleReturnValue → RequestResponseBodyMethodProcessor
  → 确定 MediaType（produces > Accept Header > 默认 JSON）
  → HttpMessageConverter.write(body, MediaType, response)
  → 响应写入 response 输出流（mv 直接标记为 "已处理"）

路径②：页面渲染
  → 返回 String/ModelAndView
  → ViewResolver 解析为 View 对象
  → view.render(model, request, response) → HTML 输出
```

### 6.2 内容协商（ContentNegotiationManager）

```text
确定响应格式的优先级：
① @RequestMapping(produces = "application/json")  ← 最优先
② 请求 Accept Header（浏览器默认 text/html）
③ 配置的默认值
```

**经典坑：** 浏览器访问接口 → 返回 404 视图 → 因为 Accept: text/html 优先级高于接口的 JSON → 加 `produces = "application/json"` 解决。

---

## 7. 异常处理与拦截器收尾

### 7.1 异常处理链

```java
// processDispatchResult → processHandlerException()
// 遍历 HandlerExceptionResolver 列表（按优先级）：

// ① ExceptionHandlerExceptionResolver  → @ExceptionHandler + @ControllerAdvice
// ② ResponseStatusExceptionResolver    → @ResponseStatus
// ③ DefaultHandlerExceptionResolver    → 标准错误（400/405/415 等，转发 /error）
```

**追问：** `@ControllerAdvice` 为什么能全局生效？→ `ExceptionHandlerExceptionResolver` 在初始化时扫描所有 `@ControllerAdvice` Bean，将其 `@ExceptionHandler` 方法收集为全局异常处理映射。

### 7.2 拦截器执行时序总结

```text
请求进入 → preHandle-1 → preHandle-2 → (Controller 执行)
           ↓ 失败则中断
         → postHandle-2 → postHandle-1（逆序）
         → afterCompletion-2 → afterCompletion-1（逆序，始终执行）

可访问性：
preHandle    — 可访问原始 request/response
postHandle   — 可访问 ModelAndView（JSON 返回时 mv 为 null）
afterCompletion — 可访问异常（ex 参数），进行资源清理
```

---

> 🎯 **核心要点**：DispatcherServlet = 前端控制器模式（统一入口）+ 责任链模式（可插拔拦截器）。面试被问"一个 HTTP 请求怎么到 Controller 的"：从 DispatcherServlet → HandlerMapping（路由）→ HandlerAdapter（三任务：参数解析/反射调用/返回值处理）→ 视图渲染或 JSON 输出。每个环节追踪哪个类、哪个方法。

**下一模块**：[06-事件驱动模型与ApplicationContext内部机制](06-事件驱动模型与ApplicationContext内部机制.md) / **返回总览**：[00-深度剖析总览](00-Spring生态深度剖析总览.md)
