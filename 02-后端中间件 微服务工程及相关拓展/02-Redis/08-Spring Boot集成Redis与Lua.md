# Spring Boot 集成 Redis 与 Lua 脚本
> 从依赖配置到组件封装，从 Lua 脚本到注解缓存——Spring Boot 项目集成 Redis 的完整指南。

## 目录
1. [Spring Data Redis 配置](#1-spring-data-redis-配置)
2. [RedisTemplate 使用详解](#2-redistemplate-使用详解)
3. [注解式缓存](#3-注解式缓存)
4. [Redisson 分布式锁](#4-redisson-分布式锁)
5. [业务组件封装](#5-业务组件封装)
6. [Lua 脚本编程](#6-lua-脚本编程)
7. [Pipeline 与事务](#7-pipeline-与事务)

---

## 1. Spring Data Redis 配置

### 1.1 Maven 依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<!-- 连接池（推荐） -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
```

### 1.2 application.yml

```yaml
spring:
  redis:
    host: 127.0.0.1
    port: 6379
    password: 
    database: 0
    timeout: 10000ms
    lettuce:
      pool:
        max-active: 16        # 最大连接数
        max-idle: 8           # 最大空闲
        min-idle: 4           # 最小空闲
        max-wait: -1ms        # 获取连接最大等待（-1=无限制）
```

| 连接池参数 | 说明 | 建议值 |
|-----------|------|:------:|
| max-active | 最大连接数 | 16-32 |
| max-idle | 最大空闲连接 | 8-16 |
| min-idle | 最小空闲连接 | 4-8 |
| max-wait | 获取连接超时 | 100-200ms |

### 1.3 RedisTemplate 配置（核心）

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // Key 序列化：String（可读性好）
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Value 序列化：JSON（跨语言兼容）
        GenericJackson2JsonRedisSerializer jsonSerializer = 
            new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
}
```

> ⚠️ 为什么不用 JdkSerializationRedisSerializer？JDK 序列化体积大、可读性差、跨语言不兼容。

### 1.4 RedisTemplate vs StringRedisTemplate

| 对比维度 | RedisTemplate | StringRedisTemplate |
|----------|--------------|-------------------|
| 序列化器 | JDK/JSON（可配置） | StringRedisSerializer |
| Value 类型 | Object | String |
| 适用场景 | 缓存对象/Hash | 缓存纯字符串/数字 |
| Key 序列化 | 可配置 | String |
| Value 可读性 | 二进制/JSON（JSON 可读） | 明文可读 |

### 1.5 连接工厂配置

```java
@Configuration
public class LettuceConfig {

    @Bean
    public RedisConnectionFactory lettuceConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName("127.0.0.1");
        config.setPort(6379);
        config.setPassword(RedisPassword.of("password"));
        config.setDatabase(0);

        GenericObjectPoolConfig<StatefulRedisConnection> poolConfig = 
            new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(16);
        poolConfig.setMaxIdle(8);
        poolConfig.setMinIdle(4);

        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
            .poolConfig(poolConfig)
            .commandTimeout(Duration.ofMillis(10000))
            .build();

        return new LettuceConnectionFactory(config, clientConfig);
    }
}
```

> 💡 Lettuce 是 Spring Boot 2.0+ 默认的 Redis 客户端，基于 Netty 异步非阻塞。相比 Jedis（BIO），Lettuce 线程安全、支持异步/响应式、连接数更少。

---

## 2. RedisTemplate 使用详解

### 2.1 5 种数据结构操作

**String（ValueOperations）**

```java
redisTemplate.opsForValue().set("key", value);
redisTemplate.opsForValue().set("key", value, 3600, TimeUnit.SECONDS);
redisTemplate.opsForValue().setIfAbsent("lock", "uuid", 30, TimeUnit.SECONDS);
Object val = redisTemplate.opsForValue().get("key");
Long count = redisTemplate.opsForValue().increment("counter");
redisTemplate.opsForValue().increment("counter", 10);
```

**Hash（HashOperations）**

```java
redisTemplate.opsForHash().put("user:1001", "name", "张三");
redisTemplate.opsForHash().putAll("user:1001", map);
Object name = redisTemplate.opsForHash().get("user:1001", "name");
Map<Object, Object> all = redisTemplate.opsForHash().entries("user:1001");
Long size = redisTemplate.opsForHash().size("user:1001");
redisTemplate.opsForHash().increment("user:1001", "score", 10);
```

**List（ListOperations）**

```java
redisTemplate.opsForList().rightPush("queue", task);
redisTemplate.opsForList().rightPushAll("list", "a", "b", "c");
Object task = redisTemplate.opsForList().leftPop("queue");
Object taskBlock = redisTemplate.opsForList()
    .leftPop("queue", 1, TimeUnit.SECONDS);
List<Object> range = redisTemplate.opsForList().range("list", 0, -1);
Long size = redisTemplate.opsForList().size("list");
redisTemplate.opsForList().trim("list", 0, 99);  // 保留前 100
```

**Set（SetOperations）**

```java
redisTemplate.opsForSet().add("set", "a", "b", "c");
Set<Object> members = redisTemplate.opsForSet().members("set");
Boolean isMember = redisTemplate.opsForSet().isMember("set", "a");
Set<Object> inter = redisTemplate.opsForSet()
    .intersect("set1", "set2");  // 交集
Long size = redisTemplate.opsForSet().size("set");
```

**ZSet（ZSetOperations）**

```java
redisTemplate.opsForZSet().add("rank", "user1", 95.0);
redisTemplate.opsForZSet().incrementScore("rank", "user1", 5.0);
Set<String> topN = redisTemplate.opsForZSet()
    .reverseRange("rank", 0, 9);
Set<ZSetOperations.TypedTuple<Object>> topWithScores = 
    redisTemplate.opsForZSet()
        .reverseRangeWithScores("rank", 0, 9);
Long rank = redisTemplate.opsForZSet()
    .reverseRank("rank", "user1");
Double score = redisTemplate.opsForZSet()
    .score("rank", "user1");
```

### 2.2 通用操作

```java
// 过期操作
redisTemplate.expire("key", 3600, TimeUnit.SECONDS);
Boolean hasKey = redisTemplate.hasKey("key");
redisTemplate.delete("key");
Long deleted = redisTemplate.delete(List.of("key1", "key2"));

// 原子操作
Boolean setIfAbsent = redisTemplate.opsForValue()
    .setIfAbsent("lock", "uuid", 30, TimeUnit.SECONDS);
```

### 2.3 缓存业务标准模板

```java
@Service
public class UserService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private UserMapper userMapper;

    public User getUser(Long userId) {
        String key = "user:" + userId;
        // 1. 从缓存获取
        User user = (User) redisTemplate.opsForValue().get(key);
        if (user != null) return user;

        // 2. 从 DB 获取
        user = userMapper.selectById(userId);
        if (user == null) {
            // 3. 缓存空值防穿透（短 TTL）
            redisTemplate.opsForValue()
                .set(key, new NullValue(), 60, TimeUnit.SECONDS);
            return null;
        }

        // 4. 写入缓存
        redisTemplate.opsForValue()
            .set(key, user, 30, TimeUnit.MINUTES);
        return user;
    }

    @Transactional
    public void updateUser(User user) {
        userMapper.updateById(user);
        redisTemplate.delete("user:" + user.getId()); // 删缓存
    }
}
```

### 2.4 配置检查清单

| 检查项 | 正确做法 | 错误做法 |
|--------|----------|----------|
| Key 序列化 | StringRedisSerializer | JdkSerializationRedisSerializer |
| Value 序列化 | JSON 序列化器 | 默认 JDK 序列化 |
| 连接池 | 必须配置 | 不配（无连接池） |
| Pipeline | 批量操作使用 | 循环逐条操作 |

---

## 3. 注解式缓存

### 3.1 启用缓存

```java
@EnableCaching  // 在配置类上添加
@SpringBootApplication
public class Application {}
```

### 3.2 注解详解

```java
@Service
public class ProductService {

    /**
     * @Cacheable：先查缓存，缓存存在直接返回，不存在执行方法并缓存结果
     * key：SpEL 表达式，#id 引用参数
     * unless：条件为 true 时不缓存
     */
    @Cacheable(value = "product", key = "#id", unless = "#result == null")
    public Product getProduct(Long id) {
        return productMapper.selectById(id);
    }

    /**
     * @CachePut：总是执行方法，并将结果更新缓存
     */
    @CachePut(value = "product", key = "#product.id")
    public Product updateProduct(Product product) {
        productMapper.updateById(product);
        return product;
    }

    /**
     * @CacheEvict：删除缓存
     * beforeInvocation=true：方法执行前删除（防异常导致删除失败）
     * allEntries=true：清空整个分区
     */
    @CacheEvict(value = "product", key = "#id")
    public void deleteProduct(Long id) {
        productMapper.deleteById(id);
    }

    /**
     * @Caching：组合多个缓存操作
     */
    @Caching(
        cacheable = @Cacheable("product"),
        evict = @CacheEvict("product_list")
    )
    public Product getProductWithEvict(Long id) {
        return productMapper.selectById(id);
    }
}
```

### 3.3 注解对比

| 注解 | 作用 | 执行方法 | 填充缓存 |
|------|------|:--------:|:--------:|
| @Cacheable | 缓存读取 | 缓存未命中时执行 | 自动 |
| @CachePut | 缓存更新 | 总是执行 | 自动 |
| @CacheEvict | 缓存删除 | 总是执行 | 不填充 |
| @Caching | 组合操作 | 视内部注解而定 | 视内部注解而定 |

### 3.4 SpEL 常用表达式

| 表达式 | 说明 |
|--------|------|
| `#id` | 方法参数 id |
| `#result` | 方法返回值 |
| `#root.args` | 所有参数数组 |
| `#root.methodName` | 方法名 |
| `#root.targetClass` | 目标类 |
| `result == null` | 返回值为 null 时不缓存 |

### 3.5 自定义 KeyGenerator

```java
@Configuration
public class CacheConfig extends CachingConfigurerSupport {

    @Bean
    public KeyGenerator keyGenerator() {
        return (target, method, params) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(target.getClass().getSimpleName()).append(":");
            sb.append(method.getName());
            for (Object param : params) {
                sb.append(":").append(param);
            }
            return sb.toString();
        };
    }
}
```

### 3.6 缓存失效场景

| 场景 | 问题 | 解决 |
|------|------|------|
| 同一类内方法调用 | AOP 代理不生效 | 注入自身 Bean 调用 |
| 方法内部调用 | 同上 | 同上 |
| 非 public 方法 | 注解不拦截 | 改为 public |
| 序列化问题 | 缓存对象未实现 Serializable | 实现序列化接口 |

> ⚠️ 注解缓存基于 AOP 代理，类内部方法调用不走代理，注解不会生效。需要用 `@Autowired` 注入自身 Bean 再调用。

---

## 4. Redisson 分布式锁

### 4.1 Maven 依赖

```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.23.5</version>
</dependency>
```

### 4.2 配置

```yaml
# application.yml
spring:
  redis:
    redisson:
      config: |
        singleServerConfig:
          address: "redis://127.0.0.1:6379"
          password: null
          connectionPoolSize: 10
          connectionMinimumIdleSize: 5
```

### 4.3 分布式锁 + 看门狗

```java
@Service
public class OrderService {

    @Autowired
    private RedissonClient redissonClient;

    public void createOrder(String orderId) {
        RLock lock = redissonClient.getLock("lock:order:" + orderId);

        // 尝试加锁，最多等待 5 秒，锁有效 10 秒
        boolean locked = lock.tryLock(5, 10, TimeUnit.SECONDS);
        if (!locked) {
            throw new RuntimeException("系统繁忙，请稍后重试");
        }

        try {
            // 业务逻辑...
            processOrder(orderId);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();  // 必须释放
            }
        }
    }
}
```

### 4.4 看门狗（Watchdog）原理

```text
lock() 或 tryLock() 不传过期时间时：

1. 默认锁过期时间 = 30 秒
2. 加锁成功后，启动一个定时任务
3. 每 10 秒检查一次锁是否还存在
4. 若锁还在，将过期时间重置为 30 秒
5. 业务完成后 unlock() 取消看门狗
```

> 💡 看门狗确保业务未完成时锁不会自动释放，防止死锁的同时提高可靠性。如果客户端崩溃，锁最长存活 30 秒后自动释放。

### 4.5 可重入锁测试

```java
@Autowired
private RedissonClient redissonClient;

public void testReentrant() {
    RLock lock = redissonClient.getLock("lock:reentrant");
    
    lock.lock();  // 第一次加锁
    try {
        // 业务逻辑中再次加锁（同一线程）
        lock.lock();  // 第二次加锁（可重入）
        try {
            // 业务逻辑
        } finally {
            lock.unlock();  // 第二次解锁
        }
    } finally {
        lock.unlock();  // 第一次解锁
    }
}
```

### 4.6 Redisson 分布式集合

```java
// 分布式 Map
RMap<String, Object> map = redissonClient.getMap("user:1001");
map.put("name", "张三");

// 分布式 Set
RSet<String> set = redissonClient.getSet("online:users");
set.add("user1");

// 分布式 AtomicLong
RAtomicLong counter = redissonClient.getAtomicLong("global:counter");
counter.incrementAndGet();

// 分布式 BlockingQueue
RBlockingQueue<String> queue = redissonClient.getBlockingQueue("task:queue");
queue.put("task1");

// 延迟队列
RDelayedQueue<String> delayedQueue = 
    redissonClient.getDelayedQueue(queue);
delayedQueue.offer("task", 10, TimeUnit.SECONDS);

// 布隆过滤器
RBloomFilter<String> bloom = redissonClient.getBloomFilter("bloom:user");
bloom.tryInit(1000000L, 0.01);
bloom.add("user:1001");
boolean exists = bloom.contains("user:1001");
```

---

## 5. 业务组件封装

### 5.1 基础工具类

```java
@Component
public class RedisBaseUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, Object value, long expireSec) {
        redisTemplate.opsForValue().set(key, value, expireSec, TimeUnit.SECONDS);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    public Boolean expire(String key, long expireSec) {
        return redisTemplate.expire(key, expireSec, TimeUnit.SECONDS);
    }

    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    public Long incr(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    public Boolean setNx(String key, Object value, long expireSec) {
        return redisTemplate.opsForValue()
            .setIfAbsent(key, value, expireSec, TimeUnit.SECONDS);
    }
}
```

### 5.2 8 大业务组件速查

| 组件 | 核心方法 | API |
|------|----------|-----|
| CacheComponent | put / get / remove | opsForValue |
| RedisLockComponent | tryLock / releaseLock | setIfAbsent + Lua |
| RateLimitComponent | isLimited | incr + expire |
| CounterComponent | add / sub | incr |
| SmsCodeComponent | saveCode / verifyCode | opsForValue |
| SimpleQueueComponent | push / poll | opsForList |
| BlackListComponent | addBlack / inBlackList | opsForSet |
| RankComponent | putScore / getTop | opsForZSet |

### 5.3 组件封装原则

> 💡 封装组件时注意：抽取常量 Key 前缀、统一 TTL 配置、异常不要吞掉、Lua 脚本优先于多次网络调用。

---

## 6. Lua 脚本编程

### 6.1 核心价值

| 价值 | 说明 |
|------|------|
| 原子执行 | 脚本中所有命令整体执行，不会被其他命令打断 |
| 减少网络开销 | 一次请求执行多个命令 |
| 逻辑封装 | 服务端实现复杂业务逻辑 |

### 6.2 基础语法

```lua
-- redis.call()：出错抛异常，脚本中断
-- redis.pcall()：出错返回错误，脚本继续
redis.call('SET', KEYS[1], ARGV[1])
local val = redis.call('GET', KEYS[1])
return val
```

**数据类型对应：**

| Lua 类型 | Redis 类型 | 说明 |
|----------|-----------|------|
| nil | nil | 空值 |
| number | integer | 整数 |
| string | bulk string | 字符串 |
| table | array/map | 表（数组或字典）|
| boolean | 无对应 | 需转 0/1 |

### 6.3 常用 Lua 命令

| 命令 | 说明 |
|------|------|
| `EVAL script numkeys key... arg...` | 直接执行脚本 |
| `SCRIPT LOAD script` | 加载脚本，返回 SHA1 |
| `EVALSHA sha1 numkeys key... arg...` | 通过 SHA1 执行 |
| `SCRIPT EXISTS sha1...` | 检查脚本是否缓存 |
| `SCRIPT FLUSH` | 清空脚本缓存 |
| `SCRIPT KILL` | 终止运行中的脚本 |

### 6.4 企业级案例

**案例 1：安全释放分布式锁**

```lua
-- KEYS[1]: 锁的 key
-- ARGV[1]: 锁的唯一标识（UUID）
if redis.call('GET', KEYS[1]) == ARGV[1] then
    return redis.call('DEL', KEYS[1])
else
    return 0
end
```

```java
// Java 调用
String script = "if redis.call('GET', KEYS[1]) == ARGV[1] " +
                "then return redis.call('DEL', KEYS[1]) " +
                "else return 0 end";

DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
Long result = redisTemplate.execute(
    redisScript, Collections.singletonList(lockKey), requestId);
return Long.valueOf(1).equals(result);
```

**案例 2：原子限流**

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

**案例 3：原子库存扣减**

```lua
-- KEYS[1]: 库存 key
-- ARGV[1]: 扣减数量
local stock = tonumber(redis.call('GET', KEYS[1]))
local num = tonumber(ARGV[1])
if not stock then return -1 end     -- 键不存在
if stock < num then return 0 end    -- 库存不足
redis.call('DECRBY', KEYS[1], num)
return tonumber(redis.call('GET', KEYS[1]))
```

**案例 4：Set 原子添加并过期**

```lua
-- KEYS[1]: Set key
-- ARGV[1]: 元素值
-- ARGV[2]: 过期时间（秒）
redis.call('SADD', KEYS[1], ARGV[1])
redis.call('EXPIRE', KEYS[1], ARGV[2])
return redis.call('SCARD', KEYS[1])
```

### 6.5 Lua vs 事务

| 特性 | Lua 脚本 | 事务 (MULTI/EXEC) |
|------|----------|-------------------|
| 原子性 | 完全原子 | 整体原子 |
| 错误处理 | pcall 可捕获 | 失败命令后继续执行 |
| 逻辑复杂度 | 支持条件/循环 | 仅批量执行 |
| 性能 | 单次网络往返 | 多次网络往返 |
| 适用 | 复杂原子操作 | 简单批量执行 |

### 6.6 注意事项

| 注意点 | 说明 |
|--------|------|
| 脚本不可过长 | 单线程执行，超 100ms 阻塞 Redis |
| key 必须通过 KEYS 传递 | Cluster 模式路由到正确节点 |
| 禁止随机函数 | `RANDOM`/`TIME` 导致主从不一致 |
| 加 local | 变量加 local 避免全局污染 |
| 脚本复用 | SCRIPT LOAD + EVALSHA 减少网络开销 |

---

## 7. Pipeline 与事务

### 7.1 Pipeline（管道）

批量发送命令，减少网络 RTT（往返时间），非原子执行。

```java
// Pipeline 批量写入
List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
    for (int i = 0; i < 1000; i++) {
        connection.stringCommands()
            .set(("key:" + i).getBytes(), ("value:" + i).getBytes());
    }
    return null;
});
```

**Pipeline 性能对比：**

```text
1000 次 SET 命令：
无 Pipeline：1000 × RTT（如 1ms）= 1000ms
带 Pipeline：1 × RTT + 传输时间 ≈ 1-10ms

节省约 99% 的网络耗时
```

### 7.2 事务（MULTI/EXEC）

```java
// SessionCallback 实现事务
List<Object> results = redisTemplate.execute(new SessionCallback<List<Object>>() {
    @Override
    public List<Object> execute(RedisOperations operations) {
        operations.multi();  // 开启事务
        operations.opsForValue().set("key1", "value1");
        operations.opsForValue().set("key2", "value2");
        operations.opsForValue().increment("counter");
        return operations.exec();  // 提交事务
    }
});
```

### 7.3 Pipeline vs 事务 vs Lua

| 特性 | Pipeline | 事务 (MULTI/EXEC) | Lua 脚本 |
|------|----------|-------------------|----------|
| 原子性 | 否 | 是 | 是 |
| 减少 RTT | 是 | 否 | 是 |
| 支持逻辑 | 否 | 否 | 条件/循环 |
| 性能 | 高 | 中 | 高 |
| 适用 | 批量写入 | 简单原子操作 | 复杂原子操作 |

> 🎯 生产推荐：简单批量用 Pipeline，复杂原子操作用 Lua 脚本，Redis 事务使用较少。

---

## 常见问题

**Q: RedisTemplate 序列化乱码？**
配置 StringRedisSerializer for key，GenericJackson2JsonRedisSerializer for value。

**Q: 注解缓存 key 冲突？**
使用 `cacheName + : + key` 命名空间隔离，或用自定义 KeyGenerator。

**Q: Lua 脚本如何调试？**
redis-cli 本地测试 → SCRIPT LOAD → EVALSHA。注意生产环境脚本不可出错。

**Q: 看门狗会无限续期吗？**
不会，业务完成 unlock() 时看门狗线程会被取消。如果客户端崩溃，锁最长存活 30 秒后自动释放。

**Q: Pipeline 和 Lua 脚本能一起用吗？**
不能。Pipeline 是批量发送命令，Lua 是一次发送脚本。可以先在 Lua 中批量操作，会比 Pipeline+多个命令更高效。

**Q: Redisson vs Jedis vs Lettuce？**
Redisson 功能最全（分布式锁、集合、队列），Lettuce 是 Spring Boot 默认（异步、线程安全），Jedis（同步、简单）。推荐 Lettuce + Redisson 组合。
