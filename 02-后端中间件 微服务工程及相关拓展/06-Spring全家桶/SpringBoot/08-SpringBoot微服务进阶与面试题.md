# 08 - Spring Boot 微服务进阶与面试题

> 定位：微服务组件接入、事件驱动、Boot 4 新特性、Spring Boot 面试题精选——收官篇

## 📚 目录

1. [微服务组件接入](#1-微服务组件接入)
2. [事件驱动与异步](#2-事件驱动与异步)
3. [Boot 4 新特性回顾](#3-boot-4-新特性回顾)
4. [Spring Boot 面试题精选](#4-spring-boot-面试题精选)

---

## 1. 微服务组件接入

### 1.1 服务注册发现（Nacos）

```yaml
# 服务注册（Nacos）
spring:
  cloud:
    nacos:
      discovery:
        server-addr: nacos:8848
        namespace: prod
  application:
    name: order-service          # ⚠️ 注册名（服务间调用用）

# 服务发现与调用（OpenFeign 或 Boot 4 @HttpServiceClient）
# @EnableDiscoveryClient  // 注册
# @LoadBalanced  // 负载均衡（Spring Cloud LoadBalancer）
```

### 1.2 微服务组件全景

| 组件 | 作用 | 方案 |
|------|------|------|
| 注册中心 | 服务发现 | Nacos（国产主流） |
| 配置中心 | 动态配置 | Nacos |
| 网关 | 统一入口 | Spring Cloud Gateway |
| 负载均衡 | 服务间路由 | LoadBalancer |
| 熔断限流 | 稳定性 | Sentinel |
| 链路追踪 | 可观测 | OTel + Jaeger |
| 消息 | 异步解耦 | RabbitMQ/Kafka |

> 🎯 **要点**：Spring Cloud 微服务 = Boot 应用 + 注册发现 + 配置中心 + 网关 + 熔断。**Nacos 一站式**（注册 + 配置）是 2026 国产主流方案。

---

## 2. 事件驱动与异步

### 2.1 应用内事件

```java
// ① 发布事件
@Service
public class OrderService {
    private final ApplicationEventPublisher publisher;

    public void createOrder(OrderDTO dto) {
        orderRepo.save(order);
        // ⚠️ 发布事件（解耦：下单后通知）
        publisher.publishEvent(new OrderCreatedEvent(order));
    }
}

// ② 监听事件
@Component
public class OrderEventListener {
    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        // 发短信/记录日志/异步处理（与下单逻辑解耦）
    }

    // ⚠️ 异步监听（事务提交后 + 独立线程）
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void asyncHandle(OrderCreatedEvent event) { }
}
```

### 2.2 消息队列集成（RabbitMQ）

```java
// ① 发送
@Service
public class OrderProducer {
    private final RabbitTemplate rabbit;

    public void sendOrder(OrderDTO order) {
        rabbit.convertAndSend("order.exchange", "order.created", order);
    }
}

// ② 接收
@Component
public class OrderConsumer {
    @RabbitListener(queues = "order.created.queue")
    public void handle(OrderDTO order) {
        // 消费处理
    }
}
```

> 🎯 **要点**：事件驱动两层——应用内事件（@EventListener 解耦模块）、消息队列（@RabbitListener 跨服务异步）。**事务后事件**（@TransactionalEventListener）保证数据一致性。

---

## 3. Boot 4 新特性回顾

| 特性 | 说明 | 面试表达 |
|------|------|---------|
| 47 模块拆分 | 自动配置模块化 | 启动快 33%、镜像小 19% |
| Jakarta EE 11 | Servlet 6.1/Tomcat 11 | javax 彻底退出 |
| 虚拟线程 | Java 21/25 | 万级并发、阻塞不占资源 |
| @HttpServiceClient | 声明式 HTTP | 替代 Feign/RestTemplate |
| 内置弹性 | @Retryable/@ConcurrencyLimit | 替代 Resilience4j |
| OTel Starter | 链路追踪 | 一个依赖搞定 |
| Jackson 3 | tools.jackson 包 | 2 标记弃用 |
| gRPC 自动配置 | Boot 4.1 | @GrpcAdvice 统一异常 |
| SSRF 防护 | InetAddressFilter | 4.1 出站请求过滤 |

```
⚠️ 面试必答：
"Boot 4 核心四变化——模块化（快）、
 Jakarta EE 11、虚拟线程、官方声明式
 HTTP 客户端；升级用 Migrator 工具。"
```

---

## 4. Spring Boot 面试题精选

**Q1: 自动配置原理？**
```
@EnableAutoConfiguration 导入 AutoConfigurationImportSelector
→ 读取 AutoConfiguration.imports → 条件装配（@ConditionalOn*）
→ 满足才注册 Bean。详见 01 篇。
```

**Q2: 为什么引入 starter 就能用？**
```
Starter = 依赖 + 自动配置打包——
依赖引入后自动配置类条件满足即装配；
版本由 Boot BOM 统一管理。
```

**Q3: Spring Boot 和 Spring 的关系？**
```
Boot 是 Spring 的"开箱即用"封装——
自动配置 + Starter + 内嵌服务器；
底层还是 Spring Framework（IoC/AOP）。
```

**Q4: 配置文件优先级？**
```
命令行 > Java 系统属性 > 环境变量 >
application-{profile}.yml > application.yml；
部署用命令行/环境变量覆盖（配置外置）。
```

**Q5: 事务失效场景？**
```
同类内部调用（this 不走代理）、非 public、
异常被吞、异常类型不匹配（默认只回滚运行时）、
自注入。同类调用是最高频。
```

**Q6: @RestControllerAdvice 作用？**
```
全局异常处理——业务异常/校验异常/兜底异常
统一响应格式；异常不暴露内部细节。
```

**Q7: JWT 认证流程？**
```
登录签发 JWT（AuthenticationManager 认证）→
JWT 过滤器解析 Bearer 令牌 → 校验后放入
SecurityContext → 授权过滤判断权限。
无状态（STATELESS）+ 白名单最小化。
```

**Q8: 怎么保证接口幂等？**
```
① 幂等键（请求头 Idempotency-Key + Redis 去重）
② 数据库唯一约束
③ 状态机校验（订单状态流转）
④ 乐观锁（版本号）
```

**Q9: 项目启动慢怎么排查？**
```
① 依赖注入耗时（懒加载 @Lazy）
② 连接池初始化（Boot 4.1 延迟连接）
③ 大 Entity 扫描（JPA 异步初始化）
④ 第三方 SDK 初始化（懒初始化）
⑤ Actuator 指标 + 启动耗时分析
```

**Q10: Spring Boot 4 相比 3 的变化？**
```
模块化拆分（47 模块、启动快 33%）、
Jakarta EE 11（javax 退出）、虚拟线程、
@HttpServiceClient、内置弹性注解、
OTel 官方 Starter。升级需 Migrator 工具。
```

**Q11: 怎么实现读写分离？**
```
dynamic-datasource 库 @DS("slave") 注解切换，
或自定义 AbstractRoutingDataSource；
事务内强制主库（避免读到延迟数据）。
```

**Q12: 优雅关闭怎么配置？**
```
server.shutdown=graceful +
lifecycle.timeout-per-shutdown-phase=30s；
流程：拒新请求 → 等存量 → 释放资源 → 退出；
K8s 配合 readiness 探针实现零中断。
```

---

> 🎯 **核心要点**：微服务进阶 = **组件接入**（Nacos 注册配置 + 网关 + 熔断）+ **事件驱动**（@EventListener 解耦 + MQ 异步）+ **Boot 4 新特性**（模块化/虚拟线程/@HttpServiceClient）+ **面试十二问**（自动配置/事务失效/幂等/优雅关闭是最高频）。Spring Boot 面试主线：原理（自动配置）→ 实践（事务/安全）→ 生产（优雅关闭/监控）→ 新特性（Boot 4）。

---

**返回总览**：[00-SpringBoot总览与核心概念](00-SpringBoot总览与核心概念.md) | **上一篇**：[07-SpringBoot生产运维与可观测](07-SpringBoot生产运维与可观测.md)
