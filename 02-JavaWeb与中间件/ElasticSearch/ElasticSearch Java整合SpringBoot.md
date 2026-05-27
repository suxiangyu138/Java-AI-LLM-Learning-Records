# ElasticSearch Java 整合 SpringBoot（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | SpringBoot 集成 ES 完整方案
> **版本**：SpringBoot 2.x/3.x | Elasticsearch 7.x/8.x
> **核心场景**：商品搜索、日志查询、数据同步、DSL 操作

---

## 一、依赖选择

### 1.1 SpringBoot 2.x + ES 7.x（企业主流）

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>

<!-- 补充 ES 客户端（高版本需单独引入） -->
<dependency>
    <groupId>org.elasticsearch.client</groupId>
    <artifactId>elasticsearch-rest-high-level-client</artifactId>
    <version>7.17.0</version>
</dependency>
```

### 1.2 SpringBoot 3.x + ES 8.x

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
<!-- 8.x 使用新版 ElasticsearchClient，不再需要 High Level Client -->
```

---

## 二、配置文件

```yaml
# application.yml
spring:
  elasticsearch:
    uris: http://localhost:9200        # 单节点
    # uris: http://host1:9200,host2:9200,host3:9200  # 集群
    
    # 8.x 如果开启了安全认证
    # username: elastic
    # password: your_password
    
    connection-timeout: 3s
    socket-timeout: 60s
```

---

## 三、方式一：ElasticsearchRepository（极简 CRUD）

### 3.1 实体类

```java
@Data
@Document(indexName = "goods")
public class Goods {
    
    @Id
    private Long id;
    
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String title;
    
    @Field(type = FieldType.Double)
    private BigDecimal price;
    
    @Field(type = FieldType.Keyword)
    private String category;
    
    @Field(type = FieldType.Keyword)
    private String brand;
    
    @Field(type = FieldType.Integer)
    private Integer stock;
    
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private Date createTime;
}
```

### 3.2 Repository 接口

```java
public interface GoodsRepository extends ElasticsearchRepository<Goods, Long> {
    
    // 方法命名自动生成查询
    List<Goods> findByTitle(String title);
    
    List<Goods> findByTitleContaining(String keyword);
    
    List<Goods> findByCategoryAndBrand(String category, String brand);
    
    List<Goods> findByPriceBetween(BigDecimal min, BigDecimal max);
    
    Page<Goods> findByTitleContaining(String title, Pageable pageable);
    
    // 自定义 DSL
    @Query("{\"bool\":{\"must\":[{\"match\":{\"title\":\"?0\"}}],\"filter\":[{\"range\":{\"price\":{\"gte\":?1,\"lte\":?2}}}]}}")
    List<Goods> searchByTitleAndPrice(String title, BigDecimal minPrice, BigDecimal maxPrice);
}
```

### 3.3 Service 使用

```java
@Service
@RequiredArgsConstructor
public class GoodsService {
    
    private final GoodsRepository goodsRepository;
    
    // 保存/更新
    public Goods save(Goods goods) {
        return goodsRepository.save(goods);
    }
    
    // 批量保存
    public void batchSave(List<Goods> goodsList) {
        goodsRepository.saveAll(goodsList);
    }
    
    // 根据 ID 查
    public Goods findById(Long id) {
        return goodsRepository.findById(id).orElse(null);
    }
    
    // 删除
    public void delete(Long id) {
        goodsRepository.deleteById(id);
    }
    
    // 分页搜索
    public Page<Goods> search(String keyword, int page, int size) {
        return goodsRepository.findByTitleContaining(
            keyword, 
            PageRequest.of(page, size)
        );
    }
}
```

---

## 四、方式二：ElasticsearchRestTemplate（灵活中间层）

### 4.1 保存与查询

```java
@Service
@RequiredArgsConstructor
public class GoodsSearchService {
    
    private final ElasticsearchRestTemplate restTemplate;
    
    // 索引文档
    public Goods save(Goods goods) {
        return restTemplate.save(goods);
    }
    
    // 批量索引
    public void batchSave(List<Goods> goodsList) {
        List<IndexQuery> queries = goodsList.stream()
            .map(g -> new IndexQueryBuilder().withObject(g).build())
            .collect(Collectors.toList());
        restTemplate.bulkIndex(queries, Goods.class);
    }
    
    // ID 查询
    public Goods findById(Long id) {
        return restTemplate.get(String.valueOf(id), Goods.class);
    }
}
```

### 4.2 使用 CriteriaQuery 查询

```java
public List<Goods> searchByCriteria(String keyword, BigDecimal minPrice, BigDecimal maxPrice) {
    
    Criteria criteria = new Criteria("title").matches(keyword)
        .and(new Criteria("price").greaterThanEqual(minPrice))
        .and(new Criteria("price").lessThanEqual(maxPrice));
    
    CriteriaQuery query = new CriteriaQuery(criteria);
    query.setPageable(PageRequest.of(0, 10));
    query.addSort(Sort.by(Sort.Direction.DESC, "createTime"));
    
    SearchHits<Goods> hits = restTemplate.search(query, Goods.class);
    
    return hits.getSearchHits().stream()
        .map(SearchHit::getContent)
        .collect(Collectors.toList());
}
```

---

## 五、方式三：NativeSearchQuery（原生 DSL — 企业主力）

### 5.1 构建 DSL 查询

