# RocketMQ 面试宝典
> 基于课程大纲全面覆盖面试高频考点，从基础概念到源码原理，从电商实战到系统设计，一站式掌握 RocketMQ 消息中间件

## 目录
1. [一、基础概念速答（20题）](#一基础概念速答20题)
2. [二、深度原理剖析（12题）](#二深度原理剖析12题)
3. [三、实战场景题（10题）](#三实战场景题10题)
4. [四、手写代码/配置文件题（6题）](#四手写代码配置文件题6题)
5. [五、系统设计题（4题）](#五系统设计题4题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板（Top 5）](#七面试回答模板top-5)
8. [八、快速查漏补缺 Checklist](#八快速查漏补缺-checklist)

---

## 一、基础概念速答（20题）

### 1. 什么是 RocketMQ？/ What is RocketMQ?
> RocketMQ 是阿里巴巴开源的分布式消息中间件，具有高吞吐、低延迟、高可用、强一致性等特点，已捐赠给 Apache 基金会。

RocketMQ is a distributed messaging middleware open-sourced by Alibaba, donated to Apache Foundation, featuring high throughput, low latency, high availability, and strong consistency.

### 2. RocketMQ 的核心角色有哪些？/ Core Roles
> **NameServer、Broker、Producer、Consumer** 四大角色。

| 角色 | 作用 | 特点 |
|------|------|------|
| **NameServer** | 路由注册中心 | 无状态、可集群部署，各节点独立不通信 |
| **Broker** | 消息存储与转发 | 支持 Master-Slave 主从部署，存储 CommitLog |
| **Producer** | 消息生产者 | 从 NameServer 获取路由信息，选择队列发送 |
| **Consumer** | 消息消费者 | 支持 Push/Pull 两种模式，维护消费进度 |

> 💡 NameServer 与 ZooKeeper 的区别：NameServer 设计更轻量，Broker 定期向所有 NameServer 发送心跳；ZooKeeper 需要选举，复杂度高。

### 3. 消息类型有哪几种？/ Message Types
> RocketMQ 支持同步、异步、单向三种基本发送方式，以及顺序、延迟、批量、事务等高级消息类型。

| 消息类型 | 特点 | 适用场景 |
|---------|------|---------|
| **Sync（同步）** | 发送后等待 Broker 确认 | 重要业务，如订单、支付通知 |
| **Async（异步）** | 发送后回调确认，不阻塞 | 高吞吐场景，如日志收集 |
| **One-way（单向）** | 发送后不关心结果 | 日志、监控等不重要的数据 |
| **Ordered（顺序）** | 同一 Queue 内严格 FIFO | 订单状态变更、库存扣减 |
| **Delay（延迟）** | 消息在指定延迟时间后才可见 | 订单超时取消、定时任务 |
| **Batch（批量）** | 一批消息一次性发送 | 批量数据同步 |
| **Transaction（事务）** | 基于半消息机制实现分布式事务 | 跨服务数据一致性 |

### 4. Topic 和 Queue 的关系？/ Topic vs Queue
> **Topic** 是逻辑分类，**Queue** 是物理存储单元。一个 Topic 包含多个 Queue，分布在不同的 Broker 上。

- **Topic**：消息的一级分类，类似传统 MQ 中的 Topic
- **Queue**：Topic 下的物理分区，是消息存储和负载均衡的最小单位
- 每个 Queue 对应一个 ConsumeQueue 文件，消费者按 Queue 进行负载均衡
- 一条消息只会落在一个 Queue 中，被一个消费者消费（集群模式下）

> 💡 Queue 数量影响并发能力：Producer 端轮询选择 Queue，Consumer 端按 Queue 分配线程消费。

### 5. Tag 的作用是什么？/ What is Tag?
> Tag 是 Topic 下的二级消息标签，用于在 Topic 内做精细化过滤。

- 一个 Topic 下的消息可以根据业务类型设置不同的 Tag
- Consumer 可以通过 Tag 过滤只消费关心的消息
- Tag 过滤在 Broker 端完成（基于 hash 比对），性能优于 SQL92 过滤器

### 6. SQL92 消息过滤是什么？/ SQL92 Filter
> 通过 SQL92 表达式的属性过滤，支持丰富的条件运算，在 Consumer 端进行。

```java
// 消费者设置 SQL92 过滤表达式
consumer.subscribe("TopicTest", MessageSelector.bySql(
    "age BETWEEN 0 AND 200 AND tag = 'important'"
));
```
> ⚠️ SQL92 过滤需要在 Broker 配置中开启 `enablePropertyFilter=true`。

### 7. 集群模式有哪些？/ Cluster Modes

| 模式 | 描述 | 适用场景 |
|------|------|---------|
| **Single（单机）** | 单台 Broker，无高可用 | 开发测试环境 |
| **2m-2s（双主双从）** | 2 个 Master + 2 个 Slave，同步复制 | 生产环境，数据不丢失 |
| **2m-2s-async（异步复制）** | 2 个 Master + 2 个 Slave，异步复制 | 高吞吐可接受少量丢失 |
| **Dledger** | 基于 Raft 协议选举，自动选主 | 强一致性场景 |
| **Controller** | 基于 Controller 组件选主 | 新版推荐的高可用方案 |

### 8. 广播模式和集群消费模式的区别？
> **Clustering（集群）**：同一条消息只会被 Consumer Group 内一个实例消费一次。
> **Broadcasting（广播）**：同一条消息会被所有订阅的 Consumer 实例消费。

| 特性 | Clustering | Broadcasting |
|------|:--------:|:-----------:|
| 消费次数 | 组内一次 | 每个实例一次 |
| 消费进度 | Broker 维护 | 客户端本地维护 |
| 负载均衡 | 自动 rebalance | 无需均衡 |
| 适用场景 | 高性能队列处理 | 全量通知推送 |

### 9. 什么是 Consumer Group？/ Consumer Group
> 消费者组是同一类消费者的逻辑集合，Group 内实例共同消费 Topic 下的消息。

- 集群模式下，Group 内各实例按 Queue 分配消费（一条消息只被消费一次）
- 广播模式下，Group 内各实例都会收到全量消息
- 消费者组名必须全局唯一，用于标识消费进度

### 10. Message ID 和 Offset 是什么？
> **Message ID** 是消息的唯一标识，**Offset** 是消息在 Queue 中的位置。

| 属性 | 说明 |
|------|------|
| **MsgId** | Producer 端生成，全局唯一 |
| **OffsetMsgId** | Broker 端生成，包含存储地址 |
| **Queue Offset** | 消息在 Queue 中的逻辑偏移量 |
| **CommitLog Offset** | 消息在 CommitLog 文件中的物理偏移量 |

### 11. 消息发送流程是怎样的？
> Producer 发送消息的完整流程：

1. **获取路由**：从 NameServer 获取 Topic 的路由信息（TopicRouteData）
2. **选择 Queue**：根据负载均衡策略选择一个 Queue（默认轮询/一致性 Hash/自定义）
3. **构建消息**：填充 Topic、Tag、Key、Body 等信息
4. **发送消息**：通过网络发送到目标 Broker
5. **处理响应**：同步模式等待 ACK，异步模式执行回调

### 12. 消息消费有哪两种模式？
> RocketMQ 的消费模式支持 Pull（拉模式）和 Push（推模式），但本质都是 Pull。

- **Push**：Consumer 持续轮询 Broker 拉取消息，封装了 Pull 的细节，消息到达后自动推送给业务监听器。客户端通过长轮询（Long Polling）实现"准实时"推送。
- **Pull**：消费者主动调用拉取方法，自行控制拉取频率和时机。

> 💡 RocketMQ 的 Push 模式本质是"伪推送"——底层仍然是客户端循环 Pull 请求，通过长轮询让请求挂起等待消息到达。

### 13. 顺序消息如何实现？/ Ordered Message
> 将消息路由到同一个 Queue，由该 Queue 的有序存储和单线程消费保证顺序。

```java
// Producer 端：选择同一个 MessageQueue
producer.send(msg, new MessageQueueSelector() {
    @Override
    public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
        Long orderId = (Long) arg;
        return mqs.get(orderId.intValue() % mqs.size());
    }
}, orderId);

// Consumer 端：顺序消费
consumer.registerMessageListener(new MessageListenerOrderly() {
    @Override
    public ConsumeOrderlyStatus consumeMessage(
            List<MessageExt> msgs, ConsumeOrderlyContext context) {
        // 单线程处理，保证顺序
        return ConsumeOrderlyStatus.SUCCESS;
    }
});
```

> ⚠️ 顺序消息会牺牲一定的并发性能，且消费失败时不能跳过（会阻塞后续消息）。

### 14. 延迟消息的 18 个等级？/ Delay Message Levels
> RocketMQ 预设了 18 个延迟等级，不支持任意时间延迟。

| Level | 延迟时间 | Level | 延迟时间 | Level | 延迟时间 |
|:-----:|:--------:|:-----:|:--------:|:-----:|:--------:|
| 1 | 1s | 2 | 5s | 3 | 10s |
| 4 | 30s | 5 | 1m | 6 | 2m |
| 7 | 3m | 8 | 4m | 9 | 5m |
| 10 | 6m | 11 | 7m | 12 | 8m |
| 13 | 9m | 14 | 10m | 15 | 20m |
| 16 | 30m | 17 | 1h | 18 | 2h |

```java
// 设置延迟等级为 3（10秒）
message.setDelayTimeLevel(3);
```

> 💡 如果需要任意时间延迟，可以自定义 TimerMessageStore 或通过时间轮实现。

### 15. 事务消息的实现机制？/ Transaction Message
> 基于**半消息（Half Message）+ 事务回查**实现最终一致性。

```
1. Producer 发送半消息（Broker 暂不投递）
2. 执行本地事务
3. 根据本地事务结果 commit 或 rollback 半消息
4. 若步骤 3 超时未响应，Broker 主动回查 Producer 询问事务状态
5. 回查成功后投递或删除消息
```

### 16. RocketMQ 的刷盘策略？
> **同步刷盘**：消息写入内存后立即刷到磁盘，性能低但数据绝对安全。
> **异步刷盘**：消息写入内存后立即返回，后台批量刷盘，性能高但可能丢失少量数据。

- **同步刷盘**：`FlushDiskType.SYNC_FLUSH`，吞吐量约 10万 TPS
- **异步刷盘**：`FlushDiskType.ASYNC_FLUSH`，吞吐量可达 100万+ TPS

### 17. 主从复制方式？
> **同步复制**：Master 收到消息后，等待 Slave 写入成功后返回 ACK，数据不丢失。
> **异步复制**：Master 写入成功后立即返回 ACK，Slave 异步复制，数据可能丢失。

| 复制方式 | 可靠性 | 吞吐量 | 适用场景 |
|---------|:-----:|:------:|---------|
| 同步复制 | 高 | 较低 | 金融、交易等对数据一致性要求极高的场景 |
| 异步复制 | 中 | 高 | 日志、监控等可接受少量丢失的场景 |

### 18. 消息重试机制？
> RocketMQ 在消费失败时支持自动重试，分为 Producer 重试和 Consumer 重试。

- **Producer 端**：同步发送失败后自动重试（默认 2 次），可配置重试次数和间隔
- **Consumer 端**：返回 `RECONSUME_LATER` 或抛出异常时，Broker 会重新投递（最多 16 次）
- 重试间隔随次数递增：10s → 30s → 1m → 2m → ... → 2h

### 19. 死信队列（DLQ）是什么？/ Dead Letter Queue
> 消息重试次数超过最大限制后，会被送入死信队列（DLQ），等待人工处理。

- DLQ 名称格式：`%DLQ%ConsumerGroupName`
- DLQ 中的消息过期后不会被删除，需人工排查并处理
- 处理方式：重新消费、补偿修复或丢弃

### 20. 消息幂等性如何保证？/ Message Idempotency
> 消费者消费消息时，即使收到重复消息，业务结果也保持不变。

| 方案 | 实现方式 | 优点 | 缺点 |
|------|---------|------|------|
| **唯一键去重** | 数据库唯一索引 + 业务 ID | 简单可靠 | 依赖数据库 |
| **Redis 去重** | SETNX 指令 + 过期时间 | 高性能 | 极端情况可能漏判 |
| **状态机校验** | 根据业务状态判断是否已处理 | 无额外依赖 | 需设计状态流转 |

> 🎯 幂等性是面试高频考点，阿里/美团面试中通常会结合订单场景考察。

---

## 二、深度原理剖析（12题）

### 1. RocketMQ 的整体架构是怎样的？/ Overall Architecture

```
                  +--------------+     +--------------+
                  |  NameServer  | ... |  NameServer  |     (无状态路由层)
                  +------+-------+     +------+-------+
                         |                     |
       +-----------------+---------------------+------------------+
       |                 |                     |                  |
+------v------+   +------v------+   +------v------+   +------v------+
| ProducerSet |   | ProducerSet |   | ProducerSet |   | ProducerSet |     (生产者层)
+------+------+   +------+------+   +------+------+   +------+------+
       |                    |                 |                  |
+------v------+   +------v------+   +------v------+   +------v------+
| Broker-M1  |   | Broker-S1  |   | Broker-M2  |   | Broker-S2  |     (存储层)
| (Master)   |<->| (Slave)    |   | (Master)   |<->| (Slave)    |
+------+------+   +------+------+   +------+------+   +------+------+
       ^                    ^                 ^                  ^
       |                    |                 |                  |
+------+--------------------+-----------------+------------------+
       |                                                 |
+------v------+                                   +------v------+
| ConsumerSet |      ... ...                       | ConsumerSet |     (消费者层)
+-------------+                                   +-------------+
```

- **NameServer**：无状态路由中心，各节点独立，Broker 向所有 NameServer 注册
- **Broker Master**：负责读写消息，接收 Producer 写入，Consumer 拉取
- **Broker Slave**：从 Master 同步数据，提供读服务，Master 宕机后接管
- **Producer**：从 NameServer 获取路由，选择队列发送
- **Consumer**：从 NameServer 获取路由，按 Queue 分配负载均衡消费

### 2. NameServer 是如何实现路由发现的？/ NameServer Routing
> NameServer 维护 Broker 的路由元数据，通过心跳机制实现动态注册和故障剔除。

**路由注册流程：**
1. **启动注册**：Broker 启动时向所有 NameServer 发送注册请求
2. **心跳维持**：每 30s 向 NameServer 发送心跳，携带 Topic、Queue 等元数据
3. **路由存储**：NameServer 维护 `RouteInfoManager`，使用 HashMap 存储路由
4. **路由删除**：NameServer 每 10s 扫描一次，120s 未收到心跳则移除 Broker
5. **路由发现**：Producer/Consumer 启动时从 NameServer 拉取 Topic 路由

> 💡 NameServer 集群之间不通信，每个 NameServer 保存全量路由信息，Broker 向所有 NameServer 注册。这种设计避免了一致性选举的复杂度。

### 3. 消息存储设计：CommitLog + ConsumeQueue + IndexFile
> RocketMQ 的文件存储采用"顺序写 + 随机读"的架构，最大化利用磁盘性能。

| 文件 | 作用 | 特点 |
|------|------|------|
| **CommitLog** | 消息主体存储文件 | 所有消息顺序写入单个文件，无磁盘寻道开销 |
| **ConsumeQueue** | 消息消费队列文件 | 每个 Queue 对应一个文件，存储 CommitLog 偏移量 |
| **IndexFile** | 索引文件 | 按 Key 或时间范围快速查找消息 |

**写入流程：** Producer 发消息 → 写入 CommitLog（顺序追加）→ 异步构建 ConsumeQueue & IndexFile

**读取流程：** Consumer 拉取 → 读 ConsumeQueue（获取偏移量 & 大小）→ 从 CommitLog 读取消息体

> 🎯 **为什么 RocketMQ 写入极快？** 因为所有消息都顺序写入 CommitLog，避免随机 I/O；而 Kafka 按 Topic/Partition 写（多个文件）。

### 4. 刷盘机制详解：同步刷盘 vs 异步刷盘
> 刷盘决定消息在 Broker 宕机时是否丢失，是性能和可靠性的权衡。

**同步刷盘（Sync Flush）：**
- 消息写入 `MappedFile` 内存映射区后，立即调用 `MappedByteBuffer.force()`
- 等待刷盘成功后才返回写入成功 ACK
- 性能约降低 70%，吞吐约 10万/s

**异步刷盘（Async Flush）：**
- 使用 `GroupCommitService` 线程池，默认每 500ms 批量刷一次
- 消息写入 `MappedFile` 后立即返回 ACK
- 吞吐可达 100万+/s，但系统崩溃可能丢失最近 500ms 的消息

```java
// 配置文件 broker.conf
flushDiskType=ASYNC_FLUSH   // 或 SYNC_FLUSH
```

> 💡 异步刷盘 + 同步复制组合：保证 Slave 有备份 + 高性能写入，是大多数场景的推荐配置。

### 5. 消息发送源码全流程分析
> 从 `DefaultMQProducer.send()` 进入，跟踪完整发送路径。

**核心步骤：**
1. **消息校验**：检查消息体大小（默认最大 4MB）、Topic 合法性
2. **路由查找**：从 `TopicPublishInfo` 获取 Topic 的 Queue 列表（先查本地缓存，再查 NameServer）
3. **选择 Queue**：默认使用 `SelectMessageQueueByRoudRobin` 轮询选择；顺序消息使用 `MessageQueueSelector`
4. **发送消息**：`NettyRemotingClient.invokeSync()` 通过 Netty 发送请求
5. **故障延迟机制（FaultTolerance）**：发送失败时自动规避故障 Broker，切换到其他 Queue

### 6. 消费者负载均衡机制
> 消费者 Rebalance 确保队列在 Consumer Group 内实例间均匀分配。

**触发条件：**
- Consumer 实例增加/减少（上线/下线）
- Broker 宕机导致 Queue 变化
- Topic 的 Queue 数量变更

**分配策略（AllocateMessageQueueStrategy）：**

| 策略 | 描述 |
|------|------|
| **AllocateMessageQueueAveragely** | 平均分配（默认） |
| **AllocateMessageQueueAverageByCircle** | 轮流分配 |
| **AllocateMessageQueueConsistentHash** | 一致性哈希 |
| **AllocateMessageQueueByConfig** | 手动配置 |
| **AllocateMessageQueueByMachineRoom** | 指定机房分配 |

**Rebalance 过程：**
1. Consumer 向 Broker 发送心跳
2. Broker 通知 Group 内所有实例触发 Rebalance
3. 各实例执行 `AllocateMessageQueueStrategy`，重新分配 Queue
4. 新 Queue 分配后，释放旧 Queue，拉取新 Queue 的消息

> ⚠️ Rebalance 过程中可能出现"同一条消息被两个消费者同时消费"的情况，因此幂等性很重要。

### 7. 消息重试与死信队列机制
> RocketMQ 提供 16 级递增重试，重试全部失败后进入 DLQ。

**消费失败触发重试：**
```java
// 返回 RECONSUME_LATER 触发重试
public ConsumeConcurrentlyStatus consumeMessage(
        List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
    try {
        process(msgs);
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    } catch (Exception e) {
        return ConsumeConcurrentlyStatus.RECONSUME_LATER; // 触发重试
    }
}
```

**重试等级递增表：**

| 重试次数 | 延迟时间 | 重试次数 | 延迟时间 |
|:-------:|:--------:|:-------:|:--------:|
| 1 | 10s | 2 | 30s |
| 3 | 1m | 4 | 2m |
| 5 | 3m | 6 | 4m |
| 7 | 5m | 8 | 6m |
| 9 | 7m | 10 | 8m |
| 11 | 9m | 12 | 10m |
| 13 | 20m | 14 | 30m |
| 15 | 1h | 16 | 2h |

> 第 16 次重试（2h）后仍失败 → 进入 DLQ：`%DLQ%ConsumerGroupName`

### 8. 事务消息的完整实现与源码分析
> RocketMQ 事务消息基于"两阶段提交 + 事务反查"实现最终一致性。

**第一阶段：发送半消息**
- Producer 发送 Half 消息（Topic 为 `RMQ_SYS_TRANS_HALF_TOPIC`）
- Broker 保存半消息到 CommitLog，但不对 Consumer 可见

**第二阶段：执行本地事务**
- Producer 收到半消息 ACK 后，执行本地事务（如 insert 订单）
- 返回 `LocalTransactionState.COMMIT_MESSAGE` 或 `ROLLBACK_MESSAGE`

**第三阶段：事务回查**
- Broker 扫描半消息队列，发现未决消息（状态未知）
- Broker 回查 Producer 的 `checkLocalTransaction()` 方法
- Producer 返回最终状态，Broker 提交或回滚

```java
// TransactionListener 实现
public class OrderTransactionListener implements TransactionListener {
    @Override
    public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        // 执行本地事务（如扣库存、生成订单）
        return LocalTransactionState.UNKNOW; // 返回 UNKNOW 触发回查
    }

    @Override
    public LocalTransactionState checkLocalTransaction(MessageExt msg) {
        // 回查：检查本地事务是否成功
        return LocalTransactionState.COMMIT_MESSAGE;
    }
}
```

### 9. 顺序消息的实现原理
> 顺序消息的核心是**同一队列单线程消费 + 队列选择器**。

**Producer 端保证写入顺序：**
- 使用 `MessageQueueSelector` 将同类消息路由到同一个 Queue
- 例如订单 ID % Queue 数量，保证同一订单的消息进入同一个 Queue

**Broker 端保证存储顺序：**
- 消息在同一个 Queue 内按 CommitLog 写入顺序存储

**Consumer 端保证消费顺序：**
- `MessageListenerOrderly` 使用锁机制，每个 Queue 由固定线程处理
- 消费失败时挂起整个队列，直到重试成功（不会跳过）
- 使用 `ConsumeOrderlyStatus.SUCCESS` 提交消费进度

### 10. 延迟消息的底层实现
> 延迟消息利用 SCHEDULE_TOPIC_XXXX 的 18 个 ConsumeQueue 实现定时投递。

**底层原理：**
1. Producer 设置延迟等级，消息写入 CommitLog
2. Broker 的 `ScheduleMessageService` 定时扫描延迟消息
3. 到期后重新投递到原始 Topic 的 ConsumeQueue
4. Consumer 看到的仍然是原始 Topic，感知不到延迟

**18 个 Queue 映射：**
- `SCHEDULE_TOPIC_XXXX` 的 Queue 0 → 1s，Queue 1 → 5s ... Queue 17 → 2h
- 每个 Queue 有单独的定时线程轮询扫描
- 使用 `TimerLog` 记录延迟消息的写入时间

> 💡 如果需要更精细的延迟时间，可以修改 `messageDelayLevel` 配置或自行实现时间轮。

### 11. 高可用机制：Master-Slave 切换与 Dledger/Raft
> RocketMQ 提供多层级的高可用保障。

**Master-Slave 自动切换：**
- 2m-2s 模式下，Master 宕机后 Consumer 和 Producer 自动切换到 Slave
- Slave 提供读服务，但不支持写入
- 需要人工或监控工具切换 Slave 为 Master

**Dledger 模式（Raft 协议）：**
- 基于 Raft 实现自动 Leader 选举
- 写请求必须写入多数节点（如 3 节点中至少 2 个成功）才返回 ACK
- 自动选主：Leader 宕机后 Follower 自动发起选举
- 强一致性保证

**Controller 模式（新版推荐）：**
- 引入独立的 Controller 组件或 Embed 在 Broker 内
- 基于 Raft/心跳实现自动 Failover
- 支持平滑升级和自动切换

### 12. 长轮询机制详解 / Long Polling
> RocketMQ 的 Push 模式底层依赖长轮询实现"准实时"消息推送。

**长轮询过程：**
1. Consumer 发送 Pull 请求到 Broker
2. Broker 检查是否有新消息
3. 如果没有新消息，请求挂起（默认 15s）
4. 新消息到达或超时时，Broker 返回响应
5. Consumer 收到消息后立即发起下一次 Pull

**关键源码位置：**
```java
// Broker 端：PullRequestHoldService 管理挂起的 Pull 请求
// 有新消息时 notifyMessageArriving() 唤醒等待的请求
public void checkHoldRequest() {
    // 遍历所有挂起的 Pull 请求，检查是否有新消息到达
}
```

**优势：** 相比 Kafka 的纯 Pull 模式，长轮询大幅降低消息延迟（毫秒级 vs 轮询间隔）；相比 Push 模式，消费者能自主控制消费速率（背压保护）。

### 13. RocketMQ vs Kafka vs RabbitMQ 对比

| 特性 | RocketMQ | Kafka | RabbitMQ |
|------|---------|-------|---------|
| **开发语言** | Java | Scala/Java | Erlang |
| **吞吐量** | 10万+/s | 100万+/s | 1万+/s |
| **延迟** | 毫秒级 | 毫秒级 | 微秒级 |
| **消息可靠性** | 极高（事务消息） | 高（ACK 机制） | 高（Confirm 机制） |
| **顺序消息** | 支持（Queue 内有序） | 支持（Partition 内有序） | 不保证全局有序 |
| **延迟消息** | 支持（18 级） | 不支持 | 支持（插件） |
| **事务消息** | 支持 | 支持（Kafka 0.11+） | 不支持原生 |
| **死信队列** | 支持 | 支持 | 支持 |
| **管理控制台** | 完善（Console） | 一般 | 完善 |
| **适用场景** | 金融/订单/交易 | 日志/大数据/流处理 | 中小系统/消息路由 |

> 🎯 面试高频题："为什么选择 RocketMQ？"答案：事务消息、顺序消息、高可用、Java 生态。

---

## 三、实战场景题（10题）

### 1. 订单系统的分布式事务如何设计？
> **场景**：用户下单需要扣减库存、锁定优惠券、扣减余额等多个操作，要求最终一致性。

**方案：RocketMQ 事务消息 + 本地事务表**

```java
// 1. 发送半消息
TransactionMQProducer producer = new TransactionMQProducer("order_group");
producer.setTransactionListener(new OrderTransactionListener());

// 2. 本地事务执行
public class OrderTransactionListener implements TransactionListener {
    @Override
    public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        Order order = (Order) arg;
        // 开启本地事务
        orderService.createOrder(order);        // 生成订单
        inventoryService.deduct(order);         // 扣减库存
        couponService.update(order);            // 更新优惠券
        return LocalTransactionState.COMMIT_MESSAGE;
    }
}
```

> 💡 限时优惠抢购场景需要额外处理：Redis 预扣库存 → MQ 异步扣减 → 最终一致性。

### 2. 支付回调异步通知如何实现？
> **场景**：支付成功后需要通知订单服务更新状态、通知物流系统发货、发送短信通知用户。

**方案：延迟消息 + 回调通知重试**
1. 支付网关回调支付服务
2. 支付服务查询支付结果，确认成功后发送消息到 MQ
3. 订单服务消费消息，更新订单状态
4. 物流服务消费消息，开始发货流程
5. 短信服务消费消息，发送通知短信

> ⚠️ 回调消息一定要设置唯一键（支付流水号）用于幂等处理，防止重复回调。

### 3. 库存回滚的幂等性处理？
> **场景**：下单过程中某个步骤失败，需要回退已扣减的库存。

**幂等性方案：**

| 方案 | 实现 |
|------|------|
| **唯一约束** | 数据库表设置 `order_id + sku_id` 唯一索引 |
| **状态机** | 订单状态：待支付 → 已支付 → 已发货 → 已完成，回滚只允许从未支付的订单 |
| **Redis 去重** | `SETNX order_rollback_{orderId}_{skuId}` |

```java
// 幂等回退库存
public void rollbackInventory(OrderRollbackMessage msg) {
    String key = "rollback:" + msg.getOrderId() + ":" + msg.getSkuId();
    Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "1", 1, TimeUnit.DAYS);
    if (Boolean.TRUE.equals(success)) {
        inventoryService.increment(msg.getSkuId(), msg.getQuantity());
        log.info("库存回滚成功: orderId={}, skuId={}", msg.getOrderId(), msg.getSkuId());
    } else {
        log.info("库存回滚已处理, 跳过: orderId={}", msg.getOrderId());
    }
}
```

### 4. 优惠券回滚如何处理？
> **场景**：下单使用优惠券后订单取消，需要回退优惠券。

**要点：**
- 回退时必须恢复优惠券到"未使用"状态，并恢复有效期
- 需要考虑优惠券已过期的情况（过期不恢复）
- 使用 `orderId` + 唯一约束防止重复回退

### 5. 订单取消的消息处理流程？
> **场景**：用户取消订单或超时未支付自动取消。

```java
// 延迟消息实现订单超时取消
// 下单时发送延迟消息
Message message = new Message("order_topic", "cancel_tag",
    orderId.getBytes());
message.setDelayTimeLevel(16); // 30 分钟后超时
producer.send(message);

// 消费端处理
public ConsumeConcurrentlyStatus consumeMessage(
        List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
    String orderId = new String(msgs.get(0).getBody());
    Order order = orderService.getOrder(orderId);
    if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
        orderService.cancel(orderId);   // 取消订单
        inventoryService.rollback(orderId); // 回滚库存
        couponService.rollback(orderId);    // 回退优惠券
    }
    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
}
```

> 💡 注意：延迟消息触发取消订单前，用户可能已经支付，需要先检查订单状态。

### 6. 线程池优化消息发送性能？
> **场景**：高并发下单场景，裸用 `producer.send()` 会导致频繁的 IO 阻塞。

**优化方案：异步发送 + 线程池**
```java
// 配置线程池
ThreadPoolExecutor sendExecutor = new ThreadPoolExecutor(
    20, 50, 60, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(1000),
    new ThreadFactoryBuilder().setNameFormat("msg-sender-%d").build(),
    new ThreadPoolExecutor.CallerRunsPolicy()
);

// 批量异步发送
for (Order order : orders) {
    sendExecutor.submit(() -> {
        Message msg = new Message("order_topic", order.toBytes());
        producer.send(msg, new SendCallback() {
            @Override
            public void onSuccess(SendResult result) {
                log.info("发送成功: {}", result.getMsgId());
            }
            @Override
            public void onException(Throwable e) {
                log.error("发送失败: {}", order.getId(), e);
                // 补偿队列
                compensationService.save(order);
            }
        });
    });
}
```

### 7. 失败补偿机制如何设计？
> **场景**：消息发送失败、消费失败等各种异常需要补偿。

**三条补偿策略：**

| 层级 | 策略 | 实现 |
|------|------|------|
| **1-内存** | 发送时失败立即重试 | Producer 内置重试 2 次 |
| **2-数据库** | 补偿表持久化未发送消息 | 定时任务扫描补偿表，重新发送 |
| **3-手动** | 管理后台手动触发补偿 | 运维排查 + 手动重发 |

```sql
-- 消息补偿表
CREATE TABLE msg_compensation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    msg_key VARCHAR(64) NOT NULL COMMENT '业务唯一键',
    topic VARCHAR(128) NOT NULL,
    msg_body TEXT NOT NULL,
    retry_count INT DEFAULT 0,
    max_retry INT DEFAULT 5,
    status TINYINT DEFAULT 0 COMMENT '0-待发送 1-已发送 2-已过期',
    create_time DATETIME,
    update_time DATETIME
);
```

### 8. Spring Boot 集成 RocketMQ 的最佳实践？
> **场景**：新项目快速集成 RocketMQ。

```yaml
# application.yml
rocketmq:
  name-server: 127.0.0.1:9876
  producer:
    group: my-producer-group
    send-message-timeout: 3000
    retry-times-when-send-failed: 2
  consumer:
    - group: my-consumer-group
      topic: order-topic
      tag: payment||cancel
      consume-thread-min: 10
      consume-thread-max: 30
```

### 9. 多模块消息路由设计？
> **场景**：微服务架构中，订单服务发送消息，多个下游服务需要订阅不同的消息。

**设计要点：**
- 使用 Tag 区分消息类型：`order_topic:create`、`order_topic:payment`、`order_topic:cancel`
- 每个服务只订阅自己关心的 Tag
- 考虑消息 Schema 管理（ProtoBuf/JSON Schema）

### 10. 死信队列的恢复与处理？
> **场景**：消息进入死信队列后如何排查和恢复。

**处理步骤：**
1. **告警**：DLQ 消息产生时发送告警通知
2. **查询**：通过 RocketMQ Console 查询死信消息
3. **分析**：查看消息体和异常原因（反序列化？业务逻辑？）
4. **修复**：修复 Bug 后，通过 Console 重新投递
5. **补偿**：手动执行补偿脚本处理部分失败的业务

```shell
# RocketMQ Console 手动重新投递
# 或通过 API
consumer.getDefaultMQPushConsumer()
    .sendMessageBack(brokerName, msg, delayLevel);
```

---

## 四、手写代码/配置文件题（6题）

### 1. Spring Boot 生产者和消费者配置
> 面试中常见的 Spring Boot 集成 RocketMQ 完整配置。

**依赖 `pom.xml`：**
```xml
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
    <version>2.2.3</version>
</dependency>
```

**配置文件 `application.yml`：**
```yaml
rocketmq:
  name-server: 192.168.1.100:9876;192.168.1.101:9876
  producer:
    group: spring-producer-group
    send-message-timeout: 3000
    compress-message-body-threshold: 4096
    max-message-size: 4096000
    retry-times-when-send-async-failed: 2
    retry-times-when-send-failed: 2
```

**生产者代码：**
```java
@Component
public class OrderMessageProducer {
    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    public void sendOrderMessage(OrderDTO order) {
        Message<String> message = MessageBuilder
            .withPayload(JSON.toJSONString(order))
            .setHeader("orderId", order.getId())
            .build();
        rocketMQTemplate.syncSend("order-topic:payment", message);
    }

    public void sendAsync(SendCallback callback) {
        rocketMQTemplate.asyncSend("order-topic:async", message, callback);
    }
}
```

**消费者代码：**
```java
@Component
@RocketMQMessageListener(
    topic = "order-topic",
    selectorExpression = "payment || cancel",
    consumerGroup = "order-consumer-group",
    consumeMode = ConsumeMode.CONCURRENTLY,
    messageModel = MessageModel.CLUSTERING
)
public class OrderConsumer implements RocketMQListener<MessageExt> {
    @Override
    public void onMessage(MessageExt message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        OrderDTO order = JSON.parseObject(body, OrderDTO.class);
        // 处理业务逻辑
    }
}
```

### 2. 事务消息 Producer + TransactionListener
> 完整的分布式事务消息代码。

```java
public class TransactionProducer {
    public static void main(String[] args) throws Exception {
        TransactionMQProducer producer = new TransactionMQProducer("tx-producer-group");
        producer.setNamesrvAddr("127.0.0.1:9876");

        // 设置线程池处理回查
        producer.setExecutorService(Executors.newFixedThreadPool(5));
        producer.setTransactionListener(new OrderTransactionListener());
        producer.start();

        // 发送半消息
        Order order = new Order(1001L, BigDecimal.valueOf(99.99));
        Message message = new Message("order-tx-topic", "create",
            JSON.toJSONBytes(order));
        // 使用 orderId 作为事务标识
        TransactionSendResult result = producer.sendMessageInTransaction(message, order);
    }
}

class OrderTransactionListener implements TransactionListener {
    // 本地事务执行，返回 COMMIT / ROLLBACK / UNKNOW
    @Override
    public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        try {
            Order order = (Order) arg;
            orderDao.insert(order);              // 插入订单
            inventoryDao.deduct(order.getSku()); // 扣减库存
            return LocalTransactionState.COMMIT_MESSAGE;
        } catch (Exception e) {
            return LocalTransactionState.ROLLBACK_MESSAGE;
        }
    }

    // 事务回查：检查本地事务是否成功
    @Override
    public LocalTransactionState checkLocalTransaction(MessageExt msg) {
        String orderId = msg.getKeys();
        Order order = orderDao.selectById(orderId);
        if (order != null) {
            return LocalTransactionState.COMMIT_MESSAGE;
        }
        return LocalTransactionState.UNKNOW;
    }
}
```

### 3. 顺序消息 Producer + Consumer
> 保证同一业务 ID 的消息严格有序。

```java
// Producer 端：选择同一个 Queue
public class OrderMessageProducer {
    public static void main(String[] args) throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("order-producer-group");
        producer.setNamesrvAddr("127.0.0.1:9876");
        producer.start();

        Long orderId = 1001L;  // 业务 ID
        Message msg = new Message("order-seq-topic", "create",
            ("order-" + orderId).getBytes());

        // MessageQueueSelector 确保同一 orderId 进入同一个 Queue
        SendResult result = producer.send(msg, (mqs, message, arg) -> {
            Long id = (Long) arg;
            int index = (int) (id % mqs.size());
            return mqs.get(index);
        }, orderId);
    }
}

// Consumer 端：顺序消费
public class OrderConsumer {
    public static void main(String[] args) throws Exception {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("order-consumer-group");
        consumer.setNamesrvAddr("127.0.0.1:9876");
        consumer.subscribe("order-seq-topic", "*");

        // 使用 MessageListenerOrderly 保证顺序
        consumer.registerMessageListener(new MessageListenerOrderly() {
            @Override
            public ConsumeOrderlyStatus consumeMessage(
                    List<MessageExt> msgs, ConsumeOrderlyContext context) {
                for (MessageExt msg : msgs) {
                    System.out.println("顺序消费: " + new String(msg.getBody()));
                }
                return ConsumeOrderlyStatus.SUCCESS;
            }
        });
        consumer.start();
    }
}
```

### 4. 延迟消息发送
> 演示 18 级延迟消息的使用。

```java
public class DelayMessageProducer {
    public static void main(String[] args) throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("delay-producer-group");
        producer.setNamesrvAddr("127.0.0.1:9876");
        producer.start();

        for (int i = 1; i <= 18; i++) {
            Message msg = new Message("delay-topic", "tagA",
                ("延迟消息 Level-" + i).getBytes());
            msg.setDelayTimeLevel(i);  // 设置延迟等级 1~18
            producer.send(msg);
            System.out.println("发送延迟等级: " + i);
        }
        producer.shutdown();
    }
}

// 自定义延迟等级（修改 broker.conf）
// messageDelayLevel=1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
```

### 5. Tag 过滤 + SQL92 过滤
> 展示两种消息过滤方式。

```java
// Tag 过滤（Broker 端性能最优）
DefaultMQPushConsumer tagConsumer = new DefaultMQPushConsumer("tag-filter-group");
// 同时订阅多个 Tag，用 || 分隔
tagConsumer.subscribe("tag-filter-topic", "tagA || tagB || tagC");
// "*" 表示消费所有 Tag
// tagConsumer.subscribe("tag-filter-topic", "*");

// SQL92 过滤（属性级过滤，功能更强）
DefaultMQPushConsumer sqlConsumer = new DefaultMQPushConsumer("sql-filter-group");
sqlConsumer.subscribe("sql-filter-topic", MessageSelector.bySql(
    "TAGS IS NOT NULL AND TAGS IN ('tagA', 'tagB') " +
    "AND age >= 18 AND isVip = true"
));

// 生产者发送时设置属性
Message msg = new Message("sql-filter-topic", "tagA", body);
msg.putUserProperty("age", "25");
msg.putUserProperty("isVip", "true");
producer.send(msg);
```

> ⚠️ SQL92 过滤需要在 Broker 配置文件开启：`enablePropertyFilter=true`，且会增加 Broker CPU 开销。

### 6. 批量消息 + 异步发送
> 批量发送提高吞吐量，异步发送避免阻塞。

```java
public class BatchAsyncProducer {
    public static void main(String[] args) throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("batch-producer-group");
        producer.setNamesrvAddr("127.0.0.1:9876");
        producer.start();

        List<Message> batchMessages = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Message msg = new Message("batch-topic", "tagA",
                ("消息-" + i).getBytes());
            batchMessages.add(msg);
        }

        // 批量发送（一次最多 4MB）
        producer.send(batchMessages);

        // 异步发送
        for (int i = 0; i < 100; i++) {
            Message msg = new Message("async-topic", "tagA",
                ("异步-" + i).getBytes());
            producer.send(msg, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    log.info("异步发送成功: {}", sendResult.getMsgId());
                }

                @Override
                public void onException(Throwable e) {
                    log.error("异步发送失败", e);
                }
            });
        }
        producer.shutdown();
    }
}
```

> 💡 批量消息大小超过 4MB 时会被 Broker 拒绝，需要手动拆分（Split）。可以通过 `ListSplitter` 工具类按大小拆分。

---

## 五、系统设计题（4题）

### 1. 设计可靠的订单-支付分布式事务
> **面试题**：设计一个高可靠的分布式订单支付系统，要求数据最终一致，不丢失消息。

**架构设计：**

```
┌─────────┐  TX  ┌──────────────┐
│ 订单服务 │─────▶│ 支付服务     │
│         │      │ (核心支付)   │
└────┬────┘      └──────┬───────┘
     │                   │
     │ 事务消息           │ 回调通知
     ▼                   ▼
┌──────────────────────────────────┐
│        RocketMQ 集群             │
│   order-tx-topic   payment-topic │
│   coupon-topic     inventory-topic│
└───┬────┬────┬────┬───────────────┘
    │    │    │    │
    ▼    ▼    ▼    ▼
  库存  优惠券 物流 短信服务
```

**关键设计点：**
1. **事务消息**：订单创建使用 RocketMQ 事务消息，确保订单和库存扣减最终一致
2. **支付回调**：支付成功后发消息，下游服务幂等消费
3. **补偿机制**：本地补偿表 + 定时任务，失败重试 16 次
4. **监控告警**：DLQ 监控、消息积压告警、重试超限告警

### 2. 设计海量日志收集系统
> **面试题**：设计一个支撑千万级 TPS 的海量日志收集系统。

**方案：**

| 组件 | 选型 | 作用 |
|------|------|------|
| **日志采集端** | Filebeat / Logstash | 收集应用日志 |
| **消息队列** | RocketMQ（异步刷盘） | 削峰填谷，解耦生产者消费者 |
| **日志清洗** | Flink / Logstash | 解析、清洗、格式化日志 |
| **存储查询** | Elasticsearch + Kibana | 全文检索与分析 |
| **长期存储** | HDFS / OSS | 归档冷数据 |

**RocketMQ 配置优化：**
- 使用 `ASYNC_FLUSH` 刷盘（日志可接受少量丢失）
- 使用 `One-way` 或 `Async` 发送模式
- Broker 使用 SSD 磁盘
- 合理设置 Topic 的 Queue 数量（建议 16~64 个）

### 3. 设计 MQ 迁移方案（RabbitMQ → RocketMQ）
> **面试题**：公司要从 RabbitMQ 迁移到 RocketMQ，如何平滑迁移？

**双写双读方案（平滑迁移）：**

```
阶段一：双写 + RabbitMQ 消费
  App ──▶ 双写 RabbitMQ + RocketMQ
         Consumer 只消费 RabbitMQ

阶段二：双写 + 双读
  App ──▶ 双写 RabbitMQ + RocketMQ
         Consumer 同时消费两个 MQ（逐步切流）

阶段三：单写 RocketMQ
  App ──▶ 只写 RocketMQ
         Consumer 只消费 RocketMQ
```

**迁移关键点：**
1. **消息格式兼容**：确保 RocketMQ 的消息体与 RabbitMQ 一致或做转换层
2. **顺序保障**：校验两个 MQ 中的消息顺序是否一致
3. **流量对比**：迁移期间对比两个 MQ 的消息量和延迟
4. **回滚方案**：出现问题立即切回 RabbitMQ

### 4. 设计多机房消息复制方案
> **面试题**：跨地域多机房部署，如何实现消息的跨机房复制和高可用？

**方案一：基于 RocketMQ DLedger 跨机房复制**
```
机房A (北京)       机房B (上海)      机房C (广州)
  Broker-M1 ────── Broker-M2 ────── Broker-M3
  (Leader)         (Follower)       (Follower)
```

**方案二：独立复制管道**
```
机房A                        机房B
  Producer ──▶ BrokerA ──▶ MQ Bridge ──▶ BrokerB ──▶ Consumer
```

**方案三：RocketMQ 5.x Controller 模式**
- 跨机房部署 Controller 集群（3 或 5 节点）
- Broker 自动选主，机房故障自动切换
- 支持设置 `brokerId=-1` 的 Slave 只做备份

> 🎯 面试重点：RocketMQ 5.x 的 Controller 模式是最新推荐的跨机房高可用方案。

---

## 六、常见坑点与最佳实践

| 序号 | 坑点/问题 | 原因 | 最佳实践 |
|:---:|----------|------|---------|
| 1 | **消息丢失** | Producer 未使用同步或异步发送，One-way 模式下不确认 | 使用同步发送 + 重试，或异步发送 + Callback |
| 2 | **重复消息** | 网络超时导致 Producer 重试，Broker 写入成功但 ACK 丢失 | Consumer 层必须做幂等处理 |
| 3 | **消息堆积** | Consumer 消费速度低于生产速度 | 增加 Queue 数量 + 增加 Consumer 实例，或提高消费并行度 |
| 4 | **顺序消息卡死** | 顺序消费模式下一条消息失败阻塞后续消息 | 合理设置重试次数，失败后手动记录并跳过 |
| 5 | **事务消息超时未回查** | `checkLocalTransaction` 抛出异常或超时 | 确保回查接口幂等且快速响应，设置合适的回查间隔 |
| 6 | **Topic 不存在自动创建** | 生产环境误发送未创建的 Topic | Broker 配置 `autoCreateTopicEnable=false` |
| 7 | **消息体过大** | 超过默认 4MB 限制 | 大消息先压缩，或存储到 OSS 并发送链接 |
| 8 | **Tag 过长** | Tag 最长 128 字符，超长会报错 | Tag 使用简短枚举值，业务分类用 UserProperty |
| 9 | **Consumer Group 混用** | 不同消费逻辑使用相同的 Group 名 | Consumer Group 命名与业务逻辑一一对应 |
| 10 | **Producer Group 混用** | 不同应用使用同一个 Producer Group | 每个微服务使用独立的 Producer Group |
| 11 | **NameServer 单点** | 只配置一个 NameServer 地址 | 至少部署 2 个 NameServer，Producer/Consumer 配置全部地址 |
| 12 | **刷盘配置不当** | 高并发使用 SYNC_FLUSH 导致性能瓶颈 | 读多写少用 ASYNC_FLUSH，重要交易用 SYNC_FLUSH + 同步复制 |
| 13 | **Rebalance 风暴** | Consumer 频繁上下线导致大量 Rebalance | 使用 `ConsumerGroup` 合理控制实例数，减少非必要启停 |
| 14 | **消息 Key 未设置** | 无法按业务 ID 快速查消息 | 发送时设置 `msg.setKeys(orderId)` 便于查询和去重 |
| 15 | **批量发送超限** | 一批消息总大小超过 4MB | 使用 `ListSplitter` 拆分后分批发送 |
| 16 | **延迟消息堆积** | `SCHEDULE_TOPIC_XXXX` 队列数不够 | 每个 Broker 的延迟消息队列数可以调整 |
| 17 | **关闭顺序消息** | 顺序消费慢、吞吐量低 | 只对真正需要顺序的业务使用，非关键链路用并发消费 |

> 💡 面试技巧：回答坑点问题时，先说明问题现象 → 分析根本原因 → 给出解决方案 → 补充预防措施。

---

## 七、面试回答模板（Top 5）

### 模板 1：RocketMQ 如何保证消息不丢失？
```text
"RocketMQ 从三个层面保证消息不丢失：

1. **Producer 端**：使用同步发送 + 重试机制（默认 2 次），发送失败时 Producer 会自动重试到其他 Broker。

2. **Broker 端**：支持同步刷盘（写入磁盘后才返回 ACK）和同步复制（Master 和 Slave 都写入才返回 ACK）。生产环境推荐 同步刷盘 + 同步复制 的组合。

3. **Consumer 端**：消息消费完成后才提交 Offset，如果消费失败会触发重试机制（16 次递增重试），最终进入死信队列等待人工处理。

总结：Producer 同步发送 + Broker 刷盘/复制 + Consumer 确认机制，三层保障消息不丢失。"
```

### 模板 2：RocketMQ 如何处理顺序消息？
```text
"RocketMQ 的顺序消息基于两个设计：

1. **Producer 端队列选择**：使用 MessageQueueSelector 将同一业务 ID 的消息发送到同一个 Queue。比如订单 ID % Queue 数量，保证同一订单的消息进入同一个 Queue。

2. **Consumer 端单线程消费**：使用 MessageListenerOrderly，每个 Queue 只有一个线程消费，消费失败时挂起整个队列（不会跳过），从而保证写入和消费的顺序一致。

需要注意的是，顺序消息会降低吞吐量，因此只对真正需要保证顺序的业务使用，例如订单状态变更、库存扣减等。"
```

### 模板 3：RocketMQ 事务消息的原理？
```text
"RocketMQ 的事务消息基于 两阶段提交 + 事务回查 实现最终一致性：

第一阶段（半消息）：Producer 发送半消息到 Broker，Broker 将消息保存到 RMQ_SYS_TRANS_HALF_TOPIC，此时消息对消费者不可见。

第二阶段（本地事务）：Producer 收到半消息 ACK 后执行本地事务（如 insert 订单），然后向 Broker 提交 Commit 或 Rollback。

事务回查：如果 Producer 在第二阶段异常（未及时返回状态），Broker 会主动回调 TransactionListener 的 checkLocalTransaction() 方法查询事务状态。

核心优势：相比传统 XA 分布式事务，RocketMQ 事务消息不阻塞数据库资源，性能更好，适合高并发场景。"
```

### 模板 4：RocketMQ 和 Kafka 的区别？
```text
"RocketMQ 和 Kafka 的差异主要体现在以下几个方面：

1. **消息模型**：RocketMQ 的消费队列（ConsumeQueue）是逻辑队列，所有消息顺序写入一个 CommitLog；Kafka 则每个 Partition 独立写入。

2. **事务消息**：RocketMQ 原生支持事务消息（半消息 + 回查），Kafka 0.11+ 才支持事务。

3. **顺序消息**：两者都支持分区内顺序，但 RocketMQ 的 MessageListenerOrderly 实现更简单。

4. **延迟消息**：RocketMQ 内置 18 级延迟消息，Kafka 不支持。

5. **延迟对比**：RocketMQ 在毫秒级延迟下吞吐约 10万/s，Kafka 可达 100万+/s。

选择建议：金融、交易、订单场景选 RocketMQ；海量日志、大数据流处理选 Kafka。"
```

### 模板 5：RocketMQ 消息幂等性如何实现？
```text
"消息幂等性是分布式系统中的核心问题，主要有三种实现方式：

1. **数据库唯一约束**：利用数据库的唯一索引或主键约束。例如消费支付成功消息时，用 payment_id + order_id 建立唯一索引，重复插入会报错，捕获后直接返回 SUCCESS。

2. **Redis 去重**：消费前先 SETNX 一个去重 Key，Key 包含业务唯一 ID 和操作类型，设置过期时间。成功才执行业务逻辑。

3. **状态机校验**：根据业务的当前状态判断是否已处理。例如订单状态流转：待支付 → 已支付 → 已发货，消费支付成功消息时，只有待支付的订单才更新为已支付。

在实际项目中，通常组合使用：数据库唯一索引兜底 + Redis 预检，双重保障。"
```

---

## 八、快速查漏补缺 Checklist

> 面试前逐项自检，打勾表示掌握。

### 基础概念
- [ ] 1. RocketMQ 是什么？核心特点有哪些？
- [ ] 2. 四大角色：NameServer、Broker、Producer、Consumer 的职责
- [ ] 3. 五种消息类型：Sync、Async、One-way、Ordered、Transaction
- [ ] 4. Topic 和 Queue 的关系与区别
- [ ] 5. Tag 过滤和 SQL92 过滤的区别
- [ ] 6. 集群模式：Single、2m-2s、2m-2s-async、Dledger、Controller
- [ ] 7. 广播模式 vs 集群消费模式
- [ ] 8. Consumer Group 的概念与作用
- [ ] 9. Message ID 的类型（MsgId vs OffsetMsgId）
- [ ] 10. Offset 的概念与存储
- [ ] 11. 延迟消息的 18 个等级
- [ ] 12. 顺序消息的实现条件
- [ ] 13. 批量消息的限制（4MB）
- [ ] 14. 事务消息的 HALF 状态
- [ ] 15. RocketMQ Console 监控功能

### 原理与架构
- [ ] 16. NameServer 路由发现与心跳机制
- [ ] 17. 存储架构：CommitLog + ConsumeQueue + IndexFile
- [ ] 18. 顺序写 CommitLog 的优势（无磁盘寻道）
- [ ] 19. 刷盘策略：同步 vs 异步
- [ ] 20. 主从复制：同步 vs 异步
- [ ] 21. Producer 发送流程（4 步）
- [ ] 22. Consumer Rebalance 机制与策略
- [ ] 23. 消息重试 16 级递增策略
- [ ] 24. 死信队列（DLQ）机制
- [ ] 25. 长轮询 Long Polling 实现
- [ ] 26. 事务消息的两阶段提交与回查
- [ ] 27. 顺序消息的 Queue 选择器与锁机制
- [ ] 28. 延迟消息的 SCHEDULE_TOPIC_XXXX
- [ ] 29. Dledger Raft 自动选主原理
- [ ] 30. Controller 模式高可用

### 实战场景
- [ ] 31. 订单系统分布式事务设计
- [ ] 32. 支付回调异步通知处理
- [ ] 33. 库存回滚幂等性处理
- [ ] 34. 优惠券回退设计
- [ ] 35. 订单超时取消（延迟消息）
- [ ] 36. 线程池优化消息发送
- [ ] 37. 失败补偿机制设计（三级）
- [ ] 38. Spring Boot 集成配置
- [ ] 39. 多模块消息路由（Tag 设计）
- [ ] 40. 死信队列排查与恢复

### 手写代码
- [ ] 41. Spring Boot Producer + Consumer 完整配置
- [ ] 42. TransactionMQProducer + TransactionListener
- [ ] 43. 顺序消息 Producer + Consumer（MessageQueueSelector）
- [ ] 44. 延迟消息发送（setDelayTimeLevel）
- [ ] 45. Tag 过滤 + SQL92 过滤
- [ ] 46. 批量消息 + 异步发送 Callback

### 系统设计
- [ ] 47. 订单支付分布式事务架构
- [ ] 48. 千万级日志收集系统设计
- [ ] 49. RabbitMQ → RocketMQ 平滑迁移
- [ ] 50. 多机房跨地域消息复制

### 对比题
- [ ] 51. RocketMQ vs Kafka vs RabbitMQ
- [ ] 52. RocketMQ vs Kafka 的事务消息对比
- [ ] 53. RocketMQ vs Kafka 的延迟消息对比
- [ ] 54. 同步刷盘 vs 异步刷盘
- [ ] 55. 同步复制 vs 异步复制

### 大厂真题回忆
- [ ] 56. 【阿里】RocketMQ 事务消息如何排查事务状态未知的情况？
- [ ] 57. 【阿里】RocketMQ 如何避免消息重复消费？
- [ ] 58. 【腾讯】RocketMQ 消息积压如何处理？
- [ ] 59. 【美团】RocketMQ 顺序消息和 Kafka 顺序消息的区别？
- [ ] 60. 【字节跳动】RocketMQ 如何进行扩容？

---

> 🎯 **面试策略建议**：
> 1. **基础题**（1-15）：掌握关键概念，能用 1-2 句话解释每个概念
> 2. **原理题**（16-30）：结合源码路径回答，展示深度
> 3. **实战题**（31-40）：强调场景 + 方案 + 坑点，展示经验
> 4. **代码题**（41-46）：手写关键代码段，注意异常处理和边界条件
> 5. **设计题**（47-50）：画出架构图（思维导图），分点说明设计决策

---

*文档版本：v1.0 | 最后更新：2025-07-22 | 基于 29-RocketMQ教程.md 课程大纲整理*
