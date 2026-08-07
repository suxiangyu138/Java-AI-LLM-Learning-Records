# BeanDefinition 注册与解析机制
> 一切 bean 的起点是「定义」——BeanDefinition 描述类、作用域、依赖与初始化方式。本文件拆解 BeanDefinition 结构、三种注册途径与 BeanFactoryPostProcessor 的修改时机

## 目录
1. [BeanDefinition 是什么](#1-beandefinition-是什么)
2. [BeanDefinition 核心属性](#2-beandefinition-核心属性)
3. [三种注册途径](#3-三种注册途径)
4. [注解解析核心：ConfigurationClassPostProcessor](#4-注解解析核心configurationclasspostprocessor)
5. [@ComponentScan 扫描机制](#5-componentscan-扫描机制)
6. [BeanFactoryPostProcessor：修改定义的时机](#6-beanfactorypostprocessor修改定义的时机)
7. [BeanRegistrar：Spring 7.0 模块化注册（2026）](#7-beanregistrarspring-70-模块化注册2026)

---

## 1. BeanDefinition 是什么

```text
BeanDefinition = bean 的「图纸/元数据」
  类名 → 实例化依据
  作用域 → 单例/原型
  依赖 → 注入依据
  初始化/销毁方法 → 生命周期回调
  懒加载 → 创建时机
```

| 概念 | 类比 |
|------|------|
| BeanDefinition | 建筑设计图 |
| BeanFactory 注册表 | 图纸档案室 |
| Bean 实例 | 建好的房子 |
| BeanPostProcessor | 装修队（入住前/后） |

> 🎯 核心认知：**Spring 容器先注册「定义」，再按定义创建「实例」**——refresh() 的 ⑤ 是「改图纸」阶段，⑪ 是「按图纸施工」阶段。

---

## 2. BeanDefinition 核心属性

### 2.1 属性速查

| 属性 | 含义 | 示例 |
|------|------|------|
| `beanClassName` | 全限定类名 | com.demo.UserService |
| `scope` | 作用域 | singleton / prototype |
| `lazyInit` | 是否懒加载 | true |
| `autowireMode` | 自动装配模式 | AUTOWIRE_BY_TYPE |
| `dependencyCheck` | 依赖检查 | 默认关闭 |
| `initMethodName` | 初始化方法 | init() |
| `destroyMethodName` | 销毁方法 | destroy() |
| `factoryBeanName` | 工厂 bean | 由 FactoryBean 创建时 |
| `constructorArgumentValues` | 构造器参数 | 有参构造 |
| `propertyValues` | 属性值 | setter 注入 |
| `primary` | 首选 bean | @Primary |
| `synthetic` | 框架内部定义 | 由框架生成 |

### 2.2 接口与实现

```java
public interface BeanDefinition extends AttributeAccessor, BeanMetadataElement {
    String SCOPE_SINGLETON = "singleton";
    String SCOPE_PROTOTYPE = "prototype";
    // 大量 getter/setter...
}
// 常用实现：GenericBeanDefinition（通用）、RootBeanDefinition（父/顶层）、
//          AnnotatedGenericBeanDefinition（注解类）、ScannedGenericBeanDefinition（扫描）

// Spring 6.2+/7.x：lambda 注册 BeanDefinition（编程式更简洁）
BeanDefinition bd = BeanDefinitionBuilder
        .genericBeanDefinition(UserService.class)
        .setScope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
        .setInitMethodName("init")
        .getBeanDefinition();
```

---

## 3. 三种注册途径

### 3.1 途径对比

| 途径 | 入口 | 解析者 | 适用 |
|------|------|------|------|
| XML | `<bean>` / `<context:component-scan>` | XmlBeanDefinitionReader | 老项目/框架 |
| 注解 | @Component/@Bean/@Import | ConfigurationClassPostProcessor | **现代默认** |
| 编程式 | registerBeanDefinition / register | 手写注册 | 动态注册、框架集成 |

### 3.2 注解注册的三条分支

```text
@Configuration 配置类解析（ConfigurationClassParser）：
  ├─ @ComponentScan → 扫描包 → ScannedGenericBeanDefinition
  ├─ @Bean 方法 → ConfigurationClassBeanDefinition（工厂方法定义）
  └─ @Import → 再递归解析（普通类/ImportSelector/ImportBeanDefinitionRegistrar）

@Configuration 类本身：注册为「配置 bean」（会被 CGLIB 代理，见 06 篇）
```

### 3.3 编程式注册示例

```java
// 动态注册（框架集成场景：如动态数据源）
DefaultListableBeanFactory factory = (DefaultListableBeanFactory) ctx.getAutowireCapableBeanFactory();
factory.registerBeanDefinition("myService", bd);

// 或使用 BeanDefinitionRegistryPostProcessor 在启动时注册
@Component
public class MyRegistrar implements BeanDefinitionRegistryPostProcessor {
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        RootBeanDefinition bd = new RootBeanDefinition(MyService.class);
        bd.setScope(BeanDefinition.SCOPE_SINGLETON);
        registry.registerBeanDefinition("myService", bd);
    }
}
```

---

## 4. 注解解析核心：ConfigurationClassPostProcessor

### 4.1 为什么它是「注解体系的发动机」

```text
它是 BeanDefinitionRegistryPostProcessor（⑤ 步第一个执行）
职责：
  ① 解析 @Configuration 类
  ② 处理 @ComponentScan → 扫描
  ③ 处理 @Import（含 ImportSelector/Registrar）
  ④ 处理 @Bean 方法 → 注册为工厂方法 BeanDefinition
  ⑤ 解析 @PropertySource → 属性源注册

顺序保证：ConfigurationClassPostProcessor 在 ⑤ 的最前面执行
（refresh ⑤ 中它总是第一个被调用）
```

### 4.2 解析流程

```text
parse(configClass)
  ├─ 处理 @PropertySource
  ├─ 处理 @ComponentScan → doScan → 注册扫描到的 BeanDefinition
  ├─ 处理 @Import → 递归
  ├─ 处理 @ImportResource（XML 引入）
  ├─ 处理 @Bean 方法
  └─ 处理接口 default 方法 / 父类（完整配置类链）
```

> 🎯 面试要点：**@ComponentScan 本质也是「注册 BeanDefinition」**——扫描器（ClassPathBeanDefinitionScanner）把包下所有被 @Component 系注解标记的类注册进容器，之后和其他定义一样走生命周期。

---

## 5. @ComponentScan 扫描机制

### 5.1 扫描过滤器（TypeFilter）

```java
@Configuration
@ComponentScan(
    basePackages = "com.demo",
    includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = Service.class),
    excludeFilters = @Filter(type = FilterType.REGEX, pattern = ".*Test.*")
)
```

| FilterType | 匹配方式 | 用途 |
|------|------|------|
| ANNOTATION | 注解 | 默认（@Component/@Service/...） |
| ASSIGNABLE_TYPE | 类型可赋值 | 接口/父类 |
| REGEX | 类名正则 | 排除测试类 |
| ASPECTJ | AspectJ 表达式 | 复杂规则 |
| CUSTOM | 自定义 TypeFilter | 特殊逻辑 |

### 5.2 默认包含的注解

```text
@Component 系（元注解机制）：
  @Component → @Service / @Repository / @Controller / @RestController / @Configuration
@Configuration 本身也在扫描范围内（嵌套配置类）

原理：候选组件注解 = 元注解上标注了 @Component 的注解集合
（Spring 用候选组件索引 + 元数据读取器判断）
```

---

## 6. BeanFactoryPostProcessor：修改定义的时机

### 6.1 作用与时机

```text
时机：refresh ⑤ —— BeanDefinition 已注册、bean 实例未创建
作用：修改/新增/删除 BeanDefinition

接口家族：
  BeanFactoryPostProcessor             → postProcessBeanFactory（改容器）
  BeanDefinitionRegistryPostProcessor  → postProcessBeanDefinitionRegistry（改注册表）
        ↑ 继承自 BeanFactoryPostProcessor，先执行 registry 方法
```

### 6.2 经典实现

| 实现 | 干了什么 |
|------|------|
| ConfigurationClassPostProcessor | 注解解析总入口 |
| PropertySourcesPlaceholderConfigurer | @Value 占位符 ${} 解析（修改 bean 定义中的占位符） |
| EventListenerMethodProcessor | 扫描 @EventListener 方法 |
| 自定义 Registrar | 动态注册 bean（如 mybatis-spring 的 MapperScannerConfigurer） |

```java
// 自定义场景：动态修改 bean 定义（如全局加属性）
@Component
public class GlobalConfigPostProcessor implements BeanFactoryPostProcessor {
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory factory) {
        BeanDefinition bd = factory.getBeanDefinition("userService");
        bd.getPropertyValues().add("environment", "prod");
    }
}
```

> ⚠️ 与 BeanPostProcessor 的区别（高频面试）：**BeanFactoryPostProcessor 改「定义」（bean 还没创建），BeanPostProcessor 改「实例」（bean 创建后）**——一个是图纸阶段，一个是施工阶段。

---

## 7. BeanRegistrar：Spring 7.0 模块化注册（2026）

### 7.1 解决什么问题

```text
痛点：大应用通过大量 @Conditional + 手动注册管理 bean，配置爆炸
方案：BeanRegistrar —— 模块化的批量注册器
  · 按模块组织注册逻辑（契合 Spring Modulith）
  · 替代大量 @Conditional 组合
  · 批量注册共享配置的 bean
```

### 7.2 使用形态

```java
// Spring 7.0 新接口（示意）
public interface BeanRegistrar {
    void registerBeans(BeanRegistrarContext context);
}
// 或注解驱动：@EnableXxx(registers = MyRegistrar.class)

// 与 @Import(ImportBeanDefinitionRegistrar) 的关系：
// 升级版——更模块化、面向批量场景；旧接口仍兼容
```

### 7.3 面试表述

```text
「Spring 7.0 引入 BeanRegistrar 支持模块化批量 bean 注册，
  配合 Modulith 拆分大容器，减少 @Conditional 爆炸；
  同时 @Fallback 提供降级 bean、容器支持并行初始化。
  核心注册机制（BeanDefinition 注册表 + 后置处理器）不变。」
```

> 🎯 **核心要点**：BeanDefinition 机制一句话 = **「先注册图纸（三种途径），⑤ 步可改图（BeanFactoryPostProcessor），⑪ 步按图施工（创建实例）」**。记忆锚点：注解解析的发动机是 ConfigurationClassPostProcessor（⑤ 第一个执行）；@ComponentScan 本质是扫描注册器；BeanFactoryPostProcessor 改定义 vs BeanPostProcessor 改实例。Spring 7.0 的 BeanRegistrar 是注册机制的模块化升级，底层注册表机制不变。

---

**下一模块**：[03-依赖注入底层原理](03-依赖注入底层原理.md) | **返回总览**：[00-SpringCore专题总览](00-SpringCore专题总览.md)
