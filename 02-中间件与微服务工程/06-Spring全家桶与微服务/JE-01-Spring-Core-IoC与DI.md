# Spring Core: IoC 容器与依赖注入 (IoC & DI)

> **"The Hollywood Principle: Don't call us, we'll call you."**  
> 版本: Spring Framework 6.x / JDK 17+  
> 适用: Spring Boot 3.x 全系列

---

## Table of Contents

1. [Spring Framework 历史与哲学](#1-spring-framework-历史与哲学)
2. [IoC 概念深度解析](#2-ioc-概念深度解析)
3. [Spring IoC 容器：BeanFactory vs ApplicationContext](#3-spring-ioc-容器beanfactory-vs-applicationcontext)
4. [Bean 的定义与配置方式](#4-bean-的定义与配置方式)
5. [依赖注入详解](#5-依赖注入详解)
6. [Bean 作用域 (Scope)](#6-bean-作用域-scope)
7. [Bean 生命周期详解](#7-bean-生命周期详解)
8. [@Configuration 与 @Bean](#8-configuration-与-bean)
9. [条件装配：@Conditional 与 @Profile](#9-条件装配conditional-与-profile)
10. [属性管理与外部化配置](#10-属性管理与外部化配置)
11. [Spring Expression Language (SpEL)](#11-spring-expression-language-spel)
12. [国际化 (i18n)](#12-国际化-i18n)
13. [事件机制](#13-事件机制)
14. [@Async 与任务执行](#14-async-与任务执行)
15. [常见陷阱与最佳实践](#15-常见陷阱与最佳实践)
16. [面试题精选](#16-面试题精选)

---

## 1. Spring Framework 历史与哲学

### 1.1 历史里程碑

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Spring Framework Timeline                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  2002: Rod Johnson 出版 "Expert One-on-One J2EE Design and          │
│        Development" — 包含 30,000+ 行代码示例                       │
│        ↓                                                           │
│  2003: Spring Framework 1.0 正式发布                               │
│        ● XML 配置 IoC 容器                                         │
│        ● AOP 支持                                                  │
│        ● JDBC 抽象层                                               │
│        ↓                                                           │
│  2004-2006: Spring 1.2 → 2.0                                      │
│        ● 声明式事务 @Transactional                                 │
│        ● 简化 XML schema                                           │
│        ↓                                                           │
│  2009: Spring 3.0 (JDK 5+)                                        │
│        ● @Configuration, @Bean — JavaConfig                       │
│        ● SpEL (Spring Expression Language)                         │
│        ● REST 支持                                                 │
│        ↓                                                           │
│  2013: Spring 4.0 (JDK 8+)                                        │
│        ● @Conditional 条件注解                                     │
│        ● WebSocket 支持                                            │
│        ● 泛型注入                                                  │
│        ↓                                                           │
│  2017: Spring 5.0 (JDK 8+)                                        │
│        ● WebFlux 响应式编程                                        │
│        ● Kotlin 支持                                               │
│        ● 响应式客户端 WebClient                                    │
│        ↓                                                           │
│  2022: Spring 6.0 (JDK 17+)                                       │
│        ● Jakarta EE 迁移 (javax.* → jakarta.*)                    │
│        ● AOT (Ahead-of-Time) 编译支持                              │
│        ● Virtual Threads (虚拟线程)                                │
│        ● @HttpExchange HTTP 接口                                   │
│        ↓                                                           │
│  2023+: Spring 6.1+                                                │
│        ● RestClient (同步 HTTP 客户端)                             │
│        ● 更成熟的 AOT/GraalVM 支持                                 │
│        ● ProblemDetail 的 RFC 7807 支持                            │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 Spring 哲学

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Spring 设计哲学                                   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  1. POJO 至上 (Plain Old Java Object)                              │
│     ● 任何 Java 类都可以是 Spring Bean                             │
│     ● 不需要实现框架接口                                           │
│     ● 不需要继承框架类                                             │
│                                                                     │
│  2. 非侵入式 (Non-invasive)                                        │
│     ● 代码不依赖框架代码 (可选)                                    │
│     ● 可以在非 Spring 环境中复用                                   │
│                                                                     │
│  3. 约定优于配置 (Convention over Configuration)                    │
│     ● 合理的默认值                                                 │
│     ● 只配置与默认不同的部分                                       │
│                                                                     │
│  4. 分层抽象 (Layered Abstraction)                                  │
│     ● 每一层都是可替换的                                           │
│     ● 从不锁定底层实现                                             │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2. IoC 概念深度解析

### 2.1 什么是 IoC (Inversion of Control)

**IoC (控制反转)** 是一种设计原则，将对象的创建、配置和生命周期管理从应用程序代码转移到容器。

**传统方式 vs IoC 方式：**

```java
// ========== 传统方式：应用程序主动控制 ==========
public class TraditionalService {
    private Database database;
    
    public TraditionalService() {
        // 应用程序自己创建依赖
        this.database = new MySQLDatabase("jdbc:mysql://localhost:3306/db");
        // 问题：硬编码依赖，难以替换，难以测试
    }
}

// ========== IoC 方式：容器控制 ==========
public class IoCService {
    private final Database database;
    
    // 应用程序只声明需要什么，不关心如何创建
    public IoCService(Database database) {
        this.database = database;
        // 谁调用我，谁提供依赖（Spring 容器）
    }
}
```

### 2.2 好莱坞原则 (Hollywood Principle)

> "Don't call us, we'll call you."

```
┌──────────────────────────────┐     ┌──────────────────────────────┐
│    传统方式 (主动拉取)        │     │    IoC 方式 (被动注入)        │
│                              │     │                              │
│  Application ──→ 创建 ──→ Service │  Application ──→ 使用 ──→ Service │
│       │                     │     │       ↑                      │
│       │  JNDI 查找          │     │       │                      │
│       └─────────→ DataSource│     │  IoC Container 注入          │
│                              │     │       │                      │
│  应用程序负责全生命周期       │     │  容器负责全生命周期           │
└──────────────────────────────┘     └──────────────────────────────┘
```

### 2.3 依赖查找 (DL) vs 依赖注入 (DI)

| 特性 | 依赖查找 (Dependency Lookup) | 依赖注入 (Dependency Injection) |
|------|------------------------------|--------------------------------|
| 谁负责获取依赖 | 应用程序主动获取 | 容器主动提供 |
| 代码侵入性 | 高 — 需调用容器 API | 低 — POJO 不感知容器 |
| 耦合度 | 与容器 API 耦合 | 与容器解耦 |
| 可测试性 | 差 (依赖容器环境) | 好 (可 mock) |
| 推荐程度 | ❌ 不推荐 | ✅ 推荐 |

```java
// ========== 依赖查找 (Dependency Lookup) — 不推荐 ==========
public class LookupExample {
    public void doSomething() {
        // 主动从容器查找 — 代码与容器 API 耦合
        ApplicationContext ctx = new ClassPathXmlApplicationContext("beans.xml");
        MyService service = ctx.getBean(MyService.class);
        service.execute();
    }
}

// ========== 依赖注入 (Dependency Injection) — 推荐 ==========
public class InjectionExample {
    private final MyService service;
    
    // 容器通过构造器注入 — 代码不知道容器存在
    public InjectionExample(MyService service) {
        this.service = service;
    }
    
    public void doSomething() {
        service.execute();
    }
}
```

### 2.4 IoC 为什么重要

```java
// 没有 IoC 的代码：紧耦合，难以测试
public class OrderService {
    private EmailService emailService = new EmailService();
    private PaymentService paymentService = new PaymentService();
    private InventoryService inventoryService = new InventoryService();
    
    // 构造函数中创建所有依赖
    public OrderService() {
        this.emailService = new EmailService();
        this.paymentService = new PaymentService();
        this.inventoryService = new InventoryService();
    }
    
    // ❌ 无法单独测试 — 所有依赖都真实加载
    // ❌ 无法替换 EmailService 实现（如 Mock）
    // ❌ 构造逻辑复杂
}

// 有 IoC 的代码：松耦合，可测试
public class OrderService {
    private final EmailService emailService;
    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    
    // 依赖通过构造器传入 — 不关心具体实现
    public OrderService(EmailService emailService, 
                       PaymentService paymentService,
                       InventoryService inventoryService) {
        this.emailService = emailService;
        this.paymentService = paymentService;
        this.inventoryService = inventoryService;
    }
}

// ✅ 单元测试时可以传入 Mock 对象
class OrderServiceTest {
    @Test
    void testCreateOrder() {
        EmailService mockEmail = mock(EmailService.class);
        PaymentService mockPayment = mock(PaymentService.class);
        InventoryService mockInventory = mock(InventoryService.class);
        
        OrderService service = new OrderService(mockEmail, mockPayment, mockInventory);
        // ... 测试业务逻辑
    }
}
```

---

## 3. Spring IoC 容器：BeanFactory vs ApplicationContext

### 3.1 容器层次结构

```
┌───────────────────────────────────────────────────────────┐
│                    BeanFactory (根接口)                     │
│  ● getBean()                                              │
│  ● containsBean()                                         │
│  ● isSingleton() / isPrototype()                          │
│  ● getType() / getAliases()                               │
└──────────────────────┬────────────────────────────────────┘
                       │ implements
┌──────────────────────┴────────────────────────────────────┐
│              ApplicationContext (继承所有功能)              │
│  ● BeanFactory 的所有能力                                  │
│  ● 国际化 (MessageSource)                                  │
│  ● 事件发布 (ApplicationEventPublisher)                    │
│  ● 资源加载 (ResourceLoader)                               │
│  ● 生命周期管理 (LifecycleProcessor)                       │
│  ● AOP 支持                                               │
└────────────────────────────────────────────────────────────┘
```

### 3.2 BeanFactory vs ApplicationContext 详细对比

```java
// ========== BeanFactory — 最基础的容器 ==========
// BeanFactory 是 Spring IoC 容器的 root 接口
// 使用场景：资源受限环境（移动设备、嵌入式系统）
// 特点：延迟加载（lazy loading），按需实例化

// 通常不直接使用 BeanFactory，而是使用其实现 DefaultListableBeanFactory
DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

// 通过 BeanDefinitionReader 加载配置
XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
reader.loadBeanDefinitions("classpath:beans.xml");

// 按需获取 Bean — 此时才实例化
MyService service = factory.getBean(MyService.class);

// ========== ApplicationContext — 企业级容器 ==========
// 最常用的容器接口，在 BeanFactory 基础上增加了企业特性

// 1. ClassPathXmlApplicationContext — XML 配置（传统方式）
ApplicationContext ctx1 = new ClassPathXmlApplicationContext("applicationContext.xml");

// 2. FileSystemXmlApplicationContext — 文件系统 XML
ApplicationContext ctx2 = new FileSystemXmlApplicationContext(
    "/opt/config/applicationContext.xml");

// 3. AnnotationConfigApplicationContext — 纯注解配置 (推荐)
ApplicationContext ctx3 = new AnnotationConfigApplicationContext(AppConfig.class);

// 4. WebApplicationContext — Web 环境
//    (由 DispatcherServlet 创建，开发中通常不直接实例化)
```

### 3.3 ApplicationContext 扩展功能详解

```java
// ========== 1. 国际化 (MessageSource) ==========
// 在 applicationContext.xml 中配置：
// <bean id="messageSource" class="org.springframework.context.support.ResourceBundleMessageSource">
//     <property name="basenames" value="messages"/>
// </bean>

// 或 JavaConfig：
@Bean
public MessageSource messageSource() {
    ResourceBundleMessageSource source = new ResourceBundleMessageSource();
    source.setBasenames("messages", "i18n/validation");
    source.setDefaultEncoding("UTF-8");
    return source;
}

// 使用：
@Service
public class I18nService {
    @Autowired
    private MessageSource messageSource;  // 从容器中获取 MessageSource
    
    public String getGreeting(Locale locale) {
        // messages.properties: greeting=Hello {0}
        // messages_zh_CN.properties: greeting=你好 {0}
        return messageSource.getMessage("greeting", 
            new Object[]{"World"}, locale);
    }
}

// ========== 2. 事件发布 (ApplicationEventPublisher) ==========
@Service
public class OrderService {
    @Autowired
    private ApplicationEventPublisher publisher;
    
    public void createOrder(Order order) {
        // ... 业务逻辑 ...
        // 发布事件 — 所有监听者会收到通知
        publisher.publishEvent(new OrderCreatedEvent(this, order));
    }
}

// ========== 3. 资源加载 (ResourceLoader) ==========
@Service
public class ResourceService {
    @Autowired
    private ResourceLoader resourceLoader;
    
    public String loadConfig() throws IOException {
        // 支持多种资源类型：
        // classpath:config.properties
        // file:/opt/config/app.properties
        // https://example.com/config.properties
        Resource resource = resourceLoader.getResource("classpath:data.txt");
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

// ========== 4. 生命周期管理 (LifecycleProcessor) ==========
// 所有实现 Lifecycle 接口的 Bean 会跟随容器的 start/stop 生命周期
@Component
public class MyLifecycleComponent implements Lifecycle {
    private volatile boolean running;
    
    @Override
    public void start() {
        // 容器启动时自动调用
        running = true;
        System.out.println("Component started");
    }
    
    @Override
    public void stop() {
        // 容器关闭时自动调用
        running = false;
        System.out.println("Component stopped");
    }
    
    @Override
    public boolean isRunning() {
        return running;
    }
}
```

### 3.4 容器的创建与销毁

```java
// ========== 创建容器 ==========
// 方式 1：直接创建（适用于独立应用）
public class Main {
    public static void main(String[] args) {
        // 创建容器 — 此时开始实例化所有 singleton bean
        AnnotationConfigApplicationContext ctx = 
            new AnnotationConfigApplicationContext(AppConfig.class);
        
        // 获取并使用 bean
        MyService service = ctx.getBean(MyService.class);
        service.execute();
        
        // 必须显式关闭容器（释放资源）
        ctx.close();
        // 或使用 try-with-resources (ApplicationContext 实现了 AutoCloseable)
    }
}

// 方式 2：try-with-resources 自动关闭
public class Main {
    public static void main(String[] args) {
        try (AnnotationConfigApplicationContext ctx = 
                new AnnotationConfigApplicationContext(AppConfig.class)) {
            MyService service = ctx.getBean(MyService.class);
            service.execute();
        } // 自动调用 close()
    }
}

// 方式 3：注册关闭钩子 (Shutdown Hook)
public class Main {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext ctx = 
            new AnnotationConfigApplicationContext(AppConfig.class);
        // 注册 JVM 关闭钩子 — 虚拟机关闭时自动关闭容器
        ctx.registerShutdownHook();
        
        MyService service = ctx.getBean(MyService.class);
        service.execute();
        // 不需要显式 close()
    }
}
```

### 3.5 容器的层次结构 (Parent-Child)

```java
// ========== 父子容器 ==========
// Spring MVC 中典型的父子容器结构：
// ┌────────────────────────────────────┐
// │ Root WebApplicationContext (父)     │
// │ ● 配置: applicationContext.xml     │
// │ ● service, repository, datasource  │
// │ ● 所有 Web 应用共享                │
// └──────────────┬─────────────────────┘
//                │ parent
// ┌──────────────┴─────────────────────┐
// │ DispatcherServlet Context (子)     │
// │ ● 配置: dispatcher-servlet.xml     │
// │ ● controller, view resolver       │
// │ ● 每个 Servlet 独立                │
// └────────────────────────────────────┘
// 
// 子容器可以访问父容器的 bean
// 父容器不能访问子容器的 bean

// 创建父子容器：
AnnotationConfigApplicationContext parent = 
    new AnnotationConfigApplicationContext(ServiceConfig.class);
AnnotationConfigApplicationContext child = 
    new AnnotationConfigApplicationContext();
child.setParent(parent);  // 设置父容器
child.register(WebConfig.class);
child.refresh();

// 在子容器中获取父容器的 bean
WebController controller = child.getBean(WebController.class);
// controller 依赖的 service 在父容器中定义
```

---

## 4. Bean 的定义与配置方式

### 4.1 三种配置方式对比

| 特性 | XML 配置 | 注解配置 | Java Config |
|------|----------|----------|-------------|
| 首次引入 | Spring 1.0 | Spring 2.5 | Spring 3.0 |
| 类型安全 | ❌ 运行时错误 | ✅ 编译期发现 | ✅ 编译期发现 |
| 可重构 | ❌ 字符串依赖 | ✅ IDE 友好 | ✅ IDE 友好 |
| 配置集中 | ✅ 一个文件看清所有 | ❌ 分散在各处 | ✅ 集中或分散均可 |
| 适合场景 | 遗留系统, 第三方配置 | 简单场景 | 企业级推荐 |
| **推荐程度** | ❌ 遗留代码维护 | ✅ 搭配使用 | ✅ 首选方式 |

### 4.2 XML 配置方式 (了解为主，维护遗留代码时需要)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="
           http://www.springframework.org/schema/beans
           http://www.springframework.org/schema/beans/spring-beans.xsd">

    <!-- 1. 基础 Bean 定义 -->
    <bean id="userService" class="com.example.service.UserService"/>
    
    <!-- 2. 构造器注入 -->
    <bean id="orderService" class="com.example.service.OrderService">
        <constructor-arg ref="userRepository"/>
        <constructor-arg ref="paymentService"/>
        <constructor-arg name="maxRetryCount" value="3"/>
    </bean>
    
    <!-- 3. Setter 注入 -->
    <bean id="emailService" class="com.example.service.EmailService">
        <property name="host" value="smtp.example.com"/>
        <property name="port" value="587"/>
        <property name="credentials" ref="credentialProvider"/>
    </bean>
    
    <!-- 4. 内部 Bean (匿名 Bean) -->
    <bean id="paymentService" class="com.example.service.PaymentService">
        <property name="apiClient">
            <bean class="com.example.client.PaymentApiClient">
                <property name="apiKey" value="${payment.api.key}"/>
            </bean>
        </property>
    </bean>
    
    <!-- 5. 集合注入 -->
    <bean id="configService" class="com.example.service.ConfigService">
        <property name="servers">
            <list>
                <value>server1.example.com</value>
                <value>server2.example.com</value>
                <value>server3.example.com</value>
            </list>
        </property>
        <property name="timeoutConfig">
            <map>
                <entry key="connect" value="5000"/>
                <entry key="read" value="10000"/>
            </map>
        </property>
    </bean>

</beans>
```

### 4.3 注解配置方式

#### 4.3.1 组件扫描注解

```java
// ========== 层级注解体系 ==========
// Spring 提供了精确的语义化注解，对应三层架构的不同层次：

// ┌──────────────────────────────────┐
// │ @Component (通用)                 │
// │ ├── @Repository (数据访问层)      │
// │ ├── @Service (业务逻辑层)         │
// │ ├── @Controller (Web 层)         │
// │ └── @Configuration (配置类)      │
// └──────────────────────────────────┘
// 
// @Repository, @Service, @Controller 都是 @Component 的派生注解
// 它们在功能上完全等价，但提供了语义化分层，便于 AOP 切面定位

// ========== 数据访问层 ==========
@Repository  // 标记为数据访问组件，Spring 会自动转换 SQLException 为 DataAccessException
public class UserRepository {
    
    @Autowired  // Spring 会注入 JdbcTemplate
    private JdbcTemplate jdbcTemplate;
    
    public User findById(Long id) {
        return jdbcTemplate.queryForObject(
            "SELECT * FROM users WHERE id = ?",
            new BeanPropertyRowMapper<>(User.class),
            id
        );
    }
    
    public List<User> findAll() {
        return jdbcTemplate.query(
            "SELECT * FROM users",
            new BeanPropertyRowMapper<>(User.class)
        );
    }
}

// ========== 业务逻辑层 ==========
@Service  // 标记为服务组件
@Transactional(readOnly = true)  // 类级别事务，可被子类/方法继承
public class UserService {
    
    // 构造器注入 — 推荐方式（详见第 5 章）
    private final UserRepository userRepository;
    
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @Transactional  // 方法级别事务，覆盖类级别
    public User createUser(User user) {
        // 写操作
        return userRepository.save(user);
    }
}

// ========== Web 层 ==========
@Controller  // 标记为控制器组件
@RequestMapping("/api/users")  // 类级别 URL 映射
public class UserController {
    
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    @GetMapping("/{id}")
    @ResponseBody  // 直接返回 JSON
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);
    }
}

// ========== 配置类 ==========
@Configuration  // 告诉 Spring：这个类包含 @Bean 定义
@ComponentScan(basePackages = "com.example")  // 扫描指定包
@PropertySource("classpath:application.properties")  // 加载属性文件
public class AppConfig {
    
    @Bean
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:mysql://localhost:3306/db");
        ds.setUsername("root");
        ds.setPassword("password");
        return ds;
    }
}
```

#### 4.3.2 组件扫描 (ComponentScan)

```java
@Configuration
@ComponentScan(
    basePackages = {"com.example.service", "com.example.repository"},
    basePackageClasses = {MarkerInterface.class},
    includeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ANNOTATION, 
            value = {Controller.class, Service.class}
        )
    },
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = ".*Test.*"
        ),
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            value = {DeprecatedService.class}
        ),
        @ComponentScan.Filter(
            type = FilterType.CUSTOM,
            value = {CustomFilter.class}
        )
    },
    lazyInit = true  // 所有扫描到的 bean 默认懒加载
)
public class ScanConfig {
}

// ========== FilterType 详解 ==========
// ANNOTATION      — 按注解过滤
// ASSIGNABLE_TYPE — 按类/接口过滤
// ASPECTJ         — 按 AspectJ 表达式过滤
// REGEX           — 按正则表达式过滤
// CUSTOM          — 自定义 TypeFilter 实现

// 自定义 TypeFilter 示例：
public class CustomFilter implements TypeFilter {
    @Override
    public boolean match(MetadataReader metadataReader, 
                        MetadataReaderFactory metadataReaderFactory) 
            throws IOException {
        ClassMetadata classMetadata = metadataReader.getClassMetadata();
        // 排除名称中包含 "Internal" 的类
        return classMetadata.getClassName().contains("Internal");
    }
}
```

### 4.4 Java Config (推荐方式)

```java
// ========== 基础配置类 ==========
@Configuration
public class AppConfig {
    
    // ========== 1. 简单的 @Bean ==========
    // 方法名 = bean 的 id/name
    // 返回值 = bean 的类型
    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .url("jdbc:mysql://localhost:3306/db")
                .username("root")
                .password("password")
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .build();
    }
    
    // ========== 2. 带名称的 @Bean ==========
    @Bean("jdbcTemplate")
    @Bean(name = {"jdbcTemplate", "jdbc", "template"})  // 多个别名
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        // Spring 自动注入 DataSource 参数
        return new JdbcTemplate(dataSource);
    }
    
    // ========== 3. Bean 之间的依赖 ==========
    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
    
    // ========== 4. 初始化与销毁方法 ==========
    @Bean(initMethod = "init", destroyMethod = "cleanup")
    public CacheService cacheService() {
        return new CacheService();
    }
    
    // ========== 5. 条件性的 Bean ==========
    @Bean
    @ConditionalOnProperty(name = "feature.flag.enabled", havingValue = "true")
    public FeatureService featureService() {
        return new FeatureService();
    }
}

// ========== 使用 @Import 组合配置类 ==========
@Configuration
@Import({
    DataSourceConfig.class,
    ServiceConfig.class,
    WebConfig.class
})
public class AppConfig {
    // 将所有配置组合在一起
}

// ========== 使用 @ImportResource 导入 XML ==========
@Configuration
@ImportResource("classpath:legacy-config.xml")  // 混合使用 XML
public class MixedConfig {
    // 在 JavaConfig 中引用 XML 配置
}
```

#### 4.4.1 @Bean 注解详解

```java
// ========== @Bean 注解完整属性 ==========
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Bean {
    
    // Bean 的名称（默认为方法名）
    @AliasFor("name")
    String[] value() default {};
    
    @AliasFor("value")
    String[] name() default {};
    
    // 自动装配模式：byName, byType, constructor, no（默认 AutowireCapableBeanFactory）
    Autowire autowire() default Autowire.NO;
    
    // 初始化方法名（方法必须无参数）
    String initMethod() default "";
    
    // 销毁方法名
    String destroyMethod() default AbstractBeanDefinition.INFER_METHOD;
}

// ========== destroyMethod 的默认行为 (INFER_METHOD) ==========
// Spring 会自动查找名为 close 或 shutdown 的方法作为销毁方法
// 除非显式指定 destroyMethod = ""

@Bean(destroyMethod = "")  // 禁用自动推断
public DataSource dataSource() {
    // 某些 DataSource 实现的 close() 可能不是预期的销毁方法
    return new HikariDataSource();
}

@Bean  // 默认会找 close()/shutdown() 方法
public ExecutorService executor() {
    return Executors.newFixedThreadPool(10);
    // 容器关闭时会自动调用 executor.shutdown()
}
```

---

## 5. 依赖注入详解

### 5.1 三种注入方式对比

| 特性 | 构造器注入 | Setter 注入 | 字段注入 |
|------|-----------|-------------|----------|
| 可靠性 | ✅ 不可变，所有依赖必填 | ❌ 可被覆盖或遗漏 | ❌ 可被覆盖 |
| 测试性 | ✅ 直接 new 传参 | ✅ 调用 setter | ❌ 需要反射或框架支持 |
| 循环依赖 | ❌ 无法解决 | ✅ 可部分解决 | ✅ 可部分解决 |
| 不可变性 | ✅ final 字段 | ❌ 可变字段 | ❌ 可变字段 |
| IDE 友好 | ✅ 编译检查 | ✅ 编译检查 | ❌ 运行时错误 |
| **推荐度** | **首选** | 可选依赖使用 | **不推荐企业使用** |

### 5.2 构造器注入 (推荐)

```java
// ========== 标准构造器注入 ==========
@Service
public class OrderService {
    
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    
    // Spring 会自动匹配参数类型进行注入
    // 如果只有一个构造器，可以省略 @Autowired
    public OrderService(
            UserRepository userRepository,
            ProductRepository productRepository,
            PaymentService paymentService,
            NotificationService notificationService) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.paymentService = paymentService;
        this.notificationService = notificationService;
    }
}

// ========== 使用 Lombok 简化 ==========
@RequiredArgsConstructor  // 为所有 final 字段生成构造器
@Service
public class OrderService {
    
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    
    // Lombok @RequiredArgsConstructor 自动生成：
    // public OrderService(UserRepository ur, ProductRepository pr, 
    //                     PaymentService ps, NotificationService ns) { ... }
}

// ========== 可选参数 + 必选参数混合 ==========
@Service
public class FlexibleService {
    
    private final MandatoryDependency mandatory;  // 必选
    private OptionalDependency optional;          // 可选
    private String configValue;                   // 默认值
    
    // 必选依赖通过构造器注入
    public FlexibleService(MandatoryDependency mandatory) {
        this.mandatory = mandatory;
    }
    
    // 可选依赖通过 Setter 注入
    @Autowired(required = false)
    public void setOptional(OptionalDependency optional) {
        this.optional = optional;
    }
    
    @Value("${config.value:default}")
    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }
}
```

### 5.3 Setter 注入

```java
// ========== Setter 注入 ==========
// 适用场景：可选依赖、默认值可覆盖
@Service
public class NotificationService {
    
    private EmailSender emailSender;
    private SmsSender smsSender;
    private int retryCount = 3;  // 默认值
    
    @Autowired(required = false)  // 可选依赖
    public void setEmailSender(EmailSender emailSender) {
        this.emailSender = emailSender;
    }
    
    @Autowired(required = false)
    public void setSmsSender(SmsSender smsSender) {
        this.smsSender = smsSender;
    }
    
    @Value("${notification.retry.count:3}")
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
}
```

### 5.4 字段注入 (不推荐在企业代码中使用)

```java
// ========== 字段注入 ==========
// 虽然写起来最简洁，但被业界广泛认为是一种"反模式"
@Service
public class UserService {
    
    @Autowired  // 字段注入
    private UserRepository userRepository;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private AuditService auditService;
    
    // ❌ 问题 1: 无法用 final 修饰 — 字段可变
    // ❌ 问题 2: 测试时必须使用 Spring 容器或反射
    // ❌ 问题 3: 隐藏了依赖关系 — 从签名看不出需要什么
    // ❌ 问题 4: 违反单一职责 — 很容易无意识地增加依赖
    
    // 正确做法：
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final AuditService auditService;
    
    public UserService(UserRepository userRepository, 
                      EmailService emailService,
                      AuditService auditService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.auditService = auditService;
    }
}
```

### 5.5 @Autowired 注入机制详解

```java
// ========== @Autowired 的工作原理 ==========
// Spring 通过 AutowiredAnnotationBeanPostProcessor 处理 @Autowired
// 注入顺序：按类型匹配 (byType) → 按名称匹配 (byName)

@Service
public class AutowiredDemo {
    
    // 1. 字段注入 — Spring 通过反射设置字段值
    @Autowired
    private PaymentService paymentService;
    
    // 2. 构造器注入 — 首选方式
    // 如果只有一个构造器，@Autowired 可省略
    @Autowired  // 可省略（Spring 4.3+）
    public AutowiredDemo(OrderService orderService) {
        // ...
    }
    
    // 3. Setter 注入
    @Autowired
    public void setLogger(LoggerService logger) {
        // ...
    }
    
    // 4. 任意方法注入
    @Autowired
    public void configure(ConfigService config, DataSource dataSource) {
        // Spring 会自动解析参数
    }
    
    // 5. 集合/数组注入 — 自动收集同一类型的所有 Bean
    @Autowired
    private List<Validator> validators;  // 收集所有 Validator 实现
    
    @Autowired
    private Map<String, Filter> filterMap;  // key=bean name, value=bean
    
    // 6. Optional 注入 — 依赖可选
    @Autowired(required = false)
    private Optional<CacheService> cacheService;  // 没有实现时注入 Optional.empty()
}
```

### 5.6 解决歧义: @Qualifier, @Primary, @Resource, @Inject

```java
// ========== 场景：同一接口有多个实现 ==========
public interface PaymentService {
    void pay(Order order);
}

@Component
@Primary  // 5.6.1 @Primary — 标记为首选
public class AliPayService implements PaymentService { ... }

@Component
@Qualifier("wechatPay")
public class WechatPayService implements PaymentService { ... }

@Component
@Qualifier("unionPay")
public class UnionPayService implements PaymentService { ... }

// ========== 5.6.2 @Qualifier 指定注入哪个 Bean ==========
@Service
public class OrderService {
    
    private final PaymentService paymentService;
    
    // 方式 A：构造器参数上加 @Qualifier
    public OrderService(@Qualifier("wechatPay") PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    
    // 方式 B：字段注入 + @Qualifier
    @Autowired
    @Qualifier("unionPay")
    private PaymentService unionPayService;
    
    // 方式 C：使用 @Primary 标记的实现会被优先注入
    @Autowired
    private PaymentService primaryPaymentService;  // 拿到 AliPayService
}

// ========== 5.6.3 自定义 Qualifier 注解 ==========
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Qualifier  // 元注解 — 让自定义注解成为 Qualifier
public @interface PaymentType {
    PaymentMethod value();
    
    enum PaymentMethod {
        ALIPAY, WECHAT, UNIONPAY
    }
}

@Component
@PaymentType(PaymentMethod.ALIPAY)
public class AliPayService implements PaymentService { ... }

@Service
public class OrderService {
    // 使用自定义 Qualifier
    public OrderService(@PaymentType(PaymentMethod.ALIPAY) PaymentService paymentService) {
        // ...
    }
}

// ========== 5.6.4 @Resource (JSR-250) — 按名称注入 ==========
// @Resource 是 Java 标准注解，不是 Spring 特有的
// 默认按名称 (name) 匹配，找不到再按类型匹配
@Service
public class ResourceDemo {
    
    @Resource(name = "wechatPay")
    private PaymentService paymentService;  // 按 bean name "wechatPay" 注入
    
    @Resource  // 按字段名 "paymentService" 找 bean
    private PaymentService paymentServiceByName;
}

// ========== 5.6.5 @Inject (JSR-330) — Java 标准 ==========
// @Inject 是 Java CDI (Contexts and Dependency Injection) 的注解
// 行为基本等同于 @Autowired，但少了 required 属性
// 需要额外依赖：javax.inject (JDK 11-) / jakarta.inject (JDK 17+)
@Service
public class InjectDemo {
    
    @Inject
    private PaymentService paymentService;
    
    @Inject
    @Named("wechatPay")  // JSR-330 版 @Qualifier
    private PaymentService wechatPayService;
}

// ========== 各注解对比总结 ==========
// @Autowired    | Spring 特有   | byType → byName | required 属性
// @Resource     | JSR-250      | byName → byType  | name 属性
// @Inject       | JSR-330      | byType → byName  | @Named 配合使用
//
// 推荐：项目统一使用 @Autowired，团队约定一致即可
```

### 5.7 方法注入 (Method Injection)

```java
// ========== 场景：prototype bean 注入到 singleton bean ==========
// 问题：prototype 的 bean 注入到 singleton 后，每次拿到的都是同一个实例
// 解决方案：方法注入

// 方式 1：通过 ApplicationContext 手动获取（不推荐 — 与容器耦合）
@Component
public class SingletonBean {
    
    @Autowired
    private ApplicationContext context;
    
    public void usePrototype() {
        // 每次手动获取新的 prototype 实例
        PrototypeBean bean = context.getBean(PrototypeBean.class);
        bean.doSomething();
    }
}

// 方式 2：使用 @Lookup（推荐 — Spring 会生成子类实现）
@Component
public abstract class SingletonBean {
    
    public void usePrototype() {
        PrototypeBean bean = createPrototypeBean();
        bean.doSomething();
    }
    
    @Lookup  // Spring 会动态实现这个方法
    protected abstract PrototypeBean createPrototypeBean();
    
    // Spring 通过 CGLIB 生成子类，等价于：
    // protected PrototypeBean createPrototypeBean() {
    //     return applicationContext.getBean(PrototypeBean.class);
    // }
}

// 方式 3：Provider (JSR-330) — 接口方式
@Component
public class SingletonBean {
    
    @Autowired
    private Provider<PrototypeBean> prototypeBeanProvider;
    
    public void usePrototype() {
        PrototypeBean bean = prototypeBeanProvider.get();  // 每次都是新的
        bean.doSomething();
    }
}
```

### 5.8 @Autowired 与三级别缓存解决循环依赖

```java
// ========== 循环依赖问题 ==========
// 场景：A 依赖 B，B 依赖 A
@Service
public class AService {
    @Autowired
    private BService bService;  // A 需要 B
}

@Service
public class BService {
    @Autowired
    private AService aService;  // B 需要 A
}

// ========== Spring 如何解决 ==========
// Spring 通过"三级缓存"解决 singleton bean 的循环依赖
//
// ┌────────────────────────────────────────────────────────┐
// │                   三级缓存机制                           │
// ├────────────────────────────────────────────────────────┤
// │                                                        │
// │  Level 1: singletonObjects (已完成初始化的 Bean)        │
// │    └── 完全创建好的 Bean，可以直接使用                  │
// │                                                        │
// │  Level 2: earlySingletonObjects (早期 Bean)             │
// │    └── Bean 已经实例化但未完成属性填充                  │
// │    └── 通过三级缓存的 ObjectFactory 提前暴露            │
// │                                                        │
// │  Level 3: singletonFactories (Bean 工厂)                │
// │    └── 存放 ObjectFactory，调用 getObject()             │
// │    └── 可以生成 AOP 代理对象                            │
// │                                                        │
// └────────────────────────────────────────────────────────┘

// ========== 解决流程 (AService ←→ BService) ==========
// 1. 开始创建 A
// 2. A 实例化完成（构造器执行完，属性未填充）
// 3. A 被提前暴露到三级缓存（存入 ObjectFactory）
// 4. A 开始填充属性，发现需要 B
// 5. 开始创建 B
// 6. B 实例化完成
// 7. B 被提前暴露到三级缓存
// 8. B 开始填充属性，发现需要 A
// 9. B 从三级缓存拿到 A 的早期引用（ObjectFactory.getObject()）
//    → 此时 A 的引用被存入二级缓存
// 10. B 完成属性填充和初始化
// 11. B 存入一级缓存
// 12. A 获取 B 的完整引用
// 13. A 完成属性填充和初始化
// 14. A 存入一级缓存

// ========== Spring 能解决的循环依赖类型 ==========
// ✅ 字段注入（setter/field）— 支持，通过三级缓存
// ✅ Setter 注入 — 支持
// ❌ 构造器注入 — 不支持（实例化阶段就需要对方）
// ❌ prototype bean — 不支持（不缓存，无法提前暴露）

// ========== 构造器注入循环依赖 ==========
@Service
public class AService {
    private final BService bService;
    
    // 构造器注入 — 实例化时就需要 B
    public AService(BService bService) {
        this.bService = bService;
    }
    // ❌ 报错：Requested beans are currently in creation:
    //     Is there an unresolvable circular reference?
}

@Service
public class BService {
    private final AService aService;
    
    public BService(AService aService) {
        this.aService = aService;
    }
}

// ========== 解决方案 ==========
// 方案 1：改用 Setter 注入（不推荐，破坏不可变性）
// 方案 2：@Lazy 延迟加载其中一个依赖
@Service
public class AService {
    private final BService bService;
    
    public AService(@Lazy BService bService) {  // 使用代理延迟初始化
        this.bService = bService;
    }
    // Spring 创建 B 的代理对象注入给 A
    // 当 A 实际调用 bService 方法时，才真正初始化 B
}

// 方案 3：重新设计，消除循环依赖（最佳方案）
// 通常循环依赖意味着设计有问题，可以考虑引入中间层
```

---

## 6. Bean 作用域 (Scope)

### 6.1 作用域一览

| 作用域 | 描述 | 实例化时机 | 线程安全 |
|--------|------|-----------|---------|
| **singleton** (默认) | 每个容器一个实例 | 容器启动时（默认 eager） | 需自行保证（无状态最好） |
| **prototype** | 每次请求新实例 | 每次 getBean()/注入时 | 每次都是新对象，安全 |
| **request** | 每个 HTTP 请求一个实例 | 每次 HTTP 请求 | 仅 Web 应用 |
| **session** | 每个 HTTP Session 一个实例 | 每次新建 Session | 仅 Web 应用 |
| **application** | 每个 ServletContext 一个实例 | 应用启动时 | 仅 Web 应用 |
| **websocket** | 每个 WebSocket 一个实例 | 每次新建连接 | 仅 Web 应用 |

### 6.2 singleton 作用域

```java
// ========== singleton (默认) ==========
// 特性：每个 IoC 容器只有一个实例
// 适用：无状态的 service、repository、工具类
// 注意：必须在多线程环境下保持无状态

@Component  // 默认 scope = singleton
public class UserService {
    // 所有线程共享同一个实例
    // 不能持有 mutable 的实例变量
    // 方法内部的局部变量是安全的
}

// ========== 验证 singleton ==========
@SpringBootTest
class ScopeTest {
    
    @Autowired
    private ApplicationContext ctx;
    
    @Test
    void testSingleton() {
        ServiceA bean1 = ctx.getBean(ServiceA.class);
        ServiceA bean2 = ctx.getBean(ServiceA.class);
        
        assertSame(bean1, bean2);  // ✅ 同一个对象
        
        ServiceB bean3 = ctx.getBean(ServiceB.class);
        ServiceB bean4 = ctx.getBean(ServiceB.class);
        
        assertSame(bean3, bean4);  // ✅ 同一个对象
    }
}
```

### 6.3 prototype 作用域

```java
// ========== prototype ==========
// 特性：每次请求都创建新的实例
// 适用：有状态的组件、非线程安全的组件
// 注意：Spring 不管理 prototype bean 的完整生命周期

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
// 等价于 @Scope("prototype")
public class ShoppingCart {
    private List<Item> items = new ArrayList<>();
    
    public void addItem(Item item) {
        items.add(item);
    }
    
    // 每个用户会话使用独立的 ShoppingCart 实例
}

// ========== 验证 prototype ==========
@SpringBootTest
class PrototypeTest {
    
    @Autowired
    private ApplicationContext ctx;
    
    @Test
    void testPrototype() {
        ShoppingCart cart1 = ctx.getBean(ShoppingCart.class);
        ShoppingCart cart2 = ctx.getBean(ShoppingCart.class);
        
        assertNotSame(cart1, cart2);  // ✅ 不同对象
        
        cart1.addItem(new Item("Book"));
        assertEquals(1, cart1.getItems().size());
        assertEquals(0, cart2.getItems().size());  // ✅ 互不影响
    }
}

// ========== prototype + singleton 注坑 ==========
// 问题：prototype bean 注入到 singleton 后，每次拿到的都是同一个
@Component
public class SingletonClient {
    
    @Autowired
    private ShoppingCart cart;  // 虽然 cart 是 prototype scope
    
    public void process() {
        // 每次调用都是同一个 cart 实例 ❌
        // prototype 失效了！
    }
}

// 原因：singleton bean 只在初始化时注入一次依赖
// 解决方案：使用 @Lookup / Provider / ApplicationContext
// 详见本章 5.7 方法注入
```

### 6.4 Web 作用域 (request, session, application, websocket)

```java
// ========== request 作用域 ==========
// 每个 HTTP 请求创建一个实例，请求结束后销毁
@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
// 等价于 @RequestScope (Spring 4.3+)
public class RequestContext {
    
    private String requestId;
    private String remoteIp;
    
    public void init(HttpServletRequest request) {
        this.requestId = UUID.randomUUID().toString();
        this.remoteIp = request.getRemoteAddr();
    }
    
    // 在 Controller 中使用：
    // @Autowired
    // private RequestContext context;  // 当前请求的上下文
}

// ========== session 作用域 ==========
// 每个 HTTP Session 创建一个实例，会话结束后销毁
@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
// 等价于 @SessionScope
public class UserSession {
    
    private Long userId;
    private String username;
    private List<String> permissions;
    
    // 购物车、登录信息等适合放在 Session Bean 中
    // @Autowired
    // private UserSession currentUser;  // 当前用户会话
}

// ========== ScopedProxyMode ==========
// Web 作用域的 Bean 需要代理模式：
// INTERFACES — JDK 动态代理（要求 bean 实现接口）
// TARGET_CLASS — CGLIB 代理（推荐，不需要接口）
// NO — 无代理（不适用于 singleton 中注入 request/session bean）

// 为什么需要代理？
// singleton bean 在初始化时注入 request bean
// 但此时还没有 HTTP 请求，无法创建 request bean
// 代理对象作为"占位符"，在运行时再委托给实际 bean
```

### 6.5 自定义作用域

```java
// ========== 自定义作用域 ==========
// 实现 Scope 接口，可以创建自定义作用域

public class ThreadScope implements Scope {
    
    private final ThreadLocal<Map<String, Object>> threadScope = 
        ThreadLocal.withInitial(HashMap::new);
    
    @Override
    public Object get(String name, ObjectFactory<?> objectFactory) {
        Map<String, Object> scope = threadScope.get();
        Object bean = scope.get(name);
        if (bean == null) {
            bean = objectFactory.getObject();
            scope.put(name, bean);
        }
        return bean;
    }
    
    @Override
    public Object remove(String name) {
        return threadScope.get().remove(name);
    }
    
    @Override
    public void registerDestructionCallback(String name, Runnable callback) {
        // 线程结束时的清理逻辑
    }
    
    @Override
    public Object resolveContextualObject(String key) {
        return null;
    }
    
    @Override
    public String getConversationId() {
        return "thread-" + Thread.currentThread().getId();
    }
}

// 注册自定义作用域：
@Configuration
public class ScopeConfig {
    
    @Bean
    public static BeanFactoryPostProcessor scopeRegistrar() {
        return beanFactory -> {
            beanFactory.registerScope("thread", new ThreadScope());
        };
    }
}

// 使用：
@Component
@Scope("thread")
public class ThreadScopedBean {
    // 每个线程拥有独立的实例
}
```

---

## 7. Bean 生命周期详解

### 7.1 完整生命周期流程

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Spring Bean 完整生命周期                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  1. Bean 定义加载与注册                                             │
│     ● 解析配置（XML/注解/JavaConfig）→ BeanDefinition              │
│     ● BeanDefinitionRegistry 注册 BeanDefinition                   │
│                                                                     │
│  2. BeanFactoryPostProcessor 处理                                    │
│     ● PropertySourcesPlaceholderConfigurer 处理 ${} 占位符          │
│     ● 修改 BeanDefinition 的属性                                    │
│     ● 常见的 BeanFactoryPostProcessor                              │
│                                                                     │
│  3. 实例化 Bean (Instantiation)                                     │
│     ● 通过反射调用构造器（或工厂方法）                              │
│     ● 构造器循环依赖检测（三级缓存）                                │
│                                                                     │
│  4. 属性填充 (Populate Properties)                                  │
│     ● Setter 注入 / 字段注入                                        │
│     ● @Autowired, @Resource, @Inject 处理                           │
│                                                                     │
│  5. 设置 Bean Name (BeanNameAware)                                  │
│     ● setBeanName(String name) 回调                                 │
│                                                                     │
│  6. 设置 BeanFactory (BeanFactoryAware)                             │
│     ● setBeanFactory(BeanFactory) 回调                              │
│                                                                     │
│  7. 设置 ApplicationContext (ApplicationContextAware)               │
│     ● setApplicationContext(ApplicationContext) 回调                │
│     ● 其他 Aware 接口回调                                           │
│                                                                     │
│  8. BeanPostProcessor#postProcessBeforeInitialization               │
│     ● 初始化前的后置处理                                            │
│     ● @PostConstruct 调用位置（在 Before 中通过 InitDestroyAnnotationBeanPostProcessor 触发）  │
│                                                                     │
│  9. 初始化 (Initialization)                                         │
│     ● @PostConstruct 标注的方法                                     │
│     ● InitializingBean#afterPropertiesSet()                        │
│     ● @Bean(initMethod="customInit") 自定义初始化方法               │
│                                                                     │
│  10. BeanPostProcessor#postProcessAfterInitialization               │
│      ● 初始化后的后置处理                                           │
│      ● AOP 代理在此阶段创建                                        │
│      ● 返回的 bean 可能是代理对象                                  │
│                                                                     │
│  11. Bean 就绪 (Ready to Use)                                       │
│      ● 存入一级缓存 (singletonObjects)                              │
│      ● 可以被应用程序使用                                          │
│                                                                     │
│  12. 容器关闭 (Destruction)                                         │
│      ● @PreDestroy 标注的方法                                      │
│      ● DisposableBean#destroy()                                    │
│      ● @Bean(destroyMethod="customDestroy") 自定义销毁方法         │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 7.2 详尽的代码示例

```java
// ========== 展示生命周期所有回调的示例 Bean ==========
@Component
public class LifecycleDemoBean implements BeanNameAware, BeanFactoryAware, 
        ApplicationContextAware, InitializingBean, DisposableBean {
    
    private static final Logger log = LoggerFactory.getLogger(LifecycleDemoBean.class);
    
    private String name;
    
    // ========== 构造器 (阶段 3) ==========
    public LifecycleDemoBean() {
        log.info("① 构造器: 实例化完成");
    }
    
    // ========== 属性填充 (阶段 4) ==========
    @Autowired
    public void setDependency(SomeDependency dependency) {
        log.info("② 属性填充: 注入依赖 {}", dependency);
    }
    
    // ========== BeanNameAware (阶段 5) ==========
    @Override
    public void setBeanName(String name) {
        this.name = name;
        log.info("③ BeanNameAware: 设置 bean name = {}", name);
    }
    
    // ========== BeanFactoryAware (阶段 6) ==========
    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        log.info("④ BeanFactoryAware: 获取 BeanFactory 引用");
    }
    
    // ========== ApplicationContextAware (阶段 7) ==========
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) 
            throws BeansException {
        log.info("⑤ ApplicationContextAware: 获取 ApplicationContext 引用");
    }
    
    // ========== postProcessBeforeInitialization (阶段 8) ==========
    // 通过自定义 BeanPostProcessor 实现
    
    // ========== @PostConstruct (阶段 9, 顺序 1) ==========
    @PostConstruct
    public void postConstruct() {
        log.info("⑥ @PostConstruct: JSR-250 初始化回调");
    }
    
    // ========== InitializingBean (阶段 9, 顺序 2) ==========
    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("⑦ InitializingBean.afterPropertiesSet(): Spring 初始化回调");
    }
    
    // ========== @Bean initMethod (阶段 9, 顺序 3) ==========
    // 见 @Bean(initMethod="customInit")
    public void customInit() {
        log.info("⑧ initMethod: 自定义初始化方法");
    }
    
    // ========== postProcessAfterInitialization (阶段 10) ==========
    // 通过自定义 BeanPostProcessor 实现
    
    // ========== Bean 使用阶段 (阶段 11) ==========
    public void doWork() {
        log.info("⑨ 业务方法: Bean 正在工作");
    }
    
    // ========== @PreDestroy (阶段 12, 顺序 1) ==========
    @PreDestroy
    public void preDestroy() {
        log.info("⑩ @PreDestroy: JSR-250 销毁回调");
    }
    
    // ========== DisposableBean (阶段 12, 顺序 2) ==========
    @Override
    public void destroy() throws Exception {
        log.info("⑪ DisposableBean.destroy(): Spring 销毁回调");
    }
    
    // ========== @Bean destroyMethod (阶段 12, 顺序 3) ==========
    public void customDestroy() {
        log.info("⑫ destroyMethod: 自定义销毁方法");
    }
}

// ========== 运行输出 ==========
// ① 构造器: 实例化完成
// ② 属性填充: 注入依赖 com.example.SomeDependency@...
// ③ BeanNameAware: 设置 bean name = lifecycleDemoBean
// ④ BeanFactoryAware: 获取 BeanFactory 引用
// ⑤ ApplicationContextAware: 获取 ApplicationContext 引用
// ⑥ @PostConstruct: JSR-250 初始化回调
// ⑦ InitializingBean.afterPropertiesSet(): Spring 初始化回调
// ... (容器启动完成) ...
// ⑨ 业务方法: Bean 正在工作
// ... (容器关闭) ...
// ⑩ @PreDestroy: JSR-250 销毁回调
// ⑪ DisposableBean.destroy(): Spring 销毁回调
```

### 7.3 BeanPostProcessor 深入解析

```java
// ========== BeanPostProcessor ==========
// 接口：在 Bean 初始化前后提供扩展点
// 作用：包装/替换/代理 Bean，注入框架基础设施
// 常见实现：
//   - AutowiredAnnotationBeanPostProcessor (@Autowired 处理)
//   - CommonAnnotationBeanPostProcessor (@PostConstruct, @PreDestroy, @Resource)
//   - AbstractAdvisorAutoProxyCreator (AOP 代理创建)

@Component  // BeanPostProcessor 会作用于容器中所有 Bean
public class CustomBeanPostProcessor implements BeanPostProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(CustomBeanPostProcessor.class);
    
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) 
            throws BeansException {
        // 在所有 Bean 初始化之前调用
        // 可以返回包装后的对象
        
        if (bean instanceof ImportantBean) {
            log.info("BeforeInit: {} is being initialized", beanName);
        }
        
        return bean;  // 返回原始或替换的对象
    }
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) 
            throws BeansException {
        // 在所有 Bean 初始化之后调用
        // AOP 代理通常在此阶段创建
        
        if (bean instanceof Auditable) {
            // 为 Auditable 接口实现创建代理
            return Proxy.newProxyInstance(
                bean.getClass().getClassLoader(),
                bean.getClass().getInterfaces(),
                (proxy, method, args) -> {
                    log.info("Audit: {}.{} called", beanName, method.getName());
                    return method.invoke(bean, args);
                }
            );
        }
        
        return bean;
    }
}

