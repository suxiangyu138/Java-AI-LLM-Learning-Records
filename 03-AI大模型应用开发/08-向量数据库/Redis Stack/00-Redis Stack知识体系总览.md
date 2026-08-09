# Redis Stack 知识体系总览

> Redis Stack 已于 2025 年并入 Redis 8——搜索、向量、JSON、时序、概率结构全部内置进核心，2026 年的"Redis Stack"就是 Redis 8 本身：业务数据、缓存、向量同库同实例

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [为什么必须学透 Redis Stack](#3-为什么必须学透-redis-stack)
4. [核心概念速查](#4-核心概念速查)
5. [与周边知识的关系](#5-与周边知识的关系)
6. [学习路线推荐](#6-学习路线推荐)
7. [快速自测 10 题](#7-快速自测-10-题)

---

## 1. 知识体系导图

```text
Redis Stack（Redis 8）知识体系
│
├── 01 定位与版本演进
│   ├── Stack 历史：模块化时代（RediSearch/RedisJSON/...）
│   ├── 2025-05 并入 Redis 8.0（Redis Open Source 统一发行版）
│   ├── 版本线：8.0 → 8.2 → 8.4（FT.HYBRID / SVS-VAMANA / Vector Set）
│   └── Stack 维护版 2025-12 停更——还跑 Stack = 无补丁
│
├── 02 核心概念与数据模型
│   ├── 四类数据结构：Hash / JSON / TimeSeries / Bloom
│   ├── FT 索引模型：索引 → 文档 → 字段（TEXT/TAG/NUMERIC/VECTOR）
│   ├── Vector Set（beta）：一等公民的高维相似类型
│   └── 与关系型/向量库的概念映射
│
├── 03 安装与快速开始
│   ├── Redis 8 安装与 Docker（含 Redis Insight 8001）
│   ├── FT.CREATE 建索引最小示例
│   ├── FT.SEARCH 查询最小闭环
│   └── DIALECT 2 与命令族速查
│
├── 04 全文检索 RediSearch
│   ├── 全文索引：TEXT 字段、BM25 默认打分
│   ├── TAG / NUMERIC / GEO 过滤字段
│   ├── 中文分词方案（自带/扩展）
│   └── 弃用命令族（FT.ADD/FT.DEL/FT.SYNADD）
│
├── 05 向量索引与检索原理
│   ├── 向量字段定义：HNSW / FLAT / SVS-VAMANA（8.4）
│   ├── 距离度量：COSINE / L2 / IP
│   ├── HNSW 参数：M / EF_CONSTRUCTION / EF_RUNTIME
│   └── KNN 查询语法与分数语义
│
├── 06 混合查询与高级检索
│   ├── pre-filter + KNN 混合查询
│   ├── FT.HYBRID（8.4）：SEARCH + VSIM 双腿 RRF 融合
│   ├── 向量范围检索 VECTOR_RANGE
│   └── 查询优化：batches 模式 vs 暴力模式
│
├── 07 Vector Set 与数据扩展
│   ├── Vector Set（beta）：VADD/VSIM/VCARD 命令族
│   ├── RedisJSON：JSONPath 查询与索引
│   ├── RedisTimeSeries：时序数据
│   └── RedisBloom：概率结构（布隆/Cuckoo/Top-K）
│
├── 08 生态集成与 RAG 实战
│   ├── Spring AI：spring-ai-starter-vector-store-redis
│   ├── Spring Data Redis / Redis OM Spring
│   ├── Redisson 4.2（Spring AI Vector Store）
│   └── 语义缓存 + RAG 完整链路
│
├── 09 选型对比与决策
│   ├── vs Chroma / Qdrant / PgVector
│   ├── "已有 Redis"场景的独特定位
│   ├── 规模与语义缓存红利
│   └── 选型决策树
│
└── 10 生产实践与面试题
    ├── 迁移注意（ACL / loadmodule / FT.* 弃用）
    ├── 避坑清单与性能真相
    └── 面试高频题与答题范式
```

---

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 | 文件 |
|:---:|------|---------|---------|------|
| 01 | 定位与版本演进 | Stack→Redis 8 合并、8.0-8.4 | 全部必须掌握 | [01-定位与版本演进](./01-定位与版本演进.md) |
| 02 | 核心概念与数据模型 | 数据结构/FT 索引/Vector Set | 全部必须掌握 | [02-核心概念与数据模型](./02-核心概念与数据模型.md) |
| 03 | 安装与快速开始 | Redis 8 部署、最小闭环 | 全部必须掌握 | [03-安装与快速开始](./03-安装与快速开始.md) |
| 04 | 全文检索 RediSearch | BM25/字段类型/中文 | 中高级 | [04-全文检索RediSearch](./04-全文检索RediSearch.md) |
| 05 | 向量索引与检索原理 | HNSW/FLAT/KNN 语法 | 中高级 | [05-向量索引与检索原理](./05-向量索引与检索原理.md) |
| 06 | 混合查询与高级检索 | pre-filter/FT.HYBRID | 中高级 | [06-混合查询与高级检索](./06-混合查询与高级检索.md) |
| 07 | Vector Set 与数据扩展 | VSET/JSON/时序/Bloom | 中高级 | [07-VectorSet与数据扩展](./07-VectorSet与数据扩展.md) |
| 08 | 生态集成与 RAG 实战 | Spring AI/Redis OM | 中高级 | [08-生态集成与RAG实战](./08-生态集成与RAG实战.md) |
| 09 | 选型对比与决策 | vs 向量库、缓存定位 | 决策者 | [09-选型对比与决策](./09-选型对比与决策.md) |
| 10 | 生产实践与面试题 | 迁移/坑清单/面试 | 面试冲刺 | [10-生产实践与面试题](./10-生产实践与面试题.md) |

---

## 3. 为什么必须学透 Redis Stack

1. **2025 年完成范式级合并**：Redis Stack 模块并入 Redis 8.0 核心，2026 年起"Redis Stack"= Redis 8——**老教程的 Stack 安装/模块加载/版本矩阵已全部失效，本体系按 Redis 8 现状撰写**。
2. **"已有 Redis"场景的零新增向量方案**：业务数据、缓存、向量同库同实例——不需要再部署独立的向量数据库（对比 Chroma/Qdrant 要多维护一个服务），这是 RAG 场景最省运维的路径之一。
3. **向量 + 全文 + 过滤的混合查询是原生能力**：RediSearch 的 `pre-filter + KNN` 与 8.4 的 `FT.HYBRID`（RRF 融合）让"文本过滤 + 语义检索"一条命令完成——比多数独立向量库的 post-filter 更先进。
4. **Java 生态深度集成**：Spring AI 官方 `spring-ai-starter-vector-store-redis`、Spring Data Redis、Redis OM Spring、Redisson 4.2——Java 后端接入 Redis 向量能力的路径最丰富。
5. **语义缓存是 Redis 独有的 RAG 场景红利**：向量相似度命中缓存（语义缓存）只有"缓存型"存储才能天然提供——这是 Redis 8 在 AI 应用中的差异化价值。

---

## 4. 核心概念速查

### 4.1 核心概念

| 概念 | 一句话 | 类比 |
|------|--------|------|
| Redis Stack | 已并入 Redis 8 的模块集合（2025-05 起） | 曾用名 |
| RediSearch | 全文 + 二级索引 + 向量检索引擎 | 搜索引擎 |
| RedisJSON | 原生 JSON 文档存储与查询 | 文档库 |
| FT 索引 | `FT.CREATE` 定义的字段级索引 | 表索引 |
| VECTOR 字段 | 向量列（HNSW/FLAT/SVS-VAMANA） | 向量列 |
| Vector Set | 一等公民向量类型（beta） | 原生向量集 |
| KNN 查询 | `=>[KNN k @field $vec]` 近邻检索 | ANN 检索 |
| 混合查询 | 过滤条件 + KNN 一条命令 | 过滤感知 |

### 4.2 版本时间线（时效性重点）

| 版本 | 时间 | 关键变化 |
|------|:----:|---------|
| Redis Stack 7.4 | 2024-2025 | 模块化时代（独立安装） |
| **Redis 8.0** | **2025-05-02** | **Stack + CE 合并为 Redis Open Source；搜索/JSON/时序/Bloom 内置；BM25 默认；Vector Set 引入** |
| 8.0.6 | 2026-02-22 | 8.0 分支最新稳定版 |
| 8.2 | 2026-03 | 大版本增强（IBM Cloud 等托管 GA） |
| **8.4** | 2026 | **FT.HYBRID（RRF 融合）、SVS-VAMANA 索引、Vector Set 成熟** |
| Stack 维护版 | 2025-12 停更 | 还跑 Stack = 无安全补丁 |

### 4.3 已知局限速查（选型前必看）

- 向量规模受内存约束（向量+索引常驻内存），百万级向量需评估
- Vector Set 仍是 beta（生产慎用，Query Engine 是主路径）
- 中文全文检索依赖分词方案（内置分词对中文支持有限）
- 许可变化：RSALv2/SSPLv1/AGPLv3 三许可并存

---

## 5. 与周边知识的关系

```text
                    ┌── 02-非关系型数据库/Redis —— Redis 本体体系（数据结构/持久化/高可用）
                    ├── 04-向量数据库/ —— 通用向量库体系（索引原理/多库选型）
                    ├── 02-RAG检索增强生成/ —— RAG 方法论
                    ├── Chroma/ —— 嵌入式向量库（轻量原型场景）
                    ├── Qdrant/ —— 独立服务向量库（强过滤场景）
                    ├── PgVector/ —— PG 扩展（已有 PG 场景）
                    └── 06-开发框架-LangChain4j-SpringAI/ —— Java 集成框架
```

**本体系与 Redis 主体系的分工**：`02-.../02-非关系型数据库/Redis/` 讲 Redis 本体（缓存、数据结构、集群、持久化）；本体系讲"**Redis 8 的搜索与向量能力**"——RediSearch 全文检索、向量索引、混合查询、Vector Set、Spring AI 集成。与 Chroma/Qdrant/PgVector 三体系互补：**"已有 Redis"是它区别于所有独立向量库的第一决策变量**。读法建议：先主体系打底（命令模型/内存模型），再进本体系深化搜索与向量。

---

## 6. 学习路线推荐

**路线一：快速上手（半天，对应模块 01-03）**
定位与版本 → 核心概念 → Docker 部署 + FT.CREATE/FT.SEARCH 最小闭环；理解"Redis 8 = 搜索向量一体化"。

**路线二：进阶深化（2-3 天，对应模块 04-06）**
全文检索 → 向量索引与 KNN → 混合查询；跑通"过滤 + KNN"与 FT.HYBRID 融合，理解 HNSW 参数。

**路线三：工程实战（对应模块 07-09）**
Vector Set 与数据扩展 → Spring AI 集成 → 选型对比；用 Spring AI + Redis 搭 RAG 应用（含语义缓存），最后过一遍面试题。

> 🎯 **核心要点**：Redis Stack 的学习终点 = "**会建 FT 索引、会写 KNN 与混合查询、会用 Spring AI 接入、知道语义缓存红利、知道 Stack 已并入 Redis 8**"——五件事覆盖从"已有 Redis"到生产 RAG 的全部链路；记住"2025-05 合并"与"混合查询"是两个最值钱的考点。

---

## 7. 快速自测 10 题

1. Redis Stack 和 Redis 8 是什么关系？Stack 维护版什么时候停更？
2. FT 索引的字段类型有哪些？VECTOR 字段怎么定义？
3. HNSW 和 FLAT 分别适合什么场景？SVS-VAMANA 是什么？
4. KNN 查询的语法结构是什么？`__embedding_score` 是什么？
5. 混合查询怎么做？8.4 的 FT.HYBRID 和传统 pre-filter 有什么区别？
6. Vector Set 是什么？和 Query Engine 的向量索引什么关系？
7. 中文全文检索在 Redis 8 里怎么解决？
8. Spring AI 怎么接入 Redis 向量？核心依赖和配置？
9. 语义缓存是什么？为什么只有 Redis 类存储能做？
10. 从 Redis Stack 迁移到 Redis 8 要检查哪三件事？

> 💡 答不出的题目对应上方模块导航中的文件，按图索骥复习即可。

---

**参考来源**：

- [Redis 8.0 发布公告（Redis 官方博客）](https://redis.io/blog/redis-8-ga.md)
- [Redis 8.0 新特性文档](https://redis.io/docs/latest/develop/whats-new/8-0/)
- [Redis Stack 弃用与 Redis 8 迁移（Markaicode）](https://markaicode.com/benchmarks/redis-stack-production-benchmark-latency/)
- [Redis 8 向量搜索教程（官方）](https://redis.io/tutorials/howtos/solutions/vector/getting-started-vector.md)
- [Redis 向量文档（中文）](https://redis.ac.cn/docs/latest/develop/interact/search-and-query/advanced-concepts/vectors/)

---

**下一模块**：[01-定位与版本演进](./01-定位与版本演进.md)
