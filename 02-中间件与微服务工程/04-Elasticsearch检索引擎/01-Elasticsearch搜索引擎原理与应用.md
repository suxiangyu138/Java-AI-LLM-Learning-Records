# Elasticsearch 搜索引擎原理与应用

## 一、引言

在大数据时代，海量数据的存储与检索成为后端系统的核心挑战。传统关系型数据库（如 MySQL）在面对全文搜索、实时聚合分析、海量数据高并发查询等场景时，往往力不从心。Elasticsearch 应运而生，它是一款基于 **Apache Lucene** 构建的分布式搜索与分析引擎，凭借其近实时搜索、分布式架构、丰富的查询 DSL 和强大的聚合能力，已成为业界最主流的搜索引擎之一。

本文将从核心概念、倒排索引原理、文档与索引建模、查询 DSL、分页方案、搜索优化以及 Spring Boot 集成等维度，系统性地深入剖析 Elasticsearch 的原理与实践。

---

## 二、核心概念与倒排索引

### 2.1 什么是 Elasticsearch

Elasticsearch（简称 ES）是一个基于 Lucene 的**分布式、RESTful 风格的搜索与分析引擎**。它能够对海量数据进行近实时（Near Real-Time, NRT）的存储、搜索和分析。所谓"近实时"，是指文档从写入到可被搜索，通常有大约 1 秒的延迟（由 refresh_interval 控制），而非传统数据库的即时一致性。

ES 的典型应用场景包括：

- **全文搜索**：电商商品搜索、博客文章检索、文档管理系统。
- **日志分析**：ELK（Elasticsearch + Logstash + Kibana）技术栈的核心，用于收集、存储和分析海量日志。
- **指标监控**：APM 系统的时序指标聚合与可视化。
- **推荐与个性化**：基于用户行为数据的实时检索与过滤。

### 2.2 ES 与 MySQL 的对比

将 ES 与 MySQL 对比有助于理解二者的定位差异：

| 维度 | MySQL | Elasticsearch |
|------|-------|---------------|
| 数据模型 | 关系型（表、行、列） | 文档型（JSON 文档） |
| 事务支持 | ACID 事务，强一致性 | 最终一致性，无跨文档事务 |
| 关联查询 | JOIN，支持复杂关联 | 不擅长 JOIN，建议反范式设计 |
| 全文搜索 | 弱（LIKE 模糊匹配，无法打分） | 强（倒排索引，BM25 相关性打分） |
| 聚合分析 | 弱（GROUP BY 性能有限） | 强（Bucket + Metric 多级聚合） |
| 海量数据 | 分库分表复杂，扩展成本高 | 天然分布式，水平扩展简单 |
| 写入性能 | 受事务和索引影响较大 | 近实时写入，批量写入性能极高 |

**核心结论**：ES 擅长**搜索、聚合、海量数据的快速检索**；MySQL 擅长**事务、关联、强一致性数据存储**。二者是互补关系，而非替代关系。微服务架构中常见的设计模式是：MySQL 作为主存储，ES 作为搜索与分析引擎，通过 CDC（Change Data Capture）或双写策略同步数据。

### 2.3 倒排索引原理

倒排索引（Inverted Index）是 ES 实现快速全文搜索的基石。要理解倒排索引，首先需要理解与之相对的正排索引。

#### 正排索引 vs 倒排索引

**正排索引（Forward Index）** 是以文档 ID 为键，文档内容为值的数据结构。当我们搜索"包含某个词的文档"时，需要扫描所有文档，逐个检查内容是否包含该词——这就是 MySQL 中 LIKE '%keyword%' 的工作方式，时间复杂度为 O(n)，n 为文档总数。

**倒排索引（Inverted Index）** 反过来，以"词"为键，以"包含该词的文档列表"为值。构建过程如下：

1. **分词（Tokenization）**：将文档文本按规则切分为一个个词条（Term）。
2. **建立词表**：将所有词条去重后排序，形成 Term Dictionary。
3. **映射文档**：对每个词条，记录出现该词条的文档 ID 列表（Posting List）。

搜索时，直接查词表找到对应词条，然后读取其 Posting List 即可快速定位所有匹配文档。对于精确查找，时间复杂度为 O(1)（通过 Term Index 定位）或 O(log n)（二分查找 Term Dictionary）。

假设有两篇文档：

- 文档 1："Elasticsearch 是一个搜索引擎"
- 文档 2："搜索引擎基于倒排索引"

倒排索引结构如下（简化）：

| Term | Posting List |
|------|-------------|
| Elasticsearch | [1] |
| 一个 | [1] |
| 搜索引擎 | [1, 2] |
| 基于 | [2] |
| 倒排索引 | [2] |

搜索"搜索引擎"时，直接从词表中找到对应词条，返回文档 1 和 2，速度极快。

#### 倒排索引的三层结构

ES 的倒排索引在 Lucene 层面由三个核心部分组成：

**1. Term Dictionary（词表）**

存储所有经过分词处理后的词条，按字典序排序。由于词条数量可能极其庞大（数百万甚至数亿级别），Lucene 使用**二分查找**来快速定位词条。进一步优化是使用 **FSM（Finite State Machine，有限状态机）** 来压缩词表，减少内存占用。

**2. Term Index（词索引）**

Term Dictionary 仍然可能很大（存储在磁盘上），每次查找都需要磁盘 I/O。为了加速定位，Lucene 在内存中维护了一个 Term Index，它的底层数据结构是 **Trie 树（前缀树）的变体——FST（Finite State Transducer，有限状态转换器）**。

FST 的核心优势：

- **共享前缀**：如 "application" 和 "apply" 共享 "appl" 前缀，只需存储一次。
- **内存紧凑**：经过 FST 压缩后的 Term Index 通常只有 Term Dictionary 大小的几十分之一，可以完整加载到内存。
- **快速定位**：给定一个词条，FST 可以在内存中快速定位其在 Term Dictionary 中的文件偏移量，然后将磁盘读取范围缩小到极小的区域内。

**3. Posting List（倒排表）**

记录了包含每个词条的所有文档信息，包括：

- **文档 ID**：用于定位文档。
- **词频（Term Frequency, TF）**：该词条在文档中出现的次数，用于相关性打分。
- **位置（Position）**：词条在文档中的位置偏移，用于短语查询（match_phrase）。
- **偏移量（Offset）**：词条在原始文本中的开始和结束位置，用于高亮显示（highlight）。

Posting List 的压缩技术：

- **FOR（Frame Of Reference）**：将文档 ID 列表排序后，增量编码（存储差值而非原始值），然后按 block 打包压缩。适用于均匀分布的数据。
- **RBM（Roaring Bitmaps）**：将文档 ID 按高 16 位分桶，每个桶内根据数据密度选择不同的存储方式（数组或位图），在压缩率和查询性能之间取得平衡。

#### 倒排索引的不可变性