// ========== 使用 Ordered 接口控制执行顺序 ==========
@Component
public class OrderedPostProcessor implements BeanPostProcessor, Ordered {
    
    @Override
    public int getOrder() {
        return 0;  // 数值越小，优先级越高
    }
    // ... 实现方法
}

// ========== 限制作用范围的 BeanPostProcessor ==========
// 通过 instanceof 检查，只处理特定类型的 Bean
public class TargetedPostProcessor implements BeanPostProcessor {
    
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        // 只处理实现了特定接口的 Bean
        if (!(bean instanceof MyCustomInterface)) {
            return bean;  // 不处理
        }
        
        // 对 MyCustomInterface 的实现进行增强
        return new MyCustomProxy((MyCustomInterface) bean);
    }
}
```

### 7.4 BeanFactoryPostProcessor 深入解析

```java
// ========== BeanFactoryPostProcessor ==========
// 在 Bean 实例化之前执行，可以修改 BeanDefinition 的元数据
// 执行时机：所有 BeanDefinition 加载完成后，Bean 实例化之前

@Component
public class CustomBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(CustomBeanFactoryPostProcessor.class);
    
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) 
            throws BeansException {
        // 遍历所有 BeanDefinition
        String[] beanNames = beanFactory.getBeanDefinitionNames();
        
        for (String beanName : beanNames) {
            BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
            
            // 动态修改 BeanDefinition
            if (bd.getClassName() != null && bd.getClassName().contains("DataSource")) {
                bd.setLazyInit(true);  // 所有 DataSource 改为懒加载
                log.info("Modified {}: lazyInit=true", beanName);
            }
        }
    }
}

