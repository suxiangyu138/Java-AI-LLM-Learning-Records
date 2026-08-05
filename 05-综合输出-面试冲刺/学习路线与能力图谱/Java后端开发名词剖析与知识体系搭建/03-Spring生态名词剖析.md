# Spring 生态名词剖析

> 🌿 Spring 全家桶核心概念 50+ —— IoC/DI、AOP、Spring Boot 自动配置、Spring Cloud 微服务、Security 安全、事务管理

---

## 📚 目录

1. [Spring Framework 核心](#1-spring-framework-核心)
2. [Spring Boot](#2-spring-boot)
3. [Spring MVC](#3-spring-mvc)
4. [Spring Cloud 微服务](#4-spring-cloud-微服务)
5. [Spring Security](#5-spring-security)
6. [Spring 事务管理](#6-spring-事务管理)
7. [Spring 扩展点](#7-spring-扩展点)

---

## 1. Spring Framework 核心

### 1.1 IoC 与 DI

```text
IoC = Inversion of Control（控制反转）
  → 将对象创建和依赖管理的控制权交给 Spring 容器
  → 好莱坞原则："Don't call us, we'll call you."

DI = Dependency Injection（依赖注入）
  → IoC 的具体实现方式
  → 三种方式：
    1. 构造器注入（推荐 ✅）
    2. Setter 注入
    3. 字段注入 @Autowired（不推荐，测试困难）
```

```java
// 构造器注入（推荐）
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Spring 4.3+ 单构造器自动 @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
}

// 为什么推荐构造器注入？
// ✅ 依赖不可变（final）
// ✅ 保证依赖不为 null
// ✅ 单元测试方便（直接 new 即可）
// ✅ 避免循环依赖的问题
```

### 1.2 Bean 的作用域

| 作用域 | 说明 | 使用场景 |
|--------|------|---------|
| **singleton** | 全局唯一实例（默认） | 无状态 Service、Controller、Repository |
| **prototype** | 每次获取创建新实例 | 有状态 Bean（极少用） |
| **request** | 每个 HTTP 请求一个实例 | Web 应用 |
| **session** | 每个 HTTP Session 一个实例 | 用户会话数据 |
| **application** | ServletContext 级别 | Web 应用全局 |

### 1.3 Bean 的生命周期

```text
Bean 生命周期（简化版）：

  实例化 → 属性填充 → 
  BeanNameAware → BeanFactoryAware → ApplicationContextAware →
  @PostConstruct (init-method) → 
  InitializingBean.afterPropertiesSet() →
  ... Bean 使用中 ...
  @PreDestroy (destroy-method) →
  DisposableBean.destroy() →
  销毁
```

### 1.4 循环依赖

```java
// Spring 解决构造器循环依赖：无法解决！
// Spring 解决 Setter 循环依赖：三级缓存

// 三级缓存：
// 一级缓存：singletonObjects      → 完全初始化好的 Bean
// 二级缓存：earlySingletonObjects → 提前曝光的 Bean（未完成属性填充）
// 三级缓存：singletonFactories    → Bean 工厂（可生成代理对象）

// A → B → A 的解决流程：
// 1. A 实例化 → 放入三级缓存
// 2. A 填充属性，发现需要 B
// 3. B 实例化 → 放入三级缓存
// 4. B 填充属性，发现需要 A → 从三级缓存获取 A 的早期引用
// 5. B 初始化完成 → 放入一级缓存
// 6. A 继续填充 → A 初始化完成 → 放入一级缓存

// ⚠️ 构造器注入无法解决循环依赖（因为无法提前暴露引用）
// ⚠️ prototype 作用域无法解决循环依赖
```

---

## 2. Spring Boot

### 2.1 核心注解

```java
@SpringBootApplication  // = @Configuration + @EnableAutoConfiguration + @ComponentScan
@ComponentScan           // 扫描 @Component/@Service/@Repository/@Controller
@EnableAutoConfiguration // 通过 spring.factories 自动加载配置类
@Configuration           // 标记配置类（替代 XML）
@Bean                    // 声明 Bean，方法名 = Bean 名
```

### 2.2 自动配置原理

```text
@SpringBootApplication
  → @EnableAutoConfiguration
    → @Import(AutoConfigurationImportSelector.class)
      → 读取 META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
        → 加载所有 xxxAutoConfiguration 类
          → @ConditionalOnClass / @ConditionalOnMissingBean 等条件判断
            → 满足条件 → 创建 Bean

@Conditional 条件注解家族：
  @ConditionalOnClass       → 类路径存在某类时生效
  @ConditionalOnMissingClass
  @ConditionalOnBean        → 容器中存在某 Bean 时生效
  @ConditionalOnMissingBean
  @ConditionalOnProperty    → 配置项满足条件时生效
  @ConditionalOnExpression  → SpEL 表达式满足时生效
```

### 2.3 Starter 机制

```text
Starter = 依赖打包器

一个 Starter 包含：
  ├── 所需的所有依赖（无需手动一个个加）
  ├── 自动配置类（AutoConfiguration）
  └── 默认配置（application.yml 可覆盖）

例子：spring-boot-starter-data-redis 内部：
  ├── spring-data-redis
  ├── lettuce（Redis 客户端）
  └── RedisAutoConfiguration（自动连接配置）
```

---

## 3. Spring MVC

### 3.1 请求处理流程

```text
HTTP Request
    │
    ▼
DispatcherServlet（前端控制器）
    │
    ▼
HandlerMapping（处理器映射）→ 找到对应 Controller
    │
    ▼
HandlerAdapter（处理器适配器）→ 调用 Controller 方法
    │
    ▼
Controller → Service → Repository → 返回 ModelAndView / @ResponseBody
    │
    ▼
ViewResolver（视图解析器）→ JSP/Thymeleaf / 或 MessageConverter（JSON）
    │
    ▼
HTTP Response
```

### 3.2 核心注解

| 注解 | 说明 |
|------|------|
| `@Controller` | 标记 MVC 控制器 |
| `@RestController` | = @Controller + @ResponseBody |
| `@RequestMapping` | 映射 URL 路径 |
| `@GetMapping/PutMapping/PostMapping/DeleteMapping` | HTTP 方法映射 |
| `@PathVariable` | URL 路径参数 |
| `@RequestParam` | URL 查询参数 / 表单参数 |
| `@RequestBody` | 请求体 JSON → Java 对象 |
| `@ResponseBody` | Java 对象 → JSON 响应体 |
| `@ModelAttribute` | 绑定表单数据到 Model |
| `@ExceptionHandler` | 全局/局部异常处理 |
| `@ControllerAdvice` | 全局异常处理 + 数据绑定 |

### 3.3 拦截器 vs 过滤器

| 维度 | 过滤器 Filter | 拦截器 Interceptor |
|------|:----------:|:----------------:|
| **归属** | Servlet 规范 | Spring 框架 |
| **作用范围** | 所有请求（含静态资源） | 仅进入 DispatcherServlet 的请求 |
| **IoC 容器** | 无法注入 Spring Bean | 可以注入 Spring Bean |
| **生命周期** | init → doFilter → destroy | preHandle → postHandle → afterCompletion |
| **使用场景** | 编码、跨域、安全过滤 | 权限验证、日志、性能监控 |

---

## 4. Spring Cloud 微服务

### 4.1 核心组件一览

```text
Spring Cloud 微服务架构全景：

  ┌─────────────────────────────────────────────────────┐
  │                     网关层                          │
  │   Spring Cloud Gateway / Zuul                       │
  │   路由、限流、鉴权、日志、跨域                       │
  └──────────────────────┬──────────────────────────────┘
                         │
  ┌──────────────────────┴──────────────────────────────┐
  │                  服务治理层                          │
  │   注册中心：Nacos / Eureka / Consul                 │
  │   配置中心：Nacos / Apollo / Spring Cloud Config     │
  │   远程调用：OpenFeign / Dubbo / gRPC                │
  │   负载均衡：Spring Cloud LoadBalancer / Ribbon       │
  └──────────────────────┬──────────────────────────────┘
                         │
  ┌──────────────────────┴──────────────────────────────┐
  │                  服务保护层                          │
  │   熔断降级：Sentinel / Resilience4j / Hystrix(停更)  │
  │   分布式事务：Seata                                 │
  │   链路追踪：Micrometer + Zipkin / SkyWalking         │
  └──────────────────────┬──────────────────────────────┘
                         │
  ┌──────────────────────┴──────────────────────────────┐
  │                    业务服务层                         │
  │   订单服务 | 用户服务 | 商品服务 | 支付服务...         │
  │   Spring Boot + MyBatis-Plus + MySQL + Redis         │
  └─────────────────────────────────────────────────────┘
```

### 4.2 核心组件详解

| 组件 | 功能 | 替代品 |
|------|------|--------|
| **Nacos** | 注册中心 + 配置中心 | Eureka + Config |
| **Gateway** | API 网关，基于 WebFlux | Zuul(停更) / Kong / APISIX |
| **OpenFeign** | 声明式 HTTP 客户端（RPC调用像本地方法） | RestTemplate / WebClient |
| **Sentinel** | 流量控制、熔断降级、系统保护 | Hystrix(停更) / Resilience4j |
| **Seata** | 分布式事务（AT/TCC/Saga/XA） | RocketMQ 事务消息 |
| **Sleuth + Zipkin** | 分布式链路追踪(已迁移到Micrometer) | SkyWalking / Jaeger |

---

## 5. Spring Security

### 5.1 认证与授权

```java
// 安全配置（Spring Security 6+ Lambda 风格）
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())  // JWT 认证
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );
        return http.build();
    }
}
```

### 5.2 安全核心概念

| 概念 | 说明 |
|------|------|
| **Authentication** | 你是谁（认证）—— 用户名密码 / JWT / OAuth2 |
| **Authorization** | 你能做什么（授权）—— 角色/权限校验 |
| **Principal** | 当前登录用户信息 |
| **GrantedAuthority** | 权限（ROLE_ADMIN, READ_PRIVILEGE 等） |
| **SecurityContext** | 安全上下文，持有当前用户 Authentication |
| **SecurityContextHolder** | 存储 SecurityContext（默认 ThreadLocal） |
| **FilterChain** | 过滤器链，每个过滤器完成一个安全职责 |

### 5.3 OAuth2 四种模式

| 模式 | 适用场景 | 说明 |
|------|---------|------|
| **授权码模式** | 有后端的前端应用 | 最安全，通过授权码交换 token |
| **密码模式** | 自家应用（已废弃） | 用户名密码直接换 token |
| **客户端模式** | 服务间调用 | client_id + client_secret 换 token |
| **简化模式** | 纯前端（已废弃） | token 暴露在 URL 中，不安全 |

---

## 6. Spring 事务管理

### 6.1 声明式事务

```java
// @Transactional 的 7 个传播行为
@Transactional(propagation = Propagation.REQUIRED)       // 有则加入，无则新建（默认）
@Transactional(propagation = Propagation.REQUIRES_NEW)   // 总是新建，挂起当前
@Transactional(propagation = Propagation.SUPPORTS)       // 有则加入，无则非事务
@Transactional(propagation = Propagation.NOT_SUPPORTED)  // 非事务执行，挂起当前
@Transactional(propagation = Propagation.MANDATORY)      // 必须有事务，否则抛异常
@Transactional(propagation = Propagation.NEVER)          // 必须无事务，否则抛异常
@Transactional(propagation = Propagation.NESTED)         // 嵌套事务（savepoint）

// 常用配置
@Transactional(
    propagation = Propagation.REQUIRED,
    isolation = Isolation.READ_COMMITTED,  // 隔离级别
    timeout = 30,                           // 超时秒数
    readOnly = false,                       // 只读事务（优化用）
    rollbackFor = Exception.class           // 回滚的异常类型
)
```

### 6.2 事务失效的 8 大场景

```text
@Transactional 失效场景：
  1. 方法非 public（CGLIB 代理无法拦截）
  2. 同类方法调用（this.method() 不走代理）
  3. 异常被 catch 不抛出（默认只回滚 RuntimeException + Error）
  4. rollbackFor 设置错误（抛了 checked exception 但没配置）
  5. 数据库引擎不支持事务（MyISAM）
  6. 多线程（子线程中事务独立）
  7. @Transactional 加在非 Spring Bean 上
  8. propagation 配置不当（如 NEVER 时已存在事务）
```

---

## 7. Spring 扩展点

| 扩展点 | 时机 | 用途 |
|--------|------|------|
| **BeanFactoryPostProcessor** | Bean 实例化前 | 修改 BeanDefinition，如 PropertyPlaceholderConfigurer |
| **BeanPostProcessor** | Bean 初始化前后 | AOP 代理、依赖注入就是通过它实现 |
| **ApplicationListener** | 监听事件 | 容器刷新后的回调 |
| **InitializingBean** | afterPropertiesSet() | Bean 初始化后立即执行 |
| **FactoryBean** | 复杂 Bean 创建 | MyBatis Mapper 代理就是 FactoryBean |
| **ImportBeanDefinitionRegistrar** | 动态注册 Bean | @MapperScan 实现原理 |
| **ApplicationRunner / CommandLineRunner** | 应用启动完成 | 初始化数据、预热缓存 |

```java
// MyBatis 整合 Spring 的核心原理：
// MapperScannerConfigurer 通过 BeanDefinitionRegistryPostProcessor
// 扫描接口，为每个 Mapper 接口注册一个 MapperFactoryBean
// MapperFactoryBean.getObject() 返回 JDK 动态代理 → 这就是为什么
// 你只需要写接口，MyBatis 帮你在运行时生成实现类！
```

---

> 🎯 **一句话总结**：Spring 生态的核心逻辑是 **IoC 容器管理 Bean → AOP 横切增强 → 自动配置简化开发 → 微服务组件化治理**。面试重点：循环依赖、事务失效、自动配置原理、Bean 生命周期。

---

**下一模块**：[04-数据库与持久化名词剖析](./04-数据库与持久化名词剖析.md) → MySQL 索引、事务、分库分表

---

*创建于：2026年7月*
