# 快速学会：用 Redis 构建 Java 后端通用应用组件
基于**SpringBoot + Redis**，直接落地开发中**可复用组件**，全部是企业项目标准组件，拿来就能集成使用：
缓存组件、分布式限流、分布式锁、短队列、黑名单、排行榜、全局计数器、验证码组件。

---

## 一、前置基础（已配置直接跳过）

### 1. Maven 依赖
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### 2. YAML 配置
```yaml
spring:
  redis:
    host: localhost
    port: 6379
    database: 0
    lettuce:
      pool:
        max-active: 10
        max-idle: 5
```

### 3. 序列化配置类（必须）
```java
@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
```

统一注入：
```java
@Autowired
private RedisTemplate<String, Object> redisTemplate;
```

---

## 二、组件1：通用缓存组件（解决数据库缓存）

### 核心能力
缓存业务实体、自动过期、缓存击穿基础防护。
```java
@Component
public class CacheComponent {

    // 存入缓存，指定秒级过期
    public void setCache(String key, Object data, long expireSecond) {
        redisTemplate.opsForValue().set(key, data, expireSecond, TimeUnit.SECONDS);
    }

    // 获取缓存
    public Object getCache(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    // 删除缓存（更新/删除业务时主动清缓存）
    public void removeCache(String key) {
        redisTemplate.delete(key);
    }
}
```

### 业务使用模板
```java
public User getUser(Long id) {
    String key = "user::" + id;
    Object cache = cacheComponent.getCache(key);
    if (cache != null) {
        return (User) cache;
    }
    // 查库
    User user = userMapper.selectById(id);
    cacheComponent.setCache(key, user, 3600);
    return user;
}
```

---

## 三、组件2：Redis 分布式锁组件（防重复提交/并发）

### 核心能力
原子加锁、自动过期、手动释锁，杜绝死锁。
```java
@Component
public class RedisLockComponent {

    // 加锁：key=锁标识，expire=锁超时秒数
    public boolean tryLock(String lockKey, long expire) {
        // setIfAbsent 等价 NX 原子操作
        return redisTemplate.opsForValue()
                .setIfAbsent(lockKey, System.currentTimeMillis(), expire, TimeUnit.SECONDS);
    }

    // 释放锁
    public void unLock(String lockKey) {
        redisTemplate.delete(lockKey);
    }
}
```

### 业务使用
```java
// 防止订单重复提交
String lockKey = "lock::order::" + orderNo;
if (redisLockComponent.tryLock(lockKey, 15)) {
    try {
        // 执行下单、扣库存核心业务
    } finally {
        redisLockComponent.unLock(lockKey);
    }
} else {
    throw new RuntimeException("操作频繁，请稍后重试");
}
```

---

## 四、组件3：全局计数器组件（访问量、点赞、收藏）
```java
@Component
public class CountComponent {
    // 自增
    public Long incr(String key) {
        return redisTemplate.opsForValue().increment(key);
    }
    // 自减
    public Long decr(String key) {
        return redisTemplate.opsForValue().increment(key, -1);
    }
    // 设置过期
    public void setExpire(String key, long second) {
        redisTemplate.expire(key, second, TimeUnit.SECONDS);
    }
}
```
场景：文章阅读量、接口总请求数、商品点赞数。

---

## 五、组件4：接口限流组件（IP 限流、防刷）
基于**时间窗口 + 计数器**实现单机/集群限流。
```java
@Component
public class RateLimitComponent {

    /**
     * 限流校验
     * @param limitKey 唯一标识（IP/用户ID）
     * @param maxCount 周期内最大请求次数
     * @param second 时间周期
     */
    public boolean isLimit(String limitKey, int maxCount, long second) {
        String key = "limit::" + limitKey;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            redisTemplate.expire(key, second, TimeUnit.SECONDS);
        }
        return count > maxCount;
    }
}
```
使用：IP 1分钟最多访问60次
```java
boolean limit = rateLimitComponent.isLimit(ip, 60, 60);
if(limit){
    throw new RuntimeException("访问过于频繁");
}
```

---

## 六、组件5：验证码短期存储组件
```java
@Component
public class CodeComponent {
    // 存入验证码，5分钟过期
    public void saveCode(String phone, String code) {
        String key = "code::sms::" + phone;
        redisTemplate.opsForValue().set(key, code, 300, TimeUnit.SECONDS);
    }
    // 校验验证码
    public boolean checkCode(String phone, String code) {
        String key = "code::sms::" + phone;
        String cacheCode = (String) redisTemplate.opsForValue().get(key);
        return Objects.equals(cacheCode, code);
    }
}
```

---

## 七、组件6：简易消息队列组件（异步任务）
基于 List 实现轻量队列，无需 MQ。
```java
@Component
public class SimpleQueueComponent {
    // 入队
    public void pushTask(String queueKey, Object task) {
        redisTemplate.opsForList().rightPush(queueKey, task);
    }
    // 出队（阻塞简易版）
    public Object popTask(String queueKey) {
        return redisTemplate.opsForList().leftPop(queueKey);
    }
}
```
适用：日志记录、消息推送、轻量异步任务。

---

## 八、组件7：黑名单组件
```java
@Component
public class BlackListComponent {
    public void addBlack(String blackKey, String value) {
        redisTemplate.opsForSet().add(blackKey, value);
    }
    public boolean isBlack(String blackKey, String value) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(blackKey, value));
    }
}
```

---

## 九、组件8：排行榜组件（ZSet）
```java
@Component
public class RankComponent {
    // 添加分数
    public void addRank(String rankKey, String name, double score) {
        redisTemplate.opsForZSet().add(rankKey, name, score);
    }
    // 获取TopN 降序榜单
    public Set<ZSetOperations.TypedTuple<Object>> getTopRank(String rankKey, int topN) {
        return redisTemplate.opsForZSet().reverseRangeWithScores(rankKey, 0, topN - 1);
    }
}
```

---

## 十、整体架构：Redis 组件化设计思想
1. **分层隔离**
   RedisTemplate 为底层基础层，上层拆分：缓存、锁、限流、队列、计数器等独立组件。
2. **Key 统一规范**
   统一前缀：`业务::类型::唯一值`，方便管理、批量删除。
3. **全部带过期**
   业务缓存、限流、验证码全部设置过期，避免 Redis 内存溢出。
4. **原子操作优先**
   分布式锁、限流全部使用 Redis 原子命令，保证集群环境安全。

---

## 十一、快速落地总结
你开发 Java 项目，只需要固定引入 8 大组件：
1. 缓存组件 → 加速数据库查询
2. 分布式锁 → 并发安全
3. 计数器 → 点赞/访问统计
4. 限流组件 → 防刷防护
5. 验证码组件 → 登录/校验
6. 简易队列 → 轻量异步
7. 黑名单 → 风控拦截
8. 排行榜 → 积分/热度榜单
