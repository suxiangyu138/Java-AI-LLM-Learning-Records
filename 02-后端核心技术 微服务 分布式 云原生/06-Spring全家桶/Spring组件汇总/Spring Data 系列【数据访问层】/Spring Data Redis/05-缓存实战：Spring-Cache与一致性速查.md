# 05 缓存实战：Spring Cache 与一致性速查

> @Cacheable 家族、RedisCacheManager 配置（4.1 resetCaches）、穿透/击穿/雪崩三大难题、双写一致性方案、语义缓存（Redis 8.4 AI）——"缓存为什么慢/丢/不一致"到"怎么修"的完整手册

---

## 📚 目录

1. [Spring Cache 抽象速查](#1-spring-cache-抽象速查)
2. [RedisCacheManager 配置速查](#2-rediscachemanager-配置速查)
3. [缓存三大难题：穿透、击穿、雪崩](#3-缓存三大难题穿透击穿雪崩)
4. [双写一致性方案](#4-双写一致性方案)
5. [缓存设计红线](#5-缓存设计红线)
6. [语义缓存：Redis 8.4 的 AI 新玩法](#6-语义缓存redis-84-的-ai-新玩法)

---

## 1. Spring Cache 抽象速查

**通俗**：`@Cacheable` 一行注解 = "方法结果自动进缓存，下次同参直接命中返回"——Cache-Aside 模式的开箱即用。

### 1.1 注解家族

| 注解 | 语义 | 示例 |
|------|------|------|
| `@Cacheable` | 先查缓存，miss 才执行方法并回填 | 商品详情、配置查询 |
| `@CachePut` | **总是**执行方法并更新缓存 | 写路径刷新缓存 |
| `@CacheEvict` | 删除缓存（beforeInvocation 可选） | 更新/删除后失效 |
| `@Caching` | 组合多个缓存操作 | 一写多删 |
| `@CacheConfig` | 类级默认（cacheNames 等） | 统一缓存名 |

```java
@Service
public class ProductService {
    // ① 读：缓存 miss 才查库（Cache-Aside 自动）
    @Cacheable(cacheNames = "product", key = "#id")
    public Product getProduct(Long id) {
        return productRepository.findById(id).orElseThrow();
    }

    // ② 写：更新库 + 刷新缓存
    @CachePut(cacheNames = "product", key = "#id")
    @Transactional
    public Product updatePrice(Long id, BigDecimal price) {
        Product p = productRepository.findById(id).orElseThrow();
        p.setPrice(price);
        return productRepository.save(p);          // 返回值回填缓存
    }

    // ③ 删：删库 + 删缓存
    @CacheEvict(cacheNames = "product", key = "#id")
    @Transactional
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}
```

| 关键点 | 说明 |
|--------|------|
| 开启 | 启动类 `@EnableCaching` |
| key 生成 | SpEL（`#id`/`#user.id`）；默认 SimpleKey 拼接参数 |
| condition | 进缓存前判断（`#id > 0`） |
| unless | 结果不缓存条件（`#result == null`——**null 默认不缓存，防穿透要特殊处理**） |
| 失效 | **方法内自调用失效**（同类 this 调用——AOP 代理，同 [JPA 事务失效](../../Spring%20Data%20系列【数据访问层】/Spring%20Data%20JPA/06-事务与并发控制速查.md) 同源） |

> 🎯 面试必答：**"@Cacheable 的 condition 和 unless 区别？"**——condition 在**方法执行前**判断（决定要不要查缓存）；unless 在**方法执行后**判断（决定结果要不要写缓存）——`@Cacheable(condition = "#id > 0", unless = "#result == null")` 是标准组合（空结果不缓存，防穿透）。

### 1.2 缓存名与 key 设计

```text
Redis 存储形态：
  product::1                # cacheNames=product + key=1（RedisCache 默认前缀 "::"）
  product::1001             # 可读、可按前缀清理（SCAN product::*）

团队规范：
  ① cacheNames 用业务域（product/user/order）
  ② key 用业务主键（#id/#orderNo），不用 hashcode
  ③ 前缀可配（usePrefix + cachePrefix）
```

> ⚠️ key 设计红线：**SpEL key 别拼可变值**（时间戳/随机数 → 缓存永不命中）；**别把整个对象当 key**（序列化开销 + 不可读）。

## 2. RedisCacheManager 配置速查

### 2.1 基础配置

```java
@Configuration
public class CacheConfig {
    @Bean
    RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))                     // 默认 TTL
                .prefixCacheNameWith("app:")                          // key 前缀 app:product::1
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();                          // 默认不缓存 null（防穿透另配）

        // 按缓存名差异化 TTL
        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        perCache.put("session", defaults.entryTtl(Duration.ofHours(2)));
        perCache.put("rank", defaults.entryTtl(Duration.ofMinutes(1)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(perCache)
                .build();
    }
}
```

| 配置项 | 说明 |
|--------|------|
| entryTtl | 默认 TTL（**不设 = 永不过期，Redis 内存风险**） |
| prefixCacheNameWith | key 前缀（多环境/多应用隔离） |
| serializeKeys/Values | 序列化（**与 RedisTemplate 保持同一套约定**） |
| disableCachingNullValues | 默认 true（null 不缓存；要防穿透需单独策略） |

### 2.2 4.1 新能力：resetCaches()

```java
// 4.1：CacheManager.resetCaches() —— 单次 FLUSHDB 完成全部缓存重置
// 优化点：旧版逐缓存 clear → N 次 RTT；新版一次 FLUSHDB → 1 次
// 场景：发版后全量缓存失效（数据格式变更/业务规则变更）
cacheManager.resetCaches();
```

> 💡 4.1 resetCaches 的适用：**发布窗口的"缓存全体失效"**——一次调用全清（注意：是整个 Redis DB 的 FLUSHDB，**同 DB 的其他业务 key 也会被清**——多业务共用 DB 时慎用，或按前缀 SCAN 定向清理）。

## 3. 缓存三大难题：穿透、击穿、雪崩

> 与 [JPA 系列缓存难题](../../Spring%20Data%20系列【数据访问层】/Spring%20Data%20JPA/07-性能优化与批量操作速查.md) 同源三问，此处给 Redis/Spring Cache 视角的解法。

### 3.1 对比表（面试必考）

| 维度 | 穿透 | 击穿 | 雪崩 |
|------|------|------|------|
| 查什么 | **不存在的 key**（攻击/脏数据） | **一个热点 key** 过期 | **大量 key 同时过期** / Redis 宕机 |
| 后果 | 恶意流量打库 | 一个热点打爆库 | 成片过期打爆库 |
| 类比 | 查不存在的人 | 明星同天退休 | 全城同天退休 |

### 3.2 解法速查

| 难题 | 解法 | Spring 落地 |
|------|------|------------|
| **穿透** | ① 缓存空值（短 TTL）② 布隆过滤器 ③ 参数校验 | `@Cacheable(unless = "#result == null")` 默认不缓存 null——**要防穿透需手动缓存空值**（`disableCachingNullValues` 关掉 + unless 放开，短 TTL） |
| **击穿** | ① 互斥锁（SETNX 重建锁）② 逻辑过期 ③ 热点永不过期 + 后台刷新 | `@Cacheable(sync = true)`——**Spring Cache 内建同步重建**（单机锁语义） |
| **雪崩** | ① TTL 加随机值 ② 多级缓存（本地 Caffeine）③ Redis 高可用 | entryTtl 差异化/随机；`@Cacheable` 前加本地缓存层 |

```java
// 击穿解法：sync = true（内建互斥重建，热点 key 过期时只有一个线程查库）
@Cacheable(cacheNames = "hot", key = "#id", sync = true)
public HotData getHot(Long id) { ... }

// 穿透解法：手动缓存空值（关 disableCachingNullValues + unless 放行 + 短 TTL）
@Cacheable(cacheNames = "product", key = "#id",
        unless = "false",                       // 无条件回填（含 null）
        cacheManager = "nullCacheManager")      // 单独配置：允许 null + TTL 60s
```

> 🎯 面试必答：**"缓存击穿怎么防？"**——三层：① `@Cacheable(sync=true)` 内建互斥（单机）；② 逻辑过期/后台刷新（不重建期间读旧值）；③ 热点数据永不过期 + 主动更新——**击穿的本质是"重建缓存的并发风暴"**，互斥锁让重建只有一个线程做（与 [JPA 系列](../../Spring Data 系列【数据访问层】/Spring Data JPA/07-性能优化与批量操作速查.md) 的 Redis 互斥锁解法一致）。

## 4. 双写一致性方案

**通俗**：库和缓存都要更新，顺序错了用户就看旧数据——一致性窗口是缓存的"原罪"，只能缩小、不能消灭。

### 4.1 方案对比

| 方案 | 流程 | 窗口 | 适用 |
|------|------|:---:|------|
| **Cache-Aside（推荐）** | 读：缓存 miss 查库回填；写：**先更库，再删缓存** | 小（删缓存失败时） | **标准姿势** |
| 延迟双删 | 先删缓存 → 更库 → 延迟 1s 再删 | 更小（兜主从延迟） | 主从复制场景 |
| 双写（更新缓存） | 更库 + 写缓存 | **大**（并发写乱序覆盖） | ❌ 不推荐 |
| binlog 订阅 | Canal 监听 binlog → 删/刷缓存 | 小 | 无侵入（[解耦异步](../../../../09-Web开发全流程/后端 分布式常用名词通俗解释/01-解耦与异步.md) 5.2） |

```java
// Cache-Aside 标准姿势（写路径：先库后删）
@CacheEvict(cacheNames = "product", key = "#id")   // ② 删缓存
@Transactional
public void updateProduct(Long id, ...) {
    productRepository.update(...);                   // ① 更库（同事务）
}
// 为什么"删"不是"更新"：更新是覆盖，并发写乱序会覆盖成旧值；删除后读请求重新回填，"谁最后写库谁是新值"。
```

### 4.2 一致性红线

| 红线 | 说明 |
|------|------|
| 强一致数据别缓存 | 资金/库存强一致场景，缓存只做"读优化 + 强校验" |
| 删缓存失败要有补偿 | 删除走消息/重试（[消息可靠性](../../../../03-消息队列/消息队列理论与实战/02-消息可靠性：三大投递语义.md)） |
| 事务边界 | 更库与删缓存**不在同一事务**（缓存无事务）——删缓存放事务成功后 |
| 多实例并发 | 多节点同时重建缓存 → sync=true / 分布式锁 |

> 🎯 面试必答：**"缓存和数据库一致性怎么保证？"**——Cache-Aside + 先库后删：读 miss 查库回填、写先更库再删缓存（删优于更——避免并发写覆盖）；增强：延迟双删兜主从延迟、binlog 订阅免业务代码、删失败走消息重试；**结尾必补**："一致性窗口只能缩小不能消灭，强一致数据不上缓存"——这一句是满分分水岭。

## 5. 缓存设计红线

| # | 红线 | 理由 |
|---|------|------|
| 1 | 大对象/大列表直接缓存 | 序列化/反序列化开销 + 内存浪费 → 缓存"用得到的字段"或分页缓存 |
| 2 | 无 TTL 缓存 | Redis 内存无限增长（LFU 兜底是最后防线，不是设计） |
| 3 | 热点 key 无保护 | 击穿风暴 → sync=true/逻辑过期 |
| 4 | 缓存 key 无前缀 | 多环境/多应用互相污染（prefixCacheNameWith） |
| 5 | 变更序列化器不迁移数据 | 老数据读不了（[03 篇](03-序列化器与实体映射速查.md)） |
| 6 | 缓存里放敏感数据 | Redis 明文存储 → 风险评估（加解密 or 不放） |
| 7 | 缓存当数据库 | 丢了就崩的业务别缓存（Redis 数据可重建才可缓存） |
| 8 | 忽略缓存监控 | 命中率/内存/慢命令无监控 → 缓存黑洞（[Redis-06](../../../../02-非关系型数据库/Redis/06-Redis缓存实战与常见问题.md)） |

## 6. 语义缓存：Redis 8.4 的 AI 新玩法

> **Redis 8.4 定位"AI 上下文引擎"**：语义缓存（semantic caching）让"意思相近的查询"命中同一条缓存——2026 AI 应用热点（[00 篇](00-Spring Data Redis组件总览.md) 2.2）。

### 6.1 什么是语义缓存

```text
传统缓存（精确匹配）：问"手机壳推荐"和"推荐手机壳"→ 两个 key，两次 LLM 调用
语义缓存（向量相似）：两句都嵌入成向量 → 相似度 > 阈值 → 命中同一条 → 省一次 LLM 调用

流程：查询 → 嵌入（Embedding 模型）→ 向量相似检索（FT.HYBRID/VSIM）→ 命中返回缓存，miss 调 LLM 并回填
```

| 对比 | 传统缓存 | 语义缓存 |
|------|---------|---------|
| 匹配 | key 精确 | **向量相似度**（阈值判断） |
| 成本 | 缓存命中省 DB | 命中省 **LLM 调用**（贵得多，价值更大） |
| 实现 | RedisTemplate | Redis 8.4 向量检索 + 嵌入模型 |
| 适用 | 通用缓存 | **AI Agent / LLM 查询去重** |

### 6.2 落地骨架

```java
// ① 查询嵌入化（Spring AI EmbeddingModel）
EmbeddingModel embeddingModel;                       // 生成查询向量
float[] qVec = embeddingModel.embed(query).toArray();

// ② 向量检索（Redis 8.4：VSIM/FT.HYBRID；Java 侧走 RediSearch 客户端或搜索 API）
//    similarity > 0.85 → 命中语义缓存 → 直接返回
//    similarity < 0.85 → 调 LLM → 结果连同向量写入缓存

// ③ 写入：向量 + 答案 + 业务元数据
```

| 注意 | 说明 |
|------|------|
| 命中率与精度 | 阈值调优（0.80-0.90 区间）；**误命中比 miss 更危险**（答非所问） |
| 嵌入模型 | 与缓存数据同一模型（模型换版 = 缓存全失效） |
| 缓存失效 | 业务数据变更时按"主题"清语义缓存（8.4 搜索索引管理） |
| 生态 | Redis 官方与 DeepLearning.AI 出过语义缓存课程；Redis Iris（2026-05）提供 AI 上下文层 |

> 🎯 面试加分句：**"语义缓存是什么？"**——"把 LLM 查询嵌入成向量，Redis 8.4 的向量检索（FT.HYBRID/VSIM）按相似度命中缓存，**省的是 LLM 调用成本**（比省数据库查询贵一个量级）；关键在阈值调优（误命中比 miss 危险）与嵌入模型一致性"——2026 AI 场景面试的新增考点（配合 [RAG 知识体系总览](../../../../../03-AI大模型应用开发/04-RAG检索增强生成/00-RAG知识体系总览.md)）。

---

**下一模块**：[06-分布式锁与Lua脚本速查](06-分布式锁与Lua脚本速查.md)　**返回总览**：[00-组件总览](00-Spring Data Redis组件总览.md)

**【参考来源】**：[Spring Data Redis 官方参考文档（缓存）](https://docs.spring.io/spring-data/redis/reference/redis/cache.html)、[Redis 8.4 语义缓存（官方博客）](https://redis.io/blog/whats-new-in-two-november-2025-edition/)、[Redis-06 缓存实战与常见问题](../../../../02-非关系型数据库/Redis/06-Redis缓存实战与常见问题.md)
