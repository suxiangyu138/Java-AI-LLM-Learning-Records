# ElasticSearch 实战场景与最佳实践（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | ES 生产级实战指南
> **版本**：Elasticsearch 7.x/8.x
> **核心场景**：商品搜索、日志分析、MySQL 数据同步、RAG 语义检索

---

## 一、实战场景总览

| 场景 | 方案 | 关键组件 |
|---|---|---|
| 商品搜索 | ES 存储商品索引 + MySQL 存业务数据 | ES + Canal/DTS 同步 |
| 日志分析 | Filebeat → ES → Kibana | ELK Stack |
| 数据同步 | MySQL → ES 实时同步 | Canal、Logstash |
| RAG 语义检索 | ES 向量搜索 | dense_vector + cosine |
| 站内搜索 | 关键词搜索 + 聚合筛选 + 高亮 | SpringBoot + ES |
| 自动补全 | Completion Suggester | suggest API |

---

## 二、经典场景：商品搜索系统

### 2.1 架构图

```
         用户
          ↓
     Nginx（负载）
          ↓
   SpringBoot（API）
       ↙       ↘
    MySQL      ElasticSearch
  (业务数据)   (搜索数据)
       ↖       ↗
       Canal/同步
```

### 2.2 数据同步方案

```
MySQL binlog → Canal 监听 → 解析变更 → 同步到 ES
```

**Canal 同步流程**：

```
1. MySQL INSERT → Canal 抓到 binlog → 转成 JSON → 写 ES
2. MySQL UPDATE → Canal 抓到 binlog → 更新 ES
3. MySQL DELETE → Canal 抓到 binlog → 删除 ES
```

**核心注意**：

- 幂等性：Canal 消息可能重复消费，用 ES 的 `doc_as_upsert` 确保幂等
- 顺序性：同一行的变更必须顺序处理（Canal 按表+主键分区保证）
- 延迟：通常 200ms-500ms，秒级延迟可接受

### 2.3 SpringBoot 搜索 Service 完整示例

```java
@Service
@RequiredArgsConstructor
public class ProductSearchService {
    
    private final ElasticsearchRestTemplate restTemplate;
    
    /**
     * 商品综合搜索：关键词 + 分类过滤 + 品牌过滤 + 价格区间 + 排序
     */
    public SearchResultVO<ProductVO> search(SearchParam param) {
        
        // 1. 构建查询
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        
        // 关键词搜索
        if (StrUtil.isNotBlank(param.getKeyword())) {
            boolQuery.must(QueryBuilders.multiMatchQuery(param.getKeyword())
                .field("title", 3.0f)
                .field("brand", 1.5f)
                .field("description", 1.0f));
        } else {
            boolQuery.must(QueryBuilders.matchAllQuery());
        }
        
        // 分类过滤（filter：不评分，可缓存）
        if (CollUtil.isNotEmpty(param.getCategories())) {
            boolQuery.filter(QueryBuilders.termsQuery("category.keyword", 
                param.getCategories()));
        }
        
        // 品牌过滤
        if (CollUtil.isNotEmpty(param.getBrands())) {
            boolQuery.filter(QueryBuilders.termsQuery("brand.keyword", 
                param.getBrands()));
        }
        
        // 价格区间
        if (param.getMinPrice() != null || param.getMaxPrice() != null) {
            RangeQueryBuilder range = QueryBuilders.rangeQuery("price");
            if (param.getMinPrice() != null) range.gte(param.getMinPrice());
            if (param.getMaxPrice() != null) range.lte(param.getMaxPrice());
            boolQuery.filter(range);
        }
        
        // 仅上架
        boolQuery.filter(QueryBuilders.termQuery("is_on_sale", true));
        
        // 2. 聚合（搜索结果面筛选）
        NativeSearchQuery query = new NativeSearchQueryBuilder()
            .withQuery(boolQuery)
            .withAggregations(
                AggregationBuilders.terms("category_agg").field("category.keyword").size(20),
                AggregationBuilders.terms("brand_agg").field("brand.keyword").size(50),
                AggregationBuilders.range("price_range").field("price")
                    .addUnboundedTo(1000)
                    .addRange(1000, 3000)
                    .addRange(3000, 5000)
                    .addUnboundedFrom(5000)
            )
            // 3. 排序
            .withSorts(parseSort(param.getSortType()))
            // 4. 分页
            .withPageable(PageRequest.of(param.getPage() - 1, param.getSize()))
            // 5. 高亮
            .withHighlightFields(
                new HighlightBuilder.Field("title")
                    .preTags("<em>").postTags("</em>").fragmentSize(100).numOfFragments(1)
            )
            .build();
        
        // 6. 执行
        SearchHits<Product> hits = restTemplate.search(query, Product.class);
        
        // 7. 组装结果
        return buildResult(hits, param);
    }
    
    private List<SortBuilder<?>> parseSort(Integer sortType) {
        List<SortBuilder<?>> sorts = new ArrayList<>();
        if (sortType == null || sortType == 0) {
            sorts.add(SortBuilders.scoreSort().order(SortOrder.DESC));  // 综合
        } else if (sortType == 1) {
            sorts.add(SortBuilders.fieldSort("price").order(SortOrder.ASC));  // 价格升序
        } else if (sortType == 2) {
            sorts.add(SortBuilders.fieldSort("sales").order(SortOrder.DESC)); // 销量
        }
        sorts.add(SortBuilders.fieldSort("create_time").order(SortOrder.DESC));
        return sorts;
    }
}
```

