# 09-StreamBridge 与事件路由
> 程序化消息出口：StreamBridge 发送与动态目的地、事件路由 MessageRoutingCallback、REST 集成、批量发送与响应式限制

## 📚 目录
1. [为什么需要 StreamBridge](#1-为什么需要-streambridge)
2. [基础用法与绑定解析](#2-基础用法与绑定解析)
3. [显式声明源：spring.cloud.stream.source](#3-显式声明源springcloudstreamsource)
4. [动态目的地](#4-动态目的地)
5. [事件路由与 MessageRoutingCallback](#5-事件路由与-messageroutingcallback)
6. [REST 入口集成实战](#6-rest-入口集成实战)
7. [批量发送与性能注意](#7-批量发送与性能注意)
8. [已知限制与规避](#8-已知限制与规避)
9. [核心要点](#9-核心要点)
10. [参考来源](#10-参考来源)

## 1. 为什么需要 StreamBridge

函数式模型里 `Supplier` 是流内数据源，但很多消息来自**流之外**：

| 场景 | 例子 |
|------|------|
| REST API | 用户下单 → 发订单事件 |
| 定时任务 | 每日对账结果 → 发对账事件 |
| WebSocket/外部回调 | 支付回调 → 发支付事件 |
| 数据库变更 | 手动补数据 → 重发消息 |

`StreamBridge` 是程序化出口：任何代码（Controller、Service、Listener）都能往输出目的地发送消息，**不需要 Supplier 轮询**。

```java
@Service
public class OrderService {
    private final StreamBridge streamBridge;

    public OrderService(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    public OrderEvent create(OrderDTO dto) {
        OrderEvent event = new OrderEvent(dto.orderId(), "CREATED");
        streamBridge.send("order-events", event);   // 直接按目的地名发送
        return event;
    }
}
```

## 2. 基础用法与绑定解析

### 2.1 send 签名与绑定解析

| 重载 | 说明 |
|------|------|
| `send(String bindingName, Object payload)` | 最常用，按绑定名/目的地发送 |
| `send(String bindingName, Message<?> message)` | 带完整头信息（分区键、content-type 等） |
| `send(String bindingName, Object payload, MimeType contentType)` | 显式内容类型 |

发送名解析顺序：

```text
1. 匹配显式绑定名（如 restOrder-out-0）
2. 未匹配 → 按目的地名查找（bindings 配置中的 destination）
3. 仍无 → 作为动态目的地处理（第 4 节）
```

### 2.2 分区键传递

```java
streamBridge.send("order-out-0",
        MessageBuilder.withPayload(event)
                .setHeader("partitionKey", event.orderId())   // 或按配置的表达式
                .build());
```

> 💡 用 `Message` 重载可传递 `partitionKey`/`contentType` 等头，动态目的地场景尤其重要（无绑定配置可依赖时，头是唯一的路由信息源）。

## 3. 显式声明源：spring.cloud.stream.source

没有 Supplier 时，目标绑定默认不会创建。声明：

```yaml
spring:
  cloud:
    stream:
      source: restOrder;auditSink        # 分号分隔多个源
      bindings:
        restOrder-out-0:
          destination: order-events
          binder: kafka
```

| 声明方式 | 绑定名 | 说明 |
|----------|--------|------|
| `source: restOrder` | `restOrder-out-0` | 显式源，绑定名 = `<name>-out-0` |
| 不声明 + 动态目的地 | 无 | 见第 4 节，惰性创建 |

> ⚠️ 显式源声明后绑定受 `bindings` 配置约束（destination/分区/类型转换均生效）；不声明则退化为动态目的地，只有 send 那一刻才建资源。

## 4. 动态目的地

发送一个**未在 bindings 中配置的名字**时，Stream 把它当作动态目的地：

```java
streamBridge.send("orders-" + tenantId, payload);   // 每个租户一个 Topic
```

| 特性 | 说明 |
|------|------|
| 资源创建 | 首次 send 时由 binder 自动创建（Kafka 自动建 Topic；Rabbit 自动声明 Exchange） |
| 分区键 | 只能通过消息头传递（无绑定配置） |
| 类型转换 | 按消息头 content-type 或默认 JSON |
| 风险 | 名字失控 → 资源爆炸；生产建议对动态名做白名单/规范化 |

> ⚠️ **资源爆炸是动态目的地最大风险**：高基数键（用户 ID、请求 ID）直接做目的地名会建出海量 Topic/Exchange。只允许低基数维度（租户、业务线）动态化，其余静态绑定。

## 5. 事件路由与 MessageRoutingCallback

事件路由：消费端按消息内容把事件分发给不同处理函数（5.0 官方文档专题）。

### 5.1 多函数 + 路由选择

```java
@Bean
public Consumer<OrderEvent> orderHandler() { ... }

@Bean
public Consumer<PaymentEvent> paymentHandler() { ... }
```

```yaml
spring:
  cloud:
    function:
      definition: orderHandler;paymentHandler
```

### 5.2 MessageRoutingCallback（5.0 起文档正式支持）

自定义路由回调决定每条消息进哪个函数：

```java
@Bean
public MessageRoutingCallback routingCallback() {
    return new MessageRoutingCallback() {
        @Override
        public FunctionRoutingResult routingResult(Message<?> message) {
            String type = (String) message.getHeaders().get("eventType");
            return new FunctionRoutingResult(type.equals("PAYMENT") ? "paymentHandler" : "orderHandler");
        }
    };
}
```

| 维度 | 说明 |
|------|------|
| 判定依据 | 消息头 / payload 内容（SpEL 或代码判断） |
| 返回 | `FunctionRoutingResult(目标函数名)` |
| 兜底 | 未命中时回退到默认消费函数 |
| 版本 | 5.0.0 起文档完善（issue #3198 补充官方示例） |

> 💡 **路由 vs 组合**：`definition: a;b` + MessageRoutingCallback = **运行时按消息分流**；`definition: a\|b` = **编译期管道串联**。分流要回调，串联用管道。

## 6. REST 入口集成实战

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final StreamBridge streamBridge;

    public OrderController(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @PostMapping
    public ResponseEntity<Void> create(@RequestBody @Valid OrderDTO dto) {
        streamBridge.send("restOrder-out-0", new OrderEvent(dto.orderId(), "CREATED"));
        return ResponseEntity.accepted().build();   // 202：入队成功而非处理完成
    }
}
```

```yaml
spring:
  cloud:
    stream:
      source: restOrder
      bindings:
        restOrder-out-0:
          destination: order-events
          producer:
            partition-key-expression: payload.orderId
            required-groups: order-service
```

| 设计点 | 建议 |
|--------|------|
| 返回码 | 202 Accepted（异步语义），消息入库成功 ≠ 业务成功 |
| 失败处理 | send 抛异常时返回 5xx；生产环境建议 Outbox 模式（DB 落库 → 事务后发消息） |
| 幂等 | 请求方传幂等键（`Idempotency-Key` 头），消费端去重 |

## 7. 批量发送与性能注意

```java
// 批量发送：循环 send 会有 N 次网络往返，Kafka 下可用批量缓冲配置优化
List<OrderEvent> events = ...;
events.forEach(e -> streamBridge.send("order-out-0", e));
```

| 手段 | 说明 |
|------|------|
| binder 批量配置 | Kafka producer `batch-size`/`linger.ms`（`kafka.binder.configuration.*`）、Rabbit `batching-enabled` |
| 批量消息 API | 构造批量 `Message`（`SpringMessage` 头 `amqp_batchedHeaders`） |
| 异步化 | 高频发送场景用响应式桥接或线程池封装（注意顺序性诉求） |

> ⚠️ **性能易错点**：`send` 是同步调用，循环发送会阻塞调用线程等待缓冲/确认；高吞吐入口（HTTP/定时任务）建议批量构造 + binder 缓冲参数，而不是放大线程数。

## 8. 已知限制与规避

| 限制 | 说明 | 规避 |
|------|------|------|
| StreamBridge 非响应式 | `send` 同步阻塞（官方 issue #2557 确认） | 低频直接调；高频用 `Supplier<Flux>` 桥接或异步封装 |
| 动态目的地无配置 | 分区键/转换仅靠消息头 | 统一消息头规范 + 低基数动态名 |
| 与事务整合 | send 不参与 Kafka 事务（除非配置了事务模板） | Outbox 模式或 ChainedKafkaTransactionManager |
| 多次 send 同名 | 重复调用同名绑定安全，但资源只建一次 | 无 |

## 9. 核心要点

> 🎯 **核心要点**：
> - StreamBridge = 流外代码（REST/定时/回调）→ 消息目的地的程序化出口；
> - 解析顺序：绑定名 → 目的地 → 动态目的地（最后者资源自动建，名字要可控）；
> - 无 Supplier 时用 `spring.cloud.stream.source` 显式声明源，绑定才可配置化；
> - 按内容分流用 MessageRoutingCallback（运行时路由），管道串联用 `\|` 组合（编译期）；
> - 发送是同步的，高吞吐要批量配置；动态目的地防资源爆炸。

## 10. 参考来源

- [Producing and Consuming Messages（StreamBridge 与事件路由）](https://docs.spring.io/spring-cloud-stream/reference/spring-cloud-stream/producing-and-consuming-messages.html)
- [spring-cloud-stream Releases（5.0.0 事件路由文档，issue #3198）](https://github.com/spring-cloud/spring-cloud-stream/releases)
- [Feature Requests: Reactive StreamBridge (#2557)](https://github.com/spring-cloud/spring-cloud-stream/issues/2557)
- [Spring Cloud Stream 4.0 Migration guide（StreamBridge 迁移说明）](https://github.com/spring-cloud/spring-cloud-stream/wiki/4.0-Migration-guide)

---

**下一模块**：[10-生产实践与选型避坑](10-生产实践与选型避坑.md)　/　**返回总览**：[00-总览](00-Spring%20Cloud%20Stream总览.md)
