# JDBC：Java 数据库连接标准

## 概述

**JDBC**（Java Database Connectivity）是 Java 访问关系型数据库的标准 API，提供统一接口让 Java 程序连接 MySQL、Oracle、SQL Server 等不同数据库，实现增删改查（CRUD）操作。它是 Java 后端开发中**数据持久化的基础**，所有 ORM 框架（MyBatis、Hibernate）底层都基于 JDBC。

---

## 一、JDBC 核心组件

| 组件 | 作用 | 说明 |
|------|------|------|
| **DriverManager** | 管理数据库驱动，获取连接 | `DriverManager.getConnection(url, user, password)` |
| **Connection** | 数据库连接 | 稀缺资源，使用后必须关闭 |
| **Statement** | 执行静态 SQL | 存在 SQL 注入风险，不推荐直接使用 |
| **PreparedStatement** | 预编译 SQL，防注入 | 推荐使用，支持 `?` 占位符，执行效率更高 |
| **ResultSet** | 存储查询结果集 | 通过 `next()` 遍历，`getString()`/`getInt()` 等获取字段值 |
| **CallableStatement** | 调用存储过程 | 用于执行数据库存储过程 |

---

## 二、JDBC 操作流程

| 步骤 | 操作 | 代码示例 |
|:--:|------|---------|
| 1 | 加载驱动 | `Class.forName("com.mysql.cj.jdbc.Driver")`（MySQL 8.0 可省略） |
| 2 | 获取连接 | `DriverManager.getConnection(url, user, password)` |
| 3 | 创建 Statement | `conn.prepareStatement(sql)`（推荐 PreparedStatement） |
| 4 | 执行 SQL | 查询：`executeQuery()`；增删改：`executeUpdate()` |
| 5 | 处理结果集 | `while (rs.next()) { rs.getString("name") }` |
| 6 | 关闭资源 | 按顺序关闭：ResultSet → Statement → Connection（推荐 try-with-resources） |

---

## 三、PreparedStatement vs Statement

| 对比维度 | Statement | PreparedStatement |
|---------|-----------|-------------------|
| **SQL 注入防护** | ❌ 不安全 | ✅ 通过 `?` 占位符防止注入 |
| **预编译** | ❌ 每次编译 | ✅ 一次编译多次执行，效率更高 |
| **可读性** | 差（SQL 拼接） | 好（参数与 SQL 分离） |
| **推荐程度** | 不推荐 | **强烈推荐** |

---

## 四、核心代码示例

### 数据库准备

```sql
CREATE DATABASE IF NOT EXISTS testdb;
USE testdb;
CREATE TABLE IF NOT EXISTS user (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(20) NOT NULL,
    age INT
);
```

### Maven 依赖

```xml
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>
```

### 工具类：封装连接与资源释放

```java
public class JDBCUtil {
    private static final String URL = "jdbc:mysql://localhost:3306/testdb"
        + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = "123456";

    // 获取连接
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // 关闭资源（try-with-resources 可替代）
    public static void close(ResultSet rs, Statement stmt, Connection conn) {
        if (rs != null) try { rs.close(); } catch (SQLException e) { e.printStackTrace(); }
        if (stmt != null) try { stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
        if (conn != null) try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
    }
}
```

### CRUD 操作

```java
// 新增（防 SQL 注入）
String sql = "INSERT INTO user(name, age) VALUES (?, ?)";
PreparedStatement pstmt = conn.prepareStatement(sql);
pstmt.setString(1, "张三");
pstmt.setInt(2, 20);
int rows = pstmt.executeUpdate();

// 查询
String sql = "SELECT id, name, age FROM user";
ResultSet rs = pstmt.executeQuery();
while (rs.next()) {
    int id = rs.getInt("id");
    String name = rs.getString("name");
    int age = rs.getInt("age");
}
```

---

## 五、关键注意事项

| # | 要点 | 说明 |
|:--:|------|------|
| 1 | **使用 PreparedStatement** | 防止 SQL 注入，支持预编译，效率更高 |
| 2 | **及时释放资源** | Connection 是稀缺资源，必须在 finally 或 try-with-resources 中关闭 |
| 3 | **MySQL 8.0 URL 时区** | URL 需指定 `serverTimezone=UTC`，否则报错 |
| 4 | **连接池优于手动管理** | 生产环境使用 Druid / HikariCP，不直接使用 DriverManager |

---

## 六、JDBC 到 ORM 的演进

```
JDBC 原生 API（手动管理一切）
      ↓ 繁琐
Spring JDBC（JdbcTemplate，简化资源管理）
      ↓ 仍需写 SQL
MyBatis（SQL 与代码解耦，动态 SQL）
      ↓ 高度封装
JPA/Hibernate（自动生成 SQL，面向对象操作数据）
```

> JDBC 是所有 ORM 框架的底层基石。理解 JDBC 有助于深入理解 MyBatis 和 Hibernate 的工作原理。

---

*最后更新：2026-07-15*
