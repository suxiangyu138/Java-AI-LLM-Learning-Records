# Spring Boot 集成 Elasticsearch 与实战场景
> 从依赖配置到 CRUD 操作，从商品搜索到 MySQL 数据同步——ES 在微服务架构中的完整落地指南。

## 目录
1. [技术选型与版本对照](#1-技术选型与版本对照)
2. [Spring Boot 基础集成](#2-spring-boot-基础集成)
3. [RestHighLevelClient 原生 DSL](#3-resthighlevelclient-原生-dsl)
4. [ElasticsearchRepository CRUD](#4-elasticsearchrepository-crud)
5. [ElasticsearchRestTemplate 实战](#5-elasticsearchresttemplate-实战)
6. [商品搜索系统实战](#6-商品搜索系统实战)
7. [数据同步方案](#7-数据同步方案)
8. [ELK 日志分析](#8-elk-日志分析)
9. [RAG 语义检索与自动补全](#9-rag-语义检索与自动补全)
10. [索引重建与最佳实践](#10-索引重建与最佳实践)
11. [实战场景总览](#11-实战场景总览)

---

## 1. 技术选型与版本对照

### 1.1 版本对应关系

| Spring Boot | ES | 客户端 | 备注 |
|:-----------:|:--:|--------|------|
| 2.3.x | 7.x | RestHighLevelClient | 企业主流组合 |
| 2.7.x | 7.17 | RestHighLevelClient | 7.x 最终版本 |
| 3.0+ | 8.x | ElasticsearchClient | 新版 Lambda 风格 |

### 1.2 开发方式对比

| 方式 | 优点 | 缺点 | 适用场景 |
|------|------|------|---------|
| RestHighLevelClient | 最灵活，完整 DSL 能力 | 代码量多 | 复杂搜索场景 |
| ElasticsearchRepository | 极简 CRUD，类似 JPA | 功能有限 | 简单增删改查 |
| ElasticsearchRestTemplate | Spring 封装，介于两者之间 | 需理解模板 API | 企业主力推荐 |

---

## 2. Spring Boot 基础集成

### 2.1 Maven 依赖

```xml
<!-- Spring Boot 2.x + ES 7.x -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
<dependency>
    <groupId>org.elasticsearch.client</groupId>
    <artifactId>elasticsearch-rest-high-level-client</artifactId>
    <version>7.17.0</version>
</dependency>
```

### 2.2 配置

```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
    connection-timeout: 10s
    socket-timeout: 30s
```

### 2.3 配置类

```java
@Configuration
public class ElasticsearchConfig {
    @Bean
    public RestHighLevelClient restHighLevelClient() {
        return new RestHighLevelClient(
            RestClient.builder(HttpHost.create("http://localhost:9200"))
                .setRequestConfigCallback(b ->
                    b.setConnectTimeout(10000).setSocketTimeout(60000))
                .setHttpClientConfigCallback(b ->
                    b.setMaxConnTotal(100).setMaxConnPerRoute(100))
        );
    }
}
```

---

## 3. RestHighLevelClient 原生 DSL

### 3.1 索引操作

```java
@Component
public class EsIndexService {
    @Autowired
    private RestHighLevelClient client;

    public void createIndex() throws IOException {
        CreateIndexRequest request = new CreateIndexRequest("goods");
        request.settings(Settings.builder()
            .put("number_of_shards", 3).put("number_of_replicas", 1));
        String mapping = "{\"dynamic\":\"strict\",\"properties\":{" +
            "\"title\":{\"type\":\"text\",\"analyzer\":\"ik_max_word\"}," +
            "\"price\":{\"type\":\"double\"},\"category\":{\"type\":\"keyword\"}}}";
        request.mapping(mapping, XContentType.JSON);
        CreateIndexResponse response = client.indices().create(request, RequestOptions.DEFAULT);
        log.info("索引创建: {}", response.isAcknowledged());
    }
}
```

### 3.2 文档 CRUD

```java
@Component
public class EsDocService {
    @Autowired private RestHighLevelClient client;
    private static final String INDEX = "goods";

    public void save(Goods goods) throws IOException {
        client.index(new IndexRequest(INDEX).id(String.valueOf(goods.getId()))
            .source(JSON.toJSONString(goods), XContentType.JSON), RequestOptions.DEFAULT);
    }

    public Goods findById(Long id) throws IOException {
        GetResponse r = client.get(new GetRequest(INDEX, String.valueOf(id)), RequestOptions.DEFAULT);
        return r.isExists() ? JSON.parseObject(r.getSourceAsString(), Goods.class) : null;
    }

    public void update(Long id, Map<String, Object> fields) throws IOException {
        client.update(new UpdateRequest(INDEX, String.valueOf(id)).doc(fields), RequestOptions.DEFAULT);
    }

    public void delete(Long id) throws IOException {
        client.delete(new DeleteRequest(INDEX, String.valueOf(id)), RequestOptions.DEFAULT);
    }
}
```

### 3.3 搜索与高亮

```java
@Component
public class EsSearchService {
    @Autowired private RestHighLevelClient client;

    public SearchResponse search(String keyword, String category, Double minPrice,
                                  Double maxPrice, int page, int size) throws IOException {
        BoolQueryBuilder bool = QueryBuilders.boolQuery()
            .must(QueryBuilders.matchQuery("title", keyword))
            .filter(QueryBuilders.termQuery("is_on_sale", true));
        if (category != null) bool.filter(QueryBuilders.termQuery("category", category));
        if (minPrice != null || maxPrice != null) {
            RangeQueryBuilder r = QueryBuilders.rangeQuery("price");
            if (minPrice != null) r.gte(minPrice);
            if (maxPrice != null) r.lte(maxPrice);
            bool.filter(r);
        }
        SearchSourceBuilder s = new SearchSourceBuilder().query(bool)
            .from((page - 1) * size).size(size)
            .fetchSource(new String[]{"id", "title", "price", "category"}, null);
        HighlightBuilder h = new HighlightBuilder();
        h.field("title").preTags("<em>").postTags("</em>");
        s.highlighter(h);
        return client.search(new SearchRequest("goods").source(s), RequestOptions.DEFAULT);
    }
}
```

---

## 4. ElasticsearchRepository CRUD

### 4.1 实体类

```java
@Data
@Document(indexName = "goods")
public class Goods {
    @Id private Long id;
    @Field(type = FieldType.Text, analyzer = "ik_max_word") private String title;
    @Field(type = FieldType.Double) private Double price;
    @Field(type = FieldType.Keyword) private String category;
    @Field(type = FieldType.Keyword) private String brand;
    @Field(type = FieldType.Boolean) private Boolean isOnSale;
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second) private Date createTime;
}
```

### 4.2 Repository 接口

```java
@Repository
public interface GoodsRepository extends ElasticsearchRepository<Goods, Long> {
    List<Goods> findByTitle(String title);
    List<Goods> findByCategoryAndBrand(String category, String brand);
    List<Goods> findByPriceBetween(Double min, Double max);
    Page<Goods> findByTitleContaining(String title, Pageable pageable);

    @Query("{\"bool\":{\"must\":[{\"match\":{\"title\":\"?0\"}}]," +
           "\"filter\":[{\"range\":{\"price\":{\"gte\":?1,\"lte\":?2}}}]}}")
    List<Goods> searchByTitleAndPrice(String title, Double min, Double max);
}
```

### 4.3 方法命名规则

| 方法名 | 生成的查询 |
|--------|-----------|
| `findByTitle` | term 精确查询 |
| `findByTitleContaining` | match 模糊查询 |
| `findByPriceBetween` | range 查询 |
| `findByCategoryAndBrand` | bool must 组合 |
| `findByBrandIn` | terms 查询 |

> ⚠️ Repository 适合简单 CRUD，复杂查询（高亮、聚合、嵌套）必须使用 RestTemplate。

---

## 5. ElasticsearchRestTemplate 实战

### 5.1 NativeSearchQuery 构建

```java
@Service
@RequiredArgsConstructor
public class GoodsSearchService {

    private final ElasticsearchRestTemplate restTemplate;

    public SearchResultVO<Goods> search(SearchParam param) {
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        if (StrUtil.isNotBlank(param.getKeyword())) {
            bool.must(QueryBuilders.multiMatchQuery(param.getKeyword())
                .field("title", 3.0f).field("brand", 1.5f));
        }
        if (CollUtil.isNotEmpty(param.getCategories())) {
            bool.filter(QueryBuilders.termsQuery("category", param.getCategories()));
        }
        if (param.getMinPrice() != null || param.getMaxPrice() != null) {
            RangeQueryBuilder r = QueryBuilders.rangeQuery("price");
            if (param.getMinPrice() != null) r.gte(param.getMinPrice());
            if (param.getMaxPrice() != null) r.lte(param.getMaxPrice());
            bool.filter(r);
        }
        bool.filter(QueryBuilders.termQuery("isOnSale", true));

        NativeSearchQueryBuilder builder = new NativeSearchQueryBuilder()
            .withQuery(bool)
            .withPageable(PageRequest.of(param.getPage() - 1, param.getSize()))
            .withSorts(SortBuilders.scoreSort().order(SortOrder.DESC))
            .withHighlightFields(new HighlightBuilder.Field("title")
                .preTags("<em>").postTags("</em>"))
            .addAggregation(AggregationBuilders.terms("category_agg").field("category").size(20))
            .addAggregation(AggregationBuilders.terms("brand_agg").field("brand").size(50))
            .addAggregation(AggregationBuilders.range("price_range").field("price")
                .addUnboundedTo(1000).addRange(1000, 3000)
                .addRange(3000, 5000).addUnboundedFrom(5000));

        SearchHits<Goods> hits = restTemplate.search(builder.build(), Goods.class);
        return buildResult(hits);
    }

    private SearchResultVO<Goods> buildResult(SearchHits<Goods> hits) {
        List<GoodsVO> list = hits.getSearchHits().stream().map(hit -> {
            GoodsVO vo = BeanUtil.copyProperties(hit.getContent(), GoodsVO.class);
            List<String> hl = hit.getHighlightField("title");
            if (CollUtil.isNotEmpty(hl)) vo.setTitleHighlight(hl.get(0));
            return vo;
        }).collect(Collectors.toList());

        Map<String, List<BucketVO>> filters = new HashMap<>();
        Aggregations aggs = hits.getAggregations();
        if (aggs != null) {
            filters.put("categories", parseBuckets(aggs.get("category_agg")));
            filters.put("brands", parseBuckets(aggs.get("brand_agg")));
        }
        SearchResultVO<Goods> r = new SearchResultVO<>();
        r.setTotal(hits.getTotalHits()); r.setRecords(list); r.setFilters(filters);
        return r;
    }

    private List<BucketVO> parseBuckets(ParsedStringTerms t) {
        if (t == null) return Collections.emptyList();
        return t.getBuckets().stream().map(b -> new BucketVO(b.getKeyAsString(), b.getDocCount())).collect(Collectors.toList());
    }
}
```

---

## 6. 商品搜索系统实战

### 6.1 系统架构

```text
         用户
          ↓
     Nginx（负载均衡）
          ↓
   SpringBoot（API 层）
       ↙              ↘
    MySQL           Elasticsearch
  (业务数据)         (搜索数据)
       ↖              ↗
       Canal 实时同步
```

### 6.2 搜索 DTO

```java
@Data
public class SearchParam {
    private String keyword;
    private List<String> categories, brands;
    private Double minPrice, maxPrice;
    private Integer sortType; // 0:综合 1:价格升 2:价格降 3:销量
    private Integer page = 1, size = 20;
}

@Data
public class SearchResultVO<T> {
    private long total;
    private List<T> records;
    private Map<String, List<BucketVO>> filters;
}

@Data @AllArgsConstructor
public static class BucketVO {
    private String key;
    private long count;
}
```

### 6.3 Service 完整实现

```java
@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ElasticsearchRestTemplate restTemplate;

    public SearchResultVO<Product> search(SearchParam param) {
        BoolQueryBuilder bool = QueryBuilders.boolQuery();

        if (StrUtil.isNotBlank(param.getKeyword())) {
            bool.must(QueryBuilders.multiMatchQuery(param.getKeyword())
                .field("title", 3.0f).field("brand", 1.5f));
        } else {
            bool.must(QueryBuilders.matchAllQuery());
        }
        if (CollUtil.isNotEmpty(param.getCategories()))
            bool.filter(QueryBuilders.termsQuery("category", param.getCategories()));
        if (CollUtil.isNotEmpty(param.getBrands()))
            bool.filter(QueryBuilders.termsQuery("brand", param.getBrands()));
        if (param.getMinPrice() != null || param.getMaxPrice() != null) {
            RangeQueryBuilder r = QueryBuilders.rangeQuery("price");
            if (param.getMinPrice() != null) r.gte(param.getMinPrice());
            if (param.getMaxPrice() != null) r.lte(param.getMaxPrice());
            bool.filter(r);
        }
        bool.filter(QueryBuilders.termQuery("isOnSale", true));

        NativeSearchQueryBuilder builder = new NativeSearchQueryBuilder().withQuery(bool)
            .withPageable(PageRequest.of(param.getPage() - 1, param.getSize()));

        SortBuilder<?> sort = switch (param.getSortType() != null ? param.getSortType() : 0) {
            case 1 -> SortBuilders.fieldSort("price").order(SortOrder.ASC);
            case 2 -> SortBuilders.fieldSort("price").order(SortOrder.DESC);
            case 3 -> SortBuilders.fieldSort("sales").order(SortOrder.DESC);
            default -> SortBuilders.scoreSort().order(SortOrder.DESC);
        };
        builder.withSorts(sort, SortBuilders.fieldSort("createTime").order(SortOrder.DESC));
        builder.withHighlightFields(new HighlightBuilder.Field("title")
            .preTags("<em>").postTags("</em>"));
        builder.addAggregation(AggregationBuilders.terms("category_agg").field("category").size(20))
               .addAggregation(AggregationBuilders.terms("brand_agg").field("brand").size(50))
               .addAggregation(AggregationBuilders.range("price_range").field("price")
                   .addUnboundedTo(1000).addRange(1000, 3000)
                   .addRange(3000, 5000).addUnboundedFrom(5000));

        SearchHits<Product> hits = restTemplate.search(builder.build(), Product.class);
        return buildResult(hits);
    }
}
```

---

## 7. 数据同步方案

### 7.1 方案对比

| 方案 | 实时性 | 复杂度 | 一致性 | 推荐 |
|------|:-----:|:------:|:------:|:----:|
| Canal 订阅 binlog | 秒级 | 中 | 最终一致 | ⭐ 推荐 |
| Logstash 定时同步 | 分钟级 | 低 | 最终一致 | 可接受延迟 |
| 应用双写 | 实时 | 低 | 可能不一致 | 业务侵入 |
| MQ 异步同步 | 秒级 | 中 | 最终一致 | 通用 |

### 7.2 Canal 实时同步

```text
MySQL binlog → Canal 监听 → 解析变更 → MQ → Consumer → 写入 ES

幂等性：doc_as_upsert 确保重复消费不报错
顺序性：同一行变更按主键分区保证顺序
延迟：通常 200ms-500ms
```

```java
@Component @RequiredArgsConstructor
public class CanalSyncService {
    private final EsDocService esDocService;

    @EventListener
    public void handleBinlog(BinlogEvent event) {
        if (!"goods".equals(event.getTable())) return;
        switch (event.getAction()) {
            case "INSERT" -> esDocService.save(event.getData());
            case "UPDATE" -> esDocService.save(event.getData());
            case "DELETE" -> esDocService.delete(event.getData().getId());
        }
    }
}
```

### 7.3 Logstash 定时同步

```bash
input {
  jdbc {
    jdbc_connection_string => "jdbc:mysql://localhost:3306/es_db"
    jdbc_user => "root"
    jdbc_password => "password"
    statement => "SELECT * FROM goods WHERE update_time > :sql_last_value"
    schedule => "*/5 * * * *"
    tracking_column => "update_time"
  }
}
output {
  elasticsearch {
    hosts => ["localhost:9200"]
    index => "goods"
    document_id => "%{id}"
    action => "upsert"
    doc_as_upsert => true
  }
}
```

### 7.4 双写策略

```java
@Transactional
public void saveGoods(Goods goods) {
    goodsMapper.insert(goods);
    try {
        esDocService.save(goods);
    } catch (Exception e) {
        log.error("ES 同步失败, id: {}", goods.getId(), e);
        mqTemplate.send("sync.es.retry", goods.getId());
    }
}
```

> 💡 生产推荐 Canal + MQ 方案，解耦、可靠持久化、可观测。

---

## 8. ELK 日志分析

### 8.1 架构

```text
应用日志 → Filebeat 采集 → Logstash 解析 → Elasticsearch 存储 → Kibana 可视化
```

### 8.2 Logback + Filebeat 配置

```xml
<appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>/var/log/app/app.json</file>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <fileNamePattern>/var/log/app/app.%d{yyyy-MM-dd}.json</fileNamePattern>
        <maxHistory>30</maxHistory>
    </rollingPolicy>
</appender>
```

```yaml
# filebeat.yml
filebeat.inputs:
  - type: log
    paths: /var/log/app/*.json
    json.keys_under_root: true
output.elasticsearch:
  hosts: ["es-node1:9200", "es-node2:9200"]
  index: "app-logs-%{+yyyy.MM.dd}"
```

### 8.3 日志查询

```json
// 最近一小时 Error 日志
GET /app-logs-*/_search
{
  "query": {
    "bool": {
      "must": [{ "match": { "level": "ERROR" } }],
      "filter": [{ "range": { "@timestamp": { "gte": "now-1h" } } }]
    }
  },
  "sort": [{ "@timestamp": "desc" }], "size": 50
}

// 按服务统计错误数
GET /app-logs-*/_search
{
  "size": 0,
  "query": { "bool": { "must": [{ "match": { "level": "ERROR" } }],
    "filter": [{ "range": { "@timestamp": { "gte": "now-1d" } } }] } },
  "aggs": { "by_service": { "terms": { "field": "service_name.keyword", "size": 20 } } }
}
```

---

## 9. RAG 语义检索与自动补全

### 9.1 dense_vector 向量检索

```json
PUT /knowledge_base
{
  "mappings": {
    "properties": {
      "title": { "type": "text", "analyzer": "ik_max_word" },
      "content": { "type": "text", "analyzer": "ik_max_word" },
      "content_vector": {
        "type": "dense_vector",
        "dims": 768, "index": true, "similarity": "cosine"
      }
    }
  }
}

// 向量搜索
GET /knowledge_base/_search
{
  "knn": {
    "field": "content_vector",
    "query_vector": [0.23, -0.56, 0.78],
    "k": 10, "num_candidates": 50
  },
  "_source": ["title", "content"]
}

// 混合搜索
GET /knowledge_base/_search
{
  "query": { "match": { "title": "微服务架构" } },
  "knn": {
    "field": "content_vector", "query_vector": [0.23, -0.56, 0.78],
    "k": 10, "num_candidates": 50
  }
}
```

### 9.2 Completion Suggester 自动补全

```json
PUT /suggest_index
{ "mappings": { "properties": { "suggest": { "type": "completion" } } } }

POST /suggest_index/_doc
{ "suggest": { "input": ["华为手机", "华为Mate60", "华为P50 Pro"], "weight": 100 } }

POST /suggest_index/_search
{
  "suggest": {
    "title_suggest": {
      "prefix": "华为",
      "completion": { "field": "suggest", "size": 5, "skip_duplicates": true }
    }
  }
}
```

### 9.3 ES 8.x Lambda 客户端

```java
ElasticsearchClient client = ...;
SearchResponse<Goods> response = client.search(s -> s
    .index("goods")
    .query(q -> q.bool(b -> b
        .must(m -> m.match(t -> t.field("title").query("手机")))
        .filter(f -> f.term(t -> t.field("category").value("数码")))
    ))
    .size(20), Goods.class);
```

---

## 10. 索引重建与最佳实践

### 10.1 别名 + Reindex 零停机迁移

```java
@Service @RequiredArgsConstructor
public class IndexReindexService {
    private final ElasticsearchRestTemplate restTemplate;

    public void reindexWithAlias(String oldIndex, String newIndex, String aliasName) {
        restTemplate.indexOps(IndexCoordinates.of(newIndex)).create();

        ReindexRequest r = new ReindexRequest();
        r.setSourceIndex(oldIndex); r.setDestIndex(newIndex); r.setConflicts("proceed");
        ReindexResponse res = restTemplate.reindex(r, String.class);
        log.info("Reindex 完成: total={}", res.getTotal());

        AliasActions aa = new AliasActions();
        aa.add(new AliasAction.Remove(
            AliasActionParameters.builder().withIndices(oldIndex).withAliases(aliasName).build()));
        aa.add(new AliasAction.Add(
            AliasActionParameters.builder().withIndices(newIndex).withAliases(aliasName).build()));
        restTemplate.indexOps(IndexCoordinates.of(oldIndex, newIndex)).alias(aa);
    }
}
```

### 10.2 生产最佳实践 Checklist

- [ ] Mapping 使用 `strict` 模式
- [ ] text 字段配置 ik 分词器 + keyword 多字段
- [ ] 别名代替索引名（零停机切换）
- [ ] 生产关闭 `dynamic` 映射
- [ ] 合理分片（单分片 20-50GB）
- [ ] 索引模板 + ILM 生命周期管理
- [ ] filter 代替 must（自动缓存）
- [ ] 设置慢查询日志阈值
- [ ] 写入期间关闭副本和 refresh
- [ ] 只读索引执行 force merge

---

## 11. 实战场景总览

### 11.1 场景 vs 技术方案

| 场景 | 方案 | 核心组件 |
|------|------|----------|
| 商品搜索 | ES 搜索索引 + MySQL 业务数据 | Canal 同步 |
| 日志分析 | Filebeat → Logstash → ES → Kibana | ELK Stack |
| 数据同步 | MySQL binlog → Canal → MQ → ES | Canal + RocketMQ |
| RAG 语义检索 | dense_vector + cosine similarity | ES 8.x |
| 站内搜索 | match + bool + aggregation + highlight | SpringBoot + ES |
| 自动补全 | Completion Suggester | suggest API |

### 11.2 ES 使用时机

| 场景 | 适合 ES？ | 原因 |
|------|:---------:|------|
| 商品搜索（多关键词模糊查询） | ✅ | ES 核心能力 |
| 日志检索与监控 | ✅ | ELK 标配 |
| 大数据量复杂聚合报表 | ✅ | 聚合性能优秀 |
| 知识库语义搜索 | ✅ | 8.x 向量搜索 |
| 订单列表（状态筛选+日期排序） | ❌ | MySQL 即可 |
| 交易扣款 | ❌ | 需要事务 |
| 后台管理简单 CRUD | ❌ | MySQL 更适合 |

> 🎯 **黄金法则**：MySQL 存业务数据做事务，ES 做搜索和分析——互补不替代。生产环境 3 节点起步，用好 filter 缓存，Canal 同步保数据一致性。
