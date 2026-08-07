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

## 4. 与深度体系的映射

| 速查文档 | 深度体系模块 |
|---------|-------------|
| 02-JdbcTemplate 速查 | [Spring全家桶-06-Spring-Data数据访问层](../../../Spring全家桶/06-Spring-Data数据访问层.md) |
| 04-DataSource 与连接池速查 | [SpringBoot-04-SpringBoot数据访问](../../../SpringBoot/04-SpringBoot数据访问.md) |
| 06-异常体系与错误映射速查 | [Spring框架核心-06-声明式事务管理（异常与回滚）](../../../Spring框架核心/06-声明式事务管理.md) |
| 07-与事务/ORM 集成速查 | [Spring生态深度剖析-04-AOP代理创建与事务管理内核](../../../Spring生态深度剖析/04-AOP代理创建与事务管理内核.md) |
| 08-集成地图与常见问题 | [Spring全家桶-06-Spring-Data数据访问层](../../../Spring全家桶/06-Spring-Data数据访问层.md) |

> 💡 本系列定位"查得快"——JDBC 的 ORM 生态（JPA/MyBatisPlus）与数据库细节（MySQL）见 Spring Data 系列与关系型数据库系列；事务纵深见 Spring-TX 系列。

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

---

**下一模块**：[01-模块清单](01-模块清单.md)　**返回总览**：本页

**【参考来源】**：[Spring Framework 7.0.0-M7 发布公告（spring.io）](https://spring.io/blog/2025/07/17/spring-framework-7-0-0-M7-available-now)、[Spring Framework 7.0 Release Notes（GitHub Wiki）](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-7.0-Release-Notes)、[spring-jdbc 7.0.x Javadoc 包使用页（docs.spring.io）](https://docs.spring.io/spring-framework/docs/7.0.3-SNAPSHOT/javadoc-api/org/springframework/jdbc/core/support/package-use.html)、[Spring Framework 7.0 正式发布解析（掘金）](https://juejin.cn/post/7573592127298682930)
