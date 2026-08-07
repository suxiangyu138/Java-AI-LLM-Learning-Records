# 00 Spring TX 组件总览

> 组件卡片：spring-tx 是什么、版本现状、能做什么、与深度体系如何衔接——声明式事务的"合同层"

---

## 📚 目录

1. [组件一句话定位](#1-组件一句话定位)
2. [版本现状（2026-08）](#2-版本现状2026-08)
3. [能力地图](#3-能力地图)
4. [与深度体系的映射](#4-与深度体系的映射)
5. [快速上手 3 步](#5-快速上手-3-步)
6. [速查导航](#6-速查导航)

---

## 1. 组件一句话定位

**spring-tx 是 Spring 事务能力的"合同层"**——定义事务抽象（PlatformTransactionManager/TransactionDefinition）、异常体系（org.springframework.dao.DataAccessException）、声明式入口（@Transactional）与编程式入口（TransactionTemplate）；**它不执行任何 SQL，却让一切数据访问（JDBC/JPA/MyBatis）共享同一套事务语义**。

```text
核心心智模型：
  事务三问：开启？提交？回滚？
    ↓
  PlatformTransactionManager（统一契约，只管三件事）
    ├── getTransaction(definition)  → 开启/加入事务（按传播行为）
    ├── commit(status)              → 提交
    └── rollback(status)            → 回滚
    ↑ 各资源提供实现：
  DataSourceTransactionManager（JDBC）
  JpaTransactionManager（JPA/Hibernate）
  JtaTransactionManager（XA 分布式）

  使用层：@Transactional（声明式，AOP 织入）｜ TransactionTemplate（编程式）
```

| 维度 | 说明 |
|------|------|
| 所属 | Spring Framework 底层核心模块（spring-tx） |
| 版本线 | 随 Spring Framework 7.x（2025-11 起） |
| 语言要求 | Java 17+ |
| 定位 | 事务抽象与契约（数据访问公共层，与 spring-jdbc 互补） |

## 2. 版本现状（2026-08）

| 版本 | 说明 |
|------|------|
| Spring Framework 7.0（2025-11） | 当前主线（Boot 4.0 配套） |
| Spring Framework 7.1（2026-05） | 最新维护线（Boot 4.1 配套） |
| Spring Framework 6.2.x | 存量主线（Boot 3.x 配套） |

**7.x tx 关键变化：**

- **核心 API 稳定**：`PlatformTransactionManager`/`@Transactional`/`TransactionTemplate` 三件套在 7.0 无破坏性变更——事务语义（传播/隔离/回滚规则）完全延续 6.x；
- **CGLIB 全局代理默认**（与 Boot 对齐）：`@Transactional` 默认走 CGLIB——依赖 JDK 接口代理的存量代码用 `@Proxyable(INTERFACES)` 按 Bean 恢复（见 [Spring-Context-03](../Spring‑Context/03-配置类与扫描机制速查.md)）；
- **ORM 迁移**：Hibernate 原生支持随 JPA 3.2/Hibernate 7 迁到 `org.springframework.orm.jpa.hibernate`（`HibernateTransactionManager` 同址，spring-orm 模块）；
- **异常体系延续**：`org.springframework.dao.DataAccessException` 家族稳定，新增类型随需求少量演进。

> ⚠️ **要点**：spring-tx 是 Spring 中最"稳定"的模块之一——7.0 的升级注意点不在 tx 本身，而在**代理默认值**（CGLIB）与 **ORM 包迁移**；事务代码本身可原样保留。

## 3. 能力地图

| 能力域 | 能力点 | 对应注解/API |
|--------|--------|-------------|
| 抽象契约 | 事务管理器统一接口 | `PlatformTransactionManager`、`TransactionDefinition` |
| 声明式 | 注解事务 | `@Transactional`、`@EnableTransactionManagement` |
| 传播行为 | 7 种传播 | REQUIRED / REQUIRES_NEW / NESTED / SUPPORTS / MANDATORY / NOT_SUPPORTED / NEVER |
| 隔离级别 | 5 种隔离 | `TransactionDefinition.ISOLATION_*`（读已提交/可重复读等） |
| 回滚规则 | 精准回滚控制 | `rollbackFor` / `noRollbackFor` / `rollbackForClassName` |
| 编程式 | 命令式模板 | `TransactionTemplate`、`TransactionOperations` |
| 响应式 | 响应式事务 | `TransactionalOperator`（ReactiveTransactionManager） |
| 异常体系 | 统一数据异常 | `DataAccessException` 家族（org.springframework.dao） |
| 实现家族 | 各资源管理器 | DataSource/JPA/JTA/Reactive 实现 |

## 4. 与深度体系的映射

| 速查文档 | 深度体系模块 |
|---------|-------------|
| 02-事务管理器速查 | [Spring框架核心-06-声明式事务管理](../../../Spring框架核心/06-声明式事务管理.md) |
| 03-@Transactional 速查 | [Spring框架核心-06](../../../Spring框架核心/06-声明式事务管理.md) |
| 04-传播行为速查 | [Spring全家桶-03-Spring-事务管理深度剖析](../../../Spring全家桶/03-Spring-事务管理深度剖析.md) |
| 06-编程式事务速查 | [Spring框架核心-06](../../../Spring框架核心/06-声明式事务管理.md) |
| 08-集成地图与常见问题 | [Spring生态深度剖析-04-AOP代理创建与事务管理内核](../../../Spring生态深度剖析/04-AOP代理创建与事务管理内核.md) |

> 💡 本系列定位"查得快"，深挖事务传播源码、AOP 拦截链、回滚判定逻辑见 [Spring生态深度剖析-04](../../../Spring生态深度剖析/04-AOP代理创建与事务管理内核.md)；分布式事务（Seata）见 [Spring全家桶-14](../../../Spring全家桶/14-分布式事务-Seata.md)。

## 5. 快速上手 3 步

**① 引入依赖**（Boot 4：starter-jdbc 已含 spring-tx）：

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-tx</artifactId>
    <version>7.0.6</version>
</dependency>
<!-- Boot：spring-boot-starter-jdbc / -data-jpa 传递引入 -->
```

**② 开启 + 使用声明式事务**：

```java
@Configuration
@EnableTransactionManagement          // 开启 @Transactional 代理（Boot 4 默认开启）
public class TxConfig { }

@Service
public class OrderService {
    @Transactional(rollbackFor = Exception.class)   // 受检异常也回滚
    public void createOrder(OrderCmd cmd) {
        orderDao.insert(cmd);                       // 第一条 SQL
        accountDao.debit(cmd.userId(), cmd.amount()); // 第二条 SQL（同事务）
        // 任一失败 → 整体回滚
    }
}
```

**③ 编程式兜底**（无法用注解的场景）：

```java
TransactionTemplate tt = new TransactionTemplate(txManager);
tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
tt.executeWithoutResult(status -> {
    // 显式事务块（自调用/工具类/循环事务场景）
});
```

## 6. 速查导航

| 文档 | 内容 |
|------|------|
| [01-模块清单](01-模块清单.md) | spring-tx artifact 与包结构、依赖边界 |
| [02-事务管理器速查](02-事务管理器速查.md) | PlatformTransactionManager 族与各资源实现 |
| [03-@Transactional 速查](03-@Transactional速查.md) | 注解属性全表、声明式事务细节 |
| [04-传播行为速查](04-传播行为速查.md) | 7 种传播语义与场景速记 |
| [05-隔离级别与锁速查](05-隔离级别与锁速查.md) | 5 级隔离、并发问题、锁 |
| [06-编程式事务速查](06-编程式事务速查.md) | TransactionTemplate/TransactionalOperator |
| [07-回滚机制与测试速查](07-回滚机制与测试速查.md) | 回滚规则、事务测试、保存点 |
| [08-集成地图与常见问题](08-集成地图与常见问题.md) | 与 JDBC/ORM/分布式联动 + 高频坑 |

---

**下一模块**：[01-模块清单](01-模块清单.md)　**返回总览**：本页

**【参考来源】**：[Spring Framework 7.0 Release Notes（GitHub Wiki）](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-7.0-Release-Notes)、[Spring Framework 7.0 正式发布解析（掘金）](https://juejin.cn/post/7573592127298682930)、spring-tx 7.0.0 依赖注册页（tessl.io）、[Spring 6→7 Migration Guide（dev.to）](https://dev.to/ankurm/spring-framework-6-to-7-migration-guide-breaking-changes-deprecated-apis-and-upgrade-checklist-3bf6)
