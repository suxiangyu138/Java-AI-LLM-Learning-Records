# RabbitMQ 核心原理与 Spring Boot 实战
> 从 AMQP 模型到 Java 生产级代码——Spring AMQP 声明式配置、手动确认、死信队列、幂等消费全掌握。

## 目录
1. [Spring Boot 基础集成](#1-spring-boot-基础集成)
2. [配置类声明式定义](#2-配置类声明式定义)
3. [RabbitTemplate 生产者](#3-rabbittemplate-生产者)
4. [@RabbitListener 消费者](#4-rabbitlistener-消费者)
5. [消息可靠性进阶](#5-消息可靠性进阶)
6. [死信队列与延迟队列](#6-死信队列与延迟队列)
7. [幂等性保障](#7-幂等性保障)
8. [配置管理](#8-配置管理)

---

## 1. Spring Boot 基础集成

### 1.1 Maven 依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

> 💡 `spring-boot-starter-amqp` 自动引入 `spring-rabbit` 和 `amqp-client`。无需额外指定版本，Spring Boot 管理依赖版本。

### 1.2 application.yml 基础配置

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: admin
    password: 123456
    virtual-host: /
    connection-timeout: 10000
    connection-retry:
      enabled: true
      max-attempts: 5
      initial-interval: 1000
      multiplier: 2
    listener:
      simple:
        concurrency: 3
        max-concurrency: 10
        ack-mode: manual
        prefetch: 5
    template:
      mandatory: true
```

### 1.3 多环境配置

```yaml
# application-dev.yml
spring:
  rabbitmq:
    host: localhost
    username: guest
    password: guest
    virtual-host: /
    listener:
      simple:
        ack-mode: auto    # 开发环境可自动确认

# application-prod.yml
spring:
  rabbitmq:
    host: 10.0.1.100,10.0.1.101
    username: prod_app
    password: ENC(...)
    virtual-host: /prod
    listener:
      simple:
        ack-mode: manual
        prefetch: 1
        retry:
          enabled: true
          max-attempts: 3
```

**三环境配置模板：**

| 配置项 | 开发 | 测试 | 生产 |
|--------|:--:|:---:|:---:|
| `ack-mode` | auto | auto | **manual** |
| `prefetch` | 10 | 5 | 1 |
| 死信队列 | ❌ | ✅ | ✅ |
| 密码存储 | 明文 | 明文 | **加密** |
| 日志级别 | debug | debug | info |

---

## 2. 配置类声明式定义

### 2.1 基础配置：Direct + Queue + Binding

```java
@Configuration
public class RabbitConfig {

    @Bean
    public Queue orderQueue() {
        return new Queue("order.queue", true, false, false);
        // 参数: name, durable, exclusive, autoDelete
    }

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange("order.exchange", true, false);
    }

    @Bean
    public Binding orderBinding() {
        return BindingBuilder
            .bind(orderQueue())
            .to(orderExchange())
            .with("order.create");
    }

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange("log.exchange");
    }

    @Bean
    public Queue errorQueue() {
        return new Queue("log.error", true);
    }

    @Bean
    public Binding errorBinding() {
        return BindingBuilder.bind(errorQueue())
            .to(topicExchange()).with("log.error");
    }
}
```

### 2.2 Queue 构造参数详解

| 参数 | 说明 | 默认值 |
|------|------|:------:|
| `name` | 队列名称 | 必填 |
| `durable` | 持久化（重启后保留）| `true` |
| `exclusive` | 独占（仅创建者连接可用）| `false` |
| `autoDelete` | 自动删除（无消费者时）| `false` |
| `arguments` | 扩展参数（TTL、死信、队列长度）| `null` |

### 2.3 队列扩展参数（arguments）

| 参数 | 类型 | 说明 |
|------|------|------|
| `x-message-ttl` | long (ms) | 消息存活时间 |
| `x-expires` | long (ms) | 队列自动删除时间 |
| `x-max-length` | int | 队列最大消息数 |
| `x-max-length-bytes` | int | 队列最大字节数 |
| `x-dead-letter-exchange` | string | 死信交换机名称 |
| `x-dead-letter-routing-key` | string | 死信路由键 |
| `x-max-priority` | int | 优先级队列（0-255）|
| `x-queue-mode` | string | `lazy` 懒加载模式 |

---

## 3. RabbitTemplate 生产者

### 3.1 基本发送

```java
@Component
public class OrderProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void send(String message) {
        rabbitTemplate.convertAndSend(
            "order.exchange", "order.create", message);
    }

    public void sendOrder(OrderMessage msg) {
        rabbitTemplate.convertAndSend(
            "order.exchange", "order.create", msg);
    }
}
```

### 3.2 带消息属性发送（持久化 + 幂等）

```java
@Component
public class OrderProducer {

    public void sendWithProps(OrderMessage msg) {
        MessageProperties props = new MessageProperties();
        props.setMessageId(UUID.randomUUID().toString());
        props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        props.setContentType("application/json");
        props.setPriority(5);
        props.setExpiration("30000");

        Message message = new Message(JSON.toJSONBytes(msg), props);
        rabbitTemplate.convertAndSend(
            "order.exchange", "order.create", message);
    }
}
```

### 3.3 带回调确认（Publisher Confirm + Return）

```java
@Component
public class OrderProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void init() {
        // Confirm: 消息是否到达交换机
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.info("消息发送成功, id: {}", correlationData.getId());
            } else {
                log.error("消息发送失败, id: {}, cause: {}",
                    correlationData.getId(), cause);
                // 写入本地重试表
            }
        });

        // Return: 交换机无法路由到队列
        rabbitTemplate.setReturnsCallback(returned -> {
            log.warn("消息未找到队列, exchange: {}, routingKey: {}",
                returned.getExchange(), returned.getRoutingKey());
        });
    }

    public void sendWithConfirm(String message) {
        CorrelationData cd = new CorrelationData(UUID.randomUUID().toString());
        rabbitTemplate.convertAndSend(
            "order.exchange", "order.create", message, cd);
    }
}
```

### 3.4 RabbitTemplate 核心方法

| 方法 | 说明 | 场景 |
|------|------|------|
| `convertAndSend(exchange, key, msg)` | 发送消息（自动序列化）| 通用发送 |
| `send(exchange, key, msg)` | 发送 Message 对象 | 需自定义属性 |
| `convertSendAndReceive()` | 发送并等待回复 | RPC 模式 |
| `setConfirmCallback()` | 发送确认回调 | 生产者可靠性 |
| `setReturnsCallback()` | 退回回调 | 路由失败通知 |

---

## 4. @RabbitListener 消费者

### 4.1 自动确认（简单场景）

```java
@Component
public class OrderConsumer {

    @RabbitListener(queues = "order.queue")
    public void handleOrder(String message) {
        System.out.println("收到消息: " + message);
        // 自动确认：方法返回即视为消费成功
    }
}
```

### 4.2 手动确认（生产推荐）

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
@Component
@Slf4j
public class OrderConsumer {

    @RabbitListener(queues = "order.queue")
    public void handleOrder(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            OrderMessage order = JSON.parseObject(message.getBody(), OrderMessage.class);
            processOrder(order);

            channel.basicAck(deliveryTag, false);
            log.info("消息消费成功, deliveryTag: {}", deliveryTag);

        } catch (Exception e) {
            log.error("消息消费失败, deliveryTag: {}", deliveryTag, e);

            if (message.getMessageProperties().getRedelivered()) {
                log.warn("已重试仍失败, 转死信队列");
                channel.basicNack(deliveryTag, false, false);
            } else {
                channel.basicNack(deliveryTag, false, true);
            }
        }
    }
}
```

### 4.3 @RabbitListener 常用属性

| 属性 | 说明 | 示例 |
|------|------|------|
| `queues` | 监听的队列名数组 | `"order.queue"` |
| `queuesToDeclare` | 声明并监听 | `@Queue("log.info")` |
| `concurrency` | 消费者并发数 | `"3-5"` |
| `containerFactory` | 监听容器工厂 | `"myFactory"` |
| `priority` | 消费者优先级 | `10` |

---

## 5. 消息可靠性进阶

### 5.1 全链路持久化

| 层级 | 配置 | 说明 |
|------|------|------|
| 交换机 | `durable=true` | 交换机元数据持久化 |
| 队列 | `durable=true` | 队列 + 消息持久化 |
| 消息 | `deliveryMode=2 (PERSISTENT)` | 消息写入磁盘 |

```java
MessageProperties props = new MessageProperties();
props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
props.setContentType("application/json");
Message message = new Message(JSON.toJSONBytes(order), props);
rabbitTemplate.send("order.exchange", "order.create", message);
```

### 5.2 Publisher Confirm + Return 配置

```yaml
spring:
  rabbitmq:
    publisher-confirm-type: correlated   # 异步回调确认（推荐）
    publisher-returns: true              # 开启退回
    template:
      mandatory: true                    # 强制路由
```

### 5.3 Consumer 重试策略

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        retry:
          enabled: true           # 开启重试
          max-attempts: 3         # 最大重试次数
          initial-interval: 1000  # 初始间隔（ms）
          multiplier: 2           # 倍数增长因子
          max-interval: 10000     # 最大间隔（ms）
```

> ⚠️ 重试耗尽后抛出 `AmqpRejectAndDontRequeueException`，消息被拒绝且不重新入队 → 转入死信队列。设置合理最大值（推荐 3 次）。

---

## 6. 死信队列与延迟队列

### 6.1 死信触发条件

| 条件 | 说明 |
|------|------|
| 消费者拒绝且不重新投递 | `NACK + requeue=false` |
| 消息 TTL 过期 | 到达过期时间未被消费 |
| 队列满 | 新消息挤掉旧消息（基于 `x-max-length`）|

### 6.2 死信队列配置

```java
@Configuration
public class DlxConfig {

    @Bean
    public Queue normalQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "dlx.exchange");
        args.put("x-dead-letter-routing-key", "dlx");
        args.put("x-message-ttl", 30000);
        args.put("x-max-length", 10000);
        args.put("x-overflow", "reject-publish");
        return new Queue("normal.queue", true, false, false, args);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange("dlx.exchange", true, false);
    }

    @Bean
    public Queue dlxQueue() {
        return new Queue("dlx.queue", true);
    }

    @Bean
    public Binding dlxBinding() {
        return BindingBuilder.bind(dlxQueue())
            .to(dlxExchange()).with("dlx");
    }

    @RabbitListener(queues = "dlx.queue")
    public void handleDlx(Message message, Channel channel) throws IOException {
        log.warn("死信消息: {}", new String(message.getBody()));
        // 记录告警、自动补偿
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
```

### 6.3 延迟队列（TTL + DLX 方式）

```text
核心原理：延迟队列无消费者 + 消息 TTL 过期 → 自动投递到死信队列 → 死信消费者处理
场景：订单30分钟未支付 → 自动取消
```

```java
// 队列统一 TTL
@Bean
public Queue delayQueue() {
    Map<String, Object> args = new HashMap<>();
    args.put("x-dead-letter-exchange", "order.dlx.exchange");
    args.put("x-dead-letter-routing-key", "order.cancel");
    args.put("x-message-ttl", 1800000);  // 30分钟
    return new Queue("order.delay.queue", true);
}

// 每条消息独立 TTL
MessageProperties properties = new MessageProperties();
properties.setExpiration("1800000");
properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
Message message = new Message(body, properties);
rabbitTemplate.convertAndSend("order.delay.exchange", "order.delay", message);

// 死信消费者处理延迟任务
@RabbitListener(queues = "order.cancel.queue")
public void handleDelayTask(OrderMessage order) {
    if (order.getStatus() == OrderStatus.UNPAID) {
        orderService.cancelOrder(order.getId());
    }
}
```

### 6.4 延迟交换机插件（推荐）

```bash
rabbitmq-plugins enable rabbitmq_delayed_message_exchange
```

```java
@Configuration
public class DelayPluginConfig {

    @Bean
    public CustomExchange delayExchange() {
        return new CustomExchange("delay.exchange",
            "x-delayed-message", true, false,
            Map.of("x-delayed-type", "direct"));
    }

    @Bean
    public Queue delayQueue() {
        return new Queue("delay.queue", true);
    }

    @Bean
    public Binding delayBinding() {
        return BindingBuilder.bind(delayQueue())
            .to(delayExchange()).with("delay.key").noargs();
    }
}

@Component
public class DelayProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendDelay(String message, long delayMs) {
        MessageProperties props = new MessageProperties();
        props.setHeader("x-delay", (int) delayMs);
        props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        rabbitTemplate.send("delay.exchange", "delay.key",
            new Message(message.getBytes(), props));
    }
}
```

**方案对比：**

| 方案 | 方式 | 优点 | 缺点 |
|------|------|------|------|
| TTL + DLX | 消息过期 + 死信投递 | 内置支持 | TTL 不够灵活 |
| 延迟插件 | `x-delayed-message` | 毫秒级灵活 | 需额外安装 |

---

## 7. 幂等性保障

### 7.1 为什么需要幂等

```text
重复消费场景：
Producer → Broker → Consumer
    ├── 发送消息 → 推送 → 处理成功 → ACK 丢失
    └── 重试发送 → 再次推送 → 重复处理!
