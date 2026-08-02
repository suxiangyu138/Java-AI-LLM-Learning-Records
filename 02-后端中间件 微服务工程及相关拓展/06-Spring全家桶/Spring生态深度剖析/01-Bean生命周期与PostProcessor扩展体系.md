# 01 - Bean 生命周期与 PostProcessor 扩展体系

> 🎯 Spring 扩展机制的基石 — 两大基石（BeanFactoryPostProcessor / BeanPostProcessor）、五大扩展点、完整生命周期时序。理解每个阶段"允许干什么、禁止干什么"

---

## 目录

1. [扩展机制总览](#1-扩展机制总览)
2. [两大基石：BFPP vs BPP](#2-两大基石bfpp-vs-bpp)
3. [BeanDefinition 阶段扩展点](#3-beandefinition-阶段扩展点)
4. [实例化阶段扩展点](#4-实例化阶段扩展点)
5. [初始化阶段扩展点（BPP 核心）](#5-初始化阶段扩展点bpp-核心)
6. [完整生命周期时序图](#6-完整生命周期时序图)
7. [四大内置处理器源码定位](#7-四大内置处理器源码定位)
8. [常见陷阱与调试方法](#8-常见陷阱与调试方法)

---

## 1. 扩展机制总览

```text
Spring 扩展 = 把"创建 Bean"大流程拆成多个阶段，每个阶段提供可插拔扩展点

阶段划分（按时间线）：
① BeanDefinition 阶段（"图纸"）→ BeanFactoryPostProcessor
② 实例化前（"选材"）→ InstantiationAwareBeanPostProcessor#beforeInstantiation
③ 实例化（"造胚"）→ createBeanInstance（无扩展）
④ 实例化后/属性填充前（"加工预备"）→ InstantiationAware#postProcessAfterInstantiation
⑤ 属性填充（"组装"）→ InstantiationAware#postProcessProperties（@Autowired 在此）
⑥ 初始化前 → BeanPostProcessor#beforeInitialization
⑦ 初始化（@PostConstruct → afterPropertiesSet → init-method）
⑧ 初始化后 → BeanPostProcessor#afterInitialization（⚠️ AOP 代理在此创建）
⑨ 销毁前 → DestructionAwareBeanPostProcessor#postProcessBeforeDestruction
```

---

## 2. 两大基石：BFPP vs BPP

```text
                     ┌─ 操作对象 ─┬─ 时机 ─────┬─ 调用次数 ─┬─ 核心落地 ────┐
BeanFactoryPostProcessor BeanDefinition  Bean 创建前   全局 1 次    PropertySources
BeanPostProcessor        Bean 实例       每个 Bean 创建  每个 Bean    AOP / @Autowired

一句话：BFPP 改"图纸"（BeanDefinition），BPP 改"成品"（Bean 实例）
BFPP 中千万别调 getBean() → 会破坏生命周期、触发提前初始化
```

### 2.1 BeanPostProcessor 接口定义

```java
// 仅两个默认方法 — Spring 扩展机制"极简接口 + 强大行为"的典范
public interface BeanPostProcessor {
    // 初始化前：此时 Bean 已实例化 + 属性已填充，但 @PostConstruct 未执行
    @Nullable
    default Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }
    // 初始化后：@PostConstruct/afterPropertiesSet/init-method 全部完成
    // ⚠️ AOP 代理正是在这个方法中被创建（AbstractAutoProxyCreator）
    @Nullable
    default Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }
}
```

### 2.2 核心执行位置：initializeBean()

```java
// AbstractAutowireCapableBeanFactory.java（Spring 源码关键入口）
protected Object initializeBean(String beanName, Object bean, @Nullable RootBeanDefinition mbd) {
    invokeAwareMethods(beanName, bean);                         // BeanNameAware 等
    Object wrappedBean = bean;
    if (mbd == null || !mbd.isSynthetic()) {
        wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
    }
    invokeInitMethods(beanName, wrappedBean, mbd);              // @PostConstruct 等
    if (mbd == null || !mbd.isSynthetic()) {
        wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
    }
    return wrappedBean;  // ← 可能已经被代理包装
}
```

---

## 3. BeanDefinition 阶段扩展点

### 3.1 BeanDefinitionRegistryPostProcessor

**时机最早** — 在所有 BFPP 之前执行。可**新增 BeanDefinition**（如 MapperScannerConfigurer 扫描 MyBatis Mapper）。

```java
// 经典落地：ConfigurationClassPostProcessor 解析 @Configuration/@ComponentScan/@Import/@Bean
// 它在 PriorityOrdered 中最先执行，是 Spring 注解体系的总引擎
public interface BeanDefinitionRegistryPostProcessor extends BeanFactoryPostProcessor {
    void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry);
}
```

### 3.2 BeanFactoryPostProcessor

**修改已有 BeanDefinition** — 属性值、作用域、依赖替换。

```java
// 经典落地：PropertySourcesPlaceholderConfigurer 解析 ${} 占位符
// 调用链：refresh() → invokeBeanFactoryPostProcessors() → postProcessBeanFactory()
// ⚠️ 严禁此时调 getBean()（会触发提前实例化，破坏依赖顺序）
```

**执行顺序**（refresh 第 5 步 `invokeBeanFactoryPostProcessors`）：
1. BDRPP：PriorityOrdered → Ordered → 其他（含 postProcessBeanFactory）
2. BFPP：PriorityOrdered → Ordered → 其他

---

## 4. 实例化阶段扩展点

### 4.1 InstantiationAwareBeanPostProcessor

继承 BPP，增加**实例化级拦截能力**：

```java
// ① 实例化前：可返回代理对象，短路正常创建流程（AOP 关键入口）
//    返回非 null → 后续实例化跳过、直接进入初始化后 BPP
Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName);

// ② 实例化后：返回 false → 跳过属性填充
boolean postProcessAfterInstantiation(Object bean, String beanName);

// ③ 属性填充核心：@Autowired 依赖注入的实际执行点
//    AutowiredAnnotationBeanPostProcessor 重写此方法完成注入
PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName);
```

### 4.2 SmartInstantiationAwareBeanPostProcessor

进一步增加**预测能力** — 提前暴露代理的基石：

```java
// 预测 Bean 类型（FactoryBean 等场景）
Class<?> predictBeanType(Class<?> beanClass, String beanName);
// 提供候选构造器（@Autowired 构造器选择）
Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName);
// ⚠️ 提前暴露代理对象 — 三级缓存 getEarlyBeanReference 回调的核心
Object getEarlyBeanReference(Object bean, String beanName);
```

---

## 5. 初始化阶段扩展点（BPP 核心）

### 5.1 标准 BPP 执行位置

```
initializeBean() 执行顺序（不可改变）：
┌─────────────────────────────────────────────┐
│ ① applyBeanPostProcessorsBeforeInitialization │ ← 第 1 组 BPP
│ ② invokeInitMethods                           │ ← @PostConstruct → afterPropertiesSet → init-method
│ ③ applyBeanPostProcessorsAfterInitialization  │ ← 第 2 组 BPP（AOP 代理在此！）
└─────────────────────────────────────────────┘
```

### 5.2 MergedBeanDefinitionPostProcessor

```java
// 合并 BeanDefinition 后触发 — postProcessMergedBeanDefinition
// 经典落地：AutowiredAnnotationBeanPostProcessor 在此缓存需要注入的元数据
//           CommonAnnotationBeanPostProcessor 缓存 @PostConstruct/@PreDestroy
```

### 5.3 DestructionAwareBeanPostProcessor

```java
// Bean 销毁前执行（@PreDestroy → DisposableBean.destroy → destroy-method）
// 保证生命周期完整闭环
void postProcessBeforeDestruction(Object bean, String beanName);
```

---

## 6. 完整生命周期时序图

```text
┌─ refresh() 第 5 步 ────────────────────────────────┐
│ invokeBeanFactoryPostProcessors(beanFactory)          │
│   ① BeanDefinitionRegistryPostProcessor#postProcess..  │  ← 注册新 BeanDefinition
│   ② BeanFactoryPostProcessor#postProcessBeanFactory    │  ← 修改 BeanDefinition
├─ refresh() 第 6 步 ────────────────────────────────┤
│ registerBeanPostProcessors(beanFactory)               │  ← 注册所有 BPP
├─ refresh() 第 11 步 ───────────────────────────────┤
│ finishBeanFactoryInitialization → preInstantiateSingletons │
│                                                        │
│ 对每个单例 Bean：                                       │
│ ┌─────────────────────────────────────────────────┐   │
│ │ ③ InstantiationAwareBPP#beforeInstantiation     │   │ ← 可短路
│ │ ④ createBeanInstance（反射/构造器）              │   │ ← 实例化
│ │ ⑤ MergedBeanDefinitionBPP#postProcessMerged     │   │ ← 缓存注入元数据
│ │ ⑥ addSingletonFactory（⚠️ 三级缓存入口）        │   │ ← 提前暴露
│ │ ⑦ InstantiationAwareBPP#afterInstantiation      │   │ ← 可跳过属性填充
│ │ ⑧ InstantiationAwareBPP#postProcessProperties   │   │ ← @Autowired 注入
│ │ ⑨ 属性填充完成                                      │   │
│ │ ⑩ BPP#beforeInitialization                     │   │
│ │ ⑪ invokeInitMethods（初始化三步）                  │   │
│ │ ⑫ BPP#afterInitialization（⚠️ AOP 代理创建）   │   │
│ │ ⑬ addSingleton → 一级缓存                      │   │ ← 成品入库
│ └─────────────────────────────────────────────────┘   │
├─ 销毁时 ────────────────────────────────────────────┤
│ ⑭ DestructionAwareBPP#postProcessBeforeDestruction    │
└──────────────────────────────────────────────────────┘
```

---

## 7. 四大内置处理器源码定位

| 处理器 | 源码类 | 核心功能 |
|--------|--------|----------|
| **AutowiredAnnotationBeanPostProcessor** | `org.springframework.beans.factory.annotation` | 解析 @Autowired/@Value，`postProcessProperties` 完成注入 |
| **CommonAnnotationBeanPostProcessor** | `org.springframework.context.annotation` | 解析 @Resource/@PostConstruct/@PreDestroy |
| **AnnotationAwareAspectJAutoProxyCreator** | `org.springframework.aop.aspectj.annotation` | 继承 AbstractAutoProxyCreator → afterInitialization 中 `wrapIfNecessary` 创建代理 |
| **ConfigurationClassPostProcessor** | `org.springframework.context.annotation` | BDRPP 的实现 → 解析 @Configuration/@ComponentScan/@Import/@Bean |

---

## 8. 常见陷阱与调试方法

### 8.1 陷阱清单

| 陷阱 | 原因 | 对策 |
|------|------|------|
| BPP 注册时 Bean 被提前初始化 | @Configuration 中非静态 @Bean 返回 BPP | **声明 static** 或用 @Component |
| "not eligible for auto-proxying" 日志 | BPP 自身不参与代理（鸡生蛋问题） | 正常，无需处理 |
| @Autowired 注入的 Bean 值与代码预期不同 | postProcessAfterInstantiation 返回 false 跳过填充 | 排查该 BPP 的实现 |
| AOP 失效（自调用） | this.xxx() 绕过代理 | 注入自身 / AopContext.currentProxy() |
| 构造器注入循环依赖 | 实例化前就需要完整依赖 | 改用 Setter/@Lazy/重构 |

### 8.2 调试命令

```java
// 打印所有已注册的 BeanPostProcessor
for (String name : ctx.getBeanNamesForType(BeanPostProcessor.class)) {
    System.out.println(name + " → " + ctx.getBean(name).getClass().getName());
}
// 断点关键方法：
// AbstractAutowireCapableBeanFactory#initializeBean（BPP 执行入口）
// AbstractAutoProxyCreator#postProcessAfterInitialization（AOP 代理创建）
// AutowiredAnnotationBeanPostProcessor#postProcessProperties（@Autowired 注入）
```

---

> 🎯 **核心要点**：BPP 体系 = "改成品"，BFPP 体系 = "改图纸"。能画整张时序图 + 能解释"为什么 @Autowired 在 postProcessProperties 而不是在 afterInitialization" + 能指出 AOP 代理在 afterInitialization 创建 — 就是 PostProcessor 体系的面试满分答案。

**下一模块**：[02-SpringBoot启动流程与自动配置内核](02-SpringBoot启动流程与自动配置内核.md) / **返回总览**：[00-深度剖析总览](00-Spring生态深度剖析总览.md)
