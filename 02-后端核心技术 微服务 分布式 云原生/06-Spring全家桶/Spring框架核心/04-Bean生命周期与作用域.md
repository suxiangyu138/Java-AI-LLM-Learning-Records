# 04 Bean 生命周期与作用域

> 一个 Bean 从图纸到销毁经历九个阶段——理解生命周期时序、Aware 接口与作用域语义，是读懂 Spring 行为与踩坑的必修课

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

**一个 singleton Bean 从创建到销毁的完整时序**（工程视角）：

```text
① BeanDefinition 注册（图纸入库）
      ↓ 容器 refresh
② 实例化（构造器执行 —— 无依赖可注入，字段还是默认值）
      ↓
③ 属性填充（依赖注入：构造器参数/@Autowired/setter）
      ↓
④ Aware 回调（BeanNameAware → BeanFactoryAware → ApplicationContextAware）
      ↓
⑤ BeanPostProcessor#postProcessBeforeInitialization
      ↓
⑥ 初始化（@PostConstruct → InitializingBean#afterPropertiesSet → @Bean(initMethod)）
      ↓
⑦ BeanPostProcessor#postProcessAfterInitialization（⚠️ AOP 代理在此创建）
      ↓
⑧ 使用（getBean / 注入给其他 Bean）
      ↓ 容器关闭
⑨ 销毁（@PreDestroy → DisposableBean#destroy → @Bean(destroyMethod)）
```

```java
@Component
public class LifecycleDemo implements InitializingBean, DisposableBean {
    public LifecycleDemo() { System.out.println("② 实例化"); }
    @Autowired public void setDao(Dao dao) { System.out.println("③ 属性填充"); }

    @PostConstruct public void postConstruct() { System.out.println("⑥-1 @PostConstruct"); }
    @Override public void afterPropertiesSet() { System.out.println("⑥-2 InitializingBean"); }
    @Bean(initMethod = "init")  // ⑥-3 initMethod（配置类上声明）

    @PreDestroy public void preDestroy() { System.out.println("⑨-1 @PreDestroy"); }
    @Override public void destroy() { System.out.println("⑨-2 DisposableBean"); }
}
```

> 🎯 **核心要点**：**实例化 ≠ 初始化**——实例化只是构造器执行（对象存在但字段可能没赋值），初始化才是业务准备完成（校验配置、建连接）。AOP 代理在**初始化之后**创建（第 ⑦ 步）——所以代理能拦到所有方法调用。

---

## 2. Aware 接口家族

**Aware 接口**：在初始化前让 Bean"感知"容器能力——回调注入容器组件（Bean 名、BeanFactory、环境等）：

| Aware 接口 | 回调内容 | 使用场景 |
|-----------|---------|---------|
| `BeanNameAware` | 自己的 Bean 名 | 日志标识、按名查找兄弟 |
| `BeanFactoryAware` | BeanFactory | 程序化 getBean（反模式，慎用） |
| `ApplicationContextAware` | ApplicationContext | 发布事件、拿资源（@Resource 已覆盖大部分） |
| `EnvironmentAware` | Environment | 读取配置（@Value 已覆盖） |
| `ResourceLoaderAware` | ResourceLoader | 加载资源（@Value("classpath:...") 已覆盖） |
| `ApplicationEventPublisherAware` | 事件发布器 | 发布事件（注入 ApplicationEventPublisher 更简洁） |
| `MessageSourceAware` | MessageSource | 国际化消息（@Autowired 更简洁） |

```java
@Component
public class MyBean implements ApplicationContextAware {
    private ApplicationContext ctx;

    @Override
    public void setApplicationContext(ApplicationContext ctx) {   // 在初始化前回调
        this.ctx = ctx;
    }

    public void doSomething() {
        ctx.publishEvent(new MyEvent(this));      // 手动感知容器能力
    }
}
```

> ⚠️ **现代实践**：Aware 接口正被"直接注入"取代——`@Autowired ApplicationContext`、`@Value` 能覆盖 90% 场景。Aware 只在"初始化回调阶段就需要容器能力"时必要（那时注入还没完成？不——属性填充在 Aware 之前，所以其实都可注入；Aware 的独特价值是**不依赖注解**的纯回调）。框架内部大量使用 Aware。

---

## 3. 初始化/销毁的三种写法与优先级

| 优先级 | 初始化 | 销毁 | 说明 |
|:------:|--------|------|------|
| 1 | `@PostConstruct` | `@PreDestroy` | 注解方式（Jakarta，最常用） |
| 2 | `InitializingBean#afterPropertiesSet` | `DisposableBean#destroy` | 接口方式（侵入，框架内部常用） |
| 3 | `@Bean(initMethod=...)` | `@Bean(destroyMethod=...)` | 配置方式（第三方类无法加注解） |

**执行顺序：@PostConstruct → afterPropertiesSet → initMethod**。

```java
@Configuration
public class ConnConfig {
    @Bean(initMethod = "connect", destroyMethod = "close")     // 第三方类：配置指定回调
    public MyConnection connection() {
        return new MyConnection();      // 第三方类没有注解，用 initMethod/destroyMethod
    }
}
```

**Spring 7.0 的默认 destroyMethod**：`@Bean` 默认会推断 `close()/shutdown()` 作为销毁方法（`AbstractBeanDefinition.INFER_METHOD`）——所以第三方类实现 `AutoCloseable` 时无需显式声明 destroyMethod，容器自动调用。

