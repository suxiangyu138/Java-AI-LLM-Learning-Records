# Elasticsearch 面试宝典（进阶篇）
> 基于课程大纲全面覆盖高级面试考点 — 从搜索引擎核心原理到企业级生产调优，适用于 5 年以上资深 Java 后端/大数据架构师面试准备。

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

### Q1 什么是 Lucene 的 FST（Finite State Transducer）？在 ES 中有什么作用？
> **FST** 是一种有限状态自动机数据结构，用于高效存储 Term Dictionary，支持前缀查询、模糊查询等。

- 内存占用低，比 HashMap 节省 90% 以上空间。
- 支持按前缀遍历、模糊匹配。
- ES 在 **索引的 Term Dictionary** 中使用 FST 实现快速词项查找。

### Q2 ES 中 Fuzzy Query 的底层原理是什么？
```json
{
  "query": {
    "fuzzy": {
      "title": {
        "value": "elasticsearh",
        "fuzziness": "AUTO"
      }
    }
  }
}
```
> **Levenshtein 编辑距离** 算法：支持增加/删除/替换/调换字符。距离为 1 时允许 1 个字符差异。`fuzziness: AUTO` 根据词长度自动决定：0-2 字符必须精确，3-5 字符允许 1 编辑距离，>5 字符允许 2 编辑距离。

### Q3 wildcard 和 prefix 查询的原理和性能问题？
| 查询 | 原理 | 性能 |
|------|------|------|
| **prefix** | 扫描 Term Dictionary 中指定前缀的词项 | 前缀越长越快 |
| **wildcard** | 全量扫描 Term Dictionary，逐项匹配通配符 | **非常慢**，不推荐用于高频搜索 |

> ⚠️ **wildcard 以 `*`开头会导致全量扫描 Term Dictionary**，生产环境务必避免。
> ✅ 替代方案：使用 **ngram 分词器** 实现高效通配符效果。

### Q4 query_string 和 simple_query_string 的区别？
| 特性 | query_string | simple_query_string |
|------|-------------|---------------------|
| 语法解析 | 严格，语法错误直接报错 | 宽松，忽略错误语法 |
| 默认操作符 | OR | OR 可配置 |
| 字段指定 | title:java `+` 必须 `-` 排除 | 不支持复杂语法 |
| 异常处理 | 语法错误抛出异常 | 忽略错误，容错处理 |

> 💡 面向用户的搜索框推荐使用 `simple_query_string`，更加健壮。

### Q5 什么是 Dynamic Template？请举例说明。
> Dynamic Template 允许根据字段名或类型匹配，自动应用预设的映射规则。

```json
PUT /my_index
{
  "mappings": {
    "dynamic_templates": [
      {
        "strings_as_keyword": {
          "match_mapping_type": "string",
          "mapping": { "type": "keyword" }
        }
      },
      {
        "longs_as_integer": {
          "match_mapping_type": "long",
          "mapping": { "type": "integer" }
        }
      },
      {
        "date_detection": {
          "match": "*_date",
          "mapping": { "type": "date", "format": "yyyy-MM-dd" }
        }
      }
    ]
  }
}
```

### Q6 什么是 Index Template（索引模板）？
> Index Template 是一种预配置模板，在新索引创建时自动应用，常用于日志场景。

```json
PUT /_index_template/logs_template
{
  "index_patterns": ["logs-*"],
  "template": {
    "settings": {
      "number_of_shards": 3,
      "number_of_replicas": 1,
      "refresh_interval": "30s"
    },
    "mappings": {
      "properties": {
        "@timestamp": { "type": "date" },
        "message": { "type": "text" },
        "level": { "type": "keyword" },
        "host": { "type": "keyword" }
      }
    }
  },
  "priority": 100
}
```

### Q7 Reindex 有哪些使用场景和注意事项？
**使用场景**：
1. 修改已有字段的 Mapping 类型（ES 不允许直接修改）
2. 升级索引配置（分片数、分词器等）
3. 将数据从一个集群迁移到另一个集群

**注意事项**：
- 大规模 reindex 会产生大量查询和写入压力，建议限速：
```json
POST /_reindex
{
  "source": { "index": "old_index" },
  "dest": { "index": "new_index" },
  "requests_per_second": 500
}
```
### Q8 deep pagination（深度分页）三种方案的详细对比？
| 方案 | 支持随机跳页 | 内存消耗 | 实时性 | 适用场景 |
|------|------------|----------|--------|----------|
| **from+size** | ✅ | O(from+size) 累加，深分页极差 | ✅ 实时 | 浅分页（<10000条） |
| **scroll** | ❌ 只能向前 | 高（维护快照上下文） | ❌ 快照 | 全量导出、reindex |
| **search_after** | ❌ 只能向前 | 低（仅存储游标值） | ✅ 实时 | 深分页滚动 |
| **PIT + search_after** | ❌ 只能向前 | 中 | ✅ 实时 | ES 7.x+ 推荐方案 |

### Q9 BM25 相比 TF-IDF 做了哪些改进？
| 方面 | TF-IDF | BM25 |
|------|--------|------|
| 词频饱和度 | 线性增长，高频词得分过高 | 非线性，通过 k1 参数饱和 |
| 文档长度 | 无归一化 | 通过 b 参数归一化 |
| 公式复杂度 | 简单 | 相对复杂但效果好 |
| 适用场景 | 短文本 | 长文本、不平衡文档集合 |

```text
TF-IDF: score = tf * idf
BM25:   score = IDF * (tf * (k1+1)) / (tf + k1*(1-b+b*dl/avgdl))
```
> 💡 ES 5.0 之后默认从 TF-IDF 切换到 BM25。

