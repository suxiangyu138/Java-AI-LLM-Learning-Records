# 06 分布式锁与 Lua 脚本速查

> Lua 脚本机制（DefaultRedisScript）、分布式锁四方案对比（SETNX/Lua/Redisson/条件命令）、限流与防重实战、8.4 条件命令对锁的影响——"原子性在三方的两种实现"完整手册

---

## 📚 目录

1. [Lua 脚本：Redis 原子性的万能工具](#1-lua-脚本redis-原子性的万能工具)
2. [分布式锁四方案对比（面试必考）](#2-分布式锁四方案对比面试必考)
3. [锁的最佳实现（含释放安全）](#3-锁的最佳实现含释放安全)
4. [限流与防重实战](#4-限流与防重实战)
5. [8.4 条件命令对锁的影响](#5-84-条件命令对锁的影响)
6. [锁的边界与红线](#6-锁的边界与红线)

---

## 1. Lua 脚本：Redis 原子性的万能工具

**通俗**：Redis 单线程执行脚本——**一个 Lua 脚本内多条命令整体原子**（执行期间无其他命令插入），这是"读-判-写"组合原子性的官方解法。

```java
// DefaultRedisScript：注册脚本（evalsha 自动缓存）
@Configuration
public class LuaScriptConfig {
    // 经典：扣库存（读库存 → 判断 → 扣减，整体原子）
    @Bean
    DefaultRedisScript<Long> deductStockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText("""
            local stock = tonumber(redis.call('GET', KEYS[1]))
            if stock == nil or stock < tonumber(ARGV[1]) then
                return -1                                -- 库存不足
            end
            redis.call('DECRBY', KEYS[1], ARGV[1])
            return stock - tonumber(ARGV[1])             -- 返回扣后库存
            """);
        script.setResultType(Long.class);
        return script;
    }
}

// 执行
Long result = redisTemplate.execute(deductStockScript,
        List.of("stock:sku:1001"), "3");
// -1 = 库存不足；>=0 = 扣后库存（整个读-判-写原子完成）
```

| 要点 | 说明 |
|------|------|
| 原子性 | 单线程执行整个脚本，期间无其他命令（等价 multi/exec 但**可编程**） |
| 参数 | `KEYS`（key 数组）/ `ARGV`（参数数组）——集群下 KEYS 必须同 slot |
| 返回值 | `setResultType`（Long/String/List） |
| 性能 | 首次 EVAL，之后 EVALSHA（脚本哈希）——**脚本内容别拼接参数**（参数走 ARGV） |
| 沙箱 | 无文件 IO/网络（安全）；脚本禁循环死锁（超时被 kill） |

> 🎯 面试必答：**"为什么 Lua 脚本是原子的？"**——Redis 是**单线程**执行命令（8.4 部分多线程但脚本执行仍单线程）；一个脚本从第一条到最后一条**期间不会有其他命令插入**（等价于一把全局锁）——所以"读-判-写"组合（扣库存、释放锁前验证、限流窗口）都能在脚本内原子完成；**脚本不宜过长**（长时间执行会阻塞其他命令）。

## 2. 分布式锁四方案对比（面试必考）

| 方案 | 实现 | 原子性 | 可靠性 | 适用 |
|------|------|:---:|:---:|------|
| ① SETNX 简单锁 | `setIfAbsent(key, token, TTL)` + 手动 DEL | ⚠️（释放有窗口） | 中 | 演示/轻量 |
| ② **SETNX + Lua 释放** | 加锁 SETNX；释放用 Lua（验证 token 再删） | ✅ | 高 | **自研标准方案** |
| ③ **Redisson** | 成熟锁库（看门狗续期/可重入/公平锁） | ✅ | 最高 | **生产推荐** |
| ④ 8.4 条件 DEL | `setIfEquals`/条件 DEL（CAS 原子） | ✅ | 高（无 Lua） | 8.4 新集群（[05 篇](05-缓存实战：Spring-Cache与一致性速查.md) 相关） |

> 🎯 面试必答：**"Redis 分布式锁怎么做？"**——完整答案四层：① 加锁：`SET key token NX EX 30`（**value 必须是唯一 token**，防误删他人锁）；② 释放：**Lua 验证 token 再 DEL**（防"锁过期后删了别人的锁"）；③ 续期：Redisson 看门狗自动续期（防业务未完成锁先过期）；④ 边界：**主从切换时锁可能丢失**（极端场景用 Redlock 或换 etcd/ZK）——四层讲全，面试通关。

## 3. 锁的最佳实现（含释放安全）

### 3.1 自研标准锁（方案②）

```java
// 加锁：SET key token NX EX（原子，token 唯一）
public boolean tryLock(String key, String token, Duration ttl) {
    return Boolean.TRUE.equals(redisTemplate.opsForValue()
            .setIfAbsent(key, token, ttl));
}

// 释放：Lua 验证 token 再删（防误删）
private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>("""
        if redis.call('GET', KEYS[1]) == ARGV[1] then
            return redis.call('DEL', KEYS[1])
        else
            return 0
        end
        """, Long.class);

public boolean unlock(String key, String token) {
    Long r = redisTemplate.execute(UNLOCK_SCRIPT, List.of(key), token);
    return r != null && r == 1;
}

// 使用
String token = UUID.randomUUID().toString();
if (tryLock("lock:order:1", token, Duration.ofSeconds(30))) {
    try {
        // 业务（30s 内完成；长任务需续期——自研复杂，用 Redisson）
    } finally {
        unlock("lock:order:1", token);      // 必须 finally 释放
    }
}
```

| 红线 | 说明 |
|------|------|
| value 必须唯一 token | 无 token 的锁释放 = 可能删掉别人的锁 |
| 释放必须 Lua 验证 | "先 GET 对比再 DEL"两步有竞态（对比后、删除前锁过期被他人拿走） |
| 必须 finally | 异常也要释放（否则死锁到 TTL） |
| TTL 要够 | 业务超时 > TTL = 锁提前失效，别人进来（长任务用 Redisson 看门狗） |

### 3.2 Redisson（生产推荐，省心版）

```java
// 依赖：org.redisson:redisson-spring-boot-starter（配 spring.data.redis 自动连）
@Autowired RedissonClient redisson;

public void doLocked(String key, Runnable task) {
    RLock lock = redisson.getLock("lock:" + key);
    lock.lock(10, TimeUnit.SECONDS);          // 或 lock.lock()：看门狗自动续期（默认 30s 续）
    try {
        task.run();
    } finally {
        lock.unlock();                        // 只有持有者能解锁（内建 token 语义）
    }
}
```

| Redisson 特性 | 说明 |
|--------------|------|
| 看门狗 | 锁快过期自动续期（业务没跑完锁不丢）——**自研锁的最大痛点** |
| 可重入 | 同线程重复加锁（RLock 重入计数） |
| 公平锁/读写锁 | 场景化变体（FairLock/ReadWriteLock） |
| 红锁 | RedissonRedLock（多节点，极端可靠场景） |

> 🎯 选型一句话：**自研锁能讲清原理（面试），生产用 Redisson（省心）**——面试答"原理版"（SETNX+Lua+token），项目答"Redisson 版"（看门狗+可重入）——两个版本都答得出才是完整答案。

## 4. 限流与防重实战

### 4.1 固定窗口限流（Lua）

```java
// 固定窗口：每 1 秒最多 N 次（Lua 原子）
DefaultRedisScript<Long> rateLimitScript = new DefaultRedisScript<>("""
        local cnt = redis.call('INCR', KEYS[1])
        if cnt == 1 then
            redis.call('EXPIRE', KEYS[1], ARGV[1])     -- 首次设置窗口过期
        end
        if cnt > tonumber(ARGV[2]) then
            return 0                                    -- 超限
        end
        return 1
        """, Long.class);

// key: "rl:user:123" 窗口 1s 限 5 次
Boolean allowed = redisTemplate.execute(rateLimitScript, List.of("rl:user:123"), "1", "5") == 1;
```

> 💡 限流算法（固定窗口/滑动窗口/令牌桶/漏桶）的 Redis 实现见 [名词系列-流量治理](../../../../09-Web开发全流程/后端 分布式常用名词通俗解释/02-流量治理：限流、削峰与背压.md)；生产级限流直接上 **Sentinel**（[限流落地](../../../../09-Web开发全流程/后端 分布式常用名词通俗解释/02-流量治理：限流、削峰与背压.md) 3.2 节）——Redis Lua 限流适合"轻量自定义"场景。

### 4.2 防重（幂等）三姿势

| 姿势 | 实现 | 适用 |
|------|------|------|
| SETNX 标记 | `setIfAbsent("idem:order:1001", "1", 30min)` | 通用防重（[幂等方案](../../../../09-Web开发全流程/后端 分布式常用名词通俗解释/04-重复请求治理：幂等与防抖.md) 3.3 节） |
| Lua 计数 + 上限 | INCR + EXPIRE | 短时间限次（验证码 5 次） |
| 8.4 条件 SET | `setIfEquals`（CAS） | 状态机式防重（值变化判断） |

> ⚠️ **Redis 防重的边界**：Redis 挂了防重失效 → **资金类防重必须数据库唯一约束兜底**（[JPA 幂等](../../../../09-Web开发全流程/后端 分布式常用名词通俗解释/04-重复请求治理：幂等与防抖.md) 3.5 节三层组合）——Redis 防重是"第一道快闸"，唯一约束是"最后防线"。

## 5. 8.4 条件命令对锁的影响

> **Redis 8.4 的条件 SET/DEL 让"释放锁"这类 CAS 操作不再需要 Lua**——一条命令完成（[04 篇](04-操作API与Redis-8-4新命令速查.md) 5.1 节）。

| 操作 | 旧做法 | 8.4 新做法 |
|------|--------|-----------|
| 释放锁（验证 token 再删） | Lua 脚本（3.1 节） | **条件 DEL**（`DEL key NX` 带值匹配？——8.4 条件 DEL 按值比较） |
| 防重标记 | SETNX + 手动 | 条件 SET（CAS） |
| 状态机更新 | GET 对比 + SET（两步） | 条件 SET（一条原子） |

```java
// 8.4 条件 DEL 释放锁（若支持值比较：锁值匹配才删）
// 语义等价 3.1 的 Lua：验证 token → DEL，但一条命令
Boolean unlocked = redisTemplate.execute(conn -> conn.stringCommands()
        .set(...));   // 具体 API 随 4.1 函数式命令层支持情况
```

> 💡 8.4 条件命令的意义：**Lua 的使用场景被部分替代**（释放锁/防重/状态机）——"先 GET 再写"的两步竞态被原子命令消除；但**复杂组合**（多 key 事务性操作）仍要 Lua（条件命令只覆盖单 key 的 CAS 语义）。

## 6. 锁的边界与红线

| # | 红线 | 说明 |
|---|------|------|
| 1 | 锁的粒度 | 锁 key 要细（`lock:order:1001` 而非 `lock:order`——全局锁 = 串行化） |
| 2 | 锁内别做远程调用 | 持锁等网络 = 锁风暴（同 [JPA 事务红线](../../Spring%20Data%20系列【数据访问层】/Spring%20Data%20JPA/06-事务与并发控制速查.md)） |
| 3 | 锁 ≠ 幂等 | 锁防并发互斥，幂等防重复提交——两回事（[名词系列 04 篇](../../../../09-Web开发全流程/后端 分布式常用名词通俗解释/04-重复请求治理：幂等与防抖.md) 有对比） |
| 4 | 主从切换丢锁 | 极端场景（主节点宕机未同步锁）→ Redlock 或 etcd/ZK |
| 5 | 锁 TTL 与业务时长 | 自研锁 TTL 必须 > 业务最大耗时（或看门狗续期） |
| 6 | 时钟/超时 | 业务超时异常必须 finally 释放，否则死锁到 TTL |
| 7 | 分布式锁的替代 | 能不用锁就不用：**原子命令（INCR/DECR/条件 SET）优先、乐观锁（@Version）次之、分布式锁最后**——锁是最后手段 |

> 🎯 锁选型口诀：**"能原子命令解决的不加锁（$inc/条件 SET），能乐观锁解决的不加分布式锁（冲突少），必须互斥才上锁（Redisson 生产）"**——与 [JPA 并发控制](../../Spring%20Data%20系列【数据访问层】/Spring%20Data%20JPA/06-事务与并发控制速查.md) 的选型心法完全一致，面试先答这条"分层避免锁"再答锁的实现，是加分结构。

---

**下一模块**：[07-管道事务与批量性能速查](07-管道事务与批量性能速查.md)　**返回总览**：[00-组件总览](00-Spring Data Redis组件总览.md)

**【参考来源】**：[Spring Data Redis 官方参考文档（Lua 脚本）](https://docs.spring.io/spring-data/redis/reference/redis/scripting.html)、[Redisson 官方文档](https://redisson.org/)、[Redis 官方 Lua 脚本文档](https://redis.io/docs/latest/develop/programmability/eval-intro/)
