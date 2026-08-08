# 04 Repository 速查

> 仓库接口层次、派生方法关键词全表、@Query/@Modifying、分页排序、4.0 派生查询重写与异步查询——"写接口名 = 写查询"的完整手册

---

## 📚 目录

1. [Repository 接口层次](#1-repository-接口层次)
2. [派生方法关键词全表](#2-派生方法关键词全表)
3. [@Query 自定义查询](#3-query-自定义查询)
4. [@Modifying 写操作](#4-modifying-写操作)
5. [分页与排序](#5-分页与排序)
6. [异步与流式查询](#6-异步与流式查询)
7. [4.0 派生查询重写（升级必读）](#7-40-派生查询重写升级必读)

---

## 1. Repository 接口层次

```text
Repository<T, ID>                          # 标记接口（空）
 ├── CrudRepository<T, ID>                 # 基础 CRUD：save/findById/exists/count/delete
 │    └── PagingAndSortingRepository<T, ID> # + findAll(Pageable/Sort)
 │         └── JpaRepository<T, ID>        # ★ 实战主力：+ flush/saveAndFlush/deleteAllInBatch
 │              ├── JpaSpecificationExecutor<T>   # + Specification 动态查询（05 篇）
 │              └── QueryByExampleExecutor<T>     # + 样例查询 QBE
```

| 方法 | 说明 | 注意 |
|------|------|------|
| `save(entity)` | 插入或更新（按 id 是否 null/存在判断） | **不是简单 insert**：分离态对象执行 select+merge（[09 篇](09-集成地图与常见问题.md) 的 save 陷阱） |
| `saveAll(entities)` | 批量保存 | 批处理优化见 [07 篇](07-性能优化与批量操作速查.md) |
| `findById(id)` | 返回 `Optional<T>` | 查无返回空 Optional，别 get() 裸取 |
| `existsById(id)` | 存在性检查 | 比 findById 少一次实体装载 |
| `deleteById(id)` | 按主键删除 | **先 select 再 delete**（为触发级联）——批量删除用 @Modifying |
| `count()` | 计数 | 大表慢，配合索引 |
| `saveAndFlush` | 立即 flush | 需要立即拿到 DB 侧生成值时用 |
| `deleteAllInBatch` | 一条 SQL 批量删 | 不触发级联/生命周期回调，⚠️ 与逐条 delete 语义不同 |

> 💡 默认实现都在 `SimpleJpaRepository`（[01 篇](01-模块清单.md) 运行时对象链）——读源码从它开始：`save` 的 merge 逻辑、`deleteById` 的先查后删、每个方法上的 `@Transactional` 语义都在里面。

### 1.1 各方法的默认事务语义

| 方法 | 事务 | 说明 |
|------|------|------|
| 读方法（find/count/exists） | `@Transactional(readOnly = true)` | Repository 方法自带事务；⚠️ 只覆盖**单方法**调用 |
| 写方法（save/delete） | `@Transactional` | 单方法原子 |
| 组合逻辑（多方法） | 无 | **必须 Service 层 @Transactional**（[06 篇](06-事务与并发控制速查.md) 事务边界） |

> 🎯 面试高频：**"Repository 方法自带事务吗？"**——自带，但只包住单个方法调用；一个业务动作调三个 Repository 方法时，每个方法各开一个事务，中间任何一步失败前面已提交——所以**业务事务边界必须放在 Service 层**。

## 2. 派生方法关键词全表

### 2.1 前缀与返回值

| 前缀 | 语义 | 示例 |
|------|------|------|
| `find...By` / `get...By` / `read...By` / `query...By` | 查询 | `findByUsername` |
| `count...By` | 计数 | `countByStatus(Status.ACTIVE)` → long |
| `exists...By` | 存在性 | `existsByUsername(String u)` → boolean |
| `delete...By` / `remove...By` | 删除 | `deleteByStatus(Status.LOCKED)` → long/void |
| `findFirst...By` / `findTop...By` | 限量 | `findTop10ByStatusOrderByCreatedAtDesc` |
| `findDistinct...By` | 去重 | `findDistinctByCategory` |

### 2.2 条件关键词

| 关键词 | 语义 | SQL 片段 | 示例 |
|--------|------|---------|------|
| `And` / `Or` | 与 / 或 | `AND` / `OR` | `findByNameAndPriceGreaterThan` |
| `Between` | 区间 | `BETWEEN ?1 AND ?2` | `findByPriceBetween(BigDecimal lo, hi)` |
| `LessThan` / `LessThanEqual` | < / ≤ | `col < ?` | `findByStockLessThan(int)` |
| `GreaterThan` / `GreaterThanEqual` | > / ≥ | `col > ?` | `findByCreatedAtGreaterThan(LocalDateTime)` |
| `After` / `Before` | 日期之后/之前 | `col > ?` / `col < ?` | `findByCreatedAtAfter(LocalDateTime)` |
| `IsNull` / `IsNotNull` | 为空 / 非空 | `IS NULL` / `IS NOT NULL` | `findByDeletedAtIsNull()` |
| `Like` / `NotLike` | 模糊（手写 %） | `LIKE ?` | `findByNameLike("%手机%")` |
| `Containing` / `StartingWith` / `EndingWith` | 包含/前缀/后缀 | `LIKE '%x%'` 自动加通配 | `findByNameContaining("手机")` |
| `In` / `NotIn` | 集合成员 | `IN (?)` | `findByStatusIn(List<Status>)` |
| `True` / `False` | 布尔 | `= true/false` | `findByEnabledTrue()` |
| `IgnoreCase` | 忽略大小写 | `LOWER(col) = LOWER(?)` | `findByUsernameIgnoreCase(String)` |
| `OrderByXxxDesc/Asc` | 排序 | `ORDER BY` | `findByStatusOrderByCreatedAtDesc(Status)` |
| `Not` / `NotEqual` | 取反/不等 | `<>` | `findByStatusNot(Status.LOCKED)` |
| `IsEmpty` / `IsNotEmpty` | 集合空/非空 | `size(col)=0` | `findByItemsIsEmpty()` |

### 2.3 派生方法限制（⚠️ 必读）

| 限制 | 说明 | 解法 |
|------|------|------|
| 属性名必须与实体字段**精确匹配** | 拼错 → 4.0 起**构建期失败**（好事：编译期暴露） | 看 IDE 补全提示；`Product_` 元模型类（1.3 节）校验 |
| 嵌套属性用下划线 | `findByOrder_User_Name` | 或 4.0 起直接 `findByOrderUserName`（解析歧义时用下划线消除） |
| 复杂条件（group by、子查询、多表 join） | 表达不了 | @Query / Specification（05 篇） |
| 集合属性条件 | 语义是"至少存在一个元素满足" | 注意与 SQL EXISTS 的直觉差异 |

> 💡 派生方法**适用边界**：单表、属性路径清晰的查询（占业务 60-70%）；一旦出现 join 多个关联、分组聚合、动态条件组合，立即切 @Query/Specification——派生方法写"巨型方法名"是反模式（可读性灾难）。

### 2.4 派生方法执行链路（4.0）

```text
接口方法名
  → RepositoryFactory 解析（PartTree 分词）
  → PartTreeJpaQuery（4.0 重写后：构建期生成 JPQL 文本并验证）
  → EntityManager.createQuery(JPQL)
  → Hibernate 翻译 SQL → 数据库
```

## 3. @Query 自定义查询

### 3.1 JPQL 查询（默认）

```java
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    @Query("select o from OrderEntity o join fetch o.items where o.status = :status")
    List<OrderEntity> findByStatusWithItems(@Param("status") OrderStatus status);

    @Query("select o from OrderEntity o where o.totalAmount >= :min and o.customerId = ?2")
    List<OrderEntity> findBigOrders(@Param("min") BigDecimal min, Long customerId);
    // 参数：:name 命名参数（推荐，@Param 标注）；?1 位置参数（从 1 开始）
}
```

| JPQL 能力 | 示例 |
|-----------|------|
| 投影 | `select o.id, o.customerId from ...` → Object[] / 接口投影 / record（[07 篇](07-性能优化与批量操作速查.md) 投影节） |
| join / fetch | `join fetch o.items`（**关联加载正解**，07 篇） |
| 条件/排序 | `where`、`order by`、`group by`、`having` 全支持 |
| JPA 3.2 新函数 | `\|\|`、`replace()`、`left()`、`right()`、`cast()`、`union`/`intersect`/`except` |
| 动态过滤 | `where (:name is null or o.name = :name)` 空参跳过 |
| 分页 | 方法入参 Pageable → 自动 LIMIT/OFFSET 与 count 查询 |

### 3.2 原生 SQL

```java
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    @Query(value = "select * from `order` where status = ?1", nativeQuery = true)
    List<OrderEntity> findByStatusRaw(String status);

    @Query(value = "select order_id, sum(amount) from order_item group by order_id", nativeQuery = true)
    List<Object[]> sumByOrderRaw();                     // 复杂报表的逃生舱
}
```

> ⚠️ 原生 SQL 注意：① 表名/列名写**数据库真实名字**（不是实体名）；② 返回行→实体的映射按**列名**匹配（别名要对齐实体字段）；③ 分页：原生查询 + Pageable 在某些数据库上生成 count 子句可能不正确——复杂报表返回 `List<Object[]>` 或专用 DTO 更稳；④ 命名参数在原生查询中部分数据库（Oracle）写法不同，统一用 `?1` 位置参数更保险。

### 3.3 命名查询（集中式管理）

```java
// 实体类上集中声明（entity 内 @NamedQuery）
@Entity
@NamedQueries({
    @NamedQuery(name = "OrderEntity.findByStatus", query = "select o from OrderEntity o where o.status = :status")
})
public class OrderEntity { ... }

// Repository 中引用
@Query(name = "OrderEntity.findByStatus")
List<OrderEntity> findByStatusNamed(@Param("status") OrderStatus status);
```

> 💡 定位：**团队规范"所有 JPQL 集中在实体类顶部"**（代码审查友好）时用；单项目内 @Query 在方法上（就近原则）更常见——两者等价，选一种统一即可。

## 4. @Modifying 写操作

```java
public interface UserRepository extends JpaRepository<UserAccount, Long> {

    @Modifying
    @Query("update UserAccount u set u.status = :status where u.lastLoginAt < :cutoff")
    int lockInactive(@Param("status") Status status, @Param("cutoff") LocalDateTime cutoff);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from UserAccount u where u.id in :ids")
    int deleteByIds(@Param("ids") List<Long> ids);

    @Modifying
    @Query(value = "update user_account set status = ?1 where id = ?2", nativeQuery = true)
    int updateStatusRaw(String status, Long id);
}
```

| 属性 | 默认 | 说明 |
|------|------|------|
| `flushAutomatically` | false | 执行前先 flush 待处理实体（避免脏数据覆盖）——**批量场景建议 true** |
| `clearAutomatically` | false | 执行后清空持久化上下文（JPQL 批量更新**绕开一级缓存**，不清空会读到旧值） |

> ⚠️ **三大经典坑**：① **JPQL 批量更新不触发 @PreUpdate 生命周期回调、不更新一级缓存**（clearAutomatically 解决缓存问题）；② update/delete 的 @Modifying 方法必须处于事务中（Repository 方法自带，但组合时注意 [1.1 事务边界](#11-各方法的默认事务语义)）；③ 返回类型 int（影响行数）或 void，不要配实体返回类型。

## 5. 分页与排序

### 5.1 分页 API

```java
// PageRequest：页号从 0 开始
Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());

// 组合排序
Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id"));
Pageable p2 = PageRequest.of(0, 20, sort);

// 多属性忽略大小写（JPA 3.2 支持 NULLS FIRST/LAST，4.0 线可用）
Sort.by(Sort.Order.asc("name").nullsFirst());      // NULLS FIRST
Sort.by(Sort.Order.desc("price").nullsLast());     // NULLS LAST

// 使用
Page<Product> page = productRepository.findAll(pageable);
long total = page.getTotalElements();        // 触发 count 查询
int totalPages = page.getTotalPages();
List<Product> data = page.getContent();
```

### 5.2 Page vs Slice vs List

| 返回类型 | 是否查 count | 适用 |
|---------|:---:|------|
| `Page<T>` | ✅（额外 count 查询） | 需要总页数/总条数的列表页 |
| `Slice<T>` | ❌（多查一条判断有没有下一页） | 无限滚动/加载更多（**大表推荐**） |
| `List<T>` | ❌ | 全量/取前 N（`findTop10`） |

> 🎯 面试必答：**"Page 和 Slice 区别？"**——Page 额外执行 count 查询算总条数，数据量大时 count 本身很贵；Slice 用"多取一条"判断 hasNext，少一次 count——**"加载更多"场景用 Slice，"带页码导航"才用 Page**。

### 5.3 深分页陷阱

| 分页方式 | SQL | 问题 | 结论 |
|----------|-----|------|------|
| 页码式（offset） | `LIMIT 20 OFFSET 100000` | offset 越深越慢（全扫描跳过） | 列表页可接受；大数据量禁用 |
| 游标式（keyset） | `WHERE id > :last ORDER BY id LIMIT 20` | 恒定快 | **数据量大/无限滚动必用** |

```java
// 游标分页（Keyset Pagination）——"加载更多"大表正解
public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("select p from Product p where p.id > :lastId order by p.id asc")
    List<Product> findNextPage(@Param("lastId") Long lastId, Pageable pageable);  // size 用 PageRequest.of(0, 20)
}
```

> ⚠️ 深分页三件套（面试高频）：offset 深分页慢 → 改用**游标/keyset 分页**（快但跳页不便）；实时快照需求 → ES `search_after`/PIT；真随机跳页 → 限定 offset 深度 + 缓存。JPA 侧 keyset 是主答案。

## 6. 异步与流式查询

### 6.1 异步查询

```java
public interface ReportRepository extends JpaRepository<Report, Long> {

    @Async("reportExecutor")
    CompletableFuture<List<Report>> findByCategory(String category);   // ★ 4.0 起必须 CompletableFuture
}
```

> ⚠️ 4.0 变化：`ListenableFuture` 在 Spring Framework 7 移除，`@Async` Repository 方法**必须返回 `CompletableFuture`**——3.x 老代码用 ListenableFuture 的，升级直接编译错误（见 [09 篇](09-集成地图与常见问题.md) 迁移清单）。异步查询另需配置线程池 + `@EnableAsync`。

### 6.2 流式读取

```java
@Query("select r from Report r where r.category = :category")
Stream<Report> streamByCategory(@Param("category") String category);
// 用后必须关闭（try-with-resources），且在大事务内执行——流式 = 游标逐条读，不一次性载入内存
```

> 💡 适用：导出百万行、报表逐行处理。**必须在 @Transactional 内 + try-with-resources 关闭**；否则游标未释放会吃光数据库连接（经典连接泄漏事故）。

## 7. 4.0 派生查询重写（升级必读）

> 这是 **3.x → 4.0 最大的行为变化**（Spring Data 2025.1 Release Notes 明示"derived queries rewritten"）：派生方法不再走 CriteriaQuery 生成，改为**直接生成 JPQL 文本**。

| 变化点 | 3.x 行为 | 4.0 行为 | 影响 |
|--------|---------|---------|------|
| 生成机制 | Criteria API → 运行期组装 | **JPQL 字符串生成** | SQL 文本可能不同（行为等价） |
| 验证时机 | 运行期首次调用报错 | **构建期验证** | 坏方法名/拼错属性 → 编译失败（好事） |
| 集合条件语义 | 保持 | 保持等价 | 无感知 |
| 原生查询增强 | `spring.data.jpa.query.native.parser` 可调 | 属性移除，用 `QueryEnhancerSelector` | 配置迁移 |

```java
// 4.0 构建期验证生效：下面这行在编译时直接报错（3.x 是运行期才炸）
// List<Product> findByNme(String name);   // 属性拼错 → 编译失败
```

| 迁移动作 | 说明 |
|---------|------|
| 全量回归测试 | 重点比对 `show-sql` 输出与 3.x 的差异（等价但字面不同） |
| 去掉 show-sql 断言类测试 | 别断言 SQL 字符串原文，断言结果集 |
| 删 `spring.data.jpa.query.native.parser` | 4.0 已移除该属性 |
| 编译期错误先解 | 升级后"编译失败"的派生方法 = 旧代码里的坏查询（3.x 运行时才炸），逐条修正属性名 |

> 🎯 升级心法：**4.0 把"运行期炸弹"变"编译期错误"**——这是特性不是坑；唯一要警惕的是"行为等价但 SQL 文本变化"带来的测试断言失效与线上 SQL 执行计划差异（用 EXPLAIN 复核热点查询）。

---

**下一模块**：[05-复杂查询与 Specification 速查](05-复杂查询与Specification速查.md)　**返回总览**：[00-组件总览](00-Spring Data JPA组件总览.md)

**【参考来源】**：[Spring Data 2025.1 Release Notes（Wiki）](https://github.com/spring-projects/spring-data-commons/wiki/Spring-Data-2025.1-Release-Notes)、[Spring Data JPA 3→4 Migration Guide](https://ankurm.com/spring-data-jpa-3-to-4-migration-guide-2/)、[Spring Data JPA 官方参考文档](https://docs.spring.io/spring-data/jpa/reference/)
