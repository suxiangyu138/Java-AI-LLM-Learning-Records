# 00 Spring Beans 组件总览

> 组件卡片：spring-beans 是什么、版本现状、能做什么、与深度体系如何衔接——本系列是"组件汇总"速查型文档，深挖见 [Spring框架核心-01~04](../../../Spring框架核心/00-Spring框架核心知识体系总览.md)

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

**spring-beans 是 Spring IoC 容器的心脏**——提供 Bean 的定义（BeanDefinition）、注册（BeanDefinitionRegistry）、工厂（BeanFactory）、装配（Autowired/类型转换）与生命周期回调（BeanPostProcessor）全部机制，是 @Autowired、@Bean、@Component 以及一切"容器管理对象"的底层地基。

```text
核心心智模型：
  BeanDefinition（图纸：如何创建这个对象？）
      ↓ 注册
  BeanDefinitionRegistry（图纸仓库）
      ↓ 实例化流程（getBean）
  BeanFactory（工厂：创建 + 初始化 + 缓存单例）
      ↑
  BeanPostProcessor（在每个 Bean 创建前后插手）
      ↑
  ApplicationContext（企业级容器 = 工厂 + 更多能力）

  一句话：图纸 → 仓库 → 工厂 → 后处理器，四层协作管理对象生命周期。
```

| 维度 | 说明 |
|------|------|
| 所属 | Spring Framework 底层核心模块（spring-beans） |
| 版本线 | 随 Spring Framework 7.x（2025-11 起） |
| 语言要求 | Java 17+ |
| 定位 | IoC 容器与 Bean 生命周期管理（一切 Spring 能力的地基） |

## 2. 版本现状（2026-08）

| 版本 | 说明 |
|------|------|
| Spring Framework 7.0（2025-11） | 当前主线（Boot 4.0 配套） |
| Spring Framework 7.1（2026-05） | 最新维护线（Boot 4.1 配套） |
| Spring Framework 6.2.x | 存量主线（Boot 3.4/3.5 配套） |

**7.x beans 关键变化：**

- **`BeanRegistrar`（新接口，7.0）**：程序化注册 Bean 的一等公民——函数式接口 `register(BeanRegistry, Environment)`，配合 `@Import` 使用，支持 if/for/Environment 条件注册，**AOT / GraalVM 原生镜像友好**；取代了繁琐的 `BeanDefinitionRegistryPostProcessor` 手写 BeanDefinition 模式，Kotlin 侧有 `BeanRegistrarDsl`（2025-03 的 7.0.0-M3 引入）；
- **`@Fallback`（新注解，7.0）**：与 `@Primary` 相对的兜底候选——标注后仅当没有其他候选 Bean 时才被选中，程序化注册时对应 `fallback` 标志位；
- **`@Proxyable`（新注解，7.0）**：按 Bean 粒度指定代理类型（如 `@Proxyable(proxyTargetClass = true)`），与 Boot 的全局 CGLIB 默认互补；
- **注册选项扩展**：`registerBean` 规格支持 `backgroundInit`（后台初始化）、`infrastructure`、`order` 等细粒度控制；
- **BeanDefinition API 持续演进**：7.0.x 仍在使用 `RootBeanDefinition` / `GenericBeanDefinition` / `SimpleBeanDefinitionRegistry`，但推荐新代码走 `registerBean` 高 API 而非手工构造。

> ⚠️ **要点**：7.0 的 beans 变化是"注册方式升级"而非"容器机制重写"——`BeanRegistrar` 是新代码的首选注册方式，但 `@Component` 扫描、`@Bean`、XML 三种传统方式完全兼容；升级主要影响"手写 BeanDefinition + BeanDefinitionRegistryPostProcessor"的存量代码，可平滑迁移。

## 3. 能力地图

| 能力域 | 能力点 | 对应注解/API |
|--------|--------|-------------|
| Bean 定义 | 三类定义来源 | XML `<bean>`、`@Bean`、`@Component` 扫描 |
| 容器工厂 | 最底层容器 | `BeanFactory`、`DefaultListableBeanFactory` |
| 企业容器 | 完整上下文 | `ApplicationContext`（见 Spring-Context 系列） |
| 依赖注入 | 注入与自动装配 | `@Autowired`、`@Resource`、构造器注入 |
| 装配裁决 | 冲突解决 | `@Primary`、`@Fallback`（7.0）、`@Qualifier` |
| 生命周期 | 创建/初始化/销毁钩子 | `BeanPostProcessor`、`@PostConstruct`、`InitializingBean` |
| 作用域 | 单例/原型/请求/会话 | `@Scope`、`@Lazy`、`ObjectProvider` |
| 程序化注册 | 代码注册 Bean | `BeanRegistrar`（7.0）、`BeanDefinitionRegistry.registerBeanDefinition` |
| 条件装配 | 按条件启停 | `@Conditional`、`@Profile` |

