# RabbitMQ 客户端开发向导（Java 重点版）

> **定位**：Java + Spring AMQP 快速对接 RabbitMQ，覆盖环境准备→发送→接收→配置声明。

---

## 目录

1. [开发准备](#1-开发准备)
2. [核心功能开发](#2-核心功能开发)
3. [注意事项](#3-注意事项)
4. [问题排查](#4-问题排查)

---

## 1. 开发准备

### 连接信息

| 参数 | 默认值 | 说明 |
|------|:------:|------|
| IP | localhost | 本地/远程 |
| 端口 | 5672 | AMQP 协议 |
| 账号 | guest | 仅本地可用，远程需创建新用户 |
| 虚拟主机 | `/` | 建议按环境隔离 |

### Maven 依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

### application.yml

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
```

---

## 2. 核心功能开发

### 2.1 配置类（声明 Exchange + Queue + Binding）

```java
@Configuration
public class RabbitConfig {
    @Bean
    public DirectExchange testExchange() {
        return new DirectExchange("test_exchange", true, false);
    }

    @Bean
    public Queue testQueue() {
        return new Queue("test_queue", true, false, false);
    }

    @Bean
    public Binding binding(DirectExchange exchange, Queue queue) {
        return BindingBuilder.bind(queue).to(exchange).with("test_key");
    }
}
```

### 2.2 生产者

```java
@Component
public class RabbitProducer {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void send(String exchange, String routingKey, String message) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }
}
```

### 2.3 消费者

```java
@Component
public class RabbitConsumer {
    @RabbitListener(queues = "test_queue")
    public void receive(String message) {
        System.out.println("收到消息：" + message);
    }
}
```

---

## 3. 注意事项

| 注意点 | 说明 |
|--------|------|
| **三持久化** | Exchange + Queue + Message 全部持久化 |
| **手动确认** | 核心业务设 `ack-mode: manual` |
| **路由键匹配** | direct 类型必须 RoutingKey = BindingKey |
| **异常捕获** | 生产消费两端加 try-catch |
| **幂等** | 唯一 msgId + Redis/DB 去重 |
| **环境分离** | 开发/生产用不同账号和 vhost |

---

## 4. 问题排查

| 问题 | 排查方向 |
|------|----------|
| 连接失败 | 服务是否启动、IP/端口/账号是否正确、防火墙 |
| 收不到消息 | 路由键匹配、绑定关系、消费者注解队列名一致 |
| 重复消费 | 是否开启手动确认、确认代码是否执行 |
| 消息丢失 | 三持久化是否全开、手动确认是否配置 |
