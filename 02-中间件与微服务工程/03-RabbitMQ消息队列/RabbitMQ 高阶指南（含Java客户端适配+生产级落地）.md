# RabbitMQ 高阶指南（含 Java 客户端适配 + 生产级落地）

> **定位**：突破基础使用边界，实现高可用、高并发、高可靠的消息架构。聚焦幂等、精准延迟、事务优化、架构设计、性能极限调优。

---

## 目录

1. [消息幂等性保障](#1-消息幂等性保障)
2. [延迟队列高阶实现](#2-延迟队列高阶实现)
3. [事务消息高阶优化](#3-事务消息高阶优化)
4. [高阶架构设计](#4-高阶架构设计)
5. [高阶性能调优](#5-高阶性能调优)

---

## 1. 消息幂等性保障

> 核心：确保同一消息多次消费的结果一致。方案 1（Redis）推荐。

```java
// 生产者 → 消息头添加 msgId
properties.setMessageId(UUID.randomUUID().toString());

// 消费者 → Redis 幂等校验
String redisKey = "mq:msg:id:" + msgId;
if (redisTemplate.hasKey(redisKey)) {
    channel.basicAck(deliveryTag, false);  // 已消费，跳过
    return;
}
doBusiness(msg);
redisTemplate.opsForValue().set(redisKey, "1", 24, TimeUnit.HOURS);
channel.basicAck(deliveryTag, false);
```

| 方案 | 原理 | 适用 |
|------|------|------|
| msgId + Redis | 唯一标识 + 缓存去重 | 通用场景 |
| 数据库唯一约束 | 唯一索引防重复插入 | 数据库操作场景 |

---

## 2. 延迟队列高阶实现

> 基于 `rabbitmq_delayed_message_exchange` 插件，无需死信队列。

```bash
rabbitmq-plugins enable rabbitmq_delayed_message_exchange
```

```java
// 声明延迟交换机
new CustomExchange("delay_exchange", "x-delayed-message", true, false,
    Map.of("x-delayed-type", "direct"));

// 发送精准延迟消息（动态设置毫秒级延迟）
properties.setHeader("x-delay", delayTime);
```

---

## 3. 事务消息高阶优化

> 放弃 RabbitMQ 自带事务，采用**本地消息表 + 定时补偿**，性能提升 30%+。

```text
业务操作 + 写入本地消息表（同一 DB 事务）
          │
          ▼
       发送消息（成功→标记已发送 / 失败→标记待补偿）
          │
          ▼
     定时任务扫描 → 重试发送（最多 3 次）
```

---

## 4. 高阶架构设计

### 架构 1：多集群联邦 + 异地容灾

```text
华东主集群 ←→ 华北备用集群 ←→ 华南备用集群
     │              │              │
  华东应用        华北应用        华南应用（就近连接）
```

### 架构 2：集群分片 + 负载均衡

```text
Java 客户端 → 一致性哈希路由 → 分片集群 1 / 分片集群 2 / 分片集群 3
```

---

## 5. 高阶性能调优

| 层级 | 优化点 | 配置 |
|------|--------|------|
| 集群 | SSD 磁盘 | — |
| 配置 | `persistent_queue_store_write_strategy = buffered` | `queue_index_embed_msgs_below = 8192` |
| 配置 | `vm_memory_high_watermark.relative = 0.7` | `connections.max = 500` |
| 代码 | 批量发送 + 批量确认 | `setBatchListener(true)` |
| 代码 | 异步消费 + 线程池 | 自定义 `ThreadPoolExecutor` |
| 代码 | 大消息 GZIP 压缩 | >10KB 压缩 |
