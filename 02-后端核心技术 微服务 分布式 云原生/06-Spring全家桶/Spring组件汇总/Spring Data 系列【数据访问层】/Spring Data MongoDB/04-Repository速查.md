# 04 Repository 速查

> MongoRepository 接口层次、派生方法关键词全表（与 JPA 的差异）、@Query JSON、@Aggregation 管道、分页排序、@Modifying 更新——"写接口名 = 写查询"的完整手册

---

## 📚 目录

1. [Repository 接口层次](#1-repository-接口层次)
2. [派生方法关键词全表](#2-派生方法关键词全表)
3. [@Query JSON 自定义查询](#3-query-json-自定义查询)
4. [@Aggregation 聚合管道](#4-aggregation-聚合管道)
5. [@Modifying 与 upsert](#5-modifying-与-upsert)
6. [分页与排序](#6-分页与排序)
7. [执行链路与 4.x 行为变化](#7-执行链路与-4x-行为变化)

---

## 1. Repository 接口层次

```text
Repository<T, ID>
 ├── CrudRepository<T, ID>
 │    └── PagingAndSortingRepository<T, ID>
 │         └── MongoRepository<T, ID>     # ★ 实战主力（同步）
 └── ReactiveMongoRepository<T, ID>        # ★ 响应式主力（WebFlux）
```

| 方法 | 说明 | 注意 |
|------|------|------|
| `save(entity)` | 插入或更新（按 _id 判断） | 实体带 _id → upsert 语义 |
| `saveAll` | 批量保存 | 内部逐个 upsert（大批量用 [06 篇](06-批量操作与写关注速查.md) 批量） |
| `findById` | `Optional<T>` | 按 _id（String/ObjectId 均可） |
| `existsById` | 存在性 | — |
| `deleteById` / `deleteAll` | 删除 | 按 _id 或实体 |
| `findAll(Sort)` / `findAll(Pageable)` | 全量/分页 | 大集合慎用 findAll（无条件全扫） |

> 💡 默认实现 `SimpleMongoRepository`（[01 篇](01-模块清单.md) 运行时对象链）——读源码从它开始：save 的 upsert 逻辑、deleteById 语义、Pageable 翻译都在这。

### 1.1 与 JPA Repository 的差异

| 维度 | Spring Data JPA | Spring Data MongoDB |
|------|-----------------|---------------------|
| 实体注解 | @Entity/@Table | @Document |
| 默认实现 | SimpleJpaRepository | SimpleMongoRepository |
| save 语义 | persist/merge（先查后并） | **upsert**（_id 存在即更新，无先查） |
| 级联/懒加载 | 有（关联模型） | **无**（内嵌即加载，DBRef 需手动） |
| 事务默认 | 单方法自带事务 | 单文档原子（无需事务）；多文档需显式事务 |

> 🎯 面试必答：**"MongoRepository 的 save 和 JPA 的 save 区别？"**——JPA save 分离对象会"先 SELECT 再 MERGE"（N+1 来源之一）；Mongo 的 save 是**纯 upsert**（_id 存在 → $set 更新，不存在 → 插入），没有先查后写，天然高效——但也就没有 JPA 的级联与状态跟踪。

## 2. 派生方法关键词全表

### 2.1 前缀与返回值

| 前缀 | 语义 | 示例 |
|------|------|------|
| `find...By` / `get...By` / `read...By` / `query...By` | 查询 | `findByName` |
| `count...By` | 计数 | `countByStatus(String)` |
| `exists...By` | 存在性 | `existsBySku(String)` |
| `delete...By` / `remove...By` | 删除 | `deleteByStatus(String)` |
| `findFirst...By` / `findTop...By` | 限量 | `findTop10ByCategoryOrderByPriceDesc` |
| `findDistinct...By` | 去重 | `findDistinctByCategory` |

### 2.2 条件关键词（与 JPA 对照）

| 关键词 | 语义 | MongoDB 查询 | 与 JPA 差异 |
|--------|------|-------------|------------|
| `And` / `Or` | 与 / 或 | `{$and: ...}` / `{$or: ...}` | 相同 |
| `Between` | 区间 | `{f: {$gte: lo, $lte: hi}}` | 相同 |
| `LessThan` / `GreaterThan` 系列 | 比较 | `$lt` / `$gt` / `$lte` / `$gte` | 相同 |
| `After` / `Before` | 日期前后 | `$gt` / `$lt` | 相同 |
| `IsNull` / `IsNotNull` | 空判断 | `{f: null}` / `{f: {$ne: null}}` | ⚠️ Mongo 的 null 匹配"不存在或 null" |
| `Like` / `Containing` | 模糊 | `$regex` | ⚠️ 大小写敏感默认；正则转义注意 |
| `StartingWith` / `EndingWith` | 前缀/后缀 | `^xxx` / `xxx$` 正则 | 相同语义 |
| `In` / `NotIn` | 集合成员 | `$in` / `$nin` | 相同 |
| `True` / `False` | 布尔 | `{f: true}` | 相同 |
| `Exists` | 字段存在 | `{f: {$exists: true}}` | ✅ **MongoDB 独有** |
| `Regex` | 正则直写 | `$regex` | ✅ **MongoDB 独有**：`findByNameRegex("^A.*")` |
| `Size` | 数组长度 | `{f: {$size: 3}}` | ✅ **MongoDB 独有**：`findByTagsSize(3)` |
| `ElemMatch` | 数组元素匹配 | `$elemMatch` | ✅ **MongoDB 独有**：`findByItemsPriceGreaterThan` 也走数组语义 |
| `OrderByXxxDesc/Asc` | 排序 | `sort` | 相同 |
| `IgnoreCase` | 忽略大小写 | 正则 `i` 选项 | 相同语义（实现不同） |
| `Not` / `NotEqual` | 取反/不等 | `$ne` | 相同 |

```java
public interface ProductRepository extends MongoRepository<Product, String> {

    // MongoDB 特色派生方法
    List<Product> findByNameRegex(String pattern);            // {name: {$regex: ...}}
    List<Product> findByTagsSize(int size);                   // {tags: {$size: 3}}
    List<Product> findByDeletedAtExists(boolean exists);      // {deletedAt: {$exists: true}}
    List<Product> findByItemsPriceGreaterThan(BigDecimal min);// 数组内嵌元素匹配
    List<Product> findByNameContainingIgnoreCase(String kw);  // 大小写不敏感模糊
    Optional<Product> findFirstByCategoryOrderByPriceDesc(String category);
}
```

> 💡 派生方法适用边界（同 JPA）：单集合、属性路径清晰（占业务 60-70%）；复杂管道、聚合、动态条件 → @Aggregation / MongoTemplate（[05 篇](05-查询构造与模板速查.md)）。

## 3. @Query JSON 自定义查询

### 3.1 基础用法

```java
public interface ProductRepository extends MongoRepository<Product, String> {

    // 占位符：?0 位置 / :name 命名
    @Query("{'category': ?0, 'price': {$gte: ?1}}")
    List<Product> findCheapInCategory(String category, BigDecimal minPrice);

    @Query("{'status': :status}")
    List<Product> findByStatusNamed(@Param("status") String status);

    // 返回指定字段（投影）
    @Query(value = "{'category': ?0}", fields = "{'name': 1, 'price': 1}")
    List<Product> findProjected(String category);
}
```

| 要点 | 说明 |
|------|------|
| 语法 | **JSON 查询文档**（MongoDB 原生语法，与 shell 一致） |
| 参数 | `?0` 位置参数 / `:name` 命名参数（@Param 标注） |
| 投影 | `fields` 属性指定返回字段（1=包含 0=排除） |
| 排序 | `sort` 属性：`sort = "{'price': -1}"` |
| 校验 | 4.x 起查询 JSON 在启动/首次使用时校验（语法错启动报错） |

> ⚠️ 参数类型注意：日期参数要传 `java.util.Date` 或 java.time（JSON 里不能写 `new Date()`）；正则参数要传 `Pattern` 类型（`?0` 处）。

### 3.2 复杂查询示例

```java
// 数组/内嵌元素条件
@Query("{'items': {$elemMatch: {'sku': ?0, 'qty': {$gt: ?1}}}}")
List<OrderEntity> findByItemSkuAndQty(String sku, int qty);

// 文本搜索（需文本索引）
@Query("{'$text': {'$search': ?0}}")
List<Product> fullTextSearch(String keyword);

// 地理查询（附近的门店）
@Query("{'location': {$near: {$geometry: {type: 'Point', coordinates: [?0, ?1]}, $maxDistance: 5000}}}")
List<Store> findNearby(double lng, double lat);
```

## 4. @Aggregation 聚合管道

> **MongoDB 的杀手锏能力**——在数据库侧完成分组/统计/连接（$lookup），业务代码一行管道走天下。

```java
public interface OrderRepository extends MongoRepository<OrderEntity, String> {

    // 按状态分组统计（管道：match → group → sort → limit）
    @Aggregation(pipeline = {
        "{'$match': {'createdAt': {$gte: ?0}}}",
        "{'$group': {'_id': '$status', 'count': {$sum: 1}, 'totalAmount': {$sum: '$totalAmount'}}}",
        "{'$sort': {'count': -1}}"
    })
    List<StatusSummary> countByStatus(LocalDateTime since);

    // $lookup 连接（有限 join：order → user）
    @Aggregation(pipeline = {
        "{'$lookup': {'from': 'user_account', 'localField': 'userId', 'foreignField': '_id', 'as': 'user'}}",
        "{'$unwind': '$user'}",
        "{'$project': {'orderNo': 1, 'username': '$user.username'}}"
    })
    List<OrderWithUser> findOrdersWithUser();
}

// 投影接口（字段子集）
public interface StatusSummary { String getStatus(); long getCount(); BigDecimal getTotalAmount(); }
public interface OrderWithUser { String getOrderNo(); String getUsername(); }
```

| 管道阶段 | 作用 | 对应业务 |
|---------|------|---------|
| `$match` | 过滤（**放最前**，先缩数据量） | where |
| `$group` | 分组聚合（sum/count/avg） | group by |
| `$sort` / `$limit` / `$skip` | 排序/限量/跳过 | order by + 分页 |
| `$unwind` | 数组展开 | 行转多行 |
| `$lookup` | 集合连接（**有限 join**） | 左连接 |
| `$project` | 字段投影/计算 | select 子集 |
| `$addFields` / `$set` | 新增字段 | 计算列 |
| `$vectorSearch` | 向量检索（8.x，[08 篇](08-向量检索与AI集成速查.md)） | 语义搜索 |
| `$rankFusion` | RRF 混合检索（8.3，[08 篇](08-向量检索与AI集成速查.md)） | 全文+向量融合 |

> 🎯 面试必答：**"MongoDB 没有 join，怎么做关联查询？"**——三层答案：① 设计层：内嵌避免关联（推荐）；② 查询层：`$lookup` 管道实现有限 join（注意：大集合 $lookup 性能差，需被连接集合有索引）；③ 兜底层：应用侧分两次查（对延迟不敏感场景）。**加分句**：$lookup 是"数据库侧 join"，不是银弹——数据模型设计永远优先于查询技巧。

## 5. @Modifying 与 upsert

```java
public interface ProductRepository extends MongoRepository<Product, String> {

    @Modifying
    @Query("{'$set': {'price': ?1, 'updatedAt': ?2}}")
    void updatePrice(String id, BigDecimal newPrice, LocalDateTime now);
    // 语义：update({_id: ?0}, {$set: {...}})

    @Modifying
    @Query("{'$inc': {'stock': -?1}}")           // 原子自减（防超卖核心）
    void deductStock(String id, int qty);
}
```

| 要点 | 说明 |
|------|------|
| 语义 | update（匹配不到不插入）；upsert 需 `?upsert=true` 参数或模板操作 |
| 原子性 | 单文档更新天然原子（`$set`/`$inc` 原子）——**并发扣减用 $inc 而非读-改-写** |
| 返回 | void / long（匹配数） |
| 事务 | 单文档无需事务；多文档更新才需要（[06 篇](06-批量操作与写关注速查.md)） |

> ⚠️ **$inc 原子自减**是防超卖的核心手段（等价于 SQL 的 `UPDATE ... SET stock = stock - ? WHERE stock >= ?` 思路）：读-改-写三步并发必超卖，`$inc` 一步原子；"扣成负数"的校验再配合条件更新（`$inc` 前查 stock）或乐观锁兜底。

## 6. 分页与排序

```java
// 用法与 JPA 一致（Pageable/Sort 全家族统一）
Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id")));
Page<Product> page = productRepository.findByCategory("手机", pageable);

long total = page.getTotalElements();     // 触发 count 查询（大集合 count 也贵）
List<Product> data = page.getContent();
```

| 返回类型 | count 查询 | 适用 | 与 JPA 差异 |
|---------|:---:|------|------------|
| `Page<T>` | ✅ | 页码导航 | 相同 |
| `Slice<T>` | ❌（多查一条） | 加载更多 | 相同 |
| `List<T>` | ❌ | 取前 N | 相同 |
| `GeoPage<T>` | 地理分页 | 附近查询 | ✅ MongoDB 独有 |

> ⚠️ **深分页问题 MongoDB 更尖锐**：offset 分页（skip）大 offset 时全扫描跳过（与 SQL 同病）；**游标式分页**（`findBy...` 带 `_id > lastId` 条件）是正解——与 [JPA 系列深分页](../../Spring%20Data%20系列【数据访问层】/Spring%20Data%20JPA/04-Repository速查.md) 的 keyset 思路完全一致。大集合 count 也建议用独立统计或预聚合。

## 7. 执行链路与 4.x 行为变化

### 7.1 执行链路

```text
接口方法名
  → MongoRepositoryFactory 解析（PartTree 分词）
  → PartTreeMongoQuery（派生方法 → Query 对象）
  → Query → MongoTemplate.find/aggregate
  → MappingMongoConverter（POJO ↔ BSON）
  → MongoClient（BSON 文档 → 协议）
  → MongoDB 集群
```

### 7.2 4.x 行为变化（升级必读）

| 变化 | 3.x | 4.x | 影响 |
|------|-----|-----|------|
| 类型安全属性路径 | 字符串属性名 | Query/Criteria/Update 类型安全（4.1） | 编译期检查，重构友好 |
| 向量检索 | 无 | @VectorSearch + VectorIndex（4.0 起） | 新能力（[08 篇](08-向量检索与AI集成速查.md)） |
| 批量 API | 单集合 BulkOperations | 多集合 + bulkWrite()（4.1） | 新能力（[06 篇](06-批量操作与写关注速查.md)） |
| 空值注解 | JetBrains | JSpecify | 静态检查工具适配 |
| 派生查询 JSON 校验 | 运行期报错 | 启动/构建期校验 | 坏查询提前暴露 |

> 💡 升级建议：先跑全量测试（重点：派生方法正则/日期参数、@Aggregation 管道、自定义转换器），再逐项启用新能力（向量检索、bulkWrite）；注意 **java.time 类型参数在 JSON 查询中的序列化**差异（3.x 与 4.x 的 Date 转换默认值可能有别，见 [09 篇](09-集成地图与常见问题.md) 迁移清单）。

---

**下一模块**：[05-查询构造与模板速查](05-查询构造与模板速查.md)　**返回总览**：[00-组件总览](00-Spring Data MongoDB组件总览.md)

**【参考来源】**：[Spring Data MongoDB 官方参考文档（Repository）](https://docs.spring.io/spring-data/mongodb/reference/mongodb/repositories/query-methods.html)、[MongoDB 官方聚合管道文档](https://www.mongodb.com/docs/manual/core/aggregation-pipeline/)
