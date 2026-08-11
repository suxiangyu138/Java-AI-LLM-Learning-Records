# 04-MySQL 存储与数据故障
> 磁盘满、binlog 膨胀、误删恢复、OOM——"数据安全类故障"的现象、取证与恢复

## 📚 目录
1. [故障一：磁盘满](#1-故障一磁盘满)
2. [故障二：binlog 膨胀](#2-故障二binlog-膨胀)
3. [故障三：误删数据恢复](#3-故障三误删数据恢复)
4. [故障四：MySQL OOM/内存问题](#4-故障四mysql-oom内存问题)
5. [备份与恢复策略](#5-备份与恢复策略)
6. [核心要点](#6-核心要点)
7. [参考来源](#7-参考来源)

## 1. 故障一：磁盘满

### 1.1 现象

```text
现象：写入报 "No space left on device" / MySQL 只读
       MySQL 磁盘满 → 默认进入只读模式（保护数据）
```

```bash
# 定位哪块磁盘满
df -h
# 找大目录
du -sh /var/lib/mysql/* | sort -rh | head -10
```

### 1.2 磁盘占用四大来源

| 来源 | 说明 | 清理 |
|------|------|------|
| **binlog** | 未清理的二进制日志（最常见） | `PURGE BINARY LOGS`（见故障二） |
| 数据文件 | ibd 文件增长 | 扩容/归档 |
| 临时文件 | 大排序/大事务临时表 | 优化 SQL |
| 慢日志/错误日志 | 未轮转 | logrotate |

> ⚠️ **磁盘满的连锁反应**：只读 → 应用写入失败 → 主从延迟 → 业务故障——**磁盘使用率 > 80% 必须告警**（见日志监控指标体系的磁盘告警）。

## 2. 故障二：binlog 膨胀

### 2.1 现象与检查

```sql
-- 查看 binlog 配置
SHOW VARIABLES LIKE 'binlog_expire_logs_seconds';   -- 8.4：过期秒数
SHOW VARIABLES LIKE 'max_binlog_size';
SHOW BINARY LOGS;                                    -- 当前 binlog 列表
```

```bash
# 磁盘占用
ls -lh /var/lib/mysql/*.bin* | head
```

### 2.2 解决

```sql
-- 手动清理（谨慎：先确认从库已消费）
PURGE BINARY LOGS BEFORE NOW() - INTERVAL 7 DAY;    -- 清理 7 天前

-- 配置自动过期（8.4 推荐）
SET GLOBAL binlog_expire_logs_seconds = 604800;     -- 7 天
-- 或写入 my.cnf：
-- binlog_expire_logs_seconds = 604800
```

> ⚠️ **清理 binlog 前确认从库状态**：从库依赖 binlog 追数据——**先看 `SHOW REPLICA STATUS` 的延迟与 IO 线程**，再 PURGE（清太快 = 从库断流）。

## 3. 故障三：误删数据恢复

### 3.1 恢复层级（按损失程度）

```text
① 表数据误删（DELETE/UPDATE 无 WHERE）
   方案：binlog 回放（基于时间点恢复）
② 表结构误删（DROP TABLE）
   方案：备份 + binlog 增量
③ 全库误删（DROP DATABASE）
   方案：全量备份 + binlog 增量回放
```

### 3.2 基于 binlog 的恢复流程

```bash
# ① 确认 binlog 位置（误删时间点附近）
mysqlbinlog --no-defaults --base64-output=decode-rows -v \
  /var/lib/mysql/binlog.000123 | grep -B5 -A5 "DELETE"

# ② 恢复到误删前时间点
mysqlbinlog --stop-datetime='2026-08-08 10:00:00' \
  /var/lib/mysql/binlog.000120 /var/lib/mysql/binlog.000121 ... \
  | mysql -u root -p

# ③ 或恢复到指定位置（更精确）
mysqlbinlog --stop-position=123456789 \
  /var/lib/mysql/binlog.000123 | mysql -u root -p
```

```text
恢复三要素：
  备份（全量，起点）
  binlog（增量，从备份点到误删前）
  时间/位置（精确停止点）
```

> ⚠️ **恢复的纪律**：**先备份当前状态再恢复**（防止操作失误二次破坏）；恢复操作在从库演练后执行；**没有 binlog = 无法时间点恢复**（binlog 是数据安全的底线）。

## 4. 故障四：MySQL OOM/内存问题

### 4.1 现象与定位

```text
现象：MySQL 进程被杀（OOM）/性能骤降
检查：dmesg | grep -i oom（内核日志确认被杀）
```

```sql
-- 内存配置
SHOW VARIABLES LIKE 'innodb_buffer_pool_size';   -- 最大内存占用者（建议 60-70% 内存）
SHOW VARIABLES LIKE 'innodb_buffer_pool_instances';
SHOW VARIABLES LIKE 'max_connections';           -- 每连接有内存开销
```

### 4.2 根因与解决

| 根因 | 解决 |
|------|------|
| buffer_pool 过大 | 调整为内存的 60-70% |
| 连接数过多 | 每连接 ~1MB+ 开销——控制连接 |
| 临时表/排序内存 | 优化 SQL（避免大临时表） |
| 内存碎片 | 重启（临时）/参数调整（长期） |

```text
内存规划（8C16G 参考）：
  innodb_buffer_pool_size = 10G（60-70%）
  max_connections = 300
  其他（排序/连接缓冲）预留 30%
```

> 💡 **OOM 预防**：buffer_pool 是内存大头（60-70%）+ 连接数控制 + 慢查询（临时表）优化——**内存规划的本质是"给每类消耗留预算"**。

## 5. 备份与恢复策略

### 5.1 备份方案

| 方案 | 工具 | 特点 |
|------|------|------|
| 逻辑备份 | mysqldump | 简单、慢、跨版本 |
| 物理备份 | XtraBackup | 快、支持增量、生产推荐 |
| 快照 | 云盘快照 | 秒级、依赖云厂商 |

```bash
# XtraBackup 全量 + 增量
xtrabackup --backup --target-dir=/backup/full
xtrabackup --backup --incremental-basedir=/backup/full --target-dir=/backup/inc1

# mysqldump（小库/结构）
mysqldump -u root -p --single-transaction --routines db > db.sql
```

### 5.2 恢复演练（关键）

```text
备份的价值在"能恢复"：
  定期恢复演练（季度）
  恢复时间目标（RTO）与丢失容忍（RPO）定义
  备份完整性校验（定期抽样恢复验证）
```

> 🎯 **数据安全的黄金组合**：**全量备份 + binlog 增量 + 定期恢复演练**——备份不演练 = 可能恢复不了（"备份了"不等于"能恢复"）。

## 6. 核心要点

> 🎯 **核心要点**：
> - 磁盘满四查：df → binlog 占用 → 数据文件 → 临时文件；>80% 告警；
> - binlog 膨胀：`binlog_expire_logs_seconds` 自动过期；手动 PURGE 前确认从库；
> - 误删恢复三要素：备份（起点）+ binlog（增量）+ 时间/位置（停止点）；
> - 恢复纪律：先备份当前状态、从库演练、binlog 是数据安全底线；
> - OOM：buffer_pool 60-70% + 连接控制 + 慢查询优化；
> - 数据安全黄金组合：全量 + 增量 + 演练（备份不演练 = 可能恢复不了）。

## 7. 参考来源

- [MySQL 8.4 官方文档：binlog 管理](https://dev.mysql.com/doc/refman/8.4/en/binary-log.html)
- [MySQL 时间点恢复文档](https://dev.mysql.com/doc/refman/8.4/en/point-in-time-recovery.html)
- [Percona XtraBackup 文档](https://docs.percona.com/percona-xtrabackup/)

---

**下一模块**：[05-Redis连接与内存故障](05-Redis连接与内存故障.md)　/　**返回总览**：[00-总览](00-中间件运维故障排查总览.md)
