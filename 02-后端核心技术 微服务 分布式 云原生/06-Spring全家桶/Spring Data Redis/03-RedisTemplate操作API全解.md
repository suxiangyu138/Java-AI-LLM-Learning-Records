# 03 RedisTemplate 操作 API 全解

> 六大类型化操作接口（Value/Hash/List/Set/ZSet/Stream）的完整用法、边界与陷阱，外加 Redis 8.0 新命令（HGETDEL/HGETEX/HSETEX）与 Redis 8.4 条件 SET/DEL 在 Spring Data Redis 4.x 中的官方 API——本模块是日常业务开发的"字典"

---

## 📚 目录

1. [Operations 家族总览](#1-operations-家族总览)
2. [ValueOperations：字符串核心操作](#2-valueoperations字符串核心操作)
3. [HashOperations：对象与计数字段](#3-hashoperations对象与计数字段)
4. [ListOperations：队列与栈](#4-listoperations队列与栈)
5. [SetOperations 与 ZSetOperations](#5-setoperations-与-zsetoperations)
6. [StreamOperations：日志型队列](#6-streamoperations日志型队列)
7. [Redis 8.x 新命令与 2026 新 API](#7-redis-8x-新命令与-2026-新-api)
8. [通用操作与边界场景](#8-通用操作与边界场景)

---

## 1. Operations 家族总览

```text
RedisTemplate<K,V>
├── opsForValue()  → ValueOperations<K,V>     字符串（含位图、计数、分布式锁基础）
├── opsForHash()   → HashOperations<K,HK,HV>  哈希（对象字段、计数）
├── opsForList()   → ListOperations<K,V>      列表（队列/栈/消息缓冲）
├── opsForSet()    → SetOperations<K,V>       集合（去重、交并差）
├── opsForZSet()   → ZSetOperations<K,V>      有序集合（排行榜、延迟任务）
├── opsForStream() → StreamOperations<K,HK,HV> 流（消息队列，5.0+）
└── execute(...)   → 回调（脚本、管道、原生命令）
```

| 接口 | 底层 Redis 类型 | 典型场景 |
|------|:---:|---------|
| ValueOperations | string | 缓存、计数器、分布式锁、限流 |
| HashOperations | hash | 对象存取、购物车、配置项、分段计数 |
| ListOperations | list | 简单队列、最近浏览、消息缓冲 |
| SetOperations | set | 去重、标签、共同好友（交并差） |
| ZSetOperations | zset | 排行榜、延迟任务、滑动窗口限流 |
| StreamOperations | stream | 可靠消息队列（消费组） |

> 💡 每个 `opsForXxx()` 每次调用都返回新实例，但内部无状态、可安全复用；常用写法是每次使用时现取现用。

## 2. ValueOperations：字符串核心操作

### 2.1 基础读写

```java
ValueOperations<String, Object> ops = redisTemplate.opsForValue();

// 普通写
ops.set("user:1001", user);

// 带过期时间写（原子，避免"写后忘记设过期"）
ops.set("user:1001", user, Duration.ofMinutes(30));

// 带条件写
ops.setIfAbsent("lock:order:1001", "owner", Duration.ofSeconds(10));   // SET NX PX —— 分布式锁基石
ops.setIfPresent("user:1001", newUser);                                // SET XX（仅存在时更新）

// 读
Object v = ops.get("user:1001");
```

### 2.2 计数与字符串操作

```java
Long n = ops.increment("counter:visit");        // INCR，原子自增
Long n2 = ops.increment("counter:score", 5);    // INCRBY，可正可负
String sub = ops.get("str:key", 0, 5);          // GETRANGE 子串
ops.append("str:key", "tail");                  // APPEND 追加
```

> ⚠️ **一致性要点**：`set(k, v)` 后单独再调 `expire(k, ttl)` 是两步操作，两步之间若进程崩溃，key 成为永久 key（内存泄漏）。**必须用带 TTL 的单参数重载**。同理，判断分布式锁用 `setIfAbsent`（SET NX PX 原子），**不要用"先 setnx 再 expire"两段式**。

### 2.3 位图

```java
// 签到：user:1001:sign:202608 的 bit 7 记为已签到
ops.setBit("user:1001:sign:202608", 7, true);
Boolean signed = ops.getBit("user:1001:sign:202608", 7);
```

## 3. HashOperations：对象与计数字段

```java
HashOperations<String, String, Object> ops = redisTemplate.opsForHash();

// 单字段
ops.put("cart:1001", "sku:88", "2");
Object qty = ops.get("cart:1001", "sku:88");

// 批量
Map<String, Object> map = Map.of("sku:88", "2", "sku:99", "5");
ops.putAll("cart:1001", map);

// 计数（HSET 上的原子自增，推荐替代"get→put"）
Long n = ops.increment("cart:1001", "sku:88", 1);

// 全部取出
Map<String, Object> all = ops.entries("cart:1001");

// 删除字段
Long deleted = ops.delete("cart:1001", "sku:88");
```

> 🎯 **要点**：hash 的 `increment(field)` 是原子操作，**不要在业务代码里"先 get 再 put 自加"**——并发下必然丢更新。对象存储优先 hash（可改单字段）而非整体 JSON（改一个字段要整读整写）。

## 4. ListOperations：队列与栈

```java
ListOperations<String, Object> ops = redisTemplate.opsForList();

// 入队（右侧）/ 出队（左侧）→ FIFO 队列
ops.rightPush("task:queue", task);
Object task = ops.leftPop("task:queue");

// 阻塞弹出（任务队列推荐：无任务时挂起而非空转）
Object t = ops.leftPop("task:queue", Duration.ofSeconds(30));

// 限量列表（固定长度，用 ltrim 裁剪）
ops.rightPush("history:1001", "item-3");
ops.trim("history:1001", 0, 9);      // 只保留最近 10 条

// 范围读取
List<Object> list = ops.range("history:1001", 0, -1);
```

| API | 语义 | 注意 |
|-----|------|------|
| `rightPush` / `leftPop` | FIFO 队列 | 生产消费 |
| `rightPush` / `rightPop` | LIFO 栈 | 后进先出 |
| `leftPush` + 阻塞变体 | 高并发入队侧 | 配合消费者数设计 |
| `leftPop(key, timeout)` | 阻塞弹出（BLPOP） | **超时前返回 null，需判空** |

> ⚠️ **阻塞弹出陷阱**：`leftPop(key, Duration)` 在等待期间占用连接（Lettuce 共享连接下会独占），高并发消费者场景注意超时不宜过长；可靠性要求高请直接用 Stream（08 篇）。

## 5. SetOperations 与 ZSetOperations

### 5.1 Set：去重与交并差

```java
SetOperations<String, Object> ops = redisTemplate.opsForSet();

ops.add("tag:java", "user1", "user2");
ops.add("tag:redis", "user2", "user3");

Set<Object> common = ops.intersect("tag:java", "tag:redis");   // 共同关注：{user2}
Set<Object> union = ops.union("tag:java", "tag:redis");        // 并集
Set<Object> diff  = ops.difference("tag:java", "tag:redis");   // 差集：{user1}

Boolean isMember = ops.isMember("tag:java", "user1");          // SISMEMBER O(1)
Long size = ops.size("tag:java");
```

### 5.2 ZSet：排行榜与延迟任务

```java
ZSetOperations<String, Object> ops = redisTemplate.opsForZSet();

// 加分数 / 设置分数
Double s = ops.incrementScore("rank:game:1001", "player:1", 10);   // ZINCRBY 原子
ops.add("rank:game:1001", "player:2", 50);

// 排行榜（分数从高到低，取前三名）
Set<ZSetOperations.TypedTuple<Object>> top3 = ops.reverseRangeWithScores("rank:game:1001", 0, 2);

// 排名
Long rank = ops.reverseRank("rank:game:1001", "player:1");   // 第几名（0 起）

// 延迟任务：分数=执行时间戳，轮询取到期项
long deadline = System.currentTimeMillis();
Set<Object> due = ops.rangeByScore("delay:task", 0, deadline);   // 已到期任务
```

> 🎯 **要点**：ZSet 是"排行榜 + 延迟队列 + 滑动窗口限流"的统一实现；`incrementScore` 原子自增避免了"读分→加分→写回"的丢更新问题。

## 6. StreamOperations：日志型队列

Redis Stream（5.0+）是 Redis 官方的消息队列：可持久、支持消费组、可回放。Spring Data Redis 3.x 起完整支持：

```java
StreamOperations<String, Object, Object> ops = redisTemplate.opsForStream();

// 生产：XADD
RecordId id = ops.add(ObjectRecord.create("stream:orders", order));  // 自动生成 ID

// 消费组：创建组
ops.createGroup("stream:orders", "group:notify");

// 消费：XREADGROUP（阻塞 5 秒，读本组未 ACK 的新消息）
List<ObjectRecord<String, Object>> records =
        ops.read(Consumer.from("group:notify", "consumer-1"),
                StreamReadOptions.empty().count(10).block(Duration.ofSeconds(5)),
                StreamOffset.create("stream:orders", ReadOffset.lastConsumed()));

// 确认：XACK（消费成功后必须确认，否则消息一直处于 pending）
for (ObjectRecord<String, Object> record : records) {
    handle(record.getValue());
    ops.acknowledge("stream:orders", "group:notify", record.getId());
}
```

| Stream 概念 | Spring API | 说明 |
|------------|-----------|------|
| XADD 生产 | `ops.add(ObjectRecord)` | 消息追加到流 |
| 消费组 | `ops.createGroup` / `Consumer.from` | 组内消息被成员瓜分 |
| XREADGROUP | `ops.read(consumer, options, offset)` | 阻塞读 + pending 机制 |
| XACK | `ops.acknowledge` | 消费成功后确认，防重复投递 |
| 死信查看 | `ops.pending(...)` | pending 列表即"未确认/处理中"清单 |

> 💡 生产级消息消费更推荐 08 篇的 `StreamMessageListenerContainer`（自动轮询 + 错误处理 + 并发控制），此处 API 用于理解底层语义。

## 7. Redis 8.x 新命令与 2026 新 API

### 7.1 Redis 8.0 新 hash 命令（Spring Data Redis 4.0 引入）

| 新命令 | 语义 | 旧替代方式（两次往返） | Spring 4.x API |
|--------|------|----------------------|----------------|
| `HGETDEL` | 取字段值并删除该字段 | GET + HDEL | `hashOps.getAndDelete(key, field)` |
| `HGETEX` | 取字段值并重置过期时间 | GET + HEXPIRE | `hashOps.getAndExpire(key, field, ttl)` |
| `HSETEX` | 写字段并设过期时间 | HSET + HEXPIRE | `hashOps.set(key, field, value, ttl)` |

```java
// 一次性实现"取出旧值并删除"（如一次性优惠码校验）
Object code = hashOps.getAndDelete("coupon:batch:1", "code:XYZ");

// 会话续期："读 session 并顺带续 30 分钟"
Object session = hashOps.getAndExpire("session:user:1001", "token", Duration.ofMinutes(30));

// 写值并设过期（原子）
hashOps.set("session:user:1001", "token", newToken, Duration.ofMinutes(30));
```

> 🎯 **价值**：这些命令把"读+删""读+续期"从两次网络往返压缩为一次，且消除了两步之间的竞态窗口——Redis 8.0 + Spring Data Redis 4.x 组合下推荐优先使用。

### 7.2 Redis 8.4 条件 SET / DEL（2026 新特性）

Redis 8.4 支持**值级条件**的 SET/DEL：`SET key val IF <condition>` / `DEL key IF <condition>`，把"先读值比较再写"的两次往返与竞态窗口消除。Spring Data 2026.0.0（4.1.x）提供函数式 API：

```java
ValueOperations<String, Object> ops = redisTemplate.opsForValue();

// 比较并设置：仅当旧值等于 expected 时才写新值（乐观锁语义）
ops.set("stock:1001", 99, spec -> spec
        .ifEquals().value(100)                     // 条件：旧值 == 100
        .expire(Duration.ofSeconds(30)));          // 顺带设置过期

// 比较并删除：仅当值匹配时才删除
ops.delete("session:1001", spec -> spec
        .ifEquals().value("token-abc"));
```

**与旧方案的对比：**

| 方案 | 往返次数 | 竞态窗口 | 适用 |
|------|:---:|:---:|------|
| GET 比较 → SET/DEL（两段式） | 2 | 有（先读后写间被并发改） | 旧版 Redis |
| Lua 脚本（compare and set） | 1 | 无 | Redis 2.6+ 通用 |
| Watch 事务 | 2+ | 无（失败重试） | 低并发 |
| **条件 SET/DEL（Redis 8.4+）** | **1** | **无** | **Redis 8.4+ 专用** |

> 💡 生产注意：条件 SET/DEL 是 Redis 8.4 服务端能力，需确认线上 Redis 版本；兼容旧版本的通用原子方案仍是 Lua 脚本（见 04 篇）。

## 8. 通用操作与边界场景

### 8.1 通用命令（ValueOperations 之外）

```java
// key 级操作
redisTemplate.delete("user:1001");                    // DEL
Boolean exists = redisTemplate.hasKey("user:1001");   // EXISTS
redisTemplate.expire("user:1001", Duration.ofHours(1)); // 单独设过期（注意 2.2 节的一致性警告）
Set<String> keys = redisTemplate.keys("user:*");      // KEYS（生产禁用，用 SCAN）
redisTemplate.rename("user:1001", "user:1002");       // RENAME

// 类型判断
DataType type = redisTemplate.type("cart:1001");
```

> ⚠️ `keys("user:*")` 会阻塞 Redis 单线程全库扫描——生产环境禁止，改走 `scan`（`RedisConnection.scan()` 回调）或维护索引 key。

### 8.2 边界与陷阱速查

| 边界场景 | 现象 | 正确处理 |
|---------|------|---------|
| key 不存在时 `get` | 返回 null | 与"值为 null"区分：hash 用 `hasKey` |
| value 序列化为 null | `serialize(null)` 返回 null，命令被跳过 | 业务层先判空 |
| 数值类型混用 | `increment` 对非数字字符串报错 | 保证同 key 类型一致 |
| TTL 一致性 | 分步 set+expire 有崩溃窗口 | 用带 TTL 的原子 API |
| 大 key | 单命令阻塞 Redis | 拆分 key / 用 hash 分段 |
| `keys`/`smembers` 全量 | 阻塞与内存峰值 | scan / 分批 |
| 阻塞 pop 超时 | 返回 null | 判空 + 记录 WARN 日志 |

> 🎯 **核心要点**：操作 API 层牢记三条主线——①**原子性优先**：带条件的 set、increment、Redis 8.x 新命令，能一次往返绝不两段式；②**类型即纪律**：同 key 的 value 类型一旦确定不可混用；③**批量与阻塞**：全量命令（KEYS/SMEMBERS）与长阻塞（BLPOP）是性能暗雷，大 key 必须拆分。进阶批量能力见 05 篇管道与事务。

---

**上一模块**：[02-序列化器体系深度剖析](02-序列化器体系深度剖析.md)　**下一模块**：[04-StringRedisTemplate与Lua脚本](04-StringRedisTemplate与Lua脚本.md)
