# 04 DataSource 与连接池速查

> 数据源抽象、三种内置实现、HikariCP/Druid 接入、DataSourceUtils 的连接事务绑定——"连接从哪来"的答案

---

## 📚 目录

1. [DataSource：连接来源抽象](#1-datasource连接来源抽象)
2. [Spring 内置实现](#2-spring-内置实现)
3. [连接池集成：HikariCP / Druid](#3-连接池集成hikaricp--druid)
4. [DataSourceUtils：连接与事务绑定](#4-datasourceutils连接与事务绑定)
5. [代理与测试用数据源](#5-代理与测试用数据源)

---

## 1. DataSource：连接来源抽象

`javax.sql.DataSource`（JDK 标准）只有两个方法——**整个连接体系都建立在它之上**：

```java
public interface DataSource extends CommonDataSource {
    Connection getConnection() throws SQLException;
    Connection getConnection(String username, String password) throws SQLException;
}
```

```text
连接获取链路：
  JdbcTemplate.getConnection
    → DataSourceUtils.getConnection(dataSource)     ★ 先查事务绑定
        → 有事务：返回"事务持有的连接"（同线程同连接）
        → 无事务：dataSource.getConnection()（连接池借出）
    → 使用完毕 → DataSourceUtils.releaseConnection（事务中不真释放！）
```

> 🎯 **核心要点**：Spring 中 DataSource 就是"连接工厂"的抽象——生产环境注入 HikariCP 的池化实现，测试注入内存库，**业务代码无感知**；Spring 对"哪种池"零偏好，靠 SPI 接入。

## 2. Spring 内置实现

| 实现 | 定位 | 适用 |
|------|------|------|
| `DriverManagerDataSource` | 每次新建连接（无池） | ❌ 教学/测试（每次都慢） |
| `SingleConnectionDataSource` | 复用单连接 | ✅ 单线程测试/工具 |
| `SimpleDriverDataSource` | 简化 DriverManager 封装 | 边缘 |
| `EmbeddedDatabaseBuilder` | 内存库（H2） | ✅ 测试/CI 环境 |

```java
// 测试数据源（无连接池，别用于生产）
DataSource ds = new SingleConnectionDataSource("jdbc:mysql://localhost:3306/db",
        "root", "123456", false);   // 第四个参数：是否每次 reset 连接

// 内存库（集成测试首选）
DataSource h2 = new EmbeddedDatabaseBuilder()
        .setType(EmbeddedDatabaseType.H2)
        .addScript("classpath:schema.sql")
        .addScript("classpath:test-data.sql")
        .build();
```

> ⚠️ 铁律：**生产环境绝不使用 DriverManagerDataSource**——每次 getConnection 建立 TCP 连接，并发下必炸；生产 = 连接池（HikariCP/Druid）。

## 3. 连接池集成：HikariCP / Druid

| 连接池 | 特点 | 默认参数要点 |
|--------|------|-------------|
| HikariCP（Boot 默认） | 快、轻、零依赖 | `maximumPoolSize=10`、`minimumIdle=10`（Boot 默认按核数折算） |
| Druid（阿里） | 监控完善（Druid Monitor）、SQL 防火墙 | 需额外依赖 `druid-spring-boot-starter` |

```yaml
# Boot 4（HikariCP 默认）
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/app?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: "******"
    hikari:
      maximum-pool-size: 20          # 最大连接数（≈ 并发峰值 × (1+线程阻塞比)）
      minimum-idle: 5                # 最小空闲
      connection-timeout: 3000       # 借连接超时（ms）——池耗尽时快速失败
      max-lifetime: 1800000          # 连接最大存活（< 数据库 wait_timeout）
      idle-timeout: 600000
      pool-name: AppHikariPool
```

| 池参数 | 建议 |
|--------|------|
| `maximumPoolSize` | 计算：`(核心数 × 2) + 有效存储磁盘数` 起步；压测校准 |
| `connectionTimeout` | 3000ms 左右（过快暴露波动，过慢拖死线程） |
| `maxLifetime` | **必须小于数据库 wait_timeout**（如 MySQL 8h 则设 30min） |
| 泄漏检测 | Hikari `leakDetectionThreshold`（如 60000 打日志） |

> ⚠️ **高频故障**：`HikariPool-1 - Connection is not available, request timed out` = 连接池耗尽——先查"连接泄漏"（事务未提交/流未关闭/线程池过大），再看池大小是否匹配并发。

## 4. DataSourceUtils：连接与事务绑定

`DataSourceUtils` 是 spring-jdbc 连接管理的核心：**同一个线程内，事务连接与普通连接是同一个**（否则事务失效）。

```java
// 手工拿连接（JdbcTemplate 内部同款逻辑，业务代码少用）
Connection conn = DataSourceUtils.getConnection(dataSource);
try {
    // ... 原生 JDBC 操作（会自动加入当前事务！）
} finally {
    DataSourceUtils.releaseConnection(conn, dataSource);   // 事务中不真关
}
```

| 关键点 | 说明 |
|--------|------|
| 线程绑定 | `TransactionSynchronizationManager` 持有 `Map<DataSource, Connection>` |
| 事务内获取 | 返回事务连接（同一物理连接）→ 原生 SQL 自动参与事务 |
| releaseConnection | 事务中仅递减引用计数，**不真正关闭**；无事务才归还池 |
| 陷阱 | 绕过 DataSourceUtils 自己 `dataSource.getConnection()` → **脱离事务**且可能泄漏 |

> 🎯 **核心要点**：这就是"**JdbcTemplate 为什么天然有事务**"——连接获取走 DataSourceUtils，先看线程绑定的事务连接；手工 `new Connection` 混用是事务失效的头号原因。

## 5. 代理与测试用数据源

| 类型 | 用途 |
|------|------|
| `TransactionAwareDataSourceProxy` | 让**第三方框架**（如旧式 ORM/工具库）的 `getConnection()` 也走事务绑定 |
| `LazyConnectionDataSourceProxy` | 延迟建连（无操作不连库），启动提速 |
| `IsolationLevelDataSourceRouter` | 按调用上下文切换隔离级别（极少用） |

```java
// 让老库（不认 Spring 事务）接入事务：用代理包一层
@Bean
public DataSource txAwareDataSource(DataSource raw) {
    return new TransactionAwareDataSourceProxy(raw);
}
```

> 💡 现代生态（JPA/MyBatis）自身识别 Spring 事务同步，一般无需代理；代理主要服务"外部工具直接调 getConnection"的存量集成。

---

**下一模块**：[05-批量操作与存储过程速查](05-批量操作与存储过程速查.md)　**返回总览**：[00-Spring JDBC组件总览](00-Spring JDBC组件总览.md)