## 4. 与深度体系的映射

| 速查文档 | 深度体系模块 |
|---------|-------------|
| 01-模块清单 | [Spring框架核心-01-容器体系](../../../Spring框架核心/01-容器体系-BeanFactory与ApplicationContext.md) |
| 02-核心类与接口速查 | [Spring框架核心-01](../../../Spring框架核心/01-容器体系-BeanFactory与ApplicationContext.md) |
| 03-注解与元数据速查 | [Spring框架核心-02-BeanDefinition与三种配置方式](../../../Spring框架核心/02-BeanDefinition与三种配置方式.md) |
| 04-配置属性速查 | [Spring框架核心-02](../../../Spring框架核心/02-BeanDefinition与三种配置方式.md) |
| 05-Bean生命周期与作用域速查 | [Spring框架核心-04-Bean生命周期与作用域](../../../Spring框架核心/04-Bean生命周期与作用域.md) |
| 06-依赖注入与自动装配速查 | [Spring框架核心-03-依赖注入详解与自动装配](../../../Spring框架核心/03-依赖注入详解与自动装配.md) |
| 07-集成地图与常见问题 | [Spring生态深度剖析-01-Bean生命周期与PostProcessor](../../../Spring生态深度剖析/01-Bean生命周期与PostProcessor扩展体系.md) |

> 💡 本系列定位"查得快"，深度体系定位"学得透"——速查命中后，跳转深度体系看机制与源码；循环依赖、三级缓存等深入话题见 [Spring生态深度剖析-03-循环依赖与三级缓存](../../../Spring生态深度剖析/03-循环依赖与三级缓存深度剖析.md)。

## 5. 快速上手 3 步

**① 引入依赖**（任何 Spring 项目都已间接引入，无需显式声明）：

```xml
<!-- 通常只需 spring-context，它传递依赖 spring-beans + spring-core -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-context</artifactId>
    <version>7.0.6</version>
</dependency>
```

**② 定义 Bean**（三种方式任选，见 03-注解与元数据速查）：

```java
// 方式 A：组件扫描（最常用）
@Service
public class OrderService { }

// 方式 B：@Bean 工厂方法
@Configuration
public class AppConfig {
    @Bean
    public OrderService orderService() {
        return new OrderService();
    }
}

// 方式 C：程序化注册（Spring 7 推荐新方式）
@Configuration
@Import(MyRegistrar.class)
public class AppConfig { }

class MyRegistrar implements BeanRegistrar {
    @Override
    public void register(BeanRegistry registry, Environment env) {
        registry.registerBean("orderService", OrderService.class);
    }
}
```

**③ 注入使用**：

```java
@RestController
public class OrderController {
    private final OrderService orderService;   // 构造器注入（首选）

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
}
```

## 6. 速查导航

| 文档 | 内容 |
|------|------|
| [01-模块清单](01-模块清单.md) | spring-beans artifact 与包结构、依赖边界 |
| [02-核心类与接口速查](02-核心类与接口速查.md) | BeanFactory/BeanDefinition/Registry/PostProcessor/BeanRegistrar |
| [03-注解与元数据速查](03-注解与元数据速查.md) | @Component 家族 / @Bean / @Autowired 全家 / @Conditional |
| [04-配置属性速查](04-配置属性速查.md) | XML 命名空间 + Boot 属性 + registerBean 规格 |
| [05-Bean生命周期与作用域速查](05-Bean生命周期与作用域速查.md) | 生命周期时序、Aware、五大作用域 |
| [06-依赖注入与自动装配速查](06-依赖注入与自动装配速查.md) | 注入方式、装配规则、冲突解决、循环依赖 |
| [07-集成地图与常见问题](07-集成地图与常见问题.md) | 与 Core/Context/AOP/TX/Web 联动 + 高频坑 |

---

**下一模块**：[01-模块清单](01-模块清单.md)　**返回总览**：本页

**【参考来源】**：Spring Framework 7.0.0-M3 发布说明（spring.io/blog）、[Programmatic Bean Registration 官方文档](https://docs.spring.io/spring-framework/reference/core/beans/java/programmatic-bean-registration.html)、[Baeldung: Spring BeanRegistrar 程序化注册](https://baeldung.cn/spring-beanregistrar-registration)、Spring Framework Javadoc 7.0.0-M1（docs.spring.io）
