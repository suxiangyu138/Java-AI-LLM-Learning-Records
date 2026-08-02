# Redis 应用场景实战
> 从缓存到搜索、从社交到限流——8 大企业级场景完整 Redis 实现方案。

## 目录
1. [会话管理](#1-会话管理)
2. [分布式锁](#2-分布式锁)
3. [接口限流](#3-接口限流)
4. [轻量搜索](#4-轻量搜索)
5. [社交网络](#5-社交网络)
6. [排行榜](#6-排行榜)
7. [消息队列](#7-消息队列)
8. [内存优化](#8-内存优化)
9. [场景速查对照表](#9-场景速查对照表)

---

## 1. 会话管理

### 1.1 Session 共享

分布式系统中，用户登录状态需要在多个服务间共享。传统 Tomcat Session 无法跨服务，Redis 成为 Session 共享的标准方案。

```yaml
spring:
  session:
    store-type: redis       # Spring Session 自动存储 Session 到 Redis
    redis:
      namespace: web:session
    timeout: 86400
```

### 1.2 Token 管理

```java
// 登录：生成 Token + 存储用户信息
public String login(String username, String password) {
    // 验证用户名密码...
    String token = UUID.randomUUID().toString().replace("-", "");
    String sessionKey = "session:" + token;
    
    Map<String, Object> sessionInfo = Map.of(
        "userId", "1001", "username", username, "role", "admin"
    );
    redisTemplate.opsForHash().putAll(sessionKey, sessionInfo);
    redisTemplate.expire(sessionKey, 7200, TimeUnit.SECONDS);
    return token;
}

// 校验 Token（拦截器）
public boolean checkToken(String token) {
    String sessionKey = "session:" + token;
    Boolean hasKey = redisTemplate.hasKey(sessionKey);
    if (Boolean.TRUE.equals(hasKey)) {
        redisTemplate.expire(sessionKey, 7200, TimeUnit.SECONDS); // 续期
    }
    return hasKey;
}

// 登出
public void logout(String token) {
    redisTemplate.delete("session:" + token);
}
```

### 1.3 验证码

```java
// 发送验证码
public void sendSmsCode(String phone) {
    String code = String.format("%06d", new Random().nextInt(1000000));
    String key = "sms:code:" + phone;
    redisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES);
    // 调用短信服务发送
    smsService.send(phone, code);
}

// 校验验证码
public boolean verifySmsCode(String phone, String code) {
    String key = "sms:code:" + phone;
    String storedCode = (String) redisTemplate.opsForValue().get(key);
    if (code != null && code.equals(storedCode)) {
        redisTemplate.delete(key); // 校验成功立即删除（一次性）
        return true;
    }
    return false;
}
```

---

## 2. 分布式锁

### 2.1 基本实现

```java
@Component
public class RedisLock {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 加锁
    public boolean tryLock(String key, String requestId, long expireSec) {
        return Boolean.TRUE.equals(
            redisTemplate.opsForValue()
                .setIfAbsent(key, requestId, expireSec, TimeUnit.SECONDS)
        );
    }

    // 解锁（Lua 脚本保证原子性）
    public boolean unlock(String key, String requestId) {
        String script = "if redis.call('GET', KEYS[1]) == ARGV[1] " +
                        "then return redis.call('DEL', KEYS[1]) " +
                        "else return 0 end";
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        Long result = redisTemplate.execute(redisScript, List.of(key), requestId);
        return Long.valueOf(1).equals(result);
    }
}
```

### 2.2 锁使用模板

```java
String lockKey = "lock:order:" + orderNo;
String requestId = UUID.randomUUID().toString();

if (redisLock.tryLock(lockKey, requestId, 30)) {
    try {
        // 执行业务
    } finally {
        redisLock.unlock(lockKey, requestId);  // 必须释放
    }
} else {
    throw new RuntimeException("操作频繁，请稍后重试");
}
```

### 2.3 Redisson 看门狗（Watchdog）

> 自动续期机制：默认 30 秒过期，每 10 秒续期一次，业务未完成锁不会提前释放。

```java
@Autowired
private RedissonClient redissonClient;

public void businessWithLock(String orderId) {
    RLock lock = redissonClient.getLock("lock:order:" + orderId);
    
    // leaseTime=-1 启用看门狗（默认 30 秒，自动续期）
    lock.lock(10, TimeUnit.SECONDS);
    try {
        // 业务逻辑（可超过 10 秒，看门狗会续期）
        processOrder(orderId);
    } finally {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
```

### 2.4 看门狗原理

```text
lock() 或 tryLock() 不传过期时间时：

1. 默认锁过期时间 = 30 秒
2. 加锁成功后，启动一个定时任务
3. 每 10 秒检查一次锁是否还存在
4. 若锁还在，将过期时间重置为 30 秒
5. 业务完成后 unlock() 取消看门狗
```

> ⚠️ 锁的 4 个核心要求：互斥性（SETNX）、防死锁（自动过期）、防误删（唯一 ID 校验）、高性能（Lua 原子操作）。

---

## 3. 接口限流

### 3.1 计数器限流

```java
@Component
public class RateLimiter {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * @param key     限流标识（IP/用户ID）
     * @param maxCount 窗口内最大请求次数
     * @param windowSec 时间窗口（秒）
     */
    public boolean isAllowed(String key, int maxCount, long windowSec) {
        String redisKey = "limit:" + key;
        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (Long.valueOf(1).equals(count)) {
            redisTemplate.expire(redisKey, windowSec, TimeUnit.SECONDS);
        }
        return count <= maxCount;
    }
}
```

```java
// 使用：IP 1 分钟最多 100 次
if (!rateLimiter.isAllowed("ip:" + ipAddr, 100, 60)) {
    throw new RuntimeException("访问过于频繁");
}
```

### 3.2 Lua 原子限流

```lua
-- KEYS[1]: 限流 key
-- ARGV[1]: 窗口大小（秒）
-- ARGV[2]: 最大请求数
local current = redis.call('INCR', KEYS[1])
if tonumber(current) == 1 then
    redis.call('EXPIRE', KEYS[1], ARGV[1])
end
if tonumber(current) > tonumber(ARGV[2]) then
    return 0  -- 限流
else
    return 1  -- 放行
end
```

### 3.3 滑动窗口限流（ZSet 实现）

```java
// 滑动窗口限流：记录每个请求的时间戳
public boolean slidingWindowLimit(String key, int maxCount, long windowMs) {
    String windowKey = "sw:limit:" + key;
    long now = System.currentTimeMillis();
    long windowStart = now - windowMs;
    
    // 移除窗口外的记录
    redisTemplate.opsForZSet().removeRangeByScore(windowKey, 0, windowStart);
    
    // 添加当前请求
    redisTemplate.opsForZSet().add(windowKey, String.valueOf(now), now);
    redisTemplate.expire(windowKey, windowMs / 1000 + 1, TimeUnit.SECONDS);
    
    // 统计窗口内请求数
    Long count = redisTemplate.opsForZSet().zCard(windowKey);
    return count <= maxCount;
}
```

### 3.4 限流策略对比

| 方式 | 优点 | 缺点 | 适用 |
|------|------|------|------|
| 计数器 | 实现简单 | 边界突刺（窗口切换时流量暴增） | 精度要求不高 |
| 滑动窗口 | 更精准 | 需要 ZSet 存储每个请求时间戳 | 精度要求高 |
| 令牌桶 | 允许突发 | 实现复杂 | 允许突发的场景 |
| 漏桶 | 平滑流量 | 不适合突发场景 | 流量整形 |

---

## 4. 轻量搜索

### 4.1 前缀搜索（联想提示）

利用 ZSet 的字典序范围查询。

```java
@Component
public class SearchComponent {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // 初始化索引（项目启动时导入）
    public void initKeywords(Collection<String> keywords) {
        for (String kw : keywords) {
            redisTemplate.opsForZSet()
                .add("search:dict", kw.toLowerCase(), 0);
        }
    }

    // 前缀搜索
    public List<String> prefixSearch(String prefix, int limit) {
        String key = "search:dict";
        Set<String> result = redisTemplate.opsForZSet()
            .rangeByLex(key, 
                RedisZSetCommands.Range.range().gte(prefix),
                RedisZSetCommands.Range.range().lte(prefix + "￿"),
                0, limit);
        return new ArrayList<>(result);
    }

    // 热搜榜
    public void addHotSearch(String keyword) {
        redisTemplate.opsForZSet()
            .incrementScore("hot:search", keyword, 1);
    }

    public List<String> getHotSearch(int topN) {
        Set<String> result = redisTemplate.opsForZSet()
            .reverseRange("hot:search", 0, topN - 1);
        return new ArrayList<>(result);
    }
}
```

### 4.2 搜索架构

```text
数据采集 → 分词(IK) → 索引构建(ZSet) → 搜索匹配 → 结果排序 → 缓存加速
                                                          ↓
                                                    Hash 存储详情
```

> 💡 适用场景：10 万级数据量。百万级以上请用 Elasticsearch。

---

## 5. 社交网络

### 5.1 核心数据模型

| 功能 | 数据结构 | Key 格式 |
|------|----------|----------|
| 用户信息 | Hash | `user:{userId}` |
| 关注列表 | Set | `follow:{userId}` |
| 粉丝列表 | Set | `fans:{userId}` |
| 动态内容 | String | `feed:{feedId}` |
| 时间线 | ZSet | `timeline:{userId}` |
| 点赞 | Set | `like:{feedId}` |
| 评论 | List | `comment:{feedId}` |

### 5.2 核心实现

**用户与关系**

```java
// 关注（双向）
public void follow(Long userId, Long targetId) {
    redisTemplate.opsForSet().add("follow:" + userId, targetId.toString());
    redisTemplate.opsForSet().add("fans:" + targetId, userId.toString());
}

// 取关
public void unfollow(Long userId, Long targetId) {
    redisTemplate.opsForSet().remove("follow:" + userId, targetId.toString());
    redisTemplate.opsForSet().remove("fans:" + targetId, userId.toString());
}

// 共同好友
public Set<Object> commonFriends(Long userId1, Long userId2) {
    return redisTemplate.opsForSet()
        .intersect("follow:" + userId1, "follow:" + userId2);
}

// 是否已关注
public boolean isFollowing(Long userId, Long targetId) {
    return Boolean.TRUE.equals(
        redisTemplate.opsForSet()
            .isMember("follow:" + userId, targetId.toString()));
}

// 好友推荐（二度关系）
public Set<Object> recommendFriends(Long userId) {
    // 获取用户关注的人
    Set<Object> following = redisTemplate.opsForSet()
        .members("follow:" + userId);
    
    // 取关注的人关注的人（二度人脉）
    String[] keys = following.stream()
        .map(id -> "follow:" + id.toString())
        .toArray(String[]::new);
    
    return redisTemplate.opsForSet()
        .union("recommend:" + userId, List.of(keys));
}
```

**动态发布与时间线**

```java
public void publishFeed(Long userId, String content, Long feedId) {
    // 1. 存动态内容
    redisTemplate.opsForValue()
        .set("feed:" + feedId, content, 7, TimeUnit.DAYS);

    // 2. 写入时间线（ZSet score = 时间戳）
    long now = System.currentTimeMillis();
    redisTemplate.opsForZSet()
        .add("timeline:" + userId, feedId.toString(), now);

    // 3. 裁剪时间线，保留最近 200 条
    redisTemplate.opsForZSet()
        .removeRange("timeline:" + userId, 0, -201);
}

// 拉取首页时间线（自己 + 关注人动态）
public Set<ZSetOperations.TypedTuple<Object>> getTimeline(Long userId, int limit) {
    // 合并自己和所有关注人的时间线
    String unionKey = "timeline:union:" + userId;
    Set<Object> followList = redisTemplate.opsForSet()
        .members("follow:" + userId);

    List<String> timelineKeys = new ArrayList<>();
    timelineKeys.add("timeline:" + userId);
    for (Object followId : followList) {
        timelineKeys.add("timeline:" + followId);
    }
    
    redisTemplate.opsForZSet()
        .unionAndStore(unionKey, timelineKeys, unionKey);
    redisTemplate.expire(unionKey, 60, TimeUnit.SECONDS);

    return redisTemplate.opsForZSet()
        .reverseRangeWithScores(unionKey, 0, limit - 1);
}
```

### 5.3 推模式 vs 拉模式

| 模式 | 写操作 | 读操作 | 适用 |
|------|--------|--------|------|
| 推模式 | 发布时写入所有粉丝时间线（写放大）| 直接读取（速度快）| 大 V 粉丝多时写开销大  |
| 拉模式 | 只写自己的时间线（写开销小）| 读取时合并（读放大）| 粉丝量大的场景 |

> 💡 上例使用拉模式（读取时合并），比推模式（写入时扩散）节省 80% 内存。实际生产通常采用推拉结合：普通用户用推模式，大 V 用户用拉模式。

### 5.4 点赞与评论

```java
// 点赞
public void like(Long feedId, Long userId) {
    redisTemplate.opsForSet().add("like:" + feedId, userId.toString());
}

// 取消点赞
public void unlike(Long feedId, Long userId) {
    redisTemplate.opsForSet().remove("like:" + feedId, userId.toString());
}

// 点赞数
public Long likeCount(Long feedId) {
    return redisTemplate.opsForSet().size("like:" + feedId);
}

// 添加评论
public void addComment(Long feedId, String comment) {
    redisTemplate.opsForList().rightPush("comment:" + feedId, comment);
}

// 获取评论（分页）
public List<Object> getComments(Long feedId, int page, int size) {
    int start = (page - 1) * size;
    int end = start + size - 1;
    return redisTemplate.opsForList()
        .range("comment:" + feedId, start, end);
}
```

---

## 6. 排行榜

### 6.1 基础实现

```java
// 更新分数
redisTemplate.opsForZSet()
    .incrementScore("rank:score", "user:1001", 10.0);

// Top 10
Set<ZSetOperations.TypedTuple<Object>> top10 = 
    redisTemplate.opsForZSet()
        .reverseRangeWithScores("rank:score", 0, 9);

// 个人排名
Long rank = redisTemplate.opsForZSet()
    .reverseRank("rank:score", "user:1001");

// 分数区间人数
Long count = redisTemplate.opsForZSet()
    .count("rank:score", 80, 100);
```

### 6.2 多维度排行榜

```java
// 周榜（每日更新）
public void updateWeeklyRank(String userId, double score) {
    // 日榜
    String dailyKey = "rank:daily:" + LocalDate.now();
    redisTemplate.opsForZSet().incrementScore(dailyKey, userId, score);
    redisTemplate.expire(dailyKey, 3, TimeUnit.DAYS);
}

// 月榜
public void updateMonthlyRank(String userId, double score) {
    String monthlyKey = "rank:monthly:" + YearMonth.now();
    redisTemplate.opsForZSet().incrementScore(monthlyKey, userId, score);
    redisTemplate.expire(monthlyKey, 35, TimeUnit.DAYS);
}
```

---

## 7. 消息队列

### 7.1 List 简易队列

```java
@Component
public class SimpleQueue {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 生产者
    public void push(String queueName, Object task) {
        redisTemplate.opsForList()
            .rightPush("queue:" + queueName, task);
    }

    // 消费者（阻塞）
    public Object pop(String queueName, long timeoutSec) {
        return redisTemplate.opsForList()
            .leftPop("queue:" + queueName, timeoutSec, TimeUnit.SECONDS);
    }
}
```

### 7.2 Stream 可靠队列

```java
// 生产者
redisTemplate.opsForStream()
    .add(StreamRecords.newRecord()
        .in("order:stream")
        .ofMap(Map.of("orderId", "1001", "amount", "99.9")));

// 消费者
Consumer consumer = Consumer.from("order-group", "consumer-1");
List<MapRecord<String, Object, Object>> messages = 
    redisTemplate.opsForStream().read(consumer,
        StreamReadOptions.empty().count(1).block(Duration.ofSeconds(1)),
        StreamOffset.create("order:stream", ReadOffset.lastConsumed()));

// ACK 确认
redisTemplate.opsForStream()
    .acknowledge("order:stream", "order-group", record.getId());
```

### 7.3 队列方案对比

| 方案 | 优点 | 缺点 | 适用 |
|------|------|------|------|
| List (LPUSH/BRPOP) | 实现简单 | 不支持 ACK 和消费者组 | 简单异步任务 |
| Stream | 支持 ACK、消费者组、消息回溯 | 实现略复杂 | 需要可靠投递 |
| Pub/Sub | 广播模式 | 消息不持久，丢失无记录 | 实时通知 |

> ⚠️ 中小规模异步任务用 List 即可；需要 ACK 确认、消费者组、消息回溯用 Stream；核心业务消息用 RabbitMQ/Kafka。

---

## 8. 内存优化

### 8.1 数据结构优化

| 场景 | 不推荐 | 推荐 | 节省 |
|------|--------|------|------|
| 存储对象 | 多个 String | Hash 一个 key | 减少元数据 |
| 布尔状态 | Set | Bitmap | 1000 万仅 1.2MB |
| 基数统计 | Set | HyperLogLog | 单个仅 12KB |
| 大 Key | 单键 >100MB | 分片拆分 | 减少碎片 |

**Bitmap 示例（每日签到）：**

```java
// 用户 1001 在第 5 天签到
redisTemplate.opsForValue().setBit("sign:2024:01", 5, true);

// 第 5 天是否签到
Boolean signed = redisTemplate.opsForValue().getBit("sign:2024:01", 5);

// 当月签到天数
Long days = redisTemplate.execute(
    (RedisCallback<Long>) conn -> conn.bitCount("sign:2024:01".getBytes()));
```

**HyperLogLog 示例（UV 统计）：**

```java
// 用户访问记录
redisTemplate.opsForHyperLogLog().add("uv:page:1001", "user:1001", "user:1002");

// 独立访客数
Long uv = redisTemplate.opsForHyperLogLog().size("uv:page:1001");

// 合并多个页面
redisTemplate.opsForHyperLogLog().union("uv:total", "uv:page:1001", "uv:page:1002");
```

### 8.2 编码优化

| 原则 | 说明 |
|------|------|
| 短 Key | `u:1001` 替代 `user:id:1001:info` |
| 纯数字 | 纯数字字符串自动 int 编码，内存极小 |
| 小集合 | 控制 ziplist/intset 阈值，触发紧凑编码 |
| 过期时间 | 所有缓存数据必须设 TTL |

### 8.3 配置优化

| 配置 | 推荐值 | 说明 |
|------|:------:|------|
| `maxmemory` | 物理内存 70-80% | 内存上限 |
| `maxmemory-policy` | allkeys-lru | 淘汰策略 |
| `activedefrag` | yes | 主动碎片整理 |
| `hash-max-listpack-entries` | 512 | Hash 紧凑编码阈值 |
| `zset-max-listpack-entries` | 128 | ZSet 紧凑编码阈值 |

### 8.4 内存分析

```bash
# 查看内存使用统计
INFO memory

# 查看 key 分布
MEMORY STATS

# 查看单个 key 内存
MEMORY USAGE key_name

# 大 key 扫描
redis-cli --bigkeys

# 内存分析工具（建议）
redis-rdb-tools: rdb -c memory dump.rdb
```

---

## 9. 场景速查对照表

| 业务场景 | Redis 实现 | 数据结构 | 关键命令 |
|----------|-----------|----------|----------|
| Session 共享 | Token → 用户信息 | String/Hash | SETEX / HSET / EXPIRE |
| 分布式锁 | SET NX EX + Lua 解锁 | String + Lua | SETNX / EVAL |
| 接口限流 | 计数器 + 过期 | String | INCR / EXPIRE |
| 搜索联想 | 字典序前缀匹配 | ZSet | ZRANGEBYLEX |
| 热搜榜 | 搜索词 + 分数增量 | ZSet | ZINCRBY / ZREVRANGE |
| 关注/粉丝 | 集合存储 ID | Set | SADD / SINTER |
| 时间线 | 推/拉模式 + 时间排序 | ZSet | ZADD / ZUNIONSTORE |
| 排行榜 | 分数排序 | ZSet | ZINCRBY / ZREVRANGE |
| 消息队列 | 生产者-消费者 | List/Stream | LPUSH / BRPOP / XADD |
| 签到 | 位图 | Bitmap | SETBIT / BITCOUNT |
| UV 统计 | 基数估算 | HyperLogLog | PFADD / PFCOUNT |
| 附近的人 | GeoHash 编码 | GEO | GEOADD / GEORADIUS |
| 缓存穿透 | 布隆过滤器 | Bloom Filter | BF.ADD / BF.EXISTS |

### 场景分类速查

| 类别 | 场景 | 数据结构 | 说明 |
|------|------|----------|------|
| 会话管理 | Token/验证码 | String/Hash | 设过期时间自动失效 |
| 锁 | 分布式锁 | String+Lua | SET NX EX + 原子解锁 |
| 限流 | 接口防刷 | String+Lua | INCR + 过期 + Lua 原子 |
| 搜索 | 前缀/热搜 | ZSet | rangeByLex + score 排序 |
| 社交 | 关系/时间线 | Set+ZSet | SINTER 交集/ZUNION 合并 |
| 排行 | TopN | ZSet | reverseRange + scores |
| 队列 | 异步任务 | List/Stream | LPUSH/BRPOP/XADD |
| 优化 | 压缩/编码 | 各类 | 短 key + 小集合 + 过期 |

---

## 面试核心问题

**Q: Redis 分布式锁如何实现？**
SET NX EX + Lua 原子解锁 + 唯一 ID 防误删。推荐用 Redisson 看门狗自动续期。

**Q: 限流场景如何实现？**
计数器（简单）、滑动窗口（精确）、令牌桶（允许突发）。核心是 INCR + EXPIRE + Lua 原子保证。

**Q: 社交关系用 Redis 怎么储存？**
关注/粉丝用 Set，时间线用 ZSet，动态内容用 String。拉模式读取时合并关注人的时间线。

**Q: Bitmap 和 HyperLogLog 适合什么场景？**
Bitmap：签到、在线状态、布隆过滤器。HyperLogLog：UV 统计，12KB 存储上亿基数。
