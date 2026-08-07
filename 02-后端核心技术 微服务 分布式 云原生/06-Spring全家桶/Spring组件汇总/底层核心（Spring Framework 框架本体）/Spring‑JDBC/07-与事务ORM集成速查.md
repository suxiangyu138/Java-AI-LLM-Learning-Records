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

### 1.1 编程式事务：TransactionTemplate

声明式 `@Transactional` 之外，Spring 提供编程式入口——适合"事务边界不固定 / 动态分组"的场景：

```java
@Service
public class BatchService {
    private final TransactionTemplate txTemplate;      // Boot 自动装配可注入

    // 每条记录独立事务：一条失败不影响其他
    public void processEach(List<Long> ids) {
        for (Long id : ids) {
            txTemplate.executeWithoutResult(status -> {
                try {
                    updateStock(id);
                } catch (Exception e) {
                    status.setRollbackOnly();          // 显式标记回滚
                }
            });
        }
    }
}
```

| 对比 | @Transactional | TransactionTemplate |
|------|---------------|-------------------|
| 声明方式 | 注解（编译期固定） | 代码（运行时动态） |
| 事务边界 | 方法级 | 任意代码块 |
| 回滚控制 | rollbackFor 注解 | `status.setRollbackOnly()` |
| 单元测试 | 需 Spring 容器 / 代理 | 可直接 new + mock |
| 适用 | 99% 的常规服务方法 | 批处理逐条、动态分组、工具类 |

> 💡 面试点：TransactionTemplate 内部就是 `TransactionManager.getTransaction + commit/rollback` 的模板化封装——它和 @Transactional 走同一条事务资源绑定机制，混用安全。

### 1.2 TransactionDefinition 关键属性速查

| 属性 | 取值 | 说明 |
|------|------|------|
| propagation | REQUIRED（默认）/ REQUIRES_NEW / NESTED / MANDATORY 等 | 传播行为 |
| isolation | DEFAULT（用数据库默认） | 隔离级别 |
| timeout | -1（无超时） | 事务超时秒数，超时抛事务超时异常 |
| readOnly | false | 只读优化提示 |
| rollbackFor | 默认运行时异常 | 回滚规则 |

```java
// 编程式定义（等价注解写法）
DefaultTransactionDefinition def = new DefaultTransactionDefinition();
def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
def.setTimeout(10);
```

> 💡 面试常问"readOnly=true 到底做了什么"：MySQL / InnoDB 下它主要是**优化提示**（允许引擎跳过部分写准备），不强制只读；PostgreSQL 下会真正禁止写。答出"不同库行为不同"是加分项，别一刀切。

补充：timeout 与数据库层的配合——事务超时由 Spring 计时并触发回滚，但数据库端的长事务可能已经执行了大量工作；实践上"事务超时 + 语句超时（queryTimeout）双保险"才是完整防护。面试延伸：传播行为与超时是正交的，REQUIRES_NEW 的内层事务有自己独立的超时计时。

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

### 2.1 回滚规则与保存点

| 规则 | 行为 |
|------|------|
| 默认回滚 | RuntimeException 与 Error（含其子类） |
| 不回滚 | 受检异常（Exception 子类）——除非 `rollbackFor` |
| 反向排除 | `noRollbackFor` 指定不回滚的异常 |
| 精确匹配 | `rollbackFor = BizException.class`（含子类）；`rollbackForClassName` 按类名 |
| 保存点 | NESTED 用数据库保存点：外层回滚则内层必回滚，内层回滚不影响外层 |

```java
@Transactional(rollbackFor = { BizException.class, DataAccessException.class })
public void createOrder(OrderCmd cmd) {
    // ...
    // 受检异常也回滚（默认不回滚受检异常——新手第一大坑）
    throw new BizException("业务规则不满足");
}
```

> ⚠️ **受检异常不回滚是新手第一大坑**：默认只回滚运行时异常——捕获了 Exception 再抛出去的代码，若抛的是受检异常，事务会"正常提交"；规则：**异常要抛出代理边界才生效，且类型必须被 rollbackFor 覆盖**。

### 2.2 隔离级别与锁（与 JDBC 的关系）

| 隔离级别 | 脏读 | 不可重复读 | 幻读 | JDBC 常量 |
|---------|------|-----------|------|-----------|
| READ_UNCOMMITTED | 可能 | 可能 | 可能 | TRANSACTION_READ_UNCOMMITTED |
| READ_COMMITTED | 无 | 可能 | 可能 | TRANSACTION_READ_COMMITTED |
| REPEATABLE_READ（MySQL 默认） | 无 | 无 | 无（MVCC） | TRANSACTION_REPEATABLE_READ |
| SERIALIZABLE | 无 | 无 | 无 | TRANSACTION_SERIALIZABLE |

```java
@Transactional(isolation = Isolation.REPEATABLE_READ)
public void readConsistent() { ... }
```

