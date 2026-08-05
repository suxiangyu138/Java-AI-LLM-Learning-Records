# 17-消息队列与Spring整合
> 🎯 P1 就业必备 — RabbitMQ/RocketMQ/Kafka 三大消息中间件的Spring Boot整合实战，覆盖消息可靠性、幂等性、延迟消息、事务消息、死信队列等核心生产场景

---

## 目录
1. [本章总览](#1-本章总览)
2. [分层理论讲解](#2-分层理论讲解)
   - 2.1 [MQ核心概念回顾](#21-mq核心概念回顾)
   - 2.2 [RabbitMQ与Spring整合](#22-rabbitmq与spring整合)
   - 2.3 [RocketMQ与Spring整合](#23-rocketmq与spring整合)
   - 2.4 [Kafka与Spring整合](#24-kafka与spring整合)
   - 2.5 [消息幂等性设计](#25-消息幂等性设计)
   - 2.6 [消息可靠性保障全链路](#26-消息可靠性保障全链路)
   - 2.7 [三大MQ综合对比](#27-三大mq综合对比)
3. [高频踩坑与误区](#3-高频踩坑与误区)
4. [随堂基础练习](#4-随堂基础练习)
5. [章节综合实操案例](#5-章节综合实操案例)
6. [分层综合习题](#6-分层综合习题)
7. [本章复盘速记清单](#7-本章复盘速记清单)
8. [精通拓展补充-P2](#8-精通拓展补充-p2)

---

## 1. 本章总览

### 1.1 知识定位
- **归属**：Spring全家桶与微服务 → 层级3 微服务配套中间件 P1 就业必备
- **前置依赖**：Spring Boot自动装配、RabbitMQ/RocketMQ/Kafka基础概念（建议先学03-RabbitMQ消息队列模块）
- **重要性**：⭐⭐⭐⭐⭐（微服务解耦、异步、削峰的核心基础设施，面试必考）

### 1.2 三层学习目标

| 级别 | 目标 |
|------|------|
| **基础** | 掌握三大MQ在Spring Boot中的基础配置与收发消息 |
| **熟练** | 独立配置死信队列、延迟消息、事务消息、消息确认机制 |
| **精通** | 设计全链路消息不丢失方案 + 幂等性兜底策略，解决生产级消息可靠性问题 |

### 1.3 本张涉及技术栈

```text
消息队列与Spring整合
├── RabbitMQ (详)
│   ├── Exchange: Direct / Fanout / Topic / Headers
│   ├── RabbitTemplate / @RabbitListener / @RabbitHandler
│   ├── 死信队列(DLX) / 延迟消息 / TTL
│   ├── Publisher Confirm / Return Callback
│   └── Consumer ACK / Prefetch / QOS
├── RocketMQ
│   ├── RocketMQTemplate / @RocketMQMessageListener
│   ├── 事务消息 / 顺序消息 / 延迟消息
│   └── MessageQueueSelector / MessageListenerOrderly
├── Kafka
│   ├── KafkaTemplate / @KafkaListener / ConsumerFactory
│   ├── Partition / Consumer Group / Offset
│   └── Replica / ISR
└── 可靠性体系
    ├── 幂等性: 唯一ID + Redis去重
    └── 全链路: Producer确认 → MQ持久化 → Consumer确认 → 死信兜底
```

---

## 2. 分层理论讲解

### 2.1 MQ核心概念回顾

#### 2.1.1 消息队列的本质

> 💡 消息队列（Message Queue）是一种**异步通信中间件**，核心作用是**解耦**、**削峰**、**异步**。

| 作用 | 说明 | 典型场景 |
|------|------|----------|
| **异步解耦** | 生产者无需等待消费者处理结果 | 订单创建 → 发送通知/积分/物流 |
| **流量削峰** | 将瞬时高并发请求缓冲到MQ，消费者按能力拉取 | 秒杀抢购、大促活动 |
| **数据分发** | 一份消息广播给多个消费者 | 订单状态同步到ES/Redis/BI等 |
| **日志收集** | 海量日志集中收集与分发 | 业务日志、访问日志采集 |

#### 2.1.2 RabbitMQ核心架构

```text
                      ┌──────────────┐
                      │   Producer   │
                      └──────┬───────┘
                             │ Publish
                             ▼
                      ┌──────────────┐
                      │   Exchange   │  ← 交换机类型: Direct/Fanout/Topic/Headers
                      └──────┬───────┘
                             │ Binding (RoutingKey)
                             ▼
                      ┌──────────────┐
                      │    Queue     │  ← 消息存储
                      └──────┬───────┘
                             │ Deliver
                             ▼
                      ┌──────────────┐
                      │   Consumer   │
                      └──────────────┘
```

| RabbitMQ核心概念 | 说明 |
|-----------------|------|
| **Exchange（交换机）** | 消息路由中心，根据RoutingKey将消息分发到队列 |
| **Direct Exchange** | 精确匹配：RoutingKey完全一致才路由 |
| **Fanout Exchange** | 广播模式：忽略RoutingKey，发送到所有绑定的队列 |
| **Topic Exchange** | 模糊匹配：支持通配符 `*`（匹配一个词）和 `#`（匹配零个或多个词） |
| **Headers Exchange** | 根据消息Headers属性匹配，很少使用 |
| **Queue（队列）** | 消息存储的容器，消费者从中拉取消息 |
| **Binding（绑定）** | 将Exchange与Queue关联，并指定RoutingKey |
| **RoutingKey（路由键）** | 消息的路由标识 |
| **vhost（虚拟主机）** | 多租户隔离，每个vhost拥有独立的Exchange/Queue/Binding |

#### 2.1.3 RocketMQ核心架构

| RocketMQ核心概念 | 说明 |
|-----------------|------|
| **NameServer** | 无状态路由注册中心，Broker启动时注册Topic路由信息 |
| **Broker** | 消息存储服务器，支持Master-Slave主从同步 |
| **Producer** | 消息生产者，通过轮询NameServer获取Broker地址 |
| **Consumer** | 消息消费者，分为Push模式和Pull模式 |
| **Topic** | 消息主题，逻辑分类 |
| **Tag** | 消息二级分类，用于Consumer订阅过滤 |
| **MessageQueue** | Topic下的物理分区，每个MessageQueue对应一个Broker上的文件 |

```text
                   ┌─────────────────┐
                   │   NameServer    │  (多个，无状态)
                   └────────┬────────┘
                            │ 路由注册/发现
         ┌──────────────────┼──────────────────┐
         │                  │                  │
    ┌────▼────┐       ┌────▼────┐       ┌────▼────┐
    │Broker-1 │       │Broker-2 │       │Broker-3 │
    │ Master  │       │ Master  │       │ Master  │
    │  └Slave │       │  └Slave │       │  └Slave │
    └────┬────┘       └────┬────┘       └────┬────┘
         │                  │                  │
    ┌────▼────┐       ┌────▼────┐       ┌────▼────┐
    │Producer │       │Producer │       │Consumer │
    └─────────┘       └─────────┘       └─────────┘
```

#### 2.1.4 Kafka核心架构

| Kafka核心概念 | 说明 |
|--------------|------|
| **Topic（主题）** | 消息的逻辑分类 |
| **Partition（分区）** | Topic的物理分片，每个Partition内部有序 |
| **Consumer Group（消费组）** | 一个Group内的Consumer共同消费Topic，每个Partition只能被同一Group内的一个Consumer消费 |
| **Offset（偏移量）** | Partition内每条消息的顺序ID，Consumer通过Offset记录消费位置 |
| **Replica（副本）** | Partition的副本，Leader处理读写，Follower同步 |
| **ISR（In-Sync Replica）** | 与Leader保持同步的副本集合 |

---

### 2.2 RabbitMQ与Spring整合

#### 2.2.1 引入依赖

**pom.xml：**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

Spring Boot自动依赖管理已包含版本控制，无需指定版本。

#### 2.2.2 基础配置

**application.yml：**

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    virtual-host: /
    username: guest
    password: guest
    # ---- 生产者确认 ----
    publisher-confirm-type: correlated   # 启用发布者确认(ConfirmCallback)
    publisher-returns: true              # 启用消息回退(ReturnsCallback)
    template:
      mandatory: true                   # 消息无法路由时触发Return回调
    # ---- 消费者 ----
    listener:
      simple:
        acknowledge-mode: manual          # 手动ACK
        prefetch: 1                      # 每次预取1条
        retry:
          enabled: true                  # 消费重试
          max-attempts: 3
          initial-interval: 1000ms
```

#### 2.2.3 完整RabbitMQ配置类

```java
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    /** ============ 交换机定义 ============ */

    // 订单交换机 (Direct)
    @Bean
    public DirectExchange orderExchange() {
        return ExchangeBuilder.directExchange("order.exchange")
                .durable(true)
                .build();
    }

    // 通知交换机 (Topic)
    @Bean
    public TopicExchange notificationExchange() {
        return ExchangeBuilder.topicExchange("notification.exchange")
                .durable(true)
                .build();
    }

    // 死信交换机 (Direct)
    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange("dlx.exchange")
                .durable(true)
                .build();
    }

    // 延迟交换机 (插件方式)
    @Bean
    public CustomExchange delayedExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange("delay.exchange", "x-delayed-message", true, false, args);
    }

    /** ============ 队列定义 ============ */

    // 订单队列（绑定死信）
    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable("order.queue")
                .deadLetterExchange("dlx.exchange")
                .deadLetterRoutingKey("order.dead")
                .ttl(30000)  // 30秒未消费进入死信
                .maxLength(10000)
                .build();
    }

    // 通知队列
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable("notification.queue")
                .build();
    }

    // 死信队列
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("dlx.queue")
                .build();
    }

    /** ============ 绑定关系 ============ */

    @Bean
    public Binding orderBinding() {
        return BindingBuilder.bind(orderQueue())
                .to(orderExchange())
                .with("order.created");
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue())
                .to(notificationExchange())
                .with("notification.#");
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("order.dead");
    }

    /** ============ 消息转换器 ============ */

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /** ============ RabbitTemplate ============ */

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                          MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);

        // 发布者确认回调
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.info("消息发送成功: {}", correlationData.getId());
            } else {
                log.error("消息发送失败: {}, cause: {}", correlationData.getId(), cause);
                // 落库记录失败消息，定时任务补偿
            }
        });

        // 消息无法路由回调
        template.setReturnsCallback(returned -> {
            log.warn("消息路由失败: exchange={}, routingKey={}, replyCode={}, replyText={}",
                    returned.getExchange(), returned.getRoutingKey(),
                    returned.getReplyCode(), returned.getReplyText());
            // 记录无法路由的消息
        });

        return template;
    }

    /** ============ 错误处理（重试耗尽后投递到死信） ============ */

    @Bean
    public MessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate) {
        // 消费重试耗尽后，将消息重新发布到指定的错误交换机
        return new RepublishMessageRecoverer(rabbitTemplate, "dlx.exchange", "order.dead");
    }

    @Bean
    public RabbitListenerErrorHandler rabbitListenerErrorHandler() {
        return (amqpMessage, message, exception) -> {
            log.error("消息消费异常: {}", exception.getMessage());
            throw exception;  // 继续抛出，触发重试机制
        };
    }
}
```

#### 2.2.4 RabbitTemplate 消息发送

```java
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送订单创建事件
     */
    public void sendOrderCreated(OrderCreatedEvent event) {
        String messageId = UUID.randomUUID().toString();
        CorrelationData cd = new CorrelationData(messageId);

        rabbitTemplate.convertAndSend(
                "order.exchange",
                "order.created",
                event,
                cd
        );

        log.info("订单事件已发送: orderId={}, messageId={}", event.getOrderId(), messageId);
    }

    /**
     * 发送延迟消息（需要延迟插件）
     */
    public void sendDelayedMessage(Object message, long delayMillis) {
        rabbitTemplate.convertAndSend(
                "delay.exchange",
                "delay.routing",
                message,
                msg -> {
                    msg.getMessageProperties().setDelayLong(delayMillis);
                    return msg;
                }
        );
    }

    /**
     * 发送通知消息
     */
    public void sendNotification(String routingKey, Object notification) {
        rabbitTemplate.convertAndSend(
                "notification.exchange",
                routingKey,
                notification
        );
    }
}
```

#### 2.2.5 @RabbitListener 消息消费

```java
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationConsumer {

    /**
     * 单条消息消费（手动ACK）
     */
    @RabbitListener(queues = "order.queue")
    public void handleOrderCreated(OrderCreatedEvent event, Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            log.info("收到订单事件: orderId={}, userId={}", event.getOrderId(), event.getUserId());

            // 发送短信通知
            smsService.send(event.getUserPhone(), "您的订单 " + event.getOrderId() + " 已创建");

            // 发送邮件通知
            emailService.send(event.getUserEmail(), "订单确认", "订单 " + event.getOrderId() + " 已创建");

            // 手动ACK
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("订单通知处理失败: orderId={}", event.getOrderId(), e);
            // 拒绝消息，不重新入队（进入死信队列）
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 死信队列消息消费（人工处理或补偿）
     */
    @RabbitListener(queues = "dlx.queue")
    public void handleDeadLetter(OrderCreatedEvent event, Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            log.warn("处理死信消息: orderId={}, 重试发送通知", event.getOrderId());

            // 再次尝试发送通知
            notificationService.retryNotification(event);

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("死信处理也失败，记录到数据库人工处理: orderId={}", event.getOrderId());
            // 记录到异常表
            deadLetterRecordService.save(event, e.getMessage());
            channel.basicAck(deliveryTag, false); // 确认移除，避免循环死信
        }
    }
}
```

#### 2.2.6 @RabbitHandler 多类型分发

```java
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

@RabbitListener(queues = "order.queue")
public class OrderMultiHandler {

    @RabbitHandler
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("处理订单创建事件: {}", event.getOrderId());
    }

    @RabbitHandler
    public void handleOrderPaid(OrderPaidEvent event) {
        log.info("处理订单支付事件: {}", event.getOrderId());
    }

    @RabbitHandler
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("处理订单取消事件: {}", event.getOrderId());
    }

    // 兜底处理未知类型
    @RabbitHandler(isDefault = true)
    public void handleDefault(Object object) {
        log.warn("未知消息类型: {}", object.getClass().getName());
    }
}
```

#### 2.2.7 死信队列（DLX）深度解析

> ⚠️ **死信队列是消息可靠性保障的最后一道防线**，理解DLX对生产环境至关重要。

**什么是死信队列（Dead Letter Exchange）？**

当消息在队列中出现以下三种情况之一时，会变成**死信（Dead Letter）**，如果队列配置了 `deadLetterExchange`，消息会被重新投递到指定的死信交换机：

| 死信场景 | 说明 | 配置 |
|---------|------|------|
| **消息被拒绝** | 消费者 `basicNack` / `basicReject` 且 `requeue=false` | `channel.basicNack(delTag, false, false)` |
| **消息过期** | 消息超过TTL（Time To Live）未被消费 | `x-message-ttl` 或 `expiration` 属性 |
| **队列达到最大长度** | 队列中消息数量超过 `max-length` | `x-max-length` 或 `x-max-length-bytes` |

**死信消息原属性保留：** 死信消息会保留原始消息内容，并在header中添加以下属性：

```text
x-first-death-exchange    = 原交换机
x-first-death-queue       = 原队列
x-first-death-reason      = rejected / expired / maxlen
x-death                   = 死亡历史列表
```

**延迟消息实现方案对比：**

| 方案 | 原理 | 优点 | 缺点 |
|-----|------|------|------|
| **TTL + DLX** | 消息设置TTL，过期后进入死信队列 | 无需额外插件 | TTL不精确（队列级TTL有阻塞问题） |
| **延迟插件** | 安装 `rabbitmq_delayed_message_exchange` 插件 | 精确延迟、灵活 | 需要额外安装插件 |
| **官方推荐** | TTL + DLX 适合简单场景，复杂场景用插件 | — | — |

> 💡 **典型应用**：订单超时未支付自动取消。创建订单时发送一条延迟30分钟的消息，30分钟后消费者检查订单状态，如未支付则自动取消。

```java
// 订单超时取消 — 基于TTL+DLX方案