```java
@Service
@RequiredArgsConstructor
public class GoodsNativeSearchService {
    
    private final ElasticsearchRestTemplate restTemplate;
    
    public List<Goods> searchNative(String keyword, String brand, BigDecimal minPrice, BigDecimal maxPrice) {
        
        NativeSearchQuery query = new NativeSearchQueryBuilder()
            .withQuery(
                QueryBuilders.boolQuery()
                    .must(QueryBuilders.matchQuery("title", keyword))          // 全文匹配
                    .filter(QueryBuilders.termQuery("brand.keyword", brand))    // 精确过滤
                    .filter(QueryBuilders.rangeQuery("price")                   // 范围过滤
                        .gte(minPrice)
                        .lte(maxPrice))
            )
            .withSorts(
                SortBuilders.fieldSort("createTime").order(SortOrder.DESC),
                SortBuilders.scoreSort().order(SortOrder.DESC)
            )
            .withPageable(PageRequest.of(0, 20))
            .build();
        
        SearchHits<Goods> hits = restTemplate.search(query, Goods.class);
        
        return hits.getSearchHits().stream()
            .map(SearchHit::getContent)
            .collect(Collectors.toList());
    }
}
```

### 5.2 高亮结果处理

```java
public List<GoodsVO> searchWithHighlight(String keyword) {
    
    NativeSearchQuery query = new NativeSearchQueryBuilder()
        .withQuery(QueryBuilders.matchQuery("title", keyword))
        .withHighlightFields(
            new HighlightBuilder.Field("title")
                .preTags("<em>")
                .postTags("</em>")
                .fragmentSize(60)
                .numOfFragments(1)
        )
        .build();
    
    SearchHits<Goods> hits = restTemplate.search(query, Goods.class);
    
    return hits.getSearchHits().stream().map(hit -> {
        GoodsVO vo = BeanUtil.copyProperties(hit.getContent(), GoodsVO.class);
        // 取高亮后的标题
        List<String> highlightTitle = hit.getHighlightField("title");
        if (CollUtil.isNotEmpty(highlightTitle)) {
            vo.setTitleHighlight(highlightTitle.get(0));
        }
        return vo;
    }).collect(Collectors.toList());
}
```

### 5.3 聚合查询

```java
public Map<String, Long> brandAggregation() {
    
    NativeSearchQuery query = new NativeSearchQueryBuilder()
        .withQuery(QueryBuilders.matchAllQuery())
        .withAggregations(
            AggregationBuilders.terms("brand_agg")
                .field("brand.keyword")
                .size(20)
        )
        .build();
    
    SearchHits<Goods> result = restTemplate.search(query, Goods.class);
    
    Aggregations aggregations = (Aggregations) result.getAggregations().aggregations();
    ParsedStringTerms brandAgg = aggregations.get("brand_agg");
    
    Map<String, Long> brandCounts = new LinkedHashMap<>();
    for (Terms.Bucket bucket : brandAgg.getBuckets()) {
        brandCounts.put(bucket.getKeyAsString(), bucket.getDocCount());
    }
    return brandCounts;
}
```

---

## 六、批量操作优化（Bulk）

```java
@Service
@RequiredArgsConstructor
public class BulkService {
    
    private final ElasticsearchRestTemplate restTemplate;
    
    public void bulkInsert(List<Goods> goodsList) {
        
        List<IndexQuery> queries = goodsList.stream()
            .map(g -> new IndexQueryBuilder()
                .withId(String.valueOf(g.getId()))
                .withObject(g)
                .build())
            .collect(Collectors.toList());
        
        // 分批，每批 1000 条
        ListUtil.split(queries, 1000).forEach(batch -> {
            List<String> failedIds = restTemplate.bulkIndex(batch, Goods.class);
            if (CollUtil.isNotEmpty(failedIds)) {
                log.warn("Bulk 部分失败: {}", failedIds);
            }
        });
    }
}
```

**批量写入性能建议**：
- 每批 500-1000 条为佳
- 写入期间调大 `refresh_interval`（如 30s）
- 关闭副本 `number_of_replicas=0`，写完再开启

---

## 七、新旧客户端对比

| 版本 | 客户端 | 说明 |
|---|---|---|
| ES 7.x | `RestHighLevelClient` | 主流使用，需单独依赖 |
| ES 8.x | `ElasticsearchClient` | 新版 Java API Client，Lambda 风格 |
| SpringBoot 2.x | `ElasticsearchRestTemplate` | 封装了 High Level Client |
| SpringBoot 3.x | `ElasticsearchOperations` | 适配新版 Client |

### ES 8.x 新版 API 风格

```java
// 8.x 新版 Client 风格
ElasticsearchClient client = ...;

SearchResponse<Goods> response = client.search(s -> s
    .index("goods")
    .query(q -> q
        .bool(b -> b
            .must(m -> m.match(t -> t.field("title").query("手机")))
            .filter(f -> f.range(r -> r.field("price").gte("1000").lte("5000")))
        )
    ),
    Goods.class
);
```

---

## 八、面试核心要点

1. **Repository vs RestTemplate？** Repository 简单 CRUD，RestTemplate 灵活性高适合复杂查询
2. **Bulk 性能优化？** 分批（500-1000条），调 refresh_interval，先关副本
3. **高亮怎么做？** HighlightBuilder 配合 SearchHit.getHighlightField()
4. **ES 8.x 客户端变化？** 从 RestHighLevelClient → ElasticsearchClient（Lambda 风格）

---

## 九、极简总结

```
Repository = 简单增删改查，命名方法自动查询
RestTemplate + NativeSearchQuery = 复杂 DSL，企业主力
Bulk 批量入库 = 分 500 条一批，关 refresh，写完再开
高亮 = HighlightBuilder + getHighlightField()
```
