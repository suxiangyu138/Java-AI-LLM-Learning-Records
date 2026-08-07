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

### 1.1 关键接口方法速查（含签名细节）

| 接口 | 方法 | 签名要点 |
|------|------|---------|
| `ListableBeanFactory` | `getBeansOfType(Class)` | 返回 `Map<String,T>`，**含所有作用域**；`includeNonSingletons` 参数控制是否含 prototype |
| `HierarchicalBeanFactory` | `containsLocalBean(name)` | 只查本级、不上溯父容器（`containsBean` 会上溯） |
| `EnvironmentCapable` | `getEnvironment()` | 返回 `ConfigurableEnvironment` 时可直接改属性源 |
| `ApplicationEventPublisher` | `publishEvent(Object event)` | 任意对象即可发布（4.2+），内部自动包装为 `PayloadApplicationEvent` |
| `MessageSource` | `getMessage(String code, Object[] args, String defaultMessage, Locale)` | 缺省值必须放参数位；`NoSuchMessageException` 在无默认值时抛出 |
| `ResourcePatternResolver` | `getResources(String locationPattern)` | 支持 `classpath*:` 与 ant 风格 `**`/`*`/`?` |

```java
// 面试手写级示例：遍历父子容器查找 Bean
ApplicationContext child = ...;   // 父容器 parent
BeanFactory parentBf = child.getParentBeanFactory();
boolean local = child.containsLocalBean("svc");   // false
boolean any = child.containsBean("svc");          // true（上溯找到父容器中的）
```

> 💡 `publishEvent(Object)` 的重载细节：传 `ApplicationEvent` 子类直接按原类型分发；传普通 POJO 会被包装成 `PayloadApplicationEvent` 再分发——所以监听器签名可以直接写 `OrderCreatedEvent`，Spring 通过泛型解包；但监听 `PayloadApplicationEvent` 也能收到（多个匹配监听器全部触发，注意重复消费）。

### 1.2 WebApplicationContext：容器族的 Web 分支

```text
WebApplicationContext（接口，spring-web 定义）
├── ConfigurableWebApplicationContext（可配置）
│   ├── XmlWebApplicationContext           （XML 配置，war 存量）
│   ├── AnnotationConfigWebApplicationContext（注解配置，war）
│   └── ServletWebServerApplicationContext（Boot 内嵌服务器专用）
```

| 特性 | 普通 ApplicationContext | WebApplicationContext |
|------|----------------------|----------------------|
| 作用域 | singleton / prototype | + request / session / application / websocket |
| 服务器 | 无 | 内嵌（Boot）或 Servlet 容器装配 |
| 资源前缀默认 | classpath | ServletContext 根路径 |
| 特有 Bean | - | servletContext / request / session 等作用域代理 |
| 初始化入口 | 手动 refresh | Servlet 容器监听器 / Boot run |

> ⚠️ **作用域边界**：request/session 作用域**只在 Web 容器内有效**——在纯 context 测试中声明 `@Scope("request")` 的 Bean 会因找不到作用域注册而报 `IllegalStateException: No Scope registered for scope name 'request'`；单元测试要么 mock 容器，要么改用 `@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)` 降级。这也是"测试里为什么跑不过 Web 专用配置"的根因。

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

### 2.1 refresh() 失败时的行为与兜底

refresh() 是**非原子**操作——每一步都可能抛异常，且失败后的容器状态各不相同：

| 失败位置 | 典型异常 | 容器状态 |
|---------|---------|---------|
| 第 1-2 步 | `BeansException`（工厂创建失败） | 无有效工厂，整个启动失败 |
| 第 5 步 | `BeanDefinitionStoreException`（配置类解析失败）、`BeanDefinitionValidationException`（占位符缺失） | 图纸阶段失败，**没有任何业务 Bean 被创建** |
| 第 6-10 步 | 多播器/监听器初始化异常 | 启动失败，已注册的定义无意义 |
| 第 11 步 | `BeanCreationException`（单例实例化失败，**最常见**） | **之前创建的单例保留在缓存中**，但整体启动失败 |
| 第 12 步 | `LifecycleProcessor` 启动异常 | 单例已全部创建，事件未发布 |

