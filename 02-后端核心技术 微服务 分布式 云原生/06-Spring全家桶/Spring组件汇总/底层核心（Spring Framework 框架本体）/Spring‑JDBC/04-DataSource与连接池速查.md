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

### 1.1 为什么是 javax.sql.DataSource（SPI 设计）

DataSource 是 JDBC 2.0 引入的"连接工厂"标准——它取代了 `DriverManager` 的全局静态注册机制：

| 对比 | DriverManager | DataSource |
|------|---------------|-----------|
| 注册方式 | 静态全局（Class.forName 加载驱动） | 实例对象注入（Spring 管理） |
| 连接管理 | 每次新建 | 可由实现者池化 |
| 可测试性 | 差（全局状态） | 好（任意替换实现） |
| 扩展能力 | 无 | 可包装（事务代理 / 路由 / XA） |

```java
// Spring 中的典型装配：DataSource 就是普通 Bean
@Bean
public DataSource dataSource() {
    HikariDataSource ds = new HikariDataSource();
    ds.setJdbcUrl("jdbc:mysql://localhost:3306/app");
    ds.setUsername("root");
    ds.setPassword("******");
    return ds;
}
```

> 🎯 面试点：DataSource 接口只有 `getConnection()` 两个方法——**一切"连接池 / 事务 / 路由"能力都来自实现类包装**；这就是 Spring 能零侵入接入 HikariCP/Druid/测试内存库的原因。

### 1.2 常见 DataSource 实现盘点

| 实现 | 类型 | 一句话特点 |
|------|------|-----------|
| HikariDataSource | 池 | 快、轻，Boot 默认 |
| DruidDataSource | 池 | 监控完善、SQL 防火墙 |
| DBCP2 / Tomcat JDBC | 池 | 容器配套场景 |
| Atomikos / Narayana | XA 池 | 分布式事务专用 |
| H2 JdbcDataSource | 单连接 | 测试 / 工具 |
| Spring 内置（DriverManager 等） | 无池 | 教学 / 测试 |

识别技巧：看日志或监控里"每次 getConnection 是否新建物理连接"就能判断数据源类型——池实现复用连接，无池实现每次都握手。面试答"连接池的本质"：池是 DataSource 的一种实现，把"建连 - 校验 - 借出 - 归还 - 回收"做成可配置的生命周期管理；补充一个盲区：`spring.datasource.type` 属性可以强制指定数据源实现类，但现代 Boot 4 优先按 classpath 自动探测——排查"池参数改了没生效"前先确认数据源类型，这是最常见的误区。

再补一个面试细节：`DataSource` 与 `Connection` 的关系是"工厂与产品"——每次 getConnection 返回的连接可能是同一物理连接的不同包装（池复用），因此**判断连接是否相同用引用没有意义**，"是否同一事务"要看 DataSourceUtils 的绑定关系而不是 `==`；这个细节在排查"为什么两个连接表现不一致"时很关键。数据源类型决定参数是否生效，排障时永远先确认这一点。

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

### 2.1 内置实现的行为差异与选型建议

| 实现 | 每次 getConnection | 连接复用 | 适用 |
|------|------------------|---------|------|
| `DriverManagerDataSource` | 新建物理连接 | 无 | 仅教学 / 一次性脚本 |
| `SingleConnectionDataSource` | 返回同一连接 | 单连接复用 | 单线程测试 / 工具 |
| `SimpleDriverDataSource` | 新建物理连接 | 无 | 省去 Class.forName 的边缘场景 |
| `EmbeddedDatabaseBuilder` | 内存库连接 | H2 自行管理 | 集成测试 / CI |

```java
// SingleConnectionDataSource 的正确打开方式（单线程测试）
SingleConnectionDataSource ds = new SingleConnectionDataSource();
ds.setDriverClassName("org.h2.Driver");
ds.setUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
ds.setSuppressClose(true);            // close() 不真正关闭底层连接（复用）

JdbcTemplate tpl = new JdbcTemplate(ds);
```

> ⚠️ **SingleConnectionDataSource 是单线程模型**——多线程并发 getConnection 拿到同一个物理连接会互相干扰（游标/事务状态串台）；它只适合"单线程顺序测试"，并发测试必须用池或每次新建。

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

### 3.1 HikariCP 参数深度解读

