# 03 JdbcClient 速查

> Fluent 流式 API（6.1+，7.0 增强）——命名参数、自动映射、语句级配置，当前 JDBC 数据访问的首选

---

## 📚 目录

1. [为什么是 JdbcClient](#1-为什么是-jdbcclient)
2. [查询链速查](#2-查询链速查)
3. [更新与 DDL](#3-更新与-ddl)
4. [结果映射：query 四兄弟](#4-结果映射query-四兄弟)
5. [语句级配置（7.0）](#5-语句级配置70)
6. [从 NamedParameterJdbcTemplate 迁移](#6-从-namedparameterjdbctemplate-迁移)

---

## 1. 为什么是 JdbcClient

| 对比 | JdbcTemplate | JdbcClient |
|------|-------------|------------|
| 风格 | 方法参数堆叠 | **链式 Fluent**：`sql().param().query().list()` |
| 命名参数 | 需套 NamedParameterJdbcTemplate | **内建**（`:name`） |
| 结果映射 | RowMapper 显式传入 | `query(Order.class)` 自动映射 |
| 空结果处理 | queryForObject 抛异常 | `optional()` / `single()` 语义清晰 |
| 语句级配置（7.0） | 全局 setter | **每条链可配 fetchSize/maxRows/queryTimeout** |
| 可变参数 | 显式包装 | `params(Object...)` / `param(Object)` 逐参 |

> 🎯 **核心要点**：JdbcClient 不是新引擎——**同一执行内核的语法糖升级**（内部仍走 JdbcTemplate 逻辑）；它让"查询链路"可读可测，是 6.1+ 官方推荐的数据访问入口。

## 2. 查询链速查

```java
// 注入（Boot 4 自动装配）
@Repository
public class OrderDao {
    private final JdbcClient jdbc;

    public OrderDao(JdbcClient jdbc) { this.jdbc = jdbc; }

    // 基础链：sql → param → query → list
    public List<Order> byStatus(String status) {
        return jdbc.sql("SELECT * FROM t_order WHERE status = :status")
                .param("status", status)          // 命名参数
                .query(Order.class)
                .list();
    }

    // 位置参数 + 自动映射 + 单条
    public Optional<Order> byId(Long id) {
        return jdbc.sql("SELECT * FROM t_order WHERE id = ?")
                .param(id)
                .query(Order.class)
                .optional();                     // 0 或 1 条，无异常
    }

    // 多参数 + 排序
    public List<Order> page(String status, int offset, int limit) {
        return jdbc.sql("SELECT * FROM t_order WHERE status = :status ORDER BY id LIMIT :limit OFFSET :offset")
                .params("status", status, "limit", limit, "offset", offset)
                .query(Order.class)
                .list();
    }
}
```

| 链节 | 说明 |
|------|------|
| `sql(String)` | SQL（可含 `:name` 命名参数或 `?` 位置参数） |
| `param(name, value)` / `params(...)` | 逐参/批量参数 |
| `query(Class)` / `query(RowMapper)` | 映射方式 |
| `.list()` / `.optional()` / `.single()` / `.stream()` | 结果形态 |
| `.update()` | 执行更新返回行数 |

## 3. 更新与 DDL

```java
// 更新：update() 返回受影响行数
int n = jdbc.sql("UPDATE t_order SET status = :status WHERE id = :id")
        .param("status", "PAID").param("id", 1001L)
        .update();

// 插入 + 主键回填
KeyHolder kh = new GeneratedKeyHolder();
jdbc.sql("INSERT INTO t_order(user_id, amount) VALUES (:userId, :amount)")
        .param("userId", 1001L).param("amount", BigDecimal.valueOf(99))
        .update(kh);                              // 回填自增键
Long id = kh.getKey().longValue();

// DDL / 存储过程调用
jdbc.sql("CREATE INDEX idx_status ON t_order(status)").update();
```

> 💡 与 JdbcTemplate 语义对齐：`update()` 的返回与 `update(sql, args)` 相同；`KeyHolder` 回填机制完全一致（见 [02-JdbcTemplate 速查](02-JdbcTemplate速查.md) 第 5 节）。

## 4. 结果映射：query 四兄弟

| 方式 | 写法 | 适用 |
|------|------|------|
| 自动映射类 | `.query(Order.class)` | 列名 ↔ 字段名一致（BeanPropertyRowMapper 语义） |
| record | `.query(OrderRecord.class)` | record 构造器映射（7.x 一等公民） |
| RowMapper | `.query((rs, i) -> new Order(...))` | 复杂/多表 JOIN |
| 单列 | `.query(String.class)` | 单列列表（如 id 列表） |

```java
// record 自动映射（推荐：不可变 + 简洁）
public record OrderSummary(Long id, BigDecimal amount) { }

List<OrderSummary> list = jdbc.sql("SELECT id, amount FROM t_order WHERE amount > :min")
        .param("min", BigDecimal.TEN)
        .query(OrderSummary.class)
        .list();

// stream 流式处理（大结果集逐行消费，不全部驻留内存）
jdbc.sql("SELECT * FROM t_order WHERE batch_no = :batch")
        .param("batch", batchNo)
        .query(Order.class)
        .stream()                 // Stream<Order>
        .filter(o -> o.status().equals("CREATED"))
        .forEach(this::process);
```

> ⚠️ 注意：`stream()` 返回的是**持有连接**的流——必须用 try-with-resources 或确保全量消费后关闭，否则连接泄漏；`list()` 则立即全量取回、连接即释放。

## 5. 语句级配置（7.0）

| 配置 | 语义 | 示例 |
|------|------|------|
| `fetchSize` | 每次网络往返取行数 | `.fetchSize(1000)` |
| `maxRows` | 结果集行数上限 | `.maxRows(10_000)` |
| `queryTimeout` | 语句超时（秒） | `.queryTimeout(5)` |

```java
// 7.0 起：按语句精确控制，不再受全局 JdbcTemplate 配置束缚
List<Order> bigResult = jdbc.sql("SELECT * FROM t_order WHERE created_at >= :since")
        .param("since", since)
        .fetchSize(500)                    // 大结果流式拉取
        .maxRows(100_000)                  // 兜底上限
        .queryTimeout(30)                  // 30 秒超时
        .query(Order.class)
        .list();
```

> 🎯 **核心要点**：7.0 前 fetchSize/maxRows/queryTimeout 只能全局配置（影响所有语句）；7.0 后 JdbcClient 支持**每条语句独立配置**（#35155）——大报表语句与在线查询互不干扰，这是 JdbcClient 相对 JdbcTemplate 的"终极语法优势"。

## 6. 从 NamedParameterJdbcTemplate 迁移

```java
// 旧：NamedParameterJdbcTemplate
// List<Order> r = npjt.queryForList(sql, Map.of("status","PAID"), Order.class);
// int n = npjt.update(sql, Map.of("id", 1L));

// 新：JdbcClient（几乎一一对应）
List<Order> r = jdbc.sql(sql).params("status", "PAID").query(Order.class).list();
int n = jdbc.sql(sql).param("id", 1L).update();
```

| 旧 API | 新写法 |
|--------|--------|
| `queryForList(sql, params, Class)` | `sql(sql).params(map/varargs).query(Class).list()` |
| `queryForObject(sql, params, Class)` | `sql(sql).params(...).query(Class).single()`（多行抛 IncorrectResultSizeDataAccessException） |
| `update(sql, params)` | `sql(sql).params(...).update()` |
| `batchUpdate(sql, batchValues)` | `sql(sql).batchUpdate(listOfMaps)`（7.x）或 JdbcTemplate 批量 |
| `query(sql, RowMapper, params)` | `sql(sql).params(...).query(rowMapper).list()` |

> 💡 迁移原则：语义完全一致（同一执行内核）——迁移是**纯语法级**改动，不涉及事务/性能/异常行为变化；`queryForObject` 的空结果语义注意用 `optional()`/`single()` 显式表达。

---

**下一模块**：[04-DataSource 与连接池速查](04-DataSource与连接池速查.md)　**返回总览**：[00-Spring JDBC组件总览](00-Spring JDBC组件总览.md)
