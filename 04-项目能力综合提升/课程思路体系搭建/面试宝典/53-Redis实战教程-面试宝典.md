# Redis 实战面试宝典（进阶篇）
> 基于课程大纲覆盖Redis企业实战高级面试考点：分布式锁、消息队列、多级缓存、集群架构与底层原理

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

### 1. Redis 的 Java 客户端有哪些？如何选择？
> 常见的 Java 客户端有 Jedis、Lettuce 和 Redisson，各有侧重点。

| 客户端 | 特点 | 适用场景 |
|--------|------|---------|
| Jedis | 直连 Redis，线程不安全，需连接池 | 轻量级、简单应用 |
| Lettuce | 基于 Netty，异步/响应式，线程安全，支持集群、哨兵 | 高性能、Spring Boot 2.x 默认 |
| Redisson | 封装了大量分布式数据结构（锁、队列、CountDownLatch） | 分布式场景、分布式锁 |

> 💡 Spring Boot 2.x 默认使用 Lettuce，但 Lettuce 在某些场景下存在连接泄漏 bug，如遇问题可切换为 Jedis。

### 2. RedisTemplate 的序列化问题？
> Spring Data Redis 使用 RedisTemplate 时需注意序列化配置。

**默认问题**：
- RedisTemplate 默认使用 JdkSerializationRedisSerializer，存储二进制数据，在 Redis 中不可读
- StringRedisTemplate 使用 StringRedisSerializer，可读但只能存字符串

**推荐配置**：
```java
@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        
        // JSON 序列化
        Jackson2JsonRedisSerializer<Object> jsonSerializer = 
            new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.activateDefaultTyping(LazyValidatorFactory.ObjectMapperDefaultTyping.NON_FINAL);
        jsonSerializer.setObjectMapper(om);
        
        // key 使用 String 序列化
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        // value 使用 JSON 序列化
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        return template;
    }
}
```

> 💡 Hash 的 key 和 value 的序列化器需要分别设置，否则 Hash 操作可能出现序列化不一致。

### 3. 什么是管道（Pipeline）技术？
> Pipeline 将多条命令打包一次性发送，减少网络往返时间（RTT）。

```java
// 普通方式：每条命令往返一次网络
for (int i = 0; i < 10000; i++) {
    redisTemplate.opsForValue().set("key:" + i, String.valueOf(i));
}
// 耗时：10000 * RTT

// Pipeline 方式：批量发送，批量接收
List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
    for (int i = 0; i < 10000; i++) {
        connection.stringCommands().set(
            ("key:" + i).getBytes(), 
            String.valueOf(i).getBytes()
        );
    }
    return null;
});
// 耗时：1 * RTT + 网络传输时间
```

- **适用场景**：批量写入、批量获取，对原子性没有要求的场景
- **限制**：Pipeline 不保证原子性（Redis 逐条执行），且返回结果需要全部缓存再解析
- **集群下 Pipeline**：需要计算各 key 所在槽位，按节点分组发送

> 🎯 Pipeline 与原生批量命令（MSET/MGET）的区别：MSET 是原子操作，但单个命令长度有限制；Pipeline 可以处理大量命令，但不保证原子性。

### 4. 什么是 Redisson？它的主要功能？
> Redisson 是 Redis 官方推荐的 Java 客户端，封装了大量分布式数据结构。

**核心功能**：
1. **分布式锁**：可重入锁、公平锁、读写锁、红锁（RedLock）、信号量、闭锁
2. **分布式集合**：Map、Set、List、Queue、Deque（都支持分布式）
3. **分布式服务**：远程服务（RPC）、调度器、延迟队列
4. **同步器**：CountDownLatch、Semaphore、Lock

**Redisson 分布式锁核心原理**：
```java
@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        return Redisson.create(config);
    }
}

// 使用 Redisson 分布式锁
@Autowired
private RedissonClient redissonClient;

public void businessMethod() {
    RLock lock = redissonClient.getLock("myLock");
    try {
        // 尝试加锁，最多等待 100 秒，加锁后 30 秒自动解锁
        if (lock.tryLock(100, 30, TimeUnit.SECONDS)) {
            // 执行业务逻辑
        }
    } finally {
        lock.unlock();
    }
}
```

### 5. Redisson 的 WatchDog 机制？
> WatchDog（看门狗）是 Redisson 自动续期的核心机制。

**解决的问题**：业务执行时间超过锁的过期时间，锁自动释放导致其他线程获取锁。

**工作原理**：
1. 加锁成功后，默认过期时间 30 秒
2. Redisson 启动一个后台定时任务（WatchDog），每隔 10 秒检查一次
3. 如果锁仍被当前线程持有，自动续期到 30 秒
4. 如果当前线程宕机，WatchDog 不再续期，锁自动释放
5. `lock()` 方法默认启用 WatchDog，`tryLock(10, 30, TimeUnit.SECONDS)` 中的 30 秒是 WatchDog 的续期间隔

```java
// 底层原理（简化）：
// 加锁 Lua 脚本
"if (redis.call('exists', KEYS[1]) == 0) then " +
"    redis.call('hset', KEYS[1], ARGV[2], 1); " +
"    redis.call('pexpire', KEYS[1], ARGV[1]); " +
"    return nil; " +
"end; " +
"if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then " +
"    redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
"    redis.call('pexpire', KEYS[1], ARGV[1]); " +
"    return nil; " +
"end; " +
"return redis.call('pttl', KEYS[1]);"
```

### 6. Redis 实现全局唯一 ID 的方法？
> 分布式系统需要全局唯一 ID，Redis 的 INCR 命令可实现简单的 ID 生成。

```java
// Redis 方式：时间戳 + 自增序列
public long generateId(String keyPrefix) {
    // 生成日期字符串作为 hash field
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    String date = LocalDate.now().format(formatter);
    
    // Redis INCR 原子自增
    Long increment = redisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date, 1);
    
    // 组合：时间戳（41位） + 自增序列
    long timestamp = System.currentTimeMillis();
    return (timestamp << 12) | (increment & 0xFFF);
}
```

