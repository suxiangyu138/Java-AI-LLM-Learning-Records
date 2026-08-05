# Redis 面试宝典（基础篇）
> 基于课程大纲全面覆盖Redis面试高频考点，从入门到五大数据类型、持久化、高可用架构及缓存问题

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

### 1. 什么是 Redis？Redis 和 Memcached 有什么区别？
> Redis 是一种基于内存的 Key-Value 存储系统，支持丰富的数据结构和持久化。

| 对比维度 | Redis | Memcached |
|---------|-------|-----------|
| 数据类型 | 丰富：String/Hash/List/Set/ZSet/HyperLogLog/Geo/Stream | 仅 String |
| 持久化 | 支持 RDB + AOF | 不支持 |
| 主从复制 | 支持 | 不支持 |
| 事务 | 支持（不完整） | 不支持 |
| 集群模式 | 支持 | 不支持 |
| 内存淘汰 | 多种策略 | LRU |
| 多线程 | 6.0+ 网络 I/O 多线程 | 多线程 |

> 💡 Redis 的核心优势：数据结构丰富、持久化保障、支持分布式集群。

### 2. Redis 为什么这么快？
> Redis 单机 QPS 可达 10w+，主要得益于多方面原因。

1. **纯内存操作**：数据存储在内存中，读写速度远快于磁盘操作（纳秒级 vs 毫秒级）
2. **单线程模型**：避免了多线程切换和锁竞争的开销（6.0 网络 I/O 多线程但核心命令执行仍是单线程）
3. **I/O 多路复用**：基于 epoll 的事件驱动模型，可以高效处理大量并发连接
4. **高效的数据结构**：简单动态字符串（SDS）、双端链表、压缩列表、跳表等针对场景优化的数据结构
5. **RESP 协议**：二进制安全、解析高效的 Redis 序列化协议

> 🎯 面试常问："Redis 单线程为什么还这么快？" 核心回答点：内存 + 单线程无锁竞争 + epoll IO 多路复用。

### 3. Redis 支持哪些数据类型？各自的应用场景？
> Redis 支持五大基础类型和多种高级类型。

| 类型 | 底层实现 | 特征 | 典型场景 |
|------|---------|------|---------|
| **String** | SDS/INT | 最基础类型，可存字符串/数字/Binary | 缓存、计数器、分布式锁、Session |
| **Hash** | ZipList / Dict | 存储对象结构 | 用户信息、商品详情、配置项 |
| **List** | QuickList | 双向链表，支持左右插入 | 消息队列、最新消息列表、时间线 |
| **Set** | IntSet / Dict | 无序不重复，支持交并差 | 标签、共同好友、去重统计 |
| **ZSet** | ZipList / SkipList | 有序不重复，按 score 排序 | 排行榜、延迟队列、优先级队列 |
| **HyperLogLog** | 概率数据结构 | 基数统计，占用 12KB 固定内存 | UV 统计、去重计数 |
| **Geo** | ZSet 封装 | 地理位置计算 | 附近的人、LBS 服务 |
| **BitMap** | String 位操作 | 位运算，内存占用极小 | 签到统计、布隆过滤器 |
| **Stream** | 紧凑链表 | 消息队列，支持消费者组 | 消息队列、事件流 |

> 💡 面试常问：ZSet 的底层实现是 ZipList + SkipList。当元素少于 128 且长度小于 64 字节时用 ZipList，否则用 SkipList。

### 4. String 类型的基本操作有哪些？
> String 是 Redis 最基础也最常用的数据类型。

```bash
# 基本操作
SET key value          # 设置 key 的值
GET key                # 获取 key 的值
DEL key                # 删除 key
EXISTS key             # 判断 key 是否存在
TYPE key              # 查看 key 的类型

# 批量操作
MSET key1 val1 key2 val2   # 批量设置
MGET key1 key2              # 批量获取

# 数值操作
INCR key                # 加 1（原子操作）
DECR key                # 减 1（原子操作）
INCRBY key 5            # 加 5
DECRBY key 3            # 减 3

# 过期时间
SETEX key 10 value      # 设置带过期时间（秒）
SETNX key value         # 不存在时才设置（分布式锁基础）
EXPIRE key 10           # 设置过期时间
TTL key                 # 查看剩余过期时间

# 字符串操作
GETRANGE key 0 5        # 截取子串
STRLEN key              # 字符串长度
APPEND key "append"     # 追加字符串
```

### 5. Redis 的过期策略是怎样的？
> Redis 采用"定期删除 + 惰性删除"两种策略组合来清理过期 key。

| 策略 | 工作机制 | 优点 | 缺点 |
|------|---------|------|------|
| **定期删除** | 每 100ms 随机抽取部分带过期时间的 key，检查并删除已过期的 key | 主动清理，不会堆积 | 删除不及时，可能漏删 |
| **惰性删除** | 当访问某个 key 时，检查是否过期，过期则删除并返回 nil | 可保证不浪费 CPU 去删不用的 key | 过期 key 没被访问就永远存在 |

- **两者结合**：定期删除负责主动清理，惰性删除作为兜底。只靠定期删除会漏，只靠惰性删除会导致内存泄漏。
- **如果过期 key 没被清理掉怎么办？** 当内存超过 `maxmemory` 时，触发内存淘汰策略。

> 💡 面试必问：Redis 的过期删除不是定时扫描所有 key，而是随机抽样的，避免 CPU 过载。

### 6. Redis 的内存淘汰策略有哪些？
> 当内存使用达到 `maxmemory` 上限时，Redis 根据配置的策略淘汰 key。

