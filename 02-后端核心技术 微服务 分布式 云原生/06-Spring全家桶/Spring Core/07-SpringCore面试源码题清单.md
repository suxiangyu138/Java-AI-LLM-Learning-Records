# Spring Core 面试源码题清单
> 高频源码追问 + 标准回答 + 源码位置——覆盖容器、生命周期、循环依赖、代理四大主题，附追问链设计与避坑提示

## 目录
1. [容器体系（10 问）](#1-容器体系10-问)
2. [Bean 生命周期（10 问）](#2-bean-生命周期10-问)
3. [循环依赖与三级缓存（8 问）](#3-循环依赖与三级缓存8-问)
4. [DI 与自动装配（8 问）](#4-di-与自动装配8-问)
5. [AOP 与代理（8 问）](#5-aop-与代理8-问)
6. [Spring 7.0 新特性（4 问）](#6-spring-70-新特性4-问)
7. [源码位置速查表](#7-源码位置速查表)

---

## 1. 容器体系（10 问）

| # | 问题 | 标准回答 | 源码位置 |
|:---:|------|------|------|
| 1 | BeanFactory vs ApplicationContext？ | 引擎 vs 整车：后者 + 环境/事件/国际化/资源 | AbstractApplicationContext |
| 2 | refresh() 有几步？核心是哪两步？ | 十二步；⑤ 后置处理器（改定义）、⑪ 预实例化（建实例） | refresh() |
| 3 | ⑤ 与 ⑥ 的区别？ | ⑤ BeanFactoryPostProcessor 改 BeanDefinition；⑥ BeanPostProcessor 注册实例钩子 | invokeBeanFactoryPostProcessors / registerBeanPostProcessors |
| 4 | 为什么 new AnnotationConfigApplicationContext 一行全自动？ | 注册了 4 个默认后置处理器 + refresh | AnnotationConfigUtils |
| 5 | 容器能 refresh 两次吗？ | 不能，抛 IllegalStateException | refresh() 开头 |
| 6 | 父子容器什么用？ | 共享/覆盖 bean，SpringMVC root/child 结构 | HierarchicalBeanFactory |
| 7 | 单例 bean 什么时候创建的？ | 非懒加载单例在 refresh ⑪ 预实例化 | preInstantiateSingletons |
| 8 | 懒加载 bean 什么时候创建？ | 首次 getBean 时 | doGetBean |
| 9 | ApplicationContext 有哪些子类？ | AnnotationConfig/ClassPathXml/FileSystemXml | — |
| 10 | 容器关闭时发生什么？ | 发布 ContextClosedEvent + 销毁单例 | doClose |

---

## 2. Bean 生命周期（10 问）

| # | 问题 | 标准回答 | 源码位置 |
|:---:|------|------|------|
| 1 | doCreateBean 三步？ | 实例化 → 属性填充 → 初始化 | doCreateBean |
| 2 | 实例化有几种方式？ | 工厂方法（@Bean）/ 自动装配构造器 / 默认构造器 | createBeanInstance |
| 3 | Aware 回调有哪些？ | BeanName/BeanFactory/ApplicationContext/BeanClassLoader | initializeBean |
| 4 | 初始化方法执行顺序？ | @PostConstruct → afterPropertiesSet → init-method | invokeInitMethods |
| 5 | @PostConstruct 为什么在最前？ | 它在「初始化前」后置处理器执行 | postProcessBeforeInitialization |
| 6 | AOP 代理在哪个环节生成？ | 初始化后（afterInitialization）；循环依赖时提前代理 | applyBeanPostProcessorsAfterInitialization |
| 7 | BeanPostProcessor 注册时机？ | refresh ⑥ 注册、⑪ 才在 bean 创建时执行 | registerBeanPostProcessors |
| 8 | 销毁顺序？ | @PreDestroy → DisposableBean.destroy → destroy-method | destroySingletons |
| 9 | 原型 bean 销毁谁负责？ | 调用方（容器不跟踪原型） | — |
| 10 | 三个扩展点家族？ | InstantiationAware（实例化/填充）/ BeanPostProcessor（初始化）/ DestructionAware（销毁） | — |

---

## 3. 循环依赖与三级缓存（8 问）

| # | 问题 | 标准回答 | 源码位置 |
|:---:|------|------|------|
| 1 | 循环依赖解决的前提？ | 单例 + 非构造器注入 | — |
| 2 | 三级缓存结构？ | 成品 / 半成品 / ObjectFactory | DefaultSingletonBeanRegistry |
| 3 | 三级缓存何时放？ | populateBean 前 addSingletonFactory | doCreateBean |
| 4 | 工厂什么时候执行？ | 其他 bean 依赖且一级二级都没命中时 | getSingleton |
| 5 | 提前代理怎么回事？ | getEarlyBeanReference → SmartInstantiationAwareBeanPostProcessor | getEarlyBeanReference |
| 6 | 为什么三级不能降二级？ | AOP 一致性：延迟决策避免过度代理 | — |
| 7 | 构造器循环依赖为什么死？ | 无法提前暴露（创建即需要依赖） | BeanCurrentlyInCreationException |
| 8 | @Async 循环依赖为什么失效？ | 其代理后置处理器无提前代理能力 | AsyncAnnotationBeanPostProcessor |

---

## 4. DI 与自动装配（8 问）

| # | 问题 | 标准回答 | 源码位置 |
|:---:|------|------|------|
| 1 | 三种注入方式？推荐哪个？ | 构造器（推荐：必填+不可变）/ Setter（可选）/ 字段（原型） | — |
| 2 | @Autowired 谁处理的？ | AutowiredAnnotationBeanPostProcessor（populateBean 阶段） | postProcessProperties |
| 3 | 多候选怎么选？ | Primary → Qualifier → 泛型（7.0 精确）→ 名称 | doResolveDependency |
| 4 | @Value 怎么解析的？ | 占位符（Environment）+ SpEL + 类型转换 | doResolveDependency |
| 5 | 泛型注入底层靠什么？ | ResolvableType | — |
| 6 | @Autowired 静态字段为什么失效？ | 后置处理器不处理静态字段 | — |
| 7 | 构造器参数还要 @Autowired 吗？ | 单构造器自动推断（4.3+），多构造器需标注 | determineCandidateConstructors |
| 8 | 注入失败抛什么异常？ | NoSuchBeanDefinitionException / NoUniqueBeanDefinitionException | — |

---

## 5. AOP 与代理（8 问）

| # | 问题 | 标准回答 | 源码位置 |
|:---:|------|------|------|
| 1 | JDK vs CGLIB 选择规则？ | 有接口默认 JDK；proxyTargetClass=true 或无接口 → CGLIB | DefaultAopProxyFactory |
| 2 | Spring Boot 为什么默认 CGLIB？ | 统一行为 + 调用性能 | spring.aop.proxy-target-class |
| 3 | @Configuration 为什么被代理？ | @Bean 单例语义（内部互调返回容器单例） | ConfigurationClassEnhancer |
| 4 | proxyBeanMethods=false 会怎样？ | 不代理，@Bean 内部互调每次 new（Lite 模式） | — |
| 5 | 代理链怎么构建？ | Advisor（切点+通知）→ 拦截器链 → ReflectiveMethodInvocation | getAdvicesAndAdvisorsForBean |
| 6 | 通知执行顺序？ | Around → Before → 目标 → AfterReturning/AfterThrowing → After → Around 收尾 | ReflectiveMethodInvocation |
| 7 | 自调用为什么失效？ | this 指向目标非代理 | — |
| 8 | 循环依赖 + AOP 怎么保证一致？ | 提前代理（getEarlyBeanReference） | — |

---

## 6. Spring 7.0 新特性（4 问）

| # | 问题 | 标准回答 |
|:---:|------|------|
| 1 | Spring 7.0 版本基线？ | Framework 7.0.8（2026-06），Java 17+，Jakarta EE 11，Boot 4.0/4.1 |
| 2 | 容器新增了什么？ | BeanRegistrar（模块化注册）、@Fallback（降级 bean）、并行初始化、后台懒加载 |
| 3 | 泛型注入有什么改进？ | 同泛型多 bean 按参数精确匹配，减少 @Qualifier |
| 4 | 核心机制变了吗？ | refresh 十二步、三级缓存、BeanPostProcessor 体系保持稳定 |

---

## 7. 源码位置速查表

| 类 | 关键方法 | 对应问题 |
|------|------|------|
| AbstractApplicationContext | refresh() / finishBeanFactoryInitialization | 启动流程 |
| DefaultListableBeanFactory | registerBeanDefinition / getBeanDefinition | 注册表 |
| DefaultSingletonBeanRegistry | getSingleton / addSingletonFactory | 三级缓存 |
| AbstractAutowireCapableBeanFactory | doCreateBean / createBeanInstance / populateBean / initializeBean | 生命周期 |
| AutowiredAnnotationBeanPostProcessor | postProcessProperties / findAutowiringMetadata | DI 注入 |
| DefaultAopProxyFactory | createAopProxy | 代理选择 |
| JdkDynamicAopProxy / CglibAopProxy | invoke / intercept | 代理执行 |
| ReflectiveMethodInvocation | proceed | 拦截器链 |
| ConfigurationClassEnhancer | enhance（内部类） | @Configuration 代理 |
| ConfigurationClassPostProcessor | postProcessBeanDefinitionRegistry | 注解解析 |

### 7.1 面试追问链设计（面试官视角）

```text
链一（容器）：refresh 十二步 → ⑤ 与 ⑪ → 单例创建时机 → doCreateBean 三步
链二（生命周期）：doCreateBean → populateBean → 循环依赖 → 三级缓存 → 提前代理
链三（代理）：初始化后代理 → JDK/CGLIB 选择 → @Configuration 代理 → 自调用失效
链四（进阶）：@Async 循环依赖失效 → ObjectProvider 方案 → 设计上避免循环依赖

应对策略：每条链走到「为什么」为止，答出设计意图比背方法名重要
```

> 🎯 **核心要点**：48 道题按四链组织（容器/生命周期/循环依赖/代理 + 新特性），每道都有源码位置可查。备考策略：**先画 doCreateBean 时序图，再背三个 Map，最后过一遍代理选择规则**——三个主线覆盖 80% 追问。Spring 7.0 新特性是 2026 年差异化考点（BeanRegistrar/@Fallback/并行初始化/泛型注入），答完记得补一句「核心机制稳定」。

---

**返回总览**：[00-SpringCore专题总览](00-SpringCore专题总览.md) | **上一篇**：[06-@Configuration与AOP代理机制](06-@Configuration与AOP代理机制.md) | **关联**：[Spring框架核心面试考点](../Spring框架核心/10-面试高频考点与总结.md)