> 🎯 **核心要点**：选型口诀——**自己的类用 @PostConstruct/@PreDestroy；第三方类用 @Bean(initMethod/destroyMethod)**；接口方式留给框架内部。

---

## 4. 五大作用域详解

| 作用域 | 生命周期 | 实例数 | 使用场景 |
|--------|---------|:------:|---------|
| **singleton**（默认） | 容器启动创建，容器销毁销毁 | 1 个 | 无状态服务（绝大多数） |
| **prototype** | 每次获取新建，**容器不管理销毁** | 每次一个 | 有状态对象、临时组件 |
| **request** | 一次 HTTP 请求 | 每请求一个 | Web 请求级状态（如请求上下文） |
| **session** | 一次 HTTP 会话 | 每会话一个 | 用户会话状态（购物车） |
| **application** | ServletContext 生命周期 | 全局一个 | 全局共享状态 |

```java
@Component
@Scope("prototype")                       // 或 @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AuditRecorder { ... }

@Component
@RequestScope                              // @Scope("request") 的别名
public class RequestContext { ... }
```

**singleton 与 prototype 的本质差异**：

| 维度 | singleton | prototype |
|------|:---------:|:---------:|
| 创建时机 | 容器 refresh 时预加载 | 每次 getBean/注入时 |
| 实例数 | 1（线程共享） | 无数 |
| 销毁管理 | 容器负责（@PreDestroy 执行） | **容器不追踪**（无销毁回调） |
| 线程安全 | 必须自身无状态/线程安全 | 每次独立，天然隔离 |
| 适用 | Service、Repository、配置 | 有状态工作对象 |

> ⚠️ **request/session 作用域在非 Web 环境不可用**；注入 request Bean 到 singleton 时必须用 `@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)` 代理，否则注入的是"创建时的那个实例"。

---

## 5. 原型 Bean 的陷阱

**原型 Bean 的两个经典陷阱**：

```java
// 陷阱 1：注入到 singleton —— 注入发生在创建 singleton 时，只注入了一次！
@Component
public class SingletonService {
    @Autowired
    private AuditRecorder recorder;      // ❌ 看起来是 prototype，实际只有一个（创建时注入的那个）
}
```

**解法**（三种）：

```java
// 解法 A：ObjectProvider 延迟获取（官方推荐）
@Component
public class SingletonService {
    private final ObjectProvider<AuditRecorder> recorderProvider;

    public SingletonService(ObjectProvider<AuditRecorder> p) { this.recorderProvider = p; }

    public void doWork() {
        AuditRecorder recorder = recorderProvider.getObject();   // 每次获取都新建
        recorder.record();
    }
}

// 解法 B：@Lookup 方法注入
@Component
public abstract class SingletonService {
    @Lookup
    protected abstract AuditRecorder getRecorder();   // Spring 子类化重写此方法，每次返回新实例
}

// 解法 C：ApplicationContext.getBean（反模式，慎用）
```

```java
// 陷阱 2：prototype 的销毁无人管 —— 容器不回调 @PreDestroy
@Component
@Scope("prototype")
public class SessionPool {
    @PreDestroy public void release() { }   // ❌ prototype 下永远不会被调用
}
// 解法：实现 DisposableBean 并让调用方手动销毁，或对象自带资源管理（TWR）
```

> 🎯 **核心要点**：**singleton 注入 prototype = 只注入一次**——这是使用作用域时最常踩的坑。正确姿势是 ObjectProvider 或 @Lookup，让"获取"推迟到使用点。

---

## 6. 生命周期扩展点选型

工程上常用的扩展点与选型（源码细节见 `Spring生态深度剖析/01-Bean生命周期与PostProcessor扩展体系.md`）：

| 扩展点 | 作用对象 | 时机 | 典型用途 |
|--------|---------|------|---------|
| `BeanFactoryPostProcessor` | BeanDefinition | 所有 Bean 创建前 | 修改图纸：占位符、注册额外 Bean |
| `BeanPostProcessor` | Bean 实例 | 每个 Bean 初始化前后 | @Autowired、@PostConstruct、**AOP 代理** |
| `ApplicationListener` | 容器事件 | 容器各阶段 | 启动完成回调、优雅停机 |
| `@ConfigurationProperties` + post-processor | 配置 Bean | 图纸阶段 | 配置绑定 |
| `@EventListener` | 业务事件 | 运行期 | 解耦（见 07 模块） |
| `ApplicationRunner` | 容器就绪后 | 启动末尾 | 启动预热、数据初始化 |

```java
// 示例：启动完成后做预热（最常用的容器生命周期钩子）
@Component
public class StartupPreheater implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) {
        log.info("容器就绪，开始预热缓存...");
        cacheService.preload();
    }
}
```

> 🎯 **核心要点**：选型口诀——**"改图纸"用 BFPP，"改实例"用 BPP，"等就绪"用 ApplicationRunner/事件，"拦方法"用 AOP**。把扩展点按"作用对象 + 时机"归档，面试与设计都不慌。

---

**下一模块**：[05-AOP面向切面编程](./05-AOP面向切面编程.md) / **返回总览**：[00-Spring框架核心知识体系总览](./00-Spring框架核心知识体系总览.md)