// ========== 常见内置实现 ==========
// PropertySourcesPlaceholderConfigurer
//   - 处理 @Value("${...}") 占位符
//   - 必须在 Bean 实例化前执行
//   - 通常通过 @Bean static 方法注册
//   - 注：Spring Boot 自动注册，无需手动配置

// CustomEditorConfigurer
//   - 注册自定义属性编辑器
//   - 用于字符串到特定类型的转换

// ========== 为什么 BeanFactoryPostProcessor 必须是 static 的 ==========
@Configuration
public class Config {
    
    @Bean
    public static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
        // 必须 static：因为 BeanFactoryPostProcessor 要在 @Configuration
        // 本身的 @PostConstruct 之前执行
        return new PropertySourcesPlaceholderConfigurer();
    }
}

// ========== BeanDefinitionRegistryPostProcessor ==========
// BeanFactoryPostProcessor 的扩展接口
// 可以注册新的 BeanDefinition
@Component
public class CustomBeanDefinitionRegistry implements BeanDefinitionRegistryPostProcessor {
    
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) 
            throws BeansException {
        // 动态注册 BeanDefinition
        BeanDefinitionBuilder builder = BeanDefinitionBuilder
            .genericBeanDefinition(MyDynamicBean.class)
            .addPropertyValue("name", "dynamic")
            .setScope(BeanDefinition.SCOPE_SINGLETON);
        
        registry.registerBeanDefinition("myDynamicBean", builder.getBeanDefinition());
    }
    
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) 
            throws BeansException {
        // 标准 BeanFactoryPostProcessor 逻辑
    }
}
```

### 7.5 Aware 接口详解

```java
// ========== Aware 接口族谱 ==========
// Aware 是 Spring 提供的"回调钩子"，让 Bean 获取容器基础设施
// 所有 Aware 接口都在"属性填充后、初始化前"调用

