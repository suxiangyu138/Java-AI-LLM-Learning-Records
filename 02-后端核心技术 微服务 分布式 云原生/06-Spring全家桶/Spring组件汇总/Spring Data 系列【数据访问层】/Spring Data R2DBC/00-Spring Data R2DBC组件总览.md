# 00 Spring Data R2DBC 组件总览

> 组件卡片：Spring Data R2DBC 是什么、版本现状、能做什么、与深度体系如何衔接——本系列是"组件汇总"速查型文档，关系型数据库底层的深挖见 [JDBC 深度体系](../../../../01-关系型数据库/JDBC/00-JDBC知识体系总览.md)、[MySQL 事务与锁](../../../../01-关系型数据库/MySQL/03-事务与锁机制.md)

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

**Spring Data R2DBC 是 Spring Data 家族中面向关系型数据库的响应式（非阻塞）数据访问组件**——用 R2DBC 规范替代 JDBC 的阻塞模型，配合 WebFlux 让"请求 → SQL → 响应"全链路不占线程，是 Java 生态高并发 IO 场景访问 MySQL/PostgreSQL 的标准姿势。

```text
核心心智模型：
  WebFlux 请求（非阻塞，1 线程扛万并发）
    ↓
  Controller（Mono/Flux）
    ↓
  R2dbcRepository（派生方法 / @Query）/ R2dbcEntityTemplate
    ↓
  DatabaseClient（SQL 直写 / Criteria 类型安全）
    ↓
  R2DBC 驱动（r2dbc-mysql / r2dbc-postgresql / r2dbc-h2，基于 Netty 非阻塞协议）
    ↓
  MySQL / PostgreSQL / H2
```

| 维度 | 说明 |
|------|------|
| 所属 | Spring Data 家族（spring-data-r2dbc，与 spring-data-jdbc 同属 Relational 项目） |
| 版本线 | 2026.0 发行列车 → 4.1.x（2026-08 当前线） |
| 规范基础 | R2DBC SPI 1.0.0.RELEASE（驱动间通用协议 + TCK） |
| 驱动生态 | r2dbc-mysql（asyncer，1.4.2）、r2dbc-postgresql、r2dbc-h2 |
| 配套 | Spring Framework 7.0.x / Spring Boot 4.1.x / WebFlux / Reactor |
| 定位 | **关系型数据库的响应式数据访问层**：聚合根映射、Repository、DatabaseClient、SQL 直写 |

### 1.1 Spring Data R2DBC 解决什么问题

R2DBC 驱动本身只提供"非阻塞的 SQL 执行"，业务层仍要面对：**样板 CRUD**、**POJO ↔ 行映射**、**SQL 字符串拼接**。Spring Data R2DBC 在驱动之上提供：Repository 抽象（派生方法）、实体映射（@Table/@Id）、编程式模板（R2dbcEntityTemplate）与 DatabaseClient（SQL 直写）。

| 维度 | 原生 R2DBC 驱动 | Spring Data R2DBC |
|------|-----------------|-------------------|
| 基础 CRUD | 手写 SQL + 映射 | 继承 `R2dbcRepository` 免费获得 |
| 查询表达 | SQL 字符串手拼 | 方法名派生、@Query、Criteria 类型安全 |
| 结果映射 | 手写 RowMapper | 自动映射（R2dbcEntityTemplate 约定） |
| 与 Spring 生态 | 无集成 | Repository、审计、事件、Boot 自动配置 |

> 🎯 判断标准一句话：**"你的服务是高并发 IO 密集、需要全链路响应式（WebFlux）吗？"**——是，用 Spring Data R2DBC；传统 MVC + 阻塞 JDBC 能扛住的业务，用 [Spring Data JPA](../../Spring%20Data%20系列【数据访问层】/Spring%20Data%20JPA/00-Spring%20Data%20JPA组件总览.md) 更简单——**R2DBC 是"高并发专用武器"，不是默认选项**。

### 1.2 适用与不适用场景

| 场景 | 适合？ | 说明 |
|------|:---:|------|
| WebFlux 全链路响应式服务 | ✅ | 非阻塞从 HTTP 到 SQL 全程（[08 篇](08-WebFlux响应式集成速查.md)） |
| 高并发 IO 密集（网关、聚合服务、实时推送） | ✅ | 不占线程，线程数需求降低一个量级 |
| 大数据量流式处理（SQL 流） | ✅ | Flux + 背压天然流式（[07 篇](07-性能优化与批量速查.md)） |
| 常规业务 CRUD（并发不高） | ⚠️ 可以但没必要 | JPA 更省事（缓存/懒加载/关联） |
| 复杂关联对象图 | ❌ | **无关联映射**（聚合根模型，关联查询手写 SQL） |
| 强事务 + 复杂传播 | ⚠️ 部分 | 支持 @Transactional，但响应式场景有线程传播注意（[06 篇](06-响应式事务与并发控制速查.md)） |

