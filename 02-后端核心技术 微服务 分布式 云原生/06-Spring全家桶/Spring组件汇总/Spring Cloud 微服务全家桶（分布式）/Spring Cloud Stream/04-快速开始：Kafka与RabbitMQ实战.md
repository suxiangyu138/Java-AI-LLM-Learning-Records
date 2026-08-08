# 04-快速开始：Kafka 与 RabbitMQ 实战
> 两个完整可运行的 Demo：同一套业务代码分别对接 Kafka 与 RabbitMQ，验证"切中间件只改依赖与配置"

## 📚 目录
1. [环境准备](#1-环境准备)
2. [工程骨架（Maven）](#2-工程骨架maven)
3. [业务代码：与中间件无关](#3-业务代码与中间件无关)
4. [Kafka Binder 接入](#4-kafka-binder-接入)
5. [RabbitMQ Binder 接入](#5-rabbitmq-binder-接入)
6. [运行与验证](#6-运行与验证)
7. [切换中间件的最小改动清单](#7-切换中间件的最小改动清单)
8. [常见启动失败排查](#8-常见启动失败排查)
9. [参考来源](#9-参考来源)

## 1. 环境准备

| 依赖 | 版本参考 | 启动方式 |
|------|---------|---------|
| JDK | 17+（Boot 4 建议 21） | - |
| Kafka | 3.9.x 及以上 | Docker 或本地 `kafka-server-start` |
| RabbitMQ | 4.x | Docker 或本地服务 |

```bash
# Kafka（示例：kraft 模式单节点）
docker run -d --name kafka -p 9092:9092 \
  -e KAFKA_PROCESS_ROLES=broker,controller \
  -e KAFKA_NODE_ID=1 \
  -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 \
  apache/kafka:3.9.0

# RabbitMQ
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:4-management
```

## 2. 工程骨架（Maven）

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.1</version>
    <relativePath/>
</parent>

<properties>
    <spring-cloud.version>2025.1.1</spring-cloud.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>   <!-- 需要 REST 入口时 -->
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-stream-binder-kafka</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-stream-binder-rabbit</artifactId>  <!-- 与 kafka 二选一，或同时保留做多 Binder -->
    </dependency>
</dependencies>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

> ⚠️ **版本纪律**：Stream 版本由 Spring Cloud BOM 统一管理，**不要**单独写 `spring-cloud-stream` 的 version；`2025.1.x` 列车自带 Boot 4 兼容基线。

## 3. 业务代码：与中间件无关

```java
// 1) 消息模型
public record OrderEvent(String orderId, String status) {}

// 2) 函数端点
@Configuration
public class OrderFunctions {

    private static final Logger log = LoggerFactory.getLogger(OrderFunctions.class);

    @Bean
    public Supplier<OrderEvent> orderSupplier() {
        return () -> new OrderEvent(UUID.randomUUID().toString(), "CREATED");
    }

    @Bean
    public Consumer<OrderEvent> orderLogger() {
        return event -> log.info("收到订单事件: orderId={}, status={}", event.orderId(), event.status());
    }
}

// 3) REST 注入入口（StreamBridge）
@RestController
public class OrderController {

    private final StreamBridge streamBridge;

    public OrderController(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @PostMapping("/orders")
    public String create(@RequestBody OrderEvent order) {
        streamBridge.send("restOrder-out-0", order);
        return "sent";
    }
}
```

```yaml
# application.yml（公共部分）
spring:
  application:
    name: stream-demo
  cloud:
    function:
      definition: orderSupplier;orderLogger;restOrder
    stream:
      source: restOrder                     # 让 StreamBridge 目标成为显式绑定
      bindings:
        orderSupplier-out-0:
          destination: order-events
        restOrder-out-0:
          destination: order-events         # REST 入口复用同一目的地
        orderLogger-in-0:
          destination: order-events
          group: order-service              # 生产必须显式消费组
```

## 4. Kafka Binder 接入

```yaml
spring:
  cloud:
    stream:
      default-binder: kafka
      kafka:
        binder:
          brokers: localhost:9092           # 连接地址
          auto-create-topics: true          # 自动创建 Topic（默认开启）
          replication-factor: 1             # 单节点环境
        bindings:
          orderLogger-in-0:
            consumer:
              auto-commit-offset: true
              start-offset: latest
```

> 💡 **Kafka 专属行为**：`order-events` Topic 首次消费时由 binder 自动创建（`auto-create-topics=true`）；生产与消费共用 topic，分区数由 producer 侧 `partition-count` 决定（默认 3）。

## 5. RabbitMQ Binder 接入

```yaml
spring:
  cloud:
    stream:
      default-binder: rabbit
      rabbit:
        binder:
          addresses: localhost:5672        # 连接地址
          admin-addresses: localhost:15672 # 管理 API（声明拓扑需要）
        bindings:
          orderLogger-in-0:
            consumer:
              auto-bind-dlq: true          # 自动创建 DLQ（后续章节展开）
```

> 💡 **Rabbit 专属行为**：`order-events` 映射为 **TopicExchange**；`order-service` 消费组自动创建队列 `order-events.order-service` 并绑定（routing key `#`）；匿名消费者创建随机自动删除队列。

## 6. 运行与验证

```bash
mvn spring-boot:run
```

### 6.1 链路观察

```text
[轮询] orderSupplier（每 1s 生产 1 条）
   └─→ order-events（Kafka Topic / Rabbit Exchange）
          └─→ order-service 组内 orderLogger 消费打印
[REST] POST /orders → StreamBridge → order-events
```

| 验证项 | Kafka | RabbitMQ |
|--------|-------|----------|
| Topic/Exchange 是否创建 | `kafka-topics --bootstrap-server localhost:9092 --list` | RabbitMQ 管理台 15672 |
| 队列是否绑定 | - | 管理台 Queues → `order-events.order-service` |
| 消费是否成功 | 控制台日志 `收到订单事件` | 同左 |
| 组内竞争验证 | 起两个实例，消息只在其中一个实例打印 | 同左 |

### 6.2 双实例负载均衡验证

```bash
mvn spring-boot:run -Dserver.port=8081   # 第二个实例
```

- 两条实例 `orderLogger-in-0` 同一 group `order-service` → 消息被两个实例**轮流**消费（Kafka 按分区、Rabbit 按投递轮询）；
- 起第三个实例时，Kafka 分区数（默认 3）会成为并行上限。

## 7. 切换中间件的最小改动清单

| 改动点 | 说明 |
|--------|------|
| 依赖 | kafka binder ↔ rabbit binder（BOM 管理版本） |
| 配置 | `binders`/`binder` 连接参数（brokers ↔ addresses）+ Binder 专属属性 |
| Java 代码 | **零改动**（函数与业务逻辑不变） |
| 物理资源 | 原中间件的 Topic/Queue 数据不会自动迁移，需双写或消费回放过渡 |

> 🎯 **核心要点**：这就是 Stream 的核心价值演示——同样三个函数 + 一份公共配置，只切换 Binder 依赖与连接参数即可换中间件。若你的"切换"还要改 Java 代码，说明写法没走函数式模型。

## 8. 常见启动失败排查

| 现象 | 原因 | 处理 |
|------|------|------|
| `No function defined` / 绑定歧义 | 多函数未配 `spring.cloud.function.definition` | 显式声明 definition |
| `Dispatcher has no subscribers for channel ...` | 升级 3.x→4.x 后旧注解残留 | 删除 `@EnableBinding`/`@StreamListener`，改函数式（issue #2662） |
| `No ConfigurationProperties annotation found on KafkaBinderConfigurationProperties` | 4.x 发布制品缺注解（issue #2801） | 显式声明配置 Bean 或升级修复版本 |
| Kafka 连接超时 | brokers 配置错误 / 网络不通 | 检查 `spring.cloud.stream.kafka.binder.brokers` |
| Rabbit 拓扑未创建 | 缺 `admin-addresses` 或连接用户权限不足 | 配置管理地址与有声明权限的账号 |
| 消息不进 consumer | 匿名组（未配 group）从 latest 起消费 | 配 group 或用 `start-offset: earliest` |

## 9. 参考来源

- [Producing and Consuming Messages](https://docs.spring.io/spring-cloud-stream/reference/spring-cloud-stream/producing-and-consuming-messages.html)
- [RabbitMQ Binder Reference](https://docs.spring.io/spring-cloud-stream/reference/4.3/rabbit/rabbit_overview.html)
- [Kafka Binder Reference](https://docs.spring.io/spring-cloud-stream/reference/4.2/kafka/kafka-binder/retry-dlq.adoc)
- [spring-cloud-stream issue #2662（3.2.4→4.0.1 消息不消费）](https://github.com/spring-cloud/spring-cloud-stream/issues/2662)

---

**下一模块**：[05-绑定配置属性全解](05-绑定配置属性全解.md)　/　**返回总览**：[00-总览](00-Spring%20Cloud%20Stream总览.md)