// ┌──────────────────────────────────────────┐
// │              Aware (标记接口)              │
// ├──────────────────────────────────────────┤
// │ BeanNameAware              → setBeanName │
// │ BeanFactoryAware           → setBeanFactory │
// │ ApplicationContextAware    → setApplicationContext │
// │ ApplicationEventPublisherAware → setApplicationEventPublisher │
// │ MessageSourceAware         → setMessageSource │
// │ ResourceLoaderAware        → setResourceLoader │
// │ EnvironmentAware           → setEnvironment │
// │ EmbeddedValueResolverAware → setEmbeddedValueResolver │
// │ ImportAware                → setImportMetadata │
// └──────────────────────────────────────────┘

// ========== 实用示例：获取 Environment ==========
@Component
public class EnvironmentAwareBean implements EnvironmentAware {
    
    private Environment environment;
    
    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
    
    public String getActiveProfile() {
        return Arrays.toString(environment.getActiveProfiles());
    }
    
    public String getProperty(String key) {
        return environment.getProperty(key);
    }
}

// ========== 实用示例：动态注册 Bean ==========
@Component
public class DynamicBeanRegistrar implements ApplicationContextAware {
    
    private ApplicationContext applicationContext;
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    public <T> T getBean(Class<T> clazz) {
        // 虽然 getBean 方便，但应用层不应该使用
        // 这是给框架基础设施使用的
        return applicationContext.getBean(clazz);
    }
}
```

---

## 8. @Configuration 与 @Bean

### 8.1 Full 模式 vs Lite 模式

```java
// ========== Full 模式 ==========
// 类上标注 @Configuration（且 proxyBeanMethods = true，默认）
// 特点：Spring 通过 CGLIB 创建代理
// 效果：@Bean 方法之间调用会经过代理，确保 singleton

