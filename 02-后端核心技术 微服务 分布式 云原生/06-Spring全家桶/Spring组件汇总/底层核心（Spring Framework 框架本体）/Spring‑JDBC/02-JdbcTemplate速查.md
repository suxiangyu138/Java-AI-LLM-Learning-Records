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

### 1.1 选型决策树

```text
新代码？
├─ 是 → JdbcClient（[03-JdbcClient 速查](03-JdbcClient速查.md)）
└─ 否（维护存量）
    ├─ 已在用 NamedParameterJdbcTemplate？
    │   └─ 是 → 迁移到 JdbcClient（7.0 起废弃，API 一一对应）
    ├─ 已在用 JdbcTemplate？
    │   ├─ 代码稳定 → 可以不动（7.0 仍是一等公民）
    │   └─ 需要语句级配置 → 换 JdbcClient 的 fetchSize/maxRows/queryTimeout
    └─ 团队有"SQL 全量可控"强诉求 → 两者都满足，按风格偏好
```

| 判据 | 选 JdbcTemplate | 选 JdbcClient |
|------|----------------|---------------|
| 代码风格 | 方法式、参数堆叠 | 链式、可读性强 |
| 命名参数 | 需套 NamedParameterJdbcTemplate | 内建 |
| 语句级配置 | 无（7.0 仍无） | 有（7.0） |
| 团队存量 | 大量既有代码 | 新项目 / 新模块 |

> 💡 现实回答："新代码统一 JdbcClient，存量 JdbcTemplate 不主动重写"——重写不产生业务价值，只有升级时顺手迁移。

### 1.2 面试官追问什么

| 追问 | 回答锚点 |
|------|---------|
| JdbcTemplate 为什么是线程安全的 | 无状态设计：除配置项外没有可变字段；连接由 DataSourceUtils 按线程绑定 |
| 三个模板类是继承关系吗 | 不是——NamedParameterJdbcTemplate 是组合（内部持 JdbcTemplate），JdbcClient 也是组合 |
| 能同时用两个模板操作同一个事务吗 | 能——只要共享同一个 DataSource，连接绑定机制一致 |
| 为什么推荐 JdbcClient 却还保留 JdbcTemplate | 兼容存量 + 简单场景 JdbcTemplate 更直接；两者同一执行内核 |

> 💡 组合关系是高频考点：`NamedParameterJdbcTemplate` 内部把命名参数转成 `?` 后委托给 `JdbcTemplate` 执行——"模板套模板"的组合设计让命名参数能力可以叠加在任何执行能力上，也是理解 JdbcClient 内部结构（见 [03-JdbcClient 速查](03-JdbcClient速查.md) 第 1 节）的前置知识。

还有一个常被问的延伸：既然 JdbcTemplate 线程安全，为什么还要每次 new？——答案是不需要，Spring 中的 JdbcTemplate 就是单例 Bean；每次 new 只是失去了共享配置的收益，并不会出错。这个反直觉问题考验的是对"无状态"的理解深度：线程安全来自"无可变状态 + 连接按线程绑定"，与实例数量无关。

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

### 2.1 execute 的三种用法

```java
// ① 纯 DDL（无返回）
jdbcTemplate.execute("CREATE TABLE t_tmp (id BIGINT PRIMARY KEY, name VARCHAR(50))");

// ② 回调式：需要拿到 Statement 做自定义操作
Long one = jdbcTemplate.execute((StatementCallback<Long>) stmt -> {
    stmt.execute("SELECT 1");
    ResultSet rs = stmt.getResultSet();
    return rs.next() ? rs.getLong(1) : null;
});

// ③ 老式存储过程调用（新代码用 SimpleJdbcCall，见 05 速查）
jdbcTemplate.execute("{call sp_batch_close_order()}");
```

| 用途 | 方法 | 返回 |
|------|------|------|
| DDL / 无返回 DML | `execute(String)` | void |
| 需要 Statement 对象 | `execute(StatementCallback<T>)` | T |
| CallableStatement 场景 | `execute(CallableStatementCreator, CallableStatementCallback<T>)` | T |

