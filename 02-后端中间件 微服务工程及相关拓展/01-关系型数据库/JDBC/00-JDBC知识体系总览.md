# JDBC 知识体系总览

> JDBC 是 Java 访问关系型数据库的底层标准——MyBatis、MyBatisPlus、Spring JDBC 全部建立在它之上。懂 JDBC 才能懂 ORM 框架的本质

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [为什么必须学透 JDBC](#3-为什么必须学透-jdbc)
4. [核心概念速查](#4-核心概念速查)
5. [与周边知识的关系](#5-与周边知识的关系)
6. [学习路线推荐](#6-学习路线推荐)
7. [常见误区](#7-常见误区)
8. [快速自测 10 题](#8-快速自测-10-题)

---

## 1. 知识体系导图

```text
JDBC 知识体系
│
├── 01 核心概念与 DriverManager
│   ├── JDBC 是什么：Java 的数据库访问标准
│   ├── 驱动加载与 DriverManager（JDBC 4 自动注册）
│   ├── URL 格式：jdbc:mysql://host:port/db
│   ├── Connection 获取与关闭
│   └── JDBC 版本演进（4.0/4.1/4.2/4.3）
│
├── 02 Statement 三兄弟
│   ├── Statement：静态 SQL
│   ├── PreparedStatement：预编译 + 防注入（首选）
│   ├── CallableStatement：存储过程
│   ├── SQL 注入原理与预编译机制
│   └── 三兄弟选型
│
├── 03 ResultSet 与数据读取
│   ├── ResultSet 游标遍历
│   ├── getXxx 类型映射
│   ├── 列名 vs 列索引
│   ├── 大字段（Blob/Clob）
│   └── 结果集元数据（ResultSetMetaData）
│
├── 04 事务管理与隔离
│   ├── setAutoCommit(false) 手动事务
│   ├── commit / rollback / savepoint
│   ├── 隔离级别与 MySQL 默认
│   ├── 事务与连接的关系（一连接一事务）
│   └── 与 Spring 事务联动
│
├── 05 连接池与 DataSource
│   ├── 为什么需要连接池（创建连接昂贵）
│   ├── DataSource 标准接口
│   ├── HikariCP（Spring Boot 默认）参数
│   ├── Druid 阿里方案
│   └── 连接池参数最佳实践
│
├── 06 批量操作与批处理
│   ├── addBatch / executeBatch
│   ├── 批处理性能对比（N 次 vs 1 批）
│   ├── MySQL rewriteBatchedStatements
│   └── 批量与事务组合
│
├── 07 JDBC 与框架的关系
│   ├── MyBatis 底层：SQL 执行如何走 JDBC
│   ├── MyBatisPlus：预编译与结果映射
│   ├── Spring JdbcTemplate
│   ├── 分页的实现原理（LIMIT 拼接）
│   └── 读框架源码的 JDBC 视角
│
├── 08 常见问题与最佳实践
│   ├── 连接泄漏（未关闭）与排查
│   ├── 时区问题（serverTimezone）
│   ├── 编码问题（characterEncoding）
│   ├── 性能：预编译/批处理/查询优化
│   └── 资源关闭规范（TWR）
│
└── 09 面试高频考点与总结
    ├── 必背考点：驱动/预编译/事务/连接池
    ├── 高频陷阱题
    ├── 场景题：手写简易 JDBC 工具
    └── 记忆口诀与进阶导航
```

---

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 | 文件 |
|:---:|------|---------|---------|------|
| 01 | 核心概念与 DriverManager | 驱动加载、URL、Connection | 初中级必须掌握 | [01-核心概念与DriverManager](./01-核心概念与DriverManager.md) |
| 02 | Statement 三兄弟 | PreparedStatement、防注入 | 初中级必须掌握 | [02-Statement三兄弟](./02-Statement三兄弟.md) |
| 03 | ResultSet 与数据读取 | 游标、类型映射、元数据 | 初中级必须掌握 | [03-ResultSet与数据读取](./03-ResultSet与数据读取.md) |
| 04 | 事务管理与隔离 | 手动事务、savepoint、隔离级别 | 中高级 | [04-事务管理与隔离](./04-事务管理与隔离.md) |
| 05 | 连接池与 DataSource | HikariCP/Druid、参数 | 中高级 | [05-连接池与DataSource](./05-连接池与DataSource.md) |
| 06 | 批量操作与批处理 | addBatch、rewriteBatchedStatements | 中高级 | [06-批量操作与批处理](./06-批量操作与批处理.md) |
| 07 | JDBC 与框架的关系 | MyBatis 底层、JdbcTemplate、分页 | 中高级 | [07-JDBC与框架的关系](./07-JDBC与框架的关系.md) |
| 08 | 常见问题与最佳实践 | 泄漏/时区/编码/性能 | 中高级 | [08-常见问题与最佳实践](./08-常见问题与最佳实践.md) |
| 09 | 面试高频考点与总结 | 必背考点、陷阱题、场景题 | 面试冲刺 | [09-面试高频考点与总结](./09-面试高频考点与总结.md) |

---

## 3. 为什么必须学透 JDBC

1. **一切 ORM 的地基**：MyBatis 的 SQL 执行、MyBatisPlus 的预编译、Spring 的事务管理——**底层全是 JDBC**。不懂 JDBC，框架是"黑盒"。
2. **面试必考底层题**："PreparedStatement 为什么防注入""连接池为什么快""MyBatis 怎么执行 SQL"——JDBC 是这些题的答案源头。
3. **排查 SQL 问题的钥匙**：时区、编码、连接泄漏、批量性能——JDBC 层的问题在框架层难以定位，必须回到 JDBC 理解。
4. **手写基础设施的能力**：简易连接池、批量导入工具、报表导出——JDBC 是"脱离框架也能干活"的基本功。
5. **读源码的入口**：MyBatis 的 `PreparedStatementHandler`、Spring 的 `JdbcTemplate`、HikariCP 的连接管理——JDBC 视角是读这些源码的钥匙。

---

## 4. 核心概念速查

### 4.1 JDBC 五个核心接口

| 接口 | 作用 | 获取方式 |
|------|------|---------|
| Driver | 数据库驱动 | 自动注册（JDBC 4） |
| DriverManager | 管理驱动/获取连接 | `getConnection(url, user, pwd)` |
| Connection | 数据库连接 | 连接池 `getConnection()` |
| Statement | SQL 执行器 | `conn.createStatement()` |
| ResultSet | 结果集 | `stmt.executeQuery()` |

### 4.2 标准 JDBC 七步

```java
// ① 加载驱动（JDBC 4 自动，可省）
// ② 获取连接
// ③ 创建 Statement
// ④ 执行 SQL
// ⑤ 处理结果
// ⑥ 关闭结果集
// ⑦ 关闭连接
```

### 4.3 关键 URL 参数

```text
jdbc:mysql://localhost:3306/db?useSSL=false
  &serverTimezone=Asia/Shanghai      # 时区（必须）
  &characterEncoding=utf8            # 编码
  &rewriteBatchedStatements=true     # 批处理优化
  &useUnicode=true
```

---

## 5. 与周边知识的关系

```text
                    ┌── MySQL/ —— 数据库侧原理（事务/索引）
                    ├── MyBatis/ —— 基于 JDBC 的 ORM（读它看 JDBC 应用）
                    ├── MyBatisPlus/ —— 增强层（预编译/结果映射）
                    ├── Spring框架核心/06 —— 事务管理（底层是 JDBC 事务）
                    ├── Java异常体系/04 —— TWR 关闭连接
                    └── 08-高并发与性能优化 —— 连接池与性能
```

---

## 6. 学习路线推荐

**路线一：入门夯实（1-2 天，模块 01-03）**
核心概念 → Statement 三兄弟（重点 PreparedStatement）→ ResultSet——手写一个完整的 CRUD。

**路线二：进阶深化（1 周，模块 04-06）**
事务 → 连接池（HikariCP 参数）→ 批量操作——理解"框架替你做了什么"。

**路线三：框架联动（模块 07-09）**
MyBatis 底层视角 → 常见问题 → 面试冲刺——结合 MyBatis/MyBatisPlus 体系交叉验证。

> 🎯 **核心要点**：JDBC 学习的终点 = "**会手写（七步 CRUD）+ 懂原理（预编译/事务/连接池）+ 能排障（时区/泄漏/批量）**"——三件事是框架时代的 JDBC 基本功。

## 7. 常见误区

| 误区 | 真相 |
|------|------|
| "Class.forName 必须写" | JDBC 4 自动注册（ServiceLoader）——老教程过时 |
| "Statement 也行" | 拼接 = 注入风险 + 无预编译——99% 用 PreparedStatement |
| "getInt 取到 0 就是 0" | NULL → 0——需 wasNull 判断 |
| "连接池 close = 断开" | 归还池中（复用） |
| "分页 page 从 0 开始" | 从 1 开始——传 0 查不到 |
| "批处理就是 executeBatch" | 需关自动提交 + MySQL rewrite 才完整 |
| "时区不配也行" | 新驱动强制（报错/时间错乱） |
| "框架替代 JDBC" | 框架是 JDBC 封装——原理相同 |

---

## 8. 快速自测 10 题

1. JDBC 4 还需要 `Class.forName` 加载驱动吗？
2. PreparedStatement 为什么能防 SQL 注入？
3. Statement 和 PreparedStatement 的区别？选哪个？
4. ResultSet 怎么判断"没有下一行"？
5. 事务为什么必须 setAutoCommit(false)？
6. savepoint 是什么？什么时候用？
7. 连接池为什么快？HikariCP 的核心参数？
8. `rewriteBatchedStatements=true` 的作用？
9. MyBatis 底层怎么执行 SQL？（JDBC 视角）
10. 连接泄漏的排查方法？

> 💡 答不出的题目对应上方模块导航中的文件，按图索骥复习即可。

---

**下一模块**：[01-核心概念与DriverManager](./01-核心概念与DriverManager.md)
