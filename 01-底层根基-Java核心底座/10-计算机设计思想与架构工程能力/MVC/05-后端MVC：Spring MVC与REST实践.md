# 05-后端 MVC：Spring MVC 与 REST 实践

> 定位：MVC 在后端依然健壮——Spring MVC 7 仍是 Java Web 的事实标准。本文给出控制器层级的完整实践纪律：薄控制器、强类型绑定、集中错误处理、表单安全，以及测试策略。

## 薄控制器的黄金纪律

后端 MVC 的第一纪律与 02 篇一致：**Controller 只做路由、绑定、编排三件事**。Spring 中的具体形态：

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService products;   // 只注入服务接口

    @GetMapping("/{id}")
    public ProductDto get(@PathVariable Long id) {
        return products.findById(id);        // 编排：拿结果就返回
    }
}
```

三条硬边界：**不写业务规则**（价格计算、状态流转在 Service/领域层）；**不做数据访问**（查询在 Repository，Controller 不碰 JPA）；**返回 DTO/record 而非实体**（实体泄漏是 API 稳定性杀手——实体字段一变，API 契约就变）。2026 年新增的防膨胀手段：控制器超过 150-200 行触发"逻辑下沉"审查；用 @RequestMapping 的公共前缀聚合同资源操作。

## 强类型绑定：从 String 到值对象

Spring 默认把路径参数/查询参数绑定为 String，直接在 Controller 里 parse 是坏味道。2026 年实践推荐**值对象转换器**：实现 Converter<String, UserId>，让 @PathVariable/@RequestParam 直接绑定为领域值对象：

```java
@Component
public class UserIdConverter implements Converter<String, UserId> {
    @Override public UserId convert(String source) { return UserId.of(source); }
}

