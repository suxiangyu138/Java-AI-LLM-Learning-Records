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

### 1.1 时序与源码方法对照（面试默写版）

| 时序步骤 | 源码落点（AbstractAutowireCapableBeanFactory） | 说明 |
|---------|----------------------------------------------|------|
| ② 实例化前短路 | `resolveBeforeInstantiation` | 返回非 null 则跳过常规创建 |
| ③ 实例化 | `createBeanInstance` | 构造器推断：单构造器直接用；多构造器按规则选 |
| ④ 属性填充 | `populateBean` | 后处理器 `postProcessProperties`（@Autowired 注入在此） |
| ⑤⑥⑦ 初始化 | `initializeBean` | Aware → Before → 初始化三写法 → After |
| ⑧ 销毁登记 | `registerDisposableBeanIfNecessary` | 单例才登记（原型不登记，见第 5 节） |

> 🎯 记忆口诀：**resolve → create → populate → initialize → register**——五个动词对应 `doCreateBean` 主干，面试手写生命周期按这个骨架展开，不丢步骤。

### 1.2 销毁阶段的细节

- **销毁顺序**：容器关闭时按**注册逆序**销毁（后创建的先销毁）；
- `@PreDestroy` 抛异常：**不阻断**其他 Bean 的销毁，只记日志——资源释放失败要单独处理；
- 单例销毁 = 容器关闭时；原型从不销毁（容器不跟踪）；request/session 由 Web 容器跟踪销毁。

> ⚠️ 生产坑：自定义线程池 Bean 若 `destroyMethod` 未配，容器关闭时线程池不会优雅关闭——显式 `destroyMethod = "shutdown"` 或实现 `DisposableBean`；重启发版时线程泄漏最隐蔽。

### 1.3 面试追问：初始化抛异常会怎样？

初始化方法（含 @PostConstruct）抛异常 → 包装成 `BeanCreationException` 向上抛 → 单例预加载阶段**整个 refresh 失败，应用启动失败**（fail-fast）。排查时看日志里 `caused by` 指向哪个初始化方法；这是"启动期全量校验"的一部分——宁可启动失败，不可带病运行。

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

### 2.1 Aware 回调的实现机制

Aware 不是魔法——它是内置 `BeanPostProcessor` 按接口 **instanceof 分发**实现的：

```text
initializeBean → postProcessBeforeInitialization
 → ApplicationContextAwareProcessor（context 模块内置 BPP）
     ├─ instanceof EnvironmentAware        → setEnvironment
     ├─ instanceof EmbeddedValueResolverAware → setEmbeddedValueResolver
     ├─ instanceof ApplicationContextAware → setApplicationContext
     ├─ instanceof MessageSourceAware      → setMessageSource
     └─ ...（按序逐个判断）
```

| 维度 | Aware 注入 | 构造器注入 |
|------|-----------|-----------|
| 依赖方向 | 反向（容器 → Bean） | 正向（Bean 声明依赖） |
| 可测试性 | 差（需 mock 容器） | 好（直接传参） |
| 时机 | 属性填充后、初始化前 | 构造时 |
| 适用 | 框架扩展点实现 | 业务代码 |

> 💡 面试加分：`BeanNameAware` / `BeanFactoryAware` 由 `AbstractAutowireCapableBeanFactory` 自身处理，`ApplicationContextAware` 系由 `ApplicationContextAwareProcessor` 处理——**归属不同**，前者在 beans 层，后者在 context 层。

### 2.2 Aware 家族选型：什么时候真的需要

| 需求 | 推荐替代 |
|------|---------|
| 拿容器/工厂做动态查询 | 注入 `ObjectProvider<T>` 或 `ApplicationContext`（低频才用） |
| 拿自己的 Bean 名 | `ObjectProvider<SelfService>` 或注册时传入 |
| 框架扩展（自定义 BPP） | Aware 是正规途径 |
| 业务代码拿资源 | `@Value` / `ResourceLoader` 注入（Lite 化） |

> 🎯 结论：**业务代码远离 Aware，扩展代码拥抱 Aware**——判断标准是"这份代码是业务逻辑还是框架机制"。

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

### 3.1 三种写法的细节与冲突

| 细节 | 说明 |
|------|------|
| @PostConstruct 底层 | `CommonAnnotationBeanPostProcessor`（BPP 调用）——仅对被容器管理的 Bean 生效，`new` 出来的对象调用不到 |
| 三写法并存 | **全部执行**（不是互斥），严格按 1 → 2 → 3 顺序 |
| 初始化异常 | `BeanCreationException` 上抛，预加载阶段启动失败（见 1.3） |
| 销毁异常 | 记日志不阻断，后续销毁继续 |
| 方法可见性 | 三写法都要求方法非 private（CGLIB/反射约束） |

### 3.2 面试追问：为什么推荐 @PostConstruct 而不是 InitializingBean？

| 维度 | @PostConstruct | InitializingBean |
|------|---------------|-----------------|
| 侵入性 | 注解，无接口 | 实现 Spring 接口（容器耦合） |
| 依赖方向 | JDK/jakarta 标准 | Spring 专属 |
| 多回调 | 可标多个方法（顺序不定，少用） | 单方法 |
| 调试 | 方法名清晰 | `afterPropertiesSet` 语义隐晦 |
| 结论 | ✅ 首选 | 历史写法，兼容用 |