> ⚠️ DDL 注意：MySQL 的 DDL 会**隐式提交**——`execute("DROP TABLE ...")` 即使包在事务里也无法回滚；批量建表/删表操作要评估影响面，别在事务中间夹 DDL。

### 2.2 查询形态对照（面试必背）

| 场景 | 方法 | 空结果行为 | 多行行为 |
|------|------|-----------|---------|
| 单值 | `queryForObject(sql, Long.class, args)` | 抛 EmptyResult | 抛 IncorrectResultSize |
| 单行 Map | `queryForMap(sql, args)` | 抛 EmptyResult | 抛 IncorrectResultSize |
| 多行单列 | `queryForList(sql, Long.class, args)` | 空 List | 正常 |
| 多行对象 | `query(sql, RowMapper, args)` | 空 List | 正常 |
| 可滚动结果集 | `queryForRowSet(sql)` | 空 SqlRowSet | 正常 |

记法：**单数形态（Object/Map）空与多都抛，复数形态（List）永不抛**——这就是"为什么业务上多用 list 形态兜底"的原因；`queryForRowSet` 适合需要随意跳转游标的场景（报表导出）。

### 2.3 结果集大小与内存的权衡

| 结果集规模 | 推荐方式 | 原因 |
|-----------|---------|------|
| 百行内 | `queryForList` / `query` 直接取 | 简单直接，内存无压力 |
| 千 - 万行 | `query` + 业务分批处理 | 单批处理完即弃，降低驻留 |
| 十万级+ | `queryForRowSet`（离线）或流式 RowMapper | 避免 List 全量驻留 |
| 报表导出 | `SqlRowSet` + 逐行写文件 | 不持有连接、可滚动 |

`queryForRowSet` 的离线特性容易被忽略：它把数据拷贝进内存中的 CachedRowSet 后立即释放连接——适合"查询结果要跨方法传递、但不想一直占着连接"的场景；代价是数据被完整拷贝，大结果集内存开销反而更大，所以要按规模选型，不能只看"离线"两个字就无脑用。

补充一个面试点：`query` 返回 List 后连接就已归还，而流式处理（如 queryForRowSet）必须在数据消费完前持有结果——两者的内存模型完全不同。理解"List 形态 = 数据已落地、连接已释放；流形态 = 数据在途中、连接未释放"，就能解释为什么大结果集用流式反而要小心连接泄漏，而小结果集无脑 List 最安全。

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

### 3.1 RowMapper 实战要点与坑

| 坑 | 表现 | 解法 |
|----|------|------|
| 列名不存在 | `BadSqlGrammarException` / 数据错位 | SQL 用别名 `AS`，mapper 与别名对齐 |
| 列值为 NULL | 自动装箱 NPE（`rs.getLong` 返回 0 混淆） | 用包装类型 + `rs.getObject` 判空 |
| 表结构变更 | 编译期无感知、运行期才报 | 映射集中在 DTO + 集成测试覆盖 |
| 大字段（BLOB/TEXT） | 每行全量加载、内存暴涨 | 拆到单独查询或流式读 |
| 时区混乱 | 时间差 8 小时 | URL 设 `serverTimezone`，统一 `getTimestamp().toLocalDateTime()` |

```java
// NULL 安全映射范例
List<Order> list = jdbcTemplate.query(
        "SELECT id, amount FROM t_order", (rs, i) -> {
            Long id = rs.getLong("id");
            BigDecimal amount = rs.getBigDecimal("amount");   // NULL → null，不抛
            return new Order(id, amount);
        });

// ResultSetExtractor：跨行聚合（整个结果集只回调一次）
Long total = jdbcTemplate.query("SELECT amount FROM t_order WHERE user_id = ?",
        rs -> {                                       // ResultSetExtractor<Long>
            long sum = 0;
            while (rs.next()) sum += rs.getLong(1);
            return sum;
        }, userId);
```

> 🎯 面试追问："RowMapper 和 ResultSetExtractor 什么时候用谁？"——逐行映射用 RowMapper（每行独立成对象、内存友好）；需要跨行聚合（sum 之后再算比例）用 ResultSetExtractor，它在整个 ResultSet 上只回调一次。

### 3.2 BeanPropertyRowMapper 配置细节

