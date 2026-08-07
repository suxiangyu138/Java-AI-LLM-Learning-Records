# 00 Spring Context 组件总览

> 组件卡片：spring-context 是什么、版本现状、能做什么、与深度体系如何衔接——本系列是"组件汇总"速查型文档，深挖见 [Spring框架核心-01~09](../../../Spring框架核心/00-Spring框架核心知识体系总览.md)

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

**spring-context 是"企业级容器"ApplicationContext 的家**——在 spring-beans 的工厂地基上，补齐了配置类处理（@Configuration/@ComponentScan/@Import）、refresh() 生命周期、事件驱动、国际化 MessageSource、Environment 属性管理五大能力；Boot 的自动配置也跑在它的 refresh() 流程里。

```text
核心心智模型：
  工厂能力（spring-beans）          企业能力（spring-context）
  BeanFactory               →    ApplicationContext
  getBean/注册表                   + 预加载全部单例（refresh）
                                   + @Configuration 配置类处理
                                   + 事件发布/监听（ApplicationEventPublisher）
                                   + 国际化（MessageSource）
                                   + 属性环境（Environment/PropertySource）
                                   + 资源模式加载（ResourcePatternResolver）

  一句话：工厂解决"Bean 怎么创建"，上下文解决"容器怎么启动、怎么与外界交互"。
```

| 维度 | 说明 |
|------|------|
| 所属 | Spring Framework 底层核心模块（spring-context） |
| 版本线 | 随 Spring Framework 7.x（2025-11 起） |
| 语言要求 | Java 17+（官方推荐 JDK 25 LTS） |
| 定位 | 企业级容器与上下文服务（一切 Spring 应用进程的启动器） |

## 2. 版本现状（2026-08）

| 版本 | 说明 |
|------|------|
| Spring Framework 7.0（2025-11） | 当前主线（Boot 4.0 配套） |
| Spring Framework 7.1（2026-05） | 最新维护线（Boot 4.1 配套） |
| Spring Framework 6.2.x | 存量主线（Boot 3.x 配套） |

**7.x context 关键变化：**

- **`javax.annotation` / `javax.inject` 注解彻底移除支持**：`@javax.annotation.Resource`、`@javax.annotation.PostConstruct`、`@javax.inject.Inject` 不再被识别——**必须迁移到 `jakarta.annotation` / `jakarta.inject`**（升级 7.0 的必改项）；
- **全局代理类型默认 CGLIB**（与 Boot 一致）：`ProxyConfig` Bean（名 `AutoProxyUtils.DEFAULT_PROXY_CONFIG_BEAN_NAME`）设定上下文级默认，@Async 等一切代理处理器统一生效；按 Bean 用 `@Proxyable(INTERFACES)` 退出到 JDK 代理；
- **弹性能力内建**：`@Retryable` 与 `@ConcurrencyLimit`（并发限流）注解加入 context，`@Configuration` 类上 `@EnableResilientMethods` 开启；`@Retryable` 对响应式返回类型自动适配 Reactor retry（基础 API 在 spring-core，见 [Spring-Core-07](../Spring‑Core/07-重试与弹性速查.md)）；
- **测试上下文暂停**：`spring.test.context.cache.pause` 属性（`always`/`never`，7.0.3+ 起默认"仅切换上下文时才暂停"），空闲测试上下文可暂停后台进程；
- **`<lang:*` XML 命名空间、BeanShell、Kotlin 脚本模板废弃**（7.0 起不再推荐）。

> ⚠️ **要点**：7.0 的 context 变化是"清理 + 内建"——javax→jakarta 是硬性迁移项；其余（CGLIB 默认、弹性注解）是新能力叠加，不影响存量配置类代码。

### 2.1 版本支持策略与升级窗口

Spring Framework 官方对每个大版本提供约 12 个月的 OSS（开源免费）支持期，随后转入商业支持或 EOL——"还能用"不等于"还在修"：

| 版本线 | 状态（2026-08） | 安全补丁 | 建议 |
|--------|----------------|---------|------|
| 7.1.x | 最新维护线（Boot 4.1 配套） | ✅ 持续 | 新项目与已有项目优先对齐 |
| 7.0.x | 当前主线（Boot 4.0 配套） | ✅ 持续 | 已上线的 7.0 项目可跟进 7.1 补丁 |
| 6.2.x | 存量主线（Boot 3.x 配套） | ⚠️ 窗口收窄 | 有计划迁移者尽早规划 |
| 6.1.x 及更早 | 已 EOL | ❌ | 存在已知 CVE 风险，尽快升级 |

