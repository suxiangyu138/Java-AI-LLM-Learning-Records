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

---

**下一模块**：[01-模块清单](01-模块清单.md)　**返回总览**：本页

**【参考来源】**：[Spring Framework 7.0 Release Notes（GitHub Wiki）](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-7.0-Release-Notes)、[Spring Framework 7.0 正式发布解析（掘金）](https://juejin.cn/post/7573592127298682930)、[Spring 6→7 Migration Guide（dev.to）](https://dev.to/ankurm/spring-framework-6-to-7-migration-guide-breaking-changes-deprecated-apis-and-upgrade-checklist-3bf6)