```text
关键语义：refresh() 抛异常后容器不能"继续用"——
  正确的做法是捕获异常 → ctx.close() → 修复配置 → 重建上下文。
  不能对同一个上下文再次 refresh()（2 次 refresh 会抛 IllegalStateException：
  "context already refreshed"——模板方法没有幂等设计，这是刻意为之）。
```

> ⚠️ **生产教训**：第 11 步失败时**部分单例已被创建并可能持有外部资源**（连接池、线程池）——如果只打印异常不 close()，这些资源会泄漏。Boot 中 `SpringApplication.run` 失败时会自行清理，但手写 main 的嵌入式应用必须自己 try-catch-close。

### 2.2 每步对应的源码类速查

| 步骤 | 关键方法 | 源码位置（7.x） |
|------|---------|-----------------|
| 1 | `prepareRefresh()` | `AbstractApplicationContext` |
| 2 | `obtainFreshBeanFactory()` | 子类实现（Generic 系无操作） |
| 3 | `prepareBeanFactory()` | 注册 Aware 处理器、`@Nullable` 注解解析器 |
| 4 | `postProcessBeanFactory()` | **子类钩子**（Web 容器在此注册作用域） |
| 5 | `invokeBeanFactoryPostProcessors()` | `PostProcessorRegistrationDelegate`（含 ConfigurationClassPostProcessor） |
| 6 | `registerBeanPostProcessors()` | 同上（按 PriorityOrdered/Ordered/无序排序注册） |
| 7 | `initMessageSource()` | 找不到 Bean 则注册 `DelegatingMessageSource` 兜底 |
| 8 | `initApplicationEventMulticaster()` | 找不到 Bean 则 new 一个 `SimpleApplicationEventMulticaster` |
| 9 | `onRefresh()` | 子类钩子（Boot 内嵌服务器在此创建） |
| 10 | `registerListeners()` | 静态注册 + **早期事件补发**（`earlyApplicationEvents`） |
| 11 | `finishBeanFactoryInstantiation()` | 逐一遍历非懒加载单例 `getBean` |
| 12 | `finishRefresh()` | 发布 `ContextRefreshedEvent`、`Lifecycle.start()` |

> 🎯 **面试升级点**：第 7-8 步的"找不到就注册兜底实现"是模板方法设计的典范——`DelegatingMessageSource` 保证 `getMessage` 永不 NPE（返回 code 原样），`SimpleApplicationEventMulticaster` 保证事件发布永不 NPE。**不配置也有默认行为**，这是 context 与 beans 层最大的人性化差异。

### 2.3 启动耗时因素与优化方向

| 耗时因素 | 占比（典型） | 优化手段 |
|---------|:----------:|---------|
| 组件扫描（反射/字节码遍历） | 中 | @Indexed 编译期索引（见 [03-配置类与扫描机制速查](03-配置类与扫描机制速查.md) 第 3 节） |
| 单例实例化（业务构造逻辑） | 高（业务决定） | 懒加载策略、启动后置任务异步化 |
| CGLIB 代理生成 | 低-中 | 7.0 默认 CGLIB 有开销；纯接口场景可 `@Proxyable(INTERFACES)` |
| 配置类解析 | 低 | 减少配置类层级与 @Import 嵌套 |
| 资源扫描（classpath*:） | 低-中 | 避免大目录通配；用索引/显式列表 |

```java
// 测量各阶段耗时的快速方法
long start = System.nanoTime();
ctx.refresh();   // 整体时间
// 更细：临时给 refresh() 打点（源码级断点或 AOP 切 AbstractApplicationContext）
// 生产实践：启动日志打"Startup took X ms"并分段记录（Boot 的 StartedApplication 时间）
```

> 💡 **边界认知**：启动慢 ≠ context 慢——绝大多数时间是**业务 Bean 的构造器与依赖初始化**；先 `--debug` 启动看 Bean 创建耗时分布（Boot 的 `spring.main.startup` 探测可精确到每步），再决定优化方向，**不要盲目上 @Indexed**。