> ⚠️ **最大认知误区**：把 R2DBC 当"更快的 JDBC"用——性能提升来自**并发模型**（不占线程），不是单条 SQL 更快；单条 SQL 它通常比 JDBC **略慢**（协议开销）。**响应式的前提是整条链路都非阻塞**（连接池、中间件、第三方调用都要 reactive），否则只换数据层没意义（[08 篇](08-WebFlux响应式集成速查.md) 有"阻塞陷阱"专节）。

### 1.3 与其他 Spring Data 组件的定位差异

| 组件 | 存储 | 并发模型 | 数据模型 | 本系列的差异点 |
|------|------|---------|---------|--------------|
| Spring Data JPA | 关系型 | 阻塞（JDBC） | 表/实体/关联 | 有缓存、懒加载、脏检查、关联映射 |
| **Spring Data R2DBC** | **关系型** | **非阻塞（R2DBC）** | **表/聚合根（无关联）** | **无缓存/无懒加载/无脏检查；SQL 显式** |
| Spring Data MongoDB | MongoDB | 双模 | 文档 | 无 SQL、无事务弱、无关联 |
| Spring Data Elasticsearch | ES 9 | 双模 | 文档+向量 | 无 SQL、无事务 |
| Spring Data Redis | Redis | 双模 | K/V | 无查询 DSL |

> 💡 记忆抓手：**R2DBC = "JDBC 的响应式 + JPA 的简化（聚合根）"**——会 JPA 的开发者上手很快（Repository 语法同源），但必须放下三样东西：一级缓存、懒加载、关联导航（`order.getItems()` 这种直接不行，要手写 join SQL）。

## 2. 版本现状（2026-08）

| 发行列车 | Spring Data R2DBC | r2dbc-spi | Spring Boot | 状态 |
|----------|------------------|-----------|-------------|------|
| **2026.0** | **4.1.x（4.1.0，2026 GA）** | **1.0.0.RELEASE** | **4.1.x** | **Current（当前线）** |
| 2025.1 | 4.0.x | 1.0.0.RELEASE | 4.0.x | Stable |
| 2025.0 | 3.5.x | 1.0.0.RELEASE | 3.5.x | 停止维护 |
| 2024.1 | 3.4.x | 1.0.0.RELEASE | 3.4.x | 停止维护 |
| 2021.2 | 1.5.x | 0.9.x | 2.7.x | EOL |

> ⚠️ **版本策略（2026 起）**：R2DBC 规范在 2020 年 12 月出 1.0（GA）后协议稳定，**新项目直接 Boot 4.1 + SDR 4.1 + r2dbc-spi 1.0**；驱动版本与 Boot 对齐（**r2dbc-mysql 用 `io.asyncer:r2dbc-mysql:1.4.2`**——`dev.miku:r2dbc-mysql` 已归档停更，别用旧坐标，见 [01 篇](01-模块清单.md)）。

### 2.1 4.x 线关键变化（3.5 → 4.0 迁移要点）

| 变化 | 说明 |
|------|------|
| Java 21 + Spring Framework 7 | 与全家族同步（Boot 4 硬性要求） |
| **标识符默认带引号** | 4.0 起表名/列名默认按"带引号标识符"处理（大写/保留字不再有歧义；依赖 DB 大小写敏感的表需审查） |
| 复合主键 / 嵌入实体 | 4.0 起 @IdClass 与 @Embedded 支持完善 |
| AOT Repository 支持 | 构建期 Repository 元数据（GraalVM Native 可用） |
| JSpecify 空值注解 | 全家族统一 |
| 类型安全属性路径 | 4.1 新特性：Criteria/Update 支持类型安全路径（`PropertyPath.of(Person::getName)`） |
| **聚合根单语句 upsert** | 4.1 新特性：聚合根 upsert 一条 SQL 完成 |

### 2.2 关键演进时间线

