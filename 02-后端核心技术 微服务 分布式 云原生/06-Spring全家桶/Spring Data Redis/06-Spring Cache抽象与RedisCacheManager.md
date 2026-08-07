# 06 Spring Cache 抽象与 RedisCacheManager

> @Cacheable 注解一行开启缓存，RedisCacheManager 让 Redis 成为缓存后端——但注解默认行为的每个细节（key 生成、null 处理、TTL、并发）都是事故高发区。本模块讲透抽象层与 Redis 实现层，并给出生产级配置模板

---

## 📚 目录

1. [Spring Cache 抽象：三层结构](#1-spring-cache-抽象三层结构)
2. [四大注解全解](#2-四大注解全解)
3. [缓存名称、key 生成与过期配置](#3-缓存名称key-生成与过期配置)
4. [RedisCacheManager 配置实战](#4-rediscachemanager-配置实战)
5. [4.x 新特性：非阻塞 evict 与 resetCaches](#5-4x-新特性非阻塞-evict-与-resetcaches)
6. [缓存三大难题与注解层应对](#6-缓存三大难题与注解层应对)
7. [生产配置模板与自检清单](#7-生产配置模板与自检清单)

---

## 1. Spring Cache 抽象：三层结构

```text
注解层      @Cacheable / @CachePut / @CacheEvict / @Caching
              │  Spring AOP 拦截（@EnableCaching 激活）
抽象层      CacheManager → Cache
              │  接口：name 管理、key→value 读写
实现层      RedisCacheManager → RedisCache（本模块）
              │  底层：RedisTemplate 字节操作 + TTL + 序列化
```

| 层 | 职责 | 关键点 |
|----|------|--------|
| 注解层 | 声明式缓存语义 | AOP 拦截，`unless`/`condition` 表达式 |
| 抽象层 | 与具体缓存解耦 | 可换 Caffeine/Redis/JCache 实现 |
| 实现层 | Redis 具体行为 | TTL、双序列化、前缀、并发策略 |

> 🎯 **本质认知**：@Cacheable 不关心后端是 Redis 还是本地——**换成 RedisCacheManager，才获得分布式缓存**。Redis 实现的全部特性（TTL/序列化/前缀）都要在实现层配置。

## 2. 四大注解全解

### 2.1 @Cacheable：读缓存，未命中则执行方法

```java
@Cacheable(cacheNames = "user", key = "#id")
public User getUserById(Long id) { ... }

// 常用属性
@Cacheable(
        cacheNames = "user",                       // 缓存名（对应 Redis key 前缀）
        key = "#id + ':' + #lang",                 // SpEL 动态 key
        unless = "#result == null",                // 返回 null 不缓存（防缓存穿透的注解层兜底）
        condition = "#id != null && #id > 0",      // 前置条件：不满足连查询都不走缓存
        sync = true                                // 击穿保护：同 key 并发只放行一个线程查库
)
public User getUser(Long id, String lang) { ... }
```

| 属性 | 语义 | 典型用法 |
|------|------|---------|
| `cacheNames` / `value` | 缓存名（必填） | `"user"`、`"order:list"` |
| `key` | SpEL key 生成 | `#id`、`#user.id`、`T(java.util.Objects).hash(...)` |
| `unless` | 返回结果后判断是否**不**缓存 | `#result == null` 防穿透 |
| `condition` | 执行前判断是否**启用**缓存 | `#id > 0` |
| `sync` | 同 key 并发只查一次库 | 防击穿（Redis 实现为锁语义） |

> ⚠️ **unless vs condition 最易混淆**：`condition` 在方法调用**前**评估（决定走不走缓存），`unless` 在方法返回**后**评估（决定缓存不缓存）；`unless` 里可以引用 `#result`，`condition` 不行。

### 2.2 @CachePut：更新缓存（不读缓存，直接执行方法并写）

```java
@CachePut(cacheNames = "user", key = "#user.id")
public User updateUser(User user) { ... }   // 先更新 DB，再写缓存
```

### 2.3 @CacheEvict：失效缓存

```java
@CacheEvict(cacheNames = "user", key = "#id")
public void deleteUser(Long id) { ... }

// 清空整个缓存区（慎重：会清掉所有 key）
@CacheEvict(cacheNames = "user", allEntries = true)
public void refreshAll() { ... }

// 执行方法前先清缓存（默认执行后清）
@CacheEvict(cacheNames = "user", key = "#id", beforeInvocation = true)
public void deleteWithPreEvict(Long id) { ... }
```

> 💡 `beforeInvocation=true` 场景：方法可能抛异常但缓存必须清（例如"禁用用户"逻辑无论成败都要清）。

### 2.4 @Caching：组合

```java
@Caching(
        put = { @CachePut(cacheNames = "user", key = "#user.id"),
                @CachePut(cacheNames = "user:name", key = "#user.name") },
        evict = { @CacheEvict(cacheNames = "user:list", allEntries = true) }
)
public User saveUser(User user) { ... }
```

## 3. 缓存名称、key 生成与过期配置

### 3.1 key 生成规则

未指定 `key` 时用 `KeyGenerator` 默认实现（`SimpleKeyGenerator`）：参数列表的 `SimpleKey`（如 `SimpleKey [1001]`）。**多参数方法不写 key 会生成 `SimpleKey [a,b]` 这种不可读且易碰撞的 key——生产必须显式声明 key。**

```java
// 推荐：显式 key
@Cacheable(cacheNames = "order", key = "#orderId")
public Order getOrder(Long orderId) { ... }

// 多参数：拼接或 SpEL 对象
@Cacheable(cacheNames = "order:page", key = "#userId + '_' + #page")
public Page<Order> pageOrders(Long userId, int page) { ... }
```

### 3.2 Redis key 的最终形态

```
Redis 实际 key = 前缀(默认 cache:) + 缓存名 + :: + 生成的 key
例：cache:user::1001        （默认 keyPrefix="cache:"，nameSeparator="::"）
```

> ⚠️ **注意**：4.x 默认 key 前缀为 `cache:`，且**多缓存名共享同一前缀**——不同业务缓存名相同生成的 key 不会冲突（因为缓存名拼在 key 里），但运维按前缀区分业务困难，建议按 4.2 节显式指定业务前缀。

## 4. RedisCacheManager 配置实战

### 4.1 最简启用

```java
@Configuration
@EnableCaching   // 激活注解（必须）
public class CacheConfig { }
```

### 4.2 生产级配置

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        // 1. 全局默认配置
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))                       // 全局默认 TTL 30 分钟
                .prefixCacheNameWith("cache:")                          // key 前缀（多业务隔离）
                .disableCachingNullValues()                             // 默认不缓存 null（防穿透靠 unless）
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(StringRedisSerializer.UTF_8))   // key 必须 String
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJacksonJsonRedisSerializer())); // value JSON

        // 2. 按缓存名定制（不同业务不同 TTL）
        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        perCache.put("user", defaults.entryTtl(Duration.ofHours(1)));
        perCache.put("order:page", defaults.entryTtl(Duration.ofMinutes(5)));
        perCache.put("hot:rank", defaults.entryTtl(Duration.ofSeconds(30)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(perCache)   // 初始化缓存区
                .build();
    }
}
```

### 4.3 配置项速查

| 配置方法 | 默认 | 说明 |
|---------|:---:|------|
| `entryTtl(Duration)` | 永不过期 | 全局 TTL；per-cache 覆盖 |
| `prefixCacheNameWith(String)` | `cache:` | key 前缀 |
| `computePrefixWith(...)` | `name + "::"` | 缓存名与 key 的连接符 |
| `disableCachingNullValues()` / `enableCachingNullValues()` | 不缓存 null | 缓存 null 可抗穿透但占空间 |
| `serializeKeysWith / serializeValuesWith` | JDK 序列化（陷阱同 02 篇） | 必须显式配置 |
| `keyPrefix(String)`（按缓存名） | 跟随全局 | 每个缓存可单独前缀 |

## 5. 4.x 新特性：非阻塞 evict 与 resetCaches

### 5.1 非阻塞 evict / clear（Spring Data Redis 4.0）

旧版 `RedisCache` 的 evict/clear 内部用 `KEYS` 模式扫描（如清空 `cache:user::*`），**阻塞 Redis 单线程且跨大量 key 时可能超时**。4.0 起：

- `evict`（单 key）：走原子 `DEL`，天然非阻塞；
- `clear`（全缓存区）：内部改用 **SCAN 游标分批删除**（或 `UNLINK` 异步释放内存），不再一次 `KEYS` 全表扫描——符合 4.0 的非阻塞语义，大缓存区清空不再拖垮服务端。

> 💡 **面试点**：Spring Data Redis 4.0 release notes 明确"非阻塞 eviction for RedisCache"——旧实现 `KEYS` 扫描的阻塞事故（生产常见：清空缓存导致 Redis 卡死数秒）在 4.x 得到根治。

### 5.2 resetCaches() 优化（Spring Data 2026.0.0 / 4.1）

```java
@Resource
private CacheManager cacheManager;

public void refreshAllCaches() {
    if (cacheManager instanceof RedisCacheManager rcm) {
        // 当 Redis 仅用于缓存时：一次 FLUSHDB 重置全部缓存区
        rcm.resetCaches();
    }
}
```

- **语义**：一次性重置**所有** Redis 缓存区，替代逐缓存区 `clear()`；
- **优化点**：内部合并为单次 `FLUSHDB`，从"N 个缓存区 = N 次 SCAN+DEL"降为 1 次命令；
- ⚠️ **前提条件**：仅当 Redis 实例**专用于缓存**时才可安全使用（FLUSHDB 会清掉同库全部数据——包括非缓存业务数据）。

### 5.3 4.x 其它行为变化

| 变化 | 影响 |
|------|------|
| Jackson 3 序列化器默认迁移 | `RedisCacheConfiguration` 默认 value 序列化随包版本走（仍是 JDK 默认，务必显式配置） |
| 缓存名限制 | 某些特殊字符（`::`、通配符）在 key 中需谨慎 |
| `sync=true` 实现 | 底层用分布式锁保证单线程回填（多实例也有效） |

## 6. 缓存三大难题与注解层应对

| 难题 | 问题描述 | 注解层/配置层应对 |
|------|---------|------------------|
| **穿透** | 查询不存在的数据，缓存永不命中，压力直达 DB | ① `unless="#result == null"` 不缓存空结果；② 缓存 null（`enableCachingNullValues` + 短 TTL）；③ 布隆过滤器（代码层） |
| **击穿** | 热点 key 过期瞬间，大量并发查库 | `@Cacheable(sync = true)` 单飞模式 |
| **雪崩** | 大量 key 同时过期，DB 被打爆 | ① 全局 TTL 加随机抖动：`defaults.entryTtl(...)` 与 per-cache 差异化；② 多级缓存（本地 Caffeine + Redis） |

**TTL 随机化（代码层补充）：**

```java
// 自定义 TTL：在 entryTtl 基础上做 ±30% 抖动，打散过期时间
Duration ttl = Duration.ofMinutes(30);
long jitter = (long) (ttl.toMillis() * (0.7 + Math.random() * 0.6));
RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMillis(jitter))
        .serializeKeysWith(...);
```

> 💡 注解层只能应对部分难题：穿透的布隆过滤器、双写一致性（见 10 篇）都需要代码层配合——注解是"地基"，不是"全部"。

## 7. 生产配置模板与自检清单

**生产模板（可直接套用）：**

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .prefixCacheNameWith("cache:")
                .disableCachingNullValues()
                .serializeKeysWith(StringRedisSerializer.UTF_8)
                .serializeValuesWith(new GenericJacksonJsonRedisSerializer());

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaults)
                .build();
    }
}
```

**上线前自检清单：**

- [ ] `@EnableCaching` 已激活（忘开 = 注解静默失效，最常见的"加了注解没效果"）
- [ ] key/value 序列化已显式配置（默认 JDK = 乱码 + 兼容性坑）
- [ ] 所有 @Cacheable 显式声明 key（避免 SimpleKey 不可读/碰撞）
- [ ] 所有查询型缓存写了 `unless = "#result == null"`（防穿透）
- [ ] 热点 key 缓存开启 `sync = true`（防击穿）
- [ ] TTL 已配置且差异化（防雪崩；写 TTL 的缓存名至少一个）
- [ ] 更新/删除方法有配套 @CachePut/@CacheEvict（双写一致性链路完整）
- [ ] 缓存名不包含特殊字符；业务前缀不与其它库冲突

> 🎯 **核心要点**：Spring Cache 注解层解决"声明式缓存"的便利，RedisCacheManager 配置层决定"分布式缓存的正确性"——序列化（JDK 默认是头号坑）、TTL（没配=永不过期）、null 处理（穿透）、sync（击穿）四大配置项缺一不可。4.x 的非阻塞 evict 与 4.1 的 resetCaches（单次 FLUSHDB）是 2026 年新特性的面试加分点。

---

**上一模块**：[05-管道、事务与批量性能优化](05-管道、事务与批量性能优化.md)　**下一模块**：[07-Repository模式与查询](07-Repository模式与查询.md)
