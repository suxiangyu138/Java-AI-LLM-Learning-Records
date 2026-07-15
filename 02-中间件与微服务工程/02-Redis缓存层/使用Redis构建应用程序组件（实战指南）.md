# 使用 Redis 构建应用程序组件（实战指南）

> **定位**：Redis 应用程序组件是可集成到主应用的功能模块，依托 Redis 实现会话管理、限流、排行榜、消息队列、分布式锁等高频场景。

---

## 目录

1. [会话管理组件](#1-会话管理组件)
2. [接口限流组件](#2-接口限流组件)
3. [排行榜组件](#3-排行榜组件)
4. [消息队列组件](#4-消息队列组件)
5. [分布式锁组件](#5-分布式锁组件)

---

## 1. 会话管理组件

| 要素 | 方案 |
|------|------|
| 存储 | `session:uuid` → Hash（userId/username/permissions） |
| 过期 | 2 小时 + 操作续期 |
| 登出 | `DELETE` 对应 Key |

```java
// 创建会话
String sessionId = UUID.randomUUID().toString();
hashOps.putAll("session:" + sessionId, sessionInfo);
stringRedisTemplate.expire(sessionKey, 7200, TimeUnit.SECONDS);

// 校验 + 续期
stringRedisTemplate.expire(sessionKey, 7200, TimeUnit.SECONDS);
```

---

## 2. 接口限流组件

> 滑动窗口策略：Set 存储时间戳 + 删除窗口外 + 统计计数。

```java
// 写入当前时间戳 → 删除窗口外 → 统计
stringRedisTemplate.opsForSet().add(limitKey, String.valueOf(currentTime));
stringRedisTemplate.opsForSet().removeRangeByScore(limitKey, 0, windowStartTime);
Long count = stringRedisTemplate.opsForSet().size(limitKey);
return count != null && count <= maxCount;
```

---

## 3. 排行榜组件

| 操作 | 命令 |
|------|------|
| 更新分数 | `ZINCRBY rank:key targetId score` |
| TopN 查询 | `ZREVRANGE rank:key 0 N-1 WITHSCORES` |
| 个人排名 | `ZREVRANK rank:key targetId` |

---

## 4. 消息队列组件

| 操作 | 命令 |
|------|------|
| 发送（生产者） | `LPUSH mq:queueName message` |
| 消费（消费者） | `BRPOP mq:queueName timeout`（阻塞式） |
| 消费失败 | 重新 `LPUSH` 回队列 |

> ⚠️ 适合中小规模非核心业务（通知/日志），核心业务用 RabbitMQ/Kafka。

---

## 5. 分布式锁组件

```java
// 获取锁：SET NX EX
String lockValue = UUID.randomUUID().toString();
Boolean success = redisTemplate.opsForValue()
    .setIfAbsent("lock:" + lockKey, lockValue, 30, TimeUnit.SECONDS);

// 释放锁：Lua 原子校验 + 删除
if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('del', KEYS[1])
end
```

### 通用注意事项

| 原则 | 说明 |
|------|------|
| Key 规范 | `组件类型:业务标识:唯一ID` |
| 序列化 | String/JSON 序列化，禁用 JDK 序列化 |
| 异常降级 | Redis 不可用时 fallback |
| 监控 | 定期检查 Redis 连接数/内存/慢命令 |
