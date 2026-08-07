# 02 ApplicationContext 体系速查

> 接口层次、refresh() 启动流程、常见实现与 getBean 全链路——"企业级容器"的骨架速查

---

## 📚 目录

1. [接口层次](#1-接口层次)
2. [refresh() 十二步启动流程](#2-refresh-十二步启动流程)
3. [常见实现对照](#3-常见实现对照)
4. [getBean 查找链路](#4-getbean-查找链路)
5. [ApplicationContext 与 BeanFactory 的本质差异](#5-applicationcontext-与-beanfactory-的本质差异)

---

## 1. 接口层次

```text
ApplicationContext（多接口合并的面具）
├── ListableBeanFactory        → 批量枚举 Bean（来自 beans）
├── HierarchicalBeanFactory    → 父子容器（来自 beans）
├── EnvironmentCapable         → 暴露 Environment
├── MessageSource              → 国际化
├── ApplicationEventPublisher  → 事件发布
└── ResourcePatternResolver    → 模式资源加载（classpath*:）

ConfigurableApplicationContext（可配置：refresh/close/registerShutdownHook）
└── AbstractApplicationContext（★ 模板方法：refresh()/close() 骨架）
      └── GenericApplicationContext / AnnotationConfigApplicationContext / ...
```

**接口职责表**：

| 接口 | 关键方法 | 场景 |
|------|---------|------|
| `ApplicationContext` | `getBean` / `getBeansOfType` / `publishEvent` / `getMessage` / `getResource` | 业务代码唯一需要的类型 |
| `ConfigurableApplicationContext` | `refresh()` / `close()` / `registerShutdownHook()` / `addApplicationListener` | 启动/关闭/注册监听 |
| `AbstractApplicationContext` | refresh() 模板实现 | 自定义容器继承点（少见） |
| `MessageSource` / `ApplicationEventPublisher` | `getMessage(code,args,locale)` / `publishEvent(obj)` | 国际化、事件 |

> 🎯 **核心要点**：`ApplicationContext` 是**多接口合并**的复合契约——它能当 BeanFactory 用（`getBean`）、能发布事件、能取消息、能加载资源，全因接口继承；容器关闭时 `registerShutdownHook()` 保证 JVM 退出前执行 destroy。

## 2. refresh() 十二步启动流程

```text
refresh() 核心步骤（AbstractApplicationContext，面试背诵版）：
  1  prepareRefresh()          准备刷新：启动时间、早期事件集、属性源初始化
  2  obtainFreshBeanFactory()  创建/刷新 BeanFactory（DefaultListableBeanFactory）
  3  prepareBeanFactory()      设置类加载器、SpEL 解析器、注册 Aware 处理器等
  4  postProcessBeanFactory()  子类扩展钩子（Web 容器在此注册特殊作用域）
  5  invokeBeanFactoryPostProcessors()  ★ 执行 BeanFactoryPostProcessor
       （ConfigurationClassPostProcessor 在此处理 @Configuration/@ComponentScan/@Import）
  6  registerBeanPostProcessors()  注册 BeanPostProcessor（不执行，先注册）
  7  initMessageSource()       初始化国际化 MessageSource
  8  initApplicationEventMulticaster()  初始化事件多播器
  9  onRefresh()               子类扩展钩子（Web 容器创建内嵌服务器）
  10 registerListeners()       注册静态监听器 + 早期事件补发
  11 finishBeanFactoryInstantiation()  实例化全部非懒加载单例 ★
  12 finishRefresh()           完成：发布 ContextRefreshedEvent、启动 Lifecycle
```

> 💡 **记忆锚点**：5-6 步 = "改图纸、招工人"（PostProcessor）；7-10 = "后勤就位"（消息/事件）；11 = "开工"（单例实例化）；12 = "剪彩"（ContextRefreshedEvent）。Boot 自动配置跑在 5 步之后（`AutoConfigurationImportSelector` 通过 `@Import` 参与配置类处理）。

## 3. 常见实现对照

| 实现 | 配置来源 | 场景 |
|------|---------|------|
| `AnnotationConfigApplicationContext` | 注解 + 配置类 | ✅ 现代标准（独立/嵌入式应用） |
| `GenericApplicationContext` | 编程式 `registerBean`/`registerBeanDefinition` | ✅ 工具型/动态容器 |
| `ClassPathXmlApplicationContext` | XML 文件 | 存量系统 |
| `FileSystemXmlApplicationContext` | 文件系统 XML | 存量 |
| `AnnotationConfigServletWebServerApplicationContext` | 注解 | Boot Web 应用（**Boot 内部使用**） |
| `StaticApplicationContext` | 无（纯编程） | 测试 |

```java
// 现代最常用入口
try (AnnotationConfigApplicationContext ctx =
             new AnnotationConfigApplicationContext(AppConfig.class)) {
    ctx.getBean(OrderService.class).create(...);
}   // try-with-resources 自动 close → 触发 destroy 回调

// 编程式注册（BeanRegistrar 时代的原生起点）
GenericApplicationContext ctx = new GenericApplicationContext();
ctx.registerBean(OrderService.class);
ctx.refresh();   // 手动刷新后才可用
```

> ⚠️ **GenericApplicationContext 必须手动 refresh()**——它没有自动刷新的构造器；`AnnotationConfigApplicationContext` 构造后自动 refresh。

## 4. getBean 查找链路

```text
ctx.getBean(OrderService.class)
  → AbstractApplicationContext.getBean → 委派给 getBeanFactory()
  → AbstractBeanFactory.doGetBean
      1. 查单例缓存（三级缓存：完成的/早期/工厂）
      2. 未命中 → 检查父容器 → 查 BeanDefinition
      3. 是 FactoryBean？取 getObject() 还是工厂本身（&name）
      4. 创建：实例化 → 属性填充 → 初始化 → 代理（详见 Spring-Beans 系列）
      5. 放入单例缓存
```

> 🎯 **核心要点**：ApplicationContext 的 getBean 本质是**委托给内部持有的 DefaultListableBeanFactory**——"上下文 = 工厂 + 企业能力"的心智模型再次印证；作用域、代理、生命周期全在 beans 层完成，context 只负责编排。

## 5. ApplicationContext 与 BeanFactory 的本质差异

| 维度 | BeanFactory | ApplicationContext |
|------|-------------|-------------------|
| 加载策略 | 懒加载（getBean 时才创建） | **预加载**（refresh 时创建全部单例） |
| 事件/国际化/属性 | ❌ | ✅ |
| 配置类处理 | 无（需手动接 `ConfigurationClassPostProcessor`） | ✅ 内建 |
| 资源模式加载 | 无 | ✅ |
| 扩展钩子 | BeanPostProcessor 可注册 | refresh() 十二步 + `onRefresh`/`postProcessBeanFactory` |
| 使用场景 | 嵌入式/工具型 IoC | 一切应用容器 |

> ⚠️ 面试高频辨析："BeanFactory 是懒加载"——准确说是"按需创建"；`getBean` 懒与预加载取决于容器类型，而非绝对。

---

**下一模块**：[03-配置类与扫描机制速查](03-配置类与扫描机制速查.md)　**返回总览**：[00-Spring Context组件总览](00-Spring Context组件总览.md)
