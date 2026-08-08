# 04 操作API与Redis 8.4新命令速查

> 六大 Operations 全览、绑定 key 的 Bound 操作、Redis 8.4 新命令（条件 SET/DEL、DELEX/MSETEX/DIGEST）、函数式 API、命令版本对照——"一个命令一个场景"的完整手册

---

## 📚 目录

1. [六大 Operations 全览](#1-六大-operations-全览)
2. [Value / Hash 操作速查](#2-value--hash-操作速查)
3. [List / Set / ZSet 操作速查](#3-list--set--zset-操作速查)
4. [Stream 操作与 8.4 增强](#4-stream-操作与-84-增强)
5. [Redis 8.4 新命令（面试加分项）](#5-redis-84-新命令面试加分项)
6. [函数式 API 与版本对照](#6-函数式-api-与版本对照)

---

## 1. 六大 Operations 全览

**通俗**：RedisTemplate 按数据类型拆成六个"操作面板"——每种数据结构一套类型安全的方法，不用记原始命令。

| Operations | 对应类型 | 高频方法 | 典型场景 |
|-----------|---------|---------|---------|
| `opsForValue()` | String | set/get/increment/decrement/append | 缓存、计数、分布式锁值 |
| `opsForHash()` | Hash | put/get/entries/increment/delete | 对象字段、购物车、会话 |
| `opsForList()` | List | leftPush/rightPop/range/trim | 队列、最新列表、延迟队列 |
| `opsForSet()` | Set | add/members/isMember/intersect | 去重、标签、共同好友 |
| `opsForZSet()` | ZSet | add/rangeWithScores/incrementScore/rank | **排行榜、延迟任务、限流窗口** |
| `opsForStream()` | Stream | add/read/readGroup/ack | 消息队列（[09 篇](09-消息Pub-Sub与Stream速查.md)） |
| （附）`opsForGeo()` | GEO | add/radius | 附近的人（[Redis-03](../../../../02-非关系型数据库/Redis/03-Redis高级数据结构.md)） |
| （附）`opsForHyperLogLog()` | HLL | add/size | 基数统计（UV） |

> 💡 记忆抓手：**"opsFor 开头的都是 Redis 模板的六块面板"**——拿到 RedisTemplate 后按数据结构选面板；每块面板方法名 ≈ Redis 命令的驼峰化（set ↔ SET、increment ↔ INCR、leftPush ↔ LPUSH）。

## 2. Value / Hash 操作速查

### 2.1 ValueOperations（缓存主战场）

```java
ValueOperations<String, Object> v = redisTemplate.opsForValue();

// 基础读写
v.set("product:1", product);                        // SET
Object p = v.get("product:1");                      // GET
v.setIfAbsent("lock:order:1", "token", Duration.ofSeconds(30));  // SETNX（幂等/防重！）
v.setIfPresent("product:1", newP);                  // SETXX

// 原子操作（防超卖/计数核心）
v.increment("stock:1", -1);                          // DECRBY 原子自减
v.increment("counter:views");                        // INCR 自增

// 过期
v.set("session:abc", user, Duration.ofMinutes(30));  // SET + EXPIRE（一条命令）
v.getAndDelete("temp:key");                          // GETDEL
v.getAndExpire("product:1", Duration.ofMinutes(5));  // GETEX（8.0 新）
```

| 面试高频 | 说明 |
|---------|------|
| `set(key, value, TTL)` vs 分两步 | **一条命令**（SET 带 EX 是原子的）；分两步有"TTL 设置失败"窗口 |
| `setIfAbsent`（SETNX） | **幂等/防重/锁的地基**（[05/06 篇](06-分布式锁与Lua脚本速查.md)） |
| `increment`（INCR/DECR） | 原子计数，**禁止读-改-写**（并发丢计数） |

### 2.2 HashOperations（对象字段级操作）

```java
HashOperations<String, String, Object> h = redisTemplate.opsForHash();

h.put("cart:user:1", "sku:1001", 2);                // HSET 单字段
h.putAll("cart:user:1", Map.of("sku:1002", 3));     // HSET 批量
Integer qty = (Integer) h.get("cart:user:1", "sku:1001");   // HGET
Map<Object, Object> all = h.entries("cart:user:1"); // HGETALL
h.increment("cart:user:1", "sku:1001", 1);          // HINCRBY 原子加
h.delete("cart:user:1", "sku:1002");                // HDEL
h.size("cart:user:1");                              // HLEN

// Redis 8.0+ 新命令（3.x 起可用，需服务端 8.0）
Object v1 = h.getAndDelete("cart:user:1", "sku:1001");      // HGETDEL（取走即删）
Object v2 = h.getAndExpire("cart:user:1", "sku:1001", Duration.ofMinutes(5)); // HGETEX
h.putIfAbsent("cart:user:1", "sku:1003", 1);               // HSETEX 配合（HSETNX 语义）
```

> 💡 Hash 的适用：**"对象但只改部分字段"**（购物车数量、会话属性）——HINCRBY 原子改单字段，不用整个对象读写（对比 Value 存 JSON 每次全量序列化）。

## 3. List / Set / ZSet 操作速查

### 3.1 ListOperations（队列/栈）

```java
ListOperations<String, Object> l = redisTemplate.opsForList();

l.leftPush("queue:mail", mail1);                    // LPUSH 入队（左进）
Object m = l.rightPop("queue:mail", Duration.ofSeconds(5));  // BRPOP 阻塞取出（右出）
l.rightPushAll("list:hot", a, b, c);                // RPUSH 批量
List<Object> range = l.range("list:hot", 0, 9);     // LRANGE 分页取
l.trim("list:hot", 0, 99);                          // LTRIM 截断（只留 100 条）
l.size("queue:mail");                               // LLEN 积压量
```

> ⚠️ 队列适用边界：List 当队列（LPUSH+BRPOP）是**轻量任务队列**——无 ACK、无重试、无死信；需要可靠消费 → Stream（[09 篇](09-消息Pub-Sub与Stream速查.md)）。

### 3.2 SetOperations（去重/标签）

```java
SetOperations<String, Object> s = redisTemplate.opsForSet();

s.add("tag:phone", "product:1", "product:2");       // SADD
Set<Object> members = s.members("tag:phone");       // SMEMBERS
Boolean is = s.isMember("tag:phone", "product:1");  // SISMEMBER
s.remove("tag:phone", "product:2");                 // SREM
s.intersect("tag:phone", "tag:new");                // SINTER 共同（标签交集）
s.union("tag:phone", "tag:new");                    // SUNION
s.size("tag:phone");                                // SCARD
```

### 3.3 ZSetOperations（排行榜/延迟任务）

```java
ZSetOperations<String, Object> z = redisTemplate.opsForZSet();

z.add("rank:game:1", "user:1", 100.0);              // ZADD 带分数
z.incrementScore("rank:game:1", "user:1", 10.0);    // ZINCRBY 分数 +10（原子）
Set<ZSetOperations.TypedTuple<Object>> top = z.reverseRangeWithScores("rank:game:1", 0, 9);
                                                    // ZREVRANGE 前 10（含分数）
Long rank = z.reverseRank("rank:game:1", "user:1"); // ZREVRANK 我的名次（0 开始）
Set<Object> byScore = z.rangeByScore("rank:game:1", 90, 100);  // ZRANGEBYSCORE 分数区间
z.removeRange("rank:game:1", 1000, -1);             // ZREMRANGEBYRANK 清理尾部

// 延迟任务：分数 = 执行时间戳
z.add("task:delay", "task:1", System.currentTimeMillis() + 60000);
Set<Object> due = z.rangeByScore("task:delay", 0, System.currentTimeMillis());  // 到期任务
```

> 🎯 面试必答：**"排行榜怎么实现？"**——ZSet：`ZADD 分数` + `ZINCRBY 加分` + `ZREVRANGE 取前 N`（带分数）+ `ZREVRANK 查名次`——O(logN) 复杂度，Redis 原生解决；**同分排序**注意（默认按字典序，需业务处理）；**排行榜过滤**（黑名单）用 ZREM。

## 4. Stream 操作与 8.4 增强

```java
StreamOperations<String, Object, Object> st = redisTemplate.opsForStream();

// 生产者
RecordId id = st.add(StreamRecords.mapToString(Map.of("orderId", "1001"))
        .withStreamKey("stream:order"));            // XADD

// 消费者（消费组，[09 篇](09-消息Pub-Sub与Stream速查.md) 完整版）
List<MapRecord<String, Object, Object>> msgs = st.read(Consumer.from("group1", "c1"),
        StreamReadOptions.empty().count(10), StreamOffset.create("stream:order",
                ReadOffset.lastConsumed()));        // XREADGROUP
st.ack("stream:order", "group1", msgs.stream().map(MapRecord::getId).toList());  // XACK
```

| 8.4 Stream 增强 | 说明 |
|----------------|------|
| `XREADGROUP CLAIM min-idle-time` | 8.4：认领空闲 pending + 新消息一步完成（消费恢复更高效） |
| 用途 | 消息可靠消费（ACK 语义接近 MQ，[09 篇](09-消息Pub-Sub与Stream速查.md) 选型） |

> 💡 Stream vs List 队列：Stream 有消费组/ACK/死信思路（接近 MQ），List 队列轻量无保障——**可靠消费选 Stream**（[09 篇](09-消息Pub-Sub与Stream速查.md)）。

## 5. Redis 8.4 新命令（面试加分项）

> Redis 8.x 新命令在 Spring Data Redis 4.x 以类型化方法直接可用——**用官方新能力替代"先 GET 再 SET"的两次往返**，是 2026 面试官期待听到的答案。

### 5.1 条件 SET/DEL（compare-and-set / compare-and-delete）

```java
// 8.4 条件 SET：CAS 语义（期望值匹配才设置）——替代"GET 对比 + SET"两步
// Redis 命令：SET key value NX GET | XX GET | GT/LT 等
// Spring Data Redis 4.x 条件 API：
Boolean ok = redisTemplate.opsForValue()
        .setIfEquals("counter:1", "10", "11");      // 条件 SET：当前值=10 才设为 11（CAS！）

// 条件 DEL：期望值匹配才删除（compare-and-delete）
// 应用：**并发安全的防重/锁释放**（不用 Lua 也能原子释放锁）
```

| 场景 | 旧做法（两步） | 8.4 新做法（一条） |
|------|--------------|------------------|
| 防重（幂等） | GET 对比 → SET | **条件 SET**（CAS 原子） |
| 锁释放 | GET 对比 → DEL | **条件 DEL**（CAS 原子） |
| 库存扣减校验 | GET → DECR → 校验 | 条件 SET / 原子 DECR + 校验 |

> 🎯 面试必答：**"Redis 8.4 的条件 SET 解决什么问题？"**——把"先 GET 再 SET"两步检查变成**一条原子命令**（compare-and-set）：① 防重场景（幂等标记）免 Lua；② 锁释放场景（检查持有者再删）免 Lua——**两步之间有竞态窗口，条件命令消除窗口**；注意 8.4 条件命令的服务端版本要求（Redis 8.4+，[6.2 版本对照](#62-命令版本对照排错表)）。

### 5.2 DELEX / MSETEX / DIGEST

| 命令 | 语义 | 场景 |
|------|------|------|
| `DELEX key [key...] [NX/XX/GT/LT]` | **带条件删除**（条件：是否存在/过期时间比较） | 条件清理、TTL 治理 |
| `MSETEX key value seconds [key value seconds...]` | **批量 SET + 各自 TTL**（一条命令） | 批量会话/批量缓存（替代 N 次 SETEX） |
| `DIGEST key` | **内容完整性校验**（哈希指纹） | 数据校验/对账（Spring Data Redis 4.1 已支持连接层/模板层） |

```java
// MSETEX（4.1 支持）：一次批量写多个带 TTL 的 key
// 场景：批量初始化会话/批量写缓存——少 N-1 次 RTT（[07 篇](07-管道事务与批量性能速查.md) 批量对照）
```

### 5.3 FT.HYBRID（全文+向量混合检索，8.4）

```text
Redis 8.4 的 FT.HYBRID：全文检索 + 向量相似度 一条命令融合（RRF/线性加权）
场景：语义缓存（[05 篇](05-缓存实战：Spring-Cache与一致性速查.md) 6 节）、AI 上下文检索
注意：需要 RediSearch 模块（8.4 起开源 Redis 集成）；Java 侧走 spring-data-redis search 包或 RediSearch 客户端
```

## 6. 函数式 API 与版本对照

### 6.1 函数式/流式 API（4.x 风格）

```java
// 条件操作（4.x 函数式：SessionCallback/命令回调）
// 场景：需要"读+写"原子组合（无 Lua 时的次优解）
List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) conn -> {
    conn.stringCommands().set(byteKey, byteVal);        // 命令级 API
    conn.stringCommands().get(byteKey);
    return null;
});
```

### 6.2 命令版本对照（排错表）

| 命令/API | 最低服务端版本 | 说明 |
|---------|:---:|------|
| SET NX/XX/EX/PX | 2.6.12+ | 常规 |
| GETDEL / GETEX | 6.2+ | 常规 |
| HGETDEL / HGETEX / HSETEX | **8.0+** | 8.x 新 |
| 条件 SET/DEL（compare-and-set） | **8.4+** | 2025-11 GA |
| DELEX / MSETEX / DIGEST | **8.4+** | 2025-11 GA |
| FT.HYBRID | **8.4 + RediSearch** | 混合检索 |
| Stream 基础（XADD/XREADGROUP） | 5.0+ | 常规 |
| XREADGROUP CLAIM min-idle-time | **8.4** | 认领增强 |

> ⚠️ **版本不匹配的典型症状**：`unknown command 'DELEX'` / `ERR unknown command`——先查服务端版本（`redis-cli info server` 的 redis_version），新命令对老集群直接不可用；**上 8.4 新特性前先确认集群版本与模块**（RediSearch 是否安装）。

---

**下一模块**：[05-缓存实战：Spring Cache与一致性速查](05-缓存实战：Spring-Cache与一致性速查.md)　**返回总览**：[00-组件总览](00-Spring Data Redis组件总览.md)

**【参考来源】**：[Redis 8.4 新特性（官方博客）](https://redis.io/blog/whats-new-in-two-november-2025-edition/)、[Spring Data Redis 官方参考文档（操作 API）](https://docs.spring.io/spring-data/redis/reference/redis/template.html)、[Redis 官方命令参考](https://redis.io/docs/latest/commands/)
