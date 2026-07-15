# 微服务 - RocketMQ 消息通信

> **定位**：RocketMQ 是阿里开源的分布式消息中间件，Java 微服务异步解耦的首选方案。完美解决服务间解耦、异步通信、流量削峰。

---

## 目录

1. [核心价值](#1-核心价值)
2. [核心概念](#2-核心概念)
3. [Spring Boot 集成](#3-spring-boot-集成)
4. [进阶用法](#4-进阶用法)
5. [常见问题](#5-常见问题)

---

## 1. 核心价值

| 价值 | 说明 |
|------|------|
| **解耦** | 无直接调用，A 服务宕机不影响 B |
| **削峰** | 消息队列缓冲高并发请求 |
| **可靠投递** | 持久化 + 重试机制，消息必达 |
| **异步** | 非核心流程异步化，提升接口响应 |

---

## 2. 核心概念

| 概念 | 说明 |
|------|------|
| **Producer** | 生产者，发送消息的服务（如订单服务） |
| **Consumer** | 消费者，接收处理消息的服务（如库存服务） |
| **Topic** | 主题，消息分类（如 `order_topic`） |
| **Tag** | 标签，同一主题下的二级分类（如 `order_create`） |

---

## 3. Spring Boot 集成

### 3.1 依赖

```xml
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
    <version>2.2.3.RELEASE</version>
</dependency>
```

### 3.2 配置

```yaml
spring:
  application:
    name: order-service
  cloud:
    rocketmq:
      name-server: localhost:9876
      producer:
        group: order-producer-group
        send-message-timeout: 3000
        retry-times-when-send-failed: 3
```

### 3.3 生产者

```java
@Autowired
private RocketMQTemplate rocketMQTemplate;

// 同步发送（核心业务）
rocketMQTemplate.syncSend("order_topic:order_create", message);

// 异步发送（非核心业务）
rocketMQTemplate.asyncSend("order_topic:order_notify", message,
    (result, ex) -> { /* 回调处理 */ });
```

### 3.4 消费者

```java
@Service
@RocketMQMessageListener(
    topic = "order_topic",
    selectorExpression = "order_create",
    consumerGroup = "stock-consumer-group")
public class StockConsumer implements RocketMQListener<OrderMessage> {
    @Override
    public void onMessage(OrderMessage message) {
        stockMapper.deductStockByOrderId(message.getOrderId());
    }
}
```

> ⚠️ 消息实体类必须 `implements Serializable`。生产者/消费者 Topic + Tag 必须完全一致。

---

## 4. 进阶用法

### 消息过滤

| 方式 | 配置 |
|------|------|
| 标签过滤 | `selectorExpression = "order_create"` |
| SQL 过滤 | `selectorExpression = "amount > 1000"` |

### 死信队列

> 消费失败 16 次后进入死信队列（`%DLQ%消费者组`）

```java
@RocketMQMessageListener(
    topic = "%DLQ%stock-consumer-group",
    consumerGroup = "dlq-consumer-group")
```

### 事务消息

```java
// 发送事务消息
rocketMQTemplate.sendMessageInTransaction("topic:tag", message, bizArg);

// 事务监听器
@RocketMQTransactionListener
public class OrderTransactionListener implements RocketMQLocalTransactionListener {
    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        // 执行本地事务 → COMMIT / ROLLBACK
    }
}
```

---

## 5. 常见问题

| 问题 | 方案 |
|------|------|
| 收不到消息 | 检查 Topic+Tag 一致性、Server 是否启动 |
| 重复消费 | 幂等：订单 ID + Redis/DB 去重 |
| 消息丢失 | 开启持久化 + 延长超时 |
| 依赖冲突 | 匹配 Spring Cloud Alibaba 版本对应表 |