### 2.4 十二步的"谁驱动谁"记忆链

```text
记忆链：模板（AbstractApplicationContext）→ 委托（PostProcessorRegistrationDelegate）
       → 引擎（ConfigurationClassPostProcessor）→ 数据（BeanDefinitionRegistry）

① 模板方法：refresh() 定义骨架（12 步顺序）         —— 结构
② 委托者：第 5-6 步委托 PostProcessorRegistrationDelegate —— 执行
③ 引擎：ConfigurationClassPostProcessor 是第 5 步的头号选手 —— 配置处理
④ 落点：所有结果写进 BeanDefinitionRegistry（= DefaultListableBeanFactory）
```

> 🎯 **面试表达升级**：把"十二步"从背诵升级为**三问**——"谁调用"（AbstractApplicationContext.refresh）、"谁执行"（PostProcessor 体系）、"数据落在哪"（BeanDefinitionRegistry）——三个答案覆盖 90% 的容器源码题，比背步骤号更有迁移力。例如问"@Bean 方法何时变成定义"，答"第 5 步由 ConfigurationClassBeanDefinitionReader 写入 registry"，即完成三问闭环。

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

### 3.1 实现类选择决策树

```text
要启动什么应用？
├── 独立进程/嵌入式（无 Servlet 容器）
│   ├── 注解配置 → AnnotationConfigApplicationContext（现代标准）
│   ├── 编程式/动态注册 → GenericApplicationContext
│   └── 存量 XML → ClassPathXmlApplicationContext
├── Boot Web 应用 → AnnotationConfigServletWebServerApplicationContext（Boot 内部选择，你不需要手动建）
├── 传统 Servlet 容器（war 部署）
│   └── 有 web.xml → XmlWebApplicationContext / AnnotationConfigWebApplicationContext
└── 单元测试
    ├── 快速上下文 → GenericApplicationContext + registerBean
    └── 全量集成 → SpringBootTest / SpringJUnit4ClassRunner 托管
```

| 场景 | 推荐 | 不推荐及原因 |
|------|------|-------------|
| 单元测试中验证 Bean 装配 | `GenericApplicationContext` + `registerBean` | 注解上下文太重（跑完整扫描） |
| 无框架的 main 方法工具 | `AnnotationConfigApplicationContext` | XML 上下文无注解能力 |
| 动态热注册 Bean | `GenericApplicationContext.getDefaultListableBeanFactory().registerBeanDefinition(...)` | 注解上下文构造后注册也可但语义混乱 |
| 需要父子容器 | `AnnotationConfigApplicationContext` 构造时传 parent | 手动管理两套 XML 极易错乱 |

> 💡 **自定义 ApplicationContext 的正确姿势**：99% 的场景继承 `AbstractApplicationContext` 都是错的（十多个抽象钩子要补）；正确的扩展点是**实现 `ApplicationContextInitializer`**（Boot 提供回调）或在 `GenericApplicationContext` 上组合。面试若被问"如何扩展容器"，答"选型 + Initializer + BeanFactoryPostProcessor"比"继承 AbstractApplicationContext"得分高。

### 3.2 上下文关闭与优雅停机

```text
ctx.close() 的执行链（与 refresh 对称的"拆解"）：
  1. 发布 ContextClosedEvent（监听器做清理）
  2. 停止 Lifecycle Bean（LifecycleProcessor.onClose）
  3. 销毁全部单例（按依赖逆序 → @PreDestroy → DisposableBean → destroy-method）
  4. 关闭 BeanFactory（缓存清空）
```

| 关闭方式 | 触发时机 | 适用 |
|---------|---------|------|
| `ctx.close()` | 显式调用 | 代码可控的收尾（测试/工具） |
| `registerShutdownHook()` | JVM 退出钩子 | 常驻进程（main 方法服务） |
| try-with-resources | 块结束 | 局部容器（测试最常用） |
| Boot 的 `SpringApplication.exit` | 应用退出 | Boot 托管（含退出码） |