| 版本 | 里程碑 |
|------|--------|
| 0.x（2018-2020） | R2DBC 规范诞生（r2dbc-spi 0.8） |
| **1.0（2020-12）** | R2DBC GA：协议稳定，驱动生态成型 |
| 1.5（2021.2 列车） | Spring Data R2DBC 成熟（Boot 2.7 线） |
| 3.x（Boot 3 时代） | jakarta 迁移、@Transactional 响应式支持完善 |
| 4.0（2025.1） | Boot 4：引号标识符、复合主键/嵌入、AOT |
| 4.1（2026.0） | 类型安全属性路径、聚合根 upsert；当前主线 |

## 3. 能力地图

| 能力域 | 能力点 | 对应注解/API |
|--------|--------|-------------|
| 实体映射 | 表/列/主键映射（聚合根） | `@Table`、`@Id`、`@Column`、`@Embedded`、`@Version` |
| 仓库抽象 | 声明式响应式数据访问 | `R2dbcRepository`、`ReactiveCrudRepository`、`ReactiveSortingRepository` |
| 派生查询 | 方法名生成 SQL | `findByNameAndPrice`、`findTop5By...`、`countBy...` |
| 自定义查询 | SQL 直写 | `@Query`（占位符）、`@Modifying` |
| 模板操作 | 编程式入口 | `R2dbcEntityTemplate`（select/insert/update/delete） |
| SQL 直写 | 底层编程式 | `DatabaseClient`（bind/rowMapper/execute） |
| 类型安全查询 | Criteria API | `Criteria.where(PropertyPath)`、`Update`（4.1 类型安全） |
| 事务 | 响应式声明式事务 | `@Transactional` + `R2dbcTransactionManager` |
| 并发控制 | 乐观锁 | `@Version`（同 JPA 语义） |
| 流式/批量 | 大数据量流式处理 | `Flux<T>`、`insertAll`、批处理 |
| 辅助能力 | 审计、审计器、QBE | `@CreatedDate`、`QueryByExampleExecutor`、`AuditorAware` |

### 3.1 能力边界：Spring Data R2DBC 不做什么

| 能力边界 | 谁来做 | 说明 |
|---------|--------|------|
| 关联映射/懒加载/一级缓存 | **不做**（设计取舍） | 聚合根模型：关联数据用 join SQL 或分次查询（[03 篇](03-聚合根映射速查.md)） |
| 脏检查/自动 UPDATE | **不做** | 修改必须显式 UPDATE（无 JPA 的"改属性自动 flush"） |
| 连接池 | r2dbc-pool（Boot 自动） | 与 HikariCP 对应的响应式池 |
| 事务的 ACID 物理实现 | 数据库 | 同 JPA（[MySQL 事务与锁](../../../../01-关系型数据库/MySQL/03-事务与锁机制.md)） |
| 分布式事务 | Seata / 本地消息表 | @Transactional 只覆盖单数据源 |
| 复杂 SQL 优化 | 数据库 + 手写 SQL | R2DBC 反而更适合手写 SQL（无 ORM 生成层的干扰） |

> ⚠️ **常见归因错误**：R2DBC 查询慢怪"响应式没用"——单条 SQL 响应式不比 JDBC 快（协议开销略高）；性能价值在**并发模型**（1000 并发下线程占用少一个量级）；排查应看"链路是否全非阻塞 + SQL 是否走索引"，不是看单查询耗时。

## 4. 与深度体系的映射

| 速查文档 | 深度体系模块 |
|---------|-------------|
| 02-快速开始与连接配置速查 | [JDBC-00 总览](../../../../01-关系型数据库/JDBC/00-JDBC知识体系总览.md)（连接/驱动体系对照）、[数据库连接池系列](../../../../01-关系型数据库/数据库连接池/) |
| 03-聚合根映射速查 | [JDBC-03 ResultSet 与数据读取](../../../../01-关系型数据库/JDBC/03-ResultSet与数据读取.md)（行映射语义） |
| 06-事务与并发控制速查 | [JDBC-04 事务管理与隔离](../../../../01-关系型数据库/JDBC/04-事务管理与隔离.md)、[MySQL 事务与锁](../../../../01-关系型数据库/MySQL/03-事务与锁机制.md) |
| 07-性能优化与批量速查 | [MySQL 索引原理](../../../../01-关系型数据库/MySQL/04-索引原理与设计.md)、[SQL调优系列](../../../../01-关系型数据库/SQL调优/) |
| 09-集成地图与常见问题 | [Spring Data JPA 系列](../../Spring%20Data%20系列【数据访问层】/Spring%20Data%20JPA/00-Spring%20Data%20JPA组件总览.md)（双写/迁移对照） |

