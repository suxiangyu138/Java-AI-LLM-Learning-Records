# 全文检索 RediSearch

> TEXT/TAG/NUMERIC/GEO 四类字段、BM25 默认打分、查询语法、中文分词——Redis 8 内置搜索引擎的完整使用

## 1. 字段类型与索引设计

RediSearch 的字段类型决定查询能力，建索引时选型：

| 字段类型 | 语义 | 查询操作符 | 适用 |
|---------|------|-----------|------|
| **TEXT** | 全文分词 | `@title:关键词` | 标题、正文、摘要 |
| **TAG** | 精确枚举 | `@category:{jvm}` | 分类、状态、标签 |
| **NUMERIC** | 数值范围 | `@price:[100 500]` | 价格、时间戳、计数 |
| **GEO** | 地理位置 | `@loc:[lon lat radius km]` | 门店、坐标 |

设计要点三条：**TAG 是枚举过滤主力**（精确匹配 + 大小写不敏感，比 TEXT 的模糊匹配快）；**同一字段只能声明一种类型**（既想精确又想模糊就声明两个别名，如 `$.category AS category TAG` 与 `$.category AS category_text TEXT`）；**TEXT 字段默认启用分词与倒排索引**——不需要全文检索的字段别声明 TEXT（省内存）。

## 2. 查询语法与 BM25

`FT.SEARCH` 的查询是类 Lucene 语法：`@字段:值` 定位字段、`值1 值2` 隐式 AND、`|` 表示 OR、`-` 表示 NOT：

```bash
# 组合查询：全文 + 枚举 + 数值范围
FT.SEARCH idx:docs "@title:(JVM 内存) @category:{jvm} @year:[2024 2026]"
  SORTBY year DESC LIMIT 0 10 DIALECT 2

# 逻辑组合
FT.SEARCH idx:docs "(@title:向量 | @title:embedding) -@category:{legacy}"
  DIALECT 2
```

**打分（score）**：Redis 8 起 `FT.SEARCH` 默认 **BM25**（替代 TF-IDF）——词频-逆文档频率的现代实现，长文档与常见词的处理更合理。其他可选打分器：`TFIDF`、`BM25STD`、`DISMAX`、`DOCSCORE`（查询里 `SCORER 名` 指定）。三个查询要点：**默认返回按分数降序**（相关度最高在前）；**`LIMIT offset num` 分页**（`LIMIT 0 10` 前 10 条）；**聚合用 `FT.AGGREGATE`**（GROUPBY/统计，替代 SQL 的 GROUP BY）。

## 3. 索引管理与同步

- **加字段**：`FT.ALTER idx:docs SCHEMA ADD $.author AS author TAG`——索引在线加字段，已存数据自动回填。
- **删索引**：`FT.DROPINDEX idx:docs`（默认只删索引保留数据；`DD` 参数连数据删）。
- **同步控制**：写入自动同步（异步），强制同步用 `FT.SYNUPDATE`（同义词组更新）；`FT.INFO` 查看索引状态（`indexing` 字段显示是否同步中）。
- **同义词**：`FT.SYNUPDATE` 维护同义词组（"手机/移动电话"），查询时自动展开——提升召回但不影响精度。

## 4. 中文分词方案

Redis 8 内置分词器对中文的默认处理有限（按整串/简单切分），三个方案：

1. **自带中文分词**：RediSearch 内置了中文分词器（`FT.CREATE ... SCHEMA $.title AS title TEXT` 在中文内容上使用 `TEXT` 时自动启用中文分词）——简单场景够用。
2. **外部预分词**：写入前用 jieba/IK 分词，把分词结果存 TAG 字段（`tags`），查询时对用户输入同样预分词——可控性最强，生产推荐。
3. **混合检索兜底**：中文专有名词/型号检索靠向量语义召回（05 篇 KNN），全文只做精确词匹配——中文 RAG 的常见组合。

工程判断：**中文全文检索质量上限低于 ES**（无完整中文词库生态），中文 RAG 的召回主力是向量而非全文——全文检索在 Redis 8 里是"辅助过滤"定位，不要把它当 ES 用。

