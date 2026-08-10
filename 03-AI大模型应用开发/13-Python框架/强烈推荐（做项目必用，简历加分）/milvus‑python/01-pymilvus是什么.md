# 01 - pymilvus 是什么

> 定位：Milvus 官方 Python SDK——RAG 项目向量检索的第一入口——SDK v2 重设计（MilvusClient 统一 API + 原生异步）后，"一个客户端统治全部操作"成为事实标准；2026-08 双线并存：3.0 主线 + 2.6 维护线

---

## 📚 目录

1. [官方 SDK 与生态第一入口](#1-官方-sdk-与生态第一入口)
2. [SDK v2 重设计：四个关键变化](#2-sdk-v2-重设计四个关键变化)
3. [2026 双线基线：3.0 与 2.6](#3-2026-双线基线30-与-26)
4. [ORM 旧 API 的弃用](#4-orm-旧-api-的弃用)
5. [SDK v2 与 v1 对比](#5-sdk-v2-与-v1-对比)
6. [生态位置与分工](#6-生态位置与分工)
7. [练习 5 题](#7-练习-5-题)

---

## 1. 官方 SDK 与生态第一入口

pymilvus 是 Milvus/Zilliz 官方维护的 Python 客户端（`pip install pymilvus`），**RAG、向量检索、AI 应用里"和 Milvus 说话"的标准姿势就是它**。为什么是"第一入口"：Milvus 支持 gRPC 与 RESTful 两种协议，RESTful 可以裸调 HTTP，但生产项目的向量检索（建集合、批量插入、检索过滤）几乎全部走官方 SDK——**协议细节、数据序列化、错误处理都被 SDK 封装好了，自己裸写 gRPC 是自找麻烦**。

它同时是生态集成层的"底座"：LangChain 的 `Milvus` 类、LlamaIndex 的 `MilvusVectorStore`、各种 RAG 框架的向量存储适配，底层全部调 pymilvus——**"框架的封装千变万化，底层只有一个 pymilvus；看懂裸 SDK，任何集成层都是一层窗户纸"**（00 篇分工）。

## 2. SDK v2 重设计：四个关键变化

v2.4+ 开始官方做了 SDK v2 重设计（跨 Python/Java/Go/Node.js 统一），四个变化彻底改变了写法：

**其一，MilvusClient 统一 API**——所有语言、所有操作收敛到一个客户端类：建库、建集合、索引、加载、插入、检索全是 `client.xxx()` 方法，方法命名和参数跨语言一致。**"一个客户端统治全部"是 SDK v2 的设计口号**，取代了 v1 的 ORM 对象体系（Collection/FieldSchema 等类）。

**其二，一步式建集合**——v1 要"定义 FieldSchema → 拼 CollectionSchema → 实例化 Collection → create_index → load"五步；v2 的 `create_collection()` 一次调用可完成 schema 定义 + 索引创建 + 加载，最简形态一行 `client.create_collection("docs", dimension=1024)`（04 篇详讲）。

**其三，原生异步**——v2.5.3 起提供 `AsyncMilvusClient`：与同步版同参数，`asyncio.gather` 并发插入/检索，取代 v1 的 Future/回调模式（09 篇详讲）。

**其四，schema cache**——SDK 首次拉取集合 schema 后本地缓存，后续操作免去重复校验，减少网络往返与服务端 CPU——高频率 insert/query 场景延迟和开销明显下降（09 篇性能）。

## 3. 2026 双线基线：3.0 与 2.6

2026-08 的 pymilvus 是**双线并存**状态，版本配对是第一个要记牢的事：

| 线 | 服务器 | SDK | 状态 |
|:---:|------|-----|------|
| 3.0 主线 | Milvus 3.0.0（2026-07-29 GA） | **PyMilvus 3.0.1** | 新特性全部在这条线 |
| 2.6 维护线 | Milvus 2.6.x（2.6.18） | **PyMilvus 2.6.17**（2026-07-17） | 维护 + 修复 |

**3.0 线的新能力**（需 3.0 服务器，2.6 服务器会报 unknown-field）：外部集合（数据留在对象存储）、集合快照（MVCC 时间旅行查询）、partial array upsert、查询聚合与 faceted search、空向量支持、实体 TTL、TEXT 字段、StructArray（2.6.3 引入的数组嵌套结构）。**2.6 线的新特性**：Array of structs（2.6.3）、auto_id 下自定义主键（2.6.3）、FieldOp 数组部分更新操作符（2.6.14）、bytes 向量类型修复（2.6 后段）。

**迁移注意**：3.0 有**破坏性变更**（ORM 移除、schema 操作变化、服务器端 Storage V3 "Loon" 与新版索引格式是单向 opt-in——开了不能回滚 2.6），真实项目已有"锁文件静默升级 2.6→3.0 把 CI 打爆"的案例——**升级读迁移说明、锁版本、先验证再切**（10 篇报错速查）。

**升级路径建议（2.6 → 3.0）**：官方给的稳妥顺序是四步——**第一步**：SDK 先行升级到 3.0 线并验证与 2.6 服务器的兼容（3.0 SDK 兼容 2.6 服务器的基础操作，先换客户端再换服务器风险最小）；**第二步**：服务器升级 3.0，保持默认配置（Storage V3 "Loon" 与新版索引格式**不开**）运行确认健康；**第三步**：功能与性能验证（schema 操作、检索结果与 2.6 一致）；**第四步**：确认稳定后再按需开启 Loon/新索引（开启前 `milvus-backup` 备份——格式 opt-in 是单向的，开弓没有回头箭）。**一句话：客户端先行、服务器跟随、格式 opt-in 最后且需备份**。

## 4. ORM 旧 API 的弃用

v1 时代的 ORM 写法（`from pymilvus import connections, Collection, FieldSchema, utility`）已进入弃用通道：**官方明确 v1 接口支持随 Milvus 3.0 发布结束，新功能只给 v2 的 MilvusClient**。映射关系：

| v1 ORM | v2 MilvusClient |
|--------|-----------------|
| `connections.connect("default", host=..., port=...)` | `MilvusClient(uri="http://host:port")` |
| `Collection("name")` 对象方法 | `client.search/insert/load_collection/...` |
| `FieldSchema(name, DataType.VARCHAR)` + CollectionSchema | `client.create_schema()` + `add_field()` |
| `utility.list_collections()` | `client.list_collections()` |
| 字符串字段类型 | DataType 枚举（`DataType.VARCHAR`） |

**"老教程（v1 ORM 时代）大量过时"是这份文档反复强调的判断**——网上 2023-2024 的 pymilvus 教程多数是 ORM 写法；**2026 学 pymilvus 只认 MilvusClient**。迁移小项目的实操：把 `connections.connect` 换成 `MilvusClient(uri=...)`，`Collection("x")` 换成 `client` 上的同名方法——大多数代码是机械替换（10 篇有速查表）。

## 5. SDK v2 与 v1 对比

| 维度 | v1（ORM） | v2（MilvusClient） |
|------|-----------|-------------------|
| 并发模型 | 线程/Future/回调 | 原生 async/await |
| 建集合 | 多步 ORM（schema→Collection→index→load） | 一次 `create_collection()` |
| Schema 校验 | 每次请求服务端校验 | 本地 schema cache |
| 跨语言一致性 | 各语言接口各异 | 统一 MilvusClient 设计 |
| 状态 | 已弃用，3.0 停止支持 | 唯一官方推荐 |
| 适用 | 存量老项目 | 新项目一律 |

**选型判断**：新项目无脑 v2；存量 ORM 项目按"非紧急但必迁"处理——**拖得越久，3.0 服务器上线的迁移成本越高**（ORM 在 3.0 SDK 里被移除，拖到那时是重写不是替换）。

## 6. 生态位置与分工

**与 Milvus 主体系的分工**：`08-向量数据库/Milvus/` 那套讲"数据库本身"——架构、部署、索引原理、数据模型、生态（向量检索原理、HNSW 内部机制、集群部署都在那套）；**本体系只讲"pymilvus 怎么写"**——每个 SDK 动作背后的数据库语义（如 load 到底加载了什么、索引参数传给谁）会点透，但原理深潜看主体系。**"SDK 是驾驶舱，数据库原理是发动机——本体系教你开车，主体系教你修车"**。

**与框架的分工**：LangChain/LlamaIndex 里 Milvus 是"集成件"（封装类），本体系是"裸 SDK 全控"——学习期裸姿势，项目期看需求（**要深度控制检索参数/要调试就裸 SDK，要快速搭建就用框架集成**）。

**生态周边**：`milvus_cli`（命令行工具，调试用）、Zilliz Cloud（云托管版，token 认证）、Milvus Lite（本地嵌入式，原型开发，02 篇）、Milvus MCP Server（官方 2026 年新件——基于 SDK 构建的 MCP 服务，让 AI Agent 通过 MCP 协议直接操作 Milvus：建集合、插入、检索全部可用自然语言/工具调用驱动——"**MCP 时代向量库的 Agent 接入层**"，与 06-MCP 协议体系呼应；它在 SDK 之上薄薄一层，读懂本体系就能读懂它的每个动作）。

**为什么它排"强烈推荐"**：向量检索是 2026 年 AI 应用的标配能力（RAG、Agent 记忆、语义搜索全都要它），Milvus 是向量数据库事实标准，pymilvus 是它唯一的官方 Python 入口——**"做项目必用、简历加分"的双重属性与 LangChain 同级**：不是"了解即可"的锦上添花，而是"做 RAG 项目绕不开"的必修课。

## 7. 练习 5 题

1. 为什么说 pymilvus 是"生态集成层的底座"？LangChain/LlamaIndex 与它的关系？
2. SDK v2 重设计的四个关键变化是什么？哪一个对写法影响最大？
3. 2026-08 的版本配对是什么？为什么 2.6 服务器不能直接用 3.0 SDK？
4. ORM 旧 API 为什么必须迁移？v1→v2 的三行核心映射是什么？
5. 本体系与 Milvus 主体系的分工边界在哪里？索引原理去哪学？

> 🎯 **核心要点**：pymilvus = Milvus 官方 Python SDK + SDK v2 统一设计（**MilvusClient 一个客户端统治全部 + 一步建集合 + 原生异步 + schema cache**）；2026-08 双线并存（3.0 主线配 3.0.1 / 2.6 维护线 2.6.17），ORM 旧 API 已弃用——**"2026 学 pymilvus 只认 MilvusClient，老教程过时"**。

---

**下一模块**：[02-安装与连接.md](02-安装与连接.md) / **返回总览**：[00-milvus‑python总览.md](00-milvus‑python总览.md)