> 💡 分工约定：**速查页回答"API 怎么写"，深度页回答"数据库/JDBC 怎么工作"**——SQL 执行计划、事务隔离实现、索引原理在 MySQL/JDBC 系列；本系列聚焦"响应式模型 + 聚合根映射 + Repository 编排"。

## 5. 快速上手 3 步

**① 引入依赖**（Boot 4.1 + MySQL 8）：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-r2dbc</artifactId>
</dependency>
<dependency>
    <groupId>io.asyncer</groupId>
    <artifactId>r2dbc-mysql</artifactId>        <!-- 版本由 BOM 管理 -->
</dependency>
```

**② 配置连接**（application.yml）：

```yaml
spring:
  r2dbc:
    url: r2dbc:mysql://localhost:3306/shop      # ★ R2DBC URL 前缀，不是 jdbc:
    username: root
    password: root
    pool:
      enabled: true                             # 响应式连接池（r2dbc-pool）
      max-size: 20
      initial-size: 5
```

**③ 声明实体 + Repository + 使用**：

```java
@Table("product")
public record Product(
        @Id Long id,                            // 自增主键
        String name,
        BigDecimal price) {}

public interface ProductRepository extends R2dbcRepository<Product, Long> {
    Flux<Product> findByNameContaining(String keyword);       // 派生查询 → LIKE
    Mono<Product> findFirstByOrderByPriceDesc();
}

// 使用（WebFlux 控制器直接返回响应式类型）
@GetMapping("/products")
public Flux<Product> search(@RequestParam String kw) {
    return productRepository.findByNameContaining(kw);        // 非阻塞，不占线程
}
```

### 5.1 快速上手补充：H2 零配置联调

```xml
<!-- 本地开发/测试：H2 响应式驱动，零配置 -->
<dependency>
    <groupId>io.r2dbc</groupId>
    <artifactId>r2dbc-h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

```yaml
spring:
  r2dbc:
    url: r2dbc:h2:mem:///shop?options=DB_CLOSE_DELAY=-1
```

> 💡 注意与 JDBC 的差异：**R2DBC 连接串前缀是 `r2dbc:`**（`r2dbc:mysql:` / `r2dbc:postgresql:` / `r2dbc:h2:`），不是 `jdbc:`；配错前缀启动直接报"Unknown driver"。

### 5.2 三步验证集成真的通了

| 步骤 | 做法 | 预期 |
|------|------|------|
| ① 看启动日志 | 启动无连接报错 | 连接池初始化成功 |
| ② 写读验证 | save 一条 → findById（block 或测试里 stepVerifier） | 返回同一实体，id 已回填 |
| ③ 控制台核对 | mysql 客户端查 `select * from product` | 行存在，列与实体一致 |

> 💡 排障起点：**先确认 R2DBC URL 前缀与驱动坐标**——`NoSuchBeanDefinitionException: ConnectionFactory` 或 `Unknown driver` 一般是 starter 没引全/URL 写错；数据库层问题同 JDBC 排查路径（[JDBC-00](../../../../01-关系型数据库/JDBC/00-JDBC知识体系总览.md)）。

## 6. 速查导航

| 文档 | 内容 |
|------|------|
| [01-模块清单](01-模块清单.md) | artifact 坐标、驱动生态、包结构、依赖边界与职责切割 |
| [02-快速开始与连接配置速查](02-快速开始与连接配置速查.md) | starter、R2DBC URL、连接池、Boot 属性、事务管理器、多数据源 |
| [03-聚合根映射速查](03-聚合根映射速查.md) | @Table/@Id/@Column、@Embedded、复合主键、@Version、无关联导航、审计、转换器 |
| [04-Repository 速查](04-Repository速查.md) | 仓库层次、派生方法、@Query/@Modifying、分页、QBE、4.x 行为变化 |
| [05-查询构造与 DatabaseClient 速查](05-查询构造与DatabaseClient速查.md) | DatabaseClient、R2dbcEntityTemplate、Criteria/Update 类型安全、SQL 直写、bind |
| [06-响应式事务与并发控制速查](06-响应式事务与并发控制速查.md) | 响应式事务传播、R2dbcTransactionManager、隔离级别、乐观锁、连接传播注意 |
| [07-性能优化与批量速查](07-性能优化与批量速查.md) | 批量写入、流式读取、无缓存下的优化、背压、WebFlux 组合 |
| [08-WebFlux 响应式集成速查](08-WebFlux响应式集成速查.md) | 全链路非阻塞、阻塞陷阱、超时/熔断联动、响应式测试 |
| [09-集成地图与常见问题](09-集成地图与常见问题.md) | 与全家桶联动、JPA 双写、3.5→4.1 迁移清单、高频坑排错 |

