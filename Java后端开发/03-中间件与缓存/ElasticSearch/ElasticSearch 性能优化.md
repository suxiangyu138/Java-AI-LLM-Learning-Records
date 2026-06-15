# ElasticSearch 性能优化（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | ES 性能优化全指南
> **版本**：Elasticsearch 7.x/8.x
> **核心场景**：写入优化、查询优化、JVM调优、硬件配置

---

## 一、优化总览

```
ES 性能优化
├── 写入优化 → 高吞吐导入数据
├── 查询优化 → 搜索响应加速
├── 索引优化 → Mapping / Settings 设计
├── 硬件优化 → 磁盘 / 内存 / CPU
└── JVM 优化 → GC / 堆内存
```

---

## 二、写入性能优化

### 2.1 批量写入（Bulk API）

```java
// Java 端代码优化
public void optimizedBulk(List<Goods> list) {
    List<IndexRequest> requests = list.stream()
        .map(g -> new IndexRequest("goods")
            .id(String.valueOf(g.getId()))
            .source(JSON.toJSONString(g), XContentType.JSON))
        .collect(Collectors.toList());
    
    // 每批 1000 条，多线程并发
    BulkProcessor bulkProcessor = BulkProcessor.builder(
        (request, bulkListener) -> {
            requests.forEach(request::add);
        },
        new BulkProcessor.Listener() {
            @Override
            public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
                if (response.hasFailures()) {
                    log.error("Bulk 失败: {}", response.buildFailureMessage());
                }
            }
            @Override
            public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
                log.error("Bulk 异常", failure);
            }
        })
        .setBulkActions(1000)         // 每 1000 条一次
        .setBulkSize(new ByteSizeValue(5, ByteSizeUnit.MB))  // 或达到 5MB
        .setFlushInterval(TimeValue.timeValueSeconds(5))
        .setConcurrentRequests(2)    // 2 个并发 Bulk
        .build();
}
```

### 2.2 写入时期优化

```json
PUT /my_index
{
  "settings": {
    "refresh_interval": "30s",            // 调大 refresh，默认 1s
    "number_of_replicas": 0,              // 先关副本
    "translog": {
      "durability": "async",              // 异步 Translog
      "sync_interval": "30s"
    }
  }
}

// 数据导入完成后恢复
PUT /my_index/_settings
{
  "refresh_interval": "1s",
  "number_of_replicas": 1
}
```

### 2.3 写入优化 Checklist

| 优化项 | 调整 | 效果 |
|---|---|---|
| **调大 Refresh Interval** | 1s → 30s | 减少 Segment 生成 |
| **关闭副本** | 先设 0，写完再开 | 减少一倍写入量 |
| **异步 Translog** | async | 减少磁盘 I/O |
| **增加 Bulk 大小** | 500 → 1000-5000 | 减少网络开销 |
| **增加 Bulk 并发** | 1 → 4-8 | 提升吞吐 |
| **使用自动生成 ID** | 不用指定 ID | 避免 ID 去重开销 |
| **关闭 `_all` 字段** | 7.x 默认关闭 | 减少索引大小 |

---

## 三、查询性能优化

### 3.1 能用 filter 别用 must

```json
// ❌ 慢：参与评分计算
{ "bool": { "must": [{ "term": { "status": "active" } }] } }

// ✅ 快：不评分，自动缓存
{ "bool": { "filter": [{ "term": { "status": "active" } }] } }
```

filter 结果会被 ES 自动缓存（Node Query Cache），重复查询几乎零开销。

### 3.2 减少返回字段

```json
// 只取需要的字段，减少网络传输 + 磁盘读取
GET /goods/_search
{
  "_source": ["title", "price", "brand"],
  "query": { "match_all": {} }
}
```

### 3.3 调大 max_result_window（或换 search_after）

```yaml
# 不太好的做法：调大深分页上限（默认 10000）
PUT /goods/_settings
{
  "index.max_result_window": 50000
}
```

**更好的做法**：用 `search_after` 替代深分页（见 DSL 查询篇）。

### 3.4 合理设置 Shard 数

```
单 Shard 建议大小：30-50GB
太少：并发度不够，单 Shard 压力大
太多：每个 Shard 启动占用资源，协调节点归并成本高
```

