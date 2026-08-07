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
7. [学习路线推荐](#7-学习路线推荐)
8. [核心概念速查](#8-核心概念速查)

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

**spring-core 的边界：什么不属于它**——理解边界与理解能力同样重要，面试问"core 里有什么"其实是问"什么在 core、什么不在"：

| 常见误以为在 core | 实际归属 | 判定理由 |
|------------------|---------|---------|
| Bean 容器（BeanFactory） | spring-beans | 容器是"管理对象"的运行时，不是能力库 |
| ApplicationContext | spring-context | 企业级容器（事件/国际化/环境装配） |
| AOP 代理实现 | spring-aop | 织入机制独立成模块 |
| MVC 注解（@GetMapping 等） | spring-web | web 层语义注解归 web 模块 |
| 事务（@Transactional） | spring-tx | 事务是 AOP + 资源管理的产物 |
| 组件扫描器 | spring-context（indexer 生成索引） | 扫描需要类路径遍历能力 |

> 🎯 **记忆锚点**："core 里只有两类东西——**注解与接口**（数据）和**工具与引擎**（能力），没有运行时状态"；凡是需要"启动、管理、织入"的东西都在上层模块。

**七大能力域的一句话速记**（面试开头 30 秒必答结构）：

| 能力域 | 一句话 | 对应文档 |
|--------|--------|---------|
| 注解体系 | 组件注解是"数据"，扫描是别人的活 | [02](02-注解体系速查.md) |
| 工具类 | 容器内部的公共代码库，零依赖可用 | [03](03-核心工具类速查.md) |
| 类型系统 | 把擦除的泛型在声明处找回来 | [04](04-类型系统与类型转换速查.md) |
| 类型转换 | 字符串到万物的 SPI 门面 | [04](04-类型系统与类型转换速查.md) |
| 资源抽象 | 一切可读之物，统一成 getInputStream | [05](05-资源抽象速查.md) |
| SpEL | 寄生在 Spring 语义上的受限表达式 | [06](06-SpEL表达式速查.md) |
| 重试（7.0） | 瞬时故障的轻量重试引擎 | [07](07-重试与弹性速查.md) |

> 💡 这七个域也正好对应"Spring 为什么能零依赖起步"的答案：把通用能力全部下沉到最底层模块，上层各司其职——面试讲模块化设计时直接引用这张表。

## 2. 版本现状（2026-08）

| 版本 | 说明 |
|------|------|
| Spring Framework 7.0（2025-11） | 当前主线（Boot 4.0 配套） |
| Spring Framework 7.1（2026-05） | 最新维护线（Boot 4.1 配套） |
| Spring Framework 6.2.x | 存量主线（Boot 3.x 配套） |

**版本选型建议**：存量 Boot 3.x 项目不必强行升级——6.2.x 维护线仍在；新项目直接 7.x（Boot 4.x）；评估升级时以"依赖树里 core 版本唯一"为底线，混合版本（6.2 + 7.0 共存）是 NoSuchMethodError 的头号来源。版本号语义：7.0.x 的 x 为补丁线（如 7.0.8 引入 SpEL 操作上限），7.1.x 为特性线，升级补丁线风险最低。

**7.x core 关键变化：**

- **`spring-jcl` 模块移除**：日志桥接改为直接依赖 Apache Commons Logging 1.3.0——对应用几乎透明（传递依赖），但显式声明过 `spring-jcl` 的项目需移除；
- **`ListenableFuture` 移除**：`org.springframework.util.concurrent` 中的 ListenableFuture 删除，统一用 `CompletableFuture`；
- **Spring Retry 并入 core**：新包 `org.springframework.core.retry`（`RetryTemplate`/`RetryPolicy`/BackOff 策略），配套注解在 spring-context（`@Retryable`/`@ConcurrencyLimit`/`@EnableResilientMethods`）——见 [07-重试与弹性速查](07-重试与弹性速查.md)；
- **SpEL 增强**：`Optional` 空安全导航与 Elvis 运算符自动解包；**单次求值操作上限 10,000 次**（7.0.8 起，可配 `spring.expression.maxOperations`）；
- **空安全注解迁移**：Spring 自带 JSR 305 语义空注解废弃，代码库迁移到 **JSpecify**（`org.jspecify.annotations`）——第三方代码基于旧注解的编译警告增多；
- **AOT 元数据统一格式**：资源提示由正则改 glob（`"/files/*.ext"` 不再匹配嵌套路径，用 `"/files/**/*.ext"`）；注册类型反射提示自动覆盖方法/构造器/字段；
- **类文件读取**：Java 24+ 使用 JEP 484 Class-File API（`ClassFileMetadataReader`）替代精简 ASM 实现扫描组件。

> ⚠️ **要点**：core 的 7.0 变化几乎不破坏业务代码——唯一注意项是显式依赖 spring-jcl 的项目（移除）与 AOT 提示写法（glob）；迁移到 JDK 25 LTS 可获得 ClassFileMetadataReader 带来的扫描加速。

**6.2 → 7.0 迁移核对清单**（按影响面从高到低）：

| 检查项 | 具体动作 | 影响面 |
|--------|---------|--------|
| 显式 spring-jcl 依赖 | 从 pom/gradle 移除 | 编译期可能报传递依赖冲突 |
| ListenableFuture 用法 | 替换为 `CompletableFuture`（如 `settableFuture.complete(x)` → `completedFuture(x)`） | 编译报错即发现 |
| 独立 spring-retry 依赖 | 移除 `spring-retry` 坐标；`maxAttempts` → `maxRetries`；包名改 `org.springframework.core.retry` | 编译期 |
| `org.springframework.lang.*` 空注解 | 无需立即改（已废弃但可用）；新代码用 JSpecify | 仅编译警告 |
| AOT 资源提示 | 正则改 glob（`"/files/*.ext"` → `"/files/**/*.ext"`） | 仅原生镜像场景 |
| SpEL 超长表达式 | 确认单次求值 < 10,000 操作，否则配置 `spring.expression.maxOperations` | 运行时异常 |

> 💡 **迁移顺序建议**：先编译（暴露移除项）→ 再跑测试（暴露运行时行为差异）→ 最后做 AOT 构建验证（仅原生镜像项目）；core 层的迁移工作量通常只占整体升级的 10% 以内。

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

**能力被谁消费**——面试常问"core 的能力在上层是怎么落地的"，三张最常见的消费链路：

| 上层功能 | core 能力 | 消费时刻 |
|---------|----------|---------|
| @Autowired 泛型匹配 | ResolvableType | 容器 refresh 时解析注入点 |
| @Value("${...}") 注入 | PropertyResolver + ConversionService | 属性占位符解析器 |
| @Component 扫描 | stereotype 注解 + MergedAnnotations | 扫描器回调（beans/context） |
| 缓存注解 key 解析 | SpEL | 缓存拦截器每次调用 |
| @ConfigurationProperties 绑定 | TypeDescriptor + 转换器 | Boot 绑定器 |
| 静态/模板资源 | Resource 抽象 | ResourceHttpRequestHandler |

> 💡 观察规律：**core 的能力几乎都是"被回调"而非"主动运行"**——容器在特定阶段调用它们；这也解释了为什么单独用 core 什么都不会发生，必须配合 beans/context。

## 4. 与深度体系的映射

| 速查文档 | 深度体系模块 |
|---------|-------------|
| 02-注解体系速查 | [Spring框架核心-02-BeanDefinition与三种配置方式](../../../Spring框架核心/02-BeanDefinition与三种配置方式.md) |
| 04-类型系统与类型转换速查 | [Spring框架核心-03-依赖注入详解与自动装配](../../../Spring框架核心/03-依赖注入详解与自动装配.md) |
| 05-资源抽象速查 | [Spring框架核心-09-资源国际化与测试](../../../Spring框架核心/09-资源国际化与测试.md) |
| 06-SpEL 表达式速查 | [Spring框架核心-03（@Value 与 SpEL）](../../../Spring框架核心/03-依赖注入详解与自动装配.md) |
| 08-集成地图与常见问题 | [Spring Core-00-源码专题总览](../../../Spring Core/00-SpringCore专题总览.md) |

> 💡 本系列定位"查得快"，源码专题定位"读得深"——容器源码、BeanDefinition 解析机制见 [Spring Core-01~05](../../../Spring Core/00-SpringCore专题总览.md)；core 的能力大多"藏在背后"被 beans/context 消费，深挖入口在 Spring-Beans/Context 两系列。

**两条衔接路径的配合方式**：

| 场景 | 用什么 | 为什么 |
|------|--------|--------|
| 写代码查 API 用法 | 本系列速查（02-07 篇） | 表格直达签名与示例 |
| 面试讲原理 | 源码专题（Spring Core-01~05） | 需要类图与调用链 |
| 排查线上问题 | 先速查定位能力归属 → 再进源码专题看实现 | 避免在错误模块浪费时间 |
| 系统学习容器 | Spring-Beans/Context 系列 | core 能力是它们的零件 |

> 💡 **典型阅读路径**：遇到 `@Autowired` 泛型匹配问题 → 04 篇确认 ResolvableType 用法 → Beans-06 看注入裁决链 → 源码专题看 DefaultListableBeanFactory 实现——四层由浅入深。

## 5. 快速上手 3 步

**① 引入依赖**（通常无需显式——任何 Spring 模块都会传递引入）：

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-core</artifactId>
    <version>7.0.6</version>
</dependency>
```

> 💡 **版本管理**：生产项目不要手写版本号——通过 `spring-framework-bom`（或 Boot 4 的 `spring-boot-dependencies`）统一管理，避免"core 7.0、beans 6.2"混用；单测或工具类项目只需引入 core 时，BOM 同样适用。

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

### 5.1 快速上手的常见失败表现

| 现象 | 原因 | 修复 |
|------|------|------|
| 类找不到（NoClassDefFoundError） | 版本混用：core 7.x 搭配 beans 6.x | 统一 BOM（`spring-framework-bom`）管理版本 |
| 方法不存在（NoSuchMethodError） | 编译 7.0 运行在 6.2 容器（或反之） | 升级时全量替换依赖，禁止部分升级 |
| Assert 抛异常但业务需求是"用户友好报错" | 用错了断言场景 | 对外接口用 Jakarta Validation；Assert 只做内部契约 |
| 重试一直失败打爆下游 | 未配 BackOff 或未排除确定错误 | 配置退避 + 异常分类（见 [07-重试与弹性速查](07-重试与弹性速查.md)） |

> ⚠️ **版本一致性是 core 的第一坑**：spring-core 被所有模块传递依赖，一处显式声明旧版本即可引发全链路 NoSuchMethodError——生产排查此类问题先 `mvn dependency:tree` 看 core 版本。

### 5.2 面试追问点

- **"core 和 JDK 的关系"**：core 编译期零依赖、纯 JDK——这意味着它可以脱离 Spring 单独当"工具库"用（如只引入 spring-core 用 StringUtils/ResolvableType）；
- **"为什么注解定义在 core 而不是 context"**：因为 beans/context/web 都要消费注解，放 core 才能避免反向依赖；同理 `Environment` 接口在 core、实现类在 context（[01-模块清单](01-模块清单.md) 第 2 节）；
- **"core 版本升级为什么很少破坏业务"**：core 是纯能力库、无 SPI 变化面窄，7.0 的破坏项集中在"删除"而非"修改语义"；
- **"零依赖如何做到"**：日志走 commons-logging 门面（7.0 直接依赖）、Kotlin/GraalVM 支持全部 optional——编译期 classpath 只需 JDK。

### 5.3 生产实践要点

| 实践 | 做法 | 收益 |
|------|------|------|
| 统一版本 | BOM 管理，锁定唯一 core 版本 | 杜绝 NoSuchMethodError |
| 断言分层 | 内部契约用 Assert、外部输入用 Validation | 异常语义清晰 |
| 泛型注入显式化 | 注入点字段/参数尽量写全泛型 | ResolvableType 匹配可靠 |
| 资源读取统一 | 一律 `getInputStream()`，禁止 getFile() | jar 打包后不炸 |
| SpEL 输入隔离 | 用户输入只走 SimpleEvaluationContext | 防表达式注入 |
| 重试带退避 | 只重试瞬时异常 + 指数退避 + 总超时 | 防打爆下游 |
| 升级顺序 | 编译 → 测试 → AOT 验证 三步走 | 每步失败面可控 |

> 🎯 **收尾要点**：core 的学习目标不是"记住 API"，而是建立"能力归属感"——遇到问题先问"这是 core 的能力吗、它在上层被谁消费、什么条件下不生效"，这套思维正是排查框架问题的通用方法。

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

**每篇的使用姿势**：02-07 篇都是"表格 + 示例 + 边界"三段式——第一遍只扫表格建立地图，第二遍带着问题精读示例代码，第三遍把"⚠️ 边界"小节抄进团队 wiki；08 篇的坑表建议贴在项目 README 的疑难杂症区。若时间只够读两篇，优先 04（类型系统，面试最常考）与 08（常见问题，最能救急）。

## 7. 学习路线推荐

| 路线 | 适用 | 顺序 |
|------|------|------|
| 入门速查 | 认识 Spring 地基 | 00 总览 → 02 注解 → 03 工具类 → 06 SpEL → 01 模块清单 |
| 项目实践 | 使用核心能力 | 04 类型系统 → 05 资源 → 06 SpEL → 07 重试 → 08 常见问题 |
| 面试冲刺 | 底层原理考点 | 04 ResolvableType → 06 SpEL 安全 → 07 弹性 → [Spring Core-00-源码专题](../../../Spring Core/00-SpringCore专题总览.md) |

## 8. 核心概念速查

| 术语 | 一句话解释 |
|------|-----------|
| spring-core | 零依赖地基模块（注解/工具/类型/SpEL/资源/retry） |
| @Component 族 | 组件注解（定义在此，扫描在 beans/context） |
| ResolvableType | 运行时泛型解析（泛型注入的底层） |
| ConversionService | 类型转换门面（Converter 族） |
| Resource | 统一资源抽象（classpath:/file:/url:） |
| SpEL | 表达式语言（@Value/#{}、缓存键） |
| RetryTemplate（7.0） | 编程式重试引擎（core.retry 包） |
| Environment 接口 | 属性解析契约（实现装配在 context） |

---

**下一模块**：[01-模块清单](01-模块清单.md)　**返回总览**：本页

**【参考来源】**：[Spring Framework 7.0 Release Notes（GitHub Wiki）](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-7.0-Release-Notes)、[Spring Framework 7.0 正式发布解析（掘金）](https://juejin.cn/post/7573592127298682930)、[Spring 6→7 Migration Guide（dev.to）](https://dev.to/ankurm/spring-framework-6-to-7-migration-guide-breaking-changes-deprecated-apis-and-upgrade-checklist-3bf6)
