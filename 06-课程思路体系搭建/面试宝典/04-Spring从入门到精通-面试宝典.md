# Spring 面试宝典
> 基于课程大纲全面覆盖 Spring 面试高频考点，从 IoC/DI 到 AOP、事务、循环依赖

## 目录
1. [一、基础概念速答](#一基础概念速答15-20题)
2. [二、深度原理剖析](#二深度原理剖析10-15题)
3. [三、实战场景题](#三实战场景题8-12题)
4. [四、手写代码题](#四手写代码题5-8题)
5. [五、系统设计题](#五系统设计题3-5题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板](#七面试回答模板top-5高频题的结构化回答模板)
8. [八、快速查漏补缺 Checklist](#八快速查漏补缺-checklist)

---

## 一、基础概念速答（15-20题）

### 1.1 什么是 Spring 框架？
Spring 是一个轻量级的**控制反转（IoC）**和**面向切面（AOP）**的容器框架。它通过 IoC 容器管理对象的生命周期和依赖关系，通过 AOP 实现横切关注点的分离。Spring 的核心是 IoC 容器和 AOP 模块，此外还包括数据访问、Web MVC、事务管理、消息等模块。

### 1.2 什么是 IoC 和 DI？它们之间是什么关系？
**IoC（控制反转）**：将对象的创建和依赖关系的管理权从程序代码反转给容器。传统上对象自己 `new` 依赖，现在由容器注入。

**DI（依赖注入）**：IoC 的具体实现方式。容器在创建对象时，将依赖的对象通过构造函数、setter 或字段注入的方式传给目标对象。

> 🎯 关系：IoC 是设计思想，DI 是实现方式。**IoC 是目标，DI 是手段。**

### 1.3 IoC 的实现方式有哪些？
| 注入方式 | 说明 | 优点 | 缺点 |
|---------|------|------|------|
| Setter 注入 | 通过 setter 方法注入 | 灵活，可部分注入 | 依赖可变，不够安全 |
| 构造器注入 | 通过构造函数参数注入 | 不可变，保证依赖完整性 | 参数过多时构造器臃肿 |
| 字段注入（@Autowired） | 直接注入字段 | 代码简洁 | 难测试，依赖不明确 |

> 💡 Spring 官方推荐**构造器注入**，因为可以保证依赖不可变且非空。

### 1.4 Bean 的作用域有哪些？
| 作用域 | 说明 | 适用场景 |
|--------|------|---------|
| `singleton` | 默认，容器内仅一个实例 | 无状态 Bean（Service、DAO） |
| `prototype` | 每次获取创建一个新实例 | 有状态 Bean（Model、DTO） |
| `request` | 每次 HTTP 请求一个实例 | Web 应用的请求上下文 |
| `session` | 每个 HTTP Session 一个实例 | Web 应用的会话上下文 |
| `application` | ServletContext 生命周期一个实例 | Web 应用全局共享 |
| `websocket` | 每个 WebSocket 一个实例 | WebSocket 应用 |

### 1.5 Spring Bean 的生命周期（5步/7步/10步）？
**5步**：实例化 → 属性赋值 → 初始化 → 使用 → 销毁
**7步**：增加 BeanPostProcessor 的 postProcessBeforeInitialization 和 postProcessAfterInitialization
**10步**：再增加 Aware 接口回调（BeanNameAware、BeanFactoryAware、ApplicationContextAware）

```java
// 完整的生命周期回调顺序
1. 实例化（构造方法）
2. 属性赋值（setter/字段注入）
3. BeanNameAware.setBeanName()
4. BeanFactoryAware.setBeanFactory()
5. ApplicationContextAware.setApplicationContext()
6. BeanPostProcessor.postProcessBeforeInitialization()
7. @PostConstruct / InitializingBean.afterPropertiesSet()
8. BeanPostProcessor.postProcessAfterInitialization()
   --- 至此 Bean 可以使用 ---
9. @PreDestroy / DisposableBean.destroy()
```

### 1.6 @Autowired 和 @Resource 的区别是什么？
| 特性 | @Autowired | @Resource |
|------|-----------|-----------|
| 来源 | Spring 注解 | Java 标准注解（JSR-250）|
| 注入方式 | 先 byType 再 byName | 先 byName 再 byType |
| 组合 | 常搭配 @Qualifier | 可用 name 属性指定 |
| 推荐度 | Spring 项目推荐 | 通用性更好 |

> 💡 @Autowired 属于 Spring 生态，@Resource 是 JavaEE 标准。如果两个实现都没有指定名称，@Autowired 可能因为多实现报错，需要 @Qualifier 辅助。

### 1.7 ApplicationContext 和 BeanFactory 的区别？
| 特性 | BeanFactory | ApplicationContext |
|------|------------|-------------------|
| 延迟加载 | 默认延迟加载 | 默认立即加载（可以配置懒加载）|
| 事件发布 | 不支持 | 支持事件机制 |
| 国际化 | 不支持 | 支持 MessageSource |
| AOP 支持 | 需手动集成 | 自动集成 |
| 扩展性 | 基础功能 | 包含更多企业级功能 |

### 1.8 FactoryBean 和 BeanFactory 的区别？
**BeanFactory**：Spring 容器的最顶层接口，管理 Bean 的工厂。

**FactoryBean**：工厂 Bean 接口，用于创建复杂对象的工厂。实现 `FactoryBean` 接口的 Bean，容器返回的不是它本身，而是其 `getObject()` 方法返回的对象。

```java
@Component
public class DateFactoryBean implements FactoryBean<Date> {
    @Override
    public Date getObject() {
        return new Date();
    }
    @Override
    public Class<?> getObjectType() {
        return Date.class;
    }
    @Override
    public boolean isSingleton() {
        return false;
    }
}
// 获取 Date 对象即可，DateFactoryBean 隐藏了创建逻辑
```

### 1.9 AOP 中的关键术语有哪些？
| 术语 | 说明 | 类比 |
|------|------|------|
| Aspect | 切面，横切关注点模块 | 事务管理模块 |
| JoinPoint | 连接点，程序执行点 | 方法调用 |
| Pointcut | 切点，匹配连接点的表达式 | 过滤器 |
| Advice | 通知，在特定连接点执行的动作 | before/after |
| Target | 目标对象 | 被代理的对象 |
| Proxy | 代理对象 | 动态生成的类 |
| Weaving | 织入，将切面应用到目标对象的过程 | 代理生成过程 |

### 1.10 Spring AOP 有哪几种通知类型？
```java
@Before("execution(* com.example.service.*.*(..))")
public void before() { }                    // 前置通知

@After("execution(* com.example.service.*.*(..))")
public void after() { }                     // 后置通知（finally）

@AfterReturning("execution(* com.example.service.*.*(..))")
public void afterReturning() { }            // 返回通知

@AfterThrowing("execution(* com.example.service.*.*(..))")
public void afterThrowing() { }             // 异常通知

@Around("execution(* com.example.service.*.*(..))")
public Object around(ProceedingJoinPoint pjp) { // 环绕通知
    // 前置逻辑
    Object result = pjp.proceed();
    // 后置逻辑
    return result;
}
```

### 1.11 JDK 动态代理和 CGLIB 代理的区别是什么？
| 特性 | JDK 动态代理 | CGLIB 代理 |
|------|-------------|-----------|
| 依赖 | 目标必须实现接口 | 对类直接代理 |
| 实现 | InvocationHandler + Proxy | MethodInterceptor + Enhancer |
| 性能（创建）| 快 | 较慢 |
| 性能（调用）| 较慢（反射）| 快（FastClass 机制）|
| 不可代理 | — | final 类和方法 |

> 🎯 Spring 的选择逻辑：如果目标类实现了接口，默认用 JDK 动态代理；否则用 CGLIB。可以通过 `@EnableAspectJAutoProxy(proxyTargetClass=true)` 强制使用 CGLIB。

### 1.12 Spring 事务的传播行为有哪些？
| 传播行为 | 说明 |
|---------|------|
| `REQUIRED` | 默认，支持当前事务，没有则新建 |
| `SUPPORTS` | 支持当前事务，没有则以非事务执行 |
| `MANDATORY` | 必须在一个已有事务中执行，否则抛异常 |
| `REQUIRES_NEW` | 无论当前有无事务，都创建新事务 |
| `NOT_SUPPORTED` | 以非事务方式执行，挂起当前事务 |
| `NEVER` | 以非事务执行，有事务则抛异常 |
| `NESTED` | 嵌套事务（JDBC savepoint 实现）|

### 1.13 Spring 事务的隔离级别有哪些？
| 隔离级别 | 脏读 | 不可重复读 | 幻读 |
|---------|:----:|:---------:|:----:|
| `DEFAULT` | 数据库默认 | 数据库默认 | 数据库默认 |
| `READ_UNCOMMITTED` | 可能 | 可能 | 可能 |
| `READ_COMMITTED` | 不会 | 可能 | 可能 |
| `REPEATABLE_READ` | 不会 | 不会 | 可能 |
| `SERIALIZABLE` | 不会 | 不会 | 不会 |

### 1.14 Spring 中常用的后置处理器有哪些？
| 处理器 | 接口 | 作用 |
|--------|------|------|
| BeanPostProcessor | 初始化前后回调 | 处理 @Autowired、AOP 代理等 |
| BeanFactoryPostProcessor | 容器刷新前回调 | 处理 @PropertySource、配置元数据 |
| BeanDefinitionRegistryPostProcessor | 继承上者 | 添加/修改 BeanDefinition |
| InstantiationAwareBeanPostProcessor | 实例化前后回调 | 控制 Bean 实例化过程 |

### 1.15 Spring 事件机制是怎样的？
Spring 事件机制基于**观察者模式**，包含三个组件：
1. **事件（Event）**：继承 `ApplicationEvent`
2. **发布者（Publisher）**：注入 `ApplicationEventPublisher` 并调用 `publishEvent()`
3. **监听器（Listener）**：`@EventListener` 或实现 `ApplicationListener`

```java
// 1. 自定义事件
public class OrderCreatedEvent extends ApplicationEvent {
    private Long orderId;
    public OrderCreatedEvent(Object source, Long orderId) {
        super(source);
        this.orderId = orderId;
    }
}

// 2. 发布事件
@Service
public class OrderService {
    @Autowired
    private ApplicationEventPublisher publisher;
    public void createOrder() {
        // ... 业务逻辑
        publisher.publishEvent(new OrderCreatedEvent(this, orderId));
    }
}

// 3. 监听事件
@Component
public class OrderEventListener {
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        // 发短信、发邮件等异步处理
    }
}

// 4. 异步执行需加 @Async
@EnableAsync
@Async
@EventListener
public void handleAsync(OrderCreatedEvent event) { ... }
```

### 1.16 Spring 中设计模式的应用有哪些？
| 设计模式 | Spring 中的体现 |
|---------|----------------|
| 工厂模式 | BeanFactory、ApplicationContext |
| 单例模式 | 默认 Bean 作用域 |
| 代理模式 | AOP 代理（JDK/CGLIB）|
| 模板方法模式 | JdbcTemplate、RestTemplate、TransactionTemplate |
| 观察者模式 | 事件驱动模型（ApplicationEvent、EventListener）|
| 策略模式 | Resource 加载、Bean 实例化策略 |
| 适配器模式 | AdvisorAdapter（BeforeAdviceAdapter 等）|
| 装饰器模式 | BeanWrapper、TransactionAwareCacheDecorator |
| 责任链模式 | AOP 拦截器链、过滤器链 |
| 委派模式 | DispatcherServlet（委派给 HandlerMapping）|

---

## 二、深度原理剖析（10-15题）

### 2.1 Spring IoC 容器的工作原理是什么？
```java
// IoC 容器启动流程（简化版）
1. 加载配置文件（XML/注解） → 创建 BeanDefinitionReader
2. 解析配置 → 生成 BeanDefinition 注册到 BeanDefinitionRegistry
3. 调用 BeanFactoryPostProcessor 处理元数据
4. 实例化所有非懒加载的单例 Bean
   4.1 构造器/工厂方法创建 Bean 实例
   4.2 属性赋值（依赖注入）
   4.3 Aware 接口回调
   4.4 BeanPostProcessor 前置处理
   4.5 初始化方法（@PostConstruct / InitializingBean）
   4.6 BeanPostProcessor 后置处理（AOP 代理在此生成）
5. 注册完成，Bean 可用了
```

### 2.2 Spring 如何解决循环依赖？
Spring 通过**三级缓存**解决 `singleton` 作用域下的 setter 注入循环依赖。

```java
public class DefaultSingletonBeanRegistry {
    // 一级缓存：完全创建好的单例 Bean
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);
    
    // 二级缓存：提前暴露的早期 Bean（尚未属性赋值）
    private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(16);
    
    // 三级缓存：单例工厂（用于生成代理对象）
    private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);
}
```

**工作流程**（A 依赖 B，B 依赖 A）：
1. A 开始实例化 → 放入三级缓存（`singletonFactories` 中的 `ObjectFactory`）
2. A 属性赋值时发现依赖 B → 去容器找 B
3. B 开始实例化 → 也放入三级缓存
4. B 属性赋值时发现依赖 A → 从三级缓存拿到 A 的 `ObjectFactory`，调用 `getObject()` 获取 A 的早期引用（移入二级缓存）
5. B 完成创建 → 放入一级缓存
6. A 拿到 B 引用继续 → 属性赋值完成 → 放入一级缓存

> ⚠️ 注意：构造器注入无法解决循环依赖（因为实例化阶段就需要依赖），prototype 作用域也无法解决（不缓存）。

### 2.3 AOP 通知的执行顺序是怎样的？
```
Around Before
    ↓
@Before
    ↓
目标方法执行
    ↓
@AfterReturning（正常返回） 或  @AfterThrowing（异常）
    ↓
@After（finally）
    ↓
Around After
```

> 💡 多个切面时，通过 `@Order` 决定执行顺序。**值越小优先级越高**，但环绕通知的执行顺序是嵌套的（洋葱模型）。

### 2.4 Spring 的事务管理如何实现？
Spring 事务管理的核心是 `TransactionInterceptor`（实现 `MethodInterceptor`），通过 AOP 的环绕通知实现。

```java
// @Transactional 原理流程
1. AOP 拦截 @Transactional 方法
2. TransactionInterceptor.invoke() 被调用
3.   TransactionAspectSupport.createTransactionIfNecessary()
        → 获取事务属性（传播行为、隔离级别等）
        → 从 DataSourceTransactionManager 获取连接
        → 设置 autoCommit=false
        → 绑定到当前线程（TransactionSynchronizationManager）
4. 目标方法执行
5.   正常 → 事务管理器 commit()
     异常 → 根据 rollbackFor 决定是否 rollback()
6. 清理事务资源
```

### 2.5 @Transactional 失效的场景有哪些？
1. **方法内部调用**：`this` 调用本类方法，不走代理
2. **非 public 方法**：@Transactional 只对 public 方法生效
3. **异常未被 Spring 捕获**：异常被 try-catch 吃掉
4. **异常类型错误**：默认只回滚 `RuntimeException` 和 `Error`
5. **传播行为设置不当**：SUPPORTS/NEVER 等不会创建事务
6. **数据库引擎不支持**：如 MyISAM 不支持事务
7. **事务管理器未配置正确**

### 2.6 Spring AOP 代理生成时机是什么？
AOP 代理在 **Bean 初始化后**（`BeanPostProcessor.postProcessAfterInitialization()`）生成。`AbstractAutoProxyCreator` 是核心类，在 Bean 初始化完成后调用 `wrapIfNecessary()` 判断是否需要创建代理。

```java
// AbstractAutoProxyCreator 核心逻辑
public Object postProcessAfterInitialization(Object bean, String beanName) {
    if (bean instanceof AopInfrastructureBean) {
        return bean;
    }
    // 查找匹配的 Advisor，创建代理
    return wrapIfNecessary(bean, beanName, cacheKey);
}
```

### 2.7 Spring 的 Propagation.REQUIRES_NEW 原理是什么？
`REQUIRES_NEW` 会在方法调用时**挂起当前事务，创建一个新事务**。新事务独立提交/回滚，互不影响。底层实现通过 `DataSourceTransactionManager` 的 `doBegin()` 获取新的数据库连接，再通过 `TransactionSynchronizationManager` 挂起原有的事务资源。

### 2.8 Spring 如何管理 MyBatis 的事务？
Spring 通过 `SqlSessionTemplate` 整合 MyBatis，其内部使用 `SqlSessionInterceptor`。事务管理通过 `DataSourceUtils` 获取与当前事务绑定的 Connection，从而保证 MyBatis 操作与 Spring 事务是统一的。

### 2.9 BeanPostProcessor 和 BeanFactoryPostProcessor 的区别？
```java
// BeanPostProcessor —— 操作 Bean 实例（Bean 创建后调用）
@Component
public class MyBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean; // AOP 代理在此阶段创建
    }
}

// BeanFactoryPostProcessor —— 操作 BeanDefinition（Bean 创建前调用）
@Component
public class MyBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory factory) {
        // 修改 BeanDefinition 属性
        BeanDefinition bd = factory.getBeanDefinition("dataSource");
        bd.getPropertyValues().add("maxActive", 20);
    }
}
```

### 2.10 Spring 中 @Configuration 和 @Component 的区别？
`@Configuration` 内部使用 `CGLIB` 生成代理，保证 `@Bean` 方法返回的单例是容器管理的。`@Component` 不做代理，每次调用 `@Bean` 方法都会新建对象。

```java
@Configuration
public class AppConfig {
    @Bean
    public A a() {
        return new A(b()); // CGLIB 代理保证 b() 返回的是同一个单例
    }
    @Bean
    public B b() {
        return new B();
    }
}
```

### 2.11 Spring 内建的 BeanPostProcessor 有哪些？
| 处理器 | 作用 |
|--------|------|
| `AutowiredAnnotationBeanPostProcessor` | 处理 @Autowired、@Value |
| `CommonAnnotationBeanPostProcessor` | 处理 @Resource、@PostConstruct、@PreDestroy |
| `ApplicationContextAwareProcessor` | 处理 Aware 接口 |
| `AsyncAnnotationBeanPostProcessor` | 处理 @Async |
| `PersistenceAnnotationBeanPostProcessor` | 处理 @PersistenceUnit、@PersistenceContext |

### 2.12 Spring 的 Aware 接口体系
Aware 接口是 Spring 为 Bean 提供访问容器内部能力的接口体系。

```java
public interface Aware {} // 标记接口

// 常用子接口
BeanNameAware           → 获取 Bean 名称
BeanFactoryAware        → 获取 BeanFactory
ApplicationContextAware → 获取 ApplicationContext
EnvironmentAware        → 获取 Environment
ResourceLoaderAware     → 获取 ResourceLoader
ApplicationEventPublisherAware → 获取事件发布器
MessageSourceAware      → 获取国际化资源
```

---

## 三、实战场景题（8-12题）

### 3.1 项目中有多个 DataSource，如何配置事务管理器？
```java
@Configuration
public class DataSourceConfig {
    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.secondary")
    public DataSource secondaryDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Bean
    @Primary
    public PlatformTransactionManager primaryTransactionManager(
            @Qualifier("primaryDataSource") DataSource ds) {
        return new DataSourceTransactionManager(ds);
    }
    
    @Bean
    public PlatformTransactionManager secondaryTransactionManager(
            @Qualifier("secondaryDataSource") DataSource ds) {
        return new DataSourceTransactionManager(ds);
    }
}
// 使用 @Transactional("primaryTransactionManager") 指定事务管理器
```

### 3.2 如何保证缓存和数据库的事务一致性？
```java
// 方案：使用 @Transactional 保证操作原子性
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private RedisTemplate redisTemplate;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public User updateUser(User user) {
        // 1. 更新数据库
        userMapper.updateById(user);
        // 2. 删除缓存（先更新 DB 后删缓存而非先删缓存）
        redisTemplate.delete("user:" + user.getId());
        // 3. 如果 DB 更新失败，事务回滚，不会出现 DB 与缓存不一致
        return user;
    }
}
```

### 3.3 Async 注解的使用和注意事项
```java
@EnableAsync // 在配置类上开启异步支持
@SpringBootApplication
public class Application { }

@Service
public class NotificationService {
    @Async // 在方法上标注异步执行
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompletableFuture<Void> sendEmail(Long userId) {
        // 异步发送邮件（独立事务）
        return CompletableFuture.completedFuture(null);
    }
}

// ⚠️ 注意事项：
// 1. 默认使用 SimpleAsyncTaskExecutor（不推荐生产），需自定义线程池
// 2. 方法不能是 private
// 3. 同类方法调用 @Async 失效（AOP 代理问题）
// 4. 异步方法默认事务失效（不同线程事务上下文不传递）
```

### 3.4 Spring 中如何实现多线程并发事务控制？
```java
@Configuration
public class ThreadPoolConfig {
    @Bean("businessExecutor")
    public Executor businessExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("biz-");
        executor.setRejectedExecutionHandler(new CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}

// 子线程事务需手动控制
@Service
public class BatchService {
    @Autowired
    private PlatformTransactionManager transactionManager;
    
    public void batchProcess(List<Data> list) {
        list.parallelStream().forEach(data -> {
            DefaultTransactionDefinition def = new DefaultTransactionDefinition();
            def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            TransactionStatus status = transactionManager.getTransaction(def);
            try {
                // 子线程的业务操作
                transactionManager.commit(status);
            } catch (Exception e) {
                transactionManager.rollback(status);
            }
        });
    }
}
```

### 3.5 Spring 中如何使用自定义注解 + AOP 实现权限校验？
```java
// 1. 自定义注解
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {
    String value();
}

// 2. AOP 切面
@Aspect
@Component
public class PermissionAspect {
    @Around("@annotation(requiresPermission)")
    public Object checkPermission(ProceedingJoinPoint pjp, RequiresPermission requiresPermission) {
        String permission = requiresPermission.value();
        String currentUserRole = SecurityContextHolder.getContext()
                .getAuthentication().getAuthorities().toString();
        if (!currentUserRole.contains(permission)) {
            throw new AccessDeniedException("无权限访问");
        }
        return pjp.proceed();
    }
}
```

### 3.6 项目中 Spring Boot 结合 JDK 动态代理影响类型转换的场景如何解决？
```java
// 场景：Controller 强制转为实现类而不是接口
// 解决方式一：启动类添加 @EnableAspectJAutoProxy(proxyTargetClass = true)
// 解决方式二：使用接口接收返回类型

@SpringBootApplication
@EnableAspectJAutoProxy(proxyTargetClass = true) // 强制使用 CGLIB
public class Application { }
```

### 3.7 如何处理 Spring 事务中超时和只读事务？
```java
@Service
public class ReportService {
    // 只读事务，优化数据库连接
    @Transactional(readOnly = true, timeout = 30)
    public List<Report> generateReport(Date start, Date end) {
        return reportMapper.queryReport(start, end);
    }
    
    // 超时控制，超过 5 秒自动回滚
    @Transactional(timeout = 5, propagation = Propagation.REQUIRES_NEW)
    public void slowProcess() {
        // 如果执行超过 5 秒，抛出 TransactionTimedOutException
    }
}
```

### 3.8 当 @Autowired 注入接口有多个实现时如何处理？
```java
// 方式一：@Primary 标记首选
@Component
@Primary
public class MysqlUserDao implements UserDao { }

// 方式二：@Qualifier 指定名称
@Service
public class UserService {
    @Autowired
    @Qualifier("oracleUserDao")
    private UserDao userDao;
}

// 方式三：注入 List 或 Map
@Component
public class UserHandler {
    @Autowired
    private List<UserDao> userDaos;  // 注入所有实现
    @Autowired
    private Map<String, UserDao> userDaoMap; // Bean 名为 key
}

// 方式四：创建自定义注解
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Qualifier
public @interface DbType { String value(); }
```

---

## 四、手写代码题（5-8题）

### 4.1 手写 Spring IoC 容器（极简版）
```java
// 1. 注解定义
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyComponent {}

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyAutowired {}

// 2. 简易 IoC 容器
public class SimpleIoCContainer {
    private final Map<String, Object> beans = new ConcurrentHashMap<>();
    
    public void scanPackage(String basePackage) throws Exception {
        // 扫描包下所有类
        String path = basePackage.replace(".", "/");
        Enumeration<URL> resources = Thread.currentThread()
                .getContextClassLoader().getResources(path);
        while (resources.hasMoreElements()) {
            File file = new File(resources.nextElement().toURI());
            for (File classFile : file.listFiles(f -> f.getName().endsWith(".class"))) {
                String className = basePackage + "." + classFile.getName().replace(".class", "");
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(MyComponent.class)) {
                    String beanName = lowerFirst(clazz.getSimpleName());
                    Object instance = clazz.getDeclaredConstructor().newInstance();
                    beans.put(beanName, instance);
                }
            }
        }
        // 注入依赖
        for (Object bean : beans.values()) {
            for (Field field : bean.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(MyAutowired.class)) {
                    field.setAccessible(true);
                    Object dependency = beans.get(lowerFirst(field.getType().getSimpleName()));
                    field.set(bean, dependency);
                }
            }
        }
    }
    
    public <T> T getBean(String name) {
        return (T) beans.get(name);
    }
}
```

### 4.2 手写 Spring AOP 切面实现日志记录
```java
@Aspect
@Component
public class LoggingAspect {
    
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    // 切点：Service 层所有方法
    @Pointcut("execution(* com.example.service.*.*(..))")
    public void servicePointcut() {}
    
    @Around("servicePointcut()")
    public Object logExecutionTime(ProceedingJoinPoint pjp) throws Throwable {
        String methodName = pjp.getSignature().getName();
        String className = pjp.getTarget().getClass().getSimpleName();
        
        // 记录入参
        Object[] args = pjp.getArgs();
        log.info("[{}.{}] 开始执行，参数: {}", className, methodName, args);
        
        long start = System.currentTimeMillis();
        Object result;
        try {
            result = pjp.proceed();
            long cost = System.currentTimeMillis() - start;
            log.info("[{}.{}] 执行完成，耗时 {}ms，返回: {}", className, methodName, cost, result);
            return result;
        } catch (Exception e) {
            log.error("[{}.{}] 执行异常: {}", className, methodName, e.getMessage(), e);
            throw e;
        }
    }
}
```

### 4.3 手写 Spring 事务管理（编程式事务）
```java
@Service
public class UserService {
    @Autowired
    private PlatformTransactionManager transactionManager;
    
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        def.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        
        TransactionStatus status = transactionManager.getTransaction(def);
        try {
            accountMapper.decrease(fromId, amount);
            accountMapper.increase(toId, amount);
            transactionManager.commit(status);
        } catch (Exception e) {
            transactionManager.rollback(status);
            throw e;
        }
    }
}
```

### 4.4 手写基于 XML 的 Spring Bean 配置
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="
           http://www.springframework.org/schema/beans
           http://www.springframework.org/schema/beans/spring-beans.xsd">
    
    <!-- Setter 注入 -->
    <bean id="userService" class="com.example.service.UserServiceImpl">
        <property name="userDao" ref="userDao"/>
        <property name="maxRetry" value="3"/>
    </bean>
    
    <!-- 构造器注入 -->
    <bean id="userDao" class="com.example.dao.UserDaoImpl">
        <constructor-arg name="dataSource" ref="dataSource"/>
    </bean>
    
    <!-- 内部 Bean -->
    <bean id="orderService" class="com.example.service.OrderServiceImpl">
        <property name="dataSource">
            <bean class="com.zaxxer.hikari.HikariDataSource">
                <property name="jdbcUrl" value="jdbc:mysql://localhost:3306/db"/>
            </bean>
        </property>
    </bean>
    
    <!-- 集合注入 -->
    <bean id="service" class="com.example.Service">
        <property name="list">
            <list>
                <value>item1</value>
                <value>item2</value>
            </list>
        </property>
        <property name="map">
            <map>
                <entry key="key1" value="val1"/>
            </map>
        </property>
    </bean>
</beans>
```

### 4.5 手写 @Cacheable 注解的简易实现
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyCacheable {
    String key();
    long ttl() default 300000; // 默认 5 分钟
}

@Aspect
@Component
public class MyCacheAspect {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Around("@annotation(myCacheable)")
    public Object around(ProceedingJoinPoint pjp, MyCacheable myCacheable) throws Throwable {
        String key = myCacheable.key();
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }
        // 加分布式锁防止缓存击穿
        synchronized (key.intern()) {
            cached = redisTemplate.opsForValue().get(key);
            if (cached != null) return cached;
            
            Object result = pjp.proceed();
            redisTemplate.opsForValue().set(key, result, myCacheable.ttl(), TimeUnit.MILLISECONDS);
            return result;
        }
    }
}
```

---

## 五、系统设计题（3-5题）

### 5.1 如何设计一个支持动态数据源切换的框架？
```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataSource {
    String value() default "master";
}

// 1. 动态数据源持有者
public class DynamicDataSourceContextHolder {
    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();
    public static void set(String ds) { CONTEXT.set(ds); }
    public static String get() { return CONTEXT.get(); }
    public static void clear() { CONTEXT.remove(); }
}

// 2. 动态数据源
public class DynamicDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        return DynamicDataSourceContextHolder.get();
    }
}

// 3. AOP 切面切换
@Aspect
@Component
public class DataSourceAspect {
    @Around("@annotation(dataSource)")
    public Object around(ProceedingJoinPoint pjp, DataSource dataSource) throws Throwable {
        DynamicDataSourceContextHolder.set(dataSource.value());
        try {
            return pjp.proceed();
        } finally {
            DynamicDataSourceContextHolder.clear();
        }
    }
}
```

### 5.2 设计一个通用的 Spring 配置中心客户端
```java
@Component
public class ConfigCenterClient implements InitializingBean {
    @Autowired
    private Environment environment;
    
    // 监听配置变更（模拟 Apollo/Nacos）
    @EventListener
    public void onConfigChange(ConfigChangeEvent event) {
        // 刷新 Bean 中 @Value 的值
        for (String key : event.changedKeys()) {
            String newValue = event.getChange(key).getNewValue();
            System.setProperty(key, newValue);
        }
    }
    
    public String getConfig(String key, String defaultValue) {
        return environment.getProperty(key, defaultValue);
    }
}
```

### 5.3 如何使 @Async 使用自定义线程池并实现优雅关闭？
```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("async-");
        executor.setWaitForTasksToCompleteOnShutdown(true); // 优雅关闭
        executor.setAwaitTerminationSeconds(60); // 等待 60 秒
        executor.setRejectedExecutionHandler(new CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
    
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            log.error("异步方法 [{}] 异常: {}", method.getName(), ex.getMessage(), ex);
        };
    }
}
```

---

## 六、常见坑点与最佳实践（表格）

| 坑点 | 问题描述 | 解决方案 | 最佳实践 |
|------|---------|---------|---------|
| @Transactional 同类方法调用失效 | this 调用不走代理 | 注入自身 Bean 或使用 AopContext.currentProxy() | 将事务方法放在不同 Service 中 |
| try-catch 吃掉了异常 | 事务无法检测到异常回滚 | 在 catch 中手动 `TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()` | catch 块处理可控异常后重新 throw |
| @Autowired 字段注入单元测试难 | 无法 Mock 字段注入 | 使用构造器注入 | 统一使用构造器注入 |
| 循环依赖 prototpe 模式 | prototype 作用域无法解决循环依赖 | 避免 prototype 互相引用 | 尽量用 singleton 加 setter 注入 |
| AOP 代理导致 @PostConstruct 执行两次 | 代理对象重新执行初始化 | 使用 `InitializingBean.afterPropertiesSet()` | 尽可能在代理中不使用 @PostConstruct |
| 懒加载+Lazy 导致 NullPointerException | 代理对象创建后未完成依赖注入 | 确保在调用时才访问 | 避免在构造方法中使用依赖 Bean |
| 多线程下 Transactional 失效 | 事务绑定在主线程上 | 编程式事务手动控制子线程 | 子线程使用编程式事务 |
| @Value 注入 static 字段失败 | Spring 不支持 static 字段注入 | 用 setter 方法间接注入 | 不要在 static 字段上用 @Value |
| 循环依赖滚雪球 | 循环引用链太长导致异常 | 重构设计，提取中间层 | 合理分层避免不必要的循环引用 |
| Async + Transactional 同时使用异步事务失效 | 异步方法的 @Transactional 在独立线程执行 | 在异步方法内部使用编程式事务 | 谨慎混合使用，明确事务边界 |

---

## 七、面试回答模板（Top 5高频题的结构化回答模板）

### 7.1 说说 Spring IoC 和 DI
**回答结构**（概念 → 实现 → 源码）：
1. **定义**：IoC 是控制反转，对象创建权交给容器；DI 是依赖注入，容器负责装配
2. **实现方式**：构造器注入（推荐）、setter 注入、字段注入
3. **原理**：BeanDefinition 描述 Bean → 反射实例化 → populateBean 属性填充
4. **源码级**：`AbstractApplicationContext.refresh()` → `finishBeanFactoryInitialization()` → `getBean()` → `doGetBean()` → `createBean()` → `doCreateBean()`

### 7.2 Spring Bean 生命周期
**三步记忆法**（实例化 → 初始化 → 销毁）：
1. **实例化**：构造方法/工厂创建
2. **属性赋值**：依赖注入
3. **Aware 回调**：名称 → 工厂 → 上下文
4. **BeanPostProcessor**：BeforeInit → @PostConstruct/InitializingBean → AfterInit
5. **Bean 就绪**
6. **销毁**：@PreDestroy → DisposableBean → destroy-method

### 7.3 Spring 如何解决循环依赖
**标准回答模板**：
1. **场景**：setter 注入 + singleton 下 A 依赖 B、B 依赖 A
2. **三级缓存**：singletonObjects（成品）、earlySingletonObjects（半成品）、singletonFactories（工厂）
3. **流程**：A 创建 → 三级缓存放工厂 → 注入 B → B 创建 → 三级缓存拿工厂 → B 注入 A 早期引用 → B 完成 → A 完成
4. **不能解决**：构造器注入（实例化时就抛错）、prototype 作用域（无缓存）
5. **为什么三级**：需要 `ObjectFactory` 在 AOP 场景下提前生成代理对象，两级缓存只够非 AOP 场景

### 7.4 Spring AOP 的实现原理
**先分类再深入**：
1. **底层机制**：JDK 动态代理（接口）或 CGLIB（类）
2. **织入时机**：Bean 初始化后（`BeanPostProcessor.postProcessAfterInitialization`）
3. **核心类**：`AbstractAutoProxyCreator` → `wrapIfNecessary()` → 查找 Advisor → 创建代理
4. **调用链**：代理对象 → `JdkDynamicAopProxy.invoke()` → `ReflectiveMethodInvocation.proceed()` 责任链执行
5. **Spring 选择**：有接口用 JDK，没接口或用 CGLIB 强制用 CGLIB

### 7.5 @Transactional 原理和失效场景
**结构**：
1. **原理**：AOP 环绕通知 → `TransactionInterceptor` → `PlatformTransactionManager` → 数据库 Connection
2. **流程**：创建事务 → 绑定到线程 → 执行目标方法 → 提交/回滚 → 清理资源
3. **失效场景**（5 种以上）：
   - 同类方法自调用（this.xxx）
   - 非 public 方法
   - 异常被 catch
   - rollbackFor 指定错误异常类型
   - 传播行为设置不当

---

## 八、快速查漏补缺 Checklist

- [ ] IoC/DI 概念及三种注入方式
- [ ] Bean 生命周期（5步/7步/10步）
- [ ] Bean 作用域（singleton/prototype/request/session）
- [ ] 三级缓存解决循环依赖
- [ ] AOP 术语（JoinPoint/Pointcut/Advice/Aspect）
- [ ] JDK 动态代理 vs CGLIB（原理 + 选择逻辑）
- [ ] Spring 事务传播行为（7种）
- [ ] Spring 事务隔离级别（5种）
- [ ] @Transactional 失效场景
- [ ] @Autowired vs @Resource
- [ ] FactoryBean vs BeanFactory
- [ ] ApplicationContext vs BeanFactory
- [ ] BeanPostProcessor vs BeanFactoryPostProcessor
- [ ] Spring 事件机制（Event/Publish/Listener）
- [ ] Aware 接口体系
- [ ] Spring 设计模式（工厂/单例/代理/模板/观察者/策略/适配器）
- [ ] @Configuration vs @Component 代理区别
- [ ] @Async 使用注意事项
- [ ] 编程式事务 vs 声明式事务
- [ ] Spring 整合 MyBatis 原理
- [ ] 多数据源配置与切换
- [ ] Spring 扩展点（ApplicationListener/ImportBeanDefinitionRegistrar/BeanDefinitionRegistryPostProcessor）
