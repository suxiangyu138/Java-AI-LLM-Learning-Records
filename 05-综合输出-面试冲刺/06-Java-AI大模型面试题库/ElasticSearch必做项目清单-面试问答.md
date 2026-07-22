# ElasticSearch 面试问答清单
> 🎯 基于项目实战清单，涵盖面试高频问题与完美解答方案，帮助你在面试中脱颖而出。

## 目录
1. [基础概念与核心原理](#1-基础概念与核心原理)
2. [项目实战深度问答](#2-项目实战深度问答)
3. [进阶与系统设计](#3-进阶与系统设计)
4. [场景题与故障排查](#4-场景题与故障排查)

---

## 1. 基础概念与核心原理

> 💡 面试官在这一环节考察你的基本功是否扎实

### Q1：ES 中的倒排索引是什么？为什么它比 B+ 树更适合全文搜索？
**面试官意图：** 考察对 ES 核心原理的理解，判断你是否理解 ES 为什么能实现毫秒级搜索。

**完美解答：**

**倒排索引的结构：**

传统的 B+ 树索引是"文档 → 关键词"的正向索引，而倒排索引是 **"关键词 → 文档列表"** 的反向映射：

```
倒排索引示例：
┌──────────┬──────────────────────────────┐
│ 关键词    │ 文档列表（文档ID + 位置信息）     │
├──────────┼──────────────────────────────┤
│ Java     │ doc1:pos1, doc2:pos3, doc3:pos5  │
│ Spring   │ doc1:pos2, doc2:pos1             │
│ Redis    │ doc3:pos2, doc4:pos1             │
│ 缓存     │ doc3:pos4, doc4:pos3             │
└──────────┴──────────────────────────────┘
```

**为什么倒排索引适合全文搜索：**

| 维度 | B+ 树（MySQL 方案） | 倒排索引（ES 方案） |
|------|-------------------|-------------------|
| 搜索方式 | LIKE '%keyword%' 全表扫描 | 直接查关键词对应文档列表 |
| 时间复杂度 | O(N) 全表扫描 | O(1) 哈希定位 |
| 分词支持 | 不支持 | 天然支持（IK、standard 等分词器） |
| 相关性排序 | 不支持 | TF-IDF / BM25 评分排序 |
| 模糊匹配 | 只支持前缀 LIKE | 支持通配符、正则、短语匹配 |
| 千万级数据 | 秒级到分钟级 | 毫秒级 |

**核心流程：**
```
原文： "Java 是一种编程语言"
         ↓ 分词器（Analyzer）
[Java] [ ] [是] [一] [种] [编程] [语言]
         ↓ 建立倒排索引
Java → {doc1:pos0}    编程 → {doc1:pos4}
语言 → {doc1:pos5}    ...

搜索 "Java 编程"：
1. 分词：Java, 编程
2. 查倒排索引：Java→doc1, 编程→doc1
3. 合并结果：doc1（评分合并）
```

> 💡 **面试金句**：如果说 MySQL 的 B+ 树是为"我知道我要查什么"设计的（精确查找），那么 ES 的倒排索引就是为"我不确定具体是什么，但我知道大概长什么样"设计的（模糊搜索）。

---

### Q2：ES 中 Mapping 是什么？有哪些字段类型？如何定义一个索引的 Mapping？
**面试官意图：** 考察对 ES 数据建模的基础能力。

**完美解答：**

**Mapping 的作用：** 相当于 MySQL 的表结构定义，指定了每个字段的类型、分词器、索引方式等。

**常用字段类型：**

| 分类 | 字段类型 | 说明 |
|------|---------|------|
| **文本类** | `text` | 全文检索，会被分词，支持模糊搜索 |
| | `keyword` | 精确匹配，不分词，适合排序和聚合 |
| **数值类** | `integer`, `long`, `float`, `double` | 数值类型，支持范围查询 |
| **时间类** | `date` | 日期类型，支持时间范围查询 |
| **布尔类** | `boolean` | true/false |
| **地理位置** | `geo_point` | 经纬度，支持地理距离查询 |
| **嵌套类** | `nested` | 嵌套对象（独立索引） |
| | `object` | JSON 对象（默认，扁平化存储） |

**自定义 Mapping 示例（商品索引）：**
```json
PUT /products
{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 1,
    "analysis": {
      "analyzer": {
        "ik_smart_analyzer": {
          "type": "custom",
          "tokenizer": "ik_smart"
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "product_id": { "type": "keyword" },
      "product_name": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart",
        "fields": {
          "keyword": { "type": "keyword" }
        }
      },
      "category": { "type": "keyword" },
      "price": { "type": "double" },
      "brand": { "type": "keyword" },
      "description": {
        "type": "text",
        "analyzer": "ik_max_word"
      },
      "tags": { "type": "keyword" },
      "sales_count": { "type": "integer" },
      "create_time": { "type": "date", "format": "yyyy-MM-dd HH:mm:ss" },
      "is_active": { "type": "boolean" }
    }
  }
}
```

> ⚠️ **注意**：`text` 类型字段默认会被分词，精确匹配和聚合时用 `.keyword`（如 `product_name.keyword`）。`text` 和 `keyword` 的选型取决于查询方式——搜索用 `text`，筛选/排序/分组用 `keyword`。

---

### Q3：ES 中 Query DSL 的 must、should、filter、must_not 有什么区别？
**面试官意图：** 考察 ES 查询语法的基础知识，以及是否能写出高效的复合查询。

**完美解答：**

**Bool 查询四个子句对比：**

| 子句 | 作用 | 是否参与评分 | 是否使用缓存 | 相当于 SQL |
|------|------|:----------:|:----------:|-----------|
| **must** | 必须匹配，类似 AND | 是 | 否 | `AND` + 影响排序 |
| **filter** | 必须匹配，类似 AND | **否**（不计分） | **是**（结果缓存） | `AND`（仅过滤） |
| **should** | 至少匹配一个，类似 OR | 是（匹配越多分越高） | 否 | `OR` |
| **must_not** | 必须不匹配 | 否 | 是 | `NOT IN / !=` |

**复合查询示例——电商高级筛选：**
```json
{
  "query": {
    "bool": {
      "must": [
        { "match": { "product_name": "手机" } },
        { "match": { "description": "5G" } }
      ],
      "filter": [
        { "term": { "category": "数码" } },
        { "range": { "price": { "gte": 2000, "lte": 8000 } } },
        { "terms": { "brand": ["华为", "小米", "OPPO"] } },
        { "term": { "is_active": true } }
      ],
      "should": [
        { "match": { "product_name": "旗舰" } },
        { "range": { "sales_count": { "gte": 10000 } } }
      ],
      "must_not": [
        { "term": { "status": "下架" } }
      ]
    }
  },
  "sort": [
    { "sales_count": { "order": "desc" } }
  ]
}
```

> 💡 **性能优化关键**：能用 `filter` 的千万别用 `must`！`filter` 不评分且结果可缓存，在 99% 的过滤场景（分类、价格区间、状态筛选）中用 `filter`，只对全文检索的匹配结果用 `must`。

---

## 2. 项目实战深度问答

> 💡 面试官会深挖你的项目细节，验证你"真的做过"而不是"背过"

### Q4：MySQL 中的数据如何同步到 ES？如何保证数据一致性？
**面试官意图：** 考察数据同步方案的实战能力，这是 ES 落地的核心场景。

**完美解答：**

**四种同步方案对比：**

| 方案 | 实时性 | 复杂度 | 数据一致性 | 适用场景 |
|------|:-----:|:------:|:----------:|---------|
| **Logstash 定时同步** | 分钟级延迟 | 低 | 最终一致 | 全量同步、日志分析 |
| **Canal 监听 binlog** | 秒级延迟 | 中 | 最终一致 | 增量同步、实时性要求高 |
| **双写（应用层同步）** | 实时 | 低 | 可能不一致 | 小规模、简单场景 |
| **定时任务扫描** | 分钟级 | 低 | 最终一致 | 数据量大、一致性要求不高 |

**推荐方案：Canal 监听 binlog 增量同步**

```mermaid
MySQL binlog → Canal → MQ(可选) → ES 消费者 → ES 索引
```

```java
// 1. Canal 客户端监听 binlog
@Component
public class CanalClient {

    @PostConstruct
    public void start() {
        CanalConnector connector = CanalConnectors.newSingleConnector(
            new InetSocketAddress("127.0.0.1", 11111), "example", "", "");

        connector.connect();
        connector.subscribe("product_db.product_table");

        while (true) {
            Message message = connector.getWithoutAck(100); // 批量拉取
            long batchId = message.getId();
            int size = message.getEntries().size();
            if (batchId == -1 || size == 0) {
                Thread.sleep(1000);
                continue;
            }

            for (CanalEntry.Entry entry : message.getEntries()) {
                if (entry.getEntryType() == CanalEntry.EntryType.ROWDATA) {
                    processEntry(entry);
                }
            }
            connector.ack(batchId);
        }
    }

    private void processEntry(CanalEntry.Entry entry) {
        CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
        CanalEntry.EventType eventType = rowChange.getEventType();

        for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
            if (eventType == CanalEntry.EventType.DELETE) {
                // 3. 删除 ES 文档
                deleteFromES(getId(rowData.getBeforeColumnsList()));
            } else if (eventType == CanalEntry.EventType.INSERT || 
                       eventType == CanalEntry.EventType.UPDATE) {
                // 2. 构建 ES 文档
                Map<String, Object> doc = buildESDoc(rowData.getAfterColumnsList());
                // 3. 同步到 ES
                indexToES(doc);
            }
        }
    }
}
```

**数据一致性保障：**
```
正常流程：MySQL 更新 → Canal 监听 → 更新 ES
     ↓
补偿流程：定时任务（每 5 分钟）对比 MySQL 和 ES 数据差异
     ↓
异常处理：异步重试队列 → 死信队列 → 人工介入
```

> 🎯 **核心原则**：Canal 的 binlog 监听是最成熟可靠的方案。如果要求秒级同步，用 Canal + MQ 异步处理；如果分钟级就足够，Logstash 是最简单的方式。

---

### Q5：ES 在海量数据下如何进行深度分页优化？
**面试官意图：** 考察对 ES 分页机制的深入理解，以及在实际高并发场景中的优化能力。

**完美解答：**

**三种分页方案对比：**

| 方案 | 实现方式 | 优点 | 缺点 | 适用场景 |
|------|---------|------|------|---------|
| **from/size** | 普通分页 `from=0, size=10` | 使用简单 | 深度分页性能极差（每页都要排序全部结果） | 浅分页（前几百条） |
| **Scroll** | 快照式游标 | 适合大数据量导出 | 维护快照、有状态 | 批量导出、后台任务 |
| **search_after** | 游标式跳转 | 深度分页性能好、无状态 | 不能跳页 | **无限滚动加载（推荐）** |

**性能对比（百万级数据）：**

```
from=0, size=10      → 1ms    （能接受）
from=10000, size=10  → 800ms  （已经慢了）
from=100000, size=10 → 3s     （不可接受）
from=1000000, size=10→ OOM    （可能崩溃）
```

**推荐方案——search_after：**

```json
// 第一页查询
GET /products/_search
{
  "size": 10,
  "sort": [
    { "sales_count": "desc" },
    { "product_id": "asc" }  // 必须有一个唯一值字段兜底
  ]
}

// 返回结果中包含 sort_values
// 第二页：传入上一页最后一个文档的 sort_values
GET /products/_search
{
  "size": 10,
  "search_after": [9999, "PROD_100123"],
  "sort": [
    { "sales_count": "desc" },
    { "product_id": "asc" }
  ]
}
```

```java
@Service
public class ProductSearchService {

    @Autowired
    private RestHighLevelClient client;

    public SearchResponse searchWithCursor(String keyword, Object[] searchAfter, int size) {
        SearchRequest searchRequest = new SearchRequest("products");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        // 构建查询
        sourceBuilder.query(QueryBuilders.matchQuery("product_name", keyword));
        sourceBuilder.size(size);

        // 排序（必须与 search_after 一致）
        sourceBuilder.sort("sales_count", SortOrder.DESC);
        sourceBuilder.sort("product_id", SortOrder.ASC);

        // 设置游标
        if (searchAfter != null) {
            sourceBuilder.searchAfter(searchAfter);
        }

        searchRequest.source(sourceBuilder);
        return client.search(searchRequest, RequestOptions.DEFAULT);
    }
}
```

> ⚠️ **千万别用 `from + size` 做深度分页！** 前端的"翻页"场景如果用户只翻前几页，用 from/size 没问题。如果是"无限滚动"（如抖音下拉加载），必须用 search_after。如果是导出全量数据，用 Scroll。

---

### Q6：ELK 日志平台在实际生产环境中怎么搭建的？遇到过什么问题？
**面试官意图：** 考察日志系统的工程化落地经验。

**完美解答：**

**标准 ELK 架构：**
```
应用服务 → Filebeat（日志采集）→ Logstash（过滤解析）→ ES（存储）→ Kibana（可视化）
               ↓                              ↓
          采集性能好                     复杂的日志解析
```

**生产架构参考：**
```
            → Logstash(解析) → ES node1
应用 A      → Logstash(解析) → ES node2
(Filebeat)  → Logstash(解析) → ES node3
应用 B      → Logstash(解析) → ES node4
(Filebeat)  → Logstash(解析) → ES node5
                    ↓
             Kibana 查询 [所有 ES 节点]
```

**实践中的问题与解决方案：**

| 问题 | 表现 | 解决方案 |
|------|------|---------|
| **日志格式不统一** | 各团队日志格式不同，解析困难 | 统一日志规范，使用 MDC 添加 traceId、userId |
| **ES 索引爆炸** | 每天产生大量索引，管理困难 | 使用 ILM（索引生命周期管理），30 天自动删除 |
| **日志丢失** | Filebeat 压力大时丢日志 | Filebeat 增加 `queue.mem.events`，开启重试 |
| **检索慢** | 大跨度时间查询卡死 | 时间范围固定为一天，跨天用 Scroll |
| **磁盘空间不足** | 日志量太大 | 配置 ILM 策略，冷数据迁移到冷节点或 S3 |

**生产级索引模板：**
```json
PUT _index_template/logs_template
{
  "index_patterns": ["app-logs-*"],
  "template": {
    "settings": {
      "number_of_shards": 3,
      "number_of_replicas": 1
    },
    "mappings": {
      "properties": {
        "app_name":   { "type": "keyword" },
        "level":      { "type": "keyword" },
        "message":    { "type": "text", "analyzer": "ik_smart" },
        "trace_id":   { "type": "keyword" },
        "class_name": { "type": "keyword" },
        "timestamp":  { "type": "date" },
        "cost_ms":    { "type": "integer" },
        "ip":         { "type": "ip" }
      }
    }
  }
}
```

> 💡 **最佳实践**：不要试图把所有日志都塞进 ES。结构化业务日志进 ES 用于检索和告警，非结构化的大日志（如 Nginx access log）进 S3 或 HDFS 归档。

---

## 3. 进阶与系统设计

> 💡 这是拉开差距的环节，考察架构思维

### Q7：ES 集群的节点角色有哪些？如何规划集群架构？
**面试官意图：** 考察对 ES 分布式架构的理解和集群设计能力。

**完美解答：**

**节点角色及职责：**

| 节点类型 | 角色 | 主要职责 | 配置 |
|----------|------|---------|------|
| **主节点（Master）** | 管理者 | 管理集群元数据、分配分片、选举决策 | `node.roles: [master]` |
| **数据节点（Data）** | 存储者 | 存储数据、执行 CRUD、聚合查询 | `node.roles: [data]` |
| **协调节点（Coordinating）** | 路由器 | 接收请求、分发到数据节点、汇总结果 | `node.roles: []` |
| **热节点（Hot）** | 热数据 | SSD 存储，高频读写 | 搭配 ILM 使用 |
| **冷节点（Warm）** | 冷数据 | HDD 存储，低频访问 | 搭配 ILM 使用 |

**推荐集群规划：**

```
小规模（开发/测试）——3 节点：
  node1: master + data
  node2: master + data
  node3: master + data

中规模（50 节点以内）——5 节点：
  node1: master（专用）
  node2: master（专用）
  node3: master（专用）
  node4: data（热节点，SSD）
  node5: data（热节点，SSD）

大规模（50+ 节点）——分层架构：
  3 台：专用 master 节点（不存数据）
  10 台：热数据节点（SSD，高频最近数据）
  10 台：冷数据节点（HDD，历史归档数据）
  2 台：专用协调节点（处理复杂聚合查询）
```

> ⚠️ **配置建议**：master 节点至少 3 个（奇数），防止脑裂。数据节点和 master 节点分离，避免 GC 影响集群稳定性。

---

### Q8：ES 搜索评分算法 BM25 的原理是什么？和 TF-IDF 比有什么改进？
**面试官意图：** 考察对搜索引擎底层算法的理解深度。

**完美解答：**

**BM25 是 ES 5.0 之后默认的评分算法，取代了 TF-IDF。**

**两种算法公式对比：**

**TF-IDF 公式：**
```
score = TF × IDF
TF = 词在文档中出现的频率
IDF = log(总文档数 / 包含该词的文档数)
```

**BM25 公式（简化）：**
```
score = IDF × ((k1 + 1) × TF) / (k1 × (1 - b + b × docLen/avgDocLen) + TF)

其中：
k1 = 1.2（控制 TF 饱和曲线）
b = 0.75（控制文档长度归一化程度）
docLen/avgDocLen = 当前文档长度 / 平均文档长度
```

**BM25 的核心改进：**

| 改进点 | TF-IDF 问题 | BM25 方案 |
|--------|------------|-----------|
| **TF 饱和** | TF 线性增长，某个词出现 100 次得分就是 10 次的 10 倍 | TF 增长到一定程度后饱和（通过 k1 参数控制），防止高频词过度影响 |
| **文档长度归一化** | 长文档天然包含更多词，得分偏高 | 引入 `docLen/avgDocLen`，惩罚长文档，公平对待短文档 |
| **参数可调** | 无调节参数 | k1 和 b 参数可调，适应不同业务场景 |

**实际调优示例：**
```json
{
  "index": {
    "similarity": {
      "default": {
        "type": "BM25",
        "k1": 1.2,    // 默认值，正文搜索用 1.2
        "b": 0.75     // 默认值
      },
      "title_similarity": {
        "type": "BM25",
        "k1": 2.0,    // 标题匹配：提高 TF 的影响
        "b": 0.3      // 标题通常短，降低长度惩罚
      }
    }
  }
}
```

> 💡 **面试加分**：能说出 BM25 的三个参数优化场景——正文搜索用默认值 1.2/0.75；标题匹配可以提高 k1 到 2.0 并降低 b 到 0.3；短文本搜索降低 k1 到 0.5。

---

## 4. 场景题与故障排查

> 💡 考察实际解决问题的能力

### Q9：ES 集群状态变成 RED 怎么办？如何排查？
**面试官意图：** 考察对 ES 运维故障的应急排查能力。

**完美解答：**

**ES 集群状态说明：**

| 状态 | 含义 | 严重程度 |
|:----:|------|:--------:|
| **GREEN** | 所有主分片和副本分片都正常分配 | ✅ |
| **YELLOW** | 所有主分片正常，但部分副本未分配 | ⚠️ 不影响读写 |
| **RED** | 有主分片未分配，部分数据不可读写 | 🚨 紧急处理 |

**排查步骤：**

```bash
# 1. 查看集群健康状态
GET _cluster/health
# 返回中查看 unassigned_shards 数量

# 2. 查看未分配分片的详细信息
GET _cat/shards?v&h=index,shard,prirep,state,node,unassigned.reason
# 找到 state=UNASSIGNED 的分片

# 3. 查看未分配原因
GET _cluster/allocation/explain
# 会给出详细的未分配原因和推荐操作
```

**RED 状态的原因和解决：**

| 原因 | 场景 | 解决方案 |
|------|------|---------|
| **节点宕机** | 分片所在节点挂了 | 重新拉起节点，ES 会自动恢复 |
| **磁盘空间满** | 数据节点磁盘达到 95% 水位线 | 清理数据、扩容磁盘、或增加节点 |
| **分片分配延迟** | 大集群重启后分配较慢 | 等待或调整 `delayed_timeout` |
| **副本分片数超** | 副本数超过可用节点数 | 降低副本数 |
| **索引损坏** | 硬件故障导致分片数据损坏 | `POST /_clipg` 尝试修复，或从快照恢复 |

**紧急止血命令：**
```bash
# 紧急分配未分配的分片（知道原因后）
POST /_cluster/reroute
{
  "commands": [
    {
      "allocate_stale_primary": {
        "index": "my-index",
        "shard": 0,
        "node": "node-1",
        "accept_data_loss": true  # 谨慎使用！
      }
    }
  ]
}
```

> ⚠️ **重要原则**：RED 状态的处理顺序是：先排查原因（`_cluster/allocation/explain`）→ 解决问题（磁盘/节点/配置）→ 自动恢复。不要轻易手动 reroute，除非你完全确定数据可以丢弃。

---

### Q10：ES 写入性能差怎么办？如何优化写入吞吐量？
**面试官意图：** 考察 ES 写入性能调优的实际经验。

**完美解答：**

**ES 写入性能瓶颈排查与优化：**

**索引级别优化：**

| 优化项 | 配置 | 说明 |
|--------|------|------|
| **增加 refresh 间隔** | `index.refresh_interval: 30s` | 默认 1 秒强制 refresh（生成新段），频繁 refresh 影响写入 |
| **增加 translog flush 间隔** | `index.translog.durability: async` | 异步刷盘减少 IO，批量写入场景效果好 |
| **减少副本数** | `index.number_of_replicas: 0` | 批量写入时先关副本，写完后恢复 |
| **使用 bulk 批量写入** | 每批 500-1000 条 | 减少网络往返，大幅提升吞吐 |

**批量写入优化示例：**
```java
@Component
public class BulkIndexService {

    @Autowired
    private RestHighLevelClient client;

    public void bulkIndex(List<Product> products) {
        BulkRequest bulkRequest = new BulkRequest();
        bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.NONE); // 不强制 refresh

        for (Product product : products) {
            IndexRequest request = new IndexRequest("products")
                .id(product.getId())
                .source(JSON.toJSONString(product), XContentType.JSON);
            bulkRequest.add(request);
        }

        // 批量写入，设置超时
        client.bulk(bulkRequest, RequestOptions.DEFAULT);
    }
}
```

**业务场景优化方案：**

| 场景 | 优化策略 | 效果 |
|------|---------|------|
| **日志批处理** | 关闭副本 + 批量写入 + 30s refresh | 提升 5-10 倍写入性能 |
| **实时写入** | bulk 批量 + 适当调整 refresh | 提升 2-3 倍 |
| **全量数据导入** | 关副本 + 关 refresh + 关 translog 异步 | 提升 10+ 倍 |

> 💡 **典型调优配置（日志批量导入）**：
> ```json
> PUT /logs/_settings
> {
>   "index": {
>     "refresh_interval": "30s",
>     "number_of_replicas": 0,
>     "translog.durability": "async",
>     "translog.sync_interval": "30s"
>   }
> }
> ```
> 导入完成后恢复：`refresh_interval: 1s`，`number_of_replicas: 1`

---

### Q11：ES 中一个分片的数据量控制在多少比较合适？分片数设计的原则是什么？
**面试官意图：** 考察 ES 分片设计经验，分片数设置不当是 ES 最常见的配置错误之一。

**完美解答：**

**分片设计核心原则：**

| 原则 | 说明 | 原因 |
|------|------|------|
| **单分片数据量 20-50GB** | 控制在 50GB 以内 | 分片过大导致查询慢、恢复慢 |
| **单节点分片数不超过 20** | 每个节点管理的分片越少越好 | 分片多导致 GC 压力大、管理开销大 |
| **分片数 = 节点数 × 1-3** | 不要超过节点的 3 倍 | 分片太多导致小分片过多 |

**分片数的计算公式：**
```
分片数 = (数据总量 / 单分片目标大小) × (1 + 增长余量)

举例：
总数据量 500GB，单分片 50GB
最小分片数 = 500 / 50 = 10
考虑未来增长 2 倍 = 20 分片
```

**最佳实践：**
```json
// 创建索引时指定分片数（创建后不可修改！）
PUT /products
{
  "settings": {
    "index": {
      "number_of_shards": 5,    // 先估算好，不能改
      "number_of_replicas": 1
    }
  }
}
```

**常见错误：**
```bash
❌ 错误1：分片太多（每个节点 50+ 分片）
→ 后果：集群重启慢、查询慢、GC 频繁

❌ 错误2：分片太少（3 节点只有 3 分片）
→ 后果：无法充分利用集群并行查询能力

❌ 错误3：每天新建索引不分片
→ 后果：大索引查询慢、Shard 不均匀
```

> 💡 **推荐方案**：时间序列数据（如日志）按天/月建索引，每个索引 3-5 个分片；业务数据（如商品）按业务线建索引，每个索引 5-10 个分片。分片数一旦确定不能修改，必须重建索引。

---

## 💎 面试加分金句

- "倒排索引是 ES 的核心，它允许 ES 在毫秒级内从数十亿文档中找到匹配结果，这是 MySQL 的 LIKE 查询永远做不到的。"
- "ES 不是银弹，它擅长的是全文搜索和聚合分析，不适合做 OLTP 高频事务。一个合理的架构是 MySQL 负责事务，ES 负责搜索。"
- "分片设计是 ES 架构中最关键的决策之一，分片太多或太少都是问题。我的原则是一步到位、只大不小，因为分片数在创建索引后就不能修改了。"
- "BM25 评分算法相比 TF-IDF 最大的改进是引入了 TF 饱和和长度归一化，让短标题匹配的文档不会输给长文本内容。"
- "数据同步的双重保障机制——Canal 实时同步 + 定时任务兜底对比，确保 ES 和 MySQL 数据的最终一致性。"

## 📋 高频追问清单

| 追问方向 | 应对策略 |
|----------|----------|
| ES 为什么搜索是近实时的？ | 写入到 segment 内存中，refresh 后才可见（默认 1 秒） |
| ES 的删除操作是立即生效的吗？ | 不是，标记删除，segment merge 时物理删除 |
| ES 聚合为什么会 OOM？ | bucket 数量太大，设置 `size` 限制聚合结果数 |
| ES 脑裂怎么解决？ | 设置 `discovery.zen.minimum_master_nodes: (master数/2)+1` |
| 为什么 ES 不支持事务？ | 分布式系统 CAP 理论，ES 优先保证可用性和分区容错性 |
| term 和 match 查询有什么区别？ | term 精确匹配（不分词），match 分词匹配 |
| ES 的 routing 有什么作用？ | 控制文档路由到指定分片，减少查询时需要扫描的分片数 |
| 索引生命周期管理（ILM）是什么？ | 自动管理索引的 hot→warm→cold→delete 生命周期 |

## 🔗 关联知识点

- [MySQL必做项目清单-面试问答](#) — MySQL 与 ES 数据同步方案
- [Redis必做项目清单-面试问答](#) — ES + Redis 多级缓存架构
- [Docker必做项目清单-面试问答](#) — ELK 容器化部署
