# 23 - JDBC数据库编程

> 定位：掌握JDBC核心API——DriverManager/Connection/Statement/PreparedStatement/ResultSet，理解SQL注入防护、事务管理、连接池与批处理，为MyBatis等ORM框架打下底层基础

---

## 目录

1. [JDBC概述与架构](#1-jdbc概述与架构)
2. [驱动加载与Connection获取](#2-驱动加载与connection获取)
3. [Statement与SQL注入风险](#3-statement与sql注入风险)
4. [PreparedStatement：防注入与预编译](#4-preparedstatement防注入与预编译)
5. [ResultSet结果集处理](#5-resultset结果集处理)
6. [JDBC事务管理](#6-jdbc事务管理)
7. [批处理（Batch Processing）](#7-批处理batch-processing)
8. [CallableStatement调用存储过程](#8-callablestatement调用存储过程)
9. [连接池概述（HikariCP/Druid）](#9-连接池概述hikaricpdruid)
10. [JDBC完整CRUD实战](#10-jdbc完整crud实战)
11. [最佳实践与常见问题](#11-最佳实践与常见问题)
12. [面试高频考点](#12-面试高频考点)

---

## 1. JDBC概述与架构

JDBC（Java Database Connectivity）是Oracle定义的**Java操作关系型数据库统一标准API**——一套接口规范，各数据库厂商提供具体实现（驱动Jar包）。

```
Java 应用 → JDBC API → 厂商驱动(MySQL/Oracle/MSSQL) → 数据库
```

| 四大功能 | 说明 |
|----------|------|
| 建立连接 | Java与数据库建立TCP通信链路 |
| 发送SQL | 执行CRUD、DDL、存储过程 |
| 解析结果 | 接收查询返回的ResultSet |
| 资源管理 | 处理SQLException，管理连接生命周期 |

> 💡 **核心**：换数据库只需换驱动和URL，无需修改业务代码。

```xml
<dependency> <!-- MySQL 8.0+ -->
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.36</version>
</dependency>
```

> ⚠️ MySQL8驱动类 `com.mysql.cj.jdbc.Driver`（带 `cj`），MySQL5为 `com.mysql.jdbc.Driver`（无 `cj`），不可混用。

---

## 2. 驱动加载与Connection获取

### 2.1 DriverManager

DriverManager统一管理注册驱动，提供静态方法获取连接。JDK 1.6+ 已通过SPI自动注册驱动，无需手动 `Class.forName()`。

### 2.2 获取连接

```java
Connection conn = DriverManager.getConnection(
    "jdbc:mysql://localhost:3306/test_db?serverTimezone=UTC", "root", "123456");
```

### 2.3 JDBC URL参数

```
jdbc:mysql://host:port/database?参数1=值1&参数2=值2
```

| 参数 | 说明 | 必填 |
|------|------|:----:|
| `serverTimezone=UTC` | MySQL8强制时区设置 | MySQL8必须 |
| `characterEncoding=utf8` | 解决中文乱码 | 推荐 |
| `useSSL=false` | 开发环境关闭SSL校验 | 推荐 |
| `allowMultiQueries=true` | 允许一次执行多条SQL | 按需 |

> 💡 `Connection` 非线程安全，禁止多线程共享同一个连接。

---

## 3. Statement与SQL注入风险

### 3.1 Statement方法

```java
Statement stmt = conn.createStatement();
int n = stmt.executeUpdate("UPDATE user SET age=18 WHERE id=1"); // 增删改
ResultSet rs = stmt.executeQuery("SELECT * FROM user");           // 查询
boolean b = stmt.execute("DELETE FROM user");                     // 兼容执行
```

### 3.2 SQL注入演示

```java
// ──── 危险：恒等绕过，无需密码即可登录 ────
String input = "' OR '1'='1";
// 拼接后的SQL：SELECT * FROM user WHERE username = '' OR '1'='1' AND password = '' OR '1'='1'
// → WHERE 永真，绕过登录校验！
String sql = "SELECT * FROM user WHERE username = '" + input + "' AND password = '" + input + "'";
Statement stmt = conn.createStatement();
ResultSet rs = stmt.executeQuery(sql);

// ──── 危险：删除注入 ────
String userId = "1 OR 1=1";
// DELETE FROM user WHERE id = 1 OR 1=1 → 删除全表数据！
stmt.executeUpdate("DELETE FROM user WHERE id = " + userId);
```

| 注入类型 | 恶意输入 | 后果 |
|----------|----------|------|
| 恒等绕过 | `' OR '1'='1` | 登录绕过，越权查询 |
| 注释截断 | `admin' -- ` | 注释掉后续密码校验 |
| UNION注入 | `' UNION SELECT ...` | 窃取其他表数据 |
| 逻辑删除 | `1 OR 1=1` | 全表删除/更新 |

> 🎯 Statement的字符串拼接SQL存在严重安全漏洞，**企业开发严禁使用**，所有参数化查询必须使用 PreparedStatement。

---

## 4. PreparedStatement：防注入与预编译

### 4.1 Statement vs PreparedStatement对比

| 对比维度 | Statement | PreparedStatement |
|----------|-----------|-------------------|
| SQL传递 | 字符串拼接 | `?` 占位符 + 参数分离 |
| SQL注入 | ❌ 高危 | ✅ 自动转义特殊字符，彻底防护 |
| 预编译缓存 | ❌ 每次重新编译 | ✅ 数据库缓存编译模板，重复执行性能高 |
| 代码可读性 | 差，引号嵌套混乱 | 好，SQL与参数分离 |
| 动态表名/列名 | ✅ 支持拼接 | ❌ `?` 只能替代值 |
| 企业推荐 | ❌ 禁止使用 | ✅ 唯一推荐 |

### 4.2 核心用法

```java
String sql = "INSERT INTO user(username, age, email) VALUES(?, ?, ?)";
PreparedStatement pstmt = conn.prepareStatement(sql);
pstmt.setString(1, "张三");     // 占位符下标从1开始
pstmt.setInt(2, 25);
pstmt.setString(3, "zs@example.com");
int rows = pstmt.executeUpdate();            // 增删改
// ResultSet rs = pstmt.executeQuery();      // 查询
```

### 4.3 防注入原理

```
Statement:   SELECT * FROM user WHERE id = 1' OR '1'='1    ← SQL结构被篡改
PreparedStatement:
  ① 发送模板：SELECT * FROM user WHERE id = ?              ← 数据库先完成编译
  ② 传入参数：setInt(1, 1)                                 ← 参数作为纯数据
  ③ 自动转义：参数不参与SQL编译，注入无效                  ← 注入失败！
```

> 💡 **预编译缓存**：同一条SQL反复执行（如批量插入），只需编译一次，后续仅传参数，数据库和网络开销大幅降低。

---

## 5. ResultSet结果集处理

### 5.1 游标与取值

`ResultSet` 内部维护一个游标，初始在第一行**之前**。

```java
while (rs.next()) {                                          // 游标下移
    int id = rs.getInt("id");                                // 列名（推荐）
    String name = rs.getString("username");
    // String name2 = rs.getString(2);                       // 列索引（从1，不推荐）
}
```

| 数据库类型 | get方法 | Java类型 |
|------------|---------|----------|
| INT | `getInt()` | `int` |
| VARCHAR/CHAR | `getString()` | `String` |
| DECIMAL | `getBigDecimal()` | `BigDecimal` |
| DATE | `getDate()` | `java.sql.Date` |
| TIMESTAMP | `getTimestamp()` | `java.sql.Timestamp` |
| BOOLEAN | `getBoolean()` | `boolean` |

### 5.2 ResultSetMetaData

```java
ResultSetMetaData meta = rs.getMetaData();
int colCount = meta.getColumnCount();
String colName = meta.getColumnName(1);
String colType = meta.getColumnTypeName(1);
```

> ⚠️ 资源关闭顺序：`ResultSet` → `Statement`/`PreparedStatement` → `Connection`，反序会导致资源泄露。

---

## 6. JDBC事务管理

### 6.1 ACID四大特性

| 特性 | 说明 | 类比 |
|------|------|------|
| 原子性 | 一组SQL全部成功或全部回滚 | 转账：扣钱+加钱同时成功或同时失败 |
| 一致性 | 事务前后业务数据逻辑一致 | 转账前后两人总金额不变 |
| 隔离性 | 并发事务互不干扰 | A转账给B时，C看不到中间状态 |
| 持久性 | 提交后数据永久落地 | 宕机重启后数据不丢失 |

### 6.2 手动事务标准模板

JDBC默认 `autoCommit=true`，多SQL事务需关闭自动提交。

```java
Connection conn = null;
PreparedStatement pstmt = null;
try {
    conn = DriverManager.getConnection(url, user, pwd);
    conn.setAutoCommit(false);                           // 关闭自动提交

    pstmt = conn.prepareStatement("UPDATE account SET balance=balance-? WHERE id=?");
    pstmt.setInt(1, 100); pstmt.setInt(2, 1);
    pstmt.executeUpdate();

    pstmt = conn.prepareStatement("UPDATE account SET balance=balance+? WHERE id=?");
    pstmt.setInt(1, 100); pstmt.setInt(2, 2);
    pstmt.executeUpdate();
    conn.commit();
} catch (SQLException e) {
    if (conn != null) try { conn.rollback(); } catch (SQLException ex) { }
    e.printStackTrace();
} finally {
    if (pstmt != null) try { pstmt.close(); } catch (SQLException e) { }
    if (conn != null) try { conn.close(); } catch (SQLException e) { }
}
```

### 6.3 事务隔离级别

| 级别 | 脏读 | 不可重复读 | 幻读 | 默认 |
|------|:----:|:----------:|:----:|:----:|
| `READ_UNCOMMITTED` | ✅ | ✅ | ✅ | |
| `READ_COMMITTED` | ❌ | ✅ | ✅ | Oracle |
| `REPEATABLE_READ` | ❌ | ❌ | ✅ | **MySQL InnoDB** |
| `SERIALIZABLE` | ❌ | ❌ | ❌ | |

```java
conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
// 保存点
Savepoint sp = conn.setSavepoint("sp1");
conn.rollback(sp);             // 回滚到保存点
conn.releaseSavepoint(sp);
```

> 💡 隔离级别越高并发性能越差。MySQL InnoDB默认**可重复读**，通过MVCC在保证隔离性的同时提供较高并发性能。

---

## 7. 批处理（Batch Processing）

减少频繁网络交互，一次性批量发送多条SQL。

```java
String sql = "INSERT INTO user(username, age) VALUES(?, ?)";
PreparedStatement pstmt = conn.prepareStatement(sql);
conn.setAutoCommit(false);
for (int i = 1; i <= 1000; i++) {
    pstmt.setString(1, "user_" + i);
    pstmt.setInt(2, 18 + (i % 30));
    pstmt.addBatch();
    if (i % 200 == 0) { pstmt.executeBatch(); conn.commit(); pstmt.clearBatch(); }
}
pstmt.executeBatch(); conn.commit(); pstmt.clearBatch();
```

| 方法 | 说明 |
|------|------|
| `addBatch()` | 当前参数加入批处理队列 |
| `executeBatch()` | 批量发送执行，返回int[] |
| `clearBatch()` | 清空队列 |

> ⚠️ 配合**手动事务**性能最佳，避免单事务数据量过大。

---

## 8. CallableStatement调用存储过程

```java
// 无参存储过程
CallableStatement cstmt = conn.prepareCall("{call get_all_users()}");
ResultSet rs = cstmt.executeQuery();

// 带输入输出参数：{call get_user_name(?, ?)}
CallableStatement cstmt2 = conn.prepareCall("{call get_user_name(?, ?)}");
cstmt2.setInt(1, 1);                                  // 输入参数
cstmt2.registerOutParameter(2, Types.VARCHAR);        // 注册输出参数类型
cstmt2.execute();
String name = cstmt2.getString(2);                    // 获取输出值
```

| 方法 | 用途 |
|------|------|
| `setXxx(index, value)` | 设置输入参数 |
| `registerOutParameter(index, sqlType)` | 注册输出参数类型 |
| `getXxx(index)` | 获取输出参数值 |

---

## 9. 连接池概述（HikariCP/Druid）

### 9.1 为什么需要连接池

原生JDBC频繁创建/关闭Connection经历**TCP三次握手 + 数据库认证**，高并发下开销极大。连接池复用已建立的TCP连接。

```
无连接池： 获得连接 → 执行SQL → 关闭连接  （每次都新建TCP连接）
有连接池： 从池中取 → 执行SQL → 归还到池  （复用已建立的连接）
```

### 9.2 主流连接池对比

| 连接池 | 特点 | 推荐场景 |
|--------|------|----------|
| **HikariCP** | Spring Boot默认，性能最高 | 所有项目，尤其高并发 |
| **Druid**（阿里） | 监控面板、防注入、慢SQL日志 | 中大型项目，需监控 |
| C3P0 | 老牌稳定，性能偏弱 | 遗留老旧项目 |

### 9.3 核心配置（Spring Boot + HikariCP）

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/test_db?serverTimezone=UTC
    username: root
    password: 123456
    hikari:
      maximum-pool-size: 20          # 最大连接数（默认10）
      minimum-idle: 5                # 最小空闲连接（默认同max）
      connection-timeout: 3000       # 获取连接超时ms（默认30000）
      idle-timeout: 600000           # 空闲超时ms
      max-lifetime: 1800000          # 最大存活ms
```

> 💡 连接池原理：初始化时创建一批Connection缓存到池中，`getConnection()` 从池中取，`close()` 归还而非真正关闭TCP连接。

---

## 10. JDBC完整CRUD实战

完整DAO封装（基于MySQL8，`user(id, username, age, email)` 表结构）：

```java
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {
    private static final String URL = "jdbc:mysql://localhost:3306/test_db"
            + "?serverTimezone=UTC&characterEncoding=utf8&useSSL=false";
    private static final String USER = "root", PWD = "123456";

    private Connection getConn() throws SQLException {
        return DriverManager.getConnection(URL, USER, PWD);
    }

    // 查询
    public User selectById(Integer id) {
        String sql = "SELECT * FROM user WHERE id=?";
        try (Connection c = getConn(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, id);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public List<User> selectAll() {
        String sql = "SELECT * FROM user";
        List<User> list = new ArrayList<>();
        try (Connection c = getConn(); PreparedStatement p = c.prepareStatement(sql);
             ResultSet rs = p.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // 新增（返回自增主键）
    public Integer insert(User u) {
        String sql = "INSERT INTO user(username,age,email) VALUES(?,?,?)";
        try (Connection c = getConn();
             PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, u.getUsername()); p.setInt(2, u.getAge());
            p.setString(3, u.getEmail()); p.executeUpdate();
            try (ResultSet keys = p.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    // 更新
    public int update(User u) {
        String sql = "UPDATE user SET username=?,age=?,email=? WHERE id=?";
        try (Connection c = getConn(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, u.getUsername()); p.setInt(2, u.getAge());
            p.setString(3, u.getEmail()); p.setInt(4, u.getId());
            return p.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    // 删除
    public int deleteById(Integer id) {
        String sql = "DELETE FROM user WHERE id=?";
        try (Connection c = getConn(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, id); return p.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    private User map(ResultSet rs) throws SQLException {
        return new User(rs.getInt("id"), rs.getString("username"),
                        rs.getInt("age"), rs.getString("email"));
    }
}

// 实体类（实际项目使用 Lombok @Data）
class User {
    private Integer id; private String username;
    private Integer age; private String email;
    public User() {}
    public User(Integer id, String username, Integer age, String email) {
        this.id = id; this.username = username; this.age = age; this.email = email;
    }
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
```

> 💡 `try-with-resources`（JDK 7+）自动调用 `close()` 释放资源，代码简洁。事务场景需手动控制时使用 `try-catch-finally`。

---

## 11. 最佳实践与常见问题

| 实践要点 | 说明 |
|----------|------|
| 始终使用PreparedStatement | 杜绝SQL注入 |
| try-with-resources | JDK 7+ 自动释放资源 |
| 资源逆序关闭 | ResultSet → Statement → Connection |
| 生产用连接池 | 原生JDBC只适合学习 |
| 事务异常回滚 | catch块中必须 `rollback()` |
| 编码统一UTF-8 | URL加 `characterEncoding=utf8`，库/表用 `utf8mb4` |
| 避免长事务 | 事务内不执行慢SQL或远程调用 |

| 常见问题 | 原因 | 解决 |
|----------|------|------|
| `ClassNotFoundException` | 驱动缺失或类名错误 | 检查依赖；MySQL8用 `cj` 包 |
| 中文乱码 | 编码不一致 | URL加 `characterEncoding=utf8` |
| 连接超时 | 连接池满/网络不通 | 检查池配置 |
| 连接泄露 | Connection未close | try-with-resources |
| 事务不回滚 | 表非InnoDB/异常被catch | `ENGINE=InnoDB`；异常重抛 |
| MySQL5vs8驱动 | 类名/时区/SSL不同 | MySQL8: `cj` + `serverTimezone` |

---

## 12. 面试高频考点

| 问题 | 回答要点 |
|------|----------|
| JDBC本质？ | 操作关系型数据库的统一接口规范，厂商提供实现 |
| 五大核心组件？ | Driver → DriverManager → Connection → PreparedStatement → ResultSet |
| PreparedStatement优势？ | 预编译缓存提效 + 占位符防注入 + 代码可读性好 |
| ACID？ | 原子性、一致性、隔离性、持久性 |
| MySQL默认隔离级别？ | REPEATABLE-READ，解决脏读和不可重复读（MVCC实现） |
| 手动事务步骤？ | `setAutoCommit(false)` → SQL → `commit()`/`rollback()` |
| 连接池作用？ | 复用TCP连接，避免频繁三次握手和认证开销 |
| HikariCP为何快？ | Javassist字节码优化 + FastList无锁集合 + 减少上下文切换 |
| 如何获取自增主键？ | `prepareStatement(sql, RETURN_GENERATED_KEYS)` → `getGeneratedKeys()` |
| JDBC vs ORM？ | ORM框架底层仍封装JDBC，简化了连接/参数/结果集处理 |

---

> 🎯 **核心总结**：JDBC是Java数据库编程的基石。核心掌握三大重点：① PreparedStatement防注入（理解Statement为何危险）；② 事务手动控制（ACID + 隔离级别）；③ 连接池原理（为什么以及如何提升性能）。所有ORM框架底层均封装JDBC——理解JDBC是掌控数据库编程的第一步。