| 策略 | 描述 | 适用场景 |
|------|------|---------|
| **noeviction** | 不淘汰，写入时报错（默认策略） | 禁止删除数据的场景 |
| **allkeys-lru** | 从所有 key 中淘汰最近最少使用的 | 最常用的策略 |
| **allkeys-lfu** | 从所有 key 中淘汰访问频率最低的 | 需要按访问频率淘汰 |
| **volatile-lru** | 从设置了过期时间的 key 中淘汰最近最少使用的 | 缓存类应用 |
| **volatile-lfu** | 从设置了过期时间的 key 中淘汰访问频率最低的 | 缓存类应用 |
| **volatile-ttl** | 从设置了过期时间的 key 中淘汰剩余时间最短的 | 按过期时间淘汰 |
| **volatile-random** | 从设置了过期时间的 key 中随机淘汰 | 简单场景 |
| **allkeys-random** | 从所有 key 中随机淘汰 | 简单场景 |

> 🎯 最常用的配置：`maxmemory-policy allkeys-lru`。Redis 4.0 之后引入了 LFU 策略。

### 7. RDB 和 AOF 两种持久化的区别？
> Redis 提供两种持久化方式，各有优劣。

| 对比维度 | RDB（快照） | AOF（追加文件） |
|---------|:----------:|:--------------:|
| 存储内容 | 二进制数据快照 | 写命令文本 |
| 文件大小 | 较小，二进制压缩 | 较大，可 AOF 重写瘦身 |
| 恢复速度 | 快（直接加载） | 慢（重放命令） |
| 数据丢失 | 可能丢失两次 save 间隔的数据 | 根据配置丢失 1 秒或 0 秒数据 |
| 对性能影响 | save 阻塞，bgsave 子进程 fork | 写时追加，性能影响小 |
| 优先级 | 低 | 高（启动时优先加载 AOF） |
| 适用场景 | 数据备份、灾难恢复 | 数据安全要求高的场景 |

```bash
# RDB 配置
save 900 1      # 900秒内至少1个key变化则保存
save 300 10     # 300秒内至少10个key变化则保存
save 60 10000   # 60秒内至少10000个key变化则保存
dbfilename dump.rdb

# AOF 配置
appendonly yes
appendfsync everysec    # 每秒 fsync，默认配置
# appendfsync always    # 每次写都 fsync，最安全但最慢
# appendfsync no        # 由 OS 决定，不安全
```

> 🎯 最佳实践：两者同时开启。RDB 用于快速恢复和备份，AOF 用于数据安全保障。Redis 4.0+ 支持混合持久化。

### 8. 主从复制的原理是什么？
> 主从复制是 Redis 高可用的基础，支持一主多从架构。

**核心流程**（全量同步）：
1. 从库向主库发送 `SYNC` 或 `PSYNC` 命令
2. 主库执行 `BGSAVE` 生成 RDB 快照
3. 主库将 RDB 文件发送给从库
4. 从库加载 RDB 文件
5. 主库将 RDB 生成期间的写命令通过缓冲区发送给从库
6. 后续增量同步：主库将写命令持续发送给从库（基于长连接）

**增量同步**：
- 从库断开重连后，主库通过 `repl_backlog_buffer`（环形缓冲区）发送断连期间的增量数据
- 如果从库落后太多，超过缓冲区大小，则触发全量同步

```bash
# 从库配置
replicaof <master-ip> <master-port>   # 6.x 语法
# slaveof <master-ip> <master-port>   # 旧版语法
masterauth <password>
```

> 💡 主从复制是异步的，主库不等待从库确认。这是数据一致性和性能之间的权衡。

### 9. Redis 哨兵（Sentinel）的作用？
> Redis Sentinel 是 Redis 的高可用解决方案，自动监控和故障转移。

**三大功能**：
1. **监控（Monitoring）**：持续检查主从节点是否正常运行
2. **自动故障转移（Automatic Failover）**：主库宕机时将从库升级为新主库
3. **通知（Notification）**：通知客户端新的主库地址

**工作原理**：
- 哨兵节点之间通过 `__sentinel__:hello` 频道进行通信
- 哨兵通过 Ping/Pong 检测节点健康状态（主观下线 / 客观下线）
- 客观下线后哨兵进行故障转移投票（Raft 一致性算法）
- 选择新的主库（优先选择优先级最高、数据最完整的从库）

> ⚠️ 哨兵至少需要 3 个节点（奇数个）才能实现高可用，避免"脑裂"场景。

### 10. Redis 集群的哈希槽机制？
> Redis Cluster 通过哈希槽将数据分布到多个节点。

**核心概念**：
- 整个集群有 **16384 个哈希槽**
- 每个 key 通过 `CRC16(key) % 16384` 计算所属的哈希槽
- 每个节点负责一部分哈希槽（如 3 节点集群平均分配：0-5460, 5461-10922, 10923-16383）
- 新增/删除节点时通过迁移哈希槽重新分配数据

```bash
# 集群命令
CLUSTER INFO                  # 查看集群信息
CLUSTER NODES                 # 查看节点信息
CLUSTER KEYSLOT key           # 查看 key 的哈希槽
CLUSTER GETKEYSINSLOT slot 10 # 查看槽中的 key
```

**重定向机制**：
- 客户端向错误的节点发起请求时，节点返回 `MOVED` 或 `ASK` 错误
- `MOVED`：哈希槽已永久迁移到其他节点，客户端更新本地缓存
- `ASK`：哈希槽正在迁移中，客户端重定向到目标节点

> 🎯 哈希槽优势：添加或删除节点时只需迁移部分槽和数据，无需全量 rehash。

### 11. 什么是缓存穿透？如何解决？
> 缓存穿透是指查询不存在的数据，每次都会穿透缓存直达数据库。

**原因**：查询一个数据库和缓存都不存在的数据（如恶意攻击使用不存在的用户 ID 持续请求）。

**解决方案**：
1. **缓存空值**：查询结果为空时，也缓存一个空值（设置较短 TTL，如 60 秒）
2. **布隆过滤器**：使用布隆过滤器在请求到达前先判断数据是否存在，不存在直接拒绝
3. **参数校验**：在应用层校验参数合法性，不符合规则直接拦截
4. **限流降级**：对可疑 IP 进行限流

