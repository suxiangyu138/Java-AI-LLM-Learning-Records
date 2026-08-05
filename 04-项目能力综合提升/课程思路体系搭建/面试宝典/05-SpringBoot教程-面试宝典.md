# Spring Boot 面试宝典
> 基于课程大纲全面覆盖 Spring Boot 面试高频考点，从自动配置原理到项目实战

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

### 1.1 什么是 Spring Boot？
Spring Boot 是 Spring 框架的**快速开发脚手架**，它通过**自动配置**和**Starter 依赖管理**大幅简化了 Spring 应用的搭建和开发。核心目标："Convention Over Configuration" —— 约定优于配置。

### 1.2 Spring Boot 的核心特性有哪些？
| 特性 | 说明 |
|------|------|
| Starter 依赖管理 | 一键导入场景依赖，无需手动管理版本 |
| 自动配置 | 根据 Classpath 自动配置 Bean |
| 内嵌 Web 服务器 | 内嵌 Tomcat/Jetty/Undertow |
| Actuator | 生产级监控和管理端点 |
| 外部化配置 | 支持 properties/yaml/环境变量/命令行参数 |
| 无代码生成 | 无需 XML 配置，纯注解驱动 |

### 1.3 @SpringBootApplication 包含哪些注解？
```java
@SpringBootApplication // 复合注解，等效于：
@SpringBootConfiguration   // 标记为配置类
@EnableAutoConfiguration   // 开启自动配置（核心）
@ComponentScan             // 扫描当前包及子包的组件

// 其中 @EnableAutoConfiguration 又包含：
@Import(AutoConfigurationImportSelector.class)
```

### 1.4 Spring Boot 自动配置的原理是什么？
**自动配置的核心**：`@EnableAutoConfiguration` 通过 `AutoConfigurationImportSelector` 加载 `META-INF/spring.factories` 中所有 `EnableAutoConfiguration` 配置类，然后根据 `@Conditional` 系列注解**条件装配**。

```java
// spring.factories 文件内容示例
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration,\
org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
// ... 上百个自动配置类
```

> 🎯 面试亮点：Spring Boot 2.7+ 开始使用 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 替代 `spring.factories`。

### 1.5 @Conditional 系列注解有哪些？
| 注解 | 条件 |
|------|------|
| `@ConditionalOnClass` | Classpath 中存在指定类 |
| `@ConditionalOnMissingClass` | Classpath 中不存在指定类 |
| `@ConditionalOnBean` | 容器中存在指定 Bean |
| `@ConditionalOnMissingBean` | 容器中不存在指定 Bean |
| `@ConditionalOnProperty` | 配置文件中存在指定属性 |
| `@ConditionalOnResource` | 存在指定资源文件 |
| `@ConditionalOnWebApplication` | 当前是 Web 应用 |
| `@ConditionalOnExpression` | SpEL 表达式为 true |
| `@ConditionalOnJava` | Java 版本匹配 |

### 1.6 Spring Boot Starter 机制是什么？
Starter 是一组**聚合依赖描述**，通过引入一个 Starter 即可获得完整的场景依赖。例如引入 `spring-boot-starter-web` 会自动引入 Tomcat、SpringMVC、Jackson 等依赖，同时触发 `WebMvcAutoConfiguration` 进行自动配置。

**自定义 Starter 步骤**：
1. 创建 autoconfigure 模块：包含自动配置类
2. 创建 starter 模块：引入 autoconfigure 模块（空 Jar，只做依赖传递）
3. 在 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册自动配置类
4. 配置属性使用 `@ConfigurationProperties` 绑定

### 1.7 Spring Boot 配置文件加载优先级是什么？
```
命令行参数 > --spring.config.location > java:comp/env > application-{profile}.properties
> application-{profile}.yml > application.properties > application.yml
> @PropertySource
```

**内部配置加载顺序（从高到低）**：
1. `file:./config/`（当前项目下的 config 目录）
2. `file:./`（当前项目根目录）
3. `classpath:/config/`（classpath 下的 config 目录）
4. `classpath:/`（classpath 根目录）

### 1.8 application.properties 和 application.yml 的区别？
| 特性 | properties | yaml |
|------|-----------|------|
| 格式 | 键值对（扁平化）| 层次结构（缩进）|
| 可读性 | 重复前缀难阅读 | 清晰，无重复 |
| 序列化支持 | 不支持 | 支持 List、Map 等 |
| 编码 | 默认 ISO 8859-1 | UTF-8 |