**Snowflake 算法 vs Redis 自增**：

| 方案 | 优点 | 缺点 |
|------|------|------|
| Snowflake | 无中心化、高性能、趋势递增 | 依赖机器时钟，时钟回拨问题 |
| Redis INCR | 实现简单、严格递增 | Redis 单点问题（可集群缓解） |
| 数据库号段 | 批量获取、无中心化 | 实现略复杂 |

> 💡 美团 Leaf、百度 UidGenerator 都是号段模式或雪花算法的变体。

### 7. 什么是多级缓存？为什么需要多级缓存？
> 多级缓存将不同层次的缓存组合，兼顾响应速度和缓存命中率。

**层级结构**：
```
请求 -> 浏览器缓存 -> Nginx 本地缓存 -> Redis 集群 -> 本地进程缓存(Caffeine) -> 数据库
```

**各层级对比**：

| 层级 | 延迟 | 容量 | 命中率 |
|------|:----:|:----:|:------:|
| 浏览器缓存 | 0ms | 小 | 低 |
| Nginx 本地缓存 | 1ms | 中 | 中 |
| Redis 集群 | 5ms | 大 | 高 |
| Caffeine 本地缓存 | 0.1ms | 小 | 中 |
| 数据库 | 10-50ms | 极大 | - |

> 🎯 多级缓存的核心思想：将热点数据尽量缓存在离用户最近的层级，最热的数据在本地内存中响应，减少远端调用。

### 8. Canal 是什么？如何实现缓存同步？
> Canal 是阿里巴巴开源的 MySQL binlog 增量订阅和消费组件。

**工作原理**：
```
MySQL 主库 -> binlog -> Canal 伪装成从库拉取 binlog -> 解析 -> MQ -> 消费端更新缓存
```

```java
// Canal 客户端示例
@KafkaListener(topics = "canal-topic")
public void handleBinlog(String message) {
    CanalEntry.Entry entry = CanalEntry.Entry.parseFrom(message.getBytes());
    String tableName = entry.getHeader().getTableName();
    
    if ("product".equals(tableName)) {
        CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
        List<CanalEntry.RowData> rowDatasList = rowChange.getRowDatasList();
        
        for (CanalEntry.RowData rowData : rowDatasList) {
            if (rowChange.getEventType() == CanalEntry.EventType.UPDATE) {
                CanalEntry.Column idColumn = rowData.getAfterColumnsList()
                    .stream().filter(c -> "id".equals(c.getName())).findFirst().get();
                // 更新缓存
                redisTemplate.delete("product:" + idColumn.getValue());
            }
        }
    }
}
```

> 💡 Canal 方案实现了"数据库 -> 缓存"的最终一致性，业务代码无需关心缓存更新逻辑。

### 9. Redis 的 Stream 数据结构详解？
> Redis 5.0 引入的 Stream 是 Redis 原生的消息队列实现。

**核心概念**：
- **消息**：键值对组成的消息内容
- **消费者组（Consumer Group）**：一组消费者协同消费，每条消息只被组内一个消费者处理
- **消费者（Consumer）**：组内的消费方
- **游标（cursor）**：每个消费者维护自己的消费进度

**Stream vs 其他消息队列**：

| 特性 | Redis Stream | Kafka | RabbitMQ |
|------|:-----------:|:-----:|:--------:|
| 持久化 | 支持（RDB/AOF） | 支持 | 支持 |
| 消费者组 | 支持 | 支持 | 支持 |
| ACK 机制 | 支持 | 自动 | 支持 |
| 消息回溯 | 支持 | 支持 | 有限 |
| 性能 | 高 | 极高 | 中 |
| 数据量 | 受内存限制 | 磁盘 | 磁盘 |

```bash
# Stream 核心命令
XADD mystream * name John age 30      # 添加消息（* 表示自动生成 ID）
XLEN mystream                          # 获取消息长度
XRANGE mystream - +                    # 获取所有消息
XREAD COUNT 10 BLOCK 0 STREAMS mystream 0  # 阻塞读取新消息
XGROUP CREATE mystream mygroup 0      # 创建消费者组
XREADGROUP GROUP mygroup consumer1 STREAMS mystream >  # 消费者组读取
XACK mystream mygroup 1600000000000-0  # 确认消息
XPENDING mystream mygroup              # 查看待确认消息
```

### 10. Redis 集群模式下如何实现批量操作？
> 集群模式下 key 分布在不同节点，批量操作需要特殊处理。

**处理方式**：
1. **Hash Tag**：使用 `{xxx}` 将相关 key 强制映射到同一槽位
   - 如 `user:{1001}:name` 和 `user:{1001}:age` 的哈希槽相同
2. **按节点分组**：先计算 key 的槽位，按节点分组，分别执行 Pipeline
3. **串行执行**：遍历每个 key 单独操作（最通用但性能最差）

```java
// 按节点分组批量操作
public Map<String, Object> batchGet(List<String> keys) {
    // 使用 Redisson
    RedissonClient redisson = Redisson.create(config);
    // 集群模式下计算槽位，按节点分组
    Map<RedisNode, List<String>> nodeKeys = new HashMap<>();
    for (String key : keys) {
        int slot = redisson.getConnectionManager().calcSlot(key);
        RedisNode node = redisson.getConnectionManager().getEntry(slot).getClient().getAddr();
        nodeKeys.computeIfAbsent(node, k -> new ArrayList<>()).add(key);
    }
    // 对每个节点分别 Pipeline 执行
    Map<String, Object> result = new HashMap<>();
    nodeKeys.forEach((node, nodeKeyList) -> {
        // 每个节点单独执行
    });
    return result;
}
```

---

## 二、深度原理剖析

### 1. Redis 网络模型详解（epoll 与 IO 多路复用）
> Redis 核心命令执行是单线程，但通过 IO 多路复用实现高并发。

**IO 模型演进**：
```
阻塞 IO (BIO) -> 非阻塞 IO (NIO) -> IO 多路复用 (select/poll/epoll)
```

**Redis 使用的 IO 模型**：
- Linux：epoll
- macOS/FreeBSD：kqueue
- 其他：select