```java
// 缓存空值示例（伪代码）
public Object query(String key) {
    Object value = redis.get(key);
    if (value != null) {
        return value;
    }
    // 缓存未命中，查询数据库
    value = db.query(key);
    if (value == null) {
        redis.set(key, "NULL", 60); // 缓存空值，TTL 60s
    } else {
        redis.set(key, value, 3600);
    }
    return value;
}
```

> ⚠️ 布隆过滤器存在一定误判率（判断存在可能不存在，但判断不存在一定不存在）。

### 12. 什么是缓存击穿？如何解决？
> 缓存击穿是指某个热点 key 在缓存失效的瞬间被大量并发请求同时穿透到数据库。

**特点**：高并发访问同一个热点 key，该 key 恰好过期。

**解决方案**：
1. **互斥锁**（分布式锁）：第一个请求获取锁并查询数据库，其他请求等待或快速返回
2. **逻辑过期**：缓存中写入逻辑过期时间而非 TTL，后台线程异步刷新
3. **热点 key 永不过期**：后台定时更新，不给 TTL

```java
// 互斥锁方案（伪代码）
public Object queryWithLock(String key, long timeout) {
    Object value = redis.get(key);
    if (value != null) {
        return value;
    }
    String lockKey = "lock:" + key;
    if (redis.setnx(lockKey, "1", timeout)) {
        try {
            value = db.query(key);
            redis.set(key, value, 3600);
            return value;
        } finally {
            redis.del(lockKey);
        }
    } else {
        Thread.sleep(50);
        return redis.get(key); // 重试
    }
}
```

### 13. 什么是缓存雪崩？如何解决？
> 缓存雪崩是指大量缓存同时过期，或者缓存节点宕机，导致大量请求直接打到数据库。

**原因**：
- 大量 key 设置了相同的过期时间（如零点集体过期）
- Redis 节点宕机导致缓存不可用

**解决方案**：
1. **过期时间打散**：在基础过期时间上加随机值，避免集体过期
2. **多级缓存**：本地缓存（Caffeine）+ Redis 缓存 + 数据库
3. **服务限流降级**：Sentinel 等限流组件，超出阈值直接返回默认值
4. **缓存预热**：提前加载热点数据，避免启动后瞬间大量缓存未命中
5. **Redis 高可用**：主从 + 哨兵 + 集群，避免单点故障

```java
// 过期时间加随机偏移
int baseTTL = 3600;
int randomTTL = baseTTL + new Random().nextInt(600);
redis.set(key, value, randomTTL);
```

> 🎯 面试三连问：缓存穿透、缓存击穿、缓存雪崩是 Redis 面试最高频的三大问题，必须熟练掌握。

### 14. Redis 的发布订阅机制？
> Redis Pub/Sub 是一种消息通信模式，发送者（publisher）发送消息，订阅者（subscriber）接收消息。

```bash
# 发布订阅命令
SUBSCRIBE channel        # 订阅频道
PUBLISH channel message  # 向频道发送消息
UNSUBSCRIBE channel      # 退订频道
PSUBSCRIBE pattern*      # 模式订阅（支持通配符）
```

- **特点**：消息即发即失，不持久化；消费者不在线则消息丢失
- **限制**：无法回溯历史消息，没有消息确认机制
- **替代方案**：Redis 5.0+ Stream 提供了更完善的消息队列功能

> 💡 Pub/Sub 适合实时性要求高、允许丢消息的场景；消息队列需求应优先使用 Stream。

### 15. Redis 事务是如何工作的？
> Redis 事务通过 MULTI, EXEC, DISCARD, WATCH 命令实现。

```bash
# 基本事务
MULTI             # 开启事务
SET key1 value1   # 命令入队
SET key2 value2   # 命令入队
GET key1          # 命令入队
EXEC              # 执行事务（一次性执行队列中的所有命令）

# 带 WATCH 的乐观锁
WATCH key1        # 监视 key1，如果被其他客户端修改则事务失败
MULTI
SET key1 new_val
EXEC              # 如果 key1 在 WATCH 后被修改，EXEC 返回 nil
```

- **Redis 事务与 MySQL 事务的区别**：
  - MySQL 事务支持 ACID（特别是回滚），Redis 不支持回滚
  - Redis 事务是"一次性提交，依次执行"，中间失败不会回滚已执行的命令
  - Redis 事务的隔离性通过单线程保证

> ⚠️ Redis 事务只是保证"一组命令的原子性执行"，不提供传统意义的事务回滚。

### 16. Redis 是单线程的吗？6.0 后的多线程模型？
> 理解 Redis 多线程演进对面试很重要。

**Redis 6.0 之前**：严格单线程，所有命令在一个线程中串行执行。

**Redis 6.0 之后**：引入多线程 I/O，但核心命令执行仍是单线程。

| 组件 | 6.0 前 | 6.0 后 |
|------|--------|--------|
| 网络 I/O（读/写） | 单线程 | 多线程（默认关闭） |
| 命令执行 | 单线程 | 单线程（不变） |
| 持久化 | 子进程（BGSAVE/AOF rewrite） | 子进程 |
| 异步删除 | 不支持 | 支持（UNLINK, FLUSHALL ASYNC） |

- **多线程 I/O 的作用**：将 socket 读取和写入操作由多个线程处理，提升网络 I/O 吞吐量
- **命令执行仍是单线程**：避免多线程竞态条件和锁开销

```bash
# 开启多线程 I/O
io-threads 4
io-threads-do-reads yes
```

> 💡 多线程 I/O 在纯内存操作瓶颈不在 CPU 时效果不明显，主要在网络开销大时有改善。

---

## 二、深度原理剖析

### 1. SDS（简单动态字符串） vs C 字符串？
> Redis 的 String 类型底层使用 SDS 实现，优于 C 语言原生字符串。