// 1. 定义订单延迟队列（30分钟TTL，到期进入死信）
@Bean
public Queue orderDelayQueue() {
    return QueueBuilder.durable("order.delay.queue")
            .deadLetterExchange("order.exchange")          // 到期投递到订单交换机
            .deadLetterRoutingKey("order.timeout.cancel")   // 使用超时取消路由键
            .ttl(30 * 60 * 1000)  // 30分钟
            .build();
}

// 2. 消费者监听超时取消消息
@RabbitListener(queues = "order.cancel.queue")
public void handleOrderTimeout(OrderTimeoutEvent event) {
    Order order = orderService.getById(event.getOrderId());
    if (order != null && order.getStatus() == OrderStatus.UNPAID) {
        orderService.cancel(order.getId(), "订单超时未支付，系统自动取消");
        log.info("订单超时自动取消: orderId={}", order.getId());
    }
}
```

#### 2.2.8 消息确认机制

| 确认层级 | 机制 | 说明 |
|---------|------|------|
| **生产者 → Broker** | **Publisher Confirm** | 消息到达交换机后，Broker异步回调确认 |
| **生产者 → 队列** | **Publisher Return** | 消息无法路由到队列时触发回调 |
| **消费者 → Broker** | **Consumer ACK** | 消费者处理完成后通知Broker删除消息 |

**Publisher Confirm 三种模式：**

```yaml
# application.yml 配置
spring:
  rabbitmq:
    # 三种publisher-confirm-type:
    #   none       — 不启用（默认）
    #   correlated — 异步回调（推荐）
    #   simple     — 同步等待
    publisher-confirm-type: correlated
    publisher-returns: true
    template:
      mandatory: true
```

**Consumer ACK 三种模式：**

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        acknowledge-mode: manual   # NONE / AUTO / MANUAL
```

| AcknowledgeMode | 说明 | 适用场景 |
|----------------|------|----------|
| **NONE** | 自动ACK，消息投递后立即标记为已消费 | 允许少量丢失、性能优先的场景 |
| **AUTO** | 自动ACK，方法无异常即确认，抛出异常则重试或进入死信 | 大多数业务场景 |
| **MANUAL** | 手动ACK，由业务代码控制`basicAck`/`basicNack` | 对消息可靠性要求严格的场景 |

**生产者确认回调完整实现：**

