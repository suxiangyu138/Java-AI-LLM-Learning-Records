# Redis 缓存实战与常见问题
> 把 Redis 用好不只是 SET/GET，更要解决穿透、击穿、雪崩、一致性四大缓存经典问题，掌握正确的缓存模式。

## 目录
1. [Cache Aside 模式](#1-cache-aside-模式)
2. [缓存穿透](#2-缓存穿透)
3. [缓存击穿](#3-缓存击穿)
4. [缓存雪崩](#4-缓存雪崩)
5. [缓存一致性](#5-缓存一致性)
6. [缓存淘汰策略](#6-缓存淘汰策略)
7. [大 Key 与热 Key](#7-大-key-与热-key)
8. [缓存问题对比总结](#8-缓存问题对比总结)

---

## 1. Cache Aside 模式

最常用的缓存模式，也叫旁路缓存（Cache Aside Pattern）。核心原则：缓存只作为旁路加速，不参与业务主流程。

### 1.1 读流程

```text
1. 读 Redis 缓存
   ├── 命中 → 直接返回
   └── 未命中 → 读数据库 → 回写 Redis → 返回
```

```java
public User getUser(Long userId) {
    String key = "user:" + userId;
    User user = (User) redisTemplate.opsForValue().get(key);
    if (user != null) return user;       // 命中

    user = userMapper.selectById(userId); // 查 DB
    if (user != null) {
        redisTemplate.opsForValue().set(key, user, 30, TimeUnit.MINUTES);
    }
    return user;
}
```

### 1.2 写流程

```text
更新 DB → 删除缓存（不是更新！）
```

```java
@Transactional
public void updateUser(User user) {
    userMapper.updateById(user);                     // 1. 先更新 DB
    redisTemplate.delete("user:" + user.getId());    // 2. 再删除缓存
}
```

> 为什么删除缓存而不是更新缓存？
> - 更新缓存可能做无用的序列化（更新后长时间没人读）
> - 并发写场景下，多次更新顺序不可控，可能导致缓存 dirty write
> - 删除缓存更简单安全，下次读时懒加载重新填充

### 1.3 先删缓存再更新 DB 的竞态问题

```text
时间线：
线程 A（写）：删除缓存 → 更新 DB
线程 B（读）：                 读缓存（未命中）→ 读取旧 DB 数据 → 回写旧数据到缓存
                                ↑
                        此时 A 还没更新完，B 读到脏数据
```

> 💡 这就是为什么推荐「先更新 DB 再删缓存」而不是「先删缓存再更新 DB」。后者被并发读破的概率更高。

---

## 2. 缓存穿透

### 2.1 问题

查询一个**根本不存在**的数据，缓存未命中，请求直接打到数据库。
恶意攻击者利用不存在 ID 大量请求，压垮数据库。

```text
正常请求：缓存 → DB（有数据）→ 回写缓存
穿透请求：缓存 → DB（无数据）→ 每次穿透到 DB
                               ↑ 空值不回写缓存，每次都查 DB
```

### 2.2 解决方案

**方案一：布隆过滤器**

布隆过滤器原理：N 个哈希函数 + M 位位图，判断 key **一定不存在** 或 **可能存在**。

```java
// 初始化：加载所有存在的 key 到布隆过滤器
RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter("user:filter");
bloomFilter.tryInit(1000000L, 0.01);

public User getUser(Long userId) {
    // 布隆过滤器判断
    if (!bloomFilter.contains("user:" + userId)) {
        return null;  // 一定不存在，直接返回
    }
    // 查缓存 → 查 DB
}
```

| 布隆过滤器参数 | 说明 |
|---------------|------|
| expectedInsertions | 预估元素数量 |
| falseProbability | 期望误判率（越小越好） |
| hashFunctions | 哈希函数个数 = ln(2) × (M/N) |

```text
100 万数据，1% 误判率：
位图大小 ≈ 11.5 MB
哈希函数 ≈ 7 个
布隆过滤器只存 hash，不存原始数据，无法删除元素
```

**方案二：缓存空值**

```java
public User getUser(Long userId) {
    String key = "user:" + userId;
    Object obj = redisTemplate.opsForValue().get(key);
    
    if (obj != null) {
        if (obj instanceof User) return (User) obj;
        return null;  // NULL 缓存
    }
    
    User user = userMapper.selectById(userId);
    if (user == null) {
        // 缓存空值，短 TTL 防长时间不一致
        redisTemplate.opsForValue().set(key, new NullValue(), 60, TimeUnit.SECONDS);
    } else {
        redisTemplate.opsForValue().set(key, user, 30, TimeUnit.MINUTES);
    }
    return user;
}
```

**方案三：参数校验**

```java
public User getUser(Long userId) {
    if (userId == null || userId <= 0) {
        throw new IllegalArgumentException("无效用户 ID");
    }
    // 查缓存 → 查 DB
}
```

### 2.3 方案对比

| 方案 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| 布隆过滤器 | 彻底拦截不存在请求 | 内存占用 + 需维护全量 key | 数据量大且有规律 ID |
| 缓存空值 | 实现简单 | 占用缓存空间 | 中小项目快速实现 |
| 参数校验 | 零开销 | 仅能拦截非法参数 | 配合其他方案使用 |

> 🎯 最佳实践：参数校验 + 布隆过滤器 + 空值缓存三重防护。

---

## 3. 缓存击穿

### 3.1 问题

一个**热点 key 刚好过期**的瞬间，大量并发请求同时发现缓存失效，同时查数据库。

```text
时间线：
              ┌─── 缓存过期 ───┐
              │                ▼
请求 1 ───────┤ 查 DB  ────────┼──┐
请求 2 ───────┤ 查 DB  ────────┼──┤ 数据库
请求 3 ───────┤ 查 DB  ────────┼──┤ 被打爆
请求 N ───────┤ 查 DB  ────────┼──┘
```

### 3.2 解决方案

**方案一：互斥锁**

```java
public String getHotData(String key) {
    // 1. 查缓存
    String data = (String) redisTemplate.opsForValue().get(key);
    if (data != null) return data;

    // 2. 加锁（分布式锁）
    String lockKey = "lock:" + key;
    Boolean locked = redisTemplate.opsForValue()
        .setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);

    if (Boolean.TRUE.equals(locked)) {
        try {
            // 3. 再次查缓存（Double Check）
            data = (String) redisTemplate.opsForValue().get(key);
            if (data != null) return data;

            // 4. 查 DB
            data = queryDB(key);
            redisTemplate.opsForValue().set(key, data, 30, TimeUnit.MINUTES);
            return data;
        } finally {
            redisTemplate.delete(lockKey);  // 释放锁
        }
    } else {
        // 5. 没拿到锁，等待重试
        Thread.sleep(50);
        return getHotData(key);  // 递归重试
    }
}
```

**方案二：逻辑过期**

```java
// 不设 TTL，而是存一个逻辑过期时间
public class CacheData<T> {
    private T data;
    private long expireTime;  // 逻辑过期时间戳
}

private ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);

public T getWithLogicalExpire(String key) {
    CacheData<T> cacheData = (CacheData<T>) redisTemplate.opsForValue().get(key);

    // 未过期，直接返回
    if (cacheData.expireTime > System.currentTimeMillis()) {
        return cacheData.data;
    }

    // 已过期 → 尝试加锁重建
    String lockKey = "lock:" + key;
    if (Boolean.TRUE.equals(redisTemplate.opsForValue()
            .setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS))) {
        // 异步后台刷新缓存
        executor.submit(() -> {
            T newData = queryDB(key);
            redisTemplate.opsForValue().set(key, 
                new CacheData<>(newData, System.currentTimeMillis() + 30000));
            redisTemplate.delete(lockKey);
        });
    }

    // 直接返回旧数据（短暂不一致但可用）
    return cacheData.data;
}
```

### 3.3 方案对比

| 方案 | 优点 | 缺点 | 适用 |
|------|------|------|------|
| 互斥锁 | 数据强一致 | 可能阻塞，有锁竞争 | 数据一致性要求高 |
| 逻辑过期 | 高性能不阻塞 | 短暂不一致，实现复杂 | 高并发容忍短不一致 |

> 🎯 强一致性场景用互斥锁，高并发弱一致性场景用逻辑过期。

---

## 4. 缓存雪崩

### 4.1 问题

大**量 key 同时过期**或 **Redis 宕机**，所有请求直接打向数据库。

```text
正常情况：  │缓存│→ 数据库（少量查询）
雪崩情况：  │缓存│→ 数据库（大量并发 → 数据库崩溃）
            └──缓存大规模失效──┘
```

### 4.2 解决方案

**方案一：TTL 加随机值**

```java
// 设置过期时间时，加随机偏移
long ttl = 3600 + new Random().nextInt(600);  // 1小时 ± 5分钟
redisTemplate.opsForValue().set(key, data, ttl, TimeUnit.SECONDS);
```

```bash
# 批量设过期时间时，分散 TTL
SETEX user:1001 3600  data
SETEX user:1002 3720  data
SETEX user:1003 3850  data
# 避免全部在同一秒过期
```

**方案二：多级缓存**

```java
@Configuration
public class CaffeineConfig {
    @Bean
    public Cache<String, Object> caffeineCache() {
        return Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();
    }
}

@Service
public class CacheService {
    @Autowired private Cache<String, Object> caffeineCache;
    @Autowired private RedisTemplate<String, Object> redisTemplate;
    @Autowired private UserMapper userMapper;

    public User getUser(Long userId) {
        String key = "user:" + userId;

        // 1. 本地缓存（最快）
        User user = (User) caffeineCache.getIfPresent(key);
        if (user != null) return user;

        // 2. Redis 缓存
        user = (User) redisTemplate.opsForValue().get(key);
        if (user != null) {
            caffeineCache.put(key, user);  // 回填本地缓存
            return user;
        }

        // 3. 数据库
        user = userMapper.selectById(userId);
        if (user != null) {
            redisTemplate.opsForValue().set(key, user, 3600, TimeUnit.SECONDS);
            caffeineCache.put(key, user);
        }
        return user;
    }
}
```

| 缓存层级 | 延迟 | 容量 | 优点 |
|---------|------|------|------|
| Caffeine（本地） | 纳秒级 | 小（GB 内） | 极快，不受网络影响 |
| Redis（集中） | 毫秒级 | 大（GB-TB） | 共享，分布式 |
| 数据库 | 毫秒级+ | 最大 | 最终数据源 |

**方案三：限流熔断**

- 使用 Sentinel / Hystrix 做服务降级
- Redis 宕机时直接返回缓存降级数据或错误提示

```java
// Sentinel 限流降级
@SentinelResource(value = "getUser", fallback = "getUserFallback")
public User getUser(Long userId) {
    // 正常逻辑
}

public User getUserFallback(Long userId, Throwable e) {
    // 降级：返回本地缓存或默认值
    return new User("默认用户");
}
```

**方案四：高可用**

- Redis 主从 + Sentinel，防止单点宕机
- 持久化保障，重启自动恢复

---

## 5. 缓存一致性

### 5.1 不一致的根本原因

缓存和数据库是两个独立系统，更新动作无法原子化。

```text
并发场景举例：
时间：     T1         T2          T3
写线程：   更新 DB     ──→        删除缓存
读线程：              查缓存（未删）→ 返回旧数据
                                              ↑ 读到旧数据
```

### 5.2 四种一致性方案

**方案一：Cache Aside（最终一致）**

```text
写：先更新 DB → 再删除缓存
```

简单安全，适合大多数场景。

**方案二：延迟双删**

```java
@Transactional
public void updateUser(User user) {
    redisTemplate.delete("user:" + user.getId());  // 1. 先删缓存
    userMapper.updateById(user);                   // 2. 更新 DB
    // 3. 延迟再删（解决并发读-写时序问题）
    CompletableFuture.runAsync(() -> {
        try { Thread.sleep(500); } catch (InterruptedException e) { }
        redisTemplate.delete("user:" + user.getId());
    });
}
```

```text
延迟双删时间线：
1. 删除缓存（清空脏数据）
2. 更新 DB（耗时操作）
3. 延迟 500ms 再次删除（清除期间读请求回写的旧数据）
```

**方案三：Canal + MQ（强最终一致）**

```text
MySQL binlog → Canal 监听 → MQ (RocketMQ/Kafka) → 消费端删除/更新缓存
```

| 组件 | 作用 |
|------|------|
| Canal | 阿里开源，监听 MySQL binlog 增量订阅 |
| MQ | 异步解耦，削峰填谷 |
| 消费端 | 解析 binlog 事件，删除或更新缓存 |

```java
// Canal 消费端伪代码
@RabbitListener(queues = "cache.sync.queue")
public void handleBinlogEvent(BinlogEvent event) {
    if (event.getTable().equals("user")) {
        redisTemplate.delete("user:" + event.getRowId());
    }
}
```

**方案四：读写锁（Redisson ReadWriteLock）**

```java
RReadWriteLock rwLock = redissonClient.getReadWriteLock("user:lock:" + userId);

// 读数据：加读锁（共享）
RLock readLock = rwLock.readLock();
readLock.lock();
try {
    user = redisTemplate.opsForValue().get(key);
    if (user == null) user = userMapper.selectById(userId);
} finally { readLock.unlock(); }

// 写数据：加写锁（互斥）
RLock writeLock = rwLock.writeLock();
writeLock.lock();
try {
    userMapper.updateById(user);
    redisTemplate.delete(key);
} finally { writeLock.unlock(); }
```

### 5.3 方案对比

| 方案 | 一致性 | 性能 | 复杂度 | 适用 |
|------|--------|------|--------|------|
| Cache Aside | 最终一致 | 高 | 低 | 绝大多数场景 |
| 延迟双删 | 较强 | 高 | 中 | 对一致性要求较高 |
| Canal + MQ | 强最终一致 | 高 | 高 | 核心业务，数据敏感 |
| 读写锁 | 强 | 中 | 中 | 读写比例失衡场景 |

### 5.4 一致性最佳实践

> 💡 不要追求缓存强一致性——缓存的设计目标就是最终一致。真有强一致需求就直读数据库。

> 💡 所有缓存设过期时间（5-30分钟）兜底，即使删除失败也会自动过期重建。

> 💡 先更新 DB 再删缓存，不要反过来。先删缓存再更新 DB 的并发窗口期更长。

---

## 6. 缓存淘汰策略

当内存达到 `maxmemory` 上限时，Redis 根据 `maxmemory-policy` 决定淘汰哪些 key。

### 6.1 8 种策略速查

| 策略 | 说明 | 是否建议 |
|------|------|:--------:|
| noeviction | 不淘汰，写操作报错 | ❌ |
| **allkeys-lru** | **通用推荐：淘汰最近最少使用** | ✅ |
| volatile-lru | 仅对有过期时间的 key 执行 LRU | ⚠️ |
| allkeys-lfu | 淘汰最不经常使用 | ✅ |
| volatile-lfu | 仅对有过期 key 执行 LFU | ⚠️ |
| volatile-random | 过期 key 随机淘汰 | ❌ |
| allkeys-random | 所有 key 随机淘汰 | ❌ |
| volatile-ttl | 淘汰 TTL 最小的 | ❌ |

```bash
maxmemory 2gb
maxmemory-policy allkeys-lru
maxmemory-samples 5
```

> 💡 **LRU vs LFU：** LRU 适合周期性访问模式，LFU 适合持续热点模式。Redis 4.0+ 推荐 LFU 场景可用 `allkeys-lfu`。

### 6.2 近似 LRU 算法

Redis 的 LRU 不是精确 LRU，而是采样式近似 LRU：

```text
精确 LRU：维护全量链表，内存开销大
Redis LRU：
1. 每次随机采样 maxmemory-samples 个 key（默认 5 个）
2. 淘汰其中空闲时间最久的 key
3. 采样数越大越精确，但 CPU 消耗也越大
```

| maxmemory-samples | 精确度 | CPU 消耗 |
|:-----------------:|:------:|:--------:|
| 5（默认） | 接近理论 LRU 90% | 低 |
| 10 | 接近理论 LRU 99% | 中 |
| 20 | 非常接近 | 高 |

---

## 7. 大 Key 与热 Key

### 7.1 大 Key

| 定义 | 单个 key 包含大量数据 |
|------|----------------------|
| String > 10MB | 超大字符串 |
| 集合 > 1 万元素 | 大 Set/ZSet/List/Hash |

**危害：**
- 阻塞 Redis（读写大 key 耗时）
- 网络带宽打满
- 数据倾斜（集群中某节点负载过高）
- 慢查询、超时

**检测：**

```bash
redis-cli --bigkeys              # 扫描大 key（逐个遍历）
MEMORY USAGE key_name            # 查看 key 内存占用
SCAN 0 TYPE string COUNT 100     # 手动扫描
redis-cli --hotkeys              # 检测热 key（需要开启 maxmemory-policy）
```

```java
// 程序化检测大 Key
public void detectBigKeys() {
    ScanOptions options = ScanOptions.scanOptions()
        .match("*")
        .count(100)
        .build();
    
    Cursor<String> cursor = redisTemplate.scan(options);
    while (cursor.hasNext()) {
        String key = cursor.next();
        Long size = redisTemplate.opsForValue().size(key);
        if (size != null && size > 1024 * 1024) { // > 1MB
            log.warn("发现大 Key: {} size={}", key, size);
        }
    }
}
```

**处理：**

```bash
# 拆分大 key
hash:user:1001 → hash:user:1001:base + hash:user:1001:detail

# 分批删除（UNLINK 非阻塞删除）
UNLINK big_key

# 集合裁剪（限制集合大小）
LTRIM list_key 0 999  # List 保留前 1000 条
ZREMRANGEBYRANK zset 0 -10001  # ZSet 保留最新 10000 条
```

### 7.2 热 Key

**定义：** 某个 key 的访问频率极高，导致单节点 CPU/带宽打满。

**检测：**

```bash
redis-cli --hotkeys              # Redis 4.0+ 支持
MONITOR | grep "hot_key"         # 实时监控（注意性能开销）
```

```java
// 客户端本地统计
@Component
public class HotKeyDetector {
    private LoadingCache<String, AtomicLong> counter = Caffeine.newBuilder()
        .expireAfterWrite(10, TimeUnit.SECONDS)
        .build(key -> new AtomicLong(0));

    public void record(String key) {
        counter.get(key).incrementAndGet();
    }

    public Map<String, Long> getHotKeys() {
        return counter.asMap().entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().get()));
    }
}
```

**处理：**

| 方案 | 说明 | 优点 | 缺点 |
|------|------|------|------|
| 本地缓存 | Caffeine 本地缓存热点 key | 减少 Redis 压力 | 内存占用 |
| 读写分离 | 从节点分担读请求 | 简单有效 | 有复制延迟 |
| 拆分子 key | `hot_key` → `hot_key:1`、`hot_key:2` | 分散单点压力 | 客户端需聚合 |
| 限流 | 对热 key 访问做限流 | 保护后端 | 可能影响用户体验 |

---

## 8. 缓存问题对比总结

| 问题 | 原因 | 表现 | 解决方案 |
|------|------|------|----------|
| **穿透** | 查询不存在数据 | 缓存无效，直接打 DB | 布隆过滤器 + 空值缓存 + 参数校验 |
| **击穿** | 热点 key 过期 | 高并发打 DB | 互斥锁 + 逻辑过期 |
| **雪崩** | 大量 key 同时过期 / Redis 宕机 | DB 崩溃 | TTL 随机化 + 多级缓存 + 高可用 |
| **一致性** | 缓存与 DB 更新不同步 | 读到脏数据 | Cache Aside + 延迟双删 + Canal |

### 问题排查流程图

```text
请求到达
  ↓
缓存中是否有数据？──有──→ 直接返回
  ↓ 无
Redis 是否正常？──宕机──→ 走多级缓存/限流降级
  ↓ 正常
key 是否存在？──不存在──→ 布隆过滤拦截 or 缓存空值
  ↓ 存在但过期
是否热点 key？──是──→ 互斥锁或逻辑过期保护
  ↓ 否
正常查 DB 回写缓存
```

---

## 面试核心问题

**Q: 缓存穿透、击穿、雪崩的区别和解决方案？**
见上表。穿透是不存在的 key，击穿是热点 key 过期，雪崩是大量同时过期或宕机。

**Q: 缓存一致性方案？**
Cache Aside（先更新 DB 再删缓存）、延迟双删、Canal + MQ、读写锁。不要追求强一致，设过期兜底。

**Q: 大 Key 的危害和检测？**
危害：阻塞 Redis、带宽打满、数据倾斜。检测：`--bigkeys`、`MEMORY USAGE`、SCAN 遍历。

**Q: 为什么删除缓存而不是更新？**
更新可能做无用功（更新后没人读），并发写可能乱序。删除更简单安全，下次读时懒加载。

**Q: Redis 淘汰策略推荐？**
通用推荐 `allkeys-lru`，持续热点场景 `allkeys-lfu`。不推荐 `noeviction`。