> 💡 注意：隔离级别是**连接级**属性——DataSourceTransactionManager 开启事务时对绑定连接执行 `setTransactionIsolation`；所以"同一个事务内多次查询"的一致性由隔离级别保证，这解释了"事务内查不到刚插入数据"的隔离级别场景（见 [08-集成地图与常见问题](08-集成地图与常见问题.md) 高频问题表）。

补充：保存点（Savepoint）的底层行为——NESTED 传播在支持保存点的数据库（MySQL / PostgreSQL）上，内层回滚只回滚到保存点，外层仍可继续提交；在不支持保存点的数据库上，Spring 会退化为"同事务"行为（内层回滚即整体回滚）。所以 NESTED 的"部分回滚"语义是**有条件的**，依赖数据库能力——面试被问"NESTED 和 REQUIRES_NEW 区别"时，先答保存点机制，再答数据库差异，完整度立刻拉开。

再补一个锁的面试点：悲观锁（`SELECT ... FOR UPDATE`）在事务内获取，锁释放随提交/回滚——所以"查询加锁"必须与"修改"在同一个事务里，否则锁白加；Spring 事务边界就是锁的边界，这是"为什么 @Transactional 方法内 FOR UPDATE 才有效"的答案。

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

### 3.1 事务失效场景大全（排查清单）

| 场景 | 原因 | 解法 |
|------|------|------|
| 同类自调用 `this.method()` | 绕过代理，注解不生效 | 注入自身代理 / 拆 Service / 用 TransactionTemplate |
| 方法非 public | 代理只拦 public 方法 | 改 public |
| 类未进 Spring 容器 | `new` 出来的对象无代理 | 交给容器管理 |
| 异常被 catch 吞掉 | 代理边界看不到异常 | 让异常抛出，或 `setRollbackOnly()` 手动标记 |
| 异常类型不受 rollbackFor 覆盖 | 受检异常默认不回滚 | `rollbackFor = Exception.class` 或业务异常继承 RuntimeException |
| 多线程调用 | 新线程不携带事务绑定 | 事务边界保持单线程；并行任务各自事务 |
| 绕过模板取连接 | 裸 `Connection` 不走 DataSourceUtils | 统一走 JdbcTemplate / JdbcClient |
| 事务内切数据源 | 连接已绑定第一个库 | 见第 5 节多数据源边界 |

> 🎯 **排查口诀**："自调用、非 public、吞异常、绕模板、跨线程"——事务没生效先从这五条里找。

### 3.2 面试官追问什么

| 追问 | 回答锚点 |
|------|---------|
| @Transactional 加在私有方法上生效吗 | 不生效——代理只拦 public |
| 同类方法互相调用为什么失效 | 走的是 this 引用，没有经过代理 |
| 事务方法里开子线程执行 SQL | 子线程无事务绑定——资源不随线程迁移 |
| 传播 REQUIRES_NEW 会怎样 | 挂起外层事务、另取连接——两条连接、两个独立事务 |
| 编程式与声明式能混用吗 | 能——同一事务管理器、同一绑定机制 |

> 🎯 面试主线："代理是事务的载体、绑定是事务的机制、连接是事务的容器"——代理（AOP 拦截）、绑定（TransactionSynchronizationManager）、连接（DataSourceUtils）三者串起来，事务问题基本都能答；这条主线也是从"速查"通向 [Spring-TX 系列](../Spring‑TX（Spring‑Transaction）/00-Spring TX组件总览.md) 的桥。

延伸：面试常问"@Transactional 与 AOP 的关系"——@Transactional 是 Spring AOP 的声明式事务实现，底层是 TransactionInterceptor 环绕通知；7.0 全局 CGLIB 代理的变更影响所有 AOP 注解，@Proxyable 可以按 Bean 恢复接口代理（见 [Spring-AOP-00](../Spring‑AOP/00-Spring AOP组件总览.md)）。

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

### 4.1 混用 JdbcTemplate 与 MyBatis/JPA 的注意点

| 组合 | 事务一致性 | 注意点 |
|------|-----------|--------|
| JdbcTemplate + MyBatis（同 DataSource） | ✅ 同一物理连接 | 跨事务注意 MyBatis 二级缓存可能读到旧数据 |
| JdbcTemplate + JPA（同 DataSource） | ✅ 同一物理连接 | JPA 一级缓存（EntityManager）与 JDBC 直查结果可能不一致 |
| 任一组合（不同 DataSource） | ❌ 各自独立 | 必须设计跨库方案（见第 5 节） |

```java
// 典型混用场景：JPA 管领域实体，JdbcClient 做批量/报表
@Service
public class MixedService {
    private final OrderRepository repo;       // JPA
    private final JdbcClient jdbc;            // JDBC

    @Transactional
    public void closeBatch(List<Long> ids) {
        repo.markClosed(ids);                 // JPA 更新（EntityManager 缓存同步）
        jdbc.sql("UPDATE t_order SET closed_at = NOW() WHERE id IN (:ids)")
                .param("ids", ids)
                .update();                    // 同一事务同连接执行
        // 再用 repo 查同一条记录时，EntityManager 可能返回缓存旧值 → 手动 refresh
    }
}
```

