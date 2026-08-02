# 05 - SQL Server 运维与高可用

> 定位：备份恢复、Always On 集群、日志管理、监控视图、升级迁移、SQL Server 面试题——企业运维全体系

## 📚 目录

1. [备份与恢复](#1-备份与恢复)
2. [Always On 高可用](#2-always-on-高可用)
3. [日志管理](#3-日志管理)
4. [监控与性能视图](#4-监控与性能视图)
5. [升级迁移](#5-升级迁移)
6. [SQL Server 面试题精选](#6-sql-server-面试题精选)

---

## 1. 备份与恢复

### 1.1 三种备份

| 备份类型 | 命令 | 用途 |
|---------|------|------|
| 完整备份 | `BACKUP DATABASE MyDB TO DISK = '...'` | 全量基础 |
| 差异备份 | `BACKUP DATABASE MyDB TO DISK = '...' WITH DIFFERENTIAL` | 增量（基于上次完整） |
| 事务日志备份 | `BACKUP LOG MyDB TO DISK = '...'` | 时间点恢复 |

```sql
-- 完整备份
BACKUP DATABASE MyDB TO DISK = 'E:\backups\MyDB_full.bak'
    WITH INIT, FORMAT, COMPRESSION;

-- 事务日志备份（每小时）
BACKUP LOG MyDB TO DISK = 'E:\backups\MyDB_log.bak';

-- 恢复时间点（PITR）
RESTORE DATABASE MyDB FROM DISK = 'E:\backups\MyDB_full.bak' WITH NORECOVERY;
RESTORE LOG MyDB FROM DISK = 'E:\backups\MyDB_log.bak'
    WITH RECOVERY, STOPAT = '2026-08-02 10:00:00';
```

### 1.2 恢复模式

| 模式 | 日志 | PITR | 适用 |
|------|:---:|:---:|------|
| FULL | 全记录 | ✅ | 生产默认 |
| SIMPLE | 自动截断 | ❌ | 开发/测试 |
| BULK_LOGGED | 减少大操作日志 | 有限 | ETL |

---

## 2. Always On 高可用

### 2.1 Always On 是什么

```
Always On = SQL Server 的高可用与灾难恢复方案

三件套：
  ① 故障转移集群（FCI）：共享存储 + 自动切换
  ② 可用性组（AG）：数据库级复制（多副本）
  ③ 可读辅助副本：读写分离（applicationIntent=ReadOnly）

⚠️ 对比 MySQL 主从：
  Always On：更完整的自动切换 + 可读副本路由透明
  MySQL 主从：需额外中间件（MHA/ProxySQL）
```

### 2.2 AG 配置要点

```
AG 核心能力：
  同步/异步提交可选（按业务重要性）
  自动故障转移（最少 2 副本 + 见证服务器）
  只读路由（JDBC applicationIntent=ReadOnly → 自动只读副本）
  在线索引维护（辅助副本上重建索引）
```

> 🎯 **要点**：Always On 是 SQL Server 的高可用护城河——MySQL/PostgreSQL 均需第三方方案才能达到同等效果（MySQL MHA/Orchestrator + ProxySQL、PG Patroni）。

---

## 3. 日志管理

### 3.1 事务日志（LDF）

```
事务日志特点：
  FULL 恢复模式下日志持续增长！
  必须定期备份日志来截断（释放空间）

⚠️ 日志满的后果：数据库只读（无法写入）

监控：
  DBCC SQLPERF(LOGSPACE)   -- 查看日志使用率
```

```sql
-- 日志备份后截断（释放空间）
BACKUP LOG MyDB TO DISK = '...';

-- 收缩日志（紧急空间不足时，生产慎用）
DBCC SHRINKFILE(MyDB_log, 100);
```

### 3.2 SQL Server Agent（作业调度）

```sql
-- Agent 作业：定时备份/日志清理/重建索引
-- SSMS → SQL Server Agent → 新建作业 → 计划
-- 或 T-SQL 创建
EXEC sp_add_job @job_name = 'NightlyBackup';
EXEC sp_add_jobschedule @job_name = 'NightlyBackup',
    @freq_type = 4, @freq_interval = 1,    -- 每天
    @active_start_time = 020000;            -- 凌晨 2 点
```

---

## 4. 监控与性能视图

### 4.1 动态管理视图（DMV）

```sql
-- 系统视图（sys.dm_*）是 SQL Server 监控核心

-- ① 当前等待（性能瓶颈第一看）
SELECT wait_type, waiting_tasks_count, wait_time_ms
FROM sys.dm_os_wait_stats
ORDER BY wait_time_ms DESC;

-- ② 慢查询（按耗时）
SELECT TOP 10 total_elapsed_time/execution_count AS avg_time,
       execution_count, text
FROM sys.dm_exec_query_stats qs
CROSS APPLY sys.dm_exec_sql_text(qs.sql_handle)
ORDER BY total_elapsed_time DESC;

-- ③ 缺失索引（优化器建议）
SELECT * FROM sys.dm_db_missing_index_details;

-- ④ 当前阻塞
SELECT session_id, blocking_session_id, wait_type
FROM sys.dm_exec_requests WHERE blocking_session_id > 0;
```

### 4.2 关键指标

```
SQL Server 监控四指标：
  ① 等待统计（dm_os_wait_stats）——瓶颈定位
  ② 慢查询（dm_exec_query_stats）——SQL 优化
  ③ 锁/阻塞（dm_exec_requests）——并发问题
  ④ 磁盘 IO（dm_io_virtual_file_stats）——存储瓶颈
```

---

## 5. 升级迁移

```bash
# SQL Server 升级路线：
# ① 原地升级（installer）
# ② 备份恢复（.bak 跨服务器）
# ③ 导入导出向导（SSMS 图形化）
# ④ 数据库复制向导

# MySQL → SQL Server 迁移：
#   SQL Server Migration Assistant (SSMA) for MySQL
#   Microsoft 官方工具（免费）
```

```
跨版本兼容性：
  .bak 文件向下兼容（低版本无法恢复高版本备份）
  迁移时确认目标版本 ≥ 源版本
```

---

## 6. SQL Server 面试题精选

**Q1: SQL Server 和 MySQL 的区别？**
```
SQL Server：微软商业 + T-SQL + SSMS 图形化 + BI 集成（SSIS/SSRS/SSAS）；
MySQL：开源 + 互联网 OLTP 广泛；
SQL Server 在政务/国企/金融场景占比高（Windows 生态绑定）。
```

**Q2: 聚集索引和非聚集索引？**
```
聚集索引（1个）：叶子层是数据行，决定物理顺序；
非聚集索引（999个）：叶子层存键 + 聚集键，需 Key Lookup 回表；
INCLUDE 列免回表（覆盖索引）。
```

**Q3: RCSI 是什么？为什么开启？**
```
读提交快照隔离——SELECT 不加锁（读行版本快照），
消除读写互相阻塞。是 SQL Server 并发优化的第一推荐。
```

**Q4: 锁升级是什么？**
```
行锁 ≥ 5000 → 自动升级表锁（减少开销）。
可能导致意外大范围阻塞。
```

**Q5: 查询存储（Query Store）的作用？**
```
查询性能的飞行记录仪，记录执行计划与运行统计；
升级前后对比、回归检测、强制最优计划。
```

**Q6: Always On 和 MySQL 主从的区别？**
```
AG：自动切换 + 只读路由透明 + 同步可选；
MySQL 主从：需第三方中间件（MHA/ProxySQL）。
```

**Q7: FULL 恢复模式日志满了怎么办？**
```
立即备份日志 → 收缩日志；根本方案：增加日志备份频率。
日志满 = 数据库只读。
```

**Q8: Java 连接 SQL Server 的注意事项？**
```
MSSQL JDBC 驱动；Windows 集成认证需 DLL；
BIT → Boolean、IDENTITY → 自增；
存储过程用 SimpleJdbcCall。
```

---

> 🎯 **核心要点**：SQL Server 运维 = **三种备份**（FULL/DIFF/LOG + PITR）+ **Always On**（AG 自动切换 + 可读副本）+ **DMV 监控**（等待/慢查询/缺失索引）+ **Query Store**（性能回归检测）。"Always On + Query Store + 存储过程"是政务/国企 SQL Server 的三把运维利器。

---

**返回总览**：[00-SQLServer总览与核心概念](00-SQLServer总览与核心概念.md) | **上一篇**：[04-SQLServer与SpringBoot集成](04-SQLServer与SpringBoot集成.md)
