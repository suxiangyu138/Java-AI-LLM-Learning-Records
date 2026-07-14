# Spring Boot 自动配置与最佳实践

## 前言

Spring Boot 是 Spring 生态中里程碑式的产物，它基于"**约定优于配置**"（Convention Over Configuration）的理念，极大地降低了 Spring 应用开发的门槛。本文将从核心自动配置原理出发，深入剖析 Spring Boot 的底层工作机制，同时覆盖多环境配置、Actuator 监控、异常处理、拦截器与过滤器等生产级实践，最后总结面试高频考点与工程最佳实践，帮助读者建立完整的 Spring Boot 知识体系。

---

## 一、Spring Boot 核心特性

### 1.1 起步依赖（Starter）

起步依赖是 Spring Boot 解决依赖管理痛点的核心手段。传统 Spring 项目中，开发者需要手动协调大量 jar 包的版本（spring-core、spring-webmvc、jackson、tomcat-embed-core 等），稍不注意就会引入版本冲突。Spring Boot 通过 **Starter** 将某一类功能所需的所有依赖打包成一个坐标，开发者只需引入一个 starter 即可。

#### 常见 Starter

| Starter | 功能说明 |
|---|---|
| `spring-boot-starter-web` | 构建 Web 应用，内嵌 Tomcat，包含 Spring MVC + REST 支持 |
| `spring-boot-starter-webflux` | 构建响应式 Web 应用，内嵌 Netty，基于 Spring WebFlux |
| `spring-boot-starter-data-jpa` | Spring Data JPA + Hibernate 的整合 |
| `spring-boot-starter-data-redis` | Redis 客户端（Lettuce/Jedis）整合 |
| `spring-boot-starter-test` | JUnit 5 + Mockito + AssertJ 等测试框架 |
| `spring-boot-starter-security` | Spring Security 安全认证 |
| `spring-boot-starter-actuator` | 生产级监控与管理端点 |

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

只要引入 `spring-boot-starter-web`，Spring MVC、内嵌 Tomcat、Jackson JSON 处理器等就会被自动引入，且版本由 Spring Boot 统一仲裁。

#### 版本仲裁 —— spring-boot-dependencies BOM

`spring-boot-starter-parent` 的 `<parent>` 标签中引用了 `spring-boot-dependencies`，这是一个 BOM（Bill of Materials），集中管理了数百个第三方依赖的版本号。当你声明一个依赖而不指定版本时，Maven 会从该 BOM 中读取对应版本，确保全家桶兼容。

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.5</version>
</parent>
```

如果需要覆盖某个依赖的版本，在 `pom.xml` 的 `<properties>` 中覆盖对应属性即可：

```xml
<properties>
    <jackson.version>2.16.2</jackson.version>
</properties>
```

### 1.2 自动配置（Auto Configuration）

Spring Boot 最引人注目的能力就是**自动配置**。开发者只需引入一个 starter，Spring Boot 就能自动推断出需要配置哪些 Bean，并完成装配。

#### @SpringBootApplication 组合注解

`@SpringBootApplication` 是一个三合一注解：

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootConfiguration        // 其实是 @Configuration
@EnableAutoConfiguration        // 开启自动配置
@ComponentScan(excludeFilters = { ... }) // 组件扫描
public @interface SpringBootApplication {
    // ...
}
```

- **@SpringBootConfiguration**：本质是 `@Configuration`，标志当前类为配置类。
- **@EnableAutoConfiguration**：自动配置的核心开关。
- **@ComponentScan**：启用组件扫描，默认扫描启动类所在包及其子包。

#### @EnableAutoConfiguration 原理

`@EnableAutoConfiguration` 的注解定义中通过 `@Import(AutoConfigurationImportSelector.class)` 导入了一个选择器。

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

`AutoConfigurationImportSelector` 实现了 `DeferredImportSelector` 接口，它的核心逻辑在 `getAutoConfigurationEntry()` 方法中：

```
1. 从 spring.factories 中加载 EnableAutoConfiguration 对应的配置类列表
   - Spring Boot 3.0+ 改用 META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
2. 去重
3. 按 @Conditional 条件过滤
4. 排序（@AutoConfigureBefore / @AutoConfigureAfter / @AutoConfigureOrder）
5. 返回最终需加载的自动配置类
```

`Spring Boot 2.x` 时代的加载方式（仍在 `spring.factories` 中）：

```properties
# META-INF/spring.factories
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration,\
org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration,\
org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
...
```