**epoll 的三个关键系统调用**：
- `epoll_create()`：创建 epoll 实例
- `epoll_ctl()`：注册要监听的事件（添加/修改/删除 fd）
- `epoll_wait()`：等待事件就绪（阻塞）

**epoll 的两种触发模式**：
| 模式 | 说明 | 适用场景 |
|------|------|---------|
| LT（水平触发） | 只要数据可读，每次 epoll_wait 都返回 | 不易丢失事件，实现简单 |
| ET（边缘触发） | 数据从无到有时才通知，需要一次读完 | 效率高，减少系统调用次数 |

> 💡 Redis 使用 epoll 的 LT 模式。虽然 ET 更高效，但需要应用程序配合非阻塞 IO 和循环读取，复杂度高。

**Redis 6.0 多线程网络模型**：
```text
主线程：
  - accept 新连接
  - 命令执行（核心）
  - 定时任务

IO 线程：
  - 读取 socket 数据
  - 解析命令
  - 写入响应数据
```

### 2. Redis 底层数据结构完整解析
> Redis 五种数据类型有各自的高效底层实现。

**三种顶级结构**：
```
RedisObject（统一对象头）
  |-- type: 数据类型
  |-- encoding: 编码方式
  |-- lru: 上次访问时间（用于 LFU/LRU）
  |-- refcount: 引用计数
  |-- ptr: 指向实际数据结构的指针
```

**数据类型编码矩阵**：

| 数据类型 | 编码方式 | 触发条件 |
|---------|---------|---------|
| String | INT | 纯整数 |
| String | EMBSTR | 长度 <= 44 字节 |
| String | RAW | 长度 > 44 字节 |
| Hash | ZipList | 元素 < 512 且 value 长度 < 64 |
| Hash | HashTable | 超出 ZipList 条件 |
| List | QuickList | 总作为底层实现 |
| Set | IntSet | 元素全为整数且数量 < 512 |
| Set | HashTable | 超出 IntSet 条件 |
| ZSet | ZipList | 元素 < 128 且 score < 64 |
| ZSet | SkipList | 超出 ZipList 条件 |

### 3. Redis 的 Dict 和渐进式 rehash 精讲
> Dict（字典）是 Redis 最重要的基础结构，Hash 和 Set 等类型都依赖它。

**Dict 结构**：
```c
struct dict {
    dictType *type;      // 类型特定函数
    void *privdata;      // 私有数据
    dictht ht[2];        // 两张哈希表（ht[0] 主表，ht[1] 扩容用）
    long rehashidx;      // rehash 进度，-1 表示未进行
    int16_t pauserehash; // rehash 是否暂停
};
```

**渐进式 rehash 详细流程**：
1. 负载因子检查（扩容 >= 1，BGSVAE 时 >= 5；缩容 < 0.1）
2. 为 ht[1] 分配空间（2^n 且 n 为第一个 >= ht[0].used * 2 的幂）
3. rehashidx 设为 0，开始渐进式迁移
4. 每次增删改查操作，迁移一个 bucket（rehashidx 指向的 bucket 及其链表）
5. 新增键直接插入 ht[1]
6. 删除/更新/查找在 ht[0] 和 ht[1] 中依次操作
7. rehashidx 到底后置 -1，释放 ht[0]，ht[1] 成为新 ht[0]

> 🎯 面试高频追问：渐进式 rehash 在持久化 BGSAVE 期间怎么办？BGSAVE 时提高扩容阈值（负载因子 > 5），减少 COW 内存复制。

### 4. ZipList 连锁更新问题及优化
> ZipList 是 Redis 为了节省内存设计的连续内存数据结构。

**ZipList 结构**：
```
<zlbytes> <zltail> <zllen> <entry1> <entry2> ... <entryN> <zlend>
```

每个 entry 的结构：
```
<prevlen> <encoding> <data>
```

- `prevlen`：前一个 entry 的长度（< 254 字节用 1 字节，>= 254 用 5 字节）

**连锁更新问题**：
1. 当插入或删除一个 entry 时，可能导致后续 entry 的 prevlen 从 1 字节变成 5 字节
2. 这个变化会引发"多米诺骨牌"效应，导致后续所有 entry 都需要调整自己的 prevlen
3. 最坏情况下，引发 N 次级联更新，时间复杂度 O(N^2)

**优化措施（Redis 7.0）**：
- ZipList 被 Listpack 替代（在 Stream 等结构中）
- Listpack 每个 entry 自带长度，不依赖前一个 entry，从根本上解决了连锁更新

> 💡 生产环境中真实触发连锁更新的概率非常低，因为需要连续多个 253-254 字节的 entry 才有可能触发。

### 5. Redis 内存碎片问题及解决方案
> 内存碎片是 Redis 运行一段时间后常见的问题。

**产生原因**：
- 频繁修改数据（更新、删除），导致内存分配和释放不均
- Redis 使用 jemalloc/tcmalloc 分配器，分配的内存块大小与请求大小不匹配
- 过期 key 清理后，留下的空洞

**查看方式**：
```bash
INFO memory
# used_memory: 实际使用的数据大小
# used_memory_rss: 操作系统实际分配的内存
# mem_fragmentation_ratio: used_memory_rss / used_memory
```

**判断标准**：
- `mem_fragmentation_ratio > 1.5`：碎片严重
- `mem_fragmentation_ratio < 1`：可能触发了 swap（危险信号）

**解决方案**：
1. **重启**：最直接的方法，但需要做好主从切换
2. **activedefrag（Redis 4.0+）**：自动碎片整理
```bash
# 开启自动碎片整理
activedefrag yes
active-defrag-ignore-bytes 100mb       # 碎片达到 100MB 触发整理
active-defrag-threshold-lower 10       # 碎片率达到 10% 开始整理
active-defrag-threshold-upper 100      # 碎片率达到 100% 全力整理
active-defrag-cycle-min 25             # 整理占 CPU 最小比例
active-defrag-cycle-max 75             # 整理占 CPU 最大比例
```

3. **合理规划内存**：预估数据量，避免频繁扩容

