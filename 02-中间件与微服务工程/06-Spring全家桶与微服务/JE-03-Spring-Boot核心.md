# Spring Boot 核心

> **"Convention over Configuration — make decisions for me, but let me override them."**  
> 版本: Spring Boot 3.x / Spring Framework 6.x / JDK 17+  
> 适用: 企业级微服务开发

---

## Table of Contents

1. [为什么需要 Spring Boot](#1-为什么需要-spring-boot)
2. [@SpringBootApplication 深度解析](#2-springbootapplication-深度解析)
3. [自动配置 (Auto-Configuration) 原理](#3-自动配置-auto-configuration-原理)
4. [Starter 机制](#4-starter-机制)
5. [自定义 Starter 开发](#5-自定义-starter-开发)
6. [外部化配置](#6-外部化配置)
7. [Profile 管理](#7-profile-管理)
8. [嵌入式服务器](#8-嵌入式服务器)
9. [日志系统](#9-日志系统)
10. [DevTools 与热部署](#10-devtools-与热部署)
11. [Actuator 生产监控](#11-actuator-生产监控)
12. [Metrics 与 Micrometer](#12-metrics-与-micrometer)
13. [Spring Boot 测试](#13-spring-boot-测试)
14. [Spring Boot 3.x 迁移指南](#14-spring-boot-3x-迁移指南)
15. [GraalVM 与 Native Image](#15-graalvm-与-native-image)
16. [面试题精选](#16-面试题精选)

---

## 1. 为什么需要 Spring Boot

### 1.1 Spring 时代的问题

在 Spring Boot 出现之前（2014 年之前），开发 Spring 应用需要：

```xml
<!-- 一个典型的 Spring MVC 项目需要大量样板配置 -->

<!-- 1. web.xml — 配置 DispatcherServlet -->
<web-app>
    <servlet>
        <servlet-name>dispatcher</servlet-name>
        <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
        <init-param>
            <param-name>contextConfigLocation</param-name>
            <param-value>/WEB-INF/spring-servlet.xml</param-value>
        </init-param>
        <load-on-startup>1</load-on-startup>
    </servlet>
    <servlet-mapping>
        <servlet-name>dispatcher</servlet-name>
        <url-pattern>/</url-pattern>
    </servlet-mapping>
    
    <!-- 字符编码 Filter -->
    <filter>
        <filter-name>encodingFilter</filter-name>
        <filter-class>org.springframework.web.filter.CharacterEncodingFilter</filter-class>
        <init-param>
            <param-name>encoding</param-name>
            <param-value>UTF-8</param-value>
        </init-param>
    </filter>
</web-app>

<!-- 2. spring-servlet.xml — Spring 配置 -->
<beans>
    <context:component-scan base-package="com.example"/>
    
    <mvc:annotation-driven/>
    
    <bean class="org.springframework.web.servlet.view.InternalResourceViewResolver">
        <property name="prefix" value="/WEB-INF/views/"/>
        <property name="suffix" value=".jsp"/>
    </bean>
    
    <!-- 数据源 -->
    <bean class="org.apache.commons.dbcp.BasicDataSource" destroy-method="close">
        <property name="driverClassName" value="com.mysql.jdbc.Driver"/>
        <property name="url" value="jdbc:mysql://localhost:3306/db"/>
        <property name="username" value="root"/>
        <property name="password" value="password"/>
    </bean>
    
    <!-- 事务管理 -->
    <bean class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
        <property name="dataSource" ref="dataSource"/>
    </bean>
    
    <!-- 还有很多很多... -->
</beans>

<!-- 3. pom.xml 需要手动管理一堆版本依赖 -->
```

**痛点总结：**

```
┌─────────────────────────────────────────────────────────────────────┐
│                  传统 Spring 应用的痛点                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  1. 配置繁琐: 大量 XML 配置，即使是简单项目也要写数百行               │
│  2. 依赖冲突: 手动管理版本号，经常遇到 jar 冲突                      │
│  3. 环境差异: 开发/测试/生产环境需要手动切换配置                      │
│  4. 部署复杂: 需要部署到外部 Tomcat，配置 JNDI 数据源                │
│  5. 监控缺失: 没有内置的健康检查、指标监控                            │
│  6. 入门困难: 新手需要理解大量概念才能写出第一个接口                  │
│  7. 依赖管理: 各个框架的版本兼容需要查阅大量文档                      │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 Spring Boot 的解决方案

```java
// ========== Spring Boot 让一切变得简单 ==========

// main 方法启动 — 不需要 web.xml，不需要外部容器
@SpringBootApplication  // 一键配置
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        // 启动：
        // 1. 创建 Spring 容器
        // 2. 自动配置嵌入式 Tomcat
        // 3. 扫描组件
        // 4. 注册所有自动配置
    }
}

// application.yml — 零 XML 配置
// server:
//   port: 8080
// spring:
//   datasource:
//     url: jdbc:mysql://localhost:3306/db

// pom.xml — 继承 starter parent，无需管理版本
// <parent>
//     <groupId>org.springframework.boot</groupId>
//     <artifactId>spring-boot-starter-parent</artifactId>
//     <version>3.2.0</version>
// </parent>
// <dependencies>
//     <dependency>
//         <groupId>org.springframework.boot</groupId>
//         <artifactId>spring-boot-starter-web</artifactId>
//     </dependency>
// </dependencies>
```

### 1.3 Spring Boot 核心特性

| 特性 | 说明 |
|------|------|
| **自动配置 (Auto-Configuration)** | 根据类路径上的依赖自动配置 Spring 应用 |
| **起步依赖 (Starters)** | 一组相关的依赖描述符，一键引入功能模块 |
| **嵌入式服务器** | 内嵌 Tomcat/Jetty/Undertow，java -jar 即可运行 |
| **外部化配置** | 17 级优先级，支持 properties/YAML/环境变量 |
| **Actuator** | 生产级监控端点，健康检查、指标、审计 |
| **Spring Boot CLI** | Groovy 脚本快速原型 (较少使用) |
| **Spring Initializr** | https://start.spring.io/ — 项目快速生成 |

### 1.4 Spring Boot 版本选择

```yaml
# 版本选择策略:
# - 使用最新的稳定版本
# - 注意与 JDK 版本的兼容性
# - 注意与 Spring Cloud 版本的兼容性

# Spring Boot 3.x: 需要 JDK 17+
# Spring Boot 2.7.x: 需要 JDK 11+ (已停止维护)
# Spring Boot 2.6.x: 需要 JDK 8+ (已停止维护)

# 版本号规则:
# 3.2.0
# │ │ │
# │ │ └── Patch (修复版本 — 每月发布)
# │ └──── Minor (功能版本 — 每季度发布)
# └────── Major (大版本 — 每 2-3 年)

# 当前版本线:
# 3.2.x — 推荐 (2024 年最新)
# 3.1.x — 维护中
# 3.0.x — 已停止维护
```

---

## 2. @SpringBootApplication 深度解析

### 2.1 组合注解结构

```java
// ========== @SpringBootApplication 展开 ==========
// @SpringBootApplication 是一个组合注解，包含三个核心注解

@SpringBootConfiguration    // 实际上就是 @Configuration
@EnableAutoConfiguration    // 启用自动配置 (最关键)
@ComponentScan             // 组件扫描 (默认扫描当前包及子包)
public @interface SpringBootApplication {
    
    // 排除指定的自动配置类
    @AliasFor(annotation = EnableAutoConfiguration.class)
    Class<?>[] exclude() default {};
    
    // 排除指定的自动配置类名
    @AliasFor(annotation = EnableAutoConfiguration.class)
    String[] excludeName() default {};
    
    // 扫描的基础包
    @AliasFor(annotation = ComponentScan.class)
    String[] scanBasePackages() default {};
    
    // 扫描的基础包类
    @AliasFor(annotation = ComponentScan.class)
    Class<?>[] scanBasePackageClasses() default {};
}
```

### 2.2 @SpringBootConfiguration

```java
// ========== @SpringBootConfiguration ==========
// 本质上就是 @Configuration
// 只是标识这是一个 "Spring Boot 配置类"
// 通常作为主配置类

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Configuration  // Spring 标准 @Configuration
@Indexed       // 支持 Spring 的索引机制 (加速扫描)
public @interface SpringBootConfiguration {
    @AliasFor(annotation = Configuration.class)
    boolean proxyBeanMethods() default true;
}
```

### 2.3 @EnableAutoConfiguration

```java
// ========== @EnableAutoConfiguration ==========
// 这是 Spring Boot 最核心的注解
// 负责启用自动配置机制

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@AutoConfigurationPackage   // 注册自动配置的基础包
@Import(AutoConfigurationImportSelector.class)  // 核心：导入自动配置选择器
public @interface EnableAutoConfiguration {
    
    // 排除指定的自动配置类
    Class<?>[] exclude() default {};
    
    // 排除指定的自动配置类名
    String[] excludeName() default {};
}
```

### 2.4 @ComponentScan

```java
// ========== @ComponentScan ==========
// 默认扫描 @SpringBootApplication 所在包及其子包

// 假设 Application 在 com.example.myapp 包下:
// com/
//   example/
//     myapp/
//       Application.java          ← 扫描起点
//       controller/               ← 会被扫描
//       service/                  ← 会被扫描
//       config/                   ← 会被扫描
//       repository/               ← 会被扫描
//     otherpackage/               ← 不会被扫描！
//       SomeComponent.java

// 解决方案 1: 指定 scanBasePackages
@SpringBootApplication(scanBasePackages = {
    "com.example.myapp",
    "com.example.common"
})

// 解决方案 2: 使用 @ComponentScan 单独指定
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.example.myapp",
    "com.example.common"
})

// 解决方案 3: 使用 scanBasePackageClasses (类型安全)
@SpringBootApplication(scanBasePackageClasses = {
    Application.class,
    CommonMarker.class  // 放在 com.example.common 包下的标记接口
})
```

### 2.5 SpringApplication 启动流程

```java
// ========== SpringApplication.run() 启动流程 ==========
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        // 完整启动流程:
        ConfigurableApplicationContext ctx = SpringApplication.run(Application.class, args);
    }
}

// 展开后:
// 1. 创建 SpringApplication 实例
//    ● 推断应用类型 (REACTIVE, SERVLET, NONE)
//    ● 读取 spring.factories → ApplicationContextInitializer
//    ● 读取 spring.factories → ApplicationListener
//    ● 推断主启动类
//
// 2. 调用 run() 方法
//    ● 创建 DefaultBootstrapContext
//    ● 设置 java.awt.headless = true
//    ● 读取 spring.factories → SpringApplicationRunListener
//    ● 启动监听器
//    ● 装配环境参数 (命令行参数、Profile)
//    ● 创建 ApplicationContext
//    ● 准备 ApplicationContext → 后置处理
//    ● 刷新 ApplicationContext (执行所有自动配置)
//    ● 刷新完成后执行 CommandLineRunner / ApplicationRunner
//    ● 发布 ApplicationReadyEvent

// ========== 自定义 SpringApplication ==========
@SpringBootApplication
public class CustomApplication {
    public static void main(String[] args) {
        // 方式 1: 使用 SpringApplication API
        SpringApplication app = new SpringApplication(CustomApplication.class);
        app.setBannerMode(Banner.Mode.OFF);        // 关闭 Banner
        app.setLazyInitialization(true);            // 全局懒加载
        app.setLogStartupInfo(false);               // 关闭启动信息
        app.addListeners(new CustomApplicationListener());
        app.setDefaultProperties(Map.of("app.name", "MyApp"));
        app.run(args);
        
        // 方式 2: 使用 Fluent API (Spring Boot 3.x)
        new SpringApplicationBuilder(CustomApplication.class)
            .bannerMode(Banner.Mode.OFF)
            .lazyInitialization(true)
            .profiles("dev")
            .listeners(new CustomApplicationListener())
            .properties("app.name=MyApp")
            .run(args);
    }
}

// ========== ApplicationRunner vs CommandLineRunner ==========
@Component
public class StartupRunner implements ApplicationRunner {
    // 推荐：参数已解析为 ApplicationArguments
    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("应用启动完成!");
        System.out.println("非选项参数: " + args.getNonOptionArgs());
        System.out.println("选项参数: " + args.getOptionNames());
        
        // --server.port=9090 --app.name=MyApp arg1 arg2
        // 非选项参数: [arg1, arg2]
        // 选项参数: [server.port, app.name]
        // args.getOptionValues("server.port") → ["9090"]
    }
}

@Component
public class LegacyRunner implements CommandLineRunner {
    // 原始字符串数组
    @Override
    public void run(String... args) throws Exception {
        System.out.println("原始参数: " + Arrays.toString(args));
    }
}
```

---

## 3. 自动配置 (Auto-Configuration) 原理

### 3.1 工作机制总览

```
┌─────────────────────────────────────────────────────────────────────┐
│               Spring Boot 自动配置工作机制                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  @EnableAutoConfiguration                                           │
│       │                                                            │
│       ▼                                                            │
│  @Import(AutoConfigurationImportSelector.class)                      │
│       │                                                            │
│       ▼                                                            │
│  AutoConfigurationImportSelector                                    │
│       │                                                            │
│       │  1. 读取 spring.factories 或                                │
│       │     META-INF/spring/org.springframework.boot.autoconfigure. │
│       │     AutoConfiguration.imports 文件                           │
│       │                                                            │
│       ▼                                                            │
│  List<AutoConfiguration> (可能 100+ 个)                             │
│       │                                                            │
│       │  2. 应用过滤条件:                                           │
│       │     ● @ConditionalOnClass — 类路径是否有某个类              │
│       │     ● @ConditionalOnMissingBean — 容器是否已存在某个 Bean    │
│       │     ● @ConditionalOnProperty — 配置项是否设置               │
│       │     ● @ConditionalOnWebApplication — 是否是 Web 应用        │
│       │     ● @ConditionalOnResource — 资源是否存在                │
│       │     ● ...                                                  │
│       │                                                            │
│       ▼                                                            │
│  Filtered AutoConfiguration List                                    │
│       │                                                            │
│       │  3. 按 AutoConfiguration 的 @AutoConfigureOrder /           │
│       │     @AutoConfigureAfter / @AutoConfigureBefore 排序         │
│       │                                                            │
│       ▼                                                            │
│  ApplicationContext.refresh() → 创建所有匹配的 Bean                  │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 3.2 自动配置注册文件

```java
// ========== 自动配置的注册方式 (版本差异) ==========

// Spring Boot 2.7 之前:
// META-INF/spring.factories
// org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
// com.example.autoconfigure.MyAutoConfiguration,\
// com.example.autoconfigure.AnotherAutoConfiguration

// Spring Boot 2.7+ (推荐方式):
// META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
// 文件内容 (一行一个):
// com.example.autoconfigure.MyAutoConfiguration
// com.example.autoconfigure.AnotherAutoConfiguration

// 同时前向兼容:
// spring.factories 仍然被支持，但推荐使用新的 imports 文件
```

### 3.3 @Conditional 条件注解族

```java
// ========== 常用 @Conditional 注解 ==========

// 1. @ConditionalOnClass — 类路径存在时加载
@Configuration
@ConditionalOnClass(name = "org.springframework.data.redis.core.RedisTemplate")
public class RedisAutoConfiguration {
    // 当类路径有 RedisTemplate 时才加载此配置
}

// 2. @ConditionalOnMissingClass — 类路径不存在时加载
@Configuration
@ConditionalOnMissingClass("com.example.legacy.LegacyService")
public class ModernServiceAutoConfiguration { }

// 3. @ConditionalOnBean / @ConditionalOnMissingBean — Bean 存在/不存在
@Configuration
public class DataSourceAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean  // 容器中没有 DataSource 时才创建
    public DataSource dataSource() {
        return new HikariDataSource();
    }
    
    @Bean
    @ConditionalOnBean(DataSource.class)  // 有 DataSource 时才创建
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}

// 4. @ConditionalOnProperty — 配置属性匹配
@Configuration
@ConditionalOnProperty(
    name = "app.feature.notification.enabled",
    havingValue = "true",      // 配置为 true 时加载
    matchIfMissing = false     // 配置不存在时不加载
)
public class NotificationAutoConfiguration { }

// 5. @ConditionalOnWebApplication — Web 应用环境
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class WebMvcAutoConfiguration {
    // 仅在 Servlet Web 应用时加载
}

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class WebFluxAutoConfiguration {
    // 仅在响应式 Web 应用时加载
}

// 6. @ConditionalOnResource — 资源存在
@Configuration
@ConditionalOnResource(resources = "classpath:logback-spring.xml")
public class LogbackAutoConfiguration { }

// 7. @ConditionalOnExpression — SpEL 表达式
@Configuration
@ConditionalOnExpression("${app.feature.enabled:false} and '${app.env}' != 'testing'")
public class FeatureAutoConfiguration { }

// 8. @ConditionalOnJava — JDK 版本
@Configuration
@ConditionalOnJava(range = ConditionalOnJava.Range.EQUAL_OR_NEWER, 
                   value = JavaVersion.SEVENTEEN)
public class VirtualThreadAutoConfiguration { }

// 9. @ConditionalOnSingleCandidate — 只有一个候选 Bean
@Configuration
public class SingleCandidateConfig {
    @Bean
    @ConditionalOnSingleCandidate(DataSource.class)
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
```

### 3.4 自动配置类源码分析

```java
// ========== 以 DataSourceAutoConfiguration 为例 ==========
// 这是 Spring Boot 中最核心的自动配置之一

@AutoConfiguration  // 标识为自动配置类 (Spring Boot 3.x)
@ConditionalOnClass({DataSource.class, EmbeddedDatabaseType.class})
// 条件: 类路径有 javax.sql.DataSource 和 EmbeddedDatabaseType
@EnableConfigurationProperties(DataSourceProperties.class)
// 绑定配置前缀: spring.datasource
@Import({DataSourcePoolMetadataProvidersConfiguration.class, 
         DataSourceInitializationConfiguration.class})
public class DataSourceAutoConfiguration {

    // 嵌入数据库配置 (H2/HSQL/ Derby)
    @Configuration
    @ConditionalOnMissingBean(DataSource.class)
    @ConditionalOnProperty(prefix = "spring.datasource", 
                          name = "url", matchIfMissing = true)
    static class EmbeddedDatabaseConfiguration {
        
        @Bean
        @ConditionalOnMissingBean
        public DataSource dataSource(DataSourceProperties properties) {
            // 创建嵌入式数据源
            return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .build();
        }
    }

    // 连接池配置 (HikariCP)
    @Configuration
    @ConditionalOnClass(HikariDataSource.class)
    @ConditionalOnMissingBean(DataSource.class)
    @ConditionalOnProperty(name = "spring.datasource.type", 
                          havingValue = "com.zaxxer.hikari.HikariDataSource",
                          matchIfMissing = true)
    static class Hikari {
        
        @Bean
        public HikariDataSource dataSource(DataSourceProperties properties) {
            return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        }
    }
}

// ========== 完整解析过程 ==========
// 1. 创建 SpringApplication 实例时，读取 spring.factories
// 2. 获取所有 EnableAutoConfiguration 配置类列表 (约 130+ 个)
// 3. 启动时，对每个配置类应用 @Conditional 条件
// 4. DataSourceAutoConfiguration 检查:
//    - 类路径有 DataSource? 是 (spring-jdbc 在 classpath)
//    - 类路径有 EmbeddedDatabaseType? 是 (spring-jdbc 包含)
//    → 条件通过，加载此配置
// 5. 内部的 Hikari 配置检查:
//    - 类路径有 HikariDataSource? 是 (spring-boot-starter-jdbc 依赖)
//    - 容器中没有自定义 DataSource? 是 (尚未创建)
//    - spring.datasource.type 未设置或为 Hikari?
//    → 条件通过，创建 HikariDataSource
// 6. 结果: 一行代码没写，我们就有了一个配置好的 HikariCP 连接池
```

### 3.5 调试自动配置

```yaml
# ========== 调试自动配置 ==========
# application.yml:
debug: true
# 或
logging:
  level:
    org.springframework.boot.autoconfigure: DEBUG

# 启动时输出:
# ============================
# AUTO-CONFIGURATION REPORT
# ============================
#
# Positive matches: (自动配置生效的)
# -----------------
# DataSourceAutoConfiguration matched:
#    - @ConditionalOnClass found required class 'javax.sql.DataSource'
#    - @ConditionalOnClass found required class 'org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType'
#
# Negative matches: (自动配置未生效的)
# -----------------
# ActiveMQAutoConfiguration:
#    Did not match:
#       - @ConditionalOnClass did not find required class 'javax.jms.ConnectionFactory'
#
# Exclusions:
# -----------
# None
```

### 3.6 排除不需要的自动配置

```java
// ========== 排除自动配置 ==========
// 有时不需要某些自动配置，需要手动排除

// 方式 1: 在 @SpringBootApplication 中排除
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,       // 不需要数据库
    SecurityAutoConfiguration.class,          // 不需要安全
    RedisAutoConfiguration.class              // 不需要 Redis
})
public class Application { }

// 方式 2: 使用 excludeName (字符串方式)
@SpringBootApplication(excludeName = {
    "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})

// 方式 3: 在 application.yml 中排除
// spring:
//   autoconfigure:
//     exclude:
//       - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
//       - org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration

// 方式 4: 使用注解排除特定配置类
@SpringBootApplication
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
public class Application { }
```

---

## 4. Starter 机制

### 4.1 Starter 命名规范

```xml
<!-- ========== Starter 命名规范 ========== -->

<!-- Spring Boot 官方 Starter -->
<!-- 命名: spring-boot-starter-{module} -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <!-- 自动包含: spring-boot-starter, spring-web, spring-webmvc,
         spring-boot-starter-tomcat, jackson-databind, ... -->
</dependency>

<!-- 社区/公司自定义 Starter -->
<!-- 命名: {module}-spring-boot-starter -->
<!-- 例如: mybatis-spring-boot-starter -->

<!-- 常见官方 Starter -->
<!--
spring-boot-starter-web           → Web 应用 (含 Tomcat, Jackson)
spring-boot-starter-webflux       → 响应式 Web
spring-boot-starter-data-jpa      → JPA + Hibernate
spring-boot-starter-data-redis    → Redis + Lettuce
spring-boot-starter-security      → Spring Security
spring-boot-starter-test          → 测试 (JUnit5, Mockito, AssertJ)
spring-boot-starter-actuator      → 监控端点
spring-boot-starter-validation    → Bean Validation
spring-boot-starter-amqp          → RabbitMQ
spring-boot-starter-mail          → 邮件发送
spring-boot-starter-thymeleaf     → Thymeleaf 模板
spring-boot-starter-aop           → AOP
-->
```

### 4.2 Starter 的依赖管理

```xml
<!-- ========== spring-boot-starter-parent 的作用 ========== -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>
</parent>

<!--
这行配置做了以下事情:
1. 设置 Java 版本为 17
2. 设置 UTF-8 编码
3. 提供 dependency-management → 管理所有依赖的版本
4. 配置插件: spring-boot-maven-plugin
5. 配置资源过滤
6. 配置 profile
-->

<!-- 如果不使用 parent，可以手动引入 BOM: -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>3.2.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

---

## 5. 自定义 Starter 开发

### 5.1 Starter 开发场景

```java
// ========== 什么时候需要自定义 Starter？ ==========

// 1. 公司内部通用组件: 日志、鉴权、ID 生成器、分布式锁
// 2. 第三方服务集成: 短信、支付、OSS 对象存储
// 3. 业务基础模块: 统一的异常处理、响应体包装

// 一个完整的 Starter 需要:
// 1. 自动配置类 (@AutoConfiguration)
// 2. 配置属性类 (@ConfigurationProperties)
// 3. spring.factories 或 AutoConfiguration.imports 注册
// 4. 可选: Actuator HealthIndicator
```

### 5.2 Starter 项目结构

```
# ========== Starter 项目结构 ==========
#
# my-redis-starter/                     ← Starter 项目
# ├── pom.xml
# ├── src/main/java/com/example/redis/
# │   ├── MyRedisAutoConfiguration.java
# │   ├── MyRedisProperties.java
# │   ├── MyRedisTemplate.java
# │   └── MyRedisHealthIndicator.java
# └── src/main/resources/
#     └── META-INF/spring/
#         └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
#
# my-app/                               ← 使用方项目
# ├── pom.xml (引入 my-redis-starter)
# └── application.yml
```

### 5.3 Starter 完整代码示例

```java
// ========== 1. 配置属性类 ==========
@ConfigurationProperties(prefix = "my.redis")
public class MyRedisProperties {
    
    private String host = "localhost";
    private int port = 6379;
    private String password;
    private int database = 0;
    private Pool pool = new Pool();
    
    // getters, setters...
    
    public static class Pool {
        private int maxActive = 8;
        private int maxIdle = 8;
        private int minIdle = 0;
        private long maxWait = -1;
        
        // getters, setters...
    }
}

// ========== 2. 核心服务类 ==========
// 注意：这个类不是 @Component，由 @Bean 创建
public class MyRedisTemplate {
    
    private final JedisPool jedisPool;
    
    public MyRedisTemplate(MyRedisProperties properties) {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(properties.getPool().getMaxActive());
        config.setMaxIdle(properties.getPool().getMaxIdle());
        config.setMinIdle(properties.getPool().getMinIdle());
        config.setMaxWaitMillis(properties.getPool().getMaxWait());
        
        this.jedisPool = new JedisPool(config,
            properties.getHost(),
            properties.getPort(),
            2000,  // connectionTimeout
            properties.getPassword(),
            properties.getDatabase());
    }
    
    public String get(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get(key);
        }
    }
    
    public void set(String key, String value) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set(key, value);
        }
    }
    
    public void set(String key, String value, long ttlSeconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(key, ttlSeconds, value);
        }
    }
    
    public void destroy() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }
}

// ========== 3. 自动配置类 ==========
@AutoConfiguration  // Spring Boot 3.x 使用 @AutoConfiguration
@EnableConfigurationProperties(MyRedisProperties.class)
// 条件：用户没有自定义 MyRedisTemplate
@ConditionalOnMissingBean(MyRedisTemplate.class)
// 条件：my.redis.enabled = true (默认 true)
@ConditionalOnProperty(prefix = "my.redis", name = "enabled", 
                      havingValue = "true", matchIfMissing = true)
public class MyRedisAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public MyRedisProperties myRedisProperties() {
        return new MyRedisProperties();
    }
    
    @Bean(destroyMethod = "destroy")  // 指定销毁方法
    @ConditionalOnMissingBean
    public MyRedisTemplate myRedisTemplate(MyRedisProperties properties) {
        return new MyRedisTemplate(properties);
    }
}

// ========== 4. 可选的 HealthIndicator ==========
@Component
@ConditionalOnClass(HealthIndicator.class)
public class MyRedisHealthIndicator implements HealthIndicator {
    
    @Autowired
    private MyRedisTemplate redisTemplate;
    
    @Override
    public Health health() {
        try {
            String pong = redisTemplate.get("health:ping");
            if ("pong".equals(pong)) {
                return Health.up()
                    .withDetail("cache", "available")
                    .build();
            }
            // 尝试写入
            redisTemplate.set("health:ping", "pong", 10);
            return Health.up()
                .withDetail("cache", "initialized")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("cache", "unavailable")
                .withException(e)
                .build();
        }
    }
}

// ========== 5. 注册自动配置 (Spring Boot 3.x) ==========
// 文件: META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
// 内容:
// com.example.redis.MyRedisAutoConfiguration

// 注册自动配置 (Spring Boot 2.x) — 兼容方式:
// 文件: META-INF/spring.factories
// 内容:
// org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
// com.example.redis.MyRedisAutoConfiguration

// ========== 6. 使用方配置 ==========
// application.yml:
// my:
//   redis:
//     enabled: true
//     host: localhost
//     port: 6379
//     password: mypassword
//     pool:
//       max-active: 20
//       max-idle: 10

// ========== 7. 使用方 pom.xml ==========
// <dependency>
//     <groupId>com.example</groupId>
//     <artifactId>my-redis-starter</artifactId>
//     <version>1.0.0</version>
// </dependency>

// ========== 8. 使用 ==========
@Service
public class UserService {
    
    @Autowired  // Starter 自动配置注入
    private MyRedisTemplate redisTemplate;
    
    public void cacheUser(User user) {
        redisTemplate.set("user:" + user.getId(), 
            JsonUtils.toJson(user), 3600);
    }
}
```

### 5.4 自动配置元数据

```xml
<!-- ========== 自动配置元数据 (IDE 提示) ========== -->
<!-- 在 src/main/resources/META-INF/ 下创建 additional-spring-configuration-metadata.json -->
{
  "properties": [
    {
      "name": "my.redis.host",
      "type": "java.lang.String",
      "description": "Redis 服务器主机地址",
      "defaultValue": "localhost"
    },
    {
      "name": "my.redis.port",
      "type": "java.lang.Integer",
      "description": "Redis 服务器端口",
      "defaultValue": 6379
    },
    {
      "name": "my.redis.pool.max-active",
      "type": "java.lang.Integer",
      "description": "连接池最大活跃连接数",
      "defaultValue": 8
    }
  ]
}

<!-- 这样用户在 IDEA 中写配置时可以获得自动提示和文档 -->
```

### 5.5 @ConditionalOnXxx 最佳实践

```java
// ========== Starter 中 @Conditional 的使用原则 ==========

// 1. 始终提供 @ConditionalOnMissingBean
//    让用户可以完全替换您的 Bean
@AutoConfiguration
public class MyServiceAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean  // 用户自定义了就不创建
    public MyService myService() {
        return new MyServiceImpl();
    }
}

// 2. 使用 @ConditionalOnProperty 控制启用/禁用
@AutoConfiguration
@ConditionalOnProperty(prefix = "my.feature", name = "enabled", 
                      havingValue = "true", matchIfMissing = true)
public class FeatureAutoConfiguration { }

// 3. 使用 @AutoConfigureAfter/@AutoConfigureBefore 控制顺序
@AutoConfiguration
@AutoConfigureAfter(DataSourceAutoConfiguration.class)  
// 在数据源配置之后执行
@AutoConfigureBefore(MyServiceAutoConfiguration.class)  
// 在 MyService 配置之前执行
public class TransactionAutoConfiguration { }

// 4. 不要过度使用条件
// 如果某个功能几乎总是需要的，可以不使用 @Conditional
```

---

## 6. 外部化配置

### 6.1 配置层级与优先级

```java
// ========== Spring Boot 外部化配置优先级 (从高到低) ==========
// 
// 1. @TestPropertySource 注解 (测试环境)
// 2. 命令行参数: --server.port=9090
// 3. SPRING_APPLICATION_JSON 环境变量
// 4. ServletConfig init parameters
// 5. ServletContext init parameters
// 6. JNDI 属性 (java:comp/env)
// 7. Java 系统属性 (System.getProperties()) — -Dkey=value
// 8. OS 环境变量 — export SERVER_PORT=9090
// 9. RandomValuePropertySource — random.*
// 10. application-{profile}.properties|yml (profile 特定)
// 11. application-{profile}.properties|yml (jar 包外的 config 目录)
// 12. application.properties|yml (jar 包外)
// 13. application.properties|yml (jar 包内)
// 14. @PropertySource 注解
// 15. SpringApplication.setDefaultProperties()
//
// 原则: 优先级高的覆盖优先级低的

// ========== 命令行参数示例 ==========
// java -jar app.jar \
//   --server.port=9090 \
//   --spring.datasource.url=jdbc:mysql://prod-db:3306/db \
//   --app.name=MyApp

// ========== 环境变量示例 (注意命名规范) ==========
// Linux/Mac:
//   export SERVER_PORT=9090
//   export SPRING_DATASOURCE_URL="jdbc:mysql://prod-db:3306/db"
// Windows (cmd):
//   set SERVER_PORT=9090
// Windows (PowerShell):
//   $env:SERVER_PORT = "9090"
```

### 6.2 application.properties vs application.yml

```yaml
# ========== application.properties ==========
# server.port=8080
# server.servlet.context-path=/api
# spring.datasource.url=jdbc:mysql://localhost:3306/db
# spring.datasource.username=root
# spring.datasource.password=secret

# ========== application.yml (推荐) ==========
# YAML 的优点:
# 1. 层次结构清晰，可读性强
# 2. 支持多文档 (--- 分隔)
# 3. 支持集合表达 (List, Map)

server:
  port: 8080
  servlet:
    context-path: /api  # 等价于 properties 的 server.servlet.context-path

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db
    username: root
    password: secret
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 20000

  jpa:
    hibernate:
      ddl-auto: validate  # none, validate, update, create, create-drop
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQLDialect

  redis:
    host: localhost
    port: 6379
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0

# 自定义配置
app:
  name: MyApp
  version: 1.0.0
  description: ${app.name} version ${app.version}  # 引用其他属性
  features:
    notification: true
    analytics: false

  # List 配置
  servers:
    - server1.example.com:8080
    - server2.example.com:8080
    - server3.example.com:8080

  # Map 配置
  timeout:
    connect: 5000
    read: 10000
    write: 5000
```

### 6.3 多文档 YAML

```yaml
# ========== 多文档 YAML ==========
# 在同一个文件中使用 --- 分隔多个 profile 配置

# 默认配置 (所有环境共享)
server:
  port: 8080
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db

---
# 开发环境
spring:
  config:
    activate:
      on-profile: dev
server:
  port: 8081
logging:
  level:
    com.example: DEBUG

---
# 生产环境
spring:
  config:
    activate:
      on-profile: prod
server:
  port: 80
logging:
  level:
    com.example: WARN
```

### 6.4 @ConfigurationProperties 绑定

```java
// ========== @ConfigurationProperties 详解 ==========

@ConfigurationProperties(prefix = "app")
@Validated
@Component  // 注册为 Bean
public class AppProperties {
    
    // 基础类型
    private String name;
    private String version;
    private String description;
    
    // 嵌套对象
    private Security security = new Security();
    
    // 集合
    private List<String> servers;
    private Map<String, Duration> timeout;
    private List<DataSourceConfig> dataSources;  // 对象列表
    
    // Duration (ISO-8601 格式: PT5S, PT10M, PT2H)
    private Duration sessionTimeout = Duration.ofMinutes(30);
    
    // DataSize
    private DataSize maxFileSize = DataSize.ofMegabytes(10);
    
    // getters, setters...
    
    public static class Security {
        private boolean enabled = true;
        private Jwt jwt = new Jwt();
        
        // getters, setters...
        
        public static class Jwt {
            private String secret;
            private long expiration = 86400000;  // 24h in ms
            
            // getters, setters...
        }
    }
    
    public static class DataSourceConfig {
        private String name;
        private String url;
        private String username;
        private String password;
        
        // getters, setters...
    }
}

// application.yml:
// app:
//   name: MyApp
//   version: 1.0.0
//   servers:
//     - server1:8080
//     - server2:8080
//   timeout:
//     connect: PT5S
//     read: PT30S
//   security:
//     enabled: true
//     jwt:
//       secret: my-secret-key
//       expiration: 3600000
//   data-sources:
//     - name: primary
//       url: jdbc:mysql://localhost:3306/db1
//       username: root
//       password: secret
//     - name: secondary
//       url: jdbc:mysql://localhost:3306/db2
//       username: root
//       password: secret
```

### 6.5 随机属性与占位符

```yaml
# ========== 随机属性生成 ==========
app:
  id: ${random.value}          # 随机 UUID (包含 -)
  secret: ${random.uuid}       # 随机 UUID
  number: ${random.int}        # 随机 int
  range: ${random.int(1000,9999)}  # 1000-9999 随机
  port: ${random.int[1024,65535]}  # 端口范围
  temperature: ${random.long}  # 随机 long
  token: ${random.alphanumeric(32)}  # 32 位随机字符串

# ========== 属性占位符引用 ==========
app:
  name: MyApp
  description: "Application: ${app.name}"  # 引用其他属性
  home-dir: ${user.home}/app               # 系统属性
  java-home: ${JAVA_HOME:default/path}     # 环境变量 + 默认值

# ========== 默认值 ==========
app:
  port: ${APP_PORT:8080}              # 环境变量 APP_PORT, 默认 8080
  timeout: ${app.timeout.connect:3000} # 引用 + 默认值
```

---

## 7. Profile 管理

### 7.1 Profile 基本用法

```java
// ========== Profile 概念 ==========
// Profile 是 Spring 为不同环境提供不同配置的机制
// 典型环境: dev (开发), test (测试), staging (预发布), prod (生产)

// ========== 1. Profile 特定配置文件 ==========
// 命名: application-{profile}.yml
// 
// application.yml           ← 基础配置 (所有环境共享)
// application-dev.yml       ← 开发环境配置
// application-test.yml      ← 测试环境配置
// application-prod.yml      ← 生产环境配置
//
// 加载顺序: application.yml + application-{profile}.yml
// Profile 配置覆盖基础配置

// application.yml (基础):
server:
  port: 8080
spring:
  profiles:
    active: dev

// application-dev.yml:
server:
  port: 8081
logging:
  level:
    com.example: DEBUG

// application-prod.yml:
server:
  port: 80
logging:
  level:
    com.example: WARN

// ========== 2. @Profile 注解 ==========
@Configuration
@Profile("dev")  // 仅在 dev 环境加载
public class DevConfig {
    
    @Bean
    public DataSource devDataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .build();
    }
}

@Service
@Profile("prod")  // 仅在 prod 环境加载
public class ProdNotificationService implements NotificationService {
    // 生产环境的真实通知服务
}

@Service
@Profile("dev")  // 仅在 dev 环境加载
public class DevNotificationService implements NotificationService {
    // 开发环境的模拟通知服务
}

// ========== 3. 激活 Profile ==========
// 方式 1: application.yml
// spring:
//   profiles:
//     active: dev,redis  # 同时激活多个

// 方式 2: 命令行参数
// --spring.profiles.active=dev,redis

// 方式 3: 环境变量
// SPRING_PROFILES_ACTIVE=prod,redis

// 方式 4: Java 系统属性
// -Dspring.profiles.active=dev

// 方式 5: 编程方式
// SpringApplication.setAdditionalProfiles("dev")
```

### 7.2 高级 Profile 技巧

```java
// ========== Profile 组 (Spring Boot 2.4+) ==========
// application.yml:
// spring:
//   profiles:
//     group:
//       dev: [dev, dev-db, dev-redis]     # 激活 dev 时同时激活 dev-db, dev-redis
//       prod: [prod, prod-db, prod-redis]

// ========== Profile 条件表达式 ==========
// 否定
@Profile("!dev")  // 非 dev 环境

// 且关系
@Profile("dev & redis")  // dev AND redis 都激活

// 或关系
@Profile("dev | test")   // dev OR test 至少一个激活

// ========== 特定文档的 Profile 配置 (YAML) ==========
// 不再推荐使用 spring.profiles 标识符 (2.4+)
// 使用 spring.config.activate.on-profile:
// spring:
//   config:
//     activate:
//       on-profile: dev
// server:
//   port: 8081

// ========== 编程方式获取当前 Profile ==========
@Component
public class ProfileAwareService {
    
    @Autowired
    private Environment environment;
    
    public boolean isDev() {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }
    
    public String[] getActiveProfiles() {
        return environment.getActiveProfiles();
    }
    
    // 默认 profile
    public String[] getDefaultProfiles() {
        return environment.getDefaultProfiles();
    }
}
```

---

## 8. 嵌入式服务器

### 8.1 支持的服务器

```xml
<!-- ========== Spring Boot 支持的嵌入式服务器 ========== -->

<!-- 默认: Tomcat (spring-boot-starter-web 包含) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <!-- 自动包含 spring-boot-starter-tomcat -->
</dependency>

<!-- 切换为 Jetty -->
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

<!-- 切换为 Undertow (推荐, 性能好) -->
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

### 8.2 服务器配置

```yaml
# ========== 服务器配置 ==========
server:
  # 端口
  port: 8080
  
  # 随机端口 (测试使用)
  # port: 0
  
  # 绑定地址 (默认 0.0.0.0)
  address: 0.0.0.0
  
  # 上下文路径
  servlet:
    context-path: /api
    # Session 配置
    session:
      timeout: 30m  # 30 分钟 (支持: s, m, h, d)
      cookie:
        http-only: true
        secure: false
        max-age: 1800  # 30 分钟
  
  # 连接配置
  connection-timeout: 5s  # 请求超时
  max-connections: 8192   # 最大连接数
  
  # Tomcat 特定配置
  tomcat:
    max-threads: 200       # 最大工作线程数
    min-spare-threads: 10  # 最小空闲线程数
    max-connections: 8192  # 最大连接数
    accept-count: 100      # 等待队列长度
    connection-timeout: 20s # 连接超时
    max-swallow-size: 2MB
    uri-encoding: UTF-8
    # 访问日志
    accesslog:
      enabled: true
      directory: logs
      pattern: "%h %l %u %t \"%r\" %s %b %D"
      # %h - 远程主机, %l - 标识符, %u - 用户名
      # %t - 时间, %r - 请求行, %s - 状态码
      # %b - 响应大小, %D - 处理时间(毫秒)
    # 压缩
    compression:
      enabled: true
      min-response-size: 1024  # 大于 1KB 才压缩
      mime-types: text/html,text/xml,text/plain,text/css,application/json,application/javascript
  
  # Undertow 特定配置
  undertow:
    threads:
      io: 4         # IO 线程数 (CPU 核心数)
      worker: 32    # 工作线程数
    buffer-size: 1024
    direct-buffers: true
    accesslog:
      enabled: true
      dir: logs
      pattern: common

# ========== SSL/TLS 配置 ==========
server:
  port: 443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: secret
    key-store-type: PKCS12
    key-alias: tomcat
    # 或者使用 PEM 格式
    # certificate: classpath:cert.pem
    # certificate-private-key: classpath:key.pem
```

### 8.3 编程化服务器配置

```java
// ========== 编程式配置 ==========
@Configuration
public class ServerConfig {
    
    // 自定义 Tomcat 配置
    @Bean
    public TomcatServletWebServerFactory tomcatFactory() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        
        // 添加自定义 Connector 配置
        factory.addConnectorCustomizers(connector -> {
            connector.setProperty("address", "0.0.0.0");
            connector.setMaxSwallowSize(-1);  // 不限制 swallow
        });
        
        // 添加错误页面
        factory.addErrorPages(new ErrorPage(HttpStatus.NOT_FOUND, "/404.html"));
        factory.addErrorPages(new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/500.html"));
        
        // 404 处理
        // Spring Boot 默认:
        // - 如果是浏览器访问: 返回 Whitelabel Error Page
        // - 如果是 API 访问: 返回 JSON 错误
        
        return factory;
    }
    
    // 编程方式设置 SSL (从外部源加载证书)
    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        
        SslStoreProvider sslStoreProvider = new SslStoreProvider() {
            @Override
            public KeyStore getKeyStore() throws Exception {
                // 从配置中心或数据库加载 KeyStore
                return loadKeyStore();
            }
            
            @Override
            public KeyStore getTrustStore() throws Exception {
                return loadTrustStore();
            }
        };
        
        tomcat.setSslStoreProvider(sslStoreProvider);
        return tomcat;
    }
}
```

---

## 9. 日志系统

### 9.1 Spring Boot 日志架构

```yaml
# ========== Spring Boot 日志架构 ==========
#
# SLF4J (Simple Logging Facade for Java) — 日志门面
#   │
#   ├──→ Logback (默认实现) — Spring Boot 默认
#   ├──→ Log4j2 (可切换, 性能更好)
#   └──→ JUL (Java Util Logging)
#
# 选择标准:
# - Logback: 默认, 足够好, 零配置可用
# - Log4j2: 异步日志性能更好, 支持更多 Appender
# - JUL: JDK 内置, 功能有限
#
# spring-boot-starter-web 包含:
#   spring-boot-starter-logging → logback-classic → SLF4J + Logback

# ========== 基础日志配置 ==========
logging:
  level:
    root: INFO                     # 根日志级别
    com.example: DEBUG             # 特定包级别
    org.springframework: WARN      # Spring 框架日志
    org.hibernate.SQL: DEBUG       # Hibernate SQL 日志
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE  # SQL 参数
  
  # 文件输出
  file:
    name: logs/myapp.log          # 日志文件路径
    max-size: 100MB               # 每个文件最大大小
    max-history: 30               # 保留天数
    total-size-cap: 1GB           # 总大小上限
  
  # 日志格式
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
```

### 9.2 Logback 配置 (logback-spring.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!-- logback-spring.xml — 支持 Spring Profile 扩展的 Logback 配置 -->
<configuration scan="true" scanPeriod="60 seconds">
    
    <!-- 引入 Spring Boot 默认配置 -->
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    
    <!-- 控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <!-- %d 日期, %thread 线程, %-5level 级别, %logger 日志器, %msg 消息, %n 换行 -->
            <pattern>${CONSOLE_LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>
    
    <!-- 滚动文件输出 -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH:-logs}/${APP_NAME:-app}.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!-- 按天滚动，压缩旧日志 -->
            <fileNamePattern>${LOG_PATH:-logs}/${APP_NAME:-app}.%d{yyyy-MM-dd}.%i.gz</fileNamePattern>
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>100MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
            <!-- 保留 30 天 -->
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>${FILE_LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>
    
    <!-- 错误日志单独输出 -->
    <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH:-logs}/${APP_NAME:-app}-error.log</file>
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>ERROR</level>
        </filter>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH:-logs}/${APP_NAME:-app}-error.%d{yyyy-MM-dd}.%i.gz</fileNamePattern>
            <maxHistory>60</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>${FILE_LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>
    
    <!-- JSON 格式输出 (用于 ELK 收集) -->
    <appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH:-logs}/${APP_NAME:-app}-json.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH:-logs}/${APP_NAME:-app}-json.%d{yyyy-MM-dd}.%i.gz</fileNamePattern>
            <maxHistory>7</maxHistory>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    </appender>
    
    <!-- 异步输出 (提高性能) -->
    <appender name="ASYNC_FILE" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="FILE"/>
        <queueSize>512</queueSize>
        <discardingThreshold>0</discardingThreshold>
    </appender>
    
    <!-- Spring Profile 特定配置 -->
    <springProfile name="dev">
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
            <appender-ref ref="FILE"/>
        </root>
        <logger name="com.example" level="DEBUG"/>
    </springProfile>
    
    <springProfile name="prod">
        <root level="WARN">
            <appender-ref ref="ASYNC_FILE"/>
            <appender-ref ref="JSON_FILE"/>
        </root>
        <logger name="com.example" level="INFO"/>
        <logger name="org.springframework" level="WARN"/>
    </springProfile>
    
