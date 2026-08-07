# 00 Spring Validation 组件总览

> 组件卡片：Spring Validation 是什么、版本现状、能做什么、与深度体系如何衔接——本系列是"组件汇总"速查型文档；Spring Validation 是 Spring Framework 内置的校验抽象层（非独立项目），暂无独立深度体系，深挖见官方参考文档

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

**Spring Validation 是 Spring Framework 内置的校验抽象层**——`org.springframework.validation` 包把"Bean Validation（jakarta.validation）+ 数据绑定 + 方法级校验"统一到一套 API，让 @Valid/@Validated 在 MVC、WebFlux、Service 方法上处处生效。

```text
核心心智模型：
  校验 API（org.springframework.validation）
    ├── Validator（Spring 自有接口）：supports + validate
    ├── Errors / BindingResult：错误容器
    └── DataBinder：数据绑定 + 校验一体
       │
  与 Bean Validation（jakarta.validation）的桥
    ├── LocalValidatorFactoryBean：JSR-303 Provider → Spring Validator
    ├── @Valid（JSR-303 触发）/ @Validated（Spring 扩展：分组 + 方法级）
    └── 约束注解：@NotNull @Size @Pattern @Valid（嵌套）…
       │
  三个应用场景
    ├── MVC 参数校验：@RequestBody/@ModelAttribute → MethodArgumentNotValidException
    ├── 方法约束校验：Controller/Service 方法参数 → HandlerMethodValidationException（6.1+）
    └── 编程式校验：Validator/ValidationUtils 手动 validate
```

> 🎯 **一句话**：Spring Validation = "框架的校验总闸"——注解写约束、框架跑校验、异常统一接，业务代码零 try-catch。

## 2. 版本现状（2026-08）

| 维度 | 现状 |
|------|------|
| 载体 | Spring Framework 内置（spring-context / spring-web），**无独立版本号**，随框架 7.0.x |
| Bean Validation 基线 | **3.1**（SF 7 提升的最低要求） |
| 校验实现 | Hibernate Validator 9.x（Spring Boot 4 / SF 7 配套线） |
| 命名空间 | Jakarta EE 11 全面转向 `jakarta.validation`（`javax.validation` 已移除） |
| 相关注解迁移 | `javax.annotation`/`javax.inject` 全部改为 `jakarta.*` |

关键演进（面试点）：

- **6.1 引入方法约束校验**：Controller 方法参数上的约束注解（@NotNull/@Min）直接生效，抛 `HandlerMethodValidationException`（取代旧 AOP 方案的可选路径）；
- **7.0 全面 Jakarta**：Spring Validation 只认 `jakarta.validation`；老的 javax 系注解/依赖在 SF 7 直接失效；
- **Boot 4**：`spring-boot-starter-validation` 仍是标准入口（内含 Hibernate Validator + Jakarta EL）。

> 💡 写代码注意：**约束注解用 `jakarta.validation.constraints.*`；@Valid 用 `jakarta.validation.Valid`；@Validated 用 `org.springframework.validation.annotation.Validated`**——三者的包名别混。

## 3. 能力地图

| 能力域 | 能力 | 代表 API |
|--------|------|---------|
| 接口校验 | 自有 Validator 接口 | `Validator`、`ValidationUtils`、`Errors`/`BindingResult` |
| Bean Validation 集成 | 把 JSR-303 Provider 桥进 Spring | `LocalValidatorFactoryBean`、`SpringValidatorAdapter` |
| MVC 参数校验 | @RequestBody/@ModelAttribute 校验 | `@Valid`/`@Validated` → `MethodArgumentNotValidException` |
| 方法约束校验 | 方法参数/返回值约束（6.1+） | `HandlerMethodValidationException`、`ParameterValidationResult` |
| 分组校验 | 按场景启用部分约束 | `@Validated(GroupA.class)`、`@GroupSequence` |
| 嵌套校验 | 对象内对象递归校验 | 约束字段上加 `@Valid` |
| 自定义约束 | 业务专属注解 | `@Constraint(validatedBy=...)` + `ConstraintValidator` |
| 数据绑定 | 表单/参数绑定 + 校验一体 | `DataBinder`、`WebDataBinder` |
| 方法级（AOP） | 任意 Bean 方法校验 | `@Validated`（类级）+ `MethodValidationPostProcessor` |

