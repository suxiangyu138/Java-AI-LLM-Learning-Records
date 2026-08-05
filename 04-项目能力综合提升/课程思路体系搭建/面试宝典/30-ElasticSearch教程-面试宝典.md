# Elasticsearch 面试宝典（基础篇）
> 基于课程大纲全面覆盖面试高频考点 — 从基础概念到进阶实践，适用于 1-5 年 Java 后端/大数据工程师面试准备。

## 目录
1. [一、基础概念速答（20题）](#一基础概念速答20题)
2. [二、深度原理剖析（12题）](#二深度原理剖析12题)
3. [三、实战场景题（10题）](#三实战场景题10题)
4. [四、手写代码/配置文件题（6题）](#四手写代码配置文件题6题)
5. [五、系统设计题（4题）](#五系统设计题4题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板（Top 5）](#七面试回答模板top-5)
8. [八、快速查漏补缺Checklist](#八快速查漏补缺checklist)

---

## 一、基础概念速答（20题）

### Q1 Elasticsearch 是什么？底层基于什么技术？
> Elasticsearch 是一个基于 **Apache Lucene** 的分布式全文检索引擎，提供 RESTful API，使用 Java 开发。

| 项目 | 说明 |
|------|------|
| 底层 | Apache Lucene |
| 语言 | Java |
| 协议 | RESTful JSON over HTTP |
| 默认端口 | 9200 (HTTP) / 9300 (Transport, ES7-) |

### Q2 什么是倒排索引（Inverted Index）？
倒排索引是 ES 的底层核心数据结构，将文档中的"词"映射到"文档ID"，实现快速全文搜索。

| 对比 | 正排索引 | 倒排索引 |
|------|----------|----------|
| 结构 | 文档 → 词语 | 词语 → 文档 |
| 场景 | 精准查询（如 MySQL 的 id 索引） | 全文检索（如百度搜索） |
| 速度 | 查询固定字段快 | 搜索词语极快 |

```text
倒排表结构示例：
"java" → doc1, doc3, doc5
"elasticsearch" → doc1, doc4
"面试" → doc2, doc3, doc5
```

### Q3 ES 集群的核心角色有哪些？
| 节点角色 | 职责 | 配置项 |
|----------|------|--------|
| **Master** | 集群管理、索引创建删除、分片分配 | `node.roles: [master]` |
| **Data** | 数据存储、CRUD、搜索聚合 | `node.roles: [data]` |
| **Coordinating** | 请求路由、结果聚合 | 无 data/master/ingest 角色 |
| **Ingest** | 数据预处理管道 | `node.roles: [ingest]` |

### Q4 什么是 Index、Type、Document、Field？
| 术语 | 对应关系型数据库 | 说明 |
|------|------------------|------|
| **Index** | Database | 索引，是文档的逻辑容器 |
| **Type** | Table | **ES 7.x 已废弃**，一个 Index 只有一个 `_doc` |
| **Document** | Row | JSON 文档，数据的最小单位 |
| **Field** | Column | 文档中的字段，有对应类型和 Mapping |

### Q5 什么是 Shard 和 Replica？
- **Primary Shard（主分片）**：数据拆分的基础单元，索引创建后不可修改数量。
- **Replica Shard（副本分片）**：主分片的拷贝，用于容灾和分担读压力。

> 💡 **默认配置**：`number_of_shards: 1`（ES 7.x+）/ `number_of_replicas: 1`

### Q6 ES 为什么是"近实时"（Near Real-Time）搜索？
ES 写入流程：
```
写入 → 写 translog → 内存 buffer → refresh（每秒）→ segment 可被搜索 → flush → 磁盘持久化
```
> ⚠️ **关键**：`refresh_interval` 默认 1 秒，因此在写入后最多 1 秒才能被搜索到，这就是"近实时"的含义。

### Q7 text 和 keyword 的区别是什么？
| 类型 | 是否分词 | 适用场景 | 能否排序/聚合 |
|------|----------|----------|--------------|
| **text** | 是（经过 analyzer 分词） | 全文搜索、文章内容 | ❌ 不能直接排序/聚合 |
| **keyword** | 否（当作一个整体） | 精确匹配、标签、枚举值 | ✅ 可以排序和聚合 |

> 💡 如果需要同时支持两种查询，可以使用 **multi-fields**：
```json
{
  "title": {
    "type": "text",
    "fields": {
      "keyword": { "type": "keyword" }
    }
  }
}
```

### Q8 什么是 Mapping？动态映射 vs 显式映射？
| 映射方式 | 说明 | 优缺点 |
|----------|------|--------|
| **动态映射** | ES 自动推断字段类型 | 快速上手，但类型推断可能不准确 |
| **显式映射** | 手动定义字段类型和参数 | 精确控制，生产环境推荐 |

```json
PUT /my_index
{
  "mappings": {
    "properties": {
      "title": { "type": "text", "analyzer": "ik_max_word" },
      "price": { "type": "double" },
      "createTime": { "type": "date", "format": "yyyy-MM-dd HH:mm:ss" }
    }
  }
}
```

### Q9 什么是 Analyzer（分词器）？它的三个组件是什么？
分词器将文本拆分为词项（Term），由三部分组成：

| 组件 | 作用 | 示例 |
|------|------|------|
| **Character Filter** | 字符过滤（HTML 标签去除等） | HTML Strip |
| **Tokenizer** | 分词器（按规则切词） | standard, ik_max_word |
| **Token Filter** | 词项过滤（小写、停用词、同义词） | lowercase, synonym |

### Q10 IK 分词器两种模式的区别？
| 模式 | 粒度 | 示例："中华人民共和国" |
|------|------|----------------------|
| `ik_max_word` | 最细粒度 | 中华人民共和国 / 中华人民 / 中华 / 华人 / 人民共和国 / 人民 |
| `ik_smart` | 智能粒度 | 中华人民共和国 |

> 💡 索引时用 `ik_max_word`（尽可能多的词），搜索时用 `ik_smart`（更精准）。

### Q11 ES 中如何创建、查询、更新、删除文档？
| 操作 | Method | Endpoint | 说明 |
|------|--------|----------|------|
| 新增/全量替换 | PUT | `/index/_doc/id` | id 存在则覆盖 |
| 新增（自动生成 id） | POST | `/index/_doc` | 返回自动生成 id |
| 查询 | GET | `/index/_doc/id` | 精准查询 |
| 更新部分字段 | POST | `/index/_update/id` | 只更新指定字段 |
| 删除 | DELETE | `/index/_doc/id` | 标记删除 |

### Q12 match 和 term 查询的区别？
| 查询 | 类型 | 是否分词 | 适用场景 |
|------|------|----------|----------|
| **match** | 全文查询 | 是 | 对 text 字段做全文搜索 |
| **term** | 精确查询 | 否 | 对 keyword 字段做精确匹配 |

> ⚠️ **常见错误**：对 text 字段使用 term 查询，因为 text 字段被分词，term 匹配不到完整的原始字符串。

### Q13 bool 查询的四种子句是什么？
| 子句 | 含义 | 评分影响 |
|------|------|----------|
| **must** | 必须匹配（AND 逻辑） | ✅ 贡献评分 |
| **filter** | 必须匹配（AND 逻辑） | ❌ 不贡献评分，有缓存 |
| **should** | 应该匹配（OR 逻辑） | ✅ 贡献评分 |
| **must_not** | 不能匹配（NOT 逻辑） | ❌ 不贡献评分 |

```json
{
  "query": {
    "bool": {
      "must": [{ "match": { "title": "java" } }],
      "filter": [{ "term": { "status": "published" } }],
      "should": [{ "match": { "content": "spring" } }],
      "must_not": [{ "term": { "deleted": true } }]
    }
  }
}
```

### Q14 ES 的聚合（Aggregation）分哪几类？
| 聚合类型 | 说明 | 示例 |
|----------|------|------|
| **Bucket（桶聚合）** | 分组统计 | terms, date_histogram, range |
| **Metric（指标聚合）** | 数值计算 | avg, sum, max, min, cardinality |
| **Pipeline（管道聚合）** | 基于聚合结果再计算 | avg_bucket, cumulative_sum |

```json
// 统计各分类的商品数量 + 平均价格
{
  "size": 0,
  "aggs": {
    "by_category": {
      "terms": { "field": "category.keyword" },
      "aggs": {
        "avg_price": { "avg": { "field": "price" } }
      }
    }
  }
}
```

### Q15 ES 的分页方式有哪几种？分别适用于什么场景？
| 分页方式 | 原理 | 适用场景 | 限制 |
|----------|------|----------|------|
| **from + size** | 截取结果 | 浅分页（前 10000 条） | 深度分页导致 OOM |
| **scroll** | 生成快照游标 | 全量导出、批量处理 | 有状态，不适合实时查询 |
| **search_after** | 基于排序值游标 | 深分页滚动 | 需要排序，不支持随机跳页 |

> ⚠️ **面试高频**：为什么 `from + size` 不适合深分页？因为 ES 需要在每个分片查询 `from + size` 条，然后在协调节点汇总排序，深度分页会导致巨大的内存消耗。

### Q16 什么是 Highlighing（高亮）？
> 在搜索结果中标记匹配的关键词片段。

```json
{
  "query": { "match": { "content": "elasticsearch" } },
  "highlight": {
    "fields": { "content": {} },
    "pre_tags": ["<em>"],
    "post_tags": ["</em>"]
  }
}
```

### Q17 Bulk API 是什么？有什么优势？
Bulk API 允许一次性批量发送多个写入/更新/删除操作，减少网络开销。

```json
POST /_bulk
{ "index": { "_index": "products", "_id": "1" } }
{ "title": "Java 编程思想", "price": 79 }
{ "index": { "_index": "products", "_id": "2" } }
{ "title": "Spring 实战", "price": 59 }
```

> 💡 **最佳实践**：单次 Bulk 建议 5-15 MB，5000-10000 条文档。

### Q18 什么是 refresh、flush、merge、translog？
| 机制 | 触发时机 | 作用 |
|------|----------|------|
| **refresh** | 默认每秒 1 次 | 内存 buffer → segment（可搜索） |
| **flush** | 默认 30 分钟或 translog 满 | segment 落盘 + translog 清空 |
| **merge** | 后台持续进行 | 小 segment 合并成大 segment，清理已删文档 |
| **translog** | 每次写入 | 事务日志，防止数据丢失 |

### Q19 match_phrase 与 match 的区别？
| 查询 | 匹配要求 | 示例："java elasticsearch" |
|------|----------|-----------------------------|
| match | 任意词匹配即可 | 包含 java 或 elasticsearch 都返回 |
| match_phrase | 短语精确位置匹配 | 必须连续包含 "java elasticsearch" |

### Q20 multi_match 有什么用？
> 同时在多个字段上进行 match 查询。

```json
{
  "query": {
    "multi_match": {
      "query": "java elasticsearch",
      "fields": ["title^3", "content", "tags"]
    }
  }
}
```
> 💡 `^3` 表示 title 字段的权重是其他字段的 3 倍。

---

## 二、深度原理剖析（12题）

### Q21 请详细描述 ES 的写入流程。
```
Client → Coordinating Node（计算路由）
         → Primary Shard
             → 写入 translog（磁盘）
             → 写入内存 buffer
             → 同步到 Replica Shard
                 → Replica 写入 translog + buffer
                 → 返回 ACK 给协调节点
             → 返回成功给 Client
（异步）→ refresh（每秒）→ segment 可搜索
（异步）→ flush → segment 落盘 + translog 清空
（后台）→ merge → 合并小 segment
```

### Q22 路由（Routing）是如何计算的？
```
shard = hash(routing) % number_of_primary_shards
```
- 默认 routing = `_id`
- 可以自定义 routing 实现相同用户的数据到同一分片
> ⚠️ **主分片数在索引创建后不可修改**，否则路由公式会变化。

### Q23 什么是 Segment？为什么说它是"不可变的"？
- **Segment** 是 Lucene 中存储倒排索引的最小单位，类似一个"小文件"。
- **不可变性**：Segment 写入后不会被修改，更新文档是通过"标记删除 + 新增文档"实现。
- **优势**：缓存友好、无需锁、读性能高。
- **劣势**：需要 merge 去清理已删文档。

### Q24 ES 如何保证单文档的原子性？
> ES 对单文档的 **CRUD 操作是原子性的**（基于 _primary 和 _seqNo），但**不支持跨文档事务**。

- 每次操作写 translog 到磁盘（通过 `fsync`），确保宕机后能恢复。
- `wait_for_active_shards` 参数可控制写入前至少有多少副本确认。

### Q25 segment merge 的过程是怎样的？
```
小 segment A, B, C → merge 线程 → 新大 segment D → 提交 → 删除 A, B, C
```
> 💡 merge 时占用 IO 和 CPU，ES 通过 `_regulations` 自动限速。`forcemerge` 可强制合并已不再写入的索引（如日志索引归档前）。

### Q26 text 字段为什么不能直接排序？
- text 字段经过分词后产生多个词项，无法确定按哪个词排序。
- text 字段默认不启用 `doc_values`（doc_values 只能用于 keyword、数值、日期类型）。
> 如果需要对 text 排序，需要使用 `.keyword` 子字段（multi-field）。

### Q27 ES 的评分机制是什么？
ES 5.x 之前使用 **TF-IDF**，之后使用 **BM25** 作为默认相似度算法。

```
BM25 公式（简化）：
Score = IDF * (tf * (k1 + 1)) / (tf + k1 * (1 - b + b * (dl / avgdl)))
```

| 参数 | 含义 | 默认值 |
|------|------|--------|
| k1 | 词频饱和度控制 | 1.2 |
| b | 文档长度归一化 | 0.75 |
| tf | 词频（Term Frequency） | — |
| IDF | 逆向文档频率 | — |

### Q28 什么是 Doc Values？和 FieldData 有什么区别？
| 特性 | Doc Values | FieldData |
|------|------------|-----------|
| 存储方式 | 磁盘（列式存储） | 内存（堆） |
| 构建时机 | 索引时构建 | 查询时延迟加载 |
| 适用场景 | 排序、聚合、脚本 | 对 text 字段的聚合（已不推荐） |
| 内存占用 | 低（OS 缓存管理） | 高（直接占用 JVM 堆） |

> 💡 ES 从设计上推荐使用 Doc Values，**text 字段默认没有 doc_values**。

### Q29 什么是 _source 字段？可以关闭吗？
- `_source` 存储原始的 JSON 文档内容。
- 作用：用于 `GET` 返回、更新、高亮显示等。
- 可以关闭（节省磁盘空间），但会丧失很多功能：
```json
{ "mappings": { "_source": { "enabled": false } } }
```
> 💡 **建议保留** `_source`，通过 `includes`/`excludes` 过滤不需要的字段。

### Q30 什么是 Translog？它的作用是什么？
**Translog（事务日志）** 是 ES 防止数据丢失的关键机制。

```
写入 → translog（append-only，追加写）→ 内存 buffer → ...
                       ↓ 宕机后重启
                 从 translog 恢复
```
| 持久化策略 | 说明 |
|------------|------|
| `async` | 异步 fsync（默认，性能好，可能丢数据） |
| `request` | 每次请求 fsync（安全，性能差） |

### Q31 ES 7.x 相比 ES 6.x 有哪些重要变更？
| 变更 | 说明 |
|------|------|
| 废弃 Type | 一个 Index 只能有一个 `_doc` |
| 默认分片改为 1 | 之前默认 5 个主分片 |
| 移除 Mapping 中的 `_all` 字段 | 改用 `copy_to` |
| Zen Discovery 替换 | 新 Seeded Gossip 协议 |
| 内置高亮重构 | 统一使用 Lucene 高亮 |

### Q32 为什么不建议使用 scroll 做实时搜索？
- scroll 创建的是**搜索时点的快照**，后续写入的数据不可见。
- scroll 需要维护上下文（`context`），**消耗大量内存**。
- 适用场景：**全量数据导出**、**reindex**、**后台批量处理**。

---

## 三、实战场景题（10题）

### Q33 电商系统中如何实现"多条件筛选 + 关键词搜索"？
使用 **bool + filter** 实现筛选条件缓存，配合 **must** 做全文搜索。
```json
{
  "query": {
    "bool": {
      "must": [{ "match": { "title": "手机" } }],
      "filter": [
        { "term": { "brand.keyword": "华为" } },
        { "range": { "price": { "gte": 2000, "lte": 5000 } } },
        { "term": { "stock": true } }
      ]
    }
  }
}
```

### Q34 如何实现"搜索引擎联想词/自动补全"？
使用 **completion suggester** 或 **前缀查询**。
```json
// Mapping 定义
{
  "mappings": {
    "properties": {
      "suggest": { "type": "completion" }
    }
  }
}
// 搜索建议
{
  "suggest": {
    "my-suggest": { "prefix": "jav", "completion": { "field": "suggest" } }
  }
}
```

### Q35 日志场景中如何避免索引无限增长？
使用 **ILM（Index Lifecycle Management）** 或**定时任务**：
1. 按日期滚动索引：`logs-2026.07.22`
2. 配置 `rollover` 策略（按大小/时间/文档数）
3. 冷数据自动迁移到冷节点（`warm` → `cold` → `frozen`）
4. 过期自动删除

### Q36 如何在 ES 中做"基于时间的统计分析"？
使用 **date_histogram** 聚合。
```json
{
  "size": 0,
  "aggs": {
    "sales_over_time": {
      "date_histogram": {
        "field": "order_time",
        "calendar_interval": "day",
        "format": "yyyy-MM-dd"
      },
      "aggs": {
        "total_sales": { "sum": { "field": "amount" } }
      }
    }
  }
}
```

### Q37 现在有 1000 万商品数据，如何实现高效分页？
> 生产环境中**优先推荐 search_after**。

```json
// 第一次查询
{
  "size": 20,
  "sort": [{ "id": "asc" }, { "price": "asc" }],
  "query": { "match_all": {} }
}
// 后续查询 — 传入上一页最后一个文档的 sort 值
{
  "size": 20,
  "sort": [{ "id": "asc" }, { "price": "asc" }],
  "search_after": [100, 2999],
  "query": { "match_all": {} }
}
```

### Q38 如何对已有索引新增字段并修改类型？
> ES 不允许修改已有字段的类型，必须通过 **reindex**。

```json
// 1. 创建新索引（正确映射）
PUT /new_index { "mappings": { "properties": { ... } } }

// 2. 全量迁移
POST /_reindex
{
  "source": { "index": "old_index" },
  "dest": { "index": "new_index" }
}

// 3. 删除旧索引，创建别名
POST /_aliases
{
  "actions": [
    { "remove": { "index": "old_index", "alias": "my_alias" } },
    { "add": { "index": "new_index", "alias": "my_alias" } }
  ]
}
```

### Q39 如何在搜索中对某个字段加权（Boosting）？
```json
// 方式一：字段权重
{
  "query": {
    "multi_match": {
      "query": "手机",
      "fields": ["title^5", "content^2", "tags"]
    }
  }
}
// 方式二：boost 参数
{
  "query": {
    "match": {
      "title": { "query": "手机", "boost": 5 }
    }
  }
}
// 方式三：boosting query
{
  "query": {
    "boosting": {
      "positive": { "match": { "title": "手机" } },
      "negative": { "match": { "category": "二手" } },
      "negative_boost": 0.3
    }
  }
}
```

### Q40 什么是 Elastic Stack（ELK）？各组件的作用？
| 组件 | 作用 |
|------|------|
| **Elasticsearch** | 分布式存储和搜索引擎 |
| **Logstash** | 数据采集、转换、管道处理 |
| **Kibana** | 可视化展示、监控、Dev Tools |
| **Filebeat** | 轻量日志采集器（取代 Logstash 采集职责） |

### Q41 如何监控 ES 集群健康状态？
```bash
# 集群健康状态
GET _cluster/health
# 返回：green（全部正常）、yellow（副本未分配）、red（主分片未分配）

# 节点信息
GET _cat/nodes?v

# 索引信息
GET _cat/indices?v

# 分片分配
GET _cat/shards?v
```

### Q42 如何实现"搜索我附近的门店"（Geo 查询基础）？
```json
// Mapping
{ "location": { "type": "geo_point" } }

// 搜索 5km 范围内
{
  "query": {
    "geo_distance": {
      "distance": "5km",
      "location": { "lat": 39.9042, "lon": 116.4074 }
    }
  }
}
```

---

## 四、手写代码/配置文件题（6题）

### Q43 手写：创建索引并指定 Mapping 和 Settings
```json
PUT /products
{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 1,
    "refresh_interval": "10s"
  },
  "mappings": {
    "properties": {
      "title": {
        "type": "text",
        "analyzer": "ik_max_word",
        "fields": { "keyword": { "type": "keyword" } }
      },
      "price": { "type": "double" },
      "createTime": { "type": "date", "format": "yyyy-MM-dd HH:mm:ss" },
      "tags": { "type": "keyword" },
      "description": { "type": "text", "analyzer": "ik_smart" }
    }
  }
}
```

### Q44 手写：Bulk 批量写入 + bool 复杂查询
```json
// 批量写入
POST /products/_bulk
{"index":{"_id":1}}
{"title":"Java并发编程实战","price":89,"tags":["Java","并发"]}
{"index":{"_id":2}}
{"title":"深入理解Java虚拟机","price":99,"tags":["Java","JVM"]}

// 复杂查询：搜索Java书籍，价格50-100，排除已下架
POST /products/_search
{
  "query": {
    "bool": {
      "must": [{ "match": { "title": "Java" } }],
      "filter": [
        { "range": { "price": { "gte": 50, "lte": 100 } } },
        { "term": { "status": "上架" } }
      ]
    }
  },
  "sort": [{ "price": { "order": "desc" } }],
  "from": 0,
  "size": 20,
  "highlight": {
    "fields": { "title": { "pre_tags": ["<b>"], "post_tags": ["</b>"] } }
  }
}
```

### Q45 手写：Terms 聚合分析 + Pipeline 聚合
```json
// 按品牌聚合 + 统计每个品牌的平均价格 + 所有品牌的平均价格
POST /products/_search
{
  "size": 0,
  "aggs": {
    "by_brand": {
      "terms": { "field": "brand.keyword", "size": 10, "order": { "avg_price": "desc" } },
      "aggs": {
        "avg_price": { "avg": { "field": "price" } }
      }
    },
    "overall_avg": {
      "avg_bucket": { "buckets_path": "by_brand>avg_price" }
    }
  }
}
```

### Q46 手写：按日期直方图统计 + 累积求和（Pipeline）
```json
POST /orders/_search
{
  "size": 0,
  "aggs": {
    "orders_over_time": {
      "date_histogram": {
        "field": "order_date",
        "calendar_interval": "month",
        "format": "yyyy-MM"
      },
      "aggs": {
        "monthly_total": { "sum": { "field": "amount" } },
        "cumulative_total": {
          "cumulative_sum": { "buckets_path": "monthly_total" }
        }
      }
    }
  }
}
```

### Q47 手写：Scroll 全量遍历
```json
// 初始化 scroll
POST /products/_search?scroll=2m
{
  "size": 1000,
  "query": { "match_all": {} }
}

// 后续滚动（返回 _scroll_id）
POST /_search/scroll
{
  "scroll": "2m",
  "scroll_id": "DXF1ZXJ5QW5kRmV0Y2gB..."
}

// 清理 scroll 上下文
DELETE /_search/scroll
{
  "scroll_id": "DXF1ZXJ5QW5kRmV0Y2gB..."
}
```

### Q48 手写：force merge + index settings 优化
```json
// 对只读索引执行 force merge（合并为 1 个 segment）
POST /logs-2026-07/_forcemerge?max_num_segments=1

// 写入优化配置
PUT /logs_write/_settings
{
  "index": {
    "refresh_interval": "-1",      // 写入时关闭自动 refresh
    "number_of_replicas": 0,       // 写入时关闭副本
    "translog.durability": "async", // 异步 translog
    "translog.sync_interval": "30s"
  }
}
// 写入完成后恢复
PUT /logs_write/_settings
{
  "index": {
    "refresh_interval": "30s",
    "number_of_replicas": 1,
    "translog.durability": "request"
  }
}
```

---

## 五、系统设计题（4题）

### Q49 设计一个"电商商品搜索引擎"
> **要求**：支持关键词搜索、多条件筛选、价格区间、销量排序、分页、高亮。

**架构方案**：
```
应用层 → ES Coordinating Node → ES Data Nodes
          ├─ 商品索引（products）
          │  ├─ title（text + ik）— 全文搜索
          │  ├─ category（keyword）— 类目筛选
          │  ├─ brand（keyword）— 品牌筛选
          │  ├─ price（double）— 价格区间 + 排序
          │  ├─ sales（integer）— 排序
          │  └─ status（keyword）— 上下架
          └─ 搜索流程
              1. bool: must(title 匹配) + filter(类目/品牌/价格)
              2. sort 根据用户选择
              3. search_after 分页
              4. highlight 高亮
```

### Q50 设计一个"日志监控与分析平台"
> **要求**：每天 TB 级日志，支持实时搜索和历史归档。

**架构方案**：
```
Filebeat（采集）→ Kafka（缓冲）→ Logstash（清洗）→ ES（存储）→ Kibana（展示）
                     ↓
               ES 滚动索引: logs-yyyy.MM.dd
               ILM 策略: hot(7天) → warm(30天) → cold(90天) → delete
```
> 💡 **关键设计**：使用别名写入，按日期滚动，ILM 自动管理生命周期，对历史索引 force merge 节省空间。

### Q51 如何将 MySQL 数据同步到 ES？
| 方案 | 优缺点 | 适用场景 |
|------|--------|----------|
| **Logstash JDBC Input** | 简单，定时轮询 | 数据量不大，实时性要求不高 |
| **Canal（阿里）** | 监听 MySQL binlog，实时性强 | 增量实时同步 |
| **双写（业务代码同步）** | 灵活可控，但增加业务复杂度 | 中小项目 |
| **Stream 组件（Kafka Connect）** | 解耦，可扩展 | 微服务架构 |

### Q52 设计一个"多租户 SaaS 搜索系统"
> **核心**：租户数据隔离

| 隔离方案 | 优点 | 缺点 |
|----------|------|------|
| **Index 隔离** | 天然物理隔离，查询简单 | 索引过多（集群分片数限制） |
| **Alias + Routing** | 每个租户一个 alias + routing | 单索引上限问题 |
| **Field 隔离** | 同一索引加 tenant_id 过滤 | 查询需始终带 tenant_id |

---

## 六、常见坑点与最佳实践

### 常见踩坑
| 坑点 | 现象 | 解决方案 |
|------|------|----------|
| text 字段 + term 查询 | 查不到结果 | 使用 `.keyword` 或 match 查询 |
| 主分片数过多 | 集群不稳定、merge 开销大 | 建议单分片 20-50GB |
| from + size 深分页 | 内存 OOM | 改用 search_after |
| 动态映射导致字段类型错误 | 日期被映射为 text | 生产环境禁用动态映射 |
| 不设置 `_source` 过滤 | 返回大量无用字段 | 使用 `_source` includes/excludes |
| Bulk 条数过多 | ES 内存溢出 | 控制单批 5-15MB |
| IK 热更新不生效 | 新词无法识别 | 配置 IK 远程词典 |

### 最佳实践清单
> ✅ 生产环境使用 **显式 Mapping**，禁用 `dynamic: true`
> ✅ 日志类索引使用 **按日期滚动 + ILM**
> ✅ 搜索类场景将 **filter 前置**，利用缓存
> ✅ 使用 **别名（Alias）** 访问索引，方便 reindex
> ✅ 合理设置 `number_of_shards`（单分片 20-50GB）
> ✅ Bulk 写入时临时关闭 refresh 和副本

---

## 七、面试回答模板（Top 5）

### 模板 1：请介绍一下 Elasticsearch
> 建议回答结构：底层 → 核心能力 → 适用场景 → 对比竞品

"Elasticsearch 是一个基于 Apache Lucene 的分布式搜索引擎，提供 RESTful API。它在底层使用倒排索引实现全文搜索，天然支持分布式扩展。我常用它做电商搜索、日志分析、商品推荐等场景。相比于 Solr，ES 的优点是开箱即用、生态丰富（ELK Stack）、实时性更好。"

### 模板 2：倒排索引是如何工作的？
> 建议回答结构：定义 → 结构 → 与正排对比 → 优缺点

"倒排索引是将文档中的词项映射到文档 ID 的索引结构。例如对 'Java 编程' 分词后得到 'java' 和 '编程'，分别指向包含它们的文档 ID 列表。优点是搜索速度快，缺点是写入时需要分词、维护词典，写入性能低于正排索引。"

### 模板 3：ES 写入过程是怎样的？
> 建议回答结构：整体流程 → 两个异步机制 → 数据安全

"文档通过路由算法到达主分片，先写 translog 保证不丢数据，然后写入内存 buffer。每秒一次的 refresh 将 buffer 变为 segment 并开放搜索。后台通过 flush 将 segment 持久化到磁盘。segment 在后台 merge 合并清理已删除文档。"

### 模板 4：ES 和 MySQL 的区别？
| 维度 | ES | MySQL |
|------|-----|-------|
| 数据模型 | 文档（JSON） | 关系型（表） |
| 查询 | 全文搜索 + 聚合分析 | SQL 精确查询 + JOIN |
| 事务 | 单文档原子性 | ACID 事务 |
| 一致性 | 最终一致性 | 强一致性 |
| 扩展性 | 天然分布式 + 水平扩展 | 读写分离 + 分库分表 |

### 模板 5：项目中最难解决的 ES 问题？
> 建议挑选 1-2 个真实问题，按 STAR 原则回答

"一次线上 ES 集群频繁 Full GC，排查发现某个索引主分片设置过多（15 个），每个分片又有很多 segment，导致 merge 频繁。解决方案是重建索引，将分片数调整为 3 个，同时调大 `refresh_interval` 和 `indices.memory.index_buffer_size`，优化后集群稳定。"

---

## 八、快速查漏补缺 Checklist

### 基础概念
- [ ] 倒排索引的核心原理
- [ ] Cluster → Node → Index → Document → Field 层级关系
- [ ] Primary Shard vs Replica Shard
- [ ] 近实时的含义（refresh 机制）
- [ ] text vs keyword 的区别和适用场景
- [ ] dynamic vs explicit mapping
- [ ] Analyzer 三组件
- [ ] IK 分词器两种模式

### 查询与聚合
- [ ] match vs term vs match_phrase
- [ ] bool 查询四种子句
- [ ] from+size / scroll / search_after 三种分页
- [ ] Bucket / Metric / Pipeline 聚合
- [ ] date_histogram 按时间聚合
- [ ] Highlight 高亮

### 写入与优化
- [ ] Bulk API 最佳实践
- [ ] 写入完整流程
- [ ] refresh / flush / merge / translog
- [ ] Segment 不可变性
- [ ] 写入优化（关闭 refresh + 副本）

### 其他
- [ ] ES 7.x 重要变更
- [ ] reindex 流程
- [ ] Elastic Stack 各组件职责
- [ ] 集群健康状态三色
