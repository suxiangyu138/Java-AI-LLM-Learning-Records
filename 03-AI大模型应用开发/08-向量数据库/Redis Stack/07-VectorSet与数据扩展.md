# Vector Set 与数据扩展

> Vector Set（beta）命令族、RedisJSON 深度、TimeSeries 时序、Bloom 概率结构——Redis 8 多模型的完整拼图

## 1. Vector Set（beta）：原生向量类型

Vector Set 是 Redis 8 引入的**一等公民向量数据结构**，命令族：

| 命令 | 职责 | 示例 |
|------|------|------|
| `VADD` | 添加向量 | `VADD vs:items DIM 4 DISTANCE_METRIC COSINE <blob> MEMBER "id1"` |
| `VSIM` | 相似检索 | `VSIM vs:items COUNT 10 <blob>` |
| `VREM` | 删除向量 | `VREM vs:items MEMBER "id1"` |
| `VCARD` / `VDIM` | 基数/维度查询 | `VCARD vs:items` |
| `VINFO` | 配置查看 | `VINFO vs:items` |
| `VRANDMEMBER` | 随机采样 | `VRANDMEMBER vs:items COUNT 3` |

设计定位（Redis 创始人 Salvatore Sanfilippo 设计）：**数据即索引**——向量集本身就是检索入口，不需要 FT.CREATE 声明索引结构。与 Query Engine 向量索引的取舍：

| 维度 | Vector Set | FT 向量索引 |
|------|-----------|------------|
| 建索引 | 无需（VADD 即建） | FT.CREATE 声明 |
| 业务字段 | 无（纯向量 + MEMBER id） | 与文档字段共存 |
| 混合查询 | 无 | 原生支持 |
| 状态 | **beta** | 生产主路径 |

**生产判断：VSET 仍是 beta——生产用 FT 向量索引**；VSET 适合"纯向量检索原型、极简语义"（如去重检测、快速相似判断）；MEMBER 是业务关联的钩子（应用层用 MEMBER 反查业务数据）。

VSET 的使用场景扩展：**语义去重**（新文档向量与库内 VSIM 相似度超阈值即判重复——配合 Bloom 指纹做两级去重）；**相似推荐**（用户行为向量入 VSET，VSIM 找相似用户/物品——无业务字段需求的纯相似场景）；**原型验证**（FT 索引建好之前先用 VSET 验证向量质量与阈值——零配置起手）。VSET 的命令极简（VADD/VSIM 两个主力），学习成本是四套向量库体系里最低的入口——**从 VSET 入门理解"向量检索是什么"，再进 FT 索引掌握生产形态**，是一条平滑的学习路径。

## 2. RedisJSON：嵌套文档与 JSONPath

RedisJSON 是 JSON 文档的一等公民存储：

```bash
JSON.SET user:1 $ '{"name": "张三", "tags": ["java", "ai"], "profile": {"level": 5}}'
JSON.GET user:1 $.profile.level        # → 5（JSONPath 取值）
JSON.MERGE user:1 $ '{"profile": {"level": 6}}'   # 部分更新（8.0 增强）
```

与 FT 索引的配合：**JSON 字段经 JSONPath 声明进索引**（`$.profile.level AS level NUMERIC`）——嵌套字段可检索；`JSON.MERGE` 是 8.0 的部分更新增强（替代"读-改-写"）。三个注意：**JSON 的向量是浮点数组**（Hash 才是二进制）；**JSONPath 语法**（`$` 根、`.` 属性、`[]` 数组）与业务解析共用；**大 JSON 文档的性能**（单文档 MB 级时评估拆分——Redis 是内存存储，文档越大内存越贵）。

JSON 的高级操作速查：**数组操作**——`JSON.ARRAPPEND doc:1 $.tags "ai"`、`JSON.ARRTRIM`（截断）、`JSON.ARRINDEX`（定位）；**数值操作**——`JSON.NUMINCRBY doc:1 $.count 1`（原子自增）；**多路径取值**——`JSON.GET doc:1 $.a $.b`（一次取多个字段，减少往返）；**批量**——`JSON.MGET`（多 key 同路径取值）。工程要点：**`JSON.SET` 整文档覆盖**（部分更新用 `JSON.MERGE` 或路径级 SET）；**索引声明与写入路径一致**（`$.profile.level` 声明了 NUMERIC，写入就要保证该路径存在——路径缺失的文档不会被索引该字段）。

## 3. RedisTimeSeries：时序数据

```bash
TS.CREATE ts:cpu:1 RETENTION 86400000          # 保留 24h
TS.ADD ts:cpu:1 * 42.5
TS.RANGE ts:cpu:1 - + AGGREGATION avg 60000    # 按分钟聚合
```

核心能力：**采样 + 保留策略**（RETENTION 自动过期）、**聚合查询**（AVG/MIN/MAX/SUM 按时间桶）、**压缩存储**（时序特有的高效编码）。适用场景：监控指标、IoT 传感器、业务趋势统计——**与向量检索无直接关系，但它是"业务数据同库"拼图的一部分**（RAG 应用里"按时间过滤向量结果"的时序元数据可同库）。