---

## 三、实战场景题

### 1. （字节）如何设计一个高可靠的 Redis 分布式锁？（Redlock 原理）
> 单机 Redis 分布式锁在某些场景下不够安全，Redlock 提供了多节点强一致性方案。

**RedLock 算法步骤**：
1. 获取当前时间戳（毫秒）
2. 依次在 N 个（通常 5 个）独立的 Redis 节点上获取锁，使用 `SET key value NX EX 10`
3. 计算总耗时 = 当前时间 - 开始时间
4. 如果成功获取半数以上节点（> N/2）的锁，且总耗时 < 锁过期时间，则认为加锁成功
5. 如果加锁失败，在所有节点上释放锁

```java
// Redisson RedLock 实现
RLock lock1 = redissonClient1.getLock("myLock");
RLock lock2 = redissonClient2.getLock("myLock");
RLock lock3 = redissonClient3.getLock("myLock");

RedissonRedLock redLock = new RedissonRedLock(lock1, lock2, lock3);
try {
    if (redLock.tryLock(10, 30, TimeUnit.SECONDS)) {
        // 执行业务逻辑
    }
} finally {
    redLock.unlock();
}
```

**关于 RedLock 的争议**：
- Martin Kleppmann（《DDIA》作者）认为 RedLock 不安全，因为：
  - 时钟漂移可能导致锁过期条件计算错误
  - GC pause 可能导致锁持有者实际已超时但自以为还持有锁
- Redis 作者 antirez 逐条反驳了这些观点
- **结论**：大多数业务场景下单机/哨兵模式的分布式锁已足够；极端重要场景可考虑 RedLock 或使用 ZooKeeper

### 2. （阿里）海量数据合并去重场景（BitMap + HyperLogLog + 布隆过滤器对比）
> Redis 提供多种去重工具，选择合适的工具至关重要。

| 工具 | 内存占用 | 精度 | 特性 |
|------|:-------:|:----:|------|
| Set | O(N) | 精确 | 可获取所有元素 |
| BitMap | O(bit_count/8) | 精确 | 位运算高效 |
| HyperLogLog | 12KB 固定 | 0.81% 误差 | UV 统计专用 |
| BloomFilter | O(N * hash_count / 8) | 有误判率 | 判断不存在最准确 |

**场景选择**：
```text
1. 商品去重（精确） -> Set
2. 日活 UV 统计（允许误差） -> HyperLogLog
3. 用户签到（精确、小型） -> BitMap
4. 防止缓存穿透（只判不存在） -> BloomFilter
5. 百亿数据排重 -> BloomFilter（占用内存极小）
```

```bash
# 布隆过滤器插件（Redis 4.0+ 通过 module 加载）
BF.RESERVE bloom_filter 0.01 1000000    # 创建，误判率 1%，容量 100w
BF.ADD bloom_filter user123             # 添加元素
BF.EXISTS bloom_filter user123          # 判断是否存在（可能有误判）
```

### 3. （美团）Redis 异步秒杀系统完整设计
> 秒杀系统核心是"削峰填谷"，Redis 在此扮演关键角色。

**完整流程**：
```
用户 -> 前端限流（验证码/按钮） -> Nginx 限流 -> 
Redis 预扣库存（Lua 脚本原子操作） -> 通过后 ->
MQ 异步落单 -> 消费 MQ 写数据库 -> 返回结果给用户
```

**预扣库存 Lua 脚本**：
```lua
-- keys[1]: 库存 key
-- keys[2]: 用户限购 key
-- argv[1]: 用户 ID
-- argv[2]: 限购数量
local stock = tonumber(redis.call('GET', keys[1]))
if not stock or stock <= 0 then
    return -1  -- 库存不足
end
local userKey = keys[2]
local bought = redis.call('GET', userKey)
if bought then
    return 0  -- 已购买过
end
redis.call('DECR', keys[1])
redis.call('SET', userKey, 1, 'EX', 86400)
return 1  -- 成功
```

**异步下单优化**：
```java
public class SecKillService {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private BlockingQueue<OrderRequest> orderQueue;

    // 秒杀请求入口
    public Result handleSeckill(Long userId, Long productId) {
        // 1. Lua 脚本执行预扣库存
        Long result = executeLuaScript(productId, userId);
        if (result != 1) {
            return Result.fail("库存不足或已购买");
        }
        // 2. 放入阻塞队列，异步处理
        boolean enqueued = orderQueue.offer(new OrderRequest(userId, productId));
        if (!enqueued) {
            // 队列满，补偿恢复库存
            redisTemplate.opsForValue().increment("stock:" + productId, 1);
            return Result.fail("系统繁忙");
        }
        return Result.success("排队中");
    }

    // 后台线程消费
    @PostConstruct
    public void initConsumer() {
        new Thread(() -> {
            while (true) {
                OrderRequest req = orderQueue.take();
                // 创建订单（写入数据库）
                createOrder(req.getUserId(), req.getProductId());
            }
        }).start();
    }
}
```

### 4. （腾讯）Redis 实现 Feed 流（关注推送）系统
> 社交系统中用户关注动态的推送机制。

**推模式 vs 拉模式**：
| 方案 | 原理 | 优点 | 缺点 |
|------|------|------|------|
| 推模式（Fanout） | 发消息时推送给所有粉丝 | 读延迟低 | 大 V 发消息压力大 |
| 拉模式 | 粉丝主动拉取关注列表的动态 | 写压力小 | 粉丝读延迟高 |
| 推拉结合 | 普通用户推，大 V 粉丝拉 | 均衡 | 实现复杂 |

