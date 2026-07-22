# Weaviate 向量数据库 面试宝典
> 基于课程大纲全面覆盖面试高频考点 — Weaviate 架构、Spring AI 集成与向量检索全解析

## 目录
1. [一、基础概念速答（18题）](#一基础概念速答18题)
2. [二、深度原理剖析（12题）](#二深度原理剖析12题)
3. [三、实战场景题（10题）](#三实战场景题10题)
4. [四、手写代码题（8题）](#四手写代码题8题)
5. [五、系统设计题（5题）](#五系统设计题5题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板（Top 5）](#七面试回答模板top-5)
8. [八、快速查漏补缺Checklist](#八快速查漏补缺checklist)

---

## 一、基础概念速答（18题）

### Q1 什么是向量数据库（Vector Database）？与传统数据库有何区别？
> 向量数据库是专门存储和检索**高维向量数据**的数据库，核心能力是**近似最近邻搜索（ANN）**。

| 对比维度 | 向量数据库 | 传统关系型数据库 |
|----------|-----------|-----------------|
| 数据类型 | 高维浮点向量（如 1536 维） | 结构化数据（行/列） |
| 索引结构 | HNSW、IVF、Flat | B+Tree、Hash |
| 查询方式 | 向量相似度（余弦/欧氏/内积） | 精确匹配、范围查询 |
| 核心场景 | 语义搜索、RAG、推荐系统 | 事务处理、报表 |
| 检索结果 | 按相似度排序的 Top-K | 精确匹配集合 |

> 💡 向量数据库是 RAG（Retrieval-Augmented Generation）架构的核心组件，负责从海量文档中找出与用户 query 语义最相似的片段。

### Q2 什么是向量嵌入（Vector Embedding）？
> Embedding 是将文本、图片、音视频等非结构化数据通过深度学习模型映射为固定维度的浮点数向量。

```text
"Java 是一种编程语言" → [0.012, -0.045, 0.098, ..., 0.076]  // 1536 维
```

| 特性 | 说明 |
|------|------|
| **语义保留** | 语义相似的文本在向量空间中距离更近 |
| **维度固定** | 常用 256、384、768、1024、1536、3072 维 |
| **生成方式** | 通过 Embedding 模型（如 text-embedding-3、bge、m3e） |
| **存储要求** | 每个 float 占 4 字节，1536 维向量约 6KB |

### Q3 Weaviate 是什么？它有哪些核心特性？
> **Weaviate** 是一个开源的、云原生的向量搜索引擎，支持混合搜索与生成式检索。

| 核心特性 | 说明 |
|----------|------|
| **架构** | Go 语言编写，云原生，支持 Kubernetes 部署 |
| **向量引擎** | 内置 HNSW、Flat 索引，支持多种距离度量 |
| **混合搜索** | BM25 关键词 + 向量相似度的融合检索 |
| **生成式搜索** | 检索结果直接送入 LLM 生成答案（RAG 原生支持） |
| **GraphQL API** | 原生 GraphQL 查询接口，也支持 RESTful API |
| **多租户** | 原生 Multi-Tenancy 支持，数据隔离 |
| **AI Native** | 从设计之初就为 AI 工作负载优化 |

### Q4 Weaviate 中的核心术语有哪些？对应关系型数据库什么概念？

| Weaviate 术语 | 类比 RDBMS | 说明 |
|---------------|-----------|------|
| **Class** | Table（表） | 类似数据库表，定义对象类型 |
| **Object** | Row（行） | 一条数据记录 |
| **Property** | Column（列） | 对象的字段，支持向量化属性 |
| **Vector** | 隐式索引 | 自动或手动生成的向量嵌入 |
| **Schema** | DDL（表结构定义） | 管理 Class 和 Property 的元信息 |
| **Tenant** | Schema/Database | 多租户隔离单位 |

> 💡 Weaviate 中的 Class 类似于 RDBMS 的表，但多了 `vectorizer`、`vectorIndexType` 等 AI 相关配置。

### Q5 什么是 HNSW（Hierarchical Navigable Small World）索引？
> HNSW 是目前最主流的 ANN（Approximate Nearest Neighbor）算法，基于**分层可导航小世界图**构建。

| 特性 | 说明 |
|------|------|
| **原理** | 构建多层图结构，上层是"高速公路"，下层是"本地道路" |
| **搜索过程** | 从顶层随机入口开始，逐层下降找到近似最近邻 |
| **关键参数** | `efConstruction`（构建质量）、`ef`（搜索范围）、`M`（连接数） |
| **时间复杂度** | O(log n) 级别 |
| **优缺点** | 查询极快但内存占用较高 |

> 🎯 HNSW 是 Weaviate 默认的向量索引类型，适合**高精度、低延迟**的场景。

### Q6 什么是 Flat（Brute Force）索引？
> Flat 索引是**暴力穷举**方式，计算查询向量与所有向量的距离，不建树/图结构。

| 特性 | 说明 |
|------|------|
| **精度** | 100%（精确最近邻，非近似） |
| **速度** | O(n)，数据量大时极慢 |
| **适用场景** | 小数据集（万级以下）、需要精确结果的场景 |
| **内存占用** | 无需额外索引结构 |

### Q7 Weaviate 支持哪些距离度量（Distance Metrics）？

| 距离类型 | API 名称 | 公式简述 | 适用场景 |
|----------|---------|---------|---------|
| **余弦距离** | `cosine` | 1 - cos(θ) | 文本语义相似度（最常用） |
| **欧氏距离** | `l2-squared` | Σ(xi - yi)² | 图像特征、聚类场景 |
| **内积距离** | `dot` | 1 - dot(x, y) | 归一化后等同余弦，推荐场景 |
| **曼哈顿距离** | `manhattan` | Σ\|xi - yi\| | 特定特征空间 |

> ⚠️ Weaviate 中**距离越小表示越相似**。余弦距离 = 1 - 余弦相似度，范围 [0, 2]。

### Q8 Weaviate 中的向量化方式有哪些？
> Weaviate 支持多种向量化策略，无需外部模型调用。

| 向量化方式 | 配置方式 | 说明 |
|-----------|---------|------|
| **内置 Vectorizer** | `"vectorizer": "text2vec-openai"` | 集成 OpenAI/Cohere/HuggingFace 等模型 |
| **模块化 Vectorizer** | `"vectorizer": "text2vec-transformers"` | 本地部署 Transformer 模型 |
| **外部向量** | 不指定 vectorizer | 用户自行调用 Embedding API，传入已有向量 |

### Q9 什么是 Weaviate 的模块（Module）系统？
> Module 是 Weaviate 的插件化扩展机制，提供向量化、生成式、重排序等能力。

| 模块类型 | 示例 | 功能 |
|----------|------|------|
| **向量化模块** | `text2vec-openai`, `text2vec-cohere` | 将文本自动转为向量 |
| **生成式模块** | `generative-openai`, `generative-cohere` | 检索后调用 LLM 生成答案 |
| **重排序模块** | `reranker-cohere` | 对检索结果二次排序 |
| **自定义模块** | 用户自行开发 | 扩展特定能力 |

### Q10 Weaviate 与 Milvus 的核心区别是什么？

| 对比维度 | Weaviate | Milvus |
|----------|---------|--------|
| **开发语言** | Go | Go + C++（核心引擎 Knowhere） |
| **API 风格** | GraphQL + RESTful | gRPC + RESTful |
| **云原生** | 原生支持 K8s | 支持 K8s，架构更复杂 |
| **模块系统** | 内置 Vectorizer + Generative | 需外部集成 Embedding 服务 |
| **混合搜索** | 原生 BM25 + 向量混合 | 需手动组合 |
| **多租户** | Schema 级原生支持 | 需通过 Collection 隔离 |
| **适用场景** | AI/LLM 应用快速开发 | 大规模生产级检索系统 |
| **社区活跃度** | 生态较新，快速增长 | 更成熟的社区 |

### Q11 Weaviate 与 Pinecone 的核心区别？

| 对比维度 | Weaviate（开源） | Pinecone（商业 SaaS） |
|----------|-----------------|----------------------|
| **开源** | Apache 2.0 开源 | 闭源 SaaS |
| **自托管** | 支持 | 不支持 |
| **混合搜索** | 原生支持 | 需用稀疏-密集向量组合 |
| **Generative Search** | 内置模块支持 | 需自行编排 |
| **成本** | 自托管低成本 | 按量付费，较高 |
| **运维** | 需自行运维 | 全托管免运维 |

### Q12 Weaviate 与 Qdrant 的核心区别？

| 对比维度 | Weaviate | Qdrant |
|----------|---------|--------|
| **API** | GraphQL + REST | gRPC + REST |
| **向量化** | 内置 Vectorizer 模块 | 需外部提供向量 |
| **过滤** | GraphQL where 子句 | Filter 机制 + payload 索引 |
| **分组搜索** | 需自行实现 | 原生 Scroll/GroupBy |
| **写入性能** | 常规 | 更高批量写入吞吐 |

### Q13 什么是混合搜索（Hybrid Search）？
> 混合搜索 = **BM25 全文检索** + **向量语义检索** 的加权融合。

```text
Hybrid(query) = α × BM25(query) + (1-α) × VectorSimilarity(query)
```

| 搜索方式 | 优点 | 缺点 |
|----------|------|------|
| **BM25 全文搜索** | 关键词精确匹配高 | 无法处理同义词、语义近似 |
| **向量搜索** | 语义相似度高 | 关键词精确匹配弱 |
| **混合搜索** | 两者兼顾 | 权重 α 需调优 |

> 💡 Weaviate 的混合搜索通过 `hybrid` 查询操作符实现，自动融合 BM25 分数和向量距离分数。

### Q14 什么是生成式搜索（Generative Search）？
> 生成式搜索 = **检索（Retrieval）** + **生成（Generation）**，即 RAG 的数据库原生实现。

```text
用户 Query → 向量检索 Top-K → LLM 生成答案
```

> Weaviate 通过 `generative-openai` 或 `generative-ollama` 模块内置此能力，一条 GraphQL 查询即可完成 RAG。

### Q15 Weaviate 的多租户（Multi-Tenancy）是如何工作的？
> 多租户允许同一个 Schema（Class）在逻辑上隔离不同客户的数据。

| 概念 | 说明 |
|------|------|
| **租户（Tenant）** | 数据隔离的最小单位，通过 `tenant` 参数指定 |
| **隔离级别** | 对象级隔离，不同租户操作互不可见 |
| **使用方式** | 创建 Class 时启用 `multiTenancyConfig`，操作时传入 `tenant` |
| **优势** | 一套 Schema 服务所有客户，显著降低运维成本 |

```graphql
# 启用多租户的 Class
{
  "class": "Document",
  "multiTenancyConfig": { "enabled": true }
}
```

### Q16 Weaviate 的 GraphQL API 有哪些核心查询类型？

| 查询类型 | 说明 | 示例场景 |
|----------|------|---------|
| `Get{}` | 按条件检索对象 | 过滤 + 向量搜索 |
| `Aggregate{}` | 聚合统计 | 计数、分组统计 |
| `Explore{}` | 概念探索 | 找到与给定概念最相关对象 |
| `Meta{}` | Schema 元信息 | 查看 Class 属性 |

### Q17 什么是 Weaviate 的 Referencing（交叉引用）？
> Weaviate 支持在 Object 之间建立**交叉引用（Cross-References）**，类似 RDBMS 的外键。

```graphql
# 定义引用属性
{
  "class": "Article",
  "properties": [{
    "name": "author",
    "dataType": ["Author"]  # 引用 Author Class
  }]
}
```

> 💡 交叉引用支持**反向查询（Beacon/HasNext）**，适合知识图谱类应用。

### Q18 Weaviate 的 Backup/Restore 机制是怎样的？
> Weaviate 支持**创建备份（Create Backup）**和**恢复（Restore）**，可以导出到 S3、GCS 或本地文件系统。

```bash
# 创建备份
POST /v1/backups/{backend}
{"id": "my-backup", "include": ["DocumentClass"]}
```

---

## 二、深度原理剖析（12题）

### Q1 向量数据库中 ANN（Approximate Nearest Neighbor）搜索的几种主流算法对比？

| 算法 | 原理 | 查询速度 | 内存占用 | 建索引时间 | Weaviate 支持 |
|------|------|---------|---------|-----------|:---:|
| **HNSW** | 分层可导航小世界图 | ⭐⭐⭐⭐⭐ | ⭐⭐（高） | ⭐⭐（慢） | ✅ 默认 |
| **Flat** | 暴力全量计算 | ⭐（极慢） | ⭐⭐⭐⭐⭐（无额外） | ⭐⭐⭐⭐⭐（无） | ✅ |
| **IVF** | 倒排文件 + 聚类 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ❌（不在核心引擎中） |
| **IVF_PQ** | IVF + 乘积量化压缩 | ⭐⭐⭐ | ⭐⭐⭐⭐⭐（省内存） | ⭐⭐⭐ | ❌ |
| **DiskANN** | 基于 SSD 的图索引 | ⭐⭐⭐ | ⭐⭐⭐⭐⭐（SSD） | ⭐⭐⭐ | ❌ |

> 🎯 HNSW 是当前**召回率和延迟的最佳平衡点**，也是 Weaviate 的默认和首选索引类型。

### Q2 HNSW 的关键参数如何调优？

| 参数 | 默认值 | 作用 | 调优建议 |
|------|-------|------|---------|
| `efConstruction` | 128 | 构建时的动态列表大小，越大索引质量越高 | 200-500（高精度），64-100（快速构建） |
| `ef` | -1（自动） | 搜索时的动态列表大小，越大召回越高 | 100-300（高召回），40-80（低延迟） |
| `maxConnections`(M) | 64 | 每个节点最大邻居数 | 16-32（省内存），32-64（高精度） |
| `dynamicEf` | true | 是否根据查询动态调整 ef | 建议开启 |
| `vectorCacheMaxObjects` | 1e12 | 向量缓存的对象数上限 | 根据可用内存调整 |

```yaml
# Weaviate 中 HNSW 配置示例
vectorIndexConfig:
  skip: false
  efConstruction: 128
  ef: -1
  maxConnections: 64
  dynamicEf: true
  vectorCacheMaxObjects: 10000000
  distance: cosine
```

### Q3 Weaviate 的 Class 与 Property 设计有哪些最佳实践？
> 合理的 Schema 设计是 Weaviate 高性能使用的基础。

| 设计原则 | 说明 |
|----------|------|
| **Class 命名** | 首字母大写（PascalCase），单数形式，如 `Article`、`Product` |
| **Property 命名** | 小驼峰，如 `content`、`authorName` |
| **向量化属性** | 使用 `moduleConfig` 指定哪个属性用于生成向量 |
| **索引属性** | 高频过滤字段设置 `indexFilterable: true` |
| **Tokenization** | 全文搜索字段设置 `indexSearchable: true` 并选 `word` 分词 |
| **避免过度 Embedding** | 不需要语义搜索的字段不要浪费向量存储 |

```json
{
  "class": "Article",
  "description": "知识库文章",
  "vectorizer": "text2vec-openai",
  "moduleConfig": {
    "text2vec-openai": {
      "model": "text-embedding-3-small",
      "vectorizeClassName": false,
      "properties": ["title", "content"]
    }
  },
  "properties": [
    { "name": "title", "dataType": ["text"], "description": "文章标题" },
    { "name": "content", "dataType": ["text"], "description": "文章内容" },
    { "name": "category", "dataType": ["text"],
      "moduleConfig": { "text2vec-openai": { "skip": true } } },
    { "name": "authorId", "dataType": ["int"],
      "indexFilterable": true }
  ]
}
```

### Q4 Weaviate 的写入流程是怎样的？
> 数据写入经过**向量化 → 索引构建 → 持久化**三个阶段。

```text
写入流程：
用户 Object
  → (1) 若配置了 vectorizer，自动调用 Embedding 模型生成向量
  → (2) 将向量插入 HNSW/Flat 索引（异步构建）
  → (3) 原始数据持久化到 WAL + Object Store
  → (4) 返回确认
```

> ⚠️ 如果使用外部向量（自行调用 Embedding API），Weaviate 会跳过步骤 (1)，直接存储传入的 vector。

### Q5 Weaviate 的搜索流程如何工作？

```text
搜索流程：
用户 Query
  → (1) 查询向量生成（若配置 Vectorizer 则自动转化）
  → (2) ANN 索引查找近似最近邻（如 HNSW 图遍历）
  → (3) BM25 全文检索（若为混合搜索）
  → (4) 分数融合（权重 α 调整）
  → (5) 可选：Generative 模块调用 LLM 生成答案
  → (6) 返回 Top-K 结果
```

### Q6 什么是 BM25 算法？在 Weaviate 中如何工作？
> BM25（Best Matching 25）是信息检索领域的经典排序算法，基于 TF-IDF 改进。

```text
BM25(d, q) = Σ IDF(t) × TF(t, d) × (k1 + 1) / (TF(t, d) + k1 × (1 - b + b × |d|/avgdl) )
```

| 参数 | 含义 | Weaviate 默认值 |
|------|------|----------------|
| k1 | 词频饱和度控制 | 1.2 |
| b | 文档长度归一化 | 0.75 |
| IDF(t) | 逆文档频率 | 自动计算 |

> 💡 Weaviate 的 BM25 通过 `bm25` 查询操作符使用，是对 `text` 类型属性最有效的全文搜索方式。

### Q7 Weaviate 的混合搜索分数融合机制是怎样的？
> Weaviate 使用倒数排名融合（Reciprocal Rank Fusion, RRF）来合并 BM25 和向量的排名。

```text
score = α × normalized_vector_score + (1-α) × normalized_bm25_score
RRF_score(d) = 1/(k + rank_vector(d)) + 1/(k + rank_bm25(d))
```

| 融合方式 | 说明 | 适用场景 |
|----------|------|---------|
| **简单加权** | 线性加权 BM25 和向量分数 | 置信度明确时 |
| **RRF 融合** | 基于排名而非分数融合，更鲁棒 | Weaviate 默认推荐方式 |
| **α 参数** | `alpha` 控制向量搜索权重（0-1），默认 0.75 | `alpha=1` 纯向量，`alpha=0` 纯 BM25 |

### Q8 Weaviate 的持久化机制是怎样的？
> Weaviate 使用 **LSM-Tree（Log-Structured Merge-Tree）** 风格的存储引擎。

| 存储组件 | 作用 |
|----------|------|
| **WAL（Write-Ahead Log）** | 先写日志，确保写入不丢失 |
| **Shard-level Object Store** | 按分片存储对象的 JSON 数据 |
| **Vector Index** | HNSW 或 Flat 向量索引，存于内存 + mmap |
| **Inverted Index** | 倒排索引，支持 BM25 全文检索 |
| **Property Index** | 按 property 构建的索引，支持 filter |

### Q9 什么是 Weaviate 的分片（Sharding）机制？
> Weaviate 支持自动水平分片，将数据分布到多个节点。

| 分片概念 | 说明 |
|----------|------|
| **分片键** | 自动基于对象 ID 哈希 |
| **分片数量** | 创建 Class 时指定 `shardingConfig.desiredCount` |
| **分片分布** | 自动在集群节点间均衡 |
| **扩容** | 增加节点后自动重新平衡 |

```json
{
  "class": "Article",
  "shardingConfig": {
    "desiredCount": 6,
    "desiredVirtualCount": 128
  }
}
```

### Q10 Weaviate 的 Replication（副本）机制如何工作？
> 副本提供高可用性和读取扩展能力，Weaviate 使用**最终一致性模型**。

| 复制参数 | 说明 |
|----------|------|
| `factor` | 副本因子，1=无副本，2=1主1备，3=1主2备 |
| 一致性级别 | `ONE`（写一个节点确认），`QUORUM`（多数确认），`ALL`（全部确认） |
| 故障恢复 | 自动检测节点故障，重新同步 |

```json
{
  "class": "Article",
  "replicationConfig": {
    "factor": 2
  }
}
```

### Q11 Weaviate 中的 Tokenization（分词策略）有哪些？

| 分词策略 | 说明 | 适用场景 |
|----------|------|---------|
| `word` | 按空格和标点切词 | 英文文本（默认） |
| `lowercase` | 转为小写，特殊字符保留 | 代码/标识符搜索 |
| `whitespace` | 仅按空格切分 | URL/路径搜索 |
| `field` | 整个属性作为一个 token | 精确匹配 |
| `trigram` | 按三元组切分 | 模糊搜索、拼写容错 |

### Q12 Weaviate 的 Filter 过滤机制有哪些类型？

| 过滤操作符 | 说明 | 示例 |
|-----------|------|------|
| `Equal` | 精确等于 | `{ path: ["status"], operator: Equal, valueString: "active" }` |
| `NotEqual` | 不等于 | `{ operator: NotEqual, valueString: "deleted" }` |
| `GreaterThan` / `GreaterThanEqual` | 大于 / 大于等于 | `{ operator: GreaterThan, valueNumber: 100 }` |
| `LessThan` / `LessThanEqual` | 小于 / 小于等于 | `{ operator: LessThan, valueNumber: 50 }` |
| `Like` | 通配符匹配（`*` 和 `?`） | `{ operator: Like, valueString: "Java*" }` |
| `WithinGeoRange` | 地理围栏 | `{ operator: WithinGeoRange, valueGeoRange: {...} }` |
| `And` / `Or` | 复合逻辑 | `{ operator: And, operands: [...] }` |

---

## 三、实战场景题（10题）

### Q1 如何设计一个企业知识库 RAG 系统的 Weaviate Schema？
> 企业知识库通常需要存储文档碎片、元数据，并支持混合搜索。

```json
{
  "class": "Chunk",
  "description": "文档碎片单元",
  "vectorizer": "text2vec-openai",
  "moduleConfig": {
    "text2vec-openai": {
      "model": "text-embedding-3-small",
      "properties": ["content", "title"]
    }
  },
  "properties": [
    { "name": "content", "dataType": ["text"], "description": "碎片文本" },
    { "name": "title", "dataType": ["text"], "description": "所属文档标题" },
    { "name": "docId", "dataType": ["int"], "indexFilterable": true },
    { "name": "chunkIndex", "dataType": ["int"], "description": "碎片序号" },
    { "name": "category", "dataType": ["text"], "indexFilterable": true,
      "moduleConfig": { "text2vec-openai": { "skip": true } } },
    { "name": "tags", "dataType": ["text[]"], "indexFilterable": true }
  ],
  "vectorIndexConfig": {
    "distance": "cosine",
    "efConstruction": 256,
    "maxConnections": 64
  }
}
```

### Q2 如何处理千万级数据的性能优化？
> 大规模向量搜索的性能优化需要从索引、分片、硬件多个角度入手。

| 优化策略 | 具体措施 |
|----------|---------|
| **索引调优** | 增大 `efConstruction` 提高索引质量；调整 `ef` 平衡召回和延迟 |
| **分片策略** | 增加分片数 `desiredCount`，充分利用多节点并行 |
| **硬件加速** | 使用 SSD 存储，保证足够内存缓存 HNSW 图 |
| **减少向量维度** | 使用更低维度的 Embedding 模型（384 vs 1536） |
| **批量写入** | 使用 Batch API 而非逐条写入 |
| **Filter 下推** | 先过滤再搜索，减少向量搜索范围 |

```java
// 批量写入示例
List<WeaviateDocument> documents = new ArrayList<>();
for (int i = 0; i < 1000; i++) {
    documents.add(/* ... */);
}
weaviateClient.data().creator()
    .withClassName("Chunk")
    .withObjects(documents) // 批量
    .run();
```

### Q3 在 Spring AI 项目中如何集成 Weaviate？
> Spring AI 提供了 `WeaviateVectorStore` 作为 Spring Boot 的自动配置组件。

```xml
<!-- Maven 依赖 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-weaviate-store</artifactId>
    <version>1.0.0</version>
</dependency>
```

```yaml
# application.yml 配置
spring:
  ai:
    vectorstore:
      weaviate:
        host: http://localhost:8080
        api-key: your-api-key   # 可选
        object-class: Chunk     # Weaviate 中的 Class 名称
        filter-field: category  # 过滤字段
      embedding:
        openai:
          api-key: ${OPENAI_API_KEY}
          model: text-embedding-3-small
```

### Q4 如何使用 Spring AI 的 WeaviateVectorStore 进行相似度查询？

```java
@Service
public class WeaviateSearchService {

    @Autowired
    private VectorStore vectorStore;

    /**
     * 基础相似度查询
     */
    public List<Document> search(String query, int topK) {
        return vectorStore.similaritySearch(
            SearchRequest.query(query)
                .withTopK(topK)
        );
    }

    /**
     * 带过滤条件的相似度查询
     */
    public List<Document> searchWithFilter(String query, String category, int topK) {
        return vectorStore.similaritySearch(
            SearchRequest.query(query)
                .withTopK(topK)
                .withFilterExpression("category == '" + category + "'")
        );
    }

    /**
     * 混合搜索（BM25 + 向量）
     */
    public List<Document> hybridSearch(String query, int topK) {
        return vectorStore.similaritySearch(
            SearchRequest.query(query)
                .withTopK(topK)
                .withSimilarityThreshold(0.5)
        );
    }
}
```

### Q5 如何使用 Spring AI 向 Weaviate 中写入数据？

```java
@Service
public class WeaviateIngestionService {

    @Autowired
    private VectorStore vectorStore;

    /**
     * 单条数据写入
     */
    public void addDocument(String title, String content, String category) {
        Document doc = Document.builder()
            .withId(UUID.randomUUID().toString())
            .withText(content)
            .withMetadata(Map.of(
                "title", title,
                "category", category,
                "timestamp", System.currentTimeMillis()
            ))
            .build();

        vectorStore.add(List.of(doc));
    }

    /**
     * 批量导入文档
     */
    @Transactional
    public void batchImport(List<RawDocument> rawDocs) {
        List<Document> docs = rawDocs.stream()
            .map(r -> Document.builder()
                .withId(r.getId())
                .withText(r.getContent())
                .withMetadata(Map.of(
                    "title", r.getTitle(),
                    "category", r.getCategory()
                ))
                .build()
            )
            .collect(Collectors.toList());

        // Spring AI 自动调用 Embedding 模型并为每条数据生成向量
        vectorStore.add(docs);
    }
}
```

### Q6 如何直接通过 Weaviate 客户端（非 Spring AI）进行 CRUD？

```java
// 使用 Weaviate Java 客户端
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.base.Result;
import io.weaviate.client.v1.data.model.WeaviateObject;

// 创建客户端
WeaviateClient client = new WeaviateClient("http://localhost:8080");

// ===== Create =====
WeaviateObject obj = WeaviateObject.builder()
    .className("Chunk")
    .id(UUID.randomUUID())
    .properties(Map.of(
        "content", "Weaviate 是一个开源的向量数据库",
        "category", "技术"
    ))
    .build();
Result<WeaviateObject> result = client.data().creator()
    .withObject(obj)
    .run();

// ===== Read =====
Result<List<WeaviateObject>> searchResult = client.data().graphQL()
    .withQuery("{ Get { Chunk { content title category } } }")
    .run();

// ===== Update =====
client.data().updater()
    .withClassName("Chunk")
    .withId(objId)
    .withProperties(Map.of("content", "更新后的内容"))
    .run();

// ===== Delete =====
client.data().deleter()
    .withClassName("Chunk")
    .withId(objId)
    .run();
```

### Q7 GraphQL 查询实战：不同查询场景的完整示例？

```graphql
# 1. 基础向量搜索 - 查找与 "Java 向量数据库" 语义最相似的3条记录
{
  Get {
    Chunk(
      nearText: { concepts: ["Java 向量数据库"] }
      limit: 3
    ) {
      content
      title
      category
      _additional { distance }
    }
  }
}

# 2. 带过滤的向量搜索 - 限定技术分类
{
  Get {
    Chunk(
      nearText: { concepts: ["Spring Boot 集成"] }
      where: {
        path: ["category"]
        operator: Equal
        valueString: "技术"
      }
      limit: 5
    ) {
      content
      title
      _additional { distance }
    }
  }
}

# 3. BM25 全文搜索
{
  Get {
    Chunk(
      bm25: { query: "向量数据库 开源" }
      limit: 5
    ) {
      content
      title
      _additional { score }
    }
  }
}

# 4. 混合搜索（BM25 + 向量）
{
  Get {
    Chunk(
      hybrid: {
        query: "向量数据库 开源 实现原理"
        alpha: 0.5
      }
      limit: 5
    ) {
      content
      title
      _additional { 
        score
        distance
      }
    }
  }
}

# 5. 生成式搜索（RAG）- 自动用 LLM 生成答案
{
  Get {
    Chunk(
      nearText: { concepts: ["Weaviate 和 Milvus 区别"] }
      limit: 3
    ) {
      content
      title
      _additional {
        generate(
          singleResult: {
            prompt: """
              基于以下内容回答问题：{content}
              问题：Weaviate 和 Milvus 的核心区别是什么？
            """
          }
        ) {
          singleResult
          error
        }
      }
    }
  }
}
```

### Q8 如何设计与实现多租户隔离的 Weaviate 应用？

```java
@Configuration
public class WeaviateMultiTenantConfig {

    @Bean
    public WeaviateVectorStore multiTenantVectorStore(
            WeaviateClient weaviateClient,
            EmbeddingModel embeddingModel) {

        // 创建多租户 Class
        createMultiTenantClass(weaviateClient);

        return new WeaviateVectorStore(weaviateClient, embeddingModel);
    }

    private void createMultiTenantClass(WeaviateClient client) {
        // 1. 创建启用多租户的 Class
        client.schema().classCreator()
            .withClass(WeaviateClass.builder()
                .className("TenantDocument")
                .multiTenancyConfig(MultiTenancyConfig.builder()
                    .enabled(true)
                    .build())
                .properties(List.of(
                    Property.builder()
                        .name("content")
                        .dataType(List.of("text"))
                        .build()
                ))
                .build())
            .run();

        // 2. 为每个租户注册 Tenant
        for (String tenant : List.of("tenant-a", "tenant-b")) {
            client.schema().tenantsCreator()
                .withClassName("TenantDocument")
                .withTenants(List.of(Tenant.builder()
                    .name(tenant)
                    .build()))
                .run();
        }
    }

    /**
     * 按租户隔离写入数据
     */
    public void addForTenant(String tenantId, String content) {
        // 在实际写入时指定 tenant
        Document doc = Document.builder()
            .withText(content)
            .withMetadata(Map.of("tenant", tenantId))
            .build();

        // 通过 tenant 参数隔离
        vectorStore.add(List.of(doc));
    }
}
```

### Q9 Spring AI Weaviate 中如何实现生成式搜索（Generative Search）？

```java
@Service
public class GenerativeSearchService {

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private ChatClient chatClient;

    /**
     * 两步式 RAG：检索 + 生成（更灵活）
     */
    public String ragAnswer(String question) {
        // Step 1: 检索相关文档
        List<Document> docs = vectorStore.similaritySearch(
            SearchRequest.query(question).withTopK(3)
        );

        // Step 2: 构建 Prompt 并调用 LLM
        String context = docs.stream()
            .map(Document::getText)
            .collect(Collectors.joining("\n---\n"));

        return chatClient.prompt()
            .withSystemMessage("基于以下上下文回答问题：\n" + context)
            .withUserMessage(question)
            .call()
            .getContent();
    }

    /**
     * 通过 Weaviate 的 Generative 模块实现（数据库内置）
     */
    public String weaviateGenerativeSearch(String question) {
        // 利用 Weaviate 的 generative-openai 模块在数据库内部完成
        // GraphQL: Get{ Chunk(nearText:{concepts:["?"]}){
        //   _additional{ generate(singleResult:{prompt:"..."}) { singleResult } }
        // }}
        // 需配置 generative-openai 模块
        return "Generated answer from Weaviate's built-in generative module";
    }
}
```

### Q10 如何从现有数据库中同步数据到 Weaviate？

```java
@Service
public class DataSyncService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private VectorStore vectorStore;

    /**
     * 全量同步 MySQL 数据到 Weaviate
     */
    @Scheduled(cron = "0 0 3 * * ?")  // 每天凌晨3点
    public void fullSync() {
        // 1. 从 MySQL 读取数据
        List<Map<String, Object>> rows = jdbcTemplate
            .queryForList("SELECT id, title, content, category FROM articles WHERE status = 1");

        // 2. 转换为 Document 并写入 Weaviate
        List<Document> docs = rows.stream()
            .map(row -> Document.builder()
                .withId(row.get("id").toString())
                .withText((String) row.get("content"))
                .withMetadata(Map.of(
                    "title", row.get("title"),
                    "category", row.get("category")
                ))
                .build())
            .collect(Collectors.toList());

        // 3. 批量写入（自动生成向量）
        vectorStore.add(docs);
    }
}
```

---

## 四、手写代码题（8题）

### Q1 Spring Boot 配置类：完整 Weaviate + OpenAI 配置

```yaml
# application.yml
spring:
  ai:
    vectorstore:
      weaviate:
        host: http://localhost:8080
        object-class: KnowledgeChunk
    embedding:
      openai:
        api-key: ${OPENAI_API_KEY:sk-your-key}
        model: text-embedding-3-small
```

```java
@Configuration
@EnableAutoConfiguration
public class WeaviateConfig {

    @Bean
    public WeaviateVectorStore weaviateVectorStore(
            WeaviateClient weaviateClient,
            EmbeddingModel embeddingModel) {

        return new WeaviateVectorStore(
            weaviateClient,
            embeddingModel,
            true  // autoCreateSchema
        );
    }

    @Bean
    public WeaviateClient weaviateClient(
            @Value("${spring.ai.vectorstore.weaviate.host}") String host) {

        Config config = new Config("http", host.replace("http://", "").replace("https://", ""));
        return new WeaviateClient(config);
    }
}
```

### Q2 纯向量相似度搜索实现

```java
/**
 * 使用 Spring AI WeaviateVectorStore 进行纯向量相似度搜索
 */
public List<Document> vectorSearch(String query, int topK, double threshold) {
    return vectorStore.similaritySearch(
        SearchRequest.query(query)
            .withTopK(topK)
            .withSimilarityThreshold(threshold)
    );
}

/**
 * 手动实现余弦相似度计算
 */
public double cosineSimilarity(float[] vecA, float[] vecB) {
    double dotProduct = 0.0;
    double normA = 0.0;
    double normB = 0.0;

    for (int i = 0; i < vecA.length; i++) {
        dotProduct += vecA[i] * vecB[i];
        normA += Math.pow(vecA[i], 2);
        normB += Math.pow(vecB[i], 2);
    }

    return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
}
```

### Q3 Schema 创建与删除

```java
/**
 * 创建 Weaviate Schema（Class）
 */
public void createSchema(WeaviateClient client) {
    client.schema().classCreator()
        .withClass(WeaviateClass.builder()
            .className("Product")
            .description("商品信息")
            .vectorizer("text2vec-openai")
            .moduleConfig(Map.of(
                "text2vec-openai", Map.of(
                    "model", "text-embedding-3-small",
                    "vectorizeClassName", false
                )
            ))
            .vectorIndexType("hnsw")
            .vectorIndexConfig(Map.of(
                "distance", "cosine",
                "efConstruction", 256,
                "maxConnections", 64
            ))
            .properties(List.of(
                Property.builder()
                    .name("name").dataType(List.of("text"))
                    .description("商品名称").build(),
                Property.builder()
                    .name("description").dataType(List.of("text"))
                    .description("商品描述").build(),
                Property.builder()
                    .name("price").dataType(List.of("number"))
                    .description("价格").build(),
                Property.builder()
                    .name("category").dataType(List.of("text"))
                    .moduleConfig(Map.of(
                        "text2vec-openai", Map.of("skip", true)
                    ))
                    .build()
            ))
            .build())
        .run();
}

/**
 * 删除 Weaviate Schema（Class及其所有数据）
 */
public void deleteSchema(WeaviateClient client, String className) {
    client.schema().classDeleter()
        .withClassName(className)
        .run();  // 谨慎操作：会删除 Class 下的所有数据
}
```

### Q4 Spring AI Weaviate 中的数据修改与删除

```java
/**
 * 更新文档（元数据更新）
 */
public void updateDocument(String docId, String newContent, Map<String, Object> newMetadata) {
    Document updatedDoc = Document.builder()
        .withId(docId)
        .withText(newContent)
        .withMetadata(newMetadata)
        .build();

    // Spring AI VectorStore 原生 update 方法
    vectorStore.doUpdate(updatedDoc);
}

/**
 * 批量删除文档
 */
public void deleteDocuments(List<String> docIds) {
    // Spring AI 方式
    vectorStore.delete(docIds);
}

/**
 * 基于过滤条件删除
 */
public void deleteByCategory(String category) {
    // 手动实现：先查询再删除
    List<Document> docs = vectorStore.similaritySearch(
        SearchRequest.query("*")
            .withFilterExpression("category == '" + category + "'")
            .withTopK(1000)
    );

    List<String> ids = docs.stream()
        .map(Document::getId)
        .collect(Collectors.toList());

    vectorStore.delete(ids);
}
```

### Q5 匹配查询（BM25）实现

```java
/**
 * 使用 BM25 全文检索
 * Weaviate 原生 GraphQL 方式
 */
public List<Map> bm25Search(String query, String className, int limit) {
    String gql = String.format("""
        {
          Get {
            %s(
              bm25: { query: "%s" }
              limit: %d
            ) {
              content
              title
              _additional { score }
            }
          }
        }
        """, className, query, limit);

    Result<GraphQLResponse> result = client.graphQL().raw()
        .withQuery(gql)
        .run();

    return extractData(result);
}

/**
 * 带 BM25 参数的搜索
 */
public List<Map> bm25SearchWithParams(String query, String className, 
                                       List<String> properties, int limit) {
    String props = properties.stream()
        .map(p -> "\"" + p + "\"")
        .collect(Collectors.joining(", "));

    String gql = String.format("""
        {
          Get {
            %s(
              bm25: {
                query: "%s"
                properties: [%s]
              }
              limit: %d
            ) {
              content
              title
              _additional { score }
            }
          }
        }
        """, className, query, props, limit);

    Result<GraphQLResponse> result = client.graphQL().raw()
        .withQuery(gql)
        .run();

    return extractData(result);
}
```

### Q6 混合搜索（Hybrid Search）代码实现

```java
/**
 * Spring AI 方式实现混合搜索
 * 通过调整 alpha 参数控制向量和 BM25 权重
 */
public List<Document> hybridSearch(String query, double alpha, int topK, String filter) {
    SearchRequest request = SearchRequest.query(query)
        .withTopK(topK);

    // alpha=0.75 表示 75% 向量 + 25% BM25
    // Spring AI Weaviate 通过 extension 传递混合搜索参数
    request.getExtensions().put("alpha", alpha);

    if (filter != null && !filter.isEmpty()) {
        request.withFilterExpression(filter);
    }

    return vectorStore.similaritySearch(request);
}

/**
 * 直接通过 Weaviate GraphQL 实现混合搜索
 */
public String buildHybridSearchGql(String query, double alpha, int limit) {
    return String.format("""
        {
          Get {
            Chunk(
              hybrid: {
                query: "%s"
                alpha: %f
                vector: %s   // 可选：直接传入向量而非文本
              }
              limit: %d
            ) {
              content
              title
              _additional {
                score
                distance
              }
            }
          }
        }
        """, query, alpha, null, limit);
}
```

### Q7 自定义 Embedding 调用实现

```java
/**
 * 外部调用 Embedding API，手动传入向量
 * 适用于已使用其他 Embedding 模型的场景
 */
@Service
public class CustomEmbeddingService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${custom.embedding.api.url}")
    private String embeddingApiUrl;

    /**
     * 调用外部 Embedding 服务（如 bge-large-zh）
     */
    public float[] generateEmbedding(String text) {
        Map<String, Object> request = Map.of("input", text);
        ResponseEntity<EmbeddingResponse> response = restTemplate.postForEntity(
            embeddingApiUrl, request, EmbeddingResponse.class
        );
        return response.getBody().getEmbedding();
    }

    /**
     * 使用外部向量直接写入 Weaviate
     */
    public void addWithCustomVector(String text) {
        float[] vector = generateEmbedding(text);

        // 通过 Weaviate Java 客户端直接写入带向量的对象
        WeaviateObject obj = WeaviateObject.builder()
            .className("Chunk")
            .id(UUID.randomUUID())
            .vector(vector)  // 直接传入向量，Weaviate 不再自动生成
            .properties(Map.of("content", text))
            .build();

        client.data().creator().withObject(obj).run();
    }
}
```

### Q8 Weaviate 异常处理与重试机制

```java
/**
 * 带重试机制的 Weaviate 操作
 */
@Component
public class WeaviateRetryTemplate {

    @Retryable(
        retryFor = {WeaviateConnectionException.class, TimeoutException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<Document> searchWithRetry(String query, int topK) {
        return vectorStore.similaritySearch(
            SearchRequest.query(query).withTopK(topK)
        );
    }

    @Recover
    public List<Document> recover(WeaviateConnectionException e, String query, int topK) {
        // 降级策略：返回空结果或缓存数据
        log.error("Weaviate search failed after retries: {}", e.getMessage());
        return Collections.emptyList();
    }

    /**
     * 健康检查 + 熔断
     */
    @EventListener
    public void handleWeaviateHealth(ApplicationReadyEvent event) {
        try {
            Meta meta = client.misc().metaGetter().run().getResult();
            log.info("Weaviate connected. Version: {}", meta.getVersion());
        } catch (Exception e) {
            log.warn("Weaviate is not available at startup, will retry later");
        }
    }
}
```

---

## 五、系统设计题（5题）

### Q1 设计一个基于 Weaviate 的智能客服系统

**需求**：支持 100 万级 FAQ 知识库，实时回答用户问题，支持多轮对话。

**系统架构**：

```text
用户 → 负载均衡 → API Gateway
                         |
             ┌───────────┼───────────┐
             ↓           ↓           ↓
          Chat Service  Search Service  LLM Service
             │           │           │
             └───────────┼───────────┘
                         ↓
                    Weaviate Cluster
                   (3 nodes, factor=2)
```

| 组件 | 技术选型 | 说明 |
|------|---------|------|
| **搜索服务** | Spring AI + Weaviate | 混合搜索 FAQ 向量库 |
| **LLM 服务** | OpenAI / 本地模型 | 基于检索结果生成答案 |
| **缓存** | Redis | 热点问题缓存，降低 Weaviate QPS |
| **数据同步** | Spring 定时任务 | 定期从 MySQL 同步最新 FAQ |

### Q2 设计一个多模态搜索系统（文本 + 图片）

**需求**：用户可以通过文字搜索图片，也可以通过图片搜索相似图片。

**架构设计**：

```text
文本 Query → 文本 Embedding 模型 → 文本向量
                                       ↓
图片 Query → 图片 Embedding 模型 → 图片向量
                                       ↓
                                   Weaviate
                             Class: "MultiModalItem"
                             - text_vector (from content)
                             - image_vector (from image_url)
                                       ↓
                               混合向量搜索
```

```json
{
  "class": "MultiModalItem",
  "properties": [
    { "name": "content", "dataType": ["text"] },
    { "name": "imageUrl", "dataType": ["text"],
      "moduleConfig": { "text2vec-openai": { "skip": true } } },
    { "name": "imageVector", "dataType": ["number[]"] }
  ]
}
```

### Q3 设计一个高可用 Weaviate 生产集群

**需求**：高可用、可横向扩展、数据不丢失。

| 维度 | 方案 |
|------|------|
| **集群节点** | 3 节点起步，推荐奇数节点 |
| **副本因子** | `factor: 2`（1主1备）或 `factor: 3`（1主2备） |
| **分片数** | `desiredCount: 6`，均匀分布在 3 节点 |
| **备份策略** | 每日全量备份到 S3，开启 WAL |
| **监控** | Prometheus + Grafana，监控 9200 端口指标 |
| **部署** | Kubernetes StatefulSet + PVC |
| **资源** | 64GB RAM + 4CPU（推荐），SSD 持久卷 |

```yaml
# K8s 部署示例
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: weaviate
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: weaviate
        image: semitechnologies/weaviate:1.28
        env:
        - name: CLUSTER_HOSTNAME
          value: $(HOSTNAME)
        - name: CLUSTER_JOIN
          value: weaviate-0.weaviate.default.svc.cluster.local
        - name: PERSISTENCE_DATA_PATH
          value: /var/lib/weaviate
        volumeMounts:
        - mountPath: /var/lib/weaviate
          name: data
  volumeClaimTemplates:
  - metadata:
      name: data
    spec:
      storageClassName: ssd
      accessModes: ["ReadWriteOnce"]
      resources:
        requests:
          storage: 500Gi
```

### Q4 设计一个企业级 RAG 知识库系统

**需求**：百万级企业文档，支持语义搜索、关键词搜索、多租户隔离、权限控制。

**完整架构**：

```text
文档输入层
  ↓ 文档解析（PDF/Word/HTML → 纯文本）
文档处理层
  ↓ 文本分割（Chunking：500 tokens，overlap 50）
  ↓ Embedding 生成（text-embedding-3-small，1536维）
数据存储层
  ↓ Weaviate Multi-Tenant Class "KnowledgeChunk"
  ↓ 属性：content, title, docId, chunkIndex, tenantId, permission
搜索层
  ↓ Hybrid Search（alpha=0.7 向量 + 0.3 BM25）
  ↓ Re-ranking（Cohere Reranker）
生成层
  ↓ LLM 生成答案（gpt-4o / claude-3.5）
```

| 关键设计点 | 实施细节 |
|-----------|---------|
| **Chunk 策略** | LangChain TextSplitter 按 500 tokens 分割，overlap 50 |
| **多租户** | 每个租户独立 Tenant，搜索时自动附加 tenantId 过滤 |
| **权限过滤** | Permission 元数据字段，搜索时按用户角色过滤 |
| **缓存** | Redis 缓存 Top-100 热点问题结果 |
| **监控** | Weaviate Prometheus metrics + 自定义业务日志 |

### Q5 从 MySQL 到 Weaviate 的数据迁移方案设计

**需求**：将现有 MySQL 中的 500 万条数据迁移到 Weaviate，持续增量同步。

```text
迁移流程：
1. 全量迁移（首次）
   MySQL → 分批读取（每批 1000 条）→ Embedding → Weaviate Batch API

2. 增量同步（日常）
   MySQL Binlog → Canal/Kafka → 增量消费者 → Embedding → Weaviate

3. 校验与修复
   定时任务对比 MySQL 和 Weaviate 数据量 → 修复不一致
```

```java
/**
 * 分页迁移方案
 */
@Service
public class MigrationService {
    private static final int BATCH_SIZE = 500;
    private static final int PAGE_SIZE = 100;

    public void migrate() {
        int page = 0;
        long totalMigrated = 0;

        while (true) {
            // 分批读取 MySQL
            List<Map<String, Object>> batch = jdbcTemplate
                .queryForList("SELECT * FROM articles LIMIT ? OFFSET ?",
                    BATCH_SIZE, page * BATCH_SIZE);

            if (batch.isEmpty()) break;

            // 转换并写入 Weaviate
            List<Document> docs = batch.stream()
                .map(this::convertToDocument)
                .collect(Collectors.toList());

            vectorStore.add(docs);
            totalMigrated += docs.size();
            page++;
        }
    }
}
```

---

## 六、常见坑点与最佳实践（表格）

### 配置与集成坑点

| 坑点 | 问题现象 | 解决方案 |
|------|---------|---------|
| **Spring AI 版本不兼容** | WeaviateVectorStore 找不到类 | 确保 `spring-ai-weaviate-store` 版本与 Spring Boot 版本匹配 |
| **Host 配置错误** | 连接拒绝 `Connection Refused` | Weaviate 默认端口 8080，检查 `spring.ai.vectorstore.weaviate.host` |
| **Class 自动创建失败** | Schema 创建时报错 500 | 手动创建 Schema 或设置 `autoCreateSchema=false` |
| **OpenAI API Key 未配置** | Embedding 生成返回 401 | 检查 `spring.ai.embedding.openai.api-key` |
| **CORS 跨域问题** | 前端 GraphQL 查询报 CORS | 启动 Weaviate 时设置 `CORS_ORIGIN=*` |

### 数据操作坑点

| 坑点 | 问题现象 | 解决方案 |
|------|---------|---------|
| **向量维度不匹配** | Weaviate 返回维度错误 | 同一 Class 必须使用固定维度的 Embedding 模型 |
| **Schema 不可变** | 修改 Property 类型失败 | Schema 创建后不可修改，需删除重建 |
| **删除 Class 误操作** | 整个 Class 数据丢失 | 操作前备份 `CREATE BACKUP` |
| **Batch 写入过大** | 请求超时或 OOM | 单次 Batch 不超过 1000 条，建议 200-500 |
| **GraphQL 查询嵌套过深** | 响应慢或超时 | 控制 `limit` 合理值，避免深层嵌套查询 |

### 性能优化最佳实践

| 实践 | 说明 | 建议 |
|------|------|------|
| **选择合适的 Embedding 模型** | 不同模型维度、精度、速度不同 | 中文场景推荐 bge-large-zh 或 m3e |
| **使用 Batch API 写入** | 单条写入极慢 | 批量写入吞吐可提升 10-100x |
| **控制向量维度** | 维度越高精度越高但速度越慢 | 通用场景 768-1024 维，精度优先 1536 维 |
| **优化 HNSW 参数** | 根据召回率和延迟要求调整 | 生产环境 `ef` 建议 200-400 |
| **定期维护** | 删除 tombstone、compact 存储 | 定期执行 Weaviate 的 WAL compaction |
| **监控查询延迟** | 识别慢查询 | 重点关注耗时 >500ms 的查询并优化 |

### Embedding 模型选型对比

| 模型 | 维度 | 适用语言 | 特点 | 推荐场景 |
|------|------|---------|------|---------|
| **text-embedding-3-small** | 1536 | 多语言 | OpenAI 性价比之选 | 通用场景、英文为主 |
| **text-embedding-3-large** | 3072 | 多语言 | 精度最高、成本较高 | 高精度场景 |
| **bge-large-zh-v1.5** | 1024 | 中文为主 | 中文检索 SOTA | 中文 RAG 知识库 |
| **m3e-base** | 768 | 中文为主 | 轻量、中文效果好 | 中文轻量场景 |
| **stella-base-zh-v3** | 768 | 中文 | 检索 + 聚类表现好 | 中文检索场景 |
| **jina-embeddings-v2** | 768 | 多语言 | 长文本支持好（8K） | 长文档检索 |

### 向量数据库综合对比

| 对比维度 | **Weaviate** | **Milvus** | **Pinecone** | **Qdrant** |
|----------|:-----------:|:---------:|:-----------:|:---------:|
| **开源** | ✅ Apache 2.0 | ✅ Apache 2.0 | ❌ 闭源 | ✅ Apache 2.0 |
| **开发语言** | Go | Go + C++ | 闭源 | Rust |
| **API** | GraphQL+REST | gRPC+REST | REST | gRPC+REST |
| **混合搜索** | ✅ 原生 | ❌ 需手动 | ❌ 需手动 | ✅ 原生 |
| **生成式搜索** | ✅ 内置模块 | ❌ | ❌ | ❌ |
| **多租户** | ✅ Schema 级 | ❌ Collection 级 | ❌ Index 级 | ✅ Collection 级 |
| **分布式** | ✅ K8s 原生 | ✅ K8s | ✅ SaaS | ✅ K8s |
| **RAG 友好度** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **社区生态** | 快速增长 | 成熟 | 成熟 | 快速增长 |

---

## 七、面试回答模板（Top 5）

### Template 1：我们为什么选择 Weaviate 而不是 Milvus？
> **回答要点**：
> "我们在项目中选择了 Weaviate 而非 Milvus，主要基于以下几点考虑：
> 1. **RAG 生态整合** — Weaviate 内置了 Vectorizer（自动调用 Embedding 模型）和 Generative Search（检索完直接调用 LLM 生成答案），作为 RAG 方案开箱即用。而 Milvus 只做向量存储和检索，需要额外编排 Embedding 和 LLM 调用。
> 2. **GraphQL API** — Weaviate 原生支持 GraphQL，可以用一条查询完成混合搜索 + 过滤 + 生成的全部操作。Milvus 使用 gRPC，灵活性稍差。
> 3. **多租户支持** — Weaviate 在 Schema 级别支持多租户，一套 Class 可以服务所有租户，按 tenant 隔离数据，非常方便。
> 4. **运维简洁** — Weaviate 架构相对轻量，K8s 部署简单。Milvus 依赖 etcd、minio、pulsar 等组件，运维复杂度更高。"

### Template 2：说说你对 Weaviate 混合搜索的理解
> **回答要点**：
> "Weaviate 的混合搜索（Hybrid Search）是将 BM25 全文检索和向量语义搜索通过 RRF（Reciprocal Rank Fusion）融合。具体来说：
> - **BM25** 负责关键词精确匹配，适合检索包含特定术语的文档。
> - **向量搜索** 负责语义相似度，适合找到语义相近但表述不同的内容。
> - RRF 算法基于排名位置而非分数进行融合，更加鲁棒。
> - 通过 alpha 参数控制权重：`alpha=1` 为纯向量搜索，`alpha=0` 为纯 BM25，默认 `0.75`。
> - 实践中，混合搜索的召回率比纯向量搜索高 5%-15%，尤其在处理专业术语和实体名时优势明显。"

### Template 3：Weaviate 的 Schema 设计需要注意什么？
> **回答要点**：
> "Weaviate Schema 设计有几个关键原则：
> 1. **Class 命名采用 PascalCase**，Properties 采用 camelCase，保持统一规范。
> 2. **明确指定 vectorizer**，配置哪些属性参与向量化，不需要语义搜索的属性用 `skip: true` 跳过，避免浪费向量存储空间。
> 3. **高频过滤字段设置 indexFilterable: true**，全文检索字段设置 indexSearchable: true。
> 4. **注意 Schema 不可变性** — 创建后不能修改 Property 类型，必须删除 Class 重建。因此生产环境建议提前设计好 Schema，上线前做 Data Migration。
> 5. **距离度量选择** — 文本语义搜索推荐 `cosine`，图像特征推荐 `l2-squared`。"

### Template 4：在 Spring AI 中如何集成 Weaviate？
> **回答要点**：
> "Spring AI 对 Weaviate 的集成主要通过 `WeaviateVectorStore` 实现：
> 1. **添加依赖** — 引入 `spring-ai-weaviate-store` Maven 依赖。
> 2. **配置 application.yml** — 配置 `spring.ai.vectorstore.weaviate.host` 指向 Weaviate 实例地址，配置 Embedding 模型（如 OpenAI）。
> 3. **注入 VectorStore** — 在 Service 中直接 `@Autowired` 注入 `VectorStore` 接口。
> 4. **核心操作** — `vectorStore.add(List<Document>)` 写入数据并自动生成 Embedding；`vectorStore.similaritySearch(SearchRequest)` 进行相似度查询；支持 Filter Expression 过滤。
> 5. **自动 Schema 创建** — 可以通过 `autoCreateSchema=true` 让 Spring AI 自动创建 Class。"

### Template 5：谈谈 Weaviate 和传统数据库在 AI 场景下的应用差异
> **回答要点**：
> "在 AI 场景下，Weaviate 与传统关系型数据库的定位完全不同：
> - **MySQL/PostgreSQL** 擅长精确查询和事务处理，但在语义搜索上无能为力。
> - **Elasticsearch** 擅长全文检索（BM25），但无法理解语义相似度。
> - **Weaviate** 以向量为核心，天然支持语义搜索、混合检索，并且内置了与 LLM 的集成能力。
> 在实际项目中，我们采用**MySQL + Weaviate 的混合架构**：MySQL 负责事务性数据存储和精确查询，Weaviate 负责语义理解和智能搜索。业务数据通过定时任务或 Binlog 从 MySQL 同步到 Weaviate，实现各自发挥优势。"

---

## 八、快速查漏补缺Checklist

### 基础概念
- [ ] 能说出向量数据库与传统数据库的区别
- [ ] 理解 Embedding 的概念和生成方式
- [ ] 了解 Weaviate 核心特性（开源、云原生、GraphQL、混合搜索、Generative）
- [ ] 熟记 Weaviate 术语：Class、Object、Property、Schema、Tenant
- [ ] 掌握 HNSW 和 Flat 索引的区别和适用场景
- [ ] 知道三种以上距离度量及使用场景

### 核心原理
- [ ] 理解 ANN 搜索原理和 HNSW 参数调优方法
- [ ] 理解 BM25 算法原理和混合搜索的 RRF 融合机制
- [ ] 熟悉 Weaviate Schema 设计的最佳实践
- [ ] 了解 Weaviate 分片和副本机制
- [ ] 了解 Weaviate Module 系统（Vectorizer、Generative）

### Spring AI 实战
- [ ] 能写出完整的 Spring AI + Weaviate 配置
- [ ] 掌握 Similarity Search 的 Java 代码实现
- [ ] 掌握 CRUD 操作的 Java 代码实现
- [ ] 掌握 BM25 和 Hybrid Search 的代码实现
- [ ] 了解多租户的配置和使用方式
- [ ] 掌握 Batch 写入的批量导入代码

### 数据库对比
- [ ] 能对比 Weaviate vs Milvus vs Pinecone vs Qdrant 的优缺点
- [ ] 能对比主流 Embedding 模型（text-embedding-3、bge、m3e、stella、jina）
- [ ] 知道何时选择 Weaviate 而非其他向量数据库

### 系统设计
- [ ] 能够设计基于 Weaviate 的 RAG 系统
- [ ] 了解高可用 Weaviate 集群部署方案
- [ ] 了解数据迁移（MySQL → Weaviate）方案
- [ ] 了解多模态搜索系统设计思路

### 避坑指南
- [ ] 知道 Schema 创建后不可修改
- [ ] 知道 Batch 写入不宜过大（200-500 条/批）
- [ ] 知道同一 Class 内向量维度必须一致
- [ ] 知道 Embedding 模型选型对检索效果的影响
- [ ] 知道需要配置监控和备份策略

---

> 🎯 **复习建议**：先通读基础概念部分建立整体认知，再重点掌握 Spring AI 的代码实践（面试手写高频），最后理解原理部分用于深度面试问答。系统设计题建议结合实际项目经验进行准备，写出你参与过的 RAG/知识库项目中 Weaviate 的具体使用场景。
