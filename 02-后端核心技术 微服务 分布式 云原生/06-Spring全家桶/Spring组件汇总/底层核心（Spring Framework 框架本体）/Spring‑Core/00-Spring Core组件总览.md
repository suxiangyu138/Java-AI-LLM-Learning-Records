# 00 Spring Core 组件总览

> 组件卡片：spring-core 是什么、版本现状、能做什么、与深度体系如何衔接——一切 Spring 模块的地基

---

## 📚 目录

1. [组件一句话定位](#1-组件一句话定位)
2. [版本现状（2026-08）](#2-版本现状2026-08)
3. [能力地图](#3-能力地图)
4. [与深度体系的映射](#4-与深度体系的映射)
5. [快速上手 3 步](#5-快速上手-3-步)
6. [速查导航](#6-速查导航)

---

## 1. 组件一句话定位

**spring-core 是 Spring 全家桶的"地基中的地基"**——注解体系（@Component 家族、@AliasFor）、工具类（Assert/StringUtils/ReflectionUtils）、类型系统（ResolvableType）、类型转换（ConversionService）、资源抽象（Resource）、SpEL 表达式、环境接口（Environment）全部定义于此；**没有任何 Spring 模块不依赖它**。

```text
核心心智模型：
  spring-core = 零依赖的基础设施层（可选 Kotlin/GraalVM 支持）
    ├── 注解体系   @Component 家族 / @AliasFor / @Indexed / null-safety
    ├── 工具类     Assert / StringUtils / ReflectionUtils / StopWatch ...
    ├── 类型系统   ResolvableType / TypeDescriptor / ConversionService
    ├── 资源抽象   Resource / ResourceLoader / 类路径与 URL 统一
    ├── 表达式     SpEL（SpelExpressionParser）
    ├── 环境接口   PropertyResolver / Environment（实现装配在 context）
    └── 重试（7.0） RetryTemplate / RetryPolicy（Spring Retry 并入）
```

| 维度 | 说明 |
|------|------|
| 所属 | Spring Framework 底层核心模块（spring-core） |
| 版本线 | 随 Spring Framework 7.x（2025-11 起） |
| 语言要求 | Java 17+（官方推荐 JDK 25 LTS） |
| 定位 | 一切模块的基础设施层（无容器、无 Bean，只有"能力"） |

## 2. 版本现状（2026-08）

| 版本 | 说明 |
|------|------|
| Spring Framework 7.0（2025-11） | 当前主线（Boot 4.0 配套） |
| Spring Framework 7.1（2026-05） | 最新维护线（Boot 4.1 配套） |
| Spring Framework 6.2.x | 存量主线（Boot 3.x 配套） |

**7.x core 关键变化：**

- **`spring-jcl` 模块移除**：日志桥接改为直接依赖 Apache Commons Logging 1.3.0——对应用几乎透明（传递依赖），但显式声明过 `spring-jcl` 的项目需移除；
- **`ListenableFuture` 移除**：`org.springframework.util.concurrent` 中的 ListenableFuture 删除，统一用 `CompletableFuture`；
- **Spring Retry 并入 core**：新包 `org.springframework.core.retry`（`RetryTemplate`/`RetryPolicy`/BackOff 策略），配套注解在 spring-context（`@Retryable`/`@ConcurrencyLimit`/`@EnableResilientMethods`）——见 [07-重试与弹性速查](07-重试与弹性速查.md)；
- **SpEL 增强**：`Optional` 空安全导航与 Elvis 运算符自动解包；**单次求值操作上限 10,000 次**（7.0.8 起，可配 `spring.expression.maxOperations`）；
- **空安全注解迁移**：Spring 自带 JSR 305 语义空注解废弃，代码库迁移到 **JSpecify**（`org.jspecify.annotations`）——第三方代码基于旧注解的编译警告增多；
- **AOT 元数据统一格式**：资源提示由正则改 glob（`"/files/*.ext"` 不再匹配嵌套路径，用 `"/files/**/*.ext"`）；注册类型反射提示自动覆盖方法/构造器/字段；
- **类文件读取**：Java 24+ 使用 JEP 484 Class-File API（`ClassFileMetadataReader`）替代精简 ASM 实现扫描组件。

> ⚠️ **要点**：core 的 7.0 变化几乎不破坏业务代码——唯一注意项是显式依赖 spring-jcl 的项目（移除）与 AOT 提示写法（glob）；迁移到 JDK 25 LTS 可获得 ClassFileMetadataReader 带来的扫描加速。

## 3. 能力地图

| 能力域 | 能力点 | 关键类/API |
|--------|--------|-----------|
| 注解 | 组件注解族 | `@Component`/`@Service`/`@Repository`/`@Controller` |
| 注解 | 组合/元注解 | `@AliasFor`、`@Indexed`、`MergedAnnotations` |
| 空安全 | 空注解 | `@NonNull`/`@Nullable`/`@NonNullApi`（7.0 起 JSpecify） |
| 工具 | 断言/字符串/反射 | `Assert`/`StringUtils`/`ReflectionUtils`/`ClassUtils`/`StopWatch` |
| 类型系统 | 泛型与类型描述 | `ResolvableType`/`TypeDescriptor`/`GenericTypeResolver` |
| 类型转换 | 转换服务 | `ConversionService`/`Converter`/`GenericConverter` |
| 资源 | 统一资源抽象 | `Resource`/`ResourceLoader`/`classpath*:` 模式 |
| 表达式 | SpEL | `SpelExpressionParser`/`StandardEvaluationContext` |
| 环境接口 | 属性解析 | `PropertyResolver`/`Environment`（实现装配在 context） |
| 弹性（7.0） | 重试 | `RetryTemplate`/`RetryPolicy`/`BackOffPolicy`（`core.retry` 包） |
| 序列化/编码（7.x） | 通用编解码 | `SerializationUtils`/`Codec`（`core.codec` 供 web 用） |

## 4. 与深度体系的映射

| 速查文档 | 深度体系模块 |
|---------|-------------|
| 02-注解体系速查 | [Spring框架核心-02-BeanDefinition与三种配置方式](../../../Spring框架核心/02-BeanDefinition与三种配置方式.md) |
| 04-类型系统与类型转换速查 | [Spring框架核心-03-依赖注入详解与自动装配](../../../Spring框架核心/03-依赖注入详解与自动装配.md) |
| 05-资源抽象速查 | [Spring框架核心-09-资源国际化与测试](../../../Spring框架核心/09-资源国际化与测试.md) |
| 06-SpEL 表达式速查 | [Spring框架核心-03（@Value 与 SpEL）](../../../Spring框架核心/03-依赖注入详解与自动装配.md) |
| 08-集成地图与常见问题 | [Spring Core-00-源码专题总览](../../../Spring Core/00-SpringCore专题总览.md) |

> 💡 本系列定位"查得快"，源码专题定位"读得深"——容器源码、BeanDefinition 解析机制见 [Spring Core-01~05](../../../Spring Core/00-SpringCore专题总览.md)；core 的能力大多"藏在背后"被 beans/context 消费，深挖入口在 Spring-Beans/Context 两系列。

## 5. 快速上手 3 步

**① 引入依赖**（通常无需显式——任何 Spring 模块都会传递引入）：

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-core</artifactId>
    <version>7.0.6</version>
</dependency>
```

**② 直接使用核心能力**：

```java
// 断言与字符串
Assert.hasText(orderId, "orderId must not be blank");
String name = StringUtils.capitalize("order");          // "Order"

// 反射工具
Method m = ReflectionUtils.findMethod(OrderService.class, "create", Long.class);

// ResolvableType：泛型运行时解析
ResolvableType rt = ResolvableType.forClassWithGenerics(List.class, String.class);
System.out.println(rt.getGeneric(0).getType());          // class java.lang.String

// SpEL
ExpressionParser parser = new SpelExpressionParser();
String v = parser.parseExpression("'Hello ' + 'World'").getValue(String.class);

// Resource
Resource r = new ClassPathResource("templates/email.html");
```

**③ 用重试（7.0 内建弹性）**：

```java
RetryTemplate template = RetryTemplate.builder()
        .maxRetries(3)                                // 7.0 起为 maxRetries
        .exponentialBackoff(200, 2, 5_000)
        .build();

String result = template.execute(ctx -> remoteCall(), ctx -> fallback(ctx));
```

## 6. 速查导航

| 文档 | 内容 |
|------|------|
| [01-模块清单](01-模块清单.md) | spring-core artifact 与包结构、spring-jcl 移除、依赖边界 |
| [02-注解体系速查](02-注解体系速查.md) | @Component 家族 / @AliasFor / @Indexed / 空安全（JSpecify） |
| [03-核心工具类速查](03-核心工具类速查.md) | Assert/StringUtils/ReflectionUtils/StopWatch 等 |
| [04-类型系统与类型转换速查](04-类型系统与类型转换速查.md) | ResolvableType / ConversionService / Converter |
| [05-资源抽象速查](05-资源抽象速查.md) | Resource 接口与实现、classpath*/ant 模式 |
| [06-SpEL 表达式速查](06-SpEL表达式速查.md) | 语法、上下文、安全限制（7.0） |
| [07-重试与弹性速查](07-重试与弹性速查.md) | core.retry + @Retryable/@ConcurrencyLimit（7.0） |
| [08-集成地图与常见问题](08-集成地图与常见问题.md) | 被谁消费 + 高频坑 |

---

**下一模块**：[01-模块清单](01-模块清单.md)　**返回总览**：本页

**【参考来源】**：[Spring Framework 7.0 Release Notes（GitHub Wiki）](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-7.0-Release-Notes)、[Spring Framework 7.0 正式发布解析（掘金）](https://juejin.cn/post/7573592127298682930)、[Spring 6→7 Migration Guide（dev.to）](https://dev.to/ankurm/spring-framework-6-to-7-migration-guide-breaking-changes-deprecated-apis-and-upgrade-checklist-3bf6)