```yaml
# YAML 示例
server:
  port: 8080
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db
    username: root
    password: 123456
```

### 1.9 @ConfigurationProperties 和 @Value 的区别？
| 特性 | @ConfigurationProperties | @Value |
|------|------------------------|--------|
| 绑定方式 | 批量绑定到对象（松散绑定）| 单个值绑定 |
| SpEL 支持 | 不支持 | 支持 |
| JSR-303 校验 | 支持（@Validated）| 不支持 |
| 复杂类型 | 支持 List、Map 等 | 仅支持简单类型 |
| 使用场景 | 配置属性组 | 单个配置项 |

```java
@Component
@ConfigurationProperties(prefix = "app.datasource")
@Validated
public class DataSourceProperties {
    @NotEmpty(message = "url不能为空")
    private String url;
    private String username;
    private String password;
    private List<String> schemas;
    private Map<String, String> properties;
    // getter/setter...
}
```

### 1.10 Spring Boot 内嵌 Tomcat 是如何工作的？
Spring Boot 通过引入 `spring-boot-starter-tomcat` 实现内嵌 Tomcat。`TomcatServletWebServerFactory` 负责创建 `Tomcat` 实例，配置端口、协议等。核心代码在 `ServletWebServerApplicationContext` 中通过 `createWebServer()` 启动。

```java
// 切换为 Jetty
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jetty</artifactId>
</dependency>
```

### 1.11 Spring Boot Actuator 的作用是什么？
Actuator 提供生产级的**监控和管理端点**，包括：
- `/health`：健康检查
- `/info`：应用信息
- `/metrics`：指标（JVM 内存、GC、请求计数等）
- `/env`：环境配置
- `/loggers`：日志级别管理
- `/beans`：容器中所有 Bean
- `/mappings`：请求映射
- `/scheduledtasks`：定时任务

### 1.12 Spring Boot 的启动流程是怎样的？
```
1. 创建 SpringApplication 实例
   1.1 推断 Web 应用类型（Reactive/Servlet/None）
   1.2 加载 META-INF/spring.factories 中的 ApplicationContextInitializer
   1.3 加载 META-INF/spring.factories 中的 ApplicationListener
   1.4 推断主启动类
2. 执行 SpringApplication.run()
   2.1 启动计时器
   2.2 设置 java.awt.headless
   2.3 获取并启动 SpringApplicationRunListener（事件发布）
   2.4 准备 Environment（加载配置）
   2.5 创建 ApplicationContext（AnnotationConfigServletWebServerApplicationContext）
   2.6 准备 BeanFactory
   2.7 刷新容器（refresh）—— 核心步骤
   2.8 刷新后处理
   2.9 结束计时器，发布 ApplicationReadyEvent
3. 返回 ApplicationContext
```

### 1.13 Spring Boot 支持哪些 Profile 多环境配置？
```yaml
# application.yml（主配置）
spring:
  profiles:
    active: dev

# application-dev.yml
server:
  port: 8080

# application-prod.yml
server:
  port: 80
  ssl:
    enabled: true
```

### 1.14 什么是 Spring Boot 的 Fat Jar？
Fat Jar（可执行 Jar）是将应用代码和所有依赖打包成一个 Jar 文件。Spring Boot 的 Maven/Gradle 插件在打包时使用特定的 Jar 格式（BOOT-INF 结构），通过自定义的 `JarLauncher` 启动，避免依赖冲突。

```
my-app.jar
├── BOOT-INF/
│   ├── classes/          # 应用代码
│   └── lib/              # 所有依赖 Jar
├── META-INF/
│   └── MANIFEST.MF       # Main-Class: JarLauncher
└── org.springframework.boot.loader/
    ├── JarLauncher.class
    └── Launcher.class
```

### 1.15 Spring Boot 如何实现热部署？
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <optional>true</optional>
</dependency>
```
DevTools 原理：使用**双 ClassLoader** 机制——基础类库用 Base ClassLoader 加载，应用类用 Restart ClassLoader。当检测到文件变更时，丢弃旧 ClassLoader 创建新的，实现快速重启。此外还支持 LiveReload 自动刷新浏览器。

---

## 二、深度原理剖析（10-15题）

### 2.1 @EnableAutoConfiguration 源码分析
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@AutoConfigurationPackage
@Import(AutoConfigurationImportSelector.class)
public @interface EnableAutoConfiguration {
    String ENABLED_OVERRIDE_PROPERTY = "spring.boot.enableautoconfiguration";
    Class<?>[] exclude() default {};
    String[] excludeName() default {};
}
```