@GetMapping("/users/{id}")
public UserDto get(@PathVariable UserId id) { ... }   // 绑定直接得到领域对象
```

请求体的值对象绑定用 Jackson（Boot 4 为 Jackson 3：`tools.jackson.databind.json.JsonMapper`，不再是 Jackson 2 的 ObjectMapper），@JsonCreator/@JsonValue 定义构造与序列化。强类型绑定的收益：类型校验前移（非法 ID 在绑定层 400）、领域规则单点封装、Controller 代码变薄。查询参数/表单绑定同理：@ModelAttribute 绑定到 DTO，@Valid 触发校验。

## 集中错误处理：ProblemDetail 统一契约

分散的 try-catch 是控制器膨胀的另一个来源。正确做法是全局异常处理器：@RestControllerAdvice 继承 ResponseEntityExceptionHandler，统一返回 **ProblemDetail**（RFC 7807 错误格式）：status、title、detail、instance 四个标准字段 + 自定义扩展（校验错误明细）。

```java
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(BizException.class)
    ProblemDetail handleBiz(BizException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(e.getStatus(), e.getMessage());
        pd.setProperty("code", e.getCode());          // 业务错误码进扩展字段
        return pd;
    }
    // 生产环境隐藏内部异常细节：setProperty("trace", null)
}
```

错误处理的两条纪律：**业务异常（BizException）带错误码进扩展字段，技术异常（SQL/IO）绝不回传细节**；**校验错误（@Valid 失败）聚合返回**（所有字段错误一次返回，而非第一个）。ProblemDetail 是 Spring 6 引入、2026 年成为标配——API 错误格式的统一与状态码语义化（4xx 客户端错、5xx 服务端错）是 REST API 的专业分水岭。

## 表单绑定与安全：@ModelAttribute 与 @InitBinder

服务端渲染场景（Thymeleaf）的表单绑定：方法级 @ModelAttribute 集中共享参考数据（下拉选项），避免每个处理器重复装配；@ControllerAdvice 的 @ModelAttribute 可全局提供（注意缓存，别在每个请求都查库）。**安全关键**：@InitBinder 的 setDisallowedFields("id", "role", ...) 防止**批量赋值攻击**（Mass Assignment）——攻击者提交隐藏字段 role=admin 直接越权。多步表单用 @SessionAttributes 要克制（会话状态跨标签页冲突），2026 年推荐"提交一步、持久化草稿、继续下一步"的替代流程。

## 请求生命周期的完整追踪

一个 POST /api/orders 请求在 Spring MVC 中的完整旅程，是理解框架与 MVC 思想对应关系的快照：**1. 拦截器链**（HandlerInterceptor 的 preHandle）——认证、追踪 ID 注入、限流等横切逻辑在此执行（横切关注点不污染 Controller）；**2. HandlerMapping**——按路径与方法找到 OrderController.create；**3. HandlerAdapter**——参数解析器把 JSON body 反序列化为 CreateOrderCommand（HttpMessageConverter 经 Jackson 3）、@Valid 触发校验（失败抛 MethodArgumentNotValidException）；**4. Controller 方法执行**——编排 OrderService.place；**5. 返回值处理**——HandlerAdapter 的返回值处理器把 OrderDto 交给消息转换器序列化为 JSON；**6. 拦截器后置与 afterCompletion**——日志收尾、资源清理；**7. 异常路径**——任何环节抛出的异常经 HandlerExceptionResolver 进入 @RestControllerAdvice 统一转为 ProblemDetail。这条链路的工程意义：**每个环节都是可插拔的钩子**（拦截器/参数解析器/消息转换器/异常解析器），MVC 的"关注点分离"在框架层面被结构化为七个可替换的关卡——理解关卡顺序，调试"请求为什么 400/500"就是顺藤摸瓜。

## 测试策略：MockMvc 与契约测试

Controller 测试用 **MockMvc**（@WebMvcTest 切片）：验证路由映射、参数绑定、校验触发、异常映射四类行为——不启动完整 Spring 容器，测试速度毫秒级。2026 年注意点：Spring 7 弃用 UrlPathHelper.setAlwaysUseFullPath，改用 PathPatternParser——MockMvc 测试中需显式传 servletPath（`mockMvc.perform(get("/users").servletPath("/api"))`）。跨服务 API 用**契约测试**（Pact 类）锁定请求/响应 schema——这是微服务独立部署的前提（与 DDD 体系 10 篇的质量门禁呼应）。测试金字塔：Controller 测试薄（验证映射与绑定）、Service 测试厚（业务规则）、领域测试最厚（纯单元）。

补充一点测试策略的背景：@WebMvcTest 只装载 Controller 层（Controller + 转换器 + 校验器），服务与仓储用 @MockBean 打桩——所以它能毫秒级跑完且隔离性最好；跨层行为（绑定 → 服务 → 数据库）的真实链路交给 Service 层集成测试覆盖，两层测试各司其职，避免"Controller 测试里查库"的慢测试反模式。

测试的典型断言样例（MockMvc 一段式）：

```java
mockMvc.perform(get("/api/products/{id}", "P1001"))
       .andExpect(status().isOk())
       .andExpect(jsonPath("$.name").value("示例商品"))
       .andExpect(jsonPath("$.price.amount").value(19900));  // 值对象嵌套结构

mockMvc.perform(post("/api/orders").contentType(APPLICATION_JSON)
       .content("{\"productId\":\"P1001\",\"qty\":-1}"))      // 非法入参
       .andExpect(status().isBadRequest())
       .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));  // ProblemDetail 扩展字段
```

断言习惯：正例验证结构（JSON 路径逐字段）、反例验证契约（错误码与状态码）、边界值各一（分页/空列表/超大 ID）——三类断言覆盖"对的行为、错的行为、边界的行为"，是控制器测试的完整矩阵。

## 控制器层级的解剖与职责分配

一个典型订单控制器的解剖示例，演示薄控制器的完整形态：

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orders;        // 唯一依赖：服务接口

    @GetMapping("/{id}")
    public OrderDto get(@PathVariable OrderId id) {      // 值对象绑定
        return orders.findById(id);
    }

    @PostMapping
    @ResponseStatus(CREATED)
    public OrderDto create(@Valid @RequestBody CreateOrderCommand cmd) {  // 校验入参
        OrderId id = orders.place(cmd);       // 编排：一个服务调用
        return orders.findById(id);
    }

    @PostMapping("/{id}/cancel")
    public OrderDto cancel(@PathVariable OrderId id, @RequestBody CancelReason reason) {
        orders.cancel(id, reason);            // 业务规则在服务/领域层
        return orders.findById(id);
    }
}
```