`Spring Boot 3.x` 引入了新的清单文件 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`，每行一个全限定类名：

```
org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration
org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
```

### 1.3 @Conditional 条件注解族

自动配置类不会全部生效——`AutoConfigurationImportSelector` 返回的配置类还要经过 `@Conditional` 系列注解的条件匹配，只有满足条件的配置才会被生效。这是自动配置"智能"的根源。

| 条件注解 | 作用 |
|---|---|
| `@ConditionalOnClass` | 当类路径中存在指定类时生效 |
| `@ConditionalOnMissingClass` | 当类路径中不存在指定类时生效 |
| `@ConditionalOnBean` | 当容器中存在指定 Bean 时生效 |
| `@ConditionalOnMissingBean` | 当容器中不存在指定 Bean 时生效 |
| `@ConditionalOnProperty` | 当配置属性满足指定条件时生效 |
| `@ConditionalOnWebApplication` | 当应用是 Web 应用时生效 |
| `@ConditionalOnNotWebApplication` | 当应用不是 Web 应用时生效 |
| `@ConditionalOnExpression` | 根据 SpEL 表达式的值决定是否生效 |
| `@ConditionalOnResource` | 当指定资源存在于类路径中时生效 |
| `@ConditionalOnJava` | 根据 Java 版本条件生效 |
| `@ConditionalOnSingleCandidate` | 当指定 Bean 在容器中只有一个候选时生效 |

以 `DataSourceAutoConfiguration` 为例，它只有在类路径中存在 `DataSource.class` 且容器中尚未定义 `DataSource` Bean 时才会生效：

```java
@AutoConfiguration
@ConditionalOnClass({ DataSource.class, EmbeddedDatabaseType.class })
@ConditionalOnMissingBean(type = "io.r2dbc.spi.ConnectionFactory")
@EnableConfigurationProperties(DataSourceProperties.class)
@Import({ DataSourcePoolMetadataProvidersConfiguration.class,
          DataSourceInitializationConfiguration.InitializationSpecificCredentialsDataSourceInitializationConfiguration.class })
public class DataSourceAutoConfiguration {
    // ...
}
```

再如 `HttpEncodingAutoConfiguration`，它通过 `@ConditionalOnProperty` 来控制是否生效：

```java
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ServerProperties.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(CharacterEncodingFilter.class)
@ConditionalOnProperty(prefix = "server.servlet.encoding", value = "enabled", matchIfMissing = true)
public class HttpEncodingAutoConfiguration {
    // ...
}
```

当 `server.servlet.encoding.enabled=false` 时，该配置类不会生效。

#### @ConditionalOnMissingBean —— 自定义覆盖的关键

这是 Spring Boot 预留的"扩展点"。如果自动配置类上标注了 `@ConditionalOnMissingBean(XXX.class)`，那么你在自己的 `@Configuration` 类中手动定义了一个同类型 Bean 后，自动配置的对应 Bean 就会被跳过。这是**自定义覆盖自动配置**的标准手段：

```java
@Configuration
public class MyDataSourceConfig {
    @Bean
    public DataSource dataSource() {
        return new HikariDataSource(); // 自定义数据源，覆盖自动配置
    }
}
```

### 1.4 内嵌 Servlet 容器

Spring Boot 默认使用 **Tomcat** 作为内嵌 Servlet 容器（spring-boot-starter-web 的传递依赖中包含了 tomcat-embed-core）。如果要切换为 Jetty 或 Undertow，只需排除 Tomcat 并引入对应的 starter：

```xml
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

内嵌容器的配置均可在 `application.yml` 中完成：

```yaml
server:
  port: 8080
  servlet:
    context-path: /api
    encoding:
      charset: UTF-8
      enabled: true
  tomcat:
    max-connections: 10000
    threads:
      max: 200
      min-spare: 20
    connection-timeout: 5000
```

---

## 二、多环境配置

### 2.1 配置文件的格式与加载顺序

Spring Boot 支持 `application.properties` 和 `application.yml` 两种格式。YAML 因其层级清晰、支持多文档块而更受推荐。

#### YAML 多文档块

一个 `.yml` 文件中使用 `---` 分隔多个文档块，不同块可以通过 `spring.config.activate.on-profile` 指定激活条件（Spring Boot 2.4+ 语法）：

```yaml
# 默认配置
server:
  port: 8080

---
spring:
  config:
    activate:
      on-profile: dev
server:
  port: 8081

---
spring:
  config:
    activate:
      on-profile: prod
server:
  port: 8080
```

### 2.2 Profile 激活方式

**方式一：application.yml 中指定**

```yaml
spring:
  profiles:
    active: dev
```

**方式二：命令行参数**

```bash
java -jar myapp.jar --spring.profiles.active=prod
```

**方式三：环境变量**

```bash
set SPRING_PROFILES_ACTIVE=dev
# Linux: export SPRING_PROFILES_ACTIVE=dev
```

**方式四：Profile 分组（Spring Boot 2.4+）**

```yaml
spring:
  profiles:
    group:
      "dev": [ "dev-db", "dev-redis", "dev-log" ]
      "prod": [ "prod-db", "prod-redis", "prod-log" ]
```

这样激活 `dev` 分组时，`application-dev-db.yml`、`application-dev-redis.yml`、`application-dev-log.yml` 会被一并加载。

### 2.3 配置优先级（由高到低）

```
1. 命令行参数（--xxx=yyy）
2. JNDI 属性（java:comp/env）
3. 操作系统环境变量
4. application-{profile}.yml（profile-specific 配置）
5. application.yml（默认配置）
6. @PropertySource 加载的自定义配置
```

高优先级配置会覆盖低优先级配置中的相同属性，同时不同来源的配置会**互补合并**。

### 2.4 @Profile 注解

除了配置文件级别，还可以在代码级别使用 `@Profile` 控制 Bean 的注册：