核心是 `AutoConfigurationImportSelector` 实现了 `ImportSelector` 和 `DeferredImportSelector`，其 `selectImports()` 方法：

```java
public String[] selectImports(AnnotationMetadata annotationMetadata) {
    // 1. 检查是否开启自动配置
    if (!isEnabled(annotationMetadata)) return new String[0];
    
    // 2. 加载 AutoConfiguration.imports 中的配置类
    AutoConfigurationEntry autoConfigurationEntry = getAutoConfigurationEntry(annotationMetadata);
    return StringUtils.toStringArray(autoConfigurationEntry.getConfigurations());
}

// 3. 配置过滤（使用 ON_CLASS 条件过滤不合条件的配置）
ConfigurationClassParser.processDeferredImportSelectors()
```

### 2.2 Spring Boot 自动配置条件装配实现原理
`AutoConfigurationImportSelector` 加载完配置类后，通过 `ConfigurationClassParser` 解析 `@Conditional` 注解。`ConditionEvaluator` 负责评估条件，核心是 `Condition` 接口的 `matches()` 方法。

```java
// OnClassCondition 实现原理
public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    // 读取 @ConditionalOnClass 的 value
    MultiValueMap<String, Object> attrs = metadata.getAllAnnotationAttributes(
            ConditionalOnClass.class.getName());
    String[] classNames = (String[]) attrs.getFirst("value");
    for (String className : classNames) {
        if (!ClassUtils.isPresent(className, context.getClassLoader())) {
            return false; // classpath 不存在则返回 false
        }
    }
    return true;
}
```

> 💡 面试亮点：Spring Boot 通过 `spring-autoconfigure-metadata.properties` 进行条件过滤优化，避免加载大量不需要的类。

### 2.3 Spring Boot 的 SpringApplication 构造过程源码分析
```java
public SpringApplication(ResourceLoader resourceLoader, Class<?>... primarySources) {
    this.resourceLoader = resourceLoader;
    Assert.notNull(primarySources, "PrimarySources must not be null");
    this.primarySources = new LinkedHashSet<>(Arrays.asList(primarySources));
    
    // 1. 推断 Web 应用类型
    this.webApplicationType = WebApplicationType.deduceFromClasspath();
    
    // 2. 加载 ApplicationContextInitializer（SPI）
    setInitializers((Collection) getSpringFactoriesInstances(
            ApplicationContextInitializer.class));
    
    // 3. 加载 ApplicationListener（SPI）
    setListeners((Collection) getSpringFactoriesInstances(ApplicationListener.class));
    
    // 4. 推断主启动类
    this.mainApplicationClass = deduceMainApplicationClass();
}
```

### 2.4 Spring Boot run() 方法的核心步骤
```java
public ConfigurableApplicationContext run(String... args) {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    
    // 1. 创建引导上下文
    DefaultBootstrapContext bootstrapContext = createBootstrapContext();
    
    // 2. 配置 headless 属性
    configureHeadlessProperty();
    
    // 3. 获取 SpringApplicationRunListeners（事件监听器）
    SpringApplicationRunListeners listeners = getRunListeners(args);
    listeners.starting(bootstrapContext); // 发布 ApplicationStartingEvent
    
    try {
        // 4. 准备命令行参数
        ApplicationArguments applicationArguments = new DefaultApplicationArguments(args);
        
        // 5. 准备 Environment（加载配置）
        ConfigurableEnvironment environment = prepareEnvironment(listeners, bootstrapContext, applicationArguments);
        configureIgnoreBeanInfo(environment);
        
        // 6. 打印 Banner
        printBanner(environment);
        
        // 7. 创建 ApplicationContext
        context = createApplicationContext();
        context.setApplicationStartup(this.applicationStartup);
        
        // 8. 准备上下文（初始化器、注册单例 Bean）
        prepareContext(bootstrapContext, context, environment, listeners, applicationArguments, printedBanner);
        
        // 9. 刷新容器（最核心步骤 —— 调用 AbstractApplicationContext.refresh()）
        refreshContext(context);
        
        // 10. 刷新后处理
        afterRefresh(context, applicationArguments);
        
        stopWatch.stop();
        // 11. 发布 ApplicationReadyEvent
        listeners.started(context);
        
        // 12. 调用 ApplicationRunner 和 CommandLineRunner
        callRunners(context, applicationArguments);
    } catch (Throwable ex) {
        // 异常处理，发布 ApplicationFailedEvent
        handleRunFailure(context, ex, listeners);
        throw new IllegalStateException(ex);
    }
    return context;
}
```