@Configuration  // Full 模式 — proxyBeanMethods = true
public class FullModeConfig {
    
    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .url("jdbc:mysql://localhost:3306/db")
                .build();
    }
    
    @Bean
    public JdbcTemplate jdbcTemplate() {
        // 调用 dataSource() 方法
        // Full 模式下：Spring 不会真的调用 dataSource()
        // 而是从容器中获取已有的 dataSource bean
        return new JdbcTemplate(dataSource());  // ✅ 同一个 singleton 实例
    }
    
    @Bean
    public TransactionManager transactionManager() {
        return new DataSourceTransactionManager(dataSource());  // ✅ 同一实例
    }
}

// ========== Lite 模式 ==========
// 类上没有 @Configuration（只有 @Component / @Bean 方法在普通类中）
// 或 @Configuration(proxyBeanMethods = false)
// 特点：没有 CGLIB 代理，每次 @Bean 方法调用都是真实的 Java 调用

@Configuration(proxyBeanMethods = false)  // Lite 模式
public class LiteModeConfig {
    
    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .url("jdbc:mysql://localhost:3306/db")
                .build();
    }
    
    @Bean
    public JdbcTemplate jdbcTemplate() {
        // Lite 模式下：每次调用 dataSource() 都会执行方法体
        // 可能创建不同的 DataSource 实例！
        return new JdbcTemplate(dataSource());  // ⚠️ 可能拿到不同实例
    }
}

// ========== 什么时候用 Lite 模式？ ==========
// 1. 性能敏感场景 — 避免 CGLIB 代理开销
// 2. @Bean 方法之间没有相互调用时
// 3. 配置类本身不需要被增强时

// ========== 在普通类中使用 @Bean (Lite 模式) ==========
@Component  // 普通类，不是 @Configuration
public class CommonBeanConfig {
    
    @Bean  // Lite 模式
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl("https://api.example.com")
                .build();
    }
    // 这两个 @Bean 方法没有相互依赖，Lite 模式完全够用
}
```

### 8.2 @Bean 方法的依赖注入

```java
// ========== 方式 1：方法参数注入 (推荐) ==========
@Configuration
public class MethodParamConfig {
    
    @Bean
    public ServiceA serviceA(ServiceB serviceB, ServiceC serviceC) {
        // Spring 自动从容器中注入 serviceB 和 serviceC
        return new ServiceA(serviceB, serviceC);
    }
}

// ========== 方式 2：方法调用注入 (仅 Full 模式) ==========
@Configuration
public class MethodCallConfig {
    
    @Bean
    public ServiceB serviceB() {
        return new ServiceB();
    }
    
    @Bean
    public ServiceA serviceA() {
        // Full 模式下，Spring 会拦截 serviceB() 调用，从容器获取
        return new ServiceA(serviceB());
    }
}

// ========== 方式 3：通过字段注入 (不推荐) ==========
@Configuration
public class FieldInjectConfig {
    
    @Autowired
    private ServiceB serviceB;  // 字段注入
    
    @Bean
    public ServiceA serviceA() {
        return new ServiceA(serviceB);
    }
    // 不推荐：Configuration 类应该专注于 Bean 定义
}
```

### 8.3 @Conditional 条件装配

```java
// ========== @Conditional 基础用法 ==========
// 根据条件决定是否创建 Bean

@Configuration
public class ConditionalConfig {
    
    @Bean
    @Conditional(WindowsCondition.class)  // 仅 Windows 环境创建
    public WindowsService windowsService() {
        return new WindowsService();
    }
    
    @Bean
    @Conditional(LinuxCondition.class)  // 仅 Linux 环境创建
    public LinuxService linuxService() {
        return new LinuxService();
    }
}

// ========== 实现 Condition 接口 ==========
public class WindowsCondition implements Condition {
    
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String osName = context.getEnvironment().getProperty("os.name");
        return osName != null && osName.toLowerCase().contains("windows");
    }
}

