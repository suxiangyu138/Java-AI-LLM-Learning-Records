# 05 Redis 缓存 Demo

> 给数据加速：装 Redis 8、用 Spring Data Redis 读写缓存，实现"先查缓存再查数据库"的旁路缓存，理解序列化陷阱与缓存一致性问题。缓存是 Level2 项目的必考技术，本 Demo 建立最小可用模型。

## 📚 目录

1. [目标与验收](#1-目标与验收)
2. [Redis 安装与连接](#2-redis-安装与连接)
3. [工程集成：依赖与配置](#3-工程集成依赖与配置)
4. [RedisTemplate 读写与序列化](#4-redistemplate-读写与序列化)
5. [旁路缓存 Demo](#5-旁路缓存-demo)
6. [缓存一致性：先想清楚再动手](#6-缓存一致性先想清楚再动手)
7. [常见坑](#7-常见坑)
8. [核心要点](#8-核心要点)

---

## 1. 目标与验收

本 Demo 的产出：订单查询接口接入 Redis 缓存（第一次查数据库，之后查缓存），更新订单时缓存失效，用 Redis Desktop Manager（或命令行 redis-cli）能看到 key 的变化。验收标准：**能讲清旁路缓存的读写流程**；**能解释"为什么 key 要设计成这样"**；**能说出序列化报错的原因与解决**。版本基线：Redis 8（2026-08 主流）、Spring Data Redis 4.x（随 Boot 4.1 管理）。

Redis 的核心心智先建立：**它是内存中的数据结构服务器**——key-value 存储、单线程执行命令（所以快、所以简单）、支持字符串/哈希/列表/集合/有序集合五种结构。缓存只是它最常见的用法，不是它的全部。

## 2. Redis 安装与连接

Windows 安装 Redis：Redis 官方不支持 Windows，用 Memurai 或微软维护的 Windows 移植版，最省心的方式是 **Docker Desktop 跑官方镜像**（`docker run -d --name redis -p 6379:6379 redis:8`）——Level1 阶段用 Docker 跑中间件，比折腾 Windows 移植版稳定得多（Docker 安装见仓库 Docker 体系）。装完验证：命令行 `redis-cli ping` 返回 PONG 即成功。

验证工具链：`redis-cli` 是命令行的"数据库面板"——`SET hello world`、`GET hello`、`EXPIRE hello 60`、`KEYS *` 四条命令先练熟。看数据结构用 `TYPE key`，看过期时间用 `TTL key`。本 Demo 全程用 redis-cli 观察 Java 代码写入的数据，这是"理解缓存生效"的最直观方式。

**五种数据结构的一句话用途**顺手记下（Level2 面试必问）：String（计数器、缓存值、分布式锁的载体——`SETNX`）、Hash（对象字段的存取，如用户资料）、List（消息队列的简单实现——`LPUSH/BRPOP`）、Set（去重、交集并集——共同好友）、ZSet（有序集合——排行榜、延迟队列的时间戳排序）。每种结构一个典型场景能讲出来即可，不用背命令大全——用的时候查，面试的时候讲场景。

## 3. 工程集成：依赖与配置

在 04 篇工程上加 `spring-boot-starter-data-redis` 依赖（自动包含 Lettuce 连接器，Lettuce 是 Boot 默认客户端）。配置三要素：

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.database=0
```

要点：**database 默认 0**，多业务隔离时用不同 db（Demo 阶段保持 0 即可）；**密码**本地不设（生产必设，Level2 讲）；**连接池** Boot 4 的 Lettuce 默认连接池关闭，高并发场景再配 `spring.data.redis.lettuce.pool.*`——Demo 阶段不配，知道这个参数存在即可。启动后日志无 Redis 报错、redis-cli 里 `CLIENT LIST` 能看到一条来自 Java 的连接，即集成成功。

## 4. RedisTemplate 读写与序列化

Spring Data Redis 的核心类 RedisTemplate——写入与读出的序列化机制是本 Demo 最大的坑源：

```java
@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        // key 用 String 序列化，value 用 JSON 序列化
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();
        template.setKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashKeySerializer(keySerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
```

不配序列化器的后果：**默认 JDK 序列化**——key 变成 `\xac\xed\x00\x05t\x00...` 一串乱码（redis-cli 里 KEY 全是这种），value 也读不出来。这是 90% 新手第一次用 Redis 必踩的坑，配置两行解决。**StringRedisTemplate** 是简化版（key/value 都是字符串），做计数、简单 KV 直接注入它，不需要上面这段配置——本 Demo 两个都试一下，理解它们的差异。

## 5. 旁路缓存 Demo

给 04 篇的订单查询接口加缓存，实现**旁路缓存（Cache Aside）**——业界最常用模式：读时先查缓存，未命中查库再回填；写时先更新数据库，再删除缓存。

```java
@Service
public class OrderCacheService {
    private static final String KEY_PREFIX = "order:";

    @Autowired private StringRedisTemplate redis;
    @Autowired private OrderService orderService;

    public OrderEntity getByIdCached(Long id) {
        String key = KEY_PREFIX + id;
        String json = redis.opsForValue().get(key);
        if (json != null) return JSONUtil.toBean(json, OrderEntity.class);  // 命中缓存
        OrderEntity entity = orderService.getById(id);                      // 未命中查库
        if (entity != null) redis.opsForValue().set(key, JSONUtil.toJsonStr(entity), Duration.ofMinutes(30));
        return entity;
    }

    public void updateAndEvict(Long id) {
        orderService.updateById(...);      // 先更新数据库
        redis.delete(KEY_PREFIX + id);     // 再删缓存
    }
}
```

三个要点：**key 设计**（`order:1`——业务前缀 + 主键，冒号是 Redis 的命名习惯，同类 key 能统一管理）；**TTL 必须设**（`Duration.ofMinutes(30)`——不设 TTL 的缓存会无限膨胀，这是生产事故级别的坏习惯）；**JSON 序列化存值**（value 存 JSON 字符串，与 StringRedisTemplate 配套，简单直观）。

## 6. 缓存一致性：先想清楚再动手

缓存最经典的问题是**一致性**——数据库与缓存里的数据不一致。本 Demo 用"先更新数据库、再删除缓存"的顺序，理解两个顺序为什么是这个选择：**先删缓存再更新库**会在"删缓存后、更新库前"的窗口期让其他请求把旧数据读回缓存（缓存了旧值，且永远不会再更新——因为下次更新时缓存又被删，但读请求可能再次回填旧值）；**先更新库再删缓存**的窗口期只影响"读到旧值的这一次请求"，且缓存最终会被删掉，下次读必然是新值。**删除比更新好**的原因：更新缓存需要知道新值（多一次查询成本），删除只要一个命令；且并发写时"更新"会有覆盖顺序问题，"删除"天然幂等。

面试延伸（Level1 知道结论即可）：删缓存失败的兜底（延迟双删、消息队列重试）放 Level2；"为什么不用更新缓存"的答案是"删除更简单且天然幂等"。本 Demo 的验证方法：查一次订单（缓存生成）→ redis-cli 看 key → 改数据库数据（绕过接口）→ 再查接口（还是旧值，因为缓存还在）→ 调更新接口（缓存被删）→ 再查接口（新值，缓存重建）——亲手跑一遍这个序列，一致性问题的全貌就直观了。

## 7. 常见坑

**连接拒绝**：Redis 没启动或端口不对。`redis-cli ping` 验证，Docker 环境检查容器状态与端口映射。

**key 乱码**：RedisTemplate 没配序列化器，JDK 默认序列化。按第 4 节配置，或改用 StringRedisTemplate。

**反序列化报错**：value 存了 JSON 但读取时类型不匹配，或实体类没有无参构造器。JSON 存值用与读取时相同的类型；MyBatis-Plus 实体有 Lombok 注解，注意 JSON 反序列化需要默认构造器与 setter。

**类型转换异常**：Redis 里存的不是字符串（比如意外存了字节数组）。统一用 StringRedisTemplate 或统一配置序列化器，不要混用。

**缓存击穿/穿透**：Demo 阶段知道名词与危害即可（热点 key 失效瞬间高并发打库、查询不存在的数据每次都穿透到库），解决方案（互斥锁、布隆过滤器）是 Level2 内容。

## 8. 核心要点

1. Redis 是内存数据结构服务器：单线程、五种结构、TTL——缓存只是最常用用法。
2. Windows 用 Docker 跑官方镜像最省心，redis-cli 是观察缓存的最佳工具。
3. 序列化配置是第一个坑：key 用 String、value 用 JSON，两行配置解决。
4. 旁路缓存：读先查缓存再查库回填，写先更新库再删缓存；TTL 必须设。
5. 删除缓存优于更新缓存：更简单且幂等，一致性窗口最小。

> 🎯 **核心要点**：本 Demo 的技术价值不在"会用 RedisTemplate"，而在**"缓存一致性为什么这么设计"的思考过程**——先更新库再删缓存、删除优于更新，每个选择都有明确的并发理由。这个思考习惯是 Level2 面试深挖时"讲得出取舍"的来源。

---

**下一模块**：[06 调用大模型 API 的第一个 Demo](./06-调用大模型%20API%20的第一个%20Demo.md) | **返回总览**：[Level1 总览](./00-Level1%20基础%20Demo%20总览.md)
