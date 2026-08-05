# 02 - SQL Server 索引与性能

> 定位：聚集/非聚集索引设计与 INCLUDE 覆盖索引、执行计划深入分析、统计信息与直方图、查询存储（Query Store）、缺失索引与索引碎片——SQL Server 性能优化完整体系

## 📚 目录

1. [聚集索引 vs 非聚集索引设计](#1-聚集索引-vs-非聚集索引设计)
2. [INCLUDE 与过滤索引](#2-include-与过滤索引)
3. [执行计划分析](#3-执行计划分析)
4. [统计信息与直方图](#4-统计信息与直方图)
5. [查询存储（Query Store）](#5-查询存储query-store)
6. [缺失索引与索引碎片](#6-缺失索引与索引碎片)

---

## 1. 聚集索引 vs 非聚集索引设计

### 1.1 结构对比

```
聚集索引（Clustered Index）：
  叶子层 = 数据页（表数据按索引键物理排序）
  每表只能 1 个（默认主键即聚集索引）
  查找数据最快（无需回表）

非聚集索引（Nonclustered Index）：
  叶子层 = 索引键 + 聚集键（或 RID）
  每表最多 999 个
  查找需回表（Key Lookup）
```

| 维度 | 聚集索引 | 非聚集索引 |
|------|:---:|:---:|
| 叶子层 | 数据行 | 索引键+聚集键 |
| 数量 | 1 | 999 |
| 物理排序 | 表数据按它排 | 逻辑有序 |
| 查找 | 直接定位 | 回表 |
| 主键默认 | 聚集 | 可指定 NONCLUSTERED |

### 1.2 聚集键选择

```sql
-- ✅ 最佳聚集键：窄、静态、递增
CREATE TABLE users (
    id INT IDENTITY PRIMARY KEY,      -- ✅ 窄+静态+递增
    name NVARCHAR(50)
);

-- ⚠️ 避免：宽列/频繁更新/随机值作聚集键
CREATE TABLE orders (
    order_id UNIQUEIDENTIFIER PRIMARY KEY,  -- ❌ 随机 → 页分裂严重
    ...
);
```

```
⚠️ 聚集键不好的后果：
  随机键 → 频繁页分裂 + 索引碎片
  宽键 → 非聚集索引叶子变大（每行重复存储）
  更新频繁 → 行迁移开销

⚠️ 面试必答：
"聚集键三原则——窄（少字节）、
 静态（不变）、递增（追加插入无分裂）；
 默认主键 IDENTITY 是最佳实践；
 GUID 主键是 SQL Server 常见性能坑。"
```

### 1.3 非聚集索引使用场景

```sql
-- ① 高频 WHERE 列
CREATE NONCLUSTERED INDEX IX_Users_Email ON users(email);

-- ② 高频 JOIN 列
CREATE NONCLUSTERED INDEX IX_Orders_UserID ON orders(user_id);

-- ③ 排序/分组列
CREATE NONCLUSTERED INDEX IX_Orders_Created ON orders(created_at);
-- 索引天然有序 → ORDER BY / GROUP BY 免排序
```

---

## 2. INCLUDE 与过滤索引

### 2.1 INCLUDE（包含列）

```sql
-- ⚠️ INCLUDE = 把查询常用但不过滤的列放索引叶
-- 目的：免回表（Key Lookup 变 Index Seek）
CREATE NONCLUSTERED INDEX IX_Orders_Cover
    ON orders(user_id, status)           -- 键列（过滤/排序用）
    INCLUDE (total_amount, created_at);  -- 非键列（查询取用）

-- 查询：SELECT total_amount, created_at FROM orders WHERE user_id=1 AND status='A'
-- → 完全走索引（无需访问数据页）
```

### 2.2 INCLUDE vs 键列

| 维度 | 键列（键） | INCLUDE（包含列） |
|------|:---:|:---:|
| 过滤/排序 | ✅ | ❌ |
| 查询取值 | ✅ | ✅ |
| 索引体积 | 大（排序影响） | 小（追加叶） |
| 数量限制 | 16 列 / 900 字节 | 1023 列 |
| 适用 | 过滤/排序 | 免回表取数 |

```
⚠️ 面试必答：
"INCLUDE 列 = '查询要但不过滤'的列——
 放进索引叶避免回表；
 键列是过滤/排序用、INCLUDE 是取值用，
 两者组合 = 覆盖索引。"
```

### 2.3 过滤索引（Filtered Index）

```sql
-- 只索引满足条件的行（90% 查询只查活跃用户 → 索引小一半）
CREATE NONCLUSTERED INDEX IX_Users_Active
    ON users(email)
    WHERE status = 'active';          -- ⚠️ 过滤条件

-- 查询必须带相同条件才能用
SELECT * FROM users WHERE email='a@b.com' AND status='active';
```

---

## 3. 执行计划分析

### 3.1 关键操作符

| 操作符 | 含义 | 性能 |
|--------|------|:---:|
| Index Seek | 索引查找（WHERE 命中） | ✅ 最优 |
| Clustered Index Seek | 聚集索引直接定位 | ✅ 最优 |
| Index Scan | 索引全扫（无 WHERE 命中） | ⚠️ |
| Table Scan | 表全扫（无索引） | ❌ 红线 |
| Key Lookup | 非聚集索引回表 | ⚠️ 大量需覆盖 |
| RID Lookup | 堆表回表（无聚集） | ⚠️ |
| Nested Loops | 小表驱动大表循环 | ✅ 小集 |
| Hash Match | 哈希连接（大集） | ✅ 大集 |
| Merge Join | 有序连接 | ✅ 有序输入 |
| Sort | 排序（ORDER BY） | ⚠️ 加索引免 |
| Spool | 中间结果缓存 | 视场景 |

### 3.2 执行计划获取方式

```sql
-- ① 图形化：SSMS Ctrl+M（实际执行）
-- ② 文本统计
SET STATISTICS IO ON;
SET STATISTICS TIME ON;
SELECT * FROM orders WHERE user_id = 1;
-- 输出：扫描计数/逻辑读取/CPU 时间

-- ③ 预估计划（不执行）
SET SHOWPLAN_ALL ON;

-- ④ 历史计划（Query Store / Plan Cache）
SELECT * FROM sys.dm_exec_query_stats;
```

### 3.3 执行计划优化实战

```
优化流程：
  ① 找慢查询（Query Store/慢日志）
  ② EXPLAIN 看访问方式（Seek vs Scan）
  ③ Key Lookup 多 → 加 INCLUDE（覆盖索引）
  ④ Scan → 缺索引（看 Missing Index 建议）
  ⑤ 排序/临时表 → 加索引免排序

⚠️ 面试必答：
"执行计划三看——访问方式（Seek 好/Scan 差）、
 连接算法（Nested 小集/Hash 大集）、
 Key Lookup 数量（多 → 覆盖索引）；
 优化口诀：'先 Seek 后覆盖再统计'。"
```

---

## 4. 统计信息与直方图

### 4.1 统计信息是什么

```
统计信息 = 列值分布的直方图（每列最多 200 个 bucket）
  优化器用它估算行数、决定索引使用

自动更新（Auto Update Statistics）：
  表数据变化 > 20%（或 500 行+20%）→ 触发更新
  自动创建（Auto Create Statistics）：
  查询用了无统计的列 → 自动创建

⚠️ 面试必答：
"统计信息 = 优化器的'眼睛'——
 直方图估算行数、判断索引价值；
 统计过期 = 优化器选错执行计划 = 查询突然变慢。"
```

### 4.2 统计信息管理

```sql
-- 查看统计信息及更新时间
SELECT s.name AS stats_name,
       stats_date(s.object_id, s.stats_id) AS last_updated,
       sp.rows_sampled, sp.rows
FROM sys.stats s
CROSS APPLY sys.dm_db_stats_properties(s.object_id, s.stats_id) sp
WHERE s.object_id = OBJECT_ID('Orders');

-- 手动更新（大表变更后）
UPDATE STATISTICS Orders;
UPDATE STATISTICS Orders WITH FULLSCAN;      -- 全量扫描（精确）

-- 更新全部表
EXEC sp_updatestats;
```

### 4.3 统计信息过期排查

```
症状：同一查询以前快、现在慢（或反之）
排查：
  ① 看统计信息 last_updated
  ② 看预估行数 vs 实际行数（执行计划偏差大）
  ③ 强制更新统计（临时修复）
  ④ 长期方案：维护计划定期更新 / 异步统计

⚠️ 面试必答：
"查询突然变慢三查——统计过期？
 索引碎片？数据量变化（增长/倾斜）？
 统计是最常被忽略的'隐形杀手'。"
```

---

## 5. 查询存储（Query Store）

### 5.1 是什么与为什么

```
Query Store = 查询性能的"飞行记录仪"（2016+ 独有）

解决的问题：
  ① 升级后查询变慢（新执行计划回归）
  ② 慢查询难以回溯（历史无记录）
  ③ 参数嗅探导致的计划不稳定

能力：
  记录每查询的执行计划 + 运行时统计
  历史对比（周/月性能趋势）
  强制最优计划（Plan Forcing）

⚠️ 面试必答：
"Query Store 是 SQL Server 独有优势——
 记录查询执行历史、检测性能回归、
 可强制计划；
 MySQL/PG 无同类内置（需第三方监控）。"
```

### 5.2 配置与使用

```sql
-- ① 开启
ALTER DATABASE MyDB SET QUERY_STORE = ON
    (OPERATION_MODE = READ_WRITE,
     MAX_STORAGE_SIZE_MB = 100,
     INTERVAL_LENGTH_MINUTES = 15);

-- ② 查询变慢的查询（按执行时间排序）
SELECT TOP 10 qt.query_id,
       MAX(rs.avg_duration) AS max_duration,
       COUNT(rs.count_executions) AS exec_count
FROM sys.query_store_query q
JOIN sys.query_store_plan p ON q.query_id = p.query_id
JOIN sys.query_store_runtime_stats rs ON p.plan_id = rs.plan_id
JOIN sys.query_store_query_text qt ON q.query_text_id = qt.query_text_id
GROUP BY qt.query_id
ORDER BY max_duration DESC;

-- ③ 强制计划（防止回归）
EXEC sp_query_store_force_plan @query_id = 1, @plan_id = 2;
```

---

## 6. 缺失索引与索引碎片

### 6.1 缺失索引建议

```sql
-- SQL Server 自动记录优化器的缺失索引建议
SELECT mig.index_group_handle,
       migs.avg_user_impact,          -- 预估提升百分比
       migs.avg_total_user_cost,      -- 当前平均成本
       mid.statement AS table_name,
       mid.equality_columns,          -- 等值列
       mid.inequality_columns,        -- 范围列
       mid.included_columns           -- 建议 INCLUDE 列
FROM sys.dm_db_missing_index_group_stats migs
JOIN sys.dm_db_missing_index_groups mig
    ON migs.group_handle = mig.index_group_handle
JOIN sys.dm_db_missing_index_details mid
    ON mig.index_handle = mid.index_handle
ORDER BY avg_user_impact DESC;
-- ⚠️ 按 avg_user_impact 降序创建（收益最高优先）
```

### 6.2 索引碎片与维护

```sql
-- 查看碎片率
SELECT OBJECT_NAME(ind.object_id) AS table_name,
       ind.name AS index_name,
       avg_fragmentation_in_percent AS frag_pct,
       page_count
FROM sys.dm_db_index_physical_stats(
    DB_ID(), NULL, NULL, NULL, 'LIMITED') ips
JOIN sys.indexes ind
    ON ips.object_id = ind.object_id
    AND ips.index_id = ind.index_id
WHERE avg_fragmentation_in_percent > 5
ORDER BY frag_pct DESC;

-- 维护策略：
--   碎片 5-30% → REORGANIZE（重组，在线）
--   碎片 > 30% → REBUILD（重建，可 OFFLINE）
ALTER INDEX IX_Users_Email ON users REORGANIZE;
ALTER INDEX IX_Orders_Cover ON orders REBUILD WITH (ONLINE = ON);
```

### 6.3 索引维护最佳实践

```
✅ 定期维护（每周）：重组/重建 + 更新统计
✅ 碎片 > 30% 重建（ONLINE=ON 不停机）
✅ 索引越多写越慢（权衡读写比）
✅ 大表重建用 ONLINE（避免阻塞）
⚠️ 辅助副本上维护索引（Always On 特性）减少主库负载
```

---

> 🎯 **核心要点**：SQL Server 索引性能 = **聚集键三原则**（窄/静态/递增）+ **INCLUDE 覆盖索引**（免回表）+ **执行计划三看**（Seek/Join/Key Lookup）+ **统计信息**（直方图是优化器眼睛）+ **Query Store**（独有性能记录仪）+ **碎片维护**（>30% 重建 ONLINE）。"覆盖索引 + 统计 + Query Store"是 SQL Server 调优三大件。

---

**返回总览**：[00-SQLServer总览与核心概念](00-SQLServer总览与核心概念.md) | **上一篇**：[01-T-SQL进阶特性](01-T-SQL进阶特性.md) | **下一篇**：[03-SQLServer事务与并发控制](03-SQLServer事务与并发控制.md)