**升级 6.x → 7.x 的三类工作（按风险排序）**：

| 类型 | 具体内容 | 风险表现 |
|------|---------|---------|
| 强制迁移 | `javax.annotation.*` / `javax.inject.*` → `jakarta.*` 全套替换 | 不换则注解被静默忽略，`@Resource` 注入直接为 null、`@PostConstruct` 不再执行——**编译不报错、运行才炸** |
| 行为变化 | 代理默认改 CGLIB；`spring-jcl` 移除（日志走 core 的适配）；测试上下文缓存暂停策略调整 | 强依赖 JDK 接口代理的存量代码行为改变 |
| 能力叠加 | `@EnableResilientMethods`、`@Retryable`、`@ConcurrencyLimit` | 低风险，不开启即无影响 |

> 💡 升级节奏建议：先跑依赖树（`mvn dependency:tree` / Gradle `dependencies`）定位 javax 依赖来源（往往藏在第三方 jar），全局替换 import 后**优先回归测试启动与注入**——javax 注解失效的最大特征是"启动成功但 Bean 没注入"。

## 3. 能力地图

| 能力域 | 能力点 | 对应注解/API |
|--------|--------|-------------|
| 配置类 | 声明式配置 | `@Configuration`、`@Bean`、`@ComponentScan` |
| 装配编排 | 导入与组合 | `@Import`（Selector/BeanRegistrar）、`@EnableXxx` |
| 条件装配 | 环境/条件开关 | `@Profile`、`@Conditional` |
| 容器启动 | refresh() 生命周期 | `ConfigurableApplicationContext`、`ApplicationListener` |
| 事件驱动 | 发布/监听 | `ApplicationEventPublisher`、`@EventListener`、`@TransactionalEventListener` |
| 国际化 | 消息解析 | `MessageSource`、`ResourceBundleMessageSource` |
| 属性管理 | Environment 与占位符 | `Environment`、`@PropertySource`、`PropertySourcesPlaceholderConfigurer` |
| 弹性（7.0） | 重试/限流 | `@EnableResilientMethods`、`@Retryable`、`@ConcurrencyLimit` |
| 异步 | 线程池集成 | `@Async`、`TaskExecutor` |
| 资源 | 模式加载 | `ResourcePatternResolver`、`classpath*:` |

### 3.1 能力归属边界（哪些"不在" context）

| 能力 | 归属 | 原因 |
|------|------|------|
| 事务注解 `@Transactional` | spring-tx | context 只提供 `@EnableTransactionManagement` 开关与事件钩子 |
| Web MVC（控制器/拦截器） | spring-webmvc | context 只提供 `WebApplicationContext` 与 request/session 作用域注册 |
| 数据访问（JdbcTemplate） | spring-jdbc | context 不涉及任何数据源 |
| AOP 切点表达式 | spring-aop / aspectjweaver | context 只负责 `@EnableAspectJAutoProxy` 装配 |
| 配置绑定（`@ConfigurationProperties`） | Spring Boot | context 的 `@Value` 只做占位符，不做类型化绑定 |
| HTTP 客户端 | spring-web | context 无网络能力 |
| Bean 定义与生命周期 | spring-beans | context 在其上做"预加载 + 编排" |

> 🎯 **判断口诀**："**接口在 core，工厂在 beans，编排在 context，专项能力在各自模块**"——面试被问"XXX 在哪个模块"，先按这个口诀定位，再补充"开关类注解（@EnableXxx）在 context"这个特例。

## 4. 与深度体系的映射

