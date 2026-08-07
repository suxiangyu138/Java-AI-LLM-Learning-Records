# 00 Spring JDBC 组件总览

> 组件卡片：spring-jdbc 是什么、版本现状、能做什么、与深度体系如何衔接——关系型数据访问的地基

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

**spring-jdbc 是把"JDBC 裸 API"包装成可用模板层的数据访问模块**——JdbcTemplate 消除样板代码（连接获取/释放、异常转换、参数绑定），JdbcClient 提供流式 fluent API（6.1 起），同时提供 DataSource 抽象、批量操作与存储过程调用；**一切 ORM（JPA/MyBatis 等）的数据源管理与事务底层仍是它**。

```text
核心心智模型：
  裸 JDBC 的痛点：Connection/Statement/ResultSet 手工管理、SQLException 到处抛、
                 参数绑定繁琐、连接泄漏常见
                    ↓
  spring-jdbc 的答案：
    DataSource（连接来源抽象）→ JdbcTemplate/JdbcClient（模板执行）
    → SQLExceptionTranslator（异常翻译为 DataAccessException 体系）
    → DataSourceTransactionManager（与 spring-tx 接轨）

  一句话：把"连-执-收"三件事做成模板，把 SQLException 变成可分类处理的异常体系。
```

| 维度 | 说明 |
|------|------|
| 所属 | Spring Framework 底层核心模块（spring-jdbc） |
| 版本线 | 随 Spring Framework 7.x（2025-11 起，Jakarta EE 11 基线） |
| 语言要求 | Java 17+ |
| 定位 | JDBC 模板化 + 数据源抽象 + 异常映射（ORM 的数据地基） |

### 1.1 能力边界：spring-jdbc 不做什么

理解"它不做什么"和"它做什么"同样重要——这决定了选型时不会用错工具：

| 不做的事 | 交给谁 | 原因 |
|---------|--------|------|
| 连接池 | HikariCP / Druid | spring-jdbc 只定义 `DataSource` 抽象，池化是外部实现 |
| ORM 实体管理 | JPA / MyBatis | 模板层只做"行 ↔ 对象"轻量映射，不做实体生命周期 |
| 分布式事务 | Seata / XA 中间件 | `DataSourceTransactionManager` 只管单数据源本地事务 |
| 动态 SQL 生成 | MyBatis / QueryDSL | 模板层要求自己写 SQL |
| 数据库迁移 | Flyway / Liquibase | `ResourceDatabasePopulator` 只能执行脚本，无版本管理 |

| 维度 | 裸 JDBC | spring-jdbc | MyBatis | JPA |
|------|---------|-------------|---------|-----|
| 样板代码量 | 高（连-执-收全手工） | 中低（模板封装） | 中（XML/注解配置） | 低（自动生成） |
| SQL 可控性 | 100% | 100% | 高（动态 SQL） | 低（JPQL 抽象） |
| 学习成本 | 低 | 低 | 中 | 高（映射规则多） |
| 性能调优空间 | 最大 | 大 | 大 | 中（缓存需谨慎） |
| 适合场景 | 工具类/脚本 | 轻量数据访问、批量 | 复杂查询、报表 | 领域模型驱动 |

> 💡 **定位金句**：spring-jdbc 处在"裸 JDBC"与"全功能 ORM"的中间层——比裸 JDBC 少 80% 样板代码，比 ORM 多 100% 的 SQL 可控性。

## 2. 版本现状（2026-08）

| 版本 | 说明 |
|------|------|
| Spring Framework 7.0（2025-11） | 当前主线（Boot 4.0 配套） |
| Spring Framework 7.1（2026-05） | 最新维护线（Boot 4.1 配套） |
| Spring Framework 6.2.x | 存量主线（Boot 3.x 配套；JdbcClient 自 6.1 起可用） |

**7.x jdbc 关键变化：**

- **JdbcClient "回炉"增强（7.0）**：新增**语句级配置**——`fetchSize`、`maxRows`、`queryTimeout` 可按每条语句设置（#35155）；`maxRows` 达到后丢弃后续行（#34709）；泛型空性细化（#34911）；
- **`NamedParameterJdbcTemplate` 标记废弃**：7.0 起建议直接注入 `JdbcTemplate` 或 `JdbcClient`（命名参数能力已并入 JdbcClient）；
- **`DaoSupport` 系列废弃**（#35145）：`JdbcDaoSupport` 等模板基类不再推荐，改为组合 `JdbcTemplate`/`JdbcClient`；
- **Kotlin 扩展修正**（#35846）：`queryForObject(sql, Array)` / `queryForList(sql, Array)` 改为 vararg 版本，与 Java API 对齐（Kotlin 代码需调整）；
- **ORM 侧迁移**：Hibernate 原生支持（`HibernateTransactionManager`）随 JPA 3.2/Hibernate 7 迁移到 `org.springframework.orm.jpa.hibernate`（spring-orm 模块，事务相关内容见 [Spring-TX-01](../Spring‑TX（Spring‑Transaction）/01-模块清单.md)）。

