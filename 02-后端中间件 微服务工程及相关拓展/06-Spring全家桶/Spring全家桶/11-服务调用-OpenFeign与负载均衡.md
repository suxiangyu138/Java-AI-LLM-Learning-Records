# 11-服务调用-OpenFeign与负载均衡

> 🎯 P1 就业必备 — OpenFeign 是 Spring Cloud 微服务间声明式 HTTP 调用的核心组件,基于接口 + 注解的方式屏蔽底层 HTTP 通信细节;配合 Spring Cloud LoadBalancer 实现客户端负载均衡,是微服务同步调用链中最关键的环节。

---

## 目录

1. [本章总览](#1-本章总览)
2. [分层理论讲解](#2-分层理论讲解)
   - 2.1 [OpenFeign 架构总览](#21-openfeign-架构总览)
   - 2.2 [OpenFeign 快速入门](#22-openfeign-快速入门)
   - 2.3 [@FeignClient 配置详解](#23-feignclient-配置详解)
   - 2.4 [请求参数绑定](#24-请求参数绑定)
   - 2.5 [Feign 日志体系](#25-feign-日志体系)
   - 2.6 [超时与重试配置](#26-超时与重试配置)
   - 2.7 [请求拦截器 RequestInterceptor](#27-请求拦截器-requestinterceptor)
   - 2.8 [编码器与解码器](#28-编码器与解码器)
   - 2.9 [负载均衡 LoadBalancer](#29-负载均衡-loadbalancer)
   - 2.10 [Feign vs RestTemplate vs Dubbo](#210-feign-vs-resttemplate-vs-dubbo)
3. [高频踩坑与误区](#3-高频踩坑与误区)
   - 3.1 [Feign 请求 Header 丢失问题](#31-feign-请求-header-丢失问题)
   - 3.2 [Feign 超时 + 重试导致重复请求](#32-feign-超时--重试导致重复请求)
   - 3.3 [Feign 调用超时排查 7 步法](#33-feign-调用超时排查-7-步法)
   - 3.4 [@SpringQueryMap 忘记使用](#34-springquerymap-忘记使用)
   - 3.5 [同服务多 @FeignClient contextId 冲突](#35-同服务多-feignclient-contextid-冲突)
   - 3.6 [Feign 与 LoadBalancer 集成失效](#36-feign-与-loadbalancer-集成失效)
4. [随堂基础练习](#4-随堂基础练习)
5. [章节综合实操案例](#5-章节综合实操案例)
6. [分层综合习题](#6-分层综合习题)
7. [本章复盘速记清单](#7-本章复盘速记清单)
8. [精通拓展补充-P2](#8-精通拓展补充-p2)

---

## 1. 本章总览

### 1.1 知识定位

| 项目 | 内容 |
|------|------|
| **归属** | Spring全家桶核心组件 -> 层级2 P1就业必备 -> Spring Cloud微服务生态 |
| **前置依赖** | Spring Boot Web、Spring Cloud 基础、Nacos 服务注册与发现 |
| **重要性** | ⭐⭐⭐⭐⭐（几乎每个微服务项目都通过 Feign 实现服务间调用） |
| **学习难度** | 中等偏低（API 声明式调用上手快,但超时/重试/Header 传递等陷阱较多） |

### 1.2 前置知识

| 知识领域 | 要求 | 说明 |
|---------|------|------|
| Spring Boot Web | 掌握 | 理解 HTTP 协议、RESTful API 设计、@RestController |
| Spring Cloud 基础 | 掌握 | 了解微服务架构中服务注册与发现的基本概念 |
| Nacos 服务发现 | 掌握 | 服务名 -> 真实地址的映射,服务实例列表查询 |
| RESTful API 设计 | 掌握 | GET/POST/PUT/DELETE 语义和状态码 |
| Spring MVC 注解 | 掌握 | @GetMapping、@PostMapping、@RequestParam 等 |

### 1.3 三层学习目标

| 级别 | 目标 | 对应能力 |
|------|------|---------|
| **L1-应用** | 掌握 @FeignClient 注解声明客户端,能完成 CRUD 调用;能配置超时、日志、拦截器;能集成 LoadBalancer 实现负载均衡 | 初级工程师 |
| **L2-原理** | 理解 Feign 动态代理执行流程;掌握 Fallback 降级机制;能排查 Header 丢失、超时重试等问题 | 中级工程师 |
| **L3-架构** | 深入理解 Feign 源码级执行链路;掌握自定义编码器/解码器/ErrorDecoder;能设计高可用的服务间调用方案 | 高级工程师 |

---## 2. 分层理论讲解

### 2.1 OpenFeign 架构总览

```
┌────────────────────────────────────────────────────────────────────┐
│                          Consumer Service                           │
│                                                                     │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                   FeignClient Interface                         │ │
│  │  @FeignClient(name = "user-service")                           │ │
│  │  interface UserFeignClient {                                   │ │
│  │      @GetMapping("/user/{id}") User getUser(@PathVariable id); │ │
│  │  }                                                              │ │
│  └───────────────────────┬────────────────────────────────────────┘ │
│                          │                                          │
│                          ▼                                          │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │             Feign Dynamic Proxy (JDK 动态代理)                   │ │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────────┐  │ │
│  │  │ Method   │ │ Method   │ │ Method   │ │ ...              │  │ │
│  │  │ Handler 1│ │ Handler 2│ │ Handler 3│ │                  │  │ │
│  │  └────┬─────┘ └────┬─────┘ └────┬─────┘ └──────────────────┘  │ │
│  └───────┼────────────┼────────────┼───────────────────────────┘  │
│          │            │            │                               │
│          ▼            ▼            ▼                               │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                   Feign Invocation Handler                      │ │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────────┐  │ │
│  │  │Feign     │ │Retry     │ │Hystrix  │ │ ...              │  │ │
│  │  │Filter    │ │Filter    │ │Filter    │ │                  │  │ │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────────────┘  │ │
│  └──────────────────────────┬─────────────────────────────────────┘ │
│                             │                                       │
│                             ▼                                       │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │              LoadBalancer (负载均衡)                            │ │
│  │  ┌────────────┐ ┌────────────┐ ┌────────────┐                  │ │
│  │  │RoundRobin  │ │  Random    │ │NacosWeight │                  │ │
│  │  │  轮询       │ │  随机       │ │  权重       │                  │ │
│  │  └────────────┘ └────────────┘ └────────────┘                  │ │
│  └──────────────────────────┬─────────────────────────────────────┘ │
│                             │                                       │
│                             ▼                                       │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                    HTTP 请求执行                                 │ │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐                      │ │
│  │  │Encoder   │ │  HTTP    │ │ Decoder  │                      │ │
│  │  │编码器     │ │  Client  │ │ 解码器   │                      │ │
│  │  └──────────┘ └──────────┘ └──────────┘                      │ │
│  └────────────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────┘
         │
         │ HTTP 请求
         ▼
┌────────────────────────────────────────────────────────────────────┐
│                         Provider Service                           │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │  @RestController @RequestMapping("/user")                      │ │
│  │  public class UserController { ... }                           │ │
│  └────────────────────────────────────────────────────────────────┘ │
│  Instance 1: 192.168.1.10:8081                                      │
│  Instance 2: 192.168.1.11:8081                                      │
│  Instance 3: 192.168.1.12:8081                                      │
└────────────────────────────────────────────────────────────────────┘
```

### 2.2 OpenFeign 快速入门

#### 2.2.1 添加依赖

```xml
<!-- 消费者服务：OpenFeign -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>

<!-- 注册中心（以 Nacos 为例） -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>

<!-- 负载均衡（Spring Cloud 官方实现） -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
```

#### 2.2.2 启用 OpenFeign

```java
package com.example.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.example.order.feign")  // 扫描 FeignClient 接口
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
```

#### 2.2.3 服务提供者（user-service）

```java
package com.example.user.controller;

import com.example.user.entity.User;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/user")
public class UserController {

    private final Map<Long, User> userDb = new HashMap<>();

    public UserController() {
        userDb.put(1L, new User(1L, "张三", "zhangsan@example.com", 25));
        userDb.put(2L, new User(2L, "李四", "lisi@example.com", 30));
        userDb.put(3L, new User(3L, "王五", "wangwu@example.com", 28));
    }

    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userDb.get(id);
    }

    @GetMapping("/list")
    public List<User> list() {
        return new ArrayList<>(userDb.values());
    }

    @PostMapping
    public User createUser(@RequestBody User user) {
        user.setId(System.currentTimeMillis());
        userDb.put(user.getId(), user);
        return user;
    }

    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        userDb.put(id, user);
        return user;
    }

    @DeleteMapping("/{id}")
    public String deleteUser(@PathVariable Long id) {
        userDb.remove(id);
        return "deleted";
    }
}
```

#### 2.2.4 FeignClient 声明式客户端（消费者）

```java
package com.example.order.feign;

import com.example.order.entity.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 声明式调用 user-service
 * name/value: 目标服务名（注册到 Nacos 的服务名）
 * path: 基础路径前缀（可选）
 * fallback: 熔断降级处理类（需集成 Sentinel 或 Hystrix）
 */
@FeignClient(name = "user-service", path = "/user")
public interface UserFeignClient {

    /**
     * GET 查询单个用户
     * 映射到 user-service 的 GET /user/{id}
     */
    @GetMapping("/{id}")
    User getUser(@PathVariable("id") Long id);

    /**
     * GET 查询所有用户
     */
    @GetMapping("/list")
    List<User> listUsers();

    /**
     * POST 创建用户
     */
    @PostMapping
    User createUser(@RequestBody User user);

    /**
     * PUT 更新用户
     */
    @PutMapping("/{id}")
    User updateUser(@PathVariable("id") Long id, @RequestBody User user);

    /**
     * DELETE 删除用户
     */
    @DeleteMapping("/{id}")
    String deleteUser(@PathVariable("id") Long id);
}
```

#### 2.2.5 Feign 调用控制器

```java
package com.example.order.controller;

import com.example.order.entity.User;
import com.example.order.feign.UserFeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/feign/user")
public class FeignUserController {

    private final UserFeignClient userFeignClient;

    public FeignUserController(UserFeignClient userFeignClient) {
        this.userFeignClient = userFeignClient;
    }

    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userFeignClient.getUser(id);
    }

    @GetMapping("/list")
    public List<User> list() {
        return userFeignClient.listUsers();
    }

    @PostMapping
    public User createUser(@RequestBody User user) {
        return userFeignClient.createUser(user);
    }

    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User user) {
        return userFeignClient.updateUser(id, user);
    }

    @DeleteMapping("/{id}")
    public String deleteUser(@PathVariable Long id) {
        return userFeignClient.deleteUser(id);
    }
}
```

> 💡 使用 OpenFeign 后，调用远程服务就像调用本地接口一样简单。动态代理在底层帮我们完成了：服务发现 → 负载均衡 → 序列化 → HTTP 请求 → 反序列化 的全过程。

### 2.3 @FeignClient 配置详解

#### 2.3.1 @FeignClient 注解属性

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` / `value` | String | 必填 | 目标服务名，注册中心的服务 ID |
| `url` | String | "" | 直接指定 URL（用于直连测试，跳过注册中心） |
| `path` | String | "" | 所有请求的前缀路径 |
| `fallback` | Class | void | 熔断降级时执行的 Fallback 类 |
| `fallbackFactory` | Class | void | Fallback 工厂类，可获取异常原因 |
| `configuration` | Class[] | {} | 自定义 Feign 配置类 |
| `contextId` | String | "" | Bean 名称别名（同名 Service 多 Client 时用） |
| `primary` | boolean | true | 是否为主 Bean |
| `qualifiers` | String[] | {} | 限定符 |

#### 2.3.2 application.yml 全局配置

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:                              # 默认配置（对所有 FeignClient 生效）
            connect-timeout: 5000               # 连接超时（ms）
            read-timeout: 10000                 # 读取超时（ms）
            logger-level: FULL                  # 日志级别
            retryer: com.example.config.FeignRetryConfig  # 重试策略
            request-interceptors:               # 请求拦截器
              - com.example.interceptor.FeignAuthInterceptor
            decode404: false                    # 是否将 404 解码为正常响应
            encode-decode: true                 # 是否启用编码解码
          user-service:                         # 特定服务配置（覆盖默认）
            connect-timeout: 3000
            read-timeout: 5000
            logger-level: HEADERS
```

#### 2.3.3 Java 配置类方式

```java
package com.example.order.config;

import feign.Logger;
import feign.Retryer;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.form.FormEncoder;
import org.springframework.cloud.openfeign.FeignClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Feign 全局配置
 */
@Configuration
public class FeignGlobalConfig {

    /**
     * 日志级别配置
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    /**
     * 重试配置
     * period: 初始重试间隔 100ms
     * maxPeriod: 最大重试间隔 1000ms
     * maxAttempts: 最大重试次数 5（含首次调用）
     */
    @Bean
    public Retryer feignRetryer() {
        return new Retryer.Default(100, TimeUnit.SECONDS.toMillis(1), 5);
    }
}

/**
 * 特定服务的 Feign 配置（不要加 @Configuration，否则全局生效）
 */
public class UserServiceFeignConfig {

    @Bean
    public Logger.Level loggerLevel() {
        return Logger.Level.BASIC;
    }

    @Bean
    public Retryer retryer() {
        return Retryer.NEVER_RETRY;  // 不重试
    }
}

/**
 * Feign 自定义编码器（支持表单提交）
 */
@Configuration
public class FeignEncoderConfig {

    @Bean
    public Encoder feignFormEncoder() {
        return new FormEncoder();
    }
}
```

```java
// 特定服务配置方式
@FeignClient(name = "user-service", configuration = UserServiceFeignConfig.class)
public interface UserFeignClient {
    // ...
}
```

### 2.4 请求参数绑定

#### 2.4.1 参数注解对照表

| 注解 | 使用位置 | HTTP 映射 | 说明 |
|------|----------|-----------|------|
| `@PathVariable` | 方法参数 | URL 路径变量 | 替换 URL 模板中的 `{id}` |
| `@RequestParam` | 方法参数 | URL 查询参数 | `?page=1&size=10` |
| `@SpringQueryMap` | POJO 参数 | URL 查询参数 | 将 POJO 字段展开为查询参数 |
| `@RequestBody` | 方法参数 | 请求体 JSON | POST/PUT 请求体 |
| `@RequestHeader` | 方法参数 | 请求头 | 传递单个 Header |

#### 2.4.2 @SpringQueryMap 详解

**问题**:GET 请求传递多个参数时,如果全部用 `@RequestParam` 会导致方法签名过长。

**解决**:用 `@SpringQueryMap` 将 POJO 展开为查询参数。

```java
// POJO 包装查询参数
@Data
public class UserQueryParam {
    private String name;
    private Integer age;
    private String email;
    private Integer page = 1;
    private Integer size = 10;
}

// Feign 客户端
@FeignClient(name = "user-service", path = "/api/users")
public interface UserClient {

    @GetMapping("/search")
    List<User> searchUsers(@SpringQueryMap UserQueryParam param);
}

// 调用方
List<User> users = userClient.searchUsers(new UserQueryParam() {{
    setName("张三");
    setAge(25);
}});
// 实际请求: GET /api/users/search?name=张三&age=25&page=1&size=10
```

> ⚠️ 必须使用 `@SpringQueryMap`,而不是 Spring MVC 的 `@RequestParam` 或 `@ModelAttribute`。Feign 不会自动将 POJO 展开为查询参数,不加 `@SpringQueryMap` 会导致参数为 null。

#### 2.4.3 @RequestHeader 传递请求头

```java
@FeignClient(name = "user-service", path = "/api/users")
public interface UserClient {

    @GetMapping("/{id}")
    User getUserById(@PathVariable("id") Long id,
                     @RequestHeader("Authorization") String token);
}

// 调用时传入
User user = userClient.getUserById(1L, "Bearer eyJhbGciOi...");
```

> 💡 如果每个方法都需要传 Token,建议用 RequestInterceptor 统一添加(见 2.8 节),而不是在每个方法参数中声明。

#### 2.4.4 @PathVariable 和 @RequestParam 最佳实践

```java
@FeignClient(name = "user-service", path = "/api/users")
public interface UserClient {

    // @PathVariable 必须显式指定 value（Spring Boot 2.x+ 要求）
    @GetMapping("/{id}")
    User getUserById(@PathVariable("id") Long id);

    // @RequestParam 多参数
    @GetMapping("/search")
    List<User> searchUsers(@RequestParam("keyword") String keyword,
                           @RequestParam("page") Integer page,
                           @RequestParam("size") Integer size);

    // @RequestBody 用于 POST/PUT
    @PostMapping
    User createUser(@RequestBody User user);
}
```

> ⚠️ `@PathVariable` 在 Spring Boot 2.x+ 中**必须显式指定 value 属性**,否则编译后参数名丢失,Feign 会报错 `PathVariable annotation was empty on param 0`。

---

### 2.5 Feign 日志体系

#### 2.5.1 四种日志级别

| 级别 | 说明 | 适用场景 |
|------|------|---------|
| `NONE` | 不记录任何日志（默认） | 生产环境 |
| `BASIC` | 只记录请求方法、URL、响应状态码、执行时间 | 常规监控 |
| `HEADERS` | 记录 BASIC 信息 + 请求和响应的 Header | 调试 Header 问题 |
| `FULL` | 记录 HEADERS 信息 + 请求体、响应体 | 开发调试、排错 |

#### 2.5.2 日志配置示例

```yaml
# application.yml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            logger-level: FULL

# 同时需要设置 Logger 级别
logging:
  level:
    com.example.order.feign: DEBUG  # FeignClient 接口所在包
```

```java
// 或者通过 Java 配置
@Configuration
public class FeignLogConfig {
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}
```

### 2.6 超时与重试配置

#### 2.6.1 超时参数

| 参数 | 说明 | 推荐值 |
|------|------|--------|
| `connect-timeout` | TCP 连接建立超时 | 3000~5000ms |
| `read-timeout` | 数据读取超时（等待响应的最长时间） | 5000~10000ms |
| `write-timeout` | 数据发送超时 | 5000ms |

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connect-timeout: 5000
            read-timeout: 10000
          # 特定服务调优
          payment-service:
            connect-timeout: 3000
            read-timeout: 30000   # 支付服务响应可能较慢
          report-service:
            connect-timeout: 5000
            read-timeout: 60000   # 报表服务可能需要更长时间
```

#### 2.6.2 重试配置

```java
/**
 * 自定义重试策略
 */
public class CustomRetryer implements Retryer {

    private final int maxAttempts;          // 最大尝试次数
    private final long backoffPeriod;       // 重试间隔（ms）
    private int attempt;                    // 当前尝试次数

    public CustomRetryer() {
        this(2000, 3);  // 默认：间隔 2s，最多 3 次
    }

    public CustomRetryor(long backoffPeriod, int maxAttempts) {
        this.backoffPeriod = backoffPeriod;
        this.maxAttempts = maxAttempts;
        this.attempt = 1;
    }

    @Override
    public void continueOrPropagate(RetryableException e) {
        if (attempt++ >= maxAttempts) {
            throw e;  // 超过最大重试次数，抛出异常
        }
        System.out.println("Feign 重试调用，第 " + attempt + " 次");
        try {
            Thread.sleep(backoffPeriod);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public Retryer clone() {
        return new CustomRetryer(backoffPeriod, maxAttempts);
    }
}
```

```java
// 配置重试器
@Configuration
public class FeignRetryConfig {
    @Bean
    public Retryer feignRetryer() {
        return new CustomRetryer(1000, 3);
    }
}
```

> ⚠️ 重试机制需要特别注意幂等性：GET 请求可以安全重试，但 POST/PUT/DELETE 请求如果服务端已处理成功但响应丢失，重试可能导致重复操作。建议在服务端实现幂等性方案（如唯一请求号）。

### 2.7 请求拦截器（RequestInterceptor）

#### 2.7.1 拦截器作用

RequestInterceptor 可以在 Feign 发出 HTTP 请求前拦截请求，统一添加 Header、参数等。常见使用场景：

| 场景 | 说明 |
|------|------|
| 传递 Token | 将当前请求的认证 Token 传递到下游服务 |
| 添加 TraceId | 全链路追踪的 TraceId 透传 |
| 统一 Header | 添加 Content-Type、Accept、User-Agent 等 |
| 请求签名 | 对请求参数进行签名 |
| 多语言 Header | 传递 Accept-Language 实现国际化 |

#### 2.7.2 Token 传递拦截器（解决 Header 丢失问题）

```java
package com.example.order.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign 请求拦截器 —— 解决 Header 丢失问题
 *
 * 核心问题：Feign 默认不会透传上游请求的 Header（如 Authorization Token）
 * 解决方式：通过 RequestInterceptor 从当前 RequestContext 获取 Header 并设置到 Feign 请求中
 */
@Component
public class FeignTokenInterceptor implements RequestInterceptor {

    private static final String TOKEN_HEADER = "Authorization";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TENANT_ID_HEADER = "X-Tenant-Id";

    @Override
    public void apply(RequestTemplate template) {
        // 从当前 HTTP 请求中获取 Header
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();

            // 1. 透传 Authorization Token
            String token = request.getHeader(TOKEN_HEADER);
            if (token != null && !template.headers().containsKey(TOKEN_HEADER)) {
                template.header(TOKEN_HEADER, token);
            }

            // 2. 透传 TraceId（全链路追踪）
            String traceId = request.getHeader(TRACE_ID_HEADER);
            if (traceId != null) {
                template.header(TRACE_ID_HEADER, traceId);
            } else {
                // 如果没有上游 TraceId，生成新的
                template.header(TRACE_ID_HEADER, java.util.UUID.randomUUID().toString());
            }

            // 3. 透传租户 ID（多租户场景）
            String tenantId = request.getHeader(TENANT_ID_HEADER);
            if (tenantId != null) {
                template.header(TENANT_ID_HEADER, tenantId);
            }
        }
    }
}
```

#### 2.7.3 多拦截器链

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            request-interceptors:
              - com.example.interceptor.FeignTokenInterceptor
              - com.example.interceptor.FeignLogInterceptor
              - com.example.interceptor.FeignTenantInterceptor
```

> 🎯 **Header 丢失问题**是微服务面试必问题。根本原因是 Feign 创建新 HTTP 请求时不会自动复制上游请求的上下文信息。通过 RequestInterceptor 从 `RequestContextHolder` 获取并设置 Header 是标准解决方案。在异步线程中需要注意 `RequestContextHolder` 的线程绑定特性，可能需要手动传递上下文。

### 2.8 编码器与解码器

#### 2.8.1 自定义解码器

```java
package com.example.order.config;

import feign.FeignException;
import feign.Response;
import feign.codec.DecodeException;
import feign.codec.Decoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;

/**
 * 自定义 Feign 解码器
 */
@Configuration
public class FeignDecoderConfig {

    @Bean
    public Decoder feignDecoder() {
        return new Decoder() {
            @Override
            public Object decode(Response response, Type type)
                    throws IOException, DecodeException, FeignException {

                // 读取响应体
                if (response.body() == null) {
                    return null;
                }

                Reader reader = response.body().asReader();
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(reader)) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                }

                String body = sb.toString();
                System.out.println("Feign 响应原始内容: " + body);

                // 实际项目中这里应该使用 Jackson/Gson 进行反序列化
                // 这里简化为返回原始字符串
                return body;
            }
        };
    }
}
```

#### 2.8.2 自定义编码器

```java
package com.example.order.config;

import feign.RequestTemplate;
import feign.codec.EncodeException;
import feign.codec.Encoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * 自定义 Feign 编码器（处理特殊格式请求）
 */
@Configuration
public class FeignEncoderConfig {

    @Bean
    public Encoder feignEncoder() {
        return new Encoder() {
            @Override
            public void encode(Object object, Type bodyType, RequestTemplate template)
                    throws EncodeException {
                // 如果是 Map 类型，转为查询参数
                if (object instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) object;
                    map.forEach((key, value) -> {
                        if (value != null) {
                            template.query(key, value.toString());
                        }
                    });
                }
                // 其他情况使用 Spring 的默认编码器
            }
        };
    }
}
```

### 2.9 负载均衡（LoadBalancer）

#### 2.9.1 Spring Cloud LoadBalancer 架构

Spring Cloud 从 2020.x 版本开始移除了 Netflix Ribbon，官方推荐使用 `spring-cloud-starter-loadbalancer` 作为负载均衡实现。

```
FeignClient 调用
      │
      ▼
Service Name → 需要解析为 IP:Port
      │
      ▼
LoadBalancerClient.choose("user-service")
      │
      ▼
ServiceInstanceListSupplier
      │
      ├── NacosServiceInstanceListSupplier
      │   （从 Nacos 获取实例列表）
      │
      ▼
负载均衡算法（ReactiveLoadBalancer）
      │
      ├── RoundRobinLoadBalancer（轮询）
      ├── RandomLoadBalancer（随机）
      └── NacosLoadBalancer（Nacos 权重感知）
      │
      ▼
选中的 ServiceInstance（IP + Port）
      │
      ▼
Feign 发起 HTTP 请求
```

#### 2.9.2 负载均衡算法详解

**RoundRobin（轮询）**

```java
// RoundRobinLoadBalancer 核心源码
public class RoundRobinLoadBalancer implements ReactorLoadBalancer<ServiceInstance> {

    private final AtomicInteger position;  // 原子计数器

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        // 1. 获取服务实例列表
        ServiceInstanceListSupplier supplier = getSupplier();
        return supplier.get(request).next().map(instances -> {
            // 2. 轮询选择：position 自增并取模
            int pos = Math.abs(this.position.incrementAndGet());
            ServiceInstance instance = instances.get(pos % instances.size());
            return Response.just(instance);
        });
    }
}
```

**Random（随机）**

```java
// RandomLoadBalancer 核心源码
public class RandomLoadBalancer implements ReactorLoadBalancer<ServiceInstance> {

    private final Random random = new Random();

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        ServiceInstanceListSupplier supplier = getSupplier();
        return supplier.get(request).next().map(instances -> {
            // 随机选择一个实例
            int index = random.nextInt(instances.size());
            return Response.just(instances.get(index));
        });
    }
}
```

**NacosWeighted（Nacos 权重感知）**

```yaml
# 启用 Nacos 权重负载均衡
spring:
  cloud:
    loadbalancer:
      nacos:
        enabled: true  # 开启 Nacos 权重负载均衡策略
```

```java
// NacosLoadBalancer 核心逻辑
public class NacosLoadBalancer implements ReactorLoadBalancer<ServiceInstance> {

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        ServiceInstanceListSupplier supplier = getSupplier();
        return supplier.get(request).next().map(instances -> {
            // 1. 计算总权重
            double totalWeight = instances.stream()
                    .mapToDouble(this::getWeight)
                    .sum();

            // 2. 随机数落在某个权重区间
            double random = ThreadLocalRandom.current().nextDouble(0, totalWeight);
            double currentWeight = 0;

            // 3. 权重随机选择
            for (ServiceInstance instance : instances) {
                currentWeight += getWeight(instance);
                if (random <= currentWeight) {
                    return Response.just(instance);
                }
            }

            return Response.just(instances.get(0));
        });
    }

    private double getWeight(ServiceInstance instance) {
        // 从 Nacos 元数据中获取权重
        String weightStr = instance.getMetadata().get("weight");
        if (weightStr != null) {
            return Double.parseDouble(weightStr);
        }
        return 1.0;  // 默认权重 1.0
    }
}
```

#### 2.9.3 切换负载均衡算法

```yaml
# 全局切换为随机算法
spring:
  cloud:
    loadbalancer:
      configurations: random  # random | round_robin | nacos_weighted
```

```java
// 或者通过 Java 配置自定义
@Configuration
public class LoadBalancerConfig {

    @Bean
    public ReactorLoadBalancer<ServiceInstance> randomLoadBalancer(
            Environment environment,
            LoadBalancerClientFactory loadBalancerClientFactory) {
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new RandomLoadBalancer(
                loadBalancerClientFactory.getLazyProvider(name,
                        ServiceInstanceListSupplier.class), name);
    }
}
```

### 2.10 Feign vs RestTemplate vs Dubbo

| 对比维度 | OpenFeign | RestTemplate | Dubbo |
|---------|-----------|--------------|-------|
| **通信协议** | HTTP/1.1 (REST) | HTTP/1.1 (REST) | TCP 二进制协议 (RPC) |
| **序列化方式** | JSON/XML | JSON/XML | Hessian2 / protobuf / kryo |
| **调用方式** | 声明式接口 + 注解 | 模板方法 + URL | 接口 + 注解 |
| **代码侵入性** | 低（接口 + 注解） | 低（URL 字符串） | 中（需引入 API 包） |
| **性能** | 中（HTTP 协议开销） | 中（HTTP 协议开销） | 高（TCP 长连接 + 高效序列化） |
| **连接管理** | 连接池（Apache HttpClient） | 连接池 | 长连接复用 |
| **负载均衡** | LoadBalancer | @LoadBalanced + LoadBalancer | 内置多种策略 |
| **熔断降级** | 集成 Sentinel/Hystrix | 需手动实现 | 内置 + Sentinel |
| **服务治理** | 依赖外部组件 | 依赖外部组件 | 内置服务治理 |
| **跨语言** | 好（HTTP 协议） | 好（HTTP 协议） | 差（需自定义协议转换） |
| **开发效率** | 高（声明式） | 中（手动拼接 URL） | 中（需生成桩代码） |
| **网关集成** | 原生支持 Spring Cloud Gateway | 需适配 | 需额外适配 |
| **调试方便性** | 好（HTTP 可抓包） | 好 | 差（二进制协议难抓包） |
| **生态集成** | Spring Cloud 原生 | Spring 原生 | Apache Dubbo 生态 |
| **适用场景** | Spring Cloud 微服务、对外 REST API | 简单调用、非微服务场景 | 高性能内部调用、大型微服务 |

> 🎯 **选型建议**：Spring Cloud 项目首选 OpenFeign（声明式调用 + 云原生生态）；需要极致性能（如 10w+ QPS）的内部调用选 Dubbo；临时或简单的 HTTP 调用用 RestTemplate。

---

## 3. 高频踩坑与误区

### 3.1 Feign 请求 Header 丢失问题

#### 3.1.1 现象

通过 Feign 调用下游服务时,上游请求携带的 `Authorization` Token 未传递到下游,导致下游认证失败。

```java
// OrderController 中有请求头: Authorization: Bearer xxx
// 但 OrderService 通过 Feign 调用 UserService 时,Token 丢失了
// UserService 收到请求后返回 401 Unauthorized
```

#### 3.1.2 原因

Feign 每次发送请求时,默认**不会自动透传**当前请求的 Header。Feign 认为自己是一个独立的 HTTP 客户端,与当前 Web 请求上下文无关。`RequestContextHolder` 中的数据只有当前线程可以访问。

> ⚠️ Feign 不是 Servlet 容器的一部分,它不会自动访问 `HttpServletRequest` 中的 Header。

#### 3.1.3 解决方案:RequestInterceptor

```java
@Component
public class FeignHeaderInterceptor implements RequestInterceptor {

    private static final List<String> HEADER_KEYS = Arrays.asList(
        "Authorization", "X-Trace-Id", "X-Request-Id"
    );

    @Autowired
    private HttpServletRequest request;

    @Override
    public void apply(RequestTemplate template) {
        // 从当前请求获取所有需要透传的 Header
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                if (HEADER_KEYS.contains(name)) {
                    String value = request.getHeader(name);
                    template.header(name, value);
                }
            }
        }
    }
}
```

**异步线程场景**:

```java
// ❌ 错误:在异步线程中 RequestContextHolder 为空
@Async
public CompletableFuture<String> asyncCallService() {
    ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    // attributes 为 null!异步线程中丢失了请求上下文
    return CompletableFuture.completedFuture(result);
}

// ✅ 解决方案:手动传递 RequestAttributes 到异步线程
public CompletableFuture<String> asyncCallService() {
    ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

    return CompletableFuture.supplyAsync(() -> {
        RequestContextHolder.setRequestAttributes(attributes);
        try {
            return feignClient.call();
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    });
}
```

### 3.2 Feign 超时 + 重试导致重复请求

#### 3.2.1 现象

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connectTimeout: 2000
            readTimeout: 3000
```

加上 Retryer:

```java
@Bean
public Retryer feignRetryer() {
    return new Retryer.Default(100, 1000, 3);
}
```

如果一个 POST 下单接口响应超过 3s,Feign 会触发 readTimeout 然后重试:

```
请求1: POST /orders (发送成功)
→ 服务端正在处理(创建订单耗时 8s)
→ Feign 等待 3s 后 readTimeout
→ 服务端实际上正在处理中,但 Feign 以为失败了
→ Feign 触发重试
→ 请求2: POST /orders (又发送一次)
→ 服务端重复处理 → 重复下单!
```

#### 3.2.2 原因

Feign 的重试由 `Retryer` 控制,当发生 `RetryableException`(如 `SocketTimeoutException`)时自动重试。但重试时 Feign **不区分当前请求的 HTTP 方法是否幂等**,因此非幂等(POST/PUT/PATCH)请求的重试可能导致严重的业务问题(重复扣款、重复下单等)。

#### 3.2.3 解决方案

**方案一:全局关闭重试,只在 GET 请求上手动重试(推荐)**

```java
@Bean
public Retryer feignRetryer() {
    return Retryer.NEVER_RETRY;  // 关闭自动重试
}
```

**方案二:自定义 Retryer 仅对 GET 重试**

```java
public class SafeRetryer implements Retryer {
    private final Retryer.Default delegate = new Retryer.Default(100, 1000, 3);

    @Override
    public void continueOrPropagate(RetryableException e) {
        if (e.request() != null && "GET".equals(e.request().httpMethod().name())) {
            delegate.continueOrPropagate(e);
        } else {
            throw e;  // 非 GET 直接抛出,不重试
        }
    }

    @Override
    public Retryer clone() {
        return new SafeRetryer();
    }
}
```

| 操作类型 | 建议 |
|----------|------|
| GET 查询 | 开启重试(幂等安全) |
| DELETE 删除 | 可以重试(一般幂等) |
| PUT 全量更新 | 谨慎重试(需业务保证幂等) |
| POST 新增 | **禁止重试**(非幂等) |

### 3.3 Feign 调用超时排查 7 步法

#### 3.3.1 超时分类

| 类型 | 位置 | 表现 |
|------|------|------|
| 连接超时 | TCP 建立连接阶段 | `Connection refused` 或 `Connect timed out` |
| 读取超时 | 连接已建立,等待响应数据 | `Read timed out` 或 `SocketTimeoutException` |

#### 3.3.2 超时排查 7 步法

```
Step 1: 确认是 connectTimeout 还是 readTimeout
        → 看异常栈: ConnectException / SocketTimeoutException

Step 2: 检查 Feign 超时配置是否生效
        → 确认 yml 路径: spring.cloud.openfeign.client.config.{serviceName}
        → 开启 FULL 日志看实际耗时

Step 3: 检查服务提供者接口的响应时间
        → 直接 curl 调用: curl -w '%{time_total}' http://ip:port/api
        → 确认是服务端慢还是网络慢

Step 4: 检查是否有多次超时配置叠加
        → Feign 超时 → LoadBalancer 超时 → 容器(Tomcat)超时
        → 最外层(Feign readTimeout)必须大于服务端实际处理时间

Step 5: 检查是否有重试放大了超时时间
        → 重试 3 次 × 5s = 总等待 15s
        → 需要确认总耗时是否可接受

Step 6: 检查服务端是否会阻塞(死锁/慢 SQL/Full GC)
        → 查看数据库连接池、GC 日志、线程堆栈

Step 7: 检查网络层面
        → 跨 VPC / 防火墙 / 负载均衡器超时
        → ping / traceroute / telnet 确认网络连通性
```

#### 3.3.3 超时排查配置检查清单

```yaml
# 检查以下所有配置项
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connectTimeout: 5000    # ① Feign 连接超时
            readTimeout: 10000      # ② Feign 读取超时

# ③ 如果开启了断路保护
spring:
  cloud:
    openfeign:
      circuitbreaker:
        enabled: true

# ④ Sentinel 或 Hystrix 的超时(如果有)
# ⑤ 服务端 Tomcat 的 connection-timeout
server:
  tomcat:
    connection-timeout: 5000

# ⑥ 客户端 HttpClient 连接池超时(如果有)
```

### 3.4 @SpringQueryMap 忘记使用

#### 3.4.1 现象

```java
// ❌ 错误:多参数 GET 请求忘记加 @RequestParam
@FeignClient(name = "user-service", path = "/api/users")
public interface UserClient {
    @GetMapping("/search")
    List<User> searchUsers(String name, Integer age, String email);
    // 参数全为 null!Feign 不会自动绑定未注解的参数
}

// ❌ 错误:直接传 POJO 不加 @SpringQueryMap
@FeignClient(name = "user-service", path = "/api/users")
public interface UserClient {
    @GetMapping("/search")
    List<User> searchUsers(UserQueryParam param);
    // param 传过去了但字段全为 null
}
```

#### 3.4.2 原因

Feign 的 SpringMvcContract 只识别有注解的参数。没有注解的参数不会被绑定到请求模板中,导致服务端收到 null 或者报错。

#### 3.4.3 修复

```java
// ✅ 正确方式一:每个参数标注 @RequestParam
@GetMapping("/search")
List<User> searchUsers(@RequestParam("name") String name,
                       @RequestParam("age") Integer age,
                       @RequestParam("email") String email);

// ✅ 正确方式二:POJO + @SpringQueryMap
@GetMapping("/search")
List<User> searchUsers(@SpringQueryMap UserQueryParam param);
```

### 3.5 同服务多 @FeignClient contextId 冲突

#### 3.5.1 现象

```java
@FeignClient(name = "user-service", path = "/api/users")
public interface UserQueryClient { ... }

@FeignClient(name = "user-service", path = "/api/admin")
public interface UserAdminClient { ... }
```

启动报错:

```
The bean 'user-service.FeignClientSpecification' could not be registered.
A bean with that name has already been defined.
```

#### 3.5.2 原因

Feign 会为每个 `@FeignClient` 创建一个 Bean,Bean 的默认名称是 `{name}.FeignClientSpecification`。如果两个客户端使用相同的 `name`(服务名),Bean 名称冲突,导致启动失败。

#### 3.5.3 修复

```java
@FeignClient(name = "user-service", contextId = "userQueryClient",
             path = "/api/users")
public interface UserQueryClient { ... }

@FeignClient(name = "user-service", contextId = "userAdminClient",
             path = "/api/admin")
public interface UserAdminClient { ... }
```

> 💡 `contextId` 的值就是 Bean 名称,务必保证唯一。一个常见最佳实践是 `contextId` 用接口的简单类名。

### 3.6 Feign 与 LoadBalancer 集成失效

#### 3.6.1 现象

使用 `name` 方式调用时报错:

```
java.net.UnknownHostException: user-service
```

或:

```
No instances available for user-service
```

#### 3.6.2 原因

1. 缺少 LoadBalancer 依赖
2. 缺少 Nacos 服务发现依赖
3. 服务提供者未注册到 Nacos
4. 服务名拼写错误
5. Feign 使用了 `url` 属性(此时不走 LoadBalancer)

#### 3.6.3 修复

```xml
<!-- 确保依赖完整 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

```yaml
# 确保配置正确
spring:
  application:
    name: order-service
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
```

```java
// 确保客户端只使用 name,不需要写 url
@FeignClient(name = "user-service")  // ✅ 正确,走 LoadBalancer
// @FeignClient(name = "user-service", url = "http://localhost:8081")  // ❌ 不走 LoadBalancer
public interface UserClient { ... }
```

#### 3.6.4 验证 LoadBalancer 是否生效

```bash
# 启动 product-service 的两个实例(不同端口)
# 在 order-service 中调用查看端口是否能轮询
# 多次调用,观察日志确认交替命中不同实例
```

---

## 4. 随堂基础练习

### 练习 1：搭建 Feign 调用

创建两个服务 `provider-service` 和 `consumer-service`：

1. `provider-service` 提供 `GET /hello?name={name}` 接口，返回 `Hello, {name}!`
2. `consumer-service` 通过 FeignClient 调用 provider-service
3. 访问 consumer-service 的 `/greet?name=World` 验证调用结果

### 练习 2：Feign CRUD 完整实现

在练习 1 基础上实现完整的 CRUD 调用：

1. `provider-service` 提供 `Book` 资源 REST 接口（GET/POST/PUT/DELETE）
2. `consumer-service` 中声明对应的 FeignClient 接口
3. 编写 Controller 调用 FeignClient 的 CRUD 方法
4. 验证 CREATE → READ → UPDATE → DELETE 全流程

### 练习 3：Feign 日志与超时配置

1. 配置 Feign 日志级别为 `FULL`
2. 设置 `connect-timeout=3000`，`read-timeout=5000`
3. 在 provider-service 中模拟接口延时 4s，验证超时配置生效
4. 观察控制台输出的完整请求/响应日志

### 练习 4：负载均衡观察

1. 启动 3 个 provider-service 实例（端口 8081/8082/8083）
2. 从 consumer-service 连续调用 10 次
3. 观察请求是否均匀分配到三个实例
4. 分别测试 `round_robin` 和 `random` 两种算法
5. 在 Nacos 控制台修改某个实例的权重为 5（其他保持 1），测试 Nacos 权重负载均衡

---

## 5. 章节综合实操案例

### 5.1 案例背景

实现一个 **电商订单系统**，通过 OpenFeign 调用用户服务、商品服务、库存服务，完成下单全流程。同时实现负载均衡、熔断降级、Header 透传等企业级能力。

### 5.2 项目结构

```
feign-demo/
├── pom.xml
├── user-service/              # 用户服务 (8081)
├── product-service/           # 商品服务 (8082)
├── inventory-service/         # 库存服务 (8083)
└── order-service/             # 订单服务 (8084) —— Feign 消费者
    ├── pom.xml
    └── src/main/java/com/example/order/
        ├── OrderApplication.java
        ├── config/
        │   ├── FeignConfig.java
        │   ├── FeignLogConfig.java
        │   └── LoadBalancerConfig.java
        ├── feign/
        │   ├── UserFeignClient.java
        │   ├── ProductFeignClient.java
        │   └── InventoryFeignClient.java
        ├── interceptor/
        │   └── FeignContextInterceptor.java
        ├── controller/
        │   └── OrderController.java
        ├── entity/
        │   ├── User.java
        │   ├── Product.java
        │   └── Order.java
        └── service/
            └── OrderService.java
```

### 5.3 父模块 pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.1.0</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>feign-demo</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <modules>
        <module>user-service</module>
        <module>product-service</module>
        <module>inventory-service</module>
        <module>order-service</module>
    </modules>

    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2022.0.3</spring-cloud.version>
        <spring-cloud-alibaba.version>2022.0.0.0</spring-cloud-alibaba.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>com.alibaba.cloud</groupId>
                <artifactId>spring-cloud-alibaba-dependencies</artifactId>
                <version>${spring-cloud-alibaba.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

### 5.4 order-service 完整代码

**order-service/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.example</groupId>
        <artifactId>feign-demo</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>order-service</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-loadbalancer</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
    </dependencies>
</project>
```

**order-service application.yml**

```yaml
server:
  port: 8084

spring:
  application:
    name: order-service
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
    openfeign:
      client:
        config:
          default:
            connect-timeout: 5000
            read-timeout: 10000
            logger-level: FULL
          user-service:
            connect-timeout: 3000
            read-timeout: 5000
          inventory-service:
            connect-timeout: 3000
            read-timeout: 3000
      compression:
        request:
          enabled: true
          mime-types: application/json,application/xml
          min-request-size: 2048
        response:
          enabled: true

logging:
  level:
    com.example.order.feign: DEBUG
```

**OrderApplication.java**

```java
package com.example.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.example.order.feign")
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
```

**Entity 类**

```java
package com.example.order.entity;

public class User {
    private Long id;
    private String name;
    private String email;
    // constructors, getters, setters ...
    public User() {}
    public User(Long id, String name, String email) {
        this.id = id; this.name = name; this.email = email;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
```

```java
package com.example.order.entity;

public class Product {
    private Long id;
    private String name;
    private Double price;
    private Integer stock;
    // constructors, getters, setters ...
    public Product() {}
    public Product(Long id, String name, Double price, Integer stock) {
        this.id = id; this.name = name; this.price = price; this.stock = stock;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
}
```

```java
package com.example.order.entity;

public class Order {
    private Long orderId;
    private Long userId;
    private Long productId;
    private String userName;
    private String productName;
    private Double totalPrice;
    private Integer quantity;
    private String status;
    private Long timestamp;

    // constructors
    public Order() {}

    // getters and setters ...
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Double totalPrice) { this.totalPrice = totalPrice; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
}
```

**FeignClient 接口**

```java
package com.example.order.feign;

import com.example.order.entity.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "user-service", path = "/user",
        contextId = "userFeignClient")
public interface UserFeignClient {

    @GetMapping("/{id}")
    User getUser(@PathVariable("id") Long id);
}
```

```java
package com.example.order.feign;

import com.example.order.entity.Product;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "product-service", path = "/product",
        contextId = "productFeignClient")
public interface ProductFeignClient {

    @GetMapping("/{id}")
    Product getProduct(@PathVariable("id") Long id);

    @PostMapping("/deduct")
    Boolean deductStock(@RequestParam("productId") Long productId,
                        @RequestParam("quantity") Integer quantity);
}
```

```java
package com.example.order.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "inventory-service", path = "/inventory",
        contextId = "inventoryFeignClient")
public interface InventoryFeignClient {

    @GetMapping("/check/{productId}")
    Boolean checkStock(@PathVariable("productId") Long productId,
                       @RequestParam("quantity") Integer quantity);

    @PostMapping("/lock")
    Boolean lockStock(@RequestParam("productId") Long productId,
                      @RequestParam("quantity") Integer quantity,
                      @RequestParam("orderId") Long orderId);
}
```

**Feign 配置类**

```java
package com.example.order.config;

import feign.Logger;
import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class FeignConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    /**
     * 重试策略：生产环境谨慎使用写操作的重试
     */
    @Bean
    public Retryer feignRetryer() {
        return new Retryer.Default(100, TimeUnit.SECONDS.toMillis(1), 3);
    }
}
```

**请求拦截器（Header 透传）**

```java
package com.example.order.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class FeignContextInterceptor implements RequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(FeignContextInterceptor.class);

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();

            // 透传 Authorization
            String auth = request.getHeader("Authorization");
            if (auth != null) {
                template.header("Authorization", auth);
            }

            // 透传 TraceId
            String traceId = request.getHeader("X-Trace-Id");
            if (traceId == null) {
                traceId = java.util.UUID.randomUUID().toString().replace("-", "");
            }
            template.header("X-Trace-Id", traceId);

            // 记录调用链路
            log.info("Feign调用 [{}] {} - TraceId: {}",
                    template.method(), template.url(), traceId);
        }
    }
}
```

**OrderService 业务逻辑**

```java
package com.example.order.service;

import com.example.order.entity.Order;
import com.example.order.entity.Product;
import com.example.order.entity.User;
import com.example.order.feign.InventoryFeignClient;
import com.example.order.feign.ProductFeignClient;
import com.example.order.feign.UserFeignClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final UserFeignClient userFeignClient;
    private final ProductFeignClient productFeignClient;
    private final InventoryFeignClient inventoryFeignClient;

    public OrderService(UserFeignClient userFeignClient,
                        ProductFeignClient productFeignClient,
                        InventoryFeignClient inventoryFeignClient) {
        this.userFeignClient = userFeignClient;
        this.productFeignClient = productFeignClient;
        this.inventoryFeignClient = inventoryFeignClient;
    }

    /**
     * 创建订单完整流程
     */
    public Order createOrder(Long userId, Long productId, Integer quantity) {
        log.info("开始创建订单 - userId: {}, productId: {}, quantity: {}",
                userId, productId, quantity);

        long start = System.currentTimeMillis();

        // 1. 调用 user-service 获取用户信息
        User user = userFeignClient.getUser(userId);
        log.info("获取用户信息成功 - user: {}", user.getName());

        // 2. 调用 product-service 获取商品信息
        Product product = productFeignClient.getProduct(productId);
        log.info("获取商品信息成功 - product: {}", product.getName());

        // 3. 调用 inventory-service 检查库存
        boolean hasStock = inventoryFeignClient.checkStock(productId, quantity);
        if (!hasStock) {
            throw new RuntimeException("库存不足");
        }
        log.info("库存检查通过");

        // 4. 锁定库存
        Long orderId = System.currentTimeMillis();
        boolean locked = inventoryFeignClient.lockStock(productId, quantity, orderId);
        if (!locked) {
            throw new RuntimeException("库存锁定失败");
        }
        log.info("库存锁定成功");

        // 5. 扣减商品库存
        boolean deducted = productFeignClient.deductStock(productId, quantity);
        if (!deducted) {
            throw new RuntimeException("商品库存扣减失败");
        }
        log.info("商品库存扣减成功");

        // 6. 构造订单
        Order order = new Order();
        order.setOrderId(orderId);
        order.setUserId(userId);
        order.setProductId(productId);
        order.setUserName(user.getName());
        order.setProductName(product.getName());
        order.setTotalPrice(product.getPrice() * quantity);
        order.setQuantity(quantity);
        order.setStatus("CREATED");
        order.setTimestamp(System.currentTimeMillis());

        long cost = System.currentTimeMillis() - start;
        log.info("订单创建完成 - orderId: {}, 耗时: {}ms", orderId, cost);

        return order;
    }
}
```

**OrderController**

```java
package com.example.order.controller;

import com.example.order.entity.Order;
import com.example.order.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 创建订单
     */
    @PostMapping("/create")
    public Order createOrder(@RequestParam Long userId,
                              @RequestParam Long productId,
                              @RequestParam(defaultValue = "1") Integer quantity) {
        return orderService.createOrder(userId, productId, quantity);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "service", "order-service",
                "timestamp", String.valueOf(System.currentTimeMillis())
        );
    }
}
```

### 5.5 验证步骤

```bash
# 1. 启动 Nacos
sh startup.cmd -m standalone

# 2. 启动各服务（分别在不同终端）
java -jar user-service/target/user-service-1.0.0.jar --server.port=8081
java -jar product-service/target/product-service-1.0.0.jar --server.port=8082
java -jar inventory-service/target/inventory-service-1.0.0.jar --server.port=8083
java -jar order-service/target/order-service-1.0.0.jar --server.port=8084

# 3. 测试下单
curl -X POST "http://localhost:8084/order/create?userId=1&productId=1&quantity=2"

# 4. 验证负载均衡（启动多个 product-service 实例）
java -jar product-service-1.0.0.jar --server.port=8085
java -jar product-service-1.0.0.jar --server.port=8086

# 多次调用下单接口，观察 product-service 日志，确认请求分布到不同实例

# 5. 验证 Header 透传
curl -X POST "http://localhost:8084/order/create?userId=1&productId=1" \
  -H "Authorization: Bearer test-token-123"
# 观察 user-service 日志，确认接收到 Authorization Header

# 6. 测试超时
# 在 product-service 中添加 Thread.sleep(6000) 模拟超时
# 调用下单接口，预期在 read-timeout=5000 时触发超时异常
```

---

## 6. 分层综合习题

### 6.1 基础题

1. **简要说明 OpenFeign 的核心作用和原理。** Feign 的动态代理是如何工作的？

2. **写出 @FeignClient 注解中最常用的 3 个属性及其作用。** 如何同时调用同一个服务的两个不同接口组？

3. **Feign 的日志级别有哪几种？** 分别记录哪些信息？如何配置 Feign 日志？

4. **如何为 Feign 设置连接超时和读取超时？** 全局配置和单个服务配置的优先级关系是什么？

### 6.2 进阶题

1. **Feign 请求 Header 丢失的根本原因是什么？** 写出完整的解决方案（包括异步线程场景）。

2. **对比 OpenFeign、RestTemplate 和 Dubbo 的优缺点。** 在什么场景下应该选择 Dubbo 而不是 Feign？

3. **Spring Cloud LoadBalancer 支持哪些负载均衡算法？** 如何切换到 Nacos 权重感知的负载均衡策略？权重是如何计算的？

4. **Feign 的重试机制需要注意哪些问题？** 什么情况下重试可能导致数据不一致？如何避免？

### 6.3 精通题

1. **设计一个微服务链路追踪方案，要求在 Feign 调用中自动透传 TraceId，要求：**
   - 所有 Feign 调用自动携带 TraceId
   - 异步线程调用 Feign 时 TraceId 不丢失
   - TraceId 在日志中自动打印
   - 写出去完整的代码（拦截器 + 配置 + 日志 MDC）

2. **源码分析题：** 阅读 `FeignClientFactoryBean` 的源码，描述 Feign 动态代理的创建过程。`@EnableFeignClients` 是如何扫描并注册 FeignClient 的？

3. **在生产环境中，Feign 的超时时间和熔断降级应该如何配合？** 如果你需要在服务级别配置如下策略，请写出完整的配置和代码：
   - A 服务：连接超时 2s，读取超时 5s，熔断阈值 50%
   - B 服务：连接超时 3s，读取超时 30s（大文件下载），熔断阈值 80%

4. **Feign 的调用链路性能优化：** 在一个 A -> B -> C -> D 的四层调用链中，每层的响应时间波动较大（100ms ~ 5s），请设计一个综合优化方案：
   - 超时时间如何分层设置？
   - 哪些层应该开启重试，哪些层不应该？
   - 如何通过异步调用优化同步阻塞问题？
   - 如何控制整体链路的超时时间？

---

## 7. 本章复盘速记清单

### 7.1 核心注解

| 注解 | 作用 | 使用位置 |
|------|------|---------|
| `@EnableFeignClients` | 启用 Feign 客户端，扫描 @FeignClient 接口 | Spring Boot 启动类 |
| `@FeignClient` | 声明一个 Feign 客户端接口 | 接口定义 |
| `@RefreshScope` | 配置动态刷新（配合 Nacos Config） | Feign 配置类 |
| `@LoadBalanced` | 启用负载均衡（配合 RestTemplate） | RestTemplate Bean |

### 7.2 @FeignClient 属性速查

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| name/value | String | (必填) | 目标服务名 |
| url | String | "" | 直连 URL（调试用） |
| path | String | "" | 路径前缀 |
| contextId | String | "" | Bean 别名（防冲突） |
| fallback | Class | void | 降级处理类 |
| fallbackFactory | Class | void | 降级工厂类 |
| configuration | Class[] | {} | 配置类 |

### 7.3 配置项速查

| 配置项 | 说明 | 推荐值 |
|--------|------|--------|
| `connect-timeout` | 连接超时 | 3000~5000ms |
| `read-timeout` | 读取超时 | 5000~10000ms |
| `logger-level` | 日志级别 | FULL (开发) / BASIC (生产) |
| `retryer` | 重试策略 | 幂等接口可配置，非幂等不重试 |
| `compression.request.enabled` | 请求压缩 | true |
| `compression.response.enabled` | 响应压缩 | true |

### 7.4 负载均衡算法

| 算法 | 说明 | 配置方式 |
|------|------|---------|
| RoundRobin | 轮询，请求依次分配到每个实例 | `spring.cloud.loadbalancer.configurations=round_robin` |
| Random | 随机，请求随机分配到某个实例 | `spring.cloud.loadbalancer.configurations=random` |
| NacosWeighted | 按权重分配，权重越高流量越多 | `spring.cloud.loadbalancer.nacos.enabled=true` |

### 7.5 Feign 常见问题速查

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| Header 丢失 | Feign 不自动透传 Header | RequestInterceptor 获取 RequestContextHolder |
| PathVariable 报错 | 未指定 value 属性 | 显式写 `@PathVariable("id")` |
| 多参数 GET 报错 | 参数未加 @RequestParam | 每个参数加 @RequestParam |
| 路径 404 | Feign 和 Controller 路径重复 | 使用 path 属性或检查路径拼接 |
| Bean 冲突 | 同名服务多个 FeignClient | 用 contextId 区分 |
| 异步 Header 丢失 | 子线程无 RequestAttributes | 手动传递 RequestAttributes |

---

## 8. 精通拓展补充-P2

### 8.1 Feign vs OpenFeign vs Spring Cloud OpenFeign 区别

#### 8.1.1 历史演进

```
Netflix Feign (2013年 Netflix 开源)         OpenFeign (社区版)         Spring Cloud OpenFeign
       │                                      │                             │
       ├── 原始 Feign 核心                     ├── Feign 捐赠给              ├── Spring Cloud 封装
       ├── 注解: @RequestLine                 │   OpenFeign 社区维护         ├── 注解: Spring MVC
       ├── 集成 Ribbon + Hystrix              ├── 核心 API 不变             ├── 集成 LoadBalancer
       └── Netflix 停止维护后闭源              └── 仍使用 @RequestLine        └── 集成 Spring Boot 自动配置
```

#### 8.1.2 核心区别对比

| 维度 | Feign (Netflix/原生) | OpenFeign (社区版) | Spring Cloud OpenFeign |
|------|---------------------|-------------------|----------------------|
| **Maven 坐标** | `com.netflix.feign:feign-core` | `io.github.openfeign:feign-core` | `org.springframework.cloud:spring-cloud-starter-openfeign` |
| **注解** | `@RequestLine`、`@Param` | `@RequestLine`、`@Param` | Spring MVC 注解:`@GetMapping`、`@RequestParam` 等 |
| **契约(Contract)** | Feign 原生 Contract | Feign 原生 Contract | SpringMvcContract(解析 Spring MVC 注解) |
| **编码器** | `GsonEncoder` / `JacksonEncoder` | 同上 | `SpringEncoder`(集成 HttpMessageConverter) |
| **负载均衡** | 需手动集成 Ribbon | 需手动集成 | 自动集成 Spring Cloud LoadBalancer |
| **熔断降级** | 需手动集成 Hystrix | 需手动集成 | 自动集成 Sentinel / CircuitBreaker |
| **Spring 整合** | 需手动配置 Bean | 需手动配置 Bean | `@EnableFeignClients` 自动扫描装配 |
| **配置方式** | 代码 Builder | 代码 Builder | application.yml + Java Config |
| **当前状态** | 已停止维护 | 社区维护(更新缓慢) | 活跃维护,随 Spring Cloud 版本更新 |

#### 8.1.3 一句话总结

**Spring Cloud OpenFeign = OpenFeign 核心 + Spring MVC 注解 + Spring Cloud LoadBalancer + Spring Boot 自动配置**。

```xml
<!-- 项目中引入的是 Spring Cloud OpenFeign -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
    <!-- 它传递依赖了 io.github.openfeign:feign-core -->
</dependency>
```

> 💡 日常开发中说的"Feign"通常指 Spring Cloud OpenFeign——它已经成为 Spring Cloud 微服务间调用的标准解决方案。

### 8.2 Feign 动态代理源码分析

Feign 使用 JDK 动态代理为每个 `@FeignClient` 接口创建代理对象。

```java
// FeignClientFactoryBean —— 核心工厂类
public class FeignClientFactoryBean
        implements FactoryBean<Object>, InitializingBean, ApplicationContextAware {

    private Class<?> type;        // FeignClient 接口类型
    private String name;          // 服务名
    private String url;           // 直连 URL
    private String path;          // 路径前缀
    private Class<?> fallback;    // 降级类

    @Override
    public Object getObject() {
        return getTarget();  // 创建代理对象
    }

    <T> T getTarget() {
        // 1. 构建 Feign.Builder
        Feign.Builder builder = feign(context);

        // 2. 处理 URL
        if (!StringUtils.hasText(url)) {
            // 从注册中心获取服务列表
            // 使用 LoadBalancer 负载均衡
            return builder.target(new Target<T>(
                    name, name, "http://" + name + path));
        } else {
            // 直连模式（固定 URL）
            return builder.target(new Target<T>(
                    name, name, url + path));
        }
    }

    protected Feign.Builder feign(AnnotationContext context) {
        // 3. 应用配置：解码器、编码器、拦截器、日志级别等
        Feign.Builder builder = Feign.builder()
                .contract(new SpringMvcContract())  // 支持 Spring MVC 注解
                .encoder(new SpringEncoder(messageConverters))
                .decoder(new SpringDecoder(messageConverters))
                .logger(new Slf4jLogger(type))
                .logLevel(loggerLevel);

        // 4. 添加拦截器
        for (RequestInterceptor interceptor : requestInterceptors) {
            builder.requestInterceptor(interceptor);
        }

        // 5. 添加重试器
        if (retryer != null) {
            builder.retryer(retryer);
        }

        return builder;
    }
}
```

#### Feign 调用流程

```
FeignClient.getUser(1L)
    │
    ▼
JDK 动态代理 → InvocationHandler.invoke()
    │
    ▼
SynchronousMethodHandler.invoke(args[])
    │
    ├── 1. 创建 RequestTemplate（解析注解、填充参数）
    │     └─ 应用 all RequestInterceptor（拦截器链）
    │
    ├── 2. 通过 LoadBalancer 选择一个目标实例
    │     └─ 解析服务名 → 获取实例列表 → 算法选择
    │
    ├── 3. 编码请求体（Encoder）
    ├── 4. 执行 HTTP 请求（Client.execute()）
    │     └─ Apache HttpClient / OkHttp / JDK HttpURLConnection
    │
    ├── 5. 解码响应体（Decoder）
    ├── 6. 重试判断：如果失败且满足重试条件，回到步骤 2
    │
    └── 7. 返回结果
```

### 8.3 Feign 性能优化

#### 8.3.1 替换 HTTP 客户端

Feign 默认使用 `HttpURLConnection`（不支持连接池），建议替换为 Apache HttpClient 或 OkHttp。

```xml
<!-- 使用 Apache HttpClient 作为 Feign 底层 -->
<dependency>
    <groupId>io.github.openfeign</groupId>
    <artifactId>feign-httpclient</artifactId>
</dependency>

<!-- 或者使用 OkHttp -->
<dependency>
    <groupId>io.github.openfeign</groupId>
    <artifactId>feign-okhttp</artifactId>
</dependency>
```

```yaml
spring:
  cloud:
    openfeign:
      httpclient:
        enabled: true          # 启用 Apache HttpClient（带连接池）
        max-connections: 200   # 最大连接数
        max-connections-per-route: 50  # 单个路由最大连接数
        time-to-live: 900      # 连接存活时间（秒）
#      okhttp:
#        enabled: true        # 启用 OkHttp（与 httpclient 二选一）
```

#### 8.3.2 请求/响应压缩

```yaml
spring:
  cloud:
    openfeign:
      compression:
        request:
          enabled: true
          mime-types: application/json,application/xml
          min-request-size: 2048   # 超过 2KB 的请求才压缩
        response:
          enabled: true
```

#### 8.3.3 缓存与连接池调优

| 配置 | 说明 | 推荐值 |
|------|------|--------|
| `feign.httpclient.max-connections` | 总连接数 | 200~500 |
| `feign.httpclient.max-connections-per-route` | 单个服务的连接数 | 50~100 |
| `feign.httpclient.time-to-live` | 连接存活时间 | 900s |
| `feign.okhttp.read-timeout` | 读取超时 | 与业务匹配 |
| `feign.okhttp.connect-timeout` | 连接超时 | 5000ms |

### 8.4 Feign 与 Hystrix / Sentinel 集成

```yaml
# 启用 Feign 的 Sentinel 或 Hystrix 支持
# 方式一：Sentinel（推荐）
spring:
  cloud:
    sentinel:
      enabled: true
      transport:
        dashboard: localhost:8080

feign:
  sentinel:
    enabled: true

# 方式二：Hystrix（已进入维护模式，不推荐新项目使用）
# feign:
#   hystrix:
#     enabled: true
```

```java
// 使用 Sentinel 做 Feign 降级
@FeignClient(name = "user-service", path = "/user",
             fallback = UserFeignClientFallback.class)
public interface UserFeignClient {
    @GetMapping("/{id}")
    User getUser(@PathVariable("id") Long id);
}

// 降级实现类：当调用失败或熔断时执行
@Component
public class UserFeignClientFallback implements UserFeignClient {
    @Override
    public User getUser(Long id) {
        // 返回默认值或兜底逻辑
        User fallbackUser = new User();
        fallbackUser.setId(id);
        fallbackUser.setName("默认用户");
        fallbackUser.setEmail("unknown@example.com");
        return fallbackUser;
    }
}
```

```java
// 使用 fallbackFactory 获取异常原因（推荐方式）
@FeignClient(name = "user-service", path = "/user",
             fallbackFactory = UserFeignClientFallbackFactory.class)
public interface UserFeignClient {
    @GetMapping("/{id}")
    User getUser(@PathVariable("id") Long id);
}

@Component
public class UserFeignClientFallbackFactory
        implements FallbackFactory<UserFeignClient> {

    private static final Logger log =
            LoggerFactory.getLogger(UserFeignClientFallbackFactory.class);

    @Override
    public UserFeignClient create(Throwable cause) {
        log.error("调用 user-service 失败", cause);
        return id -> {
            User fallbackUser = new User();
            fallbackUser.setId(id);
            fallbackUser.setName("降级用户（" + cause.getMessage() + "）");
            return fallbackUser;
        };
    }
}
```

### 8.5 设计模式运用

| 设计模式 | 应用位置 | 说明 |
|---------|---------|------|
| 代理模式 | `FeignClientFactoryBean` | JDK 动态代理为接口生成 HTTP 调用实现 |
| 装饰器模式 | `RequestInterceptor` | 拦截器链对请求进行逐层装饰 |
| 策略模式 | `LoadBalancer` | 不同的负载均衡算法可自由切换 |
| 工厂模式 | `Feign.Builder` | 构建不同配置的 Feign 客户端 |
| 适配器模式 | `SpringMvcContract` | 将 Spring MVC 注解适配为 Feign 的 MethodMetadata |
| 模板方法模式 | `LoadBalancerFeignClient` | 定义请求-负载均衡-执行的模板流程 |
| 观察者模式 | 日志记录 | 监听请求生命周期输出日志 |

### 8.6 常见调优清单

| 优化项 | 操作 | 效果 |
|--------|------|------|
| 替换 HTTP 客户端 | 启用 Apache HttpClient 或 OkHttp | 连接池复用，性能提升 2-5 倍 |
| 开启压缩 | 开启 request/response 压缩 | 减少带宽，大 JSON 有效 |
| 调整连接池 | 增大 max-connections | 支持高并发调用 |
| 合理超时 | 按服务逐个配置超时时间 | 防止雪崩，快速失败 |
| 熔断降级 | 集成 Sentinel | 防止级联故障 |
| 请求拦截器缓存 | 减少拦截器中重复计算 | 降低调用延迟 |
| 禁用 404 解码 | `decode404: false` | 快速失败，便于排错 |
| 日志级别 | 生产环境用 BASIC 或 NONE | 减少日志 IO 开销 |

> 🎯 本章学习了 OpenFeign 服务调用的完整知识体系：从声明式接口的定义到动态代理原理，从超时重配到负载均衡策略，从 Header 透传到熔断降级。掌握这些知识后，你可以优雅地实现微服务间的通信，并针对不同场景进行调优。

---

> 本文档属于 `02-中间件与微服务工程/06-Spring全家桶与微服务` 模块，P1 就业必备层级。OpenFeign + LoadBalancer 是 Spring Cloud 微服务间同步调用的基础设施，需重点掌握 Header 透传、超时配置、安全重试和降级保护。
