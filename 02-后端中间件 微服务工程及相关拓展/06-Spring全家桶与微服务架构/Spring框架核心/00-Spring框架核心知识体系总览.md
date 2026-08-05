# Spring 框架核心知识体系总览

> 从 BeanFactory 容器体系到依赖注入、Bean 生命周期、AOP、事务与事件——Spring Framework 核心是理解整个 Spring 生态（Boot/Cloud/AI）的地基，2025 年 7.0 代际升级后更值得系统重学

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [为什么必须学透 Spring 框架核心](#3-为什么必须学透-spring-框架核心)
4. [核心概念速查](#4-核心概念速查)
5. [与周边知识的关系](#5-与周边知识的关系)
6. [学习路线推荐](#6-学习路线推荐)
7. [快速自测 10 题](#7-快速自测-10-题)

---

## 1. 知识体系导图

```text
Spring 框架核心知识体系
│
├── 01 容器体系：BeanFactory 与 ApplicationContext
│   ├── IoC 本质：容器反转了"谁创建谁"
│   ├── BeanFactory：最底层的容器能力
│   ├── ApplicationContext：企业级容器的五次升级
│   ├── 容器体系类层次图与常见实现
│   ├── BeanFactory vs ApplicationContext 对比
│   └── 单例注册表与 getBean 的查找过程
│
├── 02 BeanDefinition 与三种配置方式
│   ├── BeanDefinition：Bean 的"图纸"
│   ├── 配置方式演进：XML → 注解 → JavaConfig
│   ├── 组件扫描 @ComponentScan 与过滤规则
│   ├── @Configuration 与 @Bean：JavaConfig 细节
│   ├── 条件装配 @Conditional 家族
│   └── Spring 7.0 的 BeanRegistrar 新编程式注册
│
├── 03 依赖注入详解与自动装配
│   ├── 三种注入方式对比与构造器注入优先
│   ├── @Autowired 装配规则与 @Qualifier/@Primary
│   ├── 泛型注入与集合注入
│   ├── @Resource vs @Autowired（jakarta 规范）
│   ├── 循环依赖：成因、处理与规避
│   └── 注入失败场景排查手册
│
├── 04 Bean 生命周期与作用域
│   ├── 生命周期全景时序（工程视角）
│   ├── Aware 接口家族与使用场景
│   ├── 初始化/销毁的三种写法与优先级
│   ├── 五大作用域与自定义作用域
│   ├── 原型 Bean 的陷阱：依赖注入 vs 获取
│   └── 生命周期扩展点选型（衔接源码剖析系统）
│
├── 05 AOP 面向切面编程
│   ├── AOP 解决的问题与核心概念（切点/通知/切面）
│   ├── 切点表达式 Pointcut 语法全解
│   ├── 五类通知与执行顺序
│   ├── 注解驱动 @Aspect（Spring Boot 开箱即用）
│   ├── JDK 动态代理 vs CGLIB：选择与细节
│   ├── 自调用失效与内部调用陷阱
│   └── Spring 7.0：CGLIB 一致默认与 @Proxyable
│
├── 06 声明式事务管理
│   ├── 编程式 vs 声明式事务
│   ├── @Transactional 属性全景（传播/隔离/超时/回滚）
│   ├── 七种事务失效场景
│   ├── 传播行为 7 种语义与嵌套事务真相
│   ├── 隔离级别与并发问题对应
│   └── 事务管理器的选择（JDBC/JPA/分布式）
│
├── 07 事件驱动机制
│   ├── 观察者模式在 Spring 的实现
│   ├── 事件定义与发布（ApplicationEvent/EventPublisher）
│   ├── @EventListener 注解驱动与异步事件
│   ├── 事务事件 @TransactionalEventListener
│   ├── 事件传播与异常处理
│   └── 事件使用场景与注意事项
│
├── 08 配置管理：Environment 与配置属性
│   ├── Environment/PropertySource 体系
│   ├── 配置文件加载优先级（properties/yaml/环境变量/命令行）
│   ├── @Value 占位符与 SpEL
│   ├── @ConfigurationProperties 类型安全绑定
│   ├── Profile 环境切换
│   └── 配置混乱排查方法
│
├── 09 资源、国际化与测试
│   ├── Resource 抽象与 ResourceLoader
│   ├── MessageSource 国际化
│   ├── TestContext 测试框架与 @SpringBootTest
│   ├── 单元测试：Mockito/断言/分层测试策略
│   └── 测试隔离与启动加速
│
└── 10 面试高频考点与总结
    ├── 必背考点：IoC/DI/生命周期/AOP/事务
    ├── 高频陷阱题 10 连问
    ├── 场景题：手写简化版 IoC 容器
    └── 记忆口诀与进阶导航
```

---

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 | 文件 |
|:---:|------|---------|---------|------|
| 01 | 容器体系 | BeanFactory/ApplicationContext 层次、getBean 过程 | 初中级必须掌握 | [01-容器体系-BeanFactory与ApplicationContext](./01-容器体系-BeanFactory与ApplicationContext.md) |
| 02 | BeanDefinition 与配置方式 | 三方式演进、扫描、条件装配、BeanRegistrar | 初中级必须掌握 | [02-BeanDefinition与三种配置方式](./02-BeanDefinition与三种配置方式.md) |
| 03 | 依赖注入与自动装配 | 注入方式、@Autowired 规则、循环依赖 | 初中级必须掌握 | [03-依赖注入详解与自动装配](./03-依赖注入详解与自动装配.md) |
| 04 | Bean 生命周期与作用域 | 生命周期时序、Aware、五作用域 | 中高级 | [04-Bean生命周期与作用域](./04-Bean生命周期与作用域.md) |
| 05 | AOP 面向切面编程 | 切点语法、五通知、代理模型、自调用陷阱 | 中高级 | [05-AOP面向切面编程](./05-AOP面向切面编程.md) |
| 06 | 声明式事务管理 | @Transactional、传播/隔离、七种失效 | 中高级 | [06-声明式事务管理](./06-声明式事务管理.md) |
| 07 | 事件驱动机制 | 事件发布订阅、异步事件、事务事件 | 中高级 | [07-事件驱动机制](./07-事件驱动机制.md) |
| 08 | 配置管理 | Environment、@Value、@ConfigurationProperties、Profile | 中高级 | [08-配置管理-Environment与配置属性](./08-配置管理-Environment与配置属性.md) |
| 09 | 资源、国际化与测试 | Resource、MessageSource、TestContext | 中高级 | [09-资源国际化与测试](./09-资源国际化与测试.md) |
| 10 | 面试高频考点与总结 | 必背考点、陷阱题、手写容器 | 面试冲刺 | [10-面试高频考点与总结](./10-面试高频考点与总结.md) |

---

## 3. 为什么必须学透 Spring 框架核心

1. **整个 Spring 生态的地基**：Spring Boot 的自动配置、Spring Cloud 的服务发现、Spring AI 的模型接入——全部建立在 IoC 容器与 AOP 之上。不懂核心容器，上层框架的"魔法"永远解释不清。
2. **2025 年发生代际升级**：Spring Framework 7.0（2025-11-13 GA）与 Spring Boot 4.0（2025-11-20）全面拥抱 Jakarta EE 11、JSpecify 空安全、CGLIB 一致代理、BeanRegistrar 编程式注册——**老教程的 Spring 6/5 内容大量过时**，本体系按 7.0 现状撰写。
3. **面试核心区**：IoC/DI 原理、Bean 生命周期、循环依赖、AOP 代理选择、事务失效场景——这些是 Java 后端面试中区分度最高的题目，答好=进大厂的基本盘。
4. **源码阅读的入口**：`DefaultListableBeanFactory`、`AbstractApplicationContext.refresh()`、`AnnotationConfigApplicationContext` 是 Spring 源码阅读的三张地图——本体系给工程视角，`Spring生态深度剖析/` 给源码细节，两者互补。
5. **工程排障的根基**：Bean 找不到、循环依赖报错、事务不生效、AOP 失效、配置不加载——90% 的 Spring 启动/运行问题都出在核心容器机制上。

---

## 4. 核心概念速查

### 4.1 五大核心概念

| 概念 | 一句话 | 对应机制 |
|------|--------|---------|
| IoC（控制反转） | 对象创建与装配权交给容器 | BeanFactory 单例注册表 |
| DI（依赖注入） | 依赖由容器注入而非自己 new | @Autowired/构造器注入 |
| Bean | 容器管理的对象单元 | BeanDefinition 描述 |
| AOP | 横切关注点与业务解耦 | 动态代理（JDK/CGLIB） |
| 声明式事务 | 事务边界用注解声明 | 事务管理器 + AOP 代理 |

### 4.2 核心注解速查

| 注解 | 用途 | 版本 |
|------|------|:----:|
| `@Component/@Service/@Repository/@Controller` | 注册 Bean（分层语义） | 2.5+ |
| `@Configuration` + `@Bean` | JavaConfig 声明式配置 | 3.0+ |
| `@Autowired` / `@Qualifier` / `@Primary` | 自动装配与冲突解决 | 2.5+ |
| `@Value` | 占位符注入配置 | 3.0+ |
| `@ConfigurationProperties` | 类型安全绑定 | 4.0+ |
| `@Aspect` / `@Around` 等 | 声明式 AOP | 2.0+ |
| `@Transactional` | 声明式事务 | 2.0+ |
| `@EventListener` / `@TransactionalEventListener` | 事件驱动 | 4.2+ |
| `@Conditional*` | 条件装配 | 4.0+ |
| `@Proxyable` | CGLIB 代理开关（Spring 7.0 新） | 7.0 |

### 4.3 版本时间线（时效性重点）

| 版本 | 发布时间 | 关键变化 |
|------|:-------:|---------|
| Spring 6.0 | 2022-11 | Java 17 基线、Jakarta EE 9、AOT 起步 |
| Spring 6.1/6.2 | 2023-11 / 2024-11 | 虚拟线程支持、RestClient、观察性强化 |
| **Spring 7.0** | **2025-11-13** | Jakarta EE 11、javax.* 全移除、JSpecify、BeanRegistrar、CGLIB 一致默认 |
| Spring Boot 4.0 | 2025-11-20 | 模块化 Starter、OpenTelemetry 专用 Starter、虚拟线程默认开启 |

---

## 5. 与周边知识的关系

```text
                    ┌── Spring全家桶/ —— 课程式全栈剖析（IoC/AOP/事务/微服务）
                    ├── Spring生态深度剖析/ —— 源码级：生命周期扩展点、三级缓存、代理内核
Spring 框架核心 ────┼── Spring生态/ —— 家族版图、版本兼容矩阵、Boot4/Spring7 新特性
                    ├── SpringBoot/ —— 自动装配与启动流程
                    ├── Java面向对象 —— 接口/代理/多态是容器实现的语言基础
                    └── Java异常体系 —— 事务回滚依赖异常传播机制
```

**本体系与其他系统的分工**：

| 系统 | 视角 | 本体系处理方式 |
|------|------|--------------|
| Spring全家桶 | 课程式（含练习题） | 交叉引用，不重复练习内容 |
| Spring生态深度剖析 | 源码级扩展点/内部机制 | 只给"工程怎么用 + 指向源码位置" |
| Spring生态 | 版图/版本矩阵/新特性全览 | 版本窗口在本体系标注，细节指向 03 号文件 |

---

## 6. 学习路线推荐

**路线一：入门夯实（3~4 天，对应模块 01-04）**
容器体系 → 配置方式 → 依赖注入 → 生命周期与作用域；每个概念手写最小示例验证，重点吃透"容器怎么创建 Bean、怎么注入依赖"。

**路线二：进阶深化（1 周，对应模块 05-09）**
AOP（重点切点表达式与代理选择）→ 事务（重点传播行为与失效场景）→ 事件 → 配置管理 → 测试；用一个小项目把 AOP 日志、事务、事件、配置全部串起来。

**路线三：面试冲刺（对应模块 10 + 深度剖析系统）**
背考点 → 自测陷阱题 → 手写简化 IoC 容器；然后进入 `Spring生态深度剖析/` 啃源码级细节（三级缓存、PostProcessor）。

> 🎯 **核心要点**：Spring 核心的学习终点是"**能解释容器每一步在做什么**"——从 `new AnnotationConfigApplicationContext()` 到 `getBean`，每一环的职责、扩展点、失效场景都了然于心。

---

## 7. 快速自测 10 题

1. `BeanFactory` 和 `ApplicationContext` 的区别？
2. 为什么推荐构造器注入而不是字段注入？
3. `@Autowired` 按类型找不到多个候选时怎么解决？
4. 循环依赖在什么情况下会报错、什么情况下能解决？
5. Bean 的初始化方法有哪三种写法？执行顺序是什么？
6. JDK 动态代理和 CGLIB 的区别？Spring 7.0 默认用哪个？
7. `@Transactional` 在同类内部方法调用时生效吗？为什么？
8. 事务传播行为 `REQUIRED` 和 `REQUIRES_NEW` 的区别？
9. `@ConfigurationProperties` 和 `@Value` 的区别？
10. `@TransactionalEventListener` 和 `@EventListener` 的区别？

> 💡 答不出的题目对应上方模块导航中的文件，按图索骥复习即可。

---

**下一模块**：[01-容器体系-BeanFactory与ApplicationContext](./01-容器体系-BeanFactory与ApplicationContext.md)