倒排索引一旦写入磁盘就不可修改，这种设计带来了以下优点：

- **无需加锁**：没有并发写冲突，读性能极高。
- **文件系统缓存友好**：静态数据可被操作系统高效缓存。

但也带来一个明显缺点：更新文档必须重建整个 Segment。ES 通过**段合并（Merge）** 策略来解决这一问题——多个小段定期合并为一个大段，删除和更新操作在合并时真正生效。

### 2.4 相关性打分

搜索结果的排序质量取决于相关性打分算法。ES 先后使用了两种算法：

#### TF-IDF（Term Frequency - Inverse Document Frequency）

TF-IDF 是 ES 早期版本（5.0 之前）的默认算法。其核心思想是：

- **TF（词频）**：一个词条在文档中出现的次数越多，该文档的相关性越高。
- **IDF（逆文档频率）**：一个词条在越多的文档中出现，它对区分文档的贡献度越低（如"的"、"了"等停用词，IDF 很低）。

TF-IDF 的公式简化为：`score = TF * IDF`。

缺点：当某个词条在单篇文档中出现极多次时，TF 值会线性增长，可能导致某些不相关的文档因词频过高而获得高评分。

#### BM25（Best Matching 25）

ES 5.0 之后将默认算法切换为 BM25，它是 TF-IDF 的改进版本。核心改进在于引入了**非线性词频饱和度控制**：

- BM25 的 TF 部分不是线性的，而是收敛的。当一个词在文档中出现多次时，TF 贡献会趋向一个上限（由参数 `k1` 控制），避免单篇文档因词频过高而主导排序。
- 引入文档长度归一化（参数 `b` 控制），长文档中词频会被适当惩罚，短文档中词频会被奖励。

BM25 的两个关键参数：

- **k1**（默认 1.2）：控制词频饱和度的增长速度。值越小，词频增长对分数的影响越快饱和。
- **b**（默认 0.75）：控制文档长度归一化的程度。值越大，文档长度对打分的影响越大。

在实际应用中，BM25 的排序效果通常明显优于 TF-IDF，尤其在处理长短文档混合的场景中优势更为突出。

---

## 三、文档与索引

### 3.1 核心概念与 RDB 类比

| ES 概念 | RDB 类比 | 说明 |
|---------|---------|------|
| Index | Database | 索引是文档的逻辑容器 |
| ~~Type~~ | Table | **ES 7.x 废弃，8.x 移除**，一个 Index 只对应一种文档类型 |
| Document | Row | JSON 格式的数据记录 |
| Field | Column | 文档中的字段 |
| Mapping | Schema | 定义字段的类型、分词器等元信息 |
| Shard | 分片 | 索引数据的水平切分单元 |
| Replica | 副本 | 分片的冗余拷贝 |

> **重要变更**：ES 6.x 开始一个 Index 只能包含一个 Type，ES 7.x 标记 Type 为废弃，ES 8.x 彻底移除 Type。因此，现在设计索引时无需关心 Type 的概念，一个 Index 天然对应一种 Document 类型。

### 3.2 Mapping 映射

Mapping 是 ES 中定义字段类型和索引方式的元数据，类似于数据库的 Schema。

#### Dynamic Mapping（动态映射）

当向一个不存在的索引写入文档时，ES 会自动根据字段值推断字段类型：

| JSON 数据类型 | ES 自动推断类型 |
|--------------|----------------|
| 字符串（"hello"） | text + keyword（多字段） |
| 整数（123） | long |
| 浮点数（3.14） | float |
| 布尔值（true/false） | boolean |
| 对象（{"a": 1}） | object |
| 数组（[...]） | 根据第一个非 null 元素推断 |
| 日期格式字符串 | date 或 text |

动态映射方便快捷，适合非生产环境的快速原型开发。但在生产环境中，**强烈建议使用显式映射**，原因如下：

- 自动推断的字段类型可能不符合业务需求（如时间戳可能被推断为 long）。
- 字符串被映射为 text + keyword，会增加不必要的存储开销。
- 无法控制分词器、是否索引等高级选项。

#### Explicit Mapping（显式映射）

创建索引时明确指定各字段的类型和属性：

```json
PUT /my_index
{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 1,
    "refresh_interval": "10s"
  },
  "mappings": {
    "dynamic": "strict",
    "properties": {
      "title": {
        "type": "text",
        "analyzer": "ik_max_word",
        "fields": {
          "keyword": {
            "type": "keyword"
          }
        }
      },
      "price": {
        "type": "float"
      },
      "create_time": {
        "type": "date",
        "format": "yyyy-MM-dd HH:mm:ss||epoch_millis"
      },
      "tags": {
        "type": "keyword"
      },
      "description": {
        "type": "text",
        "analyzer": "ik_smart"
      },
      "location": {
        "type": "geo_point"
      },
      "specs": {
        "type": "nested",
        "properties": {
          "name": { "type": "keyword" },
          "value": { "type": "keyword" }
        }
      },
      "status": {
        "type": "integer"
      }
    }
  }
}
```

**关键属性说明**：

- `dynamic`：`true`（默认，自动添加新字段）、`false`（忽略新字段）、`strict`（遇到未映射字段报错）。生产环境推荐 `strict`。
- `analyzer`：指定分词器，仅对 `text` 类型有效。
- `fields`：多字段特性，同一个字段支持不同的索引方式，如 `title` 同时有 `text`（用于全文搜索）和 `keyword`（用于精确聚合）。

#### 字段类型的不可修改性

Mapping 中字段类型一旦创建，**通常不能修改**。这是因为 ES 依赖倒排索引的结构来检索数据，修改字段类型意味着需要重建整个索引。如果需要修改，只能通过以下方式：

1. **创建新索引**，重新定义 Mapping，然后使用 Reindex API 迁移数据。
2. **新增字段**（允许）：使用 `PUT /my_index/_mapping` 添加之前不存在的字段。
3. **使用别名**：通过索引别名实现零停机切换。

### 3.3 text vs keyword

这是 ES 中使用频率最高也最容易混淆的一组字段类型。

#### text 类型

- **分词**：会被分词器拆分为多个词条，构建倒排索引。
- **搜索方式**：全文搜索，查询时输入的查询文本也会被分词。
- **聚合与排序**：**不支持**（直接对 text 字段聚合会报错），如需聚合要用 `fields` 子字段的 keyword。
- **适用场景**：文章标题、正文、商品描述、评论内容等需要全文检索的字段。

#### keyword 类型

- **不分词**：作为整体存储到倒排索引中（精确值）。
- **搜索方式**：精确查询（term）、前缀查询、通配符查询、范围查询。
- **聚合与排序**：**原生支持**，适合做 terms 聚合、排序等操作。
- **适用场景**：用户 ID、订单状态、标签、分类、邮箱、URL、电话号码等。

#### 多字段（multi-fields）模式

大多数情况下，同一个字段既需要全文搜索又需要精确聚合。ES 通过 `fields` 语法支持多字段模式：

