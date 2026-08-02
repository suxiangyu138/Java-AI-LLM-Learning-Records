# 02 - Spring JMS 集成

> 定位：JmsTemplate 完整 API、@JmsListener 深度配置（容器/线程/事务）、消息转换器的类型安全与 MappingJackson 原理、连接池配置、同步/异步发送对比、请求-应答模式——Spring JMS 从基础到生产的完整实践

## 📚 目录

1. [JmsTemplate 完整能力](#1-jmstemplate-完整能力)
2. [连接管理：单连接 vs 连接池](#2-连接管理单连接-vs-连接池)
3. [@JmsListener 深度配置](#3-jmslistener-深度配置)
4. [消息转换器原理与类型安全](#4-消息转换器原理与类型安全)
5. [同步与异步发送](#5-同步与异步发送)
6. [请求-应答模式](#6-请求-应答模式)

---

## 1. JmsTemplate 完整能力

### 1.1 五种发送方式

```java
@Service
public class MessageProducer {

    private final JmsTemplate jms;

    // ① 匿名内部类（最灵活）
    public void sendRaw(String json) {
        jms.send(session -> {
            TextMessage msg = session.createTextMessage(json);
            msg.setStringProperty("type", "order");
            msg.setIntProperty("priority", 9);
            return msg;
        });
    }

    // ② convertAndSend（自动 JSON 转换，最常用）
    public void sendObject(OrderDTO order) {
        jms.convertAndSend("orders.queue", order);
    }

    // ③ convertAndSend + 后处理器（设置 JMS 属性）
    public void sendWithHeaders(OrderDTO order) {
        jms.convertAndSend("orders.queue", order, msg -> {
            msg.setStringProperty("region", "east");
            msg.setJMSPriority(9);
            return msg;
        });
    }

    // ④ 指定 Destination 对象（非字符串）
    public void sendToQueue(Queue queue, OrderDTO order) {
        jms.convertAndSend(queue, order);
    }

    // ⑤ execute：获取原生 Session（批量发送/手动控制）
    public void batchSend(List<OrderDTO> orders) {
        jms.execute(session -> {
            MessageProducer p = session.createProducer(
                    session.createQueue("orders.batch"));
            for (OrderDTO o : orders) {
                p.send(session.createTextMessage(toJson(o)));
            }
            return null;   // ⚠️ Void 用 null
        }, true);          // 启动连接
    }
}
```

### 1.2 JmsTemplate 配置全解

```java
@Bean
public JmsTemplate jmsTemplate(ConnectionFactory factory) {
    JmsTemplate t = new JmsTemplate(factory);

    // ===== 发送配置 =====
    t.setDefaultDestinationName("orders.queue");     // 默认目的地
    t.setDeliveryMode(DeliveryMode.PERSISTENT);      // 持久化
    t.setTimeToLive(60_000);                         // 60 秒过期
    t.setPriority(4);                                // 优先级（1-9）

    // ===== 接收配置 =====
    t.setReceiveTimeout(5_000);                      // 接收超时 5 秒

    // ===== 会话配置 =====
    t.setSessionTransacted(false);                   // 不启用事务（监听器开启更合适）
    t.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);
    t.setExplicitQosEnabled(true);                   // ⚠️ 启用 QoS（优先级/TTL/DeliveryMode 才生效）

    // ===== 消息转换器 =====
    t.setMessageConverter(jacksonConverter());

    return t;
}
```

---

## 2. 连接管理：单连接 vs 连接池

### 2.1 连接池的必要性

```
⚠️ JMS Connection 是重量级对象（TCP 连接）
  每次发送新建 Connection → 慢 + 资源浪费
  JmsTemplate 默认缓存 Session 但 Connection 数量有限

连接池方案：
  ① ActiveMQ PooledConnectionFactory（ActiveMQ 原生池）
  ② spring.activemq.pool.enabled=true（Boot Starter 内置）
  ③ GenericObjectPool（Apache Commons Pool2）
```

```java
// ActiveMQ 原生连接池
@Bean
public ConnectionFactory pooledConnectionFactory() {
    ActiveMQConnectionFactory amq = new ActiveMQConnectionFactory(
            "failover:(tcp://mq1:61616,tcp://mq2:61616)");

    PooledConnectionFactory pool = new PooledConnectionFactory();
    pool.setConnectionFactory(amq);
    pool.setMaxConnections(20);        // ⚠️ 最大连接数
    pool.setMaximumActiveSessionPerConnection(50);  // 每连接 50 个 Session
    pool.setIdleTimeout(30000);        // 空闲 30 秒回收
    pool.setBlockIfSessionPoolIsFull(true);  // 池满阻塞等待
    pool.setBlockIfSessionPoolIsFullTimeout(5000);
    return pool;
}
```

### 2.2 连接池参数调优

| 参数 | 建议值 | 说明 |
|------|:---:|------|
| maxConnections | CPU 核心数 × 2 | 过多连接反增网络开销 |
| maxActiveSessionPerConnection | 10-50 | 过大 → Broker 连接数压力 |
| idleTimeout | 30000ms | 超时回收可节省 Broker 连接数 |
| blockIfFull | true | 池满等待而非抛异常（可靠性优先） |

---

## 3. @JmsListener 深度配置

### 3.1 容器工厂高级配置

```java
@Bean
public DefaultJmsListenerContainerFactory jmsFactory(ConnectionFactory cf) {
    DefaultJmsListenerContainerFactory f = new DefaultJmsListenerContainerFactory();
    f.setConnectionFactory(cf);

    // ===== 并发控制 =====
    f.setConcurrency("3-10");              // 3 常驻、最大 10
    f.setMaxMessagesPerTask(1);            // ⚠️ 每次取一条（处理完再取，防堆积）
    f.setReceiveTimeout(5000);             // 接收等待超时

    // ===== 事务 =====
    f.setSessionTransacted(true);          // ⚠️ 监听器事务（异常回滚）

    // ===== 确认 =====
    f.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);

    // ===== 订阅持久性 =====
    f.setSubscriptionDurable(true);        // Topic 持久订阅
    f.setDurableSubscriptionName("order-sub");

    // ===== 错误处理 =====
    f.setErrorHandler(t -> {
        log.error("JMS 监听器异常", t.getCause());
        // 可在此自定义：告警/降级/人工处理
    });

    // ===== 连接缓存 =====
    f.setCacheLevel(DefaultMessageListenerContainer.CACHE_CONSUMER);  // 共享消费者
    return f;
}
```

### 3.2 监听器完整注解示例

```java
@Component
public class OrderListener {

    // ① 基础：destination + 自动 JSON 反序列化
    @JmsListener(destination = "orders.queue", containerFactory = "jmsFactory")
    public void handle(OrderDTO order) {
        // order 由 MappingJackson2MessageConverter 自动转换
    }

    // ② 手动 ACK + 原生 Message
    @JmsListener(destination = "orders.critical")
    public void handleManual(Message message, Session session) throws JMSException {
        try {
            process(message);
            message.acknowledge();        // ⚠️ 成功才确认
        } catch (Exception e) {
            session.recover();            // 回滚 → 重投
        }
    }

    // ③ 选择器过滤（只消费指定属性消息）
    @JmsListener(destination = "orders.priority",
                 selector = "priority = 9 AND region = 'east'")
    public void handlePriority(OrderDTO order) { }

    // ④ Topic 持久订阅
    @JmsListener(destination = "events.topic",
                 containerFactory = "topicFactory",
                 subscription = "order-service-sub")
    public void handleEvent(EventDTO event) { }

    // ⑤ 并发专属（单独容器工厂，独立线程池）
    @JmsListener(destination = "orders.slow",
                 containerFactory = "slowConsumerFactory")
    public void handleSlow(OrderDTO order) {
        Thread.sleep(100);   // 模拟慢消费
    }
}
```

---

## 4. 消息转换器原理与类型安全

### 4.1 MappingJackson2 原理

```
MappingJackson2MessageConverter 的工作流程：
  发送端：Java 对象 → Jackson 序列化 → JSON 字符串 → TextMessage
  接收端：TextMessage → JSON 字符串 → _type 属性定位 Java 类 → Jackson 反序列化

⚠️ _type 是类型映射的关键：
  发消息时自动写入 msg.setStringProperty("_type", "com.example.OrderDTO")
  收消息时根据 _type 反序列化为正确的 Java 类型
```

```java
@Bean
public MessageConverter jacksonConverter() {
    MappingJackson2MessageConverter c = new MappingJackson2MessageConverter();

    // ① 发送 TextMessage（兼容性好）
    c.setTargetType(MessageType.TEXT);
    c.setTypeIdPropertyName("_type");       // ⚠️ 类型标识属性名

    // ② 接收时：只信任白名单包（安全）
    c.setTrustedPackages(
            "java.util",
            "java.lang",
            "com.example.dto",
            "com.example.event"
    );

    return c;
}
```

### 4.2 多类型转换实现

```java
// ⚠️ 同队列发送多种类型消息时：手动设置类型
jms.convertAndSend("events.queue", new OrderCreatedEvent(1L), msg -> {
    msg.setStringProperty("_type", OrderCreatedEvent.class.getName());
    return msg;
});
jms.convertAndSend("events.queue", new PaymentCompletedEvent(2L), msg -> {
    msg.setStringProperty("_type", PaymentCompletedEvent.class.getName());
    return msg;
});

// 消费者端指定泛型接收 Object 再 instanceof 分派
@JmsListener(destination = "events.queue")
public void dispatch(Message msg) throws JMSException {
    String type = msg.getStringProperty("_type");
    Object payload = converter.fromMessage(msg);
    if (payload instanceof OrderCreatedEvent) { handle((OrderCreatedEvent) payload); }
    // ...
}
```

---

## 5. 同步与异步发送

### 5.1 同步发送（默认，确认 Broke 收到）

```java
// JmsTemplate 默认同步（等待 Broker 确认）
jms.convertAndSend("orders.queue", order);
// → 返回时 Broker 已接收（持久化消息已写磁盘）

// 性能调优：异步发送（不等待确认，较快但有丢失风险）
ActiveMQConnectionFactory amq = (ActiveMQConnectionFactory) factory;
amq.setUseAsyncSend(true);              // ⚠️ Broker DMA 异常时丢消息
// 适用：日志/通知类可承受丢失的场景
```

### 5.2 同步 vs 异步选型

| 维度 | 同步（默认） | 异步 |
|------|:---:|:---:|
| 可靠性 | ✅ Broker 确认 | ⚠️ 可能丢 |
| 延迟 | 较高（等确认） | ✅ 低 |
| 吞吐 | 中 | ✅ 高 |
| 适用 | 关键业务 | 日志/通知/指标 |

```
⚠️ 面试必答：
"同步（等 Broker 确认，可靠）、
 异步（不等确认，快但可能丢）——
 关键业务（订单/支付）必须同步发送；
 日志/通知用异步 + 缓冲批量发送。"
```

---

## 6. 请求-应答模式

### 6.1 实现（JmsTemplate + TemporaryQueue）

```java
// 同步请求-应答（类似 HTTP 请求，但通过 MQ）
public OrderVO requestReply(OrderDTO request) {
    return jms.execute(session -> {
        // ① 临时队列（当前会话专用，用完即删）
        TemporaryQueue replyQueue = session.createTemporaryQueue();

        // ② 发送请求（含临时队列为回复地址）
        TextMessage msg = session.createTextMessage(toJson(request));
        msg.setJMSReplyTo(replyQueue);       // ⚠️ 回调地址
        msg.setJMSCorrelationID(UUID.randomUUID().toString());
        producer.send(msg);

        // ③ 等回复（阻塞，超时 30 秒）
        MessageConsumer replyConsumer = session.createConsumer(replyQueue);
        Message reply = replyConsumer.receive(30_000);
        return parseResponse((TextMessage) reply);
    }, true);
}

// 消费者：处理 + 回复
@JmsListener(destination = "orders.rpc")
public Message handleRpc(Message request, Session session) throws JMSException {
    Destination replyTo = request.getJMSReplyTo();
    String correlationId = request.getJMSCorrelationID();

    TextMessage reply = session.createTextMessage(process(request));
    reply.setJMSCorrelationID(correlationId);  // ⚠️ 关联请求
    session.createProducer(replyTo).send(reply);
    return reply;
}
```

> 🎯 **要点**：请求-应答 = JMS 版的 RPC——通过 `JMSReplyTo` + `JMSCorrelationID` 实现同步调用语义。临时队列生命周期随会话销毁，免清理。

---

> 🎯 **核心要点**：Spring JMS = **JmsTemplate 五种发送**（raw/convert/header/Destination/execute）+ **连接池**（PooledConnectionFactory 调优）+ **@JmsListener 深度配置**（容器工厂/并发/事务/ACK/错误处理）+ **类型安全转换**（MappingJackson2 + _type + 白名单）+ **同步 vs 异步发送**（可靠性 vs 性能）+ **请求-应答**（JMSReplyTo + 临时队列 RPC）。

---

**返回总览**：[00-ActiveMQ总览与核心概念](00-ActiveMQ总览与核心概念.md) | **上一篇**：[01-JMS消息模型与可靠性](01-JMS消息模型与可靠性.md) | **下一篇**：[03-SpringBoot集成](03-SpringBoot集成.md)