> 💡 反向思考：InitializingBean 的优势是**编译期强制**（接口不实现就编译不过）——框架级 Bean（如内置组件）用它保证回调必实现；业务代码不需要这种强制，注解更轻。

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

**作用域使用场景速查**：

| 作用域 | 典型 Bean | 反例（不该用） |
|--------|----------|---------------|
| singleton | 无状态服务、连接池、配置 | 有状态计数器 |
| prototype | 状态隔离的临时对象、昂贵且有界实例 | 无状态服务（浪费） |
| request | 请求级上下文、追踪 ID | 跨请求共享的数据 |
| session | 登录态、购物车 | 大对象（内存随会话膨胀） |
| application | ServletContext 级全局数据 | 有状态热数据 |

> ⚠️ **proxyMode 必配**：非 singleton 作用域注入单例 Bean 时，需要 `ScopedProxyMode` 生成代理，否则注入的是"创建时的固定实例"；同样，singleton 注入 prototype 见 5。

### 4.1 作用域的实现原理

| 作用域 | 实现类 | 机制 |
|--------|--------|------|
| singleton | `SingletonBeanRegistry`（单例缓存） | 1 级缓存 Map |
| prototype | `AbstractBeanFactory` 直接创建 | 不缓存、不销毁、不登记 |
| request | `RequestScope`（context.web） | ServletRequest attributes 存实例 |
| session | `SessionScope` | HttpSession attributes 存实例 |
| 自定义 | `ConfigurableBeanFactory.registerScope(name, Scope)` | 任意实现 `Scope` 接口 |

```java
// 自定义作用域：实现 Scope 接口 + registerScope 注册
public class ThreadScope implements Scope {
    private final ThreadLocal<Map<String, Object>> holder = new ThreadLocal<>();

    @Override
    public Object get(String name, ObjectFactory<?> objectFactory) {
        Map<String, Object> map = holder.get();
        if (map == null) { map = new HashMap<>(); holder.set(map); }
        return map.computeIfAbsent(name, k -> objectFactory.getObject());
    }
    // remove / registerDestructionCallback / resolveContextualObject / getConversationId 略
}

// 注册：按线程域生效
ConfigurableBeanFactory bf = (ConfigurableBeanFactory) ctx.getAutowireCapableBeanFactory();
bf.registerScope("thread", new ThreadScope());
```

> 💡 面试追问"能自定义作用域吗"：实现 `Scope` 接口 + `registerScope` 注册——"每请求追踪 ID"、"每租户实例"等场景都可以这么做；注册表在 `ConfigurableBeanFactory`，属于 beans 层能力。

### 4.2 proxyMode 的两种代理模式

| 模式 | 代理类型 | 适用 |
|------|---------|------|
| `ScopedProxyMode.INTERFACES` | JDK 动态代理 | 作用域 Bean 有接口时 |
| `ScopedProxyMode.TARGET_CLASS` | CGLIB | 具体类；Boot 默认 CGLIB |

> ⚠️ CGLIB 代理要求类**非 final**、方法非 final 非私有——final 类配 `TARGET_CLASS` 启动即报错；用接口 + `INTERFACES` 可避开；Lombok 的 `@Value`（不可变类）配 CGLIB 也会踩坑。

### 4.3 面试追问：request Bean 的代理对象会怎样工作？

注入单例的其实是一个**作用域代理**：每次方法调用经代理转发到"当前请求的实例"——代理本身是单例，转发目标是请求实例；请求结束后实例销毁，代理继续为下一个请求服务。这就是"单例持有 request Bean"不串数据的原理。

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

### 5.1 四种解决方案的取舍对比

| 方案 | 每次取新实例 | 侵入性 | 性能 | 推荐度 |
|------|:---:|:---:|:---:|:---:|
| `ObjectProvider.getObject()` | ✅ | 低（构造器注入） | 好 | ★★★ |
| `@Lookup` 方法 | ✅ | 中（方法需可重写） | 好（CGLIB 子类） | ★★ |
| `ScopedProxy` | ✅（经代理） | 低（透明） | 略差（每次代理调用） | ★★ |
| `ctx.getBean()` | ✅ | 高（容器耦合） | 好 | ★ |

> ⚠️ @Lookup 的两个前提：类可 CGLIB 化（非 final）+ 方法可重写（非 final/private）——与"自调用不走代理"同源；方法体写什么都行（容器用生成子类覆盖），但习惯上返回 null 表意。

### 5.2 原型 Bean 的其他边界

- 原型 Bean 的 `destroyMethod` / `@PreDestroy` **不会执行**——容器不跟踪原型实例；
- 原型依赖单例：正常（原型创建时注入当前单例）；
- 原型 + AOP：代理基于类生成，原型每次创建都过一遍后处理器（性能按需评估）；
- 原型 Bean 内再注入原型：每次创建原型 A 时，注入的原型 B 是**当时新创建的**（不是同一实例，也不是缓存的旧实例）。

