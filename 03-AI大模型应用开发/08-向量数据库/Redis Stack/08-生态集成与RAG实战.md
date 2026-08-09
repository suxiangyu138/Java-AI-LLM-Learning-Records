# 生态集成与 RAG 实战

> Spring AI 官方 RedisVectorStore、Spring Data Redis、Redis OM Spring、Redisson——Java 接入 Redis 8 向量能力的四条路径 + 语义缓存 RAG 链路

## 1. Spring AI：官方 RedisVectorStore

Spring AI 2.0 官方提供 Redis 向量存储（artifact 名 1.x → 2.x 有变化）：

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-vector-store-redis</artifactId>
</dependency>
```

```yaml
spring:
  ai:
    vectorstore:
      redis:
        initialize-schema: true     # 2.0 起默认 false，必须显式开启（1.x 破坏性变化）
        index-name: spring-ai-index
        prefix: embedding:          # key 前缀
```

```java
RedisVectorStore vectorStore = RedisVectorStore.builder(jedisPooled, embeddingModel)
        .indexName("custom-index")
        .prefix("embedding:")
        .vectorAlgorithm(Algorithm.HNSW)          // 或 FLAT
        .distanceMetric(DistanceMetric.COSINE)    // COSINE / L2 / IP
        .hnswM(16)                                // HNSW 图连接数
        .hnswEfConstruction(200)                  // 建图候选
        .hnswEfRuntime(10)                        // 查询候选
        .initializeSchema(true)
        .build();
```

检索 API 与三套向量库体系一致（`VectorStore` 统一抽象）：`similaritySearch(SearchRequest)`、`searchByRange("问题", 0.8)`（阈值范围检索）、`searchByText(...)`（全文检索）、`Filter.builder().eq("category", "AI").and().eq("year", 2023)`（元数据过滤）——**换库只改依赖与配置**是 Spring AI 的通用红利（与 Chroma/Qdrant/PgVector 体系 08 篇同结论）。

## 2. Spring Data Redis 与 Redis OM Spring

**Spring Data Redis**：`RedisTemplate` 直连（`StringRedisTemplate` 管 Hash/JSON 字符串，JSON 序列化用 Jackson）——向量字节用 `RedisTemplate<String, byte[]>` 或直接 `JedisPooled`（Spring AI RedisVectorStore 内部即 Jedis 客户端）。

```java
// Spring Data Redis 直连 FT 查询（不走 Spring AI 的场景）
StringRedisTemplate redis = ...;
List<String> results = redis.execute((RedisCallback<List<String>>) conn -> {
    // 原生命令：FT.SEARCH + PARAMS + SORTBY + DIALECT 2
    return conn.execute("FT.SEARCH", "idx:docs",
            "*=>[KNN 5 @embedding $vec]",
            "PARAMS", "2", "vec", blob,
            "SORTBY", "__embedding_score",
            "DIALECT", "2");
});
```

直连的适用场景：**自定义查询形态**（FT.AGGREGATE、VECTOR_RANGE、FT.HYBRID——Spring AI 未封装的命令）；**性能敏感**（跳过抽象层直接控制）；**混合使用**（Spring AI 管标准检索 + 直连管高级查询）。注意 `RedisCallback` 里字节参数的类型转换（`byte[]` vs String），序列化器不一致是直连最常见报错。

**Redis OM Spring**（官方对象映射层）：注解式实体映射 + 自动索引：

```java
@Document(prefix = "doc", indexName = "idx:doc")
public class Document {
    @Id private String id;
    @Indexed(algorithm = Algorithm.HNSW, dimension = 768, distanceMetric = DistanceMetric.COSINE)
    private Vector embedding;      // 注解声明向量字段，自动建 FT 索引
    @Indexed private String category;
}
```

Redis OM 的价值：**实体注解即索引声明**（建表与建索引合一）、向量字段自动序列化、与 Spring AI/DJL 配合自动向量化（`@Vectorize` 注解）。适合"领域模型驱动"的团队——ORM 心智迁移到 Redis 多模型。

## 3. Redisson 4.2：轻量接入

Redisson 4.2.0 实现了 Spring AI Vector Store 并新增 `RSearch.hasIndex()`——Java 侧再添一条路径：**Redisson 用户**（分布式锁/缓存已在用）零新增依赖接入向量检索。Redisson 的定位是"Redis 的 Java 分布式工具库"，其向量能力是对 Query Engine 的封装——**深度 RAG 需求仍以 Spring AI 为主**，Redisson 适合"已有 Redisson 基础设施"的场景。

## 4. 语义缓存：Redis 独有的 RAG 红利

语义缓存是"缓存型"存储的独有能力：**缓存键不是精确字符串，而是向量相似度**——相似问题命中缓存答案。

```java
// 语义缓存伪代码：问题 → 向量 → 相似度命中 → 直接返回缓存答案
List<Document> hits = cacheStore.similaritySearch(
        SearchRequest.builder().query(question).topK(1)
                .similarityThreshold(0.92)   // 高阈值：只有高度相似才算命中
                .build());
