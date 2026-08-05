# 00 - Spring 生态深度剖析总览

> 🎯 源码级内部分析 — 不讨论"Spring 能做什么"，只剖析"Spring 内部怎么运转"。聚焦三大引擎：Bean 生命周期后处理器链、Boot 启动内核与自动配置机制、MVC 请求处理全链路。与现有的「生态全景」和「全家桶原理纵深」互补，定位**最底层机制层**

---

## 目录

1. [知识全景](#1-知识全景)
2. [文件导航](#2-文件导航)
3. [与现有 Spring 体系的定位关系](#3-与现有-spring-体系的定位关系)
4. [阅读路线推荐](#4-阅读路线推荐)
5. [核心概念速查](#5-核心概念速查)

---

## 1. 知识全景

```
Spring 生态深度剖析（8个文件 — 机制层，看懂内部如何运转）
│
├── 🧬 后处理器（01）
│   └── 01-Bean生命周期与PostProcessor扩展体系.md  # 两大扩展基石 + 五大扩展点 + 完整时序
│
├── 🚀 启动内核（02）
│   └── 02-SpringBoot启动流程与自动配置内核.md    # Boot 4 启动五阶段 + @Conditional 条件树
│
├── 🔄 循环依赖（03）
│   └── 03-循环依赖与三级缓存深度剖析.md          # getSingleton 源码/三级缓存迁移/AOP 代理唯一性
│
├── 🎭 AOP 与事务（04）
│   └── 04-AOP代理创建与事务管理内核.md            # 代理创建决策 + 事务传播行为源码链路
│
├── 🌐 MVC 链路（05）
│   └── 05-DispatcherServlet请求处理全链路.md      # doDispatch 八阶段 + 参数解析 + 返回值处理
│
├── 📡 事件驱动（06）
│   └── 06-事件驱动模型与ApplicationContext内部机制.md  # 事件广播/监听器注册/上下文 refresh 12 步
│
├── 🔧 自定义 Starter（07）
│   └── 07-自定义Starter与扩展点实战.md            # 自动配置开发 + @AutoConfiguration + 工业级 Starter
│
└── 📌 00-Spring生态深度剖析总览.md                 # ← 本文件
```

---

## 2. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | 深度剖析总览 | 全景导航 + 定位关系 + 路线 + 速查 | — |
| 01 | Bean生命周期与PostProcessor | BFPP/BPP/InstantiationAware/smart 五大扩展点含时序 | ⭐⭐⭐ |
| 02 | Boot启动流程与自动配置 | SpringApplication.run 五阶段 + ImportCandidates + @Conditional | ⭐⭐⭐ |
| 03 | 循环依赖与三级缓存 | singletonsCurrentlyInCreation + getSingleton 逐行分析 | ⭐⭐⭐ |
| 04 | AOP代理与事务管理 | 代理创建决策链 + TransactionInterceptor 传播行为 | ⭐⭐⭐ |
| 05 | DispatcherServlet 全链路 | doDispatch 八阶段 + 参数解析复合器 + 返回值处理器 | ⭐⭐ |
| 06 | 事件驱动与Context | refresh 12 步 + 事件广播器 + 监听器注册策略 | ⭐⭐ |
| 07 | 自定义Starter实战 | @AutoConfiguration + .imports 文件 + 工业级 Starter 模板 | ⭐⭐ |

---

## 3. 与现有 Spring 体系的定位关系

```text
三层金字塔（自上而下）

┌─────────────────────────────────────────────┐
│ Spring生态（7文件） — 「地图层」             │  ← 版图/版本/Boot4新特性/AI/选型/面试
├─────────────────────────────────────────────┤
│ 06-Spring全家桶（28文件） — 「原理层」      │  ← IoC/AOP/事务/MVC/Boot自动装配/微服务全链路
├─────────────────────────────────────────────┤
│ Spring生态深度剖析（8文件） — 「机制层」🆕  │  ← PostProcessor/Boot启动内核/缓存/MVC链路
└─────────────────────────────────────────────┘
```

| 对比维度 | 全家桶（原理层） | 深度剖析（本体系） |
|----------|------------------|-------------------|
| 视角 | 解释"是什么/为什么/怎么用" | 追问"内部怎么做到的" |
| 粒度 | 模块级（IoC/AOP/事务各一文件） | 链路级（PostProcessor 完整时序/refresh 12 步） |
| 代码 | 示意代码为主 | 源码骨架 + 关键方法调用链 |
| 目标 | 面试答出原理 | 面试答到"源码级理解" |

> 💡 **用法**：先刷全家桶建立原理基线 → 再读本体系理解内部机制 → 需要版本/新特性/选型时查生态全景。

---

## 4. 阅读路线推荐

### 🟢 核心三件（必读，半天）

```text
01- PostProcessor 体系 → 02- Boot 启动内核 → 03- 循环依赖
产出：能画出 Bean 生命周期完整时序、能解释 Boot 4 自动配置为什么改用 .imports、能讲清"三级缓存必要性"
```

### 🔵 全栈机制（深入，1 天）

```text
04- AOP 与事务 → 05- MVC 全链路 → 06- 事件驱动与 Context → 07- 自定义 Starter
产出：能写工业级 Starter、能画请求处理全链路时序图、能讲 refresh 12 步
```

---

## 5. 核心概念速查

| 概念 | 一句话 | 位置 |
|------|--------|------|
| BeanPostProcessor | 改"成品"：初始化前后增强 Bean（AOP 在此创建代理） | 01 |
| BeanFactoryPostProcessor | 改"图纸"：实例化前修改 BeanDefinition | 01 |
| InstantiationAwareBeanPostProcessor | 实例化级干预：可短路创建 + 跳过属性填充（@Autowired 在此执行） | 01 |
| AutoConfigurationImportSelector | Boot 自动配置核心加载器，读取 .imports 文件 | 02 |
| ImportCandidates | Boot 4 替代 spring.factories 的新机制（AOT 友好） | 02 |
| 三级缓存 | 一级成品/二级半成品/三级工厂 — 解决 AOP 代理下循环依赖唯一性 | 03 |
| earlySingletonExposure | 实例化后提前暴露标记（doCreateBean 中判断） | 03 |
| getEarlyBeanReference | 三级缓存 getObject() 的核心回调（SmartInstantiationAwareBPP） | 03 |
| doDispatch | DispatcherServlet 请求处理核心方法（8 阶段） | 05 |
| refresh 12 步 | ApplicationContext 一次性初始化全部 Bean 的核心流程 | 06 |
| @AutoConfiguration | Boot 4 专用注解，替代 @Configuration 用于自动配置类 | 07 |

---

> 🎯 **核心要点**：本体系的目标不是"知道 Spring 有三级缓存"，而是"能画出 getSingleton 的判断分支、能解释 earlySingletonExposure 为什么是 boolean、能在面试中主动画出 Bean 生命周期的 5 大扩展点时序"。三句话概括：**PostProcessor 是扩展机、refresh 是发动机、三级缓存是精巧设计**。

**下一模块**：[01-Bean生命周期与PostProcessor扩展体系](01-Bean生命周期与PostProcessor扩展体系.md)
