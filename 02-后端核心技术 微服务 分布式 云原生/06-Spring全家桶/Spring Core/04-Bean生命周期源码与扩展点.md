# Bean 生命周期源码与扩展点
> doCreateBean 是 Spring 最核心的方法——实例化 → 属性填充 → 初始化三步，被 BeanPostProcessor 链层层包裹。本文件按源码顺序拆解全流程，并给出扩展点全景图

## 目录
1. [生命周期总览](#1-生命周期总览)
2. [实例化：createBeanInstance](#2-实例化createbeaninstance)
3. [属性填充：populateBean](#3-属性填充populatebean)
4. [初始化：initializeBean](#4-初始化initializebean)
5. [销毁流程](#5-销毁流程)
6. [BeanPostProcessor 扩展点全景](#6-beanpostprocessor-扩展点全景)
7. [生命周期时序图（面试手绘版）](#7-生命周期时序图面试手绘版)

---

## 1. 生命周期总览

```text
getBean → doGetBean → createBean → doCreateBean
                                          │
        ┌─────────────────────────────────┼───────────────────────────────┐
        ▼                                 ▼                               ▼
  createBeanInstance              populateBean                    initializeBean
  （实例化：构造器）               （属性填充：注入）              （初始化：AOP/回调）
        │                                 │                               │
   InstanceAware 后置处理器        Autowired 后置处理器            Aware + 初始化后置处理器
```

### 1.1 完整生命周期（含销毁）

```text
① BeanDefinition 注册（容器启动 ⑤）
② 实例化前（InstantiationAwareBeanPostProcessor.postProcessBeforeInstantiation）
③ 实例化（createBeanInstance：构造器/工厂方法）
④ 实例化后（postProcessAfterInstantiation → 可短路属性填充）
⑤ 属性填充（populateBean：Autowired/BeanDefinition 属性）
⑥ Aware 回调（BeanNameAware/BeanFactoryAware/ApplicationContextAware）
⑦ 初始化前（postProcessBeforeInitialization）
⑧ 初始化（InitializingBean.afterPropertiesSet / @PostConstruct / init-method）
⑨ 初始化后（postProcessAfterInitialization → AOP 代理在此）
⑩ 使用
⑪ 销毁（@PreDestroy / DisposableBean / destroy-method）
```

---

## 2. 实例化：createBeanInstance

### 2.1 源码路径

```java
// AbstractAutowireCapableBeanFactory.createBeanInstance
// 决策顺序：
protected BeanWrapper createBeanInstance(String beanName, RootBeanDefinition mbd, Object[] args) {
    // ① 工厂方法（@Bean / factory-method）
    // ② 参数解析后自动装配构造器
    // ③ 默认无参构造（无特殊处理器时）
    // ④ SmartInstantiationAwareBeanPostProcessor 决定构造器
    //    （如 @Autowired 标注构造器 → AutowiredAnnotationBeanPostProcessor 选择）
}
```

| 路径 | 触发条件 | 说明 |
|------|------|------|
| 工厂方法 | @Bean 方法 / FactoryBean | ConfigurationClassBeanDefinition 走此路 |
| 自动装配构造器 | 有 @Autowired 构造器 | AutowiredAnnotationBeanPostProcessor 解析参数 |
| 默认构造器 | 无参构造 | 最常见路径 |

### 2.2 实例化策略

| 策略 | 说明 |
|------|------|
| 反射（Constructor.newInstance） | 默认，慢一点 |
| Objenesis（Spring 6+ 可配） | 绕构造器（无参也走），CGLIB 子类场景需要 |

> 💡 面试细节：`@Bean` 方法创建的对象由「工厂方法调用」产生，不是反射构造——所以 @Bean 方法内的逻辑（如参数注入、手动 new）都会执行。

---

## 3. 属性填充：populateBean

### 3.1 源码逻辑

```java
// AbstractAutowireCapableBeanFactory.populateBean
protected void populateBean(String beanName, RootBeanDefinition mbd, BeanWrapper bw) {
    // ① 检查 InstantiationAwareBeanPostProcessor.postProcessAfterInstantiation
    //    → 返回 false 可跳过整个属性填充（短路）
    // ② 注入模式（BY_NAME / BY_TYPE —— XML 时代，注解时代不走）
    // ③ applyMergedBeanDefinitionPostProcessors
    //    → AutowiredAnnotationBeanPostProcessor.postProcessProperties
    //      → 收集并执行 @Autowired/@Value 注入
    // ④ 应用 BeanDefinition 中的 propertyValues（XML <property>）
}
```

### 3.2 属性填充的先后

```text
@Autowired 注入（AutowiredAnnotationBeanPostProcessor）
   ↓
XML propertyValues（配置定义的属性）
⚠️ 实际顺序：BeanDefinition 中显式属性先应用，再跑注解注入？
   准确：applyPropertyValues 在注解注入之后（3.x 起）
   面试简化回答：注解注入 + 显式属性都会执行，注解优先
```

> 🎯 记忆点：**populateBean 是「循环依赖解决」的现场**——字段注入时依赖还没创建好，会走 getBean → 三级缓存提前暴露（05 篇详述）。

---

## 4. 初始化：initializeBean

### 4.1 源码顺序（面试手写版）

```java
// AbstractAutowireCapableBeanFactory.initializeBean
protected Object initializeBean(String beanName, Object bean, RootBeanDefinition mbd) {
    // ① 激活 Aware 回调
    if (bean instanceof Aware) {
        if (bean instanceof BeanNameAware) setBeanName(beanName);
        if (bean instanceof BeanClassLoaderAware) setBeanClassLoader(...);
        if (bean instanceof BeanFactoryAware) setBeanFactory(...);
    }

    // ② 初始化前
    Object wrappedBean = applyBeanPostProcessorsBeforeInitialization(bean, beanName);
    //     @PostConstruct / InitializingBean / init-method 之前的钩子

    // ③ 执行初始化方法
    invokeInitMethods(beanName, wrappedBean, mbd);
    //     a) InitializingBean.afterPropertiesSet()
    //     b) 自定义 init-method

    // ④ 初始化后（AOP 代理诞生地）
    wrappedBean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
    //     AbstractAutoProxyCreator 在此生成代理

    return wrappedBean;
}
```

### 4.2 初始化方法执行顺序（高频面试）

```text
① @PostConstruct 注解方法（CommonAnnotationBeanPostProcessor）
② InitializingBean.afterPropertiesSet()
③ 自定义 init-method（@Bean(initMethod)/XML init-method）
```

> ⚠️ 注意：@PostConstruct 其实是在「初始化前」的后置处理器里执行的（postProcessBeforeInitialization），所以顺序是 @PostConstruct → afterPropertiesSet → init-method。

### 4.3 AOP 代理在初始化后的原因

```text
为什么代理在 initializeBean 最后？
  需要「目标对象完整初始化后」才能包装代理
  （否则代理调用的还是未初始化对象）
但循环依赖时提前暴露的原始对象必须提前代理
  → 三级缓存的 ObjectFactory 里调用 getEarlyBeanReference
  → SmartInstantiationAwareBeanPostProcessor 提前创建代理
  → 初始化后 detect 已代理 → 不再二次代理
```

---

## 5. 销毁流程

### 5.1 销毁顺序

```text
容器关闭（close()）→ doClose
  ↓
发布 ContextClosedEvent
  ↓
销毁单例（destroySingletons）
  ↓
每个 bean 销毁顺序（与创建顺序相反）：
  ① @PreDestroy 注解方法
  ② DisposableBean.destroy()
  ③ 自定义 destroy-method
```

### 5.2 触发时机

| 场景 | 销毁触发 |
|------|------|
| Spring Boot 优雅停机 | 应用退出时容器 close |
| web 应用关闭 | Servlet 容器销毁钩子 |
| 手动 close | `((ConfigurableApplicationContext) ctx).close()` |
| 原型 bean | **不随容器销毁**（容器只管理单例销毁） |

> 💡 原型 bean 的销毁责任在调用方：容器创建后不跟踪原型实例，需自行释放资源。

---

## 6. BeanPostProcessor 扩展点全景

### 6.1 接口家族

| 接口 | 钩子方法 | 典型实现 | 时机 |
|------|------|------|------|
| BeanPostProcessor | before/afterInitialization | 各类初始化钩子 | 初始化前后 |
| InstantiationAwareBeanPostProcessor | before/afterInstantiation、postProcessProperties | AutowiredAnnotationBeanPostProcessor | 实例化 + 填充 |
| SmartInstantiationAwareBeanPostProcessor | getEarlyBeanReference、determineCandidateConstructors | AbstractAutoProxyCreator、AutowiredAnnotationBeanPostProcessor | 循环依赖提前暴露 |
| DestructionAwareBeanPostProcessor | postProcessBeforeDestruction | 各类销毁钩子 | 销毁前 |
| MergedBeanDefinitionPostProcessor | postProcessMergedBeanDefinition | 注入点收集（6.1 改名） | 定义合并后 |

### 6.2 内置后置处理器一览（refresh ⑥ 注册）

| 后置处理器 | 职责 |
|------|------|
| AutowiredAnnotationBeanPostProcessor | @Autowired/@Value |
| CommonAnnotationBeanPostProcessor | @Resource/@PostConstruct/@PreDestroy |
| AnnotationAwareAspectJAutoProxyCreator | AOP 代理（@EnableAspectJAutoProxy 注册） |
| ApplicationListenerDetector | 检测监听器 bean |
| ScheduledAnnotationBeanPostProcessor | @Scheduled（Spring Task） |
| AsyncAnnotationBeanPostProcessor | @Async 代理（Spring Task） |

### 6.3 自定义扩展点选择（面试场景题）

```text
需求：给所有 bean 初始化后打日志
  → BeanPostProcessor.postProcessAfterInitialization

需求：在 bean 实例化前拦截（如 mock 特定 bean）
  → InstantiationAwareBeanPostProcessor.postProcessBeforeInstantiation

需求：修改 BeanDefinition（全局加属性）
  → BeanFactoryPostProcessor（⑤ 步）

需求：拦截销毁（资源释放）
  → DestructionAwareBeanPostProcessor
```

---

## 7. 生命周期时序图（面试手绘版）

```text
实例化 ──► 填充 ──► Aware ──► 初始化前 ──► 初始化 ──► 初始化后 ──► 使用 ──► 销毁
  │         │         │          │            │            │                 │
构造器     @Autowired  BeanName   @PostConstruct  after      AOP 代理       @PreDestroy
/工厂      注入        Aware      (BPP before)  Properties  (BPP after)    (销毁 BPP)
方法                       │                          (init-         │
                           ▼                          method)        ▼
                     BeanFactoryAware                       DisposableBean
                     /ApplicationContextAware               /destroy-method
```

### 7.1 面试手绘口诀

```text
「实例化、填充、Aware、初始化前、初始化、初始化后、销毁」
七个节点 + 三个「钩子家族」：
  实例化钩子（InstantiationAware）
  初始化钩子（BeanPostProcessor）
  销毁钩子（DestructionAware）
AOP 代理在「初始化后」；循环依赖的提前代理在「填充时」
```

> 🎯 **核心要点**：doCreateBean = **实例化（构造器/工厂）→ 填充（@Autowired）→ 初始化（Aware → 初始化前 → 初始化 → 初始化后/代理）**，销毁反向执行。面试手绘这条链 + 标注三个扩展点家族（InstantiationAware/BPP/DestructionAware）即为满分。两个细节加分：@PostConstruct 在「初始化前」钩子执行（顺序 #1）；AOP 代理在初始化后（除循环依赖提前代理）。

---

**下一模块**：[05-循环依赖与三级缓存源码解析](05-循环依赖与三级缓存源码解析.md) | **返回总览**：[00-SpringCore专题总览](00-SpringCore专题总览.md)