### 6.1 阅读顺序建议

- **第一次接触**：03 → 04 → 05，先掌握"聚合根、仓库、DatabaseClient"三件套；
- **项目实战**：02（连接）→ 03（映射）→ 04（Repository）→ 05（SQL 直写）→ 08（WebFlux 集成）→ 09（避坑）；
- **准备面试**：06（响应式事务）→ 07（无缓存如何优化）→ 01（R2DBC vs JDBC vs JPA）→ 08（阻塞陷阱）；
- **从 JPA 迁移**：09 篇迁移对照 + 03 篇"放下三样东西"；
- **源码学习**：01 篇包结构 + 05 篇 DatabaseClient 执行链路。

### 6.2 前置知识建议

| 前置主题 | 所在文档 | 与本系列的关系 |
|---------|---------|--------------|
| SQL 基础 | [SQL调优系列](../../../../01-关系型数据库/SQL调优/) | R2DBC 大量手写 SQL，SQL 能力是硬前提 |
| JDBC 概念 | [JDBC-00](../../../../01-关系型数据库/JDBC/00-JDBC知识体系总览.md) | 事务/连接/驱动体系的对照基础 |
| Reactor 基础 | WebFlux 系列（SpringMVC 目录） | Mono/Flux/背压是响应式语法前提 |
| Spring Data 通用模型 | Spring Data JPA 系列（同级目录） | Repository/审计同源，差异在响应式类型 |

## 7. 学习路线推荐

| 路线 | 适用 | 顺序 |
|------|------|------|
| 入门速查 | 会 WebFlux、想通数据层 | 00 总览 → 02 连接 → 03 映射 → 04 Repository |
| 项目实践 | 上生产做响应式服务 | 03 → 04 → 05 → 08 WebFlux → 09 避坑 |
| 面试冲刺 | 全考点 | 06 事务 → 07 性能 → 01 三组件对比 → 08 阻塞陷阱 |
| JPA 迁移 | 存量改造 | 09 迁移对照 → 03 聚合根 → 05 SQL 直写 |

## 8. 核心概念速查

| 术语 | 一句话解释 |
|------|-----------|
| R2DBC | Reactive Relational Database Connectivity：关系库的响应式规范（r2dbc-spi 1.0） |
| R2dbcRepository | 响应式仓库接口（返回 Mono/Flux） |
| DatabaseClient | SQL 直写入口（bind 参数 + RowMapper 映射） |
| R2dbcEntityTemplate | 实体级编程式模板（select/insert/update/delete） |
| 聚合根 | 实体边界模型：无关联导航，一个实体 + 其值对象 |
| @Table / @Id / @Column | 表/主键/列映射（与 JPA 同名同义） |
| @Embedded | 嵌入值对象（Address 等，同 JPA） |
| @Version | 乐观锁版本列（CAS 更新） |
| @Transactional | 声明式事务（R2dbcTransactionManager） |
| r2dbc-pool | 响应式连接池（对应 HikariCP） |
| Mono / Flux | 0-1 个 / 0-N 个结果的响应式类型 |
| 背压 | 消费者声明处理能力，生产者按需发（[07 篇](07-性能优化与批量速查.md)） |
| 阻塞陷阱 | 响应式链路里混入阻塞调用（sleep/同步IO）导致线程占满（[08 篇](08-WebFlux响应式集成速查.md)） |

---

**下一模块**：[01-模块清单](01-模块清单.md)　**返回总览**：本页

**【参考来源】**：[Spring Data 2026.0.0-M1 released（官方博客）](https://spring.io/blog/2026/02/13/spring-data-2026-0-0-m1-released)、[Spring Data 2026.0.0 GA（thenote）](https://thenote.app/post/en/spring-data-2026-0-0-generally-available-w6vcearl6t)、[Spring Data R2DBC 官方文档（4.1.x）](https://docs.spring.io/spring-data/r2dbc/docs/4.1.x/api/index.html)、[R2DBC 官方驱动列表](https://r2dbc.io/drivers/)、[r2dbc-mysql（asyncer，官方继任者）](https://github.com/asyncer-io/r2dbc-mysql)