public class LinuxCondition implements Condition {
    
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String osName = context.getEnvironment().getProperty("os.name");
        return osName != null && osName.toLowerCase().contains("linux");
    }
}

// ========== ConditionContext 提供的信息 ==========
// context.getRegistry()      — BeanDefinitionRegistry
// context.getBeanFactory()   — ConfigurableListableBeanFactory
// context.getEnvironment()   — Environment
// context.getResourceLoader()— ResourceLoader
// context.getClassLoader()   — ClassLoader

// ========== 自定义 @ConditionalOn 注解 ==========
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(OnPropertyCondition.class)
public @interface ConditionalOnProperty {
    String name();
    String havingValue() default "";
    boolean matchIfMissing() default false;
}

public class OnPropertyCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(
            ConditionalOnProperty.class.getName());
        
        String name = (String) attributes.get("name");
        String havingValue = (String) attributes.get("havingValue");
        boolean matchIfMissing = (boolean) attributes.get("matchIfMissing");
        
        String actualValue = context.getEnvironment().getProperty(name);
        
        if (actualValue == null) {
            return matchIfMissing;
        }
        
        return havingValue.isEmpty() || havingValue.equals(actualValue);
    }
}
```

### 8.4 @Profile 环境配置

```java
// ========== @Profile 基础 ==========
@Configuration
@Profile("dev")  // 仅在 dev profile 激活时加载
public class DevConfig {
    
    @Bean
    public DataSource devDataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .build();
    }
}

@Configuration
@Profile("prod")  // 仅在 prod profile 激活时加载
public class ProdConfig {
    
    @Bean
    public DataSource prodDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:mysql://prod-db:3306/db");
        ds.setMaximumPoolSize(50);
        return ds;
    }
}

// ========== 方法级别 @Profile ==========
@Configuration
public class ServiceConfig {
    
    @Bean
    @Profile("dev")  // dev 环境下使用模拟服务
    public PaymentService mockPaymentService() {
        return new MockPaymentService();
    }
    
    @Bean
    @Profile("prod")  // prod 环境下使用真实服务
    public PaymentService realPaymentService() {
        return new RealPaymentService();
    }
    
    @Bean
    @Profile("test")  // 测试环境下使用的特殊配置
    public PaymentService testPaymentService() {
        return new TestPaymentService();
    }
}

// ========== @Profile 与逻辑运算 ==========
// 支持：!, &, |
@Configuration
@Profile("!dev")  // 非 dev 环境
public class NonDevConfig {}

@Configuration
@Profile("dev & staging")  // dev AND staging（一般不会同时激活）
public class DevAndStagingConfig {}

@Configuration
@Profile("dev | staging")  // dev OR staging
public class DevOrStagingConfig {}

// ========== 激活 Profile ==========
// 方式 1：application.properties
// spring.profiles.active=dev,test

// 方式 2：命令行参数
// --spring.profiles.active=prod

// 方式 3：环境变量
// SPRING_PROFILES_ACTIVE=prod,redis

// 方式 4：编程方式
// ConfigurableEnvironment env = ctx.getEnvironment();
// env.setActiveProfiles("dev");

// ========== @Profile 的默认行为 ==========
// 如果没有设置 spring.profiles.active
// 所有没有 @Profile 标注的 Bean 会被加载
// 有 @Profile 但条件不匹配的 Bean 不会被加载
```

---

## 9. 属性管理与外部化配置

### 9.1 @PropertySource 加载属性文件

```java
// ========== @PropertySource 基础 ==========
@Configuration
@PropertySource("classpath:config.properties")
@PropertySource("file:/opt/config/override.properties")
@PropertySource(value = "classpath:secret.properties", ignoreResourceNotFound = true)
// ignoreResourceNotFound=true：文件不存在时不报错
public class PropertyConfig {
    
    @Value("${app.name}")
    private String appName;
    
    @Value("${app.version:1.0.0}")  // 带默认值
    private String appVersion;
    
    @Value("#{systemProperties['user.home']}")  // SpEL
    private String userHome;
}

// ========== 配置文件的属性 ==========
# config.properties
app.name=MyApp
app.version=2.0.0
app.description=${app.name} is version ${app.version}  # 引用其他属性
```

### 9.2 @Value 详解

```java
// ========== @Value 支持多种表达式 ==========
@Component
public class ValueDemo {
    
    // 1. 属性占位符 (Property Placeholder)
    @Value("${database.url:jdbc:default://localhost}")  // 带默认值
    private String databaseUrl;
    
    @Value("${database.username}")
    private String username;
    
    @Value("${database.password:#{null}}")  // 默认 null
    private String password;
    
    // 2. SpEL 表达式
    @Value("#{systemProperties['java.version']}")
    private String javaVersion;
    
    @Value("#{systemEnvironment['PATH']}")
    private String pathEnv;
    
    @Value("#{T(java.lang.Math).random() * 100.0}")
    private double randomNumber;
    
    @Value("#{@dataSource.url}")  // 引用其他 Bean 的属性
    private String dataSourceUrl;
    
    // 3. 字面量
    @Value("Hello World")  // 直接字符串
    private String greeting;
    
    @Value("true")
    private boolean booleanValue;
    
    @Value("100")
    private int intValue;
    
    // 4. 集合转换
    @Value("${server.hosts:host1,host2,host3}")  // 逗号分隔
    private List<String> hosts;
    
    // 注：@Value 不适合复杂对象的绑定，复杂对象用 @ConfigurationProperties
}
```

### 9.3 @ConfigurationProperties 类型安全配置

```java
// ========== @ConfigurationProperties ==========
// 提供类型安全的属性绑定
// 支持嵌套对象、List、Map 等复杂结构
// 比 @Value 更适合管理一组相关属性

// ========== 1. 基本用法 ==========
@ConfigurationProperties(prefix = "app")  // 前缀 app.xxx
@Component  // 注册为 Bean
public class AppProperties {
    
    private String name;
    private String version;
    private String description;
    
    // getters and setters (必须)
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

// application.yml:
// app:
//   name: MyApp
//   version: 2.0.0
//   description: ${app.name} is ${app.version}

// ========== 2. 嵌套对象配置 ==========
@ConfigurationProperties(prefix = "app")
@Component
public class AppProperties {
    
    private String name;
    private Database database = new Database();  // 嵌套对象
    private Security security = new Security();
    
    // getters, setters...
    
    public static class Database {
        private String url;
        private String username;
        private String password;
        private int maxPoolSize = 10;  // 默认值
        private List<String> servers;
        private Map<String, String> options;
        
        // getters, setters...
    }
    
    public static class Security {
        private boolean enabled = true;
        private List<String> allowedOrigins;
        private Jwt jwt = new Jwt();
        
        public static class Jwt {
            private String secret;
            private long expirationMs = 3600000;
            // getters, setters...
        }
    }
}

// application.yml:
// app:
//   name: MyApp
//   database:
//     url: jdbc:mysql://localhost:3306/db
//     username: root
//     password: secret
//     max-pool-size: 20
//     servers:
//       - primary:3306
//       - replica:3306
//     options:
//       useSSL: "false"
//       serverTimezone: UTC
//   security:
//     enabled: true
//     allowed-origins:
//       - http://localhost:3000
//       - https://example.com
//     jwt:
//       secret: my-secret-key
//       expiration-ms: 7200000

// ========== 3. @ConfigurationProperties 校验 ==========
@ConfigurationProperties(prefix = "app")
@Validated  // 启用校验
@Component
public class ValidatedProperties {
    
    @NotBlank
    private String name;
    
    @NotNull
    @Valid  // 嵌套校验
    private Database database;
    
    @Min(1)
    @Max(65535)
    private int port = 8080;
    
    @NotEmpty
    private List<String> allowedOrigins;
    
    // getters, setters...
    
    @Validated
    public static class Database {
        @NotBlank
        private String url;
        
        @NotBlank
        private String username;
        
        @Min(1)
        private int maxPoolSize = 10;
        
        // getters, setters...
    }
}

// ========== 4. 启用 @ConfigurationProperties ==========
// 方式 1：@Component (最方便)
// 方式 2：@EnableConfigurationProperties
@Configuration
@EnableConfigurationProperties(AppProperties.class)  // 注册属性类
public class PropertiesConfig {
}

// 方式 3：@ConfigurationPropertiesScan (Spring Boot 2.2+)
@SpringBootApplication
@ConfigurationPropertiesScan("com.example.config")  // 扫描指定包的 @ConfigurationProperties
public class Application {
}

// ========== 5. Relaxed Binding (松散绑定) ==========
// Spring Boot 支持多种属性名风格，自动转换：
// database.url        ← 推荐 kebab-case
// database.url        ← camelCase
// database.url        ← underscore
// DATABASE_URL        ← UPPER_CASE (环境变量)
// database-url        ← kebab-case
// 
// 所有这些都会绑定到 properties.database.url
```

### 9.4 Configuration Priority (配置优先级)

```java
// ========== Spring Boot 外部化配置 17 级优先级 (从高到低) ==========
// 
// 1. @TestPropertySource 注解 (测试)
// 2. 命令行参数 (--server.port=9090)
// 3. JNDI 属性 (java:comp/env)
// 4. Java 系统属性 (System.getProperties())
// 5. OS 环境变量
// 6. RandomValuePropertySource (random.*)
// 7. application-{profile}.properties|yml (profile 特定)
// 8. application.properties|yml (主配置)
// 9. @PropertySource 注解
// 10. SpringApplication.setDefaultProperties()
// 
// 注意：优先级高的覆盖优先级低的

// ========== 命令行参数 ==========
// java -jar app.jar --server.port=9090 --app.name=MyApp

// ========== 环境变量方式 ==========
// 在 Linux/Mac: export SERVER_PORT=9090
// 在 Windows: set SERVER_PORT=9090
// 注意：环境变量使用大写 + 下划线

// ========== 不同方式示例 ==========
// 目标：设置 database.url
// 
// 命令行: --app.database.url=jdbc:mysql://localhost:3306/db
// 环境变量: APP_DATABASE_URL=jdbc:mysql://localhost:3306/db
// application.yml: app.database.url=jdbc:mysql://localhost:3306/db
// Java 系统属性: -Dapp.database.url=jdbc:mysql://localhost:3306/db
```

---

## 10. Spring Expression Language (SpEL)

```java
// ========== SpEL 概述 ==========
// Spring Expression Language: 在运行时查询和操作对象图的表达式语言
// 语法: #{expression}
// 用途: @Value, XML配置, 安全表达式, 缓存 key

// ========== 1. 基本表达式 ==========
@Value("#{1 + 1}")                    // 算术: 2
@Value("#{'Hello ' + 'World'}")       // 字符串拼接: "Hello World"
@Value("#{1 == 1}")                   // 比较: true
@Value("#{age > 18 ? 'Adult' : 'Minor'}")  // 三元运算

// ========== 2. 访问 Bean 和属性 ==========
@Value("#{@dataSource.url}")          // 访问 Bean 的属性
@Value("#{@userService.getDefaultUser()}")  // 调用 Bean 方法
@Value("#{systemProperties['user.home']}")  // 系统属性
@Value("#{systemEnvironment['PATH']}")      // 环境变量

// ========== 3. 静态方法和常量 ==========
@Value("#{T(java.lang.Math).random()}")           // 静态方法
@Value("#{T(java.lang.Math).PI}")                 // 静态常量
@Value("#{T(java.util.UUID).randomUUID().toString()}")  // UUID 生成

// ========== 4. 集合操作 ==========
@Value("#{{'a', 'b', 'c'}}")                     // 创建 List
@Value("#{{'key1':'value1', 'key2':'value2'}}")  // 创建 Map
@Value("#{users.?[age > 18]}")                   // 过滤: 成年人
@Value("#{users.![name]}")                       // 投影: 提取名称
@Value("#{users.?[age > 18].![name]}")           // 过滤 + 投影

// ========== 5. 安全导航 (Safe Navigation) ==========
@Value("#{user?.address?.city}")        // 防止 NPE, 如果 user 或 address 为 null 返回 null
// 等价于: user != null && user.address != null ? user.address.city : null

// ========== 6. 正则表达式匹配 ==========
@Value("#{'abc123' matches '\\w+'}")   // true

// ========== 7. 自定义函数注册 ==========
@Configuration
public class SpelConfig {
    
    @Bean
    public static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }
}

