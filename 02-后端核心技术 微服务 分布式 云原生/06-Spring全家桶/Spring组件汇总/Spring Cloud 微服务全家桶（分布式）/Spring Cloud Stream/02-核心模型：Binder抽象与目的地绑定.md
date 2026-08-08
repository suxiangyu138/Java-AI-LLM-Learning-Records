# 02-核心模型：Binder 抽象与目的地绑定
> 拆解 Spring Cloud Stream 五大核心概念：Destination、Binding、Group、Partition、Binder SPI，理解"一套代码对接任意消息中间件"的抽象设计

## 📚 目录
1. [设计哲学：为什么需要 Stream](#1-设计哲学为什么需要-stream)
2. [Binder 抽象（核心抽象层）](#2-binder-抽象核心抽象层)
3. [Destination 目的地](#3-destination-目的地)
4. [Binding 绑定](#4-binding-绑定)
5. [Group 消费组](#5-group-消费组)
6. [Partition 分区](#6-partition-分区)
7. [消息通道与消息模型](#7-消息通道与消息模型)
8. [多 Binder 配置](#8-多-binder-配置)
9. [核心要点](#9-核心要点)
10. [参考来源](#10-参考来源)

## 1. 设计哲学：为什么需要 Stream

微服务架构中消息传递无处不在，但每类中间件的 API、概念、行为都不同：

| 能力 | Kafka | RabbitMQ | Kinesis | 直连代码差异 |
|------|-------|----------|---------|-------------|
| 消息单位 | Topic + Partition + Offset | Exchange + Queue + Routing Key | Stream + Shard + Seq | API 完全不同 |
| 消费模型 | Consumer Group 拉取 | Queue 推送（basic.consume） | Consumer 拉取 + Checkpoint | 语义不同 |
| 顺序保证 | 分区内有序 | 单队列内有序 | 分片内有序 | 位置不同 |
| 消息确认 | offset commit | ACK/NACK | checkpoint | 概念不同 |

Spring Cloud Stream 的解法：**Binder 抽象层**。业务代码只面向 `Destination`（逻辑名）与 `Binding`（连接），Binder 负责把逻辑模型翻译成各中间件的物理资源并处理差异。业务代码因此与中间件解耦，切换中间件只改依赖与配置，不改 Java 代码。

> 🎯 **核心要点**：Stream 的价值不是"又一个消息库"，而是**中间件无关的消息编程模型**——绑定层（Binder）把底层差异全部吸收，业务侧永远只看到逻辑概念。

## 2. Binder 抽象（核心抽象层）

### 2.1 职责

| 职责 | 说明 | 实现示例（Kafka Binder） |
|------|------|--------------------------|
| 资源映射 | Destination 逻辑名 → 物理资源 | `orders` → Topic `orders`；`orders.orders-group` 队列 |
| 生产 | 将 `Message` 写入中间件 | `KafkaTemplate`/producer 发送到 Topic |
| 消费 | 从中间件拉取/接收消息转为 `Message` | `ConcurrentMessageListenerContainer` |
| 分组 | 消费组 → 中间件原生群组语义 | group → `group.id` |
| 分区 | 分区键 → 中间件分区 | partition key → Kafka partition |
| 重试/死信 | 统一错误处理策略 | RetryTemplate / DLT |

### 2.2 Binder SPI 与接口

```text
Binder<T, C extends ConsumerProperties, P extends ProducerProperties>
 ├── Bindable 接口: bindConsumer(bindingName, group, interfaceType, consumerProperties)
 │                       bindProducer(bindingName, outboundBindTarget, producerProperties)
 └── 实现类: KafkaMessageChannelBinder / RabbitMessageChannelBinder
```

- `bindConsumer`：把 `MessageChannel`（input）绑定到目标，返回 `Binding`；
- `bindProducer`：把 `MessageChannel`（output）绑定到目标，返回 `Binding`；
- 每个 Binder 实现对应一个配置类（`KafkaBinderConfigurationProperties`、`RabbitBinderConfigurationProperties`），属性前缀 `spring.cloud.stream.kafka.binder.*` / `spring.cloud.stream.rabbit.binder.*`。

> ⚠️ **已知问题（4.x）**：`KafkaBinderConfigurationProperties` 在发布制品中缺失 `@ConfigurationProperties` 注解（3.2.9 有、4.x 无），从 Boot 2 → Boot 3 / SCS 3 → 4 迁移时可能报 `IllegalStateException: No ConfigurationProperties annotation found`（issue #2801）。规避：显式声明配置属性 Bean 或升级到含修复的版本。

### 2.3 Binder 生命周期

```text
应用启动
  → 解析 spring.cloud.function.definition 与 bindings 配置
  → 从 binders.* 配置定位 Binder（默认取 spring.cloud.stream.default-binder）
  → 为每个函数绑定生成 Binding（input/output）
  → Binder 校验/创建物理资源（Topic/Queue/Exchange）
  → 启动生产/消费通道
```

## 3. Destination 目的地

- **定义**：消息的逻辑目的地名称，业务代码和配置中的引用名。
- **映射**：Kafka Binder 默认 1:1 映射为 Topic 名（可加 `prefix`）；RabbitMQ Binder 映射为 **TopicExchange**（队列另由 group 决定）。
- **命名约定**：全小写、连字符分隔（如 `orders-input`、`payment-events`）；同一名字在 Kafka 下是 Topic、在 Rabbit 下是 Exchange——**这正是切换中间件只需改配置的原因**。

| 配置 | 示例 | 说明 |
|------|------|------|
| `bindings.<b>.destination` | `orders` | 逻辑目的地名（必配才能连接物理资源） |
| `bindings.<b>.group` | `order-service` | 消费组名（Kafka 下参与 offset 语义） |

> 💡 一个 input 绑定没配 `group` 时是"匿名消费者"：Kafka 下每次重启生成新 `group.id`（从上次提交点重新消费，容易重复）；Rabbit 下创建随机自动删除队列。生产环境**务必配置 group**（且匿名组无法启用 Kafka DLQ，见 [08-消息可靠性与重试死信](08-消息可靠性与重试死信.md)）。

## 4. Binding 绑定

Binding 是"应用 ←→ 目的地"的连接，分两种方向：

| 方向 | 生成来源 | 中间层 | 生命周期 |
|------|---------|--------|---------|
| input（消费） | `Consumer`/`Function` 的 `-in-0` | `SubscribableChannel` | 随应用启停 |
| output（生产） | `Supplier`/`Function` 的 `-out-0` | `MessageChannel` | 随应用启停 |

每个 Binding 有独立的配置对象（`BindingProperties`），包含：

| 属性组 | 关键项 | 作用 |
|--------|--------|------|
| 通用 | `destination`、`group`、`content-type`、`binder` | 连哪、分组、内容类型、用哪个 Binder |
| 消费侧 | `consumer.*`（见 [05](05-绑定配置属性全解.md)） | concurrency、重试、批量、offset |
| 生产侧 | `producer.*` | 分区键、批量、重试、errorChannel |
| Binder 专属 | `spring.cloud.stream.kafka.bindings.<b>.*` | 各 Binder 特有属性 |

## 5. Group 消费组

### 5.1 语义

- 同一 Destination + 同一 Group 的多个实例，构成一个消费组；
- 组内竞争：每条消息只被组内一个实例消费（Kafka 按分区分配、Rabbit 轮询投递）；
- 组间广播：不同组的实例都能收到同一份消息（Kafka 组间独立消费、Rabbit 组各自队列）。

### 5.2 与中间件原生的对应

| 概念 | Kafka | RabbitMQ |
|------|-------|----------|
| group → 原生 | `group.id` | 每个 group 一个专属 Queue 绑到 Exchange |
| 组内均衡 | 分区分配（RangeAssignor/Sticky） | 队列投递轮询（prefetch 相关） |
| 组内扩容 | 增加实例 → 重新分配分区 | 增加实例 → 同一队列多消费者 |

> 🎯 **核心要点**：group 是"负载均衡 + 持久化消费位点"的最小单元。Kafka 下 group 决定 offset 归属；Rabbit 下 group 决定队列归属（`<destination>.<group>`）。水平扩展、故障恢复都围绕 group 进行。

## 6. Partition 分区

### 6.1 为什么需要分区

| 需求 | 无分区 | 有分区 |
|------|--------|--------|
| 同 key 消息有序 | 无法保证 | 同 key 进同分区，分区内有序 |
| 水平扩展 | 消费组内竞争即可扩展 | 分区是并行度上限，配合 key 路由扩展 |
| 消息处理幂等 | 依赖全局去重 | 分区内天然按 key 隔离 |

### 6.2 分区配置

```yaml
spring:
  cloud:
    stream:
      bindings:
        orders-out-0:
          destination: orders
          producer:
            partition-key-expression: payload.orderId   # 分区键 SpEL
            partition-count: 6                          # 分区数
        orders-in-0:
          destination: orders
          group: order-service
          consumer:
            partitioned: true                           # 声明消费端感知分区
```

| 属性 | 默认 | 说明 |
|------|------|------|
| `producer.partition-key-expression` | `-` | 分区键 SpEL（`headers['k']`、`payload.x`） |
| `producer.partition-key-extractor-name` | `-` | 自定义 `PartitionKeyExtractorStrategy` Bean 名 |
| `producer.partition-count` | 3 | 分区数量 |
| `producer.partition-selector-name` | `-` | 自定义 `PartitionSelectorStrategy` Bean 名（默认 key.hashCode % count） |
| `consumer.partitioned` | false | 输入绑定声明感知分区（Rabbit 下必须 true 才按分区路由） |

### 6.3 分区映射

- **Kafka**：分区键 → Kafka 分区号（binder 自动建 topic 时按 `partition-count` 建分区）；消费组内一个实例负责若干分区；
- **RabbitMQ**：分区生产时以分区号为 routing key 投递；分区消费时每个分区一个队列（`<dest>-<partitionIndex>`），实例间按队列分配，实现与 Kafka 分区等价的效果。

> ⚠️ 分区键值变化会导致消息进不同分区，破坏有序性——业务上应使用稳定字段（订单号、用户 ID），不要用时间戳等易变值。

## 7. 消息通道与消息模型

### 7.1 消息模型

Stream 全程使用 Spring 的 `Message<T>` 抽象：

| 部分 | 说明 |
|------|------|
| payload | 业务负载（`byte[]`/POJO/JSON/文本） |
| headers | 元信息：`contentType`、`partitionKey`、`target-protocol` 等；DLQ 场景追加 `x-exception-*` 头 |

### 7.2 通道类型

| 类型 | 方向 | 说明 |
|------|------|------|
| `SubscribableChannel` | input | 可订阅消费（函数式模型由框架内部订阅） |
| `MessageChannel` | output | 可发送生产（`StreamBridge` 可编程发送） |

> ⚠️ **兼容警告**：4.0 起注解模型删除，`@Input`/`@Output` 注解、`Processor`/`Source`/`Sink` 接口（`@EnableBinding` 时代的产物）均已不存在；网上大量旧教程（"`@StreamListener` + `@Input` 通道"）在 4.x/5.x 下**直接编译失败**，请认准函数式模型（见 [03-函数式编程模型与绑定生成](03-函数式编程模型与绑定生成.md)）。

## 8. 多 Binder 配置

一套应用可同时对接多种中间件（如订单事件进 Kafka、审计日志进 Rabbit）：

```yaml
spring:
  cloud:
    stream:
      default-binder: kafka                 # 全局默认
      binders:
        kafka:
          type: kafka
          environment:
            spring.cloud.stream.kafka.binder.brokers: localhost:9092
        rabbit:
          type: rabbit
          environment:
            spring.cloud.stream.rabbit.binder.addresses: localhost:5672
      bindings:
        orders-in-0:
          destination: orders
          binder: kafka                     # 按绑定指定 Binder
        audit-out-0:
          destination: audit
          binder: rabbit
```

| 配置项 | 作用 |
|--------|------|
| `spring.cloud.stream.binders.<name>.type` | Binder 类型（kafka/rabbit/kinesis/...） |
| `spring.cloud.stream.binders.<name>.environment` | 该 Binder 专属环境变量（可覆盖全局配置） |
| `spring.cloud.stream.default-binder` | 未指定 binder 的绑定使用此默认 |
| `bindings.<b>.binder` | 单绑定指定 Binder |

> ⚠️ **多 Binder 踩坑**：同一应用中存在多个 binder 时，`ListenerContainerWithDlqAndRetryCustomizer` 这类自定义 Bean 会被 `DefaultBinderFactory` 覆盖，需改用 `BinderCustomizer` 在 `KafkaMessageChannelBinder` 上设置容器自定义器（详见 [08-消息可靠性与重试死信](08-消息可靠性与重试死信.md)）。

## 9. 核心要点

> 🎯 五概念一句话记忆：
> - **Destination** = 逻辑目的地名（物理资源的抽象入口）；
> - **Binding** = 应用与目的地之间的连接管道（input/output 双向）；
> - **Group** = 竞争消费单元（负载均衡 + 位点/队列归属）；
> - **Partition** = 有序性与并行度单元（key 路由）；
> - **Binder** = 把上述逻辑翻译成中间件物理行为的 SPI 插件。
>
> 业务代码只写函数 + 配置，不碰任何中间件原生 API——这就是 Stream 与"直接封装 KafkaTemplate"的本质区别。

## 10. 参考来源

- [Spring Cloud Stream Reference：Core concepts](https://docs.spring.io/spring-cloud-stream/reference/)
- [Producing and Consuming Messages](https://docs.spring.io/spring-cloud-stream/reference/spring-cloud-stream/producing-and-consuming-messages.html)
- [RabbitMQ Binder Reference（拓扑与属性）](https://docs.spring.io/spring-cloud-stream/reference/4.3/rabbit/rabbit_overview.html)
- [spring-cloud-stream GitHub issue #2801（KafkaBinderConfigurationProperties）](https://github.com/spring-cloud/spring-cloud-stream/issues/2801)

---

**下一模块**：[03-函数式编程模型与绑定生成](03-函数式编程模型与绑定生成.md)　/　**返回总览**：[00-总览](00-Spring%20Cloud%20Stream总览.md)
