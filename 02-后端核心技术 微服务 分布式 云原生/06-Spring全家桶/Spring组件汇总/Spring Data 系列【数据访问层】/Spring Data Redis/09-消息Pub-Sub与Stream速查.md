# 09 消息 Pub/Sub 与 Stream 速查

> Pub/Sub 与 Stream 双模型、RedisMessageListenerContainer 与 @RedisListener（2026 注解）、Stream 消费组全流程、8.4 CLAIM 增强、可靠性与死信、与 MQ 的选型边界——"Redis 消息的正确打开方式"完整手册

---

## 📚 目录

1. [Pub/Sub vs Stream：两条消息路线](#1-pubsub-vs-stream两条消息路线)
2. [Pub/Sub：广播通知（丢消息模型）](#2-pubsub广播通知丢消息模型)
3. [Stream：可靠消息队列](#3-stream可靠消息队列)
4. [@RedisListener 注解式监听（2026）](#4-redislistener-注解式监听2026)
5. [8.4 Stream 增强与死信处理](#5-84-stream-增强与死信处理)
6. [与 MQ 的选型边界（面试必考）](#6-与-mq-的选型边界面试必考)

---

## 1. Pub/Sub vs Stream：两条消息路线

| 维度 | Pub/Sub（发布订阅） | Stream（流） |
|------|--------------------|-------------|
| 模型 | 广播：发出去就没了 | 日志型：消息持久化在流里 |
| 离线订阅 | ❌（订阅前发的收不到） | ✅（从任意位置读） |
| 消费组 | ❌（人人收到全部） | ✅（组内竞争消费） |
| ACK | ❌ | ✅（XACK 确认） |
| 可靠性 | ❌（断线/宕机丢消息） | ✅（pending 列表可恢复） |
| 适用 | 通知/广播/实时推送 | **任务队列/可靠消息** |
| 一句话 | 广播站（不存） | 持久化日志（可回放） |

> 🎯 面试必答：**"Pub/Sub 和 Stream 怎么选？"**——Pub/Sub 是**广播**（发即焚，离线丢消息）→ 通知/实时推送；Stream 是**持久化日志 + 消费组 + ACK**（可靠，接近 MQ）→ 任务队列/可靠消息——**"消息能不能丢"是分水岭**：能丢用 Pub/Sub，不能丢用 Stream（或直接上 MQ，见第 6 节）。

## 2. Pub/Sub：广播通知（丢消息模型）

### 2.1 发送与接收

```java
// 发送（模板）
redisTemplate.convertAndSend("channel:order", "ORDER_PAID:1001");   // 广播到频道

// 接收一：容器化监听（经典姿势）
@Configuration
public class PubSubConfig {
    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory factory) {
        RedisMessageListenerContainer c = new RedisMessageListenerContainer();
        c.setConnectionFactory(factory);
        c.addMessageListener(orderListener(), new ChannelTopic("channel:order"));
        c.addMessageListener(patternListener(), new PatternTopic("channel:*"));  // 通配订阅
        return c;
    }

    @Bean
    MessageListener orderListener() {
        return (message, pattern) -> {
            String body = new String(message.getBody());      // 字节 → 字符串（序列化约定）
            // 业务处理（注意：监听线程是容器线程，慢处理要异步化）
        };
    }
}
```

| 要点 | 说明 |
|------|------|
| 频道 | ChannelTopic（精确）/ PatternTopic（`channel:*` 通配） |
| 消息体 | 字节流（发送端序列化约定与接收端一致，[03 篇](03-序列化器与实体映射速查.md)） |
| 线程 | 容器自带线程池消费（慢处理会拖监听 → 收到后丢业务线程池） |
| **丢失** | ① 订阅前发的收不到；② 断线期间发的收不到；③ 无持久化 |

> ⚠️ **Pub/Sub 三大丢失场景**（面试必答的"为什么不能用于可靠消息"）：发布时无订阅者 → 消息丢失；消费者断线期间 → 消息丢失；Redis 重启 → 消息丢失——**"广播通知"语义下可接受，任务队列绝对不行**。

## 3. Stream：可靠消息队列

### 3.1 生产与消费组全流程

```java
// ① 生产（XADD）
RecordId id = redisTemplate.opsForStream().add(
        StreamRecords.mapToString(Map.of("orderId", "1001", "type", "PAID"))
                .withStreamKey("stream:order"));
// id 形如 "1723000000000-0"（时间戳-序号），全局唯一可排序

// ② 消费组创建（XGROUP CREATE，需在消费前创建）
redisTemplate.opsForStream().createGroup("stream:order", "group:order");

// ③ 组内消费（XREADGROUP）
List<MapRecord<String, Object, Object>> msgs = redisTemplate.opsForStream().read(
        Consumer.from("group:order", "consumer-1"),          // 组 + 消费者名
        StreamReadOptions.empty().count(10).block(Duration.ofSeconds(5)),
        StreamOffset.create("stream:order", ReadOffset.lastConsumed()));
        // lastConsumed：从上次消费位置继续（组内竞争：一条消息只给组内一个消费者）

// ④ 处理成功 → ACK（XACK）——ack 之前消息在 pending 列表（可重投）
if (!msgs.isEmpty()) {
    // 业务处理...
    redisTemplate.opsForStream().ack("stream:order", "group:order",
            msgs.stream().map(MapRecord::getId).toList());
}
```

### 3.2 消费流程要点

| 环节 | API | 说明 |
|------|-----|------|
| 创建组 | `createGroup(stream, group)` | 先建组再消费（或 XGROUP CREATE MKSTREAM） |
| 组内竞争 | `read(Consumer, ..., lastConsumed)` | 一条消息只给组内一个消费者（负载均衡） |
| ACK | `ack(stream, group, ids)` | 确认后才出 pending |
| 未 ACK 恢复 | `read(..., ReadOffset.from(pendingIds))` | 崩溃恢复：从 pending 重新读（at-least-once） |
| 独立消费 | `read(StreamOffset.from("0"))` | 不建组全量读（日志回放） |
| 长度控制 | `trim(stream, ~, count)` | XTRIM 限制流长度（防无限增长） |

> 🎯 面试必答：**"Stream 怎么做到可靠消费？"**——at-least-once 语义：消息写入流（持久化）→ 消费组竞争读取 → **处理成功 XACK**（移出 pending）→ 消费者崩溃未 ACK 的消息留在 pending → 恢复后从 pending 重读——**"先处理再 ACK"保证至少一次投递**；代价是**消费端必须幂等**（重复投递，[幂等方案](../../../../09-Web开发全流程/后端 分布式常用名词通俗解释/04-重复请求治理：幂等与防抖.md)）。

## 4. @RedisListener 注解式监听（2026）

> **2026.0 新特性**：注解声明式监听（替代手写容器配置）——Spring Messaging 风格，与 @KafkaListener/@RabbitListener 同族。

```java
@Configuration
@EnableRedisListeners                                    // 开启注解监听
public class RedisListenerConfig { }

@Component
public class OrderMessageListener {

    @RedisListener(topic = "channel:order")              // Pub/Sub 频道
    public void onOrderPaid(OrderPaidEvent event) {
        // 参数自动反序列化（MessageConverter 支持，[03 篇](03-序列化器与实体映射速查.md) 序列化约定）
    }

    @RedisListener(stream = "stream:order", group = "group:order")   // Stream 消费组
    public void onOrderStream(StreamMessage message) {
        // Stream 消费（组内竞争 + ACK 语义）
    }
}
```

| 对比 | 容器配置（经典） | @RedisListener（2026） |
|------|----------------|----------------------|
| 声明方式 | Bean 配置 addMessageListener | 注解 + 方法 |
| 参数绑定 | 裸 Message（字节） | 自动反序列化（类型安全） |
| Stream 支持 | StreamMessageListenerContainer 手配 | 注解 group 声明 |
| 复杂度 | 高（样板代码多） | **低（推荐新代码）** |

> 💡 @RedisListener 依赖 Spring Messaging 基础设施（与 Kafka/Rabbit 监听注解同族）——**新项目用注解式，存量容器配置可渐进迁移**；序列化约定（[03 篇](03-序列化器与实体映射速查.md)）在注解式下由 MessageConverter 接管（类型安全红利）。

## 5. 8.4 Stream 增强与死信处理

### 5.1 8.4：XREADGROUP CLAIM min-idle-time

```java
// 8.4 增强：认领"空闲 pending" + 新消息一步完成
// 场景：消费者 1 崩溃 → 其 pending 消息空闲超时 → 消费者 2 认领（恢复处理）
// 旧做法：XCLAIM 单独认领 → XREADGROUP 读新消息（两步）
// 8.4 新做法：XREADGROUP ... CLAIM min-idle-time 30s（一步：认领空闲 pending + 新消息）

// 应用：消费者崩溃恢复/重平衡的标准姿势
List<MapRecord<String, Object, Object>> recovered = redisTemplate.opsForStream().read(
        Consumer.from("group:order", "consumer-2"),
        StreamReadOptions.empty()
                .claimIdleTime(Duration.ofSeconds(30))     // 8.4：认领空闲超 30s 的 pending
                .count(100),
        StreamOffset.create("stream:order", ReadOffset.lastConsumed()));
```

| 8.4 增强 | 解决 |
|---------|------|
| CLAIM min-idle-time | 崩溃消费者恢复 **一步完成**（旧版 XCLAIM + XREADGROUP 两步） |
| 意义 | 消费恢复/重平衡的运维复杂度下降 |

### 5.2 死信处理（DLQ 思路）

```java
// Redis Stream 无内建死信队列——用"重试次数"字段 + 人工/定时处理实现 DLQ 思路：
// ① 消息带 retryCount 字段（生产时初始化 0）
// ② 消费失败 → 不 ACK（留在 pending）→ 重试（XCLAIM 重新投递）
// ③ 重试 N 次仍失败 → 写入死信流（stream:order:dlq）→ 人工/补偿任务处理
```

| 死信要素 | Redis Stream 做法 | 对照 MQ |
|---------|------------------|---------|
| 重试 | pending + XCLAIM（次数自行维护） | 内建重试 |
| 死信 | 手动写入 dlq 流 | 内建死信队列 |
| 延迟 | 无内建延迟——用 ZSet 延迟任务模拟（[04 篇](04-操作API与Redis-8-4新命令速查.md) 3.3） | 内建延迟队列 |

> ⚠️ **Stream 的边界**：可靠性接近 MQ，但**没有**延迟队列、死信队列、事务消息等 MQ 高级特性——"消息功能需求一多"就该上真 MQ（第 6 节）。

## 6. 与 MQ 的选型边界（面试必考）

| 场景 | 选 Stream | 选 MQ（Kafka/RocketMQ/Pulsar） |
|------|:---:|:---:|
| 轻量任务队列（已用 Redis 的团队） | ✅ | — |
| 高吞吐日志/埋点流 | ❌ | ✅ Kafka |
| 事务消息/延迟消息 | ❌ | ✅ RocketMQ |
| 复杂路由/多消费者拓扑 | ⚠️ 有限 | ✅ |
| 积压百万级 | ⚠️（内存/磁盘约束） | ✅（磁盘日志 + 分区扩展） |
| 消息可靠性要求极高 | ⚠️（at-least-once + 手动死信） | ✅ |

> 🎯 面试必答：**"Redis Stream 能替代 MQ 吗？"**——**不能（完整答案三层）**：① 简单场景可以（轻量任务队列、已用 Redis 不想引中间件）；② 边界明显：无延迟/死信/事务消息、积压能力有限、多消费者拓扑弱；③ **结论**：消息功能需求简单用 Stream（省一套中间件），"队列高级特性/高吞吐/强可靠"直接上真 MQ（[消息队列选型](../../../../03-消息队列/消息队列理论与实战/08-选型与生产实践.md)）——**加分句**："Stream 的可靠消费（pending+ACK）思路与 MQ 一致，只是能力下限，不是替代品"。

---

**下一模块**：[10-集成地图与常见问题](10-集成地图与常见问题.md)　**返回总览**：[00-组件总览](00-Spring Data Redis组件总览.md)

**【参考来源】**：[Spring Data Redis 官方参考文档（消息）](https://docs.spring.io/spring-data/redis/reference/redis/pubsub.html)、[Redis 官方 Stream 文档](https://redis.io/docs/latest/develop/data-types/streams/)、[Redis 8.4 新特性（官方博客）](https://redis.io/blog/whats-new-in-two-november-2025-edition/)