```java
@Slf4j
@Component
public class RabbitMQConfirmService implements RabbitTemplate.ConfirmCallback,
                                               RabbitTemplate.ReturnsCallback {

    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        String messageId = correlationData != null ? correlationData.getId() : "unknown";
        if (ack) {
            log.info("消息确认成功: messageId={}", messageId);
            // 更新消息状态为已确认
            messageLogService.updateStatus(messageId, MessageStatus.CONFIRMED);
        } else {
            log.error("消息确认失败: messageId={}, cause={}", messageId, cause);
            // 记录失败，定时任务补偿
            messageLogService.updateStatus(messageId, MessageStatus.FAILED);
        }
    }

    @Override
    public void returnedMessage(ReturnedMessage returned) {
        log.warn("消息路由失败: exchange={}, routingKey={}, text={}",
                returned.getExchange(), returned.getRoutingKey(),
                returned.getReplyText());
        // 记录无法路由的消息
    }
}
```

#### 2.2.9 消费者手动ACK

```java
@RabbitListener(queues = "order.queue")
public void onMessage(OrderCreatedEvent event, Message message, Channel channel) {
    long deliveryTag = message.getMessageProperties().getDeliveryTag();
    try {
        // 业务处理
        process(event);

        // ---- 手动确认 ----
        channel.basicAck(deliveryTag, false);
        // basicAck参数: deliveryTag, multiple(是否批量确认)
    } catch (BusinessException e) {
        // ---- 业务异常：拒绝，不重新入队，进入死信 ----
        channel.basicNack(deliveryTag, false, false);
        // basicNack参数: deliveryTag, multiple, requeue
    } catch (RetryableException e) {
        // ---- 可重试异常：拒绝，重新入队 ----
        channel.basicNack(deliveryTag, false, true);
    } catch (Exception e) {
        // ---- 未知异常：拒绝，不重新入队 ----
        channel.basicNack(deliveryTag, false, false);
    }
}
```

#### 2.2.10 Prefetch 预取配置

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        prefetch: 1             # 每次预取1条（处理完再取下一条）
        concurrency: 3          # 并发消费者数
        max-concurrency: 10     # 最大并发消费者数
```

**basicQos 配置详解：**

```java
@Bean
public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
        ConnectionFactory connectionFactory) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setMessageConverter(new Jackson2JsonMessageConverter());

    // ---- 关键配置 ----
    factory.setPrefetchCount(1);         // 每次预取1条（公平分发）
    factory.setConcurrentConsumers(3);   // 初始消费者数
    factory.setMaxConcurrentConsumers(10);
    factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);

    // 重试配置
    factory.setAdviceChain(RetryInterceptorBuilder.stateless()
            .maxAttempts(3)
            .backOffOptions(1000, 2.0, 10000) // 初始间隔、倍数、最大间隔
            .build());

    return factory;
}
```

> 💡 **Prefetch 调优原则**：`prefetch=1` 适用于任务耗时不均的场景，可避免某个消费者积压大量消息导致其他消费者空闲；`prefetch>1` 可提升吞吐量，但会降低公平性。一般建议 `prefetch=(预期处理时间/网络延迟)*2` 作为起点。

---

### 2.3 RocketMQ与Spring整合

#### 2.3.1 引入依赖

```xml
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
    <version>2.2.3</version>
</dependency>
```

#### 2.3.2 基础配置

```yaml
rocketmq:
  name-server: 127.0.0.1:9876
  producer:
    group: order-producer-group
    send-message-timeout: 3000
    retry-times-when-send-failed: 3
  consumer:
    # 消费组配置在@RocketMQMessageListener中指定
```

#### 2.3.3 RocketMQTemplate 消息发送

```java
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class RocketMQOrderProducer {

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 发送普通消息
     */
    public void sendOrderCreated(OrderCreatedEvent event) {
        // topic:tag 格式
        rocketMQTemplate.convertAndSend("order-topic:created", event);
    }

    /**
     * 发送同步消息
     */
    public SendResult sendSync(OrderCreatedEvent event) {
        Message<OrderCreatedEvent> msg = MessageBuilder.withPayload(event)
                .setHeader("KEYS", event.getOrderId())   // 业务Key，用于查询
                .build();
        return rocketMQTemplate.syncSend("order-topic:created", msg);
    }

    /**
     * 发送异步消息
     */
    public void sendAsync(OrderCreatedEvent event) {
        rocketMQTemplate.asyncSend("order-topic:created", event,
                new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        log.info("异步发送成功: {}", sendResult.getMsgId());
                    }

                    @Override
                    public void onException(Throwable e) {
                        log.error("异步发送失败", e);
                    }
                });
    }

    /**
     * 发送有序消息
     */
    public void sendOrderly(Long orderId, Object message) {
        rocketMQTemplate.syncSendOrderly(
                "order-topic:status",
                message,
                orderId.toString(),  // 基于orderId选择队列，保证同一订单有序
                timeout
        );
    }

    /**
     * 发送延迟消息
     */
    public void sendDelayed(OrderTimeoutEvent event) {
        // delayLevel: 1=1s, 2=5s, 3=10s, 4=30s, 5=1m, 6=2m, 7=3m, 8=4m, 9=5m, 10=6m,
        //             11=7m, 12=8m, 13=9m, 14=10m, 15=20m, 16=30m, 17=1h, 18=2h
        rocketMQTemplate.syncSend("order-topic:timeout",
                MessageBuilder.withPayload(event).build(),
                timeout,
                16  // delayLevel=16 对应30分钟
        );
    }
}
```

#### 2.3.4 @RocketMQMessageListener 消费

```java
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(
        topic = "order-topic",
        selectorExpression = "created",    // Tag过滤
        consumerGroup = "notification-consumer-group",
        consumeMode = ConsumeMode.CONCURRENTLY,   // CONCURRENTLY / ORDERLY
        messageModel = MessageModel.CLUSTERING     // CLUSTERING / BROADCASTING
)
public class RocketMQNotificationConsumer implements RocketMQListener<OrderCreatedEvent> {

    @Override
    public void onMessage(OrderCreatedEvent event) {
        log.info("收到订单通知: orderId={}", event.getOrderId());
        // 业务处理
        notificationService.send(event);
    }
}
```

> 💡 **RocketMQ 消费者两种模式：**
> - `RocketMQListener<T>`：实现 `onMessage` 方法，抛出异常即重试（默认重试16次）
> - `RocketMQPushConsumer`：原生API，更灵活的控制

#### 2.3.5 事务消息

> ⚠️ **事务消息是RocketMQ区别于其他MQ的核心特性**，用于保证本地事务与消息发送的一致性。

**事务消息流程：**

```text
1. Producer 发送 half message（半消息，对Consumer不可见）
2. Broker 持久化半消息，返回确认
3. Producer 执行本地事务
4. Producer 向 Broker 提交 Commit 或 Rollback
   ├── Commit → 消息对消费者可见
   └── Rollback → 消息被删除
5. 如果Producer未提交（网络超时等），Broker回调检查本地事务状态
   ├── COMMIT_MESSAGE → 提交
   └── ROLLBACK_MESSAGE → 回滚
         └── UNKNOWN → 继续回调
```

**实现步骤：**

```java
// 1. 自定义事务监听器
@Component
@RocketMQTransactionListener
public class OrderTransactionListener implements RocketMQLocalTransactionListener {

    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        // 步骤2：执行本地事务
        try {
            OrderCreatedEvent event = (OrderCreatedEvent) ((RocketMQLocalRequestMetaData) arg).getPayload();
            orderService.createOrder(event);  // 创建订单（本地事务）
            return RocketMQLocalTransactionState.COMMIT;  // 提交消息
        } catch (Exception e) {
            log.error("本地事务失败", e);
            return RocketMQLocalTransactionState.ROLLBACK; // 回滚消息
        }
    }

    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        // 步骤3：Broker回调检查本地事务状态
        String orderId = (String) msg.getHeaders().get("orderId");
        Order order = orderService.getById(orderId);
        if (order != null) {
            return RocketMQLocalTransactionState.COMMIT;
        }
        return RocketMQLocalTransactionState.ROLLBACK;
    }
}

// 2. 发送事务消息
@Component
public class TransactionProducer {

    private final RocketMQTemplate rocketMQTemplate;

    public void sendOrderTransaction(OrderCreatedEvent event) {
        Message<OrderCreatedEvent> msg = MessageBuilder
                .withPayload(event)
                .setHeader("orderId", event.getOrderId())
                .build();

        rocketMQTemplate.sendMessageInTransaction(
                "order-tx-topic:created",
                msg,
                event
        );
    }
}
```

| 事务消息状态 | 含义 |
|-------------|------|
| **COMMIT_MESSAGE** | 提交事务，消息对消费者可见 |
| **ROLLBACK_MESSAGE** | 回滚事务，消息被删除 |
| **UNKNOWN** | 不确定，Broker会再次回调检查 |

#### 2.3.6 顺序消息

> 💡 顺序消息保证同一业务ID的消息按发送顺序被消费。RocketMQ利用MessageQueue的FIFO特性实现。

**生产者 — 消息队列选择器：**

```java
// 使用syncSendOrderly自动选择队列
rocketMQTemplate.syncSendOrderly(
    "order-topic:status",
    message,
    event.getOrderId().toString(),  // HashKey: 相同orderId进入同一队列
    3000
);

// 或自定义MessageQueueSelector
rocketMQTemplate.syncSend("order-topic:status", message,
    new MessageQueueSelector() {
        @Override
        public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
            Long orderId = (Long) arg;
            int index = (int) (orderId % mqs.size());
            return mqs.get(index);
        }
    },
    event.getOrderId()
);
```

**消费者 — 有序消费：**

```java
@Component
@RocketMQMessageListener(
        topic = "order-topic",
        selectorExpression = "status",
        consumerGroup = "order-status-group",
        consumeMode = ConsumeMode.ORDERLY   // 关键：有序消费
)
public class OrderStatusConsumer implements RocketMQListener<OrderStatusEvent> {

