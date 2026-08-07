# BeanFactory 与 ApplicationContext 源码体系
> 容器的两层架构：BeanFactory 是最朴素的 IoC 引擎，ApplicationContext 是「引擎 + 生态」。本文件拆解容器层次、refresh() 十二步与 Spring 7.0 的容器新特性

## 目录
1. [容器层次架构](#1-容器层次架构)
2. [BeanFactory 接口体系](#2-beanfactory-接口体系)
3. [ApplicationContext 能力矩阵](#3-applicationcontext-能力矩阵)
4. [refresh() 十二步源码解析](#4-refresh-十二步源码解析)
5. [启动入口：AnnotationConfigApplicationContext](#5-启动入口annotationconfigapplicationcontext)
6. [Spring 7.0 容器新特性（2026）](#6-spring-70-容器新特性2026)
7. [常见面试追问](#7-常见面试追问)

---

## 1. 容器层次架构

```text
BeanFactory（最底层：只管 bean 的获取与实例化）
    │ 继承/聚合
ApplicationContext（在 BeanFactory 之上扩展五大能力）
    │
    ├─ 实现类：AnnotationConfigApplicationContext（注解）
    ├─          ClassPathXmlApplicationContext（XML）
    └─          FileSystemXmlApplicationContext（文件）
```

| 层 | 职责 | 典型接口 |
|------|------|------|
| 底层引擎 | bean 定义注册、获取、单例管理 | `BeanFactory` |
| 能力扩展 | 环境、事件、国际化、资源、AOP 集成 | `ApplicationContext` |
| 高级能力 | 自动注册后置处理器、单例预加载 | `AbstractApplicationContext`（模板） |

> 🎯 一句话：**BeanFactory 是「发动机」，ApplicationContext 是「整车」**——面试被问区别，先说职责拆分，再讲 refresh()。

---

## 2. BeanFactory 接口体系

### 2.1 接口方法

```java
public interface BeanFactory {
    Object getBean(String name);
    <T> T getBean(Class<T> requiredType);
    boolean containsBean(String name);
    boolean isSingleton(String name);       // 作用域判断
    boolean isPrototype(String name);
    String[] getAliases(String name);
    // Spring 6+ 泛型获取：getBean(ResolvableType)
}
```

### 2.2 分层接口（功能拆分的工程范式）

| 接口 | 扩展能力 | 实现类 |
|------|------|------|
| `ListableBeanFactory` | 批量查询（getBeansOfType） | DefaultListableBeanFactory |
| `HierarchicalBeanFactory` | 父子容器 | 同上 |
| `AutowireCapableBeanFactory` | 编程式注入 | 同上 |
| `ConfigurableBeanFactory` | 作用域/类型转换器 | 同上 |
| `ConfigurableListableBeanFactory` | 冻结配置、预实例化 | 同上（最终形态） |

> 💡 面试加分：**接口分层 = 职责分离的教科书案例**——每个接口只暴露「使用者需要的」能力，实现类层层叠加。

### 2.3 DefaultListableBeanFactory 的核心字段

```java
public class DefaultListableBeanFactory extends ... {
    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();
    private volatile List<String> beanDefinitionNames;
    // 单例缓存（一级缓存在父类 DefaultSingletonBeanRegistry）
}
```

---

## 3. ApplicationContext 能力矩阵

| 能力 | 接口 | 说明 |
|------|------|------|
| Bean 管理 | 继承 Listable/Hierarchical/AutowireCapable | 容器全部能力 |
| 环境 | `EnvironmentCapable` | 属性源（PropertySource）管理 |
| 事件 | `ApplicationEventPublisher` | publishEvent 发布 |
| 国际化 | `MessageSource` | getMessage 多语言 |
| 资源 | `ResourcePatternResolver` | classpath:/file: 统一访问 |
| AOP 集成 | 自动注册 `AutoProxyCreator` | 容器启动时注册代理后置处理器 |

```java
// 使用侧视角：一个接口拿到全部能力
public class Demo {
    @Autowired
    private ApplicationContext ctx;

    public void use() {
        ctx.getBean("userService");              // Bean 管理
        ctx.getEnvironment().getProperty("k");   // 环境
        ctx.publishEvent(new MyEvent(this));      // 事件
        ctx.getMessage("greeting", null, Locale.CHINA); // 国际化
        ctx.getResource("classpath:app.yml");     // 资源
    }
}
```

---

## 4. refresh() 十二步源码解析

### 4.1 总览：AbstractApplicationContext.refresh()

```java
public void refresh() throws BeansException {
    // ① 启动前的准备：环境、时间戳、早期监听器
    prepareRefresh();

    // ② 创建 BeanFactory（新容器：DefaultListableBeanFactory）
    ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

    // ③ 准备 BeanFactory：类加载器、表达式解析器、注册默认环境 bean
    prepareBeanFactory(beanFactory);

    try {
        // ④ 子类扩展点：允许子容器注册自己的后置处理器
        postProcessBeanFactory(beanFactory);

        // ⑤ 执行 BeanFactoryPostProcessor（修改 BeanDefinition！）
        invokeBeanFactoryPostProcessors(beanFactory);

        // ⑥ 注册 BeanPostProcessor（不执行，只注册）
        registerBeanPostProcessors(beanFactory);

        // ⑦ 初始化国际化 MessageSource
        initMessageSource();

        // ⑧ 初始化事件广播器
        initApplicationEventMulticaster();

        // ⑨ 子类扩展点：onRefresh（Servlet 容器等）
        onRefresh();

        // ⑩ 注册监听器（含早期监听器）
        registerListeners();

        // ⑪ 核心！实例化所有非懒加载单例
        finishBeanFactoryInitialization(beanFactory);

        // ⑫ 完成：发布 ContextRefreshedEvent
        finishRefresh();
    } catch (BeansException ex) {
        destroyBeans();                        // 失败回滚
        cancelRefresh(ex);
        throw ex;
    }
}
```

### 4.2 十二步速记表

| 步 | 关键动作 | 面试常问 |
|:---:|------|------|
| ① | prepareRefresh 准备环境 | — |
| ② | 创建 BeanFactory | 两种容器都走这步 |
| ③ | 准备：注册默认 bean | `environment`/`systemProperties` |
| ④ | 子类扩展点 | Web 容器在此注册 |
| ⑤ | **执行 BeanFactoryPostProcessor** | 改 BeanDefinition 的时机 |
| ⑥ | 注册 BeanPostProcessor | 只注册不执行 |
| ⑦ | 初始化 MessageSource | 国际化 |
| ⑧ | 初始化事件广播器 | 事件总线 |
| ⑨ | onRefresh 扩展点 | SpringMVC 创建 WebMvc 容器 |
| ⑩ | 注册监听器 | 事件订阅 |
| ⑪ | **预实例化单例** | Bean 生命周期主场 |
| ⑫ | finishRefresh | 发布 ContextRefreshedEvent |

> 🎯 面试必答：**⑤ 和 ⑪ 是两座大山**——⑤ 处理「bean 定义」阶段（改元数据），⑪ 处理「bean 实例」阶段（真正创建对象）。BeanFactoryPostProcessor 与 BeanPostProcessor 一字之差，作用对象完全不同。

### 4.3 ⑤ 与 ⑪ 的深入分工

| 步骤 | 处理对象 | 典型实现 |
|------|------|------|
| ⑤ BeanFactoryPostProcessor | BeanDefinition | ConfigurationClassPostProcessor（@ComponentScan）、PropertySourcesPlaceholderConfigurer（@Value 占位符） |
| ⑥/⑪ BeanPostProcessor | bean 实例 | AutowiredAnnotationBeanPostProcessor、AbstractAutoProxyCreator |

---

## 5. 启动入口：AnnotationConfigApplicationContext

### 5.1 构造流程

```java
// 用户代码：一行启动
new AnnotationConfigApplicationContext(AppConfig.class);

// 内部三步：
// ① this() → 无参构造：注册默认后置处理器
//    （ConfigurationClassPostProcessor、AutowiredAnnotationBeanPostProcessor
//      EventListenerMethodProcessor 等——通过注解驱动的注册器）
// ② register(AppConfig.class) → 把配置类注册为 BeanDefinition
// ③ refresh() → 走十二步
```

### 5.2 注解驱动的注册机制

```java
// 为什么 new 一下就全自动？关键在注册器：
// AnnotationConfigUtils.registerAnnotationConfigProcessors(registry) 注册：
//   · ConfigurationClassPostProcessor    → 解析 @Configuration/@ComponentScan
//   · AutowiredAnnotationBeanPostProcessor → 解析 @Autowired/@Value
//   · CommonAnnotationBeanPostProcessor   → 解析 @Resource/@PostConstruct
//   · EventListenerMethodProcessor       → 解析 @EventListener
// 这就是「约定大于配置」的源码起点
```

---

## 6. Spring 7.0 容器新特性（2026）

### 6.1 核心新增能力

| 特性 | 说明 | 替代了什么 |
|------|------|------|
| `BeanRegistrar` | 模块化批量注册 bean，拆分大容器 | 部分 `@Conditional` 组合 |
| `@Fallback` | 降级/备用 bean 支持 | 手动兜底逻辑 |
| 并行 bean 初始化 | 非依赖 bean 并行创建 | 大应用启动提速 |
| 后台懒加载 | 后台线程懒加载 bean | 启动后按需 |
| 泛型注入改进 | 同泛型多 bean 精确匹配，少用 @Qualifier | 6.x 的歧义问题 |
| JSpecify null-safety | @Nullable/@NonNull 一等公民 | 编译期空值检查 |
| 原生韧性注解 | @Retryable/@ConcurrencyLimit/@CircuitBreaker 内建 | Resilience4j 集成 |

### 6.2 版本基线（2026 现状）

| 项 | 版本 |
|------|------|
| Spring Framework | 7.0.8（2026-06）；7.1 预计 2026-11 |
| Spring Boot | 4.0.x / 4.1.x |
| Java 基线 | 17+（兼容 21/24） |
| Jakarta EE | 11（javax 全部移除） |
| AOT/GraalVM | 一等公民（反射/资源/代理提示自动收集） |

### 6.3 面试表述

```text
「Spring 7.0 容器层面：BeanRegistrar 支持模块化批量注册、
 @Fallback 提供降级 bean、容器支持并行初始化与后台懒加载、
 泛型注入按参数精确匹配。refresh() 十二步骨架未变——
 核心生命周期（实例化→填充→初始化）与三级缓存机制保持稳定。」
```

---

## 7. 常见面试追问

| 问题 | 标准回答 |
|------|------|
| BeanFactory vs ApplicationContext？ | 功能分层：后者 = 前者 + 环境/事件/国际化/资源；启动方式不同（ApplicationContext 走 refresh 预加载单例） |
| refresh 能被调用两次吗？ | 不能——第二次会抛 IllegalStateException（容器已启动标记） |
| ⑤ 和 ⑥ 的区别？ | ⑤ 操作 BeanDefinition（元数据），⑥ 注册 BeanPostProcessor（实例钩子） |
| 为什么要有父子容器？ | 子容器可覆盖/共享父容器 bean（SpringMVC 的 root/child 结构） |
| Spring 7.0 有什么变化？ | 见 6.2/6.3 表述 |

> 🎯 **核心要点**：容器体系 = **BeanFactory（引擎）+ ApplicationContext（整车）+ refresh()（启动流程）** 三件套。记忆锚点：十二步中的 ⑤（BeanDefinition 后置处理）与 ⑪（单例创建）是面试主战场；`new AnnotationConfigApplicationContext(xxx.class)` 一行背后是「注册默认后置处理器 → 注册配置类 → refresh」。Spring 7.0（2026）新增 BeanRegistrar/@Fallback/并行初始化，但十二步骨架与三级缓存不变——面试答「核心机制稳定、能力增量演进」最稳。

---

**下一模块**：[02-BeanDefinition注册与解析机制](02-BeanDefinition注册与解析机制.md) | **返回总览**：[00-SpringCore专题总览](00-SpringCore专题总览.md)
