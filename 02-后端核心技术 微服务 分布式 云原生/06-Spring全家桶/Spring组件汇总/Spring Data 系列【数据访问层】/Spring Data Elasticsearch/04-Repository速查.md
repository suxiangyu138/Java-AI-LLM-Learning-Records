# 04 Repository 速查

> ElasticsearchRepository 声明式查询：派生方法关键词全表、@Query JSON、@SearchTemplateQuery、分页排序、返回类型、自定义实现——"方法名即 DSL"

---

## 📚 目录

1. [Repository 接口定义](#1-repository-接口定义)
2. [派生查询关键词全表（24 个）](#2-派生查询关键词全表24-个)
3. [@Query 自定义查询](#3-query-自定义查询)
4. [@SearchTemplateQuery 搜索模板](#4-searchtemplatequery-搜索模板)
5. [分页与排序](#5-分页与排序)
6. [返回类型速查](#6-返回类型速查)
7. [自定义实现与多 Repository 组装](#7-自定义实现与多-repository-组装)
8. [响应式 Repository](#8-响应式-repository)

---

## 1. Repository 接口定义

```java
public interface ProductRepository extends ElasticsearchRepository<Product, String> {
    // 无需任何实现，方法名即查询
    List<Product> findByName(String name);
    Page<Product> findByCategoryAndPriceBetween(String category, double lo, double hi, Pageable pageable);
}

// 启动时：Boot 自动扫描 → ElasticsearchRepositoryFactoryBean 生成代理实现
// 等价 XML/注解控制：
// @EnableElasticsearchRepositories(basePackages = "com.demo.es.repo")  // 非 Boot 场景
```

| 要点 | 说明 |
|------|------|
| 泛型 | `<实体类, ID类型>` |
| 继承层级 | `ElasticsearchRepository<T, ID>` ← `CrudRepository` ← `PagingAndSortingRepository` ← `Repository` |
| 自动扫描 | Boot 下接口在启动类同包/子包即被发现 |
| 自定义片段 | 见第 7 节（老接口 + 片段拼接） |
| 方法命名冲突 | 两个方法解析出同一查询会启动报错（命名不唯一） |

> 🎯 **心智模型**：Repository 方法名 → 解析器按"主题词 + 谓词关键词"翻译成 ES 查询——`findByCategoryAndPriceBetween` 拆成 `bool.must[term(category), range(price, from, to)]`。**查不到结果先看方法名**，拼写错误（如 `Contain` 非 `Containing`）直接解析失败。

### 1.1 内置 CRUD 方法速查

| 方法 | 语义 | 说明 |
|------|------|------|
| `save(entity)` / `saveAll(iterable)` | 覆盖式写入 | 同 ID 覆盖（删除+重建） |
| `findById(id)` | 按 ID 查 | Optional 返回 |
| `existsById(id)` | 是否存在 | — |
| `findAll()` | 全量 | 慎用大索引 |
| `findAll(pageable)` | 分页全量 | — |
| `count()` | 文档数 | — |
| `deleteById(id)` / `delete(entity)` / `deleteAll()` | 删除 | 不可恢复 |

> ⚠️ **save 的"更新"语义**：ES 无真正更新——save 同 ID 文档 = 整体覆盖。**部分字段更新必须用** `ElasticsearchOperations.update()`（见 [05 篇](05-查询构造与Operations速查.md)），Repository 的 save 会把缺省字段清空。

## 2. 派生查询关键词全表（24 个）

| 关键词 | 示例方法 | 生成的查询 |
|--------|---------|-----------|
| `And` | `findByNameAndPrice` | `bool.must`（两个条件） |
| `Or` | `findByNameOrPrice` | `bool.should` |
| `Is` | `findByName` | `bool.must` + `query_string` |
| `Not` | `findByNameNot` | `bool.must_not` |
| `Between` | `findByPriceBetween` | `range` from/to 均包含 |
| `LessThan` | `findByPriceLessThan` | `range` 上界开区间 |
| `LessThanEqual` | `findByPriceLessThanEqual` | `range` 上界闭区间 |
| `GreaterThan` | `findByPriceGreaterThan` | `range` 下界开区间 |
| `GreaterThanEqual` | `findByPriceGreaterThanEqual` | `range` 下界闭区间 |
| `Before` | `findByPriceBefore` | `range` 到（含） |
| `After` | `findByPriceAfter` | `range` 从（含） |
| `Like` | `findByNameLike` | `query_string` 通配 `?*` |
| `StartingWith` | `findByNameStartingWith` | `query_string` 前缀 |
| `EndingWith` | `findByNameEndingWith` | `query_string` 后缀 |
| `Contains` / `Containing` | `findByNameContaining` | `query_string` 包含 `*?*` |
| `In` | `findByNameIn(Collection)` | Keyword 字段 → `terms`；否则 `query_string` |
| `NotIn` | `findByNameNotIn(Collection)` | `bool.must_not` + terms / query_string |
| `True` | `findByAvailableTrue` | `query_string` true |
| `False` | `findByAvailableFalse` | `query_string` false |
| `OrderBy` | `findByAvailableTrueOrderByNameDesc` | 查询 + sort |
| `Exists` | `findByNameExists` | `exists` 查询 |
| `IsNull` | `findByNameIsNull` | `bool.must_not` + exists |
| `IsNotNull` | `findByNameIsNotNull` | `bool.must` + exists |
| `IsEmpty` | `findByNameIsEmpty` | exists + must_not 通配 `*` |
| `IsNotEmpty` | `findByNameIsNotEmpty` | `wildcard` |

> ⚠️ **派生查询的两个限制**：① **`query_string` 语义**——`Containing` 等生成的查询走 query_string 解析，特殊字符（`+ - && || ! ( )` 等）会被当语法处理，搜索框输入必须转义；② **GeoJson 等复杂参数无法派生**——地理查询必须用 `ElasticsearchOperations` + CriteriaQuery 或 NativeQuery（见 05 篇）。

### 2.1 派生查询的选择要点

| 场景 | 用派生方法？ | 建议 |
|------|:---:|------|
| 固定条件组合（and/or/range） | ✅ | 首选，声明式最清晰 |
| 全文搜索框（用户自由输入） | ⚠️ | 用户输入建议 match 而非 query_string 派生 |
| 高亮/聚合/向量 | ❌ | 必须 @Query 或 Operations |
| 动态组合条件 | ❌ | CriteriaQuery 拼接（05 篇） |
| 分页深度 > 10000 | ❌ | search_after（06 篇） |

> 💡 经验法则：**静态查询用派生方法，动态/复杂查询走 Operations**——两者共存互补，不是替代关系；大项目通常"Repository 放派生方法，Service 里用 Operations 做复杂场景"。

## 3. @Query 自定义查询

### 3.1 位置占位符（?0、?1）

```java
public interface BookRepository extends ElasticsearchRepository<Book, String> {
    @Query("{\"match\": {\"name\": {\"query\": \"?0\"}}}")
    Page<Book> findByName(String name, Pageable pageable);

    // 集合参数直接透传
    @Query("{\"ids\": {\"values\": ?0 }}")
    List<Book> findByIds(Collection<String> ids);
}
```

| 特性 | 写法 | 说明 |
|------|------|------|
| 位置占位 | `?0`、`?1` | 按方法参数顺序 |
| 集合参数 | 直接作为 JSON 数组 | 无需手拼引号 |
| SpEL 参数 | `#{#name}` | 文本块 + 命名引用 |
| SpEL 属性 | `#{#parameter.value}` | record/对象属性访问 |
| SpEL Bean | `#{@queryParameter.value}` | 引用容器 Bean（无需入参） |
| 集合投影 | `#{#parameters.![value]}` | 非 String 集合元素转换 |
| @Param 重命名 | `findByName(@Param("another") ...)` | 配合 `#{#another.![value]}` |

### 3.2 SpEL 完整示例（6.x 文本块写法）

```java
public interface BookRepository extends ElasticsearchRepository<Book, String> {
    @Query("""
        {
          "bool": {
            "must": [
              { "term": { "name": "#{#name}" } },
              { "terms": { "category": #{#categories} } }
            ]
          }
        }
        """)
    Page<Book> findByNameAndCategories(String name, List<String> categories, Pageable pageable);

    @Query("""
        {
          "match": {
            "title": {
              "query": "#{#parameter.value}",
              "fuzziness": "AUTO"
            }
          }
        }
        """)
    SearchHits<Book> fuzzySearch(QueryParameter parameter);
}
```

> 💡 **SpEL 优势**：命名参数 + 文本块让 JSON 可读性大幅提升，集合参数不用手写引号拼接——新代码优先 SpEL 写法；`?0` 风格适合简单单参数。

### 3.3 @Query 的属性速查

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `value` / `query` | 空 | 查询 JSON（query 元素） |
| `count` | false | true 时按 count 投影执行（返回 long） |

> ⚠️ **@Query 的 JSON 边界**：值是请求体的 **query 元素**（不含 `{ "query": ... }` 外层包装，`count` 为 true 时除外）；写错结构（把整个 body 贴进来）会报 `parsing_exception`。

## 4. @SearchTemplateQuery 搜索模板

```java
public interface BookRepository extends ElasticsearchRepository<Book, String> {
    // 使用 ES 侧已注册的模板（模板用 _script API 注册）
    @SearchTemplateQuery(id = "book-by-title")
    SearchHits<Book> findByTitle(String title);
}
```

| 要点 | 说明 |
|------|------|
| 模板注册 | ES 侧 `_scripts/{id}` 或 Kibana "开发工具" 注册 |
| 参数传递 | 方法参数按名称/位置映射到模板变量 |
| 优势 | 查询改动不重新发版（改模板即可） |
| 代价 | 模板调试链路长，版本管理复杂 |

> 💡 什么时候用搜索模板？**查询由非开发人员（运营/分析师）频繁调整**时——把查询下沉到 ES 侧模板，应用只传参数；常规 CRUD 项目不值得引入这层复杂度。

## 5. 分页与排序

```java
// 排序
Sort sort = Sort.by(Sort.Direction.DESC, "price");
// 或 Sort.by("price").descending().and(Sort.by("stock"));

// 分页（第 1 页，每页 10，价格降序）
Pageable pageable = PageRequest.of(0, 10, sort);

Page<Product> page = productRepository.findByCategoryAndPriceBetween("手机", 1000, 5000, pageable);
page.getTotalElements();   // 总命中数
page.getContent();         // 本页数据
```

| 场景 | 方案 | 说明 |
|------|------|------|
| 常规分页 | `Pageable` + `Page` | from+size 实现，**默认上限 10000 条** |
| 深分页（>10000） | search_after / PIT | 见 [06 篇](06-聚合滚动与批量速查.md) |
| 纯排序 | `Sort` 参数 | 注意 Text 字段不可排序 |
| 索引级排序 | `@Setting(sortFields=...)` | 与 search_after 配套 |

> ⚠️ **深分页禁令**：from 超过 `index.max_result_window`（默认 10000）直接报错——不是分页 bug，是设计如此（from+size 深分页代价指数级）。翻页超过 10000 的业务（管理后台导出、搜索流）必须换 search_after。

## 6. 返回类型速查

```java
List<Product> list;                     // 默认返回类型
Stream<Product> stream;                 // 流式（慎用：保持连接直到消费完）
SearchHits<Product> hits;               // ★ 命中封装（含分数/高亮）
List<SearchHit<Product>> hitList;       // 逐个命中
Stream<SearchHit<Product>> hitStream;
SearchPage<Product> page;               // 分页 + SearchHits
long count;                             // count 投影（@Query count=true）
```

| 返回类型 | 拿到什么 | 何时用 |
|---------|---------|--------|
| `List<T>` / `Page<T>` | 实体列表 | 常规业务 |
| `SearchHits<T>` | 分数、高亮、命中明细 | **高亮查询必须**（否则高亮不注入） |
| `List<SearchHit<T>>` | 每个命中的元信息 | 需要逐条 score/highlight |
| `Stream<T>` | 惰性流 | 大数据量只读遍历（注意资源释放） |

> ⚠️ **高亮头号坑**：高亮查询返回类型必须是 `SearchHits`/`SearchPage` 系列——返回 `List<T>` 时高亮字段**静默丢失**（框架不会报错，只是不注入）。

## 7. 自定义实现与多 Repository 组装

```java
// ① 接口：继承标准 + 声明自定义方法
public interface ProductRepository extends ElasticsearchRepository<Product, String>, ProductSearchCustom {
    Page<Product> findByCategory(String category, Pageable pageable);
}

// ② 自定义片段接口
public interface ProductSearchCustom {
    SearchHits<Product> fullTextSearch(String keyword, int page, int size);
}

// ③ 实现类（命名必须：接口名 + Impl）
public class ProductRepositoryImpl implements ProductSearchCustom {
    private final ElasticsearchOperations operations;   // 注入模板

    @Override
    public SearchHits<Product> fullTextSearch(String keyword, int page, int size) {
        NativeQuery query = NativeQuery.builder()
            .withQuery(q -> q.multiMatch(mm -> mm
                .fields("name^2", "description")       // name 权重翻倍
                .query(keyword)))
            .withPageable(PageRequest.of(page, size))
            .build();
        return operations.search(query, Product.class, IndexCoordinates.of("product"));
    }
}

// ④ 组装：Spring Data 自动把两个片段拼进同一个 Bean（ProductRepository）
```

| 要点 | 说明 |
|------|------|
| 命名约定 | 实现类 = 主接口名 + `Impl`（否则不识别） |
| 注入 | 实现类可注入任何 Bean（含 ElasticsearchOperations） |
| 组合 | 标准方法 + 自定义片段共存于同一代理 |
| 适用 | 派生方法表达不了的复杂查询/聚合/向量检索 |

> 🎯 **大项目分工范式**：Repository 接口 = "静态查询声明区"（派生 + @Query），Impl 片段 = "复杂查询收容区"（NativeQuery 聚合/kNN）——一个 Bean 两个面，Service 只面对单一接口。

## 8. 响应式 Repository

```java
public interface ReactiveProductRepository extends ReactiveElasticsearchRepository<Product, String> {
    Flux<Product> findByNameContaining(String keyword);
    Mono<Page<Product>> findByCategory(String category, Pageable pageable);
}

// 使用
reactiveProductRepository.findByNameContaining("手机")
    .map(Product::getName)
    .take(10)
    .subscribe(System.out::println);
```

| 对比项 | 命令式 | 响应式 |
|--------|--------|--------|
| 接口 | `ElasticsearchRepository` | `ReactiveElasticsearchRepository` |
| 返回 | `List/Page/Optional` | `Flux/Mono` |
| 线程 | 阻塞调用线程 | 事件循环（不阻塞） |
| 适用 | 常规 Web 服务 | 网关、高并发聚合服务 |

> ⚠️ **响应式全链路**：响应式 Repository 的收益只有在**整条链路（Controller→Service→Repo）全响应式**时才兑现——混用阻塞中间层等于白上反应式，还多背复杂度。WebFlux 项目才考虑。

---

**下一模块**：[05-查询构造与 Operations 速查](05-查询构造与Operations速查.md)　**返回总览**：[00-Spring Data Elasticsearch组件总览](00-Spring Data Elasticsearch组件总览.md)

**【参考来源】**：[Query methods 章节（官方 6.1.0）](https://docs.spring.io/spring-data/elasticsearch/reference/elasticsearch/repositories/elasticsearch-repository-queries.html)、[Repositories 定义与查询方法（Spring Data Commons 文档）](https://docs.spring.io/spring-data/elasticsearch/reference/repositories/query-methods-details.html)、[@Query Javadoc](https://docs.spring.io/spring-data/elasticsearch/reference/api/java/org/springframework/data/elasticsearch/annotations/Query.html)、[ES 查询 DSL 详解（深度：04-ELK-04）](../../../../04-ELK/Elasticsearch/04-ES查询DSL详解.md)