> ⚠️ **要点**：7.0 的 jdbc 变化核心是"**收口到 JdbcClient**"——新代码直接用 JdbcClient，存量 NamedParameterJdbcTemplate 代码可平滑迁移（API 几乎一一对应）；JdbcTemplate 本身稳定不变。

### 2.1 从 6.x 迁移到 7.0 的落地清单

| 迁移项 | 6.x 写法 | 7.0 写法 |
|--------|---------|---------|
| 命名参数查询 | `namedJdbcTemplate.queryForList(sql, params, Order.class)` | `jdbcClient.sql(sql).params(params).query(Order.class).list()` |
| 语句级参数控制 | 全局 `jdbcTemplate.setFetchSize(...)` | `.fetchSize(500).maxRows(100_000).queryTimeout(30)` |
| DAO 基类 | `extends JdbcDaoSupport` | 构造器注入 `JdbcClient`（组合优于继承） |
| Kotlin 调用 | `queryForObject(sql, arrayOf(...))` | vararg 版本（`sql, *args`） |
| Hibernate 事务管理器 | `HibernateTransactionManager`（hibernate5 包） | `org.springframework.orm.jpa.hibernate`（JPA 3.2） |

迁移优先级建议：**先迁移 NamedParameterJdbcTemplate → JdbcClient**（改动最小、收益最大），`DaoSupport` 存量代码可留到下次重构再动——7.0 只是标记 deprecation，并非立即删除。

> 💡 判断迁移成本：JdbcClient 与 NamedParameterJdbcTemplate 的方法几乎一一对应（对照表见 [03-JdbcClient 速查](03-JdbcClient速查.md) 第 6 节），中等项目的迁移量通常在一个工作日内完成，且属于纯语法级改动——事务与异常行为完全不变。

### 2.2 版本选型建议

| 项目现状 | 推荐版本 | 理由 |
|---------|---------|------|
| 新项目（2026 年起） | Spring Framework 7.0+ / Boot 4.0+ | JdbcClient 语句级配置、命名参数收编、JDK 17+ 基线 |
| 存量 Boot 3.x 项目 | 6.2.x 维护线 | 不升级也能用 JdbcClient（6.1+），无迁移压力 |
| 需要长期维护线 | 7.1.x（2026-05 起） | 跟随 Boot 4.1 的补丁节奏 |

选型决策不是"越新越好"：7.x 的 jdbc 变化集中在 API 收口（NamedParameterJdbcTemplate 与 DaoSupport 废弃），如果团队存量代码量大，可以先停留在 6.2 维护线，把"新代码用 JdbcClient"作为增量约束逐步推进；只有新项目或强依赖语句级配置时才需要立即升级 7.x。反过来看，升级 7.x 的 jdbc 成本其实很低——核心 API 稳定，迁移是语法级改动（见 2.1 清单），真正的升级成本在 Boot/JDK 层面而非 jdbc 本身。

补充：7.x 与 6.2 的选择还有一个隐性因素——Jakarta EE 基线。7.0 切到 Jakarta EE 11，依赖它的 Servlet 容器（Tomcat 11）与 JDK 17+ 要求会连锁影响整个技术栈；如果中间件生态还没跟上，6.2 维护线反而是更稳的选择。选型时把"框架自身能力"与"周边生态成熟度"分开评估，是资深工程师与新手的分水岭。

## 3. 能力地图

| 能力域 | 能力点 | 关键类/API |
|--------|--------|-----------|
| 模板执行 | 通用 SQL 执行 | `JdbcTemplate`（execute/update/query/batchUpdate） |
| 命名参数 | 具名占位符 | `NamedParameterJdbcTemplate`（7.0 起废弃）、JdbcClient 内建 |
| 流式 API | Fluent 查询（推荐） | `JdbcClient`（6.1+，7.0 语句级配置） |
| 结果映射 | 行 → 对象 | `RowMapper`、`BeanPropertyRowMapper`、`ColumnMapRowMapper` |
| 数据源 | 连接来源抽象 | `DataSource`（javax.sql）、`DriverManagerDataSource`、连接池集成 |
| 批量 | 批量执行 | `batchUpdate`、`BatchPreparedStatementSetter` |
| 存储过程 | 调用数据库过程 | `SimpleJdbcCall`、`SimpleJdbcInsert` |
| 异常体系 | 统一异常翻译 | `DataAccessException` 族、`SQLExceptionTranslator` |
| 事务接轨 | JDBC 事务管理器 | `DataSourceTransactionManager`（见 Spring-TX 系列） |
| 对象映射（旧） | 一列一字段映射 | `BeanPropertyRowMapper`（保留） |