```json
{
  "title": {
    "type": "text",
    "analyzer": "ik_smart",
    "fields": {
      "keyword": {
        "type": "keyword"
      },
      "english": {
        "type": "text",
        "analyzer": "standard"
      }
    }
  }
}
```

查询时：

- 全文搜索：`match` 查询 `title` 字段。
- 精确聚合：`term` 查询或 `terms` 聚合使用 `title.keyword` 字段。
- 英文搜索：`match` 查询 `title.english` 字段。

### 3.4 分片与副本

#### Primary Shard（主分片）

索引的数据被水平切分为多个分片，每个分片是一个完整的 Lucene 索引实例。

- 主分片数量在创建索引时指定，**创建后不可修改**（除非使用 `_split` 或 `_shrink` API）。
- 默认配置：`number_of_shards: 1`（ES 7.x+，早期版本默认为 5）。
- 确定分片数量的依据：
  - 建议每个分片数据量在 10GB~50GB 之间。
  - 分片数量 = 预期数据总量 / 每个分片目标大小。
  - 同时考虑节点数：总主分片数 = 节点数 * 每个节点的主分片数。
  - **避免过度分片**：分片过多会导致集群管理开销增大（各分片需要维持集群状态、GC 压力增大）。

```json
PUT /my_index
{
  "settings": {
    "number_of_shards": 5,
    "number_of_replicas": 1
  }
}
```

#### Replica Shard（副本分片）

每个主分片可以有一个或多个副本分片。副本分片是主分片的完整拷贝。

- **高可用**：主分片所在的节点宕机时，副本分片可以被提升为新的主分片。
- **扩展读**：查询请求可以分发到主分片或副本分片，副本越多，查询吞吐量越大。
- **动态调整**：副本数可以随时修改，无需重建索引。
- 默认配置：`number_of_replicas: 1`。

```json
PUT /my_index/_settings
{
  "number_of_replicas": 2
}
```

#### 路由算法

ES 通过路由算法决定文档应该写入哪个分片：

```
shard = hash(_routing) % number_of_primary_shards
```

- `_routing` 默认使用文档的 `_id` 字段。用户也可以显式指定 routing 值。
- 哈希函数对 `_routing` 值计算哈希，然后对主分片数量取模。
- 这解释了为什么主分片数量创建后不可修改——一旦修改，取模结果会变化，原有数据的位置映射将全部失效。

**自定义 routing 的应用场景**：

```json
PUT /my_index/_doc/1?routing=user_123
{
  "user_id": "user_123",
  "title": "文档标题"
}
```

- 将某个用户的所有文档路由到同一个分片上，查询时也指定相同的 `routing`，可以跳过分片广播，**仅查询一个分片**，大幅提升查询性能。
- 但也要注意，自定义 routing 可能导致分片数据分布不均匀（某些分片数据量远大于其他分片），需要权衡。

---

## 四、查询 DSL

Elasticsearch 提供了一套基于 JSON 的丰富查询语言——**Query DSL**（Domain Specific Language）。查询 DSL 分为两大类别：

- **Leaf Query（叶子查询）**：直接针对特定字段进行匹配，如 `match`、`term`、`range`。
- **Compound Query（复合查询）**：组合多个叶子查询或其他复合查询，如 `bool`、`boosting`。

### 4.1 全文搜索

全文搜索的核心特征是：**查询文本会被分词**，然后与倒排索引中的词条匹配。

#### match 查询

最基本的全文搜索查询：

```json
GET /my_index/_search
{
  "query": {
    "match": {
      "title": "Elasticsearch 搜索引擎"
    }
  }
}
```

执行过程：

1. 对查询文本"Elasticsearch 搜索引擎"进行分词，得到 `["elasticsearch", "搜索引擎"]`。
2. 对每个词条进行搜索，默认使用 OR 逻辑（只要匹配任意一个词条就返回）。
3. 按相关性得分降序排列。

可以通过 `operator` 参数控制匹配逻辑：

```json
GET /my_index/_search
{
  "query": {
    "match": {
      "title": {
        "query": "Elasticsearch 搜索引擎",
        "operator": "and"
      }
    }
  }
}
```

`operator: "and"` 要求文档必须同时包含所有词条。

`minimum_should_match` 参数可以指定最少匹配词条数（百分比或绝对值）：

```json
{
  "match": {
    "title": {
      "query": "Elasticsearch 搜索引擎 原理",
      "minimum_should_match": "2"
    }
  }
}
```

#### match_phrase 查询

短语匹配，要求词条按照给定顺序且紧密相邻地出现在文档中：

```json
GET /my_index/_search
{
  "query": {
    "match_phrase": {
      "content": {
        "query": "搜索引擎 原理",
        "slop": 1
      }
    }
  }
}
```

`slop` 参数控制词条之间允许的间隔词数。例如，`slop: 1` 可以匹配"搜索引擎的底层原理"（中间隔了一个"的"和"底层"）。

- `slop: 0`（默认）：词条必须完全连续相邻。
- `slop: 2`：允许最多 2 个词条间隔。

`match_phrase` 的底层实现是 Lucene 的 **PhraseQuery**，它利用倒排索引中的位置信息来验证词条间的距离约束。

#### multi_match 查询

同时在多个字段中进行全文搜索：

```json
GET /my_index/_search
{
  "query": {
    "multi_match": {
      "query": "Elasticsearch",
      "fields": ["title^2", "content"],
      "type": "best_fields"
    }
  }
}
```

- `fields`：指定搜索字段列表，`^2` 表示该字段的权重提升 2 倍。
- `type`：支持多种匹配策略：
  - `best_fields`（默认）：取匹配度最高的字段的得分作为最终得分。
  - `most_fields`：合并所有匹配字段的得分。
  - `cross_fields`：将词条视为跨字段匹配，用于结构化的文档（如姓名分成 first_name 和 last_name）。
  - `phrase` / `phrase_prefix`：对每个字段执行 `match_phrase`。

#### query_string 查询

支持类似 Google 的语法（AND、OR、NOT、通配符、正则、模糊匹配等）：

```json
GET /my_index/_search
{
  "query": {
    "query_string": {
      "query": "Elasticsearch AND (搜索引擎 OR 原理) -数据库",
      "default_field": "content"
    }
  }
}
```

**注意**：`query_string` 功能强大，但也容易导致语法解析错误。如果用户输入直接拼接进 `query_string`，可能会抛出异常。推荐在面向用户的搜索栏中使用 `simple_query_string`，它对语法错误更宽容。

### 4.2 精确查询

精确查询的核心特征是：**不对查询文本分词**，直接使用查询文本与倒排索引中的词条进行精确匹配。

#### term / terms 查询

```json
GET /my_index/_search
{
  "query": {
    "term": {
      "status.keyword": "ACTIVE"
    }
  }
}
```