### Q10 什么是 Explain API？如何调试搜索得分？
```json
GET /products/_search
{
  "explain": true,
  "query": { "match": { "title": "java" } }
}
```
返回结果包含每一条匹配的详细得分计算过程：
```json
"_explanation": {
  "value": 1.5,
  "description": "weight(title:java in 1) [PerFieldSimilarity]",
  "details": [
    { "value": 2.0, "description": "score(freq=1.0)" },
    { "value": 0.75, "description": "idf(docFreq=100, docCount=1000)" }
  ]
}
```

### Q11 ES 的节点发现机制？什么是 Seeded Gossip？
> ES 7.x 使用 **Seeded Gossip** 代替了原来的 Zen Discovery。

| 版本 | 发现机制 | 特点 |
|------|----------|------|
| ES 6.x 及之前 | Zen Discovery（基于单播 + 广播） | 配置 `discovery.zen.ping.unicast.hosts` |
| ES 7.x+ | Seeded Gossip（基于种子节点） | 配置 `discovery.seed_hosts`，更简单 |

> 💡 **Gossip 协议**：每个节点定期与其他节点交换元数据信息，实现集群拓扑收敛。不像 ZooKeeper 那样强一致，但更适合 ES 的最终一致性模型。

### Q12 什么是"脑裂"（Split Brain）问题？如何解决？
> 脑裂是指集群中同时出现两个 Master 节点，导致数据不一致的问题。

**产生原因**：网络分区导致部分节点认为 Master 已宕机，重新选举出新的 Master。

**解决方案**：
| 版本 | 配置 | 说明 |
|------|------|------|
| ES 7.x 之前 | `discovery.zen.minimum_master_nodes: N/2 + 1` | 需手动配置 |
| ES 7.x+ | 内置故障检测 | 自动处理，无需单独配置 |

### Q13 什么是 Shard Allocation？有哪些策略？
控制分片在集群节点上的分配策略。

| 配置 | 说明 |
|------|------|
| `cluster.routing.allocation.enable` | 启用/禁用分片分配（all / primaries / new_primaries / none） |
| `index.routing.allocation.include._name` | 将索引分配指定到特定节点名 |
| `index.routing.allocation.exclude._ip` | 排除某些 IP 的节点 |
| `index.routing.allocation.require._role` | 要求分配到特定角色的节点 |

### Q14 什么是 Rerouting？如何手动迁移分片？
```json
POST /_cluster/reroute
{
  "commands": [
    {
      "move": {
        "index": "my_index",
        "shard": 0,
        "from_node": "node-1",
        "to_node": "node-2"
      }
    },
    {
      "allocate_replica": {
        "index": "my_index",
        "shard": 1,
        "node": "node-3"
      }
    }
  ]
}
```

### Q15 磁盘水位控制（Disk Watermark）有哪些配置？
| 配置 | 默认值 | 行为 |
|------|--------|------|
| `cluster.routing.allocation.disk.watermark.low` | 85% | 低于此水位允许分配分片 |
| `cluster.routing.allocation.disk.watermark.high` | 90% | 超过此水位会将分片迁移到其他节点 |
| `cluster.routing.allocation.disk.watermark.flood_stage` | 95% | 所有索引强制设为只读 |

> ⚠️ 生产环境中需要根据磁盘容量合理设置水位线，避免集群进入只读状态。

### Q16 ES 的快照（Snapshot）和恢复（Restore）机制？
```json
// 注册快照仓库
PUT /_snapshot/my_backup
{
  "type": "s3",
  "settings": {
    "bucket": "my-es-backup",
    "region": "us-east-1"
  }
}

// 创建快照
PUT /_snapshot/my_backup/snapshot_20260722
{
  "indices": "logs-*",
  "ignore_unavailable": true
}

// 恢复快照
POST /_snapshot/my_backup/snapshot_20260722/_restore
{
  "indices": "logs-2026-07",
  "rename_replacement": "restored-"
}
```

### Q17 什么是 CCR（Cross-Cluster Replication）？
> CCR 是 ES 企业版功能，用于跨集群的索引数据实时复制。

| 角色 | 说明 |
|------|------|
| **Leader** | 源集群中的数据索引 |
| **Follower** | 目标集群中复制出来的只读索引 |
| **基于** | 底层使用 Lucene 的 segment 复制 + translog 实时同步 |

> 💡 适用场景：**异地多活**、**灾备**、**跨机房数据同步**。

### Q18 如何为 ES 配置安全认证（Basic Auth + TLS）？
> ES 提供免费的基础安全功能，推荐开启。

```yaml
# elasticsearch.yml
xpack.security.enabled: true
xpack.security.transport.ssl.enabled: true
xpack.security.transport.ssl.verification_mode: certificate
xpack.security.transport.ssl.keystore.path: elastic-certificates.p12
xpack.security.transport.ssl.truststore.path: elastic-certificates.p12
xpack.security.http.ssl.enabled: true
xpack.security.http.ssl.keystore.path: elastic-certificates.p12
```

### Q19 什么是 Ingest Pipeline？与 Logstash 有什么不同？
> Ingest Pipeline 是 ES 内置的预处理管道，可在写入数据时对文档进行转换、解析、丰富。

```json
PUT /_ingest/pipeline/logs_processor
{
  "description": "处理日志数据",
  "processors": [
    { "grok": { "field": "message", "patterns": ["%{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:level} %{GREEDYDATA:content}"] } },
    { "date": { "field": "timestamp", "formats": ["yyyy-MM-dd HH:mm:ss"] } },
    { "remove": { "field": "message" } },
    { "geoip": { "field": "client_ip", "target_field": "geo" } }
  ]
}
```

