# 05 本地缓存：Caffeine 与 Spring Cache

> 本地缓存是链路中延迟最低的一层（微秒级）——Caffeine 用 W-TinyLFU 把命中率推到同类最高，但分布式一致性、堆内存占用与失效广播是它绕不开的三道坎

---

## 📚 目录

1. [W-TinyLFU：命中率的引擎](#1-w-tinylfu命中率的引擎)
2. [Caffeine 核心配置](#2-caffeine-核心配置)
3. [Spring Cache 集成](#3-spring-cache-集成)
4. [失效广播：跨实例一致性](#4-失效广播跨实例一致性)
5. [选型与红线](#5-选型与红线)

---

## 1. W-TinyLFU：命中率的引擎

Caffeine（2026 基准 3.2.0）的高命中率来自 **W-TinyLFU** 淘汰算法，它同时解决了两代前辈的缺陷：

- **LRU 的缓存污染**：偶发的大流量请求（全表扫描、批处理任务）会把高频热点挤出缓存。LRU 只认"最近用过"，不认"用过多少次"。
- **LFU 的冷启动与僵化**：新热点因初始频率低永远进不了缓存（冷启动）；历史高频数据早就不被访问却长期占位（僵化）。LFU 只认"用过多少次"，不认"是否还热"。

W-TinyLFU 的结构是**窗口缓存（Window Cache）+ 主空间（分段 LRU）+ 频率草图（Count-Min Sketch）**：新数据先进小窗口 LRU 做热度观察，频率草图用极小的内存估算每条数据的访问频率，只有"在窗口里表现出热度"的数据才有资格进入主空间；主空间内再用分段 LRU 保护刚晋升的数据不被立即淘汰。效果是**高频数据留存、新热点快速进入、低频偶发流量无法污染缓存**，实测命中率比传统 LRU 高 10-20%。

## 2. Caffeine 核心配置

```java
Cache<String, Goods> cache = Caffeine.newBuilder()
        .maximumSize(10_000)                       // 容量上限，防内存失控
        .expireAfterWrite(10, TimeUnit.MINUTES)    // 写后过期：绝对失效兜底
        .refreshAfterWrite(5, TimeUnit.MINUTES)    // 读后刷新：过期前异步重建
        .recordStats()                             // 命中率统计，监控必需
        .build();
```

三种构建形态对应不同职责：**Cache**（手动读写，`get/put/invalidate`）适合"写路径明确"的业务缓存；**LoadingCache**（注入 `CacheLoader`，`get(key)` 未命中自动加载）把"查缓存 → 回源 → 回填"三合一，配合 `refreshAfterWrite` 的 `refresh(key)` 就是防击穿的标准形态；**AsyncCache**（异步加载，返回 `CompletableFuture`）适合回源本身较慢（RPC/聚合查询）的场景，避免阻塞读线程。容量控制还有 `maximumWeight` + `weigher` 形态——按 value 字节数而非条数限制，防止大 value 占满内存；注意 `maximumSize` 与 `maximumWeight` 不能同时设置。

三个时间配置的分工最容易混淆：`expireAfterWrite` 是**强过期**——到期后数据不可读，必须回源；`refreshAfterWrite` 是**软刷新**——到期后旧值仍可读，同时异步重新加载新值，**天然防击穿**（[03 篇](03-缓存三大问题：穿透击穿雪崩.md)的永不过期方案即源于此）。生产标准姿势是两者搭配：`refreshAfterWrite(5m) + expireAfterWrite(10m)`，刷新线程池单独命名（如 `cache-refresh-%d`），避免与业务线程池互相干扰。

`recordStats()` 是监控的入口——通过 `cache.stats()` 读取 hitRate/missCount/evictionCount，接入 Micrometer 后每分钟上报命中率（[09 篇](09-缓存监控与容量规划.md)的本地缓存告警依赖它）。

## 3. Spring Cache 集成

Spring Boot 项目通过 `spring-boot-starter-cache` + Caffeine 即可用注解声明缓存：

```java
@Cacheable(cacheNames = "goods", key = "#id", sync = true)
public Goods getGoods(Long id) { ... }

@CacheEvict(cacheNames = "goods", key = "#id")
public void updateGoods(Long id) { ... }

@CachePut(cacheNames = "goods", key = "#id")
public Goods saveGoods(Goods goods) { ... }
```

`@Cacheable` 是 Cache Aside 的注解化（先查缓存，未命中执行方法并回填）；`sync = true` 开启**进程内请求合并**，同一 key 并发未命中时只执行一次方法——这是注解层面防击穿的第一道防线。`@CachePut` 在方法执行后强制写缓存（新增/更新场景），`@CacheEvict` 删除缓存（`allEntries = true` 清空整个缓存名空间），`@Caching` 组合多注解（如"更新 + 失效"一步完成），`@CacheConfig` 类级统一缓存名与 key 策略。

三个高频坑：**不缓存 null**（用 `unless = "#result == null"` 或空值占位，否则穿透 DB）；**不设 maximumSize**（缓存无限膨胀 OOM）；**统一 TTL**（不同数据变更频率不同，按缓存名分档配置，如价格/库存 30-60 秒、静态配置 1 小时）。

还有一个容易被忽视的**事务边界问题**：`@Cacheable` 与 `@Transactional` 叠加时，缓存写发生在事务提交前——事务回滚了缓存却被写入了不存在的数据。解决方式：缓存操作与事务解耦（先提交事务，再通过事件监听器/`TransactionSynchronizationManager` 在提交后触发缓存操作），或在 `@CachePut` 上配合事务回滚逻辑。

## 4. 失效广播：跨实例一致性

Caffeine 是 JVM 内存缓存，**天生不感知其他实例**。分布式部署时实例 A 更新了 DB 并驱逐了自己的本地缓存，实例 B、C 还在为旧数据服务——这就是"缓存分裂"。标准解法是**失效事件广播**：

```text
更新 DB → 发送失效事件（Redis Pub/Sub 或 MQ）→ 所有实例监听 → 本地 invalidate(key)
```

工程要点：自定义 CacheManager 监听事件后按 namespace 分发执行 `caffeineCache.invalidate(key)`；Caffeine 原生只支持精确 key 驱逐，批量场景用 `asMap().keySet().removeIf(k -> k.matches(pattern))` 实现通配匹配；MQ 可能重复投递，驱逐操作要幂等（重复删无害）；实例启动/关闭时的事件丢失要用"本地缓存短 TTL + 定期全量刷新"兜底（[04 篇](04-缓存一致性.md)的多级一致性）。发布/重启也会造成"冷缓存"——新实例本地缓存为空，全部流量瞬间压到 Redis/DB，这个场景靠"启动预热 + 降级限流"过渡（[08 篇](08-缓存并发控制与降级.md)），大促发布窗口避开高峰是硬纪律。

## 5. 选型与红线

| 维度 | Caffeine | Guava Cache | Ehcache |
|---|---|---|---|
| 淘汰算法 | W-TinyLFU | LRU | LFU/LRU 可配 |
| 命中率 | 最高 | 中 | 中 |
| 异步刷新 | ✅ refreshAfterWrite | ❌ | 部分 |
| 堆外/磁盘 | ❌ | ❌ | ✅（大容量场景） |
| 定位 | **新项目默认首选** | 存量项目维护 | 需要超大容量的遗留场景 |

本地缓存的三条红线：**容量必须设上限**（占用应用堆内存，回看 [专题 3](../专题%203：JVM%20性能调优/00-总览.md) 的活跃数据估算——本地缓存容量超过堆的 10% 就要警惕 GC 压力）；**只缓存可容忍短暂不一致的数据**（本地缓存是各实例私有副本，一致窗口天然大于 Redis）；**必须暴露命中率指标**（没有监控的本地缓存是黑盒，删了都不知道，Caffeine `recordStats()` 是唯一的数据来源）。本地缓存与分布式缓存的分工口诀："**能进本地的数据先进本地，Redis 只兜底**"——热点 key、不可变配置、字典类数据天然适合本地；需要跨实例一致的（用户状态、库存余量）绝不进本地。

> 🎯 **核心要点**：W-TinyLFU 用窗口 + 频率草图兼顾"新热点准入"与"高频留存"，命中率比 LRU 高 10-20%；refreshAfterWrite + expireAfterWrite 双时间配置是防击穿的标准姿势；@Cacheable(sync=true) 注解级单飞 + unless 防 null + 按缓存名分档 TTL；分布式下必须用 Redis Pub/Sub 或 MQ 做失效广播，本地缓存只放可容忍短暂不一致的数据。

---

**下一模块**：[06 分布式缓存：Redis 与 Memcached](06-分布式缓存：Redis与Memcached.md)

**返回总览**：[00-总览](00-总览.md)
