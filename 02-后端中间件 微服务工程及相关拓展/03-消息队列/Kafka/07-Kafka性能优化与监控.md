# 07 - Kafka 性能优化与监控

> 🎯 Kafka 的性能天花板极高 — 单机百万 TPS 需要从 OS 参数、JVM 调优、Broker 配置三个层面协同优化，缺一不可

---

## 目录

1. [性能优化全景图](#1-性能优化全景图)
2. [OS 层面优化](#2-os-层面优化)
3. [Broker 配置优化](#3-broker-配置优化)
4. [Producer 端优化](#4-producer-端优化)
5. [Consumer 端优化](#5-consumer-端优化)
6. [JMX 监控体系](#6-jmx-监控体系)
7. [压测方法](#7-压测方法)

---

## 1. 性能优化全景图

```text
性能优化四层模型：

┌─────────────────────────────────────────────────────┐
│  应用层：分区数、批量策略、压缩、幂等/事务关/开         │
├─────────────────────────────────────────────────────┤
│  JVM 层：GC 策略、堆大小、直接内存                    │
├─────────────────────────────────────────────────────┤
│  OS 层：页缓存、文件描述符、网络缓冲、Swap             │
├─────────────────────────────────────────────────────┤
│  硬件层：SSD/HDD、网卡、CPU、内存                      │
└─────────────────────────────────────────────────────┘

常见瓶颈定位：
  磁盘 IO 满 → 检查 log.dirs 磁盘 utilization
  网络满 → 检查网卡带宽
  GC 频繁 → 检查 JVM GC 日志，增大堆
  CPU 高 → 压缩/解压、SSL、日志清理
```

---

## 2. OS 层面优化

### 2.1 页缓存（Page Cache）

```text
Kafka 严重依赖 OS 页缓存：

写入路径：
  Producer → Broker 进程 → OS 页缓存 → 异步刷盘（默认 5s）

读取路径：
  消费者拉取 → 页缓存命中 → 零拷贝直接发送（热数据）
  消费者拉取 → 页缓存未命中 → 磁盘读取（冷数据）
             ↑
         经验：Kafka 堆外内存 = 页缓存，比 JVM 堆更重要！

建议：
  ✅ Broker 内存配置：JVM 堆 6~8GB + 剩余给 OS 页缓存
  ✅ 热数据比例 > OS 页缓存大小 → 命中率下降 → 磁盘读增加
  ✅ 监控 page cache hit ratio
```

### 2.2 文件系统选择

| 文件系统 | 推荐度 | 说明 |
|----------|:---:|------|
| **XFS** | ⭐⭐⭐⭐⭐ | 大文件顺序写最优，Kafka 官方推荐 |
| ext4 | ⭐⭐⭐ | 可用，但非最优 |
| ZFS/Btrfs | ⭐⭐ | 不推荐，写放大 |

```bash
# 挂载 XFS 建议参数
mount -o noatime,nodiratime /dev/sdb /data/kafka

# noatime：不更新文件访问时间，减少写操作
```

### 2.3 OS 参数优化

```bash
# /etc/sysctl.conf 推荐配置

# ====== 虚拟内存 ======
vm.swappiness=1                       # 尽量不使用 swap
vm.dirty_background_ratio=5           # 后台刷脏页阈值（%）
vm.dirty_ratio=60                     # 同步刷脏页阈值（%）

# ====== 网络 ======
net.core.rmem_default=1048576         # Socket 接收缓冲默认 1MB
net.core.wmem_default=1048576         # Socket 发送缓冲默认 1MB
net.core.rmem_max=16777216            # Socket 接收缓冲最大 16MB
net.core.wmem_max=16777216            # Socket 发送缓冲最大 16MB
net.ipv4.tcp_wmem=4096 65536 16777216
net.ipv4.tcp_rmem=4096 65536 16777216

# ====== 连接 ======
net.core.somaxconn=4096               # 最大连接队列
net.ipv4.tcp_max_syn_backlog=4096     # SYN 队列长度

# 生效
sysctl -p
```

### 2.4 文件描述符

```bash
# /etc/security/limits.conf
kafka  soft  nofile  100000
kafka  hard  nofile  200000

# Kafka 每 Partition ≈ 2 个文件描述符（.log + .index）
# 1000 Partitions × 3 Replicas ≈ 6000 FDs
# 再加上网络连接，建议 > 10万
```

---

## 3. Broker 配置优化

### 3.1 线程模型

```properties
# 网络线程：处理连接、接收请求
num.network.threads=8       # CPU 核心数 / 2

# I/O 线程：处理磁盘读写
num.io.threads=16           # CPU 核心数 × 2

# 后台线程
background.threads=10       # 日志清理等后台操作
```

### 3.2 网络优化

```properties
socket.send.buffer.bytes=1048576    # 1MB 发送缓冲
socket.receive.buffer.bytes=1048576 # 1MB 接收缓冲
socket.request.max.bytes=104857600  # 100MB 单次请求最大

# 副本同步
num.replica.fetchers=4              # 副本拉取线程数
replica.fetch.wait.max.ms=500       # Follower 拉取等待
replica.fetch.max.bytes=10485760    # 单次拉取最大 10MB
```

### 3.3 日志与存储

```properties
# 多磁盘配置（分散 I/O）
log.dirs=/data/kafka/disk1,/data/kafka/disk2,/data/kafka/disk3

# 日志刷新（通常用默认，让 OS 管理）
log.flush.interval.messages=10000   # 多少条消息刷一次盘
log.flush.interval.ms=1000          # 多少毫秒刷一次盘
# ⚠️ 不建议改太小，影响吞吐；可靠性靠副本保证

# 日志段
log.segment.bytes=1073741824        # 1GB 滚动
log.index.interval.bytes=4096       # 索引间隔（越小索引越密，查找越快）
```

---

## 4. Producer 端优化

### 4.1 批量与压缩

```properties
# 批量 — 提升吞吐的核心
batch.size=65536                    # 64KB（增大 = 更少请求 = 更高吞吐）
linger.ms=5                         # 5ms（低延迟场景 0，高吞吐 5~20）

# 压缩 — 减少网络传输
compression.type=lz4                # 推荐 lz4（速度/压缩率均衡）
# none < gzip(高压缩低速度) < snappy(快) < lz4(快+好) < zstd(最优)

# 缓冲区
buffer.memory=134217728             # 128MB
```

### 4.2 压缩算法对比

| 算法 | 压缩率 | 速度 | CPU 开销 | 推荐场景 |
|------|:---:|:---:|:---:|------|
| **none** | 0% | 最快 | 无 | 内部网络/低延迟 |
| **gzip** | 最高 | 最慢 | 最高 | 带宽紧缺/网络成本高 |
| **snappy** | 中等 | 快 | 低 | 通用（老项目） |
| **lz4** | 中等 | 很快 | 低 | **推荐（通用最优）** |
| **zstd** | 高 | 快 | 中 | 带宽紧缺 + 想快 |

### 4.3 连接复用

```properties
# 减少连接创建/销毁开销
connections.max.idle.ms=540000      # 9 分钟空闲关闭（默认）

# ⚠️ Kafka Producer 是线程安全的，应用层应复用同一个实例！
# ❌ 每条消息 new KafkaProducer
# ✅ 单例 KafkaProducer
```

---

## 5. Consumer 端优化

### 5.1 拉取参数

```properties
# 拉取量
fetch.min.bytes=1048576             # 最少拉取 1MB（减少空请求）
fetch.max.wait.ms=500               # 类比 linger.ms，等够数据
max.partition.fetch.bytes=10485760  # 单分区最大拉取 10MB
fetch.max.bytes=52428800            # 单次请求最大拉取 50MB

# 批量处理
max.poll.records=500                # poll() 返回最大条数（默认500）
```

### 5.2 并行消费

```java
// 方案 1：多 Consumer 实例（天然并行，推荐）
// 每个 Consumer 在自己线程中 poll

// 方案 2：单 Consumer + 多处理线程
ExecutorService executor = Executors.newFixedThreadPool(10);
while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
    for (ConsumerRecord<String, String> record : records) {
        executor.submit(() -> process(record));
    }
    consumer.commitAsync(); // ⚠️ 注意位移提交时机
}

// ⚠️ 方案 2 的陷阱：
//   - 位移提交可能在实际处理前 → 丢消息
//   - 提交可能在处理后 → 重复消息
//   → 需要精细的 Offset 管理
```

### 5.3 消费速率控制

```java
// 暂停/恢复分区 — 优雅的背压机制
Set<TopicPartition> partitions = consumer.assignment();
consumer.pause(partitions);   // 暂停拉取（还在组内，不触发 Rebalance）
// ... 处理积压 ...
consumer.resume(partitions);  // 恢复拉取
```

---

## 6. JMX 监控体系

### 6.1 启用 JMX

```bash
# 启动时开启 JMX
export JMX_PORT=9999
bin/kafka-server-start.sh config/server.properties

# 或通过环境变量
export KAFKA_JMX_OPTS="-Dcom.sun.management.jmxremote
  -Dcom.sun.management.jmxremote.port=9999
  -Dcom.sun.management.jmxremote.authenticate=false
  -Dcom.sun.management.jmxremote.ssl=false"
```

### 6.2 核心监控指标

#### Broker 级

| 指标 | MBean | 说明 | 告警 |
|------|------|------|:---:|
| **BytesInPerSec** | `kafka.server:type=BrokerTopicMetrics,name=BytesInPerSec` | 入站速率 | >80%带宽 |
| **BytesOutPerSec** | `kafka.server:type=BrokerTopicMetrics,name=BytesOutPerSec` | 出站速率 | >80%带宽 |
| **MessagesInPerSec** | `kafka.server:type=BrokerTopicMetrics,name=MessagesInPerSec` | 消息速率 | 突降 |
| **TotalProduceRequests** | `kafka.server:type=BrokerTopicMetrics,name=TotalProduceRequests` | 生产请求 | |
| **TotalFetchRequests** | `kafka.server:type=BrokerTopicMetrics,name=TotalFetchRequests` | 消费请求 | |
| **RequestQueueSize** | `kafka.network:type=RequestChannel,name=RequestQueueSize` | 请求队列 | >500 |
| **ActiveControllerCount** | `kafka.controller:type=KafkaController,name=ActiveControllerCount` | Controller 状态 | =0 ❌ |

#### 分区/副本级

| 指标 | 说明 | 告警 |
|------|------|:---:|
| **UnderReplicatedPartitions** | 同步滞后的分区数 | >0 |
| **OfflinePartitionsCount** | 无 Leader 分区数 | >0 |
| **UnderMinIsrPartitionCount** | ISR < min.insync.replicas | >0 |
| **IsrShrinksPerSec** | ISR 缩小速率 | >0 |
| **IsrExpandsPerSec** | ISR 扩大速率 | 频繁震荡 |

#### 消费组级

| 指标 | 说明 | 告警 |
|------|------|:---:|
| **RecordsLagMax** | 最大积压数 | >阈值 |
| **RecordsConsumedRate** | 消费速率 | 低于预期 |

### 6.3 监控工具选型

| 工具 | 特点 | 适用 |
|------|------|------|
| **Kafka Manager (CMAK)** | 轻量，Topic/消费组管理 | 中小集群 |
| **Kafka UI** | 现代 UI，多集群 | 开发/测试 |
| **Prometheus + Grafana** | 指标采集 + 可视化 | **生产推荐** |
| **Burrow** | 专攻消费组 Lag 监控 | 补充监控 |
| **Datadog / 云监控** | SaaS 方案 | 企业级 |

---

## 7. 压测方法

### 7.1 自带压测工具

```bash
# ===== Producer 压测 =====
bin/kafka-producer-perf-test.sh \
  --topic test-topic \
  --num-records 10000000 \
  --record-size 1024 \
  --throughput -1 \
  --producer-props bootstrap.servers=localhost:9092 \
    acks=1 compression.type=lz4 linger.ms=5 batch.size=65536

# 输出示例：
# 10000000 records sent, 205761.316733 records/sec (200.94 MB/sec)
# → 单分区 ~200 MB/s 吞吐！

# ===== Consumer 压测 =====
bin/kafka-consumer-perf-test.sh \
  --bootstrap-server localhost:9092 \
  --topic test-topic \
  --messages 10000000 \
  --group test-group
```

### 7.2 压测建议

```text
压测矩阵：
  ├── 不同 acks (0/1/all)
  ├── 不同压缩 (none/snappy/lz4/zstd)
  ├── 不同 batch.size (16/32/64/128 KB)
  ├── 不同 partition 数 (1/3/8/16)
  └── 不同 replica 数 (1/3)

评估标准：
  ✅ TPS（吞吐）— 目标吞吐 × 1.5 余量
  ✅ P99 延迟 — <50ms（低延迟）/ <500ms（通用）
  ✅ 资源使用 — CPU <70%、磁盘 <80%
```

---

## 性能调优速查表

| 问题 | 可能原因 | 检查命令 | 解决方案 |
|------|----------|----------|----------|
| **吞吐低** | batch 太小 | 查看 avg batch size | 增大 batch.size / linger.ms |
| **延迟高** | linger.ms 过大 | 查看 producer-metrics | 减小 linger.ms |
| **磁盘 IO 高** | 页缓存不足 | `iostat -x 1` | 加大内存 / 多磁盘 |
| **网络瓶颈** | 压缩未开启 | 查看网卡流量 | 开启 lz4/zstd 压缩 |
| **频繁 GC** | 堆太小或内存泄漏 | GC 日志 | 增大 Xmx，检查内存 |
| **消费慢** | 消费逻辑重 | 应用 trace | 优化逻辑 / 增加消费者 |

---

## 一句话总结

> Kafka 性能优化的核心是**让数据尽量在内存和网络中"流动"而不落地** — 页缓存避免磁盘读、零拷贝避免 CPU 拷贝、批量减少网络往返、压缩减少数据量。
