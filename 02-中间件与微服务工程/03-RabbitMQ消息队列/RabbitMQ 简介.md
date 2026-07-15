# RabbitMQ 简介

> **定位**：基于 AMQP 0-9-1 协议的开源消息中间件，Erlang 开发，轻量、稳定、生态完善。

---

## 六大核心组件

```text
Producer → Exchange → Queue → Consumer
              ↑
           Binding
              │
          Virtual Host
```

| 组件 | 说明 |
|------|------|
| **Broker** | RabbitMQ 服务节点 |
| **Producer** | 生产者，发送消息 |
| **Consumer** | 消费者，接收处理消息 |
| **Exchange** | 交换机，路由转发消息 |
| **Queue** | 队列，存储消息 |
| **Virtual Host** | 虚拟主机，多租户隔离 |

---

## 核心优势

| 优势 | 说明 |
|------|------|
| **高可靠** | 持久化 + 确认机制 + 死信队列，全链路不丢 |
| **灵活路由** | Direct/Topic/Fanout/Headers 四种交换机 |
| **多语言** | Java/Go/Python/PHP 等全支持 |
| **轻量稳定** | Erlang 天生高并发 |

---

## 典型场景

- 服务解耦：订单→库存→物流→积分 异步联动
- 异步通信：注册后发短信/邮件
- 流量削峰：秒杀高并发缓冲
- 分布式任务分发

> 相比 Kafka/RocketMQ，RabbitMQ 更侧重**消息可靠性和路由灵活性**，适合对消息安全要求高的核心业务。
