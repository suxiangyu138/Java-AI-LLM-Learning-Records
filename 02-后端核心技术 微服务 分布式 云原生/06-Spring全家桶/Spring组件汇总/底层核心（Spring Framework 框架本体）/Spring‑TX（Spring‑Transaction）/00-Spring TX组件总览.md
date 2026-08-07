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
7. [学习路线推荐](#7-学习路线推荐)
8. [核心概念速查](#8-核心概念速查)

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

### 1.1 为什么叫"合同层"：依赖倒置的教科书

spring-tx 被称作"合同层"，根因是它的依赖方向与直觉相反——**不是 tx 依赖数据访问实现，而是所有数据访问模块反向依赖 tx**：

```text
业务代码（只面向契约编程，import org.springframework.transaction.* / org.springframework.dao.*）
   │
   ▼
spring-tx（合同层：管理器接口 + 异常族 + 拦截器 + 模板）
   ▲
spring-jdbc / spring-orm（实现层：DataSourceTransactionManager / JpaTransactionManager）
```

这个"倒挂"带来三个直接收益：

| 收益 | 说明 |
|------|------|
| 业务零改动换库 | 换数据库、换 ORM 只换实现依赖，业务事务代码原样保留 |
| 语义统一 | 所有资源共享同一套传播/隔离/回滚语义，团队心智一致 |
| 可替换 | 事务管理器本身是 Bean——测试可换假实现（如 `TestTransactionManager`） |

> 🎯 **一句话记忆**：tx 定规则（合同），jdbc/orm 交实现（履约），业务只签合同（面向接口编程）——这是"面向接口、依赖倒置"在数据访问层的标准答案。

### 1.2 事务三问与两种入口的对应

"事务三问"（开启？提交？回滚？）在 spring-tx 中有两套入口，殊途同归：

| 入口 | 开启时机 | 提交/回滚时机 | 失败语义 |
|------|---------|--------------|---------|
| `@Transactional`（声明式） | 代理拦截方法进入时 | 方法正常返回 / 异常按规则 | 异常类型决定回滚（默认运行时异常） |
| `TransactionTemplate`（编程式） | `execute` 调用时 | 回调正常返回 / 回调抛异常 | 运行时异常自动回滚并重新抛出 |

两者最终都调用同一个 `PlatformTransactionManager`——**引擎相同、方向盘不同**；理解这一点，就理解了"为什么编程式能解决声明式的自调用失效"（无代理依赖），也理解了"为什么混用安全"（同一线程同一事务资源）。

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

### 2.1 版本演进：tx 的"变"与"不变"

| 版本线 | 关键事件 | 对事务代码的影响 |
|--------|---------|----------------|
| Spring 4.x（2013） | 引入 `@EnableTransactionManagement`、Java 配置化 | 摆脱 XML `<tx:annotation-driven>` |
| Spring 5.x（2017） | 引入响应式事务 `TransactionalOperator` | 新增 reactive 包，阻塞 API 不变 |
| Spring 6.x（2022） | 事务抽象冻结；新增 `TransactionOperations` 接口 | 编程式事务面向接口成为官方推荐 |
| Spring 7.x（2025） | CGLIB 代理默认、ORM 包迁移 | 代理行为变化（见下），事务语义零变化 |

**7.x 升级核对清单（Boot 3 → Boot 4 迁移）：**

```text
□ 1. @Transactional 代码零改动（传播/隔离/回滚语义不变）
□ 2. 依赖接口代理的代码 → @Proxyable(INTERFACES) 或显式恢复
□ 3. final 类/方法上的 @Transactional → 去掉 final（CGLIB 不可代理）
□ 4. org.springframework.orm.hibernate5.* → org.springframework.orm.jpa.hibernate.*
□ 5. 自定义 TransactionInterceptor 子类 → 核对 API 签名
```

> 💡 升级结论一句话：**事务业务代码不需要动，代理配置要核对**——7.0 把"代理默认值"从 JDK 接口代理切换为 CGLIB，是升级中唯一可能与事务行为相关的破坏点。

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

### 3.1 能力边界：spring-tx 不做什么

| 能力边界 | 谁来做 | 说明 |
|---------|--------|------|
| 连接池管理 | HikariCP / Druid | tx 只"借用"连接，不创建、不回收池 |
| 分布式事务协议 | Seata / JTA / 本地消息表 | tx 的 JTA 实现只是桥接，本身不实现 XA 中间件 |
| SQL 生成与执行 | JdbcTemplate / MyBatis / JPA | tx 不碰任何 SQL |
| 数据库锁与 MVCC | 数据库引擎 | tx 只翻译隔离级别、超时等请求参数 |
| 读写分离 / 分库分表 | ShardingSphere / MyCat | tx 可配合，但不感知路由 |

**由此推出的使用原则：**

- 连接池不够用 → 排查池配置与事务时长，不是事务框架的问题；
- 分布式一致性 → 先评估业务是否真的需要强一致（见 [Spring全家桶-14](../../../Spring全家桶/14-分布式事务-Seata.md)）；
- 锁等待 / 死锁 → 数据库层现象，事务代码只负责"画对边界"。

> ⚠️ **常见归因错误**：把连接池耗尽、死锁、慢 SQL 归罪于"事务框架"——这些是资源层问题；spring-tx 能做的只是暴露正确的事务边界，**边界越小，资源占用越少**。

## 4. 与深度体系的映射

| 速查文档 | 深度体系模块 |
|---------|-------------|
| 02-事务管理器速查 | [Spring框架核心-06-声明式事务管理](../../../Spring框架核心/06-声明式事务管理.md) |
| 03-@Transactional 速查 | [Spring框架核心-06](../../../Spring框架核心/06-声明式事务管理.md) |
| 04-传播行为速查 | [Spring全家桶-03-Spring-事务管理深度剖析](../../../Spring全家桶/03-Spring-事务管理深度剖析.md) |
| 06-编程式事务速查 | [Spring框架核心-06](../../../Spring框架核心/06-声明式事务管理.md) |
| 08-集成地图与常见问题 | [Spring生态深度剖析-04-AOP代理创建与事务管理内核](../../../Spring生态深度剖析/04-AOP代理创建与事务管理内核.md) |

> 💡 本系列定位"查得快"，深挖事务传播源码、AOP 拦截链、回滚判定逻辑见 [Spring生态深度剖析-04](../../../Spring生态深度剖析/04-AOP代理创建与事务管理内核.md)；分布式事务（Seata）见 [Spring全家桶-14](../../../Spring全家桶/14-分布式事务-Seata.md)。

### 4.1 速查与深度的分工约定

| 需求类型 | 用哪份材料 | 例子 |
|---------|-----------|------|
| 忘记 API / 查属性 | 本系列速查页 | `rollbackFor` 怎么写、NESTED 什么语义 |
| 理解机制 | 深度体系文档 | rollbackOnly 链路、代理与拦截器源码 |
| 排查线上问题 | 本系列 08 篇问题表 + 深度篇 | UnexpectedRollbackException 怎么定位 |
| 面试系统性复习 | 05-面试冲刺体系 | 传播七种、回滚规则、隔离锁三连问 |
| 分布式事务 | Spring全家桶-14 | Seata AT / TCC / 本地消息表选型 |

> 🎯 **协作原则**：速查页回答"是什么、怎么用"，深度页回答"为什么、怎么实现"——面试被追问到源码层时，把 [Spring生态深度剖析-04](../../../Spring生态深度剖析/04-AOP代理创建与事务管理内核.md) 的调用链作为扩展阅读锚点即可闭环。

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

### 5.1 快速上手补充：XML 方式与生效验证

**XML 等价配置**（存量项目 / 框架代码常用）：

```xml
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:tx="http://www.springframework.org/schema/tx"
       xsi:schemaLocation="http://www.springframework.org/schema/tx
           https://www.springframework.org/schema/tx/spring-tx.xsd">

    <bean id="txManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
        <property name="dataSource" ref="dataSource"/>
    </bean>

    <tx:annotation-driven transaction-manager="txManager"/>
</beans>
```

**三步验证事务是否真的生效：**

| 步骤 | 做法 | 预期 |
|------|------|------|
| ① 看日志 | 开启 `org.springframework.transaction` DEBUG 日志 | 出现 `Getting transaction for ...` / `Completing transaction ...` |
| ② 故意抛异常 | 在事务方法中段抛 `RuntimeException` | 前段 INSERT 未落库（回滚） |
| ③ 断点看代理 | 调试时查看注入 Bean 的类型 | 是 CGLIB 代理类而非原始类型 |

> 💡 排障起点建议：**先看日志再看代码**——`Getting transaction for` 一行日志就能区分"没进事务"与"进了事务但没回滚"两类问题。

### 5.2 事务相关的日志与监控配置

**日志开启方式（application.yml）：**

```yaml
logging:
  level:
    org.springframework.transaction: DEBUG   # 事务开启/提交/回滚日志
    org.springframework.jdbc.datasource: DEBUG  # 连接获取/归还（排查连接泄漏）
```

**典型日志含义速查：**

| 日志行 | 含义 | 下一步 |
|--------|------|--------|
| `Getting transaction for [xxx]` | 事务已开启（拦截器已工作） | 无——正常 |
| `Completing transaction for [xxx]` | 提交路径完成 | 正常 |
| `Initiating transaction commit` | 正在提交 | 正常 |
| `Initiating transaction rollback` | 正在回滚 | 查异常类型与规则 |
| `UnexpectedRollbackException` | 内圈标记了回滚 | 查内圈异常是否被吞 |

> 🎯 **快速结论**：看到"Getting/Completing"成对出现且无 rollback 日志，事务边界就是对的——问题转移到异常语义与资源层；监控指标（提交/回滚计数、事务时长）可交给 Micrometer 的 `@Transactional` 打点。

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

### 6.1 阅读顺序建议

- **第一次接触**：03 → 04 → 07，先掌握"怎么用"三件套（注解、传播、回滚），再回头补 02 的原理；
- **排查具体问题**：按现象查 08 的高频问题表，命中后跳对应专题页；
- **准备面试**：04（传播）→ 07（回滚规则）→ 05（隔离与锁）是三大必考主题，02 的 rollbackOnly 机制是深挖加分项；
- **源码学习**：02 的 `AbstractPlatformTransactionManager` 模板 + 03 的 `TransactionAspectSupport` 环绕逻辑，两条线即可贯通全链路；
- **编程式需求**：自调用、循环逐条事务、动态传播——直接看 06 篇，无需绕道声明式。

每篇速查文档都保持"查得快"的定位——深度内容通过第 4 节的能力映射跳转到体系文档，不在速查页展开；想学 Seata 分布式事务、事务拦截器源码，直接走对应体系链接。

### 6.2 前置知识建议

| 前置主题 | 所在文档 | 与本系列的关系 |
|---------|---------|--------------|
| AOP 代理与环绕通知 | [Spring-AOP 系列](../Spring‑AOP/00-Spring AOP组件总览.md) | 声明式事务的织入机制 |
| JDBC 连接与 DataSource | [Spring-JDBC-07](../Spring‑JDBC/07-与事务ORM集成速查.md) | 连接绑定与归还 |
| Bean 生命周期 | [Spring-Context 系列](../Spring‑Context/00-Spring Context组件总览.md) | 代理初始化时序（失效场景根因） |
| 事件机制 | [Spring-Context-05](../Spring‑Context/05-事件驱动机制速查.md) | 事务事件（AFTER_COMMIT） |

> 💡 零基础顺序：AOP 概念 → JDBC 连接模型 → 本系列 03/04/07 → 源码深挖（02/03 的源码小节）——链条最短且每步都有落点。

## 7. 学习路线推荐

| 路线 | 适用 | 顺序 |
|------|------|------|
| 入门速查 | 会用事务 | 00 总览 → 03 @Transactional → 04 传播 → 07 回滚 |
| 项目实践 | 排查事务问题 | 02 管理器 → 04 传播 → 05 隔离锁 → 06 编程式 → 08 常见问题 |
| 面试冲刺 | 事务全考点 | 04 传播 → 07 回滚规则 → 05 隔离 → 08 考点清单 → [Spring全家桶-03](../../../Spring全家桶/03-Spring-事务管理深度剖析.md) |

## 8. 核心概念速查

| 术语 | 一句话解释 |
|------|-----------|
| PlatformTransactionManager | 事务契约三方法（getTransaction/commit/rollback） |
| @Transactional | 声明式事务注解（AOP 拦截器织入） |
| 传播行为 | 事务嵌套语义（REQUIRED 加入 / REQUIRES_NEW 独立） |
| 隔离级别 | 并发一致性承诺（脏读/不可重复读/幻读） |
| rollbackOnly | 内圈失败标记 → 外圈整体回滚机制 |
| TransactionTemplate | 编程式事务模板 |
| TransactionSynchronizationManager | 线程 → 事务资源绑定 |
| ProblemDetail 无关 / rollbackFor | 指定异常触发回滚的规则 |

---

**下一模块**：[01-模块清单](01-模块清单.md)　**返回总览**：本页

**【参考来源】**：[Spring Framework 7.0 Release Notes（GitHub Wiki）](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-7.0-Release-Notes)、[Spring Framework 7.0 正式发布解析（掘金）](https://juejin.cn/post/7573592127298682930)、spring-tx 7.0.0 依赖注册页（tessl.io）、[Spring 6→7 Migration Guide（dev.to）](https://dev.to/ankurm/spring-framework-6-to-7-migration-guide-breaking-changes-deprecated-apis-and-upgrade-checklist-3bf6)