**推模式实现**（适合中小规模）：
```java
// 发动态时，推送给在线粉丝
public void pushFeed(Long userId, Long feedId, String content) {
    // 获取用户的活跃粉丝（最近登录过的）
    Set<String> activeFans = redisTemplate.opsForSet().members("fans:active:" + userId);
    
    // 推送到每个粉丝的收件箱（ZSet 按时间排序）
    for (String fanId : activeFans) {
        redisTemplate.opsForZSet().add(
            "inbox:" + fanId, 
            feedId, 
            System.currentTimeMillis()
        );
        // 控制收件箱大小，只保留最近 1000 条
        redisTemplate.opsForZSet().removeRange("inbox:" + fanId, 0, -1001);
    }
}

// 粉丝拉取动态（滚动分页）
public List<Feed> pullFeed(Long userId, Long lastFeedId, int pageSize) {
    Set<String> feedIds = redisTemplate.opsForZSet().reverseRangeByScore(
        "inbox:" + userId, 
        0, 
        lastFeedId != null ? lastFeedId : Double.MAX_VALUE,
        0, 
        pageSize
    );
    // 根据 feedId 查询具体内容
    return loadFeedDetails(feedIds);
}
```

### 5. （字节）Redis 分布式缓存一致性场景——先删缓存还是先更新数据库？
> 经典面试问题，需要理解各种方案的优劣。

**方案一：Cache Aside Pattern（先更新 DB，后删缓存）**
```java
// 推荐方案
public void updateData(Long id, String value) {
    database.update(id, value);
    redisTemplate.delete("data:" + id);
}
```
- **问题**：更新数据库成功，删除缓存失败（短暂不一致）
- **解决**：删除重试 + TTL 兜底

**方案二：先删缓存，后更新 DB**
```java
// 不推荐
public void updateData(Long id, String value) {
    redisTemplate.delete("data:" + id);
    database.update(id, value);
}
```
- **问题**：删缓存后，另一个线程读取到旧数据并写入缓存（脏数据）

**方案三：延迟双删**
```java
public void updateData(Long id, String value) {
    redisTemplate.delete("data:" + id);
    database.update(id, value);
    // 延迟后再次删除，兜底
    executorService.schedule(() -> {
        redisTemplate.delete("data:" + id);
    }, 500, TimeUnit.MILLISECONDS);
}
```

> 🎯 **最佳实践**：先更新数据库，再删除缓存，配合 TTL 兜底。如果要追求强一致，使用 Canal 监听 binlog 同步。

### 6. 商户查询缓存的缓存更新策略选择
> 不同业务场景选择不同的缓存策略。

**三种缓存更新策略**：

| 策略 | 原理 | 一致性 | 适用场景 |
|------|------|:------:|---------|
| **TTL 过期** | 设置过期时间，过期自动删除 | 弱 | 低频更新、可容忍脏数据 |
| **主动更新 + TTL** | 数据变更时主动更新缓存 | 强 | 高频更新、一致性要求高 |
| **后台异步更新** | Canal 监听 binlog 更新 | 最终一致 | 数据来自多个来源 |

**商铺缓存实战**：
```java
public Shop queryShop(Long id) {
    String key = "shop:" + id;
    // 1. 查缓存
    String json = redisTemplate.opsForValue().get(key);
    if (json != null) {
        return JSON.parseObject(json, Shop.class);
    }
    // 2. 缓存未命中，查数据库
    Shop shop = shopMapper.selectById(id);
    if (shop == null) {
        return null;
    }
    // 3. 回填缓存，设置随机 TTL 避免雪崩
    int ttl = 30 * 60 + new Random().nextInt(600); // 30min ± 10min
    redisTemplate.opsForValue().set(key, JSON.toJSONString(shop), ttl, TimeUnit.SECONDS);
    return shop;
}
```

### 7. Lua 脚本在 Redis 中的高级应用
> Lua 脚本可以保证多条 Redis 命令的原子性执行。

**常用 Lua 脚本模式**：
```lua
-- 1. 限流器（滑动窗口）
local key = KEYS[1]
local limit = tonumber(ARGV[1])
local now = tonumber(ARGV[2])
local window = tonumber(ARGV[3])

redis.call('ZREMRANGEBYSCORE', key, 0, now - window)
local count = redis.call('ZCARD', key)
if count >= limit then
    return 0
end
redis.call('ZADD', key, now, now)
redis.call('EXPIRE', key, window)
return 1

-- 2. 分布式锁（可重入）
local key = KEYS[1]
local threadId = ARGV[1]
local ttl = ARGV[2]

if redis.call('EXISTS', key) == 0 then
    redis.call('HSET', key, threadId, 1)
    redis.call('PEXPIRE', key, ttl)
    return 1
end
if redis.call('HEXISTS', key, threadId) == 1 then
    redis.call('HINCRBY', key, threadId, 1)
    redis.call('PEXPIRE', key, ttl)
    return 1
end
return 0
```

> 💡 Lua 脚本的性能优势：减少网络开销，保证原子性，且 Lua 脚本在 Redis 中可被缓存复用（`SCRIPT LOAD` + `EVALSHA`）。

---

## 四、手写代码题

### 1. Redisson 分布式锁的最佳实践
```java
@Service
public class OrderService {
    @Autowired
    private RedissonClient redissonClient;

    public boolean createOrder(Long userId, Long productId) {
        String lockKey = "lock:order:" + userId;
        RLock lock = redissonClient.getLock(lockKey);
        
        // 尝试加锁：最多等待 3 秒，加锁后 30 秒自动释放（WatchDog 自动续期）
        boolean isLocked = lock.tryLock();
        if (!isLocked) {
            return false; // 获取锁失败，快速返回
        }
        
        try {
            // 检查是否已下单
            if (redisTemplate.opsForValue().get("ordered:" + userId) != null) {
                return false;
            }
            // 检查库存
            Long stock = redisTemplate.opsForValue().decrement("stock:" + productId);
            if (stock < 0) {
                redisTemplate.opsForValue().increment("stock:" + productId);
                return false;
            }
            // 记录下单
            redisTemplate.opsForValue().set("ordered:" + userId, "1", 1, TimeUnit.DAYS);
            // 异步落单到数据库
            asyncCreateOrder(userId, productId);
            return true;
        } finally {
            lock.unlock();
        }
    }
}
```