### 2.5 Spring Boot 内嵌 Tomcat 如何与 Spring 整合？
`ServletWebServerApplicationContext` 在 `refresh()` 时调用 `onRefresh()` 方法，触发 `createWebServer()`：

```java
// ServletWebServerApplicationContext 关键逻辑
private void createWebServer() {
    // 1. 获取 ServletWebServerFactory（TomcatServletWebServerFactory）
    ServletWebServerFactory factory = getWebServerFactory();
    
    // 2. 获取 ServletContext 初始化器
    SelfInitializerServletContextInitializer initializer = getSelfInitializer();
    
    // 3. 创建 WebServer（这里启动 Tomcat）
    this.webServer = factory.getWebServer(initializer);
    
    // 4. 将 DispatcherServlet 注册到 Tomcat
    // TomcatServletWebServerFactory.getWebServer() 内部：
    //   - 创建 Tomcat 实例
    //   - 设置端口、Host 等
    //   - 添加 Context
    //   - 通过 TomcatStarter 初始化 ServletContainerInitializer
    //   - 最终 DispatcherServlet 被注册到 Servlet 容器
}
```

### 2.6 Spring Boot 配置加载的底层机制
Spring Boot 使用 `PropertySourceLoader` 加载配置文件，主要有两个实现：
- `PropertiesPropertySourceLoader`：加载 `.properties` 文件
- `YamlPropertySourceLoader`：加载 `.yml` / `.yaml` 文件

加载顺序由 `ConfigFileApplicationListener`（2.4 之前）或 `ConfigDataEnvironmentPostProcessor`（2.4+）控制。配置通过 `Environment` 抽象暴露：

```java
// 配置属性绑定原理
// ConfigurationPropertiesBindingPostProcessor 负责绑定
// RelaxedPropertyResolver 支持松散绑定（kebab-case、camelCase 互转）

user-name → userName (kebab-case → camelCase)
user_name → userName (underscore → camelCase)
USER_NAME → userName (uppercase → camelCase)
userName → 直接匹配
```

### 2.7 Spring Boot 的 @EnableAutoConfiguration 排除和覆盖机制
```java
// 方式一：exclude 排除
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})

// 方式二：配置排除
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration

// 方式三：条件覆盖
// 自定义 Bean 并使用 @ConditionalOnMissingBean，优先级高于自动配置
@Bean
@ConditionalOnMissingBean(DataSource.class)
public DataSource customDataSource() {
    return new HikariDataSource();
}

// 方式四：通过 @AutoConfigureBefore/@AutoConfigureAfter 调整顺序
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
@Configuration
public class MyDataSourceConfig { }
```

### 2.8 Spring Boot 2.x 和 3.x 的主要区别
| 特性 | Spring Boot 2.x | Spring Boot 3.x |
|------|----------------|----------------|
| Java 基线 | Java 8+ | Java 17+ |
| Spring 版本 | Spring 5 | Spring 6 |
| 底层框架 | Jakarta EE 8 | Jakarta EE 9+（javax → jakarta）|
| GraalVM | 实验性 | 原生编译正式支持 |
| Micrometer | 1.x | 1.10+（新 API）|
| AOT 编译 | 不支持 | 支持 Ahead-Of-Time 编译 |
| Observed 注解 | 无 | 新增 @Observed |

### 2.9 Spring Boot 的指标监控 Micrometer 原理
Micrometer 是 Spring Boot 的指标门面库，类似 SLF4J 之于日志。核心概念：
- **Meter**：指标（Counter、Gauge、Timer、DistributionSummary）
- **MeterRegistry**：注册中心（SimpleMeterRegistry、PrometheusMeterRegistry）
- **MeterBinder**：自动绑定指标

