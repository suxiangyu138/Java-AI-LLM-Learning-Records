# 07-RabbitMQ Binder 深入
> RabbitMQ Binder 全维度剖析：Exchange/Queue 拓扑生成规则、生产者与消费者专属属性、重试与死信、延迟消息、批量发送

## 📚 目录
1. [RabbitMQ Binder 架构](#1-rabbitmq-binder-架构)
2. [Exchange / Queue 拓扑规则](#2-exchange--queue-拓扑规则)
3. [消费者专属属性](#3-消费者专属属性)
4. [生产者专属属性](#4-生产者专属属性)
5. [重试与死信机制](#5-重试与死信机制)
6. [延迟消息支持](#6-延迟消息支持)
7. [批量发送与消费](#7-批量发送与消费)
8. [对接已有基础设施](#8-对接已有基础设施)
9. [核心要点](#9-核心要点)
10. [参考来源](#10-参考来源)

## 1. RabbitMQ Binder 架构

```text
Spring Cloud Stream 应用
  └─ RabbitMessageChannelBinder
       ├─ 生产者: RabbitTemplate → Exchange（默认 TopicExchange）→ 队列
       ├─ 消费者: SimpleMessageListenerContainer（spring-rabbit）→ 队列
       │     └─ RetryTemplate / 死信（autoBindDlq）处理
       ├─ 拓扑声明: RabbitAdmin（连接管理地址自动声明 Exchange/Queue/Binding）
       └─ 配置: RabbitBinderConfigurationProperties（spring.cloud.stream.rabbit.binder.*）
```

与 Kafka Binder 的最大差异：Rabbit 是"推模型 + 队列中间态"，每条消费组对应**独立队列**；死信处理优先用 Rabbit 原生 DLX/DLQ 机制。

## 2. Exchange / Queue 拓扑规则

### 2.1 默认拓扑

| 资源 | 命名规则 | 说明 |
|------|---------|------|
| Exchange | `<destination>`（默认 **TopicExchange**） | 生产者投递到 Exchange |
| Queue（组） | `<prefix><destination>.<group>` | 每个消费组一条专属队列 |
| Binding | 非分区：routing key `#`（匹配全部） | 组队列绑定到 Exchange |
| Queue（匿名） | 随机名 + auto-delete | 无 group 时自动创建 |
| Queue（分区） | `<destination>-<partitionIndex>` | 分区消费时按分区建队列，routing key = 分区号 |

```text
        ┌───────────────── order-events (TopicExchange) ─────────────────┐
        │                                                                │
  routing key "#"                                              routing key "0"/"1"/"2"
        ▼                                                                ▼
order-events.order-service (组队列)                    order-events-0/1/2 (分区队列)
```

### 2.2 消费者交换器/队列配置（`rabbit.bindings.<b>.consumer.*`）

| 属性 | 默认 | 说明 |
|------|------|------|
| `exchange-type` | topic | `direct`/`fanout`/`topic`/`headers`（分区场景仅 direct/topic） |
| `binding-routing-key` | `#`（非分区） | 绑定队列到 Exchange 的 routing key（可多个） |
| `declare-exchange` | true | 是否声明 Exchange（false 用已有资源） |
| `bind-queue` | true | 是否绑定队列（false 用已有资源） |
| `exchange-durable` | true | Exchange 持久化 |
| `exchange-auto-delete` | false | Exchange 自动删除 |
| `prefix` | - | Exchange/Queue 名前缀 |
| `queue-name-group-only` | false | 队列名直接用组名（自定义基建场景） |
| `delayed-exchange` | false | 声明为延迟 Exchange（需 rabbitmq_delayed_message_exchange 插件） |

## 3. 消费者专属属性

| 属性 | 默认 | 说明 |
|------|------|------|
| `concurrency`（公共） | 1 | 并发消费者数（容器 concurrentConsumers） |
| `max-concurrency` | - | 最大并发（配合缩容） |
| `auto-bind-dlq` | false | 自动创建 DLQ + DLX + 绑定 |
| `dead-letter-queue-name` | - | 自定义 DLQ 名（默认 `<queue>.dlq`） |
| `republish-to-dlq` | false | 失败消息"发布"到 DLQ（带异常头）而非直接拒绝 |
| `dlq-ttl` | - | DLQ 消息 TTL（重试回退间隔，配 `dlq-dead-letter-exchange` 实现"回退重试"） |
| `dlq-dead-letter-exchange` | - | DLQ 的死信 Exchange（默认 exchange 名，实现回原队列） |
| `dlq-lazy` / `dlq-max-length` / `dlq-max-length-bytes` / `dlq-max-priority` / `dlq-expires` | - | DLQ 队列参数透传 |
| `dlq-quorum.enabled` | false | DLQ 用仲裁队列（生产推荐） |
| `requeue-rejected` | false | 拒绝消息是否重回队列（true + 无 DLQ = 无限重投） |
| `transacted` | - | 使用 Rabbit 本地事务（性能低于 confirm 模式） |
| `max-attempts`（公共） | 3 | binder 级重试次数 |

> ⚠️ **requeue-rejected 是"无限循环"开关**：默认 false（失败进 DLQ）；设 true 且未配 DLQ 时，失败消息会被无限重投——仅适合瞬时故障场景，且必须配合 `ImmediateAcknowledgeAmqpException` 主动放弃。

## 4. 生产者专属属性

| 属性 | 默认 | 说明 |
|------|------|------|
| `routing-key-expression` | - | routing key 的 SpEL（如 `payload.type`） |
| `batching-enabled` | false | 批量发送 |
| `batch-size` | 100 | 批量条数阈值 |
| `batch-buffer-limit` | 10000 | 缓冲字节上限 |
| `batch-timeout` | 5000ms | 批量超时 |
| `compress` | false | 压缩（gzip） |
| `delivery-mode` | PERSISTENT | 持久化/非持久化投递 |
| `delay` | - | 延迟投递（毫秒，需延迟插件） |
| `use-native-encoding` | false | 原生序列化 |
| `required-groups`（公共） | - | 等组队列绑定完成再发（防丢） |

```yaml
spring:
  cloud:
    stream:
      rabbit:
        bindings:
          order-out-0:
            producer:
              routing-key-expression: payload.type
              batching-enabled: true
              batch-size: 500
              batch-timeout: 3000
```

## 5. 重试与死信机制

### 5.1 默认行为链

```text
消费失败
  ├─ max-attempts > 1 → binder 级 RetryTemplate 重试（退避：backOffInitialInterval × multiplier）
  │       └─ 耗尽 → 死信处理
  └─ max-attempts = 1 → 直接走死信处理
        ├─ auto-bind-dlq=true → 拒绝投递到 DLQ（<queue>.dlq）
        └─ republish-to-dlq=true → 发布到 DLQ（携带 x-exception-stacktrace 等头）
```

### 5.2 两种死信模式对比

| 模式 | 机制 | 异常信息 | 适用 |
|------|------|---------|------|
| 原生拒绝（auto-bind-dlq） | basicNack 进 DLQ（DLX 路由） | 无异常细节，仅 `x-death` 计数 | 简单的隔离即可 |
| 重新发布（republish-to-dlq） | 重新发布到 DLQ | `x-exception-stacktrace` / `x-original-exchange` / `x-original-routing-key` | 需要排查现场、后续回放 |

### 5.3 DLQ + TTL 回退重试（经典可靠重试模式）

```text
主队列 ←── 默认 Exchange 路由（routing key = 队列名） ←── DLQ（TTL 到期）
主队列 --失败--> DLQ --等待 dlq-ttl--> 回到主队列重试
```

```yaml
spring:
  cloud:
    stream:
      rabbit:
        bindings:
          order-in-0:
            consumer:
              auto-bind-dlq: true
              dlq-ttl: 5000            # 等 5 秒再回主队列
              dlq-dead-letter-exchange: ""   # 默认 exchange → 按队列名路由回主队列
```

| 控制点 | 用法 |
|--------|------|
| 强制进死信 | 抛 `AmqpRejectAndDontRequeueException`（或 `requeueRejected=false` 后抛任意异常） |
| 放弃/确认丢弃 | 抛 `ImmediateAcknowledgeAmqpException` |
| 重试次数跟踪 | 读取 `x-death` 头计数，达到上限后转停车场队列 |

> ⚠️ **背压警告**：binder 级重试（RetryTemplate）在退避期间**挂起监听线程**——重试队列长、单条处理慢时阻塞后续消息。高吞吐场景把重试交给 DLQ+TTL 模式（Rabbit 原生能力，不占线程）。

### 5.4 DLQ 消费与回放

框架不提供 DLQ 消息的标准消费机制。常见做法：

| 方案 | 说明 |
|------|------|
| `@RabbitListener` 直连 DLQ 队列 | 解析 `x-exception-stacktrace` 等头，重新投递或告警 |
| 回放应用 | 独立 Stream 应用消费 DLQ → 重发主队列（`x-retries` 自定义头计数） |
| 停车场队列 | 重试达上限后投递 `parkingLot` 队列人工处理 |

## 6. 延迟消息支持

| 方式 | 前提 | 说明 |
|------|------|------|
| 延迟插件 | `rabbitmq_delayed_message_exchange` 插件 | `delayed-exchange: true` + producer `delay: 毫秒` |
| DLQ + TTL 回退 | 无需插件 | 第 5.3 节模式，天然支持"重试退避" |
| 混合 | - | 延迟 Exchange 只做"一次性延迟投递"，重试用 DLQ+TTL |

> 💡 场景选择：订单超时取消（一次性延迟）→ 延迟 Exchange；消费失败重试（多次回退）→ DLQ + TTL。前者延迟精准可重排，后者天然带失败信息。

## 7. 批量发送与消费

| 端 | 配置 | 说明 |
|----|------|------|
| 生产 | `producer.batching-enabled: true` + batch-size/batch-timeout | 积攒到阈值或超时后整批发送（吞吐优化） |
| 消费 | `consumer.batch-mode: true`（公共） | 整批传入 `Function<List<T>, ...>`，头为 `amqp_batchedHeaders` |

> ⚠️ 批量 + 死信交互：Rabbit 批量消费单条失败时整个批次行为取决于容器错误处理；与 Kafka 一样建议批量模式 + `max-attempts: 1` 简化语义，保证走 DLQ 兜底。

## 8. 对接已有基础设施

```yaml
spring:
  cloud:
    stream:
      bindings:
        order-in-0:
          destination: my-exchange          # 已存在的 Exchange
          group: my-queue                   # 已存在的队列
      rabbit:
        bindings:
          order-in-0:
            consumer:
              bind-queue: false             # 不重新绑定
              declare-exchange: false       # 不声明 Exchange
              queue-name-group-only: true   # 队列名 = 组名
```

| 场景 | 关闭项 |
|------|--------|
| 复用已有 Exchange/Queue | `declare-exchange: false` + `bind-queue: false` |
| 跨应用共享交换器 | 只关 `declare-exchange`，保留绑定 |
| 自定义队列名 | `queue-name-group-only: true` |

> ⚠️ **多 Rabbit Binder 冲突**：同一应用配置多个 rabbit binder（连接不同集群）时，需禁用 `RabbitAutoConfiguration`，否则自动配置的 ConnectionFactory 会与 binder 自定义的打架。

## 9. 核心要点

> 🎯 **核心要点**：
> - 拓扑记忆：Exchange = destination；组队列 = `<dest>.<group>`；默认 topic 交换 + `#` 路由；
> - 死信三件套：`auto-bind-dlq`（创建）、`republish-to-dlq`（带现场信息）、`dlq-ttl` + `dlq-dead-letter-exchange`（回退重试）；
> - 重试两条路：binder RetryTemplate（挂线程、适合低频）vs DLQ+TTL（不占线程、适合高频/长退避）；
> - `requeue-rejected: true` 无 DLQ = 无限重投，生产禁止；
> - 延迟投递用插件，失败重试用 DLQ+TTL——两者别混。

## 10. 参考来源

- [RabbitMQ Binder Reference Guide（4.3）](https://docs.spring.io/spring-cloud-stream/reference/4.3/rabbit/rabbit_overview.html)
- [Retry With the RabbitMQ Binder（4.1）](https://docs.spring.io/spring-cloud-stream/reference/4.1/rabbit/rabbit_overview/rabbitmq-retry.html)
- [Spring Cloud Stream RabbitMQ Binder（4.0.x 文档）](https://docs.spring.io/spring-cloud-stream/docs/4.0.x/reference/html/spring-cloud-stream-binder-rabbit.html)
- [spring-cloud-stream GitHub（Retry/DLQ 变更记录）](https://github.com/spring-cloud/spring-cloud-stream)

---

**下一模块**：[08-消息可靠性与重试死信](08-消息可靠性与重试死信.md)　/　**返回总览**：[00-总览](00-Spring%20Cloud%20Stream总览.md)
