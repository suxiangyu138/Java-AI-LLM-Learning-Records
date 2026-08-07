# 04 StringRedisTemplate 与 Lua 脚本

> StringRedisTemplate 是"无序列化心智负担"的专用模板；Lua 脚本则让复杂原子操作在服务端一次完成——两者叠加，能优雅实现分布式限流、原子扣减、防抖等生产高频场景

---

## 📚 目录

1. [StringRedisTemplate：与 RedisTemplate 的分工](#1-stringredistemplate与-redistemplate-的分工)
2. [Lua 脚本机制与 Redis 的执行模型](#2-lua-脚本机制与-redis-的执行模型)
3. [DefaultRedisScript 使用全解](#3-defaultredisscript-使用全解)
4. [脚本缓存与 evalsha](#4-脚本缓存与-evalsha)
5. [经典脚本实战](#5-经典脚本实战)
6. [Lua 陷阱与安全边界](#6-lua-陷阱与安全边界)

---

## 1. StringRedisTemplate：与 RedisTemplate 的分工

```java
@Bean  // Spring Boot 自动配置已提供，这里展示定位
public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
    return new StringRedisTemplate(factory);
}
```

**本质：一个 key/value 全部使用 `StringRedisSerializer` 的 RedisTemplate。** 因此：

| 对比项 | RedisTemplate | StringRedisTemplate |
|--------|:---:|:---:|
| key 序列化 | 默认 JDK（需显式改 String） | 固定 String |
| value 序列化 | 默认 JDK（需显式改 JSON 等） | 固定 String |
| 存储内容 | 任意 Java 对象 | 字符串（数字/JSON 串/布尔需自行转换） |
| 读写一致 | 与自定义序列化器绑定 | 与 redis-cli 直接互读 |
| 适用场景 | 对象缓存、多态数据 | 计数、限流、分布式锁、与运维脚本互通 |

**关键认知：两者操作的是同一 Redis，但序列化规则不同。** 用 `RedisTemplate`（JSON）写入的 key，用 `StringRedisTemplate` 读会得到 JSON 字符串（可读、但仍是字符串）；用 JDK 序列化的 key，两边都读不了。

> ⚠️ **混用事故**：同一业务 key 绝不能一会儿用 RedisTemplate（对象）写、一会儿用 StringRedisTemplate 写——一个 key 两种序列化产物，数据互相覆盖且语义错乱。**按 key 前缀划分职责**：`cache:*` 走对象模板，`counter:*`/`lock:*` 走字符串模板。

```java
@Resource
private StringRedisTemplate stringRedisTemplate;

// 典型用途：计数器（返回 Long，原子）
Long visit = stringRedisTemplate.opsForValue().increment("counter:visit");

// 典型用途：分布式锁（SET NX PX）
Boolean locked = stringRedisTemplate.opsForValue()
        .setIfAbsent("lock:pay:1001", "instance-1", Duration.ofSeconds(10));
```

## 2. Lua 脚本机制与 Redis 的执行模型

**Lua 脚本为什么能保证原子：** Redis 是单线程命令处理器，`EVAL` 执行脚本期间整个脚本作为一个单元运行，**期间没有其它命令能插入**——相当于把多条命令打包成一条"原子命令"。

**适合用脚本解决的场景：** 需要"读-判断-写"三步且不允许中间被打断的逻辑（库存扣减、限流计数、防抖、先比较后删除）。

**执行模型：**

```text
应用层                              Redis 服务端
  ┌────────────────┐  EVAL script keys args ──►  ┌──────────────────────┐
  │ DefaultRedisScript│─────────────────────────►  │ 单线程执行整个脚本      │
  │ 脚本 + 参数        │◄────────────── 返回结果 ── │ 期间无其它命令插入      │
  └────────────────┘                             └──────────────────────┘
```

**脚本内可用 API：**

```lua
redis.call('SET', KEYS[1], ARGV[1])      -- 执行命令（出错抛异常，回滚整个脚本）
redis.call('EXPIRE', KEYS[1], ARGV[2])
redis.pcall('GET', KEYS[1])              -- 容错版：出错返回 err 表而非终止
```

> 💡 `call` vs `pcall`：`call` 出错会终止脚本并向客户端抛错；`pcall` 捕获错误返回 err 表。**脚本内错误会让已执行部分生效吗？** 会——Redis 对脚本不做整体回滚（与事务不同），已写的 key 保留。所以脚本逻辑要写对。

## 3. DefaultRedisScript 使用全解

### 3.1 完整示例：原子扣减库存

```java
// 1. 定义脚本（static 常量，避免每次构建）
private static final String DECR_STOCK_SCRIPT =
        "if redis.call('EXISTS', KEYS[1]) == 1 then " +
        "  local stock = redis.call('GET', KEYS[1]); " +
        "  if tonumber(stock) >= tonumber(ARGV[1]) then " +
        "    return redis.call('DECRBY', KEYS[1], ARGV[1]); " +
        "  end " +
        "  return -1; " +
        "else " +
        "  return -2; " +
        "end";

// 2. 封装为 DefaultRedisScript（可复用，线程安全）
@Bean
public DefaultRedisScript<Long> decrStockScript() {
    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    script.setScriptText(DECR_STOCK_SCRIPT);
    script.setResultType(Long.class);    // 返回值类型——反序列化依据
    return script;
}

// 3. 执行
@Resource
private StringRedisTemplate stringRedisTemplate;

public Long decrStock(String key, int qty) {
    return stringRedisTemplate.execute(
            decrStockScript(),            // 脚本
            List.of(key),                 // KEYS
            String.valueOf(qty));         // ARGV（脚本内用 ARGV[1] 引用）
}
```

### 3.2 参数与返回值规则

| 环节 | 规则 |
|------|------|
| KEYS | 用 List 传入，脚本内 `KEYS[1]` 引用；**不允许硬编码在脚本里**（集群模式下无法路由） |
| ARGV | 变长参数，脚本内 `ARGV[1]` 起 |
| 返回值 | `setResultType(...)` 声明；`Long`/`Integer`/`Boolean`/`String`/`List` 常用 |
| 返回 null | 脚本返回 nil → Java 得到 null |
| 多结果 | `ResultType.LIST` 或自定义 `List<...>` |

> ⚠️ **集群注意**：脚本中所有 KEYS 必须属于同一个 hash slot（Redis Cluster 按 key 路由）。跨 key 脚本要么保证 key 同槽（`{user:1001}` 花括号哈希标签），要么拆脚本。

## 4. 脚本缓存与 evalsha

**执行链路优化：** 每次 `EVAL` 都传输完整脚本文本。Redis 提供 `SCRIPT LOAD` 返回 SHA1，之后用 `EVALSHA` 只传哈希：

```text
首次执行：EVAL script（传全文）  →  SCRIPT LOAD 缓存脚本，拿到 sha
后续执行：EVALSHA sha（只传哈希，省流量）
```

Spring Data Redis 的 `DefaultRedisScript` 内置了这段优化：

```java
// 首次 execute 时若未加载过，自动 SCRIPT LOAD 并缓存 sha
// 后续 execute 直接走 EVALSHA；若脚本被 FLUSH（SCRIPT FLUSH），自动回退 EVAL 重载
public Long decrStock(String key, int qty) {
    return stringRedisTemplate.execute(decrStockScript(), List.of(key), String.valueOf(qty));
}
```

> 💡 **机制细节**：Spring 的 `DefaultRedisScript` 在每次 execute 时会先尝试 EVALSHA，命中缓存则只传 20 字节哈希；未命中（`NOSCRIPT` 错误）自动降级 EVAL。**对应用完全透明**，无需手工管理脚本缓存。

## 5. 经典脚本实战

### 5.1 固定窗口限流（原子计数 + 过期）

```java
private static final String RATE_LIMIT_SCRIPT =
        "local c = redis.call('INCR', KEYS[1]); " +
        "if c == 1 then " +
        "  redis.call('EXPIRE', KEYS[1], ARGV[1]); " +   // 第一个请求设置窗口
        "end " +
        "if c > tonumber(ARGV[2]) then return 0 end " +
        "return 1";

public boolean tryAcquire(String userId, int limit, int windowSeconds) {
    Long ok = stringRedisTemplate.execute(rateLimitScript(),
            List.of("ratelimit:" + userId), String.valueOf(windowSeconds), String.valueOf(limit));
    return ok != null && ok == 1L;
}
```

> 💡 **注意**：固定窗口在窗口边界会突刺（窗口末段+下窗口初段可双倍通过）。严格限流改用**滑动窗口**（ZSet 时间戳方案）或 05 篇的 Redis 模块内实现，但多数场景固定窗口已足够。

### 5.2 防抖（限流：N 秒内只放行一次）

```java
private static final String DEBOUNCE_SCRIPT =
        "if redis.call('SET', KEYS[1], '1', 'NX', 'EX', ARGV[1]) then " +
        "  return 1 " +     // 首次，放行
        "else " +
        "  return 0 " +     // 窗口内重复请求，拦截
        "end";
```

### 5.3 分布式锁：加锁 + 续期（看门狗）

```java
// 加锁（原子）
Boolean locked = stringRedisTemplate.opsForValue()
        .setIfAbsent("lock:" + key, ownerId, Duration.ofSeconds(10));

// 续期（脚本：仅当持有者是自己时延长 TTL——防误续他人锁）
private static final String RENEW_SCRIPT =
        "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
        "  return redis.call('EXPIRE', KEYS[1], ARGV[2]) " +
        "else " +
        "  return 0 " +
        "end";

// 释放（脚本：先比对持有者再删——防误删他人锁）
private static final String UNLOCK_SCRIPT =
        "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
        "  return redis.call('DEL', KEYS[1]) " +
        "else " +
        "  return 0 " +
        "end";
```

> 🎯 **要点**：分布式锁三个原子动作（加锁、续期、释放）全部走脚本/原子命令，**任何"先判断后操作"的分步实现都有误删/误续风险**。生产可直接用 Redisson（看门狗内置），但面试要能讲出底层脚本。

## 6. Lua 陷阱与安全边界

| 陷阱 | 说明 | 规避 |
|------|------|------|
| 脚本过长阻塞 | 脚本执行期间 Redis 单线程被占，耗时脚本拖垮全局 | 脚本只做 O(1)/O(log N) 操作，严禁循环大集合 |
| 死循环风险 | `while true` 会让 Redis 卡死 | 脚本逻辑务必有限步；超时只能 SHUTDOWN 恢复（redis 7+ 有 timeout 保护可配） |
| 不读旧数据写新数据 | 脚本内读的 key 若在脚本外被改，仍按单线程快照语义 | 所有涉及的 key 都应作为 KEYS 传入 |
| 随机性 | `redis.call('TIME')`/随机函数使脚本不可复制 | 生产告警一般不用复制功能，风险可控 |
| 集群跨 slot | 多 key 脚本需同 slot | `{tag}` 哈希标签 |
| 脚本无回滚 | call 出错前已写入的 key 不回滚 | 逻辑自洽，先校验后写入 |
| 权限与安全 | 脚本内可执行任意命令 | 只用白名单命令；禁止 `CONFIG`/`FLUSHALL` |
| 超时配置 | 7.x 起 `busy-reply-threshold` 可限制脚本时长 | 配 busy-reply-threshold 兜底 |

**脚本调试三件套：**

```bash
redis-cli EVAL "return redis.call('GET', KEYS[1])" 1 mykey   # 手测脚本
redis-cli SCRIPT LOAD "..."                                # 手动加载看 sha
redis-cli --eval ./myscript.lua key1 key2 , arg1 arg2       # 文件调试（注意逗号分隔）
```

> 🎯 **核心要点**：StringRedisTemplate 解决"读写一致与可读性"，Lua 脚本解决"复杂逻辑的原子性"。三句话记忆——①脚本把多步操作压成一次原子执行；②KEYS 必须参数化传入（集群路由依据）；③脚本里只做轻量操作，重逻辑是阻塞事故源。生产高频原子场景（限流/防抖/锁/扣减）都有标准脚本模板，掌握后一通百通。

---

**上一模块**：[03-RedisTemplate操作API全解](03-RedisTemplate操作API全解.md)　**下一模块**：[05-管道、事务与批量性能优化](05-管道、事务与批量性能优化.md)