```

### 7.2 方案一：msgId + Redis（推荐）

```java
@Component
@Slf4j
public class IdempotentConsumer {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @RabbitListener(queues = "order.queue")
    public void handle(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageProperties().getMessageId();

        if (messageId == null) {
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        // SET NX：原子操作，首次执行成功
        String redisKey = "mq:msg:id:" + messageId;
        Boolean success = redisTemplate.opsForValue()
            .setIfAbsent(redisKey, "1", Duration.ofHours(24));

        if (Boolean.FALSE.equals(success)) {
            log.info("重复消息已跳过, msgId: {}", messageId);
            channel.basicAck(deliveryTag, false);
            return;
        }

        try {
            doBusiness(message);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            redisTemplate.delete(redisKey);  // 释放幂等锁
            channel.basicNack(deliveryTag, false,
                !message.getMessageProperties().getRedelivered());
        }
    }
}
```

### 7.3 方案二：数据库唯一约束

```sql
CREATE TABLE mq_consume_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id VARCHAR(64) NOT NULL,
    status TINYINT DEFAULT 0,
    create_time DATETIME NOT NULL,
    UNIQUE KEY uk_message_id (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

```java
@Transactional
public void consumeWithUniqueKey(String messageId, Runnable business) {
    int count = consumeRecordMapper.insertIgnore(messageId);
    if (count == 0) return;  // 已消费
    business.run();
    consumeRecordMapper.updateStatus(messageId, 1);
}
```

### 7.4 方案三：业务状态机

```java
public boolean processOrderPayment(String orderId, PaymentInfo payment) {
    // 只有 PENDING_PAY 状态才能更新为 PAID
    int rows = orderMapper.updateStatusWithCondition(
        orderId, "PAID", payment, "PENDING_PAY");
    return rows > 0;  // rows == 0 → 已处理过
}
```

### 7.5 方案对比

| 方案 | 优点 | 缺点 | 适用 |
|------|------|------|------|
| Redis SET NX | 高性能，分布式 | 依赖 Redis | 通用场景（推荐）|
| 数据库唯一键 | 强一致 | 性能瓶颈 | DB 操作场景 |
| 业务状态机 | 零额外成本 | 业务耦合 | 订单状态变更 |

> 🎯 **幂等核心原则**：先校验再执行，防止重复操作。推荐 msgId + Redis + 唯一索引组合使用。

---

## 8. 配置管理

### 8.1 配置文件路径

| 部署方式 | 路径 |
|----------|------|
| Docker | `/etc/rabbitmq/rabbitmq.conf` |
| Linux | `/etc/rabbitmq/rabbitmq.conf` |
| Windows | `C:\Program Files\...\etc\rabbitmq.conf` |

### 8.2 核心配置项

```ini
listeners.tcp.default = 5672
management.tcp.port = 15672
loopback_users = none
connection_timeout = 10000
default_message_durability = true
disk_free_limit.absolute = 1024MB
connections.max = 200
channels.max = 2000
vm_memory_high_watermark.relative = 0.4
```

### 8.3 RabbitTemplate 全局配置

```java
@Configuration
public class RabbitTemplateConfig {

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setCreateMessageIds(true);
        template.setMessageConverter(converter);
        template.setConfirmCallback((cd, ack, cause) -> {
            if (!ack) log.error("发送确认失败: {}", cause);
        });
        template.setReturnsCallback(returned -> {
            log.warn("消息退回, exchange={}", returned.getExchange());
        });
        return template;
    }
}
```

### 8.4 生产级最佳实践总结

| 层级 | 实践 |
|------|------|
| **配置** | 多环境 profile 分离 + 手动确认 + 持久化 + 重连 |
| **生产者** | 消息重试 + Confirm 回调 + 唯一 msgId + 持久化 |
| **消费者** | 手动 ACK + 异常 NACK + 幂等校验 + 死信兜底 |
| **监控** | Web 界面监控队列/连接 + 日志 + 死信告警 |

> 💡 **核心口诀**：生产发交换，交换绑队列；直连精准配，主题模糊追；广播全推送，死信兜底幂等防重。