### 3.5 查询优化 Checklist

| 优化项 | 做法 |
|---|---|
| **filter 替代 must** | 精确条件全部用 filter |
| **`_source` 过滤** | 只取需要的字段 |
| **避免深分页** | 用 search_after |
| **避免 `*` 通配符开头** | `*keyword` 极慢 |
| **避免 text 排序** | 用 `.keyword` 或 Doc Values |
| **强制合并** | 闲时 `_forcemerge?max_num_segments=1` |
| **减少 Shard 数** | 小数据量索引用 1 个 Shard 即可 |

---

## 四、Mapping 设计优化

### 4.1 关闭不必要的索引

```json
{
  "description": {
    "type": "text",
    "index": false        // 该字段不建索引（不搜索，仅供展示）
  }
}
```

### 4.2 禁用 Doc Values（不排序不聚合的字段）

```json
{
  "log_content": {
    "type": "text",
    "doc_values": false   // text 类型没有 doc_values，keyword 有
  },
  "session_id": {
    "type": "keyword",
    "doc_values": false   // 不需要排序/聚合，关掉省磁盘
  }
}
```

### 4.3 用 keyword 替代 text（不需要分词时）

```json
// 不需要全文搜索，用 keyword 更省空间
{ "status": { "type": "keyword" } }       // ✅
{ "status": { "type": "text" } }          // ❌ 浪费
```

---

## 五、JVM 优化

### 5.1 堆内存

```
堆大小 = min(物理内存的 50%, 32GB)
32GB 上限原因：JVM 指针压缩技术（Compressed Oops）在 32GB 以下生效

# jvm.options
-Xms16g
-Xmx16g
```

### 5.2 GC 配置

```bash
# 生产推荐 G1GC（8.x 默认）
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
```

### 5.3 GC 监控

```json
GET _nodes/stats/jvm?human
// 观察 heap_used_percent, gc 次数和时间
```

---

## 六、硬件优化

| 硬件 | 建议 | 原因 |
|---|---|---|
| **磁盘** | SSD，首选 NVMe | 磁盘 I/O 是最大瓶颈 |
| **内存** | 64G+（堆外给 OS Cache） | Lucene 严重依赖 OS Page Cache |
| **CPU** | 多核（8C+） | 分片并发 + Segment 合并消耗 CPU |
| **网络** | 万兆 | 节点间数据同步 |

**关键认知**：ES 堆内存之外的内存归 OS，OS Page Cache 会缓存倒排索引文件，所以物理内存要远大于 JVM 堆内存。

---

## 七、性能指标监控

```json
// 索引级别
GET _cat/indices/goods?v&s=store.size:desc
// 查看索引大小、文档数量

// 节点级别
GET _cat/nodes?v&h=name,heap.percent,ram.percent,cpu,disk.used_percent,load_1m

// 慢日志
GET goods/_settings?include_defaults&filter_path=*.slowlog

// 线程池
GET _cat/thread_pool?v&h=node_name,name,active,queue,rejected
```

关键监控指标：

| 指标 | 告警阈值 |
|---|---|
| **Heap Used** | > 80% |
| **CPU** | 持续 > 80% |
| **Disk Used** | > 85% |
| **GC 时间** | > 1s 频繁出现 |
| **Thread Pool Rejected** | > 0 |
| **Unassigned Shards** | > 0 |
| **Search Queue** | 持续增长 |

---

## 八、面试核心要点

1. **写入怎么提性能？** Bulk + 调大 refresh_interval + 关副本
2. **查询怎么提性能？** filter 替代 must + _source 过滤 + 避免深分页
3. **堆内存设多少？** 不超过 32GB（指针压缩），不超过物理内存 50%
4. **磁盘为什么必须 SSD？** Segment 合并、Flush 都是顺序大 I/O
5. **慢查询怎么看？** 开启 slowlog，Kibana 监控，ES APM

---

## 九、极简总结

```
写入快 = Bulk 分批(1000) + refresh 30s + 关副本
查询快 = filter 替代 must + _source 精简 + 避免通配符/深分页
内存 = 堆 50%物理/上限32G，剩余全给 OS Cache
磁盘 = 必须 SSD，NVMe 更好
建议每个 Shard 存 30-50GB 数据
```
