# 01-Spring IoC容器深度剖析
> 🎯 Spring全家桶根基中的根基 — 掌握IoC容器原理、Bean生命周期、依赖注入机制、循环依赖三级缓存，是理解整个Spring生态的起点

---

## 目录
1. [本章总览](#1-本章总览)
2. [IoC核心概念与Bean定义](#2-ioc核心概念与bean定义)
3. [依赖注入三种方式](#3-依赖注入三种方式)
4. [Bean作用域详解](#4-bean作用域详解)
5. [Bean生命周期完整流程](#5-bean生命周期完整流程)
6. [循环依赖与三级缓存](#6-循环依赖与三级缓存)
7. [配置方式演进：XML→注解→JavaConfig](#7-配置方式演进xml注解javaconfig)
8. [高频踩坑与误区](#8-高频踩坑与误区)
9. [随堂基础练习](#9-随堂基础练习)
10. [章节综合实操案例](#10-章节综合实操案例)
11. [分层综合习题](#11-分层综合习题)
12. [本章复盘速记清单](#12-本章复盘速记清单)
13. [精通拓展补充-P2](#13-精通拓展补充-p2)

---

## 1. 本章总览

### 1.1 知识定位
- **归属**：Spring Framework 核心 → 层级1 P0核心必学
- **重要性**：⭐⭐⭐⭐⭐（Spring全家桶一切组件的根基）

### 1.2 前置依赖
- Java反射机制（`Class.forName`、`Constructor.newInstance`）
- Java注解（`@interface`、元注解）
- XML基础（了解即可，现代开发已转向注解）
- Maven依赖管理

### 1.3 三层学习目标

| 级别 | 目标 | 检验标准 |
|------|------|----------|
| **基础** | 会使用`@Component`/`@Autowired`等注解完成Bean注入 | 完成简单的SpringBoot CRUD项目 |
| **熟练** | 理解Bean生命周期各阶段、能处理注入冲突（`@Qualifier`/`@Primary`） | 解决多实现类注入、Bean找不到等常见报错 |
| **精通** | 吃透三级缓存解决循环依赖的源码逻辑、理解`BeanFactory`与`ApplicationContext`区别 | 面试手画循环依赖流程图、分析启动慢原因 |

### 1.4 本章思维导图
```
IoC容器
├── 核心概念：控制反转、依赖注入、容器
├── Bean定义：@Component/@Service/@Repository/@Controller
├── 依赖注入：构造器注入（推荐）、Setter注入、字段注入（@Autowired）
├── Bean作用域：singleton/prototype/request/session
├── Bean生命周期：实例化→属性填充→初始化→使用→销毁
├── 循环依赖：构造器循环（无法解决）vs Setter循环（三级缓存解决）
├── 配置方式：XML → 注解 → JavaConfig（@Configuration + @Bean）
└── 高级：FactoryBean、BeanFactoryPostProcessor、条件注解@Conditional
```

---

## 2. IoC核心概念与Bean定义

### 2.1 什么是IoC（控制反转）

> 💡 **一句话**：把对象的创建、管理权从程序员手中"反转"给Spring容器。

**传统方式 vs IoC方式**：

```java
// 传统方式：程序员自己new对象，控制权在自己手中
public class OrderService {
    private UserService userService = new UserServiceImpl(); // 硬编码依赖
}

// IoC方式：Spring容器负责创建和注入，控制权反转给容器
@Service
public class OrderService {
    @Autowired
    private UserService userService; // 容器自动注入
}
```

### 2.2 IoC容器核心接口

| 接口 | 说明 | 区别 |
|------|------|------|
| `BeanFactory` | IoC容器最底层接口，提供最基本的Bean管理功能 | 延迟加载（用到才创建） |
| `ApplicationContext` | `BeanFactory`的子接口，企业级IoC容器 | 启动时预初始化所有单例Bean |
| `WebApplicationContext` | Web环境专用，包含ServletContext引用 | 用于Spring MVC |

```java
// BeanFactory - 延迟加载
BeanFactory factory = new DefaultListableBeanFactory();
// Bean只有在getBean()时才创建

// ApplicationContext - 启动时加载（推荐）
ApplicationContext ctx = new AnnotationConfigApplicationContext(AppConfig.class);
// 启动时所有单例Bean已完成初始化
```

### 2.3 Bean定义注解

| 注解 | 说明 | 使用场景 |
|------|------|----------|
| `@Component` | 通用组件注解 | 工具类、通用Bean |
| `@Service` | 标识业务逻辑层 | Service层 |
| `@Repository` | 标识数据访问层 | DAO层（自动翻译JDBC异常） |
| `@Controller` | 标识控制器层 | MVC中的Controller |
| `@Configuration` | 标识配置类 | 配合`@Bean`声明第三方Bean |
| `@Bean` | 方法级别，方法返回值作为Bean | 声明第三方库对象 |

```java
@Configuration
public class AppConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate(); // 将第三方类纳入Spring管理
    }
}

@Service
public class UserService {
    public String getUser() { return "user"; }
}
```

---

## 3. 依赖注入三种方式

### 3.1 三种注入方式对比

| 注入方式 | 实现 | 优点 | 缺点 | 推荐度 |
|----------|------|------|------|--------|
| **构造器注入** | `@Autowired` 在构造器上 | 不可变性、强制依赖、便于测试 | 参数多时代码长 | ⭐⭐⭐⭐⭐ |
| **Setter注入** | `@Autowired` 在setter上 | 可选依赖、可重新配置 | 对象可能处于不完整状态 | ⭐⭐⭐ |
| **字段注入** | `@Autowired` 在字段上 | 代码简洁 | 无法final、难测试、隐藏依赖 | ⭐⭐ |

```java
// ✅ 推荐：构造器注入（Spring 4.3+ 单构造器可省略@Autowired）
@Service
public class OrderService {
    private final UserService userService;
    private final PaymentService paymentService;

    public OrderService(UserService userService, PaymentService paymentService) {
        this.userService = userService;
        this.paymentService = paymentService;
    }
}

// ⚠️ 字段注入（不推荐，但大量遗留代码使用）
@Service
public class OrderService {
    @Autowired
    private UserService userService;     // 无法加final，反射注入
    @Autowired
    private PaymentService paymentService;
}
```

### 3.2 处理注入冲突

```java
// 场景：同一个接口多个实现类
public interface PaymentService { void pay(); }

@Service("alipay")
public class AlipayService implements PaymentService { ... }

@Service("wechatPay")
public class WechatPayService implements PaymentService { ... }

// 方案1：@Qualifier指定Bean名称
@Autowired
@Qualifier("alipay")
private PaymentService paymentService;

// 方案2：@Primary优先注入
@Service
@Primary
public class AlipayService implements PaymentService { ... }

// 方案3：@Resource（JSR-250，按名称注入）
@Resource(name = "alipay")
private PaymentService paymentService;
```

---

## 4. Bean作用域详解

| 作用域 | 说明 | 生命周期 | 使用场景 |
|--------|------|----------|----------|
| **singleton** | 默认，IoC容器中只有一个实例 | 随容器创建而创建，容器销毁而销毁 | 无状态Service、DAO |
| **prototype** | 每次获取都创建新实例 | 容器创建后不再管理销毁 | 有状态的Bean、每次请求不同的对象 |
| **request** | 每个HTTP请求一个实例 | 请求结束销毁 | Web环境中请求级数据 |
| **session** | 每个HTTP会话一个实例 | 会话结束销毁 | 用户登录信息 |
| **application** | ServletContext级别单例 | 随Web应用生命周期 | Web应用全局数据 |

> ⚠️ **关键陷阱**：singleton Bean中注入prototype Bean时，prototype只会被注入一次！因为singleton只初始化一次。

```java
// 解决方案：使用@Lookup或ApplicationContext获取
@Component
public abstract class SingletonBean {
    @Lookup
    public abstract PrototypeBean getPrototypeBean(); // Spring通过CGLIB重写此方法
}
```

---

## 5. Bean生命周期完整流程

> 🎯 这是Spring面试最高频考点之一，必须完整掌握每一步。

### 5.1 生命周期流程图
```
1. 实例化（Instantiation）
    ↓ 通过构造器/工厂方法创建对象
2. 属性填充（Populate Properties）
    ↓ 注入@Autowired标记的属性
3. BeanNameAware.setBeanName()
    ↓
4. BeanFactoryAware.setBeanFactory()
    ↓
5. ApplicationContextAware.setApplicationContext()
    ↓
6. BeanPostProcessor.postProcessBeforeInitialization()
    ↓ 可在此对Bean进行代理替换
7. @PostConstruct 标注的方法
    ↓
8. InitializingBean.afterPropertiesSet()
    ↓
9. 自定义init-method
    ↓
10. BeanPostProcessor.postProcessAfterInitialization()
    ↓ 生成代理对象的位置（AOP在此完成）
11. Bean就绪，可使用
    ↓
12. @PreDestroy 标注的方法
    ↓
13. DisposableBean.destroy()
    ↓
14. 自定义destroy-method
```

### 5.2 关键生命周期代码演示

```java
@Component
public class UserService implements BeanNameAware, InitializingBean, DisposableBean {

    @Autowired
    private OrderService orderService; // 步骤2：属性填充

    public UserService() {
        System.out.println("1. 构造器实例化");
    }

    @Override
    public void setBeanName(String name) {
        System.out.println("3. BeanNameAware: " + name);
    }

    @PostConstruct
    public void init() {
        System.out.println("7. @PostConstruct 初始化");
    }

    @Override
    public void afterPropertiesSet() {
        System.out.println("8. InitializingBean.afterPropertiesSet");
    }

    @PreDestroy
    public void preDestroy() {
        System.out.println("12. @PreDestroy 销毁");
    }

    @Override
    public void destroy() {
        System.out.println("13. DisposableBean.destroy");
    }
}
```

---

## 6. 循环依赖与三级缓存

> 🔥 面试最常问：Spring如何解决循环依赖？三级缓存分别存什么？

### 6.1 什么是循环依赖

```java
@Service
public class A {
    @Autowired
    private B b;     // A依赖B
}

@Service
public class B {
    @Autowired
    private A a;     // B依赖A → 形成循环依赖
}
```

### 6.2 三级缓存机制

| 缓存 | 名称 | 存储内容 | 作用 |
|------|------|----------|------|
| **一级缓存** | `singletonObjects` | 完全初始化好的Bean（成品） | 存放最终可用的Bean |
| **二级缓存** | `earlySingletonObjects` | 提前暴露的Bean（半成品，属性未填充完） | 解决循环依赖 |
| **三级缓存** | `singletonFactories` | Bean的ObjectFactory（lambda表达式） | 生成Bean的早期引用，可进行AOP代理 |

### 6.3 解决方案流程图

```
A创建 → 实例化A → 将A的ObjectFactory放入三级缓存
    → 填充A的属性b → 发现依赖B → 去获取B
        → B创建 → 实例化B → 将B的ObjectFactory放入三级缓存
            → 填充B的属性a → 发现依赖A → 从三级缓存获取A的ObjectFactory
                → 调用getObject()拿到A的早期引用（如有AOP返回代理对象）
                → 将A升级到二级缓存 → B拿到A的引用 → B完成初始化
        → B放入一级缓存，删除二三级缓存
    → A拿到完整的B → A完成初始化
→ A放入一级缓存，删除二三级缓存
```

> ⚠️ **无法解决的场景**：
> 1. **构造器循环依赖**：A的构造器需要B，B的构造器需要A → 抛出`BeanCurrentlyInCreationException`
> 2. **prototype作用域的循环依赖**：prototype Bean不经过缓存 → 无法解决
>
> **解决方案**：改用setter注入，或重构代码消除循环依赖（推荐）

### 6.4 源码关键流程

```java
// AbstractAutowireCapableBeanFactory.doCreateBean() 关键代码片段
// 1. 实例化后，放入三级缓存
addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));

// 2. 属性填充
populateBean(beanName, mbd, instanceWrapper);

// 3. 初始化
exposedObject = initializeBean(beanName, exposedObject, mbd);

// DefaultSingletonBeanRegistry.getSingleton() 获取Bean的流程
// 一级缓存找不到 → 二级缓存找 → 三级缓存找（并升级到二级）
protected Object getSingleton(String beanName, boolean allowEarlyReference) {
    Object singletonObject = this.singletonObjects.get(beanName);    // 一级
    if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
        singletonObject = this.earlySingletonObjects.get(beanName);   // 二级
        if (singletonObject == null && allowEarlyReference) {
            ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName); // 三级
            if (singletonFactory != null) {
                singletonObject = singletonFactory.getObject();
                this.earlySingletonObjects.put(beanName, singletonObject); // 升级到二级
                this.singletonFactories.remove(beanName);
            }
        }
    }
    return singletonObject;
}
```

---

## 7. 配置方式演进：XML→注解→JavaConfig

### 7.1 三种方式对比

| 配置方式 | 时代 | 特点 | 推荐度 |
|----------|------|------|--------|
| XML配置 | Spring 1.x-3.x | 集中管理、修改无需重编译 | 遗留项目维护 |
| 注解配置 | Spring 2.5+ | `@Component`/`@Autowired`，分散配置 | 与JavaConfig配合 |
| JavaConfig | Spring 3.0+ | `@Configuration`+`@Bean`，类型安全 | ⭐⭐⭐⭐⭐ 现代标准 |

### 7.2 JavaConfig示例

```java
@Configuration
@ComponentScan("com.example")          // 扫描注解Bean
@PropertySource("classpath:app.properties") // 加载配置文件
@EnableTransactionManagement          // 开启事务
public class AppConfig {

    @Bean
    @ConditionalOnMissingBean          // 条件注解
    public DataSource dataSource(
            @Value("${db.url}") String url,
            @Value("${db.username}") String username,
            @Value("${db.password}") String password) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        return ds;
    }
}

// SpringBoot主类本身就是@Configuration
@SpringBootApplication  // = @Configuration + @EnableAutoConfiguration + @ComponentScan
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

---

## 8. 高频踩坑与误区

### ⚠️ 坑1：`@Autowired`注入为null

```java
// ❌ 错误：在构造器中直接使用@Autowired的字段，此时还未注入
@Service
public class UserService {
    @Autowired
    private OrderService orderService;
    
    public UserService() {
        orderService.create(); // NPE！orderService此时还是null
    }
}

// ✅ 正确：在@PostConstruct或构造器注入中使用
@Service
public class UserService {
    private final OrderService orderService;
    
    public UserService(OrderService orderService) {
        this.orderService = orderService;
        this.orderService.create(); // 正确，参数传入
    }
}
```

### ⚠️ 坑2：多个同类型Bean报错

```
Field paymentService in OrderService required a single bean, but 2 were found
```

> 解决方案：`@Qualifier`、`@Primary`、或使用`Map<String, BeanType>`收集所有实现

### ⚠️ 坑3：`@Configuration`中的`@Bean`方法被多次调用

```java
// ❌ 错误认识：认为每次调用beanB()都创建新实例
@Configuration
public class AppConfig {
    @Bean
    public A beanA() {
        return new A(beanB()); // 实际上beanB()返回的是容器中的单例
    }
    @Bean
    public B beanB() {
        return new B();
    }
}
// 原因：@Configuration类被CGLIB代理，@Bean方法会被拦截，保证单例
// 如果去掉@Configuration只用@Component，则每次调用都会创建新实例！
```

---

## 9. 随堂基础练习

### 练习1：Bean注册
题目：分别使用`@Component`和`@Bean`两种方式，将`DataSource`对象注册到Spring容器中。

### 练习2：依赖注入
题目：有一个`OrderService`依赖`UserService`和`ProductService`，请使用构造器注入方式完成。

### 练习3：多实现类
题目：`PaymentService`接口有两个实现`Alipay`和`WechatPay`，如何使用`@Qualifier`指定注入哪个？

### 练习4：Bean作用域
题目：验证singleton和prototype的区别 — 连续两次从容器获取Bean，打印hashCode。

---

## 10. 章节综合实操案例

> 💻 场景：搭建一个简易的依赖注入示例

```java
// === 实体类 ===
@Data
@AllArgsConstructor
public class User {
    private Long id;
    private String name;
}

// === DAO层 ===
@Repository
public class UserRepository {
    private final Map<Long, User> db = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        db.put(1L, new User(1L, "张三"));
        db.put(2L, new User(2L, "李四"));
    }
    
    public User findById(Long id) {
        return db.get(id);
    }
}

// === Service层 ===
@Service
public class UserService {
    private final UserRepository userRepository;

    // 构造器注入（推荐方式）
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUser(Long id) {
        User user = userRepository.findById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在: " + id);
        }
        return user;
    }
}

// === 配置类 ===
@Configuration
@ComponentScan("com.example")
public class AppConfig {
}

// === 测试 ===
public class Main {
    public static void main(String[] args) {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(AppConfig.class);
        UserService userService = ctx.getBean(UserService.class);
        System.out.println(userService.getUser(1L)); // User(id=1, name=张三)
    }
}
```

---

## 11. 分层综合习题

### 基础题
1. 列举5个Spring的Bean注册注解及其使用场景
2. `@Autowired`和`@Resource`有什么区别？
3. singleton和prototype作用域的区别是什么？

### 进阶应用题
4. 当一个接口有3个实现类时，有哪几种方式指定注入哪一个？
5. `BeanFactory`和`ApplicationContext`的区别？为什么SpringBoot使用后者？
6. 简述Bean的完整生命周期（列出所有回调方法）

### 精通拔高题
7. Spring三级缓存分别存储什么？画图说明循环依赖的解决流程
8. 为什么构造器注入可以解决循环依赖问题（实际上Spring不支持），但setter注入不行？分析底层原因
9. `@Configuration`注解的proxyBeanMethods属性有何作用？何时设为false？

---

## 12. 本章复盘速记清单

| 类别 | 要点 |
|------|------|
| **核心注解** | `@Component` `@Service` `@Repository` `@Controller` `@Configuration` `@Bean` `@Autowired` `@Qualifier` `@Primary` `@Resource` `@Scope` `@PostConstruct` `@PreDestroy` |
| **核心概念** | IoC控制反转、DI依赖注入、ApplicationContext > BeanFactory |
| **作用域** | singleton(默认)、prototype、request、session |
| **注入方式** | 构造器注入（推荐）> Setter注入 > 字段注入 |
| **三级缓存** | 一级`singletonObjects`(成品)、二级`earlySingletonObjects`(半成品)、三级`singletonFactories`(工厂) |
| **循环依赖** | 构造器循环→无解；Setter循环→三级缓存解决；prototype循环→无解 |
| **生命周期** | 实例化→属性填充→Aware→BeanPostProcessor#before→@PostConstruct→afterPropertiesSet→initMethod→BeanPostProcessor#after→就绪→@PreDestroy→destroy |

---

## 13. 精通拓展补充-P2

### 13.1 FactoryBean与BeanFactory

| 接口 | 作用 | 获取方式 |
|------|------|----------|
| `BeanFactory` | IoC容器，管理Bean | `ctx.getBean(XxxBean.class)` |
| `FactoryBean` | 工厂Bean，用于创建复杂Bean（如MyBatis的Mapper代理） | `ctx.getBean("&beanName")` 获取FactoryBean本身 |

```java
@Component
public class MyFactoryBean implements FactoryBean<UserService> {
    @Override
    public UserService getObject() {
        return new UserService(); // 复杂构建逻辑
    }
    @Override
    public Class<?> getObjectType() {
        return UserService.class;
    }
    @Override
    public boolean isSingleton() {
        return true;
    }
}
```

### 13.2 BeanFactoryPostProcessor vs BeanPostProcessor

| 接口 | 执行时机 | 作用 |
|------|----------|------|
| `BeanFactoryPostProcessor` | 所有Bean定义加载后、实例化前 | 修改Bean定义元数据（如`@Value`占位符替换） |
| `BeanPostProcessor` | Bean初始化前后 | 对Bean实例进行代理替换（AOP的核心） |

### 13.3 @Conditional条件注解体系

```java
@Bean
@ConditionalOnClass(name = "com.mysql.cj.jdbc.Driver") // 类存在时才注册
public DataSource mysqlDataSource() { ... }

@Bean
@ConditionalOnMissingBean(DataSource.class) // 没有DataSource Bean时才注册
public DataSource h2DataSource() { ... }

@Bean
@ConditionalOnProperty(name = "cache.enabled", havingValue = "true") // 配置存在且为true
public CacheManager cacheManager() { ... }
```

### 13.4 Spring容器的扩展点总览

```
BeanFactoryPostProcessor       → 修改BeanDefinition（@Configuration解析）
    ↓
InstantiationAwareBeanPostProcessor → 实例化前后拦截
    ↓
BeanPostProcessor#before       → 初始化前（@PostConstruct处理在此）
    ↓
BeanPostProcessor#after        → 初始化后 → AOP代理生成在此
    ↓
SmartInitializingSingleton     → 所有单例Bean初始化完成后回调
```
