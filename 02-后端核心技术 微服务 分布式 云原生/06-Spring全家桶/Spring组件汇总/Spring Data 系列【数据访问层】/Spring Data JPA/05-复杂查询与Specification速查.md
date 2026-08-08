# 05 复杂查询与 Specification 速查

> JpaSpecificationExecutor、4.0 Specification 三件套、Criteria API、QueryByExample、QueryDSL 现状——"动态多条件查询"的完整手册

---

## 📚 目录

1. [动态查询四方案选型](#1-动态查询四方案选型)
2. [Specification 三件套（4.0）](#2-specification-三件套40)
3. [Specification 实战：多条件搜索页](#3-specification-实战多条件搜索页)
4. [Criteria API 底层速查](#4-criteria-api-底层速查)
5. [QueryByExample 样例查询](#5-querybyexample-样例查询)
6. [QueryDSL 现状](#6-querydsl-现状)
7. [动态查询性能注意](#7-动态查询性能注意)

---

## 1. 动态查询四方案选型

> 场景：列表页 N 个可选筛选条件（关键字、状态、价格区间、时间范围、排序），条件任意组合——这是业务系统最普遍的查询形态。

| 方案 | 原理 | 类型安全 | 复杂度 | 适用 |
|------|------|:---:|:---:|------|
| 派生方法组合 | 每个组合写一个方法 | ✅ | 低 | 条件**固定且少**（≤3 个组合）——**大多数场景够用** |
| **Specification** | 谓词构建器动态拼接 | ✅ | 中 | **任意组合的动态查询（首选）** |
| Criteria API 直写 | 底层 API 手动组装 | ✅ | 高 | Specification 覆盖不了（动态 join、子查询） |
| QueryByExample | 样例对象匹配 | ✅ | 低 | 精确匹配为主的简单筛选（不支持区间/模糊） |
| @Query + 空参跳过 | `(:x is null or o.x = :x)` | ⚠️ 弱 | 低 | 参数化 JPQL 特例（子查询/复杂结构时兜底） |

> 🎯 选型一句话：**固定组合 → 派生方法；任意组合 → Specification；Specification 表达不了 → Criteria API 直写或空参 @Query；精确匹配 → QBE**。禁止"条件一多就 if/else 拼字符串"。

## 2. Specification 三件套（4.0）

> ⚠️ **4.0 重大变化**：Spring Data 不再让所有 Specification 都走 `CriteriaQuery`——拆成三个专用类型（Spring Data 2025.1 特性）：

| 类型 | 用途 | 底层 |
|------|------|------|
| `Specification<T>` | **查询**（SELECT 谓词），API 与 3.x 完全一致 | `CriteriaQuery` |
| `DeleteSpecification<T>` | **批量删除**（Repository 删除条件） | `CriteriaDelete` |
| `UpdateSpecification<T>` | **批量更新**（Repository 更新条件） | `CriteriaUpdate` |

### 2.1 Specification（查询）——3.x 老 API 不变

```java
public interface ProductRepository extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> { }     // 加这一个接口即可

// 用法一：组合式构建
public class ProductSpecs {
    public static Specification<Product> priceBetween(BigDecimal lo, BigDecimal hi) {
        return (root, query, cb) -> cb.between(root.get("price"), lo, hi);
    }
    public static Specification<Product> nameContains(String kw) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + kw.toLowerCase() + "%");
    }
    public static Specification<Product> statusIn(List<Status> statuses) {
        return (root, query, cb) -> root.get("status").in(statuses);
    }
}

// 使用：任意组合 + 分页排序
Specification<Product> spec = Specification
        .where(ProductSpecs.priceBetween(BigDecimal.valueOf(100), BigDecimal.valueOf(5000)))
        .and(ProductSpecs.nameContains("手机"))
        .and(ProductSpecs.statusIn(List.of(Status.ON_SALE)));
Page<Product> page = productRepository.findAll(spec, PageRequest.of(0, 10, Sort.by("price").descending()));
```

| JpaSpecificationExecutor 方法 | 说明 |
|------|------|
| `findAll(Specification)` | 查询 |
| `findOne(Specification)` | 单条（多条抛异常） |
| `findAll(Specification, Pageable)` / `(Specification, Sort)` | 分页/排序 |
| `count(Specification)` | 计数 |
| `exists(Specification)` | 存在性 |

> 💡 组合语法：`Specification.where(spec1).and(spec2).or(spec3)`；`null` Specification 自动忽略（`where(null)` 等价无条件）——**分支里传 null 是合法用法**，不用特判。

### 2.2 DeleteSpecification（4.0 新增）

```java
public interface ProductRepository extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {

    void delete(DeleteSpecification<Product> spec);          // 批量删除（一条 SQL）
}

// 使用：删除过期下架商品
repository.delete((root, query, cb) -> cb.and(
        cb.lessThan(root.get("updatedAt"), cutoff),
        cb.equal(root.get("status"), Status.OFF_SHELF)));
```

> 💡 对比 [04 篇](04-Repository速查.md) 的 `deleteById`（先 select 再逐条删）：DeleteSpecification 走 **CriteriaDelete 一条 SQL**，大表清理性能差一个量级，且**不触发**生命周期回调/级联——"批量清数据"用它，"业务级联删"用逐条。

### 2.3 UpdateSpecification（4.0 新增）

```java
void update(UpdateSpecification<Product> spec);

// 使用：批量改价
repository.update((root, query, cb) -> cb.and(
        cb.equal(root.get("category"), "手机"),
        cb.greaterThan(root.get("price"), BigDecimal.valueOf(3000))));
```

> ⚠️ UpdateSpecification 只是"定位条件"，**更新字段值**仍要走 @Modifying @Query 或 CriteriaUpdate 手动 set（4.0 的 UpdateSpecification 用于 Repository 批量删除场景的配套；字段赋值用 `@Modifying` JPQL 更直观——两者按场景选用，见 [04 篇](04-Repository速查.md) 第 4 节）。

## 3. Specification 实战：多条件搜索页

```java
// Service 层组合（以商品搜索为例，条件：关键字/分类/价格区间/库存下限/上架状态/时间范围）
@Service
@RequiredArgsConstructor
public class ProductSearchService {
    private final ProductRepository productRepository;

    public Page<Product> search(ProductQuery q, Pageable pageable) {
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (StringUtils.hasText(q.keyword())) {
                ps.add(cb.like(cb.lower(root.get("name")),
                        "%" + q.keyword().toLowerCase() + "%"));
            }
            if (q.categoryId() != null) {
                ps.add(cb.equal(root.join("category").get("id"), q.categoryId()));
                // join：join 的路径要在 fetch 前加 distinct（分页 count 会重复计数）
            }
            if (q.priceLo() != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("price"), q.priceLo()));
            }
            if (q.priceHi() != null) {
                ps.add(cb.lessThanOrEqualTo(root.get("price"), q.priceHi()));
            }
            if (q.status() != null) {
                ps.add(cb.equal(root.get("status"), q.status()));
            }
            if (q.createdAfter() != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("createdAt"), q.createdAfter()));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return productRepository.findAll(spec, pageable);
    }
}
```

> 🎯 关键细节（面试/代码审查必问）：**Specification 里 join 集合后，分页 count 可能重复计数**（一对多 join 让行数膨胀）——要么 `query.distinct(true)`（select distinct），要么让 count 查询走不 join 的独立条件；`root.join("category")` 只做过滤不做加载，需要加载用 `fetch`（见 [07 篇](07-性能优化与批量操作速查.md)）。

## 4. Criteria API 底层速查

> Specification 本质是 Criteria API 的语法糖封装（lambda 里拿到的 root/query/cb 就是 Criteria 对象）。直接写 Criteria 用于：跨实体的动态 join、子查询、表达式聚合。

```java
// EntityManager 直写（Service 内注入 EntityManager 或 Repository 默认方法）
CriteriaBuilder cb = em.getCriteriaBuilder();
CriteriaQuery<Product> query = cb.createQuery(Product.class);
Root<Product> root = query.from(Product.class);

// 动态 join + 子查询
Subquery<Long> sub = query.subquery(Long.class);
Root<OrderItem> subRoot = sub.from(OrderItem.class);
sub.select(subRoot.get("productId"))
   .where(cb.greaterThan(subRoot.get("quantity"), 100));

List<Predicate> ps = new ArrayList<>();
ps.add(root.get("id").in(sub));                                  // 子查询：销量>100 的商品
if (flag) ps.add(cb.equal(root.get("status"), Status.ON_SALE));
query.where(ps.toArray(new Predicate[0])).orderBy(cb.desc(root.get("createdAt")));

List<Product> result = em.createQuery(query).setMaxResults(20).getResultList();
```

| Criteria 元素 | 用途 |
|------|------|
| `CriteriaBuilder` | 谓词工厂：equal/like/between/greaterThan/in/isNull/and/or/not |
| `Root<T>` | 实体根（root.get("属性") 建路径） |
| `Join` | root.join("关联") / fetch("关联") |
| `Subquery` | 子查询 |
| `Path` | 嵌套属性 root.get("order").get("user").get("name") |
| `CriteriaUpdate/Delete` | 批量更新/删除（Specification 的底层） |

> 💡 分层原则：**业务代码不要直接散用 Criteria**——太啰嗦且难测；把 Criteria 逻辑收敛到 Repository 默认方法/Specification 类里（每个方法一个静态工厂，便于单测）。

## 5. QueryByExample 样例查询

```java
public interface ProductRepository extends JpaRepository<Product, Long>,
        QueryByExampleExecutor<Product> { }        // 4.0 起从 JpaRepository 可直接用

// 使用：把"非 null 字段"作为查询条件（精确匹配）
Product probe = new Product();
probe.setCategory("手机");          // 只有 category 参与匹配
probe.setStatus(Status.ON_SALE);

ExampleMatcher matcher = ExampleMatcher.matching()
        .withIgnorePaths("id", "price")            // 忽略字段
        .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)  // 字符串包含
        .withIgnoreCase();
Example<Product> example = Example.of(probe, matcher);
List<Product> result = productRepository.findAll(example);
```

| 能力边界 | 说明 |
|---------|------|
| 支持 | 非 null 字段精确匹配、字符串匹配器（EXACT/CONTAINING/STARTING/ENDING）、忽略字段、大小写 |
| 不支持 | **区间、大于/小于、in、排序组合、关联嵌套条件**——这些用 Specification |

> 🎯 QBE 一句话：**"探针对象 + 匹配器"的样板查询**——适合测试数据查询、管理后台"全字段模糊筛选"原型；业务级动态查询主力还是 Specification。

## 6. QueryDSL 现状

| 项 | 现状（2026-08） |
|----|----------------|
| 官方地位 | Querydsl 项目由社区维护（OpenFeign 组织下），**非 Spring 官方组件** |
| Spring Data 集成 | `QuerydslPredicateExecutor` 接口 4.x 仍保留（需显式引入 `querydsl-jpa` + 注解处理器） |
| 推荐度 | 新项目**默认选 Specification**（官方亲儿子、无额外处理器）；QueryDSL 优势在"类型安全的复杂 join 表达式"，已重度使用 QueryDSL 的老项目可继续 |
| 与 4.0 兼容 | 派生查询重写不影响 QueryDSL 路径（其走 QuerydslPredicateExecutor 独立实现） |

```java
// 老项目继续用（若已引入 querydsl-jpa + annotationProcessor）
public interface ProductRepository extends JpaRepository<Product, Long>,
        QuerydslPredicateExecutor<Product> { }
// 使用：productRepository.findAll(QProduct.product.price.between(100, 5000).and(...))
```

> ⚠️ 迁移提醒：3.x 若用 `QuerydslPredicateExecutor`，升 4.x 前确认 querydsl-jpa 5.x 与 Hibernate 7 的兼容性（QueryDSL 5.x 的 Hibernate 适配层版本需对齐）；**新代码一律 Specification**，别引入第三套查询体系。

## 7. 动态查询性能注意

| # | 注意点 | 原因与解法 |
|---|--------|-----------|
| 1 | join 集合 + 分页 → 行膨胀 | count 重复计数：`query.distinct(true)` 或拆独立 count（见第 3 节） |
| 2 | 每字段 OR 条件 | `where (:x is null or o.x = :x)` 在数据库侧无法优化——条件多时先砍必选条件，或动态拼接（Specification 天然动态，不走这种写法） |
| 3 | 排序字段无索引 | `ORDER BY` 无索引列 → 文件排序：给高频排序列加联合索引（[MySQL 索引原理](../../../../01-关系型数据库/MySQL/04-索引原理与设计.md)） |
| 4 | Specification 里写 EAGER 关联 | join 的关联若 EAGER 会重复 join：显式 LAZY + 需要时 fetch |
| 5 | 大结果集全量 | 动态查询默认分页（Pageable 必传）；导出场景用 [04 篇](04-Repository速查.md) 流式读取 |
| 6 | 原生 count 对不上 | 复杂 Specification 的 count 查询可能与列表 SQL 不等价：`show-sql` 核对两条 SQL |

> 🎯 动态查询性能口诀：**"过滤条件走索引、join 方向从小表出发、分页必传 Pageable、排序列有索引"**——四件事做到，Specification 再多条件也不会慢；怀疑 SQL 时 `show-sql: true` 把生成的两条（列表+count）SQL 拿去 EXPLAIN。

---

**下一模块**：[06-事务与并发控制速查](06-事务与并发控制速查.md)　**返回总览**：[00-组件总览](00-Spring Data JPA组件总览.md)

**【参考来源】**：[Spring Data 2025.1 Release Notes（Wiki）](https://github.com/spring-projects/spring-data-commons/wiki/Spring-Data-2025.1-Release-Notes)、[Spring Data JPA 官方参考文档](https://docs.spring.io/spring-data/jpa/reference/)、[Querydsl 官方仓库](https://github.com/querydsl/querydsl)
