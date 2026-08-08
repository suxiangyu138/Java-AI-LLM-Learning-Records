# 06-Kafka Binder 深入
> Kafka Binder 全维度剖析：Topic/分区/Offset 映射、消费组语义、批量、事务、响应式 Binder（Reactor Kafka）、Kafka Streams Binder（5.0 新控制项）

## 📚 目录
1. [Kafka Binder 架构](#1-kafka-binder-架构)
2. [Topic 与分区映射](#2-topic-与分区映射)
3. [消费组与 Offset 语义](#3-消费组与-offset-语义)
4. [生产者配置（Kafka 专属）](#4-生产者配置kafka-专属)
5. [消费者配置（Kafka 专属）](#5-消费者配置kafka-专属)
6. [批量消费与批处理](#6-批量消费与批处理)
7. [Kafka 事务支持](#7-kafka-事务支持)
8. [响应式 Kafka Binder](#8-响应式-kafka-binder)
9. [Kafka Streams Binder（流处理）](#9-kafka-streams-binder流处理)
10. [核心要点](#10-核心要点)
11. [参考来源](#11-参考来源)

## 1. Kafka Binder 架构

```text
Spring Cloud Stream 应用
  └─ KafkaMessageChannelBinder（核心）
       ├─ 生产者: KafkaProducer / KafkaTemplate → Topic（分区）
       ├─ 消费者: ConcurrentMessageListenerContainer（spring-kafka 容器）
       │     └─ ErrorHandler / RetryTemplate / DLQ 处理
       ├─ 事务: ChainedKafkaTransactionManager / KafkaAwareTransactionManager
       └─ 配置: KafkaBinderConfigurationProperties（spring.cloud.stream.kafka.binder.*）
```

底层依赖 spring-kafka（Container、Listener）与 kafka-clients，但 Binder 层把"容器管理、重试、DLQ、类型转换"统一接管，业务代码不直接接触 `ConsumerRecord`。

## 2. Topic 与分区映射

### 2.1 命名与创建

| 项 | 行为 |
|----|------|
| Destination → Topic | 默认 1:1（`orders` → Topic `orders`） |
| `binder.prefix` | Topic 前缀（如 `prod.` → `prod.orders`） |
| `binder.auto-create-topics` | 默认 true，自动创建 Topic |
| 分区数 | producer `partition-count`（默认 3） |
| `binder.replication-factor` | 副本数（生产 ≥ 3；本地 1） |

> ⚠️ 生产环境建议关闭 `auto-create-topics`，由 DBA/平台统一建 Topic（含权限、副本、清理策略），否则容量与权限难以管控。

### 2.2 分区键与分区选择

| 环节 | 机制 |
|------|------|
| 分区键提取 | `producer.partition-key-expression`（SpEL）或自定义 `PartitionKeyExtractorStrategy` |
| 分区选择 | 默认 `key.hashCode % partition-count`；自定义 `PartitionSelectorStrategy` |
| 分区一致性 | 输出绑定与输入绑定同 key 时自动映射到同一分区（partitioned=true 场景） |

```yaml
spring:
  cloud:
    stream:
      bindings:
        order-out-0:
          destination: order-events
          producer:
            partition-key-expression: payload.orderId
            partition-count: 12
        order-in-0:
          destination: order-events
          group: order-service
          consumer:
            partitioned: true
```

> 💡 同一条订单的创建/支付/取消事件用 `orderId` 做分区键，保证该订单的全生命周期事件进同一分区，消费端按分区串行处理——这是事件溯源类场景的标准姿势。

## 3. 消费组与 Offset 语义

### 3.1 group → group.id

| 场景 | 行为 |
|------|------|
| 显式 group | `group.id = <group>`；重启从上次提交的 offset 继续消费 |
| 匿名（无 group） | 每次启动生成随机 `group.id`，从 `start-offset` 起消费，易重复消费 |
| 组内多实例 | Kafka 自动分配分区（Range/Sticky 策略），每个分区同一时刻只归一个实例 |

### 3.2 Offset 提交

| 属性 | 默认 | 说明 |
|------|------|------|
| `consumer.auto-commit-offset` | true | 容器自动提交 offset |
| `consumer.start-offset` | latest | 无既有 offset 时的起点：`earliest` / `latest` |
| `consumer.enable-dlq` | false | 启用死信 Topic（**必须显式 group**） |
| `consumer.reset-offsets` | - | 显式重置偏移（`spring.cloud.stream.kafka.bindings.<b>.consumer.reset-offsets`，开发排障用） |

> ⚠️ **至少一次 vs 恰好一次**：默认自动提交 + 处理后提交 = 可能重复消费（处理成功后提交前崩溃）。业务处理必须幂等（幂等键、DB 唯一约束、状态机防重入）。恰好一次需 Kafka 事务 + 读-写同一事务（幂等生产者），Stream 提供事务支持（见第 7 节）。

## 4. 生产者配置（Kafka 专属）

| 属性（`kafka.bindings.<b>.producer.*`） | 说明 |
|------|------|
| `sync` | 同步发送（默认异步） |
| `buffer-size` / `batch-timeout` | 批量缓冲 |
| `compression-type` | 压缩（gzip/snappy/lz4/zstd） |
| `configuration.<kafka-any-property>` | 透传任意 Kafka producer 配置（`acks`、`retries`、`linger.ms`） |
| `required-groups`（公共） | 等消费组就绪再发（防丢消息） |

> ⚠️ **4.x 已知问题**：绑定级 `configuration.*` 的 producer 属性在 4.x 运行时**不生效**（仅 binder 级 `spring.cloud.stream.kafka.binder.configuration.*` 生效，Stack Overflow 实证），5.x 官方确认修复多绑定问题（#3173）——使用前先验证目标版本的生效范围，必要时把通用项提到 binder 级。

## 5. 消费者配置（Kafka 专属）

| 属性（`kafka.bindings.<b>.consumer.*`） | 默认 | 说明 |
|------|------|------|
| `auto-commit-offset` | true | 自动提交 |
| `start-offset` | latest | 无 offset 起点 |
| `enable-dlq` | false | 死信 Topic |
| `dlq-name` | - | 自定义 DLQ Topic 名 |
| `dlq-partitions` | - | DLQ Topic 分区数 |
| `transaction-id-prefix` | - | 开启事务模式（非空即启用） |
| `default-retryable` | false | 未归类异常是否可重试 |
| `retryable-exceptions` | - | 可重试异常白名单 |
| `configuration.*` | - | 透传 kafka consumer 配置（`max.poll.records`、`fetch.min.bytes` 等） |
| `ack-each-record` | - | 逐条 ack（批量场景） |

## 6. 批量消费与批处理

```yaml
spring:
  cloud:
    stream:
      bindings:
        order-in-0:
          destination: order-events
          group: order-service
          consumer:
            batch-mode: true
            max-attempts: 1        # 批量模式下重试语义特殊，见 08
```

```java
@Bean
public Function<List<OrderEvent>, List<OrderEvent>> batchNormalize() {
    return batch -> batch.stream().map(e -> new OrderEvent(e.orderId(), e.status().toUpperCase())).toList();
}
```

| 要点 | 说明 |
|------|------|
| 批大小 | 由 `max.poll.records` 等容器配置控制 |
| 头结构 | 批量头按消息粒度嵌套（`kafka_batchConvertedHeaders`），转换失败时批缩小而头数量不变 |
| 事务联动 | 批量 + 事务时整体提交，单条失败走 `AfterRollbackProcessor` |

> ⚠️ 批量模式 + DLQ 时**无重试**（直接进 DLQ），且失败消息无法单独回滚处理——吞吐优先的场景接受该权衡，正确性优先用单条模式。

## 7. Kafka 事务支持

### 7.1 开启方式

```yaml
spring:
  cloud:
    stream:
      kafka:
        bindings:
          order-in-0:
            consumer:
              transaction-id-prefix: order-tx-
```

### 7.2 语义与行为

| 维度 | 说明 |
|------|------|
| 消费-处理-生产原子性 | 消费 → 业务处理 → 后续生产在同一事务内，提交成功才提交 offset |
| 事务管理器 | 5.0 起 `KafkaMessageChannelBinder` 使用 `setKafkaAwareTransactionManager`（替代已删除的 `setTransactionManager`，issue #3130） |
| 恰好一次 | 与幂等生产者配合实现端到端恰好一次语义 |
| 重试交互 | 事务开启后 `retryable-exceptions` / `default-retryable` **被忽略**——binder 配置 `AfterRollbackProcessor` 仅用 `BackOff` 控制回滚重试 |

> ⚠️ **事务 + 重试陷阱**：事务模式下错误重试由 `AfterRollbackProcessor` 接管，`max-attempts` 等属性表现不同（Stack Overflow #79309828 实证）。先确认目标版本的事务重试语义再配置，别指望两套机制叠加生效。

### 7.3 事务与外部系统

Kafka 事务只覆盖"Kafka 内"（消费 + 生产）。若业务同时写 DB 与 Kafka，需要**事务消息模式**（先写 DB，用 CDC/Outbox 模式发 Kafka，或 ChainedKafkaTransactionManager 同步两套事务——后者需 DB 支持 XA，生产慎用）。

## 8. 响应式 Kafka Binder

| 维度 | 说明 |
|------|------|
| Artifact | `spring-cloud-stream-binder-kafka-reactive`（4.0 引入） |
| 底层 | Reactor Kafka（`KafkaReceiver`/`KafkaSender`），背压原生 |
| 适用 | WebFlux 全链路响应式服务（端到端非阻塞） |
| 编程模型 | 仍是函数式：`Consumer<Flux<...>>` / `Supplier<Flux<...>>` |
| 绑定名 | 与普通 binder 相同（`-in-0`/`-out-0`），配置前缀 `spring.cloud.stream.kafka.binder.*` 同样适用 |

```java
@Bean
public Function<Flux<OrderEvent>, Flux<PaymentEvent>> reactiveOrderProcessor() {
    return orders -> orders.flatMap(order -> paymentService.pay(order));
}
```

> ⚠️ **响应式限制（官方实证 issue #2557）**：`StreamBridge` 目前**不是响应式**的（`send` 为同步），响应式 Kafka 与 StreamBridge 混用时需要变通（如通过 sink Flux 桥接）。WebFlux 场景若消息量不大，优先用普通 binder 避免复杂度。

## 9. Kafka Streams Binder（流处理）

用于状态化流处理（窗口聚合、join、KTable）——"Stream 里的 Stream"：

| 维度 | 说明 |
|------|------|
| Artifact | `spring-cloud-stream-binder-kafka-streams` |
| 编程模型 | 函数返回 `KStream`/`KTable` 处理函数（`Function<KStream<...>, KStream<...>>`） |
| 用途 | 实时聚合、事件 join、状态化计算（Streaming SQL 的替代） |
| 5.0 新特性 | `CacheEnabled`、`LoggingEnabled` 属性控制 KTable 物化状态缓存与 changelog 日志（issues #3136/#3094） |

```yaml
spring:
  cloud:
    stream:
      kafka:
        binder:
          brokers: localhost:9092
        bindings:
          count-out-0:
            consumer:
              cache-enabled: true       # KTable 状态缓存（5.0+）
              logging-enabled: false    # changelog 日志（5.0+）
```

> 🎯 **定位辨析**：普通 Kafka Binder = 消息传递（Producer/Consumer 语义）；Kafka Streams Binder = 流计算（状态化、窗口、聚合）。需要"单词计数/会话窗口/事件 join"时选 Streams，否则别引入。

## 10. 核心要点

> 🎯 **核心要点**：
> - Kafka Binder 把 Topic/分区/offset/group.id 全部映射到 Stream 逻辑模型，业务零感知；
> - 有序性 = 分区键（稳定字段）；并行度 = 分区数；幂等 = 业务侧必做（至少一次语义）；
> - 事务 = 消费+生产原子（恰好一次），但与重试配置有冲突，事务模式重试走 AfterRollbackProcessor；
> - 响应式 binder（Reactor Kafka）只服务全链路响应式场景，StreamBridge 非响应式是已知限制；
> - Kafka Streams binder 是流计算而非消息传递，按需引入。

## 11. 参考来源

- [Kafka Binder Reference（Retry/DLQ）](https://docs.spring.io/spring-cloud-stream/reference/4.2/kafka/kafka-binder/retry-dlq.adoc)
- [Dead-Letter Topic Processing（5.0.2-SNAPSHOT 文档）](https://www.spring-doc.cn/spring-cloud-stream/5.0.2-SNAPSHOT/kafka_kafka-binder_dlq.en.html)
- [spring-cloud-stream Releases（5.0.x 变更）](https://github.com/spring-cloud/spring-cloud-stream/releases)
- [Feature Requests: Reactive StreamBridge (#2557)](https://github.com/spring-cloud/spring-cloud-stream/issues/2557)
- [KafkaBinderConfigurationProperties issue (#2801)](https://github.com/spring-cloud/spring-cloud-stream/issues/2801)

---

**下一模块**：[07-RabbitMQ Binder深入](07-RabbitMQ Binder深入.md)　/　**返回总览**：[00-总览](00-Spring%20Cloud%20Stream总览.md)