```java
// 开启下划线转驼峰（默认开启）；可自定义列名映射器
BeanPropertyRowMapper<Order> mapper = BeanPropertyRowMapper.newInstance(Order.class);
mapper.setPrimitivesDefaultedForNullValue(true);   // null 列 → 基本类型默认值（谨慎使用）
mapper.setColumnToPropertyNameMapper(column -> {
    if ("created_at".equals(column)) return "createdTime";   // 个别列特殊映射
    return StringUtils.uncapitalize(toCamel(column));
});
jdbcTemplate.query(sql, mapper, args);
```

| 配置 | 默认 | 注意 |
|------|------|------|
| 下划线转驼峰 | 开 | 关闭后列名必须与字段名完全一致 |
| primitivesDefaultedForNullValue | false | 开启后 null 列映射为 0/false——掩盖空值，慎开 |
| 自定义列映射器 | 无 | 个别列名差异时使用 |

> ⚠️ 生产建议：用 DTO（包装类型）承接可空列，保持默认配置——`setPrimitivesDefaultedForNullValue(true)` 会把"数据库 null"静默变成"0/空串"，是隐蔽数据 bug 的来源，排查时极难发现。

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

### 4.1 参数类型推断的源码级细节

JdbcTemplate 的位置参数最终走 `ArgumentPreparedStatementSetter` → `StatementCreatorUtils` 做类型推断：

| 参数类型 | 推断行为 |
|---------|---------|
| `null` | `ps.setNull(i, Types.NULL)`——不报错 |
| `String` / 基本类型包装 | 对应 `setString` / `setXxx` |
| `java.util.Date` | 自动转 `ps.setTimestamp`（内置转换） |
| `LocalDateTime` | `setObject`（依赖 JDBC 4.2 驱动支持） |
| `List/Set/数组` | 仅命名参数展开；位置参数当普通对象 setObject |
| 枚举 | `setObject` 按枚举对象设值，部分老驱动报错——需显式类型 |

```java
// 显式声明类型（日期/特殊值场景的保底方案）
jdbcTemplate.update(
        "UPDATE t_order SET paid_at = ? WHERE id = ?",
        new PreparedStatementSetter() {
            public void setValues(PreparedStatement ps) throws SQLException {
                ps.setTimestamp(1, Timestamp.valueOf(now));   // 显式 java.sql.Timestamp
                ps.setLong(2, orderId);
            }
        });
```

> ⚠️ 高频线上坑：`LocalDateTime` 参数在部分旧驱动上 `setObject` 失败，报 `Can't infer the SQL type`——解法是显式转 `Timestamp`，或用 `MapSqlParameterSource.registerSqlType` 声明类型。

### 4.2 命名参数与位置参数的选型

| 维度 | 位置参数 `?` | 命名参数 `:name` |
|------|-------------|-----------------|
| 可读性 | SQL 中看不出语义 | SQL 自解释 |
| 参数顺序 | 必须严格对应 | 与顺序无关 |
| 集合展开 | 不支持 | `IN (:ids)` 自动展开 |
| 参数复用 | 同 SQL 中重复参数要写多次 | 同一参数名可用多次 |
| 解析开销 | 无 | 有解析开销，可忽略 |

选型建议：SQL 短、参数少、无重复 → 位置参数（JdbcTemplate 直接可用）；SQL 长、参数多、要复用 → 命名参数（JdbcClient 或 NamedParameterJdbcTemplate）。团队规范通常统一为"新代码全部命名参数"，减少心智负担；面试答"为什么命名参数在长 SQL 上更好"——顺序无关意味着改 SQL 时不需要同步调整参数数组，重构安全性高。

补充一个细节：命名参数的展开发生在 SQL 解析阶段——`:ids` 展开后如果 SQL 总长度超过驱动限制（如 MySQL max_allowed_packet），会报 SQL 过长错误；几千个 id 的场景建议分片传入（每片 500-1000），既控制包大小又避免 IN 列表过长。

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

### 5.1 KeyHolder 多列回填与数据库差异