### 3.1 能力地图的阅读方式：一个查询的完整链路

能力地图的 5 个能力域恰好对应 JDBC 编程的 5 个生命周期问题：

| 编程问题 | 能力域 | 代表 API |
|---------|--------|---------|
| SQL 怎么写 | 模板执行 / 流式 API | `JdbcTemplate`、`JdbcClient` |
| 结果怎么拿 | 结果映射 | `RowMapper`、`BeanPropertyRowMapper` |
| 连接从哪来 | 数据源 | `DataSource`、HikariCP |
| 出错怎么办 | 异常体系 | `SQLExceptionTranslator` |
| 多语句怎么成组 | 事务接轨 | `DataSourceTransactionManager` |

> 💡 面试时把"一次 JdbcTemplate 查询的完整链路"讲下来（SQL 传入 → 参数绑定 → `DataSourceUtils` 取连接（先查事务绑定）→ 执行 → RowMapper 映射 → 异常翻译 → `DataSourceUtils` 释放连接），就等于把整张能力地图串起来了——这是数据访问层面试的"万能主线"。

### 3.2 能力地图之外：容易被忽略的能力点

| 隐藏能力 | 说明 | 所在包 |
|---------|------|--------|
| `ResourceDatabasePopulator` | 执行 SQL 脚本（DDL/初始化数据），Boot 的 schema.sql 初始化就是它 | datasource.init |
| `EmbeddedDatabaseBuilder` | 一行代码起 H2/HSQLDB/Derby 内存库 | datasource.embedded |
| `SqlRowSet` | 可滚动的离线结果集（不持有连接） | core |
| `SimpleJdbcInsert` | 自动生成 INSERT 语句 + 主键回填 | core |
| `BeanPropertyRowMapper` | 列名 ↔ 属性名自动映射（含下划线转驼峰） | core |

这些能力面试很少直接问，但生产代码里出现频率极高——比如"测试怎么起内存库""启动时怎么执行初始化脚本"这类问题，答案都在 spring-jdbc 内部而非外部工具；读文档时不要只盯着 JdbcTemplate 的查询 API，这些"周边能力"往往是工程落地时的关键拼图。

## 4. 与深度体系的映射

| 速查文档 | 深度体系模块 |
|---------|-------------|
| 02-JdbcTemplate 速查 | [Spring全家桶-06-Spring-Data数据访问层](../../../Spring全家桶/06-Spring-Data数据访问层.md) |
| 04-DataSource 与连接池速查 | [SpringBoot-04-SpringBoot数据访问](../../../SpringBoot/04-SpringBoot数据访问.md) |
| 06-异常体系与错误映射速查 | [Spring框架核心-06-声明式事务管理（异常与回滚）](../../../Spring框架核心/06-声明式事务管理.md) |
| 07-与事务/ORM 集成速查 | [Spring生态深度剖析-04-AOP代理创建与事务管理内核](../../../Spring生态深度剖析/04-AOP代理创建与事务管理内核.md) |
| 08-集成地图与常见问题 | [Spring全家桶-06-Spring-Data数据访问层](../../../Spring全家桶/06-Spring-Data数据访问层.md) |

> 💡 本系列定位"查得快"——JDBC 的 ORM 生态（JPA/MyBatisPlus）与数据库细节（MySQL）见 Spring Data 系列与关系型数据库系列；事务纵深见 Spring-TX 系列。

### 4.1 阅读策略：速查与深度的分工

| 目标 | 速查文档 | 深度体系 |
|------|---------|---------|
| 快速上手写代码 | 02 / 03 / 05（API 与可运行示例） | - |
| 理解事务传播与回滚 | 07（接轨概览） | [Spring-TX 系列](../Spring‑TX（Spring‑Transaction）/00-Spring TX组件总览.md) |
| 面试深挖连接池 / 翻译器 | 04 / 06（含源码级小节） | Spring 框架核心系列 |
| 数据层整体选型 | 08（集成地图） | [Spring全家桶-06-Spring-Data数据访问层](../../../Spring全家桶/06-Spring-Data数据访问层.md) |

> 💡 本系列每篇的"面试官追问"小节就是速查 → 深度体系之间的"桥"——先照着追问自测，答不出的知识点再去深度体系里补，比从头通读效率高得多。

