# 08 响应式：ReactiveRedisTemplate 速查

> ReactiveRedisTemplate 响应式三件套、Lettuce 响应式驱动优势、与 R2DBC 的全链路组合、阻塞陷阱、响应式事务边界——"Redis 的非阻塞另一半"完整手册（本系列新增深度模块）

---

## 📚 目录

1. [为什么 Redis 是响应式家族的天然成员](#1-为什么-redis-是响应式家族的天然成员)
2. [ReactiveRedisTemplate 三件套](#2-reactiveredistemplate-三件套)
3. [响应式缓存与语义缓存](#3-响应式缓存与语义缓存)
4. [与 R2DBC 的全链路组合](#4-与-r2dbc-的全链路组合)
5. [阻塞陷阱与性能](#5-阻塞陷阱与性能)
6. [响应式测试](#6-响应式测试)

---

## 1. 为什么 Redis 是响应式家族的天然成员

**通俗**：Redis 协议是"发命令等响应"的网络 IO——**天生适合非阻塞**；Lettuce 基于 Netty 就是为此而生。响应式栈（WebFlux + R2DBC + ReactiveRedis）里 Redis 是"最容易响应式化"的一环（同步 JDBC 换 R2DBC 有代价，Lettuce 响应式零额外成本）。

| 驱动 | 同步 | 响应式 | 说明 |
|------|:---:|:---:|------|
| **Lettuce** | ✅ RedisTemplate | ✅ **ReactiveRedisTemplate（原生）** | Netty 非阻塞，共享连接 |
| Jedis | ✅ | ⚠️（阻塞模型，不推荐） | 响应式场景直接排除 |

```text
响应式全链路（2026 主流组合）：
  WebFlux（Netty）→ WebClient（第三方）→ ReactiveRedisTemplate（Redis）
                                          → R2DBC（MySQL/PostgreSQL，[R2DBC 系列](../../Spring%20Data%20系列【数据访问层】/Spring%20Data%20R2DBC/00-Spring%20Data%20R2DBC组件总览.md)）
  全程非阻塞：一个线程同时服务 N 个请求（IO 等待期间线程去干别的）
```

> 🎯 面试必答：**"Redis 响应式和同步怎么选？"**——响应式项目（WebFlux 全链路）**Redis 必须用 ReactiveRedisTemplate**（同步模板会阻塞事件循环线程）；传统 MVC 项目用同步模板即可——**判断标准只有一个：你的 HTTP 层是 WebFlux 还是 MVC**（[阻塞陷阱](#5-阻塞陷阱与性能)）。

## 2. ReactiveRedisTemplate 三件套

### 2.1 模板与类型化操作（与同步一一对应）

```java
// 注入（Boot 自动装配）
ReactiveRedisTemplate<String, Object> reactiveTemplate;

// 类型化操作：ReactiveValueOperations / ReactiveHashOperations / ...（与同步六面板一一对应）
ReactiveValueOperations<String, Object> v = reactiveTemplate.opsForValue();

// 读写（Mono/Flux）
Mono<Boolean> set = v.set("product:1", product, Duration.ofMinutes(10));
Mono<Object> get = v.get("product:1");

// 原子操作（响应式）
Mono<Long> incr = v.increment("counter:views");              // INCR
Mono<Boolean> lock = v.setIfAbsent("lock:1", "token", Duration.ofSeconds(30));  // SETNX

// 批量（响应式管道：Flux 流式）
Flux<Object> batchGet = Flux.fromIterable(keys)
        .flatMap(k -> v.get(k));                             // 并发批量读
```

| 同步 API | 响应式 API | 返回 |
|---------|-----------|------|
| `redisTemplate.opsForValue()` | `reactiveTemplate.opsForValue()` | ReactiveValueOperations |
| `opsForHash/List/Set/ZSet/Stream` | 同（Reactive 前缀） | ReactiveXxxOperations |
| `executePipelined` | `execute(ReactiveRedisCallback)` | Flux<Object> |
| `execute(script, ...)` | `execute(ReactiveScriptExecutor)` | Mono/Flux |

### 2.2 响应式连接与序列化（与同步共用配置）

```java
// 序列化配置与同步完全一致（同一套约定，见 03 篇）
@Bean
ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {
    ReactiveRedisTemplate<String, Object> t = new ReactiveRedisTemplate<>(factory,
            RedisSerializationContext.<String, Object>newSerializationContext()
                    .key(StringRedisSerializer.UTF_8)
                    .value(new GenericJackson2JsonRedisSerializer())
                    .hashKey(StringRedisSerializer.UTF_8)
                    .hashValue(new GenericJackson2JsonRedisSerializer())
                    .build());
    return t;
}
```

| 要点 | 说明 |
|------|------|
| 连接工厂 | `ReactiveRedisConnectionFactory`（Lettuce 实现，Boot 自动装配） |
| 序列化 | 与同步 RedisTemplate **同一套约定**（key String/value JSON）——否则同步写响应式读乱码 |
| 连接模型 | Lettuce **共享连接 + 非阻塞**（不需要池化，[02 篇](02-快速开始与连接配置速查.md) 3 节） |

> ⚠️ **双模板一致性问题**：项目同时用 RedisTemplate（同步）和 ReactiveRedisTemplate 时，**序列化器必须完全一致**（复制配置）——两条路径读写同一批 key，不一致 = 跨端乱码（[03 篇](03-序列化器与实体映射速查.md) 铁律）。

### 2.3 响应式 Lua 与消息

```java
// 响应式 Lua（限流/锁在响应式链路里照常原子）
Mono<Long> allowed = reactiveTemplate.execute(
        new ReactiveRedisScript<>("...lua...", Long.class),
        List.of("rl:user:1"), "1", "5");

// 响应式消息监听（[09 篇](09-消息Pub-Sub与Stream速查.md)）
ReactiveRedisMessageListenerContainer container;   // 响应式容器（Mono/Flux 订阅消息）
```

## 3. 响应式缓存与语义缓存

### 3.1 响应式 Spring Cache（ReactiveCacheManager）

```java
// Spring Cache 的响应式实现（@Cacheable 在响应式方法上可用）
// 依赖：spring-data-redis 4.x 提供 ReactiveRedisCacheManager
@Bean
ReactiveRedisCacheManager cacheManager(ReactiveRedisConnectionFactory factory) {
    RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeValuesWith(SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()));
    return ReactiveRedisCacheManager.builder(factory)
            .cacheDefaults(defaults)
            .build();
}

// 使用（@Cacheable 对 Mono/Flux 生效）
@Cacheable(cacheNames = "product", key = "#id")
public Mono<Product> getProduct(Long id) { return productRepository.findById(id); }
```

> 💡 响应式缓存与同步缓存**同一个注解**（@Cacheable），区别在 CacheManager 实现（ReactiveRedisCacheManager）——响应式方法（返回 Mono/Flux）配响应式 CacheManager，同步方法配同步的，别混用。

### 3.2 响应式语义缓存（AI 链路）

```text
WebFlux 请求 → 嵌入（Reactive 嵌入调用）→ ReactiveRedisTemplate 向量检索
            → 命中返回 / miss 调 LLM（WebClient）→ 回填
全程非阻塞——AI 应用的响应式语义缓存链路（[05 篇](05-缓存实战：Spring-Cache与一致性速查.md) 6 节 + Redis 8.4）
```

## 4. 与 R2DBC 的全链路组合

```java
// 全链路响应式示例：查库（R2DBC）+ 缓存（ReactiveRedis）组合
@Service
@RequiredArgsConstructor
public class ProductReactiveService {
    private final ProductRepository productRepository;          // R2DBC（[R2DBC 系列](../../Spring%20Data%20系列【数据访问层】/Spring%20Data%20R2DBC/04-Repository速查.md)）
    private final ReactiveRedisTemplate<String, Object> redis;

    // Cache-Aside（响应式版）：缓存 → miss 查库 → 回填
    public Mono<Product> getProduct(Long id) {
        String key = "product:" + id;
        return redis.opsForValue().get(key)                     // ① 查缓存（响应式）
                .cast(Product.class)
                .switchIfEmpty(productRepository.findById(id)   // ② miss 查库（R2DBC 非阻塞）
                        .flatMap(p -> redis.opsForValue()
                                .set(key, p, Duration.ofMinutes(10))   // ③ 回填
                                .thenReturn(p)));
    }

    // 写路径：更库（R2DBC 事务）+ 删缓存（响应式）
    @Transactional
    public Mono<Product> updatePrice(Long id, BigDecimal price) {
        return productRepository.findById(id)
                .flatMap(p -> { p.setPrice(price);
                        return productRepository.save(p); })
                .flatMap(p -> redis.opsForValue()
                        .delete("product:" + id)                // Cache-Aside：先库后删
                        .thenReturn(p));
    }
}
```

> 🎯 面试必答：**"响应式项目里数据库和 Redis 怎么配合？"**——Cache-Aside 响应式版：`redis.get → switchIfEmpty(db.find) → 回填`；写路径 `R2DBC 事务更新 + ReactiveRedis 删缓存`——**全链路 Mono/Flux 贯穿**，任何一环 block 都会破坏非阻塞（[R2DBC 08 篇](../../Spring%20Data%20系列【数据访问层】/Spring%20Data%20R2DBC/08-WebFlux响应式集成速查.md) 阻塞陷阱同源）。

## 5. 阻塞陷阱与性能

### 5.1 阻塞陷阱（与 R2DBC 系列同源）

| # | 陷阱 | 症状 | 正解 |
|---|------|------|------|
| ① | WebFlux 里用同步 RedisTemplate | 事件循环线程被阻塞（IO 等待） | 换 ReactiveRedisTemplate |
| ② | 响应式方法里 `.block()` | 线程占用 + 全服务卡死 | Mono/Flux 贯穿到底 |
| ③ | 响应式链路里混同步组件 | 局部阻塞拖全链路 | 全组件响应式（[R2DBC 08 篇](../../Spring%20Data%20系列【数据访问层】/Spring%20Data%20R2DBC/08-WebFlux响应式集成速查.md) 3 节） |
| ④ | 响应式里配了连接池 | 池化开销 + 语义冲突 | Lettuce 共享连接即可（[02 篇](02-快速开始与连接配置速查.md) 3 节） |

```java
// 反模式：WebFlux 控制器里用同步模板（block 事件循环）
@GetMapping("/products/{id}")
public Mono<Product> get(@PathVariable Long id) {
    Product p = redisTemplate.opsForValue().get("product:" + id);  // ❌ 同步 IO 在 Netty 线程
    return Mono.justOrEmpty(p);
}
```

### 5.2 响应式批量性能

| 场景 | 同步做法 | 响应式做法 |
|------|---------|-----------|
| 批量读（预热） | executePipelined | `Flux.fromIterable(keys).flatMap(k -> v.get(k))` |
| 批量写 | executePipelined | `Flux.fromIterable(data).concatMap(d -> v.set(...))` |
| 并发限制 | 管道全发 | `.flatMap(fn, concurrency)`（**响应式可控制并发度**） |
| 背压 | 无 | 天然背压（消费者声明速率） |

```java
// 响应式批量：并发度可控（比管道更细粒度）
Flux.fromIterable(products)
        .flatMap(p -> v.set("product:" + p.getId(), p), 100)   // 并发 100
        .then();
```

> 💡 响应式批量的优势：**并发度可调 + 背压天然**——比管道"一把梭"更精细（管道要么全发要么分批，响应式按需拉取）；大批量导入的响应式姿势（[R2DBC 07 篇](../../Spring%20Data%20系列【数据访问层】/Spring%20Data%20R2DBC/07-性能优化与批量速查.md) 流式思路同源）。

## 6. 响应式测试

```java
// StepVerifier（与 R2DBC/WebFlux 同一套断言，[R2DBC 08 篇](../../Spring%20Data%20系列【数据访问层】/Spring%20Data%20R2DBC/08-WebFlux响应式集成速查.md) 5 节）
@SpringBootTest
class ReactiveRedisCacheTest {

    @Autowired ReactiveRedisTemplate<String, Object> redis;

    @Test
    void setAndGet_shouldRoundTrip() {
        StepVerifier.create(redis.opsForValue().set("k", "v", Duration.ofMinutes(1)))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(redis.opsForValue().get("k"))
                .expectNext("v")
                .verifyComplete();
    }

    @Test
    void ttl_shouldExpire() {
        StepVerifier.create(redis.opsForValue().set("tmp", "x", Duration.ofMillis(100)))
                .expectNext(true).verifyComplete();
        StepVerifier.create(Mono.delay(Duration.ofMillis(300))
                        .then(redis.opsForValue().get("tmp")))
                .verifyComplete();                    // 过期后为 null → 空 Mono 完成
    }
}
```

| 要点 | 说明 |
|------|------|
| 测试库 | 用独立 Redis（Testcontainers 或本地实例 + 独立 database 编号） |
| 断言 | StepVerifier（expectNext/verifyComplete/verifyError） |
| 清理 | 测试后清 key（FLUSHDB 或按前缀删——**别连生产 Redis 跑测试**） |
| 时序 | 过期/延迟类断言用 `Mono.delay` 推进（勿 sleep） |

> 🎯 测试三坑（与 R2DBC 系列一致）：① 别连生产 Redis（数据污染）；② 断言必须 StepVerifier（`.block()` 会掩盖时序）；③ 同步与响应式模板的序列化一致性要在测试里验证（双模板互通测试必写）。

---

**下一模块**：[09-消息Pub/Sub与Stream速查](09-消息Pub-Sub与Stream速查.md)　**返回总览**：[00-组件总览](00-Spring Data Redis组件总览.md)

**【参考来源】**：[Spring Data Redis 官方参考文档（响应式）](https://docs.spring.io/spring-data/redis/reference/redis/reactive.html)、[Lettuce 官方文档（响应式）](https://lettuce.io/docs/)、[R2DBC 系列-响应式集成](../../Spring%20Data%20系列【数据访问层】/Spring%20Data%20R2DBC/08-WebFlux响应式集成速查.md)
