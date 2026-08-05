# 07 JDBC 与框架的关系

> MyBatis、MyBatisPlus、Spring JDBC——一切 ORM 都是 JDBC 的封装。用 JDBC 视角读框架，框架从"黑盒"变"透明"

---

## 📚 目录

1. [ORM 的本质：JDBC 封装](#1-orm-的本质jdbc-封装)
2. [MyBatis 的 JDBC 视角](#2-mybatis-的-jdbc-视角)
3. [MyBatisPlus 的 JDBC 视角](#3-mybatisplus-的-jdbc-视角)
4. [Spring JDBC / JdbcTemplate](#4-spring-jdbc--jdbctemplate)
5. [分页的实现原理](#5-分页的实现原理)
6. [读框架源码的 JDBC 地图](#6-读框架源码的-jdbc-地图)

---

## 1. ORM 的本质：JDBC 封装

**所有 Java 数据库框架 = JDBC 的上层封装**：

```text
框架替你做的事（全部是 JDBC 的自动化）：
  ① 连接管理：DataSource 获取/归还（05 模块）
  ② SQL 管理：预编译 + 参数绑定（02 模块）
  ③ 结果映射：ResultSet → 对象（03 模块）
  ④ 事务管理：autoCommit/commit/rollback（04 模块）
  ⑤ 批量操作：addBatch/executeBatch（06 模块）

你写的业务代码 → 框架 → JDBC → 数据库
  Mapper 接口 / SQL 注解 / 实体类 → 最终都是 JDBC 调用
```

**框架的价值（不是取代 JDBC，是自动化）**：

```text
① 消除样板代码（七步 → 一行注解）
② 结果映射自动化（ResultSet → 对象）
③ 连接/事务托管（池化 + 声明式事务）
→ JDBC 是"发动机"，框架是"自动挡"——原理相同
```

> 🎯 **核心要点**：ORM 本质 = "**JDBC 的五件事自动化（连接/SQL/映射/事务/批量）**"——**"框架不是取代 JDBC 而是封装它"**是读框架源码的认知前提。

---

## 2. MyBatis 的 JDBC 视角

**MyBatis 执行一条 SQL 的 JDBC 本质**：

```java
// Mapper 接口方法：
List<User> list = userMapper.selectList(...);

// 底层（MyBatis 内部，JDBC 视角简化）：
// ① 从 SqlSession 拿连接（DataSource.getConnection —— 池中借）
// ② 解析 SQL → PreparedStatement（预编译！）
Connection conn = dataSource.getConnection();
PreparedStatement ps = conn.prepareStatement("SELECT * FROM user WHERE age > ?");
ps.setInt(1, minAge);
// ③ 执行
ResultSet rs = ps.executeQuery();
// ④ 结果映射（ResultSet → User 对象）
while (rs.next()) {
    User u = new User();
    u.setId(rs.getLong("id"));        // 属性映射 = getXxx 的反射版
    u.setName(rs.getString("name"));
}
// ⑤ 关闭（归还连接）
```

**MyBatis 与 JDBC 的对应关系**（面试必答）：

| MyBatis 组件 | JDBC 对应 |
|-------------|----------|
| SqlSession | Connection 的会话封装 |
| MappedStatement | PreparedStatement 的 SQL 描述 |
| ParameterHandler | setXxx 参数绑定 |
| ResultSetHandler | ResultSet → 对象映射 |
| Executor | 执行策略（Simple/Batch/Reuse） |

```text
关键认知：
  ① MyBatis 的 #{} 预编译（PreparedStatement）—— 与 JDBC ? 相同
  ② ${} 拼接（Statement 风格）—— 注入风险（与 JDBC 拼接同）
  ③ BatchExecutor = JDBC 批处理（addBatch/executeBatch）
```

> 🎯 **核心要点**：MyBatis = "**JDBC 的组件化封装**"——**"#{} 预编译、${} 拼接（风险）、BatchExecutor 批处理"三个对应**是面试深度题（与 `MyBatis/` 系统联动）。

---

## 3. MyBatisPlus 的 JDBC 视角

**MyBatisPlus = MyBatis + 通用 CRUD 增强**（JDBC 视角无新东西）：

```java
// saveBatch 的 JDBC 真相：
// MyBatisPlus 内部 → BatchExecutor → addBatch/executeBatch
// 与 JDBC 手写批量完全同源（06 模块的 1500ms 数据）

// 结果映射：实体字段 → ResultSet 列（反射 + 驼峰映射）
// 逻辑删除：SQL 改写（UPDATE ... SET deleted=1）→ 仍是预编译执行

// 分页插件：MybatisPlusInterceptor → SQL 改写（LIMIT）→ 预编译执行
```

**MyBatisPlus 的 JDBC 知识点**：

| MP 能力 | JDBC 底层 |
|---------|----------|
| saveBatch | addBatch/executeBatch |
| 逻辑删除 | SQL 改写（UPDATE） |
| 乐观锁 | UPDATE ... WHERE version=? |
| 分页 | SQL 改写（LIMIT）+ COUNT |
| 枚举处理 | getXxx 类型映射 |

> 🎯 **核心要点**：MyBatisPlus = "**MyBatis 的增强（JDBC 无新机制）**"——**"saveBatch 就是 JDBC 批处理、分页就是 SQL 改写"**让 MP 的"魔法"透明化（与 `MyBatisPlus/` 系统联动）。

---

## 4. Spring JDBC / JdbcTemplate

**JdbcTemplate：Spring 对 JDBC 的轻量封装**（无 ORM，但自动化样板）：

```java
// JdbcTemplate：消除七步样板（连接/预编译/映射自动）
@Autowired
private JdbcTemplate jdbcTemplate;

// 查询 → 自动映射
List<User> users = jdbcTemplate.query(
        "SELECT id, name FROM user WHERE age > ?",
        (rs, rowNum) -> new User(
                rs.getLong("id"),
                rs.getString("name")),     // RowMapper：ResultSet → 对象
        minAge);

// 更新（自动预编译 + 参数绑定）
int rows = jdbcTemplate.update(
        "UPDATE user SET name = ? WHERE id = ?", name, id);

// 查询单值
Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM user", Integer.class);
```

**JdbcTemplate vs MyBatis vs 手写 JDBC**：

| 维度 | 手写 JDBC | JdbcTemplate | MyBatis |
|------|:---------:|:------------:|:-------:|
| 样板代码 | 多（七步） | 少 | 最少 |
| 映射 | 手动 | RowMapper | 自动/XML |
| 动态 SQL | 手拼 | 手拼 | ✅ 标签 |
| 场景 | 教学 | 简单查询 | 复杂映射 |

> 🎯 **核心要点**：JdbcTemplate = "**JDBC 的轻量封装（自动连接/预编译，RowMapper 映射）**"——**"RowMapper 就是 ResultSet → 对象的回调"**是 JDBC 视角的关键；适合简单查询，复杂映射用 MyBatis。

---

## 5. 分页的实现原理

**分页 = SQL 改写（框架替你做）**——JDBC 视角：

```java
// 手写 JDBC 分页（MySQL）：
String sql = "SELECT * FROM user ORDER BY id LIMIT ?, ?";
ps.setInt(1, offset);       // 偏移（page-1）× size
ps.setInt(2, size);
// + COUNT 查询总数：
String countSql = "SELECT COUNT(*) FROM user";

// MyBatisPlus 分页（底层同样）：
// selectPage → 插件改写：
//   ① SELECT COUNT(*) FROM user WHERE ...（总数）
//   ② SELECT * FROM user WHERE ... LIMIT 0, 10（当前页）
// → 与手写 JDBC 完全一致（插件只是自动拼 LIMIT + COUNT）
```

**分页的 JDBC 细节**（面试深度）：

```text
① 页码转换：前端 page=1 → offset=(page-1)×size（page 从 1 开始）
② 大偏移问题：LIMIT 100000, 10 = 扫 10 万行再丢
   → 深分页优化：游标/Keyset（见 MyBatisPlus/06 与 Milvus/06）
③ COUNT 优化：条件复杂时 COUNT 慢（索引）
④ 不同数据库：MySQL LIMIT / Oracle ROWNUM / PG LIMIT —— 方言差异
   → 这就是"分页插件要指定 DbType"的原因
```

> 🎯 **核心要点**：分页 = "**SQL 改写（LIMIT + COUNT）**"——**"框架分页 = 手写 LIMIT 的自动化"是本质**；深分页与方言差异（DbType）是进阶考点（`MyBatisPlus/06` 联动）。

---

## 6. 读框架源码的 JDBC 地图

**带着 JDBC 知识读框架源码**（学习路径）：

```text
① MyBatis 源码入口（JDBC 视角）：
   - SimpleExecutor.doQuery → StatementHandler → prepareStatement
   - PreparedStatementHandler.instantiateStatement → conn.prepareStatement
   - DefaultResultSetHandler.handleResultSets → ResultSet 遍历映射
② Spring 事务源码：
   - DataSourceTransactionManager.doBegin → conn.setAutoCommit(false)
   - doCommit → conn.commit()；doRollback → conn.rollback()
③ HikariCP 源码：
   - HikariPool.getConnection → 池中借（FastList + 无锁）
   - ProxyConnection.close → 归还池（不是真关闭）
④ MyBatisPlus 分页源码：
   - PaginationInnerInterceptor.beforeQuery → SQL 改写（LIMIT）

→ 每个框架的核心机制 = 本体系某模块的 JDBC 知识
```

**JDBC 知识 → 框架源码的映射表**：

| JDBC 知识 | 框架源码位置 |
|-----------|-------------|
| PreparedStatement | MyBatis PreparedStatementHandler |
| ResultSet 映射 | MyBatis DefaultResultSetHandler |
| 事务五步 | Spring DataSourceTransactionManager |
| 连接池 | HikariCP HikariPool |
| 批处理 | MyBatis BatchExecutor |
| 分页改写 | MP PaginationInnerInterceptor |

> 🎯 **核心要点**：读源码地图 = "**六个框架入口 = 本体系六个模块**"——**"带着 JDBC 知识读框架源码，每个机制都能对上"**是最高效的源码学习法（框架源码细节见 `MyBatis/09`）。

---

**下一模块**：[08-常见问题与最佳实践](./08-常见问题与最佳实践.md) / **返回总览**：[00-JDBC知识体系总览](./00-JDBC知识体系总览.md)
