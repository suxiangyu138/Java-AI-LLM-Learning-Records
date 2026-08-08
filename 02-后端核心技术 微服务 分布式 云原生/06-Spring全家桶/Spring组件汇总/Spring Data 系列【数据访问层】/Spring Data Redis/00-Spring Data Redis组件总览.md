# 00 Spring Data Redis 组件总览

> 组件卡片：Spring Data Redis 是什么、版本现状、能做什么、与深度体系如何衔接——本系列是"组件汇总"速查型文档，Redis 引擎本身的深挖见 [Redis 深度体系（01-08）](../../../../02-非关系型数据库/Redis/01-Redis入门与安装配置.md)

---

## 📚 目录

1. [组件一句话定位](#1-组件一句话定位)
2. [版本现状（2026-08）](#2-版本现状2026-08)
3. [能力地图](#3-能力地图)
4. [与深度体系的映射](#4-与深度体系的映射)
5. [快速上手 3 步](#5-快速上手-3-步)
6. [速查导航](#6-速查导航)
7. [学习路线推荐](#7-学习路线推荐)
8. [核心概念速查](#8-核心概念速查)

---

## 1. 组件一句话定位

**Spring Data Redis 是 Spring Data 家族中面向 Redis 的缓存与数据访问组件**——用 `RedisTemplate` 模板、序列化器体系和 Spring Cache 抽象，把 Redis 的 K/V、集合、Stream、Lua 能力封装成"Spring 风格的编程体验"，是 Java 生态对接 Redis 8.x 的标准姿势。

```text
核心心智模型：
  业务代码
    ├── RedisTemplate（同步：Value/Hash/List/Set/ZSet/Stream 六大 Operations）
    ├── ReactiveRedisTemplate（响应式：Mono/Flux，Lettuce 响应式驱动）
    ├── Spring Cache 抽象（@Cacheable → RedisCacheManager）
    ├── Repository（@RedisHash 对象映射 + 二级索引）
    └── 消息（@RedisListener / StreamMessageListenerContainer）
            ↓
  RedisConnectionFactory（Lettuce 7.5.x / Jedis 7.4.x 双驱动）
            ↓
  Redis 8.4 集群（内存 KV + 持久化 + 哨兵/集群 + 搜索/语义缓存）
```

| 维度 | 说明 |
|------|------|
| 所属 | Spring Data 家族（spring-data-redis） |
| 版本线 | 2026.0 发行列车 → 4.1.x（2026-08 当前线） |
| 对接引擎 | Redis 8.4（2025-11 GA，8.4.4 为 2026-06 最新） |
| 驱动 | Lettuce 7.5.1 / Jedis 7.4.1（4.1 起 Jedis 走 **UnifiedJedis** 新 API） |
| 配套 | Spring Framework 7.0.x / Spring Boot 4.1.x |
| 定位 | **缓存 + 数据结构服务 + 消息/Stream + 对象仓库**四合一数据访问层 |

### 1.1 Spring Data Redis 解决什么问题

直接用原生客户端（Jedis/Lettuce）写 Redis，开发者要面对：**连接管理**（线程安全、池化、驱动差异）、**序列化**（key/value 编码、类型安全、跨语言兼容）、**样板操作**（每次 get/set 都要处理连接与序列化）。Spring Data Redis 收编为：连接工厂抽象（屏蔽驱动差异）、模板（自动连接与序列化）、类型化 Operations、Spring Cache 注解、消息监听容器。

| 维度 | 原生客户端 | Spring Data Redis |
|------|-----------|-------------------|
| 连接 | 手管连接/池 | `RedisConnectionFactory` 自动池化 |
| 序列化 | 手写编解码 | `RedisSerializer` 体系（key/value 分设） |
| 操作 | 命令直调 | 六大类型化 Operations + 函数式 API |
| 缓存 | 手写缓存逻辑 | `@Cacheable` + RedisCacheManager |
| 消息 | 手写监听线程 | 容器化监听 + @RedisListener 注解 |
| 与 Spring 生态 | 无集成 | 事务绑定、可观测（Micrometer）、Boot 自动配置 |

> 🎯 判断标准一句话：**"业务里有没有 Redis 连接/序列化/缓存的重复代码？"**——有，用 Spring Data Redis；纯运维脚本/一次性命令，redis-cli 直接敲更轻。

### 1.2 适用与不适用场景

| 场景 | 适合？ | 说明 |
|------|:---:|------|
| 缓存（热点数据、会话、验证码） | ✅ | Spring Cache + RedisCacheManager 是主战场 |
| 计数/限流/排行榜（原子操作） | ✅ | ZSet/INCR/Lua 是 Redis 强项 |
| 分布式锁/防重 | ✅ | Lua 原子脚本（[06 篇](06-分布式锁与Lua脚本速查.md)） |
| 轻量消息/任务队列 | ✅ | Pub/Sub（丢消息）+ Stream（可靠，[09 篇](09-消息Pub-Sub与Stream速查.md)） |
| 语义缓存 / AI 上下文（2026 新能力） | ✅ | Redis 8.4 定位 AI 上下文引擎（[05 篇](05-缓存实战：Spring-Cache与一致性速查.md) 6 节） |
| 强事务/强一致的核心业务数据 | ❌ | Redis 是缓存/加速层，**事务性主数据用 MySQL**（[JPA 系列](../../Spring%20Data%20系列【数据访问层】/Spring%20Data%20JPA/00-Spring%20Data%20JPA组件总览.md)） |
| 复杂查询/报表/多条件检索 | ❌ | 内存数据结构服务，不是查询引擎 |

> ⚠️ **最大认知误区**：把 Redis 当"主数据库"用。Redis 是**缓存/加速/协作层**——数据可丢失（除非 AOF+主从）、无复杂查询、事务语义有限；**业务主数据存 MySQL，Redis 只放"丢得起"或"能重建"的数据**（[Redis 持久化深度](../../../../02-非关系型数据库/Redis/04-Redis持久化与数据安全.md)）。

### 1.3 与其他 Spring Data 组件的定位差异（家族横向对比）

| 组件 | 存储 | 并发模型 | 数据模型 | 本系列的差异点 |
|------|------|---------|---------|--------------|
| Spring Data JPA | 关系型 | 阻塞 | 表/实体/关联 | 事务强、查询强、有缓存/懒加载 |
| Spring Data MongoDB | MongoDB | 双模 | 文档+向量 | 查询/聚合/向量 |
| Spring Data Elasticsearch | ES 9 | 双模 | 文档+倒排+向量 | 全文检索 |
| Spring Data R2DBC | 关系型 | **非阻塞** | 聚合根 | 响应式 SQL |
| **Spring Data Redis** | **Redis 8.4** | **双模（同步/响应式）** | **K/V + 集合 + Stream** | **无查询 DSL、无事务（有限）、缓存与消息是主战场** |

> 💡 本系列定位"缓存与协作层"——Redis 在家族里是**唯一没有查询语言、没有持久化强保证**的组件，它的价值是**速度与原子性**；Repository/Pageable/审计同源，但序列化与消息是 Redis 独有的深水区。

## 2. 版本现状（2026-08）

| 发行列车 | Spring Data Redis | Redis 服务端 | Spring Boot | 状态 |
|----------|------------------|-------------|-------------|------|
| **2026.0** | **4.1.x（4.1.0，2026 GA）** | **8.4（2025-11 GA，8.4.4）** | **4.1.x** | **Current（当前线）** |
| 2025.1 | 4.0.x | 8.0/8.2 | 4.0.x | Stable |
| 2025.0 | 3.5.x | 7.4/8.0 | 3.5.x | 停止维护 |
| 2024.1 | 3.4.x | 7.2/7.4 | 3.4.x | 停止维护 |
| 2021.2 | 2.7.x | 5.x/6.x | 2.7.x | EOL |

> ⚠️ **版本策略（2026 起）**：Redis 8.4 是 2026 主线（AI 上下文引擎定位，FT.HYBRID 混合检索、原子槽迁移）；**新项目直接 Boot 4.1 + SDR 4.1 + Redis 8.4**；存量 Boot 3.5 项目停留在 3.5.x 且已停止维护。

### 2.1 4.1 线关键变化（4.0 → 4.1 迁移要点）

| 变化 | 说明 |
|------|------|
| **Jedis 走 UnifiedJedis** | 4.1 头条：legacy Jedis API 替换为 `JedisRedisClient`/`RedisClusterClient`（Jedis 7.4.1）；`JedisClusterConnection` 构造器废弃（[01 篇](01-模块清单.md) 专节） |
| @RedisListener 注解式监听 | 2026.0 新特性：注解声明监听器（替代纯容器配置，[09 篇](09-消息Pub-Sub与Stream速查.md)） |
| RedisMessageSendingTemplate | 新模板：消息发送统一入口 |
| DIGEST 命令支持 | 连接与模板层支持 Redis 8.4 的 DIGEST（完整性校验） |
| CacheManager.resetCaches() 优化 | 4.1：单次 FLUSHDB 完成全部缓存重置（原逐缓存清空） |
| 类型安全属性路径 | 全家族 4.1 特性（Criteria/Update 层） |
| 依赖升级 | Lettuce 7.5.1 / Jedis 7.4.1 / Commons Pool 2.13.1 |

### 2.2 Redis 8.x 服务端演进（服务端侧）

| 版本 | 里程碑 |
|------|--------|
| 8.0（2025-05） | 性能与可观测大版本；HGETDEL/HGETEX/HSETEX 新命令 |
| 8.2 | 增量优化 |
| **8.4（2025-11）** | **AI 上下文引擎**：FT.HYBRID（全文+向量混合检索、RRF）、向量 SIMD 加速（AVX2/AVX512）、**原子槽迁移 ASM**（CLUSTER MIGRATION）、DELEX/MSETEX/DIGEST、**条件 SET/DEL（compare-and-set）**、XREADGROUP CLAIM min-idle-time、AOF 自动修复 |
| 8.4.4（2026-06） | 8.4 线最新补丁（安全支持中） |

> 💡 服务端能力的窗口：**条件 SET/DEL、DELEX、DIGEST 等新命令需要 Redis 8.0+/8.4**——低版本集群调用会报 `unknown command`；升级前确认集群版本（[04 篇](04-操作API与Redis-8-4新命令速查.md) 有版本对照表）。

## 3. 能力地图

| 能力域 | 能力点 | 对应注解/API |
|--------|--------|-------------|
| 连接管理 | 驱动抽象/池化/哨兵集群 | `RedisConnectionFactory`（Lettuce/Jedis） |
| 模板操作 | 同步类型化操作 | `RedisTemplate` + 六大 `Operations` |
| 序列化 | key/value 编解码体系 | `RedisSerializer` 家族、`StringRedisTemplate` |
| 缓存抽象 | 声明式缓存 | `@Cacheable`/`@CachePut`/`@CacheEvict` + `RedisCacheManager` |
| 对象映射 | 实体 ↔ Hash 映射 | `@RedisHash`、`@Indexed`、`RedisRepository` |
| 脚本 | Lua 原子执行 | `DefaultRedisScript`（eval/evalsha） |
| 管道/事务 | 批量与原子 | `executePipelined`、`@Transactional` 绑定、multi/exec |
| 消息 | Pub/Sub + Stream | `RedisMessageListenerContainer`、`@RedisListener`、`StreamMessageListenerContainer` |
| 响应式 | 非阻塞全链路 | `ReactiveRedisTemplate`、`ReactiveRedisConnectionFactory` |
| 可观测 | 命令级指标/追踪 | Micrometer + 连接工厂观测回调 |
| 分布式能力 | 锁/限流/防重 | Lua 脚本、Redisson 对照（[06 篇](06-分布式锁与Lua脚本速查.md)） |

### 3.1 能力边界：Spring Data Redis 不做什么

| 能力边界 | 谁来做 | 说明 |
|---------|--------|------|
| 数据持久化/高可用 | Redis 集群（AOF/主从/哨兵/集群） | 组件只做访问（[Redis 深度体系](../../../../02-非关系型数据库/Redis/04-Redis持久化与数据安全.md)） |
| 查询语言/复杂检索 | 无（内存数据结构） | 需要复杂查询选 ES/MongoDB |
| 强事务 | MySQL + 分布式事务 | Redis 事务无回滚、无隔离级别（[07 篇](07-管道事务与批量性能速查.md)） |
| 消息可靠性 | MQ（Kafka/RocketMQ/Pulsar） | Pub/Sub 丢消息；Stream 接近 MQ 但功能有限（[09 篇](09-消息Pub-Sub与Stream速查.md) 选型） |
| 嵌入生成/语义缓存向量 | 嵌入模型 + Redis 8.4 能力 | Redis 只存与检索（[05 篇](05-缓存实战：Spring-Cache与一致性速查.md) 6 节） |

> ⚠️ **常见归因错误**：缓存数据丢了怪"Spring Data Redis"——丢失是持久化配置问题（AOF/主从）；乱码是序列化配置问题（[03 篇](03-序列化器与实体映射速查.md)）；排查路径："连得上吗 → 序列化对吗 → 数据该不该放 Redis"。

## 4. 与深度体系的映射

| 速查文档 | 深度体系模块 |
|---------|-------------|
| 02-连接配置速查 | [Redis-01 入门与安装配置](../../../../02-非关系型数据库/Redis/01-Redis入门与安装配置.md)、[Redis-05 高可用架构](../../../../02-非关系型数据库/Redis/05-Redis高可用架构.md) |
| 03-序列化与映射速查 | [Redis-02 核心数据结构详解](../../../../02-非关系型数据库/Redis/02-Redis核心数据结构详解.md) |
| 04-操作API速查 | [Redis-02/03 数据结构](../../../../02-非关系型数据库/Redis/03-Redis高级数据结构.md) |
| 05-缓存实战速查 | [Redis-06 缓存实战与常见问题](../../../../02-非关系型数据库/Redis/06-Redis缓存实战与常见问题.md) |
| 06-锁与Lua速查 | [Redis-08 Spring Boot集成Redis与Lua](../../../../02-非关系型数据库/Redis/08-Spring%20Boot集成Redis与Lua.md) |
| 07-管道事务速查 | [Redis-04 持久化与数据安全](../../../../02-非关系型数据库/Redis/04-Redis持久化与数据安全.md) |
| 09-消息篇 | [Redis-07 应用场景实战](../../../../02-非关系型数据库/Redis/07-Redis应用场景实战.md) |

> 💡 分工约定：**速查页回答"API 怎么写"，深度页回答"引擎怎么工作"**——数据结构实现、持久化、高可用原理在 Redis 深度体系；本系列聚焦"连接/序列化/模板/缓存/消息的编排"。

## 5. 快速上手 3 步

**① 引入依赖**（Boot 4.1 + Redis 8.4）：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

**② 配置连接**（application.yml，默认 Lettuce）：

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password:                    # 无认证留空
      lettuce:
        pool:
          max-active: 16           # 连接池（Lettuce 池化需 commons-pool2 依赖）
          max-idle: 8
```

**③ 声明模板 + 使用**：

```java
@Configuration
public class RedisConfig {
    @Bean
    RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> t = new RedisTemplate<>();
        t.setConnectionFactory(factory);
        t.setKeySerializer(new StringRedisSerializer());       // key 必须 String！
        t.setValueSerializer(new GenericJackson2JsonRedisSerializer());  // value 用 JSON
        return t;
    }
}

// 使用
@Service
public class ProductCacheService {
    private final RedisTemplate<String, Object> redisTemplate;

    public void cacheProduct(Long id, Product p) {
        redisTemplate.opsForValue().set("product:" + id, p, Duration.ofMinutes(10));
    }

    public Product getProduct(Long id) {
        return (Product) redisTemplate.opsForValue().get("product:" + id);
    }
}
```

### 5.1 快速上手补充：本地起 Redis 8.4

```bash
docker run -d --name redis84 -p 6379:6379 redis:8.4
# 生产：主从 + 哨兵/集群（见 Redis 深度体系 05 篇）
```

> 💡 三步验证：`SET 写 → GET 读 → redis-cli 核对`——**乱码检查第一步**：redis-cli 里 key 能看清（String 序列化）、value 是 JSON（Jackson 序列化）即序列化配置 OK（[03 篇](03-序列化器与实体映射速查.md)）。

### 5.2 排障起点

| 症状 | 第一步检查 |
|------|-----------|
| `Connection refused` | redis-cli 手动连（[02 篇](02-快速开始与连接配置速查.md) 6 节） |
| key 乱码 `\xAC\xED\x00\x05t...` | 序列化器配置（[03 篇](03-序列化器与实体映射速查.md)） |
| `NOAUTH Authentication required` | password 配置（[02 篇](02-快速开始与连接配置速查.md)） |
| `unknown command` | Redis 版本低于 8.x 新命令要求（[04 篇](04-操作API与Redis-8-4新命令速查.md)） |

## 6. 速查导航

| 文档 | 内容 |
|------|------|
| [01-模块清单](01-模块清单.md) | artifact 坐标、驱动双轨、**UnifiedJedis 迁移专节**、包结构、职责切割 |
| [02-快速开始与连接配置速查](02-快速开始与连接配置速查.md) | starter、连接工厂、Lettuce/Jedis 差异、连接池、哨兵/集群、TLS、Boot 属性 |
| [03-序列化器与实体映射速查](03-序列化器与实体映射速查.md) | 序列化家族、JDK 陷阱、Jackson 3 迁移、@RedisHash/@Indexed、乱码排查 |
| [04-操作API与Redis 8.4新命令速查](04-操作API与Redis-8-4新命令速查.md) | 六大 Operations、条件 SET/DEL、DELEX/MSETEX/DIGEST、函数式 API |
| [05-缓存实战：Spring Cache与一致性速查](05-缓存实战：Spring-Cache与一致性速查.md) | @Cacheable 家族、RedisCacheManager、穿透/击穿/雪崩、双写一致性、**语义缓存（AI）** |
| [06-分布式锁与Lua脚本速查](06-分布式锁与Lua脚本速查.md) | Lua 原子、锁四方案对比、限流/防重、Redisson 对照 |
| [07-管道事务与批量性能速查](07-管道事务与批量性能速查.md) | pipeline、事务绑定、watch、批量、性能量化对比 |
| [08-响应式：ReactiveRedisTemplate速查](08-响应式与ReactiveRedisTemplate速查.md) | **响应式三件套（新补）**、Lettuce 响应式优势、全链路呼应 R2DBC、阻塞陷阱 |
| [09-消息Pub/Sub与Stream速查](09-消息Pub-Sub与Stream速查.md) | 监听容器、@RedisListener、Stream 消费组、8.4 CLAIM 改进、可靠性选型 |
| [10-集成地图与常见问题](10-集成地图与常见问题.md) | 家族联动、3.x→4.1 迁移清单、高频坑排错表、测试实践 |

### 6.1 阅读顺序建议

- **第一次接触**：03 → 04 → 05，先掌握"序列化、操作、缓存"三件套；
- **项目实战**：02（连接）→ 03（序列化）→ 04（操作）→ 05（缓存）→ 06（锁）→ 10（避坑）；
- **准备面试**：05（缓存难题）→ 06（分布式锁）→ 07（管道事务）→ 03（序列化）；
- **响应式/AI**：08（响应式，配合 [R2DBC 系列](../../Spring%20Data%20系列【数据访问层】/Spring%20Data%20R2DBC/00-Spring%20Data%20R2DBC组件总览.md)）→ 05 第 6 节（语义缓存）；
- **升级迁移**：10 篇迁移清单（UnifiedJedis 重点）。

### 6.2 前置知识建议

| 前置主题 | 所在文档 | 与本系列的关系 |
|---------|---------|--------------|
| Redis 数据结构 | [Redis-02/03](../../../../02-非关系型数据库/Redis/02-Redis核心数据结构详解.md) | Operations 的语义基础 |
| 持久化/高可用 | [Redis-04/05](../../../../02-非关系型数据库/Redis/04-Redis持久化与数据安全.md) | 缓存丢失与集群连接的前提 |
| Spring Data 通用模型 | Spring Data JPA 系列（同级目录） | Repository/审计同源 |
| 响应式模型 | [R2DBC 系列](../../Spring%20Data%20系列【数据访问层】/Spring%20Data%20R2DBC/08-WebFlux响应式集成速查.md) | 08 篇响应式的全链路视角 |

## 7. 学习路线推荐

| 路线 | 适用 | 顺序 |
|------|------|------|
| 入门速查 | 会写缓存 | 00 总览 → 02 连接 → 03 序列化 → 04 操作 → 05 缓存 |
| 项目实践 | 上生产做缓存/锁 | 03 → 05 → 06 → 07 → 10 避坑 |
| 面试冲刺 | 全考点 | 05 缓存难题 → 06 锁 → 07 管道事务 → 03 序列化 → 08 响应式 |
| AI/响应式 | 语义缓存 + 全链路 | 08 响应式 → 05 语义缓存 → 09 消息 |

## 8. 核心概念速查

| 术语 | 一句话解释 |
|------|-----------|
| RedisConnectionFactory | 连接层抽象（Lettuce/Jedis 两实现，屏蔽驱动差异） |
| RedisTemplate | 线程安全的高级模板（连接 + 序列化 + 六大 Operations） |
| StringRedisTemplate | key/value 全 String 序列化的专用模板 |
| Operations | 按数据类型划分的类型化接口（Value/Hash/List/Set/ZSet/Stream） |
| RedisSerializer | 序列化器（key 与 value 可分别指定，JDK/JSON/String/自定义） |
| @Cacheable | 声明式缓存（方法结果进 Redis，Cache-Aside 自动实现） |
| @RedisHash | 实体 ↔ Hash 映射（对象直接存为 Hash + 二级索引） |
| Lua 脚本 | 服务端原子执行（分布式锁/限流的地基） |
| Pipeline | 批量命令减少 RTT（无原子性） |
| @RedisListener | 2026 注解式消息监听（Pub/Sub 通道） |
| Stream | 日志型消息队列（消费组/ACK/可靠消费） |
| ReactiveRedisTemplate | 响应式模板（Mono/Flux，Lettuce 响应式驱动） |
| UnifiedJedis | Jedis 7 统一客户端 API（4.1 迁移目标） |
| FT.HYBRID | Redis 8.4 混合检索（全文+向量，RRF 融合） |
| 语义缓存 | 8.4 AI 定位：向量相似度命中缓存（[05 篇](05-缓存实战：Spring-Cache与一致性速查.md) 6 节） |

---

**下一模块**：[01-模块清单](01-模块清单.md)　**返回总览**：本页

**【参考来源】**：[Spring Data 2026.0.0 GA（thenote）](https://thenote.app/post/en/spring-data-2026-0-0-generally-available-w6vcearl6t)、[spring-data-redis 4.1.0-RC1 Release（NewReleases）](https://newreleases.io/project/github/spring-projects/spring-data-redis/release/4.1.0-RC1)、[Jedis UnifiedJedis 迁移指南（DeepWiki）](https://deepwiki.com/redis/jedis/2.2-unifiedjedis-and-modern-clients)、[Redis 8.4 GA 公告（官方）](https://redis.io/blog/redis-8-4-open-source-ga.md)、[Redis 8.4 新特性（官方博客）](https://redis.io/blog/whats-new-in-two-november-2025-edition/)
