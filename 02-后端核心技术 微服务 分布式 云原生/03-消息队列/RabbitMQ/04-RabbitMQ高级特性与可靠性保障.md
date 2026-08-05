# RabbitMQ 高级特性与可靠性保障
> 消息不丢、不重、不乱是 MQ 生产的铁律。本章覆盖幂等、事务、延迟、死信等高级特性，以及日常管理与监控告警。

## 目录
1. [全链路消息不丢失](#1-全链路消息不丢失)
2. [幂等性设计](#2-幂等性设计)
3. [事务消息](#3-事务消息)
4. [死信队列深度应用](#4-死信队列深度应用)
5. [延迟队列实现](#5-延迟队列实现)
6. [管理监控](#6-管理监控)
7. [性能优化](#7-性能优化)

---

## 1. 全链路消息不丢失

### 1.1 消息丢失三大环节

```text
┌─────────────────────────────────────────────────────────────┐
│                    消息丢失全景图                             │
├─────────────┬─────────────────────┬─────────────────────────┤
│  生产阶段    │     存储阶段         │     消费阶段             │
├─────────────┼─────────────────────┼─────────────────────────┤
│ 网络超时    │  MQ 宕机             │  消费者 Crash            │
│ 路由错误    │  刷盘失败            │  业务异常                 │
│ 无 Confirm  │  磁盘故障            │  自动 ACK 导致丢失        │
│ 未持久化    │  无副本复制          │  NACK 处理不当            │
└─────────────┴─────────────────────┴─────────────────────────┘
```

### 1.2 三层确认机制

| 机制 | 保障范围 | 说明 |
|------|----------|------|
| **Publisher Confirm** | 生产者→Broker | 确保消息到达交换机 |
| **Publisher Return** | 交换机→队列 | 明确路由不到队列时回退 |
| **Consumer ACK** | 消费者→Broker | 确保消息被成功处理 |

### 1.3 全链路完整配置

```yaml
spring:
  rabbitmq:
    # 生产者层
    publisher-confirm-type: correlated
    publisher-returns: true
    template:
      mandatory: true
    # 消费者层
    listener:
      simple:
        ack-mode: manual
        prefetch: 1
        retry:
          enabled: true
          max-attempts: 3
          initial-interval: 2000
          multiplier: 2
          max-interval: 30000
```

### 1.4 确认回调代码

```java
@Component @Slf4j
public class ReliabilityConfig {

    @Autowired private RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void setupConfirmAndReturn() {
        // ConfirmCallback: 消息是否到达交换机
        rabbitTemplate.setConfirmCallback((cd, ack, cause) -> {
            if (!ack) {
                log.error("确认失败, id: {}, cause: {}", cd.getId(), cause);
                saveToRetryTable(cd); // 写入重试表
            }
        });
        // ReturnsCallback: 交换机未匹配到队列
        rabbitTemplate.setReturnsCallback(returned -> {
            log.warn("消息退回, exchange={}", returned.getExchange());
        });
    }
}
```

### 1.5 消息丢失防护检查清单

| 环节 | 检查项 | 是否必须 |
|------|--------|:--------:|
| 生产者 | Publisher Confirm 已开启 | ✅ |
| 生产者 | 失败重试机制已实现 | ✅ |
| 生产者 | 消息发送时设置 `PERSISTENT` | ✅ |
| 生产者 | 消息有唯一 ID | ✅ |
| Broker | 队列 `durable=true` | ✅ |
| Broker | 交换机 `durable=true` | ✅ |
| Broker | 集群部署（>= 3 节点）| 生产建议 |
| Broker | 镜像队列或仲裁队列 | 生产建议 |
| 消费者 | 手动 ACK（非自动确认）| ✅ |
| 消费者 | 异常时 NACK + 死信 | ✅ |
| 消费者 | 幂等去重机制 | ✅ |

> ⚠️ 生产环境必须手动 ACK，自动确认在消费者宕机时直接丢失消息。

---

## 2. 幂等性设计

### 2.1 重复消费的产生原因

| 原因 | 说明 |
|------|------|
| 网络重试 | 生产者未收到 Confirm，重试发送同一条消息 |
| 消费者 ACK 丢失 | 处理成功但 ACK 未到达 Broker |
| Broker 重试 | 消费者 NACK 后重新入队 |
| 消费者 Crash | 消息重新投递到其他消费者 |

### 2.2 方案一：msgId + Redis（推荐）

```java
@Component
@Slf4j
public class IdempotentChecker {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public boolean tryProcess(String msgId, Runnable business) {
        String key = "mq:msg:id:" + msgId;
        // SET NX + TTL：原子操作，仅首次设置成功
        Boolean success = redisTemplate.opsForValue()
            .setIfAbsent(key, "1", Duration.ofHours(24));

        if (Boolean.TRUE.equals(success)) {
            try {
                business.run();
                return true;
            } catch (Exception e) {
                redisTemplate.delete(key);  // 失败释放，允许重试
                throw e;
            }
        }
        log.info("消息重复, 已跳过, msgId: {}", msgId);
        return false;
    }
}

// 使用
@RabbitListener(queues = "order.queue")
public void handle(Message message, Channel channel) throws IOException {
    String msgId = message.getMessageProperties().getMessageId();
    idempotentChecker.tryProcess(msgId, () -> {
        OrderMessage order = JSON.parseObject(message.getBody(), OrderMessage.class);
        orderService.process(order);
    });
    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
}
```

### 2.3 方案二：数据库唯一约束

```sql
CREATE TABLE mq_consume_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id VARCHAR(64) NOT NULL,
    status TINYINT DEFAULT 0 COMMENT '0=处理中 1=成功 2=失败',
    create_time DATETIME NOT NULL,
    UNIQUE KEY uk_message_id (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

```java
@Transactional
public boolean consumeWithUniqueKey(String messageId, Runnable business) {
    try {
        consumeRecordMapper.insert(new MqConsumeRecord(messageId, 0));
        business.run();
        consumeRecordMapper.updateStatus(messageId, 1);
        return true;
    } catch (DuplicateKeyException e) {
        return false;  // 已消费
    }
}
```

### 2.4 方案三：业务状态机

```java
public boolean processOrderPayment(String orderId, PaymentInfo payment) {
    // 只有 PENDING_PAY 状态才能更新为 PAID
    int rows = orderMapper.updateStatusWithCondition(
        orderId, "PAID", payment, "PENDING_PAY");
    return rows > 0;  // rows == 0 → 已处理过
}
```

### 2.5 方案对比与选型

| 方案 | 优点 | 缺点 | 适用 | 推荐度 |
|------|------|------|------|:------:|
| **Redis SET NX** | 高性能、低延迟 | 依赖 Redis | 通用场景 | ⭐⭐⭐ |
| **数据库唯一键** | 强一致、自带持久化 | DB 性能瓶颈 | 数据库为核心 | ⭐⭐ |
| **业务状态机** | 零额外成本 | 业务耦合 | 订单/状态变更 | ⭐⭐ |

> ⚠️ **组合推荐**：Redis 做第一道防线（高性能），数据库唯一约束做第二道防线（强一致）。

---

## 3. 事务消息

### 3.1 RabbitMQ 原生事务（不推荐）

```java
// 开启事务模式
rabbitTemplate.setChannelTransacted(true);

@Transactional(rollbackFor = Exception.class)
public void sendInTransaction(OrderMessage order) {
    orderService.saveOrder(order);
    rabbitTemplate.convertAndSend("order.exchange", "order.create", order);
    // 任一步失败 → 业务回滚 + 消息取消
}
```

> ⚠️ RabbitMQ 原生事务性能极差（降低约 90% 吞吐），生产环境绝对不要使用。使用「本地消息表 + 定时补偿」方案。

### 3.2 推荐方案：本地消息表 + 定时补偿

```text
业务操作 + 写入本地消息表（同一 DB 事务）
         │
         ▼
      尝试发送消息
       ├── 成功 → 标记已发送 (status=1)
       └── 失败 → 标记待发送 (status=0)
         │
         ▼
  定时任务扫描 → 重试发送（最多 3 次, 指数退避）
         │
         ▼
  超过重试次数 → 标记永久失败（人工介入）
```

```java
@Component
@Slf4j
public class LocalMessageProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private MqMessageMapper mqMessageMapper;

    @Transactional(rollbackFor = Exception.class)
    public void createOrderWithMessage(Order order) {
        // 1. 业务操作
        orderMapper.insert(order);

        // 2. 写本地消息表（同事务）
        MqMessageRecord record = new MqMessageRecord();
        record.setMessageId(UUID.randomUUID().toString());
        record.setBusinessId(order.getId());
        record.setExchange("order.exchange");
        record.setRoutingKey("order.create");
        record.setPayload(JSON.toJSONString(order));
        record.setStatus(0);
        mqMessageMapper.insert(record);

        // 3. 尝试发送
        trySend(record);
    }

    private void trySend(MqMessageRecord record) {
        try {
            rabbitTemplate.convertAndSend(
                record.getExchange(), record.getRoutingKey(), record.getPayload(),
                new CorrelationData(record.getMessageId()));
            mqMessageMapper.updateStatus(record.getId(), 1);
        } catch (Exception e) {
            log.error("发送失败, 等待补偿: {}", record.getMessageId(), e);
        }
    }
}

/**
 * 定时补偿任务（每 30 秒）
 */
@Scheduled(fixedDelay = 30000)
public void compensate() {
    List<MqMessageRecord> pendingList = mqMessageMapper.selectByStatus(0, 100);
    for (MqMessageRecord record : pendingList) {
        if (record.getRetryCount() >= 3) {
            mqMessageMapper.updateStatus(record.getId(), 2);  // 永久失败
            continue;
        }
        try {
            rabbitTemplate.convertAndSend(
                record.getExchange(), record.getRoutingKey(), record.getPayload());
            mqMessageMapper.updateStatus(record.getId(), 1);
        } catch (Exception e) {
            mqMessageMapper.incrementRetry(record.getId());
        }
    }
}
```

### 3.3 本地消息表设计

```sql
CREATE TABLE mq_message_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id VARCHAR(64) NOT NULL,
    business_id VARCHAR(64),
    exchange VARCHAR(64) NOT NULL,
    routing_key VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    status TINYINT DEFAULT 0 COMMENT '0=待发送 1=已发送 2=失败',
    retry_count INT DEFAULT 0,
    create_time DATETIME NOT NULL,
    update_time DATETIME,
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 3.4 方案对比

| 方案 | 性能 | 可靠性 | 复杂度 | 推荐 |
|------|:----:|:------:|:------:|:----:|
| RabbitMQ 原生事务 | ❌ 极差 | 高 | 低 | ❌ |
| 本地消息表 + 定时补偿 | ✅ 好 | 高 | 中 | ✅ 推荐 |

> 🎯 **事务消息核心**：保证「业务操作」与「消息发送」的最终一致性，而不是强一致性。

---

## 4. 死信队列深度应用

### 4.1 死信触发条件详解

| 条件 | 触发时机 | 配置方式 |
|------|----------|----------|
| 消费者 NACK + requeue=false | 消费者处理异常时 | `channel.basicNack(tag, false, false)` |
| 消息 TTL 过期 | 超过 `x-message-ttl` 指定的时间 | `args.put("x-message-ttl", 30000)` |
| 队列达到最大长度 | 超过 `x-max-length` | `args.put("x-max-length", 10000)` |
| 消息被拒绝（Reject）| 消费者 `basicReject` | `channel.basicReject(tag, false)` |

### 4.2 死信配置模板

```java
@Configuration
public class DlxConfig {

    // 业务队列（带死信配置）
    @Bean
    public Queue businessQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "dlx.exchange");
        args.put("x-dead-letter-routing-key", "dlx");
        args.put("x-message-ttl", 30000);
        args.put("x-max-length", 10000);
        args.put("x-overflow", "reject-publish");
        return new Queue("business.queue", true, false, false, args);
    }

    @Bean
    public DirectExchange businessExchange() {
        return new DirectExchange("business.exchange", true, false);
    }

    @Bean
    public Binding businessBinding() {
        return BindingBuilder.bind(businessQueue())
            .to(businessExchange()).with("business.key");
    }

    // 死信交换机 + 队列
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

    // 死信消费者
    @RabbitListener(queues = "dlx.queue")
    public void handleDlx(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        try {
            String body = new String(message.getBody());
            log.warn("收到死信消息: {}", body);
            alertService.sendAlert("死信告警", body);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            channel.basicNack(tag, false, false);
        }
    }
}
```

### 4.3 死信消息属性

死信消息头部会添加 `x-death` 属性，记录死信原因：

| `reason` 值 | 说明 |
|-------------|------|
| `rejected` | 消费者拒绝且不重新投递 |
| `expired` | 消息 TTL 过期 |
| `maxlen` | 队列满 |
| `dropped` | 队列被删除时丢弃 |

### 4.4 死信队列最佳实践

| 实践 | 说明 |
|------|------|
| 核心业务必须配死信 | 订单、支付、库存等重要业务 |
| 死信消费者告警 | 死信产生意味着异常，需通知值班 |
| 死信监控报表 | 统计死信产生速率、TOP 异常类型 |
| 死信自动补偿 | 选择性自动放回原队列或重试 |

---

## 5. 延迟队列实现

### 5.1 方案一：TTL + DLX（标准方式）

```text
核心原理：普通队列无消费者 + 消息 TTL 超时 → 自动投递死信队列 → 死信消费者处理
场景：订单 30 分钟未支付自动取消、定时提醒、重试延迟
```

```java
@Configuration
public class DelayQueueConfig {

    // 延迟队列（无消费者，TTL 过期后转入 real.queue）
    @Bean
    public Queue delayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "real.exchange");
        args.put("x-dead-letter-routing-key", "real.key");
        args.put("x-message-ttl", 1800000);  // 30分钟
        return new Queue("delay.queue", true);
    }

    @Bean
    public DirectExchange delayExchange() {
        return new DirectExchange("delay.exchange");
    }

    @Bean
    public Binding delayBinding() {
        return BindingBuilder.bind(delayQueue())
            .to(delayExchange()).with("delay.key");
    }

    // 真实消费者队列
    @Bean
    public Queue realQueue() {
        return new Queue("real.queue", true);
    }

    @Bean
    public DirectExchange realExchange() {
        return new DirectExchange("real.exchange");
    }

    @Bean
    public Binding realBinding() {
        return BindingBuilder.bind(realQueue())
            .to(realExchange()).with("real.key");
    }

    @RabbitListener(queues = "real.queue")
    public void handleDelayTask(String message) {
        log.info("延迟任务到期: {}", message);
    }
}

public void sendDelayTask(String data) {
    rabbitTemplate.convertAndSend("delay.exchange", "delay.key", data);
}
```

### 5.2 方案二：延迟消息插件（推荐）

```bash
rabbitmq-plugins enable rabbitmq_delayed_message_exchange
```

```java
@Configuration
public class DelayPluginConfig {

    @Bean
    public CustomExchange delayExchange() {
        return new CustomExchange("delay.exchange", "x-delayed-message",
            true, false, Map.of("x-delayed-type", "direct"));
    }

    @Bean
    public Queue delayQueue() { return new Queue("delay.queue", true); }

    @Bean
    public Binding delayBinding() {
        return BindingBuilder.bind(delayQueue())
            .to(delayExchange()).with("delay.key").noargs();
    }
}

@Component
public class DelayProducer {

    @Autowired private RabbitTemplate rabbitTemplate;

    public void sendWithDelay(String message, long delayMs) {
        MessageProperties props = new MessageProperties();
        props.setHeader("x-delay", (int) delayMs);
        props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        rabbitTemplate.send("delay.exchange", "delay.key",
            new Message(message.getBytes(), props));
    }
}
```

### 5.3 两种方案对比

| 特性 | TTL + DLX | 延迟插件 |
|------|:---------:|:--------:|
| 依赖 | 内置特性，无需插件 | 需要安装插件 |
| 灵活度 | 固定 TTL 或每条消息设置 | 每条消息独立毫秒级延迟 |
| 精度 | 秒级 | 毫秒级 |
| 运维 | 零额外维护 | 插件维护 |

> 💡 **推荐使用延迟插件方式**：更灵活、精度更高、使用更简单。

---

## 6. 管理监控

### 6.1 管理界面关键监控指标

| 指标 | 位置 | 含义 | 异常判断 |
|------|------|------|----------|
| **Ready** | Queues | 待消费消息数 | 持续增长 → 消息积压 |
| **Unacked** | Queues | 已投递未确认 | 过多 → ACK 异常 |
| **Total** | Queues | 消息总量 | — |
| **Connections** | Connections | TCP 连接数 | 激增 → 连接泄漏 |
| **Memory** | Overview → Nodes | 内存使用率 | 超过 40% → 关注 |
| **Disk Free** | Overview → Nodes | 磁盘剩余 | 低于阈值 → 阻塞 |

### 6.2 命令行巡检

```bash
# 1. 服务状态
systemctl status rabbitmq-server

# 2. 队列积压检查
rabbitmqctl list_queues -q name messages messages_ready messages_unacknowledged

# 3. 连接数
rabbitmqctl list_connections | wc -l

# 4. 磁盘空间
df -h /var/lib/rabbitmq

# 5. 日志异常
grep -i "error" /var/log/rabbitmq/rabbitmq.log | tail -20
```

### 6.3 备份与恢复

```bash
# 全量配置备份
rabbitmqctl export_definitions /backup/rabbitmq_$(date +%Y%m%d).json

# 恢复（注意：会清除所有现有数据）
rabbitmqctl stop_app
rabbitmqctl reset
rabbitmqctl import_definitions /backup/backup.json
rabbitmqctl start_app
```

### 6.4 常用告警规则

| 告警指标 | 阈值 | 严重度 |
|----------|:----:|:------:|
| 消息积压 | Ready > 10000 | 严重 |
| 消费延迟 | > 10 分钟 | 严重 |
| 磁盘剩余 | < 2GB | 严重 |
| 内存使用 | > 80% | 严重 |
| 节点宕机 | 不可达 | 严重 |

### 6.5 监控工具集成

| 工具 | 方式 | 说明 |
|------|------|------|
| **Prometheus + Grafana** | `rabbitmq_prometheus` 插件 | 官方推荐 |
| **ELK** | 日志采集 | 日志分析和搜索 |
| **Spring Boot Actuator** | `/actuator/health` | 应用级健康检查 |

---

## 7. 性能优化

### 7.1 核心参数调优

| 配置项 | 默认值 | 推荐值 | 说明 |
|--------|:------:|:------:|------|
| `prefetch` | 250 | 10-50 | 预取消息数 |
| `concurrency` | 1 | 3-10 | 消费者并发数 |
| `max-concurrency` | — | CPU * 2 | 最大并发数 |

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        concurrency: 5
        max-concurrency: 10
        prefetch: 10
    cache:
      connection:
        mode: channel
      channel:
        size: 10
```

### 7.2 消息优化

| 优化手段 | 说明 | 建议 |
|----------|------|------|
| 消息体大小 | 单条建议 < 64KB | 大消息拆分或压缩 |
| 消息压缩 | GZIP 压缩 > 10KB | 减少网络传输和存储 |
| 批量消费 | 一次拉取多条处理 | 减小网络开销 |

```java
// GZIP 压缩
public byte[] compress(String data) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
        gzip.write(data.getBytes(StandardCharsets.UTF_8));
    }
    return bos.toByteArray();
}
```

### 7.3 Lazy Queue

```java
@Bean
public Queue lazyQueue() {
    return QueueBuilder.durable("lazy.queue")
        .lazy().build();
}
```

| 模式 | 存储方式 | 适用场景 |
|:----:|----------|----------|
| **Default** | 尽量存内存，溢出后磁盘 | 高吞吐、低延迟 |
| **Lazy** | 全量磁盘存储 | 消息堆积场景 |

### 7.4 性能优化速查表

| 场景 | 瓶颈 | 优化方案 |
|------|------|----------|
| 生产者发送慢 | 网络/确认 | 批量发送 + Confirm 异步 |
| 消费者处理慢 | 业务逻辑 | 增大并发 + 减小 prefetch |
| 消息积压 | 消费跟不上 | Lazy Queue + 扩容消费者 |
| 内存过高 | 消息堆积 | Lazy Queue + 限制队列长度 |
| 网络带宽 | 消息体过大 | GZIP 压缩 |

### 7.5 避坑总结

| 坑点 | 正确做法 |
|------|----------|
| 自动确认丢消息 | 生产务必手动 ACK |
| 消息不持久化 | 队列 `durable=true` + 消息 `PERSISTENT` |
| 无死信队列 | 核心业务必须配置死信交换机 |
| 重试次数过多 | 设置 `max-attempts: 3` + 死信兜底 |
| 连接泄漏 | 复用 Connection，用完关闭 Channel |
| CPU 100% | 检查 prefetch 值和消费代码 |
| 磁盘写满 | 监控 `disk_free_limit`，定期清理 |

> 🎯 **可靠性铁三角**：手动 ACK 是底线，死信队列是兜底，幂等校验是最后一道防线。
