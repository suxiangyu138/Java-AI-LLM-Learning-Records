# 00 Spring WebMVC 组件总览

> 组件卡片：spring-webmvc 是什么、版本现状、能做什么、与深度体系如何衔接——Servlet 栈 Web 框架的"最后一层"

---

## 📚 目录

1. [组件一句话定位](#1-组件一句话定位)
2. [版本现状（2026-08）](#2-版本现状2026-08)
3. [能力地图](#3-能力地图)
4. [与深度体系的映射](#4-与深度体系的映射)
5. [快速上手 3 步](#5-快速上手-3-步)
6. [速查导航](#6-速查导航)
7. [学习路线推荐](#7-学习路线推荐)
8. [核心概念速查](#8-核心概念速查)

---

## 1. 组件一句话定位

**spring-webmvc（Spring MVC）是 Servlet 栈的 Web 框架层**——以 DispatcherServlet 为总入口，把"HTTP 请求"映射到"控制器方法"，负责参数绑定、数据校验、消息转换、异常处理与视图渲染；**建立在 spring-web 的 HTTP 基础设施之上，是"底层核心"八系列中唯一直接面向用户的模块**。

```text
核心心智模型：
  一次请求的 MVC 五步：
  DispatcherServlet（中央调度）
    → HandlerMapping（请求 → 处理器，路由表）
    → HandlerInterceptor（拦截器链）
    → HandlerAdapter（参数绑定 → 控制器方法执行 → 返回值处理）
    → ViewResolver / HttpMessageConverter（视图渲染 或 JSON 直出）

  一句话：前端控制器模式——所有请求先进 DispatcherServlet，由它编排路由、执行与响应。
```

| 维度 | 说明 |
|------|------|
| 所属 | Spring Framework 底层核心模块（spring-webmvc） |
| 版本线 | 随 Spring Framework 7.x（2025-11 起） |
| 语言要求 | Java 17+；Servlet 6.x（Jakarta EE 11） |
| 定位 | Servlet 栈 Web 框架（与 WebFlux 并列的响应式替代） |

```text
与相邻技术栈的定位差异（面试常问"为什么还要 MVC"）：
  Servlet/JSP 原生     路由/绑定/渲染全手动，重复代码多
  Struts2              类级 Action + 拦截器体系，XML 配置重，已衰退
  Spring MVC（本模块） 注解驱动 + 前端控制器，Servlet 栈事实标准
  Spring WebFlux       响应式栈（Netty），非阻塞 IO 场景替代
```

| 维度 | Spring MVC（本模块） | Spring WebFlux |
|------|--------------------|----------------|
| IO 模型 | Servlet 阻塞式（Boot 4 可配虚拟线程） | 非阻塞事件循环（Netty） |
| 编程模型 | 注解控制器 + 拦截器 | 注解 + RouterFunction 函数式 |
| 生态集成 | 事务/模板/视图/Spring Security 集成最全 | 流式/背压优先 |
| 适用 | 传统业务系统（默认选择） | 高并发 IO 密集网关/流式推送场景 |
| 共用点 | 同一套 spring-web 基础设施、注解风格、校验/转换体系 | 同左 |

> 💡 选型结论：没有明确需求不要用 WebFlux——MVC 的事务、模板、Security 集成更成熟；WebFlux 的"高并发"优势只在 IO 密集场景成立，且与 MVC 互斥（一个应用一个栈）。

## 2. 版本现状（2026-08）

| 版本 | 说明 |
|------|------|
| Spring Framework 7.0（2025-11） | 当前主线（Boot 4.0 配套） |
| Spring Framework 7.1（2026-05） | 最新维护线（Boot 4.1 配套） |
| Spring Framework 6.2.x | 存量主线（Boot 3.x 配套） |

**7.x mvc 关键变化：**

- **请求映射 API 版本化（7.0.0-M3，#34566）**：`@RequestMapping(version = "1.2")` 支持无值/固定值/基线值（`"1.2+"`）三种匹配；类级 `@ApiVersion("v2")`（含 `defaultVersion = true`）；Boot 属性 `spring.mvc.apiversion.use.path-segment` / `spring.mvc.apiversion.supported`；
- **PathPattern 全面取代 AntPathMatcher（7.0，#34018）**：web 模块中 AntPathMatcher 废弃；支持**路径开头多段通配**（`/**/pages/index.html`）；Security 7.0 同步切换并执行更严格的通配规则（`**` 不得出现在模式中间，除非回退 `ant_path_matcher` 策略）；
- **`HttpHeaders` 不再直接实现 `MultiValueMap`**（7.0）：API 收紧为专用头模型（`get`/`add`/`set` 语义不变，类型层面解耦）；
- **`HttpMessageConverters` 统一配置**：`WebMvcConfigurer.configureMessageConverters` 用框架级 API 取代 Boot 旧类型（见 [Spring-Web-03](../Spring‑Web/03-消息转换与内容协商速查.md)）；
- **DataBinder 收敛（7.1，#36802）**：`disallowedFields` 属性废弃——推荐**不可变对象构造器绑定**（record/主构造器）或显式 `allowedFields` 白名单；
- **7.0 延续**：Jackson CBOR 默认装配、Kotlin Serialization 转换器只编解码 @Serializable 类型、虚拟线程默认（Boot 4）；
- **7.1**：`PreFlightRequestFilter`（CORS 预检，见 [Spring-Web-06](../Spring‑Web/06-过滤链与CORS速查.md)）。

> ⚠️ **要点**：7.0 的 MVC 变化集中在"路由匹配现代化"（PathPattern + API 版本化）与"配置 API 统一"（HttpMessageConverters）——存量代码主要注意通配规则收紧与转换器类名更新。

### 版本演进时间线

| 版本线 | 时间 | MVC 侧关键变化 |
|--------|------|---------------|
| 5.3.x | 2020–2022 | 经典时代：AntPathMatcher 默认、Jackson 2 命名 |
| 6.0.x | 2022-11 | Jakarta EE 9+ 迁移（javax → jakarta）、PathPattern 可选启用 |
| 6.1.x | 2023-11 | ProblemDetail 错误体、构造器绑定（不可变 DTO） |
| 6.2.x | 2024-11 | 存量主线（Boot 3.4/3.5 配套）；虚拟线程支持 |
| 7.0.x | 2025-11 | PathPattern 唯一默认、API 版本化、转换器新命名 |
| 7.1.x | 2026-05 | DataBinder 收敛（disallowedFields 废弃）、CORS 预检 Filter |

### 7.0 新特性清单

| 类别 | 新特性 | 影响面 |
|------|--------|--------|
| 路由 | PathPattern 唯一默认（AntPathMatcher 废弃，#34018） | 通配写法收紧；`/**` 开头多段通配新增 |
| 路由 | API 版本化（`@RequestMapping(version)` / `@ApiVersion`） | 新增能力，需 ApiVersionConfigurer 或 Boot 属性启用 |
| 配置 | `HttpMessageConverters` 统一配置 API | 自定义转换器类名变更（`JacksonJson...`） |
| 模型 | `HttpHeaders` 不再实现 `MultiValueMap` | 类型引用代码需调整 |
| 移除 | suffixPatternMatch / trailingSlashMatch / favorPathExtension | 路径后缀、尾部斜杠匹配行为变化 |

> 💡 7.0 移除项提醒：`trailingSlashMatch`（尾部斜杠匹配）7.0 已彻底移除——`/api/orders/` 与 `/api/orders` 不再等价，客户端必须按定义路径访问。

## 3. 能力地图

| 能力域 | 能力点 | 对应注解/API |
|--------|--------|-------------|
| 中央调度 | 前端控制器 | `DispatcherServlet`（init/请求分发/组件装配） |
| 请求映射 | 路由表 | `@RequestMapping` 家族、`@ApiVersion`（7.0） |
| 参数绑定 | 方法参数解析 | `@RequestParam`/`@PathVariable`/`@RequestBody`/`@ModelAttribute` |
| 数据校验 | Bean Validation | `@Validated`/`@Valid`（jakarta.validation）+ `BindingResult` |
| 响应转换 | 消息直出 | `@ResponseBody`/`ResponseEntity`/`HttpMessageConverter` |
| 异常处理 | 统一错误 | `@ExceptionHandler`/`@ControllerAdvice`/`ProblemDetail`（RFC 7807） |
| 拦截器 | 请求横切 | `HandlerInterceptor`（pre/post/after 三方法） |
| 视图渲染 | 模板/模型 | `ViewResolver`/`ModelAndView`/Thymeleaf |
| 内容协商 | 多格式响应 | `produces`/`Accept` 协商、静态资源 |
| 异步 | 流式/推送 | `SseEmitter`/`DeferredResult`/虚拟线程（见 [Spring-Web-07](../Spring‑Web/07-异步与流式速查.md)） |

### 能力域协作关系

```text
一次 POST /api/orders（JSON 创建订单）中能力域接力：
  中央调度 → 请求映射 → 参数绑定（转换器读 JSON）
  → 数据校验（@Valid）→ 控制器业务逻辑
  → 异常处理（失败时 @ControllerAdvice 接管）
  → 响应转换（转换器写 JSON）
  全程横切：拦截器（pre → 执行 → post/after）
```

| 请求阶段 | 能力域 | 关键类/注解 |
|---------|--------|-----------|
| 1 入口 | 中央调度 | `DispatcherServlet` |
| 2 路由 | 请求映射 | `RequestMappingHandlerMapping` / `@RequestMapping` |
| 3 入参 | 参数绑定 | `RequestResponseBodyMethodProcessor` / `@RequestBody` |
| 4 校验 | 数据校验 | `@Valid` → `MethodArgumentNotValidException` |
| 5 业务 | 控制器方法 | 业务代码（事务在服务层，见 [Spring-TX 系列](../Spring‑TX（Spring‑Transaction）/00-Spring TX组件总览.md)） |
| 6 出错 | 异常处理 | `@ExceptionHandler` / `ProblemDetail` |
| 7 出参 | 响应转换 | `RequestResponseBodyMethodProcessor` / 转换器 |

> 🎯 理解 MVC 的捷径：把请求想象成"流水线"——每个能力域只做一件事，**顺序固定、职责单一**；面试讲 MVC 架构时按此表把十步拆成"能力域 × 阶段"就清楚了。

### 各能力域的扩展点

| 能力域 | 扩展接口 | 注册方式 |
|--------|---------|---------|
| 路由 | 自定义 `HandlerMapping`（实现接口） | 注入 Bean 即被收集 |
| 参数解析 | `HandlerMethodArgumentResolver` | `WebMvcConfigurer.addArgumentResolvers` |
| 返回值处理 | `HandlerMethodReturnValueHandler` | `WebMvcConfigurer.addReturnValueHandlers` |
| 消息转换 | `HttpMessageConverter` | `WebMvcConfigurer.configureMessageConverters` |
| 异常处理 | `@ControllerAdvice` / `HandlerExceptionResolver` | 组件扫描 / 注入 Bean |
| 拦截器 | `HandlerInterceptor` | `WebMvcConfigurer.addInterceptors` |
| 校验 | `ConstraintValidator` / `Validator` | SPI 自动发现 / `mvcValidator` |
| 视图 | `ViewResolver` / `View` | `WebMvcConfigurer.configureViewResolvers` |

> 💡 统一规律：**MVC 的每个能力域都有一个"策略接口 + WebMvcConfigurer 回调"**——想扩展哪里就找对应的 add/configure 方法；这就是 WebMvcConfigurer 被称为"MVC 总配置面板"的原因（详见 [02 篇第 4 节](02-DispatcherServlet与请求全链路速查.md) 的解析器链）。

### 异步与虚拟线程（Boot 4）

```text
Boot 4（Framework 7.x）默认虚拟线程处理请求：
  每个请求跑在虚拟线程上（OS 线程按需挂载/卸载）
  → 阻塞 IO（JDBC/文件/第三方调用）不再占满平台线程
  → MVC 的"每请求一线程"模型天然受益，无需换响应式栈

显式异步接口：
  DeferredResult / Callable：先返回，任务完成后补写响应
  SseEmitter / ResponseBodyEmitter：流式推送
  结论：虚拟线程时代，普通接口写同步方法即可；异步 API 留给长连接/推送
```

| 场景 | 推荐写法 |
|------|---------|
| 普通业务接口 | 同步方法（虚拟线程兜底） |
| 第三方调用慢接口 | 同步即可（虚拟线程不阻塞平台线程） |
| 长连接推送 | SseEmitter（显式异步） |
| 超大文件下载 | StreamingResponseBody（流式） |

> ⚠️ 虚拟线程注意事项：**ThreadLocal 语义变化**（虚拟线程不回收）——请求级状态用 request 属性/请求作用域 Bean，别长期持有 ThreadLocal（见 [08 篇第 3 节](08-集成地图与常见问题.md) 高频坑表）。

## 4. 与深度体系的映射

| 速查文档 | 深度体系模块 |
|---------|-------------|
| 02-DispatcherServlet 与请求全链路速查 | [SpringMVC-01-DispatcherServlet请求处理全链路](../../../SpringMVC/01-DispatcherServlet请求处理全链路.md) |
| 03-控制器与请求映射速查 | [SpringMVC-02-注解驱动开发：@Controller与请求映射](../../../SpringMVC/02-注解驱动开发：@Controller与请求映射.md) |
| 04-响应与消息转换速查 | [SpringMVC-04-响应与消息转换：HttpMessageConverter与Jackson 3](../../../SpringMVC/04-响应与消息转换：HttpMessageConverter与Jackson 3.md) |
| 05-异常处理速查 | [SpringMVC-05-异常处理：@ExceptionHandler与ProblemDetail](../../../SpringMVC/05-异常处理：@ExceptionHandler与ProblemDetail.md) |
| 06-数据校验与绑定速查 | [SpringMVC-03-参数绑定与数据校验](../../../SpringMVC/03-参数绑定与数据校验.md) |
| 07-拦截器与视图速查 | [SpringMVC-06-拦截器与过滤器](../../../SpringMVC/06-拦截器与过滤器.md) |
| 08-集成地图与常见问题 | [SpringMVC-09-生产实践与面试题](../../../SpringMVC/09-生产实践与面试题.md) |

> 💡 本系列定位"查得快"，深度体系定位"学得透"——**SpringMVC 深度体系（10 篇）与本系列一一对应**；DispatcherServlet 源码深挖见 [Spring生态深度剖析-05](../../../Spring生态深度剖析/05-DispatcherServlet请求处理全链路.md)。

### 速查与深潜的分工

| 场景 | 资料 | 理由 |
|------|------|------|
| 写接口查语法/属性 | 本系列（速查 8 篇） | 表格直达答案 |
| 面试原理深挖 | SpringMVC 深度体系（10 篇） | 源码级推导 + 流程图 |
| DispatcherServlet 逐行源码 | Spring生态深度剖析-05 | 全链路代码注释 |
| 消息转换器全家表 | Spring-Web 系列 03 | 转换器/协商全表 |

> 💡 组合用法：面试前先过本系列速查表建立框架，再对不熟的点进深度体系看推导；代码层面以 Boot 4 实际装配为准（自动配置清单可在 IDE 里看 `WebMvcAutoConfiguration`）。

## 5. 快速上手 3 步

**① 引入依赖**（Boot 4）：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

**② 写控制器**（三件套：映射 + 参数 + 响应）：

```java
@RestController                       // = @Controller + @ResponseBody（JSON 直出）
@RequestMapping("/api/orders")
public class OrderController {

    @GetMapping("/{id}")
    public Order getById(@PathVariable Long id) { ... }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)          // 201
    public Order create(@Valid @RequestBody OrderCreateCmd cmd) { ... }

    @GetMapping
    public List<Order> list(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "20") int size) { ... }
}
```

**③ 全局异常兜底**（ProblemDetail 风格）：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(OrderNotFoundException.class)
    public ProblemDetail notFound(OrderNotFoundException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, e.getMessage());
        pd.setTitle("Order Not Found");
        return pd;                                  // RFC 7807 JSON 错误体
    }
}
```

```bash
# 验证三步成果（Boot 4 默认 8080 端口）
curl -X POST http://localhost:8080/api/orders \
     -H "Content-Type: application/json" \
     -d '{"amount": 99.9, "userId": "u1001"}'
# → 201 Created，响应体为 JSON 订单对象
curl http://localhost:8080/api/orders/9999
# → 404 + application/problem+json 错误体（第三步的全局兜底生效）
```

> 💡 三步之外还要会两件事：**看装配**（启动日志的 `RequestMappingHandlerMapping` 行能看到全部路由）与**看转换**（异常/JSON 问题先查转换器链，见 [04-响应与消息转换速查](04-响应与消息转换速查.md)）。

### 从三步到生产的增量清单

| 增量点 | 说明 | 对应文档 |
|--------|------|---------|
| 全局校验 | `@Valid` + Advice 统一 400 | [06-数据校验与绑定速查](06-数据校验与绑定速查.md) |
| 拦截器 | 鉴权/审计/限流横切 | [07-拦截器与视图速查](07-拦截器与视图速查.md) |
| 消息转换 | 时间格式/时区/NULL 策略 | [04-响应与消息转换速查](04-响应与消息转换速查.md) |
| 上传 | `spring.servlet.multipart.*` 限制 | [08-集成地图与常见问题](08-集成地图与常见问题.md) |
| 版本化 | 7.0 `@ApiVersion`（多版本共存） | [03-控制器与请求映射速查](03-控制器与请求映射速查.md) |
| 异步 | 长任务 DeferredResult / SSE | [Spring-Web-07](../Spring‑Web/07-异步与流式速查.md) |

> 🎯 一句话路线：**先能跑（3 步）→ 再补横切（校验/异常/拦截）→ 最后上生产细节（转换/上传/版本化）**——本系列 8 篇按这个顺序读最顺。

## 6. 速查导航

| 文档 | 内容 |
|------|------|
| [01-模块清单](01-模块清单.md) | spring-webmvc artifact 与包结构、组件清单 |
| [02-DispatcherServlet 与请求全链路速查](02-DispatcherServlet与请求全链路速查.md) | 请求十步、HandlerMapping/Adapter 家族 |
| [03-控制器与请求映射速查](03-控制器与请求映射速查.md) | @RequestMapping 家族、参数解析、@ApiVersion（7.0） |
| [04-响应与消息转换速查](04-响应与消息转换速查.md) | @ResponseBody 原理、返回值处理器、转换器 |
| [05-异常处理速查](05-异常处理速查.md) | @ExceptionHandler/@ControllerAdvice/ProblemDetail |
| [06-数据校验与绑定速查](06-数据校验与绑定速查.md) | Bean Validation、BindingResult、DataBinder（7.1 变化） |
| [07-拦截器与视图速查](07-拦截器与视图速查.md) | HandlerInterceptor 三方法、ViewResolver、静态资源 |
| [08-集成地图与常见问题](08-集成地图与常见问题.md) | 底层核心八系列收官 + 高频坑 + 面试考点 |

> 💡 使用建议：本系列按"请求链路"组织——02 → 08 恰好是一条请求从前到后的完整旅程；不熟悉的章节之间随时用 [08 篇第 1 节](08-集成地图与常见问题.md) 的集成地图定位"当前在链路哪一段"。

### 官方资料速查

| 资料 | 用途 |
|------|------|
| [Spring MVC 参考文档（7.x）](https://docs.spring.io/spring-framework/reference/web/webmvc.html) | 官方权威手册 |
| [Spring Framework 7.0 Release Notes（GitHub Wiki）](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-7.0-Release-Notes) | 版本变化明细 |
| [Spring Boot Web 属性清单](https://docs.spring.io/spring-boot/appendix/application-properties/index.html) | `spring.mvc.*` / `spring.servlet.*` 全量属性 |
| [API Version 参考（7.0 新章节）](https://docs.spring.io/spring-framework/reference/7.0-SNAPSHOT/web/webmvc/mvc-config/api-version.html) | `@ApiVersion` 官方用法 |
| [ProblemDetail 参考](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html) | RFC 7807 错误体官方示例 |

> 💡 速查文档与官方文档的配合：**官方文档讲"标准答案"，本系列讲"考法/坑/取舍"**——面试题里 80% 的坑来自官方文档字缝里的细节（如 7.0 移除 trailingSlashMatch、版本化默认关闭等），本系列把它们显式化，考前过一遍本页 + 各篇速查表即可覆盖大部分考点。

## 7. 学习路线推荐

| 路线 | 适用 | 顺序 |
|------|------|------|
| 入门速查 | 快速写接口 | 00 总览 → 03 控制器映射 → 04 响应 → 05 异常 |
| 项目实践 | 完整 Web 开发 | 02 全链路 → 03 映射 → 06 校验 → 07 拦截器 → 08 常见问题 |
| 面试冲刺 | MVC 全考点 | 02 十步 → 04 @ResponseBody 原理 → 05 异常链 → 08 考点清单 → [SpringMVC-09](../../../SpringMVC/09-生产实践与面试题.md) |

## 8. 核心概念速查

| 术语 | 一句话解释 |
|------|-----------|
| DispatcherServlet | 前端控制器（请求中央调度） |
| HandlerMapping | 请求 → 处理器（路由表） |
| HandlerAdapter | 处理器执行（参数解析 + 返回值处理） |
| @RestController | @Controller + @ResponseBody 组合 |
| HandlerMethodArgumentResolver | 方法参数解析器链 |
| HandlerMethodReturnValueHandler | 返回值处理器链 |
| @ControllerAdvice | 全局异常/绑定处理 |
| ProblemDetail | RFC 7807 标准错误体 |
| HandlerInterceptor | 请求横切（pre/post/after） |
| @ApiVersion（7.0） | 请求映射 API 版本化 |

---

**下一模块**：[01-模块清单](01-模块清单.md)　**返回总览**：本页

**【参考来源】**：[Spring Framework 7.0 Release Notes（GitHub Wiki）](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-7.0-Release-Notes)、[Spring Framework 7.0.0-M8 发布公告（spring.io）](https://spring.io/blog/2025/08/14/spring-framework-7-0-0-M8-available-now)、[PathMatcher 废弃 Issue #34018](https://github.com/spring-projects/spring-framework/issues/34018)、[DataBinder disallowedFields 废弃 Issue #36802](https://github.com/spring-projects/spring-framework/issues/36802)、[Spring Framework 7.0 正式发布解析（掘金）](https://juejin.cn/post/7573592127298682930)