```java
// 自定义指标
@Component
public class CustomMetrics {
    private final Counter orderCounter;
    
    public CustomMetrics(MeterRegistry registry) {
        this.orderCounter = Counter.builder("order.created")
                .description("创建订单数量")
                .tag("region", "cn")
                .register(registry);
    }
    
    public void orderCreated() {
        orderCounter.increment();
    }
}
```

### 2.10 Spring Boot 的优雅关闭机制
```yaml
# application.yml
server:
  shutdown: graceful  # 开启优雅关闭（Spring Boot 2.3+）

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s  # 最大等待时间
```

原理：注册 `SmartLifecycle` 接口，在收到关闭信号时停止接受新请求，处理完已接收请求后再关闭。Tomcat 底层通过 `Connector.pause()` 暂停请求接收。

---

## 三、实战场景题（8-12题）

### 3.1 Spring Boot 项目如何实现统一异常处理？
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNotFound(ResourceNotFoundException e) {
        return Result.error(404, e.getMessage());
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return Result.error(400, msg);
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error(500, "服务器内部错误");
    }
}
```

### 3.2 Spring Boot 如何配置跨域？
```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

### 3.3 如何自定义 Spring Boot Starter？
```java
// 1. 自动配置类
@Configuration
@ConditionalOnClass(RedisClient.class)
@EnableConfigurationProperties(RedisClientProperties.class)
public class RedisClientAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public RedisClient redisClient(RedisClientProperties properties) {
        return new RedisClient(properties.getHost(), properties.getPort());
    }
}

// 2. 属性绑定
@ConfigurationProperties(prefix = "my.redis")
public class RedisClientProperties {
    private String host = "localhost";
    private int port = 6379;
    private int timeout = 3000;
    // getter/setter...
}

// 3. 注册（META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports）
// my.redis.RedisClientAutoConfiguration
```

### 3.4 Spring Boot 如何整合 MyBatis 并配置多数据源？
```java
@Configuration
public class MultiDataSourceConfig {
    
    @Primary
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.master")
    public DataSource masterDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.slave")
    public DataSource slaveDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Primary
    @Bean
    public SqlSessionFactory masterSqlSessionFactory(@Qualifier("masterDataSource") DataSource ds) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(ds);
        bean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath:mapper/master/**/*.xml"));
        return bean.getObject();
    }
    
    @Bean
    public SqlSessionFactory slaveSqlSessionFactory(@Qualifier("slaveDataSource") DataSource ds) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(ds);
        bean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath:mapper/slave/**/*.xml"));
        return bean.getObject();
    }
}
```

### 3.5 Spring Boot 如何实现分布式定时任务调度？
```java
@Configuration
@EnableScheduling
public class ScheduleConfig implements SchedulingConfigurer {
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("scheduled-task-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.initialize();
        taskRegistrar.setTaskScheduler(scheduler);
    }
}

@Component
public class BusinessTask {
    
    @Scheduled(cron = "0 0/5 * * * ?") // 每 5 分钟执行
    public void syncData() {
        // 分布式环境需加分布式锁防止重复执行
        // 使用 Redis SETNX + 过期时间
    }
}
```

### 3.6 Spring Boot 如何实现接口幂等性？
```java
// 自定义注解
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    String key();
    long ttl() default 5000; // 幂等有效期
}

// AOP 实现
@Aspect
@Component
public class IdempotentAspect {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint pjp, Idempotent idempotent) throws Throwable {
        // 从请求头中获取幂等 Key
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes()).getRequest();
        String idempotentKey = request.getHeader("Idempotent-Key");
        if (StringUtils.isEmpty(idempotentKey)) {
            throw new IllegalArgumentException("缺少幂等 Key");
        }
        
        // 尝试设置到 Redis
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "1", idempotent.ttl(), TimeUnit.MILLISECONDS);
        if (Boolean.FALSE.equals(success)) {
            // 重复请求
            return Result.error("重复请求，请勿重试");
        }
        return pjp.proceed();
    }
}
```

### 3.7 Spring Boot 性能优化有哪些手段？
| 优化方向 | 具体措施 |
|---------|---------|
| 启动速度 | 延迟初始化 `spring.main.lazy-initialization=true` |
| 启动速度 | 使用 Spring Boot 3.x AOT 编译 |
| 启动速度 | 排除不必要的自动配置 `@SpringBootApplication(exclude = {...})` |
| 运行性能 | 内嵌 Tomcat 参数调优（maxThreads、maxConnections、acceptCount）|
| 运行性能 | 使用 Undertow 替代 Tomcat（高并发场景更优）|
| 内存 | JVM 参数调优（堆大小、GC 策略）|
| 内存 | 减少自动配置加载的组件 |
| 监控 | 开启 Actuator 监控慢请求、GC |

