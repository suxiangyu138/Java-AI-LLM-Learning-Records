# 02-Spring 容器源码架构地图
> Spring 容器源码的"导航图"：BeanFactory 层级、ApplicationContext 体系、Environment/类型转换/资源加载的类图索引与入口速查

## 📚 目录
1. [地图怎么用](#1-地图怎么用)
2. [BeanFactory 接口层级](#2-beanfactory-接口层级)
3. [ApplicationContext 体系](#3-applicationcontext-体系)
4. [BeanDefinition 与注册](#4-beandefinition-与注册)
5. [Environment 与配置体系](#5-environment-与配置体系)
6. [类型转换与 BeanWrapper](#6-类型转换与-beanwrapper)
7. [refresh() 十二步速查](#7-refresh-十二步速查)
8. [核心要点](#8-核心要点)
9. [参考来源](#9-参考来源)

## 1. 地图怎么用

| 使用方式 | 说明 |
|----------|------|
| 查入口 | 遇到"某功能入口在哪"→ 查本文件入口速查表 |
| 查归属 | 遇到"这个类是什么"→ 查接口层级定位 |
| 配合深挖 | 机制细节（生命周期/循环依赖）→ 引用「Spring生态深度剖析」01~06 |

> 💡 本文件是**索引不是教程**：每节给出类图骨架 + 关键方法 + 一句话职责，深读时用 IDEA 在对应类上打断点。

## 2. BeanFactory 接口层级

```text
BeanFactory（最底层：getBean/containsBean）
 └─ HierarchicalBeanFactory（父子容器）
     └─ ConfigurableBeanFactory（可配置：单例注册表/类型转换/BeanPostProcessor 注册）
         └─ AbstractBeanFactory
             ├─ AbstractAutowireCapableBeanFactory（自动装配：实例化-属性填充-初始化）
             │     └─ DefaultListableBeanFactory  ★ 实际使用的核心实现
             │           （BeanDefinition 注册表 + 单例注册表 + 依赖解析）
             └─ (XmlBeanFactory 已废弃，7.x 用 DefaultListableBeanFactory)
```

| 接口/类 | 关键方法 | 职责 |
|---------|---------|------|
| `BeanFactory` | `getBean` | 最基础契约 |
| `HierarchicalBeanFactory` | `getParentBeanFactory` | 父子容器 |
| `ConfigurableBeanFactory` | `addBeanPostProcessor`、`registerSingleton` | 可配置能力 |
| `AbstractAutowireCapableBeanFactory` | `createBean` → `doCreateBean` | **实例化三阶段**（构造/属性/初始化） |
| `DefaultListableBeanFactory` | `preInstantiateSingletons` | 启动时预实例化入口 |

> 🎯 **读法锚点**：面试"Spring 怎么创建 Bean"的源码路径 = `AbstractApplicationContext.refresh` → `finishBeanFactoryInitialization` → `DefaultListableBeanFactory.preInstantiateSingletons` → `AbstractAutowireCapableBeanFactory.doCreateBean`（详见「Spring生态深度剖析」01/03）。

## 3. ApplicationContext 体系

```text
ApplicationContext（高级容器：资源加载/事件广播/国际化/环境抽象）
 ├─ ConfigurableApplicationContext（refresh()/close()/addApplicationListener）
 │     └─ AbstractApplicationContext  ★ 模板方法：refresh() 十二步总导演
 │           ├─ GenericApplicationContext（可编程注册 BeanDefinition）
 │           │     └─ AnnotationConfigApplicationContext  ★ Boot 4 时代主要入口
 │           └─ ServletWebServerApplicationContext（Boot 内嵌 Web 容器上下文）
 └─ WebApplicationContext（Web 专用：ServletContext/请求作用域）
```

| 类 | 定位 |
|----|------|
| `AnnotationConfigApplicationContext` | 注解驱动容器（Spring 应用主入口） |
| `GenericApplicationContext` | 无 XML 的可编程容器基座 |
| `ServletWebServerApplicationContext` | Boot 内嵌 Tomcat 等 Web 容器上下文 |
| `AbstractApplicationContext#refresh` | 容器启动总方法（见第 7 节） |

## 4. BeanDefinition 与注册

```text
BeanDefinition（Bean 图纸：class/scope/lazy/initMethod/属性值...）
 ├─ GenericBeanDefinition（常用实现，可合并 Parent）
 ├─ ScannedGenericBeanDefinition（组件扫描产物）
 └─ ConfigurationClassBeanDefinition（@Bean 方法产物）

注册：BeanDefinitionRegistry.registerBeanDefinition(name, definition)
存储：DefaultListableBeanFactory.beanDefinitionMap
```

| 注册路径 | 入口 | 产物类型 |
|----------|------|---------|
| 组件扫描 | `ClassPathBeanDefinitionScanner`（`@Component/@Service`...） | ScannedGenericBeanDefinition |
| 配置类 `@Bean` | `ConfigurationClassPostProcessor`（解析 `@Bean` 方法） | ConfigurationClassBeanDefinition |
| `@Import` | `ConfigurationClassParser` | 由 ImportSelector/Registrar 决定 |
| 编程注册 | `BeanDefinitionRegistry` 直接调用 | GenericBeanDefinition |

> 💡 **读法锚点**：三种注册路径汇聚到同一个 `DefaultListableBeanFactory`，这是"所有 Bean 都在一个注册表"的源码事实。

## 5. Environment 与配置体系

```text
Environment
 ├─ PropertyResolver（getProperty：占位符解析）
 │     └─ ConfigurablePropertyResolver
 └─ EnvironmentCapable
       └─ AbstractEnvironment
             ├─ PropertySources（有序属性源列表）
             │     ├─ 系统属性 System.getProperties()
             │     ├─ 系统环境变量 System.getenv()
             │     └─ 配置文件属性源（Boot：ConfigDataEnvironmentPostProcessor 注入）
             └─ MutablePropertySources（可增删，后增者优先）
```

| 关键类 | 职责 |
|--------|------|
| `StandardEnvironment` | 非 Web 环境 |
| `StandardServletEnvironment` | Web 环境（加 ServletContext 参数） |
| `PropertySourcesPlaceholderConfigurer` | `${...}` 占位符解析（BeanFactoryPostProcessor） |
| `ConfigDataEnvironmentPostProcessor` | Boot 加载 application.yml 的核心 |

> ⚠️ 读配置优先级问题的正确姿势：断点打在 `PropertySourcesPropertyResolver.getProperty`，看 `propertySources` 的**顺序**——源码顺序即优先级。

## 6. 类型转换与 BeanWrapper

```text
类型转换体系（三套并存，7.x 以 ConversionService 为主）
 ├─ ConversionService（convert(Object, Class)）
 │     ├─ DefaultConversionService（内置常用转换器）
 │     └─ ApplicationConversionService（Boot 扩展）
 ├─ PropertyEditor（老体系，已边缘化）
 └─ TypeConverterDelegate（BeanWrapper 内部委托）

BeanWrapper
 └─ BeanWrapperImpl（属性设置/嵌套属性/类型转换的封装）
       └─ 由 AbstractAutowireCapableBeanFactory.initializeBean → applyPropertyValues 使用
```

| 场景 | 源码入口 |
|------|---------|
| `@Value` 注入 | `AutowiredAnnotationBeanPostProcessor` → TypeConverter |
| 配置绑定（Boot） | `Binder`（`spring-boot` 的 `ConfigurationProperties` 绑定引擎，独立于 ConversionService） |
| SpEL 求值 | `StandardEvaluationContext` |

## 7. refresh() 十二步速查

```text
AbstractApplicationContext#refresh（7.0 版本，与 Boot 4 对齐）
 ① prepareRefresh()              环境准备/早期监听器
 ② obtainFreshBeanFactory()      刷新 BeanFactory（可关闭旧容器）
 ③ prepareBeanFactory()          注入基础设施（BeanClassLoader/Environment 等）
 ④ postProcessBeanFactory()      子类扩展点（Web 容器注入 ServletContext）
 ⑤ invokeBeanFactoryPostProcessors() ★ 执行 BFPP（ConfigurationClassPostProcessor 在此）
 ⑥ registerBeanPostProcessors()  ★ 注册 BPP（BeanPostProcessor 全部实例化）
 ⑦ initMessageSource()           国际化
 ⑧ initApplicationEventMulticaster()  事件广播器
 ⑨ onRefresh()                   模板方法（Boot 启动 Web 服务器在此）
 ⑩ registerListeners()           注册监听器 Bean
 ⑪ finishBeanFactoryInitialization() ★ 预实例化所有单例（createBean 主战场）
 ⑫ finishRefresh()               完成：发布 ContextRefreshedEvent
```

| 步骤 | 面试锚点 |
|------|---------|
| ⑤ | `ConfigurationClassPostProcessor` 执行时机（自动配置/@Bean 解析） |
| ⑥ | BPP 注册先于单例实例化（扩展点可用性） |
| ⑨ | Boot 内嵌服务器启动点 |
| ⑪ | 单例创建主战场（循环依赖在此解决） |
| ⑫ | `ContextRefreshedEvent`（应用启动后事件） |

> 💡 完整逐行分析见「Spring生态深度剖析」02/06；本文件只做**步骤索引**。

## 8. 核心要点

> 🎯 **核心要点**：
> - 三张地图：BeanFactory 层级（能力）、ApplicationContext 体系（入口）、refresh 十二步（时序）；
> - 所有 Bean 汇聚 `DefaultListableBeanFactory` 两个注册表（BeanDefinition + 单例）；
> - 配置优先级 = `PropertySources` 列表顺序（断点看顺序）；
> - 读源码固定动作：入口类 → 断点 → 看调用栈 → 对表核对。

## 9. 参考来源

- [Spring Framework GitHub（v7.0.x）](https://github.com/spring-projects/spring-framework)
- [Spring Framework Reference：Core（容器章节）](https://docs.spring.io/spring-framework/reference/core.html)
- [Spring 生态深度剖析（同仓库 01~06 机制详解）](../Spring生态深度剖析/00-Spring生态深度剖析总览.md)

---

**下一模块**：[03-扩展机制与设计模式源码全景](03-扩展机制与设计模式源码全景.md)　/　**返回总览**：[00-总览](00-高级进阶与源码学习总览.md)