```java
@Component
@Profile("dev")
public class DevDataSourceInitializer implements CommandLineRunner {
    @Override
    public void run(String... args) {
        System.out.println(">>>> 开发环境 - 初始化测试数据...");
    }
}

@Component
@Profile("prod")
public class ProdDataSourceInitializer implements CommandLineRunner {
    @Override
    public void run(String... args) {
        System.out.println(">>>> 生产环境 - 跳过测试数据");
    }
}
```

### 2.5 @Value 与 @ConfigurationProperties

#### @Value 注入（不推荐用于批量配置）

```java
@Component
public class AppConfig {
    @Value("${app.name:default-app}")
    private String appName;

    @Value("${app.version:1.0.0}")
    private String appVersion;
}
```

缺点：分散在各处，不易管理，不支持松散绑定（relaxed binding）和类型转换。

#### @ConfigurationProperties（推荐，类型安全）

```java
@Component
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {
    private String name;
    private String version;
    private Contact contact = new Contact(); // 嵌套类

    @Data
    public static class Contact {
        private String email;
        private String phone;
    }
}
```

对应的配置文件：

```yaml
app:
  name: myapp
  version: 2.0.0
  contact:
    email: admin@example.com
    phone: "123456789"
```

使用 `@ConfigurationProperties` 的优势：

- **类型安全**：编译期即可检查类型，运行时发生类型错误会有清晰的报错信息
- **松散绑定**：`app.contact.email` 与 `app.contact-email`、`app.contact_email` 等效
- **元数据支持**：可生成 `spring-configuration-metadata.json`，在 IDE 中获得自动补全
- **JSR-303 校验**：支持 `@NotNull`、`@Min`、`@Max` 等校验注解

```java
@Validated
@Component
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {
    @NotBlank
    private String name;

    @Min(0)
    @Max(9999)
    private int maxConnections;
}
```

### 2.6 配置加密

生产环境中，数据库密码、Redis 密码等敏感信息不应明文存储在配置文件中。**jasypt-spring-boot** 是常用的配置加密方案。

```xml
<dependency>
    <groupId>com.github.ulisesbocchio</groupId>
    <artifactId>jasypt-spring-boot-starter</artifactId>
    <version>3.0.5</version>
</dependency>
```

```yaml
jasypt:
  encryptor:
    password: my-secret-key
    algorithm: PBEWithMD5AndDES

spring:
  datasource:
    password: ENC(encrypted-password-here)
```

加密工具类生成密文：

```java
StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
encryptor.setPassword("my-secret-key");
encryptor.setAlgorithm("PBEWithMD5AndDES");
String encrypted = encryptor.encrypt("original-password");
System.out.println(encrypted);
```

> **注意**：jasypt 的密钥本身仍然是明文，更安全的做法是结合环境变量 `jasypt.encryptor.password=${JASYPT_SECRET}` 从外部传入密钥，或在微服务体系中使用配置中心（Nacos/Apollo）统一管理敏感配置。

---

## 三、Actuator 监控

Spring Boot Actuator 是生产环境下必不可少的监控组件，提供了大量 HTTP 端点用于查看应用健康状况、运行时信息、日志级别等。

### 3.1 引入与配置

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,env,loggers,threaddump,heapdump,mappings
      base-path: /actuator
  endpoint:
    health:
      show-details: always
      show-components: always
    env:
      show-values: always  # Spring Boot 3.x 默认不显示值
```

### 3.2 核心端点详解

| 端点 | 路径 | 说明 |
|---|---|---|
| `health` | `/actuator/health` | 健康检查，返回 UP/DOWN 状态，可集成数据库、Redis 等外部组件 |
| `info` | `/actuator/info` | 自定义应用信息 |
| `metrics` | `/actuator/metrics` | 应用指标（JVM 内存、线程、GC、HTTP 请求等） |
| `env` | `/actuator/env` | 环境变量与配置属性 |
| `loggers` | `/actuator/loggers` | 查看和动态修改日志级别 |
| `threaddump` | `/actuator/threaddump` | 线程转储 |
| `heapdump` | `/actuator/heapdump` | 堆转储（生成 .hprof 文件用于 OOM 分析） |
| `mappings` | `/actuator/mappings` | 显示所有 RequestMapping 映射 |
| `scheduledtasks` | `/actuator/scheduledtasks` | 显示定时任务信息 |

#### info 端点自定义

```yaml
info:
  app:
    name: '@project.name@'
    version: '@project.version@'
    encoding: '@project.build.sourceEncoding@'
    java:
      source: '@java.version@'
      target: '@java.version@'
```

#### 动态修改日志级别

```bash
# 将 com.example 包的日志级别改为 DEBUG
curl -X POST http://localhost:8080/actuator/loggers/com.example \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "DEBUG"}'
```

这在线上故障排查时极为实用——无需重启即可临时启用 DEBUG 日志获取详细信息，排查完毕后再恢复到 WARN。

### 3.3 集成 Prometheus + Grafana

在微服务监控体系中，Actuator 的 metrics 端点通过 **Micrometer** 接入 **Prometheus**，再由 **Grafana** 进行可视化。

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```yaml
management:
  endpoints:
    web:
      exposure:
        include: prometheus,health,metrics
  metrics:
    tags:
      application: ${spring.application.name}
