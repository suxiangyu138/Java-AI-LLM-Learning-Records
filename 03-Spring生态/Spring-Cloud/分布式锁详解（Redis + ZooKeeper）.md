# 分布式锁详解

## 为什么需要分布式锁

单体应用中用 `synchronized` 或 `ReentrantLock` 做互斥，但在分布式多实例部署时，JVM 级锁无法跨进程生效。分布式锁解决的是**跨 JVM 的互斥访问**问题。

典型场景：防止重复下单、定时任务单实例执行、库存扣减。

## 基于 Redis 的分布式锁

### 基础实现（SETNX）

```java
// 加锁
String lockKey = "order:lock:" + orderId;
String lockValue = UUID.randomUUID().toString();  // 唯一标识，用于区分持有者
Boolean locked = redisTemplate.opsForValue()
        .setIfAbsent(lockKey, lockValue, 30, TimeUnit.SECONDS);
if (!Boolean.TRUE.equals(locked)) {
    throw new BusinessException("操作过于频繁，请稍后重试");
}

try {
    // 执行业务
} finally {
    // 解锁：Lua 脚本保证原子性（比较 + 删除）
    String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) else return 0 end";
    redisTemplate.execute(
            new DefaultRedisScript<>(script, Long.class),
            Collections.singletonList(lockKey),
            lockValue);
}
```

**要点：**
- 加锁必须设过期时间，防止死锁
- value 用唯一标识，解锁时先比较后删除（Lua 保证原子性）
- 不要直接 del key（会释放别人的锁）

### Redisson（推荐）

Redisson 封装了完整的分布式锁实现，支持自动续期：

```java
RLock lock = redisson.getLock("order:lock:" + orderId);
try {
    if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
        // 10秒等待获取锁，30秒自动释放
        // 执行业务
    }
} finally {
    if (lock.isHeldByCurrentThread()) {
        lock.unlock();
    }
}
```

**Redisson 优势：**
- 看门狗机制：锁默认 30 秒，每 10 秒自动续期，业务完成前不会过期
- 可重入：基于 Redis Hash 实现，同一线程可重复加锁
- 自动释放：锁持有者失效时，其他线程可竞争

### 注解方式（最简洁）

```java
@RedisLock(key = "order:#{#orderId}", waitTime = 10, leaseTime = 30)
public void createOrder(Long orderId) {
    // 业务逻辑
}
```

## 基于 ZooKeeper 的分布式锁

利用 ZK 的临时顺序节点 + Watch 机制：

- 客户端在 `/lock` 下创建临时顺序节点
- 判断自己是否是最小序号，是则获取锁
- 不是则 watch 前一个节点的删除事件
- 客户端断开连接时临时节点自动删除

**优势**：强一致性、自动释放（连接断开则临时节点删除）
**劣势**：性能不如 Redis，维护成本高

## 基于数据库的分布式锁

利用唯一索引 + `INSERT` / `DELETE`：

```sql
INSERT INTO distributed_lock (lock_key, owner, expire_at) VALUES ('order:123', 'server1', NOW() + INTERVAL 30 SECOND);
```

**劣势**：性能差、没有自动续期、需要手动清理过期锁。生产环境不推荐。

## 方案对比

| 特性 | Redis | ZooKeeper | 数据库 |
|------|-------|-----------|--------|
| 性能 | 高 | 中 | 低 |
| 一致性 | 最终一致（AP） | 强一致（CP） | 强一致 |
| 自动续期 | Redisson 支持 | 天然支持（临时节点） | 需自己实现 |
| 复杂度 | 低（Redisson） | 中 | 低 |
| 推荐场景 | 绝大多数场景 | 强一致性要求 | 不推荐 |

## 常见问题

**1. 锁过期了业务还没执行完？**
- Redisson 看门狗自动续期
- 手动实现时需要预估锁时间，或后台起线程续期

**2. Redlock 算法？**
Redis 作者提出的多节点冗余方案：向多个独立 Redis 实例申请锁，超过半数成功才算获取成功。实际项目中 Redisson 单节点已足够，Redlock 只在极高一致性场景使用。

**3. 可重入性**
同一个线程/进程可以对已持有的锁再次加锁。`synchronized` 天然可重入，Redisson 用 Hash 结构实现了可重入。
