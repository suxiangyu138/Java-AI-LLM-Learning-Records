# Kafka 核心知识点与 Java 实战

## Kafka 是什么

Kafka 是一个分布式消息流平台，三个核心能力：
- **消息队列**：发布-订阅模式，解耦生产者和消费者
- **流处理**：对数据流做实时转换和分析
- **数据存储**：消息持久化到磁盘，可回溯重放

## 核心概念

| 概念 | 说明 |
|------|------|
| Producer | 生产者，发送消息 |
| Consumer | 消费者，拉取消息（Pull 模式） |
| Consumer Group | 消费者组，组内分担消费，组间广播 |
| Broker | Kafka 服务节点 |
| Topic | 消息主题/分类，逻辑概念 |
| Partition | 分区，物理存储单位，支持并行 |
| Offset | 消息在分区内的偏移量 |
| Replica | 副本，保证高可用 |

## Kafka vs RabbitMQ

| 特性 | Kafka | RabbitMQ |
|------|-------|----------|
| 设计目标 | 高吞吐日志流 | 灵活路由消息 |
| 吞吐量 | 百万级/秒 | 万级/秒 |
| 消息消费 | Pull（拉） | Push（推） |
| 消息回溯 | 支持（按 offset 回退） | 不支持（消费即删除） |
| 消息路由 | 简单 | 复杂（Exchange + Binding） |
| 典型场景 | 日志收集、流处理、事件溯源 | 业务异步、任务分发 |

## Java 实战（Spring Kafka）

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

### 生产者

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
```

```java
@Component
public class OrderProducer {
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void sendOrderCreated(Long orderId) {
        kafkaTemplate.send("order-events", "order-created",
                JSON.toJSONString(Map.of("orderId", orderId)));
    }
}
```

### 消费者

```yaml
spring:
  kafka:
    consumer:
      group-id: order-service
      auto-offset-reset: earliest   # 新消费者从最早消息开始消费
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
```

```java
@Component
public class OrderConsumer {
    @KafkaListener(topics = "order-events", groupId = "order-service")
    public void handleOrderEvent(String message) {
        OrderEvent event = JSON.parseObject(message, OrderEvent.class);
        // 处理业务
    }
}
```

### 可靠投递

```java
// 生产者：同步发送 + 确认
kafkaTemplate.setProducerListener(new ProducerListener<>() {
    @Override
    public void onSuccess(ProducerRecord<String, String> record, RecordMetadata metadata) {
        // 发送成功
    }

    @Override
    public void onError(ProducerRecord<String, String> record, RecordMetadata metadata, Exception ex) {
        // 发送失败，记录到死信表，后续补偿
    }
});

// 消费者：手动签收
@KafkaListener(topics = "order-events")
public void handle(String message, Acknowledgment ack) {
    try {
        // 处理消息
        ack.acknowledge();   // 手动确认
    } catch (Exception e) {
        // 异常不确认，会重试
    }
}
```

## 常见使用场景

- **日志收集**：各服务 → Kafka → ELK，解耦采集和消费速度
- **异步解耦**：下单后发消息，短信服务、积分服务各自消费
- **CDC（变更捕获）**：MySQL binlog → Kafka → 下游实时处理
- **事件溯源**：所有业务变更以事件形式持久化到 Kafka，可回溯

## 实战注意事项

- **消息幂等**：消费者需要防重复（加业务唯一键去重）
- **顺序性**：Kafka 只保证 Partition 内有序。需要顺序的消息发到同一 Partition（key 相同的消息进同一分区）
- **消费者组**：组内消费者数 ≤ Partition 数，多了的会空闲
- **消息大小**：默认 1MB，大消息用外部存储（OSS），Kafka 只传 URL