解剖结果：每个方法都是"绑定入参 → 调服务 → 返回 DTO"的三段式，没有 if 业务分支、没有数据访问、没有 JSON 手工拼接。对照纪律检查：**业务规则**（"已发货不可取消"）在领域层（见 DDD 体系）；**事务边界**在服务层方法上；**权限校验**在方法级注解（@PreAuthorize）或拦截器；**横切逻辑**（日志/追踪）在拦截器。控制器"瘦身成功"的可观测信号：文件行数 <150、方法平均 <15 行、无任何 import 数据访问组件（JdbcTemplate/EntityManager）。

## 服务端渲染场景（SSR）的 MVC 补充

前后端分离是默认形态，但 SSR（Thymeleaf 等模板）场景仍有存量与适用场景，MVC 在其间有额外纪律：**控制器返回 ModelAndView 而非 DTO**——Model 是视图数据容器（键值映射），View 名经 ViewResolver 解析为模板；**模板即 View，必须只读呈现**——禁止在模板里写业务逻辑（Thymeleaf 的 th:if 只做展示分支，不做业务决策）；**表单提交走 PRG 模式**（Post-Redirect-Get）——POST 处理完重定向到 GET，防止刷新重复提交；**CSRF 防护**——表单提交场景必须开启 CSRF Token（前后端分离的 JSON API 则通常关闭，用 Token 认证替代）；**SSR 与 SPA 的边界**——内容型站点（SEO 敏感）用 SSR，应用型站点（交互复杂）用 SPA + 独立 API——2026 年的 Next.js/Nuxt 元框架则把两者融合（SSR 首屏 + 客户端水合），后端 MVC 退化为"纯 API 提供者"。SSR 场景的判断标准：SEO 需求 + 首屏速度 > 交互复杂度时选 SSR，否则 SPA。

## 生产级补充项

- **缓存**：GET 资源加 ETag/If-None-Match（Spring 的 ShallowEtagHeaderFilter）或 Cache-Control 头，压测时 QPS 差异显著；
- **压缩**：响应 gzip 压缩（Boot 默认开启需确认配置）;
- **幂等**：写操作（POST/PUT/DELETE）支持幂等键（Idempotency-Key 头），防重放——支付类接口必备；
- **分页**：列表接口默认分页（Spring Data 的 Pageable），防全表返回；分页参数统一校验（页大小上限、偏移量上限），防止恶意大页拖垮数据库；
- **DTO 而非实体**：序列化层用 record/DTO 显式声明契约，实体与 API 解耦；
- **审计与追踪**：Controller 入口统一生成请求 ID（traceId）贯穿日志（MDC），问题定位从"翻日志猜请求"变成"按 ID 串全链路"——这是生产排障的基础设施，不是可选优化；微服务场景 traceId 跨服务传递（OpenTelemetry），与仓库的日志监控指标体系衔接。

> 🎯 **核心要点**：后端 MVC 的 2026 形态一句话——"Controller 薄到只剩路由绑定编排，错误处理集中到 ProblemDetail，绑定强类型到值对象，测试用 MockMvc 锁定行为"。MVC 在后端的生命力来自它的纪律仍然正确：请求生命周期短、无状态、可测——这正是它与前端困境的本质差异。解剖示例给出了可观测的瘦身信号（<150 行、<15 行/方法、无数据访问 import），把"薄控制器"从口号变成可检查的工程标准。

---

**参考来源**：

- [Spring MVC Framework: A Practical, Modern Guide | TheLinuxCode](https://thelinuxcode.com/spring-mvc-framework-a-practical-modern-guide-to-building-maintainable-web-apps/)
- [Spring MVC @ModelAttribute Annotation (2026)](https://thelinuxcode.com/spring-mvc-modelattribute-annotation-2026-practical-mental-model-runnable-form-binding-example/)
- [Spring Annotations: The 2026 Essential Cheat Sheet | Marco Molteni](https://marmo.dev/spring-annotation-meaning)
- [sivalabs-agent-skills: spring-webmvc-rest-api reference](https://github.com/sivaprasadreddy/sivalabs-agent-skills/blob/main/skills/spring-boot/references/spring-webmvc-rest-api.md)
- [Building Java Web Applications Using Spring 7 | Pluralsight](https://www.pluralsight.com/courses/building-java-web-applications-using-spring-7)

**下一模块**：[06-Spring Framework 7 MVC 新特性](06-SpringFramework7MVC新特性.md) / **返回总览**：[00-MVC架构模式总览](00-MVC架构模式总览.md)
