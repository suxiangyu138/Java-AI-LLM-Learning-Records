# 02 - Dubbo 快速入门与配置

> 🎯 从 XML 到注解、从 API 到 Spring Boot Starter — 三种方式接入 Dubbo，跑通第一个 Provider-Consumer 调用

---

## 目录

1. [环境准备](#1-环境准备)
2. [Spring Boot Starter 方式（⭐ 推荐）](#2-spring-boot-starter-方式-推荐)
3. [核心配置项](#3-核心配置项)
4. [多协议暴露](#4-多协议暴露)
5. [多注册中心](#5-多注册中心)

---

## 1. 环境准备

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 8 / 17 / 21 | Dubbo 3.x 最低 JDK 8 |
| Dubbo | 3.2.x / 3.3.x | 当前稳定版 |
| Zookeeper | 3.7+ | 注册中心（也可用 Nacos） |
| Spring Boot | 2.7 / 3.x | 推荐 Spring Boot 3 + Dubbo 3.3 |

### Maven BOM 统一版本

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.apache.dubbo</groupId>
            <artifactId>dubbo-bom</artifactId>
            <version>3.3.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

---

## 2. Spring Boot Starter 方式（⭐ 推荐）

### 2.1 公共 API 模块

```java
// dubbo-api — 供 Provider 和 Consumer 共享
public interface UserService {
    User getUser(Long id);
    List<User> listUsers(String keyword);
}

@Data
public class User implements Serializable {   // ⚠️ 必须序列化
    private Long id;
    private String name;
    private Integer age;
}
```

### 2.2 Provider（服务提供方）

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.apache.dubbo</groupId>
    <artifactId>dubbo-spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.dubbo</groupId>
    <artifactId>dubbo-registry-zookeeper</artifactId>
</dependency>
```

```yaml
# application.yml
dubbo:
  application:
    name: user-provider
  registry:
    address: zookeeper://127.0.0.1:2181
  protocol:
    name: dubbo
    port: 20880
```

```java
@DubboService   // ⭐ Dubbo 3.x 注解，替代 @Service
public class UserServiceImpl implements UserService {
    @Override
    public User getUser(Long id) {
        return new User(id, "张三", 25);
    }
}

@SpringBootApplication
@EnableDubbo    // 开启 Dubbo
public class ProviderApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProviderApplication.class, args);
    }
}
```

### 2.3 Consumer（服务消费方）

```yaml
# application.yml
dubbo:
  application:
    name: user-consumer
  registry:
    address: zookeeper://127.0.0.1:2181
```

```java
@RestController
public class UserController {

    @DubboReference   // ⭐ Dubbo 3.x 注解，替代 @Reference
    private UserService userService;

    @GetMapping("/user/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getUser(id);  // 像本地方法一样调用！
    }
}
```

---

## 3. 核心配置项

| 配置 | 说明 | 推荐值 |
|------|------|--------|
| `dubbo.application.name` | 应用名（唯一标识） | user-provider |
| `dubbo.registry.address` | 注册中心地址 | zookeeper://127.0.0.1:2181 |
| `dubbo.protocol.name` | 协议 | dubbo (dubbo2) / tri (triple) |
| `dubbo.protocol.port` | 协议端口 | 20880 (dubbo) / 50051 (triple) |
| `dubbo.provider.timeout` | 默认超时(ms) | 3000 |
| `dubbo.provider.retries` | 默认重试次数 | 2 |
| `dubbo.consumer.check` | 启动时检查 Provider 可用 | false (开发) / true (生产) |
| `dubbo.consumer.timeout` | 调用超时 | — |

### 常用属性配置

```yaml
dubbo:
  application:
    name: user-provider
    logger: slf4j
  registry:
    address: zookeeper://127.0.0.1:2181
    timeout: 5000               # 注册中心超时
  protocol:
    name: dubbo
    port: 20880
    threads: 200                # 业务线程池大小
  provider:
    timeout: 3000
    retries: 0                  # 幂等接口可设 >0
    token: true                 # 令牌验证
```

---

## 4. 多协议暴露

```yaml
dubbo:
  protocols:
    dubbo-protocol:             # 协议1：Dubbo2（TCP）
      id: dubbo
      name: dubbo
      port: 20880
    triple-protocol:            # 协议2：Triple（HTTP/2）
      id: triple
      name: tri
      port: 50051
```

```java
// 指定协议暴露
@DubboService(protocol = "dubbo,triple")
public class UserServiceImpl implements UserService { }
```

---

## 5. 多注册中心

```yaml
dubbo:
  registries:
    zk-registry:                # 注册中心1：ZooKeeper
      address: zookeeper://127.0.0.1:2181
    nacos-registry:             # 注册中心2：Nacos
      address: nacos://127.0.0.1:8848
```

```java
// 指定注册中心
@DubboService(registry = {"zk-registry", "nacos-registry"})
public class UserServiceImpl implements UserService { }
```

> 🎯 **最佳实践**：新项目统一用 `@DubboService` + `@DubboReference`（Dubbo 3.x 标准注解），配置文件用 YAML，注册中心优先 Nacos（注册+配置二合一）。
