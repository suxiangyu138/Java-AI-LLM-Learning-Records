# 01 容器体系：BeanFactory 与 ApplicationContext

> IoC 容器是 Spring 的心脏——理解 BeanFactory 的底层能力与 ApplicationContext 的企业级扩展，是读懂一切 Spring 行为的前提

---

## 📚 目录

1. [IoC 的本质：容器反转了什么](#1-ioc-的本质容器反转了什么)
2. [BeanFactory：最底层的容器能力](#2-beanfactory最底层的容器能力)
3. [ApplicationContext：企业级容器的五次升级](#3-applicationcontext企业级容器的五次升级)
4. [容器体系类层次图与常见实现](#4-容器体系类层次图与常见实现)
5. [getBean 的查找过程](#5-getbean-的查找过程)
6. [容器启动流程速览](#6-容器启动流程速览)

---

## 1. IoC 的本质：容器反转了什么

**控制反转（Inversion of Control）**：对象**创建权、生命周期管理权、依赖装配权**从"对象自己"反转给"容器"。

```java
// 传统：对象自己 new 依赖 —— 依赖硬编码，换实现要改代码
public class OrderService {
    private final OrderDao dao = new MySqlOrderDao();    // 自己造依赖
}

// IoC：只声明"我要什么"，由容器给 —— 换实现只改配置/声明
public class OrderService {
    private final OrderDao dao;                          // 依赖由容器注入
    public OrderService(OrderDao dao) { this.dao = dao; }
}
```

**反转的三个维度**：

| 维度 | 传统（主动） | IoC（被动） |
|------|------------|------------|
| 创建 | 自己 `new` | 容器 `createBean` |
| 装配 | 自己组装依赖 | 容器自动注入 |
| 生命周期 | 自己管理 | 容器管理（单例缓存/销毁回调） |

**为什么值得反转**：解耦（依赖抽象而非实现）→ 可替换（换实现零改动）→ 可测试（注入 Mock）→ 统一管理（单例、代理、事务）。

> 🎯 **核心要点**：IoC 不是 Spring 发明的模式，而是"**依赖倒置原则**"（DIP，见 `Java面向对象/09`）的工程化实现——Spring 只是把"谁创建、谁注入、何时销毁"变成了容器基础设施。

---

## 2. BeanFactory：最底层的容器能力

**BeanFactory 是容器的最底层接口**，定义了 IoC 的基本契约——"按名字/类型获取 Bean，惰性实例化"：

```java
public interface BeanFactory {
    Object getBean(String name);
    <T> T getBean(Class<T> requiredType);
    boolean containsBean(String name);
    boolean isSingleton(String name);
    boolean isPrototype(String name);
    String[] getAliases(String name);
    // ... 更多
}
```

**BeanFactory 的三个关键特性**：

| 特性 | 说明 |
|------|------|
| 惰性实例化 | 只有调用 `getBean` 才创建实例（与 ApplicationContext 的预加载不同） |
| 能力最小 | 只负责"Bean 的获取与元信息查询"，无资源加载、事件、国际化 |
| 核心实现 | `DefaultListableBeanFactory`——**几乎所有容器的终极实现**（ApplicationContext 内部组合它） |

```java
// 最小容器用法（几乎只在源码/测试中出现）
DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
factory.registerBeanDefinition("orderService",
        new RootBeanDefinition(OrderService.class));        // 注册"图纸"
OrderService service = factory.getBean(OrderService.class); // 惰性创建
```

> 💡 面试点：`DefaultListableBeanFactory` 是 Spring 的"万能容器"——ApplicationContext 都把 Bean 注册与获取委托给它。**理解了这个类，就理解了容器的一半**。

---

## 3. ApplicationContext：企业级容器的五次升级

**ApplicationContext（应用上下文）在 BeanFactory 之上叠加了五层企业级能力**：

```java
public interface ApplicationContext extends EnvironmentCapable,
        ListableBeanFactory, HierarchicalBeanFactory,          // ① 可列举/可分层
        MessageSource,                                          // ② 国际化
        ApplicationEventPublisher,                              // ③ 事件发布
        ResourcePatternResolver {                               // ④ 资源加载
    // ⑤ 更多：单例预加载、自动注册 BeanPostProcessor、工厂钩子
}
```

| 能力 | 接口/机制 | 工程价值 |
|------|-----------|---------|
| 可列举 | `ListableBeanFactory` | 按类型列出所有 Bean（`getBeansOfType`） |
| 可分层 | `HierarchicalBeanFactory` | 父子容器（Spring MVC 的经典用法：子容器可看父容器） |
| 国际化 | `MessageSource` | `getMessage("key", ...)` 多语言 |
| 事件 | `ApplicationEventPublisher` | `publishEvent` 发布事件 |
| 资源 | `ResourcePatternResolver` | `classpath:`、`file:`、`url:` 统一加载 |
| 预加载 | `refresh()` 时实例化单例 | 启动即就绪（与 BeanFactory 惰性相反） |

```java
// 工程中几乎总是 ApplicationContext —— 启动即创建所有单例，提前暴露配置/依赖错误
ApplicationContext ctx = new AnnotationConfigApplicationContext(AppConfig.class);
OrderService service = ctx.getBean(OrderService.class);   // 已预加载，直接可用
```

> 🎯 **核心要点**：**BeanFactory = 会造 Bean 的容器；ApplicationContext = 企业级容器**（加资源/事件/国际化/预加载）。日常开发用 ApplicationContext，BeanFactory 只出现在源码与框架内部。

---

## 4. 容器体系类层次图与常见实现

```text
BeanFactory                          —— 最底层契约
  └── ListableBeanFactory            —— 可列举
        └── ApplicationContext       —— 企业级容器（组合）
              ├── ClassPathXmlApplicationContext   （XML 时代，已边缘化）
              ├── FileSystemXmlApplicationContext  （XML 文件）
              ├── AnnotationConfigApplicationContext  ✅ 现代主力（注解驱动）
              └── GenericWebApplicationContext / ServletWebServerApplicationContext
                    └── AnnotationConfigServletWebServerApplicationContext  ✅ Boot 内部容器

核心实现组合：
ApplicationContext
  └── 组合持有 DefaultListableBeanFactory  ←—— 所有 Bean 注册/获取的真正执行者
  └── 组合持有 BeanPostProcessor 集合     ←—— 初始化扩展点（AOP 代理在此诞生）
  └── refresh() 方法                      ←—— 容器启动总指挥
```

**常见实现对比**：

| 实现 | 配置方式 | 场景 |
|------|---------|------|
| `AnnotationConfigApplicationContext` | 注解/JavaConfig | 独立应用、测试、源码阅读首选 |
| `ClassPathXmlApplicationContext` | XML | 遗留项目 |
| `AnnotationConfigServletWebServerApplicationContext` | 注解 + 内嵌 Web 服务器 | Spring Boot 内部 |

> 💡 **Spring Boot 与容器**：`SpringApplication.run()` 最终创建的是 `AnnotationConfigServletWebServerApplicationContext`（web）或 `AnnotationConfigApplicationContext`（非 web）——Boot 的自动配置本质上就是向这个容器注册大量 `@Configuration` 类。

---

## 5. getBean 的查找过程

`ctx.getBean(X.class)` 的内部过程（工程视角，源码在 `AbstractBeanFactory.doGetBean`）：

```text
getBean(type)
  ├── ① 单例缓存查找：singletonObjects（一级缓存）命中 → 直接返回
  ├── ② 父容器查找：当前容器没有 → 向上找父容器
  ├── ③ BeanDefinition 解析：按类型匹配候选 → 多个则按 @Primary/@Priority/名字 决出
  ├── ④ 创建（若还没创建）：
  │     ├── 单例：createBean → 放入单例缓存（含三级缓存过程，见 03 模块循环依赖）
  │     └── 原型：每次 createBean 都新建
  └── ⑤ 类型检查：实例化后校验 isAssignableFrom，不匹配抛 BeanNotOfRequiredTypeException
```

**getBean 的常见异常**（排查手册）：

| 异常 | 含义 | 排查方向 |
|------|------|---------|
| `NoSuchBeanDefinitionException` | 没有这个 Bean | 是否注册/扫描到？类是否被排除？ |
| `NoUniqueBeanDefinitionException` | 多个候选 | @Primary / @Qualifier / 名字 |
| `BeanCurrentlyInCreationException` | 循环依赖无法解决 | 构造器循环依赖/原型循环依赖 |
| `BeanCreationException` | 创建过程失败 | 看 cause（初始化方法、依赖注入出错） |

> 🎯 **核心要点**：getBean 的路径 = **缓存 → 父容器 → 定义解析 → 创建**——排查"Bean 找不到"时按这条链逐环检查：注册了没有 → 注册成没成 → 名字对不对 → 有没有歧义。

---

## 6. 容器启动流程速览

**ApplicationContext 启动的总指挥是 `refresh()`**（`AbstractApplicationContext.refresh`，13 步，工程视角 8 步核心）：

```text
refresh()
 ├── ① 准备：记录启动时间、设置环境、属性校验
 ├── ② 创建 BeanFactory：内部 new DefaultListableBeanFactory
 ├── ③ 加载 BeanDefinition：解析配置类/@ComponentScan 扫描 → 全部图纸入库
 ├── ④ BeanFactoryPostProcessor 执行：修改"图纸"（如 @ConfigurationProperties 绑定、占位符解析）
 ├── ⑤ 注册 BeanPostProcessor：初始化扩展点注册（后面创建 Bean 时逐个调用）
 ├── ⑥ 预加载所有单例 Bean：preInstantiateSingletons —— 按序创建全部单例
 │     └── 每个 Bean 的创建内部：实例化 → 属性填充 → 初始化（AOP 代理在此生成）
 ├── ⑦ 发布 ContextRefreshedEvent：容器就绪事件
 └── ⑧ 完成：容器可用，getBean 直接命中缓存
```

**启动失败 vs 运行失败**：单例预加载意味着**启动期就暴露配置错误**（Bean 缺失、依赖注入失败、循环依赖）——这是 ApplicationContext 的重要工程价值：错误前置。

> 💡 与源码系统的分工：本模块给"8 步启动地图"，`Spring生态深度剖析/02-SpringBoot启动流程与自动配置内核.md` 给逐行源码与自动配置细节——两篇配合阅读。

---

**下一模块**：[02-BeanDefinition与三种配置方式](./02-BeanDefinition与三种配置方式.md) / **返回总览**：[00-Spring框架核心知识体系总览](./00-Spring框架核心知识体系总览.md)