---

## 三、经典场景：ELK 日志分析

### 3.1 ELK 架构

```
应用日志 → Filebeat 采集 → Logstash 处理 → Elasticsearch 存储 → Kibana 可视化
```

### 3.2 Logback 输出 JSON（给 Filebeat 采集）

```xml
<!-- logback-spring.xml -->
<appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>/var/log/app/app.json</file>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <fileNamePattern>/var/log/app/app.%d{yyyy-MM-dd}.json</fileNamePattern>
        <maxHistory>30</maxHistory>
    </rollingPolicy>
</appender>
```

### 3.3 Filebeat 配置

```yaml
# filebeat.yml
filebeat.inputs:
  - type: log
    enabled: true
    paths:
      - /var/log/app/*.json
    json.keys_under_root: true
    json.add_error_key: true

output.elasticsearch:
  hosts: ["es-node1:9200", "es-node2:9200", "es-node3:9200"]
  index: "app-logs-%{+yyyy.MM.dd}"
  
setup.template:
  name: "app-logs"
  pattern: "app-logs-*"
```

### 3.4 日志查询示例

```json
// 查询包含 Error 的日志，最近 1 小时
GET /app-logs-*/_search
{
  "query": {
    "bool": {
      "must": [{ "match": { "message": "Error" } }],
      "filter": [{
        "range": { "@timestamp": { "gte": "now-1h" } }
      }]
    }
  },
  "sort": [{ "@timestamp": "desc" }],
  "size": 50
}

// 按服务名聚合，统计错误数量
GET /app-logs-*/_search
{
  "size": 0,
  "query": {
    "bool": {
      "must": [{ "match": { "level": "ERROR" } }],
      "filter": [{ "range": { "@timestamp": { "gte": "now-1d" } } }]
    }
  },
  "aggs": {
    "by_service": {
      "terms": { "field": "service_name.keyword", "size": 20 }
    }
  }
}
```

---

## 四、经典场景：RAG 向量语义检索（ES 8.x）

### 4.1 创建带向量的 Mapping

```json
PUT /knowledge_base
{
  "mappings": {
    "properties": {
      "title": { "type": "text", "analyzer": "ik_max_word" },
      "content": { "type": "text", "analyzer": "ik_max_word" },
      "content_vector": {
        "type": "dense_vector",
        "dims": 768,
        "index": true,
        "similarity": "cosine"  // 余弦相似度
      }
    }
  }
}
```