### 2. 基于 Redis 的分布式限流器（滑动窗口）
```java
@Component
public class RedisRateLimiter {
    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 滑动窗口限流
     * @param key       限流标识
     * @param maxCount  窗口内最大请求数
     * @param windowMs  窗口大小（毫秒）
     * @return 是否允许通过
     */
    public boolean allowRequest(String key, int maxCount, long windowMs) {
        long now = System.currentTimeMillis();
        String redisKey = "rate:sliding:" + key;
        
        // Lua 脚本保证原子性
        String luaScript = 
            "redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[1] - ARGV[3]) " +
            "local count = redis.call('ZCARD', KEYS[1]) " +
            "if count >= tonumber(ARGV[2]) then " +
            "    return 0 " +
            "end " +
            "redis.call('ZADD', KEYS[1], ARGV[1], ARGV[1] .. ':' .. math.random()) " +
            "redis.call('EXPIRE', KEYS[1], math.ceil(ARGV[3] / 1000)) " +
            "return 1";
        
        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(luaScript, Long.class),
            Collections.singletonList(redisKey),
            String.valueOf(now),
            String.valueOf(maxCount),
            String.valueOf(windowMs)
        );
        return result == 1L;
    }
}
```

### 3. Redis + MQ 的异步缓存一致性方案
```java
@Component
public class CacheConsistencyManager {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    // 更新数据库 + 发送删除缓存消息
    @Transactional
    public void updateWithCacheEvict(Long id, String newData) {
        // 1. 更新数据库
        database.update(id, newData);
        // 2. 发送缓存删除消息（保证事务提交后发送）
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    rabbitTemplate.convertAndSend("cache.exchange", 
                        "cache.delete", 
                        new CacheDeleteMessage("data", id));
                }
            }
        );
    }

    // 消费消息，删除缓存
    @RabbitListener(queues = "cache.delete.queue")
    public void handleCacheDelete(CacheDeleteMessage msg) {
        redisTemplate.delete(msg.getKeyPrefix() + ":" + msg.getId());
        log.info("Cache deleted: {}", msg.getKeyPrefix() + ":" + msg.getId());
    }

    // 兜底：设置缓存 TTL，即使删除失败也能自动过期
    public void setWithTTL(String key, Object value, long ttlSeconds) {
        redisTemplate.opsForValue().set(key, JSON.toJSONString(value), ttlSeconds, TimeUnit.SECONDS);
    }
}
```

### 4. Caffeine + Redis 二级缓存实现
```java
@Component
public class TwoLevelCache {
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    // Caffeine 本地缓存
    private final Cache<String, Object> caffeineCache = Caffeine.newBuilder()
        .maximumSize(10000)               // 最大条目
        .expireAfterWrite(10, TimeUnit.SECONDS)  // 写入后 10 秒过期
        .recordStats()                     // 记录统计信息
        .build();

    public Object get(String key) {
        // 1. 查本地缓存
        Object value = caffeineCache.getIfPresent(key);
        if (value != null) {
            return value;
        }
        // 2. 查 Redis
        String json = redisTemplate.opsForValue().get(key);
        if (json != null) {
            value = JSON.parseObject(json, Object.class);
            caffeineCache.put(key, value);
            return value;
        }
        return null;
    }

    public void put(String key, Object value, long ttl) {
        caffeineCache.put(key, value);
        redisTemplate.opsForValue().set(key, JSON.toJSONString(value), ttl, TimeUnit.SECONDS);
    }

    public void evict(String key) {
        caffeineCache.invalidate(key);
        redisTemplate.delete(key);
    }
}
```

### 5. Redis + Lua 实现可重入分布式锁
```lua
-- lock.lua
-- KEYS[1]: 锁 key
-- ARGV[1]: 线程标识（UUID:线程ID）
-- ARGV[2]: 过期时间(ms)
-- 返回值：1 成功，0 失败

if redis.call('EXISTS', KEYS[1]) == 0 then
    redis.call('HINCRBY', KEYS[1], ARGV[1], 1)
    redis.call('PEXPIRE', KEYS[1], ARGV[2])
    return 1
end
if redis.call('HEXISTS', KEYS[1], ARGV[1]) == 1 then
    redis.call('HINCRBY', KEYS[1], ARGV[1], 1)
    redis.call('PEXPIRE', KEYS[1], ARGV[2])
    return 1
end
return 0
```

```lua
-- unlock.lua
-- KEYS[1]: 锁 key
-- ARGV[1]: 线程标识
-- 返回值：1 释放成功，0 不是自己持有

if redis.call('HEXISTS', KEYS[1], ARGV[1]) == 0 then
    return 0  -- 不是自己持有的锁
end
local count = redis.call('HINCRBY', KEYS[1], ARGV[1], -1)
if count > 0 then
    redis.call('PEXPIRE', KEYS[1], 30000)
    return 0  -- 还有重入次数
else
    redis.call('DEL', KEYS[1])
    return 1  -- 完全释放
end
```

---

## 五、系统设计题

### 1. 设计一个分布式定时任务调度系统（基于 Redis）
> 利用 ZSet 实现延迟队列，调度分布式定时任务。

**架构设计**：
```java
@Component
public class RedisDelayQueue {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    
    // 添加延迟任务
    public void addTask(String taskId, long executeTime, String taskData) {
        redisTemplate.opsForZSet().add("delay:queue", 
            taskId + ":" + taskData, executeTime);
    }
    
    // 消费者轮询
    @Scheduled(fixedDelay = 1000)
    public void pollTasks() {
        long now = System.currentTimeMillis();
        // 获取已到期的任务
        Set<String> tasks = redisTemplate.opsForZSet().rangeByScore(
            "delay:queue", 0, now, 0, 100);
        
        for (String task : tasks) {
            String taskId = task.split(":")[0];
            RLock lock = redissonClient.getLock("task:lock:" + taskId);
            if (lock.tryLock()) {
                try {
                    // 移除任务
                    Long removed = redisTemplate.opsForZSet().remove(
                        "delay:queue", task);
                    if (removed > 0) {
                        // 执行任务
                        executeTask(task);
                    }
                } finally {
                    lock.unlock();
                }
            }
        }
    }
}
```

### 2. 设计一个 Redis 集群下的分布式 Session 方案
> 用 Redis 替代 Tomcat Session，实现无状态应用。

