# 09 - Spring 生态集成与最佳实践

> 🎯 Spring Cloud Stream + RocketMQ  = 声明式消息驱动，配合生产避坑指南和面试题，从能用到精通

---

## 目录

1. [Spring Boot 集成 RocketMQ](#1-spring-boot-集成-rocketmq)
2. [Spring Cloud Stream 集成](#2-spring-cloud-stream-集成)
3. [消息可靠性实践](#3-消息可靠性实践)
4. [生产避坑指南](#4-生产避坑指南)
5. [高频面试题精选](#5-高频面试题精选)
6. [生产级配置模板](#6-生产级配置模板)

---

## 1. Spring Boot 集成 RocketMQ

### 1.1 依赖引入

```xml
<!-- rocketmq-spring-boot-starter（非官方但最流行） -->
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
    <version>2.3.1</version>
</dependency>
```

### 1.2 最小配置

```yaml
# application.yml
rocketmq:
  name-server: localhost:9876
  producer:
    group: my-producer-group
    send-message-timeout: 3000
    retry-times-when-send-failed: 2
```

### 1.3 发送消息

```java
@Service
public class OrderMessageService {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    // 1. 发送普通消息
    public void sendOrderCreated(Order order) {
        rocketMQTemplate.convertAndSend("order-topic", order);
    }

    // 2. 带 Tag
    public void sendOrderPaid(Order order) {
        rocketMQTemplate.convertAndSend("order-topic:PAID", order);
        //                               Topic:Tag
    }

    // 3. 带 Key（用于定位消息）
    public void sendWithKey(Order order) {
        Message<String> msg = MessageBuilder.withPayload(JSON.toJSONString(order))
                .setHeader(RocketMQHeaders.KEYS, order.getId())
                .build();
        rocketMQTemplate.send("order-topic", msg);
    }

    // 4. 同步发送 + 获取结果
    public SendResult sendSync(Order order) {
        return rocketMQTemplate.syncSend("order-topic", order);
    }

    // 5. 异步发送
    public void sendAsync(Order order) {
        rocketMQTemplate.asyncSend("order-topic", order, new SendCallback() {
            @Override
            public void onSuccess(SendResult result) {
                log.info("异步发送成功: msgId={}", result.getMsgId());
            }
            @Override
            public void onException(Throwable e) {
                log.error("异步发送失败", e);
            }
        });
    }

    // 6. 顺序消息
    public void sendOrderly(Order order) {
        rocketMQTemplate.syncSendOrderly("order-topic", order, order.getId());
        //                                    hashKey → 相同 Key 到相同 Queue
    }

    // 7. 事务消息
    public void sendTransaction(Order order) {
        rocketMQTemplate.sendMessageInTransaction("order-topic",
            MessageBuilder.withPayload(order).build(), order);
    }

    // 8. 延迟消息
    public void sendDelay(Order order, int delayLevel) {
        Message<String> msg = MessageBuilder.withPayload(JSON.toJSONString(order))
                .setHeader(RocketMQHeaders.KEYS, order.getId())
                .build();
        rocketMQTemplate.syncSend("order-topic", msg, 3000, delayLevel);
        //  delayLevel: 1=1s, 2=5s, 3=10s, ..., 18=2h
    }
}
```

### 1.4 消费消息

```java
@Service
@RocketMQMessageListener(
    topic = "order-topic",
    consumerGroup = "order-consumer-group",
    selectorExpression = "PAID || CREATED",  // Tag 过滤
    consumeMode = ConsumeMode.CONCURRENTLY,  // 并发消费
    messageModel = MessageModel.CLUSTERING   // 集群模式
)
public class OrderConsumer implements RocketMQListener<Order> {

    @Override
    public void onMessage(Order order) {
        log.info("收到订单: id={}, status={}", order.getId(), order.getStatus());
        processOrder(order);
    }
}

// 顺序消费
@Service
@RocketMQMessageListener(
    topic = "order-topic",
    consumerGroup = "order-consumer-group",
    consumeMode = ConsumeMode.ORDERLY,  // 顺序消费
    consumeThreadMax = 1               // 顺序消费一般 1 个线程
)
public class OrderOrderlyConsumer implements RocketMQListener<Order> {
    @Override
    public void onMessage(Order order) {
        processOrderInOrder(order);
    }
}

// 获取原生 MessageExt
@Service
@RocketMQMessageListener(topic = "order-topic", consumerGroup = "order-group")
public class OrderConsumerExt implements RocketMQListener<MessageExt> {
    @Override
    public void onMessage(MessageExt msg) {
        log.info("topic={}, queueId={}, offset={}, keys={}, reconsumeTimes={}",
            msg.getTopic(), msg.getQueueId(), msg.getQueueOffset(),
            msg.getKeys(), msg.getReconsumeTimes());
        process(msg);
    }
}
```

### 1.5 事务消息回查

```java
@RocketMQTransactionListener(txProducerGroup = "order-tx-group")
public class OrderTransactionListener implements RocketMQLocalTransactionListener {

    @Override
    // 执行本地事务
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        try {
            Order order = (Order) arg;
            orderService.createOrder(order);
            return RocketMQLocalTransactionState.COMMIT;
        } catch (Exception e) {
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }

    @Override
    // Broker 回查
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        String orderId = msg.getHeaders().get(RocketMQHeaders.KEYS, String.class);
        Order order = orderService.getById(orderId);
        if (order != null && order.getStatus() == 1) {
            return RocketMQLocalTransactionState.COMMIT;
        } else if (order != null && order.getStatus() == -1) {
            return RocketMQLocalTransactionState.ROLLBACK;
        }
        return RocketMQLocalTransactionState.UNKNOWN;
    }
}
```

---

## 2. Spring Cloud Stream 集成

### 2.1 依赖

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-stream-rocketmq</artifactId>
    <version>2022.0.0.0</version>
</dependency>
```

### 2.2 配置

```yaml
spring:
  cloud:
    stream:
      rocketmq:
        binder:
          name-server: localhost:9876
        bindings:
          order-output:
            producer:
              group: order-producer-group
              sync: true
          order-input:
            consumer:
              group: order-consumer-group
              orderly: false
              tags: PAID || CREATED
      bindings:
        order-output:         # 生产者通道
          destination: order-topic
          content-type: application/json
        order-input:          # 消费者通道
          destination: order-topic
          content-type: application/json
          group: order-consumer-group
```

### 2.3 发送与消费

```java
// 生产者
@EnableBinding(Source.class)
public class OrderProducer {
    @Autowired
    private Source source;

    public void send(Order order) {
        source.output().send(MessageBuilder.withPayload(order).build());
    }
}

// 消费者
@EnableBinding(Sink.class)
public class OrderStreamConsumer {
    @StreamListener(Sink.INPUT)
    public void receive(Order order) {
        log.info("收到: {}", order);
    }
}

// 函数式（Spring Cloud Stream 3.x +）
@Configuration
public class OrderStreamConfig {
    @Bean
    public Consumer<Order> orderConsumer() {
        return order -> log.info("收到: {}", order);
    }

    @Bean
    public Supplier<Order> orderProducer() {
        return () -> {
            // 定时/事件触发生成消息
            return new Order();
        };
    }
}
```

### 2.4 Spring Cloud Stream vs spring-boot-starter

| 维度 | spring-boot-starter | Spring Cloud Stream |
|------|---------------------|---------------------|
| **抽象层级** | 低（RocketMQ 原生 API） | 高（绑定器抽象） |
| **多 MQ 切换** | ❌ 需改代码 | ✅ 改配置即可 |
| **复杂度** | 简单直接 | 需要学习抽象概念 |
| **功能覆盖** | 完整 | 部分（如事务消息支持有限） |
| **推荐度** | 专注 RocketMQ | 多 MQ 可能切换 |

---

## 3. 消息可靠性实践

### 3.1 消息零丢失方案

```text
生产者端：
  ✅ 同步发送 + 重试 3 次
  ✅ 异常时写入本地消息表（定时补偿）
  ✅ 发送前记录 send_log(status=PENDING)

Broker 端：
  ✅ 多 Master 多 Slave（异步复制）
  ✅ 或 SYNC_FLUSH + SYNC_MASTER（金融级）
  ✅ 或 DLedger / Controller 模式

消费者端：
  ✅ 手动确认后才提交
  ✅ 消费前：幂等检查
  ✅ 消费后：标记已消费
  ✅ 异常：触发重试，不要吞异常

补偿：
  ✅ 定时扫描 send_log(status=PENDING) → 重新发送
  ✅ 定时对比 DB 和消费状态 → 补发
```

### 3.2 消费幂等方案

```java
@Component
public class IdempotentConsumer {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @KafkaListener(topics = "order-topic")
    public void consume(ConsumerRecord<String, String> record) {
        String msgKey = record.key();  // 业务唯一 ID

        // 方案 1：Redis Set NX（推荐）
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent("msg:lock:" + msgKey, "1", Duration.ofHours(24));
        if (Boolean.FALSE.equals(locked)) {
            log.warn("重复消息跳过: key={}", msgKey);
            return;
        }

        try {
            processBusiness(record.value());
        } catch (Exception e) {
            // 失败 → 释放锁，等待重试
            redisTemplate.delete("msg:lock:" + msgKey);
            throw e;
        }
    }
}
```

### 3.3 分布式事务方案

```text
RocketMQ 事务消息 + 本地消息表：

订单创建流程：
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  OrderService │ ──→ │  RocketMQ     │ ──→ │InventorySvc  │
│  (本地事务)    │     │  (半消息)     │     │  (消费)      │
└──────┬───────┘     └──────┬───────┘     └──────┬───────┘
       │ ① 发送半消息        │                    │
       │ ──────────────────→│                    │
       │                    │                    │
       │ ② 执行本地事务      │                    │
       │ INSERT order       │                    │
       │                    │                    │
       │ ③ COMMIT/ROLLBACK  │                    │
       │ ──────────────────→│                    │
       │                    │ ④ 投递消息         │
       │                    │ ──────────────────→│
       │                    │                    │ ⑤ 扣库存
       │                    │                    │ ⑥ 返回结果

异常恢复：
  ✅ 本地事务回滚 + 半消息回滚
  ✅ 库存扣减失败 → 重试/补偿
  ✅ Broker 回查 checkLocalTransaction
```

---

## 4. 生产避坑指南

### 4.1 十大高频坑

| # | 坑 | 原因 | 解决 |
|---|------|------|------|
| 1 | **Producer Group 名冲突** | 两个不同应用用同一 Group | 每个应用唯一 Group |
| 2 | **Consumer Group 和 Producer Group 重名** | 同名导致消费混乱 | 严格区分 Group 命名 |
| 3 | **延时消息延迟不准** | 用 delayLevel 而非 time | 5.x 用定时消息，4.x 记住 delayLevel |
| 4 | **未设重试次数** | 默认 16 次 → 重试风暴 | 根据业务设 maxReconsumeTimes |
| 5 | **消息体过大** | >4MB 发送失败 | 压缩或用外链（OSS URL）|
| 6 | **NameServer 单点** | 无 HA | 至少部署 2 台 NameServer |
| 7 | **不处理死信** | 死信堆积不知 | 监控 %DLQ% + 定期处理 |
| 8 | **Consumer 数 > Queue 数** | 多余的 Consumer 闲置 | Consumer 数 ≤ Queue 数 |
| 9 | **未开启故障延迟** | 重复向故障 Broker 发 | sendLatencyFaultEnable=true |
| 10 | **忘记 git 转义** | Topic 名含特殊字符 | 只用字母+数字+连字符 |

### 4.2 容量规划

```text
消息量估算：
  日均消息量 = 日均用户数 × 每用户消息数
  TPS = 日均消息量 / 86400

Queue 数规划：
  Queue 数 = max(TPS / 单Queue吞吐, 最大Consumer数)
  单 Queue 吞吐 ≈ 2000~5000 TPS

Broker 数规划：
  Broker 数 = ceil(总 TPS / 单 Broker TPS) + 冗余
  单 Broker ≈ 50000~100000 TPS（异步刷盘）

磁盘容量：
  磁盘 = 日均消息量 × 平均大小 × 保留天数 × 副本数 × 1.3
```

### 4.3 消息堆积处理

```text
消费堆积 SOS 处理流程：

① 确认堆积原因
   sh bin/mqadmin consumerProgress -g xxx
   → 是消费慢？还是消费停了？

② 紧急止损
   → 停止无关消费者，保障核心业务
   → 临时扩容 Consumer 实例
   
③ 长期方案
   → 增加 Queue 数（需提前规划）
   → 优化消费逻辑（批量、异步化）
   → 增加 Consumer 实例
   → 代码层面性能优化
```

---

## 5. 高频面试题精选

### Q1: RocketMQ 的 NameServer 和 Kafka 的 ZooKeeper 有什么区别？

```text
NameServer：
  → 无状态、节点间不通信、最终一致性
  → CAP 偏 AP（可用+分区容忍）
  → 部署简单、运维成本极低
  → 专为 RocketMQ 设计

ZooKeeper：
  → 有状态、ZAB 共识协议、强一致性
  → CAP 偏 CP（一致+分区容忍）
  → 部署需 3/5 个节点
  → 通用协调服务

Kafka 3.3+ 也用 KRaft 替代了 ZK，理念趋近
```

### Q2: RocketMQ 怎么保证消息顺序？

```text
全局有序：1 个 Topic 只有 1 个 Queue，1 个 Consumer
分区有序（推荐）：MessageQueueSelector 将相同 Key 路由到同一 Queue
  → Producer: 选择固定 Queue
  → Consumer: MessageListenerOrderly 单线程消费

代价：吞吐下降、单点失败整个 Queue 阻塞
```

### Q3: RocketMQ 事务消息的实现原理？

```text
两阶段提交 + Broker 回查：

Phase 1: 发送半消息（half message）
  → 消息存在 CommitLog，但对消费者不可见
  → 标记为 TRANSACTION_PREPARED_TYPE

Phase 2: 执行本地事务 + 提交/回滚
  → COMMIT: 消息标记为可消费 → 消费者可见
  → ROLLBACK: 逻辑删除
  → 超时/未响应: Broker 发起回查

回查: checkLocalTransaction
  → Broker 根据 Producer Group → 找到对应 Producer
  → 最多回查 15 次，间隔 60s
  → 回查结果: COMMIT / ROLLBACK / UNKNOW（继续回查）
```

### Q4: CommitLog + ConsumeQueue 为什么要分开？

```text
CommitLog: 所有 Topic 消息混合顺序写入 → 100% 顺序写
ConsumeQueue: 按 Topic/Queue 索引 CommitLog 位置 → 快速定位

优势：
  ✅ 写：只需要 1 个顺序写的文件（CommitLog）
  ✅ 读：通过轻量索引（20B/条）定位 → 虽然读是随机的但靠页缓存
  ✅ 清理：只需清理 CommitLog（不含消费索引信息）

Kafka 是 Partition 独立存储（N 个顺序写），各有优劣
```

### Q5: 消费堆积怎么处理？

```text
短时堆积：增加 Consumer 实例
长期堆积：
  ① 增加 MessageQueue 数量（提前规划，只能加不能减）
  ② 优化消费逻辑（批处理、缓存、异步化）
  ③ 扩容机器
  ④ 重置消费位点到最新（跳过历史，慎用！）
```

### Q6: RocketMQ 和 Kafka 怎么选？

```text
选 RocketMQ：
  ✅ 业务解耦（订单/支付/通知）
  ✅ 需要事务消息、延迟消息
  ✅ 需要 Tag/SQL92 Broker 端过滤
  ✅ 国内团队、阿里技术栈

选 Kafka：
  ✅ 大数据管道（日志/埋点/CDC）
  ✅ 流处理生态（Flink/Kafka Streams/kSQL）
  ✅ 需要 Connect 插件生态
  ✅ 国际化团队
```

### 更多面试题速览

| # | 问题 | 关键词 |
|---|------|--------|
| 7 | 同步刷盘 vs 异步刷盘？ | ASYNC_FLUSH/SYNC_FLUSH |
| 8 | DLedger 和 Controller 区别？ | Raft 内嵌 vs 外置选举 |
| 9 | 广播模式 vs 集群模式？ | 各自消费 vs 分摊消费 |
| 10 | Offset 存储在哪里？ | Broker consumerOffset.json |
| 11 | 消息过滤有哪些方式？ | Tag / SQL92 / 类过滤（5.x） |
| 12 | 如何实现消息零丢失？ | SYNC_FLUSH + SYNC_MASTER + 手动确认 |
| 13 | Broker 如何处理故障？ | 心跳超时 120s → NameServer 剔除 |
| 14 | PageCache vs MappedByteBuffer？ | Kafka 用 PageCache，RocketMQ 用 MMAP |
| 15 | 5.x 有什么新特性？ | Controller、Proxy、分级存储、定时消息 |

---

## 6. 生产级配置模板

```yaml
# ===== RocketMQ Spring Boot 生产配置 =====
rocketmq:
  name-server: broker1:9876;broker2:9876
  producer:
    group: ${spring.application.name}-producer
    send-message-timeout: 5000
    retry-times-when-send-failed: 3
    retry-times-when-send-async-failed: 3
    compress-message-body-threshold: 4096
    retry-next-server: true
    max-message-size: 4194304
    access-key: ${rocketmq.ak}
    secret-key: ${rocketmq.sk}

  consumer:
    group: ${spring.application.name}-consumer
    orderly: false
    message-model: CLUSTERING
    consume-thread-max: 16
    consume-thread-min: 4
    max-reconsume-times: 10
    pull-batch-size: 32
```

---

## 总结：RocketMQ 学习体系回顾

```text
┌──────────────┬─────────────────────────────────────────┐
│  入门级       │  What/Why/QuickStart                     │
│  (01-02)     │  四大组件 / 安装部署 / CLI                 │
├──────────────┼─────────────────────────────────────────┤
│  原理级       │  CommitLog+ConsumeQueue / 消息类型        │
│  (03-04)     │  事务/顺序/延迟/重试/死信                  │
├──────────────┼─────────────────────────────────────────┤
│  实战级       │  Producer/Consumer / NameServer / 集群    │
│  (05-07)     │  DLedger/Controller / 高可用              │
├──────────────┼─────────────────────────────────────────┤
│  工程级       │  存储设计/性能优化 / Spring 集成            │
│  (08-09)     │  避坑指南 / 面试冲刺                       │
└──────────────┴─────────────────────────────────────────┘

RocketMQ 的标签：金融级、事务消息、延迟消息、阿里生态
和 Kafka 并不是替代关系，而是不同的战场选择。
```