## 5. 快速上手 3 步

**① 引入依赖**（Boot 4 用 spring-boot-starter-jdbc 自动配置数据源与模板）：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
```

**② 使用 JdbcClient 查询**（7.0 推荐风格）：

```java
@Repository
public class OrderDao {
    private final JdbcClient jdbcClient;

    public OrderDao(JdbcClient jdbcClient) { this.jdbcClient = jdbcClient; }

    public List<Order> findAbove(BigDecimal min) {
        return jdbcClient.sql("SELECT * FROM t_order WHERE amount > ?")
                .param(min)
                .query(Order.class)          // 自动映射（字段名 ↔ 列名）
                .list();
    }

    public Optional<Order> findById(Long id) {
        return jdbcClient.sql("SELECT * FROM t_order WHERE id = ?")
                .param(id)
                .query(Order.class)
                .optional();
    }
}
```

**③ 更新与批量**：

```java
int updated = jdbcClient.sql("UPDATE t_order SET status = ? WHERE id = ?")
        .params("PAID", 1L)
        .update();

jdbcTemplate.batchUpdate(
        "INSERT INTO t_log(user_id, msg) VALUES (?, ?)",
        logs, 500,                     // 每 500 条一批
        (ps, log) -> { ps.setLong(1, log.userId()); ps.setString(2, log.msg()); });
```

### 5.1 完整最小可运行示例（Boot 4 + JdbcClient + H2）

把 3 步串成一个可运行的最小工程，便于对照实际启动：

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:h2:mem:app;MODE=MySQL
    driver-class-name: org.h2.Driver
    username: sa
  sql:
    init:
      mode: always          # 启动时执行 schema.sql
```

```java
// src/main/resources/schema.sql
CREATE TABLE t_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL
);
```

```java
@RestController
public class OrderController {
    private final JdbcClient jdbc;

    public OrderController(JdbcClient jdbc) { this.jdbc = jdbc; }

    @PostMapping("/orders")
    public Long create(@RequestBody OrderCmd cmd) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO t_order(user_id, amount, status) " +
                 "VALUES (:userId, :amount, 'CREATED')")
                .param("userId", cmd.userId())
                .param("amount", cmd.amount())
                .update(kh);                        // 自增主键回填
        return kh.getKey().longValue();
    }

    @GetMapping("/orders")
    public List<Order> list() {
        return jdbc.sql("SELECT * FROM t_order ORDER BY id DESC")
                .query(Order.class)
                .list();
    }
}
```

> ⚠️ 示例用 H2 内存库保证"零外部依赖可跑"；切到 MySQL 只需改 URL 与驱动依赖——业务代码零改动，这正是 `DataSource` 抽象的价值（[04-DataSource 与连接池速查](04-DataSource与连接池速查.md)）。

### 5.2 快速上手阶段的常见报错

| 报错 | 原因 | 处理 |
|------|------|------|
| `CannotGetJdbcConnectionException` | 数据源没配好 / 驱动缺失 / URL 错误 | 检查 spring.datasource.* 与驱动依赖 |
| `BadSqlGrammarException` | SQL 语法错误或表不存在 | 开 DEBUG 日志看实际执行的 SQL |
| `EmptyResultDataAccessException` | queryForObject 无结果 | 换成 optional() / list() 形态 |
| `IncorrectResultSizeDataAccessException` | 期望单行但查到多行 | 检查条件唯一性，或改 list() |
| `Cannot infer the SQL type` | 参数类型无法推断（如 LocalDateTime + 老驱动） | 显式转 Timestamp 或声明类型 |

> 💡 上手阶段的报错 90% 是"配置问题"和"API 语义问题"两类——先检查数据源配置，再核对查询方法语义（单数形态抛异常、复数形态不抛），基本能覆盖大部分报错；具体的异常类型与翻译机制见 [06-异常体系与错误映射速查](06-异常体系与错误映射速查.md)。

## 6. 速查导航

| 文档 | 内容 |
|------|------|
| [01-模块清单](01-模块清单.md) | spring-jdbc artifact 与包结构、依赖边界、7.0 废弃清单 |
| [02-JdbcTemplate 速查](02-JdbcTemplate速查.md) | JdbcTemplate/NamedParameterJdbcTemplate 全 API |
| [03-JdbcClient 速查](03-JdbcClient速查.md) | 6.1+ 流式 API，7.0 语句级配置 |
| [04-DataSource 与连接池速查](04-DataSource与连接池速查.md) | 数据源抽象、HikariCP/Druid 集成 |
| [05-批量操作与存储过程速查](05-批量操作与存储过程速查.md) | batchUpdate、SimpleJdbcCall/Insert |
| [06-异常体系与错误映射速查](06-异常体系与错误映射速查.md) | DataAccessException 家族、翻译器 |
| [07-与事务/ORM 集成速查](07-与事务ORM集成速查.md) | DataSourceTransactionManager、JPA/MyBatis 底层 |
| [08-集成地图与常见问题](08-集成地图与常见问题.md) | Boot 数据访问联动 + 高频坑 |