```

配置完成后访问 `/actuator/prometheus` 即可看到 Prometheus 格式的指标数据。

### 3.4 Spring Boot Admin

Spring Boot Admin 是社区提供的一套监控管理 UI，可以集中监控多个 Spring Boot 应用实例。

- **Server 端**：一个 Spring Boot 应用，启动 Admin Server
- **Client 端**：各业务服务注册到 Admin Server

```xml
<!-- Admin Server -->
<dependency>
    <groupId>de.codecentric</groupId>
    <artifactId>spring-boot-admin-starter-server</artifactId>
    <version>3.2.3</version>
</dependency>
```

```java
@EnableAdminServer
@SpringBootApplication
public class AdminServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminServerApplication.class, args);
    }
}
```

```yaml
# Client 端配置
spring:
  boot:
    admin:
      client:
        url: http://localhost:9090
```

### 3.5 自定义 HealthIndicator

当应用依赖一些无法被 Spring Boot 自动识别的组件时，可以自定义健康检查：

```java
@Component
public class CustomServiceHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        try {
            // 检查某个外部服务的连通性
            boolean isHealthy = checkExternalService();
            if (isHealthy) {
                return Health.up()
                    .withDetail("externalService", "available")
                    .build();
            }
            return Health.down()
                .withDetail("externalService", "unavailable")
                .build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }

    private boolean checkExternalService() {
        // 实际检查逻辑
        return true;
    }
}
```

### 3.6 自定义 Endpoint

```java
@Component
@Endpoint(id = "custom")
public class CustomEndpoint {
    @ReadOperation
    public Map<String, Object> custom() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "running");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    @WriteOperation
    public void reset(@Selector String param) {
        // 写操作逻辑
    }
}
```

访问 `/actuator/custom` 即可看到自定义端点返回的信息。

---

## 四、异常处理与统一返回

良好的异常处理机制是构建健壮 RESTful API 的基础。Spring Boot 推荐通过 `@RestControllerAdvice` 实现全局异常处理，并配合统一的返回结构体。

### 4.1 统一返回体

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> success() {
        return new Result<>(200, "success", null);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> error(IErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }
}
```

### 4.2 错误码枚举

```java
public interface IErrorCode {
    int getCode();
    String getMessage();
}

@Getter
@AllArgsConstructor
public enum BusinessErrorCode implements IErrorCode {
    // 通用错误
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    // 业务错误
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_EXISTS(1002, "用户已存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    ORDER_NOT_FOUND(2001, "订单不存在"),
    ORDER_STATUS_INVALID(2002, "订单状态无效"),

    // 系统错误
    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用");

    private final int code;
    private final String message;
}
```

