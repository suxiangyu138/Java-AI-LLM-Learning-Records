# 01 - Spring Boot 自动配置原理

> 定位：@SpringBootApplication 三合一、自动配置加载流程、条件装配、Starter 机制、自定义 Starter——Spring Boot 面试第一考点

## 📚 目录

1. [@SpringBootApplication 三合一](#1-springbootapplication-三合一)
2. [自动配置加载流程](#2-自动配置加载流程)
3. [条件装配 Conditional](#3-条件装配-conditional)
4. [Starter 机制](#4-starter-机制)
5. [自定义 Starter 实战](#5-自定义-starter-实战)

---

## 1. @SpringBootApplication 三合一

```java
// ⚠️ @SpringBootApplication = 三个注解的组合
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

```java
// 等价展开：
@SpringBootConfiguration      // ① 配置类（@Configuration 派生）
@EnableAutoConfiguration      // ② ⚠️ 开启自动配置（核心）
@ComponentScan                // ③ 组件扫描（当前包及子包）
public class Application { }
```

| 注解 | 作用 |
|------|------|
| @SpringBootConfiguration | 标记为配置类 |
| **@EnableAutoConfiguration** | **自动配置入口（核心）** |
| @ComponentScan | 扫描当前包及子包的 @Component |

> 🎯 **要点**：三合一——配置类 + 自动配置 + 组件扫描。**扫描范围是当前包及子包**——启动类位置错误会导致 Bean 找不到（经典坑）。

---

## 2. 自动配置加载流程

### 2.1 完整链路

```
自动配置执行流程：
  ① @EnableAutoConfiguration → 导入 AutoConfigurationImportSelector
  ② 读取 META-INF/spring/org.springframework.boot.autoconfigure.
     AutoConfiguration.imports（配置类清单）
  ③ 逐个检查条件（@ConditionalOnClass/Property/Bean...）
  ④ 条件满足 → 注册配置类 → 装配 Bean

⚠️ 面试必答：
"自动配置 = 读取 imports 清单 →
 条件判断 → 满足才装配。
 核心：'有依赖才装配'（条件注解驱动）。"
```

### 2.2 imports 文件

```properties
# META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
# ... 47 个模块（Boot 4 拆分后按模块分散）
```

```java
// 自动配置类的特征：@AutoConfiguration = @Configuration + 条件
@AutoConfiguration
@ConditionalOnClass({ DataSource.class })          // ⚠️ classpath 有才装配
@EnableConfigurationProperties(DataSourceProperties.class)
public class DataSourceAutoConfiguration {
    // 装配 DataSource Bean（默认 HikariCP）
}
```

---

## 3. 条件装配 Conditional

### 3.1 常用条件注解

| 注解 | 条件 | 场景 |
|------|------|------|
| @ConditionalOnClass | classpath 有某类 | 依赖驱动装配 |
| @ConditionalOnMissingBean | 容器无某 Bean | 允许用户覆盖 |
| @ConditionalOnProperty | 配置属性存在/值 | 开关控制 |
| @ConditionalOnWebApplication | 是 Web 应用 | Web 相关装配 |
| @ConditionalOnExpression | SpEL 表达式 | 复杂条件 |

```java
// ⚠️ 面试必答示例：
// @ConditionalOnMissingBean：用户自定义则用户优先（覆盖机制）
@Bean
@ConditionalOnMissingBean
public ObjectMapper objectMapper() {
    return new ObjectMapper();       // 用户自己配了就不装配
}

// @ConditionalOnProperty：开关控制
@Bean
@ConditionalOnProperty(name = "app.cache.enabled", havingValue = "true")
public CacheManager cacheManager() { }
```

### 3.2 条件机制的价值

```
条件装配解决"覆盖与开关"：
  ① 用户 Bean 优先（@ConditionalOnMissingBean）
  ② 配置开关（@ConditionalOnProperty）
  ③ 依赖驱动（@ConditionalOnClass）

⚠️ 面试必答：
"条件装配 = 自动配置的'智能开关'——
 依赖在才装、用户配了不装、
 开关关了不装。这是 Boot 可定制性的根基。"
```

---

## 4. Starter 机制

### 4.1 Starter 是什么

```xml
<!-- ⚠️ Starter = 依赖集合 + 自动配置的打包 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<!-- 引入后：Tomcat + Spring MVC + Jackson 全部就绪 -->

<!-- 常用 Starter -->
spring-boot-starter-web        <!-- Web（MVC + Tomcat） -->
spring-boot-starter-data-jpa   <!-- JPA + Hibernate -->
spring-boot-starter-security   <!-- 安全 -->
spring-boot-starter-test       <!-- 测试全家桶 -->
spring-boot-starter-actuator   <!-- 生产监控 -->
spring-boot-starter-data-redis <!-- Redis -->
spring-boot-starter-amqp       <!-- RabbitMQ -->
```

### 4.2 Starter 的两部分

```
一个 Starter = 两部分：
  ① 依赖管理：pom 里声明一组依赖（版本 Boot BOM 统一）
  ② 自动配置：META-INF 里的 AutoConfiguration.imports
     （依赖引入 → 自动配置生效）

⚠️ 面试必答：
"Starter = 依赖 + 自动配置的组合——
 引入依赖后配置类自动生效；
 BOM（依赖管理）保证版本兼容。"
```

---

## 5. 自定义 Starter 实战

### 5.1 创建步骤

```java
// ① 定义配置属性（@ConfigurationProperties）
@ConfigurationProperties(prefix = "app.hello")
public class HelloProperties {
    private String message = "默认问候";
    private boolean enabled = true;
    // getter/setter
}
```

```java
// ② 定义自动配置类
@AutoConfiguration
@EnableConfigurationProperties(HelloProperties.class)
@ConditionalOnProperty(name = "app.hello.enabled", havingValue = "true")
public class HelloAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public HelloService helloService(HelloProperties properties) {
        return new HelloService(properties.getMessage());
    }
}
```

```
# ③ 注册自动配置（META-INF/spring/...AutoConfiguration.imports）
com.example.hello.HelloAutoConfiguration
```

```java
// ④ 使用方引入依赖后直接注入
@Service
public class DemoService {
    private final HelloService helloService;   // 自动注入
    // 配置：app.hello.message=你好
}
```

### 5.2 自定义 Starter 规范

```
✅ 命名：xxx-spring-boot-starter（官方约定）
✅ 配置属性：@ConfigurationProperties（类型安全）
✅ 条件装配：@ConditionalOnMissingBean（用户可覆盖）
✅ 开关控制：@ConditionalOnProperty
✅ 自动配置注册：AutoConfiguration.imports
✅ 提供 spring.factories 兼容（旧版）或 imports（新版）

⚠️ 面试必答：
"自定义 Starter 五步——属性类、配置类、
 条件装配、imports 注册、命名规范；
 这是'给团队写公共组件'的标准姿势。"
```

---

> 🎯 **核心要点**：自动配置体系 = **三合一启动类**（配置 + 自动配置 + 扫描）+ **加载流程**（imports 清单 → 条件判断 → 装配）+ **条件装配**（OnClass/MissingBean/Property）+ **Starter 机制**（依赖 + 自动配置打包）+ **自定义 Starter**（属性 + 条件 + 注册）。"为什么引入依赖就能用"是 Boot 面试第一问。

---

**返回总览**：[00-SpringBoot总览与核心概念](00-SpringBoot总览与核心概念.md) | **下一篇**：[02-SpringBoot配置体系](02-SpringBoot配置体系.md)
