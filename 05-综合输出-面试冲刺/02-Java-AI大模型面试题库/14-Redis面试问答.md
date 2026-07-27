# Redis 面试问答清单
> 🎯 基于项目实战清单，涵盖面试高频问题与完美解答方案，帮助你在面试中脱颖而出。

## 目录
1. [基础概念与核心原理](#1-基础概念与核心原理)
2. [项目实战深度问答](#2-项目实战深度问答)
3. [进阶与系统设计](#3-进阶与系统设计)
4. [场景题与故障排查](#4-场景题与故障排查)

---

## 1. 基础概念与核心原理

> 💡 面试官在这一环节考察你的基本功是否扎实

### Q1：Redis 的五大数据结构分别适用于什么业务场景？
**面试官意图：** 考察对 Redis 基本数据结构的理解深度，以及能否在实际业务中做出正确选型。

**完美解答：**

Redis 的五大数据结构各有其典型应用场景：

| 数据结构 | 底层实现 | 核心特性 | 典型业务场景 |
|----------|---------|---------|-------------|
| **String** | SDS（简单动态字符串） | 最基础、原子操作、过期时间 | 验证码存储、计数器、分布式 ID、缓存对象（JSON序列化） |
| **Hash** | 压缩列表 / 哈希表 | 字段级操作、内存紧凑 | 用户信息、购物车、商品详情 |
| **List** | 双向链表 / 压缩列表 | 消息顺序性、队列特性 | 消息队列、最新动态、时间线 |
| **Set** | 哈希表 | 元素唯一、集合运算 | 好友列表、共同关注、去重、抽奖 |
| **ZSet** | 跳表 + 哈希表 | 排序、按分数范围查询 | 排行榜、延时队列、优先级队列 |

举例来说，**ZSet** 实现排行榜：`ZADD leaderboard 100 user1` 添加用户分数，`ZREVRANGE leaderboard 0 9 WITHSCORES` 获取 Top10，`ZINCRBY leaderboard 10 user1` 实时更新分数——全部是 O(logN) 操作，性能极高。

> 💡 **核心选型原则**：需要排序用 ZSet，需要唯一去重用 Set，需要字段级操作用 Hash，需要队列用 List，其余通用场景用 String。

**延伸追问应对：** 如果被问到底层数据结构细节，需准备 SDS 的结构（预分配空间、惰性空间释放）、跳表的查询过程（多层索引、时间复杂度 O(logN)）、压缩列表与哈希表的转换条件。

---

### Q2：解释缓存穿透、缓存击穿、缓存雪崩的区别与解决方案
**面试官意图：** 这是 Redis 面试的"三大问题"，几乎必问，考察对缓存异常场景的理解和解决能力。

**完美解答：**

| 问题 | 现象 | 原因 | 解决方案 |
|------|------|------|---------|
| **缓存穿透** | 缓存和数据库都没有数据，请求直接打到 DB | 恶意请求查询不存在的数据 | ① 布隆过滤器 ② 缓存空值（设置短过期时间） |
| **缓存击穿** | 缓存有数据，但某个热点 key 过期，大量请求同时打 DB | 热点 key 过期 + 高并发 | ① 互斥锁（SETNX） ② 逻辑过期（不设物理过期） |
| **缓存雪崩** | 大量 key 同时过期，DB 被打垮 | 同一时间大规模 key 过期 | ① 随机过期时间 ② 多级缓存 ③ 熔断降级 |

**场景举例**：
- **穿透**：用户请求一个不存在的商品 ID，每次都穿透到数据库。通过在 Redis 中缓存空值（过期时间 30 秒）或使用布隆过滤器预存所有合法 ID 来解决。
- **击穿**：某个爆款商品的缓存过期，上万请求同时涌入。使用互斥锁 `SETNX lock_key value NX EX 10`，只有一个线程能查 DB 回写缓存，其他线程等待或降级。
- **雪崩**：大量商品设置了相同的过期时间（如凌晨 0 点）。在设置过期时间时加上随机值：`SET key value EX 3600 + random(60,600)`。

**延伸追问应对：** 面试官可能会问布隆过滤器的原理——它使用多个哈希函数映射到 bit 数组，判断"不存在"是 100% 准确的，判断"存在"有误判率。可以用 `Redisson` 的 `RBloomFilter` 组件。

---

### Q3：Redis 的持久化机制 RDB 和 AOF 的区别是什么？
**面试官意图：** 考察对 Redis 数据安全的理解，以及能否根据业务场景选择合适的持久化方案。

**完美解答：**

| 对比维度 | RDB | AOF |
|----------|-----|-----|
| **原理** | 生成数据快照（二进制） | 记录写操作命令（文本） |
| **文件大小** | 小 | 大（可重写压缩） |
| **恢复速度** | 快 | 慢 |
| **数据安全性** | 可能丢失最后一次备份后的数据 | 根据 fsync 策略，最多丢失 1 秒数据 |
| **性能影响** | fork 子进程，可能阻塞主进程 | 写时追加，影响较小 |
| **适用场景** | 备份、灾备、快速恢复 | 数据安全性要求高 |

**推荐方案**：Redis 4.0 之后建议使用**混合持久化**（`aof-use-rdb-preamble yes`）。AOF 重写时生成 RDB 格式的头部，后续增量命令用 AOF 格式，结合两者优点——既有 RDB 的快速恢复能力，又有 AOF 的数据安全性。

> ⚠️ **注意**：生产环境中千万别只用默认配置。如果允许少量数据丢失，用 RDB 即可；如果对数据安全要求极高（如订单相关），必须开启 AOF + 每秒 fsync。

---

## 2. 项目实战深度问答

> 💡 面试官会深挖你的项目细节，验证你"真的做过"而不是"背过"

### Q4：描述一个你使用 Redis 实现分布式锁的完整方案，需要注意哪些坑？
**面试官意图：** 分布式锁是 Redis 面试的"必考题"，考察对并发控制、原子性、可靠性等关键点的掌握程度。

**完美解答：**

我曾在秒杀系统中使用 Redis 实现分布式锁，完整的方案如下：

**加锁（使用 SET 命令的扩展参数，保证原子性）：**
```bash
SET lock:product:1001 ${requestId} NX EX 30
```
- `NX`：只有在 key 不存在时才设置，实现互斥
- `EX 30`：自动过期，防止死锁
- `requestId`：唯一标识（如 UUID），用于安全释放锁

**解锁（使用 Lua 脚本保证原子性）：**
```lua
-- 释放锁，先判断是否自己的锁，防止误删别人的锁
if redis.call("get", KEYS[1]) == ARGV[1] then
    return redis.call("del", KEYS[1])
else
    return 0
end
```
> ⚠️ **一定要用 Lua 脚本**，不能先 GET 再 DEL，因为 GET 和 DEL 不是原子操作，会出现线程切换导致误删锁。

**核心坑点及解决方案：**

| 坑点 | 问题描述 | 解决方案 |
|------|----------|---------|
| **死锁** | 加锁后程序崩溃，锁无法释放 | 设置过期时间（EX）兜底 |
| **锁误删** | 线程 A 的锁过期后被线程 B 获取，A 执行完误删 B 的锁 | 使用 requestId 校验 |
| **锁续期** | 业务执行时间超长，锁自动过期 | Redisson WatchDog 自动续期 |
| **可重入** | 同一个线程需要多次获取同一把锁 | 使用 Redisson 可重入锁或自己维护计数器 |
| **主从切换** | master 宕机，锁未同步到 slave | Redlock 算法（但 Redlock 有争议，多数场景不需要） |

**代码展示（使用 Redisson，推荐生产使用）：**
```java
// Redisson 自动处理了加锁、续期、解锁的所有细节
RLock lock = redissonClient.getLock("lock:product:" + productId);
try {
    // 尝试加锁，最多等 10 秒，30 秒自动释放
    if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
        // 业务逻辑，不需要手动续期，WatchDog 自动处理
        doBusiness();
    }
} finally {
    if (lock.isHeldByCurrentThread()) {
        lock.unlock();
    }
}
```

---

### Q5：如果让你设计一个秒杀系统，Redis 在其中扮演什么角色？
**面试官意图：** 考察综合架构能力，看你能不能把 Redis 的多个特性组合起来解决真实的高并发问题。

**完美解答：**

秒杀系统是 Redis 综合能力的试金石，Redis 在其中承担了四个关键角色：

**1. 缓存预热 + 库存预扣**
```java
// 系统启动时加载库存到 Redis
stringRedisTemplate.opsForValue().set("stock:1001", "100");

// 秒杀时原子扣减库存
Long remain = stringRedisTemplate.opsForValue().decrement("stock:1001");
if (remain < 0) {
    stringRedisTemplate.opsForValue().increment("stock:1001"); // 恢复
    return "已售罄";
}
```

**2. 分布式锁防超卖**
使用 Redisson 的 RLock 保证库存扣减的原子性，防止并发扣减导致库存为负。

**3. 限流削峰**
使用滑动窗口算法限制用户请求频率：
```bash
# 用户级别的限流：10秒内最多请求5次
INCR seckill:limit:{userId}:{timeWindow}
EXPIRE seckill:limit:{userId}:{timeWindow} 10
```

**4. 消息队列缓冲**
将秒杀请求放入 Redis List，后端 worker 异步消费，防止瞬时流量打垮数据库：
```bash
# 生产者：秒杀请求入队
LPUSH seckill_queue {userId}:{productId}

# 消费者：异步处理
BRPOP seckill_queue 0
```

> 💡 **核心设计理念**：Redis 做最前面的流量入口和状态管理，MySQL 做最终的持久化。80% 的请求在 Redis 层就被过滤或处理，只有真正成功的请求才落到数据库。

---

### Q6：你在项目中如何实现接口限流？详细说明限流算法的实现
**面试官意图：** 考察对限流算法的理解深度和工程实现能力，这是保护后端系统的重要手段。

**完美解答：**

我实现过一个基于 Redis 注解驱动的限流框架，支持三种算法：

**1. 计数器算法（简单但存在临界问题）**
```bash
# 每个用户每分钟最多 10 次请求
INCR rate:limit:{userId}:minute
EXPIRE rate:limit:{userId}:minute 60
```
> ⚠️ 缺陷：在窗口切换时刻可能出现双倍流量（前窗口末尾 + 后窗口开头）

**2. 滑动窗口算法（更精确，推荐）**
使用 ZSet 记录请求时间戳，统计窗口内的请求数量：
```java
public boolean slidingWindowTryAcquire(String key, int maxCount, long windowSeconds) {
    long now = System.currentTimeMillis();
    long windowStart = now - windowSeconds * 1000;

    // 移除窗口外的数据
    redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
    // 添加当前请求
    redisTemplate.opsForZSet().add(key, String.valueOf(now), now);
    // 设置过期时间
    redisTemplate.expire(key, windowSeconds * 2, TimeUnit.SECONDS);
    // 统计窗口内请求数
    Long count = redisTemplate.opsForZSet().zCard(key);

    return count <= maxCount;
}
```

**3. 令牌桶算法（支持突发流量）**
使用 Lua 脚本实现令牌的匀速生产和限流判断：
```lua
-- 令牌桶 Lua 脚本
local key = KEYS[1]
local rate = tonumber(ARGV[1])      -- 每秒生成令牌数
local capacity = tonumber(ARGV[2])  -- 桶容量
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4]) -- 请求的令牌数

local lastTime = redis.call("hget", key, "last_time") or now
local tokens = redis.call("hget", key, "tokens") or capacity

-- 计算时间差生成的令牌数
local elapsed = math.max(0, now - lastTime)
tokens = math.min(capacity, tokens + elapsed * rate)

if tokens >= requested then
    redis.call("hset", key, "tokens", tokens - requested)
    redis.call("hset", key, "last_time", now)
    return 1 -- 放行
else
    return 0 -- 限流
end
```

**注解封装（面向切面）：**
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    String key() default "";
    int maxCount() default 10;
    int timeWindow() default 60;
    TimeUnit unit() default TimeUnit.SECONDS;
    String algorithm() default "sliding_window";
}

// 使用方式
@RateLimit(key = "submitOrder", maxCount = 100, timeWindow = 1)
public Result submitOrder(OrderRequest request) {
    // 业务逻辑
}
```

---

## 3. 进阶与系统设计

> 💡 这是拉开差距的环节，考察架构思维

### Q7：Redis 的主从复制、哨兵和集群三种架构有什么区别？如何选型？
**面试官意图：** 考察对 Redis 高可用方案的理解深度，以及在不同业务场景下的架构选型能力。

**完美解答：**

| 架构模式 | 核心能力 | 自动故障转移 | 数据分片 | 读写分离 | 节点数要求 | 适用场景 |
|----------|---------|:----------:|:--------:|:--------:|:----------:|---------|
| **主从复制** | 数据备份、读写分离 | 否 | 否 | 是 | 最少 2 个 | 小型应用、数据备份 |
| **哨兵模式** | 自动故障转移、高可用 | 是 | 否 | 是 | 3+ 节点 | 中小规模、高可用要求 |
| **集群模式** | 自动分片、高可用、水平扩展 | 是 | 是 | 是 | 最少 3 主 3 从 | 大规模、海量数据 |

**选型建议：**
- **10G 以内数据 + 高可用**：哨兵模式，部署简单，管理方便
- **100G+ 海量数据**：集群模式，通过分片把数据分散到多台机器
- **数据量小且允许宕机恢复**：主从复制，最低成本

> 💡 **集群的优势**：Cluster 模式下，Redis 自动将 key 通过 CRC16 算法哈希到 16384 个槽位，分配到不同节点，实现了数据的水平分片。扩容时只需重新分配槽位，对业务几乎无感。

---

### Q8：如何设计 Redis 缓存更新策略？Cache Aside 模式有什么优缺点？
**面试官意图：** 考察缓存一致性问题的理解和解决方案，这是缓存设计中最重要的设计决策。

**完美解答：**

**Cache Aside 模式是目前最通用的缓存更新策略：**
```
读操作：
  1. 读 Redis 缓存 → 有则返回
  2. 无则读 MySQL → 回写 Redis → 返回

写操作：
  1. 更新 MySQL
  2. 删除 Redis 缓存（不是更新！）
```

> ⚠️ **为什么不更新缓存而是删除？** 因为更新缓存有并发问题：线程 A 更新 MySQL 后更新缓存，但线程 B 在中间读到了旧数据，会导致缓存中一直是旧值。删除缓存后，下次读取时自然重新加载，保证一致性。

**为什么先更新 DB 再删缓存，而不是先删缓存再更新 DB？**
- 先删缓存再更新 DB：线程 A 删缓存后，线程 B 读缓存没命中，查 DB 得到旧数据并回写缓存，导致缓存再次变成旧值。
- 先更新 DB 再删缓存：更新 DB 后，即使有线程读到了旧缓存，下一次也会重新加载。

**Cache Aside 的不足：**
| 问题 | 场景 | 解决方式 |
|------|------|---------|
| 最终一致性 | 删缓存失败 | 延迟双删 + 重试机制 |
| 写后立刻读 | A 更新 DB 后删缓存，但 B 在删除前读到了旧缓存 | 同步懒加载，业务上可接受 |
| 热点 key 频繁失效 | 频繁更新导致缓存频繁失效，DB 压力大 | 旁路更新 + 异步刷新 |

**兜底方案——延迟双删：**
```java
public void updateData(String key, Object data) {
    redisTemplate.delete(key);          // 第一次删缓存
    mysqlDao.update(data);               // 更新数据库
    Thread.sleep(500);                   // 休眠 500ms
    redisTemplate.delete(key);          // 第二次删缓存
}
```

---

### Q9：Redis 支持哪些数据淘汰策略？如何选择？
**面试官意图：** 考察对 Redis 内存管理机制的理解，以及在实际场景中的配置能力。

**完美解答：**

Redis 在内存达到 `maxmemory` 上限时，根据 `maxmemory-policy` 策略淘汰数据：

| 策略 | 作用范围 | 淘汰逻辑 | 适用场景 |
|------|---------|---------|---------|
| `noeviction` | - | 不淘汰，返回 OOM 错误 | 禁止丢失数据的场景 |
| `allkeys-lru` | 所有 key | 淘汰最近最少使用 | **最常用**，通用场景 |
| `volatile-lru` | 设置了 TTL 的 key | 淘汰最近最少使用 | 缓存和持久化混合使用 |
| `allkeys-lfu` | 所有 key | 淘汰最不频繁使用 | 访问频次差异很大的场景 |
| `volatile-ttl` | 设置了 TTL 的 key | 淘汰即将过期的 | 对过期时间敏感的场景 |
| `allkeys-random` | 所有 key | 随机淘汰 | 缓存数据访问均匀的场景 |

**生产推荐**：**`allkeys-lru`** 是最通用、最安全的选择。如果业务中有冷热数据差异明显（大多数场景），LRU 策略表现最优。

> 💡 **LFU vs LRU**：LRU 适合"最新数据更可能被访问"的场景，LFU 适合"高频数据更可能被访问"的场景。例如新闻网站用 LFU 更好，因为热点新闻会被反复访问。

---

## 4. 场景题与故障排查

> 💡 考察实际解决问题的能力

### Q10：如果 Redis 出现大量慢查询，你如何排查和优化？
**面试官意图：** 考察 Redis 性能调优的实际经验。

**完美解答：**

**排查步骤：**

1. **开启慢查询日志**
```bash
# 设置慢查询阈值为 50 毫秒（生产推荐 10-50ms）
CONFIG SET slowlog-log-slower-than 50000
# 设置最多保留 1000 条慢日志
CONFIG SET slowlog-max-len 1000

# 查看慢查询
SLOWLOG GET 10
```

2. **分析慢查询原因及优化方案**

| 常见原因 | 具体表现 | 优化方案 |
|----------|---------|---------|
| **大 key 操作** | 操作一个包含百万元素的 Hash/Set/ZSet | 拆分大 key、使用 `HSCAN`/`SSCAN` 分批操作 |
| **批量 `keys` 命令** | 全量扫描所有 key | 使用 `SCAN` 代替 `KEYS`（游标式分批扫描） |
| **复杂聚合操作** | `SORT`、`ZUNIONSTORE`、`ZINTERSTORE` | 业务层缓存结果，或用 Lua 脚本拆分 |
| **大 value 读写** | value 超过 10MB | 压缩 value、拆分字段、用 Hash 代替 String |
| **内存淘汰频繁** | 淘汰策略消耗 CPU | 检查内存使用率，提前扩容或优化数据量 |

3. **使用 `redis-cli` 大 key 分析**
```bash
# 使用 redis-cli 自带的 bigkeys 扫描
redis-cli --bigkeys
```

> ⚠️ **线上执行 `KEYS` 命令是大忌**！千万级 key 的 `KEYS *` 会阻塞 Redis 几十秒，导致所有操作超时。一律用 `SCAN` 代替。

---

### Q11：线上 Redis 内存持续增长，你如何分析并解决？
**面试官意图：** 考察线上问题排查流程和内存优化能力。

**完美解答：**

**排查流程：**

1. **查看内存使用情况**
```bash
INFO memory
# 重点关注：
# used_memory：Redis 分配的内存
# used_memory_rss：操作系统看到的 Redis 内存
# mem_fragmentation_ratio：内存碎片率
```

2. **找到大内存消耗者**
```bash
# 查看所有 key 的大小分布
redis-cli --bigkeys

# 查看单个 key 的内存占用
MEMORY USAGE key_name

# 查看过期 key 的数量
INFO keyspace
```

3. **常见内存泄漏原因和解决方案**

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| **无过期时间 key 堆积** | key 没有设置 TTL，永久占用内存 | `redis-cli -n 0 keys "*" | while read key; do redis-cli -n 0 TTL $key; done` 检查无过期 key |
| **内存碎片高** | mem_fragmentation_ratio > 1.5 | 重启 Redis 或使用 `ACTIVE DEFRAG`（4.0+） |
| **大 value** | 单个 value 超过 1MB | 压缩、拆分、Hash 结构替代 String |
| **Hash 编码不紧凑** | 小 Hash 未使用 ziplist | 配置 `hash-max-ziplist-entries` 和 `hash-max-ziplist-value` |

4. **优化实操**
```bash
# 方式一：设置合理的过期策略
# 业务代码中所有缓存 key 必须设置 TTL
redisTemplate.opsForValue().set(key, value, 1, TimeUnit.HOURS);

# 方式二：主动淘汰
# 删除一批确定无用的 key
UNLINK key1 key2 key3  # 异步删除，不阻塞

# 方式三：切换淘汰策略
CONFIG SET maxmemory-policy allkeys-lru
CONFIG SET maxmemory 4gb
```

---

### Q12：如果秒杀活动中 Redis 宕机了，如何保证数据不丢失且系统可用？
**面试官意图：** 考察灾备意识和系统设计的鲁棒性。

**完美解答：**

**分层保证方案：**

**第一层：本地缓存兜底**
```java
// 使用 Caffeine 本地缓存作为 Redis 的备用层
@Bean
public Cache<String, Integer> localCache() {
    return Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(1, TimeUnit.SECONDS)
        .build();
}

public Integer getStock(Long productId) {
    // 1. 查本地缓存
    Integer stock = localCache.getIfPresent("stock:" + productId);
    if (stock != null) return stock;

    // 2. 查 Redis 缓存
    stock = redisTemplate.opsForValue().get("stock:" + productId);
    if (stock != null) {
        localCache.put("stock:" + productId, stock);
        return stock;
    }

    // 3. 查 MySQL
    return mysqlDao.getStock(productId);
}
```

**第二层：降级策略**
```java
public Result seckill(Long userId, Long productId) {
    try {
        // 正常流程：Redis 扣库存
        return doSeckillWithRedis(userId, productId);
    } catch (RedisException e) {
        // 降级策略：直接降级到数据库
        log.warn("Redis 异常，降级到数据库模式", e);
        return doSeckillWithDB(userId, productId);
    }
}
```

**第三层：数据恢复**
- 利用 AOF + RDB 持久化文件恢复
- 从 MySQL 恢复库存数据到 Redis
- 补偿机制：对无法确认的订单进行对账和补发

**核心原则：**
> 🎯 **Redis 宕机不能导致业务不可用，只能导致性能下降。** Redis 是缓存层，不是数据唯一存储层。库存数据在 MySQL 中有最终备份，Redis 宕机后系统应平滑降级，等 Redis 恢复后自动切回。

---

### Q13：Redis 的热点 key 问题如何发现和解决？
**面试官意图：** 考察对热点问题的认知深度和实际的治理经验，这是大流量场景下的常见难题。

**完美解答：**

**发现问题：**

1. **客户端统计**：在 Redis 客户端侧统计 key 的访问频率
2. **代理层监控**：如果使用 Twemproxy/Redis Proxy，在代理层采集
3. **热 key 自动发现**
```bash
# 使用 Redis-cli 的 hotkeys 参数（4.0+）
redis-cli --hotkeys
```

**解决方案：**

| 方案 | 原理 | 优缺点 |
|------|------|--------|
| **本地缓存拆解** | 将热点 key 的数据缓存到应用层本地缓存 | 实现简单，但存在一致性问题 |
| **key 打散** | 将单个 key 拆分为多个副本，分散读压力 | 适合读多写少的场景 |
| **读写分离** | 主节点写，从节点读 | 存在数据延迟 |
| **节点扩容** | 增加集群节点，分散流量 | 需要重新分片 |

**Key 打散示例：**
```java
// 将热点 key 拆分为 10 个副本
public String getHotData(String key) {
    int replica = ThreadLocalRandom.current().nextInt(10);
    String replicaKey = key + ":" + replica;
    String value = redisTemplate.opsForValue().get(replicaKey);
    if (value == null) {
        // 使用分布式锁保证只有一个线程更新所有副本
        String lockKey = "lock:" + key;
        if (lock.tryLock(lockKey)) {
            try {
                value = loadFromDB(key);
                for (int i = 0; i < 10; i++) {
                    redisTemplate.opsForValue().set(key + ":" + i, value, 1, TimeUnit.HOURS);
                }
            } finally {
                lock.unlock(lockKey);
            }
        }
    }
    return value;
}
```

---

## 💎 面试加分金句

- "Redis 的单线程模型曾被认为是性能瓶颈，但正是单线程配合 IO 多路复用，避免了锁竞争，在纯内存操作场景下能达到 10W+ QPS。瓶颈在于网络 IO 和内存带宽，而非 CPU。"
- "分布式锁的核心不在于加锁，而在于解锁的安全性——绝对不能误删别人的锁。Lua 脚本的原子性校验是必须的。"
- "处理缓存击穿时我倾向于使用逻辑过期方案而不是互斥锁，因为互斥锁可能导致大量线程阻塞，而逻辑过期允许每个线程获取到旧数据并异步刷新，对用户体验影响更小。"
- "Redis 集群的 16384 个槽位设计不是偶然的——这是经过压缩的 CRC16 结果，保证了哈希的均匀性，同时在 Gossip 协议传播中占用的带宽最小。"
- "缓存一致性没有银弹，Cache Aside + 延迟双删 + 最终一致性回查是我在电商场景中使用的最佳实践组合。"

## 📋 高频追问清单

| 追问方向 | 应对策略 |
|----------|----------|
| Redis 单线程为什么还那么快？ | IO 多路复用 + 纯内存操作 + 避免上下文切换 |
| 如何保证 Redis 和 MySQL 的数据一致性？ | Cache Aside 模式 + 延迟双删 + 最终一致性补偿 |
| Redis 的过期 key 是怎么删除的？ | 惰性删除 + 定时删除（每 100ms 采样） |
| Redis 事务和 Lua 脚本的区别？ | Lua 脚本在 Redis 中是原子执行的，事务的 EXEC 才执行 |
| 怎么设计一个分布式锁的 Redlock 方案？ | 需要在多数节点（5 取 3）加锁成功才算成功 |
| Redis 的 pipeline 有什么作用？ | 批量发送命令减少 RTT，但不保证原子性 |
| 为什么要用 Redis 而不用本地缓存？ | 分布式系统需要统一缓存，本地缓存会数据不一致 |
| Redis 的缓存穿透布隆过滤器误判率怎么控制？ | 调整哈希函数数量和位数组大小 |

## 🔗 关联知识点

- [MySQL必做项目清单-面试问答](#) — 数据库缓存一致性方案对比
- [RabbitMQ必做项目清单-面试问答](#) — 消息队列 + Redis 协同的秒杀架构
- [Docker必做项目清单-面试问答](#) — Redis 容器化部署最佳实践
