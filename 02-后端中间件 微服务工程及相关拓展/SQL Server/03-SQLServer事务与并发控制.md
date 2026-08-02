# 03 - SQL Server 事务与并发控制

> 定位：隔离级别（含 Snapshot）、锁升级、死锁图、乐观并发——SQL Server 并发模型与企业实践

## 📚 目录

1. [隔离级别体系](#1-隔离级别体系)
2. [Snapshot 隔离](#2-snapshot-隔离)
3. [锁机制与锁升级](#3-锁机制与锁升级)
4. [死锁检测](#4-死锁检测)
5. [乐观并发](#5-乐观并发)
6. [SQL Server 2025 并发增强](#6-sql-server-2025-并发增强)

---

## 1. 隔离级别体系

| 级别 | 脏读 | 不可重复读 | 幻读 |
|------|:---:|:---:|:---:|
| READ UNCOMMITTED | 可能 | 可能 | 可能 |
| READ COMMITTED（默认） | 不可能 | 可能 | 可能 |
| REPEATABLE READ | 不可能 | 不可能 | 可能 |
| SNAPSHOT | 不可能 | 不可能 | 不可能 |
| SERIALIZABLE | 不可能 | 不可能 | 不可能 |

```
⚠️ SQL Server 默认 READ COMMITTED（与 PG 一致、与 MySQL RR 不同）
  SNAPSHOT 是 SQL Server 最大亮点
  （基于行版本控制，读写互不阻塞）

⚠️ 面试必答：
"SQL Server 默认 RC + 可选 Snapshot 隔离——
 开启 RCSI 后读写不互斥，
 是 SQL Server 并发优化的第一手段。"
```

### 1.1 隔离级别实现差异（锁 vs 行版本）

| 级别 | 读实现 | 防幻读手段 | 开销 |
|------|--------|-----------|------|
| READ UNCOMMITTED | 不加锁（脏读） | — | 最低 |
| READ COMMITTED | 行级 S 锁（读完即放） | — | 低 |
| REPEATABLE READ | 行级 S 锁（持有到事务结束） | — | 中 |
| SNAPSHOT | 行版本（事务快照） | 更新冲突检测 | 中（tempdb） |
| SERIALIZABLE | Range 锁（键 + 范围） | ✅ 范围锁 | 高（阻塞最重） |

```
⚠️ 面试必答：
"SQL Server 的 RR 不防幻读（无间隙锁概念，
 要防幻读上 SERIALIZABLE 或 SNAPSHOT）；
 这点与 MySQL RR 默认防幻读不同——
 同样叫 RR，锁语义不同。"
```

---

## 2. Snapshot 隔离

### 2.1 RCSI（读提交快照隔离）

```sql
-- ① 开启 RCSI（最推荐的并发优化）
ALTER DATABASE MyDB SET READ_COMMITTED_SNAPSHOT ON;
-- ⚠️ 开启后：SELECT 不再被 UPDATE/DELETE 阻塞！

-- ② RCSI vs 普通 RC
--   普通 RC：SELECT 等写锁释放（阻塞）
--   RCSI：SELECT 读行版本快照（不阻塞）
--   代价：tempdb 维护行版本（轻微开销）
```

### 2.2 两种 Snapshot 对比

| 级别 | 数据源 | 一致性 |
|------|------|:---:|
| RCSI | 语句开始时 | 语句级 |
| Snapshot | 事务开始时 | 事务级 |

```sql
-- Snapshot 事务级（严格要求时）
SET TRANSACTION ISOLATION LEVEL SNAPSHOT;
-- ⚠️ 代价比 RCSI 大（需先开启 ALLOW_SNAPSHOT_ISOLATION）
```

> 🎯 **要点**：RCSI = SQL Server 并发的"一键优化"——开启后读写互不阻塞。与 PG 的 MVCC 理念相似但实现为 tempdb 行版本存储。

### 2.3 RCSI 原理与代价（源码级）

```
RCSI 工作原理：
  ① 开启后：UPDATE/DELETE 写行时在 tempdb 生成行版本
     （行头加 version_info 指针 → tempdb 版本存储）
  ② SELECT 读到被修改的行 → 顺指针读 tempdb 中的旧版本
  ③ 版本清理：无活动事务引用后，后台版本清理器回收

代价与注意：
  ① tempdb 空间：高更新率下版本存储增长快（需监控 tempdb）
  ② 快照无行锁 → 更新冲突时（并发改同行）返回错误：
     错误 3966（RC 快照过期）/ 3967（快照被其他事务修改）
  ③ 应用需捕获 3966/3967 并重试
```

```sql
-- 监控 tempdb 版本存储占用
SELECT SUM(version_store_reserved_page_count) * 8 / 1024 AS version_store_mb
FROM sys.dm_db_file_space_usage
WHERE database_id = 2;   -- tempdb

-- 查看版本清理滞后（长事务是元凶）
SELECT session_id, elapsed_time_seconds, status
FROM sys.dm_tran_active_snapshot_database_transactions
ORDER BY elapsed_time_seconds DESC;
```

---

## 3. 锁机制与锁升级

### 3.1 锁粒度与锁升级

```
SQL Server 锁粒度（自动选择）：
  RID/Key（行）→ Page（页）→ Table（表）
  默认从行锁开始，锁数量超阈值 → 升级到表锁

锁升级逃逸（Lock Escalation）：
  行锁数 ≥ 5000 → 自动升级为表锁
  → 可能导致阻塞范围扩大

⚠️ 面试必答：
"'锁升级'是 SQL Server 独有机制——
 行锁太多自动升表锁减少开销，
 但可能导致意外大范围阻塞。"
```

### 3.2 常见锁类型

| 锁 | 场景 | 阻塞 |
|------|------|:---:|
| S（共享） | SELECT | 不阻塞 S、阻塞 X |
| X（排他） | INSERT/UPDATE/DELETE | 阻塞全部 |
| U（更新） | UPDATE 前的读取 | 阻塞 X |
| IS/IX（意向） | 表级协调（自动） | 极少冲突 |

```sql
-- 查看当前锁
SELECT * FROM sys.dm_tran_locks;

-- 控制锁超时（默认无限等）
SET LOCK_TIMEOUT 5000;   -- 5 秒超时
```

### 3.3 锁兼容矩阵与意向锁（排查阻塞必备）

```
锁兼容矩阵（行 = 已持有，列 = 请求）：
          S    U    X   IS   IX
  S       ✅   ✅   ❌   ✅   ❌
  U       ✅   ❌   ❌   ✅   ❌
  X       ❌   ❌   ❌   ❌   ❌
  IS      ✅   ✅   ❌   ✅   ✅
  IX      ❌   ❌   ❌   ✅   ✅
```

```
意向锁的作用（IS/IX）：
  行锁之前先在表/页加意向锁 → 高层操作（如表锁、DDL）
  能快速判断"有没有行锁在手上"，不用逐行检查
  → 阻塞排查时：看到 IX 是正常的（行锁的"影子"）

排查阻塞三连：
  ① sys.dm_tran_locks：谁持有、等什么
  ② sys.dm_exec_requests + BLOCKING_SESSION_ID：谁被堵
  ③ sys.dm_exec_sql_text：被堵的 SQL 是什么
```

```sql
-- 阻塞会话定位（生产救火命令）
SELECT r.blocking_session_id, r.session_id,
       r.wait_type, r.wait_time, t.text AS blocked_sql
FROM sys.dm_exec_requests r
CROSS APPLY sys.dm_exec_sql_text(r.sql_handle) t
WHERE r.blocking_session_id > 0;
```

---

## 4. 死锁检测

### 4.1 死锁图

```
SQL Server 死锁：
  等待图检测（默认），选 deadlock priority 低者回滚
  图形化：SSMS → 扩展事件 → Deadlock Graph

⚠️ 比 MySQL 更强大：SQL Server 有 Deadlock Graph
  可视化死锁参与者的 SQL 语句与锁资源
```

### 4.1 死锁检测原理（源码级）

```
SQL Server 死锁检测（Lock Monitor）：
  ① 专用后台线程 Lock Monitor 周期扫描等待图
     （默认 5 秒一轮，检测到死锁可即时触发）
  ② 等待图成环 → 选"牺牲者"回滚：
     按 DEADLOCK_PRIORITY（低者先死）→ 同优先级比事务成本
  ③ 被牺牲会话收到错误 1205（Transaction was deadlocked）
     → 事务自动回滚 → 应用层必须捕获 1205 重试

扩展事件抓取死锁（替代 SSMS 手动看）：
  CREATE EVENT SESSION Deadlocks ON SERVER
  ADD EVENT sqlserver.lock_deadlock ...
```

```java
// Spring 捕获 1205 重试（SQL Server 专用）
@Retryable(retryFor = CannotAcquireLockException.class,
           exceptionExpression = "#{message.contains('1205')}",
           maxAttempts = 3)
public void transfer(int from, int to, BigDecimal amount) { ... }
```

```sql
-- 设置死锁优先级（数值越低越优先被回滚）
SET DEADLOCK_PRIORITY LOW;    -- 本会话优先被杀死
SET DEADLOCK_PRIORITY HIGH;   -- 本会话优先存活
```

### 4.2 预防措施

```
✅ 事务按相同顺序访问资源（防环）
✅ 开启 RCSI（SELECT 不加锁 → 减少死锁）
✅ 缩短事务（不在事务中做耗时操作）
✅ 合适索引（减少扫描范围 → 少加锁）
```

---

## 5. 乐观并发

### 5.1 ROWVERSION（版本号）

```sql
-- ROWVERSION = 自动递增的二进制版本号（每次行修改都更新）
CREATE TABLE products (
    id INT PRIMARY KEY,
    name NVARCHAR(50),
    price DECIMAL(10,2),
    rowver ROWVERSION              -- ⚠️ 乐观并发标记
);

-- 更新时校验版本
UPDATE products SET price = 99.99
WHERE id = 1 AND rowver = 0x0123;   -- 版本匹配才更新
-- ⚠️ 版本不匹配 → 0 行更新 → 被其他人改过
```

### 5.2 乐观并发完整实现（存储过程实战）

```sql
-- 完整乐观并发：读 → 校验 → 写（存储过程封装）
CREATE PROCEDURE usp_UpdatePrice
    @id INT, @newPrice DECIMAL(10,2), @expectedRowver ROWVERSION
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @updated INT;

    -- 版本校验更新（版本不匹配 → 0 行）
    UPDATE products SET price = @newPrice
    WHERE id = @id AND rowver = @expectedRowver;
    SET @updated = @@ROWCOUNT;

    -- 业务冲突处理
    IF @updated = 0
        THROW 51000, '数据已被他人修改，请刷新后重试', 1;
    RETURN @updated;
END;
```

### 5.3 乐观并发 vs 悲观并发

| 并发模型 | 机制 | 适用 |
|---------|------|------|
| 悲观 | SELECT 加锁（FOR UPDATE） | 高冲突 |
| 乐观 | 版本号/时间戳校验 | 低冲突 |
| RCSI | 行版本快照读 | 通用（推荐） |

> 🎯 **要点**：ROWVERSION + RCSI = SQL Server 乐观并发的标配组合——读不加锁 + 写校验版本。对比 MySQL（乐观锁需手动 `version INT`）、PG 的 xmin 可实现类似效果。

---

## 6. SQL Server 2025 并发增强

> 💡 SQL Server 2025（2025-11 发布）的锁与并发改进，2026 生产逐步落地。

### 6.1 Optimized Locking（优化锁）

```
SQL Server 2025 Optimized Locking（默认开启）：
  ① 锁内存大幅下降：元数据 + 事务级锁简化，
     不再为每行分配完整锁结构
  ② 阻塞减少：锁占用与等待显著降低
  ③ 兼容性：现有应用无需改动（默认行为）

⚠️ 注意：
  部分场景可回退（ALTER DATABASE 关闭 optimized locking）
  监控：sys.dm_tran_locks 视图仍可用（行为一致）
```

### 6.2 tempdb 与 Always On 并发增强

| 2025 增强 | 效果 |
|----------|------|
| tempdb 空间资源治理 | 防止版本存储抢占临时表空间 |
| 可选参数计划优化 | 参数化查询计划更稳定（防"计划抖动"） |
| Always On 更快故障转移 | 诊断 + 重连路径优化（容灾 RTO 缩短） |
| 自动执行计划修正 | 性能回归自动回退计划（Query Store 增强） |

> 🎯 **要点**：2025 的并发主线 = **Optimized Locking 默认开**（降锁开销）+ **tempdb 治理**（防版本存储膨胀）+ **计划自动修正**（稳定性兜底）——DBA 关注点从"调锁"转向"调版本存储与计划"。

---

> 🎯 **核心要点**：SQL Server 并发 = **RCSI**（tempdb 行版本，读写不互斥，第一推荐）+ **锁兼容矩阵与意向锁**（阻塞排查）+ **Lock Monitor 死锁检测**（1205 重试）+ **ROWVERSION 乐观并发**（版本校验）+ **2025 Optimized Locking**（锁内存/阻塞大降）。"RCSI 开启 + 索引优化 = SQL Server 并发基础优化"是企业 DBA 标准操作。

---

**返回总览**：[00-SQLServer总览与核心概念](00-SQLServer总览与核心概念.md) | **上一篇**：[02-SQLServer索引与性能](02-SQLServer索引与性能.md) | **下一篇**：[04-SQLServer与SpringBoot集成](04-SQLServer与SpringBoot集成.md)