### 6.1 场景式导航：遇到问题先翻哪篇

| 你正在做的事 | 直接打开 |
|-------------|---------|
| 新项目写第一个 DAO | [03-JdbcClient 速查](03-JdbcClient速查.md) |
| 维护存量 NamedParameterJdbcTemplate 代码 | [02-JdbcTemplate 速查](02-JdbcTemplate速查.md) |
| 线上连接池耗尽 / 连接超时 | [04-DataSource 与连接池速查](04-DataSource与连接池速查.md) |
| 导入 10 万行数据太慢 | [05-批量操作与存储过程速查](05-批量操作与存储过程速查.md) |
| 捕获 DuplicateKeyException 做幂等 | [06-异常体系与错误映射速查](06-异常体系与错误映射速查.md) |
| 事务不回滚 / 多数据源 | [07-与事务ORM集成速查](07-与事务ORM集成速查.md) |
| 面试复习 + 整体排查 | [08-集成地图与常见问题](08-集成地图与常见问题.md) |

### 6.2 系列之外：再往深走的方向

| 方向 | 入口 |
|------|------|
| 事务传播 / 隔离 / 回滚规则 | [Spring-TX 系列](../Spring‑TX（Spring‑Transaction）/00-Spring TX组件总览.md) |
| Boot 数据访问自动配置源码 | [SpringBoot-04-SpringBoot数据访问](../../../SpringBoot/04-SpringBoot数据访问.md) |
| MyBatis / JPA 选型与混用 | [Spring全家桶-06-Spring-Data数据访问层](../../../Spring全家桶/06-Spring-Data数据访问层.md) |
| MySQL 底层（索引 / 锁 / 事务） | 关系型数据库系列 |

本系列定位"JDBC 层的速查与收口"——当问题超出"连接、模板、异常、事务接轨"这个范围（比如慢 SQL 优化、分库分表、分布式事务），就应该切换到对应深度体系，而不是在本系列里硬找答案；速查文档的价值是"快速定位与记忆锚点"，深度体系才是完整理解的来源。

## 7. 学习路线推荐

| 路线 | 适用 | 顺序 |
|------|------|------|
| 入门速查 | 快速上手数据访问 | 00 总览 → 03 JdbcClient → 02 JdbcTemplate → 04 DataSource |
| 项目实践 | 生产数据访问 | 04 连接池 → 05 批量 → 06 异常 → 07 事务集成 → 08 常见问题 |
| 面试冲刺 | 数据层考点 | 02 模板原理 → 04 线程绑定 → 06 翻译链 → 08 考点清单 → [SpringBoot-04](../../../SpringBoot/04-SpringBoot数据访问.md) |

## 8. 核心概念速查

| 术语 | 一句话解释 |
|------|-----------|
| JdbcTemplate | 经典 JDBC 模板（execute/update/query） |
| JdbcClient | Fluent 流式 API（6.1+，7.0 语句级配置，推荐） |
| RowMapper | 结果集每行 → 对象的映射器 |
| DataSource | 连接来源抽象（连接池接入点） |
| DataSourceUtils | 连接获取/释放 + 事务线程绑定 |
| DataAccessException | 数据库异常统一体系（翻译产物） |
| SQLExceptionTranslator | SQLException → DataAccessException 翻译器 |
| rewriteBatchedStatements | MySQL 真批量关键参数 |

---

**下一模块**：[01-模块清单](01-模块清单.md)　**返回总览**：本页

**【参考来源】**：[Spring Framework 7.0.0-M7 发布公告（spring.io）](https://spring.io/blog/2025/07/17/spring-framework-7-0-0-M7-available-now)、[Spring Framework 7.0 Release Notes（GitHub Wiki）](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-7.0-Release-Notes)、[spring-jdbc 7.0.x Javadoc 包使用页（docs.spring.io）](https://docs.spring.io/spring-framework/docs/7.0.3-SNAPSHOT/javadoc-api/org/springframework/jdbc/core/support/package-use.html)、[Spring Framework 7.0 正式发布解析（掘金）](https://juejin.cn/post/7573592127298682930)