```json
GET /my_index/_search
{
  "query": {
    "terms": {
      "tags.keyword": ["tech", "elastic", "search"]
    }
  }
}
```

> **关键陷阱**：切勿对 `text` 字段使用 `term` 查询。因为 `text` 字段的值会被分词，而 `term` 不会对查询文本分词。例如，text 字段中"搜索引擎"可能被分词为"搜索"和"引擎"，如果用 `term` 精确查找"搜索引擎"，将无法匹配。正确的做法是对 `keyword` 字段使用 `term`。

#### range 查询

```json
GET /my_index/_search
{
  "query": {
    "range": {
      "price": {
        "gte": 100,
        "lte": 500
      }
    }
  }
}
```

参数：`gt`（大于）、`gte`（大于等于）、`lt`（小于）、`lte`（小于等于）。

日期范围查询：

```json
GET /my_index/_search
{
  "query": {
    "range": {
      "create_time": {
        "gte": "2024-01-01",
        "lte": "now-1d/d"
      }
    }
  }
}
```

日期数学表达式：`now-1d/d` 表示"当前时间减去一天，然后向下取整到日"。

#### exists 查询

查找指定字段不为空的文档：

```json
GET /my_index/_search
{
  "query": {
    "exists": {
      "field": "description"
    }
  }
}
```

#### prefix 查询

前缀匹配（效率较低，因为需要扫描 Term Dictionary 中指定前缀的所有词条）：

```json
GET /my_index/_search
{
  "query": {
    "prefix": {
      "title.keyword": "Ela"
    }
  }
}
```

#### wildcard 查询

通配符查询，支持 `*`（任意多字符）和 `?`（单个字符）：

```json
GET /my_index/_search
{
  "query": {
    "wildcard": {
      "title.keyword": "Elastic*"
    }
  }
}
```

> **性能警告**：`wildcard` 查询（尤其是前缀为通配符的模式，如 `*search`）性能极差，因为它需要对 Term Dictionary 进行全面扫描。如果可以，优先选择 `prefix` 或 `match_phrase_prefix`。

#### fuzzy 查询

模糊匹配，基于 Levenshtein 编辑距离：

```json
GET /my_index/_search
{
  "query": {
    "fuzzy": {
      "title.keyword": {
        "value": "Elasticseach",
        "fuzziness": "AUTO"
      }
    }
  }
}
```

`fuzziness` 参数：
- `AUTO`（默认）：根据词条长度自动选择编辑距离（0~2）。
- 固定值：如 `1` 或 `2`。

`fuzzy` 查询适用于搜索拼写错误的场景（用户输入"Elasticseach"仍能匹配"Elasticsearch"），但性能开销较大。

### 4.3 复合查询：bool

`bool` 查询是最常用也最强大的复合查询，它包含四种类型的子句：

| 子句 | 作用 | SQL 类比 | 是否影响 _score |
|------|------|----------|----------------|
| `must` | 必须匹配，贡献分数 | AND | 是 |
| `filter` | 必须匹配，不贡献分数 | AND | 否 |
| `should` | 匹配任意个，影响分数 | OR | 是 |
| `must_not` | 必须不匹配 | NOT | 否 |

```json
GET /my_index/_search
{
  "query": {
    "bool": {
      "must": [
        { "match": { "title": "Elasticsearch" } },
        { "match": { "content": "搜索引擎" } }
      ],
      "filter": [
        { "term": { "status.keyword": "ACTIVE" } },
        { "range": { "price": { "gte": 100, "lte": 500 } } }
      ],
      "should": [
        { "match": { "tags.keyword": "tech" } },
        { "match": { "tags.keyword": "java" } }
      ],
      "must_not": [
        { "term": { "deleted_flag": true } }
      ],
      "minimum_should_match": 1
    }
  }
}
```

#### filter vs must 的选择策略

这是 ES 查询优化中最常被问及的问题。两者的核心区别在于**是否计算相关性分数**：

| 对比维度 | filter | must |
|---------|--------|------|
| 是否计算 _score | 否（默认赋值为 0） | 是（进行 TF-IDF / BM25 打分） |
| 缓存 | 自动缓存（节点级别的节点查询缓存） | 不缓存 |
| 性能 | 高（跳过打分计算、利用缓存） | 较低 |
| 适用场景 | 结构化精确筛选（状态、时间范围、标签） | 全文搜索相关性排序 |

**最佳实践**：

- 将所有不需要影响相关性的条件放到 `filter` 子句中——包括但不限于：状态筛选、时间范围、分类过滤、权限过滤。
- 只在确实需要相关性排序时才使用 `must`。
- 一个好的 `bool` 查询通常只有一个 `must`（全文匹配），其余条件全部放在 `filter` 中。

```json
{
  "query": {
    "bool": {
      "must": [
        { "match": { "title": "搜索关键词" } }
      ],
      "filter": [
        { "term": { "status.keyword": "PUBLISHED" } },
        { "range": { "create_time": { "gte": "2024-01-01" } } },
        { "terms": { "category_id.keyword": [1, 2, 5] } }
      ]
    }
  }
}
```

### 4.4 聚合分析

ES 的聚合（Aggregation）是其核心能力之一，用于数据的统计分析。聚合分为三大类：

#### Bucket 聚合（桶聚合）

将文档分组到不同的"桶"中，类似 SQL 的 `GROUP BY`。

**terms 聚合**（按值分组）：

```json
GET /my_index/_search
{
  "size": 0,
  "aggs": {
    "by_status": {
      "terms": {
        "field": "status.keyword",
        "size": 10,
        "order": { "_count": "desc" }
      }
    }
  }
}
```

**histogram 聚合**（等间隔直方图）：

```json
GET /my_index/_search
{
  "size": 0,
  "aggs": {
    "price_ranges": {
      "histogram": {
        "field": "price",
        "interval": 100
      }
    }
  }
}
```

**date_histogram 聚合**（按时间间隔分组）：

```json
GET /my_index/_search
{
  "size": 0,
  "aggs": {
    "orders_over_time": {
      "date_histogram": {
        "field": "create_time",
        "calendar_interval": "day",
        "format": "yyyy-MM-dd",
        "min_doc_count": 0
      }
    }
  }
}
```

#### Metric 聚合（指标聚合）

对桶内的数据进行数值计算。

```json
GET /my_index/_search
{
  "size": 0,
  "aggs": {
    "by_category": {
      "terms": {
        "field": "category_id.keyword"
      },
      "aggs": {
        "avg_price": { "avg": { "field": "price" } },
        "max_price": { "max": { "field": "price" } },
        "min_price": { "min": { "field": "price" } },
        "sum_sales": { "sum": { "field": "sales" } },
        "stats_all": { "stats": { "field": "price" } },
        "unique_buyers": { "cardinality": { "field": "buyer_id.keyword" } }
      }
    }
  }
}
```

