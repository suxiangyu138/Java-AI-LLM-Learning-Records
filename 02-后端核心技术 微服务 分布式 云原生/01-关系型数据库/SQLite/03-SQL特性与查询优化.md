# SQL特性与查询优化
> SQLite 的 SQL 方言特性、CTE 与窗口函数、索引设计与 EXPLAIN QUERY PLAN：写出高性能查询的完整方法论。

---

## 📚 目录

1. [SQL 方言特性](#1-sql-方言特性)
2. [CTE 与递归查询](#2-cte-与递归查询)
3. [窗口函数](#3-窗口函数)
4. [索引设计](#4-索引设计)
5. [EXPLAIN QUERY PLAN 与优化器](#5-explain-query-plan-与优化器)
6. [查询优化技巧](#6-查询优化技巧)

---

## 1. SQL 方言特性

### 1.1 与标准 SQL 的差异

| 特性 | SQLite | MySQL/PG 对照 |
|------|--------|--------------|
| 类型 | 动态/STRICT | 静态 |
| UPSERT | ON CONFLICT DO UPDATE | INSERT ... ON DUPLICATE KEY |
| 返回行 | RETURNING（3.35+） | 类似 |
| 窗口函数 | ✅（3.25+） | ✅ |
| CTE/递归 | ✅（3.8.3+） | ✅ |
| 部分索引 | ✅ | PG ✅/MySQL ❌ |
| 表达式索引 | ✅ | ✅ |
| UPSERT 语义 | 完整（DO NOTHING/UPDATE） | 部分 |

### 1.2 常用方言语法

```sql
-- UPSERT（3.24+ 标准姿势）
INSERT INTO users (id, name, count)
VALUES (1, 'alice', 1)
ON CONFLICT(id) DO UPDATE SET
  count = users.count + 1;

-- INSERT OR REPLACE（旧语法，注意：替换 = 删除+插入）
INSERT OR REPLACE INTO users (id, name) VALUES (1, 'bob');

-- RETURNING（3.35+，拿回插入后的值）
INSERT INTO orders (user_id, amount)
VALUES (42, 99.9)
RETURNING id, created_at;

-- 批量插入（VALUES 多行）
INSERT INTO t (a, b) VALUES (1, 'x'), (2, 'y'), (3, 'z');
```

### 1.3 实用内建函数

```text
字符串：substr、instr、replace、printf、trim/ltrim/rtrim
数值：round、abs、random、randomblob、hex、unhex
日期：date/time/datetime/julianday/strftime（详见下方）
聚合：count/sum/avg/min/max/group_concat/total
JSON：json/jsonb 家族（05 模块）
全文：match（06 模块）

日期函数（SQLite 特色）：
  datetime('now')          当前 UTC 时间
  datetime('now', 'localtime')  本地时间
  date('2026-08-06', '+1 day')  日期运算
  strftime('%Y-%m-%d', 'now')   格式化
  julianday('2026-08-06')       儒略日（差值计算）
```

---

## 2. CTE 与递归查询

### 2.1 公共表表达式（CTE）

```sql
-- WITH 子句：可读性 + 复用
WITH recent AS (
    SELECT user_id, MAX(created_at) AS last_time
    FROM orders
    GROUP BY user_id
)
SELECT u.name, r.last_time
FROM users u JOIN recent r ON u.id = r.user_id;

-- 多个 CTE（逗号分隔）
WITH a AS (...), b AS (...) SELECT ... FROM a JOIN b;
```

### 2.2 递归 CTE（图/树查询）

```sql
-- 递归：组织树（manager 关系）
WITH RECURSIVE org_tree AS (
    -- 基础：根节点
    SELECT id, name, manager_id, 1 AS depth
    FROM employees WHERE manager_id IS NULL
    UNION ALL
    -- 递归：下一层
    SELECT e.id, e.name, e.manager_id, t.depth + 1
    FROM employees e JOIN org_tree t ON e.manager_id = t.id
)
SELECT * FROM org_tree ORDER BY depth, id;

-- 递归：1..10 序列
WITH RECURSIVE seq(x) AS (
    SELECT 1 UNION ALL SELECT x + 1 FROM seq WHERE x < 10
)
SELECT x FROM seq;
```

```text
递归 CTE 的应用场景：
  树形结构（组织、分类、评论）
  图遍历（可达性、路径）
  序列生成（日历、编号）

注意：默认递归上限 1000（可 PRAGMA recursive_triggers/或修改编译参数）
```

---

## 3. 窗口函数

### 3.1 基础语法

```sql
-- 每用户的订单序号（按时间）
SELECT user_id, amount,
       ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY created_at) AS rn
FROM orders;

-- 累计求和（跑动合计）
SELECT date, amount,
       SUM(amount) OVER (ORDER BY date) AS running_total
FROM daily_sales;

-- 与上一行的差值
SELECT date, amount,
       amount - LAG(amount) OVER (ORDER BY date) AS delta
FROM daily_sales;
```

### 3.2 常用窗口函数

| 函数 | 作用 |
|------|------|
| ROW_NUMBER() | 行号 |
| RANK() / DENSE_RANK() | 排名（并列处理不同） |
| LAG / LEAD | 前/后 N 行 |
| FIRST_VALUE / LAST_VALUE | 分区首/末值 |
| NTILE(n) | 分桶（百分位） |
| 聚合 + OVER | 累计/移动窗口 |

```text
窗口 vs GROUP BY：
  GROUP BY：聚合后行数减少（每组一行）
  窗口：保留所有行 + 附加聚合结果
  例："每用户订单数 + 订单明细" → 窗口一次搞定

性能注意：窗口函数需要排序（ORDER BY 子句）
  大表 + 无索引 → 内存排序（可观察到的慢）
```

---

## 4. 索引设计

### 4.1 索引类型

| 类型 | 语法 | 特点 |
|------|------|------|
| 普通索引 | CREATE INDEX | 单列/多列 |
| 唯一索引 | CREATE UNIQUE INDEX | 约束 + 加速 |
| 部分索引 | WHERE 子句 | 只索引子集（省空间） |
| 表达式索引 | ON t(expr) | 索引函数结果 |
| 覆盖索引 | 含查询全部列 | 免回表 |

```sql
-- 部分索引：只索引活跃订单（3.53 支持自愈表达式索引）
CREATE INDEX idx_active ON orders(status)
  WHERE status = 'ACTIVE';

-- 表达式索引：忽略大小写唯一
CREATE UNIQUE INDEX idx_email_ci ON users(lower(email));

-- 覆盖索引：查询免回表
CREATE INDEX idx_orders_uid_time ON orders(user_id, created_at);
-- SELECT user_id, created_at FROM orders WHERE user_id = 42
-- → 索引树直接出结果（无需查表树）
```

### 4.2 索引设计原则

```text
① WHERE 条件列 → 索引（等值在前，范围在后）
② ORDER BY 列 → 索引（避免排序）
③ GROUP BY 列 → 索引（分组加速）
④ 组合索引最左前缀：(a,b,c) 支持 a、a+b、a+b+c
⑤ 小表无需索引（全表扫描更快）
⑥ 写放大权衡：索引多 → 写入慢

EXPLAIN 验证：查询计划是否用了索引（见下）
```

### 4.3 索引的代价

```text
索引存储：每索引一棵 B-tree（额外空间）
索引维护：每次写操作更新所有相关索引（写放大）
索引碎化：频繁更新产生空闲页（可 REINDEX）

衡量标准：
  读多写少 → 大胆建索引
  写多读少 → 克制（日志/流水类）
  混合 → EXPLAIN 验证收益
```

---

## 5. EXPLAIN QUERY PLAN 与优化器

### 5.1 查看执行计划

```sql
EXPLAIN QUERY PLAN
SELECT u.name, o.amount
FROM users u JOIN orders o ON u.id = o.user_id
WHERE u.id = 42;

-- 输出示例：
-- SCAN users (id 索引查找？)
-- SEARCH orders USING INDEX idx_orders_uid (user_id=?)
```

```text
计划中关键词：
  SCAN：全表扫描（无索引，逐行读）
  SEARCH：索引查找（高效）
  USING INDEX：使用了哪个索引
  USING COVERING INDEX：覆盖索引（免回表）
  TEMP B-TREE：临时排序（可优化为索引排序）

判断标准：
  SEARCH > SCAN（能用索引就不用全表）
  COVERING 最好（连表都不用读）
```

### 5.2 查询优化器（Query Planner）

```text
优化器能力（SQLite）：
  ① 索引选择（代价模型估算）
  ② 连接顺序（启发式 + 代价）
  ③ 子查询扁平化（3.50+ 改进）
  ④ 表达式优化（3.49+ 计划器改进）
  ⑤ 自愈表达式索引（3.53：表达式索引失效时自动重建？）

局限：
  单线程执行（无并行查询）
  连接优化基于简化代价模型
  → 复杂查询性能靠索引设计（而非优化器魔法）
```

### 5.3 常见计划问题

| 现象 | 原因 | 对策 |
|------|------|------|
| 意外 SCAN | 缺索引/统计过期 | 建索引 |
| 排序 TEMP B-TREE | ORDER BY 无索引 | 建排序索引 |
| 回表多 | 索引不含查询列 | 覆盖索引 |
| 连接顺序差 | 大表先扫 | 重写 FROM 顺序/加条件 |
| LIKE '%x%' 慢 | 前缀通配无法用索引 | trigram 索引/FTS5 |

---

## 6. 查询优化技巧

### 6.1 实战清单

| 技巧 | 说明 |
|------|------|
| 用 EXISTS 替代 IN | 子查询场景更优 |
| 避免 SELECT * | 只取所需列（减少 IO） |
| 大分页用游标 | OFFSET 深分页慢（扫描跳过） |
| 批量写 | 事务包裹（一次提交） |
| 分析统计 | ANALYZE（优化器统计） |
| 参数化查询 | 复用预编译语句 |

### 6.2 深分页问题

```sql
-- 慢：OFFSET 深分页（跳过 10 万行）
SELECT * FROM logs ORDER BY id LIMIT 20 OFFSET 100000;

-- 快：游标（记录上一页最后 id）
SELECT * FROM logs WHERE id > 100000 ORDER BY id LIMIT 20;
-- 利用主键索引定位，无需扫描跳过
```

### 6.3 批量写入模式

```sql
-- 批量插入的正确姿势（万级+）：
-- ① 单事务包裹（默认每语句一个事务 → 慢）
BEGIN;
INSERT INTO t VALUES ...;  -- 或 executemany（预编译）
COMMIT;

-- ② 预编译 + 绑定（JDBC preparedStatement 批量）
-- ③ 关闭同步（PRAGMA synchronous=OFF——可容忍丢失场景）

实测参考：事务批量插入比逐条快 10-100 倍
```

> 🎯 **核心要点**：SQLite 的查询性能 = 索引设计 + 计划验证的组合——优化器简单直接，不会替你"创造奇迹"。标准流程：写查询 → EXPLAIN QUERY PLAN → 发现 SCAN/排序 → 建索引 → 复验。CTE/窗口/部分索引/覆盖索引是四个最常用的高级武器；深分页与批量写入是工程高频场景的必修课。

---

## 7. 小结

| 问题 | 答案 |
|------|------|
| 方言差异？ | UPSERT/RETURNING/部分索引/表达式索引 |
| 递归 CTE？ | WITH RECURSIVE（树/图/序列） |
| 窗口函数？ | ROW_NUMBER/LAG/累计等（3.25+） |
| 索引四类？ | 普通/唯一/部分/表达式/覆盖 |
| 看执行计划？ | EXPLAIN QUERY PLAN（SEARCH > SCAN） |
| 深分页？ | 游标（id > last）替代 OFFSET |

**下一模块**：[04-事务与锁机制](04-事务与锁机制.md)　**返回总览**：[00-SQLite知识体系总览](00-SQLite知识体系总览.md)
