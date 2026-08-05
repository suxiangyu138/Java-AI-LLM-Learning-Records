# 06 - 事件驱动模型与 ApplicationContext 内部机制

> 🎯 refresh 12 步是 ApplicationContext 的"发动机" — 从资源加载到单例预实例化，12 个步骤严格有序。事件广播器是 Spring 内通信的总线，理解它才能理解微服务组件的启动时序

---

## 目录

1. [refresh 12 步全景](#1-refresh-12-步全景)
2. [Spring 事件体系](#2-spring-事件体系)
3. [ApplicationContext 的层次与生命周期](#3-applicationcontext-的层次与生命周期)
4. [微服务组件如何利用事件与生命周期](#4-微服务组件如何利用事件与生命周期)
5. [常见问题：事件不触发与 Context 层级](#5-常见问题事件不触发与-context-层级)

---

## 1. refresh 12 步全景

```java
// AbstractApplicationContext#refresh() — Spring 容器初始化最核心的方法
// 所谓"Spring 启动"的时刻，就是这个方法完整执行完成的时刻

public void refresh() throws BeansException, IllegalStateException {
    synchronized (this.startupShutdownMonitor) {              // ← StartupStep 监控锁

// ===== 步骤 1：准备刷新 — 设置启动时间、激活状态、验证必要属性 =====
    prepareRefresh();
    // ① 设置 closed=false, active=true
    // ② 初始化 PropertySources（占位符来源）
    // ③ 校验所有标记为 "required" 的 Environment 属性

// ===== 步骤 2：获取 BeanFactory — 创建/加载 DefaultListableBeanFactory =====
    ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();
    // ① 如果是可刷新 Context → 销毁已有 BeanFactory、创建新的（正常只需一次）
    // ② 加载所有 BeanDefinition（XML 或注解扫描）

// ===== 步骤 3：准备 BeanFactory — 配置标准特性 =====
    prepareBeanFactory(beanFactory);
    // ① 设置 ClassLoader（当前线程的 ContextClassLoader）
    // ② 注册三个默认的 BPP：
    //    ApplicationContextAwareProcessor（注入各种 Aware）
    //    ApplicationListenerDetector（监听器检测）
    //    LoadTimeWeaverAwareProcessor
    // ③ 注册默认 Environment Bean（environment/systemProperties/systemEnvironment）

// ===== 步骤 4：后置处理 BeanFactory — 留给子类扩展（空实现）=====
    postProcessBeanFactory(beanFactory);
    // 典型子类重写：ServletWebServerApplicationContext 在此注册 Web 相关 Scope
    //             （request/session/application）

// ===== 步骤 5：★ 调用 BFPP — 自动配置类在此被处理 =====
    invokeBeanFactoryPostProcessors(beanFactory);
    // ① 先 BDRPP（如 ConfigurationClassPostProcessor 解析 @Configuration/@ComponentScan/@Import）
    // ② 再 BFPP（如 PropertySourcesPlaceholderConfigurer 替换 ${} 占位符）
    // ★ 自动配置：@EnableAutoConfiguration → AutoConfigurationImportSelector → 在此执行

// ===== 步骤 6：★ 注册 BPP — 为后续 Bean 创建准备好所有后置处理器 =====
    registerBeanPostProcessors(beanFactory);
    // 对 BeanPostProcessor 实例化 + 排序：
    //   PriorityOrdered → Ordered → 其他 → MergedBeanDefinitionBPP
    // ApplicationListenerDetector 最后注册（保证所有 BPP 都被它检测）

// ===== 步骤 7：初始化 MessageSource — 国际化 =====
    initMessageSource();

// ===== 步骤 8：★ 初始化事件广播器 — 默认 SimpleApplicationEventMulticaster =====
    initApplicationEventMulticaster();
    // DispatcherServlet#initStrategies 也在此之后执行

// ===== 步骤 9：onRefresh — 留给子类扩展 =====
    onRefresh();
    // ★ ServletWebServerApplicationContext 在此启动内嵌 Web 服务器
    //   → ServletWebServerFactory.getWebServer() → Tomcat.start()

// ===== 步骤 10：注册监听器 — 收集所有 ApplicationListener =====
    registerListeners();
    // 此时广播 earlyApplicationEvents（步骤 11 之前缓存的事件）

// ===== 步骤 11：★ 实例化所有非懒加载的单例 Bean =====
    finishBeanFactoryInitialization(beanFactory);
    // ① 冻结 BeanDefinition（不再允许修改）
    // ② 预实例化：preInstantiateSingletons → getBean(beanName) 逐个创建

// ===== 步骤 12：完成刷新 — 清理缓存、发布 ContextRefreshedEvent =====
    finishRefresh();
    // ① 清除资源缓存
    // ② 初始化 LifecycleProcessor → 调用所有 Lifecycle Bean 的 start()
    // ③ ★ 发布 ContextRefreshedEvent → 通知所有监听器"容器就绪"

    } // synchronized 结束
    // 异常处理：destroyBeans() + cancelRefresh() → active=false
}
```

**追问：Bean 什么时候才被真正创建？** → 步骤 11 的 `preInstantiateSingletons()`（非懒加载单例）。在此之前，容器只有 BeanDefinition（图纸），没有 Bean 实例。

---

## 2. Spring 事件体系

### 2.1 核心组件

| 组件 | 接口/实现 | 职责 |
|------|-----------|------|
| 事件 | ApplicationEvent | 继承 EventObject，携带事件源 |
| 发布者 | ApplicationEventPublisher | publishEvent 同一线程同步执行 |
| 广播器 | SimpleApplicationEventMulticaster | 遍历匹配的 Listener，逐个调用 |
| 监听器 | ApplicationListener<E> | 实现 onApplicationEvent(E) 或 @EventListener |

### 2.2 事件广播机制

```java
// SimpleApplicationEventMulticaster#multicastEvent(ApplicationEvent, ResolvableType)
// ① 解析泛型类型（判断该 Listener 是否要接收此类型事件）
// ② 遍历所有匹配的 ApplicationListener
// ③ 如果有 Executor（线程池）→ 异步执行
//    否则 → 同步执行（默认在同一线程，发布者等待所有监听器处理完毕）
//    这意味着事件发布是同步阻塞的，一个监听器慢会拖慢全部

// ★ Bootstrap 事件总线标准事件序列：
ApplicationStartingEvent          → SpringApplication 构造完成后
ApplicationEnvironmentPreparedEvent → Environment 准备完毕但 Context 尚未创建
ApplicationContextInitializedEvent  → Context 创建并执行完 Initializer
ApplicationPreparedEvent            → Context 已准备、未 refresh
ApplicationStartedEvent             → refresh 完成、未执行 runner
ApplicationReadyEvent               → runner 执行完毕、应用完全就绪
```

### 2.3 @EventListener 与 @TransactionalEventListener

```java
// ① 面向注解的监听器（无需实现接口）— Spring 4.2+
@EventListener
public void handleOrderCreated(OrderCreatedEvent event) { ... }

// ② 事务绑定监听器 — 只在事务提交后才执行
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void sendNotification(OrderCreatedEvent event) { ... }
// phase 取值：BEFORE_COMMIT / AFTER_COMMIT（默认） / AFTER_ROLLBACK / AFTER_COMPLETION
// 经典场景：订单创建后发消息 → 必须在事务提交后，否则消息发出了但事务回滚
```

---

## 3. ApplicationContext 的层次与生命周期

### 3.1 常用子类

| 子类 | 应用类型 | 关键特征 |
|------|----------|----------|
| AnnotationConfigApplicationContext | 独立普通应用 | 基于注解 |
| AnnotationConfigServletWebServerApplicationContext | **Web（Servlet）Boot 默认** | onRefresh 启动 Tomcat |
| AnnotationConfigReactiveWebServerApplicationContext | WebFlux | onRefresh 启动 Netty |
| GenericApplicationContext | 通用基类 | 不是特定类型 |

### 3.2 BeanFactory vs ApplicationContext

| 对比 | BeanFactory | ApplicationContext |
|------|-------------|-------------------|
| 层级 | 底层容器 | 高级容器（继承 BeanFactory） |
| Bean 实例化 | 延迟（首次 getBean） | 预实例化（refresh 步骤 11） |
| 自动 BPP/BFPP 注册 | 无（需要手动注册） | 自动检测注册 |
| 国际化/事件 | 不支持 | 支持 MessageSource + 事件发布 |
| 使用 | 极少直接使用 | 开发标准 |

### 3.3 Parent Context 层级（如 Spring MVC）

```text
典型 Web 应用：
  Root WebApplicationContext（父容器 — Service/Repository/Datasource 等）
          ↑ 可通过继承访问父容器的 Bean
  Servlet WebApplicationContext（子容器 — Controller/HandlerMapping 等）
          ↓ 不可反过来（父不能访问子的 Bean）
```

**追问：** Spring Boot 有这种层级吗？→ Boot 默认**单一 Context**，不分父子 — 这是简单化的刻意设计。

---

## 4. 微服务组件如何利用事件与生命周期

| 组件 | 使用机制 | 目的 |
|------|----------|------|
| Nacos 注册 | ApplicationListener<WebServerInitializedEvent> | Web 服务器启动后注册服务（确保端口已就绪） |
| Ribbon LoadBalancer | SmartInitializingSingleton | 所有单例 Bean 初始化完成后为 @LoadBalanced RestTemplate 添加拦截器 |
| Feign 客户端 | ImportBeanDefinitionRegistrar + FactoryBean | 动态注册 Feign 接口的代理 BeanDefinition |
| Sentinel Dashboard | SmartInitializingSingleton + HandlerInterceptor | 全 Bean 就绪后扫描 Sentinel 资源、注册拦截器 |
| Spring Cloud Bus | @EventListener + RemoteApplicationEvent | 刷新配置广播（跨服务配置刷新） |

**关键规律：** 各组件选择不同的生命周期时机（refresh 第几步 / 哪个事件），是为了保证自己的依赖 Bean 已就绪 — **时序决定正确性**。

---

## 5. 常见问题：事件不触发与 Context 层级

| 问题 | 原因 | 对策 |
|------|------|------|
| 发布事件后监听器未收到 | ① Listener 未被 Spring 管理（不在 Context 中）② 子 Context 发布、Listener 在父 Context | 确保同 Context 或跨 Context 传播 |
| 事件处理中改了数据但事务未提交 | 事件同步执行时事务未提交 | 用 @TransactionalEventListener(AFTER_COMMIT) |
| 事件监听器慢拖慢主流程 | SimpleApplicationEventMulticaster 默认同步执行 | 注入线程池 Executor 使其异步 |
| afterPropertiesSet 抛异常定位困难 | 异常被 Spring 包装为 BeanCreationException | 源码中打断点定位具体行 |

---

> 🎯 **核心要点**：refresh 12 步 = Spring 的"一次启动全流程"。每个微服务组件选择在哪个步骤/哪个事件点做初始化，是架构设计水平的体现。事件体系 = **同步广播 + 泛型匹配 + 事务绑定可选**。能讲清楚 `ContextRefreshedEvent` 和 `WebServerInitializedEvent` 的先后关系，面试就是源码级理解。

**下一模块**：[07-自定义Starter与扩展点实战](07-自定义Starter与扩展点实战.md) / **返回总览**：[00-深度剖析总览](00-Spring生态深度剖析总览.md)