@Component
public class SpelService {
    
    // 更复杂的 SpEL 可用在使用 SpelExpressionParser 的场景
    public void evaluate() {
        ExpressionParser parser = new SpelExpressionParser();
        
        // 解析表达式
        Expression exp = parser.parseExpression("'Hello ' + 'World'");
        String result = exp.getValue(String.class);  // "Hello World"
        
        // 在特定上下文中求值
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        ctx.setVariable("name", "Spring");
        String msg = parser.parseExpression(
            "'Hello ' + #name").getValue(ctx, String.class);  // "Hello Spring"
        
        // 操作对象
        User user = new User("John", 25);
        ctx.setRootObject(user);
        String name = parser.parseExpression("name").getValue(ctx, String.class);  // "John"
    }
}
```

---

## 11. 国际化 (i18n)

### 11.1 MessageSource 配置

```java
// ========== 国际化配置 ==========
// Spring 通过 MessageSource 接口支持国际化
// 实现类: ResourceBundleMessageSource, ReloadableResourceBundleMessageSource

@Configuration
public class I18nConfig {
    
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        
        // 设置资源文件基础名（注意：不包括路径和扩展名）
        source.setBasenames(
            "messages",           // classpath:messages.properties
            "i18n/validation",    // classpath:i18n/validation.properties
            "i18n/error"         // classpath:i18n/error.properties
        );
        
        // 设置默认编码
        source.setDefaultEncoding("UTF-8");
        
        // 设置缓存时间（秒），-1 表示永远缓存
        source.setCacheSeconds(3600);
        
        // 找不到消息时是否抛出异常，false 则返回默认消息
        source.setUseCodeAsDefaultMessage(true);
        
        return source;
    }
    
    @Bean  // 开发环境下不缓存，方便调试
    @Profile("dev")
    public MessageSource devMessageSource() {
        ReloadableResourceBundleMessageSource source = 
            new ReloadableResourceBundleMessageSource();
        source.setBasenames("classpath:messages", "classpath:i18n/validation");
        source.setDefaultEncoding("UTF-8");
        source.setCacheSeconds(1);  // 每秒刷新
        return source;
    }
}
```

### 11.2 资源文件

```properties
# ========== messages.properties (默认, 英语) ==========
greeting=Hello {0}, welcome to {1}!
order.created=Order #{0} has been created successfully
error.not.found=Resource not found: {0}

# ========== messages_zh_CN.properties (简体中文) ==========
greeting=你好 {0}，欢迎来到{1}！
order.created=订单 #{0} 已成功创建
error.not.found=资源未找到: {0}

# ========== messages_ja_JP.properties (日语) ==========
greeting=こんにちは {0}、{1} へようこそ！
order.created=注文 #{0} が正常に作成されました
error.not.found=リソースが見つかりません: {0}
```

### 11.3 MessageSource 使用

```java
// ========== 在 Controller 中使用 ==========
@RestController
@RequestMapping("/api")
public class I18nController {
    
    @Autowired
    private MessageSource messageSource;
    
    @GetMapping("/greeting")
    public String greeting(
            @RequestHeader(name = "Accept-Language", required = false) Locale locale) {
        // 从请求头获取语言设置
        if (locale == null) {
            locale = Locale.CHINA;  // 默认中文
        }
        
        return messageSource.getMessage("greeting", 
            new Object[]{"User", "Spring Boot"}, locale);
    }
}

// ========== 在 Service 中使用 ==========
@Service
public class I18nService {
    
    @Autowired
    private MessageSource messageSource;
    
    // 使用 LocaleContextHolder 获取当前线程的 Locale
    public String getLocalizedMessage(String code, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, args, locale);
    }
}

// ========== LocaleResolver 配置 ==========
// 控制如何确定用户的 Locale
@Configuration
public class LocaleConfig implements WebMvcConfigurer {
    
    @Bean
    public LocaleResolver localeResolver() {
        // 1. 基于 Cookie 的 LocaleResolver
        CookieLocaleResolver resolver = new CookieLocaleResolver();
        resolver.setCookieName("LANG");
        resolver.setDefaultLocale(Locale.CHINA);
        resolver.setCookieMaxAge(3600 * 24 * 30);  // 30 天
        return resolver;
        
        // 2. 基于 Session 的 LocaleResolver
        // SessionLocaleResolver resolver = new SessionLocaleResolver();
        // resolver.setDefaultLocale(Locale.US);
        // return resolver;
        
        // 3. 基于 Accept-Language 请求头的（默认）
        // AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        // return resolver;
    }
    
    @Bean  // 支持 URL 参数切换语言
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");  // ?lang=zh_CN
        return interceptor;
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}
```

---

## 12. 事件机制

### 12.1 Spring 事件架构

```
┌──────────────────────────────────────────────────────────────┐
│                    Spring 事件机制架构                         │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ApplicationEventPublisher (发布者)                           │
│       │                                                     │
│       │ publishEvent(event)                                  │
│       ▼                                                     │
│  ApplicationEventMulticaster (事件广播器)                     │
│       │                                                     │
│       ├──→ ApplicationListener (同步监听者)                  │
│       │     ● 默认：同步执行                                  │
│       │     ● 同一个线程，事务上下文中                         │
│       │                                                     │
│       └──→ @EventListener (注解式监听者)                     │
│             ● 支持异步 (@Async + @EventListener)             │
│             ● 支持条件筛选 (condition)                        │
│             ● 支持事务阶段 (@TransactionalEventListener)      │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

### 12.2 自定义事件

```java
// ========== 1. 定义事件类 ==========
// 继承 ApplicationEvent (Spring 4.2 之前)
// 或使用普通的 POJO (Spring 4.2+ 推荐)

// 传统方式（继承 ApplicationEvent）
public class OrderCreatedEvent extends ApplicationEvent {
    
    private final Order order;
    
    public OrderCreatedEvent(Object source, Order order) {
        super(source);  // source 通常为发布者
        this.order = order;
    }
    
    public Order getOrder() {
        return order;
    }
}

// 推荐方式（POJO，不需要继承）
public class PaymentReceivedEvent {
    
    private final String orderId;
    private final BigDecimal amount;
    private final LocalDateTime timestamp;
    
    public PaymentReceivedEvent(String orderId, BigDecimal amount) {
        this.orderId = orderId;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }
    
    // getters
}
```

### 12.3 发布事件

```java
// ========== 2. 发布事件 ==========
@Service
public class OrderService {
    
    @Autowired
    private ApplicationEventPublisher publisher;
    
    @Transactional
    public Order createOrder(OrderCreateRequest request) {
        // 1. 业务逻辑
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setAmount(request.getAmount());
        order.setStatus(OrderStatus.PENDING);
        
        order = orderRepository.save(order);
        
        // 2. 发布事件（在当前事务中）
        publisher.publishEvent(new OrderCreatedEvent(this, order));
        
        // 如果后续操作失败，事务回滚
        // 监听者中抛出的异常也会导致事务回滚（同步监听）
        
        return order;
    }
}
```

### 12.4 监听事件

```java
// ========== 3. 监听事件 ==========

// 方式 1：实现 ApplicationListener 接口
@Component
public class OrderEventListener implements ApplicationListener<OrderCreatedEvent> {
    
    @Override
    public void onApplicationEvent(OrderCreatedEvent event) {
        Order order = event.getOrder();
        // 处理订单创建后的逻辑
        System.out.println("订单 " + order.getId() + " 已创建");
    }
}

// 方式 2：@EventListener 注解 (推荐)
@Component
public class OrderEventHandlers {
    
    private static final Logger log = LoggerFactory.getLogger(OrderEventHandlers.class);
    
    @EventListener  // 监听 OrderCreatedEvent
    public void handleOrderCreated(OrderCreatedEvent event) {
        Order order = event.getOrder();
        log.info("订单创建事件: orderId={}", order.getId());
        // 发送通知、更新库存、记录日志等
    }
    
    // 监听普通 POJO 事件（Spring 4.2+）
    @EventListener
    public void handlePaymentReceived(PaymentReceivedEvent event) {
        log.info("收款事件: orderId={}, amount={}", 
            event.getOrderId(), event.getAmount());
    }
    
    // 监听多个事件类型
    @EventListener({OrderCreatedEvent.class, OrderShippedEvent.class})
    public void handleOrderStateChange(Object event) {
        log.info("订单状态变更: {}", event.getClass().getSimpleName());
    }
    
    // 条件筛选 — 只处理金额大于 1000 的事件
    @EventListener(condition = "#event.amount > 1000")
    public void handleLargePayment(PaymentReceivedEvent event) {
        log.info("大额收款: {}", event.getAmount());
    }
    
    // 返回新事件 — 监听器返回的事件会自动发布
    @EventListener
    public LogEvent handleOrderAndCreateLog(OrderCreatedEvent event) {
        // 自动将返回值发布为新事件
        return new LogEvent("order_created", event.getOrder().getId());
    }
}

// ========== 4. 异步监听 ==========
// 配合 @Async 使用，使监听器在独立的线程中执行
@Component
@EnableAsync  // 在配置类中启用
public class AsyncEventHandlers {
    
    @Async  // 异步执行
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        // 执行耗时操作（如发送邮件、推送通知）
        // 不会阻塞主事务
        sendEmail(event.getOrder());
    }
    
    private void sendEmail(Order order) {
        // 模拟发送邮件
        System.out.println("发送邮件: " + order.getId());
    }
}
```

### 12.5 @TransactionalEventListener

```java
// ========== @TransactionalEventListener ==========
// 在事务的特定阶段执行事件监听
// 确保监听器只在事务成功提交后执行

@Component
public class TransactionalEventHandlers {
    
    // 默认在事务提交后执行
    @TransactionalEventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        // 事务已提交，可以安全地发送消息、触发后续流程
        // 如果事务回滚，此监听器不会执行
        sendNotification(event.getOrder());
    }
    
    // 指定事务阶段
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)  // 提交后 (默认)
    public void afterCommit(OrderCreatedEvent event) { ... }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)  // 回滚后
    public void afterRollback(OrderCreatedEvent event) { ... }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION)  // 完成后（无论提交/回滚）
    public void afterCompletion(OrderCreatedEvent event) { ... }
    
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)  // 提交前（仍在事务中）
    public void beforeCommit(OrderCreatedEvent event) { ... }
    
    // 设置 fallbackExecution: 没有事务时是否执行
    @TransactionalEventListener(fallbackExecution = true)
    public void handleWithoutTransaction(OrderCreatedEvent event) {
        // 即使调用方没有事务，监听器也会执行
    }
}

// ========== 完整示例：订单创建事件流 ==========
// @Transactional
// OrderService.createOrder()
//   ├── 1. 保存订单到数据库
//   ├── 2. publisher.publishEvent(new OrderCreatedEvent(...))
//   │   └── 此时事件被广播，但 @TransactionalEventListener 暂不执行
//   └── 3. 事务提交成功
//       └── 4. @TransactionalEventListener 开始执行
//           ├── 发送邮件通知
//           ├── 更新缓存
//           └── 发送 MQ 消息
```

### 12.6 自定义 EventMulticaster

```java
// ========== 自定义事件广播器 ==========
// 默认 SimpleApplicationEventMulticaster 同步执行
// 可以替换为异步执行

@Configuration
public class AsyncEventConfig {
    
    @Bean
    public ApplicationEventMulticaster applicationEventMulticaster() {
        SimpleApplicationEventMulticaster multicaster = 
            new SimpleApplicationEventMulticaster();
        
        // 设置 TaskExecutor — 所有监听器异步执行
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.initialize();
        
        multicaster.setTaskExecutor(executor);
        
        // 设置 ErrorHandler — 监听器异常处理
        multicaster.setErrorHandler(throwable -> {
            // 监听器异常不会影响主流程
            System.err.println("Event listener error: " + throwable.getMessage());
        });
        
        return multicaster;
    }
}
```

---

## 13. @Async 与任务执行

### 13.1 启用异步

