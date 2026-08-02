# 03 - Spring Boot 集成

> 定位：Boot Starter 全配置、Artemis vs Classic 双模切换、嵌入式 Broker 深度、生产 checklist、集成测试三层——从开发到生产一键部署

## 📚 目录

1. [Starter 与 Artemis 双模](#1-starter-与-artemis-双模)
2. [Classic 与 Artemis 的完整配置差异](#2-classic-与-artemis-的完整配置差异)
3. [嵌入式 Broker：开发测试与轻量生产](#3-嵌入式-broker开发测试与轻量生产)
4. [生产配置 Checklist](#4-生产配置-checklist)
5. [集成测试三重境界](#5-集成测试三重境界)

---

## 1. Starter 与 Artemis 双模

### 1.1 Classic vs Artemis Starter 选型

```xml
<!-- Classic（存量项目主流） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-activemq</artifactId>
</dependency>
<!-- 底层为 activemq-client（OpenWire 协议） -->

<!-- Artemis（新项目推荐） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-artemis</artifactId>
</dependency>
<!-- 底层为 artemis-jakarta-client（AMQP/MQTT/STOMP） -->

<!-- ⚠️ 不能同时引入两个 Starter（ConnectionFactory Bean 冲突） -->
```

### 1.2 Boot 自动装配逻辑

```
spring-boot-starter-activemq 做了什么：
  ① 检测 classpath 有 ActiveMQConnectionFactory ?
     → 自动创建 ActiveMQConnectionFactory Bean
  ② 有 PooledConnectionFactory ?
     → 包装为连接池
  ③ 自动创建 JmsTemplate Bean（无需手动 @Bean）
  ④ 有 @JmsListener 注解 → 自动启用监听容器

⚠️ 面试必答：
"Boot Starter 自动装配 = 类路径检测 +
 自动创建 ConnectionFactory + JmsTemplate +
 @EnableJms 自动开启监听器；
 开发者只需配置 application.yml。"
```

---

## 2. Classic 与 Artemis 的完整配置差异

### 2.1 Classic 配置

```yaml
spring:
  activemq:
    broker-url: failover:(tcp://mq1:61616,tcp://mq2:61616)?randomize=true&maxReconnectAttempts=10
    user: ${MQ_USER}
    password: ${MQ_PASSWORD}

    # 连接池（必须启用）
    pool:
      enabled: true
      max-connections: 50
      max-sessions-per-connection: 20
      block-if-full: true
      block-if-full-timeout: 5000

    # 安全：只信任白名单
    packages:
      trust-all: false               # ⚠️ 生产必设 false
      trusted: com.example.dto,com.example.event

    # 发送默认值
    non-blocking-redelivery: true

  jms:
    pub-sub-domain: false             # Queue 模式
    listener:
      concurrency: 5-20
      max-concurrency: 30
      auto-startup: true
    template:
      delivery-mode: PERSISTENT
      time-to-live: 60000
      qos-enabled: true
```

### 2.2 Artemis 配置

```yaml
spring:
  artemis:
    mode: native                       # native（外部 Broker）| embedded（内嵌）
    host: mq1
    port: 61616
    user: ${MQ_USER}
    password: ${MQ_PASSWORD}
    # Artemis 集群支持（多节点发现）
    # host: mq-cluster
    # port: 61616

    pool:
      enabled: true
      max-connections: 50

    # Artemis 特有：HA 策略
    ha-policy: replication            # replication（复制）| shared-store（共享存储）
```

### 2.3 Classic vs Artemis 配置对照

| 配置 | Classic | Artemis |
|------|---------|---------|
| 连接 URL | `failover:(tcp://...)` | `tcp://host:61616`（客户端 ha） |
| 连接池 | `spring.activemq.pool.enabled` | `spring.artemis.pool.enabled` |
| 内嵌 | 需手动 @Bean BrokerService | `spring.artemis.mode=embedded` |
| 多协议 | OpenWire only | AMQP/MQTT/STOMP |
| 前缀 | `spring.activemq.*` | `spring.artemis.*` |

---

## 3. 嵌入式 Broker：开发测试与轻量生产

### 3.1 Classic 嵌入式

```java
// Classic 嵌入式 Broker（显式配置）
@Configuration
public class EmbeddedClassicBrokerConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public BrokerService broker() throws Exception {
        BrokerService broker = new BrokerService();

        // ① 传输连接器
        broker.addConnector("tcp://localhost:61616");
        broker.addConnector("vm://localhost");       // 进程内（最快）

        // ② 持久化
        broker.setPersistent(false);                 // 开发：非持久化
        // ③ 可嵌入 Web Console
        broker.setUseJmx(true);
        broker.setBrokerName("embedded-broker");

        return broker;
    }
}
```

### 3.2 Artemis 嵌入式

```yaml
spring:
  artemis:
    mode: embedded                         # ⚠️ 一行开启嵌入式
    host: localhost
    port: 61616
    embedded:
      enabled: true
      persistent: false                    # 开发：非持久化
      queues: orders.queue,events.topic    # ⚠️ 预创建队列
```

### 3.3 生产能用嵌入式吗？

```
⚠️ 原则上不推荐（嵌入式 Broker = 与应用同 JVM）：
  → JVM 崩溃 = Broker 也崩溃（消息丢失）
  → 单点故障（不能独立扩展）

适用场景：
  ✅ 开发/测试（免外部依赖）
  ✅ 单元测试（集成测试用真实 Broker）
  ✅ 边缘设备/桌面应用（不能部署独立中间件）
  ⚠️ 生产：选外部独立部署 Broker（高可用 + 独立运维）
```

---

## 4. 生产配置 Checklist

```yaml
spring:
  activemq:
    # ① 高可用：failover 连接串（至少两个 Broker）
    broker-url: failover:(tcp://mq1:61616,tcp://mq2:61616)?randomize=true&maxReconnectAttempts=10

    # ② 连接池（复用连接，防资源泄漏）
    pool:
      enabled: true
      max-connections: 30
      max-sessions-per-connection: 20
      block-if-full: true           # 池满等待不丢消息
      block-if-full-timeout: 5000

    # ③ 安全：白名单 + HTTPS
    packages:
      trust-all: false
      trusted: com.example.dto

  jms:
    listener:
      concurrency: 5-20             # 消费者线程
      acknowledge-mode: CLIENT      # ⚠️ 手动确认
    template:
      delivery-mode: PERSISTENT     # 持久化
      time-to-live: 60000
```

```java
// ④ 重投策略（防止无限重试 → 死信风暴）
@Bean
public RedeliveryPolicy redeliveryPolicy() {
    RedeliveryPolicy p = new RedeliveryPolicy();
    p.setMaximumRedeliveries(5);
    p.setInitialRedeliveryDelay(5000);
    p.setBackOffMultiplier(2.0);
    p.setUseExponentialBackOff(true);
    p.setMaximumRedeliveryDelay(60000);
    return p;
}

// ⑤ 异常处理器：统一告警 + 记日志
@Bean
public JmsListenerContainerFactory<?> factoryWithErrorHandler(ConnectionFactory cf) {
    DefaultJmsListenerContainerFactory f = new DefaultJmsListenerContainerFactory();
    f.setConnectionFactory(cf);
    f.setErrorHandler(t -> {
        log.error("JMS listener error: {}", t.getCause().getMessage());
        // ⚠️ 对接告警系统（Prometheus AlertManager / 钉钉 / 邮件）
    });
    return f;
}
```

```
⚠️ 生产 Checklist 八项：
  ① failover 高可用 URL
  ② 连接池启用 + 池满阻塞
  ③ trust-all=false + trusted 白名单
  ④ 手动 CLIENT_ACK 模式
  ⑤ PERSISTENT 持久化
  ⑥ redeliveryPolicy 限重投次数
  ⑦ 异常处理器（告警通知）
  ⑧ DLQ 监控（Web Console / JMX）
```

---

## 5. 集成测试三重境界

### 5.1 第一重：Mock JMS（最快，只测业务逻辑）

```java
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {AppConfig.class})
class BusinessLogicTest {

    @MockBean
    private JmsTemplate jms;                    // ⚠️ mock JMS

    @Autowired
    private OrderService orderService;

    @Test
    void 下单后消息被发送() {
        orderService.createOrder(new OrderDTO());
        // 验证 send 被调用（不关心真实 Broker）
        verify(jms).convertAndSend(eq("orders.queue"), any(OrderDTO.class));
    }
}
```

### 5.2 第二重：嵌入式 Broker（真实 JMS，不依赖外部）

```java
@SpringBootTest     // ⚠️ 用 vm://localhost 嵌入式 Broker
class EmbeddedBrokerTest {

    @Autowired
    private JmsTemplate jms;

    @Test
    void 发送并接收消息() {
        jms.convertAndSend("test.queue", "hello");

        Message msg = jms.receive("test.queue");
        assertNotNull(msg);
        assertEquals("hello", ((TextMessage) msg).getText());
    }

    @Test
    void 监听器能消费消息() throws Exception {
        jms.convertAndSend("test.queue", "hello");

        // 等 listener 消费（轮询+ 超时）
        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> testConsumer.receivedCount() > 0);
    }
}
```

### 5.3 第三重：Testcontainers（真实 Artemis 独立 Broker）

```java
@SpringBootTest
@Testcontainers
class RealBrokerTest {

    @Container
    static GenericContainer<?> broker = new GenericContainer<>(
            "apache/activemq-artemis:2.33.0")
            .withExposedPorts(61616);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.artemis.host", broker::getHost);
        r.add("spring.artemis.port", () -> broker.getMappedPort(61616));
    }

    @Autowired
    private JmsTemplate jms;

    @Test
    void 真实Artemis环境验证() {
        jms.convertAndSend("test.queue", "hello");
        assertEquals("hello", jms.receiveAndConvert("test.queue"));
    }
    // ⚠️ 与生产环境一致（Artemis 二进制 + 完整协议栈）
}
```

> 🎯 **要点**：三层测试对应三个场景——Mock（最快，CI 单测）、嵌入式（验证 JMS 语义）、Testcontainers（与生产一致）。"三层测试金字塔"是 JMS 集成的质量安全保障。

---

> 🎯 **核心要点**：Boot 集成 = **Artemis 双模**（Classic/Artemis Starter 切换）+ **配置差异**（activemq.* vs artemis.*）+ **嵌入式 Broker**（开发/测试/边缘）+ **生产八项 Checklist**（failover/连接池/安全/手动ACK/重试上限/告警/DLQ）+ **测试三重境界**（Mock → 嵌入式 → Testcontainers）。

---

**返回总览**：[00-ActiveMQ总览与核心概念](00-ActiveMQ总览与核心概念.md) | **上一篇**：[02-SpringJMS集成](02-SpringJMS集成.md) | **下一篇**：[04-集群与运维](04-集群与运维.md)
