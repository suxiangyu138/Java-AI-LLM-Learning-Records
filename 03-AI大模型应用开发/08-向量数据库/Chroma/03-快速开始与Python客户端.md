# 快速开始与 Python 客户端

> 从 `pip install` 到完整 CRUD 的最小闭环——四种客户端形态、写操作语义、读操作语义、批量与并发红线，一篇全部打通

## 1. 安装与四种客户端形态

```bash
pip install chromadb
python -c "import chromadb; print(chromadb.__version__)"   # 安装验证，应输出 1.5.x
```

chromadb 包内置 Python 客户端，且 1.x 起核心为 Rust（经 PyO3 FFI 嵌入 Python 进程），**无需额外装任何依赖即可本地持久化**。注意安装的是 Rust 核心的预编译轮子（wheel），首次安装体积较大属正常；Linux 环境若装的是 0.x 旧版，升级 1.x 前先看 01 篇的存储迁移说明（旧数据格式不兼容）。客户端有四种形态，按生命周期选择：

| 形态 | 构造方式 | 数据落盘 | 适用场景 |
|------|---------|:-------:|---------|
| EphemeralClient | `chromadb.EphemeralClient()` | 否 | 测试、教学 |
| PersistentClient | `chromadb.PersistentClient(path="./chroma")` | 是 | 单机应用、原型（**默认选择**） |
| HttpClient | `chromadb.HttpClient(host, port)` | 服务端决定 | 多进程/多语言、生产 |
| CloudClient | `chromadb.CloudClient(tenant, database, api_key)` | 云端 | 托管 |

1.x 的持久化是**自动的**——`PersistentClient` 每次写操作立即写盘，**没有 `client.persist()` 手动调用**（0.3.x 时代的方法，已废弃）。重启后用同一个 path 重新构造客户端，数据自然恢复：

```python
# Server 模式客户端：多进程/多语言场景
client = chromadb.HttpClient(
    host="192.168.1.10",
    port=8000,
    # 1.x 内置认证已失效，认证交给网络层（见 07）——客户端无需 auth 配置
)
```

HttpClient 与 Embedded 客户端 API 完全一致，区别只在连接层：HttpClient 支持连接池复用与超时配置，适合服务端常驻调用；**连接不到服务时抛连接异常而不是静默降级**，调用方要做重试与降级处理（短暂故障可重试，持续不可达应触发服务降级而非堆积重试）。

```python
import chromadb

client = chromadb.PersistentClient(path="./chroma_data")
collection = client.get_or_create_collection(
    name="documents",
    metadata={"hnsw:space": "cosine"},   # 距离度量，创建后不可变
)
```

## 2. 写操作：add / upsert / update / delete

写操作的参数都是"列式"列表——ids、embeddings、metadatas、documents 四个列表长度必须一致（embeddings 与 documents 可二选一）。

| 方法 | 语义 | 重复 ID 行为 | 推荐度 |
|------|------|-------------|:------:|
| `add` | 纯插入 | 抛 `UniqueConstraintError` | 首次导入 |
| `upsert` | 存在则全量覆盖，不存在则插入 | 静默覆盖 | **增量/幂等首选** |
| `update` | 仅更新已存在记录，支持部分字段 | 不存在则静默跳过 | 改 metadata |
| `delete` | 按 ids 或 where 条件删除（1.5.3+ 支持 limit） | — | 清理 |

```python
collection.add(
    ids=["doc1", "doc2"],
    documents=["Java 虚拟机内存模型", "Redis 缓存淘汰策略"],
    metadatas=[{"topic": "jvm", "year": 2024}, {"topic": "cache", "year": 2026}],
)

collection.upsert(
    ids=["doc2"],
    documents=["Redis 8 缓存淘汰策略（更新版）"],
    metadatas=[{"topic": "cache", "year": 2026}],
)
```

三条语义要点：一是 `update` 只改 documents 时，客户端会自动用 embedding 函数重算向量；二是**并发写多进程不可靠**——`PersistentClient` 的本地索引非线程安全，并发 upsert 可能永久损坏库（`sqlite3.OperationalError: database is locked` 即信号），多写者必须上 HttpClient 服务模式；三是**批量 50-250 条/批**对 HNSW 建索引最友好，超大批次拆开写。

## 3. 读操作：query 与 get

`query` 做相似度检索，`get` 做精确过滤检索，两者都支持 where 过滤与 include 控制返回字段：

```python
# 相似搜索：默认返回 documents + metadatas + distances
results = collection.query(
    query_texts=["JVM 是什么"],
    n_results=5,                                   # 默认 10，超过集合总量会报错
    where={"topic": {"$eq": "jvm"}},               # 元数据过滤（后置）
    where_document={"$contains": "内存"},          # 文档内容过滤
)
# results 是嵌套结构：ids[0] 对应第一个 query_texts 的结果

# 精确检索：按 ID 或条件拉取，扁平列主序
items = collection.get(
    ids=["doc1"],
    where={"year": {"$gte": 2025}},
    limit=10, offset=0,
)
```