    @Override
    public void onMessage(OrderStatusEvent event) {
        // 同一个orderId的消息会被同一个线程处理
        log.info("处理订单状态变更: orderId={}, status={}", event.getOrderId(), event.getStatus());
        orderService.updateStatus(event.getOrderId(), event.getStatus());
    }
}
```

#### 2.3.7 延迟消息

RocketMQ的延迟消息基于固定的18个延迟级别，不支持自定义时间：

| Level | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 13 | 14 | 15 | 16 | 17 | 18 |
|-------|---|---|---|---|---|---|---|---|---|----|----|----|----|----|----|----|----|----|
| 延迟  | 1s | 5s | 10s | 30s | 1m | 2m | 3m | 4m | 5m | 6m | 7m | 8m | 9m | 10m | 20m | 30m | 1h | 2h |

配置文件自定义延迟级别：

```yaml
rocketmq:
  producer:
    # 自定义延迟级别（需在Broker端配置 messageDelayLevel）
    message-delay-level: 1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
```

---

### 2.4 Kafka与Spring整合

#### 2.4.1 引入依赖

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

#### 2.4.2 基础配置

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    # ---- 生产者 ----
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all                    # 0 / 1 / all（all=Leader+所有ISR都确认）
      retries: 3
      compression-type: snappy     # 压缩: none/gzip/snappy/lz4/zstd
      batch-size: 16384            # 16KB 批量发送
      linger-ms: 5                 # 最多等待5ms凑够batch-size
    # ---- 消费者 ----
    consumer:
      group-id: notification-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"   # 信任所有包的反序列化
      enable-auto-commit: false              # 手动提交offset
      auto-offset-reset: earliest            # earliest/latest/none
      max-poll-records: 50                   # 一次poll最大条数
    # ---- 监听器 ----
    listener:
      ack-mode: manual_immediate      # 手动确认模式
```

#### 2.4.3 KafkaTemplate 消息发送

```java
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Component
public class KafkaOrderProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 发送消息（异步）
     */
    public void send(String topic, String key, Object message) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, key, message);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Kafka发送成功: topic={}, partition={}, offset={}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Kafka发送失败", ex);
            }
        });
    }

    /**
     * 发送到指定分区
     */
    public void sendToPartition(String topic, int partition, String key, Object message) {
        MessageRecord<Object> record = new ProducerRecord<>(topic, partition, key, message);
        kafkaTemplate.send(record);
    }
}
```

#### 2.4.4 @KafkaListener 消息消费

```java
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class KafkaNotificationConsumer {

    /**
     * 单一Topic消费
     */
    @KafkaListener(
            topics = "order-topic",
            groupId = "notification-group",
            concurrency = "3"          // 并发消费者数（不超过分区数）
    )
    public void onOrderEvent(OrderCreatedEvent event,
                              ConsumerRecord<String, OrderCreatedEvent> record,
                              Acknowledgment ack) {
        try {
            log.info("收到Kafka消息: key={}, partition={}, offset={}",
                    record.key(), record.partition(), record.offset());

            notificationService.send(event);

            // 手动提交offset
            ack.acknowledge();
        } catch (Exception e) {
            log.error("消费失败", e);
            // Kafka不会重试单条消息，通过error handler处理
            throw e;
        }
    }

    /**
     * 多Topic消费
     */
    @KafkaListener(topicPattern = "order-.*")
    public void onOrderPattern(String message, Acknowledgment ack) {
        log.info("匹配到order-前缀topic: {}", message);
        ack.acknowledge();
    }

    /**
     * 批量消费
     */
    @KafkaListener(
            topics = "order-topic",
            batch = "true"               // 批量模式
    )
    public void onBatch(List<OrderCreatedEvent> events, Acknowledgment ack) {
        log.info("批量消费: size={}", events.size());
        events.forEach(event -> process(event));
        ack.acknowledge();
    }
}
```

#### 2.4.5 ConsumerFactory 自定义配置

```java
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "default-group");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 50);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, Object>>
            kafkaListenerContainerFactory(ConsumerFactory<String, Object> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(3);                    // 并发数
        factory.setBatchListener(false);              // 是否批量消费
        factory.getContainerProperties()
                .setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE); // 手动确认
        factory.getContainerProperties()
                .setPollTimeout(3000);                // poll超时时间

        // 错误处理
        factory.setCommonErrorHandler(new DefaultErrorHandler(
                new FixedBackOff(1000L, 3)            // 重试3次，间隔1秒
        ));

        return factory;
    }
}
```

#### 2.4.6 Kafka关键概念速查

| 概念 | 说明 | 重要性 |
|------|------|--------|
| **Partition（分区）** | Topic的物理分片，分区内消息有序 | 分区数决定最大并发度 |
| **Consumer Group（消费组）** | 组内消费者共同消费Topic，每个分区只能被组内一个消费者消费 | 实现负载均衡 |
| **Offset（偏移量）** | 消费者消费位置，由消费者或Broker管理 | 确保消息不重复消费 |
| **Replica（副本）** | 分区副本，Leader提供读写，Follower同步 | 保证数据不丢失 |
| **ISR（同步副本集合）** | 与Leader保持同步的Follower副本集合 | 保证数据一致性 |

---

### 2.5 消息幂等性设计

#### 2.5.1 为什么需要幂等性

> ⚠️ **MQ天然存在重复消息**：生产者重试、消费者重试、网络故障等都会导致消息重复。**消费端必须做幂等处理**，即同一条消息处理多次与处理一次的结果相同。

#### 2.5.2 幂等性实现方案

**方案一：唯一ID + Redis去重（推荐）**

```java
@Component
public class IdempotentConsumer {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String IDEMPOTENT_KEY_PREFIX = "mq:idempotent:";
    private static final long IDEMPOTENT_EXPIRE_HOURS = 24;

    /**
     * 消息幂等处理模板
     */
    public boolean tryProcess(String messageId, String businessKey, Runnable processor) {
        String key = IDEMPOTENT_KEY_PREFIX + messageId;

        // SET NX: 如果key不存在则写入（原子操作）
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, businessKey,
                        IDEMPOTENT_EXPIRE_HOURS, TimeUnit.HOURS);

        if (Boolean.TRUE.equals(success)) {
            // 首次消费
            try {
                processor.run();
                return true;
            } catch (Exception e) {
                // 处理失败，删除幂等Key（允许重试）
                redisTemplate.delete(key);
                throw e;
            }
        } else {
            // 重复消息，跳过
            log.info("重复消息已跳过: messageId={}", messageId);
            return false;
        }
    }
}
```

**方案二：数据库唯一索引**

```java
// 消费日志表 DDL
CREATE TABLE `message_consume_log` (
    `message_id` VARCHAR(64) NOT NULL COMMENT '消息唯一ID',
    `business_key` VARCHAR(128) NOT NULL COMMENT '业务Key',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0=处理中, 1=已完成',
    `create_time` DATETIME NOT NULL,
    PRIMARY KEY (`message_id`),
    UNIQUE KEY `uk_message_id` (`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

// 消费时基于唯一索引防重
@Transactional
public void consumeWithIdempotent(String messageId, OrderCreatedEvent event) {
    // insert ignore 如果messageId已存在则忽略
    int count = messageConsumeLogDao.insertIgnore(messageId, event.getOrderId());
    if (count == 0) {
        log.info("重复消息已跳过: messageId={}", messageId);
        return;
    }
    // 执行业务逻辑
    orderService.createOrder(event);
    // 更新状态
    messageConsumeLogDao.updateStatus(messageId, 1);
}
```

**方案三：业务本身幂等（乐观锁）**

```java
// 利用数据库乐观锁实现幂等
@Transactional
public void processOrderPaid(String orderId) {
    // UPDATE order SET status = 'PAID', version = version + 1
    // WHERE order_id = ? AND status = 'UNPAID'
    // 如果影响行数为0，说明已支付（重复消息）
    int rows = orderDao.updateStatusByVersion(orderId, "PAID", "UNPAID");
    if (rows == 0) {
        log.info("订单已支付，跳过重复消息: orderId={}", orderId);
    }
}
```

| 方案 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| **Redis去重** | 高性能，支持自动过期 | Redis宕机可能失效 | 高并发、允许少量重复 |
| **DB唯一索引** | 绝对可靠 | 性能低于Redis | 金融、交易类（不允许重复） |
| **业务幂等** | 无需额外存储 | 需要业务支持乐观锁 | 状态流转类业务 |

> 💡 **生产推荐组合**：Redis去重（高性能兜底） + DB唯一索引（最终一致性保障），两条防线确保不会因重复消息造成数据错误。

---

### 2.6 消息可靠性保障全链路

#### 2.6.1 消息丢失全景图

```text
┌────────────────────────────────────────────────────────────────────────┐
│                      消息丢失全景图                                      │
├──────────────┬─────────────────┬──────────────────┬───────────────────┤
│   生产阶段    │    MQ存储阶段    │    消费阶段        │    补救措施       │
├──────────────┼─────────────────┼──────────────────┼───────────────────┤
│ 网络超时丢失  │ MQ宕机未持久化   │ 自动ACK失败       │ 生产者重试         │
│ 路由不到队列  │ 刷盘失败         │ 业务异常未处理     │ 死信队列兜底       │
│ 未开启Confirm│ 主从同步延迟     │ NACK处理不当      │ 定时补偿任务       │
└──────────────┴─────────────────┴──────────────────┴───────────────────┘
```

#### 2.6.2 生产者保障 — Confirm + 重试

```java
@Component
public class ReliableProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private MessageLogService messageLogService;

    /**
     * 可靠发送 — 落库 + 发送 + 回调更新状态
     */
    public void reliableSend(OrderCreatedEvent event) {
        // 1. 消息落库（状态=0:待发送）
        String messageId = UUID.randomUUID().toString();
        messageLogService.save(messageId, event);

        // 2. 发送消息
        CorrelationData cd = new CorrelationData(messageId);
        rabbitTemplate.convertAndSend("order.exchange", "order.created", event, cd);

        // 3. ConfirmCallback中更新状态为已确认(状态=1)
        // 4. 定时任务扫描状态=0且超过30秒的消息，重新发送
    }
}