> ⚠️ **常见坑**：① 单例销毁顺序按依赖逆序，但**循环依赖的 Bean 销毁顺序未定义**——资源清理别依赖顺序；② `@PreDestroy` 抛异常会**中断后续销毁**（默认），清理代码要 try-catch；③ 停机期间新请求到达的竞态——先摘流量（注册中心下线）再 close，顺序反了会出现"处理中请求被打断"。

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

### 4.1 getBean 常见异常速查（面试与排障双用）

| 异常 | 触发条件 | 排查路径 |
|------|---------|---------|
| `NoSuchBeanDefinitionException` | 类型/名字不存在 | ① 是否扫描漏了（basePackages）；② 是否被 @Profile 条件裁掉；③ 类型是接口时是否有实现类注册 |
| `NoUniqueBeanDefinitionException` | 同类型多个 Bean，未指定名字/@Primary | `@Primary` 指定主选；或 `@Qualifier` 按名取；或注入 `Map<String,T>` / `List<T>` |
| `BeanCurrentlyInCreationException` | **构造器循环依赖**（A 构造注入 B、B 构造注入 A） | 单例属性注入循环 Spring 能解（三级缓存），构造器循环**无解**——重构为 setter/`@Lazy` 注入 |
| `BeanCreationException` | 实例化/初始化阶段异常（构造器抛错、@PostConstruct 抛错） | 看 cause，多半是业务代码问题 |
| `BeanNotOfRequiredTypeException` | 类型不匹配（如拿到代理类型强转失败） | 检查代理机制与接口声明（7.0 CGLIB 默认后更常见） |

```java
// 应对"多个同类型 Bean"的三种标准姿势
@Autowired
private List<PaymentHandler> handlers;      // ① 全部注入，按序处理（策略模式）

@Autowired
@Qualifier("alipay")
private PaymentHandler alipay;              // ② 按名字精确取

@Autowired
private Map<String, PaymentHandler> map;    // ③ key=Bean 名，动态路由
```

> ⚠️ **7.0 新增坑**：代理默认 CGLIB 后，`getBean(SomeClass.class)` 返回的是**子类代理**——若代码里做 `x.getClass().isAssignableFrom(...)` 之类精确类型判断会意外失效；接口类型 `getBean(IFace.class)` 则不受影响。生产代码优先面向接口取 Bean。

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

### 5.1 面试追问与答题要点

| 追问 | 答题锚点 | 易错点 |
|------|---------|--------|
| "ApplicationContext 里有哪些 BeanFactory？" | 内部持有 `DefaultListableBeanFactory`，`getBeanFactory()` 暴露；一个上下文一个工厂 | 答"上下文实现了 BeanFactory 接口"不完整——是**持有 + 实现（委托）** |
| "refresh() 能被调用两次吗？" | 不能——`AbstractApplicationContext.refresh()` 会校验 `refreshed` 标志，二次调用抛 `IllegalStateException` | 误以为"重启容器=再 refresh"（正确姿势是 close 后重建） |
| "预加载单例失败会导致什么？" | 第 11 步失败整体启动失败；**前面已创建的单例不清除**，需手动 close | 忽略资源泄漏风险 |
| "父子容器查 Bean 的顺序？" | **先查本级**再上溯父容器；`getBeansOfType` 只查本级（`Listable` 语义），`getBean` 会上溯 | 混淆 `containsBean`（上溯）与 `containsLocalBean`（本级） |
| "为什么 Boot 用 ServletWebServer 上下文？" | 需要第 9 步 `onRefresh()` 创建内嵌服务器 + request/session 作用域，普通注解上下文没有这些钩子 | 答不出"作用域注册在第 4 步、服务器在第 9 步"的时机 |

> 🎯 **一句话总结**：ApplicationContext = 一个被"企业能力"包装起来的 `DefaultListableBeanFactory`；面试只要抓住"**持有并委托工厂、模板方法定义生命周期、钩子留给子类**"三条主线，任何容器题都能组织出结构化答案。

---

**下一模块**：[03-配置类与扫描机制速查](03-配置类与扫描机制速查.md)　**返回总览**：[00-Spring Context组件总览](00-Spring Context组件总览.md)