| 组件 | 位置 | 性能 | 灵活性 |
|------|------|------|--------|
| **Ingest Pipeline** | ES 内部 | 高（直接写入前处理） | 中等（处理器有限） |
| **Logstash** | ES 外部 | 相对较低（需独立部署） | 极高（丰富插件体系） |

### Q20 Nested 类型和 Object 类型有何本质区别？
> Object 类型的数组会被**扁平化**，丢失每个子对象内部的字段关联。

```json
// Object 类型（错误的数据关联）
PUT /users/_doc/1
{
  "name": "张三",
  "grades": [{ "subject": "数学", "score": 90 }, { "subject": "语文", "score": 80 }]
}
// object 会被扁平化为：
// grades.subject: [数学, 语文]
// grades.score: [90, 80]
// 此时搜索 subject=语文 + score=90 也能命中，产生了错误的关联！

// Nested 类型（正确的数据关联）
{
  "mappings": {
    "properties": {
      "grades": { "type": "nested" }
    }
  }
}
// nested 使用独立的 Lucene 块存储每个子对象，保持关联关系
```

| 特性 | Object | Nested |
|------|--------|--------|
| 存储方式 | 扁平化到父文档 | 独立 Lucene 块 |
| 查询方式 | 普通 query | `nested` query |
| 查询性能 | 快 | 较慢（需跨块查询） |
| 关联正确性 | ❌ 数组关联错乱 | ✅ 正确 |

---

## 二、深度原理剖析（12题）

### Q21 ES 的 Bulk 写入线程池模型是怎样的？
- ES 使用 **fixed thread pool** 处理写入请求。
- 默认 `thread_pool.write.size` = CPU 核心数，`queue_size` = 10000。
- 当队列满时，返回 `429 Too Many Requests`（RemoteTransportException）。
> 💡 **优化**：如果频繁 429，说明写入压力过大，应降低 Bulk 并发或增大 `queue_size`。

### Q22 Translog 的持久化策略如何影响性能和数据安全？
| 参数 | 默认值 | 说明 |
|------|--------|------|
| `index.translog.durability` | `request` | `request`=每次写入都 fsync；`async`=后台批量 fsync |
| `index.translog.sync_interval` | `5s` | async 模式下 fsync 间隔 |
| `index.translog.flush_threshold_size` | `512mb` | translog 大小触发 flush |

> ⚠️ 如果对数据安全性要求极高（如金融场景），保持 `request` 模式，但写入吞吐会下降。
> ✅ 批量写入场景，可切换到 `async`，写入性能提升 3-5 倍。

### Q23 什么是 Doc Values？列式存储的优势在哪里？
> Doc Values 是 ES 在索引时构建的**列式存储**结构，用于排序、聚合和脚本操作。

**列式存储优势**：
```
行式存储（_source）：  doc1:{title,price,tags}, doc2:{title,price,tags}
列式存储（doc_values）： title:[doc1,doc2], price:[doc1,doc2], tags:[doc1,doc2]
```
1. **高效聚合**：只需要读取参与计算的字段，不需要加载整行文档
2. **高压缩比**：同类型数据一起存储，可高效压缩（LZ4、ZSTD）
3. **磁盘友好**：基于磁盘，不占用 JVM 堆内存

### Q24 ES 的段合并（Segment Merge）策略详解
合并策略由 **Merge Policy** 和 **Merge Scheduler** 控制。

| 策略参数 | 默认值 | 说明 |
|----------|--------|------|
| `segmentsPerTier` | 10 | 每层允许的最大 segment 数 |
| `maxMergedSegment` | 5GB | 单个 merge 的最大 segment 大小 |
| `index.merge.scheduler.max_merge_count` | 6 | 并发 merge 线程数 |
| `index.merge.policy.max_merged_doc` | 极大 | 参与 merge 的最大文档数 |

> 💡 `forcemerge` 触发全量合并到 `max_num_segments` 个 segment，适用于只读索引。

### Q25 ES 的协调节点（Coordinating Node）做了什么？
```
Client Request → Coordinating Node
                 1. 解析请求
                 2. 路由计算：确定需要访问哪些分片
                 3. 向各分片发送子请求
                 4. 归并结果：排序、聚合、裁剪
                 5. 返回最终结果给客户端
```
> 💡 大查询场景（如全表扫描聚合），协调节点会成为**内存瓶颈**。建议将协调节点与数据节点分离部署。

### Q26 ES 如何实现"最终一致性"？
1. **主分片写入 → 同步副本**：写入主分片后同步到副本，可设 `wait_for_active_shards`。
2. **refresh 延迟**：默认 1s 后数据才可见（不是瞬间）。
3. **segment 不可变**：更新标记删除，查询时会过滤。
4. **translog 恢复**：宕机后通过 translog 重放未 flush 的操作。
5. **分片恢复**：节点宕机后，ES 自动在其他节点上重建副本。

### Q27 ES 的 Memory Circuit Breaker 机制是什么？
> 防止查询/聚合请求消耗过多堆内存导致 Full GC。

| 断路器 | 触发阈值（默认） | 保护对象 |
|--------|-----------------|----------|
| `indices.breaker.fielddata.limit` | 40% JVM | FieldData 缓存 |
| `indices.breaker.request.limit` | 60% JVM | 请求级别的数据结构 |
| `network.breaker.inflight_requests.limit` | 100% JVM | 网络请求 |
| `script.max_compilations_rate` | 75/5m | 脚本编译速率 |
| `search.max_buckets` | 65536 | 聚合桶数量 |

