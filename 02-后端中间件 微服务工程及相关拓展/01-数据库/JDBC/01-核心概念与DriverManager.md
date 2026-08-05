# 01 核心概念与 DriverManager

> JDBC 是 Java 访问关系数据库的标准 API——驱动加载、Connection 获取、URL 格式，理解"驱动 + 连接"模型是第一步

---

## 📚 目录

1. [JDBC 是什么](#1-jdbc-是什么)
2. [JDBC 规范演进](#2-jdbc-规范演进)
3. [驱动加载机制](#3-驱动加载机制)
4. [JDBC URL 详解](#4-jdbc-url-详解)
5. [Connection 获取与关闭](#5-connection-获取与关闭)
6. [标准七步 CRUD 实战](#6-标准七步-crud-实战)

---

## 1. JDBC 是什么

**JDBC（Java Database Connectivity）**：Java 访问关系型数据库的**标准 API**（`java.sql` / `javax.sql` 包）——"接口在 JDK，实现在驱动"：

```text
Java 程序（业务代码）
  │  使用 JDBC 接口（java.sql.*）
  ▼
JDBC 驱动（Driver 实现）—— 每个数据库一个（MySQL Connector/J、PostgreSQL JDBC）
  │
  ▼
数据库（MySQL / PostgreSQL / Oracle ...）

关键设计：JDBC 定义接口，厂商实现驱动
  → Java 代码不依赖具体数据库（换库只换驱动 + URL）
```

**JDBC 的五个核心接口**：

| 接口 | 作用 | 类比 |
|------|------|------|
| Driver | 驱动入口（注册到 DriverManager） | 适配器 |
| DriverManager | 驱动管理 + 连接工厂 | 工厂 |
| Connection | 数据库连接（一个连接 = 一个会话） | 会话 |
| Statement | SQL 执行器 | 命令 |
| ResultSet | 查询结果集 | 游标 |

> 🎯 **核心要点**：JDBC = "**标准接口 + 厂商驱动**"——**Java 代码面向接口编程，换数据库只换驱动与 URL**（这是它 20 多年长盛不衰的设计）。

---

## 2. JDBC 规范演进

| 版本 | JDK | 关键变化 |
|:----:|:---:|---------|
| JDBC 1.0 | JDK 1.1 | 基础 CRUD |
| JDBC 2.0 | JDK 1.2 | ResultSet 可滚动、批量更新 |
| JDBC 3.0 | JDK 1.4 | 连接池标准接口（DataSource）、savepoint |
| **JDBC 4.0** | JDK 6 | **驱动自动加载**（ServiceLoader）、异常改进 |
| JDBC 4.1 | JDK 7 | try-with-resources 支持（Connection 等 AutoCloseable） |
| **JDBC 4.2** | JDK 8 | `setObject/getObject` 支持 LocalDate/LocalTime（java.time） |
| JDBC 4.3 | JDK 9 | 小幅完善 |

**现代 JDBC 的收益**（面试谈资）：

```text
① JDBC 4.0（JDK 6）：不用 Class.forName（自动注册驱动）
② JDBC 4.1（JDK 7）：Connection/Statement 实现 AutoCloseable → TWR 关闭
③ JDBC 4.2（JDK 8）：LocalDate/LocalTime 直接存取（告别 Timestamp 转换）
→ 老教程的"Class.forName 加载驱动"在 JDK 6+ 已是历史
```

> 🎯 **核心要点**：JDBC 演进 = "**自动加载驱动（4.0）+ TWR 关闭（4.1）+ java.time 支持（4.2）**"——**老教程三件套（Class.forName/手动关闭/Date 转换）在现代已是反模式**。

---

## 3. 驱动加载机制

**JDBC 4 的自动注册**（ServiceLoader 机制）：

```java
// 老写法（JDBC 3 及以前）：必须手动加载
Class.forName("com.mysql.cj.jdbc.Driver");
Class.forName("org.postgresql.Driver");

// 现代（JDBC 4+）：自动加载（依赖里有驱动 jar 即可）
// 原理：驱动 jar 的 META-INF/services/java.sql.Driver 声明驱动类
//       DriverManager 初始化时 ServiceLoader 扫描加载
```

```xml
<!-- 依赖驱动（Maven）—— 自动注册的核心 -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>8.4.0</version>
</dependency>
```

**驱动加载的细节**：

```text
① 自动加载：classpath 中有驱动 jar → 自动注册（无需 Class.forName）
② 多驱动共存：DriverManager 按 URL 前缀选择驱动
   jdbc:mysql:// → MySQL 驱动；jdbc:postgresql:// → PG 驱动
③ 注册方式：Driver 静态块中 DriverManager.registerDriver(this)
④ Class.forName 何时还需要？—— 极少数"驱动未实现 ServiceLoader"的旧驱动
```

> 🎯 **核心要点**：驱动加载 = "**JDBC 4 自动注册（ServiceLoader）**"——**"Class.forName 已不需要"是现代面试题**（老教程过时点）；多驱动按 URL 前缀自动匹配。

---

## 4. JDBC URL 详解

**URL 的通用结构**：`jdbc:<子协议>:<数据源>`——各数据库不同：

```java
// MySQL
String url = "jdbc:mysql://localhost:3306/order_db"
        + "?useSSL=false"
        + "&serverTimezone=Asia/Shanghai"
        + "&characterEncoding=utf8"
        + "&rewriteBatchedStatements=true";

// PostgreSQL
String url = "jdbc:postgresql://localhost:5432/order_db";

// Oracle（SID 与 ServiceName 两种）
String url = "jdbc:oracle:thin:@localhost:1521:ORCL";
```

**MySQL URL 的必配参数**（2026 实践）：

| 参数 | 作用 | 不配的后果 |
|------|------|-----------|
| serverTimezone | 时区 | **报错/时间错乱**（新驱动必须） |
| characterEncoding | 编码 | 中文乱码 |
| useSSL | 是否加密 | 默认警告/安全 |
| rewriteBatchedStatements | 批处理改写 | 批处理性能差（见 06 模块） |
| useUnicode | Unicode 支持 | 与编码配合 |

> 🎯 **核心要点**：URL = "**jdbc:子协议:地址 + 参数**"——**MySQL 的 serverTimezone/characterEncoding 必配**（时区与乱码两大事故的源头）；`&` 连接参数是连接池/框架配置的基础。

---

## 5. Connection 获取与关闭

```java
// 获取连接（DriverManager 直连 —— 学习/测试用）
try (Connection conn = DriverManager.getConnection(url, user, password)) {
    // 使用连接
}
// ✅ TWR 自动关闭（JDBC 4.1，Connection 实现 AutoCloseable）

// 生产：连接池获取（见 05 模块）
// DataSource ds = new HikariDataSource(config);
// try (Connection conn = ds.getConnection()) { ... }
```

**Connection 的三个关键认知**：

```text
① 创建连接昂贵：网络握手 + 认证 + 会话初始化（毫秒-几十毫秒）
   → 生产必须连接池（复用连接）
② 一个连接一个事务：事务边界 = 连接生命周期（见 04 模块）
③ 关闭语义：DriverManager 连接 close = 断开；
   连接池连接 close = 归还池中（不是真断开！）
```

> 🎯 **核心要点**：Connection = "**昂贵的会话（创建慢）+ 一连接一事务 + 关闭语义因来源而异**"——**"连接池的 close 是归还"是最重要的认知差**（很多人误以为会真断开）；TWR 是现代关闭规范。

---

## 6. 标准七步 CRUD 实战

**JDBC 完整 CRUD**（不依赖框架的手写标准）：

```java
public class JdbcCrud {
    private static final String URL = "jdbc:mysql://localhost:3306/db"
            + "?serverTimezone=Asia/Shanghai&characterEncoding=utf8";
    private static final String USER = "root";
    private static final String PWD = "root";

    // 查询（七步全流程）
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        // ① 获取连接（TWR 关闭）
        try (Connection conn = DriverManager.getConnection(URL, USER, PWD);
             // ③ 创建 Statement（PreparedStatement 首选，见 02 模块）
             PreparedStatement ps = conn.prepareStatement("SELECT id, name FROM user");
             // ④ 执行查询 → ⑤ 结果集
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {                     // ⑤ 遍历结果
                User u = new User();
                u.setId(rs.getLong("id"));
                u.setName(rs.getString("name"));
                users.add(u);
            }
        } catch (SQLException e) {                  // ⑥ 异常处理
            throw new RuntimeException("查询失败", e);
        }
        return users;                               // ⑦ 返回（连接已自动关闭）
    }

    // 插入（带参数）
    public void insert(User u) {
        String sql = "INSERT INTO user (name, age) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PWD);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getName());           // 参数占位符
            ps.setInt(2, u.getAge());
            ps.executeUpdate();                     // 返回影响行数
        } catch (SQLException e) {
            throw new RuntimeException("插入失败", e);
        }
    }
}
```

**七步的记忆**（面试必背）：**① 连接 → ② Statement → ③ 执行 → ④ 结果 → ⑤ 处理 → ⑥ 关闭 → ⑦ 异常**——TWR 让"关闭"自动化（④⑤ 合并为 try-with-resources）。

> 🎯 **核心要点**：七步 CRUD = "**连接 → 预编译 → 执行 → 结果 → 遍历 → 异常 → 关闭**"——**TWR 管理资源 + PreparedStatement 防注入**是现代手写 JDBC 的两个铁律（02 模块详述）。

---

**下一模块**：[02-Statement三兄弟](./02-Statement三兄弟.md) / **返回总览**：[00-JDBC知识体系总览](./00-JDBC知识体系总览.md)