### 3.8 Spring Boot 整合 Spring Security 实现 JWT 认证
```java
// JWT 认证过滤器
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null && validateToken(token)) {
            String username = getUsernameFromToken(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }
}

// 安全配置
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeHttpRequests()
                .antMatchers("/api/auth/**").permitAll()
                .antMatchers("/api/**").authenticated()
            .and()
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

---

## 四、手写代码题（5-8题）

### 4.1 手写 Spring Boot 启动类
```java
@SpringBootApplication  // 包含 @Configuration + @EnableAutoConfiguration + @ComponentScan
public class Application {
    
    public static void main(String[] args) {
        // 启动 Spring Boot 应用
        ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);
        
        // 获取所有注册的 Bean 名称
        String[] beanNames = context.getBeanDefinitionNames();
        System.out.println("共加载 " + beanNames.length + " 个 Bean");
    }
}
```

### 4.2 手写 RestTemplate 封装类
```java
@Component
public class HttpClient {
    private final RestTemplate restTemplate;
    
    public HttpClient(RestTemplateBuilder builder) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }
    
    public <T, R> R post(String url, T request, Class<R> responseType) {
        return restTemplate.postForObject(url, request, responseType);
    }
    
    public <R> R get(String url, Class<R> responseType, Object... uriVariables) {
        return restTemplate.getForObject(url, responseType, uriVariables);
    }
    
    public <R> ResponseEntity<R> getWithHeaders(String url, Class<R> responseType, Object... vars) {
        return restTemplate.getForEntity(url, responseType, vars);
    }
}
```

### 4.3 手写 Spring Boot 健康检查端点
```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    @Autowired
    private DataSource dataSource;
    
    @Override
    public Health health() {
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(2000)) {
                return Health.up()
                        .withDetail("database", "MySQL")
                        .withDetail("status", "connected")
                        .build();
            } else {
                return Health.down()
                        .withDetail("error", "数据库连接无效")
                        .build();
            }
        } catch (Exception e) {
            return Health.down(e)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
```

### 4.4 手写 Spring Boot 配置属性绑定类
```java
@Component
@ConfigurationProperties(prefix = "app.oss")
@Validated
public class OssProperties {
    
    @NotEmpty(message = "endpoint 不能为空")
    private String endpoint;
    
    @NotEmpty
    private String accessKeyId;
    
    @NotEmpty
    private String accessKeySecret;
    
    @NotEmpty
    private String bucketName;
    
    private String region = "cn-hangzhou";
    
    // 内部类也支持绑定
    private Retry retry = new Retry();
    
    // getter/setter...
    
    public static class Retry {
        private int maxAttempts = 3;
        private long backoffDelay = 1000L;
        // getter/setter...
    }
}
```

### 4.5 手写 Spring Boot 拦截器（登录校验）
```java
@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // 从请求头获取 Token
        String token = request.getHeader("Authorization");
        if (StringUtils.isEmpty(token)) {
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write(JSON.toJSONString(Result.error(401, "未登录")));
            return false;
        }
        // 校验 Token...
        return true;
    }
    
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) {
        // 后置处理
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 清理资源
    }
}

