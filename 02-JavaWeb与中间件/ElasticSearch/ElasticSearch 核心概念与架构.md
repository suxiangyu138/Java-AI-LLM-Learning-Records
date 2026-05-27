# ElasticSearch 核心概念与架构（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | ES 基础概念速查
> **版本**：Elasticsearch 7.x/8.x
> **核心场景**：全文检索、日志分析、商品搜索、RAG 语义检索

---

## 一、ElasticSearch 是什么

- **ElasticSearch**：基于 Apache Lucene 的**分布式全文搜索引擎**
- 核心能力：海量数据的**近实时**存储、检索、聚合分析
- 开发语言：Java
- 数据格式：JSON（RESTful API）

### 1.1 ES 在企业架构中的位置

```
用户请求 -> Nginx -> SpringBoot 应用 -> MySQL（写）
                         ↓
                    ElasticSearch（读/搜索）
                         ↑
              Logstash/Canal 同步数据
```

---

## 二、核心概念 vs MySQL（必背对应关系）

| ElasticSearch | MySQL | 说明 |
|---|---|---|
| **Cluster（集群）** | 数据库集群 | 多节点组成，对外统一服务 |
| **Node（节点）** | 单个 MySQL 实例 | 集群中的一个 ES 实例 |
| **Index（索引）** | Database / Table | 逻辑命名空间，一组文档的集合 |
| **Document（文档）** | Row（行） | JSON 格式的一条数据 |
| **Field（字段）** | Column（列） | 文档中的键值对 |
| **Mapping（映射）** | Schema（表结构） | 定义字段类型、分词器等 |
| **Shard（分片）** | 分库分表的分片 | 索引拆分为多个分片，分布式存储 |
| **Replica（副本）** | 主从复制 | 分片的冗余备份，提升可用性 |

### 2.1 一个完整示例

```json
// MySQL 中的一条记录
// users 表：id=1, name="张三", age=25, desc="计算机专业学生"

// ES 中对应的 Document
{
  "_index": "user_index",
  "_type": "_doc",
  "_id": "1",
  "_source": {
    "id": 1,
    "name": "张三",
    "age": 25,
    "desc": "计算机专业学生"
  }
}
```

---

## 三、集群架构原理

### 3.1 Cluster（集群）

多个 Node 组成一个 Cluster，通过 `cluster.name` 标识同一个集群。集群具备：
- **自动发现**：新节点自动加入
- **自动分片分配**：节点上下线自动迁移分片
- **负载均衡**：请求自动路由到正确节点

### 3.2 Node（节点）

每个节点默认同时承担以下角色（生产建议分离）：

| 角色 | 配置 | 职责 |
|---|---|---|
| **Master** | `node.master: true` | 管理集群元数据、创建/删除索引、分配分片 |
| **Data** | `node.data: true` | 存储数据，执行 CRUD、搜索、聚合 |
| **Ingest** | `node.ingest: true` | 数据预处理（pipeline） |
| **Coordinating** | 所有三种都为 false | 请求路由，负载均衡（类似网关） |

### 3.3 Shard（分片）与 Replica（副本）

```
Index: user_index（3 个 Primary Shard + 1 个 Replica，共 6 个 Shard）

    Node-1          Node-2          Node-3
  ┌─────────┐    ┌─────────┐    ┌─────────┐
  │ P0  R1  │    │ P1  R2  │    │ P2  R0  │
  └─────────┘    └─────────┘    └─────────┘
  
  P = Primary Shard（主分片）
  R = Replica Shard（副本分片）
```

- **Primary Shard**：数据写入的主入口，数量在索引创建时确定（不可修改）
- **Replica Shard**：主分片的副本，可动态增加，提供故障转移 + 读负载均衡

核心公式：`节点数 >= 主分片数 * (副本数 + 1)`

---

## 四、数据写入与查询流程

### 4.1 写入流程

```
1. 客户端发送写请求到任意节点（Coordinating Node）
2. Coordinating Node 通过路由算法计算分片位置：
   shard = hash(document_id) % number_of_primary_shards
3. 请求转发到 Primary Shard 所在节点
4. Primary Shard 写入成功后，并行复制到 Replica Shard
5. 所有 Replica 确认后返回成功给客户端
```

### 4.2 查询流程

```
1. 客户端发送查询请求到 Coordinating Node
2. 转发到索引的所有分片（Primary + Replica）
3. 每个分片执行查询，返回结果
4. Coordinating Node 汇总排序后返回给客户端
```

### 4.3 路由公式（重要）

```
shard_num = hash(_routing) % num_primary_shards
```

- 默认 `_routing = _id`
- 自定义路由可让相关文档落在同一分片（如按用户ID路由）

---

## 五、Mapping 核心类型

| 类型 | 用途 | 说明 |
|---|---|---|
| `text` | 全文搜索（分词） | 会被分词器分析，支持 match 查询 |
| `keyword` | 精确匹配 | 不分词，用于过滤、排序、聚合 |
| `long / integer / short / byte` | 整数 | |
| `float / double` | 浮点数 | |
| `boolean` | 布尔值 | |
| `date` | 日期 | 支持多种格式 |
| `geo_point` | 地理位置 | 经纬度 |
| `dense_vector` | 向量 | 8.x 用于语义检索/向量搜索 |

---

## 六、近实时搜索原理

ES 不是真正的实时搜索，而是**近实时**（Near Real-Time，默认 1s）：

```
写入 -> Memory Buffer -> Refresh(1s) -> Segment(searchable)
                              ↓
                      Translog(持久化) -> Flush -> 磁盘
```

- **Refresh**：Memory Buffer 写入 Segment，数据可被搜索（默认 1s 一次）
- **Flush**：Segment 从缓存刷到磁盘（默认 30min 或 Translog 达到 512MB）
- **Translog**：写入前先记录 Translog（类似 MySQL binlog），防止数据丢失

---

## 七、面试核心要点

1. **ES 为什么快？** 倒排索引 + 内存缓存 + 分片并行搜索
2. **为什么主分片不可改？** 路由公式 `hash % num_shards`，改分片数会改变数据分布
3. **Refresh 和 Flush 区别？** Refresh（可搜索）vs Flush（持久化到磁盘）
4. **Coordinating Node 作用？** 请求路由、结果聚合，不存数据

---

## 八、极简总结

```
ES 核心 = 集群(Cluster) > 节点(Node) > 索引(Index) > 分片(Shard) > 文档(Document) > 字段(Field)
存 JSON，倒排索引查，分词搜，分片并行跑
```
