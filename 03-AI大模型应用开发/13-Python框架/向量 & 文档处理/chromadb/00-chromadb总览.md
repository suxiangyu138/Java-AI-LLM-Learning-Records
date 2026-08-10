# 00 - chromadb 总览

> 定位：ChromaDB——"嵌入式向量数据库，Python 生态 RAG 的第一选择"——一行 `pip install chromadb` 就有向量库可用，原型到中小规模生产（1-2M 向量）一站打通——184.8M 下载的 RAG 检索存储端

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [与主体系的分工](#3-与主体系的分工)
4. [学习路线推荐](#4-学习路线推荐)
5. [核心概念速查](#5-核心概念速查)
6. [常见误区](#6-常见误区)
7. [一周学习计划示例](#7-一周学习计划示例)
8. [快速自测 10 题](#8-快速自测-10-题)
9. [参考来源](#9-参考来源)

---

## 1. 知识体系导图

```text
chromadb（本体系 11 篇）
├── 定位层：01 chromadb 是什么（嵌入式向量数据库/RAG 原型事实标准）
│          02 安装与快速开始（三客户端/Collection 五步）
├── 核心层：03 核心概念与数据模型（ID/Document/Embedding/Metadata）
│          04 嵌入与 EmbeddingFunction（ONNX 默认/OpenAI/自定义）
│          05 数据写入与更新（add/upsert/delete/get）
├── 应用层：06 查询与过滤（query/where/where_document 全语法）
│          07 索引与距离度量（cosine/l2/ip/HNSW 参数）
│          08 与 LangChain/LlamaIndex 集成（LangChainChroma/ChromaVectorStore）
│          09 生产实践与性能（服务器模式/鉴权/Docker/1.x Rust）
└── 验收层：10 生产实战与自测（端到端 RAG + 20 题）
```

## 2. 模块导航

| 篇 | 模块 | 核心内容 | 核心产出 |
|:---:|------|---------|---------|
| 00 | 总览 | 导图/分工/路线/速查/误区 | 学习计划 |
| 01 | chromadb 是什么 | 嵌入式向量库定位/2026 基线/替代品 | 认知 |
| 02 | 安装与快速开始 | 三客户端/Collection 五步 CRUD | 会建库 |
| 03 | 核心概念与数据模型 | 五要素/元数据 schema 约束 | 懂数据 |
| 04 | 嵌入与 EmbeddingFunction | 默认 ONNX/OpenAI/自定义/持久化 | 会嵌入 |
| 05 | 数据写入与更新 | add/upsert/update/delete/批量 | 会写库 |
| 06 | 查询与过滤 | query/where 全操作符/混合过滤 | 会检索 |
| 07 | 索引与距离度量 | cosine vs l2/ip/HNSW 参数/不可变 | 会调参 |
| 08 | 框架集成 | LangChain/LlamaIndex/unstructured 配合 | 会集成 |
| 09 | 生产与性能 | 部署形态/鉴权/Docker/备份/1.x 变化 | 会部署 |
| 10 | 实战与自测 | 端到端 RAG + 20 题 | 毕业产出 |

## 3. 与主体系的分工

**与 RAG 体系（`../../../04-RAG检索增强生成/阶段%201：基础概念/00-阶段总览与学习路径.md`）的分工**：RAG 链路"加载 → 切分 → 向量化 → 检索 → 生成"，那个体系讲全链路方法论，**本体系专讲"向量化 + 检索存储"这一环的嵌入式实现**——**"unstructured 管喂什么数据进库，Chroma 管数据怎么存怎么取"**。

**与向量数据库主体系（`../../../08-向量数据库/向量数据库/00-向量数据库知识体系总览.md`）及 Chroma 主体系（`../../../08-向量数据库/Chroma/00-Chroma知识体系总览.md`）的分工**：那两套讲**原理与选型全景**（索引原理/存储架构/版本演进/多库对比），本体系是 **13-Python 框架视角的实操篇**——**"原理课讲为什么，本体系讲怎么用：Python API 的每个参数、每个坑"**（同一数据库，课本 vs 工具书）。

**与 milvus‑python（`../../强烈推荐%EF%BC%88做项目必用%EF%BC%8C简历加分%EF%BC%89/milvus‑python/00-milvus‑python总览.md`）的分工**：同为 RAG 检索存储端 SDK——**Chroma 是嵌入式轻量端（原型/中小规模，<200MB 内存），Milvus 是分布式重器端（百万级以上）**；选型标准与互迁路径在 01/09 篇。

**与 unstructured（`../../强烈推荐%EF%BC%88做项目必用%EF%BC%8C简历加分%EF%BC%89/unstructured/00-unstructured总览.md`）的分工**：unstructured 是数据预处理端（文档解析+分块），Chroma 是检索存储端——**"unstructured 产出 Chunk → embedding → Chroma 入库检索"是 RAG 数据管道的完整两段**（10 篇实战打通）。

**与 LangChain（`../../强烈推荐%EF%BC%88做项目必用%EF%BC%8C简历加分%EF%BC%89/LangChain/00-LangChain总览.md`）、LlamaIndex（`../../强烈推荐%EF%BC%88做项目必用%EF%BC%8C简历加分%EF%BC%89/llama‑index/00-LlamaIndex总览.md`）的分工**：那两个框架各自封装了 LangChainChroma / ChromaVectorStore——**"本体系讲裸库 API，框架篇讲集成姿势——裸库是集成层的地基"**（08 篇详讲）。

**2026-08 基线**：chromadb **1.5.9**（2026-05-05 发布，1.5.x 系列 2026 年十连更：1.5.0 于 2026-02-09 → 1.5.9 于 2026-05-05）；**关键认知**：**v1.0.0（2025-03-01）Rust 核心重写**——写入/查询约 4 倍提升（服务器模式写入 ~10K → ~40K+ vectors/s，标准查询 p95 约 10ms）；Apache-2.0、**PyPI 累计 184.8M 下载**（近 30 天约 12.6M，周更节奏）；默认嵌入 all-MiniLM-L6-v2（384 维，本地 ONNX 运行）、默认距离 l2；**"原型用嵌入式三行起步，生产走服务器模式/Docker——1-2M 向量以内 Chroma 足够，再大换 Milvus/Qdrant"**。

## 4. 学习路线推荐

**路线一：标准路线（3-5 天）**——01 → 10 逐篇 + 每篇动手——**毕业标准：独立实现"文档分块 → 嵌入 → Chroma 入库 → 过滤检索 → LangChain 问答"的端到端 RAG**。

**路线二：速成路线（1-2 天）**——01 → 02 → 06 → 08 → 10（跳过 03/04/05/07/09 精读）——适合已有向量库经验、只想最快把 Chroma 用起来的人。

**路线三：项目驱动路线**——RAG 项目遇到"向量库选型/过滤写法/部署持久化"按需查篇——**"collection.query 一行上手，难点全在过滤语法与生产持久化——遇到再查"**。

## 5. 核心概念速查

| 概念 | 一句话 | 对应篇 |
|------|--------|:---:|
| Collection | 集合：向量表（ID/Embedding/Metadata/Document） | 02/03 |
| Embedding | 向量：文本的数字表示（默认 384 维 ONNX） | 04 |
| Metadata | 元数据：可过滤的标签/属性（where 过滤对象） | 03/06 |
| Document | 原始文本：入库带回来，检索可直接取 | 03 |
| where | 元数据过滤条件（$eq/$gt/$in/$contains…） | 06 |
| where_document | 文档内容过滤（$contains/$regex…） | 06 |
| hnsw:space | 距离度量：l2（默认）/cosine/ip | 07 |
| EmbeddingFunction | 嵌入函数：默认 ONNX/OpenAI/自定义 | 04 |
| PersistentClient | 本地持久化客户端（SQLite 单进程独占） | 02/09 |
| HttpClient | 服务器模式客户端（生产多进程必用） | 02/09 |
| LangChainChroma | LangChain 向量库封装 | 08 |
| ChromaVectorStore | LlamaIndex 向量库封装 | 08 |

## 6. 常见误区

**误区一：Chroma 是"玩具库"**——1.x Rust 重写后服务器模式写入 **40K+ vectors/s、查询 p95 约 10ms**；**"1-2M 向量以内的生产 RAG 用它完全够，它缺的是分布式不是性能"**（01/09 篇）。

**误区二：默认嵌入模型直接上生产**——默认 all-MiniLM 是**英文为主的轻量模型**；中文/领域词汇/多语言场景必须换 OpenAI/HuggingFace 嵌入，**嵌入函数是集合的持久化配置，换模型必须重建集合**（04 篇）。

**误区三：距离度量不配置**——默认 **l2 是欧氏距离**，文本相似度检索标准是 **cosine**；而且**空间参数创建后不可改，只能克隆集合**——"建集合前先想好度量"（07 篇）。

**误区四：元数据随便塞**——1.x 元数据 **schema 强制**：同一字段混用字符串和整数会直接报错；字段类型要第一天定好（03 篇）。

**误区五：多进程共用 PersistentClient**——本地模式对数据目录**独占锁**，两个进程指向同路径会损坏索引；**Web 服务/多进程必须服务器模式 + HttpClient**（02/09 篇）。

**误区六：Docker 部署不挂卷**——1.0+ Rust 服务器**始终写盘**（默认 /data），不挂载卷**重启即清空**——"持久化卷是生产第一纪律"（09 篇）。

**误区七：老教程照搬 0.x API**——1.0 Rust 重写带来**破坏性变更**：内置鉴权移除、服务端配置改 config.yaml、数据路径从 /chroma/chroma 移到 /data——**"2026 教程配 1.5.x，锁版本是必须"**（09 篇）。

## 7. 一周学习计划示例

| 天 | 内容 | 动手任务 |
|:---:|------|---------|
| 1 | 01 + 02 | 装库，三种客户端各建一个 Collection，看五要素输出 |
| 2 | 03 + 04 | 设计商品/文档两种元数据 schema；对比默认 vs OpenAI 嵌入 |
| 3 | 05 + 06 | 批量入库 1000 条；写 where/where_document 组合过滤 |
| 4 | 07 | 建 cosine/l2 两个集合对比检索结果；调 ef_search |
| 5 | 08 + 09 | 用 LangChainChroma 接 RAG；服务器模式 + token 鉴权跑通 |
| 6-7 | 10 自测 + 面试 | 端到端 RAG 全链路，跑 20 题 |

## 8. 快速自测 10 题

1. Chroma 与 Milvus/Qdrant/FAISS/pgvector 的本质区别？"嵌入式"意味着什么？
2. 三种客户端（Ephemeral/Persistent/Http）各自适用什么场景？为什么多进程必须 Http？
3. Collection 五要素是什么？元数据 schema 强制是什么意思？
4. 默认嵌入模型是什么？换嵌入函数为什么必须重建集合？
5. where 与 where_document 的区别？$contains 在两种语境下的含义？
6. 默认距离度量是什么？为什么文本检索推荐 cosine？
7. HNSW 的 ef_search/construction_ef 是什么？为什么空间参数不可变？
8. 1.0 Rust 重写带来哪三个破坏性变更？性能收益是多少？
9. LangChainChroma 与裸 Chroma 的关系？LangChain 嵌入为什么要包装？
10. 2026-08 版本基线？Chroma 的规模边界（多少向量以内够用）？

## 9. 参考来源

- [chromadb GitHub 官方仓库（Release/源码）](https://github.com/chroma-core/chroma)
- [chromadb PyPI 页面（1.5.9 版本信息）](https://pypi.org/project/chromadb/)
- [PyPI 下载统计（184.8M 累计下载）](https://pepy.tech/projects/chromadb)
- [Chroma 官方文档（客户端/嵌入/过滤/集合配置）](https://docs.trychroma.com/)
- [Chroma Cookbook（官方示例库：核心/策略/集成）](https://cookbook.chromadb.dev/)
- [Chroma Changelog：GroupBy 与聚合（2026-01）](https://www.trychroma.com/changelog/groupby)
- [Chroma 1.0 迁移指南（Rust 重写破坏性变更）](https://docs.trychroma.com/docs/overview/migration)
- [Embedding Functions 官方文档（默认/OpenAI/自定义）](https://docs.trychroma.com/docs/embeddings/embedding-functions)
- [Where 过滤器官方参考（完整操作符语法）](https://docs.trychroma.com/reference/where-filter)

---

**下一模块**：[01-chromadb是什么.md](01-chromadb是什么.md)
