# 02 JdbcTemplate 速查

> JdbcTemplate 与 NamedParameterJdbcTemplate 的全 API、结果映射、参数绑定——存量代码主力，7.0 起退居二线

---

## 📚 目录

1. [三兄弟定位](#1-三兄弟定位)
2. [核心 API 速查](#2-核心-api-速查)
3. [RowMapper 结果映射](#3-rowmapper-结果映射)
4. [参数绑定与命名参数](#4-参数绑定与命名参数)
5. [自增主键回填](#5-自增主键回填)
6. [配置项速查](#6-配置项速查)

---

## 1. 三兄弟定位

| 类 | 定位 | 7.0 状态 |
|----|------|---------|
| `JdbcTemplate` | 经典模板：占位符 `?`，全 API | ✅ 稳定（继续使用） |
| `NamedParameterJdbcTemplate` | 命名参数 `:name` 封装 | ⚠️ 废弃（收编进 JdbcClient） |
| `JdbcClient` | Fluent 流式 API（6.1+） | ✅ 推荐（见 [03-JdbcClient 速查](03-JdbcClient速查.md)） |

> 🎯 **核心要点**：三者底层同一套执行引擎（`JdbcTemplate` 是内核）——JdbcClient 是**外壳风格升级**，性能/事务语义完全一致；选型：新代码 JdbcClient，存量 JdbcTemplate 可不动。

## 2. 核心 API 速查

| 方法 | 用途 | 返回 |
|------|------|------|
| `execute(String)` | 任意 DDL/DML | void |
| `update(sql, args...)` | INSERT/UPDATE/DELETE | int（受影响行数） |
| `queryForObject(sql, Type.class, args...)` | 单行单列/单对象 | T |
| `queryForList(sql, Type.class, args...)` | 多行单列 | `List<T>` |
| `queryForMap(sql, args...)` | 单行 Map | Map |
| `query(sql, RowMapper, args...)` | 多行自定义映射 | `List<T>` |
| `queryForRowSet(sql)` | 可滚动结果集 | `SqlRowSet` |
| `batchUpdate(sql, batchArgs, batchSize)` | 批量执行 | `int[]` |

```java
// 单值查询
Long count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM t_order WHERE status = ?", Long.class, "PAID");

// 多行 → 对象（RowMapper 或 类自动映射）
List<Order> orders = jdbcTemplate.query(
        "SELECT * FROM t_order WHERE amount > ?", (rs, i) -> new Order(
                rs.getLong("id"), rs.getBigDecimal("amount")), min);

// 简化：列名与字段名一致时
List<Order> orders2 = jdbcTemplate.query(sql,
        BeanPropertyRowMapper.newInstance(Order.class), min);
```

> ⚠️ 经典坑：`queryForObject` 空结果抛 `EmptyResultDataAccessException`，多行抛 `IncorrectResultSizeDataAccessException`——"无结果"场景用 `query(...).stream().findFirst()` 或 JdbcClient 的 `optional()`。

## 3. RowMapper 结果映射

| 映射器 | 语义 |
|--------|------|
| `RowMapper<T>` | 自定义：`T mapRow(ResultSet, int rowNum)`（每行调用一次） |
| `BeanPropertyRowMapper<T>` | 列名 ↔ 字段名自动映射（驼峰策略可配 `setColumnToPropertyNameMapper`） |
| `ColumnMapRowMapper` | 每行 → `Map<String,Object>` |
| `SingleColumnRowMapper` | 单列值提取 |

```java
// 自定义 RowMapper（含枚举/时间转换等复杂逻辑）
jdbcTemplate.query(sql, (rs, rowNum) -> new Order(
        rs.getLong("id"),
        OrderStatus.valueOf(rs.getString("status")),   // 枚举转换
        rs.getTimestamp("created_at").toLocalDateTime()
), args);

// 多表 JOIN 结果 → 扁平 DTO（RowMapper 常见用法）
jdbcTemplate.query(joinSql, (rs, i) -> new OrderDetailDto(
        rs.getLong("order_id"), rs.getString("user_name"), rs.getBigDecimal("total")));
```

> 💡 面试点：RowMapper 每行回调一次、**不持有全结果**（流式处理大结果集不炸内存）；`ResultSetExtractor` 则是"整个结果集一次性提取"（聚合统计场景）。

## 4. 参数绑定与命名参数

```java
// 位置参数：? 占位
jdbcTemplate.update("UPDATE t_order SET status=? WHERE id=? AND status=?",
        "PAID", 1001L, "CREATED");

// 命名参数（NamedParameterJdbcTemplate / JdbcClient）
MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("status", "PAID")
        .addValue("ids", List.of(1L, 2L, 3L));          // 集合自动展开为 ?,?,?
int n = namedJdbcTemplate.update(
        "UPDATE t_order SET status=:status WHERE id IN (:ids)", params);

// 参数类型：Date/Timestamp 需显式 java.sql 类型或交给 Spring 推断
ps.setTimestamp(i, Timestamp.valueOf(localDateTime));
```

| 参数来源 | 说明 |
|---------|------|
| `Object...` | 位置参数简写 |
| `MapSqlParameterSource` | 命名参数 + 类型控制（`registerSqlType`） |
| `BeanPropertySqlParameterSource` | POJO 属性 → 命名参数（`:propertyName`） |
| 集合展开 | 命名参数遇 `List/Set/数组` 自动展开为 `IN (?,?,?)` |

> ⚠️ 命名参数展开集合是面试常考点：`IN (:ids)` 的 `:ids` 若为 `List.of(1,2,3)`，Spring 展开为 3 个占位符——**空集合**会展开为 0 个参数导致 SQL 错误，需提前判空。

## 5. 自增主键回填

```java
KeyHolder keyHolder = new GeneratedKeyHolder();
jdbcTemplate.update(conn -> {
    PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO t_order(user_id, amount) VALUES (?, ?)",
            Statement.RETURN_GENERATED_KEYS);          // ★ 声明要回填
    ps.setLong(1, 1001L);
    ps.setBigDecimal(2, BigDecimal.valueOf(99.5));
    return ps;
}, keyHolder);

Long orderId = keyHolder.getKey().longValue();         // 回填的主键
```

> 💡 替代方案：`SimpleJdbcInsert`（见 [05-批量操作与存储过程速查](05-批量操作与存储过程速查.md)）提供 `usingGeneratedKeyColumns("id")` 的声明式回填——代码更简。

## 6. 配置项速查

| 属性（JdbcTemplate setter） | 默认 | 说明 |
|----------------------------|------|------|
| `fetchSize` | 驱动默认 | 每次网络往返取行数（大结果集调大，500-1000） |
| `maxRows` | 无限制 | 结果集上限（防失控查询） |
| `queryTimeout` | 0（无） | 单次查询超时秒数（防慢 SQL 拖死） |
| `ignoreWarnings` | false | 忽略 SQLWarning |
| `resultSetType` / `resultSetConcurrency` | 默认 | 可滚动/只读结果集 |
| `exceptionTranslator` | SQLErrorCode 翻译器 | 自定义异常映射（见 [06-异常体系与错误映射速查](06-异常体系与错误映射速查.md)） |

> 🎯 **核心要点**：`fetchSize`/`maxRows`/`queryTimeout` 是"大结果集三件套"——JdbcTemplate 全局设置；7.0 起 JdbcClient 支持**按语句设置**，更细粒度（[03-JdbcClient 速查](03-JdbcClient速查.md)）。

---

**下一模块**：[03-JdbcClient 速查](03-JdbcClient速查.md)　**返回总览**：[00-Spring JDBC组件总览](00-Spring JDBC组件总览.md)