> ⚠️ 业务中若频繁触发断路器，应优化查询或增大堆内存。

### Q28 什么是 Global Ordinals？对聚合性能有什么影响？
> Global Ordinals 是一种对 keyword 字段的**全局编号系统**，用于加速聚合和排序。

- 每个 segment 有自己的本地 ordinal（Term 到 int 的映射）。
- **Global Ordinals** 将整个索引的 ordinal 统一，使得跨 segment 聚合不需要合并字符串。
- 构建 Global Ordinals 需要扫描所有 segment，是一个**耗时操作**（默认延迟加载）。
> 💡 **优化**：对频繁聚合的字段，可在 Mapping 中设置 `eager_global_ordinals: true`，在 refresh 时提前构建。

### Q29 ES 如何管理 JVM 堆内存？
| 配置 | 建议值 | 说明 |
|------|--------|------|
| `-Xms` = `-Xmx` | 不超过物理内存 50%，最多 32GB | 堆内存建议固定值，避免动态调整 |
| `indices.memory.index_buffer_size` | 10% JVM | 索引缓冲区 |
| `indices.fielddata.cache.size` | 20% JVM（最多） | FieldData 缓存 |
| `indices.breaker.fielddata.limit` | 40% JVM | FieldData 断路器 |

> ⚠️ **JVM 堆大小不建议超过 32GB**，因为 JDK 的普通对象指针压缩技术在超过 32GB 时失效，导致内存浪费。

### Q30 ES 7.x 的 PIT（Point In Time）机制是什么？
> PIT 是 ES 7.x 引入的替代 scroll 的机制，结合 search_after 实现实时深分页。

```json
// 1. 创建 PIT
POST /products/_pit?keep_alive=5m

// 2. 使用 PIT + search_after 分页
POST /_search
{
  "pit": { "id": "myPitId", "keep_alive": "5m" },
  "size": 100,
  "sort": [{ "id": "asc" }],
  "track_total_hits": false,
  "search_after": [100]
}

// 3. 释放 PIT
DELETE /_pit { "id": "myPitId" }
```

### Q31 ES 中 geo_point 和 geo_shape 的区别？
| 类型 | 说明 | 适用场景 |
|------|------|----------|
| **geo_point** | 点坐标（经度，纬度） | 附近门店查询、距离计算 |
| **geo_shape** | 面、线、多边形 | 区域围栏、行政区划查询 |

### Q32 ES 的跨集群搜索（Cross-Cluster Search）原理？
```yaml
# elasticsearch.yml
cluster:
  remote:
    dc_cluster_a:
      seeds: ["node-a1:9300", "node-a2:9300"]
```
```json
// 跨集群查询
GET dc_cluster_a:logs/_search
{
  "query": { "match_all": {} }
}
```
**原理**：本地集群的协调节点直接将请求转发到远程集群，归并结果。数据不复制，查询实时。

---

## 三、实战场景题（10题）

### Q33 现有一个日志系统写入压力极大，如何从 ES 层面优化写入性能？
| 优化项 | 配置/措施 | 预期提升 |
|--------|-----------|----------|
| 批量写入 | 使用 Bulk API，每批 5000-10000 条 | 3-5x |
| 关闭 refresh | `refresh_interval: -1` 或 `"30s"` | 2-3x |
| 关闭副本 | `number_of_replicas: 0`（写入完成后开启） | 1.5-2x |
| 异步 translog | `translog.durability: async` + `sync_interval: 30s` | 2-3x |
| 增加线程池 | `thread_pool.write.queue_size: 20000` | 防 429 错误 |
| 使用 SSD | 磁盘 I/O 是第一瓶颈 | 5-10x |

### Q34 如何配置 ILM（Index Lifecycle Management）实现日志自动管理？
```json
PUT /_ilm/policy/logs_policy
{
  "policy": {
    "phases": {
      "hot": {
        "min_age": "0ms",
        "actions": {
          "rollover": { "max_size": "50GB", "max_age": "1d" },
          "set_priority": { "priority": 100 }
        }
      },
      "warm": {
        "min_age": "7d",
        "actions": {
          "allocate": { "require": { "data_type": "warm" } },
          "forcemerge": { "max_num_segments": 1 },
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
        "actions": { "delete": {} }
      }
    }
  }
}
```

### Q35 一个索引的搜索响应时间越来越慢，如何排查和优化？
| 步骤 | 排查手段 | 可能的根因 |
|------|----------|------------|
| 1 | `GET /_cat/nodes?v` 查看负载 | 节点负载不均 |
| 2 | `GET /_cat/segments/index_name?v` | segment 数量过多 |
| 3 | `GET /_nodes/hot_threads` | 慢查询/GC |
| 4 | `GET /_cluster/allocation/explain` | 分片分配问题 |
| 5 | `GET /_cat/thread_pool` | 队列堆积 |
| 6 | Explain API 单条查询分析 | 查询语句效率低 |

### Q36 如何实现"搜索结果中根据商品销量加权"？
```json
{
  "query": {
    "function_score": {
      "query": { "match": { "title": "手机" } },
      "functions": [
        {
          "field_value_factor": {
            "field": "sales_count",
            "factor": 0.1,
            "modifier": "log1p",
            "missing": 1
          }
        },
        {
          "gauss": {
            "create_time": {
              "origin": "now",
              "scale": "30d",
              "decay": 0.5
            }
          }
        }
      ],
      "score_mode": "multiply",
      "boost_mode": "multiply"
    }
  }
}
```

