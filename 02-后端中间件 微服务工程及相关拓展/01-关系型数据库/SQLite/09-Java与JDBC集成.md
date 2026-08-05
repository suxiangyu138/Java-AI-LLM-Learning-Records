# Java与JDBC集成
> sqlite-jdbc 驱动、连接配置与初始化、Spring Boot 集成、MyBatis 支持、常用操作模式：Java 生态使用 SQLite 的完整指南。

---

## 📚 目录

1. [sqlite-jdbc 驱动](#1-sqlite-jdbc-驱动)
2. [连接管理](#2-连接管理)
3. [JDBC 操作模式](#3-jdbc-操作模式)
4. [Spring Boot 集成](#4-spring-boot-集成)
5. [MyBatis 与 ORM](#5-mybatis-与-orm)
6. [常用场景代码](#6-常用场景代码)

---

## 1. sqlite-jdbc 驱动

### 1.1 依赖与版本

```xml
<!-- Maven 依赖（org.xerial） -->
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.53.0.0</version>  <!-- 与 SQLite 版本对应 -->
</dependency>
```

```text
sqlite-jdbc 特点（org.xerial）：
  ① 内置 SQLite 原生库（无需额外安装）
  ② 支持 JDBC 4.x（无需 Class.forName）
  ③ 支持 WAL/JSON/JSONB/FTS5 等全部特性
  ④ 提供 SQLiteConfig 便捷配置
  ⑤ 多平台（win/mac/linux/android）

版本对应：驱动版本 3.53.x = SQLite 3.53 引擎
  → 升级驱动 = 升级 SQLite 引擎（含安全修复）
```

### 1.2 驱动注册

```java
// JDBC 4+ 自动注册（无需手动）
// 如需显式加载：
Class.forName("org.sqlite.JDBC");

// 连接 URL 格式：
//   jdbc:sqlite:test.db           相对路径
//   jdbc:sqlite:/abs/path/test.db 绝对路径
//   jdbc:sqlite::memory:          内存库
//   jdbc:sqlite:file:test.db?mode=ro  只读（URI）
```

---

## 2. 连接管理

### 2.1 SQLiteConfig：初始化配置

```java
// 推荐的配置方式（一次设置全部 PRAGMA）
SQLiteConfig config = new SQLiteConfig();
config.setJournalMode(SQLiteConfig.JournalMode.WAL);   // WAL
config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
config.setBusyTimeout(5000);                            // busy 等待
config.enforceForeignKeys(true);                        // 外键

Connection conn = DriverManager.getConnection(
    "jdbc:sqlite:app.db", config.toProperties());
```

| 配置 | 方法 | 对应 PRAGMA |
|------|------|-----------|
| WAL 模式 | setJournalMode(WAL) | journal_mode=WAL |
| 同步级别 | setSynchronous(NORMAL) | synchronous=NORMAL |
| 忙等待 | setBusyTimeout(5000) | busy_timeout=5000 |
| 外键 | enforceForeignKeys(true) | foreign_keys=ON |
| 缓存 | setCacheSize(16000) | cache_size=-16000 |

### 2.2 连接池

```text
SQLite 连接池的注意：
  ① SQLite 适合"少连接"（单写者）
  ② 连接池（HikariCP）管理多连接 → 锁竞争
  ③ 最佳实践：
     读多写少：连接池（读并行）
     写密集：单连接 + 内部队列（避免锁争）

HikariCP 配置：
  maximumPoolSize：小（4-8 足够，SQLite 非并发库）
  initializationFailTimeout：连接初始化时执行 PRAGMA
```

```java
// HikariCP + SQLite（Spring Boot 默认池）
HikariConfig hc = new HikariConfig();
hc.setJdbcUrl("jdbc:sqlite:app.db");
hc.setMaximumPoolSize(8);          // 不要大（单写者）
hc.setConnectionInitSql(
    "PRAGMA journal_mode=WAL;" +
    "PRAGMA synchronous=NORMAL;" +
    "PRAGMA busy_timeout=5000;" +
    "PRAGMA foreign_keys=ON;");    // 每个连接初始化
```

### 2.3 多线程注意

```text
JDBC 连接线程规则：
  ① 同一连接跨线程使用 → 需同步（不建议）
  ② 每线程独立连接 → 安全（推荐）
  ③ 连接池按需借还 → 标准模式

sqlite-jdbc 内部：
  每连接独立 native 句柄
  连接线程安全由应用保证（池管理）

写场景：
  多线程并发写同一库 → 锁竞争（BUSY）
  → 方案：写队列（单线程提交）+ 读连接池
```

---

## 3. JDBC 操作模式

### 3.1 基础 CRUD

```java
// 查询
try (Connection conn = DriverManager.getConnection("jdbc:sqlite:app.db");
     PreparedStatement ps = conn.prepareStatement(
         "SELECT id, name FROM users WHERE id = ?")) {
    ps.setInt(1, 42);
    try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
            int id = rs.getInt("id");
            String name = rs.getString("name");
        }
    }
}

// 更新（事务）
try (PreparedStatement ps = conn.prepareStatement(
        "UPDATE users SET name = ? WHERE id = ?")) {
    ps.setString(1, "alice");
    ps.setInt(2, 42);
    int rows = ps.executeUpdate();
}
```

### 3.2 批量写入（性能关键）

```java
// 批量插入：事务 + 预编译 + 分批
conn.setAutoCommit(false);
try (PreparedStatement ps = conn.prepareStatement(
        "INSERT INTO logs (ts, msg) VALUES (?, ?)")) {
    for (int i = 0; i < 100000; i++) {
        ps.setLong(1, System.currentTimeMillis());
        ps.setString(2, "log-" + i);
        ps.addBatch();
        if (i % 10000 == 0) ps.executeBatch();   // 每万条提交
    }
    ps.executeBatch();
    conn.commit();
}
// 实测：比逐条自动提交快 10-100 倍
```

### 3.3 类型映射

| SQLite 存储类 | JDBC 类型 | 注意 |
|-------------|-----------|------|
| INTEGER | int/long | 大整数用 long |
| REAL | double | 金额用整数分 |
| TEXT | String | 日期存 ISO8601 |
| BLOB | byte[] | JSONB/向量 |
| NULL | null | — |

```text
类型陷阱（02 模块呼应）：
  动态类型 → JDBC getString 可能拿到数字（无 STRICT 时）
  STRICT 表 + 明确映射 → 减少类型 bug
  布尔：getInt 0/1（无 getBoolean 原生）
  日期：getString（ISO8601）或 getLong（epoch）
```

---

## 4. Spring Boot 集成

### 4.1 配置

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:sqlite:./data/app.db
    driver-class-name: org.sqlite.JDBC
    # HikariCP 配置
    hikari:
      maximum-pool-size: 8
      connection-init-sql: |
        PRAGMA journal_mode=WAL;
        PRAGMA synchronous=NORMAL;
        PRAGMA busy_timeout=5000;
        PRAGMA foreign_keys=ON;
```

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.53.0.0</version>
</dependency>
```

### 4.2 Spring JDBC / JdbcTemplate

```java
@Repository
public class UserDao {
    private final JdbcTemplate jdbc;

    public UserDao(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<User> findActive() {
        return jdbc.query(
            "SELECT id, name FROM users WHERE status = 1",
            (rs, i) -> new User(rs.getInt("id"), rs.getString("name")));
    }

    @Transactional
    public void batchCreate(List<User> users) {
        jdbc.batchUpdate(
            "INSERT INTO users (name) VALUES (?)",
            users, 1000, (ps, u) -> ps.setString(1, u.name()));
    }
}
```

### 4.3 事务管理

```java
// Spring 事务（@Transactional）→ JDBC 事务
// 注意：Spring 事务默认每个连接一个事务
// 写密集场景建议：
//   ① 单连接（不配池）→ 事务天然串行
//   ② 或池 + 写方法内手动批量

// SQLite 特有：BEGIN IMMEDIATE 防止锁升级
// 可通过连接初始化/拦截器设置
// 例：DataSource 初始化时执行 BEGIN IMMEDIATE？
// → 实践中用 busy_timeout + 重试即可（SQLITE_BUSY 重试）
```

---

## 5. MyBatis 与 ORM

### 5.1 MyBatis 集成

```xml
<!-- mybatis-config 或 Spring 配置 -->
<dataSource type="POOLED">
    <property name="driver" value="org.sqlite.JDBC"/>
    <property name="url" value="jdbc:sqlite:./data/app.db"/>
</dataSource>
```

```text
MyBatis + SQLite 注意：
  ① 驱动兼容（JDBC 标准，无特殊）
  ② 自增主键：useGeneratedKeys + keyProperty（INTEGER PRIMARY KEY）
  ③ 布尔映射：0/1 → boolean（typeHandler 可选）
  ④ 日期：TEXT（ISO8601）→ LocalDateTime（typeHandler）
  ⑤ 动态 SQL 完全可用（无方言差异——SQLite 兼容标准）

SQL 方言注意：
  LIMIT ? OFFSET ?（SQLite 支持）
  不支持：MySQL 的 ON DUPLICATE KEY（用 ON CONFLICT）
  UPDATE ... JOIN（SQLite 不支持——用子查询）
```

### 5.2 JPA/Hibernate

```text
JPA + SQLite 的现状：
  无官方方言（Hibernate 社区方言）
  常用：org.hibernate.community.dialect.SQLiteDialect
  （Hibernate 6+ 社区模块）

常见问题：
  自增（IDENTITY）需方言支持
  类型映射（无 BOOLEAN/DATE 原生）
  锁/并发语义（SQLite 单写者）

建议：
  简单项目：Spring JDBC / MyBatis（推荐）
  JPA 强需求：确认方言与特性支持（谨慎）
```

### 5.3 选择建议

| 场景 | 推荐 | 理由 |
|------|------|------|
| 简单 CRUD | JdbcTemplate | 轻量、可控 |
| 复杂 SQL | MyBatis | SQL 友好、无方言魔法 |
| JPA 生态强依赖 | Hibernate + 社区方言 | 兼容性成本高 |
| 高性能批量 | 原生 JDBC（手写） | 最大控制 |

---

## 6. 常用场景代码

### 6.1 内存库（缓存/测试）

```java
// 内存库：进程内、极快、退出即失
Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");

// 共享内存库（多连接同库——需 URI 命名）
// jdbc:sqlite:file:shared?mode=memory&cache=shared
```

### 6.2 JSONB 操作（05 模块）

```java
// 存储 JSONB（BLOB）
try (PreparedStatement ps = conn.prepareStatement(
        "INSERT INTO doc (id, data) VALUES (?, jsonb(?))")) {
    ps.setInt(1, 1);
    ps.setString(2, "{\"user\":{\"name\":\"alice\"},\"tags\":[\"a\",\"b\"]}");
    ps.executeUpdate();
}

// 提取（JSONB 路径查询）
try (PreparedStatement ps = conn.prepareStatement(
        "SELECT json_extract(data, '$.user.name') FROM doc WHERE id = ?")) {
    ps.setInt(1, 1);
    // → "alice"
}
```

### 6.3 FTS5 与向量（06/07 模块）

```java
// FTS5 查询
String sql = "SELECT rowid, bm25(articles_fts) AS r " +
             "FROM articles_fts WHERE articles_fts MATCH ? " +
             "ORDER BY r LIMIT 20";
// 参数：查询词（"sqlite AND database"）

// 向量查询（sqlite-vec 扩展加载）
// ① 加载扩展：SELECT load_extension('vec0');
// ② KNN：
String vecSql = "SELECT id, distance FROM embeddings " +
                "WHERE embedding MATCH ? AND k = 50";
// 参数：向量文本 "[0.1, -0.2, ...]"
```

### 6.4 备份（08 模块）

```java
// VACUUM INTO（在线一致性备份）
try (Statement st = conn.createStatement()) {
    st.execute("VACUUM INTO 'backup.db'");
}

// backup API（程序化增量）
// sqlite-jdbc 提供 org.sqlite.SQLiteBackup？——无官方封装
// 常用替代：VACUUM INTO（简单）或文件级方案（Litestream）
```

> 🎯 **核心要点**：Java 集成 SQLite = sqlite-jdbc（零依赖）+ SQLiteConfig（初始化 PRAGMA）+ 连接策略（读池写队列）。三个关键纪律：**连接初始化必配 WAL/NORMAL/busy/外键；批量写入必用事务+预编译；类型映射明确（布尔 0/1、日期 ISO8601、JSONB 用 BLOB）**。框架选择：轻量用 JdbcTemplate/MyBatis（无方言坑），JPA 需谨慎评估。

---

## 7. 小结

| 问题 | 答案 |
|------|------|
| 驱动？ | org.xerial:sqlite-jdbc（内置原生库） |
| 初始化配置？ | SQLiteConfig（WAL/NORMAL/busy/外键） |
| 连接池？ | 小池（8 内）或写队列（写密集） |
| Spring Boot？ | Hikari + connection-init-sql 配置 PRAGMA |
| ORM 选择？ | JdbcTemplate/MyBatis 推荐；JPA 谨慎 |
| 批量写入？ | 事务 + 预编译（10-100×） |

**返回总览**：[00-SQLite知识体系总览](00-SQLite知识体系总览.md)　**返回上级**：[01-关系型数据库](../)
