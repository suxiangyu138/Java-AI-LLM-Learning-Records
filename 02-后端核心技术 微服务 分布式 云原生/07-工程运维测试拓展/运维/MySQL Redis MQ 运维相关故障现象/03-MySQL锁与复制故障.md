# 03-MySQL 锁与复制故障
> 锁等待、死锁、主从延迟——"数据库阻塞与一致性的三大故障"的现象、取证与解决

## 📚 目录
1. [故障一：锁等待（Lock wait timeout）](#1-故障一锁等待lock-wait-timeout)
2. [故障二：死锁（Deadlock）](#2-故障二死锁deadlock)
3. [故障三：主从延迟](#3-故障三主从延迟)
4. [锁与事务的取证命令](#4-锁与事务的取证命令)
5. [核心要点](#5-核心要点)
6. [参考来源](#6-参考来源)

## 1. 故障一：锁等待（Lock wait timeout）

### 1.1 现象与定位

```text
现象：应用报 "Lock wait timeout exceeded; try restarting transaction"（默认 50s）
原因：事务 A 持锁未提交，事务 B 等待超时
```

```sql
-- 当前锁等待（现场）
SELECT * FROM information_schema.innodb_trx;           -- 所有事务
SELECT * FROM information_schema.innodb_lock_waits;    -- 锁等待关系

-- 谁持有锁、谁在等（核心查询）
SELECT waiting_trx_id, waiting_thread, waiting_query,
       blocking_trx_id, blocking_thread
FROM sys.innodb_lock_waits;
```

### 1.2 根因矩阵

| 根因 | 特征 | 解决 |
|------|------|------|
| **长事务** | 事务长时间不提交（缺 commit/慢 SQL） | 事务拆分、及时提交 |
| 慢 SQL 持锁 | 事务内慢 SQL 拉长持锁时间 | 优化 SQL |
| 大事务 | 一次改太多行（持锁范围大） | 分批处理 |
| 热点行竞争 | 同一行高并发更新 | 排队/异步化 |
| 间隙锁 | RR 隔离级别下范围查询锁间隙 | 索引优化/隔离级别评估 |

> 🎯 **锁等待的本质**：**事务持锁时间过长**——排查主线是"谁持锁、持了多久、为什么没提交"（innodb_trx 看 trx_started/trx_state）。

## 2. 故障二：死锁（Deadlock）

### 2.1 现象与定位

```text
现象：应用报 "Deadlock found when trying to get lock"
原因：两个事务互相等对方持有的锁（环）
```

```sql
-- 死锁现场（最近一次死锁的完整信息）
SHOW ENGINE INNODB STATUS\G
-- 找 "LATEST DETECTED DEADLOCK" 段落：
--   两个事务的 SQL、持锁/等待的锁、回滚了哪个事务
```

### 2.2 死锁解决

```text
解决方向：
  ① 加锁顺序一致（A→B 永远先锁 A）
  ② 缩短事务（减少持锁时间）
  ③ 索引优化（减少锁范围）
  ④ 事务内避免用户交互（等待输入）
  ⑤ 死锁自动回滚 + 重试（业务侧兜底）
```

| 死锁预防 | 说明 |
|----------|------|
| 锁顺序一致 | 多表更新按固定顺序 |
| 短事务 | 快速提交 |
| 索引 | 行锁靠索引定位（无索引 = 表锁） |
| 重试 | 死锁回滚后业务重试（幂等） |

> ⚠️ **死锁与锁等待的区别**：死锁 = 互相等待（MySQL 立即检测并回滚一个）；锁等待 = 单向等待（超时 50s 才报）——**死锁靠预防（顺序/短事务），锁等待靠排查（谁持锁）**。

## 3. 故障三：主从延迟

### 3.1 现象与定位

```text
现象：从库数据落后主库（读从库读到旧数据）
```

```sql
-- 从库执行：延迟时间
SHOW SLAVE STATUS\G
-- 关键字段：
--   Seconds_Behind_Master    ← 延迟秒数
--   Slave_IO_Running         ← IO 线程（拉 binlog）
--   Slave_SQL_Running        ← SQL 线程（执行）
--   Last_SQL_Error           ← 执行错误（延迟的常见原因！）

-- 8.4 新语法
SHOW REPLICA STATUS\G
```

### 3.2 根因矩阵

| 根因 | 特征 | 解决 |
|------|------|------|
| **大事务** | 主库一次改大量数据（DDL/批量） | 分批、pt-osc |
| **慢 SQL 在从库重放** | 从库执行慢（缺索引/硬件差） | 从库加索引/升级 |
| SQL 线程错误 | `Last_SQL_Error` 有值 | 修复错误（跳过/重建） |
| 主库压力大 | 主库写压力传导 | 读写分离优化 |
| 从库负载高 | 从库被读流量打满 | 从库扩容/读分流 |
| 网络/IO | binlog 拉取慢 | 网络/磁盘 |

```text
重点排查：Last_SQL_Error 是延迟的"第一嫌疑"
  （SQL 线程卡住 = 延迟只增不减）
```

> 🎯 **主从延迟排查顺序**：`Seconds_Behind_Master`（延迟多少）→ `Last_SQL_Error`（有没有错）→ `Slave_IO/SQL_Running`（线程状态）→ 主库 binlog 大小（大事务？）。

## 4. 锁与事务的取证命令

```sql
-- 事务全景
SELECT trx_id, trx_state, trx_started,
       TIMESTAMPDIFF(SECOND, trx_started, NOW()) AS trx_age,
       trx_rows_locked
FROM information_schema.innodb_trx
ORDER BY trx_started;

-- 锁等待关系
SELECT * FROM sys.innodb_lock_waits;

-- 死锁现场
SHOW ENGINE INNODB STATUS\G;

-- 锁超时配置
SHOW VARIABLES LIKE 'innodb_lock_wait_timeout';   -- 默认 50s

-- 开启死锁日志（写入错误日志）
SET GLOBAL innodb_print_all_deadlocks = ON;
```

```bash
# 应用侧：慢事务排查
# 慢日志中的长事务（持锁时间长）
grep -B2 -A2 "lock wait" /var/log/mysql/error.log
```

| 场景 | 命令 |
|------|------|
| 谁在持锁 | `sys.innodb_lock_waits` |
| 事务年龄 | `innodb_trx`（trx_started 排序） |
| 死锁现场 | `SHOW ENGINE INNODB STATUS` |
| 死锁日志 | `innodb_print_all_deadlocks=ON` |
| 主从延迟 | `SHOW REPLICA STATUS` |

## 5. 核心要点

> 🎯 **核心要点**：
> - 锁等待：单向等待超时——查 innodb_trx/innodb_lock_waits 找"谁持锁没提交"；
> - 死锁：互相等待——SHOW ENGINE INNODB STATUS 取现场，预防靠锁顺序一致 + 短事务 + 重试；
> - 死锁 vs 锁等待：立即检测回滚 vs 50s 超时；
> - 主从延迟三查：Seconds_Behind_Master → Last_SQL_Error → 大事务；
> - 大事务是锁等待与主从延迟的"共同元凶"——**事务越短，数据库越稳**；
> - 取证纪律：先 SHOW ENGINE INNODB STATUS 再处理（现场不可复现）。

## 6. 参考来源

- [MySQL 8.4 官方文档：InnoDB 锁与事务](https://dev.mysql.com/doc/refman/8.4/en/innodb-locking.html)
- [MySQL 死锁检测文档](https://dev.mysql.com/doc/refman/8.4/en/innodb-deadlock-detection.html)
- [MySQL 复制延迟排查（官方）](https://dev.mysql.com/doc/refman/8.4/en/replication-options-replica.html)

---

**下一模块**：[04-MySQL存储与数据故障](04-MySQL存储与数据故障.md)　/　**返回总览**：[00-总览](00-中间件运维故障排查总览.md)