### Q37 使用 Java High Level REST Client 实现 bulk 写入 + bool 查询
```java
// Maven 依赖
<dependency>
    <groupId>org.elasticsearch.client</groupId>
    <artifactId>elasticsearch-rest-high-level-client</artifactId>
    <version>7.17.0</version>
</dependency>

// Bulk 写入
BulkRequest bulkRequest = new BulkRequest();
for (Product p : products) {
    bulkRequest.add(new IndexRequest("products")
        .id(p.getId())
        .source(JsonUtils.toJson(p), XContentType.JSON));
}
BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
if (bulkResponse.hasFailures()) {
    log.error("Bulk 写入失败: {}", bulkResponse.buildFailureMessage());
}

// Bool 查询
SearchRequest searchRequest = new SearchRequest("products");
BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
    .must(QueryBuilders.matchQuery("title", "java"))
    .filter(QueryBuilders.termQuery("status", "published"))
    .filter(QueryBuilders.rangeQuery("price").gte(50).lte(100))
    .should(QueryBuilders.matchQuery("description", "spring"));
searchRequest.source().query(boolQuery);
searchRequest.source().from(0).size(20);
searchRequest.source().sort(new FieldSortBuilder("price").order(SortOrder.DESC));
SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
SearchHits hits = response.getHits();
for (SearchHit hit : hits.getHits()) {
    String json = hit.getSourceAsString();
    Product p = JsonUtils.parse(json, Product.class);
}
```

### Q38 Spring Data Elasticsearch 的常用操作示例
```java
// 实体类
@Document(indexName = "products")
public class Product {
    @Id
    private String id;
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String title;
    @Field(type = FieldType.Keyword)
    private String category;
    @Field(type = FieldType.Double)
    private Double price;
    @Field(type = FieldType.Date)
    private Date createTime;
}

// Repository 接口
public interface ProductRepository
    extends ElasticsearchRepository<Product, String> {
    List<Product> findByTitle(String title);
    Page<Product> findByCategoryAndPriceBetween(
        String category, Double min, Double max, Pageable pageable);
}

// 自定义查询（NativeSearchQuery）
NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
    .withQuery(QueryBuilders.matchQuery("title", "java"))
    .withFilter(QueryBuilders.rangeQuery("price").gte(50))
    .withPageable(PageRequest.of(0, 20))
    .withSort(SortBuilders.fieldSort("price").order(SortOrder.DESC))
    .build();
SearchHits<Product> products =
    elasticsearchOperations.search(searchQuery, Product.class, IndexCoordinates.of("products"));
```

### Q39 如何在 ES 中实现"搜索附近的门店并按距离排序"？
```json
// Mapping
{
  "mappings": {
    "properties": {
      "name": { "type": "text" },
      "location": { "type": "geo_point" },
      "address": { "type": "keyword" }
    }
  }
}

// 搜索 5km 内并排序
{
  "query": {
    "bool": {
      "must": { "match_all": {} },
      "filter": {
        "geo_distance": {
          "distance": "5km",
          "location": { "lat": 39.9042, "lon": 116.4074 }
        }
      }
    }
  },
  "sort": [
    {
      "_geo_distance": {
        "location": { "lat": 39.9042, "lon": 116.4074 },
        "order": "asc",
        "unit": "km"
      }
    }
  ]
}
```

### Q40 如何通过配置 Logstash 实现 MySQL 到 ES 的同步？
```ruby
input {
  jdbc {
    jdbc_driver_library => "/path/mysql-connector-java.jar"
    jdbc_driver_class => "com.mysql.cj.jdbc.Driver"
    jdbc_connection_string => "jdbc:mysql://localhost:3306/mydb"
    jdbc_user => "root"
    jdbc_password => "password"
    statement => "SELECT * FROM products WHERE update_time > :sql_last_value"
    schedule => "*/5 * * * *"
    use_column_value => true
    tracking_column => "update_time"
    tracking_column_type => "timestamp"
  }
}

output {
  elasticsearch {
    hosts => ["localhost:9200"]
    index => "products"
    document_id => "%{id}"
    action => "index"
  }
}
```

### Q41 Nested 查询如何书写？性能如何优化？
```json
// Nested 查询语法
{
  "query": {
    "nested": {
      "path": "grades",
      "query": {
        "bool": {
          "must": [
            { "match": { "grades.subject": "数学" } },
            { "range": { "grades.score": { "gte": 85 } } }
          ]
        }
      }
    }
  }
}
```
**性能优化建议**：
1. Nested 查询比普通查询慢，因为需要在独立的 Lucene 块中搜索。
2. 尽量减少 Nested 字段的嵌套深度。
3. 如果不需要关联查询，考虑将子对象展开为扁平结构。
4. 使用 `include_in_root: true` 将 nested 字段同时存储到父文档（提升某些场景性能）。

### Q42 聚合中出现不准的统计数值如何解决？
> 默认 `terms` 聚合返回文档数**最多的前 10 个桶**，且 count 只是近似值（基于分片采样）。

**解决方案**：
```json
// 1. 增大 size
"terms": { "field": "category.keyword", "size": 1000 }

// 2. 设置 shard_size 精确控制分片采样
"terms": {
  "field": "category.keyword",
  "size": 10,
  "shard_size": 100    // 每个分片返回更多候选值
}

// 3. 如果需要完全精确
"terms": {
  "field": "category.keyword",
  "size": 1000,
  "shard_size": 2000,
  "execution_hint": "map"  // 强制全量精确统计（慎用，内存消耗大）
}
```