### 4.2 向量搜索

```json
GET /knowledge_base/_search
{
  "knn": {
    "field": "content_vector",
    "query_vector": [0.23, -0.56, 0.78, ...],  // 768 维向量，通过 embedding 模型生成
    "k": 10,
    "num_candidates": 50
  },
  "_source": ["title", "content"]
}
```

### 4.3 混合搜索（向量 + 关键词）

```json
GET /knowledge_base/_search
{
  "query": {
    "bool": {
      "must": [
        { "match": { "title": "微服务架构" } }
      ]
    }
  },
  "knn": {
    "field": "content_vector",
    "query_vector": [0.23, -0.56, ...],
    "k": 10,
    "num_candidates": 50
  }
}
```

---

## 五、经典场景：自动补全（Suggester）

```json
PUT /suggest_index
{
  "mappings": {
    "properties": {
      "suggest": { "type": "completion" }
    }
  }
}

// 插入数据
POST /suggest_index/_doc
{
  "suggest": {
    "input": ["华为手机", "华为Mate60", "华为P50"],
    "weight": 100
  }
}

// 搜索补全
POST /suggest_index/_search
{
  "suggest": {
    "title_suggest": {
      "prefix": "华为",
      "completion": { "field": "suggest", "size": 10 }
    }
  }
}
```

---

## 六、ES 使用时机判断

| 场景 | 用 ES？ | 说明 |
|---|---|---|
| 商品搜索（多个关键词模糊查） | ✅ 适合 | ES 核心能力 |
| 订单列表（按状态筛选+日期排序） | ❌ 不适合 | MySQL 更适合，除非数据量极大 |
| 交易扣款 | ❌ 不适合 | 需要事务，ES 不支持 |
| 日志检索与监控 | ✅ 适合 | ELK 标配 |
| 后台管理简单 CRUD | ❌ 不适合 | MySQL 即可 |
| 大数据量复杂聚合报表 | ✅ 适合 | ES 聚合性能优秀 |
| 知识库语义搜索 | ✅ 适合 | 8.x 向量搜索 |

---

## 七、MySQL + ES 数据一致性方案

### 7.1 方案对比

| 方案 | 延迟 | 一致性 | 复杂度 |
|---|---|---|---|
| **Canal 监听 Binlog** | 秒级 | 最终一致 | 中 |
| **应用双写** | 实时 | 可能不一致 | 低但不可靠 |
| **MQ 异步同步** | 毫秒-秒级 | 最终一致 | 中 |
| **定时扫表全量同步** | 分钟级 + | 最终一致 | 低 |

### 7.2 推荐：Canal + MQ

```
MySQL → Canal → RocketMQ/Kafka → Consumer → ES

优势：
- 解耦：Canal 不需要知道 ES 地址
- 可靠：MQ 持久化，消费失败可重试
- 可观测：MQ 消息量就是同步量
```

---

## 八、面试核心要点

1. **MySQL 数据怎么同步到 ES？** Canal 监听 binlog → 解析 → 写入 ES
2. **ES 为什么不能替代 MySQL？** 无事务、无 JOIN、写入非实时
3. **ES 8.x 向量搜索能做啥？** 语义检索、相似度搜索、RAG
4. **自动补全怎么实现？** Completion Suggester
5. **数据一致性怎么保证？** Canal + MQ 最终一致，消费幂等

---

## 九、极简总结

```
商品搜索 = MySQL 存业务数据 + ES 存搜索索引 + Canal 同步
ELK 日志 = Filebeat 采集 + Logstash 处理 + ES 存储 + Kibana 可视化
ES 用在哪 = 搜索、日志、大数据量模糊查、语义检索
ES 不适用 = 事务、金融、JOIN、简单 CRUD
最佳实践 = filter 做过滤、别开深分页、生产至少 3 节点
```