- `avg` / `sum` / `max` / `min`：基本统计指标。
- `stats`：包含上述所有统计值（count、min、max、avg、sum）。
- `cardinality`：基数统计（去重计数），类似 SQL 的 `COUNT(DISTINCT)`。基于 HyperLogLog 算法，精度可配置（`precision_threshold`），在内存和准确性之间取得平衡。

#### Pipeline 聚合（管道聚合）

基于其他聚合的结果进行二次计算。

```json
GET /my_index/_search
{
  "size": 0,
  "aggs": {
    "sales_per_month": {
      "date_histogram": {
        "field": "sale_time",
        "calendar_interval": "month"
      },
      "aggs": {
        "total_sales": { "sum": { "field": "amount" } }
      }
    },
    "monthly_moving_avg": {
      "moving_fn": {
        "buckets_path": "sales_per_month>total_sales",
        "window": 3,
        "script": "MovingFunctions.unweightedAvg(values)"
      }
    }
  }
}
```

### 4.5 高阶查询

#### nested 查询

ES 中的 `object` 类型在处理对象数组时存在一个经典问题：**内部对象的字段边界被抹平了**。考虑以下文档：

```json
{
  "specs": [
    { "name": "color", "value": "red" },
    { "name": "size", "value": "XL" }
  ]
}
```

如果使用 `object` 类型存储，ES 内部将其扁平化为：

```json
{
  "specs.name": ["color", "size"],
  "specs.value": ["red", "XL"]
}
```

此时搜索"color AND XL"会错误地匹配，因为"color"和"XL"来自不同的内部对象。

解决方案是使用 `nested` 类型。它将数组中的每个对象**独立索引为隐藏的 Lucene 文档**，保持对象内部的字段关联：

```json
{
  "mappings": {
    "properties": {
      "specs": {
        "type": "nested",
        "properties": {
          "name": { "type": "keyword" },
          "value": { "type": "keyword" }
        }
      }
    }
  }
}
```

查询时必须使用 `nested` 查询：

```json
GET /my_index/_search
{
  "query": {
    "nested": {
      "path": "specs",
      "query": {
        "bool": {
          "must": [
            { "term": { "specs.name": "color" } },
            { "term": { "specs.value": "red" } }
          ]
        }
      }
    }
  }
}
```

这个查询确保 "name=color" 和 "value=red" 必须来自**同一个**嵌套对象。

#### join 查询

ES 的 `join` 类型允许在同一索引中定义父子关系。它通过维护一个**父子关系的映射表**（在内存中存储文档间的父子关系）来实现关联。

```json
{
  "mappings": {
    "properties": {
      "my_join_field": {
        "type": "join",
        "relations": {
          "question": "answer"
        }
      }
    }
  }
}
```

查询示例：

```json
GET /qa_index/_search
{
  "query": {
    "has_child": {
      "type": "answer",
      "query": { "match": { "content": "Elasticsearch" } }
    }
  }
}
```

**使用约束**：

- 父子文档必须位于同一分片（通过 `_routing` 保证）。
- `has_child` 查询性能较差（需要遍历所有子文档）。
- `join` 会导致索引维护开销增大。
- **官方建议**：除非确有必要，优先使用 `nested`（性能更好）；如果关联需求复杂，考虑将数据拆分到不同索引中，在应用层做 JOIN。

---

## 五、分页方案

ES 提供了多种分页方案，各有优劣，选择合适的方案对系统性能至关重要。

### 5.1 from + size 浅分页

最基础的分页方式：

```json
GET /my_index/_search
{
  "from": 0,
  "size": 10,
  "query": {
    "match_all": {}
  }
}
```

**工作原理**：

- 协调节点将请求广播到所有相关分片。
- 每个分片返回 `from + size` 条结果（如第 3 页，每页 10 条，则每个分片返回 30 条）。
- 协调节点对全部分片返回的结果进行排序，取全局第 `from` 到 `from + size` 条。

**性能瓶颈**：

- 如果索引有 5 个分片，查第 10000 页（`from: 99990, size: 10`），每个分片需要返回 100000 条记录，协调节点需要排序 500000 条记录，然后再截取 10 条。
- `index.max_result_window` 默认值为 `10000`（即 `from + size` 的默认上限），超过时会报错。
- 深度分页（Deep Pagination）时，CPU、内存和网络开销呈线性增长。

**适用场景**：数据量小（条数 < 10000），或者只涉及前几页的页面导航。

### 5.2 scroll 快照遍历

Scroll 适用于**全量数据导出**场景，如将 ES 数据导出到 Hadoop、数据迁移、批量重建索引等。

```json
POST /my_index/_search?scroll=5m
{
  "size": 1000,
  "query": { "match_all": {} }
}
```

返回结果中的 `_scroll_id` 用于后续请求：

```json
POST /_search/scroll
{
  "scroll": "5m",
  "scroll_id": "DXF1ZXJ5QW5kRmV0Y2gBAAAAAAAA..."
}
```

**关键特性**：

- **快照语义**：scroll 创建时生成一个数据快照，后续遍历基于这个快照。即使新数据写入，也不会出现在遍历结果中（非实时）。
- **上下文开销**：scroll 期间需要在 ES 节点上保持**搜索上下文**（Search Context），占用内存。超时时间（`scroll=5m`）意味着如果 5 分钟内没有执行下一个 scroll 请求，上下文会被自动清理。
- 深度分页稳定：无论数据多少，每次只返回 `size` 条记录，性能平滑。

**适用场景**：全量导出、数据迁移、后台批处理任务。**不适合**需要实时数据的用户交互式分页。

### 5.3 search_after 游标分页

search_after 是 ES 官方推荐的**深度分页**方案，它利用上一页的最后一条记录的排序值作为下一页的起点。

**第一步**：发起搜索请求，获取排序值。

```json
GET /my_index/_search
{
  "size": 10,
  "query": { "match_all": {} },
  "sort": [
    { "create_time": { "order": "desc" } },
    { "_id": { "order": "asc" } }
  ]
}
```

**第二步**：从返回结果的 `sort` 字段获取最后一条记录的排序值，用于下一页。

```json
GET /my_index/_search
{
  "size": 10,
  "query": { "match_all": {} },
  "search_after": ["2024-12-01T10:30:00.000Z", "doc_123"],
  "sort": [
    { "create_time": { "order": "desc" } },
    { "_id": { "order": "asc" } }
  ]
}
```

**核心要求**：

1. **排序值必须唯一**：如果排序值有重复，分页结果可能不稳定（同一页数据可能出现在不同页中）。解决方案：在排序字段列表末尾加上 `_id`（文档 ID 全局唯一）。
2. **不能跳页**：`search_after` 只能基于上一页的结果请求下一页，不支持直接跳转到第 N 页。这是有意为之的设计——ET 认为深度分页中的跳页没有实际意义。
3. **实时性**：每次查询都是基于当前索引的最新数据（不同于 scroll 的快照语义）。

**适用场景**：用户滚动加载（无限滚动）、深度分页（数千页之后）、后台翻页任务。

