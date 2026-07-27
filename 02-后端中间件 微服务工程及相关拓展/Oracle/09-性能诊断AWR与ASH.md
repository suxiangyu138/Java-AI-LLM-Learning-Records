# 09 - 性能诊断：AWR 与 ASH

> 🎯 AWR 是 Oracle 的"体检报告"——自动采集性能指标生成诊断报告。ASH 是"心电图"——每秒采样当前正在执行的 SQL。两者结合 = Oracle 性能诊断的完整武器库

---

## 目录

1. [AWR 自动负载仓库](#1-awr-自动负载仓库)
2. [ASH 活动会话历史](#2-ash-活动会话历史)
3. [实战诊断流程](#3-实战诊断流程)

---

## 1. AWR 自动负载仓库

```sql
-- 生成 AWR 报告（核心诊断工具）
@?/rdbms/admin/awrrpt.sql
-- 输入: 报告类型(HTML), 快照间隔, 输出文件名

-- 手动创建快照
EXEC DBMS_WORKLOAD_REPOSITORY.CREATE_SNAPSHOT;

-- 查看快照列表
SELECT snap_id, begin_interval_time, end_interval_time
FROM dba_hist_snapshot ORDER BY snap_id DESC;

-- AWR 保留策略
SELECT retention FROM dba_hist_wr_control;  -- 默认保留 8 天
EXEC DBMS_WORKLOAD_REPOSITORY.MODIFY_SNAPSHOT_SETTINGS(retention => 43200); -- 30天
```

### AWR 报告关键指标

```text
AWR 报告核心章节（从最重要开始看）：

1. Top 10 Foreground Events → 看什么等待最多
   db file sequential read → 索引读取（正常）
   db file scattered read  → 全表扫描（⚠️ 可能有问题）
   log file sync           → 提交太频繁（⚠️）
   enq: TX - row lock contention → 行锁争用（🔴 严重）

2. SQL ordered by Elapsed Time → 最慢的 SQL
   → 优化 Top 5 通常解决 80% 的性能问题

3. Instance Efficiency Percentages
   Buffer Hit % > 95% → 正常
   Library Hit % > 95% → 正常

4. Top Segments → 最热的表/索引
```

## 2. ASH 活动会话历史

```sql
-- ASH = 每秒采样，比 AWR 更精细（AWR 是小时级）
-- 查看当前活跃会话
SELECT sample_time, session_id, sql_id, event, blocking_session
FROM v$active_session_history
WHERE sample_time > SYSDATE - 5/1440   -- 最近 5 分钟
ORDER BY sample_time DESC;

-- 过去 1 小时 Top SQL
SELECT sql_id,
       COUNT(*) AS samples,
       COUNT(*) * 10 AS estimated_seconds
FROM v$active_session_history
WHERE sample_time > SYSDATE - 1/24
GROUP BY sql_id ORDER BY samples DESC;
```

## 3. 实战诊断流程

```text
生产环境变慢 → 三步诊断：

Step 1: 现在有什么在跑？
  SELECT sid, sql_id, event, status, seconds_in_wait
  FROM v$session WHERE status = 'ACTIVE' AND type != 'BACKGROUND';

Step 2: 过去 15 分钟有什么问题？
  SELECT event, COUNT(*), COUNT(*)*10 AS total_seconds
  FROM v$active_session_history
  WHERE sample_time > SYSDATE - 15/1440
  GROUP BY event ORDER BY COUNT(*) DESC;

Step 3: 哪个 SQL 是罪魁祸首？
  SELECT sql_id, COUNT(*),
         (SELECT sql_text FROM v$sql s WHERE s.sql_id = a.sql_id AND ROWNUM=1) AS sql
  FROM v$active_session_history a
  WHERE sample_time > SYSDATE - 1/24
  GROUP BY sql_id HAVING COUNT(*) > 10 ORDER BY COUNT(*) DESC;
```

```sql
-- 快速诊断包
-- 1. 当前锁等待
SELECT sid, blocking_session, event, seconds_in_wait
FROM v$session WHERE blocking_session IS NOT NULL;

-- 2. 长时间运行 SQL
SELECT sid, sql_id, elapsed_time/1e6 AS sec, sql_text
FROM v$sql_monitor WHERE status = 'EXECUTING';

-- 3. 表空间即将满
SELECT tablespace_name, ROUND(used_percent,1) pct
FROM dba_tablespace_usage_metrics WHERE used_percent > 80;
```

## 核心要点回顾

- AWR = 小时级的性能快照（"体检报告"）
- ASH = 秒级的活动采样（"心电图"）
- Top 5 SQL 优化 = 解决 80% 性能问题
- `db file scattered read` = 全表扫描（关注）
- `enq: TX - row lock contention` = 行锁（立即关注）
- `v$active_session_history` 是最常用的诊断视图

## 参考资料

1. Oracle Database Performance Tuning Guide