| 对比 | C 字符串 | SDS |
|------|---------|-----|
| 获取长度 | O(n) | O(1)（有 len 字段） |
| 缓冲区溢出 | 不安全 | 自动扩容 |
| 二进制安全 | 否（\0 截断） | 是（用 len 判断结束） |
| 修改次数 | 每次都重新分配 | 预分配，减少分配次数 |
| 最大长度 | 受 \0 限制 | 可存储任意二进制数据 |

**SDS 结构**（SDS 5.0 后简化）：
```c
struct sdshdr {
    int len;        // 已使用长度
    int alloc;      // 分配的总长度（不含头和空终止符）
    char buf[];     // 字节数组
};
```

### 2. ZipList 和 QuickList 的区别？
> ZipList 是 List/Hash/ZSet 的底层编码之一，QuickList 是 List 在 3.2 后的新编码。

**ZipList（压缩列表）**：
- 连续内存块，节省内存但修改代价大
- 元素较少且内容不长时使用
- 连锁更新问题：当插入/删除元素导致所有后续元素的 prevlen 从 1 字节变为 5 字节

**QuickList（快速列表）**：
- 是 ZipList 的双向链表，每个节点是一个 ZipList
- 平衡了内存占用和修改性能
- 是 Redis 3.2 之后 List 的唯一实现

```text
QuickList 结构：
[head] <-> [ZipList] <-> [ZipList] <-> [ZipList] <-> [tail]
                   每个 ZipList 存储多个元素
```

### 3. SkipList（跳表）的实现原理？
> ZSet 的有序性离不开 SkipList（跳表）的支持。

- **数据结构**：多层级链表，每层是下一层的"快速通道"
- **查找时间复杂度**：平均 O(log N)，最坏 O(N)
- **插入/删除**：O(log N)
- **与平衡树对比**：实现简单，范围查找效率高，内存占用可通过参数调节

```text
SkipList 结构示意：
level 3: 1 -------------------------------> 9
level 2: 1 --------> 5 -----------------> 9
level 1: 1 --> 3 --> 5 --> 7 --> 8 --> 9
level 0: 1 -> 2 -> 3 -> 4 -> 5 -> 6 -> 7 -> 8 -> 9
                     ^
             查找 7 的路径
```

- **特点**：插入时随机决定层数（power law 分布，大部分节点层数低）
- **最大层数**：默认 32（Redis 7.0 支持 64）

### 4. Redis 的渐进式 rehash 原理？
> Dict（字典）在扩容或缩容时采用渐进式 rehash 避免阻塞服务。

**触发条件**：
- 扩容：负载因子 >= 1（BGSAVE 中 >= 5）
- 缩容：负载因子 < 0.1

**渐进式 rehash 流程**：
1. 为 `ht[1]` 分配空间（2 的 N 次方）
2. 在字典中维护 `rehashidx` 指针，初始为 0
3. 每次增删改查操作时，顺带将 `ht[0]` 的一个 bucket 迁移到 `ht[1]`
4. 新写入操作直接在 `ht[1]` 上进行
5. 当所有 bucket 迁移完毕，`rehashidx` 置为 -1，`ht[0]` 指向 `ht[1]`，释放旧 `ht[0]`

> 💡 渐进式 rehash 的巧妙之处：将一次性大规模数据迁移分散到每个操作中，保证 Redis 服务不卡顿。

### 5. Redis 的 reactor 模型和 epoll 机制？
> Redis 基于 I/O 多路复用（epoll/kqueue/select）实现高性能网络处理。

**Reactor 模式**：
```
事件循环（Event Loop）：
  1. epoll_wait 等待事件就绪
  2. 根据事件类型分发给处理器
  3. 处理完后再回到事件循环
```

**epoll 优势**：
- **select 限制**：最大 FD 为 1024，每次需遍历所有 FD
- **poll 改进**：无最大 FD 限制，但仍需遍历
- **epoll 突破**：事件驱动，仅返回就绪的 FD（O(1)），使用 mmap 加速内核态用户态数据传输

**Redis 的事件类型**：
- **文件事件**：Socket 的可读/可写事件（客户端连接、读写请求）
- **时间事件**：定时任务（过期 key 清理、AOF 刷盘等）

> 🎯 面试常问：Redis 通过 epoll 实现单线程处理大量并发连接，但核心瓶颈从来不在 CPU 而在内存和网络。

---

## 三、实战场景题

### 1. （字节）如何用 Redis 实现分布式锁？有哪些注意事项？
> 分布式锁是 Redis 最经典的应用场景之一。

```java
// 基础实现：使用 SETNX + EXPIRE
// Redis 命令：SET key value NX EX 10

// 正确的 Jedis 实现
String result = jedis.set(lockKey, requestId, "NX", "EX", 30);
if ("OK".equals(result)) {
    try {
        // 执行业务逻辑
    } finally {
        // 使用 Lua 脚本保证原子性释放
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        jedis.eval(script, Collections.singletonList(lockKey), Collections.singletonList(requestId));
    }
}
```

**注意事项**：
1. **原子性加锁**：`SET key value NX EX 30`，不能分开 SETNX + EXPIRE
2. **唯一标识**：value 用 requestId 标识持有者，防止误删其他线程的锁
3. **原子性释放**：使用 Lua 脚本保证 GET + DEL 的原子性
4. **合理过期时间**：设置合理的过期时间，防止死锁；业务超时时需要续期
5. **Redisson**：生产环境推荐使用 Redisson，它提供了可重入锁、WatchDog 自动续期、RedLock 等高级功能

### 2. （阿里）如何用 Redis 实现排行榜功能？
> 利用 ZSet 有序集合特性实现实时排行榜。

```bash
# 添加/更新用户分数
ZADD leaderboard:202607 100 user001
ZADD leaderboard:202607 200 user002
ZADD leaderboard:202607 150 user003

# 获取 Top 10 排行榜
ZREVRANGE leaderboard:202607 0 9 WITHSCORES

# 获取用户排名
ZREVRANK leaderboard:202607 user001

# 获取用户分数
ZSCORE leaderboard:202607 user001

# 获取某分数段的用户
ZRANGEBYSCORE leaderboard:202607 100 200 WITHSCORES
```