### 4.3 全局异常处理

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 自定义业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return Result.error(400, message);
    }

    /**
     * 参数绑定异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .collect(Collectors.joining(", "));
        return Result.error(400, message);
    }

    /**
     * 参数类型不匹配
     */
    @ExceptionHandler(TypeMismatchException.class)
    public Result<Void> handleTypeMismatchException(TypeMismatchException e) {
        return Result.error(400, "参数类型错误: " + e.getPropertyName());
    }

    /**
     * HTTP 请求方法不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        return Result.error(405, "请求方法不支持，支持的方法: " +
            String.join(", ", e.getSupportedMethods()));
    }

    /**
     * 兜底异常 —— 避免未处理异常暴露给客户端
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("未知异常, URI: {}", request.getRequestURI(), e);
        return Result.error(500, "服务器内部错误，请稍后重试");
    }
}
```

### 4.4 自定义业务异常

```java
@Getter
public class BusinessException extends RuntimeException {
    private final int code;
    private final String message;

    public BusinessException(IErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BusinessException(IErrorCode errorCode, Object... args) {
        super(String.format(errorCode.getMessage(), args));
        this.code = errorCode.getCode();
        this.message = String.format(errorCode.getMessage(), args);
    }
}
```

使用：

```java
@GetMapping("/users/{id}")
public Result<UserVO> getUser(@PathVariable Long id) {
    User user = userService.getById(id);
    if (user == null) {
        throw new BusinessException(BusinessErrorCode.USER_NOT_FOUND);
    }
    return Result.success(userConverter.toVO(user));
}
```

### 4.5 参数校验国际化

Spring Boot 支持国际化校验消息，在 `messages.properties` 中定义：

```properties
# messages.properties
user.name.notnull=用户名不能为空
user.email.invalid=邮箱格式不正确

# messages_zh_CN.properties
user.name.notnull=用户名不能为空
user.email.invalid=邮箱格式不正确
```

在实体上使用：

```java
@Data
public class UserCreateRequest {
    @NotBlank(message = "{user.name.notnull}")
    private String name;

    @Email(message = "{user.email.invalid}")
    private String email;

    @NotNull
    @Min(1)
    private Integer age;
}
```

然后在配置类中注册 `MessageSource`：

```java
@Bean
public MessageSource messageSource() {
    ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
    messageSource.setBasename("classpath:messages");
    messageSource.setDefaultEncoding("UTF-8");
    return messageSource;
}
```

这样做的好处是，当客户端请求头 `Accept-Language: en-US` 时返回英文提示，请求头 `Accept-Language: zh-CN` 时返回中文提示。

---

## 五、拦截器与过滤器

### 5.1 Filter（过滤器）

Filter 是 Java Servlet 规范中的组件，在 Spring 容器之外运作。它基于回调机制，对请求进行前置/后置处理。

#### 实现方式一：@WebFilter + @ServletComponentScan

```java
@WebFilter(urlPatterns = "/*", filterName = "logFilter")
@Slf4j
public class LogFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) {
        log.info("LogFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        long start = System.currentTimeMillis();
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        log.info("请求开始: {} {}", httpRequest.getMethod(), httpRequest.getRequestURI());
        chain.doFilter(request, response);
        long duration = System.currentTimeMillis() - start;
        log.info("请求结束: {} {}, 耗时: {}ms",
            httpRequest.getMethod(), httpRequest.getRequestURI(), duration);
    }

    @Override
    public void destroy() {
        log.info("LogFilter destroyed");
    }
}
```

在启动类上添加 `@ServletComponentScan`：

```java
@SpringBootApplication
@ServletComponentScan
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

#### 实现方式二：FilterRegistrationBean（推荐，可排序）

```java
@Configuration
public class FilterConfig {
    @Bean
    public FilterRegistrationBean<LogFilter> logFilter() {
        FilterRegistrationBean<LogFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new LogFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(1); // 设置优先级，数值越小越优先
        return registrationBean;
    }
}
```

### 5.2 Interceptor（拦截器）

Interceptor 是 Spring MVC 框架提供的组件，基于 Java 反射（AOP）机制，拥有更精细的控制能力。

```java
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /**
     * 在 Controller 方法调用之前执行
     * 返回 true 继续执行，返回 false 中断请求
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write(JSON.toJSONString(Result.error(401, "未授权")));
            return false;
        }
        // 验证 token 逻辑...
        return true;
    }

    /**
     * Controller 方法执行之后、视图渲染之前执行
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) throws Exception {
        // 后置处理
    }

    /**
     * 请求完成之后执行（无论是否发生异常）
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) throws Exception {
        if (ex != null) {
            log.error("请求处理异常", ex);
        }
    }
}
```

#### 注册拦截器

```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
            .addPathPatterns("/api/**")      // 拦截路径
            .excludePathPatterns(
                "/api/auth/login",           // 排除登录接口
                "/api/auth/register",
                "/actuator/**",               // 排除监控端点
                "/swagger-ui/**",
                "/v3/api-docs/**"
            );
    }
}
```

### 5.3 Filter 与 Interceptor 的区别

| 对比项 | Filter | Interceptor |
|---|---|---|
| **规范层级** | Java Servlet 规范 | Spring MVC 框架 |
| **容器依赖** | 不依赖 Spring 容器（但可注入 Spring Bean） | 依赖 Spring 容器 |
| **作用范围** | 所有 Web 请求（含静态资源、Servlet） | 仅 Spring MVC 的 Controller 请求 |
| **调用时机** | 请求进入 Servlet 容器后、进入 DispatcherServlet 前 | 进入 DispatcherServlet 后、进入 Controller 前 |
| **回调接口** | Filter 接口（init/doFilter/destroy） | HandlerInterceptor 接口（preHandle/postHandle/afterCompletion） |
| **获取 Handler** | 无法获取 | 可以获取 MethodHandler，获取 Controller 方法信息 |
| **粒度** | 粗粒度（基于 URL 模式） | 细粒度（可基于 URL、Handler 类型等） |

### 5.4 执行顺序

```
请求到达
  ↓
Filter.doFilter（前置逻辑）
  ↓
DispatcherServlet
  ↓
Interceptor.preHandler
  ↓
Controller 方法执行
  ↓
Interceptor.postHandler
  ↓
视图渲染（如有）
  ↓
Interceptor.afterCompletion
  ↓
Filter.doFilter（后置逻辑）
  ↓
响应返回
```

多个 Filter 通过 `@Order` 或 `FilterRegistrationBean.setOrder()` 控制顺序；多个 Interceptor 通过 `registry.addInterceptor()` 的顺序控制。

---

## 六、最佳实践

### 6.1 Lombok 简化代码

Lombok 通过编译期注解生成 getter/setter/toString/equals/hashCode/构造器/日志对象等代码，极大减少了样板代码。

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

```java
@Data                        // @Getter + @Setter + @ToString + @EqualsAndHashCode
@Builder                     // 建造者模式
@NoArgsConstructor
@AllArgsConstructor
@Slf4j                       // 注入 log 对象
public class User {
    private Long id;
    private String name;
    private String email;
}

// 使用
User user = User.builder()
    .name("张三")
    .email("zhangsan@example.com")
    .build();
log.info("创建用户: {}", user);
```

> **注意**：`@Builder` 和 `@NoArgsConstructor` 同时使用时需格外小心，因为 `@Builder` 会生成全参构造器，如果还需要无参构造器必须显式声明 `@NoArgsConstructor`，否则 MyBatis/JPA 等 ORM 框架在反射创建对象时会报错。推荐同时使用 `@Builder` + `@NoArgsConstructor` + `@AllArgsConstructor`。

### 6.2 MapStruct 对象转换

在分层架构中，Entity（持久层）、DTO（传输层）、VO（展示层）之间需要频繁转换。MapStruct 在编译期生成类型安全的转换代码，性能优于反射实现的 BeanUtils。

```xml
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.5.Final</version>
</dependency>
```

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </path>
            <path>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>1.5.5.Final</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

```java
// Entity
@Data
public class User {
    private Long id;
    private String name;
    private String email;
    private LocalDateTime createTime;
}

// DTO
@Data
public class UserDTO {
    private Long id;
    private String name;
    private String email;
    private String createTimeStr; // String 类型，需要自定义转换
}

// MapStruct 转换器
@Mapper(componentModel = "spring")
public interface UserConverter {
    UserConverter INSTANCE = Mappers.getMapper(UserConverter.class);

    @Mapping(target = "createTimeStr", source = "createTime",
             dateFormat = "yyyy-MM-dd HH:mm:ss")
    UserDTO toDTO(User user);

    List<UserDTO> toDTOList(List<User> users);
}
```

使用：

```java
@Service
public class UserService {
    @Autowired
    private UserConverter userConverter;

    public UserDTO getUser(Long id) {
        User user = userMapper.selectById(id);
        return userConverter.toDTO(user);
    }
}
```

### 6.3 优雅关闭

生产环境下，直接 `kill -9` 杀死进程可能导致正在处理的请求被中断、数据库连接池未释放、消息丢失等问题。Spring Boot 提供了优雅关闭（Graceful Shutdown）：

```yaml
server:
  shutdown: graceful          # 开启优雅关闭
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s  # 宽限期，超时后强制关闭
```

开启后，应用收到 `SIGTERM`（`kill -15`）信号时：

1. 停止接收新请求（Tomcat 将连接标记为 CLOSE）
2. 等待正在处理中的请求完成（最多等待 30 秒）
3. 销毁 Bean、释放资源
4. 退出进程

结合 Docker 部署时，务必使用 `STOPSIGNAL SIGTERM` 而非 `SIGKILL`：

```dockerfile
STOPSIGNAL SIGTERM
```

### 6.4 Banner 自定义

将 `banner.txt` 放置在 `src/main/resources` 下，Spring Boot 启动时将打印其中的内容。可以使用以下占位符：

```
${spring-boot.version}
${application.version}
${application.title}
${application.formatted-version}
```

也可以关闭 Banner：

```yaml
spring:
  main:
    banner-mode: off  # console / log / off
```

### 6.5 外部化配置注入详解

#### @ConfigurationProperties 嵌套

```java
@Component
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {
    private String name;
    private Security security = new Security();

    @Data
    public static class Security {
        private Jwt jwt = new Jwt();
        private Cors cors = new Cors();

        @Data
        public static class Jwt {
            private String secret;
            private long expiration = 3600000L; // 默认值
            private String header = "Authorization";
        }

        @Data
        public static class Cors {
            private List<String> allowedOrigins = new ArrayList<>();
            private List<String> allowedMethods = new ArrayList<>();
        }
    }
}
```

对应的配置：

```yaml
app:
  name: myapp
  security:
    jwt:
      secret: my-secret-key
      expiration: 7200000
    cors:
      allowed-origins:
        - http://localhost:3000
        - https://example.com
      allowed-methods:
        - GET
        - POST
        - PUT
        - DELETE
```

#### @DurationUnit 与 @DataSizeUnit

Spring Boot 提供了 `@DurationUnit` 和 `@DataSizeUnit` 注解，用于自动解析时间/数据大小字符串配置：

```java
@Component
@ConfigurationProperties(prefix = "app.task")
@Data
public class TaskProperties {
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration timeout = Duration.ofSeconds(30);

    @DataSizeUnit(DataUnit.MEGABYTES)
    private DataSize maxFileSize = DataSize.ofMegabytes(10);
}
```

配置时可使用人类可读的字符串：

```yaml
app:
  task:
    timeout: 30s      # 或 5m、2h
    max-file-size: 10MB  # 或 1024KB、1GB
```

---

## 七、常见面试题深度解析

### 7.1 Spring Boot 如何实现自动配置？

**核心答案（面试评分要点）：**

1. **入口注解**：`@SpringBootApplication` 组合了 `@EnableAutoConfiguration`。
2. **导入选择器**：`@EnableAutoConfiguration` 通过 `@Import(AutoConfigurationImportSelector.class)` 导入配置选择器。
3. **加载候选配置**：`AutoConfigurationImportSelector` 从 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`（Spring Boot 3.x）或 `spring.factories`（Spring Boot 2.x）中读取所有自动配置类的全限定名。
4. **条件过滤**：加载的自动配置类上通常标注了 `@ConditionalOnClass`、`@ConditionalOnMissingBean` 等条件注解，只有满足条件的配置才会被注册为 Bean。
5. **排序生效**：通过 `@AutoConfigureBefore`、`@AutoConfigureAfter`、`@AutoConfigureOrder` 控制配置类加载顺序。

**深挖问题（面试加分项）：**

- **`AutoConfigurationImportSelector` 实现了哪个接口？** 实现了 `DeferredImportSelector`，延迟导入意味着自动配置在所有用户自定义 `@Configuration` 之后处理，这样用户可以通过 `@ConditionalOnMissingBean` 优先覆盖自动配置。
- **Condition 的匹配流程？** `AutoConfigurationImportSelector` 内部使用 `ConditionEvaluator`，遍历配置类上的所有 `@Conditional` 注解，通过 `Condition` 接口的 `matches` 方法进行匹配。
- **Spring Boot 3.x 相对于 2.x 的改动？** 自动配置类的清单从 `spring.factories` 迁移到了 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`，并且自动配置类全部基于 Jakarta EE 9+（javax → jakarta）。

### 7.2 如何自定义一个 Starter？

**分模块架构：**

```
my-starter
├── my-starter-autoconfigure   # 自动配置模块
│   ├── src/main/java/...
│   │   └── com/example/starter/
│   │       ├── HelloService.java
│   │       └── HelloAutoConfiguration.java
│   └── src/main/resources/
│       └── META-INF/
│           └── spring/
│               └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
└── my-starter-starter          # Starter 聚合模块（可选，但推荐）
    └── pom.xml                 # 依赖 my-starter-autoconfigure
```

**自动配置类：**

```java
@AutoConfiguration
@ConditionalOnClass(HelloService.class)
@EnableConfigurationProperties(HelloProperties.class)
public class HelloAutoConfiguration {

    @Autowired
    private HelloProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public HelloService helloService() {
        return new HelloService(properties.getPrefix(), properties.getSuffix());
    }
}
```

**属性类：**

```java
@ConfigurationProperties(prefix = "hello")
@Data
public class HelloProperties {
    private String prefix = "Hello";
    private String suffix = "!";
}
```

**服务类：**

```java
public class HelloService {
    private final String prefix;
    private final String suffix;

    public HelloService(String prefix, String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
    }

    public String sayHello(String name) {
        return prefix + ", " + name + suffix;
    }
}
```

**注册文件：**

```properties
# META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
com.example.starter.HelloAutoConfiguration
```

**starter 的 pom.xml：**

```xml
<project>
    <groupId>com.example</groupId>
    <artifactId>my-starter-starter</artifactId>
    <version>1.0.0</version>
    <dependencies>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>my-starter-autoconfigure</artifactId>
            <version>1.0.0</version>
        </dependency>
    </dependencies>
</project>
```

**使用者只需引入：**

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>my-starter-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

然后在配置文件中设置属性：

```yaml
hello:
  prefix: 你好
  suffix: ！
```

在代码中注入：

```java
@Autowired
private HelloService helloService;

public void test() {
    helloService.sayHello("Spring Boot"); // 输出: 你好, Spring Boot！
}
```

**那么为什么需要把 autoconfigure 和 starter 拆成两个模块？**

- **autoconfigure 模块**：包含自动配置逻辑和条件判断，可能依赖第三方库
- **starter 模块**：作为门面（Facade），仅仅传递依赖 autoconfigure 模块以及该 starter 所需的其他依赖

这样设计的好处是：如果使用者不需要自动配置，可以直接依赖 autoconfigure 模块并使用其中提供的 API，而不会引入额外的不必要依赖。这种解耦模式是 Spring Boot 官方的推荐实践。

### 7.3 Spring Boot 启动流程

Spring Boot 的启动入口是 `SpringApplication.run()`，其完整流程可以概括为以下三个阶段：

#### 第一阶段：准备阶段

```
① 推断应用类型（WebApplicationType）
   - 根据类路径中是否存在相应类推断为 NONE / SERVLET / REACTIVE
② 加载所有可用 SpringApplicationRunListener
   - 从 spring.factories 初始化并排序
③ 推断 Main 类（通过堆栈信息）
```

##### 第二阶段：运行阶段

```
④ 创建 DefaultBootstrapContext（启动上下文）
⑤ 依次调用 SpringApplicationRunListener.starting()
⑥ 准备 Environment
   - 创建 ApplicationContext 对应的 Environment（StandardServletEnvironment / StandardReactiveWebEnvironment）
   - 配置 PropertySource 并激活 Profile
⑦ 创建 ApplicationContext
   - SERVLET → AnnotationConfigServletWebServerApplicationContext
   - REACTIVE → AnnotationConfigReactiveWebServerApplicationContext
   - NONE → AnnotationConfigApplicationContext
⑧ 准备 Context（prepareContext）
   - 设置 Environment
   - 执行 BeanFactoryPostProcessor
   - 注册 BeanDefinition
   - 发送 ApplicationPreparedEvent
⑨ 刷新 Context（refreshContext）
   - 调用 AbstractApplicationContext.refresh()，这是 Spring 容器的核心流程
   - refresh() 内部调用 invokeBeanFactoryPostProcessors、registerBeanPostProcessors、
      initMessageSource、initApplicationEventMulticaster、onRefresh（创建内嵌 Web 容器）、
      registerListeners、finishBeanFactoryInitialization（实例化所有单例 Bean）、finishRefresh
```

##### 第三阶段：收尾阶段

```
⑩ afterRefresh（空方法，可被子类重写）
⑪ 依次调用 SpringApplicationRunListener.started()
⑫ 调用所有 ApplicationRunner 和 CommandLineRunner
⑬ 依次调用 SpringApplicationRunListener.ready()
⑭ 返回 ConfigurableApplicationContext
```

**核心点解析：**

- **onRefresh()**：这是内嵌 Web 容器的创建时机。`ServletWebServerApplicationContext.onRefresh()` 会调用 `createWebServer()`，创建并启动 Tomcat/Jetty/Undertow。
- **refreshContext()** 是整个 Spring 框架的核心，继承了 `AbstractApplicationContext.refresh()` 的 13 个步骤，是 IoC 容器初始化的全部过程。
- **ApplicationRunner vs CommandLineRunner**：二者都是在容器启动完成后执行，区别在于 `ApplicationRunner` 将命令行参数封装为 `ApplicationArguments`，提供了更便捷的访问方式（如判断是否包含某个 option 参数）。

### 7.4 更多高频面试题

**Q：Spring Boot 的核心注解有哪些？**
A：`@SpringBootApplication`、`@EnableAutoConfiguration`、`@ConditionalOnClass`、`@ConditionalOnMissingBean`、`@ConfigurationProperties` 等。

**Q：Spring Boot 支持哪些内嵌容器？如何切换？**
A：支持 Tomcat（默认）、Jetty、Undertow。排除 spring-boot-starter-tomcat，引入对应的 starter 即可。

**Q：@ConfigurationProperties 和 @Value 的区别？**
A：@ConfigurationProperties 支持松散绑定、类型安全、JSR-303 校验、元数据生成；@Value 是 SpEL 表达式，不支持松散绑定，适合单值注入。

**Q：Spring Boot 的配置加载优先级？**
A：命令行参数 > JNDI > 环境变量 > application-{profile}.yml > application.yml。

**Q：如何自定义一个 HealthIndicator？**
A：实现 HealthIndicator 接口，重写 health() 方法，返回 Health.up() 或 Health.down()。

**Q：如何动态修改日志级别？**
A：通过 Actuator 的 loggers 端点：POST /actuator/loggers/com.example，body 包含 `{"configuredLevel": "DEBUG"}`。

**Q：Filter 和 Interceptor 的区别？**
A：Filter 基于 Servlet 规范，在 Spring 容器外；Interceptor 基于 Spring MVC，可以获取 Handler 信息。Filter 范围更大（含静态资源），Interceptor 更精细。

**Q：Spring Boot 3.0 的主要变化？**
A：Java 17 基线、Jakarta EE 9+（javax → jakarta）、Spring Framework 6、GraalVM Native Image 支持、自动配置清单迁移到新的 imports 文件、@SpringBootApplication 不再强制 @EnableAutoConfiguration（逻辑等效）、AOT（Ahead-Of-Time）编译引擎。

---

## 总结与速查清单

| 知识点 | 核心内容 | 是否掌握 |
|---|---|---|
| 起步依赖 | 通过 Starter 统一管理依赖，BOM 仲裁版本 | □ |
| 自动配置原理 | @SpringBootApplication → @EnableAutoConfiguration → AutoConfigurationImportSelector → spring.factories/imports → @Conditional 过滤 → 注册 Bean | □ |
| @Conditional 族 | @ConditionalOnClass/MissingBean/Property/WebApplication 等，控制自动配置是否生效 | □ |
| 内嵌容器 | 默认 Tomcat，可切换 Jetty/Undertow | □ |
| 多环境配置 | application-{profile}.yml，spring.profiles.active，分组 profile | □ |
| @ConfigurationProperties | 类型安全配置注入，松散绑定，嵌套对象，JSR-303 校验 | □ |
| Actuator 监控 | health/info/metrics/loggers/env/threaddump/heapdump/mappings 端点 | □ |
| 统一异常处理 | @RestControllerAdvice + Result<T> + BusinessException + 参数校验 | □ |
| 过滤器与拦截器 | Filter（Servlet 规范） vs Interceptor（Spring MVC），执行顺序 | □ |
| Lombok | @Data/@Builder/@Slf4j 减少样板代码 | □ |
| MapStruct | 编译期生成的 Bean 转换器，替代反射 BeanUtils | □ |
| 优雅关闭 | server.shutdown=graceful + spring.lifecycle.timeout-per-shutdown-phase | □ |
| 自定义 Starter | autoconfigure + starter 双模块，spring.factories/imports，@ConditionalOnMissingBean | □ |
| 启动流程 | 推断应用类型 → 加载 Listener → 创建 Environment → 创建 ApplicationContext → prepareContext → refreshContext（onRefresh 创建内嵌容器）→ afterRefresh → callRunners | □ |

---

> **参考**：Spring Boot 官方文档、Spring Boot Actuator 参考指南、Spring Framework 源码。实际生产中应结合具体业务场景灵活应用上述实践，不断优化迭代。建议读者动手实现一个自定义 Starter、搭建一套 Actuator + Prometheus + Grafana 监控体系，以加深理解。
