# 微服务集成 RocketMQ
> 阿里开源的高吞吐消息中间件，电商金融场景首选——事务消息、顺序消息、Tag 过滤、死信队列全解析。

## 目录
1. [RocketMQ 核心架构](#1-rocketmq-核心架构)
2. [Spring Boot 基础集成](#2-spring-boot-基础集成)
3. [生产者开发](#3-生产者开发)
4. [消费者开发](#4-消费者开发)
5. [事务消息](#5-事务消息)
6. [顺序消息](#6-顺序消息)
7. [消息可靠性保障](#7-消息可靠性保障)
8. [常见问题](#8-常见问题)

---

## 1. RocketMQ 核心架构

### 1.1 四大核心组件

| 组件 | 说明 |
|------|------|
| **NameServer** | 路由注册中心（无状态，可集群部署）|
| **Broker** | 消息存储服务器，支持主从同步 |
| **Producer** | 生产者 |
| **Consumer** | 消费者 |

```text
                    ┌──────────────────┐
                    │   NameServer     │
                    │  (路由注册发现)   │
                    └────────┬─────────┘
                             │
             ┌───────────────┼───────────────┐
             │               │               │
        ┌────▼────┐    ┌────▼────┐    ┌────▼────┐
        │Producer1│    │Producer2│    │Producer3│
        └────┬────┘    └────┬────┘    └────┬────┘
             └───────┬──────┴──────┬────────┘
                     │             │
                ┌────▼────┐   ┌────▼────┐
                │  Broker │   │  Broker │  (主从集群)
                │ Master  │   │  Slave  │
                └────┬────┘   └────┬────┘
                     │             │
                ┌────▼────┐   ┌────▼────┐
                │Consumer1│   │Consumer2│
                └─────────┘   └─────────┘
```

### 1.2 消息模型

| 概念 | 说明 |
|------|------|
| **Topic** | 消息主题分类（如 `order_topic`）|
| **Tag** | 二级标签（如 `order_create`、`order_pay`）|
| **MessageQueue** | Topic 的物理分片，并行消费基本单位 |
| **ConsumerGroup** | 消费者组，组内负载均衡 |
| **Offset** | 消费位置标记 |

### 1.3 消息类型
| 类型 | 说明 | 场景 |
|------|------|------|
| 普通消息 | 无序消息 | 大多数业务 |
| 顺序消息 | 全局/分区有序 | 订单状态流转 |
| 事务消息 | 最终一致性 | 跨服务数据同步 |
| 延迟消息 | 固定级别延迟 | 订单超时取消 |
| 批量消息 | 批量发送 | 批量数据同步 |

---

## 2. Spring Boot 基础集成

### Maven 依赖
```xml
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
    <version>2.2.3.RELEASE</version>
</dependency>
```

### application.yml
```yaml
spring:
  cloud:
    rocketmq:
      name-server: localhost:9876
      producer:
        group: order-producer-group
        send-message-timeout: 3000
        retry-times-when-send-failed: 3
```

### NameServer 部署
```bash
# 启动 NameServer（先启动）
nohup sh bin/mqnamesrv &

# 启动 Broker
nohup sh bin/mqbroker -n localhost:9876 &
```

---

## 3. 生产者开发

### 3.1 同步发送（核心业务）
```java
@Service
public class OrderProducer {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    public void sendOrderMessage(OrderMessage order) {
        // syncSend：同步发送，阻塞等待 Broker 确认
        SendResult result = rocketMQTemplate.syncSend(
            "order_topic:order_create",
            order
        );
        if (result.getSendStatus() == SendStatus.SEND_OK) {
            log.info("消息发送成功: {}", result.getMsgId());
        } else {
            log.error("消息发送失败: {}", result.getSendStatus());
        }
    }
}
```

### 3.2 异步发送（非核心业务）
```java
public void sendAsync(OrderMessage order) {
    rocketMQTemplate.asyncSend(
        "order_topic:order_notify",
        order,
        new SendCallback() {
            @Override
            public void onSuccess(SendResult result) {
                log.info("异步发送成功: {}", result.getMsgId());
            }

            @Override
            public void onException(Throwable e) {
                log.error("异步发送失败", e);
                // 重试或写入本地补偿表
            }
        }
    );
}
```

### 3.3 单向发送（不关心结果）
```java
public void sendOneWay(OrderMessage order) {
    rocketMQTemplate.sendOneWay("order_topic:log", order);
}
```

---

## 4. 消费者开发

### 4.1 基本消费者
```java
@Service
@RocketMQMessageListener(
    topic = "order_topic",
    selectorExpression = "order_create",    // Tag 过滤
    consumerGroup = "stock-consumer-group"
)
public class StockConsumer implements RocketMQListener<OrderMessage> {

    @Override
    public void onMessage(OrderMessage message) {
        // 默认自动提交偏移量
        stockService.deductStock(message.getOrderId());
        log.info("库存扣减成功: {}", message.getOrderId());
    }
}
```

### 4.2 手动 ACK（推荐）
```java
@Service
@RocketMQMessageListener(
    topic = "order_topic",
    selectorExpression = "order_pay",
    consumerGroup = "pay-consumer-group"
)
public class PayConsumer implements RocketMQListener<OrderMessage> {

    @Override
    public ConsumeConcurrentlyStatus onMessage(OrderMessage message) {
        try {
            // 1. 执行业务
            paymentService.processPayment(message);

            // 2. 成功 → ACK
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        } catch (Exception e) {
            log.error("消费失败", e);
            // 3. 失败 → 稍后重试
            return ConsumeConcurrentlyStatus.RECONSUME_LATER;
        }
    }
}
```

### 4.3 消息过滤
| 过滤方式 | 配置 | 说明 |
|----------|------|------|
| **Tag 过滤** | `selectorExpression = "order_create"` | 精确匹配 |
| 多 Tag | `selectorExpression = "order_create \|\| order_pay"` | 或匹配 |
| SQL 过滤 | `selectorExpression = "amount > 1000"` | 基于属性 |

---

## 5. 事务消息

### 5.1 原理
```text
1. Producer 发送 half 消息（Broker 标记为"暂不可见"）
2. Broker 返回确认
3. Producer 执行本地事务
   ├── COMMIT → Broker 使消息可见 → Consumer 消费
   ├── ROLLBACK → Broker 删除消息
   └── UNKNOWN → Broker 定时回查 Producer 事务状态
4. 回查超时 → 消息自动回滚
```

### 5.2 代码实现

**生产者**
```java
@Service
public class OrderTransactionProducer {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    public void createOrderWithTransaction(OrderMessage order) {
        Message message = MessageBuilder.withPayload(order)
            .setHeader("orderId", order.getOrderId())
            .build();

        TransactionSendResult result = rocketMQTemplate.sendMessageInTransaction(
            "order_topic:order_create",
            message,
            order.getOrderId()    // 业务参数
        );
        log.info("事务消息状态: {}", result.getLocalTransactionState());
    }
}
```

**事务监听器**
```java
@RocketMQTransactionListener
public class OrderTransactionListener implements RocketMQLocalTransactionListener {

    @Autowired
    private OrderMapper orderMapper;

    // Step 1: 执行本地事务
    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        String orderId = (String) arg;
        try {
            orderMapper.insert(orderId, "PENDING");
            return RocketMQLocalTransactionState.COMMIT;
        } catch (Exception e) {
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }

    // Step 2: 回查（Broker 未收到 Commit/Rollback 时调用）
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        String orderId = (String) msg.getHeaders().get("orderId");
        Order order = orderMapper.selectById(orderId);
        if (order != null) {
            return RocketMQLocalTransactionState.COMMIT;
        }
        return RocketMQLocalTransactionState.ROLLBACK;
    }
}
```

---

## 6. 顺序消息

### 6.1 生产者：将同一业务发送到同一队列
```java
public void sendOrderlyMessage(OrderMessage order) {
    rocketMQTemplate.syncSendOrderly(
        "order_topic:order_status",
        order,
        order.getOrderId()     // HashKey → 相同 Key 进入同一队列
    );
}
```

### 6.2 消费者：顺序消费
```java
@Service
@RocketMQMessageListener(
    topic = "order_topic",
    selectorExpression = "order_status",
    consumerGroup = "status-consumer-group",
    consumeMode = ConsumeMode.ORDERLY   // 顺序消费模式
)
public class OrderlyConsumer implements RocketMQListener<OrderMessage> {

    @Override
    public ConsumeOrderlyStatus onMessage(OrderMessage message) {
        try {
            processOrderStatus(message);
            return ConsumeOrderlyStatus.SUCCESS;
        } catch (Exception e) {
            // 顺序消息重试：暂停当前队列片刻
            return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
        }
    }
}
```

> ⚠️ 顺序消费会降低吞吐量，因为同队列消息串行处理。

---

## 7. 消息可靠性保障

### 7.1 生产者端
| 配置 | 说明 | 建议 |
|------|------|:----:|
| `retry-times-when-send-failed: 3` | 发送失败重试次数 | 2-3 次 |
| `send-message-timeout: 3000` | 发送超时 | 3000-5000ms |
| 同步发送 | 阻塞等确认 | 核心业务 |
| 异步发送 + 回调 | 非阻塞 | 非核心业务 |

### 7.2 Broker 端
```ini
# broker.conf
flushDiskType=ASYNC_FLUSH         # 异步刷盘（性能优先）
# flushDiskType=SYNC_FLUSH        # 同步刷盘（金融场景）
brokerRole=SYNC_MASTER            # 同步复制
```

| 刷盘策略 | 可靠性 | 性能 |
|:--------:|:------:|:----:|
| 同步刷盘 | ⭐⭐⭐ | ⭐ |
| 异步刷盘 | ⭐⭐ | ⭐⭐⭐ |

### 7.3 消费者端
```java
@Override
public ConsumeConcurrentlyStatus onMessage(OrderMessage message) {
    // 幂等校验（推荐 Redis SET NX）
    String msgId = message.getMsgId();
    if (Boolean.TRUE.equals(redisTemplate.hasKey("mq:msg:" + msgId))) {
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    try {
        processOrder(message);
        redisTemplate.opsForValue().set("mq:msg:" + msgId, "1", 24, TimeUnit.HOURS);
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    } catch (Exception e) {
        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
    }
}
```

### 7.4 死信队列
> 消费失败达到最大重试次数（默认 16 次）后，消息进入死信队列 `%DLQ%消费者组名`。

```java
@RocketMQMessageListener(
    topic = "%DLQ%stock-consumer-group",   // 死信队列
    consumerGroup = "dlq-consumer-group"
)
public class DlxConsumer implements RocketMQListener<MessageExt> {
    @Override
    public void onMessage(MessageExt message) {
        log.error("死信消息: msgId={}, body={}",
            message.getMsgId(), new String(message.getBody()));
        // 人工介入：记录告警、补偿处理
    }
}
```

---

## 8. 常见问题

| 问题 | 排查方向 | 解决方案 |
|------|----------|----------|
| **收不到消息** | Topic/Tag 是否一致 | 检查 `selectorExpression` |
| **重复消费** | 幂等机制 | 唯一 msgId + Redis 去重 |
| **消息丢失** | 持久化配置 | 开启同步刷盘 + 同步复制 |
| **顺序乱序** | 确认顺序消费模式 | 同 Key 同队列 + `ConsumeMode.ORDERLY` |
| **消息积压** | 消费速度 | 增加消费者 + 批量消费 |
| **事务不回查** | 监听器 | 确保 `checkLocalTransaction` 正确实现 |
| **依赖冲突** | 版本 | 匹配 Spring Cloud Alibaba 版本对应表 |

---

## 生产级最佳实践
| 实践 | 说明 |
|------|------|
| 消息持久化 | 队列 `durable=true` + 消息 `PERSISTENT` |
| 幂等设计 | 唯一 msgId + Redis/DB 去重 |
| 手动 ACK | 避免自动提交导致消息丢失 |
| 死信队列 | 消费失败 3 次后进入 DLQ |
| 监控告警 | 消息积压/消费延迟 > 阈值告警 |
| 压测先行 | 上线前进行全链路压测 |

> 💡 相比 RabbitMQ：RocketMQ 在吞吐量和事务消息上有显著优势，适合电商金融等需要高吞吐和高可靠的场景。
