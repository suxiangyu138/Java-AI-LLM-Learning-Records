# RabbitMQ 入门指南

> **定位**：消息中间件本质是"中转站"——生产者→RabbitMQ→消费者，实现解耦。RabbitMQ 就像快递驿站：你送到驿站，驿站保管分发，快递员取走。

---

## 目录

1. [核心组件](#1-核心组件)
2. [Docker 部署](#2-docker-部署)
3. [Web 界面操作](#3-web-界面操作)
4. [入门避坑](#4-入门避坑)

---

## 1. 核心组件

| 组件 | 说明 |
|------|------|
| **Producer** | 生产者，发送消息 |
| **Consumer** | 消费者，接收消息 |
| **Queue** | 队列，消息存储仓库（FIFO） |
| **Exchange** | 交换机，消息路由器 |

```text
Producer → Exchange → Queue → Consumer
```

---

## 2. Docker 部署

```bash
docker pull rabbitmq:management
docker run -d -p 5672:5672 -p 15672:15672 --name rabbitmq rabbitmq:management
```

> 访问 `http://localhost:15672`，默认账号密码 `guest/guest`

---

## 3. Web 界面操作

| 步骤 | 操作 |
|:----:|------|
| 1 | 创建 Exchange（`Exchanges` → `Add` → 选 `direct` 类型） |
| 2 | 创建 Queue（`Queues` → `Add`，默认配置） |
| 3 | 绑定（Exchange → `Bindings` → 填 `routingKey` → 选 Queue） |
| 4 | 发消息（Exchange → `Publish message` → 填 Routing key 和内容） |
| 5 | 收消息（Queue → `Get messages` → 查看） |

---

## 4. 入门避坑

| 坑点 | 正确做法 |
|------|----------|
| 一上来深入底层 | 入门先掌握"组件+使用"，Erlang/AMQP 后期再深入 |
| 默认只本地访问 | 远程需创建新用户 |
| 消息默认不持久化 | 创建时勾选 `Durable`，发消息设持久化 |
| 滥用交换机类型 | 入门先用 `direct`，Topic/Fanout 后续学 |
| 报错不知道原因 | `docker logs rabbitmq` 查日志 |