// 定时补偿 — 扫描未确认的消息
@Scheduled(fixedRate = 30000)
public void compensateUnconfirmedMessages() {
    List<MessageLog> unconfirmed = messageLogService
            .getUnconfirmedMessages(Duration.ofSeconds(30));

    for (MessageLog msg : unconfirmed) {
        if (msg.getRetryCount() >= 5) {
            messageLogService.updateStatus(msg.getId(), MessageStatus.FAILED);
            log.error("消息已达最大重试次数: messageId={}", msg.getId());
            continue;
        }
        // 重新发送
        rabbitTemplate.convertAndSend(
                msg.getExchange(), msg.getRoutingKey(), msg.getPayload(),
                new CorrelationData(msg.getId()));
        messageLogService.incrementRetry(msg.getId());
    }
}
```

#### 2.6.3 消费者保障 — 手动ACK + 重试 + 死信

```text
消费流程：
接收消息 → 幂等校验(Redis) → 执行业务逻辑 → 手动ACK → 完成
                                       ↓ (异常)
                             判断异常类型 ─┬─ 可重试 → NACK(requeue=true)
                                          └─ 不可重试 → NACK(requeue=false) → 死信队列
```

#### 2.6.4 全链路可靠性配置参考

**RabbitMQ端到端可靠性配置：**

```yaml
spring:
  rabbitmq:
    publisher-confirm-type: correlated
    publisher-returns: true
    template:
      mandatory: true
    listener:
      simple:
        acknowledge-mode: manual
        prefetch: 1
        retry:
          enabled: true
          max-attempts: 3
          initial-interval: 1000ms
          multiplier: 2.0
          max-interval: 10000ms
```

```java
// 队列：死信交换机 + 最大长度 + TTL
@Bean
public Queue reliableQueue() {
    return QueueBuilder.durable("business.queue")
            .deadLetterExchange("dlx.exchange")
            .deadLetterRoutingKey("business.dead")
            .maxLength(50000)
            .build();
}
```

#### 2.6.5 三条防线总结

| 防线 | 手段 | 兜底措施 |
|------|------|----------|
| **第一道：生产者保证消息到MQ** | Confirm机制 + 重试 + 消息落库 | 定时任务扫描补偿发送 |
| **第二道：MQ保证消息不丢** | 持久化 + 主从同步(镜像队列/ISR) | 集群部署 |
| **第三道：消费者保证消息必达** | 手动ACK + 幂等 + 重试 + 死信 | 死信队列人工处理 |

> 🎯 **可靠性设计的核心思想**：每条消息的生命周期都要可追踪、可追溯、可补偿。关键路径上的消息必须有唯一的消息ID贯穿始终。

---

### 2.7 三大MQ综合对比

| 对比维度 | RabbitMQ | RocketMQ | Kafka |
|---------|----------|----------|-------|
| **开发语言** | Erlang | Java | Scala/Java |
| **协议/API** | AMQP 0-9-1 | 自研协议 | 自研TCP协议 |
| **部署模式** | 单机/普通集群/镜像集群 | NameServer + Broker(Master-Slave) | ZooKeeper/KRaft + Broker |
| **消息模型** | Exchange → Queue | Topic → MessageQueue | Topic → Partition |
| **吞吐量** | 万级/秒 | 十万级/秒 | 百万级/秒 |
| **延迟** | 微秒级 | 毫秒级 | 毫秒级 |
| **消息可靠性** | 高（Confirm + ACK + 镜像队列） | 高（同步刷盘 + 主从同步） | 高（ISR副本 + acks=all） |
| **消息有序性** | 单队列有序 | 分区内有序（MessageQueue） | 分区内有序（Partition） |
| **事务消息** | 不支持（有TCC代替方案） | **支持（半消息+回调检查）** | 不支持（Kafka EOS仅Exactly-Once） |
| **延迟消息** | 插件/TTL+DLX | **固定18级延迟** | 不支持（需第三方） |
| **死信队列** | **原生支持（DLX）** | 支持（消费重试超限进入） | 不支持（需自建） |
| **消息过滤** | Exchange Binding + Headers | **Tag过滤** + SQL表达式 | 服务端不支持，客户端过滤 |
| **消息回溯** | 不支持 | 支持按时间回溯 | 支持按Offset/时间回溯 |
| **客户端语言** | 多语言 | Java为主 | 多语言 |
| **运维复杂度** | 低（管理控制台完善） | 中（需部署NameServer） | 高（需ZooKeeper/KRaft） |
| **学习曲线** | 低 | 中 | 中 |
| **社区活跃度** | 非常活跃 | 活跃 | 非常活跃 |
| **典型场景** | 业务解耦、异步通知、可靠投递 | 电商交易、金融支付、事务消息 | 日志采集、大数据管道、实时计算 |
| **Spring Boot集成** | spring-boot-starter-amqp | rocketmq-spring-boot-starter | spring-kafka |
| **Spring Cloud Stream** | 支持 | 支持 | 支持 |
| **适用规模** | 中小型到大型 | 中大型（阿里核心场景验证） | 大型到超大型 |

> 🎯 **选型建议**：
> - **中小型业务 + 对延迟敏感 + 需要死信队列** → RabbitMQ
> - **电商/金融 + 需要事务消息 + 需要顺序消息** → RocketMQ
> - **海量日志/大数据 + 超高吞吐 + 流式计算** → Kafka

---

## 3. 高频踩坑与误区

### 3.1 RabbitMQ常见坑

| 坑点 | 表现 | 原因 | 解决方案 |
|------|------|------|----------|
| **消息莫名其妙丢失** | Confirm成功，但消费者收不到 | 未开启持久化 + 自动ACK + 队列未设置durable | 队列/消息持久化 + 手动ACK |
| **消息重复消费** | 同一条消息处理两次 | 生产者重试 + 自动ACK + 消费者重启 | 消费端幂等处理 |
| **@RabbitListener不生效** | 启动报错 `Listener method failed` | 未加 `@EnableRabbit` 或扫描路径不对 | 在配置类上加 `@EnableRabbit` |
| **死信不投递** | 消息到达TTL但没进入死信队列 | 死信交换机和队列未正确绑定 | 检查routingKey是否匹配 |
| **消息累积不消费** | 队列消息堆积 | Prefetch设置过大，消费者处理慢 | 降低prefetch + 增加消费者并发 |
| **ConfirmCallback不回调** | 发送后无确认回调 | `publisher-confirm-type` 未配置 | 配置 `publisher-confirm-type: correlated` |
| **Channel被关闭** | 大量日志 `Channel closed` | 消费者处理太慢，超时被Broker断开 | 调整心跳超时 + 增加消费者线程 |

### 3.2 RabbitMQ典型错误示例

```java
// ❌ 错误：自动ACK + 未处理异常，消息丢失
@RabbitListener(queues = "order.queue")
public void onMessage(OrderCreatedEvent event) {  // 默认AUTO，抛异常消息被丢弃
    orderService.create(event);  // 如果抛异常，消费者已自动ACK，消息丢失！
}

