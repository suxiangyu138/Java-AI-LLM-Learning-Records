# 07 与事务/ORM 集成速查

> DataSourceTransactionManager、@Transactional 与 JDBC 的关系、JPA/MyBatis 底层如何接轨——"数据访问全家桶"的拼图

---

## 📚 目录

1. [事务三件套与 JDBC 的关系](#1-事务三件套与-jdbc-的关系)
2. [DataSourceTransactionManager 原理](#2-datasourcetransactionmanager-原理)
3. [@Transactional 与 JdbcTemplate 的天然配合](#3-transactional-与-jdbctemplate-的天然配合)
4. [ORM 底层接轨：JPA / MyBatis](#4-orm-底层接轨jpa--mybatis)
5. [多数据源与事务边界](#5-多数据源与事务边界)

---

## 1. 事务三件套与 JDBC 的关系

```text
事务能力分层（spring-tx 定义，spring-jdbc 提供 JDBC 实现）：
  ① TransactionManager（接口）→ DataSourceTransactionManager（jdbc 实现）
  ② TransactionDefinition（传播/隔离/超时定义）
  ③ TransactionTemplate / @Transactional（编程式 / 声明式入口）
        ↓
  核心机制：TransactionSynchronizationManager 绑定"线程 → 事务资源"
      事务开启 → DataSourceUtils.getConnection 返回绑定连接（同一物理连接）
      → 所有 JdbcTemplate 操作自动在同一事务中
```

> 🎯 **核心要点**：spring-jdbc 与 spring-tx 的接缝就是 **DataSourceTransactionManager + DataSourceUtils 的线程绑定**——只要走模板（JdbcTemplate/JdbcClient），事务开不开都由绑定连接决定，业务代码零感知。

## 2. DataSourceTransactionManager 原理

| 步骤 | 行为 |
|------|------|
| 开启 | `getConnection()` 后 `setAutoCommit(false)` + `ConnectionHolder` 绑定线程 |
| 提交 | `connection.commit()` + 解除绑定 + 归还池 |
| 回滚 | `connection.rollback()`（基于保存点可部分回滚，见传播 NESTED） |
| 嵌套 | REQUIRES_NEW → 借第二条物理连接（池需足够） |

```java
// 显式装配（Boot 4 自动配置，仅多数据源/自定义池时需要手动）
@Bean
public DataSourceTransactionManager transactionManager(DataSource dataSource) {
    return new DataSourceTransactionManager(dataSource);
}
```

> ⚠️ **单事务=单连接**：REQUIRED 传播下整个事务一条物理连接；`REQUIRES_NEW` 挂起外层、**另取一条连接**——连接池 `maximumPoolSize` 必须容纳"并发事务数 × 嵌套层数"，否则池耗尽（见 [04-DataSource 与连接池速查](04-DataSource与连接池速查.md)）。

## 3. @Transactional 与 JdbcTemplate 的天然配合

```java
@Service
public class OrderService {
    private final JdbcClient jdbc;

    @Transactional                       // 声明式事务（默认：回滚 RuntimeException/Error）
    public void createOrder(OrderCmd cmd) {
        jdbc.sql("INSERT INTO t_order(user_id, amount, status) VALUES (?, ?, ?)")
                .params(cmd.userId(), cmd.amount(), "CREATED").update();

        jdbc.sql("UPDATE t_account SET balance = balance - ? WHERE user_id = ?")
                .params(cmd.amount(), cmd.userId()).update();   // 同事务第二条语句

        if (cmd.amount().signum() <= 0) {
            throw new IllegalArgumentException("amount invalid");  // → 全部回滚
        }
    }
}
```

| 配合点 | 说明 |
|--------|------|
| 同一事务 | 两条 update 走同一绑定连接，要么都成功要么都回滚 |
| 异常回滚 | 默认 RuntimeException/Error；受检异常不回滚（`rollbackFor` 显式声明） |
| 代理限制 | 同类自调用 `this.createOrder` 绕过代理 → 事务失效（见 [Spring-AOP-00](../Spring‑AOP/00-Spring AOP组件总览.md)） |
| 7.0 注意 | 全局 CGLIB 代理默认——JDK 接口代理依赖的存量代码行为变化，`@Proxyable(INTERFACES)` 按 Bean 恢复 |

> 💡 **测试配合**：`@SpringBootTest + @Transactional`（测试方法事务自动回滚）是 DAO 集成测试标准姿势——每方法独立事务、测完回滚，数据零残留。

## 4. ORM 底层接轨：JPA / MyBatis

| ORM | 与 spring-jdbc 的关系 | 事务管理器 |
|-----|----------------------|-----------|
| Spring Data JPA | 不直接用 JdbcTemplate（Hibernate 自己执行 SQL） | `JpaTransactionManager` |
| MyBatis | **不依赖** JdbcTemplate（自有 SqlSession），但复用 DataSource 与异常翻译 | `DataSourceTransactionManager` |
| 裸 JdbcTemplate | 直接 | `DataSourceTransactionManager` |
| Spring Data JDBC | 基于 JdbcTemplate 之上的轻量仓储 | `DataSourceTransactionManager` |

```text
多 ORM 同事务的接缝：
  所有 ORM 都走 DataSourceUtils/TransactionSynchronizationManager 的
  "线程 → 连接"绑定 —— 只要共享同一个 DataSource，
  JdbcTemplate 与 MyBatis/JPA 在同一事务内混合使用是安全的（同一物理连接）。
  例外：JPA 使用"持久化上下文"（EntityManager），与 JDBC 混用需注意缓存一致性。
```

> 🎯 **核心要点**：MyBatis-Spring 与 Spring Data JDBC **都构建在 spring-jdbc 的 DataSource 管理之上**；JPA 相对独立但事务资源仍由 Spring 协调——"多 ORM 混用"的底线是**共享 DataSource**。

## 5. 多数据源与事务边界

| 场景 | 方案 | 局限 |
|------|------|------|
| 多数据源读写分离 | `@DataSourceRouter`/AbstractRoutingDataSource + 手动切换 | 事务内切换需注意连接已绑定 |
| 跨库事务 | `JtaTransactionManager`（XA，需事务中间件） | 性能与复杂度高 |
| 分布式事务 | Seata（AT/TCC）/ 本地消息表 | 见 [Spring全家桶-14-分布式事务-Seata](../../../Spring全家桶/14-分布式事务-Seata.md) |
| 跨库"伪事务" | 业务补偿 + 最终一致 | 高可用优先场景 |

> ⚠️ **多数据源头号坑**：事务开启时连接已被绑定到第一个 DataSource——中途切到第二个库的操作**不在事务内**（且可能抛 "Cannot switch DataSource in transaction"）；多数据源 + 事务 = 先设计边界（独立事务 / 分布式事务 / 最终一致）。

---

**下一模块**：[08-集成地图与常见问题](08-集成地图与常见问题.md)　**返回总览**：[00-Spring JDBC组件总览](00-Spring JDBC组件总览.md)
