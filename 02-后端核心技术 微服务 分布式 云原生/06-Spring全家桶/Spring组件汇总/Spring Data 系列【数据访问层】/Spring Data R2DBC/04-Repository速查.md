# 04 Repository 速查

> R2dbcRepository 接口层次、派生方法、@Query 直写 SQL、@Modifying、分页排序、QueryByExample、4.x 行为变化——"写接口名 = 写 SQL"的完整手册

---

## 📚 目录

1. [Repository 接口层次](#1-repository-接口层次)
2. [派生方法关键词全表](#2-派生方法关键词全表)
3. [@Query SQL 直写](#3-query-sql-直写)
4. [@Modifying 写操作](#4-modifying-写操作)
5. [分页与排序](#5-分页与排序)
6. [QueryByExample 样例查询](#6-querybyexample-样例查询)
7. [4.x 行为变化](#7-4x-行为变化)

---

## 1. Repository 接口层次

```text
Repository<T, ID>
 ├── ReactiveCrudRepository<T, ID>          # 基础 CRUD（返回 Mono/Flux）
 │    └── ReactiveSortingRepository<T, ID>  # + findAll(Sort)
 │         └── R2dbcRepository<T, ID>       # ★ 实战主力
 │              └── QueryByExampleExecutor<T>  # QBE（可单独加）
```

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `save(entity)` | `Mono<T>` | 插入（无 id）或更新（有 id），**无缓存语义，直写 SQL** |
| `saveAll(Iterable)` / `insertAll` | `Flux<T>` | 批量（[07 篇](07-性能优化与批量速查.md) 批量优化） |
| `findById(id)` | `Mono<T>` | 按主键（查无返回空 Mono，不是 Optional） |
| `existsById` | `Mono<Boolean>` | 存在性 |
| `count()` | `Mono<Long>` | 计数 |
| `deleteById` / `delete(entity)` | `Mono<Void>` | 删除 |
| `findAll()` | `Flux<T>` | 全量（大表慎用） |

> ⚠️ **响应式类型是硬约束**：Repository 方法必须返回 `Mono`/`Flux`（或 `Mono<Page>` 等响应式包装）——**返回 `T`/`List<T>` 会直接报错**；这与 JPA 最大的 API 差异，老 JPA 代码迁移不是改签名这么简单（[09 篇](09-集成地图与常见问题.md) 迁移对照）。

### 1.1 与 JPA Repository 的差异

| 维度 | Spring Data JPA | Spring Data R2DBC |
|------|-----------------|-------------------|
| 返回类型 | `T` / `List<T>` / `Page<T>` | `Mono<T>` / `Flux<T>` / `Mono<Page<T>>` |
| save 语义 | persist/merge（先查后并） | **直写**（无 id 插入，有 id 更新；无先查） |
| 一级缓存 | 有 | **无**（每次查询都打库） |
| 懒加载 | 有 | **无** |
| 派生查询生成 | JPQL | SQL（同名字段/操作符） |

> 🎯 面试必答：**"R2DBC 的 save 和 JPA 的 save 区别？"**——JPA 分离对象会先 SELECT 再 MERGE；R2DBC 的 save **无状态**：有 id 直接 UPDATE（`WHERE id=?`），无 id 直接 INSERT——没有先查后写、没有持久化上下文跟踪；**代价是"改属性不调 save 就丢"**（无脏检查），收益是"无状态、直写、可预测"。

## 2. 派生方法关键词全表

### 2.1 前缀与返回值

| 前缀 | 语义 | 返回 | 示例 |
|------|------|------|------|
| `find...By` / `get...By` / `read...By` / `query...By` | 查询 | Mono/Flux | `findByUsername` |
| `findFirst...By` / `findTop...By` | 限量 | Mono/Flux | `findTop5ByCategoryOrderByPriceDesc` |
| `count...By` | 计数 | `Mono<Long>` | `countByStatus` |
| `exists...By` | 存在性 | `Mono<Boolean>` | `existsBySku` |
| `delete...By` / `remove...By` | 删除 | `Mono<Long>` | `deleteByStatus` |

### 2.2 条件关键词（与 JPA 对照）

| 关键词 | 语义 | SQL 片段 | 与 JPA 差异 |
|--------|------|---------|------------|
| `And` / `Or` | 与 / 或 | `AND` / `OR` | 相同 |
| `Between` | 区间 | `BETWEEN ?1 AND ?2` | 相同 |
| `LessThan` / `GreaterThan` 系列 | 比较 | `<` / `>` / `<=` / `>=` | 相同 |
| `After` / `Before` | 日期前后 | `>` / `<` | 相同 |
| `IsNull` / `IsNotNull` | 空判断 | `IS NULL` / `IS NOT NULL` | 相同（SQL 语义） |
| `Like` / `Containing` | 模糊 | `LIKE '%x%'` | 相同（Containing 自动加通配符） |
| `StartingWith` / `EndingWith` | 前后缀 | `LIKE 'x%'` / `LIKE '%x'` | 相同 |
| `In` / `NotIn` | 集合成员 | `IN (?)` | 相同 |
| `True` / `False` | 布尔 | `= true/false` | 相同 |
| `OrderByXxxDesc/Asc` | 排序 | `ORDER BY` | 相同 |
| `IgnoreCase` | 忽略大小写 | `LOWER(col) = LOWER(?)` | 相同 |
| `Not` / `NotEqual` | 取反/不等 | `<>` | 相同 |

```java
public interface ProductRepository extends R2dbcRepository<Product, Long> {

    Flux<Product> findByCategoryAndPriceBetween(String category, BigDecimal lo, BigDecimal hi);
    Mono<Product> findFirstByOrderByPriceDesc();
    Mono<Long> countByStatus(String status);
    Flux<Product> findByNameContainingIgnoreCase(String kw);
    Mono<Boolean> existsBySku(String sku);
}
```

> 💡 派生方法适用边界（同 JPA）：单表、属性路径清晰的查询；**多表 join/复杂 SQL 必须 @Query 或 DatabaseClient**（聚合根模型下 join 是常态，别硬写派生方法）。

## 3. @Query SQL 直写

### 3.1 基本用法

```java
public interface OrderRepository extends R2dbcRepository<OrderEntity, Long> {

    // 命名参数（推荐）：:name
    @Query("select * from `order` where customer_id = :customerId and status = :status")
    Flux<OrderEntity> findByCustomerAndStatus(@Param("customerId") Long cid,
                                              @Param("status") String status);

    // 位置参数：?1 ?2（从 1 开始）
    @Query("select * from order_item where order_id = ?1 and qty > ?2")
    Flux<OrderItem> findItems(Long orderId, int minQty);

    // 返回标量：Mono<Long>
    @Query("select count(*) from product where category = :category")
    Mono<Long> countByCategory(@Param("category") String category);

    // 返回单字段（投影）：Flux<String>
    @Query("select name from product where category = :category")
    Flux<String> findNamesByCategory(@Param("category") String category);
}
```

| 要点 | 说明 |
|------|------|
| 语言 | **原生 SQL**（不是 JPQL）——表名/列名用数据库真实名字 |
| 参数 | `:name`（@Param）/ `?1` 位置参数 |
| 返回 | 实体 / 标量 / DTO 投影（构造器/接口） |
| 表名 | 带引号标识符：`order` 是保留字，用反引号 `` `order` ``（MySQL） |
| 分页 | 方法入参 `Pageable` → 自动拼 LIMIT/OFFSET（见 5 节） |

> ⚠️ **SQL 直写是 R2DBC 的第一公民**：没有 JPQL 层、没有方言生成器——SQL 完全由你掌控（好：可预测、可 EXPLAIN；坏：**方言差异自己管**，跨库移植要改 SQL）。

### 3.2 复杂查询：join 与聚合

```java
// join 查询（聚合根模型的常态姿势）
@Query("""
    select o.id as id, o.order_no as order_no, u.username as username
    from `order` o
    join user_account u on o.customer_id = u.id
    where o.status = :status
    """)
Flux<OrderWithUser> findOrdersWithUser(@Param("status") String status);

// 聚合（分组统计）
@Query("select category, count(*) as cnt, sum(price) as total from product group by category")
Flux<CategoryStat> statByCategory();
```

```java
// DTO 投影（构造器投影：字段名/顺序对齐）
public record OrderWithUser(Long id, String orderNo, String username) {}
public record CategoryStat(String category, Long cnt, BigDecimal total) {}
```

> 💡 **DTO 投影的映射规则**：SQL 列名 → record/接口 getter（`order_no` ↔ `orderNo` 由映射器按驼峰匹配，4.x 对 record 投影支持完善）；列名与属性不一致时用 SQL 别名对齐（`select ... as ...`）。

## 4. @Modifying 写操作

```java
public interface ProductRepository extends R2dbcRepository<Product, Long> {

    @Modifying
    @Query("update product set price = :price, updated_at = :ts where id = :id")
    Mono<Long> updatePrice(@Param("id") Long id, @Param("price") BigDecimal price,
                           @Param("ts") LocalDateTime ts);     // 返回影响行数

    @Modifying
    @Query("delete from product where status = :status")
    Mono<Long> deleteByStatusRaw(@Param("status") String status);

    // 原子自减（防超卖核心，同 JPA 的 @Modifying + 条件更新）
    @Modifying
    @Query("update product set stock = stock - :qty where id = :id and stock >= :qty")
    Mono<Long> deductStock(@Param("id") Long id, @Param("qty") int qty);   // 0 = 库存不足
}
```

| 要点 | 说明 |
|------|------|
| 注解 | `@Modifying` + `@Query`（原生 SQL） |
| 返回 | `Mono<Long>`（影响行数）/ `Mono<Void>` |
| 原子性 | 单条 UPDATE 天然原子（`stock = stock - ?` 防超卖，与 JPA/MyBatis 同思路） |
| 事务 | 单条无需；多条组合加 `@Transactional`（[06 篇](06-响应式事务与并发控制速查.md)） |

> ⚠️ **无生命周期回调**：@Modifying 批量更新不触发审计注解（@LastModifiedDate 不自动改）——批量场景手动维护时间列（与 JPA 相同边界）；**@Version 也不参与**（需手动 where version 条件）。

## 5. 分页与排序

### 5.1 分页 API

```java
// Pageable/Sort 与 JPA 同源（全家族统一）
Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt")));

// 用法一：派生方法
Mono<Page<Product>> page = productRepository.findByCategory("手机", pageable);

// 用法二：@Query + Pageable（自动拼 LIMIT/OFFSET + count）
@Query("select * from product where category = :category")
Mono<Page<Product>> findByCategoryPaged(@Param("category") String category, Pageable pageable);
```

| 返回类型 | count 查询 | 适用 |
|---------|:---:|------|
| `Mono<Page<T>>` | ✅（额外 count） | 页码导航 |
| `Flux<T>` + Pageable | ❌（只 LIMIT/OFFSET） | 加载更多（无 total） |

> ⚠️ **响应式分页细节**：① 返回 `Mono<Page<T>>` 才会自动 count（两趟查询）；② **深分页同 SQL 问题**——大 offset 慢，游标分页（`WHERE id > :lastId ORDER BY id LIMIT :size`）是正解（与 [JPA 系列 keyset](../../Spring%20Data%20系列【数据访问层】/Spring%20Data%20JPA/04-Repository速查.md) 完全一致）；③ `Sort` 字段名要对应真实列名（4.x 类型安全可用 `Sort.by(Product::getCreatedAt)`）。

## 6. QueryByExample 样例查询

```java
public interface ProductRepository extends R2dbcRepository<Product, Long>,
        QueryByExampleExecutor<Product> { }

// 使用：非 null 字段精确匹配
Product probe = new Product();
probe.setCategory("手机");
probe.setStatus("ON");

ExampleMatcher matcher = ExampleMatcher.matching()
        .withIgnorePaths("id", "price")
        .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING);
Flux<Product> result = productRepository.findAll(Example.of(probe, matcher));
```

| 能力边界 | 说明 |
|---------|------|
| 支持 | 非 null 字段精确匹配、字符串匹配器、忽略字段、大小写 |
| 不支持 | 区间、in、排序组合、关联条件——**动态多条件查询用 [05 篇](05-查询构造与DatabaseClient速查.md) Criteria** |

> 🎯 QBE 定位（同 JPA）：管理后台"全字段模糊筛选"原型；业务级动态查询主力是 Criteria/DatabaseClient。

## 7. 4.x 行为变化

| 变化 | 3.x | 4.x | 影响 |
|------|-----|-----|------|
| 标识符带引号 | 无引号 | **默认带引号** | 表/列大小写敏感行为变化（升级重点审查） |
| 复合主键 | 支持有限 | @IdClass 完善 | 老复合键项目可平移 |
| @Embedded | 有限 | 完善（嵌套） | 值对象组合 |
| 类型安全属性路径 | 字符串 | Criteria/Update 类型安全（4.1） | 写法升级（[05 篇](05-查询构造与DatabaseClient速查.md)） |
| 聚合根 upsert | — | 单语句 upsert（4.1） | 新能力（[07 篇](07-性能优化与批量速查.md)） |
| AOT Repository | — | 支持 | GraalVM Native 可用 |
| 空值注解 | JetBrains | JSpecify | 静态检查适配 |

> 💡 升级建议：先跑全量测试（重点：表列大小写、@Query 引号行为、投影映射），再启用新能力（类型安全路径、upsert）——[09 篇](09-集成地图与常见问题.md) 有完整迁移清单。

---

**下一模块**：[05-查询构造与 DatabaseClient 速查](05-查询构造与DatabaseClient速查.md)　**返回总览**：[00-组件总览](00-Spring Data R2DBC组件总览.md)

**【参考来源】**：[Spring Data R2DBC 官方参考文档（Repository）](https://docs.spring.io/spring-data/r2dbc/reference/r2dbc/repositories.html)、[Spring Data Relational 4.0 Release Notes](https://mygit.top/release/262427881)