```java
// ========== 1. 启用 @Async ==========
@Configuration
@EnableAsync  // 开启异步执行支持
public class AsyncConfig {
    
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);       // 核心线程数
        executor.setMaxPoolSize(10);       // 最大线程数
        executor.setQueueCapacity(100);    // 队列容量
        executor.setThreadNamePrefix("async-");  // 线程名前缀
        executor.setWaitForTasksToCompleteOnShutdown(true);  // 优雅关闭
        executor.setAwaitTerminationSeconds(60);  // 等待 60 秒
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // CallerRunsPolicy: 当线程池满时，由调用者线程执行
        executor.initialize();
        return executor;
    }
    
    // @EnableAsync 可以指定默认线程池
    // @EnableAsync("taskExecutor")
}

// ========== 2. 可选：多个线程池，分别使用 ==========
@Configuration
@EnableAsync
public class MultiExecutorConfig {
    
    @Bean("emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("email-");
        executor.initialize();
        return executor;
    }
    
    @Bean("notificationTaskExecutor")
    public Executor notificationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("notif-");
        executor.initialize();
        return executor;
    }
}
```

### 13.2 使用 @Async

```java
// ========== 在 Service 中使用 @Async ==========
@Service
public class NotificationService {
    
    // 无返回值异步方法
    @Async
    public void sendEmail(String to, String subject, String content) {
        // 这个方法在单独的线程中执行
        System.out.println(Thread.currentThread().getName() + " 发送邮件");
        // 模拟耗时操作
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("邮件发送完成: " + to);
    }
    
    // 指定线程池
    @Async("emailTaskExecutor")
    public void sendSms(String phone, String message) {
        // 使用 "emailTaskExecutor" 线程池
    }
    
    // 有返回值的异步方法
    @Async
    public Future<String> processAsync(Long id) {
        // 模拟耗时计算
        String result = doHeavyComputation(id);
        return new AsyncResult<>(result);  // AsyncResult 实现 Future
    }
    
    // 使用 CompletableFuture（推荐）
    @Async
    public CompletableFuture<String> processWithCF(Long id) {
        String result = doHeavyComputation(id);
        return CompletableFuture.completedFuture(result);
    }
}

// ========== 调用异步方法 ==========
@Service
public class OrderService {
    
    @Autowired
    private NotificationService notificationService;
    
    public void createOrder(Order order) {
        // 同步业务逻辑
        saveOrder(order);
        
        // 异步通知 — 不阻塞主流程
        notificationService.sendEmail(order.getUserEmail(), 
            "订单创建成功", "您的订单 " + order.getId() + " 已创建");
        
        // 获取异步结果
        CompletableFuture<String> future = notificationService.processWithCF(order.getId());
        future.thenAccept(result -> {
            // 异步完成后的回调
            System.out.println("处理结果: " + result);
        });
    }
}
```

### 13.3 @Async 的限制与注意事项

```java
// ========== 重要限制 ==========
// 1. @Async 不能在同一类中调用（代理机制限制）
@Service
public class MyService {
    
    @Async
    public void asyncMethod() {
        // 这个方法可以异步执行
    }
    
    public void caller() {
        this.asyncMethod();  // ❌ 不会异步！因为这是内部调用，不经过代理
        // 解决方案：注入自身代理，或拆分到不同类
    }
    
    @Autowired
    private MyService self;  // 注入自身代理
    
    public void callerFixed() {
        self.asyncMethod();  // ✅ 会异步执行（经过代理）
    }
}

// 2. @Async 方法必须是 public
// 3. @Async 方法返回值只能是 void, Future, CompletableFuture, ListenableFuture
// 4. @Async 方法不能与 @Transactional 在同一方法上共存（事务需要同一线程）

// ========== 异常处理 ==========
@Configuration
public class AsyncExceptionConfig implements AsyncConfigurer {
    
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.initialize();
        return executor;
    }
    
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            // 处理 @Async void 方法中的未捕获异常
            System.err.println("Async method " + method.getName() + 
                " threw: " + ex.getMessage());
        };
    }
}
```

---

## 14. 常见陷阱与最佳实践

### 14.1 陷阱 1：循环依赖

```java
// ========== 循环依赖 ==========
// 问题：构造器注入导致循环依赖
// 症状：BeanCreationException: Requested beans are currently in creation

// 解决方案：
// 1. 重新设计，消除循环依赖（最佳）
// 2. @Lazy 延迟注入（临时方案）
// 3. @Autowired 字段注入（不推荐）
@Service
public class AService {
    private final BService bService;
    
    public AService(@Lazy BService bService) {  // @Lazy 打破循环
        this.bService = bService;
    }
}
```

### 14.2 陷阱 2：自调用导致 AOP 失效

```java
// ========== 自调用 AOP 失效 ==========
@Service
public class TransactionalService {
    
    @Transactional
    public void methodA() {
        // 事务生效
        // ...
    }
    
    public void methodB() {
        // 内部调用 methodA
        this.methodA();  // ❌ 事务不生效！自调用不经过代理
    }
    
    @Autowired
    private TransactionalService self;  // 注入自身代理
    
    public void methodC() {
        self.methodA();  // ✅ 事务生效（经过代理）
    }
}

// 同样适用于：@Async, @Cacheable, @Validated 等所有 AOP 注解
```

### 14.3 陷阱 3：prototype-in-singleton 问题

```java
// ========== prototype-in-singleton ==========
// 问题：将 prototype bean 注入到 singleton bean 后，scope 失效
// 每次拿到的都是同一个实例

// 解决方案 1：@Lookup 方法注入
@Component
public abstract class SingletonBean {
    
    @Lookup
    protected abstract PrototypeBean getPrototypeBean();
    
    public void usePrototype() {
        PrototypeBean bean = getPrototypeBean();  // 每次都是新实例
        bean.doSomething();
    }
}

// 解决方案 2：ApplicationContextAware
@Component
public class SingletonBean implements ApplicationContextAware {
    private ApplicationContext context;
    
    @Override
    public void setApplicationContext(ApplicationContext context) {
        this.context = context;
    }
    
    public void usePrototype() {
        PrototypeBean bean = context.getBean(PrototypeBean.class);
        bean.doSomething();
    }
}

// 解决方案 3：ObjectFactory / Provider
@Component
public class SingletonBean {
    
    @Autowired
    private ObjectFactory<PrototypeBean> factory;
    
    public void usePrototype() {
        PrototypeBean bean = factory.getObject();
        bean.doSomething();
    }
}
```

### 14.4 陷阱 4：代理对象类型转换异常

```java
// ========== 代理对象 ClassCastException ==========
@Service
public class UserService {
    // ...
}

// 问题：使用 CGLIB 代理时，不能转换为具体类外的类型
@Service
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class SessionScopedBean {
    // ...
}

// 注入时可能 ClassCastException
@Autowired
private SessionScopedBean bean;  // ❌ 实际注入的是代理对象

// 解决方案：依赖接口编程
public interface SessionService {
    void process();
}

@Service
@Scope(proxyMode = ScopedProxyMode.INTERFACES)
public class SessionServiceImpl implements SessionService {
    // ...
}

@Autowired
private SessionService sessionService;  // ✅ 面向接口
```

### 14.5 最佳实践清单

```java
// ========== 1. 使用构造器注入，避免字段注入 ==========
@RequiredArgsConstructor  // Lombok
@Service
public class GoodPractice {
    private final DependencyA depA;
    private final DependencyB depB;
    // 不可变，可测试，依赖关系明确
}

// ========== 2. 面向接口编程 ==========
// 不直接依赖具体类
public interface PaymentService { void pay(Order order); }

@Service
public class PaymentServiceImpl implements PaymentService {
    @Override
    public void pay(Order order) { ... }
}

// ========== 3. Bean 默认无状态 ==========
// 不在 singleton bean 中保存 mutable 状态
@Service
public class StatelessService {
    // ✅ 方法参数是线程安全的
    public Result process(Request request) {
        // 局部变量是线程安全的
        String transactionId = UUID.randomUUID().toString();
        // ...
    }
    
    // ❌ 实例变量存在线程安全问题
    private int counter;  // 不要这样！
}

// ========== 4. 合理使用 @Scope ==========
// 90%+ 的 Bean 应该是 singleton
// prototype 用于有状态组件
// web scope 谨慎使用，注意代理模式

// ========== 5. 充分使用 @ConfigurationProperties ==========
// 代替散落的 @Value
@ConfigurationProperties(prefix = "app.mail")
@Component
public class MailProperties {
    private String host;
    private int port;
    private String username;
    // getters, setters
}

// ========== 6. 善用事件解耦 ==========
// 同步操作 + 事件驱动异步处理
@Service
public class BestPracticeOrderService {
    
    @Transactional
    public Order createOrder(OrderRequest req) {
        Order order = saveOrder(req);
        publisher.publishEvent(new OrderCreatedEvent(this, order));
        // 事务提交后，监听器执行
        return order;
    }
}
```

---

## 15. 面试题精选

### 15.1 基础题

**Q1: 什么是 IoC？什么是 DI？两者有什么关系？**

A: IoC (Inversion of Control) 是一种设计原则，将对象的创建和依赖管理转移给容器。DI (Dependency Injection) 是 IoC 的一种实现方式，容器在创建对象时将依赖注入进去。关系：IoC 是目标（控制反转），DI 是实现手段（依赖注入）。

**Q2: @Autowired 和 @Resource 的区别？**

| 特性 | @Autowired | @Resource |
|------|-----------|-----------|
| 来源 | Spring 特有 | JSR-250 (Java 标准) |
| 匹配策略 | 先按类型，再按名称 | 先按名称，再按类型 |
| 属性 | required 属性 | name 属性 |
| 推荐 | 同一项目统一使用 | 规范兼容性 |

**Q3: Spring 如何解决循环依赖？**

A: 通过三级缓存：
- Level 1: singletonObjects (完全初始化)
- Level 2: earlySingletonObjects (早期引用)
- Level 3: singletonFactories (ObjectFactory 工厂)

只能解决 singleton + 字段/setter 注入的循环依赖。构造器注入无法解决，prototype 无法解决。

### 15.2 进阶题

**Q4: Bean 的生命周期是怎样的？**

A: 实例化 → 属性填充 → Aware 接口回调 → BeanPostProcessor before → @PostConstruct → InitializingBean → init-method → BeanPostProcessor after → 就绪 → @PreDestroy → DisposableBean → destroy-method。

**Q5: @Configuration 的 proxyBeanMethods = true/false 的区别？**

A: true (Full 模式) — CGLIB 代理，@Bean 间方法调用会从容器获取，保证 singleton；false (Lite 模式) — 无代理，性能好，但 @Bean 间调用会生成新实例。

**Q6: 如何确保 prototype bean 在 singleton 中每次获取都是新实例？**

A: 三种方式：@Lookup 方法注入、ObjectFactory/Provider、ApplicationContext.getBean()。

### 15.3 高级题

**Q7: BeanFactoryPostProcessor 和 BeanPostProcessor 的区别？**

| | BeanFactoryPostProcessor | BeanPostProcessor |
|--|-------------------------|-------------------|
| 执行时机 | Bean 实例化之前 | Bean 初始化前后 |
| 操作对象 | BeanDefinition（元数据） | Bean 实例 |
| 典型用途 | 修改属性占位符，注册自定义 scope | AOP 代理，包装 Bean |

**Q8: Spring 事件机制的局限性？如何与消息队列区分使用？**

A: Spring 事件是进程内的同步/异步通知，适用于单个 JVM 内的解耦。消息队列 (Kafka/RabbitMQ) 用于跨服务、跨进程的异步通信。选择：单体应用内部用事件，微服务间用 MQ。

**Q9: @TransactionalEventListener 和 @EventListener 的区别？**

A: @TransactionalEventListener 在事务的特定阶段执行，默认在事务提交后才执行。@EventListener 在事件发布时立即执行。前者适合需要事务保障的场景。

### 15.4 设计题

**Q10: 你如何设计一个可扩展的支付模块，支持多种支付方式？**

A:
1. 定义 PaymentService 接口
2. 每种支付方式作为独立实现类 (AliPayService, WeChatPayService)
3. 通过 @Qualifier 或自定义注解区分
4. 使用策略模式 + 工厂模式动态选择支付方式
5. 支付事件驱动后续流程（通知、对账）
6. @ConfigurationProperties 管理每种支付方式的配置
7. @ConditionalOnProperty 控制启用/禁用

---

> **记住：Spring Core 是整个 Spring 生态系统的基石。如果你没有深入理解 IoC 容器和 DI 的原理，后面的 MVC、Boot、Cloud 都只是空中楼阁。花足够的时间在这章上，动手写代码验证每一个概念。**
