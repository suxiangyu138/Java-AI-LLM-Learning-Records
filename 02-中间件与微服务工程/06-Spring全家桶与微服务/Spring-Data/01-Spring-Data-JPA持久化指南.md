# Spring Data JPA 持久化指南

## 目录

1. [JPA 与 Hibernate 基础](#1-jpa-与-hibernate-基础)
2. [Spring Data JPA 核心](#2-spring-data-jpa-核心)
3. [持久化上下文](#3-持久化上下文)
4. [性能优化](#4-性能优化)
5. [审计与乐观锁](#5-审计与乐观锁)
6. [JPA vs MyBatis 选型](#6-jpa-vs-mybatis-选型)
7. [总结与清单](#7-总结与清单)

---

## 1. JPA 与 Hibernate 基础

### 1.1 JPA 规范 vs Hibernate 实现

在 Java 持久化领域，理解 JPA（Jakarta Persistence API）与 Hibernate 的关系是第一步，也是最为关键的一步。

**JPA 是一套规范，而非具体实现。** 它由 Java 社区制定，定义了一组接口和注解，规定了 Java 对象如何映射到数据库表、如何执行 CRUD 操作、如何管理实体生命周期等标准。JPA 本身不干活，它只定规矩。

**Hibernate 是 JPA 规范最主流的实现。** 在 Hibernate 5.x 和 6.x 时代，它完全实现了 JPA 2.x / 3.x 标准。除此之外，Hibernate 还提供了大量 JPA 规范之外的"超纲"功能，比如更灵活的抓取策略（`@Fetch`）、二级缓存集成、更强大的 Criteria API 等。当你写 JPA 注解时，本质上是 Hibernate 在执行底层 SQL。

**Spring Data JPA 是更高一层的抽象。** 它不直接与 JPA 规范打交道，而是通过 Repository 接口为你自动生成实现。Spring Data JPA 替你做了最繁琐的事情：你只需要定义一个接口继承 `JpaRepository`，它就能在运行时动态生成代理实现类，自动完成 CRUD、分页、排序等操作。

三者的层次关系可以概括为：

```
应用程序代码
    ↕
Spring Data JPA（Repository 抽象，自动生成实现）
    ↕
JPA 规范（javax.persistence / jakarta.persistence 包下的接口和注解）
    ↕
Hibernate（真正的 ORM 实现，执行 SQL）
    ↕
JDBC（数据库连接底层）
    ↕
数据库（MySQL / PostgreSQL / Oracle / H2 等）
```

### 1.2 实体映射基础

#### @Entity 与 @Table

每个 JPA 实体本质上是一个 POJO，用 `@Entity` 标记后，JPA 就知道这是一个需要持久化的类。`@Table` 用于指定映射到的数据库表名：

```java
@Entity
@Table(name = "users")
public class User {
    // ...
}
```

如果不写 `@Table`，默认表名就是类名（区分大小写取决于数据库配置）。规范的做法是显式指定表名。

#### @Id 与 @GeneratedValue

每个实体必须有一个主键，用 `@Id` 标记。`@GeneratedValue` 定义主键生成策略，JPA 提供了四种策略：

**AUTO（默认策略）：**
```java
@Id
@GeneratedValue(strategy = GenerationType.AUTO)
private Long id;
```
由 JPA 提供者（Hibernate）根据数据库方言自动选择。对于 MySQL，通常退化为 IDENTITY；对于 PostgreSQL，通常退化为 SEQUENCE。**AUTO 在某些场景下可能不是最优选择**，尤其是当使用 Hibernate 5 的旧版本时，它会默认使用 `hibernate_sequence` 全局序列，导致多表共用一个序列，引发主键冲突。

**IDENTITY：**
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```
依赖数据库自增列（MySQL 的 AUTO_INCREMENT、SQL Server 的 IDENTITY）。插入时数据库自动赋值。**注意：** IDENTITY 策略会导致 Hibernate 无法批量插入（batch insert），因为 Hibernate 必须在插入后立即执行 SELECT LAST_INSERT_ID() 获取主键值，这会打断 JDBC 批处理。

**SEQUENCE：**
```java
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
@SequenceGenerator(name = "user_seq", sequenceName = "user_sequence", allocationSize = 50)
private Long id;
```
使用数据库序列（PostgreSQL、Oracle、DB2 支持）。**推荐策略。** 通过 `allocationSize` 预分配序列值，Hibernate 可以在内存中缓存一段 ID 范围，实现高效的批量插入。在 MySQL 下不支持原生 SEQUENCE，但 Hibernate 可以通过表模拟。

**TABLE：**
```java
@Id
@GeneratedValue(strategy = GenerationType.TABLE, generator = "user_table_gen")
@TableGenerator(name = "user_table_gen", table = "id_generator", pkColumnName = "gen_name", valueColumnName = "gen_value", allocationSize = 50)
private Long id;
```
使用一张单独的数据库表来模拟序列。**可移植性最强**（所有数据库都支持），但性能最差（每次生成 ID 都要读写这张表）。现代项目中基本不推荐使用。

#### @Column 注解

```java
@Column(name = "user_name", nullable = false, unique = true, length = 50, updatable = false)
private String userName;
```

`@Column` 提供了细致的列映射配置：
- `name`：列名，默认与字段名相同（驼峰转下划线需依赖 `spring.jpa.hibernate.naming.physical-strategy`）
- `nullable`：是否可为空，默认 true
- `unique`：是否唯一
- `length`：字符串长度，仅对 DDL 自动建表有效
- `updatable` / `insertable`：是否参与更新/插入，常用于时间戳等自动填充字段

### 1.3 关系映射

关系映射是 JPA 最强大也最容易踩坑的地方。五种关系映射需要逐一理解。

#### @OneToOne —— 一对一关系

用户与身份证信息、订单与物流信息都是一对一的典型场景。

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 持有方（owning side）
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "id_card_id")
    private IdCard idCard;
}

@Entity
@Table(name = "id_cards")
public class IdCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String cardNumber;

    // 被持有方（inverse side），mappedBy 指向持有方的字段名
    @OneToOne(mappedBy = "idCard")
    private User user;
}
```

**关键点：**
- `mappedBy` 必须放在被持有方，值是持有方的关系字段名
- `cascade` 通常在持有方配置
- `orphanRemoval = true` 表示如果持有方不再引用某个 IdCard，该 IdCard 会被自动删除

#### @OneToMany 与 @ManyToOne —— 一对多 / 多对一

这是最常见的映射关系，比如一个用户有多篇文章。

```java
@Entity
public class User {
    @Id
    private Long id;

    // 一对多：一个用户有多篇文章
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Article> articles = new ArrayList<>();
}

@Entity
public class Article {
    @Id
    private Long id;
    private String title;

    // 多对一：多篇文章属于同一个用户
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User author;
}
```

**重要原则：** `@ManyToOne` 是关系维护方（持有外键），`@OneToMany` 使用 `mappedBy` 放弃外键维护。这样设计的好处是，当添加文章时，只需设置 `article.setAuthor(user)`，然后保存文章即可，不需要同时维护两端。

**常见陷阱：** 如果 `@OneToMany` 不使用 `mappedBy`，JPA 会在中间表中维护关系，导致产生多余的更新语句。几乎永远不要在 `@OneToMany` 一侧使用 `@JoinColumn` 来维护外键。

#### @ManyToMany —— 多对多关系

学生与课程、用户与角色的典型场景。

```java
@Entity
public class Student {
    @Id
    private Long id;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "student_course",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private Set<Course> courses = new HashSet<>();
}

@Entity
public class Course {
    @Id
    private Long id;

    @ManyToMany(mappedBy = "courses")
    private Set<Student> students = new HashSet<>();
}
```

**最佳实践：**
- 使用 `Set` 而非 `List` 避免 Hibernate 中的重复问题和低效的 delete+insert 行为
- `@JoinTable` 只在关系持有方配置
- `CascadeType` 谨慎使用，不要在双向 ManyToMany 中使用 `ALL`，避免级联删除导致意外数据丢失

### 1.4 级联操作 CascadeType

`CascadeType` 控制当对当前实体执行某个操作时，是否自动传播到关联实体。

| 策略 | 含义 |
|------|------|
| `PERSIST` | 保存当前实体时，自动保存关联实体 |
| `MERGE` | 合并当前实体时，自动合并关联实体 |
| `REMOVE` | 删除当前实体时，自动删除关联实体 |
| `REFRESH` | 刷新当前实体时，自动刷新关联实体（从数据库重新读取） |
| `DETACH` | 分离当前实体时，自动分离关联实体 |
| `ALL` | 包含以上所有行为 |

**orphanRemoval 与 CascadeType.REMOVE 的区别：**
- `CascadeType.REMOVE`：删除主实体时，级联删除关联实体。比如删除用户时，一并删除其文章。
- `orphanRemoval = true`：当从集合中移除某个关联实体（没有父实体引用它了），自动删除该实体。比如从 `user.getArticles()` 中 remove 一篇文章，该文章会被 DELETE。

实际开发中，一对多关系推荐使用 `orphanRemoval = true` 配合 `CascadeType.ALL`（或 `PERSIST + MERGE`），由父实体完全管理子实体的生命周期。

### 1.5 获取策略 FetchType

#### 延迟加载 vs 立即加载

```java
// 延迟加载 —— 只有在访问该属性时才查询数据库
@OneToMany(fetch = FetchType.LAZY)

// 立即加载 —— 加载主实体时立即查询关联实体
@ManyToOne(fetch = FetchType.EAGER)
```

**原则：** 默认情况下，`@OneToMany` 和 `@ManyToMany` 是 LAZY，`@ManyToOne` 和 `@OneToOne` 是 EAGER。**强烈建议将所有关联都显式设为 LAZY**，EAGER 往往带来性能灾难。

#### N+1 查询问题

**问题描述：** 查询 N 条主记录后，每条记录又触发一次额外查询加载关联对象，总共产生 N+1 条 SQL。

```java
// 示例：N+1 问题
List<User> users = userRepository.findAll(); // 1 条 SQL
for (User user : users) {
    System.out.println(user.getArticles().size()); // 触 N 条 SQL
}
```

**解决方案汇总：**

1. **JOIN FETCH（推荐最简单的方式）：**
```java
@Query("SELECT u FROM User u JOIN FETCH u.articles")
List<User> findAllWithArticles();
```
缺点是每次查询都要显式声明，不够灵活。

2. **@EntityGraph（更灵活的方案）：**
```java
@EntityGraph(attributePaths = "articles")
@Query("SELECT u FROM User u")
List<User> findAllWithArticles();
```
或者使用命名 EntityGraph：
```java
@NamedEntityGraph(name = "User.articles", attributeNodes = @NamedAttributeNode("articles"))
@Entity
public class User { ... }

// Repository 中
@EntityGraph("User.articles")
List<User> findAll();
```

3. **@BatchSize（批量加载）：**
```java
@Entity
public class User {
    @OneToMany(mappedBy = "author")
    @BatchSize(size = 20)
    private List<Article> articles;
}
```
将 N+1 优化为 N/20+1，适合需要懒加载但又不想改查询的场景。

4. **@Fetch(FetchMode.SUBSELECT)：**
```java
@OneToMany(mappedBy = "author")
@Fetch(FetchMode.SUBSELECT)
private List<Article> articles;
```
将 N+1 优化为 2 条 SQL：先查主表，再用子查询一次查出所有关联数据。

### 1.6 继承映射

JPA 支持三种继承映射策略，各有适用场景。

#### SINGLE_TABLE —— 单表继承

所有子类的字段都放在同一张表中，通过 `@DiscriminatorColumn` 区分类型。

```java
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
public abstract class Animal {
    @Id
    private Long id;
    private String name;
}

@Entity
@DiscriminatorValue("DOG")
public class Dog extends Animal {
    private String breed; // 狗品种
}

@Entity
@DiscriminatorValue("CAT")
public class Cat extends Animal {
    private boolean indoor; // 是否家养
}
```

**优点：** 查询效率最高，不需要 JOIN。
**缺点：** 子类的独有字段必须允许为 NULL，表字段会膨胀，无法在列级别加 NOT NULL 约束。

#### JOINED ——  joined 策略

每个类对应一张表，子类表通过外键关联到父类表。

```java
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Animal { ... }

@Entity
public class Dog extends Animal { ... }
```

**优点：** 完全符合数据库范式设计，字段无冗余。
**缺点：** 查询时需要 JOIN 多张表，性能下降；插入时需要操作多张表。

#### TABLE_PER_CLASS

每个具体子类一张完整的表，包含所有继承的字段。

**优点：** 查询子类时不需要 JOIN。
**缺点：** 多态查询（`FROM Animal`）会使用 UNION，性能极差；主键不能使用自增或序列生成。**实际项目中极少使用。**

---

## 2. Spring Data JPA 核心

### 2.1 Repository 体系

Spring Data JPA 的 Repository 接口体系是一个精心设计的层次结构：

```
Repository<T, ID>  ——— 最顶层标记接口，没有方法
    ↕
CrudRepository<T, ID>  ——— CRUD 基本操作
    ↕
PagingAndSortingRepository<T, ID>  ——— 分页与排序
    ↕
JpaRepository<T, ID>  ——— JPA 特定功能（批量操作、刷新等）
    ↕
JpaSpecificationExecutor<T>  ——— 动态查询（非继承，是额外接口）
```

**典型用法：**
```java
public interface UserRepository extends JpaRepository<User, Long>,
                                        JpaSpecificationExecutor<User> {
    // 方法命名查询
    // 分页查询
    // 自定义 @Query
}
```

**各接口核心方法：**

| 接口 | 方法 |
|------|------|
| `CrudRepository` | `save(S)`, `findById(ID)`, `findAll()`, `count()`, `deleteById(ID)`, `existsById(ID)` |
| `PagingAndSortingRepository` | `findAll(Sort)`, `findAll(Pageable)` |
| `JpaRepository` | `findAll()`, `getOne(ID)`, `getById(ID)`, `flush()`, `saveAndFlush(S)`, `saveAll(Iterable)`, `deleteAllInBatch(Iterable)` |
| `JpaSpecificationExecutor` | `findOne(Specification)`, `findAll(Specification, Pageable)`, `findAll(Specification, Sort)`, `count(Specification)` |

### 2.2 方法命名查询

Spring Data JPA 最引人注目的特性就是**方法命名查询**——根据方法名自动生成查询。这是一个约定优于配置的典范。

**命名约定语法：**

```
[操作前缀] + [条件属性] + [连接符] + [条件属性] + OrderBy + [排序属性] + [排序方向]
```

**操作前缀（动词）：**

| 前缀 | 含义 | 示例 |
|------|------|------|
| `findBy` / `findAllBy` | 查询（可返回集合或单个） | `findByUserName(String name)` |
| `getBy` | 查询（同 findBy） | `getById(Long id)` |
| `queryBy` | 查询 | `queryByStatus(String status)` |
| `readBy` | 查询 | `readByEmail(String email)` |
| `countBy` | 计数 | `countByStatus(String status)` |
| `existsBy` | 判断是否存在 | `existsByUserName(String name)` |
| `deleteBy` / `removeBy` | 删除 | `deleteByUserName(String name)` |

**条件关键字：**

```java
// 精确匹配
User findByUserName(String userName);

// 多条件 AND
List<Article> findByTitleAndStatus(String title, String status);

// 多条件 OR
List<Article> findByTitleOrContent(String title, String content);

// 比较操作
List<Article> findByCreateTimeAfter(LocalDateTime date);
List<Article> findByCreateTimeBefore(LocalDateTime date);
List<Article> findByPriceBetween(BigDecimal min, BigDecimal max);
List<Article> findByReadCountGreaterThan(int count);
List<Article> findByReadCountLessThanEqual(int count);

// 模糊查询
List<Article> findByTitleLike(String pattern); // 需要自行加 %

// 空值判断
List<Article> findByTitleIsNull();
List<Article> findByTitleIsNotNull();

// IN 查询
List<Article> findByStatusIn(List<String> statuses);

// 非
List<Article> findByStatusNot(String status);

// 排序
List<Article> findByUserIdOrderByCreateTimeDesc(Long userId);
List<Article> findByUserIdOrderByCreateTimeAsc(Long userId);
```

**嵌套属性查询：**
```java
// 通过关联实体的属性查询
List<Article> findByAuthorUserName(String userName);
// 等价于 JPQL: SELECT a FROM Article a WHERE a.author.userName = ?1
```

**限制和分页：**
```java
// 取前 10 条
List<Article> findTop10ByOrderByCreateTimeDesc();

// 取第 3 页（每页 20 条）
Page<Article> findByUserId(Long userId, Pageable pageable);
```

### 2.3 @Query 自定义查询

当方法命名查询无法满足复杂需求时（如多表关联、聚合函数、子查询），使用 `@Query` 注解。

#### JPQL / HQL 查询

```java
@Query("SELECT u FROM User u WHERE u.email = :email")
Optional<User> findByEmail(@Param("email") String email);

@Query("SELECT a FROM Article a WHERE a.author.id = :userId AND a.status = :status")
List<Article> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);
```

#### 原生 SQL 查询

```java
@Query(value = "SELECT * FROM articles WHERE MATCH(title, content) AGAINST(:keyword IN BOOLEAN MODE)",
       nativeQuery = true)
List<Article> fullTextSearch(@Param("keyword") String keyword);
```

**生成策略配置：**
```java
@Query(nativeQuery = true,
       countQuery = "SELECT COUNT(*) FROM articles WHERE MATCH(title, content) AGAINST(:keyword IN BOOLEAN MODE)")
Page<Article> searchWithPagination(@Param("keyword") String keyword, Pageable pageable);
```

注意：原生 SQL 查询是直通数据库的，不会经过 JPA 的元模型处理，返回的结果集列名必须与实体字段映射兼容。

#### SpEL 表达式

Spring Data JPA 支持在 `@Query` 中使用 SpEL 表达式，最常见的是引用实体名称：

```java
@Entity
public class User { ... }

// 使用 #{#entityName} 动态获取实体名，在继承场景中特别有用
@Query("SELECT u FROM #{#entityName} u WHERE u.status = :status")
List<User> findByStatus(@Param("status") String status);

// 对于基类和子类都能正确适配
@Query("UPDATE #{#entityName} e SET e.status = :status WHERE e.id IN :ids")
@Modifying
int updateStatusByIds(@Param("status") String status, @Param("ids") List<Long> ids);
```

### 2.4 @Modifying 更新与删除

默认情况下，`@Query` 只支持 SELECT 操作。UPDATE 和 DELETE 必须额外加 `@Modifying` 注解。

```java
@Modifying
@Query("UPDATE Article a SET a.status = :status, a.updatedAt = :now WHERE a.id IN :ids")
int batchUpdateStatus(@Param("status") String status,
                      @Param("now") LocalDateTime now,
                      @Param("ids") List<Long> ids);

@Modifying
@Query("DELETE FROM Article a WHERE a.createdAt < :before")
int deleteOldArticles(@Param("before") LocalDateTime before);
```

**clearAutomatically 参数：**
```java
@Modifying(clearAutomatically = true)
@Query("UPDATE Article a SET a.readCount = a.readCount + 1 WHERE a.id = :id")
int incrementReadCount(@Param("id") Long id);
```

`clearAutomatically = true` 在 @Modifying 执行后自动清除一级缓存，避免脏读。**建议所有 @Modifying 操作都加上这个参数。**

### 2.5 分页与排序

Spring Data JPA 提供了优雅的分页支持。

#### Pageable 与 Page

```java
// Controller 层
@GetMapping("/users/{userId}/articles")
public Page<Article> getArticles(@PathVariable Long userId,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size,
                                  @RequestParam(defaultValue = "createTime,desc") String sort) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("createTime").descending());
    return articleRepository.findByUserId(userId, pageable);
}
```

**Page 对象包含的信息：**
```java
Page<Article> page = articleRepository.findAll(pageable);
page.getContent();      // 当前页数据
page.getTotalElements(); // 总记录数
page.getTotalPages();    // 总页数
page.getNumber();        // 当前页码
page.getSize();          // 每页大小
page.hasNext();          // 是否有下一页
page.isFirst();          // 是否是第一页
```

#### Slice vs Page

- **Page：** 会执行 count 查询计算总记录数，适合需要分页条的场景，但多一次 count 查询。
- **Slice：** 只判断是否有下一页（通过查询 size+1 条记录），适合"加载更多"的无限滚动场景。

```java
// 使用 Slice 避免 count 查询
Slice<Article> slice = articleRepository.findSliceByUserId(userId, pageable);
```

#### Sort 的进阶用法

```java
// 组合排序
Sort sort = Sort.by(
    Sort.Order.desc("priority"),
    Sort.Order.asc("createTime")
);

// 忽略大小写排序
Sort.by(Sort.Order.asc("userName").ignoreCase());

// 安全排序（防止 SQL 注入）
Sort sort = Sort.by("createTime").descending();
// 不要使用：Sort.by("createTime desc") 字符串形式
```

### 2.6 Specification 动态查询

当查询条件不固定、需要动态组装时（比如高级搜索），`Specification` 是利器。

```java
public interface ArticleRepository extends JpaRepository<Article, Long>,
                                           JpaSpecificationExecutor<Article> {
}
```

#### 构建动态查询

```java
public Page<Article> searchArticles(String title, String status,
                                     LocalDateTime startDate, LocalDateTime endDate,
                                     Pageable pageable) {

    Specification<Article> spec = Specification.where(null);

    if (StringUtils.hasText(title)) {
        spec = spec.and((root, query, cb) ->
            cb.like(root.get("title"), "%" + title + "%"));
    }

    if (StringUtils.hasText(status)) {
        spec = spec.and((root, query, cb) ->
            cb.equal(root.get("status"), status));
    }

    if (startDate != null && endDate != null) {
        spec = spec.and((root, query, cb) ->
            cb.between(root.get("createTime"), startDate, endDate));
    }

    return articleRepository.findAll(spec, pageable);
}
```

#### CriteriaBuilder 核心方法

| 方法 | 说明 |
|------|------|
| `cb.equal(path, value)` | 等于 |
| `cb.notEqual(path, value)` | 不等于 |
| `cb.like(path, pattern)` | 模糊匹配 |
| `cb.between(path, v1, v2)` | 区间范围 |
| `cb.greaterThan(path, value)` | 大于 |
| `cb.lessThanOrEqualTo(path, value)` | 小于等于 |
| `cb.isNull(path)` | 为空 |
| `cb.isNotNull(path)` | 非空 |
| `cb.in(path).value(list)` | IN 查询 |
| `cb.and(predicate1, predicate2)` | AND 组合 |
| `cb.or(predicate1, predicate2)` | OR 组合 |
| `cb.conjunction()` | 恒真（用于起始条件） |
| `cb.disjunction()` | 恒假 |

#### 关联查询

```java
Specification<Article> spec = (root, query, cb) -> {
    // JOIN 关联实体
    Join<Article, User> userJoin = root.join("author", JoinType.LEFT);

    return cb.and(
        cb.equal(userJoin.get("status"), "ACTIVE"),
        cb.like(root.get("title"), "%" + keyword + "%")
    );
};
```

**Specification 最佳实践：** 将常用条件封装为静态方法，便于复用：

```java
public final class ArticleSpecifications {

    public static Specification<Article> titleLike(String keyword) {
        return (root, query, cb) ->
            cb.like(root.get("title"), "%" + keyword + "%");
    }

    public static Specification<Article> inStatus(String... statuses) {
        return (root, query, cb) ->
            root.get("status").in((Object[]) statuses);
    }

    public static Specification<Article> createdBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) ->
            cb.between(root.get("createTime"), from, to);
    }
}

// 使用
articleRepository.findAll(
    Specification.where(ArticleSpecifications.titleLike("Spring"))
        .and(ArticleSpecifications.inStatus("PUBLISHED", "DRAFT"))
        .and(ArticleSpecifications.createdBetween(start, end)),
    pageable
);
```

---

## 3. 持久化上下文

### 3.1 一级缓存 PersistenceContext

**持久化上下文（PersistenceContext）** 是 JPA 中最核心的概念之一。它本质上是一个 Map（实体类型 + ID -> 实体实例），由 `EntityManager` 管理。

**核心行为：** 在一个事务（或一个 `EntityManager` 生命周期）内，同 ID 的实体只会从数据库查询一次。

```java
@Transactional
public void demonstrateFirstLevelCache() {
    // 第一次查询 —— 发出 SQL 到数据库
    User user1 = userRepository.findById(1L).orElseThrow();
    // 第二次查询 —— 直接从 PersistenceContext 获取，不发 SQL
    User user2 = userRepository.findById(1L).orElseThrow();

    System.out.println(user1 == user2); // true —— 同一个 Java 对象
}
```

**一级缓存的生命周期：**
- 事务开始时，EntityManager 被绑定到当前事务
- 每次查询，结果先进入缓存
- 脏检查（flush）时，缓存中被修改的实体自动同步到数据库
- 事务提交或回滚时，缓存被清除

### 3.2 实体状态

理解实体的四种状态是掌握 JPA 的关键。

```
          persist()                      merge()
  Transient ──────────→ Managed ←────────── Detached
        ↑                                    │
        │                 remove()            │
        └────────────────── Removed           │
                                              │
                       EntityManager.close() ─┘
```

| 状态 | 描述 | 特征 |
|------|------|------|
| **Transient（新建态）** | `new User()` 刚创建的对象 | 没有 ID（或 ID 未被 EntityManager 识别），不在 PersistenceContext 中 |
| **Managed（持久态）** | 被 EntityManager 管理的对象 | 有 ID，在 PersistenceContext 中，修改会自动同步到数据库 |
| **Detached（游离态）** | 曾经被管理，但现在 EntityManager 已关闭 | 有 ID，但不在 PersistenceContext 中，修改不会自动同步 |
| **Removed（删除态）** | 调用 remove() 后被标记删除的对象 | 在 PersistenceContext 中，但会在 flush 时被 DELETE |

**状态转换代码示例：**

```java
@Transactional
public void stateTransitionExample() {
    // 1. Transient 状态
    User user = new User();
    user.setUserName("ZhangSan");

    // 2. persist() -> 变为 Managed 状态
    entityManager.persist(user);
    // 此时 user 在 PersistenceContext 中
    user.setUserName("ZhangSanUpdated"); // 脏检查会自动检测到修改

    // 3. 如果此时 entityManager.close()
    // user 变为 Detached 状态
}
```

### 3.3 脏检查 Dirty Checking

**脏检查（Dirty Checking）** 是 JPA 的自动变更追踪机制：当实体处于 Managed 状态时，你对实体属性的任何修改，都会在 `flush()` 时被自动检测并同步到数据库。

**原理：**
1. 实体加载时，Hibernate 在 PersistenceContext 中保存一份"快照"（snapshot）
2. flush 时，Hibernate 比对当前实体属性值与快照
3. 有差异则生成 UPDATE 语句

```java
@Transactional
public void dirtyCheckingExample() {
    User user = userRepository.findById(1L).orElseThrow(); // 加载，快照 = {name: "Old"}
    user.setUserName("NewName"); // 修改
    // 不需要调用 save()！事务提交时自动 flush，生成 UPDATE
}
```

**@DynamicUpdate 优化：**

默认情况下，Hibernate 生成的 UPDATE 语句会更新所有列，即使只有一列发生变化。

```java
@Entity
@DynamicUpdate  // 只生成有变化的列
public class User {
    // ...
}
```

**注意：** `@DynamicUpdate` 有一定的性能开销（需要额外的脏检查），只在表列非常多且频繁更新部分字段的场景下才有价值。表中列数少于 20 时，通常不需要。

### 3.4 事务 @Transactional

`@Transactional` 是声明式事务管理的核心注解。

**关键配置参数：**

```java
@Service
@Transactional(readOnly = true)  // 类级别默认只读
public class UserService {

    private final UserRepository userRepository;

    // 写操作覆盖为可写
    @Transactional
    public User createUser(UserDTO dto) {
        // ...
    }

    @Transactional(rollbackFor = BusinessException.class)  // 指定回滚异常
    public void updateUser(Long id, UserDTO dto) {
        // ...
    }

    @Transactional(timeout = 5)  // 超时秒数
    public void batchImport(List<UserDTO> dtos) {
        // ...
    }

    @Transactional(noRollbackFor = MinorException.class)  // 不回滚的异常
    public void softUpdate(Long id) {
        // ...
    }
}
```

**事务传播行为 Propagation：**

| 传播行为 | 说明 |
|----------|------|
| `REQUIRED`（默认） | 当前有事务则加入，没有则创建新事务 |
| `REQUIRES_NEW` | 挂起当前事务，创建新事务 |
| `SUPPORTS` | 当前有则使用，没有则不创建 |
| `NOT_SUPPORTED` | 以非事务方式运行 |
| `MANDATORY` | 必须有事务，否则抛异常 |
| `NEVER` | 不能有事务，否则抛异常 |
| `NESTED` | 嵌套事务（JDBC Savepoint） |

**事务隔离级别 Isolation：**

```java
@Transactional(isolation = Isolation.READ_COMMITTED)
public void doSomething() { ... }
```

**@Transactional 常见陷阱：**

1. **自调用失效：** 同一个类中的方法 A（无 @Transactional）调用方法 B（有 @Transactional），事务注解不生效。因为 Spring 代理机制只在外部调用时触发。
2. **私有方法：** `@Transactional` 不能用于 `private` 方法（代理无法拦截）。
3. **事务边界：** 事务内所有操作共享同一个 `EntityManager`，所以事务内有状态变更。

---

## 4. 性能优化

### 4.1 N+1 问题的完整解决方案

N+1 查询是 JPA 最常见的性能问题，我们已经在前文介绍了问题的产生原因。这里集中给出所有解决方案的完整对比。

| 方案 | 原理 | 优点 | 缺点 | 适用场景 |
|------|------|------|------|----------|
| **JOIN FETCH** | 在 JPQL 中使用 INNER/LEFT JOIN FETCH | 最直接，一次查询全部加载 | 代码中显式声明，不够灵活 | 特定查询方法 |
| **@EntityGraph** | 通过 attributePaths 指定需要加载的关联 | 不修改 SQL，灵活组合 | 复杂场景需要配置多个 @NamedEntityGraph | 多种不同加载需求的查询 |
| **@BatchSize** | 批量加载关联对象（N/BS+1 条 SQL） | 不修改查询逻辑，透明加载 | 仍然是多条 SQL | 懒加载为主，偶尔遍历集合 |
| **@Fetch(FetchMode.SUBSELECT)** | 用子查询代替逐条查询 | 仅需 2 条 SQL | 大数据量时子查询可能较慢 | 关联数据在同一个事务内遍历 |
| **@Fetch(FetchMode.JOIN)** | 强制用 JOIN 加载 | 同 JOIN FETCH | 始终加载，可能影响所有查询 | 极少用，太过刚性 |

**最推荐方案：** 默认使用 LAZY + 特定查询使用 `@EntityGraph` 或 `JOIN FETCH`，两者兼顾了灵活性和性能。

### 4.2 批量操作

#### saveAll 批量插入

```java
@Transactional
public void batchInsert(List<User> users) {
    userRepository.saveAll(users);
}
```

**批量插入优化配置：**

```properties
# application.yml
spring.jpa.properties.hibernate.jdbc.batch_size=30
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
spring.jpa.properties.hibernate.jdbc.batch_versioned_data=true
```

**重要：** `GenerationType.IDENTITY` 不支持批量插入。如果你的主键策略是 IDENTITY，`saveAll` 仍然是一条一条 INSERT。改用 `SEQUENCE` 策略（PostgreSQL / Oracle）或手动拼接 SQL。

#### @Modifying 批量更新

```java
@Modifying(clearAutomatically = true)
@Query("UPDATE Article a SET a.status = :status WHERE a.userId = :userId")
int batchUpdateStatusByUserId(@Param("status") String status, @Param("userId") Long userId);
```

对比逐条更新的性能差异：

```java
// ❌ 低效方式 —— N 条 UPDATE
List<Article> articles = articleRepository.findByUserId(userId);
articles.forEach(a -> a.setStatus("DELETED")); // 脏检查产生 N 条 UPDATE

// ✅ 高效方式 —— 1 条 UPDATE
articleRepository.batchUpdateStatusByUserId("DELETED", userId);
```

### 4.3 只读优化

```java
@Transactional(readOnly = true)
public Page<Article> getArticles(Pageable pageable) {
    return articleRepository.findAll(pageable);
}
```

**readOnly = true 的效果：**
1. Hibernate 设置 FlushMode 为 MANUAL（不触发脏检查）
2. 实体快照不会被追踪，减少内存消耗
3. 某些数据库（如 MySQL）会在只读事务中跳过写锁
4. 传递到数据库层时，底层连接可能使用只读副本

**注意：** `readOnly = true` 不是完全禁止写入。如果你在只读事务中调用了 `save()`，Hibernate 仍然会执行 INSERT/UPDATE。它只是跳过了自动脏检查，不阻止显式写入。

### 4.4 二级缓存

Hibernate 二级缓存（Second-Level Cache / L2 Cache）跨越多个事务和 Session，存储在不同事务之间共享的数据。

#### 缓存类型

| 缓存 | 作用域 | 说明 |
|------|--------|------|
| 一级缓存 | Session/事务 | 默认开启，无法关闭 |
| 二级缓存 | SessionFactory | 跨事务共享，需要配置 |
| 查询缓存 | SessionFactory | 缓存查询结果，需要二级缓存支持 |

#### 配置与使用

```properties
# 使用 Caffeine 作为二级缓存
spring.jpa.properties.hibernate.cache.use_second_level_cache=true
spring.jpa.properties.hibernate.cache.region.factory_class=org.hibernate.cache.jcache.JCacheRegionFactory
spring.jpa.properties.hibernate.javax.cache.provider=com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider
```

**实体缓存：**
```java
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class User {
    // ...
}
```

**查询缓存：**
```java
// 开启查询缓存
spring.jpa.properties.hibernate.cache.use_query_cache=true

// 使用
@QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
@Query("SELECT u FROM User u WHERE u.status = :status")
List<User> findByStatus(@Param("status") String status);
```

#### 缓存并发策略

| 策略 | 说明 |
|------|------|
| `READ_ONLY` | 只读数据（配置项、字典表），性能最好 |
| `READ_WRITE` | 读写数据，加锁保证一致性 |
| `NONSTRICT_READ_WRITE` | 读写数据，不严格加锁 |
| `TRANSACTIONAL` | 事务级缓存，需要 JTA 支持 |

**二级缓存的适用场景：**
- 不常修改的配置表、字典表
- 多用户频繁读取的静态数据
- 读远多于写的场景

**不适用场景：**
- 频繁更新的数据
- 精确性要求极高的金融数据

### 4.5 投影查询

投影查询（Projection）只查询需要的列，避免 SELECT * 带来的不必要数据传输和对象创建开销。

#### 接口投影

```java
// 投影接口 —— 只包含需要的字段
public interface UserSummary {
    Long getId();
    String getUserName();
    String getEmail();
}

@Query("SELECT u.id AS id, u.userName AS userName, u.email AS email FROM User u")
List<UserSummary> findUserSummaries();
```

Spring Data JPA 自动为接口投影生成代理实现。接口投影是最推荐的方式，因为它：
- 不需要实现类
- 支持嵌套投影
- 可以配合 SpEL 表达式

#### 类投影（DTO）

```java
// DTO 类
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String userName;
    private String email;
}

@Query("SELECT new com.example.dto.UserDTO(u.id, u.userName, u.email) FROM User u")
List<UserDTO> findUserDTOs();
```

**注意：** DTO 投影必须使用全限定类名 + 构造器表达式，且 DTO 需要包含对应的构造方法。

#### 投影的好处

```sql
-- ❌ 无投影：SELECT * FROM users
SELECT id, user_name, password, email, phone, avatar, status, created_at, updated_at, deleted
FROM users

-- ✅ 有投影：SELECT 3 列
SELECT id, user_name, email FROM users
```

在大数据量和高并发场景下，投影查询能显著减少网络传输和对象创建压力。

---

## 5. 审计与乐观锁

### 5.1 Spring Data JPA 审计

审计功能自动记录实体的创建时间、修改时间、创建人、修改人，是实体管理的标配。

#### 启用审计

```java
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {
    @Bean
    public AuditorAware<String> auditorAware() {
        // 从 SecurityContext 获取当前用户
        return () -> Optional.ofNullable(SecurityContextHolder.getContext())
                .map(ctx -> ctx.getAuthentication())
                .map(auth -> auth.getName())
                .or(() -> Optional.of("SYSTEM"));
    }
}
```

#### 审计注解使用

```java
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 创建时间（不可更新）
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // 最后修改时间
    @LastModifiedDate
    private LocalDateTime updatedAt;

    // 创建人（不可更新）
    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    // 最后修改人
    @LastModifiedBy
    private String updatedBy;
}
```

#### 基础抽象实体类

实际项目中，通常定义一个基类避免重复代码：

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @LastModifiedBy
    private String updatedBy;
}

// 使用
@Entity
public class Article extends BaseEntity {
    // 只需定义业务字段
}
```

### 5.2 乐观锁 @Version

乐观锁是一种非阻塞的并发控制机制。它假设在大多数情况下，数据不会同时被修改，因此在更新时才检查版本冲突。

```java
@Entity
public class Product {

    @Id
    private Long id;

    private String name;

    @Version
    private Long version;
}
```

**工作流程：**

```
事务 A 读取 product(id=1, version=0)
事务 B 读取 product(id=1, version=0)

事务 A 更新：
  UPDATE product SET name='A', version=1 WHERE id=1 AND version=0
  => 成功（1 行受影响）

事务 B 更新：
  UPDATE product SET name='B', version=1 WHERE id=1 AND version=0
  => 失败（0 行受影响，数据库当前 version=1）
  => Hibernate 抛出 OptimisticLockException
```

**@Version 的关键特性：**
1. 支持的类型：`int`、`Integer`、`long`、`Long`、`Timestamp`
2. 每次 UPDATE 自动自增，无需手动管理
3. 更新时自动在 WHERE 条件中加入版本判断
4. 不推荐使用 `Timestamp` 类型（精度问题可能导致误判）

**乐观锁的实战处理：**

```java
@RestControllerAdvice
public class OptimisticLockExceptionHandler {

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<String> handleOptimisticLock(OptimisticLockException e) {
        // 提示用户重试
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("数据已被其他用户修改，请刷新后重试");
    }
}
```

**乐观锁 vs 悲观锁：**

| 特性 | 乐观锁（@Version） | 悲观锁（@Lock） |
|------|-------------------|-----------------|
| 原理 | 版本号检测，更新时验证 | SELECT ... FOR UPDATE 加锁 |
| 性能 | 好，无锁开销 | 差，会锁行甚至死锁 |
| 冲突检测时机 | 提交时 | 查询时 |
| 适用场景 | 读多写少，冲突概率低 | 写多读少，冲突概率高 |
| 实现方式 | 添加 @Version 字段 | `@Lock(LockModeType.PESSIMISTIC_WRITE)` |

**@Lock 悲观锁示例：**
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Product p WHERE p.id = :id")
Optional<Product> findByIdWithPessimisticLock(@Param("id") Long id);
```

---

## 6. JPA vs MyBatis 选型

### 6.1 JPA 的强项

**适用场景：领域驱动设计（DDD）**

JPA 的实体对象模型天然契合 DDD 的聚合根、值对象、实体等概念。你的代码操作的是**业务对象**，而不是**SQL 语句**。

```java
// JPA：操作业务对象
Order order = orderRepository.findById(orderId).orElseThrow();
order.pay(paymentAmount); // 业务方法
orderRepository.save(order); // 自动级联保存

// 对比 MyBatis：需要手动管理 SQL 和关联
Order order = orderMapper.selectById(orderId);
order.setStatus("PAID");
orderMapper.update(order);
orderItemMapper.batchUpdateStatus(orderId, "PAID"); // 手动处理子表
```

**适用场景：CRUD 密集型应用**

对于标准的管理后台、用户系统、内容管理等以 CRUD 为主的应用，JPA 的自动查询生成能减少 70% 以上的 SQL 编写量。

**适用场景：表关系复杂的场景**

JPA 的 `@OneToMany`、`@ManyToMany` 等关系映射能自动处理关联表的维护。MyBatis 需要你手写每一条 JOIN SQL 和 ResultMap。

### 6.2 MyBatis 的强项

**适用场景：复杂 SQL**

当 SQL 涉及多层嵌套子查询、动态列选择、特定数据库方言函数（如全文索引、窗口函数、递归 CTE）时，MyBatis 的 SQL 直接控制能力远胜 JPA。

```xml
<!-- MyBatis：完全控制 SQL -->
<select id="searchReport" resultType="ReportDTO">
    WITH ranked AS (
        SELECT *,
               ROW_NUMBER() OVER (PARTITION BY category ORDER BY score DESC) as rn
        FROM products
        WHERE MATCH(name, description) AGAINST(#{keyword} IN BOOLEAN MODE)
    )
    SELECT * FROM ranked WHERE rn &lt;= 10
</select>
```

**适用场景：报表查询**

报表通常需要跨多张表进行聚合和转换，结果集往往不是实体对象的直接映射，而是各种 DTO。MyBatis 的灵活映射在此处如鱼得水。

**适用场景：遗留数据库**

如果数据库已经存在（尤其是表结构不规范的旧系统），JPA 的对象映射会遇到大量困难（复合主键、无外键关联、字段类型不规范）。MyBatis 能更好地应对这些现实问题。

**适用场景：需要精细 SQL 控制**

在超大规模数据场景下，你可能需要精确控制每条 SQL 的执行计划（比如使用索引提示、分库分表路由、读写分离等）。MyBatis 让你直接掌控 SQL。

### 6.3 混合使用策略

实际的大型项目通常是 JPA + MyBatis 混合使用。这是最务实的方案。

```java
// JPA：负责标准的 CRUD 和简单关联查询
public interface UserRepository extends JpaRepository<User, Long> {
    Page<User> findByStatus(String status, Pageable pageable);
}

// MyBatis：负责复杂报表和统计查询
@Mapper
public interface ReportMapper {
    List<DailyReportVO> queryDailyReport(@Param("date") String date);
}
```

**混合使用的工程建议：**

1. **JPA 负责：** 业务实体的 CRUD、简单列表查询、关联实体操作、事务性写入
2. **MyBatis 负责：** 统计报表、大数据量导出、复杂多表 JOIN、全文搜索、原生数据库操作
3. **组织方式：** 实体类放 JPA（带 @Entity），DTO/VO 放 MyBatis（不带实体注解），两者共存
4. **事务一致性：** 同一个 @Transactional 中同时使用 JPA Repository 和 MyBatis Mapper 没有问题

### 6.4 选型决策树

```
业务场景是什么？
│
├── 领域驱动设计、DDD、充血模型 → JPA
├── 标准 CRUD、简单查询为主 → JPA
├── 表关系复杂、需要级联操作 → JPA
│
├── 复杂 SQL、多维报表 → MyBatis
├── 遗留数据库、表结构不规范 → MyBatis
├── 需要精细 SQL 优化（索引提示等） → MyBatis
│
└── 大型综合项目 → JPA + MyBatis 混合
```

---

## 7. 总结与清单

### 核心要点回顾

Spring Data JPA 的本质是让开发者专注于**实体关系建模**和**业务逻辑**，而非重复的 SQL 编写。它的学习曲线比 MyBatis 更陡峭，但一旦掌握，开发效率会显著高于 MyBatis。

### 最终检查清单

**一、实体映射**
- [ ] 合理选择主键生成策略（优先 SEQUENCE，避免 IDENTITY 导致无法批量插入）
- [ ] 所有关联关系显式设置 fetch = FetchType.LAZY
- [ ] 明确关系的维护方（owning side）和 mappedBy
- [ ] 谨慎选择 CascadeType，不在 ManyToMany 中使用 CascadeType.ALL
- [ ] 使用 Set 而非 List 管理集合关联

**二、查询优化**
- [ ] 使用 @EntityGraph 或 JOIN FETCH 解决 N+1 问题
- [ ] 复杂查询用 @Query，动态条件用 Specification
- [ ] 分页查询使用 Pageable，无限滚动使用 Slice
- [ ] 大数据量只读场景使用投影查询（接口或 DTO）
- [ ] @Modifying 操作加上 clearAutomatically = true

**三、事务与缓存**
- [ ] 读操作标注 @Transactional(readOnly = true)
- [ ] 批量操作配置 hibernate.jdbc.batch_size
- [ ] 自调用事务时注意 AOP 代理失效问题
- [ ] 只读不频繁修改的数据使用二级缓存
- [ ] 并发写入频繁的实体使用 @Version 乐观锁

**四、代码质量**
- [ ] 创建基础审计实体类避免重复代码
- [ ] 使用 DTO 投影分离内部实体与对外接口
- [ ] 合理使用继承映射（优先 SINGLE_TABLE 或 JOINED）
- [ ] 不信任 EAGER 加载 —— 始终用 LAZY
- [ ] 不在循环或流式操作中访问懒加载属性

**五、选型决策**
- [ ] 领域复杂、CRUD 密集型 → JPA
- [ ] 复杂 SQL、报表查询 → MyBatis
- [ ] 大型项目 → 混合使用 JPA + MyBatis

---

> **记住一句核心原则：** JPA 是面向对象的持久化方案，你用 Java 对象的思维去设计实体，让 JPA（Hibernate）去翻译 SQL。当你发现你需要绕过 JPA 做大量原生 SQL 时，可能是你的实体模型设计出了问题，也可能是你应该选择 MyBatis。认识每种工具的边界，才是最好的架构决策。
