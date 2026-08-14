# 07 Spring Web 注解剖析

> Web 层注解把「HTTP 请求到 Java 方法的映射协议」声明化：@RestController 注册处理器、@GetMapping 声明路由、@RequestParam 绑定参数、@ControllerAdvice 接管异常。2026 年 Spring 7 的主线变化是 API 版本化注解（@ApiVersion）进入框架与 Jackson 3 默认化

## 📚 目录

1. [控制器与映射：@RestController / @RequestMapping 六变体](#1-控制器与映射restcontroller--requestmapping-六变体)
2. [参数绑定五注解：@RequestParam / @PathVariable / @RequestBody](#2-参数绑定五注解requestparam--pathvariable--requestbody)
3. [返回值与异常处理注解](#3-返回值与异常处理注解)
4. [Spring 7 版本化注解与 Boot 4 变化](#4-spring-7-版本化注解与-boot-4-变化)
5. [高频坑与面试题](#5-高频坑与面试题)

---

## 1. 控制器与映射：@RestController / @RequestMapping 六变体

`@RestController` 是派生组合注解，等价于 `@Controller + @ResponseBody`——后者把每个方法的返回值交给消息转换器（Jackson）序列化进响应体。判断"该用哪个"的规则：**返回 JSON/XML 用 @RestController，返回视图名（模板渲染）用 @Controller + 视图解析器**。@RestController 里混入返回视图的方法会直接把视图名当字符串序列化出去。

`@RequestMapping` 是总注解，@GetMapping/@PostMapping/@PutMapping/@DeleteMapping/@PatchMapping 六个变体是它的快捷方式（内部 @AliasFor 传递属性）。完整属性表按频率排序：

- `path`/`value`：路由模板，支持 `{id}` 占位符与正则 `{id:\\d+}`；
- `consumes`/`produces`：按 Content-Type/Accept 缩小匹配，不匹配返回 **415/406**——排查"404 但路由明明存在"时先看这两个属性与客户端头；
- `params`/`headers`：按请求参数/头存在性过滤（`params = "type=wechat"`）；
- `method`：总注解上限定动词（变体注解已隐含）。

Spring 7 新增 **`version` 属性**：`@GetMapping(path = "/{id}", version = "2.0+")` 声明方法适用的 API 版本区间，与类级 `@ApiVersion("v1")` 配合实现同一路径多版本共存——细节见第 4 节。

---

## 2. 参数绑定五注解：@RequestParam / @PathVariable / @RequestBody

**@RequestParam** 绑定查询参数/表单参数。最大坑是 **required 默认 true**：请求缺参数直接 400 `MissingServletRequestParameterException`，可选参数必须显式 `required = false` 或给 `defaultValue`（给了 defaultValue 即隐含 optional）。类型转换失败抛 `MethodArgumentTypeMismatchException`（400）。

**@PathVariable** 绑定路径占位符。参数名与占位符名**必须一致**——javac 默认不保留参数名，Spring 靠 `-parameters` 编译参数（或调试信息）取名字，没有时抛 `IllegalArgumentException` 提示找不到参数名。Maven/Gradle 都要显式开启 `-parameters`，这是最经典的"本机好、CI 挂"坑。

**@RequestBody** 用消息转换器把请求体反序列化为对象。三个边界：GET 带 body 不合规范（部分容器直接丢弃）；空 body 反序列化抛 `HttpMessageNotReadableException`（400，用 Optional<T> 或 required=false 应对）；一个方法**只能有一个** @RequestBody 参数。Spring 7 默认 Jackson 3，DTO 层要注意 04 篇的三处默认行为反转（日期格式、未知属性、null 原语）。

其余三注解：`@RequestHeader`（绑定头，`@RequestHeader(name = "X-Trace-Id", required = false)`）、`@CookieValue`（绑定 Cookie）、`@RequestPart`（multipart/form-data 的文件部分，与 @RequestParam 的区别是前者走 HttpMessageConverter 支持 JSON 文件元数据混合）、`@ModelAttribute`（对象绑定，表单参数自动映射到对象字段——**批量赋值防护**：表单能覆盖对象任意字段，禁止直接用实体接收表单参数，必须用 DTO）。

---

## 3. 返回值与异常处理注解

返回值三件套：`@ResponseStatus(code = HttpStatus.CREATED)`（固定状态码，属性 `reason` 已废弃——原因短语 HTTP/2+ 不再传输）；`ResponseEntity<T>` 返回对象（可控状态码/头/体，构建 API 首选）；`@ResponseStatusException` 抛出对象（无需自定义异常类即可带状态码抛出，替代老式 ResponseStatusExceptionResolver 风格）。

异常处理注解的层次：`@ExceptionHandler` 标在 Controller 内**只处理该控制器**的异常；配合 `@ControllerAdvice`（`@RestControllerAdvice` 是其 JSON 版）标在全局类上处理所有控制器，`basePackages`/`annotations` 属性限定作用范围（多模块项目按包隔离异常语义）。匹配优先级是"**就近原则**"：Controller 内的 @ExceptionHandler > @ControllerAdvice 中异常类型最具体的处理器——因此局部处理器可以覆盖全局策略，全局策略负责兜底。Spring 7 推荐返回 `ProblemDetail`（RFC 9457 application/problem+json），标准化字段 type/title/status/detail/instance，前端与网关可统一消费：

```java
@ExceptionHandler(BizException.class)
public ProblemDetail handleBiz(BizException e) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    pd.setTitle("业务校验失败");
    pd.setProperty("code", e.getCode());   // 自定义扩展字段
    return pd;
}
```

`ProblemDetail` 相比自建错误体（{code, msg, data}）的价值在协议化：网关、监控、前端 SDK 都能按 RFC 字段做统一处理，自建体仍是国内团队主流（与前端契约已定时的务实选择），两者选一写进规范即可。

`@CrossOrigin(origins = "https://a.com", methods = {...}, maxAge = 3600, allowCredentials = "true")` 方法级/类级 CORS；项目级建议全局 `CorsRegistry` 配置统一白名单，注解只留例外场景。`allowCredentials = true` 时 origins 不能用 `*`（浏览器规范禁止）。CORS 的两个机制细节：预检（OPTIONS 请求）由框架按"非简单请求"规则自动响应，`maxAge` 控制预检结果缓存时长；注解方式优先级高于全局配置（注解覆盖而非叠加），排查"全局配置被无视"时先查类上是否有 @CrossOrigin。

### 3.1 消息转换器与内容协商机制

@ResponseBody/@RequestBody 的底层是 `HttpMessageConverter` 链：按请求 `Content-Type` 找能反序列化的转换器（Jackson JSON、XML、String 等），按 `Accept` 找序列化转换器。`produces`/`consumes` 注解属性就是**声明这条链的匹配条件**——内容协商失败分别对应 406（无可用写出转换器）与 415（无可用读入转换器）。Spring 7 默认 Jackson 3 转换器，DTO 序列化行为变化（04 篇三处默认反转）直接体现在这条链上；自定义转换器（如 Protobuf）通过 WebMvcConfigurer 注册进链，`@RequestMapping(produces = "application/x-protobuf")` 即可分派到它——注解声明 + 转换器链分派是 Web 层扩展的标准姿势。

`@InitBinder`（定制参数绑定，如白名单字段过滤）与 `@SessionAttributes`（会话属性暂存）在前后端分离 + 无状态 API 时代已边缘化——但 @InitBinder 有一个仍然活跃的场景：**批量赋值防护**。`@ModelAttribute` 绑定对象时，恶意请求可以覆盖任何有 setter 的字段（如 `role=ADMIN`）。白名单式 @InitBinder 是防御姿势之一（更推荐的还是直接用 DTO 隔离）：

```java
@InitBinder
public void initBinder(WebDataBinder binder) {
    binder.setAllowedFields("name", "email");   // 其余字段绑定一律忽略
}
```

方法论级的解法仍是"**实体类永远不直接做绑定目标**"——DTO 只暴露允许客户端修改的字段，@InitBinder 白名单只作为存量接口的补救措施。

---

## 4. Spring 7 版本化注解与 Boot 4 变化

**@ApiVersion 是 Spring 7 的正式 API 版本化方案**：类级 `@ApiVersion("v1")` 声明控制器版本，同路径的 V1/V2 控制器共存（如 `ProductControllerV1`/`ProductControllerV2` 两个类都映射 `/products`）；方法级 `@GetMapping(path = "/{id}", version = "2.0+")` 让单个方法覆盖版本区间。版本解析策略可配置（header `X-API-Version`、媒体类型 `application/vnd.api.v2+json`、URL 段），不配置则报版本歧义错误。这替代了此前各家自造的 @ApiVersion 自定义注解——社区方案正式转正。

Boot 4 升级的三个 Web 层变化：starter 更名 `spring-boot-starter-webmvc`（旧名保留兼容）；**尾斜杠匹配移除**（`/users/` 不再匹配 `/users`，Boot 4 默认 PathPatternParser 语义）；Jackson 3 默认（DTO 行为反转见 04 篇）。升级检查单必须包含：全量路由的尾斜杠回归、响应 JSON 快照对比、`javax.servlet` import 清理。

---

## 5. 高频坑与面试题

1. **@RequestParam required 默认 true**：可选参数忘写 required=false，客户端缺参 400；
2. **@PathVariable 参数名不匹配**：开 `-parameters` 编译参数是标准答案，不要靠 `@PathVariable("id")` 显式名补丁掩盖编译配置缺失；
3. **415/406 先查 consumes/produces**：路由存在但内容协商失败，404 排查的第一岔路；
4. **@RequestBody 与 GET、空 body**：语义与 400 的边界；上传大文件时 `spring.servlet.multipart.max-file-size/max-request-size` 与 @RequestPart 的配套限制（超限抛 `MaxUploadSizeExceededException`，应在 @ControllerAdvice 中转 413）；
5. **面试必答框架**：「请求参数怎么到方法参数的？」——HandlerMapping 匹配路由 → `InvocableHandlerMethod` 遍历参数 → 各 `HandlerMethodArgumentResolver` 按注解分派（@RequestParam 走 RequestParamMethodArgumentResolver、@RequestBody 走 RequestResponseBodyMethodProcessor）→ 类型转换/校验/序列化 → 反射调用。能说出 resolver 分派机制，比背注解属性高一档；追问「@RequestParam 与 @RequestBody 能同时用吗？」——可以，两者分属不同 resolver 且来源不同（查询串 vs 请求体），但同种来源的参数不能重复声明；
6. **「HTTP 方法注解的选择依据？」**——语义与幂等性双维度：GET 查询（幂等，可缓存）；POST 创建（非幂等，重复提交需幂等键）；PUT 整体更新（幂等）；PATCH 局部更新；DELETE 删除（幂等）。选错方法不只是风格问题——网关重试策略、缓存、安全策略都按方法语义配置；
7. **「@ApiVersion 解决了什么老问题？」**——老方案是 URL 前缀（/v1/users，污染路由）或自定义注解 + 拦截器（各家自造不互通）；Spring 7 的 @ApiVersion + version 属性把版本解析正式纳入框架，支持 header/媒体类型/URL 三策略，方法级版本区间（"2.0+"）让灰度期新旧版本平滑共存。

---

**下一模块**：[08 持久层注解剖析](./08-持久层注解剖析.md) · **返回总览**：[00 总览](./00-Java常用注解知识体系总览.md)

**相关体系**：[SpringBoot Web（DispatcherServlet 全链路）](../../../02-后端核心技术%20微服务%20分布式%20云原生/09-Web开发全流程/SpringBoot%20Web/00-SpringBootWeb总览.md) · [MVC](../../../01-底层根基-Java核心底座/10-计算机设计思想与架构工程能力/MVC/00-MVC架构模式总览.md)

---

【参考来源】
- [Spring Framework 6→7 迁移指南（@ApiVersion/PathPatternParser/尾斜杠）](https://dev.to/ankurm/spring-framework-6-to-7-migration-guide-breaking-changes-deprecated-apis-and-upgrade-checklist-3bf6)
- [Spring Annotations: The 2026 Essential Cheat Sheet（Marco Molteni）](https://marmo.dev/spring-annotation-meaning)
- [What's New in Spring Boot 4（Dan Vega, KCDC 2026）](https://www.danvega.dev/speaking/kcdc-2026-whats-new-in-spring-boot-4)
- [聚焦 Spring Framework 7 与 Spring Boot 4：Spring 团队专访（InfoQ 中文）](https://www.infoq.cn/article/z4msV9uzNy7CXYFC4K2J)
- [Spring Boot 4 升级 Jackson 3 详解（响应快照影响）](https://yunpan.plus/t/7941-1-1)