// ✅ 正确：手动ACK + 异常处理
@RabbitListener(queues = "order.queue")
public void onMessage(OrderCreatedEvent event, Channel channel,
                      @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
    try {
        orderService.create(event);
        channel.basicAck(tag, false);
    } catch (Exception e) {
        channel.basicNack(tag, false, false);  // 进入死信
    }
}
```

### 3.3 RocketMQ常见坑

| 坑点 | 原因 | 解决方案 |
|------|------|----------|
| **事务消息一直处于半消息状态** | `checkLocalTransaction` 返回UNKNOWN未处理 | 正确实现事务检查逻辑，避免无限回调 |
| **顺序消息乱序** | 使用了 `ConsumeMode.CONCURRENTLY` | 使用 `ConsumeMode.ORDERLY` |
| **消息消费失败不重试** | 自定义 `RocketMQListener` 实现不抛异常 | 消费失败必须抛出异常触发重试 |
| **Topic不存在自动创建失败** | 未配置 `autoCreateTopicEnable` | 生产环境预创建Topic |

### 3.4 Kafka常见坑

| 坑点 | 原因 | 解决方案 |
|------|------|----------|
| **消费位移提交失败导致重复消费** | 自动提交offset + 处理超时 | 改为手动提交 + 合理设置 `max.poll.interval.ms` |
| **消费者被踢出消费组** | 处理耗时超过 `max.poll.interval.ms` | 调整超时参数或减少 `max.poll.records` |
| **消息序列化失败** | JSON反序列化未配置信任包 | 设置 `spring.json.trusted.packages=*` |
| **分区分配不均衡** | 分区数不是消费者数的整数倍 | 合理设置分区数和消费者数 |
| **@KafkaListener Topic不存在** | 未配置 `auto.create.topics.enable` | 预创建Topic或启用自动创建 |

### 3.5 MQ通用误区

| 误区 | 正解 |
|------|------|
| 用了MQ就一定能解耦 | MQ解耦的前提是消息格式稳定，频繁变更的消息格式会导致所有消费者需要同步修改 |
| MQ能保证消息100%不丢 | 没有任何系统能保证100%，但通过Confirm+ACK+死信+补偿可以做到99.9999% |
| 异步MQ一定比同步调用快 | 异步增加吞吐量，但增加了一条消息投递的延迟，并且需要处理最终一致性 |
| 消费者数量越多越好 | 消费者数受限于队列/分区数，超出部分的消费者会闲置 |
| 消息队列里的消息越多越好 | 队列堆积过多会导致延迟增大，甚至OOM，应设置最大长度限制 |

---

## 4. 随堂基础练习

### 练习一：RabbitMQ基础配置

配置一个Spring Boot项目，实现以下功能：

1. 创建一个Direct交换机 `test.exchange` 和一个队列 `test.queue`
2. 使用RoutingKey `test.key` 绑定交换机到队列
3. 通过 `RabbitTemplate` 发送一条字符串消息
4. 使用 `@RabbitListener` 消费该消息并打印日志

**参考代码框架：**

```java
// 1. 配置类
@Configuration
public class TestConfig {
    @Bean public DirectExchange testExchange() {
        return new DirectExchange("test.exchange");
    }
    @Bean public Queue testQueue() {
        return new Queue("test.queue", true);
    }
    @Bean public Binding testBinding() {
        return BindingBuilder.bind(testQueue()).to(testExchange()).with("test.key");
    }
}

// 2. 生产者
@Component
public class TestProducer {
    public void send(String msg) {
        rabbitTemplate.convertAndSend("test.exchange", "test.key", msg);
    }
}

// 3. 消费者
@Component
public class TestConsumer {
    @RabbitListener(queues = "test.queue")
    public void handle(String msg) {
        System.out.println("收到: " + msg);
    }
}
```

### 练习二：手动ACK + 死信

在练习一基础上，增加以下功能：

1. 将消费者的ACK模式改为手动
2. 如果消息内容包含 "error"，拒绝消息并进入死信队列
3. 配置一个死信交换机和一个死信队列
4. 在死信队列上监听并打印死信消息

### 练习三：RocketMQ事务消息

模拟一个订单创建场景：

1. 发送事务消息，half message发送到 `order-tx-topic`
2. 本地事务执行订单创建
3. 如果本地事务成功，COMMIT消息
4. 如果本地事务失败，ROLLBACK消息
5. 实现事务回查逻辑

### 练习四：Kafka基本收发

1. 配置Kafka生产者消费者
2. 发送一个用户对象到 `user-topic`
3. 消费者接收并打印用户信息
4. 开启手动提交offset

---

## 5. 章节综合实操案例

### 案例：订单创建通知系统（RabbitMQ完整实现）

**业务需求：**

```
1. 用户下单 → order-service 发送订单创建事件到MQ
2. notification-service 消费事件 → 发送短信通知 + 邮件通知
3. 通知失败 → 进入死信队列 → 30分钟后重试
4. 重试3次仍失败 → 记录到数据库，人工处理
5. 订单30分钟未支付 → 延迟队列检查并自动取消
```

#### 5.1 项目结构

```text
order-notification-system/
├── order-service/
│   ├── OrderApplication.java
│   ├── config/
│   │   └── RabbitMQConfig.java
│   ├── producer/
│   │   └── OrderMessageProducer.java
│   ├── event/
│   │   └── OrderCreatedEvent.java
│   └── application.yml
├── notification-service/
│   ├── NotificationApplication.java
│   ├── consumer/
│   │   └── OrderNotificationConsumer.java
│   │   └── DeadLetterConsumer.java
│   │   └── OrderTimeoutConsumer.java
│   └── application.yml
└── common/
    └── OrderCreatedEvent.java (共享DTO)
```

#### 5.2 共享事件模型

```java
// common/OrderCreatedEvent.java
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderCreatedEvent {
    private Long orderId;
    private Long userId;
    private String userPhone;
    private String userEmail;
    private BigDecimal amount;
    private LocalDateTime createTime;
}
```

#### 5.3 order-service 完整配置

**application.yml：**

```yaml
spring:
  application:
    name: order-service
  rabbitmq:
    host: localhost
    port: 5672
    virtual-host: /
    username: guest
    password: guest
    publisher-confirm-type: correlated
    publisher-returns: true
    template:
      mandatory: true

server:
  port: 8081
```

**RabbitMQConfig（order-service侧）：**

```java
package com.example.order.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    /** ========== 交换机 ========== */
    @Bean
    public DirectExchange orderExchange() {
        return ExchangeBuilder.directExchange("order.exchange")
                .durable(true)
                .build();
    }

    @Bean
    public TopicExchange notificationExchange() {
        return ExchangeBuilder.topicExchange("notification.exchange")
                .durable(true)
                .build();
    }

    @Bean
    public DirectExchange dlxExchange() {
        return ExchangeBuilder.directExchange("order.dlx.exchange")
                .durable(true)
                .build();
    }

    /** ========== 队列 ========== */
    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable("order.created.queue")
                // 绑定死信交换机
                .deadLetterExchange("order.dlx.exchange")
                .deadLetterRoutingKey("order.created.dead")
                .build();
    }

    @Bean
    public Queue notificationSmsQueue() {
        return QueueBuilder.durable("notification.sms.queue")
                .build();
    }

    @Bean
    public Queue notificationEmailQueue() {
        return QueueBuilder.durable("notification.email.queue")
                .build();
    }

    @Bean
    public Queue dlxQueue() {
        return QueueBuilder.durable("order.dlx.queue")
                .build();
    }

    @Bean
    public Queue orderTimeoutQueue() {
        return QueueBuilder.durable("order.timeout.queue")
                .deadLetterExchange("order.exchange")
                .deadLetterRoutingKey("order.timeout.cancel")
                .ttl(30 * 60 * 1000)  // 30分钟
                .build();
    }

    @Bean
    public Queue orderCancelQueue() {
        return QueueBuilder.durable("order.cancel.queue")
                .build();
    }

    /** ========== 绑定 ========== */
    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder.bind(orderCreatedQueue())
                .to(orderExchange())
                .with("order.created");
    }

    @Bean
    public Binding notificationSmsBinding() {
        return BindingBuilder.bind(notificationSmsQueue())
                .to(notificationExchange())
                .with("notification.sms");
    }

    @Bean
    public Binding notificationEmailBinding() {
        return BindingBuilder.bind(notificationEmailQueue())
                .to(notificationExchange())
                .with("notification.email");
    }

    @Bean
    public Binding dlxBinding() {
        return BindingBuilder.bind(dlxQueue())
                .to(dlxExchange())
                .with("order.created.dead");
    }

    @Bean
    public Binding timeoutCancelBinding() {
        return BindingBuilder.bind(orderCancelQueue())
                .to(orderExchange())
                .with("order.timeout.cancel");
    }

    /** ========== 通用配置 ========== */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                          MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);

        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.info("消息送达Broker: {}", correlationData.getId());
            } else {
                log.error("消息未送达Broker: {}, cause={}", correlationData.getId(), cause);
            }
        });

        template.setReturnsCallback(returned ->
                log.warn("消息路由失败: exchange={}, routingKey={}",
                        returned.getExchange(), returned.getRoutingKey()));

        return template;
    }
}
```

**OrderMessageProducer：**

```java
package com.example.order.producer;

import com.example.common.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送订单创建事件
     */
    public String sendOrderCreated(OrderCreatedEvent event) {
        String messageId = UUID.randomUUID().toString();
        CorrelationData cd = new CorrelationData(messageId);

        rabbitTemplate.convertAndSend(
                "order.exchange",
                "order.created",
                event,
                cd
        );

        log.info("订单事件已投递: orderId={}, messageId={}, amount={}",
                event.getOrderId(), messageId, event.getAmount());

        return messageId;
    }

    /**
     * 发送延迟超时检查消息
     */
    public void sendTimeoutCheck(Long orderId) {
        rabbitTemplate.convertAndSend(
                "order.exchange",
                "order.timeout.check",
                orderId,
                message -> {
                    message.getMessageProperties().setDelay(30 * 60 * 1000); // 30分钟
                    return message;
                }
        );
    }
}
```

**OrderController（触发入口）：**

```java
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderMessageProducer producer;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        // 1. 创建订单
        Order order = orderService.createOrder(request);

        // 2. 发送消息
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(order.getId());
        event.setUserId(order.getUserId());
        event.setUserPhone(order.getUserPhone());
        event.setUserEmail(order.getUserEmail());
        event.setAmount(order.getAmount());
        event.setCreateTime(order.getCreateTime());

        String messageId = producer.sendOrderCreated(event);

        return ResponseEntity.ok(new OrderResponse(order, messageId));
    }
}
```

#### 5.4 notification-service 完整实现

**application.yml：**

```yaml
spring:
  application:
    name: notification-service
  rabbitmq:
    host: localhost
    port: 5672
    virtual-host: /
    username: guest
    password: guest
    listener:
      simple:
        acknowledge-mode: manual
        prefetch: 1
        retry:
          enabled: true
          max-attempts: 3
          initial-interval: 2000ms
          multiplier: 2.0