---

## 四、手写代码/配置文件题（6题）

### Q43 手写：完整的生产环境 ES 集群配置
```yaml
# elasticsearch.yml — 生产推荐配置
cluster.name: production-es
node.name: node-data-1
path.data: /data/elasticsearch/data
path.logs: /var/log/elasticsearch

network.host: 0.0.0.0
http.port: 9200
transport.port: 9300

# 节点角色分离
node.roles: [ data, ingest ]

# 集群发现
discovery.seed_hosts:
  - master-1:9300
  - master-2:9300
  - master-3:9300
cluster.initial_master_nodes:
  - master-1
  - master-2
  - master-3

# 磁盘水位
cluster.routing.allocation.disk.watermark.low: 85%
cluster.routing.allocation.disk.watermark.high: 90%
cluster.routing.allocation.disk.watermark.flood_stage: 95%

# 慢查询日志
index.search.slowlog.threshold.query.warn: 10s
index.search.slowlog.threshold.query.info: 5s
index.search.slowlog.threshold.query.debug: 2s
index.indexing.slowlog.threshold.index.warn: 10s
```

### Q44 手写：Index Template + Dynamic Template 组合配置
```json
PUT /_index_template/ecommerce_template
{
  "index_patterns": ["products-*", "orders-*"],
  "priority": 200,
  "template": {
    "settings": {
      "number_of_shards": 3,
      "number_of_replicas": 1,
      "refresh_interval": "10s"
    },
    "mappings": {
      "dynamic_templates": [
        {
          "text_to_keyword": {
            "match_mapping_type": "string",
            "mapping": { "type": "keyword" }
          }
        },
        {
          "long_to_integer": {
            "match_mapping_type": "long",
            "mapping": { "type": "integer" }
          }
        },
        {
          "date_detection": {
            "match": "*_at",
            "mapping": { "type": "date" }
          }
        },
        {
          "text_content": {
            "match": "description",
            "mapping": { "type": "text", "analyzer": "ik_max_word" }
          }
        }
      ],
      "properties": {
        "title": { "type": "text", "analyzer": "ik_max_word", "fields": { "keyword": { "type": "keyword" } } },
        "price": { "type": "double" },
        "status": { "type": "keyword" }
      }
    }
  }
}
```

### Q45 手写：CCR 跨集群复制配置
```json
// 在 Leader 集群上
// 无需特殊配置，确保索引已创建
PUT /leader_index
{
  "settings": { "number_of_shards": 3, "number_of_replicas": 1 }
}

// 在 Follower 集群上
// 1. 配置远程集群连接
PUT /_cluster/settings
{
  "persistent": {
    "cluster.remote.leader_cluster.seeds": ["leader-node1:9300", "leader-node2:9300"]
  }
}

// 2. 创建 Follower 索引
PUT /follower_index/_ccr/follow
{
  "remote_cluster": "leader_cluster",
  "leader_index": "leader_index",
  "max_read_request_operation_count": 5000,
  "max_outstanding_read_requests": 12,
  "max_write_request_operation_count": 5000,
  "max_outstanding_write_requests": 9
}

// 3. 查看复制状态
GET /follower_index/_ccr/info

// 4. 暂停/恢复复制
POST /follower_index/_ccr/pause_follow
POST /follower_index/_ccr/resume_follow
```

### Q46 手写：ES 快照管理（S3 仓库 + 自动化策略）
```json
// 1. 注册 S3 快照仓库
PUT /_snapshot/my_backup
{
  "type": "s3",
  "settings": {
    "bucket": "my-es-snapshots",
    "region": "cn-north-1",
    "base_path": "elasticsearch/backup",
    "compress": true,
    "max_snapshot_bytes_per_sec": "200mb",
    "max_restore_bytes_per_sec": "500mb"
  }
}

// 2. 创建自动化 SLM 策略（Snapshot Lifecycle Management）
PUT /_slm/policy/nightly-snapshot
{
  "name": "<nightly-snapshot-{now/d}>",
  "schedule": "0 30 2 * * ?",
  "repository": "my_backup",
  "config": {
    "indices": ["logs-*", "products"],
    "ignore_unavailable": true,
    "include_global_state": false
  },
  "retention": {
    "expire_after": "30d",
    "min_count": 5,
    "max_count": 50
  }
}

// 3. 立即执行一次快照
POST /_slm/policy/nightly-snapshot/_execute

// 4. 查看快照状态
GET /_snapshot/my_backup/_all
```

### Q47 手写：Filebeat 配置采集日志到 ES
```yaml
# filebeat.yml
filebeat.inputs:
  - type: log
    enabled: true
    paths:
      - /var/log/app/*.log
    fields:
      app_name: my-service
      environment: production
    multiline:
      pattern: '^\d{4}-\d{2}-\d{2}'
      negate: true
      match: after

output.elasticsearch:
  hosts: ["localhost:9200"]
  index: "app-logs-%{+yyyy.MM.dd}"
  pipeline: "logs_processor"

setup.ilm.enabled: true
setup.ilm.policy_name: "logs_policy"
setup.kibana:
  host: "localhost:5601"

logging.level: info
logging.to_files: true
logging.files:
  path: /var/log/filebeat
  name: filebeat.log
  keepfiles: 7
```

