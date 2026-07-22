# Java分布式框架教程 面试宝典
> 分布式事务、Netty、Redis、分布式ID、Dubbo核心高频考点全覆盖，适用于阿里/腾讯/字节/美团一二面

## 目录
1. [一、基础概念速答](#一基础概念速答)
2. [二、深度原理剖析](#二深度原理剖析)
3. [三、实战场景题](#三实战场景题)
4. [四、手写代码题](#四手写代码题)
5. [五、系统设计题](#五系统设计题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板](#七面试回答模板)
8. [八、快速查漏补缺Checklist](#八快速查漏补缺checklist)

---

## 一、基础概念速答

> 本部分覆盖分布式框架最常被问到的核心概念题，每题回答控制在2-4句，突出关键词对比。

### 1. 什么是ACID？什么是BASE？
**ACID** 是数据库事务的四大特性：Atomicity（原子性）、Consistency（一致性）、Isolation（隔离性）、Durability（持久性），适用于单机数据库。**BASE** 是分布式系统的设计哲学：Basically Available（基本可用）、Soft State（软状态）、Eventually Consistent（最终一致性），强调牺牲强一致性换取可用性和分区容错性。分布式系统通常选择 BASE 而非 ACID。

> 💡 面试时先答定义，然后补一句："分布式场景下追求ACID会导致性能急剧下降，因此大部分分布式事务方案走BASE路线。"

### 2. CAP 定理是什么？为什么分布式系统中只能选CP或AP？
CAP 定理指出一个分布式系统最多只能同时满足 Consistency（强一致性）、Availability（可用性）、Partition Tolerance（分区容错性）中的两个。由于网络分区（P）是必然发生的，因此实际只能在 C 和 A 之间二选一。ZooKeeper 选 CP（牺牲可用性保证一致性），Eureka 选 AP（牺牲一致性保证可用性）。

### 3. 2PC 和 3PC 的区别？
2PC（两阶段提交）分为 Prepare 和 Commit 两个阶段，存在 **同步阻塞** 和 **Coordinator单点故障** 问题。3PC（三阶段提交）引入 CanCommit、PreCommit、DoCommit 三个阶段，并增加了超时中断机制——参与者在等待超时后会自动中断事务。3PC 缓解了 2PC 的阻塞问题，但依然无法完全解决数据一致性问题。

> ⚠️ 实际生产中用 2PC 的很少，因为性能太差。Seata AT 模式在 2PC 基础上做了优化，才是工程级的方案。

### 4. TCC 和 2PC 的本质区别？
TCC（Try-Confirm-Cancel）是 **业务层面** 的补偿型事务，而 2PC 是 **资源层面** 的锁协议。TCC 的三个阶段都由业务代码实现：Try 做资源检查和预留，Confirm 做实际提交，Cancel 做回滚补偿。TCC 不持有数据库锁，性能远高于 2PC，但需要业务方实现空回滚和幂等控制。

### 5. Saga 模式和 TCC 的区别？
Saga 是 **长事务** 解决方案，将一个大事务拆分成多个本地子事务，每个子事务都有对应的补偿操作。与 TCC 不同，Saga 没有 Try 预留阶段，直接执行正向操作，失败时反向补偿。Saga 分为 **编排模式（Choreography）** 和 **协调模式（Orchestrator）** 两种实现。适合业务流程长、涉及服务多的场景。

### 6. BIO / NIO / AIO 的区别？
| 模型 | 全称 | I/O 模型 | 适用场景 |
|------|------|----------|----------|
| BIO | Blocking I/O | 同步阻塞 | 连接数少且固定，如 JDBC |
| NIO | Non-blocking I/O | 同步非阻塞 + 多路复用 | 高并发、短连接，如 Netty |
| AIO | Asynchronous I/O | 异步非阻塞 | 文件I/O、长连接（实际用得少） |

BIO 一个线程处理一个连接，NIO 通过 Selector 一个线程处理多个连接，AIO 是操作系统完成回调通知。

### 7. select / poll / epoll 有什么区别？
| 特性 | select | poll | epoll |
|------|--------|------|-------|
| 最大连接数 | 1024（FD_SETSIZE） | 无上限（链表） | 无上限（红黑树+链表） |
| 遍历方式 | 线性遍历所有fd | 线性遍历所有fd | 回调触发，只遍历就绪fd |
| 数据拷贝 | 每次调用都从用户态拷贝到内核态 | 同 select | mmap 共享内存，减少拷贝 |
| 工作模式 | LT | LT | LT + ET（边缘触发） |

**epoll 是 Linux 下性能最高的多路复用器**，Redis、Netty、Nginx 底层都依赖 epoll。

### 8. Redis 分布式锁的原理是什么？
使用 `SET key value NX PX 30000` 命令在 Redis 集群中抢占锁。NX 保证互斥，PX 设置自动过期防止死锁。但单机 Redis 锁在哨兵模式下存在主从切换导致锁丢失的问题，因此 Redis 官方提出了 **Redlock** 算法，要求在大多数（N/2+1）节点同时加锁成功才算成功。

### 9. 什么是 Redisson？看门狗机制是什么？
Redisson 是 Redis 官方推荐的 Java 客户端，封装了分布式锁、计数器、队列等高级功能。**看门狗（WatchDog）** 是 Redisson 分布式锁的自动续期机制：默认锁超时30秒，每10秒检查一次，如果业务还未完成则自动续期30秒，防止锁在业务执行期间过期释放。底层通过 Lua 脚本保证续期的原子性。

### 10. 分布式 ID 有哪些生成方式？
| 方式 | 优点 | 缺点 |
|------|------|------|
| UUID | 本地生成，性能极高 | 无序、长度长、索引性能差 |
| DB自增主键 | 有序、简单 | 单点瓶颈、扩展性差 |
| Redis INCR | 高性能 | 需要依赖Redis可用性 |
| Snowflake | 全局唯一、趋势递增 | 依赖机器时钟 |
| Leaf（美团） | 高可用、低延迟 | 需要部署服务 |
| Tinyid（滴滴） | 类似Leaf | 同Leaf |

### 11. Snowflake 算法的结构是怎样的？
Snowflake 生成的 ID 是64位 Long 型，由4部分组成：**1bit符号位（固定0）** + **41bit时间戳（毫秒级，可用69年）** + **10bit工作机器ID（最多1024台）** + **12bit序列号（每毫秒4096个）**。核心思想是将时间、机器、序号组合成一个64位数字，保证全局唯一且趋势递增。

### 12. RPC 和 HTTP 的区别？
| 对比 | RPC | HTTP |
|------|-----|------|
| 协议 | 自定义协议（Dubbo、gRPC） | HTTP/1.1、HTTP/2 |
| 序列化 | Hessian、Protobuf、Kryo | JSON、XML |
| 性能 | 高（二进制传输、连接复用） | 较低（文本协议、头部开销大） |
| 服务治理 | 内置服务发现、负载均衡 | 需要额外组件（Nginx、网关） |
| 跨语言 | 有限（需双方支持协议） | 天然跨语言 |

Dubbo RPC 默认基于 Netty 实现长连接复用，性能比 HTTP 调用高 5-10 倍。

### 13. Dubbo 的核心架构组件有哪些？
Dubbo 的五大核心组件：**Provider**（服务提供者）、**Consumer**（服务消费者）、**Registry**（注册中心，如 ZK/Nacos）、**Monitor**（监控中心）、**Container**（容器）。调用流程：Provider 启动注册到 Registry → Consumer 从 Registry 订阅服务地址列表 → Consumer 基于负载均衡策略调用 Provider → Monitor 记录调用统计。

### 14. Dubbo 的负载均衡策略有哪些？
| 策略 | 描述 |
|------|------|
| Random（随机） | 按权重随机，默认策略 |
| RoundRobin（轮询） | 按权重轮询，平滑加权 |
| LeastActive（最少活跃数） | 选当前处理请求最少的节点 |
| ConsistentHash（一致性哈希） | 相同参数落同一节点，支持缓存 |
| ShortestResponse（最短响应） | 选响应时间最短的节点（3.0新增） |

### 15. Seata 支持哪几种事务模式？
| 模式 | 特点 | 适用场景 |
|------|------|----------|
| AT | 自动补偿，对业务代码无侵入 | 简单的跨库事务 |
| TCC | 业务手动实现 Try/Confirm/Cancel | 复杂业务逻辑 |
| Saga | 长事务+补偿，支持编排/协调 | 业务流程长、服务多 |
| XA | 基于数据库XA协议 | 需要强一致性的场景 |

### 16. 什么是 Netty 的 Reactor 线程模型？
Netty 基于主从 Reactor 多线程模型：**Boss Group**（主 Reactor）负责 Accept 客户端连接，并将 Channel 注册到 **Worker Group**（从 Reactor），Worker Group 处理 Channel 上的读写事件。默认情况下 Boss Group 线程数为1，Worker Group 线程数为 CPU 核心数 * 2。这种模型能支撑百万级连接。

### 17. Redis 集群的三种架构及对比？
| 架构 | 数据分片 | 高可用 | 通信机制 |
|------|----------|--------|----------|
| 主从复制 | 无分片 | 手动切换 | 异步复制 |
| 哨兵模式 | 无分片 | 自动故障转移 | 哨兵节点监控 |
| Cluster | 16384槽位分片 | 自动故障转移 | Gossip协议 |

### 18. 什么是可靠消息最终一致性方案？
核心流程：**生产者发消息前先写本地消息表（同库事务）** → MQ 异步投递消息 → 消费者消费后回调确认 → 定时任务轮询未确认的消息进行重试。本质是 **本地事务 + 消息队列 + 定时重试 + 幂等消费** 的组合方案，保证消息至少被消费一次。

---

## 二、深度原理剖析

### 1. 2PC 协议的阻塞问题及优化思路
2PC 中 Coordinator 向所有 Participant 发送 Prepare 请求后，Participant 会加锁资源并等待 Coordinator 的 Commit/Abort 指令。如果 Coordinator 宕机，Participant 会一直持有锁无法释放，导致整个系统阻塞。优化方案：引入超时机制（如 3PC）、使用 Seata AT 的全局锁替换数据库锁、或者改用 TCC 业务锁替代资源锁。

### 2. TCC 的 Try-Confirm-Cancel 补偿设计详解
以账户转账为例：Try 阶段冻结转账金额（`freeze_amount = freeze_amount + 100`），Confirm 阶段扣除冻结并增加对方余额（`freeze_amount = freeze_amount - 100; balance = balance - 100`），Cancel 阶段解冻（`freeze_amount = freeze_amount - 100`）。**空回滚问题**：Try 没执行却执行了 Cancel，需要事务日志判断。**幂等问题**：Confirm/Cancel 可能重复调用，通过唯一事务 ID + 状态机去重。

```java
public interface TccService {
    @TwoPhaseBusinessAction(name = "transfer", commitMethod = "confirm", rollbackMethod = "cancel")
    boolean try(BusinessActionContext context, @BusinessActionContextParameter("amount") int amount);

    boolean confirm(BusinessActionContext context);

    boolean cancel(BusinessActionContext context);
}
```

### 3. Seata AT 模式的全局事务与分支事务
Seata AT 是对 2PC 的工程优化。**TM**（事务管理器）开启全局事务，**RM**（资源管理器）注册分支事务。AT 模式的核心是一阶段执行 SQL 并生成 **undo log**（记录数据快照），二阶段根据全局状态决定提交或回滚。回滚时通过 undo log 逆向补偿。Seata Server（TC）负责维护全局事务状态，通过全局锁保证写隔离。

```yaml
seata:
  enabled: true
  application-id: ${spring.application.name}
  tx-service-group: my_test_tx_group
  service:
    vgroup-mapping:
      my_test_tx_group: default
    grouplist:
      default: 127.0.0.1:8091
  config:
    type: nacos
  registry:
    type: nacos
```

```java
@GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
public void createOrder(Order order) {
    orderDao.insert(order);           // RM1: 订单库
    accountDao.debit(order.getUserId(), order.getAmount()); // RM2: 账户库
    inventoryDao.deduct(order.getProductId(), order.getCount()); // RM3: 库存库
}
```

> 💡 面试重点：AT 模式对业务代码 **零侵入**，但需要所有参与分支的数据源支持 undo log。

### 4. Netty 主从 Reactor 线程模型源码级分析
Netty 的 `EventLoopGroup` 实现了主从 Reactor。`NioEventLoop` 内部封装了一个 Selector 和任务队列。主 Reactor（Boss）注册 `OP_ACCEPT` 事件，接收到新连接后将 `NioSocketChannel` 注册到从 Reactor（Worker）的 Selector 上。Worker 注册 `OP_READ` 事件并处理读写。Pipeline 上链式执行 ChannelHandler，每个 Handler 可指定是否在 IO 线程执行。

```java
EventLoopGroup bossGroup = new NioEventLoopGroup(1);
EventLoopGroup workerGroup = new NioEventLoopGroup();
ServerBootstrap b = new ServerBootstrap();
b.group(bossGroup, workerGroup)
 .channel(NioServerSocketChannel.class)
 .childHandler(new ChannelInitializer<SocketChannel>() {
     @Override
     protected void initChannel(SocketChannel ch) {
         ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
         ch.pipeline().addLast(new EchoServerHandler());
     }
 });
ChannelFuture f = b.bind(8080).sync();
f.channel().closeFuture().sync();
```

### 5. Redisson 分布式锁看门狗续期机制
Redisson 加锁时默认 `lockWatchdogTimeout = 30000ms`，底层 Lua 脚本 `SET key value NX PX 30000`。加锁成功后启动一个后台定时任务（`TimeoutTask`），每隔 `internalLockLeaseTime / 3`（即10秒）检查锁是否还存在。如果锁还在且持有者是自己，则执行 `PEXPIRE` 续期。如果业务执行完成，解锁后会取消这个定时任务。这样就避免了锁在执行期间过期导致的并发问题。

```lua
-- 加锁 Lua 脚本
if (redis.call('exists', KEYS[1]) == 0) then
    redis.call('hset', KEYS[1], ARGV[2], 1);
    redis.call('pexpire', KEYS[1], ARGV[1]);
    return nil;
end;
if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then
    redis.call('hincrby', KEYS[1], ARGV[2], 1);
    redis.call('pexpire', KEYS[1], ARGV[1]);
    return nil;
end;
return redis.call('pttl', KEYS[1]);
```

### 6. Snowflake 时钟回拨问题及解决方案
时钟回拨是指服务器 NTP 同步后系统时间倒退，导致 Snowflake 可能生成重复 ID（因为时间戳变小了）。业界常见方案：

| 方案 | 实现方式 | 优缺点 |
|------|----------|--------|
| 等待 | 记录上次最大时间戳，回拨时等待追上 | 简单但阻塞 |
| 预留时间戳 | 生成 ID 时预留未来5s的时间段 | 无阻塞，可容忍小回拨 |
| 双时间戳 | 每个序列号绑定两个时间戳 | 复杂，兼容性差 |
| 停机标记 | 回拨超过阈值时抛出异常 | 需要监控告警 |

美团 Leaf 采用 **预留时间戳 + 等待** 的混合策略：回拨小于5ms直接等待，超过5ms用预留的时间段。

### 7. 美团 Leaf segment 模式 + double buffer 优化
Leaf 的 segment 模式将 DB 自增 ID 分段加载到内存。**号段（segment）** 在内存中提前分配，用完后再去数据库取下一段。**Double Buffer** 优化：当前号段消耗到一定阈值（如10%）时，异步加载下一号段到备用 buffer。这样请求全部在内存中分配，DB 压力极小。Leaf 的每个业务 key 在 DB 中记录 `max_id`（当前最大ID）和 `step`（步长，默认2000）。

```sql
CREATE TABLE leaf_alloc (
    biz_tag VARCHAR(128) PRIMARY KEY,
    max_id BIGINT NOT NULL DEFAULT 1,
    step INT NOT NULL DEFAULT 2000,
    description VARCHAR(256)
);
```

```java
// Leaf segment 核心伪代码
synchronized (this) {
    if (segmentA.used()) {
        if (segmentB.isReady()) {
            swap(segmentA, segmentB);
            asyncLoadNextSegment(); // double buffer 异步加载
        }
    }
    return segmentA.nextId();
}
```

### 8. select / poll / epoll 内核实现区别
**select** 在内核中对 fd_set 进行位图操作，每次调用都要将 fd_set 从用户态拷贝到内核态，线性扫描所有 fd，最多1024个。**poll** 改用 pollfd 链表，消除了1024上限但仍需全量拷贝和线性扫描。**epoll** 没有上限限制，注册时以红黑树存储 fd，就绪队列用链表存储。epoll 通过回调机制（设备驱动就绪时调用 ep_poll_callback）将就绪 fd 加入就绪列表，**只返回就绪的 fd**，时间复杂度 O(1)。epoll 还支持 **ET（边缘触发）** 模式，只通知一次直到数据读完，减少系统调用。

### 9. Dubbo SPI 扩展机制
Dubbo 的 SPI（Service Provider Interface）是 JDK SPI 的增强版。JDK SPI 不灵活（必须一次性加载所有实现），Dubbo SPI 支持 **按需加载**、**AOP**（通过包装类自动代理）、**自适应扩展**（`@Adaptive` 注解）。Dubbo 加载 `/META-INF/dubbo/internal/` 下的配置文件，key-value 形式如 `dubbo=org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol`。`@SPI("dubbo")` 指定默认实现，`URL` 参数动态指定要激活的扩展。

```java
@SPI("dubbo")
public interface Protocol {
    @Adaptive
    <T> Exporter<T> export(Invoker<T> invoker) throws RpcException;
    @Adaptive
    <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException;
}
```

> 🎯 Dubbo 几乎是纯 SPI 架构，从协议、序列化、负载均衡到集群容错，所有组件都可扩展。

### 10. 可靠消息最终一致性方案详解
方案分4个环节：**1）生产者发送消息前先写本地消息表**，与业务操作在一个本地事务中。**2）独立消息服务（或 MQ）轮询未发送的消息**，定时任务扫描本地消息表中 status=0 的记录。**3）消息服务投递消息到 MQ**，消费者消费后调用回调解锁。**4）消费者幂等消费**，通过唯一业务 ID（如 order_id）判断是否已消费过。

```sql
CREATE TABLE local_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    business_id VARCHAR(64) NOT NULL,
    content TEXT NOT NULL,
    status TINYINT DEFAULT 0,  -- 0:待发送 1:已发送 2:已消费
    retry_count INT DEFAULT 0,
    create_time DATETIME,
    next_retry_time DATETIME
);
```

```java
@Transactional
public void createOrderAndSendMsg(Order order) {
    // 1. 业务操作
    orderDao.insert(order);
    // 2. 写本地消息表（同一事务）
    localMessageDao.insert(new LocalMessage(order.getId(), JSON.toJSONString(order)));
}
```

### 11. Saga 模式补偿事务设计
Saga 每步都有一个正向操作和反向补偿操作。以旅行预订为例：订机票（正向）→ 订酒店 → 订车。如果订酒店失败，则调用 CancelFlight 补偿已订的机票。**编排模式** 用事件驱动，每个服务监听上一个服务的事件并执行自己的操作。**协调模式** 由 SagaOrchestrator 统一调度，状态更可控。

```java
// Saga 协调模式伪代码
public class SagaOrchestrator {
    public void execute() {
        try {
            flightService.bookFlight();     // Step1
            hotelService.bookHotel();       // Step2 — 如果失败
        } catch (Exception e) {
            flightService.cancelFlight();   // 补偿 Step1
            throw new SagaException("Booking failed, saga compensated");
        }
    }
}
```

### 12. Redlock 算法原理
Redlock 要求在 N 个独立 Redis 节点（通常 N=5）上同时加锁：**1）获取当前时间戳 T1。2）依次在 N 个节点上加锁**，每个加锁的超时时间远小于锁的过期时间。**3）计算加锁消耗的时间（T2 - T1）**。如果成功加锁数 >= N/2 + 1 且总耗时小于锁的有效时间，则加锁成功。**4）加锁失败则依次解锁所有节点**。Redlock 的核心思想是通过多数派投票避免单点故障，但也有争议（Martin Kleppmann 曾撰文批评 Redlock 不是一个可靠的分布式锁）。

> ⚠️ Redlock 在极端情况下（时钟漂移、GC 暂停）仍可能失效，实际项目中多数公司用 Redis 哨兵 + Redisson 看门狗已经足够。

---

## 三、实战场景题

### 1. Seata 分布式事务完整配置和用法
**需求**：下单场景，跨订单库、账户库、库存库。

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/order_db
seata:
  enabled: true
  application-id: order-service
  tx-service-group: my_test_tx_group
  service:
    vgroup-mapping:
      my_test_tx_group: SEATA_GROUP
    grouplist:
      SEATA_GROUP: 127.0.0.1:8091
```

```java
@Service
public class OrderService {
    @GlobalTransactional(name = "create_order_tx", rollbackFor = Exception.class)
    public void createOrder(OrderDTO dto) {
        orderMapper.insert(dto.toOrder());           // 订单库
        accountMapper.debit(dto.getUserId(), dto.getAmount()); // 账户库
        storageMapper.deduct(dto.getProductId(), dto.getCount()); // 库存库
        if (dto.getAmount() > 1000) {
            throw new RuntimeException("模拟异常，触发全局回滚");
        }
    }
}
```

### 2. Redisson 分布式锁在秒杀场景的应用
```java
@Autowired
private RedissonClient redissonClient;

public String secKill(Long productId, Long userId) {
    String lockKey = "lock:seckill:" + productId;
    RLock lock = redissonClient.getLock(lockKey);
    try {
        // 看门狗自动续期，默认30s
        lock.lock(10, TimeUnit.SECONDS); // 指定过期时间则不看门狗续期
        // 或者 no-arg lock() 使用看门狗: lock.lock();

        int stock = stockService.getStock(productId);
        if (stock <= 0) {
            return "sold out";
        }
        stockService.decrement(productId);
        orderService.createOrder(productId, userId);
        return "success";
    } finally {
        lock.unlock();
    }
}
```

### 3. 美团 Leaf 式分布式 ID 生成服务
```java
@Service
public class LeafIdService {
    private final Map<String, SegmentBuffer> cache = new ConcurrentHashMap<>();

    public long nextId(String bizTag) {
        SegmentBuffer buffer = cache.computeIfAbsent(bizTag, this::initBuffer);
        return buffer.nextId();
    }

    private SegmentBuffer initBuffer(String bizTag) {
        // 从 DB 加载号段: UPDATE leaf_alloc SET max_id = max_id + step WHERE biz_tag = ?
        // 取到旧的 max_id 和 step，内存中生成 [max_id+1, max_id+step]
        SegmentBuffer buf = new SegmentBuffer();
        buf.setCurrentSegment(loadSegment(bizTag));
        // double buffer: 阈值10%时异步加载下一段
        buf.setLoading(false);
        return buf;
    }
}
```

### 4. Netty Server 端完整代码
```java
public class NettyServer {
    public static void main(String[] args) throws InterruptedException {
        EventLoopGroup boss = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, worker)
                     .channel(NioServerSocketChannel.class)
                     .option(ChannelOption.SO_BACKLOG, 128)
                     .childOption(ChannelOption.SO_KEEPALIVE, true)
                     .childHandler(new ChannelInitializer<SocketChannel>() {
                         @Override
                         protected void initChannel(SocketChannel ch) {
                             ch.pipeline()
                               .addLast(new StringDecoder())
                               .addLast(new StringEncoder())
                               .addLast(new ServerBusinessHandler());
                         }
                     });
            ChannelFuture f = bootstrap.bind(8088).sync();
            f.channel().closeFuture().sync();
        } finally {
            worker.shutdownGracefully();
            boss.shutdownGracefully();
        }
    }
}
```

### 5. Snowflake 算法 Java 完整实现
```java
public class SnowflakeIdWorker {
    private final long workerId;
    private final long epoch = 1609459200000L; // 2021-01-01
    private final long workerIdBits = 10L;
    private final long sequenceBits = 12L;
    private final long workerIdShift = sequenceBits;
    private final long timestampShift = sequenceBits + workerIdBits;
    private final long sequenceMask = ~(-1L << sequenceBits);

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdWorker(long workerId) {
        this.workerId = workerId;
    }

    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            // 时钟回拨处理：等待或抛异常
            long offset = lastTimestamp - timestamp;
            if (offset <= 5) {
                Thread.yield(); // 等待
                timestamp = System.currentTimeMillis();
            } else {
                throw new RuntimeException("Clock moved backwards");
            }
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - epoch) << timestampShift) | (workerId << workerIdShift) | sequence;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
```

### 6. 本地消息表 + 定时任务重试实现
```java
@Component
public class MessageRetryTask {
    @Scheduled(fixedDelay = 5000)
    public void retryUnsentMessages() {
        List<LocalMessage> msgs = messageDao.selectByStatus(0, 100); // 待发送
        for (LocalMessage msg : msgs) {
            try {
                // 检查重试次数
                if (msg.getRetryCount() >= 15) {
                    messageDao.updateStatus(msg.getId(), 3); // 标记为死信
                    continue;
                }
                // 发送到 MQ
                rabbitTemplate.convertAndSend("order.exchange", "order.rk", msg.getContent());
                messageDao.updateStatus(msg.getId(), 1); // 已发送
            } catch (Exception e) {
                messageDao.incrementRetry(msg.getId());
                // 更新下次重试时间：指数退避
                messageDao.updateNextRetryTime(msg.getId(),
                    LocalDateTime.now().plusMinutes(1 << msg.getRetryCount()));
            }
        }
    }
}
```

### 7. 分布式锁 + Lua 脚本实现原子操作
```java
public boolean tryLock(String key, String value, long expireMs) {
    String lua = "if redis.call('setnx', KEYS[1], ARGV[1]) == 1 " +
                 "then return redis.call('pexpire', KEYS[1], ARGV[2]) " +
                 "else return 0 end";
    Long result = redisTemplate.execute(
        new DefaultRedisScript<>(lua, Long.class),
        Collections.singletonList(key),
        value,
        String.valueOf(expireMs)
    );
    return result != null && result == 1;
}

public boolean unlock(String key, String value) {
    String lua = "if redis.call('get', KEYS[1]) == ARGV[1] " +
                 "then return redis.call('del', KEYS[1]) " +
                 "else return 0 end";
    Long result = redisTemplate.execute(
        new DefaultRedisScript<>(lua, Long.class),
        Collections.singletonList(key),
        value
    );
    return result != null && result == 1;
}
```

---

## 四、手写代码题

### 1. Snowflake 算法 Java 完整实现
> 参考 [三-5](#5-snowflake-算法-java-完整实现) 节的代码，面试要求手写核心逻辑——位运算、时钟回拨处理、序列号溢出处理。

### 2. Redisson 分布式锁 + 看门狗续期
```java
// 面试手写版: 模拟看门狗续期
public class SimpleWatchDog {
    private final RedisTemplate<String, String> redis;
    private final String lockKey;
    private final String lockValue = UUID.randomUUID().toString();
    private volatile boolean expired = false;

    public boolean lock(long leaseTime, TimeUnit unit) {
        Boolean ok = redis.opsForValue()
            .setIfAbsent(lockKey, lockValue, leaseTime, unit);
        if (Boolean.TRUE.equals(ok)) {
            // 开启看门狗线程
            Thread watchdog = new Thread(() -> {
                while (!expired) {
                    try {
                        Thread.sleep(leaseTime / 3);
                        // 续期: PEXPIRE
                        redis.expire(lockKey, leaseTime, unit);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            });
            watchdog.setDaemon(true);
            watchdog.start();
            return true;
        }
        return false;
    }

    public void unlock() {
        String lua = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                     "return redis.call('del', KEYS[1]) else return 0 end";
        redis.execute(new DefaultRedisScript<>(lua, Long.class),
                      Collections.singletonList(lockKey), lockValue);
        expired = true; // 停止看门狗
    }
}
```

### 3. Netty Server + Handler 代码
```java
public class EchoServerHandler extends SimpleChannelInboundHandler<String> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        System.out.println("Received: " + msg);
        ctx.writeAndFlush("Echo: " + msg + "\n");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
```

### 4. TCC Try-Confirm-Cancel 接口设计
```java
public interface TransferTccService {
    // Try: 冻结金额
    @TwoPhaseBusinessAction(
        name = "transferOut",
        commitMethod = "confirm",
        rollbackMethod = "cancel"
    )
    boolean tryTransfer(
        @BusinessActionContextParameter(paramName = "accountId") Long accountId,
        @BusinessActionContextParameter(paramName = "amount") BigDecimal amount
    );

    // Confirm: 扣减冻结金额
    boolean confirm(BusinessActionContext context);

    // Cancel: 解冻金额
    boolean cancel(BusinessActionContext context);
}

// 实现要点:
// 1. Try: UPDATE account SET freeze = freeze + ? WHERE id = ?
// 2. Confirm: UPDATE account SET balance = balance - ?, freeze = freeze - ? WHERE id = ?
// 3. Cancel: UPDATE account SET freeze = freeze - ? WHERE id = ?
// 4. 所有方法需幂等: INSERT INTO tx_log (tx_id, status) VALUES (?, 1) ON DUPLICATE KEY ...
// 5. 空回滚检查: 如果日志没有 Try 记录，Cancel 直接返回成功
```

### 5. 分布式 ID 生成器接口定义
```java
public interface IdGenerator {
    /** 生成唯一ID */
    long nextId();

    /** 根据业务标识生成唯一ID */
    long nextId(String bizTag);

    /** 批量预取ID */
    List<Long> nextIds(int batchSize);
}

// Snowflake 实现: 见三-5
// Leaf segment 实现: 见三-3
// UUID 实现:
public class UUIDGenerator implements IdGenerator {
    @Override
    public long nextId() {
        return Math.abs(UUID.randomUUID().getMostSignificantBits());
    }
    // 实际工作中不会用 UUID 做 Long ID，这里仅展示接口设计
}
```

### 6. 布隆过滤器防缓存穿透
```java
@Component
public class BloomFilterCache {
    private final BloomFilter<String> bloomFilter = BloomFilter.create(
        Funnels.stringFunnels(Charset.defaultCharset()),
        1000000,  // 预计元素数
        0.01      // 误判率
    );

    @PostConstruct
    public void init() {
        // 预热：从 DB 加载所有 productId 到布隆过滤器
        List<String> allIds = productDao.selectAllIds();
        allIds.forEach(bloomFilter::put);
    }

    public Object getProduct(String id) {
        if (!bloomFilter.mightContain(id)) {
            return null; // 肯定不存在，直接返回
        }
        // 可能存在的走缓存+DB
        Object val = redisTemplate.opsForValue().get("product:" + id);
        if (val == null) {
            // 互斥锁防止缓存击穿
            synchronized (id.intern()) {
                val = productDao.selectById(id);
                redisTemplate.opsForValue().set("product:" + id, val, 1, TimeUnit.HOURS);
            }
        }
        return val;
    }
}
```

---

## 五、系统设计题

### 1. 设计高并发分布式 ID 生成服务（类似 Leaf）
**需求**：QPS 10万+，可用性 99.99%，ID 趋势递增。

**设计方案**：
- **号段模式**：每个业务 tag 在 DB 中占一行，记录当前 max_id 和 step（步长）。服务启动时加载一个号段 `[max_id+1, max_id+step]`，ID 全在内存生成。
- **Double Buffer**：当前号段消耗到20%阈值时，异步线程立即加载下一号段到备用 buffer，切换时 O(1) 无锁。
- **高可用**：DB 做主从 + 连接多个 DB 实例（如 Leaf 支持双 DB 相互切换）。
- **降级**：如果 DB 不可用，切到本地内存临时 ID 生成（如用 IP 做机器标识的 Snowflake）。
- **监控**：每分钟号段消耗速度、DB 压力、切换次数等指标上报。

> 💡 美团 Leaf 落地就是这种架构，支撑了美团全业务的 ID 生成。

### 2. 设计分布式事务解决方案选型
**决策树**：

```text
业务是否要求强一致性？
├── 是 → 数据量小且跨库少 → Seata AT（零侵入）
│   └── 需要数据库 XA 支持 → Seata XA
└── 否 → 业务是否可容忍最终一致性？
    ├── 是 → 是否需要隔离性？
    │   ├── 是 → TCC（业务侵入但性能高）
    │   └── 否 → Saga（长事务、补偿）
    └── 否 → 可靠消息最终一致性（MQ + 本地消息表）
```

**阿里/字节真实场景经验**：
- 金融级（支付、转账）用 **TCC** 或 **Seata AT**，保证写隔离。
- 订单状态同步用 **可靠消息** + **MQ**，保证最终一致。
- 长流程业务（如旅行预订）用 **Saga Orchestrator**。

### 3. 设计 Redis 分布式锁实现高并发抢占
**高并发抢锁场景**（如秒杀库存100个，万人抢购）：

```text
方案一（Redisson 看门狗 + 分段锁）：
  - 将库存分为 N 个段（如10段，每段10件）
  - 每个段一个锁 key：seckill:product:100:segment:0~9
  - 用户请求先 hash(userId) % N 路由到指定段
  - 优点：锁颗粒度细，并发度提升 N 倍

方案二（Redlock 跨机房）：
  - 跨3机房部署5台 Redis 节点
  - Redlock 算法保证多数节点加锁成功
  - 适合跨地域、高可靠的场景

方案三（Lua + 乐观锁）：
  - 直接用 Lua 脚本扣库存，原子操作不加锁
  - script: if stock > 0 then decrstock else return 0 end
  - 性能最高，但退单时需要回补库存
```

### 4. 设计基于 Netty 的简易 RPC 框架
**核心架构**（5层）：

| 层次 | 组件 | 说明 |
|------|------|------|
| 1-协议层 | Protocol | 自定义协议：魔数(4B) + 序列化类型(1B) + 请求ID(8B) + 数据长度(4B) + 数据体 |
| 2-序列化层 | Serializer | 支持 JDK/Hessian/JSON 可扩展 SPI |
| 3-网络层 | NettyTransport | 基于 Netty 的客户端/服务端，连接复用 + ChannelPool |
| 4-服务治理 | Registry | 基于 ZK/Nacos 的注册发现，心跳检测 |
| 5-代理层 | Proxy | 动态代理屏蔽远程调用细节，JDK Proxy / CGLib |

```java
// 自定义协议解码器
public class RpcDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < HEADER_SIZE) return;
        in.markReaderIndex();
        int magic = in.readInt();
        if (magic != 0xCAFEBABE) {
            ctx.close();
            return;
        }
        byte serializerType = in.readByte();
        long requestId = in.readLong();
        int bodyLength = in.readInt();
        if (in.readableBytes() < bodyLength) {
            in.resetReaderIndex();
            return;
        }
        byte[] body = new byte[bodyLength];
        in.readBytes(body);
        // 反序列化 + 组装 RpcRequest
        out.add(new RpcPacket(magic, serializerType, requestId, body));
    }
}
```

---

## 六、常见坑点与最佳实践

| 主题 | 常见坑点 | 最佳实践 |
|------|----------|----------|
| 2PC | Coordinator 单点故障导致参与者永久阻塞 | 用 3PC 超时中断机制或 Seata AT 替代 |
| TCC | 空回滚（Try 没执行就 Cancel）| 事务日志表记录阶段状态，空回滚直接返回成功 |
| TCC | Confirm/Cancel 重复调用（网络重试） | 幂等表 `ON DUPLICATE KEY UPDATE` |
| Seata AT | 全局锁冲突导致高并发下性能陡降 | 控制全局事务粒度，尽量短事务 |
| Snowflake | 时钟回拨生成重复 ID | 小回拨等待，大回拨抛异常+告警 |
| Snowflake | 序列号毫秒内耗尽（高并发） | 扩大序列号位数/使用纯 Snowflake（去掉符号位） |
| Redisson 锁 | 锁超时自动释放导致并发问题 | 开启看门狗自动续期，不设 expire 参数 |
| Redlock | 锁在 GC 暂停期间失效 | 延长锁超时时间 + 设置合理的 GC 策略 |
| Dubbo | 超时设置不合理（默认1s太短） | 根据业务 P99 延迟设置分层超时：连接超时 < 读取超时 < 总超时 |
| Dubbo | 重试导致接口幂等问题 | 写接口设置 `retries=0`，读接口设置 `retries=2` |
| Netty | 直接在 IO 线程处理耗时业务 | 在 Handler 中加 `EventExecutorGroup` 将业务逻辑提交到业务线程池 |
| 本地消息表 | 重复消费不幂等 | 用唯一业务 ID 做去重表 |
| 号段 ID | 重启后号段浪费（没用完的 ID 丢失）| 接受少量 ID 浪费，DB 步长不要设太大 |
| Redis Cluster | 分布式锁在异步复制下的主从切换 | 重要数据用 Redlock 或 ZK 实现 |

---

## 七、面试回答模板

### 模板1：如何选择分布式事务方案？

> **回答思路**：先分类，再对比，最后给出决策逻辑。
>
> "分布式事务选型我一般从 **一致性强度** 和 **性能要求** 两个维度看。如果业务要求强一致性，优先看是否可用 Seata AT 模式，因为它对业务代码无侵入；AT 模式不满足隔离性要求就用 TCC。如果业务可以接受最终一致性，我会根据是否要隔离性来选——需要隔离选 TCC，不需要隔离且流程长选 Saga。另外，如果是简单的消息驱动场景，可靠消息 + MQ 就足够了。总结一下我过去在 XX 项目的经验，我们用的 Seata AT 是性价比最高的方案。"

### 模板2：Snowflake 原理及时钟回拨解决方案

> **回答思路**：先说结构，再说问题，最后给方案。
>
> "Snowflake 生成64位 Long ID，包含1bit符号位、41bit时间戳、10bit工作机器ID、12bit序列号，每毫秒可生成4096个ID，趋势递增。时钟回拨是 Snowflake 最大的隐患，我们项目里用的方案是——小的回拨（5ms以内）通过自旋等待追上上次的时间戳；超过5ms则从备用时间戳池里取，这个池子预留了未来几秒的时间段；如果回拨超过阈值则打印错误日志，人工介入。另外美团 Leaf 的解决思路是直接切断时间依赖，用号段模式。"

### 模板3：Redis 分布式锁实现原理

> **回答思路**：先讲基础实现，再讲 Redisson，最后提 Redlock。
>
> "Redis 分布式锁的核心是 `SET key value NX PX 30000`，NX 保证互斥，PX 防止死锁。但我在生产环境直接用 Redisson 框架，它对分布式锁做了完整封装：RLock 接口支持可重入、支持看门狗自动续期、支持 Redlock 多节点算法。看门狗的机制是加锁时默认30秒，后台每隔10秒检查并续期一次，确保锁不会在业务执行期间过期。如果要过面试，我建议把 Redisson 源码中加锁的 Lua 脚本背下来，面试官一定会追问细节。"

### 模板4：RPC 核心原理

> **回答思路**：全链路串讲，分5个环节。
>
> "RPC 的核心是让远程调用像本地方法一样简单。以 Dubbo 为例，全链路分5步：**1）动态代理**：Consumer 通过 JDK Proxy 生成代理对象；**2）序列化**：把方法名、参数类型、参数值序列化成二进制（Hessian/Kryo）；**3）网络传输**：基于 Netty 的长连接，自定义协议头包含魔数、序列化类型、请求ID、数据长度；**4）服务端反序列化执行**：Provider 反序列化后反射调用实现类；**5）结果返回**：序列化后经 Netty Channel 返回。请求ID 通过 CompletableFuture + Map 做异步回调，客户端拿到结果后唤醒等待线程。"

### 模板5：Netty Reactor 模型

> **回答思路**：从 BIO 演进到 Netty Reactor，对比面试官的期待。
>
> "Netty 采用主从 Reactor 多线程模型。主 Reactor（Boss Group）只负责接收连接（OP_ACCEPT），accept 后把 Channel 注册到从 Reactor（Worker Group）上，Worker Group 负责读写事件（OP_READ/OP_WRITE）。这种模型的好处是 Accept 和 IO 分离，Accept 轻量级 1-2 线程就够了，IO 线程数通常设为 CPU 核数 * 2。Netty 的 NioEventLoop 内部封装了 Selector + 任务队列 + 定时任务，能做到一个线程管理多个 Channel。跟 BIO 一比，BIO 一个线程一个连接，1万线程就 OOM 了；Netty 主从 Reactor 一个 Worker 线程可以管理上千个连接，这就是为什么 Netty 能支撑百万并发连接。"

---

## 八、快速查漏补缺 Checklist

> 🎯 面试前逐项自检，打勾的表示已掌握，空心的需要重点复习。

- [ ] 我能够清晰解释 CAP 定理，并举例说明 ZooKeeper(CP) vs Eureka(AP) 的选择
- [ ] 我能对比 2PC / 3PC / TCC / Saga / Seata AT 的优缺点和适用场景
- [ ] 我能手写 Snowflake 算法核心代码，并说明时钟回拨的至少两种解决方案
- [ ] 我理解 Seata AT 的一阶段 SQL + undo log + 二阶段回滚的完整流程
- [ ] 我能手写 Redis 分布式锁的 Lua 脚本（加锁和解锁）
- [ ] 我理解 Redisson 看门狗续期的时机（每10秒续期30秒）和实现原理
- [ ] 我能解释 Redlock 的多数派加锁逻辑及其局限性
- [ ] 我能对比 BIO / NIO / AIO / 多路复用，画出 select/poll/epoll 对比表
- [ ] 我能画出 Netty 主从 Reactor 模型图，说明 Boss 和 Worker Group 的职责
- [ ] 我能说出 Dubbo 的5大核心组件，并描述一次完整 RPC 调用流程
- [ ] 我能解释 Dubbo SPI 和 JDK SPI 的区别，以及 @Adaptive 的作用
- [ ] 我能设计一个基于号段 + double buffer 的分布式 ID 服务
- [ ] 我能设计一个基于 Netty 的简易 RPC 框架，描述5层架构
- [ ] 我能在项目中正确配置 Seata AT，包括 yaml 配置和 @GlobalTransactional 注解
- [ ] 我能说出分布式锁在 Redis Cluster 下可能存在的问题及 Redlock 的解决思路
- [ ] 我能解释 TCC 的空回滚问题和幂等控制方案
- [ ] 我能设计可靠消息最终一致性方案（本地消息表 + 定时重试）
- [ ] 我了解美团 Leaf 的 segment 模式、Snowflake 模式以及各自的适用场景
- [ ] 我能对比 Dubbo 2.x 和 Dubbo 3.0 的核心变化（应用级注册、Triple 协议）
- [ ] 我能说出至少3种分布式 ID 生成方式及其优劣对比表

---

> **作者注**：本文档覆盖 Java 分布式框架面试最核心的五个专题——分布式事务、Netty、Redis 分布式锁、分布式 ID、Dubbo RPC。建议结合实战项目经验，在实际面试中用 STAR 法则将本文知识点融入具体项目案例。最后推荐阅读 Seata 源码（GlobalTransactionScanner 的 BeanPostProcessor 机制）、Redisson 加锁 Lua 脚本、Dubbo SPI 加载源码，这三个源码级知识点是面试加分利器。