server:
  port: 8082
```

**OrderNotificationConsumer：**

```java
package com.example.notification.consumer;

import com.example.common.event.OrderCreatedEvent;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotificationConsumer {

    private final SmsService smsService;
    private final EmailService emailService;
    private final NotificationRecordService recordService;

    /**
     * SMS通知消费
     */
    @RabbitListener(queues = "notification.sms.queue")
    public void handleSmsNotification(OrderCreatedEvent event, Message message, Channel channel) {
        long tag = message.getMessageProperties().getDeliveryTag();
        try {
            log.info("发送短信通知: phone={}, orderId={}", event.getUserPhone(), event.getOrderId());
            smsService.send(event.getUserPhone(),
                    String.format("您的订单 %d 已创建，金额 %.2f 元", event.getOrderId(), event.getAmount()));

            channel.basicAck(tag, false);
            recordService.recordSuccess(event.getOrderId(), "SMS");
        } catch (Exception e) {
            log.error("短信通知失败: orderId={}", event.getOrderId(), e);
            channel.basicNack(tag, false, false);  // 进入死信
            recordService.recordFailed(event.getOrderId(), "SMS", e.getMessage());
        }
    }

    /**
     * Email通知消费
     */
    @RabbitListener(queues = "notification.email.queue")
    public void handleEmailNotification(OrderCreatedEvent event, Message message, Channel channel) {
        long tag = message.getMessageProperties().getDeliveryTag();
        try {
            log.info("发送邮件通知: email={}, orderId={}", event.getUserEmail(), event.getOrderId());
            emailService.send(event.getUserEmail(), "订单确认",
                    String.format("您的订单 %d 已创建", event.getOrderId()));

            channel.basicAck(tag, false);
            recordService.recordSuccess(event.getOrderId(), "EMAIL");
        } catch (Exception e) {
            log.error("邮件通知失败: orderId={}", event.getOrderId(), e);
            channel.basicNack(tag, false, false);
            recordService.recordFailed(event.getOrderId(), "EMAIL", e.getMessage());
        }
    }
}
```

**DeadLetterConsumer（死信重试 + 人工补偿）：**

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterConsumer {

    private final NotificationRetryService retryService;
    private final DeadLetterRecordService deadLetterRecordService;

    /**
     * 死信队列 — 30分钟后重试通知
     */
    @RabbitListener(queues = "order.dlx.queue")
    public void handleDeadLetter(OrderCreatedEvent event, Message message, Channel channel) {
        long tag = message.getMessageProperties().getDeliveryTag();

        // 获取重试次数（从消息头中读取）
        Map<String, Object> headers = message.getMessageProperties().getHeaders();
        int retryCount = 0;
        if (headers.containsKey("x-death")) {
            List<Map<String, Object>> xDeath = (List<Map<String, Object>>) headers.get("x-death");
            if (!xDeath.isEmpty()) {
                retryCount = (Integer) xDeath.get(0).getOrDefault("count", 0);
            }
        }

        try {
            log.info("处理死信消息: orderId={}, retryCount={}", event.getOrderId(), retryCount);

            if (retryCount < 3) {
                // 重试通知
                retryService.retryNotification(event);
                channel.basicAck(tag, false);
                log.info("死信重试成功: orderId={}", event.getOrderId());
            } else {
                // 超过3次，记录到数据库，人工处理
                deadLetterRecordService.save(event, retryCount, "已重试3次均失败");
                channel.basicAck(tag, false);
                log.warn("死信重试已达上限，记录人工处理: orderId={}", event.getOrderId());
            }
        } catch (Exception e) {
            log.error("死信处理异常: orderId={}", event.getOrderId(), e);
            // 避免循环死信，记录后ACK
            deadLetterRecordService.save(event, retryCount, e.getMessage());
            channel.basicAck(tag, false);
        }
    }

    /**
     * 订单超时自动取消
     */
    @RabbitListener(queues = "order.cancel.queue")
    public void handleOrderTimeout(Long orderId, Message message, Channel channel) {
        long tag = message.getMessageProperties().getDeliveryTag();
        try {
            log.info("检查订单是否超时: orderId={}", orderId);
            Order order = orderService.getById(orderId);
            if (order != null && order.getStatus() == OrderStatus.UNPAID) {
                orderService.cancel(orderId, "订单超时未支付，系统自动取消");
                log.info("订单超时已取消: orderId={}", orderId);
            }
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("超时处理失败: orderId={}", orderId, e);
            channel.basicNack(tag, false, false);
        }
    }
}
```

#### 5.5 消息幂等性实现（在notification-service中）

```java
@Component
public class IdempotentNotificationService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String IDEMPOTENT_PREFIX = "notify:idempotent:";
    private static final long TTL_HOURS = 24;

    /**
     * 幂等发送通知
     * @return true=首次发送, false=重复消息
     */
    public boolean tryNotify(Long orderId, String notifyType, Runnable notifier) {
        String key = IDEMPOTENT_PREFIX + orderId + ":" + notifyType;

        // SET NX 原子操作
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", TTL_HOURS, TimeUnit.HOURS);

        if (Boolean.TRUE.equals(success)) {
            try {
                notifier.run();
                return true;
            } catch (Exception e) {
                // 失败时删除key，允许重试
                redisTemplate.delete(key);
                throw e;
            }
        }

        log.info("重复通知已跳过: orderId={}, type={}", orderId, notifyType);
        return false;
    }
}
```

#### 5.6 全链路流程图

```text
┌─────────────────────────────────────────────────────────────────────┐
│                    订单通知系统 — 全链路流程                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  [用户下单]                                                          │
│      │                                                              │
│      ▼                                                              │
│  [order-service]                                                    │
│      1. 创建订单 (本地事务)                                          │
│      2. 发送 order.created 消息  ──►  进入 order.created.queue       │
│          (Publisher Confirm 确认)      │                            │
│                                       ▼                            │
│  ┌────────────────  notification-service  ──────────────────────┐   │
│  │                                                              │   │
│  │  order.created.queue  ──►  OrderNotificationConsumer         │   │
│  │       │                          ├── SmsService.send()       │   │
│  │       │                          └── EmailService.send()     │   │
│  │       │                          均失败?                       │   │
│  │       │                          basicNack(requeue=false)    │   │
│  │       ▼                          ────► 死信                   │   │
│  │  order.dlx.queue  ◄──  order.dlx.exchange                    │   │
│  │       │                                                      │   │
│  │       ▼ (30分钟后)                                            │   │
│  │  DeadLetterConsumer                                          │   │
│  │       ├── 重试通知 (retryCount < 3)                           │   │
│  │       └── retryCount >= 3 → 记录DB人工处理                    │   │
│  │                                                              │   │
│  │  ── 同时: order.timeout.queue (TTL=30min)                    │   │
│  │       └── 到期进入死信 → order.exchange                      │   │
│  │            └── order.timeout.cancel → order.cancel.queue      │   │
│  │                 └── OrderTimeoutConsumer                      │   │
│  │                      └── 未支付? 自动取消                      │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  [可靠性保障]                                                       │
│  ├── Producer: Confirm + Returns + 消息落库 + 定时补偿                │
│  ├── Consumer: 手动ACK + 幂等(Redis) + 重试 + 死信兜底               │
│  └── 超过3次死信: 人工处理 (数据库记录)                              │
└─────────────────────────────────────────────────────────────────────┘
```

#### 5.7 死信记录表设计

