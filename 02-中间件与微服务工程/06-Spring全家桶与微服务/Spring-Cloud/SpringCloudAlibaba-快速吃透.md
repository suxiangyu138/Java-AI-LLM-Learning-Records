# 快速吃透 Spring Cloud Alibaba

> **一句话**：阿里微服务全家桶 = Nacos（注册+配置）+ Dubbo（通信）+ Sentinel（防护）+ Gateway（入口）+ Seata（数据一致）。

---

## 一、核心组件 6 件套

| 组件 | 作用 |
|------|------|
| **Nacos** | 注册中心 + 配置中心（重中之重） |
| **Sentinel** | 流量控制、熔断降级 |
| **Dubbo** | 高性能 RPC 远程调用（替代 Feign） |
| **Gateway** | 统一网关 |
| **Seata** | 分布式事务 |
| OSS/SMS | 阿里云服务快速集成 |

---

## 二、快速上手

### 1. Nacos 注册中心

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
  application:
    name: user-service
```

```java
@SpringBootApplication
@EnableDiscoveryClient
public class UserApplication {}
```

### 2. Nacos 配置中心

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
```

```yaml
# bootstrap.yml（优先级高于 application.yml）
spring:
  cloud:
    nacos:
      config:
        server-addr: 127.0.0.1:8848
        file-extension: yml
```

```java
@RefreshScope  // 配置自动刷新
@RestController
public class ConfigController {}
```

### 3. Dubbo 远程调用

```java
// 服务提供者
@DubboService
public class UserServiceImpl implements UserService {}

// 服务消费者
@DubboReference
private UserService userService;
```

---

## 三、完整调用链路

```text
客户端 → Gateway（鉴权/路由）
          → Nacos（服务发现）
            → Dubbo/HTTP 远程调用
              → 业务服务执行
                → Sentinel（熔断降级兜底）
                  → Seata（分布式事务保证一致性）
```

---

## 四、高频面试概念

| 概念 | 一句话 |
|------|--------|
| 注册中心 | Nacos 保存服务列表，心跳检测健康状态 |
| 配置中心 | 统一配置、环境隔离、动态发布 |
| 负载均衡 | 多实例自动分发请求 |
| 服务熔断 | 下游故障快速失败，避免连锁崩溃 |
| 服务降级 | 高峰期关闭非核心接口 |
| 分布式事务 | Seata AT 模式主流方案 |

---

## 五、2~3 天学习路线

```text
Day 1：部署 Nacos → 搭建 生产者+消费者 → Nacos 注册 + 配置 + Dubbo 调用
Day 2：接入 Gateway 网关 → Sentinel 限流熔断
Day 3：了解 Seata 分布式事务场景
```
