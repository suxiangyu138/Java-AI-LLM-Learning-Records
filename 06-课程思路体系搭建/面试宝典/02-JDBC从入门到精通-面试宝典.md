# JDBC面试宝典
> 基于JDBC(2026)课程大纲，全面覆盖JDBC核心编程与面试高频考点，从基础概念到源码级原理，从手写代码到系统设计，一站通关

## 目录
1. [一、基础概念速答](#一基础概念速答)
2. [二、深度原理剖析](#二深度原理剖析)
3. [三、实战场景题](#三实战场景题)
4. [四、手写代码题](#四手写代码题)
5. [五、系统设计题](#五系统设计题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板](#七面试回答模板)
8. [八、快速查漏补缺Checklist](#八快速查漏补缺checklist)

---

## 一、基础概念速答

### 1.1 什么是JDBC？
JDBC（Java Database Connectivity）是 **Java 官方提供的一套操作数据库的标准规范（接口）**，位于 `java.sql` 和 `javax.sql` 包中。它定义了数据库操作的统一 API，由各数据库厂商提供具体实现（驱动）。

> 💡 **本质理解**：JDBC 是一组接口，驱动才是实现。就像 USB 规范与 U 盘的关系——规范统一，实现各异。

### 1.2 JDBC 编程六步骤是什么？
| 步骤 | 操作 | 核心 API |
|------|------|-----------|
| 第一步 | 注册驱动 | `Class.forName("com.mysql.cj.jdbc.Driver")` |
| 第二步 | 获取连接 | `DriverManager.getConnection(url, user, password)` |
| 第三步 | 获取操作对象 | `connection.prepareStatement(sql)` |
| 第四步 | 执行 SQL | `preparedStatement.executeQuery()` / `executeUpdate()` |
| 第五步 | 处理结果集 | `ResultSet.next()` 遍历数据 |
| 第六步 | 释放资源 | `resultSet.close()` / `statement.close()` / `connection.close()` |

### 1.3 注册驱动的两种方式有什么区别？
| 方式 | 代码 | 原理 |
|------|------|------|
| `Class.forName()` | `Class.forName("com.mysql.cj.jdbc.Driver")` | 利用类加载机制，触发 Driver 类的静态代码块，自动向 `DriverManager` 注册 |
| `DriverManager.registerDriver()` | `new com.mysql.cj.jdbc.Driver()` | 显式创建驱动实例并注册，会导致驱动注册两次（Driver 静态块也会注册）且依赖具体实现类 |

> ⚠️ **推荐使用 `Class.forName()` 方式**，解耦、简洁，驱动类名可配置在属性文件中。

### 1.4 MySQL JDBC URL 的格式是什么？
```text
jdbc:mysql://host:port/database?参数1=值1&参数2=值2
```
| 参数 | 说明 |
|------|------|
| `useSSL=false` | 关闭 SSL 连接 |
| `serverTimezone=Asia/Shanghai` | 设置服务器时区 |
| `characterEncoding=utf8` | 设置字符编码 |
| `rewriteBatchedStatements=true` | 开启批处理优化 |
| `useServerPrepStmts=true` | 开启服务端预编译 |

### 1.5 Statement 和 PreparedStatement 有什么区别？
| 对比维度 | Statement | PreparedStatement |
|----------|-----------|-------------------|
| **SQL 执行方式** | 每次编译执行 | 预编译，重复使用执行计划 |
| **SQL 注入防护** | 不防护（拼接 SQL） | 预编译 + 参数化查询，天然防护 |
| **性能** | 低（每次编译） | 高（一次编译多次执行） |
| **可读性** | 差（字符串拼接） | 好（`?` 占位符清晰） |
| **批量操作** | 不支持 | 支持 `addBatch()` + `executeBatch()` |
| **BLOB 写入** | 困难 | 支持 `setBinaryStream()` |

> 🎯 **面试结论**：永远优先使用 `PreparedStatement`，只有在执行静态 DDL 或存储过程时考虑 `Statement`。

### 1.6 JDBC 中有哪些核心接口？
| 接口 | 作用 |
|------|------|
| `Driver` | 数据库驱动接口，每个厂商实现 |
| `DriverManager` | 驱动管理器，管理注册的驱动 |
| `Connection` | 数据库连接，代表一次会话 |
| `Statement` | SQL 语句执行器 |
| `PreparedStatement` | 预编译 SQL 执行器（继承 Statement） |
| `CallableStatement` | 存储过程调用执行器（继承 PreparedStatement） |
| `ResultSet` | 查询结果集，封装返回数据 |
| `ResultSetMetaData` | 结果集元数据（列名、列数、类型等） |
| `DatabaseMetaData` | 数据库元数据（版本、表结构、驱动信息等） |

### 1.7 ResultSet 和 ResultSetMetaData 的区别？
- **ResultSet**：存储查询返回的实际数据行，通过 `next()` 遍历，`getXxx(columnIndex/columnName)` 取值。
- **ResultSetMetaData**：描述 ResultSet 的结构信息，通过 `getMetaData()` 获取，可得到列数、列名、列类型等。

```java
ResultSetMetaData metaData = resultSet.getMetaData();
int columnCount = metaData.getColumnCount();
for (int i = 1; i <= columnCount; i++) {
    String columnName = metaData.getColumnName(i);
    String columnType = metaData.getColumnTypeName(i);
}
```

### 1.8 连接池的作用是什么？
- **复用连接**：避免频繁创建和销毁连接的开销
- **控制并发**：限制最大连接数，防止数据库被压垮
- **提高响应速度**：连接预先创建，使用时直接获取
- **统一管理**：监控连接状态、超时、泄漏检测

> 💡 每次创建连接的时间大约 100~300ms，连接池可将其降低到接近 0ms。

### 1.9 常见连接池对比
| 连接池 | 特点 | 适用场景 |
|--------|------|----------|
| **HikariCP** | 极致性能，轻量级（约130KB），零重试 | Spring Boot 2.x+ 默认连接池 |
| **Druid** | 功能丰富（监控、日志、防SQL注入），阿里巴巴出品 | 需要监控、慢SQL分析 |
| **DBCP** | Apache 出品，稳定但性能一般 | 老旧项目维护 |
| **C3P0** | 自动回收空闲连接，但性能较低 | 历史项目，Hibernate 早期默认 |

### 1.10 事务在 JDBC 中如何管理？
```java
connection.setAutoCommit(false);  // 关闭自动提交
try {
    // 业务操作...
    connection.commit();           // 提交事务
} catch (SQLException e) {
    connection.rollback();         // 回滚事务
} finally {
    connection.setAutoCommit(true);// 恢复自动提交
}
```

> ⚠️ 必须关闭自动提交（默认 true），否则每条 SQL 独立事务。

### 1.11 JDBC 支持哪些事务隔离级别？
| 隔离级别 | 值 | 脏读 | 不可重复读 | 幻读 |
|----------|-----|------|------------|------|
| `READ_UNCOMMITTED` | 1 | 可能 | 可能 | 可能 |
| `READ_COMMITTED` | 2 | 避免 | 可能 | 可能 |
| `REPEATABLE_READ` | 4 | 避免 | 避免 | 可能（InnoDB MVCC 避免） |
| `SERIALIZABLE` | 8 | 避免 | 避免 | 避免 |

```java
connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
```

### 1.12 批处理是什么？如何实现？
批处理将多条 SQL 一次性发送到数据库执行，减少网络往返次数。

```java
connection.setAutoCommit(false);
PreparedStatement ps = connection.prepareStatement("INSERT INTO emp(name) VALUES(?)");
for (int i = 0; i < 10000; i++) {
    ps.setString(1, "员工" + i);
    ps.addBatch();
    if (i % 1000 == 0) {
        ps.executeBatch();   // 每1000条提交一次
        ps.clearBatch();
    }
}
ps.executeBatch();
connection.commit();
```

### 1.13 CallableStatement 如何调用存储过程？
```java
CallableStatement cs = connection.prepareCall("{ CALL get_employee_count(?, ?) }");
cs.setInt(1, deptId);           // 输入参数
cs.registerOutParameter(2, Types.INTEGER);  // 输出参数
cs.execute();
int count = cs.getInt(2);       // 获取输出值
```

### 1.14 try-with-resources 如何管理 JDBC 资源？
Java 7+ 的 try-with-resources 可自动关闭实现了 `AutoCloseable` 的 JDBC 资源：

```java
String sql = "SELECT * FROM emp WHERE id = ?";
try (Connection conn = dataSource.getConnection();
     PreparedStatement ps = conn.prepareStatement(sql)) {
    ps.setInt(1, id);
    try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            // 处理结果
        }
    }
}
```

### 1.15 BLOB 类型如何读写？
```java
// 写入 BLOB
ps.setBinaryStream(1, fileInputStream, file.length());

// 读取 BLOB
if (rs.next()) {
    Blob blob = rs.getBlob("photo");
    InputStream is = blob.getBinaryStream();
    FileOutputStream fos = new FileOutputStream("output.jpg");
    byte[] buffer = new byte[8192];
    int len;
    while ((len = is.read(buffer)) != -1) {
        fos.write(buffer, 0, len);
    }
}
```

---

## 二、深度原理剖析

### 2.1 JDBC 的本质是什么？（阿里常问）
JDBC 的本质是 **Java 官方定义的一组数据库操作规范（接口）**，体现了面向接口编程与桥接模式的思想：

```
┌──────────────┐     implements      ┌──────────────────┐
│  java.sql    │◄────────────────────│  mysql-connector  │
│  Connection  │                     │  ConnectionImpl   │
│  Statement   │◄────────────────────│  StatementImpl    │
│  ResultSet   │◄────────────────────│  ResultSetImpl    │
└──────────────┘                     └──────────────────┘
       ▲                                       ▲
       │  interface                             │ 厂商实现
       │                                        │
┌──────┴──────────────┐               ┌─────────┴────────┐
│    Java 业务代码     │               │  Oracle / PG / ...│
└─────────────────────┘               └──────────────────┘
```

JDK 只提供接口，不提供实现。数据库厂商提供驱动 jar 包（mysql-connector-java、ojdbc 等）。切换数据库只需替换驱动 jar 包和连接信息，Java 代码无需改动。

> 🎯 这就是面向接口编程的威力——是 JDBC 最核心的设计思想。

### 2.2 深入理解 `Class.forName()` 注册驱动原理（字节常问）
当执行 `Class.forName("com.mysql.cj.jdbc.Driver")` 时：

1. JVM 使用类加载器加载 `com.mysql.cj.jdbc.Driver` 类
2. 执行该类的 **静态代码块**（`static {}`）
3. 静态块中调用 `DriverManager.registerDriver(new Driver())`
4. 驱动实例被注册到 `DriverManager` 的 `CopyOnWriteArrayList<DriverInfo> registeredDrivers` 中
5. 后续 `DriverManager.getConnection()` 遍历该列表，尝试各驱动

```java
// mysql-connector-j 8.x Driver 源码（简化）
public class Driver extends NonRegisteringDriver implements java.sql.Driver {
    static {
        try {
            DriverManager.registerDriver(new Driver());
        } catch (SQLException E) {
            throw new RuntimeException("Can't register driver!");
        }
    }
    // ...
}
```

> 💡 JDBC 4.0+（Java 6+）支持 SP I自动加载：`META-INF/services/java.sql.Driver` 文件列出驱动全类名，`ServiceLoader` 自动加载。因此现代项目中可以省略 `Class.forName()`。

### 2.3 SQL 注入原理与预编译防护机制
**SQL 注入原理**：通过拼接字符串构造 SQL，用户输入被当作 SQL 代码执行：

```java
// 危险写法
String sql = "SELECT * FROM user WHERE name='" + userName + "' AND pwd='" + password + "'";
// 输入：userName = "admin' -- "，SQL 变为：
// SELECT * FROM user WHERE name='admin' -- ' AND pwd='xxx'
// -- 注释了后续验证，绕过了密码检查
```

**预编译防护原理**：
1. SQL 模板提前发送给数据库编译，`?` 占位符确定参数位置
2. 参数值通过二进制协议传输，不做 SQL 解析
3. 数据库将参数值作为纯数据处理，而非 SQL 语句

```java
// 安全写法
String sql = "SELECT * FROM user WHERE name=? AND pwd=?";
PreparedStatement ps = conn.prepareStatement(sql);  // 预编译
ps.setString(1, userName);  // 参数值被当作纯数据
ps.setString(2, password);
```

即使参数中包含 `admin' OR '1'='1`，数据库也会将其作为完整的字符串值匹配，不会拼接成 SQL。

### 2.4 PreparedStatement 预编译的完整流程
1. **创建阶段**：`conn.prepareStatement(sql)` 将 SQL 发送到数据库
2. **编译阶段**：数据库解析 SQL 语法，生成执行计划
3. **参数绑定**：`ps.setXxx(index, value)` 设置参数值
4. **执行阶段**：`ps.executeQuery()` 使用已编译的计划 + 参数值执行
5. **重用阶段**：再次 `executeQuery()` 可绑定不同参数，跳过编译

> ⚠️ MySQL 默认客户端预编译，`useServerPrepStmts=true` 开启服务端预编译才真正在服务端缓存执行计划。

### 2.5 数据库连接池核心原理（阿里P6面试题）
连接池本质上是一个 **生产者-消费者模式** 的资源池：

```
┌────────────────── 连接池 ──────────────────┐
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
│  │ Connection│  │ Connection│  │ Connection│  │  ← 空闲连接（LinkedList/Deque）
│  └──────────┘  └──────────┘  └──────────┘  │
│                                            │
│  活跃连接数：3    等待线程数：0             │
└────────────────────────────────────────────┘
```

**核心参数**：
| 参数 | HikariCP 名称 | 默认值 | 说明 |
|------|--------------|--------|------|
| 初始连接数 | `minimumIdle` | 10 | 池中最小空闲连接数 |
| 最大连接数 | `maximumPoolSize` | 10 | 池中最大连接数（含活跃和空闲） |
| 超时时间 | `connectionTimeout` | 30000ms | 获取连接的超时等待时间 |
| 最大存活时间 | `maxLifetime` | 1800000ms | 连接最大存活时间（比 MySQL wait_timeout 短） |
| 空闲检测 | `idleTimeout` | 600000ms | 空闲连接被回收的超时时间 |

**获取连接流程**：
1. 检查池中是否有空闲连接
2. 有则直接返回（复用）
3. 无则检查是否达到 `maximumPoolSize`
4. 未达到则创建新连接
5. 达到则等待 `connectionTimeout`，超时抛异常

**归还连接流程**：
1. 调用 `connection.close()`（实际非关闭，而是放回池中）
2. 连接池将连接状态重置（清空事务、结果集等）
3. 连接标记为空闲，放入空闲队列

### 2.6 HikariCP 为什么性能极优？
| 优化点 | 实现 |
|--------|------|
| **代码极致精简** | 约 130KB，类加载快，方法内联友好 |
| **无锁集合** | 使用 `ConcurrentBag` 而非 `LinkedBlockingQueue` |
| **FastList** | 替代 `ArrayList`，优化 `Statement` 关闭性能 |
| **代理模式优化** | 动态代理用 `Javassist` 而非 JDK Proxy，减少反射开销 |
| **零重试机制** | 获取连接失败立即抛异常，避免无效等待 |
| **池大小算法** | `poolSize = Tn * (Cm - 1) + 1`（Tn=CPU核数，Cm=并发数），避免上下文切换 |

### 2.7 Druid 的核心功能有哪些？
| 功能 | 说明 |
|------|------|
| **连接池** | 基本连接池功能（初始化、扩展、收缩、泄漏检测） |
| **监控统计** | DWS 页面实时监控 SQL 执行、连接、Session |
| **慢 SQL 日志** | `setSlowSqlMillis` 配置慢 SQL 阈值并记录日志 |
| **SQL 防火墙** | `WallFilter` 防止 SQL 注入，拦截高危操作 |
| **日志记录** | `LogFilter` 记录所有 SQL 和参数 |
| **密码加密** | `ConfigTools` 支持数据库密码加密存储 |

### 2.8 Statement 和 PreparedStatement 在缓存层面的区别
- **Statement**：每次执行 `executeQuery()` 时，数据库会解析 SQL、生成执行计划、执行。无缓存。
- **PreparedStatement**：**客户端缓存** — `connection.prepareStatement(sql)` 返回的 `PreparedStatement` 对象可重复使用。**服务端缓存**（需 `useServerPrepStmts=true`）— 数据库缓存执行计划，相同 SQL 直接复用。

### 2.9 JDBC 事务提交与回滚机制深度解析
MySQL InnoDB 引擎的 DML 操作自动包裹事务。JDBC 层面：

```
connection.setAutoCommit(true) (默认)
    ↓
每执行一条 SQL，自动调用 commit()
    ↓
遇到异常，自动调用 rollback()

connection.setAutoCommit(false)
    ↓
所有 SQL 在同一个事务中
    ↓
必须手动调用 commit() 或 rollback()
    ↓
不 commit 不回滚，连接归还到池时，事务状态不确定！
```

> ⚠️ **连接池 + 事务的常见坑**：`setAutoCommit(false)` 后，如果在 `finally` 中忘记 `setAutoCommit(true)`，归还连接后下次获取时仍处于非自动提交模式，导致 SQL 执行后不提交，造成死锁或数据不一致。

### 2.10 Savepoint 保存点/回滚点
```java
connection.setAutoCommit(false);
Savepoint savepoint1 = connection.setSavepoint("step1");
try {
    // 操作1...
    Savepoint savepoint2 = connection.setSavepoint("step2");
    // 操作2...
    connection.commit();
} catch (SQLException e) {
    connection.rollback(savepoint1);  // 回滚到保存点
    connection.commit();
}
```

---

## 三、实战场景题

### 3.1 生产环境数据库连接突然耗尽，如何排查？
**问题排查步骤**：
1. **确认连接池状态**：通过 Druid Monitor 或 HikariCP Metrics 查看活跃连接数
2. **检查慢 SQL**：`SHOW FULL PROCESSLIST` 查看长时间运行的查询
3. **确认事务未提交**：`SELECT * FROM information_schema.innodb_trx` 查看长时间运行的事务
4. **检查连接泄漏**：启用连接池泄漏检测
   - Druid：`setRemoveAbandoned(true)` + `setRemoveAbandonedTimeout(300)`
   - HikariCP：`setLeakDetectionThreshold(60000)`
5. **分析代码**：在 `finally` 或 try-with-resources 中确保 `close()` 被调用

**解决方案**：
- 增加连接池 `maximumPoolSize`
- 优化慢 SQL，减少锁等待时间
- 缩短事务范围
- 使用 `setQueryTimeout()` 设置查询超时

### 3.2 分页查询如何实现？百万级数据分页如何优化？
**基础分页**（MySQL）：
```java
String sql = "SELECT * FROM emp ORDER BY id LIMIT ?, ?";
ps.setInt(1, offset);  // 偏移量
ps.setInt(2, pageSize); // 每页条数
```

**深度分页优化**（Offset 过大时性能急剧下降）：
```sql
-- 方案1：子查询优化（利用覆盖索引）
SELECT * FROM emp
WHERE id >= (SELECT id FROM emp ORDER BY id LIMIT 100000, 1)
ORDER BY id LIMIT 20;

-- 方案2：游标分页（基于上一页最后ID）
SELECT * FROM emp
WHERE id > #{lastId}
ORDER BY id LIMIT 20;

-- 方案3：延迟关联
SELECT e.* FROM emp e
INNER JOIN (SELECT id FROM emp ORDER BY id LIMIT 100000, 20) tmp
ON e.id = tmp.id;
```

### 3.3 批量插入 10 万条数据，如何优化性能？
```java
// 最差：逐条提交（每条独立事务 + 网络往返）
for (Employee emp : list) {
    String sql = "INSERT INTO emp(name) VALUES('" + emp.getName() + "')";
    statement.execute(sql);
}

// 较好：PreparedStatement + 批处理
String sql = "INSERT INTO emp(name, age, dept_id) VALUES(?, ?, ?)";
PreparedStatement ps = conn.prepareStatement(sql);
conn.setAutoCommit(false);  // 关闭自动提交
for (int i = 0; i < employees.size(); i++) {
    Employee emp = employees.get(i);
    ps.setString(1, emp.getName());
    ps.setInt(2, emp.getAge());
    ps.setInt(3, emp.getDeptId());
    ps.addBatch();
    if (i % 5000 == 0) {
        ps.executeBatch();
        ps.clearBatch();
    }
}
ps.executeBatch();          // 执行剩余批次
conn.commit();              // 提交事务

// 最优：开启 rewriteBatchedStatements
// URL 添加 ?rewriteBatchedStatements=true
// MySQL 会将多条 INSERT 重写为一条多值 INSERT：
// INSERT INTO emp(name,age,dept_id) VALUES ('a',1,1),('b',2,2),...
```

> 💡 `rewriteBatchedStatements=true` 可提升批处理性能 10-100 倍。

### 3.4 模糊查询时 PreparedStatement 如何使用？
```java
String sql = "SELECT * FROM emp WHERE name LIKE ?";
PreparedStatement ps = conn.prepareStatement(sql);
ps.setString(1, "%" + keyword + "%");  // 在参数中拼接 %
ResultSet rs = ps.executeQuery();
```

> ⚠️ 不要在 SQL 模板中写 `LIKE '%?%'`，占位符不支持这种写法。必须在 setString 时拼接 `%`。

### 3.5 如何在获取新增数据的自增主键？
```java
String sql = "INSERT INTO emp(name, age) VALUES(?, ?)";
PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
ps.setString(1, "张三");
ps.setInt(2, 25);
ps.executeUpdate();

ResultSet generatedKeys = ps.getGeneratedKeys();
if (generatedKeys.next()) {
    long id = generatedKeys.getLong(1);
    // id 即为自增主键值
}
```

### 3.6 服务端预编译（Server-Side PreparedStatement）使用技巧
```java
// JDBC URL 开启服务端预编译
String url = "jdbc:mysql://localhost:3306/db?useServerPrepStmts=true&cachePrepStmts=true";

// 相同 SQL 被执行多次时，服务端缓存执行计划，大幅提升性能
for (int i = 0; i < 10000; i++) {
    // 仅在第一次 prepareStatement 时编译，后续使用缓存
    try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM emp WHERE id = ?")) {
        ps.setInt(1, i);
        ps.executeQuery();
    }
}
```

### 3.7 连接池参数如何合理配置？
| 场景 | minimumIdle | maximumPoolSize | connectionTimeout | maxLifetime |
|------|-------------|-----------------|-------------------|-------------|
| 高并发 OLTP 系统 | 20 | 50 | 5000ms | 1800000ms |
| 后台批处理任务 | 5 | 10 | 30000ms | 3600000ms |
| 低流量管理后台 | 2 | 10 | 10000ms | 1800000ms |
| 微服务 API 层 | 10 | 30 | 5000ms | 1800000ms |

> 💡 HikariCP 官方建议 `maximumPoolSize` 不宜过大。经验值 `maxPoolSize = Tn * (Cm - 1) + 1`，其中 Tn = CPU 核数，Cm = 单连接并发数。过多连接反而因上下文切换导致性能下降。

---

## 四、手写代码题

### 4.1 手写 JDBC 工具类（DBUtil）
```java
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * JDBC 工具类（基于 HikariCP 连接池）
 */
public class DBUtil {

    private static final DataSource DATA_SOURCE;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/emp_db?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8");
        config.setUsername("root");
        config.setPassword("root123");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMinimumIdle(5);           // 最小空闲连接数
        config.setMaximumPoolSize(20);       // 最大连接数
        config.setConnectionTimeout(5000);   // 获取连接超时（ms）
        config.setIdleTimeout(600000);       // 空闲超时（ms）
        config.setMaxLifetime(1800000);      // 最大存活时间（ms）
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        DATA_SOURCE = new HikariDataSource(config);
    }

    /** 获取数据库连接 */
    public static Connection getConnection() throws SQLException {
        return DATA_SOURCE.getConnection();
    }

    /** 释放资源（归还连接到连接池） */
    public static void close(Connection conn, PreparedStatement ps, ResultSet rs) {
        try { if (rs != null) rs.close(); } catch (SQLException e) { e.printStackTrace(); }
        try { if (ps != null) ps.close(); } catch (SQLException e) { e.printStackTrace(); }
        try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
    }

    /** 释放资源（无结果集） */
    public static void close(Connection conn, PreparedStatement ps) {
        close(conn, ps, null);
    }
}
```

### 4.2 手写通用 BaseDao（使用反射+泛型）
```java
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 通用 DAO 基类 - 封装通用 CRUD 操作
 */
public abstract class BaseDao<T> {

    private Class<T> entityClass;

    @SuppressWarnings("unchecked")
    public BaseDao() {
        // 通过反射获取子类泛型 T 的实际类型
        ParameterizedType pt = (ParameterizedType) this.getClass().getGenericSuperclass();
        entityClass = (Class<T>) pt.getActualTypeArguments()[0];
    }

    /** 通用更新方法（INSERT / UPDATE / DELETE） */
    public int executeUpdate(String sql, Object... params) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBUtil.getConnection();
            ps = conn.prepareStatement(sql);
            setParameters(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("数据更新失败", e);
        } finally {
            DBUtil.close(conn, ps);
        }
    }

    /** 通用查询方法 - 返回单个对象 */
    public T queryOne(String sql, Object... params) {
        List<T> list = queryList(sql, params);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 通用查询方法 - 返回对象列表 */
    public List<T> queryList(String sql, Object... params) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<T> list = new ArrayList<>();
        try {
            conn = DBUtil.getConnection();
            ps = conn.prepareStatement(sql);
            setParameters(ps, params);
            rs = ps.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            while (rs.next()) {
                T entity = entityClass.getDeclaredConstructor().newInstance();
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    String columnName = metaData.getColumnName(i);
                    Object columnValue = rs.getObject(i);
                    // 下划线转驼峰（如 dept_id -> deptId）
                    String fieldName = toCamelCase(columnName);
                    Field field = entityClass.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    field.set(entity, columnValue);
                }
                list.add(entity);
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("数据查询失败", e);
        } finally {
            DBUtil.close(conn, ps, rs);
        }
    }

    /** 设置 SQL 参数 */
    private void setParameters(PreparedStatement ps, Object... params) throws SQLException {
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
        }
    }

    /** 下划线转驼峰 */
    private String toCamelCase(String name) {
        StringBuilder sb = new StringBuilder();
        boolean needUpper = false;
        for (char c : name.toCharArray()) {
            if (c == '_') {
                needUpper = true;
            } else if (needUpper) {
                sb.append(Character.toUpperCase(c));
                needUpper = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
```

### 4.3 手写员工 DAO 实现
```java
public class EmployeeDaoImpl extends BaseDao<Employee> {

    /** 新增员工 */
    public int insert(Employee emp) {
        String sql = "INSERT INTO emp(name, age, dept_id, salary) VALUES(?, ?, ?, ?)";
        return executeUpdate(sql, emp.getName(), emp.getAge(), emp.getDeptId(), emp.getSalary());
    }

    /** 根据 ID 查询 */
    public Employee findById(Integer id) {
        String sql = "SELECT id, name, age, dept_id, salary FROM emp WHERE id = ?";
        return queryOne(sql, id);
    }

    /** 查询所有员工 */
    public List<Employee> findAll() {
        String sql = "SELECT id, name, age, dept_id, salary FROM emp";
        return queryList(sql);
    }

    /** 分页查询 */
    public List<Employee> findByPage(int pageNum, int pageSize) {
        String sql = "SELECT id, name, age, dept_id, salary FROM emp LIMIT ?, ?";
        return queryList(sql, (pageNum - 1) * pageSize, pageSize);
    }

    /** 更新员工信息 */
    public int update(Employee emp) {
        String sql = "UPDATE emp SET name=?, age=?, dept_id=?, salary=? WHERE id=?";
        return executeUpdate(sql, emp.getName(), emp.getAge(), emp.getDeptId(), emp.getSalary(), emp.getId());
    }

    /** 删除员工 */
    public int deleteById(Integer id) {
        String sql = "DELETE FROM emp WHERE id = ?";
        return executeUpdate(sql, id);
    }
}
```

### 4.4 手写使用 Druid 连接池的配置
```java
import com.alibaba.druid.pool.DruidDataSource;
import javax.sql.DataSource;
import java.sql.Connection;

public class DruidUtil {

    private static DruidDataSource dataSource;

    static {
        dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mysql://localhost:3306/emp_db?useSSL=false");
        dataSource.setUsername("root");
        dataSource.setPassword("root123");
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // 连接池配置
        dataSource.setInitialSize(5);             // 初始化连接数
        dataSource.setMinIdle(5);                 // 最小空闲连接
        dataSource.setMaxActive(20);              // 最大活跃连接数
        dataSource.setMaxWait(5000);              // 获取连接超时等待（ms）

        // 检测配置
        dataSource.setTestWhileIdle(true);
        dataSource.setTestOnBorrow(false);
        dataSource.setValidationQuery("SELECT 1");
        dataSource.setTimeBetweenEvictionRunsMillis(60000);

        // 监控与慢 SQL
        dataSource.setFilters("stat,wall,log4j2");
        dataSource.setConnectionProperties("druid.stat.slowSqlMillis=2000");
    }

    public static Connection getConnection() throws Exception {
        return dataSource.getConnection();
    }

    public static DataSource getDataSource() {
        return dataSource;
    }

    public static void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
```

### 4.5 手写事务管理工具方法
```java
/**
 * 使用模板方法模式管理事务
 */
public class TransactionTemplate {

    @FunctionalInterface
    public interface TransactionCallback<T> {
        T doInTransaction() throws SQLException;
    }

    public static <T> T execute(TransactionCallback<T> callback) throws SQLException {
        Connection conn = DBUtil.getConnection();
        boolean originalAutoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);
            T result = callback.doInTransaction();
            conn.commit();
            return result;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(originalAutoCommit);
            DBUtil.close(conn, null);
        }
    }
}

// 使用示例
TransactionTemplate.execute(() -> {
    EmployeeDaoImpl dao = new EmployeeDaoImpl();
    dao.insert(new Employee("张三", 25, 1, 15000.0));
    dao.deleteById(100);
    return null;
});
```

---

## 五、系统设计题

### 5.1 设计一个高并发系统的数据库连接方案
**需求**：日均 1 亿请求，峰值 QPS 20000，数据库为 MySQL 8.x 集群。

**架构设计**：
```
┌─────────┐     ┌──────────┐     ┌───────────┐
│ Client  │────▶│ API网关  │────▶│ 微服务 A  │──┐
└─────────┘     └──────────┘     └───────────┘  │
                                               │
                                         ┌─────┴──────┐
                                         │  连接池层    │
                                         │ (HikariCP)  │
                                         │  maxPool=30 │
                                         └─────┬──────┘
                                               │
                                         ┌─────┴──────┐
                                         │  读写分离    │
                                         │  Sharding   │
                                         └─────┬──────┘
                                    ┌──────────┼──────────┐
                                    ▼          ▼          ▼
                                ┌───────┐ ┌───────┐ ┌───────┐
                                │  Master│ │ Slave1│ │ Slave2│
                                └───────┘ └───────┘ └───────┘
```

**关键设计决策**：
1. **连接池选择**：HikariCP，每个微服务实例独立连接池，10~30 连接
2. **读写分离**：使用 ShardingSphere 或自研路由，写走主库，读走从库
3. **连接池隔离**：写操作和读操作使用不同的连接池（写池较小，读池较大）
4. **慢 SQL 治理**：Druid Monitor 或自研慢 SQL 采集系统
5. **熔断降级**：连接池获取超时触发熔断，返回降级结果
6. **连接保活**：定时 `SELECT 1` 检测连接状态

### 5.2 设计一个通用的 DAO 框架（面试高频）
**要求**：支持泛型、分页、条件查询排序、批量操作。

```java
public interface GenericDao<T, ID> {
    int insert(T entity);
    int batchInsert(List<T> entities);
    int update(T entity);
    int deleteById(ID id);
    T findById(ID id);
    List<T> findAll();
    PageResult<T> findPage(PageRequest pageRequest, T queryCondition);
    long count(T queryCondition);
}

public class PageRequest {
    private int pageNum;
    private int pageSize;
    private String orderBy;
    private boolean asc;
    // getter / setter
}

public class PageResult<T> {
    private List<T> data;
    private long total;
    private int pageNum;
    private int pageSize;
    private int totalPages;
    // getter / setter
}
```

### 5.3 设计一个防止 SQL 注入的统一解决方案
**多层防护策略**：

| 层级 | 措施 | 说明 |
|------|------|------|
| **代码层** | 强制使用 PreparedStatement | 禁止 Statement + 拼接 SQL，FindBugs/SpotBugs 扫描 |
| **ORM 层** | MyBatis/MyBatis-Plus | 使用 `#{}` 而非 `${}`，动态 SQL 使用安全拼接 |
| **网络层** | Druid WallFilter | 配置 SQL 防火墙规则，拦截 `DROP`、恶意注入 |
| **数据库层** | 最小权限原则 | 应用账户仅有 DML 权限，无 DDL 权限 |
| **输入层** | 输入校验过滤 | 对特殊字符进行转义或拦截 |

### 5.4 分布式系统下的事务一致性方案
JDBC 本地事务无法解决跨库/跨服务问题，需要分布式事务方案：

| 方案 | 适用场景 | 一致性 | 性能 |
|------|----------|--------|------|
| **XA 两阶段提交** | 短事务、强一致性 | 强一致 | 低（同步阻塞） |
| **TCC（Try-Confirm-Cancel）** | 长事务、高可用 | 最终一致 | 中 |
| **Saga（编排/协同）** | 长事务、高吞吐 | 最终一致 | 高 |
| **Seata AT 模式** | 对业务侵入小 | 最终一致 | 中 |
| **本地消息表 + 消息队列** | 异步场景 | 最终一致 | 高 |

---

## 六、常见坑点与最佳实践

### 6.1 常见坑点汇总

| 坑点 | 错误示例 | 正确做法 |
|------|----------|----------|
| **SQL 注入** | `"SELECT * FROM user WHERE name='" + name + "'"` | 使用 `PreparedStatement` + `?` 占位符 |
| **忘记关闭资源** | 未在 `finally` 中 `close()` | try-with-resources 或 `finally` 中关闭 |
| **连接池泄漏** | 获取连接后未 `close()` | `close()` 实际归还连接池，必须调用 |
| **事务不回滚** | `setAutoCommit(false)` 后异常时未 `rollback()` | `catch` 中 `rollback()`，`finally` 中恢复 `setAutoCommit(true)` |
| **分页性能差** | `LIMIT 1000000, 20` 深度分页 | 使用游标分页或子查询优化 |
| **BLOB 太大加载到内存** | `blob.getBytes(0, (int)blob.length())` | 使用 `getBinaryStream()` 流式读取 |
| **MySQL 连接 8h 断开** | 连接池 `maxLifetime` 未设置 | `maxLifetime` 设置小于 `wait_timeout`（默认 28800s） |
| **批处理无优化** | 未添加 `rewriteBatchedStatements=true` | JDBC URL 添加该参数，获得 10-100 倍提升 |
| **连接池过大** | `maximumPoolSize` 配置为 200 | 根据 CPU 核数配置（一般 10~50），过大导致上下文切换 |
| **异常后连接状态脏** | 事务异常后继续复用连接 | 事务异常后应关闭连接或重置 |

### 6.2 JDBC 最佳实践清单

- [x] 始终使用 `PreparedStatement` 替代 `Statement`
- [x] 使用连接池（HikariCP / Druid）管理连接
- [x] 资源释放使用 try-with-resources（Java 7+）
- [x] 事务操作必须 `setAutoCommit(false)`，`finally` 中恢复
- [x] 连接池 `maxLifetime` 小于数据库 `wait_timeout`
- [x] 批处理添加 `rewriteBatchedStatements=true`
- [x] 查询参数拼接 `%` 放在 setString 中，不在 SQL 模板中
- [x] 连接信息配置在属性文件或配置中心，不硬编码
- [x] 数据库密码加密存储（Druid ConfigTools / 配置中心）
- [x] 使用 `ResultSetMetaData` 实现通用查询，避免硬编码列名
- [x] 大字段（BLOB/CLOB）使用流式读写
- [x] 为连接池配置泄漏检测参数
- [x] SQL 执行设置超时时间（`statement.setQueryTimeout(10)`）

---

## 七、面试回答模板

### 7.1 "请说一下 JDBC 中 PreparedStatement 是怎么防止 SQL 注入的？"

**回答模板**：
> SQL 注入的本质是用户输入被当作 SQL 代码执行。PreparedStatement 通过两种机制防止：
>
> 第一是 **预编译机制**。SQL 模板在发送到数据库时就被编译成执行计划，`?` 占位符确定了参数的数据位置。后续设置的参数值通过二进制协议传输，数据库只将其作为纯数据填充到执行计划中，不再进行 SQL 语法解析。
>
> 第二是 **参数化查询**。调用 `setString()` / `setInt()` 等方法时，驱动会对特殊字符进行转义处理。比如单引号 ' 会被转义为 ''，确保不会被解析为字符串结束符。
>
> 而 Statement 是通过字符串拼接构造 SQL，用户输入中的 `' OR '1'='1` 等恶意内容会被直接拼接到 SQL 中，改变 SQL 语义，导致注入。
>
> 以 `SELECT * FROM user WHERE name=? AND pwd=?` 为例，即使用户输入 `admin' --`，PreparedStatement 也只将其作为完整的字符串值去匹配 name 字段，而不会改变 SQL 结构。

### 7.2 "HikariCP 为什么比 DBCP/C3P0 快？"

**回答模板**：
> HikariCP 是目前性能最优的 JDBC 连接池，主要体现在以下几个方面：
>
> 第一，**代码极致精简**。整个 jar 包只有约 130KB，相比 DBCP 和 C3P0 大幅减少。更少的代码意味着更少的对象分配、更少的类加载、更好的 CPU 缓存命中率，并且 JIT 编译内联更加高效。
>
> 第二，**优化的数据结构**。HikariCP 使用自定义的 `ConcurrentBag`（一种无锁集合）替代传统的 `LinkedBlockingQueue`。`ConcurrentBag` 采用 `ThreadLocal` 缓存 + `CopyOnWriteArrayList` 的组合策略，减少了线程竞争。
>
> 第三，**FastList 替代 ArrayList**。HikariCP 自定义了 `FastList` 替换 `ArrayList`，在 `Statement` 关闭时移除了范围检查，性能提升明显。
>
> 第四，**代理模式优化**。HikariCP 使用 Javassist 生成动态代理，相比 JDK Proxy 效率更高，因为 JDK Proxy 基于反射，而 Javassist 通过字节码生成。
>
> 第五，**零重试机制**。获取连接失败时立即返回 null 或抛异常，不做无效重试，避免浪费。
>
> 这些优化措施叠加在一起，使得 HikariCP 在微基准测试中通常比 C3P0 快 10-20 倍，比 DBCP 快 3-5 倍。

### 7.3 "项目中数据库连接池参数你是如何配置的？"

**回答模板**：
> 我们项目使用 HikariCP 作为连接池，配置参数如下：
>
> **连接数配置**：`minimumIdle=10`，`maximumPoolSize=30`。根据经验公式 `poolSize = CPU核数 × (单连接并发数 - 1) + 1`，我们的服务跑在 8 核机器上，单连接处理一个请求耗时约 10ms（含数据库查询），单连接 TPS 约为 100，目标 TPS 为 2000，所需连接数大约为 20~30。连接数不是越大越好，过多会导致上下文切换和资源争用。
>
> **超时配置**：`connectionTimeout=5000ms`（等待连接超时），`socketTimeout=10000ms`（等待数据库响应超时）。适当短的超时可以快速失败，避免线程阻塞堆积。
>
> **连接存活配置**：`maxLifetime=1800000ms`（30分钟），`idleTimeout=600000ms`（10分钟）。`maxLifetime` 必须小于 MySQL 的 `wait_timeout`（默认 8 小时），防止连接被服务端断开后客户端仍在使用。
>
> 另外开启了 `cachePrepStmts=true` 和 `useServerPrepStmts=true` 以提升 PreparedStatement 的缓存性能，还开启了 `leakDetectionThreshold=60000` 来检测连接泄漏。

### 7.4 "说一下 DAO 设计模式的优缺点"

**回答模板**：
> DAO（Data Access Object）模式通过抽象出数据访问层，将数据存取操作与业务逻辑分离。
>
> **优点**：
> 1. **关注点分离**：业务层只关心业务逻辑，不关心数据库操作细节
> 2. **可维护性强**：数据访问逻辑集中管理，修改数据库表结构只需要改动 DAO 层
> 3. **可测试性强**：DAO 接口方便 Mock，便于单元测试
> 4. **数据库无关性**：更换数据库时只需替换 DAO 实现类，业务层代码无感知
> 5. **代码复用**：通用的 CRUD 操作可以抽取到 BaseDao 中
>
> **在实际项目中**，我们通常使用 BaseDao + 泛型的方式，将通用的增删改查抽取出来，子类只实现特定的复杂查询。这样即使有 100 张表，每张表的 DAO 也就几行代码。
>
> **缺点**：
> 1. 对于简单项目，DAO 层可能显得冗余，增加了类数量
> 2. 接口与实现分离导致间接性，调试时需要多跳一层
> 3. 如果设计不合理，容易出现通用方法不够用、专有方法又太多的情况
>
> 现代项目中，MyBatis-Plus 等 ORM 框架已经内置了通用 BaseMapper，本质上是 DAO 模式思想的发展和自动化。

### 7.5 "JDBC 事务和 Spring 事务是什么关系？"

**回答模板**：
> Spring 事务管理底层就是基于 JDBC 事务实现的。
>
> Spring 提供了 `PlatformTransactionManager` 接口，其中针对 JDBC 的实现是 `DataSourceTransactionManager`。它的核心机制是：
>
> 1. **获取连接**：从 `DataSource` 获取 `Connection`
> 2. **绑定事务**：将当前连接绑定到当前线程（通过 `ThreadLocal`）
> 3. **事务操作**：调用 `connection.setAutoCommit(false)`、`connection.commit()`、`connection.rollback()`
> 4. **传播行为实现**：事务传播基于同一个 `ThreadLocal` 中的连接是否存在来判断
>
> 当一个方法标注 `@Transactional` 时，Spring AOP 会在方法执行前开启事务（关闭自动提交），方法执行成功后提交，异常时回滚。
>
> 从本质上讲，Spring 事务就是 JDBC 事务的一层封装，JDBC 事务是数据库事务在 Java 层面的体现，数据库事务（InnoDB）才是真正的底层实现。

---

## 八、快速查漏补缺Checklist

### 8.1 JDBC 核心概念
- [ ] 能说出 JDBC 的本质（接口规范 + 驱动实现）
- [ ] 能画出 JDBC 四层架构图
- [ ] 知道 SP I机制（JDBC 4.0 自动驱动加载）
- [ ] 能说出 `DriverManager` 的工作原理

### 8.2 编程与 API
- [ ] 能默写 JDBC 六步（含 try-with-resources 写法）
- [ ] 能写出完整的分页查询代码
- [ ] 能写出批处理优化代码
- [ ] 能写出获取自增主键的代码
- [ ] 能写出 BLOB 读写完整代码
- [ ] 能写出存储过程调用代码

### 8.3 SQL 注入与安全
- [ ] 能解释 SQL 注入原理并演示
- [ ] 能解释 PreparedStatement 的防护机制
- [ ] 知道 `#{}` 和 `${}` 的区别（MyBatis 场景）
- [ ] 知道 SQL 注入的多种防护手段

### 8.4 事务管理
- [ ] 能写出 JDBC 事务管理的完整代码
- [ ] 能设置和解释四种隔离级别
- [ ] 能解释 Savepoint 的使用场景
- [ ] 知道 `setAutoCommit(false)` 的注意事项
- [ ] 知道连接池 + 事务的常见坑点

### 8.5 DAO 与 BaseDao
- [ ] 能画出 DAO 模式的分层图
- [ ] 能写出带泛型的 BaseDao
- [ ] 能解释反射 + `ResultSetMetaData` 的实现原理
- [ ] 能写出下划线转驼峰的代码

### 8.6 连接池
- [ ] 能解释连接池的工作原理
- [ ] 能配置 HikariCP 和 Druid
- [ ] 能说出连接池核心参数含义
- [ ] 能解释 HikariCP 性能优势的原因
- [ ] 知道 Druid 的监控、防火墙等功能

### 8.7 性能优化
- [ ] 知道 `rewriteBatchedStatements` 的作用
- [ ] 知道 `useServerPrepStmts` 的作用
- [ ] 知道深度分页的优化方案
- [ ] 能够计算合理的连接池大小
- [ ] 知道连接泄漏的排查方法

---

> 🎯 **面试终极总结**：JDBC 面试的核心在于理解"规范与实现分离"的设计思想，以及 PreparedStatement、事务管理、连接池这三个高频考点的原理深度。手写 BaseDao 和连接池配置是最常见的编程题。按照本宝典的八大模块逐一对照，即可全面备战 JDBC 面试。