**分时段排行榜实现**：
- 日榜：`leaderboard:20260722`
- 周榜：`leaderboard:2026W30`
- 月榜：`leaderboard:202607`
- 使用 `UNION` 或定时聚合计算

> 💡 排行榜更新时使用 `ZINCRBY` 原子增减分数，避免并发问题。

### 3. （美团）Redis 如何实现附近的人功能？
> 利用 Redis 的 Geo 数据结构实现 LBS 功能。

```bash
# 添加商家地理位置
GEOADD shops 116.397128 39.916527 "shop_001"   # 天安门
GEOADD shops 116.415045 39.909608 "shop_002"   # 东单
GEOADD shops 116.337985 39.891675 "shop_003"   # 西单

# 查询附近 5 公里内的商家
GEORADIUS shops 116.397128 39.916527 5 km WITHCOORD WITHDIST COUNT 10

# 查询两点间距离
GEODIST shops shop_001 shop_002 km

# 获取商家的 GeoHash
GEOHASH shops shop_001
```

**Geo 底层原理**：Geo 类型底层使用 ZSet 存储，将经纬度编码为 GeoHash 数值作为 score 排序。GeoHash 是一种将二维经纬度编码为一维字符串的算法，编码越长精度越高。

### 4. （腾讯）如何用 Redis 实现 UV 统计？
> 大规模 UV 统计使用 HyperLogLog，占用极小内存。

```bash
# 页面 UV 统计
PFADD page:uv:20260722 user001 user002 user003
PFADD page:uv:20260722 user004 user005

# 获取 UV 值
PFCOUNT page:uv:20260722

# 合并多天的 UV
PFMERGE page:uv:202607 page:uv:20260722 page:uv:20260723
PFCOUNT page:uv:202607
```

- **HyperLogLog 特点**：
  - 标准误差 0.81%，占用固定 12KB 内存
  - 可以统计 2^64 个元素的基数
  - 无法精确统计，但误差可控
- **适用场景**：日活/月活 UV、页面访问去重计数

> ⚠️ 如果要求精确计数（如订单数），使用 Set 或 String 自增；如果允许小误差（UV），使用 HyperLogLog。

### 5. Redis 实现消息队列的几种方式对比？
> Redis 提供了三种消息队列实现方式。

| 方式 | 优点 | 缺点 | 适用场景 |
|------|------|------|---------|
| **List**（LPUSH/BRPOP） | 简单可靠，支持阻塞读取 | 不支持多消费者、ACK | 简单任务队列 |
| **Pub/Sub** | 支持多对多通信 | 不持久化，连接断开即丢失 | 实时通知、广播 |
| **Stream**（5.0+） | 持久化、消费者组、ACK、回溯 | 实现较复杂 | 完善的消息队列需求 |

```bash
# List 方式
LPUSH queue task               # 生产
BRPOP queue 0                  # 消费（阻塞等待）

# Stream 方式（推荐）
XADD mystream * field1 value1 field2 value2    # 生产
XREAD COUNT 10 BLOCK 0 STREAMS mystream 0      # 消费
XGROUP CREATE mystream group1 $                # 创建消费者组
XREADGROUP GROUP group1 consumer1 COUNT 1 BLOCK 2000 STREAMS mystream >  # 消费者组消费
```

> 🎯 面试常问：Redis Stream vs Kafka？回答要点：Kafka 适合大数据量、需要分区、长期存储的场景；Stream 适合轻量级、低延迟、Redis 生态内的场景。

### 6. （字节）如何用 Redis 实现用户签到和连续签到统计？
> BitMap 是实现签到功能的最佳工具。

```bash
# 用户 1001 在 7 月第 3 天签到
SETBIT sign:1001:202607 2 1    # 第 3 天（index 从 0 开始）
SETBIT sign:1001:202607 3 1    # 第 4 天
SETBIT sign:1001:202607 4 1    # 第 5 天

# 查询用户是否签到
GETBIT sign:1001:202607 2

# 查询本月签到天数
BITCOUNT sign:1001:202607

# 查询连续签到天数（从今天往前数连续的 1）
-- 需要编程方式判断，可利用位图计算
```

**连续签到实现**（Java 伪代码）：
```java
public int getContinuousSignDays(long userId, int year, int month) {
    String key = "sign:" + userId + ":" + year + String.format("%02d", month);
    byte[] bits = redis.get(key); // 获取位图数据
    int count = 0;
    for (int i = 31; i >= 0; i--) {
        if ((bits[i / 8] >> (7 - i % 8) & 1) == 1) {
            count++;
        } else {
            break; // 遇到未签到的就停止
        }
    }
    return count;
}
```

### 7. （阿里）大 key/热 key 问题如何发现和解决？
> 大 key 和热 key 是 Redis 运维中最常见的两类问题。

**大 key 问题**：
- **判断标准**：String > 10KB、集合 > 5000 元素
- **排查方法**：`redis-cli --bigkeys`、`DEBUG OBJECT key`
- **解决方案**：
  - 拆分大 key（Hash 拆成多个小 Hash）
  - 压缩/序列化
  - 本地缓存 + Redis

**热 key 问题**：
- **表现**：某个 key 的 QPS 极高，导致单节点 CPU/带宽打满
- **排查方法**：`redis-cli --hotkeys`（4.0+）、monitor 命令
- **解决方案**：
  - 本地缓存（Caffeine 等）
  - 读写分离（从节点分担读压力）
  - 散列热 key（加随机后缀拆分为多个 key）

> 💡 监控大 key 和热 key 是 Redis 运维的日常功课，建议在 Redis 配置中开启慢查询日志辅助定位。

---

## 四、手写代码题

