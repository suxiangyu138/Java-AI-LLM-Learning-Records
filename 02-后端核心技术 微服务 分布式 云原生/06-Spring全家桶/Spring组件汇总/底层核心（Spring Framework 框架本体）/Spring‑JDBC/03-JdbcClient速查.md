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

### 1.1 源码级：JdbcClient 如何复用 JdbcTemplate

```text
JdbcClient 内部持有一个 JdbcTemplate 实例：
  JdbcClient.create(dataSource)         → 内部 new JdbcTemplate(dataSource)
  new JdbcClient(jdbcTemplate)          → 复用外部传入的 JdbcTemplate（保留其配置）

  sql(sql)  → 解析出 :name 命名参数 → 惰性创建 NamedParameterJdbcTemplate 包装
  query(Class) → 内部转 BeanPropertyRowMapper（列名 ↔ 字段名自动映射）
  update()    → 委托 JdbcTemplate.update(...)
  batchUpdate → 委托 JdbcTemplate.batchUpdate(...)
```

| 组件 | 关系 |
|------|------|
| `JdbcClient.create(DataSource)` | 便捷工厂：内部构建 JdbcTemplate |
| `JdbcClient` 构造器 | 显式传入自定义 JdbcTemplate（保留其全局配置） |
| 命名参数 | 惰性构建 `NamedParameterJdbcTemplate` 做 `:name` 解析 |

> 🎯 面试答"JdbcClient 有性能损失吗"：没有——它**不是代理也不是 AOP**，就是普通对象组合；多一层方法调用可忽略，SQL 执行路径与 JdbcTemplate 完全相同。

延伸一句：面试问"JdbcClient 能替换 JdbcTemplate 吗"——答案是可以（同一内核），但 JdbcTemplate 的 `execute(StatementCallback)` 这类底层回调 API 在 JdbcClient 里没有一一对应；极端底层操作（如拿 Statement 做数据库特有动作）仍需要 JdbcTemplate。所以两者是"互补共存"而非"完全替代"，这是最严谨的答法。反过来也一样——JdbcTemplate 没有 JdbcClient 的 optional() 语义，空结果处理要靠手工判断，这是新代码选 JdbcClient 的又一个理由。

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

### 2.1 动态 SQL 与可选条件的拼接策略

```java
// 场景：按状态/金额范围筛选，条件均可空
public List<Order> search(String status, BigDecimal minAmount) {
    StringBuilder sql = new StringBuilder("SELECT * FROM t_order WHERE 1=1");
    List<Object> args = new ArrayList<>();
    if (status != null) { sql.append(" AND status = ?"); args.add(status); }
    if (minAmount != null) { sql.append(" AND amount >= ?"); args.add(minAmount); }
    sql.append(" ORDER BY id DESC");

    return jdbc.sql(sql.toString())
            .params(args.toArray())
            .query(Order.class)
            .list();
}
```

| 方案 | 适用 |
|------|------|
| 字符串拼接 + 位置参数（如上） | 条件少、规则简单 |
| 多个预编译 SQL + if 分支 | 条件组合固定（2-4 个枚举组合） |
| MyBatis 动态 SQL | 条件极多、需 XML 管理 |
| QueryDSL / jOOQ | 类型安全的复杂查询（重器） |

> ⚠️ 动态拼接时**值永远走 `.params()` 占位符**——任何把参数值直接拼进 SQL 字符串的写法都禁止（值含 `'` 时既报错又可能注入）；这是动态 SQL 场景的最高红线。

### 2.2 链式调用中的异常语义速查

| 环节 | 可能异常 | 说明 |
|------|---------|------|
| `.param()` 阶段 | `IllegalArgumentException` | 参数与 SQL 占位符不匹配（编码错误） |
| `.update()` 执行 | `DataAccessException` 族 | SQL 错误、约束冲突（翻译产物） |
| `.list()` 映射 | `DataAccessException` / 类型转换异常 | 列映射失败 |
| `.optional()` | 多行时抛 `IncorrectResultSizeDataAccessException` | 与 JdbcTemplate 语义一致 |

链式 API 的一个常见误解是"Fluent 风格会吞异常"——实际上异常语义与 JdbcTemplate 完全一致：参数错误是编码错误（立即抛），SQL 错误是翻译后的 `DataAccessException`（可捕获），空结果语义由你选择的结果形态决定（optional/list/single）。错误处理代码无需因为换了 API 而改变，这也是"同一执行内核"在行为层面最直接的体现。

补充：链式 API 的参数校验发生在执行前——`param` 数量与占位符不匹配时，`.update()` 或 `.list()` 阶段才报错（`InvalidDataAccessApiUsageException`），而不是链构造阶段；所以"参数没配对"的报错信息会指向执行行，调试时先数占位符再数参数。

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

### 3.1 update 返回值的使用与边界

| 场景 | 判断方式 |
|------|---------|
| 更新影响行数为 0 | `int n = update(); if (n == 0)` → 提示"数据已变更或不存在" |
| 乐观锁版本检查 | `WHERE version = :old` → n==0 说明版本冲突 |
| 幂等去重 | `WHERE status = 'CREATED'` → n==0 说明已处理过 |

