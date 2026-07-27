# 08 - Kafka 与 Spring 生态集成

> 🎯 Spring Kafka 让 Java 开发者用熟悉的注解和模板模式操作 Kafka — 从基础配置到消息转换、错误处理，覆盖生产级集成全链路

---

## 目录

1. [Spring Kafka 概述](#1-spring-kafka-概述)
2. [快速集成](#2-快速集成)
3. [KafkaTemplate 详解](#3-kafkatemplate-详解)
4. [消息监听容器](#4-消息监听容器)
5. [消息转换与序列化](#5-消息转换与序列化)
6. [错误处理与重试](#6-错误处理与重试)
7. [事务支持](#7-事务支持)
8. [生产级配置模板](#8-生产级配置模板)

---

## 1. Spring Kafka 概述

```text
Spring Kafka = Spring 对 Kafka 客户端的封装

核心组件：
┌─────────────────────────────────────────────────────┐
│  KafkaTemplate         → 发送消息（类比 RestTemplate） │
│  @KafkaListener        → 接收消息（注解驱动）          │
│  MessageListenerContainer → 消息监听容器（线程管理）    │
│  KafkaAdmin            → 自动创建 Topic              │
│  ProducerFactory / ConsumerFactory → 连接工厂        │
└─────────────────────────────────────────────────────┘

依赖：
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

---

## 2. 快速集成

### 2.1 最小化配置

```yaml
# application.yml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
    consumer:
      group-id: my-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
```

### 2.2 最简收发示例

```java
@RestController
public class KafkaDemoController {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @GetMapping("/send")
    public String send(@RequestParam String msg) {
        kafkaTemplate.send("my-topic", msg);
        // 带 Key
        // kafkaTemplate.send("my-topic", "key", msg);
        // 异步回调
        // kafkaTemplate.send("my-topic", msg)
        //     .addCallback(success -> ..., failure -> ...);
        return "ok";
    }

    @KafkaListener(topics = "my-topic", groupId = "my-group")
    public void listen(String message) {
        System.out.println("收到: " + message);
    }
}
```

---

## 3. KafkaTemplate 详解

### 3.1 发送模式

```java
// 1. 异步发送（默认，推荐）
ListenableFuture<SendResult<String, String>> future =
    kafkaTemplate.send("topic", "key", "value");
future.addCallback(
    result -> log.info("成功: {}", result.getRecordMetadata()),
    ex -> log.error("失败: {}", ex.getMessage())
);

// 2. 同步发送（阻塞等待）
SendResult<String, String> result =
    kafkaTemplate.send("topic", "key", "value").get(10, TimeUnit.SECONDS);

// 3. 指定分区
kafkaTemplate.send("topic", 0, "key", "value");  // 分区 0

// 4. 带时间戳
kafkaTemplate.send("topic", 0, System.currentTimeMillis(), "key", "value");

// 5. 使用 ProducerRecord
ProducerRecord<String, String> record =
    new ProducerRecord<>("topic", "key", "value");
record.headers().add("trace-id", "abc123".getBytes());
kafkaTemplate.send(record);
```

### 3.2 批量发送

```java
// 配置
spring.kafka.producer.properties.linger.ms=5
spring.kafka.producer.properties.batch.size=16384

// 不推荐 send(List) — 只是循环调用 send()
// Kafka 真正的批量是自动的 — 靠 batch.size + linger.ms
```

### 3.3 设置默认 Topic

```java
@Bean
public KafkaTemplate<String, String> kafkaTemplate(
        ProducerFactory<String, String> producerFactory) {
    KafkaTemplate<String, String> template = new KafkaTemplate<>(producerFactory);
    template.setDefaultTopic("default-topic");  // send 时可省略 topic
    return template;
}
```

---

## 4. 消息监听容器

### 4.1 @KafkaListener 用法

```java
// 1. 基础 — 消费单个 Topic
@KafkaListener(topics = "topic-1", groupId = "group-1")
public void listen(String message) {
    process(message);
}

// 2. 消费多个 Topic
@KafkaListener(topics = {"topic-1", "topic-2"}, groupId = "group-1")
public void listen(String message) { ... }

// 3. 获取完整 ConsumerRecord
@KafkaListener(topics = "topic-1")
public void listen(ConsumerRecord<String, String> record) {
    log.info("topic={}, partition={}, offset={}, key={}, value={}",
        record.topic(), record.partition(), record.offset(),
        record.key(), record.value());
}

// 4. 获取 Acknowledgment（手动提交）
@KafkaListener(topics = "topic-1")
public void listen(String message, Acknowledgment ack) {
    process(message);
    ack.acknowledge();  // 手动提交位移
}

// 5. 批量消费
@KafkaListener(topics = "topic-1", containerFactory = "batchFactory")
public void listen(List<ConsumerRecord<String, String>> records) {
    records.forEach(r -> process(r.value()));
}
```

### 4.2 手动提交配置

```java
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> factory(
            ConsumerFactory<String, String> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        
        // 手动提交（需要 consumer 配置 enable.auto.commit=false）
        factory.getContainerProperties().setAckMode(
                ContainerProperties.AckMode.MANUAL);
        
        // 推荐：记录后批量手动提交
        // factory.getContainerProperties().setAckMode(
        //         ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        
        return factory;
    }
}
```

### 4.3 两种容器类型

| 容器 | 说明 | 适用 |
|------|------|------|
| **ConcurrentMessageListenerContainer** | 多线程（1+ Consumer），每个线程一个 Consumer | **默认/推荐** |
| **KafkaMessageListenerContainer** | 单线程，单 Consumer | 简单场景 |

```java
// 设置并发度
factory.setConcurrency(3);  // 3 个 Consumer 线程
// 最多 = Partition 数，多了闲置
```

### 4.4 Rebalance 监听

```java
@Component
public class MyRebalanceListener implements ConsumerAwareRebalanceListener {

    @Override
    public void onPartitionsRevokedBeforeCommit(
            Consumer<?, ?> consumer, Collection<TopicPartition> partitions) {
        // 分区被撤销前 — 提交未提交的位移
        consumer.commitSync();
    }

    @Override
    public void onPartitionsAssigned(
            Consumer<?, ?> consumer, Collection<TopicPartition> partitions) {
        // 分区被分配 — 可做初始化
        log.info("Assigned partitions: {}", partitions);
    }
}

// 注册到容器
factory.getContainerProperties()
    .setConsumerRebalanceListener(new MyRebalanceListener());
```

---

## 5. 消息转换与序列化

### 5.1 JSON 序列化

```java
// 发送对象而非字符串

// 配置
@Bean
public KafkaTemplate<String, Object> kafkaTemplate(
        ProducerFactory<String, Object> producerFactory) {
    return new KafkaTemplate<>(producerFactory);
}

@Bean
public ProducerFactory<String, Object> producerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    return new DefaultKafkaProducerFactory<>(props);
}

// 发送
kafkaTemplate.send("topic", new User("张三", 25));
```

### 5.2 JSON 反序列化

```java
@Bean
public ConsumerFactory<String, User> consumerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
    props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.dto");
    props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, User.class);
    return new DefaultKafkaConsumerFactory<>(props);
}

// 消费
@KafkaListener(topics = "topic")
public void listen(User user) {
    log.info("user: {}", user);
}
```

### 5.3 自定义消息转换器

```java
// Spring 自动配置 StringJsonMessageConverter
// 也可自行配置

@Bean
public RecordMessageConverter converter() {
    StringJsonMessageConverter converter = new StringJsonMessageConverter();
    ObjectMapper mapper = new ObjectMapper();
    // 自定义 ObjectMapper
    converter.setObjectMapper(mapper);
    return converter;
}
```

---

## 6. 错误处理与重试

### 6.1 消费者异常处理

```java
// 方案 1：SeekToCurrentErrorHandler（Spring Kafka 2.x）
@Bean
public ConcurrentKafkaListenerContainerFactory<String, String> factory(
        ConsumerFactory<String, String> consumerFactory) {
    ConcurrentKafkaListenerContainerFactory<String, String> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);
    
    // 重试 3 次，间隔 1 秒；之后跳过
    factory.setCommonErrorHandler(
            new DefaultErrorHandler(
                    new FixedBackOff(1000L, 3L)));
    return factory;
}

// 方案 2：CommonErrorHandler（Spring Kafka 2.8+）
DefaultErrorHandler errorHandler = new DefaultErrorHandler((record, exception) -> {
    // 重试耗尽后的处理：记录到死信队列/数据库/日志
    log.error("消费最终失败: {}", record, exception);
}, new FixedBackOff(1000L, 3L));

// 不对某些异常重试
errorHandler.addNotRetryableExceptions(
    DeserializationException.class,
    IllegalArgumentException.class
);
```

### 6.2 死信队列（DLT）

```java
// Spring Kafka 2.8+ 原生支持 DLT

@Configuration
public class DeadLetterConfig {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, String> template) {
        // 失败消息发送到 <原topic>.DLT
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(template);
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
    }

    // 监听 DLT 做人工处理
    @KafkaListener(topics = "my-topic.DLT")
    public void handleDLT(ConsumerRecord<String, String> record) {
        log.warn("DLT收到: key={}, value={}, headers={}",
            record.key(), record.value(), record.headers());
        // 写入数据库 / 告警 / 人工处理
    }
}
```

### 6.3 生产者异常处理

```java
// 获取发送结果
ListenableFuture<SendResult<String, String>> future =
    kafkaTemplate.send("topic", "msg");

future.addCallback(
    result -> log.info("成功: offset={}", result.getRecordMetadata().offset()),
    ex -> {
        log.error("发送失败", ex);
        // 写入本地重试表 / 告警
        saveToRetryQueue(topic, key, value);
    }
);
```

---

## 7. 事务支持

### 7.1 配置事务

```java
// 事务生产者
@Bean
public ProducerFactory<String, String> producerFactory() {
    DefaultKafkaProducerFactory<String, String> factory =
            new DefaultKafkaProducerFactory<>(producerConfigs());
    factory.setTransactionIdPrefix("tx-");  // 开启事务
    return factory;
}

@Bean
public KafkaTransactionManager<String, String> kafkaTransactionManager(
        ProducerFactory<String, String> producerFactory) {
    return new KafkaTransactionManager<>(producerFactory);
}

// 使用 @Transactional
@Transactional(transactionManager = "kafkaTransactionManager")
public void sendInTransaction() {
    kafkaTemplate.send("topic-A", "msg1");
    kafkaTemplate.send("topic-B", "msg2");
    // 原子：要么都成功，要么都失败
}
```

### 7.2 Kafka + DB 事务

```text
⚠️ Kafka + DB 不是真正的 XA 事务！

方案 1：CDC（Change Data Capture）— 推荐
  写入 DB → Debezium 捕获 binlog → 写入 Kafka

方案 2：发件箱模式（Outbox Pattern）— 推荐
  INSERT INTO outbox(event_type, payload) ...
  Debezium/定时任务 扫 outbox 表 → 发 Kafka

方案 3：先发消息后落库（有风险）
  Kafka 发送成功 → DB 落库失败 → 消息已发出 → 不一致
  
方案 4：先落库后发消息（有风险）
  DB 落库成功 → Kafka 发送失败 → 需本地消息表兜底
```

---

## 8. 生产级配置模板

### 8.1 完整 application.yml

```yaml
spring:
  kafka:
    bootstrap-servers: broker1:9092,broker2:9092,broker3:9092
    
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 2147483647
      properties:
        enable.idempotence: true
        max.in.flight.requests.per.connection: 5
        linger.ms: 5
        batch.size: 32768
        compression.type: lz4
        request.timeout.ms: 30000
        delivery.timeout.ms: 120000

    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      group-id: ${spring.application.name}
      enable-auto-commit: false
      auto-offset-reset: earliest
      max-poll-records: 500
      properties:
        partition.assignment.strategy:
          org.apache.kafka.clients.consumer.CooperativeStickyAssignor
        spring.json.trusted.packages: "com.example.*"
    
    listener:
      ack-mode: manual
      concurrency: 3
```

### 8.2 Java Config 完整版

```java
@Configuration
@EnableKafka
public class KafkaConfig {

    // ===== Producer =====
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ===== Consumer =====
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "my-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG,
                CooperativeStickyAssignor.class.getName());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(
                ContainerProperties.AckMode.MANUAL);
        factory.setCommonErrorHandler(
                new DefaultErrorHandler(new FixedBackOff(1000L, 3L)));
        return factory;
    }
}
```

---

## Spring Kafka 速查

| 注解/类 | 作用 |
|------|------|
| `@KafkaListener` | 标记消息监听方法 |
| `@KafkaHandler` | 类级 Listener 的多方法路由 |
| `KafkaTemplate` | 发送消息 |
| `ProducerFactory / ConsumerFactory` | 连接工厂 |
| `DefaultErrorHandler` | 消费异常处理 + DLT |
| `@SendTo` | 消费后转发到另一个 Topic |
| `KafkaAdmin` | 自动创建 Topic |