| 速查文档 | 深度体系模块 |
|---------|-------------|
| 02-ApplicationContext 体系速查 | [Spring框架核心-01-容器体系](../../../Spring框架核心/01-容器体系-BeanFactory与ApplicationContext.md) |
| 03-配置类与扫描机制速查 | [Spring Core-06-@Configuration与AOP代理机制](../../../Spring Core/06-@Configuration与AOP代理机制.md) |
| 04-Environment 与属性管理速查 | [Spring框架核心-08-配置管理-Environment与配置属性](../../../Spring框架核心/08-配置管理-Environment与配置属性.md) |
| 05-事件驱动机制速查 | [Spring框架核心-07-事件驱动机制](../../../Spring框架核心/07-事件驱动机制.md) / [Spring生态深度剖析-06-事件驱动模型](../../../Spring生态深度剖析/06-事件驱动模型与ApplicationContext内部机制.md) |
| 06-国际化与资源访问速查 | [Spring框架核心-09-资源国际化与测试](../../../Spring框架核心/09-资源国际化与测试.md) |
| 07-集成地图与常见问题 | [Spring生态深度剖析-02-Boot启动流程与自动配置内核](../../../Spring生态深度剖析/02-SpringBoot启动流程与自动配置内核.md) |

> 💡 本系列定位"查得快"，深度体系定位"学得透"——容器启动全流程、refresh() 十二步、事件内部机制的源码深挖见 [Spring生态深度剖析-06](../../../Spring生态深度剖析/06-事件驱动模型与ApplicationContext内部机制.md)。

### 4.1 溯源示范：一个真实问题如何从速查走到源码

以生产高频问题"**@TransactionalEventListener 没触发**"为例，演示速查体系与深度体系的配合：

| 步骤 | 动作 | 所在文档 |
|------|------|---------|
| ① 现象定位 | 确认"无事务时不触发、静默无日志"的语义边界 | 本系列 [05-事件驱动机制速查](05-事件驱动机制速查.md) 第 3 节 |
| ② 语义确认 | 区分"没触发"三种原因：无事务 / 事务回滚 / 监听器没注册 | 05 第 3.1 节（边界表格） |
| ③ 机制理解 | 底层是 `TransactionSynchronization` 回调 + `TransactionalEventListenerFactory` | [Spring框架核心-07-事件驱动机制](../../../Spring框架核心/07-事件驱动机制.md) |
| ④ 源码深挖 | refresh 第 8 步多播器 → 事件注册 → 事务同步注册点逐行 | [Spring生态深度剖析-06](../../../Spring生态深度剖析/06-事件驱动模型与ApplicationContext内部机制.md) |
| ⑤ 方案落地 | `fallbackExecution=true` 或改发布位置（事务内发布） | 05 第 3 节代码示例 |

> 🎯 **使用心法**：速查文档是"**症状 → 语义**"的索引，深度体系是"**语义 → 机制**"的推导——排查问题时先到速查确认"它本该怎样"，再进深度体系弄懂"它为什么这样"，最后动手改代码。**先语义后机制，避免对着源码猜行为**。

### 4.2 两个体系的取舍建议

| 场景 | 用速查 | 用深度体系 |
|------|:------:|:----------:|
| 日常开发查 API/语义 | ✅ | |
| 排查启动/事件/配置故障 | ✅ 先定位 | ✅ 后追根因 |
| 面试复习容器考点 | ✅ 速记 | ✅ 源码细节 |
| 写方案/评审（讲原理） | | ✅ |
| 阅读 Spring 源码 | | ✅ |

> 💡 **最佳节奏**：**速查建立地图 → 深度体系建立深度 → 回到速查做索引**——三轮循环后，context 的知识从"零散知识点"固化为"可检索的体系"；这也是本系列 8 篇速查 + 深度体系 6 篇的分工初衷。

## 5. 快速上手 3 步

**① 引入依赖**：

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-context</artifactId>
    <version>7.0.6</version>   <!-- 传递引入 spring-beans + spring-core -->
</dependency>
```

**② 构建上下文（三选一）**：

```java
// A. 注解驱动（现代默认）
ApplicationContext ctx = new AnnotationConfigApplicationContext(AppConfig.class);

// B. 编程式（BeanRegistrar 时代的推荐起点）
GenericApplicationContext ctx = new GenericApplicationContext();
ctx.registerBean("service", MyService.class);
ctx.refresh();

// C. XML（存量）
ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("app.xml");
```

**③ 使用**：

```java
MyService service = ctx.getBean(MyService.class);
ctx.publishEvent(new OrderCreatedEvent(...));   // 事件发布即插即用
```

### 5.1 最小完整可运行示例（无 Boot）

```xml
<!-- pom.xml：只需这一个依赖，transitive 引入 spring-beans + spring-core -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-context</artifactId>
    <version>7.0.6</version>