| 参数 | 默认 | 含义与建议 |
|------|------|-----------|
| `maximumPoolSize` | 10 | 池上限：`(核心数 × 2) + 有效磁盘数` 起步，压测校准 |
| `minimumIdle` | 与 max 相同 | 最小空闲；高频短查询建议 min < max（省资源） |
| `connectionTimeout` | 30000 | 借连接最大等待；线上建议 3000-5000ms 快速失败 |
| `idleTimeout` | 600000 | 空闲回收（min < max 时才有意义） |
| `maxLifetime` | 1800000 | **必须 < 数据库 wait_timeout**（避免用到被服务端关闭的连接） |
| `validationTimeout` | 5000 | 连接有效性校验超时 |
| `leakDetectionThreshold` | 0（关） | 开启后超时未归还打印 WARN 日志——**泄漏排查神器** |
| `poolName` | 自动 | 命名后日志可区分多池 |

```yaml
spring:
  datasource:
    hikari:
      pool-name: AppPool
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 3000
      max-lifetime: 1800000
      leak-detection-threshold: 60000   # 60s 未归还 → 打印泄漏堆栈
```

> 💡 **面试常问：池大小怎么定？**——不是越大越好：连接是数据库端资源，过大反而增加上下文切换与锁竞争。经验公式 `池 ≈ 峰值并发 × (单查询耗时 / 平均空闲等待)` 起步、压测修正；多数 Web 应用 10-30 足够。

### 3.2 Druid 特有能力

| 能力 | 配置 | 说明 |
|------|------|------|
| SQL 监控 | `stat-view-servlet.enabled: true` | Web 页面看慢 SQL / 连接分布 |
| 慢 SQL 拦截 | `slowSqlMillis` | 超时自动记录并告警 |
| SQL 防火墙 | `wall` 过滤器 | 拦截注入 / 危险语句 |
| 连接泄漏回收 | `removeAbandoned=true` | 超时强制回收（双刃剑：可能误杀长事务） |

```yaml
# Druid 关键配置（需 druid-spring-boot-starter）
spring:
  datasource:
    druid:
      initial-size: 5
      min-idle: 5
      max-active: 20
      max-wait: 3000
      remove-abandoned: true
      remove-abandoned-timeout: 300
      stat-view-servlet:
        enabled: true
```

> ⚠️ Druid 的 `removeAbandoned` 慎开：它按"借出时间"强回收，长事务 / 慢查询会被误杀，引起诡异的数据问题；优先用监控定位真正的泄漏点，而不是靠强回收兜底。

### 3.3 连接池高频坑汇总

| 坑 | 现象 | 根因与解法 |
|----|------|-----------|
| 池参数改了没生效 | 配置后仍 10 连接 | 多数据源时改错对象；检查 `spring.datasource.hikari.*` 前缀 |
| maxLifetime 大于 wait_timeout | 偶发"连接已关闭"异常 | maxLifetime 必须小于数据库 wait_timeout（MySQL 8h → 设 30min） |
| 启动报 Failed to obtain JDBC Connection | 密码 / 网络 / 防火墙 | 先手工连库验证，排除应用层因素 |
| 池耗尽但无泄漏 | 线程池默认 200 并发 | 并发线程数 > 池大小必然排队；先算并发再调池 |
| Druid removeAbandoned 误杀 | 长事务中途被回收 | 关闭 removeAbandoned，用监控定位真实泄漏 |

> ⚠️ 核心方法论：连接池问题先分"资源不足"与"资源泄漏"两类——资源不足（并发 > 池）是设计问题，资源泄漏（借了不还）是代码问题；先用 `leakDetectionThreshold` 排除泄漏，再谈调参，顺序反了会越调越乱。

补充一个排查顺序的经验：先看 `HikariPool-1 - Connection is not available` 的等待时间——若等待发生在高峰并发期且随后恢复，多半是资源不足；若持续不可用且日志伴随 `SQLException: Connection is closed`，多半是连接泄漏或 maxLifetime 配置问题。把"什么时候开始的、持续多久、是否恢复"三个时间点记下来，比看堆栈更快定位。

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

### 4.1 DataSourceUtils 源码级细节

```text
DataSourceUtils.getConnection(DataSource) 内部逻辑：
  1. 查 TransactionSynchronizationManager.getResource(dataSource)
     → 命中：返回事务绑定连接（使用计数 +1）
     → 未命中：dataSource.getConnection()（真实借出）
  2. 若当前线程处于事务同步中，把新连接注册为事务资源
     （此后 releaseConnection 不会真正关闭它）

DataSourceUtils.releaseConnection(Connection, DataSource)：
  → 若是事务绑定连接：仅递减计数，不 close
  → 否则：conn.close()（归还池）
```

| 关键点 | 说明 |
|--------|------|
| 事务绑定的载体 | `TransactionSynchronizationManager`（线程本地 Map） |
| 引用计数 | 同一连接多次 getConnection 时计数递增，全部释放才归还 |
| 中途手动 close 的后果 | 事务后续语句报"连接已关闭"——事务内禁止手动 close |
| 与连接池的关系 | DataSourceUtils 不知道"池"存在，只对 DataSource 接口编程 |

