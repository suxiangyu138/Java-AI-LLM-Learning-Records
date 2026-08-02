# 00 - SQL Server 总览与核心概念

> 定位：SQL Server 知识体系入口——定位与适用场景、版本演进与选型、vs MySQL/PostgreSQL 深度对比、核心概念与数据库结构、模块导航

## 📚 目录

1. [SQL Server 是什么](#1-sql-server-是什么)
2. [版本演进与选型](#2-版本演进与选型)
3. [SQL Server vs MySQL vs PostgreSQL 深度对比](#3-sql-server-vs-mysql-vs-postgresql-深度对比)
4. [数据库结构核心概念](#4-数据库结构核心概念)
5. [Schema 与多租户](#5-schema-与多租户)
6. [模块导航](#6-模块导航)

---

## 1. SQL Server 是什么

```
SQL Server = Microsoft 的企业级关系数据库（RDBMS）
  1989 年首版，Windows 生态的核心数据组件

国内核心场景（Java 后端视角）：
  政务/国企：Windows Server + SQL Server 强绑定
  金融/保险：企业级支持 + 审计合规
  外企/合资：全球 Windows 基础设施标配
  医疗/教育：采购预算内的微软生态标准

JDBC 交互：
  MSSQL JDBC Driver（Microsoft 官方，开源）
  Spring Boot 集成：与 MySQL 同接口（jdbc:sqlserver://）

⚠️ 面试必答：
"SQL Server 在国内政务/国企/金融场景
 占有率高——Windows 生态绑定 + 企业级服务；
 核心差异：T-SQL 扩展 + SSMS 图形化管理 +
 SSIS/SSRS/SSAS 集成服务三件套。"
```

### 1.1 与其他主流数据库的定位差异

| 数据库 | 生态绑定 | 主要场景 |
|--------|---------|---------|
| SQL Server | Windows + .NET | 政务/国企/金融 |
| MySQL | 开源 + LAMP | 互联网 OLTP |
| PostgreSQL | 开源 + 分析 | 分析/地理/向量 |
| Oracle | 商业 + 企业 | 大型核心系统 |

```
⚠️ 面试必答：
"数据库选型看生态而非单点性能——
 SQL Server 赢在微软全家桶（Windows/AD/.NET/SSMS）、
 MySQL 赢在互联网普及、PG 赢在功能与扩展。"
```

---

## 2. 版本演进与选型

### 2.1 版本演进时间线

| 版本 | 年份 | 核心变化 |
|------|:---:|---------|
| SQL Server 2008 | 2008 | T-SQL 增强、数据压缩 |
| SQL Server 2012 | 2012 | Always On、列存储索引 |
| SQL Server 2014 | 2014 | 内存 OLTP（Hekaton） |
| SQL Server 2016 | 2016 | Query Store、JSON 支持 |
| SQL Server 2017 | 2017 | ✅ 首个 Linux 版 |
| SQL Server 2019 | 2019 | 智能查询处理、UTF-8 |
| SQL Server 2022 | 2022 | 云集成、Ledger 防篡改 |
| Azure SQL | 持续 | 云托管（PaaS） |

### 2.2 选型建议（2026）

| 场景 | 版本选型 |
|------|---------|
| 新生产系统 | SQL Server 2022（或云 Azure SQL） |
| 政务/国企存量 | SQL Server 2019（升级中） |
| 开发/测试 | Express 免费版（10GB 限制） |
| 云化 | Azure SQL（托管，免运维） |

```
⚠️ 版本注意：
  ① 2022 是当前主流（云集成 + Ledger）
  ② Linux 版 2017+ 打破 Windows 独占（容器部署可行）
  ③ Express 免费但 10GB 限制（小项目够用）
  ④ 许可证商业——开源 MySQL/PG 是替代选项
```

---

## 3. SQL Server vs MySQL vs PostgreSQL 深度对比

### 3.1 架构与功能对比

| 维度 | SQL Server | MySQL | PostgreSQL |
|------|:---:|:---:|:---:|
| 厂商 | Microsoft | Oracle | 社区 |
| 许可证 | 商业 | 开源 | 开源 |
| 平台 | Windows/ Linux | 跨平台 | 跨平台 |
| 查询语言 | T-SQL | SQL | SQL（标准最高） |
| 管理工具 | **SSMS（图形化最强）** | 命令行/Workbench | pgAdmin |
| 索引类型 | 聚集/非聚集/INCLUDE | B-tree/哈希/全文 | 六种（GIN/GiST/BRIN） |
| 分区 | 分区表 | 分区表 | 分区表+继承 |
| 事务隔离 | READ_COMMITTED_SNAPSHOT | RR（MVCC） | RC（MVCC） |
| 高可用 | **Always On** | MHA/Orchestrator | Patroni |
| BI 集成 | **SSIS/SSRS/SSAS** | 无 | 需第三方 |
| 查询缓存 | Query Store | 无 | 无 |
| 向量检索 | 无 | 无 | **pgvector** |

### 3.2 选型决策树

```
团队技术栈？
  ├─ .NET / Windows → SQL Server（生态绑定）
  ├─ Java + 互联网 → MySQL（普及度高）
  └─ Java + 分析/地理 → PostgreSQL（功能强）
业务类型？
  ├─ 政务/金融（合规+审计） → SQL Server / Oracle
  ├─ 互联网 OLTP → MySQL
  └─ BI/分析/地理/向量 → PostgreSQL
成本约束？
  ├─ 预算充足 → SQL Server（商业支持）
  └─ 预算受限 → MySQL / PostgreSQL（开源）
```

> 🎯 **要点**：选型 = 生态 + 场景 + 成本三角——**Java 后端最常见的是 MySQL（互联网）与 SQL Server（政企）**，PG 是后起之秀（分析/AI 向量）。

---

## 4. 数据库结构核心概念

### 4.1 SQL Server 的物理-逻辑结构

```
实例（Instance）→ 数据库（Database）→ Schema → 表

实例 = 一个 SQL Server 服务进程（可含多个数据库）
数据库 = 逻辑隔离单元（业务库/系统库）
Schema = 数据库内的命名空间（默认 dbo）
表 = 数据存储单元

物理存储：
  数据库由文件组（Filegroup）管理
  每个文件组包含数据文件（.mdf 主 / .ndf 次）
  事务日志独立（.ldf）
```

### 4.2 系统数据库

| 系统库 | 用途 |
|--------|------|
| master | 实例级元数据（登录/配置） |
| tempdb | 临时对象（⚠️ 频繁操作需独立盘） |
| model | 新库模板 |
| msdb | Agent 作业/备份历史 |

```sql
-- 查看数据库信息
SELECT name, database_id, state_desc FROM sys.databases;

-- 查看文件组
SELECT name, type_desc FROM sys.filegroups;
```

> 🎯 **要点**：**Schema 在 SQL Server 中不是库**（是 Database 内的命名空间）——与 MySQL 的"库即命名空间"不同、与 PG 类似。这是跨库迁移的第一认知差异。

---

## 5. Schema 与多租户

### 5.1 Schema 的作用

```sql
-- ① 逻辑分组（模块隔离）
CREATE SCHEMA sales;
CREATE SCHEMA hr;

CREATE TABLE sales.orders (id INT, ...);   -- 销售模块表
CREATE TABLE hr.employees (id INT, ...);   -- 人力模块表

-- ② 权限隔离（不同用户访问不同 Schema）
CREATE USER sales_user WITHOUT LOGIN;
GRANT SELECT ON SCHEMA::sales TO sales_user;
```

### 5.2 多租户方案对比

| 方案 | 隔离 | 运维 | 适用 |
|------|:---:|:---:|------|
| 独立数据库 | 强 | 重 | 大客户 |
| Schema 隔离 | 中 | 中 | **中小租户（SQL Server 特色）** |
| 共享表 + 租户 ID | 弱 | 轻 | 小租户 |

```
⚠️ 面试必答：
"SQL Server 多租户三方案——独立库（强隔离）、
 Schema 隔离（中隔离，SQL Server 推荐做法）、
 共享表（弱隔离，成本最低）；
 选型看租户规模与合规要求。"
```

---

## 6. 模块导航

| 序号 | 模块 | 核心内容 | 定位 |
|------|------|---------|------|
| 00 | [本总览](00-SQLServer总览与核心概念.md) | 版本、对比、结构 | 入口 |
| 01 | [T-SQL进阶特性](01-T-SQL进阶特性.md) | CTE/窗口/TOP-OFFSET/PIVOT/MERGE/临时表 | 查询 |
| 02 | [索引与性能](02-SQLServer索引与性能.md) | 聚集/INCLUDE/执行计划/统计/Query Store | 性能 |
| 03 | [事务与并发控制](03-SQLServer事务与并发控制.md) | 隔离/RCSI/锁升级/死锁/乐观并发 | 原理 |
| 04 | [SpringBoot集成](04-SQLServer与SpringBoot集成.md) | JDBC/JPA/存储过程/Windows 认证/多数据源 | 实战 |
| 05 | [运维与高可用](05-SQLServer运维与高可用.md) | 备份/Always On/日志/DMV/面试题 | 运维 |

---

> 🎯 **本体系学习建议**：SQL Server 体系面向"政务/国企/金融场景的 Java 后端"。先了解定位差异（00），再学 T-SQL 特有语法（01），深入索引/事务（02-03），最后实战（04-05）。面试核心 = T-SQL 差异 + RCSI + Always On + Query Store 四大记忆点。

---

**下一篇**：[01-T-SQL进阶特性](01-T-SQL进阶特性.md)
