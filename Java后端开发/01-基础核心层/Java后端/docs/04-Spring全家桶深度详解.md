# 🍃 Spring 全家桶深度详解

> 从 IoC 容器到底层源码，从 MVC 到 Security，全面掌握企业级 Spring 开发。

---

## 目录

1. [Spring Framework 核心架构](#1-spring-framework-核心架构)
2. [IoC 容器深度剖析](#2-ioc-容器深度剖析)
3. [Bean 生命周期与作用域](#3-bean-生命周期与作用域)
4. [依赖注入最佳实践](#4-依赖注入最佳实践)
5. [AOP 面向切面编程](#5-aop-面向切面编程)
6. [事务管理完全指南](#6-事务管理完全指南)
7. [Spring MVC 请求处理全流程](#7-spring-mvc-请求处理全流程)
8. [Spring Boot 自动配置原理](#8-spring-boot-自动配置原理)
9. [Spring Boot 核心能力](#9-spring-boot-核心能力)
10. [Spring Security 认证授权](#10-spring-security-认证授权)
11. [Spring Event 事件机制](#11-spring-event-事件机制)
12. [Spring 扩展点与高级特性](#12-spring-扩展点与高级特性)
13. [Spring Boot 3.x 新特性](#13-spring-boot-3x-新特性)
14. [常见面试题深度解析](#14-常见面试题深度解析)

---

## 1. Spring Framework 核心架构

### 1.1 整体架构

```
Spring Framework 模块全景图：

┌───────────────────────────────────────────────────────────────┐
│                     Spring Framework 5/6                       │
├─────────────┬─────────────┬──────────────┬───────────────────┤
│   Data      │   Web       │    Core      │      AOP          │
│ ─────────── │ ─────────── │ ─────────────│ ─────────────────│
│ JDBC        │ Web MVC     │ IoC Container│ Aspects           │
│ ORM         │ WebFlux     │ Beans        │ Instrumentation   │
│ TX (事务)    │ WebSocket   │ Context      │ Messaging         │
│ OXM         │ Web Servlet │ SpEL (表达式)│                    │
│ JMS         │ Web Reactive│              │                   │
├─────────────┴─────────────┴──────────────┴───────────────────┤
│                         Test                                   │
│    JUnit / TestNG / Mockito / Spring TestContext               │
└───────────────────────────────────────────────────────────────┘

核心理念：
  IoC (Inversion of Control)  — 控制反转，容器管理对象
  DI (Dependency Injection)   — 依赖注入，容器装配对象
  AOP (Aspect-Oriented Programming) — 面向切面，横切关注点
```

---

## 2. IoC 容器深度剖析

### 2.1 什么是 IoC？

```
传统方式（你控制）：
  UserService service = new UserServiceImpl();
  service.setUserMapper(new UserMapperImpl());
  // 你自己 new，自己装配，自己管理生命周期

IoC 方式（容器控制）：
  @Autowired
  private UserService service;
  // 容器帮你创建、帮你装配、帮你管理生命周期
  // 你只需要声明你的需求

"控制反转"的含义：
  不是"我不控制了"
  而是"控制权从我的代码转交给了 Spring 容器"
```

### 2.2 BeanFactory vs ApplicationContext

```
BeanFactory（底层容器）：
  - 最基础的 IoC 容器
  - 懒加载（getBean() 时才创建）
  - 功能简单：Bean 管理 + DI
  - 内存占用小

ApplicationContext（高级容器，继承 BeanFactory）：
  - 企业级 IoC 容器
  - 预加载（启动时创建所有单例 Bean）→ 启动时发现问题
  - 额外功能：
    ✅ 国际化（MessageSource）
    ✅ 事件发布（ApplicationEventPublisher）
    ✅ 资源加载（ResourceLoader）
    ✅ 环境抽象（Environment）
    ✅ 注解支持
    ✅ AOP 集成
  - Spring Boot 默认使用 AnnotationConfigApplicationContext

继承关系：
  BeanFactory ← HierarchicalBeanFactory ← ... ← ApplicationContext
      ↑                                         ↑
  底层，基础                              高级，全功能
```

### 2.3 容器启动流程

```
Spring Boot 应用启动流程：

SpringApplication.run()
  ├── 1. 创建 SpringApplication 实例
  │     - 推断应用类型（SERVLET/REACTIVE/NONE）
  │     - 加载所有 ApplicationContextInitializer
  │     - 加载所有 ApplicationListener
  │
  ├── 2. 准备环境（Environment）
  │     - 加载系统环境变量、属性
  │     - 加载 application.yml / application.properties
  │     - 激活 Profile
  │
  ├── 3. 创建 ApplicationContext
  │     - Servlet 应用 → AnnotationConfigServletWebServerApplicationContext
  │
  ├── 4. 准备 ApplicationContext
  │     - 设置 Environment
  │     - 执行 ApplicationContextInitializer
  │     - 注册主配置类（启动类）
  │
  ├── 5. 刷新 ApplicationContext（核心！）
  │     AbstractApplicationContext.refresh()
  │     ├── prepareRefresh()              准备刷新（验证环境变量等）
  │     ├── obtainFreshBeanFactory()       获取 BeanFactory
  │     ├── prepareBeanFactory()           配置 BeanFactory（类加载器、后处理器等）
  │     ├── postProcessBeanFactory()       后处理 BeanFactory
  │     ├── invokeBeanFactoryPostProcessors()  ← 执行 BeanFactory 后处理器
  │     │     核心：ConfigurationClassPostProcessor
  │     │     - 解析 @Configuration 类
  │     │     - 扫描 @ComponentScan 路径
  │     │     - 解析 @Import, @ImportResource
  │     ├── registerBeanPostProcessors()   注册 Bean 后处理器
  │     ├── initMessageSource()           国际化
  │     ├── initApplicationEventMulticaster() 事件广播器
  │     ├── onRefresh()                   创建内嵌 Web 服务器
  │     ├── registerListeners()           注册事件监听器
  │     ├── finishBeanFactoryInitialization()  ← 实例化所有单例 Bean！
  │     │     - 实例化（反射）
  │     │     - 属性注入（@Autowired）
  │     │     - 初始化回调（@PostConstruct 等）
  │     └── finishRefresh()               发布 ContextRefreshedEvent
  │
  └── 6. 执行 ApplicationRunner / CommandLineRunner
```

---

## 3. Bean 生命周期与作用域

### 3.1 完整生命周期（14 步）

```
┌─────────────────────────────────────────────────────────────┐
│ 1. 实例化（Instantiation）                                   │
│    调用构造方法（反射），创建对象                              │
│    ↓                                                        │
│ 2. 属性注入（Populate Properties）                           │
│    @Autowired / @Value / @Resource 的字段和方法被赋值         │
│    ↓                                                        │
│ 3. BeanNameAware.setBeanName()                              │
│    获取自己在容器中的名字                                     │
│    ↓                                                        │
│ 4. BeanClassLoaderAware.setBeanClassLoader()                │
│    获取类加载器                                              │
│    ↓                                                        │
│ 5. BeanFactoryAware.setBeanFactory()                        │
│    获取 BeanFactory 引用                                     │
│    ↓                                                        │
│ 6. ApplicationContextAware.setApplicationContext()          │
│    获取 ApplicationContext 引用                              │
│    ↓                                                        │
│ 7. BeanPostProcessor.postProcessBeforeInitialization()      │
│    前置处理（可返回代理对象）                                  │
│    ↓                                                        │
│ 8. @PostConstruct 注解方法                                   │
│    初始化回调（JSR-250）                                     │
│    ↓                                                        │
│ 9. InitializingBean.afterPropertiesSet()                    │
│    Spring 回调接口（优先级低于 @PostConstruct）               │
│    ↓                                                        │
│ 10. 自定义 init-method                                       │
│     XML 中的 init-method 或 @Bean(initMethod = "init")       │
│    ↓                                                        │
│ 11. BeanPostProcessor.postProcessAfterInitialization()      │
│     后置处理（AOP 代理在这里完成！）                          │
│    ↓                                                        │
│ 12. Bean 就绪！（可以被正常使用了）                           │
│    ↓                                                        │
│ 13. @PreDestroy 注解方法                                     │
│     ↓                                                        │
│ 14. DisposableBean.destroy() + 自定义 destroy-method         │
│     销毁回调（关闭容器时执行）                                 │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 作用域深度对比

| 作用域 | 常量 | 说明 | 使用场景 |
|--------|------|------|---------|
| singleton | ConfigurableBeanFactory.SCOPE_SINGLETON | 整个容器一个实例（默认） | 无状态 Service/Mapper |
| prototype | ConfigurableBeanFactory.SCOPE_PROTOTYPE | 每次获取新实例 | 有状态 Bean |
| request | WebApplicationContext.SCOPE_REQUEST | 每个 HTTP 请求一个实例 | Request 级别数据 |
| session | WebApplicationContext.SCOPE_SESSION | 每个 Session 一个实例 | Session 级别数据 |
| application | WebApplicationContext.SCOPE_APPLICATION | 整个 ServletContext 一个实例 | Web 全局数据 |

```java
// 作用域陷阱
// 单例 Bean 中注入 Prototype Bean
@Scope("singleton")
public class SingletonBean {
    @Autowired
    private PrototypeBean prototypeBean;  // 只会注入一次！
}
// 解决方案 1：@Lookup
@Lookup
public PrototypeBean getPrototypeBean() { return null; }

// 方案 2：注入 ApplicationContext
@Autowired
private ApplicationContext context;
public void use() {
    PrototypeBean bean = context.getBean(PrototypeBean.class);
}

// 方案 3：Proxy 模式
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class PrototypeBean { }
```

---

## 4. 依赖注入最佳实践

### 4.1 注入方式对比

```java
// 方式 1：构造器注入（✅ 强烈推荐）
@RestController
@RequiredArgsConstructor  // Lombok：自动为 final 字段生成构造器
public class UserController {
    private final UserService userService;
    private final OrderService orderService;
    // 优点：不可变（final）、依赖明确、测试友好、不依赖 Spring
}
// 如果只有一个构造器，Spring 自动用这个构造器，无需 @Autowired

// 方式 2：Setter 注入（⚠️ 可选依赖时使用）
public class UserService {
    private CacheService cacheService;  // 可选依赖
    @Autowired(required = false)
    public void setCacheService(CacheService cacheService) {
        this.cacheService = cacheService;
    }
}

// 方式 3：字段注入（❌ 不推荐，但最常见）
@Autowired
private UserService userService;
// 缺点：依赖隐藏（看不到有多少依赖）、不可变、测试需反射、强依赖 Spring
// 优点：代码短
```

### 4.2 @Autowired vs @Resource

```java
// @Autowired（Spring 注解）
// - 按类型注入
// - 配合 @Qualifier 按名称指定
// - required 属性控制是否必须
@Autowired(required = false)
@Qualifier("userServiceImpl")
private UserService userService;

// @Resource（JSR-250 注解，不受 Spring 约束）
// - 默认按名称注入（找 name 匹配的 Bean）
// - 找不到再按类型
// - JDK 11 后需要额外依赖（javax.annotation → jakarta.annotation）
@Resource(name = "userServiceImpl")
private UserService userService;

// 多 Bean 注入为 Map 或 List
@Autowired
private Map<String, UserService> userServiceMap;  // key=beanName, value=实例

@Autowired
private List<UserService> userServices;  // 所有 UserService Bean 的列表
// 适合策略模式：一次注入所有实现，运行时选一个
```

### 4.3 循环依赖与解决方案

```java
// 循环依赖场景
@Service
public class AService {
    @Autowired private BService bService;
}

@Service
public class BService {
    @Autowired private AService aService;
}
// → A 和 B 互相依赖，形成死循环

// Spring 的解决方案（只适用于构造器注入以外的场景）：
// 三级缓存机制

// 一级缓存：singletonObjects（成品 Bean 池）
// 二级缓存：earlySingletonObjects（半成品 Bean 的引用）
//           创建早期 Bean 引用放在这
// 三级缓存：singletonFactories（ObjectFactory，用于生成早期引用）

// 解决流程：
// 1. A 实例化 → 放入三级缓存（ObjectFactory，可生成 A 的代理引用）
// 2. A 属性注入，需要 B
// 3. B 实例化 → 属性注入，需要 A
// 4. 从三级缓存拿到 A 的 ObjectFactory → 生成 A 的早期引用
// 5. A 的早期引用放入二级缓存
// 6. B 注入 A 的早期引用 → B 初始化完成 → 放入一级缓存
// 7. A 注入 B（成品）→ A 初始化完成 → 放入一级缓存

// 构造器注入的循环依赖无法解决！（因为连实例化都没完成）
// @Async + 循环依赖可能导致问题

// ✅ 最佳实践：避免循环依赖！
// - 重新设计（抽取公共依赖 C）
// - 用事件机制解耦
// - 用 @Lazy 推迟初始化
@Lazy
@Autowired
private BService bService;  // 注入时不会立即初始化 B
```

---

## 5. AOP 面向切面编程

### 5.1 AOP 术语

```
┌──────────────────┬─────────────────────────────────────────────┐
│ Aspect（切面）    │ 横切关注点的模块化                          │
│                  │ = Pointcut + Advice                          │
├──────────────────┼─────────────────────────────────────────────┤
│ Join Point（连接点）│ 程序执行中的点（方法调用、异常抛出）        │
├──────────────────┼─────────────────────────────────────────────┤
│ Pointcut（切入点）│ 匹配 Join Point 的表达式                    │
│                  │ "哪些方法要增强"                            │
├──────────────────┼─────────────────────────────────────────────┤
│ Advice（通知）    │ 在切入点做什么                             │
│                  │ "增强什么逻辑"                              │
├──────────────────┼─────────────────────────────────────────────┤
│ Target（目标对象）│ 被增强的对象                               │
├──────────────────┼─────────────────────────────────────────────┤
│ Proxy（代理对象） │ 增强后的对象（JDK 动态代理 或 CGLIB）       │
├──────────────────┼─────────────────────────────────────────────┤
│ Weaving（织入）  │ 将切面应用到目标对象创建代理的过程           │
│                  │ 编译期 / 类加载期 / 运行期（Spring 用运行期）│
└──────────────────┴─────────────────────────────────────────────┘
```

### 5.2 Advice 五种类型

```java
@Aspect
@Component
public class LogAspect {

    @Pointcut("execution(* com.example.service.*.*(..))")
    public void servicePointcut() {}

    // 1. @Before 前置通知（方法执行前）
    @Before("servicePointcut()")
    public void before(JoinPoint joinPoint) {
        log.info("开始执行: {}", joinPoint.getSignature());
    }

    // 2. @AfterReturning 后置返回通知（方法正常返回后，可获取返回值）
    @AfterReturning(pointcut = "servicePointcut()", returning = "result")
    public void afterReturning(JoinPoint joinPoint, Object result) {
        log.info("执行成功: {}, 返回值: {}", joinPoint.getSignature(), result);
    }

    // 3. @AfterThrowing 后置异常通知（方法抛异常后）
    @AfterThrowing(pointcut = "servicePointcut()", throwing = "e")
    public void afterThrowing(JoinPoint joinPoint, Exception e) {
        log.error("执行异常: {}, 异常信息: {}", joinPoint.getSignature(), e.getMessage());
    }

    // 4. @After 最终通知（方法执行后，无论成功还是异常 — 类似 finally）
    @After("servicePointcut()")
    public void after(JoinPoint joinPoint) {
        log.info("执行结束: {}", joinPoint.getSignature());
    }

    // 5. @Around 环绕通知（最强大，可控制方法是否执行，可修改参数和返回值）
    @Around("servicePointcut()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        Object[] args = pjp.getArgs();
        // 可修改参数
        // args[0] = modify(args[0]);

        try {
            Object result = pjp.proceed(args);  // 执行目标方法
            return result;
        } catch (Exception e) {
            // 可处理异常
            throw e;
        } finally {
            long cost = System.currentTimeMillis() - start;
            if (cost > 1000) {
                log.warn("慢方法: {}, 耗时: {}ms", pjp.getSignature(), cost);
            }
        }
    }
}
```

### 5.3 Pointcut 表达式语法

```
execution(modifiers-pattern? ret-type-pattern declaring-type-pattern?name-pattern(param-pattern) throws-pattern?)

常用表达式：

// 匹配所有 public 方法
execution(public * *(..))

// 匹配 service 包下所有方法
execution(* com.example.service.*.*(..))

// 匹配 service 包及子包下所有方法
execution(* com.example.service..*.*(..))

// 匹配 UserService 中所有方法
execution(* com.example.service.UserService.*(..))

// 匹配以 save 开头的方法
execution(* save*(..))

// 匹配第一个参数为 Long 的方法
execution(* *(Long, ..))

// 其他匹配方式
@annotation(org.springframework.transaction.annotation.Transactional)
    // 匹配 @Transactional 注解的方法
within(com.example.service.*)
    // 匹配 service 包下的所有方法
@within(org.springframework.stereotype.Service)
    // 匹配 @Service 注解的类中的所有方法
@args(org.springframework.validation.annotation.Validated)
    // 匹配参数带 @Validated 注解的方法
bean(userService)
    // 匹配 beanName 为 userService 的 Bean 的所有方法
```

### 5.4 JDK 动态代理 vs CGLIB

```
JDK 动态代理：
  - 基于接口
  - Proxy.newProxyInstance() + InvocationHandler
  - 被代理的类必须实现至少一个接口
  - 创建代理对象快，调用稍慢（反射）

CGLIB 动态代理：
  - 基于继承（生成目标类的子类）
  - 通过 ASM 字节码框架修改字节码
  - 不能代理 final 类和方法
  - 创建代理对象慢（生成字节码），调用快（直接调用）

Spring Boot 默认：
  2.0 之前：有接口 → JDK 代理，无接口 → CGLIB
  2.0 之后：统一使用 CGLIB
  可以通过 spring.aop.proxy-target-class=false 恢复为 JDK 代理

为什么默认改为 CGLIB？
  - 统一行为，避免开发者的困惑
  - 避免强制注入接口类型的限制
  - 性能更好（虽然创建稍慢，但调用更快）
```

---

## 6. 事务管理完全指南

### 6.1 事务核心概念

```
@Transactional 原理：
  - 基于 AOP
  - 在方法执行前通过 DataSourceTransactionManager 开启事务
  - 方法正常返回 → 提交事务
  - 方法抛异常 → 回滚事务
  - 默认只回滚 RuntimeException 和 Error

关键配置：
  propagation   → 传播行为
  isolation     → 隔离级别
  timeout       → 超时时间
  readOnly      → 只读（优化，提示 MySQL 使用非锁定读）
  rollbackFor   → 指定回滚的异常类型
  noRollbackFor → 指定不回滚的异常类型
```

### 6.2 传播行为详解

| 传播行为 | 当前有事务 | 当前无事务 | 典型场景 |
|---------|-----------|-----------|---------|
| **REQUIRED**（默认） | 加入当前事务 | 新建事务 | 大多数场景 |
| **REQUIRES_NEW** | 挂起当前事务，新建 | 新建事务 | 日志记录（不受主事务影响） |
| **NESTED** | 嵌套事务（Savepoint） | 新建事务 | 子操作可独立回滚 |
| **SUPPORTS** | 加入当前事务 | 非事务执行 | 灵活场景 |
| **NOT_SUPPORTED** | 挂起当前事务，非事务 | 非事务执行 | 不关心事务的操作 |
| **MANDATORY** | 加入当前事务 | 抛异常 | 强制有事务 |
| **NEVER** | 抛异常 | 非事务执行 | 强制无事务 |

```java
// 实战：REQUIRES_NEW 的典型场景
@Service
public class OrderService {
    @Transactional
    public void createOrder(OrderDTO dto) {
        orderMapper.insert(order);       // 主事务
        accountService.debit(dto);       // 主事务
        try {
            logService.record(dto);      // 独立事务，失败了不影响主事务
        } catch (Exception e) {
            // 记录日志失败不影响下单
        }
    }
}

@Service
public class LogService {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Object data) { ... }
}
```

### 6.3 事务失效的 8 种场景

```java
// 1. 方法非 public（最常见！）
@Transactional
private void doSomething() { }  // 事务不生效！AOP 代理无法拦截 private 方法

// 2. 同类方法调用（不经过代理）
@Transactional
public void methodA() {
    this.methodB();  // this 调用不经过代理 → @Transactional 不生效！
}
@Transactional
public void methodB() { }
// 解决方案：注入自身 / 抽到另一个 Service / AopContext.currentProxy()

// 3. 异常被 catch 了
@Transactional
public void method() {
    try {
        int a = 1 / 0;  // 抛异常
    } catch (Exception e) {
        // 吞掉异常 → Spring 感知不到 → 不回滚
    }
}
// 解决方案：catch 后手动回滚 TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
//          或者 catch 后重新抛出 RuntimeException

// 4. 异常类型不匹配
@Transactional  // 默认只回滚 RuntimeException
public void method() throws IOException {
    throw new IOException();  // Checked Exception → 不回滚！
}
// 解决方案：@Transactional(rollbackFor = Exception.class)

// 5. 数据库引擎不支持事务
// MyISAM 不支持事务 → @Transactional 不生效

// 6. 类没有被 Spring 管理
// 没有 @Service / @Component 注解

// 7. 多线程中
new Thread(() -> {
    service.method();  // 新线程不在当前事务中
}).start();

// 8. propagation 配置错误
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public void method() { }  // 非事务执行 → 不会回滚
```

---

## 7. Spring MVC 请求处理全流程

### 7.1 DispatcherServlet 处理流程

```
────────────────────────────── 请求到达 ────────────────────────────
                                  │
          ┌───────────────────────┼───────────────────────┐
          │ 1. DispatcherServlet.doDispatch()              │
          │    getHandler(request)  → 获取 Handler         │
          │    ↓                                          │
          │ 2. HandlerMapping                              │
          │    根据 URL → 找到匹配的 Controller 方法          │
          │    RequestMappingHandlerMapping                 │
          │    ↓                                          │
          │ 3. HandlerAdapter                             │
          │    调用 Handler 方法                            │
          │    RequestMappingHandlerAdapter                 │
          │    ↓                                          │
          │    3a. 参数解析器（HandlerMethodArgumentResolver）│
          │        @PathVariable / @RequestParam            │
          │        @RequestBody / @ModelAttribute           │
          │        参数校验（@Valid）                        │
          │    ↓                                          │
          │    3b. HandlerInterceptor.preHandle()          │
          │    ↓                                          │
          │    3c. 调用 Controller 方法                     │
          │        ↓                                      │
          │    3d. HandlerInterceptor.postHandle()         │
          │    ↓                                          │
          │ 4. 返回值处理器（HandlerMethodReturnValueHandler）│
          │    @ResponseBody → HttpMessageConverter 序列化   │
          │    View → ViewResolver 解析视图名                │
          │    ↓                                          │
          │    4a. HandlerInterceptor.afterCompletion()    │
          │    ↓                                          │
          │ 5. 响应返回客户端                                │
          └───────────────────────────────────────────────┘
```

### 7.2 Filter vs Interceptor vs AOP

```
执行顺序：Filter → Interceptor → AOP → Controller → AOP → Interceptor → Filter

┌──────────────┬────────────────────┬──────────────┬────────────────┐
│ 维度          │ Filter             │ Interceptor  │ AOP            │
├──────────────┼────────────────────┼──────────────┼────────────────┤
│ 层级          │ Servlet 容器       │ Spring MVC   │ Spring 容器    │
│ 依赖          │ 不依赖 Spring      │ 依赖 Spring  │ 依赖 Spring    │
│ 访问范围       │ 可改请求/响应       │ 可获取 Handler│ 可获取参数/返回值│
│ 控制粒度       │ 粗（URL 级别）     │ 中（URL→Handler）│ 细（方法级别）│
│ 配置方式       │ @WebFilter 或 @Bean│ @Component   │ @Aspect       │
│ 适用场景       │ 字符编码/CORS/日志  │ 权限校验/登录  │ 日志/事务/缓存 │
└──────────────┴────────────────────┴──────────────┴────────────────┘
```

### 7.3 参数解析与类型转换

```java
// 自定义参数解析器
@Component
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mav,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        // 从 Header 中解析 Token → 获取用户 → 返回
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        String token = request.getHeader("Authorization");
        User user = jwtService.parseToken(token);
        return user;
    }
}

// Controller 中使用
@GetMapping("/me")
public Result<User> getCurrentUser(@LoginUser User user) {  // 自动注入当前用户
    return Result.success(user);
}
```

---

## 8. Spring Boot 自动配置原理

### 8.1 自动配置工作流程

```
@SpringBootApplication  =
  @SpringBootConfiguration     (等同 @Configuration)
  @EnableAutoConfiguration      (自动配置的魔法)
  @ComponentScan               (组件扫描)

@EnableAutoConfiguration →
  @Import(AutoConfigurationImportSelector.class)

AutoConfigurationImportSelector 执行流程：
  1. 从 META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
     读取所有自动配置类列表（Spring Boot 2.x 从 spring.factories 读取）
  2. 通过 @ConditionalOnXxx 过滤：
     @ConditionalOnClass      → classpath 有指定类
     @ConditionalOnMissingClass
     @ConditionalOnBean       → 容器中有指定 Bean
     @ConditionalOnMissingBean
     @ConditionalOnProperty   → 配置文件中有指定属性
     @ConditionalOnResource   → 存在指定资源文件
     @ConditionalOnWebApplication → 是 Web 应用
     @ConditionalOnExpression → SpEL 表达式
  3. 满足条件 → 加载配置类 → 注册 Bean
     不满足 → 跳过

示例：DataSourceAutoConfiguration
  @ConditionalOnClass(DataSource.class)  → classpath 有 DataSource 类
  @ConditionalOnMissingBean(DataSource.class) → 用户没自己注册 DataSource
  @EnableConfigurationProperties(DataSourceProperties.class)
    读取 spring.datasource.* 配置
  → 满足 → 创建 DataSource Bean（HikariCP）
```

### 8.2 自定义 Starter

```java
// 自定义 Starter 步骤：
// 1. 创建 autoconfigure 模块
// 2. 创建 starter 模块（空模块，引入 autoconfigure）
// 3. 编写自动配置类

// autoconfigure 模块结构：
// my-spring-boot-starter
//   ├── pom.xml（引入 my-spring-boot-autoconfigure）
//   └── （空项目，只是一个依赖桥）
//
// my-spring-boot-autoconfigure
//   ├── pom.xml
//   └── src/main/
//       ├── java/com/example/
//       │   ├── MyService.java
//       │   ├── MyProperties.java
//       │   └── MyAutoConfiguration.java
//       └── resources/META-INF/spring/
//           └── org.springframework.boot.autoconfigure.AutoConfiguration.imports

// MyProperties.java
@ConfigurationProperties(prefix = "my")
@Data
public class MyProperties {
    private String name = "default";
    private int timeout = 5000;
    private boolean enabled = true;
}

// MyAutoConfiguration.java
@AutoConfiguration
@EnableConfigurationProperties(MyProperties.class)
@ConditionalOnProperty(prefix = "my", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MyAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public MyService myService(MyProperties properties) {
        return new MyService(properties);
    }
}

// AutoConfiguration.imports 文件内容：
// com.example.MyAutoConfiguration
```

---

## 9. Spring Boot 核心能力

### 9.1 配置文件

```yaml
# application.yml 常用配置大全
server:
  port: 8080
  servlet:
    context-path: /api
  compression:
    enabled: true
    mime-types: text/html,text/xml,text/plain,text/css,application/json

spring:
  application:
    name: user-service
  profiles:
    active: dev

  # 数据源
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      connection-timeout: 30000

  # Redis
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2

  # Jackson
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
    default-property-inclusion: non_null  # 不序列化 null 字段
    serialization:
      write-dates-as-timestamps: false

  # 多环境配置
  # application-dev.yml    开发环境
  # application-test.yml   测试环境
  # application-prod.yml   生产环境

# 日志
logging:
  level:
    root: INFO
    com.example: DEBUG
    org.springframework.security: DEBUG
  file:
    name: logs/app.log
    max-size: 100MB
    max-history: 30
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### 9.2 Actuator 监控

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,env
      base-path: /actuator
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true  # K8s 存活性和就绪性探针
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}

# 常用端点：
# /actuator/health/readiness    - K8s Readiness Probe
# /actuator/health/liveness     - K8s Liveness Probe
# /actuator/metrics             - 所有指标
# /actuator/prometheus          - Prometheus 格式
# /actuator/env                 - 环境变量
# /actuator/loggers             - 动态日志级别
# /actuator/threaddump          - 线程转储
# /actuator/heapdump            - 堆转储（内存大的时慎用）
```

### 9.3 异步与调度

```java
// 开启异步支持
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}

// 使用异步
@Async
public CompletableFuture<String> fetchData() {
    String result = externalApi.call();
    return CompletableFuture.completedFuture(result);
}

// 开启调度
@Configuration
@EnableScheduling
public class SchedulerConfig { }

// 使用调度
@Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨 2 点
public void cleanExpiredData() { }

@Scheduled(fixedRate = 60000)     // 每隔 60 秒（任务开始计时）
public void syncData() { }

@Scheduled(fixedDelay = 30000)    // 上次任务结束后 30 秒
public void processQueue() { }
```

---

## 10. Spring Security 认证授权

### 10.1 安全架构

```
Spring Security 核心组件：

SecurityContext（安全上下文）
  └── Authentication（认证信息）
       ├── Principal（用户主体）
       ├── Credentials（凭证，通常被清除）
       └── Authorities（权限列表）

过滤器链（SecurityFilterChain）：
  ┌──────────────────────────────────────┐
  │ SecurityContextPersistenceFilter      │  持久化 SecurityContext
  │ CorsFilter                           │  跨域
  │ CsrfFilter                           │  CSRF 防护
  │ LogoutFilter                         │  登出处理
  │ UsernamePasswordAuthenticationFilter │  表单登录
  │ DefaultLoginPageGeneratingFilter     │  默认登录页
  │ BasicAuthenticationFilter            │  HTTP Basic 认证
  │ BearerTokenAuthenticationFilter      │  JWT Bearer Token
  │ ExceptionTranslationFilter           │  异常转换（403→403页面）
  │ FilterSecurityInterceptor            │  最终授权决策
  └──────────────────────────────────────┘
```

### 10.2 JWT 认证实战

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 关闭 CSRF（前后端分离场景）
            .csrf(AbstractHttpConfigurer::disable)
            // 无状态（不创建 Session）
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 路由权限配置
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()        // 登录注册公开
                .requestMatchers("/api/public/**").permitAll()      // 公开接口
                .requestMatchers("/api/admin/**").hasRole("ADMIN")  // 管理员
                .requestMatchers("/api/users/**").hasAuthority("user:read")  // 有读权限
                .anyRequest().authenticated()                        // 其余需要认证
            )
            // 添加 JWT 过滤器（在 UsernamePasswordAuthenticationFilter 之前）
            .addFilterBefore(jwtAuthenticationFilter(),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();  // 自适应单向哈希
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
        throws Exception {
        return config.getAuthenticationManager();
    }
}

// JWT 认证过滤器
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
        throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtService.validateToken(token)) {
                Authentication auth = jwtService.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }
}

// 方法级权限控制
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/admin/dashboard")
public Result<?> adminDashboard() { }

@PreAuthorize("hasAuthority('user:update') or #id == authentication.principal.id")
@PutMapping("/{id}")
public Result<?> updateUser(@PathVariable Long id, @RequestBody UserDTO dto) { }

@PostAuthorize("returnObject.data.ownerId == authentication.principal.id")
@GetMapping("/{id}")
public Result<?> getUser(@PathVariable Long id) { }
```

---

## 11. Spring Event 事件机制

```java
// 1. 定义事件
@Getter
public class UserCreatedEvent extends ApplicationEvent {
    private final Long userId;
    public UserCreatedEvent(Object source, Long userId) {
        super(source);
        this.userId = userId;
    }
}

// 2. 发布事件
@Service
@RequiredArgsConstructor
public class UserService {
    private final ApplicationEventPublisher publisher;

    @Transactional
    public void createUser(UserDTO dto) {
        User user = userMapper.insert(dto.toEntity());
        // 发布事件（事务提交后执行）
        publisher.publishEvent(new UserCreatedEvent(this, user.getId()));
    }
}

// 3. 监听事件
@Component
public class UserEventListener {

    @EventListener
    @Async  // 异步执行
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    // 事务提交后执行（默认就是 AFTER_COMMIT）
    public void handleUserCreated(UserCreatedEvent event) {
        // 发送欢迎邮件
        // 初始化用户配置
        // 上报统计数据
    }
}

// 可以用 @Order 控制多个监听器的执行顺序
// @TransactionalEventListener：
//   AFTER_COMMIT      事务提交后（默认）
//   AFTER_ROLLBACK    事务回滚后
//   AFTER_COMPLETION  事务完成后（无论成功还是回滚）
//   BEFORE_COMMIT     事务提交前
```

---

## 12. Spring 扩展点与高级特性

### 12.1 常用扩展点

```java
// 1. BeanPostProcessor（Bean 后处理器）
// 每个 Bean 初始化前后都会调用
@Component
public class MyBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;  // 初始化前
    }
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;  // 初始化后
    }
}

// 2. BeanFactoryPostProcessor（BeanFactory 后处理器）
// 在所有 Bean 定义加载后、实例化前调用
@Component
public class MyBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory factory) {
        // 可以修改 Bean 定义（如修改属性值、Scope）
    }
}

// 3. InitializingBean / DisposableBean
// @PostConstruct 和 @PreDestroy 是 JSR-250，不依赖 Spring
// InitializingBean 和 DisposableBean 是 Spring 专有接口
@Component
public class MyBean implements InitializingBean, DisposableBean {
    @Override
    public void afterPropertiesSet() { /* 属性设置完成 */ }
    @Override
    public void destroy() { /* 销毁 */ }
}

// 4. ApplicationListener
@Component
public class MyAppListener implements ApplicationListener<ContextRefreshedEvent> {
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 容器刷新完成（所有 Bean 就绪）
        log.info("应用启动完成");
    }
}

// 5. ApplicationRunner / CommandLineRunner
// 启动后执行一次性任务
@Component
public class MyRunner implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) {
        log.info("应用启动，参数: {}", args.getOptionNames());
    }
}
```

### 12.2 @Conditional 条件注解

```java
// Spring Boot 内置的条件注解
@ConditionalOnClass(RedisOperations.class)     // 类路径存在
@ConditionalOnMissingBean(CacheManager.class) // 缺少该 Bean
@ConditionalOnProperty(prefix = "my.cache", name = "enabled", havingValue = "true")
@ConditionalOnBean(DataSource.class)          // 存在该 Bean
@ConditionalOnResource(resources = "classpath:my.properties")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnExpression("${my.feature.enabled:true}")
@ConditionalOnJava(JavaVersion.TWENTY_ONE)    // Java 版本

// 自定义条件注解
@Component
public class MyCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        // 自定义逻辑判断
        return context.getEnvironment().getProperty("my.feature.enabled", Boolean.class, true);
    }
}

@Conditional(MyCondition.class)
@Bean
public MyService myService() { return new MyService(); }
```

### 12.3 Spring SPI 机制

```
Spring 的 SPI 实际上就是利用 JDK SPI 和 Boot 自己的配置加载机制：

JDK SPI（ServiceLoader）：
  META-INF/services/com.example.MyInterface
  文件内容 = 实现类的全限定名

Spring Boot AutoConfiguration.imports：
  META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
  和 JDK SPI 不同的实现，但理念相似

Spring Factories（已废弃，Spring Boot 3.x 改用 imports）：
  META-INF/spring.factories
  Spring Boot 2.x 的关键扩展机制
  3.x 中不推荐但部分场景仍可用
```

---

## 13. Spring Boot 3.x 新特性

### 13.1 关键变化

```
Spring Boot 3.x 重大变化：

1. Java 17 Baseline
   → 必须 JDK 17+ 才能运行

2. Jakarta EE 9+ 替代 Java EE
   → javax.* → jakarta.*
   → javax.servlet → jakarta.servlet
   → javax.persistence → jakarta.persistence
   → @javax.annotation.Resource → @jakarta.annotation.Resource

3. Spring Framework 6.x
   → 移除了大量过时的 API
   → 支持 GraalVM Native Image（AOT）

4. AutoConfiguration.imports 替代 spring.factories
   → 新的自动配置注册方式

5. 可观测性改进
   → Micrometer + Observation API

6. 虚拟线程支持
   → spring.threads.virtual.enabled=true

7. 声明式 HTTP 客户端
   → @HttpExchange 替代 RestTemplate（推荐用 WebClient 或 RestClient）
```

---

## 14. 常见面试题深度解析

### Q1: Spring Bean 是线程安全的吗？

```
Spring Bean 默认是单例的，但单例 ≠ 线程安全！

Spring 本身不保证 Bean 的线程安全，线程安全取决于 Bean 的实现：
  - 无状态 Bean（如 Service、Repository）：天然线程安全 ✅
    因为方法是无状态的（局部变量在线程栈上，不共享）

  - 有状态 Bean（有成员变量被修改）：不是线程安全！
    public class UnsafeService {
        private int count = 0;  // 共享状态！
        public void increment() { count++; }  // 并发问题！
    }

  - Prototype Bean：每次 new → 线程安全（每个线程有自己的实例）

最佳实践：
  1. Service/Controller 中的成员变量设为只读（注入的依赖不修改）
  2. 有状态的操作使用局部变量
  3. 必须共享状态用 ThreadLocal 或 synchronized
```

### Q2: Spring 如何解决循环依赖？

```
Spring 通过三级缓存解决构造器注入以外的循环依赖：

singletonObjects（一级缓存）：成品 Bean
earlySingletonObjects（二级缓存）：早期 Bean 引用（未完成属性注入）
singletonFactories（三级缓存）：ObjectFactory，可生成代理对象

流程（A → B → A）：
  1. 创建 A → 实例化 → 放入三级缓存（singletonFactories 存 A 的 ObjectFactory）
  2. 填充 A 的属性（需要 B）→ 去容器找 B
  3. B 不存在 → 创建 B → 实例化 → 放入三级缓存
  4. 填充 B 的属性（需要 A）→ 从三级缓存获取 A 的 ObjectFactory
     → 调用 getObject() 获取 A 的早期引用 → 放入二级缓存
  5. B 拿到 A 的早期引用 → 完成填充 → B 初始化 → 放入一级缓存
  6. A 拿到成品 B → 完成填充 → A 初始化 → 放入一级缓存

为什么构造器注入的循环依赖无法解决？
  因为构造器注入在步骤 1 就卡住了：
  创建 A 需要先调构造器，而构造器需要 B → 但又没有 B 的成品 → 死循环
  此时代理都还没创建，三级缓存也帮不上忙

为什么 @Async 的 Bean 循环依赖有问题？
  @Async 会创建代理，ObjectFactory 返回的可能是原对象而非代理
  导致注入的 Bean 和最终暴露的 Bean 不一致
```

### Q3: @SpringBootApplication 做了什么？

```
@SpringBootApplication =
  @SpringBootConfiguration  (@Configuration 的包装)
  @EnableAutoConfiguration  (自动配置的核心)
  @ComponentScan            (扫描同包及子包的 @Component/@Service/@Controller 等)

@ComponentScan：
  默认扫描启动类所在包及其子包
  通过 basePackages 可以指定其他包

@EnableAutoConfiguration：
  @Import(AutoConfigurationImportSelector.class)
  → 读取 META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
  → @ConditionalOnXxx 过滤
  → 加载满足条件的自动配置类

示例：引入 spring-boot-starter-web
  → classpath 有 DispatcherServlet.class
  → WebMvcAutoConfiguration 的条件满足
  → 自动配置 Spring MVC（DispatcherServlet、视图解析器、消息转换器等）
```

> **上一篇：** [03-Web基础与RESTful API设计](./03-Web基础与RESTful%20API设计.md)
>
> **下一篇：** [05-持久层与数据库技术](./05-持久层与数据库技术.md)
