# 00 - ActiveMQ 总览与核心概念

> 定位：ActiveMQ 知识体系入口——JMS 标准、Classic vs Artemis 深度对比、与其他 MQ 选型矩阵、协议体系、模块导航

## 📚 目录

1. [ActiveMQ 是什么](#1-activemq-是什么)
2. [Classic vs Artemis 深度对比](#2-classic-vs-artemis-深度对比)
3. [协议体系：OpenWire/AMQP/MQTT/STOMP](#3-协议体系openwireamqpmqttstomp)
4. [ActiveMQ vs RabbitMQ vs Kafka 选型矩阵](#4-activemq-vs-rabbitmq-vs-kafka-选型矩阵)
5. [JMS 核心概念](#5-jms-核心概念)
6. [模块导航](#6-模块导航)

---

## 1. ActiveMQ 是什么

```
ActiveMQ = Apache 开源消息中间件（JMS 1.1 / 2.0 标准实现）

诞生：2004 年（最早的 Java MQ 之一）
当前状态：
  ActiveMQ Classic 5.18.x（维护模式，存量巨大）
  ActiveMQ Artemis 2.33+（主力演进，性能接近 Kafka）

核心价值：
  JMS 标准的完整实现 → 任何 JMS 客户端可连接
  多协议支持（OpenWire/AMQP/MQTT/STOMP）
  与 Spring JMS 深度集成

⚠️ 面试必答：
"ActiveMQ = JMS 标准的开源参考实现——
 Classic 是历史积累（存量规模大）、
 Artemis 是下一代（多协议 + 高性能）。"
```

---

## 2. Classic vs Artemis 深度对比

### 2.1 架构差异

| 维度 | Classic 5.x | Artemis 2.x+ |
|------|:---:|:---:|
| I/O 模型 | BIO（阻塞） | **NIO（非阻塞）** |
| 存储引擎 | KahaDB | **Journal（日志文件）** |
| 内存管理 | 固定分页 | 动态分配 |
| 协议 | OpenWire only | **AMQP 1.0 / MQTT 3.1.1 / STOMP 1.2 / OpenWire** |
| 集群 | 网络连接器 | **内建集群**（服务发现） |
| HA | 共享存储/JDBC | **Live-Backup 复制** |
| 大消息 | 需配置 Blob | **原生流式** |
| 性能（吞吐） | ~10K msg/s | ~50K+ msg/s |
| Java 版本 | 8+ | 11+ |
| 新项目推荐 | ❌ 不推荐 | ✅ 首选 |

### 2.2 何时迁移到 Artemis

```
Artemis 适合：
  ✅ 新项目（性能 + 多协议）
  ✅ 需要 AMQP/MQTT（异构语言客户端接入）
  ✅ 需要高吞吐（接近 Kafka 级别）

Classic 继续适合：
  ✅ 存量系统（迁移成本高）
  ✅ 小规模（< 10K msg/s）
  ✅ 团队熟悉 JMS/ActiveMQ 5.x

迁移方案：
  Artemis 内置 OpenWire 协议桥 → 可以 X.classic 客户端连接 Artemis Broker
  （平滑迁移，逐步替换 Broker 后客户端无需修改）
```

---

## 3. 协议体系：OpenWire / AMQP / MQTT / STOMP

### 3.1 四种协议对比

| 协议 | 适用 | 特点 |
|------|------|------|
| **OpenWire** | ActiveMQ 原生 | Classic 默认、二进制高效 |
| **AMQP 1.0** | 企业异构 | 跨语言标准（C++/Python/.NET） |
| **MQTT** | IoT/移动端 | 轻量级、Pub/Sub、低带宽 |
| **STOMP** | 简单文本 | 类似 HTTP、Web/REST 友好 |

### 3.2 协议选型

```
OpenWire：Java 内部 → 最高效（二进制）
AMQP：异构语言（Java ↔ .NET ↔ Python）
MQTT：IoT 设备（传感器/嵌入式）、移动推送
STOMP：Web 端（WebSocket + STOMP over WS）
```

```java
// Artemis 连接 URL 中的协议选择
// tcp://host:61616              → Core（Artemis 原生，最快）
// amqp://host:5672               → AMQP 1.0
// mqtt://host:1883               → MQTT
// stomp://host:61613             → STOMP
```

---

## 4. ActiveMQ vs RabbitMQ vs Kafka 选型矩阵

| 维度 | ActiveMQ Artemis | RabbitMQ | Kafka |
|------|:---:|:---:|:---:|
| 协议 | JMS/AMQP/MQTT/STOMP | **AMQP 0.9.1** | 自定义 |
| 吞吐 | ~50K msg/s | ~20K msg/s | **~1M msg/s** |
| 延迟 | 中 | **低（ms 级）** | 高（批量） |
| 消息顺序 | 单消费者 | 单队列 | **分区内** |
| 持久化 | Journal + 复制 | 磁盘 + 镜像队列 | **磁盘 + 副本** |
| 事务 | ✅ JMS 事务 | ✅ AMQP TX | ❌ |
| Spring | JMS Starter | AMQP Starter | Kafka Starter |
| 伸缩 | 集群 | 集群 + 镜像 | **分区**（天然） |
| 场景 | JMS 生态/混合 | 业务通用 | 大数据流/日志 |
| 运维复杂度 | 中 | 低 | **高** |

```
⚠️ 面试必答选型决策树：
  Java 内部 + JMS 标准 → ActiveMQ
  微服务解耦 + 低延迟 → RabbitMQ
  大数据管道 + 高吞吐 → Kafka
  异构语言混合 → Artemis 的 AMQP/MQTT
  小团队简单 → RabbitMQ
  JMS 遗留兼容 → ActiveMQ Classic
```

---

## 5. JMS 核心概念

### 5.1 两大消息模型

```
P2P（点对点）：Queue → 一条消息一个消费者
  → 适用于任务分发、负载均衡

Pub/Sub（发布订阅）：Topic → 一条消息多个消费者
  → 适用于事件广播、配置刷新
```

### 5.2 JMS 核心接口

```
ConnectionFactory → Connection → Session
  → MessageProducer / MessageConsumer
  → Destination（Queue / Topic）

消息类型：
  TextMessage：JSON 字符串（最常用）
  ObjectMessage：序列化对象（⚠️ 跨语言不兼容）
  BytesMessage：二进制（文件/图片）
  MapMessage：键值对
  StreamMessage：流式数据

⚠️ 面试必答：
"JMS 六接口 + 五消息类型——
 TextMessage（JSON）是生产标准；
 ObjectMessage 有跨语言风险（限制 Java）。"
```

### 5.3 JMS 2.0 简化 API

```java
// JMS 1.1 传统 API（样板代码多）
Connection conn = factory.createConnection();
Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
MessageProducer producer = session.createProducer(session.createQueue("q"));
producer.send(session.createTextMessage("msg"));
conn.close();

// JMS 2.0 简化 API（Artemis 支持）
JMSContext ctx = factory.createContext();          // 自动管理 Connection+ Session
ctx.createProducer().send(ctx.createQueue("q"), "msg");
ctx.close();                                       // 自动关闭
// ⚠️ 减少了 50% 样板代码，与 Spring JMS 的 JmsTemplate 理念一致
```

---

## 6. 模块导航

| 序号 | 模块 | 核心内容 | 定位 |
|------|------|---------|------|
| 00 | [本总览](00-ActiveMQ总览与核心概念.md) | 版本、对比、JMS 协议 | 入口 |
| 01 | [JMS消息模型与可靠性](01-JMS消息模型与可靠性.md) | Queue/Topic/Virtual Topic、持久化、ACK、事务、DLQ | 核心 |
| 02 | [Spring JMS集成](02-SpringJMS集成.md) | JmsTemplate/@JmsListener/MappingJackson2/请求-应答 | 实战 |
| 03 | [Spring Boot集成](03-SpringBoot集成.md) | Starter/Artemis双模/嵌入式Broker/Testcontainers | 实战 |
| 04 | [集群与运维](04-集群与运维.md) | 主从/集群/安全/监控/性能调优 | 运维 |

---

> 🎯 **本体系学习建议**：ActiveMQ 以 **JMS 标准**为主线——先了解协议与模型（00），深入可靠性（01），再学 Spring 集成（02-03），最后运维（04）。面试最大价值 = JMS 标准理解 + Spring JMS 实战 + 与 RabbitMQ/Kafka 的选型对比。

---

**下一篇**：[01-JMS消息模型与可靠性](01-JMS消息模型与可靠性.md)
