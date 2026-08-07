# 05 Bean 生命周期与作用域速查

> 一个 Bean 从"图纸"到"销毁"的完整时序、Aware 家族、初始化三写法、五大作用域——面试必考主干

---

## 📚 目录

1. [生命周期全景时序](#1-生命周期全景时序)
2. [Aware 接口家族](#2-aware-接口家族)
3. [初始化/销毁的三种写法与优先级](#3-初始化销毁的三种写法与优先级)
4. [五大作用域详解](#4-五大作用域详解)
5. [原型 Bean 的陷阱](#5-原型-bean-的陷阱)
6. [生命周期扩展点选型](#6-生命周期扩展点选型)

---

## 1. 生命周期全景时序

```text
容器 refresh() 启动
  │
  ├─ ① 解析/注册 BeanDefinition（图纸入库）
  │
  └─ 预实例化单例（getBean）逐个执行：
      ② 实例化前：InstantiationAwareBeanPostProcessor.postProcessBeforeInstantiation
                   （可短路：直接返回代理/空对象）
      ③ 推断构造器 → 实例化（反射/工厂方法）
      ④ 属性填充 populateBean：
           - InstantiationAwareBeanPostProcessor.postProcessAfterInstantiation
           - 后置器 postProcessProperties（@Autowired 注入在此）
           - 各种 Aware 回调（BeanNameAware、BeanFactoryAware...）
      ⑤ BeanPostProcessor.postProcessBeforeInitialization
      ⑥ 初始化：
           - @PostConstruct
           - InitializingBean.afterPropertiesSet
           - 自定义 initMethod
      ⑦ BeanPostProcessor.postProcessAfterInitialization（★ AOP 代理在此创建）
      ⑧ 注册 DisposableBean（销毁回调登记）
  │
  └─ 容器关闭 close()：
       SmartInitializingSingleton（全量创建后）→
       @PreDestroy → DisposableBean.destroy → 自定义 destroyMethod
```

> 🎯 **核心要点（背诵版）**：实例化 → 属性填充 → Aware → Before 初始化 → **初始化三写法** → After 初始化（代理）→ 使用 → 销毁三写法。AOP 代理在 ⑦ 之后，所以代理对象才是容器暴露的"Bean"。

## 2. Aware 接口家族

| 接口 | 注入内容 | 时机 |
|------|---------|------|
| `BeanNameAware` | 自己的 Bean 名 | 属性填充后、初始化前 |
| `BeanClassLoaderAware` | 类加载器 | 同上 |
| `BeanFactoryAware` | 所在 BeanFactory | 同上 |
| `ApplicationContextAware` | 上下文（context 模块） | 同上（实现于 ApplicationContextAwareProcessor） |
| `EnvironmentAware` | Environment | 同上 |
| `MessageSourceAware` | 国际化资源 | 同上 |
| `ResourceLoaderAware` / `ApplicationEventPublisherAware` | 资源/事件发布器 | 同上 |

```java
@Service
public class SelfAwareService implements ApplicationContextAware {
    private ApplicationContext ctx;

    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        this.ctx = ctx;                       // 需要 ctx 才能自调用代理时用
    }
}
```

> ⚠️ **不推荐滥用**：Aware 把 Bean 和容器耦死，破坏可测试性；99% 的"需要容器"场景可用 `ObjectProvider` 或构造器注入替代。注入自身代理解决自调用问题：`ObjectProvider<SelfService>` 或 `@Resource` 自己。

## 3. 初始化/销毁的三种写法与优先级

| 优先级 | 初始化 | 销毁 |
|:---:|--------|------|
| 1 | `@PostConstruct` | `@PreDestroy` |
| 2 | `InitializingBean.afterPropertiesSet()` | `DisposableBean.destroy()` |
| 3 | XML/`@Bean.initMethod` | XML/`@Bean.destroyMethod` |

```java
@Service
public class Connector implements InitializingBean, DisposableBean {
    @PostConstruct
    public void post() { log.info("1. @PostConstruct"); }

    @Override
    public void afterPropertiesSet() { log.info("2. InitializingBean"); }

    @Bean(initMethod = "customInit", destroyMethod = "customDestroy")   // 3. 自定义方法
    public Connector connector() { return new Connector(); }

    public void customInit() { log.info("3. initMethod"); }
}
```

> 💡 现代推荐：纯 `@PostConstruct`/`@PreDestroy`（无接口侵入）；`@Bean` 的 `destroyMethod` 注意默认推断陷阱（见 [04-配置属性速查](04-配置属性速查.md)）。

## 4. 五大作用域详解

| 作用域 | 生命周期归属 | 默认 | 使用要点 |
|--------|------------|:---:|---------|
| `singleton` | 容器级（**容器销毁才销毁**） | ✅ | 默认；无状态服务 |
| `prototype` | 获取即新实例，**不随容器销毁** | ❌ | 有状态/昂贵且隔离对象 |
| `request` | 单次 HTTP 请求 | ❌ | Web 环境（Spring MVC） |
| `session` | 单个 HTTP 会话 | ❌ | Web 环境（登录态对象） |
| `application` / `websocket` | ServletContext / WebSocket | ❌ | Web 环境 |

```java
@Bean
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public UserContext userContext() { return new UserContext(); }
```

> ⚠️ **proxyMode 必配**：非 singleton 作用域注入单例 Bean 时，需要 `ScopedProxyMode` 生成代理，否则注入的是"创建时的固定实例"；同样，singleton 注入 prototype 见 5。

## 5. 原型 Bean 的陷阱

**问题**：单例 Bean 字段注入原型 Bean → 只注入一次，永远是同一个实例。

```java
@Service
public class SingletonService {
    @Autowired
    private PrototypeBean proto;   // ❌ 只在创建时注入一次，之后不变
}
```

| 解决方案 | 写法 | 适用 |
|---------|------|------|
| `ObjectProvider` | `protoProvider.getObject()` | ✅ 首选，显式取新实例 |
| `@Lookup` | `@Lookup public PrototypeBean getProto() { return null; }` | 方法级取用 |
| `ScopedProxy` | 原型配 `proxyMode = TARGET_CLASS` | 每次调用经代理重取 |
| `ApplicationContext.getBean` | 手动取 | 不推荐（容器耦合） |

```java
@Service
public class SingletonService {
    private final ObjectProvider<PrototypeBean> protoProvider;

    public SingletonService(ObjectProvider<PrototypeBean> protoProvider) {
        this.protoProvider = protoProvider;
    }

    public void doWork() {
        PrototypeBean fresh = protoProvider.getObject();   // 每次新实例
    }
}
```

> 🎯 **核心要点**：作用域不匹配时"注入值快照"是根源——单例在启动时固定一次，所有跨作用域注入都要用 Provider/代理打破快照。

## 6. 生命周期扩展点选型

| 扩展点 | 触发范围 | 用途 | 推荐度 |
|--------|---------|------|:---:|
| `BeanPostProcessor` | **每个 Bean** | 注入、代理、包装 | ★★★ |
| `InstantiationAwareBeanPostProcessor` | 每个 Bean（实例化前后） | 短路创建、自定义注入逻辑 | ★★ |
| `BeanFactoryPostProcessor` | 启动早期一次 | 改 BeanDefinition（极少用） | ★★ |
| `SmartInitializingSingleton` | 全部单例创建后一次 | 聚合收尾 | ★★ |
| `Aware` 家族 | 每个实现者 | 拿容器能力 | ★（慎用） |
| `@PostConstruct` | 每个标注方法 | 业务初始化 | ★★★ |

> 💡 面试延伸：实现自己的 `BeanPostProcessor` 时注意**它本身也是 Bean**——容器要先创建后处理器再处理其余 Bean；而"处理后处理器的后处理器"由框架内部注册，不存在此问题。

---

**下一模块**：[06-依赖注入与自动装配速查](06-依赖注入与自动装配速查.md)　**返回总览**：[00-Spring Beans组件总览](00-Spring Beans组件总览.md)
