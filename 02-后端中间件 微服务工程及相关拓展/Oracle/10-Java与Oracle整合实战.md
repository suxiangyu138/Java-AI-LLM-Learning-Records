# 10 - Java 与 Oracle 整合实战

> 🎯 Java 后端连接 Oracle 的核心要点：JDBC 驱动、连接池配置、PL/SQL 调用、BLOB/CLOB 处理、分页最佳实践

---

## 目录

1. [JDBC 连接与连接池](#1-jdbc-连接与连接池)
2. [PL/SQL 调用](#2-plsql-调用)
3. [分页与大数据](#3-分页与大数据)

---

## 1. JDBC 连接与连接池

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.oracle.database.jdbc</groupId>
    <artifactId>ojdbc8</artifactId>
    <version>21.9.0.0</version>
</dependency>
```

```yaml
# application.yml — HikariCP 连接 Oracle
spring:
  datasource:
    url: jdbc:oracle:thin:@//host:1521/ORCLPDB
    username: app_user
    password: password
    driver-class-name: oracle.jdbc.OracleDriver
    hikari:
      maximum-pool-size: 20
      connection-test-query: SELECT 1 FROM DUAL
      connection-timeout: 30000
```

```java
// 多数据源（主库 + 只读备库）
@Bean
@Primary
public DataSource primaryDataSource() {
    return DataSourceBuilder.create()
        .url("jdbc:oracle:thin:@//primary:1521/ORCL")
        .build();
}

@Bean
public DataSource readonlyDataSource() {
    return DataSourceBuilder.create()
        .url("jdbc:oracle:thin:@//standby:1521/ORCL")
        .build();
}
```

## 2. PL/SQL 调用

```java
// 调用存储过程
@Repository
public class UserDao {
    private final JdbcTemplate jdbcTemplate;

    // 调用无参存储过程
    public void archiveUsers() {
        jdbcTemplate.execute("CALL archive_old_users()");
    }

    // 调用带参存储过程
    public void transferMoney(Long fromId, Long toId, BigDecimal amount) {
        jdbcTemplate.update(
            "CALL transfer_money(?, ?, ?)", fromId, toId, amount);
    }

    // 调用函数
    public Double getTotalRevenue(int year) {
        return jdbcTemplate.queryForObject(
            "SELECT get_total_revenue(?) FROM DUAL",
            Double.class, year);
    }
}
```

```java
// MyBatis 调用 PL/SQL
@Select("CALL archive_old_users()")
void archiveOldUsers();

@Select("SELECT get_total_revenue(#{year}) FROM DUAL")
Double getTotalRevenue(int year);
```

## 3. 分页与大数据

```java
// Oracle 分页最佳实践（12c+）
public Page<User> findPage(int page, int size) {
    String sql = """
        SELECT * FROM users
        ORDER BY id
        OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
        """;
    int offset = (page - 1) * size;
    List<User> users = jdbcTemplate.query(sql, userRowMapper, offset, size);
    int total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
    return new Page<>(users, total, page, size);
}

// 游标读取大数据（避免 OOM）
@Select("SELECT * FROM large_table")
@Options(resultSetType = ResultSetType.FORWARD_ONLY, fetchSize = 1000)
Cursor<Record> streamAll();
```

### BLOB/CLOB 处理

```java
// 读取大文本
String text = jdbcTemplate.queryForObject(
    "SELECT content FROM documents WHERE id = ?",
    (rs, rowNum) -> {
        Clob clob = rs.getClob("content");
        return clob.getSubString(1, (int) clob.length());
    }, docId);

// 写入 BLOB
jdbcTemplate.update("INSERT INTO files(id, data) VALUES (?, ?)", ps -> {
    ps.setLong(1, fileId);
    ps.setBlob(2, new ByteArrayInputStream(fileBytes));
});
```

## 核心要点回顾

- JDBC URL: `jdbc:oracle:thin:@//host:port/service`
- `SELECT ... FROM DUAL` — Oracle 特有，测试连接必须
- HikariCP 测试查询：`SELECT 1 FROM DUAL`
- PL/SQL 调用：`CALL procedure(?, ?)` 或 `SELECT function() FROM DUAL`
- 分页：`OFFSET ... FETCH`（12c+ 推荐，不用 ROWNUM 嵌套了）

## 参考资料

1. Oracle JDBC Developer's Guide
2. HikariCP 官方文档
