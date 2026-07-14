# JPA：Java 持久化 API 详解

## 概述

**JPA**（Java Persistence API）是 Java 官方的持久化规范，定义了一套 **ORM（对象关系映射）标准接口**，将 Java 对象自动映射为数据库表记录。JPA 本身只是规范，需要具体实现（如 Hibernate、EclipseLink）。Spring Data JPA 则是在 JPA 基础上的进一步封装，简化数据访问层开发。

---

## 一、JPA 核心概念

| 概念 | 说明 | 对应注解 |
|------|------|---------|
| **实体（Entity）** | 与数据库表映射的 Java 类 | `@Entity`、`@Table` |
| **主键** | 唯一标识实体 | `@Id`、`@GeneratedValue` |
| **字段映射** | Java 属性与数据库列的映射 | `@Column`、`@Transient` |
| **实体关系** | 表之间的关联关系 | `@OneToOne` / `@OneToMany` / `@ManyToOne` / `@ManyToMany` |
| **JPQL** | 面向对象的查询语言（类似 SQL 但操作对象） | `@Query` |
| **EntityManager** | 管理实体生命周期的核心接口 | `persist()` / `find()` / `merge()` / `remove()` |

---

## 二、JPA vs MyBatis

| 对比维度 | JPA / Hibernate | MyBatis |
|---------|:---:|:---:|
| **SQL 控制** | 自动生成 SQL，控制力弱 | 手写 SQL，完全可控 |
| **学习曲线** | 中等（需理解 ORM 思想） | 低（写 SQL 即可） |
| **开发效率** | 高（自动 CRUD） | 中（需要写 Mapper XML） |
| **复杂查询** | JPQL/Criteria API，不如原生 SQL 直观 | SQL 直接编写，灵活 |
| **缓存** | 一级缓存（Session）+ 二级缓存（SessionFactory） | 一级缓存（SqlSession）+ 二级缓存（Mapper） |
| **适用场景** | 快速开发、简单 CRUD、面向对象设计 | 复杂 SQL、高性能场景、国内主流 |
| **国内流行度** | 较低 | ⭐⭐⭐⭐⭐ 主流 |

---

## 三、常用注解速查

| 分类 | 注解 | 说明 |
|------|------|------|
| **实体** | `@Entity` | 声明该类为 JPA 实体 |
| | `@Table(name = "user")` | 指定映射的数据库表名 |
| **主键** | `@Id` | 声明主键字段 |
| | `@GeneratedValue(strategy = GenerationType.IDENTITY)` | 主键自增策略 |
| **字段** | `@Column(name = "user_name", length = 50)` | 指定列名、长度等属性 |
| | `@Transient` | 该字段不映射到数据库 |
| | `@Enumerated(EnumType.STRING)` | 枚举映射 |
| | `@Lob` | 大字段（文本/二进制） |
| **关系** | `@OneToOne` / `@OneToMany` | 一对一/一对多关系 |
| | `@ManyToOne` / `@ManyToMany` | 多对一/多对多关系 |
| | `@JoinColumn(name = "user_id")` | 指定外键列名 |
| **查询** | `@Query("SELECT u FROM User u WHERE u.name = ?1")` | 自定义 JPQL |
| | `@NamedQuery` | 命名查询 |
| **回调** | `@PrePersist` / `@PostPersist` | 持久化前后回调 |
| | `@PreUpdate` / `@PostUpdate` | 更新前后回调 |

---

## 四、Spring Data JPA 核心用法

### 4.1 Repository 接口

```java
// JpaRepository<实体类型, 主键类型>
public interface UserRepository extends JpaRepository<User, Long> {
    // 方法名即查询：Spring Data JPA 自动解析
    User findByName(String name);
    List<User> findByAgeGreaterThan(int age);
    List<User> findByNameLike(String namePattern);

    // 自定义 JPQL
    @Query("SELECT u FROM User u WHERE u.email = ?1")
    User findByEmail(String email);

    // 原生 SQL
    @Query(value = "SELECT * FROM user WHERE age > ?1", nativeQuery = true)
    List<User> findByAgeNative(int age);
}
```

### 4.2 核心方法速查

| 方法 | 说明 |
|------|------|
| `save(entity)` | 新增或更新（有 ID 则更新） |
| `findById(id)` | 按主键查询，返回 `Optional<T>` |
| `findAll()` | 查询全部 |
| `deleteById(id)` | 按主键删除 |
| `count()` | 统计总数 |
| `existsById(id)` | 判断是否存在 |
| `saveAll(list)` | 批量保存 |
| `findAll(Specification)` | 动态条件查询 |

---

## 五、JPA 核心机制

| 机制 | 说明 | 注意事项 |
|------|------|---------|
| **一级缓存** | EntityManager 级别，同一个 Session 内相同查询只访问一次数据库 | 默认开启 |
| **二级缓存** | SessionFactory 级别，跨 Session 共享 | 需手动配置（EhCache/Redis） |
| **脏检查（Dirty Check）** | 事务提交时自动比较实体当前状态与快照，自动生成 UPDATE | — |
| **N+1 问题** | 查询主实体后，访问关联实体时逐个查询导致大量 SQL | 使用 `@EntityGraph` 或 JOIN FETCH 解决 |
| **DTO 投影** | 只查询需要的字段而非整个实体 | 使用构造函数表达式或 `@Query` + DTO |

---

## 六、推荐学习资源

| 阶段 | 书籍 | 说明 |
|:----:|------|------|
| **入门** | 《Spring Data JPA：入门、实战与进阶》（张振华） | 中文实战导向，适合国内开发者 |
| **进阶** | 《Java Persistence with Hibernate》 | JPA 规范+Hibernate 实现深度讲解 |
| **深入** | 《Pro JPA 2》 | 覆盖继承、多态、性能优化等高级主题 |

---

## 七、选型建议

| 场景 | 推荐方案 |
|------|---------|
| 国内互联网公司、复杂 SQL | **MyBatis / MyBatis-Plus** |
| 快速开发、简单 CRUD、面向对象 | **Spring Data JPA** |
| 大型项目 | MyBatis + JPA 混合（复杂查询用 MyBatis，简单 CRUD 用 JPA） |

---

*最后更新：2026-07-15*