### 1. 使用 Jedis 实现 Redis 分布式锁
```java
public class RedisDistributedLock {
    private final Jedis jedis;
    private final String lockKey;
    private final String requestId;
    private final int expireTime;

    public boolean tryLock() {
        // SET key value NX EX expireTime
        String result = jedis.set(lockKey, requestId, 
                                    SetParams.setParams().nx().ex(expireTime));
        return "OK".equals(result);
    }

    public boolean unlock() {
        // Lua 脚本：原子性检查并删除
        String script = 
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "   return redis.call('del', KEYS[1]) " +
            "else " +
            "   return 0 " +
            "end";
        Long result = (Long) jedis.eval(script, 
                                        Collections.singletonList(lockKey), 
                                        Collections.singletonList(requestId));
        return result == 1L;
    }
}
```

### 2. 使用 Spring Data Redis 操作五种数据类型
```java
@Service
public class RedisOperationService {
    @Autowired
    private StringRedisTemplate redisTemplate;

    // String 操作
    public void stringOps() {
        redisTemplate.opsForValue().set("key", "value", 10, TimeUnit.SECONDS);
        redisTemplate.opsForValue().increment("counter", 1);
    }

    // Hash 操作
    public void hashOps() {
        redisTemplate.opsForHash().put("user:1", "name", "张三");
        redisTemplate.opsForHash().put("user:1", "age", "25");
        Map<Object, Object> user = redisTemplate.opsForHash().entries("user:1");
    }

    // List 操作
    public void listOps() {
        redisTemplate.opsForList().leftPush("messages", "msg1");
        redisTemplate.opsForList().rightPop("messages");
    }

    // Set 操作
    public void setOps() {
        redisTemplate.opsForSet().add("tags", "java", "redis");
        Set<String> tags = redisTemplate.opsForSet().members("tags");
    }

    // ZSet 操作
    public void zsetOps() {
        redisTemplate.opsForZSet().add("leaderboard", "user1", 100);
        Set<String> top10 = redisTemplate.opsForZSet().reverseRange("leaderboard", 0, 9);
    }
}
```

### 3. 使用 Lua 脚本保证原子性
```lua
-- check_and_set.lua
-- 检查库存并扣减，返回剩余库存或 -1
local key = KEYS[1]
local stock = tonumber(redis.call('GET', key))
local deduct = tonumber(ARGV[1])

if stock >= deduct then
    redis.call('DECRBY', key, deduct)
    return stock - deduct
else
    return -1
end
```

```java
// Java 调用 Lua 脚本
public boolean deductStock(String key, int count) {
    String luaScript = 
        "local stock = tonumber(redis.call('GET', KEYS[1])) " +
        "local deduct = tonumber(ARGV[1]) " +
        "if stock >= deduct then " +
        "   redis.call('DECRBY', KEYS[1], deduct) " +
        "   return 1 " +
        "else " +
        "   return 0 " +
        "end";
    Long result = (Long) redisTemplate.execute(
        new DefaultRedisScript<>(luaScript, Long.class),
        Collections.singletonList(key),
        String.valueOf(count)
    );
    return result == 1L;
}
```

### 4. 使用 Redis 实现滑动窗口限流
```java
public class SlidingWindowRateLimiter {
    private final StringRedisTemplate redisTemplate;

    public boolean isAllowed(String key, int maxCount, long windowSeconds) {
        long now = System.currentTimeMillis();
        String windowKey = "rate_limit:" + key;
        
        // 移除窗口外的记录
        redisTemplate.opsForZSet().removeRangeByScore(windowKey, 0, now - windowSeconds * 1000);
        
        // 统计当前窗口内的请求数
        Long count = redisTemplate.opsForZSet().zCard(windowKey);
        if (count != null && count >= maxCount) {
            return false;
        }
        
        // 添加当前请求
        redisTemplate.opsForZSet().add(windowKey, String.valueOf(now), now);
        redisTemplate.expire(windowKey, windowSeconds, TimeUnit.SECONDS);
        return true;
    }
}
```

### 5. 实现布隆过滤器
```java
public class BloomFilter {
    private final static int BITS_SIZE = 1 << 28;  // 2^28 bits
    private static final int[] SEEDS = {3, 7, 11, 13, 31, 37, 61};
    private final HashFunction[] funcs;
    private final StringRedisTemplate redisTemplate;
    private final String key;

    public boolean add(String value) {
        boolean added = false;
        for (HashFunction f : funcs) {
            int hash = f.hash(value);
            if (redisTemplate.opsForValue().setBit(key, hash % BITS_SIZE, true)) {
                added = true;
            }
        }
        return added;
    }

    public boolean mightContain(String value) {
        for (HashFunction f : funcs) {
            int hash = f.hash(value);
            if (!redisTemplate.opsForValue().getBit(key, hash % BITS_SIZE)) {
                return false;
            }
        }
        return true;
    }

    // 使用 Redis BitMap 实现的布隆过滤器
    // 误判率：根据位图大小和哈希函数数量决定，通常 < 1%
}
```

---

## 五、系统设计题

### 1. 设计一个支持高并发的秒杀系统缓存架构
> 利用 Redis 扛住秒杀峰值流量。

**架构层次**：
1. **浏览器端限流**：按钮置灰、验证码
2. **Nginx 限流**：`limit_req` 模块按 IP 限流
3. **Redis 预扣库存**：使用 Lua 脚本原子扣减库存，毫秒级响应
4. **MQ 异步落单**：扣减成功后将请求发送到 RocketMQ/Kafka
5. **MySQL 最终写入**：消费 MQ 消息创建订单

```lua
-- 秒杀 Lua 脚本
local stockKey = KEYS[1]          -- 库存 key
local userKey = KEYS[2]           -- 用户限购 key
local userId = ARGV[1]            -- 用户 ID
local limitCount = tonumber(ARGV[2])  -- 限购数量

-- 检查是否已购买
if redis.call('GET', userKey) then
    return 0  -- 已购买过
end

-- 检查库存
local stock = tonumber(redis.call('GET', stockKey))
if stock <= 0 then
    return -1  -- 库存不足
end

-- 扣减库存
redis.call('DECR', stockKey)
-- 记录购买用户
redis.call('SET', userKey, 1, 'EX', 86400)
return 1  -- 成功
```

