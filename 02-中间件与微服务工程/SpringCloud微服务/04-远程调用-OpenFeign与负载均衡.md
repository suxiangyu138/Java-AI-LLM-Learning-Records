# 04 - 远程调用：OpenFeign 与负载均衡

> 🎯 OpenFeign = 声明式 HTTP 客户端，LoadBalancer = 客户端负载均衡 — 两者组合替代 RestTemplate + Ribbon，是 Spring Cloud 微服务间通信的标准姿势

---

## 目录

1. [OpenFeign 概述](#1-openfeign-概述)
2. [Feign 声明式调用](#2-feign-声明式调用)
3. [日志与拦截器](#3-日志与拦截器)
4. [超时与重试配置](#4-超时与重试配置)
5. [替换 RestTemplate 的推荐方案](#5-替换-resttemplate-的推荐方案)

---

## 1. OpenFeign 概述

> OpenFeign 让你通过 Java 接口 + 注解声明远程调用，像调用本地方法一样调用远程服务。

```text
RestTemplate 方式：
  restTemplate.getForObject("http://user-service/users/" + id, User.class);

Feign 方式：
  @FeignClient("user-service")
  interface UserClient {
      @GetMapping("/users/{id}")
      User getUser(@PathVariable Long id);   // ← 像本地方法！
  }
```

### 依赖

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
```

---

## 2. Feign 声明式调用

### 2.1 基本用法

```java
// 1. 定义 Feign 接口
@FeignClient(name = "user-service", path = "/users")   // name = 服务名
public interface UserClient {

    @GetMapping("/{id}")
    User getUser(@PathVariable Long id);

    @GetMapping
    List<User> listUsers(@RequestParam String keyword);

    @PostMapping
    Result<User> createUser(@RequestBody User user);

    // 复杂 URL：GET /users/{id}/orders?status={status}
    @GetMapping("/{id}/orders")
    List<Order> getUserOrders(@PathVariable Long id, @RequestParam String status);
}

// 2. 注入使用
@RestController
public class OrderController {

    @Autowired
    private UserClient userClient;

    @GetMapping("/order/{id}")
    public Order getOrder(@PathVariable Long id) {
        User user = userClient.getUser(id);   // 远程调用！
        return buildOrder(id, user);
    }
}

// 3. 启动类
@SpringBootApplication
@EnableFeignClients    // ⭐ 开启 Feign
public class OrderApplication { }
```

### 2.2 Feign 接口规范

| 注解 | 说明 | 示例 |
|------|------|------|
| `@FeignClient` | 声明 Feign 客户端 | `name="user-service"`, `path="/api"`, `url="..."` |
| `@GetMapping` | GET 请求 | `@GetMapping("/{id}")` |
| `@PostMapping` | POST 请求 | `@PostMapping(consumes = "application/json")` |
| `@PutMapping` | PUT 请求 | 同上 |
| `@DeleteMapping` | DELETE 请求 | 同上 |
| `@PathVariable` | 路径参数 | `@PathVariable("id")` |
| `@RequestParam` | 查询参数 | `@RequestParam("name")` |
| `@RequestBody` | 请求体 | `@RequestBody User user` |
| `@RequestHeader` | 请求头 | `@RequestHeader("Authorization")` |

---

## 3. 日志与拦截器

### 3.1 Feign 日志级别

```yaml
logging:
  level:
    com.example.feign.UserClient: DEBUG    # Feign 接口的日志级别
```

```java
@Configuration
public class FeignConfig {
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;   // NONE / BASIC / HEADERS / FULL
    }
}
```

### 3.2 请求拦截器（⭐ 传递 Token/TraceId）

```java
@Component
public class FeignAuthInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        // 从当前请求上下文获取 Token，传递给下游
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            String token = attributes.getRequest().getHeader("Authorization");
            template.header("Authorization", token);
        }
        // 传递 TraceId
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            template.header("X-Trace-Id", traceId);
        }
    }
}
```

---

## 4. 超时与重试配置

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          user-service:               # 针对特定服务
            connect-timeout: 5000     # 连接超时 5s
            read-timeout: 10000       # 读取超时 10s
          default:                    # 全局默认
            connect-timeout: 3000
            read-timeout: 5000
      compression:
        request:
          enabled: true               # 请求 Gzip 压缩
        response:
          enabled: true               # 响应 Gzip 压缩
```

### Feign 内置重试

```java
@Bean
public Retryer feignRetryer() {
    return new Retryer.Default(100, 1000, 3);  // 初始100ms, 最大1s, 重试3次
    // return Retryer.NEVER_RETRY;             // 不重试（⭐ 非幂等接口）
}
```

> ⚠️ Feign 默认重试器会重试 5 次！非幂等接口务必设为 `Retryer.NEVER_RETRY`。

---

## 5. 替换 RestTemplate 的推荐方案

| 方案 | 适用场景 | 代码量 |
|------|----------|:---:|
| **OpenFeign** | ⭐ 90% 的服务间调用 | 少 |
| **RestTemplate + @LoadBalanced** | 简单 GET 请求、非标准调用 | 中 |
| **WebClient（响应式）** | Spring WebFlux 项目 | 中 |
| **Dubbo** | 高性能 RPC 需求 | 少（需额外配置） |

> 🎯 **最佳实践**：服务间调用统一用 OpenFeign，简洁、声明式、自动负载均衡。Token/TraceId 透传用 RequestInterceptor，非幂等接口用 `Retryer.NEVER_RETRY`。
