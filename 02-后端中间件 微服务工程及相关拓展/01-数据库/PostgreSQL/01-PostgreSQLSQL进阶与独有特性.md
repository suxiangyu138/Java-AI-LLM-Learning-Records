# 01 - PostgreSQL SQL 进阶与独有特性

> 定位：PG 独有的 SQL 能力——CTE 递归与物化、窗口函数全解、LATERAL JOIN、UNION 家族、丰富数据类型、RETURNING 与 UPSERT

## 📚 目录

1. [CTE 与递归查询](#1-cte-与递归查询)
2. [窗口函数全解](#2-窗口函数全解)
3. [LATERAL JOIN](#3-lateral-join)
4. [RETURNING 与 UPSERT](#4-returning-与-upsert)
5. [UNION 家族](#5-union-家族)
6. [PG 18 SQL 新特性](#6-pg-18-sql-新特性)

---

## 1. CTE 与递归查询

### 1.1 CTE（WITH 子句）

```sql
-- CTE = 命名临时结果集（可读性 + 可复用 + 递归）
WITH user_stats AS (
    SELECT dept_id, COUNT(*) AS cnt
    FROM users GROUP BY dept_id
)
SELECT d.name, s.cnt
FROM departments d
JOIN user_stats s ON d.id = s.dept_id;

-- ⚠️ PG 18 增强：EXPLAIN 显示 CTE 物化方式（Memory/Disk）
-- 多层 CTE：可嵌套复用（比子查询清晰）
```

### 1.2 递归 CTE

```sql
-- 递归 CTE：处理树形数据（组织架构/分类层级）
WITH RECURSIVE org_tree AS (
    -- ① 锚点：根
    SELECT id, name, parent_id, 1 AS level
    FROM departments WHERE parent_id IS NULL
    UNION ALL
    -- ② 递归：子级 JOIN 自身
    SELECT d.id, d.name, d.parent_id, t.level + 1
    FROM departments d
    JOIN org_tree t ON d.parent_id = t.id
)
SELECT * FROM org_tree ORDER BY level, id;
```

> 🎯 **要点**：递归 CTE = 锚点 + 递归成员 UNION ALL——天然适配树形数据。PG 的 CTE 可配 `MATERIALIZED`/`NOT MATERIALIZED` 控制优化。

### 1.3 CTE 物化控制（优化器开关）

```sql
-- ⚠️ PG 独有：CTE 默认物化（把结果落盘/存内存再复用）
-- 问题：外层过滤无法下推 → 大 CTE 可能性能差

WITH orders_cte AS MATERIALIZED (
    SELECT * FROM orders WHERE amount > 100
)
SELECT * FROM orders_cte WHERE user_id = 1;
-- MATERIALIZED：orders 全表扫描先做，再过滤 user_id
-- 适合：CTE 被多处引用（只算一次）

WITH recent AS NOT MATERIALIZED (
    SELECT * FROM orders WHERE created_at > now() - interval '7 days'
)
SELECT * FROM recent WHERE user_id = 1;
-- NOT MATERIALIZED：CTE 内联展开 → 谓词下推
-- 适合：CTE 只引用一次（避免重复物化）

-- PG 18 增强：EXPLAIN 直接显示物化方式（Memory/Disk + 代价）
EXPLAIN (ANALYZE, BUFFERS)
WITH x AS (SELECT ...) SELECT ...;
```

| 控制 | 行为 | 适用场景 |
|------|------|---------|
| `MATERIALIZED` | 先算结果再复用 | CTE 被多次引用 |
| `NOT MATERIALIZED` | 内联展开 + 谓词下推 | CTE 单次引用、过滤性强 |
| 默认（PG 12+） | 优化器自动选择 | 多数场景 |

---

## 2. 窗口函数全解

```sql
-- PG 窗口函数（比 MySQL 更早支持、更完整）
SELECT name, dept_id, salary,
    -- 排名
    ROW_NUMBER() OVER w AS rn,
    RANK() OVER w AS rk,
    DENSE_RANK() OVER w AS dr,
    -- 前后行
    LAG(salary) OVER w AS prev_sal,
    LEAD(salary) OVER w AS next_sal,
    -- 累计
    SUM(salary) OVER (ORDER BY salary ROWS UNBOUNDED PRECEDING) AS cum
FROM employees
WINDOW w AS (PARTITION BY dept_id ORDER BY salary DESC);
-- ⚠️ PG 特有：WINDOW 子句命名窗口（复用）
```

| 窗口函数 | 用途 |
|---------|------|
| ROW_NUMBER/RANK/DENSE_RANK | 排名 |
| LAG/LEAD | 前后行 |
| SUM/AVG/COUNT OVER | 聚合窗口 |
| FIRST_VALUE/LAST_VALUE/NTH_VALUE | 首尾/N 值 |
| NTILE(n) | 分桶 |

### 2.1 Frame 子句（窗口范围精确定义）

```sql
-- ⚠️ 窗口函数的灵魂 = FRAME（计算范围），PG 三种模式
SELECT
    -- ① ROWS：按物理行数
    SUM(amount) OVER (ORDER BY day ROWS BETWEEN 6 PRECEDING AND CURRENT ROW) AS week_avg,
    -- ② RANGE：按逻辑值范围（⚠️ 默认模式，重复值同帧）
    SUM(amount) OVER (ORDER BY day RANGE BETWEEN INTERVAL '7 days' PRECEDING AND CURRENT ROW) AS rolling_7d,
    -- ③ GROUPS：按分组（ORDER BY 相同值的组为单位）
    SUM(amount) OVER (ORDER BY day GROUPS BETWEEN 1 PRECEDING AND CURRENT ROW) AS prev_group
FROM sales;

-- 移动平均（滑动窗口经典）
SELECT day, amount,
    AVG(amount) OVER (ORDER BY day ROWS BETWEEN 6 PRECEDING AND CURRENT ROW) AS ma7
FROM sales;
```

| 模式 | 单位 | 特点 |
|------|------|------|
| ROWS | 物理行 | 直观、无歧义 |
| RANGE | 值范围 | 重复值同帧（默认） |
| GROUPS | 值组 | 重复值成组移动 |

### 2.2 企业高频分析场景

| 场景 | 窗口写法 | 说明 |
|------|---------|------|
| TopN per group | `ROW_NUMBER() OVER (PARTITION BY dept ORDER BY salary DESC)` | 部门 Top3 |
| 同比/环比 | `LAG(amount) OVER (ORDER BY month)` | 对比上月 |
| 移动平均 | `AVG() OVER (... ROWS 6 PRECEDING)` | 股价/指标平滑 |
| 累积求和 | `SUM() OVER (... ROWS UNBOUNDED PRECEDING)` | 余额/库存 |
| 去重取一条 | `ROW_NUMBER() = 1` 再过滤 | 订单最新状态 |
| 分组占比 | `amount / SUM(amount) OVER (PARTITION BY dept)` | 部门内占比 |

---

## 3. LATERAL JOIN

```sql
-- LATERAL：子查询引用左侧 FROM 的列（类似 for-each 循环）
-- 场景：每个部门取前 3 名高薪员工
SELECT d.name, emp.name, emp.salary
FROM departments d
JOIN LATERAL (
    SELECT name, salary
    FROM employees e
    WHERE e.dept_id = d.id      -- ⚠️ 引用左侧列
    ORDER BY salary DESC
    LIMIT 3
) emp ON true;

-- ⚠️ LATERAL 与普通 JOIN 的区别：
--   普通 JOIN 子查询先执行（无法引用左表）
--   LATERAL 子查询可以引用左表当前行
```

> 🎯 **要点**：LATERAL = "行级子查询"——能引用左侧列执行动态子查询。TopN per group、复杂计算子查询的标准答案。

### 3.1 LATERAL vs 相关子查询 vs EXISTS

| 写法 | 原理 | 性能 |
|------|------|------|
| LATERAL JOIN | 每行执行子查询，可 LIMIT | ✅ 最快（TopN） |
| 相关子查询（EXISTS） | 逐行扫描验证 | 中等 |
| 窗口函数 | 全表一次排序 | TopN 大 N 时更优 |

```sql
-- 场景：TopN per group 的三种写法对比（取每部门薪资 Top2）
-- ① LATERAL（TopN 首选）
SELECT d.name, e.* FROM departments d
JOIN LATERAL (SELECT name, salary FROM employees
              WHERE dept_id = d.id ORDER BY salary DESC LIMIT 2) e ON true;

-- ② 窗口函数（N 较小时写起来简单）
SELECT * FROM (
    SELECT name, dept_id, salary,
           ROW_NUMBER() OVER (PARTITION BY dept_id ORDER BY salary DESC) rn
    FROM employees) t WHERE rn <= 2;

-- ③ 相关子查询 + EXISTS（数据少时可读性好，性能最差）
SELECT e.* FROM employees e
WHERE 2 > (SELECT COUNT(*) FROM employees e2
           WHERE e2.dept_id = e.dept_id AND e2.salary > e.salary);
```

> 💡 **选型经验**：数据量大、每组只取前 N → LATERAL；N 小且 SQL 简单 → 窗口函数；嵌套条件复杂 → EXISTS。

---

## 4. RETURNING 与 UPSERT

### 4.1 RETURNING

```sql
-- RETURNING：增删改操作返回受影响的行的数据（免 SELECT 回查）
INSERT INTO users(name, email) VALUES ('张三', 'a@b.com')
RETURNING id, created_at;        -- ⚠️ PG 独有

UPDATE users SET status = 'active' WHERE id = 1
RETURNING *;

DELETE FROM logs WHERE created_at < now() - interval '90 days'
RETURNING id;

-- ⚠️ PG 18：RETURNING 支持 OLD/NEW 引用（如 UPDATE 前后对比）
UPDATE products SET price = price * 0.9
WHERE id = 5
RETURNING id, OLD.price AS old_price, NEW.price AS new_price;
```

> 💡 **实战价值**：RETURNING 与 DML 原子执行——高并发下免「先查再改」的竞态窗口；配合 `INSERT ... RETURNING id` 是获取自增 ID 的标准姿势（替代 `lastval()` 显式调用）。

### 4.2 UPSERT（ON CONFLICT）

```sql
-- UPSERT = INSERT ... ON CONFLICT（有冲突则更新）
INSERT INTO users(id, name, score) VALUES (1, '张三', 100)
ON CONFLICT (id)
DO UPDATE SET name = EXCLUDED.name,        -- ⚠️ EXCLUDED = 拟插入值
              score = users.score + EXCLUDED.score;

-- DO NOTHING：冲突忽略
INSERT INTO user_logins(user_id, login_at) VALUES (1, now())
ON CONFLICT (user_id, login_at) DO NOTHING;
```

> 🎯 **要点**：RETURNING + UPSERT = 增删改操作的"省去回查"。UPSERT 的 `EXCLUDED` 引用拟插入值是关键细节。

### 4.3 UPSERT 批量场景与陷阱

```sql
-- ① 批量 UPSERT（插入/更新 10 万行，推荐 COPY + ON CONFLICT 或单条多值）
INSERT INTO users(id, name, score) VALUES
    (1, '张三', 90), (2, '李四', 85), (3, '王五', 95)
ON CONFLICT (id) DO UPDATE SET score = EXCLUDED.score;

-- ② 无主键冲突列时可指定任意唯一约束
ON CONFLICT ON CONSTRAINT users_email_key
DO UPDATE SET name = EXCLUDED.name;

-- ③ 条件更新（冲突时按业务规则决定是否更新）
INSERT INTO counters(id, cnt) VALUES (1, 1)
ON CONFLICT (id) DO UPDATE SET
    cnt = counters.cnt + EXCLUDED.cnt        -- 累加器模式
RETURNING cnt;

-- ⚠️ 三大陷阱：
--   1) 冲突列必须被唯一索引/主键覆盖，否则报错
--   2) 同一语句多行时：同一行冲突只执行一次 DO UPDATE
--   3) 高并发下 ON CONFLICT 依赖唯一索引的锁，误用 O_EXCL 会阻塞
```

| 对比项 | PG `ON CONFLICT` | MySQL `ON DUPLICATE KEY` | SQL Server `MERGE` |
|--------|:---:|:---:|:---:|
| 冲突定位 | 指定列/约束 | 任意唯一键 | MATCH 条件 |
| 语义清晰度 | ✅ 最清晰 | 隐式（易误触发） | 强（但语法繁） |
| 可读性 | 高 | 低 | 中 |

---

## 5. UNION 家族

```sql
-- UNION：去重合并
-- UNION ALL：保留全部（推荐：明确不去重时用 ALL）
-- INTERSECT：取交集
-- EXCEPT：取差集（A 有 B 无）
```

| 操作 | 语义 | 性能 |
|------|------|:---:|
| UNION | 去重合并 | 慢（排序去重） |
| UNION ALL | 全部合并 | ✅ 快 |
| INTERSECT | 交集 | 中等 |
| EXCEPT | 差集 | 中等 |

```
⚠️ 面试必答：
"PG UNIQUE 特性——RETURNING 免回查、
 UPSERT 冲突更新、LATERAL 行级子查询、
 递归 CTE 树形查询、CTE 物化控制；
 MySQL 8.0 后才补齐窗口/CTE。"
```

---

## 6. PG 18 SQL 新特性

> 💡 以下为 PG 18（2025-09 发布，2026 生产验证中）新增的 SQL 能力，面试加分项。

### 6.1 JSON_TABLE()（SQL/JSON 标准）

```sql
-- ⚠️ PG 18 首次原生支持 JSON_TABLE（对标 Oracle/MySQL 8.4）
-- 把 JSON 数组拆成关系行（免 jsonb_each + LATERAL 手工拆）
SELECT jt.*
FROM orders,
JSON_TABLE(payload, '$[*]' COLUMNS (
    item_id     BIGINT  PATH '$.id',
    item_name   TEXT    PATH '$.name',
    qty         INT     PATH '$.qty'
)) jt;

-- PG 17 前写法（等价）：jsonb_to_recordset
SELECT * FROM jsonb_to_recordset(
    (SELECT payload FROM orders WHERE id = 1)
) AS jt(item_id BIGINT, item_name TEXT, qty INT);
```

### 6.2 虚拟生成列（默认虚拟）

```sql
-- PG 18：GENERATED ALWAYS AS 默认虚拟（不占磁盘）
CREATE TABLE products (
    price   numeric,
    tax     numeric,
    price_with_tax numeric GENERATED ALWAYS AS (price * (1 + tax)) VIRTUAL
);
-- 对比 PG 17 需要显式 STORED（占磁盘）
```

### 6.3 时态约束（Temporal Constraints）

```sql
-- PG 18：时间区间唯一性约束（如排班、会议室预订不重叠）
CREATE TABLE bookings (
    room_id   int,
    period    tstzrange,
    EXCLUDE USING gist (room_id WITH =, period WITH &&)
) PARTITION BY RANGE (period);
```

| PG 18 SQL 特性 | 一句话 | 对标 |
|---------------|--------|------|
| JSON_TABLE | JSON → 关系行 | Oracle/MySQL 8.4 |
| RETURNING OLD/NEW | DML 前后值 | SQL Server OUTPUT |
| 虚拟生成列默认 | 不占磁盘 | MySQL 默认行为 |
| 时态约束 | 区间不重叠 | SQL Server 无原生 |
| uuidv7() | 时间有序 UUID | MySQL 无 |

---

> 🎯 **核心要点**：PG SQL 独有 = **CTE 递归**（树形数据）+ **LATERAL JOIN**（行级子查询）+ **RETURNING**（免回查）+ **UPSERT**（ON CONFLICT）+ **WINDOW 命名窗口** + **PG 18 JSON_TABLE/OLD-NEW/虚拟生成列**。这些是 PG 相对 MySQL 的查询能力优势——分析/复杂查询场景的利器。

---

**返回总览**：[00-PostgreSQL总览与核心概念](00-PostgreSQL总览与核心概念.md) | **下一篇**：[02-PostgreSQL索引原理与设计](02-PostgreSQL索引原理与设计.md)