> 💡 生产建议：**默认全单例，只有"必须隔离状态"才用原型**——原型 Bean 数量少、生命周期短，滥用原型会放大创建开销与 GC 压力。

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

### 6.0 扩展点触发范围对比（补充）

| 扩展点 | 触发次数 | 排序控制 | 失败影响 |
|--------|---------|---------|---------|
| BeanPostProcessor | 每 Bean 两次（前后） | PriorityOrdered/Ordered | 单个 Bean 创建失败 |
| InstantiationAwareBPP | 每 Bean 多次 | 同上 | 同上 |
| BeanFactoryPostProcessor | 启动一次 | 同上 | 启动失败 |
| SmartInitializingSingleton | 全量后一次 | 同上 | 启动失败 |
| Aware | 每个实现者一次 | 固定顺序 | 该 Bean 失败 |

> 💡 扩展点不是越多越好：**触发频率越高的扩展点，越要轻量**——BPP 对每个 Bean 执行，重逻辑会被放大 N 倍；能放 `SmartInitializingSingleton` 的一次性收尾，就不要放 BPP。

### 6.1 自定义 BeanPostProcessor 实战：初始化耗时埋点

```java
@Component
public class TimingBeanPostProcessor implements BeanPostProcessor {
    private final Map<String, Long> startTimes = new HashMap<>();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        startTimes.put(beanName, System.nanoTime());
        return bean;                        // 必须原样返回（或返回替换对象）
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        Long start = startTimes.remove(beanName);
        if (start != null) {
            long costMs = (System.nanoTime() - start) / 1_000_000;
            if (costMs > 100) {
                log.warn("Bean [{}] 初始化耗时 {}ms", beanName, costMs);
            }
        }
        return bean;
    }
}
```

### 6.2 生命周期扩展点生产实践

| 实践 | 要点 |
|------|------|
| 不要随意改返回引用 | 除包装/代理外保持原对象，否则破坏下游注入 |
| 注意与 AOP 顺序 | `AbstractAutoProxyCreator` 也在 After 阶段，先后影响代理链 |
| 只做横切逻辑 | 业务逻辑别进 BPP——难以测试、难以排查 |
| 避免重操作 | BPP 对**每个 Bean** 执行，重逻辑放大 N 倍 |
| 有状态 BPP 谨慎 | 并发创建时注意线程安全 |

> 🎯 选型一句话：**观察用 BPP，改造图纸用 BFPP，业务初始化用 @PostConstruct，一次性收尾用 SmartInitializingSingleton**——先按目的定扩展点，再动手。

### 6.2.5 生命周期各阶段失败排查对照

| 失败阶段 | 典型异常 | 排查方向 |
|---------|---------|---------|
| 实例化 | `BeanInstantiationException` | 构造器抛错/无可用构造器/抽象类 |
| 属性填充 | `BeanCreationException`（caused by 注入） | 依赖缺失、类型不匹配、循环依赖 |
| 初始化 | `BeanCreationException`（caused by 回调） | @PostConstruct 内异常、资源初始化失败 |
| 代理 | `BeanCreationException`（AOP 相关） | 切点表达式错误、代理类型不兼容 |
| 销毁 | 日志告警（不中断） | 资源释放失败、线程未关闭 |

> ⚠️ 排错要点：`BeanCreationException` 的 **caused by** 才是根因——栈顶往往只是"创建失败"的壳；先看 caused by 链，再对表定位阶段，一次到位。

### 6.3 生命周期面试问答对照表

| 面试题 | 回答锚点 |
|--------|---------|
| 生命周期顺序？ | 实例化 → 填充 → Aware → Before → 初始化三写法 → After → 使用 → 销毁 |
| 代理在哪一步？ | postProcessAfterInitialization（⑦） |
| 销毁顺序？ | 注册逆序（后创建先销毁） |
| 原型销毁吗？ | 不销毁（容器不跟踪） |
| Aware 顺序？ | 填充后、初始化前；BeanName 类先于 Context 类 |
| 三写法能共存吗？ | 能，全执行，按 1→2→3 |

### 6.4 深入体系衔接

| 本页主题 | 深度文档 |
|---------|---------|
| 生命周期与 PostProcessor 扩展 | [Spring生态深度剖析-01](../../../Spring生态深度剖析/01-Bean生命周期与PostProcessor扩展体系.md) |
| 三级缓存与循环依赖 | [Spring生态深度剖析-03](../../../Spring生态深度剖析/03-循环依赖与三级缓存深度剖析.md) |
| 容器体系全景 | [Spring框架核心-01](../../../Spring框架核心/01-容器体系-BeanFactory与ApplicationContext.md) |
| 生命周期与作用域深潜 | [Spring框架核心-04](../../../Spring框架核心/04-Bean生命周期与作用域.md) |

> 💡 速查表命中后，需要"为什么"的答案时按此表跳深挖——速查负责"是什么"，深度体系负责"为什么"。

---

**下一模块**：[06-依赖注入与自动装配速查](06-依赖注入与自动装配速查.md)　**返回总览**：[00-Spring Beans组件总览](00-Spring Beans组件总览.md)
