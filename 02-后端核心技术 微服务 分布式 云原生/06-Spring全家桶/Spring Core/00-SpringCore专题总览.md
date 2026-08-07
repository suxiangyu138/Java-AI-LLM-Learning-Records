# Spring Core 专题总览
> IoC 容器的源码级深潜——从 BeanFactory 到 ApplicationContext、从 BeanDefinition 到三级缓存、从 BeanPostProcessor 到 AOP 代理，7 篇文档把 Spring 核心机制从「会用法」推进到「懂源码」（基于 Spring Framework 7.0，2026 现状）

## 📚 目录
1. [专题定位与边界](#1-专题定位与边界)
2. [专题导航](#2-专题导航)
3. [源码阅读地图](#3-源码阅读地图)
4. [核心概念速查](#4-核心概念速查)
5. [学习路线推荐](#5-学习路线推荐)
6. [面试要点](#6-面试要点)

---

## 1. 专题定位与边界

| 相邻系统 | 定位 | 与 Spring Core 的分工 |
|------|------|------|
| Spring框架核心（同级目录） | 概念与使用层 | 那边学「怎么用」，这里学「为什么/底层怎么实现」 |
| Spring全家桶 | 模块全景（IoC→MVC→微服务） | 那边是「全景」，这里是「IoC 源码专项」 |
| Spring生态 | 家族版图与版本演进 | 那边看「地图」，这里看「引擎」 |

> 🎯 一句话定位：**Spring Core = Spring 的心脏解剖室**——聚焦 `spring-context` 与 `spring-beans` 两个模块的源码实现，其余（MVC/Data/Security）都是建在这个核心上的应用层。

---

## 2. 专题导航

| 序号 | 模块 | 核心内容 |
|:---:|------|------|
| [01-BeanFactory与ApplicationContext源码体系](01-BeanFactory与ApplicationContext源码体系.md) | 容器层次、refresh() 十二步、BeanFactory 职责拆分 |
| [02-BeanDefinition注册与解析机制](02-BeanDefinition注册与解析机制.md) | BeanDefinition 结构、XML/注解/编程式三种注册、BeanDefinitionRegistryPostProcessor |
| [03-依赖注入底层原理](03-依赖注入底层原理.md) | 三种注入方式、@Autowired 处理链、自动装配模式、泛型匹配 |
| [04-Bean生命周期源码与扩展点](04-Bean生命周期源码与扩展点.md) | doCreateBean 全流程、BeanPostProcessor 链、销毁流程 |
| [05-循环依赖与三级缓存源码解析](05-循环依赖与三级缓存源码解析.md) | 三级缓存结构、提前暴露机制、@Async 失效原理 |
| [06-@Configuration与AOP代理机制](06-@Configuration与AOP代理机制.md) | CGLIB 代理、JDK 代理选择、@EnableAspectJAutoProxy、代理链构建 |
| [07-SpringCore面试源码题清单](07-SpringCore面试源码题清单.md) | 高频源码追问 + 标准回答 + 源码位置 |

---

## 3. 源码阅读地图

```text
入口：AnnotationConfigApplicationContext
  │
  ▼
refresh() 十二步（spring-context AbstractApplicationContext）
  │
  ├─ ① ② ③ 环境与配置准备
  ├─ ④ invokeBeanFactoryPostProcessors → BeanDefinition 注册/修改
  ├─ ⑤ registerBeanPostProcessors → 扩展点注册
  ├─ ⑥⑦⑧ 国际化/事件/监听器
  ├─ ⑨⑩ preInstantiateSingletons → 单例创建（核心！）
  │      └─ getBean → doGetBean → createBean → doCreateBean
  │           ├─ createBeanInstance（实例化）
  │           ├─ populateBean（属性填充 → 循环依赖在此解决）
  │           └─ initializeBean（初始化 → AOP 代理在此生成）
  └─ ⑪⑫ 完成/清理
```

---

## 4. 核心概念速查

| 概念 | 一句话 |
|------|------|
| BeanDefinition | 描述 bean 的元数据（类、作用域、依赖、初始化方法） |
| BeanFactory | 最基础的容器接口（getBean/containsBean） |
| ApplicationContext | BeanFactory + 环境/事件/国际化/资源加载 |
| refresh() | 容器启动的十二步总调度 |
| BeanPostProcessor | 初始化前后钩子（AOP/自动装配的扩展点） |
| 三级缓存 | singletonObjects / earlySingletonObjects / singletonFactories |
| 循环依赖 | 靠「提前暴露原始实例」解决（三级缓存） |
| CGLIB 代理 | 无接口时的子类代理（@Configuration 必需） |
| JDK 代理 | 有接口时的默认代理 |
| AOT | GraalVM 原生镜像的提前编译优化（7.0 一等公民） |

---

## 5. 学习路线推荐

| 路线 | 顺序 | 适用 |
|------|------|------|
| 面试路线 | 01 → 04 → 05 → 07 | 准备源码追问 |
| 系统路线 | 01 → 02 → 03 → 04 → 05 → 06 → 07 | 完整源码深潜 |
| 进阶路线 | 先看 Spring框架核心 00-10 → 再走本专题 | 从使用到源码 |

> 💡 前置要求：先过一遍「Spring框架核心」专题（概念层），再进本专题（源码层），否则会淹没在类名里。

---

## 6. 面试要点

| 问题 | 标准回答要点 | 详见 |
|------|------|:---:|
| BeanFactory 和 ApplicationContext 区别？ | 功能分层：环境/事件/国际化/资源加载 | 01 篇 |
| refresh() 干了什么？ | 十二步：准备→后置处理器→单例创建 | 01 篇 |
| Bean 生命周期？ | doCreateBean：实例化→填充→初始化→销毁 | 04 篇 |
| 循环依赖怎么解决？ | 三级缓存 + 提前暴露 ObjectFactory | 05 篇 |
| @Autowired 失效场景？ | 静态字段/new 对象/不解析的 BeanPostProcessor | 03 篇 |
| @Configuration 为什么被代理？ | CGLIB 保证 @Bean 单例语义 | 06 篇 |
| Spring 7.0 容器新特性？ | BeanRegistrar/@Fallback/并行初始化/泛型注入改进 | 01/03 篇 |

> 🎯 **核心要点**：本专题的阅读主线 = **一条 createBean 链**（实例化→填充→初始化）贯穿四篇（01 入口 → 04 全流程 → 05 循环依赖 → 06 代理生成）。面试源码题 90% 落在这一条链上：能画出这条链 + 说出每个环节的扩展点（BeanPostProcessor 家族），Spring Core 源码面就稳了。

---

**下一篇**：[01-BeanFactory与ApplicationContext源码体系](01-BeanFactory与ApplicationContext源码体系.md) | **关联专题**：[Spring框架核心](../Spring框架核心/00-Spring框架核心知识体系总览.md)、[Spring全家桶](../Spring全家桶/00-模块概述与分层学习路线.md)