### Q48 手写：Java HLR Client 聚合查询示例
```java
// 按品牌聚合 + 计算平均价格 + 查询品牌 A 的统计趋势
SearchRequest searchRequest = new SearchRequest("products");

// 聚合构建
AggregationBuilder brandAgg = AggregationBuilders.terms("by_brand")
    .field("brand.keyword")
    .size(10)
    .subAggregation(AggregationBuilders.avg("avg_price").field("price"))
    .subAggregation(AggregationBuilders.sum("total_sales").field("sales"));

// 添加日期直方图
DateHistogramAggregationBuilder dateAgg =
    AggregationBuilders.dateHistogram("sales_over_time")
        .field("order_date")
        .calendarInterval(DateHistogramInterval.MONTH)
        .subAggregation(AggregationBuilders.sum("monthly_total").field("amount"));

searchRequest.source().aggregation(brandAgg);
searchRequest.source().aggregation(dateAgg);
searchRequest.source().size(0);
searchRequest.source().query(QueryBuilders.termQuery("status", "active"));

SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
Terms terms = response.getAggregations().get("by_brand");
for (Terms.Bucket bucket : terms.getBuckets()) {
    String brand = bucket.getKeyAsString();
    long count = bucket.getDocCount();
    Avg avg = bucket.getAggregations().get("avg_price");
    log.info("品牌={}, 商品数={}, 均价={}", brand, count, avg.getValue());
}
```

---

## 五、系统设计题（4题）

### Q49 设计一个日均 10TB 日志的 ES 集群架构
> **需求**：每天 10TB 日志写入，保留 30 天热数据，30 天冷数据，90 天归档删除。

**架构设计**：
```
采集层：Filebeat (每台机器) → Kafka（20 分区）
                                 ↓
消费层：Logstash / Kafka Connect → 清洗、解析、去重
                                 ↓
存储层：ES 集群（30 个 Data Node）
         ├─ Hot Tier（SSD, 7天）：写入为主，refresh_interval=30s
         ├─ Warm Tier（HDD, 7-30天）：forcemerge=1段，只读
         └─ Cold Tier（HDD, 30-90天）：缩减副本，低频访问
                                 ↓
展示层：Kibana（多租户 Dashboard）
```

**关键参数计算**：
```
每天数据量：10TB
每条日志平均 1KB → 每天约 100亿 条
每秒约 11.5万 条写入
每个 Data Node（10台 Hot）：约 1.15万 TPS
需要 Bulk 每批 5000 条 → 每秒 2-3 个 bulk/节点
单分片推荐 50GB → 每个索引需要约 200 分片/day
```

### Q50 设计一个"商品智能搜索推荐系统"
> **需求**：支持错别字纠正、同义词扩展、个性化排序、类目预测。

**核心技术方案**：
```
用户输入 → IK 分词 → fuzzy 纠错 → synonym 扩展 → 多路召回
                                                       ↓
                                              function_score 加权
                                                ├─ 销量 * 0.3
                                                ├─ 好评率 * 0.2
                                                ├─ 用户偏好 * 0.3
                                                └─ 时间衰减 * 0.2
                                                       ↓
                                              排序 → search_after 分页
```

**同义词配置**：
```json
PUT /_analyze
{
  "tokenizer": "standard",
  "filter": ["synonym"],
  "text": "手机"
}
// synonym.txt: 手机,智能手机,移动电话 => 统一映射
```

### Q51 设计一个"SaaS 多租户日志平台"
> **需求**：每个租户独立看自己的日志，支持全文搜索和聚合，租户间完全隔离。

**设计对比**：
| 方案 | 资源隔离 | 管理复杂度 | 查询性能 | 适用租户数 |
|------|----------|------------|----------|-----------|
| **索引隔离** | ✅ 完全隔离 | 低（ILM 自动管理） | 高 | <1000 |
| **别名+路由** | ⚠️ 部分隔离 | 中（需管理 routing） | 中 | 1000-10000 |
| **字段隔离** | ❌ 共享索引 | 高（查询始终过滤 tenant_id） | 低 | >10000 |

> 💡 推荐方案：**中型 SaaS（<1000 租户）使用索引隔离**，每个租户日志一个索引前缀，通过 ILM 自动管理。

### Q52 设计一个"ES 集群容灾方案"
> **需求**：RPO < 5 分钟，RTO < 30 分钟，跨机房/跨地域。

**方案选择**：
```
方案一：同城双活（推荐）
  机房 A（主）← 同步复制 → 机房 B（备）
  - CCR 跨集群复制
  - RPO ≈ 0（近同步）
  - RTO < 5min（手动切换）

方案二：异地灾备
  主集群 ← 异步 → 灾备集群
  - 快照定期备份（每小时）
  - CCR 跨地域异步
  - RPO ≈ 5min
  - RTO ≈ 30min

方案三：三副本+跨 AZ
  同一集群跨可用区部署
  - ES 自动分片分配
  - 副本数 = 3，分散到两个 AZ
  - AZ 故障自动恢复
```

---

## 六、常见坑点与最佳实践

### 高级踩坑及解决方案
| 坑点 | 现象 | 根因 | 解决方案 |
|------|------|------|----------|
| **堆内存超过 32GB** | GC 时间过长 | 普通对象指针压缩失效 | 堆 ≤ 32GB，超 32GB 用多节点 |
| **慢查询日志未开启** | 问题难以定位 | 默认不记录 | 开启 `index.search.slowlog` |
| **分片数过多** | 集群不稳定、OOM | 每分片有内存开销 | 单分片 20-50GB，控制分片总数 |
| **聚合不准** | terms 聚合 missing | 默认 shard_size=size | 显式设置 `shard_size` |
| **Nested 查询慢** | 响应时间暴涨 | 独立 Lucene 块开销 | 减少嵌套深度/改用 flattened |
| **Index 字段自动推断** | mapping 爆炸 | 动态映射全开 | `dynamic: strict` 或 Dynamic Template |
| **频繁 force merge** | IO 毛刺、节点失联 | merge 占用 IO 带宽 | `indices.store.throttle.max_bytes_per_sec` |