if (!hits.isEmpty()) {
    return hits.get(0).getContent();         // 缓存命中，跳过 LLM 调用
}
// 未命中：调 LLM → 答案写入缓存（问题向量 + 答案 + 元数据）
```

工程要点：**阈值要高**（0.9+，语义缓存误命中 = 答非所问，比缓存 miss 更糟）；**缓存答案要带来源与时间戳**（过期策略）；**命中率监控**（语义缓存的价值 = 命中率 × LLM 成本——命中率 30% 时成本直接降 30%）。**只有 Redis 类存储能天然做语义缓存**（向量检索 + 缓存 TTL + 内存高速命中三合一）——这是 09 篇选型对比里 Redis 8 不可替代的独特点。

语义缓存的三个进阶实践：**分级缓存**——精确命中（问题完全一致，直接用缓存）+ 语义命中（相似问题，标注"近似答案"）+ miss（调 LLM）——精确命中走字符串查询零成本，语义命中走 KNN；**缓存驱逐策略**——按"问题热度"设 TTL（热门问题 TTL 长、冷问题 TTL 短），用 Bloom/Top-K 识别热门问题；**双检防误命中**——语义命中后做一次轻量校验（关键词重叠检查）再返回，误命中率再降一个量级。语义缓存上线前用历史问答对回放评估（命中率 + 误命中率两条指标），达标再上线。

## 5. 完整 RAG 链路（Java 视角）

```text
文档入库：切分 → Embedding → RedisVectorStore.add（JSON 文档 + 向量 + 元数据）
检索：问题 → Embedding → similaritySearch（filter 元数据 + topK）
语义缓存：前置相似度命中检查 → 命中直接返回
生成：上下文 + 问题 → ChatClient 生成回答
统计：TimeSeries 记录检索量与命中率
```

五个关键决策点：**embedding 维度与 DIM 一致**（768/1024/1536 高频值）；**元数据过滤**（TAG/NUMERIC 字段声明进索引，filter 才有效）；**混合检索**（词法 + 向量用 FT.HYBRID 或应用层 RRF，06 篇）；**阈值与 topK 配合**（similarityThreshold 与 distance 方向注意——Redis 距离越小越近，Spring AI 内部换算）；**评估闭环**（命中率 + MRR 标注集对比，与 Chroma/Qdrant/PgVector 体系共用方法论）。

评估闭环落地（与三套向量库体系共用同一套标注集与方法论）：**100-200 条标注问答对**（问题 → 期望命中的文档 id）→ 跑三种检索（纯向量 / 全文 / FT.HYBRID 混合）→ 统计**命中率**（top-5 是否含期望 id）与 **MRR**（期望 id 排名倒数均值）→ 参数变更（EF_RUNTIME、候选量、融合权重）触发重跑。评估脚本进 CI 每日跑——**"Redis 8 的混合检索够不够用"用自己数据的指标回答**，而不是听宣传；语义缓存单独评估**命中率与误命中率**（缓存是降本手段，误命中是质量问题，两条指标分开看）。

## 6. 集成避坑五条

1. **`initialize-schema` 默认 false**（2.0 起）：忘开 = 索引不存在，检索报 Index not found。
2. **距离方向相反**：Redis 距离越小越近；Spring AI 的 similarityThreshold 是相似度——中间换算容易写反，用 `searchByRange` 先验证阈值语义。
3. **JSON 向量是数组、Hash 是二进制**：跨存储形态的读写序列化不一致，报维度错误先查存储形态。
4. **Jedis 连接池**：RedisVectorStore 依赖 `JedisPooled`——连接配置（超时/池大小）按查询 QPS 评估。
5. **FT.* 弃用命令**：存量代码的 `FT.ADD` 等迁移到 `FT.CREATE` + `JSON.SET`（10 篇迁移清单）。

集成排错的分层心智（与 03 篇排错一致）：**连接层**（host/port/密码）→ **索引层**（索引名/initialize-schema/字段类型）→ **数据层**（维度/序列化/prefix 匹配）→ **查询层**（filter 语法/距离方向/DIALECT）——每层报错特征不同（连接拒绝/Index not found/维度报错/结果异常），按层排查；排查入口统一从 `FT.INFO` 看索引配置与 `JSON.GET` 看数据形态开始——**先看全局状态，再钻问题细节**（与 Chroma/Qdrant/PgVector 体系的排错纪律完全一致）。

集成验收的端到端清单（Spring AI + Redis 8 上线前）：**环境**——Redis 版本 8.x（`INFO server` 确认）、客户端库版本匹配（Jedis 2025+）；**索引**——`initialize-schema` 开启、索引名与集合名一致、DIM 与模型一致；**写入**——`add()` 后 `JSON.GET` 确认文档落库、`FT.INFO` 确认索引同步；**检索**——`similaritySearch` 返回分数方向合理（距离转相似度换算）、filter 生效（TAG 字段）；**缓存**——语义缓存阈值验证（误命中率为 0 的标注集抽测）；**监控**——P95 与内存占用进面板。六步全过，集成才算验收合格——与三套向量库体系的"生产落地清单"结构一致，逐项打勾执行。

> 🎯 **核心要点**：Spring AI RedisVectorStore（builder 全参数）+ Redis OM（注解即索引）+ Redisson（轻量）；**语义缓存是独有红利**（高阈值 + 命中率监控）；距离方向相反是跨库迁移必踩点；RAG 五决策点串起"缓存 + 检索 + 生成"全链路。

---

**参考来源**：

- [Spring AI RedisVectorStore API 文档](https://docs.spring.io/spring-ai/docs/2.0.x-SNAPSHOT/api/org/springframework/ai/vectorstore/redis/RedisVectorStore.Builder.html)
- [Spring AI 向量库集成（DeepWiki）](https://deepwiki.com/spring-projects/spring-ai/4-vector-store-integrations)
- [Redis OM Spring 文档](https://redis.github.io/redis-om-spring/redis-om-spring/current/)
- [Redis 8 AI 向量搜索与语义缓存实战（Dev.to）](https://dev.to/chockalingam_rajendran_e0/redis-8-ai-vector-search-semantic-caching-streaming-in-action-13on)

---

**下一模块**：[09-选型对比与决策](./09-选型对比与决策.md) / **返回总览**：[00-Redis Stack知识体系总览](./00-Redis%20Stack知识体系总览.md)