```java
// 乐观锁风格更新（CAS 语义）
int n = jdbc.sql("UPDATE t_order SET status = 'PAID', version = version + 1 " +
                 "WHERE id = :id AND version = :oldVersion")
        .param("id", orderId).param("oldVersion", version)
        .update();
if (n == 0) {
    throw new OptimisticLockingFailureException("订单已被其他请求更新，请刷新重试");
}
```

> 💡 注意：`UPDATE` 把值改成与原来相同，MySQL 返回"匹配行数"还是"实际修改行数"由连接参数 `useAffectedRows` 决定——依赖 `n` 做业务判断时先确认驱动行为，否则会出现"改了但 n=0"的困惑。

### 3.2 更新与事务的配合要点

| 要点 | 说明 |
|------|------|
| 多条 update 自动同事务 | 同方法内多条链共享事务连接（外层有 @Transactional 时） |
| 影响行数为 0 的判断 | 不一定是失败——可能值未变化（useAffectedRows） |
| 更新后立刻查询 | 同事务同连接可见（除非隔离级别强制不可见） |
| 批量更新 | `.batchUpdate` 与 `.update()` 返回语义不同：前者 int[] |

```java
@Transactional
public void batchUpdateStatus(List<Long> ids, String newStatus) {
    for (Long id : ids) {
        jdbc.sql("UPDATE t_order SET status = :s WHERE id = :id")
                .param("s", newStatus).param("id", id)
                .update();          // 循环 N 次，但只有 1 条连接、1 个事务
    }
}
```

> 💡 注意：循环 update 在事务内"连接次数"仍是 1 次——数据库往返是 N 次，但连接借还、事务提交只有 1 次；所以事务内循环 update 比无事务快很多，但仍是 N 次网络往返，数据量大了还是要走批量（见 [05-批量操作与存储过程速查](05-批量操作与存储过程速查.md)）。

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

### 4.1 自动映射的细节与边界

| 问题 | 行为 |
|------|------|
| 列名 `user_id` → 字段 `userId` | `BeanPropertyRowMapper` 默认智能下划线转驼峰 |
| 字段类型不匹配 | 报 `Bad value for type ...` 或静默截断——建 DTO 时对齐类型 |
| record 映射 | 按构造器参数名匹配列名（参数名需与列名/别名一致） |
| 嵌套对象 | 不支持自动映射——用 RowMapper 或扁平 DTO |
| 枚举字段 | 自动映射按字符串处理，需自定义 RowMapper 转换 |

```java
// 混合映射：绝大多数列自动，单列特殊处理
List<OrderDto> list = jdbc.sql(sql)
        .param("status", status)
        .query((rs, i) -> {
            OrderDto dto = new OrderDto(
                    rs.getLong("id"),
                    rs.getBigDecimal("amount"));
            dto.setStatus(OrderStatus.valueOf(rs.getString("status")));  // 枚举转换
            return dto;
        })
        .list();
```

> 🎯 判断用不用自动映射：**列与字段一一对应、类型简单** → 自动；有任何"一字段对多列 / 需要计算 / 类型转换" → RowMapper。生产代码里 RowMapper 的使用率其实高于自动映射——自动映射是"省事"，RowMapper 是"可控"。

自动映射还有一个隐藏性能点：BeanPropertyRowMapper 会在首次映射时缓存列名→属性的映射关系（反射结果），后续行复用缓存——所以"自动映射比手写 RowMapper 慢"是不成立的，开销只在首行；真正慢的是每次 new BeanPropertyRowMapper，生产代码应复用实例。

### 4.2 空结果语义速查（四兄弟对照）

| 结果形态 | 0 条 | 1 条 | 多条 |
|---------|------|------|------|
| `.list()` | 空 List | 1 元素 | 全部元素 |
| `.optional()` | `Optional.empty()` | 有值 | 抛 IncorrectResultSizeDataAccessException |
| `.single()` | 抛 EmptyResultDataAccessException | 有值 | 抛 IncorrectResultSizeDataAccessException |
| `.stream()` | 空流 | 单元素流 | 全量流 |

> 🎯 面试高频追问："optional 和 single 都是取一条，为什么需要两个？"——optional 表达"可能存在"，single 表达"必须存在"；用 optional 的地方如果查到多条，说明业务假设被打破，抛异常是保护机制——两者把"空"与"多"两种错误分开暴露，比裸 JDBC 的 null 判断严谨得多。

实战中还常见一个用法：`optional()` 配合自定义 RowMapper 处理"可能不存在的单行"——比如用户中心按邮箱查账号，查不到返回 empty 走注册流程，查到多条说明数据异常（此时抛异常正是期望）。把"空"与"多"都当成需要处理的业务分支，是数据层健壮性的体现。

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

### 5.1 语句级配置 vs 全局配置的优先级

| 配置来源 | 生效范围 | 优先级 |
|---------|---------|--------|
| JdbcTemplate 全局 setter | 该模板所有语句 | 低（被语句级覆盖） |
| JdbcClient 链式配置 | 仅当前链 | 高 |
| 连接 URL 参数 | 连接级兜底（如 socketTimeout） | 兜底 |