**架构设计**：
```java
// 自定义 HttpSession 实现
public class RedisHttpSession implements HttpSession {
    private String sessionId;
    private StringRedisTemplate redisTemplate;
    private static final int MAX_INACTIVE_INTERVAL = 1800; // 30 分钟
    
    @Override
    public Object getAttribute(String name) {
        return redisTemplate.opsForHash().get("session:" + sessionId, name);
    }
    
    @Override
    public void setAttribute(String name, Object value) {
        redisTemplate.opsForHash().put("session:" + sessionId, name, value);
        redisTemplate.expire("session:" + sessionId, MAX_INACTIVE_INTERVAL, TimeUnit.SECONDS);
    }
    
    @Override
    public void removeAttribute(String name) {
        redisTemplate.opsForHash().delete("session:" + sessionId, name);
    }
    
    @Override
    public void invalidate() {
        redisTemplate.delete("session:" + sessionId);
    }
}
```

> 💡 实际上 Spring Session 已经提供了基于 Redis 的 Session 管理方案，直接集成 `spring-session-data-redis` 即可。

### 3. 设计一个 Redis 集群管理监控平台
> 企业级 Redis 集群治理的核心功能。

**核心模块**：
1. **集群拓扑展示**：展示主从关系、槽位分布、节点状态
2. **性能监控**：QPS、命中率、内存使用、网络流量、连接数
3. **慢查询分析**：慢查询 Top N、执行频率分析
4. **大 Key 分析**：自动扫描大 key，展示内存占用排行
5. **热 Key 发现**：自动识别高频率访问的 key
6. **容量规划**：内存增长趋势预测、扩容建议
7. **告警系统**：CPU 过高、内存超阈值、连接数爆满等告警

```bash
# 监控命令集合
INFO memory          # 内存使用情况
INFO clients         # 客户端连接数
INFO commandstats    # 命令执行统计
SLOWLOG GET 100      # 获取慢查询
CLIENT LIST          # 客户端列表
```

### 4. 设计一个 Redis 多级缓存电商商品详情页
> 商品详情页是典型的高并发读场景，适合多级缓存架构。

**缓存架构**：
```
用户请求 -> CDN（图片/静态资源）
         -> Nginx Local Cache（热点商品 HTML 片段）
         -> Redis Cluster（商品信息 JSON）
         -> Caffeine Local Cache（最热商品）
         -> 数据库（兜底）
```

**各级缓存 TTL**：
| 层级 | TTL | 容量 | 说明 |
|------|:---:|:----:|------|
| Nginx 本地 | 60s | 1000 商品 | 突发流量防护 |
| Redis | 30min | 全部商品 | 主缓存层 |
| Caffeine | 10s | 前 1000 热 Key | 毫秒级响应 |

### 5. 短链接系统中的 Redis 应用
> 短链接系统是 Redis 应用的综合案例。

**核心功能**：
1. **ID 生成**：使用 INCR 生成短链接 ID
2. **短链接映射**：Hash 结构存储长链接 -> 短链接映射
3. **访问计数**：INCR 统计每个短链接的访问次数
4. **访问限流**：对单个短链接 IP 限流
5. **热点 Short URL 缓存**：热链接自动缓存到本地

```java
public class ShortUrlService {
    // 生成短链接
    public String createShortUrl(String longUrl, long expireSeconds) {
        long id = redisTemplate.opsForValue().increment("url:counter");
        String shortCode = Base62.encode(id);
        // 存储映射
        redisTemplate.opsForHash().put("url:mapping", shortCode, longUrl);
        if (expireSeconds > 0) {
            redisTemplate.expire("url:mapping", expireSeconds, TimeUnit.SECONDS);
        }
        return shortCode;
    }
    
    // 访问短链接
    public String getLongUrl(String shortCode) {
        String longUrl = (String) redisTemplate.opsForHash().get("url:mapping", shortCode);
        if (longUrl != null) {
            redisTemplate.opsForValue().increment("url:click:" + shortCode);
        }
        return longUrl;
    }
}
```

---

## 六、常见坑点与最佳实践

### 常见坑点

| 坑点 | 原因 | 解决方案 |
|------|------|---------|
| RedisTemplate 操作 Hash 序列化异常 | key/value 序列化器设置不全 | 分别设置 keySerializer 和 valueSerializer |
| Lettuce 连接泄漏 | 使用不当导致连接未归还 | 确认使用 try-with-resources 或在连接池中操作 |
| 分布式锁过期业务未完成 | 业务耗时超出锁过期时间 | 使用 WatchDog 自动续期 |
| Pipeline 在集群模式下报错 | key 在不同的节点上 | 使用 HashTag 或将 key 按节点分组 |
| 缓存穿透导致数据库被打爆 | 查询不存在的数据 | 布隆过滤器 + 空值缓存 |
| Redisson 连接超时 | 网络抖动或 Redis 负载过高 | 调整超时参数，增加重试次数 |
| AOF 文件过大影响性能 | AOF 文件持续增长 | 开启 auto-aof-rewrite 定期重写 |
| RDB 持久化导致 Redis 卡顿 | fork 子进程复制页表 | 避开高峰期，控制内存上限 |
| 大 key 操作阻塞集群 | 大集合操作耗时过长 | 拆分大 key，使用 SSCAN/HSCAN 分批操作 |
| 主从全量同步导致带宽打满 | 大实例主从切换后触发全量同步 | 控制单实例大小，增量同步兜底 |

### 最佳实践

| 实践 | 详细说明 |
|------|---------|
| Key 规范命名 | 统一格式 `业务:实体:ID:字段`，如 `shop:product:1001:detail` |
| 控制 key 数量 | 根据业务量预估 key 总数，避免大 key 分布 |
| 合理设置 TTL | 大部分缓存场景设置 TTL，避免未用 key 长期占用内存 |
| 连接池配置 | maxTotal 根据业务 QPS 评估，建议 8-16 |
| 序列化选择 | 推荐 JSON 或 Protobuf，避免 Java 原生序列化 |
| 批量操作防 OOM | Pipeline 命令数量控制在 5000 以内 |
| 监控关键指标 | QPS、命中率、内存、连接数、慢查询 |
| 定期数据清理 | 扫描无过期时间的 key 并合理设置 TTL |
| 代码中处理降级 | 缓存不可用时降级到数据库，避免雪崩 |
| 配置合理预警 | 内存 > 80% 发出预警，预留 buffer |

