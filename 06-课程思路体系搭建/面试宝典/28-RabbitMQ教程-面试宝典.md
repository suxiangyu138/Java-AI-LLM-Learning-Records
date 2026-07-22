# RabbitMQ 面试宝典
> 基于课程大纲全面覆盖面试高频考点，从消息队列基础到分布式高可用实战

## 目录
1. [一、基础概念速答（20题）](#一基础概念速答20题)
2. [二、深度原理剖析（12题）](#二深度原理剖析12题)
3. [三、实战场景题（10题）](#三实战场景题10题)
4. [四、手写代码/配置文件题（6题）](#四手写代码配置文件题6题)
5. [五、系统设计题（4题）](#五系统设计题4题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板（Top 5）](#七面试回答模板top-5)
8. [八、快速查漏补缺 Checklist](#八快速查漏补缺-checklist)

---

## 一、基础概念速答（20题）

### 1.1 什么是消息队列（MQ）？为什么要用 MQ？
消息队列（Message Queue）是一种**异步通信中间件**，用于解耦生产者和消费者。核心作用是**异步处理、流量削峰、系统解耦**。

> 💡 面试回答时可以用"同步调用 vs 异步调用"的对比来引出 MQ 的价值。

### 1.2 同步调用和异步调用的区别？

| 特性 | 同步调用 | 异步调用（MQ） |
|------|---------|---------------|
| 响应时间 | 等待所有服务返回 | 立即返回 |
| 耦合度 | 强耦合（调用方依赖被调用方） | 松耦合（通过 MQ 间接通信） |
| 可用性 | 下游宕机导致上游失败 | 下游宕机不影响上游（消息积压） |
| 流量控制 | 无法削峰，突发流量压垮系统 | 消息缓冲，削峰填谷 |

### 1.3 RabbitMQ vs Kafka vs RocketMQ 如何选型？

| 维度 | RabbitMQ | Kafka | RocketMQ |
|------|----------|-------|----------|
| 开发语言 | Erlang | Scala/Java | Java |
| 吞吐量 | 万级/秒 | 百万级/秒 | 十万级/秒 |
| 消息可靠性 | 高（Confirm + 持久化） | 高（副本机制） | 高（同步刷盘） |
| 延迟 | 微秒级 | 毫秒级 | 毫秒级 |
| 路由能力 | 强大（多种 Exchange） | 弱（基于 Topic） | 一般（基于 Topic + Tag） |
| 适用场景 | 企业级应用、事务消息、复杂路由 | 日志采集、大数据流处理、埋点 | 金融级、分布式事务、削峰 |

> 🎯 阿里/字节高频题：**"你的项目为什么选 RabbitMQ 而不是 Kafka？"** 回答关键：业务需要灵活路由、延时消息、死信队列等特性，且吞吐量要求未达到百万级。

### 1.4 RabbitMQ 的 Exchange 类型有哪些？

| 类型 | 路由规则 | 使用场景 |
|------|---------|---------|
| **Direct** | Routing Key 精确匹配 | 点对点、单播 |
| **Fanout** | 广播到所有绑定的 Queue | 广播通知、全局消息 |
| **Topic** | Routing Key 通配符匹配（`#` 匹配多级，`*` 匹配一级） | 按主题分类的消息路由 |
| **Headers** | 根据消息 Headers 属性匹配（忽略 Routing Key） | 复杂条件路由（极少使用） |

```java
// Direct Exchange 示例
channel.exchangeDeclare("direct.exchange", BuiltinExchangeType.DIRECT);
channel.queueBind("queue.order", "direct.exchange", "order.create");
// 只有 routing key = "order.create" 的消息才会被路由到 queue.order
```

> ⚠️ 面试高频：**Topic Exchange 中 `#` 和 `*` 的区别**——`#` 匹配零个或多个单词，`*` 匹配一个单词。

### 1.5 Connection 与 Channel 有什么区别？

| 概念 | 说明 |
|------|------|
| **Connection** | TCP 长连接，一个 Connection 对应一个 TCP 连接 |
| **Channel** | 虚拟连接，一个 Connection 可创建多个 Channel |
| 关系 | Channel 复用 Connection 的 TCP 连接，减少连接开销 |

> 💡 AMQP 协议的核心优化思想：**多路复用**。一个 TCP 连接上开辟多个轻量级 Channel，每个 Channel 代表一个会话任务。

### 1.6 什么是 Virtual Host（vhost）？

Vhost 是 RabbitMQ 的**数据隔离单元**，类似于命名空间。不同 vhost 之间 Exchange、Queue、Binding 完全隔离。一个 RabbitMQ 服务器可以创建多个 vhost，常用于多环境（dev/test/prod）隔离或多租户场景。

```bash
# 创建 vhost
rabbitmqctl add_vhost /my_vhost
# 设置权限
rabbitmqctl set_permissions -p /my_vhost my_user ".*" ".*" ".*"
```

### 1.7 什么是 Binding？Binding 与 Exchange、Queue 的关系？

Binding 是**将 Queue 绑定到 Exchange 的规则**，指定了消息的路由方式。关系如下：

```
Producer → Exchange（根据 Routing Key / Headers）
                ↓ (Binding)
            Queue（绑定关系决定消息流向）
                ↓
           Consumer（消费消息）
```

### 1.8 RabbitMQ 支持的 Queue 类型有哪些？

| 队列类型 | 特性 | 适用场景 |
|---------|------|---------|
| **Classic Queue** | 传统队列，支持镜像队列高可用 | 通用场景 |
| **Quorum Queue** | 基于 Raft 协议的强一致性队列 | 数据一致性要求高的场景 |
| **Stream Queue** | 只追加的日志型队列，支持回溯消费 | 日志收集、事件溯源 |
| **Lazy Queue** | 消息直接存磁盘，内存占用低（3.6+ 引入，3.12+ 默认） | 消息积压、大消息场景 |

### 1.9 消息优先级（Priority）如何实现？

```java
// 1. 声明队列时设置最大优先级
Map<String, Object> args = new HashMap<>();
args.put("x-max-priority", 10);
channel.queueDeclare("priority.queue", true, false, false, args);

// 2. 发送消息时设置优先级
AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
    .priority(5) // 0-10，数值越大优先级越高
    .build();
channel.basicPublish("", "priority.queue", props, messageBody);
```

> ⚠️ 优先级队列的局限：只有消息堆积时优先级才生效；消费者空闲时优先级无意义。

### 1.10 消息 TTL 有哪两种设置方式？

| 设置级别 | 配置方式 | 生效范围 |
|---------|---------|---------|
| **队列 TTL** | `x-message-ttl` 参数 | 队列中所有消息的统一过期时间 |
| **消息 TTL** | `expiration` 属性 | 单条消息的过期时间 |

```java
// 队列级别 TTL
Map<String, Object> args = new HashMap<>();
args.put("x-message-ttl", 60000); // 60秒过期
channel.queueDeclare("ttl.queue", true, false, false, args);

// 消息级别 TTL
AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
    .expiration("60000") // 60秒过期
    .build();
channel.basicPublish("", "ttl.queue", props, messageBody);
```

> 🎯 如果同时设置两者，取**最小值**。消息到达队列头部才检查 TTL（不保证精确到期移除）。

### 1.11 什么是 Dead Letter Exchange（DLX）？

死信交换机（DLX）用于处理**无法被正常消费的消息**。消息变成死信的三种情况：
1. 消息被消费者拒绝且不重新入队（`basic.reject`/`basic.nack` 且 `requeue=false`）
2. 消息 TTL 过期
3. 队列达到最大长度（`x-max-length` 或 `x-max-length-bytes`）

```java
// 声明带有 DLX 的主队列
Map<String, Object> args = new HashMap<>();
args.put("x-dead-letter-exchange", "dlx.exchange");
args.put("x-dead-letter-routing-key", "dlx.routing.key");
channel.queueDeclare("main.queue", true, false, false, args);
```

### 1.12 什么是延迟消息？RabbitMQ 如何实现延迟消息？

延迟消息是指消息发送后**不立即被消费**，而是等待指定时间后才投递到消费者。实现方式：

| 方式 | 原理 | 优点 | 缺点 |
|------|------|------|------|
| **DLX + TTL** | 消息 TTL 过期后进入 DLX，消费者监听 DLQ | 无需额外插件 | 只能固定 TTL，先过期消息被阻塞 |
| **延迟消息插件** | `rabbitmq_delayed_message_exchange` | 灵活、精确到秒 | 需安装插件 |

> 💡 延迟消息最常见的面试题是**"订单超时未支付自动取消"**场景。

### 1.13 ACK 模式有哪几种？

| ACK 模式 | 说明 | 使用场景 |
|---------|------|---------|
| **自动确认（auto）** | 消息投递后立即确认 | 允许丢失消息的场景 |
| **手动确认（manual）** | 消费者处理完业务逻辑后手动调用 `basicAck` | 数据不能丢失的场景 |
| **根据异常确认（none）** | Spring 代理自动处理 | Spring 项目 |

```java
// 手动确认关键代码
channel.basicConsume(queueName, false, consumer); // autoAck=false
// 在 deliverCallback 中：
channel.basicAck(deliveryTag, false);    // 确认成功
channel.basicNack(deliveryTag, false, true);  // 失败且重新入队
channel.basicReject(deliveryTag, false);      // 拒绝且不重新入队
```

### 1.14 Prefetch Count 是什么？如何设置？

Prefetch Count 控制**消费者一次能接收多少条未确认消息**，用于流量控制和负载均衡。

```java
// 设置 prefetch count = 5，消费者一次最多拿 5 条消息
channel.basicQos(5);
```

> ⚠️ 面试重点：prefetch 设得太大导致消息倾斜（一个消费者处理慢，积压多）；设得太小降低吞吐量。推荐根据业务处理耗时调整，一般设置在 1-100 之间。

### 1.15 消息持久化包含哪些方面？

RabbitMQ 消息持久化需要三个维度同时设置为 true：

| 维度 | 配置 | 说明 |
|------|------|------|
| **交换机持久化** | `ExchangeDeclareSpec.durable(true)` | 交换机不丢失 |
| **队列持久化** | `QueueDeclareSpec.durable(true)` | 队列不丢失 |
| **消息持久化** | `MessageProperties.PERSISTENT_TEXT_PLAIN`（deliveryMode=2） | 消息不丢失 |

```java
// 完整持久化配置
channel.exchangeDeclare("exchange", "direct", true); // durable=true
channel.queueDeclare("queue", true, false, false, null); // durable=true
channel.basicPublish("exchange", "rk", 
    MessageProperties.PERSISTENT_TEXT_PLAIN, messageBody); // deliveryMode=2
```

> 🎯 高频陷阱：只配了队列持久化没配消息持久化，重启后队列在但消息没了！

### 1.16 Lazy Queue 是什么？和普通队列的区别？

Lazy Queue（3.6+ 引入，3.12+ 成为默认）将消息**尽可能存储到磁盘**，仅在消费者拉取时加载到内存。

| 对比维度 | 普通队列 | Lazy Queue |
|---------|---------|------------|
| 存储策略 | 尽量在内存 | 尽量在磁盘 |
| 内存压力 | 高（消息堆积时） | 低 |
| I/O 开销 | 低（正常流量） | 高 |
| 适用场景 | 低延迟、低积压 | 高积压、大消息 |

```bash
# 声明 Lazt Queue
rabbitmqctl set_policy lazy "^lazy-queue$" '{"queue-mode":"lazy"}' --apply-to queues
```

### 1.17 RabbitMQ 如何保证消息顺序性？

**关键点**：一个 Queue 内部是 FIFO 有序的，但多个 Consumer 并发消费时可能乱序。

| 方案 | 实现 | 缺点 |
|------|------|------|
| **单队列单消费者** | 一个 Queue 绑定一个 Consumer | 吞吐量受限 |
| **分区有序** | 同一业务 ID 的消息路由到同一个 Queue | 增加运维复杂度 |

> 💡 阿里面试高频：**"如何保证订单状态变更消息的顺序？"** 回答思路：用订单 ID 作为 routing key，确保同一订单的消息进入同一个 Queue。

### 1.18 什么是消息的幂等性？如何保证？

幂等性指**同一条消息被消费多次与消费一次的结果相同**。MQ 场景天然存在重复消息（生产者重发、网络抖动），必须在消费者端做幂等处理。

```java
// 幂等方案：唯一 ID + 状态表
if (processedTable.exists(messageId)) {
    // 已处理，直接 ACK 跳过
    channel.basicAck(deliveryTag, false);
    return;
}
// 执行业务逻辑
processMessage(message);
// 记录已处理
processedTable.save(messageId);
channel.basicAck(deliveryTag, false);
```

### 1.19 什么是 Confirm 模式？什么是 Return 机制？

| 机制 | 作用 | 触发时机 |
|------|------|---------|
| **Publisher Confirm** | 确认消息是否到达 Exchange | Exchange 收到消息后回调 |
| **Return** | 消息无法路由到 Queue 时通知生产者 | Exchange 发现无匹配 Queue |

```java
// 开启 Confirm
channel.confirmSelect();
channel.addConfirmListener((deliveryTag, multiple) -> {
    System.out.println("消息确认成功, tag: " + deliveryTag);
}, (deliveryTag, multiple) -> {
    System.out.println("消息确认失败, tag: " + deliveryTag);
});

// 开启 Return
channel.addReturnListener((replyCode, replyText, exchange, routingKey, properties, body) -> {
    System.out.println("消息无法路由, 返回: " + new String(body));
});
```

### 1.20 什么是 RabbitMQ 的 Shovel 和 Federation？

| 组件 | 功能 | 适用场景 |
|------|------|---------|
| **Shovel** | 单向数据同步（源端 → 目标端） | 跨机房数据迁移、灾备 |
| **Federation** | 双向/多向数据同步（上游 → 下游） | 分布式集群消息共享、跨地域部署 |

---

## 二、深度原理剖析（12题）

### 2.1 AMQP 协议模型是什么？

AMQP（Advanced Message Queuing Protocol）是一个**应用层标准协议**，定义了消息中间件的统一交互模型。

```
AMQP 模型层次：
Layer 0: 网络传输层（TCP/IP）
Layer 1: 协议协商层（Protocol Version Negotiation）
Layer 2: 会话管理层（Connection/Channel 管理）
Layer 3: 模型层（Exchange、Queue、Binding）
Layer 4: 数据传输层（Basic.Publish、Basic.Consume 方法）
```

> 🎯 关键亮点：AMQP 与 JMS 不同，它是**跨语言、跨平台**的线级协议，不同语言的客户端可以实现互操作。

### 2.2 消息的完整路由流程是怎样的？

```
Producer 发送消息
    → Channel.basicPublish()
        → Exchange 接收消息
            → Exchange 根据类型和 Routing Key 应用 Binding 规则
                → 匹配到一个或多个 Queue
                    → 消息存储到 Queue（持久化或内存）
                        → Consumer 拉取（Pull）或推送（Push）消息
                            → Consumer 处理并 ACK
                                → 消息从 Queue 删除
```

**关键点**：Exchange 只负责路由，不存储消息。Queue 负责存储，直到消费者确认消费。

### 2.3 Publisher Confirm 机制的原理？

Confirm 是 RabbitMQ 提供的一种**轻量级可靠投递**机制。原理如下：

1. 生产者开启 `confirmSelect`，Channel 进入 Confirm 模式
2. 每条消息分配一个递增的 `deliveryTag`
3. 消息到达 Exchange 后，异步回调 `basic.ack` 或 `basic.nack`
4. 生产者在回调中标记消息状态（已确认/已失败）

```java
// 批量确认模式（高吞吐）
channel.confirmSelect();
channel.basicPublish(exchange, routingKey, props, body);
if (channel.waitForConfirms(5000)) {
    // 消息确认成功
} else {
    // 消息确认失败，需要重发
}
```

> ⚠️ 面试常问：**Confirm 是同步阻塞还是异步回调？** 两者都支持。`waitForConfirms()` 是同步阻塞；`addConfirmListener()` 是异步回调。

### 2.4 Return 机制的触发条件和处理方式？

Return 机制是**消息无法路由到任何 Queue 时**的回调通知。触发条件：
- Exchange 收到了消息
- 但没有任何 Queue 匹配当前 Routing Key
- 且消息设置了 `mandatory=true`

```java
// 设置 mandatory=true
channel.basicPublish(exchange, routingKey, true, false, props, body);
// mandatory=true 表示无法路由时 Return；false 则直接丢弃
```

**Return + Confirm 组合使用**：Confirm 确认消息到达 Exchange，Return 处理消息无法路由到 Queue 的情况。两者配合实现端到端的可靠性。

### 2.5 消息可靠性投递的完整链路是什么？

```
生产者可靠性                  MQ 可靠性                 消费者可靠性
┌─────────────────┐    ┌───────────────────┐    ┌─────────────────┐
│ 1. 生产者重连    │    │ 1. 数据持久化      │    │ 1. 手动 ACK     │
│ 2. Confirm 确认  │ → │ 2. Lazy Queue     │ → │ 2. 重试机制     │
│ 3. Return 回调   │    │ 3. 镜像/仲裁队列   │    │ 3. 幂等性处理   │
└─────────────────┘    └───────────────────┘    └─────────────────┘
```

**完整保证策略**：
1. **生产者重连**：`ConnectionFactory` 设置自动恢复
2. **生产者确认**：Confirm 确认消息到达
3. **消息持久化**：Exchange、Queue、Message 三处 durable=true
4. **消费者 ACK**：手动确认，处理完成后才 ACK
5. **消费者重试**：Spring 的 `RetryTemplate` 或自定义重试
6. **业务幂等**：全局 ID 去重

### 2.6 死信队列（DLQ）的工作原理？

```
                    → TTL 过期 → [DLX] → [DLQ] → 死信消费者
Main Queue → 
                    → 拒绝且不重新入队 → [DLX] → [DLQ] → 死信消费者
                    
                    → 队列满 → [DLX] → [DLQ] → 死信消费者
```

```java
// 完整 DLQ 配置
@Bean
public Queue deadLetterQueue() {
    return QueueBuilder.durable("dlq.queue").build();
}

@Bean
public DirectExchange deadLetterExchange() {
    return new DirectExchange("dlx.exchange");
}

@Bean
public Binding deadLetterBinding() {
    return BindingBuilder.bind(deadLetterQueue())
        .to(deadLetterExchange()).with("dlx.routing.key");
}

@Bean
public Queue mainQueue() {
    return QueueBuilder.durable("main.queue")
        .deadLetterExchange("dlx.exchange")          // 指定 DLX
        .deadLetterRoutingKey("dlx.routing.key")     // 指定 Routing Key
        .ttl(60000)                                   // 消息 TTL
        .maxLength(10000)                             // 最大消息数
        .build();
}
```

### 2.7 延迟队列的实现原理（DLX + TTL vs 插件）？

| 对比维度 | DLX + TTL 方案 | 延迟消息插件方案 |
|---------|---------------|-----------------|
| 原理 | 消息在队列中等待 TTL 过期，进入 DLX | 消息在 Exchange 中存储，到期后路由 |
| 精确度 | 不精确（头部过期才移除） | 精确到秒级 |
| 灵活性 | 固定 TTL（除非创建多个 TTL 队列） | 每个消息可指定不同延迟时间 |
| 依赖 | 无需额外组件 | 需安装 `rabbitmq_delayed_message_exchange` |
| 性能 | 高 | 中（需要额外存储） |

**DLX + TTL 的陷阱**：RabbitMQ 只检查队列头部的消息是否过期。如果头部消息 TTL=60min，后面的消息 TTL=10min，后面的消息需要等 60min 才能被消费（头部阻塞问题）。

### 2.8 消费者 ACK 机制、重试机制与幂等性如何协同？

```
消息到达 → 消费者收到 → 处理业务逻辑
                            ↓
                    成功 → basicAck(deliveryTag)
                            ↓
                    失败 → basicNack(requeue=false)
                            ↓
                      进入重试队列
                            ↓
                      重试次数 ≤ 阈值 → 重新消费
                            ↓
                      重试次数 > 阈值 → 进入 DLQ
                            ↓
                        死信消费者处理/告警
```

**幂等性保护**：由于生产者可能重发、MQ 可能重复投递，消费者必须幂等。
- 方案一：**唯一消息 ID**（业务唯一键去重）
- 方案二：**状态机**（校验当前状态是否允许处理）

### 2.9 Spring AMQP 的 RabbitTemplate 和 @RabbitListener 原理？

**RabbitTemplate**：
- 封装了 Channel 操作，提供 `convertAndSend`、`receiveAndConvert` 等方法
- 内置 `MessageConverter`（默认 SimpleMessageConverter，支持 Java 序列化）
- 支持 `setConfirmCallback` 和 `setReturnsCallback`

**@RabbitListener**：
- 底层由 `RabbitListenerAnnotationBeanPostProcessor` 解析注解
- 注册到 `SimpleMessageListenerContainer` 或 `DirectMessageListenerContainer`
- 消费者线程模型：Container 管理多个 Consumer 线程，自动执行 ACK

```java
// RabbitTemplate 配置
@Bean
public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(new Jackson2JsonMessageConverter());
    template.setConfirmCallback((correlationData, ack, cause) -> {
        if (!ack) {
            log.error("消息发送失败: {}", cause);
            // 补偿重试
        }
    });
    template.setMandatory(true);
    template.setReturnsCallback(returned -> {
        log.error("消息路由失败: {}", returned.getMessage());
    });
    return template;
}
```

### 2.10 Lazy Queue 的内部机制？

Lazy Queue 的核心优化是将消息**优先写入磁盘**而非内存。内部实现：

1. 消息到达 Queue 后，直接追加写入磁盘文件（segment）
2. 内存中只维护索引信息（offset、size、messageCount）
3. Consumer 拉取时，从磁盘读取消息到内存
4. 支持**批量预读取**（提高 I/O 效率）
5. 磁盘满时支持**流式垃圾回收**（删除已确认消息的 segment）

> 🎯 性能数据：Lazy Queue 单节点写入吞吐量约 15-30K msg/s，内存占用降低 90%+。

### 2.11 RabbitMQ 集群模式对比？

| 模式 | 实现方式 | 数据一致性 | 可用性 | 适用场景 |
|------|---------|-----------|-------|---------|
| **普通集群** | 元数据同步，数据分散 | 最终一致 | 一般（节点宕机丢数据） | 开发环境 |
| **镜像队列** | 主从复制，所有节点同步 | 强一致 | 高（自动故障转移） | 生产环境（3.8 前） |
| **仲裁队列** | Raft 协议，多数派写入 | 强一致 | 高 | 生产环境（3.8+ 推荐） |
| **Federation** | 拉取模式，跨集群同步 | 最终一致 | 高 | 跨地域部署 |
| **Shovel** | 推模式，点对点同步 | 最终一致 | 高 | 数据迁移/灾备 |

```bash
# 配置普通集群
rabbitmqctl stop_app
rabbitmqctl join_cluster rabbit@node2
rabbitmqctl start_app

# 配置镜像队列策略
rabbitmqctl set_policy ha-all "^" '{"ha-mode":"all","ha-sync-mode":"automatic"}'
```

> ⚠️ 面试重点：**镜像队列和仲裁队列的区别**。仲裁队列（Quorum Queue）是 3.8 引入的替代镜像的推荐方案，基于 Raft 实现，更可靠、更简单。

### 2.12 流量控制（Flow Control）机制？

RabbitMQ 流控基于**信用证算法（Credit-Based Flow Control）**，防止生产者发送速度超过 Broker 处理能力。

```
Broker 内存/磁盘达到阈值
    → 降低/暂停 Connection 的 credit
        → 生产者发送速度受限
            → Broker 恢复
                → 恢复 credit
                    → 生产者恢复发送
```

```bash
# 查看内存和磁盘阈值
rabbitmqctl status | grep "disk_free_limit\|vm_memory_high_watermark"

# 设置内存阈值（默认 0.4，即物理内存的 40%）
rabbitmqctl set_vm_memory_high_watermark 0.6
```

> 💡 流控触发条件：内存超过阈值、磁盘剩余空间不足、未确认消息过多。

---

## 三、实战场景题（10题）

### 3.1 订单超时未支付自动取消（DLQ + TTL）

```
用户下单 → 发送延迟消息（TTL=30min）
                ↓
        订单服务收到消息 → 查询订单状态
                ↓
        ┌── 已支付 → 确认消费，结束
        └── 未支付 → 调用取消订单 API
                        ↓
                    恢复库存 → 发送取消通知
```

```java
// 发送延迟检测消息
rabbitTemplate.convertAndSend("order.delay.exchange", "order.timeout", 
    orderId, message -> {
        message.getMessageProperties().setExpiration("1800000"); // 30min
        return message;
    });

// 消费延迟消息
@RabbitListener(queues = "order.delay.queue")
public void handleOrderTimeout(String orderId) {
    Order order = orderService.getOrder(orderId);
    if (order.getStatus() == OrderStatus.UNPAID) {
        orderService.cancel(orderId);
        log.info("订单 {} 超时未支付，已取消", orderId);
    }
}
```

### 3.2 秒杀/抢购流量削峰

```
客户端请求 → Nginx 限流 → 业务校验
                              ↓
                    发送 MQ（秒杀令牌）
                              ↓
                消费者异步处理（串行化）
                              ↓
                    Redis 扣减库存
                              ↓
                       数据库落单
```

```java
// 秒杀接口（生产者）
public Result seckill(Long userId, Long productId) {
    // 1. 预检：Redis 库存 + 用户限购
    if (!stockPreCheck(userId, productId)) {
        return Result.fail("库存不足或已限购");
    }
    // 2. 发送 MQ 排队（100/s 的消费能力保护数据库）
    SeckillMessage msg = new SeckillMessage(userId, productId);
    rabbitTemplate.convertAndSend("seckill.exchange", "seckill", msg);
    return Result.success("排队中，请稍后查看结果");
}

// 秒杀消费者（控制在 100 TPS 内处理）
@RabbitListener(queues = "seckill.queue", concurrency = "1-5")
public void processSeckill(SeckillMessage msg) {
    // 使用 Redis 分布式锁防止超卖
    boolean locked = redisLock.tryLock("seckill:" + msg.getProductId(), 1000);
    if (!locked) return;
    try {
        // 扣库存、生成订单、发送结果通知
        orderService.createSeckillOrder(msg);
    } finally {
        redisLock.unlock("seckill:" + msg.getProductId());
    }
}
```

### 3.3 分布式事务的可靠消息最终一致性方案

```
        ┌──────────────────── 可靠消息服务 ────────────────────┐
业务服务 A ─→ 1. 发送半消息 ─→ 2. 确认消息 ─→ 投递消息 ─→ 业务服务 B
    ↑              ↓                    ↓                     ↓
    └── 3. 回查 ──┘    消息表(未确认)  消息表(已确认)     执行本地事务
```

```java
// 可靠消息最终一致性（TCC 思想 + MQ）
// 步骤 1：预发送消息（半消息状态）
@Transactional
public void createOrderWithMessage(Order order) {
    // 1. 本地事务：创建订单
    orderDao.insert(order);
    // 2. 预发送消息到 MQ（状态：PREPARE）
    messageDao.insert(new MessageRecord(order.getId(), "PREPARE"));
}

// 步骤 2：确认并投递
@Scheduled(fixedDelay = 10000)
public void confirmAndSend() {
    List<MessageRecord> messages = messageDao.findByStatus("PREPARE");
    for (MessageRecord msg : messages) {
        // 检查本地事务是否成功
        if (orderDao.exists(msg.getBizId())) {
            rabbitTemplate.convertAndSend("order.exchange", "order.created", msg);
            messageDao.updateStatus(msg.getId(), "SENT");
        } else {
            messageDao.updateStatus(msg.getId(), "CANCELLED");
        }
    }
}
```

> 🎯 字节面试题：**"不使用分布式事务框架，如何自己实现可靠消息方案？"** 核心思路：本地事务 + 消息表 + 定时任务扫描 + MQ 投递 + 消费端幂等。

### 3.4 日志收集系统设计

```
微服务 A ──┐
微服务 B ──┤──→ Logstash ─→ RabbitMQ ─→ Logstash ─→ Elasticsearch ─→ Kibana
微服务 C ──┘       ↑                            ↓
              结构化日志                    持久化 + 索引
```

**配置要点**：
- 使用 Fanout Exchange 广播日志消息
- 设置消息 TTL（日志有时间价值，过期可丢弃）
- Lazy Queue 存储海量日志（防止 OOM）
- 消费者批量拉取，批量写入 ES

### 3.5 跨服务数据同步

```
订单服务 → 发送订单消息（Topic Exchange）
                ↓
     ┌─────────────────┐
     ↓         ↓        ↓
  库存服务  支付服务  物流服务
 (扣库存)  (创建支付) (创建物流单)
```

**优势**：新增服务只需绑定新的 Queue，无需修改订单服务代码。

### 3.6 幂等性处理完整方案

| 方案 | 实现 | 适用场景 |
|------|------|---------|
| **消息 ID 去重** | 消息体携带全局唯一 ID，消费前查重 | 通用场景 |
| **业务主键防重** | 利用数据库唯一索引（INSERT IGNORE） | 插入场景 |
| **状态机验证** | 校验当前状态是否允许目标操作 | 订单状态变更 |
| **Redis 分布式锁** | SETNX 实现消费锁 | 高并发场景 |

```java
// 综合幂等方案：消息 ID + 布隆过滤器
public void consumeWithIdempotent(String messageId, Consumer<Message> consumer) {
    // 1. 布隆过滤器快速判断
    if (bloomFilter.mightContain(messageId)) {
        // 2. 可能已消费，查 Redis 确认
        if (redisTemplate.hasKey("consumed:" + messageId)) {
            log.info("消息已消费，跳过: {}", messageId);
            return;
        }
    }
    // 3. 执行业务（数据库唯一约束兜底）
    try {
        consumer.accept(message);
        // 4. 标记已消费
        redisTemplate.opsForValue().set("consumed:" + messageId, "1", 7, TimeUnit.DAYS);
        bloomFilter.put(messageId);
    } catch (DuplicateKeyException e) {
        // 5. 数据库唯一约束拦截重复
        log.warn("重复消息被数据库拦截: {}", messageId);
    }
}
```

### 3.7 消息顺序保证方案

**需求**：订单状态变更（待支付 → 已支付 → 已发货 → 已签收）必须按序执行。

**实现路径**：

```java
// 方案：订单 ID 作为 Routing Key
rabbitTemplate.convertAndSend("order.status.exchange", 
    order.getOrderId().toString(), // 同一订单 ID → 同一 Queue
    statusMessage);

// 消费者：单线程处理同一订单
@RabbitListener(queues = "#{orderStatusQueue.name}")
public void handleOrderStatus(OrderStatusMessage msg) {
    // 状态机校验
    orderService.updateStatus(msg.getOrderId(), msg.getStatus());
}
```

> ⚠️ 核心原则：**需要保证顺序的消息，必须路由到同一个 Queue，且单线程消费。**

### 3.8 海量消息积压的应对方案

| 问题 | 原因 | 解决措施 |
|------|------|---------|
| **生产者速度 > 消费者速度** | 消费者处理慢 | 增加消费者、提升消费能力 |
| **消费者宕机** | 挂掉后消息堆积 | 自动恢复、服务降级 |
| **消息处理阻塞** | 某条消息导致死循环 | 超时兜底、DLQ 隔离 |

**临时扩容方案**：

```java
// 1. 创建临时队列，消费者直接消费
// 2. 紧急扩容消费者实例
// 3. 关闭有问题的消费者，消息进入 DLQ 后重新分析
// 4. 提高 prefetch count，批量处理
@Bean
public SimpleRabbitListenerContainerFactory batchFactory(ConnectionFactory cf) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(cf);
    factory.setBatchListener(true);
    factory.setConsumerBatchEnabled(true);
    factory.setBatchSize(100); // 批量 100 条
    factory.setReceiveTimeout(3000L);
    return factory;
}
```

### 3.9 连接恢复与重试机制

```yaml
# Spring Boot 配置
spring:
  rabbitmq:
    # 连接恢复
    cache:
      connection:
        mode: CONNECTION
    # 重连配置
    template:
      retry:
        enabled: true
        max-attempts: 3
        initial-interval: 1000ms
        multiplier: 2.0
    listener:
      simple:
        retry:
          enabled: true
          max-attempts: 5
          initial-interval: 2000ms
          multiplier: 2.0
        default-requeue-rejected: false
```

```java
// 代码配置 ConnectionFactory 自动恢复
@Bean
public ConnectionFactory connectionFactory() {
    CachingConnectionFactory factory = new CachingConnectionFactory();
    factory.setHost("rabbitmq-host");
    factory.setPort(5672);
    factory.setUsername("admin");
    factory.setPassword("password");
    // 自动恢复
    factory.setChannelCacheSize(25);
    factory.getRabbitConnectionFactory().setAutomaticRecoveryEnabled(true);
    factory.getRabbitConnectionFactory().setNetworkRecoveryInterval(10000);
    factory.getRabbitConnectionFactory().setTopologyRecoveryEnabled(true);
    return factory;
}
```

### 3.10 基于 Topic 的发布订阅路由

```
Topic Exchange: order.*
    ↓              ↓              ↓
order.create   order.pay     order.ship
    ↓              ↓              ↓
 Queue A       Queue B        Queue C
 (订单服务)    (支付服务)     (物流服务)

绑定规则：
Queue A: routingKey = "order.create"
Queue B: routingKey = "order.#"  (接收所有订单消息)
Queue C: routingKey = "order.ship"
```

---

## 四、手写代码/配置文件题（6题）

### 4.1 Spring Boot RabbitMQ 生产者配置 + RabbitTemplate 发送

```java
// application.yml
spring:
  rabbitmq:
    host: 192.168.1.100
    port: 5672
    virtual-host: /my_vhost
    username: admin
    password: admin123
    publisher-confirm-type: correlated   # 开启 Confirm 回调
    publisher-returns: true              # 开启 Return 回调
    template:
      mandatory: true                    # 消息无法路由时 Return

// RabbitConfig.java
@Configuration
public class RabbitConfig {
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        
        // Confirm 回调
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.info("消息确认成功: {}", correlationData.getId());
                // 更新消息表状态为已确认
            } else {
                log.error("消息确认失败: {}, cause: {}", correlationData.getId(), cause);
                // 补偿重试或告警
            }
        });
        
        // Return 回调
        template.setReturnsCallback(returned -> {
            log.error("消息路由失败: exchange={}, routingKey={}, msg={}",
                returned.getExchange(), returned.getRoutingKey(),
                new String(returned.getMessage().getBody()));
            // 处理无法路由的消息
        });
        
        template.setMandatory(true);
        return template;
    }
    
    // 声明 Exchange、Queue、Binding
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange("order.exchange", true, false);
    }
    
    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable("order.queue")
            .deadLetterExchange("dlx.exchange")
            .deadLetterRoutingKey("dlx.routing.key")
            .ttl(60000)
            .maxLength(100000)
            .build();
    }
    
    @Bean
    public Binding orderBinding() {
        return BindingBuilder.bind(orderQueue())
            .to(orderExchange()).with("order.create");
    }
    
    // 发送消息
    @Service
    public class OrderProducer {
        @Resource
        private RabbitTemplate rabbitTemplate;
        
        public void sendOrderCreated(Order order) {
            CorrelationData correlationData = new CorrelationData(order.getOrderId());
            Message message = MessageBuilder
                .withBody(JSON.toJSONBytes(order))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setMessageId(order.getOrderId())
                .setHeader("x-retry-count", 0)
                .build();
            rabbitTemplate.convertAndSend("order.exchange", "order.create", message, correlationData);
        }
    }
}
```

### 4.2 @RabbitListener 消费者 + DLQ 配置

```java
// 消费者配置
@Configuration
public class ConsumerConfig {
    
    // 死信队列
    @Bean
    public Queue dlqQueue() {
        return QueueBuilder.durable("dlq.queue").build();
    }
    
    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange("dlx.exchange");
    }
    
    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(dlqQueue())
            .to(dlxExchange()).with("dlx.routing.key");
    }
    
    // 主队列（自动绑定 DLX）
    @Bean
    public Queue mainQueue() {
        return QueueBuilder.durable("order.queue")
            .deadLetterExchange("dlx.exchange")
            .deadLetterRoutingKey("dlx.routing.key")
            .build();
    }
    
    // 容器工厂：手动 ACK + 重试
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);  // 手动 ACK
        factory.setPrefetchCount(5);
        factory.setDefaultRequeueRejected(false);            // 失败不重新入队
        factory.setMissingQueuesFatal(false);
        factory.setErrorHandler((t, e) -> {
            log.error("消息处理异常: {}", e.getMessage());
        });
        return factory;
    }
}

// 消费者实现
@Component
public class OrderConsumer {
    
    @RabbitListener(queues = "order.queue", containerFactory = "rabbitListenerContainerFactory")
    public void handleOrder(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageProperties().getMessageId();
        
        try {
            Order order = JSON.parseObject(message.getBody(), Order.class);
            log.info("收到订单消息: {}", order.getOrderId());
            
            // 业务处理
            orderService.process(order);
            
            // 手动 ACK
            channel.basicAck(deliveryTag, false);
            log.info("消息 ACK 成功: {}", messageId);
            
        } catch (BusinessException e) {
            // 业务异常，不重试，进入 DLQ
            log.warn("业务处理失败，进入死信: {}, reason: {}", messageId, e.getMessage());
            channel.basicNack(deliveryTag, false, false);
            
        } catch (Exception e) {
            // 系统异常，根据重试次数决定
            int retryCount = message.getMessageProperties()
                .getHeader("x-retry-count") == null ? 0 : 
                (int) message.getMessageProperties().getHeader("x-retry-count");
            
            if (retryCount < 3) {
                // 重新入队重试
                message.getMessageProperties().setHeader("x-retry-count", retryCount + 1);
                channel.basicNack(deliveryTag, false, true);
                log.warn("消息重试第 {} 次: {}", retryCount + 1, messageId);
            } else {
                // 超过重试次数，进入 DLQ
                channel.basicNack(deliveryTag, false, false);
                log.error("消息超过最大重试次数，进入死信: {}", messageId);
                // 发送告警
                alertService.sendAlert("消息重试耗尽", messageId);
            }
        }
    }
}
```

### 4.3 延迟消息插件使用（rabbitmq_delayed_message_exchange）

```java
// 配置延迟交换机
@Configuration
public class DelayedConfig {
    
    @Bean
    public CustomExchange delayedExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");  // 底层 Exchange 类型
        return new CustomExchange(
            "delayed.exchange", 
            "x-delayed-message",   // 插件类型
            true, false, args
        );
    }
    
    @Bean
    public Queue delayedQueue() {
        return QueueBuilder.durable("delayed.queue").build();
    }
    
    @Bean
    public Binding delayedBinding() {
        return BindingBuilder.bind(delayedQueue())
            .to(delayedExchange()).with("delayed.routing.key").noargs();
    }
}

// 生产者：发送延迟消息
@Service
public class DelayMessageProducer {
    
    @Resource
    private RabbitTemplate rabbitTemplate;
    
    public void sendDelayedMessage(String message, long delayMillis) {
        MessageProperties props = new MessageProperties();
        props.setDelay(Math.toIntExact(delayMillis));  // 设置延迟时间（毫秒）
        Message msg = MessageBuilder.withBody(message.getBytes())
            .andProperties(props)
            .build();
        rabbitTemplate.convertAndSend("delayed.exchange", "delayed.routing.key", msg);
    }
    
    // 使用示例
    public void sendOrderTimeoutCheck(String orderId) {
        sendDelayedMessage(orderId, 30 * 60 * 1000); // 30分钟延迟
        log.info("已发送订单超时检测消息: {}", orderId);
    }
}

// 消费者：消费延迟消息
@Component
public class DelayMessageConsumer {
    
    @RabbitListener(queues = "delayed.queue")
    public void handleDelayedMessage(String message) {
        log.info("收到延迟消息: {}", message);
        // 处理业务（如取消超时订单）
        orderService.checkAndCancelOrder(message);
    }
}
```

### 4.4 Confirm + Return 回调完整配置

```java
@Configuration
@Slf4j
public class ConfirmReturnConfig {
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        
        // ====== Confirm 回调 ======
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (correlationData == null) return;
            
            String messageId = correlationData.getId();
            if (ack) {
                // 消息成功到达 Exchange
                log.info("消息 [{}] 已确认到达 Exchange", messageId);
                // 更新本地消息表状态为 SUCCESS
                messageStatusService.markSuccess(messageId);
            } else {
                // 消息未到达 Exchange（网络故障、权限问题等）
                log.error("消息 [{}] 未到达 Exchange, cause: {}", messageId, cause);
                // 查询本地消息表，决定是否重发
                MessageRecord record = messageStatusService.getByMessageId(messageId);
                if (record != null && record.getRetryCount() < 3) {
                    // 重发
                    retrySend(record);
                } else {
                    // 标记失败并告警
                    messageStatusService.markFailed(messageId);
                    alertService.sendAlert("消息投递失败", messageId);
                }
            }
        });
        
        // ====== Return 回调（消息无法路由到 Queue） ======
        template.setReturnsCallback(returned -> {
            log.warn("消息路由失败: exchange={}, routingKey={}, replyCode={}, replyText={}",
                returned.getExchange(), returned.getRoutingKey(),
                returned.getReplyCode(), returned.getReplyText());
            
            byte[] body = returned.getMessage().getBody();
            String messageBody = new String(body, StandardCharsets.UTF_8);
            
            // 记录路由失败的消息到 Redis 或数据库
            stringRedisTemplate.opsForList()
                .leftPush("routing:failure:messages", messageBody);
            
            // 告警通知
            alertService.sendAlert("消息路由失败", 
                String.format("Exchange: %s, RoutingKey: %s", 
                    returned.getExchange(), returned.getRoutingKey()));
        });
        
        template.setMandatory(true);
        return template;
    }
    
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private MessageStatusService messageStatusService;
    @Resource
    private AlertService alertService;
}
```

### 4.5 手动 ACK 消费者代码

```java
@Component
@Slf4j
public class ManualAckConsumer {
    
    // 批量消费 + 手动 ACK
    @RabbitListener(queues = "batch.queue", containerFactory = "batchContainerFactory")
    public void handleBatch(List<Message> messages, Channel channel) throws IOException {
        List<Order> successOrders = new ArrayList<>();
        List<Long> failedTags = new ArrayList<>();
        
        for (Message message : messages) {
            long deliveryTag = message.getMessageProperties().getDeliveryTag();
            try {
                Order order = JSON.parseObject(message.getBody(), Order.class);
                orderService.process(order);
                successOrders.add(order);
            } catch (Exception e) {
                log.error("处理消息失败, tag: {}", deliveryTag, e);
                failedTags.add(deliveryTag);
            }
        }
        
        // 批量 ACK 成功的消息
        if (!successOrders.isEmpty()) {
            long lastTag = messages.get(successOrders.size() - 1)
                .getMessageProperties().getDeliveryTag();
            channel.basicAck(lastTag, true);  // multiple=true 确认该 tag 之前所有
        }
        
        // 拒绝失败的消息（进入 DLQ）
        for (Long tag : failedTags) {
            channel.basicNack(tag, false, false);
        }
    }
    
    @Bean
    public SimpleRabbitListenerContainerFactory batchContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setBatchListener(true);
        factory.setConsumerBatchEnabled(true);
        factory.setBatchSize(50);
        factory.setReceiveTimeout(5000L);
        factory.setPrefetchCount(100);
        return factory;
    }
}
```

### 4.6 死信队列配置（纯 Java Config）

```java
@Configuration
public class DeadLetterConfig {
    
    // ========== 死信交换机（DLX） ==========
    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange("dlx.exchange")
            .durable(true)
            .build();
    }
    
    // ========== 死信队列（DLQ） ==========
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("dlq.queue")
            .maxLength(50000)                       // 死信队列容量
            .build();
    }
    
    // ========== 死信绑定 ==========
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
            .to(deadLetterExchange())
            .with("dlx.routing.key");
    }
    
    // ========== 主队列（带 DLX 配置） ==========
    @Bean
    public Queue mainQueue() {
        return QueueBuilder.durable("business.queue")
            // 死信交换机
            .deadLetterExchange("dlx.exchange")
            // 死信路由键
            .deadLetterRoutingKey("dlx.routing.key")
            // 消息 TTL（可选）
            .ttl(300000)
            // 队列最大长度（触发死信的第三种情况）
            .maxLength(100000)
            // 队列最大字节数
            .maxLengthBytes(100 * 1024 * 1024) // 100MB
            // 溢出策略：拒绝发布（拒绝时生产者会收到 Return 回调）
            .overflow(OverFlowBehavior.REJECT_PUBLISH)
            .build();
    }
    
    @Bean
    public DirectExchange businessExchange() {
        return ExchangeBuilder.directExchange("business.exchange")
            .durable(true)
            .build();
    }
    
    @Bean
    public Binding businessBinding() {
        return BindingBuilder.bind(mainQueue())
            .to(businessExchange())
            .with("business.routing.key");
    }
    
    // ========== 死信消费者 ==========
    @Component
    public static class DeadLetterConsumer {
        
        @RabbitListener(queues = "dlq.queue")
        public void handleDeadLetter(Message message) {
            String body = new String(message.getBody());
            String originalQueue = message.getMessageProperties()
                .getHeader("x-first-death-queue");
            String reason = message.getMessageProperties()
                .getHeader("x-first-death-reason"); // rejected/expired/maxlen
            
            log.error("收到死信消息: body={}, originalQueue={}, reason={}", 
                body, originalQueue, reason);
            
            // 根据死信原因执行不同策略
            switch (reason) {
                case "rejected":
                    // 消费者拒绝 -> 检查业务逻辑是否有 bug
                    alertService.sendAlert("消息被拒绝", body);
                    break;
                case "expired":
                    // 消息过期 -> 可能是 TTL 设置不合理
                    log.warn("消息过期，业务可能超时: {}", body);
                    break;
                case "maxlen":
                    // 队列满了 -> 需要扩容
                    alertService.sendAlert("队列满导致消息丢弃", body);
                    break;
            }
            
            // 死信消息可以存储到数据库，人工分析处理
            deadLetterRepository.save(new DeadLetterRecord(body, reason, originalQueue));
        }
    }
}
```

---

## 五、系统设计题（4题）

### 5.1 设计可靠消息投递系统（支付场景）

**需求**：支付系统需要保证"支付成功"的消息**绝对不丢失、至少投递一次、最终一致**。

**架构设计**：

```
用户支付 → 支付网关 → 支付服务（本地事务 + 消息表）
                              ↓
                    ┌──Confirm 回调──┐
                    ↓               ↓
              消息表状态:      投递到 MQ
              WAIT → SENT     (支付结果通知)
                    ↓               ↓
              定时任务重试       订单服务消费
                    ↓               ↓
              超过重试次数 → 告警  处理订单状态
```

**详细设计**：

```java
// 1. 本地事务 + 消息表（确保业务与消息原子性）
@Transactional
public PayResponse pay(PayRequest request) {
    // 扣款
    paymentDao.updateBalance(request.getUserId(), request.getAmount());
    
    // 记录支付流水
    PayRecord record = new PayRecord();
    record.setPayId(UUID.randomUUID().toString());
    record.setAmount(request.getAmount());
    record.setStatus("SUCCESS");
    paymentDao.insert(record);
    
    // 插入消息表（与业务同一个事务）
    MessageRecord msg = new MessageRecord();
    msg.setMessageId(record.getPayId());
    msg.setBusinessType("PAY_SUCCESS");
    msg.setContent(JSON.toJSONString(record));
    msg.setStatus("WAIT_CONFIRM");
    msg.setRetryCount(0);
    messageDao.insert(msg);
    
    return PayResponse.success(record.getPayId());
}

// 2. 定时任务扫描待确认消息
@Scheduled(fixedDelay = 5000)
public void scanAndSend() {
    List<MessageRecord> waitList = messageDao.findByStatus("WAIT_CONFIRM", 100);
    for (MessageRecord msg : waitList) {
        CorrelationData correlation = new CorrelationData(msg.getMessageId());
        rabbitTemplate.convertAndSend("pay.exchange", "pay.success", 
            msg.getContent(), correlation);
        // 标记为已发送
        messageDao.updateStatus(msg.getMessageId(), "SENT");
    }
}

// 3. Confirm 回调处理
// （见 4.4 节 Confirm + Return 回调完整配置）

// 4. 消费端幂等处理
@RabbitListener(queues = "pay.notify.queue")
public void handlePaySuccess(Message message) {
    String payId = message.getMessageProperties().getMessageId();
    
    // 幂等性检查
    if (orderService.isPaySuccess(payId)) {
        log.info("支付已处理，幂等跳过: {}", payId);
        return;
    }
    
    // 更新订单状态
    orderService.updateOrderToPaid(payId);
}
```

**可靠性保障总结**：
| 阶段 | 风险 | 解决方案 |
|------|------|---------|
| 数据库写入 | 写入失败 | 本地事务回滚 |
| 消息投递 | MQ 宕机或网络中断 | 定时任务扫描 + 重试 |
| 消息消费 | 消费者宕机或处理失败 | 手动 ACK + 重试 + DLQ |
| 重复消息 | 网络重试导致重复投递 | 消息幂等处理（业务 ID 去重） |

### 5.2 设计秒杀系统（MQ 削峰）

**需求**：10 万人秒杀 1000 件商品，避免数据库被打爆。

**架构设计**：

```
客户端 → CDN → Nginx（限流 1000/s）
                  ↓
           网关层（限流 + 风控）
                  ↓
          秒杀服务（预检 + MQ）
                  ↓
         ┌───────┴───────┐
         ↓               ↓
    Redis 预扣库存    RabbitMQ
         ↓               ↓
    数据库库存最终    MQ 消费者
    扣减 + 订单创建  （每 100ms 拉取一批）
```

**分层削峰方案**：

```java
// 第一层：前端限流
// - 按钮置灰 5 秒后才能再次点击
// - 随机延迟展示结果（防刷）

// 第二层：Nginx 限流
// limit_req zone=seckill burst=1000 nodelay;

// 第三层：Redis 预扣库存 + MQ 排队

@Service
public class SeckillService {
    
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    @Resource
    private RabbitTemplate rabbitTemplate;
    
    private static final String STOCK_KEY = "seckill:stock:%s";
    private static final String USER_KEY = "seckill:user:%s:%s";
    
    public Result<SeckillResponse> seckill(Long userId, Long productId) {
        // 1. 用户限购检查（一人一件）
        Boolean isBought = redisTemplate.opsForValue()
            .setIfAbsent(String.format(USER_KEY, productId, userId), "1");
        if (Boolean.FALSE.equals(isBought)) {
            return Result.fail("您已参与过秒杀");
        }
        
        // 2. Redis 预扣库存（Lua 脚本保证原子性）
        String luaScript = 
            "local stock = redis.call('get', KEYS[1]) " +
            "if stock and tonumber(stock) > 0 then " +
            "   redis.call('decrby', KEYS[1], 1) " +
            "   return 1 " +
            "else " +
            "   return 0 " +
            "end";
        
        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(luaScript, Long.class),
            Collections.singletonList(String.format(STOCK_KEY, productId))
        );
        
        if (result == null || result == 0) {
            // 还原限购标记
            redisTemplate.delete(String.format(USER_KEY, productId, userId));
            return Result.fail("秒杀已结束");
        }
        
        // 3. MQ 排队落单（异步处理）
        SeckillMessage seckillMessage = new SeckillMessage(userId, productId);
        rabbitTemplate.convertAndSend("seckill.exchange", "seckill.order", seckillMessage);
        
        return Result.success("排队成功，请稍后查看结果");
    }
}

// 第四层：MQ 消费者异步落单
@Component
public class SeckillConsumer {
    
    @RabbitListener(queues = "seckill.order.queue")
    public void createOrder(SeckillMessage msg) {
        // 1. 再次查询库存（数据库兜底）
        int stock = stockMapper.getStock(msg.getProductId());
        if (stock <= 0) {
            log.warn("数据库库存不足，秒杀失败: {}", msg.getUserId());
            return;
        }
        
        // 2. 数据库扣库存（乐观锁）
        int updated = stockMapper.deductStock(msg.getProductId(), 1);
        if (updated == 0) {
            log.warn("乐观锁规避并发，秒杀失败: {}", msg.getUserId());
            return;
        }
        
        // 3. 创建订单
        orderMapper.create(new Order(msg.getUserId(), msg.getProductId()));
        
        // 4. 发送成功通知（异步）
        notificationService.notifySuccess(msg.getUserId(), msg.getProductId());
    }
}
```

> 🎯 阿里面试题：**"秒杀系统的核心挑战是什么？"** 回答：库存防超卖、流量削锋、防刷单、用户体验。MQ 在这里的角色是缓冲 + 异步落单。

### 5.3 设计分布式事务最终一致性方案

**需求**：下单扣库存 → 积分增加 → 优惠券核销，三个服务之间需要最终一致性。

**架构设计**：

```
                  ┌─── 可靠消息服务 ───┐
订单服务 ──→ 发送消息 ─→ 消息表 ─→ MQ ─→ 库存服务
    ↑            ↓                              ↓
    └── 事务回滚 ─┘  定时任务                   积分服务
                      ↓                          ↓
                    告警                        优惠券服务
```

```java
// 可靠消息服务核心实现
@Component
public class ReliableMessageService {
    
    // 发送事务消息（prepare 状态）
    @Transactional
    public void sendTransactionMessage(TransactionMessage msg) {
        // 1. 保存半消息
        messageDao.insert(msg);
        // 2. 发送到 MQ
        CorrelationData correlation = new CorrelationData(msg.getMessageId());
        rabbitTemplate.convertAndSend("tx.exchange", "tx." + msg.getTargetService(), 
            msg.getContent(), correlation);
    }
    
    // Confirm 回调后更新状态
    public void onConfirm(String messageId, boolean success) {
        if (success) {
            messageDao.updateStatus(messageId, "SENT");
        } else {
            // 确认失败：数据库检查业务是否已提交
            TransactionMessage msg = messageDao.findById(messageId);
            if (msg != null && "PREPARE".equals(msg.getStatus())) {
                // 业务已提交但 MQ 确认失败 → 重试
                retryService.scheduleRetry(messageId);
            }
        }
    }
    
    // 定时回查（防止消息丢失）
    @Scheduled(fixedDelay = 30000)
    public void checkPreparingMessages() {
        List<TransactionMessage> preparing = messageDao.findByStatus("PREPARE", 100);
        for (TransactionMessage msg : preparing) {
            // 检查业务状态（调用回查接口）
            boolean bizSuccess = checkBizStatus(msg);
            if (bizSuccess) {
                // 业务已成功，重新发送
                rabbitTemplate.convertAndSend(msg.getExchange(), msg.getRoutingKey(), 
                    msg.getContent(), new CorrelationData(msg.getMessageId()));
            } else {
                // 业务失败，取消消息
                messageDao.updateStatus(msg.getMessageId(), "CANCELLED");
            }
        }
    }
    
    // 消费端：幂等处理
    @RabbitListener(queues = "inventory.queue")
    public void handleInventoryMessage(Message message) {
        String messageId = message.getMessageProperties().getMessageId();
        // 幂等检查
        if (processedMessageDao.exists(messageId)) {
            channel.basicAck(...);
            return;
        }
        // 执行业务
        inventoryService.deduct(...);
        // 标记已处理
        processedMessageDao.insert(messageId);
        channel.basicAck(...);
    }
    
    // 重试机制（指数退避）
    public void retryFailedMessage(String messageId) {
        TransactionMessage msg = messageDao.findById(messageId);
        if (msg.getRetryCount() > 5) {
            // 超过重试次数，人工介入
            alertService.sendAlert("分布式事务消息重试耗尽", messageId);
            messageDao.updateStatus(messageId, "FAILED");
            return;
        }
        int delay = (int) Math.pow(2, msg.getRetryCount()) * 1000;
        rabbitTemplate.convertAndSend(msg.getExchange(), msg.getRoutingKey(),
            msg.getContent(), new CorrelationData(messageId));
        messageDao.incrementRetry(messageId);
    }
}
```

### 5.4 设计多数据中心消息复制方案

**需求**：北京、上海、深圳三个数据中心，需要实现 RabbitMQ 消息的跨地域复制。

**方案对比**：

| 方案 | 优势 | 劣势 | 适用场景 |
|------|------|------|---------|
| **Federation** | 官方支持，配置简单 | 拉取模式有延迟 | 跨地域消息订阅 |
| **Shovel** | 推模式，实时性好 | 单方向限制 | 主从灾备 |
| **自定义同步** | 灵活可控 | 开发工作量大 | 定制化需求 |

**Federation 配置方案**：

```
  北京数据中心(BJ) ──→ 上海数据中心(SH) ──→ 深圳数据中心(SZ)
          │                    │                     │
     上游 Exchange        上游 Exchange          上游 Exchange
          │                    │                     │
    Federation Link       Federation Link        消费者
          ↓                    ↓
     下游 Queue           下游 Queue
```

```bash
# 1. 启用 Federation 插件
rabbitmq-plugins enable rabbitmq_federation
rabbitmq-plugins enable rabbitmq_federation_management

# 2. 上游配置（定义上游连接）
# 在 Management UI 中：Admin → Federation Upstream → Add a new upstream
# Name: shanghai-upstream
# URI: amqp://user:pass@shanghai-host:5672
# Exchange: source.exchange

# 3. 创建 Federation 策略
rabbitmqctl set_policy federation-policy "^fed\." \
  '{"federation-upstream":"shanghai-upstream"}' \
  --apply-to exchanges
```

**Shovel 配置方案（主备灾备）**：

```bash
# 启用插件
rabbitmq-plugins enable rabbitmq_shovel
rabbitmq-plugins enable rabbitmq_shovel_management

# 配置 Shovel（北京 → 上海单向同步）
rabbitmqctl set_parameter shovel bj-to-sh \
  '{"src-protocol": "amqp091", "src-uri": "amqp://user:pass@bj-host:5672",
    "src-queue": "backup.queue",
    "dest-protocol": "amqp091", "dest-uri": "amqp://user:pass@sh-host:5672",
    "dest-queue": "backup.queue",
    "reconnect-delay": 5}'
```

**高可用架构总结**：

```
               ┌──────────────────┐
               │   DNS / Load Balancer    │
               └────────┬─────────┘
                        │
          ┌─────────────┼─────────────┐
          ↓             ↓             ↓
     ┌────────┐   ┌────────┐   ┌────────┐
     │ 节点 1  │   │ 节点 2  │   │ 节点 3  │   ← RabbitMQ 仲裁队列集群
     │ (主)   │   │ (从)   │   │ (从)   │   （Raft 强一致）
     └───┬────┘   └───┬────┘   └───┬────┘
         │            │            │
         └────────────┼────────────┘
                      │
          ┌───────────┴───────────┐
          ↓                       ↓
     ┌──────────┐          ┌──────────┐
     │ Shovel/Fed│          │ 应用服务  │
     │ 其他数据中心 │          │ (消费者)  │
     └──────────┘          └──────────┘
```

---

## 六、常见坑点与最佳实践

| 坑点 | 原因 | 解决方案 | 最佳实践 |
|------|------|---------|---------|
| 消息丢失 | 未开启持久化或自动 ACK 异常丢失 | 三处持久化 + 手动 ACK | 生产环境：Exchange/Queue/Message 全部 durable=true |
| 重复消费 | 网络重试、Confirm 重发、消费者重启 | 消费端幂等处理 | 消息携带全局唯一 ID，消费前查重 |
| 消息积压 | 消费者处理慢或宕机 | 增加消费者、Lazy Queue、批量消费 | 设置队列最大长度和溢出策略 |
| 消息顺序错乱 | 多消费者并发消费同一 Queue | 单消费者或分区有序 | 按业务键路由到同一 Queue |
| 死信循环 | 消费失败重新入队，反复重试 | 设置最大重试次数，超限进入 DLQ | 配置 `x-death` 头信息追踪重试次数 |
| 内存溢出 | 消息积压导致内存爆满 | 使用 Lazy Queue | 3.12+ 默认 Lazy Queue，无需手动配置 |
| 连接泄漏 | Channel 用完不关闭 | 使用连接池管理 | Spring AMQP 自动管理 Channel 生命周期 |
| 慢消费导致堆积 | Prefetch 设置过大 | 减小 prefetch count | 根据业务处理时间调整，建议 1-50 |
| TTL 阻塞 | 头部消息未过期阻塞后续短 TTL 消息 | DLX + TTL 方式避免，或用延迟插件 | 高实时性延迟场景用插件 |
| Confirm 回调丢失 | 异步回调因服务器重启未处理 | 结合本地消息表持久化状态 | 保证 Confirm 和业务在一个事务内 |
| 跨 namespace 的 Cache 不一致 | MyBatis 二级缓存 + RabbitMQ 组合场景 | 统一用 Redis 缓存 | 不使用 MyBatis 二级缓存 |
| 集群脑裂 | 网络分区导致集群分裂 | 配置 `cluster_partition_handling` | 仲裁队列（Raft）自动处理分区 |
| Shovel 连接中断 | 网络不稳定导致 Shovel 断开 | 设置自动重连参数 | `reconnect-delay` 设置合适的重连间隔 |
| ML 元数据丢失 | 队列声明在非 durable Exchange/Queue | 使用 AutoDeclare 或代码声明 | 统一通过代码/配置声明所有元数据 |
| 消费者阻塞 | 有未 ACK 的消息达到 Channel Max | 及时 ACK 或减小 prefetch | 监控未 ACK 消息数 |
| RabbitMQ 连接失败 | Broker 不可用或防火墙拦截 | 启用自动恢复 + 重试 | 配合 Haproxy/LVS 做高可用代理 |

---

## 七、面试回答模板（Top 5）

### 7.1 如何保证 RabbitMQ 消息不丢失？

**总分结构**（生产者 + MQ + 消费者三端保障）：

1. **生产者端**：开启 Publisher Confirm 确认消息到达 Exchange，Return 回调处理无法路由的消息，配合本地消息表实现可靠投递
2. **MQ 端**：Exchange、Queue、Message 三处持久化，使用仲裁队列（Quorum Queue）保证集群高可用
3. **消费者端**：手动 ACK 模式，业务处理完成才确认，配置重试机制与死信队列兜底，消费端做幂等处理

> 扩展亮点：可以结合具体业务场景说明。如支付场景要求 100% 不丢，需要**本地事务 + 消息表 + 定时任务 + Confirm + 手动 ACK** 五重保障。

### 7.2 RabbitMQ 如何实现延迟消息？

**Step 1 - 明确两种实现方式**：
- 方式一：DLX + TTL（利用消息过期进入死信队列）
- 方式二：延迟消息插件（rabbitmq_delayed_message_exchange）

**Step 2 - 对比优劣**：

| 方案 | 优点 | 缺点 |
|------|------|------|
| DLX + TTL | 无需插件，原生支持 | TTL 精度低，头部阻塞问题 |
| 插件 | 灵活精确，每条消息独立延迟 | 需安装插件，需额外存储 |

**Step 3 - 结合实际场景**：

```java
// 订单超时取消场景
// 使用插件方式（推荐）
rabbitTemplate.convertAndSend("delayed.exchange", "delayed.routing.key",
    orderId, message -> {
        message.getMessageProperties().setDelay(30 * 60 * 1000); // 30min
        return message;
    });
```

> 🎯 面试加分：可以主动指出头部阻塞问题（head-of-line blocking），并说明为什么插件能避免此问题。

### 7.3 RabbitMQ 如何处理重复消息？

**核心思路：消费端幂等处理**。

1. **消息去重**：消息携带全局唯一 ID（如 UUID 或业务主键）
2. **幂等判断**：消费前检查是否已处理（Redis 或数据库唯一索引）
3. **兜底策略**：数据库唯一约束防止重复插入
4. **综合方案**：布隆过滤器避免 Redis 压力 + Redis 记录已消费 ID + 数据库唯一键兜底

> 扩展亮点：可以提到 Exactly-Once 语义在分布式系统中很难实现，通常做到 At-Least-Once + 幂等消费即可。

### 7.4 什么是死信队列？有什么用？

**什么是死信**：消息无法被正常消费时变成死信。三种情况——消费者拒绝且不重新入队、TTL 过期、队列满。

**死信队列的作用**：
1. **兜底处理**：失败消息集中到死信队列，后续分析处理
2. **延迟消息**：利用 TTL + DLX 实现延迟消息
3. **异常隔离**：坏消息不影响主队列的正常消费
4. **运维监控**：死信数量可作为系统健康指标

> 面试亮点：死信队列是**异常处理的最佳实践模式**，类似微服务中的熔断降级。

### 7.5 Exchange 的四种类型，以及 Topic 中 # 和 * 的区别

**四种 Exchange**：
| 类型 | 路由规则 | 代码 |
|------|---------|------|
| Direct | Routing Key 精确匹配 | 点对点 |
| Fanout | 广播到所有绑定队列 | 广播 |
| Topic | 通配符匹配 | 按主题分发 |
| Headers | Headers 属性匹配 | 复杂条件 |

**Topic Exchange 通配符**：
- `*`：匹配一个单词。如 `order.*` 匹配 `order.create`、`order.pay`
- `#`：匹配零个或多个单词。如 `order.#` 匹配 `order`、`order.create`、`order.create.timeout`

```java
// Topic 示例
channel.exchangeDeclare("topic.exchange", BuiltinExchangeType.TOPIC);
channel.queueBind("queue1", "topic.exchange", "user.*");     // user.create, user.update
channel.queueBind("queue2", "topic.exchange", "user.#");     // user, user.create, user.create.xxx
channel.queueBind("queue3", "topic.exchange", "#.complete"); // task.complete, order.task.complete
```

> ⚠️ 常见面试陷阱：`*` 只能匹配一个单词，`#` 可匹配多级。Routing Key 中单词用 `.` 分隔。

---

## 八、快速查漏补缺 Checklist

- [ ] MQ 核心价值：异步、解耦、削峰
- [ ] RabbitMQ vs Kafka vs RocketMQ 选型对比
- [ ] 同步调用 vs 异步调用的优缺点
- [ ] Exchange 四种类型（Direct/Fanout/Topic/Headers）
- [ ] Topic Exchange 中 `#` 和 `*` 通配符的区别
- [ ] Connection vs Channel（多路复用）
- [ ] Virtual Host（vhost）隔离机制
- [ ] Binding 的作用和路由绑定关系
- [ ] Queue 四种类型：Classic、Quorum、Stream、Lazy
- [ ] 消息优先级的实现和局限
- [ ] TTL 的两种设置方式（队列级/消息级）
- [ ] 死信交换机（DLX）的三种触发条件
- [ ] 死信队列的作用和配置
- [ ] 延迟消息两种实现方式对比（DLX+TTL vs 插件）
- [ ] ACK 三种模式（auto/manual/none）
- [ ] Prefetch Count 的作用和设置
- [ ] 消息持久化的三个维度
- [ ] Lazy Queue 的原理和优势
- [ ] 消息顺序保证（单队列单消费者）
- [ ] 消息幂等性方案（消息 ID + 状态表）
- [ ] Publisher Confirm 机制原理
- [ ] Return 回调机制
- [ ] Confirm + Return 协同保证可靠投递
- [ ] 消息可靠性完整链路
- [ ] AMQP 协议模型
- [ ] 消息路由完整流程
- [ ] Spring AMQP 核心组件（RabbitTemplate/@RabbitListener）
- [ ] @RabbitListener 原理（Container 管理）
- [ ] Spring Boot RabbitMQ 自动配置
- [ ] 消息转换器（Jackson2JsonMessageConverter）
- [ ] 集群四种模式（普通/镜像/仲裁/Federation/Shovel）
- [ ] 镜像队列 vs 仲裁队列区别
- [ ] Raft 协议在仲裁队列中的应用
- [ ] Flow Control 流控机制
- [ ] 手工 ACK 代码写法
- [ ] 消费者重试机制（指数退避）
- [ ] 重试耗尽进入 DLQ 的配置
- [ ] 订单超时取消场景设计
- [ ] 秒杀系统削峰设计
- [ ] 分布式事务最终一致性方案
- [ ] 可靠消息服务（本地事务 + 消息表）
- [ ] 日志收集系统设计
- [ ] 跨服务数据同步方案
- [ ] 消息积压紧急处理方案
- [ ] 连接恢复和自动重试配置
- [ ] 死信循环的避免
- [ ] TTL 头部阻塞问题
- [ ] 多数据中心复制（Federation/Shovel）
- [ ] RabbitMQ 常用命令行（rabbitmqctl）
- [ ] 插件管理（rabbitmq-plugins enable）
- [ ] Management UI 基本操作
