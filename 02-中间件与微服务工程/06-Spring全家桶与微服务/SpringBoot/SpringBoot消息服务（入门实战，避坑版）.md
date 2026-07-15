# SpringBoot 消息服务（入门实战，避坑版）

> **定位**：Spring Boot + RabbitMQ 实现异步解耦。核心：依赖 → 配置 → 配置类（交换机+队列+绑定）→ 生产者 + 消费者。

---

## 目录

1. [快速安装 RabbitMQ](#1-快速安装-rabbitmq)
2. [SpringBoot 整合](#2-springboot-整合)
3. [生产者与消费者](#3-生产者与消费者)
4. [避坑指南](#4-避坑指南)

---

## 1. 快速安装 RabbitMQ

```bash
docker pull rabbitmq:3-management
docker run -d -p 5672:5672 -p 15672:15672 --name rabbitmq \
  -e RABBITMQ_DEFAULT_USER=guest -e RABBITMQ_DEFAULT_PASS=guest rabbitmq:3-management
```

| 端口 | 用途 |
|:----:|------|
| 5672 | 消息通信（程序连接） |
| 15672 | Web 管理界面 `http://localhost:15672` |

---

## 2. SpringBoot 整合

### 依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

### 配置

```properties
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
spring.rabbitmq.publisher-confirm-type=correlated
spring.rabbitmq.publisher-returns=true
spring.rabbitmq.listener.simple.acknowledge-mode=manual
```

### 配置类

```java
@Configuration
public class RabbitMQConfig {
    @Bean
    public Queue testQueue() {
        return new Queue("test_queue", true);   // durable=true 持久化
    }

    @Bean
    public DirectExchange testExchange() {
        return new DirectExchange("test_exchange", true, false);
    }

    @Bean
    public Binding binding(Queue testQueue, DirectExchange testExchange) {
        return BindingBuilder.bind(testQueue).to(testExchange).with("test_key");
    }
}
```

---

## 3. 生产者与消费者

### 生产者

```java
@Service
public class RabbitProducerService {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void send(String exchange, String routingKey, String message) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }
}
```

### 消费者（手动 ACK）

```java
@Service
public class RabbitConsumerService {
    @RabbitListener(queues = "test_queue")
    public void receive(String message, Channel channel, Message msg) throws IOException {
        try {
            System.out.println("收到消息：" + message);
            channel.basicAck(msg.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            channel.basicNack(msg.getMessageProperties().getDeliveryTag(), false, true);
        }
    }
}
```

### 测试

```java
@GetMapping("/send")
public String send(@RequestParam String message) {
    producer.send("test_exchange", "test_key", message);
    return "发送成功";
}
```

---

## 4. 避坑指南

| 问题 | 解决 |
|------|------|
| `Connection refused` | Docker 容器是否启动 + 端口是否正确 |
| 发送成功但收不到 | 检查交换机/队列/绑定/路由键一致 + 消费者 `@Service` |
| 消息丢失 | 开启手动 ACK + `basicAck`/`basicNack` |
| 重启后队列丢失 | `durable=true` |
| `no queue 'xxx' in vhost` | `@Configuration` + 重启项目 |
