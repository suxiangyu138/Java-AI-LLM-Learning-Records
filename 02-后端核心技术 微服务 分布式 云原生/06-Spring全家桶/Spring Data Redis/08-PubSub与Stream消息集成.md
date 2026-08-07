# 08 Pub/Sub 与 Stream 消息集成

> Redis 的两套消息通道：Pub/Sub（广播、即发即弃）与 Stream（持久、消费组、可回放）。Spring Data Redis 提供监听容器与 2026 年新增的 @RedisListener 注解式监听，本模块讲透两者的机制、容器模型与可靠性边界

---

## 📚 目录

1. [Pub/Sub vs Stream：两套消息模型对比](#1-pubsub-vs-stream两套消息模型对比)
2. [Pub/Sub 基础：RedisMessageListenerContainer](#2-pubsub-基础redismessagelistenercontainer)
3. [2026 新特性：@RedisListener 注解式监听](#3-2026-新特性redislistener-注解式监听)
4. [MessageConverter 与消息转换](#4-messageconverter-与消息转换)
5. [Stream 消费组集成](#5-stream-消费组集成)
6. [可靠性边界与死信治理](#6-可靠性边界与死信治理)
7. [场景选型与生产清单](#7-场景选型与生产清单)

---

## 1. Pub/Sub vs Stream：两套消息模型对比

| 维度 | Pub/Sub（发布订阅） | Stream（日志型队列） |
|------|:---:|:---:|
| 消息持久化 | ❌ 不落盘，发送即消费，无订阅者则丢失 | ✅ 落盘（RDB/AOF），可回放 |
| 消费方式 | 广播：所有订阅者都收到 | 消费组内瓜分：一条消息只被组内一个成员消费 |
| 离线消费 | ❌ 订阅期间外的消息收不到 | ✅ 新消费者可从任意位置读（$ 最新 / 0 最旧 / ID） |
| 确认机制 | 无 | XACK 确认；未确认进入 pending 可重投 |
| 消费者位移 | 无 | 组内游标（last-delivered-id）自动维护 |
| 消息规模 | 轻量广播 | 大流量队列，可修剪（XTRIM） |
| 适用场景 | 即时广播：通知、刷新信号、WebSocket 推送联动 | 可靠任务队列：订单处理、异步通知、事件驱动 |

> 🎯 **一句话选型**：要"广播 + 不在乎丢失" → Pub/Sub；要"可靠 + 组消费 + 重放" → Stream。生产消息中间件的硬需求（持久化、确认、重试）只有 Stream 能满足。

## 2. Pub/Sub 基础：RedisMessageListenerContainer

### 2.1 容器模型

`RedisMessageListenerContainer` 是连接 Redis 订阅消息与 Spring 回调的"常驻容器"：**订阅是持久的连接占用**（专用线程），容器负责：

```text
Redis 推送消息 → 容器（专用连接） → MessageListener 回调
                                     ├── TopicListener（@Component）
                                     ├── MessageListenerAdapter（委托方法）
                                     └── 事务/异常处理钩子
```

```java
@Configuration
public class RedisPubSubConfig {

    @Bean
    public RedisMessageListenerContainer listenerContainer(
            RedisConnectionFactory factory, MessageListenerAdapter adapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        // 订阅主题 + 绑定监听器
        container.addMessageListener(adapter, new ChannelTopic("topic:notify"));
        container.addMessageListener(adapter, new PatternTopic("topic:user:*"));  // 通配订阅
        return container;
    }

    @Bean
    public MessageListenerAdapter adapter(NotifyHandler handler) {
        return new MessageListenerAdapter(handler, "onMessage");  // 委托 handler.onMessage(msg, channel)
    }
}
```

```java
@Component
public class NotifyHandler {
    public void onMessage(Message message, byte[] channel) {
        String text = new String(message.getBody(), StandardCharsets.UTF_8);
        String topic = new String(channel, StandardCharsets.UTF_8);
        // 处理推送
    }
}
```

| 容器组件 | 职责 |
|---------|------|
| `RedisMessageListenerContainer` | 连接管理、订阅注册、回调分发、异常处理 |
| `Topic`（`ChannelTopic`/`PatternTopic`） | 精确频道 / 通配模式（`*`、`?`） |
| `MessageListenerAdapter` | 把消息委托给 POJO 方法（无需实现接口） |
| `Message` | 原始字节：body + channel（序列化由你处理） |

> ⚠️ **注意**：Pub/Sub 订阅是**独占连接**——容器与 RedisTemplate 共用 factory 时，订阅走专用连接（Lettuce 共享连接下自动切换），无需你手动管理；但**一个容器实例的并发默认较小**，高吞吐订阅者注意调 `setSubscriptionExecutor`。

### 2.2 发送端

```java
@Resource
private RedisTemplate<String, Object> redisTemplate;

// 简单发送（value 走模板序列化）
redisTemplate.convertAndSend("topic:notify", order);   // PUBLISH

// 原始字符串发送
stringRedisTemplate.convertAndSend("topic:notify", "refresh:user:1001");
```

## 3. 2026 新特性：@RedisListener 注解式监听

Spring Data 2026.0.0（4.1.x）引入**注解驱动**的 Redis Pub/Sub 监听，基于 Spring Messaging 体系，与 `@KafkaListener`/`@RabbitListener` 风格统一：

```java
@Configuration
@EnableRedisListeners                 // 1. 激活监听端点扫描
public class RedisListenerConfig { }

@Component
public class NotifyListeners {

    // 2. 声明式监听：主题 + 消息类型 + MIME 选择转换器
    @RedisListener(topic = "topic:notify", consumes = "application/json")
    public void onNotify(Order order) {
        // 直接拿到反序列化后的对象（按 MIME 自动选 JSON 转换器）
        handle(order);
    }

    // 通配主题 + 通道信息
    @RedisListener(topic = "topic:user:*", consumes = "application/json")
    public void onUserEvent(String payload, @Header(Channel) String channel) { ... }
}
```

**关键能力：**

| 能力 | 说明 |
|------|------|
| `@EnableRedisListeners` | 激活注解扫描（必须） |
| `@RedisListener(topic = "...")` | 声明式主题订阅 |
| `consumes = "application/json"` | MIME 类型选择消息转换器 |
| 自动 JSON 转换器 | classpath 有 Jackson / Kotlin Serialization / Gson 时自动注册对应转换器 |
| `@Header` 参数 | 注入通道等消息头 |
| `RedisMessageSendingTemplate` | 与监听器配对的发送模板（同一 MessageConverter 体系） |

> 💡 **面试点**：@RedisListener 是 2026.0.0 的核心新特性——把"注册监听器 + 手动转换"进化成"注解 + MIME 协商"，也是 Spring 消息抽象（Messaging）在 Redis 上的落地。功能上等价于经典 `MessageListenerContainer` 的声明式封装。

## 4. MessageConverter 与消息转换

```java
@Bean
public MessageListenerAdapter adapter(NotifyHandler handler) {
    MessageListenerAdapter adapter = new MessageListenerAdapter(handler, "onMessage");

    // 注册转换器：按 MIME 选择（application/json → Jackson3 转换器）
    JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
    adapter.setMessageConverter(converter);
    return adapter;
}
```

| 转换器 | 场景 |
|--------|------|
| `StringRedisSerializer`（直接字节） | 纯字符串消息 |
| `JacksonJsonMessageConverter` | JSON 消息（4.x 默认 Jackson 3） |
| `GenericJackson2JsonRedisSerializer` 包装 | 多态消息 |
| 自定义 MessageConverter | 专用协议 |

> ⚠️ **Pub/Sub 与 Stream 的序列化注意**：容器回调收到的是字节，`MessageListenerAdapter` 帮你转换；`@RedisListener` 按 `consumes` 自动转换。**发送端与接收端的转换器必须对称**，否则"发送 JSON、接收按 String 解析"会拿到带引号的 JSON 串。

## 5. Stream 消费组集成

`StreamMessageListenerContainer` 提供**持续轮询 + 自动 ACK + 并发控制**的消费组容器：

```java
@Configuration
public class StreamConfig {

    @Bean
    public StreamMessageListenerContainer<String, ObjectRecord<String, Order>> streamContainer(
            RedisConnectionFactory factory) {
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, ObjectRecord<String, Order>> options =
                StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofSeconds(2))          // 轮询超时
                        .targetType(Order.class)                    // 消息目标类型
                        .batchSize(10)                              // 每批拉取条数
                        .build();

        StreamMessageListenerContainer<String, ObjectRecord<String, Order>> container =
                StreamMessageListenerContainer.create(factory, options);
        return container;
    }
}
```

```java
@Component
public class OrderConsumer {

    @Resource
    private StreamMessageListenerContainer<String, ObjectRecord<String, Order>> container;

    @PostConstruct
    public void register() {
        // 注册消费：stream + 消费组 + 消费者
        container.receive(
                Consumer.from("group:order", "consumer-1"),
                StreamOffset.create("stream:orders", ReadOffset.lastConsumed()),
                message -> {
                    Order order = message.getValue();        // 已按 targetType 反序列化
                    try {
                        process(order);                     // 业务处理
                        message.getStreamOperations().acknowledge(  // 手动确认（默认开启 ack）
                                "group:order", message.getId());
                    } catch (Exception e) {
                        // 业务失败：不 ACK → 消息留在 pending 可重投/进死信
                        log.error("order process failed: {}", e.getMessage());
                    }
                });
        container.start();
    }
}
```

**生产端：**

```java
@Resource
private RedisTemplate<String, Object> redisTemplate;

public void publishOrder(Order order) {
    redisTemplate.opsForStream()
            .add(ObjectRecord.create("stream:orders", order));   // XADD
}
```

| Stream 容器能力 | 配置/方法 | 作用 |
|----------------|----------|------|
| 自动轮询 | `pollTimeout` | 无消息时挂起时间（长超时省 CPU） |
| 批量消费 | `batchSize` | 每批条数（大吞吐调大） |
| 消息确认 | `acknowledge` | 手动确认，业务成功才 ACK |
| 失败重试 | 不 ACK + pending | 消息留在 pending 列表，可重投 |
| 并发控制 | `receive` 多次注册 | 组内多消费者并行消费 |
| 容器启动 | `start()` / `@PostConstruct` | 随应用启动 |

## 6. 可靠性边界与死信治理

### 6.1 Pub/Sub 的不可靠点（务必知晓）

- 消息**不持久**：发送瞬间无订阅者 → 消息永久丢失；
- **无确认**：消费者崩溃，消息已发无法重投；
- 单线程处理：慢消费者会阻塞后续推送。

**结论：Pub/Sub 只能用于"丢了也无所谓"的广播**（缓存刷新信号、配置变更通知）。任何需要可靠投递的业务，直接上 Stream。

### 6.2 Stream 的死信治理

```text
消费失败（不 ACK）→ 消息进 pending 列表
    ├── 正常：重试后 ACK（业务兜底重试，指数退避）
    ├── 超限：移动到死信 stream（stream:orders:dead）
    └── 长期滞留：监控 XPENDING 长度与 oldest 年龄，告警人工介入
```

**监控命令（运维侧）：**

```bash
XINFO STREAM stream:orders                     # 长度、消费组信息
XPENDING stream:orders group:order             # pending 条数 + 最老消息年龄
XINFO GROUPS stream:orders                     # 各消费组积压
```

> 💡 **设计建议**：消费逻辑幂等（唯一业务 ID + 去重），失败重试 3 次仍失败写入死信 stream + 告警——这是 Stream 队列的"生产级三件套"。

## 7. 场景选型与生产清单

**选型速查：**

| 业务需求 | 选型 |
|---------|------|
| 广播刷新信号（配置/缓存失效通知） | Pub/Sub（@RedisListener 或容器） |
| 简单任务队列（能容忍丢失） | List 阻塞队列或 Stream |
| 可靠任务队列（订单/通知/事件） | **Stream 消费组** |
| 多实例广播 + 每实例都处理 | Pub/Sub 广播 |
| 一条消息只处理一次 + 可重放 | Stream 消费组 |

**生产清单：**

- [ ] 订阅是独占连接：容器线程与业务线程池隔离，注意最大并发；
- [ ] Pub/Sub 消费者**幂等**（网络重发/重复投递可能）；
- [ ] Stream 消费必须 ACK（业务成功后），失败不 ACK 留 pending；
- [ ] 监听器异常必须有兜底（try-catch + 日志），否则容器行为不可控；
- [ ] 死信 stream + 积压监控告警已配置；
- [ ] 发送/接收序列化对称（MIME 与转换器一致）；
- [ ] 4.1 新特性 @RedisListener 需要 `@EnableRedisListeners` 激活。

> 🎯 **核心要点**：Pub/Sub 是"广播 + 即发即弃"的轻通道，Stream 是"持久 + 消费组 + 确认"的可靠队列——**可靠性需求是分水岭**。2026 年的 @RedisListener 把监听做成声明式注解（MIME 协商 + 自动转换器），面试新特性题必答；生产可靠性题答"Stream + 手动 ACK + 死信 + 幂等消费"即满分。

---

**上一模块**：[07-Repository模式与查询](07-Repository模式与查询.md)　**下一模块**：[09-集群、哨兵与可观测](09-集群、哨兵与可观测.md)
