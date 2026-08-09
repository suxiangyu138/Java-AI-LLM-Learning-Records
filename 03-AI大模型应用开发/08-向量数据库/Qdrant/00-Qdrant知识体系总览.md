# Qdrant 知识体系总览

> Qdrant 是 Rust 编写的开源向量数据库——单二进制部署、过滤感知搜索、2026 年 TurboQuant 量化与分布式集群，从"过滤最强"的向量库进化为生产级 RAG 基础设施

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [为什么必须学透 Qdrant](#3-为什么必须学透-qdrant)
4. [核心概念速查](#4-核心概念速查)
5. [与周边知识的关系](#5-与周边知识的关系)
6. [学习路线推荐](#6-学习路线推荐)
7. [快速自测 10 题](#7-快速自测-10-题)

---

## 1. 知识体系导图

```text
Qdrant 知识体系
│
├── 01 定位与版本演进
│   ├── Rust 单二进制、REST/gRPC 双 API
│   ├── 版本时间线：1.17（相关性反馈）→ 1.18（TurboQuant）→ 1.19（Turbo4）
│   ├── Cloud 企业能力：GPU 索引 / Multi-AZ / 审计日志
│   └── 与 Chroma/PgVector 的定位差异
│
├── 02 核心概念与数据模型
│   ├── Collection / Point / Payload 三级模型
│   ├── 命名向量：dense / sparse / multi-dense（ColBERT）
│   ├── payload 类型系统与索引
│   └── 与关系型的概念映射
│
├── 03 快速开始与客户端
│   ├── Docker 部署与端口（REST 6333 / gRPC 6334）
│   ├── Python 客户端 CRUD 全流程
│   ├── Java 客户端（io.qdrant:client）
│   └── REST/gRPC 直接调用
│
├── 04 存储架构与检索原理
│   ├── Collection → Shard → Segment 存储层级
│   ├── WAL 写路径与后台 compaction
│   ├── 过滤感知搜索（filter-aware search）原理
│   └── 单节点 vs 分布式数据流
│
├── 05 索引与量化配置
│   ├── HNSW 参数（M / ef_construct / ef）
│   ├── 量化全景：scalar / binary / PQ / TurboQuant / Turbo4
│   ├── 两阶段重排与内存层级（pinned/cached/cold）
│   └── 内存预算与调优
│
├── 06 检索过滤与混合检索
│   ├── 过滤操作符全解（范围/嵌套/前缀/slice）
│   ├── payload 索引类型（keyword/geo/text/tenant）
│   ├── 混合检索：dense + sparse + RRF
│   ├── Relevance Feedback 相关性反馈
│   └── 分组与聚合查询
│
├── 07 部署与分布式集群
│   ├── 单机 Docker/K8s 部署
│   ├── 分布式：Raft 元数据 + 分片 + 副本
│   ├── 扩缩容与重分片
│   ├── 监控与备份（快照/遥测）
│   └── Qdrant Cloud：GPU / Multi-AZ / 审计
│
├── 08 生态集成与 RAG 实战
│   ├── Java：官方客户端 / LangChain4j / Spring AI
│   ├── Python：LangChain / LlamaIndex / FastEmbed
│   ├── 完整 RAG 链路实战
│   └── 评估闭环（命中率 + MRR）
│
├── 09 选型对比与决策
│   ├── vs Chroma / PgVector / Milvus / Weaviate
│   ├── 过滤感知是最大差异化武器
│   ├── 规模与成本权衡
│   └── 选型决策树
│
└── 10 生产实践与面试题
    ├── 性能真相与避坑清单
    ├── 生产落地清单
    └── 面试高频题与答题范式
```

---

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 | 文件 |
|:---:|------|---------|---------|------|
| 01 | 定位与版本演进 | Rust 单二进制、1.17-1.19 演进 | 全部必须掌握 | [01-定位与版本演进](./01-定位与版本演进.md) |
| 02 | 核心概念与数据模型 | Collection/Point/Payload/命名向量 | 全部必须掌握 | [02-核心概念与数据模型](./02-核心概念与数据模型.md) |
| 03 | 快速开始与客户端 | Docker、Python/Java 客户端 CRUD | 全部必须掌握 | [03-快速开始与客户端](./03-快速开始与客户端.md) |
| 04 | 存储架构与检索原理 | Segment/WAL/过滤感知搜索 | 中高级 | [04-存储架构与检索原理](./04-存储架构与检索原理.md) |
| 05 | 索引与量化配置 | HNSW/量化全景/内存层级 | 中高级 | [05-索引与量化配置](./05-索引与量化配置.md) |
| 06 | 检索过滤与混合检索 | 过滤操作符/混合检索/反馈 | 中高级 | [06-检索过滤与混合检索](./06-检索过滤与混合检索.md) |
| 07 | 部署与分布式集群 | Raft/分片副本/Cloud | 中高级 | [07-部署与分布式集群](./07-部署与分布式集群.md) |
| 08 | 生态集成与 RAG 实战 | Java/LangChain4j/Spring AI | 中高级 | [08-生态集成与RAG实战](./08-生态集成与RAG实战.md) |
| 09 | 选型对比与决策 | vs Chroma/PgVector/Milvus | 决策者 | [09-选型对比与决策](./09-选型对比与决策.md) |
| 10 | 生产实践与面试题 | 坑清单、面试冲刺 | 面试冲刺 | [10-生产实践与面试题](./10-生产实践与面试题.md) |

---

## 3. 为什么必须学透 Qdrant

1. **过滤感知搜索是差异化王牌**：Chroma/PgVector 的过滤都是 post-filter（ANN 后置），过滤选择性高就吞结果；Qdrant 把过滤**推进图遍历内部**（约束 HNSW）或自动降级精确扫描——"强过滤 + 大规模"场景它是开源首选，这是面试与选型都能讲深的点。
2. **2026 年完成量化与可观测性升级**：1.18 的 TurboQuant（Google 的 Hadamard 旋转量化，2× 压缩比）与 1.19 的 Turbo4（4-bit 唯一存储表示，存储减 9 倍）把"量化必损"的认知改写——**老教程的 1.10-1.15 认知已不完整，本体系按 1.19 现状撰写**。
3. **Rust 单二进制的部署心智**：一个二进制同时是单机库与分布式节点（Raft 元数据 + 分片副本），Docker 一条命令起、K8s 一条 Helm 上集群——部署复杂度介于 Chroma（零部署）与 Milvus（K8s 全家桶）之间，是"要规模又不想要运维地狱"的甜区。
4. **Java 生态成熟**：官方 `io.qdrant:client`（gRPC）、LangChain4j `QdrantEmbeddingStore`、Spring AI 官方 starter 三路齐全——Java 后端接入零障碍，本体系给完整链路。
5. **面试高频考点**：过滤感知 vs post-filter 的原理差异、TurboQuant 量化原理、Raft + 分片 + 副本的分布式设计、多向量（ColBERT）晚交互检索——每个都能答出深度。

---

## 4. 核心概念速查

### 4.1 核心概念

| 概念 | 一句话 | 类比（关系型） |
|------|--------|--------------|
| Collection | 顶层容器：维度/度量/分片数配置 | 表 |
| Point | 数据单元：id + 向量 + payload | 行 |
| Payload | 自由 JSON 元数据，用于过滤 | 列集合 |
| 命名向量 | 一个点可带多个命名向量（dense/sparse） | 多列 |
| Shard | 数据水平分片（一致哈希） | 分表 |
| Segment | 分片内存储单元，后台 compaction | 段 |
| HNSW | 图式近似索引 | 索引 |
| 量化 | scalar/binary/PQ/TurboQuant 压缩 | 压缩 |
| 过滤感知 | 过滤融入图遍历而非事后 | 索引下推 |

### 4.2 版本时间线（时效性重点）

| 版本 | 时间 | 关键变化 |
|------|:----:|---------|
| 1.10-1.15 | 2024-2025 | 稳定演进、命名向量、gRPC 完善 |
| **1.17** | **2026-02-20** | **Relevance Feedback 查询、更新队列、延迟 fan-out、集群遥测** |
| **1.18** | **2026-05-11** | **TurboQuant 量化（2× 压缩比）、内存监控、命名向量增删、严格模式** |
| **1.19** | **2026** | **Turbo4（4-bit 唯一表示，存储减 9 倍）、Unified Memory Tiers、Per-Tenant IDF、前缀匹配** |
| Cloud | 2026-04-28 | GPU 索引（4× 构建）、Multi-AZ（99.95% SLA）、审计日志 |

### 4.3 已知局限速查（选型前必看）

- 单节点内存随数据线性增长（HNSW 图常驻），经验预算 1.5-2× 原始向量大小
- 分布式扩缩容需要重分片操作（1.19 起有实时进度）
- 全文检索（payload Text 字段）能力弱于专用搜索引擎
- 免费版无内置认证（1.x 开源版），安全靠网络层或 Cloud

---

## 5. 与周边知识的关系

```text
                    ┌── 04-向量数据库/ —— 通用向量库体系（索引原理/多库选型）
                    ├── 02-RAG检索增强生成/ —— RAG 方法论（切分/检索/重排）
                    ├── 01-大模型基础与Prompt工程/Embedding —— 向量从哪来
                    ├── Chroma/ —— 嵌入式向量库（轻量原型场景）
                    ├── PgVector/ —— PG 扩展（已有 PG 场景）
                    ├── Milvus/ —— 分布式向量库（十亿级场景）
                    └── 06-开发框架-LangChain4j-SpringAI/ —— Java 集成框架
```

**本体系与 08-向量数据库/ 的分工**：那边讲"向量数据库是什么、索引原理通识、多库选型对比"；本体系讲"**Qdrant 这个产品本身（1.19 现状）**"——数据模型、存储架构、量化、过滤感知、分布式、生态集成，与 Chroma（嵌入式）、PgVector（PG 扩展）、Milvus（超大规模）三体系互补，覆盖"轻 → 中 → 重"谱系的中间段。

---

## 6. 学习路线推荐

**路线一：快速上手（半天，对应模块 01-03）**
定位与版本 → 核心概念 → Docker 部署 + Python 客户端；跑通"建 collection → upsert → 相似搜索"最小链路，理解 Point/Payload/命名向量。

**路线二：进阶深化（2-3 天，对应模块 04-06）**
存储架构与过滤感知原理 → 索引量化 → 检索过滤与混合检索；用 EXPLAIN 风格工具验证过滤路径，跑通 dense + sparse 混合检索与量化两阶段。

**路线三：工程实战（对应模块 07-09）**
分布式集群部署 → 生态集成 → 选型对比；用 Helm 起集群、Java 客户端接入 RAG 应用，最后过一遍面试题。

> 🎯 **核心要点**：Qdrant 的学习终点 = "**会建 Collection 配命名向量、会用过滤感知检索、会选量化、会起分布式集群、知道它和 post-filter 派系的本质差异**"——五件事覆盖从单机原型到生产集群的全部链路；记住"过滤感知搜索"与"TurboQuant"是两个最值钱的考点。

---

## 7. 快速自测 10 题

1. Qdrant 1.19 相比 1.17 的三大增量是什么？当前最新版本？
2. Collection / Point / Payload 三级模型各是什么？命名向量解决了什么问题？
3. Docker 启动后 REST 和 gRPC 分别是什么端口？
4. Segment 的 appendable/optimized 两种状态是什么关系？compaction 何时触发？
5. 过滤感知搜索和 post-filter 的本质区别？为什么延迟可预测？
6. TurboQuant 的原理是什么？和 scalar 量化比优势在哪？Turbo4 又是什么？
7. 混合检索怎么做？dense 和 sparse 各自擅长什么？
8. Qdrant 分布式用什么共识协议？分片和副本分别解决什么问题？
9. Java 接入 Qdrant 有哪三条路径？
10. 什么场景选 Qdrant 而不是 Chroma / PgVector / Milvus？

> 💡 答不出的题目对应上方模块导航中的文件，按图索骥复习即可。

---

**参考来源**：

- [Qdrant 官方博客 1.19](https://qdrant.tech/blog/qdrant-1.18.x/)
- [Qdrant 官方博客 1.18](https://qdrant.tech/blog/qdrant-1.18.x/)
- [Qdrant 官方博客 1.17](https://qdrant.tech/blog/qdrant-1.17.x/)
- [Qdrant Cloud 企业发布](https://qdrant.tech/blog/qdrant-cloud-enterprise-launch/)
- [DeepWiki: qdrant 架构分析](https://deepwiki.com/qdrant/qdrant/1.1-key-concepts-and-terminology)

---

**下一模块**：[01-定位与版本演进](./01-定位与版本演进.md)
