# PgVector 知识体系总览

> PgVector 是 PostgreSQL 的向量相似度搜索扩展——"已有 PG 就用 PG"，让向量检索与业务数据同库同事务，2026 年 0.8.x 的 iterative scans 与 sparsevec 让它从"能用"进化到"好用"

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [为什么必须学透 PgVector](#3-为什么必须学透-pgvector)
4. [核心概念速查](#4-核心概念速查)
5. [与周边知识的关系](#5-与周边知识的关系)
6. [学习路线推荐](#6-学习路线推荐)
7. [快速自测 10 题](#7-快速自测-10-题)

---

## 1. 知识体系导图

```text
PgVector 知识体系
│
├── 01 定位与版本演进
│   ├── 在向量存储谱系中的位置（PG 生态原住民）
│   ├── 版本时间线：0.5 → 0.7 → 0.8.x（2026）
│   ├── 0.8 的四大增量：iterative scans / sparsevec / 并行建索引 / SIMD
│   └── 与专用向量库的定位差异
│
├── 02 安装与扩展启用
│   ├── CREATE EXTENSION 与版本支持矩阵（PG 12-17）
│   ├── 云厂商形态（RDS/Aurora/Supabase/TencentDB/阿里云）
│   ├── Docker 与自建部署
│   └── 升级路径：0.7 → 0.8（REINDEX 铁律）
│
├── 03 向量类型与操作符
│   ├── 四类型：vector / halfvec / sparsevec / bit
│   ├── 维度限制与存储体积对比
│   ├── 六距离操作符：<-> / <=> / <#> / <+> / Hamming / Jaccard
│   └── binary_quantize 二进制量化
│
├── 04 索引原理与参数调优
│   ├── HNSW：无需训练的默认推荐
│   ├── IVFFlat：需预填充数据的备选
│   ├── 参数：m / ef_construction / ef_search
│   └── 基准数据：1M 向量实测召回与延迟
│
├── 05 精确检索与过滤
│   ├── 精确 KNN 与 LIMIT
│   ├── WHERE 过滤的 post-filter 语义
│   ├── 0.8 iterative scans：修复过滤吞结果
│   └── partial index / 分区 / 投影优化
│
├── 06 混合检索与全文检索
│   ├── 向量 + PostgreSQL FTS（tsvector）
│   ├── RRF 融合与加权融合
│   ├── sparsevec 稀疏向量混合
│   └── 中文全文检索的 PG 方案
│
├── 07 高级特性与性能优化
│   ├── 两阶段检索：binary_quantize + 精排
│   ├── 并行建索引与 maintenance_work_mem
│   ├── SIMD 加速与 EXPLAIN 调优
│   ├── VACUUM / REINDEX 运维
│   └── 分表分区与高可用
│
├── 08 生态集成与 RAG 实战
│   ├── Spring AI：spring-ai-pgvector-store-spring-boot-starter
│   ├── LangChain4j 与 JdbcTemplate 直连
│   ├── Spring Data JPA + Flyway 实体层
│   └── 完整 RAG 链路实战
│
├── 09 选型对比与决策
│   ├── vs Chroma / Milvus / Qdrant / ES kNN
│   ├── "已有 PG 用 PgVector"的口诀边界
│   ├── 50M-100M 向量的规模上限
│   └── 选型决策树
│
└── 10 生产实践与面试题
    ├── 坑清单与性能真相
    ├── 生产落地清单
    └── 面试高频题与答题范式
```

---

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 | 文件 |
|:---:|------|---------|---------|------|
| 01 | 定位与版本演进 | 0.8 新特性、PG 生态定位 | 全部必须掌握 | [01-定位与版本演进](./01-定位与版本演进.md) |
| 02 | 安装与扩展启用 | 版本矩阵、云厂商、升级 | 全部必须掌握 | [02-安装与扩展启用](./02-安装与扩展启用.md) |
| 03 | 向量类型与操作符 | 四类型、六操作符 | 全部必须掌握 | [03-向量类型与操作符](./03-向量类型与操作符.md) |
| 04 | 索引原理与参数调优 | HNSW/IVFFlat、调参 | 中高级 | [04-索引原理与参数调优](./04-索引原理与参数调优.md) |
| 05 | 精确检索与过滤 | iterative scans、partial index | 中高级 | [05-精确检索与过滤](./05-精确检索与过滤.md) |
| 06 | 混合检索与全文检索 | FTS + RRF、稀疏向量 | 中高级 | [06-混合检索与全文检索](./06-混合检索与全文检索.md) |
| 07 | 高级特性与性能优化 | 量化、并行、EXPLAIN | 中高级 | [07-高级特性与性能优化](./07-高级特性与性能优化.md) |
| 08 | 生态集成与 RAG 实战 | Spring AI/LangChain4j/JPA | 中高级 | [08-生态集成与RAG实战](./08-生态集成与RAG实战.md) |
| 09 | 选型对比与决策 | vs Chroma/Milvus/Qdrant | 决策者 | [09-选型对比与决策](./09-选型对比与决策.md) |
| 10 | 生产实践与面试题 | 坑清单、面试冲刺 | 面试冲刺 | [10-生产实践与面试题](./10-生产实践与面试题.md) |

---

## 3. 为什么必须学透 PgVector

1. **"已有 PG 就用 PG"是选型第一问**：绝大多数后端系统已经有 PostgreSQL——向量检索与业务数据同库、同事务、同备份体系，省掉一个独立服务的运维成本。PigVector 是"最不新增基础设施的向量方案"，选型对比的第一章永远是它。
2. **2026 年 0.8.x 刚完成关键升级**：iterative scans 修复了"过滤条件吞掉 ANN 结果"的历史缺陷（0.8.0）、sparsevec 原生稀疏向量、binary_quantize 二进制量化、并行建索引——**老教程的 0.7 认知已不完整，本体系按 0.8.6 现状撰写**。
3. **SQL 能力是差异化武器**：与 Chroma/Qdrant 相比，PgVector 继承了 PG 的完整 SQL——JOIN 业务表、事务一致性、行级权限、分区、备份恢复——这些"向量库做不到的事"恰是它的核心竞争力。
4. **Java 生态官方集成**：Spring AI 官方 `spring-ai-pgvector-store-spring-boot-starter`，JdbcTemplate 直连只需一条 `<->` 操作符——Java 后端接入成本在所有向量方案中最低。
5. **面试高频考点**：向量扩展如何融入关系型数据库、HNSW vs IVFFlat 的取舍、混合检索 RRF 融合，都是 AI 应用面试能讲深的话题。

---

## 4. 核心概念速查

### 4.1 核心概念

| 概念 | 一句话 | 类比（关系型） |
|------|--------|--------------|
| 扩展（Extension） | `CREATE EXTENSION vector` 启用的库级能力 | 插件 |
| `vector` 类型 | float32 向量列（≤2000 维） | 特殊列 |
| `halfvec` | float16 向量列（≤4000 维，存储减半） | 压缩列 |
| `sparsevec` | 稀疏向量（≤1000 非零元素） | 稀疏列 |
| 距离操作符 | `<->`/`<=>`/`<#>` 等相似度算法 | 排序规则 |
| HNSW | 图式近似索引（默认推荐） | 索引 |
| IVFFlat | 聚类近似索引（内存敏感场景） | 旧索引 |
| iterative scans | 0.8 新增：过滤不足时重入索引 | 索引优化 |

### 4.2 版本时间线（时效性重点）

| 版本 | 时间 | 关键变化 |
|------|:----:|---------|
| 0.5-0.6 | 2022-2023 | 基本类型与 HNSW/IVFFlat |
| 0.7.x | 2024 | 稳定演进，支持 PG 12-16 |
| **0.8.0** | **2025 末-2026 初** | **iterative scans、sparsevec、binary_quantize、halfvec、并行建索引** |
| 0.8.4 | 2026-06-30 | 修复 HNSW vacuum 插入冲突、IVFFlat 内存超限 |
| 0.8.5 | 2026-07-08 | IVFFlat 小表建索引内存优化 |
| **0.8.6** | **2026-07-29** | **当前最新（2026-08 基线）** |

### 4.3 已知局限速查（选型前必看）

- 元数据过滤在 ANN 扫描之后（post-filter），高选择性过滤损失召回（0.8 iterative scans 缓解但非根治）
- 规模上限约 50M-100M 向量，超出需评估专用向量库
- HNSW 索引占内存：1M 行 1536 维 ≈ 6GB
- 向量维度上限：vector 2000 维（text-embedding-3-large 3072 维需 halfvec）

---

## 5. 与周边知识的关系

```text
                    ┌── 04-向量数据库/ —— 通用向量库体系（索引原理/多库选型）
                    ├── 02-关系型数据库/PostgreSQL —— PG 本体知识（索引/分区/事务）
                    ├── 02-RAG检索增强生成/ —— RAG 方法论
                    ├── 01-大模型基础与Prompt工程/Embedding —— 向量从哪来
                    ├── Chroma/ —— 嵌入式向量库（轻量场景替代）
                    ├── Milvus/ Qdrant/ —— 专用向量库（规模场景替代）
                    └── 06-开发框架-LangChain4j-SpringAI/ —— Java 集成框架
```

**本体系与 08-向量数据库/ 的分工**：那边讲"向量数据库是什么、索引原理通识、多库选型对比"；本体系讲"**PgVector 这个扩展本身（0.8.x 现状）**"——类型、操作符、索引调优、混合检索、运维、Java 集成，并与 PostgreSQL 主体系（`02-.../01-关系型数据库/`）互补：那边讲 PG 本体，本体系讲 PG 上的向量能力。

---

## 6. 学习路线推荐

**路线一：快速上手（半天，对应模块 01-03）**
定位与版本 → 安装 → 类型与操作符；`CREATE EXTENSION` + 建表 + 一条 `<->` 查询跑通最小链路。

**路线二：进阶深化（2-3 天，对应模块 04-07）**
索引原理与调优 → 过滤与 iterative scans → 混合检索 → 性能优化；用 EXPLAIN 验证索引生效，跑通 HNSW + FTS + RRF 混合检索。

**路线三：工程实战（对应模块 08-10）**
Spring AI 集成 → 选型对比 → 生产实践；用 Spring Boot + pgvector 搭 RAG 应用，最后过一遍坑清单与面试题。

> 🎯 **核心要点**：PgVector 的学习终点 = "**会建表选类型、会用操作符、会建 HNSW 调参、会用 RRF 混合检索、知道 50M 上限**"——五件事覆盖从同库向量检索到生产 RAG 的全部链路；记住"SQL 与事务是它的灵魂，post-filter 与规模上限是它的边界"。

---

## 7. 快速自测 10 题

1. PgVector 0.8.x 相比 0.7 的四大增量是什么？最新版本是多少？
2. `vector` / `halfvec` / `sparsevec` / `bit` 四种类型各适合什么场景？维度上限是多少？
3. 六个距离操作符分别对应什么度量？`<#>` 为什么要取负？
4. HNSW 和 IVFFlat 的本质区别？为什么 HNSW 可以在空表上建索引？
5. `m` / `ef_construction` / `ef_search` 分别影响什么？调参顺序怎么定？
6. iterative scans 解决了什么问题？`strict_order` 和 `relaxed_order` 的区别？
7. 混合检索为什么用 RRF？和加权融合比有什么取舍？
8. binary_quantize 两阶段检索的原理？召回率能到多少？
9. Spring AI 怎么接入 PgVector？核心依赖和配置是什么？
10. 什么场景适合 PgVector，什么规模必须换专用向量库？

> 💡 答不出的题目对应上方模块导航中的文件，按图索骥复习即可。

---

**参考来源**：

- [pgvector 官方 GitHub（CHANGELOG）](https://github.com/pgvector/pgvector/blob/master/CHANGELOG.md)
- [pgvector 0.8 新特性解读](https://www.jusdb.com/blog/pgvector-08-new-features-postgresql-vector-search)
- [Hivebook: pgvector 0.8.x 详解](https://hivebook.wiki/wiki/pgvector-0-8-x-postgres-vector-similarity-extension-vector-halfvec-sparsevec-bit-types-hnsw-ivfflat-indexes-iterative-scans-0-8-0-binary-quantization-hybrid-search-with-fts-and-what-changed-in-0-6-x-0-8-x)
- [Supabase HNSW 索引文档](https://supabase.com/docs/guides/ai/vector-indexes/hnsw-indexes)
- [Spring AI pgvector starter](https://libraries.io/maven/org.springframework.ai:spring-ai-pgvector-store-spring-boot-starter)

---

**下一模块**：[01-定位与版本演进](./01-定位与版本演进.md)