</configuration>
```

### 9.3 MDC (Mapped Diagnostic Context)

```java
// ========== MDC — 请求链路追踪 ==========
// MDC 可以在日志中注入额外的上下文信息
// 典型用途: traceId, userId, requestId

// 1. 在拦截器中设置 MDC
@Component
public class MDCInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) {
        // 生成或获取 traceId
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        
        // 设置 MDC
        MDC.put("traceId", traceId);
        MDC.put("userId", request.getAttribute("currentUserId") != null ? 
                request.getAttribute("currentUserId").toString() : "anonymous");
        MDC.put("requestURI", request.getRequestURI());
        
        // 设置响应头 (前端可以获取)
        response.setHeader("X-Trace-Id", traceId);
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, 
                               HttpServletResponse response, 
                               Object handler, Exception ex) {
        // 清理 MDC (防止线程池污染)
        MDC.clear();
    }
}

// 2. 在日志格式中使用 MDC
// logback-spring.xml:
// <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{traceId}] %-5level %logger{36} - %msg%n</pattern>
// 输出: 2024-01-15 10:30:00.123 [http-nio-8080-exec-1] [abc123...] INFO c.e.s.UserService - 用户查询

// 3. 自动配置 MDC 注入
@Configuration
public class MDCAutoConfiguration {
    
