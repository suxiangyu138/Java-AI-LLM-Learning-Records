# 00 Spring Data JPA 组件总览

> 组件卡片：Spring Data JPA 是什么、版本现状、能做什么、与深度体系如何衔接——本系列是"组件汇总"速查型文档，JPA 规范与 ORM 底层的深挖见 [MySQL 事务与锁机制](../../../../01-关系型数据库/MySQL/03-事务与锁机制.md)、[Spring 声明式事务管理](../../../Spring框架核心/06-声明式事务管理.md)

---

## 📚 目录

1. [组件一句话定位](#1-组件一句话定位)
2. [版本现状（2026-08）](#2-版本现状2026-08)
3. [能力地图](#3-能力地图)
4. [与深度体系的映射](#4-与深度体系的映射)
5. [快速上手 3 步](#5-快速上手-3-步)
6. [速查导航](#6-速查导航)
7. [学习路线推荐](#7-学习路线推荐)
8. [核心概念速查](#8-核心概念速查)

---

## 1. 组件一句话定位

**Spring Data JPA 是 Spring Data 家族中面向关系型数据库的"默认主力"组件**——基于 JPA（Jakarta Persistence）规范，用 Repository 接口、派生方法、`@Query` 和 `@Transactional` 把 ORM 数据访问收敛成"声明式接口"，是 Java 生态里关系型数据访问的事实标准姿势。

```text
核心心智模型：
  实体类（@Entity/@Table/@Id + 关联注解）
    ├── Repository 接口：方法名派生查询 / @Query JPQL / 原生 SQL
    ├── JpaSpecificationExecutor：动态条件查询（Specification 三件套）
    └── PagingAndSortingRepository：Pageable / Sort 统一分页排序
            ↓
  EntityManager（Jakarta Persistence 3.2，JPA 规范）
            ↓
  Hibernate ORM 7.4.x（Provider：SQL 生成、一级缓存、脏检查、锁）
            ↓
  关系型数据库（MySQL / PostgreSQL / SQL Server ...）
```

| 维度 | 说明 |
|------|------|
| 所属 | Spring Data 家族（spring-data-jpa，旗舰模块） |
| 版本线 | 2026.0 发行列车 → 4.1.x（2026-08 当前线） |
| 规范基础 | Jakarta Persistence 3.2（Jakarta EE 11） |
| 默认 Provider | Hibernate ORM 7.4.x（Boot 4.1 线） |
| 配套 | Spring Framework 7.0.x / Spring Boot 4.1.x / Java 21+ |
| 定位 | POJO 中心的关系型数据访问层：实体映射、Repository、动态查询、事务、锁 |

### 1.1 Spring Data JPA 解决什么问题

直接用 Hibernate `Session` / JPA `EntityManager` 写数据访问，开发者要面对三件事：**样板 CRUD 代码**（save/findById/delete 每张表写一遍）、**查询拼接**（动态条件 if/else 串字符串，不可编译检查）、**分页排序与事务管理的手工编排**。Spring Data JPA 把这三件事收编为声明式能力：接口方法名生成查询、`@Query` 声明 JPQL、Pageable/Sort 统一分页、`@Transactional` 声明事务边界。

| 维度 | 原生 EntityManager/Hibernate | Spring Data JPA |
|------|------------------------------|-----------------|
| 基础 CRUD | 手写 find/save/delete 模板代码 | 继承 `JpaRepository` 免费获得 |
| 查询表达 | JPQL 字符串 / Criteria 链式 | 方法名派生、@Query、Specification |
| 分页排序 | 手写 setFirstResult/setMaxResults | Pageable / Sort 与 Spring Data 统一 |
| 事务 | 手写 em.getTransaction().begin/commit | @Transactional 声明式（AOP 代理） |
| 动态查询 | 拼接 Criteria 或字符串 | JpaSpecificationExecutor（4.0 起三类型） |
| 与 Spring 生态 | 无集成 | Repository、审计、事件、Boot 自动配置 |

> 🎯 判断标准一句话：**"业务代码里有没有重复的 CRUD 模板和查询拼接逻辑？"**——有，用 Spring Data JPA；没有（纯 SQL 复杂报表/存储过程驱动），直接 JDBC 或 MyBatis 更直接。

### 1.2 适用与不适用场景

| 场景 | 适合？ | 说明 |
|------|:---:|------|
| 标准业务 CRUD、管理后台 | ✅ | Repository + 派生方法是主战场 |
| 复杂关联对象图（订单→明细→商品） | ✅ | 关联映射 + fetch join 是 JPA 强项 |
| 强事务、强一致性核心数据 | ✅ | 声明式事务 + 锁（乐观/悲观） |
| 动态多条件查询（搜索列表页） | ✅ | Specification / Criteria API |
| 向量检索 / AI 语义搜索（2026 新能力） | ⚠️ 部分 | 4.0 起经 hibernate-vector 支持向量字段；深度 RAG 仍建议 [Spring Data Elasticsearch 向量检索](../../Spring Data 系列【数据访问层】/Spring Data Elasticsearch/07-向量检索与RAG集成速查.md) |
| 复杂 SQL / 报表 / 宽表聚合 | ⚠️ 部分 | 原生 SQL @Query 可做，但不如 MyBatis 直接 |
| 大数据量批量写（每日千万级） | ❌ | ORM 逐条 flush 开销大，批量走 JDBC/DataFlow |
| 极简 CRUD、SQL 完全掌控 | ❌ | 选 MyBatis 更轻（[MyBatisPlus 系列](../../../../01-关系型数据库/MyBatisPlus/00-MyBatisPlus知识体系总览.md)） |

> ⚠️ **最大认知误区**：把 JPA 当"SQL 生成器"用。JPA 的核心价值是**对象图一致性**（对象状态机 + 脏检查 + 级联），而不是帮你写 SQL——所有查询最终都要翻译成 SQL 落在数据库上，性能问题 90% 出在"查询写法没对齐数据库特性"（N+1、深分页、大结果集）。

### 1.3 与其他 Spring Data 组件的定位差异

| 组件 | 存储 | 数据模型 | 事务 | 本系列的差异点 |
|------|------|---------|:---:|--------------|
| **Spring Data JPA** | **关系型数据库** | **表/实体/关联** | ✅ | **有外键、有事务、有锁；本系列是家族的"地基"** |
| Spring Data Redis | Redis | K/V、集合 | ⚠️ 有限 | 无索引、无查询 DSL |
| Spring Data MongoDB | MongoDB | 文档 | ⚠️ | 有聚合管道，无 SQL 事务（跨文档有限） |
| Spring Data Elasticsearch | ES 9 | 文档 + 倒排索引 + 向量 | ❌ | 查询表达式是 JSON DSL，映射是动态的 |
| Spring Data R2DBC | 关系型数据库 | 表/实体 | ✅ | 响应式（非阻塞），同库可互转，无一级缓存/延迟加载 |

> 💡 本系列定位"地基"——Repository 设计哲学（派生方法、Pageable、审计、事件）是 Spring Data 全家族的共同底座，会 JPA 后上手 MongoDB/ES/Redis 的数据访问层几乎零门槛（同级目录各有独立系列）。

## 2. 版本现状（2026-08）

| 发行列车 | Spring Data JPA | Hibernate | Spring Framework | Spring Boot | 状态 |
|----------|----------------|-----------|-----------------|-------------|------|
| **2026.0** | **4.1.x（4.1.0，2026-06-09）** | **7.4.x** | 7.0.x | **4.1.x** | **Current（当前线）** |
| 2025.1 | 4.0.x | 7.2.x | 7.0.x | 4.0.x | Stable（配 Boot 4.0.x） |
| 2025.0 | 3.5.x | 6.6.x | 6.2.x | 3.5.x | 停止维护 |
| 2024.1 | 3.4.x | 6.6.x | 6.1.x | 3.4.x | 停止维护 |
| 2024.0 | 3.3.x | 6.5.x | 6.1.x | 3.3.x | 停止维护 |
| 2023.1 | 3.2.x | 6.4.x | 6.1.x | 3.2.x | 停止维护 |
| 2021.2 | 2.7.x | 5.6.x | 5.3.x | 2.7.x | EOL |

> ⚠️ **版本策略（2026 起）**：Boot 4 / Spring Framework 7 要求 **Java 21 基线**，JPA 栈从 3.5 直接跳到 4.0（Jakarta EE 11 + JPA 3.2 + Hibernate 7）——**存量 Boot 3.5 项目若不升级 Java 21，只能停留在 3.5.x 且已停止维护**；新项目一律 4.1.x 线。

### 2.1 4.x 线关键变化（3.5 → 4.0 迁移要点）

| 变化 | 说明 |
|------|------|
| Java 21 + Jakarta EE 11 | `jakarta.persistence` API 升到 3.2；Boot 4 硬性要求 Java 21 |
| 派生查询重写 | 查询从 CriteriaQuery 生成改为 **JPQL 字符串生成，构建期验证**——原来运行期才报错的坏查询，现在编译期直接失败（迁移期最大行为差异） |
| Specification 三件套 | 新增 `DeleteSpecification` / `UpdateSpecification`（批量删改走 CriteriaDelete/CriteriaUpdate），原 `Specification` 专用于 SELECT |
| AOT Repositories | 构建期生成 Repository 元数据（AOT 模式启动更快、支持 GraalVM Native） |
| 空值注解换 JSpecify | 全家族统一 JSpecify `@NullMarked`，Kotlin/IDE 静态检查受影响 |
| 注解处理器换坐标 | `hibernate-jpamodelgen` → **`hibernate-processor`**（不改会静默丢 metamodel） |
| ListenableFuture 移除 | `@Async` Repository 方法必须返回 `CompletableFuture`（Spring Framework 7 移除前者） |
| @PersistenceConstructor 改名 | 多构造器实体用 `@PersistenceCreator` |
| 向量检索 | JPA 实体可声明向量字段（hibernate-vector），与 AI 检索打通 |
| JPA 3.2 新函数 | `\|\|` 拼接、`replace()`/`left()`/`right()`/`cast()`、集合操作 `union`/`intersect`/`except`、`NULLS FIRST/LAST` 排序 |

### 2.2 Hibernate 7.x 关键变化（6.6 → 7.4 时间线）

| 版本 | 里程碑 |
|------|--------|
| 7.0（2025-05） | Jakarta Persistence 3.2；QuerySpecification；mapping.xsd；**移除 enable_lazy_load_no_trans**；dialect 改为自动检测 |
| 7.2（2025-10） | HQL `like regexp`；悲观锁改进；@EmbeddedTable；FindMultipleOptions |
| 7.4（2026-05） | **分页+集合 fetch join 下沉 SQL**（修掉内存分页）；@Temporal 时态历史表；@Audited 内置审计表（原 Envers）；复合主键支持数据库生成列 |

### 2.3 关键演进时间线

| 版本 | 里程碑 |
|------|--------|
| 1.x | Repository 抽象诞生，`JpaRepository` 雏形 |
| 2.0（2018） | 派生查询关键词体系成熟、@Query、分页排序统一 |
| 3.0（2022） | javax → **jakarta** 命名空间迁移（Jakarta EE 9/10） |
| 3.3-3.5 | Boot 3.x 时代，Hibernate 6.4-6.6 |
| 4.0（2025.1 列车，2025-11） | Spring Framework 7 / Jakarta EE 11 / 派生查询重写 / AOT / JSpecify |
| 4.1（2026.0 列车，2026-06） | 配 Hibernate 7.4（分页下沉 SQL、@Audited/@Temporal）；当前主线 |

## 3. 能力地图

| 能力域 | 能力点 | 对应注解/API |
|--------|--------|-------------|
| 实体映射 | 表/列/主键/关联映射 | `@Entity`、`@Table`、`@Id`、`@GeneratedValue`、`@Column` |
| 关联映射 | 一对多/多对一/多对多 | `@OneToMany`、`@ManyToOne`、`@ManyToMany`、`@JoinColumn` |
| 继承与组合 | 继承策略/嵌入/共享父类 | `@Inheritance`、`@Embeddable`、`@MappedSuperclass` |
| 仓库抽象 | 声明式数据访问 | `CrudRepository`、`JpaRepository`、`PagingAndSortingRepository` |
| 派生查询 | 方法名生成 JPQL | `findByXxxAndYyy`、`countByXxx`、`existsByXxx` 等 |
| 自定义查询 | JPQL / 原生 SQL 直写 | `@Query`、`@Modifying`、`@QueryHints` |
| 动态查询 | 条件组合的编程式查询 | `JpaSpecificationExecutor`（Specification/DeleteSpecification/UpdateSpecification） |
| 事务 | 声明式事务边界 | `@Transactional`（传播/隔离/回滚规则） |
| 并发控制 | 乐观锁/悲观锁 | `@Version`、`@Lock`、`LockModeType` |
| 性能 | 抓取策略/图加载/批量 | `@EntityGraph`、fetch join、`@BatchSize`、join fetch |
| 审计 | 创建/修改时间与操作者 | `@CreatedDate`、`@CreatedBy`、`@LastModifiedDate`（+ Hibernate 7.4 `@Audited`） |
| 辅助能力 | 生命周期回调、二级缓存 | `@EntityListeners`、`@Cacheable`、实体事件 |

### 3.1 能力边界：Spring Data JPA 不做什么

| 能力边界 | 谁来做 | 说明 |
|---------|--------|------|
| SQL 生成与执行 | Hibernate Provider | 本组件只"声明"查询，SQL 由 Hibernate 翻译（[07 篇](07-性能优化与批量操作速查.md) 讲如何对齐） |
| 事务的 ACID 物理实现 | 数据库（[MySQL 事务与锁](../../../../01-关系型数据库/MySQL/03-事务与锁机制.md)） | @Transactional 只是"边界"，隔离级别/锁是数据库的事 |
| 连接池管理 | HikariCP（[数据库连接池系列](../../../../01-关系型数据库/数据库连接池/)） | Boot 默认 Hikari，由 `spring.datasource` 配置 |
| 复杂 SQL 报表 | 原生 SQL / MyBatis | JPA 原生查询只是"逃生舱"，不是主战场 |
| 分布式事务 | Seata / 本地消息表 | @Transactional 只覆盖单数据源本地事务 |

> ⚠️ **常见归因错误**：慢查询怪"JPA 性能差"——慢的是生成的 SQL 没对齐索引/产生了 N+1/深分页，与"ORM"本身无关；排查路径应是"SQL 对不对 → 走没走索引 → 查了多少行"（[07 篇](07-性能优化与批量操作速查.md) 有完整清单）。

## 4. 与深度体系的映射

| 速查文档 | 深度体系模块 |
|---------|-------------|
| 02-快速开始与连接配置速查 | [JDBC 核心概念](../../../../01-关系型数据库/JDBC/00-JDBC知识体系总览.md)、[数据库连接池系列](../../../../01-关系型数据库/数据库连接池/) |
| 03-实体映射与关联映射速查 | [MySQL 索引原理与设计](../../../../01-关系型数据库/MySQL/04-索引原理与设计.md)（外键/索引落库语义） |
| 06-事务与并发控制速查 | [Spring 声明式事务管理](../../../Spring框架核心/06-声明式事务管理.md)、[MySQL 事务与锁机制](../../../../01-关系型数据库/MySQL/03-事务与锁机制.md) |
| 07-性能优化与批量操作速查 | [MySQL 索引原理与设计](../../../../01-关系型数据库/MySQL/04-索引原理与设计.md)、[SQL调优系列](../../../../01-关系型数据库/SQL调优/) |
| 09-集成地图与常见问题 | [SpringBoot 系列](../../../SpringBoot/)、[SpringMVC 系列](../../../SpringMVC/) |

> 💡 分工约定：**速查页回答"API 怎么写"，深度页回答"数据库怎么工作"**——锁的隔离级别实现、索引 B+ 树原理、事务日志在 MySQL 系列；本系列聚焦"实体模型 ↔ 表 ↔ 查询的映射与编排"。

## 5. 快速上手 3 步

**① 引入依赖**（Boot 4.1 + Hibernate 7.4，`spring-boot-dependencies` BOM 管理版本）：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

**② 配置连接**（application.yml）：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/shop?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: root
  jpa:
    hibernate:
      ddl-auto: update        # 开发期自动建表；生产用 none + 迁移工具（Flyway/Liquibase）
    show-sql: true            # 开发期看 SQL；生产关掉
```

**③ 声明实体 + Repository + 使用**：

```java
@Entity
@Table(name = "product")
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private BigDecimal price;
    // getter/setter（或 record 风格构造器，见 03 篇）
}

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByNameContaining(String keyword);          // 派生查询 → LIKE
    Page<Product> findByPriceBetween(BigDecimal lo, BigDecimal hi, Pageable pageable);
}

// 使用（Service 内，@Transactional 见 06 篇）
Page<Product> page = productRepository.findByPriceBetween(
        BigDecimal.valueOf(100), BigDecimal.valueOf(5000), PageRequest.of(0, 10, Sort.by("price").descending()));
```

### 5.1 快速上手补充：本地起一个 MySQL

```bash
docker run -d --name mysql8 -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=shop mysql:8.4
```

> 💡 Boot 4 内置 H2 可直接启动零配置应用（`runtimeOnly 'com.h2database:h2'` + `ddl-auto: create-drop`），本地联调/写测试最方便；生产换 MySQL/PostgreSQL 只改 url。

### 5.2 三步验证集成真的通了

| 步骤 | 做法 | 预期 |
|------|------|------|
| ① 看启动日志 | 观察建表 DDL 输出（show-sql） | `create table product ...` 无异常 |
| ② 写读验证 | save 一条 → findById | 返回同一实体，id 已回填 |
| ③ 控制台核对 | `mysql -e "select * from shop.product"` | 行存在，列与实体一致 |

> 💡 排障起点：**先确认数据库通不通（JDBC url/账号），再查 JPA 侧**——`Communications link failure` 一般是库没起或连接串错；映射/查询问题才轮到 JPA 层排查。

## 6. 速查导航

| 文档 | 内容 |
|------|------|
| [01-模块清单](01-模块清单.md) | artifact 坐标、包结构、依赖边界与职责切割 |
| [02-快速开始与连接配置速查](02-快速开始与连接配置速查.md) | starter、DataSource、Hibernate 7 配置、Boot 4 属性、多数据源 |
| [03-实体映射与关联映射速查](03-实体映射与关联映射速查.md) | @Entity/@Id/主键策略、字段映射、四大关联、继承/嵌入、record 实体 |
| [04-Repository 速查](04-Repository速查.md) | 仓库层次、派生方法关键词全表、@Query/@Modifying、分页排序、4.0 行为变化 |
| [05-复杂查询与 Specification 速查](05-复杂查询与Specification速查.md) | Specification 三件套、Criteria API、QueryDSL 现状、命名查询 |
| [06-事务与并发控制速查](06-事务与并发控制速查.md) | @Transactional 传播/隔离/回滚、乐观锁、悲观锁、7.4 锁改进 |
| [07-性能优化与批量操作速查](07-性能优化与批量操作速查.md) | N+1、fetch join/EntityGraph、批处理、深分页、7.4 分页下沉、二级缓存、投影 |
| [08-审计、事件与多租户速查](08-审计事件与多租户速查.md) | 审计注解、生命周期回调、Spring Data 事件、软删除、多租户、7.4 @Audited/@Temporal |
| [09-集成地图与常见问题](09-集成地图与常见问题.md) | 与全家桶联动、3→4 迁移清单、高频坑与排错 |

### 6.1 阅读顺序建议

- **第一次接触**：03 → 04 → 06，先掌握"映射、仓库、事务"三件套；
- **项目实战**：02（连接）→ 03（映射）→ 04（Repository）→ 05（动态查询）→ 07（性能）→ 09（避坑）；
- **准备面试**：06（事务/锁，最高频）→ 07（N+1/性能）→ 04（派生方法）→ 03（映射/级联）；
- **存量升级**：09 篇的 3→4 迁移清单 + 02 篇的 Hibernate 7 配置变化；
- **源码学习**：01 篇的包结构 + 04 篇的查询执行链路，两条线贯通全流程。

### 6.2 前置知识建议

| 前置主题 | 所在文档 | 与本系列的关系 |
|---------|---------|--------------|
| SQL 基础与查询 | [SQL调优系列](../../../../01-关系型数据库/SQL调优/) | 所有 @Query 最终翻译成 SQL，看不懂 SQL 就无法调优 |
| MySQL 事务/索引 | [MySQL 事务与锁机制](../../../../01-关系型数据库/MySQL/03-事务与锁机制.md)、[索引原理](../../../../01-关系型数据库/MySQL/04-索引原理与设计.md) | 锁、隔离级别、索引覆盖的物理基础 |
| Spring 事务抽象 | [Spring 声明式事务管理](../../../Spring框架核心/06-声明式事务管理.md) | @Transactional 的 AOP 原理（代理、回滚、失效场景） |
| Spring Data 通用模型 | 本系列 04 篇 + 同级各 Spring Data 系列 | Repository/Pageable/审计全家族同源 |

## 7. 学习路线推荐

| 路线 | 适用 | 顺序 |
|------|------|------|
| 入门速查 | 会写 CRUD | 00 总览 → 02 连接 → 03 映射 → 04 Repository |
| 项目实践 | 上生产做业务系统 | 03 映射 → 04 Repository → 05 动态查询 → 06 事务 → 07 性能 → 09 避坑 |
| 面试冲刺 | 全考点 | 06 事务/锁 → 07 N+1 → 04 派生方法 → 03 级联 → 08 审计 |
| 升级迁移 | Boot 3.5 → 4.1 | 09 迁移清单 → 02 配置变化 → 04 派生查询行为变化 |

## 8. 核心概念速查

| 术语 | 一句话解释 |
|------|-----------|
| JPA | Jakarta Persistence 规范：实体/EntityManager/JPQL 的标准定义（3.2 版） |
| Hibernate | JPA 的默认 Provider：负责 SQL 生成、一级缓存、脏检查、锁实现 |
| @Entity | 实体类 ↔ 表（@Table 指定表名） |
| @Id / @GeneratedValue | 主键映射与生成策略（IDENTITY/SEQUENCE/TABLE/UUID） |
| Repository | 声明式仓库接口（派生方法自动翻译 JPQL） |
| 派生方法 | findByNameAndPrice 这类方法名 → 查询（4.0 起构建期生成+验证） |
| @Query | 方法上的 JPQL / 原生 SQL（?1 位置参数 / :name 命名参数） |
| Specification | 动态条件构建器（4.0 起分 Specification/Delete/Update 三类型） |
| @Transactional | 声明式事务边界（传播/隔离/回滚规则，AOP 代理实现） |
| @Version | 乐观锁版本列（更新时 CAS 校验，冲突抛 OptimisticLockException） |
| @Lock | 悲观锁声明（PESSIMISTIC_WRITE → select ... for update） |
| N+1 查询 | 查 1 条父记录连带 N 条子查询的性能陷阱（fetch join 解决） |
| @EntityGraph | 声明式指定抓取路径（替代手写 fetch join） |
| @EntityListeners | 实体生命周期回调（@PrePersist/@PreUpdate 等） |
| 一级缓存 | Session 内持久化上下文（同事务同实体只查一次库） |
| 二级缓存 | SessionFactory 级缓存（@Cacheable，7.4 起增强） |
| @Audited | Hibernate 7.4 内置审计表（原 Envers 能力） |
| LazyInitializationException | 事务外访问懒加载属性抛出的经典异常（原因与解法见 09 篇） |

---

**下一模块**：[01-模块清单](01-模块清单.md)　**返回总览**：本页

**【参考来源】**：[Spring Data 2025.1.0 goes GA（官方博客）](https://spring.io/blog/2025/11/14/spring-data-2025-1-goes-ga)、[Spring Data 2025.1 Release Notes（Wiki）](https://github.com/spring-projects/spring-data-commons/wiki/Spring-Data-2025.1-Release-Notes)、[Spring Data JPA 3→4 Migration Guide](https://ankurm.com/spring-data-jpa-3-to-4-migration-guide-2/)、[spring-data-jpa Releases（ReleaseAlert）](https://releasealert.dev/github/spring-projects/spring-data-jpa)、[Hibernate ORM Releases](https://hibernate.org/orm/releases/)、[Hibernate 7.4.0.Final 发布公告](https://in.relation.to/2026/05/26/orm-74/)、[Hibernate 7.4 新特性（JetBrains）](https://blog.jetbrains.com/idea/2026/05/hibernate-7-4-new-features/)、[Spring Boot 4 迁移（Java Code Geeks）](https://www.javacodegeeks.com/2026/05/spring-boot-4-migration-breaking-changes-new-defaultsand-what-actually-broke.html)
