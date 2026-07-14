# JDBC 与连接池

> **JDBC（Java Database Connectivity）是 Java 访问数据库的标准 API。无论是 MyBatis、JPA 还是 Spring Data，底层都脱离不了 JDBC。不理解 JDBC，就不能真正理解任何 Java 数据库框架。**

---

## 目录

- [1. JDBC 架构概述](#1-jdbc-架构概述)
- [2. JDBC 驱动](#2-jdbc-驱动)
- [3. 获取数据库连接](#3-获取数据库连接)
- [4. Statement 详解](#4-statement-详解)
- [5. PreparedStatement 与 SQL 注入](#5-preparedstatement-与-sql-注入)
- [6. CallableStatement（存储过程调用）](#6-callablestatement存储过程调用)
- [7. ResultSet 与结果集处理](#7-resultset-与结果集处理)
- [8. 事务管理](#8-事务管理)
- [9. 批量操作](#9-批量操作)
- [10. BLOB 和 CLOB 处理](#10-blob-和-clob-处理)
- [11. RowSet](#11-rowset)
- [12. 元数据（Metadata）](#12-元数据metadata)
- [13. 连接池原理与动机](#13-连接池原理与动机)
- [14. HikariCP 深度解析](#14-hikaricp-深度解析)
- [15. Druid 连接池](#15-druid-连接池)
- [16. 其他连接池：DBCP2 与 Tomcat JDBC Pool](#16-其他连接池dbcp2-与-tomcat-jdbc-pool)
- [17. 分页查询实现](#17-分页查询实现)
- [18. DAO 模式与服务层事务](#18-dao-模式与服务层事务)
- [19. 企业级 JDBC 最佳实践](#19-企业级-jdbc-最佳实践)
- [20. 面试题](#20-面试题)

---

## 1. JDBC 架构概述

### 1.1 JDBC 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                      Java Application                        │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │   UserDAO    │  │   OrderDAO   │  │   ProductDAO │       │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘       │
│         │                 │                 │                │
│         └─────────────────┼─────────────────┘                │
│                           │                                  │
│  ┌────────────────────────▼────────────────────────┐        │
│  │                   JDBC API                        │        │
│  │  DriverManager  │  DataSource  │  Connection     │        │
│  │  Statement      │  ResultSet   │  SQLException   │        │
│  └────────────────────────┬────────────────────────┘        │
│                           │                                  │
│  ┌────────────────────────▼────────────────────────┐        │
│  │               Connection Pool                      │        │
│  │  HikariCP / Druid / DBCP2 / Tomcat Pool          │        │
│  └────────────────────────┬────────────────────────┘        │
└─────────────────────────────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                     JDBC Driver (mysql-connector-j)          │
│                                                              │
│              ┌──────────────────────────────┐                │
│              │  Protocol: TCP (3306)        │                │
│              │  MySQL 协议解析与数据转换      │                │
│              └──────────────────────────────┘                │
└─────────────────────────────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                      MySQL Server                            │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 JDBC 核心接口

JDBC 是 JDK 内置的一组接口（位于 `java.sql` 和 `javax.sql` 包中），由各数据库厂商提供实现（驱动）。

| 接口 | 用途 | 核心方法 |
|------|------|---------|
| `DriverManager` | 管理 JDBC 驱动，创建连接 | `getConnection(url, user, pass)` |
| `DataSource` | 连接池工厂，替代 DriverManager | `getConnection()` |
| `Connection` | 数据库连接会话 | `createStatement()`, `prepareStatement()`, `setAutoCommit()`, `commit()`, `rollback()` |
| `Statement` | 执行静态 SQL | `executeQuery()`, `executeUpdate()`, `execute()` |
| `PreparedStatement` | 预编译 SQL，防 SQL 注入 | `setXxx()`, `executeQuery()`, `executeUpdate()` |
| `CallableStatement` | 调用存储过程 | `registerOutParameter()`, `execute()` |
| `ResultSet` | 查询结果集 | `next()`, `getXxx()`, `getString()`, `getInt()` |
| `SQLException` | 数据库操作异常 | `getErrorCode()`, `getSQLState()` |
| `DatabaseMetaData` | 数据库元信息 | `getTables()`, `getColumns()` |
| `ResultSetMetaData` | 结果集元信息 | `getColumnCount()`, `getColumnName()` |

### 1.3 JDBC 操作标准流程

```java
// JDBC 操作的七个标准步骤
public class JdbcDemo {

    public static void main(String[] args) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            // 1. 加载驱动（JDBC 4.0+ 自动加载，可省略）
            // Class.forName("com.mysql.cj.jdbc.Driver");

            // 2. 获取连接
            conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/mydb?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8mb4",
                "root",
                "password"
            );

            // 3. 创建 PreparedStatement
            String sql = "SELECT id, username, email FROM users WHERE status = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, 1);  // 设置参数

            // 4. 执行查询
            rs = ps.executeQuery();

            // 5. 处理结果集
            while (rs.next()) {
                Long id = rs.getLong("id");
                String username = rs.getString("username");
                String email = rs.getString("email");
                System.out.printf("id=%d, username=%s, email=%s%n", id, username, email);
            }

        } catch (SQLException e) {
            // 6. 处理异常
            System.err.printf("SQL Error: %s, Code: %d, State: %s%n",
                e.getMessage(), e.getErrorCode(), e.getSQLState());
            e.printStackTrace();
        } finally {
            // 7. 释放资源（按创建顺序的反序）
            close(rs);
            close(ps);
            close(conn);
        }
    }

    private static void close(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
```

---

## 2. JDBC 驱动

### 2.1 驱动类型

JDBC 驱动分为 4 种类型：

| 类型 | 名称 | 实现方式 | 典型代表 |
|------|------|---------|---------|
| Type 1 | JDBC-ODBC 桥 | 通过 ODBC 驱动访问数据库 | sun.jdbc.odbc.JdbcOdbcDriver（已废弃） |
| Type 2 | 本地 API 驱动 | 使用数据库厂商的本地库 | Oracle OCI |
| **Type 3** | 网络协议驱动 | 通过中间件访问数据库 | 较少使用 |
| **Type 4** | 纯 Java 驱动 | 直接用 Java Socket 连接数据库 | **mysql-connector-j、PostgreSQL JDBC** |

> 目前主流使用 **Type 4 纯 Java 驱动**，无需安装任何客户端库，只需在项目中引入一个 jar 包。

### 2.2 驱动加载机制

**JDBC 4.0 之前（手动加载）：**

```java
// 手动注册驱动（JDBC 3.0 及之前必需）
try {
    Class.forName("com.mysql.cj.jdbc.Driver");
} catch (ClassNotFoundException e) {
    e.printStackTrace();
}
```

`Class.forName()` 会触发类的静态初始化块，在其中执行 `DriverManager.registerDriver(new Driver())`。

```java
// com.mysql.cj.jdbc.Driver 源码（简化）
public class Driver extends NonRegisteringDriver implements java.sql.Driver {
    static {
        try {
            java.sql.DriverManager.registerDriver(new Driver());
        } catch (SQLException E) {
            throw new RuntimeException("Can't register driver!");
        }
    }

    public Driver() throws SQLException {
        // 调用父类构造
    }
}
```

**JDBC 4.0+（自动加载，推荐）：**

从 Java 6（JDBC 4.0）开始，`DriverManager` 使用 **ServiceLoader** 机制自动发现驱动。

```
META-INF/services/java.sql.Driver
  └── 文件内容：
      com.mysql.cj.jdbc.Driver
      com.mysql.fabric.jdbc.FabricMySQLDriver
```

当 `DriverManager.getConnection()` 首次被调用时，`ServiceLoader` 会扫描 ClassPath 下所有 `META-INF/services/java.sql.Driver` 文件中声明的驱动类，并自动加载。

```java
// JDBC 4.0+，无需 Class.forName，引入 mysql-connector-j 即可自动注册
Connection conn = DriverManager.getConnection(url, username, password);
```

### 2.3 常见 JDBC 驱动 URL 格式

| 数据库 | JDBC 驱动 | URL 格式 |
|--------|----------|---------|
| MySQL 8.0+ | `com.mysql.cj.jdbc.Driver` | `jdbc:mysql://host:3306/db?参数` |
| MySQL 5.x | `com.mysql.jdbc.Driver` | `jdbc:mysql://host:3306/db` |
| PostgreSQL | `org.postgresql.Driver` | `jdbc:postgresql://host:5432/db` |
| Oracle | `oracle.jdbc.OracleDriver` | `jdbc:oracle:thin:@host:1521:db` |
| SQL Server | `com.microsoft.sqlserver.jdbc.SQLServerDriver` | `jdbc:sqlserver://host:1433;DatabaseName=db` |
| H2 (内存库) | `org.h2.Driver` | `jdbc:h2:mem:test` |
| SQLite | `org.sqlite.JDBC` | `jdbc:sqlite:test.db` |

---

## 3. 获取数据库连接

### 3.1 DriverManager 方式

```java
// 直接通过 DriverManager 获取连接
// 每次调用都会创建一个新的 TCP 连接到 MySQL

String url = "jdbc:mysql://localhost:3306/mydb"
    + "?useSSL=false"
    + "&serverTimezone=Asia/Shanghai"
    + "&characterEncoding=utf8mb4"
    + "&rewriteBatchedStatements=true";  // 批量操作优化

String username = "root";
String password = "password";

try (Connection conn = DriverManager.getConnection(url, username, password)) {
    System.out.println("连接成功: " + conn.getCatalog());
    System.out.println("AutoCommit: " + conn.getAutoCommit());
    System.out.println("Schema: " + conn.getSchema());
} catch (SQLException e) {
    e.printStackTrace();
}
```

**问题：** `DriverManager.getConnection()` 每次创建新连接。建立 TCP 连接 + 认证的耗时通常在 50-200ms。高并发场景下不可接受。这就是**连接池**存在的根本原因。

### 3.2 DataSource 方式（推荐）

`DataSource` 是连接池的抽象接口，由连接池实现（HikariCP、Druid 等）。

```java
// DataSource 接口定义
public interface DataSource  extends CommonDataSource, Wrapper {
    Connection getConnection() throws SQLException;
    Connection getConnection(String username, String password) throws SQLException;
}
```

```java
// 基本 DataSource 实现（非连接池，仅用于演示）
public class SimpleDataSource implements DataSource {
    private final String url;
    private final String username;
    private final String password;

    public SimpleDataSource(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    // 其他方法省略...
}
```

### 3.3 连接参数详解

```java
// MySQL JDBC URL 常用参数
String url = "jdbc:mysql://localhost:3306/mydb"

    // 基本参数
    + "?useSSL=false"                          // 是否使用 SSL（开发环境 false）

    // 编码相关
    + "&characterEncoding=utf8mb4"              // 字符编码
    + "&serverTimezone=Asia/Shanghai"           // 服务器时区
    + "&connectionCollation=utf8mb4_unicode_ci" // 连接校对规则

    // 性能相关
    + "&rewriteBatchedStatements=true"          // 批量操作重写（性能提升 10x+）
    + "&useServerPrepStmts=false"               // 是否使用服务端预编译
    + "&cachePrepStmts=true"                    // 缓存 PreparedStatement
    + "&prepStmtCacheSize=250"                  // PreparedStatement 缓存数量
    + "&prepStmtCacheSqlLimit=2048"             // 缓存 SQL 的最大长度

    // 连接超时
    + "&connectTimeout=5000"                    // 连接超时（毫秒）
    + "&socketTimeout=30000"                    // Socket 超时（毫秒）

    // 其他
    + "&allowPublicKeyRetrieval=true"           // 允许获取公钥（MySQL 8.0）
    + "&useTimezone=true"                       // 使用时区转换
    + "&useLegacyDatetimeCode=false"            // 使用新版时间处理
    + "&autoReconnect=false";                   // 不建议启用（使用连接池管理）
```

---

## 4. Statement 详解

### 4.1 Statement 基础

```java
// Statement：执行静态 SQL，存在 SQL 注入风险
try (Connection conn = getConnection();
     Statement stmt = conn.createStatement()) {

    // executeQuery()：执行查询，返回 ResultSet
    String sql = "SELECT id, username FROM users WHERE id = " + userId;
    try (ResultSet rs = stmt.executeQuery(sql)) {
        while (rs.next()) {
            // 处理结果
        }
    }

    // executeUpdate()：执行 INSERT/UPDATE/DELETE，返回影响行数
    String insert = "INSERT INTO users(username) VALUES('" + username + "')";
    int rows = stmt.executeUpdate(insert);
    System.out.println("影响了 " + rows + " 行");

    // execute()：执行任意 SQL，返回 boolean（true=有结果集，false=无结果集）
    boolean hasResult = stmt.execute("SELECT 1");
    if (hasResult) {
        try (ResultSet rs = stmt.getResultSet()) {
            // 处理结果集
        }
    }

    // executeBatch()：批量执行
    stmt.addBatch("INSERT INTO users(username) VALUES('alice')");
    stmt.addBatch("INSERT INTO users(username) VALUES('bob')");
    int[] results = stmt.executeBatch();

} catch (SQLException e) {
    e.printStackTrace();
}
```

### 4.2 Statement 的 SQL 注入问题

```java
// 恶意用户输入
String userInput = "' OR '1'='1";  // 用户名输入
String passwordInput = "' OR '1'='1";  // 密码输入

// 拼接 SQL
String sql = "SELECT * FROM users WHERE username = '" + userInput
           + "' AND password = '" + passwordInput + "'";

// 实际执行的 SQL：
// SELECT * FROM users WHERE username = '' OR '1'='1' AND password = '' OR '1'='1'
//   → WHERE 条件永远为真！ → 返回所有用户数据！

System.out.println("SQL = " + sql);
// 输出: SELECT * FROM users WHERE username = '' OR '1'='1' AND password = '' OR '1'='1'
```

**SQL 注入的危害：**

```sql
-- 1. 绕过登录验证
' OR '1'='1

-- 2. 删除数据
'; DROP TABLE users; --

-- 3. 获取敏感数据
' UNION SELECT username, password FROM admin --

-- 4. 修改数据
'; UPDATE users SET role='admin' WHERE username='hacker'; --
```

**永远使用 PreparedStatement 代替 Statement 来防止 SQL 注入。**

---

## 5. PreparedStatement 与 SQL 注入

### 5.1 PreparedStatement 优势

```java
// PreparedStatement 的三大优势：
// 1. 预编译：一次编译，多次执行，提高性能
// 2. 防 SQL 注入：参数与 SQL 分开传输
// 3. 类型安全：通过 setXxx() 方法明确指定参数类型

String sql = "SELECT id, username, email FROM users WHERE username = ? AND password = ?";

try (Connection conn = getConnection();
     PreparedStatement ps = conn.prepareStatement(sql)) {

    // 设置参数（下标从 1 开始）
    ps.setString(1, username);  // 第一个 ? 
    ps.setString(2, password);  // 第二个 ?

    // 执行查询
    try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
            // 登录成功
        }
    }
}

// ★ SQL 注入彻底失效
// 即使用户输入 "' OR '1'='1"，参数也会被完整转义
// 传输到数据库的 SQL：SELECT ... WHERE username = ''' OR ''1''=''1' AND password = 'xxx'
// 参数值被当作普通字符串处理，不会改变 SQL 语义
```

### 5.2 PreparedStatement API 详解

```java
String sql = "INSERT INTO users(username, password, email, age, salary, birthday, active) "
           + "VALUES(?, ?, ?, ?, ?, ?, ?)";

try (PreparedStatement ps = conn.prepareStatement(sql)) {

    // 设置各种类型的参数
    ps.setString(1, "john_doe");           // VARCHAR
    ps.setString(2, "hashed_password");    // VARCHAR
    ps.setString(3, "john@example.com");   // VARCHAR

    ps.setInt(4, 28);                       // INT
    ps.setBigDecimal(5, new BigDecimal("5000.00")); // DECIMAL

    // 日期类型
    ps.setDate(6, Date.valueOf("1995-06-15"));    // java.sql.Date = DATE
    // ps.setTimestamp(6, Timestamp.valueOf("1995-06-15 10:30:00")); // DATETIME

    // 布尔类型（MySQL 中 TINYINT）
    ps.setBoolean(7, true);  // true=1, false=0

    // 设置 NULL
    ps.setNull(3, Types.VARCHAR);  // email 设为 NULL

    // 执行更新
    int rows = ps.executeUpdate();
    System.out.println("插入了 " + rows + " 行");
}

// setXxx() 方法对照表：
// setString()      → VARCHAR, CHAR, TEXT
// setInt()         → INT, INTEGER
// setLong()        → BIGINT
// setFloat()       → FLOAT
// setDouble()      → DOUBLE
// setBigDecimal()  → DECIMAL, NUMERIC
// setBoolean()     → BIT, TINYINT(1)
// setDate()        → DATE
// setTime()        → TIME
// setTimestamp()   → DATETIME, TIMESTAMP
// setBytes()       → BINARY, VARBINARY, BLOB
// setObject()      → 任意类型（自动类型推断，尽量少用）
```

### 5.3 获取自动生成的主键

```java
String sql = "INSERT INTO users(username, password) VALUES(?, ?)";

// 指定需要返回自动生成的主键
try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
    ps.setString(1, "john");
    ps.setString(2, "password123");
    ps.executeUpdate();

    // 获取自动生成的主键
    try (ResultSet rs = ps.getGeneratedKeys()) {
        if (rs.next()) {
            long newId = rs.getLong(1);
            System.out.println("新用户 ID: " + newId);
        }
    }
}
```

### 5.4 服务端预编译（PreparedStatement 缓存）

```sql
-- MySQL JDBC 驱动默认行为：
-- useServerPrepStmts=false
--   → 客户端预编译：JDBC 驱动模拟预编译，实际发送完整 SQL
--   → "?" 被替换为参数值后发送到服务器

-- useServerPrepStmts=true + cachePrepStmts=true
--   → 服务端预编译：第一次发送 SQL 模板，MySQL 预编译并缓存
--   → 后续只发送参数值，减少 SQL 解析开销
--   → 适合大量相同 SQL 重复执行的场景

-- 建议：对于 OLTP 高频查询，开启服务端预编译
url = "jdbc:mysql://...?useServerPrepStmts=true&cachePrepStmts=true&prepStmtCacheSize=500"
```

---

## 6. CallableStatement（存储过程调用）

### 6.1 调用无参数存储过程

```sql
-- MySQL 存储过程
DELIMITER //
CREATE PROCEDURE get_user_count()
BEGIN
    SELECT COUNT(*) AS total FROM users;
END //
DELIMITER ;
```

```java
try (CallableStatement cs = conn.prepareCall("{CALL get_user_count()}")) {
    try (ResultSet rs = cs.executeQuery()) {
        if (rs.next()) {
            long total = rs.getLong("total");
            System.out.println("用户总数: " + total);
        }
    }
}
```

### 6.2 调用带参数的存储过程

```sql
DELIMITER //
CREATE PROCEDURE get_user_by_id(IN user_id INT, OUT username VARCHAR(50))
BEGIN
    SELECT name INTO username FROM users WHERE id = user_id;
END //
DELIMITER ;
```

```java
try (CallableStatement cs = conn.prepareCall("{CALL get_user_by_id(?, ?)}")) {
    // 设置输入参数
    cs.setInt(1, 100);

    // 注册输出参数（必须在执行之前）
    cs.registerOutParameter(2, Types.VARCHAR);

    // 执行
    cs.execute();

    // 获取输出参数
    String username = cs.getString(2);
    System.out.println("用户名: " + username);
}
```

### 6.3 存储过程的优缺点

| 优点 | 缺点 |
|------|------|
| 减少网络传输（业务逻辑在数据库执行） | 难以调试和版本控制 |
| 执行速度快（预编译 + 数据库内部执行） | 移植性差（各数据库语法不同） |
| 封装复杂业务逻辑 | 逻辑分散在应用和数据库层 |
| 权限控制更灵活 | 扩展性差（数据库是瓶颈） |

> **现代开发建议：** 通常将业务逻辑放在应用层（Java），数据库只负责数据存储和查询。仅在数据一致性要求极高或需要数据库特定优化的场景使用存储过程。

---

## 7. ResultSet 与结果集处理

### 7.1 ResultSet 遍历

```java
String sql = "SELECT id, username, email, age, created_at FROM users WHERE status = ?";

try (PreparedStatement ps = conn.prepareStatement(sql)) {
    ps.setInt(1, 1);
    try (ResultSet rs = ps.executeQuery()) {

        // 方式 1：按列名读取（推荐，可读性好）
        while (rs.next()) {
            Long id = rs.getLong("id");
            String username = rs.getString("username");
            String email = rs.getString("email");
            int age = rs.getInt("age");
            Timestamp createdAt = rs.getTimestamp("created_at");
            System.out.printf("%d | %s | %s | %d | %s%n",
                id, username, email, age, createdAt);
        }

        // 方式 2：按列索引读取（性能略高，但可读性差）
        // rs.getLong(1), rs.getString(2), rs.getString(3)
    }
}
```

### 7.2 ResultSet 类型

```java
// 创建不同类型的 ResultSet

// 1. TYPE_FORWARD_ONLY（默认）：只能向前滚动
Statement stmt1 = conn.createStatement(
    ResultSet.TYPE_FORWARD_ONLY,
    ResultSet.CONCUR_READ_ONLY
);

// 2. TYPE_SCROLL_INSENSITIVE：可前后滚动，不感知数据变更
Statement stmt2 = conn.createStatement(
    ResultSet.TYPE_SCROLL_INSENSITIVE,
    ResultSet.CONCUR_READ_ONLY
);

// 3. TYPE_SCROLL_SENSITIVE：可前后滚动，感知数据变更（MySQL 不支持）
Statement stmt3 = conn.createStatement(
    ResultSet.TYPE_SCROLL_SENSITIVE,
    ResultSet.CONCUR_UPDATABLE
);

// 可滚动 ResultSet 的方法
try (Statement stmt = conn.createStatement(
        ResultSet.TYPE_SCROLL_INSENSITIVE,
        ResultSet.CONCUR_READ_ONLY);
     ResultSet rs = stmt.executeQuery("SELECT id FROM users")) {

    rs.absolute(5);        // 跳到第 5 行
    rs.relative(-2);       // 向前跳 2 行
    rs.first();            // 第 1 行
    rs.last();             // 最后 1 行
    rs.beforeFirst();      // 第一行之前
    rs.afterLast();        // 最后一行之后

    int rowCount = rs.last() ? rs.getRow() : 0;  // 获取总行数
    System.out.println("总行数: " + rowCount);
}
```

### 7.3 ResultSet 转 POJO（ORM 基础）

```java
public class User {
    private Long id;
    private String username;
    private String email;
    private int age;
    private LocalDateTime createdAt;

    // getters/setters 省略
}

// 手动映射（ORM 框架如 MyBatis 自动完成此过程）
public List<User> mapResultSetToUsers(ResultSet rs) throws SQLException {
    List<User> users = new ArrayList<>();
    while (rs.next()) {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setAge(rs.getInt("age"));

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            user.setCreatedAt(ts.toLocalDateTime());
        }

        users.add(user);
    }
    return users;
}
```

### 7.4 fetchSize 优化

```java
// fetchSize 控制每次从数据库读取的行数
// 默认：MySQL 驱动默认一次读取所有结果（内存压力大）

// 全量查询场景：设置合适的 fetchSize 避免 OOM
String sql = "SELECT * FROM large_table WHERE 1=1";

try (PreparedStatement ps = conn.prepareStatement(sql)) {
    // MySQL 需设置为 Integer.MIN_VALUE 才能启用流式读取
    ps.setFetchSize(Integer.MIN_VALUE);  // 逐行读取
    // ps.setFetchSize(1000);            // 每批 1000 行

    try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            // 处理每一行（不会一次性加载到内存）
        }
    }
}

// ★ 注意：
// setFetchSize 只是"建议"，不同驱动的实现不同
// MySQL 驱动：
//   fetchSize > 0 → 分批加载（内存控制）
//   fetchSize = Integer.MIN_VALUE → 逐行流式读取（但会占用连接）
//   fetchSize = 0 → 一次性全部加载（默认）
```

---

## 8. 事务管理

### 8.1 事务操作基础

```java
Connection conn = null;
try {
    conn = getConnection();

    // 1. 关闭自动提交（开启事务）
    conn.setAutoCommit(false);

    // 2. 执行多个 SQL
    String sql1 = "UPDATE accounts SET balance = balance - 1000 WHERE id = 1";
    String sql2 = "UPDATE accounts SET balance = balance + 1000 WHERE id = 2";

    try (PreparedStatement ps1 = conn.prepareStatement(sql1);
         PreparedStatement ps2 = conn.prepareStatement(sql2)) {

        ps1.executeUpdate();
        ps2.executeUpdate();
    }

    // 3. 提交事务
    conn.commit();
    System.out.println("转账成功");

} catch (SQLException e) {
    // 4. 回滚事务
    if (conn != null) {
        try {
            conn.rollback();
            System.out.println("转账失败，已回滚");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    e.printStackTrace();
} finally {
    // 5. 恢复自动提交
    if (conn != null) {
        try {
            conn.setAutoCommit(true);
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

### 8.2 设置事务隔离级别

```java
// MySQL 支持的隔离级别
// Connection.TRANSACTION_READ_UNCOMMITTED  = 1
// Connection.TRANSACTION_READ_COMMITTED    = 2
// Connection.TRANSACTION_REPEATABLE_READ   = 4  （MySQL 默认）
// Connection.TRANSACTION_SERIALIZABLE      = 8

try (Connection conn = getConnection()) {
    // 设置隔离级别
    conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

    conn.setAutoCommit(false);

    // ... 执行事务操作 ...

    conn.commit();
}
```

### 8.3 Savepoint（保存点）

```java
// Savepoint 允许回滚到事务中的某个点，而不是全部回滚

try (Connection conn = getConnection()) {
    conn.setAutoCommit(false);

    PreparedStatement ps1 = conn.prepareStatement("UPDATE accounts SET balance = balance - 1000 WHERE id = 1");
    ps1.executeUpdate();

    // 设置保存点
    Savepoint sp1 = conn.setSavepoint("after_debit");

    PreparedStatement ps2 = conn.prepareStatement("UPDATE accounts SET balance = balance + 1000 WHERE id = 2");
    ps2.executeUpdate();

    // 发现 id=2 不存在
    // 回滚到保存点（id=1 的扣款不会回滚）
    conn.rollback(sp1);

    // 提交（id=1 的扣款提交，id=2 的加款回滚）
    conn.commit();

    // 注意：这会导致数据不一致问题！
    // 实际应该记录错误并通知人工处理
}
```

### 8.4 事务超时

```java
// JDBC 没有直接的事务超时 API
// 可通过 Statement.setQueryTimeout() 间接控制

try (Statement stmt = conn.createStatement()) {
    stmt.setQueryTimeout(30);  // 30 秒超时
    stmt.execute("UPDATE ...");
}
// 超时后抛出 SQLException
```

### 8.5 事务的底层原理

```
JDBC 事务底层对应 MySQL 的 BEGIN/COMMIT/ROLLBACK：

conn.setAutoCommit(false)  → 实际发送：SET autocommit=0
conn.commit()              → 实际发送：COMMIT
conn.rollback()            → 实际发送：ROLLBACK
conn.setSavepoint("sp1")   → 实际发送：SAVEPOINT sp1
conn.rollback(sp1)         → 实际发送：ROLLBACK TO SAVEPOINT sp1
```

---

## 9. 批量操作

### 9.1 批量插入

```java
// 批量操作：减少网络往返次数，显著提升性能

String sql = "INSERT INTO users(username, email) VALUES(?, ?)";

try (Connection conn = getConnection();
     PreparedStatement ps = conn.prepareStatement(sql)) {

    // 先禁用自动提交
    conn.setAutoCommit(false);

    // 准备数据
    for (int i = 0; i < 10000; i++) {
        ps.setString(1, "user_" + i);
        ps.setString(2, "user_" + i + "@example.com");
        ps.addBatch();  // 添加到批量

        // 每 1000 条执行一次批量，避免一次太多
        if (i % 1000 == 0) {
            ps.executeBatch();
            ps.clearBatch();
        }
    }

    // 执行剩余的 batch
    ps.executeBatch();

    // 统一提交
    conn.commit();

}
```

### 9.2 批量操作的性能对比

```java
// 测试：插入 10000 条数据
// 环境：MySQL 8.0, 本地连接

// 方式 1：逐条插入（性能最差）
for (int i = 0; i < 10000; i++) {
    ps.setString(1, "user_" + i);
    ps.executeUpdate();  // 每次发送一个请求到数据库
}
// 耗时：约 15000ms（10000 次网络往返）

// 方式 2：批量插入（无 rewriteBatchedStatements）
ps.addBatch();  // JDBC 驱动模拟批处理，仍然逐条发送
ps.executeBatch();
// 耗时：约 8000ms（仍然逐条发送，只是减少了 JDBC 层开销）

// 方式 3：批量插入 + rewriteBatchedStatements=true（推荐）
// URL 参数：?rewriteBatchedStatements=true
// MySQL 驱动会将多条 INSERT 合并为一条多值 INSERT
// INSERT INTO users VALUES (?,?), (?,?), (?,?) ...
// 耗时：约 300ms（一次 SQL 插入 10000 行）
```

### 9.3 批量更新与删除

```java
// 批量更新
String updateSql = "UPDATE users SET status = ? WHERE id = ?";
try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
    conn.setAutoCommit(false);

    for (Long id : userIds) {
        ps.setInt(1, 1);  // status
        ps.setLong(2, id);
        ps.addBatch();
    }

    int[] results = ps.executeBatch();
    conn.commit();

    // 检查每批的执行结果
    int totalUpdated = 0;
    for (int result : results) {
        if (result > 0) totalUpdated += result;
    }
    System.out.println("共更新 " + totalUpdated + " 行");
}

// 批量删除
String deleteSql = "DELETE FROM users WHERE id = ?";
try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
    conn.setAutoCommit(false);

    for (Long id : deleteIds) {
        ps.setLong(1, id);
        ps.addBatch();
    }

    ps.executeBatch();
    conn.commit();
}
```

---

## 10. BLOB 和 CLOB 处理

### 10.1 BLOB（二进制大对象）

```java
// BLOB：存储二进制数据（图片、文件等）
// TINYBLOB(255B) / BLOB(64KB) / MEDIUMBLOB(16MB) / LONGBLOB(4GB)

// 写入 BLOB
String sql = "INSERT INTO files(filename, content, size) VALUES(?, ?, ?)";

try (PreparedStatement ps = conn.prepareStatement(sql)) {
    ps.setString(1, "photo.jpg");

    // 从文件读取二进制数据
    File file = new File("C:/photos/photo.jpg");
    FileInputStream fis = new FileInputStream(file);

    // 方式 1：直接设置二进制流
    ps.setBinaryStream(2, fis, (int) file.length());

    // 方式 2：设置字节数组
    // ps.setBytes(2, Files.readAllBytes(file.toPath()));

    ps.setLong(3, file.length());
    ps.executeUpdate();
}

// 读取 BLOB
String readSql = "SELECT filename, content, size FROM files WHERE id = ?";
try (PreparedStatement ps = conn.prepareStatement(readSql)) {
    ps.setInt(1, 1);
    try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
            String filename = rs.getString("filename");

            // 方式 1：读取为 InputStream
            try (InputStream is = rs.getBinaryStream("content");
                 FileOutputStream fos = new FileOutputStream("download/" + filename)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            // 方式 2：读取为字节数组
            // byte[] content = rs.getBytes("content");
        }
    }
}
```

### 10.2 CLOB（字符大对象）

```java
// CLOB：存储大量文本数据
// TINYTEXT(255B) / TEXT(64KB) / MEDIUMTEXT(16MB) / LONGTEXT(4GB)

// 写入 CLOB
String sql = "INSERT INTO articles(title, content) VALUES(?, ?)";

try (PreparedStatement ps = conn.prepareStatement(sql)) {
    ps.setString(1, "Article Title");
    ps.setCharacterStream(2, new StringReader(longText), longText.length());
    // 或
    // ps.setString(2, longText);
    ps.executeUpdate();
}

// 读取 CLOB
String readSql = "SELECT content FROM articles WHERE id = ?";
try (PreparedStatement ps = conn.prepareStatement(readSql)) {
    ps.setInt(1, 1);
    try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
            // 方式 1：读取为字符串
            String content = rs.getString("content");

            // 方式 2：读取为 Reader
            try (Reader reader = rs.getCharacterStream("content")) {
                char[] buffer = new char[8192];
                StringBuilder sb = new StringBuilder();
                int charsRead;
                while ((charsRead = reader.read(buffer)) != -1) {
                    sb.append(buffer, 0, charsRead);
                }
                String contentFromReader = sb.toString();
            }
        }
    }
}
```

### 10.3 BLOB/CLOB 最佳实践

```sql
-- 1. 尽量不要把大文件存储在数据库中
--    文件系统的流式读取效率远高于数据库 BLOB
--    数据库存储文件路径，文件存在磁盘或对象存储（OSS/S3）

-- 2. TEXT/BLOB 不能有默认值，不能直接参与排序和索引
--    如果需要搜索，使用全文索引或搜索引擎

-- 3. InnoDB 将 TEXT/BLOB 溢出到单独页存储
--    访问这些字段需要额外的 IO

-- 4. 建议：单独建表存储大字段
ALTER TABLE users DROP COLUMN avatar;  -- 从用户表移除
CREATE TABLE user_files (
    user_id INT PRIMARY KEY,
    file_type VARCHAR(20),
    file_path VARCHAR(500),  -- 存储文件路径
    file_size BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

## 11. RowSet

### 11.1 RowSet 概述

RowSet 是 JDBC 2.0 引入的扩展接口，它包装了 ResultSet，提供了更灵活的数据访问方式。

```java
// RowSet 层级结构
// javax.sql.RowSet (接口)
//   ├── JdbcRowSet: 保持数据库连接（connected）
//   └── CachedRowSet: 断开数据库连接（disconnected）
//       ├── WebRowSet: 可序列化为 XML
//       ├── FilteredRowSet: 支持过滤
//       └── JoinRowSet: 支持 JOIN
```

### 11.2 JdbcRowSet 示例

```java
// JdbcRowSet：保持数据库连接，可以滚动和更新

JdbcRowSet jdbcRs = new JdbcRowSetImpl();
jdbcRs.setUrl("jdbc:mysql://localhost:3306/mydb");
jdbcRs.setUsername("root");
jdbcRs.setPassword("password");
jdbcRs.setCommand("SELECT id, username, email FROM users WHERE status = ?");
jdbcRs.setInt(1, 1);
jdbcRs.execute();

// 导航
while (jdbcRs.next()) {
    int id = jdbcRs.getInt("id");
    String username = jdbcRs.getString("username");
    System.out.println(id + ": " + username);
}

// 滚动到第一行
jdbcRs.first();
```

### 11.3 CachedRowSet 示例

```java
// CachedRowSet：断开式数据集，取完数据后可关闭连接
// 适合在服务层获取数据后，在展示层使用（无需保持连接）

CachedRowSet cachedRs = new CachedRowSetImpl();

// 1. 设置连接信息
cachedRs.setUrl("jdbc:mysql://localhost:3306/mydb");
cachedRs.setUsername("root");
cachedRs.setPassword("password");
cachedRs.setCommand("SELECT id, username, email FROM users WHERE status = ?");
cachedRs.setInt(1, 1);

// 2. 获取数据（此时需要连接）
cachedRs.execute();

// 3. 关闭连接（数据在内存中）
// 连接池可以回收连接

// 4. 在断开状态下使用数据
while (cachedRs.next()) {
    System.out.println(cachedRs.getString("username"));
}

// 5. 修改并同步回数据库
cachedRs.beforeFirst();
while (cachedRs.next()) {
    if ("old_email".equals(cachedRs.getString("email"))) {
        cachedRs.updateString("email", "new_email@example.com");
        cachedRs.updateRow();
    }
}
// 6. 重新连接并提交修改
cachedRs.acceptChanges();
```

---

## 12. 元数据（Metadata）

### 12.1 DatabaseMetaData

```java
// 获取数据库本身的元信息

try (Connection conn = getConnection()) {
    DatabaseMetaData meta = conn.getMetaData();

    // 数据库基本信息
    System.out.println("数据库产品名: " + meta.getDatabaseProductName());
    System.out.println("数据库版本: " + meta.getDatabaseProductVersion());
    System.out.println("驱动名: " + meta.getDriverName());
    System.out.println("驱动版本: " + meta.getDriverVersion());
    System.out.println("JDBC 版本: " + meta.getJDBCMajorVersion() + "." + meta.getJDBCMinorVersion());
    System.out.println("URL: " + meta.getURL());
    System.out.println("用户名: " + meta.getUserName());

    // 获取所有表
    try (ResultSet tables = meta.getTables(null, null, "%", new String[]{"TABLE"})) {
        System.out.println("=== 所有表 ===");
        while (tables.next()) {
            String tableName = tables.getString("TABLE_NAME");
            String tableType = tables.getString("TABLE_TYPE");
            String remarks = tables.getString("REMARKS");
            System.out.printf("  %s (%s) - %s%n", tableName, tableType, remarks);
        }
    }

    // 获取指定表的列信息
    try (ResultSet columns = meta.getColumns(null, null, "users", "%")) {
        System.out.println("=== users 表的列 ===");
        while (columns.next()) {
            String colName = columns.getString("COLUMN_NAME");
            String colType = columns.getString("TYPE_NAME");
            int colSize = columns.getInt("COLUMN_SIZE");
            boolean nullable = columns.getInt("NULLABLE") == 1;
            String def = columns.getString("COLUMN_DEF");
            String remarks = columns.getString("REMARKS");
            System.out.printf("  %s %s(%d) nullable=%s default=%s - %s%n",
                colName, colType, colSize, nullable, def, remarks);
        }
    }

    // 获取主键信息
    try (ResultSet pk = meta.getPrimaryKeys(null, null, "users")) {
        System.out.println("=== users 表主键 ===");
        while (pk.next()) {
            String pkCol = pk.getString("COLUMN_NAME");
            short seq = pk.getShort("KEY_SEQ");
            System.out.printf("  列: %s (顺序: %d)%n", pkCol, seq);
        }
    }
}
```

### 12.2 ResultSetMetaData

```java
// 获取结果集元信息（常用于通用查询工具）

String sql = "SELECT * FROM users WHERE id = ?";
try (PreparedStatement ps = conn.prepareStatement(sql)) {
    ps.setInt(1, 1);
    try (ResultSet rs = ps.executeQuery()) {
        ResultSetMetaData meta = rs.getMetaData();

        // 列数
        int columnCount = meta.getColumnCount();
        System.out.println("列数: " + columnCount);

        // 每一列的信息
        for (int i = 1; i <= columnCount; i++) {
            String name = meta.getColumnName(i);
            String type = meta.getColumnTypeName(i);
            String className = meta.getColumnClassName(i);
            int displaySize = meta.getColumnDisplaySize(i);
            boolean nullable = meta.isNullable(i) == ResultSetMetaData.columnNullable;

            System.out.printf("列 %d: %s - %s (%s) size=%d nullable=%s%n",
                i, name, type, className, displaySize, nullable);
        }

        // 通用结果集转 JSON（ORM 框架的基础）
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                String colName = meta.getColumnName(i);
                Object value = rs.getObject(i);
                row.put(colName, value);
            }
            System.out.println(row);
        }
    }
}
```

---

## 13. 连接池原理与动机

### 13.1 为什么需要连接池？

```
不使用连接池时，每次数据库操作：
  ┌─────────────────────────────────────────────┐
  │  TCP 连接（三次握手）    ~1ms                │
  │  MySQL 认证（SSL + 密码） ~10-50ms           │
  │  执行 SQL                 ~1-100ms           │
  │  关闭连接                  ~1ms              │
  │  合计：每次操作的 50%+ 时间花费在连接/关闭上    │
  └─────────────────────────────────────────────┘

使用连接池时：
  ┌─────────────────────────────────────────────┐
  │  启动时：创建 N 个连接（一次性开销）           │
  │                                            │
  │  请求 1：从池中获取已存在的连接  ~0ms         │
  │          执行 SQL             ~1-100ms       │
  │          将连接归还池                       │
  │                                            │
  │  请求 N：从池中获取连接（复用）  ~0ms         │
  │                                            │
  │  连接池的核心价值：复用昂贵的数据库连接         │
  └─────────────────────────────────────────────┘
```

### 13.2 连接池的核心功能

```
连接池的核心组件：
┌──────────────────────────────────────────────────────────┐
│                    Connection Pool                        │
│                                                           │
│  连接池初始化 ─→ 创建最小空闲连接                          │
│                                                           │
│  获取连接时：                                              │
│    ├── 池中有空闲连接 ─→ 直接返回                           │
│    └── 池中无空闲连接 ─→                                   │
│        ├── 未达最大连接数 ─→ 创建新连接                     │
│        └── 已达最大连接数 ─→ 等待（超时抛出异常）            │
│                                                           │
│  归还连接时：                                              │
│    ├── 空闲连接未达最小空闲数 ─→ 保留在池中                  │
│    └── 空闲连接超时 ─→ 关闭连接                             │
│                                                           │
│  连接维护：                                                │
│    ├── 空闲连接存活检测（防止被 MySQL 服务器断开）            │
│    ├── 连接超时回收（关闭长期不用的连接）                     │
│    └── 坏连接剔除（检测到连接断开后移除）                     │
└──────────────────────────────────────────────────────────┘
```

### 13.3 简单连接池实现（演示原理）

```java
public class SimpleConnectionPool {
    private final String url;
    private final String username;
    private final String password;
    private final int maxSize;
    private final List<Connection> pool = new ArrayList<>();
    private final List<Connection> used = new ArrayList<>();
    private static final int DEFAULT_MAX_SIZE = 10;

    public SimpleConnectionPool(String url, String username, String password) {
        this(url, username, password, DEFAULT_MAX_SIZE);
    }

    public SimpleConnectionPool(String url, String username, String password, int maxSize) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.maxSize = maxSize;
        initialize();
    }

    private void initialize() {
        // 创建初始连接
        for (int i = 0; i < Math.min(5, maxSize); i++) {
            pool.add(createConnection());
        }
    }

    private Connection createConnection() {
        try {
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            throw new RuntimeException("创建连接失败", e);
        }
    }

    public synchronized Connection getConnection() throws SQLException {
        // 有空闲连接则直接返回
        if (!pool.isEmpty()) {
            Connection conn = pool.remove(pool.size() - 1);
            used.add(conn);
            return conn;
        }

        // 没满则创建新连接
        if (used.size() < maxSize) {
            Connection conn = createConnection();
            used.add(conn);
            return conn;
        }

        // 已满则等待（简化实现：直接抛出异常）
        throw new SQLException("连接池已满，最大连接数: " + maxSize);
    }

    public synchronized void releaseConnection(Connection conn) {
        if (conn != null) {
            used.remove(conn);
            pool.add(conn);
        }
    }

    public synchronized void close() {
        for (Connection conn : pool) {
            closeQuietly(conn);
        }
        for (Connection conn : used) {
            closeQuietly(conn);
        }
        pool.clear();
        used.clear();
    }

    private void closeQuietly(Connection conn) {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            // ignore
        }
    }
}
```

**生产级连接池比以上实现复杂得多，需要处理：**
- 线程安全（并发获取/归还）
- 连接有效性检测
- 连接超时/泄漏检测
- 动态扩容/缩容
- 中断等待线程
- 连接属性继承
- 指标监控

---

## 14. HikariCP 深度解析

### 14.1 为什么 HikariCP 最快？

HikariCP 是 Spring Boot 2.x/3.x 的默认连接池，也是目前**性能最快的 Java 连接池**。

```text
HikariCP 性能优化核心技术：

1. ConcurrentBag（无锁集合）
   ├── 核心数据结构：ThreadLocal + CopyOnWriteArrayList + LinkedBlockingQueue
   ├── 无锁读取：同一线程归还的连接直接从 ThreadLocal 获取（零竞争）
   ├── 窃取机制：本地没有时从全局队列窃取
   └── CAS 操作替代 synchronized

2. fastList（紧凑的 ArrayList）
   ├── 替代 ArrayList.remove() 的 O(n) 扫描
   ├── 逆序扫描（最近使用的连接最可能被归还）
   └── 减少边界检查（去除了 ArrayList 的 rangeCheck）

3. 优化字节码
   ├── 使用 Java 代理（delegate）而不是继承
   ├── 避免 Instrumentation 的开销
   └── 代码极致精简（核心 jar 仅 150KB）

4. 精心设计的代理类
   ├── ConnectionProxy、StatementProxy 等
   ├── 最小化代理方法的数量
   └── 每个方法只做必要的拦截
```

### 14.2 HikariCP 配置详解

```java
// Java 配置方式
HikariConfig config = new HikariConfig();

// 基本配置
config.setJdbcUrl("jdbc:mysql://localhost:3306/mydb");
config.setUsername("root");
config.setPassword("password");
config.setDriverClassName("com.mysql.cj.jdbc.Driver");

// ========== 核心参数 ==========

// 最大连接数（池中允许的最大连接数）
config.setMaximumPoolSize(20);
// 默认：10
// 原则：不是越大越好！过多连接会导致数据库上下文切换增加
// 经验公式：CPU 核心数 * 2 + 有效磁盘数
// 举例：4 核 CPU + SSD → 推荐 8-12

// 最小空闲连接数
config.setMinimumIdle(5);
// 默认：等于 maximumPoolSize
// 原则：保持在 "日常峰值" 的 60% 左右

// 连接超时（获取连接的等待时间）
config.setConnectionTimeout(30000);
// 默认：30 秒
// 如果 30 秒内获取不到连接，抛出 SQLException

// 空闲超时（连接在池中闲置多久后被回收）
config.setIdleTimeout(600000);
// 默认：10 分钟（600000ms）
// 注意：只有 minimumIdle < maximumPoolSize 时才生效

// 最大存活时间（连接在池中的最大存活时间）
config.setMaxLifetime(1800000);
// 默认：30 分钟（1800000ms）
// 建议比数据库的 wait_timeout 短 10-30 秒
// MySQL 默认 wait_timeout = 28800 秒（8 小时）

// ========== 连接验证 ==========

// 验证超时
config.setValidationTimeout(5000);
// 默认：5 秒
// 验证连接有效性的最大等待时间

// 连接测试查询
config.setConnectionTestQuery("SELECT 1");
// 默认：无（使用 JDBC4 的 Connection.isValid()）
// MySQL 驱动支持 isValid()，通常不需要设置此参数

// ========== 高级参数 ==========

// 泄露检测阈值
config.setLeakDetectionThreshold(60000);
// 默认：0（不检测）
// 如果连接被占用超过此时间，会输出警告日志（帮助定位连接泄漏）

// 连接初始化 SQL
config.setConnectionInitSql("SET NAMES utf8mb4");
// 连接创建时自动执行的 SQL

// 池名称（方便监控）
config.setPoolName("MyAppPool");

// 是否注册 JMX 监控
config.setRegisterMbeans(true);

// 创建数据源
HikariDataSource dataSource = new HikariDataSource(config);
```

**Spring Boot 配置文件示例：**

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      connection-test-query: SELECT 1
      pool-name: MyHikariPool
      leak-detection-threshold: 60000
```

### 14.3 HikariCP 连接验证机制

```
HikariCP 的连接验证流程：

1. 获取连接时验证（默认关闭）
   └── connectionTestQuery 或 isValid() 检查

2. 空闲连接验证（后台线程定期检查）
   ├── HouseKeeper 线程每分钟执行一次
   ├── 检查所有空闲连接的有效性
   └── 移除无效连接并补充新连接

3. 连接超时处理
   ├── 连接超过 maxLifetime → 关闭并从池中移除
   └── 连接超过 idleTimeout → 关闭并缩容至 minimumIdle
```

### 14.4 连接泄漏检测

```java
// 配置连接泄漏检测
HikariConfig config = new HikariConfig();
config.setLeakDetectionThreshold(60000); // 60 秒

// 当连接被获取后超过 60 秒未归还，日志会输出：
// WARN  - Connection leak detection triggered for ...
// Stack Trace:
//   at com.example.dao.UserDao.getUser(UserDao.java:25)
//   at com.example.service.UserService.getUser(UserService.java:15)
//   ...

// 使用 try-with-resources 确保连接自动归还
// 推荐写法：
try (Connection conn = dataSource.getConnection();
     PreparedStatement ps = conn.prepareStatement(sql)) {
    // 使用连接
} // 自动关闭连接，归还到池
```

### 14.5 HikariCP 监控指标

```java
// 通过 JMX 或 HikariPoolMXBean 监控
HikariDataSource ds = new HikariDataSource(config);

// 编程方式获取指标
HikariPoolMXBean poolMXBean = ds.getHikariPoolMXBean();

System.out.println("活跃连接数: " + poolMXBean.getActiveConnections());
System.out.println("空闲连接数: " + poolMXBean.getIdleConnections());
System.out.println("等待线程数: " + poolMXBean.getThreadsAwaitingConnection());
System.out.println("总连接数: " + poolMXBean.getTotalConnections());

// 或通过 Actuator 暴露（Spring Boot）
// GET /actuator/metrics/hikaricp.connections.active
// GET /actuator/metrics/hikaricp.connections.idle
// GET /actuator/metrics/hikaricp.connections.max
```

---

## 15. Druid 连接池

### 15.1 Druid 概述

阿里 Druid 是另一个广泛使用的连接池，特色是**强大的监控能力和 SQL 防火墙**。

```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid-spring-boot-starter</artifactId>
    <version>1.2.20</version>
</dependency>
```

### 15.2 Druid 配置

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver
    type: com.alibaba.druid.pool.DruidDataSource

    druid:
      # 连接池配置
      initial-size: 5
      min-idle: 5
      max-active: 20
      max-wait: 30000
      time-between-eviction-runs-millis: 60000
      min-evictable-idle-time-millis: 300000
      validation-query: SELECT 1
      test-while-idle: true
      test-on-borrow: false
      test-on-return: false

      # 监控配置
      stat-view-servlet:
        enabled: true
        url-pattern: /druid/*
        login-username: admin
        login-password: admin
        allow: 127.0.0.1

      # 过滤器
      filter:
        stat:
          enabled: true
          log-slow-sql: true
          slow-sql-millis: 2000
        wall:
          enabled: true
          config:
            delete-allow: false  # 禁止 DELETE 语句
            drop-table-allow: false  # 禁止 DROP TABLE
```

### 15.3 Druid SQL 防火墙（WallFilter）

```java
// WallFilter 可以阻止危险的 SQL 操作

// 配置示例
WallConfig wallConfig = new WallConfig();
wallConfig.setDeleteAllow(false);           // 禁止 DELETE
wallConfig.setDropTableAllow(false);        // 禁止 DROP TABLE
wallConfig.setTruncateAllow(false);         // 禁止 TRUNCATE
wallConfig.setSelelctAllow(true);           // 允许 SELECT
wallConfig.setUpdateAllow(true);            // 允许 UPDATE

// 阻止 SQL 注入
wallConfig.setMultiStatementAllow(false);   // 禁止多语句执行
wallConfig.setConditionLikeTrueAllow(false);// 禁止 LIKE 永真条件
wallConfig.setConditionAndOrAllow(false);   // 禁止 AND/OR 永真

// 常见被拦截的 SQL
// DROP TABLE users;                   → 被拦截
// DELETE FROM users;                  → 被拦截
// SELECT * FROM users WHERE 1=1;      → 被拦截
// '; UPDATE users SET role='admin'    → 被拦截
```

### 15.4 Druid StatFilter（SQL 统计）

```java
// StatFilter 收集 SQL 执行的详细统计

// 监控页面：http://localhost:8080/druid
//   ├── 数据源：连接池状态
//   ├── SQL 监控：每个 SQL 的执行次数、耗时、并发
//   ├── URI 监控：每个 URL 的访问情况
//   ├── Session 监控：活跃 Session
//   └── Spring 监控：Bean 调用情况

// SQL 监控列表示例：
// ┌──────────┬──────┬───────┬──────┬───────┬──────┐
// │ SQL      │执行  │总耗时 │最慢  │平均   │并发  │
// │          │次数  │(ms)   │(ms)  │(ms)   │最大  │
// ├──────────┼──────┼───────┼──────┼───────┼──────┤
// │SELECT *  │1000  │5000   │50    │5      │10    │
// │INSERT... │500   │10000  │200   │20     │5     │
// └──────────┴──────┴───────┴──────┴───────┴──────┘

// 慢查询自动记录（配置：slow-sql-millis=2000）
// 帮助 DBA 快速定位性能瓶颈
```

---

## 16. 其他连接池：DBCP2 与 Tomcat JDBC Pool

### 16.1 Apache DBCP2

```xml
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-dbcp2</artifactId>
    <version>2.11.0</version>
</dependency>
```

```java
// DBCP2 配置
BasicDataSource ds = new BasicDataSource();
ds.setUrl("jdbc:mysql://localhost:3306/mydb");
ds.setUsername("root");
ds.setPassword("password");
ds.setDriverClassName("com.mysql.cj.jdbc.Driver");

ds.setInitialSize(5);           // 初始连接数
ds.setMaxTotal(20);             // 最大连接数
ds.setMaxIdle(10);              // 最大空闲连接
ds.setMinIdle(5);               // 最小空闲连接
ds.setMaxWaitMillis(30000);     // 获取连接超时

// 连接验证
ds.setValidationQuery("SELECT 1");
ds.setTestOnBorrow(true);       // 获取连接时验证（性能影响大）
ds.setTestWhileIdle(true);      // 空闲时验证

// 连接池维护
ds.setTimeBetweenEvictionRunsMillis(60000);
ds.setMinEvictableIdleTimeMillis(300000);
```

### 16.2 Tomcat JDBC Pool

```java
// Tomcat JDBC Pool（Tomcat 内建连接池）
org.apache.tomcat.jdbc.pool.DataSource ds =
    new org.apache.tomcat.jdbc.pool.DataSource();

ds.setUrl("jdbc:mysql://localhost:3306/mydb");
ds.setUsername("root");
ds.setPassword("password");
ds.setDriverClassName("com.mysql.cj.jdbc.Driver");

ds.setInitialSize(5);
ds.setMaxActive(20);
ds.setMinIdle(5);
ds.setMaxIdle(10);
ds.setMaxWait(30000);

// 连接验证
ds.setValidationQuery("SELECT 1");
ds.setTestOnBorrow(true);
ds.setTestWhileIdle(true);

// 防止连接泄漏
ds.setRemoveAbandoned(true);            // 自动移除废弃连接
ds.setRemoveAbandonedTimeout(60);       // 超过 60 秒未归还视为泄漏
ds.setLogAbandoned(true);               // 记录泄漏时的堆栈
```

### 16.3 连接池对比总结

| 特性 | HikariCP | Druid | DBCP2 | Tomcat Pool |
|------|---------|-------|-------|-------------|
| **性能** | ★★★★★ | ★★★★ | ★★★ | ★★★★ |
| **监控** | 基本（JMX） | ★★★★★（内置监控页面） | 基本 | 基本 |
| **SQL 防火墙** | 无 | ★★★★★（WallFilter） | 无 | 无 |
| **配置简洁度** | ★★★★★ | ★★★★ | ★★★ | ★★★★ |
| **Spring Boot 支持** | 默认 | 需 starter | 需配置 | 较少使用 |
| **连接泄漏检测** | 支持 | 支持 | 支持 | 支持 |
| **活跃社区** | 高 | 高（阿里） | 低 | 中 |
| **生产推荐** | **优先推荐** | 阿里系项目推荐 | 较少 | Tomcat 环境 |

**选择建议：**
- **Spring Boot 通用项目**：HikariCP（默认，无需额外配置）
- **阿里系项目 + 需要 SQL 监控**：Druid
- **遗留项目**：根据现有配置选择

---

## 17. 分页查询实现

### 17.1 MySQL 分页

```sql
-- MySQL 分页语法
SELECT * FROM users
WHERE status = 1
ORDER BY id ASC
LIMIT 20 OFFSET 0;       -- 第 1 页，每页 20 条

SELECT * FROM users
WHERE status = 1
ORDER BY id ASC
LIMIT 20 OFFSET 20;      -- 第 2 页

SELECT * FROM users
WHERE status = 1
ORDER BY id ASC
LIMIT 20 OFFSET 40;      -- 第 3 页

-- LIMIT {offset}, {count} 语法
SELECT * FROM users LIMIT 0, 20;    -- 等价于 LIMIT 20 OFFSET 0
SELECT * FROM users LIMIT 20, 20;   -- 等价于 LIMIT 20 OFFSET 20
```

### 17.2 JDBC 分页实现

```java
// 封装分页查询
public class PageResult<T> {
    private List<T> content;      // 当前页数据
    private int page;             // 当前页码（从 0 开始）
    private int size;             // 每页条数
    private long totalElements;   // 总记录数
    private int totalPages;       // 总页数

    public PageResult(List<T> content, int page, int size, long totalElements) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = (int) Math.ceil((double) totalElements / size);
    }

    // getters...
}

// DAO 层分页查询
public PageResult<User> findUsersByPage(int page, int size, String status) {
    List<User> users = new ArrayList<>();
    long total = 0;

    // SQL：需要两条 SQL（一条查询数据，一条统计总数）
    String countSql = "SELECT COUNT(*) FROM users WHERE status = ?";
    String dataSql = "SELECT id, username, email, created_at FROM users "
                   + "WHERE status = ? ORDER BY id ASC LIMIT ? OFFSET ?";

    try (Connection conn = dataSource.getConnection();
         PreparedStatement countPs = conn.prepareStatement(countSql)) {

        // 1. 查询总数
        countPs.setString(1, status);
        try (ResultSet rs = countPs.executeQuery()) {
            if (rs.next()) {
                total = rs.getLong(1);
            }
        }

        // 2. 查询当前页数据
        try (PreparedStatement dataPs = conn.prepareStatement(dataSql)) {
            dataPs.setString(1, status);
            dataPs.setInt(2, size);
            dataPs.setInt(3, page * size);  // OFFSET = page * size

            try (ResultSet rs = dataPs.executeQuery()) {
                while (rs.next()) {
                    User user = new User();
                    user.setId(rs.getLong("id"));
                    user.setUsername(rs.getString("username"));
                    user.setEmail(rs.getString("email"));
                    user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    users.add(user);
                }
            }
        }

    } catch (SQLException e) {
        throw new RuntimeException("分页查询失败", e);
    }

    return new PageResult<>(users, page, size, total);
}

// 使用
PageResult<User> page = userDao.findUsersByPage(0, 20, "1");
System.out.printf("第 %d/%d 页，共 %d 条%n",
    page.getPage() + 1, page.getTotalPages(), page.getTotalElements());
```

---

## 18. DAO 模式与服务层事务

### 18.1 DAO 模式

```java
// 数据访问层：封装数据访问逻辑

public interface UserDao {
    User findById(Long id);
    User findByUsername(String username);
    List<User> findAll(int page, int size);
    long count();
    int insert(User user);
    int update(User user);
    int deleteById(Long id);
}

public class UserDaoImpl implements UserDao {
    private final DataSource dataSource;

    public UserDaoImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public User findById(Long id) {
        String sql = "SELECT id, username, email, password, role, status, created_at "
                   + "FROM users WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询用户失败, id=" + id, e);
        }
        return null;
    }

    @Override
    public int insert(User user) {
        String sql = "INSERT INTO users(username, password, email, role) VALUES(?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getEmail());
            ps.setInt(4, user.getRole());
            int rows = ps.executeUpdate();

            // 设置自动生成的主键
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        user.setId(rs.getLong(1));
                    }
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new RuntimeException("插入用户失败", e);
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setRole(rs.getInt("role"));
        user.setStatus(rs.getInt("status"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) user.setCreatedAt(ts.toLocalDateTime());
        return user;
    }
}
```

### 18.2 服务层事务管理

```java
// 服务层：负责业务逻辑和事务边界

public class UserService {
    private final UserDao userDao;
    private final DataSource dataSource;

    public UserService(DataSource dataSource) {
        this.dataSource = dataSource;
        this.userDao = new UserDaoImpl(dataSource);
    }

    // ★ 事务管理在 Service 层，而非 DAO 层
    // 一个业务方法可能调用多个 DAO 方法，这些操作应该在同一个事务中

    public void register(User user) {
        // 手动管理事务
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            // 1. 检查用户名是否已存在
            User existing = userDao.findByUsername(user.getUsername());
            if (existing != null) {
                throw new BusinessException("用户名已存在");
            }

            // 2. 加密密码
            user.setPassword(PasswordUtil.hash(user.getPassword()));

            // 3. 插入用户
            userDao.insert(user);

            // 4. 发送注册通知（在同一个事务中？取决于是否需要回滚）
            // notificationService.sendWelcomeEmail(user.getEmail());

            conn.commit();
        } catch (BusinessException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { /* ignore */ }
            }
            throw e;  // 重新抛出业务异常
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { /* ignore */ }
            }
            throw new RuntimeException("注册失败", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();  // 归还到连接池
                } catch (SQLException e) { /* ignore */ }
            }
        }
    }

    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        // 事务管理：转账操作必须在同一个事务中
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            // 设置隔离级别（避免不可重复读）
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            userDao.updateBalance(fromId, amount.negate());
            userDao.updateBalance(toId, amount);

            conn.commit();
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { /* ignore */ }
            }
            throw new RuntimeException("转账失败", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) { /* ignore */ }
            }
        }
    }
}
```

### 18.3 ThreadLocal 事务管理（进阶）

```java
// 通过 ThreadLocal 在同一线程中共享 Connection
// 实现 DAO 层自动参与 Service 层的事务

public class ConnectionHolder {
    private static final ThreadLocal<Connection> holder = new ThreadLocal<>();

    public static Connection getConnection() {
        return holder.get();
    }

    public static void setConnection(Connection conn) {
        holder.set(conn);
    }

    public static void remove() {
        holder.remove();
    }
}

// 使用 ThreadLocal 后的 DAO
public class UserDaoWithTransaction {
    public void insert(User user) {
        Connection conn = ConnectionHolder.getConnection();
        // 如果 conn 为 null，直接获取新连接（非事务模式）
        if (conn == null) {
            conn = dataSource.getConnection();
        }
        // ... 执行 SQL
    }
}

// Service 层开启事务
public class TransactionManager {
    public static void executeInTransaction(Runnable action) {
        Connection conn = dataSource.getConnection();
        try {
            conn.setAutoCommit(false);
            ConnectionHolder.setConnection(conn);

            action.run();  // 执行所有 DAO 操作

            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            ConnectionHolder.remove();
            conn.setAutoCommit(true);
            conn.close();
        }
    }
}

// 使用
transactionManager.executeInTransaction(() -> {
    userDao.insert(user);
    orderDao.createOrder(order);
    // 在同一个事务中
});
```

---

## 19. 企业级 JDBC 最佳实践

### 19.1 核心原则

| 原则 | 说明 |
|------|------|
| **使用 PreparedStatement** | 永远不用 Statement，防止 SQL 注入 |
| **try-with-resources** | 确保资源自动关闭，防止连接泄漏 |
| **使用连接池** | 绝不用 DriverManager 直接创建连接 |
| **事务在 Service 层** | DAO 层不管理事务，事务边界在 Service 层 |
| **设置 fetchSize** | 大结果集设置合理 fetchSize，防止 OOM |
| **批量操作用 addBatch** | 大批量数据使用批量操作，减少网络往返 |
| **硬编码配置外置** | 连接串、密码等配置外置到配置文件/环境变量 |
| **统一异常处理** | 将 SQLException 转为运行时异常或业务异常 |

### 19.2 推荐代码模板

```java
// 完整的 DAO 操作模板

public class UserDao {
    private final DataSource dataSource;

    public UserDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public User findById(Long id) {
        String sql = "SELECT id, username, email, role, status, created_at "
                   + "FROM users WHERE id = ?";

        // try-with-resources：自动关闭 Connection、PreparedStatement、ResultSet
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            // 将受检异常转为非受检异常
            throw new DataAccessException("查询用户失败, id=" + id, e);
        }

        return null;
    }

    public int batchInsert(List<User> users) {
        String sql = "INSERT INTO users(username, password, email) VALUES(?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);

            for (int i = 0; i < users.size(); i++) {
                User user = users.get(i);
                ps.setString(1, user.getUsername());
                ps.setString(2, user.getPassword());
                ps.setString(3, user.getEmail());
                ps.addBatch();

                // 每 500 条执行一次批量
                if (i > 0 && i % 500 == 0) {
                    ps.executeBatch();
                    ps.clearBatch();
                }
            }

            ps.executeBatch();
            conn.commit();

        } catch (SQLException e) {
            throw new DataAccessException("批量插入用户失败", e);
        }

        return users.size();
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setRole(rs.getInt("role"));
        user.setStatus(rs.getInt("status"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            user.setCreatedAt(ts.toLocalDateTime());
        }
        return user;
    }
}
```

### 19.3 配置加密

```java
// 永远不要在代码或配置文件中明码保存数据库密码

// 方案 1：使用环境变量
// application.properties
// db.password=${DB_PASSWORD}

// 方案 2：使用配置中心（Nacos/Apollo）

// 方案 3：使用加密工具
public class DecryptUtil {
    public static String decrypt(String encrypted) {
        // 使用 AES/RSA 解密
        // 密钥从密钥管理服务（KMS）获取
        return decryptedText;
    }
}

// 配置使用
config.setPassword(DecryptUtil.decrypt("ENC(xxx)"));
```

### 19.4 连接池监控告警

```java
// 定期检查连接池健康状态
public class PoolMonitor {
    private final HikariDataSource dataSource;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public PoolMonitor(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            HikariPoolMXBean pool = dataSource.getHikariPoolMXBean();

            int active = pool.getActiveConnections();
            int idle = pool.getIdleConnections();
            int waiting = pool.getThreadsAwaitingConnection();
            int total = pool.getTotalConnections();

            // 活跃连接超过 80% 告警
            if (total > 0 && (double) active / total > 0.8) {
                System.err.printf("[WARN] 连接池使用率过高: %d/%d (%.1f%%)%n",
                    active, total, (double) active / total * 100);
            }

            // 有线程等待连接告警
            if (waiting > 0) {
                System.err.printf("[WARN] 有 %d 个线程在等待连接%n", waiting);
            }

            System.out.printf("[INFO] 连接池: 活跃=%d, 空闲=%d, 等待=%d, 总计=%d%n",
                active, idle, waiting, total);

        }, 0, 30, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdown();
    }
}
```

---

## 20. 面试题

### 基础题

**Q1: JDBC 操作数据库的基本步骤？**

A: 七个步骤：
1. 加载驱动（JDBC 4.0+ 自动加载）
2. 获取连接（DriverManager.getConnection 或 DataSource）
3. 创建 Statement 或 PreparedStatement
4. 执行 SQL
5. 处理 ResultSet 结果集
6. 处理 SQLException 异常
7. 关闭资源（Connection、Statement、ResultSet）

**Q2: Statement 和 PreparedStatement 的区别？**

A:
- PreparedStatement 支持预编译，执行效率更高（尤其是多次执行相同 SQL）
- PreparedStatement 使用参数化查询（`?`），防止 SQL 注入
- PreparedStatement 代码可读性更好（类型明确）
- Statement 适合执行 DDL 或不需要参数的静态 SQL

**Q3: 什么是 SQL 注入？如何防止？**

A: SQL 注入是通过在输入中嵌入恶意 SQL 代码，改变原 SQL 语义的攻击方式。
防止方法：
- 使用 PreparedStatement 参数化查询（最有效）
- 对用户输入进行转义和校验
- 使用 ORM 框架（MyBatis、JPA）的参数化机制
- 使用连接池的 SQL 防火墙（如 Druid WallFilter）

**Q4: JDBC 中如何管理事务？**

A: 
```java
conn.setAutoCommit(false);   // 开启事务
// 执行多个 SQL
conn.commit();               // 提交事务
// 或
conn.rollback();             // 回滚事务
conn.setAutoCommit(true);    // 恢复自动提交
```

**Q5: 什么是数据库连接池？为什么要使用连接池？**

A: 连接池是维护一组数据库连接的容器，目的是复用连接，减少创建/销毁连接的开销。
优势：
- 减少连接创建时间（TCP 连接 + 认证很耗时）
- 控制最大连接数（防止数据库过载）
- 连接复用（提高性能）
- 自动维护（连接有效性检测、泄漏检测）

### 进阶题

**Q6: HikariCP 为什么快？**

A:
- **ConcurrentBag**：无锁数据结构，ThreadLocal 缓存 + CAS 操作
- **fastList**：优化的 ArrayList，逆序扫描减少查找成本
- **精简的代理类**：最小化方法拦截，只有必要的代理逻辑
- **优化字节码**：使用 Java 代理而不是 Instrumentation
- **代码极简**：核心 jar 仅 150KB，减少不必要的特性

**Q7: 如何获取 PreparedStatement 自动生成的主键？**

A: 
```java
PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
ps.executeUpdate();
ResultSet rs = ps.getGeneratedKeys();
if (rs.next()) {
    long id = rs.getLong(1);
}
```

**Q8: 批量插入时 rewriteBatchedStatements 的作用？**

A: 将多条独立的 INSERT 语句重写为一条多值 INSERT：
```sql
-- 没有 rewriteBatchedStatements：
INSERT INTO users VALUES (?,?);
INSERT INTO users VALUES (?,?);
INSERT INTO users VALUES (?,?);

-- 有 rewriteBatchedStatements：
INSERT INTO users VALUES (?,?),(?,?),(?,?);
```
性能提升明显（10000 行批量插入从 15000ms 降为 300ms）。

**Q9: JDBC 中如何处理大对象（BLOB/CLOB）？**

A:
- BLOB（二进制）：使用 `setBinaryStream()` / `getBinaryStream()` 或 `setBytes()` / `getBytes()`
- CLOB（字符）：使用 `setCharacterStream()` / `getCharacterStream()` 或 `setString()` / `getString()`
- 最佳实践：不要在大表中直接存大字段，存文件路径即可

**Q10: 什么是连接泄漏？如何检测和防止？**

A: 连接被获取后没有正确归还到连接池，导致连接池连接耗尽。
防止：
- 使用 try-with-resources 自动关闭连接
- 设置 HikariCP leakDetectionThreshold（超时预警）
- 监控连接池活跃连接数

**Q11: DataSource 和 DriverManager 有什么区别？**

A:
- DriverManager 直接创建物理连接（每次都新建）
- DataSource 通常通过连接池管理连接（从池中获取）
- DataSource 是 JNDI 资源，支持分布式事务
- 企业开发总是使用 DataSource

**Q12: JDBC 中的 getFetchSize 和 setFetchSize 的作用？**

A: 控制每次从数据库读取的行数。
- 默认（0）：一次读取所有结果
- 正数：分批读取（如 1000 条一批）
- `Integer.MIN_VALUE`（MySQL）：流式读取，逐行处理
- 合理设置可以避免大结果集导致的 OOM

**Q13: JDBC 连接 MySQL 时的常用参数有哪些？**

A:
- `useSSL=false`：关闭 SSL（开发环境）
- `serverTimezone=Asia/Shanghai`：时区
- `characterEncoding=utf8mb4`：编码
- `rewriteBatchedStatements=true`：批量优化
- `useServerPrepStmts=true`：服务端预编译
- `cachePrepStmts=true`：预编译缓存
- `connectTimeout=5000`：连接超时
- `socketTimeout=30000`：Socket 超时

**Q14: 如何实现一个简单的 ORM 框架？**

A: 核心思路：
1. 通过反射创建对象实例
2. 通过 ResultSetMetaData 获取列名
3. 通过反射调用 setter 方法设置属性值
4. 通过注解或 XML 映射 SQL 和实体类
5. 通过动态代理实现接口方法自动实现

**Q15: 分页查询在大偏移量时性能差，如何优化？**

A:
- 延迟 JOIN：先查主键再查数据
- 游标分页：基于上一页最后一条记录的 ID/时间，用 WHERE 条件代替 OFFSET
- 子查询优化：利用索引覆盖先查出主键

---

## 参考资源

- [JDBC 官方文档 (Oracle)](https://docs.oracle.com/javase/tutorial/jdbc/) -- Java 官方教程
- [MySQL Connector/J 文档](https://dev.mysql.com/doc/connector-j/8.0/en/) -- MySQL JDBC 驱动
- [HikariCP GitHub](https://github.com/brettwooldridge/HikariCP) -- 源码和配置说明
- [Druid GitHub](https://github.com/alibaba/druid) -- 阿里连接池和监控
- [Apache DBCP2 文档](https://commons.apache.org/proper/commons-dbcp/) -- DBCP2 参考

---

*最后更新: 2026-05-31*
