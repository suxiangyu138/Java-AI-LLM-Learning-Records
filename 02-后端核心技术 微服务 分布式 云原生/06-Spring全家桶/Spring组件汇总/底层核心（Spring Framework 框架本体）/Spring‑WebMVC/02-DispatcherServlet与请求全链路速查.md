# 02 DispatcherServlet 与请求全链路速查

> 请求十步、HandlerMapping/HandlerAdapter 家族、拦截器链位置——"一次请求到底经过谁"的完整答案

---

## 📚 目录

1. [DispatcherServlet：中央调度器](#1-dispatcherservlet中央调度器)
2. [请求处理十步](#2-请求处理十步)
3. [HandlerMapping 家族](#3-handlermapping-家族)
4. [HandlerAdapter 家族](#4-handleradapter-家族)
5. [拦截器链的嵌入位置](#5-拦截器链的嵌入位置)

---

## 1. DispatcherServlet：中央调度器

```text
DispatcherServlet = 前端控制器（Front Controller）模式的 Servlet 实现
  继承链：HttpServletBean → FrameworkServlet → DispatcherServlet
  生命周期：
    init()：从 WebApplicationContext 收集 8 类组件（见 01-模块清单）
    service() → doDispatch()：每次请求的中央调度
  特点：Servlet 只做"调度"，业务全在可插拔组件中
```

| 阶段 | 行为 |
|------|------|
| 初始化 | `initStrategies`：装配 HandlerMapping/Adapter/异常解析/视图解析等 |
| 请求 | `doDispatch`：路由 → 拦截 → 执行 → 响应 |
| 特殊 | `getLastModified`、异步请求处理（WebAsyncManager） |
| 卸载 | destroy 释放资源 |

> 🎯 **核心要点**：DispatcherServlet 的设计是"**一个 Servlet + 一堆策略接口**"——它本身不含路由/执行逻辑，全部委托给装配好的组件；这是面试答"MVC 为什么可扩展"的根基。

### 生命周期与请求入口链

```text
Servlet 容器视角：
  加载：web.xml / 注解 / 编程注册 → Servlet 实例
  init()：HttpServletBean（读 init-param 配置）
    → FrameworkServlet.initWebApplicationContext（初始化子容器 + 发布事件）
    → DispatcherServlet.initStrategies（装配 8 类组件）
  请求：doGet/doPost → processRequest → doService → doDispatch
  destroy()：清理资源
```

| doService 放入 request 的属性 | 用途 |
|------------------------------|------|
| `WEB_APPLICATION_CONTEXT_ATTRIBUTE` | 子容器（Servlet WebApplicationContext） |
| `LOCALE_RESOLVER_ATTRIBUTE` | 语言解析器 |
| `THEME_RESOLVER_ATTRIBUTE` / `THEME_SOURCE_ATTRIBUTE` | 主题解析器/主题源 |

> 💡 面试细节：`FrameworkServlet` 每次请求都把子容器放进 request 属性——这是 JSP 里能通过 request 拿到容器 Bean 的机制基础；也是"过滤器里注入不了 MVC 子容器 Bean"的原因（过滤器在父容器中，见 [08 篇第 1 节](08-集成地图与常见问题.md)）。

### 初始化失败的表现与排查

| 现象 | 根因 | 排查 |
|------|------|------|
| 启动报"无 HandlerMapping" | 容器里没有路由组件 | 检查是否误删自动配置 / 继承错配置基类 |
| 启动报重复映射 | 两个控制器方法条件完全等价 | 启动日志会列出冲突的类与方法 |
| 启动后所有请求 404 | DispatcherServlet 未注册或映射路径不对 | 检查 servlet-mapping 或 Boot 自动配置 |
| 启动极慢 | 大量控制器 + 复杂路径模式预编译 | 属正常（一次性），生产可监控启动耗时 |

> ⚠️ 初始化阶段的问题**全部在启动期暴露**（重复映射、无适配器）——这是 MVC 设计红利：路由错误不会拖到线上才炸；看到启动日志里的 `RequestMappingHandlerMapping` 行就说明装配完成。

### 虚拟线程下的请求入口变化（Boot 4）

Boot 4 默认 `server.tomcat.threads.virtual-enabled=true`：每个请求运行在虚拟线程上，doService → doDispatch 链路不变，但线程模型从"平台线程池复用"变为"虚拟线程按需创建"——**长阻塞 IO 不再占满线程池**，接口并发能力显著提升；代价是 ThreadLocal 不再随线程回收，请求级状态应放 request 属性或请求作用域 Bean（见 [Spring-Web-05](../Spring‑Web/05-Web上下文与作用域速查.md)）。

## 2. 请求处理十步

```text
doDispatch 十步（面试背诵版）：
  1. 检查 multipart 请求（MultipartResolver）
  2. getHandler(request)：遍历 HandlerMapping 找到 HandlerExecutionChain
  3. 无 handler → 404（NoHandlerFoundException 或错误页）
  4. getHandlerAdapter(handler)：找能执行该 handler 的适配器
  5. 执行拦截器 preHandle 链（任一 false → 短路返回）
  6. adapter.handle：参数解析 → 控制器方法执行 → 返回值处理
  7. 执行拦截器 postHandle 链
  8. 异常：HandlerExceptionResolver 解析（跳到错误视图/ProblemDetail）
  9. 渲染：ViewResolver 解析视图 或 转换器直出（@ResponseBody）
  10. 执行拦截器 afterCompletion（finally 语义）
```

> 💡 记忆锚点：**路由（2）→ 拦前（5）→ 执行（6）→ 拦后（7）→ 异常（8）→ 渲染（9）→ 收尾（10）**；拦截器三方法分别落在 5/7/10——面试画请求链路图按此。

### doDispatch 源码级对照（7.x 简化注释）

```java
// DispatcherServlet.doDispatch —— 十步与源码行的对应（简化版）
protected void doDispatch(HttpServletRequest request, HttpServletResponse response) {
    HttpServletRequest processedRequest = request;
    HandlerExecutionChain mappedHandler = null;
    ModelAndView mv = null;
    Exception dispatchException = null;

    try {
        processedRequest = checkMultipart(request);              // ① multipart 解析
        mappedHandler = getHandler(processedRequest);            // ② 遍历 HandlerMapping 路由
        if (mappedHandler == null) {                             // ③ 无处理器
            noHandlerFound(processedRequest, response);          //    → 404
            return;
        }
        HandlerAdapter ha = getHandlerAdapter(mappedHandler);    // ④ 找适配器

        if (!mappedHandler.applyPreHandle(processedRequest, response)) {
            return;                                              // ⑤ preHandle 短路
        }
        mv = ha.handle(processedRequest, response, mappedHandler.getHandler()); // ⑥ 执行

        mappedHandler.applyPostHandle(processedRequest, response, mv);          // ⑦ postHandle
    }
    catch (Exception ex) {
        dispatchException = ex;                                  // ⑧ 捕获异常
    }
    processDispatchResult(processedRequest, response, mappedHandler, mv, dispatchException); // ⑨ 渲染/错误
    mappedHandler.triggerAfterCompletion(processedRequest, response, null);     // ⑩ afterCompletion
}
```

| 十步 | 典型异常 | 默认状态码 |
|------|---------|:---:|
| ② 路由 | `NoHandlerFoundException` | 404 |
| ⑥ 参数解析 | `MissingServletRequestParameterException` | 400 |
| ⑥ 转换读入 | `HttpMessageNotReadableException` | 400 |
| ⑥ 方法不符 | `HttpRequestMethodNotSupportedException` | 405 |
| ⑧ 业务异常 | 走 @ExceptionHandler 链 | Advice 决定 |
| ⑨ 转换写出 | `HttpMediaTypeNotAcceptableException` | 406 |

> 🎯 源码结论：**十步不是概念而是真实代码**——`doDispatch` 一个方法里就长这样；面试背十步 + 能画出 doDispatch 骨架 = 明显区分度。

### 十步的分段记忆

| 段 | 步骤 | 一句话责任 | 类比 |
|----|------|-----------|------|
| 准备段 | ①-④ | 解析上传、找路由、找适配器 | 前台接待：问清来意、查登记表、找服务窗口 |
| 执行段 | ⑤-⑦ | 拦前检查、执行业务、拦后收拾 | 柜台办理：安检、办业务、复核 |
| 收尾段 | ⑧-⑩ | 异常兜底、渲染响应、清理资源 | 离店结算：出问题投诉台、打印小票、收拾桌面 |

> 💡 记忆心法：**"查（路由）→ 干（执行）→ 收（渲染清理）"三段式**——面试默写时先写三段骨架，再补每一段的细步骤，比从头硬背十步快得多。

## 3. HandlerMapping 家族

| 实现 | 匹配方式 | 优先级（order） |
|------|---------|:---:|
| `RequestMappingHandlerMapping` | `@RequestMapping` 注解（**主路由**） | 0（最高） |
| `WelcomePageHandlerMapping` | `/` → index 页 | -1（Boot） |
| `ResourceHandlerMapping` | 静态资源映射（`/**` 兜底） | 最低 |
| `SimpleUrlHandlerMapping` | XML/编程式 URL 直配 | 自定义 |
| `BeanNameUrlHandlerMapping` | Bean 名即 URL（历史） | 低 |

```java
// 路由选择过程：按 order 排序逐个 try —— 命中即用
// @RequestMapping("/api/orders") → RequestMappingHandlerMapping 命中
// /index.html → ResourceHandlerMapping 命中（静态资源兜底）
```

> 🎯 **核心要点**：多个 HandlerMapping 按 order 排序、**先命中先用**——注解路由（order 0）优先于静态资源；面试答"为什么 /api/** 和静态资源不冲突"→ 路由优先级。

### 注解路由的初始化与匹配（源码细节）

```text
RequestMappingHandlerMapping 两个阶段：
  ① 初始化（initHandlerMethods，容器启动时）：
     扫描所有 @Controller Bean → 收集 @RequestMapping 方法
     → 构建 RequestMappingInfo（路径/方法/参数/头/consumes/produces/version）
     → 放入 MappingRegistry（模式 → HandlerMethod 映射表，含 PathPattern 预编译）
  ② 匹配（getHandlerInternal，请求到达时）：
     解析请求路径 → 与映射表匹配（哈希 + 模式树）
     → 多命中时按"条件精确度"排序
     → 返回 HandlerExecutionChain（HandlerMethod + 匹配的拦截器列表）
```

| 排序维度（多命中时） | 说明 |
|---------------------|------|
| 路径特异性 | 精确路径 > `{var}` > `*` > `**` |
| 方法限定 | 限定 GET 的映射优于未限定的 |
| 条件丰富度 | 头/参数/媒体类型条件越多优先级越高 |
| 版本条件（7.0） | 固定版本优于基线版本（`1.2` > `1.2+`） |

| 路由常见坑 | 现象 | 根因 |
|-----------|------|------|
| 启动报重复映射 | 两个映射条件完全等价 | 条件设计重复（如双 `@GetMapping("/x")`） |
| 方法存在却 405 | 路径命中、方法不匹配 | GET 请求打到 POST 映射 |
| 映射生效但拦截器没跑 | 排除路径误写 | exclude 优先级高于 include |
| 路由 404 但代码无误 | 控制器未扫描到 | @ComponentScan 路径覆盖不全 |

> 💡 路由初始化发生在**容器启动时**（非首次请求）——映射写错启动即报错；`/actuator/mappings`（Boot）可在线查看全部路由与条件。

### 自定义 HandlerMapping（扩展示例）

```java
// 场景：按请求头 X-Route 决定处理器（灰度/租户路由）
@Component
public class HeaderRouteHandlerMapping extends SimpleUrlHandlerMapping {

    public HeaderRouteHandlerMapping() {
        setOrder(1);                                       // 高于静态资源、低于注解路由
    }

    @Override
    protected HandlerExecutionChain getHandlerInternal(HttpServletRequest request) {
        String route = request.getHeader("X-Route");
        if ("legacy".equals(route)) {
            return getChain("/legacy-handler", request);   // 命中遗留处理器
        }
        return super.getHandlerInternal(request);
    }
}
```

| 自定义路由要点 | 说明 |
|--------------|------|
| 继承谁 | 通常继承 `AbstractHandlerMapping` 或 `SimpleUrlHandlerMapping` |
| order 定优先级 | 数值越小越优先；注解路由 order=0 |
| 返回 HandlerExecutionChain | 可挂自定义拦截器 |
| 注入即生效 | 实现 HandlerMapping 的 Bean 会被 DispatcherServlet 收集 |

> ⚠️ 自定义路由注意：**别覆盖注解路由**——灰度/租户场景用条件判断 + 委托默认实现（如上例），而不是另起一套路由表，否则注解控制器全部失效。

## 4. HandlerAdapter 家族

| 实现 | 处理对象 | 说明 |
|------|---------|------|
| `RequestMappingHandlerAdapter` | `HandlerMethod`（注解控制器方法） | ★ 主力：参数解析 + 返回值处理 + 拦截器 |
| `HttpRequestHandlerAdapter` | `HttpRequestHandler` | 静态资源处理器等 |
| `SimpleControllerHandlerAdapter` | 旧式 Controller 接口 | 历史 |

```java
// RequestMappingHandlerAdapter 内部两步：
//   1. 参数解析：InvocableHandlerMethod 遍历 HandlerMethodArgumentResolver 链
//   2. 返回值处理：HandlerMethodReturnValueHandler 链（@ResponseBody 处理器）
```

| 参数解析器（部分） | 处理 |
|------------------|------|
| `RequestParamMethodArgumentResolver` | `@RequestParam` |
| `PathVariableMethodArgumentResolver` | `@PathVariable` |
| `RequestResponseBodyMethodProcessor` | `@RequestBody`（转换器读） |
| `ModelAttributeMethodProcessor` | `@ModelAttribute` |
| `HandlerMethodArgumentResolver`（自定义） | 扩展点 |

> 💡 面试扩展点：自定义 `HandlerMethodArgumentResolver` 是 MVC 最常用的扩展——实现 `supportsParameter` + `resolveArgument`，注册进 `WebMvcConfigurer.addArgumentResolvers`。

### 完整参数解析器链（Boot 4 默认顺序，节选）

| 顺序 | 解析器 | 处理 |
|:---:|--------|------|
| 1 | `RequestParamMethodArgumentResolver`（有注解） | `@RequestParam` |
| 3 | `PathVariableMethodArgumentResolver` | `@PathVariable` |
| 6 | `ServletRequestMethodArgumentResolver` | HttpServletRequest 等原生对象 |
| 8 | `RequestResponseBodyMethodProcessor` | `@RequestBody`（转换器读入） |
| 12 | `RequestHeaderMethodArgumentResolver` | `@RequestHeader` |
| 14 | `ServletCookieValueMethodArgumentResolver` | `@CookieValue` |
| 18 | `ModelMethodProcessor` | Model 参数 |
| 20 | `ErrorsMethodArgumentResolver` | BindingResult / Errors |
| 25 | `RequestParamMethodArgumentResolver`（无注解） | 简单类型兜底绑定 |
| 26 | `ServletModelAttributeMethodProcessor`（无注解） | 复合对象兜底绑定 |

> 💡 理解两条链的钥匙：**解析器按顺序调用，第一个 `supportsParameter` 返回 true 的接管**——无注解的简单类型与复合对象由最后两个兜底解析器处理；所以"不加注解的参数也能绑定"不是魔法，是兜底解析器在干活。

### 完整返回值处理器链（节选）

| 顺序 | 处理器 | 处理 |
|:---:|--------|------|
| 1 | `ModelAndViewMethodReturnValueHandler` | ModelAndView |
| 4 | `ResponseBodyEmitterReturnValueHandler` | SseEmitter / ResponseBodyEmitter |
| 5 | `StreamingResponseBodyReturnValueHandler` | 流式下载 |
| 6 | `HttpEntityMethodProcessor` | ResponseEntity / HttpEntity |
| 7 | `HttpHeadersReturnValueHandler` | 仅响应头 |
| 8 | `CallableMethodReturnValueHandler` | Callable（异步） |
| 9 | `DeferredResultMethodReturnValueHandler` | DeferredResult（异步） |
| 12 | `RequestResponseBodyMethodProcessor` | `@ResponseBody`（转换器写出）★ |
| 13 | `ViewNameMethodReturnValueHandler` | String 视图名（无 @ResponseBody） |
| 14 | `MapMethodProcessor` | Map → 模型 |
| 15 | `ModelAttributeMethodProcessor`（无注解） | 其他对象 → 模型 |

> 🎯 面试题"返回值怎么变成响应"的完整答案：**链顺序决定行为**——`@ResponseBody` 由 12 号接管（转换器直出），普通 String 由 13 号当视图名，`ResponseEntity` 由 6 号写状态码+头+体（详见 [04 篇第 1 节](04-响应与消息转换速查.md)）。

### 自定义参数解析器（完整示例）

```java
// 场景：把 X-User-Id 请求头解析成 CurrentUser 参数（省去控制器里重复取头）
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == CurrentUser.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) throws Exception {
        String userId = webRequest.getHeader("X-User-Id");
        if (userId == null) {
            throw new UnauthorizedException("missing X-User-Id");   // 走异常解析链
        }
        return new CurrentUser(userId);
    }
}

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserArgumentResolver());
    }
}

// 控制器使用
@GetMapping("/me")
public CurrentUser me(@CurrentUser CurrentUser user) { return user; }
```

| 扩展要点 | 说明 |
|---------|------|
| supportsParameter | 判断"这个参数归我管"（按类型或自定义注解） |
| resolveArgument | 解析出参数值；抛异常 → 进入异常解析链（@ControllerAdvice 可处理） |
| 注册 | `addArgumentResolvers` 追加到链尾（默认解析器优先） |
| 性能注意 | 解析器每次请求每个参数都可能执行，别做重量级 IO |

> 💡 参数解析器是 MVC 最实用的扩展点之一：**登录用户注入、租户上下文、请求链路追踪**三个场景用同一套模式实现——拿到自定义参数后业务代码不再碰 Servlet API。

## 5. 拦截器链的嵌入位置

```text
请求 → Servlet 容器 Filter 链 → DispatcherServlet
  → HandlerMapping 返回 HandlerExecutionChain（handler + 拦截器列表）
  → preHandle (1) → preHandle (2) ... 全部通过才执行
  → 控制器方法
  → postHandle (n) ... → postHandle (1)（逆序）
  → 渲染/转换
  → afterCompletion (n) ... → afterCompletion (1)（逆序，异常也执行）
```

| 拦截器方法 | 时机 | 典型用途 |
|-----------|------|---------|
| `preHandle` | 处理器执行前 | 鉴权（false 可短路）、日志、限流 |
| `postHandle` | 执行后、渲染前 | 修改 ModelAndView |
| `afterCompletion` | 渲染后（无论成败） | 资源清理、耗时统计 |

> ⚠️ **与 Filter 的区别**：拦截器在 DispatcherServlet 内（**只拦映射到控制器的请求**，静态资源默认不经过）；Filter 在容器层（一切请求）——"静态资源也需要鉴权"时用 Filter，见 [Spring-Web-06](../Spring‑Web/06-过滤链与CORS速查.md)。

### 拦截器短路行为推演

```text
拦截器 A、B、C（注册顺序 A → B → C），B.preHandle 返回 false：
  执行序列：
    A.preHandle → B.preHandle(false) ── 短路
    B.afterCompletion → A.afterCompletion（逆序，仅已通过的拦截器）
    控制器不执行、视图不渲染、C 完全不参与
```

| 情况 | afterCompletion 执行者 |
|------|----------------------|
| 全部通过 | A、B、C 全部（逆序 C → B → A） |
| B 短路 | 仅 A、B（逆序 B → A） |
| 控制器抛异常 | 全部（finally 语义，可拿到异常对象） |

> ⚠️ 常见误解：preHandle 返回 false 后"整个请求什么都不发生"是错的——**已经通过拦截器的 afterCompletion 一定会执行**（资源清理仍要完成）；日志清理逻辑放 afterCompletion 而不是 preHandle（详细推演见 [07 篇第 2 节](07-拦截器与视图速查.md)）。

### 拦截器的生产组合与职责边界

| 拦截器 | 放哪一段 | 注意 |
|--------|---------|------|
| 日志/链路追踪 | 注册第一位 | 最先进入、最后退出，覆盖全链路耗时 |
| 鉴权 | 业务拦截器之前 | preHandle 短路；静态资源不走拦截器需另行处理 |
| 限流 | 鉴权之后 | 避免"无权限请求也消耗限流配额" |
| 审计 | 最后注册 | afterCompletion 落库，失败请求也要记录 |

> 💡 拦截器职责边界口诀：**"登录校验做拦截、用户注入做解析器、业务规则做服务层"**——拦截器只做"请求级横切"，别把业务逻辑写进拦截器（拦截器无法注入服务依赖之外的语义，且难以测试）。一条铁律：拦截器方法里抛出的异常同样进入 MVC 异常解析链，但 preHandle 异常会导致后续拦截器的 preHandle 不执行——所以拦截器内部尽量捕获并明确短路（返回 false），而不是依赖异常传播。

### 面试官追问

| 追问 | 回答锚点 |
|------|---------|
| doDispatch 里 404 是谁抛的？ | 十步 ③ `noHandlerFound`（可配置抛出 NoHandlerFoundException） |
| 参数解析器顺序谁定的？ | RequestMappingHandlerAdapter 装配默认链，自定义的追加在链尾 |
| 拦截器与 Filter 谁先执行？ | Filter（容器层）→ DispatcherServlet → 拦截器 |
| 一次请求经过几个 Servlet？ | 一个（前端控制器模式） |
| 路由匹配性能怎么保证？ | 启动期预编译 PathPattern 解析树 + MappingRegistry 哈希索引 |
| 十步里哪一步最常出问题？ | ⑥ 执行（参数解析/转换失败）与 ⑨ 渲染（视图缺失/写出失败） |

> 🎯 本模块收官：把十步、两条链（参数/返回值）、拦截器三点背熟，再配合 [08 篇第 4 节](08-集成地图与常见问题.md) 的考点清单即可覆盖 MVC 全链路面试。自测：能否不看书画出 doDispatch 十步与两条链的完整顺序？能否说清 404/405/400/415/406 各自对应十步中的哪一步？

---

**下一模块**：[03-控制器与请求映射速查](03-控制器与请求映射速查.md)　**返回总览**：[00-Spring WebMVC组件总览](00-Spring WebMVC组件总览.md)
