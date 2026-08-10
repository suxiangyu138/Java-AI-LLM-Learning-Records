# 00 - milvus‑python（PyMilvus）总览

> 强烈推荐：Milvus Python SDK（pymilvus）——做项目必用、简历加分——"MilvusClient 一个客户端统治全部操作，一行建集合、原生异步、RAG 项目向量检索的第一入口——2026 年向量数据库 Python 客户端事实标准"

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
milvus‑python（本体系 11 篇——强烈推荐）
├── 定位层：01 pymilvus 是什么（SDK v2 重设计/3.0 双线现状）
├── 核心层：02 安装与连接（MilvusClient/Lite/uri/token）
│          03 Schema 设计与数据模型（DataType/字段属性）
│          04 集合管理（create_collection/index+load 一步）
│          05 数据写入与更新（insert/upsert/delete）
├── 检索层：06 向量索引（索引类型选择/参数）
│          07 向量搜索与过滤（search/hybrid_search/Ranker）
│          08 查询与标量过滤（query/表达式/分页迭代器）
├── 进阶层：09 异步与性能（AsyncMilvusClient/并发调优）
└── 验收层：10 生产实战与自测（裸 RAG 全链路 + 20 题）
```

## 2. 模块导航

| 篇 | 模块 | 核心内容 | 核心产出 |
|:---:|------|---------|---------|
| 00 | 总览 | 导图/分工/路线/速查/误区 | 学习计划 |
| 01 | pymilvus 是什么 | SDK v2 重设计/2026 双线基线 | 认知 |
| 02 | 安装与连接 | MilvusClient/Lite/uri/token | 会连库 |
| 03 | Schema 设计 | DataType/字段属性/设计规范 | 会建模 |
| 04 | 集合管理 | 建集合/加载释放/分区 | 会管理 |
| 05 | 数据写入 | insert/upsert/delete/批量 | 会写数 |
| 06 | 向量索引 | 索引类型/参数/选型 | 会建索引 |
| 07 | 向量搜索 | search/过滤/hybrid_search | 会检索 |
| 08 | 查询过滤 | query/表达式/迭代器 | 会精确查 |
| 09 | 异步与性能 | AsyncMilvusClient/并发调优 | 会并发 |
| 10 | 实战与自测 | 裸 RAG 全链路 + 20 题 | 毕业产出 |

## 3. 与主体系的分工

**与 Milvus 主体系（`../../../08-向量数据库/Milvus/00-Milvus知识体系总览.md`）的分工**：那套体系讲"数据库本身"（架构、部署、索引原理、数据模型、生态）——**"原理课 vs 工具课：数据库长什么样看那套，Python 代码怎么写看本套——两套配合构成'懂 Milvus + 会写 pymilvus'的完整能力"**。

**与向量数据库体系（`../../../08-向量数据库/向量数据库/00-向量数据库知识体系总览.md`）的分工**：那套是"向量数据库全景选型"（索引原理、Chroma/Milvus/FAISS 对比、2026 选型）——**"选型看那套，选定 Milvus 后用本套动手"**；与 Chroma（`../../../08-向量数据库/Chroma/`）同级的 pymilvus 就是 Chroma 客户端对应的 Milvus 版。

**与 LangChain（`../LangChain/00-LangChain总览.md`）、LlamaIndex（`../llama‑index/00-LlamaIndex总览.md`）的分工**：那两套框架里向量库是"集成件"（`Milvus` 类封装）；本体系是"**直接调 SDK 的裸姿势**"——**"框架帮你包好 vs 自己全控：学习期先用裸 SDK 懂原理，项目期再上框架集成——裸 SDK 是理解一切集成层的地基"**。

**与 Python 异步 + FastAPI（`../../../01-Python语言/Python%20异步%20+%20FastAPI/00-Python异步与FastAPI知识体系总览.md`）的分工**：异步服务里接 Milvus 用 `AsyncMilvusClient` + asyncio.gather（09 篇），与那个体系的 asyncio 知识配套。

**2026-08 基线**：PyMilvus 双线并存——**3.0 主线**：Milvus 3.0.0 服务器 2026-07-29 GA，配对 **PyMilvus 3.0.1**（3.0 线协议、ORM 弃用、新能力：外部集合/快照/TEXT/StructArray）；**2.6 维护线**：PyMilvus 2.6.x 至 **2.6.17**（2026-07-17，2.x 最后批次），新特性如 Array of structs（2.6.3）、auto_id 自定义主键（2.6.3）、FieldOp 数组部分更新（2.6.14）；**关键认知**：SDK v2 重设计（v2.4+）确立 MilvusClient 统一 API，旧 ORM 接口（connections/Collection）已弃用、随 Milvus 3.0 停止支持——**"老教程的 Collection() 写法已过时，一律以 MilvusClient 为准"**。

## 4. 学习路线推荐

**路线一：标准路线（3-5 天）**——01 → 10 逐篇 + 每篇跑代码——**毕业标准：用 pymilvus 独立实现一个"建集合 → 批量插入 → 索引 → 检索 + 过滤"的裸 RAG 链路**。

**路线二：速成路线（1-2 天）**——01 → 02 → 03 → 07 → 10（跳过 04/05/06/08/09 精读）——适合已有向量库经验、只想快速上手 pymilvus 的人。

**路线三：项目驱动路线**——RAG 项目遇到问题按需查篇——**"MilvusClient 半小时上手，难点全在生产细节（schema 设计/索引选型/并发性能）——遇到再查"**。

## 5. 核心概念速查

| 概念 | 一句话 | 对应篇 |
|------|--------|:---:|
| MilvusClient | SDK v2 统一客户端，全部操作入口 | 02 |
| AsyncMilvusClient | 原生异步客户端（v2.5.3+） | 09 |
| Milvus Lite | 本地文件版 Milvus（原型开发） | 02 |
| Schema | 字段定义集合（DataType/属性） | 03 |
| Collection | 集合 = 数据表（向量 + 标量字段） | 04 |
| insert/upsert | 写入/按主键覆盖写入 | 05 |
| auto_id | 主键自动生成（2.6.3 起可自定义） | 05 |
| 索引 | FLAT/HNSW/IVF/DISKANN/GPU 系 | 06 |
| search | 向量相似度检索（top_k/过滤） | 07 |
| hybrid_search | 多路召回 + Ranker 融合 | 07 |
| query | 标量精确查询（表达式过滤） | 08 |
| 过滤器 | search 里的 filter 表达式 | 07/08 |
| Ranker | 混合检索排序融合器 | 07 |
| schema cache | SDK 本地缓存集合 schema | 09 |

## 6. 常见误区

**误区一：照着老教程写 `Collection()` ORM 代码**——旧 ORM 接口（connections.connect/Collection/utility）已弃用，随 Milvus 3.0 停止支持——**新代码一律 MilvusClient，老代码尽快迁移**（01 篇）。

**误区二：客户端版本随便装**——**2.6.x 服务器配 2.6.x SDK、3.0 服务器配 3.0.1+ SDK**，版本错配会出现协议错误或字段不可用；且 3.0 有破坏性变更（ORM 移除），**升级必须先读迁移说明**（01/02 篇）。

**误区三：Schema 随便设计，后面改**——Schema 基本不可变（3.0 只支持加/删可空标量字段），**主键、向量维度、字段类型建了就不能改**——先设计后建库（03 篇）。

**误区四：search 不需要索引/不 load 就能搜**——**搜索前必须建索引并 `load_collection`**，否则报错或全表扫描（04/06 篇）。

**误区五：向量搜索不能加标量过滤**——search 的 filter 参数支持精确过滤、JSON 字段过滤、exists 等——**"先过滤再召回"是生产标配**（07 篇）。

**误区六：同步写法打天下**——异步服务里用同步客户端会阻塞事件循环；**FastAPI 等异步项目用 AsyncMilvusClient + asyncio.gather 是标准姿势**（09 篇）。

**误区七：本体系代替 Milvus 主体系**——SDK 用法 ≠ 数据库原理；**索引原理、架构、部署调优要看主体系**，本体系默认你会用 Milvus 但讲清楚每个 SDK 动作背后的数据库语义（01 篇分工）。

## 7. 一周学习计划示例

| 天 | 内容 | 动手任务 |
|:---:|------|---------|
| 1 | 01 + 02 | 装 pymilvus，Milvus Lite 本地跑通连接 |
| 2 | 03 + 04 | 设计商品向量 Schema，建集合 + 加载 |
| 3 | 05 + 06 | 批量插入 1 万条，建 HNSW 索引 |
| 4 | 07 | search + filter 过滤 + 稀疏向量检索 |
| 5 | 08 + 09 | query 表达式查询，AsyncMilvusClient 并发插入 |
| 6-7 | 10 自测 + 面试 | 裸 RAG 全链路，跑 20 题，写简历 |

## 8. 快速自测 10 题

1. MilvusClient 相比旧 ORM 接口的核心变化是什么？为什么说老教程过时？
2. 连接 Milvus 的三种形态（Lite/自部署/云）各自怎么配？
3. Schema 的 DataType 有哪些常用类型？哪些字段属性必须建库前定死？
4. create_collection 一步式（dimension）和完整 Schema 版有什么区别？
5. insert 与 upsert 的语义差异？auto_id 与自定义主键怎么搭配？
6. HNSW 的关键参数有哪些？数据量级如何决定索引选型？
7. search 的 filter 参数能做什么？"先过滤再召回"指什么？
8. hybrid_search 与 Ranker 解决什么问题？
9. query 和 search 的分工边界是什么？
10. AsyncMilvusClient 什么时候必须用？同步客户端在异步服务里有什么问题？

## 9. 参考来源

- [PyMilvus GitHub 官方仓库（Release/CHANGELOG）](https://github.com/milvus-io/pymilvus)
- [Milvus SDK v2 官方博客：原生异步与统一 API](https://milvus.io/blog/introducing-milvus-sdk-v2-native-async-support-unified-apis-and-superior-performance.md)
- [PyMilvus v3.0.x API 参考（官方文档）](https://blog.milvus.io/api-reference/pymilvus/v3.0.x/About.md)
- [Milvus 3.0.0 服务器 Release（2026-07-29，SDK 配对表）](https://github.com/milvus-io/milvus/releases/tag/v3.0.0)
- [PyMilvus v2.6.14 Release Notes（FieldOp 等新特性）](https://github.com/milvus-io/pymilvus/releases/tag/v2.6.14)
- [PyMilvus 包版本历史（Snyk 注册表）](https://security.snyk.io/package/pip/pymilvus/versions)
- [Zilliz 开发者中心：Python SDK Reference](https://docs.zilliz.com/reference/python)

---

**下一模块**：[01-pymilvus是什么.md](01-pymilvus是什么.md)