</dependency>
<dependency>   <!-- 可选但强烈建议：日志实现，否则运行期日志全部丢失 -->
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-slf4j2-impl</artifactId>
    <version>2.24.3</version>
</dependency>
```

```java
// AppConfig.java —— 配置类 + 组件扫描 + 一个 @Bean
@Configuration
@ComponentScan("com.example")
public class AppConfig {
    @Bean
    public OrderService orderService() {        // 与扫描出的 Bean 同存不冲突
        return new OrderService();
    }
}

// Main.java —— 入口：构建 → 使用 → 优雅关闭
public class Main {
    public static void main(String[] args) {
        try (AnnotationConfigApplicationContext ctx =
                     new AnnotationConfigApplicationContext(AppConfig.class)) {
            OrderService svc = ctx.getBean(OrderService.class);
            svc.create(42L);
            ctx.publishEvent(new OrderCreatedEvent(42L));   // 事件即插即用
        }   // try-with-resources 关闭 = close() → 触发 @PreDestroy/destroy
    }
}
```

> 💡 无 Boot 场景最容易踩的两个坑：① **缺日志实现**——spring-jcl 找不到绑定时启动仅打一行警告，看似无害，但运行期监听器异常、懒加载失败全部无日志可查；② **忘了关闭**——main 结束直接退 JVM 不会触发 destroy 回调，`@PreDestroy` 清理（连接池关闭等）被跳过，非 Web 常驻进程务必 `registerShutdownHook()` 或 try-with-resources。

### 5.2 三种构建方式的取舍

| 方式 | 是否自动 refresh | 适用场景 | 代价 |
|------|----------------|---------|------|
| `AnnotationConfigApplicationContext` | ✅ 构造即 refresh | 常规注解应用（首选） | 无法在构造与启动之间插入自定义步骤 |
| `GenericApplicationContext` | ❌ 必须手动 | 测试、动态注册、工具型容器 | 漏调 refresh 会拿到空容器 |
| `ClassPathXmlApplicationContext` | ✅ 构造即 refresh | 存量 XML 系统 | 无注解配置能力，与 7.0 新特性脱节 |

**什么时候该用 GenericApplicationContext 而不是注解上下文**：

```java
// 测试中动态造容器（不需要扫描与配置处理）
GenericApplicationContext ctx = new GenericApplicationContext();
ctx.registerBean("svc", OrderService.class, () -> new OrderService("test"));
ctx.refresh();          // 忘写这行 → getBean 抛 IllegalStateException
```

> 🎯 **面试追问**："AnnotationConfigApplicationContext 构造器做了什么？"——它内部是 `GenericApplicationContext + AnnotatedBeanDefinitionReader + ClassPathBeanDefinitionScanner` 的组合：构造即注册容器自身与内建后置处理器，随后 refresh() 走完整十二步；所以它本质是"Generic 的快捷封装"，不是另一套实现。

### 5.3 常见入门报错速查

| 报错 | 根因 | 秒修 |
|------|------|------|
| `NoSuchBeanDefinitionException` | getBean 的类型没注册 | 检查扫描包路径 / 配置类是否被加载 |
| `BeanDefinitionStoreException: Failed to parse configuration class` | 配置类里语法/导入错误 | 看 Caused by 链最底层 |
| `BeanCreationException: Error creating bean with name 'xxx'` | 构造器/初始化抛错 | 找 Caused by 的业务异常 |
| `IllegalStateException: BeanFactory not initialized` | GenericApplicationContext 忘 refresh | 补 `ctx.refresh()` |
| `NoUniqueBeanDefinitionException` | 同类型多个 Bean | `@Primary` 或 `@Qualifier` 指定 |
| 中文乱码（配置文件） | 文件非 UTF-8 | 统一 UTF-8 保存 + `setDefaultEncoding` |
| 启动只有一行 WARN 无其他日志 | 缺日志实现绑定 | 加 log4j2 / logback 依赖 |

> 💡 **排查顺序铁律**：先看**最底层 Caused by**（根因），再逐层向上还原触发路径；启动失败 80% 是"类没注册 / 依赖缺失 / 条件没满足"三类，用 `getBeanDefinitionNames()` 打印一遍即可快速定位。

### 5.4 无 Boot 与 Boot 语境的关键差异

| 能力 | 纯 spring-context | Spring Boot |
|------|------------------|-------------|
| 配置来源 | 代码 / @PropertySource / 系统属性 / 环境变量 | + application.yml 自动发现、宽松绑定 |
| 自动配置 | 无（手动 @Import） | @EnableAutoConfiguration 规模化 |
| 内嵌服务器 | 无 | 第 9 步 onRefresh 创建 |
| 日志体系 | 需自行绑定实现 | starter 自带 |
| 配置类处理 | 完全一致（Boot 复用 context 管线） | 相同管线 + DeferredImportSelector 追加 |

> 💡 **心智模型**：Boot 不是另一套容器，而是"**context + 默认配置 + 自动装配 + 服务器**"的装配层——理解这点后，纯 context 项目与 Boot 项目排障思路完全通用：先定位"它在 refresh 哪一步"，再判断该步骤行为是否被 Boot 定制过。同理，面试答"Boot 启动流程"时把 refresh 十二步作为主干、Boot 的扩展（自动配置、内嵌服务器）作为分支来组织，逻辑最清晰、最不易漏点。

## 6. 速查导航

| 文档 | 内容 |
|------|------|
| [01-模块清单](01-模块清单.md) | spring-context artifact 与包结构、依赖边界 |
| [02-ApplicationContext 体系速查](02-ApplicationContext体系速查.md) | 接口层次、refresh() 流程、常见实现 |
| [03-配置类与扫描机制速查](03-配置类与扫描机制速查.md) | @Configuration/@ComponentScan/@Import/@EnableXxx/@Proxyable |
| [04-Environment 与属性管理速查](04-Environment与属性管理速查.md) | PropertySource、Profile、占位符解析 |
| [05-事件驱动机制速查](05-事件驱动机制速查.md) | 事件发布/监听/事务事件/异步事件 |
| [06-国际化与资源访问速查](06-国际化与资源访问速查.md) | MessageSource、Resource 加载 |
| [07-集成地图与常见问题](07-集成地图与常见问题.md) | 与 Beans/Core/AOP/TX/Web 联动 + 高频坑 |

## 7. 学习路线推荐

| 路线 | 适用 | 顺序 |
|------|------|------|
| 入门速查 | 理解容器怎么跑起来 | 00 总览 → 02 ApplicationContext → 03 配置类 → 05 事件 → 04 Environment |
| 项目实践 | 排查启动/事件/配置问题 | 03 配置类 → 04 Environment → 05 事件 → 07 常见问题 → 深度体系（框架核心-07/08） |
| 面试冲刺 | 容器与事件考点 | 02 refresh 十二步 → 03 @EnableXxx 原理 → 05 事件 vs MQ → [Spring生态深度剖析-06](../../../Spring生态深度剖析/06-事件驱动模型与ApplicationContext内部机制.md) |

## 8. 核心概念速查

| 术语 | 一句话解释 |
|------|-----------|
| ApplicationContext | 企业级容器（refresh 启动、事件、国际化、属性） |
| refresh() | 容器启动十二步（配置处理→单例实例化→完成事件） |
| @Configuration | 配置类（Full 模式 CGLIB 代理保单例） |
| @ComponentScan | 组件扫描入口（basePackages 决定范围） |
| @Import | 导入类/Selector/BeanRegistrar 的装配机制 |
| Environment | 属性解析链（PropertySource 排序决定优先级） |
| ApplicationEvent | 进程内发布-订阅解耦事件 |
| MessageSource | 国际化消息解析 |
| @TransactionalEventListener | 事务提交后才触发的事件监听 |

---

**下一模块**：[01-模块清单](01-模块清单.md)　**返回总览**：本页

**【参考来源】**：[Spring Framework 7.0 Release Notes（GitHub Wiki）](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-7.0-Release-Notes)、[Spring Framework 7.0 正式发布解析（掘金）](https://juejin.cn/post/7573592127298682930)、[Spring 6→7 Migration Guide（dev.to）](https://dev.to/ankurm/spring-framework-6-to-7-migration-guide-breaking-changes-deprecated-apis-and-upgrade-checklist-3bf6)