### 2. 设计一个通用的缓存管理平台
> 企业级缓存治理平台的核心功能模块。

**功能模块**：
1. **缓存查询**：通过管理台查询任意 key 的值和 TTL
2. **缓存清理**：支持按前缀模糊批量删除 key
3. **热点监控**：自动发现热点 key 和热 key
4. **大 key 分析**：定期分析大 key 并告警
5. **慢查询分析**：分析 Redis 慢查询日志
6. **容量规划**：监控内存使用趋势，预估扩容时间
7. **自动降级**：缓存率低于阈值时自动降级

```java
// 缓存管理 API 示例
@RestController
@RequestMapping("/cache")
public class CacheController {
    @GetMapping("/keys")
    public Set<String> scanKeys(@RequestParam String pattern) {
        // 使用 SCAN 命令避免阻塞
        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
        Cursor<String> cursor = redisTemplate.scan(options);
        while (cursor.hasNext()) {
            keys.add(cursor.next());
        }
        return keys;
    }
}
```

### 3. 设计 Redis 集群的容量评估和扩容方案
> 合理的容量评估是保障 Redis 集群稳定运行的基础。

**容量评估公式**：
```
总内存 = 数据量 * (1 + 复制因子 + 内部碎片率 + RDB/AOF 开销)
- 复制因子：主从模式下数据量为 1+N（从库数量）
- 内部碎片率：约 1.1-1.2
- RDB/AOF：额外 20%-30%
```

**扩容方案**：
1. **水平扩容（Redis Cluster）**：
   - 添加新节点，迁移哈希槽
   - 平滑迁移，不需要停机
2. **垂直扩容**：
   - 升级内存更大的机器
   - 重启后从主节点同步数据

```bash
# Redis Cluster 添加节点
# 1. 启动新节点
redis-server --cluster-enabled yes --port 6380

# 2. 加入集群
redis-cli --cluster add-node 127.0.0.1:6380 127.0.0.1:6379

# 3. 迁移哈希槽
redis-cli --cluster reshard 127.0.0.1:6379
```

### 4. 设计 Redis 缓存一致性方案（MySQL + Redis）
> 缓存一致性问题一直是分布式系统中最棘手的问题之一。

**方案一：Cache Aside Pattern**
```
读：先读缓存 -> 未命中读 DB -> 回填缓存
写：先更新 DB -> 再删除缓存
```

**方案二：延迟双删**
```java
public void updateData(Long id, String data) {
    redisTemplate.delete("data:" + id);          // 先删缓存
    database.update(id, data);                   // 更新数据库
    Thread.sleep(500);                            // 延迟
    redisTemplate.delete("data:" + id);          // 再删缓存
}
```

**方案三：Canal 监听 binlog**
```
MySQL binlog -> Canal -> MQ -> 消费端同步 Redis
```

> 🎯 没有完美的强一致性方案，只能做到最终一致性。如果业务要求强一致，建议直接读数据库。

### 5. Redis 脑裂问题及解决方案
> 脑裂（Split-Brain）发生在网络分区场景下。

**场景**：主库和哨兵之间网络中断，但主库和客户端之间正常。哨兵选举了新主库，旧主库仍在写数据。网络恢复后旧主库降级为从库，数据丢失。

**Redis 提供的保护配置**：
```bash
# 最少从库数量
min-slaves-to-write 1
# 最大从库延迟
min-slaves-max-lag 10
```

**以上配置的含义**：主库至少有一个从库连接且延迟不超过 10 秒才接受写入。当网络分区时，旧主库无法与从库通信，写入被拒绝，保护数据不丢失。

**RedLock 方案**：对于极端重要的场景，使用 RedLock 算法，在多个 Redis 节点上获取锁，多数成功才算成功。

---

## 六、常见坑点与最佳实践

### 常见坑点

| 坑点 | 原因 | 解决方案 |
|------|------|---------|
| Big Key 导致阻塞 | 集合类型元素过多，操作耗时 | 拆分大 key、使用 SCAN 替代 |
| 热 Key 导致带宽打满 | 单个 key 被大量并发访问 | 本地缓存、读写分离、散列 |
| 分布式锁死锁 | 获得锁后进程崩溃未释放 | 设置过期时间、WatchDog 续期 |
| 缓存穿透 | 查询不存在的 key | 缓存空值、布隆过滤器 |
| 缓存雪崩 | 大量 key 同时过期 | 过期时间加随机数 |
| AOF 文件不断增大 | AOF 记录所有写命令 | 定期 AOF rewrite 重写 |
| RDB 执行导致阻塞 | fork 子进程或写时复制 | 避开高峰期执行 BGSAVE |
| 主从复制延迟 | 从库单线程回放跟不上 | 优化网络、避免大 Key |
| 内存碎片率过高 | 频繁修改删除数据 | 重启或开启 activedefrag |
| 使用 KEYS 命令 | 全表扫描阻塞 Redis | 使用 SCAN 替代 |

### 最佳实践

| 实践 | 说明 |
|------|------|
| Key 命名规范 | `业务:对象:实例`，如 `user:info:1001`，控制长度 |
| 设置合理过期时间 | 避免内存无限增长 |
| 使用连接池 | 避免频繁创建销毁连接 |
| 线上禁用 KEYS/FLUSHALL/FLUSHDB | 使用 SCAN/UNLINK/FLUSHALL ASYNC |
| 批量操作使用 Pipeline | 减少往返网络开销 |
| 选择合适的序列化方式 | Protobuf/JSON，避免 Java 原生序列化 |
| 控制单 Key 大小 | String < 10KB，集合 < 1w 元素 |
| 慢查询监控 | slowlog-log-slower-than 100000 (100ms) |
| 尽量用哈希结构 | 节省内存且方便管理字段 |
| 读写分离注意延迟 | 对一致性要求高的场景强制读主库 |

