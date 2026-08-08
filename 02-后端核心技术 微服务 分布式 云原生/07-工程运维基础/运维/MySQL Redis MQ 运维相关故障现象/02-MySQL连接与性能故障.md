# 02-MySQL 连接与性能故障
> 连接数满、慢查询、CPU 高、连接泄漏——"数据库最先挂的四个场景"的现象、命令与解决

## 📚 目录
1. [故障一：连接数满（Too many connections）](#1-故障一连接数满too-many-connections)
2. [故障二：慢查询](#2-故障二慢查询)
3. [故障三：CPU 飙高](#3-故障三cpu-飙高)
4. [故障四：连接泄漏](#4-故障四连接泄漏)
5. [综合排查命令集](#5-综合排查命令集)
6. [核心要点](#6-核心要点)
7. [参考来源](#7-参考来源)

## 1. 故障一：连接数满（Too many connections）

### 1.1 现象与定位

```text
现象：应用报 "Too many connections" / "Connection is not available"
```

```sql
-- 当前连接数
SHOW STATUS LIKE 'Threads_connected';
-- 最大连接数
SHOW VARIABLES LIKE 'max_connections';       -- 默认 151
-- 连接来源分布
SELECT user, host, COUNT(*) FROM information_schema.processlist GROUP BY user, host;
```

```bash
# 服务端视角：谁在连
ss -tnp | grep 3306 | wc -l
```

### 1.2 根因矩阵

| 根因 | 特征 | 解决 |
|------|------|------|
| **连接泄漏** | 连接数持续爬升不回 | 查代码（连接池未归还）——见故障四 |
| 慢查询占连接 | 大量慢 SQL 长时间持连接 | 优化 SQL（见故障二） |
| 连接池过大 | 多应用池总和超上限 | 池参数合理化 |
| 突发流量 | 曲线陡升 | 临时调大 max_connections + 限流 |
| 连接风暴 | 应用重启集体建连 | 池预热/错峰 |

> ⚠️ **调大 max_connections 是缓解不是根治**：默认 151 是保守值，但**连接数满的本质是"连接被占着不还"**——先查泄漏与慢查询，再考虑扩容。

## 2. 故障二：慢查询

### 2.1 定位

```sql
-- 开启慢日志
SET GLOBAL slow_query_log = ON;
SET GLOBAL long_query_time = 1;        -- 超过 1 秒记录
-- 慢日志位置
SHOW VARIABLES LIKE 'slow_query_log_file';

-- 正在执行的查询（现场）
SELECT * FROM information_schema.processlist
WHERE command != 'Sleep' ORDER BY time DESC;

-- 锁等待的查询
SELECT * FROM information_schema.processlist
WHERE state LIKE '%lock%';
```

```bash
# 慢日志分析工具
pt-query-digest /var/log/mysql/slow.log | head -50   # 慢查询聚合
```

### 2.2 慢 SQL 优化路径

```text
拿到慢 SQL → EXPLAIN 看执行计划：
  type：ALL（全表扫描）→ 优化索引
  rows：扫描行数过大 → 索引/分页优化
  Extra：Using filesort / Using temporary → 排序/分组优化
  key：NULL → 缺索引
```

```sql
EXPLAIN SELECT * FROM orders WHERE user_id = 123 AND status = 'PAID';
-- 看 type/key/rows 三列
```

| 慢查询根因 | 优化手段 |
|-----------|---------|
| 缺索引/索引失效 | 建索引（复合索引最左前缀） |
| 全表扫描 | 索引 + 覆盖索引 |
| 深分页 | 游标分页（WHERE id > ? LIMIT） |
| 大表 join | 拆分/冗余字段 |
| 函数导致索引失效 | 避免 `WHERE DATE(col) = ...` |
| 锁等待 | 见 [03](03-MySQL锁与复制故障.md) |

> 🎯 **慢查询优化主线**：EXPLAIN 三列（type/rows/key）→ 索引/分页/改写——**90% 的慢 SQL 是缺索引或写法问题**。

## 3. 故障三：CPU 飙高

### 3.1 定位顺序

```text
① 确认是 MySQL 的 CPU（top 看进程）
② 连接数是否正常（异常连接风暴？）
③ 慢查询是否激增（慢日志时间点）
④ 找出高消耗 SQL（processlist 快照）
```

```sql
-- 抓取当前高消耗查询（连续快照对比）
SELECT id, user, time, state, LEFT(info, 100)
FROM information_schema.processlist
WHERE command != 'Sleep' ORDER BY time DESC;
```

### 3.2 根因矩阵

| 根因 | 特征 | 解决 |
|------|------|------|
| 慢查询并发 | 多个慢 SQL 同时跑 | 优化 SQL/索引 |
| 无索引全表扫 | 单条 SQL 扫描千万行 | 加索引 |
| 连接风暴 | 连接数陡升 + CPU 飙 | 错峰/池预热 |
| 大事务 | 长事务 + 锁等待 | 拆分事务 |
| 备份/分析任务 | 定时任务撞车 | 错峰调度 |

> 💡 **CPU 高的排查要点**：CPU 是"结果"，慢 SQL/无索引/风暴是"原因"——**抓 processlist 快照是定位高消耗 SQL 的第一现场**。

## 4. 故障四：连接泄漏

### 4.1 现象

```text
现象：连接数缓慢爬升，应用重启后回落，再次爬升
特征：泄漏 = 借了不还（应用侧问题，不是 MySQL 问题）
```

```sql
-- 连接在干什么（Sleep 多 = 池中空闲？还是泄漏堆积？）
SELECT command, COUNT(*) FROM information_schema.processlist GROUP BY command;
-- Sleep 大量堆积 + Threads_connected 爬升 = 泄漏嫌疑
```

### 4.2 Java 侧排查（联动网络问题排查体系）

```text
泄漏排查路径：
  ① MySQL 侧：Threads_connected 趋势（爬升 = 泄漏）
  ② 应用侧：连接池监控（HikariCP active/idle 曲线）
  ③ 代码：连接未关闭/事务未提交/流未释放

防泄漏三板斧：
  try-with-resources 关闭连接
  事务 finally 提交/回滚
  连接池 max-lifetime/validation（复用前验证）
```

> ⚠️ **泄漏的本质**：连接"借了不还"——MySQL 只是受害者。**连接池监控（active 持续占满）是早期信号**，等 MySQL 连接数满已经晚了。

## 5. 综合排查命令集

```bash
# 快速健康检查
mysqladmin status                    # Uptime/Threads/QPS
mysqladmin processlist               # 当前连接

# 核心指标
SHOW GLOBAL STATUS LIKE 'Threads_connected';
SHOW GLOBAL STATUS LIKE 'Max_used_connections';   # 历史峰值（评估扩容）
SHOW GLOBAL STATUS LIKE 'Questions';              # QPS 计算
SHOW GLOBAL STATUS LIKE 'Slow_queries';
SHOW VARIABLES LIKE 'max_connections';
SHOW VARIABLES LIKE 'long_query_time';
```

```sql
-- 锁相关
SHOW STATUS LIKE 'Table_locks_waited';
SHOW STATUS LIKE 'Innodb_row_lock_current_waits';
SHOW ENGINE INNODB STATUS\G          -- 事务/锁全景
```

| 场景 | 命令 |
|------|------|
| 连接数 | `SHOW STATUS LIKE 'Threads_connected'` |
| 峰值评估 | `Max_used_connections` vs `max_connections` |
| 当前 SQL | `information_schema.processlist` |
| 锁状态 | `SHOW ENGINE INNODB STATUS` |
| 慢日志 | slow_query_log 开启 + pt-query-digest |

## 6. 核心要点

> 🎯 **核心要点**：
> - 连接数满三查：Threads_connected 趋势 + processlist 分布 + max_connections 对比；
> - 连接数满的本质是"占着不还"：先查泄漏与慢查询，调参是缓解；
> - 慢查询主线：EXPLAIN 三列（type/rows/key）→ 索引/分页/改写；
> - CPU 高是结果：processlist 快照抓高消耗 SQL（连续快照对比）；
> - 泄漏信号：Sleep 堆积 + 连接爬升 + 应用池 active 占满；
> - 综合命令集：mysqladmin status + processlist + INNODB STATUS 四件套。

## 7. 参考来源

- [MySQL 8.4 官方文档：连接与线程](https://dev.mysql.com/doc/refman/8.4/en/connection-threads.html)
- [MySQL 慢查询日志文档](https://dev.mysql.com/doc/refman/8.4/en/slow-query-log.html)
- [pt-query-digest（Percona Toolkit）](https://docs.percona.com/percona-toolkit/pt-query-digest.html)

---

**下一模块**：[03-MySQL锁与复制故障](03-MySQL锁与复制故障.md)　/　**返回总览**：[00-总览](00-中间件运维故障排查总览.md)
