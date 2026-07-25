# RabbitMQ 面试问答清单
> 🎯 基于项目实战清单，涵盖面试高频问题与完美解答方案，帮助你在面试中脱颖而出。

## 目录
1. [基础概念与核心原理](#1-基础概念与核心原理)
2. [项目实战深度问答](#2-项目实战深度问答)
3. [进阶与系统设计](#3-进阶与系统设计)
4. [场景题与故障排查](#4-场景题与故障排查)

---

## 1. 基础概念与核心原理

> 💡 面试官在这一环节考察你的基本功是否扎实

### Q1：RabbitMQ 的三大交换机模式有什么区别？分别适用于什么场景？
**面试官意图：** 考察消息中间件的核心概念理解，判断你是否能根据业务场景选型。

**完美解答：**

| 交换机类型 | 路由规则 | 消费者关系 | 典型场景 |
|-----------|---------|-----------|---------|
| **Fanout** | 广播，忽略 RoutingKey，发送到所有绑定队列 | 一播多收 | 系统公告广播、全局配置更新、清除所有缓存 |
| **Direct** | 精确匹配 RoutingKey，消息发送到 RoutingKey 完全一致的队列 | 点对点路由 | 按日志级别分发（error → A队列，info → B队列） |
| **Topic** | 通配符匹配 RoutingKey，`*` 匹配一个单词，`#` 匹配零或多个 | 按主题订阅 | 多维度业务消息（如 `order.created`、`order.paid`、`order.shipped`） |

**场景举例：**

**Fanout 广播消息**——适用于需要通知所有模块的场景：
```java
// 交换机定义
@Bean
public FanoutExchange noticeExchange() {
    return new FanoutExchange("notice.exchange");
}

@Bean
public Queue smsQueue() { return new Queue("sms.queue"); }
@Bean
public Queue emailQueue() { return new Queue("email.queue"); }
@Bean
public Queue inAppQueue() { return new Queue("inapp.queue"); }

// 一个消息会同时发送到 sms/email/inApp 三个队列
```

**Topic 主题匹配**——适合灵活的按维度路由：
```bash
# RoutingKey 设计：order.{操作}.{来源}
order.created.app     → 匹配 order.# 或 order.created.*
order.paid.wx        → 匹配 order.# 或 order.paid.*
order.shipped.api    → 匹配 order.#
```

> 💡 **选型口诀**：想广播就 Fanout，点对点路由用 Direct，需要灵活匹配用 Topic。

---

### Q2：RabbitMQ 如何保证消息不丢失？
**面试官意图：** 这是消息队列的高频考点，考察对消息可靠性的理解和完整方案的落地能力。

**完美解答：**

消息丢失可能发生在三个环节，每个环节都有对应的保障机制：

| 环节 | 丢失原因 | 解决方案 | 配置方式 |
|------|---------|---------|---------|
| **生产者→交换机** | 网络问题、交换机故障 | **Confirm 机制** | `spring.rabbitmq.publisher-confirm-type=correlated` |
| **交换机→队列** | 路由失败 | **Return 机制** + Mandatory | `spring.rabbitmq.publisher-returns=true` |
| **队列存储** | RabbitMQ 宕机 | **消息持久化** | `queue(durable=true)` + `message(deliveryMode=PERSISTENT)` |
| **消费者消费** | 消费者处理异常 | **手动 ACK** | `channel.basicAck()` 确认成功后调用 |

**完整配置示例：**
```yaml
spring:
  rabbitmq:
    publisher-confirm-type: correlated  # Confirm 异步回调
    publisher-returns: true              # Return 回调
    template:
      mandatory: true                    # 路由失败强制触发 Return
    listener:
      simple:
        acknowledge-mode: manual         # 手动 ACK
        retry:
          enabled: true                  # 消费重试
          max-attempts: 3
          initial-interval: 2000ms
```

**生产端代码：**
```java
@Slf4j
@Component
public class OrderMessageSender {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void init() {
        // Confirm 回调：确认消息是否到达交换机
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.info("消息已到达交换机: {}", correlationData.getId());
            } else {
                log.error("消息未到达交换机: {}, cause: {}", correlationData.getId(), cause);
                // 补偿：记录到 DB，定时任务重发
                saveToRetryTable(correlationData);
            }
        });

        // Return 回调：确认消息是否从交换机路由到队列
        rabbitTemplate.setReturnsCallback(returned -> {
            log.error("消息路由失败: exchange={}, routingKey={}, replyText={}",
                returned.getExchange(), returned.getRoutingKey(), returned.getReplyText());
        });
    }

    public void sendOrderMessage(Order order) {
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
        rabbitTemplate.convertAndSend("order.exchange", "order.created", order, correlationData);
    }
}
```

> 🎯 **总结**：消息不丢失的完整链路 = **Confirm 确认到交换机 + Mandatory 确保路由到队列 + 队列/消息持久化 + 消费者手动 ACK**。四者缺一不可。

---

### Q3：什么是死信队列？延迟队列是怎么实现的？
**面试官意图：** 考察 RabbitMQ 高级特性的掌握程度，尤其是延迟消息在业务中的实际应用。

**完美解答：**

**死信队列（DLQ）**：当消息满足以下任一条件时，会被发送到死信交换机（DLX），再由死信交换机路由到死信队列：

1. **消息 TTL 过期**（`expiration` 超时）
2. **队列达到最大长度**
3. **消费者 NACK 且不重新入队**（`basicNack(deliveryTag, false, false)`）

**延迟队列的实现原理：**

RabbitMQ 本身没有延迟队列功能，通过 **死信队列（DLX）+ TTL** 的组合来实现：

```java
@Configuration
public class DelayQueueConfig {

    // 1. 延迟队列（实际收消息的队列）
    @Bean
    public Queue businessQueue() {
        return QueueBuilder.durable("business.queue").build();
    }

    // 2. 死信交换机
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("dlx.exchange");
    }

    // 3. 等待队列：消息在这里等待 TTL 过期，然后转入死信队列
    @Bean
    public Queue delayQueue() {
        return QueueBuilder.durable("delay.queue")
            .deadLetterExchange("dlx.exchange")       // 指定死信交换机
            .deadLetterRoutingKey("business.key")     // 死信路由到 business.queue
            .ttl(30000)                                // 30 秒 TTL
            .maxLength(10000)                          // 队列最大长度
            .build();
    }

    // 4. 绑定：等待队列绑定到死信交换机
    @Bean
    public Binding delayBinding() {
        return BindingBuilder
            .bind(delayQueue())
            .to(deadLetterExchange()).with("delay");
    }

    // 5. 业务队列绑定到死信交换机
    @Bean
    public Binding businessBinding() {
        return BindingBuilder
            .bind(businessQueue())
            .to(deadLetterExchange()).with("business.key");
    }
}
```

**经典业务场景——30 分钟未支付自动取消订单：**
```java
public void createOrder(Order order) {
    // 1. 创建订单（DB 状态为待支付）
    orderMapper.insert(order);

    // 2. 发送延迟消息，30 分钟后检查支付状态
    Message message = MessageBuilder
        .withBody(order.getId().toString().getBytes())
        .setExpiration("1800000") // 30 分钟 TTL
        .build();
    rabbitTemplate.send("delay.exchange", "delay", message);

    // 3. 消费者在 business.queue 收到延迟消息后：
    //    检查订单状态 → 未支付则取消订单并释放库存
}
```

> ⚠️ **缺陷**：同一个队列中的消息 TTL 是队列级别的，前面的消息没过期会阻塞后面消息的执行（即使后面的 TTL 更短）。解决方案：为不同延迟时间创建不同的队列。

---

## 2. 项目实战深度问答

> 💡 面试官会深挖你的项目细节，验证你"真的做过"而不是"背过"

### Q4：你在秒杀系统中如何使用 RabbitMQ 实现异步削峰？描述完整流程
**面试官意图：** 考察消息队列在真实高并发场景下的运用，验证你是不是真的做过秒杀系统。

**完美解答：**

**秒杀系统整体架构流程：**

```
用户请求 → Nginx → 前置校验 → Redis 预扣库存 → MQ 削峰 → 异步落库
                                     ↓
                              消息队列缓冲
                                     ↓
                           消费者批量处理订单
```

**核心实现：**

**1. Redis 预扣库存（前置过滤）**
```java
public Result seckill(Long userId, Long productId) {
    // 1. 前置校验（限流 + 去重）
    if (isRateLimited(userId)) return Result.fail("请求过于频繁");
    if (hasBought(userId, productId)) return Result.fail("已购买");

    // 2. Redis 预扣库存
    Long stock = redisTemplate.opsForValue().decrement("stock:" + productId);
    if (stock < 0) {
        redisTemplate.opsForValue().increment("stock:" + productId); // 恢复
        return Result.fail("已售罄");
    }

    // 3. 发送 MQ 消息异步创建订单
    SeckillMessage msg = new SeckillMessage(userId, productId);
    rabbitTemplate.convertAndSend("seckill.exchange", "seckill.order", msg);

    return Result.success("排队中，请稍后查看结果");
}
```

**2. RabbitMQ 削峰配置**
```yaml
spring:
  rabbitmq:
    listener:
      simple:
        prefetch: 1           # 每次只拉取一条，手动确认后才拉取下一条
        concurrency: 10       # 最小消费者数
        max-concurrency: 30   # 最大消费者数（按需弹性扩展）
        acknowledge-mode: manual
```

**3. 消费者异步处理订单**
```java
@RabbitListener(queues = "seckill.order.queue")
public void handleSeckillOrder(SeckillMessage msg, Channel channel, Message message) {
    long deliveryTag = message.getMessageProperties().getDeliveryTag();
    try {
        // 1. 幂等检查（防止重复消费）
        if (redisTemplate.opsForValue().setIfAbsent("seckill:done:" + msg.getUserId() + ":" + msg.getProductId(), "1")) {
            // 2. 创建订单
            Order order = new Order();
            order.setUserId(msg.getUserId());
            order.setProductId(msg.getProductId());
            order.setStatus(OrderStatus.PAID);
            orderMapper.insert(order);

            // 3. 更新库存（MySQL）
            productMapper.decrementStock(msg.getProductId());

            // 4. 通知用户成功
            noticeService.sendSuccess(msg.getUserId());

            // 5. 手动确认
            channel.basicAck(deliveryTag, false);
        } else {
            // 已处理，直接确认
            channel.basicAck(deliveryTag, false);
        }
    } catch (Exception e) {
        log.error("秒杀订单处理失败", e);
        // 6. 失败处理：NACK 并进入死信队列
        channel.basicNack(deliveryTag, false, false);
    }
}
```

**效果数据：**
| 指标 | 使用 MQ 前 | 使用 MQ 后 |
|------|-----------|-----------|
| 数据库瞬时 QPS | 5000+（被打垮） | 稳定在 200（可控） |
| 订单成功率 | 80%（大量超时） | 99.5% |
| 系统可用性 | 经常宕机 | 稳定运行 |

> 🎯 **核心思想**：Redis 做第一层过滤（预扣库存），RabbitMQ 做第二层缓冲（排队处理），MySQL 做最终持久化。MQ 就像"水库"，把洪峰变成涓涓细流。

---

### Q5：消息幂等性是如何保证的？你们怎么处理重复消息？
**面试官意图：** 消息重复投递是分布式系统中的常见问题，考察你对幂等性的理解和实现经验。

**完美解答：**

**重复消息的产生原因：**

```
场景：消费者处理完成但 ACK 还没来得及发送就宕机了
RabbitMQ 认为消息未被消费 → 重新投递 → 产生重复消息
```

**幂等性三类方案对比：**

| 方案 | 原理 | 优缺点 | 适用场景 |
|------|------|--------|---------|
| **Redis 去重** | 用消息唯一 ID 写入 Redis，判断是否已处理 | 高性能，但依赖 Redis 可用性 | 高并发场景 |
| **数据库唯一索引** | 用业务 ID 建唯一索引，重复插入报错 | 可靠但性能略低 | 订单金额等敏感数据 |
| **业务状态判断** | 先查数据库状态，已处理则跳过 | 简单但需要多一次 IO | 业务状态明确的场景 |

**推荐方案——Redis + 数据库双重保障：**

```java
@Component
public class IdempotentHandler {

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 检查消息是否已处理（Redis 层面）
     */
    public boolean isProcessed(String messageId) {
        // Redis 去重：如果已存在表示已处理
        Boolean success = redisTemplate.opsForValue()
            .setIfAbsent("msg:done:" + messageId, "1", 1, TimeUnit.DAYS);
        return Boolean.FALSE.equals(success);
    }

    /**
     * 业务幂等处理示例（防重复下单）
     */
    public void processOrderCreate(SeckillMessage msg, String messageId) {
        // 1. Redis 去重（一级过滤）
        if (isProcessed(messageId)) {
            log.info("消息已处理，跳过: {}", messageId);
            return;
        }

        try {
            // 2. 业务处理：利用唯一索引（二级保障）
            Order order = new Order();
            order.setOrderNo(msg.getUserId() + "_" + msg.getProductId() + "_" + System.currentTimeMillis());
            // order_no 有唯一索引，重复插入会抛异常
            orderMapper.insert(order);

            // 3. 确认消费
            channel.basicAck(deliveryTag, false);
        } catch (DuplicateKeyException e) {
            // 唯一索引冲突：已存在，幂等成功
            log.warn("重复订单，幂等处理");
            channel.basicAck(deliveryTag, false);
        }
    }
}
```

> ⚠️ **关键点**：`setIfAbsent` 必须配合过期时间使用，防止 Key 长期占用内存。数据库唯一索引是保底方案，防止 Redis 宕机导致去重失效。

---

### Q6：消息积压怎么处理？消费者处理速度跟不上生产者怎么办？
**面试官意图：** 考察线上问题的排查和应急处理能力，这是 MQ 运维的常见场景。

**完美解答：**

**消息积压的排查步骤：**

```bash
# 1. RabbitMQ 管理后台查看队列状态（浏览器访问 15672）
# 查看 Ready（待消费数）、Unacked（处理中数）、Total（总数）

# 2. 命令行查看
rabbitmqctl list_queues name messages_ready messages_unacknowledged
```

**紧急处理方案（三阶段）：**

**第一阶段：快速降积压**

| 操作 | 说明 |
|------|------|
| **增加消费者** | `max-concurrency` 从 10 调到 50，物理机器不够加机器 |
| **调整 prefetch** | `prefetch=100`，让消费者一次拉取更多消息（适合 CPU 密集型） |
| **紧急扩容队列** | 新建临时队列，把消息转发到新队列（多消费者并行） |

```yaml
# 紧急调整消费者配置
spring:
  rabbitmq:
    listener:
      simple:
        concurrency: 20
        max-concurrency: 50
        prefetch: 50  # 从 1 调到 50，每次批量拉取
```

**第二阶段：排查根因**

| 原因 | 表现 | 解决方案 |
|------|------|---------|
| **消费者处理慢** | 每条消息耗时 > 1 秒 | 优化业务逻辑、加强索引、批量处理 |
| **消费者死循环** | 消息消费后 NACK 重试，死循环 | 限制重试次数，超过进入死信队列 |
| **消费者宕机** | Unacked 消息持续增加 | 重启消费者，检查异常 |
| **DB 连接池满** | 消费者在等待数据库连接 | 增加连接池、优化慢 SQL |

**第三阶段：批量处理（长期优化方案）**
```java
@RabbitListener(queues = "batch.queue")
public void batchProcess(List<Message> messages, Channel channel) {
    // 批量消费：一次性处理多条消息，提升吞吐量
    List<Order> orders = new ArrayList<>();
    for (Message msg : messages) {
        Order order = JSON.parseObject(msg.getBody(), Order.class);
        orders.add(order);
    }

    // 批量插入 MySQL
    orderMapper.batchInsert(orders);

    // 批量确认
    long deliveryTag = messages.get(messages.size() - 1).getMessageProperties().getDeliveryTag();
    channel.basicAck(deliveryTag, true); // multiple=true 确认之前所有消息
}
```

> 💡 **预判与预防**：设置队列最大长度和死信队列兜底，超出最大长度的消息自动进入死信队列，避免内存撑爆。

---

## 3. 进阶与系统设计

> 💡 这是拉开差距的环节，考察架构思维

### Q7：如何保证分布式事务的最终一致性？本地消息表怎么设计？
**面试官意图：** 考察分布式事务处理能力，这是跨服务、跨数据库场景下的核心难题。

**完美解答：**

**方案对比：**

| 方案 | 一致性 | 复杂度 | 性能 | 适用场景 |
|------|:------:|:------:|:----:|---------|
| **2PC（两阶段提交）** | 强 | 高 | 低 | 支付等需要强一致的场景 |
| **TCC（Try-Confirm-Cancel）** | 强 | 最高 | 中 | 金融级业务 |
| **本地消息表** | 最终一致 | 低 | 高 | 订单、积分等通用场景 |
| **MQ 事务消息** | 最终一致 | 中 | 高 | RocketMQ 特有 |

**本地消息表实现方案（推荐）：**

```sql
-- 1. 本地消息表
CREATE TABLE `local_message` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `biz_id` VARCHAR(64) NOT NULL COMMENT '业务ID（唯一）',
  `message_body` TEXT NOT NULL COMMENT '消息内容（JSON）',
  `exchange` VARCHAR(128) NOT NULL COMMENT '目标交换机',
  `routing_key` VARCHAR(128) NOT NULL COMMENT '路由键',
  `status` TINYINT DEFAULT 0 COMMENT '0-待发送 1-已发送 2-已确认 3-发送失败',
  `retry_count` INT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `next_retry_time` DATETIME DEFAULT NULL,
  INDEX idx_status_next_retry (`status`, `next_retry_time`),
  UNIQUE KEY uk_biz_id (`biz_id`)
) COMMENT '本地消息表';
```

```java
@Service
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private LocalMessageMapper messageMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Transactional(rollbackFor = Exception.class)
    public Result createOrder(Order order) {
        // 1. 创建订单（DB）
        orderMapper.insert(order);

        // 2. 写入本地消息表（同一个事务！）
        LocalMessage msg = new LocalMessage();
        msg.setBizId(order.getOrderNo());
        msg.setMessageBody(JSON.toJSONString(order));
        msg.setExchange("order.exchange");
        msg.setRoutingKey("order.created");
        msg.setStatus(0);
        messageMapper.insert(msg);

        // 事务提交后，消息表中有记录就保证了可靠性
        return Result.success();
    }
}

@Component
public class MessageRelayTask {

    @Scheduled(fixedDelay = 5000) // 5 秒轮询一次
    public void sendPendingMessages() {
        // 查询待发送和发送失败的消息
        List<LocalMessage> messages = messageMapper.selectPending(100);

        for (LocalMessage msg : messages) {
            try {
                // 发送消息
                rabbitTemplate.convertAndSend(
                    msg.getExchange(), msg.getRoutingKey(),
                    msg.getMessageBody(), new CorrelationData(msg.getBizId())
                );
                // 更新状态为已发送
                messageMapper.updateStatus(msg.getId(), 1);
            } catch (Exception e) {
                // 更新重试次数和下次重试时间
                messageMapper.updateRetry(msg.getId());
                log.error("消息发送失败，将重试: {}", msg.getBizId(), e);
            }
        }
    }
}
```

> 🎯 **核心思想**：本地消息表方案的核心是"业务操作和消息记录在同一个本地事务中"，保证业务成功则消息记录必存在，然后通过定时任务兜底发送消息，实现"先本地成功，再异步传播"的最终一致性。

---

### Q8：RabbitMQ 的高可用是怎么实现的？镜像队列和普通队列的区别是什么？
**面试官意图：** 考察对消息中间件高可用架构的理解。

**完美解答：**

**RabbitMQ 的高可用架构有三种模式：**

| 模式 | 数据复制 | 自动故障转移 | 性能影响 | 适用场景 |
|------|---------|:-----------:|:--------:|---------|
| **单机模式** | 无 | 否 | 无 | 开发测试 |
| **普通集群** | 元数据复制，数据不复制 | 否（队列元数据同步） | 低 | 对可用性要求不高的场景 |
| **镜像队列** | 全量数据复制到所有节点 | 是 | 中（写入需要同步） | **生产推荐** |

**普通集群 vs 镜像队列：**

```bash
# 普通集群：只有队列所在节点存数据，其他节点只存元数据
# 缺点：如果队列所在节点挂了，其他节点无法访问该队列的数据

# 镜像队列：队列数据在所有节点都有副本
# 优点：任意节点宕机，其他节点无缝接管
```

**镜像队列配置：**
```yaml
spring:
  rabbitmq:
    addresses: node1:5672,node2:5672,node3:5672
    # 定义镜像策略
```

```bash
# 命令行设置镜像策略（匹配所有以 "order" 开头的队列）
rabbitmqctl set_policy ha-order "^order\." '{
  "ha-mode": "all",
  "ha-sync-mode": "automatic"
}'

# 或者使用管理后台：Admin → Policies → Add policy
```

> 💡 **生产建议**：生产环境至少部署 3 个节点组成镜像队列集群，配合 Haproxy + Keepalived 做负载均衡和 VIP 漂移，保证 RabbitMQ 高可用。

---

## 4. 场景题与故障排查

> 💡 考察实际解决问题的能力

### Q9：消费者消息处理失败，一直重试导致死循环怎么办？
**面试官意图：** 考察对异常消息的兜底处理能力。

**完美解答：**

**问题现象：**
```
消费者收到消息 → 处理失败 → NACK(false) → 重新入队
→ 再次被消费 → 再次失败 → 无限循环
```

**解决方案——分级兜底：**

```java
@RabbitListener(queues = "order.queue")
public void handleOrder(Message message, Channel channel) {
    long deliveryTag = message.getMessageProperties().getDeliveryTag();
    long retryCount = getRetryCount(message); // 从 header 读取重试次数

    try {
        processOrder(message);
        channel.basicAck(deliveryTag, false);
    } catch (Exception e) {
        if (retryCount < 3) {
            // 1. 前 3 次：重试，延迟后重新入队
            log.warn("处理失败，第 {} 次重试", retryCount + 1, e);
            channel.basicNack(deliveryTag, false, true); // requeue=true
        } else if (retryCount < 5) {
            // 2. 4-5 次：延迟重试（手动控制重试间隔）
            Thread.sleep(5000);
            channel.basicNack(deliveryTag, false, true);
        } else {
            // 3. 超过 5 次：进入死信队列，人工排查
            log.error("消息处理失败超过 5 次，进入死信队列", e);
            channel.basicNack(deliveryTag, false, false); // requeue=false
        }
    }
}

private long getRetryCount(Message message) {
    // 从消息的 header 中获取重试次数
    Object retryHeader = message.getMessageProperties()
        .getHeader("x-death");
    if (retryHeader instanceof List) {
        return ((List<?>) retryHeader).size();
    }
    return 0;
}
```

**更好的方式：用 Spring 重试 + 死信队列**
```yaml
spring:
  rabbitmq:
    listener:
      simple:
        retry:
          enabled: true
          max-attempts: 5        # 最大重试次数
          initial-interval: 2000 # 初始间隔 2 秒
          multiplier: 2          # 递增倍数（2s → 4s → 8s → 16s → 32s）
          max-interval: 30000    # 最大间隔 30 秒
```

> ⚠️ **不要无限制重试**！始终设置最大重试次数，超出后进入死信队列。死信队列就像一个"垃圾箱"，防止坏消息占据正常队列。

---

### Q10：线上 RabbitMQ 连接数过高或 OOM 怎么解决？
**面试官意图：** 考察运维排查能力和资源管理能力。

**完美解答：**

**连接数过高排查：**

```bash
# 查看当前连接数
rabbitmqctl list_connections name state user peer_host

# 查看通道数
rabbitmqctl list_channels

# 查看是否有连接泄漏（连接数持续增长不释放）
rabbitmqctl list_connections | wc -l
```

**OOM 排查：**

| 原因 | 表现 | 解决方案 |
|------|------|---------|
| **队列堆积过多** | 未消费消息占满内存 | 设置队列最大长度 `x-max-length=10000` |
| **消息未持久化** | 大量消息在内存中 | 消息持久化 + 限制最大长度 |
| **内存高水位告警** | RabbitMQ 触发 flow control | 扩大内存或减少队列数 |
| **连接泄漏** | 应用创建连接不释放 | 使用连接池（如 CachingConnectionFactory） |

**预防方案：**
```bash
# 1. 配置内存阈值（默认 40%，建议 50%）
rabbitmqctl set_vm_memory_high_watermark 0.5

# 2. 设置队列最大长度
# 在创建队列时指定
@Bean
public Queue orderQueue() {
    return QueueBuilder.durable("order.queue")
        .maxLength(100000)              // 最多 10 万条
        .overflow("reject-publish")     // 超限后拒绝新消息
        .deadLetterExchange("dlx.exchange") // 死信兜底
        .build();
}

# 3. 客户端连接池配置
spring:
  rabbitmq:
    cache:
      connection:
        mode: channel   # 复用连接，创建新通道
      channel:
        size: 10        # 每个连接缓存 10 个通道
```

---

### Q11：如果消息的消费顺序必须保证，你怎么设计？
**面试官意图：** 考察对有序消息的理解，这是消息队列的一个关键限制。

**完美解答：**

**RabbitMQ 本身不保证顺序，但可以设计来保证：**

**方案一：单队列单消费者**
```yaml
# 最简单的方案：用一个队列一个消费者，天然先进先出
spring:
  rabbitmq:
    listener:
      simple:
        concurrency: 1   # 固定一个消费者
        prefetch: 1      # 一次拉一条
```

**方案二：按业务 ID 哈希路由**
```java
public class OrderlyRouter {
    public void sendOrderedMessage(String orderId, Object message) {
        // 同一个 orderId 始终发送到同一个队列
        int queueIndex = orderId.hashCode() % 4;
        String routingKey = "order.queue." + queueIndex;
        rabbitTemplate.convertAndSend("order.exchange", routingKey, message);
    }
}

// 每个队列一个消费者，保证同一个 orderId 的消息顺序
```

**方案三：如果乱序也能接受——局部有序**
- 大部分场景不需要全局有序，只需要"同一个业务实体"的消息有序
- 比如同一个订单的事件：创建 → 支付 → 发货，必须有序
- 但不同订单的事件可以并行处理

> 💡 **面试加分**：能说出"90% 的场景真的不需要全局有序，局部有序就足够了"，说明你有工程判断力。

---

## 💎 面试加分金句

- "RabbitMQ 使用 AMQP 0-9-1 协议，它的核心模型是"交换机→队列→消费者"的三层路由模型，这和 Kafka 的订阅模型有本质区别。"
- "消息可靠性不是靠单一手段实现的，而是 Confirm + 持久化 + 手动 ACK + 死信兜底的四重保障体系。"
- "死信队列不仅仅是一个兜底机制，它还可以用来实现延迟队列——这是 RabbitMQ 最巧妙的工程设计之一。"
- "幂等性是分布式系统的核心难题，我的原则是：**先 Redis 快速过滤，再数据库唯一索引保底**，双重保障才能放心。"
- "RocketMQ 的事务消息比 RabbitMQ 更适合做分布式事务，但 RabbitMQ 配合本地消息表方案也完全可以实现最终一致性。"

## 📋 高频追问清单

| 追问方向 | 应对策略 |
|----------|----------|
| RabbitMQ 和 Kafka 的核心区别？ | 模型不同（队列 vs 日志流），功能侧重点不同（可靠性 vs 吞吐量） |
| AMQP 协议是什么？ | 高级消息队列协议，定义了 Producer/Exchange/Queue/Consumer 模型 |
| Channel 和 Connection 的关系？ | Connection 是 TCP 连接，Channel 是 Connection 内的虚拟通道，复用 TCP |
| 为什么 RabbitMQ 支持多种交换机？ | 为了满足不同路由场景（广播、点对点、通配符） |
| 如何处理消息的幂等？ | Redis 去重 + 数据库唯一索引 + 业务状态判断 |
| 消息事务（txSelect）和 Confirm 模式区别？ | 事务性能差（同步），Confirm 是异步回调，推荐使用 |
| RabbitMQ 默认端口有哪些？ | 5672（AMQP），15672（管理后台），25672（集群） |
| NACK 的 requeue 参数怎么用？ | true=重新入队，false=进入死信队列或丢弃 |

## 🔗 关联知识点

- [Redis必做项目清单-面试问答](#) — 秒杀系统 Redis + RabbitMQ 协同方案
- [MySQL必做项目清单-面试问答](#) — 本地消息表保证最终一致性
- [Docker必做项目清单-面试问答](#) — RabbitMQ 容器化部署
