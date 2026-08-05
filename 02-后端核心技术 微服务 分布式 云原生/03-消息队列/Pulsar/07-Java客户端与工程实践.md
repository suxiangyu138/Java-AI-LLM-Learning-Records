# Java客户端与工程实践
> 官方客户端 API、生产/消费模式、事务与批量、Spring 集成、工程模式：Java 生态使用 Pulsar 的完整指南。

---

## 📚 目录

1. [官方客户端](#1-官方客户端)
2. [生产者](#2-生产者)
3. [消费者与订阅](#3-消费者与订阅)
4. [事务与批量](#4-事务与批量)
5. [Spring 集成](#5-spring-集成)
6. [工程模式与最佳实践](#6-工程模式与最佳实践)

---

## 1. 官方客户端

### 1.1 依赖

```xml
<!-- Maven：pulsar-client -->
<dependency>
    <groupId>org.apache.pulsar</groupId>
    <artifactId>pulsar-client</artifactId>
    <version>4.2.1</version>
</dependency>
```

```java
// 客户端创建
PulsarClient client = PulsarClient.builder()
    .serviceUrl("pulsar://localhost:6650")     // 二进制协议
    .authentication(AuthenticationFactory.token("jwt-token"))
    .build();

// 使用后关闭
client.close();
```

### 1.2 客户端配置

| 配置 | 说明 |
|------|------|
| serviceUrl | pulsar://（二进制）/ pulsar+ssl:// |
| authentication | JWT/TLS/OAuth2 |
| operationTimeout | 操作超时 |
| ioThreads | IO 线程数 |
| listenerThreads | 监听线程数 |
| enableTransaction | 事务支持（4.x） |

```text
连接方式：
  pulsar://：二进制协议（生产默认）
  http://：管理 API（8080）

多集群（复制场景）：
  serviceUrl 可配置多个（自动故障切换）
  或使用 lookup 服务
```

---

## 2. 生产者

### 2.1 基本使用

```java
// 创建生产者
Producer<String> producer = client.newProducer(Schema.STRING)
    .topic("persistent://orders/us-east/order-events")
    .create();

// 发送（同步）
MessageId msgId = producer.send("订单创建");

// 发送（异步）
CompletableFuture<MessageId> future = producer.sendAsync("订单创建");

// 批量发送（同步/异步）
List<CompletableFuture<MessageId>> futures = new ArrayList<>();
for (String msg : messages) {
    futures.add(producer.sendAsync(msg));
}
CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
```

### 2.2 生产者配置

| 配置 | 作用 |
|------|------|
| Schema | 消息类型（STRING/JSON/Avro） |
| batchEnabled | 批量发送（默认开） |
| batchingMaxMessages | 批量大小（默认 1000） |
| batchingMaxBytes | 批量字节 |
| batchingMaxPublishDelay | 批量窗口（默认 10ms） |
| compressionType | 压缩（LZ4/ZSTD/SNAPPY） |
| sendTimeout | 发送超时 |
| blockIfQueueFull | 队列满阻塞 |

```java
// 生产优化配置
Producer<String> producer = client.newProducer(Schema.STRING)
    .topic("persistent://orders/us-east/order-events")
    .compressionType(CompressionType.LZ4)
    .batchingMaxMessages(500)
    .batchingMaxPublishDelay(5, TimeUnit.MILLISECONDS)
    .blockIfQueueFull(true)
    .create();
```

### 2.3 消息键与顺序

```java
// Key 消息（Key_Shared 订阅的顺序依据）
TypedMessageBuilder<String> builder = producer.newMessage()
    .key(orderId)              // 业务键（订单 ID）
    .value("订单创建");
MessageId msgId = builder.send();

// 顺序保证：同 key → 同消费者（Key_Shared 订阅）
// 生产者侧：同 key 消息建议串行发送（或等 ack）
```

---

## 3. 消费者与订阅

### 3.1 消费者创建

```java
// 创建消费者（Shared 订阅——负载均衡）
Consumer<String> consumer = client.newConsumer(Schema.STRING)
    .topic("persistent://orders/us-east/order-events")
    .subscriptionName("order-processor")
    .subscriptionType(SubscriptionType.Shared)   // 订阅模式（03 模块）
    .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
    .subscribe();

// 消费（同步）
while (true) {
    Message<String> msg = consumer.receive();
    try {
        process(msg.getValue());      // 处理业务
        consumer.acknowledge(msg);    // 确认（推进游标）
    } catch (Exception e) {
        consumer.negativeAcknowledge(msg);  // 失败重投
    }
}
```

### 3.2 订阅模式选择

```java
// Exclusive：单消费者（严格顺序）
.subscriptionType(SubscriptionType.Exclusive)

// Failover：主备（顺序 + HA）
.subscriptionType(SubscriptionType.Failover)

// Shared：负载均衡（高吞吐）
.subscriptionType(SubscriptionType.Shared)

// Key_Shared：按 key 有序 + 并行
.subscriptionType(SubscriptionType.Key_Shared)
```

### 3.3 消费配置与模式

| 配置 | 说明 |
|------|------|
| subscriptionType | 订阅模式（见上） |
| ackTimeout | 未 ack 超时重投 |
| negativeAckRedeliveryDelay | 负 ack 重投延迟 |
| receiverQueueSize | 预取队列（默认 1000） |
| deadLetterPolicy | 死信策略 |
| subscriptionInitialPosition | 起始位置（Earliest/Latest） |

```java
// 完整消费配置（死信 + 重试）
Consumer<String> consumer = client.newConsumer(Schema.STRING)
    .topic("persistent://orders/us-east/order-events")
    .subscriptionName("order-processor")
    .subscriptionType(SubscriptionType.Key_Shared)
    .ackTimeout(30, TimeUnit.SECONDS)
    .negativeAckRedeliveryDelay(5, TimeUnit.SECONDS)
    .deadLetterPolicy(DeadLetterPolicy.builder()
        .maxRedeliverCount(5)                       // 重投 5 次
        .deadLetterTopic("persistent://orders/us-east/dlq")
        .build())
    .subscribe();
```

---

## 4. 事务与批量

### 4.1 事务（Pulsar 事务）

```java
// 事务：多 topic 原子写（或 写+ack 原子）
Transaction txn = client.newTransaction()
    .withTransactionTimeout(30, TimeUnit.SECONDS)
    .build();

try {
    // ① 事务内写多个 topic
    producer1.newMessage(txn).value("A").send();
    producer2.newMessage(txn).value("B").send();
    // ② 事务内确认消费
    consumer.acknowledge(txn, msg);
    txn.commit();                // 提交（原子生效）
} catch (Exception e) {
    txn.abort();                 // 回滚
}
```

```text
Pulsar 事务能力：
  多 topic 原子写入
  写 + ack 原子（消费与生产联动）
  对比 Kafka 事务（跨分区原子）

适用：
  分布式事务（消息侧）——见 消息队列理论 07 模块
  跨 topic 一致性

注意：
  事务需客户端/服务端启用（enableTransaction）
  事务吞吐 < 非事务（有额外开销）
```

### 4.2 批量消费

```java
// 批量消费（receive 批量获取）
Messages<String> messages = consumer.batchReceive();
for (Message<String> msg : messages) {
    process(msg.getValue());
}
consumer.acknowledge(messages);      // 批量确认

// 适用：高吞吐消费（减少往返）
// 注意：批量确认的粒度（部分失败的处理）
```

### 4.3 读写模式总结

```text
生产优化组合：
  批量（batchingMaxMessages）+ 压缩（LZ4/ZSTD）
  异步发送（sendAsync）+ 回调/CompletableFuture
  消息键（Key_Shared 顺序）

消费优化组合：
  订阅模式匹配需求（Shared/Key_Shared）
  预取队列（receiverQueueSize 调优）
  批量接收（batchReceive）
  死信 + 负 ack（失败治理）

一致性组合：
  事务（跨 topic 原子）
  幂等消费（至少一次语义兜底）
```

---

## 5. Spring 集成

### 5.1 依赖与配置

```xml
<!-- Spring Boot Pulsar（官方 starter） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-pulsar</artifactId>
</dependency>
```

```yaml
spring:
  pulsar:
    client:
      service-url: pulsar://localhost:6650
    producer:
      cache-enabled: true
    consumer:
      cache-enabled: true
```

### 5.2 注解模式

```java
// 生产者（注入 PulsarTemplate）
@Service
public class OrderService {
    @Autowired
    private PulsarTemplate<String> pulsarTemplate;

    public void createOrder(String orderJson) {
        pulsarTemplate.send(
            "persistent://orders/us-east/order-events",
            orderJson);
    }
}

// 消费者（@PulsarListener 注解）
@Component
public class OrderConsumer {
    @PulsarListener(
        subscriptionName = "order-processor",
        topics = "persistent://orders/us-east/order-events",
        subscriptionType = SubscriptionType.Shared)
    public void onOrder(String orderJson) {
        // 处理订单消息
    }
}
```

### 5.3 Spring 配置要点

```text
Spring Pulsar（Spring Boot 3.2+）：
  @PulsarListener：注解消费（类似 @KafkaListener）
  PulsarTemplate：模板生产
  自动配置（client/producer/consumer）

配置项：
  订阅模式（注解参数）
  死信/重试（注解参数）
  序列化（Schema）

工程建议：
  简单场景：注解模式（快速）
  复杂场景：手动客户端（精确控制）
  → 两者可混用
```

---

## 6. 工程模式与最佳实践

### 6.1 可靠消费模板

```java
// 可靠消费的完整模式（幂等 + 死信 + 重试）
while (running) {
    Message<String> msg = consumer.receive();
    try {
        // ① 幂等检查（消息 ID/业务键）
        if (isProcessed(msg.getMessageId().toString())) {
            consumer.acknowledge(msg);   // 已处理过 → 直接 ack
            continue;
        }
        // ② 处理业务
        processWithIdempotency(msg.getValue());
        // ③ 成功确认
        consumer.acknowledge(msg);
        markProcessed(msg.getMessageId().toString());
    } catch (Exception e) {
        // ④ 失败：负 ack（重投）→ 超限进死信
        consumer.negativeAcknowledge(msg);
        log.error("消费失败", e);
    }
}
```

### 6.2 常见错误清单

| 错误 | 后果 | 对策 |
|------|------|------|
| 忘 ack | 消息重投（无限） | 处理成功必 ack |
| 无幂等 | 重投重复处理 | 幂等键/状态机 |
| 订阅模式错误 | 顺序/吞吐不达标 | 按需求选模式 |
| 无死信策略 | 失败消息无限重试 | DeadLetterPolicy |
| 批量配置过大 | 延迟升高 | 平衡吞吐/延迟 |
| 忽略负 ack 延迟 | 快速重投风暴 | 合理延迟 |

### 6.3 生产最佳实践

```text
生产配置清单：
  ① Schema 明确（JSON/Avro——数据契约）
  ② 批量 + 压缩（吞吐）
  ③ 消息键（Key_Shared 场景）
  ④ 死信 + 重试（失败治理）
  ⑤ 幂等消费（可靠性兜底）
  ⑥ 监控（backlog/消费速率）
  ⑦ 连接池/线程调优（并发）

与消息队列理论的呼应：
  可靠性（至少一次 + 幂等）→ 理论 02
  顺序（Key_Shared）→ 理论 03
  积压治理（backlog 监控）→ 理论 04
  死信/重试 → 理论 05
  → 理论是"为什么"，客户端是"怎么做"
```

> 🎯 **核心要点**：Java 集成 = 官方客户端（生产者批量压缩 + 消费者订阅选择 + 事务原子写）+ Spring Pulsar（@PulsarListener 注解）。四个工程纪律：**处理成功必 ack、消费端幂等、死信策略必配、订阅模式按需求选**。Pulsar 客户端 API 设计清晰（订阅模式、死信、事务都是原生参数），配合《消息队列理论与实战》的理论框架即可正确落地。

---

## 7. 小结

| 问题 | 答案 |
|------|------|
| 客户端？ | pulsar-client（官方，二进制协议） |
| 生产优化？ | 批量 + 压缩 + 异步发送 |
| 消费模式？ | 四种订阅 + ack/负 ack + 死信 |
| 事务？ | 跨 topic 原子写 + 写/ack 联动 |
| Spring？ | spring-boot-starter-pulsar（@PulsarListener） |
| 可靠性？ | 处理成功必 ack + 幂等 + 死信 |

**下一模块**：[08-部署运维与选型](08-部署运维与选型.md)　**返回总览**：[00-Pulsar知识体系总览](00-Pulsar知识体系总览.md)