### 5.4 PIT（Point In Time） —— ES 7.11+ 增强版

PIT 是对 `search_after` 的增强。`search_after` 在长时间翻页过程中，如果索引数据发生较大变动（如段合并），可能导致 `search_after` 的稳定性下降。PIT 通过打开一个**轻量级的数据视图**（类似 Scroll 但更轻量），解决了这个问题。

**第一步**：打开一个 PIT。

```json
POST /my_index/_pit?keep_alive=5m
```

返回 `pit_id`。

**第二步**：基于 PIT 进行 `search_after` 分页。

```json
GET /_search
{
  "size": 10,
  "query": { "match_all": {} },
  "pit": {
    "id": "PIT_ID",
    "keep_alive": "5m"
  },
  "sort": [
    { "create_time": { "order": "desc" } }
  ],
  "search_after": ["2024-12-01T10:30:00.000Z"]
}
```

**PIT 与 Scroll 的对比**：

| 特性 | Scroll | PIT + search_after |
|------|--------|-------------------|
| 数据一致性 | 快照（静态） | 轻量视图，持续更新 |
| 内存开销 | 较高（完整搜索上下文） | 较低 |
| 实时性 | 非实时 | 准实时 |
| 推荐场景 | 全量数据导出 | 深度分页、实时翻页 |
| 版本支持 | 全版本 | ES 7.11+ |

### 5.5 分页方案总结

| 方案 | 适用场景 | 深度分页支持 | 实时性 | 跳页 | 性能 |
|------|---------|-------------|--------|------|------|
| from + size | 浅分页（前几页） | 不支持（有限制） | 实时 | 支持 | 深度分页极差 |
| scroll | 全量导出、批量迁移 | 稳定支持 | 快照（非实时） | 不支持 | 批量场景好 |
| search_after | 无限滚动、深度翻页 | 稳定支持 | 实时 | 不支持 | 深度分页好 |
| PIT + search_after | ES 7.11+ 深度分页 | 稳定支持 | 准实时 | 不支持 | 深度分页好 |

---

## 六、搜索优化

### 6.1 索引设计优化

索引设计阶段做对决策，能避免后期大量的优化工作。

#### 字段建模原则

**1. 区分 text 与 keyword**

- 需要全文搜索的字段 → `text`，指定合适的分词器。
- 需要精确匹配、聚合、排序的字段 → `keyword`。
- 同一字段兼具两种需求 → 使用 `fields` 多字段模式。

**2. 尽量禁用不需要的能力**

以下映射参数会占用额外资源，如果不需要，应当显式禁用：

| 参数 | 作用 | 禁用方式 | 节省资源 |
|------|------|---------|---------|
| `enabled` | 是否索引该字段 | `"enabled": false` | 完全跳过索引构建，适合仅存储不搜索的字段 |
| `index` | 是否倒排索引 | `"index": false` | 节省倒排索引的存储空间 |
| `doc_values` | 列式存储（用于聚合、排序） | `"doc_values": false` | 节省磁盘空间，如果不对该字段聚合排序可禁用 |
| `norms` | 归一化因子（相关性打分） | `"norms": false` | 节省内存，适合 filter 字段或无需打分的字段 |
| `store` | 是否单独存储字段值 | `"store": false`（默认） | ES 默认从 `_source` 取字段，一般不需要单独 store |

示例——对不需要搜索的字段禁用索引：

```json
{
  "mappings": {
    "properties": {
      "internal_remark": {
        "type": "text",
        "index": false,
        "doc_values": false,
        "norms": false
      },
      "log_level": {
        "type": "keyword",
        "norms": false,
        "doc_values": true
      },
      "large_text_field": {
        "enabled": false
      }
    }
  }
}
```

### 6.2 中文分词

中文分词是所有中文 ES 应用中最关键的环节。与英文不同，中文文本词与词之间没有自然分隔符，必须依赖分词器来切分。

#### IK Analyzer

IK 是目前应用最广泛的中文分词器，支持两种分词模式：

**ik_smart**（智能切分模式）：

- 输出最粗粒度的分词结果，准确率高。
- 适合搜索场景（减少无关词条干扰）。

输入："Elasticsearch是一个分布式搜索引擎"
输出：`Elasticsearch / 是 / 一个 / 分布式 / 搜索引擎`

**ik_max_word**（最大切分模式）：

- 输出所有可能的分词结果，召回率高。
- 适合索引场景（尽可能多覆盖可能的搜索词条）。

输入："Elasticsearch是一个分布式搜索引擎"
输出：`Elasticsearch / 是 / 一个 / 分布式 / 分布 / 搜索 / 搜索引擎 / 引擎`

**推荐策略**：索引时使用 `ik_max_word` 保证召回率，搜索时使用 `ik_smart` 保证准确率。通过 `search_analyzer` 参数配置：

```json
{
  "mappings": {
    "properties": {
      "title": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart"
      }
    }
  }
}
```

#### 自定义词库

IK 分词器支持自定义词库，用于识别业务专属词汇：

1. 在 IK 配置目录（`ES_HOME/plugins/ik/config/`）下创建自定义词典文件，如 `custom.dic`。
2. 编辑 `IKAnalyzer.cfg.xml`，添加：

```xml
<properties>
    <comment>IK分词器配置</entry>
    <entry key="ext_dict">custom.dic</entry>
</properties>
```

3. 词典文件格式：每行一个词条。

```
火山引擎
大语言模型
向量数据库
混合现实
```

#### pinyin 分词

支持拼音搜索（通过 `elasticsearch-analysis-pinyin` 插件）：

```json
{
  "mappings": {
    "properties": {
      "product_name": {
        "type": "text",
        "analyzer": "pinyin_analyzer"
      }
    }
  }
}
```

输入："华为手机"
输出：`huawei`、`华为`、`shouji`、`手机`、`hw`、`sj`

适合实现"输入拼音首字母快速搜索"的场景。

### 6.3 搜索性能优化

#### 路由优化

默认情况下，ES 将搜索请求广播到索引的所有分片（主分片和副本分片）。如果搜索场景有明显的业务分区（如按用户 ID、租户 ID、地域），可以通过自定义 `_routing` 将查询限定在部分分片中：

```json
GET /my_index/_search?routing=user_123
{
  "query": {
    "match": {
      "title": "搜索关键词"
    }
  }
}
```

效果：协调节点只会将请求发送到 `_routing` 对应的分片，跳过其他分片，**减少 50%~90% 的集群查询压力**（取决于分片数量）。

#### 强制合并

对于**只读索引**（如历史日志索引、归档数据），可以执行 `force merge` 将其合并为单个 Segment，显著提升查询性能并释放磁盘空间：

```json
POST /my_archive_index/_forcemerge?max_num_segments=1
```

**注意事项**：