// 注册拦截器
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private LoginInterceptor loginInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**", "/api/public/**");
    }
}
```

### 4.6 手写 Spring Boot 定时任务（注解 + 接口两种方式）
```java
@Component
public class OrderCleanupTask {
    
    // 方式一：注解方式
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨 2 点执行
    @Scheduled(fixedRate = 3600000)   // 每小时执行一次
    public void cleanExpiredOrders() {
        log.info("开始清理过期订单...");
        orderMapper.deleteExpiredOrders(LocalDateTime.now().minusDays(30));
    }
}

// 方式二：接口方式（动态注册）
@Component
public class DynamicScheduledTask implements SchedulingConfigurer {
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(
            () -> System.out.println("动态任务执行: " + LocalDateTime.now()),
            triggerContext -> {
                // 动态计算下次执行时间
                String cron = dynamicCronExpression();
                return new CronTrigger(cron).nextExecutionTime(triggerContext);
            }
        );
    }
}
```

---

## 五、系统设计题（3-5题）

### 5.1 设计一个 Spring Boot 项目如何支持灰度发布？
```java
// 1. 灰度标识注解
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GrayRelease {
    String key() default "default";
    int percentage() default 0; // 灰度百分比
}

// 2. 灰度路由 AOP
@Aspect
@Component
public class GrayReleaseAspect {
    @Around("@annotation(gray)")
    public Object route(ProceedingJoinPoint pjp, GrayRelease gray) throws Throwable {
        String userId = RequestContextHolder.getCurrentUser(); // 用户标识
        int hash = Math.abs(userId.hashCode()) % 100;
        if (hash < gray.percentage()) {
            // 灰度版本
            return handleGrayVersion(pjp);
        } else {
            // 稳定版本
            return pjp.proceed();
        }
    }
}
```

### 5.2 设计一个 Spring Boot 项目的统一配置中心
```java
// 配置动态刷新机制
@Component
public class DynamicConfigRefresher {
    @Autowired
    private ContextRefresher contextRefresher;
    
    @Scheduled(fixedDelay = 30000) // 每 30 秒检查配置变化
    public void checkConfigChanges() {
        // 1. 拉取最新配置（从配置中心）
        Map<String, Object> newConfigs = fetchConfigFromCenter();
        
        // 2. 对比旧配置
        Map<String, Object> oldConfigs = EnvironmentReader.getAllProperties(environment);
        
        if (hasChanges(oldConfigs, newConfigs)) {
            // 3. 刷新上下文（重新绑定 @ConfigurationProperties）
            Set<String> keys = contextRefresher.refresh();
            log.info("配置已刷新，变更 Key: {}", keys);
        }
    }
}
```

### 5.3 设计一个基于 Spring Boot 的 API 网关限流
```java
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private final RateLimiter rateLimiter = RateLimiter.create(100); // 每秒 100 个请求
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String ip = getClientIp(request);
        String key = "rate_limit:" + ip;
        
        // 使用 Redis 滑动窗口限流
        String script = """
            local key = KEYS[1]
            local limit = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local current = redis.call('INCR', key)
            if current == 1 then
                redis.call('EXPIRE', key, window)
            end
            if current > limit then
                return 0
            end
            return 1
        """;
        
        Long result = redisTemplate.execute(
                new DefaultRedisScript<>(script, Long.class),
                Arrays.asList(key), "100", "1");
        
        if (result == null || result == 0L) {
            response.setStatus(429);
            response.getWriter().write("请求过于频繁，请稍后再试");
            return false;
        }
        return true;
    }
}
```

---

## 六、常见坑点与最佳实践（表格）

| 坑点 | 问题描述 | 解决方案 | 最佳实践 |
|------|---------|---------|---------|
| @Value 注入失败 | 静态字段或非受管 Bean 中使用 @Value | 使用 setter 注入或 Spring Expression 替换 | 用 @ConfigurationProperties 批量绑定 |
| 循环依赖导致启动失败 | Spring Boot 2.6+ 默认禁止循环依赖 | 设置 `spring.main.allow-circular-references=true` 或重构 | 重构代码消除循环依赖 |
| 配置文件覆盖优先级混淆 | 多个配置源导致属性值不符合预期 | 明确定义配置优先级顺序 | 统一使用 application.yml 并配合 profile |
| 自动配置冲突 | 多个自动配置类尝试创建同类型 Bean | 使用 `exclude` 或用 `@ConditionalOnMissingBean` 控制 | 理解自动配置条件，按需排除 |
| 内嵌 Tomcat 端口被占用 | 端口冲突导致启动失败 | 随机端口 `server.port=0` | 配置 `server.port` 为明确值 |
| DevTools 导致 ClassCastException | 双 ClassLoader 导致类型不匹配 | 排除 devtools 或在部署时删除 | 只在开发环境使用 devtools |
| Actuator 端点暴露过多信息 | 生产环境暴露敏感配置 | 只暴露必要端点并配置安全限制 | `management.endpoints.web.exposure.include=health,info` |
| WebMvcConfigurer 冲突 | 多个自定义配置导致覆盖 | 明确 `@Order` 或合并配置 | 使用 `WebMvcConfigurer` 而非继承 `WebMvcConfigurationSupport` |
| 内嵌 Tomcat 的 Request Body 不可重复读 | Filter 中读取后 Controller 获取不到 | 使用 `ContentCachingRequestWrapper` | 用 Spring 自带的 `CommonsRequestLoggingFilter` |
| Nacos/Apollo 配置不刷新 | @Value 注入的字段不会自动刷新 | 改用 `@ConfigurationProperties` 配合 `@RefreshScope` | 尽量使用 `@ConfigurationProperties` 绑定配置 |

---

## 七、面试回答模板（Top 5高频题的结构化回答模板）

### 7.1 Spring Boot 自动配置原理
**回答结构**（注解 → 文件 → 条件装配）：
1. `@EnableAutoConfiguration` → 导入 `AutoConfigurationImportSelector`
2. 读取 `spring.factories`（或 `AutoConfiguration.imports`）中的自动配置类全名
3. 通过 `@ConditionalOnClass`、`@ConditionalOnBean` 等条件过滤
4. 符合条件的配置类被解析注册，生成 Bean
5. **举例**：`DataSourceAutoConfiguration` 在 classpath 有 `DataSource.class` 且没有自定义 `DataSource` Bean 时就创建 `HikariDataSource`

### 7.2 @Conditional 注解的作用和工作原理
1. **作用**：条件装配，满足条件才加载配置
2. **常见类型**：`@ConditionalOnClass`、`@ConditionalOnBean`、`@ConditionalOnProperty`、`@ConditionalOnMissingBean`
3. **原理**：`ConfigurationClassParser` 解析配置类时调用 `ConditionEvaluator.shouldSkip()`，通过 `Condition.matches()` 判断
4. **性能优化**：`spring-autoconfigure-metadata.properties` 提供过滤元数据，避免加载不必要的类

### 7.3 Spring Boot 启动流程
**回答结构**（构造 + run 分两段）：
1. **构造阶段**：推断 Web 类型、加载初始化器、加载监听器
2. **run 方法**：准备 Environment → 创建上下文 → 刷新容器（核心）→ 发布 `ApplicationReadyEvent`
3. **refresh 关键**：BeanFactory 配置 → Bean 定义加载 → BeanPostProcessor 注册 → 实例化单例 Bean → Tomcat 启动

### 7.4 Spring Boot 如何做配置管理？
**分类回答**：
1. **配置文件**：application.yml/properties、Profile 多环境
2. **外部化配置**：命令行参数、环境变量、配置中心（Nacos/Apollo）
3. **@ConfigurationProperties**：批量绑定、JSR-303 校验
4. **优先级**：命令行 > 环境变量 > application-{profile} > application > @PropertySource
5. **动态刷新**：Spring Cloud Bus + RefreshScope 实现配置热更新

### 7.5 Spring Boot 的 Fat Jar 原理
1. **结构**：包含 BOOT-INF/classes（应用代码）和 BOOT-INF/lib（依赖）
2. **启动器**：`JarLauncher` 通过自定义 ClassLoader 加载 BOOT-INF/lib 下所有 Jar
3. **与传统区别**：不会解压，直接读取 Jar 中的 Jar（Nested Jar 支持）
4. **优势**：自包含、可执行、版本锁定

---

## 八、快速查漏补缺 Checklist

- [ ] @SpringBootApplication 组合注解
- [ ] 自动配置原理（EnableAutoConfiguration + spring.factories/AutoConfiguration.imports）
- [ ] @Conditional 系列注解（OnClass/OnBean/OnProperty/OnMissingBean）
- [ ] Starter 机制和自定义 Starter
- [ ] 配置文件加载优先级
- [ ] application.yml vs application.properties
- [ ] @ConfigurationProperties vs @Value
- [ ] 内嵌 Tomcat 启动原理
- [ ] 启动流程（构造 + run 方法各步骤）
- [ ] Profile 多环境配置
- [ ] Actuator 监控端点
- [ ] Fat Jar 结构和启动原理
- [ ] DevTools 热部署原理（双 ClassLoader）
- [ ] 统一异常处理（@RestControllerAdvice）
- [ ] 拦截器（HandlerInterceptor）原理和注册
- [ ] 跨域配置
- [ ] 多数据源配置
- [ ] 优雅关闭（graceful shutdown）
- [ ] Micrometer 指标监控
- [ ] Spring Boot 2.x vs 3.x 差异
- [ ] JWT 认证整合
- [ ] API 限流实现
- [ ] 接口幂等性方案
- [ ] 灰度发布设计
