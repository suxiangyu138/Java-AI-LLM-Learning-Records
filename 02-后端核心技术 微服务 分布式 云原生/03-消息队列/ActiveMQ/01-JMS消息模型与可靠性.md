# 01 - JMS 消息模型与可靠性

> 定位：Queue vs Topic 全场景选型、消息持久化与内存模式、四种 ACK 确认机制对比与选型、JMS 事务与分布式事务、死信队列与重投策略、可靠性对标 RabbitMQ/Kafka——JMS 消息可靠性的完整体系

## 📚 目录

1. [P2P Queue vs Pub/Sub Topic 全场景](#1-p2p-queue-vs-pubsub-topic-全场景)
2. [持久化：KahaDB 与 JDBC 存储](#2-持久化kahadb-与-jdbc-存储)
3. [确认机制 ACK：四种模式的完整对比](#3-确认机制-ack四种模式的完整对比)
4. [JMS 事务：本地事务与 JTA 分布式](#4-jms-事务本地事务与-jta-分布式)
5. [死信队列与重投策略](#5-死信队列与重投策略)
6. [消息选择器与分组](#6-消息选择器与分组)
7. [与 RabbitMQ/Kafka 的可靠性对标](#7-与-rabbitmqkafka-的可靠性对标)

---

## 1. P2P Queue vs Pub/Sub Topic 全场景

### 1.1 基础对比

| 维度 | Queue（P2P） | Topic（Pub/Sub） |
|------|:---:|:---:|
| 消费模式 | 一条消息一个消费者（竞争） | 一条消息多个消费者 |
| 负载均衡 | ✅ 多个消费者分担 | ❌ 每个都收到全量 |
| 消息保留 | 消费后删除 | 默认不保留（无订阅） |
| 持久订阅 | 天然 | 需 `createDurableSubscriber` |
| 适用 | 任务分发/订单处理 | 事件广播/配置变更通知 |

### 1.2 完整代码示例

```java
// ===== Queue 模式：生产者 + 消费者 =====
// 生产者
Connection conn = factory.createConnection();
Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
Queue queue = session.createQueue("orders.queue");
MessageProducer producer = session.createProducer(queue);
// ⚠️ 设置持久化（Broker 重启不丢）
producer.setDeliveryMode(DeliveryMode.PERSISTENT);
TextMessage msg = session.createTextMessage("{\"orderId\":1001}");
producer.send(msg);

// 消费者（竞争消费：多实例分摊处理）
MessageConsumer consumer1 = session.createConsumer(queue);
MessageConsumer consumer2 = session.createConsumer(queue);
conn.start();
// → 消息在 consumer1 与 consumer2 之间均分（round-robin）

// ===== Topic 模式 =====
Topic topic = session.createTopic("events.topic");
MessageProducer p = session.createProducer(topic);
p.send(session.createTextMessage("用户注册事件"));

// 非持久订阅：只收到订阅期间的
MessageConsumer sub1 = session.createConsumer(topic);
// 持久订阅：离线期间消息保留，重连后收到
TopicSubscriber durableSub = session.createDurableSubscriber(
        topic, "order-service-sub");
```

### 1.3 虚拟 Topic（Virtual Topic）

```java
// ⚠️ ActiveMQ 独有：虚拟 Topic——Queue 的竞争负载 + Topic 的广播
// 生产者发到 VirtualTopic.Orders
Topic virtualTopic = session.createTopic("VirtualTopic.Orders");
producer.send(virtualTopic, msg);

// 消费者从 Queue 消费（实现广播 + 负载均衡）
Queue q1 = session.createQueue("Consumer.A.VirtualTopic.Orders");
Queue q2 = session.createQueue("Consumer.B.VirtualTopic.Orders");
// → A 的多个实例共享 q1（负载均衡）、B 独立消费 q2
```

---

## 2. 持久化：KahaDB 与 JDBC 存储

### 2.1 三种存储方式

| 方式 | 原理 | 性能 | 适用 |
|------|------|:---:|------|
| KahaDB（默认） | 日志文件 + B-tree 索引 | 高 | Classic 默认 |
| JDBC | 数据库存储 | 中 | 主从共享存储 |
| LevelDB（ZooKeeper） | 副本集群 | 高 | 集群高可用 |

### 2.2 KahaDB 原理

```
KahaDB = ActiveMQ Classic 默认的持久化引擎
  消息写入顺序日志文件（.log）
  索引存 B-tree（快速定位)
  定期清理已消费日志（checkpoint）

⚠️ 面试必答：
"KahaDB = 顺序写日志 + B-tree 索引——
 类似 PG 的 WAL 与 MySQL 的 binlog；
 写入性能高（顺序追加），恢复时重放日志。"
```

```xml
<!-- ActiveMQ broker xml 中配置 KahaDB -->
<persistenceAdapter>
    <kahaDB directory="${activemq.data}/kahadb"
            journalMaxFileLength="32mb"
            checkForCorruptJournalFiles="true" />
</persistenceAdapter>
```

### 2.3 JDBC 存储

```xml
<!-- JDBC 存储：适合主从共享存储场景 -->
<persistenceAdapter>
    <jdbcPersistenceAdapter dataSource="#mysql-ds"
                            createTablesOnStartup="true" />
</persistenceAdapter>
<!-- 多 Broker 竞争同一 DB 锁 → 自动主从切换 -->
```

---

## 3. 确认机制 ACK：四种模式的完整对比

### 3.1 四种模式

| 模式 | 行为 | 丢失风险 | 重复风险 | 适用 |
|------|------|:---:|:---:|------|
| AUTO_ACKNOWLEDGE | `receive()` 返回即确认 | ⚠️ 高 | 低 | 不推荐生产 |
| CLIENT_ACKNOWLEDGE | 显式调用 `msg.acknowledge()` | 低 | ⚠️ 中 | ✅ 生产推荐 |
| DUPS_OK_ACKNOWLEDGE | 批量延迟确认 | 低 | ⚠️ 高 | 可容忍重复 |
| SESSION_TRANSACTED | 事务提交确认 | 最低 | 低 | ✅ 关键业务 |

### 3.2 ACK 的坑与实战

```java
// ⚠️ 坑1：AUTO_ACK 模式下 receive() 返回即确认
// 业务尚未处理，进程崩了 → 消息永丢！
// 解决方案：用 CLIENT_ACK
Session session = conn.createSession(false, Session.CLIENT_ACKNOWLEDGE);
TextMessage msg = (TextMessage) consumer.receive(5000);
try {
    processOrder(msg.getText());      // 业务处理
    msg.acknowledge();                // ⚠️ 成功才确认
} catch (Exception e) {
    // 未 ack 的消息 → Broker 重投
}

// ⚠️ 坑2：批量消费时 ack 影响整批
// CLIENT_ACK 调用 msg.acknowledge() 确认本消息
// 及之前所有已接收但未 ack 的消息！
// 解决：单条处理 + 逐条 ack，或用事务模式逐条提交
Session txSession = conn.createSession(true, Session.SESSION_TRANSACTED);
for (int i = 0; i < 10; i++) {
    Message m = consumer.receive();
    processSingle(m);                 // 逐条处理
    txSession.commit();               // ⚠️ 逐条提交
}

// ⚠️ 坑3：DUPS_OK 异步写磁盘
// 性能高但可能重复 → 消费端必须幂等
```

---

## 4. JMS 事务：本地事务与 JTA 分布式

### 4.1 本地事务完整流程

```java
// JMS 本地事务：同 Session 内消息原子性
Session txSession = conn.createSession(true, Session.SESSION_TRANSACTED);
MessageProducer p = txSession.createProducer(queue);
MessageConsumer c = txSession.createConsumer(queue);
c.setMessageListener(msg -> {
    // 消费一条 + 发送一条（原子）
    p.send(txSession.createTextMessage("processed: " + msg));
    // 异常 → 整个 session 回滚 → 消费的消息回队列 + 发送的消息撤销
});
txSession.commit();

// 事务超时自动回滚（防止阻塞）
txSession.setTransactionTimeout(30);  // 30 秒
```

### 4.2 分布式事务实战（JTA + Spring）

```java
// ⚠️ 最完整的可靠方案：JDBC + JMS 在同一个事务
@Service
@Transactional(rollbackFor = Exception.class)
public class OrderService {

    private final JmsTemplate jms;
    private final OrderRepository repo;

    public void createOrder(OrderDTO dto) {
        // ① 写数据库
        Order order = repo.save(dto.toEntity());

        // ② 发消息（与数据库操作同事务）
        jms.convertAndSend("order.created", order);

        // 若 ③ 之后抛异常 → 数据库回滚 + 消息撤销！
    }
}
// ⚠️ 需要 JTA 事务管理器（Atomikos/Bitronix/Narayana）
```

### 4.3 事务 vs 非事务的性能对比

| 维度 | 非事务（AUTO_ACK） | 事务（SESSION_TRANSACTED） |
|------|:---:|:---:|
| 吞吐 | 最高 | 中（提交开销） |
| 可靠性 | 低 | ✅ 最高 |
| 适用 | 日志/通知（丢得起） | 订单/支付（丢不起） |
| 批处理优化 | — | 批量提交（N 条一提交） |

---

## 5. 死信队列与重投策略

### 5.1 死信形成的完整条件

```
消息进 DLQ 的五种触发：
  ① 消息 TTL 到期（TimeToLive 超时）
  ② 消费者 session.recover() 或 rollback()
  ③ 重投次数超 maxRedeliveries
  ④ 队列满了（producerFlowControl=true 下）
  ⑤ 消费者显式拒绝（Artemis 支持，Classic 不支持）

⚠️ 面试必答：
"DLQ 不是'丢了'——消息还在，只是标识为
 处理失败的'待人工检查'队列；
 监控 DLQ 是运维第一要务。"
```

### 5.2 完整重投策略配置

```java
// ⚠️ 生产推荐配置（指数退避 + 最大重投 + DLQ 兜底）
@Bean
public RedeliveryPolicy redeliveryPolicy() {
    RedeliveryPolicy policy = new RedeliveryPolicy();
    // ① 重投次数上限（到达上限 → DLQ）
    policy.setMaximumRedeliveries(5);

    // ② 指数退避：5s → 10s → 20s → 40s → 60s
    policy.setInitialRedeliveryDelay(5000);   // 首次 5 秒
    policy.setBackOffMultiplier(2.0);          // ×2 递增
    policy.setUseExponentialBackOff(true);
    policy.setMaximumRedeliveryDelay(60000);   // 上限 60 秒

    // ③ 不重投异常（白名单：某些异常不需要重试）
    policy.setNonBlockingRedelivery(true);     // 异步重投

    return policy;
}

// ④ 自定义 DLQ：不同队列不同死信策略
@Bean
public RedeliveryPolicy orderRedeliveryPolicy() {
    // 订单队列：最多重投 3 次（及时性要求高）
    RedeliveryPolicy policy = new RedeliveryPolicy();
    policy.setMaximumRedeliveries(3);
    policy.setInitialRedeliveryDelay(2000);

    // 自定义死信队列（非默认 ActiveMQ.DLQ）
    ActiveMQConnectionFactory cf = (ActiveMQConnectionFactory) factory;
    RedeliveryPolicyMap map = cf.getRedeliveryPolicyMap();
    map.put(new ActiveMQQueue("order.created.>"), policy);  // 通配匹配
    return policy;
}
```

---

## 6. 消息选择器与分组

### 6.1 消息选择器（过滤器）

```java
// 生产者：消息携带属性
TextMessage msg = session.createTextMessage(orderJson);
msg.setStringProperty("region", "east");
msg.setIntProperty("priority", 9);
producer.send(msg);

// 消费者：只用选择器接收自己关心的
String selector = "region = 'east' AND priority >= 5";
MessageConsumer c = session.createConsumer(queue, selector);
// ⚠️ 选择器在 Broker 侧过滤 → 网络传输只有匹配的消息
// 代价：Broker CPU 开销（大量消息时需评估）
```

### 6.2 消息分组（Message Group）

```java
// JMSXGroupID：同一组消息发给同一个消费者（保序）
TextMessage msg1 = session.createTextMessage(data1);
msg1.setStringProperty("JMSXGroupID", "order-1001");  // ⚠️ 分组键

TextMessage msg2 = session.createTextMessage(data2);
msg2.setStringProperty("JMSXGroupID", "order-1001");
// → 同 order-1001 的消息始终被同一消费者处理（保序）

// 场景：同一订单的创建/支付/发货消息必须按序处理
```

### 6.3 消息属性 vs 消息体

```
⚠️ 选择器只能用消息属性过滤（不能匹配消息体）
  ✅ msg.setStringProperty("type", "order")
  ❌ 根据消息体 JSON 内容过滤

属性存储开销：属性存在 Broker 内存索引中
  → 不要每条消息带大量属性（影响性能）
```

---

## 7. 与 RabbitMQ/Kafka 的可靠性对标

| 维度 | ActiveMQ | RabbitMQ | Kafka |
|------|:---:|:---:|:---:|
| ACK | 4 种模式（灵活） | manual auto | 偏移提交 |
| 持久化 | KahaDB/JDBC | 磁盘 + 镜像队列 | 磁盘 + 副本 |
| 事务 | ✅ JMS 本地 + JTA | ✅ AMQP TX | ❌ 无事务 |
| DLQ | ✅ 自动（配置简单） | ✅ 需配置 | ❌ 无内置 |
| 顺序 | Message Group | 单队列 | 分区内 |
| 重试 | 指数退避（内置） | 需插件 | 无内置重试 |
| 流式/日志 | ❌ 不适用 | ❌ 不适用 | ✅ 天然 |

```
⚠️ 面试必答：
"可靠性对比——ActiveMQ 强在 JMS 标准实现（ACK/事务/DLQ
 全内置）、RabbitMQ 强在低延迟 + AMQP 生态、
 Kafka 强在高吞吐 + 持久化 + 重放。
 小团队业务解耦：RabbitMQ；
 大吞吐流式：Kafka；
 JMS 生态兼容：ActiveMQ。"
```

---

> 🎯 **核心要点**：JMS 可靠性 = **Queue/Topic 全场景**（含 Virtual Topic 广播+负载均衡）+ **三种存储**（KahaDB/JDBC/LevelDB）+ **四种 ACK**（AUTO/CLIENT/DUPS/SESSION，逐条 ack 防批量丢失）+ **JTA 分布式事务**（JDBC+JMS 原子性）+ **DLQ 策略**（指数退避重投 + 不同队列不同死信）。生产顺序 = PERSISTENT → CLIENT_ACK → DLQ → 消费幂等——四道防线完整落地。

---

**返回总览**：[00-ActiveMQ总览与核心概念](00-ActiveMQ总览与核心概念.md) | **下一篇**：[02-SpringJMS集成](02-SpringJMS集成.md)