- `force merge` 是 I/O 密集型操作，会消耗大量磁盘 I/O 和 CPU，建议在低峰期执行。
- **不要对正在频繁写入的索引执行 `force merge`**，因为合并后的 Segment 很快会被新写入的数据再次分裂。
- `max_num_segments=1` 将索引合并为单个 Segment，查询性能最佳，但合并开销最大。

#### 冷热分离架构

在集群层面，将不同热度的数据分配到不同配置的节点上：

| 节点角色 | 节点配置 | 存储数据 |
|---------|---------|---------|
| Hot 节点 | 高性能 SSD，大内存 | 近期数据（近 7 天），频繁写入和查询 |
| Warm 节点 | 普通 SSD | 中期数据（7~30 天），偶尔查询 |
| Cold 节点 | HDD，廉价存储 | 历史数据（30 天以上），极少查询 |

通过索引生命周期管理（ILM, Index Lifecycle Management）自动实现数据在不同层级间的流转：

```json
PUT _ilm/policy/my_policy
{
  "policy": {
    "phases": {
      "hot": {
        "min_age": "0ms",
        "actions": {
          "rollover": {
            "max_size": "50GB",
            "max_age": "1d"
          },
          "set_priority": { "priority": 100 }
        }
      },
      "warm": {
        "min_age": "7d",
        "actions": {
          "forcemerge": { "max_num_segments": 1 },
          "allocate": { "require": { "data_type": "warm" } },
          "set_priority": { "priority": 50 }
        }
      },
      "cold": {
        "min_age": "30d",
        "actions": {
          "allocate": { "require": { "data_type": "cold" } },
          "set_priority": { "priority": 0 }
        }
      },
      "delete": {
        "min_age": "90d",
        "actions": {
          "delete": {}
        }
      }
    }
  }
}
```

---

## 七、Spring Boot 集成

### 7.1 Spring Data Elasticsearch

Spring Data Elasticsearch 是 Spring Data 家族中用于简化 ES 开发的模块，也是 Spring Boot 集成 ES 的首选方式。

#### 依赖引入

Maven（Spring Boot 3.x，对应 ES 8.x）：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
```

#### 配置

```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
    connection-timeout: 10s
    socket-timeout: 30s
    username: elastic
    password: your_password
```

#### 实体类与@Document

```java
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Document(indexName = "products", createIndex = true)
public class Product {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String description;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Double)
    private Double price;

    @Field(type = FieldType.Integer)
    private Integer stock;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime createTime;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Nested)
    private List<SpecItem> specs;

    @Field(type = FieldType.Keyword)
    private String status;
}

@Data
public class SpecItem {
    @Field(type = FieldType.Keyword)
    private String name;

    @Field(type = FieldType.Keyword)
    private String value;
}
```

#### Repository 层

```java
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends ElasticsearchRepository<Product, String> {

    // 方法命名查询：ES会根据方法名自动解析查询
    List<Product> findByTitle(String title);

    List<Product> findByPriceBetween(Double min, Double max);

    List<Product> findByCategoryAndStatus(String category, String status);

    List<Product> findByTagsIn(List<String> tags);
}
```

#### 复杂查询：ElasticsearchRestTemplate + NativeSearchQueryBuilder

对于复杂查询（如 bool 组合、高亮、聚合），需要使用 `ElasticsearchRestTemplate`：

```java
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.elasticsearch.search.aggregations.AggregationBuilders.*;

@Service
public class ProductSearchService {

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    public SearchHits<Product> searchProducts(String keyword, Double minPrice,
                                              Double maxPrice, String category, int page, int size) {
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();

        // 1. 构建 bool 查询
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        // must: 全文搜索（影响评分）
        if (keyword != null && !keyword.isEmpty()) {
            boolQuery.must(QueryBuilders.matchQuery("title", keyword));
        }

        // filter: 精确筛选（不计算评分，自动缓存）
        if (minPrice != null || maxPrice != null) {
            boolQuery.filter(QueryBuilders.rangeQuery("price")
                .gte(minPrice != null ? minPrice : 0)
                .lte(maxPrice != null ? maxPrice : Double.MAX_VALUE));
        }
        if (category != null && !category.isEmpty()) {
            boolQuery.filter(QueryBuilders.termQuery("category.keyword", category));
        }
        boolQuery.filter(QueryBuilders.termQuery("status.keyword", "ACTIVE"));

        queryBuilder.withQuery(boolQuery);

        // 2. 分页
        queryBuilder.withPageable(PageRequest.of(page, size));

        // 3. 排序
        queryBuilder.withSort(Sort.by(Sort.Direction.DESC, "price"));

        // 4. 高亮显示
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");
        highlightBuilder.preTags("<em>");
        highlightBuilder.postTags("</em>");
        queryBuilder.withHighlightBuilder(highlightBuilder);

        // 5. 聚合
        queryBuilder.addAggregation(
            AggregationBuilders.terms("category_agg")
                .field("category.keyword")
                .size(20)
        );
        queryBuilder.addAggregation(
            AggregationBuilders.stats("price_stats")
                .field("price")
        );

        // 执行查询
        return elasticsearchRestTemplate.search(
            queryBuilder.build(),
            Product.class
        );
    }
}
```

#### 结果解析与高亮处理

```java
@Service
public class ProductSearchService {

    // 上面的 searchProducts 方法...

    public Map<String, Object> searchWithHighlight(String keyword, int page, int size) {
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        queryBuilder.withQuery(QueryBuilders.matchQuery("title", keyword));
        queryBuilder.withPageable(PageRequest.of(page, size));

        // 高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title").preTags("<span class='highlight'>").postTags("</span>");
        queryBuilder.withHighlightBuilder(highlightBuilder);

        SearchHits<Product> searchHits = elasticsearchRestTemplate.search(
            queryBuilder.build(), Product.class);

        List<Map<String, Object>> resultList = new ArrayList<>();
        for (SearchHit<Product> hit : searchHits) {
            Product product = hit.getContent();
            Map<String, Object> item = new HashMap<>();
            item.put("id", product.getId());
            item.put("title", product.getTitle());
            item.put("price", product.getPrice());

            // 提取高亮片段
            List<String> highlightedTitles = hit.getHighlightField("title");
            if (highlightedTitles != null && !highlightedTitles.isEmpty()) {
                item.put("highlightTitle", highlightedTitles.get(0));
            }
            resultList.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", searchHits.getTotalHits());
        result.put("products", resultList);
        return result;
    }
}
```

### 7.2 批量操作

大批量数据写入时，使用单个文档逐条写入的性能极差，必须使用批量 API。

```java
@Service
public class ProductBulkService {

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    public void bulkInsert(List<Product> products) {
        List<IndexQuery> queries = products.stream()
            .map(product -> new IndexQueryBuilder()
                .withId(product.getId())
                .withObject(product)
                .build())
            .collect(Collectors.toList());

        BulkOperations bulkOperations = elasticsearchRestTemplate
            .opsForBulk()
            .bulkOperation(IndexOperations.IndexAction.INDEX);

        bulkOperations.add(queries);
        BulkResponse response = bulkOperations.execute();

        if (response.hasFailures()) {
            log.error("批量写入失败: {}", response.buildFailureMessage());
        } else {
            log.info("成功写入 {} 条文档", products.size());
        }
    }