```sql
CREATE TABLE `dead_letter_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `message_id` VARCHAR(64) NOT NULL COMMENT '消息唯一ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `retry_count` INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
    `reason` VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    `payload` JSON DEFAULT NULL COMMENT '原始消息体',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0=待处理, 1=已处理, 2=忽略',
    `created_at` DATETIME NOT NULL,
    `processed_at` DATETIME DEFAULT NULL,
    `handler_remark` VARCHAR(200) DEFAULT NULL COMMENT '人工处理备注',
    INDEX `idx_order_id` (`order_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='死信记录-人工处理表';
```

---

## 6. 分层综合习题

### 6.1 基础题（P0难度）

1. RabbitMQ中Exchange有哪几种类型？分别适用于什么场景？
2. 简述 `@RabbitListener` 和 `@RabbitHandler` 的区别和使用场景。
3. 什么是死信队列？消息在什么情况下会成为死信？
4. RocketMQ的NameServer的作用是什么？为什么它是无状态的？
5. Kafka中Consumer Group的作用是什么？同一个Group内的Consumer如何分配Partition？
6. 什么是消息幂等性？为什么MQ系统中消息可能被重复消费？
7. `basicQos(1)` 的prefetch=1是什么意思？对消费有什么影响？

### 6.2 进阶题（P1难度）

1. 画图描述RabbitMQ全链路消息确认流程（Publisher Confirm → Return → Consumer ACK），并说明每个环节的作用。
2. RabbitMQ死信队列 + TTL 实现延迟消息的原理是什么？有什么局限性？
3. RocketMQ事务消息的实现原理是什么？画图描述半消息 → 本地事务 → Commit/Rollback → 回查的完整流程。
4. RocketMQ顺序消息如何保证？为什么生产者需要 `MessageQueueSelector`，消费者需要 `ConsumeMode.ORDERLY`？
5. Kafka中 `acks=all` 和 `acks=1` 的区别是什么？对性能有什么影响？
6. 设计一个生产级的消息幂等方案，要求覆盖MQ重复投递、消费者重启、网络超时等场景。
7. 如何保证MQ消息不丢失？分别从生产者、MQ服务端、消费者三个角度说明。

### 6.3 拔高题（P2难度）

1. RabbitMQ中如果死信队列的消息也消费失败，如何设计消息流转避免循环死信？
2. 假设一天有1亿条订单消息通过RocketMQ流转，你会如何设计Topic、Queue数量、消费者并发度来保证高吞吐？
3. Kafka中Partition扩容后，旧数据的分区分配算法与新分区不匹配，如何保证数据不丢失且消费逻辑正确？
4. 分布式系统中，如何设计一个通用的MQ消息中间件层（支持切换RabbitMQ/RocketMQ/Kafka），在Spring中如何抽象？
5. 如果MQ集群整体宕机，如何设计业务系统的降级方案，保证核心交易链路不受影响？给出完整架构设计。

---

## 7. 本章复盘速记清单

### 7.1 RabbitMQ速记

| 要点 | 速记口诀 |
|------|----------|
| Exchange类型 | **Direct**（精确）、**Fanout**（广播）、**Topic**（通配）、**Headers**（头匹配） |
| 死信三场景 | **拒收**（Nack/Reject）、**过期**（TTL）、**队列满**（MaxLength） |
| 确认机制 | 生产者: **Confirm**（到交换机） + **Return**（到队列） → 消费者: **ACK** |
| 手动ACK | `basicAck`(确认) / `basicNack`(拒绝, requeue决定是否重新入队) |
| Prefetch | `prefetch=1` 公平分发，默认250，根据场景调整 |
| 延迟消息 | **TTL+DLX**（无需插件）或 **延迟插件**（更精确） |
| Spring注解 | `@EnableRabbit` + `@RabbitListener` + `@RabbitHandler` |

### 7.2 RocketMQ速记

| 要点 | 速记口诀 |
|------|----------|
| 核心组件 | **NameServer**（路由） + **Broker**（存储） + **Producer** + **Consumer** |
| 事务消息 | **半消息** → **本地事务** → **Commit/Rollback** → **回查** |
| 顺序消息 | 生产者: **MessageQueueSelector** → 消费者: **ORDERLY**模式 |
| 延迟级别 | 18个固定级别: 1s/5s/10s/30s/1m···30m/1h/2h |
| 消息过滤 | **Tag**（精确过滤） + SQL表达式（属性过滤） |

### 7.3 Kafka速记

| 要点 | 速记口诀 |
|------|----------|
| 吞吐密码 | **Partition**（并行度） + **Batch**（批量） + **Zero-Copy**（零拷贝） |
| 消费组 | 一个Partition只能被组内一个Consumer消费，消费者数 ≤ 分区数 |
| 可靠配置 | `acks=all` + `min.insync.replicas=2` + 手动提交offset |
| 关键参数 | `max.poll.records` + `max.poll.interval.ms` + `session.timeout.ms` |

### 7.4 可靠性铁三角

```text
┌────────────────────────────────────────────┐
│           消息可靠性铁三角                    │
├────────────────────────────────────────────┤
│                                            │
│  ┌──────────┐    ┌──────────┐            │
│  │ Producer  │    │ Consumer │            │
│  │  Confirm  │    │ Hand-ACK │            │
│  │  + 重试   │    │  + 幂等   │            │
│  └─────┬────┘    └─────┬────┘            │
│        │               │                  │
│        ▼               ▼                  │
│  ┌──────────────────────────┐            │
│  │     MQ持久化 + 主从同步    │            │
│  │   (镜像队列/ISR/同步刷盘) │            │
│  └──────────────────────────┘            │
│                                            │
│  兜底: 死信队列 + 定时补偿 + 人工处理       │
└────────────────────────────────────────────┘
```

### 7.5 选型速记

| 场景 | 首选 |
|------|------|
| 业务解耦、异步通知、死信处理 | **RabbitMQ** |
| 电商交易、事务消息、顺序消息 | **RocketMQ** |
| 日志采集、大数据管道、流计算 | **Kafka** |

---

## 8. 精通拓展补充-P2

### 8.1 Spring Cloud Stream 抽象层

Spring Cloud Stream 是对消息中间件的统一抽象，支持切换不同的Message Broker：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-stream-rabbit</artifactId>
</dependency>
<!-- 或 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-stream-kafka</artifactId>
</dependency>
```

```java
// 定义消息通道
public interface OrderChannel {
    String INPUT = "order-input";
    String OUTPUT = "order-output";

    @Input(INPUT)
    SubscribableChannel input();

    @Output(OUTPUT)
    MessageChannel output();
}

// 发送消息
@EnableBinding(OrderChannel.class)
public class OrderEventSender {
    @Autowired
    private OrderChannel channel;

    public void send(OrderCreatedEvent event) {
        channel.output().send(MessageBuilder.withPayload(event).build());
    }
}

// 接收消息
@EnableBinding(OrderChannel.class)
public class OrderEventReceiver {
    @StreamListener(OrderChannel.INPUT)
    public void handle(OrderCreatedEvent event) {
        log.info("收到订单事件: {}", event.getOrderId());
    }
}
```

> 💡 Spring Cloud Stream 适合需要同时支持多种MQ或未来可能切换MQ的场景，但会牺牲各MQ的原生高级特性（如RocketMQ事务消息）。

### 8.2 消息堆积处理方案

| 场景 | 原因 | 应急方案 | 长期方案 |
|------|------|----------|----------|
| **消费者故障** | 消费者宕机或阻塞 | 重启消费者，建立新的消费者实例 | 增加消费者数 + 监控告警 |
| **消费者处理慢** | 业务逻辑耗时过长 | 临时增加消费者（需MQ支持动态扩容） | 优化业务逻辑，异步化处理 |
| **生产者爆发** | 突增流量 | 限流降级，MQ做缓冲 | 扩容消费者集群 + 优化Prefetch |
| **死信激增** | 消费者无法处理的消息太多 | 监控死信队列，批量修复后重投 | 分析死信原因，修复Bug |

**RabbitMQ队列积压快速处理脚本：**

```bash
# 1. 查看队列状态
rabbitmqctl list_queues name messages consumers

# 2. 临时创建新队列并移到新消费者（不阻塞原业务）
#    原队列堆积消息转存
rabbitmqadmin get queue=stuck.queue count=10000 --format=raw > messages.json

# 3. 或直接增加消费者实例（新实例处理新消息）
```

### 8.3 RocketMQ海量消息处理策略

```yaml
# Broker配置优化
broker:
  # 异步刷盘提高吞吐（但可靠性降低）
  flushDiskType: ASYNC_FLUSH
  # 主从同步方式
  brokerRole: ASYNC_MASTER  # ASYNC_MASTER / SYNC_MASTER
  # 文件保留时间
  fileReservedHours: 72
  # 删除文件时间点
  deleteWhen: "04"
```

### 8.4 Kafka从容灾到高吞吐的配置

```yaml
spring:
  kafka:
    producer:
      acks: all                 # 最高可靠性
      compression-type: snappy  # 压缩提高吞吐
      batch-size: 32768         # 32KB批量
      linger-ms: 10             # 10ms内凑不齐也发送
      buffer-memory: 33554432   # 32MB发送缓冲区
    consumer:
      fetch-min-bytes: 1024     # 批量拉取最小字节
      fetch-max-wait-ms: 500    # 批量拉取等待时间
      max-poll-records: 500     # 单次拉取最大条数
```

### 8.5 MQ与分布式事务 — 最终一致性方案

```text
MQ实现最终一致性的典型模式：

[服务A]                   [MQ]                    [服务B]
   │                       │                        │
   │--- 1. 本地事务 -----→ │                        │
   │    (业务操作+消息表)   │                        │
   │                       │                        │
   │--- 2. 发送消息 ------→│                        │
   │                       │--- 3. 投递消息 -------→│
   │                       │                        │--- 4. 本地事务
   │                       │                        │    (业务+幂等检查)
   │                       │                        │
   │    ── 定时任务 ──     │                        │
   │    扫描未完成消息       │                        │
   │    重新投递            │                        │

优点：比XA/Seata性能高，适合高并发
缺点：最终一致性，存在短暂的不一致窗口
适用：非强一致性业务（积分、通知、日志）
不适用：金融强一致（需要Seata AT/TCC）
```

### 8.6 消息轨迹与全链路追踪

生产环境需要在消息中注入TraceId，实现全链路的追踪：

```java
public class BaseMessage {
    private String traceId;      // 全链路追踪ID
    private String messageId;    // 消息唯一ID
    private Long timestamp;      // 发送时间戳
    private String source;       // 来源服务
    // 业务字段由子类定义
}

// 使用MDC实现日志联动
public class MqTraceInterceptor {
    public static void injectTrace(Message message) {
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            message.getMessageProperties().setHeader("traceId", traceId);
        }
    }

    public static void restoreTrace(Message message) {
        String traceId = message.getMessageProperties().getHeader("traceId");
        if (traceId != null) {
            MDC.put("traceId", traceId);
        }
    }
}
```

---

> 🎯 **本章核心一句话**：消息队列是微服务解耦与异步的基石，RabbitMQ掌握死信和确认机制，RocketMQ精通事务和顺序消息，Kafka理解分区和消费组，三者结合幂等性和全链路可靠性设计，才能在生产环境中真正用好MQ。