    @Bean
    public TaskDecorator mdcTaskDecorator() {
        // 确保异步任务也继承 MDC 上下文
        return runnable -> {
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            return () -> {
                try {
                    MDC.setContextMap(contextMap);
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            };
        };
    }
    
    @Bean
    public Executor mdcAwareExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setTaskDecorator(mdcTaskDecorator());
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("mdc-async-");
        executor.initialize();
        return executor;
    }
}

// 4. @Async 也支持 MDC (需要配置 TaskDecorator)
@EnableAsync
@Configuration
public class AsyncConfig implements AsyncConfigurer {
    
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setTaskDecorator(runnable -> {
            Map<String, String> context = MDC.getCopyOfContextMap();
            return () -> {
                try {
                    MDC.setContextMap(context);
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            };
        });
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.initialize();
        return executor;
    }
}
```

---

## 10. DevTools 与热部署

### 10.1 DevTools 配置

```xml
<!-- ========== DevTools 依赖 ========== -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <optional>true</optional>  <!-- 防止传递到其他模块 -->
</dependency>
```

```yaml
# ========== DevTools 配置 ==========
spring:
  devtools:
    restart:
      enabled: true           # 启用热重启
      exclude: static/**,public/**,resources/**  # 排除的目录
      additional-paths: src/main/java  # 额外监听的路径
      additional-exclude: src/main/resources/templates/**
      poll-interval: 2s       # 轮询间隔
      quiet-period: 400ms     # 静默期 (避免多次触发)
      log-condition-evaluation-delta: false  # 关闭条件评估日志
    livereload:
      enabled: true           # LiveReload 服务器 (浏览器自动刷新)
      port: 35729
    remote:
      secret: my-secret       # 远程调试密钥 (慎用)

# ========== DevTools 原理 ==========
# DevTools 使用了两个 ClassLoader:
# - Base ClassLoader: 加载第三方 jar (不常变化)
# - Restart ClassLoader: 加载项目类 (频繁变化)
# 当检测到文件变化时, 丢弃旧的 Restart ClassLoader, 创建新的
# 这比重启 JVM 快得多 (跳过 jar 加载)
```

### 10.2 手动触发重启

```java
// ========== 手动触发重启 ==========
// DevTools 监控 classpath 文件变化

// 触发条件 (任何 classpath 文件被修改):
// 1. Java 文件改动 → IDE 自动编译 → 触发重启
// 2. resources 文件改动 → 触发重启
// 3. 静态资源 (css/js/html) → 默认不触发

// 在 IntelliJ IDEA 中:
// 1. File → Settings → Build, Execution, Deployment → Compiler
//    → 勾选 "Build project automatically"
// 2. Ctrl+Shift+A → "Registry..." → 勾选 compiler.automake.allow.when.app.running

// 使用 Spring DevTools 的 Remote 模式 (生产环境慎用):
// java -jar app.jar -Dspring.devtools.remote.secret=secret
```

---

## 11. Actuator 生产监控

### 11.1 启用 Actuator

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

```yaml
# ========== Actuator 配置 ==========
management:
  # 端点暴露配置
  endpoints:
    web:
      exposure:
        include: health,info,metrics,env,beans,loggers,mappings,threaddump,heapdump
        exclude: shutdown  # 禁用 shutdown 端点
      base-path: /actuator  # 端点路径前缀 (默认)
  
  # 端点详细度
  endpoint:
    health:
      show-details: when-authorized  # always, never, when-authorized
      show-components: when-authorized
      roles: ADMIN
    env:
      show-values: when-authorized
    configprops:
      show-values: when-authorized
    
  # 信息端点
  info:
    env:
      enabled: true
    build:
      enabled: true
    git:
      enabled: true
      mode: full  # simple | full
    java:
      enabled: true
    os:
      enabled: true
  
  # 指标
  metrics:
    tags:
      application: ${spring.application.name:unknown}
    export:
      prometheus:
        enabled: true
```

### 11.2 核心端点详解

```yaml
# ========== 核心端点功能 ==========

# /actuator/health — 健康检查
# GET /actuator/health
# {
#   "status": "UP",
#   "components": {
#     "db": {
#       "status": "UP",
#       "details": { "database": "MySQL", "result": 1 }
#     },
#     "redis": {
#       "status": "UP",
#       "details": { "version": "7.0.0" }
#     },
#     "diskSpace": {
#       "status": "UP",
#       "details": { "total": 500000000000, "free": 200000000000 }
#     }
#   }
# }

# /actuator/info — 应用信息
# GET /actuator/info
# {
#   "build": {
#     "artifact": "my-app",
#     "version": "1.0.0",
#     "time": "2024-01-15T10:00:00Z"
#   },
#   "git": {
#     "commit": { "id": "abc123", "time": "2024-01-15T09:00:00Z" }
#   },
#   "java": { "version": "17.0.9" },
#   "os": { "name": "Linux", "version": "5.15.0" }
# }

# /actuator/metrics — 应用指标
# GET /actuator/metrics/jvm.memory.used
# {
#   "name": "jvm.memory.used",
#   "measurements": [{ "statistic": "VALUE", "value": 536870912 }],
#   "availableTags": [
#     { "tag": "area", "values": ["heap", "nonheap"] },
#     { "tag": "id", "values": ["G1 Eden Space", "G1 Old Gen"] }
#   ]
# }

# /actuator/env — 环境属性
# GET /actuator/env
# {
#   "activeProfiles": ["dev"],
#   "propertySources": [
#     { "name": "server.ports", "properties": { "local.server.port": 8080 }},
#     { "name": "systemEnvironment", "properties": { "PATH": "/usr/bin" }}
#   ]
# }

# /actuator/loggers — 动态修改日志级别 (运行时!)
# GET /actuator/loggers/com.example → { "configuredLevel": null, "effectiveLevel": "DEBUG" }
# POST /actuator/loggers/com.example { "configuredLevel": "WARN" } → 即时生效！

# /actuator/threaddump — 线程快照 (排查死锁)
# GET /actuator/threaddump → 所有线程的状态、堆栈

# /actuator/heapdump — 堆转储 (OOM 分析)
# GET /actuator/heapdump → 下载 heapdump 文件

# /actuator/beans — 所有 Bean 列表
# GET /actuator/beans → 所有 Bean 的名称、类型、依赖

# /actuator/mappings — URL 映射
# GET /actuator/mappings → 所有 @RequestMapping 映射

# /actuator/configprops — 配置属性
# GET /actuator/configprops → 所有 @ConfigurationProperties
```

### 11.3 自定义 HealthIndicator

```java
// ========== 自定义健康检查 ==========
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    @Autowired
    private DataSource dataSource;
    
    @Override
    public Health health() {
        try (Connection conn = dataSource.getConnection()) {
            // 执行验证查询
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SELECT 1");
            }
            
            return Health.up()
                    .withDetail("database", conn.getMetaData().getDatabaseProductName())
                    .withDetail("url", conn.getMetaData().getURL())
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withException(e)
                    .build();
        }
    }
}

// ========== 更加复杂的 HealthIndicator ==========
@Component
public class ExternalServiceHealthIndicator extends AbstractHealthIndicator {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        // 检查外部服务
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                "https://api.example.com/health", String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                builder.up()
                    .withDetail("service", "example-api")
                    .withDetail("statusCode", response.getStatusCodeValue());
            } else {
                builder.down()
                    .withDetail("service", "example-api")
                    .withDetail("statusCode", response.getStatusCodeValue());
            }
        } catch (Exception e) {
            builder.down(e)
                    .withDetail("service", "example-api");
        }
    }
}

// ========== Composite HealthIndicator (聚合多个检查) ==========
@Component
public class MySystemHealthIndicator implements ReactiveHealthIndicator {
    
    @Autowired
    private List<HealthIndicator> indicators;
    
    @Override
    public Mono<Health> health() {
        return Flux.fromIterable(indicators)
                .flatMap(indicator -> Mono.just(indicator.health()))
                .reduce(new Health.Builder(), (builder, health) -> {
                    builder.withDetail(health.getStatus().getCode(), health.getDetails());
                    return builder;
                })
                .map(builder -> builder.build());
    }
}
```

### 11.4 自定义 Info 端点

```java
// ========== 自定义 Info 端点 ==========
// 方式 1: 实现 InfoContributor 接口
@Component
public class CustomInfoContributor implements InfoContributor {
    
    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("app", Map.of(
            "name", "My Spring Boot Application",
            "version", "1.0.0",
            "description", "Enterprise-grade application"
        ));
        
        builder.withDetail("deploy", Map.of(
            "environment", System.getenv("ENV") != null ? 
                System.getenv("ENV") : "local",
            "host", System.getenv("HOSTNAME") != null ? 
                System.getenv("HOSTNAME") : "unknown"
        ));
    }
}

// 方式 2: 在 application.yml 中配置
// info:
//   app:
//     name: MyApp
//     version: 1.0.0
//   build:
//     artifact: "@project.artifactId@"  # Maven 变量
//     version: "@project.version@"

// 方式 3: Maven 构建时生成 build-info
// <plugin>
//     <groupId>org.springframework.boot</groupId>
//     <artifactId>spring-boot-maven-plugin</artifactId>
//     <executions>
//         <execution>
//             <goals>
//                 <goal>build-info</goal>
//             </goals>
//         </execution>
//     </executions>
// </plugin>
```

---

## 12. Metrics 与 Micrometer

### 12.1 Micrometer 架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                  Micrometer 指标架构                                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  Application Code                                                   │
│       │                                                            │
│       ▼                                                            │
│  Micrometer (门面)                                                  │
│  ├── MeterRegistry (指标注册表)                                     │
│  ├── Meter (指标)                                                   │
│  │   ├── Counter     (计数器: 请求总数, 错误数)                     │
│  │   ├── Gauge       (仪表: 内存使用, 线程池大小)                    │
│  │   ├── Timer       (计时器: 请求延迟, SQL 执行时间)                │
│  │   ├── DistributionSummary (分布: 响应大小, 价格分布)              │
│  │   └── LongTaskTimer (长任务计时器: 备份任务持续时间)              │
│  │                                                                 │
│  └── 绑定器 (Binders)                                              │
│      ├── JvmGcMetrics, JvmMemoryMetrics, JvmThreadMetrics          │
│      ├── TomcatMetrics, JettyMetrics                                │
│      ├── DatabaseConnectionPoolMetrics                              │
│      └── ...                                                       │
│                                                                     │
│  Exporters (导出)                                                   │
│  ├── PrometheusMeterRegistry → /actuator/prometheus                │
│  ├── GraphiteMeterRegistry → Graphite                               │
│  ├── DatadogMeterRegistry → Datadog                                 │
│  └── ...                                                           │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 12.2 自定义 Metrics

```java
// ========== 自定义指标 ==========
@Service
public class MetricsService {
    
    private final Counter orderCounter;
    private final Timer orderTimer;
    private final DistributionSummary orderAmountSummary;
    private final Gauge pendingOrdersGauge;
    private final AtomicLong pendingOrders = new AtomicLong(0);
    
    public MetricsService(MeterRegistry registry) {
        // Counter: 只增不减
        this.orderCounter = Counter.builder("orders.created")
                .tag("service", "order-service")
                .description("Total number of created orders")
                .register(registry);
        
        // Timer: 记录耗时分布
        this.orderTimer = Timer.builder("orders.processing.time")
                .tag("service", "order-service")
                .description("Order processing time")
                .publishPercentiles(0.5, 0.95, 0.99)  // P50, P95, P99
                .publishPercentileHistogram()
                .sla(Duration.ofMillis(100), Duration.ofMillis(500), 
                     Duration.ofSeconds(1), Duration.ofSeconds(3))
                .register(registry);
        
        // DistributionSummary: 数值分布
        this.orderAmountSummary = DistributionSummary.builder("orders.amount")
                .tag("currency", "CNY")
                .description("Order amount distribution")
                .baseUnit("yuan")
                .publishPercentiles(0.5, 0.9, 0.99)
                .minimumExpectedValue(1.0)
                .maximumExpectedValue(100000.0)
                .register(registry);
        
        // Gauge: 可增可减的仪表
        this.pendingOrdersGauge = Gauge.builder("orders.pending")
                .tag("service", "order-service")
                .description("Number of pending orders")
                .register(registry, pendingOrders, AtomicLong::doubleValue);
    }
    
    @Timed(value = "orders.create", description = "Time taken to create an order")
    @Transactional
    public Order createOrder(OrderRequest request) {
        return orderTimer.record(() -> {
            // 业务逻辑
            Order order = doCreateOrder(request);
            
            // 记录计数器
            orderCounter.increment();
            
            // 记录金额分布
            orderAmountSummary.record(request.getAmount().doubleValue());
            
            return order;
        });
    }
    
    public void afterOrderPaid(Long orderId) {
        pendingOrders.decrementAndGet();
    }
    
    @Timed(value = "orders.batch", longTask = true)
    public void batchProcessOrders() {
        // 批量处理订单 (长任务)
        // longTask = true 表示记录持续运行时间
    }
    
    // 直接使用 Meter
    @Autowired
    private MeterRegistry registry;
    
    public void customMetric() {
        Counter counter = Counter.builder("custom.counter")
                .tag("method", "customMetric")
                .register(registry);
        counter.increment();
    }
}
```

### 12.3 Prometheus 集成

```yaml
# ========== Prometheus 集成配置 ==========
management:
  endpoints:
    web:
      exposure:
        include: prometheus  # 暴露 Prometheus 端点
  metrics:
    export:
      prometheus:
        enabled: true
        pushgateway:
          enabled: false
    tags:
      application: ${spring.application.name:myapp}
      instance: ${HOSTNAME:localhost}
```

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```yaml
# Prometheus 配置 (prometheus.yml):
# scrape_configs:
#   - job_name: 'spring-boot-app'
#     metrics_path: '/actuator/prometheus'
#     static_configs:
#       - targets: ['localhost:8080']
```

### 12.4 @Timed 注解

```java
// ========== @Timed 注解 ==========
// @Timed 可以自动记录方法执行时间

@RestController
@RequestMapping("/api/v1/orders")
@Timed  // 类级别: 所有方法都记录
public class OrderController {
    
    @PostMapping
    @Timed(value = "orders.api.create", 
           percentiles = {0.5, 0.95, 0.99},
           description = "Create order API")
    public Result<Order> create(@RequestBody @Valid OrderCreateRequest request) {
        return Result.success(orderService.create(request));
    }
    
    @GetMapping("/{id}")
    @Timed("orders.api.getById")
    public Result<Order> getById(@PathVariable Long id) {
        return Result.success(orderService.findById(id));
    }
}

// 启用 @Timed:
@Configuration
public class TimedConfiguration {
    
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}
```

---

## 13. Spring Boot 测试

### 13.1 测试层次

```java
// ========== Spring Boot 测试层次 ==========
//
// 层次 1: 单元测试 (无 Spring 上下文)
//   测试 Service/工具类
//   使用 Mockito, JUnit 5
//   @Test, @Mock, @InjectMocks
//
// 层次 2: Slice Test (部分 Spring 上下文)
//   @WebMvcTest — 只加载 Web 层
//   @DataJpaTest — 只加载 JPA 层
//   @JsonTest — JSON 序列化测试
//   @RestClientTest — REST 客户端测试
//
// 层次 3: 集成测试 (完整 Spring 上下文)
//   @SpringBootTest — 加载完整应用
//   可以使用 @Testcontainers
```

### 13.2 @SpringBootTest

```java
// ========== @SpringBootTest ==========
// 加载完整的应用上下文

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
// webEnvironment 选项:
//   MOCK (默认): 加载 Mock Servlet 环境, 不启动真实服务器
//   RANDOM_PORT: 启动真实服务器, 随机端口
//   DEFINED_PORT: 使用配置的端口
//   NONE: 不加载 Web 环境
class FullIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private ApplicationContext context;
    
    @LocalServerPort
    private int port;
    
    @Test
    void contextLoads() {
        // 验证应用上下文加载成功
        assertThat(context).isNotNull();
    }
    
    @Test
    void shouldReturnHealth() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/actuator/health", String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }
}

// ========== 自定义测试配置 ==========
@SpringBootTest
@TestPropertySource(properties = {
    "app.feature.enabled=false",    // 覆盖配置
    "spring.datasource.url=jdbc:h2:mem:testdb"
})
// 或者使用 properties 属性:
// @SpringBootTest(properties = "app.feature.enabled=false")
class PropertyOverrideTest { }

// ========== 测试 Profile ==========
@SpringBootTest
@ActiveProfiles("test")  // 激活 test profile
class ProfileTest { }
```

### 13.3 测试切片 (Slice Tests)

```java
// ========== @WebMvcTest — Web 层测试 ==========
@WebMvcTest(UserController.class)  // 只加载 UserController
class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean  // Mock Service 层
    private UserService userService;
    
    @Test
    void shouldReturnUsers() throws Exception {
        when(userService.findAll(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(new User(1L, "John"))));
        
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("John"));
    }
}

// ========== @DataJpaTest — JPA 层测试 ==========
@DataJpaTest  // 只加载 JPA Repository + Entity
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
// 使用嵌入式数据库 (H2)
class UserRepositoryTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Test
    void shouldFindByEmail() {
        // 使用 entityManager 准备数据
        User user = new User();
        user.setEmail("test@example.com");
        user.setName("Test User");
        entityManager.persistAndFlush(user);
        
        // 测试 Repository
        Optional<User> found = userRepository.findByEmail("test@example.com");
        
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test User");
    }
}

// ========== @JsonTest — JSON 序列化测试 ==========
@JsonTest
class UserJsonTest {
    
    @Autowired
    private JacksonTester<User> json;
    
    @Test
    void shouldSerializeUser() throws Exception {
        User user = new User(1L, "John", "john@example.com");
        
        // 序列化验证
        assertThat(json.write(user))
            .hasJsonPathStringValue("$.name")
            .extractingJsonPathStringValue("$.name")
            .isEqualTo("John");
        
        // 反序列化验证
        String jsonStr = "{\"id\":1,\"name\":\"John\",\"email\":\"john@example.com\"}";
        assertThat(json.parse(jsonStr).getObject())
            .hasFieldOrPropertyWithValue("name", "John");
    }
}

// ========== @RestClientTest — REST 客户端测试 ==========
@RestClientTest(UserApiClient.class)
class UserApiClientTest {
    
    @Autowired
    private MockRestServiceServer server;
    
    @Autowired
    private UserApiClient client;
    
    @Test
    void shouldGetUser() {
        server.expect(requestTo("/api/users/1"))
                .andRespond(withSuccess(
                    "{\"id\":1,\"name\":\"John\"}",
                    MediaType.APPLICATION_JSON));
        
        User user = client.getUserById(1L);
        assertThat(user.getName()).isEqualTo("John");
    }
}
```

### 13.4 Testcontainers 集成测试

```java
// ========== Testcontainers ==========
// 使用 Docker 容器运行真实的数据库/中间件进行测试

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class RealDatabaseIntegrationTest {
    
    // MySQL 容器
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    // Redis 容器
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);
    
    // 动态设置配置
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
        
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void shouldPersistAndRetrieveUser() {
        // 创建用户
        UserCreateRequest request = new UserCreateRequest();
        request.setName("Test");
        request.setEmail("test@example.com");
        
        ResponseEntity<Result> response = restTemplate.postForEntity(
            "/api/v1/users", request, Result.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // 验证数据真的写入数据库
        User saved = userRepository.findByEmail("test@example.com").orElseThrow();
        assertThat(saved.getName()).isEqualTo("Test");
    }
}
```

### 13.5 @MockBean / @SpyBean

```java
// ========== @MockBean vs @SpyBean ==========

@Service
public class PaymentService {
    
    public boolean processPayment(Order order) {
        // 调用外部支付网关
        return externalPaymentGateway(order);
    }
    
    private boolean externalPaymentGateway(Order order) {
        // 真实调用 — 测试时要 Mock
        return true;
    }
}

@SpringBootTest
class OrderServiceTest {
    
    // @MockBean: 完全替换 Bean, 所有方法返回默认值
    @MockBean
    private PaymentService paymentService;
    
    // @SpyBean: 保留真实实现, 只 Stub 特定方法
    @SpyBean
    private OrderService orderService;
    
    @Autowired
    private InventoryService inventoryService;
    
    @Test
    void shouldUseMock() {
        // @MockBean 替换了 paymentService
        when(paymentService.processPayment(any())).thenReturn(true);
        
        // 业务测试...
    }
    
    @Test
    void shouldUseSpy() {
        // @SpyBean 保留了真实的 orderService
        // 但可以 Stub 特定方法
        doNothing().when(orderService).sendNotification(any());
        
        // 调用真实方法
        orderService.createOrder(new Order());
        
        // 验证 sendNotification 没有被调用 (被 Spy 替换)
        verify(orderService, never()).sendNotification(any());
    }
}
```

---

## 14. Spring Boot 3.x 迁移指南

### 14.1 主要变化

```java
// ========== Spring Boot 3.x 主要变化 ==========

// 1. Java 17 基线
//    ● 最低要求 JDK 17
//    ● 推荐使用 JDK 21 LTS

// 2. Jakarta EE 9+ 迁移
//    ● javax.* → jakarta.*
//    ● javax.servlet → jakarta.servlet
//    ● javax.persistence → jakarta.persistence
//    ● javax.validation → jakarta.validation
//    ● javax.transaction → jakarta.transaction

// 从 2.x 迁移 3.x 的代码变更:
// Spring Boot 2.x (javax.*):
import javax.persistence.Entity;
import javax.validation.Valid;
import javax.servlet.http.HttpServletRequest;

// Spring Boot 3.x (jakarta.*):
import jakarta.persistence.Entity;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

// 3. Hibernate 6.x (从 5.x 升级)
//    ● javax.persistence → jakarta.persistence
//    ● 一些 SQL 方言变更
//    ● 一些类型映射变更

// 4. Spring Security 6.x
//    ● 配置方法链变化 (废弃 WebSecurityConfigurerAdapter)
//    ● Lambda DSL 成为标准

// 5. 其他变更
//    ● Spring Framework 6.x
//    ● RestTemplate 标记为 @Deprecated (推荐 WebClient/RestClient)
//    ● 移除 2.x 中废弃的类和功能
//    ● Spring Data 3.x
//    ● Actuator 端点变更
```

### 14.2 迁移清单

```java
// ========== 迁移核对清单 ==========

// ☐ 1. JDK 升级到 17+
//     java -version → 17.0.9+

// ☐ 2. 更新 pom.xml
//     <parent>
//         <groupId>org.springframework.boot</groupId>
//         <artifactId>spring-boot-starter-parent</artifactId>
//         <version>3.2.0</version>  <!-- 从 2.7.x 升级 -->
//     </parent>

// ☐ 3. 替换所有 javax.* 为 jakarta.*
//     find . -name "*.java" -exec sed -i 's/javax\.persistence/jakarta.persistence/g' {} \;
//     find . -name "*.java" -exec sed -i 's/javax\.validation/jakarta.validation/g' {} \;
//     find . -name "*.java" -exec sed -i 's/javax\.servlet/jakarta.servlet/g' {} \;
//     find . -name "*.java" -exec sed -i 's/javax\.transaction/jakarta.transaction/g' {} \;

// ☐ 4. 检查废弃 API 使用
//     - RestTemplate → 考虑替换为 WebClient/RestClient
//     - AbstractSecurityWebApplicationInitializer → 使用新 API
//     - WebMvcConfigurerAdapter → 使用 WebMvcConfigurer

// ☐ 5. 更新 application.yml 中已废弃的配置项
//     spring.profiles → spring.config.activate.on-profile (YAML 中)

// ☐ 6. Spring Security 配置更新
//     @Configuration
//     @EnableWebSecurity
//     public class SecurityConfig {
//         @Bean
//         public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//             http
//                 .authorizeHttpRequests(auth -> auth
//                     .anyRequest().authenticated()
//                 )
//                 .formLogin(Customizer.withDefaults());
//             return http.build();
//         }
//     }

// ☐ 7. 验证所有依赖的版本兼容性

// ☐ 8. 测试覆盖
```

### 14.3 Virtual Threads (虚拟线程) 支持

```yaml
# ========== Spring Boot 3.2+ Virtual Threads ==========
# JDK 21 引入了虚拟线程 (Virtual Threads, Project Loom)
# 允许以阻塞式编程模型达到高并发

# 在 application.yml 中启用:
spring:
  threads:
    virtual:
      enabled: true  # Tomcat, Jetty, Undertow 都使用虚拟线程
```

```java
// ========== 虚拟线程使用示例 ==========
@Service
public class VirtualThreadService {
    
    // 启用虚拟线程后，@Async 自动使用虚拟线程
    @Async
    public CompletableFuture<String> processAsync() {
        // 每个请求使用独立的虚拟线程
        // 虚拟线程非常轻量 (KB 级 vs 线程 MB 级)
        return CompletableFuture.completedFuture("done");
    }
}

// 手动创建虚拟线程:
@Configuration
public class VirtualThreadConfig {
    
    @Bean
    public Executor virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
```

---

## 15. GraalVM 与 Native Image

### 15.1 什么是 Native Image

```java
// ========== GraalVM Native Image ==========
// 将 Java 应用编译为原生可执行文件
// ● 启动速度: 毫秒级 (vs JVM 秒级)
// ● 内存占用: 大幅降低 (减少 50-80%)
// ● 不需要 JRE: 直接运行
// ● 包体积: 较大 (包含 AOT 编译代码)

// 适用场景:
// ✅ Serverless 函数 (AWS Lambda, 冷启动关键)
// ✅ 微服务 (快速扩容)
// ✅ CLI 工具
// ✅ 容器化部署 (镜像更小)
// ❌ 需要动态类加载的应用 (反射, 代理, 序列化)
// ❌ 需要大量 JIT 优化的计算密集型应用
```

### 15.2 配置 Native Image

```xml
<!-- ========== Maven 配置 ========== -->
<build>
    <plugins>
        <plugin>
            <groupId>org.graalvm.buildtools</groupId>
            <artifactId>native-maven-plugin</artifactId>
        </plugin>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <image>
                    <builder>paketobuildpacks/builder:tiny</builder>
                </image>
            </configuration>
        </plugin>
    </plugins>
</build>
```

```java
// ========== 编译和运行 ==========
// 传统 JAR:
// mvn clean package
// java -jar target/app.jar

// Native Image:
// mvn -Pnative native:compile
// ./target/app

// 或者使用 Docker:
// mvn -Pnative spring-boot:build-image
// docker run -p 8080:8080 app:latest

// ========== 运行时配置 ==========
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

// 提示 GraalVM 哪些需要反射:
@Configuration
@RegisterReflectionForBinding({
    User.class,
    Order.class,
    Result.class
})
public class ReflectionConfig { }

// 提示接口需要动态代理:
@Configuration
@RegisterProxy(SomeInterface.class)
public class ProxyConfig { }

// 提示需要保留构造器和方法的类:
@Configuration
@RegisterReflection(classes = {
    MyConverter.class
}, methods = true, fields = true)
public class AdvancedReflectionConfig { }
```

### 15.3 局限性

```yaml
# ========== Native Image 局限性 ==========
#
# 1. 反射: 需要在编译时注册
#    ● Spring 自动处理了大部分反射场景
#    ● 自定义反射需要 @RegisterReflectionForBinding
#
# 2. 动态代理: 需要在编译时注册
#    ● Spring 自动处理 AOP 代理
#
# 3. 序列化: 需要在编译时注册
#    ● Jackson 自动处理
#    ● 自定义序列化需要配置
#
# 4. 动态类加载: 不支持
#    ● Class.forName() 在编译时未知的类
#
# 5. JNI (Java Native Interface): 需要配置
#
# 6. 资源文件: 需要配置 include/exclude
```

---

## 16. 面试题精选

### 16.1 基础题

**Q1: @SpringBootApplication 由哪些注解组成？**

A: @SpringBootConfiguration (本质是 @Configuration) + @EnableAutoConfiguration + @ComponentScan。@EnableAutoConfiguration 通过 @Import(AutoConfigurationImportSelector.class) 加载自动配置。

**Q2: Spring Boot 的自动配置是如何工作的？**

A: 
1. @EnableAutoConfiguration → @Import(AutoConfigurationImportSelector.class)
2. AutoConfigurationImportSelector 读取 META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
3. 获取所有 AutoConfiguration 类列表 (~130+)
4. 应用 @Conditional 系列注解过滤（@ConditionalOnClass, @ConditionalOnMissingBean 等）
5. 匹配成功的配置类注入到容器

**Q3: Spring Boot 的配置优先级？**

A: 17 级，从高到低: @TestPropertySource → 命令行参数 → SPRING_APPLICATION_JSON → 系统属性 → 环境变量 → Profile 特定配置 → 应用主配置 → @PropertySource → 默认属性。

### 16.2 进阶题

**Q4: 如何自定义一个 Spring Boot Starter？**

A: 
1. 创建自动配置类 @AutoConfiguration
2. 创建配置属性类 @ConfigurationProperties
3. 创建核心服务类（不标注 @Component）
4. 在 META-INF/spring/ 下注册自动配置
5. 可选: 添加 HealthIndicator, 配置元数据
6. 提供 @ConditionalOnMissingBean 让用户可以替换

**Q5: Spring Boot 的测试切片有哪些？**

A: @WebMvcTest (Web 层), @DataJpaTest (JPA), @JsonTest (JSON), @RestClientTest (REST 客户端), @SpringBootTest (完整集成)。

**Q6: Actuator 的核心端点及用途？**

A: health (健康检查), info (应用信息), metrics (指标), env (环境属性), beans (Bean 列表), loggers (日志级别,支持动态修改), mappings (URL 映射), threaddump (线程快照), heapdump (堆转储)。

### 16.3 高级题

**Q7: Spring Boot 3.x 从 2.x 迁移的主要变化？**

A: 
1. Java 17 基线 (最低)
2. Jakarta EE 迁移 (javax.* → jakarta.*)
3. Hibernate 6.x
4. Spring Security Lambda DSL
5. RestTemplate 废弃推荐 RestClient/WebClient
6. AOT 编译支持
7. Virtual Threads 支持

**Q8: Micrometer 中的 Counter, Gauge, Timer, DistributionSummary 各自用途？**

A: Counter — 只增不减的计数器（请求数）；Gauge — 可增可减的仪表值（内存使用）；Timer — 耗时记录（请求延迟）；DistributionSummary — 数值分布（响应大小）。所有指标都可以添加 Tag 标签进行维度聚合。

---

> **Spring Boot 是现代 Java 微服务的基石。理解自动配置原理和 Starter 机制，能让你从"用户"变成"创造者"。不用害怕深入源码 — AutoConfigurationImportSelector 只有几百行代码，读懂了你就掌握了 Spring Boot 的核心。**