### 生产优化清单
> ✅ **节点角色分离**：Master（3 台）/ Data（多台）/ Coordinating（2 台）
> ✅ **JVM 配置**：`-Xms` = `-Xmx` ≤ 32GB，使用 G1GC 或 ZGC
> ✅ **操作系统**：`vm.max_map_count=262144`，禁用 swap
> ✅ **索引设计**：显式 mapping + dynamic template + 按时间滚动
> ✅ **写入策略**：Bulk API + 关闭 `refresh` 和副本 + async translog
> ✅ **查询优化**：filter 前置 + 限制 `size` + 避免 `*` wildcard
> ✅ **监控告警**：集群健康、节点 CPU/内存、查询/写入延迟、慢日志

---

## 七、面试回答模板（Top 5）

### 模板 6：ES 在项目中的架构定位和选型理由
"我们选择 ES 作为搜索引擎和日志分析平台。在电商搜索场景中，ES 天然的分布式架构、倒排索引实现毫秒级全文搜索，配合 ik 分词器解决了中文搜索痛点。在日志场景中，ES 的滚动索引 + ILM 实现了 TB 级日志的自动管理，ELK 生态让我们无需从零搭建平台。相比于 MySQL like 模糊查询，ES 性能提升百倍；相比于 Solr，ES 的 API 更现代化、扩展更方便。"

### 模板 7：遇到过的最棘手的 ES 性能问题
"一次线上 ES 集群的写入性能严重下降。排查过程：第一步，检查节点 CPU 发现某一节点 IO wait 异常高；第二步，查看 `/_nodes/hot_threads` 发现 merge 线程占满；第三步，分析发现该节点的数据索引频繁 force merge。原因：业务方每天凌晨对历史索引 force merge，同时该节点还承载了实时写入。解决方案：将 force merge 的任务调度到低峰期，并将写入节点和 merge 节点角色分离。优化后写入 TPS 从 2000 恢复到 15000。"

### 模板 8：ES 的数据一致性问题如何保证？
"ES 是一个最终一致性的分布式系统。对于单文档操作，可以设置 `wait_for_active_shards=all` 确保所有副本写入成功。对于跨文档事务，ES 不支持分布式事务，需要业务层面通过补偿机制保证。在日志等允许最终一致性的场景，使用默认配置即可。在金融等要求强一致的场景，建议在业务层使用 ID 生成策略 + 幂等写入 + 补偿校验来兜底。"

### 模板 9：ES 的冷热数据分离架构
"冷热分离是 ES 生产环境的必选项。通过配置节点标签将节点分为 hot、warm、cold 三层，hot 节点使用 SSD 承载实时写入和近 7 天查询，warm 节点使用 HDD 承载近期历史查询，cold 节点存储长期归档。结合 ILM 策略，索引在 hot 阶段满足 rollover 条件后自动迁移到 warm，30 天后迁移到 cold，90 天后删除。这套架构在保证查询体验的同时，将存储成本降低了 70%。"

### 模板 10：如何评估和规划 ES 集群规模？
"评估 ES 集群规模遵循：存储容量预估 → 写入 TPS 预估 → 查询 QPS 预估 → 节点规划。一般按：单 Data Node 分配 32GB JVM，搭配 1:1 的堆外内存用于 OS 缓存，单节点推荐承载 2-4TB 数据。分片数 = 总数据量 / 50GB（单分片推荐大小）。最终集群规模 = 分片数 / 节点最大分片数（建议每 GB JVM 不超过 20 个分片）。从 3 节点起步，随着数据增长水平扩展。"

---

## 八、快速查漏补缺Checklist

### 高级查询
- [ ] fuzzy / wildcard / prefix / query_string / boosting
- [ ] function_score 多种加权方式
- [ ] Nested 查询语法和性能优化
- [ ] Geo 查询（geo_distance / geo_shape）
- [ ] Explain API 调试得分

### Mapping 高级
- [ ] Dynamic Template 配置
- [ ] Index Template 配置
- [ ] Reindex 流程
- [ ] 多字段 multi-fields
- [ ] copy_to 合并字段
- [ ] 关闭 _source 的影响

### 分布式与集群
- [ ] 节点角色分离设计
- [ ] Discovery 机制（Seeded Gossip）
- [ ] 脑裂原理与解决方案
- [ ] Shard Allocation / Rerouting
- [ ] Disk Watermark 水位控制
- [ ] CCR 跨集群复制流程

### 性能调优
- [ ] Bulk 最佳大小（5-15MB）
- [ ] Translog 持久化策略选择
- [ ] Refresh / Merge 调优
- [ ] Force Merge 使用场景
- [ ] Doc Values 列式存储原理
- [ ] Global Ordinals / eager 加载
- [ ] Circuit Breaker 配置
- [ ] PIT + search_after 深分页

### 运维与监控
- [ ] ILM 策略配置完整流程
- [ ] SLM 快照自动化
- [ ] Filebeat / Logstash 配置
- [ ] Ingest Pipeline 处理器
- [ ] 集群健康三色（green/yellow/red）
- [ ] 慢日志配置

### Java 生态
- [ ] Java High Level REST Client (7.x)
- [ ] Spring Data Elasticsearch
- [ ] Bulk / Bool / Agg / Scroll 代码编写
