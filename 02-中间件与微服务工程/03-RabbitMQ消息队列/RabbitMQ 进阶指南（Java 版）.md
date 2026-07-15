# RabbitMQ 进阶指南（Java 版）

> **定位**：面向有入门基础的 Java 开发者，聚焦生产级可靠性、高级特性、性能优化和实践。

---

## 目录

1. [消息可靠性进阶](#1-消息可靠性进阶)
2. [高级特性](#2-高级特性)
3. [性能优化](#3-性能优化)
4. [生产级最佳实践](#4-生产级最佳实践)

---

## 1. 消息可靠性进阶

### 1.1 全链路持久化

```java
// 生产者：消息持久化 + 重试
rabbitTemplate.convertAndSend(exchange, routingKey, message, msg -> {
    msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
    return msg;
});
```

```yaml
# 连接可靠性
spring:
  rabbitmq:
    connection-timeout: 10000
    connection-retry:
      enabled: true
      max-attempts: 5
      initial-interval: 1000
      multiplier: 2
```

### 1.2 手动确认 + 异常拒绝

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        ack-mode: manual
        prefetch: 1
        retry:
          enabled: true
          max-attempts: 3
```

```java
@RabbitListener(queues = "test_queue")
public void receive(Message message, Channel channel) throws IOException {
    long deliveryTag = message.getMessageProperties().getDeliveryTag();
    try {
        doBusiness(new String(message.getBody()));
        channel.basicAck(deliveryTag, false);       // 成功 → ACK
    } catch (Exception e) {
        channel.basicNack(deliveryTag, false, true); // 失败 → NACK + 重投
    }
}
```

### 1.3 幂等防重复

```java
String msgId = message.getMessageProperties().getMessageId();
String redisKey = "mq:msg:idempotent:" + msgId;
if (redisTemplate.hasKey(redisKey)) {
    channel.basicAck(deliveryTag, false);  // 已消费，跳过
    return;
}
doBusiness(msg);
redisTemplate.opsForValue().set(redisKey, "1", 30, TimeUnit.MINUTES);
channel.basicAck(deliveryTag, false);
```

---

## 2. 高级特性

### 2.1 死信队列（DLX）

```java
@Bean
public Queue normalQueue() {
    Map<String, Object> args = new HashMap<>();
    args.put("x-dead-letter-exchange", "dlx_exchange");
    args.put("x-dead-letter-routing-key", "dlx_key");
    args.put("x-message-ttl", 60000);
    return new Queue("normal_queue", true, false, false, args);
}
```

### 2.2 延迟队列

```text
核心原理：延迟队列无消费者 + 消息 TTL 过期 → 自动投递到死信队列 → 死信消费者处理
```

```java
// Flexibly set delay per message
properties.setExpiration(String.valueOf(delayTime));
```

### 2.3 事务消息

```java
@Transactional(transactionManager = "rabbitTransactionManager")
public void sendTransactionMessage(String exchange, String routingKey, String message) {
    saveOrder(message);          // 业务操作
    rabbitTemplate.convertAndSend(exchange, routingKey, message); // 消息发送
    // 任一步失败 → 全部回滚
}
```

> ⚠️ 事务降低性能，非核心场景推荐"最终一致性"方案。

---

## 3. 性能优化

| 优化点 | 配置/做法 |
|--------|----------|
| **连接池** | `cache.connection.mode: channel` + `size: 5` |
| **信道池** | `cache.channel.size: 10` |
| **预取数** | `prefetch: 5`（按业务调整） |
| **批量消费** | `setBatchListener(true)` + `setBatchSize(10)` |
| **异步消费** | 线程池异步处理耗时业务 |
| **消息压缩** | 大消息（>10KB）GZIP 压缩 |

---

## 4. 生产级最佳实践

| 层级 | 实践 |
|------|------|
| **配置** | 多环境 profile 分离 + 必开手动确认+持久化+重连 |
| **生产者** | 消息重试 + 异常处理 + 唯一 msgId |
| **消费者** | 手动 ACK + 异常 NACK + 幂等校验 |
| **监控** | Web 界面监控队列/连接 + 日志 + 死信告警 |
