# 05 动态刷新与 Bus 集成速查

> 刷新篇：**配置改了怎么让服务不重启就生效**——`@RefreshScope` 重建机制、`/actuator/refresh` 单实例刷新、**Bus 广播全网刷新（bus-refresh）**、Webhook 全自动链路、刷新失效排查——与 [Spring Cloud Bus](../Spring Cloud Bus/00-Spring Cloud Bus知识体系总览.md) 系列配合阅读。

---

## 📚 目录

1. [刷新三要素：改哪里、刷什么、谁重建](#1-刷新三要素改哪里刷什么谁重建)
2. [@RefreshScope：重建机制详解](#2-refreshscope重建机制详解)
3. [单实例刷新：/actuator/refresh](#3-单实例刷新actuatorrefresh)
4. [全网刷新：Bus 集成（bus-refresh）](#4-全网刷新bus-集成bus-refresh)
5. [Webhook 全自动刷新链路](#5-webhook-全自动刷新链路)
6. [刷新失效排查清单](#6-刷新失效排查清单)

---

## 1. 刷新三要素：改哪里、刷什么、谁重建

```text
一次"动态刷新"的完整链路：
  ① 改哪里：Git 配置仓库（提交 push）
  ② 刷什么：Config Server 重新 fetch → 客户端再拉一次最新配置（refresh 事件触发）
  ③ 谁重建：@RefreshScope 标注的 Bean 在刷新事件后重新创建（读新值）
```

| 环节 | 工具 | 说明 |
|------|------|------|
| 配置变更 | Git 提交 | 版本化、可回滚 |
| Server 拉新 | 客户端 refresh 时 Server 自动 fetch（或 fetch-interval 缓存） | 无需重启 Server |
| 客户端拿新 | /actuator/refresh 或 Bus 广播 | 触发 Environment 刷新 + Bean 重建 |
| Bean 重建 | @RefreshScope | 只有标注的 Bean 重建 |

> 🎯 关键认知：**"刷新"不是把配置推给客户端，而是"客户端主动再拉一次"**——refresh 端点/事件只是触发信号，配置还是从 Server 拉——这也是 Config 与 Nacos（长轮询推送）的本质区别（[08 篇](08-生产实践与选型避坑.md)）。

## 2. @RefreshScope：重建机制详解

### 2.1 用法

```java
@RefreshScope                            // ★ 刷新时重建该 Bean
@RestController
public class OrderPropertiesController {
    @Value("${order.timeout:5000}")
    private int timeout;

    @GetMapping("/timeout")
    public int getTimeout() { return timeout; }
}

@RefreshScope
@ConfigurationProperties(prefix = "order")
public class OrderProperties {           // 绑定类同样适用
    private int timeout;
    private int retryCount;
    // getter/setter
}
```

### 2.2 原理

```text
@RefreshScope = 自定义的 Bean scope（RefreshScope 类）：
  ├── 普通 scope：容器创建 Bean 一次，复用
  ├── RefreshScope：Bean 保存在 scope 缓存里
  ├── 刷新事件 → RefreshScope 清缓存 → 下次访问 Bean 时重建（重新注入最新属性）
  └── 所以：@Value/@ConfigurationProperties 在重建时重新解析
```

### 2.3 什么能刷、什么不能刷

| 目标 | 能刷？ | 说明 |
|------|:---:|------|
| @Value 字段 | ✅ | Bean 重建时重新注入 |
| @ConfigurationProperties Bean | ✅ | 重建时重新绑定 |
| 普通单例 Bean | ❌ | 不标注 @RefreshScope 不重建 |
| **连接池/HTTP 客户端（初始化时建连接）** | ⚠️ | 重建 = 重新初始化（连接池重建可能断连，需评估） |
| **@RefreshScope + @FeignClient / @Scheduled 等代理 Bean** | ⚠️ | 代理与 scope 缓存交互复杂，踩坑重灾区 |
| 常量/static 字段 | ❌ | 编译期就定了 |
| 原生镜像（AOT） | ❌ | 官方：refresh scope 不支持 native image |

> ⚠️ **最常见误区**：以为"刷新 = 全部配置生效"——**只有 @RefreshScope Bean 会重建**；普通 @Service 里 new 出来的依赖、static 常量、初始化期建立连接的对象都不会变（[08 篇](08-生产实践与选型避坑.md) 坑位表）。

## 3. 单实例刷新：/actuator/refresh

### 3.1 暴露端点

```yaml
# 客户端配置
management:
  endpoints:
    web:
      exposure:
        include: refresh, health
```

### 3.2 刷新

```bash
# 只刷新当前实例（本地开发/定向排查）
curl -X POST http://localhost:8081/actuator/refresh
# 返回：被变更的属性 key 列表（JSON 数组）
["order.timeout"]
```

| 维度 | 说明 |
|------|------|
| 作用域 | 当前实例 |
| 触发 | 手动 POST 或外部定时 |
| 返回 | 变更的 key 列表（可用于断言刷新成功） |
| 前提 | spring-boot-starter-actuator + 端点暴露 + @RefreshScope Bean |

> 💡 单实例刷新适用：**验证配置、灰度单机验证**；生产 50 个实例逐个刷不现实——上 Bus（下节）。

## 4. 全网刷新：Bus 集成（bus-refresh）

### 4.1 架构

```text
             Git 仓库（配置变更 push）
                  │
                  ▼
       Config Server ──► 任意客户端 POST /actuator/bus-refresh
                  │
            发布 RefreshRemoteApplicationEvent 到 MQ
                  │
        ┌─────────┼─────────┐
        ▼         ▼         ▼
    实例A       实例B      实例C      ← 所有订阅实例消费事件
    各自 refresh    各自 refresh    各自 refresh
```

### 4.2 依赖与配置（客户端，所有需要刷新的服务）

```xml
<!-- 每个微服务都加（或 bus-amqp） -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-bus-amqp</artifactId>
</dependency>
```

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
  cloud:
    bus:
      id: ${spring.application.name}:${server.port}:${random.value}   # ★ 实例唯一 ID
      refresh:
        enabled: true                  # ★ 2021.0+ 默认 false，必须显式开
management:
  endpoints:
    web:
      exposure:
        include: bus-refresh, health
```

### 4.3 刷新命令三形态

```bash
# ① 全网刷新（向任意一个实例发）
curl -X POST http://localhost:8081/actuator/bus-refresh

# ② 定向刷新：只刷某个服务的所有实例
curl -X POST "http://localhost:8081/actuator/bus-refresh/order-service:**"

# ③ 定向刷新：只刷某个具体实例
curl -X POST "http://localhost:8081/actuator/bus-refresh/order-service:8082"
```

### 4.4 与单实例 refresh 的取舍

| 维度 | /actuator/refresh | /actuator/bus-refresh |
|------|-------------------|----------------------|
| 范围 | 单实例 | 全网（或定向） |
| 依赖 | 无 | MQ（RabbitMQ/Kafka）+ Bus |
| 场景 | 本地验证、单机排查 | 生产全网刷新 |
| 可靠性 | 直接生效 | fire-and-forget（事件丢了不重试） |
| SCA 替代 | — | Nacos 内置推送，不需要 Bus |

> 🎯 面试必答：**"Config 怎么做到全网刷新？"**——配套 **Spring Cloud Bus**：任意实例收到 `/actuator/bus-refresh` → 发布 `RefreshRemoteApplicationEvent` 到 MQ → 所有订阅实例消费事件后各自执行 refresh（@RefreshScope 重建）——**一次请求全网生效**；注意事件可能丢（fire-and-forget），关键变更后要抽查实例。

## 5. Webhook 全自动刷新链路

### 5.1 完整链路

```text
开发者 push Git ──► Git 平台 Webhook（push 事件）──► POST Config Server /monitor
                                                       │
                                                       发布 RefreshRemoteApplicationEvent（Bus）
                                                       ▼
                                              全网实例刷新（无需任何手工操作）
```

### 5.2 配置

```yaml
# Config Server（需 bus 依赖 + monitor 支持）
spring:
  rabbitmq:
    host: localhost
  cloud:
    config:
      server:
        monitor:
          # GitHub 的默认路径（GitLab 用 /monitor 自定义头部）
management:
  endpoints:
    web:
      exposure:
        include: bus-refresh, health
```

```text
Git 平台配置 Webhook：
  URL: http://config-server:8888/monitor
  事件: push（Github）或 Push Hook（GitLab）
  注意: 内网可达（Git 平台能访问到 Config Server）
```

> ⚠️ 安全提醒：**/monitor 暴露在公网 = 任何人都能触发全网刷新**——必须加认证（Basic Auth/TLS/IP 白名单，见 [07 篇](07-安全认证与生产运维.md)）。

### 5.3 全自动 vs 半自动

| 模式 | 操作 | 适用 |
|------|------|------|
| 纯手动 | 改 Git → 手动 bus-refresh | 低频率配置、无 MQ 环境 |
| 半自动 | 改 Git → 脚本/流水线调 bus-refresh | 有 CI，想保留控制 |
| 全自动 | Webhook → /monitor | 高频配置变更、成熟团队 |

## 6. 刷新失效排查清单

### 6.1 逐段排查链路

| # | 排查段 | 验证方法 | 常见失败点 |
|---|--------|---------|-----------|
| ① | Git 是否改了 | 仓库 commit 是否 push | 忘 push / 分支不对 |
| ② | Server 是否拿到新版本 | `GET /{app}/{profile}` 看 version 字段 | 工作目录落后（force-pull） |
| ③ | 刷新信号是否发出 | MQ 队列看 RefreshRemoteApplicationEvent | refresh.enabled=false / 端点没暴露 |
| ④ | 实例是否收到 | 实例日志 RefreshScopeRefreshedEvent | bus.id 重复（事件被去重误判） |
| ⑤ | Bean 是否重建 | 日志 Refresh scope / 字段值变化 | 目标 Bean 没标 @RefreshScope |

### 6.2 高频坑速查

| 现象 | 原因 | 解决 |
|------|------|------|
| 刷新后值没变 | Bean 没标 @RefreshScope | 标注目标 Bean |
| 全网刷新没生效 | `spring.cloud.bus.refresh.enabled` 还是 false | 显式开启（2021.0+ 默认关） |
| 只有部分实例刷新 | bus.id 重复（所有实例同 ID） | id 用 app:port:random |
| refresh 端点 404 | actuator 没暴露 refresh | include: refresh |
| Server 一直返回旧配置 | fetch 缓存/工作目录落后 | fetch-interval: 0 或 force-pull |
| 刷了但连接池没变 | 连接池不是 @RefreshScope 或重建有状态连接 | 评估重建影响，或连接池动态刷新（如 Hikari 需重建） |

> 💡 排查口诀：**"改没改 → 拉到没 → 信号发没发 → 实例收没收到 → Bean 重建没"**——五段式排查，每一段都有日志/响应可查（[08 篇](08-生产实践与选型避坑.md) 也有）。

---

**下一模块**：[06-加密解密与密钥管理速查](06-加密解密与密钥管理速查.md)　**返回总览**：[00-Spring Cloud Config知识体系总览](00-Spring Cloud Config知识体系总览.md)

**【参考来源】**：[Spring Cloud Config 5.0.4（Push 通知与 Bus）](https://docs.spring.io/spring-cloud-config/reference/server/push-notifications-and-spring-cloud-bus.html)、[Spring Cloud Bus 5.0.2 官方文档](https://docs.spring.io/spring-cloud-bus/docs/current/reference/html/index.html)、[Spring Cloud Bus 系列（本仓库）](../Spring Cloud Bus/00-Spring Cloud Bus知识体系总览.md)