```java
// 复合键 / 多列回填（如"文档号 + 版本号"同时生成）
KeyHolder kh = new GeneratedKeyHolder();
jdbcTemplate.update(conn -> {
    PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO t_doc(title, content) VALUES (?, ?)",
            new String[]{"doc_no", "version"});          // ★ 指定要回填的列
    ps.setString(1, "t");
    ps.setString(2, "c");
    return ps;
}, kh);

Map<String, Object> keys = kh.getKeys();                 // 整行键值
String docNo = (String) keys.get("doc_no");
```

| 数据库 | 回填行为 |
|--------|---------|
| MySQL | `RETURN_GENERATED_KEYS` 返回自增键；多列需显式列名 |
| PostgreSQL | 支持 RETURNING 语义，多列友好 |
| Oracle | 依赖序列/触发器，`getKey` 可能为 null——用显式查询兜底 |
| H2 | 兼容 MySQL 行为 |

> 💡 回填失败经典排查：`getKey()` 返回 null 且无异常——多半是驱动不支持 `RETURN_GENERATED_KEYS` 或列名与驱动元数据不符；先 `getKeys()` 打印实际内容，比猜快得多。

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

### 6.1 面试官追问什么

| 追问 | 回答锚点 |
|------|---------|
| JdbcTemplate 是线程安全的吗 | 是——无状态（除配置项），连接来自 DataSourceUtils 的每线程绑定 |
| 为什么 queryForObject 空结果抛异常 | 语义是"必须恰好一条"；业务可空场景用 list / optional 形态 |
| 批量 10 万条怎么最快 | batchUpdate 分批 + `rewriteBatchedStatements` + 事务包裹（[05-批量操作与存储过程速查](05-批量操作与存储过程速查.md)） |
| fetchSize 设多少合适 | 500-1000 起步；受驱动与网络往返影响，压测校准 |
| 怎么看到执行的 SQL 与参数 | `logging.level.org.springframework.jdbc.core=DEBUG`（会打印 SQL 与参数值） |
| BeanPropertyRowMapper 支持嵌套属性吗 | 不支持——多表 JOIN 用自定义 RowMapper 或扁平 DTO |
| 如何防 SQL 注入 | 永远 `?` 占位 + 参数绑定；禁止任何字符串拼接（参数来自白名单也不拼） |
| 一个事务里多次查询会新开连接吗 | 不会——DataSourceUtils 返回事务绑定连接（见 [04-DataSource 与连接池速查](04-DataSource与连接池速查.md)） |

> 🎯 面试主线："连接管理（DataSourceUtils）→ 参数绑定（StatementCreatorUtils）→ 结果映射（RowMapper）→ 异常翻译（SQLExceptionTranslator）"——把这条链讲清楚，JdbcTemplate 的追问基本都能接住。

### 6.2 生产实践配置清单

| 配置 | 建议值 | 目的 |
|------|--------|------|
| `queryTimeout` | 5-30s（按接口） | 防慢 SQL 拖死线程池 |
| `maxRows` | 分页上限 × 系数 | 防失控查询打爆内存 |
| `fetchSize` | 500-1000 | 大结果集减少网络往返 |
| `ignoreWarnings` | true（诊断期 false） | 避免非致命警告频繁刷日志 |
| 日志级别 | jdbc.core=DEBUG（按需） | 看 SQL 与参数，排查必备 |

> 🎯 生产经验："全局宽松 + 热点收紧"——全局 `queryTimeout=30` 兜底，报表等特殊场景用 JdbcClient 语句级配置单独收紧（[03-JdbcClient 速查](03-JdbcClient速查.md) 第 5 节），既安全又不误伤正常查询；注意 fetchSize 在 MySQL 下需要 `useCursorFetch=true` 才真正生效（见 [08-集成地图与常见问题](08-集成地图与常见问题.md)）。

再补一条运维视角：`queryTimeout` 的生效依赖驱动实现——MySQL 驱动把它转成 `Statement.setQueryTimeout`，由服务端 `max_execution_time` 兜底配合；排查"超时配置没生效"时，先确认驱动版本支持，再确认没有更低层级的 socketTimeout 抢先触发。

---

**下一模块**：[03-JdbcClient 速查](03-JdbcClient速查.md)　**返回总览**：[00-Spring JDBC组件总览](00-Spring JDBC组件总览.md)