全文检索的典型场景模板（RAG 应用）——**"按标题/标签/分类过滤后再语义检索"**：全文/枚举过滤是向量检索的前置收窄器（06 篇 pre-filter + KNN），而不是独立搜索引擎。三个高频组合：**分类 + 时间 + 向量**（`@category:{jvm} @year:[2024 +inf]=>[KNN ...]`）；**关键词 + 语义双路**（FT.HYBRID 词法腿）；**精确词兜底**（用户输入专有名词时先全文精确命中，语义检索结果并列展示——中文场景尤其实用，因为向量对型号/人名不敏感）。

## 5. 查询性能与索引优化

全文检索的性能工程三件事：

1. **索引字段克制**：只声明需要检索的字段——TEXT 字段建倒排索引有内存/写入代价，不需要全文的字段用 TAG/NUMERIC 替代（精确匹配更快更省）。
2. **查询走索引**：`@title:关键词` 走倒排、`@category:{jvm}` 走 TAG 索引——**过滤条件字段必须建索引**，未声明字段的查询退化为全索引扫描（`FT.EXPLAIN` 可验证执行路径）。
3. **`LIMIT` 控制返回**：默认返回 10 条（`LIMIT 0 10`），大结果集按需分页——**不要 `LIMIT 0 100000` 拉全量**（聚合场景用 `FT.AGGREGATE`，导出用 `FT.SEARCH` + 游标）。

`FT.EXPLAIN idx:docs "@title:向量"` 输出查询执行树（分词 → 倒排取交集 → 打分排序）——排查"为什么慢/为什么结果不对"的第一工具；`FT.INFO` 看索引统计（文档数、索引大小、`indexing` 同步状态）。

## 6. 与 ES 的定位差异

| 维度 | Redis 8 RediSearch | Elasticsearch |
|------|-------------------|---------------|
| 形态 | 内置模块，同实例 | 独立分布式集群 |
| 规模 | 内存约束（GB-TB 级） | 磁盘 + 分布式（PB 级） |
| 中文生态 | 基础分词 | IK/分词器生态完整 |
| 运维 | 零新增（已有 Redis） | 独立集群运维 |
| 场景 | 应用内搜索、过滤辅助 | 独立搜索引擎 |

判断：**应用数据 + 搜索 + 缓存同库**是 Redis 8 的红利；**搜索是核心产品且规模大**选 ES（或 09 篇的独立向量库）。Redis 8 的搜索定位是"够用 + 顺带"，不是"最强"。

选型检查单（写进 ADR）：**数据量**（内存内搜索 vs 磁盘搜索——百万文档内 Redis 够用，千万级评估 ES）；**中文需求**（强中文分词 → ES/独立方案，Redis 靠预分词将就）；**运维约束**（已有 Redis 团队 → Redis 8 零新增，无 Redis 团队 → ES 未必更重）；**搜索与业务耦合**（搜索是业务数据的一部分 → Redis 8 同库优势大，搜索是独立产品 → ES 独立部署合理）。四条过完，"Redis 8 的搜索够不够用"就有答案了。

与三套向量库体系的全文能力对照（选型时的横向参考）：Chroma 的 where_document（FTS5 trigram，中文 1-2 字符失效）、PgVector 的 FTS（zhparser 方案）、Qdrant 的 payload text 索引（轻量）——**Redis 8 的 RediSearch 是四者中全文能力最强的**（BM25、同义词、聚合、成熟查询语法），但都弱于 ES；向量 + 全文的混合查询能力，Redis 8 与 Qdrant 并列第一梯队（pre-filter 进查询路径），优于 Chroma/PgVector 的 post-filter——这是"已有 Redis"场景的额外红利。

> 🎯 **核心要点**：四字段类型（TEXT/TAG/NUMERIC/GEO）决定查询能力；BM25 是 8 的默认打分；FT.ALTER 在线加字段、FT.SYNUPDATE 管同义词；中文靠预分词或向量兜底；定位是"应用内搜索"而非 ES 替代。

---

**参考来源**：

- [Redis 8.0 新特性文档（搜索部分）](https://redis.io/docs/latest/develop/whats-new/8-0/)
- [Redis 向量文档（中文）](https://redis.ac.cn/docs/latest/develop/interact/search-and-query/advanced-concepts/vectors/)
- [Redis Stack 模块深度解读（CalmOps）](https://calmops.com/database/redis/redis-stack-modules-overview/)

---

**下一模块**：[05-向量索引与检索原理](./05-向量索引与检索原理.md) / **返回总览**：[00-Redis Stack知识体系总览](./00-Redis%20Stack知识体系总览.md)
