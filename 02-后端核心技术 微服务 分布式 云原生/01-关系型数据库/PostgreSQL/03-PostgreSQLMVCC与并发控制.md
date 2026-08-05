# 03 - PostgreSQL MVCC 与并发控制

> 定位：PG 独有的 MVCC 实现——元组版本链（无回滚段）、VACUUM 死元组清理、隔离级别、锁体系——与 MySQL 最大的底层差异

## 📚 目录

1. [MVCC 实现对比（PG vs MySQL）](#1-mvcc-实现对比pg-vs-mysql)
2. [元组版本链与可见性](#2-元组版本链与可见性)
3. [VACUUM 体系](#3-vacuum-体系)
4. [隔离级别](#4-隔离级别)
5. [锁机制](#5-锁机制)

---

## 1. MVCC 实现对比（PG vs MySQL）

```
PG 的 MVCC：多版本存储（同一行有多个历史版本）
  更新 = INSERT 新版本 + 标记旧版本
  旧版本留在数据页中（VACUUM 回收）

MySQL 的 MVCC：undo log 回滚段
  更新 = 修改数据 + undo log 记录旧值
  旧值在独立回滚段

⚠️ 面试必答：
"PG MVCC = 多版本存在表空间（VACUUM 回收）；
 MySQL MVCC = undo log 版本链；
 PG 无回滚段 → 不会出现'undo log 膨胀'问题，
 但需要 VACUUM 清理死元组。"
```

| 维度 | PG | MySQL |
|------|:---:|:---:|
| 旧版本位置 | 表空间内 | undo 表空间 |
| 回收机制 | VACUUM | 事务提交回滚段自动清理 |
| 回滚方式 | 直接读旧版本 | undo 回滚 |
| 膨胀问题 | 表膨胀（需 VACUUM） | undo 膨胀 |
| 长事务影响 | 阻止 VACUUM 回收 | undo 持续增长 |

---

## 2. 元组版本链与可见性

### 2.1 隐藏列

```
PG 每行隐藏字段（系统列）：
  xmin：插入此版本的事务 ID
  xmax：删除/更新此版本的事务 ID
  ctid：物理位置（页内偏移）

可见性规则：
  版本可见 ⟺ xmin 已提交且（xmax 为空或 xmax 未提交）
  → 事务只能看到提交时间在自己快照之前的版本

⚠️ 面试必答：
"PG 元组版本链 = xmin/xmax 标记 +
 ctid 物理定位——可见性靠事务快照判断
 （非 undo 回滚段串链）。"
```

### 2.2 HOT 更新（性能关键优化）

```
HOT（Heap Only Tuple）更新：
  更新时若索引列未变 → 新版本放在旧版本同页内
  通过「页内指针」链到新版本（不新增索引条目）

好处：
  ① 免索引更新（最贵的写路径省掉）
  ② 减少索引膨胀

代价：
  ① 索引列被更新的行 → 无法 HOT（必须新索引条目）
  ② 页面空间不足 → 回退普通更新

⚠️ 面试必答：
"HOT = 索引列不变时同页内更新，
 免索引写 + 防索引膨胀；
 高频更新场景检查 n_tup_hot_upd 占比。"
```

```sql
-- 检查 HOT 命中率（n_tup_hot_upd / n_tup_upd 高 = 索引设计合理）
SELECT relname, n_tup_upd, n_tup_hot_upd,
       round(100.0 * n_tup_hot_upd / NULLIF(n_tup_upd,0), 1) AS hot_pct
FROM pg_stat_user_tables ORDER BY n_tup_upd DESC;
```

### 2.3 快照隔离

```
PG 事务快照（Snapshot）：
  记录事务开始时的活动事务列表
  通过 xmin/xmax 与快照比较判断可见

可见性判断（源码级 SnapshotData）：
  xmin_all_snapshot：快照可见的最小 xid
  xmax_all_snapshot：快照不可见的第一个 xid
  xip[]：仍在运行的 xid 列表（介于两者之间逐项比对）

判断规则：
  版本 xmin < 快照 xmin        → 必可见（早已提交）
  版本 xmin >= 快照 xmax       → 必不可见（晚于快照）
  xmin 在 xip[] 中            → 不可见（事务仍在运行）
  版本 xmax 在 xip[] 中        → 可见（删除者未提交）

对比 MySQL：
  MySQL ReadView 也做类似判断
  但 PG 的版本通过 VACUUM 回收
  （MySQL 通过 purge 线程清理 undo）
```

### 2.4 事务 ID 环形复用与冻结（面试必考）

```
事务 ID（xid）是 32 位整数 → 约 42 亿次事务后回绕（wraparound）
PG 用「冻结」（freeze）解决：
  VACUUM 把足够旧的事务 xmin 标记为 Frozen（特殊值 2）
  → 所有事务都可见它，永不失效

⚠️ 危险：回绕接近时 PG 会强制进入单用户/只读模式！
  监控命令：
```

```sql
-- 检查数据库距离回绕还有多少（age 超 1.5 亿就要警惕）
SELECT datname, age(datfrozenxid) AS tx_age
FROM pg_database ORDER BY tx_age DESC;

-- 查看最旧活跃事务（长事务是回绕+膨胀的元凶）
SELECT pid, xact_start, now() - xact_start AS duration, state
FROM pg_stat_activity
WHERE state <> 'idle' AND xact_start IS NOT NULL
ORDER BY xact_start LIMIT 5;

-- 紧急处理：全库冻结（低峰期执行）
VACUUM FREEZE VERBOSE ANALYZE;
```

> 💡 **实战经验**：`idle in transaction` 会话是 VACUUM 与回绕的头号敌人——用 `idle_in_transaction_session_timeout = '5min'` 兜底，Spring 事务里避免长事务/嵌套事务。

---

## 3. VACUUM 体系

### 3.1 VACUUM 是什么

```
VACUUM = 回收死元组（被删除/更新后的旧版本）

两种模式：
  ① VACUUM（普通）：标记死元组空间可复用（不归还 OS）
  ② VACUUM FULL：物理清理 + 紧缩表（锁定表！）

⚠️ 面试必答：
"VACUUM 是 PG 的生命线——不清理死元组
 会导致表膨胀 + 索引膨胀；
 Autovacuum 是默认守护进程。"
```

### 3.2 Autovacuum 与 PG 18 增强

```sql
-- Autovacuum：自动后台清理（默认开启）
-- 触发公式（死元组数超阈值即触发）：
--   死元组 > autovacuum_vacuum_scale_factor * 行数 + autovacuum_vacuum_threshold
--   默认 = 0.2 × 行数 + 50
autovacuum = on
autovacuum_vacuum_threshold = 50        -- 死元组阈值
autovacuum_analyze_threshold = 50

-- ⚠️ PG 18 新增：
autovacuum_vacuum_max_threshold = 100000000   -- 死元组硬上限（防止超大表等太久）
-- PG 18 新：autovacuum_max_workers 动态扩展（无需重启）
-- PG 17 新：eager freezing（VACUUM 时主动冻结旧元组，防回绕更从容）
```

### 3.3 Autovacuum 生产调优实战

```ini
# 生产推荐配置（大表/高变更 OLTP）
autovacuum_max_workers          = 6        # 默认 3，按库表规模调
autovacuum_naptime              = 15s      # 默认 60s，繁忙 OLTP 收紧
autovacuum_vacuum_cost_delay    = 2ms
autovacuum_vacuum_cost_limit    = 400      # 提升每次清理 I/O 预算
autovacuum_vacuum_scale_factor  = 0.015    # 默认 0.2 → 大表更早触发
autovacuum_analyze_scale_factor = 0.015    # 默认 0.1
autovacuum_freeze_max_age       = 400000000
```

```sql
-- ⚠️ 关键技巧：按表覆盖（大表/热表单独调，别全局一刀切）
ALTER TABLE orders SET (
    autovacuum_vacuum_scale_factor    = 0.01,
    autovacuum_vacuum_threshold       = 50,
    autovacuum_analyze_scale_factor   = 0.005
);

-- 超大表用绝对阈值（scale_factor 对亿级表几乎不触发）
ALTER TABLE events SET (
    autovacuum_vacuum_scale_factor = 0,
    autovacuum_vacuum_threshold    = 100000
);

-- 监控 autovacuum 健康
SELECT relname, n_dead_tup, last_autovacuum, autovacuum_count
FROM pg_stat_user_tables
ORDER BY n_dead_tup DESC LIMIT 10;
```

```
⚠️ 面试必答：
"Autovacuum 调优三招 = 按表覆盖（大表低 scale_factor）
 + 硬上限 max_threshold（PG 18）
 + 监控 n_dead_tup；
 VACUUM 不及时 = 表膨胀 + 索引膨胀 + 回绕风险。"
```

---

## 4. 隔离级别

### 4.1 级别与异常

| 级别 | 脏读 | 不可重复读 | 幻读 | 序列化异常 |
|------|:---:|:---:|:---:|:---:|
| Read Uncommitted | — | 可能 | 可能 | 可能 |
| Read Committed（默认） | 不可能 | 可能 | 可能 | 可能 |
| Repeatable Read | 不可能 | 不可能 | 不可能 | 可能 |
| Serializable | 不可能 | 不可能 | 不可能 | 不可能 |

```
⚠️ PG vs MySQL：
  PG 默认 Read Committed（SQL 标准默认）
  MySQL 默认 Repeatable Read
  PG 的 RR 通过 SSI（Serializable Snapshot Isolation）实现

⚠️ 面试必答：
"PG 默认 RC（MySQL 默认 RR）——
 PG 的 RR 是真正的可序列化快照隔离，
 不牺牲并发但性能开销可控。"
```

### 4.1 隔离级别实现原理（源码级）

```
PG 四级隔离实现：
  Read Committed → 语句开始取新快照
                   （每个语句看到最新已提交数据）
  Repeatable Read → 事务开始取快照（事务内所有语句一致）
                   + SSI 的读写冲突检测（防幻读/写偏斜）
  Serializable   → 快照隔离 + SSI 冲突检测 + 序列化异常
                   回滚（序列化失败报错 40001）

SSI（可序列化快照隔离）核心：
  跟踪事务间读写依赖（rw-antidependency）
  检测潜在环 → 触发序列化失败（不是锁等待）

对比 MySQL：
  MySQL RR 靠 next-key 锁（间隙锁）防幻读
  PG 靠 SSI 冲突检测（无间隙锁 → 无死锁风险，但可能 40001 失败）
  应用层应对：捕获 40001 重试事务（Spring @Retryable）
```

```java
// Spring 处理 PG 序列化失败重试
@Retryable(retryFor = CannotSerializeTransactionException.class, maxAttempts = 3)
public void transfer(int from, int to, BigDecimal amount) { ... }
```

---

## 5. 锁机制

### 5.1 锁级别（完整 8 级）

```
PG 表级锁（轻 → 重，冲突矩阵简表）：

  级别                    冲突的锁                   典型操作
  AccessShareLock         AccessExclusive            SELECT
  RowShareLock            Exclusive/AccessExclusive  SELECT FOR UPDATE
  RowExclusiveLock        Share/ShareRowExclusive/    INSERT/UPDATE/DELETE
                          Exclusive/AccessExclusive
  ShareUpdateExclusive    ShareUpdateExclusive/       VACUUM(非 full)
                          Share/ShareRowExclusive/…
  Share                   RowExclusive/…             CREATE INDEX CONCURRENTLY
  ShareRowExclusive       RowExclusive/Share/…       INSERT+FK 校验
  Exclusive               RowShare/RowExclusive/…    REFRESH MAT VIEW
  AccessExclusiveLock     ALL（含 AccessShare）      ALTER TABLE/DROP/TRUNCATE

⚠️ 读写不互斥：SELECT 不阻塞 UPDATE
  （MVCC 快照读天然非阻塞）——但 SELECT 与 ALTER TABLE 互斥

⚠️ 面试必答：
"PG 锁轻到重八级——读写不互斥（MVCC 天然）、
 ALTER TABLE 要 AccessExclusiveLock
 （唯一会阻塞读的操作）。"
```

```sql
-- 查看锁等待（排查阻塞的利器）
SELECT pid, wait_event_type, wait_event,
       pg_blocking_pids(pid) AS blockers, state, query
FROM pg_stat_activity
WHERE wait_event_type = 'Lock' AND state = 'active';

-- 锁超时兜底（防止无限等待）
SET lock_timeout = '5s';
-- 会话级：SET lock_timeout 仅当前会话；全局：postgresql.conf
```

### 5.2 死锁检测与实战

```
PG 死锁：自动检测（等待图）+ deadlock_timeout（默认 1s）
  检测到死锁 → 回滚代价最小的事务（日志打印死锁详情）

经典死锁案例：
  事务 A：UPDATE t1 → 等 t2 的行锁
  事务 B：UPDATE t2 → 等 t1 的行锁
  → 互相等待成环 → 1 秒后检测，牺牲一方

预防：
  ① 事务按相同顺序操作（避免环）
  ② 尽量短事务（缩小锁持有窗口）
  ③ 死锁超时配置（deadlock_timeout 调小加快发现，但增加误判）
```

```
⚠️ 面试必答：
"PG 死锁自动检测（deadlock_timeout 默认 1s 扫等待图）
 + 代价最小牺牲；
 业务侧：统一加锁顺序 + 短事务 + 捕获 40P01 重试。"
```

---

> 🎯 **核心要点**：PG MVCC = **多版本存在表空间**（xmin/xmax 标记 + HOT 同页更新 + VACUUM 回收）+ **快照隔离**（xip 列表判断）+ **事务 ID 冻结防回绕** + **Autovacuum 守护**（按表覆盖调优）+ **默认 RC**（SSI 防幻读）+ **八级锁读写不互斥**。对比 MySQL：PG 无回滚段但需 VACUUM，这是**最大的结构差异**。

---

**返回总览**：[00-PostgreSQL总览与核心概念](00-PostgreSQL总览与核心概念.md) | **上一篇**：[02-PostgreSQL索引原理与设计](02-PostgreSQL索引原理与设计.md) | **下一篇**：[04-PostgreSQL扩展生态](04-PostgreSQL扩展生态.md)
