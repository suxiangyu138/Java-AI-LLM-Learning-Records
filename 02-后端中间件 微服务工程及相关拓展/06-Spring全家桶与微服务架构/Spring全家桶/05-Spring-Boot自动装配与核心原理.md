# 05-Spring Boot自动装配与核心原理
> 🎯 Spring Boot是Spring生态的加速器 — 掌握自动装配原理、Starter机制、配置优先级、Actuator监控，是进入微服务世界的必备技能

---

## 目录
1. [本章总览](#1-本章总览)
2. [Spring Boot核心特性](#2-spring-boot核心特性)
3. [自动装配原理深度剖析](#3-自动装配原理深度剖析)
4. [Starter机制与自定义Starter](#4-starter机制与自定义starter)
5. [配置文件与多环境管理](#5-配置文件与多环境管理)
6. [Actuator监控端点](#6-actuator监控端点)
7. [内置Web容器](#7-内置web容器)
8. [高频踩坑与误区](#8-高频踩坑与误区)
9. [随堂基础练习](#9-随堂基础练习)
10. [章节综合实操案例](#10-章节综合实操案例)
11. [分层综合习题](#11-分层综合习题)
12. [本章复盘速记清单](#12-本章复盘速记清单)
13. [精通拓展补充-P2](#13-精通拓展补充-p2)

---

## 1. 本章总览

### 1.1 知识定位
- **归属**：Spring全家桶核心组件 → 层级2 P0核心必学
- **前置依赖**：Spring MVC + Maven + IoC/AOP
- **重要性**：⭐⭐⭐⭐⭐（现代Java后端开发的事实标准）

### 1.2 三层学习目标

| 级别 | 目标 |
|------|------|
| **基础** | 使用Spring Initializr创建项目，编写application.yml配置文件 |
| **熟练** | 理解自动装配原理，能自定义Starter，使用Actuator监控 |
| **精通** | 吃透`@EnableAutoConfiguration`源码流程，能排查自动装配失效问题 |

---

## 2. Spring Boot核心特性

### 2.1 Spring Boot解决了什么问题

> 🎯 **约定大于配置（Convention over Configuration）**：提供默认配置，开发者只需关注业务代码。

| 传统Spring MVC | Spring Boot |
|---------------|-------------|
| 手动配置web.xml | 自动配置DispatcherServlet |
| 手动配置数据源、事务管理器 | 引入starter依赖即自动配置 |
| 需要外置Tomcat部署 | 内嵌Tomcat，直接`java -jar`运行 |
| 大量XML配置 | 零XML，全注解+yml配置 |
| 依赖版本手动管理 | `spring-boot-starter-parent`统一管理版本 |

### 2.2 @SpringBootApplication 三合一

```java
@SpringBootApplication
// 等价于以下三个注解的组合：
// @SpringBootConfiguration   → @Configuration（标识配置类）
// @EnableAutoConfiguration   → 开启自动装配（核心！）
// @ComponentScan             → 扫描当前包及子包的@Component
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 2.3 SpringApplication启动流程

```
SpringApplication.run()
    ↓
1. 创建SpringApplication实例
    ├── 推断应用类型（SERVLET/REACTIVE/NONE）
    ├── 加载ApplicationContextInitializer
    └── 加载ApplicationListener
    ↓
2. 执行run()
    ├── 准备Environment（加载配置文件）
    ├── 创建ApplicationContext（默认AnnotationConfigServletWebServerApplicationContext）
    ├── 准备Context（执行Initializer）
    ├── 刷新Context → 自动装配发生在这里！
    │   ├── 扫描@ComponentScan指定的包
    │   └── 执行@EnableAutoConfiguration → 加载spring.factories中的配置类
    ├── 启动内嵌WebServer（Tomcat/Jetty/Undertow）
    └── 执行Runner（ApplicationRunner / CommandLineRunner）
```

---

## 3. 自动装配原理深度剖析

> 🔥 这是Spring Boot面试最核心考点，必须完整掌握！

### 3.1 自动装配入口

```java
// 入口：@SpringBootApplication → @EnableAutoConfiguration
@Import(AutoConfigurationImportSelector.class)
public @interface EnableAutoConfiguration { ... }
```

### 3.2 完整流程

```
@EnableAutoConfiguration
    ↓
@Import(AutoConfigurationImportSelector.class)
    ↓
AutoConfigurationImportSelector.selectImports()
    ↓
getAutoConfigurationEntry()
    ↓
SpringFactoriesLoader.loadFactoryNames()
    ↓ 读取 classpath 下的配置文件
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
    │  (Spring Boot 2.7+ 新增，替代 spring.factories)
    │
    └── META-INF/spring.factories 中的
        org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
        org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration,\
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,\
        ...（共100+自动配置类）
    ↓
对加载的配置类进行过滤（@Conditional条件判断）
    ├── @ConditionalOnClass：类存在时才生效
    ├── @ConditionalOnMissingBean：容器中没有指定Bean时才生效
    ├── @ConditionalOnProperty：配置值匹配时才生效
    └── @ConditionalOnBean：容器中有指定Bean时才生效
    ↓
满足条件的配置类被加载，其内部@Bean方法被调用
    ↓
自动装配完成！
```

### 3.3 以DataSource自动装配为例

```java
// spring-boot-autoconfigure 中的 DataSourceAutoConfiguration
@AutoConfiguration                                          // Spring Boot 2.7+
@ConditionalOnClass({ DataSource.class, EmbeddedDatabaseType.class })
@EnableConfigurationProperties(DataSourceProperties.class)  // 绑定 spring.datasource.*
@Import({ DataSourcePoolMetadataProvidersConfiguration.class })
public class DataSourceAutoConfiguration {

    @Configuration
    @ConditionalOnMissingBean(DataSource.class)
    static class EmbeddedDatabaseConfiguration { ... }       // 内嵌数据库(H2)

    @Configuration
    @ConditionalOnProperty(name = "spring.datasource.type")  // 自定义数据源
    static class Generic { ... }

    @Configuration
    static class PooledDataSourceConfiguration {             // 连接池自动选择
        @Bean
        @ConditionalOnMissingBean(DataSource.class)
        DataSource dataSource(DataSourceProperties properties) {
            // 按优先级选择：HikariCP → Tomcat Pool → Commons DBCP2 → Oracle UCP
            return properties.initializeDataSourceBuilder().build();
        }
    }
}
```

### 3.4 断点调试验证

```yaml
# 开启自动装配报告（DEBUG级别）
logging:
  level:
    org.springframework.boot.autoconfigure: DEBUG
```
启动后控制台输出：
```
Positive matches:（生效的自动配置）
   DataSourceAutoConfiguration matched:
      - @ConditionalOnClass classes found: javax.sql.DataSource

Negative matches:（未生效的自动配置）
   RedisAutoConfiguration did not match:
      - @ConditionalOnClass did not find required class 'org.springframework.data.redis.core.RedisOperations'
```

---

## 4. Starter机制与自定义Starter

### 4.1 Starter命名规范

| 类型 | 命名规范 | 示例 |
|------|----------|------|
| 官方Starter | `spring-boot-starter-{模块}` | `spring-boot-starter-web` |
| 第三方Starter | `{模块}-spring-boot-starter` | `mybatis-spring-boot-starter` |

### 4.2 常用官方Starter

| Starter | 提供的功能 |
|---------|-----------|
| `spring-boot-starter-web` | Spring MVC + 内嵌Tomcat + Jackson |
| `spring-boot-starter-data-jpa` | Spring Data JPA + Hibernate |
| `spring-boot-starter-data-redis` | Spring Data Redis + Lettuce |
| `spring-boot-starter-security` | Spring Security |
| `spring-boot-starter-test` | JUnit5 + Mockito + Spring Test |
| `spring-boot-starter-actuator` | 监控端点 |
| `spring-boot-starter-validation` | Bean Validation（Hibernate Validator） |

### 4.3 自定义Starter

```
my-spring-boot-starter/
├── pom.xml
├── src/main/java/com/example/
│   ├── MyAutoConfiguration.java       # 自动配置类
│   └── MyProperties.java              # 配置属性类
└── src/main/resources/
    └── META-INF/spring/
        └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

```java
// 1. 配置属性类
@ConfigurationProperties(prefix = "my.starter")
@Data
public class MyProperties {
    private boolean enabled = true;
    private String name = "default";
    private int timeout = 5000;
}

// 2. 自动配置类
@AutoConfiguration
@EnableConfigurationProperties(MyProperties.class)
@ConditionalOnProperty(prefix = "my.starter", name = "enabled", havingValue = "true", 
                        matchIfMissing = true)
public class MyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MyService myService(MyProperties properties) {
        return new MyService(properties.getName(), properties.getTimeout());
    }
}

// 3. spring.factories (或 AutoConfiguration.imports)
// resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
com.example.MyAutoConfiguration
```

---

## 5. 配置文件与多环境管理

### 5.1 配置文件格式对比

| 格式 | 示例 | 特点 |
|------|------|------|
| `application.yml` | 层级缩进，可读性好 | ⭐⭐⭐⭐⭐ 推荐 |
| `application.properties` | key=value，简单 | 传统方式 |
| `application.yaml` | 同yml | 同yml |

### 5.2 配置优先级（从高到低）

```
1. 命令行参数（--server.port=8081）
2. 操作系统环境变量（SPRING_APPLICATION_JSON）
3. JVM系统属性（-Dserver.port=8081）
4. application-{profile}.yml（profile-specific）
5. application.yml（默认）
6. @PropertySource 加载的配置文件
7. 默认配置（Spring Boot 内置）
```

### 5.3 多环境配置

```yaml
# application.yml（公共配置）
spring:
  application:
    name: my-app
  profiles:
    active: dev  # 激活哪个环境

# application-dev.yml（开发环境）
server:
  port: 8080
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver

# application-prod.yml（生产环境）
server:
  port: 80
spring:
  datasource:
    url: jdbc:mysql://prod-db:3306/mydb
    username: ${DB_USERNAME}  # 从环境变量获取
    password: ${DB_PASSWORD}
```

### 5.4 @ConfigurationProperties类型安全绑定

```java
// 松散绑定：配置文件中的 my.server.ip-address → Java的ipAddress
@Data
@ConfigurationProperties(prefix = "my.server")
public class ServerProperties {
    private String ipAddress;     // 绑定 my.server.ip-address 或 my.server.ipAddress
    private int port;
    private List<String> whitelist;
    private Map<String, String> headers;
    private Duration timeout;     // 支持 10s、500ms、1m 等格式
    private DataSize maxSize;     // 支持 10MB、1GB 等格式
}
```

```yaml
my:
  server:
    ip-address: 192.168.1.1
    port: 9090
    whitelist:
      - 10.0.0.1
      - 10.0.0.2
    headers:
      x-custom: value1
      x-auth: value2
    timeout: 30s
    max-size: 10MB
```

---

## 6. Actuator监控端点

### 6.1 常用端点

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus  # 暴露的端点
  endpoint:
    health:
      show-details: always  # 显示健康检查详情
```

| 端点 | 路径 | 作用 |
|------|------|------|
| `health` | `/actuator/health` | 应用健康状态（含DB、Redis等组件状态） |
| `info` | `/actuator/info` | 应用信息（可以自定义） |
| `metrics` | `/actuator/metrics` | 应用指标（JVM内存、HTTP请求数等） |
| `prometheus` | `/actuator/prometheus` | Prometheus格式指标 |
| `env` | `/actuator/env` | 环境配置（⚠️ 生产环境不要暴露） |
| `beans` | `/actuator/beans` | 容器中所有Bean列表 |
| `mappings` | `/actuator/mappings` | 所有URL映射 |
| `loggers` | `/actuator/loggers` | 动态修改日志级别 |

```json
// GET /actuator/health 示例响应
{
  "status": "UP",
  "components": {
    "db": { "status": "UP", "details": { "database": "MySQL", "result": 1 } },
    "redis": { "status": "UP", "details": { "version": "7.0.0" } },
    "diskSpace": { "status": "UP", "details": { "total": 500GB, "free": 200GB } }
  }
}
```

### 6.2 自定义HealthIndicator

```java
@Component
public class MyServiceHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        try {
            // 检查外部服务是否可用
            boolean isUp = checkExternalService();
            if (isUp) {
                return Health.up().withDetail("externalService", "available").build();
            }
            return Health.down().withDetail("externalService", "unavailable").build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
```

---

## 7. 内置Web容器

### 7.1 三种容器对比

| 容器 | 特点 | 适用场景 |
|------|------|----------|
| **Tomcat** | 成熟稳定、生态完善（默认） | 大部分场景 |
| **Jetty** | 轻量级、快速启动 | 云原生、嵌入式 |
| **Undertow** | 高性能、非阻塞 | 高并发场景 |

```xml
<!-- 切换容器 -->
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
    <artifactId>spring-boot-starter-undertow</artifactId>
</dependency>
```

### 7.2 容器参数调优

```yaml
server:
  tomcat:
    threads:
      max: 200          # 最大工作线程数
      min-spare: 10     # 最小空闲线程数
    max-connections: 8192   # 最大连接数
    accept-count: 100       # 等待队列长度
    connection-timeout: 20000  # 连接超时（ms）
```

---

## 8. 高频踩坑与误区

| 序号 | 问题 | 原因 | 解决方案 |
|------|------|------|----------|
| 1 | 自动装配不生效 | Bean扫描路径不对（主类不在根包） | 主类放在根包，或使用`@ComponentScan`指定 |
| 2 | 自定义Starter不加载 | 未配置`spring.factories`或`AutoConfiguration.imports` | 检查META-INF/spring下的配置文件 |
| 3 | 配置不生效 | @ConfigurationProperties未加`@EnableConfigurationProperties` | 加此注解或使用`@ConfigurationPropertiesScan` |
| 4 | 多环境配置混乱 | profile激活逻辑不清楚 | 理解优先级，使用`spring.profiles.active` |
| 5 | 启动慢 | 加载了不需要的自动配置 | 使用`@EnableAutoConfiguration(exclude=...)`排除 |

---

## 9. 随堂基础练习

1. 创建一个SpringBoot项目，编写application.yml配置端口和数据源
2. 使用`@ConfigurationProperties`绑定自定义配置前缀
3. 开启Actuator的health端点和metrics端点，访问验证
4. 配置dev和prod两套环境配置文件

---

## 10. 章节综合实操案例

```java
// 自定义短信Starter
@Data
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {
    private String accessKey;
    private String secretKey;
    private String signName = "我的应用";
    private int connectTimeout = 5000;
    private int readTimeout = 10000;
}

@AutoConfiguration
@EnableConfigurationProperties(SmsProperties.class)
@ConditionalOnProperty(prefix = "sms", name = "access-key")
public class SmsAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public SmsService smsService(SmsProperties properties) {
        return new SmsService(properties);
    }
}
```

```yaml
# 使用方配置
sms:
  access-key: ${SMS_AK}       # 从环境变量获取
  secret-key: ${SMS_SK}
  sign-name: 我的应用
  connect-timeout: 5000
```

---

## 11. 分层综合习题

### 基础题
1. `@SpringBootApplication`包含哪三个注解？
2. `application.yml`和`application.properties`有什么区别？

### 进阶应用题
3. 描述Spring Boot自动装配的完整流程
4. `@ConditionalOnMissingBean`和`@ConditionalOnClass`分别是什么作用？

### 精通拔高题
5. `AutoConfigurationImportSelector`是如何筛选自动配置类的？分析`filter()`方法
6. 如果两个自动配置类都定义了同类型Bean，Spring如何决定使用哪个？
7. Spring Boot 2.7前后的自动配置注册方式有何变化？

---

## 12. 本章复盘速记清单

| 类别 | 要点 |
|------|------|
| **三合一注解** | `@SpringBootApplication` = `@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan` |
| **自动装配流程** | `@EnableAutoConfiguration` → `AutoConfigurationImportSelector` → `spring.factories`/`.imports` → `@Conditional过滤` |
| **条件注解** | `@ConditionalOnClass` `@ConditionalOnMissingBean` `@ConditionalOnProperty` `@ConditionalOnBean` |
| **Starter命名** | 官方：`spring-boot-starter-*`；第三方：`*-spring-boot-starter` |
| **配置优先级** | 命令行 > 环境变量 > profile配置 > 默认配置 |
| **配置文件** | `application.yml` > `application.properties` |
| **Actuator** | health/info/metrics/env/beans/mappings/loggers |

---

## 13. 精通拓展补充-P2

### 13.1 Spring Boot 2.x vs 3.x 差异

| 维度 | 2.x | 3.x |
|------|-----|-----|
| Java版本 | Java 8+ | Java 17+ |
| Jakarta | `javax.*` | `jakarta.*` |
| 自动配置注册 | `spring.factories` | `AutoConfiguration.imports`（推荐） |
| AOP代理 | 可选JDK/CGLIB | 默认CGLIB |
| 观察性 | — | 新增Micrometer Observation API |

### 13.2 GraalVM原生镜像

```bash
# Spring Boot 3.x + GraalVM 原生编译
mvn -Pnative spring-boot:build-image
# 生成原生可执行文件，启动时间 < 0.1s，内存占用大幅降低
```

### 13.3 Spring Boot启动优化

```java
// 1. 延迟初始化（仅开发环境）
spring.main.lazy-initialization=true

// 2. 排除不需要的自动配置
@SpringBootApplication(exclude = {
    RedisAutoConfiguration.class,
    MongoAutoConfiguration.class
})

// 3. 减少启动时日志
logging.level.org.springframework.boot=WARN
```
