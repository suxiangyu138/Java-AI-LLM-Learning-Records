# 数据库连接池与 JDBC 编程
> JDBC 是 Java 连接数据库的底层标准，连接池是生产环境的必备组件。本文涵盖 JDBC 核心 API、事务管理、连接池原理（HikariCP + Druid）及最佳实践。

## 目录
1. [JDBC 概述](#1-jdbc-概述)
2. [JDBC 核心 API](#2-jdbc-核心-api)
3. [JDBC CRUD 实战](#3-jdbc-crud-实战)
4. [PreparedStatement 防注入](#4-preparedstatement-防注入)
5. [JDBC 事务管理](#5-jdbc-事务管理)
6. [为什么需要连接池](#6-为什么需要连接池)
7. [HikariCP 连接池](#7-hikaricp-连接池)
8. [Druid 连接池](#8-druid-连接池)
9. [连接池选择建议](#9-连接池选择建议)
10. [常见问题排查](#10-常见问题排查)

---

## 1. JDBC 概述

JDBC（Java Database Connectivity）是 Java 访问数据库的底层标准 API。MyBatis、Hibernate、JPA 等所有 ORM 框架的底层都基于 JDBC。

```
Java 应用 → ORM 框架（MyBatis/JPA） → JDBC API → MySQL 驱动 → 数据库
```

### JDBC 与 ORM 框架对比

| 维度 | 原生 JDBC | MyBatis | JPA/Hibernate |
|------|-----------|---------|---------------|
| 代码量 | 大量样板代码 | 简洁 | 最少 |
| SQL 控制 | 完全控制 | 完全控制 | 自动生成 |
| 学习成本 | 低 | 中 | 高 |
| 灵活性 | 极高 | 高 | 中 |
| 性能调优 | 手动 | 手动 | 较难 |

> 💡 **理解 JDBC 是深入理解 MyBatis 的前提**——MyBatis 的 SqlSession、事务管理、连接池本质上都是对 JDBC 的封装。

---

## 2. JDBC 核心 API

| API | 作用 | 说明 |
|-----|------|------|
| `DriverManager` | 注册驱动、获取连接 | JDBC 4.0 后无需显式 `Class.forName()` |
| `Connection` | 数据库连接 | 事务管理、创建 Statement |
| `Statement` | 执行静态 SQL | 有 SQL 注入风险 |
| `PreparedStatement` | 执行预编译 SQL | 防注入（推荐） |
| `CallableStatement` | 执行存储过程 | 企业开发较少使用 |
| `ResultSet` | 查询结果集 | 遍历结果、映射为 Java 对象 |

---

## 3. JDBC CRUD 实战

### 3.1 完整查询示例

```java
public List<User> findAll() {
    String sql = "SELECT id, username, password, phone, created_at FROM t_user";
    List<User> users = new ArrayList<>();

    try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
         PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

        while (rs.next()) {
            User user = new User();
            user.setId(rs.getLong("id"));
            user.setUsername(rs.getString("username"));
            user.setPassword(rs.getString("password"));
            user.setPhone(rs.getString("phone"));
            user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            users.add(user);
        }
    } catch (SQLException e) {
        log.error("查询失败", e);
    }
    return users;
}
```

### 3.2 插入并获取自增主键

```java
public Long insert(User user) {
    String sql = "INSERT INTO t_user(username, password, phone) VALUES (?, ?, ?)";

    try (Connection conn = dataSource.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

        ps.setString(1, user.getUsername());
        ps.setString(2, user.getPassword());
        ps.setString(3, user.getPhone());
        ps.executeUpdate();

        // 获取自增主键
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getLong(1);
            }
        }
    } catch (SQLException e) {
        log.error("插入失败", e);
    }
    return null;
}
```

### 3.3 更新与删除

```java
public int update(User user) {
    String sql = "UPDATE t_user SET username = ?, phone = ? WHERE id = ?";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, user.getUsername());
        ps.setString(2, user.getPhone());
        ps.setLong(3, user.getId());
        return ps.executeUpdate();  // 返回影响行数
    }
}

public int deleteById(Long id) {
    String sql = "DELETE FROM t_user WHERE id = ?";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setLong(1, id);
        return ps.executeUpdate();
    }
}
```

---

## 4. PreparedStatement 防注入

### 4.1 为什么 Statement 不安全

```java
// Statement：字符串拼接 —— SQL 注入风险
String username = "admin' OR '1'='1";
String sql = "SELECT * FROM t_user WHERE username = '" + username + "'";
// 实际执行：SELECT * FROM t_user WHERE username = 'admin' OR '1'='1'

// PreparedStatement：预编译 —— 参数和 SQL 分离
String sql = "SELECT * FROM t_user WHERE username = ?";
PreparedStatement ps = conn.prepareStatement(sql);
ps.setString(1, username);
// username 中的特殊字符会被转义，不会破坏 SQL 结构
```

### 4.2 JDBC 批量操作

```java
public void batchInsert(List<User> users) {
    String sql = "INSERT INTO t_user(username, password) VALUES (?, ?)";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

        for (User user : users) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.addBatch();  // 加入批处理
        }

        int[] results = ps.executeBatch();  // 一次性批量执行
    }
}
```

> ⚠️ **生产环境禁止使用 Statement**，一律使用 PreparedStatement。

---

## 5. JDBC 事务管理

### 5.1 事务操作

```java
public void transfer(Long fromId, Long toId, BigDecimal amount) {
    Connection conn = null;
    try {
        conn = dataSource.getConnection();
        conn.setAutoCommit(false);  // 关闭自动提交，开启事务

        String sql1 = "UPDATE account SET balance = balance - ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql1)) {
            ps.setBigDecimal(1, amount);
            ps.setLong(2, fromId);
            ps.executeUpdate();
        }

        String sql2 = "UPDATE account SET balance = balance + ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql2)) {
            ps.setBigDecimal(1, amount);
            ps.setLong(2, toId);
            ps.executeUpdate();
        }

        conn.commit();  // 提交事务
    } catch (SQLException e) {
        if (conn != null) {
            try {
                conn.rollback();  // 异常回滚
            } catch (SQLException ex) {
                log.error("回滚失败", ex);
            }
        }
        log.error("转账失败", e);
    } finally {
        if (conn != null) {
            conn.setAutoCommit(true);  // 恢复自动提交
            conn.close();
        }
    }
}
```

### 5.2 事务隔离级别设置

```java
// Connection 接口定义
conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
```

---

## 6. 为什么需要连接池

数据库连接是昂贵的资源：

| 资源项 | 说明 |
|--------|------|
| 连接创建耗时 | TCP 握手 + TLS + MySQL 认证 ≈ 50~200ms |
| 连接内存占用 | 每个连接占用 MySQL 内存 256KB~2MB |
| 连接数上限 | MySQL 默认 `max_connections` = 151 |

**连接池的核心思想**：预先创建一批连接并复用，用完后归还连接池，而不是销毁重建。

```
无连接池：获取连接 → 使用 → 关闭连接
有连接池：从池中借 → 使用 → 归还到池
```

---

## 7. HikariCP 连接池

Spring Boot 2.x 起默认连接池，以极致的性能著称。

### 7.1 Spring Boot 配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: 123456
    hikari:
      maximum-pool-size: 20        # 最大连接数
      minimum-idle: 5              # 最小空闲连接
      idle-timeout: 600000         # 空闲超时 10分钟
      connection-timeout: 30000    # 获取连接等待超时
      max-lifetime: 1800000        # 连接最大存活时间 30分钟
      connection-test-query: SELECT 1  # 连接有效性检测
```

### 7.2 核心配置原则

| 参数 | 建议值 | 说明 |
|------|--------|------|
| `maximum-pool-size` | CPU 核心数 × 2 + 磁盘数 | **不是越大越好**，过多连接导致上下文切换 |
| `max-lifetime` | 比 MySQL `wait_timeout` 短 | 避免被 MySQL 断开（默认 30 分钟 vs 8 小时） |
| `idle-timeout` | 600000 (10分钟) | 控制空闲连接回收 |
| `connection-timeout` | 30000 (30秒) | 等待超时，耗尽时快速失败 |

### 7.3 Java 配置方式

```java
@Configuration
public class DataSourceConfig {
    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/mydb");
        config.setUsername("root");
        config.setPassword("123456");
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setConnectionTestQuery("SELECT 1");
        return new HikariDataSource(config);
    }
}
```

---

## 8. Druid 连接池

阿里巴巴开源的连接池，提供监控、防火墙、慢 SQL 日志等功能。

### 8.1 Maven 依赖

```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid-spring-boot-starter</artifactId>
    <version>1.2.20</version>
</dependency>
```

### 8.2 配置

```yaml
spring:
  datasource:
    druid:
      url: jdbc:mysql://localhost:3306/mydb
      username: root
      password: 123456
      initial-size: 5
      min-idle: 5
      max-active: 20
      max-wait: 30000

      # 监控配置
      stat-view-servlet:
        enabled: true
        url-pattern: /druid/*
        login-username: admin
        login-password: admin

      # SQL 防火墙
      filter:
        wall:
          enabled: true

      # 慢 SQL 记录
      filter:
        stat:
          enabled: true
          slow-sql-millis: 1000
          log-slow-sql: true
```

### 8.3 Druid 优势

| 功能 | 说明 |
|------|------|
| 内置监控面板 | 访问 `/druid` 查看连接池状态、SQL 执行统计 |
| SQL 防火墙 | 防止 SQL 注入攻击 |
| 慢 SQL 日志 | 自动记录超过阈值的慢查询 |
| 连接泄漏检测 | 自动回收长时间未归还的连接 |

---

## 9. 连接池选择建议

| 场景 | 推荐 | 原因 |
|------|------|------|
| Spring Boot 单体 / 中小项目 | HikariCP | 默认集成，开箱即用，性能极致 |
| 需要 SQL 监控和防火墙 | Druid | 监控面板和 SQL 防火墙更完善 |
| 排查连接问题 | 临时加入 Druid | Druid 监控更方便定位问题 |

> 💡 **最佳实践**：先用 HikariCP（Spring Boot 默认），排查问题时引入 Druid 的数据源监控辅助定位。

---

## 10. 常见问题排查

### 10.1 "Connection is not available, request timed out"

连接池耗尽。排查步骤：

| 步骤 | 检查项 | 说明 |
|------|--------|------|
| 1 | 连接泄漏 | 获取连接后是否未关闭（未用 try-with-resources） |
| 2 | maximum-pool-size | 是否设置过小，无法支撑并发 |
| 3 | 慢查询 | 慢查询长时间占用连接不释放 |
| 4 | MySQL 最大连接数 | `SHOW VARIABLES LIKE 'max_connections'` |

### 10.2 连接泄漏排查

```java
// 错误：未关闭连接（硬伤）
Connection conn = dataSource.getConnection();
PreparedStatement ps = conn.prepareStatement(sql);
ResultSet rs = ps.executeQuery();
// 没有 finally 关闭，连接永远不归还

// 正确：try-with-resources 自动关闭
try (Connection conn = dataSource.getConnection();
     PreparedStatement ps = conn.prepareStatement(sql);
     ResultSet rs = ps.executeQuery()) {
    // 自动关闭
}
```

Druid 检测连接泄漏：

```yaml
druid:
  remove-abandoned: true
  remove-abandoned-timeout: 1800  # 超过 30 分钟自动回收
```

### 10.3 max-lifetime 设置

设置原则：`max-lifetime` < MySQL `wait_timeout`，建议差 30 秒。

| 参数 | 默认值 | 建议 |
|------|--------|------|
| MySQL `wait_timeout` | 28800 秒（8 小时） | 保持默认 |
| HikariCP `max-lifetime` | 1800000 毫秒（30 分钟） | 足够安全，保持默认 |

> 🎯 **连接池配置的核心**：池不是越大越好，要根据 CPU 核心数、业务并发量、数据库规格综合评估。建议压测确定最优值。
