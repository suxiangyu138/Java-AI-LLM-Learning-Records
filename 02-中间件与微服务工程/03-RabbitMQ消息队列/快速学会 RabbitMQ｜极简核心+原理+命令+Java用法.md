# 快速学会 RabbitMQ｜极简核心+原理+命令+Java 用法

> **定位**：RabbitMQ 是基于 AMQP 协议的开源消息队列，Erlang 开发。核心作用：异步解耦、削峰限流、可靠投递。

---

## 目录

1. [五大核心概念](#1-五大核心概念)
2. [四种交换机类型](#2-四种交换机类型)
3. [Docker 部署](#3-docker-部署)
4. [消息可靠机制](#4-消息可靠机制)
5. [Java Spring Boot 使用](#5-java-spring-boot-使用)
6. [常见问题](#6-常见问题)

---

## 1. 五大核心概念

| 概念 | 说明 |
|------|------|
| **Producer** | 生产者，发送消息到交换机 |
| **Consumer** | 消费者，监听队列处理消息 |
| **Exchange** | 交换机，路由转发，不存消息 |
| **Queue** | 队列，持久化存储消息 |
| **Binding** | 绑定，交换机→队列的路由规则 |

```text
生产者 → Exchange → Binding → Queue → 消费者
```

---

## 2. 四种交换机类型

| 类型 | 匹配方式 | 场景 |
|------|----------|------|
| **Direct** | `routingKey = bindingKey` 精确匹配 | 单业务精准推送 |
| **Topic** ⭐ | `*` 单词、`#` 多词 模糊匹配 | 日志分类、复杂路由 |
| **Fanout** | 无视 routingKey，全队列广播 | 消息群发、缓存刷新 |
| Headers | 请求头匹配 | 几乎不用 |

---

## 3. Docker 部署

```bash
docker run -d \
  --name rabbitmq \
  -p 5672:5672 -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=admin \
  -e RABBITMQ_DEFAULT_PASS=123456 \
  --restart always \
  rabbitmq:3-management
```

| 端口 | 用途 |
|:----:|------|
| 5672 | 程序连接端口 |
| 15672 | Web 管理控制台 |

---

## 4. 消息可靠机制

| 机制 | 作用 |
|------|------|
| **消息持久化** | 队列+消息持久化，重启不丢 |
| **生产者确认（Confirm）** | 确保消息到达交换机 |
| **手动 ACK** | 消费者处理完签收，失败重回队列 |
| **死信队列（DLQ）** | 超时/拒收/重试失败 → 转入死信 |

---

## 5. Java Spring Boot 使用

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: admin
    password: 123456
```

| 注解/API | 用途 |
|----------|------|
| `@Configuration` | 配置交换机、队列、绑定 |
| `@RabbitListener` | 消费者监听队列 |
| `rabbitTemplate.convertAndSend()` | 生产者发消息 |

---

## 6. 常见问题

| 问题 | 方案 |
|------|------|
| 消息丢失 | 持久化 + Confirm + 手动 ACK |
| 消息重复 | 业务幂等（唯一 key 去重） |
| 消息积压 | 增加消费者、优化消费速度、拆分队列 |

> **口诀**：生产发交换，交换绑队列；直连精准配，主题模糊追；广播全推送，异步解耦削高峰。