两个结果形态务必记牢：**query 返回按输入查询分组的嵌套列表**（`results["ids"][0]` 是第一条查询的结果数组），**get 返回扁平列表**（ids/documents/metadatas 同位索引属于同一条记录）。`include` 参数控制返回哪些字段（`["documents","metadatas","embeddings","distances","uris"]`），默认 query 带 documents+metadatas+distances，get 带 documents+metadatas——**只取需要的字段能显著减小响应体**。

## 4. 辅助 API：count / peek / 集合管理

- `collection.count()`：返回集合内记录总数——钳制 `n_results` 的必备前置，也是监控数据量的入口。
- `collection.peek(limit=5)`：不加过滤地取前几条记录——排查数据是否写入、检查字段形态的最快手段。
- `client.list_collections()` / `client.get_collection(name)` / `client.delete_collection(name)`：集合生命周期管理；`get_collection` 与 `get_or_create_collection` 的区别是前者**不存在即抛错**，导入流程用 `get_or_create` 保证幂等。
- `collection.modify(name=..., metadata=...)`：改名与改元数据——注意只能改这两样，embedding 函数与索引配置不可改。
- `collection.delete(ids=[...])` 或 `delete(where={...})`：按 ID 或条件删；1.5.3+ 支持 `limit` 参数分批删，防大范围误删与长事务。

Server 模式还暴露 REST 端点（`/api/v1/collections` 等），非 Python 语言可以直接调 HTTP——Spring AI 的 `ChromaApi` 就是这么实现的（见 08），Java 生态无需依赖 Python 客户端。

## 5. 检索结果取数防御

- `n_results` 超过 collection 总量会抛错——先 `k = min(k, collection.count())` 钳制。
- 后置过滤可能让结果**少于 k 条**甚至为空——取数前判空：`results["ids"] and results["ids"][0]`。
- 调试过滤条件时先跑 `get(where=..., include=[])` 看命中的 ID 集合，再上 query 排序。

## 6. 客户端工程习惯与错误处理

异常类型是排查的第一线索，熟记五种：

| 异常 | 触发场景 | 对策 |
|------|---------|------|
| `UniqueConstraintError` | add 重复 ID | 换 upsert 或先 get 查重 |
| 维度不匹配错误 | embedding 与集合维度不一致 | 检查模型一致性 |
| `InvalidCollectionNameError` | 非法集合名 | 检查命名规则 |
| `n_results` 超量报错 | query 的 k > 集合总数 | 先 count() 钳制 |
| `database is locked` | 多进程并发写本地库 | 换 Server 模式 |

工程习惯三条：**写操作幂等化**——增量入库一律 `upsert`，用业务 ID 做去重键，重跑任务不报错；**批量写入限速**——50-250 条/批 + 批间小间隔，兼顾 HNSW 建图质量与负载；**读多写少场景开持久客户端复用**——`PersistentClient` 与 `HttpClient` 都是重量级对象，应用生命周期内创建一次复用，不要在每次请求里重建（索引加载与连接建立开销都在构造时）。线程模型补一句：**单进程内多线程读是安全的**（索引只读），不安全的是多进程并发写——同一进程内单写者 + 多读者是最稳的组合。

## 7. Embedding 函数与一致性铁律

不显式指定 embedding 函数时，chromadb 用内置默认模型（0.5+ 为 nomic-embed-text-v1.5）并**发起网络请求**生成向量——本地实验要么配置好模型下载，要么直接传 embeddings 绕开。内网/离线环境的三条路径：**本地模型**（如 BGE、text2vec 通过 ONNX/transformers 加载，封装成自定义 embedding 函数传入 collection）；**局域网模型服务**（Ollama/vLLM 提供 OpenAI 兼容接口，客户端对接）；**纯向量模式**（应用自己算好 embeddings 直接传，Chroma 只当存储——生产最常用，模型选择权完全在应用）。

铁律有三条：**写读必须用同一个 embedding 函数**（换模型 = 换空间，检索毫无意义）；**把模型名写进 collection metadata** 留痕；**embedding 维度与 space 创建后不可变**，换模型就建新集合（用 forking 对比效果，见 06）。

> 🎯 **核心要点**：`PersistentClient(path)` 一条命令获得本地持久化向量库；写操作记"add 抛错、upsert 覆盖、update 部分、delete 按条件"，读操作记"query 嵌套、get 扁平、后置过滤、include 控制返回"；并发写必须逃到服务模式。

---

**参考来源**：

- [Chroma 官方文档 - Quickstart](https://docs.trychroma.com/)
- [Chroma Persistent 存储指南](https://ossaihub.com/code/chroma-quickstart-persistent/)
- [ChromaDB 持久化与迁移问题解决记录](https://journal.matuteiglesias.link/Dev/2025-11-20_resolved-chromadb-client-configuration-and-migration-issues_de31795564cd)
- [并发 upsert 损坏库 Issue #1584](https://github.com/chroma-core/chroma/issues/1584)

---

**下一模块**：[04-存储架构与检索原理](./04-存储架构与检索原理.md) / **返回总览**：[00-Chroma知识体系总览](./00-Chroma知识体系总览.md)