---

## 七、面试回答模板

### 模板1：如何设计一个高并发的秒杀系统？（Redis 角度）
> 面试官期望：Redis 预扣库存 + Lua + MQ 异步

**回答结构**：
1. **前端限流**：按钮置灰、限流、验证码
2. **Redis 预扣**：使用 Lua 脚本原子性扣减库存，秒级响应
3. **MQ 削峰**：扣减成功后发送消息到 MQ，异步落库
4. **乐观锁兜底**：数据库层使用 CAS 更新，防止超卖
5. **集群隔离**：秒杀商品缓存和普通业务缓存隔离
6. **核心点**：Redis 扛峰值流量，数据库做最终持久化，让请求"排队"而不是"挤压"

### 模板2：Redis 集群模式下如何进行批量操作？
> 面试官期望：了解集群架构限制及解决方案

**回答结构**：
1. **问题本质**：集群模式下 key 分布在不同节点，Pipeline 和事务无法跨节点保证
2. **解决方案一**：Hash Tag 强制相关 key 在同一槽位（`{user:1001}:name`、`{user:1001}:age`）
3. **解决方案二**：计算 key 的槽位，按节点分组，分别 Pipeline
4. **解决方案三**：使用 Redisson 或 SCAN 命令分批处理
5. **最佳实践**：设计 key 时就用 Hash Tag 将相关数据放在同一槽

### 模板3：Redis 缓存一致性如何保证？
> 面试官期望：Cache Aside + 延迟双删 + Canal 方案

**回答结构**：
1. **Cache Aside 模式**：先更新 DB，后删缓存。更安全，但短暂不一致
2. **延迟双删方案**：先删缓存、更新 DB、延迟后再次删缓存。在高并发下更可靠
3. **Canal 监听 binlog**：无侵入的最终一致性方案，业务代码无需改动
4. **TTL 兜底**：无论哪种方案，设置 TTL 保证最终一致性
5. **强一致方案**：2PC 或分布式锁，但牺牲性能和可用性
6. **总结**：根据业务容忍度选择，大多数场景 Cache Aside + TTL 已足够

### 模板4：Redis 底层数据结构有哪些？各有什么优缺点？
> 面试官期望：能详细说明 SDS、ZipList、SkipList、Dict 等底层结构

**回答结构**：
1. **SDS**（String 底层）：O(1) 获取长度，二进制安全，预分配减少内存分配次数
2. **Dict**（Hash 底层）：渐进式 rehash 实现平滑扩容缩容
3. **ZipList**（小数据场景）：连续内存节省空间，但连锁更新问题，7.0 被 Listpack 取代
4. **QuickList**（List 底层）：ZipList 链表，平衡内存和效率
5. **SkipList**（ZSet 底层）：O(logN) 查找，实现比平衡树简单，范围查询高效
6. **IntSet**（Set 小整数场景）：有序连续整数，二分查找，升级策略节省内存
7. **总结**：每种数据结构都针对特定场景做了极致优化，这是 Redis 高性能的基础

### 模板5：Redis 主从、哨兵、集群全流程故障转移过程
> 面试官期望：深入理解高可用机制

**回答结构**：
1. **主从复制**：全量同步（RDB + buffer）+ 增量同步（repl_backlog）
2. **哨兵故障转移**：
   - 主观下线（每个哨兵自己判断）
   - 客观下线（多个哨兵达成共识，>= quorum）
   - Leader 选举（Raft 算法选出一个哨兵执行 failover）
   - 选新主库（优先级 > 同步进度 > runid）
3. **集群故障转移**：
   - 集群节点互 Ping，半数以上节点认为某节点下线则标记为 FAIL
   - 从节点发起选举，raft 算法选 leader
   - 新主库接管槽位，集群恢复正常
4. **关键参数**：哨兵 `down-after-milliseconds`、`failover-timeout`

---

## 八、快速查漏补缺 Checklist

- [ ] Redis 六种 Java 客户端的对比（Jedis/Lettuce/Redisson）
- [ ] RedisTemplate 序列化配置（Jackson2JsonRedisSerializer）
- [ ] Pipeline 原理和使用场景（减少 RTT）
- [ ] Redisson 分布式锁原理（Lua + Hash + 可重入 + WatchDog）
- [ ] RedLock 多节点锁原理与争议
- [ ] Lua 脚本原子性操作
- [ ] Redis Stream 消息队列完整用法（XADD/XREAD/XGROUP/XREADGROUP/XACK）
- [ ] 多级缓存架构（Nginx -> Redis -> Caffeine -> DB）
- [ ] Canal 缓存同步原理（binlog 监听）
- [ ] 集群模式下批量操作方案（Hash Tag + 分组 Pipeline）
- [ ] Redis 底层数据结构（SDS/Dict/ZipList/QuickList/SkipList/IntSet）
- [ ] 渐进式 rehash 原理和流程
- [ ] ZipList 连锁更新问题和 Listpack 优化
- [ ] epoll LT/ET 模式区别
- [ ] Redis 6.0 多线程网络模型
- [ ] Redis Object 编码矩阵（每种数据类型的底层编码转换条件）
- [ ] 内存碎片整理（activedefrag）
- [ ] 延迟队列实现（ZSet 轮询）
- [ ] Feed 流推拉模式对比（推/拉/推拉结合）
- [ ] 限流器实现（滑动窗口 ZSet + Lua）
- [ ] 缓存一致性方案（Cache Aside / 延迟双删 / Canal）
- [ ] 大 key 热 key 发现与解决方案
- [ ] 全局 ID 生成（Redis INCR / 雪花算法对比）
- [ ] 分布式 Session 方案（Spring Session + Redis）
- [ ] 短链接系统的 Redis 应用
