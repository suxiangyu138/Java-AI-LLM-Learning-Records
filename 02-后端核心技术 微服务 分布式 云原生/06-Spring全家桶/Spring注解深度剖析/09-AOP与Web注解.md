# 09 AOP 与 Web 注解

> AOP 注解是"横切逻辑的声明语言"：@Pointcut 表达"切哪里"、五通知表达"织入什么"。Web 注解是"HTTP 与方法的映射协议"：@RequestMapping 表达"哪个请求"、参数注解表达"参数从哪来"。两者同属"边界层注解"——一个切方法调用链、一个切请求处理链，放在一篇是因为它们共享同一个心智：**声明匹配规则 + 注解绑定上下文**

## 📚 目录

1. [@Pointcut 表达式语法](#1-pointcut-表达式语法)
2. [五种通知注解与参数绑定](#2-五种通知注解与参数绑定)
3. [@EnableAspectJAutoProxy 与切面排序](#3-enableaspectjautoproxy-与切面排序)
4. [Web 映射与参数绑定注解](#4-web-映射与参数绑定注解)
5. [Web 异常与版本化注解](#5-web-异常与版本化注解)
6. [高频坑与面试题](#6-高频坑与面试题)

---

## 1. @Pointcut 表达式语法

@Pointcut 的 value 是 AspectJ 表达式，七个指示符按使用频率：`execution`（方法签名匹配，最常用）、`within`（类型范围）、`@annotation`（方法上带某注解）、`@within`（类上带某注解的所有方法）、`@target`（目标类带注解）、`@args`（参数带注解）、`bean("name*")`（Spring 扩展，按 Bean 名匹配——AOP 排除自调用场景的利器）。组合运算符 `&& || !` 与命名切点复用（`@Pointcut("execution(...)")` 定义命名切点，`@Before("pointcutName()")` 引用）。

execution 的完整骨架 `execution(修饰符 返回值 包.类.方法(参数) throws)` 中，参数部分常用 `..`（任意个数任意类型）与 `*`（单个任意）——`execution(* com.x.service.*.*(..))` 匹配 service 包全部方法。两个工程实践：切点表达式**集中定义在切面类**（复用 + 可读），`bean()` 指示符解决"按名称精确切入"的 Spring 特有需求。@Pointcut 方法只做声明（空方法体），它是元数据载体而非执行入口。

---

## 2. 五种通知注解与参数绑定

- **@Before**：前置，可拿 JoinPoint 参数（方法名/参数/目标对象）；
- **@AfterReturning**：正常返回后（`returning = "result"` 绑定返回值——result 类型必须与切点兼容，否则通知**不生效**且无报错，这是最隐蔽的通知失效形态）；
- **@AfterThrowing**：抛异常后（`throwing = "ex"` 绑定异常）；
- **@After**：无论成败（finally 语义，用于资源清理/指标记录等"无论如何都要做"的收尾）；
- **@Around**：环绕，唯一可控制是否执行目标方法（`ProceedingJoinPoint.proceed()` 决定调用），也是 @Transactional/@Cacheable 等拦截器的实现形态。

参数绑定三通道：JoinPoint/ProceedingJoinPoint 位置参数、`@annotation(myLog)` 绑定方法上的注解实例（01 篇的合成代理对象——属性读取已过别名解析）、`returning/throwing/args` 具名绑定。@Around 是性能敏感点：它包装整个调用链，`proceed()` 前后都占栈帧——非必要场景优先用轻量通知。审计日志切面的完整骨架：

```java
@Aspect
@Component
public class AuditAspect {

    @Pointcut("@annotation(auditLog)")
    public void auditPointcut(AuditLog auditLog) { }

    @Around(value = "auditPointcut(auditLog)", argNames = "pjp,auditLog")
    public Object around(ProceedingJoinPoint pjp, AuditLog auditLog) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            saveLog(auditLog.module(), auditLog.action(), "SUCCESS", System.currentTimeMillis() - start);
            return result;
        } catch (Throwable e) {
            saveLog(auditLog.module(), auditLog.action(), "FAIL:" + e.getMessage(), System.currentTimeMillis() - start);
            throw e;
        }
    }
}
```

切点声明带注解参数（`@Pointcut("@annotation(auditLog)")` 的方法参数形式）是标准姿势：切点与通知共用同一个注解实例，`argNames` 明确参数名映射（避免 -parameters 依赖）。审计日志的工程注意：入参记录必须**脱敏**、日志写入**异步化**（不阻塞业务链路）、失败场景用独立日志器不干扰业务日志。

---

## 3. @EnableAspectJAutoProxy 与切面排序

@EnableAspectJAutoProxy 的 `proxyTargetClass` 决定代理策略：true 强制 CGLIB（无接口类、方法级切点全覆盖）；false（默认）有接口用 JDK 代理——CGLIB 支持接口类但默认策略优先 JDK。`exposeProxy = true` 暴露 AopContext.currentProxy()（自调用穿透代理的补救手段，配合 06/07 篇的自调用失效场景使用——能解但脏，优先重构）。

多切面执行顺序由 **@Order** 控制（数字越小越先执行，环绕时"先入的切面后出"——洋葱模型）。两个切面都拦截同一方法时：@Order(1) 的 @Before 先跑、@Order(1) 的 @After 后跑。事务切面（TransactionInterceptor）默认 Order 为 LOWEST_PRECEDENCE（最后执行）——自定义环绕切面默认在事务切面**之外**，需要"切面内事务生效"时要把自定义切面 Order 调到事务之后。

---

## 4. Web 映射与参数绑定注解

@RestController = @Controller + @ResponseBody（返回值直写响应体，走 Jackson 3 转换器）。@RequestMapping 家族六变体的匹配属性：path（`{id}` 占位 + 正则）、params/headers（请求参数/头过滤）、consumes/produces（内容协商，不匹配 415/406）、method。Spring 7 新增 **version 属性**：`@GetMapping(path = "/{id}", version = "2.0+")` 声明版本区间，配合类级 **@ApiVersion("v1")** 实现同路径多版本共存（版本解析策略可配 header/媒体类型/URL）。@ResponseBody 的底层是 HttpMessageConverter 链——按 Accept/Content-Type 选择转换器（Jackson 3 是 JSON 默认实现），自定义媒体类型（Protobuf 等）注册转换器后注解层零改动即可分派，这是"注解声明 + 转换器链分派"的扩展模式。

参数绑定五注解的语义分工：@RequestParam（查询参数，**required 默认 true**，缺参 400）；@PathVariable（路径占位，**参数名必须与占位符一致**，依赖 -parameters 编译参数）；@RequestBody（请求体反序列化，一个方法只能一个）；@RequestHeader（头绑定）；@CookieValue（Cookie 绑定）。@ModelAttribute（表单/查询对象绑定——**实体类禁止直接绑定**，批量赋值防护靠 DTO 或 @InitBinder 白名单）；@RequestPart（multipart 文件 + JSON 混合）。

---

## 5. Web 异常与版本化注解

@ExceptionHandler 的处理范围按"就近原则"：Controller 内优先于全局 @ControllerAdvice（@RestControllerAdvice 是其 JSON 版），异常类型最具体的处理器胜出（同类型时先声明者优先）。Spring 7 推荐返回 **ProblemDetail**（RFC 9457：type/title/status/detail/instance + 自定义属性）。@ResponseStatus(code) 给异常/方法固定状态码（reason 属性已废弃）。

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ProblemDetail handleBiz(BizException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        pd.setTitle("业务校验失败");
        pd.setProperty("code", e.getCode());
        return pd;
    }
}
```

全局异常处理的工程要点：按异常类型分层（业务异常 → 400 语义化、校验异常 → 字段级错误、兜底 Exception → 500 + traceId）；`ProblemDetail` 与自建错误体（{code, msg, data}）二选一写进规范（协议化 vs 前端契约，国内团队自建体仍为主流，两者都合理但不要混用）；兜底处理器必须吞掉内部细节（堆栈进日志、traceId 进响应）。多模块项目用多个 @ControllerAdvice + basePackages 按包隔离异常语义（订单模块与支付模块的错误码体系各自维护）。

**@ApiVersion 版本化的完整心智**：类级声明控制器版本（V1/V2 两个控制器类同路径共存），方法级 version 区间让单方法跨版本服务；策略配置决定版本信号来源（`X-API-Version` 头、媒体类型 `application/vnd.api.v2+json`、URL 段），未配置策略时版本歧义直接报错。这是社区自造 @ApiVersion 注解转正为框架能力的 2026 标志性变化。@CrossOrigin 细节（origins 白名单、allowCredentials 与 `*` 冲突、maxAge 预检缓存）同属此层——注解只留例外场景，全局 CORS 走 CorsRegistry。

---

## 6. 高频坑与面试题

1. **@AfterReturning 绑定类型不匹配静默失效**：returning 类型与切点返回值不兼容时通知整体不执行且无告警——排查"通知没生效"先查绑定类型（如切点方法返回 Object 但 returning 声明为具体子类型，子类方法永不触发）；
2. **切面 Order 与事务的关系**：自定义切面默认在事务外层，事务内需自定义逻辑时要把 Order 调到事务切面之后（或理解洋葱序的进入/退出方向）；反过来"切面在事务内抛异常会怎样"也由洋葱序决定——异常穿透顺序即切面出栈顺序；
3. **@PathVariable 参数名不匹配**：开 -parameters；@RequestParam 可选参数忘写 required = false 是 400 事故榜首；
4. **面试必答框架**：「@Transactional 与自定义 @Around 切面谁先执行？」——按 @Order 洋葱序，事务切面默认 Order 最低（最后执行、最先进入/最后退出）——自定义切面默认包在事务**外**，切面里读不到事务内未提交状态，需要时显式调序；「@ApiVersion 解决什么？」——多版本共存的正规化：老方案 URL 前缀污染路由、自造注解互不通用，框架级 version 属性 + 三策略解析让灰度期新旧版本并行；
5. **「@ControllerAdvice 的 basePackages 为什么重要？」**——多模块项目各模块异常语义不同（订单模块 4xx 业务码、支付模块另一套），全局唯一的 @ControllerAdvice 会变成 if-else 泥潭；按包隔离的多个 @ControllerAdvice 让异常语义随模块边界切分，再配合 ProblemDetail 统一错误结构（语义分层、结构统一）；
6. **「AOP 自调用失效的官方补救是什么？」**——@EnableAspectJAutoProxy(exposeProxy = true) + AopContext.currentProxy() 拿代理再调用——能解但引入代码异味，官方立场是"重构优先"（抽独立 Bean 或注入自身），exposeProxy 是存量代码的过渡手段。

---

**下一模块**：[10 测试与自研注解工程](./10-测试与自研注解工程.md) · **返回总览**：[00 总览](./00-Spring注解深度剖析总览.md)

**相关体系**：[Spring 框架核心（AOP 面向切面编程篇）](../Spring框架核心/00-Spring框架核心知识体系总览.md) · [SpringBoot Web（请求处理全链路）](../../09-Web开发全流程/SpringBoot%20Web/00-SpringBootWeb总览.md)

---

【参考来源】
- [Spring Annotations: The 2026 Essential Cheat Sheet（Marco Molteni）](https://marmo.dev/spring-annotation-meaning)
- [Spring Framework 6→7 迁移指南（@ApiVersion/PathPatternParser）](https://dev.to/ankurm/spring-framework-6-to-7-migration-guide-breaking-changes-deprecated-apis-and-upgrade-checklist-3bf6)
- [What's New in Spring Boot 4（Dan Vega, KCDC 2026）](https://www.danvega.dev/speaking/kcdc-2026-whats-new-in-spring-boot-4)
- [聚焦 Spring Framework 7 与 Spring Boot 4：Spring 团队专访（InfoQ 中文）](https://www.infoq.cn/article/z4msV9uzNy7CXYFC4K2J)