## 4. RedisBloom：概率结构

| 结构 | 命令族 | 用途 | 典型场景 |
|------|--------|------|---------|
| Bloom Filter | `BF.ADD`/`BF.EXISTS` | 存在性判断（可误判） | 去重、防穿透 |
| Cuckoo Filter | `CF.ADD`/`CF.EXISTS` | 支持删除的布隆 | 动态集合去重 |
| Count-Min Sketch | `CMS.INCRBY`/`CMS.QUERY` | 频率估计 | 热点统计 |
| Top-K | `TOPK.ADD`/`TOPK.LIST` | 高频 Top-K | 热榜 |
| t-digest | `TDIGEST.ADD`/`TDIGEST.QUANTILE` | 分位数估计 | 延迟分布 |

工程要点：**概率结构是"内存换准确度"**——布隆误判率 1% 时省 90% 内存；**向量场景的配套用途**——RAG 的去重（文档指纹防重复入库）、缓存穿透防护（语义缓存的前置存在性检查）。

时序与概率结构的实战细节：**TimeSeries 的标签过滤**——`TS.CREATE ts:cpu:1 LABELS host server1`（标签用于多序列聚合，`TS.MRANGE ... FILTER host=server1`）；**Bloom 的容量预估**——`BF.RESERVE key error_rate capacity`（误判率与容量建前声明，动态扩容性能差）；**Top-K 的 K 值**——`TOPK.RESERVE key k width depth`（K 是榜单长度，width/depth 影响精度）。三个结构都不需要"建表"式配置（Bloom 除外）——写入即用，与 Redis 的"命令即 API"哲学一致。

## 5. 多模型组合：一个 RAG 场景的全景

把四类结构串起来（RAG 应用）：

```text
文档入库：JSON（文档元数据 + 向量）→ FT 索引（全文 + TAG + VECTOR）
去重检查：Bloom Filter（文档哈希，防重复入库）
语义缓存：Hash/JSON（问题 → 答案）+ VSIM 或 FT 向量（相似度命中）
趋势统计：TimeSeries（检索量、缓存命中率）
```

**"业务数据 + 缓存 + 向量 + 统计同库同实例"**是 Redis 8 多模型的完整价值——一个实例覆盖 RAG 的全部存储面，运维面收敛到 Redis 一个服务（对比独立向量库 + 独立搜索 + 独立缓存的组合）。

多模型组合的运维纪律（一个实例多个模型的治理）：**命名空间隔离**（key 前缀分区：`doc:`/`cache:`/`vec:`/`ts:`——`INFO keyspace` 按前缀对账）；**内存预算分账**（各模型的内存占用定期统计——向量、缓存、时序各占多少，超预算定位到模型）；**淘汰策略按池设置**（`maxmemory-policy` 是实例级的，模型间的"淘汰优先级"靠 TTL 而非淘汰策略——向量数据不设 TTL、缓存数据设 TTL，让"该走的走、该留的留"）；**备份一致性**（RDB/AOF 覆盖全部模型，恢复演练要验证向量索引的完整性——`FT.INFO` 的 `indexing` 状态在恢复后要确认）。

多模型与"多实例"的边界判断（什么时候拆实例）：**性能隔离**（缓存高频写 + 向量高频读互相拖累时——拆成"缓存实例 + 向量实例"）；**容量隔离**（向量膨胀挤占缓存预算——拆实例让各自预算独立）；**故障隔离**（一个模型的问题不应拖垮全部——生产重要度分层时拆）。判断口诀：**"同库"是默认，"拆实例"是手段**——先同库跑（零成本），监控指标证明互相拖累再拆（指标驱动，与 09 篇决策纪律一致）。

> 🎯 **核心要点**：Vector Set 是 beta 轻量替代（生产主路径是 FT 向量索引）；RedisJSON 嵌套文档 + JSONPath 进索引；TimeSeries/Bloom 是"同库拼图"；多模型组合让 RAG 的存储面收敛到一个 Redis 实例——这是它 vs 独立向量库的架构性差异。

---

**参考来源**：

- [Redis 8.0 新特性解读（OneUptime）](https://oneuptime.com/blog/post/2026-03-31-redis-whats-new-80-vector-sets-modules/view)
- [Redis Stack 模块深度解读（CalmOps）](https://calmops.com/database/redis/redis-stack-modules-overview/)
- [Redis 8 新数据结构与向量搜索（Scaled2C）](https://scaled2c.com/blog/ai-native-software-development/redis-80-new-data-structures-and-vector-search.html)

---

**下一模块**：[08-生态集成与RAG实战](./08-生态集成与RAG实战.md) / **返回总览**：[00-Redis Stack知识体系总览](./00-Redis%20Stack知识体系总览.md)