    public void bulkUpdate(List<Product> products) {
        BulkOperations bulkOperations = elasticsearchRestTemplate
            .opsForBulk()
            .bulkOperation(IndexOperations.IndexAction.UPDATE);

        List<UpdateQuery> queries = products.stream()
            .map(product -> UpdateQuery.builder(product.getId())
                .withDocument(elasticsearchRestTemplate
                    .getElasticsearchConverter()
                    .mapObject(product))
                .build())
            .collect(Collectors.toList());

        bulkOperations.add(queries);
        bulkOperations.execute();
    }
}
```

**批量操作最佳实践**：

- 每个批量请求包含 1000~5000 条文档，或总大小不超过 10MB。
- 使用 `bulkProcessor` 异步批量处理器，自动控制批量大小和刷新间隔。
- 写入期间可以临时调大 `refresh_interval`（如 `-1s` 或 `30s`），写入完成后恢复默认值，减少段合并频率。

### 7.3 索引重建与别名切换

当 Mapping 需要变更（如修改字段类型、添加新的分词器配置）时，由于 ES 不支持直接修改已有字段类型，需要通过**重建索引+别名切换**的方式实现零停机迁移。

```java
@Service
public class IndexReindexService {

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    /**
     * 零停机索引重建
     * 步骤:
     * 1. 创建新索引（带新Mapping）
     * 2. Reindex 数据（从旧索引迁移到新索引）
     * 3. 切换别名（将别名从旧索引指向新索引）
     * 4. 删除旧索引
     */
    public void reindexWithAlias(String oldIndex, String newIndex, String aliasName) {
        // 1. 创建新索引（假设新索引的Mapping已在ES中手动创建完成）
        boolean indexCreated = elasticsearchRestTemplate.indexOps(
            IndexCoordinates.of(newIndex)).create();
        if (!indexCreated) {
            throw new RuntimeException("创建新索引失败: " + newIndex);
        }

        // 2. Reindex 数据
        ReindexRequest reindexRequest = new ReindexRequest();
        reindexRequest.setSourceIndex(oldIndex);
        reindexRequest.setDestIndex(newIndex);
        reindexRequest.setConflicts("proceed");
        reindexRequest.setMaxDocs(100000); // 控制每批迁移数量

        ReindexResponse reindexResponse = elasticsearchRestTemplate.reindex(
            reindexRequest, String.class);
        log.info("Reindex 完成: 总数量={}, 失败数={}",
            reindexResponse.getTotal(), reindexResponse.getFailures().size());

        // 3. 别名切换（使用ES的 _aliases API）
        AliasActions aliasActions = new AliasActions();
        // 将旧别名从旧索引移除
        aliasActions.add(new AliasAction.Remove(
            AliasActionParameters.builder()
                .withIndices(oldIndex)
                .withAliases(aliasName)
                .build()
        ));
        // 将别名添加到新索引
        aliasActions.add(new AliasAction.Add(
            AliasActionParameters.builder()
                .withIndices(newIndex)
                .withAliases(aliasName)
                .build()
        ));

        elasticsearchRestTemplate.indexOps(IndexCoordinates.of(oldIndex, newIndex))
            .alias(aliasActions);

        // 4. 可选：删除旧索引
        // elasticsearchRestTemplate.indexOps(IndexCoordinates.of(oldIndex)).delete();
    }
}
```

**使用别名进行搜索**：

```java
// 在Repository中指定别名
@Document(indexName = "products_alias")  // 指向别名而非具体索引
public class Product {
    // ...
}
```

通过别名机制，应用层只需要知道别名即可。索引重建时，应用完全无感知，实现了**零停机迁移**。

---

## 八、总结

本文从 Elasticsearch 的倒排索引核心原理出发，系统讲解了 ES 的核心概念、文档与索引建模、各类查询 DSL、分页方案、搜索优化策略以及 Spring Boot 集成实践。以下是一份实践要点清单，供日常开发参考：

### 实践要点清单

**基础认知**
- [ ] 理解 ES 是基于 Lucene 的分布式搜索引擎，核心优势在全文搜索和聚合分析
- [ ] 明确 ES 与 MySQL 的定位差异：互补而非替代
- [ ] 掌握倒排索引的三层结构：Term Index（FST 内存加速）→ Term Dictionary（磁盘排序词表）→ Posting List（文档 ID 列表 + 位置信息）

**索引建模**
- [ ] Production 环境使用显式 Mapping，关闭 `dynamic` 或设为 `strict`
- [ ] 区分 text 和 keyword 的使用场景，利用多字段特性兼顾搜索和聚合
- [ ] 合理设置分片数量，避免过度分片（建议每个分片 10-50GB）
- [ ] 禁用不需要的能力：`index: false`、`doc_values: false`、`norms: false`

**查询 DSL**
- [ ] 全文搜索优先使用 `match`，短语匹配使用 `match_phrase`
- [ ] 精确值查询使用 `term`/`terms`（配合 keyword 字段）
- [ ] 使用 `bool` 复合查询时，将不影响相关性的条件放入 `filter`（自动缓存，性能更优）
- [ ] 利用聚合分析实现分组统计、直方图、基数去重等功能

**分页策略**
- [ ] 浅分页（前几页）使用 `from + size`
- [ ] 全量数据导出使用 `scroll`（注意设置合理的超时时间）
- [ ] 深度分页推荐 `search_after`（ES 7.11+ 可配合 PIT）
- [ ] 避免深度分页中的跳页需求（产品设计上改为"加载更多"）

**搜索优化**
- [ ] 中文场景安装 IK Analyzer，配置索引时 `ik_max_word` + 搜索时 `ik_smart`
- [ ] 利用自定义路由减少搜索涉及的 shard 数
- [ ] 只读索引执行 force merge 提升查询性能
- [ ] 实施冷热分离架构，配合 ILM 自动管理索引生命周期

**Spring Boot 集成**
- [ ] 使用 `@Document` 注解映射实体类
- [ ] 简单查询使用 `ElasticsearchRepository` 方法命名查询
- [ ] 复杂查询使用 `ElasticsearchRestTemplate` + `NativeSearchQueryBuilder`
- [ ] 批量写入使用 `BulkOperations`，控制每批大小 1000~5000 条
- [ ] 索引重建使用 Reindex API + 别名切换，实现零停机迁移

---

> **延伸阅读**：Elasticsearch 的技术体系远不止本文所述，还包括集群部署与高可用配置、性能调优（JVM 参数、线程池配置）、安全认证（X-Pack）、SQL 访问接口、机器学习异常检测等高级功能。建议在实战中逐步深入探索。
