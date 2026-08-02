# 06 - PostgreSQL 性能优化与运维

> 定位：EXPLAIN 实战、VACUUM 调优、连接池、备份恢复、监控、升级——PG 的生产化运营

## 📚 目录

1. [EXPLAIN 执行计划分析](#1-explain-执行计划分析)
2. [VACUUM 调优](#2-vacuum-调优)
3. [连接与内存参数](#3-连接与内存参数)
4. [备份与恢复](#4-备份与恢复)
5. [监控与升级](#5-监控与升级)
6. [PG 面试题精选](#6-pg-面试题精选)

---

## 1. EXPLAIN 执行计划分析

### 1.1 EXPLAIN 解读

```sql
-- EXPLAIN ANALYZE：实际执行 + 统计（PG 18 默认含 BUFFERS）
EXPLAIN ANALYZE
SELECT u.name, COUNT(o.id)
FROM users u JOIN orders o ON u.id = o.user_id
WHERE u.status = 'active'
GROUP BY u.name;
```

```
关键字段：
  Seq Scan（全表扫描）→ ⚠️ 力争避免
  Index Scan（索引扫描）
  Bitmap Index Scan → Bitmap Heap Scan（位图扫描）
  Nested Loop / Hash Join / Merge Join（连接方式）
  cost=0.00..12.50（启动代价..总代价）
  rows=1000（预估行数）vs actual time（实际）
  Buffers: shared hit=5（PG 18 默认显示）

⚠️ 面试必答：
"EXPLAIN 三看——访问方式（Index/Seq/Bitmap）、
 连接算法（Nested/Hash/Merge）、
 rows 预估 vs 实际偏差（偏差大 = 统计信息过期）。"
```

### 1.2 PG 18 EXPLAIN 增强

```
① BUFFERS 默认显示（无需手动加）
② CTE 物化方式标记（Memory/Disk）
③ Index Searches 指标（索引树遍历次数）
④ 生产统计迁移到开发环境（pg_restore_*_stats）
```

---

## 2. VACUUM 调优

### 2.1 关键参数

```sql
-- Autovacuum 调优（防表膨胀）
ALTER TABLE large_table SET (
    autovacuum_vacuum_scale_factor = 0.05,     -- 5% 死元组就触发（大表降低）
    autovacuum_vacuum_threshold = 1000,
    autovacuum_analyze_scale_factor = 0.02     -- 更频繁 ANALYZE
);

-- PG 18：大表附加 cap 防等太久
autovacuum_vacuum_max_threshold = 100000000    -- 死元组 ≥ 1 亿绝对触发
```

### 2.2 VACUUM 监控

```sql
-- 查看死元组情况
SELECT schemaname, relname, n_dead_tup, n_live_tup,
       last_vacuum, last_autovacuum
FROM pg_stat_user_tables
WHERE n_dead_tup > 1000
ORDER BY n_dead_tup DESC;
-- ⚠️ 死元组持续增长 → Autovacuum 跟不上（调参或手动 VACUUM）
```

> 🎯 **要点**：VACUUM 调优的核心 = 大表降低 scale_factor（防等太久）+ PG 18 的 max_threshold 兜底。死元组监控是 PG DBA 的基本功。

---

## 3. 连接与内存参数

### 3.1 关键参数

```sql
-- 内存分配（默认偏保守，生产必调）
shared_buffers = 4GB          -- ⚠️ 物理内存 25%（PG 推荐）
work_mem = 64MB               -- 单操作排序/哈希内存（大表查询调大）
maintenance_work_mem = 512MB  -- VACUUM/CREATE INDEX 内存
effective_cache_size = 12GB   -- 操作系统缓存估算（指点优化器）

-- 连接
max_connections = 200
-- ⚠️ 使用连接池（PgBouncer）管理，不要直连 PG 全部连接
```

### 3.2 PG 性能调优公式

```
shared_buffers = 物理内存 × 25%（PG 官方推荐）
effective_cache_size = 物理内存 × 75%
work_mem = (空闲内存 / max_connections) / 4

⚠️ 使用 PgBouncer（连接池）+ 应用侧 HikariCP 双层连接管理
```

---

## 4. 备份与恢复

### 4.1 备份方案

| 方式 | 命令 | 特点 |
|------|------|------|
| pg_dump | `pg_dump db > backup.sql` | 逻辑备份（可跨版本） |
| pg_basebackup | `pg_basebackup -D /backup` | 物理全量 |
| WAL 归档 | `archive_mode = on` | PITR 时间点恢复 |
| pgBackRest | 第三方工具 | 企业级（增量/并行/加密） |

```bash
# 生产备份策略
# 每日 pg_dump（逻辑） + 持续 WAL 归档（PITR）
pg_dump -h localhost -U postgres -Fc mydb > mydb_$(date +%Y%m%d).dump
```

### 4.2 PITR（时间点恢复）

```sql
-- PITR = WAL 归档 + 基础备份
-- 可恢复到任意时间点（误删数据后回退）

-- 配置
archive_mode = on
archive_command = 'cp %p /archive/%f'
```

---

## 5. 监控与升级

### 5.1 监控视图

```sql
-- pg_stat_activity：当前连接/查询
SELECT pid, state, query, now() - query_start AS duration
FROM pg_stat_activity
WHERE state != 'idle' AND query NOT LIKE '%pg_stat%'
ORDER BY duration DESC;

-- pg_stat_user_tables：表级统计（读写/死元组）
-- pg_stat_statements：SQL 性能统计（需要 CREATE EXTENSION）
-- pg_locks：锁等待（死锁排查）
```

### 5.2 PG 18 升级

```bash
# pg_upgrade：原地升级（免 pg_dump 恢复）
# PG 18 新增 swap 模式（更快）
/usr/lib/postgresql/18/bin/pg_upgrade \
    --old-datadir=/var/lib/postgresql/17/main \
    --new-datadir=/var/lib/postgresql/18/main \
    --old-bindir=/usr/lib/postgresql/17/bin \
    --new-bindir=/usr/lib/postgresql/18/bin \
    --link                           # 硬链接（快，不能回滚）
```

---

## 6. PG 面试题精选

**Q1: PG 和 MySQL 的最大区别？**
```
PG：多版本存储（VACUUM 回收）+ 六种索引 + 扩展最强（pgvector/PostGIS）；
MySQL：undo 回滚段 + 运维简单 + 互联网应用最广。
```

**Q2: VACUUM 是做什么的？不 VACUUM 会怎样？**
```
回收死元组（更新/删除后的旧版本）；不 VACUUM →
表膨胀 + 索引膨胀 + 全表扫描变慢 + 事务 ID 回卷。
Autovacuum 是默认守护进程。
```

**Q3: PG 有哪些索引类型？**
```
B-tree（通用）、GIN（全文/数组/JSONB）、
GiST（几何）、BRIN（超大表）、Hash（等值）、
向量（pgvector ANN）。
```

**Q4: JSONB 和 JSON 的区别？**
```
JSONB 二进制存储（可索引/查询快）、JSON 文本（原样保留）。
生产一律 JSONB + GIN 索引。
```

**Q5: PG 18 新特性？**
```
AIO 异步 IO（快 3×）、UUID v7（有序）、
B-tree skip scan（快 21×）、OAuth2、
虚拟生成列、Autovacuum max_threshold。
```

**Q6: pgvector 的定位？**
```
中小规模向量检索（百万级）零额外部署；
结构化过滤 + 向量排序一条 SQL；
大规模用专用库（Milvus）。
```

**Q7: 读写分离怎么做？**
```
流复制（SR）+ Pgpool-II 中间件 或应用层 @DS 注解；
PG 逻辑复制比 MySQL binlog 更灵活（可选择复制）。
```

**Q8: 长事务的危害？**
```
阻止 VACUUM 回收 → 表膨胀加速 → 事务 ID 回卷风险。
解决：监控长事务 + idle_in_transaction_session_timeout。
```

---

> 🎯 **核心要点**：PG 运维体系 = **EXPLAIN 三看**（访问/连接/偏差）+ **VACUUM 调优**（大表 scale_factor + max_threshold）+ **参数调优**（shared_buffers 25% + work_mem）+ **备份**（pg_dump + WAL PITR）+ **监控**（pg_stat_* 视图）+ **升级**（pg_upgrade link）。"VACUUM 是 PG 的生命线，监控死元组是 DBA 基本功"。

---

**返回总览**：[00-PostgreSQL总览与核心概念](00-PostgreSQL总览与核心概念.md) | **上一篇**：[05-PostgreSQL与SpringBoot集成](05-PostgreSQL与SpringBoot集成.md)
