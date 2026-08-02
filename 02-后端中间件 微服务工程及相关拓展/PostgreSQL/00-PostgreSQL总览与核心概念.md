# 00 - PostgreSQL 总览与核心概念

> 定位：PostgreSQL 知识体系入口——定位、版本现状（2026-07 验证）、PG 18 新特性详解、vs MySQL 深度对比、核心概念、模块导航

## 📚 目录

1. [PostgreSQL 是什么](#1-postgresql-是什么)
2. [版本演进与 18 新特性](#2-版本演进与-18-新特性)
3. [PG vs MySQL 深度对比](#3-pg-vs-mysql-深度对比)
4. [核心架构概念](#4-核心架构概念)
5. [核心术语表](#5-核心术语表)
6. [模块导航](#6-模块导航)

---

## 1. PostgreSQL 是什么

```
PostgreSQL = 高级开源关系型数据库（ORDBMS）
  1986 年伯克利 POSTGRES 项目演化，开源界功能最全

核心优势：
  ① SQL 标准合规度最高（ISO/IEC 9075 全覆盖）
  ② 丰富数据类型（JSONB/数组/几何/范围/网络）
  ③ 扩展生态强大（PostGIS 地理/pgvector 向量/Citus 分布式）
  ④ MVCC 实现优雅（无回滚段，读不阻塞写）
  ⑤ 优化器优秀（多连接算法/并行查询/CTE 优化）
  ⑥ 事务能力完整（SSI 可串行化/保存点/递归 CTE）

⚠️ 面试必答：
"PG = 开源功能之王——
 SQL 标准最高、扩展最丰富（PostGIS/pgvector）、
 MVCC 无回滚段设计、优化器最强；
 从'MySQL 平替'升级为'数据平台'。"
```

### 1.1 为什么 2026 年 PG 越来越火

| 驱动力 | 说明 |
|--------|------|
| AI 向量检索 | pgvector 让 PG 成为 AI 应用的默认库 |
| JSONB 灵活性 | 无模式开发 + 关系查询两不误 |
| 分析能力 | CTE/窗口/并行查询对标数仓 |
| MySQL 被 Oracle 收购后 | 开源社区重心转移 |
| 版本节奏 | 每年 9 月大版本（16/17/18） |

---

## 2. 版本演进与 18 新特性

### 2.1 版本演进

| 版本 | 年份 | 核心特性 |
|------|:---:|---------|
| PG 10 | 2017 | 逻辑复制 |
| PG 12 | 2019 | 分区增强 |
| PG 15 | 2022 | 压缩、merge |
| PG 16 | 2023 | 并行增强 |
| PG 17 | 2024 | 增量备份 |
| **PG 18** | 2025-10/2026 | AIO、UUIDv7、Skip Scan |

### 2.2 PG 18 核心新特性（2026-07 验证）

| 特性 | 说明 | 影响 |
|------|------|------|
| **异步 IO（AIO）** | io_uring 异步读，大表扫描快 2-3× | 分析场景质变 |
| **UUID v7** | 时间有序 UUID（48 位时间戳） | 索引友好、分布式 ID |
| **B-tree Skip Scan** | 不依赖前导列用联合索引 | 少建一半索引 |
| **OAuth 2.0 认证** | 原生 Bearer Token | 免代理 SSO |
| 虚拟生成列 | 默认虚拟（不占磁盘） | 存储节省 |
| Temporal 约束 | 时间区间 PK/唯一 | 时态数据 |
| EXPLAIN 增强 | BUFFERS 默认 + Index Searches | 调优更省力 |
| 自我连接消除 | 冗余自连接自动移除 | 查询更简单 |

```sql
-- PG 18 UUID v7 示例（时间有序，索引友好）
CREATE TABLE orders (
    id uuid DEFAULT uuidv7() PRIMARY KEY,   -- ⚠️ 替代随机 UUIDv4
    created_at timestamptz DEFAULT now()
);
-- 对比 UUIDv4：v7 按时间排序插入 → 索引不碎片化
```

> 🎯 **要点**：PG 18 三大亮点——**AIO**（性能 3×）、**UUIDv7**（分布式 ID 原生解）、**Skip Scan**（索引使用更聪明）。对 Java 后端影响最大的其实是 UUIDv7 与 AIO。

---

## 3. PG vs MySQL 深度对比

### 3.1 架构与功能对比

| 维度 | PostgreSQL | MySQL |
|------|:---:|:---:|
| SQL 标准 | ✅ 最合规 | 中等 |
| 复杂查询 | ✅ 优化器强 | 简单查询快 |
| MVCC | 多版本存储（VACUUM 回收） | undo log 版本链 |
| 索引 | **六种**（B-tree/GIN/GiST/BRIN/Hash/向量） | B-tree/Hash/全文 |
| JSON | **JSONB（可索引）** | JSON（不可索引） |
| 扩展 | ✅ PostGIS/pgvector/PL 语言 | 较少 |
| 并发 | 读不阻塞写（快照） | 读写互斥（锁粒度） |
| 递归 CTE | ✅ | ⚠️ 8.0+ 才支持 |
| 窗口函数 | ✅ 完整 | ⚠️ 8.0+ |
| 分区 | 声明式分区 | 分区表 |
| 运维 | 较复杂（VACUUM） | 简单（自动清理） |
| 向量检索 | ✅ pgvector | ❌ |

### 3.2 MVCC 实现差异（核心）

```
PG：多版本存储在表空间
  更新 = 插入新版本 + 旧版本留原地
  回收 = VACUUM（Autovacuum 自动）
  → 无回滚段、无 undo 膨胀
  → 但需管理表膨胀

MySQL：undo log 回滚段
  更新 = 修改原记录 + undo 记旧值
  回收 = purge 线程自动
  → 无需手动管理
  → 长事务导致 undo 膨胀

⚠️ 面试必答：
"PG 与 MySQL MVCC 最大差异——
 PG 多版本在表内（VACUUM 回收）、
 MySQL 在 undo 回滚段（purge 回收）；
 PG 无回滚段但需要 VACUUM 运维。"
```

### 3.3 场景选型

| 场景 | 选型 | 原因 |
|------|------|------|
| 互联网 OLTP | MySQL | 生态/运维简单 |
| 分析/复杂查询 | PostgreSQL | 优化器 + CTE + 并行 |
| 地理空间 | PostgreSQL | PostGIS 无敌 |
| AI 向量检索 | PostgreSQL | pgvector 零额外部署 |
| 政务/合规 | PostgreSQL | 功能全 + 开源可控 |
| JSON 无模式 | PostgreSQL | JSONB 可索引 |

---

## 4. 核心架构概念

### 4.1 进程架构

```
PG 进程模型（与 MySQL 线程模型不同）：
  Postmaster（主进程，管理子进程）
  Backend 进程（每连接一个）
  辅助进程：WAL writer / Checkpointer / Autovacuum / Stats

⚠️ 面试必答：
"PG = 多进程架构（每连接一个进程）、
 MySQL = 多线程架构（共享内存）；
 PG 进程隔离更稳（一个连接崩不影响其他），
 MySQL 线程开销更小（高并发连接更轻）。"
```

### 4.2 存储结构

```
PG 存储层级：
  集群（Cluster）→ 数据库 → Schema → 表 → 页（8KB）→ 元组

表空间（Tablespace）：物理位置控制（可放不同磁盘）
Schema：命名空间（多租户/模块隔离）
页大小 8KB（对比 InnoDB 16KB）
TOAST：大字段自动压缩外置存储
```

### 4.3 WAL 预写日志

```
WAL（Write-Ahead Log）：
  修改前先写日志（与 MySQL redo log 同原理）
  崩溃恢复重放 WAL
  ⚠️ 对比 MySQL：PG 的 WAL 同时承担复制（流复制）

PG 特有：WAL 压缩、WAL 归档（PITR）
```

---

## 5. 核心术语表

| 术语 | 定义 | MySQL 对应 |
|------|------|-----------|
| MVCC | 多版本并发控制（元组版本链） | undo log MVCC |
| VACUUM | 清理死元组（回收空间） | purge 线程 |
| Autovacuum | 自动 VACUUM 守护进程 | 自动清理 |
| WAL | 预写日志（崩溃恢复+复制） | redo log |
| CTE | 公用表表达式（WITH 子句） | 8.0+ |
| 窗口函数 | OVER 子句 | 8.0+ |
| LATERAL | 行级子查询 JOIN | ❌ 无 |
| RETURNING | 增删改返回行 | ❌ 无 |
| UPSERT | ON CONFLICT 冲突处理 | ON DUPLICATE |
| JSONB | 二进制 JSON（可索引） | JSON |
| BRIN 索引 | 块范围索引（超大表） | ❌ 无 |
| GIN 索引 | 倒排索引（全文/数组） | FULLTEXT |
| GiST 索引 | 通用搜索树（几何） | ❌ 无 |
| pgvector | 向量相似检索扩展 | ❌ 无 |
| PostGIS | 地理空间扩展 | ❌ 无 |
| Tablespace | 表空间（物理位置） | 无 |
| Schema | 命名空间 | 库即命名空间 |
| 流复制 | WAL 日志复制 | binlog 复制 |
| 逻辑复制 | 行级复制（选择性） | binlog 解析 |

---

## 6. 模块导航

| 序号 | 模块 | 核心内容 | 定位 |
|------|------|---------|------|
| 00 | [本总览](00-PostgreSQL总览与核心概念.md) | 定位、版本、对比 | 入口 |
| 01 | [SQL进阶与独有特性](01-PostgreSQLSQL进阶与独有特性.md) | CTE/LATERAL/RETURNING/UPSERT/窗口 | 查询 |
| 02 | [索引原理与设计](02-PostgreSQL索引原理与设计.md) | 六种索引/Skip Scan/GIN/GiST/BRIN | 索引 |
| 03 | [MVCC与并发控制](03-PostgreSQLMVCC与并发控制.md) | 元组版本/VACUUM/隔离级别/锁 | 原理 |
| 04 | [扩展生态](04-PostgreSQL扩展生态.md) | pgvector/PostGIS/PL/FDW/全文 | 扩展 |
| 05 | [SpringBoot集成](05-PostgreSQL与SpringBoot集成.md) | JDBC/JSONB/pgvector/Flyway/读写分离 | 实战 |
| 06 | [性能优化与运维](06-PostgreSQL性能优化与运维.md) | EXPLAIN/VACUUM调优/备份/监控/面试 | 运维 |

---

> 🎯 **本体系学习建议**：PG 的学习重点是**差异**——与 MySQL 的 3 大差异（MVCC 无回滚段 + 六种索引 + 扩展生态）是面试主线。先建立对比认知（00），再学独有特性（01-04），最后实战（05-06）。

---

**下一篇**：[01-PostgreSQL SQL进阶与独有特性](01-PostgreSQLSQL进阶与独有特性.md)
