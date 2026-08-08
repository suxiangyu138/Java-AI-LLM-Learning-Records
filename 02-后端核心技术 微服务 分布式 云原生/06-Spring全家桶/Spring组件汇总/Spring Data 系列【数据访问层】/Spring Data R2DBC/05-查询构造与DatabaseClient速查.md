# 05 查询构造与 DatabaseClient 速查

> DatabaseClient SQL 直写、R2dbcEntityTemplate 实体级模板、Criteria/Update 类型安全（4.1）、参数绑定与 RowMapper、执行链路——"一切查询的最终出口"完整手册

---

## 📚 目录

1. [DatabaseClient：SQL 直写入口](#1-databaseclientsql-直写入口)
2. [R2dbcEntityTemplate：实体级模板](#2-r2dbcentitytemplate实体级模板)
3. [Criteria 与 Update：类型安全条件（4.1）](#3-criteria-与-update类型安全条件41)
4. [参数绑定与 RowMapper](#4-参数绑定与-rowmapper)
5. [事务内直写与执行链路](#5-事务内直写与执行链路)
6. [模板 vs Repository 选型](#6-模板-vs-repository-选型)

---

## 1. DatabaseClient：SQL 直写入口

**通俗**：R2DBC 的"原生 SQL 大锤"——复杂 join、报表、特殊 SQL 全部在这里写，参数绑定安全、映射可控。

```java
@Service
@RequiredArgsConstructor
public class ReportService {
    private final DatabaseClient client;      // Boot 自动装配（绑定 spring.r2dbc.url）

    // ① 查询 → 实体
    public Flux<Product> findHotProducts(int limit) {
        return client.sql("select * from product order by sales desc limit :limit")
                .bind("limit", limit)
                .map((row, meta) -> new Product(
                        row.get("id", Long.class),
                        row.get("name", String.class),
                        row.get("price", BigDecimal.class)))
                .all();
    }

    // ② 查询 → DTO record（构造器投影）
    public Flux<OrderWithUser> findOrders(String status) {
        return client.sql("""
                select o.id, o.order_no, u.username
                from `order` o join user_account u on o.customer_id = u.id
                where o.status = :status
                """)
                .bind("status", status)
                .map(OrderWithUser.class)      // record 投影自动映射（列名→构造器）
                .all();
    }

    // ③ 单条 / 标量
    public Mono<Long> countActive() {
        return client.sql("select count(*) from user_account where status = 'ACTIVE'")
                .map((row, meta) -> row.get(0, Long.class))
                .one();
    }
}
```

| 操作 | API | 说明 |
|------|-----|------|
| 查询多条 | `.sql(...).map(...).all()` → `Flux<T>` | 逐行映射 |
| 查询单条 | `.map(...).one()` → `Mono<T>` | 多于一条报错 |
| 查零或一 | `.map(...).first()` → `Mono<T>` | 空返回空 Mono |
| 更新/删除 | `.sql(...).fetch().rowsUpdated()` → `Mono<Integer>` | 影响行数 |
| 插入回填 | `.sql("insert ...").then()` | 自增 id 回填走实体模板更顺 |
| 事务 | `.inTransaction(...)` / 配合 @Transactional | [06 篇](06-响应式事务与并发控制速查.md) |

> 🎯 **什么时候用 DatabaseClient**：多表 join、复杂聚合、报表、动态 SQL、以及"实体映射器管不了"的类型（JSON 列）——**R2DBC 的哲学是"复杂查询直接写 SQL"**，这与 JPA（@Query 是逃生舱）正好相反：SQL 直写是 R2DBC 的第一公民。

## 2. R2dbcEntityTemplate：实体级模板

**通俗**：不想写 SQL 时的"实体级便捷层"——select/insert/update/delete 按实体约定生成 SQL（同 JPA 的"约定优于配置"，但**没有缓存/懒加载/脏检查**）。

```java
@Repository
@RequiredArgsConstructor
public class ProductDao {
    private final R2dbcEntityTemplate template;

    // 查询（实体名推断表名，属性推断列名）
    public Flux<Product> findByCategory(String category) {
        return template.select(Product.class)
                .matching(Criteria.where("category").is(category))
                .all();
    }

    // 更新（显式 Update 对象，无脏检查）
    public Mono<Long> updatePrice(Long id, BigDecimal newPrice) {
        return template.update(Product.class)
                .matching(Criteria.where("id").is(id))
                .apply(Update.set("price", newPrice))
                .first();                       // Mono<Long> 影响行数
    }

    // 插入/保存（无 id 插入，有 id 更新）
    public Mono<Product> save(Product p) { return template.insert(p); }

    // 批量插入
    public Flux<Product> insertAll(List<Product> list) { return template.insertAll(list); }
}
```

| 能力 | API | 说明 |
|------|-----|------|
| 查询 | `select(实体).matching(Criteria).all()/one()/first()` | Criteria 动态条件 |
| 更新 | `update(实体).matching(条件).apply(Update).first()` | 显式更新 |
| 删除 | `delete(实体).matching(条件).all()` | — |
| 插入 | `insert(entity)` / `insertAll(list)` | 自增 id 回填 |
| 事务 | 方法级 @Transactional 包住 | [06 篇](06-响应式事务与并发控制速查.md) |

> 💡 与 MongoTemplate 的对照：R2dbcEntityTemplate ≈ MongoTemplate 的"实体级"那一半；区别是 MongoDB 的模板还管聚合/索引，R2DBC 的实体模板只管 CRUD——**复杂查询请去 DatabaseClient**。

## 3. Criteria 与 Update：类型安全条件（4.1）

> **4.1 新特性**：类型安全属性路径（`PropertyPath.of(Product::getPrice)`），告别字符串属性名——编译期检查、重构友好。

```java
// 4.1 类型安全写法（推荐新代码）
import static org.springframework.data.relational.core.query.Criteria.*;

Criteria c = where(PropertyPath.of(Product::getCategory)).is("手机")
        .and(PropertyPath.of(Product::getPrice)).between(BigDecimal.valueOf(100), BigDecimal.valueOf(5000));

// 传统字符串写法（仍可用）
Criteria legacy = Criteria.where("category").is("手机")
        .and("price").between(100, 5000);

// 动态组合（同 JPA Specification 思路）
List<Criteria> cs = new ArrayList<>();
if (kw != null) cs.add(where(Product::getName).like("%" + kw + "%"));
if (min != null) cs.add(where(Product::getPrice).greaterThanOrEquals(min));
Criteria finalC = cs.isEmpty() ? Criteria.empty() : cs.stream().reduce(Criteria::and).get();
```

| Criteria 方法 | 语义 |
|--------------|------|
| `is` / `not` | = / <> |
| `greaterThan` / `lessThan` / `greaterThanOrEquals` / `lessThanOrEquals` | 比较 |
| `between(a, b)` / `notBetween` | 区间 |
| `like` / `notLike` | LIKE（通配符自己写） |
| `in` / `notIn` | IN |
| `isNull` / `isNotNull` | IS NULL |
| `and` / `or`（静态组合） | 逻辑 |

```java
// Update 类型安全（4.1）
Update u = Update.update(Product::getPrice, newPrice)
        .set(Product::getUpdatedAt, LocalDateTime.now())
        .set(Product::getStatus, "OFF");
// 传统：Update.update("price", newPrice).set("updatedAt", ...)
```

> 🎯 升级价值点：**4.1 类型安全属性路径 = 数据库字段重构时的编译期安全网**——把高频查询的字符串条件换成 `PropertyPath` 写法，字段改名即编译报错（与 JPA 的 Criteria 元模型同理，但免注解处理器）。

## 4. 参数绑定与 RowMapper

### 4.1 参数绑定

```java
// 命名参数（DatabaseClient 默认：:name）
client.sql("select * from product where category = :category and price >= :min")
        .bind("category", "手机")
        .bind("min", BigDecimal.valueOf(100));

// 可空参数
.bindNull("min", BigDecimal.class)          // 显式绑定 null（不绑 = SQL 参数缺失报错）

// 集合参数（IN 展开）
client.sql("select * from product where id in (:ids)")
        .bind("ids", List.of(1L, 2L, 3L));  // 驱动自动展开成 (1,2,3)
```

> ⚠️ **参数安全红线**：**永远用 bind，禁止字符串拼接**（SQL 注入）；DatabaseClient 的 bind 是预编译占位（`?`），与 JDBC PreparedStatement 同安全等级。

### 4.2 RowMapper 三种姿势

```java
// ① lambda 手动映射（最可控）
.map((row, meta) -> new Product(
        row.get("id", Long.class),
        row.get("name", String.class),
        row.get("price", BigDecimal.class)))

// ② record/DTO 自动映射（列名对齐构造器）
.map(OrderWithUser.class)

// ③ Bean 映射（属性名对齐列名，自动驼峰）
.map(Product.class)
```

| 姿势 | 适用 |
|------|------|
| lambda 手动 | 列名复杂/多表 join 别名/类型转换 |
| record 投影 | 新代码首选（简洁不可变） |
| Bean 自动 | 列名与属性名完全一致 |

> 💡 映射规则：**列名 → 属性名默认按驼峰转换**（`order_no` → `orderNo`）；不一致时用 SQL 别名对齐（`select ... as xxx`）或 lambda 手动取。

## 5. 事务内直写与执行链路

### 5.1 事务内直写

```java
// DatabaseClient 自动感知 @Transactional（Reactor Context 传播连接）
@Transactional
public Mono<Void> transfer(Long from, Long to, BigDecimal amt) {
    return client.sql("update account set balance = balance - :amt where id = :id")
            .bind("amt", amt).bind("id", from)
            .fetch().rowsUpdated()
            .flatMap(rows -> client.sql("update account set balance = balance + :amt where id = :id")
                    .bind("amt", amt).bind("id", to)
                    .fetch().rowsUpdated().then())
            .then();
}
```

> ⚠️ **事务内直写的注意**：① 必须保持**同一响应式管道**（事务连接靠 Reactor Context 传播，跨 `.subscribe()`/换线程池即断，[06 篇](06-响应式事务与并发控制速查.md) 失效清单）；② DatabaseClient 与 Repository 混用在同一事务时，二者都走同一 ConnectionFactory 的事务上下文，**可以混用**。

### 5.2 执行链路

```text
业务代码
  → DatabaseClient.sql(...)（参数绑定）
  → R2dbcConnectionProvider（事务上下文 / 无事务则从池取连接）
  → ConnectionPool（r2dbc-pool，非阻塞获取 Mono<Connection>）
  → 驱动（r2dbc-mysql：SQL 解析 + 协议编解码，Netty 非阻塞 IO）
  → MySQL 服务器 → 结果集 → Row 流 → RowMapper → 实体/DTO
  → Mono/Flux 交还给调用方（全程无阻塞等待）
```

> 🎯 面试必答：**"DatabaseClient 和 JdbcTemplate 什么区别？"**——JdbcTemplate 是阻塞的（每个查询占一个线程等 IO）；DatabaseClient 是响应式的（**同一个线程可以同时处理多个查询**——IO 等待期间线程去干别的）；API 上 DatabaseClient 全部返回 Mono/Flux、参数用 bind（占位符安全绑定）、映射用 RowMapper（同 JdbcTemplate 概念）。

## 6. 模板 vs Repository 选型

| 维度 | Repository | R2dbcEntityTemplate | DatabaseClient |
|------|-----------|--------------------:|----------------|
| 学习成本 | 低（声明式） | 中 | 中高（SQL 直写） |
| 动态条件 | 派生方法难表达 | ✅ Criteria 动态 | ✅ 手拼 SQL（小心注入） |
| 多表 join | @Query | ❌ | ✅ **主战场** |
| 复杂聚合 | @Query | ❌ | ✅ |
| 类型安全 | 方法名解析 | ✅ 4.1 属性路径 | SQL 文本（bind 安全） |
| 适用 | 常规 CRUD（**默认首选**） | 动态条件 CRUD | **复杂查询/报表/特殊类型** |

> 🎯 工程惯例：**CRUD 走 Repository（可读可测），动态条件走实体模板（Criteria），复杂查询走 DatabaseClient（SQL 直写）**——三件套按查询复杂度递进，SQL 直写绝不用于简单 CRUD（重复造轮子）。

---

**下一模块**：[06-响应式事务与并发控制速查](06-响应式事务与并发控制速查.md)　**返回总览**：[00-组件总览](00-Spring Data R2DBC组件总览.md)

**【参考来源】**：[Spring Data R2DBC 官方参考文档（DatabaseClient）](https://docs.spring.io/spring-data/r2dbc/reference/r2dbc/advanced-dbclient.html)、[Spring Data 2026.0.0-M1（类型安全属性路径）](https://spring.io/blog/2026/02/13/spring-data-2026-0-0-m1-released)
