# 05 Jakarta 校验注解剖析

> Jakarta Validation（原 Bean Validation）把"入参合法性"从 if-else 地狱变成声明式注解。它真正难的不是注解本身，而是三组容易混淆的概念：@NotNull/@NotEmpty/@NotBlank 的语义边界、@Valid 的级联传播、分组校验的作用域。校验失效时，问题十有八九出在这三处

## 📚 目录

1. [体系与版本基线](#1-体系与版本基线)
2. [内置约束：三兄弟与数值族](#2-内置约束三兄弟与数值族)
3. [级联校验：@Valid 的传播机制](#3-级联校验valid-的传播机制)
4. [@Validated、分组与自定义约束](#4-validated分组与自定义约束)
5. [Spring Boot 4 集成与高频坑](#5-spring-boot-4-集成与高频坑)

---

## 1. 体系与版本基线

校验体系分三层：**规范**（Jakarta Validation，3.1 随 Jakarta EE 11 现行，4.0 目标 EE 12、目前仅 4.0.0-M1 里程碑）、**参考实现**（Hibernate Validator，9.1.2.Final 2026-07-06）、**集成方**（Spring Boot 的 `spring-boot-starter-validation` 自动装配）。使用层面记住一个结论：**注解来自 `jakarta.validation.constraints` 包，实现几乎永远是 Hibernate Validator**，版本选型时三者要匹配（Boot 4 对应 Validation 3.1 + HV 9.x）。

约束注解的 `@Retention(RUNTIME)` 是必然的：校验发生在运行时反射字段值、执行 `ConstraintValidator`，整条链路没有编译期成分。

---

## 2. 内置约束：三兄弟与数值族

@NotNull/@NotEmpty/@NotBlank 是面试与事故双料高频点，语义边界一张表说清：

| 注解 | null | 空串 "" | 纯空白 "   " | 适用类型 |
|------|:---:|:---:|:---:|------|
| @NotNull | ❌ 拒绝 | ✅ 放行 | ✅ 放行 | 任意对象（包括 String/集合） |
| @NotEmpty | ❌ 拒绝 | ❌ 拒绝 | ✅ 放行 | CharSequence/Collection/Map/数组 |
| @NotBlank | ❌ 拒绝 | ❌ 拒绝 | ❌ 拒绝 | 仅 CharSequence |

两个使用禁忌：@NotEmpty/@NotBlank 不能标在 int 等非适用类型上（抛 `UnexpectedTypeException`，因为没有对应 ConstraintValidator 实现）；@NotBlank 本质是 `@NotNull` 的语义超集，标了 @NotBlank 就不需要再标 @NotNull。

数值族与格式族：`@Min/@Max`（整数边界，含边界值）、`@DecimalMin/@DecimalMax`（小数，value 为字符串）、`@Positive/@PositiveOrZero/@Negative`、`@Digits(integer=, fraction=)`（精度控制，金额场景关键）、`@Size(min=, max=)`（字符串长度或集合大小，对 null 放行——配合 @NotNull 才是完整约束）、`@Email`（宽松 RFC 匹配，`a@b` 都能过，严格校验需 @Pattern 或自定义）、`@Pattern(regexp=)`（正则，注意 `\` 在注解字符串里要转义）、`@Past/@Future`（时间比较）。

剩余内置约束按用途归类：布尔族 `@AssertTrue/@AssertFalse`（跨字段逻辑断言，如"两个字段至少一个非空"）、时间族 `@PastOrPresent/@FutureOrPresent`（含边界）、空值族 `@Null`（少见但有用——声明字段"必须为空"，如删除接口的更新字段）。Hibernate Validator 还提供规范外的扩展注解：`@Length`（@Size 的 Hibernate 版）、`@Range`（@Min+@Max 组合）、`@URL`（URL 校验，比 @Pattern 写 URL 正则可靠）、`@ScriptAssert`（脚本断言，**不推荐**——表达式注入风险与性能差，用 Java 代码写自定义约束替代）。

---

## 3. 级联校验：@Valid 的传播机制

校验器默认只检查**当前对象**的字段，遇到对象字段、集合字段时**不深入内部**。`@Valid` 的唯一职责就是告诉校验器"继续往下走"：

```java
public class Order {
    @NotNull
    private User user;              // 只校验非空，不校验 User 内部
    @Valid
    private User validUser;         // 继续校验 User 内部所有约束
    @Valid
    private List<@NotNull Item> items;  // 容器元素级联：每个元素非空且内部被校验
}
```

容器元素注解的位置语义是高频考点：`List<@NotNull Item>` 约束的是**元素不能为 null**，`List<@Valid Item>` 是元素内部级联，`@NotNull List<Item>` 是集合本身非空——三者可以叠加但含义完全不同。Map 场景 `Map<@NotBlank String, @Valid Item>` 可分别约束键值。漏写 @Valid 是"校验静默失效"的第一大原因：不报错、不警告，只有数据脏了才发现。

> 🎯 **核心要点**：级联是显式声明（@Valid），不是默认行为。任何嵌套 DTO/集合字段，要么确认"内部无需校验"，要么补 @Valid。

---

## 4. @Validated、分组与自定义约束

**@Validated vs @Valid**：@Valid 是规范注解（`jakarta.validation.Valid`），只有"级联"一个语义；@Validated 是 Spring 的包装，追加两个能力——**分组**（`@Validated(Group.class)` 指定校验哪组）与**方法级校验**（类上标 @Validated 后，方法参数/返回值的约束才生效）。只用级联场景两者等价，Spring 项目里常混用：嵌套字段用 @Valid，Controller 参数用 @Validated 分组。

**分组校验**：约束可声明所属分组 `@NotBlank(groups = Create.class)`，校验时指定分组则**只执行该组的约束**。最深的坑是 **Default 组语义**——未显式声明 groups 的约束属于 `Default` 组，而 `@Validated(Create.class)` 不会执行 Default 组约束，且**级联 @Valid 只传播 Default 组**。所以引入分组后，公共约束必须显式声明 `groups = {Create.class, Update.class}` 或让分组接口继承 Default。`@GroupSequence` 还能定义组间校验顺序（先便宜约束后昂贵约束），配合 `failFast = true` 实现快速失败。

**自定义约束**的标准姿势是两步：定义注解（`@Constraint(validatedBy = XxxValidator.class)` + @Target/@Retention(RUNTIME) + `message()`/`groups()`/`payload()` 三要素）+ 实现 `ConstraintValidator<注解, 类型>`。

```java
// 文件一：注解定义
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneValidator.class)
@Documented
public @interface Phone {
    String message() default "手机号格式非法";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

// 文件二：校验逻辑
public class PhoneValidator implements ConstraintValidator<Phone, String> {
    private static final Pattern P = Pattern.compile("^1[3-9]\\d{9}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null) return true;      // null 由 @NotNull 负责，这里放行
        return P.matcher(value).matches();
    }
}
```

规范强制 message/groups/payload 三要素齐全（缺失时框架报错）。`message` 支持参数插值：`"长度必须在 {min} 到 {max} 之间"` 中的 `{min}/{max}` 自动替换为注解属性值，`{value}` 代表注解 value；自定义约束的 message 放 `ValidationMessages_zh_CN.properties` 里做国际化。三个工程细节：

1. **Validator 实例是单例**：`initialize` 只调一次，实现类必须无状态（不能缓存请求数据）；**null 放行是铁律**——null 交给 @NotNull 分层负责，自定义校验器不重复判空；多类型复用用 `ConstraintValidator<A, Object>` 配合 `isValid` 里 instanceof 分发；
2. **组合约束**（composed constraint）：新注解上直接叠加既有注解（如自定义 `@StrongPassword` 上叠 @Size/@Pattern），`@ReportAsSingleViolation` 让多条子约束合并为一条报错；
3. **message 国际化**：`{jakarta.validation.constraints.NotNull.message}` 走 MessageSource 解析，中文项目配 ValidationMessages_zh_CN.properties。

---

## 5. Spring Boot 4 集成与高频坑

Spring Framework 7 中方法级校验升级为独立 `MethodValidation` 拦截器（Bean Validation 3.1 支持参数与返回值双向校验），Controller 层由 `@Validated`/`@Valid` 参数触发，返回 400 + `MethodArgumentNotValidException`；Service 层类标 @Validated 后参数约束抛 `ConstraintViolationException`（500，需全局异常处理器兜底转换）。

```java
@Service
@Validated
public class OrderService {
    public Order create(@NotNull @Valid CreateOrderCmd cmd) {   // 参数约束直接声明在方法上
        return orderRepo.save(cmd.toOrder());
    }
}
```

分组校验的标准应用形态是"同一 DTO 服务两种场景"：Create 场景要求密码必填、Update 场景允许不传密码（null 表示不修改）。实现上定义 `Create`/`Update` 两个分组接口，`@NotBlank(groups = Create.class)` 挂在 password 上，Controller 分别 `@Validated(Create.class)`/`@Validated(Update.class)`——比拆两个 DTO 省事，但要注意 04 篇所述 Default 组丢失陷阱（公共约束显式声明进两个分组）。DTO 拆分派会反驳：两种场景的字段集差异大到一定程度，拆 DTO 更诚实。工程折中：字段差异小用分组，差异大拆 DTO。

**jakarta 迁移红线**：Spring 7 起 `javax.validation.*` 注解编译可通过但**被静默忽略**——校验全部失效且无任何报错，升级时必须全局替换 import 为 `jakarta.validation.*`。

高频坑清单：@Email 过于宽松需自定义；@Size 不挡 null；分组引入后 Default 组丢失；漏 @Valid 级联失效；Validator 单例里放状态；`@Validated` 标在未实现接口的类上方法校验不生效（代理机制要求）；嵌套 record 组件校验同样需要 @Valid；`@NotBlank` 标注非字符串类型抛 `UnexpectedTypeException`（约束与类型不匹配是配置期错误，启动/首请求即暴露）。

程序化校验（非 Controller 入口）的标准姿势是 `Validator` 注入 + `ConstraintViolation` 集合处理：

```java
Set<ConstraintViolation<Order>> vs = validator.validate(order);
if (!vs.isEmpty()) {
    throw new BizException(vs.iterator().next().getMessage());
}
```

`getPropertyPath()` 能定位到具体字段（嵌套对象的路径如 `user.phone`），适合把多条违规拼成结构化错误返回。`Validator` 实现 `failFast` 属性（Hibernate Validator 特有）让校验在第一个违规处停止，大对象 + 慢校验器场景收益明显；但 API 层通常需要**全部错误一次性返回**给前端，此时不要开 failFast。

**校验异常的三条路径**要在全局异常处理器里区分：`MethodArgumentNotValidException`（Controller @RequestBody 校验失败，400，errors 里含字段级明细）、`BindException`（@ModelAttribute 表单绑定失败，400）、`ConstraintViolationException`（方法级校验/程序化校验，默认 500 需转换）。统一的错误契约（字段名 + message 数组）应在这三个处理器中收敛成同一结构，否则前端要面对三套错误格式——校验体系的工程成熟度就看这一层。

面试速答：「@Valid 与 @Validated 的区别？」——@Valid 是 Jakarta 规范注解只做级联；@Validated 是 Spring 扩展，多出分组与方法级校验两个能力；「为什么校验注解不自动级联？」——校验对象内部需要成本决策，规范选择显式化，漏写 @Valid 是静默失效第一大原因；「自定义约束的 Validator 为什么必须无状态？」——框架以单例缓存 Validator 实例，放状态会跨请求串数据。

---

**下一模块**：[06 Spring 核心注解剖析](./06-Spring核心注解剖析.md) · **返回总览**：[00 总览](./00-Java常用注解知识体系总览.md)

**相关体系**：[SpringBoot Web（参数校验与异常处理链路）](../../../02-后端核心技术%20微服务%20分布式%20云原生/09-Web开发全流程/SpringBoot%20Web/00-SpringBootWeb总览.md)

---

【参考来源】
- [Jakarta Validation 规范页（3.1 现行 / 4.0 目标 EE 12）](https://jakarta.ee/specifications/bean-validation/)
- [Jakarta Validation 4.0.0-M1 规范草案](https://jakartaee-specifications.netlify.app/specifications/bean-validation/4.0/jakarta-validation-spec-4.0.0-m1)
- [Hibernate Validator Releases（9.1.2.Final 2026-07-06）](https://hibernate.org/validator/releases/)
- [Bean Validation 官方新闻源](https://beanvalidation.org/news/news.atom)
- [Spring Framework 6→7 迁移指南（javax 静默忽略与 MethodValidation）](https://dev.to/ankurm/spring-framework-6-to-7-migration-guide-breaking-changes-deprecated-apis-and-upgrade-checklist-3bf6)
