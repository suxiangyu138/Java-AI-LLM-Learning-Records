# 07 - Dubbo 3 新特性与 Spring 生态

> 🎯 Dubbo 3.x 是里程碑版本 — Triple 协议、应用级服务发现、云原生支持；同时与 Spring Cloud Alibaba 深度集成，是 Java 微服务的推荐技术栈

---

## 目录

1. [Dubbo 3.x 核心新特性](#1-dubbo-3x-核心新特性)
2. [Triple 协议深度](#2-triple-协议深度)
3. [应用级服务发现迁移](#3-应用级服务发现迁移)
4. [Spring Cloud Alibaba 集成](#4-spring-cloud-alibaba-集成)
5. [Dubbo + Spring Boot 3 实战](#5-dubbo--spring-boot-3-实战)

---

## 1. Dubbo 3.x 核心新特性

| 特性 | Dubbo 2.x | Dubbo 3.x | 影响 |
|------|-----------|-----------|------|
| 服务发现 | 接口级 | **应用级** | 注册中心压力降低 100 倍 |
| 新协议 | dubbo (TCP) | **Triple (HTTP/2)** | 跨语言、网关友好 |
| 云原生 | — | **Proxyless Mesh** | 支持 Istio 直连 |
| 跨语言 | — | **多语言 SDK** | Go/Node.js/Python |
| 可观测性 | — | **内置 Tracing/Metrics** | 无需额外 Agent |
| JDK | JDK 8 | JDK 8 / 17 / 21 | 支持 Virtual Threads |

### 1.1 下一代协议：Triple

```text
Triple = HTTP/2 + Protobuf + Stream

核心价值：
  → 完全兼容 gRPC 协议（gRPC 客户端可直接调 Triple 服务）
  → 支持 Client/Server/Bidirectional Streaming
  → 天然穿透网关（HTTP/2 协议通用）
  → 跨语言调用（Go/Python/Node.js SDK）
```

### 1.2 应用级发现

```text
Dubbo 2.x 接口级：
  /dubbo/com.example.UserService/providers/[url1, url2, ...]
  /dubbo/com.example.OrderService/providers/[url1, url2, ...]
  → 100 接口 × 100 节点 = 10000 条注册数据

Dubbo 3.x 应用级：
  /dubbo/user-provider/instances/[ip1, ip2, ...]
  → 1 应用 × 100 节点 = 100 条注册数据
  → 接口定义迁移到元数据中心
```

---

## 2. Triple 协议深度

### 2.1 流式调用三模式

```java
// 1. Unary（一对一）：传统请求-响应
User user = userService.getUser(1L);

// 2. Server Stream（一对多）：服务端推送多条数据
userService.listUsers(new StreamObserver<User>() {
    @Override public void onNext(User user) { /* 收到一条 */ }
    @Override public void onCompleted() { /* 完成 */ }
});

// 3. Bidirectional Stream（双向流）：客户端和服务端互相推送
StreamObserver<Message> serverStream = chatService.chat(new StreamObserver<Message>() {
    @Override public void onNext(Message msg) { /* 收到服务端消息 */ }
});
serverStream.onNext(Message.of("Hello"));  // 发送消息
serverStream.onCompleted();
```

### 2.2 生成 IDL（Protobuf）

```protobuf
// user.proto
syntax = "proto3";
package com.example;
option java_package = "com.example.api";

service UserService {
  rpc GetUser (GetUserRequest) returns (User) {}
  rpc ListUsers (ListUsersRequest) returns (stream User) {}  // Server Stream
}

message GetUserRequest { int64 id = 1; }
message User { int64 id = 1; string name = 2; int32 age = 3; }
```

```xml
<!-- Maven Plugin 生成 Java 代码 -->
<plugin>
    <groupId>org.xolstice.maven.plugins</groupId>
    <artifactId>protobuf-maven-plugin</artifactId>
</plugin>
```

---

## 3. 应用级服务发现迁移

### 3.1 三种注册模式

```yaml
dubbo:
  application:
    register-mode: instance   # instance / interface / all
```

| 模式 | 说明 | 适用场景 |
|------|------|----------|
| `instance` | 仅应用级注册 | Dubbo 3.x 新项目 |
| `interface` | 仅接口级注册 (Dubbo 2.x) | 遗留系统 |
| `all` | 双注册（过渡方案） | Dubbo 2.x→3.x 迁移期间 |

### 3.2 平滑迁移

```text
阶段 1：register-mode=all（双注册）
  → 同时注册接口级和应用级，Dubbo 2.x 和 3.x 消费者都能发现

阶段 2：验证 3.x 消费者正常工作
  → 逐步升级消费者到 3.x

阶段 3：register-mode=instance（纯应用级）
  → 下线接口级注册，完成迁移
```

```yaml
# 迁移期配置
dubbo:
  application:
    register-mode: all           # 双注册过渡
    service-discovery:
      migration: FORCE_APPLICATION  # 强制使用应用级发现
```

---

## 4. Spring Cloud Alibaba 集成

### 4.1 技术栈对照

| Spring Cloud | Spring Cloud Alibaba | Dubbo 的角色 |
|-------------|---------------------|-------------|
| Feign + Ribbon | Dubbo RPC | 替代 Feign，高性能 RPC |
| Eureka / Consul | Nacos | 注册中心 + 配置中心 |
| Hystrix | Sentinel | 熔断降级 |
| Spring Cloud Gateway | Spring Cloud Gateway | 网关（不变） |
| Sleuth + Zipkin | SkyWalking | 链路追踪 |

### 4.2 集成配置

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.dubbo</groupId>
    <artifactId>dubbo-spring-boot-starter</artifactId>
</dependency>
```

```yaml
spring:
  application:
    name: user-provider
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
        namespace: prod

dubbo:
  application:
    name: ${spring.application.name}
  registry:
    address: spring-cloud://localhost   # ⭐ 使用 Spring Cloud 注册中心
  protocol:
    name: dubbo
    port: 20880
```

---

## 5. Dubbo + Spring Boot 3 实战

### 5.1 完整 POM

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>
</parent>

<dependencies>
    <dependency>
        <groupId>org.apache.dubbo</groupId>
        <artifactId>dubbo-spring-boot-starter</artifactId>
        <version>3.3.0</version>
    </dependency>
    <dependency>
        <groupId>org.apache.dubbo</groupId>
        <artifactId>dubbo-registry-nacos</artifactId>
        <version>3.3.0</version>
    </dependency>
    <dependency>
        <groupId>com.alibaba.nacos</groupId>
        <artifactId>nacos-client</artifactId>
        <version>2.3.0</version>
    </dependency>
</dependencies>
```

### 5.2 与 WebFlux 共存

```java
@DubboService(protocol = "tri")     // Triple 协议（HTTP/2）
public class UserServiceImpl implements UserService { }

@RestController
public class UserController {

    @DubboReference(protocol = "tri")
    private UserService userService;

    @GetMapping("/user/{id}")
    public Mono<User> getUser(@PathVariable Long id) {
        return Mono.fromFuture(
            RpcContext.getServiceContext().getCompletableFuture()
        ).thenReturn(userService.getUser(id));
    }
}
```

### 5.3 Virtual Threads（Java 21）

```yaml
dubbo:
  protocol:
    name: dubbo
    threadpool: virtual    # Dubbo 3.3+ 支持虚拟线程
```

---

> 🎯 **Dubbo 3.x 是 Java 微服务的推荐版**：Triple 协议解决跨语言、应用级发现降低注册压力、Spring Cloud Alibaba 无缝集成。新项目直接上 Dubbo 3.x + Nacos + Triple 协议。