> 💡 底线原则：**同一事务内共享同一个 DataSource**——这是多 ORM 混用的前提；JPA 的缓存一致性问题用 `entityManager.refresh(entity)` 或减少混用边界（谁写谁查）。

补充：JPA 与 JDBC 混用还有一个隐藏成本——JPA 的 flush 时机（默认事务提交前 flush）意味着同一事务内 JdbcTemplate 先写、JPA 后读时可能读到旧数据；解决方式是 `entityManager.flush()` 主动同步，或约定"一种技术写、另一种只读"的职责边界。

## 5. 多数据源与事务边界

| 场景 | 方案 | 局限 |
|------|------|------|
| 多数据源读写分离 | `@DataSourceRouter`/AbstractRoutingDataSource + 手动切换 | 事务内切换需注意连接已绑定 |
| 跨库事务 | `JtaTransactionManager`（XA，需事务中间件） | 性能与复杂度高 |
| 分布式事务 | Seata（AT/TCC）/ 本地消息表 | 见 [Spring全家桶-14-分布式事务-Seata](../../../Spring全家桶/14-分布式事务-Seata.md) |
| 跨库"伪事务" | 业务补偿 + 最终一致 | 高可用优先场景 |

> ⚠️ **多数据源头号坑**：事务开启时连接已被绑定到第一个 DataSource——中途切到第二个库的操作**不在事务内**（且可能抛 "Cannot switch DataSource in transaction"）；多数据源 + 事务 = 先设计边界（独立事务 / 分布式事务 / 最终一致）。

### 5.1 AbstractRoutingDataSource：读写分离的最小实现

```java
// 按上下文路由到不同数据源（写库 / 读库）
public class RoutingDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        return DbContextHolder.get();      // 如 "master" / "slave"
    }
}

@Bean
public DataSource routingDataSource(@Qualifier("master") DataSource master,
                                    @Qualifier("slave") DataSource slave) {
    RoutingDataSource routing = new RoutingDataSource();
    Map<Object, Object> targets = new HashMap<>();
    targets.put("master", master);
    targets.put("slave", slave);
    routing.setTargetDataSources(targets);
    routing.setDefaultTargetDataSource(master);
    return routing;
}
```

| 面试追问 | 回答锚点 |
|---------|---------|
| 多数据源 + 事务怎么设计 | 事务内禁止切换；写走主、读走从按方法路由；跨库强一致用分布式事务（Seata） |
| AbstractRoutingDataSource 是池吗 | 不是——按 key 委托到目标 DataSource（池在目标里） |
| 路由 key 怎么传递 | ThreadLocal（请求结束必须清理，防线程池复用串 key） |
| 主从延迟怎么办 | 读关键数据强制走主（路由 hint 或 `@Transactional(readOnly=false)`） |
| 什么时候该上分布式事务 | 多库强一致且无法用补偿 / 消息最终一致时——先评估业务能否接受最终一致 |

> 🎯 决策顺序：单库 → 读写分离（路由 + 延迟容忍）→ 分库分表（中间件）→ 分布式事务（最后手段）——每上一级复杂度翻倍，别为不存在的规模买单。

### 5.2 跨库事务方案对比（何时选什么）

| 方案 | 一致性 | 性能 | 复杂度 | 适用 |
|------|--------|------|--------|------|
| 本地事务 + 消息表 | 最终一致 | 高 | 中 | 业务可接受短暂不一致 |
| Seata AT 模式 | 最终一致（自动补偿） | 中 | 中高 | 多库多服务、Java 系 |
| Seata TCC | 最终一致（手动补偿） | 中 | 高 | 强隔离需求、资金类 |
| XA（JTA） | 强一致 | 低 | 高 | 短事务、小众场景 |
| 业务补偿 + 对账 | 最终一致 | 高 | 高 | 无法用事务的场景 |

> ⚠️ 选型提醒：先问"这个跨库强一致真的需要吗"——多数场景可以通过"单库聚合 + 最终一致"降低复杂度；XA 在 2026 年的生产占比已经很小（性能与协调器复杂度高），Seata 类方案才是主流；具体细节见 [Spring全家桶-14-分布式事务-Seata](../../../Spring全家桶/14-分布式事务-Seata.md)。

补充一个判断框架：什么时候"必须"分布式事务？——资金强一致、跨库唯一约束、跨服务数据依赖不可重试；什么时候"不该"用？——单次请求内多个独立操作（本可用本地事务）、可补偿业务（订单状态机）、读多写少（可用最终一致）。先做这个判断题，再谈方案选型，是面试的加分路径。

---

**下一模块**：[08-集成地图与常见问题](08-集成地图与常见问题.md)　**返回总览**：[00-Spring JDBC组件总览](00-Spring JDBC组件总览.md)