> 🎯 **面试连环问**：为什么 JdbcTemplate 内部从不手动 close 连接？→ 因为它全部走 `DataSourceUtils.releaseConnection`——事务中"假释放"、无事务时"真归还"；这就是模板代码里永远见不到 `conn.close()` 的原因。

### 4.2 面试官追问什么

| 追问 | 回答锚点 |
|------|---------|
| 事务中 getConnection 会拿新连接吗 | 不会——DataSourceUtils 先查线程绑定，命中即复用 |
| 没有事务时连接何时归还 | releaseConnection 直接 close（归还池），模板内 finally 保证 |
| 为什么说"事务 = 连接" | 事务状态（autoCommit / 隔离级别）是连接级的属性 |
| 手工 new Connection 会怎样 | 脱离事务绑定——事务内更新看不到它写的未提交数据，还可能泄漏 |
| DataSourceUtils 与连接池谁管连接 | DataSourceUtils 管"借还时机"，连接池管"物理连接复用" |

> 🎯 面试金句："Spring 把连接的生命周期从'业务代码手工管理'提升到'框架按事务语义管理'——JdbcTemplate 无需 close 是因为框架承诺了释放时机，这个承诺的载体就是 DataSourceUtils 与事务同步机制。"

最后强调：DataSourceUtils 的绑定是"按线程"的——虚拟线程（JDK 21+）环境下每个虚拟线程独立绑定，同一任务并发执行互不干扰；Spring 事务绑定在虚拟线程里依然安全，这也是 Spring 6.1+ 官方宣称虚拟线程友好的底层原因之一。

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

### 5.1 自定义 DataSource 与集成测试技巧

```java
// 自定义包装：打印每次连接获取耗时（排查泄漏/慢获取）
@Bean
public DataSource loggingDataSource(DataSource target) {
    return new DelegatingDataSource(target) {
        @Override
        public Connection getConnection() throws SQLException {
            long start = System.nanoTime();
            Connection c = super.getConnection();
            System.out.println("getConnection by " + Thread.currentThread().getName()
                    + " took " + (System.nanoTime() - start) / 1_000_000 + "ms");
            return c;
        }
    };
}
```

| 测试技巧 | 做法 |
|---------|------|
| 集成测试隔离 | `@TestPropertySource` 指向 H2 内存库，零外部依赖 |
| 脚本初始化 | `EmbeddedDatabaseBuilder.addScript("classpath:test-schema.sql")` |
| 事务回滚 | `@Transactional` 测试方法自动回滚（见 [07-与事务ORM集成速查](07-与事务ORM集成速查.md)） |
| 断言 SQL 与参数 | 日志开 DEBUG：`logging.level.org.springframework.jdbc.core=DEBUG` |
| 池耗尽演练 | 调小 `maximumPoolSize=2` + 并发线程，观察 `request timed out` 行为 |

> 💡 **经验**：本地 / CI 用 H2 + 测试脚本，线上用真实库——H2 的 MySQL 兼容模式（`;MODE=MySQL`）能覆盖 90% 语法，但窗口函数 / 锁行为差异仍需压测环境兜底。

### 5.2 代理数据源的适用边界

| 代理 | 何时需要 | 何时不需要 |
|------|---------|-----------|
| TransactionAwareDataSourceProxy | 第三方工具直接调 `getConnection()` | JPA / MyBatis（自身走事务同步） |
| LazyConnectionDataSourceProxy | 启动时不想连库（健康检查 / 多环境） | 常规单库应用 |

使用注意：代理是"包装"关系——`TransactionAwareDataSourceProxy` 包住真实数据源后，所有 JdbcTemplate 与第三方代码必须注入**代理**，否则事务绑定还是对不上；换代理等于换 Bean，注意检查注入点是否一致。另外，代理叠加的顺序有讲究：先事务代理、再延迟代理（外层延迟、内层事务），顺序反了会导致延迟建连失效。

还有一个组合场景：读写分离架构下（见 [07-与事务ORM集成速查](07-与事务ORM集成速查.md) 第 5 节），`LazyConnectionDataSourceProxy` 常与 `AbstractRoutingDataSource` 搭配——延迟到真正执行 SQL 时才路由，避免"路由了但没执行"造成空连接；这个组合是"启动提速 + 路由正确"的标准答案。

---

**下一模块**：[05-批量操作与存储过程速查](05-批量操作与存储过程速查.md)　**返回总览**：[00-Spring JDBC组件总览](00-Spring JDBC组件总览.md)