---

## 七、面试回答模板

### 模板1：Redis 为什么这么快？
> 面试官期望：内存 + IO 多路复用 + 单线程 + 高效数据结构

**回答结构**：
1. **纯内存**：所有数据在内存中，读写纳秒级，远快于磁盘
2. **单线程模型**：避免锁竞争和上下文切换的开销，提升 CPU 利用率
3. **IO 多路复用**：基于 epoll（Linux）的事件驱动模型，可以高效处理上万并发连接
4. **高效数据结构**：SDS、ZipList、SkipList、Dict 等数据结构都为特定场景做了极致优化
5. **RESP 协议**：二进制安全，解析效率高
6. **补充（6.0+）**：引入多线程 IO 进一步提升了网络吞吐量，但核心命令执行仍是单线程

### 模板2：Redis 持久化机制如何选择？
> 面试官期望：RDB vs AOF 对比 + 混合持久化

**回答结构**：
1. **RDB 特点**：全量快照，适合备份和快速恢复；可能丢失两次 save 之间的数据
2. **AOF 特点**：记录写命令，数据更安全；文件更大，恢复更慢
3. **混合持久化（4.0+）**：AOF rewrite 时生成 RDB 快照 + 增量 AOF 日志，兼顾恢复速度和数据安全
4. **推荐配置**：同时开启 RDB + AOF，appendfsync everysec
5. **场景选择**：如果能接受分钟级数据丢失，用 RDB；要求秒级数据安全，用 AOF；生产环境推荐混合持久化

### 模板3：Redis 的缓存三大问题及解决方案
> 面试官期望：穿透、击穿、雪崩的完整对比和解决方案

**回答结构**：
1. **缓存穿透**：查询不存在的数据 -> 缓存空值 / 布隆过滤器 / 参数校验
2. **缓存击穿**：热点 key 过期瞬间高并发 -> 互斥锁 / 逻辑过期 / 永不过期
3. **缓存雪崩**：大面积 key 同时过期或节点宕机 -> 过期时间随机化 / 多级缓存 / 限流降级 / 高可用
4. **对比总结**：穿透是"不存在"的数据，击穿是"热点 key 过期"，雪崩是"大面积过期"

### 模板4：分布式锁如何实现？
> 面试官期望：单机 Redis 锁 + Redisson + RedLock 的完整演进

**回答结构**：
1. **基础实现**：`SET key value NX EX 30`，保证原子性加锁
2. **释放锁**：使用 Lua 脚本保证 GET + DEL 原子性，防止误删
3. **存在的问题**：过期时间不好设置，业务没执行完锁过期了
4. **Redisson 优化**：WatchDog 自动续期、可重入锁、信号量
5. **跨节点场景**：RedLock 算法在多个 Redis 节点上获取锁，多数成功才算成功
6. **面试追问**：RedLock 是否真的安全？学术界有争议，Martin Kleppmann 认为 RedLock 不够安全，但生产环境仍广泛使用

### 模板5：Redis 主从、哨兵、集群的区别和选择？
> 面试官期望：三种高可用方案的对比

**回答结构**：
1. **主从复制**：一个主库多个从库，数据异步同步。解决读扩展问题，但主库故障需要手动切换
2. **哨兵模式**：在主从基础上增加了自动故障转移。解决了高可用问题，但没有解决单节点存储上限问题
3. **Redis Cluster**：去中心化分布式集群，分片存储数据。解决了海量数据存储和高可用问题
4. **选择建议**：
   - 数据量 < 10GB，简单高可用 -> 主从 + 哨兵
   - 数据量 > 10GB，需要水平扩展 -> Redis Cluster
   - 对一致性要求极高 -> 考虑其他方案（如 Codis）

---

## 八、快速查漏补缺 Checklist

- [ ] Redis 数据类型及底层实现（SDS, ZipList, SkipList, QuickList, Dict）
- [ ] String 类型常用命令（SET/GET/INCR/EXPIRE/SETNX/SETEX）
- [ ] Hash 类型常用命令（HSET/HGET/HGETALL/HDEL）
- [ ] List 类型常用命令（LPUSH/RPOP/BRPOP/LLEN）
- [ ] Set 类型常用命令（SADD/SMEMBERS/SINTER/SUNION/SDIFF）
- [ ] ZSet 类型常用命令（ZADD/ZRANK/ZREVRANK/ZRANGE/ZREVRANGE）
- [ ] Geo 类型常用命令（GEOADD/GEORADIUS/GEODIST）
- [ ] HyperLogLog 常用命令（PFADD/PFCOUNT/PFMERGE）
- [ ] BitMap 常用命令（SETBIT/GETBIT/BITCOUNT/BITOP）
- [ ] Stream 常用命令（XADD/XREAD/XGROUP/XREADGROUP）
- [ ] 过期策略：定期删除 + 惰性删除
- [ ] 内存淘汰策略：8 种策略及适用场景
- [ ] 持久化：RDB vs AOF 对比
- [ ] 主从复制：全量同步 + 增量同步原理
- [ ] 哨兵：监控 + 自动故障转移 + 通知
- [ ] 集群：16384 哈希槽 + CRC16 + MOVED/ASK 重定向
- [ ] 缓存穿透、击穿、雪崩的解决方案
- [ ] 分布式锁实现：SET NX EX + Lua + Redisson
- [ ] 缓存一致性：Cache Aside + 延迟双删 + Canal
- [ ] 渐进式 rehash 原理
- [ ] I/O 多路复用 + 单线程模型
- [ ] 大 key / 热 key 问题排查和解决
- [ ] Redis 6.0 多线程模型（网络 IO 多线程）
- [ ] Lua 脚本实现原子性操作
- [ ] Pipeline 批量操作优化
- [ ] Redis 事物与 MySQL 事务的区别
