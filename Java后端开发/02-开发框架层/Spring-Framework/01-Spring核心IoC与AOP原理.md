# Spring Framework 核心原理：IoC 与 AOP 深度解析

> 本文面向 Java 中高级开发者，系统性地剖析 Spring 框架的两大核心基石——控制反转（IoC）与面向切面编程（AOP），涵盖设计思想、源码原理、实战技巧与面试高频考点。

---

## 目录

1. [IoC（控制反转）与 DI（依赖注入）](#1-ioc控制反转与-di依赖注入)
2. [Bean 生命周期（重点）](#2-bean-生命周期重点)
3. [Bean 作用域](#3-bean-作用域)
4. [AOP（面向切面编程）](#4-aop面向切面编程)
5. [事务管理](#5-事务管理)
6. [Spring 扩展点](#6-spring-扩展点)
7. [Spring 面试核心问题 Checklist](#7-spring-面试核心问题-checklist)

---

## 1. IoC（控制反转）与 DI（依赖注入）

### 1.1 核心思想：从"主动创建"到"被动注入"

在传统 Java 开发中，对象之间的依赖关系由程序员在代码中主动控制——通过 `new` 关键字直接创建依赖对象。这种方式的弊端随着系统规模的扩大而迅速暴露：

```java
// 传统方式：上层直接依赖具体实现，耦合度极高
public class UserService {
    private UserDao userDao = new UserDaoImpl(); // 硬编码具体实现

    public void register(User user) {
        userDao.save(user);
    }
}
```

上述代码中，`UserService` 不仅承担了自己的业务逻辑，还负责创建 `UserDao` 的实例。一旦 `UserDao` 的实现发生变化（例如从 MySQL 切换到 MongoDB），`UserService` 的代码也必须修改——这就是**侵入式设计**的典型问题。

**控制反转（Inversion of Control, IoC）** 的核心思想是：**将对象创建和依赖管理的控制权从应用程序代码转移到容器**。程序不再主动 new 对象，而是由 IoC 容器负责实例化和管理 Bean 的生命周期，并在需要时将其注入到依赖方。

这一思想在实践中被概括为 **好莱坞原则（Hollywood Principle）**——"Don't call us, we'll call you"（别打给我们，我们会打给你）。你的代码不需要主动调用容器来获取对象，容器会在合适的时机把对象"推送"给你。

```java
// IoC 方式：被动接收依赖，关注点分离
@Service
public class UserService {
    private final UserDao userDao;

    // 构造器注入——由容器负责传入具体实现
    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    public void register(User user) {
        userDao.save(user);
    }
}
```

### 1.2 IoC 容器：BeanFactory vs ApplicationContext

Spring IoC 容器的顶层接口是 `BeanFactory`，而日常开发中使用更多的是它的子接口 `ApplicationContext`。两者存在清晰的层次关系：

#### BeanFactory —— 基础设施

`BeanFactory` 是 Spring IoC 容器的**最基础接口**，定义了容器的最基本功能——管理 Bean 的定义、注册和获取。

```java
// BeanFactory 的核心方法（简化版）
public interface BeanFactory {
    Object getBean(String name);
    <T> T getBean(Class<T> requiredType);
    boolean containsBean(String name);
    boolean isSingleton(String name);
    boolean isPrototype(String name);
}
```

**核心特征**：
- **懒加载**：Bean 只有在通过 `getBean()` 首次被获取时才进行实例化和初始化。这意味着启动时很快，但第一次请求时会经历完整的 Bean 创建流程，存在一定的性能开销。
- **轻量级**：在内存受限的环境（如 Android）中可以仅使用 BeanFactory。
- **无事件机制**：不支持事件发布/监听模式，不提供国际化支持。
- 典型实现：`XmlBeanFactory`（Spring 3.1 后标记为废弃）、`DefaultListableBeanFactory`。

#### ApplicationContext —— 企业级容器

`ApplicationContext` 继承自 `BeanFactory`，在其基础上增加了大量企业级功能：

```java
public interface ApplicationContext extends BeanFactory, MessageSource,
        ApplicationEventPublisher, ResourcePatternResolver {
    // 继承了多个功能接口
}
```

**ApplicationContext 对 BeanFactory 的增强**：
1. **预初始化**：默认情况下，ApplicationContext 在启动时会立即实例化所有单例 Bean（可通过 `lazy-init` 改变），这有助于尽早发现配置错误。
2. **国际化（i18n）**：通过实现 `MessageSource` 接口支持多语言消息解析，在配置 `messagesource` Bean 后即可在代码中通过 `MessageSource#getMessage()` 获取国际化文本。
3. **事件发布机制**：通过 `ApplicationEventPublisher` 接口支持事件驱动编程，Bean 之间可以解耦地进行通信。
4. **资源访问**：通过 `ResourceLoader` 接口提供统一的资源加载方式（file://, classpath:, URL 等）。
5. **AOP 整合**：更方便地与 Spring AOP 功能集成。

**常见实现类**：
- `ClassPathXmlApplicationContext`：从 classpath 加载 XML 配置文件。
- `FileSystemXmlApplicationContext`：从文件系统路径加载 XML 配置文件。
- `AnnotationConfigApplicationContext`：基于 Java 注解配置的容器（Spring 3.0+ 推荐使用）。
- `WebApplicationContext`：专为 Web 应用设计的容器，在 Servlet 环境下使用。

```java
// 三种启动方式的对比
// 1. XML 配置（传统方式）
ApplicationContext ctx = new ClassPathXmlApplicationContext("applicationContext.xml");

// 2. 注解配置（现代方式）
ApplicationContext ctx = new AnnotationConfigApplicationContext(AppConfig.class);

// 3. 启动类获取（Spring Boot 方式——本质仍是 AnnotationConfigApplicationContext）
SpringApplication.run(Application.class, args);
```

### 1.3 Bean 定义方式

Spring IoC 容器管理的最小单位是 Bean，而 Bean 的定义方式历经了从 XML 到注解再到 Java Config 的演进。

#### 方式一：XML 配置（传统，适用于第三方类库）

```xml
<!-- beans.xml -->
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
           http://www.springframework.org/schema/beans/spring-beans.xsd">

    <!-- 直接定义 Bean -->
    <bean id="userDao" class="com.example.dao.UserDaoImpl"/>

    <!-- 带构造器参数注入 -->
    <bean id="userService" class="com.example.service.UserService">
        <constructor-arg ref="userDao"/>
    </bean>

    <!-- 带属性 Setter 注入 -->
    <bean id="userController" class="com.example.controller.UserController">
        <property name="userService" ref="userService"/>
    </bean>
</beans>
```

XML 配置的优势在于**无需重新编译即可修改配置**，特别适合管理第三方依赖的 Bean 定义。但缺点也很明显：**配置冗长、类型不安全、难以重构**。

#### 方式二：注解 + 组件扫描（主流，适用于内部代码）

Spring 2.5 引入了注解驱动的 Bean 定义，配合 `@ComponentScan` 实现自动化配置。

**Bean 定义注解**：
- `@Component`：通用组件注解，任何被 Spring 管理的 Bean 都可以使用。
- `@Service`：标注业务层组件。语义上指向 Service 层，但本质与 `@Component` 相同，可以互换。区分在于：AOP 切面、一些框架的后置处理器可能会针对 `@Service` 做特殊处理。
- `@Repository`：标注数据访问层组件。相比 `@Component`，Spring 会为 `@Repository` 标注的 Bean 自动添加持久化异常转换（`PersistenceExceptionTranslationPostProcessor`），将原生的 SQL 异常转换为 Spring 统一的 `DataAccessException` 体系。
- `@Controller`：标注 Web 层组件。Spring MVC 会扫描该注解并建立请求映射。

```java
// 配置类：开启组件扫描
@Configuration
@ComponentScan(basePackages = "com.example")
public class AppConfig {
}

// SERVICE 层
@Service
public class UserService {
    @Autowired
    private UserDao userDao;
}

// DAO 层
@Repository
public class UserDaoImpl implements UserDao {
    // 基于 JdbcTemplate 或 JPA 的数据访问实现
}
```

#### 方式三：@Bean + Java Config（灵活，适用于第三方类库与复杂配置）

Spring 3.0 引入了基于 Java 类的配置方式，结合 `@Configuration` 和 `@Bean` 注解，兼具 XML 的灵活性和注解的类型安全：

```java
@Configuration
public class AppConfig {

    @Bean
    public DataSource dataSource() {
        // 可以通过代码灵活创建复杂对象
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:mysql://localhost:3306/db");
        ds.setUsername("root");
        ds.setPassword("password");
        ds.setMaximumPoolSize(20);
        return ds;
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
```

在 `@Bean` 方法中，参数列表中的对象会自动被容器注入（无需额外的 `@Autowired`），这是因为 Spring 在解析 `@Bean` 方法时，会自动将参数视为对容器中其他 Bean 的引用。

### 1.4 依赖注入的三种方式

依赖注入（Dependency Injection, DI）是 IoC 的具体实现方式。Spring 支持三种注入形式，各有优劣。

#### 构造器注入（Constructor Injection）—— 官方推荐

```java
@Service
public class UserService {

    private final UserDao userDao;
    private final EmailService emailService;

    public UserService(UserDao userDao, EmailService emailService) {
        this.userDao = userDao;
        this.emailService = emailService;
    }
}
```

**优势**：
- **不可变性**：依赖字段可以声明为 `final`，确保一旦初始化就不会被修改，符合不可变对象的设计原则。
- **非空保证**：当 Spring 创建 Bean 时，必须提供所有构造器参数。如果容器中找不到对应的依赖，启动阶段就会抛出 `NoSuchBeanDefinitionException`，而不是在运行时才暴露问题——这被称为"快速失败"（fail-fast）。
- **便于测试**：构造器参数在单元测试中可以直接通过 `new UserService(mockUserDao, mockEmailService)` 传入 Mock 对象，无需依赖 Spring 容器。

**适用场景**：**强依赖、必须的依赖**——推荐作为默认选择。

#### Setter 注入（Setter Injection）

```java
@Service
public class UserService {

    private UserDao userDao;

    @Autowired
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }
}
```

**优势**：
- 可选依赖：如果依赖不是必须的，Setter 注入允许 Bean 先被创建，依赖后续再设置。
- 解决循环依赖：Setter 注入配合三级缓存可以解决单例 Bean 的循环引用问题（构造器注入则不能，详见循环依赖章节）。

**劣势**：
- 字段无法声明为 `final`，对象在创建后可能处于不完整的状态。
- 如果依赖被修改，可能导致运行时问题。

**适用场景**：**可选的、有默认值的依赖**（如可以在无 Logger 时静默输出）。

#### 字段注入（Field Injection）—— 不推荐

```java
@Service
public class UserService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private EmailService emailService;
}
```

**为何不推荐**（这也是 IntelliJ IDEA 默认会对 `@Autowired` 字段给出警告的原因）：
1. **无法声明为 `final`**：无法保证字段的不可变性。
2. **难以测试**：在单元测试中无法通过 `new` 直接注入 Mock 对象，必须依赖 Spring 容器或通过反射强行设置私有字段（如 `TestReflectionUtils.setField`）。
3. **反射破坏封装**：`@Autowired` 在字段上的注入是通过 `Field.setAccessible(true)` 强制修改私有字段的值——这本质上破坏了对象的封装性。
4. **隐藏依赖**：字段注入隐藏了类的依赖关系，从构造器签名无法直观看出该类需要哪些依赖。当依赖过多时，不易察觉类是否违反了"单一职责原则"。

#### @Resource vs @Autowired

在 Spring 中，除了 `@Autowired`，还存在 `@Resource`（javax.annotation.Resource / jakarta.annotation.Resource），两者存在重要区别：

| 对比项 | @Autowired | @Resource |
|--------|-----------|-----------|
| 来源 | Spring 定义 | JSR-250 规范 |
| 注入策略 | 先按类型（byType）匹配 | 先按名称（byName）匹配 |
| 空值安全 | 默认依赖必须存在 | 同左（可通过 `required` 调整） |
| 多实现处理 | 需配合 @Qualifier | 指定 name 属性 |

```java
// @Autowired：先按类型匹配，匹配到多个再按名称
@Autowired
private UserDao userDao;  // 如果容器中有且只有一个 UserDao 类型，则注入成功

// @Resource：先按名称匹配，如果找不到再按类型
@Resource(name = "userDaoImpl")
private UserDao userDao;  // 按 Bean 的 id="userDaoImpl" 匹配
```

### 1.5 注入多个实现与冲突解决

当一个接口存在多个实现类时，Spring 在注入时会面临"到底注入哪一个"的问题。

```java
public interface PaymentService {
    void pay(Order order);
}

@Component
public class AlipayService implements PaymentService { ... }

@Component
public class WechatPayService implements PaymentService { ... }
```

此时尝试直接 `@Autowired` 一个 `PaymentService` 会抛出 `NoUniqueBeanDefinitionException`，因为在容器中找到了两个符合 `PaymentService` 类型的 Bean。

#### 解决方案一：@Primary（首选实现）

```java
@Component
@Primary  // 标记为首选注入实现
public class AlipayService implements PaymentService { ... }
```

`@Primary` 告诉 Spring：当有多个类型匹配的 Bean 时，优先选择被 `@Primary` 标记的那个。这适合明确"默认实现"的场景。

#### 解决方案二：@Qualifier（精确指定）

```java
@Service
public class OrderService {

    @Autowired
    @Qualifier("wechatPayService")  // 按 Bean 名称精确指定
    private PaymentService paymentService;
}
```

`@Qualifier` 的值默认为 Bean 的类名（首字母小写），也可以在 `@Component("customName")` 中自定义。

#### 解决方案三：收集所有实现（策略模式）

```java
@Service
public class OrderService {

    // 注入该类型的所有 Bean，形成一个 List
    @Autowired
    private List<PaymentService> paymentServices;

    // 或者注入 Map，key 为 Bean 名称
    @Autowired
    private Map<String, PaymentService> paymentServiceMap;

    public void processPayment(Order order, String channel) {
        // 根据 channel 动态选择实现
        PaymentService service = paymentServiceMap.get(channel + "PayService");
        if (service != null) {
            service.pay(order);
        }
    }
}
```

这种注入方式本质上实现了**策略模式 + 工厂模式的结合**，无需手动维护一个 `Map<String, PaymentService>`，Spring 自动帮我们完成了收集。

---

## 2. Bean 生命周期（重点）

Bean 的生命周期是 Spring 框架最核心的知识点之一，也是面试中的高频重点。理解 Bean 的完整生命历程，是理解 AOP、事务管理、循环依赖等高级特性的基础。

### 2.1 完整生命周期流程图

```mermaid
graph TD
    A[实例化 Instantiation] --> B[属性填充 Populate]
    B --> C[BeanNameAware.setBeanName]
    C --> D[BeanFactoryAware.setBeanFactory]
    D --> E[ApplicationContextAware.setApplicationContext]
    E --> F[BeanPostProcessor.postProcessBeforeInitialization]
    F --> G[@PostConstruct / initMethod]
    G --> H[InitializingBean.afterPropertiesSet]
    H --> I[BeanPostProcessor.postProcessAfterInitialization]
    I --> J[AOP 代理生成 - 在 postProcessAfterInitialization 中完成]
    J --> K[Bean 就绪 - 放入 singletonObjects]
    K --> L[业务方法调用]
    L --> M[容器关闭]
    M --> N[@PreDestroy / destroyMethod]
    N --> O[DisposableBean.destroy]
```

### 2.2 各阶段详细解析

#### 阶段一：实例化（Instantiation）

Spring 通过反射创建 Bean 的实例。这一阶段仅仅是调用了构造器，**尚未设置任何属性值**。对于单例 Bean，Spring 会走**三级缓存**中的 `singletonFactories` 来暴露早期引用。

```java
// 实例化阶段核心代码（AbstractAutowireCapableBeanFactory）
BeanWrapper instanceWrapper = createBeanInstance(beanName, mbd, args);
// 这里通过构造器反射调用来创建对象实例
// 此时返回的对象属性全为 null（基本类型为默认值）
```

#### 阶段二：属性填充（Populate Bean）

Spring 会解析当前 Bean 的所有依赖，包括 `@Autowired`、`@Resource`、`@Value`、XML 中的 `<property>` 等，并通过反射将依赖注入到当前实例中。

```java
// 属性填充核心代码（简化）
populateBean(beanName, mbd, instanceWrapper);
// 这里的逻辑包括：
// 1. 处理 InstantiationAwareBeanPostProcessor 的 postProcessAfterInstantiation
// 2. 按类型或名称注入依赖
// 3. 处理 @Autowired、@Value、@Resource 等注解
```

如果在此阶段发现循环依赖（A 引用 B，B 又引用 A），Spring 会利用**三级缓存**机制来解决（详见下文循环依赖分析）。

#### 阶段三：Aware 接口回调

Spring 在属性填充完成后，如果 Bean 实现了特定的 Aware 接口，会依次回调这些接口的方法，将底层框架信息传递给 Bean。

```java
// Aware 回调的核心代码（AbstractAutowireCapableBeanFactory.invokeAwareMethods）
if (bean instanceof BeanNameAware) {
    ((BeanNameAware) bean).setBeanName(beanName);
}
if (bean instanceof BeanFactoryAware) {
    ((BeanFactoryAware) bean).setBeanFactory(beanFactory);
}
if (bean instanceof ApplicationContextAware) {
    ((ApplicationContextAware) bean).setApplicationContext(applicationContext);
}
```

常用的 Aware 接口如下：

| Aware 接口 | 注入的信息 | 典型使用场景 |
|-----------|-----------|------------|
| `BeanNameAware` | 当前 Bean 在容器中的名称（id 或 name） | 需要知道自己在容器中的 id |
| `BeanFactoryAware` | 当前所在的 BeanFactory 容器引用 | 需要手动获取其他 Bean |
| `ApplicationContextAware` | ApplicationContext 引用 | 发布事件、获取资源、手动获取 Bean |
| `EnvironmentAware` | 当前运行环境（配置文件、profile） | 读取环境变量 |
| `ResourceLoaderAware` | 资源加载器 | 加载外部资源文件 |
| `ApplicationEventPublisherAware` | 事件发布器 | 发布自定义事件 |
| `MessageSourceAware` | 国际化资源 | 获取国际化消息 |

#### 阶段四：BeanPostProcessor 前置处理

`BeanPostProcessor.postProcessBeforeInitialization()` 会在**初始化方法调用之前**被回调。所有注册到容器中的 `BeanPostProcessor` 都会在这一步依次执行。

这一阶段是 Spring 内置功能扩展的入口之一，例如：
- `ApplicationContextAwareProcessor`：在此阶段回调 `ApplicationContextAware` 等 Aware 接口。
- `InitDestroyAnnotationBeanPostProcessor`：扫描 `@PostConstruct` 注解。

```java
@Override
public Object postProcessBeforeInitialization(Object bean, String beanName) {
    // 在所有初始化方法之前执行
    // 可以在此对 Bean 进行包装/替换
    return bean;
}
```

#### 阶段五：初始化方法执行

Spring 提供了多种初始化回调方式，按以下顺序执行：

**第一步：@PostConstruct 标注的方法**

```java
@Component
public class UserService {

    @PostConstruct
    public void init() {
        // 在构造器调用和属性注入之后执行
        // 可以在此进行一些初始化校验或数据加载
        System.out.println("@PostConstruct: 初始化完成");
    }
}
```

**第二步：InitializingBean.afterPropertiesSet()**

```java
@Component
public class UserService implements InitializingBean {

    @Override
    public void afterPropertiesSet() throws Exception {
        // 所有属性设置完成后触发
        // 通常用于校验所有必需属性是否已注入
        Assert.notNull(userDao, "userDao must not be null");
    }
}
```

**第三步：自定义 initMethod（@Bean 的 initMethod 属性或 XML init-method）**

```java
@Bean(initMethod = "customInit")
public UserService userService() {
    return new UserService();
}
```

#### 阶段六：BeanPostProcessor 后置处理 —— AOP 的关键

`BeanPostProcessor.postProcessAfterInitialization()` 在初始化方法全部完成后执行。**这里就是 AOP 动态代理的生成时机**。

```java
@Override
public Object postProcessAfterInitialization(Object bean, String beanName) {
    // 判断当前 Bean 是否需要被 AOP 增强（是否匹配 Pointcut）
    if (bean matches any Pointcut) {
        // 创建代理对象（JDK 或 CGLIB），替换原始 Bean
        return createProxy(bean);
    }
    return bean;
}
```

这就是 `AnnotationAwareAspectJAutoProxyCreator` 的工作本质——它作为一个 `BeanPostProcessor`，在 `postProcessAfterInitialization` 阶段检查每个刚完成初始化的普通 Bean 是否需要被 AOP 增强，如果需要，就返回一个代理对象替代原始对象。

**关键理解**：Spring 容器最终持有的 Bean 引用可能是代理对象，而非原始对象。这一点对于理解事务、缓存、异步等注解的失效场景至关重要。

#### 阶段七：Bean 就绪

经过以上所有步骤后，Bean 被放入一级缓存 `singletonObjects`（单例 Bean 的情况），此时它可以接受业务调用。

#### 阶段八：销毁阶段

容器关闭时（调用 `ApplicationContext.close()`），Spring 按以下顺序执行销毁回调：

```java
// 销毁回调顺序
// 1. @PreDestroy 标注的方法
@Component
public class UserService {
    @PreDestroy
    public void cleanup() {
        System.out.println("@PreDestroy: 释放资源");
    }
}

// 2. DisposableBean.destroy()
@Component
public class UserService implements DisposableBean {
    @Override
    public void destroy() {
        System.out.println("DisposableBean.destroy: 释放资源");
    }
}

// 3. 自定义 destroyMethod
@Bean(destroyMethod = "customDestroy")
public UserService userService() { ... }
```

### 2.3 BeanPostProcessor 的深度理解

`BeanPostProcessor` 是 Spring 框架中最核心的扩展接口之一，整个 AOP、事务、注解解析等核心功能都依赖它。

```java
public interface BeanPostProcessor {
    // Bean 初始化方法（@PostConstruct / afterPropertiesSet / init-method）之前执行
    default Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    // Bean 初始化方法之后执行
    default Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }
}
```

**常见的内置 BeanPostProcessor**：

| BeanPostProcessor 实现 | 功能 |
|----------------------|------|
| `AutowiredAnnotationBeanPostProcessor` | 解析 `@Autowired`、`@Value`、`@Inject` 注解 |
| `CommonAnnotationBeanPostProcessor` | 解析 `@Resource`、`@PostConstruct`、`@PreDestroy` 注解 |
| `AnnotationAwareAspectJAutoProxyCreator` | AOP 核心：为匹配切面的 Bean 创建代理对象 |
| `ApplicationContextAwareProcessor` | 回调 `ApplicationContextAware`、`EnvironmentAware` 等接口 |
| `PersistenceExceptionTranslationPostProcessor` | 为 `@Repository` 标注的 DAO 层添加异常转换 |

**注意事项**：**BeanPostProcessor 本身也是 Bean，但它必须在普通 Bean 之前初始化**。Spring 会在容器启动的早期阶段，优先实例化所有 `BeanPostProcessor` 实现，然后再用它们来处理普通 Bean。这意味着 `BeanPostProcessor` 本身不会经历完整的 BeanPostProcessor 回调链——它们"自举"了自己。

### 2.4 循环依赖：三级缓存机制

循环依赖是指两个或多个 Bean 之间相互引用，形成闭环。例如 `A` 依赖 `B`，`B` 又依赖 `A`。

```java
@Component
public class A {
    @Autowired
    private B b;
}

@Component
public class B {
    @Autowired
    private A a;
}
```

#### 三级缓存的结构

Spring 通过三个层级的 ConcurrentHashMap 来解决单例 Bean 的 Setter 注入循环依赖问题：

```java
public class DefaultSingletonBeanRegistry {

    // 一级缓存：singletonObjects —— 完全初始化好的单例 Bean（成品）
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

    // 二级缓存：earlySingletonObjects —— 提前暴露的早期单例 Bean（半成品，属性未填充完整）
    private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(16);

    // 三级缓存：singletonFactories —— 单例 Bean 的工厂（用于创建早期对象并处理 AOP 代理）
    private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);
}
```

#### 三级缓存的工作流程

以 A→B→A 的循环依赖场景为例：

1. **创建 A**：Spring 调用 A 的构造器创建实例（此时 A 为"半成品"），然后立即将 A 封装为 `ObjectFactory` 放入三级缓存 `singletonFactories`（key 为 "a"）。
2. **填充 A 的属性**：Spring 发现 A 依赖 B，于是去容器中查找 B。
3. **创建 B**：容器中还没有 B 的实例，于是开始创建 B 的流程。B 构造器实例化后，也将自己封装为 `ObjectFactory` 放入三级缓存。
4. **填充 B 的属性**：Spring 发现 B 依赖 A，于是去容器中查找 A。
5. **从三级缓存获取 A 的早期引用**：
   - 依次检查一级缓存 `singletonObjects`（没有，A 还没初始化完）
   - 二级缓存 `earlySingletonObjects`（没有，这是第一次暴露）
   - 三级缓存 `singletonFactories`（找到！）
   - 调用 `singletonFactories.get("a").getObject()` 获取 A 的早期引用，将其放入二级缓存，然后从三级缓存移除
6. **B 拿到 A 的早期引用**，完成属性填充和初始化，最终将自己放入一级缓存。
7. **A 拿到 B 的完整引用**，完成属性填充和后续的初始化流程，进入 `postProcessAfterInitialization` 生成 AOP 代理，最终放入一级缓存。

#### 为什么必须三级缓存而不是两级？

这是面试中最高频的问题之一。答案是：**为了处理 AOP 代理对象的提早暴露**。

如果只有两级缓存（singletonObjects 和 earlySingletonObjects），那么当 A 在对象创建后、填充属性前被放入二级缓存时，它还是一个**原始对象**。而 AOP 是在 `BeanPostProcessor.postProcessAfterInitialization()` 阶段才生成代理对象的——此时 A 可能已经被 B 使用了，B 中持有的是原始 A，而非 A 的 AOP 代理，这就导致了 AOP 增强失效。

三级缓存通过 `ObjectFactory` 解决了这个问题：

```java
// AbstractAutowireCapableBeanFactory 中的代码
// 在实例化 Bean 后，将其包装为 ObjectFactory 放入三级缓存
addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));

// getEarlyBeanReference 会调用 BeanPostProcessor 的 getEarlyBeanReference 方法
// 如果是 SmartInstantiationAwareBeanPostProcessor（如 AOP 相关处理器），
// 就在这里提前创建 AOP 代理！
protected Object getEarlyBeanReference(String beanName, RootBeanDefinition mbd, Object bean) {
    Object exposedObject = bean;
    // 遍历所有 SmartInstantiationAwareBeanPostProcessor
    // 如果匹配切面，直接在这里创建代理并返回
    return exposedObject;
}
```

简单来说：**三级缓存允许在 Bean 还处于半成品阶段时就为它生成 AOP 代理**，从而确保被其他 Bean 引用的"早期对象"已经是代理对象，而不会等到 `postProcessAfterInitialization` 阶段才生成代理。

#### 为什么构造器注入不能解决循环依赖？

```java
@Component
public class A {
    private final B b;

    public A(B b) {  // 构造器注入
        this.b = b;
    }
}

@Component
public class B {
    private final A a;

    public B(A a) {  // 构造器注入
        this.a = a;
    }
}
```

原因：构造器注入要求对象在**实例化阶段**就传入所有依赖。当 A 的构造器需要 B 时，B 还没有创建（甚至没有实例化），三级缓存尚未建立。A 无法完成实例化，也就不会将自己的 `ObjectFactory` 放入三级缓存——三级缓存是在构造器执行完成后才写入的。结果是 A 和 B 相互等待，都无法完成构造，Spring 最终抛出 `BeanCurrentlyInCreationException`。

**解决方案**：
1. 将构造器注入改为 Setter 注入（允许先实例化后注入）。
2. 使用 `@Lazy` 注解，让一个 Bean 延迟加载代理对象，在真正使用时才创建。

```java
@Component
public class A {
    private B b;

    @Autowired
    public void setB(@Lazy B b) {  // @Lazy 会为 B 创建一个代理
        this.b = b;
    }
}
```

---

## 3. Bean 作用域

Spring 的 Bean 作用域（Scope）决定了容器中 Bean 的存活范围和生命周期管理策略。

### 3.1 五种内置作用域

#### singleton —— 单例（默认）

```java
@Component
@Scope("singleton")  // 可省略，默认为 singleton
public class UserService {
    // 每个 Spring 容器中只存在一个实例
}
```

**特性**：
- 容器中每个 Bean ID 对应一个实例。
- 默认预初始化（在容器启动时创建，可通过 `@Lazy` 改为懒加载）。
- **线程安全问题**：多个线程共享同一个 Bean 实例，如果 Bean 中包含可变状态（如非 `final` 的成员变量），则存在并发问题。因此单例 Bean 中应**避免使用非线程安全的实例变量**，或者采用 ThreadLocal 等同步机制。

```java
// 不安全的单例——存在并发问题
@Component
public class CounterService {
    private int count = 0;  // 多线程共享状态，非线程安全

    public void increment() {
        count++;  // 非原子操作，并发下会出现脏读
    }
}

// 安全的做法：使用 ThreadLocal
@Component
public class SafeCounterService {
    private final ThreadLocal<Integer> countHolder = ThreadLocal.withInitial(() -> 0);

    public void increment() {
        countHolder.set(countHolder.get() + 1);
    }
}
```

#### prototype —— 原型

```java
@Component
@Scope("prototype")
public class TaskExecutor {
    // 每次获取都创建新实例
}
```

**特性**：
- 每次通过 `getBean()` 或注入获取时，都创建一个全新的实例。
- **容器不管理原型 Bean 的完整生命周期**：原型 Bean 在被创建并交给调用者后，容器就"忘记"它了。销毁回调（`@PreDestroy`、`DisposableBean`）不会在原型 Bean 上执行。这意味着原型 Bean 需要调用者自己负责资源释放。
- 由于每次获取都创建新对象，性能开销大于单例。

**典型使用场景**：状态非共享、每次都需要独立实例的对象，例如 `Command` 模式中的每个命令对象。

#### request / session / application —— Web 作用域

这三个作用域仅在 Web 环境（`WebApplicationContext`）中有效。

```java
@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestContext {
    // 每个 HTTP 请求拥有一个独立实例
}

@Component
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class SessionContext {
    // 每个 HTTP Session 拥有一个独立实例
}

@Scope("application")  // ServletContext 级别，全局共享
```

**关键点**：

为什么需要 `proxyMode`？因为这些 Web 作用域的 Bean 生命周期比单例 Bean 更短（请求结束就销毁），而注入它们的 Bean 往往是 `singleton` 的（如 Service 层）。如果不使用代理，单例 Bean 在初始化时就要注入 request 作用域的 Bean，但此时没有活跃的 HTTP 请求，无法创建 request 实例。通过 `ScopedProxyMode`，Spring 注入的实际上是一个**代理对象**，它能够在每次方法调用时从当前线程绑定的请求上下文中获取真实的 Bean 实例。

```java
// 代理模式工作原理
// 注入的实际上是一个代理对象
@Component
public class OrderService {
    @Autowired
    private RequestContext requestContext;  // 实际注入的是代理

    public void process() {
        // 代理在内部从 RequestContextHolder 获取当前请求的真实 RequestContext 实例
        String userId = requestContext.getUserId();
    }
}
```

### 3.2 自定义作用域

如果内置作用域不能满足需求，可以实现 `Scope` 接口来自定义作用域：

```java
public class ThreadScope implements Scope {

    private final ThreadLocal<Map<String, Object>> threadScope =
            ThreadLocal.withInitial(HashMap::new);

    @Override
    public Object get(String name, ObjectFactory<?> objectFactory) {
        return threadScope.get().computeIfAbsent(name, k -> objectFactory.getObject());
    }

    @Override
    public Object remove(String name) {
        return threadScope.get().remove(name);
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback) {
        // 原型 Bean 的销毁回调，此处不做处理
    }

    @Override
    public Object resolveContextualObject(String key) {
        return null;
    }

    @Override
    public String getConversationId() {
        return Thread.currentThread().getName();
    }
}
```

注册自定义作用域：

```java
@Configuration
public class ScopeConfig {

    @Bean
    public static BeanFactoryPostProcessor customScopeRegistrar() {
        return beanFactory -> {
            beanFactory.registerScope("thread", new ThreadScope());
        };
    }
}

// 使用
@Component
@Scope("thread")
public class ThreadLocalScopedBean { ... }
```

---

## 4. AOP（面向切面编程）

AOP（Aspect-Oriented Programming）是 Spring 框架的另一大核心。它允许开发者将与业务逻辑无关的**横切关注点**（cross-cutting concerns）——如日志记录、权限校验、事务管理、性能监控——从业务代码中分离出来，实现模块化。

### 4.1 核心概念

Spring AOP 涉及以下核心术语：

| 概念 | 英文 | 含义 | 类比 |
|------|------|------|------|
| **切面** | Aspect | 封装横切关注点的模块，包含 Pointcut 和 Advice | 法规手册 |
| **连接点** | JoinPoint | 程序执行过程中的特定位置（如方法调用、异常抛出） | 路口 |
| **切入点** | Pointcut | 匹配连接点的表达式，决定 Advice 在哪些连接点上执行 | 路口的筛选规则 |
| **通知** | Advice | 在特定连接点执行的具体增强逻辑 | 在路口执行的具体操作 |
| **目标对象** | Target | 被 AOP 增强的原始业务对象 | 需要被法规约束的对象 |
| **代理** | Proxy | 将增强织入后生成的代理对象 | 被法规约束后的对象 |
| **织入** | Weaving | 将 Advice 应用到 Target 并创建 Proxy 的过程 | 法规与实际结合的过程 |

**核心关系**：
- **Aspect = Pointcut + Advice**：切面是"在哪里（Where）"+"做什么（What）"的组合。
- Spring AOP 使用**运行时织入**（通过代理），而非 AspectJ 的编译期织入或类加载器织入。

### 4.2 Advice 的五种类型

```java
@Aspect
@Component
public class LoggingAspect {

    // 1. @Before：在目标方法执行之前运行
    @Before("execution(* com.example.service.*.*(..))")
    public void beforeAdvice(JoinPoint joinPoint) {
        System.out.println("【前置通知】准备执行: " + joinPoint.getSignature().getName());
    }

    // 2. @AfterReturning：在目标方法正常返回后运行
    @AfterReturning(value = "execution(* com.example.service.*.*(..))", returning = "result")
    public void afterReturningAdvice(JoinPoint joinPoint, Object result) {
        System.out.println("【返回通知】方法返回值: " + result);
    }

    // 3. @AfterThrowing：在目标方法抛出异常后运行
    @AfterThrowing(value = "execution(* com.example.service.*.*(..))", throwing = "ex")
    public void afterThrowingAdvice(JoinPoint joinPoint, Exception ex) {
        System.out.println("【异常通知】抛出异常: " + ex.getMessage());
    }

    // 4. @After（finally）：在目标方法执行完成后运行（无论正常还是异常）
    @After("execution(* com.example.service.*.*(..))")
    public void afterAdvice(JoinPoint joinPoint) {
        System.out.println("【最终通知】方法执行结束: " + joinPoint.getSignature().getName());
    }

    // 5. @Around：环绕通知——最强通知，可完全控制方法执行
    @Around("execution(* com.example.service.*.*(..))")
    public Object aroundAdvice(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            System.out.println("【环绕前置】开始执行: " + pjp.getSignature().getName());
            // 调用目标方法（如果不调用，目标方法不会执行！）
            Object result = pjp.proceed();
            System.out.println("【环绕返回】执行成功，耗时: " +
                    (System.currentTimeMillis() - start) + "ms");
            return result;
        } catch (Throwable t) {
            System.out.println("【环绕异常】执行异常: " + t.getMessage());
            throw t;  // 异常必须继续抛出，否则上层会误以为方法正常执行
        } finally {
            System.out.println("【环绕结束】执行完毕");
        }
    }
}
```

**@Around vs 其他四种通知**：

`@Around` 是功能最强大的通知类型，它可以：
- 在方法执行前和执行后分别执行自定义逻辑
- 决定是否调用目标方法（`pjp.proceed()`）
- 修改目标方法的返回值
- 捕获或替换目标方法的异常

而 `@Before`、`@After` 等更轻量，适用于只需在某个点触发的场景。

**多 Advice 的执行顺序**：
- **默认顺序**：`@Around` → `@Before` → 目标方法 → `@AfterReturning/@AfterThrowing` → `@After` → `@Around`
- 同一切面中的多个通知按代码顺序执行。
- 多切面之间可通过 `@Order` 或 `Ordered` 接口控制优先级：值越小，优先级越高（前置通知先执行，后置通知后执行）。

### 4.3 Pointcut 表达式详解

切入点表达式是 AOP 的"眼睛"，决定了增强逻辑在哪些连接点上执行。

#### 常用指示符（Designator）

```java
@Aspect
@Component
public class PointcutDefinition {

    // execution —— 最常用，匹配方法执行
    @Pointcut("execution(public * com.example.service.*.*(..))")
    public void serviceLayer() {}

    // within —— 匹配类/包级别
    @Pointcut("within(com.example.service.*)")
    public void withinService() {}

    // this —— 匹配代理对象为指定类型
    @Pointcut("this(com.example.service.UserService)")
    public void proxyIsUserService() {}

    // target —— 匹配目标对象为指定类型
    @Pointcut("target(com.example.service.UserService)")
    public void targetIsUserService() {}

    // args —— 匹配方法参数类型
    @Pointcut("args(Long, String)")
    public void argsMatch() {}

    // @annotation —— 匹配标注了特定注解的方法
    @Pointcut("@annotation(com.example.annotation.Loggable)")
    public void loggableMethods() {}

    // @within —— 匹配标注了特定注解的类中的所有方法
    @Pointcut("@within(org.springframework.stereotype.Service)")
    public void serviceBeans() {}
}
```

#### execution 表达式的完整语法

```
execution([权限修饰符] [返回值类型] [包名.类名.方法名]([参数列表]) [异常类型])
```

```
execution(
    public                   // 访问修饰符（可省略）
    String                   // 返回值类型（* 表示任意）
    com.example.service     // 包路径
    .UserService             // 类名（.. 表示当前包及子包）
    .getUser                 // 方法名（* 表示任意）
    (Long)                   // 参数（.. 表示任意参数）
)
```

**常用通配符**：
- `*`：匹配任意一个字符、一个包名、一个参数、一个返回值。
- `..`：匹配任意多层子包、任意多个参数。
- `+`：匹配类及所有子类。

```java
// 常见 Pointcut 表达式示例

// 匹配所有 Service 类的任意方法
execution(* com.example.service.*.*(..))

// 匹配 Service 包及其子包中的所有方法
execution(* com.example.service..*.*(..))

// 匹配所有返回值为 void 的无参方法
execution(void com.example..*())

// 匹配任意类中以 findBy 开头的方法
execution(* com.example..*.findBy*(..))

// 匹配所有标注 @Transactional 的方法（事务注解本质也是 AOP）
@annotation(org.springframework.transaction.annotation.Transactional)
```

### 4.4 代理机制：JDK 动态代理 vs CGLIB

Spring AOP 的底层通过两种代理机制实现：JDK 动态代理和 CGLIB。

#### JDK 动态代理

JDK 动态代理是 Java 内置的代理机制，通过 `java.lang.reflect.Proxy` 和 `InvocationHandler` 实现。

```java
// 接口定义
public interface UserService {
    void save(User user);
}

// 目标实现
public class UserServiceImpl implements UserService {
    @Override
    public void save(User user) {
        System.out.println("保存用户: " + user.getName());
    }
}

// JDK 动态代理实现
public class JdkProxyFactory {
    @SuppressWarnings("unchecked")
    public static <T> T createProxy(T target) {
        return (T) Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                target.getClass().getInterfaces(),  // 必须基于接口！
                (proxy, method, args) -> {
                    System.out.println("JDK 代理前置处理");
                    Object result = method.invoke(target, args);
                    System.out.println("JDK 代理后置处理");
                    return result;
                }
        );
    }
}

// 使用
UserService proxy = JdkProxyFactory.createProxy(new UserServiceImpl());
proxy.save(user);  // 调用时触发 InvocationHandler 的 invoke 方法
```

**JDK 代理的限制**：
- **必须基于接口**：目标对象必须实现至少一个接口，代理对象只实现这些接口，不继承目标类。
- **通过反射调用**：`method.invoke(target, args)` 本质是反射调用，相比直接方法调用有轻微的性能开销。
- 代理对象和目标是"兄弟"关系（共同的接口），而非父子关系——因此无法使用 `instanceof` 判断目标类的具体类型。

#### CGLIB 代理

CGLIB（Code Generation Library）基于 ASM 字节码框架，通过**继承目标类**来生成子类代理。

```java
// CGLIB 代理（不需要接口）
public class CglibProxyFactory implements MethodInterceptor {
    @SuppressWarnings("unchecked")
    public static <T> T createProxy(T target) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(target.getClass());  // 以目标类为父类
        enhancer.setCallback(new CglibProxyFactory());
        return (T) enhancer.create();  // 创建子类代理对象
    }

    @Override
    public Object intercept(Object obj, Method method, Object[] args,
                            MethodProxy proxy) throws Throwable {
        System.out.println("CGLIB 前置处理");
        // 调用父类（目标类）的方法——注意这里用 MethodProxy 而非反射
        Object result = proxy.invokeSuper(obj, args);
        System.out.println("CGLIB 后置处理");
        return result;
    }
}

// 使用（UserServiceImpl 可以没有接口）
UserServiceImpl proxy = CglibProxyFactory.createProxy(new UserServiceImpl());
```

**CGLIB 的限制**：
- **无法代理 `final` 类**：因为动态生成的是子类，`final` 类不能被继承。
- **无法代理 `final` 方法**：`final` 方法不能被子类重写。
- 相比 JDK 代理，CGLIB 在创建代理时更慢（需要生成字节码），但方法调用时稍快（因为通过 `MethodProxy` 直接调用，而非反射）。

#### Spring 的选择规则

```java
// 摘自 DefaultAopProxyFactory
public AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException {
    if (config.isOptimize() || config.isProxyTargetClass() ||
            hasNoUserSuppliedProxyInterfaces(config)) {
        Class<?> targetClass = config.getTargetClass();
        if (targetClass.isInterface() || Proxy.isProxyClass(targetClass)) {
            return new JdkDynamicAopProxy(config);
        }
        return new ObjenesisCglibAopProxy(config);
    } else {
        return new JdkDynamicAopProxy(config);
    }
}
```

翻译成自然语言的规则：

1. **如果目标类实现了接口** → 默认使用 JDK 动态代理。
2. **如果目标类没有接口** → 默认使用 CGLIB。
3. **如果显式配置 `proxy-target-class=true`** → 强制使用 CGLIB 代理。

```xml
<!-- Spring XML 配置强制使用 CGLIB -->
<aop:aspectj-autoproxy proxy-target-class="true"/>

<!-- Spring Boot 默认已设置为 true -->
spring.aop.proxy-target-class=true  <!-- Spring Boot 2.0+ 默认值 -->
```

**Spring Boot 2.0+ 的变更**：Spring Boot 默认将 `spring.aop.proxy-target-class` 设为 `true`，因此即使在有接口的情况下也使用 CGLIB。这是为了避免 JDK 代理的一些局限性（如类型转换不便）。

#### 两种代理的对比总结

| 对比维度 | JDK 动态代理 | CGLIB |
|---------|-------------|-------|
| 实现原理 | 反射 + 接口代理 | ASM 字节码 + 继承 |
| 目标要求 | 必须实现接口 | 无需接口，但不能是 final 类 |
| 代理类关系 | 和目标实现同一接口 | 是目标类的子类 |
| 方法调用 | 反射调用（`Method.invoke`） | 直接调用（`MethodProxy.invokeSuper`） |
| 创建开销 | 低 | 较高（需生成字节码） |
| 调用性能 | 较低（反射） | 较高（直接调用） |
| 限制 | 接口变化会影响代理 | final 方法/类不可代理 |
| Spring 默认行为 | 有接口时使用 | 无接口时使用（Boot 2.0+ 默认强制 CGLIB） |

### 4.5 AOP 实现原理全链路

```java
// 启动 AOP 的入口
@Configuration
@EnableAspectJAutoProxy  // 核心：开启 AOP 自动代理
public class AppConfig {
}
```

`@EnableAspectJAutoProxy` 注解通过 `@Import(AspectJAutoProxyRegistrar.class)` 向容器中注册了一个关键的 Bean 定义——`AnnotationAwareAspectJAutoProxyCreator`。

**全链路流程**：

```
@EnableAspectJAutoProxy
    ↓
AspectJAutoProxyRegistrar
    ↓
注册 AnnotationAwareAspectJAutoProxyCreator → 它是一个 SmartInstantiationAwareBeanPostProcessor
    ↓
容器启动，初始化 BeanPostProcessor（优先于普通 Bean）
    ↓
扫描所有 Bean 定义 → 识别 @Aspect 标注的切面类 → 解析 Pointcut 和 Advice
    ↓
普通 Bean 开始创建...（实例化 → 填充属性 → 初始化）
    ↓
Bean 进入 postProcessAfterInitialization 阶段
    ↓
AnnotationAwareAspectJAutoProxyCreator 判断当前 Bean 是否匹配任一 Pointcut
    ↓
匹配？→ 创建代理（JDK/CGLIB）→ 将代理对象返回到容器
不匹配？→ 返回原始对象
    ↓
容器中最终的 Bean 可能是代理对象
```

**源码级关键点**：

```java
// AnnotationAwareAspectJAutoProxyCreator 继承链
AnnotationAwareAspectJAutoProxyCreator
    extends AspectJAwareAdvisorAutoProxyCreator
        extends AbstractAdvisorAutoProxyCreator
            extends AbstractAutoProxyCreator
                implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware

// 核心方法在 AbstractAutoProxyCreator 中
@Override
public Object postProcessAfterInitialization(Object bean, String beanName) {
    if (bean instanceof Advisor || bean instanceof Advice) {
        return bean;  // 切面本身不代理
    }
    // 获取所有匹配当前 Bean 的 Advisor（切面 + 通知）
    Advisor[] advisors = getAdvicesAndAdvisorsForBean(bean, beanName, null);
    if (advisors != null && advisors.length > 0) {
        // 创建代理对象
        return createProxy(bean, beanName, advisors);
    }
    return bean;
}
```

---

## 5. 事务管理

Spring 的事务管理使用 AOP 思想，通过 `@Transactional` 注解实现声明式事务。理解其原理是避免事务"不生效"的关键。

### 5.1 @Transactional 核心属性

```java
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Transactional {

    // 事务管理器 Bean 的名称——用于指定使用哪个事务管理器
    @AliasFor("transactionManager")
    String value() default "";

    @AliasFor("value")
    String transactionManager() default "";

    // 事务传播行为（默认：REQUIRED）
    Propagation propagation() default Propagation.REQUIRED;

    // 事务隔离级别（默认：数据库默认隔离级别）
    Isolation isolation() default Isolation.DEFAULT;

    // 事务超时时间（单位：秒，默认：-1 表示使用数据库默认超时）
    int timeout() default TransactionDefinition.TIMEOUT_DEFAULT;

    // 是否只读事务（仅对查询有效，可优化数据库性能）
    boolean readOnly() default false;

    // 触发回滚的异常类型（默认只有 RuntimeException 和 Error 才会回滚）
    Class<? extends Throwable>[] rollbackFor() default {};

    // 不回滚的异常类型
    Class<? extends Throwable>[] noRollbackFor() default {};
}
```

#### 传播行为（Propagation）

| 传播行为 | 说明 | 典型场景 |
|---------|------|---------|
| `REQUIRED`（默认） | 如果有事务则加入，没有则新建 | 大多数业务方法 |
| `REQUIRES_NEW` | 总是新建一个事务，挂起当前事务 | 日志记录（独立于主事务，互不影响） |
| `NESTED` | 如果有事务则在嵌套事务中执行，没有则新建 | 批处理中的部分失败回滚 |
| `SUPPORTS` | 有则加入，没有则以非事务方式执行 | 查询方法 |
| `NOT_SUPPORTED` | 以非事务方式执行，挂起当前事务 | 不需要事务的操作 |
| `MANDATORY` | 必须在已有事务中执行，否则抛异常 | 安全检查 |
| `NEVER` | 必须在非事务中执行，有事务则抛异常 | 禁止事务的操作 |

#### 隔离级别（Isolation）

| 级别 | 说明 | 避免的问题 |
|-----|------|-----------|
| `DEFAULT` | 使用数据库默认隔离级别 | 取决于数据库 |
| `READ_UNCOMMITTED` | 读未提交 | 无法避免任何问题 |
| `READ_COMMITTED` | 读已提交（Oracle 默认） | 避免脏读 |
| `REPEATABLE_READ` | 可重复读（MySQL InnoDB 默认） | 避免脏读、不可重复读 |
| `SERIALIZABLE` | 串行化 | 避免所有问题（性能最差） |

#### 回滚规则

```java
// 默认：只有 RuntimeException 和 Error 才回滚
@Transactional  // 抛出 IOException 不会回滚！

// 显式指定 CheckedException 也回滚
@Transactional(rollbackFor = Exception.class)  // 所有异常都回滚

// 指定特定异常不回滚
@Transactional(noRollbackFor = ArithmeticException.class)
```

### 5.2 事务失效的 7 种经典场景

这是面试中几乎必问的话题：

#### 场景 1：同类方法调用（不走代理）

```java
@Service
public class UserService {

    @Transactional
    public void createUser(User user) {
        // 事务生效
        saveUser(user);
    }

    public void saveUser(User user) {
        // 事务生效吗？看谁调用的
        userDao.save(user);
    }

    // 失效案例：内部调用
    public void register(User user) {
        // this.createUser(user) —— 直接调用本类方法，没有经过代理！
        // @Transactional 事务失效！
        this.createUser(user);
    }

    // 解决办法：注入自身代理
    @Autowired
    private UserService self;  // 注意：这需要循环依赖支持

    public void registerV2(User user) {
        self.createUser(user);  // 通过代理调用，事务生效
    }
}
```

**根本原因**：Spring 的 `@Transactional` 是通过 AOP 代理实现的。同类方法之间直接调用（`this.method()`）走的是原始对象，没有经过代理对象的拦截，因此事务不会生效。

**解决办法**：
1. 将 `@Transactional` 方法放到另一个 Service 中，通过注入的代理调用。
2. 从 `ApplicationContext` 中手动获取代理对象。
3. 使用 `AopContext.currentProxy()`（需配置 `exposeProxy=true`）。

#### 场景 2：非 public 方法

```java
@Service
public class UserService {

    @Transactional  // 失效！private 方法不受代理
    private void createUser(User user) {
        userDao.save(user);
    }

    // protected 或 default 同样不生效
    @Transactional
    void createUser2(User user) { ... }  // 也失效
}
```

**根本原因**：Spring 的 `AbstractFallbackTransactionAttributeSource` 在解析 `@Transactional` 时会检查方法的可见性，非 public 方法直接返回 `null`（即视为没有事务注解）。这是 CGLIB/JDK 代理机制的局限性——代理仅限于 public 方法（CGLIB 的 `MethodInterceptor` 也不会拦截非 public 方法）。

#### 场景 3：异常被 catch 吞掉

```java
@Transactional
public void createUser(User user) {
    try {
        userDao.save(user);
        throw new RuntimeException("错误");  // 触发回滚
    } catch (Exception e) {
        System.out.println("异常被吞掉了");
        // 没有往外抛异常！事务拦截器没有捕获到异常，因此不会回滚
    }
}
```

**解决办法**：在 `catch` 中重新抛出异常：

```java
@Transactional
public void createUser(User user) {
    try {
        userDao.save(user);
    } catch (Exception e) {
        System.out.println("记录异常：" + e.getMessage());
        throw e;  // 重新抛出，让事务管理器看到异常
    }
}
```

#### 场景 4：rollbackFor 设置错误

```java
@Transactional  // 默认只有 RuntimeException 和 Error 才回滚
public void createUser(User user) throws FileNotFoundException {
    // 抛出 FileNotFoundException（Check Exception）
    // 事务不会回滚！
    throw new FileNotFoundException("文件未找到");
}

// 改正
@Transactional(rollbackFor = Exception.class)
public void createUser(User user) throws Exception { ... }
```

#### 场景 5：多线程事务

```java
@Transactional
public void processBatch(List<Data> dataList) {
    ExecutorService executor = Executors.newFixedThreadPool(5);
    for (Data data : dataList) {
        executor.submit(() -> {
            // 子线程中的数据库操作不在父事务的管理范围内！
            // 即使子线程抛出异常，父事务也不会回滚
            process(data);
        });
    }
    executor.shutdown();
}
```

**根本原因**：Spring 的事务是通过 `ThreadLocal` 将 `Connection` 绑定到当前线程上的。子线程中的操作无法获取到父线程的 `Connection`，因此每个子线程都在独立的事务中执行。

**解决办法**：
- 不使用并行处理，在同一个线程中完成所有操作。
- 使用编程式事务手动管理各线程的事务。
- 使用 `TransactionTemplate` 在子线程中独立控制事务。

#### 场景 6：数据库引擎不支持事务

```java
// 如果 MySQL 使用了 MyISAM 引擎，它不支持事务
// @Transactional 无论如何配置都不会生效
// MyISAM 会忽略 commit/rollback 指令
```

**解决办法**：使用 InnoDB 引擎（MySQL 5.5+ 的默认引擎）。

#### 场景 7：事务传播行为使用不当

```java
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public void createUser(User user) {
    // 以非事务方式运行，即使抛出 RuntimeException 也不会回滚
    userDao.save(user);
    throw new RuntimeException();
}
```

### 5.3 事务实现原理

Spring 事务的底层是基于 AOP + PlatformTransactionManager 实现的：

```java
// 核心接口
public interface PlatformTransactionManager {
    TransactionStatus getTransaction(TransactionDefinition definition);
    void commit(TransactionStatus status);
    void rollback(TransactionStatus status);
}

// JDBC 场景的实现
public class DataSourceTransactionManager extends AbstractPlatformTransactionManager {
    // 通过 DataSource.getConnection() 获取连接
    // 通过 Connection.setAutoCommit(false) 开启事务
    // 将 Connection 绑定到当前线程（TransactionSynchronizationManager）
}
```

**事务拦截器的工作流程**：

```java
// TransactionInterceptor 的本质是一个 MethodInterceptor（AOP 环绕通知）
public class TransactionInterceptor implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        // 提取 @Transactional 的属性
        TransactionAttribute txAttr = getTransactionAttribute(invocation);

        // 获取 PlatformTransactionManager
        PlatformTransactionManager ptm = getTransactionManager(txAttr);

        // 开启事务（获取连接、禁止自动提交、绑定到线程）
        TransactionStatus status = ptm.getTransaction(txAttr);

        try {
            // 执行目标业务方法
            Object result = invocation.proceed();

            // 正常返回 → 提交事务
            ptm.commit(status);
            return result;
        } catch (Throwable ex) {
            // 判断是否需要回滚（检查异常类型）
            if (txAttr.rollbackOn(ex)) {
                ptm.rollback(status);  // 回滚
            } else {
                ptm.commit(status);    // 即使异常也提交（针对 CheckedException）
            }
            throw ex;
        }
    }
}
```

**事务资源（Connection）的线程绑定**：

```java
// TransactionSynchronizationManager——事务资源管理器
public abstract class TransactionSynchronizationManager {

    // 线程绑定资源（核心！）
    private static final ThreadLocal<Map<Object, Object>> resources =
            new NamedThreadLocal<>("Transactional resources");

    // 将 DataSource 对应的 Connection 绑定到当前线程
    public static void bindResource(Object key, Object value) {
        // key = DataSource, value = ConnectionHolder（包装了 Connection）
        resources.get().put(key, value);
    }

    // 获取当前线程绑定的 Connection
    public static Object getResource(Object key) {
        return resources.get().get(key);
    }
}
```

这就是 Spring 事务能够在同一个线程中"共享"同一个 `Connection` 的原因——所有 DAO 操作都通过 `DataSourceUtils.getConnection()` 获取连接，而该方法会优先从 `TransactionSynchronizationManager` 获取当前线程绑定的连接。

---

## 6. Spring 扩展点

Spring 框架的成功很大程度上得益于其完善的扩展机制。了解这些扩展点，不仅有助于深入理解 Spring 的工作原理，也能在框架集成和业务开发中灵活运用。

### 6.1 BeanFactoryPostProcessor

`BeanFactoryPostProcessor` 在 Bean 定义加载完成后、Bean 实例化之前执行，允许开发者修改 Bean 的定义信息（如属性值）。

```java
@FunctionalInterface
public interface BeanFactoryPostProcessor {
    void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory);
}
```

**经典实现 — PropertySourcesPlaceholderConfigurer**：

```java
// 作用：解析 Bean 定义中的 ${...} 占位符，替换为配置文件中的实际值
@Configuration
public class AppConfig {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigurer() {
        PropertySourcesPlaceholderConfigurer configurer =
                new PropertySourcesPlaceholderConfigurer();
        configurer.setLocation(new ClassPathResource("application.properties"));
        return configurer;
    }
}

// 在 application.properties 中
// database.url=jdbc:mysql://localhost:3306/db

// 在 Bean 定义中
@Value("${database.url}")
private String url;
```

`PropertySourcesPlaceholderConfigurer` 作为一个 `BeanFactoryPostProcessor`，会在容器生命周期的早期阶段扫描所有 Bean 定义中的 `@Value` 或 `<property>` 中的 `${...}` 占位符，并从配置文件中读取对应的值进行替换。

**自定义 BeanFactoryPostProcessor 示例**：

```java
@Component
public class MyBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        // 遍历所有 Bean 定义
        String[] beanNames = beanFactory.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
            // 修改特定 Bean 的作用域
            if (beanName.contains("prototype")) {
                bd.setScope("singleton");
            }
        }
    }
}
```

### 6.2 BeanPostProcessor

已在 Bean 生命周期章节中详细阐述，此处不再赘述。

### 6.3 Aware 接口系列

`Aware` 接口在 Bean 生命周期章节已覆盖，此处补充其设计思想。

```java
// 所有 Aware 接口的"空标记"接口
public interface Aware {
}

// 子接口——每个都具有回调功能
public interface BeanNameAware extends Aware { ... }
public interface BeanFactoryAware extends Aware { ... }
public interface ApplicationContextAware extends Aware { ... }
public interface ApplicationEventPublisherAware extends Aware { ... }
public interface ResourceLoaderAware extends Aware { ... }
public interface ServletContextAware extends Aware { ... }
```

**使用示例**：

```java
@Component
public class MyApplicationContextProvider implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        applicationContext = ctx;
    }

    // 提供静态方法获取 Bean——适用于非 Spring 管理的类
    public static <T> T getBean(Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }
}
```

**设计思想**：`Aware` 接口利用了回调机制，将基础设施注入到 Bean 中，既避免了 Bean 对 Spring API 的强依赖（如果不实现 Aware 接口，Bean 就是一个 POJO），又在需要时提供了访问框架底层的能力。

### 6.4 ApplicationListener / 事件发布

Spring 的事件机制是一个轻量级的事件驱动模型，允许 Bean 之间解耦通信。

```java
// 1. 自定义事件（继承 ApplicationEvent）
public class OrderCreatedEvent extends ApplicationEvent {
    private final Long orderId;
    private final String orderNo;

    public OrderCreatedEvent(Object source, Long orderId, String orderNo) {
        super(source);
        this.orderId = orderId;
        this.orderNo = orderNo;
    }

    // getters...
}

// 2. 事件发布者
@Service
public class OrderService {

    @Autowired
    private ApplicationEventPublisher publisher;

    public void createOrder(Order order) {
        // 业务逻辑...
        // 发布事件——不关心谁会处理
        publisher.publishEvent(new OrderCreatedEvent(this, order.getId(), order.getOrderNo()));
    }
}

// 3. 事件监听器——方式一：注解驱动
@Component
public class OrderEventListener {

    @EventListener
    @Async  // 可异步执行，不阻塞主流程
    public void handleOrderCreated(OrderCreatedEvent event) {
        // 发送短信通知、更新缓存等
        System.out.println("收到订单创建事件: " + event.getOrderNo());
    }
}

// 4. 事件监听器——方式二：接口实现
@Component
public class EmailNotificationListener implements ApplicationListener<OrderCreatedEvent> {

    @Override
    public void onApplicationEvent(OrderCreatedEvent event) {
        // 发送邮件通知
        System.out.println("发送邮件通知: 订单 " + event.getOrderNo() + " 已创建");
    }
}
```

**事件机制的底层原理**：

`ApplicationEventPublisher` 实际上是 `ApplicationContext` 的父接口之一。当调用 `publishEvent()` 时，Spring 会遍历所有注册的 `ApplicationListener`，通过 `SimpleApplicationEventMulticaster` 同步或异步地调用每个监听器的 `onApplicationEvent()` 方法（默认是同步调用——这意味着监听器抛出异常会影响发布者的流程，需注意）。

### 6.5 FactoryBean

`FactoryBean` 是 Spring 中用于创建复杂对象的工厂接口。与 `@Bean` 不同，`FactoryBean` 允许更灵活的对象创建逻辑，特别是在框架集成中广泛使用。

```java
// 接口定义
public interface FactoryBean<T> {
    T getObject() throws Exception;        // 返回由工厂创建的 Bean 实例
    Class<?> getObjectType();              // 返回工厂创建的 Bean 类型
    default boolean isSingleton() {        // 是否单例
        return true;
    }
}
```

**FactoryBean 与 @Bean 的区别**：
- `@Bean` 是在配置类中定义 Bean 的创建逻辑。
- `FactoryBean` 是一个独立的工厂类，Spring 容器会自动识别它——从容器中获取时，`getBean("factoryBeanName")` 返回的是 `getObject()` 的结果，而不是 FactoryBean 本身；要想获取 FactoryBean 本身，需要在 Bean 名称前加 `&`（如 `getBean("&factoryBeanName")`）。

**经典案例 — MyBatis 的 MapperFactoryBean**：

```java
// 简化版 MapperFactoryBean
public class MapperFactoryBean<T> implements FactoryBean<T> {

    private Class<T> mapperInterface;

    public MapperFactoryBean(Class<T> mapperInterface) {
        this.mapperInterface = mapperInterface;
    }

    @Override
    public T getObject() throws Exception {
        // 通过 JDK 动态代理创建 Mapper 接口的实现
        return (T) Proxy.newProxyInstance(
                mapperInterface.getClassLoader(),
                new Class[]{mapperInterface},
                new MapperProxy<>(sqlSessionTemplate)
        );
    }

    @Override
    public Class<?> getObjectType() {
        return mapperInterface;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
```

通过 `FactoryBean`，MyBatis 不需要为每个 Mapper 接口编写实现类，而是在运行时通过动态代理创建实现。

**自定义 FactoryBean 示例**：

```java
@Component
public class MyConnectionFactory implements FactoryBean<Connection> {

    @Value("${database.url}")
    private String url;

    @Value("${database.username}")
    private String username;

    @Value("${database.password}")
    private String password;

    @Override
    public Connection getObject() throws Exception {
        // 创建复杂对象
        return DriverManager.getConnection(url, username, password);
    }

    @Override
    public Class<?> getObjectType() {
        return Connection.class;
    }

    @Override
    public boolean isSingleton() {
        return false;  // 每次获取都获取新的连接
    }
}
```

### 6.6 ImportBeanDefinitionRegistrar

`ImportBeanDefinitionRegistrar` 允许在 `@Configuration` 类中使用 `@Import` 来动态注册 Bean 定义。这个扩展点是 Spring Boot 自动配置和许多框架集成的底层实现基础。

```java
// 核心接口
public interface ImportBeanDefinitionRegistrar {
    void registerBeanDefinitions(
            AnnotationMetadata importingClassMetadata,
            BeanDefinitionRegistry registry
    );
}
```

**经典案例 — @EnableAspectJAutoProxy 的底层**：

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(AspectJAutoProxyRegistrar.class)  // 导入注册器
public @interface EnableAspectJAutoProxy {
    boolean proxyTargetClass() default false;
    boolean exposeProxy() default false;
}

// AspectJAutoProxyRegistrar 实现了 ImportBeanDefinitionRegistrar
class AspectJAutoProxyRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(
            AnnotationMetadata importingClassMetadata,
            BeanDefinitionRegistry registry) {

        // 注册 AnnotationAwareAspectJAutoProxyCreator 的 Bean 定义
        AopConfigUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(registry);

        // 解析 @EnableAspectJAutoProxy 注解的属性
        AnnotationAttributes attributes = AnnotationConfigUtils.attributesFor(
                importingClassMetadata, EnableAspectJAutoProxy.class);

        // 设置 proxyTargetClass 和 exposeProxy 等属性
        if (attributes != null) {
            // ...
        }
    }
}
```

**自定义 ImportBeanDefinitionRegistrar**：

```java
public class MyMapperScannerRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(
            AnnotationMetadata importingClassMetadata,
            BeanDefinitionRegistry registry) {

        // 扫描指定包下的接口，为每个接口动态注册 Bean 定义
        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(
                registry, false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(MyMapper.class));

        // 扫描并注册
        int count = scanner.scan("com.example.mapper");
        System.out.println("总共注册了 " + count + " 个 Mapper Bean");
    }
}
```

---

## 7. Spring 面试核心问题 Checklist

以下整理为 Spring 面试中最核心的考察点，逐个对照自查：

### IoC / DI 基础
- [ ] 什么是 IoC？控制反转翻转了什么？好莱坞原则的含义。
- [ ] DI 的三种方式对比，为什么官方推荐构造器注入？
- [ ] `@Autowired` 和 `@Resource` 的区别（装配策略、来源规范）。
- [ ] BeanFactory 与 ApplicationContext 的区别与联系。
- [ ] Bean 定义的三种方式及各自适用场景。
- [ ] 如何解决 `NoUniqueBeanDefinitionException`？（`@Primary`、`@Qualifier`、`List` 收集）

### Bean 生命周期
- [ ] 完整描述 Bean 从创建到销毁的每一步。
- [ ] `BeanPostProcessor` 的 `postProcessBeforeInitialization` 和 `postProcessAfterInitialization` 执行时机。
- [ ] AOP 代理在哪一步生成？为什么？
- [ ] 三级缓存如何解决循环依赖？**为什么必须是三级**？
- [ ] 为什么构造器注入无法解决循环依赖？
- [ ] 内部方法调用 `@Transactional` 失效的根本原因。

### AOP
- [ ] AOP 核心概念（Aspect, Pointcut, JoinPoint, Advice, Target, Weaving）的准确含义。
- [ ] JDK 动态代理 vs CGLIB 的底层原理、优缺点、适用范围。
- [ ] Spring 选择代理机制的规则。
- [ ] `@Around` 和其他通知类型的区别，`ProceedingJoinPoint.proceed()` 不调用会怎样？
- [ ] 编写 execution 匹配 Service 包下所有无参方法的表达式。
- [ ] `@EnableAspectJAutoProxy` 的工作流程（注册了什么？创建了什么？）。

### 事务
- [ ] `@Transactional` 默认回滚规则？（仅 RuntimeException 和 Error，CheckedException 不回滚）
- [ ] 列举至少 5 种事务失效场景并解释原因。
- [ ] `REQUIRED` vs `REQUIRES_NEW` vs `NESTED` 的区别。
- [ ] 多线程事务为什么不共享父线程的事务？
- [ ] 同类方法调用事务失效的本质原因和解决方案。
- [ ] `PlatformTransactionManager` 的核心方法。

### 扩展机制
- [ ] `BeanFactoryPostProcessor` 和 `BeanPostProcessor` 的区别和执行时机。
- [ ] `FactoryBean` 的作用，获取 FactoryBean 本身需加 `&` 前缀。
- [ ] `@Import` 和 `ImportBeanDefinitionRegistrar` 的作用。
- [ ] Aware 接口的设计思想（回调 + 避免强依赖）。
- [ ] Spring 事件机制的构成：事件、发布者、监听器、多播器。

---

> 本文深入剖析了 Spring Framework 的两大核心——IoC 容器和 AOP 机制，涵盖了从设计思想到源码实现的完整知识体系。理解这些底层原理，不仅是为了应对面试，更是为了在实际开发中能够正确地使用 Spring、高效地诊断问题、灵活地扩展功能。Spring 框架的设计思想（控制反转、面向切面、模板方法、策略模式）本身就是 Java 企业级开发领域的最佳实践总结，掌握它们将使你在软件设计的道路上走得更远。