## 4. 与深度体系的映射

| 本卡片模块 | 深度体系对应 | 衔接说明 |
|-----------|-------------|---------|
| 02 核心类 | [Spring框架核心 深度体系](../../../Spring框架核心/00-Spring框架核心知识体系总览.md) | 校验是容器能力（Validator/DataBinder 由容器装配），与 DI/AOP 同源 |
| 04 集成地图 | [SpringMVC 深度体系](../../../SpringMVC/00-SpringMVC知识体系总览.md)（若有）| MVC 参数校验的异常处理链路 |
| 02 核心类 | [Spring Batch 深度体系-05 ItemProcessor 与 ItemWriter](../../../Spring Batch/05-ItemProcessor与ItemWriter体系.md) | Batch 的 ValidatingItemProcessor 正是这套校验抽象在批处理中的应用 |

> 💡 Spring Validation 生态位：它是**框架内置能力而非独立组件**——每个 Spring 应用都隐式拥有；本仓库暂无独立深度体系，深入以 [Spring Framework 官方文档 Validation 章节](https://docs.spring.io/spring-framework/reference/core/validation.html) 为准。

## 5. 快速上手 3 步

```text
① 引入 spring-boot-starter-validation（Boot 4 管理版本）
② 约束注解标在 DTO/参数上，@Valid/@Validated 触发
③ 全局异常处理器接住 MethodArgumentNotValidException / HandlerMethodValidationException
```

```java
// MVC 参数校验三件套
public record CreateOrderRequest(
        @NotBlank String customerId,
        @NotNull @Positive BigDecimal amount,
        @Size(max = 200) String remark) {}

@PostMapping("/orders")
Order create(@Valid @RequestBody CreateOrderRequest request) { ... }

// 全局统一异常处理（ControllerAdvice）
@ExceptionHandler(MethodArgumentNotValidException.class)
ResponseEntity<?> onBadBody(MethodArgumentNotValidException ex) {
    List<String> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage()).toList();
    return ResponseEntity.badRequest().body(errors);
}
```

> 💡 运行即得：Boot 4 自动配置 LocalValidatorFactoryBean（classpath 有 validator 时）；DTO 用 **record** 时约束注解加在组件上，校验与构造天然一体。

## 6. 速查导航

| 卡片 | 内容 | 何时翻 |
|------|------|--------|
| [01-模块清单](01-模块清单.md) | 无独立模块的"依赖真相" | 加依赖、解版本冲突 |
| [02-核心类与注解速查](02-核心类与注解速查.md) | 校验 API/注解/异常 | 写校验时随手查 |
| [03-配置属性速查](03-配置属性速查.md) | starter 配置、消息资源、属性 | 写配置时 |
| [04-集成地图与常见问题](04-集成地图与常见问题.md) | MVC/WebFlux/Batch 集成 + 高频坑 | 集成/排障 |

## 7. 学习路线推荐

```text
快速上手：00 → 01 → 05（三步）→ 02 核心类
Web 应用校验：00 → 02（异常体系）→ 04（MVC/WebFlux 集成）
框架深度：00 → 02（DataBinder/方法校验）→ Spring Framework 官方 Validation 章节
```

## 8. 核心概念速查

| 概念 | 一句话 |
|------|--------|
| Validator | Spring 自有校验接口（supports/validate） |
| @Valid vs @Validated | 触发校验 vs 触发+分组+方法级 |
| LocalValidatorFactoryBean | Bean Validation → Spring 的桥（Boot 自动配置） |
| MethodArgumentNotValidException | @RequestBody/@ModelAttribute 校验失败 |
| HandlerMethodValidationException | 方法参数约束校验失败（6.1+） |
| 分组 | 按场景启用约束子集 |
| 嵌套校验 | 字段上加 @Valid 递归校验 |
| ConstraintValidator | 自定义约束的实现接口 |

---

> 🎯 **核心要点**：Spring Validation 不是独立组件而是框架内置抽象——记住三对关系：**@Valid（触发）vs @Validated（触发+分组+方法级）、MethodArgumentNotValidException（参数对象）vs HandlerMethodValidationException（方法约束）、jakarta 包名（SF 7 唯一）**。

**下一模块**：[01-模块清单](01-模块清单.md)
