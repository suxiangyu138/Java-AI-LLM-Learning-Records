# 05 查询构造与 Operations 速查

> ElasticsearchOperations 编程式查询：CriteriaQuery / StringQuery / NativeQuery 三类查询对象、高亮、排序过滤、文档级操作、查询执行链路——"动态查询的主战场"

---

## 📚 目录

1. [三类查询对象选型](#1-三类查询对象选型)
2. [CriteriaQuery：链式条件查询](#2-criteriaquery链式条件查询)
3. [NativeQuery：全能力查询（主力）](#3-nativequery全能力查询主力)
4. [查询公共参数速查（BaseQuery）](#4-查询公共参数速查basequery)
5. [高亮查询速查](#5-高亮查询速查)
6. [文档级操作（DocumentOperations）](#6-文档级操作documentoperations)
7. [搜索操作（SearchOperations）](#7-搜索操作searchoperations)
8. [查询执行链路与调优](#8-查询执行链路与调优)

---

## 1. 三类查询对象选型

| 维度 | CriteriaQuery | StringQuery | NativeQuery |
|------|--------------|-------------|-------------|
| 表达方式 | Java 链式（类型安全） | JSON 字符串 | ELC builder（类型安全） |
| 能力覆盖 | 常规条件（match/range/exists） | 全部 DSL（不校验语法） | **全部 DSL**（聚合/kNN/高亮/建议器） |
| 动态拼接 | ✅ 适合 | ❌ 拼字符串易错 | ✅ 适合 |
| 类型检查 | ✅ 编译期 | ❌ 运行期 | ✅ 编译期 |
| 使用占比 | 简单场景 | 尽量不用 | **复杂场景主力** |
| 性能 | 无差异（最终都是 JSON） | 无差异 | 无差异 |

> 🎯 **选型一句话**："简单用 Criteria，复杂用 Native，String 只有偷懒一个理由"——生产代码里 StringQuery 出现得越少越好。

## 2. CriteriaQuery：链式条件查询

```java
// 动态拼接：根据入参条件组合查询
Criteria criteria = new Criteria();
boolean first = true;
if (StringUtils.hasText(category)) {
    criteria = first ? Criteria.where("category").is(category)
                     : criteria.and("category").is(category);
    first = false;
}
if (priceLow != null || priceHigh != null) {
    criteria = first ? Criteria.where("price").between(priceLow, priceHigh)
                     : criteria.and("price").between(priceLow, priceHigh);
    first = false;
}

CriteriaQuery query = new CriteriaQuery(criteria)
    .addSort(Sort.by(Sort.Direction.DESC, "price"))
    .setPageable(PageRequest.of(0, 10));

SearchHits<Product> hits = operations.search(query, Product.class, IndexCoordinates.of("product"));
```

### 2.1 Criteria 方法速查

| 方法 | 语义 | 对应 DSL |
|------|------|---------|
| `where("field").is(value)` | 精确匹配 | term / query_string |
| `where("field").in(list)` | 集合匹配 | terms |
| `where("field").contains("xx")` | 包含 | query_string `*xx*` |
| `where("field").startsWith("xx")` | 前缀 | query_string `xx*` |
| `where("field").endsWith("xx")` | 后缀 | query_string `*xx` |
| `where("field").between(a, b)` | 范围（含边界） | range |
| `where("field").greaterThan(v)` / `greaterThanEqual(v)` | 大于 | range |
| `where("field").lessThan(v)` / `lessThanEqual(v)` | 小于 | range |
| `where("field").exists(true/false)` | 存在性 | exists / must_not exists |
| `where("field").isNotEmpty()` | 非空 | wildcard |
| `where("field").matches("term")` | match 语义 | match |
| `criteria.and(...)` / `criteria.or(...)` | 条件组合 | bool must / should |

> ⚠️ **Criteria 的坑**：① `is()` 对字符串生成 query_string（特殊字符需转义）；② **Nested 对象与 GeoJson 不能派生**——嵌套查询/地理查询必须 NativeQuery；③ `between` 双边界都包含（与 [04 篇](04-Repository速查.md) 派生 Between 一致）。

## 3. NativeQuery：全能力查询（主力）

```java
import static co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.*;  // 可选静态导入

NativeQuery query = NativeQuery.builder()
    // ① 主查询：bool 组合（must/should/must_not/filter）
    .withQuery(q -> q.bool(b -> b
        .must(m -> m.match(t -> t.field("name").query("手机")))
        .must(m -> m.range(r -> r.field("price")
            .gte(JsonData.of(1000)).lte(JsonData.of(5000))))
        .filter(f -> f.term(t -> t.field("status").value("on_sale")))))
    // ② 聚合
    .withAggregation("by_category", a -> a.terms(t -> t.field("category.keyword").size(10)))
    // ③ 排序 + 分页
    .withSort(s -> s.field(f -> f.field("price").order(SortOrder.Desc)))
    .withPageable(PageRequest.of(0, 10))
    .build();

SearchHits<Product> hits = operations.search(query, Product.class, IndexCoordinates.of("product"));
```

### 3.1 NativeQueryBuilder 方法速查（6.x）

| 方法 | 用途 |
|------|------|
| `withQuery(Query)` / `withQuery(Function)` | 主查询（ELC Query 或 builder lambda） |
| `withFilter(Query)` | filter 子句（不影响评分，缓存友好） |
| `withAggregation(name, Aggregation)` | 聚合 |
| `withKnnSearches(KnnSearch)` / `withKnnSearches(List)` | kNN 向量检索（5.3.1+，见 07 篇） |
| `withSuggester(Suggester)` | 搜索建议器（拼写纠错） |
| `withFieldCollapse(FieldCollapse)` | 字段折叠（去重） |
| `withSort(SortOptions...)` | 排序 |
| `withHighlightQuery(HighlightQuery)` | 高亮 |
| `withSearchExtension(key, JsonData)` | 搜索扩展参数 |
| `withPageable(Pageable)` / `withSourceFilter(...)` | 分页 / 源过滤 |
| `withSearchAfter(List)` / `withScrollTime(Duration)` | 深分页 / 滚动 |
| `withMinScore(float)` / `withTrackTotalHits(...)` | 评分过滤 / 命中数统计控制 |
| `withPreference(String)` | 分片偏好（粘性路由） |
| `withRoute(String)` | 路由键 |
| `withRuntimeFields(List<RuntimeField>)` | 运行时字段 |

> 🎯 **为什么 NativeQuery 是主力**：它直接透传 ELC 的 Query 模型——SDE 不必为每个新 DSL 特性"补课"，ES 新增查询能力（如 kNN、hybrid、新布尔组合）当天就能用。**"薄封装、全能力"是 4.4 起的设计取向**。

### 3.2 常用 ELC 查询 lambda 速查

| 查询 | lambda 写法（q -> ...） | 用途 |
|------|------------------------|------|
| term | `q.term(t -> t.field("status").value("on"))` | 精确值 |
| terms | `q.terms(t -> t.field("ids").terms(v -> v.value(List.of(...))))` | 多值 |
| match | `q.match(m -> m.field("name").query("手机"))` | 全文匹配（分词） |
| multi_match | `q.multiMatch(mm -> mm.fields("name^2", "desc").query(kw))` | 多字段加权搜索 |
| range | `q.range(r -> r.field("price").gte(JsonData.of(100)).lte(JsonData.of(999)))` | 范围 |
| bool | `q.bool(b -> b.must(...).should(...).filter(...))` | 组合（查询主力） |
| wildcard | `q.wildcard(w -> w.field("code").value("ab*"))` | 通配（慢，慎用） |
| exists | `q.exists(e -> e.field("brand"))` | 存在性 |
| nested | `q.nested(n -> n.path("specs").query(nq -> nq.bool(...)))` | 嵌套对象查询 |
| fuzzy | `q.fuzzy(f -> f.field("name").value("手机").fuzziness("AUTO"))` | 模糊匹配 |
| match_phrase | `q.matchPhrase(mp -> mp.field("name").query("智能手机"))` | 短语匹配 |

## 4. 查询公共参数速查（BaseQuery）

| 参数 | 方法 | 说明 |
|------|------|------|
| 分页 | `withPageable(Pageable)` | from/size |
| 排序 | `withSort(SortOptions...)` / `withSort(Sort)` | 支持多字段、`_score` |
| 高亮 | `withHighlightQuery(HighlightQuery)` | 见第 5 节 |
| 源过滤 | `withSourceFilter(SourceFilter)` | 减少传输（大文档场景） |
| 字段选择 | `withFields(...)` / `withStoredFields(...)` | 只取部分字段 |
| 深分页 | `withSearchAfter(List)` / `withPointInTime(PIT)` / `withScrollTime(...)` | 见 06 篇 |
| 最小分数 | `withMinScore(float)` | 过滤低相关结果 |
| 命中统计 | `withTrackTotalHits(true)` / `withTrackTotalHitsUpTo(n)` | 大结果集性能优化 |
| 超时 | `withTimeout(Duration)` | 查询超时 |
| 路由 | `withRoute(String)` | 指定分片（routing 场景） |
| 偏好 | `withPreference("_local")` | 就近读取 |
| 运行时字段 | `withRuntimeFields(...)` | 查询时计算字段 |
| 索引膨胀 | `withIndicesBoost(...)` | 多索引加权 |
| 解释 | `withExplain(true)` | 评分明细（排错用） |
| 请求缓存 | `withRequestCache(true)` | 命中请求缓存 |
| 多 ID 获取 | `withIds(...)` | multi-get |

```java
// 源过滤 + 最小分数 + 命中数截断的典型组合（列表页性能优化）
NativeQuery query = NativeQuery.builder()
    .withQuery(q -> q.match(m -> m.field("name").query(kw)))
    .withSourceFilter(new FetchSourceFilterBuilder()
        .withIncludes("id", "name", "price").build())      // 只传必要字段
    .withMinScore(0.5f)
    .withTrackTotalHitsUpTo(1000)                          // 总数统计到 1000 为止
    .withPageable(PageRequest.of(0, 20))
    .build();
```

> 💡 **列表接口三件套**：`SourceFilter`（减传输）+ `TrackTotalHitsUpTo`（减统计开销）+ 合理 `Pageable`（减结果量）——三个参数一次配齐，搜索列表页 QPS 明显提升。

## 5. 高亮查询速查

```java
// ① 定义高亮参数
HighlightField nameField = new HighlightField("name");
HighlightParameters params = HighlightParameters.builder()
    .withPreTags("<em class='hl'>")       // 高亮前缀
    .withPostTags("</em>")                // 高亮后缀
    .withFragmentSize(80)                 // 片段长度
    .withNumberOfFragments(2)             // 最多片段数
    .build();
HighlightQuery highlight = new HighlightQueryBuilder()
    .withFields(nameField)                // 也可以 builder 内指定字段参数
    .withParameters(params)
    .build();

// ② 组装查询（注意 withHighlightQuery 位置）
NativeQuery query = NativeQuery.builder()
    .withQuery(q -> q.match(m -> m.field("name").query(kw)))
    .withHighlightQuery(highlight)
    .build();

// ③ 返回类型必须是 SearchHits（否则高亮静默丢失）
SearchHits<Product> hits = operations.search(query, Product.class, IndexCoordinates.of("product"));
List<String> fragments = hits.getSearchHit(0).getHighlightField("name");  // 高亮片段
```

| 参数 | 说明 |
|------|------|
| `withPreTags` / `withPostTags` | 高亮标签（默认 `<em>`） |
| `withFragmentSize` | 片段字符数 |
| `withNumberOfFragments` | 片段数量（0 = 整字段返回） |
| `withPhraseLimit` | 短语匹配限制 |
| `getHighlightField(field)` | 从 SearchHit 取高亮片段 |

> ⚠️ **高亮三连坑**：① 返回类型必须是 `SearchHits`/`SearchPage`；② 高亮字段必须分词索引（Text），Keyword 字段高亮无意义；③ 搜索关键词与高亮关键词不一致时高亮不生效（ES 按命中的 term 高亮，不做二次匹配）。

## 6. 文档级操作（DocumentOperations）

```java
ElasticsearchOperations ops = ...;
IndexCoordinates index = IndexCoordinates.of("product");

// 写入
ops.save(product, index);                    // 覆盖式写入
ops.save(List.of(p1, p2), index);            // 批量
ops.bulkIndex(List.of(queries), index);      // 全量控制（IndexQuery 列表）

// 部分更新（区别于 save 的覆盖式）
UpdateQuery update = UpdateQuery.builder("docId")
    .withDoc(JsonData.of(Map.of("stock", 99)))   // 只改 stock 字段
    .withRetryOnConflict(3)                      // 乐观并发：冲突重试 3 次
    .build();
ops.update(update, index);

// 读取
Optional<Product> p = ops.get("docId", Product.class, index);
boolean exists = ops.exists("docId", index);
long count = ops.count(Query.findAll(), index);   // 索引内文档数

// 删除
String delete = ops.delete("docId", index);
String[] ids = ops.delete(List.of("id1", "id2"), index);
```

| 操作 | 语义 | 场景 |
|------|------|------|
| `save` | 整体覆盖（同 ID 删旧写新） | 全量文档写入 |
| `update`（UpdateQuery） | **字段级部分更新**（服务端 script/partial） | 计数器、状态流转、减少传输 |
| `bulkIndex` | 批量写入（IndexQuery 列表） | 大数据量导入 |
| `get` / `exists` | 单文档读取 | — |
| `count` | 文档数统计 | 看板 |

> 🎯 **save vs update 的选择**：字段多、全量覆盖 → `save`；只改一两个字段、高并发递增 → `update` + `withRetryOnConflict`（乐观并发，冲突重试）。**生产并发写场景几乎都用 update 而不是 save**——save 的覆盖写会丢并发更新。

## 7. 搜索操作（SearchOperations）

```java
SearchHits<Product> hits = operations.search(query, Product.class, index);          // 常规
SearchPage<Product> page = operations.searchForPage(query, Product.class, index);   // 分页封装
long total = operations.count(query, index);                                        // 计数
List<Aggregation> aggs = operations.aggregate(query, Product.class, index);         // 聚合（见 06 篇）
boolean exists = operations.exists(query, index);                                   // 是否存在命中

// 多索引搜索
SearchHits<Product> multi = operations.search(query, Product.class,
    IndexCoordinates.of("product_v1", "product_v2"));

// 滚动（深分页，见 06 篇）
SearchScrollHits<Product> scrollHits = operations.searchScrollStart(1, query, Product.class, index);
```

| 方法 | 返回 | 说明 |
|------|------|------|
| `search(query, clazz, index)` | `SearchHits<T>` | 最常用 |
| `searchForPage(...)` | `SearchPage<T>` | 分页元数据 + 总命中 |
| `aggregate(...)` | `List<Aggregation>` | 聚合结果（06 篇） |
| `count(...)` | `long` | 计数 |
| `exists(...)` | `boolean` | 有无命中 |
| `searchScrollStart` / `searchScrollContinue` / `searchScrollClear` | `SearchScrollHits<T>` | 滚动 API |
| `openPointInTime` / `closePointInTime` | PIT id | PIT 深分页 |

> ⚠️ **索引坐标（IndexCoordinates）**：4.0 起所有模板方法都必须显式传 `IndexCoordinates`（索引名）——不再从实体隐式推断；写多索引查询时用逗号拼接。漏传会直接编译不过（好事：显式化）。

## 8. 查询执行链路与调优

### 8.1 一次查询的执行链路（源码视角）

```text
业务代码 → ElasticsearchOperations.search(query, clazz, index)
  → ElasticsearchTemplate
    → QueryConverters.convertQuery(query)          # SDE 查询对象 → ELC Query
    → ElasticsearchClient.search(request)           # ELC 客户端
      → Rest5Client（HTTP/2、连接池）
        → POST /{index}/_search
        ← 命中 JSON
    ← convertSearchHits（ELC 响应 → SearchHits<T> + 实体映射）
```

| 环节 | 可优化点 |
|------|---------|
| 查询构造 | 复用预构建 Query 对象（减少 builder 开销，收益小） |
| 传输 | SourceFilter 减体量、gzip 压缩（见 02 篇回调） |
| 引擎 | mapping 分词选型、filter 缓存、索引结构（04-ELK 深度篇） |
| 结果映射 | 实体字段裁剪、避免超大 Nested 反序列化 |

### 8.2 查询性能调优速查

| 症状 | 首要检查 | 方向 |
|------|---------|------|
| 响应慢（引擎侧耗时高） | `_profile` API 看各阶段 | 慢查询定位：查询还是 fetch 阶段 |
| 传输慢（Java 侧耗时高） | 命中文档体积 | SourceFilter、字段裁剪 |
| 大量 `_score` 计算 | filter 子句用了吗 | 过滤条件放 `filter`（免评分 + 缓存） |
| 深分页慢 | 用 search_after 了吗 | 见 06 篇 |
| 高亮慢 | term_vector 开没开 | Text 字段高亮开 term_vector（`@Field(termVector=WITH_POSITIONS_OFFSETS)`） |

> 💡 **排障三板斧**：① 传输层日志开 trace 看真实 DSL（02 篇）；② 拿 DSL 去 Kibana `_profile` 分析阶段耗时；③ 对比"原生客户端直查 vs SDE 查询"耗时可定位是不是组件层开销——三斧下去 90% 的"ES 慢"问题都能定位到层。

---

**下一模块**：[06-聚合、滚动与批量速查](06-聚合滚动与批量速查.md)　**返回总览**：[00-Spring Data Elasticsearch组件总览](00-Spring Data Elasticsearch组件总览.md)

**【参考来源】**：[Elasticsearch Operations 章节（官方 6.1.0）](https://docs.spring.io/spring-data/elasticsearch/reference/elasticsearch/template.html)、[NativeQuery Javadoc（6.0）](https://docs.spring.io/spring-data/elasticsearch/reference/6.0/api/java/org/springframework/data/elasticsearch/client/elc/NativeQuery.html)、[BaseQueryBuilder Javadoc](https://docs.spring.io/spring-data/elasticsearch/reference/api/java/org/springframework/data/elasticsearch/core/query/BaseQueryBuilder.html)、[ES 查询 DSL 详解（深度：04-ELK-04）](../../../../04-ELK/Elasticsearch/04-ES查询DSL详解.md)