```java
// 生产实践：在线接口从严，报表批任务放宽
// 在线查询：3 秒必断 + 行数上限，保护数据库
public List<Order> onlineList(String status) {
    return jdbc.sql("SELECT * FROM t_order WHERE status = :status")
            .param("status", status)
            .queryTimeout(3)
            .maxRows(1_000)
            .query(Order.class)
            .list();
}

// 报表导出：大结果流式拉取
public void exportDaily() {
    jdbc.sql("SELECT * FROM t_order WHERE created_at >= :since")
            .param("since", yesterday())
            .fetchSize(1000)
            .query(Order.class)
            .stream()                        // ★ 流式：边读边写 CSV
            .forEach(this::writeCsvRow);
}
```

> ⚠️ 大结果集 + `stream()` 必须确保流被完全消费或显式关闭（流关闭才归还连接）；数据量不大时直接 `list()` 更省心——两者按数据量权衡，不要无脑流式。

### 5.2 语句级配置的适用场景与误用

| 场景 | 建议配置 |
|------|---------|
| 在线列表接口 | `queryTimeout(3)` + `maxRows(1000)` |
| 大报表导出 | `fetchSize(1000)` + `maxRows(500_000)` |
| 后台批处理 | `queryTimeout(300)` 放宽 |
| 预防性兜底 | 全局 JdbcTemplate `setQueryTimeout(10)` |

误用提醒：`maxRows` 是"截断"不是"分页"——达到上限后剩余行被丢弃，业务上要靠 `LIMIT`/`OFFSET` 分页而非 maxRows；`fetchSize` 不是越大越好，超过驱动网络缓冲反而浪费内存。语句级配置解决的是"让每类语句有独立的安全边界"，不要把它当成性能调优手段——性能问题先查索引与执行计划。

另外注意语句级配置的继承语义：链上未显式设置的项沿用 JdbcTemplate 全局值——语句级配置是"覆盖"而非"重置"。所以全局兜底（如 queryTimeout=10）依然对显式设置过 fetchSize 的链生效，安全边界不会因部分配置而失效；理解这个覆盖语义，配置就不会出现"以为关了实际没关"的盲区。

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

### 6.1 迁移检查清单与面试官追问

| 检查项 | 说明 |
|--------|------|
| `queryForObject` → `optional()` / `single()` | 逐处确认空结果语义变化（原本抛异常 → 现在可返回 empty） |
| 批量 `batchUpdate` 调用点 | JdbcClient `.batchUpdate(...)` 或保持 JdbcTemplate 批量 |
| `Map<String, ?>` 参数 | `.params(Map)` 直接兼容 |
| 动态 SQL 拼接处 | 语法不变，仅入口类更换 |
| 单元测试回归 | 行为等价（同一执行内核），全量跑一遍即可 |

| 追问 | 回答锚点 |
|------|---------|
| JdbcClient 与 JdbcTemplate 性能差异 | 无差异——同一执行内核，纯风格封装 |
| `optional()` 与 `single()` 区别 | optional：0-1 条，多行抛 IncorrectResultSize；single：必须恰好 1 条 |
| 命名参数和位置参数能混用吗 | 不能——一条 SQL 要么全 `?` 要么全 `:name` |
| 链式 API 怎么保证连接关闭 | 内部仍走 DataSourceUtils 的 try/finally；`stream()` 需自行关闭 |
| 7.0 语句级配置的实现机制 | 链上持有配置对象，委托 JdbcTemplate 时按语句覆盖全局设置 |

### 6.2 迁移后的验证与回归

| 验证项 | 方法 |
|--------|------|
| 功能回归 | 全量跑 DAO 集成测试（`@SpringBootTest + @Transactional`） |
| 空结果语义 | 特意查不存在的数据，确认 optional() / single() 行为符合预期 |
| 批量性能 | 对比迁移前后 batchUpdate 耗时（应无差异） |
| 事务一致性 | 制造异常验证回滚（同一执行内核，理论上零风险） |

迁移最大的风险不是功能差异——同一执行内核保证了行为等价——而是"人"的回归：比如把 `queryForObject` 无脑改成 `.single()`，导致空数据时反而抛异常。因此迁移清单第一项永远是"逐处确认空结果语义"，这是唯一的语义变化点；其余改动都是机械替换，可以由 IDE 重构辅助完成。

迁移的最后一个建议：小步提交。把迁移拆成"一个 DAO 一个 PR"的粒度，每个 PR 配一次集成测试回归——JdbcClient 迁移虽然语法简单，但"批量提交 + 代码评审 + 测试覆盖"的组合能最大程度避免逐处手改引入的遗漏；一次迁移几百个调用点的"大爆炸式"改法，是回归事故的高发源。

---

**下一模块**：[04-DataSource 与连接池速查](04-DataSource与连接池速查.md)　**返回总览**：[00-Spring JDBC组件总览](00-Spring JDBC组件总览.md)
