# Spring Data Redis 知识体系总览

> 从连接工厂、RedisTemplate、序列化器三大地基，到操作 API、Lua、管道事务、Spring Cache 缓存抽象、Repository、Pub/Sub 与 Stream 消息、集群可观测——Spring Data Redis 是 Spring 生态对接 Redis 的官方标准姿势，2026 年随 4.1.x（2026.0.0 发行列车）与 Spring Boot 4.1、Redis 8.x 完成新一轮代际升级

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [为什么必须系统学透 Spring Data Redis](#3-为什么必须系统学透-spring-data-redis)
4. [核心概念速查](#4-核心概念速查)
5. [与周边知识的关系](#5-与周边知识的关系)
6. [学习路线推荐](#6-学习路线推荐)
7. [快速自测 10 题](#7-快速自测-10-题)

---

## 1. 知识体系导图

```text
Spring Data Redis 知识体系
│
├── 01 核心架构：连接工厂与 RedisTemplate
│   ├── RedisConnectionFactory 抽象：Lettuce vs Jedis 两套驱动
│   ├── 连接池、共享连接、超时与配置项全解
│   ├── RedisTemplate 线程安全与底层回调模型
│   └── Spring Boot 4.x 自动配置与 @EnableCaching 装配
│
├── 02 序列化器体系深度剖析
│   ├── RedisSerializer 家族：String / JDK / Jackson / 自定义
│   ├── JDK 序列化三大陷阱：可读性、兼容性、性能
│   ├── Jackson 2 → Jackson 3 迁移（4.x 新命名）
│   ├── key/value 双序列化与 StringRedisTemplate
│   └── 序列化选型对照表与踩坑手册
│
├── 03 RedisTemplate 操作 API 全解
│   ├── ValueOperations / Hash / List / Set / ZSet
│   ├── StreamOperations：XADD/XREAD 与消费组
│   ├── Redis 8.x 新命令：HGETDEL/HGETEX/HSETEX
│   ├── 2026 条件 SET/DEL（compare-and-set / compare-and-delete）
│   └── 类型化操作与泛型边界、null 语义
│
├── 04 StringRedisTemplate 与 Lua 脚本
│   ├── StringRedisTemplate 定位：省去序列化心智负担
│   ├── DefaultRedisScript 与 eval/evalsha 脚本缓存
│   ├── Lua 原子性、沙箱与超时陷阱
│   └── 经典脚本实战：分布式限流、原子计数、防抖
│
├── 05 管道、事务与批量性能优化
│   ├── executePipelined：一次 RTT 执行 N 条命令
│   ├── @Transactional + RedisTemplate 会话绑定
│   ├── multi/exec 与 watch 乐观锁
│   ├── 管道 vs 事务 vs 普通逐条的性能对比
│   └── 4.x 约束：管道与事务互斥、非阻塞 API
│
├── 06 Spring Cache 抽象与 RedisCacheManager
│   ├── @Cacheable / @CachePut / @CacheEvict / @Caching
│   ├── RedisCacheConfiguration：TTL、双序列化、前缀
│   ├── RedisCache 4.0 非阻塞 evict 与 resetCaches 优化
│   ├── 缓存穿透/击穿/雪崩的 Cache 层应对
│   └── 生产级缓存配置模板
│
├── 07 Repository 模式与查询
│   ├── @RedisHash 实体映射与二级索引 @Indexed
│   ├── 查询方法派生与 PagingAndSortingRepository
│   ├── TTL / 过期策略 / 分区与 hash-key 设计
│   └── 适用边界：何时用 Repository、何时该手动
│
├── 08 Pub/Sub 与 Stream 消息集成
│   ├── RedisMessageListenerContainer 回调模型
│   ├── 2026 新注解：@RedisListener + @EnableRedisListeners
│   ├── MessageConverter 与 MIME 类型选择器
│   ├── Stream 消费组：XREADGROUP、ACK、消费者均衡
│   └── 可靠性与死信：Pub/Sub 的丢失与 Stream 的兜底
│
├── 09 集群、哨兵与可观测
│   ├── RedisClusterConnection 命令路由与拓扑刷新
│   ├── 哨兵高可用与 Lettuce/Jedis 集群差异
│   ├── Micrometer Tracing：Redis 命令级观测
│   └── 故障切换下的行为与重试策略
│
└── 10 最佳实践与面试题
    ├── 缓存三大难题 + 双写一致性方案
    ├── key 设计、命名空间与可观测规范
    ├── 生产故障案例与排查手册
    └── 面试高频 15 问
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|----------|---------|
| 01 | 核心架构：连接工厂与 RedisTemplate | Lettuce/Jedis 驱动、连接池、RedisTemplate 回调模型、Boot 自动配置 | 入门必读 |
| 02 | 序列化器体系深度剖析 | 序列化器家族、JDK 陷阱、Jackson 3 迁移、选型对照 | 入门必读 |
| 03 | RedisTemplate 操作 API 全解 | 五大类型操作、Stream、Redis 8.x 新命令、条件 SET/DEL | 全部 |
| 04 | StringRedisTemplate 与 Lua 脚本 | Lua 脚本机制、原子操作、分布式限流实战 | 进阶 |
| 05 | 管道、事务与批量性能优化 | 管道、事务绑定、watch、性能量化对比 | 进阶 |
| 06 | Spring Cache 抽象与 RedisCacheManager | @Cacheable 家族、RedisCache 配置、三大缓存难题 | 高频核心 |
| 07 | Repository 模式与查询 | @RedisHash、@Indexed 二级索引、查询方法派生 | 进阶 |
| 08 | Pub/Sub 与 Stream 消息集成 | 消息监听容器、@RedisListener、Stream 消费组 | 进阶 |
| 09 | 集群、哨兵与可观测 | 集群路由、拓扑刷新、Micrometer Tracing | 高级 |
| 10 | 最佳实践与面试题 | 缓存一致性、key 设计、故障排查、面试题 | 冲刺 |

## 3. 为什么必须系统学透 Spring Data Redis

**1）它是 Spring 生态对接 Redis 的官方标准通道。** 无论用 Spring Boot 3.x 还是 4.x，`spring-boot-starter-data-redis` 都是事实标准；了解其内部抽象（ConnectionFactory → RedisTemplate → Operations 三层），才能解释"为什么这样写能跑、那样写出乱码/阻塞/NPE"。

**2）序列化是最大的隐性坑。** 90% 的 Spring Data Redis 生产事故源于序列化：默认 JDK 序列化导致的乱码与扩容灾难、类型擦除导致的反序列化失败、Jackson 版本迁移导致的 `@class` 缺失。这套体系把序列化单独拆成一篇，是其他资料少有的深度。

**3）消息与缓存是两大高频业务场景。** Spring Cache 抽象（@Cacheable）与 Redis 消息（Pub/Sub + Stream）是面试与实战双热点；2026 年的 `@RedisListener` 注解式监听、`RedisCache.resetCaches()` 单次 FLUSHDB 优化，都是新特性加分项。

**4）Redis 8.x 新命令与新 API 是差异化竞争力。** Redis 8.0 的 HGETDEL/HGETEX/HSETEX、Redis 8.4 的条件 SET/DEL（compare-and-set），在 Spring Data Redis 4.x 中以函数式 API 直接可用——用官方新能力替代"先 GET 再 SET"的两次往返，是 2026 年面试官期待听到的答案。

> 🎯 **核心要点**：Spring Data Redis 不是"Redis 的 Java 客户端"，而是 Redis 在 Spring 世界中的**完整落地栈**——连接管理、序列化、类型化操作、缓存抽象、消息、Repository、可观测一应俱全。学它 = 学一条从"连上 Redis"到"生产可用"的全链路。

## 4. 核心概念速查

| 概念 | 一句话本质 | 关键类/注解 |
|------|-----------|------------|
| ConnectionFactory | 连接层抽象，屏蔽驱动差异 | `RedisConnectionFactory`（Lettuce/Jedis 实现） |
| RedisTemplate | 线程安全的高级模板，自动管理连接与序列化 | `RedisTemplate<K,V>` |
| StringRedisTemplate | key/value 全 String 序列化的专用模板 | `StringRedisTemplate` |
| Operations | 按数据类型划分的类型化操作接口 | `ValueOperations`/`HashOperations`/`ListOperations`/`SetOperations`/`ZSetOperations`/`StreamOperations` |
| RedisSerializer | 序列化器抽象，key 与 value 可分别指定 | `StringRedisSerializer`/`GenericJacksonJsonRedisSerializer` |
| 管道 Pipeline | 批量发送命令减少 RTT，无原子性保证 | `executePipelined(...)` |
| 事务 Transaction | multi/exec 批量执行，有原子性，可 watch | `@Transactional` + `setEnableTransactionSupport(true)` |
| Lua 脚本 | 服务端原子执行逻辑，替代多次往返 | `DefaultRedisScript<T>` |
| Spring Cache | 缓存抽象注解，适配 Redis 做实现 | `@Cacheable`/`@CachePut`/`@CacheEvict` + `RedisCacheManager` |
| Repository | 领域对象映射与二级索引查询 | `@RedisHash`/`@Indexed`/`RedisRepository` |
| 消息监听 | 订阅通道的容器化回调 | `RedisMessageListenerContainer`/`@RedisListener`(2026) |
| Stream | Redis 5+ 的日志型消息队列，支持消费组 | `StreamOperations`/`StreamMessageListenerContainer` |
| 可观测 | 命令级指标与追踪 | Micrometer + `RedisConnectionFactory` 观测回调 |

## 5. 与周边知识的关系

```text
                    ┌─────────────────────────────┐
                    │  Redis 底层知识体系（02-非关系型数据库/Redis） │
                    │  数据结构 / 持久化 / 高可用 / 集群 / 场景      │
                    └──────────────┬──────────────┘
                                   │ 提供协议与能力（RESP、命令集、集群拓扑）
                    ┌──────────────▼──────────────┐
                    │   Spring Data Redis（本体系）  │
                    │  连接 / 序列化 / 模板 / 缓存 / 消息 │
                    └──────┬───────────────┬───────┘
                           │               │
          ┌────────────────▼───┐   ┌───────▼────────────────┐
          │ Spring Cache 抽象   │   │ Spring Messaging 体系   │
          │ （06-Spring全家桶/    │   │ @RedisListener 基于     │
          │  Spring框架核心-事件） │   │ Spring Messaging 构建   │
          └────────────────────┘   └────────────────────────┘
```

- **向上承接**：[Redis 底层知识库（08-Spring Boot集成Redis与Lua）](../../02-非关系型数据库/Redis/08-Spring Boot集成Redis与Lua.md) 讲"Redis 是什么、怎么集成"；本体系讲"Spring Data Redis 内部是什么、为什么、怎么用到生产"。
- **横向联动**：[Spring框架核心-配置管理](../Spring框架核心/08-配置管理-Environment与配置属性.md) 的 `spring.data.redis.*` 属性绑定、[Spring生态-技术选型](../Spring生态/05-Spring技术选型实战指南.md) 中的缓存/消息选型对比。
- **向下延伸**：分布式锁（Redisson）、分布式限流（Sentinel 与 Lua）、延迟任务（Redis ZSet）等场景，均以本体系的模板能力为地基。

## 6. 学习路线推荐

**路线 A（初级 · 快速上手 3 天）**
01 核心架构 → 02 序列化 → 03 操作 API → 06 Spring Cache。目标：能独立完成缓存读写、解决乱码、写对 @Cacheable。

**路线 B（中级 · 生产工程师 1 周）**
在路线 A 基础上加：04 Lua 脚本 → 05 管道事务 → 08 Pub/Sub 与 Stream。目标：能实现分布式限流/防抖、缓存一致性方案、消息可靠消费。

**路线 C（高级 · 架构与面试冲刺）**
全套 01-10，重点 02（序列化深潜）、06（缓存难题）、09（集群可观测）、10（面试题）。目标：能讲透 RedisTemplate 源码级原理、设计缓存/消息架构、回答 2026 年新特性面试题。

## 7. 快速自测 10 题

1. RedisTemplate 是线程安全的吗？底层为什么依赖回调模型而非直接持有连接？
2. 为什么默认的 JdkSerializationRedisSerializer 是生产陷阱？乱码长什么样？
3. Jackson 2 与 Jackson 3 序列化器在 Spring Data Redis 4.x 中的命名是什么？`@class` 丢失会报什么错？
4. `opsForValue().set(key, value, ttl)` 与 `expire()` 分开调用有什么一致性风险？
5. 管道和事务的区别是什么？在 Lettuce 驱动下能否同时使用？
6. `@Cacheable` 的 `unless` 与 `condition` 有什么区别？key 是怎么生成的？
7. RedisCache 4.0 的 evict 非阻塞化解决了什么问题？`resetCaches()` 优化了什么？
8. Pub/Sub 消息会丢失吗？要可靠消费应该用什么？
9. `@RedisListener(topic = "my-channel")` 是什么版本引入的？依赖哪些基础设施？
10. Redis 8.4 的条件 SET 相比"先 GET 再 SET"省了几次往返？API 长什么样？

> 💡 答不上的题，对应的模块序号就是你的学习优先级；答不出的答案全在本体系文档里。

---

**下一模块**：[01-核心架构：连接工厂与 RedisTemplate](01-核心架构：连接工厂与RedisTemplate.md)　**返回总览**：本页
